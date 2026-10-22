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
import com.dbxtune.alarm.AlarmHelper;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoOracle;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSessions
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSessions.class.getSimpleName();
	public static final String   SHORT_NAME       = "Sessions";
	public static final String   HTML_DESC        =
		"<html>" +
		"One row per session connected to the instance: who it is, what it is doing right now, and how much resources it has used.<br>" +
		"<br>" +
		"<ul>" +
		"   <li><b>TYPE</b>   - USER is a client connection, BACKGROUND is one of Oracle's own processes (DBWR, LGWR...)</li>" +
		"   <li><b>STATUS</b> - ACTIVE is executing something right now, INACTIVE is waiting for the client to send the next statement.</li>" +
		"   <li><b>STATE</b>  - WAITING: the session is waiting for <code>EVENT</code>. Anything else: it is on CPU (and <code>EVENT</code> is the LAST thing it waited for).</li>" +
		"   <li><b>BLOCKING_SESSION</b> - The SID that blocks this session (holds a lock it needs).</li>" +
		"</ul>" +
		"<b>Tip</b>: Sort the DIFF or RATE values on <code>CPU_CS</code> or <code>LOGICAL_READS</code> to find the sessions that uses most resources right now.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "V$SESSION"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"CPU_CS",
		"LOGICAL_READS",
		"PHYSICAL_READS",
		"REDO_SIZE",
		"EXECUTIONS",
		"USER_COMMITS",
		"USER_ROLLBACKS",
		"HARD_PARSES",
		"ROUNDTRIPS"
		};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.MEDIUM; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmSessions(counterController, guiController);
	}

	public CmSessions(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_SESSION_COUNT        = "SessCount";
	public static final String GRAPH_NAME_ACTIVE_BY_WAIT_CLASS = "ActiveByWaitClass";
	public static final String GRAPH_NAME_PGA_MB               = "SessPgaMb";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_SESSION_COUNT,
			"User Sessions: Active, Inactive, Blocked", 	                // Menu CheckBox text
			"Number of User Sessions: Active, Inactive and Blocked (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Active", "Inactive", "Blocked" },
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_ACTIVE_BY_WAIT_CLASS,
			"Active User Sessions: on CPU or Waiting", 	                // Menu CheckBox text
			"Active User Sessions at Sample Time: on CPU, or Waiting per Wait Class (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_PGA_MB,
			"Session Memory (PGA) in MB", 	                // Menu CheckBox text
			"Session Memory (PGA) in MB, for all Sessions (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Used", "Allocated" },
			LabelType.Static,
			TrendGraphDataPoint.Category.MEMORY,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		// SID is unique at any point in time, but it is reused when a session ends. SERIAL# makes it a "new" row (so the DIFF counters start over)
		pkCols.add("SID");
		pkCols.add("SERIAL#");

		return pkCols;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "V$SESSION", "V$PROCESS", "V$SESSTAT", "V$STATNAME");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoOracle oraVersionInfo = (DbmsVersionInfoOracle) versionInfo;

		// 11.2 and above
		String sql = getSql("CAST(0 as NUMBER(10))", "CAST(NULL as VARCHAR2(128))", "");

		// 12.1 and above, in a Container Database: what container is the session in
		if (versionInfo.getLongVersion() >= Ver.ver(12,1) && oraVersionInfo.isCdb())
		{
			String sqlCdb = getSql("s.CON_ID", "c.NAME", "left join V$CONTAINERS c on c.CON_ID = s.CON_ID \n");

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
					_logger.warn(CM_NAME + ": The SQL for a CDB (version " + Ver.versionNumToStr(versionInfo.getLongVersion()) + ") failed, the container name will not be shown. Caught: " + ex.getMessage().trim());
				}
			}
		}

		return sql;
	}

	private String getSql(String CON_ID, String CON_NAME, String joinContainers)
	{
		// Note: CAST(... as NUMBER(12,1)) is needed to keep decimals (a NUMBER without precision is treated as a BIGINT)
		return ""
			+ "select \n"
			+ "     s.SID \n"
			+ "    ,s.SERIAL# \n"
			+ "    ," + CON_ID   + " as CON_ID \n"
			+ "    ," + CON_NAME + " as CON_NAME \n"
			+ "    ,s.USERNAME \n"
			+ "    ,s.SCHEMANAME \n"
			+ "    ,s.STATUS \n"
			+ "    ,s.TYPE \n"
			+ "    ,s.SERVER \n"
			+ "    ,s.OSUSER \n"
			+ "    ,s.MACHINE \n"
			+ "    ,s.PROGRAM \n"
			+ "    ,s.MODULE \n"
			+ "    ,s.ACTION \n"
			+ "    ,s.CLIENT_IDENTIFIER \n"
			+ "    ,s.SERVICE_NAME \n"
			+ "    ,s.LOGON_TIME \n"
			+ "    ,s.LAST_CALL_ET \n"
			+ "    ,CAST(case when s.SID = SYS_CONTEXT('USERENV','SID') then 'Y' else 'N' end as VARCHAR2(1)) as IS_OWN_SESSION \n"
			+ "    ,s.SQL_ID \n"
			+ "    ,s.SQL_CHILD_NUMBER \n"
			+ "    ,s.SQL_EXEC_START \n"
			+ "    ,s.PREV_SQL_ID \n"
			+ "    ,s.BLOCKING_SESSION \n"
			+ "    ,s.FINAL_BLOCKING_SESSION \n"
			+ "    ,s.WAIT_CLASS \n"
			+ "    ,s.EVENT \n"
			+ "    ,s.STATE \n"
			+ "    ,TRUNC(s.WAIT_TIME_MICRO / 1000)                                                           as WAIT_TIME_MS \n"
			+ "    ,CAST(case when s.TADDR is not null then 'Y' else 'N' end as VARCHAR2(1))                  as HAS_OPEN_TRAN \n"
			+ "    ,s.RESOURCE_CONSUMER_GROUP \n"
			+ "    ,p.SPID                                                                                    as OS_PID \n"
			+ "    ,CAST(p.PGA_USED_MEM  / 1048576 as NUMBER(12,1))                                           as PGA_USED_MB \n"
			+ "    ,CAST(p.PGA_ALLOC_MEM / 1048576 as NUMBER(12,1))                                           as PGA_ALLOC_MB \n"
			+ "    ,CAST(p.PGA_MAX_MEM   / 1048576 as NUMBER(12,1))                                           as PGA_MAX_MB \n"
			+ "    ,st.CPU_CS \n"
			+ "    ,st.LOGICAL_READS \n"
			+ "    ,st.PHYSICAL_READS \n"
			+ "    ,st.REDO_SIZE \n"
			+ "    ,st.EXECUTIONS \n"
			+ "    ,st.USER_COMMITS \n"
			+ "    ,st.USER_ROLLBACKS \n"
			+ "    ,st.HARD_PARSES \n"
			+ "    ,st.ROUNDTRIPS \n"
			+ "from V$SESSION s \n"
			+ joinContainers
			+ "left join V$PROCESS p on p.ADDR = s.PADDR \n"

			// Session Statistics: some of the counters, as one row per session. (join on the NAME since STATISTIC# is not the same in all versions)
			// NOTE: decode(..., 0) the "else 0" is needed. Without it the aggregate skips NULL values, and Oracle sends a warning with the result
			//       (JDBC: 17110 'execution completed with warning'), which is treated as a failed sample
			+ "left join (select t.SID \n"
			+ "                 ,max(decode(n.NAME, 'CPU used by this session'         , t.VALUE, 0)) as CPU_CS \n"
			+ "                 ,max(decode(n.NAME, 'session logical reads'            , t.VALUE, 0)) as LOGICAL_READS \n"
			+ "                 ,max(decode(n.NAME, 'physical reads'                   , t.VALUE, 0)) as PHYSICAL_READS \n"
			+ "                 ,max(decode(n.NAME, 'redo size'                        , t.VALUE, 0)) as REDO_SIZE \n"
			+ "                 ,max(decode(n.NAME, 'execute count'                    , t.VALUE, 0)) as EXECUTIONS \n"
			+ "                 ,max(decode(n.NAME, 'user commits'                     , t.VALUE, 0)) as USER_COMMITS \n"
			+ "                 ,max(decode(n.NAME, 'user rollbacks'                   , t.VALUE, 0)) as USER_ROLLBACKS \n"
			+ "                 ,max(decode(n.NAME, 'parse count (hard)'               , t.VALUE, 0)) as HARD_PARSES \n"
			+ "                 ,max(decode(n.NAME, 'SQL*Net roundtrips to/from client', t.VALUE, 0)) as ROUNDTRIPS \n"
			+ "           from V$SESSTAT t \n"
			+ "           join V$STATNAME n on n.STATISTIC# = t.STATISTIC# \n"
			+ "           where n.NAME in ('CPU used by this session', 'session logical reads', 'physical reads', 'redo size', 'execute count', \n"
			+ "                            'user commits', 'user rollbacks', 'parse count (hard)', 'SQL*Net roundtrips to/from client') \n"
			+ "           group by t.SID) st on st.SID = s.SID \n"
			+ "order by s.SID \n"
			+ "";
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "One row per session connected to the instance.");

			mtd.addColumn(CM_NAME, "CON_ID",          "<html>Container ID the session is in.<br>0 = This is not a Container Database (or the version is below 12.1).<br>1 = CDB$ROOT, 3 and above = a Pluggable Database (PDB).</html>");
			mtd.addColumn(CM_NAME, "CON_NAME",        "<html>Name of the Container (PDB) the session is in. NULL if this is not a Container Database.</html>");
			mtd.addColumn(CM_NAME, "IS_OWN_SESSION",  "<html>Y = this is the session that DbxTune uses to collect this information (it is not counted in the graphs).</html>");
			mtd.addColumn(CM_NAME, "WAIT_TIME_MS",    "<html>If STATE is WAITING: how long (in milliseconds) the session has waited for EVENT so far.<br>Otherwise: how long the LAST wait took.<br><b>Formula</b>: WAIT_TIME_MICRO / 1000</html>");
			mtd.addColumn(CM_NAME, "HAS_OPEN_TRAN",   "<html>Y = the session has an open transaction (it has changed data that is not yet committed).</html>");
			mtd.addColumn(CM_NAME, "OS_PID",          "<html>Operating System process id (or thread id) of the server process that serves this session.<br><b>Formula</b>: V$PROCESS.SPID</html>");
			mtd.addColumn(CM_NAME, "PGA_USED_MB",     "<html>Memory (PGA) in MB the server process uses right now.<br><b>Formula</b>: V$PROCESS.PGA_USED_MEM / 1024 / 1024</html>");
			mtd.addColumn(CM_NAME, "PGA_ALLOC_MB",    "<html>Memory (PGA) in MB the server process has allocated right now (including free PGA memory not yet released to the OS).<br><b>Formula</b>: V$PROCESS.PGA_ALLOC_MEM / 1024 / 1024</html>");
			mtd.addColumn(CM_NAME, "PGA_MAX_MB",      "<html>The most memory (PGA) in MB the server process has ever allocated.<br><b>Formula</b>: V$PROCESS.PGA_MAX_MEM / 1024 / 1024</html>");
			mtd.addColumn(CM_NAME, "CPU_CS",          "<html>CPU time used by the session, in centiseconds (1/100 second). RATE 100 = one CPU fully used.<br><b>Note</b>: it is updated when a call ends, so a long running statement shows up when it is finished.<br><b>Formula</b>: V$SESSTAT 'CPU used by this session'</html>");
			mtd.addColumn(CM_NAME, "LOGICAL_READS",   "<html>Number of blocks read from the Buffer Cache.<br><b>Formula</b>: V$SESSTAT 'session logical reads'</html>");
			mtd.addColumn(CM_NAME, "PHYSICAL_READS",  "<html>Number of blocks read from disk.<br><b>Formula</b>: V$SESSTAT 'physical reads'</html>");
			mtd.addColumn(CM_NAME, "REDO_SIZE",       "<html>Bytes of redo generated (how much the session changes data).<br><b>Formula</b>: V$SESSTAT 'redo size'</html>");
			mtd.addColumn(CM_NAME, "EXECUTIONS",      "<html>Number of SQL statements executed.<br><b>Formula</b>: V$SESSTAT 'execute count'</html>");
			mtd.addColumn(CM_NAME, "USER_COMMITS",    "<html>Number of commits.<br><b>Formula</b>: V$SESSTAT 'user commits'</html>");
			mtd.addColumn(CM_NAME, "USER_ROLLBACKS",  "<html>Number of rollbacks.<br><b>Formula</b>: V$SESSTAT 'user rollbacks'</html>");
			mtd.addColumn(CM_NAME, "HARD_PARSES",     "<html>Number of hard parses (a new execution plan had to be created). Many of them: the application probably does not use bind variables.<br><b>Formula</b>: V$SESSTAT 'parse count (hard)'</html>");
			mtd.addColumn(CM_NAME, "ROUNDTRIPS",      "<html>Number of network roundtrips to/from the client.<br><b>Formula</b>: V$SESSTAT 'SQL*Net roundtrips to/from client'</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		//--------------------------------------------------------
		// User sessions: Active, Inactive, Blocked (our own session is not counted)
		//--------------------------------------------------------
		if (GRAPH_NAME_SESSION_COUNT.equals(tgdp.getName()))
		{
			int active   = 0;
			int inactive = 0;
			int blocked  = 0;

			for (int r = 0; r < this.size(); r++)
			{
				if ( ! "USER".equals(this.getAbsString(r, "TYPE")) || "Y".equals(this.getAbsString(r, "IS_OWN_SESSION")) )
					continue;

				if ("ACTIVE".equals(this.getAbsString(r, "STATUS")))
					active++;
				else
					inactive++;

				// BLOCKING_SESSION is a number, where NULL is stored as 0 (and there is no SID 0)
				Double blockingSession = this.getAbsValueAsDouble(r, "BLOCKING_SESSION");
				if (blockingSession != null && blockingSession > 0)
					blocked++;
			}

			Double[] arr = new Double[3];
			arr[0] = Double.valueOf(active);
			arr[1] = Double.valueOf(inactive);
			arr[2] = Double.valueOf(blocked);

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//--------------------------------------------------------
		// What are the ACTIVE user sessions doing right now: on CPU, or waiting (per Wait Class)
		// This is a "snapshot" at sample time (CmSystemEvent has the average over the whole sample)
		//--------------------------------------------------------
		if (GRAPH_NAME_ACTIVE_BY_WAIT_CLASS.equals(tgdp.getName()))
		{
			LinkedHashMap<String, Double> dataMap = new LinkedHashMap<String, Double>();

			// Always have 'ON CPU', so the graph has at least one series
			dataMap.put("ON CPU", Double.valueOf(0));

			for (int r = 0; r < this.size(); r++)
			{
				if ( ! "USER".equals(this.getAbsString(r, "TYPE")) || "Y".equals(this.getAbsString(r, "IS_OWN_SESSION")) )
					continue;

				if ( ! "ACTIVE".equals(this.getAbsString(r, "STATUS")) )
					continue;

				// If STATE is not WAITING, the session is on CPU (and WAIT_CLASS is the last thing it waited for)
				String label = "WAITING".equals(this.getAbsString(r, "STATE")) ? this.getAbsString(r, "WAIT_CLASS") : "ON CPU";

				// An ACTIVE session that waits for the client (for example in the middle of a fetch) is not doing anything
				if ("Idle".equals(label))
					continue;

				Double cnt = dataMap.get(label);
				dataMap.put(label, (cnt == null) ? 1.0 : cnt + 1.0);
			}

			tgdp.setData(this.getTimestamp(), dataMap);
		}

		//--------------------------------------------------------
		// PGA Memory, for ALL sessions (also the background processes)
		//--------------------------------------------------------
		if (GRAPH_NAME_PGA_MB.equals(tgdp.getName()))
		{
			Double used  = this.getAbsValueSum("PGA_USED_MB");
			Double alloc = this.getAbsValueSum("PGA_ALLOC_MB");

			Double[] arr = new Double[2];
			arr[0] = (used  == null) ? 0.0 : used;
			arr[1] = (alloc == null) ? 0.0 : alloc;

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}

	@Override
	public void sendAlarmRequest()
	{
		AlarmHelper.sendAlarmRequestForColumn(this, "PROGRAM");
		AlarmHelper.sendAlarmRequestForColumn(this, "USERNAME");
		AlarmHelper.sendAlarmRequestForColumn(this, "OSUSER");
	}

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		List<CmSettingsHelper> list = new ArrayList<>();

		list.addAll( AlarmHelper.getLocalAlarmSettingsForColumn(this, "PROGRAM") );
		list.addAll( AlarmHelper.getLocalAlarmSettingsForColumn(this, "USERNAME") );
		list.addAll( AlarmHelper.getLocalAlarmSettingsForColumn(this, "OSUSER") );

		return list;
	}
}
