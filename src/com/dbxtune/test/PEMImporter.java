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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
//import javax.xml.bind.DatatypeConverter;
//import jakarta.xml.bind.DatatypeConverter;


/**
 * found at: https://stackoverflow.com/questions/2138940/import-pem-into-java-key-store
 */
public class PEMImporter 
{

	public static SSLServerSocketFactory createSSLFactory(File privateKeyPem, File certificatePem, String password) throws Exception 
	{
		final SSLContext context = SSLContext.getInstance("TLS");
		final KeyStore keystore = createKeyStore(privateKeyPem, certificatePem, password);
		final KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		kmf.init(keystore, password.toCharArray());
		final KeyManager[] km = kmf.getKeyManagers();
		context.init(km, null, null);
		return context.getServerSocketFactory();
	}

	/**
	 * Create a KeyStore from standard PEM files
	 * 
	 * @param privateKeyPem the private key PEM file
	 * @param certificatePem the certificate(s) PEM file
	 * @param the password to set to protect the private key
	 */
	public static KeyStore createKeyStore(File privateKeyPem, File certificatePem, final String password)
	throws Exception, KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException 
	{
		final X509Certificate[] cert = createCertificates(certificatePem);
		final KeyStore keystore = KeyStore.getInstance("JKS");
		keystore.load(null);
		// Import private key
		final PrivateKey key = createPrivateKey(privateKeyPem);
		keystore.setKeyEntry(privateKeyPem.getName(), key, password.toCharArray(), cert);
		return keystore;
	}

	private static byte[] parseBase64Binary(String hexString)
	{
		//return DatatypeConverter.parseBase64Binary(hexString);
		
		byte[] decodedBytes = Base64.getDecoder().decode(hexString);
		return decodedBytes;
	}
	
	private static PrivateKey createPrivateKey(File privateKeyPem) 
	throws Exception 
	{
		final BufferedReader r = new BufferedReader(new FileReader(privateKeyPem));
		String s = r.readLine();
		if (s == null || !s.contains("BEGIN PRIVATE KEY")) {
			r.close();
			throw new IllegalArgumentException("No PRIVATE KEY found");
		}
		final StringBuilder b = new StringBuilder();
		s = "";
		while (s != null) {
			if (s.contains("END PRIVATE KEY")) {
				break;
			}
			b.append(s);
			s = r.readLine();
		}
		r.close();
		final String hexString = b.toString();
		final byte[] bytes = parseBase64Binary(hexString);
		return generatePrivateKeyFromDER(bytes);
	}

	private static X509Certificate[] createCertificates(File certificatePem) 
	throws Exception 
	{
		final List<X509Certificate> result = new ArrayList<X509Certificate>();
		final BufferedReader r = new BufferedReader(new FileReader(certificatePem));
		String s = r.readLine();
		if (s == null || !s.contains("BEGIN CERTIFICATE")) {
			r.close();
			throw new IllegalArgumentException("No CERTIFICATE found");
		}
		StringBuilder b = new StringBuilder();
		while (s != null) {
			if (s.contains("END CERTIFICATE")) {
				String hexString = b.toString();
				final byte[] bytes = parseBase64Binary(hexString);
				X509Certificate cert = generateCertificateFromDER(bytes);
				result.add(cert);
				b = new StringBuilder();
			} else {
				if (!s.startsWith("----")) {
					b.append(s);
				}
			}
			s = r.readLine();
		}
		r.close();

		return result.toArray(new X509Certificate[result.size()]);
	}

	private static RSAPrivateKey generatePrivateKeyFromDER(byte[] keyBytes) 
	throws InvalidKeySpecException, NoSuchAlgorithmException 
	{
		final PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
		final KeyFactory factory = KeyFactory.getInstance("RSA");
		return (RSAPrivateKey) factory.generatePrivate(spec);
	}

	private static X509Certificate generateCertificateFromDER(byte[] certBytes) 
	throws CertificateException 
	{
		final CertificateFactory factory = CertificateFactory.getInstance("X.509");
		return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certBytes));
	}

}