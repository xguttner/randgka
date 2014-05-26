package cz.muni.fi.randgka.tools;

/**
 * Application shared constants.
 */
public final class Constants {
	
	// implicit intent action for key retrieval
	public static final String ACTION_GKA_KEY = "cz.muni.fi.randgka.ACTION_GKA_KEY";
	public static final int REQUEST_RETRIEVE_GKA_KEY = 1789;

	// technology and randomness source options, also displayed in one of the spinners
	public static final String BLUETOOTH_GKA = "Bluetooth";
	public static final String WIFI_GKA = "Wi-Fi";
	public static final String NATIVE_ES = "native";
	public static final String RAND_EXT_ES = "randomness extractor";

	// constants regarding the communication service and protocol instance lifecycle
	public static final String RETRIEVE_KEY = "cz.muni.fi.randgka.RETRIEVE_KEY";
	public static final String LEADER_RUN = "cz.muni.fi.randgka.LEADER_RUN";
	public static final String MEMBER_RUN = "cz.muni.fi.randgka.MEMBER_RUN";
	public static final String GKA_RUN = "cz.muni.fi.randgka.GKA_RUN";
	public static final String SET_AVAILABLE_SOURCES = "cz.muni.fi.randgka.SET_AVAILABLE_SOURCES";
	public static final String STOP = "cz.muni.fi.randgka.STOP";
	public static final String TECHNOLOGY = "cz.muni.fi.randgka.TECHNOLOGY";
	public static final String ENTROPY_SOURCE = "cz.muni.fi.randgka.ENTROPY_SOURCE";
	public static final String CONNECTED_TO_BLUETOOTH_SERVER = "cz.muni.fi.randgka.CONNECTED_TO_BLUETOOTH_SERVER";
	
	// protocol parameters
	public static final String VERSION = "cz.muni.fi.randgka.VERSION";
	public static final String NONCE_LENGTH = "cz.muni.fi.randgka.NONCE_LENGTH";
	public static final String GROUP_KEY_LENGTH = "cz.muni.fi.randgka.GROUP_KEY_LENGTH";
	public static final String PUBLIC_KEY_LENGTH = "cz.muni.fi.randgka.PUBLIC_KEY_LENGTH";
	public static final String PARTICIPANTS = "cz.muni.fi.randgka.PARTICIPANTS";
	public static final String KEY = "cz.muni.fi.randgka.KEY";
	public static final String IS_LEADER = "cz.muni.fi.randgka.IS_LEADER";
	
	public static final String PMESSAGE = "cz.muni.fi.randgka.PMESSAGE";
	public static final String DEVICE = "cz.muni.fi.randgka.DEVICE";

	// chosen security related constants
	public static final String SIGN_ALG = "SHA256withRSA";
	public static final String PREFFERED_CSP = "BC";
	public static final String RSA = "RSA";
	public static final String HASH_ALG = "SHA-256";
}
