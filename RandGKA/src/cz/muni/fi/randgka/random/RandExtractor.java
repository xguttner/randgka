package cz.muni.fi.randgka.random;

import cz.muni.fi.randgka.library.ByteSequence;

public interface RandExtractor{
	
	public void initialize(MinEntropySource mes, ByteSequence seed);
	
}
