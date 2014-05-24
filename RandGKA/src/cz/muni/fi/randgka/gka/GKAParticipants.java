package cz.muni.fi.randgka.gka;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.util.Log;

public class GKAParticipants implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3730203057969772914L;
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
		if (toMerge != null) {
			for (GKAParticipant p : participants) {
				if (p.equals(toMerge)) {
					if (toMerge.getAuthPublicKey() != null) p.setAuthPublicKey(toMerge.getAuthPublicKey());
					contains = true;
				}
			}
			if (!contains) add(toMerge);
		}
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
			Collections.sort(participants, new Comparator<GKAParticipant>() {
				@Override
				public int compare(GKAParticipant g0, GKAParticipant g1) {
					return g0.getId() - g1.getId();
				}
			});
			for (GKAParticipant p : participants) {
				System.arraycopy(p.getNonce(), 0, noncesArray, i*nonceLen, nonceLen);
				i++;
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

	public int getTOLength() {
		int length = 0;
		if (participants != null) {
			for (GKAParticipant p : participants) {
				length += p.getTOLength();
			}
		}
		return length+1;
	}
	
	public byte[] getTransferObject() {
		try {
			ByteArrayOutputStream bStream = new ByteArrayOutputStream();
			bStream.write((byte)participants.size());
			for (GKAParticipant p : participants) {
					bStream.write(p.getTransferObject());
			}
			return bStream.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void mergeFromTO(byte[] bytes, int nonceLen) {
		int offset = 1;
		int i = 0;
		byte participantsSize = bytes[0];
		byte[] currentParticipantBytes = null;
		while (i < participantsSize) {
			currentParticipantBytes = new byte[bytes.length - offset];
			System.arraycopy(bytes, offset, currentParticipantBytes, 0, bytes.length - offset);
			GKAParticipant p = new GKAParticipant();
			p.setNonceLen(nonceLen);
			p.fromTransferObject(currentParticipantBytes);
			
			if (participants.contains(p)) {
				GKAParticipant pIn = getParticipant(p.getId());
				Log.d("pin", pIn.toString());
				if (!pIn.isMe()) {
					pIn.setAuthPublicKey(p.getAuthPublicKey());
					pIn.setName(p.getName());
					pIn.setNonce(p.getNonce());
				}
			} else {
				participants.add(p);
			}
			 
			offset += p.getTOLength();
			i++;
		}
	}

	public void initialize(Integer nonceLength, Integer publicKeyLength) {
		if (participants != null) {
			for (GKAParticipant p : participants) {
				p.setNonceLen(nonceLength);
				p.setPkLen(publicKeyLength);
			}
		}
	}
	
}
