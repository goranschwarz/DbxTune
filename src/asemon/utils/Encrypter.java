/**
 * Basically copy paste from the net
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.utils;

import java.io.UnsupportedEncodingException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

public class Encrypter
{
	private Cipher ecipher;
	private Cipher dcipher;

	// 8-byte Salt
	byte[] salt = { (byte)0xA9, (byte)0x9B, (byte)0xC8, (byte)0x32, (byte)0x56, (byte)0x35, (byte)0xE3, (byte)0x03 };

	// Iteration count
	int iterationCount = 19;

	public Encrypter(SecretKey key)
	{
		try
		{
//			ecipher = Cipher.getInstance("DES");
//			dcipher = Cipher.getInstance("DES");
			ecipher = Cipher.getInstance("TripleDES");
			dcipher = Cipher.getInstance("TripleDES");
			
			ecipher.init(Cipher.ENCRYPT_MODE, key);
			dcipher.init(Cipher.DECRYPT_MODE, key);

		}
		catch (javax.crypto.NoSuchPaddingException e)    {}
		catch (java.security.NoSuchAlgorithmException e) {}
		catch (java.security.InvalidKeyException e)      {}
	}

	public Encrypter(String passPhrase)
	{
		try
		{
			// Create the key
			KeySpec keySpec = new PBEKeySpec(passPhrase.toCharArray(), salt, iterationCount);
			SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);
			ecipher = Cipher.getInstance(key.getAlgorithm());
			dcipher = Cipher.getInstance(key.getAlgorithm());

			// Prepare the parameter to the ciphers
			AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt, iterationCount);

			// Create the ciphers
			ecipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
			dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
		}
		catch (java.security.InvalidAlgorithmParameterException e) {}
		catch (java.security.spec.InvalidKeySpecException e)       {}
		catch (javax.crypto.NoSuchPaddingException e)              {}
		catch (java.security.NoSuchAlgorithmException e)           {}
		catch (java.security.InvalidKeyException e)                {}
	}
	public String encrypt(String str)
	{
		try
		{
			// Encode the string into bytes using utf-8
			byte[] utf8 = str.getBytes("UTF8");

			// Encrypt
			byte[] enc = ecipher.doFinal(utf8);

			// Encode bytes to base64 to get a string
			// A ALTERNATE FOR 'sun.misc.BASE64Encoder' can be http://commons.apache.org/codec/download_codec.cgi
			return new sun.misc.BASE64Encoder().encode(enc);
		}
		catch (javax.crypto.BadPaddingException e) {}
		catch (IllegalBlockSizeException e)        {}
		catch (UnsupportedEncodingException e)     {}
		return null;
	}

	public String decrypt(String str)
	{
		try
		{
			// Decode base64 to get bytes
			// A ALTERNATE FOR 'sun.misc.BASE64Decoder' can be http://commons.apache.org/codec/download_codec.cgi
			byte[] dec = new sun.misc.BASE64Decoder().decodeBuffer(str);

			// Decrypt
			byte[] utf8 = dcipher.doFinal(dec);

			// Decode using utf-8
			return new String(utf8, "UTF8");
		}
		catch (javax.crypto.BadPaddingException e) {}
		catch (IllegalBlockSizeException e)        {}
		catch (UnsupportedEncodingException e)     {}
		catch (java.io.IOException e)              {}
		return null;
	}

	// Here's an example that uses the class:
	public static void main(String[] args)
	{
		try
		{
			// Generate a temporary key. In practice, you would save this key.
			// See also e464 Encrypting with DES Using a Pass Phrase.
//			SecretKey key = KeyGenerator.getInstance("DES").generateKey();
//			System.out.println("key='"+key+"'.");
//			// Create encrypter/decrypter class
//			Encrypter encrypter = new Encrypter(key);

			// Create encrypter/decrypter class
			Encrypter encrypter = new Encrypter("qazZSE44wsxXDR55");

			// Encrypt
//			String inStr     = "123";
//			String inStr     = "sybase12";
			String inStr     = "a string that is a bit long";
			String encrypted = encrypter.encrypt(inStr);
			System.out.println("encrypted='"+encrypted+"', source='"+inStr+"'.");

			// Decrypt
			String decrypted = encrypter.decrypt(encrypted);
			System.out.println("encrypted='"+encrypted+"', afterDecrypt='"+decrypted+"'.");
		}
		catch (Exception e)
		{
		}
	}
}
