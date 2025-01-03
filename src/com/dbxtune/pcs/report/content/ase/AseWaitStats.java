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
package com.dbxtune.pcs.report.content.ase;

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.pcs.report.content.ReportChartTimeSeriesStackedBar;
import com.dbxtune.pcs.report.content.ReportChartTimeSeriesStackedBar.TopGroupCountReport;
import com.dbxtune.pcs.report.content.ReportChartTimeSeriesStackedBar.TopGroupDataRecord;
import com.dbxtune.pcs.report.content.SparklineHelper;
import com.dbxtune.pcs.report.content.SparklineHelper.AggType;
import com.dbxtune.pcs.report.content.SparklineHelper.DataSource;
import com.dbxtune.pcs.report.content.SparklineHelper.SparklineResult;
import com.dbxtune.pcs.report.content.SparklineJfreeChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

public class AseWaitStats 
extends AseAbstract
{
//	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public AseWaitStats(DailySummaryReportAbstract reportingInstance)
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

	@Override
	public void writeMessageText(Writer w, MessageType messageType)
	throws IOException
	{
		w.append("Wait Statistics for the full day (origin: CmSysWaits / monSysWaits) <br>");
		w.append("<br>");
		w.append("Sample Period:<br>");
		w.append("&emsp;&bull; <code>" + PROPKEY_SamplePeriodInMinutes + " = " + Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_SamplePeriodInMinutes, DEFAULT_SamplePeriodInMinutes) + " </code><br>");
		w.append("<br>");
//		sb.append("wait_type for the first chart:<br>");
//		sb.append("&emsp;&bull; <code>" + PROPKEY_Selected + " = " + Configuration.getCombinedConfiguration().getProperty(PROPKEY_Selected, DEFAULT_Selected) + " </code><br>");
//		sb.append("<br>");
		w.append("Top WaitEvents for last chart:<br>");
		w.append("&emsp;&bull; <code>" + PROPKEY_TopCount + " = " + Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_TopCount, DEFAULT_TopCount) + " </code><br>");
		w.append("<br>");
		w.append("ASE Source table is 'monSysWaits'. <br>");
		w.append("PCS Source table is 'CmSysWaits_diff'. (PCS = Persistent Counter Store) <br>");

		w.append(getDbxCentralLinkWithDescForGraphs(false, "Below are Wait Statistics on the Server level, what is the server waiting for.",
				"CmSysWaits_byClass",
				"CmSysWaits_byEvent"
				));

		_CmSysWaits_byClass .writeHtmlContent(w, null, null);
		_CmSysWaits_byEvent .writeHtmlContent(w, null, null);
	}

	@Override
	public String getSubject()
	{
		return "Wait Statistics for the full day (origin: CmSysWaits / monSysWaits)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}

	public static final String PROPKEY_SamplePeriodInMinutes = "DailySummaryReport.AseWaitStats.samplePeriodInminutes";
	public static final int    DEFAULT_SamplePeriodInMinutes = 10;

	public static final String PROPKEY_TopCount              = "DailySummaryReport.AseWaitStats.top.count";
	public static final int    DEFAULT_TopCount              = 20;

//	public static final String PROPKEY_Selected              = "DailySummaryReport.AseWaitStats.selected.wait_types";
//	public static final String DEFAULT_Selected              = "ASYNC_NETWORK_IO, LCK_M_IX, LCK_M_X, LATCH_SH, LATCH_EX, PAGEIOLATCH_SH, PAGEIOLATCH_EX, SOS_SCHEDULER_YIELD, ASYNC_IO_COMPLETION, WRITELOG, TDC, OLEDB";

	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmSysWaits_diff" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		int samplePeriod = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_SamplePeriodInMinutes, DEFAULT_SamplePeriodInMinutes);
		int topCount     = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_TopCount             , DEFAULT_TopCount);

		// Below "simple" creation wasn't possible due to: 
		// - we want to have BOTH WaitEventID & WaitEventDesc in the "waitType"
		// - we want to discard by BOTH WaitEventID & WaitClassDesc
		// So we needed to create specialized SQL etc...
//		_CmSysWaits_byClass     = createTsStackedBarChart(conn, "CmSysWaits", "WaitClassDesc", s, null, "WaitTime", null, null, "Wait Classes in seconds, grouped by 10 minutes intervall");
//		_CmSysWaits_byEvent     = createTsStackedBarChart(conn, "CmSysWaits", "WaitEventDesc", s, null, "WaitTime", null, null, "Wait Events  in seconds, grouped by 10 minutes intervall");

		String schema = getReportingInstance().getDbmsSchemaName();
		
		//----------------------------------------
		// High level -- CLASS
		//----------------------------------------
		_CmSysWaits_byClass = new ReportChartTimeSeriesStackedBar(this, conn, schema, "CmSysWaits", "WaitClassDesc", samplePeriod, null, "WaitTime", null, null, null, "Wait Classes in Seconds, grouped by " + samplePeriod + " minutes intervall")
		{
			@Override
			protected String getSql()
			{
				int samplePeriod = getSamplePeriodInMinutes();

				String sql = ""
					    + "select \n"
					    + "     DATEADD(MINUTE, DATEDIFF(MINUTE, '2000-01-01', [SessionSampleTime]) / " + samplePeriod + " * " + samplePeriod + ", '2000-01-01') as [Period] \n"
					    + "    ,[WaitClassDesc] \n"
					    + "    ,sum([WaitTime]) as [WaitTimeInSec] \n"
					    + "from [CmSysWaits_diff] \n"
					    + "where 1=1 \n"
						+ getReportPeriodSqlWhere()
					    + "  and [WaitEventID] not in (178, 250) \n"
					    + "  and [WaitClassDesc] not in ('waiting for internal system event') \n"
					    + "  and [Waits] > 0 \n"
					    + "group by DATEADD(MINUTE, DATEDIFF(MINUTE, '2000-01-01', [SessionSampleTime]) / " + samplePeriod + " * " + samplePeriod + ", '2000-01-01'), [WaitClassDesc] \n"
					    + "order by 1, 2 \n"
					    + "";
				return sql;
			}
		};




		//----------------------------------------
		// Lower level -- EVENT ID (top report section)
		//----------------------------------------
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
				sb.append("  <th>WaitTime__chart</th> \n");
				sb.append("  <th>").append(valueColName).append("</th> \n");
				sb.append("  <th>WaitTime as HH:MM:SS</th> \n");
				sb.append("  <th>WaitTimePerWait__chart</th> \n");
				sb.append("  <th>SAP Wiki</th> \n");
				sb.append("</tr> \n");

				String sparklineJavaScriptInitCode_wt   = "";
				String sparklineJavaScriptInitCode_wtpc = "";
				int cnt = 1;
				for (Entry<String, Double> entry : getTop().entrySet())
				{
					String waitEventId = StringUtils.substringBetween(entry.getKey(), "[", "]");

					String sparklineDataStr_wt   = "";
					String sparklineDataStr_wtpc = "";

					//---- WaitTime -------------------------------------
					{
						// Below is to create a "spark-line" or "mini-chart" for the wait time...
						String sparklineClassName      = "sparklines_aseWaitTime__chart";
						String sparklineTooltipPostfix = " - SUM 'WaitTime' (in seconds) for below period";
						//String sparklineDataStr        = "";
						int groupByMinutes = 10;

						try
						{
							SparklineResult result = SparklineHelper.getSparclineData(
									conn,
									DataSource.CounterModel,
									getReportingInstance().getReportPeriodOrRecordingBeginTime().toLocalDateTime(),
									getReportingInstance().getReportPeriodOrRecordingEndTime()  .toLocalDateTime(), 
									groupByMinutes,                                     // params.getGroupDataInMinutes()
									AggType.SUM,                                        // params.getGroupDataAggregationType()
									null,                                               // params.getDbmsSchemaName()
									"CmSysWaits_diff",                                  // params.getDbmsTableName()
									"SessionSampleTime",                                // params.getDbmsSampleTimeColumnName()
									"WaitTime",                                         // params.getDbmsDataValueColumnName()
									false,                                              // params.getDbmsDataValueColumnNameIsExpression()
									Arrays.asList("WaitEventID"),                       // colNameList
									Arrays.asList(StringUtil.parseInt(waitEventId, 0)), // colValList
									null,                                               // params.getDbmsExtraWhereClause()
									0,                                                  // params.getNoDataInGroupDefaultValue()
									0);                                                 // params.getDecimalScale()             

//							String valStr              = StringUtil.toCommaStr(result.values, ",");
							String jfreeChartInlinePng = SparklineJfreeChart.create(result);

							// Create the data that will be in <td>...HERE...</td>
//							sparklineDataStr_wt = "<div class='" + sparklineClassName + "' values='" + valStr + "'>" + jfreeChartInlinePng + "</div>";
							sparklineDataStr_wt = SparklineHelper.getSparklineDiv(result, sparklineClassName, jfreeChartInlinePng);
							
							// Create JavaScript code to initialize all SparkLines with a specific class name
							if (StringUtil.isNullOrBlank(sparklineJavaScriptInitCode_wt))
								sparklineJavaScriptInitCode_wt = SparklineHelper.getJavaScriptInitCode(AseWaitStats.this, result, sparklineClassName, sparklineTooltipPostfix);
						}
						catch (SQLException ex)
						{
							sparklineDataStr_wt = "Problems getting data, Caught: " + ex;
						}
					}
					//---- WaitTimePerWait -------------------------------------
					{
						// Below is to create a "spark-line" or "mini-chart" for the wait time...
						String sparklineClassName      = "sparklines_aseWaitTimePerWait__chart";
						String sparklineTooltipPostfix = " - MAX 'WaitTimePerWait' (in seconds) for below period";
						//String sparklineDataStr        = "";
						int groupByMinutes = 10;

						try
						{
							SparklineResult result = SparklineHelper.getSparclineData(
									conn,
									DataSource.CounterModel,
									getReportingInstance().getReportPeriodOrRecordingBeginTime().toLocalDateTime(),
									getReportingInstance().getReportPeriodOrRecordingEndTime()  .toLocalDateTime(), 
									groupByMinutes,                                     // params.getGroupDataInMinutes()
									AggType.MAX,                                        // params.getGroupDataAggregationType()
									null,                                               // params.getDbmsSchemaName()
									"CmSysWaits_diff",                                  // params.getDbmsTableName()
									"SessionSampleTime",                                // params.getDbmsSampleTimeColumnName()
									"WaitTimePerWait",                                  // params.getDbmsDataValueColumnName()
									false,                                              // params.getDbmsDataValueColumnNameIsExpression()
									Arrays.asList("WaitEventID"),                       // colNameList
									Arrays.asList(StringUtil.parseInt(waitEventId, 0)), // colValList
									null,                                               // params.getDbmsExtraWhereClause()
									0,                                                  // params.getNoDataInGroupDefaultValue()
									3);                                                 // params.getDecimalScale()             

//							String valStr              = StringUtil.toCommaStr(result.values, ",");
							String jfreeChartInlinePng = SparklineJfreeChart.create(result);

							// Create the data that will be in <td>...HERE...</td>
//							sparklineDataStr_wtpc = "<div class='" + sparklineClassName + "' values='" + valStr + "'>" + jfreeChartInlinePng + "</div>";
							sparklineDataStr_wtpc = SparklineHelper.getSparklineDiv(result, sparklineClassName, jfreeChartInlinePng);
							
							// Create JavaScript code to initialize all SparkLines with a specific class name
							if (StringUtil.isNullOrBlank(sparklineJavaScriptInitCode_wtpc))
								sparklineJavaScriptInitCode_wtpc = SparklineHelper.getJavaScriptInitCode(AseWaitStats.this, result, sparklineClassName, sparklineTooltipPostfix);
						}
						catch (SQLException ex)
						{
							sparklineDataStr_wtpc = "Problems getting data, Caught: " + ex;
						}
					}
					
					String formatedWaitTimeMs = ResultSetTableModel.renderHighlightSortColumnForHtml("WaitTime", NumberFormat.getInstance().format(entry.getValue()));
							
					sb.append("<tr> \n");
					sb.append("  <td>").append(cnt++           ).append("</td> \n");
					sb.append("  <td>").append(entry.getKey()  ).append("</td> \n");
					sb.append("  <td>").append(sparklineDataStr_wt).append("</td> \n");
					sb.append("  <td>").append(formatedWaitTimeMs).append("</td> \n");
					sb.append("  <td>").append(TimeUtils.secToTimeStrLong(entry.getValue().intValue())).append("</td> \n");
					sb.append("  <td>").append(sparklineDataStr_wtpc).append("</td> \n");
					sb.append("  <td> <a href='https://wiki.scn.sap.com/wiki/display/SYBASE/ASE+Wait+Event+" + waitEventId + "' target='_blank'>WaitEventID - " + waitEventId + "</a>").append("</td> \n");
					sb.append("</tr> \n");
				}
				sb.append("</table> \n");
				sb.append("<br> \n");
				sb.append("All WaitEventID's from SAP Wiki, can be found <a href='https://wiki.scn.sap.com/wiki/display/SYBASE/ASE+Wait+Events+1+to+49' target='_blank'>here</a>\n");
				sb.append("<br> \n");
				sb.append("Or at <a href='https://help.sap.com/viewer/product/SAP_ASE?expandAll=true' target='_blank'>help.sap.com</a>, manual '<i>Performance and Tuning Series: Monitoring Tables</i>', section '<i>Wait Events</i>'... <a href='https://help.sap.com/viewer/9a57ec3c9937415eb6bf8c5abc91ca86/16.0.3.9/en-US/a8dc5707bc2b10149755d58b18eacb29.html' target='_blank'>here is a direct link</a>\n");
				sb.append("<br> \n");
				
				sb.append(sparklineJavaScriptInitCode_wt);
				sb.append(sparklineJavaScriptInitCode_wtpc);
					
				return sb.toString();
			}
			
			@Override
			protected String getTopGroupsSql()
			{
				String sql = ""
						+ "select top " + getTopCount() + " \n"
					    + "     [WaitEventID] \n"
					    + "    ,[WaitEventDesc] \n"
					    + "    ,sum([WaitTime]) as [WaitTimeInSec] \n"
					    + "from [CmSysWaits_diff] \n"
					    + "where 1=1 \n"
						+ getOwner().getReportEntry().getReportPeriodSqlWhere()
						+ "  and [WaitTime] > 0 \n"
					    + "  and [WaitEventID] not in (178, 250) \n"
					    + "  and [WaitClassDesc] not in ('waiting for internal system event') \n"
						+ "group by [WaitEventID], [WaitEventDesc] \n"
						+ "order by 3 desc \n"
						+ "";

				return sql;
			}

			@Override
			public TopGroupDataRecord createRecord(ResultSet rs)
			throws SQLException
			{
				String WaitEventID   = rs.getString(1);
				String WaitEventDesc = rs.getString(2);
				double value         = rs.getDouble(3);

				return new TopGroupDataRecord("[" + WaitEventID + "] " + WaitEventDesc, value);
			}
		};

		//----------------------------------------
		// Lower level -- EVENT ID
		//----------------------------------------
		_CmSysWaits_byEvent = new ReportChartTimeSeriesStackedBar(this, conn, schema, "CmSysWaits", "WaitEventDesc", samplePeriod, tgcp, "WaitTime", null, null, null, "Wait Events (top-"+tgcp.getTopCount()+"-WaitEvents) in Seconds, grouped by 10 minutes intervall")
		{
			@Override
			protected String getSql()
			{
				int samplePeriod = getSamplePeriodInMinutes();

				String sql = ""
					    + "select \n"
					    + "     DATEADD(MINUTE, DATEDIFF(MINUTE, '2000-01-01', [SessionSampleTime]) / " + samplePeriod + " * " + samplePeriod + ", '2000-01-01') as [Period] \n"
					    + "    ,[WaitEventID] \n"
					    + "    ,[WaitEventDesc] \n"
					    + "    ,sum([WaitTime]) as [WaitTimeInSec] \n"
					    + "from [CmSysWaits_diff] \n"
					    + "where 1=1 \n"
						+ getReportEntry().getReportPeriodSqlWhere()
// No need to restricts using below... if we use the above: TopGroupCountReport
//					    + "  and [WaitEventID] not in (178, 250) \n"
//					    + "  and [WaitClassDesc] not in ('waiting for internal system event') \n"
					    + "  and [Waits] > 0 \n"
					    + "group by DATEADD(MINUTE, DATEDIFF(MINUTE, '2000-01-01', [SessionSampleTime]) / " + samplePeriod + " * " + samplePeriod + ", '2000-01-01'), [WaitEventID], [WaitEventDesc] \n"
					    + "order by 1, 2 \n"
					    + "";
				return sql;
			}
			
			@Override
			public DataRecord createRecord(ResultSet rs) throws SQLException
			{
				Timestamp ts            = rs.getTimestamp(1);
				int       WaitEventID   = rs.getInt      (2);
				String    WaitEventDesc = rs.getString   (3);
				double    value         = rs.getDouble   (4);

				return new DataRecord(ts, "[" + WaitEventID + "] " + WaitEventDesc, value);
			}
		};
	}

	private IReportChart _CmSysWaits_byClass;
	private IReportChart _CmSysWaits_byEvent;

}
