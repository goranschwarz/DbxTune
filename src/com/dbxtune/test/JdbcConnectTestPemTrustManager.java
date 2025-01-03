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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.UUID;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class JdbcConnectTestPemTrustManager 
implements X509TrustManager
{
	private X509Certificate  _cert;
	private X509TrustManager _trustManager;

	public JdbcConnectTestPemTrustManager(String certToTrust) 
			throws IOException, GeneralSecurityException 
	{
		if (certToTrust == null)
			certToTrust = System.getProperty("certToTrust");

		if (certToTrust == null)
			throw new IOException("No PEM file was passed.");
try {		
System.out.println("############################# JdbcConnectTestPemTrustManager: certToTrust='" + certToTrust + "'.");

		try (InputStream inStream = new FileInputStream(certToTrust)) 
		{
			// Load Certificate
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			_cert = (X509Certificate)cf.generateCertificate(inStream);

			// Create a KeyStore
			KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
			try 
			{
				// Note: KeyStore requires it be loaded even if you don't load anything into it:
				ks.load(null);
			} 
			catch (Exception e) 
			{ 
				e.printStackTrace(); 
			}
			ks.setCertificateEntry(UUID.randomUUID().toString(), _cert);

			// and a Trust Manager
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(ks);
			for (TrustManager tm : tmf.getTrustManagers()) 
			{
				if (tm instanceof X509TrustManager) 
				{
					_trustManager = (X509TrustManager) tm;
					break;
				}
			}
		}

System.out.println("############################# JdbcConnectTestPemTrustManager: _trustManager='" + _trustManager + "'.");
		if (_trustManager == null) 
		{
			throw new GeneralSecurityException("No X509TrustManager found");
		}
}
catch (Exception ex)
{
	ex.printStackTrace();
	throw ex;
}
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) 
	throws CertificateException
	{
		System.out.println("############################# JdbcConnectTestPemTrustManager.checkClientTrusted()");
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) 
	throws CertificateException
	{
		System.out.println("############################# JdbcConnectTestPemTrustManager.checkServerTrusted()");
		_trustManager.checkServerTrusted(chain, authType);
	}

	@Override
	public X509Certificate[] getAcceptedIssuers()
	{
		System.out.println("############################# JdbcConnectTestPemTrustManager.getAcceptedIssuers()");
		return new X509Certificate[] { _cert };
	}

}
