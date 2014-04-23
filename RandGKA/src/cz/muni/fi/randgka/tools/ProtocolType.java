package cz.muni.fi.randgka.tools;

public enum ProtocolType {
	AUGOT((byte)0);
	
	private final byte value;
    private ProtocolType(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }
    
    public static ProtocolType valueOf(byte b) {
    	return ProtocolType.values()[b];
    }
}