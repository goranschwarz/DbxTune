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
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.h2.tools.SimpleResultSet;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.DictCompression;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.SparklineHelper;
import com.dbxtune.pcs.report.content.SparklineHelper.AggType;
import com.dbxtune.pcs.report.content.SparklineHelper.DataSource;
import com.dbxtune.pcs.report.content.SparklineHelper.SparkLineParams;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

public class OracleTopSql
extends OracleAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _sqlTextRstm;
	private List<String>        _miniChartJsList = new ArrayList<>();

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

		// Get a description of this section, and column names
		w.append(getSectionDescriptionHtml(_shortRstm, true));

		w.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
		w.append(toHtmlTable(_shortRstm));

		// The SQL Text, in a table of it's own (the texts are long)
		if (_sqlTextRstm != null)
		{
			w.append("<br>\n");
			w.append("<details open> \n");
			w.append("<summary>SQL Text for the above Statements (click to collapse) </summary> \n");
			w.append("<br>\n");
			w.append("SQL Text by SQL_ID, Row Count: " + _sqlTextRstm.getRowCount() + "<br>\n");
			w.append(toHtmlTable(_sqlTextRstm));
			w.append("\n");
			w.append("</details> \n");
		}

		// Write JavaScript code for the mini charts
		if (isFullMessageType())
		{
			for (String str : _miniChartJsList)
				w.append(str);
		}
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

		// The statements that are in the top ## on: elapsed time, CPU time or buffer gets
		String sql = getCmDiffColumnsAsSqlComment("CmSqlStats")
			    + "select * \n"
			    + "from ( \n"
			    + "    select \n"
			    + "         [SQL_ID] \n"
			    + "        ,max([CON_NAME])                                                                                 as [CON_NAME] \n"
			    + "        ,count(distinct [PLAN_HASH_VALUE])                                                               as [plan_count] \n"
			    + " \n"
			    + "        ,row_number() over (order by sum([ELAPSED_TIME]) desc)                                           as [rank_elapsed] \n"
			    + "        ,row_number() over (order by sum([CPU_TIME])     desc)                                           as [rank_cpu] \n"
			    + "        ,row_number() over (order by sum([BUFFER_GETS])  desc)                                           as [rank_gets] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                                        as [EXECUTIONS__chart] \n"
			    + "        ,sum([EXECUTIONS])                                                                               as [EXECUTIONS__sum] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                                                        as [ELAPSED_TIME_ms__chart] \n"
			    + "        ,CAST( sum([ELAPSED_TIME]) / 1000.0                                    as DECIMAL(20,0) )        as [ELAPSED_TIME_ms__sum] \n"
			    + "        ,CAST( sum([ELAPSED_TIME]) / 1000.0 / nullif(sum([EXECUTIONS]), 0)     as DECIMAL(20,1) )        as [ELAPSED_TIME_ms__per_exec] \n"
			    + " \n"
			    + "        ,CAST( sum([CPU_TIME])     / 1000.0                                    as DECIMAL(20,0) )        as [CPU_TIME_ms__sum] \n"
			    + "        ,CAST( sum([CPU_TIME])     / 1000.0 / nullif(sum([EXECUTIONS]), 0)     as DECIMAL(20,1) )        as [CPU_TIME_ms__per_exec] \n"
			    + " \n"
			    + "        ,CAST( sum([USER_IO_WAIT_TIME])     / 1000.0                           as DECIMAL(20,0) )        as [USER_IO_WAIT_ms__sum] \n"
			    + "        ,CAST( sum([APPLICATION_WAIT_TIME]) / 1000.0                           as DECIMAL(20,0) )        as [APPLICATION_WAIT_ms__sum] \n"
			    + "        ,CAST( sum([CONCURRENCY_WAIT_TIME]) / 1000.0                           as DECIMAL(20,0) )        as [CONCURRENCY_WAIT_ms__sum] \n"
			    + " \n"
			    + "        ,sum([BUFFER_GETS])                                                                              as [BUFFER_GETS__sum] \n"
			    + "        ,CAST( sum([BUFFER_GETS])    * 1.0 / nullif(sum([EXECUTIONS]), 0)      as DECIMAL(20,1) )        as [BUFFER_GETS__per_exec] \n"
			    + "        ,sum([DISK_READS])                                                                               as [DISK_READS__sum] \n"
			    + "        ,CAST( sum([DISK_READS])     * 1.0 / nullif(sum([EXECUTIONS]), 0)      as DECIMAL(20,1) )        as [DISK_READS__per_exec] \n"
			    + "        ,sum([ROWS_PROCESSED])                                                                           as [ROWS_PROCESSED__sum] \n"
			    + "        ,CAST( sum([ROWS_PROCESSED]) * 1.0 / nullif(sum([EXECUTIONS]), 0)      as DECIMAL(20,1) )        as [ROWS_PROCESSED__per_exec] \n"
			    + "        ,sum([SORTS])                                                                                    as [SORTS__sum] \n"
			    + "        ,sum([PARSE_CALLS])                                                                              as [PARSE_CALLS__sum] \n"
			    + " \n"
			    + "        ,count(distinct [SessionSampleTime])                                                             as [samples_count] \n"
			    + "        ,min([SessionSampleTime])                                                                        as [SessionSampleTime__min] \n"
			    + "        ,max([SessionSampleTime])                                                                        as [SessionSampleTime__max] \n"
			    + "        ,max([" + col_SQL_FULLTEXT + "])                                                                 as [SQL_FULLTEXT] \n"
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
				"<b>Note</b>: The collector only records the statements with most buffer gets (property <code>CmSqlStats.sample.topRows</code>), " +
				"and the first sample where a statement is seen is not counted (those counters are 'since the statement was loaded'). So a statement that was executed only once can be missing.<br>" +
				"");

		_shortRstm.setColumnDescription("SQL_ID"                  , "Oracle's id of the statement (a hash of the SQL Text)");
		_shortRstm.setColumnDescription("CON_NAME"                , "Container (PDB) name. If the statement was executed in several containers: one of them.");
		_shortRstm.setColumnDescription("plan_count"              , "How many different execution plans (PLAN_HASH_VALUE) the statement had during the period. More than 1 = the plan changed.");
		_shortRstm.setColumnDescription("rank_elapsed"            , "Place in the list when it's sorted on elapsed time (1 = most)");
		_shortRstm.setColumnDescription("rank_cpu"                , "Place in the list when it's sorted on CPU time (1 = most)");
		_shortRstm.setColumnDescription("rank_gets"               , "Place in the list when it's sorted on buffer gets (1 = most)");
		_shortRstm.setColumnDescription("EXECUTIONS__chart"       , "Number of executions, in 10 minute chunks");
		_shortRstm.setColumnDescription("EXECUTIONS__sum"         , "Number of executions");
		_shortRstm.setColumnDescription("ELAPSED_TIME_ms__chart"  , "Elapsed time in milliseconds, in 10 minute chunks");
		_shortRstm.setColumnDescription("ELAPSED_TIME_ms__sum"    , "Elapsed time (CPU + waits) for all executions, in milliseconds");
		_shortRstm.setColumnDescription("ELAPSED_TIME_ms__per_exec", "Elapsed time for ONE execution, in milliseconds");
		_shortRstm.setColumnDescription("CPU_TIME_ms__sum"        , "CPU time for all executions, in milliseconds");
		_shortRstm.setColumnDescription("CPU_TIME_ms__per_exec"   , "CPU time for ONE execution, in milliseconds");
		_shortRstm.setColumnDescription("USER_IO_WAIT_ms__sum"    , "Time waited on disk IO, in milliseconds");
		_shortRstm.setColumnDescription("APPLICATION_WAIT_ms__sum", "Time waited in the Wait Class 'Application', in milliseconds: mostly row locks held by other sessions");
		_shortRstm.setColumnDescription("CONCURRENCY_WAIT_ms__sum", "Time waited in the Wait Class 'Concurrency', in milliseconds: latches, buffer busy, library cache locks");
		_shortRstm.setColumnDescription("BUFFER_GETS__sum"        , "Logical reads (blocks read from the buffer cache) for all executions");
		_shortRstm.setColumnDescription("BUFFER_GETS__per_exec"   , "Logical reads for ONE execution");
		_shortRstm.setColumnDescription("DISK_READS__sum"         , "Physical reads (blocks read from disk) for all executions");
		_shortRstm.setColumnDescription("DISK_READS__per_exec"    , "Physical reads for ONE execution");
		_shortRstm.setColumnDescription("ROWS_PROCESSED__sum"     , "Rows returned or changed, for all executions");
		_shortRstm.setColumnDescription("ROWS_PROCESSED__per_exec", "Rows returned or changed, for ONE execution");
		_shortRstm.setColumnDescription("SORTS__sum"              , "Number of sorts");
		_shortRstm.setColumnDescription("PARSE_CALLS__sum"        , "Number of parse calls");
		_shortRstm.setColumnDescription("samples_count"           , "In how many collector samples the statement did something");
		_shortRstm.setColumnDescription("SessionSampleTime__min"  , "First sample where the statement did something");
		_shortRstm.setColumnDescription("SessionSampleTime__max"  , "Last sample where the statement did something");

		//--------------------------------------------------------------------------------
		// The SQL Text goes into a table of it's own: SQL_ID + some numbers + the text
		//--------------------------------------------------------------------------------
		SimpleResultSet srs = new SimpleResultSet();
		srs.addColumn("SQL_ID",                    Types.VARCHAR,       30, 0);
		srs.addColumn("EXECUTIONS__sum",           Types.BIGINT,         0, 0);
		srs.addColumn("ELAPSED_TIME_ms__sum",      Types.BIGINT,         0, 0);
		srs.addColumn("ELAPSED_TIME_ms__per_exec", Types.DECIMAL,       20, 1);
		srs.addColumn("SQL_FULLTEXT",              Types.VARCHAR, 1024*128, 0);

		for (int r=0; r<_shortRstm.getRowCount(); r++)
		{
			String sqlText = _shortRstm.getValueAsString(r, "SQL_FULLTEXT");

			// When using Dictionary Compression the column is just a hashId: get the real text from the lookup table
			if (hasDictCompCols)
			{
				try
				{
					sqlText = DictCompression.getValueForHashId(conn, null, "CmSqlStats", "SQL_FULLTEXT", sqlText);
				}
				catch (SQLException ex)
				{
					sqlText = "Problems getting Dictionary Compressed column for tabName='CmSqlStats', colName='SQL_FULLTEXT', hashId='" + sqlText + "'.";
				}
			}

			srs.addRow(
					_shortRstm.getValueAsString(r, "SQL_ID"),
					_shortRstm.getValueAsLong  (r, "EXECUTIONS__sum"),
					_shortRstm.getValueAsLong  (r, "ELAPSED_TIME_ms__sum"),
					_shortRstm.getValueAsBigDecimal(r, "ELAPSED_TIME_ms__per_exec"),
					"<xmp>" + sqlText + "</xmp>");
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

		// The text (or it's hash id) is not needed in the first table any more
		_shortRstm.removeColumnNoCase("SQL_FULLTEXT");

		//--------------------------------------------------------------------------------
		// Mini Charts
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

		_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm,
				SparkLineParams.create       (DataSource.CounterModel)
				.setHtmlChartColumnName      ("ELAPSED_TIME_ms__chart")
				.setHtmlWhereKeyColumnName   ("SQL_ID")
				.setDbmsTableName            ("CmSqlStats_diff")
				.setDbmsSampleTimeColumnName ("SessionSampleTime")
				.setDbmsDataValueColumnName  ("sum([ELAPSED_TIME]) / 1000.0").setGroupDataAggregationType(AggType.USER_PROVIDED)
				.setDbmsWhereKeyColumnName   ("SQL_ID")
				.setDbmsExtraWhereClause     (sql_and_skipNewDiffRateRows.trim())
				.setDecimalScale             (0)
				.setSparklineTooltipPostfix  ("Elapsed time in milliseconds in below period")
				.validate()));
	}
}
