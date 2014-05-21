package cz.muni.fi.randgka.gka;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import cz.muni.fi.randgka.tools.PMessage;

public class AugotGKA implements GKAProtocol {

	private BigInteger p, g, q;

	private GKAParticipants participants;
	private SecureRandom secureRandom;
	private GKAProtocolParams gkaProtocolParams;
	private BigInteger secret, blindedSecret, secretInverse, key;
	private ByteArrayOutputStream secondRoundBroadcast;
	private int secretLength;
	private boolean initialized = false, failFlag = false;
	private List<Integer> receivedParts;
	private ByteArrayOutputStream veriString;

	public AugotGKA() {
	}

	@Override
	public void init(GKAParticipants participants, SecureRandom secureRandom, GKAProtocolParams gkaProtocolParams) {
		try {
			this.participants = participants;
			this.secureRandom = secureRandom;
			this.gkaProtocolParams = gkaProtocolParams;

			getModpGroup();
			
			initialized = true;
			
			clear();
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
		DocumentBuilder builder = dbf.newDocumentBuilder();
		Document doc = builder.parse(gkaProtocolParams.getModpFile());
		NodeList groupNodes = doc.getElementsByTagName("group");
		NodeList paramNodes;
		Node groupNode, pNode, gNode, secretLengthNode;
		Integer size;
		NamedNodeMap attributes;
		for (int i = 0; i < groupNodes.getLength(); i++) {
			groupNode = groupNodes.item(i);
			attributes = groupNode.getAttributes();
			size = Integer.parseInt(attributes.getNamedItem("size").getTextContent());
			if (size == gkaProtocolParams.getGroupKeyLength() * 8) {
				paramNodes = groupNode.getChildNodes();
				pNode = paramNodes.item(3);
				gNode = paramNodes.item(5);
				secretLengthNode = paramNodes.item(1);
				p = new BigInteger(pNode.getTextContent(), 16);
				g = new BigInteger(gNode.getTextContent(), 16);
				secretLength = Integer.parseInt(secretLengthNode.getTextContent()) / 8;
				q = p.subtract(BigInteger.ONE).divide(BigInteger.ONE.add(BigInteger.ONE));
				break;
			}
		}
	}

	private GKAProtocolRound errorRound() {
		GKAProtocolRound gkaProtocolRound = new GKAProtocolRound();
		gkaProtocolRound.setActionCode(GKAProtocolRound.ERROR);
		return gkaProtocolRound;
	}
	
	@Override
	public GKAProtocolRound nextRound(PMessage message) {
		
		try {
			if (failFlag) return null;

			PMessage response = preCreateMessage(message);
			GKAProtocolRound round = null;
			
			switch (message.getRoundNo()) {

			case (byte) 0:
				clear();
				return r0s(response);

			case (byte) 1:
				clear();
				if (gkaProtocolParams.getVersion() == 0) {
					response.setRoundNo((byte) 4);
					return r2m(message, response);
				} else {
					generateNonce();
					return r1m(message, response);
				}

			case (byte) 2:
				generateNonce();
				return r1s(message, response);

			case (byte) 3:
				participants.mergeFromTO(message.getMessage(), gkaProtocolParams.getNonceLength());
				round = r2m(message, response);
				if (gkaProtocolParams.getVersion() > 0) {
					selfSign(round);
					round.setActionCode(GKAProtocolRound.PRINT_PARTICIPANTS);
				}
				return round;

			case (byte) 4:
				if (gkaProtocolParams.getVersion() > 0) {
					if (!message.selfVerify(participants.getParticipant(message.getOriginatorId()).getAuthPublicKey(), participants.getNonces())) {
						failFlag = true;
						return errorRound();
					}
					round = r2s(message, response);
					selfSign(round);
				} else round = r2s(message, response);
				return round;

			case (byte) 5:
				if (gkaProtocolParams.getVersion() > 0 && !message.selfVerify(participants.getParticipant(message.getOriginatorId()).getAuthPublicKey(), participants.getNonces())) {
					failFlag = true;
					return errorRound();
				}
				return r3m(message, response);

			case (byte) 6:
				return r3s(message);

			case (byte) 7:
				return r4m(message);

			default:
				break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void selfSign(GKAProtocolRound round) {
		if (round != null) {
			for (Entry<GKAParticipant, PMessage> m : round.getMessages().entrySet()) {
				m.getValue().selfSign(gkaProtocolParams.getPrivateKey(),
						participants.getNonces(), null, gkaProtocolParams.getPublicKeyLength());
			}
		}
	}

	private PMessage preCreateMessage(PMessage message) {
		PMessage pMessage = new PMessage();
		pMessage.setRoundNo((byte) (message.getRoundNo() + 1));
		if (participants.getMe()!= null) pMessage.setOriginatorId(participants.getMe().getId());
		pMessage.setSigLength(0);
		pMessage.setSignature(null);
		pMessage.setSigned(false);

		return pMessage;
	}

	private GKAProtocolRound r3s(PMessage message) throws IOException {
		GKAProtocolRound round = new GKAProtocolRound();

		if (Arrays.equals(veriString.toByteArray(), message.getMessage())) {
			receivedParts.add(message.getOriginatorId());
			if (receivedParts.size() == participants.size() - 1) {
				round.setActionCode(GKAProtocolRound.SUCCESS);
			}
		} else {
			failFlag = true;
			round.setActionCode(GKAProtocolRound.ERROR);
		}
		message.setRoundNo((byte) 7);
		
		for (GKAParticipant p : participants.getAllButMe().participants) {
			if (p.equals(message.getOriginatorId())) {
				PMessage lMessage = new PMessage((byte)7, participants.getMe().getId(), veriString.size(), 0, false, veriString.toByteArray(), null);
				round.put(p, lMessage);
			} else round.put(p, message);
		}

		return round;
	}

	private GKAProtocolRound r4m(PMessage message) {
		GKAProtocolRound round = new GKAProtocolRound();
		
		if (Arrays.equals(veriString.toByteArray(), message.getMessage())) {
			receivedParts.add(message.getOriginatorId());
			if (receivedParts.size() == participants.size() - 1) {
				round.setActionCode(GKAProtocolRound.SUCCESS);
			}
		} else {
			failFlag = true;
			round.setActionCode(GKAProtocolRound.ERROR);
		}

		return round;
	}

	private GKAProtocolRound r1s(PMessage message, PMessage response) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
		GKAProtocolRound round = new GKAProtocolRound();

		if (!receivedParts.contains(message.getOriginatorId())) {
			receivedParts.add(message.getOriginatorId());

			byte[] memNonce = new byte[gkaProtocolParams.getNonceLength()];
			System.arraycopy(message.getMessage(), 0, memNonce, 0, gkaProtocolParams.getNonceLength());

			byte[] intBytes = new byte[4];
			System.arraycopy(message.getMessage(), gkaProtocolParams.getNonceLength(), intBytes, 0, 4);
			int pkEncLen = ByteBuffer.wrap(intBytes).getInt();

			byte[] pkEnc = new byte[pkEncLen];
			System.arraycopy(message.getMessage(), 4 + gkaProtocolParams.getNonceLength(), pkEnc, 0, pkEncLen);

			GKAParticipant participant = participants.getParticipant(message.getOriginatorId());
			participant.setNonce(memNonce);

			PublicKey memPK = KeyFactory.getInstance("RSA", "BC").generatePublic(new X509EncodedKeySpec(pkEnc));
			participant.setAuthPublicKey(memPK);
		}

		if (receivedParts.size() == participants.size() - 1) {
			byte[] newMessage = participants.getTransferObject();
			response.setLength(newMessage.length);
			response.setMessage(newMessage);
			round.put(participants.getAllButMe(), response);

			receivedParts.clear();
		}
		round.setActionCode(GKAProtocolRound.PRINT_PARTICIPANTS);
		return round;
	}

	private GKAProtocolRound r1m(PMessage message, PMessage response) throws IOException {
		ByteArrayOutputStream bs = new ByteArrayOutputStream();

		bs.write(participants.getMe().getNonce());

		int pkEncLen = participants.getMe().getAuthPublicKey().getEncoded().length;
		byte[] intBytes = ByteBuffer.allocate(4).putInt(pkEncLen).array();
		bs.write(intBytes);
		bs.write(participants.getMe().getAuthPublicKey().getEncoded());

		response.setLength(bs.size());
		response.setMessage(bs.toByteArray());

		GKAProtocolRound round = new GKAProtocolRound();
		round.put(participants.getLeader(), response);
		
		return round;
	}

	private GKAProtocolRound r2s(PMessage message, PMessage response) throws IOException, NoSuchAlgorithmException {
		GKAProtocolRound round = new GKAProtocolRound();

		GKAParticipant originator = participants.getParticipant(message.getOriginatorId());
		if (!receivedParts.contains(originator.getId())) {
			receivedParts.add(originator.getId());

			byte[] keyBytes = new byte[gkaProtocolParams.getGroupKeyLength()];
			System.arraycopy(message.getMessage(), 0, keyBytes, 0, gkaProtocolParams.getGroupKeyLength());

			BigInteger receivedPublic = new BigInteger(1, keyBytes);
			BigInteger compoundPublic = receivedPublic.modPow(secret, p);
			key = key.multiply(compoundPublic).mod(p);

			originator.setDHPublicKey(receivedPublic);
			participants.merge(originator);

			byte[] originatorIdArray = ByteBuffer.allocate(4).putInt(message.getOriginatorId()).array();
			secondRoundBroadcast.write(originatorIdArray);
			secondRoundBroadcast.write(keyBytes);
			secondRoundBroadcast.write(compoundPublic.toByteArray(), (compoundPublic.toByteArray().length > gkaProtocolParams.getGroupKeyLength()) ? 1 : 0, gkaProtocolParams.getGroupKeyLength());

			if (receivedParts.size() == participants.size() - 1) {
				response.setLength(secondRoundBroadcast.size());
				response.setMessage(secondRoundBroadcast.toByteArray());

				round.put(participants.getAllButMe(), response);
				
				key = key.multiply(blindedSecret).mod(p);
				
				if (gkaProtocolParams.getVersion() != GKAProtocolParams.AUTH_CONF) round.setActionCode(GKAProtocolRound.SUCCESS);
				else setVeriString();
				
				receivedParts.clear();
			}
		}

		return round;
	}
	
	private void setVeriString() throws IOException, NoSuchAlgorithmException {
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		bs.write(participants.getNonces());
		bs.write(key.toByteArray());
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		veriString = new ByteArrayOutputStream();
		veriString.write(md.digest(bs.toByteArray()));
	
	}

	private GKAProtocolRound r2m(PMessage message, PMessage response) {

		GKAProtocolRound round = new GKAProtocolRound();
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		bs.write(blindedSecret.toByteArray(), (blindedSecret.toByteArray().length > gkaProtocolParams.getGroupKeyLength()) ? 1 : 0, gkaProtocolParams.getGroupKeyLength());
		response.setLength(gkaProtocolParams.getGroupKeyLength());
		response.setMessage(bs.toByteArray());

		round.put(participants.getLeader(), response);
		return round;
	}

	private GKAProtocolRound r3m(PMessage message, PMessage response) throws IOException, NoSuchAlgorithmException {
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

			System.arraycopy(message.getMessage(), offset + 4, currentKeyArray, 0, gkaProtocolParams.getGroupKeyLength());
			System.arraycopy(message.getMessage(), offset + 4 + gkaProtocolParams.getGroupKeyLength(), currentCompoundKeyArray, 0, gkaProtocolParams.getGroupKeyLength());

			originator.setDHPublicKey(new BigInteger(1, currentKeyArray));
			participants.merge(originator);

			BigInteger compoundPublic = new BigInteger(1,
					currentCompoundKeyArray);
			compoundPublic = compoundPublic.mod(p);
			key = key.multiply(compoundPublic).mod(p);
			if (originator.equals(participants.getMe())) key = key.multiply(compoundPublic.modPow(secretInverse, p)).mod(p);

			offset += 4+2*gkaProtocolParams.getGroupKeyLength();
		}

		receivedParts.clear();
		
		if (gkaProtocolParams.getVersion() == GKAProtocolParams.AUTH_CONF) {
			setVeriString();
			response.setMessage(veriString.toByteArray());
			response.setLength(veriString.size());
			round.put(participants.getLeader(), response);
		} else {
			round.setActionCode(GKAProtocolRound.SUCCESS);
		}
		
		return round;
	}

	@Override
	public BigInteger getKey() {
		return key;
	}

	public boolean isInitialized() {
		return initialized;
	}

	private byte[] generateNonce() {
		byte[] nonce = new byte[gkaProtocolParams.getNonceLength()];
		secureRandom.nextBytes(nonce);
		participants.getMe().setNonce(nonce);

		return nonce;
	}

	private GKAProtocolRound r0s(PMessage response) throws IOException {
		GKAProtocolRound round = new GKAProtocolRound();

		byte[] intBytes;
		ByteArrayOutputStream bs = new ByteArrayOutputStream();

		for (GKAParticipant p : participants.getAllButMe().getParticipants()) {
			intBytes = ByteBuffer.allocate(4).putInt(p.getId()).array();
			bs.write(intBytes);
			intBytes = ByteBuffer.allocate(4).putInt(gkaProtocolParams.getVersion()).array();
			bs.write(intBytes);
			intBytes = ByteBuffer.allocate(4).putInt(gkaProtocolParams.getNonceLength()).array();
			bs.write(intBytes);
			intBytes = ByteBuffer.allocate(4).putInt(gkaProtocolParams.getGroupKeyLength()).array();
			bs.write(intBytes);
			intBytes = ByteBuffer.allocate(4).putInt(gkaProtocolParams.getPublicKeyLength()).array();
			bs.write(intBytes);

			response.setMessage(bs.toByteArray());
			response.setLength(bs.size());

			round.put(p, response);
		}
		return round;
	}
	
	private void clear() {
		for (GKAParticipant p : participants.getParticipants()) {
			p.setDHPublicKey(null);
		}
		
		failFlag = false;
		
		if (receivedParts == null) this.receivedParts = new ArrayList<Integer>();
		else receivedParts.clear();
		
		secondRoundBroadcast = new ByteArrayOutputStream();

		byte[] secretBytes = new byte[secretLength];
		secureRandom.nextBytes(secretBytes);
		secret = new BigInteger(1, secretBytes).mod(q);
		blindedSecret = g.modPow(secret, p);
		secretInverse = secret.modInverse(q);
		key = BigInteger.ONE;
		failFlag = false;
	}
}
