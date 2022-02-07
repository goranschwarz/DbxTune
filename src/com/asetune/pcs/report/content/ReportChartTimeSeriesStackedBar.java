/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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

import java.awt.Color;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.Dataset;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeTableXYDataset;

import com.asetune.graph.CategoryAxisSparselyLabeled;
import com.asetune.graph.CategoryPlotSparselyLabeled;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;

public class ReportChartTimeSeriesStackedBar
extends ReportChartAbstract
{
	private static Logger _logger = Logger.getLogger(ReportChartTimeSeriesStackedBar.class);

	private String              _dataGroupColumn;
	private int                 _dataGroupMinutes;
	private String              _dataTableName;
	private TopGroupCountReport _topGroupCountReport;
	private String              _dataValueColumn;
	private Double              _dataDivideByValue;
	private String              _keepGroups;
	private String              _skipGroups;
	private Set<String>         _keepGroupSet;
	private Set<String>         _skipGroupSet;

	public String              getGroupColumnName()       { return _dataGroupColumn; }
	public String              getValueColumnName()       { return _dataValueColumn; }
	public Double              getValueDivideBy()         { return _dataDivideByValue; }
	public String              getTableName()             { return _dataTableName; }
	public int                 getSamplePeriodInMinutes() { return _dataGroupMinutes; }
	public TopGroupCountReport getTopGroupCountReport()   { return _topGroupCountReport; }
	public String              getKeepGroups()            { return _keepGroups; }
	public String              getSkipGroups()            { return _skipGroups; }
	
	public ReportChartTimeSeriesStackedBar(ReportEntryAbstract reportEntry, DbxConnection conn, String cmName, String dataGroupColumn, int dataGroupMinutes, TopGroupCountReport topGroupCountReport, String dataValueColumn, Double dataDivideByValue, String keepGroups, String skipGroups, String graphTitle)
	{
		super(reportEntry, conn, ChartType.STACKED_BAR, cmName, dataGroupColumn, graphTitle);
		
		_dataGroupColumn     = dataGroupColumn;
		_dataGroupMinutes    = dataGroupMinutes;
		_topGroupCountReport = topGroupCountReport;
		_dataValueColumn     = dataValueColumn;
		_dataDivideByValue   = dataDivideByValue;
		_keepGroups          = keepGroups;
		_skipGroups          = skipGroups;

		_keepGroupSet        = StringUtil.parseCommaStrToSet(keepGroups);
		_skipGroupSet        = StringUtil.parseCommaStrToSet(skipGroups);
		
		_dataTableName       = cmName +  "_diff";
		
		if (_topGroupCountReport != null)
			_topGroupCountReport.setOwner(this);
		
		create();
	}

	@Override
	protected Dataset createDataset()
	{
		return createCategoryDatasetFromCm(getConnection());
	}

	@Override
	protected JFreeChart createChart()
	{
		return createChart(getDataset(), getGraphTitle(), null, null, -1);
	}


	//----------------------------------------------------------
	// Below are code for creating Graph/Chart images that can be included in the HTML Report
	//----------------------------------------------------------

	private JFreeChart createChart(Dataset inDataset, String graphTitle, String timeAxisLabel, String valueAxisLabel, int maxValue) 
	{
		if ( ! (inDataset instanceof CategoryDataset) )
			throw new RuntimeException("The passed dataset is of type '" + inDataset.getClass() + "' it must implement 'CategoryDataset'.");

		CategoryDataset dataset = (CategoryDataset) inDataset;

//		CategoryAxis                categoryAxis = new CategoryAxis(domainAxisLabel);
		CategoryAxisSparselyLabeled categoryAxis = new CategoryAxisSparselyLabeled(timeAxisLabel);
		ValueAxis                   valueAxis    = new NumberAxis(valueAxisLabel);

		categoryAxis.setDateFormat(new SimpleDateFormat("HH:mm"));
//		categoryAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
		
		StackedBarRenderer render = new StackedBarRenderer();
		
		render.setShadowVisible(false);
//		renderer.setDrawBarOutline(true);
		render.setBarPainter(new StandardBarPainter());
		
//		CategoryPlot plot = new CategoryPlot(dataset, categoryAxis, valueAxis, render);
		CategoryPlot plot = new CategoryPlotSparselyLabeled(dataset, categoryAxis, valueAxis, render);
		plot.setOrientation(PlotOrientation.VERTICAL);
		plot.setDomainGridlinesVisible(true);

		// Theme - GRAY
		plot.setDomainGridlinePaint(Color.WHITE);
		plot.setRangeGridlinePaint(Color.WHITE);
		plot.setBackgroundPaint(Color.LIGHT_GRAY);
		
		JFreeChart chart = new JFreeChart(graphTitle, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
		chart.setBackgroundPaint(Color.WHITE);
		
		return chart;
	}


	protected String getSql()
	{
		// NOTE: Below DATEADD/DATEDIFF only works for T-SQL, for Postgres we might do: https://gis.stackexchange.com/questions/57582/group-by-timestamp-interval-10-minutes-postgresql
		int samplePeriod = getSamplePeriodInMinutes();
		
		String divideByValue = "";
		if (getValueDivideBy() != null && getValueDivideBy() > 0.0)
			divideByValue = " / " + getValueDivideBy();
			
		
		String sql = 
				"select DATEADD(MINUTE, DATEDIFF(MINUTE, '2000-01-01', [SessionSampleTime]) / " + samplePeriod + " * " + samplePeriod + ", '2000-01-01') as [SamplePeriod] \n" 
				+"      ,[" + getGroupColumnName() + "] \n"
				+"      ,sum([" + getValueColumnName() + "])" + divideByValue + " as [" + getValueColumnName() + "] \n"
				+" from [" + getTableName() + "] \n"
				+" where 1 = 1 \n"
				+ getReportEntry().getReportPeriodSqlWhere()
//				+ keepGroupWhere
//				+ skipGroupWhere
////			+ "	--  and [SessionSampleTime] between '2020-09-10 13:00' and '2020-09-10 14:00' \n"
				+ "group by DATEADD(MINUTE, DATEDIFF(MINUTE, '2000-01-01', [SessionSampleTime]) / " + samplePeriod + " * " + samplePeriod + ", '2000-01-01'), [" + getGroupColumnName() + "] \n"
				+" order by 1, 2 \n"
				;

		return sql;
	}

	private Dataset createCategoryDatasetFromCm(DbxConnection conn)
	{
		if (StringUtil.isNullOrBlank(getCmName())         ) throw new IllegalArgumentException("cmName can't be null or blank");
		if (StringUtil.isNullOrBlank(getGroupColumnName())) throw new IllegalArgumentException("groupColName can't be null or blank");
		if (StringUtil.isNullOrBlank(getValueColumnName())) throw new IllegalArgumentException("valueColName can't be null or blank");

        //	select 
        //		 DATEADD(MINUTE, DATEDIFF(MINUTE, '2000-01-01', x."SessionSampleTime") / 10 * 10, '2000-01-01') as "Period"
        //		,x."wait_type"
        //		,sum(x."wait_time_ms") as wait_time_ms
        //	from "PUBLIC"."CmWaitStats_diff" x 
        //	where x."SessionSampleTime" between '2020-09-10 13:00' and '2020-09-10 14:00' and x."wait_time_ms"
        //	  and x."wait_type" in ('OLEDB','ASYNC_NETWORK_IO','LATCH_EX','PAGEIOLATCH_SH','SOS_SCHEDULER_YIELD','ASYNC_IO_COMPLETION')
        //	group by DATEADD(MINUTE, DATEDIFF(MINUTE, '2000-01-01', x."SessionSampleTime") / 10 * 10, '2000-01-01'), x."wait_type"
        //	order by 1, 2
		
//		+---------------------+-------------------+------------+
//		|Period               |wait_type          |WAIT_TIME_MS|
//		+---------------------+-------------------+------------+
//		|2020-09-10 13:00:00.0|ASYNC_NETWORK_IO   |281         |
//		|2020-09-10 13:00:00.0|LATCH_EX           |11237       |
//		|2020-09-10 13:00:00.0|OLEDB              |1           |
//		|2020-09-10 13:00:00.0|PAGEIOLATCH_SH     |6049        |
//		|2020-09-10 13:00:00.0|SOS_SCHEDULER_YIELD|1478        |
//		|2020-09-10 13:10:00.0|ASYNC_NETWORK_IO   |1252        |
//		|2020-09-10 13:10:00.0|LATCH_EX           |2824        |
//		|2020-09-10 13:10:00.0|PAGEIOLATCH_SH     |4432        |
//		|2020-09-10 13:10:00.0|SOS_SCHEDULER_YIELD|1557        |
//		|2020-09-10 13:20:00.0|ASYNC_NETWORK_IO   |1543        |
//		|2020-09-10 13:20:00.0|LATCH_EX           |794         |
//		|2020-09-10 13:20:00.0|OLEDB              |1           |
//		|2020-09-10 13:20:00.0|PAGEIOLATCH_SH     |4706        |
//		|2020-09-10 13:20:00.0|SOS_SCHEDULER_YIELD|619         |
//		|2020-09-10 13:30:00.0|ASYNC_NETWORK_IO   |796         |
//		|2020-09-10 13:30:00.0|LATCH_EX           |1883        |
//		|2020-09-10 13:30:00.0|PAGEIOLATCH_SH     |6013        |
//		|2020-09-10 13:30:00.0|SOS_SCHEDULER_YIELD|829         |
//		|2020-09-10 13:40:00.0|ASYNC_NETWORK_IO   |1074        |
//		|2020-09-10 13:40:00.0|LATCH_EX           |1001        |
//		|2020-09-10 13:40:00.0|PAGEIOLATCH_SH     |3002        |
//		|2020-09-10 13:40:00.0|SOS_SCHEDULER_YIELD|1135        |
//		|2020-09-10 13:50:00.0|ASYNC_NETWORK_IO   |1134        |
//		|2020-09-10 13:50:00.0|LATCH_EX           |8707        |
//		|2020-09-10 13:50:00.0|PAGEIOLATCH_SH     |5578        |
//		|2020-09-10 13:50:00.0|SOS_SCHEDULER_YIELD|2292        |
//		+---------------------+-------------------+------------+
//		Rows 26
		
//		String tabName   = cmName + "_diff";

//		String keepGroupWhere = "";
//		String skipGroupWhere = "";
//		String sampleStartEndWhere = "";
//		int    samplePeriod = dataGroupMinutes; // in minutes
//samplePeriod = 1; // in minutes if you want to debug things
		
        // SQL Where clauses
//		if (!keepGroupList.isEmpty()) keepGroupWhere  = "   and [" + groupColName + "] in ("     + StringUtil.toCommaStrQuoted("'", keepGroupList) + ") \n";
//		if (!skipGroupList.isEmpty()) skipGroupWhere  = "   and [" + groupColName + "] not in (" + StringUtil.toCommaStrQuoted("'", skipGroupList) + ") \n";
//		if (false)                    sampleStartEndWhere = "   and [SessionSampleTime] between '" + _startDate + "' and '" + _endDate + "' \n";
		
//		LinkedHashMap<String, Integer> topGroupCountMap = null;
//		if (StringUtil.isNullOrBlank(keepGroupWhere) && topGroupCountReport!= null && topGroupCountReport.getTopCount() > 1)
//		{
//			topGroupCountMap = getTopGroupsByValue(conn);
//			if (topGroupCountMap.size() > 0)
//			{
//				keepGroupWhere = "   and [" + groupColName + "] in (" + StringUtil.toCommaStrQuoted("'", topGroupCountMap.keySet()) + ") \n";
//
//				if (topGroupCountReport != null)
//				{
//					// Set some properties
//					topGroupCountReport.setOwner(this);
//					topGroupCountReport.setTop(topGroupCountMap);
//
//					// Create the User defined Report, and set it as the Post Comment
//					String topOutput = topGroupCountReport.process();
//					if (StringUtil.hasValue(topOutput))
//						setPostComment(topOutput);
//				}
//			}
//		}
		
		// If we have a TOP GROUP REPORT Object
		TopGroupCountReport topGroupCountReport = getTopGroupCountReport();
		if (topGroupCountReport != null && topGroupCountReport.getTopCount() > 0)
		{
			LinkedHashMap<String, Double> topGroupCountMap = topGroupCountReport.getTopGroupsByValue(conn);
			if (topGroupCountMap.size() > 0)
			{
				_keepGroupSet = topGroupCountMap.keySet();

				// Set some properties
				topGroupCountReport.setTop(topGroupCountMap);

				// Create the User defined Report, and set it as the Post Comment
				String topOutput = topGroupCountReport.process();
				if (StringUtil.hasValue(topOutput))
					setPostComment(topOutput);
			}
		}
		

		// get SQL
		String sql = conn.quotifySqlString(getSql());

		// Early exit if the table do not exist
		boolean tabExists = DbUtils.checkIfTableExistsNoThrow(conn, null, null, getTableName());
		if ( ! tabExists )
		{
			setProblem("<br><font color='red'>ERROR: Creating ReportChartObject, the underlying table '" + getTableName() + "' did not exist, skipping this Chart.</font><br>\n");

			_logger.info("createDataset(): The table '" + getTableName() + "' did not exist.");
			return null;
		}

		boolean useCategoryDataset = true;
		DefaultCategoryDataset categoryDataset  = null;
		TimeTableXYDataset     timeTableDataset = null;
		if (useCategoryDataset)
			categoryDataset  = new DefaultCategoryDataset();
		else
			timeTableDataset = new TimeTableXYDataset();
		
		// autoclose: stmnt, rs
		try (Statement stmnt = conn.createStatement())
		{
			// set TIMEOUT
			stmnt.setQueryTimeout(getDefaultQueryTimeout());

			// Execute and read result
			try (ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
				{
					// Read the data (delegated to a method that can be overridden)
					DataRecord dr = createRecord(rs);
					if (dr != null)
					{
						if (!_skipGroupSet.isEmpty() && _skipGroupSet.contains(dr.group))
						{
							if (_logger.isDebugEnabled())
								_logger.debug("createCategoryDatasetFromCm(): SKIP-GROUP,     in SKIP-SET. group='" + dr.group + "', ts='" + dr.ts + "', value=" + dr.value + ", _skipGroupSet: " + _skipGroupSet);
							continue;
						}

						if (!_keepGroupSet.isEmpty() && !_keepGroupSet.contains(dr.group))
						{
							if (_logger.isDebugEnabled())
								_logger.debug("createCategoryDatasetFromCm(): SKIP-GROUP, NOT in KEEP-SET. group='" + dr.group + "', ts='" + dr.ts + "', value=" + dr.value + ", _keepGroupSet: " + _keepGroupSet);
							continue;
						}

						if (useCategoryDataset)
							categoryDataset.addValue(dr.value, dr.group, dr.ts);
						else
							timeTableDataset.add(new Minute(dr.ts), dr.value, dr.group);
					}
				}
			}

			// Sort so that the "group" with the highest values (for each group) is first in the Category Labels...
			if (useCategoryDataset)
			{
				categoryDataset = sortCategoryDataset(categoryDataset);
			}
			
			return useCategoryDataset ? categoryDataset : timeTableDataset;
		}
		catch (SQLException ex)
		{
			_logger.error("Problems in createDataset() when fetching DB data for cmName='" + getCmName() + "', groupColName='" + getGroupColumnName() + "', valueColName='" + getValueColumnName() + "'.", ex);

			setProblem("Problems in createDataset() when fetching DB data for cmName='" + getCmName() + "', groupColName='" + getGroupColumnName() + "', valueColName='" + getValueColumnName() + "'.");
			setException(ex);
			
			return null;
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected DefaultCategoryDataset sortCategoryDataset(DefaultCategoryDataset originDataset)
	{
		// algorithm
		// - iterate all records in originDataset: calculate SUM values for each "group" in a MAP
		// - sort the temporary MAP by counter value descending
		// - copy all records from originDataset to newDataset (add order will be by the MAP)
		try 
		{
			// A MAP That will hold SUM for each of the "groups"
			Map<Comparable, Double> groupSumValues = new LinkedHashMap<>();

			// NOTE: 
			// - COLS = "Timestamp"
			// - ROWS = "groups"
			
			List<Comparable> tsKeys    = originDataset.getColumnKeys();
			List<Comparable> groupKeys = originDataset.getRowKeys();

//System.out.println("ROWS="+groupKeys);
//System.out.println("COLS="+tsKeys);

			// Iterate all records in originDataset: calculate SUM values for each "group" in a MAP
			for (Comparable tsKey : tsKeys)
			{
				for (Comparable groupKey : groupKeys)
				{
					Number num = originDataset.getValue(groupKey, tsKey);
					Double curVal = groupSumValues.getOrDefault(groupKey, 0.0);
//System.out.println("AAAAA: tsKey="+tsKey+", num="+num+", curVal="+curVal+", groupKey="+groupKey+".");

					Double sumVal = curVal + (num == null ? 0.0 : num.doubleValue());
					groupSumValues.put(groupKey, sumVal);
				}
			}

			// Sort the MAP by counter value descending
			Map<Comparable, Double> sorted =
					groupSumValues.entrySet().stream()
					.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
					.collect(Collectors.toMap(
							Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

//System.out.println("sorted="+StringUtil.toCommaStr(sorted));
			
			// Copy all records from originDataset to newDataset (add order will be by the MAP)
			DefaultCategoryDataset newDataset = new DefaultCategoryDataset();

			for (Comparable tsKey : tsKeys)
			{
				for (Comparable groupKey : sorted.keySet())
				{
					Number value = originDataset.getValue(groupKey, tsKey);
					if (value == null)
						value = 0.0;
					
					newDataset.addValue(value, groupKey, tsKey);
				}
			}
			
			return newDataset;
		} 
		catch(RuntimeException ex) 
		{
			_logger.error("Problems sorting Category Dataset, skipping the sort...", ex);
			return originDataset;
		}
	}

	/**
	 * From a ResultSet create a DataRecord (Timestamp ts, String group, Double value)
	 * <p>
	 * Can be used/overidden if you have specified your own SQL Statement using getSql()
	 * 
	 * @param rs The ResultSet to read current row from.
	 * @return
	 * @throws SQLException
	 */
	public DataRecord createRecord(ResultSet rs)
	throws SQLException
	{
		Timestamp ts     = rs.getTimestamp(1);
		String    group  = rs.getString(2);
		double    value  = rs.getDouble(3);

		return new DataRecord(ts, group, value);
	}

	public static class DataRecord
	{
		Timestamp ts;
		String    group;
		Double    value;

		public DataRecord(Timestamp ts, String group, Double value)
		{
			this.ts    = ts;
			this.group = group;
			this.value = value;
		}
	}

	public static class TopGroupDataRecord
	{
		String    group;
		Double    value;

		public TopGroupDataRecord(String group, Double value)
		{
			this.group = group;
			this.value = value;
		}
	}

	/**
	 * This is called from xxx to handle reporting of the Top Count 
	 * <p>
	 * Implement the method <code>process</code> and any returned string will be printed in the report! 
	 */
	public abstract static class TopGroupCountReport
	{
		private ReportChartTimeSeriesStackedBar _owner;
		private int                             _topCount;
		private LinkedHashMap<String, Double>   _topGroupCountMap;
		
		public TopGroupCountReport(int topCount)
		{
			_topCount = topCount;
		}

		public void setOwner(ReportChartTimeSeriesStackedBar owner) { _owner = owner; }
		public void setTopCount(int topCount)                       { _topCount = topCount; }
		public void setTop(LinkedHashMap<String, Double> map)       { _topGroupCountMap = map; }

		public ReportChartTimeSeriesStackedBar getOwner()           { return _owner; }
		public int                             getTopCount()        { return _topCount; }
		public LinkedHashMap<String, Double>   getTop()             { return _topGroupCountMap; }


		public abstract String process();

	
		protected String getTopGroupsSql()
		{
			String sql = ""
					+ "select top " + getTopCount() + " \n"
					+ "    [" + getOwner().getGroupColumnName() + "] \n"
					+ "   ,sum([" + getOwner().getValueColumnName() + "]) as [" + getOwner().getValueColumnName() + "] \n"
					+ "from [" + getOwner().getTableName() + "] \n"
					+ "where 1 = 1 \n"
					+ getOwner().getReportEntry().getReportPeriodSqlWhere()
					+ "  and [" + getOwner().getValueColumnName() + "] > 0 \n"
					+ "group by [" + getOwner().getGroupColumnName() + "] \n"
					+ "order by 2 desc \n"
					+ "";

			return sql;
		}

		protected LinkedHashMap<String, Double> getTopGroupsByValue(DbxConnection conn)
		{
			LinkedHashMap<String, Double> map = new LinkedHashMap<>();

			String sql = conn.quotifySqlString(getTopGroupsSql());

			try (Statement stmnt = conn.createStatement())
			{
				// set TIMEOUT
				stmnt.setQueryTimeout(getOwner().getDefaultQueryTimeout());

				// Execute and read result
				try (ResultSet rs = stmnt.executeQuery(sql))
				{
					while (rs.next())
					{
						// Read the data (delegated to a method that can be overridden)
						TopGroupDataRecord dr = createRecord(rs);
						if (dr != null)
						{
							map.put(dr.group, dr.value);
						}
					}
				}
			}
			catch(SQLException ex) 
			{
				_logger.error("Problems in getTopGroupsByValue() when fetching TOP DB data for tabName='" + getOwner().getTableName() + "', groupColName='" + getOwner().getGroupColumnName() + "', valueColName='" + getOwner().getValueColumnName() + "', sql=" + sql, ex);
			}

			return map;
		}

		public TopGroupDataRecord createRecord(ResultSet rs)
		throws SQLException
		{
			String    group  = rs.getString(1);
			double    value  = rs.getDouble(2);

			return new TopGroupDataRecord(group, value);
		}

	}
}
