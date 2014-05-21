package cz.muni.fi.randgka.randgkaapp;

import java.net.InetAddress;
import java.util.Set;

import cz.muni.fi.randgka.bluetoothgka.BluetoothCommunicationService;
import cz.muni.fi.randgka.tools.BluetoothDeviceToDisplay;
import cz.muni.fi.randgka.tools.Constants;
import cz.muni.fi.randgka.wifigka.WifiCommunicationService;
import cz.muni.fi.randgkaapp.R;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Build;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class GKAMemberActivity extends Activity {

	private ArrayAdapter<BluetoothDeviceToDisplay> devices;
	private Spinner devicesSpinner;
	private boolean discoveryRunning;
	private BluetoothAdapter bluetoothAdapter;
	private BroadcastReceiver discoveryBR, br;
	private WifiP2pManager wifiP2pManager;
	private Channel wifiChannel;
	
	private boolean freshKey,
		wifiEnabled;
	private String technology;
	
	private Context context;
	
	private PeerListListener peerListListener;
	private ConnectionInfoListener connectionInfoListener;
	
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gkamember);
		
		devicesSpinner = (Spinner)findViewById(R.id.spinner1);
		
		freshKey = getIntent().getBooleanExtra("freshKey", false);
		technology = getIntent().getStringExtra("technology");
		if (technology.equals(Constants.WIFI_GKA)) {
			context = this;
			IntentFilter intentFilter = new IntentFilter();
		    intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		    intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		    intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		    
		    wifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
		    wifiChannel = wifiP2pManager.initialize(this, getMainLooper(), null);
		    
		    peerListListener = new PeerListListener() {
		        @Override
		        public void onPeersAvailable(WifiP2pDeviceList peerList) {
		        	if (devicesSpinner != null) {
		            	ArrayAdapter<WifiP2pDevice> wifiDevices = new ArrayAdapter<WifiP2pDevice>(context, android.R.layout.simple_spinner_item);
	            		wifiDevices.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		            	devicesSpinner.setAdapter(wifiDevices);
		            }
		        }
		    };
		    
		    connectionInfoListener = new ConnectionInfoListener() {
				
				@Override
				public void onConnectionInfoAvailable(WifiP2pInfo info) {
					// InetAddress from WifiP2pInfo struct.
			        InetAddress groupOwnerAddress = info.groupOwnerAddress;

			        // After the group negotiation, we can determine the group owner.
			        if (info.groupFormed && info.isGroupOwner) {
			            // Do whatever tasks are specific to the group owner.
			            // One common case is creating a server thread and accepting
			            // incoming connections.
			        } else if (info.groupFormed) {
			            // The other device acts as the client. In this case,
			            // you'll want to create a client thread that connects to the group
			            // owner.
			        }
				}
			};
		    
		    br = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					String action = intent.getAction();
					if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
						Log.d("is", "enabled");
			            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
			            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
			                wifiEnabled = true;
			            }
			        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
			        	Log.d("is", "enabled");
			        	if (wifiP2pManager != null) {
			        		wifiP2pManager.requestPeers(wifiChannel, peerListListener);
			            }
			        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
			        	NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

			            if (networkInfo.isConnected()) {

			                // We are connected with the other device, request connection
			                // info to find group owner IP

			                wifiP2pManager.requestConnectionInfo(wifiChannel, connectionInfoListener);
			            }
			        }
				}
			};
		    registerReceiver(br, intentFilter);
		    
		    wifiP2pManager.discoverPeers(wifiChannel, new WifiP2pManager.ActionListener() {
				
				@Override
				public void onSuccess() {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void onFailure(int arg0) {
					// TODO Auto-generated method stub
					Log.d("failure","on");
				}
			});
		} else if (technology.equals(Constants.BLUETOOTH_GKA)) {
			bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			enableBluetooth();
			
			IntentFilter deviceFound = new IntentFilter(BluetoothDevice.ACTION_FOUND);
			discoveryBR = new BroadcastReceiver() {
				
				@Override
				public void onReceive(Context context, Intent intent) {
					String action = intent.getAction();
			        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
			            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			            if (devicesSpinner != null) {
			            	if (devices == null) {
			            		devices = new ArrayAdapter<BluetoothDeviceToDisplay>(context, android.R.layout.simple_spinner_item);
			            		devices.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			            	}
			            	if (devices.getPosition(new BluetoothDeviceToDisplay(device)) == -1) devices.add(new BluetoothDeviceToDisplay(device));
			            	devicesSpinner.setAdapter(devices);
			            }
			        }
				}
			};
			registerReceiver(discoveryBR, deviceFound);
			
			devices = new ArrayAdapter<BluetoothDeviceToDisplay>(this, android.R.layout.simple_spinner_item);
			devices.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
			for (BluetoothDevice device : pairedDevices) {
		        devices.add(new BluetoothDeviceToDisplay(device));
		    }
		    devicesSpinner.setAdapter(devices);
			
			discoveryRunning = true;
			bluetoothAdapter.startDiscovery();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.empty, menu);
		return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// for enabling bluetooth - send to MainActivity if fail
		if (requestCode == Constants.REQUEST_ENABLE_BT) {
			if (resultCode == RESULT_CANCELED) {
				Intent moving = new Intent(this, MainActivity.class);
				startActivity(moving);
			}
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		 
		if (discoveryRunning) {
			discoveryRunning = false;
			bluetoothAdapter.cancelDiscovery();
		}
		unregisterReceiver(discoveryBR);
	}
	
	/**
	 * Check, if the bluetooth device is accessible and send request to enable it.
	 */
	private void enableBluetooth() {
		if (bluetoothAdapter == null) {
			Intent moving = new Intent(this, MainActivity.class);
			startActivity(moving);
		}
		if (!bluetoothAdapter.isEnabled()) {
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
		}
	}
	
	public void connectTo(View view) {
		Intent commServiceIntent = null;
		if (technology.equals(Constants.WIFI_GKA)) {
			WifiP2pDevice targetDevice = (WifiP2pDevice)devicesSpinner.getSelectedItem();
			
			WifiP2pConfig config = new WifiP2pConfig();
	        config.deviceAddress = targetDevice.deviceAddress;
	        config.wps.setup = WpsInfo.PBC;
	        config.groupOwnerIntent = 0;

	        wifiP2pManager.connect(wifiChannel, config, new ActionListener() {

	            @Override
	            public void onSuccess() {
	                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
	            }

	            @Override
	            public void onFailure(int reason) {
	                
	            }
	        });
			
			commServiceIntent = new Intent(this, WifiCommunicationService.class);
			commServiceIntent.setAction(WifiCommunicationService.MEMBER_RUN);
			commServiceIntent.putExtra("wifiDevice", targetDevice);
		} else if (technology.equals(Constants.BLUETOOTH_GKA)) {
			BluetoothDevice bluetoothDevice = ((BluetoothDeviceToDisplay)devicesSpinner.getSelectedItem()).getBluetoothDevice();
			
			// stop discovery
			if (discoveryRunning) {
				discoveryRunning = false;
				bluetoothAdapter.cancelDiscovery();
			}
			
			// connect
			commServiceIntent = new Intent(this, BluetoothCommunicationService.class);
			commServiceIntent.setAction(BluetoothCommunicationService.MEMBER_RUN);
			commServiceIntent.putExtra("bluetoothDevice", bluetoothDevice);
		}
		
		commServiceIntent.putExtra("freshKey", freshKey);
		commServiceIntent.putExtra(Constants.RETRIEVE_KEY, getIntent().getBooleanExtra(Constants.RETRIEVE_KEY, false));
		startService(commServiceIntent);
		
		Intent moving = new Intent(this, GKAActivity.class);
		moving.putExtra("technology", technology);
		startActivity(moving);
	}
 
}
