package cz.muni.fi.randgka.gka;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import android.util.Base64;
import cz.muni.fi.randgka.tools.Byteable;

public class GKAParticipant implements Byteable {

	private int id;
	private boolean me;
	private GKAParticipantRole role;
	//private Key dhPublicKey;
	private BigInteger publicKey;
	
	private static final int STATIC_LENGTH = 92;
	private static final int PUBLIC_KEY_LENGTH = 86;
	
	public GKAParticipant() {}
	
	public GKAParticipant(int id, boolean me, GKAParticipantRole role/*, Key key*/, BigInteger publicKey) {
		this.id = id;
		this.me = me;
		this.role = role;
		//this.dhPublicKey = key;
		this.publicKey = publicKey;
	}

	public GKAParticipant(byte[] bytes) {
		fromBytes(bytes);
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public boolean isMe() {
		return me;
	}

	public void setMe(boolean me) {
		this.me = me;
	}

	public GKAParticipantRole getRole() {
		return role;
	}

	public void setRole(GKAParticipantRole role) {
		this.role = role;
	}
	
	/*public Key getKey() {
		return dhPublicKey;
	}

	public void setKey(Key key) {
		this.dhPublicKey = key;
	}*/
	
	public BigInteger getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(BigInteger publicKey) {
		this.publicKey = publicKey;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
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
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "GKAParticipant [id=" + id + ", me=" + me + ", role=" + role
				+ ", publicKey=" + publicKey + "]";
	}
	
	@Override
	public byte[] getBytes() {
		byte[] bytes = new byte[STATIC_LENGTH];
		
		byte [] idBytes = ByteBuffer.allocate(4).putInt(id).array();
		System.arraycopy(idBytes, 0, bytes, 0, 4);
		
		bytes[4] = (byte)(me?1:0);
		bytes[5] = role.getValue();
		
		/*if (dhPublicKey != null) {
			byte[] pcBytes = Base64.encode(dhPublicKey.getEncoded(), Base64.DEFAULT);
			System.arraycopy(pcBytes, 0, bytes, 6, PUBLIC_KEY_LENGTH);
		}*/
		if (publicKey != null) {
			byte[] pcBytes = Base64.encode(publicKey.toByteArray(), Base64.DEFAULT);
			System.arraycopy(pcBytes, 0, bytes, 6, PUBLIC_KEY_LENGTH);
		}
		
		return bytes;
	}

	@Override
	public int length() {
		return STATIC_LENGTH;
	}

	@Override
	public void fromBytes(byte[] bytes) {
		
		byte [] idBytes = new byte[4];
		System.arraycopy(bytes, 0, idBytes, 0, 4);
		id = ByteBuffer.wrap(idBytes).getInt();
		
		me = (bytes[4]!=0);
		role = GKAParticipantRole.valueOf(bytes[5]);
		
		byte[]pcBytes = new byte[PUBLIC_KEY_LENGTH];
		System.arraycopy(bytes, 6, pcBytes, 0, PUBLIC_KEY_LENGTH);
		publicKey = new BigInteger(1, pcBytes);
		
		/*byte[]pcBytes = new byte[PUBLIC_KEY_LENGTH];
		System.arraycopy(bytes, 6, pcBytes, 0, PUBLIC_KEY_LENGTH);
		try {
			pcBytes = Base64.decode(pcBytes, Base64.DEFAULT);
			dhPublicKey = KeyFactory.getInstance("RSA", "BC").generatePublic(new X509EncodedKeySpec(pcBytes));
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		}*/
	}
}
