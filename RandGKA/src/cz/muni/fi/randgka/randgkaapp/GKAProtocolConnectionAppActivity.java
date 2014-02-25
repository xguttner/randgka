package cz.muni.fi.randgka.randgkaapp;

import java.util.Set;

import cz.muni.fi.randgka.library.Constants;
import cz.muni.fi.randgka.library.DiscoveryBroadcastReceiver;
import cz.muni.fi.randgka.library.ProtocolBroadcastReceiver;
import cz.muni.fi.randgka.randgkamiddle.ConnectionService;
import cz.muni.fi.randgkaapp.R;
import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class GKAProtocolConnectionAppActivity extends Activity {

	private BluetoothAdapter bluetoothAdapter;
	private boolean discoveryRunning = false;
	private DiscoveryBroadcastReceiver discoveryBR;
	private ProtocolBroadcastReceiver protocolBR;
	//private BluetoothCommunication bluetoothCommunication;
	private static final int REQUEST_ENABLE_BT = 1785, REQUEST_DISCOVERABLE_BT = 1786;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bluetooth_gka);
		
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		enableBluetooth();
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
				Intent moving = new Intent(this, GKAProtocolStartAppActivity.class);
				startActivity(moving);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.bluetooth_gka, menu);
		return true;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		 
		if (discoveryRunning) bluetoothAdapter.cancelDiscovery();
		unregisterReceiver(discoveryBR);
	}
	
	public void actAsClient(View view) {
		IntentFilter deviceFound = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		if (discoveryBR == null) discoveryBR = new DiscoveryBroadcastReceiver((Spinner)findViewById(R.id.spinner1));
		registerReceiver(discoveryBR, deviceFound);
		
		discoveryRunning = true;
		bluetoothAdapter.startDiscovery();
		
		printPairedDevices(); //TODO think about better place to put this refresh - this way only the clients can see the paired devices
	}
	
	public void actAsServer(View view) {
		// stop discovery
		if (discoveryRunning) bluetoothAdapter.cancelDiscovery();
		
		// make device discoverable
		startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE), REQUEST_DISCOVERABLE_BT);
	
		Intent bpsIntent = new Intent(this, ConnectionService.class);
		bpsIntent.setAction(Constants.SERVER_START);
		startService(bpsIntent);
		/*try {
			bluetoothCommunication.actAsServer(view.getContext(), bluetoothAdapter);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		
	}
	
	public void connectToDiscovered(View view) {
		Spinner spinner = (Spinner)findViewById(R.id.spinner1);
		BluetoothDevice bluetoothDevice = (BluetoothDevice)spinner.getSelectedItem();
		
		connectTo(bluetoothDevice);
	}
	
	public void connectToPaired(View view) {
		Spinner spinner = (Spinner)findViewById(R.id.spinner2);
		BluetoothDevice bluetoothDevice = (BluetoothDevice)spinner.getSelectedItem();
		
		connectTo(bluetoothDevice);
	}
	
	private void connectTo(BluetoothDevice bluetoothDevice) {
		// stop discovery
		if (discoveryRunning) bluetoothAdapter.cancelDiscovery();
		
		// connect
		//bluetoothCommunication.connectToDevice(bluetoothDevice, view.getContext());
		Intent bpsIntent = new Intent(this, ConnectionService.class);
		bpsIntent.setAction(Constants.CONNECT_TO_DEVICE);
		bpsIntent.putExtra("bluetoothDevice", bluetoothDevice);
		startService(bpsIntent);
		
		Intent moving = new Intent(this, GKAProtocolStartAppActivity.class);
		startActivity(moving);
	}
	
	private void printPairedDevices() {
		Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
		// If there are paired devices
		if (pairedDevices.size() > 0) {
			Spinner spinnerPaired = (Spinner)findViewById(R.id.spinner2);
			ArrayAdapter<BluetoothDevice> pairedDevicesArray = new ArrayAdapter<BluetoothDevice>(this, android.R.layout.simple_spinner_item);
			
            pairedDevicesArray.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        		
		    // Loop through paired devices
		    for (BluetoothDevice device : pairedDevices) {
		        // Add the name and address to an array adapter to show in a ListView
		        pairedDevicesArray.add(device);
		    }
		    spinnerPaired.setAdapter(pairedDevicesArray);
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
}
