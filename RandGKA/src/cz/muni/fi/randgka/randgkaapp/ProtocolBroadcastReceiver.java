package cz.muni.fi.randgka.randgkaapp;

import java.math.BigInteger;
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

	private TextView participantsTextView,
				keyTextView;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Constants.GET_PARTICIPANTS)) {
			if (participantsTextView != null) {
				participantsTextView.setText("");
				BluetoothGKAParticipants gkaParticipants = (BluetoothGKAParticipants)intent.getSerializableExtra("participants");
				if (gkaParticipants != null) {
					BluetoothFeatures bf = null;
					for (GKAParticipant g : gkaParticipants.getParticipants()) {
						bf = gkaParticipants.getBluetoothFeaturesFor(g.getId());
						participantsTextView.append(""+bf.getName()+" ("+bf.getMacAddress()+")"+
								((g.getAuthPublicKey()!=null)?("\nPublicKey: "+((RSAPublicKey)g.getAuthPublicKey()).getModulus()+
										" / "+((RSAPublicKey)g.getAuthPublicKey()).getPublicExponent()):"")+"\n\n");
					}
				}
			}
		} else if (intent.getAction().equals(Constants.GET_GKA_KEY)) {
			if (keyTextView != null) {
				keyTextView.setText("");
				byte[] key = intent.getByteArrayExtra("key");
				keyTextView.append((new BigInteger(key)).toString(16));
			}
		}
	}
	
	public void setTextViews(TextView participantsTextView, TextView keyTextView) {
		this.participantsTextView = participantsTextView;
		this.keyTextView = keyTextView;
	}

}
