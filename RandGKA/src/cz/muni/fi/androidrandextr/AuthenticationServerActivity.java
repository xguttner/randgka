package cz.muni.fi.androidrandextr;

import cz.muni.fi.androidrandextr.gka.AuthenticationProtocol;
import cz.muni.fi.randgka.library.CommunicationThread;
import cz.muni.fi.randgkaapp.R;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class AuthenticationServerActivity extends Activity {

	private CommunicationThread ct;
	private AuthenticationProtocol ap;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_authentication_server);
		
		ct = (CommunicationThread)getIntent().getSerializableExtra("communicationThread");
		
		waitForAuthChallenge();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.authentication_server, menu);
		return true;
	}

	private void waitForAuthChallenge() {
		//Handler handler = ct.getMainThreadHandler();
		//handler.
	}
	
}
