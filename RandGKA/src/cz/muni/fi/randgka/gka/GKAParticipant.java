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
	private int pkLen,
				dhLen;
	
	private static final int STATIC_LENGTH = 14;
	
	public GKAParticipant() {
	}
	
	public GKAParticipant(int id, boolean me, GKAParticipantRole role, int pkLen, int dhLen, BigInteger dhPublicKey, PublicKey authPublicKey) {
		this.id = id;
		this.me = me;
		this.role = role;
		this.pkLen = pkLen;
		this.dhLen = dhLen;
		this.dhPublickKey = dhPublicKey;
		this.authPublicKey = authPublicKey;
	}

	public GKAParticipant(byte[] bytes) {
		fromBytes(bytes);
	}
	
	private int transPKLen(int pkLen) {
		switch (pkLen) {
			case 32: 
				pkLen = 86;
				break;
			case 64:
				pkLen = 130;
				break;
			case 128:
				pkLen = 218;
				break;
			case 2048:
				pkLen = 394;
				break;
			case 4096:
				pkLen = 746;
				break;
			default:
				pkLen = 86;
				break;
		}
		return pkLen;
	}
	
	private int transDHLen(int dhLen) {
		return 0;
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
	
	public BigInteger getDhPublickKey() {
		return dhPublickKey;
	}

	public void setDhPublickKey(BigInteger dhPublickKey) {
		this.dhPublickKey = dhPublickKey;
	}

	public int getPkLen() {
		return pkLen;
	}

	public void setPkLen(int pkLen) {
		this.pkLen = pkLen;
	}

	public int getDhLen() {
		return dhLen;
	}

	public void setDhLen(int dhLen) {
		this.dhLen = 0;
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
		Log.d("set", Arrays.toString(Base64.encode(authPublicKey.getEncoded(), Base64.DEFAULT)));
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
		
		byte [] intBytes = ByteBuffer.allocate(4).putInt(id).array();
		System.arraycopy(intBytes, 0, bytes, 0, 4);
		
		bytes[4] = (byte)(me?1:0);
		bytes[5] = role.getValue();
		
		intBytes = ByteBuffer.allocate(4).putInt(dhLen).array();
		System.arraycopy(intBytes, 0, bytes, 6, 4);
		
		intBytes = ByteBuffer.allocate(4).putInt(pkLen).array();
		System.arraycopy(intBytes, 0, bytes, 10, 4);
		
		if (dhPublickKey != null) {
			byte[] pcBytes = Base64.encode(dhPublickKey.toByteArray(), Base64.DEFAULT);
			System.arraycopy(pcBytes, 0, bytes, STATIC_LENGTH, transDHLen(dhLen));
		}
		Log.d("len", pkLen+" "+Base64.encode(authPublicKey.getEncoded(), Base64.DEFAULT).length);
		if (authPublicKey != null) System.arraycopy(Base64.encode(authPublicKey.getEncoded(), Base64.DEFAULT), 0, bytes, STATIC_LENGTH+transDHLen(dhLen), transPKLen(pkLen));
		
		return bytes;
	}

	@Override
	public int length() {
		return STATIC_LENGTH+transDHLen(dhLen)+transPKLen(pkLen);
	}

	@Override
	public void fromBytes(byte[] bytes) {
		
		byte [] intBytes = new byte[4];
		System.arraycopy(bytes, 0, intBytes, 0, 4);
		id = ByteBuffer.wrap(intBytes).getInt();
		
		me = (bytes[4]!=0);
		role = GKAParticipantRole.valueOf(bytes[5]);
		
		System.arraycopy(bytes, 6, intBytes, 0, 4);
		dhLen = ByteBuffer.wrap(intBytes).getInt();
		
		System.arraycopy(bytes, 10, intBytes, 0, 4);
		pkLen = ByteBuffer.wrap(intBytes).getInt();
		
		byte[]pcBytes = new byte[transDHLen(dhLen)];
		System.arraycopy(bytes, 14, pcBytes, 0, transDHLen(dhLen));
		dhPublickKey = new BigInteger(1, pcBytes);
		
		try {
			pcBytes = new byte[transPKLen(pkLen)];
			System.arraycopy(bytes, STATIC_LENGTH+transDHLen(dhLen), pcBytes, 0, transPKLen(pkLen));
			Log.d("from", pkLen+" "+Arrays.toString(pcBytes));
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
