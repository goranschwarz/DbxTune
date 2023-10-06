/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;

// http://moi.vonos.net/java/symmetric-encryption-openssl-bc/

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;
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
	private static Logger _logger = Logger.getLogger(OpenSslAesUtil.class);

	private static final int AES_NIVBITS = 128; // CBC Initialization Vector (same as cipher block size) [16 bytes]

//	public static final String VERSION_PREFIX_V1 = "";
	public static final String VERSION_PREFIX_V2 = "v2;";

	public static final int    VERSION_1         = 1;
	public static final int    VERSION_2         = 2;
	
	public static final int    DEFAULT_VERSION        = 2;
	public static final String DEFAULT_VERSION_PREFIX = VERSION_PREFIX_V2;

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

//	public static class UserNotFoundException
//	extends Exception
//	{
//		private static final long serialVersionUID = 1L;
//
//		public UserNotFoundException(String user, String server, String filename)
//		{
//			super("The Username'" + user + "', with server '" + server + "' was not found in file '" + filename + "'.");
//		}
//	}

	/**
	 * Encode a string<br>
	 * using same method as os command:<br>
	 * <code>echo "secretPasswordToEncrypt" | openssl enc -aes-128-cbc -a -salt -pass pass:sybase</code>
	 * Or in OpenSSL Version 1.1.1
	 * <code>echo "secretPasswordToEncrypt" | openssl enc -base64 -aes-256-cbc -pbkdf2 -iter 100000 -k sybase</code>
	 * 
	 * @param passwd
	 * @param sourceStringToEncode
	 * @return
	 * @throws DecryptionException
	 */
	public static String encode(String passwd, String sourceStringToEncode, int version)
	throws DecryptionException
	{
		if (version <= 1)
		{
			OpenSslAesUtil crypter = new OpenSslAesUtil(128);

			byte[] ba = crypter.encipher_v1(passwd.getBytes(), sourceStringToEncode);
			return Base64.encodeBase64String(ba);
		}
		else if (version == 2)
		{
			OpenSslAesUtil crypter = new OpenSslAesUtil(256);

			byte[] ba = crypter.encipher_v2(passwd, sourceStringToEncode);
			return DEFAULT_VERSION_PREFIX + Base64.encodeBase64String(ba);
		}
		else
		{
			throw new DecryptionException("Encryption with Version " + version + " is not supported.", null);
		}
	}

	private byte[] encipher_v1(byte[] pwd, String sourceString)
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
			throw new DecryptionException("Encryption failed, Caught: " + e, e);
		}
	}

	/**
	 * TODO: Cleanup this code... It's ugly (more or less copied from decipher_v2() which was grabbed from stackoverflow... I'm not sure even I understands it correctly)
	 */
//	private byte[] encipher_v2(String password, String sourceString)
//	throws DecryptionException
//	{
//		String saltedMagic = "Salted__";
//
//		final byte[] salt = (new SecureRandom()).generateSeed(8);
//
//
//		byte[] src = sourceString.getBytes();
//
//		try
//		{
//			int keylen = 32;
//			int ivlen = 16;
//			
//			byte[] keyAndIV = SecretKeyFactory.getInstance("PBKDF2withHmacSHA256").generateSecret(new PBEKeySpec(password.toCharArray(), salt, 100_000, (keylen + ivlen) * 8)).getEncoded();
//			Cipher cipher   = Cipher.getInstance("AES/CBC/PKCS5Padding");
//			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyAndIV, 0, keylen, "AES"), new IvParameterSpec(keyAndIV, keylen, ivlen));
//			
//			byte output[] = null;
//
//			try (InputStream is = new ByteArrayInputStream(src))
//			{
//				int cnt;
//				byte tmp[];
//				byte[] data = new byte[1024];
//
//				while ((cnt = is.read(data)) > 0)
//				{
//					if ( (tmp = cipher.update(data, 0, cnt)) != null )
//					{
//						output = ArrayUtils.addAll(output, tmp);
//					}
//				}
//				tmp = cipher.doFinal();
//				output = ArrayUtils.addAll(output, tmp);
//			}
//
//
//			int len = output.length;
//			byte[] bytesEnc = new byte[len];
//			System.arraycopy(output, 0, bytesEnc, 0, len);
//			
//			// compose the final output: Salted__########????????????????????????
//			//                           ^^^^^^^^                                  (length=8)
//			//                           Magic   ^^^^^^^^                          (length=8)
//			//                                   seed    ^^^^^^^^^^^^^^^^^^^^^^^^  (unknown-length)
//			//                                           encrypted data            
//			byte[] openSslOutArr = new byte[len + 16];
//			System.arraycopy(saltedMagic.getBytes(), 0, openSslOutArr, 0, 8);                // byte 0-8  are "Salted__"
//			System.arraycopy(salt,                   0, openSslOutArr, 8, 8);                // byte 8-16 are the generated seed number
//			System.arraycopy(bytesEnc,               0, openSslOutArr, 16, bytesEnc.length); // byte 16-? are the encrypted data
//
//			return openSslOutArr;
//		}
//		catch (Exception e) 
//		{
//			throw new DecryptionException("Encryption failed, Caught: " + e, e);
//		}
////		throw new DecryptionException("encipher_v2 -- NOT YET IMPLEMENTED", null);
//	}
	private byte[] encipher_v2(String password, String sourceString)
	throws DecryptionException
	{
		String saltedMagic = "Salted__";

		final byte[] salt = (new SecureRandom()).generateSeed(8);


		byte[] src = sourceString.getBytes();

		try
		{
			int keylen = 32;
			int ivlen = 16;
			
			byte[] keyAndIV = SecretKeyFactory.getInstance("PBKDF2withHmacSHA256").generateSecret(new PBEKeySpec(password.toCharArray(), salt, 100_000, (keylen + ivlen) * 8)).getEncoded();
			Cipher cipher   = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyAndIV, 0, keylen, "AES"), new IvParameterSpec(keyAndIV, keylen, ivlen));
			
			byte[] bytesEnc = cipher.doFinal(src);
			int len = bytesEnc.length;
			
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
			throw new DecryptionException("Encryption failed, Caught: " + e, e);
		}
//		throw new DecryptionException("encipher_v2 -- NOT YET IMPLEMENTED", null);
	}



	private static int getEncryptedPasswordVersion(String base64Str)
	{
		// Base version is always 1 -- No Prefix in the input string 'base64Str'
		int version = 1;

		// Version 1 do not have any prefix
		// Version 2 have prefix of 'v2;'
		if (base64Str.startsWith(VERSION_PREFIX_V2))
			version = 2;

		return version;
	}
	

	/**
	 * Decode a String that has been encoded with os command openssl<br>
	 * This is the same as doing the following os command:<br>
	 * <code>echo 'U2FsdGVkX1+1dw6kuaMMFu4WkNLM1Cw09OYYWC5vRa4=' | openssl enc -aes-128-cbc -a -d -salt -pass pass:sybase</code>
	 * Or in OpenSSL Version 1.1.1
	 * <code>echo "encryptedPassword" | openssl enc -base64 -aes-256-cbc -pbkdf2 -iter 100000 -k sybase -d</code>
	 * 
	 * @param passwd
	 * @param base64Str
	 * @return
	 * @throws DecryptionException
	 */
	public static String decode(String passwd, String base64Str)
	throws DecryptionException
	{
		// Base version is always 1 -- No Prefix in the input string 'base64Str'
		int version = 1;

		// Version 1 do not have any prefix
		// Version 2 have prefix of 'v2;'
		if (base64Str.startsWith(VERSION_PREFIX_V2))
		{
			version = 2;
	
			// Strip away the version prefix
			base64Str = base64Str.substring(VERSION_PREFIX_V2.length());
		}

		// Decode based on the version prefix
		if (version == 1)
		{
			OpenSslAesUtil decrypter = new OpenSslAesUtil(128);

			byte[] ba = decrypter.decipher_v1(passwd.getBytes(), Base64.decodeBase64(base64Str));
			return new String(ba).trim();
		}
		else if (version == 2)
		{
			OpenSslAesUtil decrypter = new OpenSslAesUtil(256);

			byte[] ba = decrypter.decipher_v2(passwd, Base64.decodeBase64(base64Str));
			return new String(ba).trim();
		}
		else
		{
			throw new DecryptionException("Decryption of Version " + version + " is not supported.", null);
		}
	}

	private byte[] decipher_v1(byte[] pwd, byte[] src)
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

//	private byte[] decipher_v2(byte[] pwd, byte[] src)
//	throws DecryptionException
//	{
//		throw new DecryptionException("decipher_v2 -- NOT YET IMPLEMENTED", null);
//	}
//	private byte[] decipher_v2(String pwd, byte[] src)
//	throws DecryptionException
//	{
//		String password = pwd;
//		
//		// file pw: decrypt openssl(1.1.1+) enc -aes-256-cbc -pbkdf2 -k $pw
//		byte[] salt = new byte[8];
//		byte[] data = new byte[1024];
//		byte tmp[];
//
//		int keylen = 32;
//		int ivlen = 16;
//		int cnt;
//
////		try (InputStream is = new FileInputStream(args[0]))
//		try (InputStream is = new ByteArrayInputStream(src))
//		{
//			if ( is.read(salt) != 8 || !Arrays.equals(salt, "Salted__".getBytes()) || is.read(salt) != 8 )
//				throw new Exception("salt fail");
//
//			byte[] keyAndIV = SecretKeyFactory.getInstance("PBKDF2withHmacSHA256").generateSecret(new PBEKeySpec(password.toCharArray(), salt, 100_000, (keylen + ivlen) * 8)).getEncoded();
//			Cipher cipher   = Cipher.getInstance("AES/CBC/PKCS5Padding");
//			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyAndIV, 0, keylen, "AES"), new IvParameterSpec(keyAndIV, keylen, ivlen));
//
//			byte output[] = null;
//
//			while ((cnt = is.read(data)) > 0)
//			{
//				if ( (tmp = cipher.update(data, 0, cnt)) != null )
//				{
//					output = ArrayUtils.addAll(output, tmp);
////					System.out.write(tmp);
//				}
//			}
//			tmp = cipher.doFinal();
//			output = ArrayUtils.addAll(output, tmp);
////			System.out.write(tmp);
//			
//			return output;
//		}
//		catch (Exception ex)
//		{
//			ex.printStackTrace();
//			throw new DecryptionException("Problems when decrypting.", ex);
//		}
//	}
	private byte[] decipher_v2(String pwd, byte[] inSrc)
	throws DecryptionException
	{
		String password = pwd;

		// openssl non-standard extension: salt embedded at start of encrypted file
		byte[] salt = Arrays.copyOfRange(inSrc, 8, 16);  // 0..7 is "SALTED__", 8..15 is the salt

		// 
		byte[] dataSrc = Arrays.copyOfRange(inSrc, 16, inSrc.length); // 16 is where data starts (after the "Salted__########"
		
		int keylen = 32;
		int ivlen = 16;

		try
		{
			byte[] keyAndIV = SecretKeyFactory.getInstance("PBKDF2withHmacSHA256").generateSecret(new PBEKeySpec(password.toCharArray(), salt, 100_000, (keylen + ivlen) * 8)).getEncoded();
			Cipher cipher   = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyAndIV, 0, keylen, "AES"), new IvParameterSpec(keyAndIV, keylen, ivlen));

			return cipher.doFinal(dataSrc);
		}
		catch (Exception ex)
		{
			throw new DecryptionException("Problems when decrypting.", ex);
		}
	}

//https://stackoverflow.com/questions/73456313/decrypt-file-in-java-that-was-encrypted-with-openssl
//	static void SO73456313OpensslEnc2_Java (String[] args) throws Exception {
//	    // file pw: decrypt openssl(1.1.1+) enc -aes-256-cbc -pbkdf2 -k $pw
//	    byte[] salt = new byte[8], data = new byte[1024], tmp; 
//	    int keylen = 32, ivlen = 16, cnt;
//	    try( InputStream is = new FileInputStream(args[0]) ){
//	        if( is.read(salt) != 8 || !Arrays.equals(salt, "Salted__".getBytes() )
//	                || is.read(salt) != 8 ) throw new Exception("salt fail");
//	        byte[] keyAndIV = SecretKeyFactory.getInstance("PBKDF2withHmacSHA256") 
//	                .generateSecret( new PBEKeySpec(args[1].toCharArray(), salt, 10000, (keylen+ivlen)*8) 
//	                ).getEncoded();
//	        Cipher ciph = Cipher.getInstance("AES/CBC/PKCS5Padding"); 
//	        ciph.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyAndIV,0,keylen,"AES"), 
//	                new IvParameterSpec(keyAndIV,keylen,ivlen));
//	        while( (cnt = is.read(data)) > 0 ){
//	            if( (tmp = ciph.update(data, 0, cnt)) != null ) System.out.write(tmp);
//	        }
//	        tmp = ciph.doFinal(); System.out.write(tmp);
//	    }
//	}


	public static String getDefaultPassPhrase()
	{
		String passPhrase = Configuration.getCombinedConfiguration().getProperty("OpenSslAesUtil.readPasswdFromFile.encyption.password");

		if (passPhrase == null && "SqlServerTune".equals(Version.getAppName())) 
			passPhrase = "mssql";

		if (passPhrase == null)
			passPhrase = "sybase";
			
		return passPhrase;
	}
	
	/**
	 * Read the Password file as a String
	 * @param filename    Can be null, then the default filename is used.
	 * @return
	 * @throws IOException
	 */
	public static String readPasswdFile(String filename)
	throws IOException
	{
		// If no filename was passed: use "~/.passwd.enc" or get from property
		if (filename == null)
			filename = getPasswordFilename();

		String fileContent = FileUtils.readFile(filename, StandardCharsets.UTF_8.name());

		return fileContent;
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
	 * Do the following (on Linux) to generate a password: (for version 1, before OpenSSL 1.1.1)<br>
	 * <pre>
	 * echo "secretPasswordToEncrypt" | openssl enc -aes-128-cbc -a -salt -pass pass:sybase
	 * </pre>
	 * Do the following (on Linux) to generate a password: (for version 2, which uses OpenSSL 1.1.1 and possibly above)<br>
	 * <pre>
	 * echo "secretPasswordToEncrypt" | openssl enc -base64 -aes-256-cbc -pbkdf2 -iter 100000 -k sybase
	 * </pre>
	 * 
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

			int    fallbackEncPasswdAtRow = 0;
			int    srvMatchEncPasswdAtRow = 0;
			
			
			int rowNumber = 0;
			List<String> lines = Files.readAllLines(Paths.get(filename), Charset.forName("UTF-8"));
			for (String line : lines)
			{
				rowNumber++;

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
								srvMatchEncPasswd      = fEncPasswd;
								srvMatchEncPasswdAtRow = rowNumber;
							}
						}
						else
						{
							fallbackEncPasswd      = fEncPasswd;
							fallbackEncPasswdAtRow = rowNumber;
						}
					}
				}
			}
			
			String rawEncryptedStr      = null;
			int    rawEncryptedStrAtRow = 0;

			// Generic password *without* server specification (use this as a FALLBACK)
			// entry looking like |sa:encryptedPasswd|
			if (fallbackEncPasswd != null)
			{
				rawEncryptedStr      = fallbackEncPasswd;
				rawEncryptedStrAtRow = fallbackEncPasswdAtRow;
			}

			// password WITH server specification
			// entry looking like |sa:PROD_A_ASE:encryptedPasswd|
			if (srvMatchEncPasswd != null) 
			{
				rawEncryptedStr      = srvMatchEncPasswd;
				rawEncryptedStrAtRow = srvMatchEncPasswdAtRow;
			}

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
						throw new DecryptionException("When reading password file '" + f + "', at row " + rawEncryptedStrAtRow + ", we Caught: " + originEx.getMessage(), originEx);
						//throw originEx;
					}
				}
			}

			// Nothing was found...
			return null;
		}
		else
		{
			throw new FileNotFoundException("The password file '" + f + "' didn't exist.");
		}
	}

	

	//-------------------------------
	// WRITE
	//-------------------------------
	public static void writePasswdToFile(String clearPassword, String user)
	throws IOException, DecryptionException
	{
		writePasswdToFile(clearPassword, user, null, null, null, DEFAULT_VERSION);
	}
	
	public static void writePasswdToFile(String clearPassword, String user, String serverName)
	throws IOException, DecryptionException
	{
		writePasswdToFile(clearPassword, user, serverName, null, null, DEFAULT_VERSION);
	}
	
	public static void writePasswdToFile(String clearPassword, String user, String serverName, String filename)
	throws IOException, DecryptionException
	{
		writePasswdToFile(clearPassword, user, serverName, filename, null, DEFAULT_VERSION);
	}

	//-------------------------------
	// WRITE -- backwards compatibility for VERSION-1 passwords
	//-------------------------------
	public static void writePasswdToFile_v1(String clearPassword, String user)
	throws IOException, DecryptionException
	{
		writePasswdToFile(clearPassword, user, null, null, null, 1);
	}
	
	public static void writePasswdToFile_v1(String clearPassword, String user, String serverName)
	throws IOException, DecryptionException
	{
		writePasswdToFile(clearPassword, user, serverName, null, null, 1);
	}
	
	public static void writePasswdToFile_v1(String clearPassword, String user, String serverName, String filename)
	throws IOException, DecryptionException
	{
		writePasswdToFile(clearPassword, user, serverName, filename, null, 1);
	}

	//-------------------------------
	// REMOVE
	//-------------------------------
	public static boolean removePasswdFromFile(String user)
	throws IOException, DecryptionException
	{
		return writePasswdToFile("not-used", user, null, null, null, true, DEFAULT_VERSION);
	}
	
	public static boolean removePasswdFromFile(String user, String serverName)
	throws IOException, DecryptionException
	{
		return writePasswdToFile("not-used", user, serverName, null, null, true, DEFAULT_VERSION);
	}
	
	public static boolean removePasswdFromFile(String user, String serverName, String filename)
	throws IOException, DecryptionException
	{
		return writePasswdToFile("not-used", user, serverName, filename, null, true, DEFAULT_VERSION);
	}

	/**
	 * Write a password as an encrypted string to a file using the DEFAULT_VERSION for encryption version<br>
	 * <br>
	 * This will emulate openssl encryption (in java), for OpenSSL below version 1.1.1 <br>
	 * <pre>
	 * echo "secretPasswordToEncrypt" | openssl enc -aes-128-cbc -a -salt -pass pass:sybase
	 * </pre>
	 * When you have used OpenSSL 1.1.1 and above (which will be <b>prefixed</b> with "v2;" in the password file) <br>
	 * <pre>
	 * echo "secretPasswordToEncrypt" | openssl enc -base64 -aes-256-cbc -pbkdf2 -iter 100000 -k sybase
	 * </pre>
	 * 
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
		writePasswdToFile(clearPassword, user, serverName, filename, encPasswd, false, DEFAULT_VERSION);
	}

	/**
	 * Write a password as an encrypted string to a file using a <b>specific version</b> for encryption version<br>
	 * <br>
	 * This will emulate openssl encryption (in java), for OpenSSL below version 1.1.1 <br>
	 * <pre>
	 * echo "secretPasswordToEncrypt" | openssl enc -aes-128-cbc -a -salt -pass pass:sybase
	 * </pre>
	 * When you have used OpenSSL 1.1.1 and above (which will be <b>prefixed</b> with "v2;" in the password file) <br>
	 * <pre>
	 * echo "secretPasswordToEncrypt" | openssl enc -base64 -aes-256-cbc -pbkdf2 -iter 100000 -k sybase
	 * </pre>
	 * 
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
	 * @param version        The encryption version to use  
	 *
	 * @throws IOException for example if the password file didn't exist or that we had problems writing to the file
	 */
	public static void writePasswdToFile(String clearPassword, String user, String serverName, String filename, String encPasswd, int version)
	throws IOException, DecryptionException
	{
		writePasswdToFile(clearPassword, user, serverName, filename, encPasswd, false, version);
	}

	/**
	 * Write a password as an encrypted string to a file using a <b>specific version</b> for encryption version<br>
	 * <br>
	 * This will emulate openssl encryption (in java), for OpenSSL below version 1.1.1 <br>
	 * <pre>
	 * echo "secretPasswordToEncrypt" | openssl enc -aes-128-cbc -a -salt -pass pass:sybase
	 * </pre>
	 * When you have used OpenSSL 1.1.1 and above (which will be <b>prefixed</b> with "v2;" in the password file) <br>
	 * <pre>
	 * echo "secretPasswordToEncrypt" | openssl enc -base64 -aes-256-cbc -pbkdf2 -iter 100000 -k sybase
	 * </pre>
	 * 
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
	private static boolean writePasswdToFile(String clearPassword, String user, String serverName, String filename, String encPasswd, boolean remove, int version)
	throws IOException, DecryptionException
	{
		// If no password was passed: use "sybase" or get from property
		if (encPasswd == null)
			encPasswd = getDefaultPassPhrase();

		// If no filename was passed: use "~/.passwd.enc" or get from property
		if (filename == null)
			filename = getPasswordFilename();

		// Encrypt the clear password
		String encrypedPassword = encode(encPasswd, clearPassword, version);
		
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
				if (remove)
				{
					//throw new UserNotFoundException(user, serverName, filename);
					String serverNameTmp = serverName == null ? "" : serverName;
					throw new IOException("The Username'" + user + "', with server '" + serverNameTmp + "' was not found in file '" + filename + "'.");
				}
				else
				{
					// Short or Long entry...
					String newEntry = user + sep + encrypedPassword;
					if (serverName != null)
						newEntry = user + sep + serverName + sep + encrypedPassword;
					
					lines.add(newEntry);
				}
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

	
	/**
	 * Upgrade a 
	 * 
	 * @param filename    The file holding the password (null = ~/.passwd.enc)
	 * @param encPasswd   The passprase you used when generating the password (null = "sybase") 
	 * @param doBackup    If anything was upgraded, make a backup of the 'filename' before writing the new content
	 * @return
	 * @throws IOException
	 * @throws DecryptionException
	 */
	public static int upgradePasswdFile(String filename, String[] encPasswdArr, boolean doBackup, boolean continueOnFailedEntries)
	throws IOException, DecryptionException
	{
		// If no filename was passed: use "~/.passwd.enc" or get from property
		if (filename == null)
			filename = getPasswordFilename();

		// If no password was passed: use default or get from property
		if (encPasswdArr == null)
			encPasswdArr = new String[] {getDefaultPassPhrase()};
		
		// try some extra passwords
		if ( ! ArrayUtils.contains(encPasswdArr, "sybase") ) encPasswdArr = ArrayUtils.addAll(encPasswdArr, "sybase");
		if ( ! ArrayUtils.contains(encPasswdArr, "mssql" ) ) encPasswdArr = ArrayUtils.addAll(encPasswdArr, "mssql");

		// Read the file
		File f = new File(filename);
		if (f.exists())
		{
			List<String> lines = Files.readAllLines(Paths.get(filename), Charset.forName("UTF-8"));
			int lineNum = -1;
			int upgradeCount = 0;
			int upgradeFailCount = 0;
			int processedCount = 0;
			
			for (String line : lines)
			{
				lineNum++;

				if (line.trim().startsWith("#"))
					continue;

				String[] sa = line.split(":");
				if (sa.length >= 2)
				{
					processedCount++;

//					boolean srvEntry = false;
					String fUser      = null;
					String fServer    = null;
					String fEncPasswd = null;
					
					if (sa.length == 2)
					{
//						srvEntry   = false;
						fUser      = sa[0].trim();
						fServer    = null;
						fEncPasswd = sa[1].trim();
					}
					else
					{
//						srvEntry   = true;
						fUser      = sa[0].trim();
						fServer    = sa[1].trim();
						fEncPasswd = sa[2].trim();
					}
					
					// Get the version of the password
					int passwordVersion = getEncryptedPasswordVersion(fEncPasswd);
					
					// Upgrade password
					if (passwordVersion < DEFAULT_VERSION)
					{
						// Try Some passwords to decrypt 
						DecryptionException firstPasswdEx = null;
						int loopCnt = -1;
						for (String encPassword : encPasswdArr)
						{
							loopCnt++;
							if (_logger.isDebugEnabled())
								_logger.debug("TRYING WITH PASSWORD[" + loopCnt + "]: '" + encPassword + "'.");
								
							try 
							{
								String oldPasswordCleanText = decode(encPassword, fEncPasswd);
								String newPassword          = encode(encPassword, oldPasswordCleanText, DEFAULT_VERSION);

								// Compose the new record, by simply replace the old password with the new (so we can keep current line "format/spacing")
								String newRecord = line.replace(fEncPasswd, newPassword);

								// Make below print a little prettier
								if ( fServer == null )
									fServer = "";
									
								_logger.info("Upgrading password entry (fromVersion=" + passwordVersion + ", toVersion=" + DEFAULT_VERSION + "). file='" + f + "', lineNum=" + lineNum + ", srv='" + fServer + "', user='" + fUser + "'. OldEntry=|" + line + "|, newEntry=|" + newRecord + "|.");

								// Replace the old record in the list
								upgradeCount++;
								lines.set(lineNum, newRecord);

								// Reset first exception
								firstPasswdEx = null;
								
								// On first SUCCESS, break the loop
								break;
							}
							catch (DecryptionException ex)
							{
								if (_logger.isDebugEnabled())
									_logger.debug("PROBLEMS, when TRYING WITH PASSWORD[" + loopCnt + "]: '" + encPassword + "'.", ex);

								if (firstPasswdEx == null)
									firstPasswdEx = ex; 								
							}
						}
						if (firstPasswdEx != null)
						{
							String msg = "Failed to upgrading password entry (fromVersion=" + passwordVersion + ", toVersion=" + DEFAULT_VERSION + "). file='" + f + "', lineNum=" + lineNum + ", srv='" + fServer + "', user='" + fUser + "', encPasswd='" + fEncPasswd + "'. entry=|" + line + "|.";
							_logger.error(msg);

							if (continueOnFailedEntries)
							{
								// Compose the new record, by simply replace the old password with the new (so we can keep current line "format/spacing")
								String newRecord = "## commented out by password upgrade ## " + line;

								// Replace the old record in the list
								upgradeFailCount++;
								lines.set(lineNum, newRecord);
							}
							else
							{
								throw new DecryptionException(msg, firstPasswdEx);
							}
						}
					}
				}
			}

			// Now write the file back again
			if (upgradeCount > 0 || upgradeFailCount > 0)
			{
				// Should we make a copy of the old file first?
				if (doBackup)
				{
					String backupFilename = filename + "." + TimeUtils.getCurrentTimeForFileNameYmdHms();

					_logger.info("Making a backup copy of the password file='" + f + "', to='" + backupFilename + "'.");
					
					FileUtils.copy(filename, backupFilename, false, false);
				}
				
				_logger.info("Writing a new password file='" + f + "', because we upgraded " + upgradeCount + " entries to a never encryption version. upgradeFailCount=" + upgradeFailCount);
				Files.write(Paths.get(filename), lines, Charset.forName("UTF-8"), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			}
			else
			{
				_logger.info("Password upgrade did NOT find any passwords to upgrade. Checked " + processedCount + " entries in file='" + f + "'.");
			}

			return upgradeCount + upgradeFailCount;
		}
		else
		{
			throw new FileNotFoundException("The password file '" + f + "' didn't exist.");
		}
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


//		try { upgradePasswdFile(null, null, true, true); }
//		catch (Exception ex) { ex.printStackTrace(); }
//System.exit(0);
		
		// Version: 1
		try
		{
			String txt_abc123 = "abc123456789-123456789-123456789-";
//			String txt_abc123 = "h1SYsxOSG7/3p5en";
			System.out.println("Origin String to be encoded=|" + txt_abc123 +"|.");

			String enc_abc123 = encode("sybase", txt_abc123, VERSION_1);
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


		// Version: 2
		try
		{
			String txt_abc123 = "abc123-version-2";
			System.out.println("Origin String to be encoded=|" + txt_abc123 +"|.");

			String enc_abc123 = encode("sybase", txt_abc123, VERSION_2);
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

		
		try { System.out.println("V2 ---- decrypted=|" + decode("sybase", "v2;U2FsdGVkX1+z4srbRsKArxZPUEcmzkK0yIgkwGA7sTU=") + "|"); }
		catch(Exception e) { e.printStackTrace(); }
		

//		OpenSslAesUtil d = new OpenSslAesUtil(128);
//		String r = new String(d.decipher("mypassword".getBytes(), Base64.decodeBase64("U2FsdGVkX187CGv6DbEpqh/L6XRKON7uBGluIU0nT3w=")));
//		System.out.println(r);
//
//		r = new String(d.decipher("sybase".getBytes(), Base64.decodeBase64("U2FsdGVkX1/2chgTZtP5+b30Hwv1n2prE5CqtWcoH8A=")));
//		System.out.println(r);
		try { System.out.println("V1 --- decrypted=|" + decode("sybase", "U2FsdGVkX1/2chgTZtP5+b30Hwv1n2prE5CqtWcoH8A=") + "|"); }
		catch(Exception e) { e.printStackTrace(); }
		

		System.out.println("readFromFile=|" + getPasswordFilename() + "|");
		try { System.out.println("readFromFile=|" + readPasswdFromFile("sa", null, null, "mssql") + "|"); }
		catch(Exception e) { e.printStackTrace(); }
		
		try { System.out.println("x1-ok="   + decode("sybase", "U2FsdGVkX1/foj2pv2V24rLfl7RLdcMGdd8jaTngzns=")); } catch(Exception e) { e.printStackTrace(); }
//		try { System.out.println("x2-fail=" + decode("sybase", "U2FsdGVkX1+4mSAv8/x8TRYx8wPrWUovDh8HBY16ZTY=")); } catch(Exception e) { e.printStackTrace(); }
		try { System.out.println("x3-ok="   + decode("sysopr", "U2FsdGVkX1+4mSAv8/x8TRYx8wPrWUovDh8HBY16ZTY=")); } catch(Exception e) { e.printStackTrace(); }
	}
}
