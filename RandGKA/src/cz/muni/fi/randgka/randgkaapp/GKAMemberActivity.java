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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class GKAMemberActivity extends Activity {

	private ArrayAdapter<BluetoothDeviceToDisplay> devices;
	private Spinner devicesSpinner;
	private boolean discoveryRunning;
	private BluetoothAdapter bluetoothAdapter;
	private BroadcastReceiver discoveryBR;
	
	private String technology;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gkamember);
		
		devicesSpinner = (Spinner)findViewById(R.id.spinner1);
		
		technology = getIntent().getStringExtra("technology");
		
		if (technology.equals(Constants.BLUETOOTH_GKA)) {
			bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			
			if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
				Intent getBackIntent = new Intent(this, GKADecisionActivity.class);
				startActivity(getBackIntent);
			}
			
			IntentFilter deviceFound = new IntentFilter(BluetoothDevice.ACTION_FOUND);
			discoveryBR = new BroadcastReceiver() {
				
				@Override
				public void onReceive(Context context, Intent intent) {
					String action = intent.getAction();
			        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
			            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			            if (devicesSpinner != null) {
			            	if (devices == null) {
			            		devices = new ArrayAdapter<BluetoothDeviceToDisplay>(context, android.R.layout.simple_spinner_item);
			            		devices.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			            	}
			            	if (devices.getPosition(new BluetoothDeviceToDisplay(device)) == -1) devices.add(new BluetoothDeviceToDisplay(device));
			            	devicesSpinner.setAdapter(devices);
			            }
			        }
				}
			};
			registerReceiver(discoveryBR, deviceFound);
			
			devices = new ArrayAdapter<BluetoothDeviceToDisplay>(this, android.R.layout.simple_spinner_item);
			devices.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
			for (BluetoothDevice device : pairedDevices) {
		        devices.add(new BluetoothDeviceToDisplay(device));
		    }
		    devicesSpinner.setAdapter(devices);
		    
		    bluetoothAdapter.startDiscovery();
		    discoveryRunning = true;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.empty, menu);
		return true;
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
	public void connectTo(View view) {
		if (technology.equals(Constants.BLUETOOTH_GKA)) {
			BluetoothDevice bluetoothDevice = ((BluetoothDeviceToDisplay)devicesSpinner.getSelectedItem()).getBluetoothDevice();
			
			// stop discovery
			if (discoveryRunning) {
				discoveryRunning = false;
				bluetoothAdapter.cancelDiscovery();
			}
			
			// connect
			Intent commServiceIntent = new Intent(this, BluetoothCommunicationService.class);
			commServiceIntent.setAction(Constants.MEMBER_RUN);
			commServiceIntent.putExtra("bluetoothDevice", bluetoothDevice);
			commServiceIntent.putExtra(Constants.RETRIEVE_KEY, getIntent().getBooleanExtra(Constants.RETRIEVE_KEY, false));
			startService(commServiceIntent);
		}
	}
 
}
