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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.cm.mysql;

import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmDescriptionProviderBase;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventHoldingLocksWhileWaitForClientInput;
import com.dbxtune.alarm.events.mysql.AlarmEventMyBlockingSession;
import com.dbxtune.alarm.events.mysql.AlarmEventMyLongRunningStatement;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSettingsHelper.RegExpInputValidator;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.mysql.gui.CmActiveStatementsPanel;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.pcs.PcsColumnOptions;
import com.dbxtune.pcs.PcsColumnOptions.ColumnType;
import com.dbxtune.sql.ResultSetMetaDataCached;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

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
		"<p>The connections that are working right now, and the ones that hold a transaction or a lock:</p>" +
		"<ul>" +
		"    <li>executing something (Command is not Sleep),</li>" +
		"    <li>or has an open InnoDB transaction (HasOpenTxn is Y), even if it is not executing anything right now,</li>" +
		"    <li>or holds a metadata lock that another connection waits for (for example LOCK TABLES).</li>" +
		"</ul>" +
		"<p>Who blocks who (row locks and metadata locks), the locks of the connections that block or are blocked, " +
		"and the execution plan of statements that has run for a while (only if the login has ALL global privileges, see the tooltip of 'ExecPlan').</p>" +
		"Table Background colors:" +
		"<ul>" +
		"    <li>ORANGE - The connection was executing the same statement in the previous sample (it has run for more than one sample).</li>" +
		"    <li>YELLOW - An open transaction, but the connection is not executing anything (Command is Sleep): it waits for the client, and holds its locks.</li>" +
		"    <li>PINK   - The connection is blocked by another connection.</li>" +
		"    <li>RED    - The connection blocks other connections from working, and is not blocked itself.</li>" +
		"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "threads", "events_statements_current", "INNODB_TRX", "data_locks", "metadata_locks"};
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

	public static final String  PROPKEY_sample_pidLocks                   = CM_NAME + ".sample.pidLocks";
	public static final boolean DEFAULT_sample_pidLocks                   = true;

	private static final int    EXEC_PLAN_MAX_FETCH_PER_SAMPLE = 3;
	private static final int    EXEC_PLAN_CACHE_SIZE           = 200;
	private static final int    LOOKUP_QUERY_TIMEOUT_SEC       = 5;
	private static final int    LOCKS_SHOWN_PER_PID            = 100;  // 'PidLocks' shows the first ones (grouped by table, index, type, mode and status)

	/**
	 * Can the login do 'EXPLAIN FOR CONNECTION' of other logins statements? It needs ALL global privileges (MySQL source: GLOBAL_ACLS),
	 * so with the normal monitoring login (no access to user data) it is NOT allowed. null = not yet known, it is tried once.
	 */
	private Boolean _execPlanAllowed = null;

	/** The plan of a statement: key is "pid|statementEventId", so it is fetched once per statement execution */
	private Map<String, String> _execPlanCache = new LinkedHashMap<String, String>()
	{
		private static final long serialVersionUID = 1L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, String> eldest)
		{
			return size() > EXEC_PLAN_CACHE_SIZE;
		}
	};

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_execPlan                 , DEFAULT_sample_execPlan);
		Configuration.registerDefaultValue(PROPKEY_sample_execPlan_minExecTimeInSec, DEFAULT_sample_execPlan_minExecTimeInSec);
		Configuration.registerDefaultValue(PROPKEY_sample_pidLocks                 , DEFAULT_sample_pidLocks);
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		list.add(new CmSettingsHelper("Get Execution Plan"              , PROPKEY_sample_execPlan                 , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_execPlan                 , DEFAULT_sample_execPlan                 ), DEFAULT_sample_execPlan                 , "Get the execution plan (EXPLAIN FORMAT=JSON FOR CONNECTION) of statements that has run for a while. NOTE: this needs ALL global privileges for the login, otherwise it is turned off at the first try (with an INFO message in the log)." ));
		list.add(new CmSettingsHelper("Get Execution Plan, Min ExecTime", PROPKEY_sample_execPlan_minExecTimeInSec, Integer.class, conf.getIntProperty    (PROPKEY_sample_execPlan_minExecTimeInSec, DEFAULT_sample_execPlan_minExecTimeInSec), DEFAULT_sample_execPlan_minExecTimeInSec, "Only get the execution plan for statements that has run for more than ## seconds." ));
		list.add(new CmSettingsHelper("Get Locks"                       , PROPKEY_sample_pidLocks                 , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_pidLocks                 , DEFAULT_sample_pidLocks                 ), DEFAULT_sample_pidLocks                 , "Get the locks (row locks and metadata locks) of the connections that block others, or are blocked (the column 'PidLocks')." ));

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
			map.put("SqlText" , new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));
			map.put("ExecPlan", new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));

			// Set the map in the super
			setPcsColumnOptions(map);
		}

		return map;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("Pid");

		return pkCols;
	}

	/**
	 * One row per connection (Pid = the processlist id, CONNECTION_ID(), the Id of SHOW PROCESSLIST).<br>
	 * The blocking columns (BlockedByPid, BlockingOtherPids...), the "Has..." checkboxes and the HTML columns are filled in by localCalculation().
	 */
	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// NOTE: 'IF(1 = 0, t.PROCESSLIST_INFO, NULL)' is a NULL with the type LONGTEXT (for the columns that are filled in by localCalculation)
		//       The "Has..." columns are 0 here, and BOOLEAN (a checkbox) in modifyResultSetMetaData()
		//       The statement is the one in events_statements_current: the running one, or (Command is Sleep) the last one. ExecTimeInSec is only for a running statement.
		String sql = ""
			+ "select /* ${cmCollectorName} */ \n"
			+ "     t.PROCESSLIST_ID                                         as Pid \n"
			+ "    ,t.THREAD_ID                                              as ThreadId \n"
			+ "    ,CAST('' as CHAR(3))                                      as MultiSampled \n"
			+ "    ,t.PROCESSLIST_USER                                       as UserName \n"
			+ "    ,t.PROCESSLIST_HOST                                       as Host \n"
			+ "    ,a.ATTR_VALUE                                             as ProgramName \n"
			+ "    ,t.PROCESSLIST_DB                                         as CurrentSchema \n"
			+ "    ,t.PROCESSLIST_COMMAND                                    as Command \n"
			+ "    ,t.PROCESSLIST_STATE                                      as State \n"
			+ "    ,CAST(IF(s.EVENT_ID is not null and s.END_EVENT_ID is null, s.TIMER_WAIT / 1000000000000.0, 0) as DECIMAL(12,1)) as ExecTimeInSec \n"
			+ "    ,t.PROCESSLIST_TIME                                       as TimeInStateSec \n"
			+ "    ,CASE WHEN x.trx_id is not null THEN 'Y' ELSE 'N' END     as HasOpenTxn \n"
			+ "    ,x.trx_state                                              as TxnState \n"
			+ "    ,x.trx_started                                            as TxnStartTime \n"
			+ "    ,CAST(TIMESTAMPDIFF(SECOND, x.trx_started, NOW()) as SIGNED) as TxnAgeInSec \n"
			+ "    ,x.trx_rows_locked                                        as TxnRowsLocked \n"
			+ "    ,x.trx_rows_modified                                      as TxnRowsModified \n"
			+ "    ,CAST(0  as SIGNED)                                       as BlockedByPid \n"
			+ "    ,CAST(0  as SIGNED)                                       as BlockedSeconds \n"
			+ "    ,CAST('' as CHAR(10))                                     as BlockedLockType \n"
			+ "    ,CAST('' as CHAR(255))                                    as BlockedOn \n"
			+ "    ,CAST('' as CHAR(1000))                                   as BlockingOtherPids \n"
			+ "    ,CAST(0  as SIGNED)                                       as BlockingOthersMaxSec \n"
			+ "    ,CAST(0  as SIGNED)                                       as RootBlockerPid \n"
			+ "    ,s.EVENT_ID                                               as StmntEventId \n"
			+ "    ,s.DIGEST                                                 as Digest \n"
			+ "    ,s.ROWS_EXAMINED                                          as RowsExamined \n"
			+ "    ,s.ROWS_SENT                                              as RowsSent \n"
			+ "    ,s.ROWS_AFFECTED                                          as RowsAffected \n"
			+ "    ,s.CREATED_TMP_DISK_TABLES                                as TmpDiskTables \n"
			+ "    ,s.SELECT_FULL_JOIN                                       as FullJoins \n"
			+ "    ,s.NO_INDEX_USED                                          as NoIndexUsed \n"
			+ "    ,CAST(s.LOCK_TIME / 1000000000000.0 as DECIMAL(12,3))     as LockTimeSec \n"
			+ "    ,0                                                        as HasSqlText \n"
			+ "    ,0                                                        as HasExecPlan \n"
			+ "    ,0                                                        as HasPidLocks \n"
			+ "    ,0                                                        as HasBlockedPidsInfo \n"
			+ "    ,t.PROCESSLIST_INFO                                       as SqlText \n"
			+ "    ,IF(1 = 0, t.PROCESSLIST_INFO, NULL)                      as ExecPlan \n"
			+ "    ,IF(1 = 0, t.PROCESSLIST_INFO, NULL)                      as PidLocks \n"
			+ "    ,IF(1 = 0, t.PROCESSLIST_INFO, NULL)                      as BlockedPidsInfo \n"
			+ "from performance_schema.threads t \n"
			+ "left join performance_schema.events_statements_current s on s.THREAD_ID = t.THREAD_ID \n"
			+ "left join information_schema.INNODB_TRX                x on x.trx_mysql_thread_id = t.PROCESSLIST_ID \n"
			+ "left join performance_schema.session_connect_attrs     a on a.PROCESSLIST_ID = t.PROCESSLIST_ID and a.ATTR_NAME = 'program_name' \n"
			+ "where t.TYPE = 'FOREGROUND' \n"
			+ "  and t.PROCESSLIST_ID <> CONNECTION_ID() \n"
			+ "  and (   t.PROCESSLIST_COMMAND not in ('Sleep', 'Daemon', 'Binlog Dump', 'Binlog Dump GTID') \n"
			+ "       or x.trx_id is not null \n"
			+ "       or t.PROCESSLIST_ID in (select blocking_pid from sys.x$schema_table_lock_waits) \n"
			+ "      ) \n"
			+ "order by t.PROCESSLIST_ID \n"
			+ "";

		return sql;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return MySqlCmUtils.checkViewsAreReadable(this, conn,
				"performance_schema.threads", "performance_schema.events_statements_current", "performance_schema.session_connect_attrs",
				"information_schema.INNODB_TRX", "sys.x$innodb_lock_waits", "sys.x$schema_table_lock_waits");
	}

	/**
	 * The "Has..." columns are 0 in the SQL, here they are made into BOOLEAN, so they are shown as checkboxes.<br>
	 * They are set to true in localCalculation(), when the row has a SQL Text / an Execution Plan / locks / blocked connections.
	 */
	@Override
	public ResultSetMetaDataCached modifyResultSetMetaData(ResultSetMetaDataCached rsmdc)
	{
		for (String colName : new String[] { "HasSqlText", "HasExecPlan", "HasPidLocks", "HasBlockedPidsInfo" })
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

	/** The SQL Text, the Execution Plan and the HTML tables (locks, blocked connections) as a tooltip, on the checkbox column and on the column itself (as the other DbxTune's does) */
	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol)
	{
		if ("HasSqlText".equals(colName) || "SqlText".equals(colName))
		{
			int pos_SqlText = findColumn("SqlText");
			if (pos_SqlText >= 0)
			{
				Object sqlText = getValueAt(modelRow, pos_SqlText);
				if (sqlText != null && StringUtil.hasValue(sqlText.toString()))
					return "<html><pre>" + StringEscapeUtils.escapeHtml4(StringUtil.makeApproxLineBreak(sqlText.toString(), 150, 10, "\n")) + "</pre></html>";
			}
		}

		if ("HasExecPlan".equals(colName) || "ExecPlan".equals(colName))
		{
			int pos_ExecPlan = findColumn("ExecPlan");
			if (pos_ExecPlan >= 0)
			{
				Object execPlan = getValueAt(modelRow, pos_ExecPlan);
				if (execPlan != null && StringUtil.hasValue(execPlan.toString()))
					return "<html><pre>" + StringEscapeUtils.escapeHtml4(execPlan.toString()) + "</pre></html>";
			}
		}

		// The HTML tables: locks, and who I am blocking
		String htmlColName = null;
		if ("HasPidLocks"       .equals(colName) || "PidLocks"       .equals(colName)) htmlColName = "PidLocks";
		if ("HasBlockedPidsInfo".equals(colName) || "BlockedPidsInfo".equals(colName)) htmlColName = "BlockedPidsInfo";
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
	 * Set 'MultiSampled' to YES if the connection was executing the SAME statement (same statement event id) in the previous sample
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample)
	{
		int pos_MultiSampled  = newSample.findColumn("MultiSampled");
		int pos_StmntEventId  = newSample.findColumn("StmntEventId");
		int pos_ExecTimeInSec = newSample.findColumn("ExecTimeInSec");

		if (prevSample == null || pos_MultiSampled == -1 || pos_StmntEventId == -1 || pos_ExecTimeInSec == -1)
			return;

		for (int rowId = 0; rowId < newSample.getRowCount(); rowId++)
		{
			// Only a running statement (ExecTimeInSec is 0 when Command is Sleep: then it is the last statement, which has ended)
			if (newSample.getValueAsDouble(rowId, pos_ExecTimeInSec, 0d) <= 0)
				continue;

			int prevRowId = prevSample.getRowNumberForPkValue(newSample.getPkValue(rowId));
			if (prevRowId < 0)
				continue;

			long thisEventId = newSample .getValueAsLong(rowId    , pos_StmntEventId, -1L);
			long prevEventId = prevSample.getValueAsLong(prevRowId, pos_StmntEventId, -2L);

			if (thisEventId > 0 && thisEventId == prevEventId)
				newSample.setValueAt("YES", rowId, pos_MultiSampled);
		}
	}

	/**
	 * Fill in: who blocks who (from sys.x$innodb_lock_waits and sys.x$schema_table_lock_waits), 'BlockingOtherPids' + 'BlockingOthersMaxSec' + 'RootBlockerPid' (from the other rows),
	 * the locks, 'BlockedPidsInfo' and 'ExecPlan'
	 */
	@Override
	public void localCalculation(CounterSample newSample)
	{
		int pos_Pid                  = newSample.findColumn("Pid");
		int pos_BlockedByPid         = newSample.findColumn("BlockedByPid");
		int pos_BlockedSeconds       = newSample.findColumn("BlockedSeconds");
		int pos_BlockedLockType      = newSample.findColumn("BlockedLockType");
		int pos_BlockedOn            = newSample.findColumn("BlockedOn");
		int pos_BlockingOtherPids    = newSample.findColumn("BlockingOtherPids");
		int pos_BlockingOthersMaxSec = newSample.findColumn("BlockingOthersMaxSec");
		int pos_RootBlockerPid       = newSample.findColumn("RootBlockerPid");

		if (pos_Pid == -1 || pos_BlockedByPid == -1 || pos_BlockedSeconds == -1 || pos_BlockedLockType == -1 || pos_BlockedOn == -1 || pos_BlockingOtherPids == -1 || pos_BlockingOthersMaxSec == -1 || pos_RootBlockerPid == -1)
		{
			_logger.warn(getName() + ": localCalculation(), Problems finding one of the columns: Pid, BlockedByPid, BlockedSeconds, BlockedLockType, BlockedOn, BlockingOtherPids, BlockingOthersMaxSec, RootBlockerPid.");
			return;
		}

		// Where each connection is in the sample
		Map<Long, Integer> pidToRowId = new HashMap<>();
		for (int rowId = 0; rowId < newSample.getRowCount(); rowId++)
			pidToRowId.put(newSample.getValueAsLong(rowId, pos_Pid, -1L), rowId);

		Configuration conf = Configuration.getCombinedConfiguration();
		DbxConnection conn = getCounterController() == null ? null : getCounterController().getMonConnection();

		//---------------------------------------------------------------
		// Who waits for who: ROW locks (InnoDB) first, they are exact. Then METADATA locks (for example an ALTER TABLE that waits for an open transaction)
		// For a metadata lock wait, the view lists ALL connections that holds a lock on the table: we pick one that is not blocked itself (the root cause)
		//---------------------------------------------------------------
		if (conn != null && newSample.getRowCount() > 0)
		{
			String sql = "select waiting_pid, blocking_pid, wait_age_secs, locked_table, locked_index, locked_type, waiting_lock_mode, blocking_lock_mode from sys.x$innodb_lock_waits";
			try (Statement stmnt = conn.createStatement())
			{
				stmnt.setQueryTimeout(LOOKUP_QUERY_TIMEOUT_SEC);
				try (ResultSet rs = stmnt.executeQuery(sql))
				{
					while (rs.next())
					{
						Integer rowId = pidToRowId.get(rs.getLong(1));
						if (rowId == null || newSample.getValueAsLong(rowId, pos_BlockedByPid, 0L) != 0)
							continue;

						String on = rs.getString(4) + (rs.getString(5) == null ? "" : " " + rs.getString(5)) + " (" + rs.getString(6) + " " + rs.getString(7) + ", blocker has " + rs.getString(8) + ")";
						newSample.setValueAt(Long.valueOf(rs.getLong(2))             , rowId, pos_BlockedByPid);
						newSample.setValueAt(Long.valueOf(rs.getLong(3))             , rowId, pos_BlockedSeconds);
						newSample.setValueAt("ROW"                                   , rowId, pos_BlockedLockType);
						newSample.setValueAt(StringUtil.truncate(on, 255, true, null), rowId, pos_BlockedOn);
					}
				}
			}
			catch (SQLException ex)
			{
				_logger.info(getName() + ": Problems getting the row lock waits. (the blocking columns are empty for this sample). sql=|" + sql + "|, Caught: " + ex);
			}

			sql = "select waiting_pid, blocking_pid, waiting_query_secs, object_schema, object_name, waiting_lock_type, blocking_lock_type from sys.x$schema_table_lock_waits where waiting_pid <> blocking_pid";
			try (Statement stmnt = conn.createStatement())
			{
				stmnt.setQueryTimeout(LOOKUP_QUERY_TIMEOUT_SEC);

				// waiting pid -> the candidates that blocks it
				Map<Long, List<Object[]>> mdlWaits = new LinkedHashMap<>();
				try (ResultSet rs = stmnt.executeQuery(sql))
				{
					while (rs.next())
						mdlWaits.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>()).add(new Object[] { rs.getLong(2), rs.getLong(3), rs.getString(4) + "." + rs.getString(5), rs.getString(6), rs.getString(7) });
				}

				for (Map.Entry<Long, List<Object[]>> entry : mdlWaits.entrySet())
				{
					Integer rowId = pidToRowId.get(entry.getKey());
					if (rowId == null || newSample.getValueAsLong(rowId, pos_BlockedByPid, 0L) != 0)
						continue;

					// The holder that is not waiting itself, otherwise the first one
					Object[] blocker = entry.getValue().get(0);
					for (Object[] candidate : entry.getValue())
					{
						if ( ! mdlWaits.containsKey((Long)candidate[0]) && ! isRowLockWaiter(newSample, pidToRowId, (Long)candidate[0], pos_BlockedByPid) )
						{
							blocker = candidate;
							break;
						}
					}

					String others = entry.getValue().size() > 1 ? ", " + (entry.getValue().size() - 1) + " more connection(s) hold a lock on it" : "";
					String on     = "metadata lock on " + blocker[2] + " (wants " + blocker[3] + ", blocker has " + blocker[4] + others + ")";
					newSample.setValueAt(blocker[0]                              , rowId, pos_BlockedByPid);
					newSample.setValueAt(blocker[1]                              , rowId, pos_BlockedSeconds);
					newSample.setValueAt("METADATA"                              , rowId, pos_BlockedLockType);
					newSample.setValueAt(StringUtil.truncate(on, 255, true, null), rowId, pos_BlockedOn);
				}
			}
			catch (SQLException ex)
			{
				_logger.info(getName() + ": Problems getting the metadata lock waits. (the blocking columns are empty for this sample). sql=|" + sql + "|, Caught: " + ex);
			}
		}

		//---------------------------------------------------------------
		// Who am I blocking: the rows where 'BlockedByPid' is my Pid
		// NOTE: 'BlockedByPid' is 0 when the connection is not blocked (there is no processlist id 0)
		//---------------------------------------------------------------
		for (int rowId = 0; rowId < newSample.getRowCount(); rowId++)
		{
			long pid = newSample.getValueAsLong(rowId, pos_Pid, -1L);

			String blockingOtherPids    = "";
			long   blockingOthersMaxSec = 0;

			for (int r = 0; r < newSample.getRowCount(); r++)
			{
				if (newSample.getValueAsLong(r, pos_BlockedByPid, 0L) != pid)
					continue;

				blockingOtherPids   += (blockingOtherPids.isEmpty() ? "" : ", ") + newSample.getValueAsLong(r, pos_Pid, -1L);
				blockingOthersMaxSec = Math.max(blockingOthersMaxSec, newSample.getValueAsLong(r, pos_BlockedSeconds, 0L));
			}

			if ( ! blockingOtherPids.isEmpty() )
			{
				newSample.setValueAt(StringUtil.truncate(blockingOtherPids, 1000, true, null), rowId, pos_BlockingOtherPids);
				newSample.setValueAt(Long.valueOf(blockingOthersMaxSec)                      , rowId, pos_BlockingOthersMaxSec);
			}
		}

		//---------------------------------------------------------------
		// RootBlockerPid: follow 'BlockedByPid' to the connection that is not blocked itself
		//---------------------------------------------------------------
		for (int rowId = 0; rowId < newSample.getRowCount(); rowId++)
		{
			long rootPid = newSample.getValueAsLong(rowId, pos_BlockedByPid, 0L);
			if (rootPid <= 0)
				continue;

			// Max as many steps as there are rows: in a deadlock the connections wait for each other (until InnoDB breaks it)
			for (int step = 0; step < newSample.getRowCount(); step++)
			{
				Integer r = pidToRowId.get(rootPid);
				long nextPid = r == null ? 0 : newSample.getValueAsLong(r, pos_BlockedByPid, 0L);
				if (nextPid <= 0)
					break;
				rootPid = nextPid;
			}

			newSample.setValueAt(Long.valueOf(rootPid), rowId, pos_RootBlockerPid);
		}

		//---------------------------------------------------------------
		// The checkbox 'HasSqlText': true when the row has a SQL Text
		//---------------------------------------------------------------
		int pos_HasSqlText = newSample.findColumn("HasSqlText");
		int pos_SqlText    = newSample.findColumn("SqlText");
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
		if (conn != null && conf.getBooleanProperty(PROPKEY_sample_pidLocks, DEFAULT_sample_pidLocks))
			lookupPidLocks(conn, newSample);

		// Who am I blocking: what they execute, and their locks (from the other rows of the sample, so AFTER the lookups)
		setBlockedPidsInfo(newSample);

		//---------------------------------------------------------------
		// Execution Plan: for statements that has been executing for a while
		// A plan is fetched ONCE (then it is in the cache), and only a few NEW plans in each sample (so we do not slow down the collector)
		//---------------------------------------------------------------
		if (conn != null && conf.getBooleanProperty(PROPKEY_sample_execPlan, DEFAULT_sample_execPlan) && ! Boolean.FALSE.equals(_execPlanAllowed))
			lookupExecPlans(conn, newSample, conf.getIntProperty(PROPKEY_sample_execPlan_minExecTimeInSec, DEFAULT_sample_execPlan_minExecTimeInSec));
	}

	/** Is this connection blocked by a ROW lock (set above, before the metadata locks are looked at) */
	private static boolean isRowLockWaiter(CounterSample newSample, Map<Long, Integer> pidToRowId, long pid, int pos_BlockedByPid)
	{
		Integer rowId = pidToRowId.get(pid);
		return rowId != null && newSample.getValueAsLong(rowId, pos_BlockedByPid, 0L) != 0;
	}

	/**
	 * 'PidLocks' (a HTML table) and 'HasPidLocks': the row locks (performance_schema.data_locks, grouped) and the metadata locks of the connections
	 * that block others, or are blocked. ONE statement per kind for those connections, with a short timeout.
	 */
	private void lookupPidLocks(DbxConnection conn, CounterSample newSample)
	{
		int pos_ThreadId           = newSample.findColumn("ThreadId");
		int pos_BlockedByPid       = newSample.findColumn("BlockedByPid");
		int pos_BlockingOtherPids  = newSample.findColumn("BlockingOtherPids");
		int pos_PidLocks           = newSample.findColumn("PidLocks");
		int pos_HasPidLocks        = newSample.findColumn("HasPidLocks");

		if (pos_ThreadId == -1 || pos_BlockedByPid == -1 || pos_BlockingOtherPids == -1 || pos_PidLocks == -1 || pos_HasPidLocks == -1)
			return;

		// The threads to get the locks for (data_locks and metadata_locks has an index on THREAD_ID)
		Map<Long, Integer> threadToRowId = new HashMap<>();
		String             threadList    = "";
		for (int rowId = 0; rowId < newSample.getRowCount(); rowId++)
		{
			boolean isBlocked  = newSample.getValueAsLong(rowId, pos_BlockedByPid, 0L) > 0;
			boolean isBlocking = StringUtil.hasValue(newSample.getValueAsString(rowId, pos_BlockingOtherPids));
			if ( ! isBlocked && ! isBlocking )
				continue;

			long threadId = newSample.getValueAsLong(rowId, pos_ThreadId, -1L);
			threadToRowId.put(threadId, rowId);
			threadList += (threadList.isEmpty() ? "" : ", ") + threadId;
		}

		if (threadList.isEmpty())
			return;

		Map<Long, StringBuilder> locksPerThread = new HashMap<>();
		Map<Long, Integer>       countPerThread = new HashMap<>();

		String sql = ""
			+ "select THREAD_ID, 'ROW' as KIND, concat(OBJECT_SCHEMA, '.', OBJECT_NAME) as OBJ, INDEX_NAME, LOCK_TYPE, LOCK_MODE, LOCK_STATUS, count(*) as CNT \n"
			+ "from performance_schema.data_locks \n"
			+ "where THREAD_ID in (" + threadList + ") \n"
			+ "group by THREAD_ID, OBJECT_SCHEMA, OBJECT_NAME, INDEX_NAME, LOCK_TYPE, LOCK_MODE, LOCK_STATUS \n"
			+ "union all \n"
			+ "select OWNER_THREAD_ID, 'METADATA', concat(OBJECT_TYPE, ' ', IFNULL(OBJECT_SCHEMA, ''), IF(OBJECT_NAME is null, '', concat('.', OBJECT_NAME))), NULL, LOCK_DURATION, LOCK_TYPE, LOCK_STATUS, count(*) \n"
			+ "from performance_schema.metadata_locks \n"
			+ "where OWNER_THREAD_ID in (" + threadList + ") \n"
			+ "  and OBJECT_TYPE in ('TABLE', 'GLOBAL', 'SCHEMA', 'BACKUP LOCK', 'BACKUP TABLES') \n"
			+ "group by OWNER_THREAD_ID, OBJECT_TYPE, OBJECT_SCHEMA, OBJECT_NAME, LOCK_DURATION, LOCK_TYPE, LOCK_STATUS \n"
			+ "order by 1, 7 desc, 3";
		try (Statement stmnt = conn.createStatement())
		{
			stmnt.setQueryTimeout(LOOKUP_QUERY_TIMEOUT_SEC);
			try (ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
				{
					long threadId = rs.getLong(1);
					int  count    = countPerThread.merge(threadId, 1, Integer::sum);
					if (count > LOCKS_SHOWN_PER_PID)
						continue;

					String status = rs.getString(7);
					StringBuilder sb = locksPerThread.computeIfAbsent(threadId, k -> new StringBuilder());
					sb.append("<TR").append("WAITING".equals(status) || "PENDING".equals(status) ? " style='color:red'" : "").append(">");
					sb.append("<TD>").append(rs.getString(2)).append("</TD>");
					sb.append("<TD>").append(StringEscapeUtils.escapeHtml4(rs.getString(3))).append("</TD>");
					sb.append("<TD>").append(rs.getString(4) == null ? "" : StringEscapeUtils.escapeHtml4(rs.getString(4))).append("</TD>");
					sb.append("<TD>").append(rs.getString(5)).append("</TD>");
					sb.append("<TD>").append(rs.getString(6)).append("</TD>");
					sb.append("<TD>").append(status).append("</TD>");
					sb.append("<TD ALIGN='right'>").append(rs.getLong(8)).append("</TD>");
					sb.append("</TR>");
				}
			}
		}
		catch (SQLException ex)
		{
			_logger.info(getName() + ": Problems getting the locks (column 'PidLocks' is empty for this sample). Caught: " + ex);
			return;
		}

		for (Map.Entry<Long, StringBuilder> entry : locksPerThread.entrySet())
		{
			Integer rowId = threadToRowId.get(entry.getKey());
			if (rowId == null)
				continue;

			int count = countPerThread.getOrDefault(entry.getKey(), 0);

			// (it starts with "<TABLE": that is how CountersModel.toHtmlTableString() knows that it is HTML, when it makes the alarm description)
			String html = "<TABLE BORDER=1 class='dbx-table-basic'>"
					+ "<TR> <TH>Kind</TH> <TH>Object</TH> <TH>Index</TH> <TH>Type / Duration</TH> <TH>Mode</TH> <TH>Status</TH> <TH>Count</TH> </TR>"
					+ entry.getValue()
					+ (count > LOCKS_SHOWN_PER_PID ? "<TR> <TD COLSPAN=7><I>... and " + (count - LOCKS_SHOWN_PER_PID) + " more lines</I></TD> </TR>" : "")
					+ "</TABLE>";

			newSample.setValueAt(html        , rowId, pos_PidLocks);
			newSample.setValueAt(Boolean.TRUE, rowId, pos_HasPidLocks);
		}
	}

	/**
	 * 'BlockedPidsInfo' (a HTML table) and 'HasBlockedPidsInfo': for a connection that blocks others, the connections that wait for it:
	 * who they are, what they execute, what they wait for and their locks. All of it from the other rows of the sample.
	 */
	private void setBlockedPidsInfo(CounterSample newSample)
	{
		int pos_Pid                = newSample.findColumn("Pid");
		int pos_BlockedByPid       = newSample.findColumn("BlockedByPid");
		int pos_SqlText            = newSample.findColumn("SqlText");
		int pos_PidLocks           = newSample.findColumn("PidLocks");
		int pos_HasBlockedPidsInfo = newSample.findColumn("HasBlockedPidsInfo");
		int pos_BlockedPidsInfo    = newSample.findColumn("BlockedPidsInfo");

		if (pos_Pid == -1 || pos_BlockedByPid == -1 || pos_SqlText == -1 || pos_PidLocks == -1 || pos_HasBlockedPidsInfo == -1 || pos_BlockedPidsInfo == -1)
			return;

		// The columns of the blocked connection that goes into the "Info" cell (the ones that have a value)
		String[] infoColumns = { "UserName", "Host", "ProgramName", "CurrentSchema", "Command", "State", "BlockedSeconds", "BlockedLockType", "BlockedOn", "TxnStartTime" };

		for (int rowId = 0; rowId < newSample.getRowCount(); rowId++)
		{
			long pid = newSample.getValueAsLong(rowId, pos_Pid, -1L);

			StringBuilder sb = new StringBuilder();

			for (int r = 0; r < newSample.getRowCount(); r++)
			{
				if (newSample.getValueAsLong(r, pos_BlockedByPid, 0L) != pid)
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

				String locks = newSample.getValueAsString(r, pos_PidLocks);

				sb.append("  <TR>\n");
				sb.append("    <TD><B>").append(newSample.getValueAsLong(r, pos_Pid, -1L)).append("</B></TD>\n");
				sb.append("    <TD>").append(info).append("</TD>\n");
				sb.append("    <TD><PRE>").append(sqlText == null ? "" : StringEscapeUtils.escapeHtml4(sqlText)).append("</PRE></TD>\n");
				sb.append("    <TD>").append(locks == null ? "" : locks).append("</TD>\n");
				sb.append("  </TR>\n");
			}

			if (sb.length() > 0)
			{
				// (it starts with "<TABLE": that is how CountersModel.toHtmlTableString() knows that it is HTML, when it makes the alarm description)
				String html = "<TABLE BORDER=1 class='dbx-table-basic'>\n"
						+ "  <TR> <TH>Blocked Pid</TH> <TH>Info</TH> <TH>SQL Text</TH> <TH>Locks</TH> </TR>\n"
						+ sb
						+ "</TABLE>\n";

				newSample.setValueAt(html        , rowId, pos_BlockedPidsInfo);
				newSample.setValueAt(Boolean.TRUE, rowId, pos_HasBlockedPidsInfo);
			}
		}
	}

	/**
	 * 'ExecPlan' and 'HasExecPlan': EXPLAIN FORMAT=JSON FOR CONNECTION, for statements that has run for more than 'minExecTimeInSec'.
	 * <p>
	 * MySQL only allows it for another login's statement when the login has ALL global privileges (MySQL source: GLOBAL_ACLS & ~GRANT_ACL),
	 * otherwise error 1045 'Access denied'. That is tested at the first try: if it is not allowed, it is turned off with one INFO message.
	 */
	private void lookupExecPlans(DbxConnection conn, CounterSample newSample, int minExecTimeInSec)
	{
		int pos_Pid           = newSample.findColumn("Pid");
		int pos_StmntEventId  = newSample.findColumn("StmntEventId");
		int pos_ExecTimeInSec = newSample.findColumn("ExecTimeInSec");
		int pos_ExecPlan      = newSample.findColumn("ExecPlan");
		int pos_HasExecPlan   = newSample.findColumn("HasExecPlan");

		if (pos_Pid == -1 || pos_StmntEventId == -1 || pos_ExecTimeInSec == -1 || pos_ExecPlan == -1 || pos_HasExecPlan == -1)
			return;

		int fetchCount = 0;

		for (int rowId = 0; rowId < newSample.getRowCount(); rowId++)
		{
			// Only for statements that are running, and has done so for a while
			if (newSample.getValueAsDouble(rowId, pos_ExecTimeInSec, 0d) < minExecTimeInSec)
				continue;

			long   pid      = newSample.getValueAsLong(rowId, pos_Pid, -1L);
			String cacheKey = pid + "|" + newSample.getValueAsString(rowId, pos_StmntEventId);

			String execPlan = _execPlanCache.get(cacheKey);
			if (execPlan == null && fetchCount < EXEC_PLAN_MAX_FETCH_PER_SAMPLE)
			{
				fetchCount++;

				StringBuilder sb = new StringBuilder();
				try (Statement stmnt = conn.createStatement())
				{
					stmnt.setQueryTimeout(2);
					try (ResultSet rs = stmnt.executeQuery("EXPLAIN FORMAT=JSON FOR CONNECTION " + pid))
					{
						while (rs.next())
							sb.append(rs.getString(1));
					}
					_execPlanAllowed = Boolean.TRUE;
				}
				catch (SQLException ex)
				{
					// 1045 = Access denied: the login does not have ALL global privileges, so we can never do it (no need to try again)
					if (ex.getErrorCode() == 1045)
					{
						_execPlanAllowed = Boolean.FALSE;
						_logger.info(getName() + ": The execution plans of running statements are NOT collected: 'EXPLAIN FOR CONNECTION' of another login's statement needs ALL global privileges (the normal monitoring login has no access to user data). "
								+ "To turn off this try, set '" + PROPKEY_sample_execPlan + "=false'. Caught: " + ex.getMessage());
						return;
					}

					// Other errors: the statement can not be explained (it is not a SELECT/INSERT/UPDATE/DELETE/REPLACE), or it has ended. Not a problem: just no plan for this row
					_logger.debug(getName() + ": Problems getting execution plan for pid=" + pid + ". Caught: " + ex);
					_execPlanCache.put(cacheKey, "");
					continue;
				}

				execPlan = sb.toString();
				_execPlanCache.put(cacheKey, execPlan);
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
			final long pid      = cm.getAbsValueAsLong(r, "Pid", -1L);
			String     userName = cm.getAbsString(r, "UserName");
			String     host     = cm.getAbsString(r, "Host");
			String     command  = cm.getAbsString(r, "Command");

			//-------------------------------------------------------
			// ImBlockingOthersMaxTimeInSec: This connection blocks others, and is NOT blocked itself (it is the root cause)
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("ImBlockingOthersMaxTimeInSec"))
			{
				String blockingOtherPids    = cm.getAbsString(r, "BlockingOtherPids");
				long   blockedByPid         = cm.getAbsValueAsLong(r, "BlockedByPid"        , 0L);
				long   blockingOthersMaxSec = cm.getAbsValueAsLong(r, "BlockingOthersMaxSec", 0L);

				if (StringUtil.hasValue(blockingOtherPids) && blockedByPid == 0)
				{
					int threshold = conf.getIntProperty(PROPKEY_alarm_ImBlockingOthersMaxTimeInSec, DEFAULT_alarm_ImBlockingOthersMaxTimeInSec);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest(" + cm.getName() + "): ImBlockingOthersMaxTimeInSec: pid=" + pid + ", threshold=" + threshold + ", BlockingOthersMaxSec='" + blockingOthersMaxSec + "', BlockingOtherPids='" + blockingOtherPids + "'.");

					if (blockingOthersMaxSec > threshold)
					{
						int blockCount = StringUtil.commaStrToList(blockingOtherPids).size();

						AlarmEvent ae = new AlarmEventMyBlockingSession(cm, threshold, pid, userName, host, command, blockingOthersMaxSec, "[" + blockingOtherPids + "]", blockCount);

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
									int    rowId = cm.getRowIdWhere(DATA_ABS, "Pid", pid);
									String label = "pid " + pid;

									if (rowId == -1)
										alarmEvent.setCancelDescription(label + " has no active statement or open transaction now");
									else
									{
										String BlockingOtherPids    = cm.getAbsString(rowId, "BlockingOtherPids");
										Object BlockingOthersMaxSec = cm.getAbsValue (rowId, "BlockingOthersMaxSec");

										String cancelMsg = StringUtil.isNullOrBlank(BlockingOtherPids)
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
			// HoldingLocksWhileWaitForClientInputInSec: an open transaction, and the connection is waiting for the client (Command is Sleep)
			// 'TimeInStateSec' is for a sleeping connection: how long since the last statement ended
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("HoldingLocksWhileWaitForClientInputInSec"))
			{
				String hasOpenTxn     = cm.getAbsString(r, "HasOpenTxn");
				long   timeInStateSec = cm.getAbsValueAsLong(r, "TimeInStateSec", 0L);

				if ("Y".equals(hasOpenTxn) && "Sleep".equals(command))
				{
					int threshold = conf.getIntProperty(PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSec, DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSec);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest(" + cm.getName() + "): HoldingLocksWhileWaitForClientInputInSec: pid=" + pid + ", threshold=" + threshold + ", TimeInStateSec='" + timeInStateSec + "', Command='" + command + "', HasOpenTxn='" + hasOpenTxn + "'.");

					if (timeInStateSec > threshold)
					{
						String txnStartTime = cm.getAbsString(r, "TxnStartTime");
						long   rowsLocked   = cm.getAbsValueAsLong(r, "TxnRowsLocked", 0L);

						AlarmEvent ae = new AlarmEventHoldingLocksWhileWaitForClientInput(cm, threshold, pid, (int) timeInStateSec, txnStartTime, rowsLocked > 0);

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
									int    rowId = cm.getRowIdWhere(DATA_ABS, "Pid", pid);
									String label = "pid " + pid;

									if (rowId == -1)
										alarmEvent.setCancelDescription(label + " has no open transaction now");
									else
									{
										Object Command        = cm.getAbsValue(rowId, "Command");
										Object TimeInStateSec = cm.getAbsValue(rowId, "TimeInStateSec");

										String cancelMsg = getAlarmCancelText(label + ": command", "'" + Command + "' for " + TimeInStateSec + " seconds", " seconds");
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
				double execTimeInSec = cm.getAbsValueAsDouble(r, "ExecTimeInSec", 0d);

				if (execTimeInSec > 0)
				{
					int threshold = conf.getIntProperty(PROPKEY_alarm_StatementExecInSec, DEFAULT_alarm_StatementExecInSec);

					// Skip some users or programs (for example backup or batch jobs that we know takes a long time)
					String skipUserRegex    = conf.getProperty(PROPKEY_alarm_StatementExecInSecSkipUser   , DEFAULT_alarm_StatementExecInSecSkipUser);
					String skipProgramRegex = conf.getProperty(PROPKEY_alarm_StatementExecInSecSkipProgram, DEFAULT_alarm_StatementExecInSecSkipProgram);
					String program          = cm.getAbsString(r, "ProgramName");

					boolean skip = (StringUtil.hasValue(skipUserRegex)    && userName.matches(skipUserRegex))
					            || (StringUtil.hasValue(skipProgramRegex) && program .matches(skipProgramRegex));

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest(" + cm.getName() + "): StatementExecInSec: pid=" + pid + ", threshold=" + threshold + ", ExecTimeInSec='" + execTimeInSec + "', skip=" + skip + ", UserName='" + userName + "', ProgramName='" + program + "'.");

					if (execTimeInSec > threshold && ! skip)
					{
						AlarmEvent ae = new AlarmEventMyLongRunningStatement(cm, threshold, pid, (long) execTimeInSec, userName, host, cm.getAbsString(r, "CurrentSchema"), cm.getAbsString(r, "Digest"));

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
									int    rowId = cm.getRowIdWhere(DATA_ABS, "Pid", pid);
									String label = "pid " + pid;

									if (rowId == -1)
										alarmEvent.setCancelDescription(label + " has no active statement now");
									else
									{
										Object Command       = cm.getAbsValue(rowId, "Command");
										Object ExecTimeInSec = cm.getAbsValue(rowId, "ExecTimeInSec");

										String cancelMsg = getAlarmCancelText(label + ": command", "'" + Command + "', statement running for " + ExecTimeInSec + " seconds", " seconds");
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

		list.add(new CmSettingsHelper("ImBlockingOthersMaxTimeInSec"            , isAlarmSwitch, PROPKEY_alarm_ImBlockingOthersMaxTimeInSec            , Integer.class, conf.getIntProperty(PROPKEY_alarm_ImBlockingOthersMaxTimeInSec            , DEFAULT_alarm_ImBlockingOthersMaxTimeInSec            ), DEFAULT_alarm_ImBlockingOthersMaxTimeInSec            , "If 'BlockingOthersMaxSec' is greater than ## seconds (and the connection is not blocked itself) then send 'AlarmEventMyBlockingSession'." ));
		list.add(new CmSettingsHelper("HoldingLocksWhileWaitForClientInputInSec", isAlarmSwitch, PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSec, Integer.class, conf.getIntProperty(PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSec, DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSec), DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSec, "If a connection has an open transaction ('HasOpenTxn' is Y), its Command is Sleep (it waits for the client) and 'TimeInStateSec' is greater than ## seconds then send 'AlarmEventHoldingLocksWhileWaitForClientInput'." ));
		list.add(new CmSettingsHelper("StatementExecInSec"                      , isAlarmSwitch, PROPKEY_alarm_StatementExecInSec                      , Integer.class, conf.getIntProperty(PROPKEY_alarm_StatementExecInSec                      , DEFAULT_alarm_StatementExecInSec                      ), DEFAULT_alarm_StatementExecInSec                      , "If a statement is running and 'ExecTimeInSec' is greater than ## seconds then send 'AlarmEventMyLongRunningStatement'." ));
		list.add(new CmSettingsHelper("StatementExecInSec SkipUser"             ,                PROPKEY_alarm_StatementExecInSecSkipUser              , String .class, conf.getProperty   (PROPKEY_alarm_StatementExecInSecSkipUser              , DEFAULT_alarm_StatementExecInSecSkipUser              ), DEFAULT_alarm_StatementExecInSecSkipUser              , "If 'StatementExecInSec' is true; Skip connections where 'UserName' matches this regexp."   , new RegExpInputValidator()));
		list.add(new CmSettingsHelper("StatementExecInSec SkipProgram"          ,                PROPKEY_alarm_StatementExecInSecSkipProgram           , String .class, conf.getProperty   (PROPKEY_alarm_StatementExecInSecSkipProgram           , DEFAULT_alarm_StatementExecInSecSkipProgram           ), DEFAULT_alarm_StatementExecInSecSkipProgram           , "If 'StatementExecInSec' is true; Skip connections where 'ProgramName' matches this regexp.", new RegExpInputValidator()));

		return list;
	}

	@Override
	public void initLocalToolTipTextOnTableColumnHeader()
	{
		setLocalToolTipTextOnTableColumnHeader("Pid",                  "<html>The connection id (CONNECTION_ID(), the Id of SHOW PROCESSLIST, what KILL takes).</html>");
		setLocalToolTipTextOnTableColumnHeader("ThreadId",             "<html>The performance_schema thread id of the connection.</html>");
		setLocalToolTipTextOnTableColumnHeader("MultiSampled",         "<html>YES = the connection was executing the same statement in the previous sample. So it has been running for more than one sample.</html>");
		setLocalToolTipTextOnTableColumnHeader("UserName",             "<html>The user of the connection.</html>");
		setLocalToolTipTextOnTableColumnHeader("Host",                 "<html>The client host of the connection.</html>");
		setLocalToolTipTextOnTableColumnHeader("ProgramName",          "<html>The client program, as the client said when it connected (the connection attribute 'program_name'). Empty if the client did not say.</html>");
		setLocalToolTipTextOnTableColumnHeader("CurrentSchema",        "<html>The current (default) schema of the connection.</html>");
		setLocalToolTipTextOnTableColumnHeader("Command",              "<html>What the connection does: Query (a statement), Sleep (it waits for the client), Connect...</html>");
		setLocalToolTipTextOnTableColumnHeader("State",                "<html>What the statement does right now, for example 'executing', 'Sending data', 'Waiting for table metadata lock', 'User sleep'.</html>");
		setLocalToolTipTextOnTableColumnHeader("ExecTimeInSec",        "<html>How long the statement has been running, in seconds. 0 when no statement is running (Command is Sleep).<br><b>Alarm</b>: StatementExecInSec</html>");
		setLocalToolTipTextOnTableColumnHeader("TimeInStateSec",       "<html>Seconds in the current Command/State. For a Sleep connection: how long since the last statement ended.<br><b>Alarm</b>: HoldingLocksWhileWaitForClientInputInSec</html>");
		setLocalToolTipTextOnTableColumnHeader("HasOpenTxn",           "<html>Y if the connection has an open InnoDB transaction (it is in information_schema.INNODB_TRX).</html>");
		setLocalToolTipTextOnTableColumnHeader("TxnState",             "<html>The state of the transaction: RUNNING, LOCK WAIT, ROLLING BACK, COMMITTING.</html>");
		setLocalToolTipTextOnTableColumnHeader("TxnStartTime",         "<html>When the transaction started.</html>");
		setLocalToolTipTextOnTableColumnHeader("TxnAgeInSec",          "<html>How long the transaction has been open, in seconds.</html>");
		setLocalToolTipTextOnTableColumnHeader("TxnRowsLocked",        "<html>Approximate number of rows the transaction has locked.</html>");
		setLocalToolTipTextOnTableColumnHeader("TxnRowsModified",      "<html>Number of rows the transaction has inserted, updated or deleted.</html>");
		setLocalToolTipTextOnTableColumnHeader("BlockedByPid",         "<html>The connection this one waits for (a row lock or a metadata lock). 0 = not blocked.<br>For a metadata lock wait: one of the connections that holds a lock on the table, preferably one that is not blocked itself.</html>");
		setLocalToolTipTextOnTableColumnHeader("BlockedSeconds",       "<html>How long this connection has waited for the lock, in seconds.</html>");
		setLocalToolTipTextOnTableColumnHeader("BlockedLockType",      "<html>ROW = an InnoDB row (or table) lock. METADATA = a metadata lock (for example a DDL that waits for an open transaction, or a statement that waits for the DDL).</html>");
		setLocalToolTipTextOnTableColumnHeader("BlockedOn",            "<html>What it waits for: the table, the index and the lock mode.</html>");
		setLocalToolTipTextOnTableColumnHeader("BlockingOtherPids",    "<html>The connections that waits for this one.</html>");
		setLocalToolTipTextOnTableColumnHeader("BlockingOthersMaxSec", "<html>The longest time one of the connections in 'BlockingOtherPids' has waited, in seconds.<br><b>Alarm</b>: ImBlockingOthersMaxTimeInSec</html>");
		setLocalToolTipTextOnTableColumnHeader("RootBlockerPid",       "<html>For a blocked connection: the connection at the start of the chain (the one that is not blocked itself).</html>");
		setLocalToolTipTextOnTableColumnHeader("StmntEventId",         "<html>The performance_schema event id of the statement (it is different for each statement of the connection).</html>");
		setLocalToolTipTextOnTableColumnHeader("Digest",               "<html>The digest of the statement: the same digest as in the tab 'Statements' (performance_schema.events_statements_summary_by_digest).</html>");
		setLocalToolTipTextOnTableColumnHeader("RowsExamined",         "<html>Rows read by the statement (so far, if it is running).</html>");
		setLocalToolTipTextOnTableColumnHeader("RowsSent",             "<html>Rows returned by the statement (so far).</html>");
		setLocalToolTipTextOnTableColumnHeader("RowsAffected",         "<html>Rows changed by the statement.</html>");
		setLocalToolTipTextOnTableColumnHeader("TmpDiskTables",        "<html>Internal temporary tables on disk, created by the statement.</html>");
		setLocalToolTipTextOnTableColumnHeader("FullJoins",            "<html>Joins without an index, in the statement.</html>");
		setLocalToolTipTextOnTableColumnHeader("NoIndexUsed",          "<html>1 = the statement read a table without an index.</html>");
		setLocalToolTipTextOnTableColumnHeader("LockTimeSec",          "<html>Time the statement waited for table locks, in seconds.</html>");
		setLocalToolTipTextOnTableColumnHeader("HasSqlText",           "<html>Checked if 'SqlText' has a value. Hover over the cell to see the SQL Text.</html>");
		setLocalToolTipTextOnTableColumnHeader("HasExecPlan",          "<html>Checked if 'ExecPlan' has a value. Hover over the cell to see the execution plan.</html>");
		setLocalToolTipTextOnTableColumnHeader("HasPidLocks",          "<html>Checked if 'PidLocks' has a value. Hover over the cell to see the locks.</html>");
		setLocalToolTipTextOnTableColumnHeader("HasBlockedPidsInfo",   "<html>Checked if 'BlockedPidsInfo' has a value. Hover over the cell to see the connections this one blocks.</html>");
		setLocalToolTipTextOnTableColumnHeader("SqlText",              "<html>The statement that runs (or, for a Sleep connection, nothing: MySQL does not keep the text of the last statement here).</html>");
		setLocalToolTipTextOnTableColumnHeader("ExecPlan",             "<html>The execution plan (EXPLAIN FORMAT=JSON FOR CONNECTION) of a statement that has run for a while (property '" + PROPKEY_sample_execPlan_minExecTimeInSec + "').<br>"
		                                                             + "<b>Note</b>: MySQL only allows it when the login has ALL global privileges. The normal monitoring login (no access to user data) does not, so this is normally empty (it says so once in the log).</html>");
		setLocalToolTipTextOnTableColumnHeader("PidLocks",             "<html>The locks of a connection that blocks others or is blocked: row locks (performance_schema.data_locks) and metadata locks (performance_schema.metadata_locks), grouped. Waiting/pending ones in red.</html>");
		setLocalToolTipTextOnTableColumnHeader("BlockedPidsInfo",      "<html>For a connection that blocks others: the connections that waits for it, what they execute, and their locks.</html>");
	}
}
