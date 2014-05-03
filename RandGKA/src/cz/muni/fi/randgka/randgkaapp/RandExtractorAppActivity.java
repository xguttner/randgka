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
import cz.muni.fi.randgka.provider.minentropy.MinEntropySourceType;
import cz.muni.fi.randgka.tools.Constants;
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

public class RandExtractorAppActivity extends Activity {

	private SharedPreferences sp;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rand_extractor_app);
        
		sp = PreferenceManager.getDefaultSharedPreferences(this);
		
		switch (MinEntropySourceType.valueOf(sp.getString("pref_mes_type", "CAMERA"))) {
			case MICROPHONE:
				break;
				
			case CAMERA:
			default:
				SurfaceView surface=(SurfaceView)findViewById(R.id.surfaceView1);
				SurfaceHolder holder=surface.getHolder();
		        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		        
		        CameraMES cameraMES = new CameraMES();
		        cameraMES.initialize(surface);
		        CameraMESHolder.cameraMES = cameraMES;
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
		if (CameraMESHolder.cameraMES!= null) {
			CameraMESHolder.cameraMES.stop();
			CameraMESHolder.cameraMES = null;
		}
	}
	
	public void getData(View view) {
		int outputLength = Integer.parseInt(sp.getString("pref_re_length", "128"))/8;
		
		TextView textView = (TextView) findViewById(R.id.textView2);
		
		Provider pr = new RandGKAProvider();
        try {
			SecureRandom sr = SecureRandom.getInstance(sp.getString("pref_re_type", "UHRandExtractor"), pr);
			
			String outFileName = sp.getString("pref_re_outfile", "");
			if (outFileName.length() > 0) {
				String state = Environment.getExternalStorageState();
			    if (Environment.MEDIA_MOUNTED.equals(state)) {
			    	File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
			    	File outFile = new File(dir, outFileName);
			        try {
			        	if ((dir.exists() || dir.mkdir()) && (outFile.exists() || outFile.createNewFile()) && outFile.canWrite()) {
			        		FileOutputStream fos = new FileOutputStream(outFile);
			        		int maxBytesInOneRound = Constants.MAX_RE_OUTPUT/8;
			        		if (outputLength > maxBytesInOneRound) {
			        			int rounds = (int)Math.ceil((double)outputLength/maxBytesInOneRound);
			        			byte[] bytes = new byte[maxBytesInOneRound];
			        			for (int i = 0; i < rounds; i++) {
			        				sr.nextBytes(bytes);
			        				fos.write(bytes);
			        			}
			        		} else {
			        			byte[] bytes = new byte[outputLength];
				        		sr.nextBytes(bytes);
				        		fos.write(bytes);
			        		}
			        		fos.close();
			        		String message = "Stored "+outputLength*8+" bits into: "+outFile.getAbsolutePath();
			        		textView.setText(message.toCharArray(), 0, message.length());
			        	} else Log.e("random sequence", "Storing into external storage failed.");
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
			    } else {
			    	Log.e("random sequence", "Storing into external storage failed.");
			    }
			} else {
				byte[] bytes = new byte[outputLength];
        		sr.nextBytes(bytes);
				BigInteger randSequence = new BigInteger(1, bytes);
				randSequence.shiftRight(8 - bytes.length%8);
				textView.setText(randSequence.toString(16).toCharArray(), 0, randSequence.toString(16).toCharArray().length);
			}
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
    }
	
}
