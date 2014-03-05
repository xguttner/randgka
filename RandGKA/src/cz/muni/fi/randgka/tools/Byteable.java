package cz.muni.fi.randgka.tools;

/**
 * Interface ensuring the class can be transformed to and from byte array.
 */
public interface Byteable {
	
	/**
	 * @return byte array of given object according to its structure
	 */
	public byte[] getBytes();
	
	/**
	 * @return length in bytes of the resulting byte array
	 */
	public int length();
	
	/**
	 * Constructs object from the given byte array bytes.
	 * 
	 * @param bytes - byte array containing an object
	 */
	public void fromBytes(byte[] bytes);
}
