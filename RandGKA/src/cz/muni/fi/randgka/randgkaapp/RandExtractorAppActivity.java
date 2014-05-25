package cz.muni.fi.randgka.randgkaapp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;

import cz.muni.fi.randgka.provider.RandGKAProvider;
import cz.muni.fi.randgka.provider.minentropy.CameraMES;
import cz.muni.fi.randgka.provider.minentropy.CameraMESHolder;
import cz.muni.fi.randgka.provider.random.UHRandExtractorParams;
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

/**
 * Activity enabling access to the randomness extractor output.
 */
public class RandExtractorAppActivity extends Activity {
	
	private TextView otv;
	private EditText ofet,
		olet;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rand_extractor_app);

		// get view components
		otv = (TextView) findViewById(R.id.textView2);
		ofet = (EditText) findViewById(R.id.editText1);
		olet = (EditText) findViewById(R.id.editText2);
		
		SurfaceView surface=(SurfaceView)findViewById(R.id.surfaceView1);
		SurfaceHolder holder=surface.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        // invoke min-entropy source object
        CameraMES cameraMES = new CameraMES();
        cameraMES.initialize(surface);
        CameraMESHolder.cameraMES = cameraMES;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.empty, menu);
		return true;
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if (CameraMESHolder.cameraMES != null) {
			CameraMESHolder.cameraMES.stop();
			CameraMESHolder.cameraMES = null;
		}
	}
	
	/**
	 * Get data from the randomness extractor and process them in a wanted way.
	 * 
	 * @param view
	 */
	public void getData(View view) {
		// get output length
		int outputLength = 0;
		try {
			outputLength = Integer.parseInt(olet.getText().toString());
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		// get output file name
		String outFileName = ofet.getText().toString();
		
        try {
        	Provider pr = new RandGKAProvider();
			SecureRandom sr = SecureRandom.getInstance(RandGKAProvider.RAND_EXTRACTOR, pr);
			
			if (outputLength > 0) {
				
				// store the min-entropy sequence
				if (outFileName.length() > 0) {
					String state = Environment.getExternalStorageState();
				    if (Environment.MEDIA_MOUNTED.equals(state)) {
				    	File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
				    	File outFile = new File(dir, outFileName);
				        try {
				        	if ((dir.exists() || dir.mkdir()) && (outFile.exists() || outFile.createNewFile()) && outFile.canWrite()) {
				        		FileOutputStream fos = new FileOutputStream(outFile);
				        		int outputLengthLeft = outputLength;
				        		byte[] currentOutput = new byte[UHRandExtractorParams.MAXIMAL_OUTPUT/8];
				        		// round using maximal output length
				        		while (UHRandExtractorParams.getLengths(outputLengthLeft).getKey() == null) {
				        			sr.nextBytes(currentOutput);
				        			fos.write(currentOutput);
				        			outputLengthLeft -= UHRandExtractorParams.MAXIMAL_OUTPUT/8;
				        		}
				        		// round using output length appropriate to the remaining length
				        		currentOutput = new byte[UHRandExtractorParams.getLengths(outputLengthLeft).getKey()/8];
				        		sr.nextBytes(currentOutput);
				        		fos.write(currentOutput);
				        		fos.close();
				        		
				        		String message = "Stored "+outputLength+" bits into: "+outFile.getAbsolutePath();
				        		otv.setText(message.toCharArray(), 0, message.length());
				        	} 
				        	else Log.e("random sequence", "Storing into external storage failed.");
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
				    } else {
				    	Log.e("random sequence", "Storing into external storage failed.");
				    }
				} 
				// display the randomness extractor produced sequence
				else {
					byte[] bytes = new byte[outputLength/8];
	        		sr.nextBytes(bytes);
					BigInteger randSequence = new BigInteger(1, bytes);
					randSequence.shiftRight(8 - bytes.length%8);
					otv.setText(randSequence.toString(16).toCharArray(), 0, randSequence.toString(16).toCharArray().length);
				}
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
    }
	
}
