package cz.muni.fi.randgka.gka;

public enum GKAParticipantRole {
	LEADER((byte)0), MEMBER((byte)1);
	
	private final byte value;
    private GKAParticipantRole(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }
    
    public static GKAParticipantRole valueOf(byte b) {
    	return GKAParticipantRole.values()[b];
    }
}
