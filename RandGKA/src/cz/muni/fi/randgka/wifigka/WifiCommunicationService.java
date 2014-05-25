package cz.muni.fi.randgka.wifigka;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import cz.muni.fi.randgka.gka.AugotGKA;
import cz.muni.fi.randgka.gka.GKAParticipant;
import cz.muni.fi.randgka.gka.GKAParticipantRole;
import cz.muni.fi.randgka.gka.GKAParticipants;
import cz.muni.fi.randgka.gka.GKAProtocol;
import cz.muni.fi.randgka.gka.GKAProtocolParams;
import cz.muni.fi.randgka.gka.GKAProtocolRound;
import cz.muni.fi.randgka.provider.RandGKAProvider;
import cz.muni.fi.randgka.randgkaapp.GKAActivity;
import cz.muni.fi.randgka.tools.Constants;
import cz.muni.fi.randgka.tools.LengthsNotEqualException;
import cz.muni.fi.randgka.tools.LongTermKeyProvider;
import cz.muni.fi.randgka.tools.PMessage;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * Class utilizing the communication over the Bluetooth channel.
 */
public class WifiCommunicationService extends Service {
		
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
		private ServerSocket ss;
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
		private boolean retrieveKey;
		
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
					if (e.getValue() != null) e.getValue().s.close();
				}
			}
			threads = new HashMap<byte[], CommunicationThread>();
			
			protocol = new AugotGKA();
			participants = new GKAParticipants();
			newParticipantId = 0;
			
			context = this.getBaseContext();
			lbm = LocalBroadcastManager.getInstance(this);
			pHandler = new PMessageHandler();
			
			// was invoked by another app?
			if (intent != null) retrieveKey = intent.getBooleanExtra(Constants.RETRIEVE_KEY, false);
			else retrieveKey = false;
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
					actAsServer(this);
				} 
				// member initialization + connection to opened server socket
				else if (action.equals(Constants.MEMBER_RUN)) {
					initialize(intent);
					connectToServer(this);
				} 
				// after camera preview has been set, we can utilize it in randomness retrieval
				else if (action.equals(Constants.SET_SECURE_RANDOM)) {
				    secureRandom = new SecureRandom(); //SecureRandom.getInstance(RandGKAProvider.RAND_EXTRACTOR, new RandGKAProvider());
				    longTermKeyProvider = new LongTermKeyProvider(context, secureRandom);
				}
				// protocol run invocation (by leader)
				else if (action.equals(Constants.GKA_RUN)) {
					if (participants != null) {
						initRun(intent);
						if (listenSocketThread != null) listenSocketThread.interrupt();
						
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
				if (pr.getActionCode() == GKAProtocolRound.SUCCESS) getKey(retrieveKey);
				// participants are determined - print them
				else if (pr.getActionCode() == GKAProtocolRound.PRINT_PARTICIPANTS) {
					Intent getDevices = new Intent(GKAActivity.GET_PARTICIPANTS);
					getDevices.putExtra(Constants.PARTICIPANTS, participants);
					lbm.sendBroadcast(getDevices);
				}
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
		 * @param retrieveKey - true if send back to the calling app, false if print
		 */
		private void getKey(boolean retrieveKey) {
			Intent printKeyIntent = new Intent(retrieveKey ? GKAActivity.SHOW_RETRIEVE_KEY : GKAActivity.GET_GKA_KEY);
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
						e.getValue().s.close();
					}
				}
				threads.clear();
			}
			if (ss != null) ss.close();
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
		 * @throws IOException
		 */
		public void actAsServer(Context context) throws IOException {
			// get device address
			InetAddress addr = getInetAddress();
			if (addr == null) throw new IllegalArgumentException();
			
			// set application's device participant
			GKAParticipant me = new GKAParticipant(0, addr.getHostAddress(), addr.getAddress(), true, GKAParticipantRole.LEADER, 0, 0, null, null);
			participants.add(me);
			threads.put(me.getAddress(), null);
			
			// open thread for members' devices socket acceptance
			if (listenSocketThread == null || !listenSocketThread.isAlive()) {
				ss = new ServerSocket();
				ss.setReuseAddress(true);
				ss.bind(new InetSocketAddress(addr, Constants.WIFI_PORT));
					
				listenSocketThread = new Thread(new ListenSocketThread(ss));
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
		private class ListenSocketThread extends Thread{
			
			private ServerSocket ss;
			
			public ListenSocketThread(ServerSocket ss) {
				this.ss = ss;
			}
			
			@Override
			public void run() {
				
				while (true) {
		            try {
		            	// accept new socket connection request
		                Socket s = ss.accept();
		                
			            if (s != null && participants != null) {
			            	// invoke a new communication thread
			    	        CommunicationThread ct = new CommunicationThread(s);
			    	        ct.start();
			    	        
			    	        // register the new participant
			    	        GKAParticipant participant = new GKAParticipant(++newParticipantId, s.getInetAddress().getCanonicalHostName(), s.getInetAddress().getAddress(), false, GKAParticipantRole.MEMBER, 0, 0, null, null);
			    	        if (!participants.contains(participant.getAddress())) {
								participants.add(participant);

								// if the participant is truly new to the protocol, print it
								Intent printDeviceIntent = new Intent(GKAActivity.PRINT_DEVICE);
				    			printDeviceIntent.putExtra(Constants.DEVICE, s.getInetAddress().getHostName());
				    			lbm.sendBroadcast(printDeviceIntent);
							}
			    	        registerThread(participant.getAddress(), ct);
			    	
			    	    }
		            } catch (IOException e) {
		            	e.printStackTrace();
		                break;
		            }
		        }
				this.interrupt();
			}
		}
		
		/**
		 * Function to provide member's connection to the leader
		 * 
		 * @param context - application context
		 */
		public void connectToServer(Context context) {
			try {
				// get device address
				InetAddress myAddr = getInetAddress();
				if (myAddr == null) throw new SocketException();
				
				// new thread for socket establishment
				Thread connectThread = new Thread(new Runnable() {
					
					@Override
					public void run() {
						// get device address
						InetAddress myAddr = getInetAddress();
						
						try {
							// get the server device address by setting the last byte to be (byte)1
							byte[] iaBytes = new byte[]{myAddr.getAddress()[0], myAddr.getAddress()[1], myAddr.getAddress()[2], (byte)1};
							InetAddress ia = InetAddress.getByAddress(iaBytes);
							
							Socket s = new Socket(ia, Constants.WIFI_PORT);
							
					        // invoke communication thread
					        CommunicationThread ct = new CommunicationThread(s);
					        ct.start();
					        
					        // leader's participant object
					        GKAParticipant participant = new GKAParticipant(0, ia.getHostName(), ia.getAddress(), false, GKAParticipantRole.LEADER, 0, 0, null, null);
							participants.add(participant);
							threads.put(participant.getAddress(), ct);
							
						} catch (UnknownHostException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
				connectThread.start();
		        
				// member's participant object
				GKAParticipant me = new GKAParticipant(1, myAddr.getHostAddress()+" (this device)", myAddr.getAddress(), true, GKAParticipantRole.MEMBER, 0, 0, null, null);
				participants.add(me);
				threads.put(me.getAddress(), null);
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
			
			private Socket s;
		    private InputStream inStream;
		    private OutputStream outStream;
		 
		    public CommunicationThread(Socket s) {
		    	this.s = s;
		    	this.setStreams();
		    }
		    
		    public void setStreams() {
		    	if (s != null) {
			        try {
			            inStream = s.getInputStream();
			            outStream = s.getOutputStream();
			        } catch (IOException e) {
			        	e.printStackTrace();
			        }
		        }
		    }
		 
		    /**
		     * Receiving data
		     */
		    public void run() {
		        int bytes = 0; // bytes returned from read()
		        byte[] buffer = new byte[2048]; // buffer for the read() stream
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
		                
		                if (pHandler == null) break;
		                
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
		            if (s != null) s.close();
		        } catch (IOException e) {
		        	e.printStackTrace();
		        }
		        super.interrupt();
		    }
		}
		
		/**
		 * @param isLeader - true if device participant is leader (is a hotspot), false otherwise
		 * @return true if is connected via wifi in a given manner, false otherwise
		 */
		public static boolean isWifiConnected(boolean isLeader) {
			InetAddress inetAddress = getInetAddress();
			if (inetAddress != null) {
				// if is leader, last byte is (byte)1
               	if (isLeader && inetAddress.getAddress()[3] == (byte)1) return true;
               	// if is member, last byte is not (byte)1
               	else if (!isLeader && inetAddress.getAddress()[3] != (byte)1) return true;
               	else return false;
			}
	        return false;
		}
		
		/**
		 * @return InetAddress for current device
		 */
		public static InetAddress getInetAddress() {
			try {
	            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	                NetworkInterface intf = en.nextElement();
	                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                    InetAddress inetAddress = enumIpAddr.nextElement();
	                    // determine the interface according to wanted properties
	                    if (!inetAddress.isLoopbackAddress() && inetAddress.getAddress().length == 4) {
	                    	return inetAddress;
	                    }
	                }
	            }
	        } catch (SocketException ex) {
	        }
	        return null;
		}
}
