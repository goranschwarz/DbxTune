package com.asetune.test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.List;

import com.btr.proxy.search.ProxySearch;
import com.btr.proxy.util.Logger;
import com.btr.proxy.util.Logger.LogLevel;

public class ProxyTest
{
	private void installLogger()
	{
		Logger.setBackend(new Logger.LogBackEnd()
		{
			@Override
			public void log(Class<?> clazz, LogLevel loglevel, String msg, Object... params)
			{
				System.out.println(loglevel + "\t" + MessageFormat.format(msg, params));
			}

			@Override
			public boolean isLogginEnabled(LogLevel logLevel)
			{
				return true;
			}
		});
	}

	public static void main(String[] args)
	{
//		System.setProperty("http.proxyHost", "10.65.12.21");
//		System.setProperty("http.proxyPort", "8080");
//		System.setProperty("java.net.useSystemProxies", "true");

		ProxyTest pt = new ProxyTest();
		pt.installLogger();

		ProxySearch proxySearch = ProxySearch.getDefaultProxySearch();

//		ProxySearch proxySearch = new ProxySearch();
//		proxySearch.addStrategy(Strategy.JAVA);
//		proxySearch.addStrategy(Strategy.BROWSER);
//		proxySearch.addStrategy(Strategy.OS_DEFAULT);
//		proxySearch.addStrategy(Strategy.ENV_VAR);

		ProxySelector myProxySelector = proxySearch.getProxySelector();
		                
		ProxySelector.setDefault(myProxySelector);
		System.out.println("Using proxy selector: "+myProxySelector);

		//		String webAddress = "http://www.google.com";
		String webAddress = "http://www.asetune.com";
		try
		{
			URL url = new URL(webAddress);
			List<Proxy> result = myProxySelector.select(url.toURI());
			if (result == null || result.size() == 0) 
			{
				System.out.println("No proxy found for this url.");
				return;
			}
			System.out.println("Proxy Settings found using 'xxx' strategy.\n" +
					"Proxy used for URL is: "+result.get(0));

			
			System.out.println("Now open a connection to the url: " + webAddress);
			System.out.println("==============================================");

			// open the connection and prepare it to POST
			URLConnection conn = url.openConnection();
			conn.setConnectTimeout(10 * 1000); 

			// Return the response
			InputStream in = conn.getInputStream();
			LineNumberReader lr = new LineNumberReader(new InputStreamReader(in));
			String line;
			while ((line = lr.readLine()) != null)
			{
				System.out.println("response line "+lr.getLineNumber()+": " + line);
			}
			System.out.println("---- END -------------------------------------");
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
}
