package cz.muni.fi.randgka.randgkaapp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;

import cz.muni.fi.randgka.provider.minentropy.CameraMES;
import cz.muni.fi.randgka.provider.minentropy.MinEntropySource;
import cz.muni.fi.randgka.provider.minentropy.MinEntropySourceType;
import cz.muni.fi.randgka.tools.ByteSequence;
import cz.muni.fi.randgkaapp.R;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

public class MinEntropySourceAppActivity extends Activity {
	private MinEntropySource mes;
	private SharedPreferences sp;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_min_entropy_source_app);
        
		sp = PreferenceManager.getDefaultSharedPreferences(this);
		
		switch (MinEntropySourceType.valueOf(sp.getString("pref_mes_type", "CAMERA"))) {
		
		case MICROPHONE:
			break;
			
		case CAMERA:
		default:
			
			SurfaceView surface=(SurfaceView)findViewById(R.id.surfaceView);
			SurfaceHolder holder=surface.getHolder();
	        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	        
	        CameraMES cameraMES = new CameraMES();
	        cameraMES.initialize(surface);
	        mes = cameraMES;
			
			break;
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.empty, menu);
		return true;
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mes.stop();
	}
	
	public void getSourceData(View view) {
		TextView textView = (TextView) findViewById(R.id.textView2);
		
		int outputLength = Integer.parseInt(sp.getString("pref_mes_length", "128"));
		ByteSequence minEntropySequence = null;
		
		String outFileName = sp.getString("pref_mes_outfile", "");
		if (outFileName.length() > 0) {
			String state = Environment.getExternalStorageState();
		    if (Environment.MEDIA_MOUNTED.equals(state)) {
		    	File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		    	File outFile = new File(dir, outFileName);
		        try {
		        	if ((dir.exists() || dir.mkdir()) && (outFile.exists() || outFile.createNewFile()) && outFile.canWrite()) {
		        		mes.getMinEntropyData(outputLength, outFile);
		        		String message = "Stored "+outputLength+"  bits into: "+outFile.getAbsolutePath();
		        		textView.setText(message.toCharArray(), 0, message.length());
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
			minEntropySequence = mes.getMinEntropyData(outputLength, null);
			BigInteger minEntropyNum = new BigInteger(1, minEntropySequence.getSequence());
			minEntropyNum.shiftRight(8 - minEntropySequence.getBitLength()%8);
			textView.setText(minEntropyNum.toString(16).toCharArray(), 0, minEntropyNum.toString(16).toCharArray().length);
		}
		
	}

}
