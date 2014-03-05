package cz.muni.fi.randgka.randgkaapp;

import java.io.IOException;

import cz.muni.fi.randgka.library.Constants;
import cz.muni.fi.randgka.library.MinEntropySourceType;
import cz.muni.fi.randgka.provider.minentropy.CameraMES;
import cz.muni.fi.randgka.randgkamiddle.MinEntropySourceActivity;
import cz.muni.fi.randgka.tools.ByteSequence;
import cz.muni.fi.randgkaapp.R;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;

public class MinEntropySourceAppActivity extends Activity {
	private CameraMES cameraRS;
	private SurfaceView surface;
	//private MinEntropySource source;
	//private ByteSequence sequence;
	//private File file;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rand_source);
		surface=(SurfaceView)findViewById(R.id.surfaceView);
        SurfaceHolder holder=surface.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.rand_source, menu);
		return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == Constants.MINENTROPY_DATA_RC) {
			if (data != null && data.getSerializableExtra("minEntropyData") != null) {
				try {
					ByteSequence minEntropySequence = (ByteSequence)data.getSerializableExtra("minEntropyData");
					
					String strSeq = (minEntropySequence != null)?(new String(minEntropySequence.getSequence(), "UTF-8")):"";
					
					String stringSeq = (strSeq.length()>0)?("Returned data "+minEntropySequence.toString()):"Saved into file.";
					
					EditText outputSeq = (EditText) findViewById(R.id.editText2);
					outputSeq.setText(stringSeq.toCharArray(), 0, stringSeq.length());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	public void getSourceData(View view) {
		RadioGroup sourceTypeGroup = (RadioGroup) findViewById(R.id.radioGroup1);
		RadioGroup sourceDataTypeGroup = (RadioGroup) findViewById(R.id.radioGroup2);
		RadioGroup sourceOutputMethodGroup = (RadioGroup) findViewById(R.id.radioGroup3);
		EditText minEntropyDataLength = (EditText) findViewById(R.id.editText1);
		
		//source = null;
		//sequence = null;
		
		//GainMode gm = GainMode.SAFE;
		
		MinEntropySourceType minEntropySourceType = null;
		String targetFileName = null;
		boolean preprocess = false;
		
		switch (sourceTypeGroup.getCheckedRadioButtonId()) {
			/*case R.id.radio1_1:
				source = new MicrophoneRS();
				sampleLength = 1024;
				break;*/
			case R.id.radio1_0:
			default:
				minEntropySourceType = MinEntropySourceType.CAMERA;
				//source = cameraRS;
				break;
		}
		
		switch (sourceOutputMethodGroup.getCheckedRadioButtonId()) {
			case R.id.radio3_1:
				//file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "rawAREData");
				targetFileName = "minEntropyData";
				break;
			case R.id.radio3_0:
			default:
				//file = null;
				targetFileName = null;
				break;
		}
		
		switch (sourceDataTypeGroup.getCheckedRadioButtonId()) {
			case R.id.radio2_1:
				//sequence = source.getRawSourceData(Integer.parseInt(sampleNumber.getText().toString()), file);
				preprocess = false;
				break;
			case R.id.radio2_0:
			default:
				//sequence = source.getPreprocessedSourceData(Integer.parseInt(sampleNumber.getText().toString()), file);
				preprocess = true;
				break;
		}
		
		Intent getMinEntropyData = new Intent(this, MinEntropySourceActivity.class);
		getMinEntropyData.setAction(Constants.GET_MINENTROPY_DATA);
		getMinEntropyData.putExtra("preprocess", preprocess);
		getMinEntropyData.putExtra("targetFileName", targetFileName);
		getMinEntropyData.putExtra("minEntropySourceType", minEntropySourceType);
		getMinEntropyData.putExtra("minEntropyDataLength", Integer.parseInt(minEntropyDataLength.getText().toString()));
		
		//startActivityForResult(getMinEntropyData, Constants.MINENTROPY_DATA_RC);
		
		/*String strSeq;
		try {
			strSeq = (sequence != null)?(new String(sequence.getSequence(), "UTF-8")):"";
			
			String stringSeq = (strSeq.length()>0)?("Returned data "+sequence.toString()):"Saved into file.";
			
			EditText outputSeq = (EditText) findViewById(R.id.editText2);
			outputSeq.setText(stringSeq.toCharArray(), 0, stringSeq.length());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}

}
