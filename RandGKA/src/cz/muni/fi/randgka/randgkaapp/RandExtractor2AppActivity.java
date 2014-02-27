package cz.muni.fi.randgka.randgkaapp;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;

import cz.muni.fi.randgka.library.ByteSequence;
import cz.muni.fi.randgka.library.CameraMES;
import cz.muni.fi.randgka.library.MinEntropySource;
import cz.muni.fi.randgka.library.MinEntropySourceType;
import cz.muni.fi.randgka.provider.RandGKAProvider;
import cz.muni.fi.randgka.provider.random.UHRandExtractor;
import cz.muni.fi.randgkaapp.R;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class RandExtractor2AppActivity extends Activity {

	private MinEntropySource source;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rand_extractor2_app);
        
        SurfaceView surface = null;
        
        MinEntropySourceType minEntropySourceType = (MinEntropySourceType)getIntent().getSerializableExtra("minEntropySourceType");
		String targetFileName = getIntent().getStringExtra("targetFileName");
		
		surface=(SurfaceView)findViewById(R.id.surfaceView1);
        SurfaceHolder holder=surface.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
		source = new CameraMES(surface);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.rand_extractor2_app, menu);
		return true;
	}
	
	public void extract(View view) {
		int minEntropySequenceLength = 839;
		DataInputStream dis = new DataInputStream(getResources().openRawResource(R.raw.seed));
		ByteSequence seed = null;
		try {
			byte[] seedData = new byte[minEntropySequenceLength];
			for (int i = 0; i < minEntropySequenceLength; i++) {
					seedData[i] = (byte)(0xff & dis.read());
			}
			seed = new ByteSequence(seedData, minEntropySequenceLength);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		byte [] bytes = new byte[100];
		
		UHRandExtractor re = new UHRandExtractor();
		re.initialize(source, seed);
		re.nextBytes(bytes);
		Log.d("bytes", new String(bytes));
    }
	
}
