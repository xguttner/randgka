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
	
	private Spinner technologySpinner,
		roleSpinner;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gka_decision);
		
		returnKey = getIntent().getBooleanExtra(Constants.RETRIEVE_KEY, false);
		if (returnKey) {
			TextView externalIntentNotifierTV = (TextView) findViewById(R.id.textView3);
			externalIntentNotifierTV.setText("Called by an external application.");
		}
		
		ArrayAdapter<String> technologies = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
		technologies.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		technologies.add(Constants.BLUETOOTH_GKA);
		technologies.add(Constants.WIFI_GKA);
		technologySpinner = (Spinner)findViewById(R.id.spinner2);
		technologySpinner.setAdapter(technologies);
		
		ArrayAdapter<String> roles = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
		roles.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		roles.add(LEADER);
		roles.add(MEMBER);
		roleSpinner = (Spinner)findViewById(R.id.spinner1);
		roleSpinner.setAdapter(roles);
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.empty, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return false;
	}
	
	public void moveToGKARole(View view) {
		CheckBox freshKeyBox = (CheckBox)findViewById(R.id.checkBox1);
		boolean freshKey = freshKeyBox.isChecked();
		
		String technology = (String) technologySpinner.getSelectedItem();
		String role = (String) roleSpinner.getSelectedItem();
		
		Intent moving = null;
		if (role.equals(LEADER)) moving = new Intent(this, GKALeaderActivity.class);
		else moving = new Intent(this, GKAMemberActivity.class);
		
		moving.putExtra("technology", technology);
		moving.putExtra("freshKey", freshKey);
		
		if (returnKey) {
			moving.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
			moving.putExtra(Constants.RETRIEVE_KEY, true);
		}
		
		startActivity(moving);
	}
}
