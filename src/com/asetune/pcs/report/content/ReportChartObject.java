/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.pcs.report.content;

import java.awt.BasicStroke;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;

import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;

public class ReportChartObject
{
	private static Logger _logger = Logger.getLogger(ReportChartObject.class);

	private JFreeChart _chart;
	private XYDataset  _dataset;

	private String     _problem;
	private Exception  _exception;

	
	private ReportEntryAbstract _reportEntry;
	private DbxConnection _conn;
	private String        _cmName;
	private String        _graphName;
	private int           _maxValue;
	private String        _skipNames;
	private String        _graphTitle;
	
	public ReportChartObject(ReportEntryAbstract reportEntry, DbxConnection conn, String cmName, String graphName, int maxValue, String skipNames, String graphTitle)
	{
		_reportEntry = reportEntry;
		_conn        = conn;
		_cmName      = cmName;
		_graphName   = graphName;
		_maxValue    = maxValue;
		_skipNames   = skipNames;
		_graphTitle  = graphTitle;

		try
		{
			// Create the dataset
			XYDataset _dataset = createDataset(_conn, _cmName, _graphName, _skipNames);

			// Create the graph/chart
			if (_dataset != null)
			{
				_chart = createChart(_dataset, _graphTitle, null, null, _maxValue);
			}
		}
		catch (RuntimeException rte)
		{
			_logger.warn("Problems creating ReportChartObject, caught RuntimeException.", rte);
		}
	}

//	public void setDataset(XYDataset dataset) { _dataset = dataset; }
//	public void setChart(JFreeChart chart)    { _chart   = chart;   }
	public void setProblem(String problem)    { _problem = problem;   }
	public void setException(Exception ex)    { _exception = ex;   }
	
	public String getHtmlContent(String preText, String postText)
	{
		StringBuilder sb = new StringBuilder();

		// Any PROBLEMS... return at the end of this block
		if (StringUtil.hasValue(_problem))
		{
			sb.append(_problem);
			sb.append("<br>\n");

			if (_exception != null)
			{
				sb.append("<b>Exception:</b> ").append(_exception.toString()).append("<br>\n");
				sb.append("<pre>");
				sb.append(StringUtil.exceptionToString(_exception));
				sb.append("</pre>");
			}

			return sb.toString();
		}


		// Pre Text
		if (StringUtil.hasValue(preText))
		{
			sb.append(preText);
			sb.append("<br>\n");
		}

		// CHART
		sb.append(toHtmlInlineImage());
		sb.append("<br>\n");

		// Post Text
		if (StringUtil.hasValue(postText))
		{
			sb.append(postText);
			sb.append("<br>\n");
		}

		return sb.toString();
	}

	public String toHtmlInlineImage()
	{
		try
		{
//			OutputStream out = new FileOutputStream("c:/tmp/xxx.png");
			ByteArrayOutputStream bo = new ByteArrayOutputStream();
			Base64OutputStream b64o = new Base64OutputStream(bo, true);

			int width  = 1900;
			int height = 300;
//			ChartUtilities.writeChartAsPNG(b64o, _chart, 2048, 300);
			ChartUtilities.writeChartAsPNG(b64o, _chart, width, height );

			String base64Str = new String(bo.toByteArray());
			
			String htmlStr = "<img width='" + width + "' height='" + height + "' src='data:image/png;base64," + base64Str + "'>";
			return htmlStr;
		}
		catch (IOException ex)
		{
			_exception = ex;
			_problem = ex.toString();

			return "";
		}
	}




	//----------------------------------------------------------
	// Below are code for creating Graph/Chart images that can be included in the HTML Report
	//----------------------------------------------------------

	private int _defaultQueryTimeout = 0; 

//	public String getDbxCentralLinkForGraphs(String graphList)
//	{
//		String dbxcLink = getReportingInstance().getDbxCentralBaseUrl();
//		RecordingInfo recordingInfo = getReportingInstance().getInstanceRecordingInfo();
//
//		String startTime = recordingInfo.getStartTime();
//		String endTime   = recordingInfo.getEndTime();
//		String srvName   = getReportingInstance().getServerName();
////		String graphList = "CmSummary_aaCpuGraph,CmEngines_cpuSum,CmEngines_cpuEng,CmSysLoad_EngineRunQLengthGraph";
//
//		if (StringUtil.hasValue(startTime)) startTime = startTime.substring(0, "YYYY-MM-DD".length()) + " 00:00";
//		if (StringUtil.hasValue(endTime))   endTime   = endTime  .substring(0, "YYYY-MM-DD".length()) + " 23:59";
//		
//		String dbxCentralLink = dbxcLink + "/graph.html"
//				+ "?subscribe=false"
//				+ "&cs=dark"
//				+ "&gcols=1"
//				+ "&sessionName=" + srvName 
//				+ "&startTime="   + startTime 
//				+ "&endTime="     + endTime 
//				+ "&graphList="   + graphList 
//				+ "";
//		
//		return dbxCentralLink;
//	}

//	public String toHtmlInlineImage(JFreeChart chart)
//	{
//		try
//		{
////			OutputStream out = new FileOutputStream("c:/tmp/xxx.png");
//			ByteArrayOutputStream bo = new ByteArrayOutputStream();
//			Base64OutputStream b64o = new Base64OutputStream(bo, true);
//
//			int width  = 1900;
//			int height = 300;
////			ChartUtilities.writeChartAsPNG(b64o, chart, 2048, 300);
//			ChartUtilities.writeChartAsPNG(b64o, chart, width, height );
//
//			String base64Str = new String(bo.toByteArray());
//			
//			String htmlStr = "<img width='" + width + "' height='" + height + "' src='data:image/png;base64," + base64Str + "'><br>";
//			return htmlStr;
//		}
//		catch (IOException ex)
//		{
//			ex.printStackTrace();
//			return "";
//		}
//	}

//	public JFreeChart createChart(DbxConnection conn, String cmName, String graphName, boolean isPercentGraph, String graphTitle)
//	{
//		XYDataset dataset = createDataset(conn, cmName, graphName);
////		JFreeChart chart = createChart(dataset, graphTitle, "TIME", "VALUE");
//		JFreeChart chart = createChart(dataset, graphTitle, null, null, isPercentGraph ? 100 : -1);
//		return chart;
//	}

	public JFreeChart createChart(DbxConnection conn, String cmName, String graphName, int maxValue, String skipNames, String graphTitle)
	{
		// Create the dataset
		XYDataset dataset = createDataset(conn, cmName, graphName, skipNames);
		if (dataset == null)
			return null;
		
		// Create the graph/chart
		JFreeChart chart = createChart(dataset, graphTitle, null, null, maxValue);
		return chart;
	}

	private JFreeChart createChart(XYDataset dataset, String graphTitle, String timeAxisLabel, String valueAxisLabel, int maxValue) 
	{
		JFreeChart chart = ChartFactory.createTimeSeriesChart(graphTitle, timeAxisLabel, valueAxisLabel, dataset, true, true, false);
		XYPlot plot = (XYPlot) chart.getPlot();
//		plot.setDomainCrosshairVisible(true);
//		plot.setRangeCrosshairVisible(true);

		// Set thicker lines
		boolean thickLines = false;
		if (thickLines)
		{
			int seriesCount = plot.getSeriesCount();
			for (int i = 0; i < seriesCount; i++)
				plot.getRenderer().setSeriesStroke(i, new BasicStroke(2));
		}

//		XYItemRenderer r = plot.getRenderer();
//		if (r instanceof XYLineAndShapeRenderer) 
//		{
//			XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
//			renderer.setBaseShapesVisible(true);
//			renderer.setBaseShapesFilled(true);
//		}
		DateAxis axis = (DateAxis) plot.getDomainAxis();
//		axis.setDateFormatOverride(new SimpleDateFormat("dd-MMM-yyyy HH:mm"));
		axis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));
		
		// for Percent Graph: set MAX to 100%
		if (maxValue > 0)
		{
			NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
			rangeAxis.setRange(0.0, maxValue);
		}

		return chart;
	}

	private XYDataset createDataset(DbxConnection conn, String cmName, String graphName, String skipNames)
	{
		if (StringUtil.isNullOrBlank(cmName))     throw new IllegalArgumentException("cmName can't be null or blank");
		if (StringUtil.isNullOrBlank(graphName))  throw new IllegalArgumentException("graphName can't be null or blank");

		//----------------------------------------
		// TYPICAL look of a graph table
		//----------------------------------------
		// CREATE TABLE "CMengineActivity_cpuSum"
		//   "SessionStartTime"   DATETIME        NOT NULL,
		//   "SessionSampleTime"  DATETIME        NOT NULL,
		//   "SampleTime"         DATETIME        NOT NULL,
		//   "label_0"            VARCHAR(30)         NULL,
		//   "data_0"             NUMERIC(10, 1)      NULL,
		//   "label_1"            VARCHAR(30)         NULL,
		//   "data_1"             NUMERIC(10, 1)      NULL,
		//   "label_2"            VARCHAR(30)         NULL,
		//   "data_2"             NUMERIC(10, 1)      NULL

		List<String> skipNameList = StringUtil.parseCommaStrToList(skipNames);
		
		String tabName   = cmName + "_" + graphName;
		String sql = 
				 "select * "
				+" from [" + tabName + "] \n"
//				+" where [SessionSampleTime] >= " + whereSessionSampleTime
				+" order by [SessionSampleTime] \n"
				;
		
		sql = conn.quotifySqlString(sql);

		// Early exit if the table do not exist
		boolean tabExists = DbUtils.checkIfTableExistsNoThrow(conn, null, null, tabName);
		if ( ! tabExists )
		{
			_logger.info("createDataset(): The table '" + tabName + "' did not exist.");
			return null;
		}

		int readCount = 0;

		// autoclose: stmnt, rs
		try (Statement stmnt = conn.createStatement())
		{
			Map<String, TimeSeries> graphSeriesMap = new LinkedHashMap<>(); 

			// set TIMEOUT
			stmnt.setQueryTimeout(_defaultQueryTimeout);

			// Execute and read result
			try (ResultSet rs = stmnt.executeQuery(sql))
			{
				ResultSetMetaData md = rs.getMetaData();
				int colCount = md.getColumnCount();
				
				int colDataStart = 4; // "SessionStartTime", "SessionSampleTime", "CmSampleTime", "System+User CPU (@@cpu_busy + @@cpu_io)"

				String[] labels = new String[(colCount-3)/2];
				Double[] datapt = new Double[(colCount-3)/2];
				
				List<String> labelNames = new LinkedList<>();
				for (int c=colDataStart; c<colCount+1; c++)
					labelNames.add( md.getColumnLabel(c) );

				while (rs.next())
				{
					readCount++;

				//	Timestamp sessionStartTime  = rs.getTimestamp(1);
					Timestamp sessionSampleTime = rs.getTimestamp(2);
				//	Timestamp cmSampleTime      = rs.getTimestamp(3);

					Timestamp ts = sessionSampleTime;
					
					// Start to read column 4
					// move c (colIntex) 2 cols at a time, move ca (ArrayIndex) by one
					for (int c=4, ca=0; c<=colCount; c+=2, ca++)
					{
						labels[ca] = rs.getString(c);
						datapt[ca] = new Double(rs.getDouble(c+1));					
					}

					for (int ca=0; ca<datapt.length; ca++)
					{
						String label     = labels[ca];
						Double dataValue = datapt[ca];

						if (label == null)
							label = "colnum-"+ca;
						
						boolean addEntry = true;

						// SKIP any labels that are in the SkipList
						for (String skipName : skipNameList)
						{
							if (label.startsWith(skipName))
								addEntry = false;
						}

						if (addEntry)
						{
							// Grab TimeSeries object
							TimeSeries graphTimeSerie = graphSeriesMap.get(label);
							if (graphTimeSerie == null)
							{
								graphTimeSerie = new TimeSeries(label);
								graphSeriesMap.put(label, graphTimeSerie);
							}
							
							// Add the dataPoint to the TimeSeries
							//graphTimeSerie.add(new Millisecond(ts), dataValue);
							graphTimeSerie.addOrUpdate(new Millisecond(ts), dataValue);
						}
					}
				}
			}

			TimeSeriesCollection dataset = new TimeSeriesCollection();
			for (TimeSeries dbxGraphData : graphSeriesMap.values())
			{
				dataset.addSeries(dbxGraphData);
			}

			return dataset;
		}
		catch (SQLException ex)
		{
			_logger.error("Problems in createDataset() when fetching DB data for cmName='" + cmName + "', grapgName='" + graphName + "'.", ex);

			setProblem("Problems in createDataset() when fetching DB data for cmName='" + cmName + "', grapgName='" + graphName + "'.");
			setException(ex);
			
			//_fullRstm = ResultSetTableModel.createEmpty(name);
//			_logger.warn("Problems getting '" + name + "': " + ex);
			
			return null;
		}
	}

	public XYDataset createDataset_NEW_NOT_USED(DbxConnection conn, String cmName, String graphName)
	{
		if (StringUtil.isNullOrBlank(cmName))     throw new IllegalArgumentException("cmName can't be null or blank");
		if (StringUtil.isNullOrBlank(graphName))  throw new IllegalArgumentException("graphName can't be null or blank");

		String tabName   = cmName + "_" + graphName;
		String sql = 
				 "select * "
				+" from [" + tabName + "] \n"
//				+" where [SessionSampleTime] >= " + whereSessionSampleTime
				+" order by [SessionSampleTime] \n"
				;
		
		sql = conn.quotifySqlString(sql);
		
		int readCount = 0;

		Map<String, TimeSeries> graphSeriesMap = new LinkedHashMap<>(); 

		// autoclose: stmnt, rs
		try (Statement stmnt = conn.createStatement())
		{
			// set TIMEOUT
			stmnt.setQueryTimeout(_defaultQueryTimeout);

			// Execute and read result
			try (ResultSet rs = stmnt.executeQuery(sql))
			{
				ResultSetMetaData md = rs.getMetaData();
				int colCount = md.getColumnCount();
				
				int colDataStart = 4; // "SessionStartTime", "SessionSampleTime", "CmSampleTime", "System+User CPU (@@cpu_busy + @@cpu_io)"
				
				List<String> labelNames = new LinkedList<>();
				for (int c=colDataStart; c<colCount+1; c++)
					labelNames.add( md.getColumnLabel(c) );

				while (rs.next())
				{
					readCount++;

				//	Timestamp sessionStartTime  = rs.getTimestamp(1);
					Timestamp sessionSampleTime = rs.getTimestamp(2);
				//	Timestamp cmSampleTime      = rs.getTimestamp(3);

					Timestamp ts = sessionSampleTime;
					
					for (int c=colDataStart, l=0; c<colCount+1; c++, l++)
					{
						String label = labelNames.get(l);
//						double dataValue = rs.getDouble(c);
						Double dataValue = rs.getDouble(c);

						// Grab TimeSeries object
						TimeSeries graphTimeSerie = graphSeriesMap.get(label);
						if (graphTimeSerie == null)
						{
							graphTimeSerie = new TimeSeries(label);
							graphSeriesMap.put(label, graphTimeSerie);
						}
						
						// Add the dataPoint to the TimeSeries
						graphTimeSerie.add(new Millisecond(ts), dataValue);
					}
				}
			}
		}
		catch (SQLException ex)
		{
			setProblem("Problems in createDataset() when fetching DB data for cmName='" + cmName + "', grapgName='" + graphName + "'.");
			setException(ex);
			
			//_fullRstm = ResultSetTableModel.createEmpty(name);
//			_logger.warn("Problems getting '" + name + "': " + ex);
		}

		TimeSeriesCollection dataset = new TimeSeriesCollection();
		for (TimeSeries dbxGraphData : graphSeriesMap.values())
		{
			dataset.addSeries(dbxGraphData);
		}

		return dataset;
	}

//	public XYDataset createGraphDataset(List<DbxGraphData> data)
//	{
//		String graphLabel = "";
//
//		Map<String, TimeSeries> graphSeriesMap = new LinkedHashMap<>(); 
//		
//		for (DbxGraphData entry : data)
//		{
//			graphLabel = entry.getGraphLabel();
//			Timestamp ts = entry.getSessionSampleTime();
//
//			Map<String, Double> dataPoint = entry.getData();
//			for (Entry<String, Double> dpe : dataPoint.entrySet())
//			{
//				String seriesName = dpe.getKey();
//				Double dataValue  = dpe.getValue();
//				
//				TimeSeries graphTimeSerie = graphSeriesMap.get(seriesName);
//				if (graphTimeSerie == null)
//				{
//					graphTimeSerie = new TimeSeries(seriesName);
//					graphSeriesMap.put(seriesName, graphTimeSerie);
//				}
//				
//				graphTimeSerie.add(new Millisecond(ts), dataValue);
//			}
//		}
//
//		TimeSeriesCollection dataset = new TimeSeriesCollection();
//		for (TimeSeries dbxGraphData : graphSeriesMap.values())
//		{
//			dataset.addSeries(dbxGraphData);
//		}
//
//		return dataset;
//	}
//	public List<DbxGraphData> getGraphData(DbxConnection conn, String cmName, String graphName, String graphLabel, String  graphProps, String  graphCategory, boolean isPercentGraph)
//	{
//		if (StringUtil.isNullOrBlank(cmName))     throw new IllegalArgumentException("cmName can't be null or blank");
//		if (StringUtil.isNullOrBlank(graphName))  throw new IllegalArgumentException("graphName can't be null or blank");
//		if (StringUtil.isNullOrBlank(graphLabel)) throw new IllegalArgumentException("graphLabel can't be null or blank");
//
//		if (StringUtil.isNullOrBlank(graphProps))
//			graphProps = "#{#yAxisScaleLabels#:{#name#:#percent#, #s0#:# %#, #s1#:# %#, #s2#:# %#, #s3#:# %#, #s4#:# %#}}#".replace('#', '"');
//		
//		if (StringUtil.isNullOrBlank(graphCategory))
//			graphCategory = "-UNKNOWN-";
//		
//		String tabName   = cmName + "_" + graphName;
//		String sql = 
//				 "select * "
//				+" from [" + tabName + "] \n"
////				+" where [SessionSampleTime] >= " + whereSessionSampleTime
//				+" order by [SessionSampleTime] \n"
//				;
//		
//		int readCount = 0;
//		List<DbxGraphData> list = new ArrayList<>();
//
//		// autoclose: stmnt, rs
//		try (Statement stmnt = conn.createStatement())
//		{
//			// set TIMEOUT
//			stmnt.setQueryTimeout(_defaultQueryTimeout);
//
//			// Execute and read result
//			try (ResultSet rs = stmnt.executeQuery(sql))
//			{
//				ResultSetMetaData md = rs.getMetaData();
//				int colCount = md.getColumnCount();
//				
//				int colDataStart = 4; // "SessionStartTime", "SessionSampleTime", "CmSampleTime", "System+User CPU (@@cpu_busy + @@cpu_io)"
//				
//				List<String> labelNames = new LinkedList<>();
//				for (int c=colDataStart; c<colCount+1; c++)
//					labelNames.add( md.getColumnLabel(c) );
//
//				while (rs.next())
//				{
//					readCount++;
//					LinkedHashMap<String, Double> labelAndDataMap = new LinkedHashMap<>();
//
//				//	Timestamp sessionStartTime  = rs.getTimestamp(1);
//					Timestamp sessionSampleTime = rs.getTimestamp(2);
//				//	Timestamp cmSampleTime      = rs.getTimestamp(3);
//					
//					for (int c=colDataStart, l=0; c<colCount+1; c++, l++)
//					{
//						labelAndDataMap.put( labelNames.get(l), rs.getDouble(c) );
//					}
//
//					DbxGraphData e = new DbxGraphData(cmName, graphName, sessionSampleTime, graphLabel, graphProps, graphCategory, isPercentGraph, labelAndDataMap);
//					list.add(e);
//					
//					// Remove already added records (keep only last X number of rows)
////					if (onlyLastXNumOfRecords > 0)
////					{
////						if (list.size() > onlyLastXNumOfRecords)
////							list.remove(0);
////					}
//				}
//			}
//		}
//		catch (SQLException ex)
//		{
//			setProblem(ex);
//			
//			//_fullRstm = ResultSetTableModel.createEmpty(name);
////			_logger.warn("Problems getting '" + name + "': " + ex);
//		}
//
//		// For debugging purposes
//		if (readCount == 0)
//		{
////			_logger.info("NO Records was found: getGraphData(sessionName='"+sessionName+"', cmName='"+cmName+"', graphName='"+graphName+"', startTime='"+startTime+"', endTime='"+endTime+"', sampleType="+sampleType+", sampleValue="+sampleValue+") SQL=|"+sql+"|, readCount="+readCount+", retListSize="+list.size());
//		}
//		
//		return list;
//	}
}
