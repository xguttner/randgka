package cz.muni.fi.randgka.tools;

public final class Constants {
	
	public static final String RUN_GKA_PROTOCOL = "cz.muni.fi.androidrandextr.RUN_GKA_PROTOCOL";
	
	public static final String SERVER_START = "cz.muni.fi.androidrandextr.SERVER_START";
	
	public static final String CONNECT_TO_DEVICE = "cz.muni.fi.androidrandextr.CONNECT_TO_DEVICE";

	public static final String GET_PARTICIPANTS = "cz.muni.fi.androidrandextr.GET_PARTICIPANTS";
	
	public static final String GET_PARAMS = "cz.muni.fi.androidrandextr.GET_PARAMS";

	public static final String RETURN_DEVICES = "cz.muni.fi.androidrandextr.RETURN_DEVICES";
	
	public static final String GET_MINENTROPY_DATA = "cz.muni.fi.androidrandextr.GET_MINENTROPY_DATA";
	
	public static final String GET_RANDOM_DATA = "cz.muni.fi.androidrandextr.GET_RANDOM_DATA";
	
	public static final String GET_GKA_KEY = "cz.muni.fi.androidrandextr.GET_GKA_KEY";
	
	public static final String PROTOCOL_RANDOMNESS = "cz.muni.fi.androidrandextr.PROTOCOL_RANDOMNESS";
	
	public static final String ACTION_GKA_KEY = "cz.muni.fi.randgka.ACTION_GKA_KEY";

	public static final String RETURN_GKA_KEY = "cz.muni.fi.androidrandextr.RETURN_GKA_KEY";
	
	/**
	 * Request codes for intents
	 */
	public static final int MINENTROPY_DATA_RC = 1877;

	public static final int RANDOM_DATA_RC = 1878;
	
	public static final int REQUEST_ENABLE_BT = 1785;
	public static final int REQUEST_DISCOVERABLE_BT = 1786;

	/**
	 * Message whats 
	 */
	public static final int BROADCAST_PARTICIPANTS = 2877;
	
	public static final int RECEIVED_DATA = 2878;
	
	public static final String BLUETOOTH_GKA = "Bluetooth";
	
	public static final String WIFI_GKA = "Wi-Fi";

	public static final String SHOW_RETRIEVE_KEY = "cz.muni.fi.androidrandextr.SHOW_RETRIEVE_KEY";

	public static final String RETRIEVE_KEY = "cz.muni.fi.androidrandextr.RETRIEVE_KEY";

	public static final String KEY = "key";

	public static final int WIFI_PORT = 6540;
	
	public static final String LEADER_RUN = "LEADER_RUN",
			MEMBER_RUN = "MEMBER_RUN",
			GKA_RUN = "GKA_RUN",
			SET_SECURE_RANDOM = "SET_SECURE_RANDOM",
			STOP = "STOP";

	public static final String VERSION = "version";

	public static final String NONCE_LENGTH = "nonceLength";

	public static final String GROUP_KEY_LENGTH = "groupKeyLength";

	public static final String PUBLIC_KEY_LENGTH = "publicKeyLength";

	public static final String PARTICIPANTS = "participants";

	public static final String PMESSAGE = "pMessage";

	public static final String DEVICE = "device";

	public static final String TECHNOLOGY = "technology";

	public static final String IS_LEADER = "isLeader";

	public static final int REQUEST_RETRIEVE_GKA_KEY = 1789;
}
