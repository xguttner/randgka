package cz.muni.fi.randgka.bluetoothgka;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import cz.muni.fi.randgka.gka.GKAParticipant;
import cz.muni.fi.randgka.gka.GKAParticipants;

public class BluetoothGKAParticipants extends GKAParticipants implements Serializable {
	
	private static final long serialVersionUID = 8756429984808629417L;
	
	private Map<Integer, BluetoothFeatures> features;
	
	public BluetoothGKAParticipants() {
		super();
		features = new HashMap<Integer, BluetoothFeatures>();
	}
	
	public BluetoothGKAParticipants(byte[] bytes) {
		fromBytes(bytes);
	}
	
	public Collection<BluetoothFeatures> getFeatures() {
		return features.values();
	}
	 
	public void add(GKAParticipant participant, BluetoothFeatures bf) {
		add(participant);
		features.put(participant.getId(), bf);
	}
	
	private void mergeUsingMac(GKAParticipant gkaParticipant, BluetoothFeatures bf) {
		if (features != null) {
			GKAParticipant currentParticipant = null;
			Integer entryToRemoveId = null;
			for (Entry<Integer, BluetoothFeatures> e : features.entrySet()) {
				if (e.getValue().equals(bf)) {
					currentParticipant = getParticipant(e.getKey());
					currentParticipant.setId(gkaParticipant.getId());
					if (currentParticipant.getAuthPublicKey() == null) currentParticipant.setAuthPublicKey(gkaParticipant.getAuthPublicKey());
					entryToRemoveId = e.getKey();
					break;
				}
			}
			if (currentParticipant != null) {
				features.remove(entryToRemoveId);
				features.put(currentParticipant.getId(), bf);
			}
		}
	}
	
	public void mergeUsingMac(BluetoothGKAParticipants participants2) {
		if (participants2 != null && participants2.getParticipants() != null) {
			for (GKAParticipant p : participants2.getParticipants()) {
				mergeUsingMac(p, participants2.getBluetoothFeaturesFor(p.getId()));
			}
		}
	}
	
	public BluetoothFeatures getBluetoothFeaturesFor(Integer gkaParticipantId) {
		return features.get(gkaParticipantId);
	}
	
	@Override
	public byte[] getBytes() {
		byte[] bytes = new byte[length()];
		System.arraycopy(super.getBytes(), 0, bytes, 0, super.length());
		
		int offset = super.length();
		
		if (features != null) {
			for (Entry<Integer, BluetoothFeatures> e : features.entrySet()) {
				System.arraycopy(ByteBuffer.allocate(4).putInt(e.getKey()).array(), 0, bytes, offset, 4);
				offset += 4;
				System.arraycopy(e.getValue().getBytes(), 0, bytes, offset, e.getValue().length());
				offset += e.getValue().length();
			}
		}
		return bytes;
	}
	
	@Override
	public int length() {
		return mapLength() + super.length();
	}
	
	@Override
	public void fromBytes(byte[] bytes) {
		super.fromBytes(bytes);
		
		features = new HashMap<Integer, BluetoothFeatures>();
		int offset = super.length();
		Integer currentId;
		byte[] currentIdBytes = new byte[4];
		BluetoothFeatures currentBF;
		while (offset < bytes.length) {
			System.arraycopy(bytes, offset, currentIdBytes, 0, 4);
			currentId = ByteBuffer.wrap(currentIdBytes).getInt();
			offset += 4;
			byte[] currentBFBytes = new byte[bytes.length - offset];
			System.arraycopy(bytes, offset, currentBFBytes, 0, bytes.length - offset);
			currentBF = new BluetoothFeatures(currentBFBytes);
			offset += currentBF.length();
			features.put(currentId, currentBF);
		}
	}
	
	private int mapLength() {
		int length = 0;
		if (features != null) {
			for (BluetoothFeatures bf : features.values()) {
				length += 4;
				length += bf.length();
			}
		}
		return length;
	}

	@Override
	public String toString() {
		return "BluetoothGKAParticipants [features=" + features
				+ ", participants=" + participants + "]";
	}
}
