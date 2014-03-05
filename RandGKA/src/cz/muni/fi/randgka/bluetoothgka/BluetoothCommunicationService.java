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
import cz.muni.fi.randgka.library.Constants;
import cz.muni.fi.randgka.library.PublicKeyCryptography;
import cz.muni.fi.randgka.provider.RandGKAProvider;
import cz.muni.fi.randgka.randgkaapp.GKAProtocolPrintKeyAppActivity;
import cz.muni.fi.randgka.tools.MessageAction;
import cz.muni.fi.randgka.tools.PMessage;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;

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
	
	@Override
	public void onCreate() {
		participants = new BluetoothGKAParticipants();
		threads = new HashMap<String, CommunicationThread>();
		pHandler = new PMessageHandler();
		protocol = new AugotGKA();
		lbm = LocalBroadcastManager.getInstance(this);
		context = this.getBaseContext();
		publicKeyCryptography = new PublicKeyCryptography(context);
		newParticipantId = 0;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent.getAction();
		if (action.equals(Constants.SERVER_START)) {
			try {
				actAsServer(this, BluetoothAdapter.getDefaultAdapter());
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (action.equals(Constants.CONNECT_TO_DEVICE)) {
			BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("bluetoothDevice");
			connectToServer(bluetoothDevice, this);
		} else if (action.equals(Constants.RUN_GKA_PROTOCOL)) {
			if (listenSocketThread != null) listenSocketThread.interrupt();
			
			GKAProtocolParams params = new GKAProtocolParams(false, 128, 128);
			
			Provider pr = new RandGKAProvider();
	        //try {
				SecureRandom secureRandom = new SecureRandom();// SecureRandom.getInstance("UHRandExtractor", pr);
				protocol.init(participants, secureRandom, params);
				GKAProtocolRound firstRound = protocol.nextRound(new PMessage(MessageAction.GKA_PROTOCOL, (byte)0, 0, 0, null));
				sendRound(firstRound);
	        /*} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}*/
		}
		return START_NOT_STICKY;
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
		Intent runProtocolIntent = new Intent(context, GKAProtocolPrintKeyAppActivity.class);
	    runProtocolIntent.setAction(Intent.ACTION_VIEW);
	    runProtocolIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    runProtocolIntent.putExtra("key", protocol.getKey().toByteArray());
	    context.startActivity(runProtocolIntent);
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
							BluetoothFeatures bf = new BluetoothFeatures(bfBytes);
							
							participants.setPublicKey(bf);
							
							Intent getDevices = new Intent(Constants.GET_PARTICIPANTS);
							getDevices.putExtra("participants", participants);
							lbm.sendBroadcast(getDevices);
							
							PMessage broadcastParticipantsMessage = new PMessage(MessageAction.BROADCAST_PARTICIPANTS, (byte)0, (byte)0, participants.length(), participants.getBytes());
							CommunicationThread ct = null;
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
							
							if (!protocol.isInitialized()) {
								GKAProtocolParams params = new GKAProtocolParams(false, 128, 128);
								
								Provider pr = new RandGKAProvider();
						        //try {
									SecureRandom secureRandom = new SecureRandom();//SecureRandom.getInstance("UHRandExtractor", pr);
									protocol.init(participants, secureRandom, params);
									sendRound(protocol.nextRound(pMessage));
						        /*} catch (NoSuchAlgorithmException e) {
									e.printStackTrace();
								}*/
							}
							else {
								sendRound(protocol.nextRound(pMessage));
							}
							
							break;
							
						default:
							break;
					}
				}
			}
		}
	}
	
	public void actAsServer(Context context, BluetoothAdapter bluetoothAdapter) throws IOException {
		try {
			GKAParticipant me = new GKAParticipant(0, true, GKAParticipantRole.LEADER, null);
			BluetoothFeatures bf = new BluetoothFeatures(bluetoothAdapter.getAddress(), bluetoothAdapter.getName(), publicKeyCryptography.getPublicKey());
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
		    	        
		    	        GKAParticipant participant = new GKAParticipant(++newParticipantId, false, GKAParticipantRole.MEMBER, null);
		    			BluetoothFeatures bf = new BluetoothFeatures(bs.getRemoteDevice().getAddress(), bs.getRemoteDevice().getName(), null);
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
            // MY_UUID is the app's UUID string, also used by the server code
            BluetoothSocket bs = bluetoothDevice.createRfcommSocketToServiceRecord(GKA_UUID);
            
            bs.connect();
	        // invoke communication thread
	        CommunicationThread ct = new CommunicationThread(bs);
	        ct.start();
	        
	        GKAParticipant participant = new GKAParticipant(0, false, GKAParticipantRole.LEADER, null);
			BluetoothFeatures bf = new BluetoothFeatures(bluetoothDevice.getAddress(), bluetoothDevice.getName(), null);
			participants.add(participant, bf);
			threads.put(bf.getMacAddress(), ct);
	        
			GKAParticipant me = new GKAParticipant(1, true, GKAParticipantRole.MEMBER, null);
			BluetoothFeatures myBf = new BluetoothFeatures(BluetoothAdapter.getDefaultAdapter().getAddress(), BluetoothAdapter.getDefaultAdapter().getName(), publicKeyCryptography.getPublicKey());
			participants.add(me, myBf);
			threads.put(myBf.getMacAddress(), null);
	        
			byte[] messageBytes = new byte[me.length()+myBf.length()];
			System.arraycopy(me.getBytes(), 0, messageBytes, 0, me.length());
			System.arraycopy(myBf.getBytes(), 0, messageBytes, me.length(), myBf.length());

	        ct.write((new PMessage(MessageAction.ADD_PARTICIPANT, (byte)1, (byte)1, me.length()+myBf.length(), messageBytes)).getBytes());
			
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
