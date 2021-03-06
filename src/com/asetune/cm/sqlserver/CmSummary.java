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
import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JPanel;

import org.apache.log4j.Logger;

import com.asetune.CounterController;
import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.AlarmEventBlockingLockAlarm;
import com.asetune.alarm.events.AlarmEventLongRunningTransaction;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSettingsHelper.RegExpInputValidator;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.sqlserver.gui.CmSummaryPanel;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
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
		"pack_received", "pack_sent", "packet_errors", "total_errors"
	};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
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
	public static final String  PROPKEY_oldestOpenTran_discard_tempdb = "CmSummary.oldestOpenTran.discard.tempdb";
	public static final boolean DEFAULT_oldestOpenTran_discard_tempdb = false;
	
	
	public static final String GRAPH_NAME_AA_CPU                   = "aaCpuGraph";         // String x=GetCounters.CM_GRAPH_NAME__SUMMARY__AA_CPU;
	public static final String GRAPH_NAME_BLOCKING_LOCKS           = "BlockingLocksGraph";
	public static final String GRAPH_NAME_CONNECTION               = "ConnectionsGraph";   // String x=GetCounters.CM_GRAPH_NAME__SUMMARY__CONNECTION;
	public static final String GRAPH_NAME_CONNECTION_RATE          = "ConnRateGraph";
	public static final String GRAPH_NAME_AA_DISK_READ_WRITE       = "aaReadWriteGraph";   // String x=GetCounters.CM_GRAPH_NAME__SUMMARY__AA_DISK_READ_WRITE;
	public static final String GRAPH_NAME_AA_NW_PACKET             = "aaPacketGraph";      // String x=GetCounters.CM_GRAPH_NAME__SUMMARY__AA_NW_PACKET;
	public static final String GRAPH_NAME_OLDEST_TRAN_IN_SEC       = "OldestTranInSecGraph";
	public static final String GRAPH_NAME_MAX_SQL_EXEC_TIME_IN_SEC = "MaxSqlExecTimeInSec";
	
	private void addTrendGraphs()
	{
		// GRAPH
		addTrendGraph(GRAPH_NAME_AA_CPU,
			"CPU Summary, Global Variables", 	                        // Menu CheckBox text
			"CPU Summary for all Engines (using @@cpu_busy, @@cpu_io)", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT,
			new String[] { "System+User CPU (@@cpu_busy + @@cpu_io)", "System CPU (@@cpu_io)", "User CPU (@@cpu_busy)" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.CPU,
			true,  // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_BLOCKING_LOCKS,
			"Blocking Locks", 	                                     // Menu CheckBox text
			"Number of Concurrently Blocking Locks, above 5 sec (from sysprocesses)", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL,
			new String[] { "Blocking Locks" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.LOCK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_CONNECTION,
			"Connections/Users in SQL-Server", 	          // Menu CheckBox text
			"Connections/Users connected to the SQL-Server", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL,
			new String[] { "UserConnections (abs)", "distinctLogins (abs)", "@@connections (diff)", "@@connections (rate)" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.SRV_CONFIG,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_CONNECTION_RATE,
			"Connection Rate in ASE", 	          // Menu CheckBox text
			"Connection Attemtps per Second (source @@connections)", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			new String[] { "@@connections (rate)" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_AA_DISK_READ_WRITE,
			"Disk read/write, Global Variables", 	                         // Menu CheckBox text
			"Disk read/write per second, using @@total_read, @@total_write", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			new String[] { "@@total_read", "@@total_write" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_AA_NW_PACKET,
			"Network Packets received/sent, Global Variables", 	                            // Menu CheckBox text
			"Network Packets received/sent per second, using @@pack_received, @@pack_sent", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			new String[] { "@@pack_received", "@@pack_sent", "@@packet_errors" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.NETWORK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_OLDEST_TRAN_IN_SEC,
			"Oldest Open Transaction in any Databases",     // Menu CheckBox text
			"Oldest Open Transaction in any Databases, in Seconds", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_SECONDS,
			new String[] { "Seconds" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.LOCK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_MAX_SQL_EXEC_TIME_IN_SEC,
			"Max Active SQL Execution Time In Seconds",     // Menu CheckBox text
			"Max Active SQL Execution Time In Seconds", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_SECONDS,
			new String[] { "Max Active SQL Execution Time In Seconds" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
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
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isAzureAnalytics)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isAzureAnalytics)
	{
		List <String> pkCols = new LinkedList<String>();
		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isAzureAnalytics)
	{
		String dm_tran_active_transactions   = "dm_tran_active_transactions";
		String dm_tran_database_transactions = "dm_tran_database_transactions";
		String dm_tran_session_transactions  = "dm_tran_session_transactions";
		
		if (isAzureAnalytics)
		{
			dm_tran_active_transactions   = "dm_pdw_nodes_tran_active_transactions";
			dm_tran_database_transactions = "dm_pdw_nodes_tran_database_transactions";
			dm_tran_session_transactions  = "dm_pdw_nodes_tran_session_transactions";
		}

		// ----- SQL-Server 2014 and above
		String user_objects_deferred_dealloc_page_count = "0";
		if (srvVersion >= Ver.ver(2014))
		{
			user_objects_deferred_dealloc_page_count = "user_objects_deferred_dealloc_page_count";
		}

		String notInTempdb = "";
		if ( Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_oldestOpenTran_discard_tempdb, DEFAULT_oldestOpenTran_discard_tempdb))
		{
			notInTempdb = "and database_id != 2 ";
		}

		int LockWaitsThresholdSec = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_LockWaitsThresholdSec, DEFAULT_alarm_LockWaitsThresholdSec);
		int oldestOpenTranInSec   = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_oldestOpenTranInSec  , DEFAULT_alarm_oldestOpenTranInSec);

		String sql = "" +
				"declare @listeners           varchar(768) \n" +
				"\n" +
				"declare @LockWaits           int \n" +
				"declare @LockWaitThreshold   int \n" +
				"declare @Connections         int \n" +
				"declare @distinctLogins      int \n" +
				"declare @maxSqlExecTimeInSec int \n" +
				"declare @fullTranslogCount   int \n" +
				"\n" +
				"declare @oldestOpenTranBeginTime      datetime \n" +
				"declare @oldestOpenTranId             bigint \n" +
				"declare @oldestOpenTranSpid           int \n" +
				"declare @oldestOpenTranDbname         nvarchar(128) \n" +
				"declare @oldestOpenTranWaitType       nvarchar(60)  \n" +
				"declare @oldestOpenTranCmd            nvarchar(32)  \n" +
				"declare @oldestOpenTranLoginName      nvarchar(128) \n" +
				"declare @oldestOpenTranName           nvarchar(32)  \n" +
				"declare @oldestOpenTranTempdbUsageMb  decimal(12,1) \n" +
				"declare @oldestOpenTranInSec          int = 0\n" +
				"declare @oldestOpenTranInSecThreshold int = " + oldestOpenTranInSec + " \n" +
				"\n" +
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
//				"--  and type_desc = 'TSQL' \n" +
				"order by listener_id \n" +
				"\n" +
				"/* Get info about Open Transactions */\n" +
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
				"        select @oldestOpenTranTempdbUsageMb = CAST( ( \n" + // The below calculations also used in: CmTempdbSpidUsage
				"                                                          (ts.user_objects_alloc_page_count - ts.user_objects_dealloc_page_count - " + user_objects_deferred_dealloc_page_count + ") \n" +  
				"                                                        + (ts.internal_objects_alloc_page_count - ts.internal_objects_dealloc_page_count) \n" +
				"                                                    ) / 128.0 AS decimal(12,1) \n" +
				"                                                  ) \n" +
				"                                              from tempdb.sys.dm_db_session_space_usage ts \n" + 
				"                                              where ts.session_id = @oldestOpenTranSpid \n" +
				"    end \n" +
				"end \n" +
				"\n" +
				"/* And some other metrics */\n" +
				"select @LockWaits           = count(*) FROM sys.sysprocesses WHERE blocked != 0 AND ecid != 0 AND waittime >= " + (LockWaitsThresholdSec * 1000) + " \n" +
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
				"select \n" +
				"  srvVersion                   = @@version  \n" +
				", atAtServerName               = @@servername  \n" +
				", clusterInstanceId            = 'Not Enabled'  \n" +
				", clusterCoordId               = 'Not Enabled'  \n" +
				", timeIsNow                    = getdate()  \n" +
				", utcTimeDiff                  = datediff(mi, getutcdate(), getdate())  \n" +
				", OnHostName                   = convert(varchar(100), SERVERPROPERTY('MachineName')) \n" +
				", NetworkAddressInfo           = @listeners \n" +
				", srvPageSize                  = convert(int, 8196)--@@maxpagesize  \n" +
				", LockWaits                    = @LockWaits                    \n" +
				", LockWaitThreshold            = @LockWaitThreshold            \n" +
				", cpu_busy                     = @@cpu_busy                    \n" +
				", cpu_io                       = @@io_busy                     \n" +
				", cpu_idle                     = @@idle                        \n" +
				", io_total_read                = @@total_read                  \n" +
				", io_total_write               = @@total_write                 \n" +
				", Connections                  = @Connections                  \n" + 
				", aaConnections                = @@connections                 \n" +
				", distinctLogins               = @distinctLogins               \n" +
				", fullTranslogCount            = @fullTranslogCount            \n" +
				", oldestOpenTranBeginTime      = @oldestOpenTranBeginTime      \n" +
				", oldestOpenTranId             = @oldestOpenTranId             \n" +
				", oldestOpenTranSpid           = @oldestOpenTranSpid           \n" +
				", oldestOpenTranDbname         = @oldestOpenTranDbname         \n" +
				", oldestOpenTranName           = @oldestOpenTranName           \n" +
				", oldestOpenTranWaitType       = @oldestOpenTranWaitType       \n" +
				", oldestOpenTranCmd            = @oldestOpenTranCmd            \n" +
				", oldestOpenTranLoginName      = @oldestOpenTranLoginName      \n" +
				", oldestOpenTranTempdbUsageMb  = @oldestOpenTranTempdbUsageMb  \n" +
				", oldestOpenTranInSec          = @oldestOpenTranInSec          \n" +
				", oldestOpenTranInSecThreshold = @oldestOpenTranInSecThreshold \n" +
				", maxSqlExecTimeInSec          = @maxSqlExecTimeInSec          \n" +
				", StartDate                    =               (select sqlserver_start_time from sys.dm_os_sys_info) \n" +
				", DaysRunning                  = datediff(day, (select sqlserver_start_time from sys.dm_os_sys_info), getdate()) \n" +
				", pack_received                = @@pack_received  \n" +
				", pack_sent                    = @@pack_sent      \n" +
				", packet_errors                = @@packet_errors  \n" +
				", total_errors                 = @@total_errors   \n";

		return sql;
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
					alarmHandler.addAlarm( new AlarmEventBlockingLockAlarm(cm, threshold, LockWaits) );
			}
		}


		//-------------------------------------------------------
		// Long running transaction
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("oldestOpenTranInSec"))
		{
			Double oldestOpenTranInSec = cm.getAbsValueAsDouble(0, "oldestOpenTranInSec");
			Double oldestOpenTranSpid  = cm.getAbsValueAsDouble(0, "oldestOpenTranSpid", 0d);
			if (oldestOpenTranInSec != null && oldestOpenTranSpid.intValue() != 0 )
			{
				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_oldestOpenTranInSec, DEFAULT_alarm_oldestOpenTranInSec);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", oldestOpenTranInSec='"+oldestOpenTranInSec+"'.");

				if (oldestOpenTranInSec.intValue() > threshold)
				{
					// Get config 'skip some known values'
					String skipDbnameRegExp   = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_oldestOpenTranInSecSkipDbname,   DEFAULT_alarm_oldestOpenTranInSecSkipDbname);
					String skipLoginRegExp    = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_oldestOpenTranInSecSkipLogin,    DEFAULT_alarm_oldestOpenTranInSecSkipLogin);
					String skipCmdRegExp      = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_oldestOpenTranInSecSkipCmd,      DEFAULT_alarm_oldestOpenTranInSecSkipCmd);
					String skipTranNameRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_oldestOpenTranInSecSkipTranName, DEFAULT_alarm_oldestOpenTranInSecSkipTranName);
					String skipWaitTypeRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_oldestOpenTranInSecSkipWaitType, DEFAULT_alarm_oldestOpenTranInSecSkipWaitType);

					String oldestOpenTranDbname        = cm.getAbsString       (0, "oldestOpenTranDbname");
					String oldestOpenTranLoginName     = cm.getAbsString       (0, "oldestOpenTranLoginName");
					String oldestOpenTranCmd           = cm.getAbsString       (0, "oldestOpenTranCmd");
					String oldestOpenTranName          = cm.getAbsString       (0, "oldestOpenTranName");
					String oldestOpenTranWaitType      = cm.getAbsString       (0, "oldestOpenTranWaitType");
					Double oldestOpenTranTempdbUsageMb = cm.getAbsValueAsDouble(0, "oldestOpenTranTempdbUsageMb");
					
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
						AlarmEvent ae = new AlarmEventLongRunningTransaction(cm, threshold, oldestOpenTranInSec, oldestOpenTranSpid.intValue(), oldestOpenTranDbname, oldestOpenTranName, oldestOpenTranCmd, oldestOpenTranWaitType, oldestOpenTranLoginName, oldestOpenTranTempdbUsageMb);
						
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
	}

	public static final String  PROPKEY_alarm_LockWaits                       = CM_NAME + ".alarm.system.if.LockWaits.gt";
	public static final int     DEFAULT_alarm_LockWaits                       = 5;
	                                                                          
	public static final String  PROPKEY_alarm_LockWaitsThresholdSec           = CM_NAME + ".alarm.system.if.LockWaits.threshold.seconds";
	public static final int     DEFAULT_alarm_LockWaitsThresholdSec           = 30;

	public static final String  PROPKEY_alarm_oldestOpenTranInSec             = CM_NAME + ".alarm.system.if.oldestOpenTranInSec.gt";
	public static final int     DEFAULT_alarm_oldestOpenTranInSec             = 60;
	
	public static final String  PROPKEY_alarm_oldestOpenTranInSecSkipTranName = CM_NAME + ".alarm.system.if.oldestOpenTranInSec.skip.tranName";
	public static final String  DEFAULT_alarm_oldestOpenTranInSecSkipTranName = "";
	
	public static final String  PROPKEY_alarm_oldestOpenTranInSecSkipDbname   = CM_NAME + ".alarm.system.if.oldestOpenTranInSec.skip.dbname";
	public static final String  DEFAULT_alarm_oldestOpenTranInSecSkipDbname   = "";

	public static final String  PROPKEY_alarm_oldestOpenTranInSecSkipLogin    = CM_NAME + ".alarm.system.if.oldestOpenTranInSec.skip.login";
	public static final String  DEFAULT_alarm_oldestOpenTranInSecSkipLogin    = "";

	public static final String  PROPKEY_alarm_oldestOpenTranInSecSkipCmd      = CM_NAME + ".alarm.system.if.oldestOpenTranInSec.skip.cmd";
	public static final String  DEFAULT_alarm_oldestOpenTranInSecSkipCmd      = "";

	public static final String  PROPKEY_alarm_oldestOpenTranInSecSkipWaitType = CM_NAME + ".alarm.system.if.oldestOpenTranInSec.skip.waitType";
	public static final String  DEFAULT_alarm_oldestOpenTranInSecSkipWaitType = "(BROKER_RECEIVE_WAITFOR)";

//	public static final String  PROPKEY_alarm_fullTranslogCount               = CM_NAME + ".alarm.system.if.fullTranslogCount.gt";
//	public static final int     DEFAULT_alarm_fullTranslogCount               = 0;
	
	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("LockWaits",                        isAlarmSwitch, PROPKEY_alarm_LockWaits                       , Integer.class, conf.getIntProperty   (PROPKEY_alarm_LockWaits                       , DEFAULT_alarm_LockWaits                      ), DEFAULT_alarm_LockWaits                      , "If 'LockWaits' (number of spid's that has waited more than the threshold) is greater than ## then send 'AlarmEventBlockingLockAlarm'." ));
		list.add(new CmSettingsHelper("LockWaits TimeThreshold",                         PROPKEY_alarm_LockWaitsThresholdSec           , Integer.class, conf.getIntProperty   (PROPKEY_alarm_LockWaitsThresholdSec           , DEFAULT_alarm_LockWaitsThresholdSec          ), DEFAULT_alarm_LockWaitsThresholdSec          , "Number of seconds before we start to count 'LockWaits', which makes it a threshold input to when 'LockWaits' will fire." ));
		list.add(new CmSettingsHelper("oldestOpenTranInSec",              isAlarmSwitch, PROPKEY_alarm_oldestOpenTranInSec             , Integer.class, conf.getIntProperty   (PROPKEY_alarm_oldestOpenTranInSec             , DEFAULT_alarm_oldestOpenTranInSec            ), DEFAULT_alarm_oldestOpenTranInSec            , "If 'oldestOpenTranInSec' is greater than ## then send 'AlarmEventLongRunningTransaction'." ));
		list.add(new CmSettingsHelper("oldestOpenTranInSec SkipTranName",                PROPKEY_alarm_oldestOpenTranInSecSkipTranName , String .class, conf.getProperty      (PROPKEY_alarm_oldestOpenTranInSecSkipTranName , DEFAULT_alarm_oldestOpenTranInSecSkipTranName), DEFAULT_alarm_oldestOpenTranInSecSkipTranName, "If 'oldestOpenTranInSec' is true; then we can filter out transaction names using a Regular expression... if (tranName.matches('regexp'))... This to remove alarms of 'DUMP DATABASE' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("oldestOpenTranInSec SkipDbname",                  PROPKEY_alarm_oldestOpenTranInSecSkipDbname   , String .class, conf.getProperty      (PROPKEY_alarm_oldestOpenTranInSecSkipDbname   , DEFAULT_alarm_oldestOpenTranInSecSkipDbname  ), DEFAULT_alarm_oldestOpenTranInSecSkipDbname  , "If 'oldestOpenTranInSec' is true; then we can filter out transaction names using a Regular expression... if (dbname.matches('regexp'))...   This to remove alarms of 'DUMP DATABASE' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("oldestOpenTranInSec SkipLogin",                   PROPKEY_alarm_oldestOpenTranInSecSkipLogin    , String .class, conf.getProperty      (PROPKEY_alarm_oldestOpenTranInSecSkipLogin    , DEFAULT_alarm_oldestOpenTranInSecSkipLogin   ), DEFAULT_alarm_oldestOpenTranInSecSkipLogin   , "If 'oldestOpenTranInSec' is true; then we can filter out transaction names using a Regular expression... if (login.matches('regexp'))...    This to remove alarms of 'DUMP DATABASE' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("oldestOpenTranInSec SkipCmd",                     PROPKEY_alarm_oldestOpenTranInSecSkipCmd      , String .class, conf.getProperty      (PROPKEY_alarm_oldestOpenTranInSecSkipCmd      , DEFAULT_alarm_oldestOpenTranInSecSkipCmd     ), DEFAULT_alarm_oldestOpenTranInSecSkipCmd     , "If 'oldestOpenTranInSec' is true; then we can filter out transaction names using a Regular expression... if (cmd.matches('regexp'))...      This to remove alarms of 'DUMP DATABASE' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("oldestOpenTranInSec WaitType",                    PROPKEY_alarm_oldestOpenTranInSecSkipWaitType , String .class, conf.getProperty      (PROPKEY_alarm_oldestOpenTranInSecSkipWaitType , DEFAULT_alarm_oldestOpenTranInSecSkipWaitType), DEFAULT_alarm_oldestOpenTranInSecSkipWaitType, "If 'oldestOpenTranInSec' is true; then we can filter out transaction names using a Regular expression... if (waitType.matches('regexp'))... This to remove alarms of 'DUMP DATABASE' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
//		list.add(new CmSettingsHelper("fullTranslogCount",                isAlarmSwitch, PROPKEY_alarm_fullTranslogCount               , Integer.class, conf.getIntProperty   (PROPKEY_alarm_fullTranslogCount               , DEFAULT_alarm_fullTranslogCount              ), DEFAULT_alarm_fullTranslogCount              , "If 'fullTranslogCount' is greater than ## then send 'AlarmEventFullTranLog'." ));

		return list;
	}
}
