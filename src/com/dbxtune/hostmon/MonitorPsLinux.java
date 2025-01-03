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

import com.dbxtune.cm.os.CmOsPs;
import com.dbxtune.ssh.SshConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.VersionShort;

public class MonitorPsLinux
extends HostMonitor
{
//	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public MonitorPsLinux()
	{
		this(-1, null);
	}
	public MonitorPsLinux(int utilVersion, String utilExtraInfo)
	{
		super(utilVersion, utilExtraInfo);
	}

	@Override
	public String getModuleName()
	{
		return "MonitorPsLinux";
	}

	@Override
	public String getCommand()
	{
		String cmd = super.getCommand();
		
		if (StringUtil.hasValue(cmd))
			return cmd;
		
		int top = Configuration.getCombinedConfiguration().getIntProperty(CmOsPs.PROPKEY_top, CmOsPs.DEFAULT_top);

		// NOTE: 'etimes' is not available in RHEL 6.6 (possible version 6)
		//       'etimes' was introduced in version 3.3 according to -- https://abi-laboratory.pro/?view=changelog&l=procps-ng&v=3.3.12
		if ( getUtilVersion() < VersionShort.toInt(3,3,0))
		{
			return "ps -e -ww --format pid,ppid,euser,tty,vsz,rss,cputime,pmem,pcpu,args --sort=-pcpu | head -n " + top;
		}
		else
		{
			return "ps -e -ww --format pid,ppid,euser,tty,vsz,rss,etimes,cputime,pmem,pcpu,args --sort=-pcpu | head -n " + top;
		}

	}

	@Override
	public HostMonitorMetaData createMetaData(int utilVersion, Configuration utilExtraInfo)
	{
		HostMonitorMetaData md = new HostMonitorMetaData();
		md.setTableName(getModuleName());

		if ( utilVersion < VersionShort.toInt(3,3,0))
		{
			// NOTE: 'etimes' was introduced in version 3.3 (so it's skipped here)
			md.addIntColumn ("pid",         1,  1,  false,        "a number representing the process ID");
			md.addIntColumn ("ppid",        2,  2,  false,        "parent process ID");
			md.addStrColumn ("user",        3,  3,  false, 20,    "effective user name.  This will be the textual user ID, if it can be obtained and the field width permits, or a decimal representation otherwise.");
			md.addStrColumn ("tty",         4,  4,  false, 10,    "controlling tty (terminal)");
			md.addIntColumn ("vsize",       5,  5,  false,        "virtual memory size of the process in KiB (1024-byte units).  Device mappings are currently excluded; this is subject to change.");
			md.addIntColumn ("rss",         6,  6,  false,        "resident set size, the non-swapped physical memory that a task has used (in kiloBytes).");
			md.addStrColumn ("cputime",     7,  7,  false, 15,    "cumulative CPU time, '[DD-]hh:mm:ss' format.");
			md.addDecColumn ("%mem",        8,  8,  false, 5, 1,  "ratio of the process's resident set size  to the physical memory on the machine, expressed as a percentage.");
			md.addDecColumn ("%cpu",        9,  9,  false, 5, 1,  "cpu utilization of the process in ##.# format. Currently, it is the CPU time used divided by the time the process has been running (cputime/realtime ratio), expressed as a percentage.  It will not add up to 100% unless you are lucky.");
			md.addStrColumn ("command",     10, 10, false, 4096,  "command with all its arguments as a string. Modifications to the arguments may be shown.  The output in this column may contain spaces.  A process marked <defunct> is partly dead, waiting to be fully destroyed by its parent.  Sometimes the process args will be unavailable; when this happens, ps will instead print the executable name in brackets.  (alias cmd, command).  See also the comm format keyword, the -f option, and the c option.");
		}
		else
		{
			md.addIntColumn ("pid",         1,  1,  false,        "a number representing the process ID");
			md.addIntColumn ("ppid",        2,  2,  false,        "parent process ID");
			md.addStrColumn ("user",        3,  3,  false, 20,    "effective user name.  This will be the textual user ID, if it can be obtained and the field width permits, or a decimal representation otherwise.");
			md.addStrColumn ("tty",         4,  4,  false, 10,    "controlling tty (terminal)");
			md.addIntColumn ("vsize",       5,  5,  false,        "virtual memory size of the process in KiB (1024-byte units).  Device mappings are currently excluded; this is subject to change.");
			md.addIntColumn ("rss",         6,  6,  false,        "resident set size, the non-swapped physical memory that a task has used (in kiloBytes).");
			md.addIntColumn ("etimes",      7,  7,  false,        "elapsed time since the process was started, in seconds."); // 'etimes' was introduced in version 3.3
			md.addStrColumn ("cputime",     8,  8,  false, 15,    "cumulative CPU time, '[DD-]hh:mm:ss' format.");
			md.addDecColumn ("%mem",        9,  9,  false, 5, 1,  "ratio of the process's resident set size  to the physical memory on the machine, expressed as a percentage.");
			md.addDecColumn ("%cpu",        10, 10, false, 5, 1,  "cpu utilization of the process in ##.# format. Currently, it is the CPU time used divided by the time the process has been running (cputime/realtime ratio), expressed as a percentage.  It will not add up to 100% unless you are lucky.");
			md.addStrColumn ("command",     11, 11, false, 4096,  "command with all its arguments as a string. Modifications to the arguments may be shown.  The output in this column may contain spaces.  A process marked <defunct> is partly dead, waiting to be fully destroyed by its parent.  Sometimes the process args will be unavailable; when this happens, ps will instead print the executable name in brackets.  (alias cmd, command).  See also the comm format keyword, the -f option, and the c option.");
		}


		// What regexp to use to split the input row into individual fields
		md.setParseRegexp(HostMonitorMetaData.REGEXP_IS_SPACE);

		md.setOsCommandStreaming(false);
		
		md.setPercentCol("%mem", "%cpu");

		md.setPkCol("pid");
		
//		md.setDiffCol( "Used-KB", "Available-KB", "Used-MB", "Available-MB" );

		// Skip the header line
		md.setSkipRows("pid", "PID");

//		// Get SKIP and ALLOW from the Configuration
//		md.setSkipAndAllowRows(null, Configuration.getCombinedConfiguration());
//		md.setSkipAndAllowRows("hostmon.MonitorVmstat.", Configuration.getCombinedConfiguration());

		return md;
	}

	/**
	 * The last column "cmd/args" may have *many* arguments... so try to "wrap up" everything in the column "cmd/args" as a single field 
	 */
	@Override
	public String[] parseRow(HostMonitorMetaData md, String row, String[] preParsed, int type)
	{
		if (type == SshConnection.STDERR_DATA)
			return null;
//System.out.println("XXX row=|" + row + "|");
		
		// Now concatenate everything for the "cmd/args" field (if there are parameters)
		if (preParsed.length > md.getParseColumnCount())
		{
//System.out.println("  -- preParsed.length=" + preParsed.length + ", md.getParseColumnCount()=" + md.getParseColumnCount());
			String[] allCols = new String[md.getParseColumnCount()];

			// Copy everything 
			String cmdArgs = "";
			for (int i=md.getParseColumnCount()-1; i<preParsed.length; i++)
			{
				cmdArgs += preParsed[i] + " ";
//				System.out.println("    ++ preParsed[" + i + "]=|" + preParsed[i] + "|, cmdArgs=|" + cmdArgs + "|");
			}
			
			// Copy all "pre" columns (or columns to left of "cmd/args")
			for (int i=0; i<allCols.length; i++)
				allCols[i] = preParsed[i];

			// Then set all the concatenated columns into "cmd/args"
			allCols[allCols.length-1] = cmdArgs.trim();
			
			// Assign the preParsed to the "above fix"
			preParsed = allCols;
		}

		// Let "super" do it's intended work
		String[] data = super.parseRow(md, row, preParsed, type);
		if (data == null)
			return null;

		return data;
	}

//	@Override
//	public String[] parseRow(HostMonitorMetaData md, String row, String[] preParsed, int type)
//	{
//		if (type == SshConnection.STDERR_DATA)
//			return null;
//
//		// Let "super" do it's intended work
//		String[] data = super.parseRow(md, row, preParsed, type);
//		if (data == null)
//			return null;
//
//		// Strip off *trailing* '%' characters in 'UsedPct' fields
//		int usedPct_pos = 4;
//		if (usedPct_pos < data.length)
//		{
//			if (data[usedPct_pos].endsWith("%"))
//				data[usedPct_pos] = data[usedPct_pos].substring(0, data[usedPct_pos].length()-1);
//		}
//
//		return data;
//	}
	
	
}
