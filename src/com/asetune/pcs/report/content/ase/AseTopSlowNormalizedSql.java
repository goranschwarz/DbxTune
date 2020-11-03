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
package com.asetune.pcs.report.content.ase;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import com.asetune.gui.ModelMissmatchException;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.gui.ResultSetTableModel.TableStringRenderer;
import com.asetune.pcs.DictCompression;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class AseTopSlowNormalizedSql extends AseAbstract
{
	private static Logger _logger = Logger.getLogger(AseTopSlowNormalizedSql.class);

	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _longRstm;
//	private Exception           _problem = null;
	private ResultSetTableModel _skippedDsrRows;
	
	public AseTopSlowNormalizedSql(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public void writeMessageText(Writer sb)
	throws IOException
	{
		if (_shortRstm.getRowCount() == 0)
		{
			sb.append("No rows found <br>\n");
		}
		else
		{
			// Get a description of this section, and column names
			sb.append(getSectionDescriptionHtml(_shortRstm, true));

			sb.append(createSkippedEntriesReport(_skippedDsrRows));

			sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
//			sb.append(_shortRstm.toHtmlTableString("sortable"));
			sb.append(toHtmlTable(_shortRstm));

			if (_longRstm != null)
			{
				sb.append("<br>\n");
				sb.append("SQL Text by NormJavaSqlHashCode: " + _longRstm.getRowCount() + "<br>\n");
				sb.append("Tip: To format the SQL text below you can use any online formatting tool, like <a href='https://poorsql.com/'>Poor Man's T-SQL Formatter</a><br>\n");
//				sb.append(_longRstm.toHtmlTablesVerticalString("sortable"));
//				sb.append(_longRstm.toHtmlTableString("sortable"));

				// Create a default renderer
				TableStringRenderer tableRender = new ResultSetTableModel.TableStringRenderer()
				{
					@Override
					public String cellValue(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
					{
						if ("NormSQLText".equals(colName))
						{
						//	return "<xmp style='width:100%; max-height:400px; overflow:auto'>" + strVal + "</xmp>";
							return "<xmp>" + strVal + "</xmp>";
						}
						return strVal;
					}
				};
				sb.append(_longRstm.toHtmlTableString("sortable", true, true, null, tableRender));
				
//				// Surround the column 'NormSQLText' content with '<xmp>' content '</xmp>'
//				Map<String, String> colNameValueTagMap = new HashMap<>();
//				colNameValueTagMap.put("NormSQLText",   "xmp");
//
//				sb.append(toHtmlTable(_longRstm, colNameValueTagMap));
			}
		}
	}
//	@Override
//	public String getMessageText()
//	{
//		StringBuilder sb = new StringBuilder();
//
//		if (_shortRstm.getRowCount() == 0)
//		{
//			sb.append("No rows found <br>\n");
//		}
//		else
//		{
//			// Get a description of this section, and column names
//			sb.append(getSectionDescriptionHtml(_shortRstm, true));
//
//			sb.append(createSkippedEntriesReport(_skippedDsrRows));
//
//			sb.append("Row Count: ").append(_shortRstm.getRowCount()).append("<br>\n");
////			sb.append(_shortRstm.toHtmlTableString("sortable"));
//			sb.append(toHtmlTable(_shortRstm));
//
//			if (_longRstm != null)
//			{
//				sb.append("<br>\n");
//				sb.append("SQL Text by NormJavaSqlHashCode: ").append(_longRstm.getRowCount()).append("<br>\n");
//				sb.append("Tip: To format the SQL text below you can use any online formatting tool, like <a href='https://poorsql.com/'>Poor Man's T-SQL Formatter</a><br>\n");
////				sb.append(_longRstm.toHtmlTablesVerticalString("sortable"));
////				sb.append(_longRstm.toHtmlTableString("sortable"));
//
//				// Create a default renderer
//				TableStringRenderer tableRender = new ResultSetTableModel.TableStringRenderer()
//				{
//					@Override
//					public String cellValue(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
//					{
//						if ("NormSQLText".equals(colName))
//						{
//						//	return "<xmp style='width:100%; max-height:400px; overflow:auto'>" + strVal + "</xmp>";
//							return "<xmp>" + strVal + "</xmp>";
//						}
//						return strVal;
//					}
//				};
//				sb.append(_longRstm.toHtmlTableString("sortable", true, true, null, tableRender));
//				
////				// Surround the column 'NormSQLText' content with '<xmp>' content '</xmp>'
////				Map<String, String> colNameValueTagMap = new HashMap<>();
////				colNameValueTagMap.put("NormSQLText",   "xmp");
////
////				sb.append(toHtmlTable(_longRstm, colNameValueTagMap));
//			}
//		}
//
//		return sb.toString();
//	}

	@Override
	public String getSubject()
	{
		return "Top [SQL Captured] SLOW Normalized SQL Statements (order by: sumCpuTime, origin: monSysStatement) [with gt: execTime="+_statement_gt_execTime+", logicalReads="+_statement_gt_logicalReads+", physicalReads="+_statement_gt_physicalReads+"]";
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
				"Top [SQL Captured] Slow Normalized SQL Statements are presented here (ordered by: sumCpuTime) <br>" +
				"How we <i>normalize</i> SQL Text: " +
				"<ul>" +
				"  <li>When we cross the threshold to <i>Capture SQL</i></li>" +
				"  <li>The SQLText is parsed.</li>" +
				"  <li>All constants in the where clause is replaced with a question mark (?)</li>" +
				"  <li>All IN(list of values) are replaced with a elipse. (...)</li>" +
				"  <li>At the end: lets grab the <i>Java Hash Code</i> of the normalized SQL Text, which is stored in 'NormJavaSqlHashCode'</li>" +
				"  <li>If the SQL Text can't be parsed or the SQL Text is missing, then the value of 'NormJavaSqlHashCode' is set to -1</li>" +
				"</ul>" +
				"<br>" +
				"Thresholds: with GreaterThan: execTime="+_statement_gt_execTime+", logicalReads="+_statement_gt_logicalReads+", physicalReads="+_statement_gt_physicalReads+"<br>" +
				"Thresholds: having sumCpuTime &gt;= 1000 <br>" +
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
		rstm.setColumnDescription("records"                    , "Number of entries for this 'NormJavaSqlHashCode' in the report period");
		rstm.setColumnDescription("SkipThis"                   , "Send a 'skip' request to DbxCentral, this means that in future reports this entry will be excluded.");
                                                              
		rstm.setColumnDescription("StartTime_min"              , "First entry was sampled for this NormJavaSqlHashCode");
		rstm.setColumnDescription("EndTime_max"                , "Last entry was sampled for this NormJavaSqlHashCode");
		rstm.setColumnDescription("Duration"                   , "Start/end time presented as HH:MM:SS, so we can see if this NormJavaSqlHashCode is just for a short time or if it spans over a long period of time.");
                                                              
		rstm.setColumnDescription("avgElapsed_ms"              , "Average Time it took to execute this Statement during the report period (sumElapsed_ms/records)   ");
		rstm.setColumnDescription("avgCpuTime"                 , "Average CpuTime this Statement used            during the report period (sumCpuTime/records)      ");
		rstm.setColumnDescription("avgWaitTime"                , "Average avgWaitTime this Statement waited      during the report period (sumWaitTime/records)     ");
		rstm.setColumnDescription("avgMemUsageKB"              , "Average MemUsageKB this Statement used         during the report period (sumMemUsageKB/records)   ");
		rstm.setColumnDescription("avgPhysicalReads"           , "Average PhysicalReads this Statement used      during the report period (sumPhysicalReads/records)");
		rstm.setColumnDescription("avgLogicalReads"            , "Average LogicalReads this Statement used       during the report period (sumLogicalReads/records) ");
		rstm.setColumnDescription("avgRowsAffected"            , "Average RowsAffected this Statement did        during the report period (sumRowsAffected/records) ");
                                                              
		rstm.setColumnDescription("sumElapsed_ms"              , "How many milliseconds did we spend in execution during the report period");
		rstm.setColumnDescription("sumCpuTime"                 , "How much CPUTime did we use during the report period");
		rstm.setColumnDescription("sumWaitTime"                , "How much WaitTime did we use during the report period");
		rstm.setColumnDescription("sumMemUsageKB"              , "How much MemUsageKB did we use during the report period");
		rstm.setColumnDescription("sumPhysicalReads"           , "How much PhysicalReads did we use during the report period");
		rstm.setColumnDescription("sumLogicalReads"            , "How much LogicalReads did we use during the report period");
		rstm.setColumnDescription("sumRowsAffected"            , "How many RowsAffected did did this Statement do during the report period");

		rstm.setColumnDescription("LogicalReadsPerRowsAffected", "How Many LogicalReads per RowsAffected did this Statement do during the report period (Algorithm: sumLogicalReads/sumRowsAffected)");
	}

	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "MonSqlCapStatements" };
	}



///////////////////////////////////////////////////////////////////////////////////
	protected String getDsrSkipEntriesAsHtmlTable()
	{
		String srvName   = getReportingInstance().getDbmsServerName();
		String className = this.getClass().getSimpleName();
		
		return getReportingInstance().getDsrSkipEntriesAsHtmlTable(srvName, className);
	}

	protected Map<String, Set<String>> getDsrSkipEntries()
	{
		String srvName   = getReportingInstance().getDbmsServerName();
		String className = this.getClass().getSimpleName();
		
		_skipEntries = getReportingInstance().getDsrSkipEntries(srvName, className);
		return _skipEntries;
	}
	private Map<String, Set<String>> _skipEntries;

	protected int getDsrSkipCount()
	{
		if (_skipEntries == null)
			_skipEntries = getDsrSkipEntries();

		int cnt = 0;

		for (Set<String> entry : _skipEntries.values())
		{
			cnt += entry.size();
		}
		
		return cnt;
	}

	protected ResultSetTableModel removeSkippedEntries(ResultSetTableModel rstm, int topRows, Map<String, Set<String>> dsrSkipEntries)
	{
		ResultSetTableModel allRemovedRows = new ResultSetTableModel(rstm, rstm.getName() + "_skippedRows", false);
		
		for (Entry<String, Set<String>> entry : dsrSkipEntries.entrySet())
		{
			String             colName = entry.getKey();
			Collection<String> values  = entry.getValue();

			List<ArrayList<Object>> removedRows = rstm.removeRows(colName, values);

			//System.out.println("removeSkippedEntries(): removeCnt="+removedRows.size()+", colName='"+colName+"', values='"+values+"'.");
			//if (!removedRows.isEmpty())
			//	System.out.println("REMOVED-ROWS: "+removedRows);
			
			if ( ! removedRows.isEmpty() )
				allRemovedRows.addRows(removedRows);
		}
		
		// finally (make sure that we have no more rows than "TOP" was saying
		// these rows should NOT be part of the returned 'allRemovedRows'
		if (topRows > 0)
		{
			while(rstm.getRowCount() > topRows)
			{
				rstm.removeRow(rstm.getRowCount() - 1);
			}
		}
		
		return allRemovedRows;
	}

	protected String createSkippedEntriesReport(ResultSetTableModel skipRstm)
	{
		String dbxCentralAdminUrl = getReportingInstance().getDbxCentralBaseUrl() + "/admin/admin.html";

		if (skipRstm == null)
			return "--NO-SKIPPED-ROWS (null)--<br><br> \n";
		
		if (skipRstm.getRowCount() == 0)
		{
			return "No <i>Skip</i> records was found. Skip records can be defined in DbxCentral under <a href='" + dbxCentralAdminUrl + "' target='_blank'>" + dbxCentralAdminUrl + "</a> or in the report table by pressing <i>'Skip This'</i> links in the below tables.<br> \n";
		}
		
		// Remove column 'SkipThis' or change the header and content to 'Remove This From The Skip Set'
		if (true)
		{
			skipRstm.removeColumn("SkipThis");
		}
		
		// add the content as a "hidden" section that user needs to expand to see
		StringBuilder sb = new StringBuilder();
		
		sb.append("<b>Skip Values:</b> The Skip Set contained " + getDsrSkipCount() + " entries, Record that <b>was skipped</b> was: " + skipRstm.getRowCount() + "<br> \n");
		sb.append("Note: Skip Values can be defined in DbxCentral under <a href='" + dbxCentralAdminUrl + "' target='_blank'>" + dbxCentralAdminUrl + "</a> or in the report table by pressing <i>'Skip This'</i> links in the below tables.<br> \n");

		String skipEntriesHtmlTable    = "<b>Table of the Skip Set.</b> <br>" 
		                               + getDsrSkipEntriesAsHtmlTable();

		String removedRecordsHtmlTable = "<b>Here is a Table of the " + skipRstm.getRowCount() + " skipped records.</b> <br>" 
		                               + skipRstm.toHtmlTableString("sortable");

		String htmlText = skipEntriesHtmlTable + removedRecordsHtmlTable;
		
		String showHideDiv = createShowHideDiv("skip-section", false, "Show/Hide Skipped entries...", htmlText);
		sb.append( msOutlookAlternateText(showHideDiv, "Skipped Entries", "") );
		sb.append("<br>");

		return sb.toString();
	}
///////////////////////////////////////////////////////////////////////////////////


	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		// Set: _statement_gt_* variables
		getSlowQueryThresholds(conn);
		
		int topRows          = localConf.getIntProperty(this.getClass().getSimpleName()+".top", 20);
		int havingSumCpuTime = 1000; // 1 second

		int skipCount = getDsrSkipCount();
		
//		String sql = "-- source table: MonSqlCapStatements \n"
//			    + "select top " + (topRows + skipCount) + " \n"
//			    + "    [NormJavaSqlHashCode] \n"
//			    + "   ,count(*)                     as [records] \n"
//			    + " \n"
//			    + "   ,cast('' as varchar(512))     as [SkipThis] \n"
//			    + "	  ,min([StartTime])             as [StartTime_min] \n"
//			    + "	  ,max([EndTime])               as [EndTime_max] \n"
//			    + "	  ,cast('' as varchar(30))      as [Duration] \n"
//			    + " \n"
//			    + "   ,avg([Elapsed_ms])            as [avgElapsed_ms] \n"
//			    + "   ,avg([CpuTime])               as [avgCpuTime] \n"
//			    + "   ,avg([WaitTime])              as [avgWaitTime] \n"
//			    + "   ,avg([MemUsageKB])            as [avgMemUsageKB] \n"
//			    + "   ,avg([PhysicalReads])         as [avgPhysicalReads] \n"
//			    + "   ,avg([LogicalReads])          as [avgLogicalReads] \n"
//			    + "   ,avg([RowsAffected])          as [avgRowsAffected] \n"
////			    + "   ,avg([QueryOptimizationTime]) as [avgQueryOptimizationTime] \n"
////			    + "   ,avg([PagesModified])         as [avgPagesModified] \n"
////			    + "   ,avg([PacketsSent])           as [avgPacketsSent] \n"
////			    + "   ,avg([PacketsReceived])       as [avgPacketsReceived] \n"
//			    + " \n"
//			    + "   ,sum([Elapsed_ms])            as [sumElapsed_ms] \n"
//			    + "   ,sum([CpuTime])               as [sumCpuTime] \n"
//			    + "   ,sum([WaitTime])              as [sumWaitTime] \n"
//			    + "   ,sum([MemUsageKB])            as [sumMemUsageKB] \n"
//			    + "   ,sum([PhysicalReads])         as [sumPhysicalReads] \n"
//			    + "   ,sum([LogicalReads])          as [sumLogicalReads] \n"
//			    + "   ,sum([RowsAffected])          as [sumRowsAffected] \n"
////			    + "   ,sum([QueryOptimizationTime]) as [sumQueryOptimizationTime] \n"
////			    + "   ,sum([PagesModified])         as [sumPagesModified] \n"
////			    + "   ,sum([PacketsSent])           as [sumPacketsSent] \n"
////			    + "   ,sum([PacketsReceived])       as [sumPacketsReceived] \n"
//
////				+ "   ,(sum([LogicalReads])*1.0) / (coalesce(sum([RowsAffected]),1)*1.0) as [LogicalReadsPerRowsAffected] \n"
//				+ "   ,-9999999.0 as [LogicalReadsPerRowsAffected] \n"
//				
//			    + "from [MonSqlCapStatements] \n"
//			    + "where 1 = 1 \n"
////			    + "where [ProcName] is NULL \n"
////			    + "where [ProcedureID] = 0 \n"
//				+ getReportPeriodSqlWhere("StartTime")
//			    + "group by [NormJavaSqlHashCode] \n"
//			    + "having [sumCpuTime] >= " + havingSumCpuTime + " \n"
////			    + "order by [records] desc \n"
////			    + "order by [sumLogicalReads] desc \n"
//			    + "order by [sumCpuTime] desc \n"
//			    + "";

		String sql = "-- source table: MonSqlCapStatements \n"
			    + "select top " + (topRows + skipCount) + " \n"
			    + "    [NormJavaSqlHashCode] \n"
			    + "   ,count(*)                     as [records] \n"
			    + " \n"
			    + "   ,cast('' as varchar(512))     as [SkipThis] \n"
			    + "	  ,min([StartTime])             as [StartTime_min] \n"
			    + "	  ,max([EndTime])               as [EndTime_max] \n"
			    + "	  ,cast('' as varchar(30))      as [Duration] \n"
			    + " \n"
			    + "   ,sum([Elapsed_ms])            as [Elapsed_ms__sum] \n"
			    + "   ,avg([Elapsed_ms])            as [Elapsed_ms__avg] \n"
			    
			    + "   ,sum([CpuTime])               as [CpuTime__sum] \n"
			    + "   ,avg([CpuTime])               as [CpuTime__avg] \n"
			    
			    + "   ,sum([WaitTime])              as [WaitTime__sum] \n"
			    + "   ,avg([WaitTime])              as [WaitTime__avg] \n"
			    
			    + "   ,sum([MemUsageKB])            as [MemUsageKB__sum] \n"
			    + "   ,avg([MemUsageKB])            as [MemUsageKB__avg] \n"
			    
			    + "   ,sum([PhysicalReads])         as [PhysicalReads__sum] \n"
			    + "   ,avg([PhysicalReads])         as [PhysicalReads__avg] \n"
			    
			    + "   ,sum([LogicalReads])          as [LogicalReads__sum] \n"
			    + "   ,avg([LogicalReads])          as [LogicalReads__avg] \n"
			    
			    + "   ,sum([RowsAffected])          as [RowsAffected__sum] \n"
			    + "   ,avg([RowsAffected])          as [RowsAffected__avg] \n"
			    
//			    + "   ,sum([QueryOptimizationTime]) as [QueryOptimizationTime__sum] \n"
//			    + "   ,avg([QueryOptimizationTime]) as [QueryOptimizationTime__avg] \n"

//			    + "   ,sum([PagesModified])         as [PagesModified__sum] \n"
//			    + "   ,avg([PagesModified])         as [PagesModified__avg] \n"

//			    + "   ,sum([PacketsSent])           as [PacketsSent__sum] \n"
//			    + "   ,avg([PacketsSent])           as [PacketsSent__avg] \n"

//			    + "   ,sum([PacketsReceived])       as [PacketsReceived__sum] \n"
//			    + "   ,avg([PacketsReceived])       as [PacketsReceived__avg] \n"
			    + " \n"

//				+ "   ,(sum([LogicalReads])*1.0) / (coalesce(sum([RowsAffected]),1)*1.0) as [LogicalReadsPerRowsAffected] \n"
				+ "   ,-9999999.0 as [LogicalReadsPerRowsAffected] \n"
				
			    + "from [MonSqlCapStatements] \n"
			    + "where 1 = 1 \n"
//			    + "where [ProcName] is NULL \n"
//			    + "where [ProcedureID] = 0 \n"
				+ getReportPeriodSqlWhere("StartTime")
			    + "group by [NormJavaSqlHashCode] \n"
			    + "having [CpuTime__sum] >= " + havingSumCpuTime + " \n"
//			    + "order by [records] desc \n"
//			    + "order by [LogicalReads__sum] desc \n"
			    + "order by [CpuTime__sum] desc \n"
			    + "";
		
		_shortRstm = executeQuery(conn, sql, false, "Top SQL");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("Top SQL");
			return;
		}
		else
		{
			// Describe the table
			setSectionDescription(_shortRstm);

			// set duration
			setDurationColumn(_shortRstm, "StartTime_min", "EndTime_max", "Duration");

			// Fill in a column with a "skip link" to DbxCentral
			setSkipEntriesUrl(_shortRstm, "SkipThis", "NormJavaSqlHashCode", null);
						
			// Do some calculations (which was hard to do in a PORTABLE SQL Way)
			int pos_sumLogicalReads             = _shortRstm.findColumn("sumLogicalReads");
			int pos_sumRowsAffected             = _shortRstm.findColumn("sumRowsAffected");
			int pos_LogicalReadsPerRowsAffected = _shortRstm.findColumn("LogicalReadsPerRowsAffected");

			if (pos_sumLogicalReads >= 0 && pos_sumRowsAffected >= 0 && pos_LogicalReadsPerRowsAffected >= 0)
			{
				for (int r=0; r<_shortRstm.getRowCount(); r++)
				{
					//------------------------------------------------
					// set "LogicalReadsPerRowsAffected"
					long sumLogicalReads = _shortRstm.getValueAsLong(r, pos_sumLogicalReads);
					long sumRowsAffected = _shortRstm.getValueAsLong(r, pos_sumRowsAffected);

					BigDecimal calc = new BigDecimal(-1);
					if (sumRowsAffected > 0)
						calc = new BigDecimal( (sumLogicalReads*1.0) / (sumRowsAffected*1.0) ).setScale(2, RoundingMode.HALF_EVEN);
					
					_shortRstm.setValueAtWithOverride(calc, r, pos_LogicalReadsPerRowsAffected);
				}
			}


			// Remove anything from the *SKIP* entries
			_skippedDsrRows = removeSkippedEntries(_shortRstm, topRows, getDsrSkipEntries());


			// Check if table "MonSqlCapSqlText" has Dictionary Compressed Columns (any columns ends with "$dcc$")
			boolean hasDictCompCols = false;
			try {
				hasDictCompCols = DictCompression.hasCompressedColumnNames(conn, null, "MonSqlCapSqlText");
			} catch (SQLException ex) {
				_logger.error("Problems checking for Dictionary Compressed Columns in table 'MonSqlCapSqlText'.", ex);
			}

			// Get SQL Text
			if (_shortRstm.getRowCount() > 0)
			{
				// For each record... try to get the SQL Text based on the NormJavaSqlHashCode
				int pos_NormJavaSqlHashCode = _shortRstm.findColumn("NormJavaSqlHashCode");
				if (pos_NormJavaSqlHashCode != -1)
				{
					for (int r=0; r<_shortRstm.getRowCount(); r++)
					{
						int NormJavaSqlHashCode = _shortRstm.getValueAsInteger(r, pos_NormJavaSqlHashCode);
						
						if (NormJavaSqlHashCode != -1)
						{
							String col_NormSQLText = "[NormSQLText]";
							if (hasDictCompCols)
								col_NormSQLText = DictCompression.getRewriteForColumnName("MonSqlCapSqlText", "NormSQLText$dcc$").replace(" AS [NormSQLText]", "");

							// TODO: remove the <xmp> and replace it wit a post processing step where it's inserted (ResultSetTableModel rendering function)
							sql = ""
								    + "select distinct [NormJavaSqlHashCode], [ServerLogin], cast('' as varchar(512)) as [SkipThis], " + col_NormSQLText + " as [NormSQLText] \n"
								    + "from [MonSqlCapSqlText] \n"
								    + "where [NormJavaSqlHashCode] = " + NormJavaSqlHashCode + " \n"
								    + "";
							
							sql = conn.quotifySqlString(sql);
							try ( Statement stmnt = conn.createStatement() )
							{
								// Unlimited execution time
								stmnt.setQueryTimeout(0);
								try ( ResultSet rs = stmnt.executeQuery(sql) )
								{
//									ResultSetTableModel rstm = new ResultSetTableModel(rs, "SqlDetails");
									ResultSetTableModel rstm = createResultSetTableModel(rs, "SqlDetails", sql);
									
									if (_longRstm == null)
										_longRstm = rstm;
									else
										_longRstm.add(rstm);
	
									// Fill in a column with a "skip link" to DbxCentral
									setSkipEntriesUrl(_longRstm, "SkipThis", "NormJavaSqlHashCode", "NormSQLText");
									
									if (_logger.isDebugEnabled())
										_logger.debug("SqlDetails.getRowCount()="+ rstm.getRowCount());
								}
							}
							catch(SQLException ex)
							{
								setProblemException(ex);
	
								_logger.warn("Problems getting SQL by NormJavaSqlHashCode = "+NormJavaSqlHashCode+": " + ex);
							} 
							catch(ModelMissmatchException ex)
							{
								setProblemException(ex);
	
								_logger.warn("Problems (merging into previous ResultSetTableModel) when getting SQL by NormJavaSqlHashCode = "+NormJavaSqlHashCode+": " + ex);
							} 
						}
					}
				}
			}
		}
	}
}
