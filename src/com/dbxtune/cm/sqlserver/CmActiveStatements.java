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
package com.dbxtune.cm.sqlserver;

import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.ICounterController.DbmsOption;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventBlockingLockAlarm;
import com.dbxtune.alarm.events.AlarmEventExtensiveUsage;
import com.dbxtune.alarm.events.AlarmEventHoldingLocksWhileWaitForClientInput;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSettingsHelper.RegExpInputValidator;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.sqlserver.gui.CmActiveStatementsPanel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.pcs.PcsColumnOptions;
import com.dbxtune.pcs.PcsColumnOptions.ColumnType;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoSqlServer;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.NumberUtils;
import com.dbxtune.utils.SqlServerUtils;
import com.dbxtune.utils.SqlServerUtils.LockRecord;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmActiveStatements
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmActiveStatements.class.getSimpleName();
	public static final String   SHORT_NAME       = "Active Statements";
	public static final String   HTML_DESC        = 
			"<html>" +
			"<p>Statemenets that are currently executing in the SQL Server.</p>" +
			"<br><br>" +
			"Table Background colors:" +
			"<ul>" +
			"    <li>ORANGE     - SPID was visible in previous sample as well.</li>" +
			"    <li>LIGHT_BLUE - SPID is waiting for a memory grant.</li>" +
			"    <li>PINK       - SPID is Blocked by another SPID from running, this SPID is the Victim of a Blocking Lock, which is showned in RED.</li>" +
			"    <li>RED        - SPID is Blocking other SPID's from running, this SPID is Responslibe or the Root Cause of a Blocking Lock.</li>" +
			"    <li>YELLOW     - SPID is a system process.</li>" +
			"</ul>" +
			// The below just to fill out to 1000 chars so we get "focusable tooltip"
			"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
			"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
			"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
			"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
			"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_exec_sessions", "dm_exec_requests", "dm_exec_connections", "dm_exec_sql_text", "dm_exec_query_plan"};
	public static final String[] NEED_ROLES       = new String[] {"VIEW SERVER STATE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] { "used_memory_pct", "percent_complete" };
	public static final String[] DIFF_COLUMNS     = new String[] {
		"exec_cpu_time",
		"exec_reads",
		"exec_logical_reads",
		"exec_writes",
		"exec_phys_reads_mb",
		"exec_logical_reads_mb",
		"exec_writes_mb",
		"sess_cpu_time",
		"sess_reads",
		"sess_logical_reads",
		"sess_writes"
		};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.ALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmActiveStatements(counterController, guiController);
	}

	public CmActiveStatements(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setShowClearTime(false);

		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX                      = CM_NAME;
	
	public static final String  PROPKEY_sample_systemSpids        = PROP_PREFIX + ".sample.systemSpids";
	public static final boolean DEFAULT_sample_systemSpids        = true;

	public static final String  PROPKEY_sample_showplan           = PROP_PREFIX + ".sample.showplan";
	public static final boolean DEFAULT_sample_showplan           = true;

	public static final String  PROPKEY_sample_monSqlText         = PROP_PREFIX + ".sample.monSqltext";
	public static final boolean DEFAULT_sample_monSqlText         = true;

	public static final String  PROPKEY_sample_liveQueryPlan      = PROP_PREFIX + ".sample.liveQueryPlan";
	public static final boolean DEFAULT_sample_liveQueryPlan      = true;

	public static final String  PROPKEY_sample_holdingLocks       = PROP_PREFIX + ".sample.holdingLocks";
	public static final boolean DEFAULT_sample_holdingLocks       = true;
	
	public static final String  PROPKEY_sample_spidLocks          = PROP_PREFIX + ".sample.spidLocks";
	public static final boolean DEFAULT_sample_spidLocks          = true;

	public static final String  PROPKEY_sample_spidWaits          = PROP_PREFIX + ".sample.spidWaits";
	public static final boolean DEFAULT_sample_spidWaits          = true;

	public static final String  PROPKEY_sample_spidInputBuffer    = PROP_PREFIX + ".sample.spidInputBuffer";
	public static final boolean DEFAULT_sample_spidInputBuffer    = true;

	
	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmActiveStatementsPanel(this);
	}

	@Override
	public void initLocalToolTipTextOnTableColumnHeader()
	{
		setLocalToolTipTextOnTableColumnHeader("monSource",                 "<html>"
		                                                                  + "  <ul> "
		                                                                  + "    <li>ACTIVE  = session_id's that are currently executing some SQL Statement.</li> "
		                                                                  + "    <li>BLOCKER = session_id's that are blocking others from working. <b>Note:</b> sessions that are <i>sleeping/inactive</i> will still displayed</li> "
		                                                                  + "  </ul>"
		                                                                  + "  session_id's can be in both the 'ACTIVE' and 'BLOCKER' section. That is if the session_id is both blocking and currently executing a statement.<br>"
		                                                                  + "  If the session_id is <b>only</b> displayed in the 'BLOCKER' section, it probably means that the client has started a transaction, but currently is at the client side code and doing <i>something</i>, meaning client side logic.<br>"
		                                                                  + "</html>");
		setLocalToolTipTextOnTableColumnHeader("multiSampled",              "<html>The session_id is still executing the <b>same</b> SQL Statement as it did in the previous sample.</html>");
		setLocalToolTipTextOnTableColumnHeader("ImBlockedBySessionId",      "<html>This session_id is blocked by some other session_id</html>");
		setLocalToolTipTextOnTableColumnHeader("ImBlockingOtherSessionIds", "<html>This session_id is <b>blocking</b> other session_id's, This is a list of This session_id is blocked by some other session_id's which this This session_id is blocked by some other session_id is blocking.</html>");
		setLocalToolTipTextOnTableColumnHeader("ImBlockingOthersMaxTimeInSec", "Max Time in Seconds this session_id has been Blocking other session_id's from executing, because this session_id hold lock(s), that some other session_id wants to grab.");
		setLocalToolTipTextOnTableColumnHeader("HasBufferSqlText",          "<html>Checkbox to indicate that 'BufferSqlText' column has a value<br><b>Note:</b> Hower over this cell to see the SQL Statement.</html>");
		setLocalToolTipTextOnTableColumnHeader("LastBufferSqlText",         "<html>The SQL Text sent by the client, fetched from 'sys.dm_exec_input_buffer(session_id, NULL)'</html>");
		setLocalToolTipTextOnTableColumnHeader("HasSqlText",                "<html>Checkbox to indicate that 'lastKnownSql' column has a value<br><b>Note:</b> Hower over this cell to see the SQL Statement.</html>");
		setLocalToolTipTextOnTableColumnHeader("HasQueryplan",              "<html>Checkbox to indicate that 'query_plan' column has a value<br><b>Note:</b> Hower over this cell to see the Query plan.</html>");
		setLocalToolTipTextOnTableColumnHeader("HasLiveQueryplan",          "<html>Checkbox to indicate that 'live_query_plan' column has a value<br><b>Note:</b> Hower over this cell to see the Query plan.</html>");
		setLocalToolTipTextOnTableColumnHeader("ExecTimeInMs",              "<html>How many milliseconds has this session_id been executing the current SQL Statement</html>");
		setLocalToolTipTextOnTableColumnHeader("ExecTimeHms",               "<html>Same as 'ExecTimeInMs' but in a more readable format</html>");
		setLocalToolTipTextOnTableColumnHeader("UsefullExecTime",           "<html>More or less same thing as the column 'ExecTimeInMs' but it subtracts the 'wait_time'...<br>The idea is to display <i>time</i> used on something <i>usefull</i>, e.g. Not in sleep mode.</html>");
		setLocalToolTipTextOnTableColumnHeader("UsefullExecTimeHms",        "<html>Same as 'UsefullExecTime' but in a more readable format</html>");
		setLocalToolTipTextOnTableColumnHeader("lastKnownSql",              "<html>"
		                                                                  + "  Last SQL Statement executed by this session_id.<br>"
		                                                                  + "  If 'monSource' is in status:<br>"
		                                                                  + "  <ul> "
		                                                                  + "    <li>ACTIVE  = SQL Statement that is currently executing.</li> "
		                                                                  + "    <li>BLOCKER = The <b>last</b> SQL Statement that was executed by this session_id. <br>"
		                                                                  + "        <b>Note:</b> This might <b>not</b> be the SQL Statement that caused the blocking...<br>"
		                                                                  + "        Meaning the session_id still holds lock(s) that an earlier issued SQL Statement (in the same transaction) is responsible for.</li> "
		                                                                  + "  </ul>"
		                                                                  + "  session_id's can be in both the 'ACTIVE' and 'BLOCKER' section. That is if the session_id is both blocking and currently executing a statement.<br>"
		                                                                  + "  If the session_id is <b>only</b> displayed in the 'BLOCKER' section, it probably means that the client has started a transaction, but currently is at the client side code and doing <i>something</i>, meaning client side logic is in play.<br>"
		                                                                  + "</html>");
		setLocalToolTipTextOnTableColumnHeader("query_plan",                "<html>Query Plan for the SQL-Statement that is currently executing.<br><b>Note:</b> Only valid for the 'ACTIVE' sessions.</html>");
		setLocalToolTipTextOnTableColumnHeader("LiveQueryPlan",             "<html>Query Plan for the SQL-Statement that is currently executing.<br><b>Note:</b> Only valid for the 'ACTIVE' sessions.</html>");

		setLocalToolTipTextOnTableColumnHeader("HasSpidLocks",               "This SPID holds the following locks in the database");
		setLocalToolTipTextOnTableColumnHeader("SpidLockCount",              "This SPID holds number of locks in the database");
		setLocalToolTipTextOnTableColumnHeader("SpidLocks",                  "This SPID holds the following locks in the database");
		setLocalToolTipTextOnTableColumnHeader("HasBlockedSpidsInfo",        "Has values in column 'BlockedSpidsInfo'");
		setLocalToolTipTextOnTableColumnHeader("BlockedSpidsInfo",           "If this SPID is BLOCKING other spid's, then here is a html-table of showplan for the Blocked spid's. (Note: 'Get Showplan' must be enabled)");

		setLocalToolTipTextOnTableColumnHeader("TempdbUsageMb",              "Number of MB this Statement is using in tempdb (for both 'user objects' and 'internal objects'.");
		setLocalToolTipTextOnTableColumnHeader("TempdbUsageMbUser",          "Number of MB this Statement is using in tempdb (for user objects, probably temp tables).");
		setLocalToolTipTextOnTableColumnHeader("TempdbUsageMbInternal",      "Number of MB this Statement is using in tempdb (for internal objects, probably worker tables, etc).");

		setLocalToolTipTextOnTableColumnHeader("memory_grant_requested",      "If the SPID has requested any memory grant. (has row in sys.dm_exec_query_memory_grants)");
		setLocalToolTipTextOnTableColumnHeader("memory_grant_wait_time_ms",   "Wait time in milliseconds, if we are waiting for other memory grants to be released.");
		setLocalToolTipTextOnTableColumnHeader("requested_memory_kb",         "Total requested amount of memory in kilobytes.");
		setLocalToolTipTextOnTableColumnHeader("granted_memory_kb",           "Total amount of memory actually granted in kilobytes. Can be NULL if the memory is not granted yet. For a typical situation, this value should be the same as 'requested_memory_kb'. For index creation, the server may allow additional on-demand memory beyond initially granted memory");
		setLocalToolTipTextOnTableColumnHeader("used_memory_kb",              "Physical memory used at this moment in kilobytes");
		setLocalToolTipTextOnTableColumnHeader("used_memory_pct",             "Percent calculation of 'granted_memory_kb' and 'used_memory_kb'.");

		
		setLocalToolTipTextOnTableColumnHeader("SpidWaitCountSum",            "Summary of wait counts from CmSpidWait/dm_exec_session_wait_stats  (if enabled).");
		setLocalToolTipTextOnTableColumnHeader("SpidWaitTimeMsSum",           "Summary of wait time in MS from CmSpidWait/dm_exec_session_wait_stats  (if enabled).");
		setLocalToolTipTextOnTableColumnHeader("HasSpidWaitInfo",             "True/false if wait info was available from CmSpidWait/dm_exec_session_wait_stats  (if enabled).");
		setLocalToolTipTextOnTableColumnHeader("SpidWaitInfo",                "A 'table' with all WaitTypes/WaitCount/WaitTimeMs from CmSpidWait/dm_exec_session_wait_stats  (if enabled).");
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public Map<String, PcsColumnOptions> getPcsColumnOptions()
	{
		Map<String, PcsColumnOptions> map = super.getPcsColumnOptions();

		// No settings in the super, create one, and set it at super
		if (map == null)
		{
			map = new HashMap<>();
			map.put("LastBufferSqlText", new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));
			map.put("lastKnownSql"     , new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));
			map.put("query_plan"       , new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));

			// Set the map in the super
			setPcsColumnOptions(map);
		}

		return map;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("session_id");
		pkCols.add("monSource");     // the "spid" can be in both ACTIVE and BLOCKER section

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSqlServer ssVersionInfo = (DbmsVersionInfoSqlServer) versionInfo;
		long srvVersion = ssVersionInfo.getLongVersion();

		boolean showHoldingLocks = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sample_holdingLocks, DEFAULT_sample_holdingLocks);

		String dm_exec_sessions            = "dm_exec_sessions";
		String dm_exec_requests            = "dm_exec_requests";
//		String dm_exec_connections         = "dm_exec_connections";
		String dm_exec_sql_text            = "dm_exec_sql_text";
		String dm_exec_query_plan          = "dm_exec_query_plan";
		String dm_exec_query_memory_grants = "dm_exec_query_memory_grants";

		// get Actual-Query-Plan instead of Estimated-QueryPlan
		if (isDbmsOptionEnabled(DbmsOption.SQL_SERVER__LAST_QUERY_PLAN_STATS))
		{
			dm_exec_query_plan = "dm_exec_query_plan_stats";
		}

		if (ssVersionInfo.isAzureSynapseAnalytics())
		{
			dm_exec_sessions            = "dm_pdw_nodes_exec_sessions";
			dm_exec_requests            = "dm_exec_requests";            // SAME NAME IN AZURE ????
//			dm_exec_connections         = "dm_pdw_exec_connections";
			dm_exec_sql_text            = "dm_exec_sql_text";            // SAME NAME IN AZURE ????
			dm_exec_query_plan          = "dm_exec_query_plan";            // SAME NAME IN AZURE ????
			dm_exec_query_memory_grants = "dm_exec_query_memory_grants"; // SAME NAME IN AZURE ????
		}

//		String LiveQueryPlanActive  = "";
//		String LiveQueryPlanBlocked = "";

//		String LiveQueryPlanActive  = "    ,LiveQueryPlan = convert(nvarchar(max), 'No live query plan available. Only in 2016 SP1 and above')";
//		String LiveQueryPlanBlocked = "    ,LiveQueryPlan = convert(nvarchar(max), 'No live query plan available. Only in 2016 SP1 and above')";
		String LiveQueryPlanActive  = "    ,LiveQueryPlan = convert(nvarchar(max), null) \n";
		String LiveQueryPlanBlocked = "    ,LiveQueryPlan = convert(nvarchar(max), null) \n";


		String HasBufferSqlText_1  = "";
		String LastBufferSqlText_1 = "";
		String HasBufferSqlText_2  = "";
		String LastBufferSqlText_2 = "";
		if (srvVersion >= Ver.ver(2014))
		{
			boolean getSpidInputBuffer = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sample_spidInputBuffer, DEFAULT_sample_spidInputBuffer);

			// First SQL Statement (added to variable: sql1)
			HasBufferSqlText_1  = "    ,HasBufferSqlText    = convert(bit,0) \n";
			LastBufferSqlText_1 = "    ,LastBufferSqlText = (select 'type=''' + eib.event_type + ''', params=' + cast(eib.parameters as nvarchar(10)) + ': ' + char(10) + eib.event_info from sys.dm_exec_input_buffer(der.session_id, der.request_id) eib) \n"; 

			// Second SQL Statement (added to variable: sql2_cols)
			HasBufferSqlText_2  = HasBufferSqlText_1;
			LastBufferSqlText_2 = LastBufferSqlText_1.replace("dm_exec_input_buffer(der.session_id, der.request_id)", "dm_exec_input_buffer(p1.spid, NULL)");

			// if NOT enabled... keep the columns, but make them "empty"
			if ( ! getSpidInputBuffer )
			{
				LastBufferSqlText_1 = "    ,LastBufferSqlText = convert(nvarchar(max), null) /* enable 'dm_exec_input_buffer(...)' with property: " + PROPKEY_sample_spidInputBuffer + " = true */ \n";
				LastBufferSqlText_2 = LastBufferSqlText_1;
			}
		}

//		
//		if (srvVersion >= Ver.ver(2016,0,0, 1)) // 2016 SP1 (or check if the table exists; select 1 from sys.all_tables where name = 'dm_exec_query_statistics_xml'
//		{
//			LiveQueryPlanActive  = "    ,LiveQueryPlan = (select lqp.query_plan from sys.dm_exec_query_statistics_xml(des.session_id) lqp) \n";
//			LiveQueryPlanBlocked = "    ,LiveQueryPlan = (select lqp.query_plan from sys.dm_exec_query_statistics_xml(p1.spid) lqp) \n";
//		}
//      NOTE: The above seems to "stack-trace" the SQL-Server... maybe "defer" it into a post step, like we do in AseTune
// possibly use: dm_exec_query_profiles 
// possibly use: dm_exec_query_plan_stats instead... at least it will give the ACTUAL plan... https://blog.matesic.info/post/Last-Actual-Plan-with-sysdm_exec_query_plan_stats

		String dop_active = "";
		String dop_locks  = "";
		if (srvVersion >= Ver.ver(2016))
		{
			dop_active = "    ,der.dop \n";
			dop_locks  = "    ,-1 as dop \n";
		}
		
		// Is 'context_info_str' enabled (if it causes any problem, it can be disabled)
		String contextInfoStr1 = "/*    ,context_info_str    = replace(cast(der.context_info as varchar(128)),char(0),'') -- " + SqlServerCmUtils.HELPTEXT_howToEnable__context_info_str + " */ \n";
		String contextInfoStr2 = "/*    ,context_info_str    = replace(cast(p1.context_info as varchar(128)),char(0),'') -- " + SqlServerCmUtils.HELPTEXT_howToEnable__context_info_str + " */ \n";
		if (SqlServerCmUtils.isContextInfoStrEnabled())
		{
			// Make the binary 'context_info' into a String
			contextInfoStr1 = "    ,context_info_str    = replace(cast(der.context_info as varchar(128)),char(0),'') /* " + SqlServerCmUtils.HELPTEXT_howToDisable__context_info_str + " */ \n";
			contextInfoStr2 = "    ,context_info_str    = replace(cast(p1.context_info as varchar(128)),char(0),'') /* " + SqlServerCmUtils.HELPTEXT_howToDisable__context_info_str + " */ \n";
		}

//		String user_objects_deferred_dealloc_page_count = "0";
//		if (srvVersion >= Ver.ver(2014))
//		{
//			user_objects_deferred_dealloc_page_count = "user_objects_deferred_dealloc_page_count";			
//		}
		
		String whereIsUserSpid  = "  AND des.is_user_process = 1 \n";
		if (Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sample_systemSpids, DEFAULT_sample_systemSpids))
		{
			whereIsUserSpid = "  AND (des.is_user_process = 1 OR (des.is_user_process = 0 AND des.status != 'sleeping')) \n ";
		}

// PREPARATIONS for better handling of "transaction info"
		String sql_preDrop = ""
			    + "-- Drop any of the old temporary tables if they still exists \n" 
			    + "if (object_id('tempdb..#tran_info') is not null) drop table #tran_info \n" 
			    + "go \n"
			    + " \n"
			    + "";
		
		String sql_postDrop = ""
			    + "\n"
			    + "\n"
			    + "-- Drop temporary tables \n" 
			    + "drop table #tran_info \n" 
			    + "";
		
		String sql_tranInfo_populate = ""
			    + "-- Populate some temptables \n" 
			    + "SELECT  /* ${cmCollectorName} */ \n"
			    + "     st.session_id \n"
			    + "    ,tran_id         = MAX(st.transaction_id) \n"
			    + "    ,tran_begin_time = MIN(tat.transaction_begin_time) \n"
			    + "    ,tran_name       = MAX(tat.name) \n"
			    + "    ,COALESCE(SUM(dbt.database_transaction_log_record_count  ), 0)        as wal_record_count \n"
			    + "    ,COALESCE(SUM(dbt.database_transaction_log_bytes_used    ), 0) / 1024 as wal_kb_used \n"
			    + "INTO #tran_info \n"
			    + "FROM sys.dm_tran_session_transactions st \n"
			    + "INNER JOIN sys.dm_tran_database_transactions dbt ON st.transaction_id = dbt.transaction_id \n"
			    + "INNER JOIN sys.dm_tran_active_transactions   tat ON st.transaction_id = tat.transaction_id \n"
			    + "GROUP BY st.session_id \n"
			    + "\n"
			    + "\n";

		String sql_tranInfo_join_1                = "LEFT OUTER JOIN #tran_info ON des.session_id = #tran_info.session_id \n";
		String sql_tranInfo_join_2                = "LEFT OUTER JOIN #tran_info ON p1.spid = #tran_info.session_id \n";
		String sql_tranInfo_join_3                = sql_tranInfo_join_2;

		String sql_tranInfo_col__tran_id          = "    ,tran_id                   = #tran_info.tran_id \n";
		String sql_tranInfo_col__tran_begin_time  = "    ,tran_begin_time           = #tran_info.tran_begin_time \n";
		String sql_tranInfo_col__tran_name        = "    ,tran_name                 = #tran_info.tran_name \n";
		String sql_tranInfo_col__wal_kb_used      = "    ,wal_record_count          = #tran_info.wal_record_count \n";
		String sql_tranInfo_col__wal_record_count = "    ,wal_kb_used               = #tran_info.wal_kb_used \n";



		String sql1 =
			"SELECT /* ${cmCollectorName} */  \n" +
			"     monSource    = convert(varchar(20), 'ACTIVE') \n" +
			"    ,multiSampled = convert(varchar(10), '') \n" +
			"    ,des.login_name \n" +
			"    ,des.session_id \n" +
			dop_active +
			"    ,ImBlockedBySessionId = der.blocking_session_id \n" +
			"    ,ImBlockingOtherSessionIds    = convert(varchar(512), '') \n" +
			"    ,ImBlockingOthersMaxTimeInSec = convert(int, 0) \n" +
			"    ,des.status \n" +
			"    ,der.command \n" +
//			"    ,tran_id                   = der.transaction_id \n" +
//			"    ,tran_begin_time           = (SELECT TOP 1 tat.transaction_begin_time FROM sys.dm_tran_active_transactions tat WHERE tat.transaction_id = der.transaction_id) \n" +
//			"    ,tran_name                 = (SELECT TOP 1 tat.name \n" +
//			"                                  FROM       sys.dm_tran_session_transactions tst \n" +
//	        "                                  INNER JOIN sys.dm_tran_active_transactions  tat ON tst.transaction_id = tat.transaction_id \n" + 
//	        "                                  WHERE tst.session_id = des.session_id) \n" +
			sql_tranInfo_col__tran_id + 
			sql_tranInfo_col__tran_begin_time + 
			sql_tranInfo_col__tran_name + 
			sql_tranInfo_col__wal_kb_used + 
			sql_tranInfo_col__wal_record_count + 
			"    ,ProcName                  = isnull(object_schema_name(dest.objectid, dest.dbid),'') + '.' + object_name(dest.objectid, dest.dbid) \n" +
			"    ,StmntStart                = der.statement_start_offset \n" +
			"    ,des.[HOST_NAME] \n" +
//			"    ,TempdbUsageMb       = (select \n" + 
//			"                            CAST( ( \n" + // The below calculations also used in: CmTempdbSpidUsage
//			"                                        (user_objects_alloc_page_count - user_objects_dealloc_page_count - " + user_objects_deferred_dealloc_page_count + ") \n" + 
//			"                                      + (internal_objects_alloc_page_count - internal_objects_dealloc_page_count) \n" + 
//			"                                  ) / 128.0 AS decimal(12,1) \n" + 
//			"                                ) \n" + 
//			"                            from tempdb.sys.dm_db_session_space_usage ts \n" + 
//			"                            where ts.session_id = des.session_id \n" +
//			"                           ) \n" +
			"    ,TempdbUsageMb             = convert(numeric(12,1), NULL)  \n" +
			"    ,TempdbUsageMbUser         = convert(numeric(12,1), NULL)  \n" +
			"    ,TempdbUsageMbInternal     = convert(numeric(12,1), NULL)  \n" +
			HasBufferSqlText_1 +
			"    ,HasSqlText                = convert(bit,0) \n" +
			"    ,HasQueryplan              = convert(bit,0) \n" +
			"    ,HasLiveQueryplan          = convert(bit,0) \n" +
			"    ,HasSpidLocks              = convert(bit,0) \n" +
			"    ,HasBlockedSpidsInfo       = convert(bit,0) \n" +
			"    ,SpidLockCount             = convert(int,-1) \n" +
			"    ,HasSpidWaitInfo           = convert(bit   , 0) \n"  +             // filled in with 'localCalculation(...)' with values from CmSpidWait
			"    ,DB_NAME(der.database_id) AS database_name \n" +
//			"    ,(select db.name from sys.databases db where db.database_id = der.database_id) AS database_name \n" +
			"    ,SpidWaitCountSum          = convert(bigint, -1) \n" +             // filled in with 'localCalculation(...)' with values from CmSpidWait
			"    ,SpidWaitTimeMsSum         = convert(bigint, -1) \n" +             // filled in with 'localCalculation(...)' with values from CmSpidWait 
			"    ,exec_cpu_time             = der.cpu_time \n" +
			"    ,exec_reads                = der.reads \n" +
			"    ,exec_logical_reads        = der.logical_reads \n" +
			"    ,exec_writes               = der.writes \n" +
			"    ,exec_phys_reads_mb        = CAST(der.reads         / 128.0 AS numeric(10,1)) \n" +
			"    ,exec_logical_reads_mb     = CAST(der.logical_reads / 128.0 AS numeric(10,1)) \n" +
			"    ,exec_writes_mb            = CAST(der.writes        / 128.0 AS numeric(10,1)) \n" +
			"    ,sess_cpu_time             = des.cpu_time \n" +
			"    ,sess_reads                = des.reads \n" +
			"    ,sess_logical_reads        = des.logical_reads \n" +
			"    ,sess_writes               = des.writes \n" +
			"    ,memory_grant_requested    = CASE WHEN dem.session_id IS NULL THEN convert(bit,0) ELSE convert(bit,1) END \n" +
			"    ,memory_grant_wait_time_ms = dem.wait_time_ms \n" +
			"    ,requested_memory_kb       = dem.requested_memory_kb \n" +
			"    ,granted_memory_kb         = dem.granted_memory_kb \n" +
			"    ,used_memory_kb            = dem.used_memory_kb \n" +
			"    ,used_memory_pct           = convert(numeric(9,1), (dem.used_memory_kb*1.0) / (dem.granted_memory_kb*1.0) * 100.0) \n" +
//			"    ,dec.last_write \n" +
			"    ,der.start_time \n" +
			"    ,ExecTimeInMs              = CASE WHEN datediff(day, der.start_time, getdate()) >= 24 THEN -1 ELSE  datediff(ms, der.start_time, getdate()) END \n" +               // protect from: Msg 535: Difference of two datetime fields caused overflow at runtime. above 24 days or so, the MS difference is overflowned
			"    ,ExecTimeHms               = CAST('' as varchar(20))\n" + // filled in with 'localCalculation(...)'
			"    ,UsefullExecTime           = CASE WHEN datediff(day, der.start_time, getdate()) >= 24 THEN -1 ELSE (datediff(ms, der.start_time, getdate()) - der.wait_time) END \n" + // protect from: Msg 535: Difference of two datetime fields caused overflow at runtime. above 24 days or so, the MS difference is overflowned
			"    ,UsefullExecTimeHms        = CAST('' as varchar(20))\n" + // filled in with 'localCalculation(...)'
			"    ,des.[program_name] \n" +
			     contextInfoStr1 +
			"    ,der.wait_type \n" +
			"    ,der.wait_time \n" +
			"    ,der.last_wait_type \n" +
			"    ,der.wait_resource \n" +
			"    ,CASE des.transaction_isolation_level \n" +
			"        WHEN 0 THEN 'Unspecified[0]' \n" +
			"        WHEN 1 THEN 'ReadUnCommitted[1]' \n" +
			"        WHEN 2 THEN 'ReadCommitted[2]' \n" +
			"        WHEN 3 THEN 'Repeatable[3]' \n" +
			"        WHEN 4 THEN 'Serializable[4]' \n" +
			"        WHEN 5 THEN 'Snapshot[5]' \n" +
			"        ELSE        'Unknown' + convert(varchar(10), des.transaction_isolation_level) + ']' \n" +
			"    END AS transaction_isolation_level \n" +
			"    ,des.is_user_process \n" +
			"    ,der.percent_complete \n" +
			"    ,der.estimated_completion_time \n" +
			"    ,estimated_compl_time_dhms = cast('' as varchar(20)) \n" +
//			"	 ,estimated_finish_time = CASE WHEN estimated_completion_time > 0 \n" + 
//			"                                  THEN cast(cast(dateadd(ms,der.estimated_completion_time, getdate()) as time) as varchar(8)) \n" +
//			"	                               ELSE NULL \n" +
//			"	                          END \n" +
			"    ,estimated_finish_time = CASE WHEN estimated_completion_time > 0 \n" +
			"                                  THEN iif( datediff(day, getdate(), dateadd(ms,der.estimated_completion_time, getdate())) = 0 \n" +       // if within SINGLE Day
			"                                           ,convert(varchar(8),  cast(dateadd(ms,der.estimated_completion_time, getdate()) as time)) \n" + // Short: hh:mm:ss
			"                                           ,convert(varchar(19), dateadd(ms,der.estimated_completion_time, getdate()), 121) \n" +          // Long:  YYYY-MM-DD hh:mm:ss
			"                                          ) \n" +
			"                                  ELSE NULL \n" +
			"                             END \n" +
//			"    ,OBJECT_NAME(dest.objectid, der.database_id) AS OBJECT_NAME \n" +
			"    ,der.sql_handle \n" +
			LastBufferSqlText_1 +
			"    ,SUBSTRING(dest.text, der.statement_start_offset / 2,  \n" +
			"        ( CASE WHEN der.statement_end_offset = -1  \n" +
			"               THEN DATALENGTH(dest.text)  \n" +
			"               ELSE der.statement_end_offset  \n" +
			"          END - der.statement_start_offset ) / 2 +2) AS [lastKnownSql] \n" +
			"    ,SpidLocks        = convert(varchar(max),null) \n" +
			"    ,BlockedSpidsInfo = convert(varchar(max),null) \n" +
			"    ,SpidWaitInfo     = convert(varchar(max),null) \n" +    // filled in with 'localCalculation(...)' with values from CmSpidWait
			"    ,deqp.query_plan \n" +
			LiveQueryPlanActive +
			"FROM sys." + dm_exec_sessions + " des \n" +
			"JOIN sys." + dm_exec_requests + " der ON des.session_id = der.session_id \n" +
			"LEFT OUTER JOIN sys." + dm_exec_query_memory_grants + " dem ON des.session_id = dem.session_id \n" +
//			"LEFT OUTER JOIN sys." + dm_exec_connections + " dec ON des.session_id = dec.session_id \n" +
			sql_tranInfo_join_1 +
			"OUTER APPLY sys." + dm_exec_sql_text + "(der.sql_handle) dest \n" +
			"OUTER APPLY sys." + dm_exec_query_plan + "(der.plan_handle) deqp \n" +
			"WHERE des.session_id != @@spid \n " +
			whereIsUserSpid +
			"  AND (der.executing_managed_code = 0 OR (der.executing_managed_code = 1 AND der.wait_type != 'SLEEP_TASK')) \n" + // SSIS seems to be executing ALL THE TIME... so discard some of them... (this may be unique to MaxM)
			"";

		//-----------------------------------------------------------------------
		// SPID's that are BLOCKING
		//
		String sql2_cols = 
//			"SELECT DISTINCT /* ${cmCollectorName} */  \n" +
			"     monSource    = convert(varchar(20), 'BLOCKER')  \n" +
			"    ,multiSampled = convert(varchar(10), '')  \n" +
			"    ,p1.loginame                                  --des.login_name \n" +
			"    ,p1.spid                                      --des.session_id \n" +
			dop_locks +
			"    ,ImBlockedBySessionId = p1.blocked            --der.blocking_session_id \n" +
			"    ,ImBlockingOtherSessionIds    = convert(varchar(512), '')  \n" +
			"    ,ImBlockingOthersMaxTimeInSec = convert(int, 0) " +
			"    ,p1.status                                    --des.status \n" +
			"    ,p1.cmd                                       --der.command \n" +
//			"    ,tran_id         = (SELECT TOP 1 tst.transaction_id \n" +
//			"                        FROM sys.dm_tran_session_transactions tst \n" +
//			"                        WHERE tst.session_id = p1.spid) \n" +
//			"    ,tran_begin_time = (SELECT TOP 1 tat.transaction_begin_time \n" +
//			"                        FROM       sys.dm_tran_session_transactions tst \n" +
//	        "                        INNER join sys.dm_tran_active_transactions  tat on tst.transaction_id = tat.transaction_id \n" + 
//	        "                        WHERE tst.session_id = p1.spid) \n" +
//			"    ,tran_name       = (SELECT TOP 1 tat.name \n" +
//			"                        FROM       sys.dm_tran_session_transactions tst \n" +
//	        "                        INNER join sys.dm_tran_active_transactions  tat on tst.transaction_id = tat.transaction_id \n" + 
//	        "                        WHERE tst.session_id = p1.spid) \n" +
			sql_tranInfo_col__tran_id + 
			sql_tranInfo_col__tran_begin_time + 
			sql_tranInfo_col__tran_name + 
			sql_tranInfo_col__wal_kb_used + 
			sql_tranInfo_col__wal_record_count + 
			"    ,ProcName            = isnull(object_schema_name(dest.objectid, dest.dbid),'') + '.' + object_name(dest.objectid, dest.dbid) \n" +
			"    ,StmntStart          = -1 \n" +
			"    ,p1.hostname                                  --des.[HOST_NAME] \n" +
			"    ,TempdbUsageMb          = convert(numeric(12,1), NULL)  \n" +
			"    ,TempdbUsageMbUser      = convert(numeric(12,1), NULL)  \n" +
			"    ,TempdbUsageMbInternal  = convert(numeric(12,1), NULL)  \n" +
			HasBufferSqlText_2 +
			"    ,HasSqlText          = convert(bit,0)  \n" +
			"    ,HasQueryplan        = convert(bit,0)  \n" +
			"    ,HasLiveQueryplan    = convert(bit,0) \n" +
			"    ,HasSpidLocks        = convert(bit,0) \n" +
			"    ,HasBlockedSpidsInfo = convert(bit,0) \n" +
			"    ,SpidLockCount       = convert(int,-1) \n" +
			"    ,HasSpidWaitInfo     = convert(bit   , 0) \n"  +             // filled in with 'localCalculation(...)' with values from CmSpidWait
			"    ,DB_NAME(p1.dbid) AS database_name  \n" +
//			"    ,(SELECT db.name FROM sys.databases db WHERE db.database_id = p1.dbid) AS database_name \n" +
			"    ,SpidWaitCountSum    = convert(bigint, -1) \n" +             // filled in with 'localCalculation(...)' with values from CmSpidWait
			"    ,SpidWaitTimeMsSum   = convert(bigint, -1) \n" +             // filled in with 'localCalculation(...)' with values from CmSpidWait 
			"    ,p1.cpu                                       --der.cpu_time \n" +
			"    ,0                                            --der.reads   \n" +
			"    ,0                                            --der.logical_reads   \n" +
			"    ,0                                            --der.writes   \n" +
			"    ,CAST(0 AS numeric(10,1))                     --exec_phys_reads_mb    \n" +
			"    ,CAST(0 AS numeric(10,1))                     --exec_logical_reads_mb \n" +
			"    ,CAST(0 AS numeric(10,1))                     --exec_writes_mb        \n" +
			"    ,0                                            --des.cpu_time \n" +
			"    ,0                                            --des.reads   \n" +
			"    ,0                                            --des.logical_reads   \n" +
			"    ,0                                            --des.writes   \n" +
			"    ,memory_grant_requested    = convert(bit,0) \n" +
			"    ,memory_grant_wait_time_ms = -1 \n" +
			"    ,requested_memory_kb       = -1 \n" +
			"    ,granted_memory_kb         = -1 \n" +
			"    ,used_memory_kb            = -1 \n" +
			"    ,used_memory_pct           = convert(numeric(9,1), 0) \n" +
			"    ,p1.last_batch                                --der.start_time  \n" +
			"    ,ExecTimeInMs       = CASE WHEN datediff(day, p1.last_batch, getdate()) >= 24 THEN -1 ELSE  datediff(ms, p1.last_batch, getdate()) END  \n" +
			"    ,ExecTimeHms        = CAST('' as varchar(20))\n" + // filled in with 'localCalculation(...)'
			"    ,UsefullExecTime    = CASE WHEN datediff(day, p1.last_batch, getdate()) >= 24 THEN -1 ELSE (datediff(ms, p1.last_batch, getdate()) - p1.waittime) END  \n" +
			"    ,UsefullExecTimeHms = CAST('' as varchar(20))\n" + // filled in with 'localCalculation(...)'
			"    ,p1.program_name                              --des.[program_name] \n" +
			      contextInfoStr2 +
			"    ,p1.lastwaittype                              --der.wait_type \n" +  // p1.waittype -- is 'binary(2)' -- Description='Reserved.' ... so 'lastwaittype' seems better
			"    ,p1.waittime                                  --der.wait_time \n" +
			"    ,p1.lastwaittype                              --der.last_wait_type \n" +
			"    ,p1.waitresource                              --der.wait_resource \n" +
			"    ,'unknown' \n" +
//			"--    ,CASE des.transaction_isolation_level \n" +
//			"--        WHEN 0 THEN 'Unspecified[0]' \n" +
//			"--        WHEN 1 THEN 'ReadUnCommitted[1]' \n" +
//			"--        WHEN 2 THEN 'ReadCommitted[2]' \n" +
//			"--        WHEN 3 THEN 'Repeatable[3]' \n" +
//			"--        WHEN 4 THEN 'Serializable[4]' \n" +
//			"--        WHEN 5 THEN 'Snapshot[5]' \n" +
//			"--        ELSE        'Unknown' + convert(varchar(10), des.transaction_isolation_level) + ']' \n" +
//			"--    END AS transaction_isolation_level \n" +
//			"    ,'unknown', --OBJECT_NAME(dest.objectid, der.database_id) AS OBJECT_NAME \n" +
//			"--  , SUBSTRING(dest.text, der.statement_start_offset / 2,   \n" +
//			"--        ( CASE WHEN der.statement_end_offset = -1   \n" +
//			"--               THEN DATALENGTH(dest.text)   \n" +
//			"--               ELSE der.statement_end_offset   \n" +
//			"--          END - der.statement_start_offset ) / 2 +2) AS [lastKnownSql] \n" +
			"    ,is_user_process           = CASE WHEN p1.sid != 0x01 THEN convert(bit, 1) ELSE convert(bit, 0) END \n" +
			"    ,percent_complete          = -1 \n" +
			"    ,estimated_completion_time = -1 \n" +
			"    ,estimated_compl_time_dhms = NULL \n" +
			"    ,estimated_finish_time     = NULL \n" + 
			"    ,p1.sql_handle \n" +
			LastBufferSqlText_2 +
			"    ,dest.text \n" +
			"    ,SpidLocks        = convert(varchar(max),null) \n" +
			"    ,BlockedSpidsInfo = convert(varchar(max),null) \n" +
			"    ,SpidWaitInfo     = convert(varchar(max),null) \n" +    // filled in with 'localCalculation(...)' with values from CmSpidWait
			"    ,''                                           --deqp.query_plan  \n" +
			LiveQueryPlanBlocked +
			"";

//		String sql2_tabs = 
//			"FROM sys.sysprocesses p1 \n" +
//			"OUTER APPLY sys." + dm_exec_sql_text + "(p1.sql_handle) dest  \n" +
//			"";
//
//		String sql2_where = 
//			"WHERE p1.spid in (SELECT p2.blocked FROM sys.sysprocesses p2 WHERE p2.blocked > 0) \n" + // This sub-select did NOT materialize before executing, so it was slow... Rewrite to join was faster
//			"  AND p1.ecid = 0 \n" + // Only Parent SPID's in parallel statements ... or we can add/introduce the ECID in the primary key... 
//			"";

		//-----------------------------------------------------------------------
		// do we REALLY NEED section 2 "BLOCKER" in SQL Server...
		// DEFAULT: do NOT sample... if we need it it can be enabled with config: CmActiveStatements.sample.BLOCKER
		//
		String sql2 = ""; 
		if (Configuration.getCombinedConfiguration().getBooleanProperty("CmActiveStatements.sample.BLOCKER", false))
		{
			sql2 = 
				"\n" +
				"UNION ALL \n" +
				"\n" +
					
				"SELECT DISTINCT /* ${cmCollectorName} */  \n" +
				sql2_cols +
				"FROM sys.sysprocesses p1 \n" +
				"JOIN sys.sysprocesses p2 ON p1.spid = p2.spid AND p2.blocked > 0 \n" +
				sql_tranInfo_join_2 +
				"OUTER APPLY sys." + dm_exec_sql_text + "(p1.sql_handle) dest  \n" +
				"WHERE 1 = 1 \n" + 
				"  AND p1.ecid = 0 \n" + // Only Parent SPID's in parallel statements ... or we can add/introduce the ECID in the primary key... 
				"";
		}
		else
		{
			sql2 = 
					"\n" +
					"\n" +
					"/*----------------------------------------------------------------------------------------------------------------------------------*/ \n" +
					"/* NOTE: The 'BLOCKER' Section is disabled, but can be enabled using Configuration Property: CmActiveStatements.sample.BLOCKER=true */ \n" +
					"/*----------------------------------------------------------------------------------------------------------------------------------*/ \n" +
					"\n" +
					"";
		}

		

		//-----------------------------------------------------------------------
		// get info about SPID's that are in transaction (or holding locks) but are at Client (EventID = 250 for Sybase)
		//
		String sql3 = "";
		if (showHoldingLocks)
		{
			String cols = sql2_cols;
			cols = cols.replace("'BLOCKER'", "'HOLDING-LOCKS'");

			sql3 = 	
				"\n" +
				"UNION ALL \n" +
				"\n" +

				"SELECT /* ${cmCollectorName} */  \n" +
				cols +
				"FROM sys.sysprocesses p1 \n" +
				sql_tranInfo_join_3 +
				"OUTER APPLY sys." + dm_exec_sql_text + "(p1.sql_handle) dest  \n" +
				"WHERE p1.open_tran > 0 \n" + 
				"  AND p1.status    = 'sleeping' \n" +  
				"  AND p1.cmd       = 'AWAITING COMMAND' \n" +  
				"  AND exists (SELECT * FROM sys.dm_tran_locks WHERE request_session_id = p1.spid AND resource_type != 'DATABASE') \n" + 
//				"  AND p1.spid in (select session_id from sys.dm_tran_session_transactions) \n" + 
				"  AND p1.ecid = 0 \n" + // Only Parent SPID's in parallel statements ... or we can add/introduce the ECID in the primary key... 
				"";
		}

		String sql_prePopuate = ""
				+ sql_tranInfo_populate;

		return ""
			+ sql_preDrop
			+ sql_prePopuate
			+ sql1
			+ sql2
			+ sql3
			+ sql_postDrop
			;
	}




	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		if ("lastKnownSql".equals(colName))
		{
			return cellValue == null ? null : toHtmlString( cellValue.toString() );
		}

		// 'HasBufferSqlText' STUFF
		if ("HasBufferSqlText".equals(colName))
		{
			// Find 'MonSqlText' column, is so get it and set it as the tool tip
			int pos_sqlText = findColumn("LastBufferSqlText");
			if (pos_sqlText > 0)
			{
				Object cellVal = getValueAt(modelRow, pos_sqlText);
				if (cellVal instanceof String)
				{
					return toHtmlString((String) cellVal);
				}
			}
		}

		// 'HasSqlText' STUFF
		if ("HasSqlText".equals(colName))
		{
			// Find 'MonSqlText' column, is so get it and set it as the tool tip
			int pos_MonSqlText = findColumn("lastKnownSql");
			if (pos_MonSqlText > 0)
			{
				Object cellVal = getValueAt(modelRow, pos_MonSqlText);
				if (cellVal instanceof String)
				{
					return toHtmlString((String) cellVal);
					//return (String) cellVal;
				}
			}
		}

		// 'HasQueryplan' STUFF
		if ("HasQueryplan".equals(colName))
		{
			// Find 'ShowPlanText' column, is so get it and set it as the tool tip
			int pos_ShowPlanText = findColumn("query_plan");
			if (pos_ShowPlanText > 0)
			{
				Object o_ShowPlanText = getValueAt(modelRow, pos_ShowPlanText);
				if (o_ShowPlanText instanceof String)
				{
					String xmlPlan = (String)o_ShowPlanText;
					//return (String) cellVal;
					return ToolTipSupplierSqlServer.createXmlPlanTooltip(xmlPlan);
				}
			}
		}
		
		// 'HasLiveQueryplan' STUFF
		if ("HasLiveQueryplan".equals(colName))
		{
			// Find 'ShowPlanText' column, is so get it and set it as the tool tip
			int pos_LiveQueryPlanText = findColumn("LiveQueryPlan");
			if (pos_LiveQueryPlanText > 0)
			{
				Object o_LiveQueryPlanText = getValueAt(modelRow, pos_LiveQueryPlanText);
				if (o_LiveQueryPlanText instanceof String)
				{
					String xmlPlan = (String)o_LiveQueryPlanText;
					return ToolTipSupplierSqlServer.createXmlPlanTooltip(xmlPlan);
				}
			}
		}
		
		if ("HasSpidLocks".equals(colName))
		{
			// Find 'SpidLocks' column, is so get it and set it as the tool tip
			int pos_SpidLocks = findColumn("SpidLocks");
			if (pos_SpidLocks > 0)
			{
				Object cellVal = getValueAt(modelRow, pos_SpidLocks);
				if (cellVal instanceof String)
				{
					return "<html><pre>" + cellVal + "</pre></html>";
				}
			}
		}
		if ("SpidLocks".equals(colName))
		{
			return cellValue == null ? null : "<html><pre>" + cellValue + "</pre></html>";
		}

		if ("HasBlockedSpidsInfo".equals(colName))
		{
			// Find 'BlockedSpidsInfo' column, is so get it and set it as the tool tip
			int pos_BlockedSpidsInfo = findColumn("BlockedSpidsInfo");
			if (pos_BlockedSpidsInfo > 0)
			{
				Object cellVal = getValueAt(modelRow, pos_BlockedSpidsInfo);
				if (cellVal == null)
					return "<html>No value</html>";
				else
					return "<html><pre>" + cellVal + "</pre></html>";
			}
		}
		if ("BlockedSpidsInfo".equals(colName))
		{
			return cellValue == null ? null : "<html><pre>" + cellValue + "</pre></html>";
		}


		if ("HasSpidWaitInfo".equals(colName))
		{
			// Find 'SpidLocks' column, is so get it and set it as the tool tip
			int pos_SpidWaitInfo = findColumn("SpidWaitInfo");
			if (pos_SpidWaitInfo > 0)
			{
				Object cellVal = getValueAt(modelRow, pos_SpidWaitInfo);
				if (cellVal instanceof String)
				{
					return "<html>" + cellVal + "</html>";
				}
			}
		}
		if ("SpidWaitInfo".equals(colName))
		{
			return cellValue == null ? null : "<html>" + cellValue + "</html>";
		}

		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}
	/** add HTML around the string, and translate line breaks into <br> */
	private String toHtmlString(String in)
	{
		if (in.indexOf("<html>")>=0 || in.indexOf("<HTML>")>=0)
			return in;

		String str = StringUtil.makeApproxLineBreak(in, 150, 10, "\n");
		str = str.replace("<","&lt;").replace(">","&gt;");
		str = str.replaceAll("\\n", "<br>");
		str = str.replaceAll("\\r", "");
		
		return "<html><pre>" + str + "</pre></html>";
	}

	
	
	
	
	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Sample System SPIDs"      , PROPKEY_sample_systemSpids    , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_systemSpids    , DEFAULT_sample_systemSpids    ), DEFAULT_sample_systemSpids    , "Sample System SPID's" ));
		list.add(new CmSettingsHelper("Get Query Plan"           , PROPKEY_sample_showplan       , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_showplan       , DEFAULT_sample_showplan       ), DEFAULT_sample_showplan       , "Also get queryplan" ));
		list.add(new CmSettingsHelper("Get SQL Text"             , PROPKEY_sample_monSqlText     , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_monSqlText     , DEFAULT_sample_monSqlText     ), DEFAULT_sample_monSqlText     , "Also get SQL Text"  ));
		list.add(new CmSettingsHelper("Get Live Query Plan"      , PROPKEY_sample_liveQueryPlan  , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_liveQueryPlan  , DEFAULT_sample_liveQueryPlan  ), DEFAULT_sample_liveQueryPlan  , "Also get LIVE queryplan" ));
		list.add(new CmSettingsHelper("Get SPID's holding locks" , PROPKEY_sample_holdingLocks   , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_holdingLocks   , DEFAULT_sample_holdingLocks   ), DEFAULT_sample_holdingLocks   , "Include SPID's that holds locks even if that are not active in the server." ));
		list.add(new CmSettingsHelper("Get SPID Locks"           , PROPKEY_sample_spidLocks      , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_spidLocks      , DEFAULT_sample_spidLocks      ), DEFAULT_sample_spidLocks      , "Do 'select <i>someCols</i> from syslockinfo where spid = ?' on every row in the table. This will help us to diagnose what the current SQL statement is locking."));
		list.add(new CmSettingsHelper("Get SPID Waits"           , PROPKEY_sample_spidWaits      , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_spidWaits      , DEFAULT_sample_spidWaits      ), DEFAULT_sample_spidWaits      , "Get info from CmSpidWait/dm_exec_session_wait_stats. This will help us to diagnose in detail what we have been waiting on since the last sample. NOTE: CmSpidWait needs to be enabled"));
		list.add(new CmSettingsHelper("Get SPID Input Buffer"    , PROPKEY_sample_spidInputBuffer, Boolean.class, conf.getBooleanProperty(PROPKEY_sample_spidInputBuffer, DEFAULT_sample_spidInputBuffer), DEFAULT_sample_spidInputBuffer, "Get info from sys.dm_exec_input_buffer. This will help us to diagnose what SQL Text the client sent to SQL Server"));

		return list;
	}

	
	@Override
	public Class<?> getColumnClass(int columnIndex)
	{
		// use CHECKBOX for some columns of type bit/Boolean
		String colName = getColumnName(columnIndex);

		if      ("HasBufferSqlText"   .equals(colName)) return Boolean.class;
		else if ("HasSqlText"         .equals(colName)) return Boolean.class;
		else if ("HasQueryplan"       .equals(colName)) return Boolean.class;
		else if ("HasLiveQueryplan"   .equals(colName)) return Boolean.class;
		else if ("HasSpidLocks"       .equals(colName)) return Boolean.class;
		else if ("HasBlockedSpidsInfo".equals(colName)) return Boolean.class;
		else if ("HasSpidWaitInfo"    .equals(colName)) return Boolean.class;
		else return super.getColumnClass(columnIndex);
	}

	/** 
	 * Fill in TempdbUsage*
	 */
	@Override
	public void localCalculation(CounterSample newSample)
	{
		// make: column 'program_name' with value "SQLAgent - TSQL JobStep (Job 0x38AAD6888E5C5E408DE573B0A25EE970 : Step 1)"
		// into:                                  "SQLAgent - TSQL JobStep (Job '<name-of-the-job>' : Step 1 '<name-of-the-step>')
		SqlServerCmUtils.localCalculation_resolveSqlAgentProgramName(newSample);


		boolean getTempdbSpidUsage = Configuration.getCombinedConfiguration().getBooleanProperty(CmSummary.PROPKEY_sample_tempdbSpidUsage, CmSummary.DEFAULT_sample_tempdbSpidUsage);

		int pos_SPID                      = newSample.findColumn("session_id");
		int pos_estimated_completion_time = newSample.findColumn("estimated_completion_time");
		int pos_estimated_compl_time_dhms = newSample.findColumn("estimated_compl_time_dhms");
		
		int pos_ExecTimeInMs       = newSample.findColumn("ExecTimeInMs");
		int pos_ExecTimeHms        = newSample.findColumn("ExecTimeHms");
		int pos_UsefullExecTime    = newSample.findColumn("UsefullExecTime");
		int pos_UsefullExecTimeHms = newSample.findColumn("UsefullExecTimeHms");
		
		// Loop on all diffData rows
		for (int rowId=0; rowId < newSample.getRowCount(); rowId++) 
		{
			Object o_SPID        = newSample.getValueAt(rowId, pos_SPID);
			
			if (o_SPID instanceof Number)
			{
				int spid = ((Number)o_SPID).intValue();

				// Maintain 'TempdbUsageMb*' columns
				if (getTempdbSpidUsage)
				{
					// 1: Get tempdb info about 'spid', and if we have a value:
					//    2: Set values for columns
					//       - TempdbUsageMb
					//       - TempdbUsageMbUser
					//       - TempdbUsageMbInternal
					//    (if the above columns can't be found... Simply write a message to the error log)
					//
					TempdbUsagePerSpid.TempDbSpaceInfo spaceInfo = TempdbUsagePerSpid.getInstance().getEntryForSpid(spid);
					if (spaceInfo != null)
					{
						newSample.setValueAt(spaceInfo.getTotalSpaceUsedInMb()               , rowId, "TempdbUsageMb");
						newSample.setValueAt(spaceInfo.getUserObjectSpaceUsedInMb()          , rowId, "TempdbUsageMbUser");
						newSample.setValueAt(spaceInfo.getInternalObjectSpaceUsedInMb()      , rowId, "TempdbUsageMbInternal");
					}
				}
			}
			
			// set: "estimated_completion_time" to: "#d #h #m #s"
			if (pos_estimated_completion_time != -1 && pos_estimated_compl_time_dhms != -1)
			{
				// Get value if Milliseconds
				int estimated_completion_time = newSample.getValueAsInteger(rowId, pos_estimated_completion_time, 0);

				String newValue_estimated_compl_time_dhms = "";
				if (estimated_completion_time > 0)
				{
					//String newValue_estimated_compl_time_dhms = TimeUtils.msToTimeStrDHMS(estimated_completion_time);
					long days    = TimeUnit.MILLISECONDS.toDays   (estimated_completion_time);
					long hours   = TimeUnit.MILLISECONDS.toHours  (estimated_completion_time) - TimeUnit.DAYS   .toHours(  TimeUnit.MILLISECONDS.toDays   (estimated_completion_time));
					long minutes = TimeUnit.MILLISECONDS.toMinutes(estimated_completion_time) - TimeUnit.HOURS  .toMinutes(TimeUnit.MILLISECONDS.toHours  (estimated_completion_time));
					long seconds = TimeUnit.MILLISECONDS.toSeconds(estimated_completion_time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(estimated_completion_time));
				//	long millis  = TimeUnit.MILLISECONDS.toMillis (estimated_completion_time) - TimeUnit.SECONDS.toMillis( TimeUnit.MILLISECONDS.toSeconds(estimated_completion_time));

					// Build expected string
					if ( days    > 0 ) newValue_estimated_compl_time_dhms += days    + "d" + " ";
					if ( hours   > 0 ) newValue_estimated_compl_time_dhms += hours   + "h" + " ";
					if ( minutes > 0 ) newValue_estimated_compl_time_dhms += minutes + "m" + " ";
					if ( seconds > 0 ) newValue_estimated_compl_time_dhms += seconds + "s" + " ";

					// Get rid of trailing spaces
					newValue_estimated_compl_time_dhms = newValue_estimated_compl_time_dhms.trim();
				}
				
				// Set value
				newSample.setValueAt(newValue_estimated_compl_time_dhms, rowId, pos_estimated_compl_time_dhms);
			}

			// set: "ExecTimeHms"
			if (pos_ExecTimeInMs != -1 && pos_ExecTimeHms != -1)
			{
				int ExecTimeInMs = newSample.getValueAsInteger(rowId, pos_ExecTimeInMs, 0);

				String newValue_ExecTimeHms = TimeUtils.msToTimeStrLong(ExecTimeInMs);

				// Set value
				newSample.setValueAt(newValue_ExecTimeHms, rowId, pos_ExecTimeHms);
			}

			// set: "UsefullExecTimeHms"
			if (pos_UsefullExecTime != -1 && pos_UsefullExecTimeHms != -1)
			{
				int UsefullExecTime = newSample.getValueAsInteger(rowId, pos_UsefullExecTime, 0);

				String newValue_UsefullExecTimeHms = TimeUtils.msToTimeStrLong(UsefullExecTime);

				// Set value
				newSample.setValueAt(newValue_UsefullExecTimeHms, rowId, pos_UsefullExecTimeHms);
			}
		}
	}

	/** 
	 * Fill in the WaitEventDesc column with data from
	 * MonTableDictionary.. transforms a WaitEventId -> text description
	 * This so we do not have to do a subselect in the query that gets data
	 * doing it this way, means better performance, since the values are cached locally in memory
	 * Also do post lookups of dbcc sqltext, sp_showplan, dbcc stacktrace
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
//	public void localCalculation(CounterSample newSample)
	{
//		long startTime = System.currentTimeMillis();

		Configuration conf = Configuration.getCombinedConfiguration();
		boolean getShowplan        = conf == null ? true : conf.getBooleanProperty(PROPKEY_sample_showplan,        DEFAULT_sample_showplan);
		boolean getMonSqltext      = conf == null ? true : conf.getBooleanProperty(PROPKEY_sample_monSqlText,      DEFAULT_sample_monSqlText);
//		boolean getDbccSqltext     = conf == null ? false: conf.getBooleanProperty(getName()+".sample.dbccSqltext",    false);
//		boolean getProcCallStack   = conf == null ? true : conf.getBooleanProperty(getName()+".sample.procCallStack",  true);
//		boolean getDbccStacktrace  = conf == null ? false: conf.getBooleanProperty(getName()+".sample.dbccStacktrace", false);
		boolean getLiveQueryPlan   = conf == null ? true : conf.getBooleanProperty(PROPKEY_sample_liveQueryPlan,   DEFAULT_sample_liveQueryPlan);
		boolean getSpidLocks       = conf == null ? false: conf.getBooleanProperty(PROPKEY_sample_spidLocks,       DEFAULT_sample_spidLocks);
		boolean getSpidWaits       = conf == null ? false: conf.getBooleanProperty(PROPKEY_sample_spidWaits,       DEFAULT_sample_spidWaits);
		boolean getSpidInputBuffer = conf == null ? false: conf.getBooleanProperty(PROPKEY_sample_spidInputBuffer, DEFAULT_sample_spidInputBuffer);

		// Where are various columns located in the Vector 
		int pos_SPID = -1;
//		int pos_WaitEventID         = -1, pos_WaitEventDesc     = -1, pos_WaitClassDesc = -1;
		int pos_HasShowPlan         = -1, pos_ShowPlanText      = -1;
		int pos_HasMonSqlText       = -1, pos_MonSqlText        = -1;
		int pos_HasBufferSqlText    = -1, pos_LastBufferSqlText = -1;
//		int pos_HasDbccSqlText      = -1, pos_DbccSqlText       = -1;
//		int pos_HasProcCallStack    = -1, pos_ProcCallStack     = -1;
//		int pos_HasStacktrace       = -1, pos_DbccStacktrace    = -1;
		int pos_HasLiveQueryPlan    = -1, pos_LiveQueryPlan     = -1;
		int pos_HasSpidLocks        = -1, pos_SpidLocks         = -1, pos_SpidLockCount = -1;
		int pos_HasBlockedSpidsInfo = -1, pos_BlockedSpidsInfo  = -1;

		int pos_SpidWaitCountSum    = -1;
		int pos_SpidWaitTimeMsSum   = -1;
		int pos_HasSpidWaitInfo     = -1, pos_SpidWaitInfo     = -1;

		
		int pos_BlockingOtherSpids = -1, pos_BlockingSPID   = -1;
		int pos_ImBlockingOthersMaxTimeInSec= -1;
		int pos_wait_time          = -1;
		int pos_multiSampled       = -1;
		int pos_StartTime          = -1;
		int pos_StmntStart         = -1; // this would be like "row number" inside a procedure (but it's the "character position" instead of row number 
		int pos_sql_handle         = -1;
//		int waitEventID = 0;
//		String waitEventDesc = "";
//		String waitClassDesc = "";
		CounterSample counters = diffData;
//		CounterSample counters = newSample;

		if ( ! MonTablesDictionaryManager.hasInstance() )
			return;
		MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

		if (counters == null)
			return;

		// Find column Id's
		List<String> colNames = counters.getColNames();
		if (colNames==null) 
			return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if      (false) ; // Dummy on first if, to make "move" easier 
//			else if (colName.equals("WaitEventID"))                  pos_WaitEventID                = colId;
//			else if (colName.equals("WaitEventDesc"))                pos_WaitEventDesc              = colId;
//			else if (colName.equals("WaitClassDesc"))                pos_WaitClassDesc              = colId;
//			else if (colName.equals("SPID"))                         pos_SPID                       = colId;
			else if (colName.equals("session_id"))                   pos_SPID                       = colId;
			else if (colName.equals("HasQueryplan"))                 pos_HasShowPlan                = colId;
			else if (colName.equals("query_plan"))                   pos_ShowPlanText               = colId;
			else if (colName.equals("HasBufferSqlText"))             pos_HasBufferSqlText           = colId;
			else if (colName.equals("LastBufferSqlText"))            pos_LastBufferSqlText          = colId;
			else if (colName.equals("HasSqlText"))                   pos_HasMonSqlText              = colId;
			else if (colName.equals("lastKnownSql"))                 pos_MonSqlText                 = colId;
			else if (colName.equals("HasLiveQueryplan"))             pos_HasLiveQueryPlan           = colId;
			else if (colName.equals("LiveQueryPlan"))                pos_LiveQueryPlan              = colId;
//			else if (colName.equals("HasDbccSqlText"))               pos_HasDbccSqlText             = colId;
//			else if (colName.equals("DbccSqlText"))                  pos_DbccSqlText                = colId;
//			else if (colName.equals("HasProcCallStack"))             pos_HasProcCallStack           = colId;
//			else if (colName.equals("ProcCallStack"))                pos_ProcCallStack              = colId;
//			else if (colName.equals("HasStacktrace"))                pos_HasStacktrace              = colId;
//			else if (colName.equals("DbccStacktrace"))               pos_DbccStacktrace             = colId;
			else if (colName.equals("ImBlockingOtherSessionIds"))    pos_BlockingOtherSpids         = colId;
			else if (colName.equals("ImBlockedBySessionId"))         pos_BlockingSPID               = colId;
			else if (colName.equals("ImBlockingOthersMaxTimeInSec")) pos_ImBlockingOthersMaxTimeInSec = colId;
			else if (colName.equals("wait_time"))                    pos_wait_time                  = colId;
			else if (colName.equals("multiSampled"))                 pos_multiSampled               = colId;
//			else if (colName.equals("StartTime"))                    pos_StartTime                  = colId;
			else if (colName.equals("start_time"))                   pos_StartTime                  = colId;
			else if (colName.equals("StmntStart"))                   pos_StmntStart                 = colId;
			else if (colName.equals("sql_handle"))                   pos_sql_handle                 = colId;
			else if (colName.equals("HasSpidLocks"))                 pos_HasSpidLocks               = colId;
			else if (colName.equals("SpidLocks"))                    pos_SpidLocks                  = colId;
			else if (colName.equals("SpidLockCount"))                pos_SpidLockCount              = colId;
			else if (colName.equals("HasBlockedSpidsInfo"))          pos_HasBlockedSpidsInfo        = colId;
			else if (colName.equals("BlockedSpidsInfo"))             pos_BlockedSpidsInfo           = colId;

			else if (colName.equals("SpidWaitCountSum"))             pos_SpidWaitCountSum           = colId;
			else if (colName.equals("SpidWaitTimeMsSum"))            pos_SpidWaitTimeMsSum          = colId;
			else if (colName.equals("HasSpidWaitInfo"))              pos_HasSpidWaitInfo            = colId;
			else if (colName.equals("SpidWaitInfo"))                 pos_SpidWaitInfo               = colId;
		}

//		if (pos_WaitEventID < 0 || pos_WaitEventDesc < 0 || pos_WaitClassDesc < 0)
//		{
//			_logger.debug("Can't find the position for columns ('WaitEventID'="+pos_WaitEventID+", 'WaitEventDesc'="+pos_WaitEventDesc+", 'WaitClassDesc'="+pos_WaitClassDesc+")");
//			return;
//		}
		
		if (pos_SPID < 0 || pos_HasShowPlan < 0 || pos_ShowPlanText < 0)
		{
System.out.println("Can't find the position for columns ('SPID'="+pos_SPID+", 'HasShowPlan'="+pos_HasShowPlan+", 'ShowPlanText'="+pos_ShowPlanText+")");
			_logger.debug("Can't find the position for columns ('SPID'="+pos_SPID+", 'HasShowPlan'="+pos_HasShowPlan+", 'ShowPlanText'="+pos_ShowPlanText+")");
			return;
		}

//		if (pos_HasDbccSqlText < 0 || pos_DbccSqlText < 0)
//		{
//			_logger.debug("Can't find the position for columns ('HasDbccSqlText'="+pos_HasDbccSqlText+", 'DbccSqlText'="+pos_DbccSqlText+")");
//			return;
//		}
//
//		if (pos_HasProcCallStack < 0 || pos_ProcCallStack < 0)
//		{
//			_logger.debug("Can't find the position for columns ('HasProcCallStack'="+pos_HasProcCallStack+", 'ProcCallStack'="+pos_ProcCallStack+")");
//			return;
//		}

		if (pos_HasMonSqlText < 0 || pos_MonSqlText < 0)
		{
System.out.println("Can't find the position for columns (''HasMonSqlText'="+pos_HasMonSqlText+", 'MonSqlText'="+pos_MonSqlText+")");
			_logger.debug("Can't find the position for columns (''HasMonSqlText'="+pos_HasMonSqlText+", 'MonSqlText'="+pos_MonSqlText+")");
			return;
		}

//		if (pos_HasStacktrace < 0 || pos_DbccStacktrace < 0)
//		{
//			_logger.debug("Can't find the position for columns ('HasShowplan'="+pos_HasStacktrace+", 'DbccStacktrace'="+pos_DbccStacktrace+")");
//			return;
//		}
		
		if (pos_HasLiveQueryPlan < 0 || pos_LiveQueryPlan < 0)
		{
System.out.println("Can't find the position for columns ('HasLiveQueryPlan'="+pos_HasLiveQueryPlan+", 'LiveQueryPlan'="+pos_LiveQueryPlan+")");
			_logger.debug("Can't find the position for columns ('HasLiveQueryPlan'="+pos_HasLiveQueryPlan+", 'LiveQueryPlan'="+pos_LiveQueryPlan+")");
			return;
		}

		if (pos_BlockingOtherSpids < 0 || pos_BlockingSPID < 0)
		{
System.out.println("Can't find the position for columns ('BlockingOtherSpids'="+pos_BlockingOtherSpids+", 'BlockingSPID'="+pos_BlockingSPID+")");
			_logger.debug("Can't find the position for columns ('BlockingOtherSpids'="+pos_BlockingOtherSpids+", 'BlockingSPID'="+pos_BlockingSPID+")");
			return;
		}
		
		if (pos_wait_time < 0|| pos_ImBlockingOthersMaxTimeInSec < 0)
		{
System.out.println("Can't find the position for columns ('wait_time'="+pos_wait_time+", 'ImBlockingOthersMaxTimeInSec'="+pos_ImBlockingOthersMaxTimeInSec+")");
			_logger.debug("Can't find the position for columns ('wait_time'="+pos_wait_time+", 'ImBlockingOthersMaxTimeInSec'="+pos_ImBlockingOthersMaxTimeInSec+")");
			return;
		}
		
		if (pos_multiSampled < 0)
		{
System.out.println("Can't find the position for columns ('multiSampled'="+pos_multiSampled+")");
			_logger.debug("Can't find the position for columns ('multiSampled'="+pos_multiSampled+")");
			return;
		}
		
		if (pos_StartTime < 0)
		{
System.out.println("Can't find the position for columns ('StartTime'="+pos_StartTime+")");
			_logger.debug("Can't find the position for columns ('StartTime'="+pos_StartTime+")");
			return;
		}

		if (pos_StmntStart < 0)
		{
System.out.println("Can't find the position for columns ('StmntStart'="+pos_StmntStart+")");
			_logger.debug("Can't find the position for columns ('StmntStart'="+pos_StmntStart+")");
			return;
		}

		if (pos_sql_handle < 0)
		{
System.out.println("Can't find the position for columns ('sql_handle'="+pos_sql_handle+")");
			_logger.debug("Can't find the position for columns ('sql_handle'="+pos_sql_handle+")");
			return;
		}

		if (pos_HasSpidLocks < 0 || pos_SpidLocks < 0 || pos_SpidLockCount < 0)
		{
			_logger.debug("Can't find the position for columns ('HasSpidLocks'="+pos_HasSpidLocks+", 'SpidLocks'="+pos_SpidLocks+", 'SpidLockCount'="+pos_SpidLockCount+")");
			return;
		}
		
		if (pos_BlockedSpidsInfo < 0)
		{
			_logger.debug("Can't find the position for columns ('BlockedSpidsInfo'="+pos_BlockedSpidsInfo+")");
			return;
		}

		if (pos_HasBlockedSpidsInfo < 0)
		{
			_logger.debug("Can't find the position for columns ('HasBlockedSpidsInfo'="+pos_HasBlockedSpidsInfo+")");
			return;
		}

		// Used to NOT lookup AFTER first time it happened in THIS loop
		int getLockSummaryForSpid_timeoutOnPrevoisLookup_rowId = -1;

		// Loop on all diffData rows
		for (int rowId=0; rowId < counters.getRowCount(); rowId++) 
		{
			String thisRowPk = counters.getPkValue(rowId);
			int prevPkRowId = (prevSample == null) ? -1 : prevSample.getRowNumberForPkValue(thisRowPk);
			boolean prevPkExists = prevPkRowId >= 0;

//			Object o_waitEventId = counters.getValueAt(rowId, pos_WaitEventID);
			Object o_SPID        = counters.getValueAt(rowId, pos_SPID);
//System.out.println("xxx: rowId="+rowId+", thisRowPk='"+thisRowPk+"', prevPkRowId="+prevPkRowId+", prevPkExists="+prevPkExists+", o_SPID="+o_SPID);

			if (prevPkExists)
			{
				Object o_this_StartTime = counters  .getValueAt(       rowId,       pos_StartTime);
				Object o_prev_StartTime = prevSample.getValueAt(       prevPkRowId, pos_StartTime);

				int    this_StmntStart = counters   .getValueAsInteger(rowId,       pos_StmntStart, -1);
				int    prev_StmntStart = prevSample .getValueAsInteger(prevPkRowId, pos_StmntStart, -1);

				String this_sql_handle = counters   .getValueAsString(rowId,        pos_sql_handle); // returns "" if null
				String prev_sql_handle = prevSample .getValueAsString(prevPkRowId,  pos_sql_handle); // returns "" if null

				if (o_this_StartTime instanceof Timestamp && o_prev_StartTime instanceof Timestamp)
				{
					if ( o_this_StartTime.equals(o_prev_StartTime) 
					    && this_StmntStart == prev_StmntStart 
					    && this_sql_handle.equals(prev_sql_handle))
					{
						counters.setValueAt("YES", rowId, pos_multiSampled);
					}
				}
			}

//			if (o_waitEventId instanceof Number)
//			{
//				waitEventID	  = ((Number)o_waitEventId).intValue();
//
//				if (mtd.hasWaitEventDescription(waitEventID))
//				{
//					waitEventDesc = mtd.getWaitEventDescription(waitEventID);
//					waitClassDesc = mtd.getWaitEventClassDescription(waitEventID);
//				}
//				else
//				{
//					waitEventDesc = "";
//					waitClassDesc = "";
//				}
//
//				//row.set( pos_WaitEventDesc, waitEventDesc);
//				counters.setValueAt(waitEventDesc, rowId, pos_WaitEventDesc);
//				counters.setValueAt(waitClassDesc, rowId, pos_WaitClassDesc);
//			}

			if (o_SPID instanceof Number)
			{
				int spid = ((Number)o_SPID).intValue();

				String spidLocks       = "This was disabled";
				int    spidLockCount   = -1;

//				if (srvVersion >= Ver.ver(2016,0,0, 1)) // 2016 SP1 (or check if the table exists; select 1 from sys.all_tables where name = 'dm_exec_query_statistics_xml'
//				{
				if (getLiveQueryPlan && getServerVersion() >= Ver.ver(2016,0,0, 1)) // 2016 SP1 
				{
					String liveQueryPlan = SqlServerUtils.getLiveQueryPlanNoThrow(getCounterController().getMonConnection(), spid);
					if (StringUtil.hasValue(liveQueryPlan))
						counters.setValueAt(liveQueryPlan, rowId, pos_LiveQueryPlan);
				}

				if (getSpidLocks)
				{
					List<LockRecord> lockList = null;

					if (getLockSummaryForSpid_timeoutOnPrevoisLookup_rowId != -1)
					{
						spidLocks = "Timeout - on rowId=" + getLockSummaryForSpid_timeoutOnPrevoisLookup_rowId + ", so skipping LockSummary for this ActiveStatement (spid=" + spid + ")";
					}
					else
					{
						try
						{
							lockList = SqlServerUtils.getLockSummaryForSpid(getCounterController().getMonConnection(), spid, this);
							spidLocks = SqlServerUtils.getLockSummaryForSpid(lockList, true, false);
							if (spidLocks == null)
								spidLocks = "No Locks found";
						}
						catch (TimeoutException ex)
						{
							spidLocks = "Timeout - when getting lock information: " + ex.getMessage();
							getLockSummaryForSpid_timeoutOnPrevoisLookup_rowId = rowId;
						}
					}
					
					spidLockCount = 0;
					if (lockList != null)
					{
						for (LockRecord lockRecord : lockList)
						{
							spidLockCount += lockRecord._lockCount;
						}
					}
				}
				
				// Get information from: CmSpidWait
				if (getSpidWaits)
				{
					if (pos_SpidWaitCountSum == -1 || pos_SpidWaitTimeMsSum == -1 || pos_HasSpidWaitInfo == -1 || pos_SpidWaitInfo == -1)
					{
						_logger.info("Skipping 'getSpidWaits' due to not all columns was found. pos_SpidWaitCountSum=" + pos_SpidWaitCountSum + ", pos_SpidWaitTimeMsSum=" + pos_SpidWaitTimeMsSum + ", pos_HasSpidWaitInfo=" + pos_HasSpidWaitInfo + ", pos_SpidWaitInfo=" + pos_SpidWaitInfo);
					}
					else
					{
						int dop = -1;
						int dop_pos = counters.findColumn("dop");
						if (dop_pos != -1)
						{
							dop = counters.getValueAsInteger(rowId, dop_pos, 0);
						}
						
						CountersModel cmSpidWait = getCounterController().getCmByName(CmSpidWait.CM_NAME);
						if (cmSpidWait != null && cmSpidWait.isActive())
						{
							int[] spidWaitRowIds = cmSpidWait.getAbsRowIdsWhere("session_id", spid);
//System.out.println("XXXXXXXXXXXXXXXXXXXXXX: spid=" + spid + ", spidWaitRowIds=" + StringUtil.toCommaStr(spidWaitRowIds));
							if (spidWaitRowIds != null && spidWaitRowIds.length > 0)
							{
								int pos_wait_type            = cmSpidWait.findColumn("wait_type");
								int pos_WaitClass            = cmSpidWait.findColumn("WaitClass");
								int pos_waiting_tasks_count  = cmSpidWait.findColumn("waiting_tasks_count");
								int pos_wait_time_ms         = cmSpidWait.findColumn("wait_time_ms");

								if (pos_wait_type == -1 || pos_WaitClass == -1 || pos_HasSpidWaitInfo == -1 || pos_SpidWaitInfo == -1)
								{
									_logger.info("Issue finding columns in CmSpidWait: cmSpidWait.pos_wait_type=" + pos_wait_type + ", cmSpidWait.pos_WaitClass=" + pos_WaitClass + "', cmSpidWait.pos_WaitClass=" + pos_WaitClass + ", cmSpidWait.pos_waiting_tasks_count=" + pos_waiting_tasks_count + ", cmSpidWait.pos_wait_time_ms=" + pos_wait_time_ms);
								}
								
								NumberFormat nf = NumberFormat.getInstance();

								long abs_spidWaitCountSum  = 0;
								long diff_spidWaitCountSum = 0;

								long abs_spidWaitTimeMsSum  = 0;
								long diff_spidWaitTimeMsSum = 0;
								
								String abs_waitInfoTable    = "";
								String diff_waitInfoTable   = "";
								String rate_waitInfoTable   = "";

								for (int spidWaitRowId : spidWaitRowIds)
								{
									String wait_type = cmSpidWait.getAbsString(spidWaitRowId, pos_wait_type);
									String WaitClass = cmSpidWait.getAbsString(spidWaitRowId, pos_WaitClass);

									long cmSpidWait_sampleInterval = cmSpidWait.getSampleInterval();
									
									// NOTE: cmSpidWait.get>>Rate<<Value... is not YET CALCULATED here
									//       so we need to calculate it ourself... hence the 'cmSpidWait_sampleInterval'

									long   abs_waiting_tasks_count  = cmSpidWait.getAbsValueAsLong   (spidWaitRowId, pos_waiting_tasks_count, 0L);
									long   diff_waiting_tasks_count = cmSpidWait.getDiffValueAsLong  (spidWaitRowId, pos_waiting_tasks_count, 0L);
									double rate_waiting_tasks_count = cmSpidWait_sampleInterval <= 0 ? 0 : (diff_waiting_tasks_count * 1.0) / (cmSpidWait_sampleInterval / 1000.0);

									long   abs_wait_time_ms  = cmSpidWait.getAbsValueAsLong   (spidWaitRowId, pos_wait_time_ms, 0L);
									long   diff_wait_time_ms = cmSpidWait.getDiffValueAsLong  (spidWaitRowId, pos_wait_time_ms, 0L);
									double rate_wait_time_ms = cmSpidWait_sampleInterval <= 0 ? 0 : (diff_wait_time_ms * 1.0) / (cmSpidWait_sampleInterval / 1000.0);

									double abs_spidWaitTimePerCount  = abs_waiting_tasks_count  <= 0 ? 0D : ( abs_wait_time_ms*1.0) / ( abs_waiting_tasks_count*1.0);
									double diff_spidWaitTimePerCount = diff_waiting_tasks_count <= 0 ? 0D : (diff_wait_time_ms*1.0) / (diff_waiting_tasks_count*1.0);

									String diff_wait_time_pct = "n/a";
									if (cmSpidWait_sampleInterval > 0 && diff_wait_time_ms > 0)
									{
										double calc = NumberUtils.round( ((diff_wait_time_ms*1.0) / (cmSpidWait_sampleInterval*1.0)) * 100.0, 1);
										diff_wait_time_pct = calc + " %";
									}
									
//System.out.println("            >>>>>>>>> : spid=" + spid + ", spidWaitRowId=" + spidWaitRowId + ", wait_type='" + StringUtil.left(wait_type, 40) + "', WaitClass='" + StringUtil.left(WaitClass, 15) + "', abs_waiting_tasks_count=" + abs_waiting_tasks_count + ", diff_waiting_tasks_count=" + diff_waiting_tasks_count + ", abs_wait_time_ms=" + abs_wait_time_ms + ", diff_wait_time_ms=" + diff_wait_time_ms + ".");
									// Add to SUM
									abs_spidWaitCountSum   += abs_waiting_tasks_count;
									diff_spidWaitCountSum  += diff_waiting_tasks_count;

									// Add to SUM
									abs_spidWaitTimeMsSum  += abs_wait_time_ms;
									diff_spidWaitTimeMsSum += diff_wait_time_ms;
									
									// ABS Table
									if (abs_waiting_tasks_count > 0 || abs_wait_time_ms > 0)
									{
										abs_waitInfoTable += "<tr>"
												+ "<td>" + spid                                              + "</td>"
												+ (dop_pos == -1 ? "" : "<td>" + dop                         + "</td>")
												+ "<td>" + wait_type                                         + "</td>"
												+ "<td>" + WaitClass                                         + "</td>"
												+ "<td align='right'>" + nf.format(abs_waiting_tasks_count)  + "</td>"
												+ "<td align='right'>" + nf.format(abs_wait_time_ms)         + "</td>"
												+ "<td align='right'>" + nf.format(abs_spidWaitTimePerCount) + "</td>"
												+ "</tr> \n";
									}

									// DIFF Table
									if (diff_waiting_tasks_count > 0 || diff_wait_time_ms > 0)
									{
										diff_waitInfoTable += "<tr>"
												+ "<td>" + spid                                               + "</td>"
												+ (dop_pos == -1 ? "" : "<td>" + dop                          + "</td>")
												+ "<td>" + wait_type                                          + "</td>"
												+ "<td>" + WaitClass                                          + "</td>"
												+ "<td align='right'>" + nf.format(diff_waiting_tasks_count)  + "</td>"
												+ "<td align='right'>" + nf.format(diff_wait_time_ms)         + "</td>"
												+ "<td align='right'>" +           diff_wait_time_pct         + "</td>"
												+ "<td align='right'>" + nf.format(diff_spidWaitTimePerCount) + "</td>"
												+ "</tr> \n";
									}

									// RATE Table
									if (rate_waiting_tasks_count > 0 || rate_wait_time_ms > 0)
									{
										rate_waitInfoTable += "<tr>"
												+ "<td>" + spid                                               + "</td>"
												+ (dop_pos == -1 ? "" : "<td>" + dop                          + "</td>")
												+ "<td>" + wait_type                                          + "</td>"
												+ "<td>" + WaitClass                                          + "</td>"
												+ "<td align='right'>" + nf.format(rate_waiting_tasks_count)  + "</td>"
												+ "<td align='right'>" + nf.format(rate_wait_time_ms)         + "</td>"
												+ "<td align='right'>" + nf.format(cmSpidWait_sampleInterval) + "</td>" 
												+ "<td align='right'>" + nf.format(diff_waiting_tasks_count)  + "</td>"
												+ "<td align='right'>" + nf.format(diff_wait_time_ms)         + "</td>" 
												+ "<td align='right'>" +           diff_wait_time_pct         + "</td>"
												+ "<td align='right'>" + nf.format(diff_spidWaitTimePerCount) + "</td>"
												+ "</tr> \n";
									}
								} // end: loop PK rows
								
								// SpidWaitCountSum
								newSample.setValueAt(abs_spidWaitCountSum , rowId, pos_SpidWaitCountSum);
								diffData .setValueAt(diff_spidWaitCountSum, rowId, pos_SpidWaitCountSum);

								// SpidWaitTimeMsSum
								newSample.setValueAt(abs_spidWaitTimeMsSum , rowId, pos_SpidWaitTimeMsSum);
								diffData .setValueAt(diff_spidWaitTimeMsSum, rowId, pos_SpidWaitTimeMsSum);

								// ABS: SpidWaitInfo
								if (StringUtil.hasValue(abs_waitInfoTable))
								{
									String strVal = "<table class='dbx-table-basic tablesorter' border='1'> \n"
											+ "<tr>"
											+ " <th>spid</th>"
											+ (dop_pos == -1 ? "" : "<th>dop</th>")
											+ " <th>wait_type</th>"
											+ " <th>WaitClass</th>"
											+ " <th>ABS: waiting_tasks_count</th>"
											+ " <th>ABS: wait_time_ms</th>"
											+ " <th>ABS: WaitTimePerCount</th>"
											+ "</tr> \n" 
											+ abs_waitInfoTable
											+ "</table> \n";
									newSample.setValueAt(true  , rowId, pos_HasSpidWaitInfo);
									newSample.setValueAt(strVal, rowId, pos_SpidWaitInfo);
								}

								// DIFF: SpidWaitInfo
								if (StringUtil.hasValue(diff_waitInfoTable))
								{
									String strVal = "<table class='dbx-table-basic tablesorter' border='1'> \n"
											+ "<tr>"
											+ " <th>spid</th>"
											+ (dop_pos == -1 ? "" : "<th>dop</th>")
											+ " <th>wait_type</th>"
											+ " <th>WaitClass</th>"
											+ " <th>DIFF: waiting_tasks_count</th>"
											+ " <th>DIFF: wait_time_ms</th>"
											+ " <th>DIFF: WaitTimePct</th>"
											+ " <th>DIFF: WaitTimePerCount</th>"
											+ "</tr> \n" 
											+ diff_waitInfoTable
											+ "</table>";
									diffData.setValueAt(true  , rowId, pos_HasSpidWaitInfo);
									diffData.setValueAt(strVal, rowId, pos_SpidWaitInfo);
								}

								// RATE: SpidWaitInfo
								if (StringUtil.hasValue(rate_waitInfoTable))
								{
									String strVal = "<table class='dbx-table-basic tablesorter' border='1'> \n"
											+ "<tr>"
											+ " <th>spid</th>"
											+ (dop_pos == -1 ? "" : "<th>dop</th>")
											+ " <th>wait_type</th>"
											+ " <th>WaitClass</th>"
											+ " <th>RATE: waiting_tasks_count</th>"
											+ " <th>RATE: wait_time_ms</th>"
											+ " <th>CmSpidWait: SampleInterval Ms</th>"
											+ " <th>DIFF: waiting_tasks_count</th>"
											+ " <th>DIFF: wait_time_ms</th>"
											+ " <th>DIFF: WaitTimePct</th>"
											+ " <th>DIFF: WaitTimePerCount</th>"
											+ "</tr> \n" 
											+ rate_waitInfoTable
											+ "</table>";

									// Since RATE values are not yet available, we cant do 'rateData.setValueAt(...)'
									// So for the RATE values, lest save it in a Map<rowId, SpidWaitInfoTable>
									// and in method 'localCalculationRatePerSec(CounterSample rateData, CounterSample diffData)'
									//        we use '_tmpRate_spidWaitInfoMap' and set the RATE values
									//        at the end of 'localCalculationRatePerSec' the '_tmpRate_spidWaitInfoMap' should be set to NULL
									// Yes this is a "hack", but it was much simpler that other solutions...
									if (_tmpRate_spidWaitInfoMap == null)
										_tmpRate_spidWaitInfoMap = new HashMap<>();
									
									_tmpRate_spidWaitInfoMap.put(rowId, strVal);
									//rateData.setValueAt(true  , rowId, pos_HasSpidWaitInfo);
									//rateData.setValueAt(strVal, rowId, pos_SpidWaitInfo);
								}
							} // end: has spidWaitRowIds

						} // end: cmSpidWait != null
						
					} // end: has-wanted-columns

				} // end: getSpidWaits

//				String monSqlText    = "Not properly configured (need 'SQL batch capture' & 'max SQL text monitored').";
//				String dbccSqlText   = "User does not have: sa_role";
//				String procCallStack = "User does not have: sa_role";
//				String showplan      = "User does not have: sa_role";
//				String stacktrace    = "User does not have: sa_role";
//
//				if (getMonitorConfig("SQL batch capture") > 0 && getMonitorConfig("max SQL text monitored") > 0)
//				{
//					// monProcessSQLText; needs 'enable monitoring', 'SQL batch capture' and 'max SQL text monitored' configuration parameters for this monitoring table to collect data.
//					if (getMonSqltext)
//						monSqlText  = AseConnectionUtils.monSqlText(getCounterController().getMonConnection(), spid, true);
//					else
//						monSqlText = "This was disabled";
//					if (monSqlText == null)
//						monSqlText = "Not Available";
//				}
//				if (isServerRoleOrPermissionActive(AseConnectionUtils.SA_ROLE))
//				{
//					if (getDbccSqltext)
//						dbccSqlText  = AseConnectionUtils.dbccSqlText(getCounterController().getMonConnection(), spid, true);
//					else
//						dbccSqlText = "This was disabled";
//					if (dbccSqlText == null)
//						dbccSqlText = "Not Available";
//
//					if (getProcCallStack)
//						procCallStack  = AseConnectionUtils.monProcCallStack(getCounterController().getMonConnection(), spid, true);
//					else
//						procCallStack = "This was disabled";
//					if (procCallStack == null)
//						procCallStack = "Not Available";
//
//					if (getShowplan)
//						showplan = AseConnectionUtils.getShowplan(getCounterController().getMonConnection(), spid, "Showplan:", true);
//					else
//						showplan = "This was disabled";
//					if (showplan == null)
//						showplan = "Not Available";
//
//					if (getDbccStacktrace)
//						stacktrace = AseConnectionUtils.dbccStacktrace(getCounterController().getMonConnection(), spid, true, waitEventID);
//					else
//						stacktrace = "This was disabled";
//					if (stacktrace == null)
//						stacktrace = "Not Available";
//				}
//				boolean b = true;
//				b = !"This was disabled".equals(monSqlText)    && !"Not Available".equals(monSqlText)    && !monSqlText   .startsWith("Not properly configured");
//				counters.setValueAt(Boolean.valueOf(b), rowId, pos_HasMonSqlText);
//				counters.setValueAt(monSqlText,     rowId, pos_MonSqlText);
//
//				b = !"This was disabled".equals(dbccSqlText)   && !"Not Available".equals(dbccSqlText)   && !dbccSqlText  .startsWith("User does not have");
//				counters.setValueAt(Boolean.valueOf(b), rowId, pos_HasDbccSqlText);
//				counters.setValueAt(dbccSqlText,    rowId, pos_DbccSqlText);
//
//				b = !"This was disabled".equals(procCallStack) && !"Not Available".equals(procCallStack) && !procCallStack.startsWith("User does not have");
//				counters.setValueAt(Boolean.valueOf(b), rowId, pos_HasProcCallStack);
//				counters.setValueAt(procCallStack,  rowId, pos_ProcCallStack);
//
//				b = !"This was disabled".equals(showplan)      && !"Not Available".equals(showplan)      && !showplan     .startsWith("User does not have");
//				counters.setValueAt(Boolean.valueOf(b), rowId, pos_HasShowPlan);
//				counters.setValueAt(showplan,       rowId, pos_ShowPlanText);
//
//				b = !"This was disabled".equals(stacktrace)    && !"Not Available".equals(stacktrace)    && !stacktrace   .startsWith("User does not have");
//				counters.setValueAt(Boolean.valueOf(b), rowId, pos_HasStacktrace);
//				counters.setValueAt(stacktrace,     rowId, pos_DbccStacktrace);

				boolean b;
				Object obj;

				// Buffer SQL-Text check box
//				if (getSpidInputBuffer && pos_HasBufferSqlText != -1 && pos_LastBufferSqlText != -1)
				if (pos_HasBufferSqlText != -1 && pos_LastBufferSqlText != -1)
				{
					obj = counters.getValueAt(rowId, pos_LastBufferSqlText);
					b = (obj != null && obj instanceof String && StringUtil.hasValue((String)obj)); 
					counters.setValueAt(Boolean.valueOf(b), rowId, pos_HasBufferSqlText);
				}

				// SQL-Text check box
				obj = counters.getValueAt(rowId, pos_MonSqlText);
				b = (obj != null && obj instanceof String && StringUtil.hasValue((String)obj)); 
				counters.setValueAt(Boolean.valueOf(b), rowId, pos_HasMonSqlText);

				// QueryPlan check box
				obj = counters.getValueAt(rowId, pos_ShowPlanText);
				b = (obj != null && obj instanceof String && StringUtil.hasValue((String)obj)); 
				counters.setValueAt(Boolean.valueOf(b), rowId, pos_HasShowPlan);

				// LiveQueryPlan check box
				obj = counters.getValueAt(rowId, pos_LiveQueryPlan);
				b = (obj != null && obj instanceof String && StringUtil.hasValue((String)obj)); 
				counters.setValueAt(Boolean.valueOf(b), rowId, pos_HasLiveQueryPlan);

				// SPID Locks
				b = !"This was disabled".equals(spidLocks) && !"No Locks found".equals(spidLocks) && !"Timeout - when getting lock information".equals(spidLocks);
                counters.setValueAt(Boolean.valueOf(b), rowId, pos_HasSpidLocks);
                counters.setValueAt(spidLocks,      rowId, pos_SpidLocks);
                counters.setValueAt(spidLockCount,  rowId, pos_SpidLockCount);


				// Get LIST of SPID's that I'm blocking
				String blockingList = getBlockingListStr(counters, spid, pos_BlockingSPID, pos_SPID);

				// Get MaxBlockingTime of SPID's that I'm blocking
				int ImBlockingOthersMaxTimeInSec = getMaxBlockingTimeInSecForSpid(counters, spid, pos_BlockingSPID, pos_wait_time);

				// This could be used to test that PCS.store() will truncate string size to the tables storage size
				//blockingList += "'1:aaa:0', '1:bbb:0', '1:ccc:0', '1:ddd:0', '1:eee:0', '1:fff:0', '1:ggg:0', '1:hhh:0', '1:iii:0', '1:jjj:0', '1:kkk:0', '1:lll:0', '1:mmm:0', '1:nnn:0', '1:ooo:0', '1:ppp:0', '1:qqq:0', '1:rrr:0', '1:sss:0', '1:ttt:0', '1:uuu:0', '1:vvv:0', '1:wwww:0', '1:xxx:0', '1:yyy:0', '1:zzz:0' -end-";

				counters.setValueAt(blockingList,                 rowId, pos_BlockingOtherSpids);
				counters.setValueAt(ImBlockingOthersMaxTimeInSec, rowId, pos_ImBlockingOthersMaxTimeInSec);
			} // end: SPID is a number
		} // end: Loop on all diffData rows
		
		// Loop a seconds time, This to:
		// Fill in the column 'BlockedSpidsInfo'
		// - If this SPID is a BLOCKER - the root cause of blocking other SPID's
		//   Then get: get already collected Showplans etc for SPID's that are BLOCKED (the victims)
		// This will be helpful (to see both side of the story; ROOT cause and the VICTIMS) in a alarm message
		if (pos_BlockedSpidsInfo >= 0)
		{
			for (int rowId=0; rowId < counters.getRowCount(); rowId++) 
			{
				Object o_BlockingOtherSpids = counters.getValueAt(rowId, pos_BlockingOtherSpids);

				// MAYBE TODO: possibly check if the 'monSource' is of type "BLOCKER", before getting: getBlockedSpidInfo()
				
				if (o_BlockingOtherSpids != null && o_BlockingOtherSpids instanceof String)
				{
					String str_BlockingOtherSpids = (String) o_BlockingOtherSpids;
					if (StringUtil.hasValue(str_BlockingOtherSpids))
					{
						List<String> list_BlockingOtherSpids = StringUtil.parseCommaStrToList(str_BlockingOtherSpids);
						
						String blockedInfoStr = getBlockedSpidInfo(counters, pos_SPID, list_BlockingOtherSpids, pos_MonSqlText, pos_ShowPlanText, pos_LiveQueryPlan, pos_SpidLocks);
						if (StringUtil.hasValue(blockedInfoStr))
						{
							counters.setValueAt(blockedInfoStr, rowId, pos_BlockedSpidsInfo);
							counters.setValueAt(true,           rowId, pos_HasBlockedSpidsInfo);
						}
					}
				}
			} // end: loop all rows
		} // end: BlockedSpidsInfo
	} // end: method

	/**
	 *  <pre>Map&lt;rowId, HtmlTableStr&gt; </pre><br>
	 *  Used to temporary hold SPID Wait RATE information... which isn't available at the time in localCalculation(prevSample, newSample, diffData)
	 *  <p>
	 *  This is set in method: localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)' <br>
	 *  And should be set to NULL at the end of method: localCalculationRatePerSec(CounterSample rateData, CounterSample diffData)
	 */
	private Map<Integer, String> _tmpRate_spidWaitInfoMap;
	
	@Override
	public void localCalculationRatePerSec(CounterSample rateData, CounterSample diffData)
	{
		// in method 'localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)'
		// the RATE values are not yet created, so we need to set/adjust the column "SpidWaitInfo" and set the HTML Table created in 'localCalculation()'
		// Yes this is a "hack", but it was much simpler that other solutions...
		if (_tmpRate_spidWaitInfoMap != null)
		{
			for (Entry<Integer, String> entry : _tmpRate_spidWaitInfoMap.entrySet())
			{
				int    rowId        = entry.getKey();
				String spidWaitInfo = entry.getValue();

				rateData.setValueAt(spidWaitInfo, rowId, "SpidWaitInfo");
			}

			_tmpRate_spidWaitInfoMap = null;
		}
	}

//	@Override
//	protected Object clone() throws CloneNotSupportedException
//	{
//		// TODO Auto-generated method stub
//		return super.clone();
//	}

//	private String getBlockingListStr(CounterSample counters, int spid, int pos_BlockingSPID, int pos_SPID)
//	{
//		StringBuilder sb = new StringBuilder();
//
//		// Loop on all diffData rows
//		int rows = counters.getRowCount();
//		for (int rowId=0; rowId < rows; rowId++)
//		{
//			Object o_BlockingSPID = counters.getValueAt(rowId, pos_BlockingSPID);
//			if (o_BlockingSPID instanceof Number)
//			{
//				Number thisRow = (Number)o_BlockingSPID;
//				if (thisRow.intValue() == spid)
//				{
//					Object o_SPID = counters.getValueAt(rowId, pos_SPID);
//					if (sb.length() == 0)
//						sb.append(o_SPID);
//					else
//						sb.append(", ").append(o_SPID);
//				}
//			}
//		}
//		return sb.toString();
//	}
	private String getBlockingListStr(CounterSample counters, int spid, int pos_BlockingSPID, int pos_SPID)
	{
		Set<Integer> spidSet = null;

		// Loop on all diffData rows
		int rows = counters.getRowCount();
		for (int rowId=0; rowId<rows; rowId++)
		{
			Object o_BlockingSPID = counters.getValueAt(rowId, pos_BlockingSPID);
			if (o_BlockingSPID instanceof Number)
			{
				Number thisRow = (Number)o_BlockingSPID;
				if (thisRow.intValue() == spid)
				{
					if (spidSet == null)
						spidSet = new LinkedHashSet<Integer>();

					Object o_SPID = counters.getValueAt(rowId, pos_SPID);
					if (o_SPID instanceof Number)
						spidSet.add( ((Number)o_SPID).intValue() );
				}
			}
		}
		if (spidSet == null)
			return "";
		return StringUtil.toCommaStr(spidSet);
	}

	private int getMaxBlockingTimeInSecForSpid(CounterSample counters, int spid, int pos_BlockingSPID, int pos_wait_time)
	{
		int maxBlockingTimeInSec = 0;

		// Loop on all diffData rows
		int rows = counters.getRowCount();
		for (int rowId=0; rowId < rows; rowId++)
		{
			Object o_BlockingSPID = counters.getValueAt(rowId, pos_BlockingSPID);
			if (o_BlockingSPID instanceof Number)
			{
				Number BlockingSPID = (Number)o_BlockingSPID;
				if (BlockingSPID.intValue() == spid)
				{
//					Object o_SPID      = counters.getValueAt(rowId, pos_SPID);
					Object o_wait_time = counters.getValueAt(rowId, pos_wait_time);
					if (o_wait_time instanceof Number)
					{
						Number wait_time = (Number)o_wait_time;
						int SecondsWaiting = wait_time.intValue() / 1000;
						
						maxBlockingTimeInSec = Math.max(SecondsWaiting, maxBlockingTimeInSec);
					}
				}
			}
		}
		
		return maxBlockingTimeInSec;
	}

	private String getBlockedSpidInfo(CounterSample counters, int pos_SPID, List<String> blockedSpidList, int pos_MonSqlText, int pos_ShowPlanText, int pos_LiveQueryPlan, int pos_SpidLocks)
	{
		if (blockedSpidList == null)   return "";
		if (blockedSpidList.isEmpty()) return "";

		if (pos_SPID         < 0) return "";
		if (pos_MonSqlText   < 0) return "";
		if (pos_SpidLocks    < 0) return "";
		if (pos_ShowPlanText < 0) return "";


		StringBuilder sb = new StringBuilder(1024);

		sb.append("<TABLE BORDER=1 class='dbx-table-basic'>\n");
//		sb.append("  <TR> <TH>Blocked SPID</TH> <TH>MonSqlText</TH> <TH>LockInfo</TH> <TH>XML Showplan</TH> </TR>\n");
		sb.append("  <TR> <TH>Blocked SPID</TH> <TH>MonSqlText</TH> <TH>LockInfo</TH> <TH>ExtraInfo</TH> </TR>\n");
		
		int addCount = 0;
		
		// Loop the blockedSpidList
		for (String blockedSpidStr : blockedSpidList)
		{
			int blockedSpid = StringUtil.parseInt(blockedSpidStr, Integer.MIN_VALUE);
			if (blockedSpid == Integer.MIN_VALUE)
				continue;
			
			int rowId = getRowIdForSpid(counters, blockedSpid, pos_SPID);

			if (rowId != -1)
			{
				addCount++;
				
				Object o_monSqlText = pos_MonSqlText    == -1 ? null : counters.getValueAt(rowId, pos_MonSqlText);
				Object o_lockInfo   = pos_SpidLocks     == -1 ? null : counters.getValueAt(rowId, pos_SpidLocks);
				Object o_liveQp     = pos_LiveQueryPlan == -1 ? null : counters.getValueAt(rowId, pos_LiveQueryPlan);
////				Object o_showplan   = pos_ShowPlanText  == -1 ? null : counters.getValueAt(rowId, pos_ShowPlanText); // this is TO BIG... can we convert a XML to "simple" plan instead?
//				Object o_showplan   = null;
				
				String monSqlText = o_monSqlText == null ? "" : o_monSqlText.toString();
				String lockInfo   = o_lockInfo   == null ? "" : o_lockInfo  .toString();
//				String liveQp     = o_liveQp     == null ? "" : o_liveQp    .toString();
//				String showplan   = o_showplan   == null ? "" : o_showplan  .toString();

				// Get extra info in 1 cell:
				//  - login_name 
				//  - command 
				//  - tran_begin_time
				//  - tran_name 
				//  - ProcName 
				//  - HOST_NAME 
				//  - SpidLockCount 
				//  - database_name 
				//  - program_name 
				//  - context_info_str 
				//  - wait_time 
				//  - wait_resource 
				String extraInfo = "<TABLE BORDER=0 CELLSPACING=0 CELLPADDING=1 class='dbx-table-basic'>";
				if (counters.findColumn("login_name")       != -1) extraInfo += "<TR> <TD><B>login_name      </B></TD> <TD>" + counters.getValueAsString(rowId, "login_name"      ) + "</TD> </TR>";
				if (counters.findColumn("program_name")     != -1) extraInfo += "<TR> <TD><B>program_name    </B></TD> <TD>" + counters.getValueAsString(rowId, "program_name"    ) + "</TD> </TR>";
				if (counters.findColumn("context_info_str") != -1) extraInfo += "<TR> <TD><B>context_info_str</B></TD> <TD>" + counters.getValueAsString(rowId, "context_info_str") + "</TD> </TR>";
				if (counters.findColumn("HOST_NAME")        != -1) extraInfo += "<TR> <TD><B>HOST_NAME       </B></TD> <TD>" + counters.getValueAsString(rowId, "HOST_NAME"       ) + "</TD> </TR>";
				if (counters.findColumn("ProcName")         != -1) extraInfo += "<TR> <TD><B>ProcName        </B></TD> <TD>" + counters.getValueAsString(rowId, "ProcName"        ) + "</TD> </TR>";
				if (counters.findColumn("StmntStart")       != -1) extraInfo += "<TR> <TD><B>StmntStart      </B></TD> <TD>" + counters.getValueAsString(rowId, "StmntStart"      ) + "</TD> </TR>";

				if (counters.findColumn("command")          != -1) extraInfo += "<TR> <TD><B>command         </B></TD> <TD>" + counters.getValueAsString(rowId, "command"         ) + "</TD> </TR>";
				if (counters.findColumn("tran_begin_time")  != -1) extraInfo += "<TR> <TD><B>tran_begin_time </B></TD> <TD>" + counters.getValueAsString(rowId, "tran_begin_time" ) + "</TD> </TR>";
				if (counters.findColumn("tran_name")        != -1) extraInfo += "<TR> <TD><B>tran_name       </B></TD> <TD>" + counters.getValueAsString(rowId, "tran_name"       ) + "</TD> </TR>";
				if (counters.findColumn("SpidLockCount")    != -1) extraInfo += "<TR> <TD><B>SpidLockCount   </B></TD> <TD>" + counters.getValueAsString(rowId, "SpidLockCount"   ) + "</TD> </TR>";
				if (counters.findColumn("database_name")    != -1) extraInfo += "<TR> <TD><B>database_name   </B></TD> <TD>" + counters.getValueAsString(rowId, "database_name"   ) + "</TD> </TR>";
				if (counters.findColumn("wait_time")        != -1) extraInfo += "<TR> <TD><B>wait_time       </B></TD> <TD>" + counters.getValueAsString(rowId, "wait_time"       ) + "</TD> </TR>";
				if (counters.findColumn("wait_resource")    != -1) extraInfo += "<TR> <TD><B>wait_resource   </B></TD> <TD>" + counters.getValueAsString(rowId, "wait_resource"   ) + "</TD> </TR>";
				extraInfo += "</TABLE>\n";
				

//				// Prefer a LIVE Plan before a "long" XML Plan
//				String finalShowPlan = showplan;
//				if (StringUtil.hasValue(liveQp))
//					finalShowPlan = liveQp;
//
//				boolean skipLongShowplans     = Configuration.getCombinedConfiguration().getBooleanProperty("CmActiveStatements.longExecutionPlans.skip", true);
//				int     skipLongShowplansSize = Configuration.getCombinedConfiguration().getIntProperty    ("CmActiveStatements.longExecutionPlans.size", 8192);
//				if (skipLongShowplans && finalShowPlan.length() > skipLongShowplansSize)
//					finalShowPlan = "TODO: Rewrite long XML plan to short version...";
				
				if (monSqlText.startsWith("<html>") && monSqlText.endsWith("</html>"))
				{
					monSqlText = monSqlText.substring("<html>".length());
					monSqlText = monSqlText.substring(0, monSqlText.length() - "</html>".length());
				}

				if (lockInfo.startsWith("<html>") && lockInfo.endsWith("</html>"))
				{
					lockInfo = lockInfo.substring("<html>".length());
					lockInfo = lockInfo.substring(0, lockInfo.length() - "</html>".length());
				}

//				if (finalShowPlan.startsWith("<html>") && finalShowPlan.endsWith("</html>"))
//				{
//					finalShowPlan = finalShowPlan.substring("<html>".length());
//					finalShowPlan = finalShowPlan.substring(0, finalShowPlan.length() - "</html>".length());
//				}

				sb.append("  <TR>\n");
				sb.append("    <TD><B>")  .append(blockedSpid  ).append("</B></TD>\n");
				sb.append("    <TD>"   )  .append(monSqlText   ).append("</TD>\n");
				sb.append("    <TD>"   )  .append(lockInfo     ).append("</TD>\n");
				sb.append("    <TD>"   )  .append(extraInfo    ).append("</TD>\n");
//				sb.append("    <TD><xmp>").append(showplan     ).append("</xmp></TD>\n");
//				sb.append("    <TD><xmp>").append(finalShowPlan).append("</xmp></TD>\n");
				sb.append("  </TR>\n");
			}
		}

		sb.append("</TABLE>\n");
		
		if (addCount == 0)
			return "";

		return sb.toString();
	}
	
	private int getRowIdForSpid(CounterSample counters, int spidToFind, int pos_SPID)
	{
		// Loop on all diffData rows
		int rows = counters.getRowCount();
		for (int rowId=0; rowId < rows; rowId++)
		{
			Object o_SPID = counters.getValueAt(rowId, pos_SPID);
			if (o_SPID instanceof Number)
			{
				Number SPID = (Number)o_SPID;
				if (SPID.intValue() == spidToFind)
				{
					return rowId;
				}
			}
		}
		return -1;
	}
	
//	/** 
//	 * Get number of rows to save/request ddl information for 
//	 */
//	@Override
//	public int getMaxNumOfDdlsToPersist()
//	{
//		return Integer.MAX_VALUE; // Basically ALL Rows
//	}
//
//	/** 
//	 * Get Column names to where DBName and ObjectName is called, this must always return at least a array with 2 strings. 
//	 */
//	@Override
//	public String[] getDdlDetailsColNames()
//	{
//		String[] sa = {"dbname", "procname"};
//		return sa;
//	}
	
	


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

		for (int r=0; r<cm.getRateRowCount(); r++)
		{
			//-------------------------------------------------------
			// ImBlockingOthersMaxTimeInSec 
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("ImBlockingOthersMaxTimeInSec"))
			{
				Object o_ImBlockedBySessionId         = cm.getRateValue(r, "ImBlockedBySessionId");
				Object o_ImBlockingOthersMaxTimeInSec = cm.getRateValue(r, "ImBlockingOthersMaxTimeInSec");

				if (    (o_ImBlockingOthersMaxTimeInSec != null && o_ImBlockingOthersMaxTimeInSec instanceof Number)
				     && (o_ImBlockedBySessionId         != null && o_ImBlockedBySessionId         instanceof Number) )
				{
					int ImBlockedBySessionId         = ((Number)o_ImBlockedBySessionId        ).intValue();
					int ImBlockingOthersMaxTimeInSec = ((Number)o_ImBlockingOthersMaxTimeInSec).intValue();
					
					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_ImBlockingOthersMaxTimeInSec, DEFAULT_alarm_ImBlockingOthersMaxTimeInSec);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): ImBlockedBySessionId="+ImBlockedBySessionId+"; ImBlockingOthersMaxTimeInSec='"+ImBlockingOthersMaxTimeInSec+"', threshold="+threshold+".");

					if (ImBlockedBySessionId == 0) // meaning: THIS SPID is responsible for the block (it's not blocked, meaning; the root cause)
					{
						List<String> ImBlockingOtherSessionIdsList = StringUtil.commaStrToList(cm.getRateValue(r, "ImBlockingOtherSessionIds") + "");
						String BlockingOtherSpidsStr = ImBlockingOtherSessionIdsList + "";
						int    blockCount            = ImBlockingOtherSessionIdsList.size();
						int    spid                  = cm.getRateValueAsDouble(r, "session_id").intValue();

						if (debugPrint || _logger.isDebugEnabled())
							System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", ImBlockingOthersMaxTimeInSec='"+ImBlockingOthersMaxTimeInSec+"', ImBlockingOtherSessionIdsList="+ImBlockingOtherSessionIdsList);

						if (ImBlockingOthersMaxTimeInSec > threshold)
						{
							// Get data for 'skip names'
							String rowUsername = cm.getAbsString(r, "login_name");
							String rowHostname = cm.getAbsString(r, "HOST_NAME");
							String rowDbname   = cm.getAbsString(r, "database_name");
							String rowProgname = cm.getAbsString(r, "program_name");
							String rowProcname = cm.getAbsString(r, "ProcName");
							
							// Get config 'skip names'
							String skipUsernameRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_ImBlockingOthersMaxTimeInSecSkipUsername, DEFAULT_alarm_ImBlockingOthersMaxTimeInSecSkipUsername);
							String skipHostnameRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_ImBlockingOthersMaxTimeInSecSkipHostname, DEFAULT_alarm_ImBlockingOthersMaxTimeInSecSkipHostname);
							String skipDbnameRegExp   = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_ImBlockingOthersMaxTimeInSecSkipDbname  , DEFAULT_alarm_ImBlockingOthersMaxTimeInSecSkipDbname  );
							String skipPrognameRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_ImBlockingOthersMaxTimeInSecSkipProgname, DEFAULT_alarm_ImBlockingOthersMaxTimeInSecSkipProgname);
							String skipProcnameRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_ImBlockingOthersMaxTimeInSecSkipProcname, DEFAULT_alarm_ImBlockingOthersMaxTimeInSecSkipProcname);

							// note: this must be set to true at start, otherwise all below rules will be disabled (it "stops" processing at first doAlarm==false)
							boolean doAlarm = true;
							
							// The below could have been done with nested if(skip-name), if(skip-prog), if(skip-user), if(skip-host) doAlarm=true; 
							// Below is more readable, from a variable context point-of-view, but HARDER to understand
							// to *continue*: doAlarm needs to be true AND (regExp is empty OR not-matching)
							doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipUsernameRegExp) || ! rowUsername.matches(skipUsernameRegExp)));
							doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipHostnameRegExp) || ! rowHostname.matches(skipHostnameRegExp)));
							doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipDbnameRegExp  ) || ! rowDbname  .matches(skipDbnameRegExp  )));
							doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipPrognameRegExp) || ! rowProgname.matches(skipPrognameRegExp)));
							doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipProcnameRegExp) || ! rowProcname.matches(skipProcnameRegExp)));

							if (doAlarm)
							{
								String extendedDescText = cm.toTextTableString(DATA_RATE, r);
								String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);

								AlarmEvent ae = new AlarmEventBlockingLockAlarm(cm, threshold, spid, ImBlockingOthersMaxTimeInSec, BlockingOtherSpidsStr, blockCount);

								ae.setExtendedDescription(extendedDescText, extendedDescHtml);
								
								alarmHandler.addAlarm( ae );
							}
						}
					}
				}
			}

			//-------------------------------------------------------
			// HoldingLocksWhileWaitForClientInputInSec 
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("HoldingLocksWhileWaitForClientInputInSec"))
			{
				Object o_monSource    = cm.getRateValue(r, "monSource");
				Object o_ExecTimeInMs = cm.getRateValue(r, "ExecTimeInMs");

				if ("HOLDING-LOCKS".equals(o_monSource) && (o_ExecTimeInMs != null && o_ExecTimeInMs instanceof Number) )
				{
					int ExecTimeInSec = ((Number)o_ExecTimeInMs).intValue() / 1000;
					
					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSec, DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSec);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", ExecTimeInSec='"+ExecTimeInSec+"'.");

					if (ExecTimeInSec > threshold)
					{
						int    spid      = cm.getRateValueAsDouble(r, "session_id") .intValue();
						String startTime = cm.getRateValue        (r, "start_time") + "";
						String spidLocks = cm.getRateValue        (r, "SpidLocks")  + "";

						// only alarm if it has "Exclusive" or "Update" locks
						boolean hasExlusiveLocks = (spidLocks.contains("Exclusive") || spidLocks.contains("Update"));

						boolean onlySendAlarmForExclusiveLocks = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSecX, DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSecX);

						if ( (onlySendAlarmForExclusiveLocks && hasExlusiveLocks) || onlySendAlarmForExclusiveLocks == false )
						{
							// Get data for 'skip names'
							String rowUsername = cm.getAbsString(r, "login_name");
							String rowHostname = cm.getAbsString(r, "HOST_NAME");
							String rowDbname   = cm.getAbsString(r, "database_name");
							String rowProgname = cm.getAbsString(r, "program_name");
							String rowProcname = cm.getAbsString(r, "ProcName");
							
							// Get config 'skip names'
							String skipUsernameRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSecSkipUsername, DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSecSkipUsername);
							String skipHostnameRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSecSkipHostname, DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSecSkipHostname);
							String skipDbnameRegExp   = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSecSkipDbname  , DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSecSkipDbname  );
							String skipPrognameRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSecSkipProgname, DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSecSkipProgname);
							String skipProcnameRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSecSkipProcname, DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSecSkipProcname);

							// note: this must be set to true at start, otherwise all below rules will be disabled (it "stops" processing at first doAlarm==false)
							boolean doAlarm = true;
							
							// The below could have been done with nested if(skip-name), if(skip-prog), if(skip-user), if(skip-host) doAlarm=true; 
							// Below is more readable, from a variable context point-of-view, but HARDER to understand
							// to *continue*: doAlarm needs to be true AND (regExp is empty OR not-matching)
							doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipUsernameRegExp) || ! rowUsername.matches(skipUsernameRegExp)));
							doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipHostnameRegExp) || ! rowHostname.matches(skipHostnameRegExp)));
							doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipDbnameRegExp  ) || ! rowDbname  .matches(skipDbnameRegExp  )));
							doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipPrognameRegExp) || ! rowProgname.matches(skipPrognameRegExp)));
							doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipProcnameRegExp) || ! rowProcname.matches(skipProcnameRegExp)));

							if (doAlarm)
							{
								String extendedDescText = cm.toTextTableString(DATA_RATE, r);
								String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);

								AlarmEvent ae = new AlarmEventHoldingLocksWhileWaitForClientInput(cm, threshold, spid, ExecTimeInSec, startTime, hasExlusiveLocks);
								
								ae.setExtendedDescription(extendedDescText, extendedDescHtml);
								
								alarmHandler.addAlarm( ae );
							}
						}
					}
				}
			}

			//-------------------------------------------------------
			// TempdbUsageMb 
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("TempdbUsageMb"))
			{
				Object o_TempdbUsageMb = cm.getRateValue(r, "TempdbUsageMb");
				if (o_TempdbUsageMb != null && o_TempdbUsageMb instanceof Number)
				{
					int TempdbUsageMb = ((Number)o_TempdbUsageMb).intValue();
					
					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_TempdbUsageMb, DEFAULT_alarm_TempdbUsageMb);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", TempdbUsageMb='"+TempdbUsageMb);

					if (TempdbUsageMb > threshold)
					{
						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);

						AlarmEvent ae = new AlarmEventExtensiveUsage(cm, threshold, AlarmEventExtensiveUsage.Resource.TEMPDB, "TempdbUsageMb", TempdbUsageMb);

						ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
						alarmHandler.addAlarm( ae );
					}
				}
			}
		} // end: loop rows
	}

	public static final String  PROPKEY_alarm_ImBlockingOthersMaxTimeInSec                          = CM_NAME + ".alarm.system.if.ImBlockingOthersMaxTimeInSec.gt";
	public static final int     DEFAULT_alarm_ImBlockingOthersMaxTimeInSec                          = 60;

	public static final String  PROPKEY_alarm_ImBlockingOthersMaxTimeInSecSkipUsername              = CM_NAME + ".alarm.system.if.ImBlockingOthersMaxTimeInSec.skip.username";
	public static final String  DEFAULT_alarm_ImBlockingOthersMaxTimeInSecSkipUsername              = "";

	public static final String  PROPKEY_alarm_ImBlockingOthersMaxTimeInSecSkipHostname              = CM_NAME + ".alarm.system.if.ImBlockingOthersMaxTimeInSec.skip.hostname";
	public static final String  DEFAULT_alarm_ImBlockingOthersMaxTimeInSecSkipHostname              = "";

	public static final String  PROPKEY_alarm_ImBlockingOthersMaxTimeInSecSkipDbname                = CM_NAME + ".alarm.system.if.ImBlockingOthersMaxTimeInSec.skip.dbname";
	public static final String  DEFAULT_alarm_ImBlockingOthersMaxTimeInSecSkipDbname                = "";

	public static final String  PROPKEY_alarm_ImBlockingOthersMaxTimeInSecSkipProgname              = CM_NAME + ".alarm.system.if.ImBlockingOthersMaxTimeInSec.skip.progname";
	public static final String  DEFAULT_alarm_ImBlockingOthersMaxTimeInSecSkipProgname              = "Report Server";

	public static final String  PROPKEY_alarm_ImBlockingOthersMaxTimeInSecSkipProcname              = CM_NAME + ".alarm.system.if.ImBlockingOthersMaxTimeInSec.skip.procname";
	public static final String  DEFAULT_alarm_ImBlockingOthersMaxTimeInSecSkipProcname              = "";

	
	public static final String  PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSec              = CM_NAME + ".alarm.system.if.HoldingLocksWhileWaitForClientInputInSec.gt";
	public static final int     DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSec              = 180; // 3 minutes

	public static final String  PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSecX             = CM_NAME + ".alarm.system.if.HoldingLocksWhileWaitForClientInputInSec.exclusiveLocksOnly";
	public static final boolean DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSecX             = true;
	
	public static final String  PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSecSkipUsername  = CM_NAME + ".alarm.system.if.HoldingLocksWhileWaitForClientInputInSec.skip.username";
	public static final String  DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSecSkipUsername  = "";

	public static final String  PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSecSkipHostname  = CM_NAME + ".alarm.system.if.HoldingLocksWhileWaitForClientInputInSec.skip.hostname";
	public static final String  DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSecSkipHostname  = "";

	public static final String  PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSecSkipDbname    = CM_NAME + ".alarm.system.if.HoldingLocksWhileWaitForClientInputInSec.skip.dbname";
	public static final String  DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSecSkipDbname    = "";

	public static final String  PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSecSkipProgname  = CM_NAME + ".alarm.system.if.HoldingLocksWhileWaitForClientInputInSec.skip.progname";
	public static final String  DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSecSkipProgname  = "Report Server";

	public static final String  PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSecSkipProcname  = CM_NAME + ".alarm.system.if.HoldingLocksWhileWaitForClientInputInSec.skip.procname";
	public static final String  DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSecSkipProcname  = "";

	
	public static final String  PROPKEY_alarm_TempdbUsageMb                                         = CM_NAME + ".alarm.system.if.TempdbUsageMb.gt";
	public static final int     DEFAULT_alarm_TempdbUsageMb                                         = 16384; // 16 GB ... which is pretty much
	
	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		
		list.add(new CmSettingsHelper("ImBlockingOthersMaxTimeInSec"                , isAlarmSwitch, PROPKEY_alarm_ImBlockingOthersMaxTimeInSec                         , Integer.class, conf.getIntProperty    (PROPKEY_alarm_ImBlockingOthersMaxTimeInSec                         , DEFAULT_alarm_ImBlockingOthersMaxTimeInSec                         ), DEFAULT_alarm_ImBlockingOthersMaxTimeInSec                         , "If 'ImBlockingOthersMaxTimeInSec' is greater than ## then send 'AlarmEventBlockingLockAlarm'." ));
		list.add(new CmSettingsHelper("ImBlockingOthersMaxTimeInSec SkipUsername",                   PROPKEY_alarm_ImBlockingOthersMaxTimeInSecSkipUsername             , String .class, conf.getProperty       (PROPKEY_alarm_ImBlockingOthersMaxTimeInSecSkipUsername             , DEFAULT_alarm_ImBlockingOthersMaxTimeInSecSkipUsername             ), DEFAULT_alarm_ImBlockingOthersMaxTimeInSecSkipUsername             , "If 'ImBlockingOthersMaxTimeInSec' is true; then we can filter out user      names using a Regular expression... if (name.matches('regexp'))... This to remove alarms of '(user1|user2)'  or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("ImBlockingOthersMaxTimeInSec SkipHostname",                   PROPKEY_alarm_ImBlockingOthersMaxTimeInSecSkipHostname             , String .class, conf.getProperty       (PROPKEY_alarm_ImBlockingOthersMaxTimeInSecSkipHostname             , DEFAULT_alarm_ImBlockingOthersMaxTimeInSecSkipHostname             ), DEFAULT_alarm_ImBlockingOthersMaxTimeInSecSkipHostname             , "If 'ImBlockingOthersMaxTimeInSec' is true; then we can filter out host      names using a Regular expression... if (name.matches('regexp'))... This to remove alarms of '.*(dev|test).*' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("ImBlockingOthersMaxTimeInSec SkipDbname",                     PROPKEY_alarm_ImBlockingOthersMaxTimeInSecSkipDbname               , String .class, conf.getProperty       (PROPKEY_alarm_ImBlockingOthersMaxTimeInSecSkipDbname               , DEFAULT_alarm_ImBlockingOthersMaxTimeInSecSkipDbname               ), DEFAULT_alarm_ImBlockingOthersMaxTimeInSecSkipDbname               , "If 'ImBlockingOthersMaxTimeInSec' is true; then we can filter out database  names using a Regular expression... if (name.matches('regexp'))... This to remove alarms of '(db1|db2)'      or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("ImBlockingOthersMaxTimeInSec SkipProgname",                   PROPKEY_alarm_ImBlockingOthersMaxTimeInSecSkipProgname             , String .class, conf.getProperty       (PROPKEY_alarm_ImBlockingOthersMaxTimeInSecSkipProgname             , DEFAULT_alarm_ImBlockingOthersMaxTimeInSecSkipProgname             ), DEFAULT_alarm_ImBlockingOthersMaxTimeInSecSkipProgname             , "If 'ImBlockingOthersMaxTimeInSec' is true; then we can filter out program   names using a Regular expression... if (name.matches('regexp'))... This to remove alarms of 'Report Server'  or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("ImBlockingOthersMaxTimeInSec SkipProcname",                   PROPKEY_alarm_ImBlockingOthersMaxTimeInSecSkipProcname             , String .class, conf.getProperty       (PROPKEY_alarm_ImBlockingOthersMaxTimeInSecSkipProcname             , DEFAULT_alarm_ImBlockingOthersMaxTimeInSecSkipProcname             ), DEFAULT_alarm_ImBlockingOthersMaxTimeInSecSkipProcname             , "If 'ImBlockingOthersMaxTimeInSec' is true; then we can filter out procedure names using a Regular expression... if (name.matches('regexp'))... This to remove alarms of '(proc1|proc2)'  or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));

		list.add(new CmSettingsHelper("HoldingLocksWhileWaitForClientInputInSec"    , isAlarmSwitch, PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSec             , Integer.class, conf.getIntProperty    (PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSec             , DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSec             ), DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSec             , "If Client is HOLDING-LOCKS at DBMS and 'ExecTimeInMs' is greater than ## seconds then send 'AlarmEventHoldingLocksWhileWaitForClientInput'." ));
		list.add(new CmSettingsHelper("HoldingLocksWhileWaitForClientInputInSec ExclusiveLocksOnly", PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSecX            , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSecX            , DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSecX            ), DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSecX            , "Only send Alarm AlarmEventHoldingLocksWhileWaitForClientInput, if we are holding any Exlusive Locks" ));
		list.add(new CmSettingsHelper("HoldingLocksWhileWaitForClientInputInSec SkipUsername",       PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSecSkipUsername , String .class, conf.getProperty       (PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSecSkipUsername , DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSecSkipUsername ), DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSecSkipUsername , "If 'HoldingLocksWhileWaitForClientInputInSec' is true; then we can filter out user      names using a Regular expression... if (name.matches('regexp'))... This to remove alarms of '(user1|user2)'  or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("HoldingLocksWhileWaitForClientInputInSec SkipHostname",       PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSecSkipHostname , String .class, conf.getProperty       (PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSecSkipHostname , DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSecSkipHostname ), DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSecSkipHostname , "If 'HoldingLocksWhileWaitForClientInputInSec' is true; then we can filter out host      names using a Regular expression... if (name.matches('regexp'))... This to remove alarms of '.*(dev|test).*' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("HoldingLocksWhileWaitForClientInputInSec SkipDbname",         PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSecSkipDbname   , String .class, conf.getProperty       (PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSecSkipDbname   , DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSecSkipDbname   ), DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSecSkipDbname   , "If 'HoldingLocksWhileWaitForClientInputInSec' is true; then we can filter out database  names using a Regular expression... if (name.matches('regexp'))... This to remove alarms of '(db1|db2)'      or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("HoldingLocksWhileWaitForClientInputInSec SkipProgname",       PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSecSkipProgname , String .class, conf.getProperty       (PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSecSkipProgname , DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSecSkipProgname ), DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSecSkipProgname , "If 'HoldingLocksWhileWaitForClientInputInSec' is true; then we can filter out program   names using a Regular expression... if (name.matches('regexp'))... This to remove alarms of 'Report Server'  or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("HoldingLocksWhileWaitForClientInputInSec SkipProcname",       PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSecSkipProcname , String .class, conf.getProperty       (PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSecSkipProcname , DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSecSkipProcname ), DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSecSkipProcname , "If 'HoldingLocksWhileWaitForClientInputInSec' is true; then we can filter out procedure names using a Regular expression... if (name.matches('regexp'))... This to remove alarms of '(proc1|proc2)'  or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));

		list.add(new CmSettingsHelper("TempdbUsageMb"                               , isAlarmSwitch, PROPKEY_alarm_TempdbUsageMb                                        , Integer.class, conf.getIntProperty    (PROPKEY_alarm_TempdbUsageMb                                        , DEFAULT_alarm_TempdbUsageMb                                        ), DEFAULT_alarm_TempdbUsageMb                                        , "If 'TempdbUsageMb' is greater than ## then send 'AlarmEventExtensiveUsage'." ));

		return list;
	}

}
