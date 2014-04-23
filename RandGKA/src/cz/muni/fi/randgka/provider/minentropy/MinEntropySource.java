package cz.muni.fi.randgka.provider.minentropy;

import java.io.File;

import cz.muni.fi.randgka.tools.ByteSequence;

/**
 * Interface modeling the general source of randomness from the mobile devices.
 */
public interface MinEntropySource {
	
	/**
	 * Initialization of the source
	 * 
	 * @return true if the source is able to properly generate min-entropy sequences, false otherwise
	 */
	public boolean initialize();
	
	/**
	 * 
	 * @param minEntropyDataLength length of the data wanted to gain in bits
	 * @param storage - file for storing the data, if null, function returns the data directly
	 * @return the RandSequence of the raw data gained from the source or null if the data should be stored in the given file
	 */
	public ByteSequence getRawSourceData(int minEntropyDataLength, File storage);
	
	/**
	 * 
	 * @param minEntropyDataLength length of the data wanted to gain in bits
	 * @param storage - file for storing the data, if null, function returns the data directly
	 * @return the ByteSequence of the preprocessed data or null if the data should be stored in the given file
	 */
	public ByteSequence getPreprocessedSourceData(int minEntropyDataLength, File storage);
	
	/**
	 * 
	 * @param minEntropyDataLength length of the data to gain in bits
	 * @return the ByteSequence of the preprocessed data
	 */
	public ByteSequence getMinEntropyData(int minEntropyDataLength, File storage);
	
	/**
	 * @param usingPreprocessing
	 * @return the number of bits gained by one sample in current setting
	 */
	public int getBitsPerSample(boolean usingPreprocessing);

	/**
	 * 
	 * @return true if the source is ready to output min-entropy sequences
	 */
	public boolean ready();
	
	/**
	 * Release all the outer sources needed.
	 */
	public void stop();
}
