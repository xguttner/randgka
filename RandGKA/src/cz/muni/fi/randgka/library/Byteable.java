package cz.muni.fi.randgka.library;

public interface Byteable {
	public byte[] getBytes();
	
	public int length();
	
	public void fromBytes(byte[] bytes);
}
