package cz.muni.fi.randgka.randgkaapp;

import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;

import cz.muni.fi.randgka.bluetoothgka.BluetoothFeatures;
import cz.muni.fi.randgka.bluetoothgka.BluetoothGKAParticipants;
import cz.muni.fi.randgka.gka.GKAParticipant;
import cz.muni.fi.randgka.tools.Constants;
import cz.muni.fi.randgkaapp.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.TextView;

public class ProtocolBroadcastReceiver extends BroadcastReceiver {

	private TextView participantsTextView,
				keyTextView,
				protocolTV,  
				versionTV, 
				nonceLengthTV, 
				groupKeyLengthTV,
				publicKeyLengthTV;
	
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
								((g.getAuthPublicKey()!=null)?("\nPublicKey: "+((RSAPublicKey)g.getAuthPublicKey()).getModulus().toString(16)+
										" / "+((RSAPublicKey)g.getAuthPublicKey()).getPublicExponent().toString(16)):"")+"\n\n");
					}
				}
			}
		} else if (intent.getAction().equals(Constants.GET_PARAMS)) {
			
			String protocol = intent.getStringExtra("protocol");
			String version = intent.getBooleanExtra("isAuth", false) ? "auth" : "non-auth";
			String nonceLength = String.valueOf(intent.getIntExtra("nonceLength", 0)*8);
			String groupKeyLength = String.valueOf(intent.getIntExtra("groupKeyLength", 0)*8);
			String publicKeyLength = String.valueOf(intent.getIntExtra("publicKeyLength", 0)*8);
			
			protocolTV.setText(protocol);
			versionTV.setText(version);
			nonceLengthTV.setText(nonceLength);
			groupKeyLengthTV.setText(groupKeyLength);
			publicKeyLengthTV.setText(publicKeyLength);
			
		} else if (intent.getAction().equals(Constants.GET_GKA_KEY)) {
			if (keyTextView != null) {
				keyTextView.setText("");
				byte[] key = intent.getByteArrayExtra("key");
				keyTextView.append((new BigInteger(key)).toString(16));
			}
		}
	}
	
	public void setTextViews(TextView participantsTextView, TextView keyTextView, TextView protocolTV,  
			TextView versionTV, TextView nonceLengthTV, TextView groupKeyLengthTV, TextView publicKeyLengthTV) {
		this.protocolTV = protocolTV;  
		this.versionTV = versionTV;
		this.nonceLengthTV = nonceLengthTV;
		this.groupKeyLengthTV = groupKeyLengthTV;
		this.publicKeyLengthTV = publicKeyLengthTV;
		this.participantsTextView = participantsTextView;
		this.keyTextView = keyTextView;
	}

}
