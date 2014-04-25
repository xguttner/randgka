package cz.muni.fi.randgka.randgkaapp;

import cz.muni.fi.randgka.bluetoothgka.BluetoothCommunicationService;
import cz.muni.fi.randgka.provider.minentropy.CameraMES;
import cz.muni.fi.randgka.provider.minentropy.CameraMESHolder;
import cz.muni.fi.randgka.tools.Constants;
import cz.muni.fi.randgkaapp.R;
import android.os.Bundle;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class BluetoothGKAActivity extends Activity {

	private ProtocolBroadcastReceiver receiver;
	private boolean isLeader = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gkaprotocol);
		
		TextView protocolTV = (TextView)findViewById(R.id.textView3);
		TextView versionTV = (TextView)findViewById(R.id.textView15);
		TextView nonceLengthTV = (TextView)findViewById(R.id.textView6);
		TextView groupKeyLengthTV = (TextView)findViewById(R.id.textView8);
		TextView publicKeyLengthTV = (TextView)findViewById(R.id.textView10);
		TextView protocolParticipantsTV = (TextView)findViewById(R.id.textView13);
		TextView gkaTV = (TextView)findViewById(R.id.textView12);
		
		IntentFilter protocolFilter = new IntentFilter();
		protocolFilter.addAction(Constants.GET_PARTICIPANTS);
		protocolFilter.addAction(Constants.GET_GKA_KEY);
		protocolFilter.addAction(Constants.GET_PARAMS);
		receiver = new ProtocolBroadcastReceiver();
		receiver.setTextViews(protocolParticipantsTV, gkaTV, protocolTV, versionTV, nonceLengthTV, groupKeyLengthTV, publicKeyLengthTV);
		LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
		lbm.registerReceiver(receiver, protocolFilter);
		
		IntentFilter returnKeyFilter = new IntentFilter();
		returnKeyFilter.addAction(Constants.RETURN_GKA_KEY);
		BroadcastReceiver rkReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				Log.d("try", "to return");
				Intent result = new Intent();       
				result.putExtra("key", intent.getByteArrayExtra("key"));
				BluetoothGKAActivity.this.setResult(Activity.RESULT_OK, result);
				finish();
			}
		};
		lbm.registerReceiver(rkReceiver, returnKeyFilter);
		
		if (this.getIntent().getBooleanExtra("isLeader", false)) {
			isLeader = true;
			
			Intent paramsIntent = new Intent(this, BluetoothCommunicationService.class);
			paramsIntent.setAction(Constants.GET_PARAMS);
			startService(paramsIntent);
		}
		
		Button runButton = (Button)findViewById(R.id.button1);
		runButton.setClickable(isLeader);
		
		SurfaceView surface = null;
		
		surface=(SurfaceView)findViewById(R.id.surfaceView1);
        SurfaceHolder holder=surface.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        CameraMES cameraMES = new CameraMES();
        cameraMES.initialize(surface);
        CameraMESHolder.cameraMES = cameraMES;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.empty, menu);
		return true;
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (CameraMESHolder.cameraMES != null) {
			CameraMESHolder.cameraMES.stop();
			CameraMESHolder.cameraMES = null;
		}
	}

	public void runProtocol(View view) {
		Intent runProtocolIntent = new Intent(this, BluetoothCommunicationService.class);
		runProtocolIntent.setAction(Constants.RUN_GKA_PROTOCOL);
		startService(runProtocolIntent);
	}
}
