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
import java.util.ArrayList;
import java.util.List;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.SparklineHelper;
import com.dbxtune.pcs.report.content.SparklineHelper.AggType;
import com.dbxtune.pcs.report.content.SparklineHelper.DataSource;
import com.dbxtune.pcs.report.content.SparklineHelper.SparkLineParams;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

/**
 * The tables that the server spent most time on (reading and writing rows, and their file IO), from CmSysTableStats (sys.x$schema_table_statistics).
 */
public class MySqlTopTableAccess
extends MySqlAbstract
{
	private ResultSetTableModel _shortRstm;
	private List<String>        _miniChartJsList = new ArrayList<>();

	public MySqlTopTableAccess(DailySummaryReportAbstract reportingInstance)
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

		if (isFullMessageType())
		{
			// Write JavaScript code for the mini charts
			for (String str : _miniChartJsList)
				w.append(str);
		}
	}

	@Override
	public String getSubject()
	{
		return "Top Table Access: the tables the server spent most time on (order by: total_latency_sec, origin: CmSysTableStats / sys.x$schema_table_statistics)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}

	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmSysTableStats_diff" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		int topRows = getTopRows();

		// A table that is seen for the first time has it's counters "since the server started": do not count those rows
		String sql_and_skipNewDiffRateRows = "  and " + conn.toBitAnd("[CmRowState]", CountersModel.ROW_STATE__IS_DIFF_OR_RATE_ROW) + " = 0 \n";

		// The latencies are in picoseconds: / 1000000000000 = seconds
		String ps2sec = " / 1000000000000.0";

		String sql = getCmDiffColumnsAsSqlComment("CmSysTableStats")
			    + "select top " + topRows + " \n"
			    + "     [table_schema] \n"
			    + "    ,[table_name] \n"
			    + "    ,cast('' as varchar(512))                                                         as [total_latency_sec__chart] \n"
			    + "    ,CAST( sum([total_latency])    " + ps2sec + "             as DECIMAL(20,1) )      as [total_latency_sec] \n"
			    + "    ,cast('' as varchar(512))                                                         as [rows_fetched__chart] \n"
			    + "    ,sum([rows_fetched])                                                              as [rows_fetched] \n"
			    + "    ,CAST( sum([fetch_latency])    " + ps2sec + "             as DECIMAL(20,1) )      as [fetch_latency_sec] \n"
			    + "    ,sum([rows_inserted])                                                             as [rows_inserted] \n"
			    + "    ,CAST( sum([insert_latency])   " + ps2sec + "             as DECIMAL(20,1) )      as [insert_latency_sec] \n"
			    + "    ,sum([rows_updated])                                                              as [rows_updated] \n"
			    + "    ,CAST( sum([update_latency])   " + ps2sec + "             as DECIMAL(20,1) )      as [update_latency_sec] \n"
			    + "    ,sum([rows_deleted])                                                              as [rows_deleted] \n"
			    + "    ,CAST( sum([delete_latency])   " + ps2sec + "             as DECIMAL(20,1) )      as [delete_latency_sec] \n"
			    + "    ,CAST( sum([io_read])          / 1048576.0                as DECIMAL(20,1) )      as [io_read_mb] \n"
			    + "    ,CAST( sum([io_read_latency])  " + ps2sec + "             as DECIMAL(20,1) )      as [io_read_latency_sec] \n"
			    + "    ,CAST( sum([io_write])         / 1048576.0                as DECIMAL(20,1) )      as [io_write_mb] \n"
			    + "    ,CAST( sum([io_write_latency]) " + ps2sec + "             as DECIMAL(20,1) )      as [io_write_latency_sec] \n"
			    + "    ,count(distinct [SessionSampleTime])                                              as [samples_count] \n"
			    + "from [CmSysTableStats_diff] \n"
			    + "where [total_latency] > 0 \n"
			    + sql_and_skipNewDiffRateRows
			    + getReportPeriodSqlWhere()
			    + "group by [table_schema], [table_name] \n"
			    + "order by sum([total_latency]) desc \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, true, "MySqlTopTableAccess");

		if (_shortRstm.getRowCount() == 0)
			return;

		// Highlight sort column
		_shortRstm.setHighlightSortColumns("total_latency_sec");

		_shortRstm.setDescription(
				"The tables that the server spent most time on during the period: reading and changing rows, and the file IO for them (from the table <code>CmSysTableStats_diff</code>).<br>" +
				"A table with many <code>rows_fetched</code> for the number of statements that use it is often read without a good index (see the Top SQL section, column RowsExamined).<br>" +
				"<b>Note</b>: The collector only records the most active tables (property <code>CmSysTableStats.sample.topRows</code>), and the first sample where a table is seen is not counted.<br>" +
				"");

		_shortRstm.setColumnDescription("table_schema"            , "The schema (database)");
		_shortRstm.setColumnDescription("table_name"              , "The table");
		_shortRstm.setColumnDescription("total_latency_sec__chart", "Chart: seconds spent on the table, in 10 minute chunks\nSo you can see WHEN the table was used.");
		_shortRstm.setColumnDescription("total_latency_sec"       , "Seconds spent on the table: reading and changing rows (it includes the file IO)");
		_shortRstm.setColumnDescription("rows_fetched__chart"     , "Chart: rows read from the table, in 10 minute chunks");
		_shortRstm.setColumnDescription("rows_fetched"            , "Rows read from the table");
		_shortRstm.setColumnDescription("fetch_latency_sec"       , "Seconds spent reading rows");
		_shortRstm.setColumnDescription("rows_inserted"           , "Rows inserted");
		_shortRstm.setColumnDescription("insert_latency_sec"      , "Seconds spent inserting rows");
		_shortRstm.setColumnDescription("rows_updated"            , "Rows updated");
		_shortRstm.setColumnDescription("update_latency_sec"      , "Seconds spent updating rows");
		_shortRstm.setColumnDescription("rows_deleted"            , "Rows deleted");
		_shortRstm.setColumnDescription("delete_latency_sec"      , "Seconds spent deleting rows");
		_shortRstm.setColumnDescription("io_read_mb"              , "MB read from the table's file (what was not in the buffer pool)");
		_shortRstm.setColumnDescription("io_read_latency_sec"     , "Seconds spent reading the file");
		_shortRstm.setColumnDescription("io_write_mb"             , "MB written to the table's file");
		_shortRstm.setColumnDescription("io_write_latency_sec"    , "Seconds spent writing the file");
		_shortRstm.setColumnDescription("samples_count"           , "In how many collector samples the table was used");

		//--------------------------------------------------------------------------------
		// Mini Charts: WHEN during the period
		//--------------------------------------------------------------------------------
		_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm,
				SparkLineParams.create       (DataSource.CounterModel)
				.setHtmlChartColumnName      ("total_latency_sec__chart")
				.setHtmlWhereKeyColumnName   ("table_schema, table_name")
				.setDbmsTableName            ("CmSysTableStats_diff")
				.setDbmsSampleTimeColumnName ("SessionSampleTime")
				.setDbmsDataValueColumnName  ("sum([total_latency])" + ps2sec).setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
				.setDbmsWhereKeyColumnName   ("table_schema, table_name")
				.setDbmsExtraWhereClause     (sql_and_skipNewDiffRateRows.trim())
				.setSparklineTooltipPostfix  ("Seconds spent on the table in below period")
				.validate()));

		_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm,
				SparkLineParams.create       (DataSource.CounterModel)
				.setHtmlChartColumnName      ("rows_fetched__chart")
				.setHtmlWhereKeyColumnName   ("table_schema, table_name")
				.setDbmsTableName            ("CmSysTableStats_diff")
				.setDbmsSampleTimeColumnName ("SessionSampleTime")
				.setDbmsDataValueColumnName  ("rows_fetched")
				.setDbmsWhereKeyColumnName   ("table_schema, table_name")
				.setDbmsExtraWhereClause     (sql_and_skipNewDiffRateRows.trim())
				.setSparklineTooltipPostfix  ("Rows read from the table in below period")
				.validate()));
	}
}
