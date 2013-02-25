package com.asetune.hostmon;

import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

import com.asetune.ssh.SshConnection;

public abstract class MonitorMpstat
extends HostMonitor
{
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
	 * Factory method to create a mpstat monitoring of OS types
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
		if (osname.equals("SunOS"))
		{
			mon = new MonitorMpstatSolaris();
		}
		else if (osname.equals("Linux"))
		{
			mon = new MonitorMpstatLinux();
		}
		else if (osname.equals("AIX"))
		{
			mon = new MonitorMpstatAix();
		}					
//		else if (osname.equals("HP-UX"))
//		{
//			mon = new MonitorMpstatHp();
//		}
		else
		{
			throw new Exception("The Unix system '"+osname+"', is not supported by the module 'MonitorMpstat' for the moment.");
		}

		mon.setConnection(conn);
		if (start)
			mon.start();
		
		return mon;
	}

	public static HostMonitorMetaData[] createOfflineMetaData()
	{
		HostMonitorMetaData[] mdArr = new HostMonitorMetaData[3];

		mdArr[0] = new MonitorMpstatSolaris().createMetaData();
		mdArr[1] = new MonitorMpstatLinux()  .createMetaData();
		mdArr[2] = new MonitorMpstatAix()    .createMetaData();
//		mdArr[3] = new MonitorMpstatHp()     .createMetaData();

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
			SshConnection conn = new SshConnection("sunspot", "gorans", "xxxx");

			HostMonitor mon = createMonitor(conn, false);
			mon.start();

			while(true)
			{
				try { Thread.sleep(10*1000); }
				catch (InterruptedException e) {}
				OsTable sample = mon.getSummaryTable();
				if (sample != null)
					System.err.println(">>>>>>>>getSummaryTable(): \n" + sample.toTableString());
			}
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
