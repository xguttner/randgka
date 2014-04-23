package cz.muni.fi.randgka.randgkaapp;

import cz.muni.fi.randgkaapp.R;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;

public class BluetoothGKARoleActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bluetooth_gkarole);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.bluetooth_gkarole, menu);
		return true;
	}
	
	public void moveToBluetoothGKAMember(View view) {
		Intent moving = new Intent(this, BluetoothGKAMemberActivity.class);
		startActivity(moving);
	}

	public void moveToBluetoothGKALeader(View view) {
		Intent moving = new Intent(this, BluetoothGKALeaderActivity.class);
		startActivity(moving);
	}
}
