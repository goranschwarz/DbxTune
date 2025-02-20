/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.pcs.report.content.sqlserver;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Map.Entry;

import com.dbxtune.config.dict.SqlServerWaitTypeDictionary;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.pcs.report.content.ReportChartTimeSeriesStackedBar.TopGroupCountReport;
import com.dbxtune.pcs.report.content.SparklineHelper;
import com.dbxtune.pcs.report.content.SparklineHelper.AggType;
import com.dbxtune.pcs.report.content.SparklineHelper.DataSource;
import com.dbxtune.pcs.report.content.SparklineHelper.SparklineResult;
import com.dbxtune.pcs.report.content.SparklineJfreeChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

public class SqlServerWaitStats 
extends SqlServerAbstract
{
//	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public SqlServerWaitStats(DailySummaryReportAbstract reportingInstance)
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

//	@Override
//	public void writeShortMessageText(Writer w)
//	throws IOException
//	{
//		writeMessageText(w);
//	}

//TODO; // to htmlTable, add: sum(WaitCount) and avg(WaitTimePerCount) (Avg ms per Wait), "Per Core Per Hour (Look at Brent Ozar how he calculates that)"

	@Override
	public void writeMessageText(Writer sb, MessageType messageType)
	throws IOException
	{
		sb.append("Wait Statistics for the full day (origin: CmWaitStat / dm_os_wait_stats) <br>");
		sb.append("<br>");
		sb.append("Sample Period:<br>");
		sb.append("&emsp;&bull; <code>" + PROPKEY_SamplePeriodInMinutes + " = " + Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_SamplePeriodInMinutes, DEFAULT_SamplePeriodInMinutes) + " </code><br>");
		sb.append("<br>");
		sb.append("wait_type for the first chart:<br>");
		sb.append("&emsp;&bull; <code>" + PROPKEY_Selected + " = " + Configuration.getCombinedConfiguration().getProperty(PROPKEY_Selected, DEFAULT_Selected) + " </code><br>");
		sb.append("<br>");
		sb.append("Top wait_type's for last chart:<br>");
		sb.append("&emsp;&bull; <code>" + PROPKEY_TopCount + " = " + Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_TopCount, DEFAULT_TopCount) + " </code><br>");
		sb.append("<br>");
		sb.append("SQL-Server Source table is 'sys.dm_os_wait_stats'. <br>");
		sb.append("PCS Source table is 'CmWaitStat_diff'. (PCS = Persistent Counter Store) <br>");

		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are Wait Statistics on the Server level, what is the server waiting for.",
				"CmWaitStat_selected",
				"CmWaitStat_cxpacket",
				"CmWaitStat_all"
				));

		sb.append("<br>");
		sb.append("Note that the Wait Time is in <b>Seconds</b>...<br>");
		sb.append("It is totally <b>normal</b> to have Wait Times above the <i>Sample Period</i> (10 minutes is 600 seconds).<br>");
		sb.append("For example: if we have a Wait Time of 6,000 (which is 1 hours and 40 minutes) in a 10 minute period, it simply means that we had <b>10 tasks sleeping the whole time</b>, for example: waiting on a blocking lock, or some other resource, which is indicated by the <i>Wait Type</i>...<br>");
		sb.append("<br>");

		_CmWaitStat_selected .writeHtmlContent(sb, null, null);
		_CmWaitStat_cxpacket .writeHtmlContent(sb, null, null);
		_CmWaitStat_all      .writeHtmlContent(sb, null, null);
	}

	@Override
	public String getSubject()
	{
		return "Wait Statistics for the full day (origin: CmWaitStat / dm_os_wait_stats)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	public static final String PROPKEY_SamplePeriodInMinutes = "DailySummaryReport.SqlServerWaitStats.samplePeriodInminutes";
	public static final int    DEFAULT_SamplePeriodInMinutes = 10;

	public static final String PROPKEY_TopCount              = "DailySummaryReport.SqlServerWaitStats.top.count";
	public static final int    DEFAULT_TopCount              = 20;

	public static final String PROPKEY_Selected              = "DailySummaryReport.SqlServerWaitStats.selected.wait_types";
	public static final String DEFAULT_Selected              = "ASYNC_NETWORK_IO, LCK_M_S, LCK_M_U, LCK_M_IX, LCK_M_X, LATCH_SH, LATCH_EX, PAGEIOLATCH_SH, PAGEIOLATCH_EX, PAGELATCH_SH, PAGELATCH_EX, SOS_SCHEDULER_YIELD, ASYNC_IO_COMPLETION, WRITELOG, TDC, OLEDB, RESOURCE_SEMAPHORE";

	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmWaitStats_diff" }; // createTsStackedBarChart(...) does a check against that table
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		String selected = Configuration.getCombinedConfiguration().getProperty(PROPKEY_Selected, DEFAULT_Selected);
//		String cxpacket = "CXPACKET, CXCONSUMER";
//		String cxpacket = "CXPACKET, CXCONSUMER, CXSYNC_PORT, CXSYNC_CONSUMER, CXROWSET_SYNC";
		String cxpacket = "regex: CX.*";
		
		int samplePeriod = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_SamplePeriodInMinutes, DEFAULT_SamplePeriodInMinutes);
		int topCount     = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_TopCount             , DEFAULT_TopCount);
		
		TopGroupCountReport tgcp = new TopGroupCountReport(topCount)
		{
			@Override
			public String process()
			{
				String groupColName = getOwner().getGroupColumnName();
				String valueColName = getOwner().getValueColumnName();
				int    samplePeriod = getOwner().getSamplePeriodInMinutes();

				StringBuilder sb = new StringBuilder();
				sb.append("<br>Here are the top " + getTopCount() + " '" + groupColName + "' ordered by '" + valueColName +"' for the whole period (while the above chart is in " + samplePeriod + " minutes chunks).<br>\n");
				sb.append("<table class='sortable'> \n");
				sb.append("<tr> \n");
				sb.append("  <th>Top #</th> \n");
				sb.append("  <th>").append(groupColName).append("</th> \n");
				sb.append("  <th>wait_time_ms__chart</th> \n");
				sb.append("  <th>").append(valueColName).append("</th> \n");
				sb.append("  <th>wait_time_ms as HH:MM:SS</th> \n");
				sb.append("  <th>WaitTimeInMsPerCount__chart</th> \n");
				sb.append("  <th>SQL Skills info</th> \n");
				sb.append("  <th>Google it</th> \n");
				sb.append("  <th>MS Description</th> \n");
				sb.append("</tr> \n");
				
				String sparklineJavaScriptInitCode_ms   = "";
				String sparklineJavaScriptInitCode_wtpc = "";
				int cnt = 1;
				for (Entry<String, Double> entry : getTop().entrySet())
				{
					String sparklineDataStr_ms   = "";
					String sparklineDataStr_wtpc = "";
					
					//---- wait_time_ms -------------------------------------
					{
						// Below is to create a "spark-line" or "mini-chart" for the wait time...
						String sparklineClassName      = "sparklines_wait_time_ms__chart";
						String sparklineTooltipPostfix = " - SUM 'wait_time_ms' for below period";
						//String sparklineDataStr        = "";
						int groupByMinutes = 10;

						try
						{
							SparklineResult result = SparklineHelper.getSparclineData(
									conn,
									DataSource.CounterModel,
									getReportingInstance().getReportPeriodOrRecordingBeginTime().toLocalDateTime(),
									getReportingInstance().getReportPeriodOrRecordingEndTime()  .toLocalDateTime(), 
									groupByMinutes,                  // params.getGroupDataInMinutes()
									AggType.SUM,                     // params.getGroupDataAggregationType()
									null,                            // params.getDbmsSchemaName()
									"CmWaitStats_diff",              // params.getDbmsTableName()
									"SessionSampleTime",             // params.getDbmsSampleTimeColumnName()
									"wait_time_ms",                  // params.getDbmsDataValueColumnName()
									false,                           // params.getDbmsDataValueColumnNameIsExpression()
									Arrays.asList("wait_type"),      // colNameList
									Arrays.asList(entry.getKey()),   // colValList
									null,                            // params.getDbmsExtraWhereClause()
									0,                               // params.getNoDataInGroupDefaultValue()
									0);                              // params.getDecimalScale()             

							String valStr              = StringUtil.toCommaStr(result.values, ",");
							String jfreeChartInlinePng = SparklineJfreeChart.create(result);

							// Create the data that will be in <td>...HERE...</td>
							sparklineDataStr_ms = "<div class='" + sparklineClassName + "' values='" + valStr + "'>" + jfreeChartInlinePng + "</div>";
							
							// Create JavaScript code to initialize all SparkLines with a specific class name
							if (StringUtil.isNullOrBlank(sparklineJavaScriptInitCode_ms))
								sparklineJavaScriptInitCode_ms = SparklineHelper.getJavaScriptInitCode(SqlServerWaitStats.this, result, sparklineClassName, sparklineTooltipPostfix);
						}
						catch (SQLException ex)
						{
							sparklineDataStr_ms = "Problems getting data, Caught: " + ex;
						}
					}
					//---- WaitTimePerCount -------------------------------------
					{
						// Below is to create a "spark-line" or "mini-chart" for the wait time...
						String sparklineClassName      = "sparklines_WaitTimePerCount__chart";
						String sparklineTooltipPostfix = " - MAX 'WaitTimePerCount' in MS for below period";
						//String sparklineDataStr        = "";
						int groupByMinutes = 10;

						try
						{
							SparklineResult result = SparklineHelper.getSparclineData(
									conn,
									DataSource.CounterModel,
									getReportingInstance().getReportPeriodOrRecordingBeginTime().toLocalDateTime(),
									getReportingInstance().getReportPeriodOrRecordingEndTime()  .toLocalDateTime(), 
									groupByMinutes,                  // params.getGroupDataInMinutes()
									AggType.MAX,                     // params.getGroupDataAggregationType()
									null,                            // params.getDbmsSchemaName()
									"CmWaitStats_diff",              // params.getDbmsTableName()
									"SessionSampleTime",             // params.getDbmsSampleTimeColumnName()
									"WaitTimePerCount",              // params.getDbmsDataValueColumnName()
									false,                           // params.getDbmsDataValueColumnNameIsExpression()
									Arrays.asList("wait_type"),      // colNameList
									Arrays.asList(entry.getKey()),   // colValList
									null,                            // params.getDbmsExtraWhereClause()
									0,                               // params.getNoDataInGroupDefaultValue()
									1);                              // params.getDecimalScale()             

							String valStr              = StringUtil.toCommaStr(result.values, ",");
							String jfreeChartInlinePng = SparklineJfreeChart.create(result);

							// Create the data that will be in <td>...HERE...</td>
							sparklineDataStr_wtpc = "<div class='" + sparklineClassName + "' values='" + valStr + "'>" + jfreeChartInlinePng + "</div>";
							
							// Create JavaScript code to initialize all SparkLines with a specific class name
							if (StringUtil.isNullOrBlank(sparklineJavaScriptInitCode_wtpc))
								sparklineJavaScriptInitCode_wtpc = SparklineHelper.getJavaScriptInitCode(SqlServerWaitStats.this, result, sparklineClassName, sparklineTooltipPostfix);
						}
						catch (SQLException ex)
						{
							sparklineDataStr_wtpc = "Problems getting data, Caught: " + ex;
						}
					}

					String formatedWaitTimeMs = ResultSetTableModel.renderHighlightSortColumnForHtml("wait_time_ms", NumberFormat.getInstance().format(entry.getValue()));

					sb.append("<tr> \n");
					sb.append("  <td>").append(cnt++           ).append("</td> \n");
					sb.append("  <td>").append(entry.getKey()  ).append("</td> \n");
					sb.append("  <td>").append(sparklineDataStr_ms).append("</td> \n");
					sb.append("  <td>").append(formatedWaitTimeMs).append("</td> \n");
					sb.append("  <td>").append(TimeUtils.msToTimeStr("%?DD[d ]%HH:%MM:%SS", entry.getValue().intValue())).append("</td> \n");
					sb.append("  <td>").append(sparklineDataStr_wtpc).append("</td> \n");
					sb.append("  <td> <a href='https://www.sqlskills.com/help/waits/"+entry.getKey()+"' target='_blank'>"+entry.getKey()+"</a>").append("</td> \n");
					sb.append("  <td> <a href='https://www.google.com/search?q="+entry.getKey()+"' target='_blank'>Google: "+entry.getKey()+"</a>").append("</td> \n");
					sb.append("  <td>").append(SqlServerWaitTypeDictionary.getInstance().getDescriptionPlain(entry.getKey())).append("</td> \n");
					sb.append("</tr> \n");
				}
				sb.append("</table> \n");
				
				sb.append(sparklineJavaScriptInitCode_ms);
				sb.append(sparklineJavaScriptInitCode_wtpc);
				
				return sb.toString();
			}
		};
		
		String schema = getReportingInstance().getDbmsSchemaName();

		_CmWaitStat_selected     = createTsStackedBarChart(conn, schema, "CmWaitStats", "wait_type", samplePeriod, null, "wait_time_ms", 1000.0, selected, null, "Wait Types in Seconds (pre-selected), grouped by " + samplePeriod + " minutes intervall");
		_CmWaitStat_cxpacket     = createTsStackedBarChart(conn, schema, "CmWaitStats", "wait_type", samplePeriod, null, "wait_time_ms", 1000.0, cxpacket, null,  "Wait Type in Seconds (only " + cxpacket + "), grouped by " + samplePeriod + " minutes intervall");
		_CmWaitStat_all          = createTsStackedBarChart(conn, schema, "CmWaitStats", "wait_type", samplePeriod, tgcp, "wait_time_ms", 1000.0, null,     null, "Wait Types in Seconds (top-"+tgcp.getTopCount()+"-wait_types), grouped by " + samplePeriod + " minutes intervall");
	}

	private IReportChart _CmWaitStat_selected;
	private IReportChart _CmWaitStat_cxpacket;
	private IReportChart _CmWaitStat_all;
}
