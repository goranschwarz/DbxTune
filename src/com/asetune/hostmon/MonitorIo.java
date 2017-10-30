package com.asetune.hostmon;

import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

import com.asetune.ssh.SshConnection;
import com.asetune.ssh.SshConnection.LinuxUtilType;

public abstract class MonitorIo
extends HostMonitor
{
	public MonitorIo(int utilVersion)
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
	 * Factory method to create a iostat or veritas monitoring of OS types
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

		boolean hasVeritas = conn.hasVeritas();
		if (hasVeritas)
			osname = "VERITAS";

		HostMonitor mon = null;
		if (osname.equals("SunOS"))
		{
			mon = new MonitorIoSolaris();
			mon.setConnectedToVendor(OsVendor.Solaris);
		}
		else if (osname.equals("Linux"))
		{
			int utilVersion = conn.getLinuxUtilVersion(LinuxUtilType.IOSTAT);
			mon = new MonitorIoLinux(utilVersion);
			mon.setConnectedToVendor(OsVendor.Linux);
		}
		else if (osname.equals("AIX"))
		{
			mon = new MonitorIoAix();
			mon.setConnectedToVendor(OsVendor.Aix);
		}					
		else if (osname.equals("HP-UX"))
		{
			mon = new MonitorIoHp();
			mon.setConnectedToVendor(OsVendor.Hp);
		}					
		else if (osname.equals("VERITAS"))
		{
			mon = new MonitorIoVeritas();
			mon.setConnectedToVendor(OsVendor.Veritas);
		}
		else
		{
			throw new Exception("The Unix system '"+osname+"', is not supported by the module 'MonitorIo' for the moment.");
		}

		mon.setConnection(conn);
		if (start)
			mon.start();
		
		return mon;
	}
	
	public static HostMonitorMetaData[] createOfflineMetaData()
	{
		HostMonitorMetaData[] mdArr = new HostMonitorMetaData[5];

		mdArr[0] = new MonitorIoSolaris().createMetaData();
		mdArr[1] = new MonitorIoLinux()  .createMetaData();
		mdArr[2] = new MonitorIoAix()    .createMetaData();
		mdArr[3] = new MonitorIoHp()     .createMetaData();
		mdArr[4] = new MonitorIoVeritas().createMetaData();

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
			SshConnection conn = new SshConnection("sunspot", "gorans", "xxxxx");
			HostMonitor mon = createMonitor(conn, false);
//			mon.start();

			while(true)
			{
				try { Thread.sleep(2*1000); }
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
