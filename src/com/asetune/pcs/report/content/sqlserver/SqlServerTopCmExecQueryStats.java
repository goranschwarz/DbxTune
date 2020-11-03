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
package com.asetune.pcs.report.content.sqlserver;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.DictCompression;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class SqlServerTopCmExecQueryStats
extends SqlServerAbstract
{
	private static Logger _logger = Logger.getLogger(SqlServerTopCmExecQueryStats.class);

	private ResultSetTableModel _shortRstm;
	private Map<String, String> _planMap;

	public SqlServerTopCmExecQueryStats(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public void writeMessageText(Writer sb)
	throws IOException
	{
		// Get a description of this section, and column names
		sb.append(getSectionDescriptionHtml(_shortRstm, true));

		sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
		sb.append(toHtmlTable(_shortRstm));

		if (_planMap != null && !_planMap.isEmpty())
		{
			sb.append("\n");
			sb.append("<!-- read Javascript and CSS for Showplan --> \n");
			sb.append("<link rel='stylesheet' type='text/css' href='http://www.dbxtune.com/sqlserver_showplan/css/qp.css'> \n");
			sb.append("<script src='http://www.dbxtune.com/sqlserver_showplan/dist/qp.js' type='text/javascript'></script> \n");
			sb.append("\n");
			
			sb.append("<br> \n");
			sb.append("<div id='showplan-list'> \n");
			sb.append("<b>Display the execution plan for any of the following <code>plan_handle</code>: </b> \n");
			sb.append("<ul> \n");
			for (String planHandle : _planMap.keySet())
			{
				sb.append("    <li> <a href='#showplan-list' title='Copy plan to clipboard and show in browser' onclick='showplanForId(\"").append(planHandle).append("\"); return true;'><code>").append(planHandle).append("</code></a> </li> \n");
			}
			sb.append("</ul> \n");
			sb.append("</div> \n");
			sb.append(" \n");
			
			sb.append("<div id='showplan-head'></div> \n");
			sb.append("<div id='showplan-container'></div> \n");
			sb.append("<script type='text/javascript'> \n");
			sb.append("    function showplanForId(id) \n");
			sb.append("    { \n");
			sb.append("        var showplanText = document.getElementById('plan_'+id).innerHTML \n");
			sb.append("        QP.showPlan(document.getElementById('showplan-container'), showplanText); \n");
			sb.append("        document.getElementById('showplan-head').innerHTML = 'Below is Execution plan for <code>plan_handle: ' + id + \"</code> <br>Note: You can also view your plan at <a href='http://www.supratimas.com' target='_blank'>http://www.supratimas.com</a>, or any other <i>plan-view</i> application by pasting (Ctrl-V) the clipboard content. <br>SentryOne Plan Explorer can be downloaded here: <a href='https://www.sentryone.com/plan-explorer' target='_blank'>https://www.sentryone.com/plan-explorer</a>\"; \n");
			sb.append("        copyStringToClipboard(showplanText); \n");
			sb.append("    } \n");
			sb.append("\n");
			sb.append("    function copyStringToClipboard (string)                                   \n");
			sb.append("    {                                                                         \n");
			sb.append("        function handler (event)                                              \n");
			sb.append("        {                                                                     \n");
			sb.append("            event.clipboardData.setData('text/plain', string);                \n");
			sb.append("            event.preventDefault();                                           \n");
			sb.append("            document.removeEventListener('copy', handler, true);              \n");
			sb.append("        }                                                                     \n");
			sb.append("                                                                              \n");
			sb.append("        document.addEventListener('copy', handler, true);                     \n");
			sb.append("        document.execCommand('copy');                                         \n");
			sb.append("    }                                                                         \n");
			sb.append("</script> \n");

			for (String planHandle : _planMap.keySet())
			{
				String xmlPlan = _planMap.get(planHandle);

				sb.append("\n<script id='plan_").append(planHandle).append("' type='text/xmldata'>\n");
				sb.append(xmlPlan);
				sb.append("\n</script>\n");
			}
		}
	}

//	@Override
//	public String getMessageText()
//	{
//		StringBuilder sb = new StringBuilder();
//
//		// Get a description of this section, and column names
//		sb.append(getSectionDescriptionHtml(_shortRstm, true));
//
//		sb.append("Row Count: ").append(_shortRstm.getRowCount()).append("<br>\n");
////		sb.append(_shortRstm.toHtmlTableString("sortable"));
//		sb.append(toHtmlTable(_shortRstm));
//
//		if (_planMap != null && !_planMap.isEmpty())
//		{
//			sb.append("\n");
//			sb.append("<!-- read Javascript and CSS for Showplan --> \n");
//			sb.append("<link rel='stylesheet' type='text/css' href='http://www.dbxtune.com/sqlserver_showplan/css/qp.css'> \n");
//			sb.append("<script src='http://www.dbxtune.com/sqlserver_showplan/dist/qp.js' type='text/javascript'></script> \n");
//			sb.append("\n");
//			
//			sb.append("<br> \n");
//			sb.append("<div id='showplan-list'> \n");
//			sb.append("<b>Display the execution plan for any of the following <code>plan_handle</code>: </b> \n");
//			sb.append("<ul> \n");
//			for (String planHandle : _planMap.keySet())
//			{
//				sb.append("    <li> <a href='#showplan-list' title='Copy plan to clipboard and show in browser' onclick='showplanForId(\"").append(planHandle).append("\"); return true;'><code>").append(planHandle).append("</code></a> </li> \n");
//			}
//			sb.append("</ul> \n");
//			sb.append("</div> \n");
//			sb.append(" \n");
//			
//			sb.append("<div id='showplan-head'></div> \n");
//			sb.append("<div id='showplan-container'></div> \n");
//			sb.append("<script type='text/javascript'> \n");
//			sb.append("    function showplanForId(id) \n");
//			sb.append("    { \n");
//			sb.append("        var showplanText = document.getElementById('plan_'+id).innerHTML \n");
//			sb.append("        QP.showPlan(document.getElementById('showplan-container'), showplanText); \n");
//			sb.append("        document.getElementById('showplan-head').innerHTML = 'Below is Execution plan for <code>plan_handle: ' + id + \"</code> <br>Note: You can also view your plan at <a href='http://www.supratimas.com' target='_blank'>http://www.supratimas.com</a>, or any other <i>plan-view</i> application by pasting (Ctrl-V) the clipboard content. <br>SentryOne Plan Explorer can be downloaded here: <a href='https://www.sentryone.com/plan-explorer' target='_blank'>https://www.sentryone.com/plan-explorer</a>\"; \n");
//			sb.append("        copyStringToClipboard(showplanText); \n");
//			sb.append("    } \n");
//			sb.append("\n");
//			sb.append("    function copyStringToClipboard (string)                                   \n");
//			sb.append("    {                                                                         \n");
//			sb.append("        function handler (event)                                              \n");
//			sb.append("        {                                                                     \n");
//			sb.append("            event.clipboardData.setData('text/plain', string);                \n");
//			sb.append("            event.preventDefault();                                           \n");
//			sb.append("            document.removeEventListener('copy', handler, true);              \n");
//			sb.append("        }                                                                     \n");
//			sb.append("                                                                              \n");
//			sb.append("        document.addEventListener('copy', handler, true);                     \n");
//			sb.append("        document.execCommand('copy');                                         \n");
//			sb.append("    }                                                                         \n");
//			sb.append("</script> \n");
//
//			for (String planHandle : _planMap.keySet())
//			{
//				String xmlPlan = _planMap.get(planHandle);
//
//				sb.append("\n<script id='plan_").append(planHandle).append("' type='text/xmldata'>\n");
//				sb.append(xmlPlan);
//				sb.append("\n</script>\n");
//			}
//		}
//
////		if (_CmDeviceIo_IoRW != null)
////		{
////			sb.append(getDbxCentralLinkWithDescForGraphs(true, "Below are Graphs/Charts with various information that can help you decide how the IO Subsystem is handling the load.",
////					"CmDeviceIo_IoRW",
////					"CmDeviceIo_SvcTimeRW",
////					"CmDeviceIo_SvcTimeR",
////					"CmDeviceIo_SvcTimeW"
////					));
////
////			sb.append(_CmDeviceIo_IoRW             .getHtmlContent(null, null));
////			sb.append(_CmDeviceIo_SvcTimeRW_noLimit.getHtmlContent(null, null));
////			sb.append(_CmDeviceIo_SvcTimeRW        .getHtmlContent(null, null));
////			sb.append(_CmDeviceIo_SvcTimeR         .getHtmlContent(null, null));
////			sb.append(_CmDeviceIo_SvcTimeW         .getHtmlContent(null, null));
////		}
//
//		return sb.toString();
//	}

	@Override
	public String getSubject()
	{
		return "Top SQL Statements (order by: XXX, origin: CmExecQueryStats/dm_exec_query_stats)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmExecQueryStats_diff" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		 // just to get Column names
		String dummySql = "select * from [CmExecQueryStats_rate] where 1 = 2";
		ResultSetTableModel dummyRstm = executeQuery(conn, dummySql, false, "metadata");
		if (dummyRstm == null)
		{
			String msg = "Table 'CmExecQueryStats_rate' did not exist. So Performance Counters for this hasn't been sampled during this period.";

			//addMessage(msg);
			setProblemException(new Exception(msg));

			_shortRstm = ResultSetTableModel.createEmpty("TopCmExecQueryStats");
			return;
		}

		String col_total_worker_time__sum               = !dummyRstm.hasColumnNoCase("total_worker_time"              ) ? "" : "    ,sum([total_worker_time])               as [total_worker_time__sum]               "; 
		String col_total_physical_reads__sum            = !dummyRstm.hasColumnNoCase("total_physical_reads"           ) ? "" : "    ,sum([total_physical_reads])            as [total_physical_reads__sum]            "; 
		String col_total_logical_writes__sum            = !dummyRstm.hasColumnNoCase("total_logical_writes"           ) ? "" : "    ,sum([total_logical_writes])            as [total_logical_writes__sum]            "; 
		String col_total_logical_reads__sum             = !dummyRstm.hasColumnNoCase("total_logical_reads"            ) ? "" : "    ,sum([total_logical_reads])             as [total_logical_reads__sum]             "; 
		String col_total_clr_time__sum                  = !dummyRstm.hasColumnNoCase("total_clr_time"                 ) ? "" : "    ,sum([total_clr_time])                  as [total_clr_time__sum]                  "; 
		String col_total_elapsed_time__sum              = !dummyRstm.hasColumnNoCase("total_elapsed_time"             ) ? "" : "    ,sum([total_elapsed_time])              as [total_elapsed_time__sum]              "; 
		String col_total_rows__sum                      = !dummyRstm.hasColumnNoCase("total_rows"                     ) ? "" : "    ,sum([total_rows])                      as [total_rows__sum]                      "; 
		String col_total_dop__sum                       = !dummyRstm.hasColumnNoCase("total_dop"                      ) ? "" : "    ,sum([total_dop])                       as [total_dop__sum]                       "; 
		String col_total_grant_kb__sum                  = !dummyRstm.hasColumnNoCase("total_grant_kb"                 ) ? "" : "    ,sum([total_grant_kb])                  as [total_grant_kb__sum]                  "; 
		String col_total_used_grant_kb__sum             = !dummyRstm.hasColumnNoCase("total_used_grant_kb"            ) ? "" : "    ,sum([total_used_grant_kb])             as [total_used_grant_kb__sum]             "; 
		String col_total_ideal_grant_kb__sum            = !dummyRstm.hasColumnNoCase("total_ideal_grant_kb"           ) ? "" : "    ,sum([total_ideal_grant_kb])            as [total_ideal_grant_kb__sum]            "; 
		String col_total_reserved_threads__sum          = !dummyRstm.hasColumnNoCase("total_reserved_threads"         ) ? "" : "    ,sum([total_reserved_threads])          as [total_reserved_threads__sum]          "; 
		String col_total_used_threads__sum              = !dummyRstm.hasColumnNoCase("total_used_threads"             ) ? "" : "    ,sum([total_used_threads])              as [total_used_threads__sum]              "; 
		String col_total_columnstore_segment_reads__sum = !dummyRstm.hasColumnNoCase("total_columnstore_segment_reads") ? "" : "    ,sum([total_columnstore_segment_reads]) as [total_columnstore_segment_reads__sum] "; 
		String col_total_columnstore_segment_skips__sum = !dummyRstm.hasColumnNoCase("total_columnstore_segment_skips") ? "" : "    ,sum([total_columnstore_segment_skips]) as [total_columnstore_segment_skips__sum] "; 
		String col_total_spills__sum                    = !dummyRstm.hasColumnNoCase("total_spills"                   ) ? "" : "    ,sum([total_spills])                    as [total_spills__sum]                    "; 
		String col_total_page_server_reads__sum         = !dummyRstm.hasColumnNoCase("total_page_server_reads"        ) ? "" : "    ,sum([total_page_server_reads])         as [total_page_server_reads__sum]         "; 

		String col_AvgWorkerTimeUs                      = !dummyRstm.hasColumnNoCase("total_worker_time"              ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgWorkerTimeUs]            \n";
		String col_AvgPhysicalReads                     = !dummyRstm.hasColumnNoCase("total_physical_reads"           ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgPhysicalReads]           \n";
		String col_AvgLogicalWrites                     = !dummyRstm.hasColumnNoCase("total_logical_writes"           ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgLogicalWrites]           \n";
		String col_AvgLogicalReads                      = !dummyRstm.hasColumnNoCase("total_logical_reads"            ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgLogicalReads]            \n";
		String col_AvgClrTimeUs                         = !dummyRstm.hasColumnNoCase("total_clr_time"                 ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgClrTimeUs]               \n";
		String col_AvgElapsedTimeUs                     = !dummyRstm.hasColumnNoCase("total_elapsed_time"             ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgElapsedTimeUs]           \n";
		String col_AvgRows                              = !dummyRstm.hasColumnNoCase("total_rows"                     ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgRows]                    \n";
		String col_AvgDop                               = !dummyRstm.hasColumnNoCase("total_dop"                      ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgDop]                     \n";
		String col_AvgGrantKb                           = !dummyRstm.hasColumnNoCase("total_grant_kb"                 ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgGrantKb]                 \n";
		String col_AvgUsedGrantKb                       = !dummyRstm.hasColumnNoCase("total_used_grant_kb"            ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgUsedGrantKb]             \n";
		String col_AvgIdealGrantKb                      = !dummyRstm.hasColumnNoCase("total_ideal_grant_kb"           ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgIdealGrantKb]            \n";
		String col_AvgReservedThreads                   = !dummyRstm.hasColumnNoCase("total_reserved_threads"         ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgReservedThreads]         \n";
		String col_AvgUsedThreads                       = !dummyRstm.hasColumnNoCase("total_used_threads"             ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgUsedThreads]             \n";
		String col_AvgColumnstoreSegmentReads           = !dummyRstm.hasColumnNoCase("total_columnstore_segment_reads") ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgColumnstoreSegmentReads] \n"; 
		String col_AvgColumnstoreSegmentSkips           = !dummyRstm.hasColumnNoCase("total_columnstore_segment_skips") ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgColumnstoreSegmentSkips] \n"; 
		String col_AvgSpills                            = !dummyRstm.hasColumnNoCase("total_spills"                   ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgSpills]                  \n";
		String col_AvgPageServerReads                   = !dummyRstm.hasColumnNoCase("total_page_server_reads"        ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgPageServerReads]         \n"; 


		int topRows = localConf.getIntProperty(this.getClass().getSimpleName()+".top", 20);

		String orderByCol = "[total_worker_time__sum]";
//		String orderByCol = "[samples__count]";
//		if (dummyRstm.hasColumnNoCase("total_worker_time"))     { orderByCol = "[total_worker_time__sum]";     }

		// Check if table "CmPgStatements_diff" has Dictionary Compressed Columns (any columns ends with "$dcc$")
		boolean hasDictCompCols = false;
		try {
			hasDictCompCols = DictCompression.hasCompressedColumnNames(conn, null, "CmExecQueryStats_diff");
		} catch (SQLException ex) {
			_logger.error("Problems checking for Dictionary Compressed Columns in table 'CmExecQueryStats_diff'.", ex);
		}
		
		String col_SqlText = "SqlText";
		if (hasDictCompCols)
			col_SqlText = "SqlText$dcc$";


		String sql = getCmDiffColumnsAsSqlComment("CmActiveStatements")
			    + "select top " + topRows + " \n"
			    + "     [dbname] \n"
			    + "    ,[plan_handle] \n"
			    + "    ,max([query_plan_hash])                 as [query_plan_hash] \n"
			    + "    ,max([query_hash])                      as [query_hash] \n"
			    + "    ,count(*)                               as [samples__count] \n"
			    + "    ,min([SessionSampleTime])               as [SessionSampleTime__min] \n"
			    + "    ,max([SessionSampleTime])               as [SessionSampleTime__max] \n"
			    + "    ,cast('' as varchar(30))                as [Duration] \n"
//			    + "    ,sum([CmSampleMs])                      as [CmSampleMs__sum] \n"
			    + "    \n"
			    + "    ,sum([execution_count])                 as [execution_count__sum]\n"
			    + "    ,min([creation_time])                   as [creation_time__min]\n"
			    + "    ,max([last_execution_time])             as [last_execution_time__max]\n"
			    + "    \n"
			    + col_total_worker_time__sum               + col_AvgWorkerTimeUs            
			    + col_total_physical_reads__sum            + col_AvgPhysicalReads           
			    + col_total_logical_writes__sum            + col_AvgLogicalWrites           
			    + col_total_logical_reads__sum             + col_AvgLogicalReads            
			    + col_total_clr_time__sum                  + col_AvgClrTimeUs               
			    + col_total_elapsed_time__sum              + col_AvgElapsedTimeUs           
			    + col_total_rows__sum                      + col_AvgRows                    
			    + col_total_dop__sum                       + col_AvgDop                     
			    + col_total_grant_kb__sum                  + col_AvgGrantKb                 
			    + col_total_used_grant_kb__sum             + col_AvgUsedGrantKb             
			    + col_total_ideal_grant_kb__sum            + col_AvgIdealGrantKb            
			    + col_total_reserved_threads__sum          + col_AvgReservedThreads         
			    + col_total_used_threads__sum              + col_AvgUsedThreads             
			    + col_total_columnstore_segment_reads__sum + col_AvgColumnstoreSegmentReads 
			    + col_total_columnstore_segment_skips__sum + col_AvgColumnstoreSegmentSkips 
			    + col_total_spills__sum                    + col_AvgSpills                  
			    + col_total_page_server_reads__sum         + col_AvgPageServerReads         
			    + "    \n"
//			    + "    ,max([SqlText])                         as [SqlText] \n"
			    + "    ,max([" + col_SqlText +"])                    as [SqlText] \n"
				+ "from [CmExecQueryStats_diff] \n"
				+ "where [CmNewDiffRateRow] = 0 -- only records that has been diff calculations (not first time seen, when it swaps in/out due to execution every x minute) \n"
				+ "  and [execution_count] > 0 \n"
//				+ "  and [AvgServ_ms] > " + _aboveServiceTime + " \n"
//				+ "  and [TotalIOs]   > " + _aboveTotalIos    + " \n"
				+ getReportPeriodSqlWhere()
				+ "group by [plan_handle] \n"
				+ "order by " + orderByCol + " desc \n"
			    + "";
		
		_shortRstm = executeQuery(conn, sql, false, "TopCmExecQueryStats");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("TopCmExecQueryStats");
			return;
		}
		else
		{

			// Describe the table
			setSectionDescription(_shortRstm);

			setDurationColumn(_shortRstm, "SessionSampleTime__min", "SessionSampleTime__max", "Duration");

			calculateAvg(_shortRstm);

			// get Dictionary Compressed values for column: SqlText
			if (hasDictCompCols)
			{
				updateDictionaryCompressedColumn(_shortRstm, conn, null, "CmExecQueryStats", "SqlText", null);
			}

			// Get Showplan for StatementCache entries, and SqlText from the above table
			Set<String> planHandleObjects = getPlanHandleObjects(_shortRstm, "plan_handle");
			if (planHandleObjects != null && ! planHandleObjects.isEmpty() )
			{
				try 
				{
//					_ssqlRstm = getShowplanFromMonDdlStorage(conn, planHandleObjects);
					_planMap = getShowplanAsMapFromMonDdlStorage(conn, planHandleObjects);
				}
				catch (SQLException ex)
				{
					setProblemException(ex);
				}
			}
		}
	}

	private void calculateAvg(ResultSetTableModel rstm)
	{
		for (int r=0; r<rstm.getRowCount(); r++)
		{
			calculateAvg(rstm, r, "execution_count__sum", "total_worker_time__sum"              , "AvgWorkerTimeUs");           
			calculateAvg(rstm, r, "execution_count__sum", "total_physical_reads__sum"           , "AvgPhysicalReads");          
			calculateAvg(rstm, r, "execution_count__sum", "total_logical_writes__sum"           , "AvgLogicalWrites");          
			calculateAvg(rstm, r, "execution_count__sum", "total_logical_reads__sum"            , "AvgLogicalReads");           
			calculateAvg(rstm, r, "execution_count__sum", "total_clr_time__sum"                 , "AvgClrTimeUs");              
			calculateAvg(rstm, r, "execution_count__sum", "total_elapsed_time__sum"             , "AvgElapsedTimeUs");          
			calculateAvg(rstm, r, "execution_count__sum", "total_rows__sum"                     , "AvgRows");                   
			calculateAvg(rstm, r, "execution_count__sum", "total_dop__sum"                      , "AvgDop");                    
			calculateAvg(rstm, r, "execution_count__sum", "total_grant_kb__sum"                 , "AvgGrantKb");                
			calculateAvg(rstm, r, "execution_count__sum", "total_used_grant_kb__sum"            , "AvgUsedGrantKb");            
			calculateAvg(rstm, r, "execution_count__sum", "total_ideal_grant_kb__sum"           , "AvgIdealGrantKb");           
			calculateAvg(rstm, r, "execution_count__sum", "total_reserved_threads__sum"         , "AvgReservedThreads");        
			calculateAvg(rstm, r, "execution_count__sum", "total_used_threads__sum"             , "AvgUsedThreads");            
			calculateAvg(rstm, r, "execution_count__sum", "total_columnstore_segment_reads__sum", "AvgColumnstoreSegmentReads");
			calculateAvg(rstm, r, "execution_count__sum", "total_columnstore_segment_skips__sum", "AvgColumnstoreSegmentSkips");
			calculateAvg(rstm, r, "execution_count__sum", "total_spills__sum"                   , "AvgSpills");                 
			calculateAvg(rstm, r, "execution_count__sum", "total_page_server_reads__sum"        , "AvgPageServerReads");        
		}
	}

	private void calculateAvg(ResultSetTableModel rstm, int r, String cntColName, String srcColName, String destColName)
	{
		int pos_cnt  = rstm.findColumnNoCase(cntColName);
		int pos_src  = rstm.findColumnNoCase(srcColName);
		int pos_dest = rstm.findColumnNoCase(destColName);
		
		// Any of the columns was NOT found
		if (pos_cnt == -1 || pos_src == -1 || pos_dest == -1)
		{
			if (_logger.isDebugEnabled())
				_logger.debug("calculateAvg(): Some columns was NOT Found when calculation average value for: row="+r+", cntColName["+cntColName+"]="+pos_cnt+", srcColName["+srcColName+"]="+pos_src+", destColName["+destColName+"]="+pos_dest+"."); 
			return;
		}

		long cnt = rstm.getValueAsLong(r, pos_cnt);
		long src = rstm.getValueAsLong(r, pos_src);

		BigDecimal calc;
		if (cnt > 0)
		{
			calc = new BigDecimal( (src*1.0) / (cnt*1.0) ).setScale(1, RoundingMode.HALF_EVEN);
		}
		else
		{
			calc = new BigDecimal(-1);
		}
		
		rstm.setValueAtWithOverride(calc, r, pos_dest);
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
				"Top SQL Statements (and it's Query Plan)...  (ordered by: total_worker_time__sum) <br>" +
				"<br>" +
				"SqlServer Source table is 'dm_exec_query_stats'. <br>" +
				"PCS Source table is 'CmExecQueryStats_diff'. (PCS = Persistent Counter Store) <br>" +
				"");
	}
}

/*
 * 
 * 1> select * from sys.dm_exec_query_stats
 * 
 * RS> Col# Label                           JDBC Type Name           Guessed DBMS type Source Table
 * RS> ---- ------------------------------- ------------------------ ----------------- ------------
 * RS> 1    sql_handle                      java.sql.Types.VARBINARY varbinary(128)    -none-      
 * RS> 2    statement_start_offset          java.sql.Types.INTEGER   int               -none-      
 * RS> 3    statement_end_offset            java.sql.Types.INTEGER   int               -none-      
 * RS> 4    plan_generation_num             java.sql.Types.BIGINT    bigint            -none-      
 * RS> 5    plan_handle                     java.sql.Types.VARBINARY varbinary(128)    -none-      
 * RS> 6    creation_time                   java.sql.Types.TIMESTAMP datetime          -none-      
 * RS> 7    last_execution_time             java.sql.Types.TIMESTAMP datetime          -none-      
 * RS> 8    execution_count                 java.sql.Types.BIGINT    bigint            -none-      
 * RS> 9    total_worker_time               java.sql.Types.BIGINT    bigint            -none-      
 * RS> 10   last_worker_time                java.sql.Types.BIGINT    bigint            -none-      
 * RS> 11   min_worker_time                 java.sql.Types.BIGINT    bigint            -none-      
 * RS> 12   max_worker_time                 java.sql.Types.BIGINT    bigint            -none-      
 * RS> 13   total_physical_reads            java.sql.Types.BIGINT    bigint            -none-      
 * RS> 14   last_physical_reads             java.sql.Types.BIGINT    bigint            -none-      
 * RS> 15   min_physical_reads              java.sql.Types.BIGINT    bigint            -none-      
 * RS> 16   max_physical_reads              java.sql.Types.BIGINT    bigint            -none-      
 * RS> 17   total_logical_writes            java.sql.Types.BIGINT    bigint            -none-      
 * RS> 18   last_logical_writes             java.sql.Types.BIGINT    bigint            -none-      
 * RS> 19   min_logical_writes              java.sql.Types.BIGINT    bigint            -none-      
 * RS> 20   max_logical_writes              java.sql.Types.BIGINT    bigint            -none-      
 * RS> 21   total_logical_reads             java.sql.Types.BIGINT    bigint            -none-      
 * RS> 22   last_logical_reads              java.sql.Types.BIGINT    bigint            -none-      
 * RS> 23   min_logical_reads               java.sql.Types.BIGINT    bigint            -none-      
 * RS> 24   max_logical_reads               java.sql.Types.BIGINT    bigint            -none-      
 * RS> 25   total_clr_time                  java.sql.Types.BIGINT    bigint            -none-      
 * RS> 26   last_clr_time                   java.sql.Types.BIGINT    bigint            -none-      
 * RS> 27   min_clr_time                    java.sql.Types.BIGINT    bigint            -none-      
 * RS> 28   max_clr_time                    java.sql.Types.BIGINT    bigint            -none-      
 * RS> 29   total_elapsed_time              java.sql.Types.BIGINT    bigint            -none-      
 * RS> 30   last_elapsed_time               java.sql.Types.BIGINT    bigint            -none-      
 * RS> 31   min_elapsed_time                java.sql.Types.BIGINT    bigint            -none-      
 * RS> 32   max_elapsed_time                java.sql.Types.BIGINT    bigint            -none-      
 * RS> 33   query_hash                      java.sql.Types.BINARY    binary(16)        -none-      
 * RS> 34   query_plan_hash                 java.sql.Types.BINARY    binary(16)        -none-      
 * RS> 35   total_rows                      java.sql.Types.BIGINT    bigint            -none-      
 * RS> 36   last_rows                       java.sql.Types.BIGINT    bigint            -none-      
 * RS> 37   min_rows                        java.sql.Types.BIGINT    bigint            -none-      
 * RS> 38   max_rows                        java.sql.Types.BIGINT    bigint            -none-      
 * RS> 39   statement_sql_handle            java.sql.Types.VARBINARY varbinary(128)    -none-      
 * RS> 40   statement_context_id            java.sql.Types.BIGINT    bigint            -none-      
 * RS> 41   total_dop                       java.sql.Types.BIGINT    bigint            -none-      
 * RS> 42   last_dop                        java.sql.Types.BIGINT    bigint            -none-      
 * RS> 43   min_dop                         java.sql.Types.BIGINT    bigint            -none-      
 * RS> 44   max_dop                         java.sql.Types.BIGINT    bigint            -none-      
 * RS> 45   total_grant_kb                  java.sql.Types.BIGINT    bigint            -none-      
 * RS> 46   last_grant_kb                   java.sql.Types.BIGINT    bigint            -none-      
 * RS> 47   min_grant_kb                    java.sql.Types.BIGINT    bigint            -none-      
 * RS> 48   max_grant_kb                    java.sql.Types.BIGINT    bigint            -none-      
 * RS> 49   total_used_grant_kb             java.sql.Types.BIGINT    bigint            -none-      
 * RS> 50   last_used_grant_kb              java.sql.Types.BIGINT    bigint            -none-      
 * RS> 51   min_used_grant_kb               java.sql.Types.BIGINT    bigint            -none-      
 * RS> 52   max_used_grant_kb               java.sql.Types.BIGINT    bigint            -none-      
 * RS> 53   total_ideal_grant_kb            java.sql.Types.BIGINT    bigint            -none-      
 * RS> 54   last_ideal_grant_kb             java.sql.Types.BIGINT    bigint            -none-      
 * RS> 55   min_ideal_grant_kb              java.sql.Types.BIGINT    bigint            -none-      
 * RS> 56   max_ideal_grant_kb              java.sql.Types.BIGINT    bigint            -none-      
 * RS> 57   total_reserved_threads          java.sql.Types.BIGINT    bigint            -none-      
 * RS> 58   last_reserved_threads           java.sql.Types.BIGINT    bigint            -none-      
 * RS> 59   min_reserved_threads            java.sql.Types.BIGINT    bigint            -none-      
 * RS> 60   max_reserved_threads            java.sql.Types.BIGINT    bigint            -none-      
 * RS> 61   total_used_threads              java.sql.Types.BIGINT    bigint            -none-      
 * RS> 62   last_used_threads               java.sql.Types.BIGINT    bigint            -none-      
 * RS> 63   min_used_threads                java.sql.Types.BIGINT    bigint            -none-      
 * RS> 64   max_used_threads                java.sql.Types.BIGINT    bigint            -none-      
 * RS> 65   total_columnstore_segment_reads java.sql.Types.BIGINT    bigint            -none-      
 * RS> 66   last_columnstore_segment_reads  java.sql.Types.BIGINT    bigint            -none-      
 * RS> 67   min_columnstore_segment_reads   java.sql.Types.BIGINT    bigint            -none-      
 * RS> 68   max_columnstore_segment_reads   java.sql.Types.BIGINT    bigint            -none-      
 * RS> 69   total_columnstore_segment_skips java.sql.Types.BIGINT    bigint            -none-      
 * RS> 70   last_columnstore_segment_skips  java.sql.Types.BIGINT    bigint            -none-      
 * RS> 71   min_columnstore_segment_skips   java.sql.Types.BIGINT    bigint            -none-      
 * RS> 72   max_columnstore_segment_skips   java.sql.Types.BIGINT    bigint            -none-      
 * RS> 73   total_spills                    java.sql.Types.BIGINT    bigint            -none-      
 * RS> 74   last_spills                     java.sql.Types.BIGINT    bigint            -none-      
 * RS> 75   min_spills                      java.sql.Types.BIGINT    bigint            -none-      
 * RS> 76   max_spills                      java.sql.Types.BIGINT    bigint            -none-      
 * RS> 77   total_num_physical_reads        java.sql.Types.BIGINT    bigint            -none-      
 * RS> 78   last_num_physical_reads         java.sql.Types.BIGINT    bigint            -none-      
 * RS> 79   min_num_physical_reads          java.sql.Types.BIGINT    bigint            -none-      
 * RS> 80   max_num_physical_reads          java.sql.Types.BIGINT    bigint            -none-      
 * RS> 81   total_page_server_reads         java.sql.Types.BIGINT    bigint            -none-      
 * RS> 82   last_page_server_reads          java.sql.Types.BIGINT    bigint            -none-      
 * RS> 83   min_page_server_reads           java.sql.Types.BIGINT    bigint            -none-      
 * RS> 84   max_page_server_reads           java.sql.Types.BIGINT    bigint            -none-      
 * RS> 85   total_num_page_server_reads     java.sql.Types.BIGINT    bigint            -none-      
 * RS> 86   last_num_page_server_reads      java.sql.Types.BIGINT    bigint            -none-      
 * RS> 87   min_num_page_server_reads       java.sql.Types.BIGINT    bigint            -none-      
 * RS> 88   max_num_page_server_reads       java.sql.Types.BIGINT    bigint            -none-      
 * -----END-----
 */
