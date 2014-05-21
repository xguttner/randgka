package cz.muni.fi.randgka.wifigka;

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
import java.util.UUID;
import java.util.Map.Entry;

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
import cz.muni.fi.randgka.bluetoothgka.BluetoothFeatures;
import cz.muni.fi.randgka.bluetoothgka.BluetoothGKAParticipants;
import cz.muni.fi.randgka.gka.AugotGKA;
import cz.muni.fi.randgka.gka.GKAParticipant;
import cz.muni.fi.randgka.gka.GKAParticipantRole;
import cz.muni.fi.randgka.gka.GKAProtocol;
import cz.muni.fi.randgka.gka.GKAProtocolParams;
import cz.muni.fi.randgka.gka.GKAProtocolRound;
import cz.muni.fi.randgka.randgkaapp.GKAActivity;
import cz.muni.fi.randgka.tools.Constants;
import cz.muni.fi.randgka.tools.LongTermKeyProvider;
import cz.muni.fi.randgka.tools.PMessage;

public class WifiCommunicationService extends Service {

	private static final String SERVICE_NAME = "GKA_SERVICE";
	private static final UUID GKA_UUID = UUID.fromString("b89295fe-8eb2-4db1-9c19-a0f854d29db4");
	
	public static final String LEADER_RUN = "LEADER_RUN",
								MEMBER_RUN = "MEMBER_RUN",
								GKA_RUN = "GKA_RUN";
	
	private static final String MODP_GROUP_FILE = "modpgroups.xml";
	
	private BluetoothGKAParticipants participants;
	private Map<String, CommunicationThread> threads;
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
		try {
			init();
			
			context = this.getBaseContext();
			lbm = LocalBroadcastManager.getInstance(this);
			longTermKeyProvider = new LongTermKeyProvider(context, secureRandom);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		}
	}
	
	private void init() throws NoSuchAlgorithmException, NoSuchProviderException {
		init(null);
	}
	
	private void init(Intent intent) throws NoSuchAlgorithmException, NoSuchProviderException {
		returnKey = false;
		participants = new BluetoothGKAParticipants();
		if (threads != null) {
			for (Entry<String, CommunicationThread> e : threads.entrySet()) {
				if (e.getValue() != null) e.getValue().cancel();
			}
		}
		threads = new HashMap<String, CommunicationThread>();
		newParticipantId = 0;
		pHandler = new PMessageHandler();
		
		protocol = new AugotGKA();
		
		//Provider pr = new RandGKAProvider();
	    //try {
			secureRandom = new SecureRandom();// SecureRandom.getInstance(RandGKAProvider.RAND_EXTRACTOR, pr);
		/*} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}*/
		
		if (intent != null) {
			version = intent.getIntExtra("version", GKAProtocolParams.NON_AUTH);
			nonceLength = intent.getIntExtra("nonceLength", 0)/8;
			groupKeyLength = intent.getIntExtra("groupKeyLength", 0)/8;
			publicKeyLength = intent.getIntExtra("publicKeyLength", 0)/8;
			
			returnKey = intent.getBooleanExtra(Constants.RETRIEVE_KEY, false);
			if (intent.getBooleanExtra("freshKey", false)) longTermKeyProvider.generateKeys(publicKeyLength*8);
		}
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		try {
			String action = intent.getAction();
			
			if (action.equals(LEADER_RUN)) {
				init(intent);
				actAsServer(this, BluetoothAdapter.getDefaultAdapter());
			} 
			else if (action.equals(MEMBER_RUN)) {
				init(intent);
				connectToServer((BluetoothDevice)intent.getParcelableExtra("bluetoothDevice"), this);
			} 
			else if (action.equals(GKA_RUN)) {
				if (listenSocketThread != null) listenSocketThread.interrupt();
				if (!protocol.isInitialized()) {
					AssetManager am = getAssets();
					InputStream modpGroupIS = am.open(MODP_GROUP_FILE);
					
					GKAProtocolParams params = new GKAProtocolParams(version, nonceLength, groupKeyLength, publicKeyLength, modpGroupIS, longTermKeyProvider.getPrivateKey(publicKeyLength*8));
					protocol.init(participants, secureRandom, params);
				}
				GKAProtocolRound firstRound = protocol.nextRound(new PMessage((byte)0, 0, 0, 0, false, null, null));
				sendRound(firstRound);
			}
			else if (action.equals(GKAActivity.GET_PARAMS)) {
				printParams();
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
				if (!returnKey) printKey();
			} 
			else if (pr.getActionCode() == GKAProtocolRound.PRINT_PARTICIPANTS) {
				Intent getDevices = new Intent(GKAActivity.GET_PARTICIPANTS);
				getDevices.putExtra("participants", participants);
				lbm.sendBroadcast(getDevices);
			}
			if (pr.getMessages() != null && threads != null) {
				for (Entry<GKAParticipant, PMessage> e : pr.getMessages().entrySet()) {
					String address = participants.getBluetoothFeaturesFor(e.getKey().getId()).getMacAddress();
					threads.get(address).write(e.getValue().getBytes());
				}
			}
		}
	}
	
	private void printKey() {
		Intent printKeyIntent = new Intent(Constants.GET_GKA_KEY);
	    printKeyIntent.putExtra(Constants.KEY, protocol.getKey().toByteArray());
	    lbm.sendBroadcast(printKeyIntent);
	}
	
	@Override
	public void onDestroy() {
		pHandler = null;
		participants = null;
		if (listenSocketThread != null) listenSocketThread.interrupt();
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
								p.setDhLen(groupKeyLength);
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
	
	public void actAsServer(Context context, BluetoothAdapter bluetoothAdapter) throws IOException {
		try {
			GKAParticipant me = new GKAParticipant(0, bluetoothAdapter.getName()+" ("+ bluetoothAdapter.getAddress()+")", true, GKAParticipantRole.LEADER, nonceLength, groupKeyLength, 
					publicKeyLength, null, null, longTermKeyProvider.getPublicKey(publicKeyLength*8));
			BluetoothFeatures bf = new BluetoothFeatures(bluetoothAdapter.getAddress(), bluetoothAdapter.getName());
			participants.add(me, bf);
			threads.put(bf.getMacAddress(), null);
			
			BluetoothServerSocket bss = bluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, GKA_UUID);
			
			if (listenSocketThread != null && listenSocketThread.isAlive()) listenSocketThread.interrupt();
			listenSocketThread = new Thread(new ListenSocketThread(bss));
			listenSocketThread.start();
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		}
	}
	
	private class ListenSocketThread implements Runnable {
		
		private BluetoothServerSocket bss;
		
		public ListenSocketThread(BluetoothServerSocket bss) {
			this.bss = bss;
		}
		
		@Override
		public void run() {
			while (true) {
	            try {
	                BluetoothSocket bs = bss.accept();
		            if (bs != null) {
		            	// invoke communication thread
		    	        CommunicationThread ct = new CommunicationThread(bs);
		    	        ct.start();
		    	        
		    	        GKAParticipant participant = new GKAParticipant(++newParticipantId, bs.getRemoteDevice().getName()+" ("+ bs.getRemoteDevice().getAddress()+")", false, GKAParticipantRole.MEMBER, nonceLength, groupKeyLength, publicKeyLength, null, null, null);
		    			BluetoothFeatures bf = new BluetoothFeatures(bs.getRemoteDevice().getAddress(), bs.getRemoteDevice().getName());
		    			participants.add(participant, bf);
		    			threads.put(bf.getMacAddress(), ct);
		    	        
		    			Intent printDeviceIntent = new Intent(GKAActivity.PRINT_DEVICE);
		    			printDeviceIntent.putExtra("device", bf.getName()+" ("+bf.getMacAddress()+")");
		    			lbm.sendBroadcast(printDeviceIntent);
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
				if (bss != null) bss.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public void connectToServer(BluetoothDevice bluetoothDevice, Context context) {
		try {
            // GKA_UUID is the app's UUID string, also used by the server code
            BluetoothSocket bs = bluetoothDevice.createRfcommSocketToServiceRecord(GKA_UUID);
            bs.connect();
	        // invoke communication thread
	        CommunicationThread ct = new CommunicationThread(bs);
	        ct.start();
	        GKAParticipant participant = new GKAParticipant(0, bluetoothDevice.getName()+" ("+ bluetoothDevice.getAddress()+")", false, GKAParticipantRole.LEADER, nonceLength, groupKeyLength, publicKeyLength, null, null, null);
			BluetoothFeatures bf = new BluetoothFeatures(bluetoothDevice.getAddress(), bluetoothDevice.getName());
			participants.add(participant, bf);
			threads.put(bf.getMacAddress(), ct);
	        
			GKAParticipant me = new GKAParticipant(1, BluetoothAdapter.getDefaultAdapter().getName()+" ("+ BluetoothAdapter.getDefaultAdapter().getAddress()+")", true, GKAParticipantRole.MEMBER, nonceLength, groupKeyLength, publicKeyLength, null, null, null);
			BluetoothFeatures myBf = new BluetoothFeatures(BluetoothAdapter.getDefaultAdapter().getAddress(), BluetoothAdapter.getDefaultAdapter().getName());
			participants.add(me, myBf);
			threads.put(myBf.getMacAddress(), null);
			
        } catch (IOException e) {
        	e.printStackTrace();
        }
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
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
	 
	    public void run() {
	        byte[] buffer = new byte[2048];  // buffer store for the stream
	        int bytes; // bytes returned from read()
	        
	        // Keep listening to the InputStream until an exception occurs
	        while (true) {
	            try {
	                // Read from the InputStream
	                bytes = inStream.read(buffer);
	                PMessage pMessage = new PMessage(buffer);
	                Arrays.fill(buffer, (byte) 0x00);
	                
	                Message m = pHandler.obtainMessage();
	                
	                Bundle pMessageBundle = new Bundle();
	                pMessageBundle.putSerializable("pMessage", pMessage);
	                
	        		m.setData(pMessageBundle);
	                m.sendToTarget();
	            } catch (IOException e) {
	                break;
	            }
	        }
	    }
	    
	    /* Call this from the main activity to send data to the remote device */
	    public void write(byte[] bytes) {
	        try {
	            outStream.write(bytes);
	        } catch (IOException e) {
	        	e.printStackTrace();
	        }
	    }
	 
	    /* Call this from the main activity to shutdown the connection */
	    public void cancel() {
	        try {
	            bs.close();
	        } catch (IOException e) {
	        	e.printStackTrace();
	        }
	    }
	}
}
