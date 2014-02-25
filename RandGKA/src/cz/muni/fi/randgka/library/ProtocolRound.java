package cz.muni.fi.randgka.library;

import java.util.List;

public class ProtocolRound {
	private List<GKAParticipant> targets;
	private PMessage message;
	private boolean keyEstablished;
	
	public List<GKAParticipant> getTargets() {
		return targets;
	}
	
	public void setTargets(List<GKAParticipant> targets) {
		this.targets = targets;
	}
	
	public PMessage getMessage() {
		return message;
	}
	
	public void setMessage(PMessage message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "ProtocolRound [targets=" + targets + ", message=" + message
				+ "]";
	}

	public boolean isKeyEstablished() {
		return keyEstablished;
	}

	public void setKeyEstablished(boolean keyEstablished) {
		this.keyEstablished = keyEstablished;
	}
	
}
