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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmChartDescriptor;
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
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmServiceStats
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmServiceStats.class.getSimpleName();
	public static final String   SHORT_NAME       = "Services";
	public static final String   HTML_DESC        =
		"<html>" +
		"Who uses the database? One row per service, with the time, CPU, calls and reads of the sessions that connected through it.<br>" +
		"<br>" +
		"A service is the name that the clients connect to (<code>host:port/service_name</code>). When every application has it's own service name, this tab tells which application the load comes from.<br>" +
		"In a Container Database every PDB has at least one service, so this is also the load per PDB.<br>" +
		"<ul>" +
		"  <li><b>SYS$USERS</b> - sessions that did not use a service name (connected with a SID, or locally on the server).</li>" +
		"  <li><b>SYS$BACKGROUND</b> - Oracle's background processes.</li>" +
		"</ul>" +
		"<b>Tip</b>: Look at the RATE values of <code>DB_TIME_MICRO</code>: 1 000 000 per second is one session that is busy all the time.<br>" +
		"<b>Note</b>: The time columns (<code>..._MICRO</code>) are in microseconds.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "V$SERVICE_STATS"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	// The names of the columns, and the statistic in V$SERVICE_STATS it comes from (they are the same from 11.2)
	private static final String[][] STATS = new String[][] {
		{"DB_TIME_MICRO",                  "DB time"},
		{"DB_CPU_MICRO",                   "DB CPU"},
		{"SQL_EXECUTE_ELAPSED_TIME_MICRO", "sql execute elapsed time"},
		{"PARSE_TIME_ELAPSED_MICRO",       "parse time elapsed"},
		{"USER_IO_WAIT_TIME_MICRO",        "user I/O wait time"},
		{"APPLICATION_WAIT_TIME_MICRO",    "application wait time"},
		{"CONCURRENCY_WAIT_TIME_MICRO",    "concurrency wait time"},
		{"CLUSTER_WAIT_TIME_MICRO",        "cluster wait time"},
		{"USER_CALLS",                     "user calls"},
		{"EXECUTE_COUNT",                  "execute count"},
		{"PARSE_COUNT",                    "parse count (total)"},
		{"LOGONS",                         "logons cumulative"},
		{"USER_COMMITS",                   "user commits"},
		{"USER_ROLLBACKS",                 "user rollbacks"},
		{"SESSION_LOGICAL_READS",          "session logical reads"},
		{"PHYSICAL_READS",                 "physical reads"},
		{"PHYSICAL_WRITES",                "physical writes"},
		{"DB_BLOCK_CHANGES",               "db block changes"},
		{"REDO_SIZE",                      "redo size"},
	};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"DB_TIME_MICRO",
		"DB_CPU_MICRO",
		"SQL_EXECUTE_ELAPSED_TIME_MICRO",
		"PARSE_TIME_ELAPSED_MICRO",
		"USER_IO_WAIT_TIME_MICRO",
		"APPLICATION_WAIT_TIME_MICRO",
		"CONCURRENCY_WAIT_TIME_MICRO",
		"CLUSTER_WAIT_TIME_MICRO",
		"USER_CALLS",
		"EXECUTE_COUNT",
		"PARSE_COUNT",
		"LOGONS",
		"USER_COMMITS",
		"USER_ROLLBACKS",
		"SESSION_LOGICAL_READS",
		"PHYSICAL_READS",
		"PHYSICAL_WRITES",
		"DB_BLOCK_CHANGES",
		"REDO_SIZE"
		};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 10;

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

		return new CmServiceStats(counterController, guiController);
	}

	public CmServiceStats(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_DB_TIME = "ServiceDbTime";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_DB_TIME,
			"Services, DB Time (Average Active Sessions)", 	                // Menu CheckBox text
			"DB Time per Service, in Seconds per Second = Average Active Sessions (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		// CON_ID is always 0 in a non-CDB (and in 11.2), or when we are connected to a PDB
		pkCols.add("CON_ID");
		pkCols.add("SERVICE_NAME");

		return pkCols;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "V$SERVICE_STATS");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoOracle oraVersionInfo = (DbmsVersionInfoOracle) versionInfo;

		// 12.1 and above: V$SERVICE_STATS has CON_ID. Connected to CDB$ROOT we see the services of ALL containers, so there we show which container it is
		// Connected to a PDB the view ALSO has the services of the other containers, all with the name '--UNKNOWN--' and CON_ID 0: only our own container is read
		boolean hasConId  = versionInfo.getLongVersion() >= Ver.ver(12,1);
		boolean isCdbRoot = hasConId && oraVersionInfo.isCdbRoot();

		String statSums = ""; // one sum() for every statistic: V$SERVICE_STATS has one ROW per service and statistic
		for (String[] stat : STATS)
			statSums += "    ,sum(decode(s.STAT_NAME, '" + stat[1] + "', s.VALUE, 0)) as " + stat[0] + " \n";

		String sql = ""
			+ "select \n"
			+ "     " + (isCdbRoot ? "s.CON_ID"    : "CAST(0    as NUMBER(10))")    + " as CON_ID \n"
			+ "    ," + (isCdbRoot ? "max(c.NAME)" : "CAST(NULL as VARCHAR2(128))") + " as CON_NAME \n"
			+ "    ,s.SERVICE_NAME \n"
			+ statSums
			+ "from V$SERVICE_STATS s \n"
			+ (isCdbRoot ? "left join V$CONTAINERS c on c.CON_ID = s.CON_ID \n" : "")
			+ (hasConId && ! isCdbRoot ? "where s.CON_ID = to_number(sys_context('userenv', 'con_id')) \n" : "")
			+ "group by " + (isCdbRoot ? "s.CON_ID, " : "") + "s.SERVICE_NAME \n"
			+ "order by 1, 3 \n"
			+ "";

		return sql;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "Who uses the database? One row per service, with the time, CPU, calls and reads of the sessions that connected through it.");

			mtd.addColumn(CM_NAME, "CON_ID",                         "<html>Container ID. 0 = This is not a Container Database (or the version is below 12.1), or we are connected to a PDB.</html>");
			mtd.addColumn(CM_NAME, "CON_NAME",                       "<html>Name of the Container (PDB) the service belongs to. NULL if CON_ID is 0.</html>");
			mtd.addColumn(CM_NAME, "SERVICE_NAME",                   "<html>The name that the clients connect to. SYS$USERS = sessions without a service name. SYS$BACKGROUND = Oracle's background processes.</html>");
			mtd.addColumn(CM_NAME, "DB_TIME_MICRO",                  "<html>Time the sessions of the service has spent in the database (CPU + waits, not idle time), in microseconds.<br>RATE: 1 000 000 per second = one session that is busy all the time (the 'Average Active Sessions' of the service).</html>");
			mtd.addColumn(CM_NAME, "DB_CPU_MICRO",                   "<html>CPU time that the sessions of the service has used, in microseconds.</html>");
			mtd.addColumn(CM_NAME, "SQL_EXECUTE_ELAPSED_TIME_MICRO", "<html>Time spent executing SQL statements, in microseconds.</html>");
			mtd.addColumn(CM_NAME, "PARSE_TIME_ELAPSED_MICRO",       "<html>Time spent parsing SQL statements, in microseconds.</html>");
			mtd.addColumn(CM_NAME, "USER_IO_WAIT_TIME_MICRO",        "<html>Time spent waiting for disk IO (the Wait Class 'User I/O'), in microseconds.</html>");
			mtd.addColumn(CM_NAME, "APPLICATION_WAIT_TIME_MICRO",    "<html>Time spent waiting in the Wait Class 'Application', in microseconds: mostly row locks held by other sessions.</html>");
			mtd.addColumn(CM_NAME, "CONCURRENCY_WAIT_TIME_MICRO",    "<html>Time spent waiting in the Wait Class 'Concurrency', in microseconds: latches, mutexes, buffer busy, library cache locks.</html>");
			mtd.addColumn(CM_NAME, "CLUSTER_WAIT_TIME_MICRO",        "<html>Time spent waiting in the Wait Class 'Cluster', in microseconds. Only for RAC.</html>");
			mtd.addColumn(CM_NAME, "USER_CALLS",                     "<html>Number of calls from the clients (parse, execute, fetch, commit...).</html>");
			mtd.addColumn(CM_NAME, "EXECUTE_COUNT",                  "<html>Number of SQL statements that has been executed (also the ones Oracle runs itself, recursive SQL).</html>");
			mtd.addColumn(CM_NAME, "PARSE_COUNT",                    "<html>Number of parse calls (hard and soft).</html>");
			mtd.addColumn(CM_NAME, "LOGONS",                         "<html>Number of logons. A high RATE = an application that connects and disconnects all the time, instead of using a connection pool.</html>");
			mtd.addColumn(CM_NAME, "USER_COMMITS",                   "<html>Number of commits.</html>");
			mtd.addColumn(CM_NAME, "USER_ROLLBACKS",                 "<html>Number of rollbacks.</html>");
			mtd.addColumn(CM_NAME, "SESSION_LOGICAL_READS",          "<html>Number of blocks read: from memory (the buffer cache) or from disk.</html>");
			mtd.addColumn(CM_NAME, "PHYSICAL_READS",                 "<html>Number of blocks read from disk.</html>");
			mtd.addColumn(CM_NAME, "PHYSICAL_WRITES",                "<html>Number of blocks written to disk.</html>");
			mtd.addColumn(CM_NAME, "DB_BLOCK_CHANGES",               "<html>Number of changes made to blocks (INSERT, UPDATE, DELETE).</html>");
			mtd.addColumn(CM_NAME, "REDO_SIZE",                      "<html>Amount of redo that has been generated, in bytes.</html>");
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
		// DB time per service, in seconds per second: how many sessions of the service that were busy, on average
		//--------------------------------------------------------
		if (GRAPH_NAME_DB_TIME.equals(tgdp.getName()))
		{
			LinkedHashMap<String, Double> dataMap = new LinkedHashMap<>();

			for (int r = 0; r < this.size(); r++)
			{
				String conName      = this.getAbsString(r, "CON_NAME");
				String serviceName  = this.getAbsString(r, "SERVICE_NAME");
				Double microsPerSec = this.getRateValueAsDouble(r, "DB_TIME_MICRO");

				// service   or in a CDB: container:service   (CON_ID + SERVICE_NAME is the Primary Key, so the labels are unique)
				String label = StringUtil.hasValue(conName) ? conName + ":" + serviceName : serviceName;

				dataMap.put(label, microsPerSec == null ? 0.0 : microsPerSec / 1_000_000.0);
			}

			if (dataMap.size() > 0)
				tgdp.setData(this.getTimestamp(), dataMap);
		}
	}

	/** Local graphs (beside the table): which service (= which application) uses the database */
	@Override
	public List<CmChartDescriptor> createChartDescriptors()
	{
		List<CmChartDescriptor> list = new ArrayList<>();

		list.add(new CmChartDescriptor()
			.id("service-db-time")
			.title("DB Time by Service (microseconds)")
			.chartType(CmChartDescriptor.CHART_TYPE_DUAL_PIE_BAR)
			.labelColumn("SERVICE_NAME")
			.labelPrefixColumns("CON_NAME")
			.valueColumns("DB_TIME_MICRO")
			.seriesLabels("DB Time")
			.skipZeroRows(true)
			.pieOtherLimit(0.02)
			.splitRatio(0.6));

		list.add(new CmChartDescriptor()
			.id("service-logical-reads")
			.title("Logical Reads by Service")
			.chartType(CmChartDescriptor.CHART_TYPE_DUAL_PIE_BAR)
			.labelColumn("SERVICE_NAME")
			.labelPrefixColumns("CON_NAME")
			.valueColumns("SESSION_LOGICAL_READS")
			.seriesLabels("Logical Reads")
			.skipZeroRows(true)
			.pieOtherLimit(0.02));

		return list;
	}
}
