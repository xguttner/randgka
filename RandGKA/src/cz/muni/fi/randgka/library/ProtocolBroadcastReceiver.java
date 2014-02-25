package cz.muni.fi.randgka.library;

import cz.muni.fi.randgka.library.Constants;
import cz.muni.fi.randgka.randgkamiddle.RandExtractorActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.TextView;

public class ProtocolBroadcastReceiver extends BroadcastReceiver {

	private TextView textView;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Constants.GET_PARTICIPANTS)) {
			if (textView != null) {
				textView.setText("");
				GKAParticipants gkaParticipants = (GKAParticipants)intent.getSerializableExtra("participants");
				if (gkaParticipants != null) {
					for (GKAParticipant p : gkaParticipants.getParticipants()) {
						textView.append(p.getName()+" "+p.getMacAddress()+" "+p.getPublicKey().toString());
					}
				}
			}
		}
	}
	
	public void setTextView(TextView textView) {
		this.textView = textView;
	}

}
