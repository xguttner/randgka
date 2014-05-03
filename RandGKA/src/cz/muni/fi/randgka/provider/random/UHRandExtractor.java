package cz.muni.fi.randgka.provider.random;

import java.security.SecureRandom;
import java.security.SecureRandomSpi;

import android.util.Log;
import cz.muni.fi.randgka.provider.minentropy.CameraMES;
import cz.muni.fi.randgka.provider.minentropy.CameraMESHolder;
import cz.muni.fi.randgka.provider.minentropy.MinEntropySource;
import cz.muni.fi.randgka.tools.ByteSequence;
import cz.muni.fi.randgka.tools.Constants;
import cz.muni.fi.randgka.tools.LengthsNotEqualException;

/**
 * Implementation of the randomness Carter-Wegman Universal Hash function randomness extractor.
 *
 */
public final class UHRandExtractor extends SecureRandomSpi implements RandExtractor {

	private static final long serialVersionUID = -2074997483003942974L;

	private MinEntropySource mes;
	
	private static final int inputLength = 839;
	private static final int outputLength = Constants.MAX_RE_OUTPUT;
	private ByteSequence seed;
	private byte[]seedArray={(byte)0x9a, (byte)0x2a, (byte)0x21, (byte)0x6e, (byte)0x90, (byte)0xa5, (byte)0xae, (byte)0xa4, (byte)0xb6, (byte)0x49, (byte)0xbb, (byte)0xe5, (byte)0xe2, (byte)0x6e, (byte)0x5c, (byte)0x64,
			 (byte)0xf1, (byte)0x3e, (byte)0xc1, (byte)0x50, (byte)0xd0, (byte)0xc3, (byte)0xc3, (byte)0x1d, (byte)0x27, (byte)0xc7, (byte)0xe1, (byte)0x4e, (byte)0x06, (byte)0xac, (byte)0x35, (byte)0x2b,
			 (byte)0xe0, (byte)0xfd, (byte)0x2a, (byte)0xfd, (byte)0x0c, (byte)0xf0, (byte)0xa9, (byte)0xf1, (byte)0xfb, (byte)0x5c, (byte)0x79, (byte)0x25, (byte)0xcb, (byte)0xb3, (byte)0x25, (byte)0x2a,
			 (byte)0x36, (byte)0xd0, (byte)0x16, (byte)0xd7, (byte)0xbb, (byte)0x5e, (byte)0x17, (byte)0x51, (byte)0x51, (byte)0x67, (byte)0xaa, (byte)0x2d, (byte)0xab, (byte)0x5e, (byte)0xad, (byte)0x5b,
			 (byte)0xc0, (byte)0xbc, (byte)0x81, (byte)0x5a, (byte)0x4d, (byte)0xf1, (byte)0x23, (byte)0x58, (byte)0xea, (byte)0x09, (byte)0xbe, (byte)0x23, (byte)0x5f, (byte)0xae, (byte)0x74, (byte)0x08,
			 (byte)0xb2, (byte)0x0e, (byte)0xce, (byte)0x70, (byte)0xf2, (byte)0x01, (byte)0xb7, (byte)0x95, (byte)0x9a, (byte)0x5b, (byte)0x7a, (byte)0xff, (byte)0x4d, (byte)0xdd, (byte)0x23, (byte)0x33,
			 (byte)0x3f, (byte)0x70, (byte)0x1f, (byte)0x1a, (byte)0x7e, (byte)0x86, (byte)0xdf, (byte)0xb9, (byte)0xd3, (byte)0xe6, (byte)0x5f, (byte)0x3e, (byte)0xdc, (byte)0x75, (byte)0x88, (byte)0x96,
			 (byte)0x9a, (byte)0x74, (byte)0xf2, (byte)0x5d, (byte)0x92, (byte)0x55, (byte)0x45, (byte)0x3f, (byte)0x94, (byte)0xa3, (byte)0x8c, (byte)0x34, (byte)0x23, (byte)0xdd, (byte)0x5d, (byte)0x1a,
			 (byte)0xa6, (byte)0x97, (byte)0x33, (byte)0x1e, (byte)0x01, (byte)0x55, (byte)0xc3, (byte)0x4c, (byte)0xb7, (byte)0x29, (byte)0x1d, (byte)0x59, (byte)0x3a, (byte)0xbb, (byte)0x4e, (byte)0x1a,
			 (byte)0xc2, (byte)0xb3, (byte)0xda, (byte)0x68, (byte)0xe5, (byte)0xfc, (byte)0x3b, (byte)0xc4, (byte)0xc2, (byte)0x0a, (byte)0x55, (byte)0xe4, (byte)0x61, (byte)0xe1, (byte)0xce, (byte)0x42,
			 (byte)0x4c, (byte)0x54, (byte)0x60, (byte)0x37, (byte)0xed, (byte)0x26, (byte)0xf9, (byte)0x7a, (byte)0x31, (byte)0x37, (byte)0x7e, (byte)0x8f, (byte)0xb9, (byte)0x3c, (byte)0x23, (byte)0x9b,
			 (byte)0xf1, (byte)0x8b, (byte)0x3a, (byte)0x51, (byte)0x9b, (byte)0xb6, (byte)0x4c, (byte)0x51, (byte)0x05, (byte)0x28, (byte)0x06, (byte)0x04, (byte)0x14, (byte)0x8f, (byte)0x4c, (byte)0x7d,
			 (byte)0x09, (byte)0xbd, (byte)0x44, (byte)0x81, (byte)0x97, (byte)0x99, (byte)0x07, (byte)0x74, (byte)0x89, (byte)0x8a, (byte)0xcb, (byte)0xcc, (byte)0x70, (byte)0xd7, (byte)0x14, (byte)0x9d,
			 (byte)0x54, (byte)0x59};
	/**
	 * Non-parametric constructor. Initialize new source using CameraMES.
	 * Gain the seed.
	 * 
	 * @throws SecurityException in case of unsuccessful camera initialization.
	 */
	public UHRandExtractor() throws SecurityException {
		mes = CameraMESHolder.cameraMES;
		if (!mes.ready()) throw new SecurityException("Min-entropy source was not successfully initialized.");
		
		seed = new ByteSequence(seedArray, inputLength);
	}
	
	@Override
	protected byte[] engineGenerateSeed(int numBytes) {
		return getBits(numBytes*8);
	}

	@Override
	protected void engineNextBytes(byte[] bytes) {
		System.arraycopy(getBits(bytes.length*8), 0, bytes, 0, bytes.length);
	}

	@Override
	protected void engineSetSeed(byte[] seed) {
		// not needed
	}
	
	/**
	 * @param length - number of the random bits to be returned
	 * @return byte array of random bits of the given length
	 */
	private byte[] getBits(int length) {
		ByteSequence returnSequence = new ByteSequence();
		ByteSequence actualSequence = null;
		ByteSequence sourceSequence = null;
		
		int extractionRounds = (int)Math.ceil((double)length/outputLength);
		
		//SecureRandom sr = new SecureRandom();
		//byte[] sourceSequenceBytes = new byte[105];
		for (int j = 0; j < extractionRounds; j++) {
			sourceSequence = mes.getMinEntropyData(inputLength-1, null);
			
			//sr.nextBytes(sourceSequenceBytes);
			//sourceSequence = new ByteSequence(sourceSequenceBytes, inputLength-1);
			
			actualSequence = new ByteSequence(new byte[]{(byte)0x80}, 1); //set 1 as the first bit
			actualSequence.add(sourceSequence);
			
			for (int i = 0; i < outputLength; i++) {
				seed.rotateBitsLeft();
				try {
					returnSequence.addBit(seed.scalarProduct(actualSequence));
				} catch (LengthsNotEqualException e) {
					e.printStackTrace();
				}
			}
		}
		
		return returnSequence.getSequence();
	}
}
