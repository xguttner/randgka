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

public class LongTermKeyProvider {
	
	private static final String
				SP_FILE = "PKCData",
				PUBLIC_KEY = "PublicKey",
				PRIVATE_KEY = "PrivateKey";
	private PublicKey pubKey;
	private PrivateKey privKey;
	private SharedPreferences sp;
	private SharedPreferences.Editor spEditor;
	private SecureRandom secureRandom;
	
	public LongTermKeyProvider(Context context, SecureRandom secureRandom){
        sp = context.getSharedPreferences(SP_FILE, Context.MODE_PRIVATE);
        this.secureRandom = secureRandom;
    }
    
    public PublicKey getPublicKey(int length) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException{
        String pubKeyStr = sp.getString(PUBLIC_KEY+length, "");
        if (pubKeyStr.equals("")) {
        	generateKeys(length);
        	pubKeyStr = sp.getString(PUBLIC_KEY+length, "");
        }
        byte[] sigBytes = Base64.decode(pubKeyStr, Base64.DEFAULT);
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(sigBytes);
        KeyFactory keyFact = KeyFactory.getInstance("RSA", "BC");
        return keyFact.generatePublic(x509KeySpec);
    }
    
    public PrivateKey getPrivateKey(int length) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException{
        String privKeyStr = sp.getString(PRIVATE_KEY+length, "");
        if (privKeyStr.equals("")) {
        	generateKeys(length);
        	privKeyStr = sp.getString(PRIVATE_KEY+length, "");
        }
        byte[] sigBytes = Base64.decode(privKeyStr, Base64.DEFAULT);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(sigBytes);
        KeyFactory keyFact = KeyFactory.getInstance("RSA", "BC");
        return keyFact.generatePrivate(keySpec);
    }
    
    public void generateKeys(int length) throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "BC");
        generator.initialize(length, secureRandom);
        KeyPair pair = generator.generateKeyPair();
        pubKey = pair.getPublic();
        privKey = pair.getPrivate();
        byte[] publicKeyBytes = pubKey.getEncoded();
        String pubKeyStr = new String(Base64.encode(publicKeyBytes, Base64.DEFAULT));
        byte[] privKeyBytes = privKey.getEncoded();
        String privKeyStr = new String(Base64.encode(privKeyBytes, Base64.DEFAULT));            
        spEditor = sp.edit();
        spEditor.putString(PUBLIC_KEY+length, pubKeyStr);
        spEditor.putString(PRIVATE_KEY+length, privKeyStr);           
        spEditor.commit();
    }
}
