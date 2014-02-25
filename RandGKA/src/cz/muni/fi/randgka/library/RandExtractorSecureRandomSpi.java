package cz.muni.fi.randgka.library;

import java.security.SecureRandomSpi;

public class RandExtractorSecureRandomSpi extends SecureRandomSpi {

	@Override
	protected byte[] engineGenerateSeed(int numBytes) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void engineNextBytes(byte[] bytes) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void engineSetSeed(byte[] seed) {
		// TODO Auto-generated method stub
		
	}

}
