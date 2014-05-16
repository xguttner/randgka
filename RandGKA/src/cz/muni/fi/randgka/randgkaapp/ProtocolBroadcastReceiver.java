package cz.muni.fi.randgka.randgkaapp;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
				keyTextView,
				versionTV, 
				nonceLengthTV,
				groupKeyLengthTV,
				publicKeyLengthTV;
	
	private String bytesToHex(byte[] in) {
	    final StringBuilder builder = new StringBuilder();
	    for(byte b : in) {
	        builder.append(String.format("%02x", b));
	    }
	    return builder.toString();
	}
	
	public static final String GET_PARTICIPANTS = "get_participants",
							PRINT_DEVICE = "print_device";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(GET_PARTICIPANTS)) {
			if (participantsTextView != null) {
				participantsTextView.setText("");
				BluetoothGKAParticipants gkaParticipants = (BluetoothGKAParticipants)intent.getSerializableExtra("participants");
				if (gkaParticipants != null) {
					BluetoothFeatures bf = null;
					
					for (GKAParticipant g : gkaParticipants.getParticipants()) {
						bf = gkaParticipants.getBluetoothFeaturesFor(g.getId());
						participantsTextView.append(""+bf.getName()+" ("+bf.getMacAddress()+")");
						if (g.getAuthPublicKey()!=null) {
							try {
								MessageDigest md = MessageDigest.getInstance("SHA-256");
								md.update(bf.getMacAddress().getBytes());
								participantsTextView.append("\nAuthentication hash: " + bytesToHex(md.digest(g.getAuthPublicKey().getEncoded())));
							} catch (NoSuchAlgorithmException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							//participantsTextView.append("\nPublicKey: "+((RSAPublicKey)g.getAuthPublicKey()).getModulus().toString(16)+
							//		" / "+((RSAPublicKey)g.getAuthPublicKey()).getPublicExponent().toString(16));
						}
						participantsTextView.append("\n\n");
					}
				}
			}
		} else if (intent.getAction().equals(Constants.GET_PARAMS)) {
			
			String version = intent.getIntExtra("version", 0)!=0 ? (intent.getIntExtra("version", 1)==2 ? "Authenticated + key confirmation" : "Authenticated") : "Non-authenticated";
			String nonceLength = String.valueOf(intent.getIntExtra("nonceLength", 0)*8);
			String groupKeyLength = String.valueOf(intent.getIntExtra("groupKeyLength", 0)*8);
			String publicKeyLength = String.valueOf(intent.getIntExtra("publicKeyLength", 0)*8);
			
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
		} else if (intent.getAction().equals(PRINT_DEVICE)) {
			participantsTextView.append(intent.getStringExtra("device")+"\n");
		}
	}
	
	public void setTextViews(TextView participantsTextView, TextView keyTextView,  
			TextView versionTV, TextView nonceLengthTV, TextView groupKeyLengthTV, TextView publicKeyLengthTV) {
		this.versionTV = versionTV;
		this.nonceLengthTV = nonceLengthTV;
		this.groupKeyLengthTV = groupKeyLengthTV;
		this.publicKeyLengthTV = publicKeyLengthTV;
		this.participantsTextView = participantsTextView;
		this.keyTextView = keyTextView;
	}

}
