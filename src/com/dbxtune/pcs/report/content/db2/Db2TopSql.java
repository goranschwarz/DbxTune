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
package com.dbxtune.pcs.report.content.db2;

import java.io.IOException;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
import com.dbxtune.cm.db2.CmStatements;
import com.dbxtune.cm.db2.Db2ExplainUtils;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.gui.ResultSetTableModel.TableStringRenderer;
import com.dbxtune.pcs.DictCompression;
import com.dbxtune.pcs.inspection.ObjectLookupInspectorDb2;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.ShowplanLinkBuilder;
import com.dbxtune.pcs.report.content.SparklineHelper;
import com.dbxtune.pcs.report.content.SparklineHelper.AggType;
import com.dbxtune.pcs.report.content.SparklineHelper.DataSource;
import com.dbxtune.pcs.report.content.SparklineHelper.SparkLineParams;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The statements that used most time, read most rows, or were executed most (from CmStatements, the package cache). Built on MySqlTopSql / OracleTopSql.
 */
public class Db2TopSql
extends Db2Abstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _sqlTextRstm;
	private List<String>        _miniChartJsList = new ArrayList<>();

	/** The execution plans of the statements in the report. Key: the id of the hidden HTML element that holds the plan, value: the plan as JSON (see Db2ExplainUtils) */
	private Map<String, String> _planTexts = new LinkedHashMap<>();

	/** At most this many plans per statement (the same EXECUTABLE_ID normally has the same plan: a new plan gives a new EXECUTABLE_ID) */
	private static final int MAX_PLANS_PER_STATEMENT = 2;

	public Db2TopSql(DailySummaryReportAbstract reportingInstance)
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
								+ "data-objectname='" + rstm.getValueAsString(row, "EXECUTABLE_ID") + "' "
								+ "data-tooltip=\""   + getTooltipForSqlText(rstm, row) + "\" "
								+ ">&#x1F4AC;</div>"; // symbol popup with "..."
					}

					return strVal;
				}
			};

			w.append(_shortRstm.toHtmlTableString("sortable", true, true, null, tableRender));
		}

		// The details: the charts and the SQL Text (this is also what the "short" message has)
		if (_sqlTextRstm != null)
		{
			w.append("<br>\n");
			w.append("<details open> \n");
			w.append("<summary>Details for above Statements, including SQL Text (click to collapse) </summary> \n");
			w.append("<br>\n");
			w.append("SQL Text by 'EXECUTABLE_ID', Row Count: " + _sqlTextRstm.getRowCount() + "\n");
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
			// (type 'text/xmldata' is what dsrOpenLink() expects, the content is the JSON as text: the browser does not parse it)
			for (Entry<String, String> plan : _planTexts.entrySet())
			{
				w.append("\n<script id='").append(plan.getKey()).append("' type='text/xmldata'>\n");
				w.append(plan.getValue().replace("</script", "<\\/script"));
				w.append("\n</script>\n");
			}
		}
	}

	/**
	 * The execution plans CmActiveStatements got for a statement while it was running (EXPLAIN_FROM_SECTION), found by its EXECUTABLE_ID.
	 * Only there when the DBA created the explain tables for the collector's login (see CmActiveStatements). Each plan once, at most MAX_PLANS_PER_STATEMENT.
	 */
	private static List<String> getExecutionPlans(DbxConnection conn, String executableId)
	{
		List<String> plans = new ArrayList<>();
		if (StringUtil.isNullOrBlank(executableId) || ! DbUtils.checkIfTableExistsNoThrow(conn, null, null, "CmActiveStatements_abs"))
			return plans;

		try
		{
			// The plan is normally stored "Dictionary Compressed": the column has a hash id, and the text is in a table of it's own (one row per different plan)
			String sql = DictCompression.hasCompressedColumnNames(conn, null, "CmActiveStatements_abs")
				? "select x.[colVal] from [CmActiveStatements$dcc$EXEC_PLAN] x where x.[hashId] in (select a.[EXEC_PLAN$dcc$] from [CmActiveStatements_abs] a where a.[EXECUTABLE_ID] = " + DbUtils.safeStr(executableId) + ")"
				: "select a.[EXEC_PLAN] from [CmActiveStatements_abs] a where a.[EXECUTABLE_ID] = " + DbUtils.safeStr(executableId) + " and a.[EXEC_PLAN] is not null";

			Set<String> distinct = new LinkedHashSet<>();
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(conn.quotifySqlString(sql)))
			{
				while (rs.next() && distinct.size() < MAX_PLANS_PER_STATEMENT)
				{
					String plan = rs.getString(1);
					if (StringUtil.hasValue(plan) && plan.trim().startsWith("{"))
						distinct.add(plan);
				}
			}
			plans.addAll(distinct);
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems getting the execution plans of EXECUTABLE_ID '" + executableId + "' from CmActiveStatements_abs. Caught: " + ex);
		}
		return plans;
	}

	/** The tables of a plan (EXPLAIN_OBJECT, type TA), as '"schema"."table"': they are the real tables, also the ones behind views and aliases */
	private static Set<String> getTablesOfPlan(String planJson)
	{
		Set<String> tables = new LinkedHashSet<>();
		try
		{
			JsonNode objects = new ObjectMapper().readTree(planJson).path("objects");
			for (JsonNode o : objects)
			{
				if ("TA".equals(o.path("type").asText()))
					tables.add("\"" + o.path("schema").asText() + "\".\"" + o.path("name").asText() + "\"");
			}
		}
		catch (IOException ex)
		{
			_logger.debug("Could not read the tables of a plan. Caught: " + ex);
		}
		return tables;
	}

	/** What is shown when the "SqlText" cell of the first table is clicked: some numbers of the row, and the SQL Text. (double quotes must be avoided or escaped) */
	private String getTooltipForSqlText(ResultSetTableModel rstm, int row)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("-- Some columns extracted from current row.\n");
		sb.append("-----------------------------------------------------------------------------------------------\n");
		sb.append("-- EXECUTABLE_ID:                   ").append( rstm.getValueAsString(row, "EXECUTABLE_ID"          ) ).append("\n");
		sb.append("-- STMT_TYPE_ID:                    ").append( rstm.getValueAsString(row, "STMT_TYPE_ID"           ) ).append("\n");
		sb.append("-- SessionSampleTime__min:          ").append( rstm.getValueAsString(row, "SessionSampleTime__min" ) ).append("\n");
		sb.append("-- SessionSampleTime__max:          ").append( rstm.getValueAsString(row, "SessionSampleTime__max" ) ).append("\n");
		sb.append("-- Duration:                        ").append( rstm.getValueAsString(row, "Duration"               ) ).append("\n");

		sb.append("-- NUM_EXECUTIONS__sum:             ").append( num(rstm.getValueAsBigDecimal(row, "NUM_EXECUTIONS__sum"            )) ).append("\n");
		sb.append("-- STMT_EXEC_TIME_MS__sum:          ").append( num(rstm.getValueAsBigDecimal(row, "STMT_EXEC_TIME_MS__sum"         )) ).append("  (in milli seconds), and in (HH:MM:SS.sss) ").append( TimeUtils.msToTimeStrLong(rstm.getValueAsLong(row, "STMT_EXEC_TIME_MS__sum")) ).append(" \n");
		sb.append(">> STMT_EXEC_TIME_MS__per_exec:     ").append( num(rstm.getValueAsBigDecimal(row, "STMT_EXEC_TIME_MS__per_exec"    )) ).append("\n");
		sb.append("-- TOTAL_CPU_TIME_MS__sum:          ").append( num(rstm.getValueAsBigDecimal(row, "TOTAL_CPU_TIME_MS__sum"         )) ).append("\n");
		sb.append("-- TOTAL_ACT_WAIT_TIME_MS__sum:     ").append( num(rstm.getValueAsBigDecimal(row, "TOTAL_ACT_WAIT_TIME_MS__sum"    )) ).append("\n");
		sb.append("-- LOCK_WAIT_TIME_MS__sum:          ").append( num(rstm.getValueAsBigDecimal(row, "LOCK_WAIT_TIME_MS__sum"         )) ).append("\n");
		sb.append("-- ROWS_READ__sum:                  ").append( num(rstm.getValueAsBigDecimal(row, "ROWS_READ__sum"                 )) ).append("\n");
		sb.append(">> ROWS_READ__per_exec:             ").append( num(rstm.getValueAsBigDecimal(row, "ROWS_READ__per_exec"            )) ).append("\n");
		sb.append("-- ROWS_RETURNED__sum:              ").append( num(rstm.getValueAsBigDecimal(row, "ROWS_RETURNED__sum"             )) ).append("\n");
		sb.append("-- ROWS_MODIFIED__sum:              ").append( num(rstm.getValueAsBigDecimal(row, "ROWS_MODIFIED__sum"             )) ).append("\n");
		sb.append("-- SORT_OVERFLOWS__sum:             ").append( num(rstm.getValueAsBigDecimal(row, "SORT_OVERFLOWS__sum"            )) ).append("\n");
		sb.append("-- NUM_EXEC_WITH_ERROR__sum:        ").append( num(rstm.getValueAsBigDecimal(row, "NUM_EXEC_WITH_ERROR__sum"       )) ).append("\n");
		sb.append("-----------------------------------------------------------------------------------------------\n");
		sb.append(StringEscapeUtils.escapeHtml4(rstm.getValueAsString(row, "SqlText")));

		return sb.toString();
	}

	/** A number in the tooltip. A "per exec" value is NULL when no execution was counted in the period */
	private static String num(BigDecimal value)
	{
		return value == null ? "-" : NumberFormat.getInstance().format(value);
	}

	@Override
	public String getSubject()
	{
		return "Top SQL Statements (order by: STMT_EXEC_TIME_MS, origin: CmStatements / MON_GET_PKG_CACHE_STMT)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}

	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmStatements_diff" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		int topRows = getTopRows();

		// The text is normally stored "Dictionary Compressed": the column has a hash id, and the text is in a table of it's own
		boolean hasDictCompCols = false;
		try {
			hasDictCompCols = DictCompression.hasCompressedColumnNames(conn, null, "CmStatements_diff");
		} catch (SQLException ex) {
			_logger.error("Problems checking for Dictionary Compressed Columns in table 'CmStatements_diff'.", ex);
		}
		String col_StmtText = hasDictCompCols ? "STMT_TEXT$dcc$" : "STMT_TEXT";

		// A statement that is seen for the first time has it's counters "since it came into the package cache" (and not "since the previous sample"): do not count those rows
		// (the collector makes it a normal row if the statement came into the package cache after the previous sample)
		String sql_and_skipNewDiffRateRows = "  and " + conn.toBitAnd("[CmRowState]", CountersModel.ROW_STATE__IS_DIFF_OR_RATE_ROW) + " = 0 \n";

		// "per execution" over the whole period: NULL when no execution was counted in the period
		String perExec = " / nullif(sum([NUM_EXECUTIONS]), 0)";

		// The statements that are in the top ## on: time, rows read or executions
		// The same statement on several members (pureScale): one row (the counters are summed)
		String sql = getCmDiffColumnsAsSqlComment("CmStatements")
			    + "select * \n"
			    + "from ( \n"
			    + "    select \n"
			    + "         [EXECUTABLE_ID] \n"
			    + "        ,max([SECTION_TYPE])                                                                    as [SECTION_TYPE] \n"
			    + "        ,max([STMT_TYPE_ID])                                                                    as [STMT_TYPE_ID] \n"
			    + "        ,max([PACKAGE_SCHEMA])                                                                  as [PACKAGE_SCHEMA] \n"
			    + "        ,max([PACKAGE_NAME])                                                                    as [PACKAGE_NAME] \n"
			    + "        ,max([EFFECTIVE_ISOLATION])                                                             as [EFFECTIVE_ISOLATION] \n"
			    + "        ,max([" + col_StmtText + "])                                                            as [SqlText] \n"
			    + "        ,cast('' as varchar(512))                                                               as [ExecPlan] \n"
			    + " \n"
			    + "        ,row_number() over (order by sum([STMT_EXEC_TIME_MS]) desc)                             as [rank_time] \n"
			    + "        ,row_number() over (order by sum([ROWS_READ])         desc)                             as [rank_rows] \n"
			    + "        ,row_number() over (order by sum([NUM_EXECUTIONS])    desc)                             as [rank_execs] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                               as [NUM_EXECUTIONS__chart] \n"
			    + "        ,sum([NUM_EXECUTIONS])                                                                  as [NUM_EXECUTIONS__sum] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                               as [STMT_EXEC_TIME_MS__chart] \n"
			    + "        ,CAST( sum([STMT_EXEC_TIME_MS])                         as DECIMAL(20,0) )              as [STMT_EXEC_TIME_MS__sum] \n"
			    + "        ,CAST( sum([STMT_EXEC_TIME_MS]) " + perExec + "         as DECIMAL(20,1) )              as [STMT_EXEC_TIME_MS__per_exec] \n"
			    + "        ,max([MAX_EXEC_TIME_MS])                                                                as [MAX_EXEC_TIME_MS__max] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                               as [TOTAL_CPU_TIME_MS__chart] \n"
			    + "        ,CAST( sum([TOTAL_CPU_TIME_MS])                         as DECIMAL(20,0) )              as [TOTAL_CPU_TIME_MS__sum] \n"
			    + "        ,CAST( sum([TOTAL_CPU_TIME_MS]) " + perExec + "         as DECIMAL(20,1) )              as [TOTAL_CPU_TIME_MS__per_exec] \n"
			    + " \n"
			    + "        ,CAST( sum([TOTAL_ACT_WAIT_TIME_MS])                    as DECIMAL(20,0) )              as [TOTAL_ACT_WAIT_TIME_MS__sum] \n"
			    + "        ,CAST( sum([TOTAL_ACT_WAIT_TIME_MS]) " + perExec + "    as DECIMAL(20,1) )              as [TOTAL_ACT_WAIT_TIME_MS__per_exec] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                               as [LOCK_WAIT_TIME_MS__chart] \n"
			    + "        ,CAST( sum([LOCK_WAIT_TIME_MS])                         as DECIMAL(20,0) )              as [LOCK_WAIT_TIME_MS__sum] \n"
			    + "        ,CAST( sum([LOCK_WAIT_TIME_MS]) " + perExec + "         as DECIMAL(20,1) )              as [LOCK_WAIT_TIME_MS__per_exec] \n"
			    + "        ,CAST( sum([POOL_READ_TIME_MS])                         as DECIMAL(20,0) )              as [POOL_READ_TIME_MS__sum] \n"
			    + "        ,CAST( sum([DIRECT_READ_TIME_MS])                       as DECIMAL(20,0) )              as [DIRECT_READ_TIME_MS__sum] \n"
			    + "        ,CAST( sum([LOG_DISK_WAIT_TIME_MS])                     as DECIMAL(20,0) )              as [LOG_DISK_WAIT_TIME_MS__sum] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                               as [ROWS_READ__chart] \n"
			    + "        ,sum([ROWS_READ])                                                                       as [ROWS_READ__sum] \n"
			    + "        ,CAST( sum([ROWS_READ])     * 1.0 " + perExec + "       as DECIMAL(20,1) )              as [ROWS_READ__per_exec] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                               as [ROWS_RETURNED__chart] \n"
			    + "        ,sum([ROWS_RETURNED])                                                                   as [ROWS_RETURNED__sum] \n"
			    + "        ,CAST( sum([ROWS_RETURNED]) * 1.0 " + perExec + "       as DECIMAL(20,1) )              as [ROWS_RETURNED__per_exec] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                               as [ROWS_MODIFIED__chart] \n"
			    + "        ,sum([ROWS_MODIFIED])                                                                   as [ROWS_MODIFIED__sum] \n"
			    + "        ,CAST( sum([ROWS_MODIFIED]) * 1.0 " + perExec + "       as DECIMAL(20,1) )              as [ROWS_MODIFIED__per_exec] \n"
			    + " \n"
			    + "        ,sum([LOGICAL_READS])                                                                   as [LOGICAL_READS__sum] \n"
			    + "        ,sum([PHYSICAL_READS])                                                                  as [PHYSICAL_READS__sum] \n"
			    + "        ,sum([TOTAL_SORTS])                                                                     as [TOTAL_SORTS__sum] \n"
			    + "        ,sum([SORT_OVERFLOWS])                                                                  as [SORT_OVERFLOWS__sum] \n"
			    + "        ,sum([LOCK_WAITS])                                                                      as [LOCK_WAITS__sum] \n"
			    + "        ,sum([LOCK_TIMEOUTS])                                                                   as [LOCK_TIMEOUTS__sum] \n"
			    + "        ,sum([DEADLOCKS])                                                                       as [DEADLOCKS__sum] \n"
			    + "        ,sum([LOCK_ESCALS])                                                                     as [LOCK_ESCALS__sum] \n"
			    + "        ,sum([NUM_EXEC_WITH_ERROR])                                                             as [NUM_EXEC_WITH_ERROR__sum] \n"
			    + "        ,max([LAST_EXEC_ERROR])                                                                 as [LAST_EXEC_ERROR] \n"
			    + "        ,max([QUERY_COST_ESTIMATE])                                                             as [QUERY_COST_ESTIMATE] \n"
			    + " \n"
			    + "        ,count(distinct [SessionSampleTime])                                                    as [samples_count] \n"
			    + "        ,min([SessionSampleTime])                                                               as [SessionSampleTime__min] \n"
			    + "        ,max([SessionSampleTime])                                                               as [SessionSampleTime__max] \n"
			    + "        ,cast('' as varchar(30))                                                                as [Duration] \n"
			    + "    from [CmStatements_diff] \n"
			    + "    where ([NUM_EXECUTIONS] > 0 or [STMT_EXEC_TIME_MS] > 0) \n"   // only samples where the statement did something
			    + "  " + sql_and_skipNewDiffRateRows
			    + "  " + getReportPeriodSqlWhere()
			    + "    group by [EXECUTABLE_ID] \n"
			    + ") x \n"
			    + "where [rank_time]  <= " + topRows + " \n"
			    + "   or [rank_rows]  <= " + topRows + " \n"
			    + "   or [rank_execs] <= " + topRows + " \n"
			    + "order by [rank_time] \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, true, "Db2TopSql", false); // false = do not truncate (the SQL Text)

		if (_shortRstm.getRowCount() == 0)
			return;

		// Highlight sort column
		_shortRstm.setHighlightSortColumns("STMT_EXEC_TIME_MS__sum");

		_shortRstm.setDescription(
				"The SQL Statements that used most resources during the period (from the table <code>CmStatements_diff</code>, the package cache).<br>" +
				"A statement is one section in the package cache (<code>EXECUTABLE_ID</code>): the same text with parameter markers (?) is one statement, but the same text with other <i>literal</i> values is another one.<br>" +
				"It is listed if it is in the top " + topRows + " on <b>time</b>, <b>rows read</b> or <b>executions</b> (the three <code>rank_</code> columns says where it's placed in each).<br>" +
				"The <code>__chart</code> columns shows <b>when</b> during the period, in 10 minute chunks. <code>NUM_EXECUTIONS__chart</code> is the number of executions, the others are the value <b>per execution</b> " +
				"(a chunk where the statement ran, but no execution ended, is counted as one execution: Db2 counts an execution when it ends).<br>" +
				"<b>Note</b>: The collector only records the statements that ran in the last 30 minutes, and of them the ones with most time (properties <code>CmStatements.sample.lastXminutes</code> and <code>CmStatements.sample.topRows</code>), every 5 minutes. " +
				"A statement that is seen for the first time is only counted from the next sample, unless it came into the package cache after the previous sample. A statement that was pushed out of the package cache, and came back, starts over.<br>" +
				"");

		_shortRstm.setColumnDescription("EXECUTABLE_ID"                    , "Db2's id of the section in the package cache (hex)");
		_shortRstm.setColumnDescription("SECTION_TYPE"                     , "D = dynamic SQL, S = static SQL (in a package)");
		_shortRstm.setColumnDescription("STMT_TYPE_ID"                     , "The kind of statement");
		_shortRstm.setColumnDescription("PACKAGE_SCHEMA"                   , "The schema of the package");
		_shortRstm.setColumnDescription("PACKAGE_NAME"                     , "The package (static SQL), or the client package (dynamic SQL, for example SYSSH200)");
		_shortRstm.setColumnDescription("EFFECTIVE_ISOLATION"              , "The isolation level: UR, CS, RS or RR");
		_shortRstm.setColumnDescription("SqlText"                          , "The SQL Text: click to see it (it is also in the table below)");
		_shortRstm.setColumnDescription("ExecPlan"                         , "The execution plan(s) the collector got while the statement was running (CmActiveStatements, only when the explain tables exist for the login): it is in the table below");
		_shortRstm.setColumnDescription("rank_time"                        , "Place in the list when it's sorted on time (1 = most)");
		_shortRstm.setColumnDescription("rank_rows"                        , "Place in the list when it's sorted on rows read (1 = most)");
		_shortRstm.setColumnDescription("rank_execs"                       , "Place in the list when it's sorted on executions (1 = most)");
		_shortRstm.setColumnDescription("NUM_EXECUTIONS__chart"            , "Chart: number of executions, in 10 minute chunks\nSo you can see WHEN the statement was executed.");
		_shortRstm.setColumnDescription("NUM_EXECUTIONS__sum"              , "Number of executions");
		_shortRstm.setColumnDescription("STMT_EXEC_TIME_MS__chart"         , "Chart: time for ONE execution in milliseconds, in 10 minute chunks\nSo you can see when the statement was slower than normal.");
		_shortRstm.setColumnDescription("STMT_EXEC_TIME_MS__sum"           , "Time for all executions, in milliseconds");
		_shortRstm.setColumnDescription("STMT_EXEC_TIME_MS__per_exec"      , "Time for ONE execution, in milliseconds");
		_shortRstm.setColumnDescription("MAX_EXEC_TIME_MS__max"            , "The longest execution, in milliseconds. NOTE: since the statement came into the package cache, not only in the period");
		_shortRstm.setColumnDescription("TOTAL_CPU_TIME_MS__chart"         , "Chart: CPU time for ONE execution in milliseconds, in 10 minute chunks");
		_shortRstm.setColumnDescription("TOTAL_CPU_TIME_MS__sum"           , "CPU time, in milliseconds");
		_shortRstm.setColumnDescription("TOTAL_CPU_TIME_MS__per_exec"      , "CPU time for ONE execution, in milliseconds");
		_shortRstm.setColumnDescription("TOTAL_ACT_WAIT_TIME_MS__sum"      , "Time the statement waited (for locks, disk, the log...), in milliseconds");
		_shortRstm.setColumnDescription("TOTAL_ACT_WAIT_TIME_MS__per_exec" , "Wait time for ONE execution, in milliseconds");
		_shortRstm.setColumnDescription("LOCK_WAIT_TIME_MS__chart"         , "Chart: time waiting for locks for ONE execution in milliseconds, in 10 minute chunks\nSo you can see when the statement was blocked by other connections.");
		_shortRstm.setColumnDescription("LOCK_WAIT_TIME_MS__sum"           , "Time waiting for locks, in milliseconds");
		_shortRstm.setColumnDescription("LOCK_WAIT_TIME_MS__per_exec"      , "Time waiting for locks for ONE execution, in milliseconds");
		_shortRstm.setColumnDescription("POOL_READ_TIME_MS__sum"           , "Time reading pages from disk into the buffer pool, in milliseconds");
		_shortRstm.setColumnDescription("DIRECT_READ_TIME_MS__sum"         , "Time reading LOB/LONG data directly from disk, in milliseconds");
		_shortRstm.setColumnDescription("LOG_DISK_WAIT_TIME_MS__sum"       , "Time waiting for the log to be written (commits), in milliseconds");
		_shortRstm.setColumnDescription("ROWS_READ__chart"                 , "Chart: rows read by ONE execution, in 10 minute chunks\nA step up = the statement started to do more work for the same thing (often a changed execution plan, or a growing table without an index).");
		_shortRstm.setColumnDescription("ROWS_READ__sum"                   , "Rows read, for all executions. Much more than ROWS_RETURNED can mean a missing index.");
		_shortRstm.setColumnDescription("ROWS_READ__per_exec"              , "Rows read by ONE execution");
		_shortRstm.setColumnDescription("ROWS_RETURNED__chart"             , "Chart: rows returned by ONE execution, in 10 minute chunks");
		_shortRstm.setColumnDescription("ROWS_RETURNED__sum"               , "Rows returned to the client, for all executions");
		_shortRstm.setColumnDescription("ROWS_RETURNED__per_exec"          , "Rows returned by ONE execution");
		_shortRstm.setColumnDescription("ROWS_MODIFIED__chart"             , "Chart: rows changed by ONE execution, in 10 minute chunks");
		_shortRstm.setColumnDescription("ROWS_MODIFIED__sum"               , "Rows changed (insert/update/delete), for all executions");
		_shortRstm.setColumnDescription("ROWS_MODIFIED__per_exec"          , "Rows changed by ONE execution");
		_shortRstm.setColumnDescription("LOGICAL_READS__sum"               , "Pages read from the buffer pools (data, index, temporary...)");
		_shortRstm.setColumnDescription("PHYSICAL_READS__sum"              , "Pages that had to be read from disk");
		_shortRstm.setColumnDescription("TOTAL_SORTS__sum"                 , "Sorts");
		_shortRstm.setColumnDescription("SORT_OVERFLOWS__sum"              , "Sorts that did not fit in SORTHEAP (they were spilled to the temporary table space)");
		_shortRstm.setColumnDescription("LOCK_WAITS__sum"                  , "Times the statement waited for a lock");
		_shortRstm.setColumnDescription("LOCK_TIMEOUTS__sum"               , "Lock waits that timed out (SQL0911N reason 68)");
		_shortRstm.setColumnDescription("DEADLOCKS__sum"                   , "Deadlocks the statement was in");
		_shortRstm.setColumnDescription("LOCK_ESCALS__sum"                 , "Lock escalations (many row locks became a table lock)");
		_shortRstm.setColumnDescription("NUM_EXEC_WITH_ERROR__sum"         , "Executions that ended with an error");
		_shortRstm.setColumnDescription("LAST_EXEC_ERROR"                  , "The SQLCODE of the last error");
		_shortRstm.setColumnDescription("QUERY_COST_ESTIMATE"              , "The optimizer's cost estimate (timerons)");
		_shortRstm.setColumnDescription("samples_count"                    , "In how many collector samples the statement did something");
		_shortRstm.setColumnDescription("SessionSampleTime__min"           , "First sample where the statement did something");
		_shortRstm.setColumnDescription("SessionSampleTime__max"           , "Last sample where the statement did something");
		_shortRstm.setColumnDescription("Duration"                         , "HH:MM:SS between 'SessionSampleTime__min' and 'SessionSampleTime__max'");

		setDurationColumn(_shortRstm, "SessionSampleTime__min", "SessionSampleTime__max", "Duration");

		// When using Dictionary Compression the column is just a hashId: get the real text from the lookup table
		if (hasDictCompCols)
			updateDictionaryCompressedColumn(_shortRstm, conn, null, "CmStatements", "STMT_TEXT", "SqlText");

		//--------------------------------------------------------------------------------
		// Mini Charts: one for every measure. They are also copied to the "details" table below
		//--------------------------------------------------------------------------------
		_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm,
				SparkLineParams.create       (DataSource.CounterModel)
				.setHtmlChartColumnName      ("NUM_EXECUTIONS__chart")
				.setHtmlWhereKeyColumnName   ("EXECUTABLE_ID")
				.setDbmsTableName            ("CmStatements_diff")
				.setDbmsSampleTimeColumnName ("SessionSampleTime")
				.setDbmsDataValueColumnName  ("NUM_EXECUTIONS")
				.setDbmsWhereKeyColumnName   ("EXECUTABLE_ID")
				.setDbmsExtraWhereClause     (sql_and_skipNewDiffRateRows.trim())
				.setSparklineTooltipPostfix  ("Number of executions in below period")
				.validate()));

		// The others are "per execution" in the 10 minutes
		// A chunk where the statement ran, but no execution ended, is counted as ONE execution (Db2 counts an execution when it ends)
		String execs = "(case when sum([NUM_EXECUTIONS]) > 0 then sum([NUM_EXECUTIONS]) else 1 end)";

		// {chart column, the value, what the tooltip says}
		String[][] charts = new String[][] {
			{"STMT_EXEC_TIME_MS__chart", "sum([STMT_EXEC_TIME_MS])         / " + execs, "Average time in milliseconds per execution, for below period"},
			{"TOTAL_CPU_TIME_MS__chart", "sum([TOTAL_CPU_TIME_MS])         / " + execs, "Average CPU time in milliseconds per execution, for below period"},
			{"LOCK_WAIT_TIME_MS__chart", "sum([LOCK_WAIT_TIME_MS])         / " + execs, "Average lock wait time in milliseconds per execution, for below period"},
			{"ROWS_READ__chart"        , "sum([ROWS_READ])         * 1.0   / " + execs, "Average rows read per execution, for below period"},
			{"ROWS_RETURNED__chart"    , "sum([ROWS_RETURNED])     * 1.0   / " + execs, "Average rows returned per execution, for below period"},
			{"ROWS_MODIFIED__chart"    , "sum([ROWS_MODIFIED])     * 1.0   / " + execs, "Average rows changed per execution, for below period"},
		};

		for (String[] chart : charts)
		{
			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm,
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      (chart[0])
					.setHtmlWhereKeyColumnName   ("EXECUTABLE_ID")
					.setDbmsTableName            ("CmStatements_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  (chart[1]).setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   ("EXECUTABLE_ID")
					.setDbmsExtraWhereClause     (sql_and_skipNewDiffRateRows.trim())
					.setSparklineTooltipPostfix  (chart[2])
					.validate()));
		}

		//--------------------------------------------------------------------------------
		// The "details" table: EXECUTABLE_ID + the charts and numbers of the statement (as a table in the cell) + the SQL Text
		//--------------------------------------------------------------------------------
		SimpleResultSet srs = new SimpleResultSet();
		srs.addColumn("EXECUTABLE_ID", Types.VARCHAR,       80, 0);
		srs.addColumn("sparklines",    Types.VARCHAR,      512, 0);
		srs.addColumn("query",         Types.VARCHAR, 1024*128, 0);

		ColumnCopyRender msToHMS    = HtmlTableProducer.MS_TO_HMS;
		ColumnCopyRender oneDecimal = HtmlTableProducer.ONE_DECIMAL;

		HtmlTableProducer htp = new HtmlTableProducer(_shortRstm, "dsr-sub-table-chart");
		htp.setTableHeaders("Charts at 10 minute interval", "Total;style='text-align:right!important'", "Avg per exec;style='text-align:right!important'", "");
		htp.add("exec-cnt"    , new ColumnCopyRow().add( new ColumnCopyDef("NUM_EXECUTIONS__chart"   ) ).add(new ColumnCopyDef("NUM_EXECUTIONS__sum").setColBold()      ).addEmptyCol()                                                                    .addEmptyCol() );
		htp.add("exec-time"   , new ColumnCopyRow().add( new ColumnCopyDef("STMT_EXEC_TIME_MS__chart") ).add(new ColumnCopyDef("STMT_EXEC_TIME_MS__sum", msToHMS)    ).add(new ColumnCopyDef("STMT_EXEC_TIME_MS__per_exec", oneDecimal).setColBold()).add(new ColumnStatic("ms"  )) );
		htp.add("cpu-time"    , new ColumnCopyRow().add( new ColumnCopyDef("TOTAL_CPU_TIME_MS__chart") ).add(new ColumnCopyDef("TOTAL_CPU_TIME_MS__sum", msToHMS)    ).add(new ColumnCopyDef("TOTAL_CPU_TIME_MS__per_exec", oneDecimal).setColBold()).add(new ColumnStatic("ms"  )) );
		htp.add("lock-time"   , new ColumnCopyRow().add( new ColumnCopyDef("LOCK_WAIT_TIME_MS__chart") ).add(new ColumnCopyDef("LOCK_WAIT_TIME_MS__sum", msToHMS)    ).add(new ColumnCopyDef("LOCK_WAIT_TIME_MS__per_exec", oneDecimal).setColBold()).add(new ColumnStatic("ms"  )) );
		htp.add("rows-read"   , new ColumnCopyRow().add( new ColumnCopyDef("ROWS_READ__chart"        ) ).add(new ColumnCopyDef("ROWS_READ__sum"                  )    ).add(new ColumnCopyDef("ROWS_READ__per_exec"        , oneDecimal).setColBold()).add(new ColumnStatic("rows")) );
		htp.add("rows-ret"    , new ColumnCopyRow().add( new ColumnCopyDef("ROWS_RETURNED__chart"    ) ).add(new ColumnCopyDef("ROWS_RETURNED__sum"              )    ).add(new ColumnCopyDef("ROWS_RETURNED__per_exec"    , oneDecimal).setColBold()).add(new ColumnStatic("rows")) );
		htp.add("rows-changed", new ColumnCopyRow().add( new ColumnCopyDef("ROWS_MODIFIED__chart"    ) ).add(new ColumnCopyDef("ROWS_MODIFIED__sum"              )    ).add(new ColumnCopyDef("ROWS_MODIFIED__per_exec"    , oneDecimal).setColBold()).add(new ColumnStatic("rows")) );
		htp.validate();

		// Some rows are only shown when the statement has something there: a SELECT changes no rows, and most statements never wait for a lock
		htp.setRowFilter(new HtmlTableProducer.RowFilter()
		{
			@Override
			public boolean include(ResultSetTableModel rstm, int rstmRow, String rowKey)
			{
				if ("lock-time"   .equals(rowKey)) return rstm.getValueAsDouble(rstmRow, "LOCK_WAIT_TIME_MS__sum", true, 0d) > 0;
				if ("rows-changed".equals(rowKey)) return rstm.getValueAsDouble(rstmRow, "ROWS_MODIFIED__sum"    , true, 0d) > 0;
				return true;
			}
		});

		String dbxCentralBaseUrl = getReportingInstance().getDbxCentralPublicBaseUrl();
		String reportSrvName     = getReportingInstance().getServerName();
		int    pos_ExecPlan      = _shortRstm.findColumn("ExecPlan");

		for (int r=0; r<_shortRstm.getRowCount(); r++)
		{
			String executableId = _shortRstm.getValueAsString(r, "EXECUTABLE_ID");
			String sqlText      = _shortRstm.getValueAsString(r, "SqlText");

			// The text is not recorded when CmStatements.sample.stmtText is false
			if (StringUtil.isNullOrBlank(sqlText))
				sqlText = "-- No SQL Text: it was not recorded (property CmStatements.sample.stmtText=false)";

			//--------------------------------------------------------------------------------
			// The execution plan(s) CmActiveStatements got while the statement was running (EXPLAIN_FROM_SECTION, by the same EXECUTABLE_ID)
			// The plan is written ONCE, in a hidden block (see writeMessageText): the links read it from there when they are clicked
			// It is also written as text (as db2exfmt shows it): that works in a mail too
			//--------------------------------------------------------------------------------
			List<String> plans = getExecutionPlans(conn, executableId);

			String planLinks   = "";
			String planTexts   = "";
			String firstPlanId = null;
			for (int p = 0; p < plans.size(); p++)
			{
				String planElementId = "db2plan_" + executableId + "_" + (p + 1);
				_planTexts.put(planElementId, plans.get(p));
				if (firstPlanId == null)
					firstPlanId = planElementId;

				String link = ShowplanLinkBuilder.buildViewPlanLinkHtml(dbxCentralBaseUrl, planElementId, sqlText, DbUtils.DB_PROD_NAME_DB2_LUW, reportSrvName, "",
						plans.size() == 1 ? "View Execution Plan" : "plan " + (p + 1), planLinks.isEmpty());
				if (StringUtil.hasValue(link))
					planLinks += (planLinks.isEmpty() ? "" : ", ") + (plans.size() > 1 && planLinks.isEmpty() ? link.replace("<a ", "View Execution Plan: <a ") : link);

				planTexts += "<details><summary>Execution Plan" + (plans.size() > 1 ? " " + (p + 1) : "") + " as text (from EXPLAIN_FROM_SECTION)</summary><xmp>" + Db2ExplainUtils.planJsonToText(plans.get(p)) + "</xmp></details>\n";
			}

			// No plan: SAY so (as the other DBMS vendors does), a missing link leaves the reader wondering why
			if (plans.isEmpty())
				planLinks = "Execution Plan <b>NOT Available</b> <i>(Db2Tune gets the plans of statements that runs for a while, and only when the explain tables exist for the login: see the tab 'Active Statements')</i>";
			_shortRstm.setValueAtWithOverride(plans.isEmpty() ? "--not-found--" : plans.size() == 1 ? "yes" : plans.size() + " plans", r, pos_ExecPlan);

			// "Get LLM Optimization Advice": with the (first) plan. What the tables look like is looked up when the link is clicked,
			// and the charts and numbers of the statement is read from the table in the "sparklines" cell (see SparklineHelper.getWorkloadHarvesterJs)
			String llmAdviceLink = LlmSqlContextBuilder.buildAdviceLinkHtmlJs(dbxCentralBaseUrl, sqlText, firstPlanId, DbUtils.DB_PROD_NAME_DB2_LUW, reportSrvName, "");

			// The problems Db2 counted for it
			String problems = "";
			if (_shortRstm.getValueAsLong(r, "SORT_OVERFLOWS__sum"     , true, 0L) > 0) problems += "&emsp; Sort overflows: <b>" + _shortRstm.getValueAsString(r, "SORT_OVERFLOWS__sum") + "</b>";
			if (_shortRstm.getValueAsLong(r, "LOCK_TIMEOUTS__sum"      , true, 0L) > 0) problems += "&emsp; Lock timeouts: <b>"  + _shortRstm.getValueAsString(r, "LOCK_TIMEOUTS__sum") + "</b>";
			if (_shortRstm.getValueAsLong(r, "DEADLOCKS__sum"          , true, 0L) > 0) problems += "&emsp; Deadlocks: <b>"      + _shortRstm.getValueAsString(r, "DEADLOCKS__sum") + "</b>";
			if (_shortRstm.getValueAsLong(r, "LOCK_ESCALS__sum"        , true, 0L) > 0) problems += "&emsp; Lock escalations: <b>" + _shortRstm.getValueAsString(r, "LOCK_ESCALS__sum") + "</b>";
			if (_shortRstm.getValueAsLong(r, "NUM_EXEC_WITH_ERROR__sum", true, 0L) > 0) problems += "&emsp; Errors: <b>"         + _shortRstm.getValueAsString(r, "NUM_EXEC_WITH_ERROR__sum") + "</b> (last SQLCODE " + _shortRstm.getValueAsString(r, "LAST_EXEC_ERROR") + ")";

			String executedIn = "Isolation: <code>" + StringEscapeUtils.escapeHtml4(StringUtil.nullToValue(_shortRstm.getValueAsString(r, "EFFECTIVE_ISOLATION"), "")) + "</code>"
			                  + ("S".equals(_shortRstm.getValueAsString(r, "SECTION_TYPE")) ? "&emsp; Static SQL in package: <code>" + StringEscapeUtils.escapeHtml4(_shortRstm.getValueAsString(r, "PACKAGE_SCHEMA") + "." + _shortRstm.getValueAsString(r, "PACKAGE_NAME")) + "</code>" : "")
			                  + problems;

			// The tables of the statement: from the plan when there is one (the real tables, also behind views and aliases), otherwise parsed from the text
			// (a table without a schema can be in any schema: the package cache does not say the default schema of the statement)
			Set<String> tableList = new LinkedHashSet<>();
			for (String plan : plans)
				tableList.addAll(getTablesOfPlan(plan));
			if (tableList.isEmpty())
			{
				for (String table : CmStatements.getTablesOfStmtText(sqlText))
				{
					String[] schemaAndTable = ObjectLookupInspectorDb2.splitSchemaAndTable(ObjectLookupInspectorDb2.ANY_SCHEMA, table);
					if ( ! ObjectLookupInspectorDb2.isSystemObject(schemaAndTable[0], schemaAndTable[1]) )
						tableList.add(table);
				}
			}

			// The formatted SQL Text, and the table information (it has the formatted SQL Text too)
			String formatted = tableList.isEmpty()
					? "\n<br>\n" + getFormattedSqlAsTooltipDiv(sqlText, "Hover or Click to see formatted SQL Text...", DbUtils.DB_PROD_NAME_DB2_LUW) + "\n"
					: getDbmsTableInformationForTables(conn, "", sqlText, tableList, DbUtils.DB_PROD_NAME_DB2_LUW);

			// Grab all the charts and numbers we defined in 'htp'
			String sparklines = htp.getHtmlTextForRow(r);

			srs.addRow(
					executableId,
					sparklines,
					"<xmp>" + sqlText + "</xmp>\n"
						+ executedIn + " <br>\n"
						+ "<br>\n" + planLinks
						+ (StringUtil.hasValue(planTexts) ? "\n" + planTexts : "")
						+ (StringUtil.hasValue(llmAdviceLink) ? "<br>\n" + llmAdviceLink : "")
						+ formatted);
		}

		try
		{
			_sqlTextRstm = createResultSetTableModel(srs, "Db2TopSqlText", null, false); // DO NOT TRUNCATE COLUMNS
			srs.close();
		}
		catch (SQLException ex)
		{
			setProblemException(ex);

			_sqlTextRstm = ResultSetTableModel.createEmpty("Db2TopSqlText");
			_logger.warn("Problems getting Top SQL TEXT: " + ex);
		}
	}
}
