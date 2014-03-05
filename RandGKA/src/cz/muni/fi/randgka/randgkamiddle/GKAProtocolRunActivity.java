package cz.muni.fi.randgka.randgkamiddle;

import cz.muni.fi.randgka.bluetoothgka.BluetoothCommunicationService;
import cz.muni.fi.randgka.library.Constants;
import cz.muni.fi.randgka.randgkamiddle.RandExtractorActivity;
import cz.muni.fi.randgka.tools.ByteSequence;
import cz.muni.fi.randgkaapp.R;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;

public class GKAProtocolRunActivity extends Activity {

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == Constants.RANDOM_DATA_RC) {
			if (data != null && data.getSerializableExtra("randomData") != null) {
					ByteSequence randomSequence = (ByteSequence)data.getSerializableExtra("randomData");
					
					Intent randomnessReceived = new Intent(this, BluetoothCommunicationService.class);
					randomnessReceived.setAction(Constants.PROTOCOL_RANDOMNESS);
					randomnessReceived.putExtra("randSequence", randomSequence);
					startService(randomnessReceived);
			}
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gkaprotocol_random_data);
		
		Intent getRandomData = new Intent(this, RandExtractorActivity.class);
		getRandomData.setAction(Constants.GET_RANDOM_DATA);
		getRandomData.putExtra("randDataLength", getIntent().getIntExtra("randDataLength", 128));
		
		startActivityForResult(getRandomData, Constants.RANDOM_DATA_RC);
		
		/*DataInputStream seedStream = new DataInputStream(getResources().openRawResource(R.raw.seed));
		byte[] seedData = new byte[839];
		try {
			for (int i = 0; i < 839; i++) {
				seedData[i] = (byte)(0xff & seedStream.read());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ByteSequence randomSequence = new ByteSequence(seedData, getIntent().getIntExtra("randDataLength", 128));
		
		Intent randomnessReceived = new Intent(this, ConnectionService.class);
		randomnessReceived.setAction(Constants.PROTOCOL_RANDOMNESS);
		randomnessReceived.putExtra("randSequence", randomSequence);
		startService(randomnessReceived);*/
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.gkaprotocol_random_data, menu);
		return true;
	}

}
