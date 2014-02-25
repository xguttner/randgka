package cz.muni.fi.randgka.randgkamiddle;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cz.muni.fi.randgka.library.AugotGKA;
import cz.muni.fi.randgka.library.ByteSequence;
import cz.muni.fi.randgka.library.ByteableList;
import cz.muni.fi.randgka.library.CommunicationThread;
import cz.muni.fi.randgka.library.Constants;
import cz.muni.fi.randgka.library.GKAParticipant;
import cz.muni.fi.randgka.library.GKAParticipants;
import cz.muni.fi.randgka.library.GKAProtocol;
import cz.muni.fi.randgka.library.MessageAction;
import cz.muni.fi.randgka.library.PMessage;
import cz.muni.fi.randgka.library.ParticipateRole;
import cz.muni.fi.randgka.library.ProtocolRound;
import cz.muni.fi.randgka.library.PublicKeyCryptography;
import cz.muni.fi.randgkaapp.GKAProtocolPrintKeyAppActivity;
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
import android.util.Log;

public class ConnectionService extends Service {
	
	private static final String SERVICE_NAME = "GKA_SERVICE";
	private static final UUID GKA_UUID = UUID.fromString("b89295fe-8eb2-4db1-9c19-a0f854d29db4");
	
	private GKAParticipants participants;
	private PMessageHandler pHandler;
	private GKAProtocol protocol;
	private PublicKeyCryptography publicKeyCryptography;
	private Thread listenSocketThread;
	private LocalBroadcastManager lbm;
	private Context context;
	
	@Override
	public void onCreate() {
		participants = new GKAParticipants();
		pHandler = new PMessageHandler();
		protocol = new AugotGKA();
		lbm = LocalBroadcastManager.getInstance(this);
		context = this.getBaseContext();
		publicKeyCryptography = new PublicKeyCryptography(context);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent.getAction();
		if (action.equals(Constants.SERVER_START)) {
			try {
				Log.d("service", "actAsServer");
				actAsServer(this, BluetoothAdapter.getDefaultAdapter());
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (action.equals(Constants.CONNECT_TO_DEVICE)) {
			Log.d("service", "connectToDevice");
			BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("bluetoothDevice");
			connectToDevice(bluetoothDevice, this);
		} else if (action.equals(Constants.RUN_GKA_PROTOCOL)) {
			if (listenSocketThread != null) listenSocketThread.interrupt();
			
			startProtocol();
			
		} else if (action.equals(Constants.PROTOCOL_RANDOMNESS)) {
			protocol.setRandSequence((ByteSequence)intent.getSerializableExtra("randSequence"));
			
			ProtocolRound firstRound = protocol.runRound(null);
			sendRound(firstRound);
		}
		return START_NOT_STICKY;
	}
	
	private void sendRound(ProtocolRound pr) {
		if (pr != null) {
			if (pr.isKeyEstablished()) {
				printKey();
			}
			if (pr.getTargets() !=null) {
				for (GKAParticipant p : pr.getTargets()) {
					p.getChannel().write(pr.getMessage().getBytes());
				}
			}
		}
	}
	
	private void printKey() {
		Intent runProtocolIntent = new Intent(context, GKAProtocolPrintKeyAppActivity.class);
	    runProtocolIntent.setAction(Intent.ACTION_VIEW);
	    runProtocolIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    runProtocolIntent.putExtra("key", protocol.getKey());
	    context.startActivity(runProtocolIntent);
	}
	
	private void startProtocol() {
		
		protocol.init(participants, 128, AugotGKA.UNAUTHORIZED);
		
		Intent runProtocolIntent = new Intent(context, GKAProtocolRunActivity.class);
	    runProtocolIntent.setAction(Intent.ACTION_VIEW);
	    runProtocolIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    runProtocolIntent.putExtra("randDataLength", protocol.getRandomnessLength());
	    context.startActivity(runProtocolIntent);
	}
	
	@Override
	public void onDestroy() {
		pHandler = null;
		participants = null;
		if (listenSocketThread != null) listenSocketThread.interrupt();
	}
	/*
	private static class PMessageHandlerStatic extends Handler {
		
		private static Context context;
		
		public static void setContext(Context context) {
		}
		
		@Override
		public void handleMessage(Message message) {
			Log.d("handle", "message");
			Bundle bundle = message.getData();
			if (bundle != null) {
				PMessage pMessage = (PMessage)bundle.getSerializable("pMessage");
				if (pMessage != null) {
					switch (pMessage.getAction()) {
					
						case BROADCAST_PARTICIPANTS:
							byte[] participantsString = null;
							if (pMessage.getMessage()==null) {
								PMessage participantsMessage = new PMessage(MessageAction.BROADCAST_PARTICIPANTS, (byte)0, (byte)0, participants.toString().getBytes().length, participants.toString().getBytes());
								for (GKAParticipant p : participants) {
									if (p.getChannel() != null) p.getChannel().write(participantsMessage.getBytes());
								}
								participantsString = participants.toString().getBytes();
							}
							else {
								participantsString = pMessage.getMessage();
							}
							this.
							Intent getDevices = new Intent(Constants.GET_PARTICIPANTS);
							getDevices.putExtra("devices", participantsString);
							LocalBroadcastManager.getInstance(context).sendBroadcast(getDevices);
							break;
						
						case GKA_PROTOCOL:
							Intent intent = new Intent(context, GKAProtocolRandomDataActivity.class);
						    intent.setAction(Intent.ACTION_VIEW);
						    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						    intent.putExtra("minEntropyDataLength", 839);
						    context.startActivity(intent);
							break;
							
						default:
							break;
					}
				}
			}
		}
	}
	*/
	private class PMessageHandler extends Handler {
		@Override
		public void handleMessage(Message message) {
			Log.d("handle", "message");
			Bundle bundle = message.getData();
			if (bundle != null) {
				PMessage pMessage = (PMessage)bundle.getSerializable("pMessage");
				if (pMessage != null) {
					switch (pMessage.getAction()) {
						
						case ADD_PARTICIPANT:
							GKAParticipant participantReceived = new GKAParticipant(pMessage.getMessage());
							Log.d("participant", participantReceived.toString());
							participants.mergeParticipant(participantReceived);
							
							Intent getDevices = new Intent(Constants.GET_PARTICIPANTS);
							getDevices.putExtra("participants", participants);
							lbm.sendBroadcast(getDevices);
							
							for (GKAParticipant p : participants.getParticipants()) {
								PMessage broadcastParticipantsMessage = new PMessage(MessageAction.BROADCAST_PARTICIPANTS, (byte)0, (byte)0, participants.byteLength(), participants.getBytes());
								if (p.getChannel() != null) p.getChannel().write(broadcastParticipantsMessage.getBytes());
							}
							
							break;
					
						case BROADCAST_PARTICIPANTS:
							
							Log.d("neco", pMessage.toString());
							GKAParticipants ps = new GKAParticipants();
							ps.fromBytes(pMessage.getMessage());
							participants.mergeParticipants(ps);
							
							Intent getDevices2 = new Intent(Constants.GET_PARTICIPANTS);
							getDevices2.putExtra("participants", participants);
							lbm.sendBroadcast(getDevices2);
							break;
						
						case START_GKA_PROTOCOL:
							
							protocol.putMessage(pMessage);
							
							startProtocol();
							
							break;
						
						case GKA_PROTOCOL:
							
							ProtocolRound round = protocol.runRound(pMessage);
							sendRound(round);
							
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
			GKAParticipant me = new GKAParticipant(bluetoothAdapter.getName(),bluetoothAdapter.getAddress(),null,ParticipateRole.LEADER, publicKeyCryptography.getPublicKey(), true);
			participants.add(me);
			
			BluetoothServerSocket bss = bluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, GKA_UUID);
			
			listenSocketThread = new Thread(new ListenSocketThread(bss));
			listenSocketThread.start();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
		    	        CommunicationThread ct = new CommunicationThread(bs, pHandler);
		    	        ct.start();
		    	        
		    	        GKAParticipant gkaParticipant = new GKAParticipant(bs.getRemoteDevice().getName(),bs.getRemoteDevice().getAddress(), ct, ParticipateRole.MEMBER, null, false);
		    	        participants.add(gkaParticipant);
		    	        
		    	        /*PMessage newDevicePMessage = new PMessage(MessageAction.BROADCAST_PARTICIPANTS, (byte)0, (byte)0, (byte)0, participants.getLeader().getBytes());
		    	        newDevicePMessage.obtainMessage(pHandler.obtainMessage()).sendToTarget();*/
		            }
	            } catch (IOException e) {
	            	Log.d("server", "fail");
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
	
	public void connectToDevice(BluetoothDevice bluetoothDevice, Context context) {
		try {
            // MY_UUID is the app's UUID string, also used by the server code
            BluetoothSocket bs = bluetoothDevice.createRfcommSocketToServiceRecord(GKA_UUID);
            
            bs.connect();
	        // invoke communication thread
	        CommunicationThread ct = new CommunicationThread(bs, pHandler);
	        ct.start();
	        
	        GKAParticipant me = new GKAParticipant(BluetoothAdapter.getDefaultAdapter().getName(),BluetoothAdapter.getDefaultAdapter().getAddress(),
	        		null,ParticipateRole.MEMBER, publicKeyCryptography.getPublicKey(), true);
			participants.add(me);
	        
	        GKAParticipant gkaParticipant = new GKAParticipant(bluetoothDevice.getName(), bluetoothDevice.getAddress(), ct, ParticipateRole.LEADER, null, false);
	        participants.add(gkaParticipant);
	        
	        ct.write((new PMessage(MessageAction.ADD_PARTICIPANT, (byte)1, (byte)1, me.length(), me.getBytes())).getBytes());
			
        } catch (IOException e) {
        	//TODO logs...
        	Log.d("string", "blbe");
        }
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
