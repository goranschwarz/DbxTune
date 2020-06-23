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
