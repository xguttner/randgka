package cz.muni.fi.randgka.gka;

import java.util.HashMap;
import java.util.Map;

import cz.muni.fi.randgka.tools.PMessage;

public class GKAProtocolRound {
	
	public static final int PRINT_PARTICIPANTS = 3, // print listed participants
							CONTINUE = 2, // continue with the protocol
							ERROR = 1, // error encounter
							SUCCESS = 0; // protocol instance successfully established a new key
	
	// messages to sent to the given participants
	private Map<GKAParticipant, PMessage> messages;
	private int actionCode;
	
	public GKAProtocolRound() {
		actionCode = CONTINUE;
		messages = new HashMap<GKAParticipant, PMessage>();
	}
	
	/**
	 * Set new participants x message tuple to send
	 * 
	 * @param gkaParticipants
	 * @param pMessage
	 */
	public void put(GKAParticipants gkaParticipants, PMessage pMessage) {
		if (gkaParticipants != null) {
			for (GKAParticipant gkaParticipant : gkaParticipants.getParticipants()) {
				messages.put(gkaParticipant, pMessage);
			}
		}
	}
	
	/**
	 * Set new participant x message tuple to send
	 * 
	 * @param gkaParticipant
	 * @param pMessage
	 */
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
