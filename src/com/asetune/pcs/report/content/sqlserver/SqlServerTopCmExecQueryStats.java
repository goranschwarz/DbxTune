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
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.log4j.Logger;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.gui.ResultSetTableModel.TableStringRenderer;
import com.asetune.pcs.DictCompression;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.ase.SparklineHelper;
import com.asetune.pcs.report.content.ase.SparklineHelper.AggType;
import com.asetune.pcs.report.content.ase.SparklineHelper.DataSource;
import com.asetune.pcs.report.content.ase.SparklineHelper.SparkLineParams;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;

public class SqlServerTopCmExecQueryStats
extends SqlServerAbstract
{
	private static Logger _logger = Logger.getLogger(SqlServerTopCmExecQueryStats.class);

	private ResultSetTableModel _shortRstm;
	private ExecutionPlanCollection _planCollection;
	private List<String>        _miniChartJsList = new ArrayList<>();

	public SqlServerTopCmExecQueryStats(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public boolean hasShortMessageText()
	{
		return false;
	}

	@Override
	public void writeShortMessageText(Writer w)
	throws IOException
	{
	}

	@Override
	public void writeMessageText(Writer sb)
	throws IOException
	{
		// Get a description of this section, and column names
		sb.append(getSectionDescriptionHtml(_shortRstm, true));

//		sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
		sb.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
		TableStringRenderer tableRender = new ReportEntryTableStringRenderer()
		{
			@Override
			public String cellValue(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
			{
				if ("SqlText".equals(colName))
				{
					// Get Actual Executed SQL Text for current row
					String query_hash = rstm.getValueAsString(row, "query_hash");
				//	String sqlText    = rstm.getValueAsString(row, "SqlText");
					
					// Put the "Actual Executed SQL Text" as a "tooltip"
					return "<div title='Click for Detailes' "
							+ "data-toggle='modal' "
							+ "data-target='#dbx-view-sqltext-dialog' "
							+ "data-objectname='" + query_hash + "' "
							+ "data-tooltip=\""   + getTooltipForSqlText(rstm, row) + "\" "
							+ ">&#x1F4AC;</div>"; // symbol popup with "..."
				}

				return strVal;
			}
		};
		sb.append(_shortRstm.toHtmlTableString("sortable", true, true, null, tableRender));
//		sb.append(toHtmlTable(_shortRstm));

		// Write HTML/JavaScript Code for the Execution Plan...
		if (_planCollection != null)
			_planCollection.writeMessageText(sb);			

		// Write JavaScript code for CPU SparkLine
		for (String str : _miniChartJsList)
		{
			sb.append(str);
		}
	}

	/** double quotes (") must be avoided or escaped */
	private String getTooltipForSqlText(ResultSetTableModel rstm, int row)
	{
		StringBuilder sb = new StringBuilder();
		NumberFormat nf = NumberFormat.getInstance();

//		SessionSampleTime__min	SessionSampleTime__max	Duration	execution_count__chart	execution_count__sum	creation_time__min	last_execution_time__max	total_worker_time__chart	
		sb.append("-- Some columns extracted from current row.\n");
		sb.append("-----------------------------------------------------------------------------------------------\n");
		sb.append("-- query_hash:                ").append( rstm.getValueAsString(row, "query_hash"               ) ).append("\n");
		sb.append("-- SessionSampleTime__min:    ").append( rstm.getValueAsString(row, "SessionSampleTime__min"   ) ).append("\n");
		sb.append("-- SessionSampleTime__max:    ").append( rstm.getValueAsString(row, "SessionSampleTime__max"   ) ).append("\n");
		sb.append("-- Duration:                  ").append( rstm.getValueAsString(row, "Duration"                 ) ).append("\n");
		sb.append("-- creation_time__min:        ").append( rstm.getValueAsString(row, "creation_time__min"       ) ).append("\n");
		sb.append("-- last_execution_time__max:  ").append( rstm.getValueAsString(row, "last_execution_time__max" ) ).append("\n");
		
		sb.append("-- execution_count__sum:      ").append( nf.format(rstm.getValueAsBigDecimal(row, "execution_count__sum"     ))).append("\n");
		sb.append("-- total_worker_time__sum:    ").append( nf.format(rstm.getValueAsBigDecimal(row, "total_worker_time__sum"   ))).append("  (in micro seconds), and in (HH:MM:SS.sss) ").append(TimeUtils.usToTimeStrLong(rstm.getValueAsLong(row, "total_worker_time__sum"))).append(" \n");
		sb.append("-- AvgWorkerTimeUs:           ").append( nf.format(rstm.getValueAsBigDecimal(row, "AvgWorkerTimeUs"          ))).append("\n");
		sb.append("-- total_physical_reads__sum: ").append( nf.format(rstm.getValueAsBigDecimal(row, "total_physical_reads__sum"))).append("\n");
		sb.append("-- AvgPhysicalReads:          ").append( nf.format(rstm.getValueAsBigDecimal(row, "AvgPhysicalReads"         ))).append("\n");
		sb.append("-- total_logical_writes__sum: ").append( nf.format(rstm.getValueAsBigDecimal(row, "total_logical_writes__sum"))).append("\n");
		sb.append("-- AvgLogicalWrites:          ").append( nf.format(rstm.getValueAsBigDecimal(row, "AvgLogicalWrites"         ))).append("\n");
		sb.append("-- total_logical_reads__sum:  ").append( nf.format(rstm.getValueAsBigDecimal(row, "total_logical_reads__sum" ))).append("\n");
		sb.append("-- AvgLogicalReads:           ").append( nf.format(rstm.getValueAsBigDecimal(row, "AvgLogicalReads"          ))).append("\n");
		sb.append("-- total_elapsed_time__sum:   ").append( nf.format(rstm.getValueAsBigDecimal(row, "total_elapsed_time__sum"  ))).append("  (in micro seconds), and in (HH:MM:SS.sss) ").append(TimeUtils.usToTimeStrLong(rstm.getValueAsLong(row, "total_elapsed_time__sum"))).append(" \n");
		sb.append("-- AvgElapsedTimeUs:          ").append( nf.format(rstm.getValueAsBigDecimal(row, "AvgElapsedTimeUs"         ))).append("\n");
		sb.append("-- total_rows__sum:           ").append( nf.format(rstm.getValueAsBigDecimal(row, "total_rows__sum"          ))).append("\n");
		sb.append("-- AvgRows:                   ").append( nf.format(rstm.getValueAsBigDecimal(row, "AvgRows"                  ))).append("\n");
		sb.append("-- total_dop__sum:            ").append( nf.format(rstm.getValueAsBigDecimal(row, "total_dop__sum"           ))).append("\n");
		sb.append("-- AvgDop:                    ").append( nf.format(rstm.getValueAsBigDecimal(row, "AvgDop"                   ))).append("\n");
		sb.append("-- total_grant_kb__sum:       ").append( nf.format(rstm.getValueAsBigDecimal(row, "total_grant_kb__sum"      ))).append("\n");
		sb.append("-- AvgGrantKb:                ").append( nf.format(rstm.getValueAsBigDecimal(row, "AvgGrantKb"               ))).append("\n");
		sb.append("-----------------------------------------------------------------------------------------------\n");
		sb.append(StringEscapeUtils.escapeHtml4(rstm.getValueAsString(row, "SqlText")));

		return sb.toString();
	}
	
	@Override
	public String getSubject()
	{
		return "Top SQL Statements (order by: total_worker_time__sum, origin: CmExecQueryStats/dm_exec_query_stats)";
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

		String col_total_worker_time__chart             = !dummyRstm.hasColumnNoCase("total_worker_time"              ) ? "" : "    ,cast('' as varchar(512))  as [total_worker_time__chart]    \n"; 
		String col_total_physical_reads__chart          = !dummyRstm.hasColumnNoCase("total_physical_reads"           ) ? "" : "    ,cast('' as varchar(512))  as [total_physical_reads__chart] \n"; 
		String col_total_logical_reads__chart           = !dummyRstm.hasColumnNoCase("total_logical_reads"            ) ? "" : "    ,cast('' as varchar(512))  as [total_logical_reads__chart]  \n"; 
		String col_max_dop__chart                       = !dummyRstm.hasColumnNoCase("total_dop"                      ) ? "" : "    ,cast('' as varchar(512))  as [max_dop__chart]              \n"; 
		String col_max_grant_kb__chart                  = !dummyRstm.hasColumnNoCase("total_grant_kb"                 ) ? "" : "    ,cast('' as varchar(512))  as [max_grant_kb__chart]         \n"; 
		String col_total_spills__chart                  = !dummyRstm.hasColumnNoCase("total_spills"                   ) ? "" : "    ,cast('' as varchar(512))  as [total_spills__chart]         \n"; 
		

//		int topRows = localConf.getIntProperty(this.getClass().getSimpleName()+".top", 20);
		int topRows = getTopRows();

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


//		String sql = getCmDiffColumnsAsSqlComment("CmActiveStatements")
//			    + "select top " + topRows + " \n"
//			    + "     [dbname] \n"
//			    + "    ,[plan_handle] \n"
//			    + "    ,max([query_plan_hash])                 as [query_plan_hash] \n"
//			    + "    ,max([query_hash])                      as [query_hash] \n"
//			    + "    ,count(*)                               as [samples__count] \n"
//			    + "    ,min([SessionSampleTime])               as [SessionSampleTime__min] \n"
//			    + "    ,max([SessionSampleTime])               as [SessionSampleTime__max] \n"
//			    + "    ,cast('' as varchar(30))                as [Duration] \n"
////			    + "    ,sum([CmSampleMs])                      as [CmSampleMs__sum] \n"
//			    + "    \n"
//			    + "    ,cast('' as varchar(512))               as [execution_count__chart] \n"
//			    + "    ,sum([execution_count])                 as [execution_count__sum] \n"
//			    + "    ,min([creation_time])                   as [creation_time__min] \n"
//			    + "    ,max([last_execution_time])             as [last_execution_time__max] \n"
//			    + "    \n"
//			    + col_total_worker_time__chart            
//			    + col_total_worker_time__sum               + col_AvgWorkerTimeUs            
//			    + col_total_physical_reads__chart
//			    + col_total_physical_reads__sum            + col_AvgPhysicalReads           
//			    + col_total_logical_writes__sum            + col_AvgLogicalWrites           
//			    + col_total_logical_reads__chart
//			    + col_total_logical_reads__sum             + col_AvgLogicalReads            
//			    + col_total_clr_time__sum                  + col_AvgClrTimeUs               
//			    + col_total_elapsed_time__sum              + col_AvgElapsedTimeUs           
//			    + col_total_rows__sum                      + col_AvgRows                    
//			    + col_max_dop__chart
//			    + col_total_dop__sum                       + col_AvgDop                     
//			    + col_max_grant_kb__chart
//			    + col_total_grant_kb__sum                  + col_AvgGrantKb                 
//			    + col_total_used_grant_kb__sum             + col_AvgUsedGrantKb             
//			    + col_total_ideal_grant_kb__sum            + col_AvgIdealGrantKb            
//			    + col_total_reserved_threads__sum          + col_AvgReservedThreads         
//			    + col_total_used_threads__sum              + col_AvgUsedThreads             
//			    + col_total_columnstore_segment_reads__sum + col_AvgColumnstoreSegmentReads 
//			    + col_total_columnstore_segment_skips__sum + col_AvgColumnstoreSegmentSkips 
//			    + col_total_spills__chart
//			    + col_total_spills__sum                    + col_AvgSpills                  
//			    + col_total_page_server_reads__sum         + col_AvgPageServerReads         
//			    + "    \n"
////			    + "    ,max([SqlText])                         as [SqlText] \n"
//			    + "    ,max([" + col_SqlText +"])                    as [SqlText] \n"
//				+ "from [CmExecQueryStats_diff] \n"
//				+ "where [CmNewDiffRateRow] = 0 -- only records that has been diff calculations (not first time seen, when it swaps in/out due to execution every x minute) \n"
//				+ "  and [execution_count] > 0 \n"
////				+ "  and [AvgServ_ms] > " + _aboveServiceTime + " \n"
////				+ "  and [TotalIOs]   > " + _aboveTotalIos    + " \n"
//				+ getReportPeriodSqlWhere()
//				+ "group by [plan_handle] \n"
//				+ "order by " + orderByCol + " desc \n"
//			    + "";
		
//FIXME; group by 'query_hash' or 'query_plan_hash' ... also check that we pick up the correct PLAN in below table
//Possibly; add link directly in the table for 'plan_handle' instead of the below table!!!

		String sql = getCmDiffColumnsAsSqlComment("CmActiveStatements")
			    + "select top " + topRows + " \n"
			    + "     [dbname] \n"
			    + "    ,[query_hash] \n"
			    + "    ,max([" + col_SqlText +"])              as [SqlText] \n"
			    + "    ,max([plan_handle])                     as [plan_handle] \n"
//			    + "    ,max([query_plan_hash])                 as [query_plan_hash] \n"
			    + "    ,count(*)                               as [samples__count] \n"
			    + "    ,min([SessionSampleTime])               as [SessionSampleTime__min] \n"
			    + "    ,max([SessionSampleTime])               as [SessionSampleTime__max] \n"
			    + "    ,cast('' as varchar(30))                as [Duration] \n"
//			    + "    ,sum([CmSampleMs])                      as [CmSampleMs__sum] \n"
			    + "    \n"
			    + "    ,cast('' as varchar(512))               as [execution_count__chart] \n"
			    + "    ,sum([execution_count])                 as [execution_count__sum] \n"
			    + "    ,min([creation_time])                   as [creation_time__min] \n"
			    + "    ,max([last_execution_time])             as [last_execution_time__max] \n"
			    + "    \n"
			    + col_total_worker_time__chart            
			    + col_total_worker_time__sum               + col_AvgWorkerTimeUs            
			    + col_total_physical_reads__chart
			    + col_total_physical_reads__sum            + col_AvgPhysicalReads           
			    + col_total_logical_writes__sum            + col_AvgLogicalWrites           
			    + col_total_logical_reads__chart
			    + col_total_logical_reads__sum             + col_AvgLogicalReads            
			    + col_total_clr_time__sum                  + col_AvgClrTimeUs               
			    + col_total_elapsed_time__sum              + col_AvgElapsedTimeUs           
			    + col_total_rows__sum                      + col_AvgRows                    
			    + col_max_dop__chart
			    + col_total_dop__sum                       + col_AvgDop                     
			    + col_max_grant_kb__chart
			    + col_total_grant_kb__sum                  + col_AvgGrantKb                 
			    + col_total_used_grant_kb__sum             + col_AvgUsedGrantKb             
			    + col_total_ideal_grant_kb__sum            + col_AvgIdealGrantKb            
			    + col_total_reserved_threads__sum          + col_AvgReservedThreads         
			    + col_total_used_threads__sum              + col_AvgUsedThreads             
			    + col_total_columnstore_segment_reads__sum + col_AvgColumnstoreSegmentReads 
			    + col_total_columnstore_segment_skips__sum + col_AvgColumnstoreSegmentSkips 
			    + col_total_spills__chart
			    + col_total_spills__sum                    + col_AvgSpills                  
			    + col_total_page_server_reads__sum         + col_AvgPageServerReads         
				+ "from [CmExecQueryStats_diff] \n"
				+ "where [CmNewDiffRateRow] = 0 -- only records that has been diff calculations (not first time seen, when it swaps in/out due to execution every x minute) \n"
				+ "  and [execution_count] > 0 \n"
//				+ "  and [AvgServ_ms] > " + _aboveServiceTime + " \n"
//				+ "  and [TotalIOs]   > " + _aboveTotalIos    + " \n"
				+ getReportPeriodSqlWhere()
				+ "group by [dbname], [query_hash] \n"
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

			// - Get all "plann_handle" in table '_shortRstm'
			// - Get the Execution Plan all the "plann_handle"s
			// - In the table substitute the "plann_handle"s with a link that will display the XML Plan on the HTML Page
			_planCollection = new ExecutionPlanCollection(this, _shortRstm, this.getClass().getSimpleName())
			{
				@Override
				public Object renderCellView(Object val)
				{
//					return "view plan";
					return super.renderCellView(val);
				}
			};
			_planCollection.getPlansAndSubstituteWithLinks(conn, "plan_handle");
			
			// Mini Chart on "..."
			String whereKeyColumn = "dbname, query_hash"; 

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("execution_count__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecQueryStats_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("execution_count")
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Total 'execution_count' in below period")
					.validate()));

			if (StringUtil.hasValue(col_total_worker_time__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("total_worker_time__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecQueryStats_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("total_worker_time")   
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Total 'worker_time' in below period")
					.validate()));
			}

			if (StringUtil.hasValue(col_total_physical_reads__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("total_physical_reads__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecQueryStats_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("total_physical_reads")   
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Number of 'physical_reads' in below period")
					.validate()));
			}

			if (StringUtil.hasValue(col_total_logical_reads__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("total_logical_reads__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecQueryStats_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("total_logical_reads")   
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Number of 'logical_reads' in below period")
					.validate()));
			}

			if (StringUtil.hasValue(col_max_dop__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("max_dop__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecQueryStats_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("CASE WHEN sum([execution_count]) = 0 THEN 0.0 ELSE sum([total_dop]*1.0)/sum([execution_count]*1.0) END").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("MAX 'DOP - Degree Of Paralism' in below period")
					.validate()));
			}

			if (StringUtil.hasValue(col_max_grant_kb__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("max_grant_kb__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecQueryStats_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("CASE WHEN sum([execution_count]) = 0 THEN 0.0 ELSE sum([total_grant_kb]*1.0)/sum([execution_count]*1.0) END").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("MAX 'grant_kb' in below period")
					.validate()));
			}

			if (StringUtil.hasValue(col_total_spills__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("total_spills__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecQueryStats_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("total_spills")
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Number of 'spills' in below period")
					.validate()));
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
