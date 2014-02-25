package cz.muni.fi.randgka.library;

public interface GKAProtocol {
	
	public int getRandomnessLength();
	
	public void init(GKAParticipants participants, int keyLength, int version);
	
	public void setRandSequence(ByteSequence randSequence);
	
	public ProtocolRound runRound(PMessage message);
	
	public void putMessage(PMessage message);
	
	public byte[] getKey();
	
}
