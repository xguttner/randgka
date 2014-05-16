package cz.muni.fi.randgka.randgkaapp;

import cz.muni.fi.randgka.bluetoothgka.BluetoothCommunicationService;
import cz.muni.fi.randgka.tools.Constants;
import cz.muni.fi.randgkaapp.R;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

public class GKALeaderActivity extends Activity {

	private BluetoothAdapter bluetoothAdapter;
	private static final int REQUEST_ENABLE_BT = 1785, REQUEST_DISCOVERABLE_BT = 1786;
	private String technology;
	private boolean freshKey = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bluetooth_gkaleader);
		
		ArrayAdapter<Integer> nonceLengths = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item);
		nonceLengths.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		nonceLengths.add(32);
		nonceLengths.add(64);
		nonceLengths.add(128);
		Spinner nonceLengthsSpinner = (Spinner)findViewById(R.id.spinner2);
		nonceLengthsSpinner.setAdapter(nonceLengths);
		
		ArrayAdapter<Integer> groupKeyLengths = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item);
		groupKeyLengths.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		groupKeyLengths.add(1024);
		groupKeyLengths.add(2048);
		Spinner groupKeyLengthsSpinner = (Spinner)findViewById(R.id.spinner3);
		groupKeyLengthsSpinner.setAdapter(groupKeyLengths);
		
		ArrayAdapter<Integer> publicKeyLengths = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item);
		publicKeyLengths.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		publicKeyLengths.add(1024);
		publicKeyLengths.add(2048);
		publicKeyLengths.add(4096);
		Spinner publicKeyLengthsSpinner = (Spinner)findViewById(R.id.spinner4);
		publicKeyLengthsSpinner.setAdapter(publicKeyLengths);
		
		freshKey = getIntent().getBooleanExtra("freshKey", false);
		technology = getIntent().getStringExtra("technology");
		if (technology.equals(Constants.WIFI_GKA)) {
			
		} else if (technology.equals(Constants.BLUETOOTH_GKA)) {
			bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			enableBluetooth();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.bluetooth_gkaleader, menu);
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// for enabling bluetooth - send to MainActivity if fail
		if (requestCode == REQUEST_ENABLE_BT) {
			if (resultCode == RESULT_CANCELED) {
				Intent moving = new Intent(this, MainActivity.class);
				startActivity(moving);
			}
		}
		
		// for enabling discoverability of bluetooth device - send to MainActivity if fail
		if (requestCode == REQUEST_DISCOVERABLE_BT) {
			if (resultCode == RESULT_CANCELED) {
				Intent moving = new Intent(this, MainActivity.class);
				startActivity(moving);
			}
			else {
				Intent moving = new Intent(this, GKAActivity.class);
				moving.putExtra("isLeader", true);
				moving.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
				startActivity(moving);
			}
		}
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
		    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
	}
	
	public void leaderRun(View view) {
		Spinner spinner2 = (Spinner)findViewById(R.id.spinner2);
		Integer nonceLength = (Integer)spinner2.getSelectedItem();
		Spinner spinner3 = (Spinner)findViewById(R.id.spinner3);
		Integer groupKeyLength = (Integer)spinner3.getSelectedItem();
		Spinner spinner4 = (Spinner)findViewById(R.id.spinner4);
		Integer publicKeyLength = (Integer)spinner4.getSelectedItem();
		RadioButton authRadio = (RadioButton)findViewById(R.id.radio1);
		RadioButton authConfRadio = (RadioButton)findViewById(R.id.radio2);
		if (technology.equals(Constants.BLUETOOTH_GKA)) {
			// make device discoverable
			startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE), REQUEST_DISCOVERABLE_BT);
			Intent bpsIntent = new Intent(this, BluetoothCommunicationService.class);
			
			bpsIntent.putExtra("nonceLength", nonceLength);
			bpsIntent.putExtra("groupKeyLength", groupKeyLength);
			bpsIntent.putExtra("publicKeyLength", publicKeyLength);
			bpsIntent.putExtra("version", authRadio.isChecked()?1:(authConfRadio.isChecked()?2:0));
			bpsIntent.putExtra("freshKey", freshKey);
			
			bpsIntent.setAction(Constants.SERVER_START);
			
			if (getIntent().getBooleanExtra("return_key", false)) {
				bpsIntent.putExtra("return_key", true);
			}
			else bpsIntent.putExtra("return_key", false);
			
			startService(bpsIntent);
		} else if (technology.equals(Constants.WIFI_GKA)) {
			
		}
	}
	
}
