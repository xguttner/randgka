package cz.muni.fi.randgka.randgkamiddle;

import java.io.DataInputStream;
import java.io.IOException;

import cz.muni.fi.randgka.library.ByteSequence;
import cz.muni.fi.randgka.library.Constants;
import cz.muni.fi.randgka.library.LengthsNotEqualException;
import cz.muni.fi.randgka.library.MinEntropySourceType;
import cz.muni.fi.randgka.library.RandExtractor;
import cz.muni.fi.randgka.library.UniversalHashFuncRE;
import cz.muni.fi.randgkaapp.R;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;

public class RandExtractorActivity extends Activity {
	
	private RandExtractor randExtractor;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_random_extractor);
		
		randExtractor = new UniversalHashFuncRE();
		
		Intent getMinEntropyData = new Intent(this, MinEntropySourceActivity.class);
		getMinEntropyData.setAction(Constants.GET_MINENTROPY_DATA);
		getMinEntropyData.putExtra("preprocess", true);
		getMinEntropyData.putExtra("targetFileName", "");
		getMinEntropyData.putExtra("minEntropySourceType", MinEntropySourceType.CAMERA);
		getMinEntropyData.putExtra("minEntropyDataLength", randExtractor.minEntropyNeededForLength(getIntent().getIntExtra("randDataLength", 1)));
		
		startActivityForResult(getMinEntropyData, Constants.MINENTROPY_DATA_RC);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.rand_extractor, menu);
		return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == Constants.MINENTROPY_DATA_RC) {
			if (data != null && data.getSerializableExtra("minEntropyData") != null) {
					ByteSequence minEntropyData = (ByteSequence)data.getSerializableExtra("minEntropyData");
					getRandomData(minEntropyData);
			}
		}
	}
	
	private ByteSequence getSeed() {
		DataInputStream seedStream = new DataInputStream(getResources().openRawResource(R.raw.seed));
		byte[] seedData = new byte[randExtractor.getSeedLength()];
		try {
			for (int i = 0; i < randExtractor.getSeedLength(); i++) {
				seedData[i] = (byte)(0xff & seedStream.read());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ByteSequence(seedData, randExtractor.getSeedLength());
	}
	
	private void getRandomData(ByteSequence minEntropyData) {
		ByteSequence randomData = null;
		try {
			randomData = randExtractor.extractRandomness(getSeed(), minEntropyData);
		} catch (LengthsNotEqualException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finishWithResult(randomData);
	}
	
	private void finishWithResult(ByteSequence randomData) {
		Intent returnRandomData = new Intent();
    	returnRandomData.putExtra("randomData", randomData);
    	setResult(RESULT_OK, returnRandomData);
    	finish();
	}

}
