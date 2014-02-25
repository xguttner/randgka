package cz.muni.fi.androidrandextr;

import cz.muni.fi.randgkaapp.R;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.RadioGroup;

public class AuthenticationClientActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_authentication);
		
		//TODO: check if the device was already coupled and the key for next session is stored and enable/disable the Trust From option 
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.authentication, menu);
		return true;
	}
	
	public void proceedAuthentication(View view) {
		RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup1);
		int radioButtonId = radioGroup.getCheckedRadioButtonId();
		switch (radioButtonId) {
			case R.id.radio0: // NFC
				break;
			case R.id.radio2: // former key
				break;
			case R.id.radio1: // PIN
			default:
				Intent moving = new Intent(this, AuthenticationPINActivity.class);
				startActivity(moving);
				break;
		}
	}

}
