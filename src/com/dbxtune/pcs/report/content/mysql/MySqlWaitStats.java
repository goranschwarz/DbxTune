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

import com.dbxtune.cm.mysql.CmWaits;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.pcs.report.content.SparklineHelper;
import com.dbxtune.pcs.report.content.SparklineHelper.AggType;
import com.dbxtune.pcs.report.content.SparklineHelper.DataSource;
import com.dbxtune.pcs.report.content.SparklineHelper.SparkLineParams;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

public class MySqlWaitStats
extends MySqlAbstract
{
	private ResultSetTableModel _shortRstm;

	public MySqlWaitStats(DailySummaryReportAbstract reportingInstance)
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
		w.append(getDbxCentralLinkWithDescForGraphs(false, "Below are Wait Statistics: what the connections were waiting for during the day.",
				"CmWaits_WaitClassTime",
				"CmWaits_WaitEventTime",
				"CmWaits_IoEventAvgMs"
				));

		_CmWaits_byWaitClass.writeHtmlContent(w, null, null);

		// Get a description of this section, and column names
		w.append(getSectionDescriptionHtml(_shortRstm, true));

		w.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
		w.append(toHtmlTable(_shortRstm));

		if (isFullMessageType())
		{
			_CmWaits_IoEventAvgMs.writeHtmlContent(w, null, "<b>Note</b>: A write to the redo log (innodb_log_file) or a read of a data file (innodb_data_file) that takes more than 10-20 ms on average says that the disks are slow.");

			// Write JavaScript code for the mini charts
			for (String str : _miniChartJsList)
				w.append(str);
		}
	}

	@Override
	public String getSubject()
	{
		return "Wait Statistics for the full day (origin: CmWaits / performance_schema.events_waits_summary_global_by_event_name)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}

	public static final String PROPKEY_SamplePeriodInMinutes = "DailySummaryReport.MySqlWaitStats.samplePeriodInminutes";
	public static final int    DEFAULT_SamplePeriodInMinutes = 10;

	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmWaits_diff" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		int samplePeriod = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_SamplePeriodInMinutes, DEFAULT_SamplePeriodInMinutes);
		int topRows      = getTopRows();

		String schema = getReportingInstance().getDbmsSchemaName();

		// Chart: seconds waited per Wait Class, in chunks of 10 minutes
		_CmWaits_byWaitClass = createTsStackedBarChart(conn, schema, CmWaits.CM_NAME, "WaitClass", samplePeriod, null, "WaitTimeMs", 1000.0, null, null, "Wait Time in Seconds per Wait Class, grouped by " + samplePeriod + " minutes intervall");

		_CmWaits_IoEventAvgMs = createTsLineChart(conn, schema, CmWaits.CM_NAME, CmWaits.GRAPH_NAME_IO_EVENT_AVG_MS, -1, true, null, "File IO Wait Events, Average ms per Wait (" + CmWaits.SHORT_NAME + ")");

		// Table: the top wait events for the whole period
		// NOTE: table IO (io/table) includes the file IO it causes (the waits are nested), so 'pct_of_all_waits' can't be summed to 100 in a meaningful way
		String sql = ""
			    + "select top " + topRows + " \n"
			    + "     [WaitClass] \n"
			    + "    ,[EventName] \n"
			    + "    ,sum([WaitCount])                                                                                as [waits] \n"
			    + "    ,cast('' as varchar(512))                                                                        as [wait_time_sec__chart] \n"
			    + "    ,CAST( sum([WaitTimeMs]) / 1000.0                                        as DECIMAL(20,1) )      as [wait_time_sec] \n"
			    + "    ,cast('' as varchar(512))                                                                        as [avg_wait_ms__chart] \n"
			    + "    ,CAST( sum([WaitTimeMs]) / nullif(sum([WaitCount]), 0)                   as DECIMAL(20,3) )      as [avg_wait_ms] \n"
			    + "    ,CAST( 100.0 * sum([WaitTimeMs]) \n"
			    + "           / nullif((select sum(t.[WaitTimeMs]) from [CmWaits_diff] t where 1 = 1 " + getReportPeriodSqlWhere().trim() + "), 0) as DECIMAL(5,1) ) as [pct_of_all_waits] \n"
			    + "    ,max([MaxWaitMs])                                                                                as [max_wait_ms] \n"
			    + "from [CmWaits_diff] \n"
			    + "where [WaitTimeMs] > 0 \n"
			    + getReportPeriodSqlWhere()
			    + "group by [WaitClass], [EventName] \n"
			    + "order by sum([WaitTimeMs]) desc \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, true, "MySqlWaitStats");

		// Highlight sort column
		_shortRstm.setHighlightSortColumns("wait_time_sec");

		// Describe the table
		_shortRstm.setDescription(
				"The Wait Events that the connections waited most time on during the period (from the table <code>CmWaits_diff</code>).<br>" +
				"Only the instruments that are ON are counted: by default file IO (io/file), table IO (io/table), table locks and metadata locks. The mutex/rwlock waits (synch/...) are OFF by default.<br>" +
				"<b>Note</b>: The table IO time (io/table) includes the file IO it causes (the waits are nested), and InnoDB row lock waits are not Wait Events (see the Long Running and Blocking section).<br>" +
				"");

		_shortRstm.setColumnDescription("WaitClass"           , "The group of the Wait Event: io/file, io/table, lock/table, lock/metadata, synch/mutex...");
		_shortRstm.setColumnDescription("EventName"           , "Name of the Wait Event (the instrument)");
		_shortRstm.setColumnDescription("waits"               , "How many times the connections waited on this event");
		_shortRstm.setColumnDescription("wait_time_sec"       , "Total time, in seconds, that the connections waited on this event");
		_shortRstm.setColumnDescription("avg_wait_ms"         , "Average time for ONE wait, in milliseconds. Formula: wait time / waits");
		_shortRstm.setColumnDescription("pct_of_all_waits"    , "This events share of ALL wait time in the period, in percent (the nested waits are counted twice, see the note above)");
		_shortRstm.setColumnDescription("max_wait_ms"         , "The longest wait, in milliseconds. NOTE: since the server was started, not only in the period");
		_shortRstm.setColumnDescription("wait_time_sec__chart", "Chart: seconds waited on this event, in 10 minute chunks\nSo you can see WHEN the connections waited.");
		_shortRstm.setColumnDescription("avg_wait_ms__chart"  , "Chart: average time for ONE wait in milliseconds, in 10 minute chunks\nSo you can see when it was slower than normal (for a file IO event: when the disks were slow).");

		//--------------------------------------------------------------------------------
		// Mini Charts: WHEN during the period
		//--------------------------------------------------------------------------------
		_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm,
				SparkLineParams.create       (DataSource.CounterModel)
				.setHtmlChartColumnName      ("wait_time_sec__chart")
				.setHtmlWhereKeyColumnName   ("EventName")
				.setDbmsTableName            ("CmWaits_diff")
				.setDbmsSampleTimeColumnName ("SessionSampleTime")
				.setDbmsDataValueColumnName  ("sum([WaitTimeMs]) / 1000.0").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
				.setDbmsWhereKeyColumnName   ("EventName")
				.setSparklineTooltipPostfix  ("Seconds waited in below period")
				.validate()));

		_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm,
				SparkLineParams.create       (DataSource.CounterModel)
				.setHtmlChartColumnName      ("avg_wait_ms__chart")
				.setHtmlWhereKeyColumnName   ("EventName")
				.setDbmsTableName            ("CmWaits_diff")
				.setDbmsSampleTimeColumnName ("SessionSampleTime")
				.setDbmsDataValueColumnName  ("sum([WaitTimeMs]) / nullif(sum([WaitCount]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(3)
				.setDbmsWhereKeyColumnName   ("EventName")
				.setSparklineTooltipPostfix  ("Average milliseconds for ONE wait in below period")
				.validate()));
	}

	private List<String> _miniChartJsList = new ArrayList<>();

	private IReportChart _CmWaits_byWaitClass;
	private IReportChart _CmWaits_IoEventAvgMs;
}
