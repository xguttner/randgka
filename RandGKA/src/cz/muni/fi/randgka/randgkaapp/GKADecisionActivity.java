package cz.muni.fi.randgka.randgkaapp;

import cz.muni.fi.randgka.tools.Constants;
import cz.muni.fi.randgkaapp.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

public class GKADecisionActivity extends Activity {
	
	private static final String LEADER = "Leader";
	private static final String MEMBER = "Member";
	private boolean returnKey;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gkatech_decision);
		
		returnKey = getIntent().getBooleanExtra("return_key", false);
		if (returnKey) {
			TextView externalIntentNotifierTV = (TextView) findViewById(R.id.textView3);
			externalIntentNotifierTV.setText("Called by an external application.");
		}
		
		ArrayAdapter<String> technologies = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
		technologies.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		technologies.add(Constants.BLUETOOTH_GKA);
		technologies.add(Constants.WIFI_GKA);
		Spinner technologySpinner = (Spinner)findViewById(R.id.spinner2);
		technologySpinner.setAdapter(technologies);
		
		ArrayAdapter<String> roles = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
		roles.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		roles.add(LEADER);
		roles.add(MEMBER);
		Spinner roleSpinner = (Spinner)findViewById(R.id.spinner1);
		roleSpinner.setAdapter(roles);
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.gkatech_decision, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return false;
	}
	
	public void moveToGKARole(View view) {
		CheckBox freshKeyBox = (CheckBox)findViewById(R.id.checkBox1);
		
		boolean freshKey = freshKeyBox.isChecked();
		
		Spinner technologySpinner = (Spinner)findViewById(R.id.spinner2);
		String technology = (String) technologySpinner.getSelectedItem();
		
		Spinner roleSpinner = (Spinner)findViewById(R.id.spinner1);
		String role = (String) roleSpinner.getSelectedItem();
		if (role.equals(LEADER)) {
			Intent moving = new Intent(this, GKALeaderActivity.class);
			moving.putExtra("technology", technology);
			moving.putExtra("freshKey", freshKey);
			if (returnKey) {
				moving.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
				moving.putExtra("return_key", true);
			}
			startActivity(moving);
		} else if (role.equals(MEMBER)) {
			Intent moving = new Intent(this, GKAMemberActivity.class);
			moving.putExtra("technology", technology);
			moving.putExtra("freshKey", freshKey);
			if (returnKey) {
				moving.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
				moving.putExtra("return_key", true);
			}
			startActivity(moving);
		}
	}
}
