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
import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.h2.tools.SimpleResultSet;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.gui.ResultSetTableModel.TableStringRenderer;
import com.dbxtune.pcs.DictCompression;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.SparklineHelper;
import com.dbxtune.pcs.report.content.SparklineHelper.AggType;
import com.dbxtune.pcs.report.content.SparklineHelper.DataSource;
import com.dbxtune.pcs.report.content.SparklineHelper.SparkLineParams;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.HtmlTableProducer;
import com.dbxtune.utils.HtmlTableProducer.ColumnCopyDef;
import com.dbxtune.utils.HtmlTableProducer.ColumnCopyRender;
import com.dbxtune.utils.HtmlTableProducer.ColumnCopyRow;
import com.dbxtune.utils.HtmlTableProducer.ColumnStatic;

public class AseTopSlowNormalizedSql extends AseAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _sqlRstm;
//	private Exception           _problem = null;
	private ResultSetTableModel _skippedDsrRows;
//	private String              _sparkline_CpuTime_js = "";
	private List<String>        _miniChartJsList = new ArrayList<>();
	
	private Map<Map<String, Object>, SqlCapExecutedSqlEntries> _keyToExecutedSql;
	private Map<Map<String, Object>, SqlCapWaitEntry>          _keyToWaitTime;
	
	private ReportType _reportType    = ReportType.CPU_TIME;
	private String     _sqlOrderByCol = "-unknown-";
	private String     _sqlHavingCol  = "-unknown-";
	private String     _orderByCol_noBrackets = "-unknown-";
	
	public enum ReportType
	{
		CPU_TIME, 
		WAIT_TIME
	};
	
	public AseTopSlowNormalizedSql(DailySummaryReportAbstract reportingInstance, ReportType reportType)
	{
		super(reportingInstance);
		_reportType = reportType;

		if      (ReportType.CPU_TIME .equals(_reportType)) { _sqlOrderByCol = "[CpuTime__sum]";  _sqlHavingCol = "[CpuTime__sum]"; }
		else if (ReportType.WAIT_TIME.equals(_reportType)) { _sqlOrderByCol = "[WaitTime__sum]"; _sqlHavingCol = "[WaitTime__sum]"; }
		else throw new IllegalArgumentException("Unhandled reportType='" + reportType + "'.");

		_orderByCol_noBrackets = _sqlOrderByCol.replace("[", "").replace("]", "");
	}

	@Override
	public boolean hasMinimalMessageText()
	{
		return false;
	}

	@Override
	public boolean hasShortMessageText()
	{
		if (ReportType.CPU_TIME.equals(_reportType))
			return true;

		return false;
	}

//	@Override
//	public void writeShortMessageText(Writer w)
//	throws IOException
//	{
//	}

	@Override
	public void writeMessageText(Writer sb, MessageType messageType)
	throws IOException
	{
		if (_shortRstm.getRowCount() == 0)
		{
			sb.append("No rows found <br>\n");
		}
		else
		{
			// Full information, including (top summary table)
			if (isFullMessageType())
			{
				// Get a description of this section, and column names
				sb.append(getSectionDescriptionHtml(_shortRstm, true));

				sb.append(createSkippedEntriesReport(_skippedDsrRows));

//				sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
				sb.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
//				sb.append(_shortRstm.toHtmlTableString("sortable"));
//				sb.append(toHtmlTable(_shortRstm));

				// Create a default renderer
				if (_shortRstm != null) // always true... but just to use {} to scope 'tableRender' var
				{
					TableStringRenderer tableRender = new ReportEntryTableStringRenderer()
					{
						@Override
						public String cellValue(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
						{
							if ("txt".equals(colName))
							{
								// Get Actual Executed SQL Text for current 'NormJavaSqlHashCode'
								int    normJavaSqlHashCode = rstm.getValueAsInteger(row, "NormJavaSqlHashCode");
								
								Map<String, Object> whereColValMap = new LinkedHashMap<>();
								whereColValMap.put("NormJavaSqlHashCode", normJavaSqlHashCode);

								String executedSqlText = getSqlCapExecutedSqlTextAsString(_keyToExecutedSql, whereColValMap);

								// Put the "Actual Executed SQL Text" as a "tooltip"
								return "<div title='Click for Detailes' "
										+ "data-toggle='modal' "
										+ "data-target='#dbx-view-sqltext-dialog' "
										+ "data-objectname='" + normJavaSqlHashCode + "' "
										+ "data-tooltip=\""   + executedSqlText     + "\" "
										+ ">&#x1F4AC;</div>"; // symbol popup with "..."
							}

							if ("wait".equals(colName))
							{
								// Get Actual Executed SQL Text for current 'NormJavaSqlHashCode'
								int    normJavaSqlHashCode = rstm.getValueAsInteger(row, "NormJavaSqlHashCode");
								
								Map<String, Object> whereColValMap = new LinkedHashMap<>();
								whereColValMap.put("NormJavaSqlHashCode", normJavaSqlHashCode);

								String waitText = getSqlCapWaitTimeAsString(_keyToWaitTime, whereColValMap);

								// Put the "Actual Executed SQL Text" as a "tooltip"
								return "<div title='Click for Detailes' "
										+ "data-toggle='modal' "
										+ "data-target='#dbx-view-sqltext-dialog' "
										+ "data-objectname='" + normJavaSqlHashCode + "' "
										+ "data-tooltip=\""   + waitText     + "\" "
										+ ">&#x1F4AC;</div>"; // symbol popup with "..."
							}

							return strVal;
						}
					};
					sb.append(_shortRstm.toHtmlTableString("sortable", true, true, null, tableRender));
				}
			}

			// The "sub table" with pivot info on most mini-charts/sparkline
			if (_sqlRstm != null)
			{
				// Create a default renderer
				TableStringRenderer tableRender = new ReportEntryTableStringRenderer()
				{
					@Override
					public String cellValue(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
					{
//						if ("NormSQLText".equals(colName))
//						{
//						//	return "<xmp style='width:100%; max-height:400px; overflow:auto'>" + strVal + "</xmp>";
//							return "<xmp>" + strVal + "</xmp>";
//						}

						if ("txt".equals(colName))
						{
							// Get Actual Executed SQL Text for current 'NormJavaSqlHashCode'
							int normJavaSqlHashCode = rstm.getValueAsInteger(row, "NormJavaSqlHashCode");

							Map<String, Object> whereColValMap = new LinkedHashMap<>();
							whereColValMap.put("NormJavaSqlHashCode", rstm.getValueAsInteger(row, "NormJavaSqlHashCode"));

							String executedSqlText = getSqlCapExecutedSqlTextAsString(_keyToExecutedSql, whereColValMap);

							// Put the "Actual Executed SQL Text" as a "tooltip"
							return "<div title='Click for Detailes' "
									+ "data-toggle='modal' "
									+ "data-target='#dbx-view-sqltext-dialog' "
									+ "data-objectname='" + normJavaSqlHashCode + "' "
									+ "data-tooltip=\""   + executedSqlText     + "\" "
									+ ">&#x1F4AC;</div>"; // symbol popup with "..."
						}

						if ("wait".equals(colName))
						{
							// Get Actual Executed SQL Text for current 'NormJavaSqlHashCode'
							int    normJavaSqlHashCode = rstm.getValueAsInteger(row, "NormJavaSqlHashCode");
							
							Map<String, Object> whereColValMap = new LinkedHashMap<>();
							whereColValMap.put("NormJavaSqlHashCode", normJavaSqlHashCode);

							String waitText = getSqlCapWaitTimeAsString(_keyToWaitTime, whereColValMap);

							// Put the "Actual Executed SQL Text" as a "tooltip"
							return "<div title='Click for Detailes' "
									+ "data-toggle='modal' "
									+ "data-target='#dbx-view-sqltext-dialog' "
									+ "data-objectname='" + normJavaSqlHashCode + "' "
									+ "data-tooltip=\""   + waitText     + "\" "
									+ ">&#x1F4AC;</div>"; // symbol popup with "..."
						}

						return strVal;
					}
				};

				sb.append("<br>\n");
				sb.append("<details open> \n");
				sb.append("<summary>Details for above Statements, including SQL Text (click to collapse) </summary> \n");
				
				sb.append("<br>\n");
				sb.append("SQL Text by NormJavaSqlHashCode: " + _sqlRstm.getRowCount() + "<br>\n");
				sb.append("Tip: To format the SQL text below you can use any online formatting tool, like <a href='https://poorsql.com/'>Poor Man's T-SQL Formatter</a><br>\n");

				sb.append(_sqlRstm.toHtmlTableString("sortable", true, true, null, tableRender));

				sb.append("\n");
				sb.append("</details> \n");
			}
		}
		
		// Write JavaScript code for CPU SparkLine
		if (isFullMessageType())
		{
			for (String str : _miniChartJsList)
				sb.append(str);
		}
	}
	
	@Override
	public String getSubject()
	{
		return "Top [SQL Captured] - " + _reportType + " - SLOW Normalized SQL Statements (order by: " + _orderByCol_noBrackets + ", origin: monSysStatement) [with gt: execTime="+_statement_gt_execTime+", logicalReads="+_statement_gt_logicalReads+", physicalReads="+_statement_gt_physicalReads+"]";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}

	/**
	 * Set descriptions for the table, and the columns
	 */
	private void setSectionDescription(ResultSetTableModel rstm)
	{
		if (rstm == null)
			return;
		
		// Section description
		rstm.setDescription(
				"Top [SQL Captured] - " + _reportType + "- Slow Normalized SQL Statements are presented here (ordered by: " + _orderByCol_noBrackets + ") <br>" +
				"How we <i>normalize</i> SQL Text: " +
				"<ul>" +
				"  <li>When we cross the threshold to <i>Capture SQL</i></li>" +
				"  <li>The SQLText is parsed.</li>" +
				"  <li>All constants in the where clause is replaced with a question mark (?)</li>" +
				"  <li>All IN(list of values) are replaced with a elipse. (...)</li>" +
				"  <li>At the end: lets grab the <i>Java Hash Code</i> of the normalized SQL Text, which is stored in 'NormJavaSqlHashCode'</li>" +
				"  <li>If the SQL Text can't be parsed or the SQL Text is missing, then the value of 'NormJavaSqlHashCode' is set to -1</li>" +
				"</ul>" +
				"NormJavaSqlHashCode = -3 -- NO SQLText was found... but a *ss or *sq (*sq=Statement Cache, *sq=Dynamic SQL) was found <br>" +
				"NormJavaSqlHashCode = -2 -- Not Able to Parse/Normalize SQL Text <br>" +
				"NormJavaSqlHashCode = -1 -- NO SQLText was found. <br>" +
				"<br>" +
				"Thresholds: with GreaterThan: execTime="+_statement_gt_execTime+", logicalReads="+_statement_gt_logicalReads+", physicalReads="+_statement_gt_physicalReads+"<br>" +
				"Thresholds: having " + _orderByCol_noBrackets +" &gt;= 1000 <br>" +
				"<br>" +
				"ASE Source table is 'master.dbo.monSysStatement', which is a <i>ring buffer</i>, if the buffer is small, then we will be missing entries. <br>" +
				"PCS Source table is 'MonSqlCapStatements'. (PCS = Persistent Counter Store) <br>" +
				"<br>" +
				"The report <i>summarizes</i> (min/max/count/sum/avg) all entries/samples from the <i>MonSqlCapStatements</i> table grouped by 'NormJavaSqlHashCode'. <br>" +
				"Typically the column name <i>prefix</i> will tell you what aggregate function was used. <br>" +
				"SQL Text will be displayed in a separate table below the <i>summary</i> table.<br>" +
				"");

		// Columns description
		rstm.setColumnDescription("NormJavaSqlHashCode"        , "A Java calculation NormSQLText.hashCode() for the Normalized SQL Text (Note: this might not bee 100% accurate, but it's something... Note: -1 = Means that The NormSQLText couldn't be created/parsed/saved to the PCS.)");
		rstm.setColumnDescription("ExecCount"                  , "Number of entries for this 'NormJavaSqlHashCode' in the report period");
		rstm.setColumnDescription("ExecCount__chart"           , "A Mini Chart of when the executions was happening (grouped in 10 minute spans)");
		rstm.setColumnDescription("SkipThis"                   , "Send a 'skip' request to DbxCentral, this means that in future reports this entry will be excluded.");
                                                              
		rstm.setColumnDescription("StartTime_min"              , "First entry was sampled for this NormJavaSqlHashCode");
		rstm.setColumnDescription("EndTime_max"                , "Last entry was sampled for this NormJavaSqlHashCode");
		rstm.setColumnDescription("Duration"                   , "Start/end time presented as HH:MM:SS, so we can see if this NormJavaSqlHashCode is just for a short time or if it spans over a long period of time.");
                                                              
		rstm.setColumnDescription("Elapsed_ms__avg"            , "Average Time it took to execute this Statement during the report period (Elapsed_ms__sum/ExecCount)   ");
		rstm.setColumnDescription("CpuTime__avg"               , "Average CpuTime this Statement used            during the report period (CpuTime__sum/ExecCount)      ");
		rstm.setColumnDescription("WaitTime__avg"              , "Average avgWaitTime this Statement waited      during the report period (WaitTime__sum/ExecCount)     ");
		rstm.setColumnDescription("MemUsageKB__avg"            , "Average MemUsageKB this Statement used         during the report period (MemUsageKB__sum/ExecCount)   ");
		rstm.setColumnDescription("PhysicalReads__avg"         , "Average PhysicalReads this Statement used      during the report period (PhysicalReads__sum/ExecCount)");
		rstm.setColumnDescription("LogicalReads__avg"          , "Average LogicalReads this Statement used       during the report period (LogicalReads__sum/ExecCount) ");
		rstm.setColumnDescription("RowsAffected__avg"          , "Average RowsAffected this Statement did        during the report period (RowsAffected__sum/ExecCount) ");
                                                              
		rstm.setColumnDescription("Elapsed_ms__sum"            , "How many milliseconds did we spend in execution during the report period");
		rstm.setColumnDescription("CpuTime__sum"               , "How much CPUTime did we use during the report period");
		rstm.setColumnDescription("WaitTime__sum"              , "How much WaitTime did we use during the report period");
		rstm.setColumnDescription("MemUsageKB__sum"            , "How much MemUsageKB did we use during the report period");
		rstm.setColumnDescription("PhysicalReads__sum"         , "How much PhysicalReads did we use during the report period");
		rstm.setColumnDescription("LogicalReads__sum"          , "How much LogicalReads did we use during the report period");
		rstm.setColumnDescription("RowsAffected__sum"          , "How many RowsAffected did did this Statement do during the report period");

		rstm.setColumnDescription("LogicalReadsPerRowsAffected", "How Many LogicalReads per RowsAffected did this Statement do during the report period (Algorithm: sumLogicalReads/sumRowsAffected)");
	}

	@Override
	public List<ReportingIndexEntry> getReportingIndexes()
	{
		List<ReportingIndexEntry> list = new ArrayList<>();
		
		list.add(new ReportingIndexEntry("MonSqlCapStatements", "SPID", "KPID", "BatchID"));

		list.add(new ReportingIndexEntry("CmObjectActivity_diff", "DBName", "ObjectName", "IndexName"));
		list.add(new ReportingIndexEntry("CmObjectActivity_abs" , "DBName", "ObjectName", "IndexName", "SessionSampleTime"));

		return list;
	}

	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "MonSqlCapStatements" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		// Set: _statement_gt_* variables
		getSlowQueryThresholds(conn);
		
		// Get ASE Page Size
		int asePageSize = -1;
		try	{ asePageSize = getAsePageSizeFromMonDdlStorage(conn); }
		catch (SQLException ex) { }
		int asePageSizeDivider = 1024 * 1024 / asePageSize; // 2k->512, 4k->256, 8k=128, 16k=64

		int topRows       = getTopRows();
		int havingSumTime = 1000; // 1 second
		int dsrSkipCount  = getDsrSkipCount();
		
		String sql = "-- source table: MonSqlCapStatements \n"
			    + "select top " + (topRows + dsrSkipCount) + " \n"
//			    + "    [NormJavaSqlHashCode] \n"
	    	    + "    CASE WHEN [NormJavaSqlHashCode] = -1 AND [HashKey]          =  0 THEN -3 -- NO SQLText... but a *ss or *sq \n"
	    	    + "         WHEN [NormJavaSqlHashCode] = -1 AND [JavaSqlHashCode] != -1 THEN -2 -- Not Able to Parse/Normalize SQLText \n"
	    	    + "         WHEN [NormJavaSqlHashCode] = -1 AND [JavaSqlHashCode]  = -1 THEN -1 -- NO SQLText \n"
	    	    + "         ELSE [NormJavaSqlHashCode] \n"
	    	    + "    END as [NormJavaSqlHashCode] \n"
			    + " \n"
			    + "   ,cast('' as varchar(512))     as [SkipThis] \n"
			    + " \n"
			    + "   ,cast('' as varchar(10))      as [txt] \n"
			    + "   ,cast('' as varchar(10))      as [wait] \n"
			    + " \n"
			    + "   ,cast('' as varchar(512))     as [ExecCount__chart] \n"
			    + "   ,count(*)                     as [ExecCount] \n"
			    + " \n"
			    + "   ,cast('' as varchar(512))     as [Elapsed_ms__chart] \n"
			    + "   ,sum([Elapsed_ms])            as [Elapsed_ms__sum] \n"
			    + "   ,avg([Elapsed_ms])            as [Elapsed_ms__avg] \n"
			    
			    + "   ,cast('' as varchar(512))     as [CpuTime__chart] \n"
			    + "   ,sum([CpuTime])               as [CpuTime__sum] \n"
			    + "   ,avg([CpuTime])               as [CpuTime__avg] \n"
			    
			    + "   ,cast('' as varchar(512))     as [WaitTime__chart] \n"
			    + "   ,sum([WaitTime])              as [WaitTime__sum] \n"
			    + "   ,avg([WaitTime])              as [WaitTime__avg] \n"
			    
			    + "   ,cast('' as varchar(512))     as [MemUsageKB__chart] \n"
			    + "   ,sum([MemUsageKB])            as [MemUsageKB__sum] \n"
			    + "   ,avg([MemUsageKB])            as [MemUsageKB__avg] \n"
			    
			    + "   ,cast('' as varchar(512))     as [PhysicalReads__chart] \n"
			    + "   ,sum([PhysicalReads])         as [PhysicalReads__sum] \n"
			    + "   ,avg([PhysicalReads])         as [PhysicalReads__avg] \n"
			    
			    + "   ,cast('' as varchar(512))     as [LogicalReads__chart] \n"
			    + "   ,sum([LogicalReads])          as [LogicalReads__sum] \n"
			    + "   ,avg([LogicalReads])          as [LogicalReads__avg] \n"
			    
			    + "   ,cast('' as varchar(512))     as [LogicalReadsMb__chart] \n"
			    + "   ,sum([LogicalReads]) / "+asePageSizeDivider+" as [LogicalReadsMb__sum] \n"
			    + "   ,avg([LogicalReads]) * 1.0 / "+asePageSizeDivider+" as [LogicalReadsMb__avg] \n"
			    
			    + "   ,cast('' as varchar(512))     as [RowsAffected__chart] \n"
			    + "   ,sum([RowsAffected])          as [RowsAffected__sum] \n"
			    + "   ,avg([RowsAffected])          as [RowsAffected__avg] \n"
			    
			    + "   ,cast('' as varchar(512))     as [LogicalReadsPerRowsAffected__chart] \n"
				+ "   ,sum([LogicalReads]*1.0) / nullif(sum([RowsAffected]),0) as [LogicalReadsPerRowsAffected__avg] \n"
			    
//			    + "   ,sum([QueryOptimizationTime]) as [QueryOptimizationTime__sum] \n"
//			    + "   ,avg([QueryOptimizationTime]) as [QueryOptimizationTime__avg] \n"

//			    + "   ,sum([PagesModified])         as [PagesModified__sum] \n"
//			    + "   ,avg([PagesModified])         as [PagesModified__avg] \n"

//			    + "   ,sum([PacketsSent])           as [PacketsSent__sum] \n"
//			    + "   ,avg([PacketsSent])           as [PacketsSent__avg] \n"

//			    + "   ,sum([PacketsReceived])       as [PacketsReceived__sum] \n"
//			    + "   ,avg([PacketsReceived])       as [PacketsReceived__avg] \n"
			    + " \n"

//				+ "   ,(sum([LogicalReads])*1.0) / nullif(sum([RowsAffected]),0) as [LogicalReadsPerRowsAffected] \n"
//				+ "   ,-9999999.0 as [LogicalReadsPerRowsAffected] \n"

			    + "	  ,min([StartTime])             as [StartTime_min] \n"
			    + "	  ,max([EndTime])               as [EndTime_max] \n"
			    + "	  ,cast('' as varchar(30))      as [Duration] \n"
				
			    + "from [MonSqlCapStatements] \n"
			    + "where 1 = 1 \n"
			    + "  and [ErrorStatus] = 0 \n"
				+ getReportPeriodSqlWhere("StartTime")
			    + "group by [NormJavaSqlHashCode] \n"
			    + "having "   + _sqlHavingCol  + " >= " + havingSumTime + " \n"
			    + "order by " + _sqlOrderByCol + " desc \n"
			    + "";
		
		_shortRstm = executeQuery(conn, sql, false, "Top SQL");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("Top SQL");
			return;
		}
		else
		{
			// Highlight sort column
			_shortRstm.setHighlightSortColumns(_orderByCol_noBrackets);

			// Describe the table
			setSectionDescription(_shortRstm);

			// set duration
			setDurationColumn(_shortRstm, "StartTime_min", "EndTime_max", "Duration");

			// Fill in a column with a "skip link" to DbxCentral
			setSkipEntriesUrl(_shortRstm, "SkipThis", "NormJavaSqlHashCode", null);
						
//			// Do some calculations (which was hard to do in a PORTABLE SQL Way)
//			int pos_sumLogicalReads             = _shortRstm.findColumn("LogicalReads__sum");
//			int pos_sumRowsAffected             = _shortRstm.findColumn("RowsAffected__sum");
//			int pos_LogicalReadsPerRowsAffected = _shortRstm.findColumn("LogicalReadsPerRowsAffected");
//
//			if (pos_sumLogicalReads >= 0 && pos_sumRowsAffected >= 0 && pos_LogicalReadsPerRowsAffected >= 0)
//			{
//				for (int r=0; r<_shortRstm.getRowCount(); r++)
//				{
//					//------------------------------------------------
//					// set "LogicalReadsPerRowsAffected"
//					long sumLogicalReads = _shortRstm.getValueAsLong(r, pos_sumLogicalReads);
//					long sumRowsAffected = _shortRstm.getValueAsLong(r, pos_sumRowsAffected);
//
//					BigDecimal calc = new BigDecimal(-1);
//					if (sumRowsAffected > 0)
//						calc = new BigDecimal( (sumLogicalReads*1.0) / (sumRowsAffected*1.0) ).setScale(2, RoundingMode.HALF_EVEN);
//					
//					_shortRstm.setValueAtWithOverride(calc, r, pos_LogicalReadsPerRowsAffected);
//				}
//			}


			// Remove anything from the *SKIP* entries
			_skippedDsrRows = removeSkippedEntries(_shortRstm, topRows, getDsrSkipEntries());

			//----------------------------------------
			// Spark lines -- mini charts
			//----------------------------------------

			// Get data for: SparkLine - small chart values ... this will do:
			//  -- fill in the data cell with: <span class='aClassName' values='v1, v2, v2, v3...'>Mini Chart Here</span>
			//  -- return JavaScript Code to initialize the Spark line
			String whereKeyColumn = "NormJavaSqlHashCode"; 

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("ExecCount__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("MonSqlCapStatements")
					.setDbmsSampleTimeColumnName ("StartTime")
					.setDbmsDataValueColumnName  ("1") // not actually a column name, but will be used as: sum(1)   
					.setDbmsDataValueColumnNameIsExpression(true) // do NOT quotify the 'dbmsDataValueColumnName' 
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setSparklineTooltipPostfix  ("Number of execution in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("Elapsed_ms__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("MonSqlCapStatements")
					.setDbmsSampleTimeColumnName ("StartTime")
					.setDbmsDataValueColumnName  ("Elapsed_ms")   
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("CpuTime__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("MonSqlCapStatements")
					.setDbmsSampleTimeColumnName ("StartTime")
					.setDbmsDataValueColumnName  ("CpuTime")   
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("WaitTime__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("MonSqlCapStatements")
					.setDbmsSampleTimeColumnName ("StartTime")
					.setDbmsDataValueColumnName  ("WaitTime")   
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("MemUsageKB__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("MonSqlCapStatements")
					.setDbmsSampleTimeColumnName ("StartTime")
					.setDbmsDataValueColumnName  ("MemUsageKB")   
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("PhysicalReads__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("MonSqlCapStatements")
					.setDbmsSampleTimeColumnName ("StartTime")
					.setDbmsDataValueColumnName  ("PhysicalReads")   
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("LogicalReads__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("MonSqlCapStatements")
					.setDbmsSampleTimeColumnName ("StartTime")
					.setDbmsDataValueColumnName  ("LogicalReads")   
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("LogicalReadsMb__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("MonSqlCapStatements")
					.setDbmsSampleTimeColumnName ("StartTime")
//					.setDbmsDataValueColumnName  ("LogicalReads")   
					.setDbmsDataValueColumnName  ("sum([LogicalReads]) * 1.0 / " + asePageSizeDivider).setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("RowsAffected__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("MonSqlCapStatements")
					.setDbmsSampleTimeColumnName ("StartTime")
					.setDbmsDataValueColumnName  ("RowsAffected")   
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("LogicalReadsPerRowsAffected__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("MonSqlCapStatements")
					.setDbmsSampleTimeColumnName ("StartTime")
					.setDbmsDataValueColumnName  ("sum([LogicalReads]*1.0) / nullif(sum([RowsAffected]),0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			
			String sqlTextTable = "MonSqlCapStatements";
			if (DbUtils.checkIfTableExistsNoThrow(conn, null, null, "MonSqlCapSqlText"))
				sqlTextTable = "MonSqlCapSqlText";

			// Check if table "MonSqlCapStatements or MonSqlCapSqlText" has Dictionary Compressed Columns (any columns ends with "$dcc$")
			boolean hasDictCompCols = false;
			try {
				hasDictCompCols = DictCompression.hasCompressedColumnNames(conn, null, sqlTextTable);
			} catch (SQLException ex) {
				_logger.error("Problems checking for Dictionary Compressed Columns in table '" + sqlTextTable + "'.", ex);
			}

			//--------------------------------------------------------------------------------------
			// get Executed SQL Statements from the SQL Capture ...
			//--------------------------------------------------------------------------------------
			if (_shortRstm.getRowCount() > 0)
			{
				for (int r=0; r<_shortRstm.getRowCount(); r++)
				{
					// Get Actual Executed SQL Text for current 'NormJavaSqlHashCode'
					Map<String, Object> whereColValMap = new LinkedHashMap<>();
					whereColValMap.put("NormJavaSqlHashCode", _shortRstm.getValueAsInteger(r, "NormJavaSqlHashCode"));

					_keyToExecutedSql = getSqlCapExecutedSqlText(_keyToExecutedSql, conn, hasDictCompCols, whereColValMap);
				}
			}

			//--------------------------------------------------------------------------------------
			// get WHAT were we waiting on
			// This is a bit "iffy" ... meaning we get the info from [CmSpidWait_diff], which is what the SPID's where doing
			// it might be OFF, but lets test it...
			// We can also get information from [CmActiveStatements_diff] to get what OTHERS was doing (for example the "source" SQL if we are getting blocked)
			//--------------------------------------------------------------------------------------
			if (_shortRstm.getRowCount() > 0)
			{
				for (int r=0; r<_shortRstm.getRowCount(); r++)
				{
					// Get 'WitTimes' current 'NormJavaSqlHashCode'
					Map<String, Object> whereColValMap = new LinkedHashMap<>();
					whereColValMap.put("NormJavaSqlHashCode", _shortRstm.getValueAsInteger(r, "NormJavaSqlHashCode"));

					_keyToWaitTime = getSqlCapWaitTime(_keyToWaitTime, conn, hasDictCompCols, whereColValMap);
				}
			}

			//----------------------------------------------------
			// Create a SQL-Details ResultSet based on values in _shortRstm
			//----------------------------------------------------
			if (_shortRstm.getRowCount() > 0)
			{
				SimpleResultSet srs = new SimpleResultSet();

				srs.addColumn("NormJavaSqlHashCode", Types.VARCHAR,       60, 0);
				srs.addColumn("Skip This"          , Types.VARCHAR,       60, 0);
				srs.addColumn("Sparklines"         , Types.VARCHAR,      512, 0); 
				srs.addColumn("txt"                , Types.VARCHAR,       60, 0);
				srs.addColumn("wait"               , Types.VARCHAR,       60, 0);
				srs.addColumn("NormSQLText"        , Types.VARCHAR, 1024*128, 0); // this is 'text' in the origin table

				// Position in the "source" _shortRstm table (values we will fetch)
				int pos_NormJavaSqlHashCode = _shortRstm.findColumn("NormJavaSqlHashCode");
				int pos_skipThis            = _shortRstm.findColumn("SkipThis");
				int pos_txt                 = _shortRstm.findColumn("txt");
				int pos_wait                = _shortRstm.findColumn("wait");


				ColumnCopyRender msToHMS    = HtmlTableProducer.MS_TO_HMS;
				ColumnCopyRender oneDecimal = HtmlTableProducer.ONE_DECIMAL;
				
				HtmlTableProducer htp = new HtmlTableProducer(_shortRstm, "dsr-sub-table-chart");
				htp.setTableHeaders("Charts at 10 minute interval", "Total;style='text-align:right!important'", "Avg per exec;style='text-align:right!important'", "");
				htp.add("exec-cnt"  , new ColumnCopyRow().add( new ColumnCopyDef("ExecCount__chart"                   ) ).add(new ColumnCopyDef("ExecCount").setColBold())   .addEmptyCol()                                                        .addEmptyCol() );
				htp.add("exec-time" , new ColumnCopyRow().add( new ColumnCopyDef("Elapsed_ms__chart"                  ) ).add(new ColumnCopyDef("Elapsed_ms__sum", msToHMS) ).add(new ColumnCopyDef("Elapsed_ms__avg"                 , oneDecimal).setColBold()).add(new ColumnStatic("ms" )) );
				htp.add("cpu-time"  , new ColumnCopyRow().add( new ColumnCopyDef("CpuTime__chart"                     ) ).add(new ColumnCopyDef("CpuTime__sum"   , msToHMS) ).add(new ColumnCopyDef("CpuTime__avg"                    , oneDecimal).setColBold()).add(new ColumnStatic("ms" )) );
				htp.add("wait-time" , new ColumnCopyRow().add( new ColumnCopyDef("WaitTime__chart"                    ) ).add(new ColumnCopyDef("WaitTime__sum"  , msToHMS) ).add(new ColumnCopyDef("WaitTime__avg"                   , oneDecimal).setColBold()).add(new ColumnStatic("ms" )) );
				htp.add("l-read"    , new ColumnCopyRow().add( new ColumnCopyDef("LogicalReads__chart"                ) ).add(new ColumnCopyDef("LogicalReads__sum"       ) ).add(new ColumnCopyDef("LogicalReads__avg"               , oneDecimal).setColBold()).add(new ColumnStatic("pgs")) );
				htp.add("l-read-mb" , new ColumnCopyRow().add( new ColumnCopyDef("LogicalReadsMb__chart"              ) ).add(new ColumnCopyDef("LogicalReadsMb__sum"     ) ).add(new ColumnCopyDef("LogicalReadsMb__avg"             , oneDecimal).setColBold()).add(new ColumnStatic("mb" )) );
				htp.add("p-read"    , new ColumnCopyRow().add( new ColumnCopyDef("PhysicalReads__chart"               ) ).add(new ColumnCopyDef("PhysicalReads__sum"      ) ).add(new ColumnCopyDef("PhysicalReads__avg"              , oneDecimal).setColBold()).add(new ColumnStatic("pgs")) );
				htp.add("rowcount"  , new ColumnCopyRow().add( new ColumnCopyDef("RowsAffected__chart"                ) ).add(new ColumnCopyDef("RowsAffected__sum"       ) ).add(new ColumnCopyDef("RowsAffected__avg"               , oneDecimal).setColBold()).add(new ColumnStatic("#"  )) );
				htp.add("l-read/row", new ColumnCopyRow().add( new ColumnCopyDef("LogicalReadsPerRowsAffected__chart" ) ).add(new ColumnStatic ("n/a").setColAlign("right") ).add(new ColumnCopyDef("LogicalReadsPerRowsAffected__avg", oneDecimal).setColBold()).add(new ColumnStatic("pgs")) );
				htp.add("mem-use"   , new ColumnCopyRow().add( new ColumnCopyDef("MemUsageKB__chart"                  ) ).add(new ColumnCopyDef("MemUsageKB__sum"         ) ).add(new ColumnCopyDef("MemUsageKB__avg"                 , oneDecimal).setColBold()).add(new ColumnStatic("kb" )) );
				htp.validate();

				// add rows to Simple ResultSet
				if (pos_NormJavaSqlHashCode >= 0)
				{
					for (int r=0; r<_shortRstm.getRowCount(); r++)
					{
						String NormJavaSqlHashCode = _shortRstm.getValueAsString(r, pos_NormJavaSqlHashCode);
						String skipThis            = _shortRstm.getValueAsString(r, pos_skipThis);
						String txt                 = _shortRstm.getValueAsString(r, pos_txt);
						String wait                = _shortRstm.getValueAsString(r, pos_wait);
						String sqlText             = "--not-found--";

						// get the SQL Text
						if (true)
						{
							String col_NormSQLText = "[NormSQLText]";
							if (hasDictCompCols)
								col_NormSQLText = DictCompression.getRewriteForColumnName(sqlTextTable, "NormSQLText$dcc$", null).replace(" AS [NormSQLText]", "");

							sql = ""
								    + "select " + col_NormSQLText + " as [NormSQLText] \n"
								    + "from [" + sqlTextTable + "] \n"
								    + "where [NormJavaSqlHashCode] = " + NormJavaSqlHashCode + " \n"
								    + "";
							
							sql = conn.quotifySqlString(sql);
							try ( Statement stmnt = conn.createStatement() )
							{
								// Unlimited execution time
								stmnt.setQueryTimeout(0);
								try ( ResultSet rs = stmnt.executeQuery(sql) )
								{
									while(rs.next())
										sqlText = rs.getString(1);
								}
							}
							catch(SQLException ex)
							{
								setProblemException(ex);
	
								_logger.warn("Problems getting SQL by NormJavaSqlHashCode = "+NormJavaSqlHashCode+": " + ex + ". SQL=|" + sql + "|.");
							} 
						}
						
						// Parse the 'sqlText' and extract Table Names, then get various table and index information
						String tableInfo = getDbmsTableInformationFromSqlText(conn, null, sqlText, DbUtils.DB_PROD_NAME_SYBASE_ASE);

//						// Parse the 'sqlText' and extract Table Names..
//						// - then get table information (like we do in 'AseTopCmObjectActivity')
//						String tableInfo = "";
//						boolean parseSqlText = true;
//						if (parseSqlText)
//						{
//							// Parse the SQL Text to get all tables that are used in the Statement
//							String problemDesc = "";
//							Set<String> tableList = SqlParserUtils.getTables(sqlText);
////							List<String> tableList = Collections.emptyList();
////							try { tableList = SqlParserUtils.getTables(sqlText, true); }
////							catch (ParseException pex) { problemDesc = pex + ""; }
//
//							// Get information about ALL tables in list 'tableList' from the DDL Storage
//							Set<AseTableInfo> tableInfoSet = getTableInformationFromMonDdlStorage(conn, tableList);
//							if (tableInfoSet.isEmpty() && StringUtil.isNullOrBlank(problemDesc))
//								problemDesc = "&emsp; &bull; No tables was found in the DDL Storage for tables: " + listToHtmlCode(tableList);
//
//							// And make it into a HTML table with various information about the table and indexes 
//							tableInfo = problemDesc + getTableInfoAsHtmlTable(tableInfoSet, tableList, true, "dsr-sub-table-tableinfo");
//
//							// Finally make up a message that will be appended to the SQL Text
//							if (StringUtil.hasValue(tableInfo))
//							{
//								// Surround with collapse div
//								tableInfo = ""
//										//+ "<!--[if !mso]><!--> \n" // BEGIN: IGNORE THIS SECTION FOR OUTLOOK
//
//										+ "\n<br>\n<br>\n"
//										+ "<details open> \n"
//										+ "<summary>Show/Hide Table information for " + tableList.size() + " table(s): " + listToHtmlCode(tableList) + "</summary> \n"
//										+ tableInfo
//										+ "</details> \n"
//
//										//+ "<!--<![endif]-->    \n" // END: IGNORE THIS SECTION FOR OUTLOOK
//										+ "";
//							}
//						}
						
						// Grab all SparkLines we defined in 'subTableRowSpec'
						String sparklines = htp.getHtmlTextForRow(r);

						sqlText = "<xmp>" + sqlText + "</xmp>" + tableInfo;
						
						//-------------------------------------
						// add record to SimpleResultSet
						srs.addRow(NormJavaSqlHashCode, skipThis, sparklines, txt, wait, sqlText);
					}
				}

				// GET SQLTEXT (only)
				try
				{
					// Note the 'srs' is populated when reading above ResultSet from query
					_sqlRstm = createResultSetTableModel(srs, "Top SQL TEXT", null, false); // DO NOT TRUNCATE COLUMNS
					srs.close();
				}
				catch (SQLException ex)
				{
					setProblemException(ex);
		
					_sqlRstm = ResultSetTableModel.createEmpty("Top SQL TEXT");
					_logger.warn("Problems getting Top SQL TEXT: " + ex);
				}
			}

//			if (_shortRstm.getRowCount() > 0)
//			{
//				// For each record... try to get the SQL Text based on the NormJavaSqlHashCode
//				int pos_NormJavaSqlHashCode = _shortRstm.findColumn("NormJavaSqlHashCode");
//				if (pos_NormJavaSqlHashCode != -1)
//				{
//					for (int r=0; r<_shortRstm.getRowCount(); r++)
//					{
//						int NormJavaSqlHashCode = _shortRstm.getValueAsInteger(r, pos_NormJavaSqlHashCode);
//						
//						if (NormJavaSqlHashCode != -1)
//						{
//							String col_NormSQLText = "[NormSQLText]";
//							if (hasDictCompCols)
//								col_NormSQLText = DictCompression.getRewriteForColumnName(sqlTextTable, "NormSQLText$dcc$", null).replace(" AS [NormSQLText]", "");
//
//							sql = ""
//								    + "select distinct \n"
//								    + "    [NormJavaSqlHashCode] \n"
//								    + "   ,cast('' as varchar(512))  as [SkipThis] \n"
//								    + "   ,cast('' as varchar(512))  as [CpuTime] \n"
//								    + "   ,cast('' as varchar(10))   as [txt] \n"
//								    + "   ,cast('' as varchar(10))   as [wait] \n"
//								    + "   ,cast(-1 as int        )   as [calls] \n"
//								    + "   ,cast(-1 as decimal(16,1)) as [avg_time] \n"
//								    + "   ," + col_NormSQLText + "   as [NormSQLText] \n"
//								    + "from [" + sqlTextTable + "] \n"
//								    + "where [NormJavaSqlHashCode] = " + NormJavaSqlHashCode + " \n"
//								    + "";
//							
//							sql = conn.quotifySqlString(sql);
//							try ( Statement stmnt = conn.createStatement() )
//							{
//								// Unlimited execution time
//								stmnt.setQueryTimeout(0);
//								try ( ResultSet rs = stmnt.executeQuery(sql) )
//								{
////									ResultSetTableModel rstm = new ResultSetTableModel(rs, "SqlDetails");
//									ResultSetTableModel rstm = createResultSetTableModel(rs, "SqlDetails", sql);
//									
//									if (_sqlRstm == null)
//										_sqlRstm = rstm;
//									else
//										_sqlRstm.add(rstm);
//	
//									// Fill in a column with a "skip link" to DbxCentral
//									setSkipEntriesUrl(_sqlRstm, "SkipThis", "NormJavaSqlHashCode", "NormSQLText");
//									
//									if (_logger.isDebugEnabled())
//										_logger.debug("SqlDetails.getRowCount()="+ rstm.getRowCount());
//								}
//							}
//							catch(SQLException ex)
//							{
//								setProblemException(ex);
//	
//								_logger.warn("Problems getting SQL by NormJavaSqlHashCode = "+NormJavaSqlHashCode+": " + ex);
//							} 
//							catch(ModelMissmatchException ex)
//							{
//								setProblemException(ex);
//	
//								_logger.warn("Problems (merging into previous ResultSetTableModel) when getting SQL by NormJavaSqlHashCode = "+NormJavaSqlHashCode+": " + ex);
//							} 
//						}
//						
//					} // end: _shortRstm row loop
//
//					// Mini Chart on "CPU Time"
//					// COPY Cell data from the "details" table
//					_sqlRstm.copyCellContentFrom(_shortRstm, "NormJavaSqlHashCode", "CpuTime__chart" , "NormJavaSqlHashCode", "CpuTime");
//					_sqlRstm.copyCellContentFrom(_shortRstm, "NormJavaSqlHashCode", "ExecCount"      , "NormJavaSqlHashCode", "calls");
//					_sqlRstm.copyCellContentFrom(_shortRstm, "NormJavaSqlHashCode", "Elapsed_ms__avg", "NormJavaSqlHashCode", "avg_time");
//				}
//			}
		} // end: _shortRstm has data
		
	} // end: method
	
} // end: class
