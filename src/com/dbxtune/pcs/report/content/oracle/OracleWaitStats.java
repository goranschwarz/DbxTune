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
import java.util.ArrayList;
import java.util.List;

import com.dbxtune.cm.oracle.CmSystemEvent;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.pcs.report.content.SparklineHelper;
import com.dbxtune.pcs.report.content.SparklineHelper.AggType;
import com.dbxtune.pcs.report.content.SparklineHelper.DataSource;
import com.dbxtune.pcs.report.content.SparklineHelper.SparkLineParams;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

public class OracleWaitStats
extends OracleAbstract
{
	private ResultSetTableModel _shortRstm;

	public OracleWaitStats(DailySummaryReportAbstract reportingInstance)
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
		w.append(getDbxCentralLinkWithDescForGraphs(false, "Below are Wait Statistics: what the user sessions were waiting for during the day.",
				"CmSystemEvent_WaitClassAas",
				"CmSystemEvent_WaitEventTime",
				"CmSystemEvent_IoEventAvgMs"
				));

		_CmSystemEvent_byWaitClass.writeHtmlContent(w, null, null);

		// Get a description of this section, and column names
		w.append(getSectionDescriptionHtml(_shortRstm, true));

		w.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
		w.append(toHtmlTable(_shortRstm));

		if (isFullMessageType())
		{
			_CmSystemEvent_IoEventAvgMs.writeHtmlContent(w, null, "<b>Note</b>: A single block read ('db file sequential read') or a redo write ('log file parallel write') that takes more than 10-20 ms on average says that the disks are slow.");

			// Write JavaScript code for the mini charts
			for (String str : _miniChartJsList)
				w.append(str);
		}
	}

	@Override
	public String getSubject()
	{
		return "Wait Statistics for the full day (origin: CmSystemEvent / V$SYSTEM_EVENT)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}

	public static final String PROPKEY_SamplePeriodInMinutes = "DailySummaryReport.OracleWaitStats.samplePeriodInminutes";
	public static final int    DEFAULT_SamplePeriodInMinutes = 10;

	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmSystemEvent_diff" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		int samplePeriod = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_SamplePeriodInMinutes, DEFAULT_SamplePeriodInMinutes);
		int topRows      = getTopRows();

		String schema = getReportingInstance().getDbmsSchemaName();

		// Chart: seconds waited per Wait Class (user sessions only: the '_FG' columns), in chunks of 10 minutes
		_CmSystemEvent_byWaitClass = createTsStackedBarChart(conn, schema, CmSystemEvent.CM_NAME, "WAIT_CLASS", samplePeriod, null, "TIME_WAITED_MICRO_FG", 1_000_000.0, null, null, "User Sessions Wait Time in Seconds per Wait Class, grouped by " + samplePeriod + " minutes intervall");

		_CmSystemEvent_IoEventAvgMs = createTsLineChart(conn, schema, CmSystemEvent.CM_NAME, CmSystemEvent.GRAPH_NAME_IO_EVENT_AVG_MS, -1, true, null, "Disk IO Wait Events, Average ms per Wait (" + CmSystemEvent.SHORT_NAME + ")");

		// Table: the top wait events for the whole period
		String sql = ""
			    + "select top " + topRows + " \n"
			    + "     [WAIT_CLASS] \n"
			    + "    ,[EVENT] \n"
			    + "    ,sum([TOTAL_WAITS_FG])                                                                               as [waits] \n"
			    + "    ,cast('' as varchar(512))                                                                            as [wait_time_sec__chart] \n"
			    + "    ,CAST( sum([TIME_WAITED_MICRO_FG]) / 1000000.0                               as DECIMAL(20,1) )      as [wait_time_sec] \n"
			    + "    ,cast('' as varchar(512))                                                                            as [avg_wait_ms__chart] \n"
			    + "    ,CAST( sum([TIME_WAITED_MICRO_FG]) / 1000.0 / nullif(sum([TOTAL_WAITS_FG]), 0) as DECIMAL(20,2) )    as [avg_wait_ms] \n"
			    + "    ,CAST( 100.0 * sum([TIME_WAITED_MICRO_FG]) \n"
			    + "           / nullif((select sum(t.[TIME_WAITED_MICRO_FG]) from [CmSystemEvent_diff] t where 1 = 1 " + getReportPeriodSqlWhere().trim() + "), 0) as DECIMAL(5,1) ) as [pct_of_all_waits] \n"
			    + "    ,sum([TOTAL_TIMEOUTS_FG])                                                                            as [timeouts] \n"
			    + "from [CmSystemEvent_diff] \n"
			    + "where [TIME_WAITED_MICRO_FG] > 0 \n"
			    + getReportPeriodSqlWhere()
			    + "group by [WAIT_CLASS], [EVENT] \n"
			    + "order by sum([TIME_WAITED_MICRO_FG]) desc \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, true, "OracleWaitStats");

		// Highlight sort column
		_shortRstm.setHighlightSortColumns("wait_time_sec");

		// Describe the table
		_shortRstm.setDescription(
				"The Wait Events that the <b>user sessions</b> waited most time on during the period (from the table <code>CmSystemEvent_diff</code>).<br>" +
				"Waits in the Wait Class 'Idle' are not recorded (a session that waits for the next statement from the client is not a problem).<br>" +
				"Background processes (DBWR, LGWR...) are not included here.<br>" +
				"");

		_shortRstm.setColumnDescription("WAIT_CLASS"      , "The group that the Wait Event belongs to: User I/O, System I/O, Concurrency, Application (locks), Commit, Network...");
		_shortRstm.setColumnDescription("EVENT"           , "Name of the Wait Event");
		_shortRstm.setColumnDescription("waits"           , "How many times user sessions waited on this event");
		_shortRstm.setColumnDescription("wait_time_sec"   , "Total time, in seconds, that user sessions waited on this event");
		_shortRstm.setColumnDescription("avg_wait_ms"     , "Average time for ONE wait, in milliseconds. Formula: wait time / waits");
		_shortRstm.setColumnDescription("pct_of_all_waits", "This events share of ALL wait time (for user sessions) in the period, in percent");
		_shortRstm.setColumnDescription("timeouts"        , "How many of the waits that ended with a timeout");
		_shortRstm.setColumnDescription("wait_time_sec__chart", "Chart: seconds waited on this event, in 10 minute chunks\nSo you can see WHEN the sessions waited.");
		_shortRstm.setColumnDescription("avg_wait_ms__chart"  , "Chart: average time for ONE wait in milliseconds, in 10 minute chunks\nSo you can see when it was slower than normal (for a disk IO event: when the disks were slow).");

		//--------------------------------------------------------------------------------
		// Mini Charts: WHEN during the period
		//--------------------------------------------------------------------------------
		_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm,
				SparkLineParams.create       (DataSource.CounterModel)
				.setHtmlChartColumnName      ("wait_time_sec__chart")
				.setHtmlWhereKeyColumnName   ("EVENT")
				.setDbmsTableName            ("CmSystemEvent_diff")
				.setDbmsSampleTimeColumnName ("SessionSampleTime")
				.setDbmsDataValueColumnName  ("sum([TIME_WAITED_MICRO_FG]) / 1000000.0").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
				.setDbmsWhereKeyColumnName   ("EVENT")
				.setSparklineTooltipPostfix  ("Seconds waited by user sessions in below period")
				.validate()));

		_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm,
				SparkLineParams.create       (DataSource.CounterModel)
				.setHtmlChartColumnName      ("avg_wait_ms__chart")
				.setHtmlWhereKeyColumnName   ("EVENT")
				.setDbmsTableName            ("CmSystemEvent_diff")
				.setDbmsSampleTimeColumnName ("SessionSampleTime")
				.setDbmsDataValueColumnName  ("sum([TIME_WAITED_MICRO_FG]) / 1000.0 / nullif(sum([TOTAL_WAITS_FG]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(2)
				.setDbmsWhereKeyColumnName   ("EVENT")
				.setSparklineTooltipPostfix  ("Average milliseconds for ONE wait in below period")
				.validate()));
	}

	private List<String> _miniChartJsList = new ArrayList<>();

	private IReportChart _CmSystemEvent_byWaitClass;
	private IReportChart _CmSystemEvent_IoEventAvgMs;
}
