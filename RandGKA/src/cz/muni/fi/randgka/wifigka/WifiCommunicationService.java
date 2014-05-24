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
import java.security.Provider;
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
import cz.muni.fi.randgka.randgkaapp.GKADecisionActivity;
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

public class WifiCommunicationService extends Service {
		
		public static final String LEADER_RUN = "LEADER_RUN",
									MEMBER_RUN = "MEMBER_RUN",
									GKA_RUN = "GKA_RUN";
		
		private static final String MODP_GROUP_FILE = "modpgroups.xml";

		public static final String STOP = "STOP";

		public static final String SET_SECURE_RANDOM = "SET_SECURE_RANDOM";
		
		private GKAParticipants participants;
		private Map<byte[], CommunicationThread> threads;
		private PMessageHandler pHandler;
		private GKAProtocol protocol;
		private LongTermKeyProvider longTermKeyProvider;
		private Thread listenSocketThread;
		private LocalBroadcastManager lbm;
		private Context context;
		private int newParticipantId;
		private SecureRandom secureRandom;
		private Integer version,
						nonceLength,
						groupKeyLength,
						publicKeyLength;
		private boolean returnKey;
		
		@Override
		public void onCreate() {
		}
		
		private void initialize(Intent intent) throws NoSuchAlgorithmException, NoSuchProviderException {
			returnKey = false;
			participants = new GKAParticipants();
			if (threads != null) {
				for (Entry<byte[], CommunicationThread> e : threads.entrySet()) {
					if (e.getValue() != null) e.getValue().interrupt();
				}
			}
			threads = new HashMap<byte[], CommunicationThread>();
			newParticipantId = 0;
			pHandler = new PMessageHandler();
			
			protocol = new AugotGKA();
			
			//Provider pr = new RandGKAProvider();
		    //try {
				secureRandom = new SecureRandom();// SecureRandom.getInstance(RandGKAProvider.RAND_EXTRACTOR, pr);
			/*} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}*/
				
			context = this.getBaseContext();
			lbm = LocalBroadcastManager.getInstance(this);

			if (intent != null) {
				returnKey = intent.getBooleanExtra(Constants.RETRIEVE_KEY, false);
			}
		}
		
		private void initRun(Intent intent) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
			version = intent.getIntExtra("version", GKAProtocolParams.NON_AUTH);
			nonceLength = intent.getIntExtra("nonceLength", 0)/8;
			groupKeyLength = intent.getIntExtra("groupKeyLength", 0)/8;
			publicKeyLength = intent.getIntExtra("publicKeyLength", 0)/8;
			
			participants.initialize(nonceLength, publicKeyLength);
			Log.d("pklen", publicKeyLength+ " ");
			participants.getMe().setAuthPublicKey(longTermKeyProvider.getPublicKey(publicKeyLength*8));
		}
		
		@Override
		public int onStartCommand(Intent intent, int flags, int startId) {
			try {
				String action = intent.getAction();

				if (action.equals(LEADER_RUN)) {
					initialize(intent);
					actAsServer(this);
				} 
				else if (action.equals(MEMBER_RUN)) {
					initialize(intent);
					connectToServer(this);
				} 
				else if (action.equals(SET_SECURE_RANDOM)) {
					Provider pr = new RandGKAProvider();
				    secureRandom = new SecureRandom(); //.getInstance(RandGKAProvider.RAND_EXTRACTOR, new RandGKAProvider());
				    longTermKeyProvider = new LongTermKeyProvider(context, secureRandom);
				}
				else if (action.equals(GKA_RUN)) {
					initRun(intent);
					if (listenSocketThread != null) listenSocketThread.interrupt();
					//if (!protocol.isInitialized()) {
						AssetManager am = getAssets();
						InputStream modpGroupIS = am.open(MODP_GROUP_FILE);
						
						GKAProtocolParams params = new GKAProtocolParams(version, nonceLength, groupKeyLength, publicKeyLength, modpGroupIS, longTermKeyProvider.getPrivateKey(publicKeyLength*8));
						protocol.init(participants, secureRandom, params);
					//}
					GKAProtocolRound firstRound = protocol.nextRound(new PMessage((byte)0, 0, 0, 0, false, null, null));
					sendRound(firstRound);
				}
				else if (action.equals(GKAActivity.GET_PARAMS)) {
					printParams();
				}
				else if (action.equals(STOP)) {
					onDestroy();
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
		
		private void printParams() {
			Intent paramsIntent = new Intent(GKAActivity.GET_PARAMS);
			paramsIntent.putExtra("version", version);
			paramsIntent.putExtra("nonceLength", nonceLength);
			paramsIntent.putExtra("groupKeyLength", groupKeyLength);
			paramsIntent.putExtra("publicKeyLength", publicKeyLength);
			lbm.sendBroadcast(paramsIntent);
		}

		private void sendRound(GKAProtocolRound pr) {
			if (pr != null) {
				if (pr.getActionCode() == GKAProtocolRound.SUCCESS) {
					retrieveKey(returnKey);
				} 
				else if (pr.getActionCode() == GKAProtocolRound.PRINT_PARTICIPANTS) {
					Intent getDevices = new Intent(GKAActivity.GET_PARTICIPANTS);
					getDevices.putExtra("participants", participants);
					lbm.sendBroadcast(getDevices);
				}
				if (pr.getMessages() != null && threads != null) {
					for (Entry<GKAParticipant, PMessage> e : pr.getMessages().entrySet()) {
						byte[] address = participants.getParticipant(e.getKey().getId()).getAddress();
						threads.get(address).write(e.getValue().getBytes());
					}
				}
			}
		}
		
		private void retrieveKey(boolean returnKey) {
			Intent printKeyIntent = new Intent(returnKey ? GKAActivity.SHOW_RETRIEVE_KEY : GKAActivity.GET_GKA_KEY);
		    printKeyIntent.putExtra(Constants.KEY, protocol.getKey().toByteArray());
		    lbm.sendBroadcast(printKeyIntent);
		}
		
		@Override
		public void onDestroy() {
			super.onDestroy();
			if (pHandler != null) {
				pHandler.removeCallbacksAndMessages(null);
				pHandler = null;
			}
			if (threads != null) {
				for (Entry<byte[], CommunicationThread> e : threads.entrySet()) {
					if (e.getValue() != null) {
						e.getValue().interrupt();
					}
				}
				threads.clear();
			}
			if (participants != null) participants = null;
			if (listenSocketThread != null) {
				listenSocketThread.interrupt();
			}
			Log.d("destroyed", "ok");
		}
		
		private class PMessageHandler extends Handler {
			@Override
			public void handleMessage(Message message) {
				Bundle bundle = message.getData();
				if (bundle != null) {
					PMessage pMessage = (PMessage)bundle.getSerializable("pMessage");
					try {
						if (pMessage != null) {
							
							if (pMessage.getRoundNo()==(byte)1) {
								byte[] receivedMessageBytes = pMessage.getMessage();
								
								byte[] intBytes = new byte[4];
								
								System.arraycopy(receivedMessageBytes, 0, intBytes, 0, 4);
								int id = ByteBuffer.wrap(intBytes).getInt();
								
								System.arraycopy(receivedMessageBytes, 4, intBytes, 0, 4);
								version = ByteBuffer.wrap(intBytes).getInt();
								
								System.arraycopy(receivedMessageBytes, 8, intBytes, 0, 4);
								nonceLength = ByteBuffer.wrap(intBytes).getInt();
								
								System.arraycopy(receivedMessageBytes, 12, intBytes, 0, 4);
								groupKeyLength = ByteBuffer.wrap(intBytes).getInt();
								
								System.arraycopy(receivedMessageBytes, 16, intBytes, 0, 4);
								publicKeyLength = ByteBuffer.wrap(intBytes).getInt();
								
								printParams();
								
								GKAParticipant me = participants.getMe();
								me.setId(id);
								
								me.setAuthPublicKey(longTermKeyProvider.getPublicKey(publicKeyLength*8));
								
								for (GKAParticipant p : participants.getParticipants()) {
									p.setNonceLen(nonceLength);
									p.setPkLen(publicKeyLength);
								}
								AssetManager am = getAssets();
								InputStream modpGroupIS = am.open("modpgroups.xml");
							
								GKAProtocolParams params = new GKAProtocolParams(version, nonceLength, groupKeyLength, publicKeyLength, modpGroupIS, longTermKeyProvider.getPrivateKey(publicKeyLength*8));
								protocol.init(participants, secureRandom, params);
							}
							
							sendRound(protocol.nextRound(pMessage));
						}
					} catch (NoSuchAlgorithmException e1) {
						e1.printStackTrace();
					} catch (NoSuchProviderException e1) {
						e1.printStackTrace();
					} catch (InvalidKeySpecException e1) {
						e1.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		public void actAsServer(Context context) throws IOException {
			InetAddress addr = getInetAddress();
			if (addr == null) throw new IllegalArgumentException();
			GKAParticipant me = new GKAParticipant(0, addr.getHostAddress(), addr.getAddress(), true, GKAParticipantRole.LEADER, 0, 0, null, null);
			participants.add(me);
			threads.put(me.getAddress(), null);
			
			if (listenSocketThread == null) {
				ServerSocket ss = new ServerSocket();
				ss.setReuseAddress(true);
				ss.bind(new InetSocketAddress(addr, Constants.WIFI_PORT));
					
				listenSocketThread = new Thread(new ListenSocketThread(ss));
				listenSocketThread.start();
			}
		}

		
		private void registerThread(byte[] address, CommunicationThread thread) {
			boolean found = false;
			
			for (Entry<byte[], CommunicationThread> e : threads.entrySet()) {
				if (Arrays.equals(address, e.getKey())) {
					e.setValue(thread);
					found = true;
					break;
				}
			}
			
			if (!found) threads.put(address, thread);
		}
		
		private class ListenSocketThread extends Thread{
			
			private ServerSocket ss;
			
			public ListenSocketThread(ServerSocket ss) {
				this.ss = ss;
			}
			
			@Override
			public void run() {
				
				while (true) {
		            try {
		            	Log.d("waiting for", "connection");
		                Socket s = ss.accept();
		                Log.d("connected","ok");
			            if (s != null && participants != null) {
			            	// invoke communication thread
			    	        CommunicationThread ct = new CommunicationThread(s);
			    	        ct.start();
			    	        
			    	        GKAParticipant participant = new GKAParticipant(++newParticipantId, s.getInetAddress().getCanonicalHostName(), s.getInetAddress().getAddress(), false, GKAParticipantRole.MEMBER, 0, 0, null, null);
			    	        if (!participants.contains(participant.getAddress())) {
								participants.add(participant);

								Intent printDeviceIntent = new Intent(GKAActivity.PRINT_DEVICE);
				    			printDeviceIntent.putExtra("device", s.getInetAddress().getHostName());
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
			
			public void interrupt() {
				try {
					if (ss != null) {
						ss.close();
						ss = null;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				super.interrupt();
			}
			
		}
		
		public void connectToServer(Context context) {
			try {
				Log.d("connect to", "server");
				InetAddress myAddr = getInetAddress();
				if (myAddr == null) throw new SocketException();
				
				Thread connectThread = new Thread(new Runnable() {
					
					@Override
					public void run() {
						InetAddress myAddr = getInetAddress();
						byte[] iaBytes = new byte[]{myAddr.getAddress()[0], myAddr.getAddress()[1], myAddr.getAddress()[2], (byte)1};
						InetAddress ia;
						try {
							ia = InetAddress.getByAddress(iaBytes);
							
							Socket s = new Socket(ia, Constants.WIFI_PORT);
							Log.d("connected", "ok");
					        // invoke communication thread
					        CommunicationThread ct = new CommunicationThread(s);
					        ct.start();
					        GKAParticipant participant = new GKAParticipant(0, ia.getHostName(), ia.getAddress(), false, GKAParticipantRole.LEADER, 0, 0, null, null);
							participants.add(participant);
							threads.put(participant.getAddress(), ct);
							
							//Log.d("participant",participant.toString());
						} catch (UnknownHostException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				});
				connectThread.start();
		        
				
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
		
		private class CommunicationThread extends Thread {
			
			private Socket s;
		    private InputStream inStream;
		    private OutputStream outStream;
		    private boolean isInterrupted = false;
		 
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
		 
		    public void run() {
		    	//byte[] buffer = new byte[2048];  // buffer store for the stream
		        int bytes = 0; // bytes returned from read()
		        byte[] buffer = new byte[2048];
		       	ByteArrayOutputStream concateBytes = null;
		       	boolean dividedMessage = false;
		        // Keep listening to the InputStream until an exception occurs
		        while (true) {
		            try {
		                // Read from the InputStream
		            	
		                bytes = inStream.read(buffer);
		                
		                Log.d("read", Arrays.toString(buffer));
		                
		                if (isInterrupted) break;
		                PMessage pMessage = null;
		                if (dividedMessage) {
		                	concateBytes.write(buffer, 0, bytes);
		                	pMessage = new PMessage(concateBytes.toByteArray());
		                	dividedMessage = false;
		                }
		                else pMessage = new PMessage(buffer);
		                
		                Message m = pHandler.obtainMessage();
		                
		                Bundle pMessageBundle = new Bundle();
		                pMessageBundle.putSerializable("pMessage", pMessage);
		                
		        		m.setData(pMessageBundle);
		                m.sendToTarget();
		                
		                Arrays.fill(buffer, (byte) 0x00);
		            } catch (IOException e) {
		                break;
		            } catch (LengthsNotEqualException e) {
		            	dividedMessage = true;
	            		concateBytes = new ByteArrayOutputStream();
						concateBytes.write(buffer, 0, bytes);
						Arrays.fill(buffer, (byte) 0x00);
					} catch (ArrayIndexOutOfBoundsException e) {
						onDestroy();
						Intent getBack = new Intent(getBaseContext(), GKADecisionActivity.class);
						getBack.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						getBack.setAction(returnKey? Constants.ACTION_GKA_KEY : "empty");
						getBack.putExtra(Constants.RETRIEVE_KEY, returnKey);
						getBack.putExtra("getBack", true);
						getApplication().startActivity(getBack);
						Log.d("get", "back");
						break;
					}
		        }
		    }
		    
		    /* Call this from the main activity to send data to the remote device */
		    public void write(byte[] bytes) {
		        try {
		        	//Log.d("write", Arrays.toString(bytes));
		            outStream.write(bytes);
		            // Log.d("writeLength", bytes.length+" ");
		            //PMessage forSure = new PMessage(bytes);
		            //Log.d("forSure", forSure.getLength()+" ");
		        } catch (IOException e) {
		        	e.printStackTrace();
		        }
		    }
		 
		    /* Call this from the main activity to shutdown the connection */
		    public void interrupt() {
		    	Log.d("one", "1");
		        try {
		        	inStream.close();
		        	Log.d("one", "2");
		        	outStream.close();
		        	Log.d("one", "3");
		            s.close();
		            Log.d("one", "4");
		        } catch (IOException e) {
		        	e.printStackTrace();
		        }
		        isInterrupted = true;
		        super.interrupt();
		    }
		}
		
		public static boolean isWifiConnected(boolean isLeader) {
			InetAddress inetAddress = getInetAddress();
			if (inetAddress != null) {
               	if (isLeader && inetAddress.getAddress()[3] == (byte)1) return true;
               	else if (!isLeader && inetAddress.getAddress()[3] != (byte)1) return true;
               	else return false;
			}
	        return false;
		}
		
		public static InetAddress getInetAddress() {
			try {
	            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	                NetworkInterface intf = en.nextElement();
	                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                    InetAddress inetAddress = enumIpAddr.nextElement();
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
