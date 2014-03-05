package cz.muni.fi.randgka.gka;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.interfaces.DHKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;

import android.util.Log;
import cz.muni.fi.randgka.tools.MessageAction;
import cz.muni.fi.randgka.tools.PMessage;

public class AugotGKA implements GKAProtocol {

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
	//private KeyPair keyPair;
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
		/*try {
			DHParameterSpec param = new DHParameterSpec(p,g,gkaProtocolParams.getKeyLength()/2);
			
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
		    kpg.initialize(param, this.secureRandom);
		    
		    keyPair = kpg.generateKeyPair();
		    
		    participants.getMe().setKey(keyPair.getPublic());
		    
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		initialized = true;
	}

	@Override
	public GKAProtocolRound nextRound(PMessage message) {
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
		PMessage pMessage = new PMessage();
		
		pMessage.setRoundNo((byte)1);
		pMessage.setAction(MessageAction.GKA_PROTOCOL);
		pMessage.setOriginatorId(participants.getMe().getId());
		
		nonce = new byte[gkaProtocolParams.getNonceLength()];
		secureRandom.nextBytes(nonce);
		
		if (gkaProtocolParams.isAuthenticated()) {
			
		}
		else {
			pMessage.setLength(gkaProtocolParams.getNonceLength());
			pMessage.setMessage(nonce);
		}
		
		GKAProtocolRound round = new GKAProtocolRound();
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
		
		if (gkaProtocolParams.isAuthenticated()) {
			
		}
		else {
			// length: int in 4 bytes of leader's id + leader's nonce + member's nonce + public key length
			byte[] messageArray = new byte[4 + 2*gkaProtocolParams.getNonceLength() + gkaProtocolParams.getKeyLength()/*keyPair.getPublic().getEncoded().length*/];
			
			byte[] leaderId = ByteBuffer.allocate(4).putInt(message.getOriginatorId()).array();
			
			System.arraycopy(leaderId, 0, messageArray, 0, 4);
			System.arraycopy(leaderNonce, 0, messageArray, 4, gkaProtocolParams.getNonceLength());
			System.arraycopy(nonce, 0, messageArray, gkaProtocolParams.getNonceLength()+4, gkaProtocolParams.getNonceLength());
			//System.arraycopy(keyPair.getPublic().getEncoded(), 0, messageArray, 2*gkaProtocolParams.getNonceLength()+4, keyPair.getPublic().getEncoded().length);
			System.arraycopy(publicKey.toByteArray(), 0, messageArray, 2*gkaProtocolParams.getNonceLength()+4, gkaProtocolParams.getKeyLength());
			
			pMessage.setLength(4 + 2*gkaProtocolParams.getNonceLength() + gkaProtocolParams.getKeyLength());
			pMessage.setMessage(messageArray);
		}
		
		GKAProtocolRound round = new GKAProtocolRound();
		round.put(participants.getLeader(), pMessage);
		return round;
	}
	
	private GKAProtocolRound secondServerRound(PMessage message) {
		GKAProtocolRound round = new GKAProtocolRound();
		PMessage pMessage = new PMessage();
		
		GKAParticipant originator = participants.getParticipant(message.getOriginatorId());
		if (originator != null && originator.getPublicKey() == null) {
			//try {
				byte[] keyBytes = new byte[gkaProtocolParams.getKeyLength()];
				System.arraycopy(message.getMessage(), 2*gkaProtocolParams.getNonceLength()+4, keyBytes, 0, gkaProtocolParams.getKeyLength());
				
				//PublicKey publicKey = KeyFactory.getInstance("DiffieHellman").generatePublic(new X509EncodedKeySpec(keyBytes));
				BigInteger receivedPublic = new BigInteger(1, keyBytes);
				BigInteger compoundPublic = receivedPublic.modPow(secret, p);
				key = key.multiply(compoundPublic).mod(p);
				
				originator.setPublicKey(receivedPublic);
				participants.merge(originator);
				
				partsReceived++;
				int offset = (partsReceived-1) * secondRoundBroadcastPartLength;
				
				byte[] originatorIdArray = ByteBuffer.allocate(4).putInt(message.getOriginatorId()).array();
				System.arraycopy(originatorIdArray, 0, secondRoundBroadcast, offset, 4);
				System.arraycopy(message.getMessage(), gkaProtocolParams.getNonceLength()+4, secondRoundBroadcast, offset+4, gkaProtocolParams.getNonceLength());
				System.arraycopy(keyBytes, 0, secondRoundBroadcast, offset+4+gkaProtocolParams.getNonceLength(), gkaProtocolParams.getKeyLength());
				System.arraycopy(compoundPublic.toByteArray(), 0, secondRoundBroadcast, offset+4+gkaProtocolParams.getNonceLength()+gkaProtocolParams.getKeyLength(), gkaProtocolParams.getKeyLength());
				
				if (partsReceived == participants.size()-1) {
					pMessage.setRoundNo((byte)3);
					pMessage.setAction(MessageAction.GKA_PROTOCOL);
					pMessage.setOriginatorId(participants.getMe().getId());
					
					if (gkaProtocolParams.isAuthenticated()) {
					}
					else {
						byte[] messageArray = new byte[secondRoundBroadcastLength + gkaProtocolParams.getNonceLength()];
						System.arraycopy(nonce, 0, messageArray, 0, gkaProtocolParams.getNonceLength());
						System.arraycopy(secondRoundBroadcast, 0, messageArray, gkaProtocolParams.getNonceLength(), secondRoundBroadcastLength);
						
						pMessage.setLength(secondRoundBroadcastLength + gkaProtocolParams.getNonceLength());
						pMessage.setMessage(messageArray);
					}
					
					round.put(participants.getAllButMe(), pMessage);
					round.setActionCode(GKAProtocolRound.SUCCESS);
				}
				
			/*} catch (InvalidKeySpecException e) {
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}*/
		}
		return round;
	}
	
	private GKAProtocolRound secondMemberRound(PMessage message) {
		GKAProtocolRound round = new GKAProtocolRound();
		
		if (gkaProtocolParams.isAuthenticated()) {
			
		} else {
			
			byte[] currentIdArray = new byte[4];
			byte[] currentKeyArray = new byte[gkaProtocolParams.getKeyLength()];
			byte[] currentCompoundKeyArray = new byte[gkaProtocolParams.getKeyLength()];
			int currentId;
			GKAParticipant originator = null;
			int offset = gkaProtocolParams.getNonceLength();
			
			while (offset < message.getLength()) {
				
				System.arraycopy(message.getMessage(), offset, currentIdArray, 0, 4);
				currentId = ByteBuffer.wrap(currentIdArray).getInt();
				originator = participants.getParticipant(currentId);
				
				//if (originator != null && originator.getPublicKey() == null) {
					System.arraycopy(message.getMessage(), offset+4+gkaProtocolParams.getNonceLength(), currentKeyArray, 0, gkaProtocolParams.getKeyLength());
					System.arraycopy(message.getMessage(), offset+4+gkaProtocolParams.getNonceLength()+gkaProtocolParams.getKeyLength(), currentCompoundKeyArray, 0, gkaProtocolParams.getKeyLength());
					
					//try {
						//PublicKey publicKey = KeyFactory.getInstance("DiffieHellman").generatePublic(new X509EncodedKeySpec(currentKeyArray));
					originator.setPublicKey(new BigInteger(1, currentKeyArray));
					participants.merge(originator);
					/*} catch (InvalidKeySpecException e) {
						e.printStackTrace();
					} catch (NoSuchAlgorithmException e) {
						e.printStackTrace();
					}*/
					
					BigInteger compoundPublic = new BigInteger(1, currentCompoundKeyArray);
					key = key.multiply(compoundPublic).mod(p);
					
					if (originator.equals(participants.getMe())) {
						key = key.multiply(compoundPublic.modPow(secretInverse, p)).mod(p);
					}
				//}
				
				offset += secondRoundBroadcastPartLength;
			}
		}
		
		round.setActionCode(GKAProtocolRound.SUCCESS);
		return round;
	}
	
	@Override
	public BigInteger getKey() {
		if (participants.getMe().getRole().equals(GKAParticipantRole.LEADER)) {
			key = key.multiply(publicKey).mod(p);
		}
		
		return key;
	}

	public boolean isInitialized() {
		return initialized;
	}
	
	/*
	private BluetoothGKAParticipants participants;
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
	public void init(BluetoothGKAParticipants participants, int secretLength, int version) {
		this.participants = participants;
		this.secretLength = secretLength;
		this.version = version;
	}

	@Override
	public GKAProtocolRound runRound(PMessage message) {
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
	
	private GKAProtocolRound secondMemberRound(PMessage pMessage) {
		GKAProtocolRound pr = new GKAProtocolRound();
		
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

	private GKAProtocolRound secondServerRound(PMessage pMessage) {
		Log.d("secondServerRound", pMessage.toString());
		GKAProtocolRound pr = new GKAProtocolRound();
		
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
		
		List<BluetoothGKAParticipant> targets = new ArrayList<BluetoothGKAParticipant>();
		if (participants != null) {
			for (BluetoothGKAParticipant p : participants.getParticipants()) {
				if (p.getMacAddress().equals(macAddress)) {
					targets.add(p);
					pr.setTargets(targets);
					return pr;
				}
			}
		}
		return null;
	}

	private GKAProtocolRound firstServerRound() {
		GKAProtocolRound pr = new GKAProtocolRound();
		
		PMessage pm = new PMessage(MessageAction.START_GKA_PROTOCOL, (byte)0, (byte)0, randInit.getByteLength(), randInit.getSequence());
		pr.setMessage(pm);
		
		Log.d("firstServerRound", pm.toString());
		
		List<BluetoothGKAParticipant> targets = new ArrayList<BluetoothGKAParticipant>();
		if (participants != null) {
			for (BluetoothGKAParticipant p : participants.getParticipants()) {
				if (!p.getRole().equals(GKAParticipantRole.LEADER)) targets.add(p);
			}
		}
		pr.setTargets(targets);
		
		return pr;
	}
	
	private GKAProtocolRound firstMemberRound() {
		Log.d("firstMemberRound", this.lastMessage.toString());
		GKAProtocolRound pr = new GKAProtocolRound();
		
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
		
		List<BluetoothGKAParticipant> targets = new ArrayList<BluetoothGKAParticipant>();
		if (participants != null) {
			for (BluetoothGKAParticipant p : participants.getParticipants()) {
				if (p.getRole().equals(GKAParticipantRole.LEADER)) targets.add(p);
			}
		}
		pr.setTargets(targets);
		
		return pr;
	}
	
	private BigInteger computeKeyForTwo(BigInteger doubleBlindedSecret) {
		BigInteger key = null;
		if (participants.getMe().getRole() == GKAParticipantRole.LEADER) {
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
	*/
}
