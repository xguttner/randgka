package cz.muni.fi.randgka.randgkaapp;

import cz.muni.fi.randgka.tools.BluetoothDeviceToDisplay;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class DiscoveryBroadcastReceiver extends BroadcastReceiver {

	private Spinner spinner;
	private ArrayAdapter<BluetoothDeviceToDisplay> devices;
	
	public DiscoveryBroadcastReceiver(Spinner spinner, ArrayAdapter<BluetoothDeviceToDisplay> devices) {
		this.spinner = spinner;
		this.devices = devices;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (spinner != null) {
            	if (devices == null) {
            		devices = new ArrayAdapter<BluetoothDeviceToDisplay>(context, android.R.layout.simple_spinner_item);
            		devices.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            	}
            	if (devices.getPosition(new BluetoothDeviceToDisplay(device)) == -1) devices.add(new BluetoothDeviceToDisplay(device));
        		spinner.setAdapter(devices);
            }
        }
	}

}
