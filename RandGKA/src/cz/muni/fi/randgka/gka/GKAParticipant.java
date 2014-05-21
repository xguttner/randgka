package cz.muni.fi.randgka.gka;

import java.math.BigInteger;
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

public class GKAParticipant {

	private int id;
	private boolean me;
	private String name;
	private GKAParticipantRole role;
	private byte[] nonce;
	private BigInteger dhPublickKey;
	private PublicKey authPublicKey;
	private int pkLen,
				dhLen,
				nonceLen;
	
	public GKAParticipant() {
	}
	
	public GKAParticipant(int id, String name, boolean me, GKAParticipantRole role, int nonceLen, int pkLen, int dhLen, byte[] nonce, BigInteger dhPublicKey, PublicKey authPublicKey) {
		this.id = id;
		this.name = name;
		this.me = me;
		this.role = role;
		this.nonceLen = nonceLen;
		this.pkLen = pkLen;
		this.dhLen = dhLen;
		this.nonce = nonce;
		this.dhPublickKey = dhPublicKey;
		this.authPublicKey = authPublicKey;
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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
	
	public byte[] getNonce() {
		return nonce;
	}

	public void setNonce(byte[] nonce) {
		this.nonce = nonce;
	}

	public int getNonceLen() {
		return nonceLen;
	}

	public void setNonceLen(int nonceLen) {
		this.nonceLen = nonceLen;
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
				+ ", nonce=" + Arrays.toString(nonce) + ", dhPublickKey="
				+ dhPublickKey + ", authPublicKey=" + authPublicKey
				+ ", pkLen=" + pkLen + ", dhLen=" + dhLen + ", nonceLen="
				+ nonceLen + "]";
	}
	
	public byte[] getTransferObject() {
		byte[] bytes = new byte[getTOLength()];
		
		byte [] intBytes = ByteBuffer.allocate(4).putInt(id).array();
		System.arraycopy(intBytes, 0, bytes, 0, 4);
		
		intBytes = ByteBuffer.allocate(4).putInt(name.length()).array();
		System.arraycopy(intBytes, 0, bytes, 4, 4);
		
		System.arraycopy(name.getBytes(), 0, bytes, 8, name.length());
		
		System.arraycopy(nonce, 0, bytes, 8+name.length(), nonceLen);
		
		if (authPublicKey != null) {
			byte[] pkEnc = Base64.encode(authPublicKey.getEncoded(), Base64.DEFAULT);
			Log.d("pk", Arrays.toString(pkEnc));
			int pkEncLen = pkEnc.length;
			
			intBytes = ByteBuffer.allocate(4).putInt(pkEncLen).array();
			System.arraycopy(intBytes, 0, bytes, 8+name.length()+nonceLen, 4);
	
			System.arraycopy(pkEnc, 0, bytes, 12+name.length()+nonceLen, pkEncLen);
		} else {
			intBytes = ByteBuffer.allocate(4).putInt(0).array();
			System.arraycopy(intBytes, 0, bytes, 8+name.length()+nonceLen, 4);
		}
		
		return bytes;
	}
	
	public int getTOLength() {
		int pkEncLen = (authPublicKey != null)? Base64.encode(authPublicKey.getEncoded(), Base64.DEFAULT).length : 0;
		return 4+4+4+name.length()+nonceLen+pkEncLen;
	}
	
	public void fromTransferObject(byte[] bytes) {
		byte [] intBytes = new byte[4];
		System.arraycopy(bytes, 0, intBytes, 0, 4);
		id = ByteBuffer.wrap(intBytes).getInt();
		Log.d("itn", Arrays.toString(intBytes));
		
		me = false;
		role = id == 0 ? GKAParticipantRole.LEADER : GKAParticipantRole.MEMBER;
		
		System.arraycopy(bytes, 4, intBytes, 0, 4);
		int nameLen = ByteBuffer.wrap(intBytes).getInt();
		Log.d("itn", Arrays.toString(intBytes));
		
		byte[]nameBytes = new byte[nameLen];
		System.arraycopy(bytes, 8, nameBytes, 0, nameLen);
		name = new String(nameBytes);
		
		nonce = new byte[nonceLen];
		System.arraycopy(bytes, 8+nameLen, nonce, 0, nonceLen);
		
		System.arraycopy(bytes, 8+nameLen+nonceLen, intBytes, 0, 4);
		int pkEncLen = ByteBuffer.wrap(intBytes).getInt();
		Log.d("itn", Arrays.toString(intBytes));
		
		try {
		if (pkEncLen > 0) {
			byte[] pcBytes = new byte[pkEncLen];
			System.arraycopy(bytes, 12+nameLen+nonceLen, pcBytes, 0, pkEncLen);
			Log.d("pk", id+" "+name+" "+" "+nonceLen+" "+Arrays.toString(pcBytes));
			authPublicKey = KeyFactory.getInstance("RSA", "BC").generatePublic(new X509EncodedKeySpec(Base64.decode(pcBytes, Base64.DEFAULT)));
		}
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		}
	}
}
