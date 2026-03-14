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

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cm.os.CmOsPs;
import com.dbxtune.hostmon.HostMonitorConnection.ExecOutput;
import com.dbxtune.ssh.SshConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.FileUtils;
import com.dbxtune.utils.JsonUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;
import com.dbxtune.utils.VersionShort;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


public class MonitorPsProcFsLinux
extends HostMonitor
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private static final String LOCAL_FILE  = "scripts/dbxtune_get_process_stats.py";
	private static final String REMOTE_FILE = "/tmp/dbxtune_get_process_stats.${USER}.py";
	// NOTE: remote command, the ${USER} will be expanded to current user on the Linux side, so each user has it's own script. 
	//       This so that we can overwrite our own script when a new version of the script is needed
	//       We can't share the same file under /tmp/ even if "anyone" can write to it (chmod 666 or 777)
	
	private String _pythonCmd = null;

//	private static final int MONITOR_TYPE = 2;
//	private int MONITOR_TYPE = 2;
	
	public MonitorPsProcFsLinux()
	{
		this(-1, null);
	}
	public MonitorPsProcFsLinux(int utilVersion, String utilExtraInfo)
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
		
//		int top = Configuration.getCombinedConfiguration().getIntProperty(CmOsPs.PROPKEY_top, CmOsPs.DEFAULT_top);
		boolean useSudo = Configuration.getCombinedConfiguration().getBooleanProperty(CmOsPs.PROPKEY_linux_useSudo, CmOsPs.DEFAULT_linux_useSudo);
		String sudo = useSudo ? "sudo " : "";

		return sudo + _pythonCmd + " " + REMOTE_FILE;
	}

	@Override
	public HostMonitorMetaData createMetaData(int utilVersion, Configuration utilExtraInfo)
	{
		HostMonitorMetaData md = new HostMonitorMetaData();
		md.setTableName(getModuleName());

		md.setOsCommandStreaming(false);
		
		// Testing different Versions
//		if (MONITOR_TYPE == 1)
//		{
//			// Using ABS counters and do DIFF/RATE calculation of some columns
//			// All 3 collections will be available ABS/DIFF/RATE (possibly storing more information than we need)
//			// NOTE: We can do *filtering* on rows with "low" CPU here... (Since it will "swap" in and out of data, when CPU is above/less than # CPU)
//			//       and that will case "diff/rate" calculations to be OFF on those records
//			// So probably NOT the best solution
//			md.addIntColumn ("pid"           ,  1,  1, false,        "a number representing the process ID");
//			md.addIntColumn ("ppid"          ,  2,  2, false,        "parent process ID");
//			md.addStrColumn ("user"          ,  3,  3, false, 20,    "effective user name.  This will be the textual user ID, if it can be obtained and the field width permits, or a decimal representation otherwise.");
//			md.addStrColumn ("start_time"    ,  4,  4, false, 25,    "At what time was the process started.");
//			md.addIntColumn ("threads"       ,  5,  5, false,        "How many LWP's are this process using.");
//			md.addIntColumn ("memoryMb"      ,  6,  6, false,        "resident set size, the non-swapped physical memory that a task has used (in MegaBytes).");
//			md.addDecColumn ("%mem"          ,  7,  7, false, 5, 1,  "ratio of the process's resident set size  to the physical memory on the machine, expressed as a percentage.");
//			md.addDecColumn ("%cpu"          ,  8,  8, false, 5, 1,  "Cpu Percent Used... Note: This can show more than 100% if process is using more than one thread. Algorithm: CpuTimeMs * 100 / sample time.");
//			md.addDecColumn ("%cpuAdj"       ,  9,  9, false, 5, 1,  "Cpu Adjusted Percent Used... Note: This should NOT go above 100%. Algorithm: %cpu / cores");
//			md.addStrColumn ("cpuTime"       , 10, 10, false, 15,    "cumulative CPU time, '[DD-]hh:mm:ss.ms' format.");
//			md.addLongColumn("cpuTimeMs"     , 11, 11, false,        "cumulative CPU time (system+user) in milliseconds");
//			md.addLongColumn("userCpuMs"     , 12, 12, false,        "cumulative User CPU time in milliseconds");
//			md.addLongColumn("systemCpuMs"   , 13, 13, false,        "cumulative System CPU time in milliseconds");
//			md.addDecColumn ("%ioWait"       , 14, 14, false, 5, 1,  "How much did this process wait for IO.");
//			md.addLongColumn("ioWaitMs"      , 15, 15, false,        "Waiting for IO in milliseconds");
//			md.addIntColumn ("ioOk"          , 16, 16, false,        "If we had permission to view /proc/PID/io counters for *other* users. can be fixed with 'sudo' hack: on remote add file '/etc/sudoers.d/#USERNAME#' with the content '#USERNAME# ALL=(ALL) NOPASSWD:python3 /tmp/dbxtune_get_process_stats.#USERNAME#.py'. NOTE: Replace all '#USERNAME#' with the username you connect as, possibly 'dbxtune'. Also set property '" + CmOsPs.PROPKEY_linux_useSudo + "=true'.");
//			md.addLongColumn("totalIoOps"    , 17, 17, false,        "How many IO's did we do");
//			md.addLongColumn("ioReads"       , 18, 18, false,        "How many READ IO's did we do");
//			md.addLongColumn("ioWrites"      , 19, 19, false,        "How many WRITE IO's did we do");
//			md.addLongColumn("ioKbTotal"     , 20, 20, false,        "How many KB did we read/write");
//			md.addLongColumn("ioKbRead"      , 21, 21, false,        "How many KB did we read");
//			md.addLongColumn("ioKbWrite"     , 22, 22, false,        "How many KB did we write");
//			md.addDecColumn ("avgIoSizeKb"   , 23, 23, false, 10, 1, "Average IO Size In KB");
//			md.addDecColumn ("avgRIoSizeKb"  , 24, 24, false, 10, 1, "Average Read IO Size In KB");
//			md.addDecColumn ("avgWIoSizeKb"  , 25, 25, false, 10, 1, "Average Write IO Size In KB");
//			md.addLongColumn("ctxSwitches"   , 26, 26, false,        "Context Switches, both Voluntary and Involuntary");
//			md.addLongColumn("ctxVoluntary"  , 27, 27, false,        "Processen is waiting on something (ex. disk-I/O or Network)");
//			md.addLongColumn("ctxInvoluntary", 28, 28, false,        "Kernel forced the process to pause, to make room for something else (indicates CPU saturation)");
//			md.addStrColumn ("shortCommand"  , 29, 29, false, 256,   "Command line, in short format.");
//			md.addStrColumn ("fullCommand"   , 30, 30, false, 4096,  "command with all its arguments as a string. Modifications to the arguments may be shown.  The output in this column may contain spaces.  A process marked <defunct> is partly dead, waiting to be fully destroyed by its parent.  Sometimes the process args will be unavailable; when this happens, ps will instead print the executable name in brackets.  (alias cmd, command).  See also the comm format keyword, the -f option, and the c option.");
//
//			md.setPercentCol("%mem", "%cpu", "%cpuAdj", "%ioWait");
//
//			md.setDiffCol( "cpuTimeMs", "userCpuMs", "systemCpuMs", 
//					"ioWaitMs", "totalIoOps", "ioReads", "ioWrites", "ioKbTotal", "ioKbRead", "ioKbWrite", 
//					"ctxSwitches", "ctxVoluntary", "ctxInvoluntary" );
//
//			md.setPkCol("pid");
//		}

//TODO; // In all the SpaceUsedPredictions ... add getChartFromDbxCentral on Usage... So we can calculate on a 30 days period (possible a separate section)
		// https://dbxtune.maxm.se/api/graph/data?sessionName=MM-OP-JVSDB&cmName=CmOsDiskSpace&graphName=FsAvailableMb&startTime=30d
		// https://dbxtune.maxm.se/api/graph/data?sessionName=MM-OP-JVSDB&cmName=CmOsDiskSpace&graphName=FsAvailableMb&startTime=30d&sampleType=MIN_OVER_MINUTES&sampleValue=30
		// OR: Create a scheduled task on DbxCentral that kicks of AFTER "disk/pcs-cleanup" that does it *directly* from the Raw Graph Data...
		// OR: a Servlet in DbxCentral that gets data from the Graph Tables (basically the same thing as getting the JSON from 'https://dbxtu...') and produces HTML... (then we can also create a report "on the fly")
//TODO; In SpaceUsedPredictions column 'Data Points' tooltip (or a new column) that describes the start/end date (or hours) for the examined period

//		if (MONITOR_TYPE == 2)
//		{
			String ioWarningNote = ""
					+ "<br>"
					+ "<br><b>Warning:</b> For most DBMS Processes, don't trust the counters at a process level... Instead use the <b>iostat</b> tab/collector."
					+ "<br>"
					+ "<br><b>Note:</b> Values will be <b>off</b> in case the process is issuing <b>Async IO</b> using kernel code <code>io_submit</code> or similar to submit IOs. Hence Linux counter 'syscr/syscw' is NOT incremented"
					+ "<br>So if 'ioPerSec', 'ioReads/s' or 'ioWrites/s' is ZERO, then read the 'ioKb/s', 'ioKbRead/s' and 'ioKbWrite/s' to (at least) see how much we have read/written in KB."
					;

			// Only data in the ABS table
			// PreCalculate data and only show PerSecond counters (or similar)
			// Positive: we can filter out records earlier (processes that uses less CPU than # Percent (this due to that raw counter data is stored at a "lower" level, and diff data maintained for Processes that is NOT within the CPU Percent filter range)
			// Positive: Less data stored in the PCS Table
			// Negative: Could be harder to read sine we are mixing ABS and RATE values in the same table
			md.addIntColumn ("pid"             ,  1,  1, false,        "Process ID");
			md.addIntColumn ("ppid"            ,  2,  2, false,        "Parent process ID");
			md.addStrColumn ("user"            ,  3,  3, false, 20,    "Effective user name.");
			md.addStrColumn ("start_time"      ,  4,  4, false, 25,    "At what time was the process started.");
			md.addIntColumn ("threads"         ,  5,  5, false,        "How many LWP's are this process using.");
			md.addIntColumn ("memoryMb"        ,  6,  6, false,        "Resident set size, the non-swapped physical memory that a task has used (in MegaBytes).");
			md.addDecColumn ("%mem"            ,  7,  7, false, 5, 1,  "Ratio of the process's resident set size to the physical memory on the machine, expressed as a percentage.");
			md.addDecColumn ("%cpuAdj"         ,  8,  8, false, 5, 1,  "Cpu Adjusted Percent Used (User + System) / cores... Note: This should NOT go above 100%. Algorithm: %cpu / cores");
			md.addDecColumn ("%cpu"            ,  9,  9, false, 5, 1,  "Cpu (User + System) Percent Used... Note: This can show more than 100% if process is using more than one thread. Algorithm: CpuTimeMs * 100 / sample time.");
			md.addDecColumn ("%cpuUsr"         , 10, 10, false, 5, 1,  "User Cpu Percent Used...   Note: This can show more than 100% if process is using more than one thread. Algorithm: UserCpuTimeMs * 100 / sample time.");
			md.addDecColumn ("%cpuSys"         , 11, 11, false, 5, 1,  "System Cpu Percent Used... Note: This can show more than 100% if process is using more than one thread. Algorithm: SystemCpuTimeMs * 100 / sample time.");
			md.addStrColumn ("cpuTime"         , 12, 12, false, 15,    "Cumulative CPU time, '[DD-]hh:mm:ss.ms' format.");
//			md.addDecColumn ("cpuTimeMs"       , ? , ? , false, 10,3,  "RATE: CPU time (system+user) in milliseconds");
//			md.addDecColumn ("userCpuMs"       , ? , ? , false, 10,3,  "Cumulative User CPU time in milliseconds");
//			md.addDecColumn ("systemCpuMs"     , ? , ? , false, 10,3,  "Cumulative System CPU time in milliseconds");
			md.addDecColumn ("%ioWait"         , 13, 13, false, 5, 1,  "How much did this process wait for IO.");
			md.addLongColumn("totalIoWaitMs"   , 14, 14, false,        "TOTAL Waiting for IO in milliseconds, since the process started.");
			md.addIntColumn ("ioOk"            , 15, 15, false,        "<html>If we had permission to view /proc/PID/io counters for *other* users.<br>Can be fixed with 'sudo' hack: <br>&nbsp;&nbsp;&bull; On remote add file: <code>/etc/sudoers.d/<b>USERNAME</b></code> <br>&nbsp;&nbsp;&bull; With the content: <code><b>USERNAME</b> ALL=(ALL) NOPASSWD:python3 /tmp/dbxtune_get_process_stats.<b>USERNAME</b>.py</code>. <br><br>NOTE: Replace all <b>USERNAME</b> with the username you connect as, possibly 'dbxtune'. <br>Also set property <code>" + CmOsPs.PROPKEY_linux_useSudo + "=true</code></html>");
			md.addDecColumn ("ioKb/s"          , 16, 16, false, 10, 1, "<html>How many KB did we read/write per second<br>Algorithm: ((diff_ioReadBytes + diff_ioWriteBytes) / samplePeriodInSec) / 1024.0</html>");
			md.addDecColumn ("ioKbRead/s"      , 17, 17, false, 10, 1, "<html>How many KB did we read per second<br>Algorithm: diff_ioReadBytes / samplePeriodInSec / 1024.0</html>");
			md.addDecColumn ("ioKbWrite/s"     , 18, 18, false, 10, 1, "<html>How many KB did we write per second<br>Algorithm: diff_ioWriteBytes / samplePeriodInSec / 1024.0</html>");
			md.addDecColumn ("ioPerSec"        , 19, 19, false, 10, 1, "<html>IO Operations Per Second<br>Algorithm: (diff_ioSyscr + diff_ioSyscw) / samplePeriodInSec" + ioWarningNote + "</html>");
			md.addDecColumn ("ioReads/s"       , 20, 20, false, 10, 1, "<html>How many READ IO's did we do per second<br>Algorithm: diff_ioSyscr / samplePeriodInSec" + ioWarningNote + "</html>");
			md.addDecColumn ("ioWrites/s"      , 21, 21, false, 10, 1, "<html>How many WRITE IO's did we do per second<br>Algorithm: diff_ioSyscw / samplePeriodInSec" + ioWarningNote + "</html>");
			md.addDecColumn ("avgIoSizeKb"     , 22, 22, false, 10, 1, "<html>Average IO Size In KB in last sample period<br>Algorithm: ((diff_ioReadBytes / diff_ioSyscr) + (diff_ioWriteBytes / diff_ioSyscw)) / 1024.0" + ioWarningNote + "</html>");
			md.addDecColumn ("avgRIoSizeKb"    , 23, 23, false, 10, 1, "<html>Average Read IO Size In KB in last sample period<br>Algorithm: diff_ioReadBytes / diff_ioSyscr / 1024.0" + ioWarningNote + "</html>");
			md.addDecColumn ("avgWIoSizeKb"    , 24, 24, false, 10, 1, "<html>Average Write IO Size In KB in last sample period<br>Algorithm: diff_ioWriteBytes / diff_ioSyscw / 1024.0" + ioWarningNote + "</html>");
			md.addDecColumn ("ctxSwitches/s"   , 25, 25, false, 10, 1, "Context Switches, both Voluntary and Involuntary per second<br>Algorithm: (diff_ctxVoluntary + diff_ctxInvoluntary) / samplePeriodInSec");
			md.addDecColumn ("ctxVoluntary/s"  , 26, 26, false, 10, 1, "Processen is waiting on something (ex. disk-I/O or Network) per second<br>Algorithm: diff_ctxVoluntary / samplePeriodInSec");
			md.addDecColumn ("ctxInvoluntary/s", 27, 27, false, 10, 1, "Kernel forced the process to pause, to make room for something else (indicates CPU saturation) per second<br>Algorithm: diff_ctxInvoluntary / samplePeriodInSec");
			md.addStrColumn ("shortCommand"    , 28, 28, false, 256,   "Command line, in short format.");
			md.addStrColumn ("fullCommand"     , 29, 29, false, 4096,  "command with all its arguments as a string. Modifications to the arguments may be shown.  The output in this column may contain spaces.  A process marked <defunct> is partly dead, waiting to be fully destroyed by its parent.  Sometimes the process args will be unavailable; when this happens, ps will instead print the executable name in brackets.  (alias cmd, command).  See also the comm format keyword, the -f option, and the c option.");

			md.setPercentCol("%mem", "%cpu", "%cpuAdj", "%cpuUsr", "%cpuSys", "%ioWait");
			
			// NO DIFF COLUMNS
			//md.setDiffCol();
			//md.setMarkRateData(); // If we can add some "thing" to DISPLAY some columns in some other color, so it's easier to detect "what data" that is displayed
			//NOTE: if 'ioOk' == 0 ... The "io*" cells will be rendered with "light gray" background

			md.setPkCol("pid");
//		}

		return md;
	}
	
	@Override
	public void parseAndApply(HostMonitorMetaData md, String row, int type)
	{
		if (type != SshConnection.STDOUT_DATA)
		{
			_logger.error("Received data from STDERR, which was NOT expected: " + row);
			throw new RuntimeException("Received data from STDERR, which was NOT expected: " + row);
		}
		
		if (StringUtil.isNullOrBlank(row))
		{
			throw new RuntimeException("Received an null or empty String. Can't continue...");
		}
		else
		{
			// Check if it looks like a JSON String
			if ( ! JsonUtils.isPossibleJson(row) )
			{
				throw new RuntimeException("Received input, but it does NOT look like a JSON String. Can't continue... row=" + row);
			}
		}
		
		int top = Configuration.getCombinedConfiguration().getIntProperty(CmOsPs.PROPKEY_top, CmOsPs.DEFAULT_top);
		if (top <= 0)
			top = Integer.MAX_VALUE;
		
		double minCpuPctUsage = Configuration.getCombinedConfiguration().getDoubleProperty(CmOsPs.PROPKEY_minCpuPctUsage, CmOsPs.DEFAULT_minCpuPctUsage);
//		if (MONITOR_TYPE == 1)
//			minCpuPctUsage = -1d;
			
		
//		System.out.println("ROW.firstXChars[" + row.length() + "] = " + StringUtil.left(row, 512));
		try
		{
			List<ProcessMetrics> metrics = _monitor.processNewData(row, minCpuPctUsage);
//			_monitor.printAll(metrics);
//			_monitor.printTopList(metrics, 10);
			
			OsTable osTable = getCurrentSample();
			
			List<ProcessMetrics> topResources = _monitor.getTopList(metrics, minCpuPctUsage, top);
			for (ProcessMetrics pm : topResources)
			{
				String[] sa = null;

				// Using ABS counters and do DIFF/RATE calculation of some columns
//				if (MONITOR_TYPE == 1)
//				{
//					sa = new String[30];
//					/* pid            */ sa[ 0] = pm.pid  + "";
//					/* ppid           */ sa[ 1] = pm.ppid + "";
//					/* user           */ sa[ 2] = pm.user;
//					/* start_time     */ sa[ 3] = TimeUtils.toString(pm.rawData.startTimeMillis);
//					/* threads        */ sa[ 4] = pm.threads + "";
//					/* memoryMb       */ sa[ 5] = pm.getMemoryKbAbs() / 1024 + "";
//					/* %mem           */ sa[ 6] = pm.memoryPct  + "";
//					/* %cpu           */ sa[ 7] = pm.cpuUsagePct     + "";
//					/* %cpuAdj        */ sa[ 8] = pm.cpuAdjPct + "";
//					/* cpuTime        */ sa[ 9] = TimeUtils.msToTimeStrDHMSms(pm.getCpuTotalTimeMsAbs());
//					/* cpuTimeMs      */ sa[10] = pm.getCpuTotalTimeMsAbs() + "";
//					/* userCpuMs      */ sa[11] = pm.getCpuUserTimeMsAbs() + "";
//					/* systemCpuMs    */ sa[12] = pm.getCpuSystemTimeMsAbs() + "";
//					/* %ioWait        */ sa[13] = pm.ioWaitPct       + "";
//					/* ioWaitMs       */ sa[14] = pm.getIoWaitMsAbs() + "";;
//					/* hasIoCnt       */ sa[15] = pm.hasIoCounters() + "";
//					/* ioPerSec       */ sa[16] = pm.getIopsAbs() + "";
//					/* ioReads        */ sa[17] = pm.getIopsRAbs() + "";
//					/* ioWrites       */ sa[18] = pm.getIopsWAbs() + "";
//					/* ioKb/s         */ sa[19] = pm.getIoKbAbs() + "";
//					/* ioKbRead       */ sa[20] = pm.getIoKbRAbs() + "";
//					/* ioKbWrite      */ sa[21] = pm.getIoKbWAbs() + "";
//					/* avgIoSizeKb    */ sa[22] = pm.getAvgIoSizeKb()  + "";
//					/* avgRIoSizeKb   */ sa[23] = pm.getAvgRIoSizeKb() + "";
//					/* avgWIoSizeKb   */ sa[24] = pm.getAvgWIoSizeKb() + "";
//					/* ctxSwitches    */ sa[25] = pm.getCtxSwitchesAbs() + "";
//					/* ctxVoluntary   */ sa[26] = pm.getCtxVoluntaryAbs() + "";
//					/* ctxInvoluntary */ sa[27] = pm.getCtxInvoluntaryAbs() + "";
//					/* shortCommand   */ sa[28] = pm.getShortCommand();
//					/* fullCommand    */ sa[29] = pm.cmd;
//				}

				// Only data in the ABS table (mix Abs and Rate values)
//				if (MONITOR_TYPE == 2)
//				{
					sa = new String[29];
					/* pid              */ sa[ 0] = pm.pid  + "";
					/* ppid             */ sa[ 1] = pm.ppid + "";
					/* user             */ sa[ 2] = pm.user;
					/* start_time       */ sa[ 3] = TimeUtils.toString(pm.rawData.startTimeMillis);
					/* threads          */ sa[ 4] = pm.threads + "";
					/* memoryMb         */ sa[ 5] = pm.getMemoryKbAbs() / 1024 + "";
					/* %mem             */ sa[ 6] = pm.memoryPct + "";
					/* %cpuAdj          */ sa[ 7] = pm.cpuAdjPct + "";
					/* %cpu             */ sa[ 8] = pm.cpuUsagePct + "";
					/* %cpuUsr          */ sa[ 9] = pm.cpuUsrPct + "";
					/* %cpuSys          */ sa[10] = pm.cpuSysPct + "";
					/* cpuTime          */ sa[11] = TimeUtils.msToTimeStrDHMSms(pm.getCpuTotalTimeMsAbs());
//					/* cpuTimeMs        */ sa[? ] = pm.getCpuTotalTimeMsRate() + "";
//					/* userCpuMs        */ sa[? ] = pm.getCpuUserTimeMsRate() + "";
//					/* systemCpuMs      */ sa[? ] = pm.getCpuSystemTimeMsRate() + "";
					/* %ioWait          */ sa[12] = pm.ioWaitPct + "";
					/* totalIoWaitMs    */ sa[13] = pm.getIoWaitMsAbs() + "";;
					/* ioOk             */ sa[14] = pm.hasIoCounters() + "";
					/* ioKb/s           */ sa[15] = pm.getIoKbRate() + "";
					/* ioKbRead/s       */ sa[16] = pm.getIoKbRRate() + "";
					/* ioKbWrite/s      */ sa[17] = pm.getIoKbWRate() + "";
					/* ioPerSec         */ sa[18] = pm.getIopsRate() + "";
					/* ioReads/s        */ sa[19] = pm.getIopsRRate() + "";
					/* ioWrites/s       */ sa[20] = pm.getIopsWRate() + "";
					/* avgIoSizeKb      */ sa[21] = pm.getAvgIoSizeKb()  + "";
					/* avgRIoSizeKb     */ sa[22] = pm.getAvgRIoSizeKb() + "";
					/* avgWIoSizeKb     */ sa[23] = pm.getAvgWIoSizeKb() + "";
					/* ctxSwitches/s    */ sa[24] = pm.getCtxSwitchesRate() + "";
					/* ctxVoluntary/s   */ sa[25] = pm.getCtxVoluntaryRate() + "";
					/* ctxInvoluntary/s */ sa[26] = pm.getCtxInvoluntaryRate() + "";
					/* shortCommand     */ sa[27] = pm.getShortCommand();
					/* fullCommand      */ sa[28] = pm.cmd;
//				}
				
				
				OsTableRow ostr = new OsTableRow(osTable.getMetaData(), sa);
				osTable.addRow(ostr);
			}
		}
		catch (Exception ex)
		{
			_logger.error(getModuleName() + ": Problems creating metrics.", ex);
		}
		

//		// TODO Auto-generated method stub
//		super.parseAndApply(md, row, type);
	}

	/**
	 * On START:
	 *  - Check if python exists on the remote machine
	 *  - Check if the command EXISTS (or needs a never version) at the remote server
	 *  - If not: 
	 *      o Create (transfer) the script on the remote server, under /tmp/
	 *      o Make it executable
	 */
	@Override
	public void init(HostMonitorConnection osConn)
	throws Exception
	{
		try
		{
			// Check if python3 exist on the remote server (or fallback on python and check it's version)
			String pythonCmd = "python3";
			ExecOutput checkPython = osConn.execCommand("command -v " + pythonCmd);
			if (checkPython.getExitCode() != 0)
			{
				_logger.info("Python command '" + pythonCmd + "' was not found on the remote system. Trying to fallback to 'python'.");
				pythonCmd = "python";
				checkPython = osConn.execCommand("command -v " + pythonCmd);
				if (checkPython.getExitCode() == 0)
				{
//					// Check python version
//					checkPython = osConn.execCommand(pythonCmd + " --version");
//					
//					if (checkPython.getExitCode() == 0)
//					{
//						String version = checkPython.getStdOutStr().trim();
//						if (version.contains("3."))
//						{
//							_logger.info("Fallback on Python command '" + pythonCmd + "' using version '" + version + "'.");
//						}
//						else
//						{
//							_logger.warn("Could NOT find '3.' in the version string '" + version + "'. Lets continue and try anyway... But DO expect problems.");
//							// OR Should we throw an Exception here
//						}
//					}
//					else
//					{
//						_logger.warn("Problems executing '" + pythonCmd + " --version'. Lets continue and hope for the best.");
//						// OR Should we throw an Exception here
//					}
				}
				else
				{
					_logger.warn("Problems executing 'command -v " + pythonCmd + "'. Lets continue and hope for the best.");
					// OR Should we throw an Exception here
				}
			}

			// OR Should we check version of Python here and THROW Exception if not at Version "3.4+"

			// Check python version
			checkPython = osConn.execCommand(pythonCmd + " --version");
			
			if (checkPython.getExitCode() == 0)
			{
				String version = checkPython.getStdOutStr().trim();
				if (StringUtil.isNullOrBlank(version))
					version = checkPython.getStdErrStr().trim();
				
				if (StringUtil.hasValue(version))
				{
					int versionInt = VersionShort.parse(version);

					if (versionInt <= VersionShort.toInt(3, 4, 0))
					{
						throw new Exception("On Remote Server: Expecting Python Version 3.4 or higher. Found version was '" + version + "'.");
					}
				}
				else
				{
					_logger.warn("On Remote Server: Problems Finding Python Version. Lets continue and hope for the best.");
				}
			}
			else
			{
				throw new Exception("Problems finding Python Version. executed '" + pythonCmd + " --version'. But it exited with return code " + checkPython.getExitCode());
			}

			// Set what Python OS Command to use
			_pythonCmd = pythonCmd;


			//---------------------------------------------------------------------------------
			// Check if the Python script on the remote side exists (or needs to be renewed) 
			//---------------------------------------------------------------------------------

			// Read the script file from the classpath
			String scriptContent = FileUtils.readFile(getClass(), LOCAL_FILE);

//			String checksumLocal = DigestUtils.md5Hex(scriptContent);   
			String checksumLocal = DigestUtils.sha256Hex(scriptContent);


			// Check if the file exists (or need a newer version)
//			String checksumOut    = osConn.execCommandOutputAsStr("md5sum "    + COMMAND_TO_EXECUTE);
			String checksumOut    = osConn.execCommandOutputAsStr("sha256sum " + REMOTE_FILE);
			String checksumRemote = StringUtil.word(checksumOut, 0);

//			_logger.info(getModuleName() + ": Checking if file '" + REMOTE_FILE + "' exists or needs to be re-created the remote host.");
			if (checksumLocal.equals(checksumRemote))
			{
				_logger.info(getModuleName() + ": No need to re-create '" + REMOTE_FILE + "'. (remote checksum '" + checksumRemote + "', and local checksum '" + checksumLocal + "' is equal)");
			}
			else
			{
				_logger.info(getModuleName() + ": Create or ReCreating remote file '" + REMOTE_FILE + "' remote checksum '" + checksumRemote + "', and local checksum '" + checksumLocal + "' differs. Output from remote command to get checksum '" + checksumOut + "'.");

				String createCommand = ""
						+ "cat <<EOF> " + REMOTE_FILE + "; chmod 755 " + REMOTE_FILE + "; ls -l " + REMOTE_FILE + "; \n"
						+ scriptContent // NOTE: No extra newline here
						+ "EOF\n"
						;

				if (_logger.isDebugEnabled())
					_logger.debug(getModuleName() + ": Command that will be used to create the remote file: \n" + createCommand);

//				String createOutput = osConn.execCommandOutputAsStr(createCommand);
//				_logger.info(getModuleName() + ": Output from create command: " + createOutput);

				ExecOutput createOutput = osConn.execCommand(createCommand);
				_logger.info(getModuleName() + ": Output from create command: " + createOutput.toString());
			}
		}
		catch (Exception ex)
		{
			_logger.error(getModuleName() + ": Problems executing commands to check if file '" + REMOTE_FILE + "' exists.", ex);
			// OR Should we throw an Exception here
			throw ex;
		}
	}

	private ProcessMonitorService _monitor = new ProcessMonitorService();

//	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ProcessSnapshot 
	{
		public int     pid;
		public int     ppid;
		public long    startTimeMillis;
		public String  user;
		public String  cmd;
		public int     threads;
		public int     cores;
		public long    utimeMs;
		public long    stimeMs;
		public long    ioWaitMs;
		public long    rssKb;
		public double  memPct; // Percentage of total physical memory
		public int     ioOk;
		public long    ioReadBytes;
		public long    ioWriteBytes;
		public long    ioSyscr; // Total read syscalls
		public long    ioSyscw; // Total write syscalls
		public long    ctxVoluntary;
		public long    ctxInvoluntary;
		public long    ts;

		// Helper method to create unique key value
		public String getUniqueKey() 
		{
			return pid + "_" + startTimeMillis;
		}
		// To get a readable date in Java
		public Instant getStartInstant() 
		{
			return Instant.ofEpochMilli(startTimeMillis);
		}
		@Override
		public String toString()
		{
			return "ProcessSnapshot ["
					+ "pid="               + pid 
					+ ", ppid="            + ppid
					+ ", startTimeMillis=" + startTimeMillis 
					+ ", user="            + user 
					+ ", cmd="             + cmd 
					+ ", cores="           + cores 
					+ ", threads="         + threads 
					+ ", utimeMs="         + utimeMs 
					+ ", stimeMs="         + stimeMs 
					+ ", ioWaitMs="        + ioWaitMs 
					+ ", rssKb="           + rssKb 
					+ ", memPct="          + memPct 
					+ ", ioOk="            + ioOk 
					+ ", ioReadBytes="     + ioReadBytes 
					+ ", ioWriteBytes="    + ioWriteBytes 
					+ ", ioSyscr="         + ioSyscr 
					+ ", ioSyscw="         + ioSyscw 
					+ ", ctxVoluntary="    + ctxVoluntary 
					+ ", ctxInvoluntary="  + ctxInvoluntary 
					+ ", ts="              + ts 
					+ "]"
					;
		}	
	}

	public static class ProcessMetrics 
	{
		public int    pid;
		public int    ppid;
		public String cmd;
		public String user;
		public long   startTimeMillis;
		public double cpuUsagePct;
		public double cpuAdjPct;
		public double cpuUsrPct;
		public double cpuSysPct;
		public int    threads;
		public double ioWaitPct;
//		public long   ioWaitMs;
//		public long   memoryKb;
		public double memoryPct;
//		public long   ioReadBytesPerSec;
//		public long   ioWriteBytesPerSec;
//		public double ioReadOpsPerSec;
//		public double ioWriteOpsPerSec;
//		public double ioTotalOpsPerSec;
//		public long   totalContextSwitchesPerSec;

//		public long   diff_ctxSwitches   ;
		public long   diff_ctxVoluntary  ;
		public long   diff_ctxInvoluntary;

		public long   samplePeriodInMs;
		public double samplePeriodInSec;

//	x	public int    pid;
//	x	public int    ppid;
//	x	public long   startTimeMillis;
//	x	public String user;
//	x	public String cmd;
		public long   diff_utimeMs;
		public long   diff_stimeMs;
		public long   diff_ioWaitMs;
		public long   diff_rssKb;
//		public double memPct; // Percentage of total physical memory
		public long   diff_ioReadBytes;
		public long   diff_ioWriteBytes;
		public long   diff_ioSyscr; // Total read syscalls
		public long   diff_ioSyscw; // Total write syscalls
//		public long   ctxVoluntary;
//		public long   ctxInvoluntary;
//		public long   ts;

		public int    hasIoCounters()         { return rawData.ioOk; }
		
		public long   getMemoryKbAbs()        { return rawData.rssKb; }
		public long   getMemoryKbDiff()       { return diff_rssKb; }
		public double getMemoryKbRate()       { return diff_rssKb * 1.0 / samplePeriodInSec; }
		
		public long   getIoWaitMsAbs()        { return rawData.ioWaitMs; }
		public long   getIoWaitMsDiff()       { return diff_ioWaitMs; }
		public double getIoWaitMsRate()       { return diff_ioWaitMs * 1.0 / samplePeriodInSec; }
		
		// Context Switches: ABS,DIFF/RATE
		public long   getCtxSwitchesAbs()     { return rawData.ctxVoluntary + rawData.ctxInvoluntary; }
		public long   getCtxVoluntaryAbs()    { return rawData.ctxVoluntary; }
		public long   getCtxInvoluntaryAbs()  { return rawData.ctxInvoluntary; }
		
		public long   getCtxSwitchesDiff()    { return diff_ctxVoluntary + diff_ctxInvoluntary; }
		public long   getCtxVoluntaryDiff()   { return diff_ctxVoluntary; }
		public long   getCtxInvoluntaryDiff() { return diff_ctxInvoluntary; }
		
		public double getCtxSwitchesRate()    { return (diff_ctxVoluntary + diff_ctxInvoluntary) * 1.0 / samplePeriodInSec; }
		public double getCtxVoluntaryRate()   { return (diff_ctxVoluntary)                       * 1.0 / samplePeriodInSec; }
		public double getCtxInvoluntaryRate() { return (diff_ctxInvoluntary)                     * 1.0 / samplePeriodInSec; }

		
		// CPU: ABS,DIFF/RATE
		public long   getCpuTotalTimeMsAbs()   { return rawData.utimeMs + rawData.stimeMs; }
		public long   getCpuUserTimeMsAbs()    { return rawData.utimeMs; }
		public long   getCpuSystemTimeMsAbs()  { return rawData.stimeMs; }
		
		public long   getCpuTotalTimeMsDiff()  { return diff_utimeMs + diff_stimeMs; }
		public long   getCpuUserTimeMsDiff()   { return diff_utimeMs; }
		public long   getCpuSystemTimeMsDiff() { return diff_stimeMs; }
		
		public double getCpuTotalTimeMsRate()  { return (diff_utimeMs + diff_stimeMs) * 1.0 / samplePeriodInSec; }
		public double getCpuUserTimeMsRate()   { return (diff_utimeMs)                * 1.0 / samplePeriodInSec; }
		public double getCpuSystemTimeMsRate() { return (diff_stimeMs)                * 1.0 / samplePeriodInSec; }

		public ProcessSnapshot rawData;

//		@Override
//		public String toString() 
//		{
//			return String.format("PID: %d | PPID: %d | User: %s | CPU: %.2f%% | RAM: %d KB | Read: %d B/s | Write: %d B/s | Cmd: %s",
//					pid, ppid, user, cpuUsagePct, memoryKb, ioReadBytesPerSec, ioWriteBytesPerSec, 
//					cmd.length() > 50 ? cmd.substring(0, 47) + "..." : cmd);
//		}

		// IOPS: ABS,DIFF/RATE
		public long   getIopsAbs()   { return rawData.ioSyscr + rawData.ioSyscw; }
		public long   getIopsRAbs()  { return rawData.ioSyscr; }
		public long   getIopsWAbs()  { return rawData.ioSyscw; }

		public long   getIopsDiff()  { return diff_ioSyscr + diff_ioSyscw; }
		public long   getIopsRDiff() { return diff_ioSyscr; }
		public long   getIopsEDiff() { return diff_ioSyscw; }
                                     
		public Double getIopsRate()  { return (diff_ioSyscr + diff_ioSyscw) * 1.0 / samplePeriodInSec; } 
		public Double getIopsRRate() { return (diff_ioSyscr)                * 1.0 / samplePeriodInSec; }
		public Double getIopsWRate() { return (diff_ioSyscw)                * 1.0 / samplePeriodInSec; }
		

		// KB: ABS,DIFF/RATE
		public long   getIoKbAbs()   { return (rawData.ioReadBytes + rawData.ioWriteBytes) / 1024; }
		public long   getIoKbRAbs()  { return (rawData.ioReadBytes                       ) / 1024; }
		public long   getIoKbWAbs()  { return (rawData.ioWriteBytes                      ) / 1024; }

		public long   getIoKbDiff()  { return (diff_ioReadBytes + diff_ioWriteBytes) / 1024; }
		public long   getIoKbRDiff() { return (diff_ioReadBytes                    ) / 1024; }
		public long   getIoKbEDiff() { return (diff_ioWriteBytes                   ) / 1024; }

		public Double getIoKbRate()  { return ((diff_ioReadBytes + diff_ioWriteBytes) * 1.0 / samplePeriodInSec) / 1024.0; } 
		public Double getIoKbRRate() { return ((diff_ioReadBytes) * 1.0 / samplePeriodInSec                    ) / 1024.0; }
		public Double getIoKbWRate() { return ((diff_ioWriteBytes) * 1.0 / samplePeriodInSec                   ) / 1024.0; }
		
		// IO Size KB per IO
		public Double getAvgIoSizeKb()
		{
			// NOTE: This returns 0 if diff_ioSysc{r|w} is 0
			double avgReadBytes  = (diff_ioSyscr <= 0) ? 0.0 : (diff_ioReadBytes *1.0) / (diff_ioSyscr*1.0);
			double avgWriteBytes = (diff_ioSyscw <= 0) ? 0.0 : (diff_ioWriteBytes*1.0) / (diff_ioSyscw*1.0);
			return (avgReadBytes + avgWriteBytes) / 1024.0;

			// NOTE: This returns bytes/1 if diff_ioSysc{r|w} is 0
			//       I don't know what's the most correct (why is 'diff_ioSysc{r|w}' 0 and the 'diff_io{Read|Write}Bytes' have data... Async IO issues by 'io_submit' is not counted in 'ioSysc{r|w}'
			//double avgReadBytes  = (diff_ioSyscr <= 0) ? (diff_ioReadBytes *1.0) / 1.0 : (diff_ioReadBytes *1.0) / (diff_ioSyscr*1.0);
			//double avgWriteBytes = (diff_ioSyscw <= 0) ? (diff_ioWriteBytes*1.0) / 1.0 : (diff_ioWriteBytes*1.0) / (diff_ioSyscw*1.0);
			//return (avgReadBytes + avgWriteBytes) / 1024.0;
		}

		public Double getAvgRIoSizeKb()
		{
			double avgReadBytes  = (diff_ioSyscr <= 0) ? 0.0 : (diff_ioReadBytes *1.0) / (diff_ioSyscr*1.0);
			return avgReadBytes / 1024.0;
		}

		public Double getAvgWIoSizeKb()
		{
			double avgWriteBytes = (diff_ioSyscw <= 0) ? 0.0 : (diff_ioWriteBytes*1.0) / (diff_ioSyscw*1.0);
			return avgWriteBytes / 1024.0;
		}


		private static final Pattern SYBASE_S_FLAG = Pattern.compile("-[sS]([^\\s]+)");
		private static final Pattern DBX_COLLECTOR_PATTERN = Pattern.compile("-Dnogui\\.([^.]+)\\.srv='([^']+)'"); // Captures 'DbxTuneType' as group 1 and 'srvName' as group 2
		private static final Pattern DBX_SRV_ALIAS_PATTERN = Pattern.compile("--serverAlias\\s+([^\\s]+)"); // Captures the alias value

		public String getShortCommand()
		{
			String fullCmd = this.cmd;
			
			if (fullCmd == null || fullCmd.isEmpty()) return "unknown";
			if (fullCmd.startsWith("["))              return fullCmd; // Kernel thread

			String[] parts = fullCmd.split("\\s+");
			String binaryPath = parts[0];
			String binaryName = new File(binaryPath).getName();

			// JAVA PROCESSES
			if (binaryName.equalsIgnoreCase("java")) 
			{
				// Special case for DbxCentral
				if (fullCmd.contains(" -DDbxCentral=true")) 
				{
					return "java: DbxCentral";
				}

				// Special case for DbxCentral
				if (fullCmd.contains(" -Dnogui.")) 
				{
					// CmdLine Example: java -Dnogui.SqlServerTune.srv='gs-1-win:1433' ... com.dbxtune.SqlServerTune -n /home/sybase/.dbxtune/dbxc/conf/sqlserver.GENERIC.conf -Usa -Sgs-1-win:1433 -ugorans -L /home/sybase/.dbxtune/dbxc/log --savedir /home/sybase/.dbxtune/dbxc/data --serverAlias GS-1-WIN__SS_2016
					// Try to find Collector Type and Server Name
					Matcher collectorMatcher = DBX_COLLECTOR_PATTERN.matcher(fullCmd);
					if (collectorMatcher.find()) 
					{
						String type   = collectorMatcher.group(1);   // e.g., SqlServerTune
						String server = collectorMatcher.group(2); // e.g., gs-1-win:1433

						// Check if an Alias should override the server name
						Matcher aliasMatcher = DBX_SRV_ALIAS_PATTERN.matcher(fullCmd);
						if (aliasMatcher.find()) 
						{
							server = aliasMatcher.group(1); // e.g., GS-1-WIN__SS_2016
						}

						return "java: " + type + " (" + server + ")";
					}
				}

				// Fallback for other Java processes (Jar or Class) 
				for (int i = 1; i < parts.length; i++) 
				{
					// Look for -cp or -jar
					if (parts[i-1].equals("-cp") || parts[i-1].equals("-classpath") || parts[i-1].equals("-jar")) 
					{
						return "java: " + new File(parts[i]).getName();
					}
					// Look for a class name (contains '.' and doesn't start with '-')
					if (!parts[i].startsWith("-") && parts[i].contains(".")) 
					{
						String[] classParts = parts[i].split("\\.");
						return "java: " + classParts[classParts.length - 1];
					}
				}
				return "java (generic)";
//				return fullCmd;
			}

			// SYBASE / SAP ASE
			if (StringUtil.equalsAny(binaryName, "dataserver", "backupserver", "repserver"))
			{
				Matcher m = SYBASE_S_FLAG.matcher(fullCmd);
				if (m.find())
				{
					String srvName = m.group(1);
					if (binaryName.equals("dataserver"  )) return "sybase-ase: " + srvName;
					if (binaryName.equals("backupserver")) return "sybase-bs:  " + srvName;
					if (binaryName.equals("repserver   ")) return "sybase-rs:  " + srvName;
				}
				return "sybase (" + binaryName + ")";
			}
			
			// Postgres
			if (binaryName.startsWith("postgres")) 
			{
				// Look for the activity description which usually starts after "postgres: "
				// Note: Sometimes there is no space after the colon in raw cmdline
				int colonIdx = fullCmd.indexOf(":");

				if (colonIdx != -1 && colonIdx < fullCmd.length() - 1) 
				{
					// Extract everything after the colon
					String activity = fullCmd.substring(colonIdx + 1).trim();

					if (activity.isEmpty()) 
					{
						return "pg: master";
					}

					// Simplify common background processes
					if (activity.contains("checkpointer"                )) return "pg: checkpointer";
					if (activity.contains("walwriter"                   )) return "pg: walwriter";
					if (activity.contains("background writer"           )) return "pg: bgwriter";
					if (activity.contains("writer"                      )) return "pg: writer";
					if (activity.contains("parallel worker"             )) return "pg: parallel-worker";
					if (activity.contains("autovacuum launcher"         )) return "pg: autovacuum-launcher";
					if (activity.contains("autovacuum worker"           )) return "pg: autovacuum-worker";
					if (activity.contains("autovacuum"                  )) return "pg: autovacuum";
					if (activity.contains("stats collector"             )) return "pg: stats-coll";
					if (activity.contains("logical replication launcher")) return "pg: repl-launcher";
					if (activity.contains("logical replication worker"  )) return "pg: repl-worker";
					if (activity.contains("logical replication"         )) return "pg: repl";

					// For user sessions, they often look like: "user db host activity"
					// We can shorten this to just show the db and activity
					String[] pgParts = activity.split("\\s+");
					if (pgParts.length >= 2) 
					{
						// Returns "pg: [dbName] ([lastWord])" e.g., "pg: customer_db (idle)"
						return "pg: " + pgParts[1] + " (" + pgParts[pgParts.length - 1] + ")";
					}

					return "pg: " + activity;
				}
				return "pg: master";
			}


			// SCRIPTS (Python, Bash, Perl)
			if (binaryName.equals("python") || binaryName.equals("python3") || binaryName.equals("bash") || binaryName.equals("sh")) 
			{
				if (parts.length > 1) 
				{
					return binaryName + ": " + new File(parts[1]).getName();
				}
			}

			// DEFAULT: Just the binary name
			return binaryName;
		}
	}

	public static class ProcessMonitorService 
	{
		// JSON to POJO object
		private final ObjectMapper _mapper = new ObjectMapper();

		// Saved snapshots
		private Map<String, ProcessSnapshot> _lastSnapshots = new HashMap<>();

		/**
		 * 
		 * @param jsonRaw
		 * @return
		 * @throws Exception
		 */
		public List<ProcessMetrics> processNewData(String jsonRaw, double skipIfBelowCpuPct) 
		throws Exception 
		{
			// JSON String to List of POJO
			List<ProcessSnapshot> newSnapshots = _mapper.readValue(jsonRaw, new TypeReference<List<ProcessSnapshot>>() {});

			// Result List of metrics
			List<ProcessMetrics> metricsList = new ArrayList<>();

			// Loop all newSnap Objects, and do diff calculation of previous sample
			for (ProcessSnapshot newSnap : newSnapshots) 
			{
				// Key would be PID,PidStartTime
				String key = newSnap.getUniqueKey();

				if (_lastSnapshots.containsKey(key)) 
				{
					ProcessSnapshot prev = _lastSnapshots.get(key);
					long timeDiffMs = newSnap.ts - prev.ts;
					double timeDiffSec = timeDiffMs / 1000.0;

					if (timeDiffMs > 0) 
					{
						// CPU Percent (utime + stime)
						long totalCpuMs = (newSnap.utimeMs + newSnap.stimeMs) - (prev.utimeMs + prev.stimeMs);
						double cpuUsagePct = (totalCpuMs * 100.0) / timeDiffMs;
						
						if (cpuUsagePct > skipIfBelowCpuPct)
						{
							ProcessMetrics m = new ProcessMetrics();
							m.samplePeriodInMs  = timeDiffMs;
							m.samplePeriodInSec = timeDiffSec;
							m.pid         = newSnap.pid;
							m.ppid        = newSnap.ppid;
							m.user        = newSnap.user;
							m.cmd         = newSnap.cmd;
//							m.memoryKb    = newSnap.rssKb;
							m.memoryPct   = newSnap.memPct;
							m.cpuUsagePct = cpuUsagePct;
							m.cpuAdjPct   = cpuUsagePct / newSnap.cores;
							m.cpuUsrPct   = (newSnap.utimeMs - prev.utimeMs) * 100.0 / timeDiffMs;;
							m.cpuSysPct   = (newSnap.stimeMs - prev.stimeMs) * 100.0 / timeDiffMs;;
							m.threads     = newSnap.threads;
							
							// IO Wait & PCT
							m.diff_ioWaitMs       = newSnap.ioWaitMs - prev.ioWaitMs;
							m.ioWaitPct           = (m.diff_ioWaitMs * 100.0) / timeDiffMs;

							// IOPS
							m.diff_ioSyscr        = newSnap.ioSyscr        - prev.ioSyscr;
							m.diff_ioSyscw        = newSnap.ioSyscw        - prev.ioSyscw;

							// IO Bytes
							m.diff_ioReadBytes    = newSnap.ioReadBytes    - prev.ioReadBytes;
							m.diff_ioWriteBytes   = newSnap.ioWriteBytes   - prev.ioWriteBytes;

							// Memory
							m.diff_rssKb          = newSnap.rssKb          - prev.rssKb;

							// CPU Times
							m.diff_stimeMs        = newSnap.stimeMs        - prev.stimeMs;
							m.diff_utimeMs        = newSnap.utimeMs        - prev.utimeMs;

							// Context Switches
							m.diff_ctxVoluntary   = newSnap.ctxVoluntary   - prev.ctxVoluntary;
							m.diff_ctxInvoluntary = newSnap.ctxInvoluntary - prev.ctxInvoluntary;

							// I/O (Bytes per second)
//							m.ioReadBytesPerSec  = (newSnap.ioReadBytes  - prev.ioReadBytes ) * 1000 / timeDiffMs;
//							m.ioWriteBytesPerSec = (newSnap.ioWriteBytes - prev.ioWriteBytes) * 1000 / timeDiffMs;

							// Read, Write, Total IOPS
//							m.ioReadOpsPerSec  = (newSnap.ioSyscr - prev.ioSyscr) / timeDiffSec;
//							m.ioWriteOpsPerSec = (newSnap.ioSyscw - prev.ioSyscw) / timeDiffSec;
//							m.ioTotalOpsPerSec = m.ioReadOpsPerSec + m.ioWriteOpsPerSec;

							// Context Switches per second
//							long totalCtxt = (newSnap.ctxVoluntary + newSnap.ctxInvoluntary) - (prev.ctxVoluntary + prev.ctxInvoluntary);
//							m.totalContextSwitchesPerSec = totalCtxt * 1000 / timeDiffMs;

							m.rawData = newSnap;

							metricsList.add(m);
						}
					}
				}
				// Save this for next sample
				_lastSnapshots.put(key, newSnap);
			}

			// Remove old PID:s, which no longer exists. (to avoid memory leak)
			Set<String> currentKeys = newSnapshots.stream().map(ProcessSnapshot::getUniqueKey).collect(Collectors.toSet());
			_lastSnapshots.keySet().removeIf(k -> !currentKeys.contains(k));

			return metricsList;
		}

		public void printAll(List<ProcessMetrics> metrics)
		{
			System.out.println("COUNT: " + metrics.size());
			for (ProcessMetrics entry : metrics)
			{
				System.out.println(entry.toString());
			}
			
		}

		public void printTopList(List<ProcessMetrics> metrics, int topN) 
		{
			System.out.println("\n--- TOP " + topN + " RESOURCE CONSUMERS ---");
			metrics.stream()
				.sorted(Comparator.comparingDouble((ProcessMetrics m) -> m.cpuUsagePct).reversed())
				.limit(topN)
				.forEach(System.out::println);
		}

		public List<ProcessMetrics> getTopList(List<ProcessMetrics> metrics, double abovePct, int topN) 
		{
			return metrics.stream()
				.sorted(Comparator.comparingDouble((ProcessMetrics m) -> m.cpuUsagePct).reversed())
				.filter(m -> m.cpuUsagePct > abovePct)
				.limit(topN)
				.collect(Collectors.toList())
				;
		}
	}
}

/*
A alternate way to *better* get CPU Usage
  - Get raw values on the Linux side... from: /proc/...
  - Parse the values (received over the SSH STDOUT stream) into Java Objects
  - Calculate various values (like CPU Usage)... Data still needs to be stored in memory for "previous sample"
  - etc...
  
Right now this might be "overkill"... But I have the following, which we might implement in the future

1: A Python Script that scans /proc/... and get desired fields
   Presented as a *single* row per PID

2: Java code that parses etc

See ChatGPT History 'Top vs Ps for Stats', where we talk about:
 * why PS (on it's own) isn't good enough, because we can't remember "stuff" from previous execution
 * Various implementations (how other tools like top, htop, atop... does this)
 * Should we extract all /proc/... values to the client
 * Should we execute some (stateless script) on the server side, that just returns desired counters
 * OR: Should we have some (stetefull script) on the server side, that "does it all"... (but this leads to more problems than solutions)

The end game was:
 * Keep it stateless (remember previous sample at Java Client Side)
 * Have a Python Script that extracts the desired data

One downside of this is:
 * We need to "transfer" the script onto the "remote host" when connecting (place it in /tmp/xxxx.{USER}.sh so "we" can write/replace it when the script changes)
*/