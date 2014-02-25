package cz.muni.fi.randgka.library;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class CommunicationThread extends CommunicationChannel implements Serializable {
	
	private static final long serialVersionUID = -2952068172024143729L;
	
	private BluetoothSocket bs;
    private InputStream inStream;
    private OutputStream outStream;
    private Handler pHandler;
 
    public CommunicationThread(BluetoothSocket bs, Handler pHandler) {
    	this.bs = bs;
    	this.pHandler = pHandler;
    	this.setStreams();
    }
    
    public BluetoothDevice getBluetoothDevice() {
    	return bs.getRemoteDevice();
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
        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()
        
        //protocolHandler.runProtocol();
        
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
                
        		//pMessage.obtainMessage(pHandler.obtainMessage()).sendToTarget();
                
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
