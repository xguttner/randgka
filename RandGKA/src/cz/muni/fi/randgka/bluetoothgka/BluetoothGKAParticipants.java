package cz.muni.fi.randgka.bluetoothgka;

import java.io.Serializable;
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
	public String toString() {
		return "BluetoothGKAParticipants [features=" + features
				+ ", participants=" + participants + "]";
	}
}
