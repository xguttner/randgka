package cz.muni.fi.randgka.randgkaapp;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;

import cz.muni.fi.randgka.bluetoothgka.BluetoothCommunicationService;
import cz.muni.fi.randgka.library.Constants;
import cz.muni.fi.randgka.library.ProtocolBroadcastReceiver;
import cz.muni.fi.randgka.provider.RandGKAProvider;
import cz.muni.fi.randgka.provider.minentropy.CameraMES;
import cz.muni.fi.randgka.provider.minentropy.CameraMESHolder;
import cz.muni.fi.randgkaapp.R;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

public class GKAProtocolStartAppActivity extends Activity {

	private ProtocolBroadcastReceiver receiver;
	private SecureRandom sr;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gkaprotocol);
		
		SurfaceView surface = null;
		
		surface=(SurfaceView)findViewById(R.id.surfaceView1);
        SurfaceHolder holder=surface.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        CameraMES cameraMES = new CameraMES();
        cameraMES.initialize(surface);
        CameraMESHolder.cameraMES = cameraMES;
		
		IntentFilter protocolFilter = new IntentFilter();
		protocolFilter.addAction(Constants.GET_PARTICIPANTS);
		protocolFilter.addAction(Constants.GET_GKA_KEY);
		receiver = new ProtocolBroadcastReceiver();
		receiver.setTextView((TextView)findViewById(R.id.protocol_participants));
		LocalBroadcastManager.getInstance(this).registerReceiver(receiver, protocolFilter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.gkaprotocol, menu);
		return true;
	}

	public void runProtocol(View view) {
		Intent runProtocolIntent = new Intent(this, BluetoothCommunicationService.class);
		runProtocolIntent.setAction(Constants.RUN_GKA_PROTOCOL);
		startService(runProtocolIntent);
	}
}
