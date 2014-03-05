package cz.muni.fi.randgka.randgkaapp;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;

import cz.muni.fi.randgka.provider.RandGKAProvider;
import cz.muni.fi.randgka.provider.minentropy.CameraMES;
import cz.muni.fi.randgka.provider.minentropy.CameraMESHolder;
import cz.muni.fi.randgkaapp.R;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class RandExtractor2AppActivity extends Activity {

	private SecureRandom sr;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rand_extractor2_app);
        
        SurfaceView surface = null;
		
		surface=(SurfaceView)findViewById(R.id.surfaceView1);
        SurfaceHolder holder=surface.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        CameraMES cameraMES = new CameraMES();
        cameraMES.initialize(surface);
        CameraMESHolder.cameraMES = cameraMES;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.rand_extractor2_app, menu);
		return true;
	}
	
	public void extract(View view) {
		
		Provider pr = new RandGKAProvider();
        try {
			sr = SecureRandom.getInstance("UHRandExtractor", pr);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		byte [] bytes = new byte[100];
		sr.nextBytes(bytes);
		Log.d("rand", new String(bytes));
    }
	
}
