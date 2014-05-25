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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

/**
 * A member is offered a list of devices he can connect to. Remark that this 
 * is a list of either paired or discovered devices with the Bluetooth technology, 
 * nothing to say about the presence of our application's open socket. To find out 
 * its current state, a user has to connect to the given device first. When 
 * the two devices have not been paired before by the Android's native pairing 
 * mechanism, the user is asked to do so after pressing the Connect button.
 */
public class GKAMemberActivity extends Activity {

	private ArrayAdapter<BluetoothDeviceToDisplay> devices;
	private Spinner devicesSpinner;
	
	// state of discovery of visible bluetooth devices
	private boolean discoveryRunning,
					retrieveKey;
	
	private BroadcastReceiver discoveryBR;
	
	private BluetoothAdapter bluetoothAdapter;
	
	private LocalBroadcastManager lbm;
	
	public static final String CONNECTED = "connected";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gkamember);
		
		lbm = LocalBroadcastManager.getInstance(this);
		
		devicesSpinner = (Spinner)findViewById(R.id.spinner1);
		
		retrieveKey = getIntent().getBooleanExtra(Constants.RETRIEVE_KEY, false);
		
		// find out the bluetooth state, if disabled - go to the GKADecisionActivity to decide about it
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
			Intent getBackIntent = new Intent(this, GKADecisionActivity.class);
			startActivity(getBackIntent);
		}
		
		// register BroadcastReceiver for the "new bluetooth device found" action
		IntentFilter deviceFound = new IntentFilter();
		deviceFound.addAction(BluetoothDevice.ACTION_FOUND);
		deviceFound.addAction(GKAMemberActivity.CONNECTED);
		discoveryBR = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				Log.d("something", "received");
				// new bluetooth device was found
		        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
		            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		            if (devicesSpinner != null) {
		            	if (devices == null) {
		            		devices = new ArrayAdapter<BluetoothDeviceToDisplay>(context, android.R.layout.simple_spinner_item);
		            		devices.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		            	}
		            	// if not listed - print it
		            	if (devices.getPosition(new BluetoothDeviceToDisplay(device)) == -1) devices.add(new BluetoothDeviceToDisplay(device));
		            	devicesSpinner.setAdapter(devices);
		            }
		        }
		        else if (action.equals(GKAMemberActivity.CONNECTED)) {
		        	Log.d("here", "ami");
		        	Intent moving = new Intent(context, GKAActivity.class);
					moving.putExtra(Constants.RETRIEVE_KEY, retrieveKey);
					moving.putExtra(Constants.TECHNOLOGY, Constants.BLUETOOTH_GKA);
					if (retrieveKey) startActivityForResult(moving, Constants.REQUEST_RETRIEVE_GKA_KEY);
					else startActivity(moving);
		        }
			}
		};
		lbm.registerReceiver(discoveryBR, deviceFound);
		
		// print the already paired devices
		devices = new ArrayAdapter<BluetoothDeviceToDisplay>(this, android.R.layout.simple_spinner_item);
		devices.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
		for (BluetoothDevice device : pairedDevices) {
	        devices.add(new BluetoothDeviceToDisplay(device));
	    }
	    devicesSpinner.setAdapter(devices);
	    
	    // start discovery of bt devices
	    bluetoothAdapter.startDiscovery();
	    discoveryRunning = true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.empty, menu);
		return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		if (requestCode == Constants.REQUEST_RETRIEVE_GKA_KEY) {
			setResult(Activity.RESULT_OK, data);
			finish();
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (discoveryRunning) {
			discoveryRunning = false;
			bluetoothAdapter.cancelDiscovery();
		}
		lbm.unregisterReceiver(discoveryBR);
	}
	
	/**
	 * Connect to the chosen device. Only an Intent for communication service is invoked, where is decided,
	 * if the connection is successful and GKAActivity is activated
	 * @param view
	 */
	public void connectTo(View view) {
		//get the wanted target device to connect to
		BluetoothDevice bluetoothDevice = ((BluetoothDeviceToDisplay)devicesSpinner.getSelectedItem()).getBluetoothDevice();
		
		// stop discovery
		if (discoveryRunning) {
			discoveryRunning = false;
			bluetoothAdapter.cancelDiscovery();
		}
		
		// connect
		Intent commServiceIntent = new Intent(this, BluetoothCommunicationService.class);
		commServiceIntent.setAction(Constants.MEMBER_RUN);
		commServiceIntent.putExtra(Constants.DEVICE, bluetoothDevice);
		commServiceIntent.putExtra(Constants.RETRIEVE_KEY, retrieveKey);
		startService(commServiceIntent);
	}
 
}
