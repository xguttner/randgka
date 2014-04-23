package cz.muni.fi.randgka.bluetoothgka;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import cz.muni.fi.randgka.gka.AugotGKA;
import cz.muni.fi.randgka.gka.GKAParticipant;
import cz.muni.fi.randgka.gka.GKAParticipantRole;
import cz.muni.fi.randgka.gka.GKAProtocol;
import cz.muni.fi.randgka.gka.GKAProtocolParams;
import cz.muni.fi.randgka.gka.GKAProtocolRound;
import cz.muni.fi.randgka.library.PublicKeyCryptography;
import cz.muni.fi.randgka.provider.RandGKAProvider;
import cz.muni.fi.randgka.tools.Constants;
import cz.muni.fi.randgka.tools.MessageAction;
import cz.muni.fi.randgka.tools.PMessage;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;

public class BluetoothCommunicationService extends Service {
	
	private static final String SERVICE_NAME = "GKA_SERVICE";
	private static final UUID GKA_UUID = UUID.fromString("b89295fe-8eb2-4db1-9c19-a0f854d29db4");
	
	private BluetoothGKAParticipants participants;
	private Map<String, CommunicationThread> threads;
	private PMessageHandler pHandler;
	private GKAProtocol protocol;
	private PublicKeyCryptography publicKeyCryptography;
	private Thread listenSocketThread;
	private LocalBroadcastManager lbm;
	private Context context;
	private int newParticipantId;
	private SharedPreferences sp;
	private boolean isAuth;
	private Integer nonceLength,
					groupKeyLength,
					publicKeyLength;
	
	@Override
	public void onCreate() {
		init();
		sp = PreferenceManager.getDefaultSharedPreferences(this);
		isAuth = sp.getBoolean("pref_gka_authorized", false);
		nonceLength = Integer.parseInt(sp.getString("pref_gka_nonce_len", "1024"))/8;
		groupKeyLength = Integer.parseInt(sp.getString("pref_gka_key_len", "1024"))/8;
		publicKeyLength = Integer.parseInt(sp.getString("pref_public_key_len", "1024"))/8;
		
		pHandler = new PMessageHandler();
		context = this.getBaseContext();
		lbm = LocalBroadcastManager.getInstance(this);
		publicKeyCryptography = new PublicKeyCryptography(context);
	}
	
	private void init() {
		init(null);
	}
	
	private void init(Intent intent) {
		participants = new BluetoothGKAParticipants();
		threads = new HashMap<String, CommunicationThread>();
		newParticipantId = 0;
		
		if (intent != null) {
			isAuth = intent.getBooleanExtra("isAuth", isAuth);
			nonceLength = intent.getIntExtra("nonceLength", nonceLength*8)/8;
			groupKeyLength = intent.getIntExtra("groupKeyLength", groupKeyLength*8)/8;
			publicKeyLength = intent.getIntExtra("publicKeyLength", publicKeyLength*8)/8;
			
			if (intent.getStringExtra("protocol").equals("Augot")) {
				protocol = new AugotGKA();
			}
		}
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent.getAction();
		if (action.equals(Constants.SERVER_START)) {
			try {
				init(intent);
				actAsServer(this, BluetoothAdapter.getDefaultAdapter());
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (action.equals(Constants.CONNECT_TO_DEVICE)) {
			init();
			BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("bluetoothDevice");
			connectToServer(bluetoothDevice, this);
		} else if (action.equals(Constants.RUN_GKA_PROTOCOL)) {
			if (listenSocketThread != null) listenSocketThread.interrupt();
			reset();
			
			GKAProtocolParams params = new GKAProtocolParams(isAuth, nonceLength, groupKeyLength, publicKeyCryptography.getPrivateKey(publicKeyLength));
			
			//Provider pr = new RandGKAProvider();
	        //try {
				SecureRandom secureRandom = new SecureRandom();// SecureRandom.getInstance("UHRandExtractor", pr);
				protocol.init(participants, secureRandom, params);
				GKAProtocolRound firstRound = protocol.nextRound(new PMessage(MessageAction.GKA_PROTOCOL, (byte)0, 0, 0, false, null, null));
				sendRound(firstRound);
	        /*} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}*/
		} else if (action.equals(Constants.GET_PARAMS)) {
			Intent paramsIntent = new Intent(Constants.GET_PARAMS);
			paramsIntent.putExtra("protocol", protocol.getClass().getSimpleName());
			paramsIntent.putExtra("isAuth", isAuth);
			paramsIntent.putExtra("nonceLength", nonceLength);
			paramsIntent.putExtra("groupKeyLength", groupKeyLength);
			paramsIntent.putExtra("publicKeyLength", publicKeyLength);
			lbm.sendBroadcast(paramsIntent);
		}
		return START_NOT_STICKY;
	}

	private void reset() {
		if (participants !=null) {
			for (GKAParticipant p : participants.getParticipants()) {
				p.setDHPublicKey(null);
			}
		}
	}

	private void sendRound(GKAProtocolRound pr) {
		if (pr != null) {
			if (pr.getActionCode() == GKAProtocolRound.SUCCESS) {
				printKey();
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
	    printKeyIntent.putExtra("key", protocol.getKey().toByteArray());
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
				if (pMessage != null) {
					switch (pMessage.getAction()) {
						
						case ADD_PARTICIPANT:
							GKAParticipant participantReceived = new GKAParticipant(pMessage.getMessage());
							byte[] bfBytes = new byte[pMessage.getLength() - participantReceived.length()];
							System.arraycopy(pMessage.getMessage(), participantReceived.length(), bfBytes, 0, pMessage.getLength() - participantReceived.length());
							
							participants.merge(participantReceived);
							
							Intent getDevices = new Intent(Constants.GET_PARTICIPANTS);
							getDevices.putExtra("participants", participants);
							lbm.sendBroadcast(getDevices);
							
							// sending params to new participant
							PMessage paramsMessage = new PMessage(MessageAction.SEND_PARAMS, (byte)0, (byte)0, null, false, null, null);
							CommunicationThread ct = threads.get(participants.getBluetoothFeaturesFor(participantReceived.getId()));
							ct.write(paramsMessage.getBytes());
							
							PMessage broadcastParticipantsMessage = new PMessage(MessageAction.BROADCAST_PARTICIPANTS, (byte)0, (byte)0, participants.length(), false, participants.getBytes(), null);
							ct = null;
							String macAddress = null;
							for (GKAParticipant p : participants.getAllButMe().getParticipants()) {
								macAddress = participants.getBluetoothFeaturesFor(p.getId()).getMacAddress();
								if (threads.containsKey(macAddress)) {
									ct = threads.get(macAddress);
									if (ct != null) ct.write(broadcastParticipantsMessage.getBytes());
								}
							}
							
							break;
					
						case BROADCAST_PARTICIPANTS:
							
							BluetoothGKAParticipants ps = new BluetoothGKAParticipants(pMessage.getMessage());
							
							participants.mergeUsingMac(ps);
							
							Intent getDevices2 = new Intent(Constants.GET_PARTICIPANTS);
							getDevices2.putExtra("participants", participants);
							lbm.sendBroadcast(getDevices2);
							break;
						
						case GKA_PROTOCOL:
								sendRound(protocol.nextRound(pMessage));
							break;
							
						case INIT_GKA_PROTOCOL:
							reset();
							GKAProtocolParams params = new GKAProtocolParams(sp.getBoolean("pref_gka_authorized", false), 
									Integer.parseInt(sp.getString("pref_gka_nonce_length", "1024"))/8, 
									Integer.parseInt(sp.getString("pref_gka_key_length", "1024"))/8, publicKeyCryptography.getPrivateKey());
							
							Provider pr = new RandGKAProvider();
					        //try {
								SecureRandom secureRandom = new SecureRandom();//SecureRandom.getInstance("UHRandExtractor", pr);
								protocol.init(participants, secureRandom, params);
								sendRound(protocol.nextRound(pMessage));
					        /*} catch (NoSuchAlgorithmException e) {
								e.printStackTrace();
							}*/
						default:
							break;
					}
				}
			}
		}
	}
	
	public void actAsServer(Context context, BluetoothAdapter bluetoothAdapter) throws IOException {
		try {
			GKAParticipant me = new GKAParticipant(0, true, GKAParticipantRole.LEADER, null, publicKeyCryptography.getPublicKey(publicKeyLength));
			BluetoothFeatures bf = new BluetoothFeatures(bluetoothAdapter.getAddress(), bluetoothAdapter.getName());
			participants.add(me, bf);
			threads.put(bf.getMacAddress(), null);
			
			BluetoothServerSocket bss = bluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, GKA_UUID);
			
			listenSocketThread = new Thread(new ListenSocketThread(bss));
			listenSocketThread.start();
			
		} catch (IOException e) {
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
		    	        
		    	        GKAParticipant participant = new GKAParticipant(++newParticipantId, false, GKAParticipantRole.MEMBER, null, null);
		    			BluetoothFeatures bf = new BluetoothFeatures(bs.getRemoteDevice().getAddress(), bs.getRemoteDevice().getName());
		    			participants.add(participant, bf);
		    			threads.put(bf.getMacAddress(), ct);
		    	        
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
				// TODO Auto-generated catch block
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
	        
	        GKAParticipant participant = new GKAParticipant(0, false, GKAParticipantRole.LEADER, null, null);
			BluetoothFeatures bf = new BluetoothFeatures(bluetoothDevice.getAddress(), bluetoothDevice.getName());
			participants.add(participant, bf);
			threads.put(bf.getMacAddress(), ct);
	        
			GKAParticipant me = new GKAParticipant(1, true, GKAParticipantRole.MEMBER, null, null);
			BluetoothFeatures myBf = new BluetoothFeatures(BluetoothAdapter.getDefaultAdapter().getAddress(), BluetoothAdapter.getDefaultAdapter().getName());
			participants.add(me, myBf);
			threads.put(myBf.getMacAddress(), null);
	        
			byte[] messageBytes = new byte[me.length()+myBf.length()];
			System.arraycopy(me.getBytes(), 0, messageBytes, 0, me.length());
			System.arraycopy(myBf.getBytes(), 0, messageBytes, me.length(), myBf.length());

	        ct.write((new PMessage(MessageAction.ADD_PARTICIPANT, (byte)1, (byte)1, me.length()+myBf.length(), false, messageBytes, null)).getBytes());
			
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
		        } catch (IOException e) { }
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
	        } catch (IOException e) {}
	    }
	 
	    /* Call this from the main activity to shutdown the connection */
	    public void cancel() {
	        try {
	            bs.close();
	        } catch (IOException e) {}
	    }
	}
}
