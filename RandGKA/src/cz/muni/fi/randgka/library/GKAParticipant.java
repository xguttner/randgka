package cz.muni.fi.randgka.library;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import android.util.Base64;
import android.util.Log;

public class GKAParticipant implements Serializable, Byteable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8386147338671246190L;

	private boolean meFlag;
	private byte nameLength;
	private String macAddress;
	private ParticipateRole role;
	private CommunicationChannel channel;
	private PublicKey publicKey;
	private String name;
	
	public GKAParticipant() {}

	public GKAParticipant(String name, String macAddress,
			CommunicationChannel channel, ParticipateRole role, PublicKey publicKey, boolean meFlag) {
		super();
		this.name = name;
		this.nameLength = (byte)name.length();
		this.macAddress = macAddress;
		this.channel = channel;
		this.role = role;
		this.publicKey = publicKey;
		this.meFlag = meFlag;
	}
	
	public GKAParticipant(byte[] bytes) {
		this.fromBytes(bytes);
	}
	
	public void fromBytes(byte[] bytes) {
		meFlag = (bytes[0]!=0);
		Log.d("meFlag", (meFlag?"1":"0"));
		nameLength = bytes[1];
		Log.d("nameLength", String.format("%03d", nameLength));
		byte[]maBytes = new byte[17];
		System.arraycopy(bytes, 2, maBytes, 0, 17);
		macAddress = new String(maBytes);
		Log.d("macAddress", macAddress);
		role = ParticipateRole.valueOf(bytes[19]);
		Log.d("role", role.toString());
		channel = null;
		byte[]pcBytes = new byte[86];
		System.arraycopy(bytes, 20, pcBytes, 0, 86);
		try {
			Log.d("pcBytes",Arrays.toString(pcBytes));
			pcBytes = Base64.decode(pcBytes, Base64.DEFAULT);
			Log.d("pcBytes",Arrays.toString(pcBytes));
			publicKey = KeyFactory.getInstance("RSA", "BC").generatePublic(new X509EncodedKeySpec(pcBytes));
			Log.d("publicKey", new String(publicKey.getEncoded()));
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		byte[]nameBytes = new byte[nameLength];
		System.arraycopy(bytes, 106, nameBytes, 0, nameLength);
		name = new String(nameBytes);
		Log.d("name", name);
	}
	
	public byte[] getBytes() {
		byte[] bytes = new byte[106+nameLength];
		
		bytes[0] = (byte)(meFlag?1:0);
		bytes[1] = nameLength;
		System.arraycopy(macAddress.getBytes(), 0, bytes, 2, 17);
		bytes[19] = role.getValue();
		byte[] pcBytes = Base64.encode(publicKey.getEncoded(), Base64.DEFAULT);
		Log.d("pcBytesgb", Arrays.toString(pcBytes));
		if (publicKey != null) System.arraycopy(pcBytes, 0, bytes, 20, 86);
		System.arraycopy(name.getBytes(), 0, bytes, 106, nameLength);
		
		return bytes;
	}
	
	@Override
	public int length() {
		return 106+nameLength;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getMacAddress() {
		return macAddress;
	}

	public void setMacAddress(String macAddress) {
		this.macAddress = macAddress;
	}

	public CommunicationChannel getChannel() {
		return channel;
	}

	public void setChannel(CommunicationChannel channel) {
		this.channel = channel;
	}

	public ParticipateRole getRole() {
		return role;
	}

	public void setRole(ParticipateRole role) {
		this.role = role;
	}

	public boolean isMeFlag() {
		return meFlag;
	}

	public void setMeFlag(boolean meFlag) {
		this.meFlag = meFlag;
	}
	
	public byte getNameLength() {
		return nameLength;
	}

	public void setNameLength(byte nameLength) {
		this.nameLength = nameLength;
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(PublicKey publicKey) {
		this.publicKey = publicKey;
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
		GKAParticipant other = (GKAParticipant) obj;
		if (macAddress == null) {
			if (other.macAddress != null)
				return false;
		} else if (!macAddress.equals(other.macAddress))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "GKAParticipant [name=" + name + ", nameLength="
				+ nameLength + ", macAddress=" + macAddress + ", channel="
				+ channel + ", role=" + role + ", publicKey="
				+ Arrays.toString(((publicKey!=null)?publicKey.getEncoded():null)) + ", meFlag=" + meFlag + "]";
	}
}
