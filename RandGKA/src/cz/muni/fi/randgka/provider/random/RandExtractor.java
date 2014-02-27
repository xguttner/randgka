package cz.muni.fi.randgka.provider.random;

import cz.muni.fi.randgka.library.ByteSequence;
import cz.muni.fi.randgka.library.MinEntropySource;

public interface RandExtractor {
	
	public void initialize(MinEntropySource mes, ByteSequence seed);
	
}
