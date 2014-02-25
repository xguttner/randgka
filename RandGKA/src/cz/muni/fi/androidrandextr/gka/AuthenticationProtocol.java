package cz.muni.fi.androidrandextr.gka;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import cz.muni.fi.randgka.library.PublicKeyCryptography;
import android.content.Context;

public class AuthenticationProtocol implements AuthenticationProtocolInterface {

	private PublicKeyCryptography pkc;
	private static final byte[] ACKNOWLEDGEMENT = {(byte)0x01};
	
	public AuthenticationProtocol(Context context) {
		pkc = new PublicKeyCryptography(context);
	}
	
	@Override
	public byte[] getPublicKey() {
		return pkc.getPrivateKeyAsString().getBytes();
	}

	@Override
	public byte[] getAcknowledgement() {
		return ACKNOWLEDGEMENT;
	}

	@Override
	public byte[] getPublicKeyHash() {
		return getHash(getPublicKey());
	}

	@Override
	public byte[] getHash(byte[] hashedValue) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			return md.digest(hashedValue);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

}
