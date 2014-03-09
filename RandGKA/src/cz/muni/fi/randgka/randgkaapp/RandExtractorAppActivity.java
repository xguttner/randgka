package cz.muni.fi.randgka.randgkaapp;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;

import cz.muni.fi.randgka.provider.RandGKAProvider;
import cz.muni.fi.randgka.provider.minentropy.CameraMES;
import cz.muni.fi.randgka.provider.minentropy.CameraMESHolder;
import cz.muni.fi.randgka.provider.minentropy.MinEntropySourceType;
import cz.muni.fi.randgkaapp.R;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.SharedPreferences;
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
		
		Provider pr = new RandGKAProvider();
        try {
			SecureRandom sr = SecureRandom.getInstance(sp.getString("pref_re_type", "UHRandExtractor"), pr);
			byte[] bytes = new byte[outputLength];
			sr.nextBytes(bytes);
			
			BigInteger randSequence = new BigInteger(1, bytes);
			randSequence.shiftRight(8 - bytes.length%8);
			TextView textView = (TextView) findViewById(R.id.textView2);
			textView.setText(randSequence.toString(16).toCharArray(), 0, randSequence.toString(16).toCharArray().length);
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
    }
	
}
