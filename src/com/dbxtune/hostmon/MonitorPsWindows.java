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
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

public class MonitorPsWindows
extends HostMonitor
{
//	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public MonitorPsWindows()
	{
		this(-1, null);
	}
	public MonitorPsWindows(int utilVersion, String utilExtraInfo)
	{
		super(utilVersion, utilExtraInfo);
	}

	@Override
	public String getModuleName()
	{
		return "MonitorPsWindows";
	}

	@Override
	public String getCommand()
	{
		String cmd = super.getCommand();
		
		if (StringUtil.hasValue(cmd))
			return cmd;
		
		int top = Configuration.getCombinedConfiguration().getIntProperty(CmOsPs.PROPKEY_top, CmOsPs.DEFAULT_top);

		String discNullCpu = " | Where-Object { $_.CPU -ne $null -and $_.CPU -gt 0 }";   // Get rid of processes that has NO CPU Usage
		String sort        = " | Sort-Object CPU -Descending";         // Sort by CPU
		String topProcs    = " | Select -First " + top;                // Only get first X records
		String autoFormat  = " | Format-Table -AutoSize";              // AutoSize to NOT truncate cell content (like 'NPM(K)' sometimes get ...###)

		if (top <= 0)
		{
			topProcs = "";
		}

		// NOTE: [cultureinfo]::CurrentCulture = 'en-US'; will force the "CPU(s)" output to be: ###,###.## instead of some localized string ex: ### ###,##
		//       if the localized string is returned... parsing may of numbers may fail!
		return "powershell \"" // -start- quote for embedded command
				+ "(Get-Culture).NumberFormat.NumberGroupSeparator=''; "     // numbers should not have comma separators for readability
				+ "(Get-Culture).NumberFormat.NumberDecimalSeparator='.'; "  // numbers should have "." separator for ####.12
				+ "Get-Process" + discNullCpu + sort + topProcs + autoFormat
				+ "\""; // -end- quote for embedded command
		
		// or Get-Process | Get-Member ... to get ALL Members and get specific counter members
		// or possibly: tasklist
		// or powershell Get-WMIObject Win32_Process
		// or sysinternals: pslist
		
		// more on PWSH... possibly output to | ConvertTo-Csv -NoTypeInformation
		// and select some "columns"... Get-Process | Where-Object { $_.CPU -ne $null } | Sort-Object CPU -Descending | Select-Object Id, SessionId, StartTime, ProcessName, CPU, HandleCount, Threads, BasePriority, PriorityClass, ProcessorAffinity, MaxWorkingSet, MinWorkingSet, NonpagedSystemMemorySize64, PagedMemorySize64, PagedSystemMemorySize64, PeakPagedMemorySize64, PeakVirtualMemorySize64, PeakWorkingSet64, PrivateMemorySize64, VirtualMemorySize64, WorkingSet64, PrivilegedProcessorTime, TotalProcessorTime, UserProcessorTime, Company, FileVersion, Path, Product, ProductVersion | select -first 10 | ConvertTo-Csv -NoTypeInformation
		// NOTE: If we change to 'Select-Object' we can also get 'StartTime' and if its a 'typeperf' process that has been running for a "long" time, lets kill it (because they seems to be laying around after we do disconnect, The SSHD does not kill it's subprocesses on exit...)
	}

	@Override
	public HostMonitorMetaData createMetaData(int utilVersion, Configuration utilExtraInfo)
	{
		HostMonitorMetaData md = new HostMonitorMetaData();
		md.setTableName(getModuleName());

		md.addIntColumn("Handles",      1,  1, false,        "The number of handles that the process has opened.");
		md.addIntColumn("NPM(K)",       2,  2, false,        "The amount of non-paged memory that the process is using, in kilobytes");
		md.addIntColumn("PM(K)",        3,  3, false,        "The amount of pageable memory that the process is using, in kilobytes");
		md.addIntColumn("WS(K)",        4,  4, false,        "The size of the working set of the process, in kilobytes. The working set consists of the pages of memory that were recently referenced by the process.");
//		md.addIntColumn("VM(M)",        5,  5, false,        "The amount of virtual memory that the process is using, in megabytes. Virtual memory includes storage in the paging files on disk.");
		md.addDecColumn("CPU(s)",       5,  5, false, 20, 2, "The amount of processor time that the process has used on all processors, in seconds.");
		md.addStrColumn("CpuTimeHms",   6, -1, false, 20,    "CPU(s) transformed into #d #h:#m:#s.#ms so it's easier to read ohow many CPU Seconds it really is");
		md.addIntColumn("Id",           7,  6, false,        "The process ID (PID) of the process.");
		md.addIntColumn("SI",           8,  7, false,        "This is the Session ID. Session 0 is shown for all services, Session 1 for first logged on user and 2 because you switch from user 1 to a new user logon.");
		md.addStrColumn("ProcessName",  9,  8, false, 4096,  "The name of the process. For explanations of the concepts related to processes, see the Glossary in Help and Support Center and the Help for Task Manager.");
//		md.addStrColumn("Description", 10, -1, true , 4096,  "Description of the process name.");

		// NOTE: 'CpuTimeHms' is calculated in CmOsPs.localCalculation(OsTable thisSample)

		// What regexp to use to split the input row into individual fields
		md.setParseRegexp(HostMonitorMetaData.REGEXP_IS_SPACE);

		md.setOsCommandStreaming(false);
		
//		md.setPercentCol("pmem", "pcpu");

		md.setPkCol("Id");
		
//		md.setDiffCol( "Used-KB", "Available-KB", "Used-MB", "Available-MB" );
		md.setDiffCol( "Handles", "NPM(K)", "PM(K)", "WS(K)", "CPU(s)" );

		// Skip the header line
		md.setSkipRows("Handles", "Handles");
		md.setSkipRows("Handles", "-------");

//		// Get SKIP and ALLOW from the Configuration
//		md.setSkipAndAllowRows(null, Configuration.getCombinedConfiguration());
//		md.setSkipAndAllowRows("hostmon.MonitorVmstat.", Configuration.getCombinedConfiguration());

		return md;
	}

//	C:\>powershell "Get-Process | sort -desc cpu | select -first 30"
//
//	Handles  NPM(K)    PM(K)      WS(K)     CPU(s)     Id  SI ProcessName
//	-------  ------    -----      -----     ------     --  -- -----------
//	    333      19   157956      84980  17,848.02  13624   1 chrome
//	    437      27   614064     203092  15,146.52   7536   1 Binance
//	    761      39    22148      28044   7,812.23  15888   1 f5fpclientW
//	   1633     184   339608     286900   6,556.61  23092   1 Discord
//	   2469     165  2243824     503500   5,670.88  18208   1 chrome
//	    341      20    71828      65572   4,972.58  12684   1 Binance
//	    665      54    55996      15008   3,664.38  16492   1 atmgr
//	    893      33   263868      47996   3,644.38   6068   1 Teams
//	    751      32   425000     159680   3,354.13  16628   1 Binance
//	   2261      52   183068      94836   3,321.44  15524   1 Teams
//	    655      52    41104      13872   3,315.30  16448   1 atmgr
//	  15312     235  1262340     416668   3,303.17  21364   1 OUTLOOK
//	  17614     198   366824     355820   3,199.84  17248   1 chrome
//	   1587      57   265792      93728   2,976.95  13872   1 Teams
//	    533      19   801036      67680   2,739.50  14928   1 chrome
//	    386      20    62212      48836   2,609.63   5972   1 Binance
//	    518      53   360388     248168   2,221.11  19604   1 Teams
//	    508      31    20392      24896   1,860.08   7988   1 Binance
//	1361051      45  1104956      94888   1,859.14   8536   1 Binance
//	   2036     241  1414080     113620   1,825.17  38104   1 javaw
//	    431      24     6748       6224   1,328.39  16768   1 vpnui
//	    520      19   792672      51968   1,093.63  31424   1 chrome
//	   4384     700   213100     234308     987.13  48536   1 explorer
//	    475      22     8292      11300     945.77  16392   1 ciscowebexstart
//	   2032      70    52564      54032     863.19   6712   1 Discord
//	    591      42    38772      27140     771.95  14396   1 LogiOptionsMgr
//	    522      38    46296      53412     699.05  18260   1 chrome
//	    371      21    92976      27828     665.02  34600   1 chrome
//	    360      31   209332     114828     553.92  23440   1 Teams
//	    373      18   356992     145448     461.42  36980   1 chrome

}
