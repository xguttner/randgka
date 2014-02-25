package cz.muni.fi.randgka.library;

public enum ParticipateRole {
	LEADER((byte)0), MEMBER((byte)1);
	
	private final byte value;
    private ParticipateRole(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }
    
    public static ParticipateRole valueOf(byte b) {
    	return ParticipateRole.values()[b];
    }
}
