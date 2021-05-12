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

import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.ICounterController.DbmsOption;
import com.asetune.IGuiController;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.AlarmEventBlockingLockAlarm;
import com.asetune.alarm.events.AlarmEventExtensiveUsage;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.sqlserver.gui.CmActiveStatementsPanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.pcs.PcsColumnOptions;
import com.asetune.pcs.PcsColumnOptions.ColumnType;
import com.asetune.utils.Configuration;
import com.asetune.utils.SqlServerUtils;
import com.asetune.utils.SqlServerUtils.LockRecord;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmActiveStatements
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmActiveStatements.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmActiveStatements.class.getSimpleName();
	public static final String   SHORT_NAME       = "Active Statements";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>FIXME</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_exec_sessions", "dm_exec_requests", "dm_exec_connections", "dm_exec_sql_text", "dm_exec_query_plan"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] { "percent_complete" };
	public static final String[] DIFF_COLUMNS     = new String[] {
		"exec_cpu_time",
		"exec_reads",
		"exec_logical_reads",
		"exec_writes",
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
		super(counterController,
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

	public static final String  PROPKEY_sample_spidLocks          = PROP_PREFIX + ".sample.spidLocks";
	public static final boolean DEFAULT_sample_spidLocks          = true;

	
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
		setLocalToolTipTextOnTableColumnHeader("HasSqlText",                "<html>Checkbox to indicate that 'lastKnownSql' column has a value<br><b>Note:</b> Hower over this cell to see the SQL Statement.</html>");
		setLocalToolTipTextOnTableColumnHeader("HasQueryplan",              "<html>Checkbox to indicate that 'query_plan' column has a value<br><b>Note:</b> Hower over this cell to see the Query plan.</html>");
		setLocalToolTipTextOnTableColumnHeader("HasLiveQueryplan",          "<html>Checkbox to indicate that 'live_query_plan' column has a value<br><b>Note:</b> Hower over this cell to see the Query plan.</html>");
		setLocalToolTipTextOnTableColumnHeader("ExecTimeInMs",              "<html>How many milliseconds has this session_id been executing the current SQL Statement</html>");
		setLocalToolTipTextOnTableColumnHeader("UsefullExecTime",           "<html>More or less same thing as the column 'ExecTimeInMs' but it subtracts the 'wait_time'...<br>The idea is to display <i>time</i> used on something <i>usefull</i>, e.g. Not in sleep mode.</html>");
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

		setLocalToolTipTextOnTableColumnHeader("TempdbUsageMb",              "Number of MB this Statement is using in tempdb.");
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isAzure)
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
			map.put("lastKnownSql", new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));
			map.put("query_plan"  , new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));

			// Set the map in the super
			setPcsColumnOptions(map);
		}

		return map;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("session_id");
		pkCols.add("monSource");     // the "spid" can be in both ACTIVE and BLOCKER section

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		String dm_exec_sessions    = "dm_exec_sessions";
		String dm_exec_requests    = "dm_exec_requests";
//		String dm_exec_connections = "dm_exec_connections";
		String dm_exec_sql_text    = "dm_exec_sql_text";
		String dm_exec_query_plan  = "dm_exec_query_plan";

		// get Actual-Query-Plan instead of Estimated-QueryPlan
		if (isDbmsOptionEnabled(DbmsOption.SQL_SERVER__LAST_QUERY_PLAN_STATS))
		{
			dm_exec_query_plan = "dm_exec_query_plan_stats";
		}

		if (isAzure)
		{
			dm_exec_sessions    = "dm_pdw_nodes_exec_sessions";
			dm_exec_requests    = "dm_exec_requests";            // SAME NAME IN AZURE ????
//			dm_exec_connections = "dm_pdw_exec_connections";
			dm_exec_sql_text    = "dm_exec_sql_text";            // SAME NAME IN AZURE ????
			dm_exec_query_plan  = "dm_exec_query_plan";            // SAME NAME IN AZURE ????
		}

//		String LiveQueryPlanActive  = "";
//		String LiveQueryPlanBlocked = "";

//		String LiveQueryPlanActive  = "    ,LiveQueryPlan = convert(nvarchar(max), 'No live query plan available. Only in 2016 SP1 and above')";
//		String LiveQueryPlanBlocked = "    ,LiveQueryPlan = convert(nvarchar(max), 'No live query plan available. Only in 2016 SP1 and above')";
		String LiveQueryPlanActive  = "    ,LiveQueryPlan = convert(nvarchar(max), null) \n";
		String LiveQueryPlanBlocked = "    ,LiveQueryPlan = convert(nvarchar(max), null) \n";
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
		
		String user_objects_deferred_dealloc_page_count = "0";
		if (srvVersion >= Ver.ver(2014))
		{
			user_objects_deferred_dealloc_page_count = "user_objects_deferred_dealloc_page_count";			
		}
		
		String whereIsUserSpid  = "  AND des.is_user_process = 1 \n";
		if (Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sample_systemSpids, DEFAULT_sample_systemSpids))
		{
			whereIsUserSpid = "  AND (des.is_user_process = 1 OR (des.is_user_process = 0 AND des.status != 'sleeping')) \n ";
		}
		
		String sql1 =
			"SELECT  \n" +
			"     monSource    = convert(varchar(10), 'ACTIVE') \n" +
			"    ,multiSampled = convert(varchar(10), '') \n" +
			"    ,des.login_name \n" +
			"    ,des.session_id \n" +
			dop_active +
			"    ,ImBlockedBySessionId = der.blocking_session_id \n" +
			"    ,ImBlockingOtherSessionIds    = convert(varchar(512), '') \n" +
			"    ,ImBlockingOthersMaxTimeInSec = convert(int, 0) \n" +
			"    ,des.status \n" +
			"    ,der.command \n" +
			"    ,ProcName            = object_name(dest.objectid, dest.dbid) \n" +
			"    ,StmntStart          = der.statement_start_offset \n" +
			"    ,des.[HOST_NAME] \n" +
			"    ,TempdbUsageMb       = (select \n" + 
			"                            CAST( ( \n" + // The below calculations also used in: CmTempdbSpidUsage
			"                                        (user_objects_alloc_page_count - user_objects_dealloc_page_count - " + user_objects_deferred_dealloc_page_count + ") \n" + 
			"                                      + (internal_objects_alloc_page_count - internal_objects_dealloc_page_count) \n" + 
			"                                  ) / 128.0 AS decimal(12,1) \n" + 
			"                                ) \n" + 
			"                            from tempdb.sys.dm_db_session_space_usage ts \n" + 
			"                            where ts.session_id = des.session_id \n" +
			"                           ) \n" +
			"    ,HasSqlText          = convert(bit,0) \n" +
			"    ,HasQueryplan        = convert(bit,0) \n" +
			"    ,HasLiveQueryplan    = convert(bit,0) \n" +
			"    ,HasSpidLocks        = convert(bit,0) \n" +
			"    ,HasBlockedSpidsInfo = convert(bit,0) \n" +
			"    ,SpidLockCount       = convert(int,-1) \n" +
//			"    ,DB_NAME(der.database_id) AS database_name \n" +
			"    ,(select db.name from sys.databases db where db.database_id = der.database_id) AS database_name \n" +
			"    ,exec_cpu_time       = der.cpu_time \n" +
			"    ,exec_reads          = der.reads \n" +
			"    ,exec_logical_reads  = der.logical_reads \n" +
			"    ,exec_writes         = der.writes \n" +
			"    ,sess_cpu_time       = des.cpu_time \n" +
			"    ,sess_reads          = des.reads \n" +
			"    ,sess_logical_reads  = des.logical_reads \n" +
			"    ,sess_writes         = des.writes \n" +
//			"    ,dec.last_write \n" +
			"    ,der.start_time \n" +
			"    ,ExecTimeInMs        = CASE WHEN datediff(day, der.start_time, getdate()) >= 24 THEN -1 ELSE  datediff(ms, der.start_time, getdate()) END \n" +               // protect from: Msg 535: Difference of two datetime fields caused overflow at runtime. above 24 days or so, the MS difference is overflowned
			"    ,UsefullExecTime     = CASE WHEN datediff(day, der.start_time, getdate()) >= 24 THEN -1 ELSE (datediff(ms, der.start_time, getdate()) - der.wait_time) END \n" + // protect from: Msg 535: Difference of two datetime fields caused overflow at runtime. above 24 days or so, the MS difference is overflowned
			"    ,des.[program_name] \n" +
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
//			"    ,OBJECT_NAME(dest.objectid, der.database_id) AS OBJECT_NAME \n" +
			"    ,SUBSTRING(dest.text, der.statement_start_offset / 2,  \n" +
			"        ( CASE WHEN der.statement_end_offset = -1  \n" +
			"               THEN DATALENGTH(dest.text)  \n" +
			"               ELSE der.statement_end_offset  \n" +
			"          END - der.statement_start_offset ) / 2 +2) AS [lastKnownSql] \n" +
			"    ,SpidLocks        = convert(varchar(max),null) \n" +
			"    ,BlockedSpidsInfo = convert(varchar(max),null) \n" +
			"    ,deqp.query_plan \n" +
			LiveQueryPlanActive +
			"FROM sys." + dm_exec_sessions + " des \n" +
			"JOIN sys." + dm_exec_requests + " der ON des.session_id = der.session_id \n" +
//			"LEFT JOIN sys." + dm_exec_connections + " dec ON des.session_id = dec.session_id \n" +
			"OUTER APPLY sys." + dm_exec_sql_text + "(der.sql_handle) dest \n" +
			"OUTER APPLY sys." + dm_exec_query_plan + "(der.plan_handle) deqp \n" +
			"WHERE des.session_id != @@spid \n " +
			whereIsUserSpid +
			"  AND (der.executing_managed_code = 0 OR (der.executing_managed_code = 1 AND der.wait_type != 'SLEEP_TASK')) \n" + // SSIS seems to be executing ALL THE TIME... so discard some of them... (this may be unique to MaxM)
			"";

		String sql2 = 
			"SELECT  \n" +
			"     monSource    = convert(varchar(10), 'BLOCKER')  \n" +
			"    ,multiSampled = convert(varchar(10), '')  \n" +
			"    ,p1.loginame                                  --des.login_name \n" +
			"    ,p1.spid                                      --des.session_id \n" +
			dop_locks +
			"    ,ImBlockedBySessionId = p1.blocked            --der.blocking_session_id \n" +
			"    ,ImBlockingOtherSessionIds    = convert(varchar(512), '')  \n" +
			"    ,ImBlockingOthersMaxTimeInSec = convert(int, 0) " +
			"    ,p1.status                                    --des.status \n" +
			"    ,p1.cmd                                       --der.command \n" +
			"    ,ProcName            = object_name(dest.objectid, dest.dbid) \n" +
			"    ,StmntStart          = -1 \n" +
			"    ,p1.hostname                                  --des.[HOST_NAME] \n" +
			"    ,TempdbUsageMb       = -1 \n" +
			"    ,HasSqlText          = convert(bit,0)  \n" +
			"    ,HasQueryplan        = convert(bit,0)  \n" +
			"    ,HasLiveQueryplan    = convert(bit,0) \n" +
			"    ,HasSpidLocks        = convert(bit,0) \n" +
			"    ,HasBlockedSpidsInfo = convert(bit,0) \n" +
			"    ,SpidLockCount       = convert(int,-1) \n" +
//			"    ,DB_NAME(p1.dbid) AS database_name  \n" +
			"    ,(select db.name from sys.databases db where db.database_id = p1.dbid) AS database_name \n" +
			"    ,p1.cpu                                       --der.cpu_time \n" +
			"    ,0                                            --der.reads   \n" +
			"    ,0                                            --der.logical_reads   \n" +
			"    ,0                                            --der.writes   \n" +
			"    ,0                                            --des.cpu_time \n" +
			"    ,0                                            --des.reads   \n" +
			"    ,0                                            --des.logical_reads   \n" +
			"    ,0                                            --des.writes   \n" +
			"    ,p1.last_batch                                --der.start_time  \n" +
			"    ,ExecTimeInMs    = CASE WHEN datediff(day, p1.last_batch, getdate()) >= 24 THEN -1 ELSE  datediff(ms, p1.last_batch, getdate()) END  \n" +
			"    ,UsefullExecTime = CASE WHEN datediff(day, p1.last_batch, getdate()) >= 24 THEN -1 ELSE (datediff(ms, p1.last_batch, getdate()) - p1.waittime) END  \n" +
			"    ,p1.program_name                              --des.[program_name] \n" +
			"    ,p1.waittype                                  --der.wait_type \n" +
			"    ,p1.waittime                                  --der.wait_time \n" +
			"    ,p1.waittype                                  --der.last_wait_type \n" +
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
			"    ,is_user_process           = CASE WHEN sid != 0x01 THEN convert(bit, 1) ELSE convert(bit, 0) END \n" +
			"    ,percent_complete          = -1\n" +
			"    ,estimated_completion_time = -1\n" +
			"    ,dest.text \n" +
			"    ,SpidLocks        = convert(varchar(max),null) \n" +
			"    ,BlockedSpidsInfo = convert(varchar(max),null) \n" +
			"    ,''                                           --deqp.query_plan  \n" +
			LiveQueryPlanBlocked +
			"FROM sys.sysprocesses p1 \n" +
			"OUTER APPLY sys." + dm_exec_sql_text + "(p1.sql_handle) dest  \n" +
			"WHERE p1.spid in (select p2.blocked from sys.sysprocesses p2 where p2.blocked > 0) \n" + 
			"";
			

		return 
			sql1 +
			"\n" +
			"UNION ALL \n" +
			"\n" +
			sql2;
	}




	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		if ("lastKnownSql".equals(colName))
		{
			return cellValue == null ? null : toHtmlString( cellValue.toString() );
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
		
		return "<html><pre>" + str + "</pre></html>";
	}

	
	
	
	
	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Sample System SPIDs", PROPKEY_sample_systemSpids  , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_systemSpids  , DEFAULT_sample_systemSpids  ), DEFAULT_sample_systemSpids  , "Sample System SPID's" ));
		list.add(new CmSettingsHelper("Get Query Plan",      PROPKEY_sample_showplan     , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_showplan     , DEFAULT_sample_showplan     ), DEFAULT_sample_showplan     , "Also get queryplan" ));
		list.add(new CmSettingsHelper("Get SQL Text",        PROPKEY_sample_monSqlText   , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_monSqlText   , DEFAULT_sample_monSqlText   ), DEFAULT_sample_monSqlText   , "Also get SQL Text"  ));
		list.add(new CmSettingsHelper("Get Live Query Plan", PROPKEY_sample_liveQueryPlan, Boolean.class, conf.getBooleanProperty(PROPKEY_sample_liveQueryPlan, DEFAULT_sample_liveQueryPlan), DEFAULT_sample_liveQueryPlan, "Also get LIVE queryplan" ));
		list.add(new CmSettingsHelper("Get SPID Locks"     , PROPKEY_sample_spidLocks    , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_spidLocks    , DEFAULT_sample_spidLocks    ), DEFAULT_sample_spidLocks    , "Do 'select <i>someCols</i> from syslockinfo where spid = ?' on every row in the table. This will help us to diagnose what the current SQL statement is locking."));

		return list;
	}

	
	@Override
	public Class<?> getColumnClass(int columnIndex)
	{
		// use CHECKBOX for some columns of type bit/Boolean
		String colName = getColumnName(columnIndex);

		if      ("HasSqlText"         .equals(colName)) return Boolean.class;
		else if ("HasQueryplan"       .equals(colName)) return Boolean.class;
		else if ("HasLiveQueryplan"   .equals(colName)) return Boolean.class;
		else if ("HasSpidLocks"       .equals(colName)) return Boolean.class;
		else if ("HasBlockedSpidsInfo".equals(colName)) return Boolean.class;
		else return super.getColumnClass(columnIndex);
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
		boolean getShowplan       = conf == null ? true : conf.getBooleanProperty(PROPKEY_sample_showplan,       DEFAULT_sample_showplan);
		boolean getMonSqltext     = conf == null ? true : conf.getBooleanProperty(PROPKEY_sample_monSqlText,     DEFAULT_sample_monSqlText);
//		boolean getDbccSqltext    = conf == null ? false: conf.getBooleanProperty(getName()+".sample.dbccSqltext",    false);
//		boolean getProcCallStack  = conf == null ? true : conf.getBooleanProperty(getName()+".sample.procCallStack",  true);
//		boolean getDbccStacktrace = conf == null ? false: conf.getBooleanProperty(getName()+".sample.dbccStacktrace", false);
		boolean getLiveQueryPlan  = conf == null ? true : conf.getBooleanProperty(PROPKEY_sample_liveQueryPlan,  DEFAULT_sample_liveQueryPlan);
		boolean getSpidLocks      = conf == null ? false: conf.getBooleanProperty(PROPKEY_sample_spidLocks,      DEFAULT_sample_spidLocks);

		// Where are various columns located in the Vector 
		int pos_SPID = -1;
//		int pos_WaitEventID         = -1, pos_WaitEventDesc    = -1, pos_WaitClassDesc = -1;
		int pos_HasShowPlan         = -1, pos_ShowPlanText     = -1;
		int pos_HasMonSqlText       = -1, pos_MonSqlText       = -1;
//		int pos_HasDbccSqlText      = -1, pos_DbccSqlText      = -1;
//		int pos_HasProcCallStack    = -1, pos_ProcCallStack    = -1;
//		int pos_HasStacktrace       = -1, pos_DbccStacktrace   = -1;
		int pos_HasLiveQueryPlan    = -1, pos_LiveQueryPlan    = -1;
		int pos_HasSpidLocks        = -1, pos_SpidLocks        = -1, pos_SpidLockCount = -1;
		int pos_HasBlockedSpidsInfo = -1, pos_BlockedSpidsInfo = -1;

		int pos_BlockingOtherSpids = -1, pos_BlockingSPID   = -1;
		int pos_ImBlockingOthersMaxTimeInSec= -1;
		int pos_wait_time          = -1;
		int pos_multiSampled       = -1;
		int pos_StartTime          = -1;
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
			else if (colName.equals("HasSpidLocks"))                 pos_HasSpidLocks               = colId;
			else if (colName.equals("SpidLocks"))                    pos_SpidLocks                  = colId;
			else if (colName.equals("SpidLockCount"))                pos_SpidLockCount              = colId;
			else if (colName.equals("HasBlockedSpidsInfo"))          pos_HasBlockedSpidsInfo        = colId;
			else if (colName.equals("BlockedSpidsInfo"))             pos_BlockedSpidsInfo           = colId;
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
				Object o_this_StartTime = counters  .getValueAt(rowId,       pos_StartTime);
				Object o_prev_StartTime = prevSample.getValueAt(prevPkRowId, pos_StartTime);

				if (o_this_StartTime instanceof Timestamp && o_prev_StartTime instanceof Timestamp)
				{
					if (o_this_StartTime.equals(o_prev_StartTime))
						counters.setValueAt("YES", rowId, pos_multiSampled);
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
					try
					{
						lockList = SqlServerUtils.getLockSummaryForSpid(getCounterController().getMonConnection(), spid);
						spidLocks = SqlServerUtils.getLockSummaryForSpid(lockList, true, false);
						if (spidLocks == null)
							spidLocks = "No Locks found";
					}
					catch (TimeoutException ex)
					{
						spidLocks = "Timeout - when getting lock information";
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
//				counters.setValueAt(new Boolean(b), rowId, pos_HasMonSqlText);
//				counters.setValueAt(monSqlText,     rowId, pos_MonSqlText);
//
//				b = !"This was disabled".equals(dbccSqlText)   && !"Not Available".equals(dbccSqlText)   && !dbccSqlText  .startsWith("User does not have");
//				counters.setValueAt(new Boolean(b), rowId, pos_HasDbccSqlText);
//				counters.setValueAt(dbccSqlText,    rowId, pos_DbccSqlText);
//
//				b = !"This was disabled".equals(procCallStack) && !"Not Available".equals(procCallStack) && !procCallStack.startsWith("User does not have");
//				counters.setValueAt(new Boolean(b), rowId, pos_HasProcCallStack);
//				counters.setValueAt(procCallStack,  rowId, pos_ProcCallStack);
//
//				b = !"This was disabled".equals(showplan)      && !"Not Available".equals(showplan)      && !showplan     .startsWith("User does not have");
//				counters.setValueAt(new Boolean(b), rowId, pos_HasShowPlan);
//				counters.setValueAt(showplan,       rowId, pos_ShowPlanText);
//
//				b = !"This was disabled".equals(stacktrace)    && !"Not Available".equals(stacktrace)    && !stacktrace   .startsWith("User does not have");
//				counters.setValueAt(new Boolean(b), rowId, pos_HasStacktrace);
//				counters.setValueAt(stacktrace,     rowId, pos_DbccStacktrace);

				boolean b;
				Object obj;

				// SQL-Text check box
				obj = counters.getValueAt(rowId, pos_MonSqlText);
				b = (obj != null && obj instanceof String && StringUtil.hasValue((String)obj)); 
				counters.setValueAt(new Boolean(b), rowId, pos_HasMonSqlText);

				// QueryPlan check box
				obj = counters.getValueAt(rowId, pos_ShowPlanText);
				b = (obj != null && obj instanceof String && StringUtil.hasValue((String)obj)); 
				counters.setValueAt(new Boolean(b), rowId, pos_HasShowPlan);

				// LiveQueryPlan check box
				obj = counters.getValueAt(rowId, pos_LiveQueryPlan);
				b = (obj != null && obj instanceof String && StringUtil.hasValue((String)obj)); 
				counters.setValueAt(new Boolean(b), rowId, pos_HasLiveQueryPlan);

				// SPID Locks
				b = !"This was disabled".equals(spidLocks) && !"No Locks found".equals(spidLocks) && !"Timeout - when getting lock information".equals(spidLocks);
                counters.setValueAt(new Boolean(b), rowId, pos_HasSpidLocks);
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

		sb.append("<TABLE BORDER=1>\n");
//		sb.append("  <TR> <TH>Blocked SPID</TH> <TH>MonSqlText</TH> <TH>LockInfo</TH> <TH>XML Showplan</TH> </TR>\n");
		sb.append("  <TR> <TH>Blocked SPID</TH> <TH>MonSqlText</TH> <TH>LockInfo</TH> </TR>\n");
		
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
				Object o_ImBlockingOthersMaxTimeInSec = cm.getRateValue(r, "ImBlockingOthersMaxTimeInSec");
				if (o_ImBlockingOthersMaxTimeInSec != null && o_ImBlockingOthersMaxTimeInSec instanceof Number)
				{
					int ImBlockingOthersMaxTimeInSec = ((Number)o_ImBlockingOthersMaxTimeInSec).intValue();
					
					List<String> ImBlockingOtherSessionIdsList = StringUtil.commaStrToList(cm.getRateValue(r, "ImBlockingOtherSessionIds") + "");
					String BlockingOtherSpidsStr = ImBlockingOtherSessionIdsList + "";
					int    blockCount            = ImBlockingOtherSessionIdsList.size();

					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_ImBlockingOthersMaxTimeInSec, DEFAULT_alarm_ImBlockingOthersMaxTimeInSec);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", ImBlockingOthersMaxTimeInSec='"+ImBlockingOthersMaxTimeInSec+"', ImBlockingOtherSessionIdsList="+ImBlockingOtherSessionIdsList);

					if (ImBlockingOthersMaxTimeInSec > threshold)
					{
						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
						AlarmEvent ae = new AlarmEventBlockingLockAlarm(cm, threshold, ImBlockingOthersMaxTimeInSec, BlockingOtherSpidsStr, blockCount);
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
						alarmHandler.addAlarm( ae );
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

	public static final String  PROPKEY_alarm_ImBlockingOthersMaxTimeInSec = CM_NAME + ".alarm.system.if.ImBlockingOthersMaxTimeInSec.gt";
	public static final int     DEFAULT_alarm_ImBlockingOthersMaxTimeInSec = 60;
	
	public static final String  PROPKEY_alarm_TempdbUsageMb                = CM_NAME + ".alarm.system.if.TempdbUsageMb.gt";
	public static final int     DEFAULT_alarm_TempdbUsageMb                = 16384; // 16 GB ... which is pretty much
	
	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		
		list.add(new CmSettingsHelper("ImBlockingOthersMaxTimeInSec", isAlarmSwitch, PROPKEY_alarm_ImBlockingOthersMaxTimeInSec, Integer.class, conf.getIntProperty(PROPKEY_alarm_ImBlockingOthersMaxTimeInSec, DEFAULT_alarm_ImBlockingOthersMaxTimeInSec), DEFAULT_alarm_ImBlockingOthersMaxTimeInSec, "If 'ImBlockingOthersMaxTimeInSec' is greater than ## then send 'AlarmEventBlockingLockAlarm'." ));
		list.add(new CmSettingsHelper("TempdbUsageMb"               , isAlarmSwitch, PROPKEY_alarm_TempdbUsageMb               , Integer.class, conf.getIntProperty(PROPKEY_alarm_TempdbUsageMb               , DEFAULT_alarm_TempdbUsageMb               ), DEFAULT_alarm_TempdbUsageMb               , "If 'TempdbUsageMb' is greater than ## then send 'AlarmEventExtensiveUsage'." ));

		return list;
	}

}
