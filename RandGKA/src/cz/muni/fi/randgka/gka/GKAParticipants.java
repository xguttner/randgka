package cz.muni.fi.randgka.gka;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cz.muni.fi.randgka.tools.Byteable;

public class GKAParticipants implements Byteable {

	protected List<GKAParticipant> participants;
	
	public GKAParticipants() {
		participants = new ArrayList<GKAParticipant>();
	}
	
	public GKAParticipant getParticipant(int id) {
		if (participants != null) {
			for (GKAParticipant p : participants) {
				if (p.getId() == id) return p;
			}
		}
		return null;
	}
	
	public int size() {
		return participants.size();
	}
	
	public void add(GKAParticipant gkaParticipant) {
		participants.add(gkaParticipant);
	}
	
	public void remove(int id) {
		if (participants != null) {
			for (GKAParticipant p : participants) {
				if (p.getId() == id) participants.remove(p);
			}
		}
	}
	
	public void merge(GKAParticipants toMerge) {
		if (toMerge != null) {
			for (GKAParticipant p : toMerge.getParticipants()) {
				merge(p);
			}
		}
	}
	
	public void merge(GKAParticipant toMerge) {
		boolean contains = false;
		for (GKAParticipant p : participants) {
			if (p.equals(toMerge)) {
				//if (toMerge.getDHPublicKey() != null) p.setDHPublicKey(toMerge.getDHPublicKey());
				if (toMerge.getAuthPublicKey() != null) p.setAuthPublicKey(toMerge.getAuthPublicKey());
				contains = true;
			}
		}
		if (!contains) add(toMerge);
	}
	
	public GKAParticipant getMe() {
		for (GKAParticipant p : participants) {
			if (p.isMe()) return p;
		}
		return null;
	}
	
	public GKAParticipant getLeader() {
		for (GKAParticipant p : participants) {
			if (p.getRole().equals(GKAParticipantRole.LEADER)) return p;
		}
		return null;
	}
	
	public GKAParticipants getAllButMe() {
		GKAParticipants participants2 = new GKAParticipants();
		if (participants != null) {
			for (GKAParticipant p : participants) {
				if (!p.isMe()) participants2.add(p);
			}
		}
		return participants2;
	}
	
	public byte[] getNonces() {
		if (getLeader()!= null && getLeader().getNonceLen() > 0) {
			int nonceLen = getLeader().getNonceLen();
			byte[] noncesArray = new byte[nonceLen*size()];
			int i = 0;
			for (GKAParticipant p : participants) {
				System.arraycopy(p.getNonce(), 0, noncesArray, i*nonceLen, nonceLen);
			}
			return noncesArray;
		} else return null;
	}
	
	public List<GKAParticipant> getParticipants() {
		return participants;
	}

	public void setParticipants(List<GKAParticipant> participants) {
		this.participants = participants;
	}

	@Override
	public byte[] getBytes() {
		try {
			ByteArrayOutputStream bStream = new ByteArrayOutputStream();
			bStream.write(participants.size());
			for (GKAParticipant p : participants) {
					bStream.write(p.getBytes());
			}
			return bStream.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public int length() {
		int length = 1;
		for (GKAParticipant p : participants) {
			length += p.length();
		}
		return length;
	}

	@Override
	public void fromBytes(byte[] bytes) {
		int offset = 1;
		int i = 0;
		byte participantsSize = bytes[0];
		byte[] currentParticipantBytes = null;
		while (i < participantsSize) {
			currentParticipantBytes = new byte[bytes.length - offset];
			System.arraycopy(bytes, offset, currentParticipantBytes, 0, bytes.length - offset);
			GKAParticipant p = new GKAParticipant(currentParticipantBytes);
			add(p);
			offset += p.length();
			i++;
		}
	}

}
