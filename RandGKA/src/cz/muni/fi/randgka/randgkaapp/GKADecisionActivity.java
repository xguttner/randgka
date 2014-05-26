package cz.muni.fi.randgka.randgkaapp;

import cz.muni.fi.randgka.bluetoothgka.BluetoothCommunicationService;
import cz.muni.fi.randgka.tools.Constants;
import cz.muni.fi.randgka.wifigka.WifiCommunicationService;
import cz.muni.fi.randgkaapp.R;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * This class provides a simple decision point about the technology used for 
 * a group key agreement protocol instance and the underlying topology. 
 * As the implemented protocol use a star topology, the user is provided with 
 * options to act either as a group leader, or a member. If the Bluetooth 
 * channel is chosen, the application tries to establish the connection by 
 * itself. Moreover, each member is asked explicitly about the device it 
 * wants to connect to. If the Wi-Fi channel is chosen, the application only 
 * checks the presence of the needed Wi-Fi connection with the appropriate 
 * topology (a protocol leader has to provide an access point to members). 
 * As seen from the application's scheme, this activity also serves as a gate 
 * for other applications wanting to get a shared key via Intent with 
 * the action set to cz.muni.randgka.GET_GKA_KEY.
 */
public class GKADecisionActivity extends Activity {
	
	private static final String LEADER = "Leader",
			MEMBER = "Member";
	
	private static final int REQUEST_ENABLE_BT = 1785;
	private static final int REQUEST_DISCOVERABLE_BT = 1786;
	
	// true - result key retrieval by another app, false otherwise
	private boolean retrieveKey = false;
	
	private Spinner technologySpinner,
		roleSpinner,
		entropySourceSpinner;
	
	private Intent moving;
	
	private String role,
		entropySource;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gka_decision);
		
		// obtain the main intent - display or retrieve the resulting key
		if (getIntent().getAction() != null) retrieveKey = getIntent().getAction().equals(Constants.ACTION_GKA_KEY);
		// prevent the Intent-using attack
		if (retrieveKey) {
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
		
		ArrayAdapter<String> entropySources = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
		entropySources.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		entropySources.add(Constants.NATIVE_ES);
		entropySources.add(Constants.RAND_EXT_ES);
		entropySourceSpinner = (Spinner)findViewById(R.id.spinner3);
		entropySourceSpinner.setAdapter(entropySources);
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
		
		// Bluetooth enabling result
		if (requestCode == REQUEST_ENABLE_BT) {
			// Bluetooth not enabled - send to MainActivity
			if (resultCode == RESULT_CANCELED) {
				Intent backToMain = new Intent(this, MainActivity.class);
				startActivity(backToMain);
			}
			// Bluetooth enabled, act according to the role
			else if (resultCode == RESULT_OK) {
				// leader: ask for discoverability, start communication service
				if (role.equals(LEADER)) {
					leaderRun();
				}
				// member: move towards the leader device choice
				else {
					if (retrieveKey) startActivityForResult(moving, Constants.REQUEST_RETRIEVE_GKA_KEY);
					else startActivity(moving);
				}
			}
		}
		// wait for the leader to decide about discoverability and move to the GKAActivity
		else if (requestCode == REQUEST_DISCOVERABLE_BT) {
			if (retrieveKey) startActivityForResult(moving, Constants.REQUEST_RETRIEVE_GKA_KEY);
			else startActivity(moving);
		} else if (requestCode == Constants.REQUEST_RETRIEVE_GKA_KEY) {
			setResult(Activity.RESULT_OK, data);
			finish();
		}
	}
	
	/**
	 * Function process the next step, according to the chosen technology and role.
	 * 
	 * @param view
	 */
	public void moveToGKARole(View view) {
		// get the parameters
		String technology = (String) technologySpinner.getSelectedItem();
		role = (String) roleSpinner.getSelectedItem();
		entropySource = (String)entropySourceSpinner.getSelectedItem();
		
		// setup new intent
		moving = new Intent();
		moving.putExtra(Constants.TECHNOLOGY, technology);
		moving.putExtra(Constants.IS_LEADER, role.equals(LEADER));
		moving.putExtra(Constants.ENTROPY_SOURCE, entropySource);
		if (retrieveKey) moving.putExtra(Constants.RETRIEVE_KEY, true);
		
		// Bluetooth chosen
		if (technology.equals(Constants.BLUETOOTH_GKA)) {
			// enable bluetooth
			boolean enabled = enableBluetooth();
			
			// leader run
			if (role.equals(LEADER)) {
				moving.setClass(this, GKAActivity.class);
				if (enabled) leaderRun();
			}
			// member run
			else {
				moving.setClass(this, GKAMemberActivity.class);
				if (enabled) {
					if (retrieveKey) startActivityForResult(moving, Constants.REQUEST_RETRIEVE_GKA_KEY);
					else startActivity(moving);
				}
			}
		}
		// Wi-Fi chosen
		else if (technology.equals(Constants.WIFI_GKA)) {
			TextView tv = (TextView) findViewById(R.id.textView4);
			// leader run
			if (role.equals(LEADER)) {
				if (WifiCommunicationService.isWifiConnected(true)) wifiRun(Constants.LEADER_RUN);
				// network not in a proper state
				else tv.setText(R.string.wifi_leader_connection_warning);
			}
			else {
				if (WifiCommunicationService.isWifiConnected(false)) wifiRun(Constants.MEMBER_RUN);
				// network not in a proper state
				else tv.setText(R.string.wifi_connection_warning);
			}
		}
	}
	
	/**
	 * Ask for discoverability and start the leader run.
	 */
	private void leaderRun() {
		startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE), REQUEST_DISCOVERABLE_BT);
		
		Intent commServiceIntent = new Intent(this, BluetoothCommunicationService.class);
		commServiceIntent.setAction(Constants.LEADER_RUN);
		commServiceIntent.putExtra(Constants.ENTROPY_SOURCE, entropySource);
		commServiceIntent.putExtra(Constants.RETRIEVE_KEY, retrieveKey);
		startService(commServiceIntent);
	}
	
	/**
	 * New WifiCommunicationService intent with apropriate action, followed by move towards the GKAActivity.
	 * 
	 * @param action - Constants.LEADER_RUN or Constants.MEMBER_RUN
	 */
	private void wifiRun(String action) {
		Intent commServiceIntent = new Intent(this, WifiCommunicationService.class);
		commServiceIntent.setAction(action);
		commServiceIntent.putExtra(Constants.RETRIEVE_KEY, retrieveKey);
		commServiceIntent.putExtra(Constants.ENTROPY_SOURCE, entropySource);
		startService(commServiceIntent);
		
		moving.setClass(this, GKAActivity.class);
		if (retrieveKey) startActivityForResult(moving, Constants.REQUEST_RETRIEVE_GKA_KEY);
		else startActivity(moving);
	}
	
	/**
	 * @return true, if bluetooth is enabled, false otherwise
	 */
	private boolean enableBluetooth() {
		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		// bluetooth adapter unreachable - move to the MainActivity
		if (bluetoothAdapter == null) {
			Intent moving = new Intent(this, MainActivity.class);
			startActivity(moving);
		}
		if (!bluetoothAdapter.isEnabled()) {
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		} else return true;
		
		return false;
	}
}

	