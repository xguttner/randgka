package cz.muni.fi.randgka.library;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class GKAParticipants implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5580254176681295329L;
	private List<GKAParticipant> participants;
	
	public GKAParticipants() {
		participants = new ArrayList<GKAParticipant>();
	}
	
	public void add(GKAParticipant participant) {
		participants.add(participant);
	}
	
	public GKAParticipant getMe() {
		for (GKAParticipant p : participants) {
			if (p.isMeFlag()) return p;
		}
		return null;
	}
	
	public GKAParticipant getLeader() {
		for (GKAParticipant p : participants) {
			if (p.getRole().equals(ParticipateRole.LEADER)) return p;
		}
		return null;
	}
	
	public List<GKAParticipant> getParticipants() {
		return participants;
	}
	
	public void mergeParticipant(GKAParticipant ps) {
		boolean contains = false;
		for (GKAParticipant p : participants) {
			if (p.equals(ps)) {
				if (ps.getPublicKey() != null) p.setPublicKey(ps.getPublicKey());
				contains = true;
			}
		}
		if (!contains) add(ps);
	}
	
	public void mergeParticipants(GKAParticipants ps) {
		if (ps != null) {
			for (GKAParticipant p : ps.getParticipants()) {
				mergeParticipant(p);
			}
		}
	}

	public byte[] getBytes() {
		try {
			ByteArrayOutputStream bStream = new ByteArrayOutputStream();
			for (GKAParticipant p : participants) {
					bStream.write(p.getBytes());
					bStream.write(0xff);
			}
			return bStream.toByteArray();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public void fromBytes(byte[] bytes) {
		String bytesStr = new String(bytes);
		String delimiter = ""+(char)0xff;
		String[] participantStrings = bytesStr.split(delimiter);
		if (participantStrings != null) {
			for (String ps : participantStrings) {
				if (ps.length() > 0) {
					GKAParticipant p = new GKAParticipant(ps.getBytes());
					if (p != null) add(p);
					Log.d("pFromBytes", p.toString());
				}
			}
		}
	}

	public int byteLength() {
		int length = 0;
		for (GKAParticipant p : participants) {
			length += p.length();
		}
		return length;
	}
}
