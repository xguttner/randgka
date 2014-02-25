package cz.muni.fi.randgka.library;

public enum MessageAction {
	BROADCAST_PARTICIPANTS((byte)0), GKA_PROTOCOL((byte)1), AUTH_PROTOCOL((byte)2), ADD_PARTICIPANT((byte)3), START_GKA_PROTOCOL((byte)4);
	
	private final byte value;
    private MessageAction(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }
    
    public static MessageAction valueOf(byte b) {
    	return MessageAction.values()[b];
    }
}
