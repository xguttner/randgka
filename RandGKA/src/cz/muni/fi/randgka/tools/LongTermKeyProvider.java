package cz.muni.fi.randgka.tools;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

/**
 * Provider of the long-term key pair.
 */
public class LongTermKeyProvider {
	
	// strings using as template for keys stored using SharedPreferences
	private static final String
				SP_FILE = "PKCData",
				PUBLIC_KEY = "PublicKey",
				PRIVATE_KEY = "PrivateKey";
	
	private PublicKey pubKey;
	private PrivateKey privKey;
	private SharedPreferences sp;
	private SharedPreferences.Editor spEditor;
	private SecureRandom secureRandom;
	
	/**
	 * Constructor
	 * 
	 * @param context
	 * @param secureRandom
	 */
	public LongTermKeyProvider(Context context, SecureRandom secureRandom){
        sp = context.getSharedPreferences(SP_FILE, Context.MODE_PRIVATE);
        this.secureRandom = secureRandom;
    }
    
	/**
	 * retrieve the public key of the given length from SharedPreferences, generate a new key-pair, if not present
	 * 
	 * @param length in bits
	 * @return public key of the given length
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws InvalidKeySpecException
	 */
    public PublicKey getPublicKey(int length) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException{
        // try to find it in SharedPreferences
    	String pubKeyStr = sp.getString(PUBLIC_KEY+length, "");
        // if not present, generate a new one from scratch
    	if (pubKeyStr.equals("")) {
        	generateKeys(length);
        	pubKeyStr = sp.getString(PUBLIC_KEY+length, "");
        }
    	// form an appropriate object
        byte[] sigBytes = Base64.decode(pubKeyStr, Base64.DEFAULT);
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(sigBytes);
        KeyFactory keyFact = KeyFactory.getInstance(Constants.RSA, Constants.PREFFERED_CSP);
        return keyFact.generatePublic(x509KeySpec);
    }
    
    /**
     * retrieve the public key of the given length from SharedPreferences, generate a new key-pair, if not present
     * 
     * @param length in bits
     * @return private key of the given length
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws InvalidKeySpecException
     */
    public PrivateKey getPrivateKey(int length) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException{
    	// try to find it in SharedPreferences
    	String privKeyStr = sp.getString(PRIVATE_KEY+length, "");
    	// if not present, generate a new one from scratch
    	 if (privKeyStr.equals("")) {
        	generateKeys(length);
        	privKeyStr = sp.getString(PRIVATE_KEY+length, "");
        }
    	// form an appropriate object
         byte[] sigBytes = Base64.decode(privKeyStr, Base64.DEFAULT);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(sigBytes);
        KeyFactory keyFact = KeyFactory.getInstance(Constants.RSA, Constants.PREFFERED_CSP);
        return keyFact.generatePrivate(keySpec);
    }
    
    /**
     * generate a new key pair of the given length and store it in SharedPreferences
     * 
     * @param length in bits
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     */
    public void generateKeys(int length) throws NoSuchAlgorithmException, NoSuchProviderException {
    	// generate key pair
        KeyPairGenerator generator = KeyPairGenerator.getInstance(Constants.RSA, Constants.PREFFERED_CSP);
        generator.initialize(length, secureRandom);
        KeyPair pair = generator.generateKeyPair();
        
        pubKey = pair.getPublic();
        privKey = pair.getPrivate();
        
        // encode it
        byte[] publicKeyBytes = pubKey.getEncoded();
        String pubKeyStr = new String(Base64.encode(publicKeyBytes, Base64.DEFAULT));
        byte[] privKeyBytes = privKey.getEncoded();
        String privKeyStr = new String(Base64.encode(privKeyBytes, Base64.DEFAULT));            
        
        // store it
        spEditor = sp.edit();
        spEditor.putString(PUBLIC_KEY+length, pubKeyStr);
        spEditor.putString(PRIVATE_KEY+length, privKeyStr);           
        spEditor.commit();
    }
}
