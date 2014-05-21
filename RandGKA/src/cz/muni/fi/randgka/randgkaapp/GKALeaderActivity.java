package cz.muni.fi.randgka.randgkaapp;

import cz.muni.fi.randgka.bluetoothgka.BluetoothCommunicationService;
import cz.muni.fi.randgka.tools.Constants;
import cz.muni.fi.randgka.wifigka.WifiCommunicationService;
import cz.muni.fi.randgkaapp.R;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Build;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.Spinner;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class GKALeaderActivity extends Activity {

	private BluetoothAdapter bluetoothAdapter;
	private String technology;
	private boolean freshKey = false,
			wifiEnabled = false;
	
	private Spinner nonceLengthsSpinner,
		groupKeyLengthsSpinner,
		publicKeyLengthsSpinner;
	
	private BroadcastReceiver br;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gkaleader);
		
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
		
		freshKey = getIntent().getBooleanExtra("freshKey", false);
		technology = getIntent().getStringExtra("technology");
		if (technology.equals(Constants.WIFI_GKA)) {
			IntentFilter intentFilter = new IntentFilter();
		    intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		    
		    WifiP2pManager wifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
		    Channel wifiChannel = wifiP2pManager.initialize(this, getMainLooper(), null);
		    
		    br = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(intent.getAction())) {
			            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
			            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
			                wifiEnabled = true;
			            }
			        }
				}
			};
		    registerReceiver(br, intentFilter);
		} else if (technology.equals(Constants.BLUETOOTH_GKA)) {
			bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.empty, menu);
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// for enabling bluetooth - send to MainActivity if fail
		if (requestCode == Constants.REQUEST_ENABLE_BT) {
			if (resultCode == RESULT_CANCELED) {
				Intent moving = new Intent(this, MainActivity.class);
				startActivity(moving);
			}
		}
		
		// for enabling discoverability of bluetooth device
		if (requestCode == Constants.REQUEST_DISCOVERABLE_BT) {
			moveToGKAActivity();
		}
	}
	
	private void moveToGKAActivity() {
		Intent moving = new Intent(this, GKAActivity.class);
		moving.putExtra("isLeader", true);
		moving.putExtra("technology", technology);
		moving.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
		startActivity(moving);
	}
	
	/**
	 * Check, if the bluetooth device is accessible and send request to enable it.
	 */
	private void enableBluetooth() {
		if (bluetoothAdapter == null) {
			Intent moving = new Intent(this, MainActivity.class);
			startActivity(moving);
		}
		if (!bluetoothAdapter.isEnabled()) {
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
		}
	}
	
	public void leaderRun(View view) {
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
		commServiceIntent.putExtra("freshKey", freshKey);
		commServiceIntent.setAction(BluetoothCommunicationService.LEADER_RUN);
		
		commServiceIntent.putExtra(Constants.RETRIEVE_KEY, getIntent().getBooleanExtra(Constants.RETRIEVE_KEY, false));
		if (technology.equals(Constants.BLUETOOTH_GKA)) {
			enableBluetooth();
			
			startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE), Constants.REQUEST_DISCOVERABLE_BT);
			
			commServiceIntent.setClass(this, BluetoothCommunicationService.class);
			startService(commServiceIntent);
		} 
		else if (technology.equals(Constants.WIFI_GKA)) {
			if (wifiEnabled) {
				commServiceIntent.setClass(this, WifiCommunicationService.class);
				
				moveToGKAActivity();
			}
			else {
				Intent moving = new Intent(this, MainActivity.class);
				startActivity(moving);
			}
		}
	
		
	}
	
}
