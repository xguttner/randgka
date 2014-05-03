package cz.muni.fi.randgka.randgkaapp;

import cz.muni.fi.randgkaapp.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class GKATechDecisionActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gkatech_decision);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.gkatech_decision, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return false;
	}
	

	public void moveToBluetoothGKA(View view) {
		Intent moving = new Intent(this, BluetoothGKARoleActivity.class);
		if (getIntent().getBooleanExtra("return_key", false)) {
			moving.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
			moving.putExtra("return_key", true);
		}
		startActivity(moving);
	}
	
	public void moveToWifiGKA(View view) {
		/*Intent moving = new Intent(this, WifiGKARoleActivity.class);
		startActivity(moving);*/
	}

}
