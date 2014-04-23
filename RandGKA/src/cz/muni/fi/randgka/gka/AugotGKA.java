package cz.muni.fi.randgka.gka;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

import android.util.Log;
import cz.muni.fi.randgka.tools.MessageAction;
import cz.muni.fi.randgka.tools.PMessage;
import cz.muni.fi.randgka.tools.ProtocolType;

public class AugotGKA implements GKAProtocol {

	public static final ProtocolType PROTOCOL_TYPE = ProtocolType.AUGOT;
	
	private static final BigInteger p = new BigInteger("ab359aa76a6773ed7a93b214db0c25d0160817b8a893c001c761e198a3694509ebe8"+
			"7a5313e0349d95083e5412c9fc815bfd61f95ddece43376550fdc624e92ff38a415783b9726120"+
			"4e05d65731bba1ccff0e84c8cd2097b75feca1029261ae19a389a2e15d2939314b184aef707b82"+
			"eb94412065181d23e04bf065f4ac413f", 16),
			g = new BigInteger("2", 16),
			q = p.subtract(BigInteger.ONE).divide(BigInteger.ONE.add(BigInteger.ONE));
	
	private GKAParticipants participants;
	private SecureRandom secureRandom;
	private GKAProtocolParams gkaProtocolParams;
	private BigInteger secret,
			publicKey,
			secretInverse,
			key;
	private byte[] secondRoundBroadcast;
	private int secondRoundBroadcastLength,
			secondRoundBroadcastPartLength,
			partsReceived;
	private byte[] nonce;
	private boolean initialized = false;
	
	public AugotGKA() {}
	
	@Override
	public void init(GKAParticipants participants, SecureRandom secureRandom, GKAProtocolParams gkaProtocolParams) {
		this.participants = participants;
		this.secureRandom = secureRandom;
		
		// so far the only implemented key length
		gkaProtocolParams.setKeyLength(128);
		
		this.gkaProtocolParams = gkaProtocolParams;
		if (participants != null) {
			partsReceived = 0;
			secondRoundBroadcastPartLength = (4 + gkaProtocolParams.getNonceLength() + gkaProtocolParams.getKeyLength()*2);
			this.secondRoundBroadcastLength = (participants.size()-1) * secondRoundBroadcastPartLength;
			this.secondRoundBroadcast = new byte[secondRoundBroadcastLength];
		}
		
		byte[] secretBytes = new byte[gkaProtocolParams.getKeyLength()/2];
		secureRandom.nextBytes(secretBytes);
		secret = new BigInteger(1, secretBytes);
		secret = secret.mod(q);
		publicKey = g.modPow(secret, p);
		secretInverse = secret.modInverse(q); 
		key = BigInteger.ONE;
		initialized = true;
	}

	@Override
	public GKAProtocolRound nextRound(PMessage message) {
		
		if (message.getRoundNo() != 0 && gkaProtocolParams.isAuthenticated()) {
			if (!message.selfVerify(participants.getParticipant(message.getOriginatorId()).getAuthPublicKey())) {
				GKAProtocolRound gkaProtocolRound = new GKAProtocolRound();
				gkaProtocolRound.setActionCode(GKAProtocolRound.ERROR);
				return gkaProtocolRound;
			}
		}
		
		switch (message.getRoundNo()) {
		
			case (byte)0:
				return firstServerRound(message);
			
			case (byte)1:
				// test if the message was sent by leader - unsecure testing
				if (message.getOriginatorId() 
						== participants.getLeader().getId()) {
					return firstMemberRound(message);
				}
				break;
				
			case (byte)2:
				return secondServerRound(message);
			
			case (byte)3:
				// test if the message was sent by leader - unsecure testing
				if (message.getOriginatorId() == participants.getLeader().getId()) {
					return secondMemberRound(message);
				}
				break;
				
			default:
				break;
		}
		return null;
	}

	private GKAProtocolRound firstServerRound(PMessage message) {
		GKAProtocolRound round = new GKAProtocolRound();
		PMessage pMessage = new PMessage();
		
		pMessage.setRoundNo((byte)1);
		pMessage.setAction(MessageAction.INIT_GKA_PROTOCOL);
		pMessage.setOriginatorId(participants.getMe().getId());
		
		nonce = new byte[gkaProtocolParams.getNonceLength()];
		secureRandom.nextBytes(nonce);
		
		pMessage.setLength(gkaProtocolParams.getNonceLength());
		pMessage.setMessage(nonce);
		
		if (gkaProtocolParams.isAuthenticated()) pMessage.selfSign(gkaProtocolParams.getPrivateKey(), null);
		
		round.put(participants.getAllButMe(), pMessage);
		return round;
	}

	private GKAProtocolRound firstMemberRound(PMessage message) {
		PMessage pMessage = new PMessage();
		
		byte[] leaderNonce = message.getMessage();
		
		pMessage.setRoundNo((byte)2);
		pMessage.setAction(MessageAction.GKA_PROTOCOL);
		pMessage.setOriginatorId(participants.getMe().getId());
		
		nonce = new byte[gkaProtocolParams.getNonceLength()];
		secureRandom.nextBytes(nonce);
		
		// length: int in 4 bytes of leader's id + leader's nonce + member's nonce + public key length
		byte[] messageArray = new byte[4 + 2*gkaProtocolParams.getNonceLength() + gkaProtocolParams.getKeyLength()];
		
		byte[] leaderId = ByteBuffer.allocate(4).putInt(message.getOriginatorId()).array();
		 
		System.arraycopy(leaderId, 0, messageArray, 0, 4);
		System.arraycopy(leaderNonce, 0, messageArray, 4, gkaProtocolParams.getNonceLength());
		System.arraycopy(nonce, 0, messageArray, gkaProtocolParams.getNonceLength()+4, gkaProtocolParams.getNonceLength());
		System.arraycopy(publicKey.toByteArray(), (publicKey.toByteArray().length>gkaProtocolParams.getKeyLength())?1:0, messageArray, 2*gkaProtocolParams.getNonceLength()+4, gkaProtocolParams.getKeyLength());
		
		Log.d("arrayM", Arrays.toString(messageArray)+" "+messageArray.length+" "+(4 + 2*gkaProtocolParams.getNonceLength() + gkaProtocolParams.getKeyLength()));
		 
		Log.d("array", Arrays.toString(publicKey.toByteArray())+" "+publicKey.toByteArray().length);
		Log.d("receivedPublic", publicKey.toString(16));  
		pMessage.setLength(4 + 2*gkaProtocolParams.getNonceLength() + gkaProtocolParams.getKeyLength());
		pMessage.setMessage(messageArray);
		
		if (gkaProtocolParams.isAuthenticated()) pMessage.selfSign(gkaProtocolParams.getPrivateKey(), null);
		
		GKAProtocolRound round = new GKAProtocolRound();
		round.put(participants.getLeader(), pMessage);
		Log.d("firstRound", round.toString());
		return round;
	}
	
	private GKAProtocolRound secondServerRound(PMessage message) {
		GKAProtocolRound round = new GKAProtocolRound();
		PMessage pMessage = new PMessage();
		
		GKAParticipant originator = participants.getParticipant(message.getOriginatorId());
		if (originator != null && originator.getDHPublicKey() == null) {
				byte[] keyBytes = new byte[gkaProtocolParams.getKeyLength()];
				Log.d("arrayM", Arrays.toString(message.getMessage()));
				System.arraycopy(message.getMessage(), 2*gkaProtocolParams.getNonceLength()+4, keyBytes, 0, gkaProtocolParams.getKeyLength());
				Log.d("array", Arrays.toString(keyBytes));
				BigInteger receivedPublic = new BigInteger(1, keyBytes);
				BigInteger compoundPublic = receivedPublic.modPow(secret, p);
				key = key.multiply(compoundPublic).mod(p);
				Log.d("receivedPublic", receivedPublic.toString(16));
				Log.d("pLen", receivedPublic.toByteArray().length+"");
				Log.d("compoundPublic", compoundPublic.toString(16));
				Log.d("key1", key.toString(16));
				
				originator.setDHPublicKey(receivedPublic);
				participants.merge(originator);
				
				partsReceived++;
				int offset = (partsReceived-1) * secondRoundBroadcastPartLength;
				
				byte[] originatorIdArray = ByteBuffer.allocate(4).putInt(message.getOriginatorId()).array();
				System.arraycopy(originatorIdArray, 0, secondRoundBroadcast, offset, 4);
				System.arraycopy(message.getMessage(), gkaProtocolParams.getNonceLength()+4, secondRoundBroadcast, offset+4, gkaProtocolParams.getNonceLength());
				System.arraycopy(keyBytes, 0, secondRoundBroadcast, offset+4+gkaProtocolParams.getNonceLength(), gkaProtocolParams.getKeyLength());
				System.arraycopy(compoundPublic.toByteArray(), (compoundPublic.toByteArray().length>gkaProtocolParams.getKeyLength())?1:0, secondRoundBroadcast, offset+4+gkaProtocolParams.getNonceLength()+gkaProtocolParams.getKeyLength(), gkaProtocolParams.getKeyLength());
				
				if (partsReceived == participants.size()-1) {
					pMessage.setRoundNo((byte)3);
					pMessage.setAction(MessageAction.GKA_PROTOCOL);
					pMessage.setOriginatorId(participants.getMe().getId());
					
					byte[] messageArray = new byte[secondRoundBroadcastLength + gkaProtocolParams.getNonceLength()];
					System.arraycopy(nonce, 0, messageArray, 0, gkaProtocolParams.getNonceLength());
					System.arraycopy(secondRoundBroadcast, 0, messageArray, gkaProtocolParams.getNonceLength(), secondRoundBroadcastLength);
					
					pMessage.setLength(secondRoundBroadcastLength + gkaProtocolParams.getNonceLength());
					pMessage.setMessage(messageArray);
						
					if (gkaProtocolParams.isAuthenticated()) pMessage.selfSign(gkaProtocolParams.getPrivateKey(), null);
					
					round.put(participants.getAllButMe(), pMessage);
					round.setActionCode(GKAProtocolRound.SUCCESS);
				}
		}
		
		Log.d("secondRound", round.toString());
		return round;
	}
	
	private GKAProtocolRound secondMemberRound(PMessage message) {
		GKAProtocolRound round = new GKAProtocolRound();
		
		int currentId;
		GKAParticipant originator = null;
		int offset = gkaProtocolParams.getNonceLength();
		
		while (offset < message.getLength()) {
			
			byte[] currentIdArray = new byte[4];
			byte[] currentKeyArray = new byte[gkaProtocolParams.getKeyLength()];
			byte[] currentCompoundKeyArray = new byte[gkaProtocolParams.getKeyLength()];
			
			System.arraycopy(message.getMessage(), offset, currentIdArray, 0, 4);
			currentId = ByteBuffer.wrap(currentIdArray).getInt();
			originator = participants.getParticipant(currentId);
			
				System.arraycopy(message.getMessage(), offset+4+gkaProtocolParams.getNonceLength(), currentKeyArray, 0, gkaProtocolParams.getKeyLength());
				System.arraycopy(message.getMessage(), offset+4+gkaProtocolParams.getNonceLength()+gkaProtocolParams.getKeyLength(), currentCompoundKeyArray, 0, gkaProtocolParams.getKeyLength());
				
				originator.setDHPublicKey(new BigInteger(1, currentKeyArray));
				participants.merge(originator);
				
				BigInteger compoundPublic = new BigInteger(1, currentCompoundKeyArray);
				compoundPublic = compoundPublic.mod(p);
				Log.d("key0", key.toString(16));
				key = key.multiply(compoundPublic).mod(p);
				Log.d("compoundPublic", compoundPublic.toString(16));
				Log.d("key1", key.toString(16));
				if (originator.equals(participants.getMe())) {
					Log.d("publicKey", compoundPublic.modPow(secretInverse, p).toString(16));
					key = key.multiply(compoundPublic.modPow(secretInverse, p)).mod(p);
					Log.d("key2", key.toString(16));
				}	
			
			offset += secondRoundBroadcastPartLength;
		}
		 
		round.setActionCode(GKAProtocolRound.SUCCESS);
		Log.d("secondRound", round.toString());
		return round;
	}
	
	@Override
	public BigInteger getKey() {
		if (participants.getMe().getRole().equals(GKAParticipantRole.LEADER)) {
			Log.d("publicKey", publicKey.toString(16));
			key = key.multiply(publicKey).mod(p);
			Log.d("key2", key.toString(16));
		}
		return key;
	}

	public boolean isInitialized() {
		return initialized;
	}
}
