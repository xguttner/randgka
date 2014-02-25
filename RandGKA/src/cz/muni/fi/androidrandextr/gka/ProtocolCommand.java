package cz.muni.fi.androidrandextr.gka;

import android.os.Parcel;
import android.os.Parcelable;

public class ProtocolCommand implements Parcelable {
	private byte id;
	private byte nextId;
	private ProtocolParticipant target;
	private byte[] message;
	
	public ProtocolCommand() {}
	
	public ProtocolCommand(byte id, byte nextId, ProtocolParticipant target, byte[] message) {
		this.id = id;
		this.nextId = nextId;
		this.target = target;
		this.message = message;
	}
	
	public byte getId() {
		return id;
	}
	public void setId(byte id) {
		this.id = id;
	}
	public byte getNextId() {
		return nextId;
	}
	public void setNextId(byte nextId) {
		this.nextId = nextId;
	}
	public ProtocolParticipant getTarget() {
		return target;
	}
	public void setTarget(ProtocolParticipant target) {
		this.target = target;
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
		ProtocolCommand other = (ProtocolCommand) obj;
		if (id != other.id)
			return false;
		return true;
	}
	
	public byte[] toByteArray() {
		byte[] bytes = new byte [1024];
		bytes[0] = id;
		bytes[1] = nextId;
		System.arraycopy(message, 0, bytes, 2, message.length);
		return bytes;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		
	}
}
