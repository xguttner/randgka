package cz.muni.fi.randgka.randgkaapp;

import cz.muni.fi.randgkaapp.R;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;

/**
 * 
 * @author gitti
 * 
 * This class serves as an entrance point into the application. 
 * Its exported attribute is set to true (i.e. it can be invoked by any application). 
 * It provides a simple decision point with what functionality to continue.
 * 
 */
public class MainActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.empty, menu);
		return true;
	}

	/**
	 * Move to Activity presenting the results of min-entropy source utilizing
	 * the camera as a data source.
	 * 
	 * @param view
	 */
	public void moveToMinEntropySourceAppActivity(View view) {
		Intent moving = new Intent(this, MinEntropySourceActivity.class);
		startActivity(moving);
	}
	
	/**
	 * Move to Activity presenting the results taken from randomness extractor
	 * based on Carter-Wegman Universal hash functions. 
	 * 
	 * @param view
	 */
	public void moveToRandExtractorAppActivity(View view) {
		Intent moving = new Intent(this, RandExtractorActivity.class);
		startActivity(moving);
	}
	
	/**
	 * Move towards group key agreement protocol.
	 * 
	 * @param view
	 */
	public void moveToGKATechDecision(View view) {
		Intent moving = new Intent(this, GKADecisionActivity.class);
		startActivity(moving);
	}
	
	/**
	 * Move to Activity that enables to generate a fresh key-pair for
	 * public key cryptography.
	 * 
	 * @param view
	 */
	public void moveToPublicKeyCryptographyAppActivity(View view) {
		Intent moving = new Intent(this, LongTermKeyActivity.class);
		startActivity(moving);
	}
}
