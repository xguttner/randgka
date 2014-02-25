package cz.muni.fi.randgka.library;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

public class PMessage implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7176163014773441688L;
	
	private MessageAction action;
	private byte roundNo;
	private byte originatorId;
	private int length;
	private byte[] message;
	
	public PMessage() {}
	
	public PMessage(byte[] pMessageInBytes) {
		pMessageInBytes = Base64.decode(pMessageInBytes, Base64.DEFAULT);
		byte[] lengthBytes = new byte[4];
		System.arraycopy(pMessageInBytes, 3, lengthBytes, 0, 4);
		
		action = MessageAction.valueOf(pMessageInBytes[0]);
		
		roundNo = pMessageInBytes[1];
		originatorId = pMessageInBytes[2];
		
		ByteBuffer wrapper = ByteBuffer.wrap(lengthBytes);
		length = wrapper.getInt();
		
		message = new byte[length];
		System.arraycopy(pMessageInBytes, 7, message, 0, length);
	}

	public PMessage(MessageAction action, byte roundNo, byte originatorId, int length, byte[] message) {
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
		
		byte[] rv = new byte[length+7];
		rv[0] = action.getValue();
		rv[1] = roundNo;
		rv[2] = originatorId;
		System.arraycopy(lengthBytes, 0, rv, 3, 4);
		System.arraycopy(message, 0, rv, 7, length);
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

	public byte getOriginatorId() {
		return originatorId;
	}

	public void setOriginatorId(byte originatorId) {
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
	
}
