package com.asetune.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class NetUtils
{
	/**
	 * Get a port number which is free, start to loop at startPort<br>
	 * @param startPort what port to start the test on
	 * @return -1 if unhandled exceptions occurs, otherwise the first free port number.
	 * 
	 * @return
	 */
	public static int getFirstFreeLocalPortNumber(int startPort)
	{
		String ipAddress = null;
		try
		{
			InetAddress ip = InetAddress.getLocalHost();
			ipAddress = ip.getHostAddress();
		}
		catch (UnknownHostException e)
		{
			return -1;
		}
		
		
		for (int port=startPort; port<(32768*2); port++)
		{
			if (available(ipAddress, port))
				return port;
		}
		return -1;
	}
//	public static int getFirstFreeLocalPortNumber(int startPort)
//	{
//		for (int port=startPort; port<(32768*2); port++)
//		{
//			try
//			{
//				ServerSocket serverSocket = new ServerSocket(port);
//				serverSocket.close();
//				
//				return port;
//			}
//			catch (BindException ex)
//			{
//				continue;
//			}
//			catch (Throwable ex)
//			{
//				return -1;
//			}
//		}
//		return -1;
//	}

	private static boolean available(String hostname, int port)
	{
		if (hostname == null)
			hostname = "localhost";

		try (Socket ignored = new Socket(hostname, port))
		{
			return false;
		}
		catch (IOException ignored)
		{
			return true;
		}
	}
}
