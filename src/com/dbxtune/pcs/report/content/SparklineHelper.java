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
package com.dbxtune.pcs.report.content;

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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

public class SparklineHelper
{
	private static Logger _logger = Logger.getLogger(SparklineHelper.class);

	public static final String PROPKEY_getSparclineData_timeout = "SparklineHelper.getSparclineData.timeout";
	public static final int    DEFAULT_getSparclineData_timeout = 600;
	
//	/**
//	 */
//	public enum ColumnNameType
//	{
//		SINGLE_COLUMN_NAME, 
//		MULTI_COLUMN_NAME, 
//		SQL_EXPRESSION
//	};

	public enum DataSource
	{
		/** All Normal CM (Counter Model) */
		CounterModel,

		/** SQL Server Query Store */
		QueryStore,
	};

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
//		private DataSource  _dataSource = DataSource.CounterModel;
		private DataSource  _dataSource;

		private String  _htmlChartColumnName;
		private String  _htmlWhereKeyColumnName;
//		private boolean _htmlWhereKeyColumnNameIsList        = false;
		private String  _dbmsSchemaName;
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
		private String  _sparklineClassNamePrefix;
		private String  _sparklineClassName;
		private String  _sparklineTooltipPostfix;
		private String  _noBrowserText = "Only in Web Browser";

		public SparkLineParams(DataSource dataSource)
		{
			_dataSource = dataSource;
		}

		public static SparkLineParams create(DataSource dataSource)
		{	
			return new SparkLineParams(dataSource);
		}

		/** What is the "source" for this SparkLine */
		public SparkLineParams setDataSource                         (DataSource dataSource                      ) { _dataSource                          = dataSource;                          return this; }

		/** HTML Table column name to replace with a mini-chart */
		public SparkLineParams setHtmlChartColumnName                (String  htmlChartColumnName                ) { _htmlChartColumnName                 = htmlChartColumnName;                 return this; }

		/** HTML Table column name to get data for when building a where clause against in the SQL query */
		public SparkLineParams setHtmlWhereKeyColumnName             (String  htmlWhereKeyColumnName             ) { _htmlWhereKeyColumnName              = htmlWhereKeyColumnName;              return this; }

		/** DBMS schema name to use in query */
		public SparkLineParams setDbmsSchemaName                     (String  dbmsSchemaName                     ) { _dbmsSchemaName                      = dbmsSchemaName;                      return this; }

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
		public SparkLineParams setSparklineClassNamePrefix           (String  sparklineClassNamePrefix           ) { _sparklineClassNamePrefix            = StringUtil.stripAllNonAlphaNum(sparklineClassNamePrefix); return this; }

		/** HTML class-name to use for this mini-chart */
		public SparkLineParams setSparklineClassName                 (String  sparklineClassName                 ) { _sparklineClassName                  = sparklineClassName;                  return this; }

		/** When showing tool tip for this value, this can be used to <i>describe</i> what it represents */
		public SparkLineParams setSparklineTooltipPostfix            (String  sparklineTooltipPostfix            ) { _sparklineTooltipPostfix             = sparklineTooltipPostfix;             return this; }

		/** If we are reading this from a Mail reader that do not support JavaScript, then this value will be used as a placement instead of the mini-chart */
		public SparkLineParams setNoBrowserText                      (String  noBrowserText                      ) { _noBrowserText                       = noBrowserText;                       return this; }

		public DataSource getDataSource()                          { return _dataSource; }
		public String     getHtmlChartColumnName()                 { return _htmlChartColumnName; }
		public String     getHtmlWhereKeyColumnName()              { return _htmlWhereKeyColumnName; }
		public String     getDbmsSchemaName()                      { return _dbmsSchemaName; }
		public String     getDbmsTableName()                       { return _dbmsTableName; }
		public String     getDbmsSampleTimeColumnName()            { return _dbmsSampleTimeColumnName; }
		public String     getDbmsDataValueColumnName()             { return _dbmsDataValueColumnName; }
		public boolean    getDbmsDataValueColumnNameIsExpression() { return _dbmsDataValueColumnNameIsExpression; }
		public String     getDbmsWhereKeyColumnName()              { return _dbmsWhereKeyColumnName        != null ? _dbmsWhereKeyColumnName          : getHtmlWhereKeyColumnName(); }
//		public boolean    getDbmsWhereKeyColumnNameIsList()        { return _dbmsWhereKeyColumnNameIsList; }
		public boolean    getDbmsWhereKeyColumnNameIsList()        { return getDbmsWhereKeyColumnName().contains(","); }
		public String     getDbmsExtraWhereClause()                { return _dbmsExtraWhereclause          != null ? _dbmsExtraWhereclause            : ""; }
		public AggType    getGroupDataAggregationType()            { return _groupDataAggregationType; }
		public int        getGroupDataInMinutes()                  { return _groupDataInMinutes; }
		public int        getNoDataInGroupDefaultValue()           { return _noDataInGroupDefaultValue; }
		public int        getDecimalScale()                        { return _decimalScale; }
		public String     getSparklineClassNamePrefix()            { return _sparklineClassNamePrefix; }
//		public String     getSparklineClassName()                  { return _sparklineClassName            != null ? _sparklineClassName              : "sparklines_" + getHtmlChartColumnName(); }
		public String     getSparklineTooltipPostfix()             { return _sparklineTooltipPostfix       != null ? " - " + _sparklineTooltipPostfix : " - " + getGroupDataAggregationType() + " '" + getDbmsDataValueColumnName() +"' in below time period" ; }
		public String     getNoBrowserText()                       { return _noBrowserText                 != null ? _noBrowserText                   : ""; }

		public String     getSparklineClassName()                  
		{ 
			String prefix = _sparklineClassNamePrefix == null ? "" : _sparklineClassNamePrefix + "_";
					
			if (_sparklineClassName != null)
				return prefix + _sparklineClassName;
			
			return "sparklines_" + prefix + getHtmlChartColumnName(); 
		}

		/**
		 * Validate that all settings are OK
		 * @return The instance
		 * @throws RuntimeException is case of issues
		 */
		public SparkLineParams validate()
		{
			// Check if everything is OK
			String problem = null;

			if (getDataSource() == null)
			{
				problem = "'DataSource' has NOT been assigned. ";
			}

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

		public boolean isDataSourceIn(DataSource... dataSource)
		{
			if (dataSource == null)
				return false;

			for (DataSource ds : dataSource)
			{
				if (getDataSource().equals(ds))
					return true;
			}
			
			return false;
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

		String htmlChartColumnName = params.getHtmlChartColumnName();
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
			if (params.isDataSourceIn(DataSource.CounterModel))
			{
				String indexDdl = "";
				try
				{
					String schemaName = params.getDbmsSchemaName();
					String tableName  = params.getDbmsTableName();
					String ixSchName  = "";

					if (StringUtil.hasValue(schemaName))
					{
						ixSchName = schemaName.replaceAll("[^a-zA-Z0-9]", "") + "_"; // remove anything suspect chars keep only: a-z and numbers
					}

					String indexName  = "ix_DSR_" + ixSchName + tableName;
					String colNames   = "";
					String ixPostFix  = "";

					for (String colName : StringUtil.parseCommaStrToList(params.getDbmsWhereKeyColumnName()))
					{
						colNames += "[" + colName + "], ";
						ixPostFix += "_" + colName.replaceAll("[^a-zA-Z0-9]", ""); // remove anything suspect chars keep only: a-z and numbers
					}
					colNames += "[" + params.getDbmsSampleTimeColumnName() + "]";

					// add "column names" as a "postfix" at the end of the index name (there might be more than one index)
					indexName += ixPostFix;

					// Check if index already exists
					boolean indexExists = false;
					try (ResultSet rs = conn.getMetaData().getIndexInfo(null, schemaName, tableName, false, true))
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
						String schemaStr = StringUtil.isNullOrBlank(schemaName) ? "" : "[" + schemaName + "].";
						indexDdl = conn.quotifySqlString("create index [" + indexName + "] on " + schemaStr + "[" + tableName + "] (" + colNames + ")");
						
						long startTime = System.currentTimeMillis();
						try (Statement stmnt = conn.createStatement())
						{
							stmnt.executeUpdate(indexDdl);
						}
						_logger.info("ReportEntry '" + reportEntry.getClass().getSimpleName() + "'. Created helper index to support Daily Summary Report. ChartColumnName='" + htmlChartColumnName + "', SQL='" + indexDdl + "' ExecTime=" + TimeUtils.msDiffNowToTimeStr(startTime));
					}
					else
					{
						// Write INFO on first "index already existed"
						if ( reportEntry.writeInfoOnIndexAlreadyExisted(indexName) )
							_logger.info("ReportEntry '" + reportEntry.getClass().getSimpleName() + "'. SKIPPED Creating helper index to support Daily Summary Report (it already exists). IndexName='" + indexName + "', ChartColumnName='" + htmlChartColumnName + "', SQL='" + indexDdl + "'.");
					}
				}
				catch (SQLException ex)
				{
					_logger.error("ReportEntry '" + reportEntry.getClass().getSimpleName() + "'. Problems creating a helper index, skipping the error and continuing... ChartColumnName='" + htmlChartColumnName + "', SQL=|" + indexDdl + "|.", ex);
				}
			}

			// get value from DBMS
			try
			{
				result = SparklineHelper.getSparclineData(
						conn, 
						params.getDataSource(),
						reportBegin, 
						reportEnd, 
						params.getGroupDataInMinutes(),
						params.getGroupDataAggregationType(),
						params.getDbmsSchemaName(), 
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
		return getJavaScriptInitCode(reportEntry, result, params.getSparklineClassName(), params.getSparklineTooltipPostfix());
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

	public static String getJavaScriptInitCode(IReportEntry reportEntry, SparklineResult result, String sparklineClassName, String tooltipPostfix)
	{
		StringBuilder sb = new StringBuilder();

		// Only write this once (keep the status field in the "head" report)
		if (reportEntry.hasStatusEntry("jsLoadSparkline_writeOnce") == false)
		{
			reportEntry.setStatusEntry("jsLoadSparkline_writeOnce");

			String name = "sparkline";
			String label = "Initializing Sparklines: ";
			String topPx = "20px";
			
			sb.append("\n");
			sb.append("\n");
			sb.append("<div id='" + name + "-progress-div' style='display:none'> \n");
			sb.append("  <label for='" + name + "-progress-bar'>" + label + "</label> \n");
			sb.append("  <progress id='" + name + "-progress-bar' max='100' style='height: 20px; width:80%;'></progress> \n");
			sb.append("  <button id='" + name + "-stop-progress-but' onclick='stopSparklineInit()' type='button' class='btn btn-primary btn-sm'>Stop</button> \n");
			sb.append("</div>\n");

			sb.append("\n");
			sb.append("<script type='text/javascript'>\n");
			sb.append("\n");
			sb.append("    // Variable to hold all sparkline objects \n");
			sb.append("    const sparklineListToLoad  = []; \n");
			sb.append("    const sparklineListCreated = []; \n");
			sb.append("      var sparklineListMax     = 0; \n");
			sb.append("    const sparklineConfMap     = new Map(); \n");
			sb.append("\n");
			
			sb.append("    // function called when pressing 'Stop' button right next to the progress bar \n");
			sb.append("    function stopSparklineInit() \n");
			sb.append("    { \n");
			sb.append("        console.log('-stopSparklineInit-');  \n");
			sb.append("        while (sparklineListToLoad.length !== 0) \n");
			sb.append("            sparklineListToLoad.shift(); \n");
			sb.append("    } \n");
			
			sb.append("    // function to be called at page load, which will initialize all Charts, (and update progressbar) \n");
			sb.append("    function loadNextSparkline() \n");
			sb.append("    { \n");

			sb.append("        // Enable the progresbar; \n");
			sb.append("        if (sparklineListCreated.length === 0) \n");
			sb.append("        { \n");
			sb.append("            console.log('-load-first-" + name + "-');  \n");
			sb.append("            sparklineListMax = sparklineListToLoad.length; \n");
			
			sb.append("            // show the progressbar\n");
			sb.append("            document.getElementById('" + name + "-progress-div').style.display = 'block'; \n");  // show
			
			sb.append("            // if possible move the div into the 'progress-area' or add some attributes to it. \n");
			sb.append("            if (document.getElementById('progress-area')) \n");
			sb.append("            { \n");
			sb.append("                console.log('Moving div: " + name + "-progress-div --to--> div: progress-area');  \n");
			sb.append("                $('#" + name + "-progress-div').detach().appendTo('#progress-area'); \n");
			sb.append("            } \n");
			sb.append("            else \n");
			sb.append("            { \n");
			sb.append("                console.log('Cant find div: progress-area. instead; Setting some css options for div: " + name + "-progress-div');  \n");
			sb.append("                $('#" + name + "-progress-div').css({'position':'fixed', 'background-color':'white', 'top':'" +topPx + "', 'left':'20px', 'width':'100%'}); \n");
			sb.append("            } \n");
			sb.append("        } \n");

			sb.append("        // Disable the progresbar; \n");
			sb.append("        if (sparklineListToLoad.length === 0) \n");
			sb.append("        { \n");
			sb.append("            console.log('-end-of-" + name + "-to-load-');  \n");
			sb.append("            // hide the progressbar\n");
			sb.append("            document.getElementById('" + name + "-progress-div').style.display = 'none'; \n");   // hide
			sb.append("            return; \n");
			sb.append("        } \n");

			sb.append("        var tagName = sparklineListToLoad.shift(); \n");
			sb.append("        var config  = sparklineConfMap.get(tagName); \n");

			sb.append("        var pctLoaded = sparklineListCreated.length / sparklineListMax * 100; \n");
			sb.append("        document.getElementById('" + name + "-progress-bar').value = pctLoaded; \n");
			
			sb.append("        console.log('-creating-sparkline: ' + tagName + ', toLoadListSize=' + sparklineListToLoad.length);  \n");

			sb.append("        // Initialize all mini charts -- sparklines   \n");
			sb.append("        $('.' + tagName).sparkline('html', config);  \n");

//			sb.append("        sparklineListCreated.push(sparkline); \n");
			sb.append("        sparklineListCreated.push(tagName); \n");
			sb.append("\n");
			sb.append("        // now show the Max: ### overlay  \n");
			sb.append("        $('.sparkline-max-val').css('display', 'block');  \n");
			
//			sb.append("        // HIDE the image, and SHOW the chart! \n");
//			sb.append("        document.getElementById('img_'       + tagName).style.display = 'none'; \n");   // hide
//			sb.append("        document.getElementById('div_chart_' + tagName).style.display = 'block'; \n");  // show

			sb.append("        // Load next chart \n");
			sb.append("        setTimeout(loadNextSparkline, 10); \n");
			sb.append("    }\n");
			sb.append("\n");
			sb.append("    // Call the function loadNextSparkline() for the FIRST time \n");
			sb.append("    document.addEventListener('DOMContentLoaded', function() \n");
			sb.append("    { \n");
			sb.append("        loadNextSparkline(); \n");
			sb.append("    }); \n");
			sb.append("\n");
			sb.append("</script>\n");
			sb.append("\n");
			sb.append("\n");
		}


		// write a "timeArray", which will be used by 'JQuery sparkline' tooltip functionality. 
		if (result != null && result.tooltips != null && ! result.tooltips.isEmpty())
		{
			// Get tool tip and escape any single quotes in the tool tip text 
			if (tooltipPostfix != null)
				tooltipPostfix = tooltipPostfix.replace("'", "\\'");
			
			// The below tries to make pixels *wider* when we have few records... minPixels=3 (the default), maxPixels=9
			int maxMixelWidth = 432;   // 432 is based on defaultWidth=3 * 24h and intervalOf 10 minutes... 342==(24*6*3) hours*valuesPerHour*pixelWidth
			int pixels = maxMixelWidth / result.values.size();

			if (pixels > 9) pixels = 9;
			if (pixels < 3) pixels = 3;

//			// if we want to use BAR as type in the Sparkline, the below might be a good fit (produces the same size as; type: 'line') 
//			sb.append("                type: 'bar',							  												\n");
//			sb.append("                nullColor: 'pink',					  												\n");
//			sb.append("                barWidth: 2,							  												\n");
//			sb.append("                barSpacing: 1,						  												\n");




//			sb.append("<script type='text/javascript'> \n");
//			sb.append("\n");
//			sb.append("    // Execute when page has LOADED \n");
//			sb.append("    document.addEventListener('DOMContentLoaded', function() \n");
//			sb.append("    { \n");
//			sb.append("        // do: deferred (so we dont block the event tread at initial load) \n");
//			sb.append("        setTimeout(function() \n");
//			sb.append("        { \n");
//			sb.append("            // Initialize all mini charts -- sparklines \n");
//			sb.append("            $('." + sparklineClassName + "').sparkline('html', {										\n");
//			sb.append("                type: 'line',						  												\n");
//			sb.append("                defaultPixelsPerValue: ").append(pixels).append(",									\n");
////			sb.append("                type: 'bar',						  													\n");
//			sb.append("                chartRangeMin: 0,					  												\n");
//			sb.append("                tooltipFormat: '<span style=\"font-size:12px;\"><span style=\"color: {{color}}\">&#9679;</span> {{y}}" + tooltipPostfix + "<br>&nbsp;&nbsp; {{offset:toolTipArray}}</span>',	\n");
//			sb.append("                tooltipValueLookups: { 				  												\n");
//			sb.append("                    toolTipArray: { 																	\n");
//			String comma = " ";
//			int    pos   = 0;
//			for (String str : result.tooltips)
//			{
//				sb.append("                        " + comma + pos + ": '" + str + "' \n");
//				comma = ",";
//				pos++;
//			}
//			sb.append("                    }																				\n");
//			sb.append("                }																					\n");
//			sb.append("            });																						\n");
//			sb.append("            // now show the Max: ### overlay															\n");
//			sb.append("            $('.sparkline-max-val').css('display', 'block');											\n");
//			sb.append("        }, 100); \n");
//			sb.append("    }); \n");
//			sb.append("\n");
//			sb.append("\n");
//			sb.append("</script> \n");
			sb.append("<script type='text/javascript'> \n");
			sb.append("\n");
			sb.append("    // add name to a variable to load 'later'. \n");
			sb.append("    sparklineListToLoad.push('" + sparklineClassName + "'); 									\n"); 
			sb.append("\n");
			sb.append("    // add config to a map to load 'later'. \n");
			sb.append("    sparklineConfMap.set('" + sparklineClassName + "', {										\n");
			sb.append("        type: 'line',						  												\n");
			sb.append("        defaultPixelsPerValue: ").append(pixels).append(",									\n");
//			sb.append("        type: 'bar',						  													\n");
			sb.append("        chartRangeMin: 0,					  												\n");
			sb.append("        tooltipFormat: '<span style=\"font-size:12px;\"><span style=\"color: {{color}}\">&#9679;</span> {{y}}" + tooltipPostfix + "<br>&nbsp;&nbsp; {{offset:toolTipArray}}</span>',	\n");
			sb.append("        tooltipValueLookups: { 				  												\n");
			sb.append("            toolTipArray: { 																	\n");
			String comma = " ";
			int    pos   = 0;
			for (String str : result.tooltips)
			{
				sb.append("                        " + comma + pos + ": '" + str + "' \n");
				comma = ",";
				pos++;
			}
			sb.append("            }																				\n");
			sb.append("        }																					\n");
			sb.append("    });																						\n");
			sb.append("\n");
			sb.append("\n");
			sb.append("</script> \n");
		}

		return sb.toString();
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
				maxValue = Math.max(maxValue.doubleValue(), (val == null) ? 0 : val.doubleValue());
			}
			return maxValue;
		}

		public String getMaxValueLabel()
		{
			return "Max: " + NumberFormat.getInstance().format( getMaxValue() );
		}
	}

//	public static void main(String[] args)
//	{
//		LocalDateTime dt  = LocalDateTime.now();
//		LocalDateTime adt = adjustTimeToLowerMinuteBound(dt, 15);
//		
//		System.out.println("NOW: " + dt);
//		System.out.println("ADJ: " + adt);
//		
//	}
	/**
	 * Adjust time to the lower bound of the "minuteSpan"
	 * <p>
	 * <b>NOTE: NOT Fully tested</b> 
	 * <p>
	 * Example
	 * <pre>
	 *    adjustTimeToLowerMinuteBound(...T17:18:01,  1); --->>> 17:18 
	 *    adjustTimeToLowerMinuteBound(...T17:18:01,  5); --->>> 17:15 
	 *    adjustTimeToLowerMinuteBound(...T17:18:01, 10); --->>> 17:10 
	 * </pre>
	 * 
	 * @param dt                LocalDateTime to "truncate"
	 * @param minuteSpan        The minute to "bound" to...
	 * @return
	 */
	public static LocalDateTime adjustTimeToLowerMinuteBound(LocalDateTime dt, int minuteSpan)
	{
		if (minuteSpan == 0)
			minuteSpan = 1;

		int atMinute = dt.getMinute();

		int newMinute = atMinute / minuteSpan * minuteSpan;
		
		LocalDateTime tmp = dt.withMinute(newMinute).truncatedTo(ChronoUnit.MINUTES);
		return tmp;
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
	public static SparklineResult getSparclineData(DbxConnection conn, DataSource dataSource, LocalDateTime beginLdt, LocalDateTime endLdt, int durationInMinutes, AggType aggType, String schName, String tabName, String tabPeriodColName, String sumColName, boolean sumColNameIsExpr, List<String> whereColNameList, List<Object> whereColValList, String extraWhereClause, int noDataInPeriod, int decimalScale)
	throws SQLException
	{
		SparklineResult res = new SparklineResult();

		// Check input
		if (whereColNameList == null || whereColValList == null) throw new IllegalArgumentException("getSparclineData(): None of this can be null. whereColNameList=" + whereColNameList + ", whereColValList=" + whereColValList);
		if (whereColNameList.size() != whereColValList.size())   throw new IllegalArgumentException("getSparclineData(): ColNameList and ColValList must be of same size. whereColNameList.size()=" + whereColNameList.size() + ", whereColValList.size()=" + whereColValList.size());
		if (whereColNameList.isEmpty())                          throw new IllegalArgumentException("getSparclineData(): ColNameList and ColValList must have at least 1 entry. whereColNameList.size()=" + whereColNameList.size() + ", whereColValList.size()=" + whereColValList.size());
		
//		LocalDateTime tmpBegin = beginLdt;
//		LocalDateTime tmpEnd   = beginLdt.plusMinutes(durationInMinutes);
		LocalDateTime tmpBegin = adjustTimeToLowerMinuteBound(beginLdt, durationInMinutes);
		LocalDateTime tmpEnd   = tmpBegin.plusMinutes(durationInMinutes);
		
		StringBuilder sqlSb = new StringBuilder();
		DateTimeFormatter tsFormat = DateTimeFormatter.ISO_DATE_TIME;
		
		// If sumColName is an expression or if it's a USER_PROVIDED aggregation, then do not Quote it...
		String colName = (sumColNameIsExpr || AggType.USER_PROVIDED.equals(aggType)) ? sumColName : "[" + sumColName + "]";

		// The below build produces something like:
		//-------------------------------------------------------------------------------------
		// with [tt] ([bts], [ets]) as ( 
		//     values
		//     ('2023-04-11T00:00:00', '2023-04-11T00:10:00') 
		//    ,('2023-04-11T00:10:00', '2023-04-11T00:20:00') 
		//    ,('2023-04-11T00:20:00', '2023-04-11T00:30:00') 
		//    ... a lot more rows here ...
		//    ,('2023-04-17T23:30:00', '2023-04-17T23:40:00') 
		//    ,('2023-04-17T23:40:00', '2023-04-17T23:50:00') 
		//    ,('2023-04-17T23:50:00', '2023-04-18T00:00:00') 
		// ) 
		// select 
		//      [bts] 
		//     ,[ets] 
		//     ,( select coalesce((AVG([someColumn])), 0) 
		//        from [someTable_abs] s 
		//        where s.[SessionSampleTime] between [tt].[bts] and [tt].[ets] 
		//          and [someColumn] = 'someValue' 
		//      ) as [someLabelName] 
		// from [tt] 
		//-------------------------------------------------------------------------------------
		// Previously I used:
		// with ....
		//               select '2023-04-11T00:00:00' as [bts], '2023-04-11T00:10:00' as [ets]
		//     union all select '2023-04-11T00:10:00' as [bts], '2023-04-11T00:20:00' as [ets]
		// But that caused H2 to stack trace: java.lang.StackOverflowError ... so I switched to values... which also build a ResultSet [tt] used later on...

		// BUILD SQL (based on what DataSource)
		if (DataSource.CounterModel.equals(dataSource))
		{
			sqlSb.append("with [tt] ([bts], [ets]) as ( \n");
			sqlSb.append("    values \n");
//			sb.append("              select cast('" + tmpBegin.format(tsFormat) + "' as datetime) as [bts], cast('" + tmpEnd.format(tsFormat) + "' as datetime) as [ets] \n");
//			sqlSb.append("              select '" + tmpBegin.format(tsFormat) + "' as [bts], '" + tmpEnd.format(tsFormat) + "' as [ets] \n");
			sqlSb.append("    ('" + tmpBegin.format(tsFormat) + "', '" + tmpEnd.format(tsFormat) + "') \n");

			while(tmpEnd.isBefore(endLdt) )
			{
				tmpBegin = tmpBegin.plusMinutes(durationInMinutes);
				tmpEnd   = tmpEnd.plusMinutes(durationInMinutes);

//				sqlSb.append("    union all select '" + tmpBegin.format(tsFormat) + "' as [bts], '" + tmpEnd.format(tsFormat) + "' as [ets] \n");
				sqlSb.append("   ,('" + tmpBegin.format(tsFormat) + "', '" + tmpEnd.format(tsFormat) + "') \n");
			}
			
			sqlSb.append(") \n");
			
			String aggStr =  aggType + "(" + colName + ")";
			if (AggType.USER_PROVIDED.equals(aggType))
				aggStr = colName;
			
			String schemaStr = StringUtil.isNullOrBlank(schName) ? "" : "[" + schName + "].";

			sqlSb.append("select \n");
			sqlSb.append("     [bts] \n");
			sqlSb.append("    ,[ets] \n");
			sqlSb.append("    ,( select coalesce((" + aggStr + "), " + noDataInPeriod + ") \n");
			sqlSb.append("       from " + schemaStr + "[" + tabName + "] s \n");
			sqlSb.append("       where s.[" + tabPeriodColName + "] between [tt].[bts] and [tt].[ets] \n");
			for (int i=0; i<whereColNameList.size(); i++)
			{
				sqlSb.append("         and [" + whereColNameList.get(i) + "] = " + DbUtils.safeStrHexAsBinary(conn, whereColValList.get(i)) + " \n");
			}
			if (StringUtil.hasValue(extraWhereClause))
			{
				sqlSb.append("         " + extraWhereClause + " \n");
			}
			sqlSb.append("     ) as [sumData] \n");
			sqlSb.append("from [tt] \n");
		}

		if (DataSource.QueryStore.equals(dataSource))
		{
			String aggStr =  aggType + "(" + colName + ")";
			if (AggType.USER_PROVIDED.equals(aggType))
				aggStr = colName;
			
			String schemaStr = StringUtil.isNullOrBlank(schName) ? "" : "[" + schName + "].";

			//Example SQL that we construct
			//-------------------------------
			// select 
			//      i.[start_time]
			//     ,i.[end_time]
			//     ,sum(rs.[count_executions]) as [sumData]
			// from [" + _schemaName + "].[query_store_runtime_stats] rs
			// inner join [" + _schemaName + "].[query_store_runtime_stats_interval] i ON rs.[runtime_stats_interval_id] = i.[runtime_stats_interval_id]
			// where rs.[plan_id] = 57
			// group by i.[start_time]
			// order by i.[start_time]
			//-------------------------------
			
			// If we want to keep same "structure" for the SQL as "above" we might do something like the below
			//-------------------------------
			// with [tt] as (
			//    select 
			//         [start_time] as [bts]
			//        ,[end_time]   as [ets]
			//    from [" + _schemaName + "].[query_store_runtime_stats_interval]
			//    order by [start_time]
			// )
			// select
			//      [bts] 
			//     ,[ets] 
			//     ,( select coalesce((sum(s.[count_executions])), 0)
			//        from [" + _schemaName + "].[query_store_runtime_stats] s
			//        where s.[first_execution_time] between [tt].[bts] and [tt].[ets]
			//          and [plan_id] = 57
			//      ) as [sumData] 
			// from [tt]
			//-------------------------------
		
			
//			sqlSb.append("select \n");
//			sqlSb.append("     i.[start_time] as [bts]  \n");
//			sqlSb.append("    ,i.[end_time]   as [ets]  \n");
//			sqlSb.append("    ,coalesce((" + aggStr + "), " + noDataInPeriod + ") as [sumData] \n");
//			sqlSb.append("from " + schemaStr + "[" + tabName + "] s \n");
//			sqlSb.append("left outer join " + schemaStr + "[query_store_runtime_stats_interval] i ON s.[runtime_stats_interval_id] = i.[runtime_stats_interval_id] \n");
//			sqlSb.append("where 1 = 1 \n");
			sqlSb.append("select \n");
			sqlSb.append("     i.[start_time] as [bts]  \n");
			sqlSb.append("    ,i.[end_time]   as [ets]  \n");
			sqlSb.append("    ,coalesce((" + aggStr + "), " + noDataInPeriod + ") as [sumData] \n");
			sqlSb.append("from " + schemaStr + "[query_store_runtime_stats_interval] i \n");
			sqlSb.append("left outer join " + schemaStr + "[" + tabName + "] s ON i.[runtime_stats_interval_id] = s.[runtime_stats_interval_id] \n");
			for (int i=0; i<whereColNameList.size(); i++)
			{
				sqlSb.append("      and s.[" + whereColNameList.get(i) + "] = " + DbUtils.safeStrHexAsBinary(conn, whereColValList.get(i)) + " \n");
			}
			if (StringUtil.hasValue(extraWhereClause))
			{
				sqlSb.append("      " + extraWhereClause + " \n");
			}
			sqlSb.append("group by i.[start_time] \n");
			sqlSb.append("order by i.[start_time] \n");

//System.out.println("getSparclineData(): QUERY_STORE - SQL=\n" + sqlSb.toString());
		}

		String sql = sqlSb.toString();

//System.out.println("SparklineHelper.getSparclineData() SQL: \n" + sql);
		
		SimpleDateFormat sdf_HM  = new SimpleDateFormat("HH:mm");
		SimpleDateFormat sdf_YMD = new SimpleDateFormat("yyyy-MM-dd");
	
		long startTime  = System.currentTimeMillis();
		int  sqlTimeout = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_getSparclineData_timeout, DEFAULT_getSparclineData_timeout); // DEFAULT 10 minutes
		int  rowCount   = 0;

		try (Statement stmnt = conn.createStatement())
		{
			// Set timeout to 10 minutes
			stmnt.setQueryTimeout(sqlTimeout);

			try (ResultSet rs = stmnt.executeQuery(conn.quotifySqlString(sql)))
			{
				ResultSetMetaData rsmd = rs.getMetaData();
//				int btsDt = rsmd.getColumnType(1); // for column 1 -- Begin Time
				int valDt = rsmd.getColumnType(3); // for column 3 -- the aggregated column

				while(rs.next())
				{
					rowCount++;

					Timestamp bts = rs.getTimestamp(1);
					Timestamp ets = rs.getTimestamp(2);
//					int       val = rs.getInt      (3);
//					Number    val = rs.getInt      (3);
					Number    val = null;

//					// Convert to LOCAL Time
//					if (btsDt == Types.TIMESTAMP_WITH_TIMEZONE)
//					{
//						System.out.println("BTS---TIMESTAMP_WITH_TIMEZONE: to local ts: |"+bts+"| ->>> |"+bts.toLocalDateTime()+"|   ("+Timestamp.valueOf(bts.toLocalDateTime())+").");
//						bts = Timestamp.valueOf(bts.toLocalDateTime());
//						ets = Timestamp.valueOf(ets.toLocalDateTime());
//					}
					
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

	//if (DataSource.QueryStore.equals(dataSource))
	//{
//		System.out.println("---- DATA: bts=|"+bts+"|, val=|"+val+"|, tooltip=|"+tooltip+"|.");
	//}
					res.values  .add(val);
					res.tooltips.add(tooltip);
					res.beginTs .add(bts);
				}
			}
		}
		catch (SQLException ex)
		{
			long execTime = TimeUtils.msDiffNow(startTime);
			long sqlTimeoutMs = sqlTimeout * 1000;
			
			// If it looks like a timeout... (add 500ms to execTime) (there is no DBMS Generic Exception to identify a timeout)
			if ((execTime + 500) >= sqlTimeoutMs)
			{
				_logger.warn("SQL Timeout in getSparclineData(): ExecutionTime='" + TimeUtils.msToTimeStrShort(execTime) + "' (" + execTime + " ms). rowCount=" + rowCount + ", SQL=|" + sql + "|", ex);
			}
			
			throw ex;
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
