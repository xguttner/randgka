package cz.muni.fi.randgka.library;

import cz.muni.fi.randgka.bluetoothgka.BluetoothFeatures;
import cz.muni.fi.randgka.bluetoothgka.BluetoothGKAParticipants;
import cz.muni.fi.randgka.library.Constants;
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
					for (BluetoothFeatures bf : gkaParticipants.getFeatures()) {
						textView.append(bf.getName()+" "+bf.getMacAddress()+" "+((bf.getPublicKey()!=null)?bf.getPublicKey().toString():""));
					}
				}
			}
		}
	}
	
	public void setTextView(TextView textView) {
		this.textView = textView;
	}

}
