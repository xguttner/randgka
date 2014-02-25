package cz.muni.fi.randgkaapp;

import java.math.BigInteger;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.widget.TextView;

public class GKAProtocolPrintKeyAppActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gkaprotocol_print_key_app);
		
		BigInteger key = new BigInteger(1, getIntent().getByteArrayExtra("key"));
		
		TextView tv = (TextView) findViewById(R.id.textView1);
		tv.setText(key.toString(16));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.gkaprotocol_print_key_app, menu);
		return true;
	}

}
