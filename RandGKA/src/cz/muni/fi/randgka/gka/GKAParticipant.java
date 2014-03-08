package cz.muni.fi.randgka.gka;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import android.util.Base64;
import android.util.Log;
import cz.muni.fi.randgka.tools.Byteable;
import cz.muni.fi.randgka.tools.Constants;

public class GKAParticipant implements Byteable {

	private int id;
	private boolean me;
	private GKAParticipantRole role;
	private BigInteger dhPublickKey;
	private PublicKey authPublicKey;
	
	private static final int STATIC_LENGTH = 6;
	private int publicKeyLength;
	private static final int AUTH_PUBLIC_KEY_LENGTH = 5;
	
	public GKAParticipant() {
		setPublicKeyLength();
	}
	
	public GKAParticipant(int id, boolean me, GKAParticipantRole role, BigInteger dhPublicKey, PublicKey authPublicKey) {
		setPublicKeyLength();
		this.id = id;
		this.me = me;
		this.role = role;
		this.dhPublickKey = dhPublicKey;
		this.authPublicKey = authPublicKey;
	}

	public GKAParticipant(byte[] bytes) {
		setPublicKeyLength();
		fromBytes(bytes);
	}
	
	private void setPublicKeyLength() {
		switch (Constants.PKEY_LENGTH) {
			case 256: 
				publicKeyLength = 86;
				break;
			case 512:
				publicKeyLength = 130;
				break;
			default:
				publicKeyLength = 86;
				break;
		}
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
	
	public BigInteger getDHPublicKey() {
		return dhPublickKey;
	}

	public void setDHPublicKey(BigInteger dhPublicKey) {
		this.dhPublickKey = dhPublicKey;
	}
	
	public PublicKey getAuthPublicKey() {
		return authPublicKey;
	}
	public void setAuthPublicKey(PublicKey authPublicKey) {
		this.authPublicKey = authPublicKey;
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
				+ ", dhPublicKey=" + dhPublickKey + ", publicKey=" + ((authPublicKey!=null)?authPublicKey.toString():"null") + "]";
	}
	
	@Override
	public byte[] getBytes() {
		byte[] bytes = new byte[length()];
		
		byte [] idBytes = ByteBuffer.allocate(4).putInt(id).array();
		System.arraycopy(idBytes, 0, bytes, 0, 4);
		
		bytes[4] = (byte)(me?1:0);
		bytes[5] = role.getValue();
		
		if (dhPublickKey != null) {
			byte[] pcBytes = Base64.encode(dhPublickKey.toByteArray(), Base64.DEFAULT);
			System.arraycopy(pcBytes, 0, bytes, 6, AUTH_PUBLIC_KEY_LENGTH);
		}
		
		if (authPublicKey != null) System.arraycopy(Base64.encode(authPublicKey.getEncoded(), Base64.DEFAULT), 0, bytes, 6+AUTH_PUBLIC_KEY_LENGTH, publicKeyLength);
		
		return bytes;
	}

	@Override
	public int length() {
		return STATIC_LENGTH+AUTH_PUBLIC_KEY_LENGTH+publicKeyLength;
	}

	@Override
	public void fromBytes(byte[] bytes) {
		
		byte [] idBytes = new byte[4];
		System.arraycopy(bytes, 0, idBytes, 0, 4);
		id = ByteBuffer.wrap(idBytes).getInt();
		
		me = (bytes[4]!=0);
		role = GKAParticipantRole.valueOf(bytes[5]);
		
		byte[]pcBytes = new byte[AUTH_PUBLIC_KEY_LENGTH];
		System.arraycopy(bytes, 6, pcBytes, 0, AUTH_PUBLIC_KEY_LENGTH);
		dhPublickKey = new BigInteger(1, pcBytes);
		
		try {
			pcBytes = new byte[publicKeyLength];
			System.arraycopy(bytes, 6+AUTH_PUBLIC_KEY_LENGTH, pcBytes, 0, publicKeyLength);
			Log.d("pcB", Arrays.toString(pcBytes));
			pcBytes = Base64.decode(pcBytes, Base64.DEFAULT);
			authPublicKey = KeyFactory.getInstance("RSA", "BC").generatePublic(new X509EncodedKeySpec(pcBytes));
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		}
	}
}
