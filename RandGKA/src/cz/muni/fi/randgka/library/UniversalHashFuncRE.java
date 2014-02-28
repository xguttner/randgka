package cz.muni.fi.randgka.library;

import java.io.DataInputStream;
import java.io.IOException;

import android.util.Log;
import cz.muni.fi.randgka.library.ByteSequence;
import cz.muni.fi.randgka.library.LengthsNotEqualException;
import cz.muni.fi.randgka.provider.minentropy.MinEntropySource;

public class UniversalHashFuncRE implements RandExtractor {

	private MinEntropySource randSource;
	private ByteSequence seed;
	private int minEntropySequenceLength = 839;
	private int trueRandomSequenceLength = 629;
	private int seedLength = 839;

	
	public void initialize() {
	}
	
	@Override
	public void initialize(int defaultLength, MinEntropySource source, DataInputStream dis) throws IOException {
		//p = 839, n = 838, e = 84, m = 629
		//minEntropySequenceLength = 839;
		//trueRandomSequenceLength = 629;
		
		this.randSource = source;
		byte[] seedData = new byte[minEntropySequenceLength];
		for (int i = 0; i < minEntropySequenceLength; i++) {
			seedData[i] = (byte)(0xff & dis.read());
		}
		this.seed = new ByteSequence(seedData, minEntropySequenceLength);
	}

	public ByteSequence extractRandomness(int length) throws LengthsNotEqualException, IOException {
		ByteSequence returnSequence = new ByteSequence();
		ByteSequence actualSequence = null;
		ByteSequence sourceSequence = null;
		for (int j = 0; j < (int)(length/trueRandomSequenceLength)+((length%trueRandomSequenceLength > 0)?1:0); j++) {
			sourceSequence = this.randSource.getPreprocessedSourceData((int)(minEntropySequenceLength/randSource.getBytesPerSample(true) + ((minEntropySequenceLength%randSource.getBytesPerSample(true) > 0)?1:0)), null);
			sourceSequence.setBitLength(minEntropySequenceLength-1);
			actualSequence = new ByteSequence(new byte[]{(byte)0x80}, 1);
			actualSequence.add(sourceSequence);
			for (int i = 0; i < trueRandomSequenceLength; i++) {
				seed.rotateBitsLeft();
				returnSequence.addBit(seed.scalarProduct(actualSequence));
			}
		}
		return returnSequence;
	}

	public boolean storeRandomness(int length) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ByteSequence getStoredRandomness(int length) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ByteSequence extractRandomness(ByteSequence seed, ByteSequence minEntropyData) throws LengthsNotEqualException, IOException {
		ByteSequence randomData = new ByteSequence();
		ByteSequence actualSequence = null;
		ByteSequence sourceSequence = minEntropyData;
		//for (int j = 0; j < (int)(length/trueRandomSequenceLength)+((length%trueRandomSequenceLength > 0)?1:0); j++) {
			/*sourceSequence = this.randSource.getPreprocessedSourceData((int)(minEntropySequenceLength/randSource.getBytesPerSample(true) + ((minEntropySequenceLength%randSource.getBytesPerSample(true) > 0)?1:0)), null);
			sourceSequence.setBitLength(minEntropySequenceLength);*/
			actualSequence = new ByteSequence(new byte[]{(byte)0x80}, 1);
			sourceSequence.setBitLength(minEntropySequenceLength-1);
			actualSequence.add(sourceSequence);
		
			for (int i = 0; i < trueRandomSequenceLength; i++) {
				seed.rotateBitsLeft();
				randomData.addBit(seed.scalarProduct(actualSequence));
			}
		//}
		return randomData;
	}

	@Override
	public boolean storeRandomness(ByteSequence seed,
			ByteSequence minEntropyData) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int minEntropyNeededForLength(int length) {
		int roundsNeeded = (int)Math.ceil((double)length/trueRandomSequenceLength);
		Log.d("minEntropyNeededFor", length+"->"+roundsNeeded*minEntropySequenceLength);
		return roundsNeeded*minEntropySequenceLength;
	}

	@Override
	public int getMinEntropySequenceLength() {
		return minEntropySequenceLength;
	}

	@Override
	public int getTrueRandomSequenceLength() {
		return trueRandomSequenceLength;
	}
	
	@Override
	public int getSeedLength() {
		return seedLength;
	}

}
