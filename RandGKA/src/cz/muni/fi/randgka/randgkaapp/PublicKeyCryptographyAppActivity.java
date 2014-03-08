package cz.muni.fi.randgka.randgkaapp;

import java.util.Arrays;

import cz.muni.fi.randgka.library.PublicKeyCryptography;
import cz.muni.fi.randgkaapp.R;
import android.os.Bundle;
import android.app.Activity;
import android.util.Base64;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

public class PublicKeyCryptographyAppActivity extends Activity {

	private PublicKeyCryptography publicKeyCryptography;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_public_key_cryptography_app);
		
		publicKeyCryptography = new PublicKeyCryptography(this);
		
		printCertificate();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.public_key_cryptography_app, menu);
		return true;
	}

	public void generateKeyPair(View view) {
		publicKeyCryptography.generateKeys();
		
		printCertificate();
	}
	
	private void printCertificate() {
		EditText certificateInput = (EditText)findViewById(R.id.certificate_input);
		int length = Base64.encode(publicKeyCryptography.getPublicKey().getEncoded(), Base64.DEFAULT).length;
		byte[]hash = publicKeyCryptography.getPublicKeyHash();
		String byteStr = Arrays.toString(Base64.encode(publicKeyCryptography.getPublicKey().getEncoded(), Base64.DEFAULT));
		String hashStr = "";
		for (byte b : hash) hashStr += String.format("%02X ", b);
		certificateInput.setText(byteStr+ "|" + hashStr+"|"+length);
	}
}
