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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import com.dbxtune.sql.conn.info.DbmsVersionInfoMySql;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.Ver;

/**
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
		"The statements of the server, per normalized statement (digest) and schema: how many times they ran, the time, rows read and sent, temp tables, scans... " +
		"(performance_schema.events_statements_summary_by_digest)<br>" +
		"<br>" +
		"Diff and Rate is what happened since the previous sample. <b>AvgTimeMs</b> and <b>RowsExaminedPerExec</b> in Diff/Rate are for the executions in the sample.<br>" +
		"<br>" +
		"Only the statements that has run in the last 30 minutes (property '" + CM_NAME + ".sample.lastXminutes'), " +
		"and of them the 1000 with most time since the server started (property '" + CM_NAME + ".sample.topRows').<br>" +
		"<b>Note</b>: MySQL keeps max 10000 digests (performance_schema_digests_size). When it is full, the new statements are counted in a row with an empty Digest (and a WARNING is written in the log).<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "events_statements_summary_by_digest"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"ExecCount", "TotalTimeMs", "LockTimeMs", "CpuTimeMs", "Errors", "Warnings",
		"RowsAffected", "RowsSent", "RowsExamined", "TmpTables", "TmpDiskTables",
		"FullJoins", "FullScans", "SortMergePasses", "SortRows", "NoIndexUsed", "NoGoodIndexUsed"
	};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 300; // (every sample is up to 'sample.topRows' statements with their text: 5 minutes, as the statement CMs of SqlServerTune and OracleTune)
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

	public static final String  PROPKEY_sample_querySampleText   = CM_NAME + ".sample.querySampleText";
	public static final boolean DEFAULT_sample_querySampleText   = true;

	// One line per schema (as the "per database" graphs of PostgresTune and SqlServerTune): a line per statement would add a new graph column for every statement that is ever in the top
	public static final String GRAPH_NAME_SCHEMA_TIME    = "StmntSchemaTime";
	public static final String GRAPH_NAME_SCHEMA_EXEC    = "StmntSchemaExec";
	public static final String GRAPH_NAME_SCHEMA_ROWSEXM = "StmntSchemaRowsExam";

	/** The graph label for statements without a default schema */
	private static final String NO_SCHEMA_LABEL = "(no schema)";

	/** We only warn once (per connect) that the digest table is full */
	private boolean _warnedDigestTableFull = false;

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_lastXminutes   , DEFAULT_sample_lastXminutes);
		Configuration.registerDefaultValue(PROPKEY_sample_topRows        , DEFAULT_sample_topRows);
		Configuration.registerDefaultValue(PROPKEY_sample_querySampleText, DEFAULT_sample_querySampleText);
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		list.add(new CmSettingsHelper("Only statements of the last ## minutes", PROPKEY_sample_lastXminutes   , Integer.class, conf.getIntProperty    (PROPKEY_sample_lastXminutes   , DEFAULT_sample_lastXminutes   ), DEFAULT_sample_lastXminutes   , "Only get the statements that has run in the last ## minutes (LAST_SEEN). 0 = all statements." ));
		list.add(new CmSettingsHelper("Max Number of Rows"                    , PROPKEY_sample_topRows        , Integer.class, conf.getIntProperty    (PROPKEY_sample_topRows        , DEFAULT_sample_topRows        ), DEFAULT_sample_topRows        , "Only get the ## statements with most time (SUM_TIMER_WAIT since the server started, or since the digest table was truncated). 0 = all." ));
		list.add(new CmSettingsHelper("Get Query Sample Text"                 , PROPKEY_sample_querySampleText, Boolean.class, conf.getBooleanProperty(PROPKEY_sample_querySampleText, DEFAULT_sample_querySampleText), DEFAULT_sample_querySampleText, "Get QUERY_SAMPLE_TEXT: an example of the statement, with the values (literals) it had. Set to false if the statements can have values that should not be stored." ));

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
			map.put("DigestText"     , new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));
			map.put("QuerySampleText", new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));

			// Set the map in the super
			setPcsColumnOptions(map);
		}

		return map;
	}

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_SCHEMA_TIME,
			"Statement Time per Schema, ms per Second",    // Menu CheckBox text
			"Statement Time per Schema, in ms per Second (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.AUTO),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_SCHEMA_EXEC,
			"Statement Executions per Schema, per Second",    // Menu CheckBox text
			"Statement Executions per Schema, per Second (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.AUTO),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_SCHEMA_ROWSEXM,
			"Statement Rows Examined per Schema, per Second",    // Menu CheckBox text
			"Statement Rows Examined per Schema, per Second (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.AUTO),
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

		pkCols.add("SchemaName");
		pkCols.add("Digest");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		int     lastXminutes    = conf.getIntProperty    (PROPKEY_sample_lastXminutes   , DEFAULT_sample_lastXminutes);
		int     topRows         = conf.getIntProperty    (PROPKEY_sample_topRows        , DEFAULT_sample_topRows);
		boolean querySampleText = conf.getBooleanProperty(PROPKEY_sample_querySampleText, DEFAULT_sample_querySampleText);

		// MySQL 8.0 base. 8.0.28: SUM_CPU_TIME, 8.0.31: MAX_TOTAL_MEMORY
		String sql = getSql(false, false, lastXminutes, topRows, querySampleText);

		// MariaDB's version numbers (10.x, 11.x) are not MySQL versions (and the CM is disabled for MariaDB in checkDependsOnOther)
		boolean isMariaDb = (versionInfo instanceof DbmsVersionInfoMySql) && ((DbmsVersionInfoMySql) versionInfo).isMariaDb();

		long version = versionInfo == null ? 0 : versionInfo.getLongVersion();
		if (version >= Ver.ver(8,0,28) && ! isMariaDb)
		{
			String sqlNewer = getSql(true, version >= Ver.ver(8,0,31), lastXminutes, topRows, querySampleText);

			// The newer SQL can NOT be tested on all versions: so "dry-run" it, and if it fails use the 8.0 SQL
			// conn is null when we just want to "view" the SQL (the CM Properties dialog)
			if (conn == null)
			{
				sql = sqlNewer;
			}
			else
			{
				try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery("select * from (" + sqlNewer.replace("${cmCollectorName}", "") + ") x where 1=2"))
				{
					sql = sqlNewer;
				}
				catch (SQLException ex)
				{
					_logger.warn(CM_NAME + ": The SQL for version " + Ver.versionNumToStr(version) + " failed, the SQL for MySQL 8.0 is used (no CpuTimeMs/MaxMemoryMb). Caught: " + ex.getMessage().trim());
				}
			}
		}

		return sql;
	}

	private String getSql(boolean hasCpuTime, boolean hasMaxMemory, int lastXminutes, int topRows, boolean querySampleText)
	{
		// The timers are in picoseconds: / 1000000000 = milliseconds
		// NULL DIGEST = the digest table is full (the new statements are counted in that row)
		// SUM_CPU_TIME is only collected when the consumer 'events_statements_cpu' is ON (OFF by default), otherwise it is 0: then NULL, so it is not mistaken for "no CPU"
		String cpuTime     = hasCpuTime   ? "IF((select ENABLED from performance_schema.setup_consumers where NAME = 'events_statements_cpu') = 'YES', CAST(SUM_CPU_TIME / 1000000000.0 as DECIMAL(18,1)), NULL)" : "CAST(NULL as DECIMAL(18,1))";
		String maxMemory   = hasMaxMemory ? "CAST(MAX_TOTAL_MEMORY / 1048576.0 as DECIMAL(12,1))"  : "CAST(NULL as DECIMAL(12,1))";
		String sampleText  = querySampleText ? "QUERY_SAMPLE_TEXT" : "CAST(NULL as CHAR)";

		return ""
			+ "select /* ${cmCollectorName} */ \n"
			+ "     IFNULL(SCHEMA_NAME, '')                                       as SchemaName \n"
			+ "    ,IFNULL(DIGEST, '')                                            as Digest \n"
			+ "    ,COUNT_STAR                                                    as ExecCount \n"
			+ "    ,CAST(SUM_TIMER_WAIT  / 1000000000.0 as DECIMAL(18,1))         as TotalTimeMs \n"
			+ "    ,CAST(AVG_TIMER_WAIT  / 1000000000.0 as DECIMAL(18,3))         as AvgTimeMs \n"
			+ "    ,CAST(MAX_TIMER_WAIT  / 1000000000.0 as DECIMAL(18,1))         as MaxTimeMs \n"
			+ "    ,CAST(QUANTILE_95     / 1000000000.0 as DECIMAL(18,1))         as Quantile95Ms \n"
			+ "    ,CAST(QUANTILE_99     / 1000000000.0 as DECIMAL(18,1))         as Quantile99Ms \n"
			+ "    ,CAST(SUM_LOCK_TIME   / 1000000000.0 as DECIMAL(18,1))         as LockTimeMs \n"
			+ "    ," + cpuTime + "                                               as CpuTimeMs \n"
			+ "    ,SUM_ERRORS                                                    as Errors \n"
			+ "    ,SUM_WARNINGS                                                  as Warnings \n"
			+ "    ,SUM_ROWS_AFFECTED                                             as RowsAffected \n"
			+ "    ,SUM_ROWS_SENT                                                 as RowsSent \n"
			+ "    ,SUM_ROWS_EXAMINED                                             as RowsExamined \n"
			+ "    ,CAST(SUM_ROWS_EXAMINED / NULLIF(COUNT_STAR, 0) as DECIMAL(18,1)) as RowsExaminedPerExec \n"
			+ "    ,SUM_CREATED_TMP_TABLES                                        as TmpTables \n"
			+ "    ,SUM_CREATED_TMP_DISK_TABLES                                   as TmpDiskTables \n"
			+ "    ,SUM_SELECT_FULL_JOIN                                          as FullJoins \n"
			+ "    ,SUM_SELECT_SCAN                                               as FullScans \n"
			+ "    ,SUM_SORT_MERGE_PASSES                                         as SortMergePasses \n"
			+ "    ,SUM_SORT_ROWS                                                 as SortRows \n"
			+ "    ,SUM_NO_INDEX_USED                                             as NoIndexUsed \n"
			+ "    ,SUM_NO_GOOD_INDEX_USED                                        as NoGoodIndexUsed \n"
			+ "    ," + maxMemory + "                                             as MaxMemoryMb \n"
			+ "    ,FIRST_SEEN                                                    as FirstSeen \n"
			+ "    ,LAST_SEEN                                                     as LastSeen \n"
			+ "    ,DIGEST_TEXT                                                   as DigestText \n"
			+ "    ," + sampleText + "                                            as QuerySampleText \n"
			+ "from performance_schema.events_statements_summary_by_digest \n"
			+ (lastXminutes > 0 ? "where LAST_SEEN >= NOW() - INTERVAL " + lastXminutes + " MINUTE \n" : "")
			+ "order by SUM_TIMER_WAIT desc \n"
			+ (topRows > 0 ? "limit " + topRows + " \n" : "")
			+ "";
	}

	/** If we can't read the view: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		if (MySqlCmUtils.disableOnMariaDb(this, conn, "its 'events_statements_summary_by_digest' does not have the MySQL 8.0 columns (QUANTILE_95, QUERY_SAMPLE_TEXT...)"))
			return false;

		return MySqlCmUtils.checkViewsAreReadable(this, conn, "performance_schema.events_statements_summary_by_digest");
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "The statements of the server, per normalized statement (digest) and schema.");

			mtd.addColumn(CM_NAME, "SchemaName",          "<html>The default schema of the connection when the statement ran (empty if there was none).</html>");
			mtd.addColumn(CM_NAME, "Digest",              "<html>The digest (a hash) of the normalized statement. Empty = the digest table was full (performance_schema_digests_size): the statements that did not fit are counted in that row.</html>");
			mtd.addColumn(CM_NAME, "ExecCount",           "<html>Number of executions.</html>");
			mtd.addColumn(CM_NAME, "TotalTimeMs",         "<html>Total time of the executions, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "AvgTimeMs",           "<html>Average time per execution, in milliseconds.<br>Abs: since the server started. Diff/Rate: for the executions in the sample (TotalTimeMs / ExecCount).</html>");
			mtd.addColumn(CM_NAME, "MaxTimeMs",           "<html>The longest execution, in milliseconds (since the server started).</html>");
			mtd.addColumn(CM_NAME, "Quantile95Ms",        "<html>95% of the executions took less than this, in milliseconds (since the server started).</html>");
			mtd.addColumn(CM_NAME, "Quantile99Ms",        "<html>99% of the executions took less than this, in milliseconds (since the server started).</html>");
			mtd.addColumn(CM_NAME, "LockTimeMs",          "<html>Time waiting for table locks, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "CpuTimeMs",           "<html>CPU time, in milliseconds. MySQL 8.0.28 and above.<br>NULL when the performance_schema consumer <code>events_statements_cpu</code> is OFF (the default). Turn it on: <code>UPDATE performance_schema.setup_consumers SET ENABLED = 'YES' WHERE NAME = 'events_statements_cpu'</code></html>");
			mtd.addColumn(CM_NAME, "Errors",              "<html>Executions that ended with an error.</html>");
			mtd.addColumn(CM_NAME, "Warnings",            "<html>Warnings of the executions.</html>");
			mtd.addColumn(CM_NAME, "RowsAffected",        "<html>Rows changed (insert/update/delete).</html>");
			mtd.addColumn(CM_NAME, "RowsSent",            "<html>Rows returned to the client.</html>");
			mtd.addColumn(CM_NAME, "RowsExamined",        "<html>Rows read by the storage engines. Much more than RowsSent can mean a missing index.</html>");
			mtd.addColumn(CM_NAME, "RowsExaminedPerExec", "<html>Rows read per execution.<br>Abs: since the server started. Diff/Rate: for the executions in the sample.</html>");
			mtd.addColumn(CM_NAME, "TmpTables",           "<html>Internal temporary tables.</html>");
			mtd.addColumn(CM_NAME, "TmpDiskTables",       "<html>Internal temporary tables that had to be written to disk.</html>");
			mtd.addColumn(CM_NAME, "FullJoins",           "<html>Joins without an index.</html>");
			mtd.addColumn(CM_NAME, "FullScans",           "<html>Full scans of the first table in a join (or of the only table).</html>");
			mtd.addColumn(CM_NAME, "SortMergePasses",     "<html>Sorts that did not fit in sort_buffer_size.</html>");
			mtd.addColumn(CM_NAME, "SortRows",            "<html>Rows sorted.</html>");
			mtd.addColumn(CM_NAME, "NoIndexUsed",         "<html>Executions that read a table without an index.</html>");
			mtd.addColumn(CM_NAME, "NoGoodIndexUsed",     "<html>Executions where no good index was found.</html>");
			mtd.addColumn(CM_NAME, "MaxMemoryMb",         "<html>The most memory one execution used, in MB. MySQL 8.0.31 and above (NULL on older versions).</html>");
			mtd.addColumn(CM_NAME, "FirstSeen",           "<html>When the statement was first seen (since the server started, or the digest table was truncated).</html>");
			mtd.addColumn(CM_NAME, "LastSeen",            "<html>When the statement last ran.</html>");
			mtd.addColumn(CM_NAME, "DigestText",          "<html>The normalized statement (the values are replaced by '?'). Max performance_schema_max_digest_length characters (1024 by default).</html>");
			mtd.addColumn(CM_NAME, "QuerySampleText",     "<html>An example of the statement, with its values. Off with the property '" + PROPKEY_sample_querySampleText + "=false'.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	/** The texts as a tooltip */
	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol)
	{
		if ("DigestText".equals(colName) || "QuerySampleText".equals(colName))
		{
			if (cellValue != null && StringUtil.hasValue(cellValue.toString()))
				return "<html><pre>" + StringEscapeUtils.escapeHtml4(StringUtil.makeApproxLineBreak(cellValue.toString(), 150, 10, "\n")) + "</pre></html>";
		}

		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}

	/**
	 * A row that was not in the previous sample is a "new" row: its diff is everything since the server started.<br>
	 * But if the statement was first seen AFTER the previous sample, the counters ARE what happened in the sample, so it is a normal diff row (as in SqlServerTune CmExecQueryStats).<br>
	 * A statement that comes back (it was out of 'lastXminutes' or 'topRows') stays a "new" row.
	 * <p>
	 * AvgTimeMs and RowsExaminedPerExec: in diff (and rate) they are for the executions in the sample.<br>
	 * Also: one WARNING when the digest table is full (there is a row with an empty Digest).
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int pos_Digest              = diffData.findColumn("Digest");
		int pos_ExecCount           = diffData.findColumn("ExecCount");
		int pos_TotalTimeMs         = diffData.findColumn("TotalTimeMs");
		int pos_AvgTimeMs           = diffData.findColumn("AvgTimeMs");
		int pos_RowsExamined        = diffData.findColumn("RowsExamined");
		int pos_RowsExaminedPerExec = diffData.findColumn("RowsExaminedPerExec");
		int pos_FirstSeen           = diffData.findColumn("FirstSeen");

		if (pos_Digest == -1 || pos_ExecCount == -1 || pos_TotalTimeMs == -1 || pos_AvgTimeMs == -1 || pos_RowsExamined == -1 || pos_RowsExaminedPerExec == -1 || pos_FirstSeen == -1)
			return;

		// The sample time is CURRENT_TIMESTAMP of the server, the same clock as FIRST_SEEN
		Timestamp prevSampleTime = prevSample == null ? null : prevSample.getSampleTime();
		Timestamp thisSampleTime = newSample  == null ? null : newSample .getSampleTime();

		for (int r = 0; r < diffData.getRowCount(); r++)
		{
			if (isNewDeltaOrRateRow(r) && prevSampleTime != null && thisSampleTime != null)
			{
				Timestamp firstSeen = diffData.getValueAsTimestamp(r, pos_FirstSeen);
				if (firstSeen != null && ! firstSeen.before(prevSampleTime) && ! firstSeen.after(thisSampleTime))
					setNewDeltaOrRateRow(r, false);
			}

			long execCount = diffData.getValueAsLong(r, pos_ExecCount, 0L);

			BigDecimal avgTimeMs = null;
			BigDecimal rowsPerExec = null;
			if (execCount > 0)
			{
				avgTimeMs   = new BigDecimal(diffData.getValueAsDouble(r, pos_TotalTimeMs , 0d) / execCount).setScale(3, RoundingMode.HALF_EVEN);
				rowsPerExec = new BigDecimal(diffData.getValueAsDouble(r, pos_RowsExamined, 0d) / execCount).setScale(1, RoundingMode.HALF_EVEN);
			}
			diffData.setValueAt(avgTimeMs  , r, pos_AvgTimeMs);
			diffData.setValueAt(rowsPerExec, r, pos_RowsExaminedPerExec);

			if ( ! _warnedDigestTableFull && "".equals(diffData.getValueAsString(r, pos_Digest)) )
			{
				_warnedDigestTableFull = true;
				_logger.warn(getName() + ": The performance_schema digest table is FULL (performance_schema_digests_size), new statements are counted in a row with an empty Digest. "
						+ "Fix: increase performance_schema_digests_size in my.cnf (needs a restart), or empty the table: TRUNCATE TABLE performance_schema.events_statements_summary_by_digest (needs DROP privilege on it).");
			}
		}
	}

	@Override
	public void reset()
	{
		super.reset();
		_warnedDigestTableFull = false;
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		String colName = null;
		if (GRAPH_NAME_SCHEMA_TIME   .equals(tgdp.getName())) colName = "TotalTimeMs";
		if (GRAPH_NAME_SCHEMA_EXEC   .equals(tgdp.getName())) colName = "ExecCount";
		if (GRAPH_NAME_SCHEMA_ROWSEXM.equals(tgdp.getName())) colName = "RowsExamined";

		if (colName == null || findColumn(colName) == -1 || getRateRowCount() == 0)
			return;

		// The rate summed per schema (sorted by name, so the lines keep their order)
		// A "new" row is skipped: its diff is everything since the server started (see localCalculation)
		Map<String, Double> perSchema = new TreeMap<>();
		for (int r = 0; r < getRateRowCount(); r++)
		{
			String schema = getRateString(r, "SchemaName");
			String label  = StringUtil.hasValue(schema) ? schema : NO_SCHEMA_LABEL;

			Double val = getRateValueAsDouble(r, colName);
			if (val == null || isNewDeltaOrRateRow(r))
				val = 0d;

			perSchema.merge(label, val, Double::sum);
		}

		String[] lArray = perSchema.keySet().toArray(new String[0]);
		Double[] dArray = new Double[lArray.length];
		for (int i = 0; i < lArray.length; i++)
			dArray[i] = new BigDecimal(perSchema.get(lArray[i])).setScale(1, RoundingMode.HALF_EVEN).doubleValue();

		// Set the values
		tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
	}
}
