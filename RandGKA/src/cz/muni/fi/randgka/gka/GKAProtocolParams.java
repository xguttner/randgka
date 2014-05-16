package cz.muni.fi.randgka.gka;

import java.io.InputStream;
import java.security.PrivateKey;

/**
 * Class carrying information about the protocol.
 */
public class GKAProtocolParams {
	
	public static final int NON_AUTH = 0,
							AUTH = 1,
							AUTH_CONF = 2;
	
	private int version; // version of the protocol to use - authenticated / non-authenticated
	private int nonceLength, // length of the nonce to be used
			publicKeyLength, 
			groupKeyLength;
	private PrivateKey privateKey;
	private InputStream modpFile;
	
	public GKAProtocolParams() {
		super();
	}
	public GKAProtocolParams(int version, int nonceLength,
			int groupKeyLength, int publicKeyLength, InputStream modpFile, PrivateKey privateKey) {
		super();
		this.version = version;
		this.nonceLength = nonceLength;
		this.groupKeyLength = groupKeyLength;
		this.publicKeyLength = publicKeyLength;
		this.modpFile = modpFile;
		this.privateKey = privateKey;
	}
	public int getVersion() {
		return version;
	}
	public void setVersion(int version) {
		this.version = version;
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
	public InputStream getModpFile() {
		return modpFile;
	}
	public void setModpFile(InputStream modpFile) {
		this.modpFile = modpFile;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + version;
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
		if (version != other.version)
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
		return "GKAProtocolParams [version=" + version
				+ ", nonceLength=" + nonceLength + ", publicKeyLength="
				+ publicKeyLength + ", groupKeyLength=" + groupKeyLength
				+ ", privateKey=" + privateKey + "]";
	}
	
}
