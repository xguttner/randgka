package cz.muni.fi.randgka.gka;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.util.Base64;
import android.util.Log;
import cz.muni.fi.randgka.tools.MessageAction;
import cz.muni.fi.randgka.tools.PMessage;
import cz.muni.fi.randgka.tools.ProtocolType;

public class AugotGKA implements GKAProtocol {

	private static final ProtocolType PROTOCOL_TYPE = ProtocolType.AUGOT;
	
	private static final String modpgroupFile = "modpgroups.xml";
	
	private BigInteger p, g, q;
	
	private GKAParticipants participants;
	private SecureRandom secureRandom;
	private GKAProtocolParams gkaProtocolParams;
	private BigInteger secret,
			blindedSecret,
			secretInverse,
			key;
	private byte[] secondRoundBroadcast;
	private int secretLength,
			secondRoundBroadcastLength,
			secondRoundBroadcastPartLength,
			partsReceived;
	private boolean initialized = false;
	private List<Integer> receivedParts;
	
	public AugotGKA() {
	}
	
	@Override
	public void init(GKAParticipants participants, SecureRandom secureRandom, GKAProtocolParams gkaProtocolParams) {
		try {
			getModpGroup();
			
			this.participants = participants;
			this.secureRandom = secureRandom;
			this.gkaProtocolParams = gkaProtocolParams;
			if (participants != null) {
				partsReceived = 0;
				secondRoundBroadcastPartLength = (4 + gkaProtocolParams.getGroupKeyLength()*2);
				this.secondRoundBroadcastLength = (participants.size()-1) * secondRoundBroadcastPartLength;
				this.secondRoundBroadcast = new byte[secondRoundBroadcastLength];
			}
			
			byte[] secretBytes = new byte[secretLength];
			secureRandom.nextBytes(secretBytes);
			secret = new BigInteger(1, secretBytes);
			secret = secret.mod(q);
			blindedSecret = g.modPow(secret, p);
			secretInverse = secret.modInverse(q); 
			key = BigInteger.ONE;
			initialized = true;
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void getModpGroup() throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        DocumentBuilder builder;
		builder = dbf.newDocumentBuilder();
        Document doc = builder.parse(modpgroupFile);
        NodeList groupNodes = doc.getElementsByTagName("group");
        NodeList paramNodes;
        Node groupNode, pNode, gNode, secretLengthNode;
        Integer size;
        NamedNodeMap attributes;
        for (int i = 0; i < groupNodes.getLength(); i++) {
        	groupNode = groupNodes.item(i);
        	attributes = groupNode.getAttributes();
        	size = Integer.parseInt(attributes.getNamedItem("size").getTextContent());
        	if (size == gkaProtocolParams.getGroupKeyLength()) {
        		paramNodes = groupNode.getChildNodes();
        		pNode = paramNodes.item(1);
        		gNode = paramNodes.item(2);
        		secretLengthNode = paramNodes.item(0);
        		p = new BigInteger(pNode.getTextContent(), 16);
        		g = new BigInteger(gNode.getTextContent(), 16);
        		secretLength = Integer.parseInt(secretLengthNode.getTextContent());
        		q = p.subtract(BigInteger.ONE).divide(BigInteger.ONE.add(BigInteger.ONE));
        		break;
        	}
        }
	}
	
	@Override
	public GKAProtocolRound nextRound(PMessage message) {
		
		if (message.getRoundNo() != 0 && gkaProtocolParams.getVersion()>0) {
			Log.d("pubkey", participants.getParticipant(message.getOriginatorId()).getAuthPublicKey().toString());
			if (!message.selfVerify(participants.getParticipant(message.getOriginatorId()).getAuthPublicKey(), participants.getNonces())) {
				GKAProtocolRound gkaProtocolRound = new GKAProtocolRound();
				gkaProtocolRound.setActionCode(GKAProtocolRound.ERROR);
				return gkaProtocolRound;
			}
		}
		
		switch (message.getRoundNo()) {
		
			case (byte)0:
				return initRound();
			
			case (byte)1:
				return firstMemberRound(message);
			
			case (byte)2:
				return firstServerRound(message);
			
			case (byte)3:
				return secondMemberRound(message);
				
			case (byte)4:
				return secondServerRound(message);
			
			case (byte)5:
				return thirdMemberRound(message);
			
			case (byte)6:
				break;
			
			default:
				break;
		}
		return null;
	}

	private GKAProtocolRound firstServerRound(PMessage message) {
		GKAProtocolRound round = new GKAProtocolRound();
		PMessage pMessage = new PMessage();
		
		pMessage.setRoundNo((byte)3);
		pMessage.setAction(MessageAction.GKA_PROTOCOL);
		pMessage.setOriginatorId(participants.getMe().getId());
		pMessage.setSigLength(gkaProtocolParams.getPublicKeyLength());
		
		byte[] nonce = new byte[gkaProtocolParams.getNonceLength()];
		secureRandom.nextBytes(nonce);
		participants.getMe().setNonce(nonce);
		
		if (!receivedParts.contains(message.getOriginatorId())) {
			receivedParts.add(message.getOriginatorId());
			
			byte[] memNonce = new byte[gkaProtocolParams.getNonceLength()];
			System.arraycopy(message.getMessage(), 0, memNonce, 0, gkaProtocolParams.getNonceLength());
			
			byte[] intBytes = new byte[4];
			System.arraycopy(message.getMessage(), gkaProtocolParams.getNonceLength(), intBytes, 0, 4);
			int pkEncLen = ByteBuffer.wrap(intBytes).getInt();
			
			byte[] pkEnc = new byte[pkEncLen];
			System.arraycopy(message.getMessage(), 4+gkaProtocolParams.getNonceLength(), pkEnc, 0, pkEncLen);
			
			GKAParticipant participant = participants.getParticipant(message.getOriginatorId());
			participant.setNonce(memNonce);
			try {
				PublicKey memPK = KeyFactory.getInstance("RSA", "BC").generatePublic(new X509EncodedKeySpec(pkEnc));
				participant.setAuthPublicKey(memPK);
			} catch (InvalidKeySpecException e) {
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (NoSuchProviderException e) {
				e.printStackTrace();
			}
		}
		
		if (receivedParts.size() == participants.size()-1) {
			byte[] newMessage = participants.getTransferObject();
			pMessage.setLength(newMessage.length);
			pMessage.setMessage(newMessage);
			round.put(participants.getAllButMe(), pMessage);
			
			receivedParts.clear();
		}
		return round;
	}

	private GKAProtocolRound firstMemberRound(PMessage message) {
		PMessage pMessage = new PMessage();
		pMessage.setRoundNo((byte)2);
		pMessage.setAction(MessageAction.GKA_PROTOCOL);
		pMessage.setOriginatorId(participants.getMe().getId());
		
		byte[] nonce = new byte[gkaProtocolParams.getNonceLength()];
		secureRandom.nextBytes(nonce);
		participants.getMe().setNonce(nonce);
		
		// length: member's nonce + public key length
		int messageLength = 4+gkaProtocolParams.getNonceLength() + gkaProtocolParams.getPublicKeyLength();
		byte[] messageArray = new byte[messageLength];
		
		System.arraycopy(nonce, 0, messageArray, 0, gkaProtocolParams.getNonceLength());
		int pkEncLen = participants.getMe().getAuthPublicKey().getEncoded().length;
		byte[] intBytes = ByteBuffer.allocate(4).putInt(pkEncLen).array();
		System.arraycopy(intBytes, 0, messageArray, gkaProtocolParams.getNonceLength(), 4);
		System.arraycopy(participants.getMe().getAuthPublicKey().getEncoded(), 0, messageArray, 4+gkaProtocolParams.getNonceLength(), pkEncLen);
		
		pMessage.setLength(messageLength);
		pMessage.setMessage(messageArray);
		
		GKAProtocolRound round = new GKAProtocolRound();
		round.put(participants.getLeader(), pMessage);
		Log.d("firstRound", round.toString());
		return round;
	}
	
	private GKAProtocolRound secondServerRound(PMessage message) {
		GKAProtocolRound round = new GKAProtocolRound();
		PMessage pMessage = new PMessage();
		
		GKAParticipant originator = participants.getParticipant(message.getOriginatorId());
		if (receivedParts.contains(originator.getId())) {
				byte[] keyBytes = new byte[gkaProtocolParams.getGroupKeyLength()];
				Log.d("arrayM", Arrays.toString(message.getMessage()));
				System.arraycopy(message.getMessage(), 0, keyBytes, 0, gkaProtocolParams.getGroupKeyLength());
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
				System.arraycopy(keyBytes, 0, secondRoundBroadcast, 4, gkaProtocolParams.getGroupKeyLength());
				System.arraycopy(compoundPublic.toByteArray(), (compoundPublic.toByteArray().length>gkaProtocolParams.getGroupKeyLength())?1:0, secondRoundBroadcast, 4+gkaProtocolParams.getGroupKeyLength(), gkaProtocolParams.getGroupKeyLength());
				
				if (partsReceived == participants.size()-1) {
					pMessage.setRoundNo((byte)5);
					pMessage.setAction(MessageAction.GKA_PROTOCOL);
					pMessage.setOriginatorId(participants.getMe().getId());
					
					pMessage.setLength(secondRoundBroadcastLength);
					pMessage.setMessage(secondRoundBroadcast);
						
					if (gkaProtocolParams.getVersion()> 0) pMessage.selfSign(gkaProtocolParams.getPrivateKey(), participants.getNonces(), null);
					
					round.put(participants.getAllButMe(), pMessage);
					round.setActionCode(GKAProtocolRound.SUCCESS);
				}
		}
		
		Log.d("secondRound", round.toString());
		return round;
	}
	
	private GKAProtocolRound secondMemberRound(PMessage message) {
		
		GKAProtocolRound round = new GKAProtocolRound();
		PMessage pMessage = new PMessage();
		pMessage.setAction(MessageAction.GKA_PROTOCOL);
		pMessage.setRoundNo((byte)4);
		pMessage.setOriginatorId(participants.getMe().getId());
		pMessage.setSigLength(gkaProtocolParams.getPublicKeyLength());
		byte[] messageArray = new byte[gkaProtocolParams.getGroupKeyLength()];
		System.arraycopy(blindedSecret.toByteArray(), (blindedSecret.toByteArray().length>gkaProtocolParams.getGroupKeyLength())?1:0, messageArray, gkaProtocolParams.getNonceLength()+4, gkaProtocolParams.getGroupKeyLength());
		
		Log.d("array", Arrays.toString(blindedSecret.toByteArray())+" "+blindedSecret.toByteArray().length);
		Log.d("receivedPublic", blindedSecret.toString(16));  
		pMessage.setLength(gkaProtocolParams.getGroupKeyLength());
		pMessage.setMessage(messageArray);
		
		if (gkaProtocolParams.getVersion() > 0) pMessage.selfSign(gkaProtocolParams.getPrivateKey(), participants.getNonces(), null);
		
		round.put(participants.getLeader(), pMessage);
		return round;
	}
	
	private GKAProtocolRound thirdMemberRound(PMessage message) {
		GKAProtocolRound round = new GKAProtocolRound();
		
		int currentId;
		GKAParticipant originator = null;
		int offset = 0;
		
		while (offset < message.getLength()) {
			
			byte[] currentIdArray = new byte[4];
			byte[] currentKeyArray = new byte[gkaProtocolParams.getGroupKeyLength()];
			byte[] currentCompoundKeyArray = new byte[gkaProtocolParams.getGroupKeyLength()];
			
			System.arraycopy(message.getMessage(), offset, currentIdArray, 0, 4);
			currentId = ByteBuffer.wrap(currentIdArray).getInt();
			originator = participants.getParticipant(currentId);
			
				System.arraycopy(message.getMessage(), offset+4, currentKeyArray, 0, gkaProtocolParams.getGroupKeyLength());
				System.arraycopy(message.getMessage(), offset+4+gkaProtocolParams.getGroupKeyLength(), currentCompoundKeyArray, 0, gkaProtocolParams.getGroupKeyLength());
				
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
			Log.d("publicKey", blindedSecret.toString(16));
			key = key.multiply(blindedSecret).mod(p);
			Log.d("key2", key.toString(16));
		}
		return key;
	}

	public boolean isInitialized() {
		return initialized;
	}

	@Override
	public ProtocolType getProtocolType() {
		return PROTOCOL_TYPE;
	}
	
	private GKAProtocolRound initRound() {
		GKAProtocolRound round = new GKAProtocolRound();
		
		receivedParts = new ArrayList<Integer>();
		
		int messageLength = 14;
		byte[] intBytes;
		for (GKAParticipant p : participants.getAllButMe().getParticipants()) {
			byte[] message = new byte[messageLength];
			message[0] = gkaProtocolParams.getVersion();
			message[1] = (byte)p.getId();
			intBytes = ByteBuffer.allocate(4).putInt(gkaProtocolParams.getNonceLength()).array();
			System.arraycopy(intBytes, 0, message, 2, 4);
			intBytes = ByteBuffer.allocate(4).putInt(gkaProtocolParams.getGroupKeyLength()).array();
			System.arraycopy(intBytes, 0, message, 6, 4);
			intBytes = ByteBuffer.allocate(4).putInt(gkaProtocolParams.getPublicKeyLength()).array();
			System.arraycopy(intBytes, 0, message, 10, 4);
			
			PMessage paramsMessage = new PMessage(MessageAction.INIT_GKA_PROTOCOL, (byte)1, (byte)0, messageLength, 0, false, message, null);
			round.put(participants, paramsMessage);
		}
		return round;
	}
}
