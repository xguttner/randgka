package cz.muni.fi.randgka.randgkaapp;

import cz.muni.fi.randgkaapp.R;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

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

	public void moveToMinEntropySourceAppActivity(View view) {
		Intent moving = new Intent(this, MinEntropySourceAppActivity.class);
		startActivity(moving);
	}
	
	public void moveToRandExtractor2AppActivity(View view) {
		Intent moving = new Intent(this, RandExtractorAppActivity.class);
		startActivity(moving);
	}
	
	public void moveToGKATechDecision(View view) {
		Intent moving = new Intent(this, GKADecisionActivity.class);
		startActivity(moving);
	}
	
	public void moveToPublicKeyCryptographyAppActivity(View view) {
		Intent moving = new Intent(this, LongTermKeyAppActivity.class);
		startActivity(moving);
	}
}
