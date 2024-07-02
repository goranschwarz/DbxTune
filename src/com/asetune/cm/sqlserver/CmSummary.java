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
package com.asetune.cm.sqlserver;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import org.apache.log4j.Logger;

import com.asetune.CounterController;
import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.Version;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.AlarmEventBlockingLockAlarm;
import com.asetune.alarm.events.AlarmEventDeadlock;
import com.asetune.alarm.events.AlarmEventLongRunningTransaction;
import com.asetune.alarm.events.sqlserver.AlarmEventLowOnWorkerThreads;
import com.asetune.alarm.events.sqlserver.AlarmEventOutOfWorkerThreads;
import com.asetune.alarm.events.sqlserver.AlarmEventSuspectPages;
import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSettingsHelper.RegExpInputValidator;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.sqlserver.gui.CmSummaryPanel;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.sql.conn.info.DbmsVersionInfoSqlServer;
import com.asetune.utils.Configuration;
import com.asetune.utils.MovingAverageCounterManager;
import com.asetune.utils.NumberUtils;
import com.asetune.utils.SqlServerUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSummary
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmSummary.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSummary.class.getSimpleName();
	public static final String   SHORT_NAME       = "Summary";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Overview of how the system performs." +
		"</html>";

	public static final String   GROUP_NAME       = null;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"LockWaits", "Connections", 
		"cpu_busy", "cpu_io", "cpu_idle", "io_total_read", "io_total_write", 
		"aaConnections", "distinctLogins", 
		"pack_received", "pack_sent", "packet_errors", "total_errors",
		"ms_ticks", "process_kernel_time_ms", "process_user_time_ms",
		"tempdbUsageMbAll", "tempdbUsageMbUser", "tempdbUsageMbInternal",
		"databaseCacheMemoryMb", "grantedWorkspaceMemoryMb", "stolenServerMemoryMb", "deadlockCount",
		"usedWorkers", "availableWorkers", "workersWaitingForCPU", "requestsWaitingForWorkers", "allocatedWorkers"
	};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false; // We want *some* counters to be negative (so we can see decrements)
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.SMALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmSummary(counterController, guiController);
	}

	public CmSummary(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setCounterController(counterController);
		setGuiController(guiController);
		
		// THIS IS THE SUMMARY CM, so set this
		counterController.setSummaryCm(this);
		
		addTrendGraphs();

		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	boolean getRootBlockerSpids = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sample_rootBlockerSpids, DEFAULT_sample_rootBlockerSpids);

	// Below is used when setting "CLIENT PROPERTY" get/setClientProperty(PROPKEY_deadlockCountOverRecordingPeriod, deadlockCount)
	public static final String  PROPKEY_clientProp_deadlockCountOverRecordingPeriod = "CmSummary.clientProp.deadlockCountOverRecordingPeriod";
	
	public static final String  PROPKEY_sample_rootBlockerSpids       = "CmSummary.sample.rootBlockerSpids";
	public static final boolean DEFAULT_sample_rootBlockerSpids       = true;

	public static final String  PROPKEY_oldestOpenTran_discard_tempdb = "CmSummary.oldestOpenTran.discard.tempdb";
	public static final boolean DEFAULT_oldestOpenTran_discard_tempdb = false;

	public static final String  PROPKEY_suspectPageCount_isEnabled    = "CmSummary.suspectPageCount.enabled";
	public static final boolean DEFAULT_suspectPageCount_isEnabled    = true;
	
	public static final String  PROPKEY_sample_tempdbSpidUsage        = "CmSummary.sample.tempdb.spid.usage";
	public static final boolean DEFAULT_sample_tempdbSpidUsage        = true;
	
	public static final String GRAPH_NAME_AA_CPU                    = "aaCpuGraph";         // String x=GetCounters.CM_GRAPH_NAME__SUMMARY__AA_CPU;
//	public static final String GRAPH_NAME_SYS_INFO_CPU              = "sysInfoCpuGraph";
	public static final String GRAPH_NAME_BLOCKING_LOCKS            = "BlockingLocksGraph";
	public static final String GRAPH_NAME_CONNECTION                = "ConnectionsGraph";   // String x=GetCounters.CM_GRAPH_NAME__SUMMARY__CONNECTION;
	public static final String GRAPH_NAME_CONNECTION_RATE           = "ConnRateGraph";
	public static final String GRAPH_NAME_AA_DISK_READ_WRITE        = "aaReadWriteGraph";   // String x=GetCounters.CM_GRAPH_NAME__SUMMARY__AA_DISK_READ_WRITE;
	public static final String GRAPH_NAME_AA_NW_PACKET              = "aaPacketGraph";      // String x=GetCounters.CM_GRAPH_NAME__SUMMARY__AA_NW_PACKET;
	public static final String GRAPH_NAME_OLDEST_TRAN_IN_SEC        = "OldestTranInSecGraph";
	public static final String GRAPH_NAME_MAX_SQL_EXEC_TIME_IN_SEC  = "MaxSqlExecTimeInSec";
	public static final String GRAPH_NAME_TEMPDB_SPID_USAGE         = "TempdbSpidUsage";
	public static final String GRAPH_NAME_TARGET_AND_TOTAL_MEM_MB   = "TargetAndTotalMemMb";
	public static final String GRAPH_NAME_MEMORY_UTILAZATION_PCT    = "MemUtilizationPct";
	public static final String GRAPH_NAME_OS_MEMORY_FREE_MB         = "OsMemoryFreeMb";
	public static final String GRAPH_NAME_PERFMON_MEMORY            = "PerfMonMem";
	public static final String GRAPH_NAME_WORKER_THREAD_USAGE       = "WorkersThreadUsage";
	public static final String GRAPH_NAME_WT_WAITING_FOR_CPU        = "WtWaitingForCpu";
	public static final String GRAPH_NAME_TASKS_WAITING_FOR_WORKERS = "TasksWaitForWorkers";
	public static final String GRAPH_NAME_DEADLOCK_COUNT_SUM        = "DeadlockCountSum";
	public static final String GRAPH_NAME_MEMORY_PRESSURE           = "MemoryPressure";
	
	// If we got Suspect Page Count > 0; then we will try to populate this, so we can attach it to the alarm.
	private ResultSetTableModel _lastSuspectPage_rstm = null;
	private List<Integer>       _lastRootBlockersList = null;

	
	private long _lastTargetServerMemoryMb = -1;
	private long _lastTotalServerMemoryMb  = -1;

	public long getLastTargetServerMemoryMb() { return _lastTargetServerMemoryMb; }
	public long getLastTotalServerMemoryMb()  { return _lastTotalServerMemoryMb; }
	

	
	private void addTrendGraphs()
	{
		// GRAPH
		addTrendGraph(GRAPH_NAME_AA_CPU,
			"CPU Summary, Global Variables", 	                        // Menu CheckBox text
			"CPU Summary for all Engines (using @@cpu_busy, @@cpu_io)", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "System+User CPU (@@cpu_busy + @@cpu_io)", "System CPU (@@cpu_io)", "User CPU (@@cpu_busy)" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.CPU,
			true,  // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// NOTE: The below showed the same info as above (@@cpu_busy, @@cpu_io) so it wasnt worth keeping it
		//       But lets keep the code as a #reminder" that it has already been tested
		//addTrendGraph(GRAPH_NAME_SYS_INFO_CPU,
		//	"CPU Summary from dm_os_sys_info", 	                        // Menu CheckBox text
		//	"CPU Summary for all Engines (using dm_os_sys_info: process_kernel_time_ms, process_user_time_ms)", // Label 
		//	TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT,
		//	new String[] { "System+User CPU (kernel_time+user_time)", "System CPU (kernel_time)", "User CPU (user_time)" }, 
		//	LabelType.Static,
		//	TrendGraphDataPoint.Category.CPU,
		//	true,  // is Percent Graph
		//	false, // visible at start
		//	0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
		//	-1);   // minimum height

		int LockWaitsThresholdSec = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_LockWaitsThresholdSec, DEFAULT_alarm_LockWaitsThresholdSec);

		addTrendGraph(GRAPH_NAME_BLOCKING_LOCKS,
			"Blocking Locks", 	                                     // Menu CheckBox text
			"Number of Concurrently Blocking Locks, above " + LockWaitsThresholdSec + " sec, from sysprocesses ("+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Blocking Locks" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.LOCK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_CONNECTION,
			"Connections/Users in SQL-Server", 	          // Menu CheckBox text
			"Connections/Users connected to the SQL-Server ("+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "UserConnections (abs)", "distinctLogins (abs)", "@@connections (diff)", "@@connections (rate)" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.SRV_CONFIG,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_CONNECTION_RATE,
			"Connection Rate in ASE", 	          // Menu CheckBox text
			"Connection Attemtps per Second, using @@connections ("+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "@@connections (rate)" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_AA_DISK_READ_WRITE,
			"Disk read/write, Global Variables", 	                         // Menu CheckBox text
			"Disk read/write per second, using @@total_read, @@total_write ("+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "@@total_read", "@@total_write" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_AA_NW_PACKET,
			"Network Packets received/sent, Global Variables", 	                            // Menu CheckBox text
			"Network Packets received/sent per second, using @@pack_received, @@pack_sent ("+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "@@pack_received", "@@pack_sent", "@@packet_errors" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.NETWORK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_OLDEST_TRAN_IN_SEC,
			"Oldest Open Transaction in any Databases",     // Menu CheckBox text
			"Oldest Open Transaction in any Databases, in Seconds ("+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_SECONDS, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Seconds" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.LOCK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_MAX_SQL_EXEC_TIME_IN_SEC,
			"Max Active SQL Execution Time In Seconds",     // Menu CheckBox text
			"Max Active SQL Execution Time In Second ("+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_SECONDS, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Max Active SQL Execution Time In Seconds" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_TEMPDB_SPID_USAGE,
			"Tempdb Usage by SPID's in MB",     // Menu CheckBox text
			"Tempdb Usage by SPID's in MB ("+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "All", "User Objects", "Internal Objects" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_TARGET_AND_TOTAL_MEM_MB,
			"Target and Total Server Memory in MB",     // Menu CheckBox text
			"Target and Total Server Memory in MB ("+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Target Server Memory MB", "Total Server Memory MB" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.MEMORY,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_MEMORY_UTILAZATION_PCT,
			"SQL Server Memory Utilazation in Percent", // Menu CheckBox text
			"SQL Server Memory Utilazation in Percent ("+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "process_memory_utilization_percentage" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.MEMORY,
			true,  // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_OS_MEMORY_FREE_MB,
			"OS Free/Available Memory in MB", // Menu CheckBox text
			"OS Free/Available Memory in MB ("+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MIN_OVER_SAMPLES),
			new String[] { "available_physical_memory_mb" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.MEMORY,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_PERFMON_MEMORY,
			"PerfMon: SQL Server Memory Usage", // Menu CheckBox text
			"PerfMon: SQL Server Memory Usage ("+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Buffer Pool Cache Memory MB", "Granted Workspace Memory MB", "Stolen Server Memory MB" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.MEMORY,
			false,  // is Percent Graph
			true,   // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_WORKER_THREAD_USAGE,
			"SQL Server Worker Threads Usage", // Menu CheckBox text
			"SQL Server Worker Threads Usage ("+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "maxWorkers", "usedWorkers", "availableWorkers", "workersWaitingForCPU", "requestsWaitingForWorkers", "allocatedWorkers" },
			LabelType.Static,
			TrendGraphDataPoint.Category.OTHER,
			false,  // is Percent Graph
			true,   // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_WT_WAITING_FOR_CPU,
			"SQL Server Workers That are Waiting for CPU to be Scheduled", // Menu CheckBox text
			"SQL Server Workers That are Waiting for CPU to be Scheduled ("+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "workersWaitingForCPU", "workersWaitingForCPU_perScheduler" },
			LabelType.Static,
			TrendGraphDataPoint.Category.CPU,
			false,  // is Percent Graph
			false,  // visible at start
			0,      // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);    // minimum height

		addTrendGraph(GRAPH_NAME_TASKS_WAITING_FOR_WORKERS,
			"Tasks/Requests that are Waiting for Available Worker Threads", // Menu CheckBox text
			"Tasks/Requests that are Waiting for Available Worker Threads ("+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "requestsWaitingForWorkers" },
			LabelType.Static,
			TrendGraphDataPoint.Category.OTHER,
			false,  // is Percent Graph
			false,  // visible at start
			0,      // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);    // minimum height

		addTrendGraph(GRAPH_NAME_DEADLOCK_COUNT_SUM,
			"Deadlock Count", // Menu CheckBox text
			"Deadlock Count ("+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Deadlock Count" },
			LabelType.Static,
			TrendGraphDataPoint.Category.LOCK,
			false,  // is Percent Graph
			false,  // visible at start
			0,      // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);    // minimum height

		addTrendGraph(GRAPH_NAME_MEMORY_PRESSURE,
			"Memory Pressure", // Menu CheckBox text
			"Memory Pressure ("+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "osMem_system_high_memory_signal_state", "osMem_system_low_memory_signal_state", "memProcessPhysicalMemoryLow", "memProcessVirtualMemoryLow" },
			LabelType.Static,
			TrendGraphDataPoint.Category.LOCK,
			false,  // is Percent Graph
			false,  // visible at start
			0,      // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);    // minimum height
	}

	@Override
	protected JPanel createGui()
	{
		setTabPanel(null); // Don't think this is necessary, but lets do it anyway...

		CmSummaryPanel summaryPanel = new CmSummaryPanel(this);

		// THIS IS THE SUMMARY CM, so set this
		CounterController.getInstance().setSummaryPanel( summaryPanel );

		// add listener, so that the GUI gets updated when data changes in the CM
		addTableModelListener( summaryPanel );

		return summaryPanel;
	}

	@Override
	public boolean isRefreshable()
	{
		// The SUMMARY should ALWAYS be refreshed
		return true;
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();
		return pkCols;
	}


	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSqlServer ssVersionInfo = (DbmsVersionInfoSqlServer) versionInfo;
//		long srvVersion = ssVersionInfo.getLongVersion();
		
		String dm_tran_active_transactions   = "dm_tran_active_transactions";
		String dm_tran_database_transactions = "dm_tran_database_transactions";
		String dm_tran_session_transactions  = "dm_tran_session_transactions";
		String dm_os_performance_counters    = "dm_os_performance_counters";

		
		if (ssVersionInfo.isAzureSynapseAnalytics())
		{
			dm_tran_active_transactions   = "dm_pdw_nodes_tran_active_transactions";
			dm_tran_database_transactions = "dm_pdw_nodes_tran_database_transactions";
			dm_tran_session_transactions  = "dm_pdw_nodes_tran_session_transactions";
			dm_os_performance_counters    = "dm_pdw_nodes_os_performance_counters";
		}

		// Are we allowed to SELECT on "MOST" sys.dm_xxx tables
		boolean hasViewServerState = false;
		List<String> activeRoles = conn.getActiveServerRolesOrPermissions();
		if (activeRoles != null)
		{
			hasViewServerState = activeRoles.contains("VIEW SERVER STATE");
		}
		
		// ----- SQL-Server 2014 and above
//		String user_objects_deferred_dealloc_page_count = "0";
//		if (srvVersion >= Ver.ver(2014))
//		{
//			user_objects_deferred_dealloc_page_count = "user_objects_deferred_dealloc_page_count";
//		}

		String notInTempdb = "";
		if ( Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_oldestOpenTran_discard_tempdb, DEFAULT_oldestOpenTran_discard_tempdb))
		{
			notInTempdb = "and database_id != 2 ";
		}

//		// Check if we have access to table 'sys.dm_tcp_listener_states', if so, construct a SQL Statement
//		// NOTE: Azure SQL Database (and possibly Azure Analytics) wont have 'sys.dm_tcp_listener_states'
//		String listenerInfo = "select @listeners = '-unknown-' \n";
//		if (conn != null) // conn can be NULL from the CM->Properties tabs
//		{
//			if (DbUtils.checkIfTableIsSelectable(conn, "sys.dm_tcp_listener_states"))
//			{
//				listenerInfo = "" +
//					"/* Get info about Listeners (as a Comma Separated List): TYPE=ipv#[ip;port] */\n" +
//					"select @listeners = coalesce(@listeners + ', ', '') \n" +
//					"    + type_desc \n" +
//					"    + '=' \n" +
//					"    + CASE WHEN is_ipv4 = 1 THEN 'ipv4[' ELSE 'ipv6[' END \n" +
//					"    + ip_address \n" +
//					"    + ';' \n" +
//					"    + convert(varchar(10), port) \n" +
//					"    + ']' \n" +
//					"from sys.dm_tcp_listener_states \n" +
//					"where state_desc = 'ONLINE' \n" +
//				//	"--  and type_desc = 'TSQL' \n" +
//					"order by listener_id \n" +
//					"";
//			}
//		}

		 // where ... --- https://learn.microsoft.com/en-us/sql/relational-databases/backup-restore/manage-the-suspect-pages-table-sql-server?view=sql-server-ver16
		String suspectPageCount  = "    , suspectPageCount                    = -1 \n";
		String suspectPageErrors = "    , suspectPageErrors                   = -1 \n";
		if (Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_suspectPageCount_isEnabled, DEFAULT_suspectPageCount_isEnabled))
		{
			suspectPageCount  = "    , suspectPageCount                    = (select count(*)         from msdb.dbo.suspect_pages) \n";
			suspectPageErrors = "    , suspectPageErrors                   = (select sum(error_count) from msdb.dbo.suspect_pages) \n";
		}

		// SQL For listeners 
		String listenerInfo = "" +
				"/* Get info about Listeners (as a Comma Separated List): TYPE=ipv#[ip;port] */\n" +
				"select @listeners = coalesce(@listeners + ', ', '') \n" +
				"    + type_desc \n" +
				"    + '=' \n" +
				"    + CASE WHEN is_ipv4 = 1 THEN 'ipv4[' ELSE 'ipv6[' END \n" +
				"    + ip_address \n" +
				"    + ';' \n" +
				"    + convert(varchar(10), port) \n" +
				"    + ']' \n" +
				"from sys.dm_tcp_listener_states \n" +
				"where state_desc = 'ONLINE' \n" +
				//	"--  and type_desc = 'TSQL' \n" +
				"order by listener_id \n" +
				"";
		
		// Listeners are NOT available in Azure 
		if ( !hasViewServerState || ssVersionInfo.isAzureDb() || ssVersionInfo.isAzureSynapseAnalytics() || ssVersionInfo.isAzureManagedInstance())
			listenerInfo = "select @listeners = '-unknown-' \n";
		
		
		int LockWaitsThresholdSec = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_LockWaitsThresholdSec, DEFAULT_alarm_LockWaitsThresholdSec);
		int oldestOpenTranInSec   = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_oldestOpenTranInSec  , DEFAULT_alarm_oldestOpenTranInSec);

		String sql = "" +
				"declare @listeners                           varchar(768) \n" +
				"\n" +
				"declare @LockWaits                           int \n" +
				"declare @LockWaitThreshold                   int \n" +
				"declare @Connections                         int \n" +
				"declare @distinctLogins                      int \n" +
				"declare @maxSqlExecTimeInSec                 int \n" +
				"declare @fullTranslogCount                   int \n" +
				"\n" +
				"declare @oldestOpenTranBeginTime             datetime \n" +
				"declare @oldestOpenTranId                    bigint \n" +
				"declare @oldestOpenTranSpid                  int \n" +
				"declare @oldestOpenTranDbname                nvarchar(128) \n" +
				"declare @oldestOpenTranWaitType              nvarchar(60)  \n" +
				"declare @oldestOpenTranCmd                   nvarchar(32)  \n" +
				"declare @oldestOpenTranLoginName             nvarchar(128) \n" +
				"declare @oldestOpenTranName                  nvarchar(32)  \n" +
//				"declare @oldestOpenTranTempdbUsageMb         decimal(12,1) \n" +
				"declare @oldestOpenTranInSec                 int = 0\n" +
				"declare @oldestOpenTranInSecThreshold        int = " + oldestOpenTranInSec + " \n" +
				"\n" +
				"declare @start_date                          datetime      = NULL \n" +
				"declare @days_running                        int           = -1 \n" +
				"declare @Target_Server_Memory_MB             bigint        = -1 \n" +
				"declare @Total_Server_Memory_MB              bigint        = -1 \n" +
			    "declare @TargetVsTotal_diff_MB               bigint        = -1 \n" +
			    "declare @scheduler_count                     int           = -1 \n" +
			    "declare @ms_ticks                            bigint        = -1 \n" +
			    "declare @process_kernel_time_ms              bigint        = -1 \n" +
			    "declare @process_user_time_ms                bigint        = -1 \n" +
			    " \n" +                                                     
			    "declare @total_os_memory_mb                  bigint        = -1 \n" +
			    "declare @available_os_memory_mb              bigint        = -1 \n" +
			    "declare @system_high_memory_signal_state     bit           = -1 \n" +
			    "declare @system_low_memory_signal_state      bit           = -1 \n" +
			    " \n" +                                                     
			    "declare @memory_used_by_sqlserver_MB         bigint        = -1 \n" +
			    "declare @locked_pages_used_by_sqlserver_MB   bigint        = -1 \n" +
			    "declare @process_memory_utilization_pct      int           = -1 \n" +
			    "declare @process_physical_memory_low         bit           = -1 \n" +
			    "declare @process_virtual_memory_low          bit           = -1 \n" +
			    " \n" +
			    "declare @databaseCacheMemoryMb               numeric(12,1) = -1 \n" +
			    "declare @grantedWorkspaceMemoryMb            numeric(12,1) = -1 \n" +
			    "declare @stolenServerMemoryMb                numeric(12,1) = -1 \n" +
			    "declare @deadlockCount                       int           = -1 \n" +
			    " \n" +
	    	    "declare @max_workers_count                   int = " + ( ! hasViewServerState ? "-1 /* no='VIEW SERVER STATE' */" : "(SELECT max_workers_count FROM sys.dm_os_sys_info)") + " \n" +
	    	    "declare @wt_usedThreads                      int \n" +
	    	    "declare @wt_availableThreads                 int \n" +
	    	    "declare @wt_workersWaitingForCPU             int \n" +
	    	    "declare @wt_requestsWaitingForThreads        bigint \n" +
	    	    "declare @wt_allocatedWorkers                 int \n" +
			    "\n" +
				listenerInfo +
				"\n" + 
			    "/*------- dm_os_sys_info -------*/ \n" +
				( ! hasViewServerState ? "/* no='VIEW SERVER STATE' */" : 
			    "select \n" +
			    "       @start_date                        = sqlserver_start_time \n" +
			    "      ,@days_running                      = datediff(day, sqlserver_start_time, getdate()) \n" +
			    "      ,@Target_Server_Memory_MB           = committed_target_kb / 1024 \n" +
			    "      ,@Total_Server_Memory_MB            = committed_kb / 1024 \n" +
			    "      ,@TargetVsTotal_diff_MB             = (committed_target_kb - committed_kb) / 1024 \n" +
			    "      ,@scheduler_count                   = scheduler_count \n" +
			    "      ,@ms_ticks                          = ms_ticks \n" +
			    "      ,@process_kernel_time_ms            = process_kernel_time_ms \n" +
			    "      ,@process_user_time_ms              = process_user_time_ms \n" +
			    "from sys.dm_os_sys_info \n" 
			    ) +
			    " \n" +
			    "/*------- dm_os_sys_memory -------*/ \n" +
				( ! hasViewServerState ? "/* no='VIEW SERVER STATE' */" : 
			    "select \n" +
			    "       @total_os_memory_mb                = total_physical_memory_kb     / 1024 \n" +
			    "      ,@available_os_memory_mb            = available_physical_memory_kb / 1024 \n" +
			    "      ,@system_high_memory_signal_state   = system_high_memory_signal_state \n" +
			    "      ,@system_low_memory_signal_state    = system_low_memory_signal_state \n" +
			    "from sys.dm_os_sys_memory \n"
				) +
			    " \n" +
			    "/*------- dm_os_process_memory -------*/ \n" +
				( ! hasViewServerState ? "/* no='VIEW SERVER STATE' */" : 
			    "select \n" +
			    "       @memory_used_by_sqlserver_MB       = physical_memory_in_use_kb  / 1024 \n" +
			    "      ,@locked_pages_used_by_sqlserver_MB = locked_page_allocations_kb / 1024 \n" +
			    "      ,@process_memory_utilization_pct    = memory_utilization_percentage \n" +
			    "      ,@process_physical_memory_low       = process_physical_memory_low \n" +
			    "      ,@process_virtual_memory_low        = process_virtual_memory_low \n" +
			    "from sys.dm_os_process_memory \n"
				) +
			    " \n" +
			    "/*------- dm_os_performance_counters -------*/ \n" +
				( ! hasViewServerState ? "/* no='VIEW SERVER STATE' */" : 
			    "select \n" + // Note: We could have done this with 1 select for every counter, but this only requires 1 scan on dm_os_performance_counters
			    "       @databaseCacheMemoryMb    = CASE WHEN counter_name = 'Database Cache Memory (KB)'    THEN cast(cntr_value/1024.0 as numeric(12,1)) ELSE @databaseCacheMemoryMb    END \n" +
			    "      ,@grantedWorkspaceMemoryMb = CASE WHEN counter_name = 'Granted Workspace Memory (KB)' THEN cast(cntr_value/1024.0 as numeric(12,1)) ELSE @grantedWorkspaceMemoryMb END \n" +
			    "      ,@stolenServerMemoryMb     = CASE WHEN counter_name = 'Stolen Server Memory (KB)'     THEN cast(cntr_value/1024.0 as numeric(12,1)) ELSE @stolenServerMemoryMb     END \n" +
			    "      ,@deadlockCount            = CASE WHEN counter_name = 'Number of Deadlocks/sec'       THEN      cntr_value                          ELSE @deadlockCount            END \n" +
			    "from sys." + dm_os_performance_counters + " \n" +
			    "where counter_name in ('Stolen Server Memory (KB)', 'Database Cache Memory (KB)', 'Granted Workspace Memory (KB)', 'Number of Deadlocks/sec') \n" +
			    "  and instance_name in ('', '_Total') \n"
				) +

			    " \n" +
			    "/*------- WorkerThreads -- dm_os_schedulers -------*/ \n" +
				( ! hasViewServerState ? "/* no='VIEW SERVER STATE' */" : 
			    "select \n" + 
			    "     @wt_usedThreads               = SUM(active_workers_count) \n" +
			    "    ,@wt_availableThreads          = @max_workers_count - SUM(active_workers_count) \n" +
			    "    ,@wt_workersWaitingForCPU      = SUM(runnable_tasks_count) \n" +
			    "    ,@wt_requestsWaitingForThreads = SUM(work_queue_count) \n" +
			    "    ,@wt_allocatedWorkers          = SUM(current_workers_count) \n" +
			    "from sys.dm_os_schedulers \n" +
			    "where status = 'VISIBLE ONLINE' \n"
			    ) +
			    " \n" +
				"/*------- Get info about Open Transactions -------*/\n" +
				( ! hasViewServerState ? "/* no='VIEW SERVER STATE' */" : 
				"select @oldestOpenTranBeginTime = min(database_transaction_begin_time) \n" +
				"                                  from sys." + dm_tran_database_transactions + " \n" +
				"                                  where database_transaction_begin_time is not null \n" +
				"                                    and database_transaction_type  = 1 /* 1=read/write transaction, 2=Read-only transaction, 3=System transaction */  \n" +
				"                                    and database_transaction_log_record_count > 0 \n" + // since SQL-Server 2008
				"                                    " + notInTempdb + " \n" +
				"select @oldestOpenTranInSec     = isnull(datediff(ss, @oldestOpenTranBeginTime, getdate()),0) \n" +
				"\n" +
				"if (@oldestOpenTranBeginTime is not null) \n" +
				"begin \n" +
				"    select @oldestOpenTranId        = min(transaction_id) \n" +
				"                                      from sys." + dm_tran_database_transactions + " \n" +
				"                                      where database_transaction_begin_time = @oldestOpenTranBeginTime \n" +
				"    select @oldestOpenTranSpid      = session_id \n" +
				"                                      from sys." + dm_tran_session_transactions + " \n" +
				"                                      where transaction_id = @oldestOpenTranId \n" +
				"    select @oldestOpenTranDbname    = isnull(db_name(database_id), convert(varchar(15), database_id))  \n" +
				"                                      from sys." + dm_tran_database_transactions + "  \n" +
				"                                      where transaction_id = @oldestOpenTranId \n" +
				"    select @oldestOpenTranName      = name  \n" +
				"                                      from sys." + dm_tran_active_transactions + " \n" +
				"                                      where transaction_id = @oldestOpenTranId \n" +
//				"    select @oldestOpenTranInSec     = isnull(datediff(ss, database_transaction_begin_time, getdate()),0) \n" +
//				"                                      from sys." + dm_tran_database_transactions + "  \n" +
//				"                                      where transaction_id = @oldestOpenTranId \n" +
				"    if (@oldestOpenTranSpid is not null) \n" +
				"    begin \n" +
				"        select @oldestOpenTranWaitType      = coalesce(wait_type, last_wait_type) \n" +
				"              ,@oldestOpenTranCmd           = command \n" +
				"                                              from sys.dm_exec_requests  \n" +
				"                                              where session_id = @oldestOpenTranSpid \n" +
				"        select @oldestOpenTranLoginName     = login_name \n" +
				"                                              from sys.dm_exec_sessions \n" +
				"                                              where session_id = @oldestOpenTranSpid \n" +
//				"        select @oldestOpenTranTempdbUsageMb = CAST( ( \n" + // The below calculations also used in: CmTempdbSpidUsage
//				"                                                          (ts.user_objects_alloc_page_count - ts.user_objects_dealloc_page_count - " + user_objects_deferred_dealloc_page_count + ") \n" +  
//				"                                                        + (ts.internal_objects_alloc_page_count - ts.internal_objects_dealloc_page_count) \n" +
//				"                                                    ) / 128.0 AS decimal(12,1) \n" +
//				"                                                  ) \n" +
//				"                                              from tempdb.sys.dm_db_session_space_usage ts \n" + 
//				"                                              where ts.session_id = @oldestOpenTranSpid \n" +
				"    end \n" +
				"end \n"
				) +
				"\n" +
				"/* And some other metrics */\n" +
				"select @LockWaits           = count(*) FROM sys.sysprocesses WHERE blocked != 0 AND waittime >= " + (LockWaitsThresholdSec * 1000) + " \n" +
				"select @LockWaitThreshold   = " + (LockWaitsThresholdSec * 1000) + " \n" +
				"select @Connections         = count(*)            FROM sys.sysprocesses WHERE sid != 0x01 \n" +
				"select @distinctLogins      = count(distinct sid) FROM sys.sysprocesses WHERE sid != 0x01 \n" +
				"select @maxSqlExecTimeInSec = max(isnull(datediff(ss, start_time, getdate()),0))  \n" +
				"                             from sys.dm_exec_requests x  \n" +
				"                             where x.connection_id is not null \n" +
				"                               and x.transaction_id > 0 \n" +
				"select @fullTranslogCount   = 0 \n" +
				" \n" +
				"/* Output the data */\n" +
				"select /* ${cmCollectorName} */ \n" +
				"      srvVersion                          = @@version  \n" +
				"    , atAtServerName                      = @@servername  \n" +
				"    , clusterInstanceId                   = 'Not Enabled'  \n" +
				"    , clusterCoordId                      = 'Not Enabled'  \n" +
				"    , timeIsNow                           = getdate()  \n" +
				"    , utcTimeDiff                         = datediff(mi, getutcdate(), getdate())  \n" +
				"    , OnHostName                          = convert(varchar(100), SERVERPROPERTY('MachineName')) \n" +
				"    , NetworkAddressInfo                  = @listeners \n" +
				"    , srvPageSize                         = convert(int, 8196)--@@maxpagesize  \n" +
				"    , LockWaits                           = @LockWaits                    \n" +
				"    , LockWaitThreshold                   = @LockWaitThreshold            \n" +
				"    , RootBlockerSpids                    = convert(varchar(128), '')     \n" +
				"    , cpu_busy                            = @@cpu_busy                    \n" +
				"    , cpu_io                              = @@io_busy                     \n" +
				"    , cpu_idle                            = @@idle                        \n" +
				"    , io_total_read                       = @@total_read                  \n" +
				"    , io_total_write                      = @@total_write                 \n" +
				"    , Connections                         = @Connections                  \n" + 
				"    , aaConnections                       = @@connections                 \n" +
				"    , distinctLogins                      = @distinctLogins               \n" +
				"    , fullTranslogCount                   = @fullTranslogCount            \n" +
				suspectPageCount +
				suspectPageErrors +
				"    , tempdbUsageMbAll                    = convert(numeric(12,1), NULL)  \n" +
				"    , tempdbUsageMbUser                   = convert(numeric(12,1), NULL)  \n" +
				"    , tempdbUsageMbInternal               = convert(numeric(12,1), NULL)  \n" +
				"    , oldestOpenTranBeginTime             = @oldestOpenTranBeginTime      \n" +
				"    , oldestOpenTranId                    = @oldestOpenTranId             \n" +
				"    , oldestOpenTranSpid                  = @oldestOpenTranSpid           \n" +
				"    , oldestOpenTranDbname                = @oldestOpenTranDbname         \n" +
				"    , oldestOpenTranName                  = @oldestOpenTranName           \n" +
				"    , oldestOpenTranWaitType              = @oldestOpenTranWaitType       \n" +
				"    , oldestOpenTranCmd                   = @oldestOpenTranCmd            \n" +
				"    , oldestOpenTranLoginName             = @oldestOpenTranLoginName      \n" +
				"    , oldestOpenTranTempdbUsageMbAll      = convert(numeric(12,1), NULL)  \n" +
				"    , oldestOpenTranTempdbUsageMbUser     = convert(numeric(12,1), NULL)  \n" +
				"    , oldestOpenTranTempdbUsageMbInternal = convert(numeric(12,1), NULL)  \n" +
				"    , oldestOpenTranInSec                 = @oldestOpenTranInSec          \n" +
				"    , oldestOpenTranInSecThreshold        = @oldestOpenTranInSecThreshold \n" +
				"    , maxSqlExecTimeInSec                 = @maxSqlExecTimeInSec          \n" +

				"    , StartDate                           = @start_date \n" +
				"    , DaysRunning                         = @days_running \n" +

				"    , pack_received                       = @@pack_received  \n" +
				"    , pack_sent                           = @@pack_sent      \n" +
				"    , packet_errors                       = @@packet_errors  \n" +
				"    , total_errors                        = @@total_errors   \n" +

			    "    , total_os_memory_mb                  = @total_os_memory_mb \n" +
			    "    , available_physical_memory_mb        = @available_os_memory_mb \n" +
			    "    , system_high_memory_signal_state     = @system_high_memory_signal_state \n" +
			    "    , system_low_memory_signal_state      = @system_low_memory_signal_state \n" +
				
				"    , Total_Server_Memory_MB              = @Total_Server_Memory_MB \n" +
				"    , Target_Server_Memory_MB             = @Target_Server_Memory_MB \n" +
				"    , TargetVsTotal_diff_MB               = @TargetVsTotal_diff_MB \n" +
				"    , scheduler_count                     = @scheduler_count \n" +
				"    , ms_ticks                            = @ms_ticks \n" +
				"    , process_kernel_time_ms              = @process_kernel_time_ms \n" +
				"    , process_user_time_ms                = @process_user_time_ms \n" +
				"    , memory_used_by_sqlserver_MB         = @memory_used_by_sqlserver_MB \n" +
				"    , locked_pages_used_by_sqlserver_MB   = @locked_pages_used_by_sqlserver_MB \n" +
				"    , process_memory_utilization_pct      = @process_memory_utilization_pct \n" +
				"    , process_physical_memory_low         = @process_physical_memory_low \n" +
				"    , process_virtual_memory_low          = @process_virtual_memory_low \n" +
				
				"    , databaseCacheMemoryMb               = @databaseCacheMemoryMb \n" +
				"    , grantedWorkspaceMemoryMb            = @grantedWorkspaceMemoryMb \n" +
				"    , stolenServerMemoryMb                = @stolenServerMemoryMb \n" +
				"    , deadlockCount                       = @deadlockCount \n" +

				// Worker Threads...
			    "    , maxWorkers                          = @max_workers_count \n" +
			    "    , usedWorkers                         = @wt_usedThreads \n" +
			    "    , availableWorkers                    = @wt_availableThreads \n" +
			    "    , workersWaitingForCPU                = @wt_workersWaitingForCPU \n" +
			    "    , requestsWaitingForWorkers           = @wt_requestsWaitingForThreads \n" +
			    "    , allocatedWorkers                    = @wt_allocatedWorkers \n" +
				"";

		return sql;
	}
	
	@Override
	protected int refreshGetData(DbxConnection conn) 
	throws Exception
	{
		// Do NORMAL refresh
		int rowCount = super.refreshGetData(conn);

		// Reset last suspect RSTM
		_lastSuspectPage_rstm = null;
		_lastRootBlockersList = null;

		boolean hasViewServerState = false;
		List<String> activeRoles = conn.getActiveServerRolesOrPermissions();
		if (activeRoles != null)
		{
			hasViewServerState = activeRoles.contains("VIEW SERVER STATE");
		}
		
		// Root Blockers
		boolean getRootBlockerSpids = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sample_rootBlockerSpids, DEFAULT_sample_rootBlockerSpids);
		if (getRootBlockerSpids)
		{
			Integer LockWaits = getAbsValueAsInteger(0, "LockWaits");
			if (LockWaits != null && LockWaits > 0)
			{
				long startTime = System.currentTimeMillis();

				// Get data
				_lastRootBlockersList = SqlServerUtils.getRootBlockerList(conn);

				long refreshTime = TimeUtils.msDiffNow(startTime);
				if (refreshTime > 1_000) // more than 1 seconds... write warning
				{
					_logger.warn("Refreshing 'Root Blockers' took " + refreshTime + " ms. This is a bit long, the threshold for this message is 1 seconds.");
				}
			}
		}

		// Get Suspect count...
		Integer suspectPageCount = getAbsValueAsInteger(0, "suspectPageCount");
		if (suspectPageCount != null && suspectPageCount > 0)
		{
			String sql = getSql_suspectPageInfo(conn.getDbmsVersionInfo());
			try
			{
				int queryTimeout = 5;
				_lastSuspectPage_rstm = ResultSetTableModel.executeQuery(conn, sql, queryTimeout, "suspectPageInfo");
			}
			catch (SQLException ex)
			{
				_logger.warn("Problems getting SQL Server 'suspect pages' from 'msdb.dbo.suspect_pages'. Skipping this and continuing. SQL=" + sql, ex);
			}
		}

		// refresh tempdb usage info
		boolean getTempdbSpidUsage = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sample_tempdbSpidUsage, DEFAULT_sample_tempdbSpidUsage);
		if (getTempdbSpidUsage && hasViewServerState)
		{
			long startTime = System.currentTimeMillis();

			// Refresh
			TempdbUsagePerSpid.getInstance().refresh(conn);

			long refreshTime = TimeUtils.msDiffNow(startTime);
			if (refreshTime > 1_000) // more than 1 seconds... write warning
			{
				_logger.warn("Refreshing 'tempdb usage information' took " + refreshTime + " ms. This is a bit long, the threshold for this message is 1 seconds.");
			}
		}
		
		return rowCount;
	}
	
	public ResultSetTableModel get_lastSuspectPage_rstm()
	{
		return _lastSuspectPage_rstm;
	}

	public List<Integer> get_lastRootBlockersList()
	{
		return _lastRootBlockersList;
	}

	public static String getSql_suspectPageInfo(DbmsVersionInfo versionInfo)
	{
		String sql = ""
			    + "select /* " + Version.getAppName() + ":" + CM_NAME + " */ \n"
			    + "     sp.database_id \n"
			    + "    ,dbname             = db_name(sp.database_id) \n"
			    + "    ,sp.file_id \n"
			    + "    ,file_size_mb       = convert(numeric(10,1), mf.size / 128.0) \n"
			    + "    ,file_type          = mf.type_desc \n"
			    + "    ,logical_file_name  = mf.name \n"
			    + "    ,physical_file_name = mf.physical_name \n"
			    + "    ,sp.page_id \n"
			    + "    ,sp.event_type \n"
			    + "    ,event_type_desc = \n"
			    + "     CASE sp.event_type \n"
			    + "          WHEN 1 THEN '823 or 824 error' \n"
			    + "          WHEN 2 THEN 'Bad checksum' \n"
			    + "          WHEN 3 THEN 'Torn page' \n"
			    + "          WHEN 4 THEN 'Restored' \n"
			    + "          WHEN 5 THEN 'Repaired' \n"
			    + "          WHEN 7 THEN 'Deallocated' \n"
			    + "          ELSE '-unknown-' \n"
			    + "     END \n"
			    + "    ,sp.error_count \n"
			    + "    ,sp.last_update_date \n"
			    + "from msdb.dbo.suspect_pages sp \n"
			    + "join sys.master_files mf on sp.file_id = mf.file_id and sp.database_id = mf.database_id \n"
			    + "";

		// in 2019 -- get some page information, so we can see what TableName, IndexId, PageTypeDesc (etc)
		if (versionInfo != null && versionInfo.getLongVersion() > Ver.ver(2019))
		{
			sql = ""
				    + "select /* " + Version.getAppName() + ":" + CM_NAME + " */ \n"
				    + "     sp.database_id \n"
				    + "    ,dbname             = db_name(sp.database_id) \n"
				    + "    ,sp.file_id \n"
				    + "    ,file_size_mb       = convert(numeric(10,1), mf.size / 128.0) \n"
				    + "    ,file_type          = mf.type_desc \n"
				    + "    ,logical_file_name  = mf.name \n"
				    + "    ,physical_file_name = mf.physical_name \n"
				    + "    ,object_name = object_name(pi.object_id, pi.database_id) \n" // 2019
				    + "    ,pi.index_id \n"                                             // 2019
				    + "    ,pi.page_type_desc \n"                                       // 2019
				    + "    ,sp.page_id \n"
				    + "    ,sp.event_type \n"
				    + "    ,event_type_desc = \n"
				    + "     CASE sp.event_type \n"
				    + "          WHEN 1 THEN '823 or 824 error' \n"
				    + "          WHEN 2 THEN 'Bad checksum' \n"
				    + "          WHEN 3 THEN 'Torn page' \n"
				    + "          WHEN 4 THEN 'Restored' \n"
				    + "          WHEN 5 THEN 'Repaired' \n"
				    + "          WHEN 7 THEN 'Deallocated' \n"
				    + "          ELSE '-unknown-' \n"
				    + "     END \n"
				    + "    ,sp.error_count \n"
				    + "    ,sp.last_update_date \n"
				    + "from msdb.dbo.suspect_pages sp \n"
				    + "join sys.master_files mf on sp.file_id = mf.file_id and sp.database_id = mf.database_id \n"
				    + "outer apply sys.dm_db_page_info(sp.database_id, sp.file_id, sp.page_id, 'DETAILED') pi \n" // 2019
				    + "";
		}

		return sql;
	}

	@Override
	public void localCalculation(CounterSample newSample)
	{
		// Refresh local members used by GET methods (so that other CM's can use the same values)
		_lastTargetServerMemoryMb = newSample.getValueAsInteger(0, "Target_Server_Memory_MB", false, -1);
		_lastTotalServerMemoryMb  = newSample.getValueAsInteger(0, "Total_Server_Memory_MB" , false, -1);

		// Get tempdb SPID Usage
		boolean getTempdbSpidUsage = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sample_tempdbSpidUsage, DEFAULT_sample_tempdbSpidUsage);
		if (getTempdbSpidUsage)
		{
			// set ...
			if (newSample.findColumn("tempdbUsageMbAll") != -1)
			{
				TempdbUsagePerSpid inst = TempdbUsagePerSpid.getInstance();
				
				newSample.setValueAt(inst.getTotalSpaceUsedInMb()         , 0, "tempdbUsageMbAll");
				newSample.setValueAt(inst.getUserObjectSpaceUsedInMb()    , 0, "tempdbUsageMbUser");
				newSample.setValueAt(inst.getInternalObjectSpaceUsedInMb(), 0, "tempdbUsageMbInternal");
			}
			
			// 1: Get column position for 'oldestOpenTranSpid', and if it was found:
			//    2: Get value for 'oldestOpenTranSpid', and if we have a value:
			//       3: Get tempdb info about 'oldestOpenTranSpid', and if we have a value:
			//          4: Set values for columns
			//             - oldestOpenTranTempdbUsageMbAll
			//             - oldestOpenTranTempdbUsageMbUser
			//             - oldestOpenTranTempdbUsageMbInternal
			//             (if the above columns can't be found... Simply write a message to the error log)
			int oldestOpenTranSpid_pos = newSample.findColumn("oldestOpenTranSpid");
			if (oldestOpenTranSpid_pos != -1)
			{
				int oldestOpenTranSpid = newSample.getValueAsInteger(0, oldestOpenTranSpid_pos, -1);
				if (oldestOpenTranSpid != -1)
				{
					TempdbUsagePerSpid.TempDbSpaceInfo spaceInfo = TempdbUsagePerSpid.getInstance().getEntryForSpid(oldestOpenTranSpid);
					if (spaceInfo != null)
					{
						newSample.setValueAt(spaceInfo.getTotalSpaceUsedInMb()         , 0, "oldestOpenTranTempdbUsageMbAll");
						newSample.setValueAt(spaceInfo.getUserObjectSpaceUsedInMb()    , 0, "oldestOpenTranTempdbUsageMbUser");
						newSample.setValueAt(spaceInfo.getInternalObjectSpaceUsedInMb(), 0, "oldestOpenTranTempdbUsageMbInternal");
					}
				}
			}
		}
		
		// Root Blocker SPIDs
		if (_lastRootBlockersList != null && !_lastRootBlockersList.isEmpty())
		{
			// set ...
			if (newSample.findColumn("RootBlockerSpids") != -1)
			{
				String rootBlockersCsv = StringUtil.toCommaStr(_lastRootBlockersList);
				newSample.setValueAt(rootBlockersCsv, 0, "RootBlockerSpids");
			}
		}
	}
	
	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
//		long srvVersion = getServerVersion();

		//---------------------------------
		// GRAPH:
		//---------------------------------
//		if (GRAPH_NAME_XXX.equals(tgdp.getName()))
//		{	
//			Double[] arr = new Double[3];
//
//			int ms = (int) (System.currentTimeMillis() % 1000l);
//			ms = ms < 0 ? ms+1000 : ms;
//
//			Calendar now = Calendar.getInstance();
//			int hour   = now.get(Calendar.HOUR_OF_DAY);
//			int minute = now.get(Calendar.MINUTE);
//			int second = now.get(Calendar.SECOND);
//			
//			arr[0] = new Double(hour);
//			arr[1] = new Double(minute);
//			arr[2] = new Double(second);
//			_logger.debug("updateGraphData("+tgdp.getName()+"): hour='"+arr[0]+"', minute='"+arr[1]+"', second='"+arr[2]+"'.");
//
//			// Set the values
//			tgdp.setDataPoint(this.getTimestamp(), arr);
//		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_AA_CPU.equals(tgdp.getName()))
		{
			// FIXME: in ASE 15.7 threaded mode, the @@cpu_io seems to use the DiskController Thread for all engines...
			//        This leads to a higher IO Usage than it *really* is...
			//        One way to solve this would be to simply use @@cpu_busy and disregard @@cpu_io totally
			Double cpuUser        = getDiffValueAsDouble(0, "cpu_busy");
			Double cpuSystem      = getDiffValueAsDouble(0, "cpu_io");
			Double cpuIdle        = getDiffValueAsDouble(0, "cpu_idle");
			if (cpuUser != null && cpuSystem != null && cpuIdle != null)
			{
				double CPUTime   = cpuUser  .doubleValue() + cpuSystem.doubleValue() + cpuIdle.doubleValue();
				double CPUUser   = cpuUser  .doubleValue();
				double CPUSystem = cpuSystem.doubleValue();
//				double CPUIdle   = cpuIdle  .doubleValue();

				BigDecimal calcCPUTime       = new BigDecimal( ((1.0 * (CPUUser + CPUSystem)) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				BigDecimal calcUserCPUTime   = new BigDecimal( ((1.0 * (CPUUser            )) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				BigDecimal calcSystemCPUTime = new BigDecimal( ((1.0 * (CPUSystem          )) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//				BigDecimal calcIdleCPUTime   = new BigDecimal( ((1.0 * (CPUIdle            )) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

//				_cpuTime_txt          .setText(calcCPUTime      .toString());
//				_cpuUser_txt          .setText(calcUserCPUTime  .toString());
//				_cpuSystem_txt        .setText(calcSystemCPUTime.toString());
//				_cpuIdle_txt          .setText(calcIdleCPUTime  .toString());

				Double[] arr = new Double[3];

				arr[0] = calcCPUTime      .doubleValue();
				arr[1] = calcSystemCPUTime.doubleValue();
				arr[2] = calcUserCPUTime  .doubleValue();
				_logger.debug("updateGraphData("+tgdp.getName()+"): @@cpu_busy+@@cpu_io='"+arr[0]+"', @@cpu_io='"+arr[1]+"', @@cpu_busy='"+arr[2]+"'.");

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
//		if (GRAPH_NAME_SYS_INFO_CPU.equals(tgdp.getName()))
//		{
//			Double scheduler_count        = getDiffValueAsDouble(0, "scheduler_count");
//			Double ms_ticks               = getDiffValueAsDouble(0, "ms_ticks");
//			Double process_kernel_time_ms = getDiffValueAsDouble(0, "process_kernel_time_ms");
//			Double process_user_time_ms   = getDiffValueAsDouble(0, "process_user_time_ms");
//
//			if (ms_ticks != null && process_kernel_time_ms != null && process_user_time_ms != null)
//			{
//				double CPUTime   = (scheduler_count.doubleValue() * ms_ticks.doubleValue());
//				double CPUSystem = process_kernel_time_ms.doubleValue();
//				double CPUUser   = process_user_time_ms  .doubleValue();
//
//				BigDecimal calcCPUTime       = new BigDecimal( ((1.0 * (CPUUser + CPUSystem)) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//				BigDecimal calcSystemCPUTime = new BigDecimal( ((1.0 * (CPUSystem          )) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//				BigDecimal calcUserCPUTime   = new BigDecimal( ((1.0 * (CPUUser            )) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//
//				Double[] arr = new Double[3];
//
//				arr[0] = calcCPUTime      .doubleValue();
//				arr[1] = calcSystemCPUTime.doubleValue();
//				arr[2] = calcUserCPUTime  .doubleValue();
//				_logger.debug("updateGraphData("+tgdp.getName()+"): kernel_time+user_time='"+arr[0]+"', kernel_time='"+arr[1]+"', user_time='"+arr[2]+"'.");
//System.out.println("updateGraphData("+tgdp.getName()+"): kernel_time+user_time='"+arr[0]+"', kernel_time='"+arr[1]+"', user_time='"+arr[2]+"'.");
//
//				// Set the values
//				tgdp.setDataPoint(this.getTimestamp(), arr);
//			}
//		}
		

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_BLOCKING_LOCKS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];

			arr[0] = this.getAbsValueAsDouble (0, "LockWaits");
			_logger.debug("updateGraphData(BlockingLocksGraph): LockWait='"+arr[0]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_CONNECTION.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[4];

			arr[0] = this.getAbsValueAsDouble (0, "Connections");
			arr[1] = this.getAbsValueAsDouble (0, "distinctLogins");
			arr[2] = this.getDiffValueAsDouble(0, "aaConnections");
			arr[3] = this.getRateValueAsDouble(0, "aaConnections");
			_logger.debug("updateGraphData("+tgdp.getName()+"): Connections(Abs)='"+arr[0]+"', distinctLogins(Abs)='"+arr[1]+"', aaConnections(Diff)='"+arr[2]+"', aaConnections(Rate)='"+arr[3]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_CONNECTION_RATE.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[1];

			arr[0] = this.getRateValueAsDouble(0, "aaConnections");
			_logger.debug("updateGraphData("+tgdp.getName()+"): aaConnections(Rate)='"+arr[0]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_AA_DISK_READ_WRITE.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[2];

			arr[0] = this.getRateValueAsDouble (0, "io_total_read");
			arr[1] = this.getRateValueAsDouble (0, "io_total_write");
			_logger.debug("updateGraphData("+tgdp.getName()+"): io_total_read='"+arr[0]+"', io_total_write='"+arr[1]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_AA_NW_PACKET.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[3];

			arr[0] = this.getRateValueAsDouble (0, "pack_received");
			arr[1] = this.getRateValueAsDouble (0, "pack_sent");
			arr[2] = this.getRateValueAsDouble (0, "packet_errors");
			_logger.debug("updateGraphData("+tgdp.getName()+"): packet_errors='"+arr[0]+"', total_errors='"+arr[1]+"', packet_errors='"+arr[2]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_OLDEST_TRAN_IN_SEC.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[1];

			arr[0] = this.getAbsValueAsDouble(0, "oldestOpenTranInSec");
			_logger.debug("updateGraphData("+tgdp.getName()+"): oldestOpenTranInSec='"+arr[0]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_MAX_SQL_EXEC_TIME_IN_SEC.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[1];

			arr[0] = this.getAbsValueAsDouble(0, "maxSqlExecTimeInSec");
			_logger.debug("updateGraphData("+tgdp.getName()+"): maxSqlExecTimeInSec='"+arr[0]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_TEMPDB_SPID_USAGE.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[3];

			arr[0] = this.getAbsValueAsDouble(0, "tempdbUsageMbAll"     , true, -1d);
			arr[1] = this.getAbsValueAsDouble(0, "tempdbUsageMbUser"    , true, -1d);
			arr[2] = this.getAbsValueAsDouble(0, "tempdbUsageMbInternal", true, -1d);

			_logger.debug("updateGraphData("+tgdp.getName()+"): tempdbUsageMbAll='"+arr[0]+"', tempdbUsageMbUser='"+arr[1]+"', tempdbUsageMbInternal='"+arr[2]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_TARGET_AND_TOTAL_MEM_MB.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[2];

			arr[0] = this.getAbsValueAsDouble(0, "Target_Server_Memory_MB", true, -1d);
			arr[1] = this.getAbsValueAsDouble(0, "Total_Server_Memory_MB" , true, -1d);

			_logger.debug("updateGraphData("+tgdp.getName()+"): Target_Server_Memory_MB='"+arr[0]+"', Total_Server_Memory_MB='"+arr[1]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_MEMORY_UTILAZATION_PCT.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[1];

			arr[0] = this.getAbsValueAsDouble(0, "process_memory_utilization_pct", true, -1d);

			_logger.debug("updateGraphData("+tgdp.getName()+"): process_memory_utilization_pct='"+arr[0]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_OS_MEMORY_FREE_MB.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[1];

			arr[0] = this.getAbsValueAsDouble(0, "available_physical_memory_mb", true, -1d);

			_logger.debug("updateGraphData("+tgdp.getName()+"): available_physical_memory_mb='"+arr[0]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_PERFMON_MEMORY.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[3];

			arr[0] = this.getAbsValueAsDouble(0, "databaseCacheMemoryMb"   , true, 0d);
			arr[1] = this.getAbsValueAsDouble(0, "grantedWorkspaceMemoryMb", true, 0d);
			arr[2] = this.getAbsValueAsDouble(0, "stolenServerMemoryMb"    , true, 0d);

			_logger.debug("updateGraphData(" + tgdp.getName() + "): databaseCacheMemoryMb='" + arr[0] + "', databaseCacheMemoryMb='" + arr[1] + "', databaseCacheMemoryMb='" + arr[2] + "'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_WORKER_THREAD_USAGE.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[6];

			arr[0] = this.getAbsValueAsDouble(0, "maxWorkers"               , true, 0d);
			arr[1] = this.getAbsValueAsDouble(0, "usedWorkers"              , true, 0d);
			arr[2] = this.getAbsValueAsDouble(0, "availableWorkers"         , true, 0d);
			arr[3] = this.getAbsValueAsDouble(0, "workersWaitingForCPU"     , true, 0d);
			arr[4] = this.getAbsValueAsDouble(0, "requestsWaitingForWorkers", true, 0d);
			arr[5] = this.getAbsValueAsDouble(0, "allocatedWorkers"         , true, 0d);

			_logger.debug("updateGraphData(" + tgdp.getName() + "): maxWorkers='" + arr[0] + "', usedWorkers='" + arr[1] + "', availableWorkers='" + arr[2] + "', workersWaitingForCPU='" + arr[3] + "', requestsWaitingForWorkers='" + arr[4] + "', allocatedWorkers='" + arr[5] + "'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_WT_WAITING_FOR_CPU.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[2];

			arr[0] = this.getAbsValueAsDouble(0, "workersWaitingForCPU", true, 0d);
			arr[1] = NumberUtils.round(arr[0] / this.getAbsValueAsDouble(0, "scheduler_count", true, 1d), 2);

			_logger.debug("updateGraphData(" + tgdp.getName() + "): workersWaitingForCPU='" + arr[0] + "', workersWaitingForCPU_perScheduler='" + arr[1] + "'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_TASKS_WAITING_FOR_WORKERS.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[1];

			arr[0] = this.getAbsValueAsDouble(0, "requestsWaitingForWorkers", true, 0d);

			_logger.debug("updateGraphData(" + tgdp.getName() + "): requestsWaitingForWorkers='" + arr[0] + "'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_DEADLOCK_COUNT_SUM.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[1];

			arr[0] = this.getDiffValueAsDouble(0, "deadlockCount", true, 0d);

			_logger.debug("updateGraphData(" + tgdp.getName() + "): deadlockCount='" + arr[0] + "'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
		
		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_MEMORY_PRESSURE.equals(tgdp.getName()))
		{	
			boolean osMem_system_high_memory_signal_state = "true".equalsIgnoreCase(this.getAbsString (0, "system_high_memory_signal_state"));
			boolean osMem_system_low_memory_signal_state  = "true".equalsIgnoreCase(this.getAbsString (0, "system_low_memory_signal_state"));
			boolean memProcessPhysicalMemoryLow           = "true".equalsIgnoreCase(this.getAbsString (0, "process_physical_memory_low"));
			boolean memProcessVirtualMemoryLow            = "true".equalsIgnoreCase(this.getAbsString (0, "process_virtual_memory_low"));

			Double[] arr = new Double[4];

			arr[0] = new Double( osMem_system_high_memory_signal_state ? 1 : 0);
			arr[1] = new Double( osMem_system_low_memory_signal_state  ? 1 : 0);
			arr[2] = new Double( memProcessPhysicalMemoryLow           ? 1 : 0);
			arr[3] = new Double( memProcessVirtualMemoryLow            ? 1 : 0);

			_logger.debug("updateGraphData(" + tgdp.getName() + "): osMem_system_high_memory_signal_state=" + arr[0] + ", osMem_system_low_memory_signal_state=" + arr[1] + ", memProcessPhysicalMemoryLow=" + arr[2] + ", memProcessVirtualMemoryLow=" + arr[3]);

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}
	
	@Override
	public boolean isGraphDataHistoryEnabled(String name)
	{
		// ENABLED for the following graphs
		if (GRAPH_NAME_AA_CPU                   .equals(name)) return true; // Used by CmWaitStat
//		if (GRAPH_NAME_BLOCKING_LOCKS           .equals(name)) return true;
//		if (GRAPH_NAME_CONNECTION               .equals(name)) return true;
//		if (GRAPH_NAME_CONNECTION_RATE          .equals(name)) return true;
//		if (GRAPH_NAME_AA_DISK_READ_WRITE       .equals(name)) return true;
//		if (GRAPH_NAME_AA_NW_PACKET             .equals(name)) return true;
//		if (GRAPH_NAME_OLDEST_TRAN_IN_SEC       .equals(name)) return true;
//		if (GRAPH_NAME_MAX_SQL_EXEC_TIME_IN_SEC .equals(name)) return true;
		if (GRAPH_NAME_TEMPDB_SPID_USAGE        .equals(name)) return true;  // Used by CmTempdbSpidUsage
		if (GRAPH_NAME_WORKER_THREAD_USAGE      .equals(name)) return true;  // Used locally, CmWaitStat
		if (GRAPH_NAME_TASKS_WAITING_FOR_WORKERS.equals(name)) return true;  // Used locally
//		if (GRAPH_NAME_TARGET_AND_TOTAL_MEM_MB  .equals(name)) return true;  // Used by CmWaitStat
		if (GRAPH_NAME_DEADLOCK_COUNT_SUM       .equals(name)) return true;  // Used locally

		// default: DISABLED
		return false;
	}
	@Override
	public int getGraphDataHistoryTimeInterval(String name)
	{
		// Keep interval: default is 60 minutes
		return super.getGraphDataHistoryTimeInterval(name);
	}
	
	@Override
	public void reset()
	{
		// deadlocks
		if (true)
		{
			int movingAverageInMinutes = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_DeadlockCountMovingAverageMinutes, DEFAULT_alarm_DeadlockCountMovingAverageMinutes);

			if (movingAverageInMinutes > 0)
				MovingAverageCounterManager.getInstance(CM_NAME, "deadlockCount", movingAverageInMinutes).reset();
			
			setClientProperty(PROPKEY_clientProp_deadlockCountOverRecordingPeriod, 0);
		}
		
		super.reset();
	}
	
	@Override
	public void prepareForPcsDatabaseRollover()
	{
		setClientProperty(PROPKEY_clientProp_deadlockCountOverRecordingPeriod, 0);

		super.prepareForPcsDatabaseRollover();
	}
	
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		if (diffData != null)
		{
			// Reset some negative counters to 0
			// Since we do NOT reset Negative Counters to Zero i CmSummary
			// There might be SOME counters that needs this
			for (int rowId=0; rowId < diffData.getRowCount(); rowId++) 
			{
				checkAndSetNc20(diffData, rowId, "io_total_read");
				checkAndSetNc20(diffData, rowId, "io_total_write");
				checkAndSetNc20(diffData, rowId, "pack_received");
				checkAndSetNc20(diffData, rowId, "pack_sent");
				checkAndSetNc20(diffData, rowId, "packet_errors");
			}
			
			// Set GLOBAL property which can be extracted later
			// This one is used for: doLastRecordingActionBeforeDatabaseRollover()
			// So we can extract "extra information" about "todays" DEADLOCKS (the plan is to use: sp_blitzLock)
			// This so we can use that in the Daily Summary Report 
			int deadlockCount = diffData.getValueAsInteger(0, "deadlockCount", true, -1);

			// Get previous value and add it to deadlockCount
			Object o_deadlockCountOverRecordingPeriod = getClientProperty(PROPKEY_clientProp_deadlockCountOverRecordingPeriod);
			if (o_deadlockCountOverRecordingPeriod != null && o_deadlockCountOverRecordingPeriod instanceof Number)
			{
				int current_deadlockCountOverRecordingPeriod = ((Number)o_deadlockCountOverRecordingPeriod).intValue();
//System.out.println(">>>>>>>>>>>>>>>> PROPKEY_clientProp_deadlockCountOverRecordingPeriod: current_deadlockCountOverRecordingPeriod="+current_deadlockCountOverRecordingPeriod+", thisSampleDeadlockCount="+deadlockCount+", setNewValue="+(deadlockCount + current_deadlockCountOverRecordingPeriod));
				deadlockCount += current_deadlockCountOverRecordingPeriod;
			}

			setClientProperty(PROPKEY_clientProp_deadlockCountOverRecordingPeriod, deadlockCount);
		}
	}
	private void checkAndSetNc20(CounterSample counters, int rowId, String columnName)
	{
		int colId = counters.findColumn(columnName);
		if (colId >= 0)
		{
			Object obj  = counters.getValueAt(rowId, colId);
			if (obj != null && obj instanceof Number)
			{
				//System.out.println("colId="+colId+", name='"+columnName+"', o="+obj);
				if (((Number)obj).intValue() < 0)
				{
					//System.out.println("colId="+colId+", name='"+columnName+"', setting to Integer(0)");
					counters.setValueAt(new Integer(0), rowId, colId);
				}
			}
		}
	}
	
	//--------------------------------------------------------------------
	// Alarm Handling
	//--------------------------------------------------------------------
	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasDiffData() )
			return;

		if ( ! AlarmHandler.hasInstance() )
			return;

		AlarmHandler alarmHandler = AlarmHandler.getInstance();

		CountersModel cm = this;

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());

		
		//-------------------------------------------------------
		// Blocking Locks
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("LockWaits"))
		{
			Double LockWaits = cm.getAbsValueAsDouble (0, "LockWaits");
			if (LockWaits != null)
			{
				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_LockWaits, DEFAULT_alarm_LockWaits);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", LockWaits='"+LockWaits+"'.");

				if (LockWaits.intValue() > threshold)
				{
					String rootBlockerSpids                    = cm.getAbsString(0, "RootBlockerSpids");

					String oldestOpenTranBeginTime             = cm.getAbsString(0, "oldestOpenTranBeginTime");
					String oldestOpenTranId                    = cm.getAbsString(0, "oldestOpenTranId");
					String oldestOpenTranSpid                  = cm.getAbsString(0, "oldestOpenTranSpid");
					String oldestOpenTranDbname                = cm.getAbsString(0, "oldestOpenTranDbname");
					String oldestOpenTranName                  = cm.getAbsString(0, "oldestOpenTranName");
					String oldestOpenTranWaitType              = cm.getAbsString(0, "oldestOpenTranWaitType");
					String oldestOpenTranCmd                   = cm.getAbsString(0, "oldestOpenTranCmd");
					String oldestOpenTranLoginName             = cm.getAbsString(0, "oldestOpenTranLoginName");
					String oldestOpenTranTempdbUsageMbAll      = cm.getAbsString(0, "oldestOpenTranTempdbUsageMbAll");
					String oldestOpenTranTempdbUsageMbUser     = cm.getAbsString(0, "oldestOpenTranTempdbUsageMbUser");
					String oldestOpenTranTempdbUsageMbInternal = cm.getAbsString(0, "oldestOpenTranTempdbUsageMbInternal");
					String oldestOpenTranInSec                 = cm.getAbsString(0, "oldestOpenTranInSec");
//					String oldestOpenTranInSecThreshold        = cm.getAbsString(0, "oldestOpenTranInSecThreshold");

					// Possibly get more information about that SPID (rootBlockerSpids)
					// - get info from CmProcesses about the SPID's
					String rootBlockerSpids_Sessions_htmlTable = "";
					if (StringUtil.hasValue(rootBlockerSpids))
					{
						List<String> rootBlockerSpidsList = StringUtil.parseCommaStrToList(rootBlockerSpids);
						CountersModel cmSessions = getCounterController().getCmByName(CmSessions.CM_NAME);

						for (String rootBlockerSpid : rootBlockerSpidsList)
						{
							int rootBlockerSpid_int = StringUtil.parseInt(rootBlockerSpid, -1);
							if (rootBlockerSpid_int != -1 && cmSessions != null)
							{
								Map<String, Object> whereMap = new HashMap<>();
								whereMap.put("session_id", rootBlockerSpid_int);
								List<Integer> rowIdList = cmSessions.getRateRowIdsWhere(whereMap);
								for (Integer pkRowId : rowIdList)
								{
									rootBlockerSpids_Sessions_htmlTable += cmSessions.toHtmlTableString(CountersModel.DATA_RATE, pkRowId, true, false, false);
								}
							}
						}
					}

					// Possibly get more information about that SPID (oldestOpenTranSpid)
					// - print "open" trans info
					// - get info from CmProcesses about the oldest open tran
					String oldestOpenTranSpid_Sessions_htmlTable = "";
					if (StringUtil.hasValue(oldestOpenTranSpid))
					{
						CountersModel cmSessions = getCounterController().getCmByName(CmSessions.CM_NAME);
						int oldestOpenTranSpid_int = StringUtil.parseInt(oldestOpenTranSpid, -1);
						if (oldestOpenTranSpid_int != -1 && cmSessions != null)
						{
							Map<String, Object> whereMap = new HashMap<>();
							whereMap.put("session_id", oldestOpenTranSpid_int);
							List<Integer> rowIdList = cmSessions.getRateRowIdsWhere(whereMap);
							for (Integer pkRowId : rowIdList)
							{
								oldestOpenTranSpid_Sessions_htmlTable += cmSessions.toHtmlTableString(CountersModel.DATA_RATE, pkRowId, true, false, false);
							}
						}
					}
					
					String extendedDescText = "" 
							+   "NumberOfWaitingLocks"                + "="  + LockWaits                           + ""
							+ ", RootBlockerSpids"                    + "='" + rootBlockerSpids                    + "'"
							+ ", OldestOpenTranBeginTime"             + "='" + oldestOpenTranBeginTime             + "'"
							+ ", OldestOpenTranId"                    + "="  + oldestOpenTranId                    + ""
							+ ", OldestOpenTranSpid"                  + "="  + oldestOpenTranSpid                  + ""
							+ ", OldestOpenTranDbname"                + "='" + oldestOpenTranDbname                + "'"
							+ ", OldestOpenTranName"                  + "='" + oldestOpenTranName                  + "'"
							+ ", OldestOpenTranWaitType"              + "='" + oldestOpenTranWaitType              + "'"
							+ ", OldestOpenTranCmd"                   + "='" + oldestOpenTranCmd                   + "'"
							+ ", OldestOpenTranLoginName"             + "='" + oldestOpenTranLoginName             + "'"
							+ ", OldestOpenTranTempdbUsageMbAll"      + "="  + oldestOpenTranTempdbUsageMbAll      + ""
							+ ", OldestOpenTranTempdbUsageMbUser"     + "="  + oldestOpenTranTempdbUsageMbUser     + ""
							+ ", OldestOpenTranTempdbUsageMbInternal" + "="  + oldestOpenTranTempdbUsageMbInternal + ""
							+ ", OldestOpenTranInSec"                 + "="  + oldestOpenTranInSec                 + ""
//							+ ", OldestOpenTranInSecThreshold"        + "="  + oldestOpenTranInSecThreshold        + ""
							;
					String extendedDescHtml = "" // NO-OuterHtml, NO-Borders 
							+ "<table class='dbx-table-basic'> \n"
							+ "    <tr> <td><b>Number of Waiting Locks               </b></td> <td>" + LockWaits                            + "</td> </tr> \n"
							+ "    <tr> <td><b>Root Blocker Spids (CSV)              </b></td> <td>" + rootBlockerSpids                     + "</td> </tr> \n"
							+ "    <tr> <td><b>Oldest Open Tran BeginTime            </b></td> <td>" + oldestOpenTranBeginTime              + "</td> </tr> \n"
							+ "    <tr> <td><b>Oldest Open Tran Id                   </b></td> <td>" + oldestOpenTranId                     + "</td> </tr> \n"
							+ "    <tr> <td><b>Oldest Open Tran Spid                 </b></td> <td>" + oldestOpenTranSpid                   + "</td> </tr> \n"
							+ "    <tr> <td><b>Oldest Open Tran Dbname               </b></td> <td>" + oldestOpenTranDbname                 + "</td> </tr> \n"
							+ "    <tr> <td><b>Oldest Open Tran Name                 </b></td> <td>" + oldestOpenTranName                   + "</td> </tr> \n"
							+ "    <tr> <td><b>Oldest Open Tran WaitType             </b></td> <td>" + oldestOpenTranWaitType               + "</td> </tr> \n"
							+ "    <tr> <td><b>Oldest Open Tran Cmd                  </b></td> <td>" + oldestOpenTranCmd                    + "</td> </tr> \n"
							+ "    <tr> <td><b>Oldest Open Tran LoginName            </b></td> <td>" + oldestOpenTranLoginName              + "</td> </tr> \n"
							+ "    <tr> <td><b>Oldest Open Tran TempdbUsageMbAll     </b></td> <td>" + oldestOpenTranTempdbUsageMbAll       + "</td> </tr> \n"
							+ "    <tr> <td><b>Oldest Open Tran TempdbUsageMbUser    </b></td> <td>" + oldestOpenTranTempdbUsageMbUser      + "</td> </tr> \n"
							+ "    <tr> <td><b>Oldest Open Tran TempdbUsageMbInternal</b></td> <td>" + oldestOpenTranTempdbUsageMbInternal  + "</td> </tr> \n"
							+ "    <tr> <td><b>Oldest Open Tran InSec                </b></td> <td>" + oldestOpenTranInSec                  + "</td> </tr> \n"
//							+ "    <tr> <td><b>Oldest Open Tran InSecThreshold       </b></td> <td>" + oldestOpenTranInSecThreshold         + "</td> </tr> \n"
							+ "</table> \n"
							;

					if (StringUtil.hasValue(rootBlockerSpids_Sessions_htmlTable))
					{
						extendedDescHtml += ""
								+ "<br>"
								+ "Sessions information for 'Root Blocker SPID's: " + rootBlockerSpids + "'"
								+ rootBlockerSpids_Sessions_htmlTable
								;
					}
					
					if (StringUtil.hasValue(oldestOpenTranSpid_Sessions_htmlTable))
					{
						extendedDescHtml += ""
								+ "<br>"
								+ "Sessions information for 'Oldest Open Tran Spid: " + oldestOpenTranSpid + "'"
								+ oldestOpenTranSpid_Sessions_htmlTable
								;
					}
					
					AlarmEvent ae = new AlarmEventBlockingLockAlarm(cm, threshold, LockWaits);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);
					
					alarmHandler.addAlarm( ae );
				}
			}
		}


		//-------------------------------------------------------
		// Long running transaction
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("oldestOpenTranInSec"))
		{
			Integer oldestOpenTranInSec = cm.getAbsValueAsInteger(0, "oldestOpenTranInSec", -1);
			Integer oldestOpenTranSpid  = cm.getAbsValueAsInteger(0, "oldestOpenTranSpid" , -1);

			// Only continue if 'oldestOpenTranSpid' HAS a value... otherwise it's probably a internal transaction, which MAY not have an impact
			if (oldestOpenTranInSec != -1 && oldestOpenTranSpid != -1 )
			{
				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_oldestOpenTranInSec, DEFAULT_alarm_oldestOpenTranInSec);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", oldestOpenTranInSec='"+oldestOpenTranInSec+"'.");

				if (oldestOpenTranInSec > threshold)
				{
					// Get config 'skip some known values'
					String skipDbnameRegExp   = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_oldestOpenTranInSecSkipDbname,   DEFAULT_alarm_oldestOpenTranInSecSkipDbname);
					String skipLoginRegExp    = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_oldestOpenTranInSecSkipLogin,    DEFAULT_alarm_oldestOpenTranInSecSkipLogin);
					String skipCmdRegExp      = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_oldestOpenTranInSecSkipCmd,      DEFAULT_alarm_oldestOpenTranInSecSkipCmd);
					String skipTranNameRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_oldestOpenTranInSecSkipTranName, DEFAULT_alarm_oldestOpenTranInSecSkipTranName);
					String skipWaitTypeRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_oldestOpenTranInSecSkipWaitType, DEFAULT_alarm_oldestOpenTranInSecSkipWaitType);

					String oldestOpenTranBeginTime             = cm.getAbsString       (0, "oldestOpenTranBeginTime");
					String oldestOpenTranId                    = cm.getAbsString       (0, "oldestOpenTranId");
//					String oldestOpenTranSpid                  = cm.getAbsString       (0, "oldestOpenTranSpid");
					String oldestOpenTranDbname                = cm.getAbsString       (0, "oldestOpenTranDbname");
					String oldestOpenTranName                  = cm.getAbsString       (0, "oldestOpenTranName");
					String oldestOpenTranWaitType              = cm.getAbsString       (0, "oldestOpenTranWaitType");
					String oldestOpenTranCmd                   = cm.getAbsString       (0, "oldestOpenTranCmd");
					String oldestOpenTranLoginName             = cm.getAbsString       (0, "oldestOpenTranLoginName");
					Double oldestOpenTranTempdbUsageMbAll      = cm.getAbsValueAsDouble(0, "oldestOpenTranTempdbUsageMbAll");
					String oldestOpenTranTempdbUsageMbUser     = cm.getAbsString       (0, "oldestOpenTranTempdbUsageMbUser");
					String oldestOpenTranTempdbUsageMbInternal = cm.getAbsString       (0, "oldestOpenTranTempdbUsageMbInternal");
//					String oldestOpenTranInSec                 = cm.getAbsString       (0, "oldestOpenTranInSec");
					
					// note: this must be set to true at start, otherwise all below rules will be disabled (it "stops" processing at first doAlarm==false)
					boolean doAlarm = true;

					// The below could have been done with nested if(!skipXxx), if(!skipYyy) doAlarm=true; 
					// Below is more readable, from a variable context point-of-view, but HARDER to understand
					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipDbnameRegExp)   || ! oldestOpenTranDbname   .matches(skipDbnameRegExp  ))); // NO match in the SKIP dbname   regexp
					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipLoginRegExp)    || ! oldestOpenTranLoginName.matches(skipLoginRegExp   ))); // NO match in the SKIP login    regexp
					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipCmdRegExp)      || ! oldestOpenTranCmd      .matches(skipCmdRegExp     ))); // NO match in the SKIP Cmd      regexp
					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipTranNameRegExp) || ! oldestOpenTranName     .matches(skipTranNameRegExp))); // NO match in the SKIP TranName regexp
					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipWaitTypeRegExp) || ! oldestOpenTranWaitType .matches(skipWaitTypeRegExp))); // NO match in the SKIP TranName regexp

					// NO match in the SKIP regEx
					if (doAlarm)
					{
						// Possibly get more information about that SPID (oldestOpenTranSpid)
						// - print "open" trans info
						// - get info from CmProcesses about the oldest open tran
						String oldestOpenTranSpid_Sessions_htmlTable = "";
						if (oldestOpenTranSpid != -1)
						{
							CountersModel cmSessions = getCounterController().getCmByName(CmSessions.CM_NAME);
							if (cmSessions != null)
							{
								Map<String, Object> whereMap = new HashMap<>();
								whereMap.put("session_id", oldestOpenTranSpid);
								List<Integer> rowIdList = cmSessions.getRateRowIdsWhere(whereMap);
								for (Integer pkRowId : rowIdList)
								{
									oldestOpenTranSpid_Sessions_htmlTable += cmSessions.toHtmlTableString(CountersModel.DATA_RATE, pkRowId, true, false, false);
								}
							}
						}
						
						String extendedDescText = "" 
								+   "OldestOpenTranBeginTime"             + "='" + oldestOpenTranBeginTime             + "'"
								+ ", OldestOpenTranId"                    + "="  + oldestOpenTranId                    + ""
								+ ", OldestOpenTranSpid"                  + "="  + oldestOpenTranSpid                  + ""
								+ ", OldestOpenTranDbname"                + "='" + oldestOpenTranDbname                + "'"
								+ ", OldestOpenTranName"                  + "='" + oldestOpenTranName                  + "'"
								+ ", OldestOpenTranWaitType"              + "='" + oldestOpenTranWaitType              + "'"
								+ ", OldestOpenTranCmd"                   + "='" + oldestOpenTranCmd                   + "'"
								+ ", OldestOpenTranLoginName"             + "='" + oldestOpenTranLoginName             + "'"
								+ ", OldestOpenTranTempdbUsageMbAll"      + "="  + oldestOpenTranTempdbUsageMbAll      + ""
								+ ", OldestOpenTranTempdbUsageMbUser"     + "="  + oldestOpenTranTempdbUsageMbUser     + ""
								+ ", OldestOpenTranTempdbUsageMbInternal" + "="  + oldestOpenTranTempdbUsageMbInternal + ""
								+ ", OldestOpenTranInSec"                 + "="  + oldestOpenTranInSec                 + ""
//								+ ", OldestOpenTranInSecThreshold"        + "="  + oldestOpenTranInSecThreshold        + ""
								;
						String extendedDescHtml = "" // NO-OuterHtml, NO-Borders 
								+ "<table class='dbx-table-basic'> \n"
								+ "    <tr> <td><b>Oldest Open Tran BeginTime            </b></td> <td>" + oldestOpenTranBeginTime              + "</td> </tr> \n"
								+ "    <tr> <td><b>Oldest Open Tran Id                   </b></td> <td>" + oldestOpenTranId                     + "</td> </tr> \n"
								+ "    <tr> <td><b>Oldest Open Tran Spid                 </b></td> <td>" + oldestOpenTranSpid                   + "</td> </tr> \n"
								+ "    <tr> <td><b>Oldest Open Tran Dbname               </b></td> <td>" + oldestOpenTranDbname                 + "</td> </tr> \n"
								+ "    <tr> <td><b>Oldest Open Tran Name                 </b></td> <td>" + oldestOpenTranName                   + "</td> </tr> \n"
								+ "    <tr> <td><b>Oldest Open Tran WaitType             </b></td> <td>" + oldestOpenTranWaitType               + "</td> </tr> \n"
								+ "    <tr> <td><b>Oldest Open Tran Cmd                  </b></td> <td>" + oldestOpenTranCmd                    + "</td> </tr> \n"
								+ "    <tr> <td><b>Oldest Open Tran LoginName            </b></td> <td>" + oldestOpenTranLoginName              + "</td> </tr> \n"
								+ "    <tr> <td><b>Oldest Open Tran TempdbUsageMbAll     </b></td> <td>" + oldestOpenTranTempdbUsageMbAll       + "</td> </tr> \n"
								+ "    <tr> <td><b>Oldest Open Tran TempdbUsageMbUser    </b></td> <td>" + oldestOpenTranTempdbUsageMbUser      + "</td> </tr> \n"
								+ "    <tr> <td><b>Oldest Open Tran TempdbUsageMbInternal</b></td> <td>" + oldestOpenTranTempdbUsageMbInternal  + "</td> </tr> \n"
								+ "    <tr> <td><b>Oldest Open Tran InSec                </b></td> <td>" + oldestOpenTranInSec                  + "</td> </tr> \n"
//								+ "    <tr> <td><b>Oldest Open Tran InSecThreshold       </b></td> <td>" + oldestOpenTranInSecThreshold         + "</td> </tr> \n"
								+ "</table> \n"
								;

						if (StringUtil.hasValue(oldestOpenTranSpid_Sessions_htmlTable))
						{
							extendedDescHtml += ""
									+ "<br>"
									+ "Sessions information for 'Oldest Open Tran Spid: " + oldestOpenTranSpid + "'"
									+ oldestOpenTranSpid_Sessions_htmlTable
									;
						}

						AlarmEvent ae = new AlarmEventLongRunningTransaction(cm, threshold, oldestOpenTranInSec, oldestOpenTranSpid.intValue(), oldestOpenTranDbname, oldestOpenTranName, oldestOpenTranCmd, oldestOpenTranWaitType, oldestOpenTranLoginName, oldestOpenTranTempdbUsageMbAll);
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
						alarmHandler.addAlarm( ae );
					}
				} // end: above threshold
			} // end: oldestOpenTranInSec above threshold
		} // end: oldestOpenTranInSec

		
//		//-------------------------------------------------------
//		// Full transaction log in "any" database
//		//-------------------------------------------------------
//		if (isSystemAlarmsForColumnEnabled("fullTranslogCount"))
//		{
//			Double fullTranslogCount = cm.getAbsValueAsDouble(0, "fullTranslogCount");
//			if (fullTranslogCount != null)
//			{
//				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_fullTranslogCount, DEFAULT_alarm_fullTranslogCount);
//
//				if (debugPrint || _logger.isDebugEnabled())
//					System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", fullTranslogCount='"+fullTranslogCount+"'.");
//
//				if (fullTranslogCount.intValue() > threshold)
//					AlarmHandler.getInstance().addAlarm( new AlarmEventFullTranLog(cm, threshold, fullTranslogCount) );
//			}
//		}

		//-------------------------------------------------------
		// suspectPageCount
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("suspectPageCount"))
		{
			Integer suspectPageCount  = cm.getAbsValueAsInteger(0, "suspectPageCount", false, -1);
			if (suspectPageCount != null && suspectPageCount > 0 )
			{
				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_suspectPageCount, DEFAULT_alarm_suspectPageCount);

				Integer suspectPageErrors = cm.getAbsValueAsInteger(0, "suspectPageErrors", false, -1);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", suspectPageCount="+suspectPageCount+", suspectPageErrors="+suspectPageErrors+".");

				if (suspectPageCount.intValue() > threshold)
				{
//					String extendedDescText = FIXME; get table from suspect_pages Table // cm.toTextTableString(DATA_RATE, r);
//					String extendedDescHtml = FIXME; get table from suspect_pages Table // cm.toHtmlTableString(DATA_RATE, r, true, false, false);
					String extendedDescText = (_lastSuspectPage_rstm == null) ? "" : _lastSuspectPage_rstm.toAsciiTableString();
					String extendedDescHtml = (_lastSuspectPage_rstm == null) ? "" : _lastSuspectPage_rstm.toHtmlTableString("sortable");

					AlarmEvent ae = new AlarmEventSuspectPages(cm, threshold, suspectPageCount, suspectPageErrors);

					ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
					alarmHandler.addAlarm( ae );
				} // end: above threshold
			} // end: suspectPageCount above threshold
		} // end: suspectPageCount

		//-------------------------------------------------------
		// requestsWaitingForWorkers
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("requestsWaitingForWorkers"))
		{
			int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_requestsWaitingForWorkers, DEFAULT_alarm_requestsWaitingForWorkers);

			long requestsWaitingForWorkers  = cm.getAbsValueAsLong(0, "requestsWaitingForWorkers", false, -1L);

			if (debugPrint || _logger.isDebugEnabled())
				System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", requestsWaitingForWorkers="+requestsWaitingForWorkers+".");

			if (requestsWaitingForWorkers > threshold)
			{
				String extendedDescText = "";
				String extendedDescHtml =        getGraphDataHistoryAsHtmlImage(GRAPH_NAME_WORKER_THREAD_USAGE);
				extendedDescHtml += "<br><br>" + getGraphDataHistoryAsHtmlImage(GRAPH_NAME_TASKS_WAITING_FOR_WORKERS);
				extendedDescHtml += "<br><br>" + getGraphDataHistoryAsHtmlImage(GRAPH_NAME_AA_CPU);
				
				CountersModel cmWaitStats = getCounterController().getCmByName(CmWaitStats.CM_NAME);
				if (cmWaitStats != null)
				{
					extendedDescHtml += "<br><br>" + getGraphDataHistoryAsHtmlImage(CmWaitStats.GRAPH_NAME_TOXIC_TIME);
					extendedDescHtml += "<br><br>" + getGraphDataHistoryAsHtmlImage(CmWaitStats.GRAPH_NAME_TOXIC_COUNT);
					extendedDescHtml += "<br><br>" + getGraphDataHistoryAsHtmlImage(CmWaitStats.GRAPH_NAME_TOXIC_TPW);
				}

				AlarmEvent ae = new AlarmEventOutOfWorkerThreads(cm, threshold, requestsWaitingForWorkers);
				ae.setExtendedDescription(extendedDescText, extendedDescHtml);
				
				alarmHandler.addAlarm( ae );
			}
		}

		//-------------------------------------------------------
		// availableWorkers
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("availableWorkers"))
		{
			int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_availableWorkers, DEFAULT_alarm_availableWorkers);

			int maxWorkers        = cm.getAbsValueAsInteger(0, "maxWorkers"      , false, -1);
			int availableWorkers  = cm.getAbsValueAsInteger(0, "availableWorkers", false, -1);

			if (debugPrint || _logger.isDebugEnabled())
				System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", availableWorkers="+availableWorkers+".");

			if (maxWorkers > 0 && availableWorkers <= threshold)
			{
				int allocatedWorkers = cm.getAbsValueAsInteger(0, "allocatedWorkers", false, -1);

				String extendedDescText = "";
				String extendedDescHtml = getGraphDataHistoryAsHtmlImage(GRAPH_NAME_WORKER_THREAD_USAGE);

				AlarmEvent ae = new AlarmEventLowOnWorkerThreads(cm, threshold, availableWorkers, maxWorkers, allocatedWorkers);
				ae.setExtendedDescription(extendedDescText, extendedDescHtml);
				
				alarmHandler.addAlarm( ae );
			}
		}

		//-------------------------------------------------------
		// deadlockCount
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("DeadlockCount"))
		{
			double threshold              = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_alarm_DeadlockCount                    , DEFAULT_alarm_DeadlockCount);
			int    movingAverageInMinutes = Configuration.getCombinedConfiguration().getIntProperty   (PROPKEY_alarm_DeadlockCountMovingAverageMinutes, DEFAULT_alarm_DeadlockCountMovingAverageMinutes);

			int deadlockCount  = cm.getDiffValueAsInteger(0, "deadlockCount", false, -1);

			double movingAvgDeadlockCount = -1;
			if (movingAverageInMinutes > 0)
				movingAvgDeadlockCount = MovingAverageCounterManager.getInstance(CM_NAME, "deadlockCount", movingAverageInMinutes).add(deadlockCount).getAvg(-1, true);

			double dataVal = deadlockCount;
			if (movingAverageInMinutes > 0)
				dataVal = movingAvgDeadlockCount;
			
			if (debugPrint || _logger.isDebugEnabled())
				System.out.println("##### sendAlarmRequest(" + cm.getName() + "): threshold=" + threshold + ", dataVal=" + dataVal + ", deadlockCount=" + deadlockCount + ", movingAvgDeadlockCount=" + movingAvgDeadlockCount + ", movingAverageInMinutes=" + movingAverageInMinutes + ".");

			if (dataVal > threshold)
			{
				String extendedDescText = "";
				String extendedDescHtml = getGraphDataHistoryAsHtmlImage(GRAPH_NAME_DEADLOCK_COUNT_SUM);

				CountersModel cmPerfCounters = getCounterController().getCmByName(CmPerfCounters.CM_NAME);
				if (cmPerfCounters != null)
				{
					extendedDescHtml = "<br><br>" + cmPerfCounters.getGraphDataHistoryAsHtmlImage(CmPerfCounters.GRAPH_NAME_DEADLOCK_DETAILS);
				}
				
				AlarmEvent ae = new AlarmEventDeadlock(cm, threshold, movingAverageInMinutes, deadlockCount, movingAvgDeadlockCount);
				ae.setExtendedDescription(extendedDescText, extendedDescHtml);
				
				alarmHandler.addAlarm( ae );
			}
		}
	}

	public static final String  PROPKEY_alarm_LockWaits                         = CM_NAME + ".alarm.system.if.LockWaits.gt";
	public static final int     DEFAULT_alarm_LockWaits                         = 5;

	public static final String  PROPKEY_alarm_LockWaitsThresholdSec             = CM_NAME + ".alarm.system.if.LockWaits.threshold.seconds";
	public static final int     DEFAULT_alarm_LockWaitsThresholdSec             = 30;

	public static final String  PROPKEY_alarm_DeadlockCount                     = CM_NAME + ".alarm.system.if.deadlockCount.gt";
	public static final double  DEFAULT_alarm_DeadlockCount                     = 0D;

	public static final String  PROPKEY_alarm_DeadlockCountMovingAverageMinutes = CM_NAME + ".alarm.system.if.deadlockCount.movingAverageInMinutes";
	public static final int     DEFAULT_alarm_DeadlockCountMovingAverageMinutes = 10;

	public static final String  PROPKEY_alarm_oldestOpenTranInSec               = CM_NAME + ".alarm.system.if.oldestOpenTranInSec.gt";
	public static final int     DEFAULT_alarm_oldestOpenTranInSec               = 60;

	public static final String  PROPKEY_alarm_oldestOpenTranInSecSkipTranName   = CM_NAME + ".alarm.system.if.oldestOpenTranInSec.skip.tranName";
	public static final String  DEFAULT_alarm_oldestOpenTranInSecSkipTranName   = "";

	public static final String  PROPKEY_alarm_oldestOpenTranInSecSkipDbname     = CM_NAME + ".alarm.system.if.oldestOpenTranInSec.skip.dbname";
	public static final String  DEFAULT_alarm_oldestOpenTranInSecSkipDbname     = "";

	public static final String  PROPKEY_alarm_oldestOpenTranInSecSkipLogin      = CM_NAME + ".alarm.system.if.oldestOpenTranInSec.skip.login";
	public static final String  DEFAULT_alarm_oldestOpenTranInSecSkipLogin      = "";

	public static final String  PROPKEY_alarm_oldestOpenTranInSecSkipCmd        = CM_NAME + ".alarm.system.if.oldestOpenTranInSec.skip.cmd";
	public static final String  DEFAULT_alarm_oldestOpenTranInSecSkipCmd        = "";

	public static final String  PROPKEY_alarm_oldestOpenTranInSecSkipWaitType   = CM_NAME + ".alarm.system.if.oldestOpenTranInSec.skip.waitType";
	public static final String  DEFAULT_alarm_oldestOpenTranInSecSkipWaitType   = "(BROKER_RECEIVE_WAITFOR)";

//	public static final String  PROPKEY_alarm_fullTranslogCount                 = CM_NAME + ".alarm.system.if.fullTranslogCount.gt";
//	public static final int     DEFAULT_alarm_fullTranslogCount                 = 0;

	public static final String  PROPKEY_alarm_suspectPageCount                  = CM_NAME + ".alarm.system.if.suspectPageCount.gt";
	public static final int     DEFAULT_alarm_suspectPageCount                  = 0;

	public static final String  PROPKEY_alarm_requestsWaitingForWorkers         = CM_NAME + ".alarm.system.if.requestsWaitingForWorkers.gt";
	public static final int     DEFAULT_alarm_requestsWaitingForWorkers         = 0;

	public static final String  PROPKEY_alarm_availableWorkers                  = CM_NAME + ".alarm.system.if.availableWorkers.lt";
	public static final int     DEFAULT_alarm_availableWorkers                  = 25;

	
	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("LockWaits",                        isAlarmSwitch, PROPKEY_alarm_LockWaits                        , Integer.class, conf.getIntProperty   (PROPKEY_alarm_LockWaits                        , DEFAULT_alarm_LockWaits                        ), DEFAULT_alarm_LockWaits                        , "If 'LockWaits' (number of spid's that has waited more than the threshold) is greater than ## then send 'AlarmEventBlockingLockAlarm'." ));
		list.add(new CmSettingsHelper("LockWaits TimeThreshold",                         PROPKEY_alarm_LockWaitsThresholdSec            , Integer.class, conf.getIntProperty   (PROPKEY_alarm_LockWaitsThresholdSec            , DEFAULT_alarm_LockWaitsThresholdSec            ), DEFAULT_alarm_LockWaitsThresholdSec            , "Number of seconds before we start to count 'LockWaits', which makes it a threshold input to when 'LockWaits' will fire." ));
		list.add(new CmSettingsHelper("DeadlockCount",                    isAlarmSwitch, PROPKEY_alarm_DeadlockCount                    , Double .class, conf.getDoubleProperty(PROPKEY_alarm_DeadlockCount                    , DEFAULT_alarm_DeadlockCount                    ), DEFAULT_alarm_DeadlockCount                    , "If 'DeadlockCount' (Number of Deadlocks in last sample) is greater than ## then send 'AlarmEventDeadlockAlarm'." ));
		list.add(new CmSettingsHelper("DeadlockCount MovingAverageInMinutes",            PROPKEY_alarm_DeadlockCountMovingAverageMinutes, Integer.class, conf.getIntProperty   (PROPKEY_alarm_DeadlockCountMovingAverageMinutes, DEFAULT_alarm_DeadlockCountMovingAverageMinutes), DEFAULT_alarm_DeadlockCountMovingAverageMinutes, "Number of minutes for a Moving Average, when calculating above 'DeadlockCount'. 0=No-Moving-Average (just use the last sampled counter value" ));
		list.add(new CmSettingsHelper("oldestOpenTranInSec",              isAlarmSwitch, PROPKEY_alarm_oldestOpenTranInSec              , Integer.class, conf.getIntProperty   (PROPKEY_alarm_oldestOpenTranInSec              , DEFAULT_alarm_oldestOpenTranInSec              ), DEFAULT_alarm_oldestOpenTranInSec              , "If 'oldestOpenTranInSec' is greater than ## then send 'AlarmEventLongRunningTransaction'." ));
		list.add(new CmSettingsHelper("oldestOpenTranInSec SkipTranName",                PROPKEY_alarm_oldestOpenTranInSecSkipTranName  , String .class, conf.getProperty      (PROPKEY_alarm_oldestOpenTranInSecSkipTranName  , DEFAULT_alarm_oldestOpenTranInSecSkipTranName  ), DEFAULT_alarm_oldestOpenTranInSecSkipTranName  , "If 'oldestOpenTranInSec' is true; then we can filter out transaction names using a Regular expression... if (tranName.matches('regexp'))... This to remove alarms of 'DUMP DATABASE' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("oldestOpenTranInSec SkipDbname",                  PROPKEY_alarm_oldestOpenTranInSecSkipDbname    , String .class, conf.getProperty      (PROPKEY_alarm_oldestOpenTranInSecSkipDbname    , DEFAULT_alarm_oldestOpenTranInSecSkipDbname    ), DEFAULT_alarm_oldestOpenTranInSecSkipDbname    , "If 'oldestOpenTranInSec' is true; then we can filter out transaction names using a Regular expression... if (dbname.matches('regexp'))...   This to remove alarms of 'DUMP DATABASE' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("oldestOpenTranInSec SkipLogin",                   PROPKEY_alarm_oldestOpenTranInSecSkipLogin     , String .class, conf.getProperty      (PROPKEY_alarm_oldestOpenTranInSecSkipLogin     , DEFAULT_alarm_oldestOpenTranInSecSkipLogin     ), DEFAULT_alarm_oldestOpenTranInSecSkipLogin     , "If 'oldestOpenTranInSec' is true; then we can filter out transaction names using a Regular expression... if (login.matches('regexp'))...    This to remove alarms of 'DUMP DATABASE' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("oldestOpenTranInSec SkipCmd",                     PROPKEY_alarm_oldestOpenTranInSecSkipCmd       , String .class, conf.getProperty      (PROPKEY_alarm_oldestOpenTranInSecSkipCmd       , DEFAULT_alarm_oldestOpenTranInSecSkipCmd       ), DEFAULT_alarm_oldestOpenTranInSecSkipCmd       , "If 'oldestOpenTranInSec' is true; then we can filter out transaction names using a Regular expression... if (cmd.matches('regexp'))...      This to remove alarms of 'DUMP DATABASE' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("oldestOpenTranInSec WaitType",                    PROPKEY_alarm_oldestOpenTranInSecSkipWaitType  , String .class, conf.getProperty      (PROPKEY_alarm_oldestOpenTranInSecSkipWaitType  , DEFAULT_alarm_oldestOpenTranInSecSkipWaitType  ), DEFAULT_alarm_oldestOpenTranInSecSkipWaitType  , "If 'oldestOpenTranInSec' is true; then we can filter out transaction names using a Regular expression... if (waitType.matches('regexp'))... This to remove alarms of 'DUMP DATABASE' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
//		list.add(new CmSettingsHelper("fullTranslogCount",                isAlarmSwitch, PROPKEY_alarm_fullTranslogCount                , Integer.class, conf.getIntProperty   (PROPKEY_alarm_fullTranslogCount                , DEFAULT_alarm_fullTranslogCount                ), DEFAULT_alarm_fullTranslogCount                , "If 'fullTranslogCount' is greater than ## then send 'AlarmEventFullTranLog'." ));
		list.add(new CmSettingsHelper("suspectPageCount",                 isAlarmSwitch, PROPKEY_alarm_suspectPageCount                 , Integer.class, conf.getIntProperty   (PROPKEY_alarm_suspectPageCount                 , DEFAULT_alarm_suspectPageCount                 ), DEFAULT_alarm_suspectPageCount                 , "If 'suspectPageCount' (number of records in 'msdb.dbo.suspect_pages') is greater than ## then send 'AlarmEventSuspectPages'." ));
		list.add(new CmSettingsHelper("requestsWaitingForWorkers",        isAlarmSwitch, PROPKEY_alarm_requestsWaitingForWorkers        , Integer.class, conf.getIntProperty   (PROPKEY_alarm_requestsWaitingForWorkers        , DEFAULT_alarm_requestsWaitingForWorkers        ), DEFAULT_alarm_requestsWaitingForWorkers        , "If 'requestsWaitingForWorkers' is greater than ## then send 'AlarmEventOutOfWorkerThreads'." ));
		list.add(new CmSettingsHelper("availableWorkers",                 isAlarmSwitch, PROPKEY_alarm_availableWorkers                 , Integer.class, conf.getIntProperty   (PROPKEY_alarm_availableWorkers                 , DEFAULT_alarm_availableWorkers                 ), DEFAULT_alarm_availableWorkers                 , "If 'availableWorkers' is LESS than ## then send 'AlarmEventLowOnWorkerThreads'." ));

		return list;
	}
}
