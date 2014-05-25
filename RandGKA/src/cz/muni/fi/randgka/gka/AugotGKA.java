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

import android.util.Log;
import cz.muni.fi.randgka.tools.PMessage;

/**
 * Augot protocol
 */
public class AugotGKA implements GKAProtocol {

	// protocol parameters for group of integers modulo p: p - safe prime, g - group generator, q = (p-1)/2 
	private BigInteger p, g, q;
	// protocol participants
	private GKAParticipants participants;
	// randomness provider
	private SecureRandom secureRandom;
	// protocol parameters
	private GKAProtocolParams gkaProtocolParams;
	private BigInteger secret, // participant's random secret
		blindedSecret, // secret blinded by g^(secret) mod p
		secretInverse, // inversed secret
		key; // resulting shared key
	// field for leader's second round broadcast message collection
	private ByteArrayOutputStream secondRoundBroadcast;
	private int secretLength; // length of the random secret
	private boolean initialized = false, failFlag = false;
	private List<Integer> receivedParts; // parts collected by leader in current round
	private ByteArrayOutputStream verificationToken; // verification round

	public AugotGKA() {}

	@Override
	public void init(GKAParticipants participants, SecureRandom secureRandom, GKAProtocolParams gkaProtocolParams) {
		try {
			this.participants = participants;
			this.secureRandom = secureRandom;
			this.gkaProtocolParams = gkaProtocolParams;

			getModpGroup();
			
			initialized = true;
			
			initRun();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get modulo group parameters from the source file
	 * 
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	private void getModpGroup() throws ParserConfigurationException, SAXException, IOException {
		// source document parsing
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setValidating(false);
		DocumentBuilder builder = dbf.newDocumentBuilder();
		Document doc = builder.parse(gkaProtocolParams.getModpFile());
		
		NodeList groupNodes = doc.getElementsByTagName("group");
		NodeList paramNodes;
		Node groupNode, pNode, gNode, secretLengthNode;
		Integer size;
		NamedNodeMap attributes;
		// find the appropriate group according to the wanted key length
		for (int i = 0; i < groupNodes.getLength(); i++) {
			groupNode = groupNodes.item(i);
			attributes = groupNode.getAttributes();
			size = Integer.parseInt(attributes.getNamedItem("size").getTextContent());
			// group params found
			if (size == gkaProtocolParams.getGroupKeyLength() * 8) {
				paramNodes = groupNode.getChildNodes();
				pNode = paramNodes.item(3);
				gNode = paramNodes.item(5);
				secretLengthNode = paramNodes.item(1);
				// set all params
				p = new BigInteger(pNode.getTextContent(), 16);
				g = new BigInteger(gNode.getTextContent(), 16);
				secretLength = Integer.parseInt(secretLengthNode.getTextContent()) / 8;
				q = p.subtract(BigInteger.ONE).divide(BigInteger.ONE.add(BigInteger.ONE));
				break;
			}
		}
	}

	/**
	 * Simply returns a round symbolizing error.
	 * 
	 * @return error round
	 */
	private GKAProtocolRound errorRound() {
		GKAProtocolRound gkaProtocolRound = new GKAProtocolRound();
		gkaProtocolRound.setActionCode(GKAProtocolRound.ERROR);
		return gkaProtocolRound;
	}
	
	@Override
	public GKAProtocolRound nextRound(PMessage message) {
		try {
			// protocol has already failed
			if (failFlag) return null;

			PMessage response = preCreateMessage(message);
			GKAProtocolRound round = null;
			
			switch (message.getRoundNo()) {
			
				// get first server round - init round
				case (byte) 0:
					initRun();
					return r0s(response);
					
				// get first member round
				case (byte) 1:
					initRun();
					// non-authenticated instance
					if (gkaProtocolParams.getVersion() == GKAProtocolParams.NON_AUTH) {
						response.setRoundNo((byte) 4);
						return r2m(message, response);
					} 
					// authenticated (+ key confirmation) version
					else {
						generateNonce();
						return r1m(message, response);
					}
					
				// broadcast participants
				case (byte) 2:
					generateNonce();
					return r1s(message, response);
	
				// member sends blinded secret to leader
				case (byte) 3:
					// merge all participants received from the server
					participants.fromTransferObject(message.getMessage(), gkaProtocolParams.getNonceLength());
					round = r2m(message, response);
					// sign if protocol instance is authenticated
					if (gkaProtocolParams.getVersion() != GKAProtocolParams.NON_AUTH) {
						selfSign(round);
						round.setActionCode(GKAProtocolRound.PRINT_PARTICIPANTS);
					}
					return round;
	
				// leader broadcasts double-blinded secrets
				case (byte) 4:
					// verify and sign if protocol instance is authenticated
					if (gkaProtocolParams.getVersion() != GKAProtocolParams.NON_AUTH) {
						if (!message.selfVerify(participants.getParticipant(message.getOriginatorId()).getPublicKey(), participants.getNonces())) {
							failFlag = true;
							return errorRound();
						}
						round = r2s(message, response);
						selfSign(round);
					} 
					// simple non-authenticated round
					else round = r2s(message, response);
					return round;
	
				// compute the key
				case (byte) 5:
					if (gkaProtocolParams.getVersion() != GKAProtocolParams.NON_AUTH && !message.selfVerify(participants.getParticipant(message.getOriginatorId()).getPublicKey(), participants.getNonces())) {
						failFlag = true;
						return errorRound();
					}
					return r3m(message, response);
	
				// broadcast verification
				case (byte) 6:
					return r3s(message);
	
				// verify
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

	/**
	 * Sign all messages in the given round by digital signature 
	 * using participant's private key
	 * 
	 * @param round - round to sign
	 */
	private void selfSign(GKAProtocolRound round) {
		if (round != null) {
			for (Entry<GKAParticipant, PMessage> m : round.getMessages().entrySet()) {
				m.getValue().selfSign(gkaProtocolParams.getPrivateKey(), participants.getNonces(), null, gkaProtocolParams.getPublicKeyLength());
			}
		}
	}

	/**
	 * Precreate the responding message
	 * 
	 * @param message - message to be followed by the precreated message
	 * @return
	 */
	private PMessage preCreateMessage(PMessage message) {
		PMessage pMessage = new PMessage();
		pMessage.setRoundNo((byte) (message.getRoundNo() + 1));
		if (participants.getMe()!= null) pMessage.setOriginatorId(participants.getMe().getId());
		pMessage.setSigLength(0);
		pMessage.setSignature(null);
		pMessage.setSigned(false);
		return pMessage;
	}
	
	/**
	 * Initialization round leader -> member
	 * 
	 * @param response - precreated response message
	 * @return round to be broadcasted
	 * @throws IOException
	 */
	private GKAProtocolRound r0s(PMessage response) throws IOException {
		GKAProtocolRound round = new GKAProtocolRound();

		byte[] intBytes = new byte[4];
		
		// set appropriate params to each participant
		for (GKAParticipant p : participants.getAllButMe().getParticipants()) {
			ByteArrayOutputStream bs = new ByteArrayOutputStream();
			// participants universal id
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

			PMessage responseNew = new PMessage(response.getRoundNo(), response.getOriginatorId(), 0, 0, false, null, null);
			responseNew.setMessage(bs.toByteArray());
			responseNew.setLength(bs.size());

			round.put(p, responseNew);
		}
		return round;
	}

	/**
	 * Authentication initialization:
	 * member -> leader : random nonce + public key
	 * 
	 * @param message - original message
	 * @param response - precreated response message
	 * @return round to send to the leader
	 * @throws IOException
	 */
	private GKAProtocolRound r1m(PMessage message, PMessage response) throws IOException {
		ByteArrayOutputStream bs = new ByteArrayOutputStream();

		// set random nonce
		bs.write(participants.getMe().getNonce());

		// set encoded public key + its length
		int pkEncLen = participants.getMe().getPublicKey().getEncoded().length;
		byte[] intBytes = ByteBuffer.allocate(4).putInt(pkEncLen).array();
		bs.write(intBytes);
		bs.write(participants.getMe().getPublicKey().getEncoded());

		response.setLength(bs.size());
		response.setMessage(bs.toByteArray());

		// send the response only to the leader
		GKAProtocolRound round = new GKAProtocolRound();
		round.put(participants.getLeader(), response);
		
		return round;
	}
	
	/**
	 * Authentication propagation:
	 * leader -> members : nonces + public keys
	 * 
	 * @param message - message by one of the participants
	 * @param response - precreated response message
	 * @return response broadcasting round, if all needed parts have been collected, empty response otherwise
	 * @throws InvalidKeySpecException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 */
	private GKAProtocolRound r1s(PMessage message, PMessage response) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
		GKAProtocolRound round = new GKAProtocolRound();
		
		// process new nonce + public key
		if (!receivedParts.contains(message.getOriginatorId())) {
			receivedParts.add(message.getOriginatorId());

			// nonce
			byte[] memNonce = new byte[gkaProtocolParams.getNonceLength()];
			System.arraycopy(message.getMessage(), 0, memNonce, 0, gkaProtocolParams.getNonceLength());

			// encoded public key length
			byte[] intBytes = new byte[4];
			System.arraycopy(message.getMessage(), gkaProtocolParams.getNonceLength(), intBytes, 0, 4);
			int pkEncLen = ByteBuffer.wrap(intBytes).getInt();

			// encoded public key
			byte[] pkEnc = new byte[pkEncLen];
			System.arraycopy(message.getMessage(), 4 + gkaProtocolParams.getNonceLength(), pkEnc, 0, pkEncLen);

			// assign the values to appropriate participant object
			GKAParticipant participant = participants.getParticipant(message.getOriginatorId());
			participant.setNonce(memNonce);
			PublicKey memPK = KeyFactory.getInstance("RSA", "BC").generatePublic(new X509EncodedKeySpec(pkEnc));
			participant.setPublicKey(memPK);
		}

		// if all the needed parts have been collected - make broadcast round
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
	
	/**
	 * member -> leader : member's blinded secret
	 * 
	 * @param message - original message
	 * @param response - precreated response message
	 * @return round containing message to the leader with blinded secret
	 */
	private GKAProtocolRound r2m(PMessage message, PMessage response) {
		GKAProtocolRound round = new GKAProtocolRound();
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		bs.write(blindedSecret.toByteArray(), (blindedSecret.toByteArray().length > gkaProtocolParams.getGroupKeyLength()) ? 1 : 0, gkaProtocolParams.getGroupKeyLength());
		response.setLength(gkaProtocolParams.getGroupKeyLength());
		response.setMessage(bs.toByteArray());

		round.put(participants.getLeader(), response);
		return round;
	}

	/**
	 * Broadcast double-blinded secrets:
	 * leader -> members : double-blinded secrets
	 * 
	 * @param message - original message
	 * @param response - precreated response message
	 * @return round with double-blinded secrets, if all have been collected, empty round otherwise
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	private GKAProtocolRound r2s(PMessage message, PMessage response) throws IOException, NoSuchAlgorithmException {
		GKAProtocolRound round = new GKAProtocolRound();
		GKAParticipant originator = participants.getParticipant(message.getOriginatorId());
		
		// process new received blinded secret
		if (!receivedParts.contains(originator.getId())) {
			receivedParts.add(originator.getId());

			// get blinded secret
			byte[] keyBytes = new byte[gkaProtocolParams.getGroupKeyLength()];
			System.arraycopy(message.getMessage(), 0, keyBytes, 0, gkaProtocolParams.getGroupKeyLength());
			BigInteger receivedPublic = new BigInteger(1, keyBytes);
			
			// get double-blinded secret
			BigInteger compoundPublic = receivedPublic.modPow(secret, p);
			
			// contribute to the key
			key = key.multiply(compoundPublic).mod(p);

			// collect information into the following broadcast
			byte[] originatorIdArray = ByteBuffer.allocate(4).putInt(message.getOriginatorId()).array();
			secondRoundBroadcast.write(originatorIdArray);
			secondRoundBroadcast.write(keyBytes);
			secondRoundBroadcast.write(compoundPublic.toByteArray(), (compoundPublic.toByteArray().length > gkaProtocolParams.getGroupKeyLength()) ? 1 : 0, gkaProtocolParams.getGroupKeyLength());

			// if all needed parts have been collected, broadcast the double-blinded secrets
			if (receivedParts.size() == participants.size() - 1) {
				response.setLength(secondRoundBroadcast.size());
				response.setMessage(secondRoundBroadcast.toByteArray());

				round.put(participants.getAllButMe(), response);
				
				// compute resulting key by leader's blinded secret multiplication
				key = key.multiply(blindedSecret).mod(p);
				
				// if we don't want the confirmation - key ok to retrieve
				if (gkaProtocolParams.getVersion() != GKAProtocolParams.AUTH_CONF) round.setActionCode(GKAProtocolRound.SUCCESS);
				// if we want to confirm, set the verification token
				else setVerificationToken();
				
				receivedParts.clear();
			}
		}

		return round;
	}
	
	/**
	 * receive broadcast to compute the resulting key
	 * 
	 * @param message - original message
	 * @param response - precreated response message
	 * @return
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	private GKAProtocolRound r3m(PMessage message, PMessage response) throws IOException, NoSuchAlgorithmException {
		GKAProtocolRound round = new GKAProtocolRound();

		int currentId; // id of participant regarding current message piece
		GKAParticipant originator = null;
		int offset = 0; // message offset
		while (offset < message.getLength()) {

			byte[] currentIdArray = new byte[4];
			byte[] currentKeyArray = new byte[gkaProtocolParams.getGroupKeyLength()];
			byte[] currentCompoundKeyArray = new byte[gkaProtocolParams.getGroupKeyLength()];
			
			// get the originator
			System.arraycopy(message.getMessage(), offset, currentIdArray, 0, 4);
			currentId = ByteBuffer.wrap(currentIdArray).getInt();
			originator = participants.getParticipant(currentId);

			// get the originator's blinded secret
			System.arraycopy(message.getMessage(), offset + 4, currentKeyArray, 0, gkaProtocolParams.getGroupKeyLength());
			// get the originator's double-blinded secret
			System.arraycopy(message.getMessage(), offset + 4 + gkaProtocolParams.getGroupKeyLength(), currentCompoundKeyArray, 0, gkaProtocolParams.getGroupKeyLength());

			BigInteger compoundPublic = new BigInteger(1, currentCompoundKeyArray);
			compoundPublic = compoundPublic.mod(p);
			
			// contribute to the resulting key
			key = key.multiply(compoundPublic).mod(p);
			
			// if double-blinded secret belongs to this device participant - contribute by leader's blinded secret
			if (originator != null && originator.equals(participants.getMe())) key = key.multiply(compoundPublic.modPow(secretInverse, p)).mod(p);

			offset += 4+2*gkaProtocolParams.getGroupKeyLength();
		}
		receivedParts.clear();
		
		// key confirmation round - send to the leader
		if (gkaProtocolParams.getVersion() == GKAProtocolParams.AUTH_CONF) {
			setVerificationToken();
			response.setMessage(verificationToken.toByteArray());
			response.setLength(verificationToken.size());
			round.put(participants.getLeader(), response);
		} 
		// report success
		else round.setActionCode(GKAProtocolRound.SUCCESS);
		
		return round;
	}

	/**
	 * Leader broadcasts all participants' key confirmation info.
	 * 
	 * @param message - original message
	 * @return
	 * @throws IOException
	 */
	private GKAProtocolRound r3s(PMessage message) throws IOException {
		GKAProtocolRound round = new GKAProtocolRound();
		
		// successfully verified for current message originator
		if (Arrays.equals(verificationToken.toByteArray(), message.getMessage())) {
			receivedParts.add(message.getOriginatorId());
			// all parts successfully verified
			if (receivedParts.size() == participants.size() - 1) {
				round.setActionCode(GKAProtocolRound.SUCCESS);
			}
		} 
		// verification failed - propagate error
		else {
			failFlag = true;
			round.setActionCode(GKAProtocolRound.ERROR);
		}
		message.setRoundNo((byte) 7);
		
		// broadcast the received message
		for (GKAParticipant p : participants.getAllButMe().participants) {
			if (p.equals(message.getOriginatorId())) {
				PMessage lMessage = new PMessage((byte)7, participants.getMe().getId(), verificationToken.size(), 0, false, verificationToken.toByteArray(), null);
				round.put(p, lMessage);
			} else round.put(p, message);
		}

		return round;
	}

	/**
	 * Member's verification
	 * 
	 * @param message
	 * @return
	 */
	private GKAProtocolRound r4m(PMessage message) {
		GKAProtocolRound round = new GKAProtocolRound();
		
		// successfully verified for current message originator
		if (Arrays.equals(verificationToken.toByteArray(), message.getMessage())) {
			receivedParts.add(message.getOriginatorId());
			// all parts successfully verified
			if (receivedParts.size() == participants.size() - 1) {
				round.setActionCode(GKAProtocolRound.SUCCESS);
			}
		} 
		// verification failed - propagate error
		else {
			failFlag = true;
			round.setActionCode(GKAProtocolRound.ERROR);
		}

		return round;
	}
	
	/**
	 * Set the field containing verification token - this has to be equal for all participants
	 * to successfully verified.
	 * 
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	private void setVerificationToken() throws IOException, NoSuchAlgorithmException {
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		bs.write(participants.getNonces());
		bs.write(key.toByteArray());
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		verificationToken = new ByteArrayOutputStream();
		verificationToken.write(md.digest(bs.toByteArray()));
	}

	@Override
	public BigInteger getKey() {
		return key;
	}

	public boolean isInitialized() {
		return initialized;
	}

	/**
	 * Random nonce generation.
	 * 
	 * @return random nonce of pre-set length.
	 */
	private byte[] generateNonce() {
		byte[] nonce = new byte[gkaProtocolParams.getNonceLength()];
		secureRandom.nextBytes(nonce);
		participants.getMe().setNonce(nonce);

		return nonce;
	}
	
	/**
	 * New protocol instance initialization.
	 */
	private void initRun() {
		failFlag = false;
		
		if (receivedParts == null) this.receivedParts = new ArrayList<Integer>();
		else receivedParts.clear();
		
		secondRoundBroadcast = new ByteArrayOutputStream();
		
		// generate random secret
		byte[] secretBytes = new byte[secretLength];
		secureRandom.nextBytes(secretBytes);
		secret = new BigInteger(1, secretBytes).mod(q);
		
		// blind the secret
		blindedSecret = g.modPow(secret, p);
		
		// find its inverse
		secretInverse = secret.modInverse(q);
		
		// start computing the new key
		key = BigInteger.ONE;
	}
}
