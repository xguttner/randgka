package cz.muni.fi.randgka.randgkaapp;

import java.util.Set;

import cz.muni.fi.randgka.bluetoothgka.BluetoothCommunicationService;
import cz.muni.fi.randgka.tools.BluetoothDeviceToDisplay;
import cz.muni.fi.randgka.tools.Constants;
import cz.muni.fi.randgkaapp.R;
import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class BluetoothGKAMemberActivity extends Activity {

	private ArrayAdapter<BluetoothDeviceToDisplay> devices;
	private Spinner spinner;
	private boolean discoveryRunning;
	private BluetoothAdapter bluetoothAdapter;
	private DiscoveryBroadcastReceiver discoveryBR;
	private static final int REQUEST_ENABLE_BT = 1785;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bluetooth_gkamember);
		
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		enableBluetooth();
		
		spinner = (Spinner)findViewById(R.id.spinner1);
		devices = new ArrayAdapter<BluetoothDeviceToDisplay>(this, android.R.layout.simple_spinner_item);
		devices.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
		for (BluetoothDevice device : pairedDevices) {
	        // Add the name and address to an array adapter to show in a ListView
	        devices.add(new BluetoothDeviceToDisplay(device));
	    }
	    spinner.setAdapter(devices);
	    
	    IntentFilter deviceFound = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		discoveryBR = new DiscoveryBroadcastReceiver((Spinner)findViewById(R.id.spinner1), devices);
		registerReceiver(discoveryBR, deviceFound);
		
		discoveryRunning = true;
		bluetoothAdapter.startDiscovery();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.bluetooth_gkamember, menu);
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
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		 
		if (discoveryRunning) {
			discoveryRunning = false;
			bluetoothAdapter.cancelDiscovery();
		}
		unregisterReceiver(discoveryBR);
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
	
	public void connectTo(View view) {
		Spinner spinner = (Spinner)findViewById(R.id.spinner1);
		
		BluetoothDevice bluetoothDevice = ((BluetoothDeviceToDisplay)spinner.getSelectedItem()).getBluetoothDevice();
		
		// stop discovery
		if (discoveryRunning) {
			discoveryRunning = false;
			bluetoothAdapter.cancelDiscovery();
		}
		
		// connect
		Intent bpsIntent = new Intent(this, BluetoothCommunicationService.class);
		bpsIntent.setAction(Constants.CONNECT_TO_DEVICE);
		bpsIntent.putExtra("bluetoothDevice", bluetoothDevice);
		
		if (getIntent().getBooleanExtra("return_key", false)) {
			bpsIntent.putExtra("return_key", true);
		}
		
		startService(bpsIntent);
		
		Intent moving = new Intent(this, BluetoothGKAActivity.class);
		startActivity(moving);
	}
 
}
