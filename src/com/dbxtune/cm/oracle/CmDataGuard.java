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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmDescriptionProviderBase;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.oracle.AlarmEventOraDataGuard;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSettingsHelper.RegExpInputValidator;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoOracle;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

/**
 * How far behind the standby databases are (Data Guard).
 * <p>
 * <b>NOT TESTED AGAINST A REAL STANDBY</b>: Data Guard needs Enterprise Edition, and there was none to test on.<br>
 * What has been tested: the SQL runs on 11.2, 18c, 21c and 23 (without a standby it gives no rows), and the calculations and the alarms with made up rows.<br>
 * What has NOT been tested: that a real Data Guard fills V$ARCHIVE_DEST_STATUS, V$ARCHIVED_LOG and V$DATAGUARD_STATS the way the reference manual is read here.<br>
 * So the alarms are OFF until someone turns them on (<code>CmDataGuard.alarm.system.untested.enabled=true</code>).<br>
 * The read-only script <code>DbxTuneStarter/DbxInstaller/DbmsSetup/oracle_dataguard_probe.sql</code> prints what this CM reads: run it on a real primary to check.
 *
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmDataGuard
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmDataGuard.class.getSimpleName();
	public static final String   SHORT_NAME       = "Data Guard";
	public static final String   HTML_DESC        =
		"<html>" +
		"How far behind the standby databases are. One row per standby that this database sends redo to.<br>" +
		"<br>" +
		"<font color='red'><b>Not tested against a real standby database</b></font> (there was no Enterprise Edition to test on). The SQL and the calculations are tested, but with made up rows.<br>" +
		"So <b>the alarms of this tab are off</b>. To turn them on: set <code>CmDataGuard.alarm.system.untested.enabled=true</code>, after you have seen that the numbers here are right for your standby.<br>" +
		"<br>" +
		"<b>How it is counted</b> (on the primary, in whole log files):" +
		"<ul>" +
		"  <li><code>PRIMARY_SEQ</code> - the last log sequence that is complete on the primary.</li>" +
		"  <li><code>SHIPPED_SEQ</code>, <code>APPLIED_SEQ</code> - the last one that the standby has received, and has applied.</li>" +
		"  <li><code>TRANSPORT_SEQ_GAP</code>, <code>APPLY_SEQ_GAP</code> - how many complete logs it has not received, and not applied. 0 = in step.</li>" +
		"  <li><code>TRANSPORT_LAG_SEC</code>, <code>APPLY_LAG_SEC</code> - how old the newest redo is that it has received, and has applied. 0 when the gap is 0.</li>" +
		"</ul>" +
		"A standby with real time apply is somewhere inside the log that is being written, so it is shown as 0. The time is exact to one log file, not to the second.<br>" +
		"When the collector is connected to a standby that is open (Active Data Guard), there is one row with Oracle's own numbers from <code>V$DATAGUARD_STATS</code>.<br>" +
		"<br>" +
		"<b>Note</b>: A standby with <code>DELAY_MINS</code> set is behind in apply on purpose.<br>" +
		"<b>Note</b>: The settings (protection mode, standby redo logs, parameters) are in: DBMS Configuration, tab 'Data Guard'.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "V$ARCHIVE_DEST", "V$ARCHIVE_DEST_STATUS", "V$ARCHIVED_LOG", "V$DATAGUARD_STATS"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 30;

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

		return new CmDataGuard(counterController, guiController);
	}

	public CmDataGuard(ICounterController counterController, IGuiController guiController)
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
	/** Are we connected to a standby database? Set when we connect (in checkDependsOnOther) */
	private boolean _isStandby = false;

	public static final String GRAPH_NAME_LAG_SEC = "DgLagSec";
	public static final String GRAPH_NAME_SEQ_GAP = "DgSeqGap";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_LAG_SEC,
			"Data Guard, Standby Lag in Seconds", 	                // Menu CheckBox text
			"Data Guard, Transport and Apply Lag in Seconds, per Standby (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_SECONDS, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OTHER,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_SEQ_GAP,
			"Data Guard, Log Sequences Behind", 	                // Menu CheckBox text
			"Data Guard, Log Sequences not Received and not Applied, per Standby (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OTHER,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("DEST_ID");

		return pkCols;
	}

	/**
	 * The views must be readable, and there must be a standby to look at: otherwise disable the CM, and say why
	 */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		if ( ! OracleCmUtils.checkViewsAreReadable(this, conn, "V$DATABASE", "V$ARCHIVE_DEST", "V$ARCHIVE_DEST_STATUS", "V$ARCHIVED_LOG", "V$LOG", "V$DATAGUARD_STATS") )
			return false;

		// PRIMARY, PHYSICAL STANDBY, LOGICAL STANDBY, SNAPSHOT STANDBY
		DbmsVersionInfoOracle oraVersionInfo = (DbmsVersionInfoOracle) conn.getDbmsVersionInfo();
		_isStandby = (oraVersionInfo.getDatabaseRole() + "").contains("STANDBY");

		// On a standby we always have something to show. On a primary only if it sends redo to a standby
		if ( ! _isStandby )
		{
			int standbyDestCount = 0;
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery("select count(*) from V$ARCHIVE_DEST where TARGET = 'STANDBY' and STATUS <> 'INACTIVE'"))
			{
				while (rs.next())
					standbyDestCount = rs.getInt(1);
			}
			catch (SQLException ex)
			{
				_logger.warn(CM_NAME + ": Problems counting the standby destinations, continuing as if there are none. Caught: " + ex.getMessage().trim());
			}

			if (standbyDestCount == 0)
			{
				String msg = "This database does not send redo to a standby database (no archive destination with TARGET=STANDBY is in use), so there is no Data Guard to show.";

				_logger.info("When trying to initialize Counters Model '" + getName() + "', named '" + getDisplayName() + "': " + msg);

				setActive(false, msg);

				TabularCntrPanel tcp = getTabPanel();
				if (tcp != null)
					tcp.setToolTipText(msg);

				return false;
			}
		}

		return true;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// conn is null when we just want to "view" the SQL (the CM Properties dialog): then show the SQL for a primary
		return (conn != null && _isStandby) ? getSqlForStandby() : getSqlForPrimary();
	}

	/**
	 * On a PRIMARY: one row per archive destination that is a standby database.
	 * <p>
	 * Everything is counted in whole log files, for the redo thread of the last log that was shipped (on RAC: only that thread):
	 * <ul>
	 *   <li>x: the destination, and what V$ARCHIVE_DEST_STATUS says the standby has received (ARCHIVED_SEQ#) and applied (APPLIED_SEQ#). 0 there means "not known", and becomes NULL</li>
	 *   <li>y: adds the last COMPLETE log on the primary (the one before the CURRENT one), and the time when the shipped and the applied log ENDED (NEXT_TIME in V$ARCHIVED_LOG)</li>
	 *   <li>the gaps are "complete logs on the primary - what the standby has", and the lags are "now - the time when the log it has ended" (0 when there is no gap)</li>
	 * </ul>
	 * The columns are the same from 11.2
	 * <p>
	 * Note: the CAST(... as NUMBER(18)) is needed for the columns that can be NULL (= not known): a NUMBER without precision is read as a long, and then NULL becomes 0
	 */
	private static String getSqlForPrimary()
	{
		return ""
			+ "select \n"
			+ "     CAST('PRIMARY' as VARCHAR2(8))                                                   as SEEN_FROM \n"
			+ "    ,y.DEST_ID \n"
			+ "    ,y.DEST_NAME \n"
			+ "    ,y.DB_UNIQUE_NAME \n"
			+ "    ,y.STATUS \n"
			+ "    ,y.GAP_STATUS \n"
			+ "    ,CAST(y.PRIMARY_SEQ as NUMBER(18))                                                    as PRIMARY_SEQ \n"
			+ "    ,CAST(y.SHIPPED_SEQ as NUMBER(18))                                                    as SHIPPED_SEQ \n"
			+ "    ,CAST(y.APPLIED_SEQ as NUMBER(18))                                                    as APPLIED_SEQ \n"
			+ "    ,CAST(GREATEST(y.PRIMARY_SEQ - y.SHIPPED_SEQ, 0) as NUMBER(18))                       as TRANSPORT_SEQ_GAP \n"
			+ "    ,CAST(GREATEST(y.PRIMARY_SEQ - y.APPLIED_SEQ, 0) as NUMBER(18))                       as APPLY_SEQ_GAP \n"
			+ "    ,CAST(case when y.SHIPPED_SEQ is null                then NULL \n"
			+ "               when y.PRIMARY_SEQ - y.SHIPPED_SEQ <= 0   then 0 \n"
			+ "               else (SYSDATE - y.SHIPPED_REDO_TIME) * 86400 end as NUMBER(18))            as TRANSPORT_LAG_SEC \n"
			+ "    ,CAST(case when y.APPLIED_SEQ is null                then NULL \n"
			+ "               when y.PRIMARY_SEQ - y.APPLIED_SEQ <= 0   then 0 \n"
			+ "               else (SYSDATE - y.APPLIED_REDO_TIME) * 86400 end as NUMBER(18))            as APPLY_LAG_SEC \n"
			+ "    ,y.THREAD_NUM \n"
			+ "    ,y.STANDBY_TYPE \n"
			+ "    ,y.DATABASE_MODE \n"
			+ "    ,y.RECOVERY_MODE \n"
			+ "    ,y.PROTECTION_MODE \n"
			+ "    ,y.SYNCHRONIZATION_STATUS \n"
			+ "    ,y.TRANSMIT_MODE \n"
			+ "    ,y.DELAY_MINS \n"
			+ "    ,y.DESTINATION \n"
			+ "    ,y.ERROR \n"
			+ "from (select x.* \n"
			+ "            ,(select max(l.SEQUENCE#) - 1 from V$LOG l          where l.THREAD# = x.THREAD_NUM and l.STATUS = 'CURRENT')                                                                   as PRIMARY_SEQ \n"
			+ "            ,(select max(a.NEXT_TIME)     from V$ARCHIVED_LOG a where a.THREAD# = x.THREAD_NUM and a.SEQUENCE# = x.SHIPPED_SEQ and a.RESETLOGS_CHANGE# = x.RESETLOGS_CHANGE_NUM)           as SHIPPED_REDO_TIME \n"
			+ "            ,(select max(a.NEXT_TIME)     from V$ARCHIVED_LOG a where a.THREAD# = x.THREAD_NUM and a.SEQUENCE# = x.APPLIED_SEQ and a.RESETLOGS_CHANGE# = x.RESETLOGS_CHANGE_NUM)           as APPLIED_REDO_TIME \n"
			+ "      from (select d.DEST_ID \n"
			+ "                  ,d.DEST_NAME \n"
			+ "                  ,d.DB_UNIQUE_NAME \n"
			+ "                  ,d.STATUS \n"
			+ "                  ,s.GAP_STATUS \n"
			+ "                  ,NVL(NULLIF(s.ARCHIVED_THREAD#, 0), 1)                 as THREAD_NUM \n"
			+ "                  ,NULLIF(s.ARCHIVED_SEQ#, 0)                            as SHIPPED_SEQ \n"
			+ "                  ,NULLIF(s.APPLIED_SEQ#,  0)                            as APPLIED_SEQ \n"
			+ "                  ,(select db.RESETLOGS_CHANGE# from V$DATABASE db)      as RESETLOGS_CHANGE_NUM \n"
			+ "                  ,s.TYPE                                                as STANDBY_TYPE \n"
			+ "                  ,s.DATABASE_MODE \n"
			+ "                  ,s.RECOVERY_MODE \n"
			+ "                  ,s.PROTECTION_MODE \n"
			+ "                  ,s.SYNCHRONIZATION_STATUS \n"
			+ "                  ,d.TRANSMIT_MODE \n"
			+ "                  ,d.DELAY_MINS \n"
			+ "                  ,d.DESTINATION \n"
			+ "                  ,d.ERROR \n"
			+ "            from V$ARCHIVE_DEST d \n"
			+ "            left join V$ARCHIVE_DEST_STATUS s on s.DEST_ID = d.DEST_ID \n"
			+ "            where d.TARGET = 'STANDBY' \n"
			+ "              and d.STATUS <> 'INACTIVE' \n"
			+ "           ) x \n"
			+ "     ) y \n"
			+ "order by y.DEST_ID \n"
			+ "";
	}

	/**
	 * On a STANDBY (that is open, otherwise we could not connect): ONE row, with Oracle's own numbers from V$DATAGUARD_STATS.
	 * <p>
	 * The values there are texts like '+00 00:01:30' (days hours:minutes:seconds). If it is something else the lag is NULL (and the SQL does not fail)
	 */
	private static String getSqlForStandby()
	{
		String isInterval = "'^[+-][0-9]+ [0-9]+:[0-9]+:[0-9]+'";

		return ""
			+ "select \n"
			+ "     CAST('STANDBY' as VARCHAR2(8))               as SEEN_FROM \n"
			+ "    ,CAST(0 as NUMBER(10))                         as DEST_ID \n"
			+ "    ,CAST('(this standby)' as VARCHAR2(256))       as DEST_NAME \n"
			+ "    ,CAST(db.DB_UNIQUE_NAME as VARCHAR2(30))       as DB_UNIQUE_NAME \n"
			+ "    ,CAST(NULL as VARCHAR2(9))                     as STATUS \n"
			+ "    ,CAST(NULL as VARCHAR2(24))                    as GAP_STATUS \n"
			+ "    ,CAST(NULL as NUMBER(18))                      as PRIMARY_SEQ \n"
			+ "    ,CAST(NULL as NUMBER(18))                      as SHIPPED_SEQ \n"
			+ "    ,CAST(NULL as NUMBER(18))                      as APPLIED_SEQ \n"
			+ "    ,CAST(NULL as NUMBER(18))                      as TRANSPORT_SEQ_GAP \n"
			+ "    ,CAST(NULL as NUMBER(18))                      as APPLY_SEQ_GAP \n"
			+ "    ,CAST(case when REGEXP_LIKE(g.TRANSPORT_LAG, " + isInterval + ") \n"
			+ "               then extract(day from to_dsinterval(g.TRANSPORT_LAG)) * 86400 + extract(hour from to_dsinterval(g.TRANSPORT_LAG)) * 3600 + extract(minute from to_dsinterval(g.TRANSPORT_LAG)) * 60 + extract(second from to_dsinterval(g.TRANSPORT_LAG)) \n"
			+ "          end as NUMBER(18))                       as TRANSPORT_LAG_SEC \n"
			+ "    ,CAST(case when REGEXP_LIKE(g.APPLY_LAG, " + isInterval + ") \n"
			+ "               then extract(day from to_dsinterval(g.APPLY_LAG)) * 86400 + extract(hour from to_dsinterval(g.APPLY_LAG)) * 3600 + extract(minute from to_dsinterval(g.APPLY_LAG)) * 60 + extract(second from to_dsinterval(g.APPLY_LAG)) \n"
			+ "          end as NUMBER(18))                       as APPLY_LAG_SEC \n"
			+ "    ,CAST(NULL as NUMBER(10))                      as THREAD_NUM \n"
			+ "    ,CAST(db.DATABASE_ROLE as VARCHAR2(16))        as STANDBY_TYPE \n"
			+ "    ,CAST(db.OPEN_MODE as VARCHAR2(20))            as DATABASE_MODE \n"
			+ "    ,CAST(NULL as VARCHAR2(34))                    as RECOVERY_MODE \n"
			+ "    ,CAST(db.PROTECTION_MODE as VARCHAR2(20))      as PROTECTION_MODE \n"
			+ "    ,CAST(NULL as VARCHAR2(22))                    as SYNCHRONIZATION_STATUS \n"
			+ "    ,CAST(NULL as VARCHAR2(12))                    as TRANSMIT_MODE \n"
			+ "    ,CAST(NULL as NUMBER(10))                      as DELAY_MINS \n"
			+ "    ,CAST(NULL as VARCHAR2(256))                   as DESTINATION \n"
			+ "    ,CAST(NULL as VARCHAR2(256))                   as ERROR \n"
			+ "from V$DATABASE db \n"
			+ "cross join (select (select VALUE from V$DATAGUARD_STATS where NAME = 'transport lag' and ROWNUM = 1) as TRANSPORT_LAG \n"
			+ "                  ,(select VALUE from V$DATAGUARD_STATS where NAME = 'apply lag'     and ROWNUM = 1) as APPLY_LAG \n"
			+ "            from dual) g \n"
			+ "";
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "How far behind the standby databases are (Data Guard). NOT tested against a real standby database.");

			mtd.addColumn(CM_NAME, "SEEN_FROM",              "<html><b>PRIMARY</b> = the row is what the primary knows about a standby (V$ARCHIVE_DEST, V$ARCHIVE_DEST_STATUS, V$ARCHIVED_LOG).<br><b>STANDBY</b> = we are connected to a standby, and the row is Oracle's own numbers (V$DATAGUARD_STATS).</html>");
			mtd.addColumn(CM_NAME, "DEST_ID",                "<html>Destination number (1-31): the parameter <code>log_archive_dest_<i>n</i></code>. 0 for the row of a standby that we are connected to.</html>");
			mtd.addColumn(CM_NAME, "DEST_NAME",              "<html>Name of the destination: LOG_ARCHIVE_DEST_n.</html>");
			mtd.addColumn(CM_NAME, "DB_UNIQUE_NAME",         "<html>The DB_UNIQUE_NAME of the standby database (from the parameter of the destination).</html>");
			mtd.addColumn(CM_NAME, "STATUS",                 "<html>State of the destination: VALID (it works), ERROR, DISABLED, DEFERRED (switched off by a DBA), FULL, BAD PARAM... See also the tab 'Archive Dest'.</html>");
			mtd.addColumn(CM_NAME, "GAP_STATUS",             "<html>Is any redo missing at the standby: NO GAP (good), RESOLVABLE GAP (it is fetching what is missing), UNRESOLVABLE GAP / LOCALLY UNRESOLVABLE GAP (it can't get it by itself)...</html>");
			mtd.addColumn(CM_NAME, "PRIMARY_SEQ",            "<html>The last log sequence that is complete on the primary: the one before the log that is being written.</html>");
			mtd.addColumn(CM_NAME, "SHIPPED_SEQ",            "<html>The last log sequence that the standby has received. Empty = the primary does not know.<br><b>Formula</b>: ARCHIVED_SEQ# in V$ARCHIVE_DEST_STATUS</html>");
			mtd.addColumn(CM_NAME, "APPLIED_SEQ",            "<html>The last log sequence that the standby has applied. Empty = the primary does not know.<br><b>Formula</b>: APPLIED_SEQ# in V$ARCHIVE_DEST_STATUS</html>");
			mtd.addColumn(CM_NAME, "TRANSPORT_SEQ_GAP",      "<html>Number of complete logs that the standby has NOT received. 0 = it has all of them.<br><b>Formula</b>: PRIMARY_SEQ - SHIPPED_SEQ</html>");
			mtd.addColumn(CM_NAME, "APPLY_SEQ_GAP",          "<html>Number of complete logs that the standby has NOT applied. 0 = it has applied all of them (it can still be in the middle of the current one).<br><b>Formula</b>: PRIMARY_SEQ - APPLIED_SEQ</html>");
			mtd.addColumn(CM_NAME, "TRANSPORT_LAG_SEC",      "<html>How old the newest redo is that the standby has received, in seconds. This is what is lost if the primary is lost now.<br>0 when TRANSPORT_SEQ_GAP is 0. Otherwise: now - the time when the last received log ended. Exact to one log file.<br>For a row seen from a STANDBY: 'transport lag' in V$DATAGUARD_STATS.</html>");
			mtd.addColumn(CM_NAME, "APPLY_LAG_SEC",          "<html>How old the newest redo is that the standby has applied, in seconds. This is how much it has to catch up with at a failover.<br>0 when APPLY_SEQ_GAP is 0. Otherwise: now - the time when the last applied log ended. Exact to one log file.<br>For a row seen from a STANDBY: 'apply lag' in V$DATAGUARD_STATS.</html>");
			mtd.addColumn(CM_NAME, "THREAD_NUM",             "<html>The redo thread (instance) that the sequence numbers are for. On RAC: the thread of the last log that was shipped, the other threads are not shown.</html>");
			mtd.addColumn(CM_NAME, "STANDBY_TYPE",           "<html>PHYSICAL, LOGICAL, SNAPSHOT... (for a row seen from a STANDBY: the role of the database)</html>");
			mtd.addColumn(CM_NAME, "DATABASE_MODE",          "<html>The state of the standby database: MOUNTED-STANDBY, OPEN_READ-ONLY... as far as the primary knows.</html>");
			mtd.addColumn(CM_NAME, "RECOVERY_MODE",          "<html>What the apply does on the standby: IDLE (it is not applying), MANAGED, MANAGED REAL TIME APPLY, LOGICAL REAL TIME APPLY...</html>");
			mtd.addColumn(CM_NAME, "PROTECTION_MODE",        "<html>MAXIMUM PERFORMANCE, MAXIMUM AVAILABILITY or MAXIMUM PROTECTION.</html>");
			mtd.addColumn(CM_NAME, "SYNCHRONIZATION_STATUS", "<html>For a synchronous destination: OK, or what is wrong (CHECK NETWORK, CHECK STANDBY REDO LOG, DESTINATION HAS A GAP...).</html>");
			mtd.addColumn(CM_NAME, "TRANSMIT_MODE",          "<html>SYNCHRONOUS (a commit waits for the standby), PARALLELSYNC or ASYNCHRONOUS.</html>");
			mtd.addColumn(CM_NAME, "DELAY_MINS",             "<html>Minutes that the standby waits before it applies a log, on purpose (the DELAY attribute). With this set the apply lag is expected.</html>");
			mtd.addColumn(CM_NAME, "DESTINATION",            "<html>The service name that the redo is sent to.</html>");
			mtd.addColumn(CM_NAME, "ERROR",                  "<html>The error message of the destination, if any.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	/** The name of a standby: DB_UNIQUE_NAME if the destination has one, otherwise the destination name (LOG_ARCHIVE_DEST_n). Used as: graph label, and to identify an alarm */
	private static String getStandbyLabel(CountersModel cm, int rowId)
	{
		String dbUniqueName = cm.getAbsString(rowId, "DB_UNIQUE_NAME");
		String destName     = cm.getAbsString(rowId, "DEST_NAME");

		return (StringUtil.hasValue(dbUniqueName) && ! "NONE".equals(dbUniqueName)) ? dbUniqueName : destName;
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		// The two graphs are the same: two "lines" for every standby: transport and apply (a value that is not known is shown as 0)
		String transportCol = null;
		String applyCol     = null;
		if (GRAPH_NAME_LAG_SEC.equals(tgdp.getName())) { transportCol = "TRANSPORT_LAG_SEC"; applyCol = "APPLY_LAG_SEC"; }
		if (GRAPH_NAME_SEQ_GAP.equals(tgdp.getName())) { transportCol = "TRANSPORT_SEQ_GAP"; applyCol = "APPLY_SEQ_GAP"; }

		if (transportCol != null)
		{
			LinkedHashMap<String, Double> dataMap = new LinkedHashMap<>();

			for (int r = 0; r < this.size(); r++)
			{
				String standbyLabel = getStandbyLabel(this, r);
				Double transport    = this.getAbsValueAsDouble(r, transportCol);
				Double apply        = this.getAbsValueAsDouble(r, applyCol);

				dataMap.put(standbyLabel + ": transport", transport == null ? 0.0 : transport);
				dataMap.put(standbyLabel + ": apply",     apply     == null ? 0.0 : apply);
			}

			if (dataMap.size() > 0)
				tgdp.setData(this.getTimestamp(), dataMap);
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

		// NOT tested against a real standby: so no alarms until someone has looked at the numbers, and turned them on
		if ( ! conf.getBooleanProperty(PROPKEY_alarm_untestedEnabled, DEFAULT_alarm_untestedEnabled) )
			return;

		for (int r=0; r<cm.getAbsRowCount(); r++)
		{
			String standbyLabel   = getStandbyLabel(cm, r);
			String status         = cm.getAbsString(r, "STATUS");
			String gapStatus      = cm.getAbsString(r, "GAP_STATUS");
			Double transportLag   = cm.getAbsValueAsDouble(r, "TRANSPORT_LAG_SEC");
			Double applyLag       = cm.getAbsValueAsDouble(r, "APPLY_LAG_SEC");
			Double transportGap   = cm.getAbsValueAsDouble(r, "TRANSPORT_SEQ_GAP");
			Double applyGap       = cm.getAbsValueAsDouble(r, "APPLY_SEQ_GAP");

			// The four alarms below gets the same extra text: the row as a table, and at CANCEL what the row says now. So ONE provider for all of them
			// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
			AlarmDescriptionProviderBase descriptionProvider = new AlarmDescriptionProviderBase(cm.getPk(), cm.getAbsPkValue(r), null)
			{
				@Override
				public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
				{
					int    rowId = cm.getAbsRowIdForPkValue(getRowPk());
					String label = "Standby '" + standbyLabel + "'";

					if (rowId != -1)
					{
						// Always: (RAISE, RE-RAISE, CANCEL) - Set the row as a table
						String extendedDescText = cm.toTextTableString(DATA_ABS, rowId);
						String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, rowId, true, false, false);
						extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_LAG_SEC, standbyLabel + ": transport", standbyLabel + ": apply");
						alarmEvent.setExtendedDescription(extendedDescText, extendedDescHtml);

						// Set CANCEL message (values that was found AFTER last raise/re-raise event)
						if (AlarmPhase.CANCEL.equals(phase))
							alarmEvent.setCancelDescription(getAlarmCancelText(label + ": STATUS", "'" + cm.getAbsString(rowId, "STATUS") + "'"
									+ ", GAP_STATUS='"       + cm.getAbsString(rowId, "GAP_STATUS") + "'"
									+ ", transport lag "     + cm.getAbsString(rowId, "TRANSPORT_LAG_SEC") + " s (" + cm.getAbsString(rowId, "TRANSPORT_SEQ_GAP") + " logs)"
									+ ", apply lag "         + cm.getAbsString(rowId, "APPLY_LAG_SEC")     + " s (" + cm.getAbsString(rowId, "APPLY_SEQ_GAP")     + " logs)"));
					}
					else
					{
						// The row is gone: the destination is no longer in use
						if (AlarmPhase.CANCEL.equals(phase))
							alarmEvent.setCancelDescription(getAlarmCancelTextGone(label));
					}
				}
			};

			//-------------------------------------------------------
			// ApplyLagInSec: the standby has not applied redo that is older than ## seconds
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("ApplyLagInSec"))
			{
				double threshold = conf.getDoubleProperty(PROPKEY_alarm_ApplyLagInSec, DEFAULT_alarm_ApplyLagInSec);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): ApplyLagInSec: standby='" + standbyLabel + "', threshold=" + threshold + ", APPLY_LAG_SEC='" + applyLag + "', APPLY_SEQ_GAP='" + applyGap + "'.");

				if (applyLag != null && applyLag > threshold)
				{
					AlarmEvent ae = new AlarmEventOraDataGuard(cm, "ApplyLagInSec", standbyLabel, applyLag, applyGap, threshold);
					ae.setAlarmDescriptionProvider(this, descriptionProvider);

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "ApplyLagInSec");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			}

			//-------------------------------------------------------
			// TransportLagInSec: the standby has not received redo that is older than ## seconds
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("TransportLagInSec"))
			{
				double threshold = conf.getDoubleProperty(PROPKEY_alarm_TransportLagInSec, DEFAULT_alarm_TransportLagInSec);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): TransportLagInSec: standby='" + standbyLabel + "', threshold=" + threshold + ", TRANSPORT_LAG_SEC='" + transportLag + "', TRANSPORT_SEQ_GAP='" + transportGap + "'.");

				if (transportLag != null && transportLag > threshold)
				{
					AlarmEvent ae = new AlarmEventOraDataGuard(cm, "TransportLagInSec", standbyLabel, transportLag, transportGap, threshold);
					ae.setAlarmDescriptionProvider(this, descriptionProvider);

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "TransportLagInSec");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			}

			//-------------------------------------------------------
			// GapSequences: the standby is more than ## log sequences behind (in what it has received, or in what it has applied)
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("GapSequences"))
			{
				int  threshold = conf.getIntProperty(PROPKEY_alarm_GapSequences, DEFAULT_alarm_GapSequences);
				long seqGap    = Math.max(transportGap == null ? 0 : transportGap.longValue(), applyGap == null ? 0 : applyGap.longValue());

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): GapSequences: standby='" + standbyLabel + "', threshold=" + threshold + ", seqGap=" + seqGap + ", TRANSPORT_SEQ_GAP='" + transportGap + "', APPLY_SEQ_GAP='" + applyGap + "'.");

				if (seqGap > threshold)
				{
					Double primarySeq = cm.getAbsValueAsDouble(r, "PRIMARY_SEQ");

					AlarmEvent ae = new AlarmEventOraDataGuard(cm, standbyLabel, (primarySeq == null ? 0 : primarySeq.longValue()), cm.getAbsValueAsDouble(r, "SHIPPED_SEQ"), cm.getAbsValueAsDouble(r, "APPLIED_SEQ"), seqGap, threshold);
					ae.setAlarmDescriptionProvider(this, descriptionProvider);

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "GapSequences");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			}

			//-------------------------------------------------------
			// StandbyDestError: no redo is sent to the standby, or it has a gap that it can't fill by itself
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("StandbyDestError"))
			{
				String statusRegex = conf.getProperty(PROPKEY_alarm_StandbyDestErrorStatus,    DEFAULT_alarm_StandbyDestErrorStatus);
				String gapRegex    = conf.getProperty(PROPKEY_alarm_StandbyDestErrorGapStatus, DEFAULT_alarm_StandbyDestErrorGapStatus);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): StandbyDestError: standby='" + standbyLabel + "', statusRegex='" + statusRegex + "', gapRegex='" + gapRegex + "', STATUS='" + status + "', GAP_STATUS='" + gapStatus + "'.");

				boolean isBadStatus = StringUtil.hasValue(statusRegex) && StringUtil.hasValue(status)    && status   .matches(statusRegex);
				boolean isBadGap    = StringUtil.hasValue(gapRegex)    && StringUtil.hasValue(gapStatus) && gapStatus.matches(gapRegex);

				if (isBadStatus || isBadGap)
				{
					AlarmEvent ae = new AlarmEventOraDataGuard(cm, standbyLabel, status, gapStatus, cm.getAbsString(r, "ERROR"), statusRegex, gapRegex);
					ae.setAlarmDescriptionProvider(this, descriptionProvider);

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "StandbyDestError");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			}

		} // end: loop-rows
	}

	@Override
	public boolean isGraphDataHistoryEnabled(String name)
	{
		// ENABLED for the following graphs
		if (GRAPH_NAME_LAG_SEC.equals(name)) return true;

		// default: DISABLED
		return false;
	}

	// The alarms of this CM has NOT been tested against a real standby database (only with made up rows): so they are OFF until this is set to true
	public static final String  PROPKEY_alarm_untestedEnabled            = CM_NAME + ".alarm.system.untested.enabled";
	public static final boolean DEFAULT_alarm_untestedEnabled            = false;

	public static final String  PROPKEY_alarm_ApplyLagInSec              = CM_NAME + ".alarm.system.if.ApplyLagInSec.gt";
	public static final double  DEFAULT_alarm_ApplyLagInSec              = 900;

	public static final String  PROPKEY_alarm_TransportLagInSec          = CM_NAME + ".alarm.system.if.TransportLagInSec.gt";
	public static final double  DEFAULT_alarm_TransportLagInSec          = 300;

	public static final String  PROPKEY_alarm_GapSequences               = CM_NAME + ".alarm.system.if.GapSequences.gt";
	public static final int     DEFAULT_alarm_GapSequences               = 5;

	public static final String  PROPKEY_alarm_StandbyDestErrorStatus     = CM_NAME + ".alarm.system.if.Status.in";
	public static final String  DEFAULT_alarm_StandbyDestErrorStatus     = "(ERROR|DISABLED|FULL|BAD PARAM)";

	public static final String  PROPKEY_alarm_StandbyDestErrorGapStatus  = CM_NAME + ".alarm.system.if.GapStatus.in";
	public static final String  DEFAULT_alarm_StandbyDestErrorGapStatus  = "(.*UNRESOLVABLE.*)";

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		CmSettingsHelper.Type isPreCheck    = CmSettingsHelper.Type.IS_PRE_CHECK;

		list.add(new CmSettingsHelper("UntestedEnabled"           , isPreCheck   , PROPKEY_alarm_untestedEnabled          , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_untestedEnabled          , DEFAULT_alarm_untestedEnabled          ), DEFAULT_alarm_untestedEnabled          , "The alarms below has NOT been tested against a real standby database (only with made up rows). They are all OFF until this is set to true. Look at the numbers in the tab first: are they right for your standby?" ));

		list.add(new CmSettingsHelper("ApplyLagInSec"             , isAlarmSwitch, PROPKEY_alarm_ApplyLagInSec            , Double .class, conf.getDoubleProperty (PROPKEY_alarm_ApplyLagInSec            , DEFAULT_alarm_ApplyLagInSec            ), DEFAULT_alarm_ApplyLagInSec            , "If 'APPLY_LAG_SEC' is greater than ## seconds then send 'AlarmEventOraDataGuard'." ));
		list.add(new CmSettingsHelper("TransportLagInSec"         , isAlarmSwitch, PROPKEY_alarm_TransportLagInSec        , Double .class, conf.getDoubleProperty (PROPKEY_alarm_TransportLagInSec        , DEFAULT_alarm_TransportLagInSec        ), DEFAULT_alarm_TransportLagInSec        , "If 'TRANSPORT_LAG_SEC' is greater than ## seconds then send 'AlarmEventOraDataGuard'." ));
		list.add(new CmSettingsHelper("GapSequences"              , isAlarmSwitch, PROPKEY_alarm_GapSequences             , Integer.class, conf.getIntProperty    (PROPKEY_alarm_GapSequences             , DEFAULT_alarm_GapSequences             ), DEFAULT_alarm_GapSequences             , "If 'TRANSPORT_SEQ_GAP' or 'APPLY_SEQ_GAP' is greater than ## log sequences then send 'AlarmEventOraDataGuard'." ));
		list.add(new CmSettingsHelper("StandbyDestError"          , isAlarmSwitch, PROPKEY_alarm_StandbyDestErrorStatus   , String .class, conf.getProperty       (PROPKEY_alarm_StandbyDestErrorStatus   , DEFAULT_alarm_StandbyDestErrorStatus   ), DEFAULT_alarm_StandbyDestErrorStatus   , "If 'STATUS' matches this regexp (or 'GAP_STATUS' matches the one below) then send 'AlarmEventOraDataGuard'.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("StandbyDestError GapStatus",                PROPKEY_alarm_StandbyDestErrorGapStatus, String .class, conf.getProperty       (PROPKEY_alarm_StandbyDestErrorGapStatus, DEFAULT_alarm_StandbyDestErrorGapStatus), DEFAULT_alarm_StandbyDestErrorGapStatus, "If 'StandbyDestError' is on; also send the alarm if 'GAP_STATUS' matches this regexp.", new RegExpInputValidator()));

		return list;
	}
}
