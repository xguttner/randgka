package cz.muni.fi.randgka.randgkaapp;

import cz.muni.fi.randgka.library.Constants;
import cz.muni.fi.randgka.library.ProtocolBroadcastReceiver;
import cz.muni.fi.randgka.randgkamiddle.ConnectionService;
import cz.muni.fi.randgkaapp.R;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

public class GKAProtocolStartAppActivity extends Activity {

	private ProtocolBroadcastReceiver receiver;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gkaprotocol);
		
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
		Intent runProtocolIntent = new Intent(this, ConnectionService.class);
		runProtocolIntent.setAction(Constants.RUN_GKA_PROTOCOL);
		startService(runProtocolIntent);
	}
}
