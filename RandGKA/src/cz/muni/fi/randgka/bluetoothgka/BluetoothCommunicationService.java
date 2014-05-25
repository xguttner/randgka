package cz.muni.fi.randgka.bluetoothgka;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import cz.muni.fi.randgka.gka.AugotGKA;
import cz.muni.fi.randgka.gka.GKAParticipant;
import cz.muni.fi.randgka.gka.GKAParticipantRole;
import cz.muni.fi.randgka.gka.GKAParticipants;
import cz.muni.fi.randgka.gka.GKAProtocol;
import cz.muni.fi.randgka.gka.GKAProtocolParams;
import cz.muni.fi.randgka.gka.GKAProtocolRound;
import cz.muni.fi.randgka.provider.RandGKAProvider;
import cz.muni.fi.randgka.randgkaapp.GKAActivity;
import cz.muni.fi.randgka.randgkaapp.GKAMemberActivity;
import cz.muni.fi.randgka.tools.Constants;
import cz.muni.fi.randgka.tools.LengthsNotEqualException;
import cz.muni.fi.randgka.tools.PMessage;
import cz.muni.fi.randgka.tools.LongTermKeyProvider;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Class utilizing the communication over the Bluetooth channel.
 */
public class BluetoothCommunicationService extends Service {
	
	// parameters for Service Discovery Protocol regarding this app
	private static final String SERVICE_NAME = "GKA_SERVICE";
	private static final UUID GKA_UUID = UUID.fromString("b89295fe-8eb2-4db1-9c19-a0f854d29db4");
	
	// xml file stored using Assets containing modp group parameters
	private static final String MODP_GROUP_FILE = "modpgroups.xml";
	
	// group key agreement protocol
	private GKAProtocol protocol;
	// protocol participants
	private GKAParticipants participants;
	// threads regarding participants identified by participants' addresses
	private Map<byte[], CommunicationThread> threads;
	private PMessageHandler pHandler;
	private LongTermKeyProvider longTermKeyProvider;
	// leader's connection listen thread
	private Thread listenSocketThread;
	private LocalBroadcastManager lbm;
	private Context context;
	private BluetoothServerSocket bss;
	// incrementing id for newly connected participants
	private int newParticipantId;
	// secure randomness provider
	private SecureRandom secureRandom;
	// protocol version: 0 - non-authorized, 1 - authorized, 2 - authorized + key confirmation
	private Integer version;
	private Integer nonceLength, // length of the random nonce in bytes
					groupKeyLength, // length of the resulting group key in bytes
					publicKeyLength; // length of the public key pair in bytes
	
	// determine, if another application wants to gain the resulting key (true, otherwise false)
	private boolean retrieveKey,
		protocolRunning;
	
	private String entropySourceString;
	
	@Override
	public void onCreate() {}
	
	/**
	 * default parameters pre-setting
	 * 
	 * @param intent received from user api (activities)
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws IOException 
	 */
	private void initialize(Intent intent) throws NoSuchAlgorithmException, NoSuchProviderException, IOException {
		// release communication threads
		if (threads != null) {
			for (Entry<byte[], CommunicationThread> e : threads.entrySet()) {
				if (e.getValue() != null) e.getValue().bs.close();
			}
			threads.clear();
		}
		else threads = new HashMap<byte[], CommunicationThread>();
		
		protocol = new AugotGKA();
		participants = new GKAParticipants();
		newParticipantId = 0;
		protocolRunning = false;
		
		context = this.getBaseContext();
		lbm = LocalBroadcastManager.getInstance(this);
		pHandler = new PMessageHandler();
		
		if (intent != null) {
			retrieveKey = intent.getBooleanExtra(Constants.RETRIEVE_KEY, false);
			entropySourceString = intent.getStringExtra(Constants.ENTROPY_SOURCE);
		}
		else {
			retrieveKey = false;
			entropySourceString = Constants.RAND_EXT_ES;
		}
	}
	
	/**
	 * New protocol instance initialization
	 * 
	 * @param intent received from user api (activities)
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws InvalidKeySpecException
	 */
	private void initRun(Intent intent) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
		// set protocol parameters
		version = intent.getIntExtra(Constants.VERSION, GKAProtocolParams.NON_AUTH);
		nonceLength = intent.getIntExtra(Constants.NONCE_LENGTH, 0)/8;
		groupKeyLength = intent.getIntExtra(Constants.GROUP_KEY_LENGTH, 0)/8;
		publicKeyLength = intent.getIntExtra(Constants.PUBLIC_KEY_LENGTH, 0)/8;
		
		participants.initialize(nonceLength, publicKeyLength);
		participants.getMe().setPublicKey(longTermKeyProvider.getPublicKey(publicKeyLength*8));
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		try {
			String action = intent.getAction();
			
			// leader initialization + server socket opening
			if (action.equals(Constants.LEADER_RUN)) {
				initialize(intent);
				actAsServer(this, BluetoothAdapter.getDefaultAdapter());
			} 
			// member initialization + connection to opened server socket
			else if (action.equals(Constants.MEMBER_RUN)) {
				initialize(intent);
				connectToServer((BluetoothDevice)intent.getParcelableExtra(Constants.DEVICE), this);
			} 
			// after camera preview has been set, we can utilize it in randomness retrieval
			else if (action.equals(Constants.SET_SECURE_RANDOM)) {
			    if (pHandler == null) {
			    	Intent finishGKAActivity = new Intent(GKAActivity.NOT_ACTIVE);
			    	if (lbm == null) lbm = LocalBroadcastManager.getInstance(this);
			    	lbm.sendBroadcast(finishGKAActivity);
			    } else {
			    	if (entropySourceString.equals(Constants.RAND_EXT_ES)) secureRandom = SecureRandom.getInstance(RandGKAProvider.RAND_EXTRACTOR, new RandGKAProvider());
				    else secureRandom = new SecureRandom();
				    longTermKeyProvider = new LongTermKeyProvider(context, secureRandom);
			    }
			}
			// protocol run invocation (by leader)
			else if (action.equals(Constants.GKA_RUN)) {
				if (participants != null && !protocolRunning) {
					protocolRunning = true;
					initRun(intent);
					
					// get the source of modp group parameters
					AssetManager am = getAssets();
					InputStream modpGroupIS = am.open(MODP_GROUP_FILE);
					
					// protocol initialization
					GKAProtocolParams params = new GKAProtocolParams(version, nonceLength, groupKeyLength, publicKeyLength, modpGroupIS, longTermKeyProvider.getPrivateKey(publicKeyLength*8));
					protocol.init(participants, secureRandom, params);
					
					// start the protocol by first round calling
					GKAProtocolRound firstRound = protocol.nextRound(new PMessage((byte)0, 0, 0, 0, false, null, null));
					sendRound(firstRound);
				}
			}
			// print current protocol instance parameters
			else if (action.equals(GKAActivity.GET_PARAMS)) {
				printParams();
			}
			// stop this service
			else if (action.equals(Constants.STOP)) {
				protocolRunning = false;
				interruptThreads();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		}
		
		return START_NOT_STICKY;
	}
	
	/**
	 * Send parameters to GKAActivity class to print them.
	 */
	private void printParams() {
		Intent paramsIntent = new Intent(GKAActivity.GET_PARAMS);
		paramsIntent.putExtra(Constants.VERSION, version);
		paramsIntent.putExtra(Constants.NONCE_LENGTH, nonceLength);
		paramsIntent.putExtra(Constants.GROUP_KEY_LENGTH, groupKeyLength);
		paramsIntent.putExtra(Constants.PUBLIC_KEY_LENGTH, publicKeyLength);
		lbm.sendBroadcast(paramsIntent);
	}

	/**
	 * Function processing next protocol round.
	 * 
	 * @param pr - round to process
	 */
	private void sendRound(GKAProtocolRound pr) {
		if (pr != null) {
			// key is established - we use it in appropriate manner
			if (pr.getActionCode() == GKAProtocolRound.SUCCESS){
				protocolRunning = false;
				getKey(retrieveKey);
			}
			// participants are determined - print them
			else if (pr.getActionCode() == GKAProtocolRound.PRINT_PARTICIPANTS) {
				Intent getDevices = new Intent(GKAActivity.GET_PARTICIPANTS);
				getDevices.putExtra(Constants.PARTICIPANTS, participants);
				lbm.sendBroadcast(getDevices);
			}
			// an error occurred
			else if (pr.getActionCode() == GKAProtocolRound.ERROR) protocolRunning = false;
			// each message send to the given target
			if (pr.getMessages() != null && threads != null) {
				for (Entry<GKAParticipant, PMessage> e : pr.getMessages().entrySet()) {
					byte[] address = participants.getParticipant(e.getKey().getId()).getAddress();
					threads.get(address).write(e.getValue().getBytes());
				}
			}
		}
	}
	
	/**
	 * 
	 * @param retrieveKey - true if send back to the calling app, false if print
	 */
	private void getKey(boolean retrieveKey) {
		Intent printKeyIntent = new Intent(retrieveKey ? GKAActivity.RETRIEVE_GKA_KEY : GKAActivity.PRINT_GKA_KEY);
	    printKeyIntent.putExtra(Constants.KEY, protocol.getKey().toByteArray());
	    lbm.sendBroadcast(printKeyIntent);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		try {
			interruptThreads();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void interruptThreads() throws IOException {
		if (pHandler != null) {
			pHandler.removeCallbacksAndMessages(null);
			pHandler = null;
		}
		if (threads != null) {
			for (Entry<byte[], CommunicationThread> e : threads.entrySet()) {
				if (e.getValue() != null) {
					e.getValue().bs.close();
				}
			}
			threads.clear();
		}
		if (bss != null) {
			bss.close();
		}
	}
	
	/**
	 * Handler for transmition of the Bluetooth channel messages to the main thread.
	 */
	private class PMessageHandler extends Handler {
		
		@Override
		public void handleMessage(Message message) {
			Bundle bundle = message.getData();
			if (bundle != null) {
				PMessage pMessage = (PMessage)bundle.getSerializable(Constants.PMESSAGE);
				try {
					if (pMessage != null) {
						// protocol initialization for member
						if (pMessage.getRoundNo()==(byte)1) {
							byte[] receivedMessageBytes = pMessage.getMessage();
							
							byte[] intBytes = new byte[4];
							
							// participant id determined by leader
							System.arraycopy(receivedMessageBytes, 0, intBytes, 0, 4);
							int id = ByteBuffer.wrap(intBytes).getInt();
							
							// protocol version
							System.arraycopy(receivedMessageBytes, 4, intBytes, 0, 4);
							version = ByteBuffer.wrap(intBytes).getInt();
							
							// nonce length
							System.arraycopy(receivedMessageBytes, 8, intBytes, 0, 4);
							nonceLength = ByteBuffer.wrap(intBytes).getInt();
							
							// group key length
							System.arraycopy(receivedMessageBytes, 12, intBytes, 0, 4);
							groupKeyLength = ByteBuffer.wrap(intBytes).getInt();
							
							// public key length
							System.arraycopy(receivedMessageBytes, 16, intBytes, 0, 4);
							publicKeyLength = ByteBuffer.wrap(intBytes).getInt();
							
							// we know the params - print them!
							printParams();
							
							// edit the member participant object to the shared params
							GKAParticipant me = participants.getMe();
							me.setId(id);
							me.setPublicKey(longTermKeyProvider.getPublicKey(publicKeyLength*8));
							
							// set the given params to all participants
							for (GKAParticipant p : participants.getParticipants()) {
								p.setNonceLen(nonceLength);
								p.setPkLen(publicKeyLength);
							}
							
							// get the source of modp group parameters
							AssetManager am = getAssets();
							InputStream modpGroupIS = am.open(MODP_GROUP_FILE);
						
							// protocol instance initialization
							GKAProtocolParams params = new GKAProtocolParams(version, nonceLength, groupKeyLength, publicKeyLength, modpGroupIS, longTermKeyProvider.getPrivateKey(publicKeyLength*8));
							protocol.init(participants, secureRandom, params);
						}
						
						// process current round and determine the next
						sendRound(protocol.nextRound(pMessage));
					}
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				} catch (NoSuchProviderException e) {
					e.printStackTrace();
				} catch (InvalidKeySpecException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Register leader's participant object + open server socket to accept members.
	 * 
	 * @param context - application context
	 * @param bluetoothAdapter - leader device bluetooth adapter
	 * @throws IOException
	 */
	public void actAsServer(Context context, BluetoothAdapter bluetoothAdapter) throws IOException {
		// set leader's participant object
		GKAParticipant me = new GKAParticipant(0, bluetoothAdapter.getName(), GKAParticipant.macStringToBytes(bluetoothAdapter.getAddress()), true, GKAParticipantRole.LEADER, 0, 0, null, null);
		participants.add(me);
		threads.put(me.getAddress(), null);
		
		// register appropriate bluetooth server socket
		bss = bluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, GKA_UUID);
		
		// open thread for members' devices socket acceptance
		if (listenSocketThread == null || !listenSocketThread.isAlive()) {
			listenSocketThread = new Thread(new ListenSocketThread(bss));
			listenSocketThread.start();
		}
	}
	
	/**
	 * Register new communication thread.
	 * 
	 * @param address - device address
	 * @param thread - communication thread containing socket
	 */
	private void registerThread(byte[] address, CommunicationThread thread) {
		boolean found = false;
		
		// if device is registered, change its thread to the new one
		for (Entry<byte[], CommunicationThread> e : threads.entrySet()) {
			if (Arrays.equals(address, e.getKey())) {
				e.setValue(thread);
				found = true;
				break;
			}
		}
		
		// if device hasn't been registered, include new tuple
		if (!found) threads.put(address, thread);
	}
	
	/**
	 * Thread for opening new sockets
	 */
	private class ListenSocketThread extends Thread {
		
		private BluetoothServerSocket bss;
		
		public ListenSocketThread(BluetoothServerSocket bss) {
			this.bss = bss;
		}
		
		@Override
		public void run() {
			while (true) {
	            try {
	            	// accept new socket connection request
	                BluetoothSocket bs = bss.accept();
		            if (bs != null && participants != null) {
		            	// invoke a new communication thread
		    	        CommunicationThread ct = new CommunicationThread(bs);
		    	        ct.start();
		    	        
		    	        // register the new participant
		    	        GKAParticipant participant = new GKAParticipant(++newParticipantId, bs.getRemoteDevice().getName(), GKAParticipant.macStringToBytes(bs.getRemoteDevice().getAddress()), false, GKAParticipantRole.MEMBER, 0, 0, null, null);
		    	        if (!participants.contains(participant.getAddress())) {
							participants.add(participant);

							// if the participant is truly new to the protocol, print it
			    			Intent printDeviceIntent = new Intent(GKAActivity.PRINT_DEVICE);
			    			printDeviceIntent.putExtra(Constants.DEVICE, bs.getRemoteDevice().getName()+" ("+bs.getRemoteDevice().getAddress()+")");
			    			lbm.sendBroadcast(printDeviceIntent);
						}
		    	        registerThread(participant.getAddress(), ct);
		    	    }
	            } catch (IOException e) {
	            	e.printStackTrace();
	                break;
	            }
	        }
		}
	}
	
	/**
	 * Function to provide member's connection to the leader
	 * 
	 * @param bluetoothDevice - members bluetooth device
	 * @param context - application context
	 */
	public void connectToServer(BluetoothDevice bluetoothDevice, Context context) {
		try {
            // GKA_UUID is the app's UUID string, also used by the server code
            BluetoothSocket bs = bluetoothDevice.createRfcommSocketToServiceRecord(GKA_UUID);
            bs.connect();
            
	        // invoke a new communication thread
	        CommunicationThread ct = new CommunicationThread(bs);
	        ct.start();
	        
	        // leader's participant object
	        GKAParticipant participant = new GKAParticipant(0, bluetoothDevice.getName(), GKAParticipant.macStringToBytes(bluetoothDevice.getAddress()), false, GKAParticipantRole.LEADER, 0, 0, null, null);
			participants.add(participant);
			threads.put(participant.getAddress(), ct);
	        
			// member's participant object
			GKAParticipant me = new GKAParticipant(1, BluetoothAdapter.getDefaultAdapter().getName(), GKAParticipant.macStringToBytes(BluetoothAdapter.getDefaultAdapter().getAddress()), true, GKAParticipantRole.MEMBER, 0, 0, null, null);
			participants.add(me);
			threads.put(me.getAddress(), null);
			
			// after the successful connection, raise GKAActivity object to show the results to the member
			Intent moving = new Intent(GKAMemberActivity.CONNECTED);
			lbm.sendBroadcast(moving);
        } catch (IOException e) {
        	e.printStackTrace();
        }
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	/**
	 * Communication thread
	 */
	private class CommunicationThread extends Thread {
		
		private BluetoothSocket bs;
	    private InputStream inStream;
	    private OutputStream outStream;
	 
	    public CommunicationThread(BluetoothSocket bs) {
	    	this.bs = bs;
	    	this.setStreams();
	    }
	    
	    public void setStreams() {
	    	if (bs != null) {
		        try {
		            inStream = bs.getInputStream();
		            outStream = bs.getOutputStream();
		        } catch (IOException e) {
		        	e.printStackTrace();
		        }
	        }
	    }
	 
	    /**
	     * Receiving data
	     */
	    public void run() {
	    	// bytes returned from read()
	        int bytes = 0;
	        // buffer for read() stream
	        byte[] buffer = new byte[2048];
	       	ByteArrayOutputStream concateBytes = null;
	       	boolean dividedMessage = false;
	        // Keep listening to the InputStream until an exception occurs
	        while (true) {
	            try {
	            	// read from input stream
	                bytes = inStream.read(buffer);
	                // received message
	                PMessage pMessage = null;
	                // message was divided
	                if (dividedMessage) {
	                	concateBytes.write(buffer, 0, bytes);
	                	pMessage = new PMessage(concateBytes.toByteArray());
	                	dividedMessage = false;
	                }
	                else pMessage = new PMessage(buffer);
	                
	                // send the received message to the main thread
	                Message m = pHandler.obtainMessage();
	                Bundle pMessageBundle = new Bundle();
	                pMessageBundle.putSerializable("pMessage", pMessage);
	        		m.setData(pMessageBundle);
	                m.sendToTarget();

	                // restore default values
	                Arrays.fill(buffer, (byte) 0x00);
	            } catch (IOException e) {
	                break;
	            } 
	            // array has been separated - wait for the next buffer
	            catch (LengthsNotEqualException e) {
	            	dividedMessage = true;
            		concateBytes = new ByteArrayOutputStream();
					concateBytes.write(buffer, 0, bytes);
					Arrays.fill(buffer, (byte) 0x00);
				} 
	            // closing the thread - empty message received
	            catch (ArrayIndexOutOfBoundsException e) {
					interrupt();
					break;
				}
	        }
	    }
	    
	    /**
	     * Sending data
	     * 
	     * @param bytes - data to send
	     */
	    public void write(byte[] bytes) {
	        try {
	            outStream.write(bytes);
	        } catch (IOException e) {
	        	e.printStackTrace();
	        }
	    }
	 
	    /**
	     * Thread interruption
	     */
	    public void interrupt() {
	        try {
	        	if (inStream != null) inStream.close();
	        	if (outStream != null) outStream.close();
	            if (bs != null) bs.close();
	        } catch (IOException e) {
	        	e.printStackTrace();
	        }
	        super.interrupt();
	    }
	}
}
