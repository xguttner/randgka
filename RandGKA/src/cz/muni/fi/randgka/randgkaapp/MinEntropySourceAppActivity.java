package cz.muni.fi.randgka.randgkaapp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;

import cz.muni.fi.randgka.provider.minentropy.CameraMES;
import cz.muni.fi.randgka.provider.minentropy.MinEntropySource;
import cz.muni.fi.randgka.tools.ByteSequence;
import cz.muni.fi.randgkaapp.R;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MinEntropySourceAppActivity extends Activity {
	
	private MinEntropySource mes;
	private TextView otv;
	private EditText ofet,
		olet;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_min_entropy_source_app);
        
		otv = (TextView) findViewById(R.id.textView2);
		ofet = (EditText) findViewById(R.id.editText1);
		olet = (EditText) findViewById(R.id.editText2);
        
		SurfaceView surface=(SurfaceView)findViewById(R.id.surfaceView);
		SurfaceHolder holder=surface.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
		CameraMES cameraMES = new CameraMES();
        cameraMES.initialize(surface);
        mes = cameraMES;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.empty, menu);
		return true;
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		mes.stop();
	}
	
	public void getSourceData(View view) {
		int outputLength = 0;
		try {
			outputLength = Integer.parseInt(olet.getText().toString());
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		String outFileName = ofet.getText().toString();
		
		if (outFileName.length() > 0) {
			String state = Environment.getExternalStorageState();
		    if (Environment.MEDIA_MOUNTED.equals(state)) {
		    	File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		    	File outFile = new File(dir, outFileName);
		        try {
		        	if ((dir.exists() || dir.mkdir()) && (outFile.exists() || outFile.createNewFile()) && outFile.canWrite()) {
		        		mes.getMinEntropyData(outputLength, outFile);
		        		String message = "Stored "+outputLength+"  bits into: "+outFile.getAbsolutePath();
		        		otv.setText(message.toCharArray(), 0, message.length());
		        	} else Log.e("min-entropy", "Storing into external storage failed.");
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
		    } else {
		    	Log.e("min-entropy", "Storing into external storage failed.");
		    }
		} else {
			ByteSequence minEntropySequence = mes.getMinEntropyData(outputLength, null);
			BigInteger minEntropyNum = new BigInteger(1, minEntropySequence.getSequence());
			minEntropyNum.shiftRight(8 - minEntropySequence.getBitLength()%8);
			otv.setText(minEntropyNum.toString(16).toCharArray(), 0, minEntropyNum.toString(16).toCharArray().length);
		}
		
	}

}
