package cz.muni.fi.randgka.randgkaapp;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import cz.muni.fi.randgka.bluetoothgka.BluetoothCommunicationService;
import cz.muni.fi.randgka.gka.GKAParticipant;
import cz.muni.fi.randgka.gka.GKAParticipants;
import cz.muni.fi.randgka.gka.GKAProtocolParams;
import cz.muni.fi.randgka.provider.minentropy.CameraMES;
import cz.muni.fi.randgka.provider.minentropy.CameraMESHolder;
import cz.muni.fi.randgka.tools.Constants;
import cz.muni.fi.randgka.wifigka.WifiCommunicationService;
import cz.muni.fi.randgkaapp.R;
import android.os.Bundle;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * GKAActivity displays an overview of the protocol to be executed. To the member, 
 * the protocol parameters are displayed, while to the leader the possibility to 
 * choose them is given. Furthermore, all participating devices are printed, together 
 * with their public keys (with the appropriate hash values), to provide a basic visual 
 * verification. After all participants have connected, leader can invoke a protocol 
 * instance by pressing the "Run protocol" button. A shared key is displayed on 
 * the same screen. The protocol can be run multiple times using the same settings. 
 * If the application was invoked by another application with the aim of key retrieval, 
 * the button "Retrieve key" is displayed, with an obvious purpose, after the successful 
 * protocol instance run.
 */
public class GKAActivity extends Activity {

	// button for key retrieval confirmation
	private Button retrieveButton;
	
	// resulting key
	private byte[] key;
	
	// communication technology
	private String technology;
	
	// spinners for parameters choice
	private Spinner nonceLengthsSpinner,
		groupKeyLengthsSpinner,
		publicKeyLengthsSpinner;
	
	// parameters, participants and key output view components
	private TextView protocolParticipantsTV,
		gkaTV,
		versionTV,
		groupKeyLengthTV,
		publicKeyLengthTV,
		nonceLengthTV;
	
	// broadcast receiver actions
	public static final String GET_PARTICIPANTS = "get_participants", // print participants
			GET_PARAMS = "get_params", // print params
			PRINT_DEVICE = "print_device", // print new device (leader only)
			RETRIEVE_GKA_KEY = "show_retrieve_key", // retrieve gka key
			PRINT_GKA_KEY = "get_gka_key", // print gka key
	 		NOT_ACTIVE = "not_active";
	
	private BroadcastReceiver br;
	private LocalBroadcastManager lbm;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// if is leader: print the spinner to choose parameters + run protocol button to start protocol
		if (this.getIntent().getBooleanExtra(Constants.IS_LEADER, false)) {
			setContentView(R.layout.activity_gkaprotocol);
			
			ArrayAdapter<Integer> nonceLengths = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item);
			nonceLengths.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			nonceLengths.add(32);
			nonceLengths.add(64);
			nonceLengths.add(128);
			nonceLengthsSpinner = (Spinner)findViewById(R.id.spinner2);
			nonceLengthsSpinner.setAdapter(nonceLengths);
			
			ArrayAdapter<Integer> groupKeyLengths = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item);
			groupKeyLengths.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			groupKeyLengths.add(1024);
			groupKeyLengths.add(2048);
			groupKeyLengthsSpinner = (Spinner)findViewById(R.id.spinner3);
			groupKeyLengthsSpinner.setAdapter(groupKeyLengths);
			
			ArrayAdapter<CharSequence> publicKeyLengths = ArrayAdapter.createFromResource(this, R.array.pkLengths, android.R.layout.simple_spinner_item);
			publicKeyLengths.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			publicKeyLengthsSpinner = (Spinner)findViewById(R.id.spinner4);
			publicKeyLengthsSpinner.setAdapter(publicKeyLengths);
			
			Button runButton = (Button)findViewById(R.id.button1);
			runButton.setClickable(true);
		}
		// if is member: get the components for parameters printing
		else {
			setContentView(R.layout.activity_gkaprotocol2);
			versionTV = (TextView)findViewById(R.id.textView5);
			nonceLengthTV = (TextView)findViewById(R.id.textView6);
			groupKeyLengthTV = (TextView)findViewById(R.id.textView8);
			publicKeyLengthTV = (TextView)findViewById(R.id.textView10);
		}
		
		technology = getIntent().getStringExtra("technology");
		
		// get and hide the retrieve button
		retrieveButton = (Button)findViewById(R.id.button2);
		retrieveButton.setVisibility(View.GONE);
		
		// get the components for key and participants printing
		protocolParticipantsTV = (TextView)findViewById(R.id.textView13);
		gkaTV = (TextView)findViewById(R.id.textView11);		
		
		technology = getIntent().getStringExtra(Constants.TECHNOLOGY);
		
		// obtain camera source of randomness objects needed for randomness provider
		SurfaceView surface = null;
		surface=(SurfaceView)findViewById(R.id.surfaceView1);
        SurfaceHolder holder=surface.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        CameraMES cameraMES = new CameraMES();
        cameraMES.initialize(surface);
        CameraMESHolder.cameraMES = cameraMES;
        
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.empty, menu);
		return true;
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		registerCustomReceiver();
		
		// after the preview screen is created, we can register the secure random object utilizing it
        Intent setSecureRandom = new Intent(this, technology.equals(Constants.WIFI_GKA) ? WifiCommunicationService.class : BluetoothCommunicationService.class);
        setSecureRandom.setAction(Constants.SET_SECURE_RANDOM);
        startService(setSecureRandom);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		clean();
	}
	
	private void clean() {
		if (CameraMESHolder.cameraMES != null) {
			CameraMESHolder.cameraMES.stop();
			CameraMESHolder.cameraMES = null;
		}
		
		// stop the communication threads
		if (technology.equals(Constants.BLUETOOTH_GKA)) {
			Intent stopThreads = new Intent(this, BluetoothCommunicationService.class);
			stopThreads.setAction(Constants.STOP);
			startService(stopThreads);
		} else if (technology.equals(Constants.WIFI_GKA)) {
			Intent stopThreads = new Intent(this, WifiCommunicationService.class);
			stopThreads.setAction(Constants.STOP);
			startService(stopThreads);
		}
		
		if (br != null) lbm.unregisterReceiver(br);
	}

	private void registerCustomReceiver() {

		// register new Broadcast receiver to enable the user interaction
		IntentFilter returnKeyFilter = new IntentFilter();
		returnKeyFilter.addAction(RETRIEVE_GKA_KEY);
		returnKeyFilter.addAction(GET_PARTICIPANTS);
		returnKeyFilter.addAction(PRINT_GKA_KEY);
		returnKeyFilter.addAction(GET_PARAMS);
		returnKeyFilter.addAction(PRINT_DEVICE);
		returnKeyFilter.addAction(NOT_ACTIVE);
		br = new BroadcastReceiver() {
			
			/**
			 * simple key print in the aprropriate object
			 * 
			 * @param key to print
			 */
			private void printKey(byte[] key) {
				if (gkaTV != null) {
					gkaTV.setText("");
					gkaTV.append((new BigInteger(key)).toString(16));
				}
			}
			
			@Override
			public void onReceive(Context context, Intent intent) {
				
				// make the key retrievable
				if (intent.getAction() == RETRIEVE_GKA_KEY) {
					key = intent.getByteArrayExtra(Constants.KEY);
					printKey(key);
					retrieveButton.setVisibility(View.VISIBLE);
				} 
				// print participants
				else if (intent.getAction().equals(GET_PARTICIPANTS)) {
					protocolParticipantsTV.setText("");
					GKAParticipants gkaParticipants = (GKAParticipants)intent.getSerializableExtra(Constants.PARTICIPANTS);

					if (gkaParticipants != null) {
						for (GKAParticipant g : gkaParticipants.getParticipants()) {
							protocolParticipantsTV.append(g.getName()); // name
							if (g.getPublicKey()!=null) {
								try {
									// nonce + key message digest
									MessageDigest md = MessageDigest.getInstance("SHA-256");
									md.update(g.getNonce());
									protocolParticipantsTV.append("\nAuthentication hash: " + (new BigInteger(1, md.digest(g.getPublicKey().getEncoded())).toString(16)));
								} catch (NoSuchAlgorithmException e) {
									e.printStackTrace();
								}
							}
							protocolParticipantsTV.append("\n\n");
						}
					}
				} 
				// print params
				else if (intent.getAction().equals(GET_PARAMS)) {
					String version = intent.getIntExtra("version", 0) != GKAProtocolParams.NON_AUTH ? 
							(intent.getIntExtra("version", 1) == GKAProtocolParams.AUTH_CONF ? 
									"Authenticated + key confirmation" : "Authenticated") : "Non-authenticated";
					String nonceLength = String.valueOf(intent.getIntExtra("nonceLength", 0)*8);
					String groupKeyLength = String.valueOf(intent.getIntExtra("groupKeyLength", 0)*8);
					String publicKeyLength = String.valueOf(intent.getIntExtra("publicKeyLength", 0)*8);
					
					versionTV.setText(version);
					nonceLengthTV.setText(nonceLength);
					groupKeyLengthTV.setText(groupKeyLength);
					publicKeyLengthTV.setText(publicKeyLength);
					
				} 
				// print key
				else if (intent.getAction().equals(PRINT_GKA_KEY)) {
					key = intent.getByteArrayExtra(Constants.KEY);
					printKey(key);
				} 
				// print new device (leader only)
				else if (intent.getAction().equals(PRINT_DEVICE)) {
					protocolParticipantsTV.append(intent.getStringExtra(Constants.DEVICE)+"\n");
				}
				// communication service is not in an active state
				else if (intent.getAction().equals(NOT_ACTIVE)) {
					finish();
				}
			}
		};
		lbm = LocalBroadcastManager.getInstance(this);
		lbm.registerReceiver(br, returnKeyFilter);
	}
	
	/**
	 * Start by the retrieve key button click.
	 * Returns the key to the calling application.
	 * 
	 * @param view
	 */
	public void retrieveKey(View view) {
		Intent result = new Intent();       
		result.putExtra(Constants.KEY, key);
		setResult(Activity.RESULT_OK, result);
		finish();
	}
	
	/**
	 * Start by the run protocol button.
	 * 
	 * @param view
	 */
	public void runProtocol(View view) {
		// obtain wanted parameters
		Integer nonceLength = (Integer)nonceLengthsSpinner.getSelectedItem();
		Integer groupKeyLength = (Integer)groupKeyLengthsSpinner.getSelectedItem();
		Integer publicKeyLength = Integer.parseInt(String.valueOf((CharSequence) publicKeyLengthsSpinner.getSelectedItem()));
		RadioButton authRadio = (RadioButton)findViewById(R.id.radio1);
		RadioButton authConfRadio = (RadioButton)findViewById(R.id.radio2);
		
		// preset the new intent
		Intent commServiceIntent = new Intent();
		commServiceIntent.putExtra("nonceLength", nonceLength);
		commServiceIntent.putExtra("groupKeyLength", groupKeyLength);
		commServiceIntent.putExtra("publicKeyLength", publicKeyLength);
		commServiceIntent.putExtra("version", authRadio.isChecked()?1:(authConfRadio.isChecked()?2:0));
		commServiceIntent.putExtra(Constants.RETRIEVE_KEY, getIntent().getBooleanExtra(Constants.RETRIEVE_KEY, false));
		
		// run protocol using the appropriate technology
		if (technology.equals(Constants.WIFI_GKA)) {
			commServiceIntent.setClass(this, WifiCommunicationService.class);
			commServiceIntent.setAction(Constants.GKA_RUN);
			startService(commServiceIntent);
		} else if (technology.equals(Constants.BLUETOOTH_GKA)) {
			commServiceIntent.setClass(this, BluetoothCommunicationService.class);
			commServiceIntent.setAction(Constants.GKA_RUN);
			startService(commServiceIntent);
		}
	}
}
