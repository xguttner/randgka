package cz.muni.fi.randgka.tools;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;

import android.os.Bundle;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

public class PMessage implements Serializable, Byteable {
	
	private static final long serialVersionUID = 7176163014773441688L;

	private static final int STATIC_LENGTH = 10;
	
	private MessageAction action;
	private byte roundNo;
	private int originatorId;
	private int length;
	private byte[] message;
	
	public PMessage() {}
	
	public PMessage(byte[] pMessageInBytes) {
		fromBytes(pMessageInBytes);
	}

	public PMessage(MessageAction action, byte roundNo, int originatorId, int length, byte[] message) {
		this.action = action;
		this.roundNo = roundNo;
		this.originatorId = originatorId;
		this.length = length;
		this.message = message;
	}

	public Message obtainMessage(Message message) {
		
		Bundle pMessageBundle = new Bundle();
        pMessageBundle.putSerializable("pMessage", this);
		
		message.setData(pMessageBundle);
        
        return message;
	}
	
	public byte[] getBytes() {
		
		byte[] lengthBytes = ByteBuffer.allocate(4).putInt(length).array();
		
		byte[] rv = new byte[length+STATIC_LENGTH];
		rv[0] = action.getValue();
		rv[1] = roundNo;
		
		byte [] originatorIdBytes = ByteBuffer.allocate(4).putInt(originatorId).array();
		System.arraycopy(originatorIdBytes, 0, rv, 2, 4);
		
		System.arraycopy(lengthBytes, 0, rv, 6, 4);
		System.arraycopy(message, 0, rv, 10, length);
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

	public byte[] getMessage() {
		return message;
	}

	public void setMessage(byte[] message) {
		this.message = message;
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
		return true;
	}

	@Override
	public String toString() {
		return "PMessage [action=" + action + ", roundNo=" + roundNo
				+ ", originatorId=" + originatorId + ", length=" + length
				+ ", message=" + Arrays.toString(message) + "]";
	}

	@Override
	public int length() {
		return length+STATIC_LENGTH;
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
		Log.d("len", length+" "+pMessageInBytes.length);
		message = new byte[length];
		
		System.arraycopy(pMessageInBytes, 10, message, 0, length);
	}
	
}
