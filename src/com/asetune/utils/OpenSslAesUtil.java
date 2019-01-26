/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.PropertyConfigurator;
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

	public static class DecryptionException
	extends Exception
	{
		public DecryptionException(String message, Throwable cause)
		{
			super(message, cause);
		}
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
//		// openssl non-standard extension: salt embedded at start of encrypted file
////		byte[] salt = Arrays.copyOfRange(src, 8, 16); // 0..7 is "SALTED__", 8..15 is the salt
//		byte[] salt = new byte[] {};
//
//		try
//		{
//			BlockCipherPadding padding = new PKCS7Padding();
//			BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), padding);
//			
//			CipherParameters params = getCipherParameters(pwd, salt);
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
	throws DecryptionException
	{
		OpenSslAesUtil decrypter = new OpenSslAesUtil(128);

		byte[] ba = decrypter.decipher(passwd.getBytes(), Base64.decodeBase64(base64Str));
		return new String(ba).trim();
	}

	public byte[] decipher(byte[] pwd, byte[] src)
	throws DecryptionException
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
			throw new DecryptionException("Decryption failed, Probably wrong passphrase. Caught: " + e, e);

			//System.err.println("Error: Decryption failed, Caught: " + e);
			//return null;
		}
		catch (RuntimeException e)
		{
			throw new DecryptionException("Decryption failed, Caught: " + e, e);
			//System.err.println("Error: Decryption failed, Caught: " + e);
			//return null;
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

	
	public static String readPasswdFromFile(String user)
	throws IOException, DecryptionException
	{
		return readPasswdFromFile(user, null, null, null);
	}
	public static String readPasswdFromFile(String user, String serverName)
	throws IOException, DecryptionException
	{
		return readPasswdFromFile(user, serverName, null, null);
	}
	public static String readPasswdFromFile(String user, String serverName, String filename)
	throws IOException, DecryptionException
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
	throws IOException, DecryptionException
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
			String fallbackEncPasswd = null;
			String srvMatchEncPasswd = null;

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
								srvMatchEncPasswd = fEncPasswd;
						}
						else
						{
							fallbackEncPasswd = fEncPasswd;
						}
					}
				}
			}
			
			String rawEncryptedStr = null;

			// Generic password *without* server specification (use this as a FALLBACK)
			// entry looking like |sa:encryptedPasswd|
			if (fallbackEncPasswd != null)
				rawEncryptedStr = fallbackEncPasswd;

			// password WITH server specification
			// entry looking like |sa:PROD_A_ASE:encryptedPasswd|
			if (srvMatchEncPasswd != null) 
				rawEncryptedStr = srvMatchEncPasswd;

			// DECODE the rawEncryptedStr
			if (rawEncryptedStr != null)
			{
				// First try with the supplied passphrase
				try
				{
					return decode(encPasswd, rawEncryptedStr);
				}
				catch(DecryptionException originEx)
				{
					// Second try with the "current user" as the passphrase
					try
					{
						String userPassphrase = System.getProperty("user.name");
						return decode(userPassphrase, rawEncryptedStr);
					}
					catch(DecryptionException ex)
					{
						throw originEx;
					}
				}
			}

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

	
	
	
	
	public static void main(String[] args)
	{
		Properties log4jProps = new Properties();
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
//		log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

//		OpenSslAesUtil d = new OpenSslAesUtil(128);
//		String r = new String(d.decipher("mypassword".getBytes(), Base64.decodeBase64("U2FsdGVkX187CGv6DbEpqh/L6XRKON7uBGluIU0nT3w=")));
//		System.out.println(r);
//
//		r = new String(d.decipher("sybase".getBytes(), Base64.decodeBase64("U2FsdGVkX1/2chgTZtP5+b30Hwv1n2prE5CqtWcoH8A=")));
//		System.out.println(r);
		try { System.out.println("decrypted=|" + decode("sybase", "U2FsdGVkX1/2chgTZtP5+b30Hwv1n2prE5CqtWcoH8A=") + "|"); }
		catch(Exception e) { e.printStackTrace(); }
		

		try { System.out.println("readFromFile=|" + readPasswdFromFile("sa", null, null, null) + "|"); }
		catch(Exception e) { e.printStackTrace(); }
		
		try { System.out.println("x1-ok="   + decode("sybase", "U2FsdGVkX1/foj2pv2V24rLfl7RLdcMGdd8jaTngzns=")); } catch(Exception e) { e.printStackTrace(); }
		try { System.out.println("x2-fail=" + decode("sybase", "U2FsdGVkX1+4mSAv8/x8TRYx8wPrWUovDh8HBY16ZTY=")); } catch(Exception e) { e.printStackTrace(); }
		try { System.out.println("x3-ok="   + decode("sysopr", "U2FsdGVkX1+4mSAv8/x8TRYx8wPrWUovDh8HBY16ZTY=")); } catch(Exception e) { e.printStackTrace(); }
		
	}
}
