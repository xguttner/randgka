package cz.muni.fi.randgka.provider.random;

import java.security.SecureRandom;

import android.util.Log;
import cz.muni.fi.randgka.library.ByteSequence;
import cz.muni.fi.randgka.library.LengthsNotEqualException;
import cz.muni.fi.randgka.library.MinEntropySource;

public class UHRandExtractor extends SecureRandom implements RandExtractor {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -278388048232903714L;
	
	private static final int minEntropySequenceLength = 839;
	private static final int trueRandomSequenceLength = 629;
	private static final int seedLength = 839;
	private ByteSequence seed;
	
	private MinEntropySource mes;
	
	public void initialize(MinEntropySource mes, ByteSequence seed) {
		this.mes = mes;
		this.seed = seed;
	}
	
	@Override
	public void nextBytes(byte[] bytes) {
		int length = bytes.length*8;
		
		ByteSequence returnSequence = new ByteSequence();
		ByteSequence actualSequence = null;
		ByteSequence sourceSequence = null;
		for (int j = 0; j < (int)(length/trueRandomSequenceLength)+((length%trueRandomSequenceLength > 0)?1:0); j++) {
			sourceSequence = mes.getPreprocessedSourceData(minEntropySequenceLength, null);
			sourceSequence.setBitLength(minEntropySequenceLength-1);
			actualSequence = new ByteSequence(new byte[]{(byte)0x80}, 1);
			actualSequence.add(sourceSequence);
			for (int i = 0; i < trueRandomSequenceLength; i++) {
				Log.d("i", String.valueOf(i));
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
}
