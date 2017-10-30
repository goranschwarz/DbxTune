package com.asetune.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

// http://moi.vonos.net/java/symmetric-encryption-openssl-bc/

import java.util.Arrays;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.generators.OpenSSLPBEParametersGenerator;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.BlockCipherPadding;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;

public class OpenSslAesUtil
{
	private static final int AES_NIVBITS = 128; // CBC Initialization Vector (same as cipher block size) [16 bytes]

	private final int keyLenBits;

	public OpenSslAesUtil(int nKeyBits)
	{
		this.keyLenBits = nKeyBits;
	}

//	public static String encrypt(String key, String toEncrypt) throws Exception
//	{
//		Key skeySpec = generateKeySpec(key);
//		Cipher cipher = Cipher.getInstance("AES");
//		cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
//		byte[] encrypted = cipher.doFinal(toEncrypt.getBytes());
//		byte[] encryptedValue = Base64.encodeBase64(encrypted);
//		return new String(encryptedValue);
//	}
	
//	public static String encode(String passwd, String sourceString)
//	{
//		OpenSslAesUtil crypter = new OpenSslAesUtil(128);
//
//		byte[] ba = crypter.encipher(passwd.getBytes(), sourceString);
//		return Base64.encodeBase64String(ba);
//	}
//
//	private byte[] encipher(byte[] pwd, String sourceString)
//	{
//		try
//		{
//			BlockCipherPadding padding = new PKCS7Padding();
//			BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), padding);
//			
//			cipher.reset();
//			cipher.init(false, params);
//		}
//		catch (Exception e) 
//		{
//			e.printStackTrace();
//		}
//		return null;
//	}


	
	
	public static String decode(String passwd, String base64Str)
	{
		OpenSslAesUtil decrypter = new OpenSslAesUtil(128);

		byte[] ba = decrypter.decipher(passwd.getBytes(), Base64.decodeBase64(base64Str));
		return new String(ba).trim();
	}

	public byte[] decipher(byte[] pwd, byte[] src)
	{
		// openssl non-standard extension: salt embedded at start of encrypted file
		byte[] salt = Arrays.copyOfRange(src, 8, 16); // 0..7 is "SALTED__", 8..15 is the salt

		try
		{
			// Encryption algorithm. Note that the "strength" (bitsize) is controlled by the key object that is used.
			// Note that PKCS5 padding and PKCS7 padding are identical.
			BlockCipherPadding padding = new PKCS7Padding();
			BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), padding);

			CipherParameters params = getCipherParameters(pwd, salt);
			cipher.reset();
			cipher.init(false, params);

			int buflen = cipher.getOutputSize(src.length - 16);
			byte[] workingBuffer = new byte[buflen];
			int len = cipher.processBytes(src, 16, src.length - 16, workingBuffer, 0);
			len += cipher.doFinal(workingBuffer, len);

			// Note that getOutputSize returns a number which includes space for "padding" bytes to be stored in.
			// However we don't want these padding bytes; the "len" variable contains the length of the *real* data
			// (which is always less than the return value of getOutputSize.
			byte[] bytesDec = new byte[len];
			System.arraycopy(workingBuffer, 0, bytesDec, 0, len);
			return bytesDec;
		}
		catch (InvalidCipherTextException e)
		{
			System.err.println("Error: Decryption failed, Caught: " + e);
			return null;
		}
		catch (RuntimeException e)
		{
			System.err.println("Error: Decryption failed, Caught: " + e);
			return null;
		}
	}

	private CipherParameters getCipherParameters(byte[] pwd, byte[] salt)
	{
		// Use bouncycastle implementation of openssl non-standard (pwd,salt)->(key,iv) algorithm.
		// Note that if a "CBC" cipher is selected, then an IV is required as well as a key. When using a password,
		// Openssl *derives* the IV from the (pwd,salt) pair at the same time as it derives the key.
		//
		// * PBE = Password Based Encryption
		// * CBC = Cipher Block Chaining (ie IV is needed)
		//
		// Note also that when the IV is derived from (pwd, salt) the salt **must** be different for each message; this is
		// the default for openssl - just make sure to NOT explicitly provide a salt, or encryption security is badly affected.
		OpenSSLPBEParametersGenerator gen = new OpenSSLPBEParametersGenerator();
		gen.init(pwd, salt);
		CipherParameters cp = gen.generateDerivedParameters(keyLenBits, AES_NIVBITS);
		return cp;
	}

	public static void main(String[] args)
	{
//		OpenSslAesUtil d = new OpenSslAesUtil(128);
//		String r = new String(d.decipher("mypassword".getBytes(), Base64.decodeBase64("U2FsdGVkX187CGv6DbEpqh/L6XRKON7uBGluIU0nT3w=")));
//		System.out.println(r);
//
//		r = new String(d.decipher("sybase".getBytes(), Base64.decodeBase64("U2FsdGVkX1/2chgTZtP5+b30Hwv1n2prE5CqtWcoH8A=")));
//		System.out.println(r);
		System.out.println("decrypted=|" + decode("sybase", "U2FsdGVkX1/2chgTZtP5+b30Hwv1n2prE5CqtWcoH8A=") + "|");

		try
		{
			System.out.println("readFromFile=|" + readPasswdFromFile("sa", null, null, null) + "|");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	
	public static String readPasswdFromFile(String user)
	throws Exception
	{
		return readPasswdFromFile(user, null, null, null);
	}
	public static String readPasswdFromFile(String user, String serverName)
	throws Exception
	{
		return readPasswdFromFile(user, serverName, null, null);
	}
	public static String readPasswdFromFile(String user, String serverName, String filename)
	throws Exception
	{
		return readPasswdFromFile(user, serverName, filename, null);
	}

	/**
	 * Get encrypted password from file<br>
	 * <br>
	 * Do the folowing (on Linux) to genereate a password: <br>
	 * <pre>
	 * echo "abcdefg" | openssl enc -aes-128-cbc -a -salt -pass pass:12345
	 * </pre>
	 * File format
	 * <pre>
	 * username: encryptedPassword
	 * username: [ASE_SRV_NAME:] encryptedPassword
	 * </pre>
	 * Example:
	 * <pre>
	 * sa: U2FsdGVkX1888FrszgUsxQLYHxO1jPxqxKQ2RXC0GyU=
	 * sa: PROD_A_ASE: U2FsdGVkX1+99lkmyM9xujXxZYB3pp+QMHcrqeAigi8=
	 * dbdump: U2FsdGVkX1/tdOQuoA2LGULOtTDrMbTXxeiW+nqjs88=
	 * </pre>
	 * 
	 * @param user        the username we want to get password for
	 * @param serverName  the ASE Servername we want to get password for
	 * @param filename    The file holding the password (null = ~/.passwd.enc)
	 * @param encPasswd   The passprase you used when generating the password (null = "sybase") 
	 * @return The encrypted password, null = password wasn't found
	 * @throws IOException for example if the password file didn't exist or that we had problems reading the file
	 */
	public static String readPasswdFromFile(String user, String serverName, String filename, String encPasswd)
	throws IOException
	{

		// If no password was passed: use "sybase" or get from property
		if (encPasswd == null)
			encPasswd = Configuration.getCombinedConfiguration().getProperty("OpenSslAesUtil.readPasswdFromFile.encyption.password", "sybase");

		// If no filename was passed: use "~/.passwd.enc" or get from property
		if (filename == null)
			filename = getPasswordFilename();

		// Read the file
		File f = new File(filename);
		if (f.exists())
		{
			String xxxEncPasswd = null;
			String srvEncPasswd = null;

			List<String> lines = Files.readAllLines(Paths.get(filename), Charset.forName("UTF-8"));
			for (String line : lines)
			{
				String[] sa = line.split(":");
				if (sa.length >= 2)
				{
					boolean srvEntry = false;
					String fUser      = null;
					String fServer    = null;
					String fEncPasswd = null;
					
					if (sa.length == 2)
					{
						srvEntry   = false;
						fUser      = sa[0].trim();
						fServer    = null;
						fEncPasswd = sa[1].trim();
					}
					else
					{
						srvEntry   = true;
						fUser      = sa[0].trim();
						fServer    = sa[1].trim();
						fEncPasswd = sa[2].trim();
					}
					
					if (user.equals(fUser))
					{
						if (srvEntry)
						{
							if (serverName != null && serverName.equals(fServer))
								srvEncPasswd = fEncPasswd;
						}
						else
						{
							xxxEncPasswd = fEncPasswd;
						}
					}
				}
			}
			
			// entry looking like |sa:PROD_A_ASE:encryptedPasswd|
			if (srvEncPasswd != null)
				return decode(encPasswd, srvEncPasswd);

			// entry looking like |sa:encryptedPasswd|
			if (xxxEncPasswd != null)
				return decode(encPasswd, xxxEncPasswd);

			// Nothing was found...
			return null;
		}
		else
		{
			throw new FileNotFoundException("The password file '"+f+"' didn't exist.");
		}
	}

	public static String getPasswordFilename()
	{
		String homeDir = System.getProperty("user.home");
		String defFilename = homeDir + File.separatorChar + ".passwd.enc";

		return Configuration.getCombinedConfiguration().getProperty("OpenSslAesUtil.readPasswdFromFile.filename", defFilename);
	}
}