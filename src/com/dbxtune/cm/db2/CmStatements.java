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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.pcs.PcsColumnOptions;
import com.dbxtune.pcs.PcsColumnOptions.ColumnType;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

/**
 * The statements in the package cache (MON_GET_PKG_CACHE_STMT): how many times they ran, the time, CPU, waits, rows and reads.
 *
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmStatements
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmStatements.class.getSimpleName();
	public static final String   SHORT_NAME       = "Statements";
	public static final String   HTML_DESC        =
		"<html>" +
		"The statements in the package cache (MON_GET_PKG_CACHE_STMT): how many times they ran, the time, CPU, waits, rows read and returned, buffer pool reads, sorts...<br>" +
		"<br>" +
		"Diff and Rate is what happened since the previous sample. <b>AVG_EXEC_TIME_MS</b> and <b>ROWS_READ_PER_EXEC</b> in Diff/Rate are for the executions in the sample.<br>" +
		"<br>" +
		"Only the statements that has run in the last 30 minutes (property '" + CM_NAME + ".sample.lastXminutes'), " +
		"and of them the 1000 with most execution time since they came into the package cache (property '" + CM_NAME + ".sample.topRows').<br>" +
		"<b>Note</b>: a statement that is removed from the package cache (it is full, or the statement became invalid) loses its counters. " +
		"Overflows of the package cache are in the Summary (PKG_CACHE_NUM_OVERFLOWS).<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "MON_GET_PKG_CACHE_STMT"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"NUM_EXECUTIONS", "STMT_EXEC_TIME_MS", "TOTAL_CPU_TIME_MS", "TOTAL_ACT_WAIT_TIME_MS", "LOCK_WAIT_TIME_MS", "POOL_READ_TIME_MS", "DIRECT_READ_TIME_MS", "LOG_DISK_WAIT_TIME_MS",
		"LOCK_WAITS", "LOCK_TIMEOUTS", "DEADLOCKS", "LOCK_ESCALS",
		"ROWS_READ", "ROWS_RETURNED", "ROWS_MODIFIED", "LOGICAL_READS", "PHYSICAL_READS", "TOTAL_SORTS", "SORT_OVERFLOWS", "NUM_EXEC_WITH_ERROR"
	};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 300; // (every sample is up to 'sample.topRows' statements with their text: 5 minutes, as the statement CMs of the other DbxTune's)
	public static final int      DEFAULT_QUERY_TIMEOUT          = 30;

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

		return new CmStatements(counterController, guiController);
	}

	public CmStatements(ICounterController counterController, IGuiController guiController)
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
	public static final String  PROPKEY_sample_lastXminutes      = CM_NAME + ".sample.lastXminutes";
	public static final int     DEFAULT_sample_lastXminutes      = 30;

	public static final String  PROPKEY_sample_topRows           = CM_NAME + ".sample.topRows";
	public static final int     DEFAULT_sample_topRows           = 1000;

	public static final String  PROPKEY_sample_stmtText          = CM_NAME + ".sample.stmtText";
	public static final boolean DEFAULT_sample_stmtText          = true;

	public static final String GRAPH_NAME_TIME = "StmntTime";
	public static final String GRAPH_NAME_EXEC = "StmntExec";

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_lastXminutes, DEFAULT_sample_lastXminutes);
		Configuration.registerDefaultValue(PROPKEY_sample_topRows     , DEFAULT_sample_topRows);
		Configuration.registerDefaultValue(PROPKEY_sample_stmtText    , DEFAULT_sample_stmtText);
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		list.add(new CmSettingsHelper("Only statements of the last ## minutes", PROPKEY_sample_lastXminutes, Integer.class, conf.getIntProperty    (PROPKEY_sample_lastXminutes, DEFAULT_sample_lastXminutes), DEFAULT_sample_lastXminutes, "Only get the statements that has run in the last ## minutes (LAST_METRICS_UPDATE). 0 = all statements." ));
		list.add(new CmSettingsHelper("Max Number of Rows"                    , PROPKEY_sample_topRows     , Integer.class, conf.getIntProperty    (PROPKEY_sample_topRows     , DEFAULT_sample_topRows     ), DEFAULT_sample_topRows     , "Only get the ## statements with most execution time (STMT_EXEC_TIME, since the statement came into the package cache). 0 = all." ));
		list.add(new CmSettingsHelper("Get Statement Text"                    , PROPKEY_sample_stmtText    , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_stmtText    , DEFAULT_sample_stmtText    ), DEFAULT_sample_stmtText    , "Get STMT_TEXT. Static SQL and statements without parameter markers have the values in the text: set to false if they should not be stored." ));

		return list;
	}

	@Override
	public Map<String, PcsColumnOptions> getPcsColumnOptions()
	{
		Map<String, PcsColumnOptions> map = super.getPcsColumnOptions();

		// No settings in the super, create one, and set it at super
		if (map == null)
		{
			// The same texts are seen in many samples, so only store them once
			map = new HashMap<>();
			map.put("STMT_TEXT", new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));

			// Set the map in the super
			setPcsColumnOptions(map);
		}

		return map;
	}

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_TIME,
			"Statement Time, ms per Second",                                // Menu CheckBox text
			"Statement Time, Execution / CPU / Wait / Lock Wait, in ms per Second (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.AUTO),
			new String[] { "Execution Time", "CPU Time", "Wait Time", "Lock Wait Time" },
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_EXEC,
			"Statement Executions per Second",                              // Menu CheckBox text
			"Statement Executions per Second (" + SHORT_NAME + ")",         // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.AUTO),
			new String[] { "Executions", "Executions with Error" },
			LabelType.Static,
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

		pkCols.add("EXECUTABLE_ID");
		pkCols.add("MEMBER");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		int     lastXminutes = conf.getIntProperty    (PROPKEY_sample_lastXminutes, DEFAULT_sample_lastXminutes);
		int     topRows      = conf.getIntProperty    (PROPKEY_sample_topRows     , DEFAULT_sample_topRows);
		boolean stmtText     = conf.getBooleanProperty(PROPKEY_sample_stmtText    , DEFAULT_sample_stmtText);

		// NOTE: The times are in milliseconds, except TOTAL_CPU_TIME (microseconds)
		// NOTE: EXECUTABLE_ID as HEX: the same id as in 'Active Statements' (and what EXPLAIN_FROM_SECTION takes)
		String sql = ""
			+ "select /* ${cmCollectorName} */ \n"
			+ "     HEX(EXECUTABLE_ID)                                            as EXECUTABLE_ID \n"
			+ "    ,MEMBER \n"
			+ "    ,SECTION_TYPE \n"
			+ "    ,STMT_TYPE_ID \n"
			+ "    ,RTRIM(PACKAGE_SCHEMA)                                         as PACKAGE_SCHEMA \n"
			+ "    ,PACKAGE_NAME \n"
			+ "    ,EFFECTIVE_ISOLATION \n"
			+ "    ,NUM_EXECUTIONS \n"
			+ "    ,STMT_EXEC_TIME                                                as STMT_EXEC_TIME_MS \n"
			+ "    ,CAST(STMT_EXEC_TIME * 1.0 / NULLIF(NUM_EXECUTIONS, 0) AS DECIMAL(18,3)) as AVG_EXEC_TIME_MS \n"
			+ "    ,MAX_COORD_STMT_EXEC_TIME                                      as MAX_EXEC_TIME_MS \n"
			+ "    ,MAX_COORD_STMT_EXEC_TIMESTAMP                                 as MAX_EXEC_TIMESTAMP \n"
			+ "    ,TOTAL_CPU_TIME / 1000                                         as TOTAL_CPU_TIME_MS \n"
			+ "    ,TOTAL_ACT_WAIT_TIME                                           as TOTAL_ACT_WAIT_TIME_MS \n"
			+ "    ,LOCK_WAIT_TIME                                                as LOCK_WAIT_TIME_MS \n"
			+ "    ,POOL_READ_TIME                                                as POOL_READ_TIME_MS \n"
			+ "    ,DIRECT_READ_TIME                                              as DIRECT_READ_TIME_MS \n"
			+ "    ,LOG_DISK_WAIT_TIME                                            as LOG_DISK_WAIT_TIME_MS \n"
			+ "    ,LOCK_WAITS \n"
			+ "    ,LOCK_TIMEOUTS \n"
			+ "    ,DEADLOCKS \n"
			+ "    ,LOCK_ESCALS \n"
			+ "    ,ROWS_READ \n"
			+ "    ,ROWS_RETURNED \n"
			+ "    ,ROWS_MODIFIED \n"
			+ "    ,CAST(ROWS_READ * 1.0 / NULLIF(NUM_EXECUTIONS, 0) AS DECIMAL(18,1)) as ROWS_READ_PER_EXEC \n"
			+ "    ,POOL_DATA_L_READS + POOL_INDEX_L_READS + POOL_TEMP_DATA_L_READS + POOL_TEMP_INDEX_L_READS + POOL_XDA_L_READS + POOL_COL_L_READS as LOGICAL_READS \n"
			+ "    ,POOL_DATA_P_READS + POOL_INDEX_P_READS + POOL_TEMP_DATA_P_READS + POOL_TEMP_INDEX_P_READS + POOL_XDA_P_READS + POOL_COL_P_READS as PHYSICAL_READS \n"
			+ "    ,TOTAL_SORTS \n"
			+ "    ,SORT_OVERFLOWS \n"
			+ "    ,NUM_EXEC_WITH_ERROR \n"
			+ "    ,LAST_EXEC_ERROR \n"
			+ "    ,LAST_EXEC_ERROR_TIMESTAMP \n"
			+ "    ,QUERY_COST_ESTIMATE \n"
			+ "    ,PREP_TIME                                                     as PREP_TIME_MS \n"
			+ "    ,INSERT_TIMESTAMP \n"
			+ "    ,LAST_METRICS_UPDATE \n"
			+ "    ," + (stmtText ? "STMT_TEXT" : "CAST(NULL AS CLOB(2M))") + "   as STMT_TEXT \n"
			+ "from table(MON_GET_PKG_CACHE_STMT(NULL, NULL, NULL, -2)) t \n"
			+ (lastXminutes > 0 ? "where LAST_METRICS_UPDATE >= CURRENT TIMESTAMP - " + lastXminutes + " MINUTES \n" : "")
			+ "order by STMT_EXEC_TIME desc \n"
			+ (topRows > 0 ? "fetch first " + topRows + " rows only \n" : "")
			+ "";

		return sql;
	}

	/** If we can't execute the function: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return Db2CmUtils.checkFunctionsAreExecutable(this, conn, "MON_GET_PKG_CACHE_STMT(NULL, NULL, NULL, -2)");
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "The statements in the package cache (MON_GET_PKG_CACHE_STMT).");

			mtd.addColumn(CM_NAME, "EXECUTABLE_ID",          "<html>The id of the statement in the package cache (hex). The same id as in 'Active Statements'.</html>");
			mtd.addColumn(CM_NAME, "MEMBER",                 "<html>The database member (0 if this is not pureScale or DPF).</html>");
			mtd.addColumn(CM_NAME, "SECTION_TYPE",           "<html>D = dynamic SQL, S = static SQL (in a package).</html>");
			mtd.addColumn(CM_NAME, "STMT_TYPE_ID",           "<html>The kind of statement, for example 'DML, Select (blockable)', 'DML, Insert/Update/Delete', 'CALL'.</html>");
			mtd.addColumn(CM_NAME, "PACKAGE_SCHEMA",         "<html>The schema of the package (static SQL), or of the client package (dynamic SQL, for example NULLID).</html>");
			mtd.addColumn(CM_NAME, "PACKAGE_NAME",           "<html>The package (static SQL), or the client package (dynamic SQL, for example SYSSH200).</html>");
			mtd.addColumn(CM_NAME, "EFFECTIVE_ISOLATION",    "<html>The isolation level: UR, CS, RS or RR.</html>");
			mtd.addColumn(CM_NAME, "NUM_EXECUTIONS",         "<html>Number of executions.</html>");
			mtd.addColumn(CM_NAME, "STMT_EXEC_TIME_MS",      "<html>Total execution time, in milliseconds. (STMT_EXEC_TIME)</html>");
			mtd.addColumn(CM_NAME, "AVG_EXEC_TIME_MS",       "<html>Average time per execution, in milliseconds.<br>Abs: since the statement came into the package cache. Diff/Rate: for the executions in the sample.</html>");
			mtd.addColumn(CM_NAME, "MAX_EXEC_TIME_MS",       "<html>The longest execution, in milliseconds (since the statement came into the package cache). (MAX_COORD_STMT_EXEC_TIME)</html>");
			mtd.addColumn(CM_NAME, "MAX_EXEC_TIMESTAMP",     "<html>When the longest execution was.</html>");
			mtd.addColumn(CM_NAME, "TOTAL_CPU_TIME_MS",      "<html>CPU time, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "TOTAL_ACT_WAIT_TIME_MS", "<html>Time the executions waited (for locks, I/O, the log, ...), in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "LOCK_WAIT_TIME_MS",      "<html>Time waiting for locks, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "POOL_READ_TIME_MS",      "<html>Time reading pages from disk into the buffer pools, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "DIRECT_READ_TIME_MS",    "<html>Time for direct reads (LOBs, LONG data), in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "LOG_DISK_WAIT_TIME_MS",  "<html>Time waiting for the transaction log to be written (at commit), in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "LOCK_WAITS",             "<html>Number of times the executions waited for a lock.</html>");
			mtd.addColumn(CM_NAME, "LOCK_TIMEOUTS",          "<html>Number of lock timeouts (SQL0911N reason 68).</html>");
			mtd.addColumn(CM_NAME, "DEADLOCKS",              "<html>Number of deadlocks.</html>");
			mtd.addColumn(CM_NAME, "LOCK_ESCALS",            "<html>Number of lock escalations (row locks replaced by a table lock).</html>");
			mtd.addColumn(CM_NAME, "ROWS_READ",              "<html>Rows read. Much more than ROWS_RETURNED can mean a missing index.</html>");
			mtd.addColumn(CM_NAME, "ROWS_RETURNED",          "<html>Rows returned to the client.</html>");
			mtd.addColumn(CM_NAME, "ROWS_MODIFIED",          "<html>Rows inserted, updated or deleted.</html>");
			mtd.addColumn(CM_NAME, "ROWS_READ_PER_EXEC",     "<html>Rows read per execution.<br>Abs: since the statement came into the package cache. Diff/Rate: for the executions in the sample.</html>");
			mtd.addColumn(CM_NAME, "LOGICAL_READS",          "<html>Buffer pool page reads (data, index, temp, XML and column-organized).</html>");
			mtd.addColumn(CM_NAME, "PHYSICAL_READS",         "<html>Pages read from disk.</html>");
			mtd.addColumn(CM_NAME, "TOTAL_SORTS",            "<html>Number of sorts.</html>");
			mtd.addColumn(CM_NAME, "SORT_OVERFLOWS",         "<html>Sorts that did not fit in memory (spilled to the temporary table space).</html>");
			mtd.addColumn(CM_NAME, "NUM_EXEC_WITH_ERROR",    "<html>Executions that ended with an error.</html>");
			mtd.addColumn(CM_NAME, "LAST_EXEC_ERROR",        "<html>The SQLCODE of the last error.</html>");
			mtd.addColumn(CM_NAME, "LAST_EXEC_ERROR_TIMESTAMP", "<html>When the last error was.</html>");
			mtd.addColumn(CM_NAME, "QUERY_COST_ESTIMATE",    "<html>The optimizer's cost estimate, in timerons.</html>");
			mtd.addColumn(CM_NAME, "PREP_TIME_MS",           "<html>Time to compile (prepare) the statement, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "INSERT_TIMESTAMP",       "<html>When the statement came into the package cache. The counters are from then.</html>");
			mtd.addColumn(CM_NAME, "LAST_METRICS_UPDATE",    "<html>When the counters were last updated (about when the statement last ran).</html>");
			mtd.addColumn(CM_NAME, "STMT_TEXT",              "<html>The statement. Off with the property '" + PROPKEY_sample_stmtText + "=false'.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	/** The text as a tooltip */
	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol)
	{
		if ("STMT_TEXT".equals(colName))
		{
			if (cellValue != null && StringUtil.hasValue(cellValue.toString()))
				return "<html><pre>" + StringEscapeUtils.escapeHtml4(StringUtil.makeApproxLineBreak(cellValue.toString(), 150, 10, "\n")) + "</pre></html>";
		}

		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}

	/**
	 * A row that was not in the previous sample is a "new" row: its diff is everything since the statement came into the package cache.<br>
	 * But if it came into the package cache AFTER the previous sample, the counters ARE what happened in the sample, so it is a normal diff row (as in SqlServerTune CmExecQueryStats).<br>
	 * A statement that comes back (it was out of 'lastXminutes' or 'topRows') stays a "new" row.
	 * <p>
	 * AVG_EXEC_TIME_MS and ROWS_READ_PER_EXEC: in diff (and rate) they are for the executions in the sample.
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int pos_NumExec       = diffData.findColumn("NUM_EXECUTIONS");
		int pos_ExecTimeMs    = diffData.findColumn("STMT_EXEC_TIME_MS");
		int pos_AvgExecTimeMs = diffData.findColumn("AVG_EXEC_TIME_MS");
		int pos_RowsRead      = diffData.findColumn("ROWS_READ");
		int pos_RowsPerExec   = diffData.findColumn("ROWS_READ_PER_EXEC");
		int pos_InsertTs      = diffData.findColumn("INSERT_TIMESTAMP");

		if (pos_NumExec == -1 || pos_ExecTimeMs == -1 || pos_AvgExecTimeMs == -1 || pos_RowsRead == -1 || pos_RowsPerExec == -1 || pos_InsertTs == -1)
			return;

		// The sample time is CURRENT TIMESTAMP of the server, the same clock as INSERT_TIMESTAMP
		Timestamp prevSampleTime = prevSample == null ? null : prevSample.getSampleTime();
		Timestamp thisSampleTime = newSample  == null ? null : newSample .getSampleTime();

		for (int r = 0; r < diffData.getRowCount(); r++)
		{
			if (isNewDeltaOrRateRow(r) && prevSampleTime != null && thisSampleTime != null)
			{
				Timestamp insertTs = diffData.getValueAsTimestamp(r, pos_InsertTs);
				if (insertTs != null && ! insertTs.before(prevSampleTime) && ! insertTs.after(thisSampleTime))
					setNewDeltaOrRateRow(r, false);
			}

			long execCount = diffData.getValueAsLong(r, pos_NumExec, 0L);

			BigDecimal avgTimeMs   = null;
			BigDecimal rowsPerExec = null;
			if (execCount > 0)
			{
				avgTimeMs   = new BigDecimal(diffData.getValueAsDouble(r, pos_ExecTimeMs, 0d) / execCount).setScale(3, RoundingMode.HALF_EVEN);
				rowsPerExec = new BigDecimal(diffData.getValueAsDouble(r, pos_RowsRead  , 0d) / execCount).setScale(1, RoundingMode.HALF_EVEN);
			}
			diffData.setValueAt(avgTimeMs  , r, pos_AvgExecTimeMs);
			diffData.setValueAt(rowsPerExec, r, pos_RowsPerExec);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		String[] colNames = null;
		if (GRAPH_NAME_TIME.equals(tgdp.getName())) colNames = new String[] { "STMT_EXEC_TIME_MS", "TOTAL_CPU_TIME_MS", "TOTAL_ACT_WAIT_TIME_MS", "LOCK_WAIT_TIME_MS" };
		if (GRAPH_NAME_EXEC.equals(tgdp.getName())) colNames = new String[] { "NUM_EXECUTIONS", "NUM_EXEC_WITH_ERROR" };

		if (colNames == null)
			return;

		// The rate summed over the statements
		// A "new" row is skipped: its diff is everything since the statement came into the package cache (see localCalculation)
		Double[] arr = new Double[colNames.length];
		for (int c = 0; c < colNames.length; c++)
		{
			double sum = 0;
			for (int r = 0; r < getRateRowCount(); r++)
			{
				Double val = getRateValueAsDouble(r, colNames[c]);
				if (val != null && ! isNewDeltaOrRateRow(r))
					sum += val;
			}
			arr[c] = new BigDecimal(sum).setScale(1, RoundingMode.HALF_EVEN).doubleValue();
		}

		// Set the values
		tgdp.setDataPoint(this.getTimestamp(), arr);
	}
}
