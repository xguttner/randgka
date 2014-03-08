package cz.muni.fi.randgka.library;

import java.security.interfaces.RSAPublicKey;

import cz.muni.fi.randgka.bluetoothgka.BluetoothFeatures;
import cz.muni.fi.randgka.bluetoothgka.BluetoothGKAParticipants;
import cz.muni.fi.randgka.gka.GKAParticipant;
import cz.muni.fi.randgka.tools.Constants;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.TextView;

public class ProtocolBroadcastReceiver extends BroadcastReceiver {

	private TextView textView;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Constants.GET_PARTICIPANTS)) {
			if (textView != null) {
				textView.setText("");
				BluetoothGKAParticipants gkaParticipants = (BluetoothGKAParticipants)intent.getSerializableExtra("participants");
				if (gkaParticipants != null) {
					BluetoothFeatures bf = null;
					for (GKAParticipant g : gkaParticipants.getParticipants()) {
						bf = gkaParticipants.getBluetoothFeaturesFor(g.getId());
						textView.append(""+bf.getName()+" ("+bf.getMacAddress()+")"+
								((g.getAuthPublicKey()!=null)?("\nPublicKey: "+((RSAPublicKey)g.getAuthPublicKey()).getModulus()+
										" / "+((RSAPublicKey)g.getAuthPublicKey()).getPublicExponent()):"")+"\n\n");
					}
				}
			}
		}
	}
	
	public void setTextView(TextView textView) {
		this.textView = textView;
	}

}
