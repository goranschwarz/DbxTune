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
package com.dbxtune.pcs.report.content.mysql;

import java.io.IOException;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Types;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.h2.tools.SimpleResultSet;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.gui.ResultSetTableModel.TableStringRenderer;
import com.dbxtune.pcs.DictCompression;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
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

/**
 * The statements that used most time, read most rows, or were executed most (from CmStatements, the digest table). Built on OracleTopSql.
 */
public class MySqlTopSql
extends MySqlAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _sqlTextRstm;
	private List<String>        _miniChartJsList = new ArrayList<>();

	public MySqlTopSql(DailySummaryReportAbstract reportingInstance)
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
								+ "data-objectname='" + rstm.getValueAsString(row, "Digest") + "' "
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
			w.append("SQL Text by 'Digest', Row Count: " + _sqlTextRstm.getRowCount() + "\n");
			w.append(toHtmlTable(_sqlTextRstm));
			w.append("\n");
			w.append("</details> \n");
		}

		if (isFullMessageType())
		{
			// Write JavaScript code for the mini charts
			for (String str : _miniChartJsList)
				w.append(str);
		}
	}

	/** What is shown when the "SqlText" cell of the first table is clicked: some numbers of the row, and the SQL Text. (double quotes must be avoided or escaped) */
	private String getTooltipForSqlText(ResultSetTableModel rstm, int row)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("-- Some columns extracted from current row.\n");
		sb.append("-----------------------------------------------------------------------------------------------\n");
		sb.append("-- Digest:                    ").append( rstm.getValueAsString(row, "Digest"                 ) ).append("\n");
		sb.append("-- SchemaName:                ").append( rstm.getValueAsString(row, "SchemaName"             ) ).append("\n");
		sb.append("-- SessionSampleTime__min:    ").append( rstm.getValueAsString(row, "SessionSampleTime__min" ) ).append("\n");
		sb.append("-- SessionSampleTime__max:    ").append( rstm.getValueAsString(row, "SessionSampleTime__max" ) ).append("\n");
		sb.append("-- Duration:                  ").append( rstm.getValueAsString(row, "Duration"               ) ).append("\n");

		sb.append("-- ExecCount__sum:            ").append( num(rstm.getValueAsBigDecimal(row, "ExecCount__sum"            )) ).append("\n");
		sb.append("-- TotalTimeMs__sum:          ").append( num(rstm.getValueAsBigDecimal(row, "TotalTimeMs__sum"          )) ).append("  (in milli seconds), and in (HH:MM:SS.sss) ").append( TimeUtils.msToTimeStrLong(rstm.getValueAsLong(row, "TotalTimeMs__sum")) ).append(" \n");
		sb.append(">> TotalTimeMs__per_exec:     ").append( num(rstm.getValueAsBigDecimal(row, "TotalTimeMs__per_exec"     )) ).append("\n");
		sb.append("-- LockTimeMs__sum:           ").append( num(rstm.getValueAsBigDecimal(row, "LockTimeMs__sum"           )) ).append("\n");
		sb.append("-- CpuTimeMs__sum:            ").append( num(rstm.getValueAsBigDecimal(row, "CpuTimeMs__sum"            )) ).append("\n");
		sb.append("-- RowsExamined__sum:         ").append( num(rstm.getValueAsBigDecimal(row, "RowsExamined__sum"         )) ).append("\n");
		sb.append(">> RowsExamined__per_exec:    ").append( num(rstm.getValueAsBigDecimal(row, "RowsExamined__per_exec"    )) ).append("\n");
		sb.append("-- RowsSent__sum:             ").append( num(rstm.getValueAsBigDecimal(row, "RowsSent__sum"             )) ).append("\n");
		sb.append("-- RowsAffected__sum:         ").append( num(rstm.getValueAsBigDecimal(row, "RowsAffected__sum"         )) ).append("\n");
		sb.append("-- TmpDiskTables__sum:        ").append( num(rstm.getValueAsBigDecimal(row, "TmpDiskTables__sum"        )) ).append("\n");
		sb.append("-- NoIndexUsed__sum:          ").append( num(rstm.getValueAsBigDecimal(row, "NoIndexUsed__sum"          )) ).append("\n");
		sb.append("-- Errors__sum:               ").append( num(rstm.getValueAsBigDecimal(row, "Errors__sum"               )) ).append("\n");
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
		return "Top SQL Statements (order by: TotalTimeMs, origin: CmStatements / performance_schema.events_statements_summary_by_digest)";
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

		// The texts are normally stored "Dictionary Compressed": the column has a hash id, and the text is in a table of it's own
		boolean hasDictCompCols = false;
		try {
			hasDictCompCols = DictCompression.hasCompressedColumnNames(conn, null, "CmStatements_diff");
		} catch (SQLException ex) {
			_logger.error("Problems checking for Dictionary Compressed Columns in table 'CmStatements_diff'.", ex);
		}
		String col_DigestText      = hasDictCompCols ? "DigestText$dcc$"      : "DigestText";
		String col_QuerySampleText = hasDictCompCols ? "QuerySampleText$dcc$" : "QuerySampleText";

		// A statement that is seen for the first time has it's counters "since the server started" (and not "since the previous sample"): do not count those rows
		// (the collector makes it a normal row if the statement was first seen after the previous sample)
		String sql_and_skipNewDiffRateRows = "  and " + conn.toBitAnd("[CmRowState]", CountersModel.ROW_STATE__IS_DIFF_OR_RATE_ROW) + " = 0 \n";

		// "per execution" over the whole period: NULL when no execution was counted in the period
		String perExec = " / nullif(sum([ExecCount]), 0)";

		// The statements that are in the top ## on: time, rows examined or executions
		// The same digest in several schemas: one row (the counters are summed), SchemaName is one of them
		String sql = getCmDiffColumnsAsSqlComment("CmStatements")
			    + "select * \n"
			    + "from ( \n"
			    + "    select \n"
			    + "         [Digest] \n"
			    + "        ,max([SchemaName])                                                                      as [SchemaName] \n"
			    + "        ,count(distinct [SchemaName])                                                           as [schema_count] \n"
			    + "        ,max([" + col_DigestText + "])                                                          as [SqlText] \n"
			    + "        ,max([" + col_QuerySampleText + "])                                                     as [SampleText] \n"
			    + " \n"
			    + "        ,row_number() over (order by sum([TotalTimeMs])  desc)                                  as [rank_time] \n"
			    + "        ,row_number() over (order by sum([RowsExamined]) desc)                                  as [rank_rows] \n"
			    + "        ,row_number() over (order by sum([ExecCount])    desc)                                  as [rank_execs] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                               as [ExecCount__chart] \n"
			    + "        ,sum([ExecCount])                                                                       as [ExecCount__sum] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                               as [TotalTimeMs__chart] \n"
			    + "        ,CAST( sum([TotalTimeMs])                           as DECIMAL(20,0) )                  as [TotalTimeMs__sum] \n"
			    + "        ,CAST( sum([TotalTimeMs]) " + perExec + "           as DECIMAL(20,1) )                  as [TotalTimeMs__per_exec] \n"
			    + "        ,max([MaxTimeMs])                                                                       as [MaxTimeMs__max] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                               as [LockTimeMs__chart] \n"
			    + "        ,CAST( sum([LockTimeMs])                            as DECIMAL(20,0) )                  as [LockTimeMs__sum] \n"
			    + "        ,CAST( sum([LockTimeMs])  " + perExec + "           as DECIMAL(20,1) )                  as [LockTimeMs__per_exec] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                               as [CpuTimeMs__chart] \n"
			    + "        ,CAST( sum([CpuTimeMs])                             as DECIMAL(20,0) )                  as [CpuTimeMs__sum] \n"
			    + "        ,CAST( sum([CpuTimeMs])   " + perExec + "           as DECIMAL(20,1) )                  as [CpuTimeMs__per_exec] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                               as [RowsExamined__chart] \n"
			    + "        ,sum([RowsExamined])                                                                    as [RowsExamined__sum] \n"
			    + "        ,CAST( sum([RowsExamined]) * 1.0 " + perExec + "    as DECIMAL(20,1) )                  as [RowsExamined__per_exec] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                               as [RowsSent__chart] \n"
			    + "        ,sum([RowsSent])                                                                        as [RowsSent__sum] \n"
			    + "        ,CAST( sum([RowsSent])     * 1.0 " + perExec + "    as DECIMAL(20,1) )                  as [RowsSent__per_exec] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                               as [RowsAffected__chart] \n"
			    + "        ,sum([RowsAffected])                                                                    as [RowsAffected__sum] \n"
			    + "        ,CAST( sum([RowsAffected]) * 1.0 " + perExec + "    as DECIMAL(20,1) )                  as [RowsAffected__per_exec] \n"
			    + " \n"
			    + "        ,sum([TmpTables])                                                                       as [TmpTables__sum] \n"
			    + "        ,sum([TmpDiskTables])                                                                   as [TmpDiskTables__sum] \n"
			    + "        ,sum([SortRows])                                                                        as [SortRows__sum] \n"
			    + "        ,sum([SortMergePasses])                                                                 as [SortMergePasses__sum] \n"
			    + "        ,sum([FullScans])                                                                       as [FullScans__sum] \n"
			    + "        ,sum([FullJoins])                                                                       as [FullJoins__sum] \n"
			    + "        ,sum([NoIndexUsed])                                                                     as [NoIndexUsed__sum] \n"
			    + "        ,sum([NoGoodIndexUsed])                                                                 as [NoGoodIndexUsed__sum] \n"
			    + "        ,sum([Errors])                                                                          as [Errors__sum] \n"
			    + "        ,sum([Warnings])                                                                        as [Warnings__sum] \n"
			    + " \n"
			    + "        ,count(distinct [SessionSampleTime])                                                    as [samples_count] \n"
			    + "        ,min([SessionSampleTime])                                                               as [SessionSampleTime__min] \n"
			    + "        ,max([SessionSampleTime])                                                               as [SessionSampleTime__max] \n"
			    + "        ,cast('' as varchar(30))                                                                as [Duration] \n"
			    + "    from [CmStatements_diff] \n"
			    + "    where ([ExecCount] > 0 or [TotalTimeMs] > 0) \n"   // only samples where the statement did something
			    + "  " + sql_and_skipNewDiffRateRows
			    + "  " + getReportPeriodSqlWhere()
			    + "    group by [Digest] \n"
			    + ") x \n"
			    + "where [rank_time]  <= " + topRows + " \n"
			    + "   or [rank_rows]  <= " + topRows + " \n"
			    + "   or [rank_execs] <= " + topRows + " \n"
			    + "order by [rank_time] \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, true, "MySqlTopSql", false); // false = do not truncate (the SQL Text)

		if (_shortRstm.getRowCount() == 0)
			return;

		// Highlight sort column
		_shortRstm.setHighlightSortColumns("TotalTimeMs__sum");

		_shortRstm.setDescription(
				"The SQL Statements that used most resources during the period (from the table <code>CmStatements_diff</code>).<br>" +
				"A statement is the <b>normalized</b> statement (the 'digest': the values are replaced by '?'), so all executions of the same statement with other values are counted together.<br>" +
				"It is listed if it is in the top " + topRows + " on <b>time</b>, <b>rows examined</b> or <b>executions</b> (the three <code>rank_</code> columns says where it's placed in each).<br>" +
				"The <code>__chart</code> columns shows <b>when</b> during the period, in 10 minute chunks. <code>ExecCount__chart</code> is the number of executions, the others are the value <b>per execution</b> " +
				"(a chunk where the statement ran, but no execution ended, is counted as one execution: MySQL counts an execution when it ends).<br>" +
				"<b>Note</b>: The collector only records the statements that ran in the last 30 minutes, and of them the ones with most time (properties <code>CmStatements.sample.lastXminutes</code> and <code>CmStatements.sample.topRows</code>), every 5 minutes. " +
				"A statement that is seen for the first time is only counted from the next sample, unless it was new since the previous sample.<br>" +
				"");

		_shortRstm.setColumnDescription("Digest"                , "MySQL's id of the normalized statement (a hash of the text with the values replaced by '?'). Empty = the digest table was full, and the statements that did not fit are counted here.");
		_shortRstm.setColumnDescription("SchemaName"            , "The default schema of the connection (USE ...) when it ran. If it ran in several schemas: one of them.");
		_shortRstm.setColumnDescription("schema_count"          , "In how many schemas the statement ran (each schema has its own row in MySQL, they are summed here)");
		_shortRstm.setColumnDescription("SqlText"               , "The normalized SQL Text: click to see it (it is also in the table below)");
		_shortRstm.setColumnDescription("SampleText"            , "An example of the statement, with the values it had (QUERY_SAMPLE_TEXT)");
		_shortRstm.setColumnDescription("rank_time"             , "Place in the list when it's sorted on time (1 = most)");
		_shortRstm.setColumnDescription("rank_rows"             , "Place in the list when it's sorted on rows examined (1 = most)");
		_shortRstm.setColumnDescription("rank_execs"            , "Place in the list when it's sorted on executions (1 = most)");
		_shortRstm.setColumnDescription("ExecCount__chart"      , "Chart: number of executions, in 10 minute chunks\nSo you can see WHEN the statement was executed.");
		_shortRstm.setColumnDescription("ExecCount__sum"        , "Number of executions");
		_shortRstm.setColumnDescription("TotalTimeMs__chart"    , "Chart: time for ONE execution in milliseconds, in 10 minute chunks\nSo you can see when the statement was slower than normal.");
		_shortRstm.setColumnDescription("TotalTimeMs__sum"      , "Time for all executions, in milliseconds");
		_shortRstm.setColumnDescription("TotalTimeMs__per_exec" , "Time for ONE execution, in milliseconds");
		_shortRstm.setColumnDescription("MaxTimeMs__max"        , "The longest execution, in milliseconds. NOTE: since the server was started (or the digest table was emptied), not only in the period");
		_shortRstm.setColumnDescription("LockTimeMs__chart"     , "Chart: time waiting for locks for ONE execution in milliseconds, in 10 minute chunks\nSo you can see when the statement was blocked by other connections.");
		_shortRstm.setColumnDescription("LockTimeMs__sum"       , "Time waiting for locks, in milliseconds: table locks, and from MySQL 8.0.28 also InnoDB row locks (it is a part of TotalTimeMs)");
		_shortRstm.setColumnDescription("LockTimeMs__per_exec"  , "Time waiting for locks for ONE execution, in milliseconds");
		_shortRstm.setColumnDescription("CpuTimeMs__chart"      , "Chart: CPU time for ONE execution in milliseconds, in 10 minute chunks");
		_shortRstm.setColumnDescription("CpuTimeMs__sum"        , "CPU time, in milliseconds. Empty when the performance_schema consumer 'events_statements_cpu' is OFF (the default)");
		_shortRstm.setColumnDescription("CpuTimeMs__per_exec"   , "CPU time for ONE execution, in milliseconds");
		_shortRstm.setColumnDescription("RowsExamined__chart"   , "Chart: rows examined by ONE execution, in 10 minute chunks\nA step up = the statement started to do more work for the same thing (often a changed execution plan, or a growing table without an index).");
		_shortRstm.setColumnDescription("RowsExamined__sum"     , "Rows read by the storage engine, for all executions. Much more than RowsSent can mean a missing index.");
		_shortRstm.setColumnDescription("RowsExamined__per_exec", "Rows examined by ONE execution");
		_shortRstm.setColumnDescription("RowsSent__chart"       , "Chart: rows returned by ONE execution, in 10 minute chunks");
		_shortRstm.setColumnDescription("RowsSent__sum"         , "Rows returned to the client, for all executions");
		_shortRstm.setColumnDescription("RowsSent__per_exec"    , "Rows returned by ONE execution");
		_shortRstm.setColumnDescription("RowsAffected__chart"   , "Chart: rows changed by ONE execution, in 10 minute chunks");
		_shortRstm.setColumnDescription("RowsAffected__sum"     , "Rows changed (insert/update/delete), for all executions");
		_shortRstm.setColumnDescription("RowsAffected__per_exec", "Rows changed by ONE execution");
		_shortRstm.setColumnDescription("TmpTables__sum"        , "Internal temporary tables");
		_shortRstm.setColumnDescription("TmpDiskTables__sum"    , "Internal temporary tables that had to be written to disk");
		_shortRstm.setColumnDescription("SortRows__sum"         , "Rows sorted");
		_shortRstm.setColumnDescription("SortMergePasses__sum"  , "Sorts that did not fit in sort_buffer_size (they used a file)");
		_shortRstm.setColumnDescription("FullScans__sum"        , "Full scans of the first table in a join (or of the only table)");
		_shortRstm.setColumnDescription("FullJoins__sum"        , "Joins without an index");
		_shortRstm.setColumnDescription("NoIndexUsed__sum"      , "Executions that read a table without an index");
		_shortRstm.setColumnDescription("NoGoodIndexUsed__sum"  , "Executions where no good index was found");
		_shortRstm.setColumnDescription("Errors__sum"           , "Executions that ended with an error");
		_shortRstm.setColumnDescription("Warnings__sum"         , "Warnings of the executions");
		_shortRstm.setColumnDescription("samples_count"         , "In how many collector samples the statement did something");
		_shortRstm.setColumnDescription("SessionSampleTime__min", "First sample where the statement did something");
		_shortRstm.setColumnDescription("SessionSampleTime__max", "Last sample where the statement did something");
		_shortRstm.setColumnDescription("Duration"              , "HH:MM:SS between 'SessionSampleTime__min' and 'SessionSampleTime__max'");

		setDurationColumn(_shortRstm, "SessionSampleTime__min", "SessionSampleTime__max", "Duration");

		// When using Dictionary Compression the column is just a hashId: get the real text from the lookup table
		if (hasDictCompCols)
		{
			updateDictionaryCompressedColumn(_shortRstm, conn, null, "CmStatements", "DigestText"     , "SqlText");
			updateDictionaryCompressedColumn(_shortRstm, conn, null, "CmStatements", "QuerySampleText", "SampleText");
		}

		//--------------------------------------------------------------------------------
		// Mini Charts: one for every measure. They are also copied to the "details" table below
		//--------------------------------------------------------------------------------
		_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm,
				SparkLineParams.create       (DataSource.CounterModel)
				.setHtmlChartColumnName      ("ExecCount__chart")
				.setHtmlWhereKeyColumnName   ("Digest")
				.setDbmsTableName            ("CmStatements_diff")
				.setDbmsSampleTimeColumnName ("SessionSampleTime")
				.setDbmsDataValueColumnName  ("ExecCount")
				.setDbmsWhereKeyColumnName   ("Digest")
				.setDbmsExtraWhereClause     (sql_and_skipNewDiffRateRows.trim())
				.setSparklineTooltipPostfix  ("Number of executions in below period")
				.validate()));

		// The others are "per execution" in the 10 minutes
		// A chunk where the statement ran, but no execution ended, is counted as ONE execution (MySQL counts an execution when it ends)
		String execs = "(case when sum([ExecCount]) > 0 then sum([ExecCount]) else 1 end)";

		// {chart column, the value, what the tooltip says}
		String[][] charts = new String[][] {
			{"TotalTimeMs__chart" , "sum([TotalTimeMs])        / " + execs, "Average time in milliseconds per execution, for below period"},
			{"LockTimeMs__chart"  , "sum([LockTimeMs])         / " + execs, "Average lock wait time in milliseconds per execution, for below period"},
			{"CpuTimeMs__chart"   , "sum([CpuTimeMs])          / " + execs, "Average CPU time in milliseconds per execution, for below period"},
			{"RowsExamined__chart", "sum([RowsExamined]) * 1.0 / " + execs, "Average rows examined per execution, for below period"},
			{"RowsSent__chart"    , "sum([RowsSent])     * 1.0 / " + execs, "Average rows returned per execution, for below period"},
			{"RowsAffected__chart", "sum([RowsAffected]) * 1.0 / " + execs, "Average rows changed per execution, for below period"},
		};

		for (String[] chart : charts)
		{
			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm,
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      (chart[0])
					.setHtmlWhereKeyColumnName   ("Digest")
					.setDbmsTableName            ("CmStatements_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  (chart[1]).setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   ("Digest")
					.setDbmsExtraWhereClause     (sql_and_skipNewDiffRateRows.trim())
					.setSparklineTooltipPostfix  (chart[2])
					.validate()));
		}

		//--------------------------------------------------------------------------------
		// The "details" table: Digest + the charts and numbers of the statement (as a table in the cell) + the SQL Text
		//--------------------------------------------------------------------------------
		SimpleResultSet srs = new SimpleResultSet();
		srs.addColumn("Digest",     Types.VARCHAR,       64, 0);
		srs.addColumn("sparklines", Types.VARCHAR,      512, 0);
		srs.addColumn("query",      Types.VARCHAR, 1024*128, 0);

		ColumnCopyRender msToHMS    = HtmlTableProducer.MS_TO_HMS;
		ColumnCopyRender oneDecimal = HtmlTableProducer.ONE_DECIMAL;

		HtmlTableProducer htp = new HtmlTableProducer(_shortRstm, "dsr-sub-table-chart");
		htp.setTableHeaders("Charts at 10 minute interval", "Total;style='text-align:right!important'", "Avg per exec;style='text-align:right!important'", "");
		htp.add("exec-cnt"    , new ColumnCopyRow().add( new ColumnCopyDef("ExecCount__chart"    ) ).add(new ColumnCopyDef("ExecCount__sum").setColBold()        ).addEmptyCol()                                                                 .addEmptyCol() );
		htp.add("exec-time"   , new ColumnCopyRow().add( new ColumnCopyDef("TotalTimeMs__chart"  ) ).add(new ColumnCopyDef("TotalTimeMs__sum" , msToHMS)       ).add(new ColumnCopyDef("TotalTimeMs__per_exec" , oneDecimal).setColBold()).add(new ColumnStatic("ms"  )) );
		htp.add("lock-time"   , new ColumnCopyRow().add( new ColumnCopyDef("LockTimeMs__chart"   ) ).add(new ColumnCopyDef("LockTimeMs__sum"  , msToHMS)       ).add(new ColumnCopyDef("LockTimeMs__per_exec"  , oneDecimal).setColBold()).add(new ColumnStatic("ms"  )) );
		htp.add("cpu-time"    , new ColumnCopyRow().add( new ColumnCopyDef("CpuTimeMs__chart"    ) ).add(new ColumnCopyDef("CpuTimeMs__sum"   , msToHMS)       ).add(new ColumnCopyDef("CpuTimeMs__per_exec"   , oneDecimal).setColBold()).add(new ColumnStatic("ms"  )) );
		htp.add("rows-exam"   , new ColumnCopyRow().add( new ColumnCopyDef("RowsExamined__chart" ) ).add(new ColumnCopyDef("RowsExamined__sum"           )    ).add(new ColumnCopyDef("RowsExamined__per_exec", oneDecimal).setColBold()).add(new ColumnStatic("rows")) );
		htp.add("rows-sent"   , new ColumnCopyRow().add( new ColumnCopyDef("RowsSent__chart"     ) ).add(new ColumnCopyDef("RowsSent__sum"               )    ).add(new ColumnCopyDef("RowsSent__per_exec"    , oneDecimal).setColBold()).add(new ColumnStatic("rows")) );
		htp.add("rows-changed", new ColumnCopyRow().add( new ColumnCopyDef("RowsAffected__chart" ) ).add(new ColumnCopyDef("RowsAffected__sum"           )    ).add(new ColumnCopyDef("RowsAffected__per_exec", oneDecimal).setColBold()).add(new ColumnStatic("rows")) );
		htp.validate();

		// Some rows are only shown when the statement has something there: CPU time is only recorded with the consumer 'events_statements_cpu', and a SELECT changes no rows
		htp.setRowFilter(new HtmlTableProducer.RowFilter()
		{
			@Override
			public boolean include(ResultSetTableModel rstm, int rstmRow, String rowKey)
			{
				if ("cpu-time"    .equals(rowKey)) return rstm.getValueAsDouble(rstmRow, "CpuTimeMs__sum"   , true, 0d) > 0;
				if ("lock-time"   .equals(rowKey)) return rstm.getValueAsDouble(rstmRow, "LockTimeMs__sum"  , true, 0d) > 0;
				if ("rows-changed".equals(rowKey)) return rstm.getValueAsDouble(rstmRow, "RowsAffected__sum", true, 0d) > 0;
				return true;
			}
		});

		for (int r=0; r<_shortRstm.getRowCount(); r++)
		{
			String digest     = _shortRstm.getValueAsString(r, "Digest");
			String sqlText    = _shortRstm.getValueAsString(r, "SqlText");
			String sampleText = _shortRstm.getValueAsString(r, "SampleText");
			String schemaName = _shortRstm.getValueAsString(r, "SchemaName");

			// The digest table was full: the statements that did not fit are counted in a row without a digest and without a text
			if (StringUtil.isNullOrBlank(sqlText))
				sqlText = "-- No SQL Text: this row is all the statements that did not fit in the digest table (performance_schema_digests_size)";

			// Who and where: the schema it ran in, and the problems MySQL counted for it
			String problems = "";
			if (_shortRstm.getValueAsLong(r, "NoIndexUsed__sum"    , true, 0L) > 0) problems += "&emsp; No index used: <b>" + _shortRstm.getValueAsString(r, "NoIndexUsed__sum") + "</b> times";
			if (_shortRstm.getValueAsLong(r, "TmpDiskTables__sum"  , true, 0L) > 0) problems += "&emsp; Temp tables on disk: <b>" + _shortRstm.getValueAsString(r, "TmpDiskTables__sum") + "</b>";
			if (_shortRstm.getValueAsLong(r, "SortMergePasses__sum", true, 0L) > 0) problems += "&emsp; Sort merge passes: <b>" + _shortRstm.getValueAsString(r, "SortMergePasses__sum") + "</b>";
			if (_shortRstm.getValueAsLong(r, "Errors__sum"         , true, 0L) > 0) problems += "&emsp; Errors: <b>" + _shortRstm.getValueAsString(r, "Errors__sum") + "</b>";

			String executedIn = (StringUtil.hasValue(schemaName) ? "Schema: <code>" + StringEscapeUtils.escapeHtml4(schemaName) + "</code>" : "Schema: <i>none (no USE)</i>")
			                  + problems;

			// An example with the values (QUERY_SAMPLE_TEXT): closed, it can be long
			String sample = "";
			if (StringUtil.hasValue(sampleText))
				sample = "<details><summary>An example of the statement, with its values</summary><xmp>" + sampleText + "</xmp></details>\n";

			// The formatted SQL Text (the table information comes when MySqlTune looks up DDL)
			String formatted = "\n<br>\n" + getFormattedSqlAsTooltipDiv(sqlText, "Hover or Click to see formatted SQL Text...", DbUtils.DB_PROD_NAME_MYSQL) + "\n";

			// Grab all the charts and numbers we defined in 'htp'
			String sparklines = htp.getHtmlTextForRow(r);

			srs.addRow(
					digest,
					sparklines,
					"<xmp>" + sqlText + "</xmp>\n"
						+ executedIn + " <br>\n"
						+ sample
						+ formatted);
		}

		try
		{
			_sqlTextRstm = createResultSetTableModel(srs, "MySqlTopSqlText", null, false); // DO NOT TRUNCATE COLUMNS
			srs.close();
		}
		catch (SQLException ex)
		{
			setProblemException(ex);

			_sqlTextRstm = ResultSetTableModel.createEmpty("MySqlTopSqlText");
			_logger.warn("Problems getting Top SQL TEXT: " + ex);
		}
	}
}
