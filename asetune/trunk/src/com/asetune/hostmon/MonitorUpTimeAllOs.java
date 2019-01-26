/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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

import org.apache.log4j.Logger;

import com.asetune.ssh.SshConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;


public class MonitorUpTimeAllOs
extends MonitorUpTime
{
	private static Logger _logger = Logger.getLogger(MonitorUpTimeAllOs.class);

	/*
	------------------------------------------
	SunOS sunspot 5.10 Generic_120011-14 sun4u sparc SUNW,Sun-Fire-V890
	sunspot% uptime
	------------------------------------------
  	8:46am  up 19 day(s), 21:36,  27 users,  load average: 0.49, 0.54, 0.56

	------------------------------------------
	AIX bluesky3 3 5 00C77C6F4C00
	% uptime
	------------------------------------------
  	08:38AM   up 41 days,  15:38,  13 users,  load average: 1.83, 1.66, 1.60

	------------------------------------------
	HP-UX potamus B.11.23 U 9000/800 1488144216 unlimited-user license
	[1] % uptime
	------------------------------------------
  	8:35am  up 404 days, 16:56,  7 users,  load average: 0.40, 0.37, 0.36

	------------------------------------------
	Linux sweiq-linux 2.6.18-274.3.1.el5 #1 SMP Tue Sep 6 20:13:52 EDT 2011 x86_64 x86_64 x86_64 GNU/Linux
	[ajackson@sweiq-linux ~]$ uptime
	------------------------------------------
 	17:35:12 up 22 days,  6:23,  1 user,  load average: 0.00, 0.01, 0.00

	------------------------------------------
	Linux gorans-ub 2.6.35-30-generic #54-Ubuntu SMP Tue Jun 7 18:40:23 UTC 2011 i686 GNU/Linux
	gorans@gorans-ub:~$ uptime
	------------------------------------------
 	19:48:46 up 218 days, 7 min,  7 users,  load average: 23.16, 23.05, 23.06


	------------------------------------------------------------------------------------
	-- Some summary (note: output is manually column formated for easier reading)
	------------------------------------------------------------------------------------
	Solaris: 8:46am    up 19  day(s), 21:36,  27 users,  load average: 0.49, 0.54, 0.56
	AIX:     08:38AM   up 41  days,   15:38,  13 users,  load average: 1.83, 1.66, 1.60
	HP:      8:35am    up 404 days,   16:56,  7  users,  load average: 0.40, 0.37, 0.36
	LINUX 1: 17:35:12  up 22  days,    6:23,  1  user,   load average: 0.00, 0.01, 0.00
 	LINUX 2: 19:48:46  up 218 days,   7 min,  7 users,   load average: 23.16, 23.05, 23.06   <<<< note: does not work due to '7 min', not one word
 	
 	Ubuntu:  10:49:32  up 273 days,    2:24,  2 users,   load average: 0,00, 0,00, 0,00      <<<< note: the 'load average' fields contains ',' instead of '.' for values...

 	RHEL:    17:45:24  up 48 days,     5:04,  1 user,    load average: 1.56, 4.28, 2.45
 	RHEL:    17:45:44  up              3:14,  1 user,    load average: 0.04, 0.04, 0.05      <<<< note: "### days" is blank if the machine has been restarted today...


	*/
	
	public MonitorUpTimeAllOs()
	{
		this(-1, null);
	}
	public MonitorUpTimeAllOs(int utilVersion, String utilExtraInfo)
	{
		super(utilVersion, utilExtraInfo);
	}

	@Override
	public String getModuleName()
	{
		return "MonitorUpTimeAllOs";
	}

	@Override
	public String getCommand()
	{
		String cmd = super.getCommand();
		return cmd != null ? cmd : "uptime";
	}

	@Override
	public HostMonitorMetaData createMetaData(int utilVersion, Configuration utilExtraInfo)
	{
		HostMonitorMetaData md = new HostMonitorMetaData();
		md.setTableName(getModuleName());

		md.addStrColumn("sampleTime",            1, 1, true, 10,    "Approximately when this record was samples");
                                                 
		md.addIntColumn("daysUp",                2, 3, true,        "OS has been up for this amount of days + the hours column");
		md.addStrColumn("hoursUp",               3, 5, true, 10,    "OS has been up for (the days olumn) + this time description");
		md.addIntColumn("users",                 4, 6, true,        "Number of users logged on to the OS right now");
                                                 
		md.addStatColumn("loadAverage_1Min",     5, 10, true, 8, 2, "Load Average for last minute");
		md.addStatColumn("loadAverage_5Min",     6, 11, true, 8, 2, "Load Average for last 5 minutes");
		md.addStatColumn("loadAverage_15Min",    7, 12, true, 8, 2, "Load Average for last 15 minutes");

		// Note: The below values will be filled in by CmOsUptime.localCalculation(OsTable osSampleTable)
		md.addIntColumn("nproc",                 8,  0, true,       "OS 'nproc' number of available processing units. -1=not yet available, 0=Command failed");
		md.addStatColumn("adjLoadAverage_1Min",  9,  0, true, 8, 2, "Load Average for last minute, divided by 'nproc' (number of processes)");
		md.addStatColumn("adjLoadAverage_5Min",  10, 0, true, 8, 2, "Load Average for last 5 minutes, divided by 'nproc' (number of processes)");
		md.addStatColumn("adjLoadAverage_15Min", 11, 0, true, 8, 2, "Load Average for last 15 minutes, divided by 'nproc' (number of processes)");

		//		// Set column "sampleTime", to a special status, which will contain the  
//		// underlying sample TIME the record was sampled
//		md.setStatusCol("sampleTime", HostMonitorMetaData.STATUS_COL_SAMPLE_TIME);

		// What regexp to use to split the input row into individual fields
		md.setParseRegexp(HostMonitorMetaData.REGEXP_IS_SPACE);

		md.setOsCommandStreaming(false);
		
//		// Skip the header line
//		md.setSkipRows("memory_swap", "swap");

//		// Get SKIP and ALLOW from the Configuration
//		md.setSkipAndAllowRows(null, Configuration.getCombinedConfiguration());
//		md.setSkipAndAllowRows("hostmon.MonitorVmstat.", Configuration.getCombinedConfiguration());

		return md;
	}

	@Override
	public String[] parseRow(HostMonitorMetaData md, String row, String[] preParsed, int type)
	{
		if (type == SshConnection.STDERR_DATA)
			return null;

		// Check preParsed for correct length: if not correct: print out some info for debugging.
		if (preParsed.length != md.getParseColumnCount())
		{
			// Can we figure out what's wrong here???
			// If so: try to fix it.

			// Are the "## days" missing (it has been restarted today)
			if (preParsed.length < md.getParseColumnCount())
			{
				// Typically 2 fields are missing, see below: ## days
				if (preParsed.length == md.getParseColumnCount()-2)
				{
					if (_logger.isDebugEnabled())
						_logger.debug(">>>>>>>> PRE-FIX(missing fields '## days') >>>>>>>> "+getModuleName()+".parseRow(): Checking preParsed column count fail. preParsed.length="+preParsed.length+", expectedCount="+md.getParseColumnCount()+", preParsed=["+StringUtil.toCommaStrQuoted('"', preParsed)+"].");

					// but instead the third column is "HH:MM"
					if (preParsed[2].matches("[0-9]?[0-9]:[0-9][0-9],")) // Note the ',' at the end
					{
						String[] tmp = new String[md.getParseColumnCount()];
						
						// RHEL:    17:45:24  up 48 days,     5:04,  1 user,    load average: 1.56, 4.28, 2.45
						// RHEL:    17:45:44  up              3:14,  1 user,    load average: 0.04, 0.04, 0.05      <<<< note: "### days" is blank if the machine has been restarted today...

						tmp[0]  = preParsed[0]; // current time
						tmp[1]  = preParsed[1]; // str 'up'
						tmp[2]  = "0";          // str '##'    --->>> Just fill in with '0'
						tmp[3]  = "days";       // str 'days'  --->>> Just fill in with 'days'
						tmp[4]  = preParsed[2]; // HH:MM -- hoursUp
						tmp[5]  = preParsed[3]; // # ------ users
						tmp[6]  = preParsed[4]; // str 'user'
						tmp[7]  = preParsed[5]; // str 'load'
						tmp[8]  = preParsed[6]; // str 'average' 
						tmp[9]  = preParsed[7]; // 1 minute
						tmp[10] = preParsed[8]; // 5 minutes
						tmp[11] = preParsed[9]; // 15 minutes
						
						preParsed = tmp;
					}
				}
			}
			
			// If "hours" is smaller than a full hour... then uptime will produce "7 min" instead of "HH:MM"... meaning we will have 1 extra field...
			// Rewrite this into "0:07" and remove that extra field from the data array 
			else if (preParsed.length > md.getParseColumnCount())
			{
				if (_logger.isDebugEnabled())
					_logger.debug(">>>>>>>> PRE-FIX(field='min,') >>>>>>>> "+getModuleName()+".parseRow(): Checking preParsed column count fail. preParsed.length="+preParsed.length+", expectedCount="+md.getParseColumnCount()+", preParsed=["+StringUtil.toCommaStrQuoted('"', preParsed)+"].");
				
				// if there is one extra field, due to "7 min" instead of "HH:MM"
				// Remove that field, and "fix" the "7 min" -> "00:07"
				if ("min,".equals(preParsed[5]) || "min(s),".equals(preParsed[5])) // min(s) = Solaris
				{
				                                 // 19:48:46  up 218 days,   7 min,  7 users,   load average: 23.16, 23.05, 23.06
				                                 //        0   1   2     3   4    5  6      7      8        9     10     11    12
				                                 //                          ^ ^^^^
					// Col 4 will contain [#]# as minutes... format as "##" (1->01, 9->09, 10->10)
					String minuteSpec = preParsed[4];
					if (minuteSpec.length() == 1)
						minuteSpec = "0" + minuteSpec;

					// FIXME: Just figgured out that Solaris behaives in the same manner... but at Hours
					// See below for example...
					//    3:50pm  up 644 day(s), 58 min(s),  1 user,  load average: 0.00, 0.00, 0.00
					//    3:52pm  up 644 day(s), 1 hr(s),  1 user,  load average: 0.00, 0.00, 0.00
					//    3:54pm  up 644 day(s),  1:02,  1 user,  load average: 0.00, 0.00, 0.00
					//    4:50pm  up 644 day(s),  1:58,  1 user,  load average: 0.00, 0.00, 0.00
					//    4:52pm  up 644 day(s), 2 hr(s),  1 user,  load average: 0.00, 0.00, 0.00
					//    4:54pm  up 644 day(s),  2:02,  1 user,  load average: 0.00, 0.00, 0.00
					//    5:50pm  up 644 day(s),  2:58,  1 user,  load average: 0.00, 0.00, 0.00
					//    5:52pm  up 644 day(s), 3 hr(s),  1 user,  load average: 0.00, 0.00, 0.00
					//    5:54pm  up 644 day(s),  3:02,  1 user,  load average: 0.00, 0.00, 0.00
					// NOTE: fix this at a later stage
					
					// Copy fields into correct place (array pos 5 will be skipped, as it contains "min,")
					String[] tmp = new String[preParsed.length - 1];
					tmp[0]  = preParsed[0];
					tmp[1]  = preParsed[1];
					tmp[2]  = preParsed[2];
					tmp[3]  = preParsed[3];
					tmp[4]  = "0:" + minuteSpec;
					//       preParsed[5]; // Skip column 5, which will be 'min,'
					tmp[5]  = preParsed[6];
					tmp[6]  = preParsed[7];
					tmp[7]  = preParsed[8];
					tmp[8]  = preParsed[9];
					tmp[9]  = preParsed[10];
					tmp[10] = preParsed[11];
					tmp[11] = preParsed[12];
					
					preParsed = tmp;

					if (_logger.isDebugEnabled())
						_logger.debug(">>>>>>>> POST-FIX(field='min,') >>>>>>>> "+getModuleName()+".parseRow(): preParsed.length="+preParsed.length+", expectedCount="+md.getParseColumnCount()+", preParsed=["+StringUtil.toCommaStrQuoted('"', preParsed)+"].");
				}
			}

			// Finally check if we fixed the issue
			if (preParsed.length != md.getParseColumnCount())
				_logger.warn(getModuleName()+".parseRow(): Checking preParsed column count fail. preParsed.length="+preParsed.length+", expectedCount="+md.getParseColumnCount()+", preParsed=["+StringUtil.toCommaStrQuoted('"', preParsed)+"].");
		}

// Note: nproc will be filled in by CmOsUptime.localCalculation(OsTable osSampleTable)
//		int nproc = getConnection().getNproc();
//
//		// Add data for 'nproc' as the last column to preParsed values, which we get from SSH Connection. (only executed when the SSH Connects first time)
//		// Copy "preParsed" array into new "tmp" array
//		// Then add last entry with value of nproc
//		String[] tmp = new String[preParsed.length + 1];
//		System.arraycopy(preParsed, 0, tmp, 0, preParsed.length);
//		tmp[tmp.length - 1] = String.valueOf(nproc);
//		
//		preParsed = tmp;

		String[] data = super.parseRow(md, row, preParsed, type);
		if (data == null)
			return null;

		// Strip off *trailing* ',' characters in ALL fields
		for (int i=0; i<data.length; i++)
			if (data[i].endsWith(","))
				data[i] = data[i].substring(0, data[i].length()-1);

		return data;
	}

}
