package cz.muni.fi.randgka.randgkaapp;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

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
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

public class GKADecisionActivity extends Activity {
	
	private static final String LEADER = "Leader";
	private static final String MEMBER = "Member";
	private boolean returnKey;
	
	private Spinner technologySpinner,
		roleSpinner;
	
	private Intent moving;
	
	private String role;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gka_decision);
		
		returnKey = getIntent().getBooleanExtra(Constants.RETRIEVE_KEY, false);
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
		CheckBox freshKeyBox = (CheckBox)findViewById(R.id.checkBox1);
		boolean freshKey = freshKeyBox.isChecked();
		String technology = (String) technologySpinner.getSelectedItem();
		role = (String) roleSpinner.getSelectedItem();
		
		moving = new Intent();
		moving.putExtra("technology", technology);
		moving.putExtra("freshKey", freshKey);
		moving.putExtra("isLeader", role.equals(LEADER));
		if (returnKey) {
			moving.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
			moving.putExtra(Constants.RETRIEVE_KEY, true);
		}
		
		if (technology.equals(Constants.BLUETOOTH_GKA)) {
			boolean enabled = enableBluetooth();
			
			if (role.equals(LEADER)) {
				moving.setClass(this, GKAActivity.class);
				if (enabled) {
					startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE), Constants.REQUEST_DISCOVERABLE_BT);
					
					Intent commServiceIntent = new Intent(this, BluetoothCommunicationService.class);
					commServiceIntent.setAction(BluetoothCommunicationService.LEADER_RUN);
					startService(commServiceIntent);
				}
			}
			else {
				moving.setClass(this, GKAMemberActivity.class);
				if (enabled) startActivity(moving);
			}
		}
		else if (technology.equals(Constants.WIFI_GKA)) {
			TextView tv = (TextView) findViewById(R.id.textView4);
			if (role.equals(LEADER)) {
				if (isWifiConnected(true)) {
					Intent commServiceIntent = new Intent(this, WifiCommunicationService.class);
					commServiceIntent.setAction(WifiCommunicationService.LEADER_RUN);
					startService(commServiceIntent);
					
					moving.setClass(this, GKAActivity.class);
					startActivity(moving);
				}
				else tv.setText(R.string.wifi_leader_connection_warning);
			}
			else {
				if (isWifiConnected(false)) {
					Intent commServiceIntent = new Intent(this, WifiCommunicationService.class);
					commServiceIntent.setAction(WifiCommunicationService.MEMBER_RUN);
					commServiceIntent.putExtra("freshKey", freshKey);
					commServiceIntent.putExtra(Constants.RETRIEVE_KEY, getIntent().getBooleanExtra(Constants.RETRIEVE_KEY, false));
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
	
	private boolean isWifiConnected(boolean isLeader) {
		
		try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress.getAddress().length == 4) {
                       	if (isLeader && inetAddress.getAddress()[3] == (byte)1) return true;
                       	else if (!isLeader && inetAddress.getAddress()[3] != (byte)1) return true;
                       	else return false;
                    }
                }
            }
        } catch (SocketException ex) {
        }
        return false;
	}
}

	