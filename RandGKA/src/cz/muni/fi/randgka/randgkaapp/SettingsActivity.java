package cz.muni.fi.randgka.randgkaapp;

import cz.muni.fi.randgkaapp.R;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (android.os.Build.VERSION.SDK_INT >= 11) {
			getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
		} else {
			addPreferencesFromResource(R.xml.preferences);
		}
		
	}
}
