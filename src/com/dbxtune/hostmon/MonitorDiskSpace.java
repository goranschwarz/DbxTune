/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.hostmon;

import com.dbxtune.ssh.SshConnection;

public abstract class MonitorDiskSpace
extends HostMonitor
{
	public MonitorDiskSpace(int utilVersion, String utilExtraInfo)
	{
		super(utilVersion, utilExtraInfo);
	}

//	public static HostMonitor createMonitor(String host, String user, String passwd, boolean start)
//	throws Exception
//	{
//		return createMonitor(host, 22, user, passwd, null, start);
//	}
//
//	public static HostMonitor createMonitor(String host, int port, String user, String passwd, String keyFile, boolean start)
//	throws Exception
//	{
//		SshConnection conn = new SshConnection(host, port, user, passwd, keyFile);
//		return createMonitor(conn, start);
//	}

	/**
	 * Factory method to create a 'uptime' / 'load average' monitoring of OS types
	 * @param conn
	 * @param start true if you want to start the monitoring at once
	 * @return a HostMonitor object for the OS, which the connection was made to
	 * @throws Exception
	 */
	//@override
//	public static HostMonitor createMonitor(SshConnection conn, boolean start)
	public static HostMonitor createMonitor(HostMonitorConnection conn, boolean start)
	throws Exception
	{
		if ( ! conn.isConnected() )
			conn.connect();

		if ( ! conn.isConnected() )
			throw new Exception("Failed to connect to the remote host. conn="+conn);

		String osname = conn.getOsName();
		//System.out.println("OS Name: '"+osname+"'.");

		HostMonitor mon = null;
		if (osname.equals("Linux"))
		{
			mon = new MonitorDiskSpaceAllOs();
			mon.setConnectedToVendor(OsVendor.Linux);
		}
		else if (osname.equals("SunOS"))
		{
			mon = new MonitorDiskSpaceAllOs();
			mon.setConnectedToVendor(OsVendor.Solaris);
		}
		else if (osname.equals("AIX"))
		{
			mon = new MonitorDiskSpaceAllOs();
			mon.setConnectedToVendor(OsVendor.Aix);
		}					
		else if (osname.equals("HP-UX"))
		{
			mon = new MonitorDiskSpaceAllOs();
			mon.setConnectedToVendor(OsVendor.Hp);
		}
		else if (osname.startsWith("Windows-"))
		{
			mon = new MonitorDiskSpaceAllOs();
			mon.setConnectedToVendor(OsVendor.Windows);
		}					
		else
		{
			throw new Exception("The Unix system '"+osname+"', is not supported by the module 'MonitorDiskSpace' for the moment.");
		}

		mon.setConnection(conn);
		if (start)
			mon.start();
		
		return mon;
	}

	public static HostMonitorMetaData[] createOfflineMetaData()
	{
		HostMonitorMetaData[] mdArr = new HostMonitorMetaData[1];

		mdArr[0] = new MonitorMeminfoLinux().createMetaData();
//		mdArr[1] = new MonitorMeminfoLinux().createMetaData();
//		mdArr[2] = new MonitorMeminfoLinux().createMetaData();
//		mdArr[3] = new MonitorMeminfoLinux().createMetaData();

		return mdArr;
	}

	public static void main(String[] args)
	{
		// Set Log4j Log Level
//		Configurator.setRootLevel(Level.TRACE);

		try
		{
			SshConnection conn = new SshConnection("sunspot", "gorans", "YHNmju76");
//			SshConnection conn = new SshConnection("bluesky2", "gorans", "xxxx");
		
			HostMonitorConnectionSsh hostMonConn = new HostMonitorConnectionSsh(conn);

			HostMonitor mon = createMonitor(hostMonConn, false);
			mon.start();
				
			while(true)
			{
				try { Thread.sleep(5*1000); }
				catch (InterruptedException e) {}

				OsTable sample = mon.executeAndParse();
				if (sample != null)
					System.err.println(">>>>>>>>executeAndParse(): \n" + sample.toTableString());
			}
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
