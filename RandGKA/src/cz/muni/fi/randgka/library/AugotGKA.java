package cz.muni.fi.randgka.library;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class AugotGKA implements GKAProtocol {

	private GKAParticipants participants;
	private ByteSequence randInit;
	private PMessage lastMessage;
	
	private int randInitLength, secretLength, version;
	
	private BigInteger secret, secretInverse, blindedSecret;
	
	public static final int UNAUTHORIZED = 1, AUTHORIZED = 2, MAC_ADDRESS_LENGTH = 17;
	
	private BigInteger p, q, g, key;
	
	public AugotGKA() {
		this.p = new BigInteger("ab359aa76a6773ed7a93b214db0c25d0160817b8a893c001c761e198a3694509ebe8"+
				"7a5313e0349d95083e5412c9fc815bfd61f95ddece43376550fdc624e92ff38a415783b9726120"+
				"4e05d65731bba1ccff0e84c8cd2097b75feca1029261ae19a389a2e15d2939314b184aef707b82"+
				"eb94412065181d23e04bf065f4ac413f", 16);
		this.q = p.subtract(BigInteger.ONE).divide(BigInteger.ONE.add(BigInteger.ONE));
		this.g = new BigInteger("2", 16);
		
		this.randInitLength = 128;
	}

	@Override
	public void init(GKAParticipants participants, int secretLength, int version) {
		this.participants = participants;
		this.secretLength = secretLength;
		this.version = version;
	}

	@Override
	public ProtocolRound runRound(PMessage message) {
		if (message == null) {
			if (lastMessage == null) return firstServerRound();
			else return firstMemberRound();
		} else {
			switch (message.getRoundNo()) {
				case (byte)1:
					return secondServerRound(message);
				case (byte)2:
					return secondMemberRound(message);
				default:
					break;
			}
		}
		return null;
	}
	
	private ProtocolRound secondMemberRound(PMessage pMessage) {
		ProtocolRound pr = new ProtocolRound();
		
		Log.d("secondMemberRound", pMessage.toString());
		byte[] doubleBlindedBytes = new byte[pMessage.getLength()];
		System.arraycopy(pMessage.getMessage(), 0, doubleBlindedBytes, 0, pMessage.getLength());
		BigInteger doubleBlinded = new BigInteger(1, doubleBlindedBytes);
		
		Log.d("dbs|bs|dbs", doubleBlinded.toString(16)+" ("+doubleBlinded.bitLength()+")"+" | "+blindedSecret.toString(16)+" ("+blindedSecret.bitLength()+")");
		
		key = computeKeyForTwo(doubleBlinded);
		Log.d("key", key.toString(16));
		
		pr.setKeyEstablished(true);
		
		return pr;
	}

	private ProtocolRound secondServerRound(PMessage pMessage) {
		Log.d("secondServerRound", pMessage.toString());
		ProtocolRound pr = new ProtocolRound();
		
		byte[] macAddressB = new byte[MAC_ADDRESS_LENGTH];
		byte[] blindedMemberSecretB = new byte[pMessage.getLength()-MAC_ADDRESS_LENGTH];
		
		System.arraycopy(pMessage.getMessage(), 0, macAddressB, 0, MAC_ADDRESS_LENGTH);
		String macAddress = new String(macAddressB);
		
		System.arraycopy(pMessage.getMessage(), MAC_ADDRESS_LENGTH, blindedMemberSecretB, 0, pMessage.getLength()-MAC_ADDRESS_LENGTH);
		BigInteger blindedMemberSecret = new BigInteger(1, blindedMemberSecretB);
		
		//blindedSecret = powMod(g, secretBI, p);
		blindedSecret = g.modPow(secret, p);
		BigInteger doubleBlindedSecret = blindedMemberSecret.modPow(secret, p);
		
		Log.d("rbs|bs|dbs", (pMessage.getLength()-MAC_ADDRESS_LENGTH)+" "+blindedMemberSecret.toString(16)+" ("+blindedMemberSecret.bitLength()+")"+" | "+blindedSecret.toString(16)+" ("+blindedSecret.bitLength()+")"+" | "+doubleBlindedSecret.toString(16)+" ("+doubleBlindedSecret.bitLength()+")");
	
		PMessage pm = new PMessage(MessageAction.GKA_PROTOCOL, (byte)2, (byte)0, doubleBlindedSecret.toByteArray().length, doubleBlindedSecret.toByteArray());
		pr.setMessage(pm);
		
		key = computeKeyForTwo(doubleBlindedSecret);
		Log.d("key", key.toString(16));
		
		pr.setKeyEstablished(true);
		
		List<GKAParticipant> targets = new ArrayList<GKAParticipant>();
		if (participants != null) {
			for (GKAParticipant p : participants.getParticipants()) {
				if (p.getMacAddress().equals(macAddress)) {
					targets.add(p);
					pr.setTargets(targets);
					return pr;
				}
			}
		}
		return null;
	}

	private ProtocolRound firstServerRound() {
		ProtocolRound pr = new ProtocolRound();
		
		PMessage pm = new PMessage(MessageAction.START_GKA_PROTOCOL, (byte)0, (byte)0, randInit.getByteLength(), randInit.getSequence());
		pr.setMessage(pm);
		
		Log.d("firstServerRound", pm.toString());
		
		List<GKAParticipant> targets = new ArrayList<GKAParticipant>();
		if (participants != null) {
			for (GKAParticipant p : participants.getParticipants()) {
				if (!p.getRole().equals(ParticipateRole.LEADER)) targets.add(p);
			}
		}
		pr.setTargets(targets);
		
		return pr;
	}
	
	private ProtocolRound firstMemberRound() {
		Log.d("firstMemberRound", this.lastMessage.toString());
		ProtocolRound pr = new ProtocolRound();
		
		//blindedSecret = powMod(g, secretBI, p);
		blindedSecret = g.modPow(secret, p);
		byte[] blindedSecretBytes = blindedSecret.toByteArray();
		byte[] macAddress = participants.getMe().getMacAddress().getBytes();
		
		byte[] message = new byte[blindedSecretBytes.length+MAC_ADDRESS_LENGTH];
		System.arraycopy(macAddress, 0, message, 0, MAC_ADDRESS_LENGTH);
		System.arraycopy(blindedSecretBytes, 0, message, MAC_ADDRESS_LENGTH, blindedSecretBytes.length);
		
		PMessage pm = new PMessage(MessageAction.GKA_PROTOCOL, (byte)1, (byte)1, message.length, message);
		pr.setMessage(pm);
		
		Log.d("secret|blindedSecret", secret.toString(16)+" | "+blindedSecret.toString(16)+" ("+blindedSecret.bitLength()+")");
		
		List<GKAParticipant> targets = new ArrayList<GKAParticipant>();
		if (participants != null) {
			for (GKAParticipant p : participants.getParticipants()) {
				if (p.getRole().equals(ParticipateRole.LEADER)) targets.add(p);
			}
		}
		pr.setTargets(targets);
		
		return pr;
	}
	
	private BigInteger computeKeyForTwo(BigInteger doubleBlindedSecret) {
		BigInteger key = null;
		if (participants.getMe().getRole() == ParticipateRole.LEADER) {
			key = doubleBlindedSecret.multiply(blindedSecret).mod(p);
		}
		else {
			BigInteger leaderSecret = doubleBlindedSecret.modPow(secretInverse, p);
			Log.d("secretInverseBI | leaderSecret", secretInverse.toString(16)+" ("+secretInverse.bitLength()+") | "+leaderSecret.toString(16)+" ("+leaderSecret.bitLength()+")");
			
			key = leaderSecret.multiply(doubleBlindedSecret).mod(p);
		}
		return key;
	}
	
	@Override
	public int getRandomnessLength() {
		return randInitLength + secretLength + 8;
	}

	@Override
	public void setRandSequence(ByteSequence randSequence) {
		randInit = randSequence.getSubSequence(0, randInitLength);
		
		secret = new BigInteger(1, randSequence.getSubSequence(randInitLength, secretLength).getSequence());
		secret = secret.shiftRight(8-(secretLength%8));
		Log.d("g | q", g.toString(10)+" "+q.toString(16));
		secret = secret.mod(q);
		secretInverse = secret.modInverse(q);
		Log.d("secretFromBI", secret.toString(2));
	}
	
	@Override
	public void putMessage(PMessage message) {
		this.lastMessage = message;
	}

	@Override
	public byte[] getKey() {
		return key.toByteArray();
	}
}
