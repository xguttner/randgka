package cz.muni.fi.randgka.tools;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.util.Arrays;

import android.os.Bundle;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

public class PMessage implements Serializable, Byteable {
	
	private static final long serialVersionUID = 7176163014773441688L;

	private static final int STATIC_LENGTH = 15;
	private static String signAlg = "SHA256withRSA";
	
	private MessageAction action;
	private byte roundNo;
	private int originatorId;
	private int length;
	private int sigLength;
	private boolean signed;
	private byte[] message;
	private byte[] signature;
	
	public PMessage() {
		signed = false;
	}
	
	public PMessage(byte[] pMessageInBytes) {
		signed = false;
		fromBytes(pMessageInBytes);
	}

	public PMessage(MessageAction action, byte roundNo, int originatorId, int length, int sigLength, boolean signed, byte[] message, byte[] signature) {
		this.action = action;
		this.roundNo = roundNo;
		this.originatorId = originatorId;
		this.length = length;
		this.sigLength = sigLength;
		this.signed = signed;
		this.message = message;
		this.signature = signature;
	}

	public Message obtainMessage(Message message) {
		
		Bundle pMessageBundle = new Bundle();
        pMessageBundle.putSerializable("pMessage", this);
		
		message.setData(pMessageBundle);
        
        return message;
	}
	
	private byte[] getBytesNoSignature() {
		byte[] lengthBytes = ByteBuffer.allocate(4).putInt(length).array();
		byte[] sigLengthBytes = ByteBuffer.allocate(4).putInt(sigLength).array();
		
		byte[] rv = new byte[length+STATIC_LENGTH];
		rv[0] = action.getValue();
		rv[1] = roundNo;
		
		byte [] originatorIdBytes = ByteBuffer.allocate(4).putInt(originatorId).array();
		
		System.arraycopy(originatorIdBytes, 0, rv, 2, 4);
		System.arraycopy(lengthBytes, 0, rv, 6, 4);
		System.arraycopy(sigLengthBytes, 0, rv, 10, 4);
		
		rv[14] = (byte)((signed)?1:0);
		
		System.arraycopy(message, 0, rv, STATIC_LENGTH, length);
		
		return rv;
	}
	
	public byte[] getBytes() {
		byte[] rv = new byte[length()];
		byte[] noSig = getBytesNoSignature();
		
		System.arraycopy(noSig, 0, rv, 0, length+STATIC_LENGTH);
		
		if (signed && signature != null) System.arraycopy(signature, 0, rv, length+STATIC_LENGTH, sigLength);
		
		rv = Base64.encode(rv, Base64.DEFAULT);
		
		return rv;
	}
	
	public MessageAction getAction() {
		return action;
	}

	public void setAction(MessageAction action) {
		this.action = action;
	}

	public byte getRoundNo() {
		return roundNo;
	}

	public void setRoundNo(byte roundNo) {
		this.roundNo = roundNo;
	}

	public int getOriginatorId() {
		return originatorId;
	}

	public void setOriginatorId(int originatorId) {
		this.originatorId = originatorId;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}
	
	public int getSigLength() {
		return sigLength;
	}

	public void setSigLength(int sigLength) {
		this.sigLength = sigLength;
	}

	public byte[] getMessage() {
		return message;
	}

	public void setMessage(byte[] message) {
		this.message = message;
	}

	public boolean isSigned() {
		return signed;
	}

	public void setSigned(boolean signed) {
		this.signed = signed;
	}

	public byte[] getSignature() {
		return signature;
	}

	public void setSignature(byte[] signature) {
		this.signature = signature;
	}

	@Override
	public String toString() {
		return "PMessage [action=" + action + ", roundNo=" + roundNo
				+ ", originatorId=" + originatorId + ", length=" + length
				+ ", signed=" + signed + ", message="
				+ Arrays.toString(message) + ", signature="
				+ Arrays.toString(signature) + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((action == null) ? 0 : action.hashCode());
		result = prime * result + length;
		result = prime * result + Arrays.hashCode(message);
		result = prime * result + originatorId;
		result = prime * result + roundNo;
		result = prime * result + Arrays.hashCode(signature);
		result = prime * result + (signed ? 1231 : 1237);
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
		PMessage other = (PMessage) obj;
		if (action != other.action)
			return false;
		if (length != other.length)
			return false;
		if (!Arrays.equals(message, other.message))
			return false;
		if (originatorId != other.originatorId)
			return false;
		if (roundNo != other.roundNo)
			return false;
		if (!Arrays.equals(signature, other.signature))
			return false;
		if (signed != other.signed)
			return false;
		return true;
	}

	@Override
	public int length() {
		return length + STATIC_LENGTH + (signed?sigLength:0);
	}

	@Override
	public void fromBytes(byte[] pMessageInBytes) {
		pMessageInBytes = Base64.decode(pMessageInBytes, Base64.DEFAULT);
		
		action = MessageAction.valueOf(pMessageInBytes[0]);
		roundNo = pMessageInBytes[1];
		
		byte[] originatorIdBytes = new byte[4];
		System.arraycopy(pMessageInBytes, 2, originatorIdBytes, 0, 4);
		originatorId = ByteBuffer.wrap(originatorIdBytes).getInt();
		
		byte[] lengthBytes = new byte[4];
		System.arraycopy(pMessageInBytes, 6, lengthBytes, 0, 4);
		length = ByteBuffer.wrap(lengthBytes).getInt();
		
		byte[] sigLengthBytes = new byte[4];
		System.arraycopy(pMessageInBytes, 10, sigLengthBytes, 0, 4);
		sigLength = ByteBuffer.wrap(sigLengthBytes).getInt();
		
		signed = (pMessageInBytes[14] == (byte)1)?true:false;
		
		message = new byte[length];
		System.arraycopy(pMessageInBytes, 15, message, 0, length);
		
		if (signed) {
			signature = new byte[sigLength];
			System.arraycopy(pMessageInBytes, length+15, signature, 0, sigLength);
		}
		
	}
	
	public void selfSign(PrivateKey privateKey, SecureRandom secureRandom) {
		signed = true;
		if (privateKey != null) {
			try {
				RSAPrivateKey rk = (RSAPrivateKey)privateKey;
				Signature signature = Signature.getInstance(signAlg, "BC");
			    signature.initSign(privateKey, secureRandom);
			    signature.update(getBytesNoSignature());
			    this.signature = signature.sign();
			    
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (NoSuchProviderException e) {
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				e.printStackTrace();
			} catch (SignatureException e) {
				e.printStackTrace();
			}
		}
	}
	
	public boolean selfVerify(PublicKey publicKey) {
		try {
			
			Signature signature = Signature.getInstance(signAlg, "BC");
			signature.initVerify(publicKey);
		    signature.update(getBytesNoSignature());
		    return signature.verify(this.signature);
		    
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (SignatureException e) {
			e.printStackTrace();
		}
		return false;
	}
	
}
