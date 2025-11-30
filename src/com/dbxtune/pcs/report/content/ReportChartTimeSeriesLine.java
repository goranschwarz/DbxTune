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

import java.awt.BasicStroke;
import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.general.Dataset;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
import org.jfree.data.xy.XYDataset;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.StringUtil;

public class ReportChartTimeSeriesLine
extends ReportChartAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

//	public final static String SQL_WHERE_PREFIX = "SQL-WHERE:";
	public final static String SKIP_COLNAME_WITH_VALUE_ABOVE = "SKIP_COLNAME_WITH_VALUE_ABOVE:";
	public final static String SKIP_COLNAME_WITH_VALUE_BELOW = "SKIP_COLNAME_WITH_VALUE_BELOW:";
	
	private String        _graphName;
	private int           _maxValue;
	private boolean       _isSorted;
	private String        _skipNames;      // This is used two fold: skip "chart lines" OR skip some values that might be to "high/low" for the chart...

	public ReportChartTimeSeriesLine(ReportEntryAbstract reportEntry, DbxConnection conn, String schemaName, String cmName, String graphName, int maxValue, boolean sorted, String skipNames, String graphTitle)
	{
		super(reportEntry, conn, schemaName, ChartType.LINE, cmName, graphName, graphTitle, maxValue, sorted);
		
		_graphName       = graphName;
		_maxValue        = maxValue;
		_isSorted        = sorted;
		_skipNames       = skipNames;

		create();
	}

	@Override
	protected Dataset createDataset()
	{
		Dataset dataset = createDataset(getConnection(), getCmName(), _graphName, _skipNames);
		return dataset;
	}

	@Override
	protected JFreeChart createChart()
	{
//		return createChart(dataset, _graphTitle, null, null, _maxValue);
		return createChart(getDataset(), getGraphTitle(), null, null, _maxValue);
	}


//	public String  getGraphName() { return _graphName; }
	public int     getMaxValue()  { return _maxValue; }
	public boolean isSorted()     { return _isSorted; }
	public String  getSkipNames() { return _skipNames; }

	//----------------------------------------------------------
	// Below are code for creating Graph/Chart images that can be included in the HTML Report
	//----------------------------------------------------------

//	public JFreeChart createChart(DbxConnection conn, String cmName, String graphName, int maxValue, String skipNames, String graphTitle)
//	{
//		// Create the dataset
//		Dataset dataset = createDataset(conn, cmName, graphName, skipNames);
//		if (dataset == null)
//			return null;
//		
//		// Create the graph/chart
//		JFreeChart chart = createChart(dataset, graphTitle, null, null, maxValue);
//		return chart;
//	}

	private JFreeChart createChart(Dataset dataset, String graphTitle, String timeAxisLabel, String valueAxisLabel, int maxValue) 
	{
		if ( dataset == null )                  throw new RuntimeException("The passed dataset is null");
		if ( ! (dataset instanceof XYDataset) ) throw new RuntimeException("The passed dataset is of type '" + dataset.getClass() + "' it must implement 'XYDataset'.");

		JFreeChart chart = ChartFactory.createTimeSeriesChart(graphTitle, timeAxisLabel, valueAxisLabel, (XYDataset)dataset, true, false, false);
		XYPlot plot = (XYPlot) chart.getPlot();
//		plot.setDomainCrosshairVisible(true);
//		plot.setRangeCrosshairVisible(true);

		// Theme - WHITE
//		plot.setBackgroundPaint(Color.WHITE);
//		plot.setDomainGridlinePaint(Color.GRAY);
//		plot.setRangeGridlinePaint(Color.GRAY);

		
		// Set thicker lines
		boolean thickLines = false;
		if (thickLines)
		{
			int seriesCount = plot.getSeriesCount();
			for (int i = 0; i < seriesCount; i++)
				plot.getRenderer().setSeriesStroke(i, new BasicStroke(2));
		}

		// Special Renderer that creates a LegendItem that is square and FILLED
		XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false) 
		{
			private static final long serialVersionUID = 1L;

			@Override
			public LegendItem getLegendItem(int datasetIndex, int series) 
			{
				if (getPlot() == null) 
					return null;

				XYDataset dataset = getPlot().getDataset(datasetIndex);
				if (dataset == null) 
					return null;

				String label = dataset.getSeriesKey(series).toString();
				LegendItem legendItem = new LegendItem(label, lookupSeriesPaint(series));
//				legendItem.setLine(new Rectangle2D.Double( 0.0 , 0.0 , 5.0, 5.0) );  //setLine takes a Shape, not just Lines so you can pass any Shape to it...
				legendItem.setLine(Plot.DEFAULT_LEGEND_ITEM_BOX);
				
				return legendItem;
			}
		};
		plot.setRenderer(renderer);

		// Set format of the time labels
		DateAxis axis = (DateAxis) plot.getDomainAxis();
//		axis.setDateFormatOverride(new SimpleDateFormat("dd-MMM-yyyy HH:mm"));
		axis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));
		
		// If all values are NEAR Zero, then set Range to be 1 so we don't get exponential scale like '4E-9' and '-4E-9'
		if (isAllDataValuesNearZero())
		{
			NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
			rangeAxis.setRange(-1.0, 1.0);
		}

		// for Percent Graph: set MAX to 100%
		if (maxValue > 0)
		{
			NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
			rangeAxis.setRange(0.0, maxValue);
		}

		return chart;
	}

	private Dataset createDataset(DbxConnection conn, String cmName, String graphName, String skipNames)
	{
		if (StringUtil.isNullOrBlank(cmName))     throw new IllegalArgumentException("cmName can't be null or blank");
		if (StringUtil.isNullOrBlank(graphName))  throw new IllegalArgumentException("graphName can't be null or blank");

		String tabName   = cmName + "_" + graphName;
		
		// Early exit if the table do not exist
		boolean tabExists = DbUtils.checkIfTableExistsNoThrow(conn, null, getSchemaName(), tabName);
		if ( ! tabExists )
		{
			setProblem("<br><font color='red'>ERROR: Creating ReportChartObject, the underlying table '" + getSchemaNameSqlPrefix() + tabName + "' did not exist, skipping this Chart.</font><br>\n");

			_logger.info("createDataset(): The table '" + getSchemaNameSqlPrefix() + tabName + "' did not exist.");
			return null;
		}

		String dummySql = "select * from " + getSchemaNameSqlPrefix() + "[" + tabName + "] where 1 = 2";
		ResultSetTableModel dummyRstm = ResultSetTableModel.executeQuery(conn, dummySql, true, "metadata");

		if (dummyRstm.hasColumn("label_0") && dummyRstm.hasColumn("data_0"))
		{
//System.out.println("---------------- ReportChartTimeSeriesLine[cmName='"+cmName+"', graphName='"+graphName+"'] >>>> do 1: label[cal-VALUE]-data");
			return createDataset_labelNameAndDataInSeparateColumn(conn, cmName, graphName, skipNames);
		}
		else
		{
//System.out.println("---------------- ReportChartTimeSeriesLine[cmName='"+cmName+"', graphName='"+graphName+"'] >>>> do 2: label[col-NAME]-data");
			return createDataset_colNameIsLabelName(conn, cmName, graphName, skipNames);
		}
		
	}

	/** helper method to parse 'skipNames' into two Maps: skipName{Above|below}Value*/
	private void initSkipColumnsWithValue(String skipNames, Map<String, Double> skipNameAboveValue, Map<String, Double> skipNameBelowValue)
	{
		List<String> skipNameList = StringUtil.parseCommaStrToList(skipNames);

		for (String skipNameVal : skipNameList)
		{
			if (skipNameVal.startsWith(SKIP_COLNAME_WITH_VALUE_ABOVE))
			{
				String tmpStr = skipNameVal.substring(SKIP_COLNAME_WITH_VALUE_ABOVE.length()) + " \n";
				Map<String, String> tmpMap = StringUtil.parseCommaStrToMap(tmpStr);
				for (Entry<String, String> e : tmpMap.entrySet())
				{
					try {
						skipNameAboveValue.put(e.getKey(), Double.valueOf(e.getValue()));
					} catch (NumberFormatException nfe) {
						_logger.warn("Skipping 'SKIP_COLNAME_WITH_VALUE_ABOVE' key='"+e.getKey()+"', value='"+e.getValue()+"', caught: " + nfe);
					}
				}
			}
			if (skipNameVal.startsWith(SKIP_COLNAME_WITH_VALUE_BELOW))
			{
				String tmpStr = skipNameVal.substring(SKIP_COLNAME_WITH_VALUE_BELOW.length()) + " \n";
				Map<String, String> tmpMap = StringUtil.parseCommaStrToMap(tmpStr);
				for (Entry<String, String> e : tmpMap.entrySet())
				{
					try {
						skipNameBelowValue.put(e.getKey(), Double.valueOf(e.getValue()));
					} catch (NumberFormatException nfe) {
						_logger.warn("Skipping 'SKIP_COLNAME_WITH_VALUE_BELOW' key='"+e.getKey()+"', value='"+e.getValue()+"', caught: " + nfe);
					}
				}
			}
		}
	}

	private Dataset createDataset_labelNameAndDataInSeparateColumn(DbxConnection conn, String cmName, String graphName, String skipNames)
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
		
		// if any of the skip entries starts with "SKIP_COLNAME_WITH_VALUE_ABOVE", then parse the entry and add it to the map
		Map<String, Double> skipNameAboveValue = new HashMap<>();

		// if any of the skip entries starts with "SKIP_COLNAME_WITH_VALUE_BELOW", then parse the entry and add it to the map
		Map<String, Double> skipNameBelowValue = new HashMap<>();
		
		initSkipColumnsWithValue(skipNames, skipNameAboveValue, skipNameBelowValue);
		
		String tabName = cmName + "_" + graphName;
		String sql = 
				  "select * \n"
				+ "from " + getSchemaNameSqlPrefix() + "[" + tabName + "] \n"
				+ "where 1 = 1 \n"
				+ getReportEntry().getReportPeriodSqlWhere()
//				+ extraWhereCluase
				+ "order by [SessionSampleTime] \n"
				;
		
		sql = conn.quotifySqlString(sql);

//		// Early exit if the table do not exist
//		boolean tabExists = DbUtils.checkIfTableExistsNoThrow(conn, null, getSchemaName(), tabName);
//		if ( ! tabExists )
//		{
//			setProblem("<br><font color='red'>ERROR: Creating ReportChartObject, the underlying table '" + getSchemaNameSqlPrefix() + tabName + "' did not exist, skipping this Chart.</font><br>\n");
//
//			_logger.info("createDataset(): The table '" + getSchemaNameSqlPrefix() + tabName + "' did not exist.");
//			return null;
//		}

//		int readCount = 0;
		
		boolean skipLabelsWithNull = true;

		// autoclose: stmnt, rs
		try (Statement stmnt = conn.createStatement())
		{
			Map<String, TimeSeries> graphSeriesMap = new LinkedHashMap<>(); 

			// set TIMEOUT
			stmnt.setQueryTimeout(getDefaultQueryTimeout());

			// Execute and read result
			try (ResultSet rs = stmnt.executeQuery(sql))
			{
				ResultSetMetaData md = rs.getMetaData();
				int colCount = md.getColumnCount();
				
//				int colDataStart = 4; // "SessionStartTime", "SessionSampleTime", "CmSampleTime", "System+User CPU (@@cpu_busy + @@cpu_io)"

				String[] labels = new String[(colCount-3)/2];
				Double[] datapt = new Double[(colCount-3)/2];
				
//				List<String> labelNames = new LinkedList<>();
//				for (int c=colDataStart; c<colCount+1; c++)
//					labelNames.add( md.getColumnLabel(c) );

				while (rs.next())
				{
//					readCount++;

				//	Timestamp sessionStartTime  = rs.getTimestamp(1);
					Timestamp sessionSampleTime = rs.getTimestamp(2);
				//	Timestamp cmSampleTime      = rs.getTimestamp(3);

					Timestamp ts = sessionSampleTime;
					
					// Start to read column 4
					// move c (colIntex) 2 cols at a time, move ca (ArrayIndex) by one
					for (int c=4, ca=0; c<=colCount; c+=2, ca++)
					{
						labels[ca] = rs.getString(c);
						datapt[ca] = Double.valueOf(rs.getDouble(c+1));					
					}

					for (int ca=0; ca<datapt.length; ca++)
					{
						String label     = labels[ca];
						Double dataValue = datapt[ca];

						if (label == null && skipLabelsWithNull)
							continue;

						if (label == null)
							label = "colnum-"+ca;
						
						boolean addEntry = true;

						// SKIP any labels that are in the SkipList
						for (String skipName : skipNameList)
						{
							if (label.startsWith(skipName))
								addEntry = false;
						}
						
						for (Entry<String, Double> entry : skipNameAboveValue.entrySet())
						{
							if (label.equals(entry.getKey()))
							{
								// is GREATER than
								if (dataValue > entry.getValue())
								{
									_logger.info("Skipping value in graphName='"+tabName+"', label='"+label+"' with dataValue="+dataValue+", is ABOVE threshold=" + entry.getValue());
									addEntry = false;
								}
							}
						}

						for (Entry<String, Double> entry : skipNameBelowValue.entrySet())
						{
							if (label.equals(entry.getKey()))
							{
								// is LESS than
								if (dataValue < entry.getValue())
								{
									_logger.info("Skipping value in graphName='"+tabName+"', label='"+label+"' with dataValue="+dataValue+", is BELOW threshold=" + entry.getValue());
									addEntry = false;
								}
							}
						}

						// Should this Chart Line (label) be skipped
						if (skipNameList.contains(label))
						{
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
							
							// Remember min/max value
							setDatasetMinMaxValue(dataValue);
						}
					}
				}
			}

			TimeSeriesCollection dataset = new TimeSeriesCollection();
			for (TimeSeries dbxGraphData : graphSeriesMap.values())
			{
				dataset.addSeries(dbxGraphData);
			}

//System.out.println("################# _isSorted=" + _isSorted + ", _graphName='" + _graphName + "'.");
			if (_isSorted)
			{
				dataset = sortTimeSeriesCollection(dataset);
			}
			
			return dataset;
		}
		catch (SQLException ex)
		{
			_logger.error("Problems in createDataset() when fetching DB data for schema='" + getSchemaName() + "', cmName='" + cmName + "', graphName='" + graphName + "'.", ex);

			setProblem("Problems in createDataset() when fetching DB data for schema='" + getSchemaName() + "', cmName='" + cmName + "', graphName='" + graphName + "'.");
			setException(ex);
			
			//_fullRstm = ResultSetTableModel.createEmpty(name);
//			_logger.warn("Problems getting '" + name + "': " + ex);
			
			return null;
		}
	}

	public XYDataset createDataset_colNameIsLabelName(DbxConnection conn, String cmName, String graphName, String skipNames)
	{
		if (StringUtil.isNullOrBlank(cmName))     throw new IllegalArgumentException("cmName can't be null or blank");
		if (StringUtil.isNullOrBlank(graphName))  throw new IllegalArgumentException("graphName can't be null or blank");


		List<String> skipNameList = StringUtil.parseCommaStrToList(skipNames);

		// if any of the skip entries starts with "SKIP_COLNAME_WITH_VALUE_ABOVE", then parse the entry and add it to the map
		Map<String, Double> skipNameAboveValue = new HashMap<>();

		// if any of the skip entries starts with "SKIP_COLNAME_WITH_VALUE_BELOW", then parse the entry and add it to the map
		Map<String, Double> skipNameBelowValue = new HashMap<>();
		
		initSkipColumnsWithValue(skipNames, skipNameAboveValue, skipNameBelowValue);

		
		String tabName = cmName + "_" + graphName;
		String sql = 
				 "select * "
				+"from " + getSchemaNameSqlPrefix() + "[" + tabName + "] \n"
				+ "where 1 = 1 \n"
				+ getReportEntry().getReportPeriodSqlWhere()
				+" order by [SessionSampleTime] \n"
				;
		
		sql = conn.quotifySqlString(sql);
		
//		// Early exit if the table do not exist
//		boolean tabExists = DbUtils.checkIfTableExistsNoThrow(conn, null, getSchemaName(), tabName);
//		if ( ! tabExists )
//		{
//			setProblem("<br><font color='red'>ERROR: Creating ReportChartObject, the underlying table '" + getSchemaNameSqlPrefix() + tabName + "' did not exist, skipping this Chart.</font><br>\n");
//
//			_logger.info("createDataset(): The table '" + getSchemaNameSqlPrefix() + tabName + "' did not exist.");
//			return null;
//		}

//		int readCount = 0;

		Map<String, TimeSeries> graphSeriesMap = new LinkedHashMap<>(); 

		// autoclose: stmnt, rs
		try (Statement stmnt = conn.createStatement())
		{
			// set TIMEOUT
			stmnt.setQueryTimeout(getDefaultQueryTimeout());

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
//					readCount++;

				//	Timestamp sessionStartTime  = rs.getTimestamp(1);
					Timestamp sessionSampleTime = rs.getTimestamp(2);
				//	Timestamp cmSampleTime      = rs.getTimestamp(3);

					Timestamp ts = sessionSampleTime;
					
					for (int c=colDataStart, l=0; c<colCount+1; c++, l++)
					{
						String label = labelNames.get(l);
//						double dataValue = rs.getDouble(c);
						Double dataValue = rs.getDouble(c);

						boolean addEntry = true;

						// SKIP any labels that are in the SkipList
						for (String skipName : skipNameList)
						{
							if (label.startsWith(skipName))
								addEntry = false;
						}
						
						for (Entry<String, Double> entry : skipNameAboveValue.entrySet())
						{
							if (label.equals(entry.getKey()))
							{
								// is GREATER than
								if (dataValue > entry.getValue())
								{
									_logger.info("Skipping value in graphName='"+tabName+"', label='"+label+"' with dataValue="+dataValue+", is ABOVE threshold=" + entry.getValue());
									addEntry = false;
								}
							}
						}

						for (Entry<String, Double> entry : skipNameBelowValue.entrySet())
						{
							if (label.equals(entry.getKey()))
							{
								// is LESS than
								if (dataValue < entry.getValue())
								{
									_logger.info("Skipping value in graphName='"+tabName+"', label='"+label+"' with dataValue="+dataValue+", is BELOW threshold=" + entry.getValue());
									addEntry = false;
								}
							}
						}

						// Should this Chart Line (label) be skipped
						if (skipNameList.contains(label))
						{
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
    						graphTimeSerie.add(new Millisecond(ts), dataValue);

    						// Remember min/max value
							setDatasetMinMaxValue(dataValue);
						}
						
					}
				}
			}

			TimeSeriesCollection dataset = new TimeSeriesCollection();
			for (TimeSeries dbxGraphData : graphSeriesMap.values())
			{
				dataset.addSeries(dbxGraphData);
			}

			if (_isSorted)
			{
				dataset = sortTimeSeriesCollection(dataset);
			}

			return dataset;
		}
		catch (SQLException ex)
		{
			_logger.error("Problems in createDataset() when fetching DB data for schema='" + getSchemaName() + "', cmName='" + cmName + "', graphName='" + graphName + "'.", ex);

			setProblem("Problems in createDataset() when fetching DB data for schema='" + getSchemaName() + "', cmName='" + cmName + "', graphName='" + graphName + "'.");
			setException(ex);
			
			//_fullRstm = ResultSetTableModel.createEmpty(name);
//			_logger.warn("Problems getting '" + name + "': " + ex);
			
			return null;
		}
	}


	/** Sort the data set */
//	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected TimeSeriesCollection sortTimeSeriesCollection(TimeSeriesCollection originDataset)
	{
		// algorithm
		// - iterate all records in originDataset: calculate SUM values for each "group" in a MAP
		// - sort the temporary MAP by counter value descending
		// - copy all records from originDataset to newDataset (add order will be by the MAP)
		try 
		{
			// A MAP That will hold SUM for each of the "groups"
			Map<String, Double> keySumValues = new LinkedHashMap<>();

			// Map that will hold <SerieName, TimeSeries>
			Map<String, TimeSeries> originSeriesMap = new LinkedHashMap<>(); 

			// Get Series into Map
			for (int s=0; s<originDataset.getSeriesCount(); s++)
			{
				String     sKey = (String) originDataset.getSeriesKey(s);
				TimeSeries sVal =          originDataset.getSeries(s);
				
				originSeriesMap.put(sKey, sVal);
			}

			// Sum all values for each Key/Series. Put it in MAP: keySumValues
			for (Entry<String, TimeSeries> entry : originSeriesMap.entrySet())
			{
				String     sKey = (String) entry.getKey();
				TimeSeries sVal =          entry.getValue();

				double sumValues = 0.0;
				for (int i=0; i<sVal.getItemCount(); i++)
				{
					TimeSeriesDataItem tsdi = sVal.getDataItem(i);
					Number dataVal = tsdi.getValue();
					if (dataVal != null)
						sumValues += dataVal.doubleValue();
				}
				
				keySumValues.put(sKey, sumValues);
			}

			// Sort the MAP:keySumValues by counter value descending into MAP:sorted
			Map<String, Double> sorted =
					keySumValues.entrySet().stream()
					.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
					.collect(Collectors.toMap(
							Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));


//System.out.println("################# _graphName='"+_graphName+"', keySumValues=" + keySumValues);
//System.out.println("################# _graphName='"+_graphName+"', sorted      =" + sorted);
			
			// Copy all records from originDataset to newDataset (add order will be by the MAP: sorted)
			TimeSeriesCollection sortedDataset = new TimeSeriesCollection();
			for (String seriesName : sorted.keySet())
			{
				TimeSeries ts = originDataset.getSeries(seriesName);
				sortedDataset.addSeries(ts);
			}

			return sortedDataset;
		} 
		catch(RuntimeException ex) 
		{
			_logger.error("Problems sorting TimeSeriesCollection, skipping the sort...", ex);
			return originDataset;
		}
	}

}
