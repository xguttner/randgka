package cz.muni.fi.androidrandextr;

import cz.muni.fi.androidrandextr.gka.AuthenticationProtocol;
import cz.muni.fi.randgka.library.CommunicationThread;
import cz.muni.fi.randgkaapp.R;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;

public class AuthenticationPINActivity extends Activity {

	private CommunicationThread ct;
	private AuthenticationProtocol ap;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_authentication_pin);
		
		ct = (CommunicationThread)getIntent().getSerializableExtra("communicationThread");
		
		runProtocolFirstRound();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.authentication_pin, menu);
		return true;
	}
	
	private void runProtocolFirstRound() {
		ap = new AuthenticationProtocol(this);
		ct.write(ap.getPublicKey());
	}
	
	public void proceedAuthentication(View view) {
		
	}

}
