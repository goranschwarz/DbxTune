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
import java.util.ArrayList;
import java.util.List;

import com.dbxtune.cm.db2.CmTimeSpent;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.pcs.report.content.SparklineHelper;
import com.dbxtune.pcs.report.content.SparklineHelper.AggType;
import com.dbxtune.pcs.report.content.SparklineHelper.DataSource;
import com.dbxtune.pcs.report.content.SparklineHelper.SparkLineParams;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

/**
 * Where the time of the database went during the day: the waits per kind, and the processing (from CmTimeSpent). Built on MySqlWaitStats.
 */
public class Db2TimeSpent
extends Db2Abstract
{
	private ResultSetTableModel _shortRstm;

	public Db2TimeSpent(DailySummaryReportAbstract reportingInstance)
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
		w.append(getDbxCentralLinkWithDescForGraphs(false, "Below is where the time of the database went: what the requests waited for, and the processing.",
				"CmTimeSpent_RqstTime",
				"CmTimeSpent_WaitTime"
				));

		_CmTimeSpent_RqstTime.writeHtmlContent(w, null, null);
		_CmTimeSpent_byMetric.writeHtmlContent(w, null, null);

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
		return "Time Spent: Waits and Processing for the full day (origin: CmTimeSpent / MON_GET_DATABASE_DETAILS)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}

	public static final String PROPKEY_SamplePeriodInMinutes = "DailySummaryReport.Db2TimeSpent.samplePeriodInminutes";
	public static final int    DEFAULT_SamplePeriodInMinutes = 10;

	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmTimeSpent_diff" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		int samplePeriod = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_SamplePeriodInMinutes, DEFAULT_SamplePeriodInMinutes);
		int topRows      = getTopRows();

		String schema = getReportingInstance().getDbmsSchemaName();

		_CmTimeSpent_RqstTime = createTsLineChart(conn, schema, CmTimeSpent.CM_NAME, CmTimeSpent.GRAPH_NAME_RQST_TIME, -1, false, null, "Request Time: Wait and Processing, in ms per Second (" + CmTimeSpent.SHORT_NAME + ")");

		// Chart: seconds waited per kind of wait (the parts of TOTAL_WAIT_TIME), in chunks of 10 minutes
		_CmTimeSpent_byMetric = createTsStackedBarChart(conn, schema, CmTimeSpent.CM_NAME, "METRIC_NAME", samplePeriod, null, "TOTAL_TIME_MS", 1000.0, null, "TOTAL_RQST_TIME, TOTAL_WAIT_TIME, CLIENT_IDLE_WAIT_TIME", "Time in Seconds per Metric (waits and processing), grouped by " + samplePeriod + " minutes intervall");

		// Table: the waits and processing for the whole period
		String sql = ""
			    + "select top " + topRows + " \n"
			    + "     [METRIC_NAME] \n"
			    + "    ,max([TIME_TYPE])                                                                              as [TIME_TYPE] \n"
			    + "    ,max([PARENT_METRIC_NAME])                                                                     as [PARENT_METRIC_NAME] \n"
			    + "    ,cast('' as varchar(512))                                                                      as [time_sec__chart] \n"
			    + "    ,CAST( sum([TOTAL_TIME_MS]) / 1000.0                                  as DECIMAL(20,1) )      as [time_sec] \n"
			    + "    ,sum([COUNT])                                                                                  as [count] \n"
			    + "    ,CAST( sum([TOTAL_TIME_MS]) / nullif(sum([COUNT]), 0)                 as DECIMAL(20,3) )      as [avg_ms] \n"
			    + "    ,CAST( 100.0 * sum([TOTAL_TIME_MS]) \n"
			    + "           / nullif((select sum(t.[TOTAL_TIME_MS]) from [CmTimeSpent_diff] t where t.[METRIC_NAME] = 'TOTAL_RQST_TIME' " + getReportPeriodSqlWhere().trim() + "), 0) as DECIMAL(5,1) ) as [pct_of_rqst_time] \n"
			    + "from [CmTimeSpent_diff] \n"
			    + "where [TOTAL_TIME_MS] > 0 \n"
			    + "  and [METRIC_NAME] not in ('TOTAL_RQST_TIME', 'CLIENT_IDLE_WAIT_TIME') \n"
			    + "  and [LEVEL] = 1 \n"
			    + getReportPeriodSqlWhere()
			    + "group by [METRIC_NAME] \n"
			    + "order by sum([TOTAL_TIME_MS]) desc \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, true, "Db2TimeSpent");

		// Highlight sort column
		_shortRstm.setHighlightSortColumns("time_sec");

		// Describe the table
		_shortRstm.setDescription(
				"Where the time of the database went during the period (from the table <code>CmTimeSpent_diff</code>).<br>" +
				"TOTAL_WAIT_TIME is all the waiting, and the rows with TIME_TYPE WAIT are its parts (LOCK_WAIT_TIME, POOL_READ_TIME, LOG_DISK_WAIT_TIME...). The PROCESSING rows are the work: TOTAL_SECTION_PROC_TIME is executing statements.<br>" +
				"<b>pct_of_rqst_time</b> is how much of all request time (TOTAL_RQST_TIME) it was: the biggest wait is normally what to look at first.<br>" +
				"");

		_shortRstm.setColumnDescription("METRIC_NAME"       , "The time spent metric, for example LOCK_WAIT_TIME, POOL_READ_TIME, LOG_DISK_WAIT_TIME, TOTAL_SECTION_PROC_TIME");
		_shortRstm.setColumnDescription("TIME_TYPE"         , "WAIT, PROCESSING or TOTAL (TOTAL_WAIT_TIME)");
		_shortRstm.setColumnDescription("PARENT_METRIC_NAME", "What it is a part of: TOTAL_WAIT_TIME (a kind of wait) or TOTAL_RQST_TIME");
		_shortRstm.setColumnDescription("time_sec"          , "Total time, in seconds");
		_shortRstm.setColumnDescription("count"             , "How many times (for example lock waits, or page reads). 0 for the metrics that have no count");
		_shortRstm.setColumnDescription("avg_ms"            , "Time per count, in milliseconds (for example ms per page read: above about 10 ms the disks are slow)");
		_shortRstm.setColumnDescription("pct_of_rqst_time"  , "The share of all request time (TOTAL_RQST_TIME), in percent");
		_shortRstm.setColumnDescription("time_sec__chart"   , "Chart: seconds, in 10 minute chunks\nSo you can see WHEN.");

		//--------------------------------------------------------------------------------
		// Mini Charts: WHEN during the period
		//--------------------------------------------------------------------------------
		_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm,
				SparkLineParams.create       (DataSource.CounterModel)
				.setHtmlChartColumnName      ("time_sec__chart")
				.setHtmlWhereKeyColumnName   ("METRIC_NAME")
				.setDbmsTableName            ("CmTimeSpent_diff")
				.setDbmsSampleTimeColumnName ("SessionSampleTime")
				.setDbmsDataValueColumnName  ("sum([TOTAL_TIME_MS]) / 1000.0").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
				.setDbmsWhereKeyColumnName   ("METRIC_NAME")
				.setSparklineTooltipPostfix  ("Seconds in below period")
				.validate()));
	}

	private List<String> _miniChartJsList = new ArrayList<>();

	private IReportChart _CmTimeSpent_RqstTime;
	private IReportChart _CmTimeSpent_byMetric;
}
