package cz.muni.fi.randgka.library;

import java.io.File;

import cz.muni.fi.androidrandextr.setrt.MinEntropyApproximationRT;

/**
 * 
 * @author gitti
 *
 * Interface modeling the general source of randomness from the mobile devices.
 */
public interface MinEntropySource {
	
	/**
	 * Initialization of the source
	 */
	public void initialize();
	
	/**
	 * 
	 * @param gm sets the mode of gaining the output sequence: SAFE - predetermined process of gaining the high min-entropy distribution, MAXIMAL - autosetting the preprocessing using MinEntropyApproximationRT
	 */
	public void setGainMode(GainMode gm);

	/**
	 * 
	 * @param approx used for approximating the min-entropy for the last period of time
	 */
	public void setApprox(MinEntropyApproximationRT approx);
	
	/**
	 * 
	 * @param minEntropyDataLength length of the data wanted to gain
	 * @param storage - file for storing the data, if null, function returns the data directly
	 * @return the RandSequence of the raw data gained from the source or null if the data should be stored in the given file
	 */
	public ByteSequence getRawSourceData(int minEntropyDataLength, File storage);
	
	/**
	 * 
	 * @param minEntropyDataLength length of the data wanted to gain
	 * @param storage - file for storing the data, if null, function returns the data directly
	 * @return the RandSequence of the preprocessed data or null if the data should be stored in the given file
	 */
	public ByteSequence getPreprocessedSourceData(int minEntropyDataLength, File storage);
	
	/**
	 * @param usingPreprocessing
	 * @return the number of bytes gained by one sample in current setting
	 */
	public int getBytesPerSample(boolean usingPreprocessing);
}
