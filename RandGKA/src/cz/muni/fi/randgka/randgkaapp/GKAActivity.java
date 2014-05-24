package cz.muni.fi.randgka.randgkaapp;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import cz.muni.fi.randgka.bluetoothgka.BluetoothCommunicationService;
import cz.muni.fi.randgka.gka.GKAParticipant;
import cz.muni.fi.randgka.gka.GKAParticipants;
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
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

public class GKAActivity extends Activity {

	private Button retrieveButton;
	private byte[] key;
	private String technology;
	
	private Spinner nonceLengthsSpinner,
	groupKeyLengthsSpinner,
	publicKeyLengthsSpinner;
	
	private TextView protocolParticipantsTV,
		gkaTV,
		versionTV,
		groupKeyLengthTV,
		publicKeyLengthTV,
		nonceLengthTV;
	
	public static final String GET_PARTICIPANTS = "get_participants",
			GET_PARAMS = "get_params",
			PRINT_DEVICE = "print_device",
			RETURN_GKA_KEY = "return_gka_key",
			SHOW_RETRIEVE_KEY = "show_retrieve_key",
			GET_GKA_KEY = "get_gka_key";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (this.getIntent().getBooleanExtra("isLeader", false)) {
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
		else {
			setContentView(R.layout.activity_gkaprotocol2);
			versionTV = (TextView)findViewById(R.id.textView5);
			nonceLengthTV = (TextView)findViewById(R.id.textView6);
			groupKeyLengthTV = (TextView)findViewById(R.id.textView8);
			publicKeyLengthTV = (TextView)findViewById(R.id.textView10);
		}
		
		technology = getIntent().getStringExtra("technology");
		
		retrieveButton = (Button)findViewById(R.id.button2);
		retrieveButton.setVisibility(View.GONE);
		
		protocolParticipantsTV = (TextView)findViewById(R.id.textView13);
		gkaTV = (TextView)findViewById(R.id.textView11);		
		
		IntentFilter returnKeyFilter = new IntentFilter();
		returnKeyFilter.addAction(RETURN_GKA_KEY);
		returnKeyFilter.addAction(SHOW_RETRIEVE_KEY);
		returnKeyFilter.addAction(GET_PARTICIPANTS);
		returnKeyFilter.addAction(GET_GKA_KEY);
		returnKeyFilter.addAction(GET_PARAMS);
		returnKeyFilter.addAction(PRINT_DEVICE);
		
		BroadcastReceiver rkReceiver = new BroadcastReceiver() {
			
			private String bytesToHex(byte[] in) {
			    final StringBuilder builder = new StringBuilder();
			    for(byte b : in) {
			        builder.append(String.format("%02x", b));
			    }
			    return builder.toString();
			}
			
			@Override
			public void onReceive(Context context, Intent intent) {
				
				if (intent.getAction() == SHOW_RETRIEVE_KEY) {
					key = intent.getByteArrayExtra(Constants.KEY);
					retrieveButton.setVisibility(View.VISIBLE);
				} 
				else if (intent.getAction().equals(GET_PARTICIPANTS)) {
					protocolParticipantsTV.setText("");
					GKAParticipants gkaParticipants = (GKAParticipants)intent.getSerializableExtra("participants");

					if (gkaParticipants != null) {
						
						for (GKAParticipant g : gkaParticipants.getParticipants()) {
							protocolParticipantsTV.append(g.getName());
							if (g.getAuthPublicKey()!=null) {
								try {
									MessageDigest md = MessageDigest.getInstance("SHA-256");
									md.update(g.getNonce());
									protocolParticipantsTV.append("\nAuthentication hash: " + bytesToHex(md.digest(g.getAuthPublicKey().getEncoded())));
								} catch (NoSuchAlgorithmException e) {
									e.printStackTrace();
								}
							}
							protocolParticipantsTV.append("\n\n");
						}
					}
				} 
				else if (intent.getAction().equals(GET_PARAMS)) {
					String version = intent.getIntExtra("version", 0)!=0 ? (intent.getIntExtra("version", 1)==2 ? "Authenticated + key confirmation" : "Authenticated") : "Non-authenticated";
					String nonceLength = String.valueOf(intent.getIntExtra("nonceLength", 0)*8);
					String groupKeyLength = String.valueOf(intent.getIntExtra("groupKeyLength", 0)*8);
					String publicKeyLength = String.valueOf(intent.getIntExtra("publicKeyLength", 0)*8);
					
					versionTV.setText(version);
					nonceLengthTV.setText(nonceLength);
					groupKeyLengthTV.setText(groupKeyLength);
					publicKeyLengthTV.setText(publicKeyLength);
					
				} 
				else if (intent.getAction().equals(GET_GKA_KEY)) {
					if (gkaTV != null) {
						gkaTV.setText("");
						
						key = intent.getByteArrayExtra(Constants.KEY);
						Log.d("key","gkaact"+Arrays.toString(key));
						gkaTV.append((new BigInteger(key)).toString(16));
					}
				} 
				else if (intent.getAction().equals(PRINT_DEVICE)) {
					protocolParticipantsTV.append(intent.getStringExtra("device")+"\n");
				}
			}
		};
		
		LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
		lbm.registerReceiver(rkReceiver, returnKeyFilter);
		
		technology = getIntent().getStringExtra("technology");
		
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
	protected void onStop() {
		super.onStop();
		clean();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		clean();
	}
	
	private void clean() {
		if (CameraMESHolder.cameraMES != null) {
			CameraMESHolder.cameraMES.stop();
			CameraMESHolder.cameraMES = null;
		}
		if (technology.equals(Constants.BLUETOOTH_GKA)) {
			Intent stopService = new Intent(this, BluetoothCommunicationService.class);
			stopService.setAction(BluetoothCommunicationService.STOP);
			startService(stopService);
		} else if (technology.equals(Constants.WIFI_GKA)) {
			Intent stopService = new Intent(this, WifiCommunicationService.class);
			stopService.setAction(WifiCommunicationService.STOP);
			startService(stopService);
		}
	}

	public void retrieveKey(View view) {
		Intent result = new Intent();       
		result.putExtra(Constants.KEY, key);
		GKAActivity.this.setResult(Activity.RESULT_OK, result);
		finish();
	}
	
	public void runProtocol(View view) {
		Integer nonceLength = (Integer)nonceLengthsSpinner.getSelectedItem();
		Integer groupKeyLength = (Integer)groupKeyLengthsSpinner.getSelectedItem();
		Integer publicKeyLength = Integer.parseInt(String.valueOf((CharSequence) publicKeyLengthsSpinner.getSelectedItem()));
		RadioButton authRadio = (RadioButton)findViewById(R.id.radio1);
		RadioButton authConfRadio = (RadioButton)findViewById(R.id.radio2);
		
		Intent commServiceIntent = new Intent();
		commServiceIntent.putExtra("nonceLength", nonceLength);
		commServiceIntent.putExtra("groupKeyLength", groupKeyLength);
		commServiceIntent.putExtra("publicKeyLength", publicKeyLength);
		commServiceIntent.putExtra("version", authRadio.isChecked()?1:(authConfRadio.isChecked()?2:0));
		commServiceIntent.putExtra(Constants.RETRIEVE_KEY, getIntent().getBooleanExtra(Constants.RETRIEVE_KEY, false));
		
		if (technology.equals(Constants.WIFI_GKA)) {
			commServiceIntent.setClass(this, WifiCommunicationService.class);
			commServiceIntent.setAction(WifiCommunicationService.GKA_RUN);
			startService(commServiceIntent);
		} else if (technology.equals(Constants.BLUETOOTH_GKA)) {
			commServiceIntent.setClass(this, BluetoothCommunicationService.class);
			commServiceIntent.setAction(BluetoothCommunicationService.GKA_RUN);
			startService(commServiceIntent);
		}
	}
}
