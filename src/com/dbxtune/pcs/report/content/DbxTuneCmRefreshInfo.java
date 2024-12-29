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
package com.dbxtune.pcs.report.content;

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jfree.data.general.Dataset;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.dbxtune.SqlServerTune;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

public class DbxTuneCmRefreshInfo
extends ReportEntryAbstract
{
	private static Logger _logger = Logger.getLogger(DbxTuneCmRefreshInfo.class);

	private ResultSetTableModel _rstm1;
	private ResultSetTableModel _rstm2;
	

	public DbxTuneCmRefreshInfo(DailySummaryReportAbstract reportingInstance)
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
		if (_rstm1.getRowCount() == 0)
		{
			w.append("No Counter Models was found in <code>MonSessionSampleDetailes</code> <br>\n");
		}
		else
		{
			// Get a description of this section, and column names
			w.append(getSectionDescriptionHtml(_rstm1, true));

			w.append("Table Count: " + _rstm2.getRowCount() + "<br>\n");
			w.append(toHtmlTable(_rstm1));
		}


		if (_rstm2.getRowCount() == 0)
		{
			w.append("No Counter Models SQL Refresh Time was found in <code>MonSessionSampleDetailes</code> <br>\n");
		}
		else
		{
			// Get a description of this section, and column names
			w.append(getSectionDescriptionHtml(_rstm2, true));
			
			w.append("Table Count: " + _rstm2.getRowCount() + "<br>\n");
			w.append(toHtmlTable(_rstm2));
		}

		boolean printGraph_MonSessionSampleDetailes = Configuration.getCombinedConfiguration().getBooleanProperty("CmRefreshInfo.print.graph.MonSessionSampleDetailes", false);
		boolean printGraph_CmSummary_CmRefreshTime  = Configuration.getCombinedConfiguration().getBooleanProperty("CmRefreshInfo.print.graph.CmSummary_CmRefreshTime" , true);

//		if (_cm_sqlRefreshTimeGraph_MonSessionSampleDetailes == null)
//			printGraph_CmSummary_CmRefreshTime = true;
		
		if (_cm_sqlRefreshTimeGraph_CmSummary_CmRefreshTime == null)
			printGraph_MonSessionSampleDetailes = true;
		
		if (_cm_sqlRefreshTimeGraph_MonSessionSampleDetailes != null && printGraph_MonSessionSampleDetailes)
		{
			w.append("<br><br>\n");
			_cm_sqlRefreshTimeGraph_MonSessionSampleDetailes.writeHtmlContent(w, null, null);
		}

		if (_cm_sqlRefreshTimeGraph_CmSummary_CmRefreshTime != null && printGraph_CmSummary_CmRefreshTime)
		{
			w.append("<br><br>\n");
			_cm_sqlRefreshTimeGraph_CmSummary_CmRefreshTime.writeHtmlContent(w, null, null);
		}
	}

	@Override
	public String getSubject()
	{
		return "Counter Models Refresh Info";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false;
	}

	//--------------------------------------------------------------------------------
	// BEGIN: Top rows
	//--------------------------------------------------------------------------------
//	public String getTopRowsPropertyName()
//	{
//		return getClass().getSimpleName()+".top";
//	}
	@Override
	public int getTopRowsDefault()
	{
		return 50;
	}
//	public int getTopRows()
//	{
////		Configuration conf = getReportingInstance().getLocalConfig();
//		Configuration conf = Configuration.getCombinedConfiguration();
//		int topRows = conf.getIntProperty(getTopRowsPropertyName(), getTopRowsDefault());
//		return topRows;
//	}
	//--------------------------------------------------------------------------------
	// END: Top rows
	//--------------------------------------------------------------------------------

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		// CM Refresh Info
		String sql = ""
			    + "select \n"
			    + "     [CmName] \n"
			    + "    ,count(*)                                                   as [Count_Collections] \n"
			    + "    ,cast(avg([sqlRefreshTime]) + avg([lcRefreshTime]) as int)  as [Avg_RefreshTimeMs] \n"
			    + "    ,cast(sum([sqlRefreshTime]) + sum([lcRefreshTime]) as int)  as [Sum_RefreshTimeMs] \n"
			    + "    ,cast(max([sqlRefreshTime]) + max([lcRefreshTime]) as int)  as [Max_RefreshTimeMs] \n"
			    + "    ,cast(min([sqlRefreshTime]) + min([lcRefreshTime]) as int)  as [Min_RefreshTimeMs] \n"
//			    + "    ,cast(avg([sqlRefreshTime])                        as int)  as [Avg_SqlRefreshTimeMs] \n"
//			    + "    ,cast(avg([lcRefreshTime])                         as int)  as [Avg_LcRefreshTimeMs] \n"
			    + "    ,cast(avg([absRows])                               as int)  as [Avg_absRows] \n"
			    + "    ,cast(max([absRows])                               as int)  as [Max_absRows] \n"
//			    + "    ,cast(avg([absRows])                               as numeric(15,1))  as [Avg_absRows] \n"
			    + "    ,count([exceptionMsg])                                      as [Count_exceptionMsg] \n"
			    + "from [MonSessionSampleDetailes] \n"
			    + "group by [CmName] \n"
			    + "order by [Sum_RefreshTimeMs] desc \n"
			    + "";
		_rstm1 = executeQuery(conn, sql, true, "CM Refresh Info");
		_rstm1.setDescription("<b>What CM's has been refreshed (order by 'Sum_RefreshTimeMs')</b>");


		// SQL Refresh Time
		int topRows = getTopRows();
		
		String extraWhereClause = "";
		String extraHtmlNote    = "";
		if (SqlServerTune.APP_NAME.equals(getReportingInstance().getRecDbxAppName()))
		{
			// Skip 'CmExecQueryStatPerDb' for this section
			extraWhereClause = "      and [CmName] != 'CmExecQueryStatPerDb' \n";
			extraHtmlNote    = "<b>NOTE:</b> In below table skipping <code>CmExecQueryStatPerDb</code> since it's <b>known</b> to take a long time...<br>\n";
		}

		sql = ""
			    + "with topRows AS \n"
			    + "( \n"
			    + "    select top " + topRows + " \n"
			    + "	         [SessionSampleTime] \n"
			    + "	        ,[CmName] \n"
			    + "         ,[sqlRefreshTime] + [lcRefreshTime] AS [refreshTime] \n"
			    + "	        ,[sqlRefreshTime] \n"
			    + "	        ,[lcRefreshTime] \n"
			    + "	        ,[absRows] \n"
			    + "	        ,[exceptionMsg] \n"
			    + "    from [MonSessionSampleDetailes] \n"
			    + "    where 1=1 \n"
			    + extraWhereClause
			    + "    order by [sqlRefreshTime] desc \n"
			    + ") \n"
			    + "select * \n"
			    + "from topRows \n"
			    + "order by [SessionSampleTime] \n"
			    + "";

		_rstm2 = executeQuery(conn, sql, true, "CM Top Refresh Time");
		_rstm2.setDescription("<br><br><br>"
				+ "<b>Report SQL Refresh times, so we can discover what CM is taking a long time to refresh</b><br>"
				+ extraHtmlNote);
		
		
		// set some values for a graph
		String schemaName = null; 
		String cmName     = "DbxTune";
		String graphName  = "CmSqlRefreshTime";
		int    maxValue   = -1;
		boolean sorted    = true;
		String skipNames  = null;
		String graphTitle = "Counter Models Refresh Time (in ms)";
		
		_cm_sqlRefreshTimeGraph_MonSessionSampleDetailes = new ReportChartTimeSeriesLine_CmSqlRefreshTime(this, conn, schemaName, cmName, graphName, maxValue, sorted, skipNames, graphTitle);

		
		// OR: Should we get data from: cmName="CmSummary", graphName="CmRefreshTime"
		//     That would also include "Total"...
		String schema = getReportingInstance().getDbmsSchemaName();

		_cm_sqlRefreshTimeGraph_CmSummary_CmRefreshTime = createTsLineChart(conn, schema, "CmSummary", "CmRefreshTime", -1, true, null, "Counter Models Refresh Time (in ms)");
	}


	private IReportChart _cm_sqlRefreshTimeGraph_MonSessionSampleDetailes;
	private IReportChart _cm_sqlRefreshTimeGraph_CmSummary_CmRefreshTime;
	






	/**
	 * 
	 */
	private static class ReportChartTimeSeriesLine_CmSqlRefreshTime
	extends ReportChartTimeSeriesLine
	{

		public ReportChartTimeSeriesLine_CmSqlRefreshTime(ReportEntryAbstract reportEntry, DbxConnection conn, String schemaName, String cmName, String graphName, int maxValue, boolean sorted, String skipNames, String graphTitle)
		{
			super(reportEntry, conn, schemaName, cmName, graphName, maxValue, sorted, skipNames, graphTitle);
		}
		
		@Override
		protected Dataset createDataset()
		{
			Dataset dataset = createDataset_local(getConnection());
			return dataset;
		}

		private Dataset createDataset_local(DbxConnection conn)
		{
//			String sql = ""
//				    + "select \n"
//				    + "     [SessionSampleTime] \n"
//				    + "    ,[CmName] \n"
//				    + "    ,[sqlRefreshTime] + [lcRefreshTime] AS [refreshTime] \n"
////				    + "    ,[sqlRefreshTime] \n"
////				    + "    ,[lcRefreshTime] \n"
//				    + "from [MonSessionSampleDetailes] \n"
//				    + "where 1=1 \n"
////				    + "  and [sqlRefreshTime] > 0 \n"
//					+ getReportEntry().getReportPeriodSqlWhere()
//				    + "order by [SessionSampleTime], [CmName] \n"
//				    + "";

			// The below is doing: Get all "dataPoints" for every "Sample Times" and every "CmName" ... If the "dataPoint", can't be found (for that sample/CmName), return a 0 
			// This output will make the "graph data points" go to "zero" when the CM has not been sampled at that specific "sample time"
			String sql = ""
				    + "WITH baseTs  AS (SELECT DISTINCT [SessionSampleTime] AS [baselineTs] FROM [MonSessionSampleDetailes] WHERE 1=1 " + getReportEntry().getReportPeriodSqlWhere() + ") \n"
				    + "    ,cmNames AS (SELECT DISTINCT [CmName]                            FROM [MonSessionSampleDetailes]) \n"
				    + "SELECT \n"
				    + "     baseTs .[baselineTs] \n"
				    + "    ,cmNames.[CmName] \n"
				    + "    ,COALESCE(sample .[sqlRefreshTime] + [lcRefreshTime], 0) AS [refreshTime] \n"
				    + "FROM baseTs \n"
				    + "CROSS JOIN cmNames \n"
				    + "LEFT OUTER JOIN [MonSessionSampleDetailes] sample ON sample.[SessionSampleTime] = baseTs.[baselineTs] AND sample.[CmName] = cmNames.[CmName] \n"
				    + "ORDER BY baseTs.[baselineTs], cmNames.[CmName] \n"
				    + "";
			
			sql = conn.quotifySqlString(sql);

			Map<String, TimeSeries> graphSeriesMap = new LinkedHashMap<>(); 

			// autoclose: stmnt, rs
			try (Statement stmnt = conn.createStatement())
			{
				// set TIMEOUT
				stmnt.setQueryTimeout(getDefaultQueryTimeout());

				// Execute and read result
				try (ResultSet rs = stmnt.executeQuery(sql))
				{
//					ResultSetMetaData md = rs.getMetaData();
//					int colCount = md.getColumnCount();
					
//					String cmName = rs.getString(2);
//					int colDataStart = 4; // "SessionStartTime", "SessionSampleTime", "CmSampleTime", "System+User CPU (@@cpu_busy + @@cpu_io)"
					
//					List<String> labelNames = new LinkedList<>();
//					for (int c=colDataStart; c<colCount+1; c++)
//						labelNames.add( md.getColumnLabel(c) );

					while (rs.next())
					{
//						readCount++;

						Timestamp ts     = rs.getTimestamp(1);
						String label     = rs.getString(2); // cmName
						Double dataValue = rs.getDouble(3); // refreshTime

						// Grab TimeSeries object
						TimeSeries graphTimeSerie = graphSeriesMap.get(label);
						if (graphTimeSerie == null)
						{
							graphTimeSerie = new TimeSeries(label);
							graphSeriesMap.put(label, graphTimeSerie);
						}

						// Add the dataPoint to the TimeSeries
						graphTimeSerie.add(new Millisecond(ts), dataValue);

						// Remember min/max value
						setDatasetMinMaxValue(dataValue);
					}
				}

				TimeSeriesCollection dataset = new TimeSeriesCollection();
				for (TimeSeries dbxGraphData : graphSeriesMap.values())
				{
					dataset.addSeries(dbxGraphData);
//System.out.println("dbxGraphData.getKey()=" + StringUtil.left(dbxGraphData.getKey().toString(),30) + ", dbxGraphData.getItemCount()=" + dbxGraphData.getItemCount() + ", getMaximumItemAge=" + dbxGraphData.getMaximumItemAge() + ", getMaximumItemCount=" + dbxGraphData.getMaximumItemCount());
//					for (int xxx=0; xxx<dbxGraphData.getItemCount(); xxx++)
//					{
//						TimeSeriesDataItem di = dbxGraphData.getDataItem(xxx);
//						System.out.println("     [" + xxx + "]: period=" + di.getPeriod() + ", val=" + di.getValue());
//					}
				}

				if (isSorted())
				{
					dataset = sortTimeSeriesCollection(dataset);
				}

				return dataset;
			}
			catch (SQLException ex)
			{
				_logger.error("Problems in createDataset() when fetching DB data for 'CM SQL Refresh Time'.", ex);

				setProblem("Problems in createDataset() when fetching DB data for 'CM SQL Refresh Time'.");
				setException(ex);
				
				return null;
			}
		}
	}
}
