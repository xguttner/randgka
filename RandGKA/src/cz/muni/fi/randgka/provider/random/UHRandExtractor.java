package cz.muni.fi.randgka.provider.random;

import java.security.SecureRandomSpi;

import android.app.Activity;
import cz.muni.fi.randgka.library.ByteSequence;
import cz.muni.fi.randgka.library.LengthsNotEqualException;
import cz.muni.fi.randgka.provider.minentropy.CameraMES;
import cz.muni.fi.randgka.provider.minentropy.MinEntropySource;

public final class UHRandExtractor extends SecureRandomSpi implements RandExtractor {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2074997483003942974L;

	private MinEntropySource mes;
	
	private static final int minEntropySequenceLength = 839;
	private static final int trueRandomSequenceLength = 629;
	private static final int seedLength = 839;
	private ByteSequence seed;
	
	public UHRandExtractor() {
		this.mes = new CameraMES();
	}
	
	public void initialize(MinEntropySource mes, ByteSequence seed) {
		this.mes = mes;
		this.seed = new ByteSequence(new byte[210], 839);
	}
	
	@Override
	protected byte[] engineGenerateSeed(int numBytes) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void engineNextBytes(byte[] bytes) {
		int length = bytes.length;
		
		ByteSequence returnSequence = new ByteSequence();
		ByteSequence actualSequence = null;
		ByteSequence sourceSequence = null;
		for (int j = 0; j < (int)(length/trueRandomSequenceLength)+((length%trueRandomSequenceLength > 0)?1:0); j++) {
			sourceSequence = mes.getPreprocessedSourceData((int)(minEntropySequenceLength/mes.getBytesPerSample(true) + ((minEntropySequenceLength%mes.getBytesPerSample(true) > 0)?1:0)), null);
			sourceSequence.setBitLength(minEntropySequenceLength-1);
			actualSequence = new ByteSequence(new byte[]{(byte)0x80}, 1);
			actualSequence.add(sourceSequence);
			for (int i = 0; i < trueRandomSequenceLength; i++) {
				seed.rotateBitsLeft();
				try {
					returnSequence.addBit(seed.scalarProduct(actualSequence));
				} catch (LengthsNotEqualException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		System.arraycopy(returnSequence.getSequence(), 0, bytes, 0, bytes.length);
	}

	@Override
	protected void engineSetSeed(byte[] seed) {
		// TODO Auto-generated method stub
		
	}
}
