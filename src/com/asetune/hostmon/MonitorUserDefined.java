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
package com.asetune.hostmon;

import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

import com.asetune.ssh.SshConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.MandatoryPropertyException;


public class MonitorUserDefined
extends HostMonitor
{
	private String              _moduleName = "";
	private HostMonitorMetaData _metaData   = null;

	@Override
	public String getCommand()
	{
		return _metaData.getOsCommand();
	}

//	public MonitorUserDefined(Configuration conf, String moduleName, SshConnection conn, boolean start)
	public MonitorUserDefined(Configuration conf, String moduleName, HostMonitorConnection conn, boolean start)
	throws Exception
	{
		super(-1, null);

		_moduleName = moduleName;

		if ( ! conn.isConnected() )
			conn.connect();

		if ( ! conn.isConnected() )
			throw new Exception("Failed to connect to the remote host. conn="+conn);

		String osname = conn.getOsName();
//		System.out.println("OS Name: '"+osname+"'.");

//		if (conf == null)
//			conf = Configuration.getInstance(Configuration.CONF);
		if (conf == null)
			conf = Configuration.getCombinedConfiguration();

		// Create the meta data, from properties
		_metaData = HostMonitorMetaData.create(conf, _moduleName, osname);
		setMetaData(_metaData);

//		if (osname.equals("SunOS"))
//		else if (osname.equals("Linux"))
//		else if (osname.equals("AIX"))
//		else if (osname.equals("HP-UX"))
//		else
//		{
//			throw new Exception("The Unix system '"+osname+"', is not supported by the module 'MonitorVmstat' for the moment.");
//		}

		setConnection(conn);
		if (start)
			start();
	}

	@Override
	public HostMonitorMetaData createMetaData(int utilVersion, Configuration utilExtraInfo)
	{
		return _metaData;
	}

	@Override
	public String getModuleName()
	{
		return _moduleName;
	}

	public static HostMonitorMetaData[] createOfflineMetaData(String moduleName)
	{
		try
		{
			HostMonitorMetaData[] mdArr = new HostMonitorMetaData[1];

			mdArr[0] = HostMonitorMetaData.create(
					Configuration.getCombinedConfiguration(),
					moduleName, 
					null);
			return mdArr;
		}
		catch (MandatoryPropertyException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args)
	{
		Properties log4jProps = new Properties();
		//log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.rootLogger", "DEBUG, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		try
		{
			//------------------------------------------------------
			// mpstat linux
			//------------------------------------------------------
			Configuration confIostatLinux = new Configuration();
			
			confIostatLinux.setProperty("hostmon.udc.TestGorans.osCommand",                  "iostat -xdzk 1");
			confIostatLinux.setProperty("hostmon.udc.TestGorans.osCommand.isStreaming",      "true");
			
			confIostatLinux.setProperty("hostmon.udc.TestGorans.addStrColumn.device",        "{length=30,             sqlColumnNumber=1,  parseColumnNumber=1,  isNullable=false, description=Disk device name}");
			confIostatLinux.setProperty("hostmon.udc.TestGorans.addIntColumn.numOfSamples",  "{                       sqlColumnNumber=2,  parseColumnNumber=0,  isNullable=true,  description=Number of 'sub' sample entries of iostat this value is based on}");
			confIostatLinux.setProperty("hostmon.udc.TestGorans.addStatColumn.rrqmPerSec",   "{precision=10, scale=1, sqlColumnNumber=3,  parseColumnNumber=2,  isNullable=true,  description=The number of read requests merged per second that were queued to the device}");
			confIostatLinux.setProperty("hostmon.udc.TestGorans.addStatColumn.wrqmPerSec",   "{precision=10, scale=1, sqlColumnNumber=4,  parseColumnNumber=3,  isNullable=true,  description=The number of write requests merged per second that were queued to the device.}");
			confIostatLinux.setProperty("hostmon.udc.TestGorans.addStatColumn.readsPerSec",  "{precision=10, scale=1, sqlColumnNumber=5,  parseColumnNumber=4,  isNullable=true,  description=The number of read requests that were issued to the device per second.}");
			confIostatLinux.setProperty("hostmon.udc.TestGorans.addStatColumn.writesPerSec", "{precision=10, scale=1, sqlColumnNumber=6,  parseColumnNumber=5,  isNullable=true,  description=The number of write requests that were issued to the device per second.}");
			confIostatLinux.setProperty("hostmon.udc.TestGorans.addStatColumn.kbReadPerSec", "{precision=10, scale=1, sqlColumnNumber=7,  parseColumnNumber=6,  isNullable=true,  description=The number of kilobytes read from the device per second.}");
			confIostatLinux.setProperty("hostmon.udc.TestGorans.addStatColumn.kbWritePerSec","{precision=10, scale=1, sqlColumnNumber=8,  parseColumnNumber=7,  isNullable=true,  description=The number of kilobytes writ to the device per second.}");
			confIostatLinux.setProperty("hostmon.udc.TestGorans.addStatColumn.avgrq-sz",     "{precision=10, scale=1, sqlColumnNumber=9,  parseColumnNumber=8,  isNullable=true,  description=The average size (in  sectors) of the requests that were issued to the device.}");
			confIostatLinux.setProperty("hostmon.udc.TestGorans.addStatColumn.avgqu-sz",     "{precision=10, scale=1, sqlColumnNumber=10, parseColumnNumber=9,  isNullable=true,  description=The average queue length of the requests that were issued to the device.}");
			confIostatLinux.setProperty("hostmon.udc.TestGorans.addStatColumn.await",        "{precision=10, scale=1, sqlColumnNumber=11, parseColumnNumber=10, isNullable=true,  description=The average time (in milliseconds) for I/O requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them.}");
			confIostatLinux.setProperty("hostmon.udc.TestGorans.addStatColumn.svctm",        "{precision=10, scale=1, sqlColumnNumber=12, parseColumnNumber=11, isNullable=true,  description=The average service time (in milliseconds) for I/O requests that were issued to the device.}");
			confIostatLinux.setProperty("hostmon.udc.TestGorans.addStatColumn.utilPct",      "{precision=10, scale=1, sqlColumnNumber=13, parseColumnNumber=12, isNullable=true,  description=Percentage of CPU time during which I/O requests were issued to the device (bandwidth utilization for the device). Device saturation occurs when this value is close to 100%.}");

			confIostatLinux.setProperty("hostmon.udc.TestGorans.setPkColumns",               "device");
			confIostatLinux.setProperty("hostmon.udc.TestGorans.setPercentColumns",          "utilPct, await,svctm");
			confIostatLinux.setProperty("hostmon.udc.TestGorans.setSubSampleColumn",         "numOfSamples");
			confIostatLinux.setProperty("hostmon.udc.TestGorans.setParseRegexp",             "DeFaUlT");
			confIostatLinux.setProperty("hostmon.udc.TestGorans.skipRows.device",            "Device:");


			//------------------------------------------------------
			// mpstat linux
			//------------------------------------------------------
			Configuration confMpstatLinux = new Configuration();
			
			confMpstatLinux.setProperty("hostmon.udc.TestGorans.osCommand",                  "mpstat 1");
			confMpstatLinux.setProperty("hostmon.udc.TestGorans.osCommand.isStreaming",      "true");

			confMpstatLinux.setProperty("hostmon.udc.TestGorans.addStrColumn.CPU",           "{length=30,             sqlColumnNumber=1,  parseColumnNumber=3,  isNullable=false, description=Processor number. The keyword all indicates that statistics are calculated as averages among all processors.}");
			confMpstatLinux.setProperty("hostmon.udc.TestGorans.addIntColumn.numOfSamples",  "{                       sqlColumnNumber=2,  parseColumnNumber=0,  isNullable=true,  description=Number of 'sub' sample entries of iostat this value is based on}");
			confMpstatLinux.setProperty("hostmon.udc.TestGorans.addStatColumn.usrPct",       "{precision=10, scale=1, sqlColumnNumber=3,  parseColumnNumber=4,  isNullable=true,  description=Show the percentage of CPU utilization that occurred while executing at the user level (application).}");
			confMpstatLinux.setProperty("hostmon.udc.TestGorans.addStatColumn.nicePct",      "{precision=10, scale=1, sqlColumnNumber=4,  parseColumnNumber=5,  isNullable=true,  description=Show the percentage of CPU utilization that occurred while executing at the user level with nice priority.}");
			confMpstatLinux.setProperty("hostmon.udc.TestGorans.addStatColumn.sysPct",       "{precision=10, scale=1, sqlColumnNumber=5,  parseColumnNumber=6,  isNullable=true,  description=Show the percentage of CPU utilization that occurred while executing at the system level (kernel). Note that this does not include time spent servicing hardware and software interrupts.}");
			confMpstatLinux.setProperty("hostmon.udc.TestGorans.addStatColumn.iowaitPct",    "{precision=10, scale=1, sqlColumnNumber=6,  parseColumnNumber=7,  isNullable=true,  description=Show the percentage of time that the CPU or CPUs were idle during which the system had an outstanding disk I/O request.}");
			confMpstatLinux.setProperty("hostmon.udc.TestGorans.addStatColumn.irqPct",       "{precision=10, scale=1, sqlColumnNumber=7,  parseColumnNumber=8,  isNullable=true,  description=Show the percentage of time spent by the CPU or CPUs to service hardware interrupts.}");
			confMpstatLinux.setProperty("hostmon.udc.TestGorans.addStatColumn.softPct",      "{precision=10, scale=1, sqlColumnNumber=8,  parseColumnNumber=9,  isNullable=true,  description=Show the percentage of time spent by the CPU or CPUs to service software interrupts.}");
			confMpstatLinux.setProperty("hostmon.udc.TestGorans.addStatColumn.stealPct",     "{precision=10, scale=1, sqlColumnNumber=9,  parseColumnNumber=10, isNullable=true,  description=Show the percentage of time spent in involuntary wait by the virtual CPU or CPUs while the hypervisor was servicing another virtual processor.}");
			confMpstatLinux.setProperty("hostmon.udc.TestGorans.addStatColumn.guestPct",     "{precision=10, scale=1, sqlColumnNumber=10, parseColumnNumber=11, isNullable=true,  description=Show the percentage of time spent by the CPU or CPUs to run a virtual processor.}");
			confMpstatLinux.setProperty("hostmon.udc.TestGorans.addStatColumn.idlePct",      "{precision=10, scale=1, sqlColumnNumber=11, parseColumnNumber=12, isNullable=true,  description=Show the percentage of time that the CPU or CPUs were idle and the system did not have an outstanding disk I/O request.}");

			confMpstatLinux.setProperty("hostmon.udc.TestGorans.setPkColumns",               "CPU");
			confMpstatLinux.setProperty("hostmon.udc.TestGorans.setPercentColumns",          "usrPct, nicePct, sysPct, iowaitPct, irqPct, softPct, stealPct, guestPct, idlePct");
			confMpstatLinux.setProperty("hostmon.udc.TestGorans.setSubSampleColumn",         "numOfSamples");
			confMpstatLinux.setProperty("hostmon.udc.TestGorans.setParseRegexp",             "DEFAULT");
			confMpstatLinux.setProperty("hostmon.udc.TestGorans.skipRows.CPU",               "CPU");
			
			
			//------------------------------------------------------
			// ls -Fal linux
			//------------------------------------------------------
			Configuration confLs = new Configuration();
			
			confLs.setProperty("hostmon.udc.TestGorans.osCommand",                  "ls -Fl | egrep -v '^d'");
			confLs.setProperty("hostmon.udc.TestGorans.osCommand.isStreaming",      "false");

			confLs.setProperty("hostmon.udc.TestGorans.addStrColumn.umode",         "{length=10, sqlColumnNumber=1,  parseColumnNumber=1,  isNullable=false, description=xxx}");
			confLs.setProperty("hostmon.udc.TestGorans.addIntColumn.files",         "{           sqlColumnNumber=2,  parseColumnNumber=2,  isNullable=true,  description=xxx}");
			confLs.setProperty("hostmon.udc.TestGorans.addStrColumn.owner",         "{length=10, sqlColumnNumber=3,  parseColumnNumber=3,  isNullable=true,  description=xxx}");
			confLs.setProperty("hostmon.udc.TestGorans.addStrColumn.group",         "{length=10, sqlColumnNumber=4,  parseColumnNumber=4,  isNullable=true,  description=xxx}");
			confLs.setProperty("hostmon.udc.TestGorans.addIntColumn.sizeInBytes",   "{           sqlColumnNumber=5,  parseColumnNumber=5,  isNullable=true,  description=xxx}");
			confLs.setProperty("hostmon.udc.TestGorans.addStrColumn.date",          "{length=10, sqlColumnNumber=6,  parseColumnNumber=6,  isNullable=true,  description=xxx}");
			confLs.setProperty("hostmon.udc.TestGorans.addStrColumn.time",          "{length=5,  sqlColumnNumber=7,  parseColumnNumber=7,  isNullable=true,  description=xxx}");
			confLs.setProperty("hostmon.udc.TestGorans.addStrColumn.filename",      "{length=99, sqlColumnNumber=8,  parseColumnNumber=8,  isNullable=true,  description=xxx}");
			
			
//			SshConnection conn = new SshConnection("sunspot", "gorans", "xxxx");
//			SshConnection conn = new SshConnection("bluesky2", "gorans", "xxxx");
			SshConnection conn = new SshConnection("gorans.no-ip.org", "gorans", "1niss2e");
//			SshConnection conn = new SshConnection("sweiq-linux", "ajackson", "sybase");
		
			HostMonitorConnectionSsh hostMonConn = new HostMonitorConnectionSsh(conn);

		//	HostMonitor mon = new MonitorUserDefined(confIostatLinux, "TestGorans", hostMonConn, false);
			HostMonitor mon = new MonitorUserDefined(confMpstatLinux, "TestGorans", hostMonConn, false);
		//	HostMonitor mon = new MonitorUserDefined(confLs,          "TestGorans", hostMonConn, false);

			if (mon.isOsCommandStreaming())
				mon.start();
				
			while(true)
			{
				try { Thread.sleep(4*1000); }
				catch (InterruptedException e) {}
				
				if (mon.isOsCommandStreaming())
				{
					OsTable sample = mon.getSummaryTable();
					if (sample != null)
						System.err.println(">>>>>>>>getSummaryTable(): \n" + sample.toTableString());
				}
				else
				{					
					OsTable sample = mon.executeAndParse();
					if (sample != null)
						System.err.println(">>>>>>>>executeAndParse(): \n" + sample.toTableString());
				}
			}
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
