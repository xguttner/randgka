package cz.muni.fi.randgka.randgkaapp;

import java.io.IOException;

import cz.muni.fi.randgka.library.Constants;
import cz.muni.fi.randgka.randgkamiddle.RandExtractorActivity;
import cz.muni.fi.randgka.tools.ByteSequence;
import cz.muni.fi.randgkaapp.R;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

public class RandExtractorAppActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rand_extractor);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == Constants.RANDOM_DATA_RC) {
			if (data != null && data.getSerializableExtra("randomData") != null) {
				try {
					ByteSequence randomSequence = (ByteSequence)data.getSerializableExtra("randomData");
					
					String strSeq = (randomSequence != null)?(new String(randomSequence.getSequence(), "UTF-8")):"";
					
					String stringSeq = (strSeq.length()>0)?("Returned data "+randomSequence.toString()):"Saved into file.";
					
					EditText outputSeq = (EditText) findViewById(R.id.editText1);
					outputSeq.setText(stringSeq.toCharArray(), 0, stringSeq.length());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.rand_extractor, menu);
		return true;
	}
	
	public void getRandomData(View view) {
		EditText randDataLengthInput = (EditText)findViewById(R.id.randDataLength);
		Integer randDataLength = Integer.parseInt(randDataLengthInput.getText().toString());
		
		Intent getRandomData = new Intent(this, RandExtractorActivity.class);
		getRandomData.setAction(Constants.GET_RANDOM_DATA);
		getRandomData.putExtra("randDataLength", randDataLength);
		
		startActivityForResult(getRandomData, Constants.RANDOM_DATA_RC);
	}

}
