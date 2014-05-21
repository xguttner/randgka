package cz.muni.fi.randgka.bluetoothgka;

public class BluetoothFeatures {
	
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
}
