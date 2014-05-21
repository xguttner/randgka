package cz.muni.fi.randgka.gka;

import java.util.HashMap;
import java.util.Map;

import cz.muni.fi.randgka.tools.PMessage;

public class GKAProtocolRound {
	
	public static final int PRINT_PARTICIPANTS = 3,
							CONTINUE = 2,
							ERROR = 1,
							SUCCESS = 0;
	
	private Map<GKAParticipant, PMessage> messages;
	private int actionCode;
	
	public GKAProtocolRound() {
		actionCode = CONTINUE;
		messages = new HashMap<GKAParticipant, PMessage>();
	}
	
	public void put(GKAParticipants gkaParticipants, PMessage pMessage) {
		if (gkaParticipants != null) {
			for (GKAParticipant gkaParticipant : gkaParticipants.getParticipants()) {
				messages.put(gkaParticipant, pMessage);
			}
		}
	}
	
	public void put(GKAParticipant gkaParticipant, PMessage pMessage) {
		messages.put(gkaParticipant, pMessage);
	}
	
	public Map<GKAParticipant, PMessage> getMessages() {
		return messages;
	}
	public void setMessages(Map<GKAParticipant, PMessage> messages) {
		this.messages = messages;
	}
	public int getActionCode() {
		return actionCode;
	}
	public void setActionCode(int actionCode) {
		this.actionCode = actionCode;
	}
	
}
