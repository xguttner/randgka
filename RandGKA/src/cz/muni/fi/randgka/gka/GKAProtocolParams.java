package cz.muni.fi.randgka.gka;

import java.security.PrivateKey;

/**
 * Class carrying information about the protocol.
 */
public class GKAProtocolParams {
	
	private boolean authenticated; // version of the protocol to use - authenticated / non-authenticated
	private int nonceLength, // length of the nonce to be used
			publicKeyLength, 
			groupKeyLength;
	private PrivateKey privateKey;
	
	public GKAProtocolParams() {
		super();
	}
	public GKAProtocolParams(boolean authenticated, int nonceLength,
			int groupKeyLength, int publicKeyLength, PrivateKey privateKey) {
		super();
		this.authenticated = authenticated;
		this.nonceLength = nonceLength;
		this.groupKeyLength = groupKeyLength;
		this.publicKeyLength = publicKeyLength;
		this.privateKey = privateKey;
	}
	public boolean isAuthenticated() {
		return authenticated;
	}
	public void setAuthenticated(boolean authenticated) {
		this.authenticated = authenticated;
	}
	public int getNonceLength() {
		return nonceLength;
	}
	public void setNonceLength(int nonceLength) {
		this.nonceLength = nonceLength;
	}
	public int getGroupKeyLength() {
		return groupKeyLength;
	}
	public void setGroupKeyLength(int keyLength) {
		this.groupKeyLength = keyLength;
	}
	public int getPublicKeyLength() {
		return publicKeyLength;
	}
	public void setPublicKeyLength(int publicKeyLength) {
		this.publicKeyLength = publicKeyLength;
	}
	public PrivateKey getPrivateKey() {
		return privateKey;
	}
	public void setPrivateKey(PrivateKey privateKey) {
		this.privateKey = privateKey;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (authenticated ? 1231 : 1237);
		result = prime * result + groupKeyLength;
		result = prime * result + nonceLength;
		result = prime * result
				+ ((privateKey == null) ? 0 : privateKey.hashCode());
		result = prime * result + publicKeyLength;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GKAProtocolParams other = (GKAProtocolParams) obj;
		if (authenticated != other.authenticated)
			return false;
		if (groupKeyLength != other.groupKeyLength)
			return false;
		if (nonceLength != other.nonceLength)
			return false;
		if (publicKeyLength != other.publicKeyLength)
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "GKAProtocolParams [authenticated=" + authenticated
				+ ", nonceLength=" + nonceLength + ", publicKeyLength="
				+ publicKeyLength + ", groupKeyLength=" + groupKeyLength
				+ ", privateKey=" + privateKey + "]";
	}
	
}
