package cz.muni.fi.randgka.tools;

import android.bluetooth.BluetoothDevice;

/**
 * Class serving only to a user-friendly print of the Bluetooth device in a Spinner.
 */
public class BluetoothDeviceToDisplay {

	private BluetoothDevice bluetoothDevice;
	
	public BluetoothDeviceToDisplay(BluetoothDevice bluetoothDevice) {
		this.bluetoothDevice = bluetoothDevice;
	}
	
	public BluetoothDevice getBluetoothDevice() {
		return bluetoothDevice;
	}
	
	public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
		this.bluetoothDevice = bluetoothDevice;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((bluetoothDevice == null) ? 0 : bluetoothDevice.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BluetoothDeviceToDisplay other = (BluetoothDeviceToDisplay) obj;
		if (bluetoothDevice == null) {
			if (other.bluetoothDevice != null)
				return false;
		} else if (!bluetoothDevice.equals(other.bluetoothDevice))
			return false;
		return true;
	}

	public String toString() {
		return this.bluetoothDevice.getName()+" - "+this.bluetoothDevice.getAddress();
	}
}
