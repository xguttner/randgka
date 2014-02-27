package cz.muni.fi.randgka.randgkaapp;

import cz.muni.fi.randgkaapp.R;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void moveToMinEntropySourceAppActivity(View view) {
		Intent moving = new Intent(this, MinEntropySourceAppActivity.class);
		startActivity(moving);
	}
	
	public void moveToRandExtractorAppActivity(View view) {
		Intent moving = new Intent(this, RandExtractorAppActivity.class);
		startActivity(moving);
	}
	
	public void moveToRandExtractor2AppActivity(View view) {
		Intent moving = new Intent(this, RandExtractor2AppActivity.class);
		startActivity(moving);
	}
	
	public void moveToGKAProtocolAppActivity(View view) {
		Intent moving = new Intent(this, GKAProtocolConnectionAppActivity.class);
		startActivity(moving);
	}
	
	public void moveToPublicKeyCryptographyAppActivity(View view) {
		Intent moving = new Intent(this, PublicKeyCryptographyAppActivity.class);
		startActivity(moving);
	}
}
