package cz.muni.fi.randgka.gka;

import java.math.BigInteger;
import java.security.SecureRandom;

import cz.muni.fi.randgka.tools.PMessage;

/**
 * Group key agreement protocol interface
 */
public interface GKAProtocol {
	
	/**
	 * Initialization
	 * 
	 * @param participants - participants of current protocol instance
	 * @param secureRandom - randomness provider
	 * @param gkaProtocolParams - params of current protocol instance
	 */
	public void init(GKAParticipants participants, SecureRandom secureRandom, GKAProtocolParams gkaProtocolParams);
	
	/**
	 * @return true if current instance is initialized, false otherwise
	 */
	public boolean isInitialized();
	
	/**
	 * @param message - original message to process
	 * @return GKAProtocolRound responding to the original message
	 */
	public GKAProtocolRound nextRound(PMessage message);
	
	/**
	 * @return established group key agreement key
	 */
	public BigInteger getKey();
}
