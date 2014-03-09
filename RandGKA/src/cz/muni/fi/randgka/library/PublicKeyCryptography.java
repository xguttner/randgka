package cz.muni.fi.randgka.library;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import cz.muni.fi.randgka.tools.Constants;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

public class PublicKeyCryptography {
	
	private static final String
				SP_FILE = "PKCData",
				PUBLIC_KEY = "PublicKey",
				PRIVATE_KEY = "PrivateKey";
	private PublicKey pubKey;
	private PrivateKey privKey;
	private int publicKeyLength;
	private SharedPreferences sp1, sp2;
	private SharedPreferences.Editor spEditor;

	public PublicKeyCryptography(Context context){
        sp1 = context.getSharedPreferences(SP_FILE, Context.MODE_PRIVATE);
        sp2 = PreferenceManager.getDefaultSharedPreferences(context);
        publicKeyLength = Integer.parseInt(sp2.getString("pref_public_key_length", "512"));
        
        pubKey = getPublicKey();
        privKey = getPrivateKey();
        if (pubKey == null || privKey == null) {
        	generateKeys();
        	pubKey = getPublicKey();
            privKey = getPrivateKey();
        }
    }
	
	public byte[] getPublicKeyBase64(){
        return Base64.encode(getPublicKey().getEncoded(), Base64.DEFAULT);
    }
	
	public PublicKey getPublicKey(){
        String pubKeyStr = sp1.getString(PUBLIC_KEY, "");       
        byte[] sigBytes = Base64.decode(pubKeyStr, Base64.DEFAULT);
        Log.d("pcBytes2", Arrays.toString(sigBytes));
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(sigBytes);
        KeyFactory keyFact = null;
        try {
            keyFact = KeyFactory.getInstance("RSA", "BC");
            return  keyFact.generatePublic(x509KeySpec);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return null;
    }
    public String getPublicKeyAsString(){
        return sp1.getString(PUBLIC_KEY, "");       
    }
    public byte[] getPublicKeyHash() {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] hash = md.digest(sp1.getString(PUBLIC_KEY, "").getBytes());
	    	return hash;
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
    }
    
    public PrivateKey getPrivateKey(){
        String privKeyStr = sp1.getString(PRIVATE_KEY, "");
        byte[] sigBytes = Base64.decode(privKeyStr, Base64.DEFAULT);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(sigBytes);
        KeyFactory keyFact = null;
        try {
            keyFact = KeyFactory.getInstance("RSA", "BC");
            return  keyFact.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return null;
    }
    public String getPrivateKeyAsString(){
        return sp1.getString(PRIVATE_KEY, "");      
    }
	public void generateKeys(){
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "BC");
            generator.initialize(publicKeyLength, new SecureRandom()); // replace with my own solution
            KeyPair pair = generator.generateKeyPair();
            pubKey = pair.getPublic();
            privKey = pair.getPrivate();
            byte[] publicKeyBytes = pubKey.getEncoded();
            String pubKeyStr = new String(Base64.encode(publicKeyBytes, Base64.DEFAULT));
            byte[] privKeyBytes = privKey.getEncoded();
            String privKeyStr = new String(Base64.encode(privKeyBytes, Base64.DEFAULT));            
            spEditor = sp1.edit();
            spEditor.putString(PUBLIC_KEY, pubKeyStr);
            spEditor.putString(PRIVATE_KEY, privKeyStr);           
            spEditor.commit();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }           
    }
}
