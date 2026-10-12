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

import java.lang.invoke.MethodHandles;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import com.dbxtune.gui.MainFrame;
import com.dbxtune.pcs.PcsColumnOptions;
import com.dbxtune.pcs.PcsColumnOptions.ColumnType;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoOracle;
import com.dbxtune.utils.Configuration;
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
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"V$SESSION", "V$TRANSACTION", "V$SQL"};
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

	// How many NEW execution plans do we fetch in ONE sample (the rest is fetched in the next sample, if the statement is still running)
	private static final int    EXEC_PLAN_MAX_FETCH_PER_SAMPLE = 5;
	private static final int    EXEC_PLAN_CACHE_SIZE           = 200;

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
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		list.add(new CmSettingsHelper("Get Execution Plan"             , PROPKEY_sample_execPlan                 , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_execPlan                 , DEFAULT_sample_execPlan                 ), DEFAULT_sample_execPlan                 , "Get the execution plan for active statements, using DBMS_XPLAN.DISPLAY_CURSOR. A plan is only fetched once (they are cached), and max " + EXEC_PLAN_MAX_FETCH_PER_SAMPLE + " new plans per sample." ));
		list.add(new CmSettingsHelper("Get Execution Plan, Min ExecTime", PROPKEY_sample_execPlan_minExecTimeInSec, Integer.class, conf.getIntProperty    (PROPKEY_sample_execPlan_minExecTimeInSec, DEFAULT_sample_execPlan_minExecTimeInSec), DEFAULT_sample_execPlan_minExecTimeInSec, "Only get the execution plan for statements that has been executing for at least this number of seconds." ));

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
					sql = sqlCdb;
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
	 * The names "SqlText", "ExecPlan", "HasOpenTxn" (and STATUS, USERNAME) are used by DbxCentral (dbxcentral.graph.js)
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
			+ "    ,s.LAST_CALL_ET                                                                as \"ExecTimeInSec\" \n"
			+ "    ,(SYSDATE - s.LAST_CALL_ET / 86400)                                            as \"LastCallTime\" \n"
			+ "    ,s.WAIT_CLASS \n"
			+ "    ,s.EVENT \n"
			+ "    ,s.STATE \n"
			+ "    ,TRUNC(s.WAIT_TIME_MICRO / 1000)                                               as \"WaitTimeMs\" \n"
			+ "    ,s.BLOCKING_SESSION                                                            as \"BlockedBySid\" \n"
			+ "    ,case when s.BLOCKING_SESSION is not null then TRUNC(s.WAIT_TIME_MICRO / 1000000) else 0 end as \"BlockedSeconds\" \n"
			+ "    ,CAST(NULL as VARCHAR2(512))                                                   as \"BlockingOtherSids\" \n"
			+ "    ,CAST(0    as NUMBER(10))                                                      as \"BlockingOthersMaxSec\" \n"
			+ "    ,case when s.BLOCKING_SESSION is not null \n"
			+ "          then (select CAST(max(o.OWNER || '.' || o.OBJECT_NAME) as VARCHAR2(300)) from " + rowWaitObjectFromWhere + ") \n"
			+ "     end                                                                           as \"RowWaitObject\" \n"
			+ "    ,s.SQL_ID \n"
			+ "    ,s.PREV_SQL_ID \n"
			+ "    ,COALESCE(s.SQL_ID, s.PREV_SQL_ID)                                             as \"EffectiveSqlId\" \n"
			+ "    ,s.SQL_CHILD_NUMBER \n"
			+ "    ,s.SQL_EXEC_START \n"
			+ "    ,CAST(case when s.SQL_ID is null then 'Y' else 'N' end as VARCHAR2(1))         as \"IsPrevSql\" \n"
			+ "    ,q.CHILD_NUMBER                                                                as \"EffectiveChildNumber\" \n"
			+ "    ,q.PLAN_HASH_VALUE \n"
			+ "    ,q.SQL_FULLTEXT                                                                as \"SqlText\" \n"
			+ "    ,CAST(case when t.ADDR is not null then 'Y' else 'N' end as VARCHAR2(1))       as \"HasOpenTxn\" \n"
			+ "    ,t.START_DATE                                                                  as \"TxnStartTime\" \n"
			+ "    ,CAST(86400 * (SYSDATE - t.START_DATE) as INT)                                 as \"TxnSeconds\" \n"
			+ "    ,t.USED_UBLK                                                                   as \"TxnUndoBlocks\" \n"
			+ "    ,t.USED_UREC                                                                   as \"TxnUndoRecords\" \n"
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
	 * Fill in: 'BlockingOtherSids' + 'BlockingOthersMaxSec' (from the other rows), and 'ExecPlan'
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
		// Execution Plan: for statements that has been executing for a while
		// A plan is fetched ONCE (then it is in the cache), and only a few NEW plans in each sample (so we do not slow down the collector)
		//---------------------------------------------------------------
		Configuration conf = Configuration.getCombinedConfiguration();
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

		if (pos_CON_ID == -1 || pos_STATUS == -1 || pos_ExecTimeInSec == -1 || pos_EffectiveSqlId == -1 || pos_EffectiveChildNumber == -1 || pos_PLAN_HASH_VALUE == -1 || pos_ExecPlan == -1)
			return;

		DbxConnection conn = getCounterController() == null ? null : getCounterController().getMonConnection();
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
				newSample.setValueAt(execPlan, rowId, pos_ExecPlan);
		}
	}

	/** When we are disconnected (or the PCS database is rolled over): forget the plans, so they are stored again */
	@Override
	public void reset()
	{
		super.reset();

		_execPlanCache.clear();
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
		setLocalToolTipTextOnTableColumnHeader("ExecTimeInSec",        "<html>LAST_CALL_ET: For an ACTIVE session: seconds since it became active (how long the current call has been executing).<br>For an INACTIVE session: seconds since it became inactive (how long it has waited for the client).</html>");
		setLocalToolTipTextOnTableColumnHeader("LastCallTime",         "<html>The time when the session became ACTIVE (or INACTIVE).<br><b>Formula</b>: SYSDATE - LAST_CALL_ET</html>");
		setLocalToolTipTextOnTableColumnHeader("WAIT_CLASS",           "<html>Class of the wait event the session is waiting for (e.g. User I/O, Application, Network, Idle). If STATE is not WAITING: the LAST thing it waited for.</html>");
		setLocalToolTipTextOnTableColumnHeader("EVENT",                "<html>Wait event the session is waiting for. If STATE is not WAITING: the LAST thing it waited for.</html>");
		setLocalToolTipTextOnTableColumnHeader("WaitTimeMs",           "<html>WAIT_TIME_MICRO / 1000: If STATE is WAITING: milliseconds the session has waited for the current event. Otherwise: how long the last wait took.</html>");
		setLocalToolTipTextOnTableColumnHeader("STATE",                "<html>Wait state: WAITING (the session is waiting for EVENT). Anything else: the session is on CPU.</html>");
		setLocalToolTipTextOnTableColumnHeader("BlockedBySid",         "<html>BLOCKING_SESSION: The SID that blocks this session (it holds a lock that this session waits for). 0 = not blocked.</html>");
		setLocalToolTipTextOnTableColumnHeader("BlockedSeconds",       "<html>How many seconds this session has been blocked. 0 = not blocked.</html>");
		setLocalToolTipTextOnTableColumnHeader("BlockingOtherSids",    "<html>The sessions (SID's) that are blocked by this session. If this session is not blocked itself (BlockedBySid is 0) it is the <b>root cause</b> of the blocking.</html>");
		setLocalToolTipTextOnTableColumnHeader("BlockingOthersMaxSec", "<html>How many seconds the session that has waited the longest for this session, has been blocked.</html>");
		setLocalToolTipTextOnTableColumnHeader("RowWaitObject",        "<html>If the session is blocked: the object (owner.name) it waits for a lock on.<br><b>Formula</b>: ROW_WAIT_OBJ# translated with DBA_OBJECTS</html>");
		setLocalToolTipTextOnTableColumnHeader("SQL_ID",               "<html>SQL identifier of the SQL statement currently being executed. NULL if the session is not executing anything.</html>");
		setLocalToolTipTextOnTableColumnHeader("PREV_SQL_ID",          "<html>SQL identifier of the last SQL statement executed. Useful when SQL_ID is NULL (idle in transaction).</html>");
		setLocalToolTipTextOnTableColumnHeader("EffectiveSqlId",       "<html>COALESCE(SQL_ID, PREV_SQL_ID): The SQL shown in SqlText: the current if it is executing, otherwise the previous.</html>");
		setLocalToolTipTextOnTableColumnHeader("SQL_CHILD_NUMBER",     "<html>Child number of the SQL cursor. Combined with SQL_ID it identifies a cursor (and its execution plan) in the library cache.</html>");
		setLocalToolTipTextOnTableColumnHeader("SQL_EXEC_START",       "<html>Timestamp when execution of the current SQL statement began.</html>");
		setLocalToolTipTextOnTableColumnHeader("IsPrevSql",            "<html>Y if SQL_ID is NULL and we are showing PREV_SQL_ID instead (the session is not executing anything, but has an open transaction).</html>");
		setLocalToolTipTextOnTableColumnHeader("EffectiveChildNumber", "<html>The child number of the SQL shown in SqlText (and ExecPlan).</html>");
		setLocalToolTipTextOnTableColumnHeader("PLAN_HASH_VALUE",      "<html>Identifies the execution plan. If it changes for a SQL_ID, the statement has got a new execution plan.</html>");
		setLocalToolTipTextOnTableColumnHeader("SqlText",              "<html>The SQL text (SQL_FULLTEXT from V$SQL).</html>");
		setLocalToolTipTextOnTableColumnHeader("HasOpenTxn",           "<html>Y if the session has an open transaction (it has an entry in V$TRANSACTION).</html>");
		setLocalToolTipTextOnTableColumnHeader("TxnStartTime",         "<html>START_DATE: Time when the transaction started.</html>");
		setLocalToolTipTextOnTableColumnHeader("TxnSeconds",           "<html>How many seconds the transaction has been open.</html>");
		setLocalToolTipTextOnTableColumnHeader("TxnUndoBlocks",        "<html>USED_UBLK: Number of undo blocks used by the transaction (how much it has changed).</html>");
		setLocalToolTipTextOnTableColumnHeader("TxnUndoRecords",       "<html>USED_UREC: Number of undo records used by the transaction (roughly: how many rows it has changed).</html>");
		setLocalToolTipTextOnTableColumnHeader("ExecPlan",             "<html>Execution plan from DBMS_XPLAN.DISPLAY_CURSOR. Only for statements that has been executing for some seconds (see the options).</html>");
	}
}
