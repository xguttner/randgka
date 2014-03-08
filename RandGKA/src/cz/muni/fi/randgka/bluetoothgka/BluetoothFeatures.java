package cz.muni.fi.randgka.bluetoothgka;

import cz.muni.fi.randgka.tools.Byteable;

public class BluetoothFeatures implements Byteable {
	
	private static final int MAC_ADDRESS_LENGTH = 17;
	
	private String macAddress;
	private byte nameLength;
	private String name;
	
	public BluetoothFeatures() {}
	
	public BluetoothFeatures(String macAddress, String name) {
		super();
		this.macAddress = macAddress;
		this.nameLength = (byte)name.length();
		this.name = name;
	}

	public BluetoothFeatures(byte[] bytes) {
		fromBytes(bytes);
	}
	
	public String getMacAddress() {
		return macAddress;
	}
	public void setMacAddress(String macAddress) {
		this.macAddress = macAddress;
	}
	public byte getNameLength() {
		return nameLength;
	}
	public void setNameLength(byte nameLength) {
		this.nameLength = nameLength;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((macAddress == null) ? 0 : macAddress.hashCode());
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
		BluetoothFeatures other = (BluetoothFeatures) obj;
		if (macAddress == null) {
			if (other.macAddress != null)
				return false;
		} else if (!macAddress.equals(other.macAddress))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "BluetoothGKAParticipantFeatures [macAddress=" + macAddress
				+ ", nameLength=" + nameLength + ", name=" + name + "]";
	}

	@Override
	public byte[] getBytes() {
		byte[] bytes = new byte[length()];
		
		System.arraycopy(macAddress.getBytes(), 0, bytes, 0, MAC_ADDRESS_LENGTH);
		bytes[17] = nameLength;
		
		System.arraycopy(name.getBytes(), 0, bytes, 18, nameLength);
		
		return bytes;
	}

	@Override
	public int length() {
		return MAC_ADDRESS_LENGTH + nameLength + 1;
	}

	@Override
	public void fromBytes(byte[] bytes) {
		byte[] macBytes = new byte[MAC_ADDRESS_LENGTH];
		System.arraycopy(bytes, 0, macBytes, 0, MAC_ADDRESS_LENGTH);
		macAddress = new String(macBytes);
		
		nameLength = bytes[17];
			
		byte[] nameBytes = new byte[nameLength];
		System.arraycopy(bytes, 18, nameBytes, 0, nameLength);
		name = new String(nameBytes);
	}
}
