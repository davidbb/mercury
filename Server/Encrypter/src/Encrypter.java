import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.Cipher;

import org.bouncycastle.jce.provider.BouncyCastleProvider;


public class Encrypter {
	/**
	 * 
	 * @param args Two arguments: first is the modulus, second is the text to encrypt
	 */
	public static void main(String args[]) {
		if (args.length != 2) {
			//cannot continue
			System.out.println("FAIL - not 2 arguments");
			return;
		}
		RSAPublicKeySpec keySpec=null;
		PublicKey pubKey = null;
		BigInteger m = new BigInteger(args[0], 10);
		BigInteger e = new BigInteger("10001", 16);
		//System.out.println(m);
		//System.out.println(e);
		keySpec = new RSAPublicKeySpec(m, e);
		KeyFactory fact = null;
		Security.addProvider(new BouncyCastleProvider());
		try {
			fact = KeyFactory.getInstance("RSA");
			pubKey = fact.generatePublic(keySpec);
			//System.out.println(pubKey.toString());
		} catch (Exception exception) {
			exception.printStackTrace();
			return;
		}
		//encrypt the text, then return it
		System.out.println(encryptQR(pubKey, args[1]));
		return;
		
	}
	
	private static String encryptQR(PublicKey pubKey, String plaintext) {
		byte[] ciphertext=null;
		try {
			Cipher cipher = null;
			cipher = Cipher.getInstance("RSA/NONE/PKCS1Padding", "BC");
			cipher.init(Cipher.ENCRYPT_MODE, pubKey);
			ciphertext = cipher.doFinal(plaintext.getBytes());
		} catch (Exception e) {
			return "FAIL";
		}
    	//String encrypted=new String(cipherData);
    	//System.out.println(encrypted);
		
    	String encodedCiphertext = String.valueOf(Base64Coder.encode(ciphertext));
    	encodedCiphertext = encodedCiphertext.replaceAll("\\+", "%2B");
    	encodedCiphertext = encodedCiphertext.replaceAll("/", "%2F");
    	return encodedCiphertext;
	}
}
