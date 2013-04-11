package ca.carleton.ccsl.mercury;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;

import org.apache.http.HttpResponse;
import org.apache.http.client.*;
import org.wadael.android.utils.net.HTTPPoster;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;


public class Mercury extends Activity {
    static private final String QRPASS_PREFIX = "QRPASS:";
    static private final String QRPASS_URL = "http://chart.apis.google.com/chart?cht=qr&chs=350x350&chl=QRPASS%3A";
    static private final int MENU_GENERATE = 1;
    static private final int MENU_NEW = 2;
    static private final int MENU_ABOUT = 3;
    static private final int MENU_SEND_PUBKEY = 4;
    static private final int RSA_KEY_LENGTH = 1024;
    /**
     * the filename of the private key for QRPass
     */
    static private final String PUBKEY_FILENAME = "public.key";
    static private final String PRIVKEY_FILENAME = "private.key";
    
    /**
     * Were we in the middle of running the standalone test popup?
     */
    static private boolean runningTest = false;
    
   	private TextView mQRCodeText;
	private Button mPictureButton;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        try {
        	runningTest = savedInstanceState.getBoolean("runningTest");
		} catch (Exception e) {
			runningTest = false;
		}
		if(runningTest) {
			test();
		}
        // capture our View elements
        mQRCodeText = (TextView) findViewById(R.id.QRData);
        mPictureButton = (Button) findViewById(R.id.PictureButton);
        
        // add a click listener to the button
        mPictureButton.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
        		scan();
        	}
        });
        
        if (keysExist()) {
        	mQRCodeText.setText(R.string.step2);
        }
    }
    
    @Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("runningTest", runningTest);
	}
    
    @Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}
    
    /* Creates the menu items */
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(0, MENU_NEW, 0, "Encrypt password").setIcon(R.drawable.flash);
    	menu.add(0, MENU_GENERATE, 0, "Generate keypair").setIcon(R.drawable.key);
        menu.add(0, MENU_SEND_PUBKEY, 0, "Transmit public key").setIcon(R.drawable.ic_menu_send);
        menu.add(0, MENU_ABOUT, 0, "About").setIcon(R.drawable.ic_menu_info_details);
        return true;
    }
    
    /* Handles item selections */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_GENERATE:
            generate();
            return true;
        case MENU_NEW:
        	test();
        	return true;
        case MENU_ABOUT:
        	//showAboutPopup();
        	showFancyAboutPopup();
            return true;
        case MENU_SEND_PUBKEY:
        	scanPubKeyURL();
        	//mailPubKey();
        }
        return false;
    }
    
    private void scanPubKeyURL() {
    	if (!isOnline()) {
    		AlertDialog.Builder alert = new AlertDialog.Builder(this);  
			//TODO put this into strings.xml
			alert.setTitle("No Internet Connection");  
			alert.setMessage("Sorry, in order to transmit your public key, you " +
					"need working Internet connection. Try again when you have " +
					"connectivity.");
			alert.setNeutralButton("Okay", null);
			alert.show();
			return;
    	}
    	scan();
    }

    private boolean isOnline() {
    	try {
	    	ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    	return cm.getActiveNetworkInfo().isConnectedOrConnecting();
    	} catch (NullPointerException e) {
    		//don't have network state then
    		return false;
    	}
    }
    
    private void sendPubKey(final String path) {
		if (!keysExist()) {
			//guard if keys don't exist to warn to create them
			AlertDialog.Builder alert = new AlertDialog.Builder(this);  
			//TODO put this into strings.xml
			alert.setTitle("Keypair missing");  
			alert.setMessage("The keypair does not exist. We need to create a " +
					"new keypair.");
			alert.setNeutralButton("Okay", new DialogInterface.OnClickListener() {  
			public void onClick(DialogInterface dialog, int whichButton) {  
			  generateKeys();  
			  }  
			});
			  
			alert.show();
		}
		ObjectInputStream pubkeyObjectStream=null;
		BigInteger tempM = null;
		try {
			pubkeyObjectStream = new ObjectInputStream(new BufferedInputStream(
					new FileInputStream(getFileStreamPath(PUBKEY_FILENAME))));
			tempM = (BigInteger) pubkeyObjectStream.readObject();
		} catch (Exception error) {
			Log.e("Problem in sendPubKey()", error.getMessage());
			return;
		}
		
		final BigInteger m = tempM;
		
		ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Please wait while sending public key...");
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        
        dialog.show();
        
        
        String url = path;
        Map<String, String> kvPairs = new HashMap<String, String>();

        kvPairs.put("send_key", "yes");
        kvPairs.put("pubkey", m.toString());
        try {
        	HttpResponse re = HTTPPoster.doPost(url, kvPairs);
        	int statusCode = re.getStatusLine().getStatusCode();
        	Log.v("mercury", "Sent pubkey to server, status " + statusCode);
        	mQRCodeText.setText(R.string.step2);
        } catch (ClientProtocolException e) {
        	e.printStackTrace();
        } catch (IOException e) {
        	e.printStackTrace();
        	// Do something
        }
        
        dialog.hide();

	}
    
	@SuppressWarnings("unused")
	private void mailPubKey() {
		if (!keysExist()) {
			//guard if keys don't exist to warn to create them
			AlertDialog.Builder alert = new AlertDialog.Builder(this);  
			//TODO put this into strings.xml
			alert.setTitle("Keypair missing");  
			alert.setMessage("The keypair does not exist. We need to create a " +
					"new keypair.");
			alert.setNeutralButton("Okay", new DialogInterface.OnClickListener() {  
			public void onClick(DialogInterface dialog, int whichButton) {  
			  generateKeys();  
			  }  
			});
			  
			alert.show();
		}
		ObjectInputStream pubkeyObjectStream=null;
		RSAPublicKeySpec keySpec=null;
		PublicKey pubKey = null;
		
		try {
			pubkeyObjectStream = new ObjectInputStream(new BufferedInputStream(
					new FileInputStream(getFileStreamPath(PUBKEY_FILENAME))));
			// System.out.println("got pubkey");
			BigInteger m = null;
			m = (BigInteger) pubkeyObjectStream.readObject();
			BigInteger e = null;
			e = (BigInteger) pubkeyObjectStream.readObject();
			keySpec = new RSAPublicKeySpec(m, e);
			KeyFactory fact = null;
			fact = KeyFactory.getInstance("RSA");
			pubKey = fact.generatePublic(keySpec);
		} catch (Exception e) {
			Log.e("Problem in sendPubKey()", e.getMessage());
			return;
		}
		
		final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND); 
        emailIntent.setType("plain/text"); 
        
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Mercury Public Key");
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, pubKey.toString());
        Log.v("mercury:", pubKey.toString());
        Log.v("mercury:", pubKey.getAlgorithm());
        Log.v("mercury:", pubKey.getFormat());
        startActivity(Intent.createChooser(emailIntent, "Send mail..."));
		
	}

	private void test() {
		if (!keysExist()) {
			//guard if keys don't exist to warn to create them
			AlertDialog.Builder alert = new AlertDialog.Builder(this);  
			//TODO put this into strings.xml
			alert.setTitle("Keypair missing");  
			alert.setMessage("The keypair does not exist. We need to create a " +
					"new keypair.");
			alert.setNeutralButton("Okay", new DialogInterface.OnClickListener() {  
			public void onClick(DialogInterface dialog, int whichButton) {  
			  generateKeys();  
			  }  
			});
			  
			alert.show();
		}
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		// TODO put this into strings.xml
		runningTest = true;
		alert.setTitle("Password");
		alert.setMessage("Enter the password to encrypt. A link to the QR code password " +
				"can be emailed to specified recipients on the next step.");
		final EditText input = new EditText(this);
		input.setSingleLine(true);
		input.setTransformationMethod(PasswordTransformationMethod.getInstance());
		alert.setView(input);
		alert.setNeutralButton("Okay", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				Appendable value = input.getText();
				String password = value.toString();
				//Base64Coder.encodeString(encrypted));
		        byte[] encryptedbytes=encryptQR(password.getBytes());
		        //TODO display encrypted bytes
		        String encryptedPassword = String.valueOf(Base64Coder.encode(encryptedbytes));
		        //System.out.println(encryptedPassword);
		        Log.v("test", "*********  TEST *********\nUse the following URL" +
		        		"to obtain your QR code password.");
		        //replace the two base64 characters that need to be escaped right
		        String QRCodeLink = encryptedPassword; 
		        QRCodeLink = QRCodeLink.replaceAll("\\+", "%2B");
		        QRCodeLink = QRCodeLink.replaceAll("/", "%2F");
		        QRCodeLink = QRPASS_URL + QRCodeLink;
		        Log.v("test", QRCodeLink);
		        Log.v("test", encryptedPassword);
		        Log.v("test", "********* /TEST *********");
		        final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND); 
		        emailIntent.setType("plain/text"); 
		        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Your QR Code Password Link"); 
		        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Here's the link to view the QR code of the encrypted password.\n\n" + QRCodeLink);
		        startActivity(Intent.createChooser(emailIntent, "Send mail..."));
		        //byte[] decryptedbytes=decryptQR(Base64Coder.decode(encryptedPassword.toCharArray()));
		        //System.out.println("PASSWORD IS (drum roll): " + new String(decryptedbytes));
		        //Log.v("test","PASSWORD IS (drum roll): " + new String(decryptedbytes));
		        runningTest = false;
		        mQRCodeText.setText(R.string.step3);
			}
		});
		alert.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				runningTest = false;	
			}
		});
		alert.show();
	}

	private void generate() {
	
		if(keysExist()){
			AlertDialog.Builder alert = new AlertDialog.Builder(this);  
			//TODO put this into strings.xml
			alert.setTitle("Keypair exists");  
			alert.setMessage(R.string.overWriteKeys);
			alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {  
			public void onClick(DialogInterface dialog, int whichButton) {  
			  generateKeys();  
			  }  
			});  
			  
			alert.setNegativeButton("No", null);
			  
			alert.show();
		}
		else{
			generateKeys();
		}
		mQRCodeText.setText(R.string.step1_5);
		
		
	}
	/**
	 * Method that checks if the keypair exists in the application directory
	 * @return True if both the private and public keys exist, False if one or both keys are missing
	 */
	private boolean keysExist() {
		File privkey = getFileStreamPath(PRIVKEY_FILENAME);
		File pubkey = getFileStreamPath(PUBKEY_FILENAME);
		if (privkey.exists() && pubkey.exists()){
			return true;
		}
		else{
			return false;
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode,
    			resultCode, intent);
    	if (scanResult != null && scanResult.getContents() != null) {
    		handleResult(scanResult.getContents());
    	} else if (scanResult.getContents() == null) {
    		//User backed out of Barcode Scanner (deliberately)
    		//do nothing
    	} else {
    		alertBadScan();
    	}
    }
    
    /**
     * Handle the result of a QRCode scan in whatever way that make sense.
     * @param result A String object of the scan result. Always assume a valid scan, but perhaps not a QRPass
     */
    private void handleResult(String result) {
    	//check if this is a QRPass
    	if (result.startsWith("http")) {
    		sendPubKey(result);
    	}
    	else if (!result.startsWith(QRPASS_PREFIX)) {
    		alertBadScan();
    	} else {
    		//handle the QRPass
    		result = result.replaceFirst(QRPASS_PREFIX, "");
    		//mQRCodeText.setText(result);
    		//TODO send it to decryptQR
    		byte[] decryptedbytes = null;
			try {
				decryptedbytes = decryptQR(Base64Coder.decode(result.toCharArray()));
			} catch (IllegalArgumentException e) {
				alertBadScan();
				return;
			}
			if (decryptedbytes == null) {
				alertBadScan();
			} else {
				mQRCodeText.setText("Your password is:\n" + new String(decryptedbytes));
			}
    	}
	}

	private void alertBadScan() {
		//Carson modified code from: http://www.androidsnippets.org/snippets/20/
		AlertDialog.Builder alert = new AlertDialog.Builder(this);  
		  
		alert.setTitle(R.string.badScanTitle);  
		alert.setMessage(R.string.badScanMessage);
		  
		alert.setPositiveButton(R.string.badScanYes, new DialogInterface.OnClickListener() {  
		public void onClick(DialogInterface dialog, int whichButton) {  
		  scan();  
		  }  
		});  
		  
		alert.setNegativeButton(R.string.badScanNo, null);  
		  
		alert.show();
	}
	
	private void showFancyAboutPopup() {
		AlertDialog.Builder d = new AlertDialog.Builder(this);
        d.setTitle(R.string.aboutTitle);
        d.setIcon(android.R.drawable.ic_menu_info_details);
        d.setPositiveButton(R.string.aboutGoToHomepage, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                        Uri uri = Uri.parse("http://www.ccsl.carleton.ca");
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                }
        });
        
        d.setNegativeButton(R.string.aboutClose, null);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog, null);
        TextView text = (TextView) v.findViewById(R.id.dialogText);
        text.setText(getString(R.string.aboutText, getVersion()));
        d.setView(v);
        d.show();
	}
	
	/**
     * Returns the version of the application.
     * 
     * @return The version.
     */
    private String getVersion() {
            try {
                    return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            } catch (NameNotFoundException e) {
                    // Ignore.
            }
            return "";
    }

	private void scan() {
    	IntentIntegrator.initiateScan(this, R.string.getBarcodeReaderTitle, 
    			R.string.getBarcodeReaderMessage, R.string.getBarcodeReaderYes,
    			R.string.getBarcodeReaderNo);
    }
    /**
     * Saves a key to a file
     * @param fileName
     * @param mod
     * @param exp
     * @throws IOException
     */
	public void saveToFile(String fileName, BigInteger mod, BigInteger exp) throws IOException {
    	FileOutputStream mystr = openFileOutput(fileName, MODE_PRIVATE);
    	ObjectOutputStream oout = new ObjectOutputStream(new BufferedOutputStream(mystr));
    	try {
    	    oout.writeObject(mod);
    	    oout.writeObject(exp);
    	} catch (Exception e) {
    		    throw new IOException("Unexpected error");
    	} finally {
    	    oout.close();
    	}
    }
    
    /**
     * Method that generates RSA keypair that is <code>RSA_KEY_LENGTH</code> bits long
     */
	public void generateKeys() {
    	KeyPair mykp = null;
        SecureRandom mysr = null;
   
		try {
			mysr = SecureRandom.getInstance("SHA1PRNG");
			java.security.KeyPairGenerator mykpg = java.security.KeyPairGenerator
					.getInstance("RSA");
			mykpg.initialize(RSA_KEY_LENGTH, mysr);

			mykp = mykpg.generateKeyPair();
			KeyFactory fact = KeyFactory.getInstance("RSA");
			RSAPublicKeySpec pub = fact.getKeySpec(mykp.getPublic(),
					RSAPublicKeySpec.class);
			RSAPrivateKeySpec priv = fact.getKeySpec(mykp.getPrivate(),
					RSAPrivateKeySpec.class);

			saveToFile(PUBKEY_FILENAME, pub.getModulus(), pub
					.getPublicExponent());
			saveToFile(PRIVKEY_FILENAME, priv.getModulus(), priv
					.getPrivateExponent());
			mQRCodeText.setText(R.string.step1_5);
			// System.out.println(mykp.getPrivate());
			// System.out.println(mykp.getPublic());
			//mQRCodeText.setText("Public: " + mykp.getPublic() + "\n\nPrivate: " + mykp.getPrivate());

		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		 
    }
    
	public byte[] decryptQR(byte[] qrBytes) {
    	ObjectInputStream privkey=null;
    	byte[] cipherData=null;
		if (keysExist()) {
			try {
				privkey = new ObjectInputStream(new BufferedInputStream(new FileInputStream(getFileStreamPath(PRIVKEY_FILENAME))));
				BigInteger m=null;
				m = (BigInteger) privkey.readObject();
				BigInteger e = null;
				e = (BigInteger) privkey.readObject();
				RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(m, e);
	    	    KeyFactory fact = null;
	    	    fact = KeyFactory.getInstance("RSA");
	    	    PrivateKey privKey = null;
	    	    privKey = fact.generatePrivate(keySpec);
	    	    Cipher cipher=null;
	    	    cipher = Cipher.getInstance("RSA/NONE/PKCS1Padding");
	    	    cipher.init(Cipher.DECRYPT_MODE, privKey);
	    	    cipherData = cipher.doFinal(qrBytes);
			} catch (Exception e) {
				Log.e("Problem in decryptQR()", e.getMessage());
			}
    	}
    	return cipherData;
    	//TODO keys don't exist. can't decrypt
	}

	public byte[] encryptQR(byte[] qrBytes) {
    	ObjectInputStream pubKeyObjectStream=null;
    	byte[] cipherData=null;
    	if(keysExist()){
			try {
				pubKeyObjectStream = new ObjectInputStream(new BufferedInputStream(
						new FileInputStream(getFileStreamPath(PUBKEY_FILENAME))));
				// System.out.println("got pubkey");
				BigInteger m = null;
				m = (BigInteger) pubKeyObjectStream.readObject();
				BigInteger e = null;
				e = (BigInteger) pubKeyObjectStream.readObject();
				RSAPublicKeySpec keySpec = new RSAPublicKeySpec(m, e);
				KeyFactory fact = null;
				fact = KeyFactory.getInstance("RSA");
				PublicKey pubKey = null;
				pubKey = fact.generatePublic(keySpec);
				Cipher cipher = null;
				cipher = Cipher.getInstance("RSA/NONE/PKCS1Padding");
				cipher.init(Cipher.ENCRYPT_MODE, pubKey);
				cipherData = cipher.doFinal(qrBytes);
			} catch (Exception e) {
				Log.e("Problem in encryptQR()", e.getMessage());
			}
    		
    	}
    	//String encrypted=new String(cipherData);
    	//System.out.println(encrypted);
    	return cipherData;
    	//TODO keys don't exist. can't decrypt
    }
}