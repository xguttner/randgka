package cz.muni.fi.androidrandextr.communication;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import cz.muni.fi.androidrandextr.AuthenticationClientActivity;
import cz.muni.fi.androidrandextr.AuthenticationServerActivity;
import cz.muni.fi.randgka.library.CommunicationThread;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Spinner;


public class BluetoothCommunication {
	
	private BluetoothSocket bs;
	private CommunicationThread ct;
	private ProtocolHandler protocolHandler;
	
	private static final String SERVICE_NAME = "GKA_SERVICE";
	private static final UUID GKA_UUID = UUID.fromString("b89295fe-8eb2-4db1-9c19-a0f854d29db4");
	
	public BluetoothCommunication(ProtocolHandler protocolHandler) {
		this.protocolHandler = protocolHandler;
		this.protocolHandler.setCT(ct);
	}
	
	private class AcceptThread extends Thread {
		
	    private BluetoothServerSocket bss;
	    private Context context;
	 
	    public AcceptThread(Context context, BluetoothAdapter bluetoothAdapter) {
	    	this.context = context;
    		try { 
				bss = bluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, GKA_UUID);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	 
	    public void run() {
	    	
	        while (true) {
	            try {
	            	Log.d("server", "try");
	                bs = bss.accept();
	                Log.d("server", "tried");
		            if (bs != null) break;
	            } catch (IOException e) {
	            	Log.d("server", "fail");
	                break;
	            }
	        }
	        Log.d("server", "connected");
	        // invoke communication thread
	        ct = new CommunicationThread(bs, protocolHandler);
	        Thread comThread = new Thread(ct);
	        comThread.start();
	        
	        protocolHandler.setCT(ct);
	        protocolHandler.runProtocol();
	        
	        /*Intent moving = new Intent(context, AuthenticationServerActivity.class);
	        moving.putExtra("communicationThread", ct);
			context.startActivity(moving);*/
	        
	        this.interrupt();
	    }

	    @Override
	    public void interrupt() {
	        try {
	            bss.close();
	        } catch (IOException e) {
	        	e.printStackTrace();
	        }
	        super.interrupt();
	    }
	}
	
	private class ConnectThread extends Thread {
		
		private Context context;
	 
	    public ConnectThread(Context context, BluetoothDevice device) {
	    	
	    	this.context = context;
	    	
	        try {
	            // MY_UUID is the app's UUID string, also used by the server code
	            bs = device.createRfcommSocketToServiceRecord(GKA_UUID);
	        } catch (IOException e) {
	        	//TODO logs...
	        	Log.d("string", "blbe");
	        }
	    }
	 
	    public void run() {
	        try {
	        	Log.d("connectThread", "before");
	            bs.connect();
	            Log.d("connectThread", "connected");
	        } catch (IOException connectException) {
	            try {
	            	Log.d("fail", "in connecting to server");
	                bs.close();
	            } catch (IOException closeException) { }
	            return;
	        }
	        
	        Log.d("connectThread", "ok");
	        // invoke communication thread
	        ct = new CommunicationThread(bs, protocolHandler);
	        Thread comThread = new Thread(ct);
	        comThread.start();
	        
	        protocolHandler.setCT(ct);
	        //protocolHandler.runProtocol();
			
			this.interrupt();
	    }

	    @Override
	    public void interrupt() {
	        super.interrupt();
	    }
	}
	
	public void actAsServer(Context context, BluetoothAdapter bluetoothAdapter) throws IOException {
		Thread acceptThread = new Thread(new AcceptThread(context, bluetoothAdapter));
		acceptThread.start();
	}
	
	public void connectToDevice(BluetoothDevice bluetoothDevice, Context context) {
		Thread connectThread = new Thread(new ConnectThread(context, bluetoothDevice));
		connectThread.start();
	}
}
