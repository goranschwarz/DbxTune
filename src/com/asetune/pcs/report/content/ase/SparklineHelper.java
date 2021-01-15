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
package com.asetune.pcs.report.content.ase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.content.ReportEntryAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;

public class SparklineHelper
{
	private static Logger _logger = Logger.getLogger(SparklineHelper.class);

//	/**
//	 */
//	public enum ColumnNameType
//	{
//		SINGLE_COLUMN_NAME, 
//		MULTI_COLUMN_NAME, 
//		SQL_EXPRESSION
//	};

	public enum AggType
	{
		/** SUM(colName) NOTE: This is the DEFAULT */
		SUM,

		/** AVG(colName) */
		AVG,

		/** MIN(colName) */
		MIN, 

		/** MAX(colName) */
		MAX,

		/** specify aggregate column ant type by yourself in the setDbmsDataValueColumnName("sum([colName1]) + sum([coName2])") method. NOTE: '[' and ']' will be replaced with the DBMS specific Quoted Identifier Chars */
		USER_PROVIDED
	};
	
	/**
	 * Parameters to method: createSparkline()
	 */
	public static class SparkLineParams
	{
		private String  _htmlChartColumnName;
		private String  _htmlWhereKeyColumnName;
//		private boolean _htmlWhereKeyColumnNameIsList        = false;
		private String  _dbmsTableName;
		private String  _dbmsSampleTimeColumnName;
		private String  _dbmsDataValueColumnName;
		private boolean _dbmsDataValueColumnNameIsExpression = false;
		private String  _dbmsWhereKeyColumnName;
//		private boolean _dbmsWhereKeyColumnNameIsList        = false;
		private String  _dbmsExtraWhereclause;
		private AggType _groupDataAggregationType            = AggType.SUM;
		private int     _groupDataInMinutes                  = 10;
		private int     _noDataInGroupDefaultValue           = 0;
		private int     _decimalScale                        = 0;
		private String  _sparklineClassName;
		private String  _sparklineTooltipPostfix;
		private String  _noBrowserText = "Only in Web Browser";

		public static SparkLineParams create()
		{	
			return new SparkLineParams();
		}

		/** HTML Table column name to replace with a mini-chart */
		public SparkLineParams setHtmlChartColumnName                (String  htmlChartColumnName                ) { _htmlChartColumnName                 = htmlChartColumnName;                 return this; }

		/** HTML Table column name to get data for when building a where clause against in the SQL query */
		public SparkLineParams setHtmlWhereKeyColumnName             (String  htmlWhereKeyColumnName             ) { _htmlWhereKeyColumnName              = htmlWhereKeyColumnName;              return this; }

		/** DBMS table name to use in query */
		public SparkLineParams setDbmsTableName                      (String  dbmsTableName                      ) { _dbmsTableName                       = dbmsTableName;                       return this; }

		/** DBMS Column Name to check if record is "between" start/end in period group. (used for "group" data), This <b>should</b> have an index on it. <code>where ${dbmsSampleTimeColumnName} between ${group_begin_ts} and ${group_end_ts}</code> */
		public SparkLineParams setDbmsSampleTimeColumnName           (String  dbmsSampleTimeColumnName           ) { _dbmsSampleTimeColumnName            = dbmsSampleTimeColumnName;            return this; }

		/** DBMS Column Name to get data from (or use as as a "sum" column) */
		public SparkLineParams setDbmsDataValueColumnName            (String  dbmsDataValueColumnName            ) { _dbmsDataValueColumnName             = dbmsDataValueColumnName;             return this; }

		/** if 'dbmsDataValueColumnName' is a column name or an expression. If this is false (default), then value in 'dbmsDataValueColumnName' will be [Quotified] at the DBMS */
		public SparkLineParams setDbmsDataValueColumnNameIsExpression(boolean dbmsDataValueColumnNameIsExpression) { _dbmsDataValueColumnNameIsExpression = dbmsDataValueColumnNameIsExpression; return this; }

		/** DBMS Column Name use as a where clause -- if not specified this will be the same as 'htmlWhereKeyColumnName' */
		public SparkLineParams setDbmsWhereKeyColumnName             (String  dbmsWhereKeyColumnName             ) { _dbmsWhereKeyColumnName              = dbmsWhereKeyColumnName;              return this; }

		/** DBMS Column Name use as a where clause -- if not specified this will be the same as 'htmlWhereKeyColumnName' */
//		public SparkLineParams setDbmsWhereKeyColumnNameIsList       (boolean dbmsWhereKeyColumnNameIsList       ) { _dbmsWhereKeyColumnNameIsList        = dbmsWhereKeyColumnNameIsList;        return this; }

		/** If we want to add some extra where clauses when getting data.<br>For example: if we do not want "first time" statistics. <code>setDbmsExtraWhereClause("and and [CmNewDiffRateRow] = 0")</code> */
		public SparkLineParams setDbmsExtraWhereClause               (String dbmsExtraWhereclause                ) { _dbmsExtraWhereclause                = dbmsExtraWhereclause;                return this; }
		
		/** What Aggregation Type we should use */
		public SparkLineParams setGroupDataAggregationType           (AggType groupDataAggregationType           ) { _groupDataAggregationType            = groupDataAggregationType;            return this; }

		/** How many minutes should the data be grouped by... (it's not a normal group by... but similar) */
		public SparkLineParams setGroupDataInMinutes                 (int     groupDataInMinutes                 ) { _groupDataInMinutes                  = groupDataInMinutes;                  return this; }

		/** If data wasn't found for this specific time-group, then use this value as the default data value <br> DEFAULT is: 0 */
		public SparkLineParams setNoDataInGroupDefaultValue          (int     noDataInGroupDefaultValue          ) { _noDataInGroupDefaultValue           = noDataInGroupDefaultValue;           return this; }

		/** When getting data from the DBMS and the data is a decimal value, how many decimals do we want to show <br> DEFAULT is: 1 <br> set to -1 if you do not want any scaling, and simply accepts what the DBMS produces*/
		public SparkLineParams setDecimalScale                       (int     decimalScale                       ) { _decimalScale                        = decimalScale;                        return this; }

		/** HTML class-name to use for this mini-chart */
		public SparkLineParams setSparklineClassName                 (String  sparklineClassName                 ) { _sparklineClassName                  = sparklineClassName;                  return this; }

		/** When showing tool tip for this value, this can be used to <i>describe</i> what it represents */
		public SparkLineParams setSparklineTooltipPostfix            (String  sparklineTooltipPostfix            ) { _sparklineTooltipPostfix             = sparklineTooltipPostfix;             return this; }

		/** If we are reading this from a Mail reader that do not support JavaScript, then this value will be used as a placement instead of the mini-chart */
		public SparkLineParams setNoBrowserText                      (String  noBrowserText                      ) { _noBrowserText                       = noBrowserText;                       return this; }

		public String  getHtmlChartColumnName()                 { return _htmlChartColumnName; }
		public String  getHtmlWhereKeyColumnName()              { return _htmlWhereKeyColumnName; }
		public String  getDbmsTableName()                       { return _dbmsTableName; }
		public String  getDbmsSampleTimeColumnName()            { return _dbmsSampleTimeColumnName; }
		public String  getDbmsDataValueColumnName()             { return _dbmsDataValueColumnName; }
		public boolean getDbmsDataValueColumnNameIsExpression() { return _dbmsDataValueColumnNameIsExpression; }
		public String  getDbmsWhereKeyColumnName()              { return _dbmsWhereKeyColumnName        != null ? _dbmsWhereKeyColumnName          : getHtmlWhereKeyColumnName(); }
//		public boolean getDbmsWhereKeyColumnNameIsList()        { return _dbmsWhereKeyColumnNameIsList; }
		public boolean getDbmsWhereKeyColumnNameIsList()        { return getDbmsWhereKeyColumnName().contains(","); }
		public String  getDbmsExtraWhereClause()                { return _dbmsExtraWhereclause          != null ? _dbmsExtraWhereclause            : ""; }
		public AggType getGroupDataAggregationType()            { return _groupDataAggregationType; }
		public int     getGroupDataInMinutes()                  { return _groupDataInMinutes; }
		public int     getNoDataInGroupDefaultValue()           { return _noDataInGroupDefaultValue; }
		public int     getDecimalScale()                        { return _decimalScale; }
		public String  getSparklineClassName()                  { return _sparklineClassName            != null ? _sparklineClassName              : "sparklines_" + getHtmlChartColumnName(); }
		public String  getSparklineTooltipPostfix()             { return _sparklineTooltipPostfix       != null ? " - " + _sparklineTooltipPostfix : " - " + getGroupDataAggregationType() + " '" + getDbmsDataValueColumnName() +"' in below time period" ; }
		public String  getNoBrowserText()                       { return _noBrowserText                 != null ? _noBrowserText                   : ""; }

		/**
		 * Validate that all settings are OK
		 * @return The instance
		 * @throws RuntimeException is case of issues
		 */
		public SparkLineParams validate()
		{
			// Check if everything is OK
			String problem = null;

			if (getDbmsWhereKeyColumnNameIsList())
			{
				List<String> htmlColumnList = StringUtil.parseCommaStrToList(getHtmlWhereKeyColumnName());
				List<String> dbmsColumnList = StringUtil.parseCommaStrToList(getDbmsWhereKeyColumnName());

				if (problem == null && ! htmlColumnList.equals(dbmsColumnList))
					problem = "'DbmsWhereKeyColumnNameIsList' is set to TRUE, so 'HtmlWhereKeyColumnName' and 'DbmsWhereKeyColumnName' must be equal, but they are not. htmlColumnList=" + htmlColumnList + ", dbmsColumnList=" + dbmsColumnList;

				if (problem == null && htmlColumnList.size() == 1)
					problem = "'DbmsWhereKeyColumnNameIsList' is set to TRUE, but the list only contains 1 enty, FIX: setDbmsWhereKeyColumnNameIsList(false). dbmsColumnList=" + dbmsColumnList;
			}
			
			if ( problem != null )
			{
				throw new RuntimeException("Validating of SparkLineParams failed. Problem: " + problem);
			}
			
			return this;
		}
	}

	/**
	 * 
	 * @param conn
	 * @param reportEntry
	 * @param rstm
	 * @param params
	 * @return
	 */
	public static String createSparkline(DbxConnection conn, ReportEntryAbstract reportEntry, ResultSetTableModel rstm, SparkLineParams params)
	{
		if (rstm == null)
			return null;

		int pos_chartColumnName    = rstm.findColumnMandatory(params.getHtmlChartColumnName());
//		int pos_whereKeyColumnName = rstm.findColumnMandatory(params.getHtmlWhereKeyColumnName());

		SparklineResult result = null;

		LocalDateTime reportBegin = reportEntry.getReportingInstance().getReportPeriodOrRecordingBeginTime().toLocalDateTime();
		LocalDateTime reportEnd   = reportEntry.getReportingInstance().getReportPeriodOrRecordingEndTime()  .toLocalDateTime();

		List<String> htmlWhereColNameList = StringUtil.parseCommaStrToList(params.getHtmlWhereKeyColumnName());
		List<String> dbmsWhereColNameList = StringUtil.parseCommaStrToList(params.getDbmsWhereKeyColumnName());

		if (htmlWhereColNameList.size() != dbmsWhereColNameList.size())
			throw new RuntimeException("Expecting HTML and DBMS 'where column count' to be of same size. html.size=" + htmlWhereColNameList.size() + ", dbms.size=" + dbmsWhereColNameList.size());
		
		
		for (int r=0; r<rstm.getRowCount(); r++)
		{
//			Integer val_whereKeyColumnName = rstm.getValueAsInteger(r, pos_whereKeyColumnName);
//			Object  val_whereKeyColumnNameXXX = rstm.getValueAsObject(r, pos_whereKeyColumnName);
			
			List<Object> colValList  = new ArrayList<>();

			for (int i=0; i<htmlWhereColNameList.size(); i++)
			{
				String colName = htmlWhereColNameList.get(i);
				int    colPos = rstm.findColumnMandatory(colName);
				Object colVal = rstm.getValueAsObject(r, colPos);

				if (colVal == null)
				{
					_logger.warn("Getting value from table '" + rstm.getName() + "', columnName='" + colName + "', row=" + r + ", colPos=" + colPos +", was a NULL value.");
				}

				colValList.add(colVal);
			}

//if ("AseTopSlowProcCalls".equals(reportEntry.getClass().getSimpleName()))
//{
//	System.out.println("XXXXXXXXX: params.getHtmlWhereKeyColumnName()='"+params.getHtmlWhereKeyColumnName()+"', colNameList="+colNameList+", colValList="+colValList);
//}
			// Create a helper index in the DBMS (if it doesnt already exists)
			// The index will be called "ix_DSR_SL_" + "tableName"
			// Columns will be: "all columns in the DBMS where clause" and the "SampleTime" column
			String indexDdl = "";
			try
			{
				String tableName = params.getDbmsTableName();
				String indexName = "ix_DSR_" + tableName;
				String colNames  = "";
				String ixPostFix = "";

				for (String colName : StringUtil.parseCommaStrToList(params.getDbmsWhereKeyColumnName()))
				{
					colNames += "[" + colName + "], ";
					ixPostFix += "_" + colName.replaceAll("[^a-zA-Z0-9]", ""); // remove anything suspect chars keep onle: a-z and numbers
				}
				colNames += "[" + params.getDbmsSampleTimeColumnName() + "]";

				// add "column names" as a "postfix" at the end of the index name (there might be more than one index)
				indexName += ixPostFix;

				// Check if index already exists
				boolean indexExists = false;
				try (ResultSet rs = conn.getMetaData().getIndexInfo(null, null, tableName, false, false))
				{
					while(rs.next())
					{
						String ixName = rs.getString("INDEX_NAME");
						if (indexName.equalsIgnoreCase(ixName))
						{
							indexExists = true;
							break;
						}
					}
				}
				
				// Create the index
				if ( ! indexExists )
				{
					indexDdl = conn.quotifySqlString("create index [" + indexName + "] on [" + tableName + "] (" + colNames + ")");
					
					long startTime = System.currentTimeMillis();
					try (Statement stmnt = conn.createStatement())
					{
						stmnt.executeUpdate(indexDdl);
					}
					_logger.info("ReportEntry '" + reportEntry.getClass().getSimpleName() + "'. Created helper index to support Daily Summary Report. SQL='" + indexDdl + "' ExecTime=" + TimeUtils.msDiffNowToTimeStr(startTime));
				}
			}
			catch (SQLException ex)
			{
				_logger.error("ReportEntry '" + reportEntry.getClass().getSimpleName() + "'. Problems creating a helper index, skipping the error and continuing... SQL=|" + indexDdl + "|.", ex);
			}

			// get value from DBMS
			try
			{
				result = SparklineHelper.getSparclineData(
						conn, 
						reportBegin, 
						reportEnd, 
						params.getGroupDataInMinutes(),
						params.getGroupDataAggregationType(),
						params.getDbmsTableName(), 
						params.getDbmsSampleTimeColumnName(), 
						params.getDbmsDataValueColumnName(), 
						params.getDbmsDataValueColumnNameIsExpression(), 
						dbmsWhereColNameList, 
						colValList, 
						params.getDbmsExtraWhereClause(), 
						params.getNoDataInGroupDefaultValue(),
						params.getDecimalScale()
						);
				
				String noBrowserText = params.getNoBrowserText();  
				String jfreeChartInlinePng = SparklineJfreeChart.create(result);
				
				noBrowserText = jfreeChartInlinePng;

				// If we want to replace ZERO values with NULL (not showing on the chart)
				boolean doNotShowZeroValues = Configuration.getCombinedConfiguration().getBooleanProperty("SparklineHelper.doNotShowZeroValues", false);
				if (doNotShowZeroValues)
				{
					double zeroValue = new Double(params.getNoDataInGroupDefaultValue());

    				for (int i=0; i<result.values.size(); i++)
    				{
    					Number val = result.values.get(i);
    					if (val.doubleValue() == zeroValue)
    						result.values.set(i, null);
    				}
				}

//				// Create a CSV: #,#,#,#,#,#
//				String valStr = StringUtil.toCommaStr(result.values, ",");
//
////				String sparklineDataStr = "<span class='" + sparklineClassName + "' values='" + valStr + "'>Mini Chart Here</span>";
////				String sparklineDataStr = "<span class='" + params.getSparklineClassName() + "' values='" + valStr + "'>" + noBrowserText + "</span>";
//				String sparklineDataStr = "<div class='" + params.getSparklineClassName() + "' values='" + valStr + "'>" + noBrowserText + "</div>";
////				String sparklineDataStr = "<span class='" + params.getSparklineClassName() + "'><!-- " + valStr + " -->" + noBrowserText + "</span>";
////				String sparklineDataStr = "<span class='" + params.getSparklineClassName() + "'>" + noBrowserText + "</span>";
////if ("AseTopSlowProcCalls".equals(reportEntry.getClass().getSimpleName()))
////{
////	System.out.println("XXXXXXXXX: sparklineDataStr="+sparklineDataStr);
////}
//				// Write "on top" of the "sparkline" text/image
//				String sparklineMaxValStr = "<div class='sparkline-max-val'>" + result.getMaxValueLabel() + "</div>";
//
//				String sparkline = "<div class='sparkline-wrapper'>" + sparklineDataStr + sparklineMaxValStr + "</div>";
////				String sparkline = sparklineDataStr;

				String sparklineDiv = getSparklineDiv(result, params.getSparklineClassName(), noBrowserText);

				rstm.setValueAtWithOverride(sparklineDiv, r, pos_chartColumnName);
			}
			catch (SQLException ex)
			{
				_logger.error("Problems getting setting Sparkline value for: colNameList=" + dbmsWhereColNameList + ", colValList=" + colValList, ex);
			}
		}

		// Return init code for JavaScript
		return getJavaScriptInitCode(result, params.getSparklineClassName(), params.getSparklineTooltipPostfix());
	}

	public static String getSparklineDiv(SparklineResult result, String sparklineClassName, String noBrowserText)
	{
//		// Create a CSV: #,#,#,#,#,#
		String valStr = StringUtil.toCommaStr(result.values, ",");
		
		String sparklineDataStr   = "<div class='" + sparklineClassName + "' values='" + valStr + "'>" + noBrowserText + "</div>";
		String sparklineMaxValStr = "<div class='sparkline-max-val'>" + result.getMaxValueLabel() + "</div>";
		String sparkline          = "<div class='sparkline-wrapper'>" + sparklineDataStr + sparklineMaxValStr + "</div>";
		
		return sparkline;
	}

	public static String getJavaScriptInitCode(SparklineResult result, String sparklineClassName, String tooltipPostfix)
	{
		// write a "timeArray", which will be used by 'JQuery sparkline' tooltip functionality. 
		if (result != null && result.tooltips != null && ! result.tooltips.isEmpty())
		{
			StringBuilder sb = new StringBuilder();

			// Get tool tip and escape any single quotes in the tool tip text 
			if (tooltipPostfix != null)
				tooltipPostfix = tooltipPostfix.replace("'", "\\'");
			
			sb.append("<script type='text/javascript'> \n");
			sb.append("\n");
			sb.append("    // Execute when page has LOADED \n");
			sb.append("    document.addEventListener('DOMContentLoaded', function() \n");
			sb.append("    { \n");
			sb.append("        // Initialize all mini charts -- sparklines \n");
			sb.append("        $('." + sparklineClassName + "').sparkline('html', {										\n");
			sb.append("            type: 'line',						  												\n");
			sb.append("            chartRangeMin: 0,					  												\n");
			sb.append("            tooltipFormat: '<span style=\"font-size:12px;\"><span style=\"color: {{color}}\">&#9679;</span> {{y}}" + tooltipPostfix + "<br>&nbsp;&nbsp; {{offset:toolTipArray}}</span>',	\n");
			sb.append("            tooltipValueLookups: { 				  												\n");
			sb.append("                toolTipArray: { 																	\n");
			String comma = " ";
			int    pos   = 0;
			for (String str : result.tooltips)
			{
				sb.append("                    " + comma + pos + ": '" + str + "' \n");
				comma = ",";
				pos++;
			}
			sb.append("                }																				\n");
			sb.append("            }																					\n");
			sb.append("        });																						\n");
			sb.append("        // now show the Max: ### overlay															\n");
			sb.append("        $('.sparkline-max-val').css('display', 'block');											\n");
			sb.append("    }); \n");
			sb.append("\n");
			sb.append("\n");
			sb.append("</script> \n");
			
			return sb.toString();
		}

		return "";
	}

	/**
	 * Results when calling method getSparclineData
	 */
	public static class SparklineResult
	{
		public List<String>    tooltips = new ArrayList<>();
		public List<Timestamp> beginTs  = new ArrayList<>();
//		public List<Integer>   values   = new ArrayList<>();
		public List<Number>    values   = new ArrayList<>();
		
		public Number getMaxValue()
		{
			Number maxValue = Integer.MIN_VALUE;
			for (Number val : values)
			{
				maxValue = Math.max(maxValue.doubleValue(), val.doubleValue());
			}
			return maxValue;
		}

		public String getMaxValueLabel()
		{
			return "Max: " + NumberFormat.getInstance().format( getMaxValue() );
		}
	}

	/**
	 * 
	 * @param conn
	 * @param beginLdt
	 * @param endLdt
	 * @param durationInMinutes
	 * @param aggType 
	 * @param tabName
	 * @param tabPeriodColName
	 * @param sumColName
	 * @param sumColNameIsExpr
	 * @param whereColName
	 * @param whereColVal
	 * @param noDataInPeriod
	 * @return
	 * @throws SQLException
	 */
	public static SparklineResult getSparclineData(DbxConnection conn, LocalDateTime beginLdt, LocalDateTime endLdt, int durationInMinutes, AggType aggType, String tabName, String tabPeriodColName, String sumColName, boolean sumColNameIsExpr, List<String> whereColNameList, List<Object> whereColValList, String extraWhereClause, int noDataInPeriod, int decimalScale)
	throws SQLException
	{
		SparklineResult res = new SparklineResult();

		// Check input
		if (whereColNameList == null || whereColValList == null) throw new IllegalArgumentException("getSparclineData(): None of this can be null. whereColNameList=" + whereColNameList + ", whereColValList=" + whereColValList);
		if (whereColNameList.size() != whereColValList.size())   throw new IllegalArgumentException("getSparclineData(): ColNameList and ColValList must be of same size. whereColNameList.size()=" + whereColNameList.size() + ", whereColValList.size()=" + whereColValList.size());
		if (whereColNameList.isEmpty())                          throw new IllegalArgumentException("getSparclineData(): ColNameList and ColValList must have at least 1 entry. whereColNameList.size()=" + whereColNameList.size() + ", whereColValList.size()=" + whereColValList.size());
		
		LocalDateTime tmpBegin = beginLdt;
		LocalDateTime tmpEnd   = beginLdt.plusMinutes(durationInMinutes);
		
		StringBuilder sb = new StringBuilder();
		DateTimeFormatter tsFormat = DateTimeFormatter.ISO_DATE_TIME;
		
		// If sumColName is an expression or if it's a USER_PROVIDED aggregation, then do not Quote it...
		String colName = (sumColNameIsExpr || AggType.USER_PROVIDED.equals(aggType)) ? sumColName : "[" + sumColName + "]";

		sb.append("with [tt] as ( \n");
//		sb.append("              select cast('" + tmpBegin.format(tsFormat) + "' as datetime) as [bts], cast('" + tmpEnd.format(tsFormat) + "' as datetime) as [ets] \n");
		sb.append("              select '" + tmpBegin.format(tsFormat) + "' as [bts], '" + tmpEnd.format(tsFormat) + "' as [ets] \n");

		while(tmpEnd.isBefore(endLdt) )
		{
			tmpBegin = tmpBegin.plusMinutes(durationInMinutes);
			tmpEnd   = tmpEnd.plusMinutes(durationInMinutes);

			sb.append("    union all select '" + tmpBegin.format(tsFormat) + "' as [bts], '" + tmpEnd.format(tsFormat) + "' as [ets] \n");
		}
		
		sb.append(") \n");
		
		String aggStr =  aggType + "(" + colName + ")";
		if (AggType.USER_PROVIDED.equals(aggType))
			aggStr = colName;
		
		sb.append("select \n");
		sb.append("     [bts] \n");
		sb.append("    ,[ets] \n");
		sb.append("    ,( select coalesce((" + aggStr + "), " + noDataInPeriod + ") \n");
		sb.append("       from [" + tabName + "] s \n");
		sb.append("       where s.[" + tabPeriodColName + "] between [tt].[bts] and [tt].[ets] \n");
		for (int i=0; i<whereColNameList.size(); i++)
		{
			sb.append("         and [" + whereColNameList.get(i) + "] = " + DbUtils.safeStrHexAsBinary(conn, whereColValList.get(i)) + " \n");
		}
		if (StringUtil.hasValue(extraWhereClause))
		{
			sb.append("         " + extraWhereClause + " \n");
		}
		sb.append("     ) as [sumData] \n");
		sb.append("from [tt] \n");

		String sql = sb.toString();
		
//System.out.println("getSparclineData(): SQL=\n" + sql);

		SimpleDateFormat sdf_HM  = new SimpleDateFormat("HH:mm");
		SimpleDateFormat sdf_YMD = new SimpleDateFormat("yyyy-MM-dd");
	
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(conn.quotifySqlString(sql)))
		{
			ResultSetMetaData rsmd = rs.getMetaData();
			int valDt = rsmd.getColumnType(3); // for column 3 -- the aggregated column

			while(rs.next())
			{
				Timestamp bts = rs.getTimestamp(1);
				Timestamp ets = rs.getTimestamp(2);
//				int       val = rs.getInt      (3);
//				Number    val = rs.getInt      (3);
				Number    val = null;

				switch (valDt)
				{
				case Types.TINYINT:
				case Types.SMALLINT:
				case Types.INTEGER:
					val = rs.getInt(3);
					break;

				case Types.BIGINT:
					val = rs.getLong(3);
					break;

				case Types.REAL:
				case Types.FLOAT:
				case Types.DOUBLE:
				case Types.DECIMAL:
				case Types.NUMERIC:
					BigDecimal bdVal = rs.getBigDecimal(3);
					if (bdVal != null)
					{
						if (decimalScale >= 0)
							bdVal = bdVal.setScale(decimalScale, RoundingMode.HALF_UP); // HALF_UP is better than HALF_EVEN 
					}
					val = bdVal;
					break;

				default:
					throw new RuntimeException("Unhandled datatype=" + valDt + ", '" + ResultSetTableModel.getColumnJavaSqlTypeName(valDt)+ "' when reading aggreate column for 'sparkline' chart. SQL=|" + sql + "|");
				}
				

				String tooltip = sdf_HM.format(bts) + " - " + sdf_HM.format(ets) + " @ [" + sdf_YMD.format(bts) + "]";

				res.values  .add(val);
				res.tooltips.add(tooltip);
				res.beginTs .add(bts);
			}
		}

//System.out.println("getSparclineData(): <<<<< " + res.values);
		return res;
	}
	
//	public static void main(String[] args)
//	{
//		LocalDateTime begin = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
//		LocalDateTime end   = begin.plusHours(24);
//		
//		SparkLineParams params = SparkLineParams.create()
//				.setHtmlChartColumnName      ("CpuTime")
//				.setHtmlWhereKeyColumnName   ("NormJavaSqlHashCode")
//				.setDbmsTableName            ("MonSqlCapStatements")
//				.setDbmsSampleTimeColumnName ("StartTime")
//				.setDbmsDataValueColumnName  ("CpuTime")   
//				.setDbmsWhereKeyColumnName   ("NormJavaSqlHashCode")
//				.setSparklineTooltipPostfix  ("CPU Time in ms")
//				;
//
//		try
//		{
//			SparklineHelper.getSparclineData(
//					null, 
//					begin, 
//					end, 
//					params.getGroupDataInMinutes(),
//					params.getDbmsTableName(), 
//					params.getDbmsSampleTimeColumnName(), 
//					params.getDbmsDataValueColumnName(), 
//					params.getDbmsDataValueColumnNameIsExpression(), 
//					params.getDbmsWhereKeyColumnName(), 
//					Integer.MAX_VALUE, 
//					params.getNoDataInGroupDefaultValue());
//		}
//		catch (SQLException ex)
//		{
//			ex.printStackTrace();
//		}
//	}
}


// with [tt] as (
//         select '2020-11-12 00:00:00' as [bts], '2020-11-12 00:10:00' as [ets]
// union all select '2020-11-12 00:10:00','2020-11-12 00:20:00'
// union all select '2020-11-12 00:20:00','2020-11-12 00:30:00'
// union all select '2020-11-12 00:40:00','2020-11-12 00:50:00'
// union all select '2020-11-12 00:50:00','2020-11-12 01:00:00'
// 
// union all select '2020-11-12 01:00:00','2020-11-12 01:10:00'
// union all select '2020-11-12 01:10:00','2020-11-12 01:20:00'
// union all select '2020-11-12 01:20:00','2020-11-12 01:30:00'
// union all select '2020-11-12 01:40:00','2020-11-12 01:50:00'
// union all select '2020-11-12 01:50:00','2020-11-12 02:00:00'
// 
// union all select '2020-11-12 02:00:00','2020-11-12 02:10:00'
// union all select '2020-11-12 02:10:00','2020-11-12 02:20:00'
// union all select '2020-11-12 02:20:00','2020-11-12 02:30:00'
// union all select '2020-11-12 02:40:00','2020-11-12 02:50:00'
// union all select '2020-11-12 02:50:00','2020-11-12 03:00:00'
// 
// union all select '2020-11-12 03:00:00','2020-11-12 03:10:00'
// union all select '2020-11-12 03:10:00','2020-11-12 03:20:00'
// union all select '2020-11-12 03:20:00','2020-11-12 03:30:00'
// union all select '2020-11-12 03:40:00','2020-11-12 03:50:00'
// union all select '2020-11-12 03:50:00','2020-11-12 04:00:00'
// 
// union all select '2020-11-12 04:00:00','2020-11-12 04:10:00'
// union all select '2020-11-12 04:10:00','2020-11-12 04:20:00'
// union all select '2020-11-12 04:20:00','2020-11-12 04:30:00'
// union all select '2020-11-12 04:40:00','2020-11-12 04:50:00'
// union all select '2020-11-12 04:50:00','2020-11-12 05:00:00'
// 
// union all select '2020-11-12 05:00:00','2020-11-12 05:10:00'
// union all select '2020-11-12 05:10:00','2020-11-12 05:20:00'
// union all select '2020-11-12 05:20:00','2020-11-12 05:30:00'
// union all select '2020-11-12 05:40:00','2020-11-12 05:50:00'
// union all select '2020-11-12 05:50:00','2020-11-12 06:00:00'
// 
// union all select '2020-11-12 06:00:00','2020-11-12 06:10:00'
// union all select '2020-11-12 06:10:00','2020-11-12 06:20:00'
// union all select '2020-11-12 06:20:00','2020-11-12 06:30:00'
// union all select '2020-11-12 06:40:00','2020-11-12 06:50:00'
// union all select '2020-11-12 06:50:00','2020-11-12 07:00:00'
// 
// union all select '2020-11-12 07:00:00','2020-11-12 07:10:00'
// union all select '2020-11-12 07:10:00','2020-11-12 07:20:00'
// union all select '2020-11-12 07:20:00','2020-11-12 07:30:00'
// union all select '2020-11-12 07:40:00','2020-11-12 07:50:00'
// union all select '2020-11-12 07:50:00','2020-11-12 08:00:00'
// 
// union all select '2020-11-12 08:00:00','2020-11-12 08:10:00'
// union all select '2020-11-12 08:10:00','2020-11-12 08:20:00'
// union all select '2020-11-12 08:20:00','2020-11-12 08:30:00'
// union all select '2020-11-12 08:40:00','2020-11-12 08:50:00'
// union all select '2020-11-12 08:50:00','2020-11-12 09:00:00'
// )
// select
//      [bts] 
//     ,[ets] 
//     ,(select isnull(sum([CpuTime]),-1) from [MonSqlCapStatements] s where s.[StartTime] between [tt].[bts] and [tt].[ets] and [NormJavaSqlHashCode] = -1126870225) as [CpuTime] 
// from [tt]
