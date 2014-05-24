package cz.muni.fi.randgka.randgkaapp;

import cz.muni.fi.randgka.bluetoothgka.BluetoothCommunicationService;
import cz.muni.fi.randgka.tools.Constants;
import cz.muni.fi.randgka.wifigka.WifiCommunicationService;
import cz.muni.fi.randgkaapp.R;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

public class GKADecisionActivity extends Activity {
	
	private static final String LEADER = "Leader";
	private static final String MEMBER = "Member";
	private boolean returnKey = false;
	
	private Spinner technologySpinner,
		roleSpinner;
	
	private Intent moving;
	
	private String role;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gka_decision);
		
		if (getIntent().getAction() != null) returnKey = getIntent().getAction().equals(Constants.ACTION_GKA_KEY);
		if (returnKey) {
			TextView externalIntentNotifierTV = (TextView) findViewById(R.id.textView3);
			externalIntentNotifierTV.setText("Called by an external application.");
		}
		
		ArrayAdapter<String> technologies = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
		technologies.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		technologies.add(Constants.BLUETOOTH_GKA);
		technologies.add(Constants.WIFI_GKA);
		technologySpinner = (Spinner)findViewById(R.id.spinner2);
		technologySpinner.setAdapter(technologies);
		
		ArrayAdapter<String> roles = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
		roles.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		roles.add(LEADER);
		roles.add(MEMBER);
		roleSpinner = (Spinner)findViewById(R.id.spinner1);
		roleSpinner.setAdapter(roles);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.empty, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return false;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// for enabling bluetooth - send to MainActivity if fail
		if (requestCode == Constants.REQUEST_ENABLE_BT) {
			if (resultCode == RESULT_CANCELED) {
				Intent backToMain = new Intent(this, MainActivity.class);
				startActivity(backToMain);
			}
			else if (resultCode == RESULT_OK) {
				if (role.equals(LEADER)) {
					startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE), Constants.REQUEST_DISCOVERABLE_BT);
					
					Intent commServiceIntent = new Intent(this, BluetoothCommunicationService.class);
					commServiceIntent.setAction(BluetoothCommunicationService.LEADER_RUN);
					commServiceIntent.putExtra(Constants.RETRIEVE_KEY, returnKey);
					startService(commServiceIntent);
				}
				else startActivity(moving);
			}
		}
		else if (requestCode == Constants.REQUEST_DISCOVERABLE_BT) {
			startActivity(moving);
		}
	}
	
	public void moveToGKARole(View view) {
		String technology = (String) technologySpinner.getSelectedItem();
		role = (String) roleSpinner.getSelectedItem();
		
		moving = new Intent();
		moving.putExtra("technology", technology);
		moving.putExtra("isLeader", role.equals(LEADER));
		if (returnKey) {
			moving.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
			moving.putExtra(Constants.RETRIEVE_KEY, true);
			Log.d("propagate", "one");
		}
		
		if (technology.equals(Constants.BLUETOOTH_GKA)) {
			boolean enabled = enableBluetooth();
			
			if (role.equals(LEADER)) {
				moving.setClass(this, GKAActivity.class);
				if (enabled) {
					startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE), Constants.REQUEST_DISCOVERABLE_BT);
					
					Intent commServiceIntent = new Intent(this, BluetoothCommunicationService.class);
					commServiceIntent.setAction(BluetoothCommunicationService.LEADER_RUN);
					commServiceIntent.putExtra(Constants.RETRIEVE_KEY, returnKey);
					startService(commServiceIntent);
				}
			}
			else {
				moving.setClass(this, GKAMemberActivity.class);
				if (enabled) {
					startActivity(moving);
				}
			}
		}
		else if (technology.equals(Constants.WIFI_GKA)) {
			TextView tv = (TextView) findViewById(R.id.textView4);
			if (role.equals(LEADER)) {
				if (WifiCommunicationService.isWifiConnected(true)) {
					Intent commServiceIntent = new Intent(this, WifiCommunicationService.class);
					commServiceIntent.setAction(WifiCommunicationService.LEADER_RUN);
					commServiceIntent.putExtra(Constants.RETRIEVE_KEY, returnKey);
					startService(commServiceIntent);
					
					moving.setClass(this, GKAActivity.class);
					startActivity(moving);
				}
				else tv.setText(R.string.wifi_leader_connection_warning);
			}
			else {
				if (WifiCommunicationService.isWifiConnected(false)) {
					Intent commServiceIntent = new Intent(this, WifiCommunicationService.class);
					commServiceIntent.setAction(WifiCommunicationService.MEMBER_RUN);
					commServiceIntent.putExtra(Constants.RETRIEVE_KEY, returnKey);
					startService(commServiceIntent);
					
					moving.setClass(this, GKAActivity.class);
					startActivity(moving);
				}
				else tv.setText(R.string.wifi_connection_warning);
			}
		}
	}
	
	private boolean enableBluetooth() {
		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter == null) {
			Intent moving = new Intent(this, MainActivity.class);
			startActivity(moving);
		}
		if (!bluetoothAdapter.isEnabled()) {
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
		} else return true;
		
		return false;
	}
}

	