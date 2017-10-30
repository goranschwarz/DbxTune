package com.asetune.hostmon;

import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

import com.asetune.ssh.SshConnection;

public abstract class MonitorMeminfo
extends HostMonitor
{
	public MonitorMeminfo(int utilVersion)
	{
		super(utilVersion);
	}

	public static HostMonitor createMonitor(String host, String user, String passwd, boolean start)
	throws Exception
	{
		return createMonitor(host, 22, user, passwd, start);
	}

	public static HostMonitor createMonitor(String host, int port, String user, String passwd, boolean start)
	throws Exception
	{
		SshConnection conn = new SshConnection(host, port, user, passwd);
		return createMonitor(conn, start);
	}

	/**
	 * Factory method to create a 'uptime' / 'load average' monitoring of OS types
	 * @param conn
	 * @param start true if you want to start the monitoring at once
	 * @return a HostMonitor object for the OS, which the connection was made to
	 * @throws Exception
	 */
	//@override
	public static HostMonitor createMonitor(SshConnection conn, boolean start)
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
			mon = new MonitorMeminfoLinux();
			mon.setConnectedToVendor(OsVendor.Linux);
		}
//		else if (osname.equals("SunOS"))
//		{
//			mon = new MonitorMeminfoSolaris();
//			mon.setConnectedToVendor(OsVendor.Solaris);
//		}
//		else if (osname.equals("AIX"))
//		{
//			mon = new MonitorMeminfoAix();
//			mon.setConnectedToVendor(OsVendor.Aix);
//		}					
//		else if (osname.equals("HP-UX"))
//		{
//			mon = new MonitorMeminfoHp();
//			mon.setConnectedToVendor(OsVendor.Hp);
//		}
		else
		{
			throw new Exception("The Unix system '"+osname+"', is not supported by the module 'MonitorMeminfo' for the moment.");
		}

		mon.setConnection(conn);
		if (start)
			mon.start();
		
		return mon;
	}

	public static HostMonitorMetaData[] createOfflineMetaData()
	{
		HostMonitorMetaData[] mdArr = new HostMonitorMetaData[4];

		mdArr[0] = new MonitorUpTimeAllOs().createMetaData();
		mdArr[1] = new MonitorUpTimeAllOs().createMetaData();
		mdArr[2] = new MonitorUpTimeAllOs().createMetaData();
		mdArr[3] = new MonitorUpTimeAllOs().createMetaData();

		return mdArr;
	}

	public static void main(String[] args)
	{
		Properties log4jProps = new Properties();
		//log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		try
		{
			SshConnection conn = new SshConnection("sunspot", "gorans", "YHNmju76");
//			SshConnection conn = new SshConnection("bluesky2", "gorans", "xxxx");
		
			HostMonitor mon = createMonitor(conn, false);
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
