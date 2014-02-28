package cz.muni.fi.randgka.randgkamiddle;

import java.io.File;

import cz.muni.fi.randgka.library.ByteSequence;
import cz.muni.fi.randgka.library.MinEntropySourceType;
import cz.muni.fi.randgka.provider.minentropy.CameraMES;
import cz.muni.fi.randgka.provider.minentropy.MinEntropySource;
import cz.muni.fi.randgkaapp.R;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class MinEntropySourceActivity extends Activity {

	private ByteSequence byteSequence;
	private MinEntropySource source;
	private File targetFile;
	private int minEntropyDataLength;
	private boolean preprocess;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_min_entropy_source);
        
        SurfaceView surface = null;
    	//MinEntropySource source = null;
    	//File targetFile = null;
        
        /*surface=(SurfaceView)findViewById(R.id.surfaceView);
        SurfaceHolder holder=surface.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        source=new CameraMES(surface);*/
        
        byteSequence = null;
        
        MinEntropySourceType minEntropySourceType = (MinEntropySourceType)getIntent().getSerializableExtra("minEntropySourceType");
		String targetFileName = getIntent().getStringExtra("targetFileName");
		preprocess = getIntent().getBooleanExtra("preprocess", false);
		minEntropyDataLength = getIntent().getIntExtra("minEntropyDataLength", 1);
		
		switch (minEntropySourceType) {
		/*case MICROPHONE:
			source = new MicrophoneRS();
			sampleLength = 1024;
			break;*/
		case CAMERA:
		default:
			surface=(SurfaceView)findViewById(R.id.surfaceView);
	        SurfaceHolder holder=surface.getHolder();
	        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	        
			source = new CameraMES();
			break;
		}
		 
		if (targetFileName != null && targetFileName.length()>0) {
			Log.d("fn", targetFileName);
			targetFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), targetFileName);
		}
    }

    public void goBack(View view) {
	    if (preprocess) {
			byteSequence = source.getPreprocessedSourceData(minEntropyDataLength, targetFile);
		} else {
			byteSequence = source.getRawSourceData(minEntropyDataLength, targetFile);
		}
		
    	finishWithResult();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.min_entropy_source, menu);
        return true;
    }
    
    private void finishWithResult() {
    	Intent returnMinEntropyData = new Intent();
    	returnMinEntropyData.putExtra("minEntropyData", byteSequence);
    	setResult(RESULT_OK, returnMinEntropyData);
    	finish();
    }
    
    /*
    public void getSourceData(View view) {
		RadioGroup sourceTypeGroup = (RadioGroup) findViewById(R.id.radioGroup1);
		RadioGroup sourceDataTypeGroup = (RadioGroup) findViewById(R.id.radioGroup2);
		RadioGroup sourceOutputMethodGroup = (RadioGroup) findViewById(R.id.radioGroup3);
		EditText sampleNumber = (EditText) findViewById(R.id.editText1);
		
		source = null;
		sequence = null;
		
		GainMode gm = GainMode.SAFE;
		
		switch (sourceTypeGroup.getCheckedRadioButtonId()) {
			case R.id.radio1_1:
				source = new MicrophoneRS();
				sampleLength = 1024;
				break;
			case R.id.radio1_0:
			default:
				source = cameraRS;
				break;
		}
		
		switch (sourceOutputMethodGroup.getCheckedRadioButtonId()) {
			case R.id.radio3_1:
				file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "rawAREData");
				break;
			case R.id.radio3_0:
			default:
				file = null;
				break;
		}
		
		switch (sourceDataTypeGroup.getCheckedRadioButtonId()) {
			case R.id.radio2_1:
				sequence = source.getRawSourceData(Integer.parseInt(sampleNumber.getText().toString()), file);
				break;
			case R.id.radio2_0:
			default:
				sequence = source.getPreprocessedSourceData(Integer.parseInt(sampleNumber.getText().toString()), file);
				break;
		}
		String strSeq;
		try {
			strSeq = (sequence != null)?(new String(sequence.getSequence(), "UTF-8")):"";
			
			String stringSeq = (strSeq.length()>0)?("Returned data "+sequence.toString()):"Saved into file.";
			
			EditText outputSeq = (EditText) findViewById(R.id.editText2);
			outputSeq.setText(stringSeq.toCharArray(), 0, stringSeq.length());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/
}
