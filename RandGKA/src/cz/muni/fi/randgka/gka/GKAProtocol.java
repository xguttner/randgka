package cz.muni.fi.randgka.gka;

import java.math.BigInteger;
import java.security.SecureRandom;

import cz.muni.fi.randgka.tools.PMessage;

public interface GKAProtocol {
	
	public void init(GKAParticipants participants, SecureRandom secureRandom, GKAProtocolParams gkaProtocolParams);
	
	public boolean isInitialized();
	
	public GKAProtocolRound nextRound(PMessage message);
	
	public BigInteger getKey();
}