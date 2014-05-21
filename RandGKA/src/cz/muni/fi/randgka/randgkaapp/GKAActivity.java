package cz.muni.fi.randgka.randgkaapp;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import cz.muni.fi.randgka.bluetoothgka.BluetoothCommunicationService;
import cz.muni.fi.randgka.bluetoothgka.BluetoothFeatures;
import cz.muni.fi.randgka.bluetoothgka.BluetoothGKAParticipants;
import cz.muni.fi.randgka.gka.GKAParticipant;
import cz.muni.fi.randgka.provider.minentropy.CameraMES;
import cz.muni.fi.randgka.provider.minentropy.CameraMESHolder;
import cz.muni.fi.randgka.tools.Constants;
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
import android.widget.Button;
import android.widget.TextView;

public class GKAActivity extends Activity {

	private boolean isLeader = false;
	private Button retrieveButton;
	private byte[] key;
	private String technology;
	
	private TextView protocolParticipantsTV,
		versionTV,
		nonceLengthTV,
		groupKeyLengthTV,
		publicKeyLengthTV,
		gkaTV;
	
	public static final String GET_PARTICIPANTS = "get_participants",
			GET_PARAMS = "get_params",
			PRINT_DEVICE = "print_device",
			RETURN_GKA_KEY = "return_gka_key",
			SHOW_RETRIEVE_KEY = "show_retrieve_key",
			GET_GKA_KEY = "get_gka_key";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gkaprotocol);
		
		retrieveButton = (Button)findViewById(R.id.button2);
		retrieveButton.setVisibility(View.GONE);
		
		versionTV = (TextView)findViewById(R.id.textView15);
		nonceLengthTV = (TextView)findViewById(R.id.textView5);
		groupKeyLengthTV = (TextView)findViewById(R.id.textView8);
		publicKeyLengthTV = (TextView)findViewById(R.id.textView10);
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
					BluetoothGKAParticipants gkaParticipants = (BluetoothGKAParticipants)intent.getSerializableExtra("participants");

					if (gkaParticipants != null) {
						BluetoothFeatures bf = null;
						
						for (GKAParticipant g : gkaParticipants.getParticipants()) {
							bf = gkaParticipants.getBluetoothFeaturesFor(g.getId());
							protocolParticipantsTV.append(""+bf.getName()+" ("+bf.getMacAddress()+")");
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
				else if (intent.getAction().equals(Constants.GET_GKA_KEY)) {
					if (gkaTV != null) {
						gkaTV.setText("");
						key = intent.getByteArrayExtra(Constants.KEY);
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
		
		Button runButton = (Button)findViewById(R.id.button1);
		
		technology = getIntent().getStringExtra("technology");
		
		if (this.getIntent().getBooleanExtra("isLeader", false)) {
			isLeader = true;
			
			Intent paramsIntent = new Intent(this, BluetoothCommunicationService.class);
			paramsIntent.setAction(GET_PARAMS);
			startService(paramsIntent);
			
		} else {
			runButton.setVisibility(View.GONE);
		}
		runButton.setClickable(isLeader);
		
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
		if (CameraMESHolder.cameraMES != null) {
			CameraMESHolder.cameraMES.stop();
			CameraMESHolder.cameraMES = null;
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (CameraMESHolder.cameraMES != null) {
			CameraMESHolder.cameraMES.stop();
			CameraMESHolder.cameraMES = null;
		}
	}

	public void retrieveKey(View view) {
		Intent result = new Intent();       
		result.putExtra(Constants.KEY, key);
		GKAActivity.this.setResult(Activity.RESULT_OK, result);
		finish();
	}
	
	public void runProtocol(View view) {
		if (technology.equals(Constants.WIFI_GKA)) {
			
		} else if (technology.equals(Constants.BLUETOOTH_GKA)) {
			Intent runProtocolIntent = new Intent(this, BluetoothCommunicationService.class);
			runProtocolIntent.setAction(BluetoothCommunicationService.GKA_RUN);
			startService(runProtocolIntent);
		}
	}
}
