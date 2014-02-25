package cz.muni.fi.randgka.library;

import java.io.DataInputStream;
import java.io.IOException;

import cz.muni.fi.randgka.library.ByteSequence;
import cz.muni.fi.randgka.library.LengthsNotEqualException;
import cz.muni.fi.randgka.library.MinEntropySource;

/**
 * 
 * @author gitti
 *
 * Interface modeling the general source of randomness from the mobile devices.
 */
public interface RandExtractor {
	
	public void initialize();
	
	/**
	 * Initialization of the extractor
	 * 
	 * @param defaultLength sets the default length of the output byte sequence
	 * @param source which will produce the sequence with high min-entropy distribution
	 * @throws IOException 
	 */
	public void initialize(int defaultLength, MinEntropySource source, DataInputStream dis) throws IOException;
	
	/**
	 * 
	 * @param seed
	 * @param minEntropyData
	 * @return the RandSequence of the random data extracted by the randomness extractor
	 * @throws LengthsNotEqualException 
	 * @throws IOException 
	 */
	public ByteSequence extractRandomness(ByteSequence seed, ByteSequence minEntropyData) throws LengthsNotEqualException, IOException;
	
	/**
	 * 
	 * @param seed
	 * @param minEntropyData
	 * @param length of the random sequence stored in a safe storage, if equals 0 defaultLength will be used
	 * @return true if the random sequence of given length was successfully stored, false otherwise
	 */
	public boolean storeRandomness(ByteSequence seed, ByteSequence minEntropyData);
	
	/**
	 * 
	 * @param length of the random sequence stored in a safe storage to be withdrawn, if equals 0 defaultLength will be used
	 * @return the RandSequence of random data withdrawn from the safe storage (the sequence is deleted from the storage)
	 */
	public ByteSequence getStoredRandomness(int length);
	
	/**
	 * 
	 * @param length of wanted random sequence 
	 * @return length of min-entropy sequence needed for the wanted random sequence length
	 */
	public int minEntropyNeededForLength(int length);
	
	public int getMinEntropySequenceLength();
	public int getSeedLength();
	public int getTrueRandomSequenceLength();

}