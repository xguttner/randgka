package cz.muni.fi.randgka.randgkaapp;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;

import cz.muni.fi.randgka.provider.RandGKAProvider;
import cz.muni.fi.randgka.provider.minentropy.CameraMES;
import cz.muni.fi.randgka.provider.minentropy.CameraMESHolder;
import cz.muni.fi.randgka.tools.LongTermKeyProvider;
import cz.muni.fi.randgkaapp.R;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class LongTermKeyAppActivity extends Activity {
	private Spinner publicKeyLengthsSpinner,
		entropySourceSpinner;
	private TextView otv;
	
	private static final String NATIVE_ES = "native",
			RAND_EXT_ES = "randomness extractor";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_long_term_key_app);
		
		ArrayAdapter<CharSequence> publicKeyLengths = ArrayAdapter.createFromResource(this, R.array.pkLengths, android.R.layout.simple_spinner_item);
		publicKeyLengths.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		publicKeyLengthsSpinner = (Spinner)findViewById(R.id.spinner1);
		publicKeyLengthsSpinner.setAdapter(publicKeyLengths);
		
		ArrayAdapter<String> entropySources = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
		entropySources.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		entropySources.add(NATIVE_ES);
		entropySources.add(RAND_EXT_ES);
		entropySourceSpinner = (Spinner)findViewById(R.id.spinner2);
		entropySourceSpinner.setAdapter(entropySources);
		
		otv = (TextView) findViewById(R.id.textView1);
		
		SurfaceView surface=(SurfaceView)findViewById(R.id.surfaceView1);
		SurfaceHolder holder=surface.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
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
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (CameraMESHolder.cameraMES != null) {
			CameraMESHolder.cameraMES.stop();
			CameraMESHolder.cameraMES = null;
		}
	}

	public void generateKeyPair(View view) {
		Integer keyLength = Integer.parseInt(String.valueOf(publicKeyLengthsSpinner.getSelectedItem()));
		String entropySourceS = (String)entropySourceSpinner.getSelectedItem();
		
		SecureRandom secureRandom = null;
		try {
			if (entropySourceS.equals(NATIVE_ES)) {
				secureRandom = new SecureRandom();
			} else if (entropySourceS.equals(RAND_EXT_ES)) {
				Provider pr = new RandGKAProvider();
				secureRandom = SecureRandom.getInstance(RandGKAProvider.RAND_EXTRACTOR, pr);
			}
			LongTermKeyProvider longTermKeyProvider = new LongTermKeyProvider(this, secureRandom);
			longTermKeyProvider.generateKeys(keyLength);
			
			otv.setText("Successfully generated.");
		} catch (NoSuchAlgorithmException e) {
			otv.setText("An error occured. Try again (possibly with different settings).");
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			otv.setText("An error occured. Try again (possibly with different settings).");
			e.printStackTrace();
		}
	}
}
