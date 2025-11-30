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

import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.VersionShort;


public class MonitorMpstatLinux
extends MonitorMpstat
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	
	public MonitorMpstatLinux()
	{
		this(-1, null);
	}
	public MonitorMpstatLinux(int utilVersion, String utilExtraInfo)
	{
		super(utilVersion, utilExtraInfo);
	}

	@Override
	public String getModuleName()
	{
		return "MonitorMpstatLinux";
	}

	@Override
	public String getCommand()
	{
		String cmd = super.getCommand();
		return cmd != null ? cmd : "mpstat -P ALL "+getSleepTime();
	}

	@Override
	public HostMonitorMetaData createMetaData(int utilVersion, Configuration utilExtraInfo)
	{
		HostMonitorMetaData md = new HostMonitorMetaData();
		md.setTableName(getModuleName());

		_logger.info("When creating meta data for Linux 'mpstat', initializing it using utility version "+VersionShort.toStr(utilVersion)+" (intVer="+utilVersion+").");

		// gorans@gorans-ub:~$ mpstat -V
		// sysstat version 10.2.0
		// (C) Sebastien Godard (sysstat <at> orange.fr)
		//
		// 05:27:59 PM  CPU    %usr   %nice    %sys %iowait    %irq   %soft  %steal  %guest  %gnice   %idle
		// 05:28:04 PM  all    2.82    0.00    1.41    2.01    0.00    0.00    0.00    0.00    0.00   93.76
		//                                                                                   ^^^^^^
		//                                                                                   new col
		//
		// http://sebastien.godard.pagesperso-orange.fr/
		// Sysstat 10.1.2 released (development version).
		// Two new metrics have been added here: the time spent by processors running a niced guest (displayed as "%gnice" by sar -u and mpstat) 
		// and the amount of memory waiting to get written back to disk (displayed as "kbdirty" by sar -r). 
		// This version also includes support for "systemd", the system and service manager intended to replace the init daemon for Linux.
		//
		// The source code on github also indicates this: https://github.com/sysstat/sysstat/commits/master/mpstat.c

//System.out.println("Linux-MPSTAT: utilVersion="+utilVersion+", VersionShort.toInt(10,1,2)="+VersionShort.toInt(10,1,2));
		if ( utilVersion >= VersionShort.toInt(10,1,2) || utilVersion == -1) // -1 is not defined or "offline" mode... so choose the type with most columns (in the future might save the utilVersion in the offline database)
		{
			md.addStrColumn( "CPU",            1,  2, false,   5, "Processor number. The keyword all indicates that statistics are calculated as averages among all processors.");
			md.addIntColumn( "samples",        2,  0, true,        "Number of 'sub' sample entries of iostat this value is based on");

			md.addStatColumn("usrPct",         3,  3, true, 5,  1, "Show the percentage of CPU utilization that occurred while executing at the user level (application).");
			md.addStatColumn("nicePct",        4,  4, true, 5,  1, "Show the percentage of CPU utilization that occurred while executing at the user level with nice priority.");
			md.addStatColumn("sysPct",         5,  5, true, 5,  1, "Show the percentage of CPU utilization that occurred while executing at the system level (kernel). Note that this does not include time spent servicing hardware and software interrupts.");
			md.addStatColumn("iowaitPct",      6,  6, true, 5,  1, "Show the percentage of time that the CPU or CPUs were idle during which the system had an outstanding disk I/O request.");
			md.addStatColumn("irqPct",         7,  7, true, 5,  1, "Show the percentage of time spent by the CPU or CPUs to service hardware interrupts.");
			md.addStatColumn("softPct",        8,  8, true, 5,  1, "Show the percentage of time spent by the CPU or CPUs to service software interrupts.");
			md.addStatColumn("stealPct",       9,  9, true, 5,  1, "Show the percentage of time spent in involuntary wait by the virtual CPU or CPUs while the hypervisor was servicing another virtual processor.");
			md.addStatColumn("guestPct",      10, 10, true, 5,  1, "Show the percentage of time spent by the CPU or CPUs to run a virtual processor.");
			md.addStatColumn("gnicePct",      11, 11, true, 5,  1, "Show the percentage of time spent by the CPU or CPUs to run a niced guest.");
			md.addStatColumn("idlePct",       12, 12, true, 5,  1, "Show the percentage of time that the CPU or CPUs were idle and the system did not have an outstanding disk I/O request.");

			md.setPercentCol("gnicePct");
		}
		else
		{
			md.addStrColumn( "CPU",            1,  2, false,   5, "Processor number. The keyword all indicates that statistics are calculated as averages among all processors.");
			md.addIntColumn( "samples",        2,  0, true,        "Number of 'sub' sample entries of iostat this value is based on");

			md.addStatColumn("usrPct",         3,  3, true, 5,  1, "Show the percentage of CPU utilization that occurred while executing at the user level (application).");
			md.addStatColumn("nicePct",        4,  4, true, 5,  1, "Show the percentage of CPU utilization that occurred while executing at the user level with nice priority.");
			md.addStatColumn("sysPct",         5,  5, true, 5,  1, "Show the percentage of CPU utilization that occurred while executing at the system level (kernel). Note that this does not include time spent servicing hardware and software interrupts.");
			md.addStatColumn("iowaitPct",      6,  6, true, 5,  1, "Show the percentage of time that the CPU or CPUs were idle during which the system had an outstanding disk I/O request.");
			md.addStatColumn("irqPct",         7,  7, true, 5,  1, "Show the percentage of time spent by the CPU or CPUs to service hardware interrupts.");
			md.addStatColumn("softPct",        8,  8, true, 5,  1, "Show the percentage of time spent by the CPU or CPUs to service software interrupts.");
			md.addStatColumn("stealPct",       9,  9, true, 5,  1, "Show the percentage of time spent in involuntary wait by the virtual CPU or CPUs while the hypervisor was servicing another virtual processor.");
			md.addStatColumn("guestPct",      10, 10, true, 5,  1, "Show the percentage of time spent by the CPU or CPUs to run a virtual processor.");
			md.addStatColumn("idlePct",       11, 11, true, 5,  1, "Show the percentage of time that the CPU or CPUs were idle and the system did not have an outstanding disk I/O request.");
		}

		// Set Percent columns
		md.setPercentCol("usrPct");
		md.setPercentCol("nicePct");
		md.setPercentCol("sysPct");
		md.setPercentCol("iowaitPct");
		md.setPercentCol("irqPct");
		md.setPercentCol("softPct");
		md.setPercentCol("stealPct");
		md.setPercentCol("guestPct");
		md.setPercentCol("idlePct");

		// Use "CPU" as the Primary Key, which is used to du summary/average calculations
		md.setPkCol("CPU");

		// Set column "samples", to a special status, which will contain number of 
		// underlying samples the summary/average caclulation was based on
		md.setStatusCol("samples",    HostMonitorMetaData.STATUS_COL_SUB_SAMPLE);

		// What regexp to use to split the input row into individual fields
		md.setParseRegexp(HostMonitorMetaData.REGEXP_IS_SPACE);

		// Skip the header line
		md.setSkipRows("CPU", "CPU");

		// Get SKIP and ALLOW from the Configuration
		md.setSkipAndAllowRows(null, Configuration.getCombinedConfiguration());
		md.setSkipAndAllowRows("hostmon.MonitorMpstat.", Configuration.getCombinedConfiguration());

		// create an object that can change the "raw" row after it has been read from the input, but before it gets split  
		md.setSourceRowAdjuster(new SourceRowAdjuster()
		{
			@Override
			public String adjustRow(String row, int type)
			{
				if (row == null)
					return row;

				// In some cases the PM/AM is NOT present... it seems to be optional... so lets remove it from the input.
				// from: 00:38:26 AM  all   10.66
				//   to: 00:38:26 all   10.66
				if ( row.length() > 12 && (row.startsWith(" AM ", 8) | row.startsWith(" PM ", 8)) )
					row = row.substring(0, 8) + row.substring(12);

				return row;
			}
		});

		return md;
	}
}
