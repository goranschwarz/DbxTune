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
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;

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

import com.asetune.Version;

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
		private static final long serialVersionUID = 1L;

		public DecryptionException(String message, Throwable cause)
		{
			super(message, cause);
		}
	}

	/**
	 * Encode a string<br>
	 * using same method as os command:<br>
	 * <code>echo "secretPasswordToEncrypt" | openssl enc -aes-128-cbc -a -salt -pass pass:sybase</code>
	 * 
	 * @param passwd
	 * @param sourceStringToEncode
	 * @return
	 * @throws DecryptionException
	 */
	public static String encode(String passwd, String sourceStringToEncode)
	throws DecryptionException
	{
		OpenSslAesUtil crypter = new OpenSslAesUtil(128);

		byte[] ba = crypter.encipher(passwd.getBytes(), sourceStringToEncode);
		return Base64.encodeBase64String(ba);
	}

	private byte[] encipher(byte[] pwd, String sourceString)
	throws DecryptionException
	{
		// openssl non-standard extension: salt embedded at start of encrypted file
		String saltedMagic = "Salted__";

		final byte[] salt = (new SecureRandom()).generateSeed(8);


		byte[] src = sourceString.getBytes();

		try
		{
			BlockCipherPadding padding = new PKCS7Padding();
			BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), padding);
			
			CipherParameters params = getCipherParameters(pwd, salt);
			cipher.reset();
			cipher.init(true, params); // true == ENCRYPT

			int buflen = cipher.getOutputSize(src.length);
			byte[] workingBuffer = new byte[buflen];
			int len = cipher.processBytes(src, 0, src.length, workingBuffer, 0);
			len += cipher.doFinal(workingBuffer, len);

			// Note that getOutputSize returns a number which includes space for "padding" bytes to be stored in.
			// However we don't want these padding bytes; the "len" variable contains the length of the *real* data
			// (which is always less than the return value of getOutputSize.
			byte[] bytesEnc = new byte[len];
			System.arraycopy(workingBuffer, 0, bytesEnc, 0, len);
			
			//return bytesDec;
			
			// compose the final output: Salted__########????????????????????????
			//                           ^^^^^^^^                                  (length=8)
			//                           Magic   ^^^^^^^^                          (length=8)
			//                                   seed    ^^^^^^^^^^^^^^^^^^^^^^^^  (unknown-length)
			//                                           encrypted data            
			byte[] openSslOutArr = new byte[len + 16];
			System.arraycopy(saltedMagic.getBytes(), 0, openSslOutArr, 0, 8);                // byte 0-8  are "Salted__"
			System.arraycopy(salt,                   0, openSslOutArr, 8, 8);                // byte 8-16 are the generated seed number
			System.arraycopy(bytesEnc,               0, openSslOutArr, 16, bytesEnc.length); // byte 16-? are the encrypted data

			return openSslOutArr;
		}
		catch (Exception e) 
		{
			throw new DecryptionException("Decryption failed, Caught: " + e, e);
		}
	}


	

	/**
	 * Decode a String that has been encoded with os command openssl<br>
	 * This is the same as doing the following os command:<br>
	 * <code>echo 'U2FsdGVkX1+1dw6kuaMMFu4WkNLM1Cw09OYYWC5vRa4=' | openssl enc -aes-128-cbc -a -d -salt -pass pass:sybase</code>
	 * 
	 * @param passwd
	 * @param base64Str
	 * @return
	 * @throws DecryptionException
	 */
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
			cipher.init(false, params); // false == DECRYPT

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

	private static String getDefaultPassPhrase()
	{
		String passPhrase = Configuration.getCombinedConfiguration().getProperty("OpenSslAesUtil.readPasswdFromFile.encyption.password");

		if (passPhrase == null && "SqlServerTune".equals(Version.getAppName())) 
			passPhrase = "mssql";

		if (passPhrase == null)
			passPhrase = "sybase";
			
		return passPhrase;
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
	 * Do the following (on Linux) to generate a password: <br>
	 * <pre>
	 * echo "secretPasswordToEncrypt" | openssl enc -aes-128-cbc -a -salt -pass pass:sybase
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

		// If no password was passed: get from property, or use the user name
		if (encPasswd == null)
			encPasswd = getDefaultPassPhrase();

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

	

	//-------------------------------
	// WRITE
	//-------------------------------
	public static void writePasswdToFile(String clearPassword, String user)
	throws IOException, DecryptionException
	{
		writePasswdToFile(clearPassword, user, null, null, null);
	}
	
	public static void writePasswdToFile(String clearPassword, String user, String serverName)
	throws IOException, DecryptionException
	{
		writePasswdToFile(clearPassword, user, serverName, null, null);
	}
	
	public static void writePasswdToFile(String clearPassword, String user, String serverName, String filename)
	throws IOException, DecryptionException
	{
		writePasswdToFile(clearPassword, user, serverName, filename, null);
	}

	//-------------------------------
	// REMOVE
	//-------------------------------
	public static boolean removePasswdFromFile(String user)
	throws IOException, DecryptionException
	{
		return writePasswdToFile("not-used", user, null, null, null, true);
	}
	
	public static boolean removePasswdFromFile(String user, String serverName)
	throws IOException, DecryptionException
	{
		return writePasswdToFile("not-used", user, serverName, null, null, true);
	}
	
	public static boolean removePasswdFromFile(String user, String serverName, String filename)
	throws IOException, DecryptionException
	{
		return writePasswdToFile("not-used", user, serverName, filename, null, true);
	}

	/**
	 * Write a password as an encrypted string to a file<br>
	 * <br>
	 * This will emulate openssl encryption (in java) <br>
	 * <pre>
	 * echo "secretPasswordToEncrypt" | openssl enc -aes-128-cbc -a -salt -pass pass:sybase
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
	 * @param clearPassword  the password you want to save in the file (it will be encrypted before writing it)
	 * @param user           the username we want to write password for
	 * @param serverName     the ASE Servername we want to write password for (can be null, then it will be a generic password for all files)
	 * @param filename       The file holding the password (null = ~/.passwd.enc)
	 * @param encPasswd      The passprase you used when generating the password (null = "sybase") 
	 *
	 * @throws IOException for example if the password file didn't exist or that we had problems writing to the file
	 */
	public static void writePasswdToFile(String clearPassword, String user, String serverName, String filename, String encPasswd)
	throws IOException, DecryptionException
	{
		writePasswdToFile(clearPassword, user, serverName, filename, encPasswd, false);
	}

	/**
	 * Write a password as an encrypted string to a file<br>
	 * <br>
	 * This will emulate openssl encryption (in java) <br>
	 * <pre>
	 * echo "secretPasswordToEncrypt" | openssl enc -aes-128-cbc -a -salt -pass pass:sybase
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
	 * @param clearPassword  the password you want to save in the file (it will be encrypted before writing it)
	 * @param user           the username we want to write password for
	 * @param serverName     the ASE Servername we want to write password for (can be null, then it will be a generic password for all files)
	 * @param filename       The file holding the password (null = ~/.passwd.enc)
	 * @param encPasswd      The passprase you used when generating the password (null = "sybase") 
	 * @param remove         true if we want to delete the entry 
	 *
	 * @return (only used if remove is true) - true if the entry was found and removed, false if the entry wasn't found
	 * @throws IOException for example if the password file didn't exist or that we had problems writing to the file
	 */
	private static boolean writePasswdToFile(String clearPassword, String user, String serverName, String filename, String encPasswd, boolean remove)
	throws IOException, DecryptionException
	{
		// If no password was passed: use "sybase" or get from property
		if (encPasswd == null)
			encPasswd = getDefaultPassPhrase();

		// If no filename was passed: use "~/.passwd.enc" or get from property
		if (filename == null)
			filename = getPasswordFilename();

//FIXME: This hasn't yet been tested
		// Encrypt the clear password
		String encrypedPassword = encode(encPasswd, clearPassword);
		
		// Read the file
		File f = new File(filename);
		if (f.exists())
		{
			String sep = ": ";

			String fallbackEncPasswd = null;
			String srvMatchEncPasswd = null;

			int srvEntryFoundAtIndex      = -1;
			int fallbackEntryFoundAtIndex = -1;

			List<String> lines = Files.readAllLines(Paths.get(filename), Charset.forName("UTF-8"));
			int lineNum = -1;
			for (String line : lines)
			{
				lineNum++;

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
							{
								srvMatchEncPasswd    = fEncPasswd;
								srvEntryFoundAtIndex = lineNum;
							}
						}
						else
						{
							fallbackEncPasswd         = fEncPasswd;
							fallbackEntryFoundAtIndex = lineNum;
						}
					}
				}
			}
			
			// Used if we are removing/deleting an entry
			boolean removedEntry = false;
			int entryFoundAtIndex = -1;

			// If we have specified a serverName
			if (serverName != null)
			{
				if (srvEntryFoundAtIndex != -1) 
					entryFoundAtIndex = srvEntryFoundAtIndex;
			}
			else // this is a "fallback entry"... NO serverName has been passed
			{
				if (fallbackEntryFoundAtIndex != -1)
					entryFoundAtIndex = fallbackEntryFoundAtIndex;
			}

			// Update current entry
			if (entryFoundAtIndex != -1)
			{
				// password WITH server specification
				// entry looking like |sa:PROD_A_ASE:encryptedPasswd|
				// UPDATE/CHANGE the password
				if (serverName != null)
				{
					if (remove)
					{
						lines.remove(entryFoundAtIndex);
						removedEntry = true;
					}
					else
					{
						String newEntry = user + sep + serverName + sep + encrypedPassword;
						lines.set(entryFoundAtIndex, newEntry);
					}
				}

				// Generic password *without* server specification (use this as a FALLBACK)
				// entry looking like |sa:encryptedPasswd|
				// UPDATE/CHANGE the password
				else
				{
					if (remove)
					{
						lines.remove(entryFoundAtIndex);
						removedEntry = true;
					}
					else
					{
						String newEntry = user + sep + encrypedPassword;
						lines.set(entryFoundAtIndex, newEntry);
					}
				}
			}
			else // The user/server was NOT FOUND at all in the file: simply add it.
			{
				// Short or Long entry...
				String newEntry = user + sep + encrypedPassword;
				if (serverName != null)
					newEntry = user + sep + serverName + sep + encrypedPassword;
				
				lines.add(newEntry);
			}

			// Now write the file back again
			Files.write(Paths.get(filename), lines, Charset.forName("UTF-8"), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

			return removedEntry;
		}
		else
		{
			throw new FileNotFoundException("The password file '"+f+"' didn't exist.");
		}
	}

	/**
	 * Create the passwd file if it doesn't already exists
	 * 
	 * @param filename      null = use the default file ~/.passwd.enc
	 * @return true if the named file does not exist and was successfully created; false if the named file already exists
	 * @throws IOException         If an I/O error occurred
	 * @throws SecurityException   If a security manager exists and its SecurityManager.checkWrite(java.lang.String) method denies write access to the file
	 */
	public static boolean createPasswordFilenameIfNotExists(String filename) 
	throws IOException
	{
		if (filename == null)
			filename = getPasswordFilename();

		// 
		File f = new File(filename);
		return f.createNewFile();
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

		
		try
		{
			String txt_abc123 = "abc123";
			System.out.println("Origin String to be encoded=|" + txt_abc123 +"|.");

			String enc_abc123 = encode("sybase", txt_abc123);
			System.out.println("Encoded=|" + enc_abc123 +"|.");

			String dec_abc123 = decode("sybase", enc_abc123);
			System.out.println("Decoded=|" + dec_abc123 +"|.");
			
			if (txt_abc123.equals(dec_abc123))
				System.out.println("decode = SUCCESS");
			else
				System.out.println("decode = ------ FAILED ------");
		}
		catch(Exception ex) { ex.printStackTrace(); }
		System.out.println();
		
		
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
