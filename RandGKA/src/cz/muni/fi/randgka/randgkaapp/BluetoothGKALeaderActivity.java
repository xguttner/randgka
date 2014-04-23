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

public class BluetoothGKALeaderActivity extends Activity {

	private BluetoothAdapter bluetoothAdapter;
	private static final int REQUEST_ENABLE_BT= 1785, REQUEST_DISCOVERABLE_BT = 1786;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bluetooth_gkaleader);
		
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		enableBluetooth();
		
		ArrayAdapter<String> protocols = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
		protocols.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		protocols.add("Augot");
		Spinner protocolsSpinner = (Spinner)findViewById(R.id.spinner1);
		protocolsSpinner.setAdapter(protocols);
		
		ArrayAdapter<Integer> nonceLengths = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item);
		nonceLengths.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		nonceLengths.add(256);
		nonceLengths.add(512);
		nonceLengths.add(1024);
		Spinner nonceLengthsSpinner = (Spinner)findViewById(R.id.spinner2);
		nonceLengthsSpinner.setAdapter(nonceLengths);
		
		ArrayAdapter<Integer> groupKeyLengths = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item);
		groupKeyLengths.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		groupKeyLengths.add(1024);
		groupKeyLengths.add(2048);
		groupKeyLengths.add(4096);
		Spinner groupKeyLengthsSpinner = (Spinner)findViewById(R.id.spinner3);
		groupKeyLengthsSpinner.setAdapter(groupKeyLengths);
		
		ArrayAdapter<Integer> publicKeyLengths = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item);
		publicKeyLengths.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		publicKeyLengths.add(1024);
		publicKeyLengths.add(2048);
		publicKeyLengths.add(4096);
		Spinner publicKeyLengthsSpinner = (Spinner)findViewById(R.id.spinner4);
		publicKeyLengthsSpinner.setAdapter(publicKeyLengths);
		
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
				Intent moving = new Intent(this, BluetoothGKAActivity.class);
				moving.putExtra("isLeader", true);
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
		// make device discoverable
		startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE), REQUEST_DISCOVERABLE_BT);
		Intent bpsIntent = new Intent(this, BluetoothCommunicationService.class);
		
		Spinner spinner1 = (Spinner)findViewById(R.id.spinner1);
		String protocol = (String)spinner1.getSelectedItem();
		bpsIntent.putExtra("protocol", protocol);
		
		Spinner spinner2 = (Spinner)findViewById(R.id.spinner2);
		Integer nonceLength = (Integer)spinner2.getSelectedItem();
		bpsIntent.putExtra("nonceLength", nonceLength);
		
		Spinner spinner3 = (Spinner)findViewById(R.id.spinner3);
		Integer groupKeyLength = (Integer)spinner3.getSelectedItem();
		bpsIntent.putExtra("groupKeyLength", groupKeyLength);
		
		Spinner spinner4 = (Spinner)findViewById(R.id.spinner4);
		Integer publicKeyLength = (Integer)spinner4.getSelectedItem();
		bpsIntent.putExtra("publicKeyLength", publicKeyLength);
		
		RadioButton rb = (RadioButton)findViewById(R.id.radio0);
		bpsIntent.putExtra("isAuth", rb.isChecked());
		
		bpsIntent.setAction(Constants.SERVER_START);
		startService(bpsIntent);
	}
	
}
