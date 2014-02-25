package cz.muni.fi.androidrandextr.communication;

import cz.muni.fi.androidrandextr.gka.ProtocolCommand;
import cz.muni.fi.randgka.library.CommunicationThread;
import cz.muni.fi.randgka.library.GKAProtocol;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ProtocolHandler extends Handler {
	
	private GKAProtocol protocol;
	private CommunicationThread ct;

	public ProtocolHandler(GKAProtocol protocol) {
		this.protocol = protocol;
	}
	
	public void setCT(CommunicationThread ct) {
		this.ct = ct;
	}
	
	@Override
	public void handleMessage(Message message) {
		ProtocolCommand pc = convertToPC(message.getData().getByteArray("command"));
		/*ProtocolCommand nextPc = protocol.getCMD(pc.getNextId(), pc.getMessage());
		if (nextPc != null) {
			ct.write(nextPc.toByteArray());
		}*/
	}
	
	public void runProtocol() {
		Log.d("try", "to run");
		if (ct != null) {
			Log.d("try", "to run inside");
			/*ProtocolCommand nextPc = protocol.getCMD((byte)0x01, null);
			if (nextPc != null) {
				ct.write(nextPc.toByteArray());
			}*/
		}
	}
	
	public ProtocolCommand convertToPC(byte[] bs) {
		ProtocolCommand pc = new ProtocolCommand();
		pc.setId(bs[0]);
		pc.setNextId(bs[1]);
		byte[] message = new byte[bs.length-2];
		System.arraycopy(bs, 2, message, 0, bs.length-2);
		pc.setMessage(message);
		//pc.setTarget(Integer.parseInt((new ByteArrayInputStream(bs, 2, 1)).toString()));
		return pc;
	}
}
