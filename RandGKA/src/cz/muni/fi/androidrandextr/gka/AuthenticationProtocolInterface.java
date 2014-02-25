package cz.muni.fi.androidrandextr.gka;

public interface AuthenticationProtocolInterface {
	
	public byte[] getPublicKey();
	
	public byte[] getAcknowledgement();
	
	public byte[] getPublicKeyHash();
	
	public byte[] getHash(byte[] hashedValue);
}
