package com.asetune.hostmon;

import org.apache.log4j.Logger;

import com.asetune.utils.Configuration;
import com.asetune.utils.VersionShort;

public class MonitorIoLinux 
extends MonitorIo
{
	private static Logger _logger = Logger.getLogger(MonitorIoLinux.class);

	public MonitorIoLinux()
	{
		this(-1);
	}
	public MonitorIoLinux(int utilVersion)
	{
		super(utilVersion);
	}

	@Override
	public String getModuleName()
	{
		return "MonitorIoLinux";
	}

	@Override
	public String getCommand()
	{
		String cmd = super.getCommand();
		return cmd != null ? cmd : "iostat -xdk "+getSleepTime();
//		return cmd != null ? cmd : "iostat -xdzk "+getSleepTime(); // z doesnt seems to be standard
	}

	@Override
	public HostMonitorMetaData createMetaData(int utilVersion)
	{
		HostMonitorMetaData md = new HostMonitorMetaData();
		md.setTableName(getModuleName());

		// Device:         rrqm/s   wrqm/s     r/s     w/s   rsec/s   wsec/s avgrq-sz avgqu-sz   await  svctm  %util
		// sda               0.02     1.49    0.16    0.95     3.27    19.52    20.57     0.04   36.29   2.10   0.23

		// gorans@gorans-ub:~$ iostat -V
		// sysstat version 10.2.0
		// (C) Sebastien Godard (sysstat <at> orange.fr)
		//
		// Device:         rrqm/s   wrqm/s     r/s     w/s    rkB/s    wkB/s avgrq-sz avgqu-sz   await r_await w_await  svctm  %util
		// sda               0.40     7.40    0.74    1.50    20.08    46.56    59.67     0.12   52.65   19.91   68.77   7.93   1.77
		//                                                                                             ^^^^^^^ ^^^^^^^
		//                                                                                             new columns
		// at github oldest version (9.5.1): https://github.com/sysstat/sysstat/commits/master/iostat.c
		// I can still see r_await and w_await, so let start use those columns from 9.5.1
		// 
		// and: http://sebastien.godard.pagesperso-orange.fr/
		// Sysstat 9.1.2 released (development version).
		// New fields have been added to iostat's extended statistics, giving the average time for read and write requests to be served (thanks go to Jérôme Marchand from Redhat for his help with that). 
		// Note that those fields should not be mistaken for read and write service times as they also include the time spent by the requests in queue... 
		// Also with this version, tickless CPUs will no longer be displayed as offline processors, but as 100% idle ones.

//System.out.println("MonitorIoLinux.createMetaData(utilVersion="+utilVersion+")");
		_logger.info("When creating meta data for Linux 'iostat', initializing it using utility version "+VersionShort.toStr(utilVersion)+" (intVer="+utilVersion+").");
		_logger.debug("MonitorIoLinux.createMetaData(utilVersion="+utilVersion+")");

//System.out.println("Linux-IOSTAT: utilVersion="+utilVersion+", VersionShort.toInt(9,1,2)="+VersionShort.toInt(9,1,2));
		if ( utilVersion >= VersionShort.toInt(9,1,2) || utilVersion == -1) // -1 is not defined or "offline" mode... so choose the type with most columns (in the future might save the utilVersion in the offline database)
//		if ( utilVersion >= VersionShort.toInt(9,0,4) || utilVersion == -1) // -1 is not defined or "offline" mode... so choose the type with most columns (in the future might save the utilVersion in the offline database)
		{
			md.addStrColumn( "device",            1,  1, false,   30, "Disk device name");
			md.addIntColumn( "samples",           2,  0, true,        "Number of 'sub' sample entries of iostat this value is based on");
                                                 
			md.addStatColumn("rrqmPerSec",        3,  2, true, 10, 1, "The number of read requests merged per second that were queued to the device");
			md.addStatColumn("wrqmPerSec",        4,  3, true, 10, 1, "The number of write requests merged per second that were queued to the device.");
			md.addStatColumn("readsPerSec",       5,  4, true, 10, 1, "The number of read requests that were issued to the device per second.");
			md.addStatColumn("writesPerSec",      6,  5, true, 10, 1, "The number of write requests that were issued to the device per second.");
			md.addStatColumn("kbReadPerSec",      7,  6, true, 10, 1, "The number of kilobytes read from the device per second.");
			md.addStatColumn("kbWritePerSec",     8,  7, true, 10, 1, "The number of kilobytes writ to the device per second.");
			md.addDecColumn ("avgReadKbPerIo",    9,  0, true, 10, 1, "Avergare read size in KB per IO. Formula: kbReadPerSec/readsPerSec");
			md.addDecColumn ("avgWriteKbPerIo",  10,  0, true, 10, 1, "Avergare write size in KB per IO. Formula: kbWritePerSec/writePerSec");

			md.addStatColumn("avgrq-sz",         11,  8, true, 10, 1, "The average size (in  sectors) of the requests that were issued to the device.");
			md.addStatColumn("avgqu-sz",         12,  9, true, 10, 1, "The average queue length of the requests that were issued to the device.");
                                                
			md.addStatColumn("await",            13, 10, true, 10, 1, "The average time (in milliseconds) for I/O requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them.");
			md.addStatColumn("r_await",          14, 11, true, 10, 1, "The average time (in milliseconds) for read requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them.");
			md.addStatColumn("w_await",          15, 12, true, 10, 1, "The average time (in milliseconds) for write requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them.");
			md.addStatColumn("svctm",            16, 13, true, 10, 1, "The average service time (in milliseconds) for I/O requests that were issued to the device.");
			md.addStatColumn("utilPct",          17, 14, true, 5,  1, "Percentage of CPU time during which I/O requests were issued to the device (bandwidth utilization for the device). Device saturation occurs when this value is close to 100%.");

			md.addStrColumn("deviceDescription", 18,  0, true, 255,   "Mapping of the column 'device' to your own description.");
		}
		else
		{
			md.addStrColumn( "device",            1,  1, false,   30, "Disk device name");
			md.addIntColumn( "samples",           2,  0, true,        "Number of 'sub' sample entries of iostat this value is based on");
                                                  
			md.addStatColumn("rrqmPerSec",        3,  2, true, 10, 1, "The number of read requests merged per second that were queued to the device");
			md.addStatColumn("wrqmPerSec",        4,  3, true, 10, 1, "The number of write requests merged per second that were queued to the device.");
			md.addStatColumn("readsPerSec",       5,  4, true, 10, 1, "The number of read requests that were issued to the device per second.");
			md.addStatColumn("writesPerSec",      6,  5, true, 10, 1, "The number of write requests that were issued to the device per second.");
			md.addStatColumn("kbReadPerSec",      7,  6, true, 10, 1, "The number of kilobytes read from the device per second.");
			md.addStatColumn("kbWritePerSec",     8,  7, true, 10, 1, "The number of kilobytes writ to the device per second.");
			md.addDecColumn ("avgReadKbPerIo",    9,  0, true, 10, 1, "Avergare read size in KB per IO. Formula: kbReadPerSec/readsPerSec");
			md.addDecColumn ("avgWriteKbPerIo",  10,  0, true, 10, 1, "Avergare write size in KB per IO. Formula: kbWritePerSec/writePerSec");

			md.addStatColumn("avgrq-sz",         11,  8, true, 10, 1, "The average size (in  sectors) of the requests that were issued to the device.");
			md.addStatColumn("avgqu-sz",         12,  9, true, 10, 1, "The average queue length of the requests that were issued to the device.");
                                                 
			md.addStatColumn("await",            13, 10, true, 10, 1, "The average time (in milliseconds) for I/O requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them.");
			md.addStatColumn("svctm",            14, 11, true, 10, 1, "The average service time (in milliseconds) for I/O requests that were issued to the device.");
			md.addStatColumn("utilPct",          15, 12, true, 5,  1, "Percentage of CPU time during which I/O requests were issued to the device (bandwidth utilization for the device). Device saturation occurs when this value is close to 100%.");

			md.addStrColumn("deviceDescription", 16, 0, true, 255, "Mapping of the column 'device' to your own description.");
		}

		// Use "device" as the Primary Key, which is used to du summary/average calculations
		md.setPkCol("device");
		
		// Set column "samples", to a special status, which will contain number of 
		// underlying samples the summary/average caclulation was based on
		md.setStatusCol("samples",    HostMonitorMetaData.STATUS_COL_SUB_SAMPLE);

		// Set Percent columns
		md.setPercentCol("utilPct");

		// What regexp to use to split the input row into individual fields
		md.setParseRegexp(HostMonitorMetaData.REGEXP_IS_SPACE);

		// Skip the header line
		md.setSkipRows("device", "Device:");

		// Get SKIP and ALLOW from the Configuration
		md.setSkipAndAllowRows(null, Configuration.getCombinedConfiguration());
		md.setSkipAndAllowRows("hostmon.MonitorIo.", Configuration.getCombinedConfiguration());

		// set positions of some columns, this will be used in parseRow()
		_pos_readsPerSec  = md.getParseColumnArrayPos("readsPerSec");
		_pos_writesPerSec = md.getParseColumnArrayPos("writesPerSec");

		return md;
	}

	private int _pos_readsPerSec;  // Skip rows with 0.0
	private int _pos_writesPerSec; // Skip rows with 0.0

	@Override
	public String[] parseRow(HostMonitorMetaData md, String row, String[] preParsed, int type)
	{
		if ( preParsed.length == md.getParseColumnCount() )
		{
			// Skip header 
			if (skipRow(md, row, preParsed, type))
				return null;

			// now check the ALLOWED rows
			// If NO allow entries are installed, then true will be returned.
			if ( ! allowRow(md, row, preParsed, type))
				return null;

			// If first time sample, get ALL devices (simulate -z switch)
			if (isFirstTimeSample())
				return preParsed;

			// Skip readsPerSec && writesPerSec, which is ZERO
			// emulate the -z switch, which isn't around on all Linux platforms
			if ( "0.00".equals(preParsed[_pos_readsPerSec]) && "0.00".equals(preParsed[_pos_writesPerSec]) )
				return null;

			// Allow the row
			return preParsed;
		}
		return null;
	}
}
