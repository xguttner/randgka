package cz.muni.fi.randgka.gka;

/**
 * Class carrying information about the protocol.
 */
public class GKAProtocolParams {
	
	private boolean authenticated; // version of the protocol to use - authenticated / non-authenticated
	private int nonceLength, // length of the nonce to be used
			keyLength; // length of the desired shared key
	
	public GKAProtocolParams() {
		super();
	}
	public GKAProtocolParams(boolean authenticated, int nonceLength,
			int keyLength) {
		super();
		this.authenticated = authenticated;
		this.nonceLength = nonceLength;
		this.keyLength = keyLength;
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
	public int getKeyLength() {
		return keyLength;
	}
	public void setKeyLength(int keyLength) {
		this.keyLength = keyLength;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (authenticated ? 1231 : 1237);
		result = prime * result + keyLength;
		result = prime * result + nonceLength;
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
		if (keyLength != other.keyLength)
			return false;
		if (nonceLength != other.nonceLength)
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "GKAProtocolParams [authenticated=" + authenticated
				+ ", nonceLength=" + nonceLength + ", keyLength=" + keyLength
				+ "]";
	}
}
