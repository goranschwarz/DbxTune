/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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
package com.dbxtune.cm.db2;

import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NameNotFoundException;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmDescriptionProviderBase;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventHoldingLocksWhileWaitForClientInput;
import com.dbxtune.alarm.events.db2.AlarmEventDb2BlockingSession;
import com.dbxtune.alarm.events.db2.AlarmEventDb2LongRunningStatement;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSettingsHelper.RegExpInputValidator;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.db2.gui.CmActiveStatementsPanel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.pcs.PcsColumnOptions;
import com.dbxtune.pcs.PcsColumnOptions.ColumnType;
import com.dbxtune.sql.ResultSetMetaDataCached;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoDb2;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

/**
 * The connections that are working right now, and the ones that hold an open transaction or a lock that others wait for.
 * Who blocks who, the locks, the statement text (also the last one of an idle connection) and the execution plan.
 *
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
		"<p>The connections that are working right now, and the ones that hold a transaction or a lock:</p>" +
		"<ul>" +
		"    <li>executing something (ACTIVITY_STATE is EXECUTING, or an open cursor: IDLE),</li>" +
		"    <li>or has an open unit of work (UOW_STATE is UOWWAIT), even if it is not executing anything right now,</li>" +
		"    <li>or holds a lock that another connection waits for.</li>" +
		"</ul>" +
		"<p>Who blocks who, the locks of the connections that block or are blocked, the statement text (for an idle connection: the last statement), " +
		"and the execution plan of statements that has run for a while (only if the explain tables exist, see the tooltip of 'EXEC_PLAN').</p>" +
		"Table Background colors:" +
		"<ul>" +
		"    <li>ORANGE - The connection was executing the same statement in the previous sample (it has run for more than one sample).</li>" +
		"    <li>YELLOW - An open unit of work, but the connection is not executing anything (UOWWAIT): it waits for the client, and holds its locks.</li>" +
		"    <li>PINK   - The connection is blocked by another connection.</li>" +
		"    <li>RED    - The connection blocks other connections from working, and is not blocked itself.</li>" +
		"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "MON_GET_CONNECTION", "MON_GET_UNIT_OF_WORK", "MON_GET_ACTIVITY", "MON_GET_APPL_LOCKWAIT", "MON_GET_LOCKS"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

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
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

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

		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String  PROPKEY_sample_execPlan                   = CM_NAME + ".sample.execPlan";
	public static final boolean DEFAULT_sample_execPlan                   = true;

	public static final String  PROPKEY_sample_execPlan_minExecTimeInSec  = CM_NAME + ".sample.execPlan.minExecTimeInSec";
	public static final int     DEFAULT_sample_execPlan_minExecTimeInSec  = 5;

	public static final String  PROPKEY_sample_applLocks                  = CM_NAME + ".sample.applLocks";
	public static final boolean DEFAULT_sample_applLocks                  = true;

	public static final String  PROPKEY_sample_lastSqlText                = CM_NAME + ".sample.lastSqlText";
	public static final boolean DEFAULT_sample_lastSqlText                = true;

	private static final int    EXEC_PLAN_MAX_FETCH_PER_SAMPLE = 3;
	private static final int    EXEC_PLAN_CACHE_SIZE           = 200;
	private static final int    SQL_TEXT_MAX_FETCH_PER_SAMPLE  = 10;
	private static final int    SQL_TEXT_CACHE_SIZE            = 500;
	private static final int    LOCKS_MAX_APPLS_PER_SAMPLE     = 20;
	private static final int    LOOKUP_QUERY_TIMEOUT_SEC       = 5;
	private static final int    LOCKS_SHOWN_PER_APPL           = 100;  // 'APPL_LOCKS' shows the first ones (grouped by object, mode and status)

	/**
	 * Can the login do EXPLAIN_FROM_SECTION? It needs EXECUTE on the procedure and the explain tables (DbxTune never creates them).
	 * null = not yet known, it is tried once.
	 */
	private Boolean _execPlanAllowed = null;

	/** The plan of a statement: key is the EXECUTABLE_ID (the same section has the same plan) */
	private Map<String, String> _execPlanCache = new LinkedHashMap<String, String>()
	{
		private static final long serialVersionUID = 1L;
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, String> eldest)
		{
			return size() > EXEC_PLAN_CACHE_SIZE;
		}
	};

	/** The text of the last statement of an idle connection: key is the EXECUTABLE_ID */
	private Map<String, String> _sqlTextCache = new LinkedHashMap<String, String>()
	{
		private static final long serialVersionUID = 1L;
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, String> eldest)
		{
			return size() > SQL_TEXT_CACHE_SIZE;
		}
	};

	/**
	 * For 'CLIENT_IDLE_SEC': per connection (handle|member) the UOW_ID, RQSTS_COMPLETED_TOTAL and UOW_CLIENT_IDLE_WAIT_TIME (ms) when a new request was seen.
	 * Db2 has the idle time for the whole unit of work only (all waits for the client in it), not for the current wait.
	 */
	private Map<String, long[]> _idleTrack = new HashMap<>();

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_execPlan                 , DEFAULT_sample_execPlan);
		Configuration.registerDefaultValue(PROPKEY_sample_execPlan_minExecTimeInSec, DEFAULT_sample_execPlan_minExecTimeInSec);
		Configuration.registerDefaultValue(PROPKEY_sample_applLocks                , DEFAULT_sample_applLocks);
		Configuration.registerDefaultValue(PROPKEY_sample_lastSqlText              , DEFAULT_sample_lastSqlText);
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		list.add(new CmSettingsHelper("Get Execution Plan"              , PROPKEY_sample_execPlan                 , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_execPlan                 , DEFAULT_sample_execPlan                 ), DEFAULT_sample_execPlan                 , "Get the execution plan (EXPLAIN_FROM_SECTION) of statements that has run for a while. NOTE: this needs the explain tables and EXECUTE on SYSPROC.EXPLAIN_FROM_SECTION, otherwise it is turned off at the first try (with an INFO message in the log)." ));
		list.add(new CmSettingsHelper("Get Execution Plan, Min ExecTime", PROPKEY_sample_execPlan_minExecTimeInSec, Integer.class, conf.getIntProperty    (PROPKEY_sample_execPlan_minExecTimeInSec, DEFAULT_sample_execPlan_minExecTimeInSec), DEFAULT_sample_execPlan_minExecTimeInSec, "Only get the execution plan for statements that has run for more than ## seconds." ));
		list.add(new CmSettingsHelper("Get Locks"                       , PROPKEY_sample_applLocks                , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_applLocks                , DEFAULT_sample_applLocks                ), DEFAULT_sample_applLocks                , "Get the locks of the connections that block others, or are blocked (the column 'APPL_LOCKS')." ));
		list.add(new CmSettingsHelper("Get Last SQL Text"               , PROPKEY_sample_lastSqlText              , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_lastSqlText              , DEFAULT_sample_lastSqlText              ), DEFAULT_sample_lastSqlText              , "For a connection with an open unit of work that is not executing anything: get the text of its last statement from the package cache." ));

		return list;
	}

	@Override
	public Map<String, PcsColumnOptions> getPcsColumnOptions()
	{
		Map<String, PcsColumnOptions> map = super.getPcsColumnOptions();

		// No settings in the super, create one, and set it at super
		if (map == null)
		{
			// The same SQL Text and Execution Plan is seen in many samples, so only store them once
			map = new HashMap<>();
			map.put("SQL_TEXT" , new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));
			map.put("EXEC_PLAN", new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));

			// Set the map in the super
			setPcsColumnOptions(map);
		}

		return map;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("APPLICATION_HANDLE");
		pkCols.add("MEMBER");

		return pkCols;
	}

	/**
	 * One row per connection (APPLICATION_HANDLE is what FORCE APPLICATION takes).<br>
	 * The blocking columns (BLOCKED_BY_APPL_HANDLE, BLOCKING_OTHER_APPL_HANDLES...), CLIENT_IDLE_SEC, the "HAS_..." checkboxes and the HTML columns are filled in by localCalculation().
	 */
	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// NOTE: A connection can have more than one activity: nested ones (a CALL and the statements in the procedure) and open cursors (ACTIVITY_STATE IDLE).
		//       The one shown is the executing one, the innermost (highest NESTING_LEVEL).
		// NOTE: The "HAS_..." columns are 0 here, and BOOLEAN (a checkbox) in modifyResultSetMetaData()
		// NOTE: The names are blank-padded in some functions, so they are RTRIM:ed
		String sql = ""
			+ "select /* ${cmCollectorName} */ \n"
			+ "     c.APPLICATION_HANDLE \n"
			+ "    ,c.MEMBER \n"
			+ "    ,CAST('' AS VARCHAR(3))                                              as MULTI_SAMPLED \n"
			+ "    ,RTRIM(c.SESSION_AUTH_ID)                                            as SESSION_AUTH_ID \n"
			+ "    ,c.APPLICATION_NAME \n"
			+ "    ,c.CLIENT_HOSTNAME \n"
			+ "    ,c.CLIENT_IPADDR \n"
			+ "    ,c.CLIENT_APPLNAME \n"
			+ "    ,c.CLIENT_USERID \n"
			+ "    ,c.CLIENT_WRKSTNNAME \n"
			+ "    ,c.WORKLOAD_OCCURRENCE_STATE                                         as UOW_STATE \n"
			+ "    ,a.ACTIVITY_STATE \n"
			+ "    ,a.ACTIVITY_TYPE \n"
			+ "    ,SECONDS_BETWEEN(CURRENT TIMESTAMP, a.LOCAL_START_TIME)              as EXEC_TIME_SEC \n"
			+ "    ,a.LOCAL_START_TIME                                                  as ACT_START_TIME \n"
			+ "    ,u.UOW_START_TIME \n"
			+ "    ,CASE WHEN u.UOW_STOP_TIME IS NULL THEN SECONDS_BETWEEN(CURRENT TIMESTAMP, u.UOW_START_TIME) END as UOW_AGE_SEC \n"
			+ "    ,CAST(0 AS BIGINT)                                                   as CLIENT_IDLE_SEC \n"
			+ "    ,CAST(c.UOW_CLIENT_IDLE_WAIT_TIME / 1000.0 AS DECIMAL(15,1))         as UOW_CLIENT_IDLE_SEC \n"
			+ "    ,u.NUM_LOCKS_HELD \n"
			+ "    ,u.NUM_LOCKS_WAITING \n"
			+ "    ,CAST(u.UOW_LOG_SPACE_USED / 1048576.0 AS DECIMAL(15,1))             as UOW_LOG_USED_MB \n"
			+ "    ,u.ROWS_READ                                                         as UOW_ROWS_READ \n"
			+ "    ,u.ROWS_MODIFIED                                                     as UOW_ROWS_MODIFIED \n"
			+ "    ,CAST(0 AS BIGINT)                                                   as BLOCKED_BY_APPL_HANDLE \n"
			+ "    ,CAST(0 AS BIGINT)                                                   as BLOCKED_SEC \n"
			+ "    ,CAST('' AS VARCHAR(30))                                             as BLOCKED_LOCK_MODE \n"
			+ "    ,CAST('' AS VARCHAR(255))                                            as BLOCKED_ON \n"
			+ "    ,CAST('' AS VARCHAR(1000))                                           as BLOCKING_OTHER_APPL_HANDLES \n"
			+ "    ,CAST(0 AS BIGINT)                                                   as BLOCKING_OTHERS_MAX_SEC \n"
			+ "    ,CAST(0 AS BIGINT)                                                   as ROOT_BLOCKER_APPL_HANDLE \n"
			+ "    ,a.ROWS_READ                                                         as ACT_ROWS_READ \n"
			+ "    ,a.ROWS_RETURNED                                                     as ACT_ROWS_RETURNED \n"
			+ "    ,a.ROWS_MODIFIED                                                     as ACT_ROWS_MODIFIED \n"
			+ "    ,a.TOTAL_CPU_TIME / 1000                                             as ACT_CPU_TIME_MS \n"
			+ "    ,a.TOTAL_ACT_WAIT_TIME                                               as ACT_WAIT_TIME_MS \n"
			+ "    ,a.LOCK_WAIT_TIME                                                    as ACT_LOCK_WAIT_TIME_MS \n"
			+ "    ,a.POOL_DATA_L_READS + a.POOL_INDEX_L_READS + a.POOL_TEMP_DATA_L_READS + a.POOL_TEMP_INDEX_L_READS + a.POOL_XDA_L_READS + a.POOL_COL_L_READS as ACT_LOGICAL_READS \n"
			+ "    ,a.POOL_DATA_P_READS + a.POOL_INDEX_P_READS + a.POOL_TEMP_DATA_P_READS + a.POOL_TEMP_INDEX_P_READS + a.POOL_XDA_P_READS + a.POOL_COL_P_READS as ACT_PHYSICAL_READS \n"
			+ "    ,a.SORT_OVERFLOWS                                                    as ACT_SORT_OVERFLOWS \n"
			+ "    ,a.QUERY_COST_ESTIMATE \n"
			+ "    ,a.QUERY_CARD_ESTIMATE \n"
			+ "    ,a.EFFECTIVE_ISOLATION \n"
			+ "    ,u.UOW_ID \n"
			+ "    ,a.ACTIVITY_ID \n"
			+ "    ,c.RQSTS_COMPLETED_TOTAL \n"
			+ "    ,HEX(COALESCE(a.EXECUTABLE_ID, c.LAST_EXECUTABLE_ID))                as EXECUTABLE_ID \n"
			+ "    ,0                                                                   as HAS_SQL_TEXT \n"
			+ "    ,0                                                                   as HAS_EXEC_PLAN \n"
			+ "    ,0                                                                   as HAS_APPL_LOCKS \n"
			+ "    ,0                                                                   as HAS_BLOCKED_INFO \n"
			+ "    ,a.STMT_TEXT                                                         as SQL_TEXT \n"
			+ "    ,CAST(NULL AS CLOB(10M))                                             as EXEC_PLAN \n"
			+ "    ,CAST(NULL AS CLOB(10M))                                             as APPL_LOCKS \n"
			+ "    ,CAST(NULL AS CLOB(10M))                                             as BLOCKED_APPLS_INFO \n"
			+ "from table(MON_GET_CONNECTION(NULL, -2)) c \n"
			+ "left join table(MON_GET_UNIT_OF_WORK(NULL, -2)) u on u.APPLICATION_HANDLE = c.APPLICATION_HANDLE and u.MEMBER = c.MEMBER \n"
			+ "left join (select x.*, ROW_NUMBER() OVER (PARTITION BY x.APPLICATION_HANDLE, x.MEMBER \n"
			+ "                                        ORDER BY CASE x.ACTIVITY_STATE WHEN 'EXECUTING' THEN 0 WHEN 'QUEUED' THEN 1 ELSE 2 END, x.NESTING_LEVEL desc) as RN \n"
			+ "           from table(MON_GET_ACTIVITY(NULL, -2)) x \n"
			+ "          ) a on a.APPLICATION_HANDLE = c.APPLICATION_HANDLE and a.MEMBER = c.MEMBER and a.RN = 1 \n"
			+ "where c.APPLICATION_HANDLE <> MON_GET_APPLICATION_HANDLE() \n"
			+ "  and c.IS_SYSTEM_APPL = 0 \n"
			+ "  and (   a.APPLICATION_HANDLE IS NOT NULL \n"
			+ "       or (u.UOW_STOP_TIME IS NULL and u.WORKLOAD_OCCURRENCE_STATE in ('UOWWAIT', 'UOWEXEC', 'UOWQUEUED')) \n"
			+ "       or c.APPLICATION_HANDLE in (select w.HLD_APPLICATION_HANDLE from table(MON_GET_APPL_LOCKWAIT(NULL, -2)) w) \n"
			+ "      ) \n"
			+ "order by c.APPLICATION_HANDLE, c.MEMBER \n"
			+ "";

		return sql;
	}

	/** If we can't execute the functions: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return Db2CmUtils.checkFunctionsAreExecutable(this, conn,
				"MON_GET_CONNECTION(NULL, -2)", "MON_GET_UNIT_OF_WORK(NULL, -2)", "MON_GET_ACTIVITY(NULL, -2)", "MON_GET_APPL_LOCKWAIT(NULL, -2)", "MON_GET_LOCKS(NULL, -2)");
	}

	/**
	 * The "HAS_..." columns are 0 in the SQL, here they are made into BOOLEAN, so they are shown as checkboxes.<br>
	 * They are set to true in localCalculation(), when the row has a SQL Text / an Execution Plan / locks / blocked connections.
	 */
	@Override
	public ResultSetMetaDataCached modifyResultSetMetaData(ResultSetMetaDataCached rsmdc)
	{
		for (String colName : new String[] { "HAS_SQL_TEXT", "HAS_EXEC_PLAN", "HAS_APPL_LOCKS", "HAS_BLOCKED_INFO" })
		{
			int colPos = rsmdc.findColumn(colName);
			if (colPos != -1)
			{
				rsmdc.setColumnType     (colPos, java.sql.Types.BOOLEAN);
				rsmdc.setColumnTypeName (colPos, "boolean");
				rsmdc.setColumnClassName(colPos, "java.lang.Boolean");
			}
		}
		return rsmdc;
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmActiveStatementsPanel(this);
	}

	/** The SQL Text, the Execution Plan (as a text tree) and the HTML tables (locks, blocked connections) as a tooltip, on the checkbox column and on the column itself */
	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol)
	{
		if ("HAS_SQL_TEXT".equals(colName) || "SQL_TEXT".equals(colName))
		{
			int pos_SqlText = findColumn("SQL_TEXT");
			if (pos_SqlText >= 0)
			{
				Object sqlText = getValueAt(modelRow, pos_SqlText);
				if (sqlText != null && StringUtil.hasValue(sqlText.toString()))
					return "<html><pre>" + StringEscapeUtils.escapeHtml4(StringUtil.makeApproxLineBreak(sqlText.toString(), 150, 10, "\n")) + "</pre></html>";
			}
		}

		if ("HAS_EXEC_PLAN".equals(colName) || "EXEC_PLAN".equals(colName))
		{
			int pos_ExecPlan = findColumn("EXEC_PLAN");
			if (pos_ExecPlan >= 0)
			{
				Object execPlan = getValueAt(modelRow, pos_ExecPlan);
				if (execPlan != null && StringUtil.hasValue(execPlan.toString()))
					return "<html><pre>" + StringEscapeUtils.escapeHtml4(Db2ExplainUtils.planJsonToText(execPlan.toString())) + "</pre></html>";
			}
		}

		// The HTML tables: locks, and who I am blocking
		String htmlColName = null;
		if ("HAS_APPL_LOCKS"  .equals(colName) || "APPL_LOCKS"        .equals(colName)) htmlColName = "APPL_LOCKS";
		if ("HAS_BLOCKED_INFO".equals(colName) || "BLOCKED_APPLS_INFO".equals(colName)) htmlColName = "BLOCKED_APPLS_INFO";
		if (htmlColName != null)
		{
			int pos = findColumn(htmlColName);
			if (pos >= 0)
			{
				Object html = getValueAt(modelRow, pos);
				if (html != null && StringUtil.hasValue(html.toString()))
					return "<html>" + html + "</html>";
			}
		}

		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}

	/**
	 * Set 'MULTI_SAMPLED' to YES if the connection was executing the SAME activity (same UOW, activity id and start time) in the previous sample
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample)
	{
		int pos_MultiSampled  = newSample.findColumn("MULTI_SAMPLED");
		int pos_UowId         = newSample.findColumn("UOW_ID");
		int pos_ActivityId    = newSample.findColumn("ACTIVITY_ID");
		int pos_ActStartTime  = newSample.findColumn("ACT_START_TIME");

		if (prevSample == null || pos_MultiSampled == -1 || pos_UowId == -1 || pos_ActivityId == -1 || pos_ActStartTime == -1)
			return;

		for (int rowId = 0; rowId < newSample.getRowCount(); rowId++)
		{
			// Only a row with an activity
			Timestamp thisStart = newSample.getValueAsTimestamp(rowId, pos_ActStartTime, null);
			if (thisStart == null)
				continue;

			int prevRowId = prevSample.getRowNumberForPkValue(newSample.getPkValue(rowId));
			if (prevRowId < 0)
				continue;

			Timestamp prevStart = prevSample.getValueAsTimestamp(prevRowId, pos_ActStartTime, null);
			if (   thisStart.equals(prevStart)
			    && newSample.getValueAsLong(rowId, pos_UowId     , -1L) == prevSample.getValueAsLong(prevRowId, pos_UowId     , -2L)
			    && newSample.getValueAsLong(rowId, pos_ActivityId, -1L) == prevSample.getValueAsLong(prevRowId, pos_ActivityId, -2L) )
			{
				newSample.setValueAt("YES", rowId, pos_MultiSampled);
			}
		}
	}

	/**
	 * Fill in: CLIENT_IDLE_SEC, who blocks who (MON_GET_APPL_LOCKWAIT), 'BLOCKING_OTHER_APPL_HANDLES' + 'BLOCKING_OTHERS_MAX_SEC' + 'ROOT_BLOCKER_APPL_HANDLE' (from the other rows),
	 * the last SQL text of idle connections, the locks, 'BLOCKED_APPLS_INFO' and 'EXEC_PLAN'
	 */
	@Override
	public void localCalculation(CounterSample newSample)
	{
		int pos_ApplHandle            = newSample.findColumn("APPLICATION_HANDLE");
		int pos_BlockedBy             = newSample.findColumn("BLOCKED_BY_APPL_HANDLE");
		int pos_BlockedSec            = newSample.findColumn("BLOCKED_SEC");
		int pos_BlockedLockMode       = newSample.findColumn("BLOCKED_LOCK_MODE");
		int pos_BlockedOn             = newSample.findColumn("BLOCKED_ON");
		int pos_BlockingOthers        = newSample.findColumn("BLOCKING_OTHER_APPL_HANDLES");
		int pos_BlockingOthersMaxSec  = newSample.findColumn("BLOCKING_OTHERS_MAX_SEC");
		int pos_RootBlocker           = newSample.findColumn("ROOT_BLOCKER_APPL_HANDLE");

		if (pos_ApplHandle == -1 || pos_BlockedBy == -1 || pos_BlockedSec == -1 || pos_BlockedLockMode == -1 || pos_BlockedOn == -1 || pos_BlockingOthers == -1 || pos_BlockingOthersMaxSec == -1 || pos_RootBlocker == -1)
		{
			_logger.warn(getName() + ": localCalculation(), Problems finding one of the columns: APPLICATION_HANDLE, BLOCKED_BY_APPL_HANDLE, BLOCKED_SEC, BLOCKED_LOCK_MODE, BLOCKED_ON, BLOCKING_OTHER_APPL_HANDLES, BLOCKING_OTHERS_MAX_SEC, ROOT_BLOCKER_APPL_HANDLE.");
			return;
		}

		// Where each connection is in the sample (NOTE: with pureScale a connection can have a row per member, the first one is used)
		Map<Long, Integer> handleToRowId = new HashMap<>();
		for (int rowId = 0; rowId < newSample.getRowCount(); rowId++)
			handleToRowId.putIfAbsent(newSample.getValueAsLong(rowId, pos_ApplHandle, -1L), rowId);

		Configuration conf = Configuration.getCombinedConfiguration();
		DbxConnection conn = getCounterController() == null ? null : getCounterController().getMonConnection();

		//---------------------------------------------------------------
		// CLIENT_IDLE_SEC: how long the connection has waited for the client, in an open unit of work (UOWWAIT)
		//---------------------------------------------------------------
		setClientIdleSec(newSample);

		//---------------------------------------------------------------
		// Who waits for who
		// A lock request can wait for more than one holder (for example share locks): we pick one that is not waiting itself (the root cause)
		//---------------------------------------------------------------
		if (conn != null && newSample.getRowCount() > 0)
		{
			String sql = ""
				+ "select w.REQ_APPLICATION_HANDLE, w.HLD_APPLICATION_HANDLE, SECONDS_BETWEEN(CURRENT TIMESTAMP, w.LOCK_WAIT_START_TIME), \n"
				+ "       w.LOCK_OBJECT_TYPE, w.LOCK_MODE_REQUESTED, w.LOCK_MODE, RTRIM(t.TABSCHEMA) || '.' || RTRIM(t.TABNAME), w.TBSP_ID, w.TAB_FILE_ID \n"
				+ "from table(MON_GET_APPL_LOCKWAIT(NULL, -2)) w \n"
				+ "left join SYSCAT.TABLES t on t.TBSPACEID = w.TBSP_ID and t.TABLEID = w.TAB_FILE_ID \n"
				+ "order by w.LOCK_WAIT_START_TIME";

			try (Statement stmnt = conn.createStatement())
			{
				stmnt.setQueryTimeout(LOOKUP_QUERY_TIMEOUT_SEC);

				// waiting handle -> the holders it waits for
				Map<Long, List<Object[]>> lockWaits = new LinkedHashMap<>();
				try (ResultSet rs = stmnt.executeQuery(sql))
				{
					while (rs.next())
					{
						String obj = rs.getString(7);
						if (obj == null)
							obj = "tbsp " + rs.getLong(8) + ", file " + rs.getLong(9);
						lockWaits.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>()).add(new Object[] { rs.getLong(2), rs.getLong(3), rs.getString(4), rs.getString(5), rs.getString(6), obj });
					}
				}

				for (Map.Entry<Long, List<Object[]>> entry : lockWaits.entrySet())
				{
					Integer rowId = handleToRowId.get(entry.getKey());
					if (rowId == null)
						continue;

					// The holder that is not waiting itself, otherwise the first one
					Object[] blocker = entry.getValue().get(0);
					for (Object[] candidate : entry.getValue())
					{
						if ( ! lockWaits.containsKey((Long)candidate[0]) )
						{
							blocker = candidate;
							break;
						}
					}

					String others = entry.getValue().size() > 1 ? ", " + (entry.getValue().size() - 1) + " more connection(s) hold a lock on it" : "";
					String on     = blocker[2] + " lock on " + blocker[5] + " (wants " + blocker[3] + ", blocker has " + blocker[4] + others + ")";

					newSample.setValueAt(blocker[0]                                         , rowId, pos_BlockedBy);
					newSample.setValueAt(blocker[1]                                         , rowId, pos_BlockedSec);
					newSample.setValueAt(StringUtil.truncate(blocker[3] + "", 30, true, null), rowId, pos_BlockedLockMode);
					newSample.setValueAt(StringUtil.truncate(on, 255, true, null)           , rowId, pos_BlockedOn);
				}
			}
			catch (SQLException ex)
			{
				_logger.info(getName() + ": Problems getting the lock waits. (the blocking columns are empty for this sample). sql=|" + sql + "|, Caught: " + ex);
			}
		}

		//---------------------------------------------------------------
		// Who am I blocking: the rows where 'BLOCKED_BY_APPL_HANDLE' is my handle
		// NOTE: 'BLOCKED_BY_APPL_HANDLE' is 0 when the connection is not blocked
		//---------------------------------------------------------------
		for (int rowId = 0; rowId < newSample.getRowCount(); rowId++)
		{
			long handle = newSample.getValueAsLong(rowId, pos_ApplHandle, -1L);

			String blockingOthers       = "";
			long   blockingOthersMaxSec = 0;
			for (int r = 0; r < newSample.getRowCount(); r++)
			{
				if (newSample.getValueAsLong(r, pos_BlockedBy, 0L) != handle)
					continue;

				blockingOthers      += (blockingOthers.isEmpty() ? "" : ", ") + newSample.getValueAsLong(r, pos_ApplHandle, -1L);
				blockingOthersMaxSec = Math.max(blockingOthersMaxSec, newSample.getValueAsLong(r, pos_BlockedSec, 0L));
			}

			if ( ! blockingOthers.isEmpty() )
			{
				newSample.setValueAt(StringUtil.truncate(blockingOthers, 1000, true, null), rowId, pos_BlockingOthers);
				newSample.setValueAt(Long.valueOf(blockingOthersMaxSec)                   , rowId, pos_BlockingOthersMaxSec);
			}
		}

		//---------------------------------------------------------------
		// ROOT_BLOCKER_APPL_HANDLE: follow 'BLOCKED_BY_APPL_HANDLE' to the connection that is not blocked itself
		//---------------------------------------------------------------
		for (int rowId = 0; rowId < newSample.getRowCount(); rowId++)
		{
			long rootHandle = newSample.getValueAsLong(rowId, pos_BlockedBy, 0L);
			if (rootHandle <= 0)
				continue;

			// Max as many steps as there are rows: in a deadlock the connections wait for each other (until the deadlock detector breaks it)
			for (int step = 0; step < newSample.getRowCount(); step++)
			{
				Integer r = handleToRowId.get(rootHandle);
				long next = r == null ? 0 : newSample.getValueAsLong(r, pos_BlockedBy, 0L);
				if (next <= 0)
					break;
				rootHandle = next;
			}
			newSample.setValueAt(Long.valueOf(rootHandle), rowId, pos_RootBlocker);
		}

		//---------------------------------------------------------------
		// The text of the last statement, for connections with an open unit of work that do not execute anything (from the package cache)
		//---------------------------------------------------------------
		if (conn != null && conf.getBooleanProperty(PROPKEY_sample_lastSqlText, DEFAULT_sample_lastSqlText))
			lookupLastSqlText(conn, newSample);

		//---------------------------------------------------------------
		// The checkbox 'HAS_SQL_TEXT': true when the row has a SQL Text
		//---------------------------------------------------------------
		int pos_HasSqlText = newSample.findColumn("HAS_SQL_TEXT");
		int pos_SqlText    = newSample.findColumn("SQL_TEXT");
		if (pos_HasSqlText != -1 && pos_SqlText != -1)
		{
			for (int rowId = 0; rowId < newSample.getRowCount(); rowId++)
			{
				if (StringUtil.hasValue(newSample.getValueAsString(rowId, pos_SqlText)))
					newSample.setValueAt(Boolean.TRUE, rowId, pos_HasSqlText);
			}
		}

		//---------------------------------------------------------------
		// The locks of the connections that block others, or are blocked
		//---------------------------------------------------------------
		if (conn != null && conf.getBooleanProperty(PROPKEY_sample_applLocks, DEFAULT_sample_applLocks))
			lookupApplLocks(conn, newSample);

		// Who am I blocking: what they execute, and their locks (from the other rows of the sample, so AFTER the lookups)
		setBlockedApplsInfo(newSample);

		//---------------------------------------------------------------
		// Execution Plan: for statements that has been executing for a while
		// A plan is fetched ONCE per EXECUTABLE_ID (then it is in the cache), and only a few NEW plans in each sample (so we do not slow down the collector)
		//---------------------------------------------------------------
		if (conn != null && conf.getBooleanProperty(PROPKEY_sample_execPlan, DEFAULT_sample_execPlan) && ! Boolean.FALSE.equals(_execPlanAllowed))
			lookupExecPlans(conn, newSample, conf.getIntProperty(PROPKEY_sample_execPlan_minExecTimeInSec, DEFAULT_sample_execPlan_minExecTimeInSec));
	}

	/**
	 * 'CLIENT_IDLE_SEC': seconds since the last request of the connection ended, while it has an open unit of work (UOWWAIT).
	 * <p>
	 * Db2 only has UOW_CLIENT_IDLE_WAIT_TIME: all the waits for the client in the unit of work (it grows while it waits). So we remember its value when a new request was seen
	 * (RQSTS_COMPLETED_TOTAL changed), and the current wait is the growth after that. That can be one sample too short.
	 * The first time we see a unit of work, all of its idle time is used (we do not know about its earlier requests).
	 */
	private void setClientIdleSec(CounterSample newSample)
	{
		int pos_ApplHandle      = newSample.findColumn("APPLICATION_HANDLE");
		int pos_Member          = newSample.findColumn("MEMBER");
		int pos_UowState        = newSample.findColumn("UOW_STATE");
		int pos_UowId           = newSample.findColumn("UOW_ID");
		int pos_Rqsts           = newSample.findColumn("RQSTS_COMPLETED_TOTAL");
		int pos_UowClientIdle   = newSample.findColumn("UOW_CLIENT_IDLE_SEC");
		int pos_ClientIdleSec   = newSample.findColumn("CLIENT_IDLE_SEC");

		if (pos_ApplHandle == -1 || pos_Member == -1 || pos_UowState == -1 || pos_UowId == -1 || pos_Rqsts == -1 || pos_UowClientIdle == -1 || pos_ClientIdleSec == -1)
			return;

		Set<String> seen = new HashSet<>();
		for (int rowId = 0; rowId < newSample.getRowCount(); rowId++)
		{
			if ( ! "UOWWAIT".equals(newSample.getValueAsString(rowId, pos_UowState)) )
				continue;

			String key       = newSample.getValueAsLong(rowId, pos_ApplHandle, -1L) + "|" + newSample.getValueAsLong(rowId, pos_Member, -1L);
			long   uowId     = newSample.getValueAsLong(rowId, pos_UowId, -1L);
			long   rqsts     = newSample.getValueAsLong(rowId, pos_Rqsts, -1L);
			long   uowIdleMs = (long) (newSample.getValueAsDouble(rowId, pos_UowClientIdle, 0d) * 1000);
			seen.add(key);

			long[] track = _idleTrack.get(key);
			long   idleMs;
			if (track != null && track[0] == uowId && track[1] == rqsts)
			{
				idleMs = uowIdleMs - track[2];               // the same wait as in the previous sample
			}
			else if (track != null && track[0] == uowId)
			{
				_idleTrack.put(key, new long[] { uowId, rqsts, uowIdleMs }); // a request has ended since the previous sample: the wait starts here
				idleMs = 0;
			}
			else
			{
				_idleTrack.put(key, new long[] { uowId, rqsts, 0 });         // first time we see this unit of work
				idleMs = uowIdleMs;
			}
			newSample.setValueAt(Long.valueOf(Math.max(0, idleMs) / 1000), rowId, pos_ClientIdleSec);
		}
		_idleTrack.keySet().retainAll(seen);
	}

	/**
	 * 'SQL_TEXT' for a connection with an open unit of work that does not execute anything: the text of its last statement (LAST_EXECUTABLE_ID) from the package cache.
	 * A few new lookups per sample, then they are in the cache.
	 */
	private void lookupLastSqlText(DbxConnection conn, CounterSample newSample)
	{
		int pos_ActivityState = newSample.findColumn("ACTIVITY_STATE");
		int pos_ExecutableId  = newSample.findColumn("EXECUTABLE_ID");
		int pos_SqlText       = newSample.findColumn("SQL_TEXT");

		if (pos_ActivityState == -1 || pos_ExecutableId == -1 || pos_SqlText == -1)
			return;

		int fetchCount = 0;
		for (int rowId = 0; rowId < newSample.getRowCount(); rowId++)
		{
			// NOTE: getValueAsString() returns "" for NULL
			String executableId = newSample.getValueAsString(rowId, pos_ExecutableId);
			if (StringUtil.hasValue(newSample.getValueAsString(rowId, pos_ActivityState)) || StringUtil.isNullOrBlank(executableId) || StringUtil.hasValue(newSample.getValueAsString(rowId, pos_SqlText)))
				continue;

			String sqlText = _sqlTextCache.get(executableId);
			if (sqlText == null && fetchCount < SQL_TEXT_MAX_FETCH_PER_SAMPLE)
			{
				fetchCount++;
				sqlText = "";
				try (PreparedStatement pstmnt = conn.prepareStatement("select STMT_TEXT from table(MON_GET_PKG_CACHE_STMT(NULL, ?, NULL, -2)) t fetch first 1 rows only"))
				{
					pstmnt.setQueryTimeout(LOOKUP_QUERY_TIMEOUT_SEC);
					pstmnt.setBytes(1, Db2ExplainUtils.hexToBytes(executableId));
					try (ResultSet rs = pstmnt.executeQuery())
					{
						while (rs.next())
							sqlText = StringUtil.nullToValue(rs.getString(1), "");
					}
				}
				catch (SQLException ex)
				{
					_logger.debug(getName() + ": Problems getting the last SQL text for EXECUTABLE_ID=" + executableId + ". Caught: " + ex);
				}
				_sqlTextCache.put(executableId, sqlText);
			}

			if (StringUtil.hasValue(sqlText))
				newSample.setValueAt(sqlText, rowId, pos_SqlText);
		}
	}

	/**
	 * 'APPL_LOCKS' (a HTML table) and 'HAS_APPL_LOCKS': the locks of the connections that block others, or are blocked, grouped by object, mode and status.
	 * MON_GET_LOCKS with the handle as search argument (it would otherwise return all locks of the database), one statement per connection, with a short timeout.
	 */
	private void lookupApplLocks(DbxConnection conn, CounterSample newSample)
	{
		int pos_ApplHandle      = newSample.findColumn("APPLICATION_HANDLE");
		int pos_BlockedBy       = newSample.findColumn("BLOCKED_BY_APPL_HANDLE");
		int pos_BlockingOthers  = newSample.findColumn("BLOCKING_OTHER_APPL_HANDLES");
		int pos_ApplLocks       = newSample.findColumn("APPL_LOCKS");
		int pos_HasApplLocks    = newSample.findColumn("HAS_APPL_LOCKS");

		if (pos_ApplHandle == -1 || pos_BlockedBy == -1 || pos_BlockingOthers == -1 || pos_ApplLocks == -1 || pos_HasApplLocks == -1)
			return;

		String sql = ""
			+ "select l.LOCK_OBJECT_TYPE, RTRIM(t.TABSCHEMA) || '.' || RTRIM(t.TABNAME), l.TBSP_ID, l.TAB_FILE_ID, l.LOCK_MODE, l.LOCK_STATUS, count(*) \n"
			+ "from table(MON_GET_LOCKS(CAST(? AS CLOB(1K)), -2)) l \n"
			+ "left join SYSCAT.TABLES t on t.TBSPACEID = l.TBSP_ID and t.TABLEID = l.TAB_FILE_ID \n"
			+ "group by l.LOCK_OBJECT_TYPE, t.TABSCHEMA, t.TABNAME, l.TBSP_ID, l.TAB_FILE_ID, l.LOCK_MODE, l.LOCK_STATUS \n"
			+ "order by l.LOCK_STATUS desc, 2, 1";

		int applCount = 0;
		for (int rowId = 0; rowId < newSample.getRowCount(); rowId++)
		{
			boolean isBlocked  = newSample.getValueAsLong(rowId, pos_BlockedBy, 0L) > 0;
			boolean isBlocking = StringUtil.hasValue(newSample.getValueAsString(rowId, pos_BlockingOthers));
			if ( ! isBlocked && ! isBlocking )
				continue;

			if (applCount++ >= LOCKS_MAX_APPLS_PER_SAMPLE)
				break;

			long          handle = newSample.getValueAsLong(rowId, pos_ApplHandle, -1L);
			StringBuilder sb     = new StringBuilder();
			int           count  = 0;
			try (PreparedStatement pstmnt = conn.prepareStatement(sql))
			{
				pstmnt.setQueryTimeout(LOOKUP_QUERY_TIMEOUT_SEC);
				pstmnt.setString(1, "<application_handle>" + handle + "</application_handle>");
				try (ResultSet rs = pstmnt.executeQuery())
				{
					while (rs.next())
					{
						if (++count > LOCKS_SHOWN_PER_APPL)
							continue;

						String obj    = rs.getString(2);
						String status = rs.getString(6);
						if (obj == null)
							obj = rs.getLong(3) == 0 && rs.getLong(4) == 0 ? "" : "tbsp " + rs.getLong(3) + ", file " + rs.getLong(4);

						// LOCK_STATUS: G = Granted, W = Waiting, C = Converting
						sb.append("<TR").append("G".equals(status) ? "" : " style='color:red'").append(">");
						sb.append("<TD>").append(rs.getString(1)).append("</TD>");
						sb.append("<TD>").append(StringEscapeUtils.escapeHtml4(obj)).append("</TD>");
						sb.append("<TD>").append(rs.getString(5)).append("</TD>");
						sb.append("<TD>").append("G".equals(status) ? "GRANTED" : "W".equals(status) ? "WAITING" : "C".equals(status) ? "CONVERTING" : status).append("</TD>");
						sb.append("<TD ALIGN='right'>").append(rs.getLong(7)).append("</TD>");
						sb.append("</TR>");
					}
				}
			}
			catch (SQLException ex)
			{
				_logger.info(getName() + ": Problems getting the locks of application handle " + handle + " (column 'APPL_LOCKS' is empty for this sample). Caught: " + ex);
				return;
			}

			if (sb.length() > 0)
			{
				// (it starts with "<TABLE": that is how CountersModel.toHtmlTableString() knows that it is HTML, when it makes the alarm description)
				String html = "<TABLE BORDER=1 class='dbx-table-basic'>"
						+ "<TR> <TH>Type</TH> <TH>Object</TH> <TH>Mode</TH> <TH>Status</TH> <TH>Count</TH> </TR>"
						+ sb
						+ (count > LOCKS_SHOWN_PER_APPL ? "<TR> <TD COLSPAN=5><I>... and " + (count - LOCKS_SHOWN_PER_APPL) + " more lines</I></TD> </TR>" : "")
						+ "</TABLE>";
				newSample.setValueAt(html        , rowId, pos_ApplLocks);
				newSample.setValueAt(Boolean.TRUE, rowId, pos_HasApplLocks);
			}
		}
	}

	/**
	 * 'BLOCKED_APPLS_INFO' (a HTML table) and 'HAS_BLOCKED_INFO': for a connection that blocks others, the connections that wait for it:
	 * who they are, what they execute, what they wait for and their locks. All of it from the other rows of the sample.
	 */
	private void setBlockedApplsInfo(CounterSample newSample)
	{
		int pos_ApplHandle      = newSample.findColumn("APPLICATION_HANDLE");
		int pos_BlockedBy       = newSample.findColumn("BLOCKED_BY_APPL_HANDLE");
		int pos_SqlText         = newSample.findColumn("SQL_TEXT");
		int pos_ApplLocks       = newSample.findColumn("APPL_LOCKS");
		int pos_HasBlockedInfo  = newSample.findColumn("HAS_BLOCKED_INFO");
		int pos_BlockedInfo     = newSample.findColumn("BLOCKED_APPLS_INFO");

		if (pos_ApplHandle == -1 || pos_BlockedBy == -1 || pos_SqlText == -1 || pos_ApplLocks == -1 || pos_HasBlockedInfo == -1 || pos_BlockedInfo == -1)
			return;

		// The columns of the blocked connection that goes into the "Info" cell (the ones that have a value)
		String[] infoColumns = { "SESSION_AUTH_ID", "APPLICATION_NAME", "CLIENT_HOSTNAME", "CLIENT_APPLNAME", "UOW_STATE", "ACTIVITY_TYPE", "BLOCKED_SEC", "BLOCKED_LOCK_MODE", "BLOCKED_ON", "UOW_START_TIME" };

		for (int rowId = 0; rowId < newSample.getRowCount(); rowId++)
		{
			long handle = newSample.getValueAsLong(rowId, pos_ApplHandle, -1L);

			StringBuilder sb = new StringBuilder();
			for (int r = 0; r < newSample.getRowCount(); r++)
			{
				if (newSample.getValueAsLong(r, pos_BlockedBy, 0L) != handle)
					continue;

				String info = "<TABLE BORDER=0 CELLSPACING=0 CELLPADDING=1 class='dbx-table-basic'>";
				for (String colName : infoColumns)
				{
					int    pos = newSample.findColumn(colName);
					String val = pos == -1 ? null : newSample.getValueAsString(r, pos);
					if (StringUtil.hasValue(val))
						info += "<TR> <TD><B>" + colName + "</B></TD> <TD>" + StringEscapeUtils.escapeHtml4(val) + "</TD> </TR>";
				}
				info += "</TABLE>";

				String sqlText = newSample.getValueAsString(r, pos_SqlText);
				if (sqlText != null && sqlText.length() > 4000)
					sqlText = sqlText.substring(0, 4000) + "\n... (the first 4000 of " + sqlText.length() + " characters)";

				String locks = newSample.getValueAsString(r, pos_ApplLocks);

				sb.append("  <TR>\n");
				sb.append("    <TD><B>").append(newSample.getValueAsLong(r, pos_ApplHandle, -1L)).append("</B></TD>\n");
				sb.append("    <TD>").append(info).append("</TD>\n");
				sb.append("    <TD><PRE>").append(sqlText == null ? "" : StringEscapeUtils.escapeHtml4(sqlText)).append("</PRE></TD>\n");
				sb.append("    <TD>").append(locks == null ? "" : locks).append("</TD>\n");
				sb.append("  </TR>\n");
			}

			if (sb.length() > 0)
			{
				// (it starts with "<TABLE": that is how CountersModel.toHtmlTableString() knows that it is HTML, when it makes the alarm description)
				String html = "<TABLE BORDER=1 class='dbx-table-basic'>\n"
						+ "  <TR> <TH>Blocked Appl Handle</TH> <TH>Info</TH> <TH>SQL Text</TH> <TH>Locks</TH> </TR>\n"
						+ sb
						+ "</TABLE>\n";
				newSample.setValueAt(html        , rowId, pos_BlockedInfo);
				newSample.setValueAt(Boolean.TRUE, rowId, pos_HasBlockedInfo);
			}
		}
	}

	/**
	 * 'EXEC_PLAN' and 'HAS_EXEC_PLAN': EXPLAIN_FROM_SECTION of the running section, for statements that has run for more than 'minExecTimeInSec'.
	 * <p>
	 * It needs the explain tables (in the login's schema or SYSTOOLS, see DbmsSetup/db2.txt) and EXECUTE on SYSPROC.EXPLAIN_FROM_SECTION.
	 * When that is missing, it is turned off with one INFO message. The plan is read as JSON, and our own rows are deleted from the explain tables.
	 */
	private void lookupExecPlans(DbxConnection conn, CounterSample newSample, int minExecTimeInSec)
	{
		int pos_Member        = newSample.findColumn("MEMBER");
		int pos_ActivityState = newSample.findColumn("ACTIVITY_STATE");
		int pos_ExecTimeSec   = newSample.findColumn("EXEC_TIME_SEC");
		int pos_ExecutableId  = newSample.findColumn("EXECUTABLE_ID");
		int pos_ExecPlan      = newSample.findColumn("EXEC_PLAN");
		int pos_HasExecPlan   = newSample.findColumn("HAS_EXEC_PLAN");

		if (pos_Member == -1 || pos_ActivityState == -1 || pos_ExecTimeSec == -1 || pos_ExecutableId == -1 || pos_ExecPlan == -1 || pos_HasExecPlan == -1)
			return;

		// Where are the explain tables (found at connect)
		String explainSchema = conn.getDbmsVersionInfo() instanceof DbmsVersionInfoDb2 ? ((DbmsVersionInfoDb2) conn.getDbmsVersionInfo()).getExplainSchema() : null;
		if (explainSchema == null)
		{
			_execPlanAllowed = Boolean.FALSE;
			_logger.info(getName() + ": The execution plans of running statements are NOT collected: no explain tables were found (in the login's own schema or SYSTOOLS). See DbmsSetup/db2.txt how to create them. "
					+ "To turn off this check, set '" + PROPKEY_sample_execPlan + "=false'.");
			return;
		}

		int fetchCount = 0;
		for (int rowId = 0; rowId < newSample.getRowCount(); rowId++)
		{
			// Only for statements that are running, and has done so for a while
			if ( ! "EXECUTING".equals(newSample.getValueAsString(rowId, pos_ActivityState)) || newSample.getValueAsLong(rowId, pos_ExecTimeSec, 0L) < minExecTimeInSec )
				continue;

			String executableId = newSample.getValueAsString(rowId, pos_ExecutableId);
			if (StringUtil.isNullOrBlank(executableId))
				continue;

			String execPlan = _execPlanCache.get(executableId);
			if (execPlan == null && fetchCount < EXEC_PLAN_MAX_FETCH_PER_SAMPLE)
			{
				fetchCount++;
				try
				{
					execPlan = Db2ExplainUtils.getExecPlanJson(conn, explainSchema, executableId, newSample.getValueAsLong(rowId, pos_Member, 0L).intValue(), LOOKUP_QUERY_TIMEOUT_SEC);
					_execPlanAllowed = Boolean.TRUE;
				}
				catch (SQLException ex)
				{
					// -551 = no EXECUTE on EXPLAIN_FROM_SECTION, -204 = an explain table is missing, -219 = the explain tables are missing or old: we can never do it (no need to try again)
					int sqlCode = ex.getErrorCode();
					if (sqlCode == -551 || sqlCode == -204 || sqlCode == -219 || sqlCode == -220)
					{
						_execPlanAllowed = Boolean.FALSE;
						_logger.info(getName() + ": The execution plans of running statements are NOT collected: EXPLAIN_FROM_SECTION failed with SQLCODE " + sqlCode + ". "
								+ "It needs EXECUTE on SYSPROC.EXPLAIN_FROM_SECTION and the explain tables in schema '" + explainSchema + "' (see DbmsSetup/db2.txt). "
								+ "To turn off this try, set '" + PROPKEY_sample_execPlan + "=false'. Caught: " + ex.getMessage());
						return;
					}

					// Other errors, for example -20501 (the section is no longer in the package cache). Not a problem: just no plan for this statement
					_logger.debug(getName() + ": Problems getting execution plan for EXECUTABLE_ID=" + executableId + ". Caught: " + ex);
					_execPlanCache.put(executableId, "");
					continue;
				}
				_execPlanCache.put(executableId, execPlan);
			}

			if (StringUtil.hasValue(execPlan))
			{
				newSample.setValueAt(execPlan    , rowId, pos_ExecPlan);
				newSample.setValueAt(Boolean.TRUE, rowId, pos_HasExecPlan);
			}
		}
	}

	/** When we are disconnected (or the PCS database is rolled over): forget the plans, so they are stored again. And test again if plans are allowed */
	@Override
	public void reset()
	{
		super.reset();

		_execPlanCache.clear();
		_sqlTextCache .clear();
		_idleTrack    .clear();
		_execPlanAllowed = null;
	}

	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasAbsData() )
			return;

		if ( ! AlarmHandler.hasInstance() )
			return;

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());

		CountersModel cm   = this;
		Configuration conf = Configuration.getCombinedConfiguration();

		for (int r=0; r<cm.getAbsRowCount(); r++)
		{
			final long handle   = cm.getAbsValueAsLong(r, "APPLICATION_HANDLE", -1L);
			String     authId   = cm.getAbsString(r, "SESSION_AUTH_ID");
			String     applName = cm.getAbsString(r, "APPLICATION_NAME");
			String     host     = cm.getAbsString(r, "CLIENT_HOSTNAME");
			String     uowState = cm.getAbsString(r, "UOW_STATE");

			//-------------------------------------------------------
			// ImBlockingOthersMaxTimeInSec: This connection blocks others, and is NOT blocked itself (it is the root cause)
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("ImBlockingOthersMaxTimeInSec"))
			{
				String blockingOthers       = cm.getAbsString(r, "BLOCKING_OTHER_APPL_HANDLES");
				long   blockedBy            = cm.getAbsValueAsLong(r, "BLOCKED_BY_APPL_HANDLE" , 0L);
				long   blockingOthersMaxSec = cm.getAbsValueAsLong(r, "BLOCKING_OTHERS_MAX_SEC", 0L);

				if (StringUtil.hasValue(blockingOthers) && blockedBy == 0)
				{
					int threshold = conf.getIntProperty(PROPKEY_alarm_ImBlockingOthersMaxTimeInSec, DEFAULT_alarm_ImBlockingOthersMaxTimeInSec);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest(" + cm.getName() + "): ImBlockingOthersMaxTimeInSec: handle=" + handle + ", threshold=" + threshold + ", BLOCKING_OTHERS_MAX_SEC='" + blockingOthersMaxSec + "', BLOCKING_OTHER_APPL_HANDLES='" + blockingOthers + "'.");

					if (blockingOthersMaxSec > threshold)
					{
						int blockCount = StringUtil.commaStrToList(blockingOthers).size();
						AlarmEvent ae = new AlarmEventDb2BlockingSession(cm, threshold, handle, authId, applName, host, blockingOthersMaxSec, "[" + blockingOthers + "]", blockCount);

						// The extended description is from when the alarm was (re)raised: the statement at that time
						final String raiseText = cm.toTextTableString(DATA_ABS, r);
						final String raiseHtml = cm.toHtmlTableString(DATA_ABS, r, true, false, false);

						// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
						ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(null, null, threshold)
						{
							@Override
							public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
							{
								// Always: (RAISE, RE-RAISE, CANCEL) - Set the extended description (from when it was (re)raised)
								alarmEvent.setExtendedDescription(raiseText, raiseHtml);

								// Set CANCEL message (values that was found AFTER last raise/re-raise event)
								if (AlarmPhase.CANCEL.equals(phase))
								{
									int    rowId = cm.getRowIdWhere(DATA_ABS, "APPLICATION_HANDLE", handle);
									String label = "application handle " + handle;
									if (rowId == -1)
										alarmEvent.setCancelDescription(label + " has no active statement or open unit of work now");
									else
									{
										String BlockingOthers       = cm.getAbsString(rowId, "BLOCKING_OTHER_APPL_HANDLES");
										Object BlockingOthersMaxSec = cm.getAbsValue (rowId, "BLOCKING_OTHERS_MAX_SEC");
										String cancelMsg = StringUtil.isNullOrBlank(BlockingOthers)
										                 ? label + " is no longer blocking any connection"
										                 : getAlarmCancelText(label + ": max time it blocks others", BlockingOthersMaxSec + " seconds", " seconds");
										alarmEvent.setCancelDescription(cancelMsg);
									}
								}
							}
						});

						// Information about how to disable this alarm
						ae.createAlarmOptionsMessage(this, "ImBlockingOthersMaxTimeInSec");

						AlarmHandler.getInstance().addAlarm(ae);
					}
				}
			} // end: ImBlockingOthersMaxTimeInSec

			//-------------------------------------------------------
			// HoldingLocksWhileWaitForClientInputInSec: an open unit of work with locks, and the connection is waiting for the client (UOWWAIT)
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("HoldingLocksWhileWaitForClientInputInSec"))
			{
				long clientIdleSec = cm.getAbsValueAsLong(r, "CLIENT_IDLE_SEC", 0L);
				long numLocksHeld  = cm.getAbsValueAsLong(r, "NUM_LOCKS_HELD" , 0L);

				if ("UOWWAIT".equals(uowState) && numLocksHeld > 0)
				{
					int threshold = conf.getIntProperty(PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSec, DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSec);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest(" + cm.getName() + "): HoldingLocksWhileWaitForClientInputInSec: handle=" + handle + ", threshold=" + threshold + ", CLIENT_IDLE_SEC='" + clientIdleSec + "', NUM_LOCKS_HELD='" + numLocksHeld + "'.");

					if (clientIdleSec > threshold)
					{
						// The last request ended about CLIENT_IDLE_SEC ago
						String lastRequestTime = new Timestamp(System.currentTimeMillis() - clientIdleSec * 1000).toString();
						long   rowsModified    = cm.getAbsValueAsLong(r, "UOW_ROWS_MODIFIED", 0L);

						AlarmEvent ae = new AlarmEventHoldingLocksWhileWaitForClientInput(cm, threshold, handle, (int) clientIdleSec, lastRequestTime, rowsModified > 0);

						// The extended description is from when the alarm was (re)raised: the statement at that time
						final String raiseText = cm.toTextTableString(DATA_ABS, r);
						final String raiseHtml = cm.toHtmlTableString(DATA_ABS, r, true, false, false);

						// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
						ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(null, null, threshold)
						{
							@Override
							public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
							{
								// Always: (RAISE, RE-RAISE, CANCEL) - Set the extended description (from when it was (re)raised)
								alarmEvent.setExtendedDescription(raiseText, raiseHtml);

								// Set CANCEL message (values that was found AFTER last raise/re-raise event)
								if (AlarmPhase.CANCEL.equals(phase))
								{
									int    rowId = cm.getRowIdWhere(DATA_ABS, "APPLICATION_HANDLE", handle);
									String label = "application handle " + handle;
									if (rowId == -1)
										alarmEvent.setCancelDescription(label + " has no open unit of work now");
									else
									{
										Object UowState      = cm.getAbsValue(rowId, "UOW_STATE");
										Object ClientIdleSec = cm.getAbsValue(rowId, "CLIENT_IDLE_SEC");
										String cancelMsg = getAlarmCancelText(label + ": state", "'" + UowState + "', waited for the client " + ClientIdleSec + " seconds", " seconds");
										alarmEvent.setCancelDescription(cancelMsg);
									}
								}
							}
						});

						// Information about how to disable this alarm
						ae.createAlarmOptionsMessage(this, "HoldingLocksWhileWaitForClientInputInSec");

						AlarmHandler.getInstance().addAlarm(ae);
					}
				}
			} // end: HoldingLocksWhileWaitForClientInputInSec

			//-------------------------------------------------------
			// StatementExecInSec: executing the same statement for a long time
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("StatementExecInSec"))
			{
				long execTimeSec = cm.getAbsValueAsLong(r, "EXEC_TIME_SEC", 0L);

				if ("EXECUTING".equals(cm.getAbsString(r, "ACTIVITY_STATE")) && execTimeSec > 0)
				{
					int threshold = conf.getIntProperty(PROPKEY_alarm_StatementExecInSec, DEFAULT_alarm_StatementExecInSec);

					// Skip some users or programs (for example batch jobs that we know takes a long time)
					String skipUserRegex    = conf.getProperty(PROPKEY_alarm_StatementExecInSecSkipUser   , DEFAULT_alarm_StatementExecInSecSkipUser);
					String skipProgramRegex = conf.getProperty(PROPKEY_alarm_StatementExecInSecSkipProgram, DEFAULT_alarm_StatementExecInSecSkipProgram);
					boolean skip = (StringUtil.hasValue(skipUserRegex)    && authId   != null && authId  .matches(skipUserRegex))
					            || (StringUtil.hasValue(skipProgramRegex) && applName != null && applName.matches(skipProgramRegex));

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest(" + cm.getName() + "): StatementExecInSec: handle=" + handle + ", threshold=" + threshold + ", EXEC_TIME_SEC='" + execTimeSec + "', skip=" + skip + ", SESSION_AUTH_ID='" + authId + "', APPLICATION_NAME='" + applName + "'.");

					if (execTimeSec > threshold && ! skip)
					{
						AlarmEvent ae = new AlarmEventDb2LongRunningStatement(cm, threshold, handle, execTimeSec, cm.getAbsString(r, "ACT_START_TIME"), authId, applName, cm.getAbsString(r, "EXECUTABLE_ID"));

						// The extended description is from when the alarm was (re)raised: the statement at that time
						final String raiseText = cm.toTextTableString(DATA_ABS, r);
						final String raiseHtml = cm.toHtmlTableString(DATA_ABS, r, true, false, false);

						// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
						ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(null, null, threshold)
						{
							@Override
							public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
							{
								// Always: (RAISE, RE-RAISE, CANCEL) - Set the extended description (from when it was (re)raised)
								alarmEvent.setExtendedDescription(raiseText, raiseHtml);

								// Set CANCEL message (values that was found AFTER last raise/re-raise event)
								if (AlarmPhase.CANCEL.equals(phase))
								{
									int    rowId = cm.getRowIdWhere(DATA_ABS, "APPLICATION_HANDLE", handle);
									String label = "application handle " + handle;
									if (rowId == -1)
										alarmEvent.setCancelDescription(label + " has no active statement now");
									else
									{
										Object ActivityState = cm.getAbsValue(rowId, "ACTIVITY_STATE");
										Object ExecTimeSec   = cm.getAbsValue(rowId, "EXEC_TIME_SEC");
										String cancelMsg = getAlarmCancelText(label + ": activity", "'" + ActivityState + "', statement running for " + ExecTimeSec + " seconds", " seconds");
										alarmEvent.setCancelDescription(cancelMsg);
									}
								}
							}
						});

						// Information about how to disable this alarm
						ae.createAlarmOptionsMessage(this, "StatementExecInSec");

						AlarmHandler.getInstance().addAlarm(ae);
					}
				}
			} // end: StatementExecInSec

		} // end: loop-rows
	}

	public static final String  PROPKEY_alarm_ImBlockingOthersMaxTimeInSec             = CM_NAME + ".alarm.system.if.ImBlockingOthersMaxTimeInSec.gt";
	public static final int     DEFAULT_alarm_ImBlockingOthersMaxTimeInSec             = 60;

	public static final String  PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSec = CM_NAME + ".alarm.system.if.HoldingLocksWhileWaitForClientInputInSec.gt";
	public static final int     DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSec = 180; // 3 minutes

	public static final String  PROPKEY_alarm_StatementExecInSec                       = CM_NAME + ".alarm.system.if.StatementExecInSec.gt";
	public static final int     DEFAULT_alarm_StatementExecInSec                       = 3 * 60 * 60; // 3 Hours

	public static final String  PROPKEY_alarm_StatementExecInSecSkipUser               = CM_NAME + ".alarm.system.if.StatementExecInSec.skip.user";
	public static final String  DEFAULT_alarm_StatementExecInSecSkipUser               = "";

	public static final String  PROPKEY_alarm_StatementExecInSecSkipProgram            = CM_NAME + ".alarm.system.if.StatementExecInSec.skip.program";
	public static final String  DEFAULT_alarm_StatementExecInSecSkipProgram            = "";

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("ImBlockingOthersMaxTimeInSec"            , isAlarmSwitch, PROPKEY_alarm_ImBlockingOthersMaxTimeInSec            , Integer.class, conf.getIntProperty(PROPKEY_alarm_ImBlockingOthersMaxTimeInSec            , DEFAULT_alarm_ImBlockingOthersMaxTimeInSec            ), DEFAULT_alarm_ImBlockingOthersMaxTimeInSec            , "If 'BLOCKING_OTHERS_MAX_SEC' is greater than ## seconds (and the connection is not blocked itself) then send 'AlarmEventDb2BlockingSession'." ));
		list.add(new CmSettingsHelper("HoldingLocksWhileWaitForClientInputInSec", isAlarmSwitch, PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSec, Integer.class, conf.getIntProperty(PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSec, DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSec), DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSec, "If a connection has an open unit of work with locks (UOW_STATE is UOWWAIT and NUM_LOCKS_HELD > 0), and 'CLIENT_IDLE_SEC' is greater than ## seconds then send 'AlarmEventHoldingLocksWhileWaitForClientInput'." ));
		list.add(new CmSettingsHelper("StatementExecInSec"                      , isAlarmSwitch, PROPKEY_alarm_StatementExecInSec                      , Integer.class, conf.getIntProperty(PROPKEY_alarm_StatementExecInSec                      , DEFAULT_alarm_StatementExecInSec                      ), DEFAULT_alarm_StatementExecInSec                      , "If a statement is executing and 'EXEC_TIME_SEC' is greater than ## seconds then send 'AlarmEventDb2LongRunningStatement'." ));
		list.add(new CmSettingsHelper("StatementExecInSec SkipUser"             ,                PROPKEY_alarm_StatementExecInSecSkipUser              , String .class, conf.getProperty   (PROPKEY_alarm_StatementExecInSecSkipUser              , DEFAULT_alarm_StatementExecInSecSkipUser              ), DEFAULT_alarm_StatementExecInSecSkipUser              , "If 'StatementExecInSec' is true; Skip connections where 'SESSION_AUTH_ID' matches this regexp."  , new RegExpInputValidator()));
		list.add(new CmSettingsHelper("StatementExecInSec SkipProgram"          ,                PROPKEY_alarm_StatementExecInSecSkipProgram           , String .class, conf.getProperty   (PROPKEY_alarm_StatementExecInSecSkipProgram           , DEFAULT_alarm_StatementExecInSecSkipProgram           ), DEFAULT_alarm_StatementExecInSecSkipProgram           , "If 'StatementExecInSec' is true; Skip connections where 'APPLICATION_NAME' matches this regexp.", new RegExpInputValidator()));

		return list;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(CM_NAME, "The connections that are working right now, and the ones that hold an open transaction or a lock that others wait for.");

			mtd.addColumn(CM_NAME, "APPLICATION_HANDLE",          "<html>The connection (what FORCE APPLICATION takes).</html>");
			mtd.addColumn(CM_NAME, "MEMBER",                      "<html>The database member (0 if this is not pureScale or DPF).</html>");
			mtd.addColumn(CM_NAME, "MULTI_SAMPLED",               "<html>YES = the connection was executing the same statement (the same activity) in the previous sample. So it has been running for more than one sample.</html>");
			mtd.addColumn(CM_NAME, "SESSION_AUTH_ID",             "<html>The user of the connection (the session authorization id).</html>");
			mtd.addColumn(CM_NAME, "APPLICATION_NAME",            "<html>The client program (for JDBC normally 'db2jcc_application', unless the client sets it).</html>");
			mtd.addColumn(CM_NAME, "CLIENT_HOSTNAME",             "<html>The client host.</html>");
			mtd.addColumn(CM_NAME, "CLIENT_IPADDR",               "<html>The IP address of the client.</html>");
			mtd.addColumn(CM_NAME, "CLIENT_APPLNAME",             "<html>The application name, as the client set it (client information, for example from an application server).</html>");
			mtd.addColumn(CM_NAME, "CLIENT_USERID",               "<html>The end user, as the client set it (client information).</html>");
			mtd.addColumn(CM_NAME, "CLIENT_WRKSTNNAME",           "<html>The workstation, as the client set it (client information).</html>");
			mtd.addColumn(CM_NAME, "UOW_STATE",                   "<html>The state of the connection (WORKLOAD_OCCURRENCE_STATE):<ul>"
			                                                    + "<li>UOWEXEC - it is executing a request</li>"
			                                                    + "<li>UOWWAIT - it has an open unit of work, and waits for the client (it holds its locks)</li>"
			                                                    + "<li>DECOUPLED - no unit of work (idle between transactions)</li>"
			                                                    + "<li>UOWQUEUED - its request is queued by workload management</li>"
			                                                    + "</ul></html>");
			mtd.addColumn(CM_NAME, "ACTIVITY_STATE",              "<html>The state of the statement: EXECUTING, IDLE (an open cursor, it waits for the client to fetch), QUEUED (by workload management).<br>Empty if no statement is running: then SQL_TEXT is the last statement of the connection.</html>");
			mtd.addColumn(CM_NAME, "ACTIVITY_TYPE",               "<html>The kind of statement: READ_DML, WRITE_DML, DDL, CALL, LOAD, OTHER.</html>");
			mtd.addColumn(CM_NAME, "EXEC_TIME_SEC",               "<html>How long the statement has been running, in seconds (since LOCAL_START_TIME).<br><b>Alarm</b>: StatementExecInSec (only for EXECUTING)</html>");
			mtd.addColumn(CM_NAME, "ACT_START_TIME",              "<html>When the statement started to execute.</html>");
			mtd.addColumn(CM_NAME, "UOW_START_TIME",              "<html>When the unit of work (transaction) started.</html>");
			mtd.addColumn(CM_NAME, "UOW_AGE_SEC",                 "<html>How long the unit of work has been open, in seconds. Empty if there is no open unit of work.</html>");
			mtd.addColumn(CM_NAME, "CLIENT_IDLE_SEC",             "<html>For a connection with an open unit of work that waits for the client (UOWWAIT): seconds since its last request ended.<br>"
			                                                    + "Db2 only has the idle time of the whole unit of work (UOW_CLIENT_IDLE_SEC), so it is computed from the growth of that, after the last request was seen. "
			                                                    + "It can be one sample too short, and the first time a unit of work is seen it is all the idle time of the unit of work.<br>"
			                                                    + "<b>Alarm</b>: HoldingLocksWhileWaitForClientInputInSec</html>");
			mtd.addColumn(CM_NAME, "UOW_CLIENT_IDLE_SEC",         "<html>All the time the unit of work has waited for the client, in seconds. (UOW_CLIENT_IDLE_WAIT_TIME)</html>");
			mtd.addColumn(CM_NAME, "NUM_LOCKS_HELD",              "<html>Number of locks the unit of work holds.</html>");
			mtd.addColumn(CM_NAME, "NUM_LOCKS_WAITING",           "<html>Number of locks the unit of work waits for.</html>");
			mtd.addColumn(CM_NAME, "UOW_LOG_USED_MB",             "<html>Transaction log used by the unit of work, in MB. (UOW_LOG_SPACE_USED)</html>");
			mtd.addColumn(CM_NAME, "UOW_ROWS_READ",               "<html>Rows read in the unit of work.</html>");
			mtd.addColumn(CM_NAME, "UOW_ROWS_MODIFIED",           "<html>Rows inserted, updated or deleted in the unit of work.</html>");
			mtd.addColumn(CM_NAME, "BLOCKED_BY_APPL_HANDLE",      "<html>The connection this one waits for (a lock). 0 = not blocked.<br>If more than one connection holds the lock, one that is not blocked itself.</html>");
			mtd.addColumn(CM_NAME, "BLOCKED_SEC",                 "<html>How long this connection has waited for the lock, in seconds.</html>");
			mtd.addColumn(CM_NAME, "BLOCKED_LOCK_MODE",           "<html>The lock mode it waits for (for example X, S, U, IX).</html>");
			mtd.addColumn(CM_NAME, "BLOCKED_ON",                  "<html>What it waits for: the lock type (ROW, TABLE...), the table, the mode it wants and the mode the blocker has.</html>");
			mtd.addColumn(CM_NAME, "BLOCKING_OTHER_APPL_HANDLES", "<html>The connections that waits for this one.</html>");
			mtd.addColumn(CM_NAME, "BLOCKING_OTHERS_MAX_SEC",     "<html>The longest time one of the connections in 'BLOCKING_OTHER_APPL_HANDLES' has waited, in seconds.<br><b>Alarm</b>: ImBlockingOthersMaxTimeInSec</html>");
			mtd.addColumn(CM_NAME, "ROOT_BLOCKER_APPL_HANDLE",    "<html>For a blocked connection: the connection at the start of the chain (the one that is not blocked itself).</html>");
			mtd.addColumn(CM_NAME, "ACT_ROWS_READ",               "<html>Rows read by the statement (so far).</html>");
			mtd.addColumn(CM_NAME, "ACT_ROWS_RETURNED",           "<html>Rows returned by the statement (so far).</html>");
			mtd.addColumn(CM_NAME, "ACT_ROWS_MODIFIED",           "<html>Rows changed by the statement (so far).</html>");
			mtd.addColumn(CM_NAME, "ACT_CPU_TIME_MS",             "<html>CPU used by the statement, in milliseconds. (the activity metrics are updated about every 10 seconds while it runs)</html>");
			mtd.addColumn(CM_NAME, "ACT_WAIT_TIME_MS",            "<html>Time the statement has waited (for locks, I/O, the log...), in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "ACT_LOCK_WAIT_TIME_MS",       "<html>Time the statement has waited for locks, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "ACT_LOGICAL_READS",           "<html>Buffer pool page reads (data, index, temp, XML and column-organized) by the statement.</html>");
			mtd.addColumn(CM_NAME, "ACT_PHYSICAL_READS",          "<html>Pages the statement has read from disk.</html>");
			mtd.addColumn(CM_NAME, "ACT_SORT_OVERFLOWS",          "<html>Sorts of the statement that did not fit in memory (spilled to the temporary table space).</html>");
			mtd.addColumn(CM_NAME, "QUERY_COST_ESTIMATE",         "<html>The optimizer's cost estimate of the statement, in timerons.</html>");
			mtd.addColumn(CM_NAME, "QUERY_CARD_ESTIMATE",         "<html>The optimizer's estimate of the number of rows the statement returns.</html>");
			mtd.addColumn(CM_NAME, "EFFECTIVE_ISOLATION",         "<html>The isolation level of the statement: UR, CS, RS or RR.</html>");
			mtd.addColumn(CM_NAME, "UOW_ID",                      "<html>The unit of work id (within the connection).</html>");
			mtd.addColumn(CM_NAME, "ACTIVITY_ID",                 "<html>The activity id (within the unit of work).</html>");
			mtd.addColumn(CM_NAME, "RQSTS_COMPLETED_TOTAL",       "<html>Requests the connection has completed (used for CLIENT_IDLE_SEC).</html>");
			mtd.addColumn(CM_NAME, "EXECUTABLE_ID",               "<html>The id of the statement in the package cache (hex). The same id as in the tab 'Statements'.<br>For a connection that does not execute anything: its last statement.</html>");
			mtd.addColumn(CM_NAME, "HAS_SQL_TEXT",                "<html>Checked if 'SQL_TEXT' has a value. Hover over the cell to see the SQL Text.</html>");
			mtd.addColumn(CM_NAME, "HAS_EXEC_PLAN",               "<html>Checked if 'EXEC_PLAN' has a value. Hover over the cell to see the execution plan.</html>");
			mtd.addColumn(CM_NAME, "HAS_APPL_LOCKS",              "<html>Checked if 'APPL_LOCKS' has a value. Hover over the cell to see the locks.</html>");
			mtd.addColumn(CM_NAME, "HAS_BLOCKED_INFO",            "<html>Checked if 'BLOCKED_APPLS_INFO' has a value. Hover over the cell to see the connections this one blocks.</html>");
			mtd.addColumn(CM_NAME, "SQL_TEXT",                    "<html>The statement that runs. If no statement runs (ACTIVITY_STATE is empty) and the connection has an open unit of work: its last statement, from the package cache.</html>");
			mtd.addColumn(CM_NAME, "EXEC_PLAN",                   "<html>The execution plan (EXPLAIN_FROM_SECTION, as JSON) of a statement that has run for a while (property '" + PROPKEY_sample_execPlan_minExecTimeInSec + "'). The tooltip shows it as a text tree.<br>"
			                                                    + "<b>Note</b>: it needs the explain tables (created by the DBA, see DbmsSetup/db2.txt) and EXECUTE on SYSPROC.EXPLAIN_FROM_SECTION. It does not need access to the data.</html>");
			mtd.addColumn(CM_NAME, "APPL_LOCKS",                  "<html>The locks of a connection that blocks others or is blocked (MON_GET_LOCKS), grouped by object, mode and status. Waiting/converting ones in red.</html>");
			mtd.addColumn(CM_NAME, "BLOCKED_APPLS_INFO",          "<html>For a connection that blocks others: the connections that waits for it, what they execute, and their locks.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}
}
