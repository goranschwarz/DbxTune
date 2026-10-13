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
package com.dbxtune.cm.oracle;

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
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmDescriptionProviderBase;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventHoldingLocksWhileWaitForClientInput;
import com.dbxtune.alarm.events.oracle.AlarmEventOraBlockingSession;
import com.dbxtune.alarm.events.oracle.AlarmEventOraLongRunningStatement;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSettingsHelper.RegExpInputValidator;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.oracle.gui.CmActiveStatementsPanel;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.pcs.PcsColumnOptions;
import com.dbxtune.pcs.PcsColumnOptions.ColumnType;
import com.dbxtune.sql.ResultSetMetaDataCached;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoOracle;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.MathUtils;
import com.dbxtune.utils.StringUtil;
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
		"<p>User sessions that are doing something right now, one row per session. A session is included if it:</p>" +
		"<ul>" +
		"    <li>is executing a SQL Statement (STATUS is ACTIVE)</li>" +
		"    <li>or has an open transaction (HasOpenTxn is Y), even if it is not executing anything right now.</li>" +
		"    <li>or is blocking other sessions (BlockingOtherSids)</li>" +
		"</ul>" +
		"<p>The background processes, and the session used to collect this information, are not included.</p>" +
		"<b>Blocking</b>: <code>BlockedBySid</code> is the session this session waits for. <code>BlockingOtherSids</code> are the sessions that waits for this session.<br>" +
		"A session with an open transaction that is INACTIVE (it waits for the client) holds its locks until the client does COMMIT or ROLLBACK.<br>" +
		"<br>" +
		"<b>SQL Text and Execution Plan</b>: hover over the checkbox <code>HasSqlText</code> or <code>HasExecPlan</code>.<br>" +
		"<b>Locks, blocked sessions and waits</b>: hover over the checkbox <code>HasSidLocks</code> (the locks the session holds or waits for), " +
		"<code>HasBlockedSidsInfo</code> (the sessions that wait for it: who, what they execute, their locks) or <code>HasSidWaitInfo</code> (what it has waited for lately).<br>" +
		"All of this is also in the description of the alarms from this tab.<br>" +
		"<br>" +
		"Background colors:" +
		"<ul>" +
		"    <li>ORANGE - The session was executing the same statement in the previous sample as well (MultiSampled)</li>" +
		"    <li>YELLOW - Has an open transaction, but is not executing anything: it waits for the client, and holds its locks meanwhile</li>" +
		"    <li>PINK   - Blocked by another session (BlockedBySid)</li>" +
		"    <li>RED    - Blocks other sessions, and is not blocked itself: the root cause (BlockingOtherSids)</li>" +
		"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"V$SESSION", "V$TRANSACTION", "V$SQL", "V$LOCK", "V$SESSION_EVENT", "V$SESSION_WAIT_HISTORY", "V$SESS_TIME_MODEL", "V$SESSION_LONGOPS", "V$PROCESS", "V$TEMPSEG_USAGE"};
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
	 * FACTORY method to create the object
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

		addTrendGraphs();

		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String  PROPKEY_sample_execPlan                   = CM_NAME + ".sample.execPlan";
	public static final boolean DEFAULT_sample_execPlan                   = true;

	public static final String  PROPKEY_sample_execPlan_minExecTimeInSec  = CM_NAME + ".sample.execPlan.minExecTimeInSec";
	public static final int     DEFAULT_sample_execPlan_minExecTimeInSec  = 5;

	public static final String  PROPKEY_sample_sidLocks                   = CM_NAME + ".sample.sidLocks";
	public static final boolean DEFAULT_sample_sidLocks                   = true;

	public static final String  PROPKEY_sample_sidWaits                   = CM_NAME + ".sample.sidWaits";
	public static final boolean DEFAULT_sample_sidWaits                   = true;

	public static final String  PROPKEY_sample_sessionDetails             = CM_NAME + ".sample.sessionDetails";
	public static final boolean DEFAULT_sample_sessionDetails             = true;

	// How many NEW execution plans do we fetch in ONE sample (the rest is fetched in the next sample, if the statement is still running)
	private static final int    EXEC_PLAN_MAX_FETCH_PER_SAMPLE = 5;
	private static final int    EXEC_PLAN_CACHE_SIZE           = 200;

	// The lookups in localCalculation() (session details, locks, waits): ONE statement for ALL the sessions of the sample, with this timeout
	private static final int    LOOKUP_QUERY_TIMEOUT_SEC = 5;
	private static final int    LOOKUP_MAX_SIDS          = 500;             // (an IN list can hold 1000)
	private static final long   LOOKUP_RETRY_AFTER_MS    = 10 * 60 * 1000;  // a lookup that failed (a timeout...) is tried again after 10 minutes
	private static final int    LOCKS_SHOWN_PER_SID      = 100;             // 'SidLockCount' has them all, the table in 'SidLocks' the first ones
	private static final int    EVENTS_SHOWN_PER_SID     = 10;

	// V$LOCK: LMODE and REQUEST (the same names as in CmLocks)
	private static final String[] LOCK_MODE_NAMES = { "-", "Null", "Row Share", "Row Exclusive", "Share", "Share Row Exclusive", "Exclusive" };

	/** true when getSqlForVersion() chose the SQL for a Container Database: object names are then looked up in CDB_OBJECTS (by CON_ID) */
	private boolean _isCdbSql = false;

	/** A lookup that failed is not tried again before this time (System.currentTimeMillis). Long.MAX_VALUE = not until we reconnect. Key: name of the lookup */
	private Map<String, Long> _lookupRetryTime = new HashMap<>();

	/** The lookups we have written a warning about (the next failures of the same lookup are only logged in debug mode) */
	private Set<String> _warnedLookups = new HashSet<>();

	/** V$LOCK_TYPE: what a lock type is called ('TM' is 'DML', 'TX' is 'Transaction'). Read once per connection */
	private Map<String, String> _lockTypeNames = new HashMap<>();

	/**
	 * Names we have looked up, so every name is only asked for once (the dictionary views are expensive to read in a Container Database).
	 * The oldest entry is removed when a cache is full. An empty name = it was not found.
	 */
	private static class NameCache extends LinkedHashMap<String, String>
	{
		private static final long serialVersionUID = 1L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, String> eldest)
		{
			return size() > 2000;
		}
	}

	/** The objects of the table locks. Key: CON_ID:OBJECT_ID, value: OWNER.OBJECT_NAME[:SUBOBJECT_NAME] (OBJECT_TYPE) */
	private Map<String, String> _objectNameCache = new NameCache();

	/** PL/SQL procedures, functions and packages. Key: CON_ID:OBJECT_ID:SUBPROGRAM_ID, value: OWNER.OBJECT_NAME[.PROCEDURE_NAME] */
	private Map<String, String> _plsqlNameCache = new NameCache();

	/** V$SESSION_EVENT at the previous sample, to show what a session has waited for SINCE then. Key: SID:SERIAL#|EVENT, value: {TOTAL_WAITS, TIME_WAITED_MICRO} */
	private Map<String, long[]> _prevSessionEvents = new HashMap<>();

	/** V$SESS_TIME_MODEL at the previous sample. Key: SID:SERIAL#, value: {DB time, DB CPU} in microseconds */
	private Map<String, long[]> _prevSessTimeModel = new HashMap<>();

	/** When the two maps above were filled (System.currentTimeMillis). 0 = they are empty */
	private long _prevSidWaitsTime = 0;

	/** Execution plans we already have fetched. Key: CON_ID|SQL_ID|CHILD_NUMBER|PLAN_HASH_VALUE. The oldest entry is removed when the cache is full */
	private Map<String, String> _execPlanCache = new LinkedHashMap<String, String>()
	{
		private static final long serialVersionUID = 1L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, String> eldest)
		{
			return size() > EXEC_PLAN_CACHE_SIZE;
		}
	};

	private void addTrendGraphs()
	{
	}

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_execPlan                 , DEFAULT_sample_execPlan);
		Configuration.registerDefaultValue(PROPKEY_sample_execPlan_minExecTimeInSec, DEFAULT_sample_execPlan_minExecTimeInSec);
		Configuration.registerDefaultValue(PROPKEY_sample_sidLocks                 , DEFAULT_sample_sidLocks);
		Configuration.registerDefaultValue(PROPKEY_sample_sidWaits                 , DEFAULT_sample_sidWaits);
		Configuration.registerDefaultValue(PROPKEY_sample_sessionDetails           , DEFAULT_sample_sessionDetails);
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		list.add(new CmSettingsHelper("Get Execution Plan"             , PROPKEY_sample_execPlan                 , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_execPlan                 , DEFAULT_sample_execPlan                 ), DEFAULT_sample_execPlan                 , "Get the execution plan for active statements, using DBMS_XPLAN.DISPLAY_CURSOR. A plan is only fetched once (they are cached), and max " + EXEC_PLAN_MAX_FETCH_PER_SAMPLE + " new plans per sample." ));
		list.add(new CmSettingsHelper("Get Execution Plan, Min ExecTime", PROPKEY_sample_execPlan_minExecTimeInSec, Integer.class, conf.getIntProperty    (PROPKEY_sample_execPlan_minExecTimeInSec, DEFAULT_sample_execPlan_minExecTimeInSec), DEFAULT_sample_execPlan_minExecTimeInSec, "Only get the execution plan for statements that has been executing for at least this number of seconds." ));
		list.add(new CmSettingsHelper("Get SID Locks"                  , PROPKEY_sample_sidLocks                 , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_sidLocks                 , DEFAULT_sample_sidLocks                 ), DEFAULT_sample_sidLocks                 , "Get the locks of every session in the table, from V$LOCK (one statement per sample, for all of them, and only when some session has an open transaction or is blocked). This fills 'SidLocks', 'SidLockCount' and the locks in 'BlockedSidsInfo'." ));
		list.add(new CmSettingsHelper("Get SID Waits"                  , PROPKEY_sample_sidWaits                 , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_sidWaits                 , DEFAULT_sample_sidWaits                 ), DEFAULT_sample_sidWaits                 , "Get what every session in the table has waited for, from V$SESSION_EVENT, V$SESSION_WAIT_HISTORY and V$SESS_TIME_MODEL (three statements per sample). This fills 'SidWaitInfo'." ));
		list.add(new CmSettingsHelper("Get Session Details"            , PROPKEY_sample_sessionDetails           , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_sessionDetails           , DEFAULT_sample_sessionDetails           ), DEFAULT_sample_sessionDetails           , "Get 'RowWaitRowid', 'PlsqlEntryObject', 'PlsqlCurrentObject', 'PgaUsedMb', 'TempUsedMb' and 'LongOpsInfo' for every session in the table (two statements per sample)." ));

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
		List<String> pkCols = new LinkedList<String>();
		pkCols.add("SID");
		return pkCols;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "V$SESSION", "V$TRANSACTION", "V$SQL", "DBA_OBJECTS");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoOracle oraVersionInfo = (DbmsVersionInfoOracle) versionInfo;
		_isCdbSql = false;

		// 11.2 and above
		String sql = getSql(
				"CAST(0 as NUMBER(10))",        // CON_ID
				"CAST(NULL as VARCHAR2(128))",  // CON_NAME
				"",                             // join V$CONTAINERS
				"",                             // join V$SQL on CON_ID
				"DBA_OBJECTS o where o.OBJECT_ID = s.ROW_WAIT_OBJ#");

		// 12.1 and above, in a Container Database: the same SQL_ID (and object id) can exist in several containers
		if (versionInfo.getLongVersion() >= Ver.ver(12,1) && oraVersionInfo.isCdb())
		{
			String sqlCdb = getSql(
					"s.CON_ID",
					"c.NAME",
					"left join V$CONTAINERS c on c.CON_ID = s.CON_ID \n",
					"                 and q.CON_ID       = s.CON_ID \n",
					"CDB_OBJECTS o where o.OBJECT_ID = s.ROW_WAIT_OBJ# and o.CON_ID = s.CON_ID");

			// The SQL for 12.1 and above can NOT be tested on all versions: so "dry-run" it, and if it fails use the 11.2 SQL (which works on all versions)
			// conn is null when we just want to "view" the SQL (the CM Properties dialog)
			if (conn == null)
			{
				sql = sqlCdb;
			}
			else
			{
				try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery("select * from (" + sqlCdb + ") where 1=2"))
				{
					sql       = sqlCdb;
					_isCdbSql = true;   // the lookups in localCalculation() then also use CDB_OBJECTS (by CON_ID)
				}
				catch (SQLException ex)
				{
					_logger.warn(CM_NAME + ": The SQL for a CDB (version " + Ver.versionNumToStr(versionInfo.getLongVersion()) + ") failed, using the SQL for 11.2 instead. Caught: " + ex.getMessage().trim());
				}
			}
		}

		return sql;
	}

	/**
	 * NOTE: The column aliases in "MixedCase" MUST be quoted, otherwise Oracle makes them UPPERCASE.<br>
	 * The names "HasSqlText", "SqlText", "HasExecPlan", "ExecPlan", "HasSidLocks", "SidLocks", "HasBlockedSidsInfo", "BlockedSidsInfo", "HasSidWaitInfo", "SidWaitInfo",
	 * "MultiSampled", "HasOpenTxn", "BlockedBySid", "BlockingOtherSids" and STATUS
	 * are used by DbxCentral (dbxcentral.graph.js) and by the GUI panel (CmActiveStatementsPanel): tooltips and row colors
	 */
	private String getSql(String CON_ID, String CON_NAME, String joinContainers, String joinSqlOnConId, String rowWaitObjectFromWhere)
	{
		return ""
			+ "select \n"
			+ "     s.SID \n"
			+ "    ,s.SERIAL# \n"
			+ "    ," + CON_ID   + " as CON_ID \n"
			+ "    ," + CON_NAME + " as CON_NAME \n"
			+ "    ,s.USERNAME \n"
			+ "    ,s.STATUS \n"
			+ "    ,s.OSUSER \n"
			+ "    ,s.MACHINE \n"
			+ "    ,s.PROGRAM \n"
			+ "    ,s.MODULE \n"
			+ "    ,s.ACTION \n"
			+ "    ,s.CLIENT_INFO \n"
			+ "    ,s.LOGON_TIME \n"
			+ "    ,CAST(NULL as VARCHAR2(10))                                                    as \"MultiSampled\" \n"
			+ "    ,CAST(0 as NUMBER(1))                                                          as \"HasSqlText\" \n"   // a checkbox, see modifyResultSetMetaData() and localCalculation()
			+ "    ,CAST(0 as NUMBER(1))                                                          as \"HasExecPlan\" \n"  // a checkbox, see modifyResultSetMetaData() and localCalculation()
			+ "    ,CAST(0 as NUMBER(1))                                                          as \"HasSidLocks\" \n"         // checkboxes for 'SidLocks', 'BlockedSidsInfo' and 'SidWaitInfo', which are
			+ "    ,CAST(0 as NUMBER(1))                                                          as \"HasBlockedSidsInfo\" \n"  // filled in by localCalculation() (the lookups are NOT part of this SQL: if one
			+ "    ,CAST(0 as NUMBER(1))                                                          as \"HasSidWaitInfo\" \n"      // of them fails on some Oracle version, the Active Statements are still there)
			+ "    ,CAST(-1 as NUMBER(10))                                                        as \"SidLockCount\" \n"
			+ "    ,s.LAST_CALL_ET                                                                as \"ExecTimeInSec\" \n"
			+ "    ,(SYSDATE - s.LAST_CALL_ET / 86400)                                            as \"LastCallTime\" \n"
			+ "    ,s.WAIT_CLASS \n"
			+ "    ,s.EVENT \n"
			+ "    ,s.STATE \n"
			+ "    ,TRUNC(s.WAIT_TIME_MICRO / 1000)                                               as \"WaitTimeMs\" \n"
			+ "    ,s.BLOCKING_SESSION                                                            as \"BlockedBySid\" \n"
			+ "    ,CAST(NULL as NUMBER(10))                                                      as \"RootBlockerSid\" \n"      // filled in by localCalculation()
			+ "    ,case when s.BLOCKING_SESSION is not null then TRUNC(s.WAIT_TIME_MICRO / 1000000) else 0 end as \"BlockedSeconds\" \n"
			+ "    ,CAST(NULL as VARCHAR2(512))                                                   as \"BlockingOtherSids\" \n"
			+ "    ,CAST(0    as NUMBER(10))                                                      as \"BlockingOthersMaxSec\" \n"
			+ "    ,case when s.BLOCKING_SESSION is not null \n"
			+ "          then (select CAST(max(o.OWNER || '.' || o.OBJECT_NAME) as VARCHAR2(300)) from " + rowWaitObjectFromWhere + ") \n"
			+ "     end                                                                           as \"RowWaitObject\" \n"
			+ "    ,CAST(NULL as VARCHAR2(18))                                                    as \"RowWaitRowid\" \n"        // filled in by localCalculation()
			+ "    ,s.SQL_ID \n"
			+ "    ,s.PREV_SQL_ID \n"
			+ "    ,COALESCE(s.SQL_ID, s.PREV_SQL_ID)                                             as \"EffectiveSqlId\" \n"
			+ "    ,s.SQL_CHILD_NUMBER \n"
			+ "    ,s.SQL_EXEC_START \n"
			+ "    ,CAST(case when s.SQL_ID is null then 'Y' else 'N' end as VARCHAR2(1))         as \"IsPrevSql\" \n"
			+ "    ,q.CHILD_NUMBER                                                                as \"EffectiveChildNumber\" \n"
			+ "    ,q.PLAN_HASH_VALUE \n"
			+ "    ,CAST(NULL as VARCHAR2(400))                                                   as \"PlsqlEntryObject\" \n"    // filled in by localCalculation()
			+ "    ,CAST(NULL as VARCHAR2(400))                                                   as \"PlsqlCurrentObject\" \n"  // filled in by localCalculation()
			+ "    ,q.SQL_FULLTEXT                                                                as \"SqlText\" \n"
			+ "    ,CAST(case when t.ADDR is not null then 'Y' else 'N' end as VARCHAR2(1))       as \"HasOpenTxn\" \n"
			+ "    ,t.START_DATE                                                                  as \"TxnStartTime\" \n"
			+ "    ,CAST(86400 * (SYSDATE - t.START_DATE) as INT)                                 as \"TxnSeconds\" \n"
			+ "    ,t.USED_UBLK                                                                   as \"TxnUndoBlocks\" \n"
			+ "    ,t.USED_UREC                                                                   as \"TxnUndoRecords\" \n"
			+ "    ,CAST(NULL as NUMBER(12,1))                                                    as \"PgaUsedMb\" \n"           // the rest: filled in by localCalculation()
			+ "    ,CAST(NULL as NUMBER(12,1))                                                    as \"TempUsedMb\" \n"
			+ "    ,CAST(NULL as VARCHAR2(1000))                                                  as \"LongOpsInfo\" \n"
			+ "    ,TO_CLOB(NULL)                                                                 as \"SidLocks\" \n"
			+ "    ,TO_CLOB(NULL)                                                                 as \"BlockedSidsInfo\" \n"
			+ "    ,TO_CLOB(NULL)                                                                 as \"SidWaitInfo\" \n"
			+ "    ,TO_CLOB(NULL)                                                                 as \"ExecPlan\" \n"
			+ "from V$SESSION s \n"
			+ joinContainers
			+ "left join V$TRANSACTION t on t.SES_ADDR = s.SADDR \n"

			// The SQL: the one it is executing now, or if it is not executing anything: the last one it executed (which might have started the transaction)
			+ "left join V$SQL q          on q.SQL_ID       = COALESCE(s.SQL_ID, s.PREV_SQL_ID) \n"
			+ "                 and q.CHILD_NUMBER = NVL2(s.SQL_ID, s.SQL_CHILD_NUMBER, s.PREV_CHILD_NUMBER) \n"
			+ joinSqlOnConId

			+ "where s.TYPE = 'USER' \n"
			+ "  and s.SID <> SYS_CONTEXT('USERENV','SID') \n"
			+ "  and (   (s.STATUS = 'ACTIVE' and not (s.STATE = 'WAITING' and s.WAIT_CLASS = 'Idle')) \n"
			+ "       or t.ADDR is not null \n"
			+ "       or s.SID in (select b.BLOCKING_SESSION from V$SESSION b where b.BLOCKING_SESSION is not null) \n"
			+ "      ) \n"
			+ "order by s.SID \n"
			+ "";
	}

	/**
	 * Oracle has no boolean type (before 23): in the SQL the "Has..." columns are NUMBER(1), here they are made into BOOLEAN, so they are shown as checkboxes.<br>
	 * They are set to true in localCalculation(), when the row has a SQL Text / an Execution Plan / locks / blocked sessions / wait info.
	 */
	@Override
	public ResultSetMetaDataCached modifyResultSetMetaData(ResultSetMetaDataCached rsmdc)
	{
		for (String colName : new String[] { "HasSqlText", "HasExecPlan", "HasSidLocks", "HasBlockedSidsInfo", "HasSidWaitInfo" })
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

	/** The SQL Text, the Execution Plan and the HTML tables (locks, blocked sessions, waits) as a tooltip, on the checkbox column and on the column itself (as the other DbxTune's does) */
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

		// The HTML tables: locks, who I am blocking, and what the session has waited for
		String htmlColName = null;
		if ("HasSidLocks"       .equals(colName) || "SidLocks"       .equals(colName) || "SidLockCount".equals(colName)) htmlColName = "SidLocks";
		if ("HasBlockedSidsInfo".equals(colName) || "BlockedSidsInfo".equals(colName))                                   htmlColName = "BlockedSidsInfo";
		if ("HasSidWaitInfo"    .equals(colName) || "SidWaitInfo"    .equals(colName))                                   htmlColName = "SidWaitInfo";
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
	 * Set 'MultiSampled' to YES if the session was executing the SAME statement (same SQL_ID that started at the same time) in the previous sample
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample)
	{
		int pos_MultiSampled   = newSample.findColumn("MultiSampled");
		int pos_SQL_ID         = newSample.findColumn("SQL_ID");
		int pos_SQL_EXEC_START = newSample.findColumn("SQL_EXEC_START");

		if (prevSample == null || pos_MultiSampled == -1 || pos_SQL_ID == -1 || pos_SQL_EXEC_START == -1)
			return;

		for (int rowId = 0; rowId < newSample.getRowCount(); rowId++)
		{
			int prevRowId = prevSample.getRowNumberForPkValue(newSample.getPkValue(rowId));
			if (prevRowId < 0)
				continue;

			String    thisSqlId     = newSample .getValueAsString   (rowId    , pos_SQL_ID);
			String    prevSqlId     = prevSample.getValueAsString   (prevRowId, pos_SQL_ID);
			Timestamp thisExecStart = newSample .getValueAsTimestamp(rowId    , pos_SQL_EXEC_START, null);
			Timestamp prevExecStart = prevSample.getValueAsTimestamp(prevRowId, pos_SQL_EXEC_START, null);

			if (StringUtil.hasValue(thisSqlId) && thisSqlId.equals(prevSqlId) && thisExecStart != null && thisExecStart.equals(prevExecStart))
				newSample.setValueAt("YES", rowId, pos_MultiSampled);
		}
	}

	/**
	 * Fill in: 'BlockingOtherSids' + 'BlockingOthersMaxSec' (from the other rows), the session details, locks and waits (looked up), 'BlockedSidsInfo' and 'ExecPlan'
	 */
	@Override
	public void localCalculation(CounterSample newSample)
	{
		int pos_SID                  = newSample.findColumn("SID");
		int pos_BlockedBySid         = newSample.findColumn("BlockedBySid");
		int pos_BlockedSeconds       = newSample.findColumn("BlockedSeconds");
		int pos_BlockingOtherSids    = newSample.findColumn("BlockingOtherSids");
		int pos_BlockingOthersMaxSec = newSample.findColumn("BlockingOthersMaxSec");

		if (pos_SID == -1 || pos_BlockedBySid == -1 || pos_BlockedSeconds == -1 || pos_BlockingOtherSids == -1 || pos_BlockingOthersMaxSec == -1)
		{
			_logger.warn(getName() + ": localCalculation(), Problems finding one of the columns: SID, BlockedBySid, BlockedSeconds, BlockingOtherSids, BlockingOthersMaxSec.");
			return;
		}

		//---------------------------------------------------------------
		// Who am I blocking: the rows where 'BlockedBySid' is my SID
		// NOTE: 'BlockedBySid' is 0 when the session is not blocked (a NULL number is read as 0, and there is no SID 0)
		//---------------------------------------------------------------
		for (int rowId = 0; rowId < newSample.getRowCount(); rowId++)
		{
			long sid = newSample.getValueAsLong(rowId, pos_SID, -1L);

			String blockingOtherSids    = "";
			long   blockingOthersMaxSec = 0;

			for (int r = 0; r < newSample.getRowCount(); r++)
			{
				if (newSample.getValueAsLong(r, pos_BlockedBySid, 0L) != sid)
					continue;

				blockingOtherSids   += (blockingOtherSids.isEmpty() ? "" : ", ") + newSample.getValueAsLong(r, pos_SID, -1L);
				blockingOthersMaxSec = Math.max(blockingOthersMaxSec, newSample.getValueAsLong(r, pos_BlockedSeconds, 0L));
			}

			if ( ! blockingOtherSids.isEmpty() )
			{
				newSample.setValueAt(blockingOtherSids                     , rowId, pos_BlockingOtherSids);
				newSample.setValueAt(Integer.valueOf((int) blockingOthersMaxSec), rowId, pos_BlockingOthersMaxSec);
			}
		}

		//---------------------------------------------------------------
		// RootBlockerSid: follow 'BlockedBySid' to the session that is not blocked itself (a session that blocks others is always in the table)
		//---------------------------------------------------------------
		int pos_RootBlockerSid = newSample.findColumn("RootBlockerSid");
		for (int rowId = 0; rowId < newSample.getRowCount() && pos_RootBlockerSid != -1; rowId++)
		{
			long rootSid = newSample.getValueAsLong(rowId, pos_BlockedBySid, 0L);
			if (rootSid <= 0)
				continue;

			// Max as many steps as there are rows: in a deadlock the sessions wait for each other (until Oracle breaks it)
			for (int step = 0; step < newSample.getRowCount(); step++)
			{
				long nextSid = 0;
				for (int r = 0; r < newSample.getRowCount(); r++)
				{
					if (newSample.getValueAsLong(r, pos_SID, -1L) == rootSid)
						nextSid = newSample.getValueAsLong(r, pos_BlockedBySid, 0L);
				}

				if (nextSid <= 0)
					break;

				rootSid = nextSid;
			}

			newSample.setValueAt(Integer.valueOf((int) rootSid), rowId, pos_RootBlockerSid);
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
		// More about the sessions of this sample: session details, locks and waits
		// ONE statement per kind for ALL the sessions (not one per session), with a short timeout.
		// They are NOT part of the SQL of this CM, and each has its own try/catch: if one of them fails
		// (on an Oracle version it could not be tested on) the columns it fills are just empty.
		//---------------------------------------------------------------
		Configuration conf = Configuration.getCombinedConfiguration();
		DbxConnection conn = getCounterController() == null ? null : getCounterController().getMonConnection();

		if (conn != null && newSample.getRowCount() > 0)
		{
			// "sid, sid, ..." for the IN lists, and where each session is in the sample
			Map<Long, Integer> sidToRowId = new HashMap<>();
			String             sidList    = "";
			for (int rowId = 0; rowId < newSample.getRowCount() && rowId < LOOKUP_MAX_SIDS; rowId++)
			{
				long sid = newSample.getValueAsLong(rowId, pos_SID, -1L);
				sidToRowId.put(sid, rowId);
				sidList += (sidList.isEmpty() ? "" : ", ") + sid;
			}

			if (conf.getBooleanProperty(PROPKEY_sample_sessionDetails, DEFAULT_sample_sessionDetails))
			{
				lookupSessionDetails(conn, newSample, sidList, sidToRowId);
				lookupLongOps       (conn, newSample, sidList, sidToRowId);
			}

			// V$LOCK is expensive to read (145 ms on 11.2, also when it is small), so only when some session can have locks worth showing:
			// it has an open transaction, or it waits for another session
			int     pos_HasOpenTxn  = newSample.findColumn("HasOpenTxn");
			boolean hasTxnOrBlocked = false;
			for (int rowId = 0; rowId < newSample.getRowCount() && pos_HasOpenTxn != -1 && ! hasTxnOrBlocked; rowId++)
				hasTxnOrBlocked = "Y".equals(newSample.getValueAsString(rowId, pos_HasOpenTxn)) || newSample.getValueAsLong(rowId, pos_BlockedBySid, 0L) > 0;

			if (hasTxnOrBlocked && conf.getBooleanProperty(PROPKEY_sample_sidLocks, DEFAULT_sample_sidLocks))
				lookupSidLocks(conn, newSample, sidList, sidToRowId);

			if (conf.getBooleanProperty(PROPKEY_sample_sidWaits, DEFAULT_sample_sidWaits))
				lookupSidWaits(conn, newSample, sidList, sidToRowId);
		}
		else
		{
			// No sessions in this sample: so the next one has no "previous sample" to compare its waits with
			_prevSessionEvents.clear();
			_prevSessTimeModel.clear();
			_prevSidWaitsTime = 0;
		}

		// Who am I blocking: what they execute, and their locks (from the other rows of the sample, so AFTER the lookups)
		setBlockedSidsInfo(newSample);

		//---------------------------------------------------------------
		// Execution Plan: for statements that has been executing for a while
		// A plan is fetched ONCE (then it is in the cache), and only a few NEW plans in each sample (so we do not slow down the collector)
		//---------------------------------------------------------------
		if ( ! conf.getBooleanProperty(PROPKEY_sample_execPlan, DEFAULT_sample_execPlan) )
			return;

		int minExecTimeInSec = conf.getIntProperty(PROPKEY_sample_execPlan_minExecTimeInSec, DEFAULT_sample_execPlan_minExecTimeInSec);

		int pos_CON_ID               = newSample.findColumn("CON_ID");
		int pos_STATUS               = newSample.findColumn("STATUS");
		int pos_ExecTimeInSec        = newSample.findColumn("ExecTimeInSec");
		int pos_EffectiveSqlId       = newSample.findColumn("EffectiveSqlId");
		int pos_EffectiveChildNumber = newSample.findColumn("EffectiveChildNumber");
		int pos_PLAN_HASH_VALUE      = newSample.findColumn("PLAN_HASH_VALUE");
		int pos_ExecPlan             = newSample.findColumn("ExecPlan");
		int pos_HasExecPlan          = newSample.findColumn("HasExecPlan");

		if (pos_CON_ID == -1 || pos_STATUS == -1 || pos_ExecTimeInSec == -1 || pos_EffectiveSqlId == -1 || pos_EffectiveChildNumber == -1 || pos_PLAN_HASH_VALUE == -1 || pos_ExecPlan == -1 || pos_HasExecPlan == -1)
			return;

		if (conn == null)
			return;

		int fetchCount = 0;

		for (int rowId = 0; rowId < newSample.getRowCount(); rowId++)
		{
			String sqlId = newSample.getValueAsString(rowId, pos_EffectiveSqlId);
			if (StringUtil.isNullOrBlank(sqlId))
				continue;

			// Only for sessions that are executing, and has done so for a while
			if ( ! "ACTIVE".equals(newSample.getValueAsString(rowId, pos_STATUS)) || newSample.getValueAsLong(rowId, pos_ExecTimeInSec, 0L) < minExecTimeInSec )
				continue;

			long   childNumber = newSample.getValueAsLong(rowId, pos_EffectiveChildNumber, 0L);
			String cacheKey    = newSample.getValueAsString(rowId, pos_CON_ID) + "|" + sqlId + "|" + childNumber + "|" + newSample.getValueAsString(rowId, pos_PLAN_HASH_VALUE);

			String execPlan = _execPlanCache.get(cacheKey);
			if (execPlan == null && fetchCount < EXEC_PLAN_MAX_FETCH_PER_SAMPLE)
			{
				fetchCount++;

				StringBuilder sb = new StringBuilder();
				try (PreparedStatement pstmnt = conn.prepareStatement("select PLAN_TABLE_OUTPUT from table(DBMS_XPLAN.DISPLAY_CURSOR(?, ?, 'TYPICAL'))"))
				{
					pstmnt.setQueryTimeout(2);
					pstmnt.setString(1, sqlId);
					pstmnt.setLong  (2, childNumber);

					try (ResultSet rs = pstmnt.executeQuery())
					{
						while (rs.next())
							sb.append(rs.getString(1)).append("\n");
					}
				}
				catch (SQLException ex)
				{
					// Not a problem for the sample: just no plan for this row, and try again in next sample
					_logger.debug(getName() + ": Problems getting execution plan for SQL_ID='" + sqlId + "', childNumber=" + childNumber + ". Caught: " + ex);
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

	/** When we are disconnected (or the PCS database is rolled over): forget the plans, so they are stored again. And try the lookups that failed again */
	@Override
	public void reset()
	{
		super.reset();

		_execPlanCache.clear();

		_lookupRetryTime  .clear();
		_warnedLookups    .clear();
		_lockTypeNames    .clear();
		_objectNameCache  .clear();
		_plsqlNameCache   .clear();
		_prevSessionEvents.clear();
		_prevSessTimeModel.clear();
		_prevSidWaitsTime = 0;
	}

	//------------------------------------------------------------
	// The lookups of localCalculation(): session details, long operations, locks and waits
	//------------------------------------------------------------

	/** @return false if this lookup failed earlier, and it is not yet time to try it again */
	private boolean isLookupDue(String lookupName)
	{
		Long retryTime = _lookupRetryTime.get(lookupName);
		return retryTime == null || System.currentTimeMillis() >= retryTime;
	}

	/** A lookup failed: the columns it fills are empty in this sample. Decide when to try it again, and write it in the log (as a warning the first time) */
	private void lookupFailed(String lookupName, SQLException ex)
	{
		// ORA-00942 (no such view), ORA-00904 (no such column), ORA-01031 (not allowed): it will not get better by trying again
		boolean permanent = ex.getErrorCode() == 942 || ex.getErrorCode() == 904 || ex.getErrorCode() == 1031;
		_lookupRetryTime.put(lookupName, permanent ? Long.MAX_VALUE : System.currentTimeMillis() + LOOKUP_RETRY_AFTER_MS);

		String msg = getName() + ": Problems with the lookup '" + lookupName + "', the columns it fills are empty. "
				+ (permanent ? "It is NOT tried again until next connect." : "It is tried again in " + (LOOKUP_RETRY_AFTER_MS / 60000) + " minutes.")
				+ " Caught: " + ex.getMessage().trim();

		if (_warnedLookups.add(lookupName))
			_logger.warn(msg);
		else
			_logger.debug(msg);
	}

	/**
	 * 'RowWaitRowid', 'PgaUsedMb' and 'TempUsedMb': one statement for all the sessions.<br>
	 * 'PlsqlEntryObject' and 'PlsqlCurrentObject': the same statement gets the ID's, the names are looked up the first time we see them.
	 */
	private void lookupSessionDetails(DbxConnection conn, CounterSample newSample, String sidList, Map<Long, Integer> sidToRowId)
	{
		int pos_RowWaitRowid       = newSample.findColumn("RowWaitRowid");
		int pos_PlsqlEntryObject   = newSample.findColumn("PlsqlEntryObject");
		int pos_PlsqlCurrentObject = newSample.findColumn("PlsqlCurrentObject");
		int pos_PgaUsedMb          = newSample.findColumn("PgaUsedMb");
		int pos_TempUsedMb         = newSample.findColumn("TempUsedMb");

		if (pos_RowWaitRowid == -1 || pos_PlsqlEntryObject == -1 || pos_PlsqlCurrentObject == -1 || pos_PgaUsedMb == -1 || pos_TempUsedMb == -1)
			return;

		if ( ! isLookupDue("sessionDetails") )
			return;

		// RowWaitRowid: ROW_WAIT_FILE# is the RELATIVE file number, which is what a ROWID holds (checked against the real ROWID of a locked row, also in a bigfile tablespace)
		//               The ROW_WAIT_... columns are only valid while the session waits for a row lock
		//               In a Container Database: an OBJECT_ID is only unique within its container
		// PL/SQL      : only the ID's here, the names are looked up below (the first time we see them)
		String sql = ""
			+ "select \n"
			+ "     s.SID \n"
			+ "    ,case when s.BLOCKING_SESSION is not null and s.EVENT = 'enq: TX - row lock contention' and s.ROW_WAIT_OBJ# > 0 \n"
			+ "          then (select ROWIDTOCHAR(DBMS_ROWID.ROWID_CREATE(1, o.DATA_OBJECT_ID, s.ROW_WAIT_FILE#, s.ROW_WAIT_BLOCK#, s.ROW_WAIT_ROW#)) \n"
			+ "                from " + (_isCdbSql ? "CDB_OBJECTS o where o.CON_ID = s.CON_ID and" : "DBA_OBJECTS o where") + " o.OBJECT_ID = s.ROW_WAIT_OBJ# and o.DATA_OBJECT_ID is not null and rownum = 1) \n"
			+ "     end \n"
			+ "    ,(select x.PGA_USED_MEM from V$PROCESS x where x.ADDR = s.PADDR) \n"
			+ "    ,(select NVL(sum(u.BLOCKS), 0) from V$TEMPSEG_USAGE u where u.SESSION_ADDR = s.SADDR) * (select TO_NUMBER(v.VALUE) from V$PARAMETER v where v.NAME = 'db_block_size') \n"
			+ "    ," + (_isCdbSql ? "s.CON_ID" : "0") + " \n"
			+ "    ,s.PLSQL_ENTRY_OBJECT_ID \n"
			+ "    ,s.PLSQL_ENTRY_SUBPROGRAM_ID \n"
			+ "    ,s.PLSQL_OBJECT_ID \n"
			+ "    ,s.PLSQL_SUBPROGRAM_ID \n"
			+ "from V$SESSION s \n"
			+ "where s.SID in (" + sidList + ")";

		// The PL/SQL the sessions are in. Key: rowId, value: CON_ID:OBJECT_ID:SUBPROGRAM_ID (which is also the key in _plsqlNameCache)
		Map<Integer, String> plsqlEntryKeys   = new HashMap<>();
		Map<Integer, String> plsqlCurrentKeys = new HashMap<>();

		try (Statement stmnt = conn.createStatement())
		{
			stmnt.setQueryTimeout(LOOKUP_QUERY_TIMEOUT_SEC);

			try (ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
				{
					Integer rowId = sidToRowId.get(rs.getLong(1));
					if (rowId == null)
						continue;

					newSample.setValueAt(rs.getString(2), rowId, pos_RowWaitRowid);

					double pgaUsedBytes = rs.getDouble(3);
					if ( ! rs.wasNull() )
						newSample.setValueAt(MathUtils.roundToBigDecimal(pgaUsedBytes / 1024.0 / 1024.0, 1), rowId, pos_PgaUsedMb);

					double tempUsedBytes = rs.getDouble(4);
					if ( ! rs.wasNull() )
						newSample.setValueAt(MathUtils.roundToBigDecimal(tempUsedBytes / 1024.0 / 1024.0, 1), rowId, pos_TempUsedMb);

					long conId = rs.getLong(5);
					if (rs.getObject(6) != null) plsqlEntryKeys  .put(rowId, conId + ":" + rs.getLong(6) + ":" + rs.getLong(7));
					if (rs.getObject(8) != null) plsqlCurrentKeys.put(rowId, conId + ":" + rs.getLong(8) + ":" + rs.getLong(9));
				}
			}
		}
		catch (SQLException ex)
		{
			lookupFailed("sessionDetails", ex);
			return;
		}

		// The names of the PL/SQL we have not seen before: one statement (max 50 of them, the rest in the next sample)
		// DBA_PROCEDURES is expensive to read in a Container Database (300 ms for nothing), that is why this is not a part of the SQL above
		Set<String> keysToLookup = new HashSet<>();
		for (String key : plsqlEntryKeys  .values()) { if ( ! _plsqlNameCache.containsKey(key) && keysToLookup.size() < 50 ) keysToLookup.add(key); }
		for (String key : plsqlCurrentKeys.values()) { if ( ! _plsqlNameCache.containsKey(key) && keysToLookup.size() < 50 ) keysToLookup.add(key); }

		if ( ! keysToLookup.isEmpty() && isLookupDue("sessionDetails.plsqlNames") )
		{
			String where = "";
			for (String key : keysToLookup)
			{
				String[] id = key.split(":");
				where += (where.isEmpty() ? "" : "\n   or ") + "(" + (_isCdbSql ? "p.CON_ID = " + id[0] + " and " : "") + "p.OBJECT_ID = " + id[1] + " and p.SUBPROGRAM_ID = " + id[2] + ")";
			}

			String namesSql = ""
				+ "select " + (_isCdbSql ? "p.CON_ID" : "0") + ", p.OBJECT_ID, p.SUBPROGRAM_ID, p.OWNER, p.OBJECT_NAME, p.PROCEDURE_NAME \n"
				+ "from " + (_isCdbSql ? "CDB_PROCEDURES" : "DBA_PROCEDURES") + " p \n"
				+ "where " + where;

			try (Statement stmnt = conn.createStatement())
			{
				stmnt.setQueryTimeout(LOOKUP_QUERY_TIMEOUT_SEC);

				try (ResultSet rs = stmnt.executeQuery(namesSql))
				{
					while (rs.next())
					{
						String procedureName = rs.getString(6);
						_plsqlNameCache.put(rs.getLong(1) + ":" + rs.getLong(2) + ":" + rs.getLong(3),
								rs.getString(4) + "." + rs.getString(5) + (StringUtil.hasValue(procedureName) ? "." + procedureName : ""));
					}
				}

				// The ones that were not found: remember that as well, so we do not ask for them in every sample
				for (String key : keysToLookup)
					_plsqlNameCache.putIfAbsent(key, "");
			}
			catch (SQLException ex)
			{
				lookupFailed("sessionDetails.plsqlNames", ex);
			}
		}

		for (Map.Entry<Integer, String> entry : plsqlEntryKeys.entrySet())
		{
			String name = _plsqlNameCache.get(entry.getValue());
			if (StringUtil.hasValue(name))
				newSample.setValueAt(name, entry.getKey(), pos_PlsqlEntryObject);
		}

		for (Map.Entry<Integer, String> entry : plsqlCurrentKeys.entrySet())
		{
			String name = _plsqlNameCache.get(entry.getValue());
			if (StringUtil.hasValue(name))
				newSample.setValueAt(name, entry.getKey(), pos_PlsqlCurrentObject);
		}
	}

	/**
	 * 'LongOpsInfo': how far the long operations of a session have come (full scans, sorts, hash joins, backups...), from V$SESSION_LONGOPS. One statement for all the sessions
	 */
	private void lookupLongOps(DbxConnection conn, CounterSample newSample, String sidList, Map<Long, Integer> sidToRowId)
	{
		int pos_SERIAL      = newSample.findColumn("SERIAL#");
		int pos_STATUS      = newSample.findColumn("STATUS");
		int pos_SQL_ID      = newSample.findColumn("SQL_ID");
		int pos_LongOpsInfo = newSample.findColumn("LongOpsInfo");

		if (pos_SERIAL == -1 || pos_STATUS == -1 || pos_SQL_ID == -1 || pos_LongOpsInfo == -1)
			return;

		if ( ! isLookupDue("longOps") )
			return;

		// An operation that was interrupted stays in the view as "not finished": so only the ones that were updated lately,
		// and (below) only the ones of the statement that the session executes now
		String sql = ""
			+ "select o.SID, o.SERIAL#, o.SQL_ID, o.OPNAME, o.TARGET, o.SOFAR, o.TOTALWORK, o.UNITS, o.ELAPSED_SECONDS, o.TIME_REMAINING \n"
			+ "from V$SESSION_LONGOPS o \n"
			+ "where o.TOTALWORK > 0 \n"
			+ "  and o.SOFAR < o.TOTALWORK \n"
			+ "  and o.LAST_UPDATE_TIME > SYSDATE - 10/1440 \n"
			+ "  and o.SID in (" + sidList + ") \n"
			+ "order by o.SID, o.START_TIME";

		try (Statement stmnt = conn.createStatement())
		{
			stmnt.setQueryTimeout(LOOKUP_QUERY_TIMEOUT_SEC);

			try (ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
				{
					Integer rowId = sidToRowId.get(rs.getLong(1));
					if (rowId == null || rs.getLong(2) != newSample.getValueAsLong(rowId, pos_SERIAL, -1L))
						continue;

					if ( ! "ACTIVE".equals(newSample.getValueAsString(rowId, pos_STATUS)) )
						continue;

					String opSqlId  = rs.getString(3);
					String rowSqlId = newSample.getValueAsString(rowId, pos_SQL_ID);
					if (StringUtil.hasValue(opSqlId) && StringUtil.hasValue(rowSqlId) && ! opSqlId.equals(rowSqlId))
						continue;

					String opName    = rs.getString(4);
					String target    = rs.getString(5);
					long   soFar     = rs.getLong  (6);
					long   totalWork = rs.getLong  (7);
					String units     = rs.getString(8);
					long   elapsed   = rs.getLong  (9);
					long   remaining = rs.getLong  (10);
					boolean hasRemaining = ! rs.wasNull();

					String info = opName + (StringUtil.hasValue(target) ? " " + target : "")
							+ ": " + (soFar * 100 / totalWork) + "% (" + soFar + " of " + totalWork + (StringUtil.hasValue(units) ? " " + units : "") + ")"
							+ ", " + elapsed + " seconds so far"
							+ (hasRemaining ? ", about " + remaining + " seconds left" : "");

					// A statement can have several long operations going (one per scan, sort...)
					String prevInfo = newSample.getValueAsString(rowId, pos_LongOpsInfo);
					if (StringUtil.hasValue(prevInfo))
						info = prevInfo + "; " + info;

					if (info.length() > 1000) // the size of the column
						info = info.substring(0, 997) + "...";

					newSample.setValueAt(info, rowId, pos_LongOpsInfo);
				}
			}
		}
		catch (SQLException ex)
		{
			lookupFailed("longOps", ex);
		}
	}

	/** One row of V$LOCK */
	private static class LockRow
	{
		String type;
		long   id1;
		long   id2;
		int    lmode;
		int    request;
		long   ctime;
		int    block;
		long   conId;
	}

	/**
	 * 'SidLocks' (a HTML table), 'SidLockCount' and 'HasSidLocks': the locks a session holds or waits for, from V$LOCK. One statement for all the sessions.
	 * <p>
	 * V$LOCK is read with nothing but a filter (joined with other views it has been slow in some Oracle versions).
	 * The names of the objects (of the table locks we show) are looked up in a second statement.
	 */
	private void lookupSidLocks(DbxConnection conn, CounterSample newSample, String sidList, Map<Long, Integer> sidToRowId)
	{
		int pos_HasSidLocks  = newSample.findColumn("HasSidLocks");
		int pos_SidLocks     = newSample.findColumn("SidLocks");
		int pos_SidLockCount = newSample.findColumn("SidLockCount");

		if (pos_HasSidLocks == -1 || pos_SidLocks == -1 || pos_SidLockCount == -1)
			return;

		if ( ! isLookupDue("sidLocks") )
			return;

		// What the lock types are called: read once (if it can't be read, the table has the type only)
		if (_lockTypeNames.isEmpty())
		{
			try (Statement stmnt = conn.createStatement())
			{
				stmnt.setQueryTimeout(LOOKUP_QUERY_TIMEOUT_SEC);

				try (ResultSet rs = stmnt.executeQuery("select TYPE, NAME from V$LOCK_TYPE"))
				{
					while (rs.next())
						_lockTypeNames.put(rs.getString(1), rs.getString(2));
				}
			}
			catch (SQLException ex)
			{
				_logger.debug(getName() + ": Problems reading V$LOCK_TYPE (the lock table will only have the lock TYPE). Caught: " + ex);
			}

			// The two that matters, also if V$LOCK_TYPE could not be read (and then we do not try it again in every sample)
			_lockTypeNames.putIfAbsent("TM", "DML");
			_lockTypeNames.putIfAbsent("TX", "Transaction");
		}

		// The locks: 'AE' is the edition lock that EVERY session holds, so it is left out
		// First the ones that makes others wait, then the ones the session waits for
		String sql = ""
			+ "select l.SID, l.TYPE, l.ID1, l.ID2, l.LMODE, l.REQUEST, l.CTIME, l.BLOCK, " + (_isCdbSql ? "l.CON_ID" : "0") + " \n"
			+ "from V$LOCK l \n"
			+ "where l.TYPE <> 'AE' \n"
			+ "  and l.SID in (" + sidList + ") \n"
			+ "order by l.SID, case when l.BLOCK = 1 then 0 when l.REQUEST > 0 then 1 else 2 end, l.TYPE, l.ID1, l.ID2";

		Map<Long, List<LockRow>> locksPerSid     = new LinkedHashMap<>(); // the first LOCKS_SHOWN_PER_SID of every session
		Map<Long, Integer>       lockCountPerSid = new HashMap<>();       // all of them
		Set<String>              keysToLookup    = new HashSet<>();       // CON_ID:OBJECT_ID of the table locks, that are not in _objectNameCache
		Set<Long>                idsToLookup     = new HashSet<>();

		try (Statement stmnt = conn.createStatement())
		{
			stmnt.setQueryTimeout(LOOKUP_QUERY_TIMEOUT_SEC);

			try (ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
				{
					long sid = rs.getLong(1);
					lockCountPerSid.merge(sid, 1, Integer::sum);

					List<LockRow> locks = locksPerSid.computeIfAbsent(sid, k -> new ArrayList<>());
					if (locks.size() >= LOCKS_SHOWN_PER_SID)
						continue;

					LockRow l = new LockRow();
					l.type    = rs.getString(2);
					l.id1     = rs.getLong  (3);
					l.id2     = rs.getLong  (4);
					l.lmode   = rs.getInt   (5);
					l.request = rs.getInt   (6);
					l.ctime   = rs.getLong  (7);
					l.block   = rs.getInt   (8);
					l.conId   = rs.getLong  (9);
					locks.add(l);

					// TM = a table lock: ID1 is the OBJECT_ID (max 900 new names in a sample, an IN list can hold 1000)
					if ("TM".equals(l.type) && ! _objectNameCache.containsKey(l.conId + ":" + l.id1) && idsToLookup.size() < 900)
					{
						keysToLookup.add(l.conId + ":" + l.id1);
						idsToLookup .add(l.id1);
					}
				}
			}
		}
		catch (SQLException ex)
		{
			lookupFailed("sidLocks", ex);
			return;
		}

		// The names of the locked objects we have not seen before (if this fails: the table has the OBJECT_ID instead of the name)
		if ( ! idsToLookup.isEmpty() && isLookupDue("sidLocks.objectNames") )
		{
			String namesSql = ""
				+ "select " + (_isCdbSql ? "o.CON_ID" : "0") + ", o.OBJECT_ID, o.OWNER, o.OBJECT_NAME, o.SUBOBJECT_NAME, o.OBJECT_TYPE \n"
				+ "from " + (_isCdbSql ? "CDB_OBJECTS" : "DBA_OBJECTS") + " o \n"
				+ "where o.OBJECT_ID in (" + StringUtil.toCommaStr(idsToLookup) + ")";

			try (Statement stmnt = conn.createStatement())
			{
				stmnt.setQueryTimeout(LOOKUP_QUERY_TIMEOUT_SEC);

				try (ResultSet rs = stmnt.executeQuery(namesSql))
				{
					while (rs.next())
					{
						// (in a Container Database: this is also the objects with the same OBJECT_ID in the other containers)
						String subObjectName = rs.getString(5);
						_objectNameCache.put(rs.getLong(1) + ":" + rs.getLong(2),
								rs.getString(3) + "." + rs.getString(4) + (StringUtil.hasValue(subObjectName) ? ":" + subObjectName : "") + " (" + rs.getString(6) + ")");
					}
				}

				// The ones that were not found: remember that as well, so we do not ask for them in every sample
				for (String key : keysToLookup)
					_objectNameCache.putIfAbsent(key, "");
			}
			catch (SQLException ex)
			{
				lookupFailed("sidLocks.objectNames", ex);
			}
		}

		// One HTML table per session (it starts with "<TABLE": that is how CountersModel.toHtmlTableString() knows that it is HTML, when it makes the alarm description)
		for (Map.Entry<Long, List<LockRow>> entry : locksPerSid.entrySet())
		{
			long    sid       = entry.getKey();
			Integer rowId     = sidToRowId.get(sid);
			int     lockCount = lockCountPerSid.get(sid);
			if (rowId == null)
				continue;

			StringBuilder sb = new StringBuilder(1024);
			sb.append("<TABLE class='dbx-table-basic' BORDER=1>");
			sb.append("<TR> <TH>SID</TH> <TH>State</TH> <TH>Type</TH> <TH>Lock Name</TH> <TH>Mode Held</TH> <TH>Mode Requested</TH> <TH>Object / Resource</TH> <TH>Seconds</TH> </TR>");

			for (LockRow l : entry.getValue())
			{
				// What is locked
				String resource;
				if ("TM".equals(l.type))
				{
					String objectName = _objectNameCache.get(l.conId + ":" + l.id1);
					resource = StringUtil.hasValue(objectName) ? objectName : "OBJECT_ID=" + l.id1;
				}
				else if ("TX".equals(l.type))
				{
					// The transaction: undo segment, slot and sequence (as XIDUSN, XIDSLOT and XIDSQN in V$TRANSACTION)
					resource = "Transaction XID=" + (l.id1 >> 16) + "." + (l.id1 & 0xFFFF) + "." + l.id2;
				}
				else
				{
					resource = "ID1=" + l.id1 + ", ID2=" + l.id2;
				}

				// BLOCK: 1 = someone waits for this lock. 2 = a global lock (RAC), where Oracle does not tell if someone on another instance waits for it
				String state;
				if      (l.block == 1)  { state = "BLOCKING";                 sb.append("<TR style='color: red;'>");    }
				else if (l.request > 0) { state = "WAITING";                  sb.append("<TR style='color: orange;'>"); }
				else if (l.block == 2)  { state = "HOLDING (global, RAC)";    sb.append("<TR>");                        }
				else                    { state = "HOLDING";                  sb.append("<TR>");                        }

				sb.append("<TD>").append(sid).append("</TD>");
				sb.append("<TD>").append(state).append("</TD>");
				sb.append("<TD>").append(StringEscapeUtils.escapeHtml4(l.type)).append("</TD>");
				sb.append("<TD>").append(StringEscapeUtils.escapeHtml4(_lockTypeNames.getOrDefault(l.type, ""))).append("</TD>");
				sb.append("<TD>").append(l.lmode   >= 0 && l.lmode   < LOCK_MODE_NAMES.length ? LOCK_MODE_NAMES[l.lmode]   : "" + l.lmode  ).append("</TD>");
				sb.append("<TD>").append(l.request >= 0 && l.request < LOCK_MODE_NAMES.length ? LOCK_MODE_NAMES[l.request] : "" + l.request).append("</TD>");
				sb.append("<TD>").append(StringEscapeUtils.escapeHtml4(resource)).append("</TD>");
				sb.append("<TD>").append(l.ctime).append("</TD>");
				sb.append("</TR>");
			}

			if (lockCount > entry.getValue().size())
				sb.append("<TR> <TD COLSPAN=8><I>... and ").append(lockCount - entry.getValue().size()).append(" more locks (the session has ").append(lockCount).append(")</I></TD> </TR>");

			sb.append("</TABLE>");

			newSample.setValueAt(sb.toString()             , rowId, pos_SidLocks);
			newSample.setValueAt(Boolean.TRUE              , rowId, pos_HasSidLocks);
			newSample.setValueAt(Integer.valueOf(lockCount), rowId, pos_SidLockCount);
		}

		// The sessions that have no lock at all
		for (Integer rowId : sidToRowId.values())
		{
			if (newSample.getValueAsLong(rowId, pos_SidLockCount, -1L) == -1)
				newSample.setValueAt(Integer.valueOf(0), rowId, pos_SidLockCount);
		}
	}

	/** One row of V$SESSION_EVENT, with what has happened since the previous sample */
	private static class WaitEventRow
	{
		String event;
		String waitClass;
		long   waits;
		long   micro;
		long   maxWaitCs;
		long   waitsLastSample = -1; // -1 = the session was not here in the previous sample
		long   microLastSample = -1;
	}

	/**
	 * 'SidWaitInfo' (HTML) and 'HasSidWaitInfo': what a session has waited for. Three statements for all the sessions:
	 * <ul>
	 *   <li>V$SESS_TIME_MODEL      - 'DB time' and 'DB CPU': how much the session has worked, and how much of that was CPU</li>
	 *   <li>V$SESSION_EVENT        - every wait event since logon. We keep the previous values, to tell what happened since the previous sample</li>
	 *   <li>V$SESSION_WAIT_HISTORY - the last 10 waits</li>
	 * </ul>
	 * If one of them fails, the others are still shown
	 */
	private void lookupSidWaits(DbxConnection conn, CounterSample newSample, String sidList, Map<Long, Integer> sidToRowId)
	{
		int pos_SERIAL         = newSample.findColumn("SERIAL#");
		int pos_HasSidWaitInfo = newSample.findColumn("HasSidWaitInfo");
		int pos_SidWaitInfo    = newSample.findColumn("SidWaitInfo");

		if (pos_SERIAL == -1 || pos_HasSidWaitInfo == -1 || pos_SidWaitInfo == -1)
			return;

		long now                = System.currentTimeMillis();
		long secSincePrevSample = _prevSidWaitsTime == 0 ? -1 : Math.round((now - _prevSidWaitsTime) / 1000.0);

		//---- V$SESS_TIME_MODEL: {DB time, DB CPU} per session
		Map<Long  , long[]> timeModel       = new HashMap<>();
		Map<String, long[]> timeModelToKeep = new HashMap<>();
		if (isLookupDue("sidWaits.timeModel"))
		{
			String sql = "select m.SID, m.STAT_NAME, m.VALUE from V$SESS_TIME_MODEL m where m.STAT_NAME in ('DB time', 'DB CPU') and m.SID in (" + sidList + ")";

			try (Statement stmnt = conn.createStatement())
			{
				stmnt.setQueryTimeout(LOOKUP_QUERY_TIMEOUT_SEC);

				try (ResultSet rs = stmnt.executeQuery(sql))
				{
					while (rs.next())
					{
						long   sid = rs.getLong(1);
						long[] val = timeModel.computeIfAbsent(sid, k -> new long[] {0, 0});
						val["DB time".equals(rs.getString(2)) ? 0 : 1] = rs.getLong(3);
					}
				}
			}
			catch (SQLException ex)
			{
				timeModel.clear();
				lookupFailed("sidWaits.timeModel", ex);
			}
		}

		//---- V$SESSION_EVENT: the wait events per session
		Map<Long  , List<WaitEventRow>> events       = new HashMap<>();
		Map<String, long[]>             eventsToKeep = new HashMap<>();
		if (isLookupDue("sidWaits.events"))
		{
			String sql = ""
				+ "select e.SID, e.EVENT, e.WAIT_CLASS, e.TOTAL_WAITS, e.TIME_WAITED_MICRO, e.MAX_WAIT \n"
				+ "from V$SESSION_EVENT e \n"
				+ "where e.WAIT_CLASS <> 'Idle' \n"
				+ "  and e.SID in (" + sidList + ")";

			// The key "SID:SERIAL#|" (no event) tells that the session was looked up: an event that is new in the next sample, has then happened since this one
			for (Map.Entry<Long, Integer> entry : sidToRowId.entrySet())
				eventsToKeep.put(entry.getKey() + ":" + newSample.getValueAsLong(entry.getValue(), pos_SERIAL, -1L) + "|", new long[] {0, 0});

			try (Statement stmnt = conn.createStatement())
			{
				stmnt.setQueryTimeout(LOOKUP_QUERY_TIMEOUT_SEC);

				try (ResultSet rs = stmnt.executeQuery(sql))
				{
					while (rs.next())
					{
						long    sid   = rs.getLong(1);
						Integer rowId = sidToRowId.get(sid);
						if (rowId == null)
							continue;

						WaitEventRow e = new WaitEventRow();
						e.event     = rs.getString(2);
						e.waitClass = rs.getString(3);
						e.waits     = rs.getLong  (4);
						e.micro     = rs.getLong  (5);
						e.maxWaitCs = rs.getLong  (6);

						// Since the previous sample (the counters are since logon. SERIAL# is in the key: a new session can get the same SID)
						String sessionKey = sid + ":" + newSample.getValueAsLong(rowId, pos_SERIAL, -1L) + "|";
						long[] prev       = _prevSessionEvents.get(sessionKey + e.event);
						if (prev == null && _prevSessionEvents.containsKey(sessionKey))
							prev = new long[] {0, 0};

						if (prev != null && e.waits >= prev[0] && e.micro >= prev[1])
						{
							e.waitsLastSample = e.waits - prev[0];
							e.microLastSample = e.micro - prev[1];
						}
						eventsToKeep.put(sessionKey + e.event, new long[] {e.waits, e.micro});

						events.computeIfAbsent(sid, k -> new ArrayList<>()).add(e);
					}
				}
			}
			catch (SQLException ex)
			{
				events      .clear();
				eventsToKeep.clear();
				lookupFailed("sidWaits.events", ex);
			}
		}

		//---- V$SESSION_WAIT_HISTORY: the last 10 waits per session, as HTML table rows (SEQ# 1 is the latest)
		Map<Long, StringBuilder> waitHistory = new HashMap<>();
		if (isLookupDue("sidWaits.history"))
		{
			String sql = ""
				+ "select h.SID, h.SEQ#, h.EVENT, h.WAIT_TIME_MICRO, h.P1TEXT, h.P1, h.P2TEXT, h.P2, h.P3TEXT, h.P3 \n"
				+ "from V$SESSION_WAIT_HISTORY h \n"
				+ "where h.SID in (" + sidList + ") \n"
				+ "order by h.SID, h.SEQ#";

			try (Statement stmnt = conn.createStatement())
			{
				stmnt.setQueryTimeout(LOOKUP_QUERY_TIMEOUT_SEC);

				try (ResultSet rs = stmnt.executeQuery(sql))
				{
					while (rs.next())
					{
						String params = "";
						for (int c = 5; c <= 9; c += 2)
						{
							if (StringUtil.hasValue(rs.getString(c)))
								params += (params.isEmpty() ? "" : ", ") + rs.getString(c) + "=" + rs.getLong(c + 1);
						}

						waitHistory.computeIfAbsent(rs.getLong(1), k -> new StringBuilder())
							.append("<TR>")
							.append("<TD>").append(rs.getInt(2)).append("</TD>")
							.append("<TD>").append(StringEscapeUtils.escapeHtml4(rs.getString(3))).append("</TD>")
							.append("<TD ALIGN='right'>").append(String.format(Locale.ENGLISH, "%.3f", rs.getLong(4) / 1000.0)).append("</TD>")
							.append("<TD>").append(StringEscapeUtils.escapeHtml4(params)).append("</TD>")
							.append("</TR>");
					}
				}
			}
			catch (SQLException ex)
			{
				waitHistory.clear();
				lookupFailed("sidWaits.history", ex);
			}
		}

		//---- One HTML text per session
		String lastSample = secSincePrevSample < 0 ? "Last Sample" : "Last " + secSincePrevSample + " sec";

		for (Map.Entry<Long, Integer> entry : sidToRowId.entrySet())
		{
			long   sid     = entry.getKey();
			int    rowId   = entry.getValue();
			String prevKey = sid + ":" + newSample.getValueAsLong(rowId, pos_SERIAL, -1L);

			long[]             tm        = timeModel  .get(sid);
			List<WaitEventRow> eventList = events     .get(sid);
			StringBuilder      history   = waitHistory.get(sid);

			if (tm == null && eventList == null && history == null)
				continue;

			// (it starts with "<TABLE": that is how CountersModel.toHtmlTableString() knows that it is HTML, when it makes the alarm description)
			StringBuilder sb = new StringBuilder(2048);

			if (tm != null || eventList != null)
			{
				sb.append("<TABLE class='dbx-table-basic' BORDER=1>");
				sb.append("<TR> <TH>What</TH> <TH>Wait Class</TH>")
				  .append(" <TH>").append(lastSample).append(": Waits</TH> <TH>").append(lastSample).append(": Time ms</TH>")
				  .append(" <TH>Since Logon: Waits</TH> <TH>Since Logon: Time ms</TH> <TH>Avg ms</TH> <TH>Max ms</TH> </TR>");

				// How much the session has worked (DB time), and how much of that was on CPU. The rest is the waits below
				if (tm != null)
				{
					long[] prev = _prevSessTimeModel.get(prevKey);
					timeModelToKeep.put(prevKey, tm);

					String[] names = { "DB time (working: on CPU or waiting, but not idle)", "DB CPU" };
					for (int i = 0; i < 2; i++)
					{
						sb.append("<TR>");
						sb.append("<TD><B>").append(names[i]).append("</B></TD>");
						sb.append("<TD>").append(i == 1 ? "CPU" : "").append("</TD>");
						sb.append("<TD></TD>");
						sb.append("<TD ALIGN='right'>").append(prev != null && tm[i] >= prev[i] ? "" + (tm[i] - prev[i]) / 1000 : "").append("</TD>");
						sb.append("<TD></TD>");
						sb.append("<TD ALIGN='right'>").append(tm[i] / 1000).append("</TD>");
						sb.append("<TD></TD>");
						sb.append("<TD></TD>");
						sb.append("</TR>");
					}
				}

				// The wait events: the ones with most time in the last sample first, then the ones with most time since logon
				if (eventList != null)
				{
					eventList.sort((a, b) -> a.microLastSample != b.microLastSample ? Long.compare(b.microLastSample, a.microLastSample) : Long.compare(b.micro, a.micro));

					for (int i = 0; i < eventList.size() && i < EVENTS_SHOWN_PER_SID; i++)
					{
						WaitEventRow e = eventList.get(i);

						sb.append("<TR>");
						sb.append("<TD>").append(StringEscapeUtils.escapeHtml4(e.event)).append("</TD>");
						sb.append("<TD>").append(StringEscapeUtils.escapeHtml4(e.waitClass)).append("</TD>");
						sb.append("<TD ALIGN='right'>").append(e.waitsLastSample < 0 ? "" : "" + e.waitsLastSample).append("</TD>");
						sb.append("<TD ALIGN='right'>").append(e.microLastSample < 0 ? "" : "" + e.microLastSample / 1000).append("</TD>");
						sb.append("<TD ALIGN='right'>").append(e.waits).append("</TD>");
						sb.append("<TD ALIGN='right'>").append(e.micro / 1000).append("</TD>");
						sb.append("<TD ALIGN='right'>").append(e.waits <= 0 ? "" : String.format(Locale.ENGLISH, "%.1f", e.micro / 1000.0 / e.waits)).append("</TD>");
						sb.append("<TD ALIGN='right'>").append(e.maxWaitCs <= 0 ? "" : "" + e.maxWaitCs * 10).append("</TD>"); // MAX_WAIT is in centiseconds: empty = less than 10 ms
						sb.append("</TR>");
					}

					if (eventList.size() > EVENTS_SHOWN_PER_SID)
						sb.append("<TR> <TD COLSPAN=8><I>... and ").append(eventList.size() - EVENTS_SHOWN_PER_SID).append(" more wait events with less time</I></TD> </TR>");
				}

				sb.append("</TABLE>");
			}

			if (history != null)
			{
				sb.append("<TABLE class='dbx-table-basic' BORDER=1>");
				sb.append("<TR> <TH>Latest Waits</TH> <TH>Wait Event</TH> <TH>Waited ms</TH> <TH>Parameters</TH> </TR>");
				sb.append(history);
				sb.append("</TABLE>");
			}

			newSample.setValueAt(sb.toString(), rowId, pos_SidWaitInfo);
			newSample.setValueAt(Boolean.TRUE , rowId, pos_HasSidWaitInfo);
		}

		// For the next sample
		_prevSessionEvents = eventsToKeep;
		_prevSessTimeModel = timeModelToKeep;
		_prevSidWaitsTime  = (eventsToKeep.isEmpty() && timeModelToKeep.isEmpty()) ? 0 : now;
	}

	/**
	 * 'BlockedSidsInfo' (a HTML table) and 'HasBlockedSidsInfo': for a session that blocks others, the sessions that wait for it:
	 * who they are, what they execute, what they wait for and their locks. All of it from the other rows of the sample.
	 */
	private void setBlockedSidsInfo(CounterSample newSample)
	{
		int pos_SID                = newSample.findColumn("SID");
		int pos_BlockedBySid       = newSample.findColumn("BlockedBySid");
		int pos_SqlText            = newSample.findColumn("SqlText");
		int pos_SidLocks           = newSample.findColumn("SidLocks");
		int pos_HasBlockedSidsInfo = newSample.findColumn("HasBlockedSidsInfo");
		int pos_BlockedSidsInfo    = newSample.findColumn("BlockedSidsInfo");

		if (pos_SID == -1 || pos_BlockedBySid == -1 || pos_SqlText == -1 || pos_SidLocks == -1 || pos_HasBlockedSidsInfo == -1 || pos_BlockedSidsInfo == -1)
			return;

		// The columns of the blocked session that goes into the "Info" cell (the ones that have a value)
		String[] infoColumns = { "USERNAME", "STATUS", "OSUSER", "MACHINE", "PROGRAM", "MODULE", "ACTION", "BlockedSeconds", "EVENT", "RowWaitObject", "RowWaitRowid", "SQL_ID", "SQL_EXEC_START", "PlsqlEntryObject", "TxnStartTime" };

		for (int rowId = 0; rowId < newSample.getRowCount(); rowId++)
		{
			long sid = newSample.getValueAsLong(rowId, pos_SID, -1L);

			StringBuilder sb = new StringBuilder();

			for (int r = 0; r < newSample.getRowCount(); r++)
			{
				if (newSample.getValueAsLong(r, pos_BlockedBySid, 0L) != sid)
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

				String locks = newSample.getValueAsString(r, pos_SidLocks);

				sb.append("  <TR>\n");
				sb.append("    <TD><B>").append(newSample.getValueAsLong(r, pos_SID, -1L)).append("</B></TD>\n");
				sb.append("    <TD>").append(info).append("</TD>\n");
				sb.append("    <TD><PRE>").append(sqlText == null ? "" : StringEscapeUtils.escapeHtml4(sqlText)).append("</PRE></TD>\n");
				sb.append("    <TD>").append(locks == null ? "" : locks).append("</TD>\n");
				sb.append("  </TR>\n");
			}

			if (sb.length() > 0)
			{
				// (it starts with "<TABLE": that is how CountersModel.toHtmlTableString() knows that it is HTML, when it makes the alarm description)
				String html = "<TABLE BORDER=1 class='dbx-table-basic'>\n"
						+ "  <TR> <TH>Blocked SID</TH> <TH>Info</TH> <TH>SQL Text</TH> <TH>Locks</TH> </TR>\n"
						+ sb
						+ "</TABLE>\n";

				newSample.setValueAt(html        , rowId, pos_BlockedSidsInfo);
				newSample.setValueAt(Boolean.TRUE, rowId, pos_HasBlockedSidsInfo);
			}
		}
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
			final long sid      = cm.getAbsValueAsLong(r, "SID", -1L);
			String     username = cm.getAbsString(r, "USERNAME");
			String     program  = cm.getAbsString(r, "PROGRAM");
			String     status   = cm.getAbsString(r, "STATUS");

			//-------------------------------------------------------
			// ImBlockingOthersMaxTimeInSec: This session blocks others, and is NOT blocked itself (it is the root cause)
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("ImBlockingOthersMaxTimeInSec"))
			{
				String blockingOtherSids    = cm.getAbsString(r, "BlockingOtherSids");
				long   blockedBySid         = cm.getAbsValueAsLong(r, "BlockedBySid"        , 0L);
				long   blockingOthersMaxSec = cm.getAbsValueAsLong(r, "BlockingOthersMaxSec", 0L);

				if (StringUtil.hasValue(blockingOtherSids) && blockedBySid == 0)
				{
					int threshold = conf.getIntProperty(PROPKEY_alarm_ImBlockingOthersMaxTimeInSec, DEFAULT_alarm_ImBlockingOthersMaxTimeInSec);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest(" + cm.getName() + "): ImBlockingOthersMaxTimeInSec: sid=" + sid + ", threshold=" + threshold + ", BlockingOthersMaxSec='" + blockingOthersMaxSec + "', BlockingOtherSids='" + blockingOtherSids + "'.");

					if (blockingOthersMaxSec > threshold)
					{
						int blockCount = StringUtil.commaStrToList(blockingOtherSids).size();

						AlarmEvent ae = new AlarmEventOraBlockingSession(cm, threshold, sid, username, program, blockingOthersMaxSec, "[" + blockingOtherSids + "]", blockCount);

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
									int    rowId = cm.getRowIdWhere(DATA_ABS, "SID", sid);
									String label = "SID " + sid;

									if (rowId == -1)
										alarmEvent.setCancelDescription(label + " has no active statement or open transaction now");
									else
									{
										String BlockingOtherSids    = cm.getAbsString(rowId, "BlockingOtherSids");
										Object BlockingOthersMaxSec = cm.getAbsValue (rowId, "BlockingOthersMaxSec");

										String cancelMsg = StringUtil.isNullOrBlank(BlockingOtherSids)
										                 ? label + " is no longer blocking any session"
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
			// HoldingLocksWhileWaitForClientInputInSec: an open transaction, and the session is waiting for the client
			// 'ExecTimeInSec' is for an INACTIVE session: how long since it became inactive (when the last call from the client ended)
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("HoldingLocksWhileWaitForClientInputInSec"))
			{
				String hasOpenTxn    = cm.getAbsString(r, "HasOpenTxn");
				long   execTimeInSec = cm.getAbsValueAsLong(r, "ExecTimeInSec", 0L);

				if ("Y".equals(hasOpenTxn) && "INACTIVE".equals(status))
				{
					int threshold = conf.getIntProperty(PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSec, DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSec);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest(" + cm.getName() + "): HoldingLocksWhileWaitForClientInputInSec: sid=" + sid + ", threshold=" + threshold + ", ExecTimeInSec='" + execTimeInSec + "', STATUS='" + status + "', HasOpenTxn='" + hasOpenTxn + "'.");

					if (execTimeInSec > threshold)
					{
						String lastCallTime = cm.getAbsString(r, "LastCallTime");

						AlarmEvent ae = new AlarmEventHoldingLocksWhileWaitForClientInput(cm, threshold, sid, (int) execTimeInSec, lastCallTime, true);

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
									int    rowId = cm.getRowIdWhere(DATA_ABS, "SID", sid);
									String label = "SID " + sid;

									if (rowId == -1)
										alarmEvent.setCancelDescription(label + " has no open transaction now");
									else
									{
										Object STATUS        = cm.getAbsValue(rowId, "STATUS");
										Object ExecTimeInSec = cm.getAbsValue(rowId, "ExecTimeInSec");

										String cancelMsg = getAlarmCancelText(label + ": status", "'" + STATUS + "' for " + ExecTimeInSec + " seconds", " seconds");
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
			// StatementExecInSec: executing the same call for a long time
			// 'ExecTimeInSec' is for an ACTIVE session: how long since it became active (when the client sent the call)
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("StatementExecInSec"))
			{
				String sqlId         = cm.getAbsString(r, "SQL_ID");
				String module        = cm.getAbsString(r, "MODULE");
				long   execTimeInSec = cm.getAbsValueAsLong(r, "ExecTimeInSec", 0L);

				if ("ACTIVE".equals(status) && StringUtil.hasValue(sqlId))
				{
					int threshold = conf.getIntProperty(PROPKEY_alarm_StatementExecInSec, DEFAULT_alarm_StatementExecInSec);

					// Skip some users, programs or modules (for example backup or batch jobs that we know takes a long time)
					String skipUsernameRegex = conf.getProperty(PROPKEY_alarm_StatementExecInSecSkipUsername, DEFAULT_alarm_StatementExecInSecSkipUsername);
					String skipProgramRegex  = conf.getProperty(PROPKEY_alarm_StatementExecInSecSkipProgram , DEFAULT_alarm_StatementExecInSecSkipProgram);
					String skipModuleRegex   = conf.getProperty(PROPKEY_alarm_StatementExecInSecSkipModule  , DEFAULT_alarm_StatementExecInSecSkipModule);

					boolean skip = (StringUtil.hasValue(skipUsernameRegex) && username.matches(skipUsernameRegex))
					            || (StringUtil.hasValue(skipProgramRegex)  && program .matches(skipProgramRegex))
					            || (StringUtil.hasValue(skipModuleRegex)   && module  .matches(skipModuleRegex));

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest(" + cm.getName() + "): StatementExecInSec: sid=" + sid + ", threshold=" + threshold + ", ExecTimeInSec='" + execTimeInSec + "', skip=" + skip + ", USERNAME='" + username + "', PROGRAM='" + program + "', MODULE='" + module + "'.");

					if (execTimeInSec > threshold && ! skip)
					{
						String sqlExecStart = cm.getAbsString(r, "SQL_EXEC_START");

						AlarmEvent ae = new AlarmEventOraLongRunningStatement(cm, threshold, sid, execTimeInSec, sqlExecStart, username, program, sqlId);

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
									int    rowId = cm.getRowIdWhere(DATA_ABS, "SID", sid);
									String label = "SID " + sid;

									if (rowId == -1)
										alarmEvent.setCancelDescription(label + " has no active statement now");
									else
									{
										Object STATUS        = cm.getAbsValue(rowId, "STATUS");
										Object ExecTimeInSec = cm.getAbsValue(rowId, "ExecTimeInSec");

										String cancelMsg = getAlarmCancelText(label + ": status", "'" + STATUS + "' for " + ExecTimeInSec + " seconds", " seconds");
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

	public static final String  PROPKEY_alarm_StatementExecInSecSkipUsername           = CM_NAME + ".alarm.system.if.StatementExecInSec.skip.username";
	public static final String  DEFAULT_alarm_StatementExecInSecSkipUsername           = "";

	public static final String  PROPKEY_alarm_StatementExecInSecSkipProgram            = CM_NAME + ".alarm.system.if.StatementExecInSec.skip.program";
	public static final String  DEFAULT_alarm_StatementExecInSecSkipProgram            = "";

	public static final String  PROPKEY_alarm_StatementExecInSecSkipModule             = CM_NAME + ".alarm.system.if.StatementExecInSec.skip.module";
	public static final String  DEFAULT_alarm_StatementExecInSecSkipModule             = "";

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("ImBlockingOthersMaxTimeInSec"            , isAlarmSwitch, PROPKEY_alarm_ImBlockingOthersMaxTimeInSec            , Integer.class, conf.getIntProperty(PROPKEY_alarm_ImBlockingOthersMaxTimeInSec            , DEFAULT_alarm_ImBlockingOthersMaxTimeInSec            ), DEFAULT_alarm_ImBlockingOthersMaxTimeInSec            , "If 'BlockingOthersMaxSec' is greater than ## seconds (and the session is not blocked itself) then send 'AlarmEventOraBlockingSession'." ));
		list.add(new CmSettingsHelper("HoldingLocksWhileWaitForClientInputInSec", isAlarmSwitch, PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSec, Integer.class, conf.getIntProperty(PROPKEY_alarm_HoldingLocksWhileWaitForClientInputInSec, DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSec), DEFAULT_alarm_HoldingLocksWhileWaitForClientInputInSec, "If a session has an open transaction ('HasOpenTxn' is Y), is INACTIVE (it waits for the client) and 'ExecTimeInSec' is greater than ## seconds then send 'AlarmEventHoldingLocksWhileWaitForClientInput'." ));
		list.add(new CmSettingsHelper("StatementExecInSec"                      , isAlarmSwitch, PROPKEY_alarm_StatementExecInSec                      , Integer.class, conf.getIntProperty(PROPKEY_alarm_StatementExecInSec                      , DEFAULT_alarm_StatementExecInSec                      ), DEFAULT_alarm_StatementExecInSec                      , "If a session is ACTIVE and 'ExecTimeInSec' is greater than ## seconds then send 'AlarmEventOraLongRunningStatement'." ));
		list.add(new CmSettingsHelper("StatementExecInSec SkipUsername"         ,                PROPKEY_alarm_StatementExecInSecSkipUsername          , String .class, conf.getProperty   (PROPKEY_alarm_StatementExecInSecSkipUsername          , DEFAULT_alarm_StatementExecInSecSkipUsername          ), DEFAULT_alarm_StatementExecInSecSkipUsername          , "If 'StatementExecInSec' is true; Skip sessions where 'USERNAME' matches this regexp.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("StatementExecInSec SkipProgram"          ,                PROPKEY_alarm_StatementExecInSecSkipProgram           , String .class, conf.getProperty   (PROPKEY_alarm_StatementExecInSecSkipProgram           , DEFAULT_alarm_StatementExecInSecSkipProgram           ), DEFAULT_alarm_StatementExecInSecSkipProgram           , "If 'StatementExecInSec' is true; Skip sessions where 'PROGRAM' matches this regexp." , new RegExpInputValidator()));
		list.add(new CmSettingsHelper("StatementExecInSec SkipModule"           ,                PROPKEY_alarm_StatementExecInSecSkipModule            , String .class, conf.getProperty   (PROPKEY_alarm_StatementExecInSecSkipModule            , DEFAULT_alarm_StatementExecInSecSkipModule            ), DEFAULT_alarm_StatementExecInSecSkipModule            , "If 'StatementExecInSec' is true; Skip sessions where 'MODULE' matches this regexp."  , new RegExpInputValidator()));

		return list;
	}

	@Override
	public void initLocalToolTipTextOnTableColumnHeader()
	{
		setLocalToolTipTextOnTableColumnHeader("SID",                  "<html>Oracle session identifier.</html>");
		setLocalToolTipTextOnTableColumnHeader("SERIAL#",              "<html>Session serial number. Used together with SID to uniquely identify a session.</html>");
		setLocalToolTipTextOnTableColumnHeader("CON_ID",               "<html>Container ID the session is in. 0 = This is not a Container Database (or the version is below 12.1). 1 = CDB$ROOT, 3 and above = a Pluggable Database (PDB).</html>");
		setLocalToolTipTextOnTableColumnHeader("CON_NAME",             "<html>Name of the Container (PDB) the session is in. NULL if this is not a Container Database.</html>");
		setLocalToolTipTextOnTableColumnHeader("USERNAME",             "<html>Oracle username of the session.</html>");
		setLocalToolTipTextOnTableColumnHeader("STATUS",               "<html>Status of the session: ACTIVE (currently executing SQL) or INACTIVE (waiting for the client).</html>");
		setLocalToolTipTextOnTableColumnHeader("OSUSER",               "<html>Operating system client username.</html>");
		setLocalToolTipTextOnTableColumnHeader("MACHINE",              "<html>Operating system machine name of the client.</html>");
		setLocalToolTipTextOnTableColumnHeader("PROGRAM",              "<html>Operating system program name of the client.</html>");
		setLocalToolTipTextOnTableColumnHeader("MODULE",               "<html>Module name set by the client application (via DBMS_APPLICATION_INFO).</html>");
		setLocalToolTipTextOnTableColumnHeader("ACTION",               "<html>Action name set by the client application (via DBMS_APPLICATION_INFO).</html>");
		setLocalToolTipTextOnTableColumnHeader("CLIENT_INFO",          "<html>Client info string set by the application (via DBMS_APPLICATION_INFO).</html>");
		setLocalToolTipTextOnTableColumnHeader("LOGON_TIME",           "<html>Time at which the session logged on.</html>");
		setLocalToolTipTextOnTableColumnHeader("MultiSampled",         "<html>YES = the session was executing the same statement (same SQL_ID, started at the same time) in the previous sample. So it has been running for more than one sample.</html>");
		setLocalToolTipTextOnTableColumnHeader("HasSqlText",           "<html>Checked if the row has a SQL Text (column 'SqlText'). Hover over the checkbox to see it.</html>");
		setLocalToolTipTextOnTableColumnHeader("HasExecPlan",          "<html>Checked if the row has an Execution Plan (column 'ExecPlan'). Hover over the checkbox to see it.<br>A plan is only fetched for statements that has been executing for some seconds (see the options).</html>");
		setLocalToolTipTextOnTableColumnHeader("HasSidLocks",          "<html>Checked if the session holds (or waits for) any locks (column 'SidLocks'). Hover over the checkbox to see them.</html>");
		setLocalToolTipTextOnTableColumnHeader("HasBlockedSidsInfo",   "<html>Checked if the session blocks other sessions (column 'BlockedSidsInfo'). Hover over the checkbox to see who they are, what they execute and their locks.</html>");
		setLocalToolTipTextOnTableColumnHeader("HasSidWaitInfo",       "<html>Checked if we know what the session has waited for (column 'SidWaitInfo'). Hover over the checkbox to see it.</html>");
		setLocalToolTipTextOnTableColumnHeader("SidLockCount",         "<html>Number of locks the session holds or waits for, in V$LOCK (the edition lock 'AE', that every session has, is not counted).<br>-1 = not looked up: V$LOCK is only read when some session in the table has an open transaction, or waits for another session (or the option 'Get SID Locks' is off).</html>");
		setLocalToolTipTextOnTableColumnHeader("ExecTimeInSec",        "<html>LAST_CALL_ET: For an ACTIVE session: seconds since it became active (how long the current call has been executing).<br>For an INACTIVE session: seconds since it became inactive (how long it has waited for the client).</html>");
		setLocalToolTipTextOnTableColumnHeader("LastCallTime",         "<html>The time when the session became ACTIVE (or INACTIVE).<br><b>Formula</b>: SYSDATE - LAST_CALL_ET</html>");
		setLocalToolTipTextOnTableColumnHeader("WAIT_CLASS",           "<html>Class of the wait event the session is waiting for (e.g. User I/O, Application, Network, Idle). If STATE is not WAITING: the LAST thing it waited for.</html>");
		setLocalToolTipTextOnTableColumnHeader("EVENT",                "<html>Wait event the session is waiting for. If STATE is not WAITING: the LAST thing it waited for.</html>");
		setLocalToolTipTextOnTableColumnHeader("WaitTimeMs",           "<html>WAIT_TIME_MICRO / 1000: If STATE is WAITING: milliseconds the session has waited for the current event. Otherwise: how long the last wait took.</html>");
		setLocalToolTipTextOnTableColumnHeader("STATE",                "<html>Wait state: WAITING (the session is waiting for EVENT). Anything else: the session is on CPU.</html>");
		setLocalToolTipTextOnTableColumnHeader("BlockedBySid",         "<html>BLOCKING_SESSION: The SID that blocks this session (it holds a lock that this session waits for). 0 = not blocked.</html>");
		setLocalToolTipTextOnTableColumnHeader("RootBlockerSid",       "<html>The session at the START of the blocking chain: if A waits for B, and B waits for C, then C is the root blocker of both A and B.<br>That is the session to look at (or to kill). 0 = not blocked.<br><b>Formula</b>: follow BlockedBySid until a session that is not blocked</html>");
		setLocalToolTipTextOnTableColumnHeader("BlockedSeconds",       "<html>How many seconds this session has been blocked. 0 = not blocked.</html>");
		setLocalToolTipTextOnTableColumnHeader("BlockingOtherSids",    "<html>The sessions (SID's) that are blocked by this session. If this session is not blocked itself (BlockedBySid is 0) it is the <b>root cause</b> of the blocking.</html>");
		setLocalToolTipTextOnTableColumnHeader("BlockingOthersMaxSec", "<html>How many seconds the session that has waited the longest for this session, has been blocked.</html>");
		setLocalToolTipTextOnTableColumnHeader("RowWaitObject",        "<html>If the session is blocked: the object (owner.name) it waits for a lock on.<br><b>Formula</b>: ROW_WAIT_OBJ# translated with DBA_OBJECTS</html>");
		setLocalToolTipTextOnTableColumnHeader("RowWaitRowid",         "<html>If the session waits for a row lock: the ROWID of the row it waits for. To see the row: <code>select * from &lt;RowWaitObject&gt; where ROWID = '&lt;RowWaitRowid&gt;'</code><br><b>Formula</b>: DBMS_ROWID.ROWID_CREATE from ROW_WAIT_OBJ#, ROW_WAIT_FILE#, ROW_WAIT_BLOCK# and ROW_WAIT_ROW#</html>");
		setLocalToolTipTextOnTableColumnHeader("SQL_ID",               "<html>SQL identifier of the SQL statement currently being executed. NULL if the session is not executing anything.</html>");
		setLocalToolTipTextOnTableColumnHeader("PREV_SQL_ID",          "<html>SQL identifier of the last SQL statement executed. Useful when SQL_ID is NULL (idle in transaction).</html>");
		setLocalToolTipTextOnTableColumnHeader("EffectiveSqlId",       "<html>COALESCE(SQL_ID, PREV_SQL_ID): The SQL shown in SqlText: the current if it is executing, otherwise the previous.</html>");
		setLocalToolTipTextOnTableColumnHeader("SQL_CHILD_NUMBER",     "<html>Child number of the SQL cursor. Combined with SQL_ID it identifies a cursor (and its execution plan) in the library cache.</html>");
		setLocalToolTipTextOnTableColumnHeader("SQL_EXEC_START",       "<html>Timestamp when execution of the current SQL statement began.</html>");
		setLocalToolTipTextOnTableColumnHeader("IsPrevSql",            "<html>Y if SQL_ID is NULL and we are showing PREV_SQL_ID instead (the session is not executing anything, but has an open transaction).</html>");
		setLocalToolTipTextOnTableColumnHeader("EffectiveChildNumber", "<html>The child number of the SQL shown in SqlText (and ExecPlan).</html>");
		setLocalToolTipTextOnTableColumnHeader("PLAN_HASH_VALUE",      "<html>Identifies the execution plan. If it changes for a SQL_ID, the statement has got a new execution plan.</html>");
		setLocalToolTipTextOnTableColumnHeader("PlsqlEntryObject",     "<html>The PL/SQL procedure, function or package that the client CALLED (the top of the call stack). Empty if the client sent plain SQL or an anonymous block.<br><b>Formula</b>: PLSQL_ENTRY_OBJECT_ID and PLSQL_ENTRY_SUBPROGRAM_ID translated with DBA_PROCEDURES</html>");
		setLocalToolTipTextOnTableColumnHeader("PlsqlCurrentObject",   "<html>The PL/SQL procedure, function or package that is executing right now. Empty when the session is executing SQL (also SQL inside PL/SQL).<br><b>Formula</b>: PLSQL_OBJECT_ID and PLSQL_SUBPROGRAM_ID translated with DBA_PROCEDURES</html>");
		setLocalToolTipTextOnTableColumnHeader("SqlText",              "<html>The SQL text (SQL_FULLTEXT from V$SQL).</html>");
		setLocalToolTipTextOnTableColumnHeader("HasOpenTxn",           "<html>Y if the session has an open transaction (it has an entry in V$TRANSACTION).</html>");
		setLocalToolTipTextOnTableColumnHeader("TxnStartTime",         "<html>START_DATE: Time when the transaction started.</html>");
		setLocalToolTipTextOnTableColumnHeader("TxnSeconds",           "<html>How many seconds the transaction has been open.</html>");
		setLocalToolTipTextOnTableColumnHeader("TxnUndoBlocks",        "<html>USED_UBLK: Number of undo blocks used by the transaction (how much it has changed).</html>");
		setLocalToolTipTextOnTableColumnHeader("TxnUndoRecords",       "<html>USED_UREC: Number of undo records used by the transaction (roughly: how many rows it has changed).</html>");
		setLocalToolTipTextOnTableColumnHeader("PgaUsedMb",            "<html>PGA_USED_MEM of the session's server process, in MB: the private memory it uses now (sorts, hash joins, PL/SQL collections...).</html>");
		setLocalToolTipTextOnTableColumnHeader("TempUsedMb",           "<html>Space the session uses in the temporary tablespace now, in MB (sorts and hash joins that did not fit in memory, temporary tables, temporary LOBs).<br><b>Formula</b>: sum(BLOCKS) in V$TEMPSEG_USAGE * db_block_size</html>");
		setLocalToolTipTextOnTableColumnHeader("LongOpsInfo",          "<html>How far the long operations of the statement have come (full scans, sorts, hash joins, backups...), with Oracle's guess of the time left.<br>From V$SESSION_LONGOPS. Oracle only reports operations it expects to take more than about 6 seconds, so it is often empty.</html>");
		setLocalToolTipTextOnTableColumnHeader("SidLocks",             "<html>A table with the locks the session holds or waits for, from V$LOCK.<br>"
		                                                             + "<b>TM</b> = a table lock (a session that changes rows holds one in mode 'Row Exclusive', which does not block other DML). <b>TX</b> = the transaction, which is also what a ROW lock is: a session that waits for a row, waits for the transaction that locked it (it requests its TX lock).<br>"
		                                                             + "<font color='red'>Red</font> = this lock makes another session wait. <font color='orange'>Orange</font> = the session waits for this lock.<br>"
		                                                             + "'Seconds' is the time since the lock was granted (or requested).</html>");
		setLocalToolTipTextOnTableColumnHeader("BlockedSidsInfo",      "<html>A table with the sessions that wait for this session: who they are, what they execute, what they wait for, and their locks.</html>");
		setLocalToolTipTextOnTableColumnHeader("SidWaitInfo",          "<html>What the session has spent its time on:<br>"
		                                                             + "<b>First table</b>: 'DB time' (the time it has been working), how much of that was CPU, and the wait events it has waited most for. Both since the previous sample and since it logged on. From V$SESS_TIME_MODEL and V$SESSION_EVENT.<br>"
		                                                             + "<b>Second table</b>: the last 10 waits (1 is the latest), from V$SESSION_WAIT_HISTORY.</html>");
		setLocalToolTipTextOnTableColumnHeader("ExecPlan",             "<html>Execution plan from DBMS_XPLAN.DISPLAY_CURSOR. Only for statements that has been executing for some seconds (see the options).</html>");
	}
}
