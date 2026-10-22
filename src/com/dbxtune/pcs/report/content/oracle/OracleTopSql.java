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
package com.dbxtune.pcs.report.content.oracle;

import java.io.IOException;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Types;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.h2.tools.SimpleResultSet;

import com.dbxtune.central.llm.LlmSqlContextBuilder;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.gui.ResultSetTableModel.TableStringRenderer;
import com.dbxtune.pcs.DictCompression;
import com.dbxtune.pcs.inspection.ObjectLookupInspectorOracle;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.ShowplanLinkBuilder;
import com.dbxtune.pcs.report.content.SparklineHelper;
import com.dbxtune.pcs.report.content.SparklineHelper.AggType;
import com.dbxtune.pcs.report.content.SparklineHelper.DataSource;
import com.dbxtune.pcs.report.content.SparklineHelper.SparkLineParams;
import com.dbxtune.sql.SqlParserUtils;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.HtmlTableProducer;
import com.dbxtune.utils.HtmlTableProducer.ColumnCopyDef;
import com.dbxtune.utils.HtmlTableProducer.ColumnCopyRender;
import com.dbxtune.utils.HtmlTableProducer.ColumnCopyRow;
import com.dbxtune.utils.HtmlTableProducer.ColumnStatic;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

public class OracleTopSql
extends OracleAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _sqlTextRstm;
	private List<String>        _miniChartJsList = new ArrayList<>();

	/** The execution plans of the statements in the report. Key: the id of the hidden HTML element that holds the plan, value: the text of DBMS_XPLAN */
	private Map<String, String> _planTexts = new LinkedHashMap<>();

	public OracleTopSql(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public boolean hasMinimalMessageText()
	{
		return false;
	}

	@Override
	public boolean hasShortMessageText()
	{
		return true;
	}

	@Override
	public void writeMessageText(Writer w, MessageType messageType)
	throws IOException
	{
		if (_shortRstm.getRowCount() == 0)
		{
			w.append("No rows found <br>\n");
			return;
		}

		if (isFullMessageType())
		{
			// Get a description of this section, and column names
			w.append(getSectionDescriptionHtml(_shortRstm, true));

			w.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");

			// The SQL Text is a "tooltip" in this table (the table below has it as text)
			TableStringRenderer tableRender = new ReportEntryTableStringRenderer()
			{
				@Override
				public String cellValue(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
				{
					if ("SqlText".equals(colName))
					{
						return "<div title='Click for Details' "
								+ "data-bs-toggle='modal' "
								+ "data-bs-target='#dbx-view-sqltext-dialog' "
								+ "data-objectname='" + rstm.getValueAsString(row, "SQL_ID") + "' "
								+ "data-tooltip=\""   + getTooltipForSqlText(rstm, row) + "\" "
								+ ">&#x1F4AC;</div>"; // symbol popup with "..."
					}

					return strVal;
				}
			};

			w.append(_shortRstm.toHtmlTableString("sortable", true, true, null, tableRender));
		}

		// The details: the charts, the SQL Text, the execution plan and the tables (this is also what the "short" message has)
		if (_sqlTextRstm != null)
		{
			w.append("<br>\n");
			w.append("<details open> \n");
			w.append("<summary>Details for above Statements, including SQL Text (click to collapse) </summary> \n");
			w.append("<br>\n");
			w.append("SQL Text by 'SQL_ID', Row Count: " + _sqlTextRstm.getRowCount() + "\n");
			w.append(toHtmlTable(_sqlTextRstm));
			w.append("\n");
			w.append("</details> \n");
		}

		if (isFullMessageType())
		{
			// Write JavaScript code for the mini charts
			for (String str : _miniChartJsList)
				w.append(str);

			// The execution plans, each ONCE, as hidden blocks: "View Execution Plan" and "Get LLM Optimization Advice" read them when they are clicked
			// (type 'text/xmldata' is what dsrOpenLink() expects, the content is the plain text of DBMS_XPLAN: the browser does not parse it)
			for (Entry<String, String> plan : _planTexts.entrySet())
			{
				w.append("\n<script id='").append(plan.getKey()).append("' type='text/xmldata'>\n");
				w.append(plan.getValue().replace("</script", "<\\/script"));
				w.append("\n</script>\n");
			}
		}
	}

	/** What is shown when the "SqlText" cell of the first table is clicked: some numbers of the row, and the SQL Text. (double quotes must be avoided or escaped) */
	private String getTooltipForSqlText(ResultSetTableModel rstm, int row)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("-- Some columns extracted from current row.\n");
		sb.append("-----------------------------------------------------------------------------------------------\n");
		sb.append("-- SQL_ID:                    ").append( rstm.getValueAsString(row, "SQL_ID"                 ) ).append("\n");
		sb.append("-- CON_NAME:                  ").append( rstm.getValueAsString(row, "CON_NAME"               ) ).append("\n");
		sb.append("-- plan_count:                ").append( rstm.getValueAsString(row, "plan_count"             ) ).append("\n");
		sb.append("-- SessionSampleTime__min:    ").append( rstm.getValueAsString(row, "SessionSampleTime__min" ) ).append("\n");
		sb.append("-- SessionSampleTime__max:    ").append( rstm.getValueAsString(row, "SessionSampleTime__max" ) ).append("\n");
		sb.append("-- Duration:                  ").append( rstm.getValueAsString(row, "Duration"               ) ).append("\n");

		sb.append("-- EXECUTIONS__sum:           ").append( num(rstm.getValueAsBigDecimal(row, "EXECUTIONS__sum"          )) ).append("\n");
		sb.append("-- ELAPSED_TIME_ms__sum:      ").append( num(rstm.getValueAsBigDecimal(row, "ELAPSED_TIME_ms__sum"     )) ).append("  (in milli seconds), and in (HH:MM:SS.sss) ").append( TimeUtils.msToTimeStrLong(rstm.getValueAsLong(row, "ELAPSED_TIME_ms__sum")) ).append(" \n");
		sb.append(">> ELAPSED_TIME_ms__per_exec: ").append( num(rstm.getValueAsBigDecimal(row, "ELAPSED_TIME_ms__per_exec")) ).append("\n");
		sb.append("-- CPU_TIME_ms__sum:          ").append( num(rstm.getValueAsBigDecimal(row, "CPU_TIME_ms__sum"         )) ).append("  (in milli seconds), and in (HH:MM:SS.sss) ").append( TimeUtils.msToTimeStrLong(rstm.getValueAsLong(row, "CPU_TIME_ms__sum")) ).append(" \n");
		sb.append(">> CPU_TIME_ms__per_exec:     ").append( num(rstm.getValueAsBigDecimal(row, "CPU_TIME_ms__per_exec"    )) ).append("\n");
		sb.append("-- USER_IO_WAIT_ms__sum:      ").append( num(rstm.getValueAsBigDecimal(row, "USER_IO_WAIT_ms__sum"     )) ).append("\n");
		sb.append("-- APPLICATION_WAIT_ms__sum:  ").append( num(rstm.getValueAsBigDecimal(row, "APPLICATION_WAIT_ms__sum" )) ).append("\n");
		sb.append("-- CONCURRENCY_WAIT_ms__sum:  ").append( num(rstm.getValueAsBigDecimal(row, "CONCURRENCY_WAIT_ms__sum" )) ).append("\n");
		sb.append("-- BUFFER_GETS__sum:          ").append( num(rstm.getValueAsBigDecimal(row, "BUFFER_GETS__sum"         )) ).append("\n");
		sb.append("-- BUFFER_GETS__per_exec:     ").append( num(rstm.getValueAsBigDecimal(row, "BUFFER_GETS__per_exec"    )) ).append("\n");
		sb.append("-- DISK_READS__sum:           ").append( num(rstm.getValueAsBigDecimal(row, "DISK_READS__sum"          )) ).append("\n");
		sb.append("-- DISK_READS__per_exec:      ").append( num(rstm.getValueAsBigDecimal(row, "DISK_READS__per_exec"     )) ).append("\n");
		sb.append("-- ROWS_PROCESSED__sum:       ").append( num(rstm.getValueAsBigDecimal(row, "ROWS_PROCESSED__sum"      )) ).append("\n");
		sb.append("-- ROWS_PROCESSED__per_exec:  ").append( num(rstm.getValueAsBigDecimal(row, "ROWS_PROCESSED__per_exec" )) ).append("\n");
		sb.append("-----------------------------------------------------------------------------------------------\n");
		sb.append(StringEscapeUtils.escapeHtml4(rstm.getValueAsString(row, "SqlText")));

		return sb.toString();
	}

	/** A number in the tooltip. A "per exec" value is NULL when no execution was started in the period */
	private static String num(BigDecimal value)
	{
		return value == null ? "-" : NumberFormat.getInstance().format(value);
	}

	@Override
	public String getSubject()
	{
		return "Top SQL Statements (order by: ELAPSED_TIME, origin: CmSqlStats / V$SQLSTATS)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}

	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmSqlStats_diff" };
	}

	@Override
	public List<ReportingIndexEntry> getReportingIndexes()
	{
		List<ReportingIndexEntry> list = new ArrayList<>();

		// For the "Table Information": how the tables and their indexes were used during the recording (see OracleAbstract.readSegmentActivity)
		list.add(new ReportingIndexEntry("CmSegmentStats_diff", "OWNER", "OBJECT_NAME"));
		list.add(new ReportingIndexEntry("CmSegmentStats_abs" , "OWNER", "OBJECT_NAME", "SessionSampleTime"));

		return list;
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		int topRows = getTopRows();

		// The SQL Text is normally stored "Dictionary Compressed": the column has a hash id, and the text is in a table of it's own
		boolean hasDictCompCols = false;
		try {
			hasDictCompCols = DictCompression.hasCompressedColumnNames(conn, null, "CmSqlStats_diff");
		} catch (SQLException ex) {
			_logger.error("Problems checking for Dictionary Compressed Columns in table 'CmSqlStats_diff'.", ex);
		}
		String col_SQL_FULLTEXT = hasDictCompCols ? "SQL_FULLTEXT$dcc$" : "SQL_FULLTEXT";

		// A statement that is seen for the first time has it's counters "since it was loaded" (and not "since the previous sample"): do not count those rows
		String sql_and_skipNewDiffRateRows = "  and " + conn.toBitAnd("[CmRowState]", CountersModel.ROW_STATE__IS_DIFF_OR_RATE_ROW) + " = 0 \n";

		// "per execution" over the whole period: NULL when no execution was started in the period (the statement was already running when the recording started)
		String perExec = " / nullif(sum([EXECUTIONS]), 0)";

		// The statements that are in the top ## on: elapsed time, CPU time or buffer gets
		// For every measure: a chart (filled in below), the sum, and the value per execution
		String sql = getCmDiffColumnsAsSqlComment("CmSqlStats")
			    + "select * \n"
			    + "from ( \n"
			    + "    select \n"
			    + "         [SQL_ID] \n"
			    + "        ,max([CON_NAME])                                                                        as [CON_NAME] \n"
			    + "        ,max([" + col_SQL_FULLTEXT + "])                                                        as [SqlText] \n"
			    + "        ,cast('' as varchar(512))                                                               as [ExecPlan] \n"
			    + "        ,count(distinct [PLAN_HASH_VALUE])                                                      as [plan_count] \n"
			    + " \n"
			    + "        ,row_number() over (order by sum([ELAPSED_TIME]) desc)                                  as [rank_elapsed] \n"
			    + "        ,row_number() over (order by sum([CPU_TIME])     desc)                                  as [rank_cpu] \n"
			    + "        ,row_number() over (order by sum([BUFFER_GETS])  desc)                                  as [rank_gets] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                               as [EXECUTIONS__chart] \n"
			    + "        ,sum([EXECUTIONS])                                                                      as [EXECUTIONS__sum] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                               as [ELAPSED_TIME_ms__chart] \n"
			    + "        ,CAST( sum([ELAPSED_TIME])          / 1000.0              as DECIMAL(20,0) )            as [ELAPSED_TIME_ms__sum] \n"
			    + "        ,CAST( sum([ELAPSED_TIME])          / 1000.0 " + perExec + " as DECIMAL(20,1) )         as [ELAPSED_TIME_ms__per_exec] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                               as [CPU_TIME_ms__chart] \n"
			    + "        ,CAST( sum([CPU_TIME])              / 1000.0              as DECIMAL(20,0) )            as [CPU_TIME_ms__sum] \n"
			    + "        ,CAST( sum([CPU_TIME])              / 1000.0 " + perExec + " as DECIMAL(20,1) )         as [CPU_TIME_ms__per_exec] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                               as [USER_IO_WAIT_ms__chart] \n"
			    + "        ,CAST( sum([USER_IO_WAIT_TIME])     / 1000.0              as DECIMAL(20,0) )            as [USER_IO_WAIT_ms__sum] \n"
			    + "        ,CAST( sum([USER_IO_WAIT_TIME])     / 1000.0 " + perExec + " as DECIMAL(20,1) )         as [USER_IO_WAIT_ms__per_exec] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                               as [APPLICATION_WAIT_ms__chart] \n"
			    + "        ,CAST( sum([APPLICATION_WAIT_TIME]) / 1000.0              as DECIMAL(20,0) )            as [APPLICATION_WAIT_ms__sum] \n"
			    + "        ,CAST( sum([APPLICATION_WAIT_TIME]) / 1000.0 " + perExec + " as DECIMAL(20,1) )         as [APPLICATION_WAIT_ms__per_exec] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                               as [CONCURRENCY_WAIT_ms__chart] \n"
			    + "        ,CAST( sum([CONCURRENCY_WAIT_TIME]) / 1000.0              as DECIMAL(20,0) )            as [CONCURRENCY_WAIT_ms__sum] \n"
			    + "        ,CAST( sum([CONCURRENCY_WAIT_TIME]) / 1000.0 " + perExec + " as DECIMAL(20,1) )         as [CONCURRENCY_WAIT_ms__per_exec] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                               as [CLUSTER_WAIT_ms__chart] \n"
			    + "        ,CAST( sum([CLUSTER_WAIT_TIME])     / 1000.0              as DECIMAL(20,0) )            as [CLUSTER_WAIT_ms__sum] \n"
			    + "        ,CAST( sum([CLUSTER_WAIT_TIME])     / 1000.0 " + perExec + " as DECIMAL(20,1) )         as [CLUSTER_WAIT_ms__per_exec] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                               as [PLSQL_EXEC_ms__chart] \n"
			    + "        ,CAST( sum([PLSQL_EXEC_TIME])       / 1000.0              as DECIMAL(20,0) )            as [PLSQL_EXEC_ms__sum] \n"
			    + "        ,CAST( sum([PLSQL_EXEC_TIME])       / 1000.0 " + perExec + " as DECIMAL(20,1) )         as [PLSQL_EXEC_ms__per_exec] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                               as [PX_SERVERS__chart] \n"
			    + "        ,sum([PX_SERVERS_EXECUTIONS])                                                           as [PX_SERVERS__sum] \n"
			    + "        ,CAST( sum([PX_SERVERS_EXECUTIONS]) * 1.0 " + perExec + "    as DECIMAL(20,1) )         as [PX_SERVERS__per_exec] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                               as [ROWS_PROCESSED__chart] \n"
			    + "        ,sum([ROWS_PROCESSED])                                                                  as [ROWS_PROCESSED__sum] \n"
			    + "        ,CAST( sum([ROWS_PROCESSED])        * 1.0 " + perExec + "    as DECIMAL(20,1) )         as [ROWS_PROCESSED__per_exec] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                               as [BUFFER_GETS__chart] \n"
			    + "        ,sum([BUFFER_GETS])                                                                     as [BUFFER_GETS__sum] \n"
			    + "        ,CAST( sum([BUFFER_GETS])           * 1.0 " + perExec + "    as DECIMAL(20,1) )         as [BUFFER_GETS__per_exec] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                               as [DISK_READS__chart] \n"
			    + "        ,sum([DISK_READS])                                                                      as [DISK_READS__sum] \n"
			    + "        ,CAST( sum([DISK_READS])            * 1.0 " + perExec + "    as DECIMAL(20,1) )         as [DISK_READS__per_exec] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                               as [PHYSICAL_READ_MB__chart] \n"
			    + "        ,CAST( sum([PHYSICAL_READ_BYTES])   / 1048576.0           as DECIMAL(20,1) )            as [PHYSICAL_READ_MB__sum] \n"
			    + "        ,CAST( sum([PHYSICAL_READ_BYTES])   / 1048576.0 " + perExec + " as DECIMAL(20,1) )      as [PHYSICAL_READ_MB__per_exec] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                               as [PHYSICAL_WRITE_MB__chart] \n"
			    + "        ,CAST( sum([PHYSICAL_WRITE_BYTES])  / 1048576.0           as DECIMAL(20,1) )            as [PHYSICAL_WRITE_MB__sum] \n"
			    + "        ,CAST( sum([PHYSICAL_WRITE_BYTES])  / 1048576.0 " + perExec + " as DECIMAL(20,1) )      as [PHYSICAL_WRITE_MB__per_exec] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                               as [SORTS__chart] \n"
			    + "        ,sum([SORTS])                                                                           as [SORTS__sum] \n"
			    + "        ,CAST( sum([SORTS])                 * 1.0 " + perExec + "    as DECIMAL(20,1) )         as [SORTS__per_exec] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                               as [PARSE_CALLS__chart] \n"
			    + "        ,sum([PARSE_CALLS])                                                                     as [PARSE_CALLS__sum] \n"
			    + "        ,CAST( sum([PARSE_CALLS])           * 1.0 " + perExec + "    as DECIMAL(20,1) )         as [PARSE_CALLS__per_exec] \n"
			    + " \n"
			    + "        ,count(distinct [SessionSampleTime])                                                    as [samples_count] \n"
			    + "        ,min([SessionSampleTime])                                                               as [SessionSampleTime__min] \n"
			    + "        ,max([SessionSampleTime])                                                               as [SessionSampleTime__max] \n"
			    + "        ,cast('' as varchar(30))                                                                as [Duration] \n"
			    + "    from [CmSqlStats_diff] \n"
			    + "    where ([EXECUTIONS] > 0 or [ELAPSED_TIME] > 0) \n"   // only samples where the statement did something
			    + "  " + sql_and_skipNewDiffRateRows
			    + "  " + getReportPeriodSqlWhere()
			    + "    group by [SQL_ID] \n"
			    + ") x \n"
			    + "where [rank_elapsed] <= " + topRows + " \n"
			    + "   or [rank_cpu]     <= " + topRows + " \n"
			    + "   or [rank_gets]    <= " + topRows + " \n"
			    + "order by [rank_elapsed] \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, true, "OracleTopSql", false); // false = do not truncate (the SQL Text)

		if (_shortRstm.getRowCount() == 0)
			return;

		// Highlight sort column
		_shortRstm.setHighlightSortColumns("ELAPSED_TIME_ms__sum");

		_shortRstm.setDescription(
				"The SQL Statements that used most resources during the period (from the table <code>CmSqlStats_diff</code>).<br>" +
				"A statement is listed if it is in the top " + topRows + " on <b>elapsed time</b>, <b>CPU time</b> or <b>buffer gets</b> (the three <code>rank_</code> columns says where it's placed in each).<br>" +
				"The <code>__chart</code> columns shows <b>when</b> during the period, in 10 minute chunks. <code>EXECUTIONS__chart</code> is the number of executions, all the others are the value <b>per execution</b> " +
				"(a chunk where the statement was still running, but no execution was started, is counted as one execution: Oracle counts an execution when it starts).<br>" +
				"<b>Note</b>: The collector only records the statements with most buffer gets (property <code>CmSqlStats.sample.topRows</code>), " +
				"and the first sample where a statement is seen is not counted (those counters are 'since the statement was loaded'). So a statement that was executed only once can be missing.<br>" +
				"");

		_shortRstm.setColumnDescription("SQL_ID"                       , "Oracle's id of the statement (a hash of the SQL Text)");
		_shortRstm.setColumnDescription("CON_NAME"                     , "Container (PDB) name. If the statement was executed in several containers: one of them.");
		_shortRstm.setColumnDescription("SqlText"                      , "The SQL Text: click to see it (it is also in the table below)");
		_shortRstm.setColumnDescription("ExecPlan"                     , "The execution plan(s) the collector stored for the statement: click to see it in DbxCentral");
		_shortRstm.setColumnDescription("plan_count"                   , "How many different execution plans (PLAN_HASH_VALUE) the statement had during the period. More than 1 = the plan changed.");
		_shortRstm.setColumnDescription("rank_elapsed"                 , "Place in the list when it's sorted on elapsed time (1 = most)");
		_shortRstm.setColumnDescription("rank_cpu"                     , "Place in the list when it's sorted on CPU time (1 = most)");
		_shortRstm.setColumnDescription("rank_gets"                    , "Place in the list when it's sorted on buffer gets (1 = most)");
		_shortRstm.setColumnDescription("EXECUTIONS__chart"            , "Chart: number of executions, in 10 minute chunks\nSo you can see WHEN the statement was executed.");
		_shortRstm.setColumnDescription("EXECUTIONS__sum"              , "Number of executions");
		_shortRstm.setColumnDescription("ELAPSED_TIME_ms__chart"       , "Chart: elapsed time for ONE execution in milliseconds, in 10 minute chunks\nSo you can see when the statement was slower than normal.");
		_shortRstm.setColumnDescription("ELAPSED_TIME_ms__sum"         , "Elapsed time (CPU + waits) for all executions, in milliseconds");
		_shortRstm.setColumnDescription("ELAPSED_TIME_ms__per_exec"    , "Elapsed time for ONE execution, in milliseconds");
		_shortRstm.setColumnDescription("CPU_TIME_ms__chart"           , "Chart: CPU time for ONE execution in milliseconds, in 10 minute chunks");
		_shortRstm.setColumnDescription("CPU_TIME_ms__sum"             , "CPU time for all executions, in milliseconds");
		_shortRstm.setColumnDescription("CPU_TIME_ms__per_exec"        , "CPU time for ONE execution, in milliseconds");
		_shortRstm.setColumnDescription("USER_IO_WAIT_ms__chart"       , "Chart: time waited on disk IO for ONE execution in milliseconds, in 10 minute chunks");
		_shortRstm.setColumnDescription("USER_IO_WAIT_ms__sum"         , "Time waited on disk IO (Wait Class 'User I/O'), in milliseconds");
		_shortRstm.setColumnDescription("USER_IO_WAIT_ms__per_exec"    , "Time waited on disk IO for ONE execution, in milliseconds");
		_shortRstm.setColumnDescription("APPLICATION_WAIT_ms__chart"   , "Chart: time waited in the Wait Class 'Application' for ONE execution in milliseconds, in 10 minute chunks\nSo you can see when the statement was blocked by other sessions.");
		_shortRstm.setColumnDescription("APPLICATION_WAIT_ms__sum"     , "Time waited in the Wait Class 'Application', in milliseconds: mostly row locks held by other sessions");
		_shortRstm.setColumnDescription("APPLICATION_WAIT_ms__per_exec", "Time waited in the Wait Class 'Application' for ONE execution, in milliseconds");
		_shortRstm.setColumnDescription("CONCURRENCY_WAIT_ms__chart"   , "Chart: time waited in the Wait Class 'Concurrency' for ONE execution in milliseconds, in 10 minute chunks");
		_shortRstm.setColumnDescription("CONCURRENCY_WAIT_ms__sum"     , "Time waited in the Wait Class 'Concurrency', in milliseconds: latches, buffer busy, library cache locks");
		_shortRstm.setColumnDescription("CONCURRENCY_WAIT_ms__per_exec", "Time waited in the Wait Class 'Concurrency' for ONE execution, in milliseconds");
		_shortRstm.setColumnDescription("CLUSTER_WAIT_ms__chart"       , "Chart: time waited in the Wait Class 'Cluster' for ONE execution in milliseconds, in 10 minute chunks");
		_shortRstm.setColumnDescription("CLUSTER_WAIT_ms__sum"         , "Time waited in the Wait Class 'Cluster', in milliseconds: blocks sent between the instances of a RAC (0 when it is not a RAC)");
		_shortRstm.setColumnDescription("CLUSTER_WAIT_ms__per_exec"    , "Time waited in the Wait Class 'Cluster' for ONE execution, in milliseconds");
		_shortRstm.setColumnDescription("PLSQL_EXEC_ms__chart"         , "Chart: time in PL/SQL for ONE execution in milliseconds, in 10 minute chunks");
		_shortRstm.setColumnDescription("PLSQL_EXEC_ms__sum"           , "Time spent executing PL/SQL (for example a function that is called for every row), in milliseconds");
		_shortRstm.setColumnDescription("PLSQL_EXEC_ms__per_exec"      , "Time spent executing PL/SQL for ONE execution, in milliseconds");
		_shortRstm.setColumnDescription("PX_SERVERS__chart"            , "Chart: parallel execution servers used by ONE execution, in 10 minute chunks");
		_shortRstm.setColumnDescription("PX_SERVERS__sum"              , "Number of executions made by parallel execution servers (0 = the statement was never executed in parallel)");
		_shortRstm.setColumnDescription("PX_SERVERS__per_exec"         , "Parallel execution servers used by ONE execution (about the degree of parallelism, or 2 times that)");
		_shortRstm.setColumnDescription("ROWS_PROCESSED__chart"        , "Chart: rows returned or changed by ONE execution, in 10 minute chunks");
		_shortRstm.setColumnDescription("ROWS_PROCESSED__sum"          , "Rows returned or changed, for all executions");
		_shortRstm.setColumnDescription("ROWS_PROCESSED__per_exec"     , "Rows returned or changed, for ONE execution");
		_shortRstm.setColumnDescription("BUFFER_GETS__chart"           , "Chart: logical reads for ONE execution, in 10 minute chunks\nA step up = the statement started to do more work for the same thing (often a changed execution plan).");
		_shortRstm.setColumnDescription("BUFFER_GETS__sum"             , "Logical reads (blocks read from the buffer cache) for all executions");
		_shortRstm.setColumnDescription("BUFFER_GETS__per_exec"        , "Logical reads for ONE execution");
		_shortRstm.setColumnDescription("DISK_READS__chart"            , "Chart: physical reads for ONE execution, in 10 minute chunks");
		_shortRstm.setColumnDescription("DISK_READS__sum"              , "Physical reads (blocks read from disk) for all executions");
		_shortRstm.setColumnDescription("DISK_READS__per_exec"         , "Physical reads for ONE execution");
		_shortRstm.setColumnDescription("PHYSICAL_READ_MB__chart"      , "Chart: MB read from disk by ONE execution, in 10 minute chunks");
		_shortRstm.setColumnDescription("PHYSICAL_READ_MB__sum"        , "MB read from disk, for all executions (PHYSICAL_READ_BYTES)");
		_shortRstm.setColumnDescription("PHYSICAL_READ_MB__per_exec"   , "MB read from disk by ONE execution");
		_shortRstm.setColumnDescription("PHYSICAL_WRITE_MB__chart"     , "Chart: MB written to disk by ONE execution, in 10 minute chunks");
		_shortRstm.setColumnDescription("PHYSICAL_WRITE_MB__sum"       , "MB written to disk by the statement itself (direct path writes, and sorts or hash joins that did not fit in memory), for all executions (PHYSICAL_WRITE_BYTES)");
		_shortRstm.setColumnDescription("PHYSICAL_WRITE_MB__per_exec"  , "MB written to disk by ONE execution");
		_shortRstm.setColumnDescription("SORTS__chart"                 , "Chart: sorts made by ONE execution, in 10 minute chunks");
		_shortRstm.setColumnDescription("SORTS__sum"                   , "Number of sorts");
		_shortRstm.setColumnDescription("SORTS__per_exec"              , "Sorts made by ONE execution");
		_shortRstm.setColumnDescription("PARSE_CALLS__chart"           , "Chart: parse calls for ONE execution, in 10 minute chunks");
		_shortRstm.setColumnDescription("PARSE_CALLS__sum"             , "Number of parse calls");
		_shortRstm.setColumnDescription("PARSE_CALLS__per_exec"        , "Parse calls for ONE execution. 1 = the application parses the statement every time it executes it (it does not keep the cursor open)");
		_shortRstm.setColumnDescription("samples_count"                , "In how many collector samples the statement did something");
		_shortRstm.setColumnDescription("SessionSampleTime__min"       , "First sample where the statement did something");
		_shortRstm.setColumnDescription("SessionSampleTime__max"       , "Last sample where the statement did something");
		_shortRstm.setColumnDescription("Duration"                     , "HH:MM:SS between 'SessionSampleTime__min' and 'SessionSampleTime__max'");

		setDurationColumn(_shortRstm, "SessionSampleTime__min", "SessionSampleTime__max", "Duration");

		// When using Dictionary Compression the column is just a hashId: get the real text from the lookup table
		if (hasDictCompCols)
			updateDictionaryCompressedColumn(_shortRstm, conn, null, "CmSqlStats", "SQL_FULLTEXT", "SqlText");

		//--------------------------------------------------------------------------------
		// Mini Charts: one for every measure. They are also copied to the "details" table below
		//--------------------------------------------------------------------------------
		_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm,
				SparkLineParams.create       (DataSource.CounterModel)
				.setHtmlChartColumnName      ("EXECUTIONS__chart")
				.setHtmlWhereKeyColumnName   ("SQL_ID")
				.setDbmsTableName            ("CmSqlStats_diff")
				.setDbmsSampleTimeColumnName ("SessionSampleTime")
				.setDbmsDataValueColumnName  ("EXECUTIONS")
				.setDbmsWhereKeyColumnName   ("SQL_ID")
				.setDbmsExtraWhereClause     (sql_and_skipNewDiffRateRows.trim())
				.setSparklineTooltipPostfix  ("Number of executions in below period")
				.validate()));

		// The others are "per execution" in the 10 minutes
		// A chunk where the statement was still running, but no execution was started, is counted as ONE execution (Oracle counts an execution when it starts)
		String execs = "(case when sum([EXECUTIONS]) > 0 then sum([EXECUTIONS]) else 1 end)";

		// {chart column, the value, what the tooltip says}
		String[][] charts = new String[][] {
			{"ELAPSED_TIME_ms__chart"    , "sum([ELAPSED_TIME])          / 1000.0    / " + execs, "Average 'ELAPSED_TIME' in milliseconds per execution, for below period"},
			{"CPU_TIME_ms__chart"        , "sum([CPU_TIME])              / 1000.0    / " + execs, "Average 'CPU_TIME' in milliseconds per execution, for below period"},
			{"USER_IO_WAIT_ms__chart"    , "sum([USER_IO_WAIT_TIME])     / 1000.0    / " + execs, "Average 'USER_IO_WAIT_TIME' in milliseconds per execution, for below period"},
			{"APPLICATION_WAIT_ms__chart", "sum([APPLICATION_WAIT_TIME]) / 1000.0    / " + execs, "Average 'APPLICATION_WAIT_TIME' in milliseconds per execution, for below period"},
			{"CONCURRENCY_WAIT_ms__chart", "sum([CONCURRENCY_WAIT_TIME]) / 1000.0    / " + execs, "Average 'CONCURRENCY_WAIT_TIME' in milliseconds per execution, for below period"},
			{"CLUSTER_WAIT_ms__chart"    , "sum([CLUSTER_WAIT_TIME])     / 1000.0    / " + execs, "Average 'CLUSTER_WAIT_TIME' in milliseconds per execution, for below period"},
			{"PLSQL_EXEC_ms__chart"      , "sum([PLSQL_EXEC_TIME])       / 1000.0    / " + execs, "Average 'PLSQL_EXEC_TIME' in milliseconds per execution, for below period"},
			{"PX_SERVERS__chart"         , "sum([PX_SERVERS_EXECUTIONS]) * 1.0       / " + execs, "Average 'PX_SERVERS_EXECUTIONS' per execution, for below period"},
			{"ROWS_PROCESSED__chart"     , "sum([ROWS_PROCESSED])        * 1.0       / " + execs, "Average 'ROWS_PROCESSED' per execution, for below period"},
			{"BUFFER_GETS__chart"        , "sum([BUFFER_GETS])           * 1.0       / " + execs, "Average 'BUFFER_GETS' per execution, for below period"},
			{"DISK_READS__chart"         , "sum([DISK_READS])            * 1.0       / " + execs, "Average 'DISK_READS' per execution, for below period"},
			{"PHYSICAL_READ_MB__chart"   , "sum([PHYSICAL_READ_BYTES])   / 1048576.0 / " + execs, "Average MB read from disk per execution, for below period"},
			{"PHYSICAL_WRITE_MB__chart"  , "sum([PHYSICAL_WRITE_BYTES])  / 1048576.0 / " + execs, "Average MB written to disk per execution, for below period"},
			{"SORTS__chart"              , "sum([SORTS])                 * 1.0       / " + execs, "Average 'SORTS' per execution, for below period"},
			{"PARSE_CALLS__chart"        , "sum([PARSE_CALLS])           * 1.0       / " + execs, "Average 'PARSE_CALLS' per execution, for below period"},
		};

		for (String[] chart : charts)
		{
			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm,
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      (chart[0])
					.setHtmlWhereKeyColumnName   ("SQL_ID")
					.setDbmsTableName            ("CmSqlStats_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  (chart[1]).setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   ("SQL_ID")
					.setDbmsExtraWhereClause     (sql_and_skipNewDiffRateRows.trim())
					.setSparklineTooltipPostfix  (chart[2])
					.validate()));
		}

		//--------------------------------------------------------------------------------
		// The "details" table: SQL_ID + the charts and numbers of the statement (as a table in the cell) + the SQL Text, the execution plan and the tables
		//--------------------------------------------------------------------------------
		SimpleResultSet srs = new SimpleResultSet();
		srs.addColumn("SQL_ID",     Types.VARCHAR,       30, 0);
		srs.addColumn("sparklines", Types.VARCHAR,      512, 0);
		srs.addColumn("query",      Types.VARCHAR, 1024*128, 0);

		ColumnCopyRender msToHMS    = HtmlTableProducer.MS_TO_HMS;
		ColumnCopyRender oneDecimal = HtmlTableProducer.ONE_DECIMAL;

		HtmlTableProducer htp = new HtmlTableProducer(_shortRstm, "dsr-sub-table-chart");
		htp.setTableHeaders("Charts at 10 minute interval", "Total;style='text-align:right!important'", "Avg per exec;style='text-align:right!important'", "");
		htp.add("exec-cnt"    , new ColumnCopyRow().add( new ColumnCopyDef("EXECUTIONS__chart"         ) ).add(new ColumnCopyDef("EXECUTIONS__sum").setColBold()           ).addEmptyCol()                                                                        .addEmptyCol() );
		htp.add("exec-time"   , new ColumnCopyRow().add( new ColumnCopyDef("ELAPSED_TIME_ms__chart"    ) ).add(new ColumnCopyDef("ELAPSED_TIME_ms__sum"     , msToHMS)    ).add(new ColumnCopyDef("ELAPSED_TIME_ms__per_exec"    , oneDecimal).setColBold()).add(new ColumnStatic("ms"  )) );
		htp.add("cpu-time"    , new ColumnCopyRow().add( new ColumnCopyDef("CPU_TIME_ms__chart"        ) ).add(new ColumnCopyDef("CPU_TIME_ms__sum"         , msToHMS)    ).add(new ColumnCopyDef("CPU_TIME_ms__per_exec"        , oneDecimal).setColBold()).add(new ColumnStatic("ms"  )) );
		htp.add("io-wait"     , new ColumnCopyRow().add( new ColumnCopyDef("USER_IO_WAIT_ms__chart"    ) ).add(new ColumnCopyDef("USER_IO_WAIT_ms__sum"     , msToHMS)    ).add(new ColumnCopyDef("USER_IO_WAIT_ms__per_exec"    , oneDecimal).setColBold()).add(new ColumnStatic("ms"  )) );
		htp.add("lock-wait"   , new ColumnCopyRow().add( new ColumnCopyDef("APPLICATION_WAIT_ms__chart") ).add(new ColumnCopyDef("APPLICATION_WAIT_ms__sum" , msToHMS)    ).add(new ColumnCopyDef("APPLICATION_WAIT_ms__per_exec", oneDecimal).setColBold()).add(new ColumnStatic("ms"  )) );
		htp.add("conc-wait"   , new ColumnCopyRow().add( new ColumnCopyDef("CONCURRENCY_WAIT_ms__chart") ).add(new ColumnCopyDef("CONCURRENCY_WAIT_ms__sum" , msToHMS)    ).add(new ColumnCopyDef("CONCURRENCY_WAIT_ms__per_exec", oneDecimal).setColBold()).add(new ColumnStatic("ms"  )) );
		htp.add("cluster-wait", new ColumnCopyRow().add( new ColumnCopyDef("CLUSTER_WAIT_ms__chart"    ) ).add(new ColumnCopyDef("CLUSTER_WAIT_ms__sum"     , msToHMS)    ).add(new ColumnCopyDef("CLUSTER_WAIT_ms__per_exec"    , oneDecimal).setColBold()).add(new ColumnStatic("ms"  )) );
		htp.add("plsql-time"  , new ColumnCopyRow().add( new ColumnCopyDef("PLSQL_EXEC_ms__chart"      ) ).add(new ColumnCopyDef("PLSQL_EXEC_ms__sum"       , msToHMS)    ).add(new ColumnCopyDef("PLSQL_EXEC_ms__per_exec"      , oneDecimal).setColBold()).add(new ColumnStatic("ms"  )) );
		htp.add("px-servers"  , new ColumnCopyRow().add( new ColumnCopyDef("PX_SERVERS__chart"         ) ).add(new ColumnCopyDef("PX_SERVERS__sum"                    )    ).add(new ColumnCopyDef("PX_SERVERS__per_exec"         , oneDecimal).setColBold()).add(new ColumnStatic("#"   )) );
		htp.add("rows"        , new ColumnCopyRow().add( new ColumnCopyDef("ROWS_PROCESSED__chart"     ) ).add(new ColumnCopyDef("ROWS_PROCESSED__sum"                )    ).add(new ColumnCopyDef("ROWS_PROCESSED__per_exec"     , oneDecimal).setColBold()).add(new ColumnStatic("rows")) );
		htp.add("l-read"      , new ColumnCopyRow().add( new ColumnCopyDef("BUFFER_GETS__chart"        ) ).add(new ColumnCopyDef("BUFFER_GETS__sum"                   )    ).add(new ColumnCopyDef("BUFFER_GETS__per_exec"        , oneDecimal).setColBold()).add(new ColumnStatic("blks")) );
		htp.add("p-read"      , new ColumnCopyRow().add( new ColumnCopyDef("DISK_READS__chart"         ) ).add(new ColumnCopyDef("DISK_READS__sum"                    )    ).add(new ColumnCopyDef("DISK_READS__per_exec"         , oneDecimal).setColBold()).add(new ColumnStatic("blks")) );
		htp.add("p-read-mb"   , new ColumnCopyRow().add( new ColumnCopyDef("PHYSICAL_READ_MB__chart"   ) ).add(new ColumnCopyDef("PHYSICAL_READ_MB__sum"    , oneDecimal) ).add(new ColumnCopyDef("PHYSICAL_READ_MB__per_exec"   , oneDecimal).setColBold()).add(new ColumnStatic("mb"  )) );
		htp.add("p-write-mb"  , new ColumnCopyRow().add( new ColumnCopyDef("PHYSICAL_WRITE_MB__chart"  ) ).add(new ColumnCopyDef("PHYSICAL_WRITE_MB__sum"   , oneDecimal) ).add(new ColumnCopyDef("PHYSICAL_WRITE_MB__per_exec"  , oneDecimal).setColBold()).add(new ColumnStatic("mb"  )) );
		htp.add("sorts"       , new ColumnCopyRow().add( new ColumnCopyDef("SORTS__chart"              ) ).add(new ColumnCopyDef("SORTS__sum"                         )    ).add(new ColumnCopyDef("SORTS__per_exec"              , oneDecimal).setColBold()).add(new ColumnStatic("#"   )) );
		htp.add("parse"       , new ColumnCopyRow().add( new ColumnCopyDef("PARSE_CALLS__chart"        ) ).add(new ColumnCopyDef("PARSE_CALLS__sum"                   )    ).add(new ColumnCopyDef("PARSE_CALLS__per_exec"        , oneDecimal).setColBold()).add(new ColumnStatic("#"   )) );
		htp.validate();

		// Some rows are only shown when the statement has something there: a RAC wait, PL/SQL time, parallel execution
		htp.setRowFilter(new HtmlTableProducer.RowFilter()
		{
			@Override
			public boolean include(ResultSetTableModel rstm, int rstmRow, String rowKey)
			{
				if ("cluster-wait".equals(rowKey)) return rstm.getValueAsDouble(rstmRow, "CLUSTER_WAIT_ms__sum", true, 0d) > 0;
				if ("plsql-time"  .equals(rowKey)) return rstm.getValueAsDouble(rstmRow, "PLSQL_EXEC_ms__sum"  , true, 0d) > 0;
				if ("px-servers"  .equals(rowKey)) return rstm.getValueAsDouble(rstmRow, "PX_SERVERS__sum"     , true, 0d) > 0;
				return true;
			}
		});

		String dbxCentralBaseUrl = getReportingInstance().getDbxCentralPublicBaseUrl();
		String reportSrvName     = getReportingInstance().getServerName();
		int    pos_ExecPlan      = _shortRstm.findColumn("ExecPlan");

		for (int r=0; r<_shortRstm.getRowCount(); r++)
		{
			String sqlId   = _shortRstm.getValueAsString(r, "SQL_ID");
			String sqlText = _shortRstm.getValueAsString(r, "SqlText");

			//--------------------------------------------------------------------------------
			// The execution plan(s) the collector stored for the statement (DDL Storage): one per PLAN_HASH_VALUE it had during the day
			// The plan text is written ONCE, in a hidden block (see writeMessageText): the links read it from there when they are clicked
			//--------------------------------------------------------------------------------
			Map<String, String[]> plans = getExecutionPlans(conn, sqlId);

			String      planLinks      = ""; // for the "details" table: "View Execution Plan", or "View Execution Plan: plan 111, plan 222"
			String      shortPlanLinks = ""; // for the first table:     "view plan",           or "111, 222"
			String      firstPlanId    = null;
			String      conName        = _shortRstm.getValueAsString(r, "CON_NAME");
			String      executedBy     = ""; // who runs the statement: the PL/SQL unit it is written in, the schema it is parsed in, the module (the application)
			Set<String> planTableList  = new LinkedHashSet<>();

			for (Entry<String, String[]> plan : plans.entrySet())
			{
				String planElementId = "oraplan_" + sqlId + "_" + plan.getKey();
				_planTexts.put(planElementId, plan.getValue()[0]);

				if (firstPlanId == null)
					firstPlanId = planElementId;
				if (StringUtil.hasValue(plan.getValue()[2]))
					conName = plan.getValue()[2];
				planTableList.addAll(StringUtil.parseCommaStrToList(plan.getValue()[1], true));

				if (executedBy.isEmpty())
				{
					String parsingSchema = plan.getValue()[3];
					String module        = plan.getValue()[4];
					String program       = plan.getValue()[5];

					executedBy = (StringUtil.hasValue(program)       ? "Executed By: <code>"    + StringEscapeUtils.escapeHtml4(program)       + "</code> &emsp; " : "")
					           + (StringUtil.hasValue(parsingSchema) ? "Parsing Schema: <code>" + StringEscapeUtils.escapeHtml4(parsingSchema) + "</code> &emsp; " : "")
					           + (StringUtil.hasValue(module)        ? "Module: <code>"         + StringEscapeUtils.escapeHtml4(module)        + "</code>"         : "");
				}

				// One plan: "View Execution Plan". Several: the icon and the text once, then a link per plan
				String link = ShowplanLinkBuilder.buildViewPlanLinkHtml(dbxCentralBaseUrl, planElementId, sqlText, DbUtils.DB_PROD_NAME_ORACLE, reportSrvName, conName,
						plans.size() == 1 ? "View Execution Plan" : "plan " + plan.getKey(), planLinks.isEmpty());

				if (StringUtil.hasValue(link))
					planLinks += (planLinks.isEmpty() ? "" : ", ") + (plans.size() > 1 && planLinks.isEmpty() ? link.replace("<a ", "View Execution Plan: <a ") : link);

				String shortLink = ShowplanLinkBuilder.buildViewPlanLinkHtml(dbxCentralBaseUrl, planElementId, sqlText, DbUtils.DB_PROD_NAME_ORACLE, reportSrvName, conName,
						plans.size() == 1 ? "view plan" : plan.getKey(), false);

				if (StringUtil.hasValue(shortLink))
					shortPlanLinks += (shortPlanLinks.isEmpty() ? "" : ", ") + shortLink;
			}

			// No plan was stored: SAY so (as the other DBMS vendors does), a missing link leaves the reader wondering why
			// (a blank link is also what we get when DbxCentral has no public base URL: then a plan may well exist)
			if (plans.isEmpty())
			{
				planLinks      = "Execution Plan <b>NOT Available</b>";
				shortPlanLinks = "--not-found--";
			}
			_shortRstm.setValueAtWithOverride(shortPlanLinks, r, pos_ExecPlan);

			// "Get LLM Optimization Advice": with the (first) plan. What the tables look like is looked up when the link is clicked,
			// and the charts and numbers of the statement is read from the table in the "sparklines" cell (see SparklineHelper.getWorkloadHarvesterJs)
			String llmAdviceLink = LlmSqlContextBuilder.buildAdviceLinkHtmlJs(dbxCentralBaseUrl, sqlText, firstPlanId, DbUtils.DB_PROD_NAME_ORACLE, reportSrvName, conName);

			// Table Information: for the tables of the plan (Oracle told us which they are)
			// No plan was stored: the tables that can be read out of the SQL text, without the data dictionary (the collector does not look those up)
			Set<String> tableList = planTableList;
			if (plans.isEmpty())
			{
				tableList = new LinkedHashSet<>();
				for (String tableName : SqlParserUtils.getTables(sqlText))
				{
					if ( ! ObjectLookupInspectorOracle.isDictionaryObject(tableName) )
						tableList.add(tableName);
				}
			}

			// The formatted SQL Text, and the tables with their indexes (the same block as the other DBMS vendors has)
			// A statement on the data dictionary only has no tables to show: then just the formatted SQL Text
			String tableInfo = tableList.isEmpty() ? "" : getDbmsTableInformationForTables(conn, conName, sqlText, tableList, DbUtils.DB_PROD_NAME_ORACLE);
			if (StringUtil.isNullOrBlank(tableInfo))
				tableInfo = "\n<br>\n" + getFormattedSqlAsTooltipDiv(sqlText, "Hover or Click to see formatted SQL Text...", DbUtils.DB_PROD_NAME_ORACLE) + "\n";

			// Grab all the charts and numbers we defined in 'htp'
			String sparklines = htp.getHtmlTextForRow(r);

			srs.addRow(
					sqlId,
					sparklines,
					"<xmp>" + sqlText + "</xmp>\n"
						+ (executedBy.isEmpty() ? "" : "<br>" + executedBy + " <br>\n")
						+ "<br>\n" + planLinks
						+ (StringUtil.hasValue(llmAdviceLink) ? "<br>\n" + llmAdviceLink : "")
						+ tableInfo);
		}

		try
		{
			_sqlTextRstm = createResultSetTableModel(srs, "OracleTopSqlText", null, false); // DO NOT TRUNCATE COLUMNS
			srs.close();
		}
		catch (SQLException ex)
		{
			setProblemException(ex);

			_sqlTextRstm = ResultSetTableModel.createEmpty("OracleTopSqlText");
			_logger.warn("Problems getting Top SQL TEXT: " + ex);
		}
	}
}
