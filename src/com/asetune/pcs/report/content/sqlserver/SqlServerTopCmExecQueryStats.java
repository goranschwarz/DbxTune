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
import java.sql.SQLException;
import java.sql.Types;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.h2.tools.SimpleResultSet;

import com.asetune.cm.CountersModel;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.gui.ResultSetTableModel.TableStringRenderer;
import com.asetune.pcs.DictCompression;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.SparklineHelper;
import com.asetune.pcs.report.content.SparklineHelper.AggType;
import com.asetune.pcs.report.content.SparklineHelper.DataSource;
import com.asetune.pcs.report.content.SparklineHelper.SparkLineParams;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;
import com.asetune.utils.HtmlTableProducer;
import com.asetune.utils.HtmlTableProducer.ColumnCopyDef;
import com.asetune.utils.HtmlTableProducer.ColumnCopyRender;
import com.asetune.utils.HtmlTableProducer.ColumnCopyRow;
import com.asetune.utils.HtmlTableProducer.ColumnStatic;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;

public class SqlServerTopCmExecQueryStats
extends SqlServerAbstract
{
	private static Logger _logger = Logger.getLogger(SqlServerTopCmExecQueryStats.class);

	public enum ReportType
	{
		CPU_TIME, 
		EST_WAIT_TIME,
		TEMPDB_SPILLS,
		LOGICAL_READS,
		LOGICAL_WRITES,
		PHYSICAL_READS,
		EXECUTION_COUNT,
//		RECENTLY_COMPILED,
		MEMORY_GRANTS,
	};
	
	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _sqTextRstm;                  // only used for CPU_TIME, to fill in a "sub" table with ExecTime and SQL Text (easier for mail client)
	private ExecutionPlanCollection _planCollection;
	private List<String>        _miniChartJsList = new ArrayList<>();

	private ReportType _reportType;
	private String _sqlOrderByCol = "-unknown-";
	private String _sqlHaving     = "";

	public SqlServerTopCmExecQueryStats(DailySummaryReportAbstract reportingInstance, ReportType reportType)
	{
		super(reportingInstance);

		_reportType = reportType;

		if      (ReportType.CPU_TIME            .equals(_reportType)) { _sqlOrderByCol = "[total_worker_time_ms__sum]";   _sqlHaving = ""; }
		else if (ReportType.EST_WAIT_TIME       .equals(_reportType)) { _sqlOrderByCol = "[total_est_wait_time_ms__sum]"; _sqlHaving = ""; }
		else if (ReportType.TEMPDB_SPILLS       .equals(_reportType)) { _sqlOrderByCol = "[total_spills__sum]";           _sqlHaving = "having sum([total_spills]) > 0 \n"; }
		else if (ReportType.LOGICAL_READS       .equals(_reportType)) { _sqlOrderByCol = "[total_logical_reads__sum]";    _sqlHaving = ""; }
		else if (ReportType.LOGICAL_WRITES      .equals(_reportType)) { _sqlOrderByCol = "[total_logical_writes__sum]";   _sqlHaving = ""; }
		else if (ReportType.PHYSICAL_READS      .equals(_reportType)) { _sqlOrderByCol = "[total_physical_reads__sum]";   _sqlHaving = ""; }
		else if (ReportType.EXECUTION_COUNT     .equals(_reportType)) { _sqlOrderByCol = "[execution_count__sum]";        _sqlHaving = ""; }
//		else if (ReportType.RECENTLY_COMPILED   .equals(_reportType)) { _sqlOrderByCol = "[]";                            _sqlHaving = ""; }
		else if (ReportType.MEMORY_GRANTS       .equals(_reportType)) { _sqlOrderByCol = "[total_grant_kb__sum]";         _sqlHaving = ""; }
		else throw new IllegalArgumentException("Unhandled reportType='" + reportType + "'.");
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
		if (isFullMessageType())
		{
			// Get a description of this section, and column names
			sb.append(getSectionDescriptionHtml(_shortRstm, true));

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

	//- do the "bold" on some other level in ResultSetTableModel or _shortRstm.toHtmlTableString
	//- tooltip on time from US to MS
//					// Order by values are in bold
//					if (_sqlOrderByCol_noBrackets != null && _sqlOrderByCol_noBrackets.equals(colName))
//					{
//						return "<b>" + strVal + "</b>";
//					}

					return strVal;
				}
			};

			sb.append(_shortRstm.toHtmlTableString("sortable", true, true, null, tableRender));
		}

		// Write HTML/JavaScript Code for the Execution Plan...
		if (isFullMessageType())
		{
			if (_planCollection != null)
				_planCollection.writeMessageText(sb);
		}

		if (_sqTextRstm != null)
		{
//			sb.append("<br>\n");
//			sb.append("SQL Text by queryid, Row Count: " + _sqTextRstm.getRowCount() + " (This is the same SQL Text as the in the above table, but without all counter details).<br>\n");
//			sb.append(toHtmlTable(_sqTextRstm));

			String detailsOpen = ""; // closed is the default
			if (ReportType.CPU_TIME     .equals(_reportType)) detailsOpen = "open";  // open
			if (ReportType.EST_WAIT_TIME.equals(_reportType)) detailsOpen = "";      // closed

			sb.append("<br>\n");
			sb.append("<details " + detailsOpen + "> \n");
			sb.append("<summary>Details for above Statements, including SQL Text (click to collapse) </summary> \n");
			
			sb.append("<br>\n");
			sb.append("SQL Text by 'query_hash', Row Count: " + _sqTextRstm.getRowCount() + "\n");
			sb.append(toHtmlTable(_sqTextRstm));

			sb.append("\n");
			sb.append("</details> \n");
		}

		// Write JavaScript code for CPU SparkLine
		if (isFullMessageType())
		{
			for (String str : _miniChartJsList)
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
		sb.append("-- query_hash:                 ").append( rstm.getValueAsString(row, "query_hash"               ) ).append("\n");
		sb.append("-- SessionSampleTime__min:     ").append( rstm.getValueAsString(row, "SessionSampleTime__min"   ) ).append("\n");
		sb.append("-- SessionSampleTime__max:     ").append( rstm.getValueAsString(row, "SessionSampleTime__max"   ) ).append("\n");
		sb.append("-- Duration:                   ").append( rstm.getValueAsString(row, "Duration"                 ) ).append("\n");

		sb.append("-- creation_time__min:         ").append( rstm.getValueAsString(row, "creation_time__min"       ) ).append("\n");
		sb.append("-- last_execution_time__max:   ").append( rstm.getValueAsString(row, "last_execution_time__max" ) ).append("\n");

		sb.append("-- execution_count__sum:       ").append( nf.format(rstm.getValueAsBigDecimal(row, "execution_count__sum"       ))).append("\n");
		sb.append("-- total_elapsed_time_ms__sum: ").append( nf.format(rstm.getValueAsBigDecimal(row, "total_elapsed_time_ms__sum" ))).append("  (in milli seconds), and in (HH:MM:SS.sss) ").append(TimeUtils.msToTimeStrLong(rstm.getValueAsLong(row, "total_elapsed_time_ms__sum"))).append(" \n");
		sb.append(">> AvgElapsedTimeMs:           ").append( nf.format(rstm.getValueAsBigDecimal(row, "AvgElapsedTimeMs"           ))).append("\n");
		sb.append("-- total_worker_time_ms__sum:  ").append( nf.format(rstm.getValueAsBigDecimal(row, "total_worker_time_ms__sum"  ))).append("  (in milli seconds), and in (HH:MM:SS.sss) ").append(TimeUtils.msToTimeStrLong(rstm.getValueAsLong(row, "total_worker_time_ms__sum"))).append(" \n");
		sb.append(">> AvgWorkerTimeMs:            ").append( nf.format(rstm.getValueAsBigDecimal(row, "AvgWorkerTimeMs"            ))).append("\n");
//		sb.append("-- total_est_wait_time_ms__sum:").append( nf.format(rstm.getValueAsBigDecimal(row, "total_ets_wait_time_ms__sum"))).append("  (in milli seconds), and in (HH:MM:SS.sss) ").append(TimeUtils.msToTimeStrLong(rstm.getValueAsLong(row, "total_wait_time_ms__sum"))).append(" \n");
//		sb.append("-- AvgWaitTimeMs:              ").append( nf.format(rstm.getValueAsBigDecimal(row, "AvgWaitTimeMs"              ))).append("\n");
		sb.append("-- total_physical_reads__sum:  ").append( nf.format(rstm.getValueAsBigDecimal(row, "total_physical_reads__sum"  ))).append("\n");
		sb.append("-- AvgPhysicalReads:           ").append( nf.format(rstm.getValueAsBigDecimal(row, "AvgPhysicalReads"           ))).append("\n");
		sb.append("-- total_logical_writes__sum:  ").append( nf.format(rstm.getValueAsBigDecimal(row, "total_logical_writes__sum"  ))).append("\n");
		sb.append("-- AvgLogicalWrites:           ").append( nf.format(rstm.getValueAsBigDecimal(row, "AvgLogicalWrites"           ))).append("\n");
		sb.append("-- total_logical_reads__sum:   ").append( nf.format(rstm.getValueAsBigDecimal(row, "total_logical_reads__sum"   ))).append("\n");
		sb.append("-- AvgLogicalReads:            ").append( nf.format(rstm.getValueAsBigDecimal(row, "AvgLogicalReads"            ))).append("\n");
		sb.append("-- total_rows__sum:            ").append( nf.format(rstm.getValueAsBigDecimal(row, "total_rows__sum"            ))).append("\n");
		sb.append("-- AvgRows:                    ").append( nf.format(rstm.getValueAsBigDecimal(row, "AvgRows"                    ))).append("\n");
		sb.append("-- total_dop__sum:             ").append( nf.format(rstm.getValueAsBigDecimal(row, "total_dop__sum"             ))).append("\n");
		sb.append("-- AvgDop:                     ").append( nf.format(rstm.getValueAsBigDecimal(row, "AvgDop"                     ))).append("\n");
		sb.append("-- total_grant_kb__sum:        ").append( nf.format(rstm.getValueAsBigDecimal(row, "total_grant_kb__sum"        ))).append("\n");
		sb.append("-- AvgGrantKb:                 ").append( nf.format(rstm.getValueAsBigDecimal(row, "AvgGrantKb"                 ))).append("\n");
		sb.append("-----------------------------------------------------------------------------------------------\n");
		sb.append(StringEscapeUtils.escapeHtml4(rstm.getValueAsString(row, "SqlText")));

		return sb.toString();
	}
	
	@Override
	public String getSubject()
	{
		return "Top SQL Statements - " + _reportType + " (order by: " + _sqlOrderByCol + ", origin: CmExecQueryStats/dm_exec_query_stats)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public List<ReportingIndexEntry> getReportingIndexes()
	{
		List<ReportingIndexEntry> list = new ArrayList<>();
		
		list.add(new ReportingIndexEntry("CmIndexUsage_diff", "DbName", "SchemaName", "TableName", "IndexName"));
		list.add(new ReportingIndexEntry("CmIndexUsage_abs" , "DbName", "SchemaName", "TableName", "IndexName", "SessionSampleTime"));

		list.add(new ReportingIndexEntry("CmIndexOpStat_diff", "DbName", "SchemaName", "TableName", "IndexName"));
		list.add(new ReportingIndexEntry("CmIndexOpStat_abs" , "DbName", "SchemaName", "TableName", "IndexName", "SessionSampleTime"));
		
		return list;
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
				"Top SQL Statements (and it's Query Plan)...  (ordered by: " + _sqlOrderByCol + ") <br>" +
				"<br>" +
				"SqlServer Source table is 'dm_exec_query_stats'. <br>" +
				"PCS Source table is 'CmExecQueryStats_diff'. (PCS = Persistent Counter Store) <br>" +
				"");

		// Columns description
		rstm.setColumnDescription("dbname"                               , "Name of the database");
		rstm.setColumnDescription("query_hash"                           , "Binary hash value calculated on the query and used to identify queries with similar logic. \nYou can use the query hash to determine the aggregate resource usage for queries that differ only by literal values");
		rstm.setColumnDescription("SqlText"                              , "SQL Text typical for the 'query_hash'");
		rstm.setColumnDescription("ExecPlan"                             , "The Execution Plan");
		rstm.setColumnDescription("plan__count"                          , "");
		rstm.setColumnDescription("efficiency_pct"                       , "How efficient is the statement. \n" +
		                                                                   "This is a percentage calculation of how well we have used the CPU for this statement... or more precise: 'total_worker_time_ms__sum' / 'AvgDop' / 'total_elapsed_time_ms__sum' * 100.0 \n" +
		                                                                   "The lower percentage, the more 'waitTime' we have. (it can be physical IO, blocking locks, Waiting to be sceduled or similar) \n" + 
		                                                                   "And if it's a parallel query, where the DOP might be skewed (work is not evenly distributed over the worker threads), the percent will be 'lower'...");

		rstm.setColumnDescription("execution_count__chart"               , "Chart showing 'execution count' in a time period (normally 10 minutes)\nSo you can see when in the period (probably last 24 hours) the statement was executed.");
		rstm.setColumnDescription("execution_count__sum"                 , "Number of times that the plan has been executed.");
		rstm.setColumnDescription("creation_time__min"                   , "When was the plan compiled. The 'oldest' time... min(creation_time)");
		rstm.setColumnDescription("last_execution_time__max"             , "Latest time when it was executed");
                                                                         
		rstm.setColumnDescription("elapsed_time__chart"                  , "Chart showing 'elapsed_time' in a time period (normally 10 minutes)\nSo you can see when in the period (probably last 24 hours) the statements elapsed time.");
		rstm.setColumnDescription("total_elapsed_time_ms__sum"           , "Summary of all 'elapsed_time' for the recorded period ");
		rstm.setColumnDescription("AvgElapsedTimeMs"                     , "Average 'elapsed_time' per execution.");
		                                                                 
		rstm.setColumnDescription("worker_time__chart"                   , "Chart showing 'worker_time' in a time period (normally 10 minutes)\nSo you can see when in the period (probably last 24 hours) the statements used CPU time.");
		rstm.setColumnDescription("total_worker_time_ms__sum"            , "Actual CPU spent on this statement in the recording period.");
		rstm.setColumnDescription("AvgWorkerTimeMs"                      , "Average 'worker_time' per execution.");
		                                                                 
		rstm.setColumnDescription("est_wait_time__chart"                 , "Chart showing 'wait_time' in a time period (normally 10 minutes)\nSo you can see when in the period (probably last 24 hours) what the statements was waiting for 'stuff'.");
		rstm.setColumnDescription("total_est_wait_time_ms__sum"          , "Summary of all 'wait_time' for this statement in the recording period.");
		rstm.setColumnDescription("AvgEstWaitTimeMs"                     , "Average 'wait_time' per execution.");
		                                                                 
		rstm.setColumnDescription("physical_reads__chart"                , "Chart showing 'physical_reads' in a time period (normally 10 minutes)\nSo you can see when in the period (probably last 24 hours) the statements was doing physical_reads.");
		rstm.setColumnDescription("total_physical_reads__sum"            , "Summary of all 'physical_reads' for this statement in the recording period.");
		rstm.setColumnDescription("AvgPhysicalReads"                     , "Average 'physical_reads' per execution.");
		                                                                 
		rstm.setColumnDescription("physical_reads_mb__chart"              , "Chart showing 'physical_reads_mb' in a time period (normally 10 minutes)\nSo you can see when in the period (probably last 24 hours) the statements was doing physical_reads_mb. (this is the same as 'physical_reads' / 128.0 ");
		rstm.setColumnDescription("total_physical_reads_mb__sum"          , "Summary of all 'physical_reads_mb' for this statement in the recording period.");
		rstm.setColumnDescription("AvgPhysicalReadsMb"                    , "Average 'physical_reads_mb' per execution.");
		                                                                 
		rstm.setColumnDescription("logical_writes__chart"                , "Chart showing 'logical_writes' in a time period (normally 10 minutes)\nSo you can see when in the period (probably last 24 hours) the statements was doing logical_writes.");
		rstm.setColumnDescription("total_logical_writes__sum"            , "Summary of all 'logical_writes' for this statement in the recording period.");
		rstm.setColumnDescription("AvgLogicalWrites"                     , "Average 'logical_writes' per execution.");
		                                                                 
		rstm.setColumnDescription("logical_writes_mb__chart"              , "Chart showing 'logical_writes_mb' in a time period (normally 10 minutes)\nSo you can see when in the period (probably last 24 hours) the statements was doing logical_writes_mb. (this is the same as 'logical_writes' / 128.0 ");
		rstm.setColumnDescription("total_logical_writes_mb__sum"          , "Summary of all 'logical_writes_mb' for this statement in the recording period.");
		rstm.setColumnDescription("AvgLogicalWritesMb"                    , "Average 'logical_writes_mb' per execution.");
		                                                                 
		rstm.setColumnDescription("logical_reads__chart"                 , "Chart showing 'logical_reads' in a time period (normally 10 minutes)\nSo you can see when in the period (probably last 24 hours) the statements was doing logical_reads.");
		rstm.setColumnDescription("total_logical_reads__sum"             , "Summary of all 'logical_reads' for this statement in the recording period.");
		rstm.setColumnDescription("AvgLogicalReads"                      , "Average 'logical_reads' per execution.");
		                                                                 
		rstm.setColumnDescription("logical_reads_mb__chart"              , "Chart showing 'logical_reads_mb' in a time period (normally 10 minutes)\nSo you can see when in the period (probably last 24 hours) the statements was doing logical_reads_mb. (this is the same as 'logical_reads' / 128.0 ");
		rstm.setColumnDescription("total_logical_reads_mb__sum"          , "Summary of all 'logical_reads_mb' for this statement in the recording period.");
		rstm.setColumnDescription("AvgLogicalReadsMb"                    , "Average 'logical_reads_mb' per execution.");
		                                                                 
		rstm.setColumnDescription("clr_time__chart"                      , "Chart showing 'clr_time' in a time period (normally 10 minutes)\nSo you can see when in the period (probably last 24 hours) the statements was doing clr_time.");
		rstm.setColumnDescription("total_clr_time_ms__sum"               , "Summary of all 'clr_time' for this statement in the recording period.");
		rstm.setColumnDescription("AvgClrTimeMs"                         , "Average 'clr_time' per execution.");
		                                                                 
		rstm.setColumnDescription("rows__chart"                          , "Chart showing 'rows affected' in a time period (normally 10 minutes)\nSo you can see when in the period (probably last 24 hours) the statements was returning or ins/upd/del rows.");
		rstm.setColumnDescription("total_rows__sum"                      , "Summary of all 'rows affected' for this statement in the recording period.");
		rstm.setColumnDescription("AvgRows"                              , "Average 'rows affected' per execution.");
		                                                                 
		rstm.setColumnDescription("dop__chart"                           , "Chart showing 'dop' in a time period (normally 10 minutes)\nSo you can see when in the period (probably last 24 hours) the statements was doing work in parallel, with the number of workers.");
		rstm.setColumnDescription("total_dop__sum"                       , "Summary of all 'dop' for this statement in the recording period.");
		rstm.setColumnDescription("AvgDop"                               , "Average 'dop' per execution.");
		                                                                 
		rstm.setColumnDescription("grant_kb__chart"                      , "Chart showing 'grant_kb' in a time period (normally 10 minutes)\nSo you can see when in the period (probably last 24 hours) the statements was requesting memory grants, and how large they were.");
		rstm.setColumnDescription("total_grant_kb__sum"                  , "Summary of all 'grant_kb' for this statement in the recording period.");
		rstm.setColumnDescription("AvgGrantKb"                           , "Average 'grant_kb' per execution.");
		                                                                 
		rstm.setColumnDescription("total_grant_mb__sum"                  , "Summary of all 'grant_mb' for this statement in the recording period.");
		rstm.setColumnDescription("AvgGrantMb"                           , "Average 'grant_mb' per execution.");
		                                                                 
		rstm.setColumnDescription("total_used_grant_kb__sum"             , "Summary of all 'used_grant_kb' for this statement in the recording period.");
		rstm.setColumnDescription("AvgUsedGrantKb"                       , "Average 'used_grant_kb' per execution.");
		                                                                 
		rstm.setColumnDescription("total_ideal_grant_kb__sum"            , "Summary of all 'ideal_grant_kb' for this statement in the recording period.");
		rstm.setColumnDescription("AvgIdealGrantKb"                      , "Average 'ideal_grant_kb' per execution.");
		                                                                 
		rstm.setColumnDescription("total_reserved_threads__sum"          , "Summary of all 'reserved_threads' for this statement in the recording period.");
		rstm.setColumnDescription("AvgReservedThreads"                   , "Average 'reserved_threads' per execution.");
		                                                                 
		rstm.setColumnDescription("total_used_threads__sum"              , "Summary of all 'used_threads' for this statement in the recording period.");
		rstm.setColumnDescription("AvgUsedThreads"                       , "Average 'used_threads' per execution.");
		
		rstm.setColumnDescription("total_columnstore_segment_reads__sum" , "Summary of all 'columnstore_segment_reads' for this statement in the recording period.");
		rstm.setColumnDescription("AvgColumnstoreSegmentReads"           , "Average 'columnstore_segment_reads' per execution.");
		
		rstm.setColumnDescription("total_columnstore_segment_skips__sum" , "Summary of all 'columnstore_segment_skips' for this statement in the recording period.");
		rstm.setColumnDescription("AvgColumnstoreSegmentSkips"           , "Average 'columnstore_segment_skips' per execution.");
		
		rstm.setColumnDescription("spills__chart"                        , "Chart showing 'spills' to tempdb in a time period (normally 10 minutes)\nSo you can see when in the period (probably last 24 hours) the statements was spiiling to tempdb.");
		rstm.setColumnDescription("total_spills__sum"                    , "Summary of all 'spills' for this statement in the recording period.");
		rstm.setColumnDescription("AvgSpills"                            , "Average 'spills' per execution.");
		
		rstm.setColumnDescription("total_page_server_reads__sum"         , "Summary of all 'grant_kb' for this statement in the recording period.");
		rstm.setColumnDescription("AvgPageServerReads"                   , "Average 'grant_kb' per execution.");
		
		rstm.setColumnDescription("plan_handle"                          , "The 'id' of the execution plan.");
		rstm.setColumnDescription("samples__count"                       , "Number of records for this 'query_hash' that was found in the PCS (Persistent Counter Storage)");
		rstm.setColumnDescription("SessionSampleTime__min"               , "First record found in the PCS (Persistent Counter Storage) for this 'query_hash'.");
		rstm.setColumnDescription("SessionSampleTime__max"               , "Last record found in the PCS (Persistent Counter Storage) for this 'query_hash'.");
		rstm.setColumnDescription("Duration"                             , "Number of HH:MM:SS this 'query_hash'... or: 'SessionSampleTime__min' - 'SessionSampleTime__max'");
		rstm.setColumnDescription("newDiffRow_sum"                       , "");
	}
//	TODO; // do same "stuff" for Query Store 
//	TODO; // also add section for "efficiency_pct - sort-on-low-pct"
//	TODO; // also add section for "parallel -- efficiency_pct - sort-on-low-pct"
//	TODO; // look at how we can do "plan_count" better...

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

		// DO NOT TRUST: new data that hasn't yet been DIFF Calculated (it only has 1 sample, so it's probably Asolute values, which are *to high*)
		// If we would trust the above values, it will/may create statistical problems (showing to high values in specific periods)
		boolean skipNewDiffRateRows    = localConf.getBooleanProperty(this.getClass().getSimpleName()+".skipNewDiffRateRows", true);

		//  SQL for: only records that has been diff calculations (not first time seen, some ASE Versions has a bug that do not clear counters on reuse)
		String sql_and_skipNewOrDiffRateRows = "  and [CmNewDiffRateRow] = 0 \n"; // This is the "old" way... and used for backward compatibility
//		String sql_and_onlyNewOrDiffRateRows = "  and [CmNewDiffRateRow] = 1 \n"; // This is the "old" way... and used for backward compatibility
//		String col_newDiffRow_sum            = " ,sum([CmNewDiffRateRow])     as [newDiffRow_sum] \n";
		if (dummyRstm.hasColumn("CmRowState")) // New column name for 'CmNewDiffRateRow' (which is a bitwise state column)
		{
			// the below will produce for H2:     and  BITAND([CmRowState], 1) = ???   
			//                        for OTHERS: and  ([CmRowState] & 1) = ???
			sql_and_skipNewOrDiffRateRows = "  and " + conn.toBitAnd("[CmRowState]", CountersModel.ROW_STATE__IS_DIFF_OR_RATE_ROW) + " = 0 \n";
//			sql_and_onlyNewOrDiffRateRows = "  and " + conn.toBitAnd("[CmRowState]", CountersModel.ROW_STATE__IS_DIFF_OR_RATE_ROW) + " = " + CountersModel.ROW_STATE__IS_DIFF_OR_RATE_ROW + " \n";
//			col_newDiffRow_sum = " ,sum(" + conn.toBitAnd("[CmRowState]", CountersModel.ROW_STATE__IS_DIFF_OR_RATE_ROW) + ")     as [newDiffRow_sum] \n";
		}
//FIXME; double check the code for "CmNewDiffRateRow and CmRowState"

		// Used by "sparkline" charts to filter out "new diff/rate" rows
		String whereFilter_skipNewDiffRateRows = !skipNewDiffRateRows ? "" : sql_and_skipNewOrDiffRateRows;
		
//		TODO; // add cpu_efficiency_pct = total_worker_time / avg_dop / total_elapsed_time * 100.0
//		TODO; // same thing for Query Store
//		TODO; // Add one extra REPORT_TYPE for "INEFFICIENT_CPU" -- for "any statements"
//		TODO; // Add one extra REPORT_TYPE for "INEFFICIENT_DOP" -- Only where POD > 1
		
		String col_efficiency_pct = "";
		if (dummyRstm.hasColumnNoCase("total_worker_time") && dummyRstm.hasColumnNoCase("total_dop") && dummyRstm.hasColumnNoCase("total_elapsed_time"))
			col_efficiency_pct = "    ,cast(sum([total_worker_time]) / (sum([total_dop]) * 1.0 / nullif(sum([execution_count]), 0)) / sum([total_elapsed_time]) * 100.0 as numeric(9,1)) as [efficiency_pct] \n";

		String col_total_elapsed_time_ms__sum           = !dummyRstm.hasColumnNoCase("total_elapsed_time"             ) ? "" : "    ,sum([total_elapsed_time]/1000.0)       as [total_elapsed_time_ms__sum]           \n"; 
		String col_total_worker_time_ms__sum            = !dummyRstm.hasColumnNoCase("total_worker_time"              ) ? "" : "    ,sum([total_worker_time]/1000.0)        as [total_worker_time_ms__sum]            \n"; 
		String col_total_est_wait_time_ms__sum          = !dummyRstm.hasColumnNoCase("total_elapsed_time"             ) ? "" : "    ,sum([total_elapsed_time]/1000.0) - sum([total_worker_time]/1000.0) as [total_est_wait_time_ms__sum]   \n"; 
		String col_total_physical_reads__sum            = !dummyRstm.hasColumnNoCase("total_physical_reads"           ) ? "" : "    ,sum([total_physical_reads])            as [total_physical_reads__sum]            \n"; 
		String col_total_physical_reads_mb__sum         = !dummyRstm.hasColumnNoCase("total_physical_reads"           ) ? "" : "    ,cast (sum([total_physical_reads]) / 128 as bigint) as [total_physical_reads_mb__sum]        \n"; // h2 bigint/int seems to produce decimal values...
		String col_total_logical_writes__sum            = !dummyRstm.hasColumnNoCase("total_logical_writes"           ) ? "" : "    ,sum([total_logical_writes])            as [total_logical_writes__sum]            \n"; 
		String col_total_logical_writes_mb__sum         = !dummyRstm.hasColumnNoCase("total_logical_writes"           ) ? "" : "    ,cast (sum([total_logical_writes]) / 128 as bigint) as [total_logical_writes_mb__sum]        \n"; // h2 bigint/int seems to produce decimal values...
		String col_total_logical_reads__sum             = !dummyRstm.hasColumnNoCase("total_logical_reads"            ) ? "" : "    ,sum([total_logical_reads])             as [total_logical_reads__sum]             \n"; 
		String col_total_logical_reads_mb__sum          = !dummyRstm.hasColumnNoCase("total_logical_reads"            ) ? "" : "    ,cast (sum([total_logical_reads]) / 128 as bigint) as [total_logical_reads_mb__sum]          \n"; // h2 bigint/int seems to produce decimal values...
		String col_total_clr_time_ms__sum               = !dummyRstm.hasColumnNoCase("total_clr_time"                 ) ? "" : "    ,sum([total_clr_time]/1000.0)           as [total_clr_time_ms__sum]               \n"; 
		String col_total_rows__sum                      = !dummyRstm.hasColumnNoCase("total_rows"                     ) ? "" : "    ,sum([total_rows])                      as [total_rows__sum]                      \n"; 
		String col_total_dop__sum                       = !dummyRstm.hasColumnNoCase("total_dop"                      ) ? "" : "    ,sum([total_dop])                       as [total_dop__sum]                       \n"; 
		String col_total_grant_kb__sum                  = !dummyRstm.hasColumnNoCase("total_grant_kb"                 ) ? "" : "    ,sum([total_grant_kb])                  as [total_grant_kb__sum]                  \n"; 
		String col_total_grant_mb__sum                  = !dummyRstm.hasColumnNoCase("total_grant_kb"                 ) ? "" : "    ,sum([total_grant_kb]/1024.0)           as [total_grant_mb__sum]                  \n"; 
		String col_total_used_grant_kb__sum             = !dummyRstm.hasColumnNoCase("total_used_grant_kb"            ) ? "" : "    ,sum([total_used_grant_kb])             as [total_used_grant_kb__sum]             \n"; 
		String col_total_ideal_grant_kb__sum            = !dummyRstm.hasColumnNoCase("total_ideal_grant_kb"           ) ? "" : "    ,sum([total_ideal_grant_kb])            as [total_ideal_grant_kb__sum]            \n"; 
		String col_total_reserved_threads__sum          = !dummyRstm.hasColumnNoCase("total_reserved_threads"         ) ? "" : "    ,sum([total_reserved_threads])          as [total_reserved_threads__sum]          \n"; 
		String col_total_used_threads__sum              = !dummyRstm.hasColumnNoCase("total_used_threads"             ) ? "" : "    ,sum([total_used_threads])              as [total_used_threads__sum]              \n"; 
		String col_total_columnstore_segment_reads__sum = !dummyRstm.hasColumnNoCase("total_columnstore_segment_reads") ? "" : "    ,sum([total_columnstore_segment_reads]) as [total_columnstore_segment_reads__sum] \n"; 
		String col_total_columnstore_segment_skips__sum = !dummyRstm.hasColumnNoCase("total_columnstore_segment_skips") ? "" : "    ,sum([total_columnstore_segment_skips]) as [total_columnstore_segment_skips__sum] \n"; 
		String col_total_spills__sum                    = !dummyRstm.hasColumnNoCase("total_spills"                   ) ? "" : "    ,sum([total_spills])                    as [total_spills__sum]                    \n"; 
		String col_total_page_server_reads__sum         = !dummyRstm.hasColumnNoCase("total_page_server_reads"        ) ? "" : "    ,sum([total_page_server_reads])         as [total_page_server_reads__sum]         \n"; 

//		String col_AvgWorkerTimeUs                      = !dummyRstm.hasColumnNoCase("total_worker_time"              ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgWorkerTimeUs]            \n";
//		String col_AvgWaitTimeUs                        = !dummyRstm.hasColumnNoCase("total_elapsed_time"             ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgWaitTimeUs]              \n";
//		String col_AvgPhysicalReads                     = !dummyRstm.hasColumnNoCase("total_physical_reads"           ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgPhysicalReads]           \n";
//		String col_AvgLogicalWrites                     = !dummyRstm.hasColumnNoCase("total_logical_writes"           ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgLogicalWrites]           \n";
//		String col_AvgLogicalReads                      = !dummyRstm.hasColumnNoCase("total_logical_reads"            ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgLogicalReads]            \n";
//		String col_AvgClrTimeUs                         = !dummyRstm.hasColumnNoCase("total_clr_time"                 ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgClrTimeUs]               \n";
//		String col_AvgElapsedTimeUs                     = !dummyRstm.hasColumnNoCase("total_elapsed_time"             ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgElapsedTimeUs]           \n";
//		String col_AvgRows                              = !dummyRstm.hasColumnNoCase("total_rows"                     ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgRows]                    \n";
//		String col_AvgDop                               = !dummyRstm.hasColumnNoCase("total_dop"                      ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgDop]                     \n";
//		String col_AvgGrantKb                           = !dummyRstm.hasColumnNoCase("total_grant_kb"                 ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgGrantKb]                 \n";
//		String col_AvgUsedGrantKb                       = !dummyRstm.hasColumnNoCase("total_used_grant_kb"            ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgUsedGrantKb]             \n";
//		String col_AvgIdealGrantKb                      = !dummyRstm.hasColumnNoCase("total_ideal_grant_kb"           ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgIdealGrantKb]            \n";
//		String col_AvgReservedThreads                   = !dummyRstm.hasColumnNoCase("total_reserved_threads"         ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgReservedThreads]         \n";
//		String col_AvgUsedThreads                       = !dummyRstm.hasColumnNoCase("total_used_threads"             ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgUsedThreads]             \n";
//		String col_AvgColumnstoreSegmentReads           = !dummyRstm.hasColumnNoCase("total_columnstore_segment_reads") ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgColumnstoreSegmentReads] \n"; 
//		String col_AvgColumnstoreSegmentSkips           = !dummyRstm.hasColumnNoCase("total_columnstore_segment_skips") ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgColumnstoreSegmentSkips] \n"; 
//		String col_AvgSpills                            = !dummyRstm.hasColumnNoCase("total_spills"                   ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgSpills]                  \n";
//		String col_AvgPageServerReads                   = !dummyRstm.hasColumnNoCase("total_page_server_reads"        ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgPageServerReads]         \n"; 

		String col_AvgElapsedTimeMs                     = !dummyRstm.hasColumnNoCase("total_elapsed_time"             ) ? "" : "    ,cast( sum([total_elapsed_time]/1000.0)                      * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgElapsedTimeMs]           \n";
		String col_AvgWorkerTimeMs                      = !dummyRstm.hasColumnNoCase("total_worker_time"              ) ? "" : "    ,cast( sum([total_worker_time]/1000.0)                       * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgWorkerTimeMs]            \n";
		String col_AvgEstWaitTimeMs                     = !dummyRstm.hasColumnNoCase("total_elapsed_time"             ) ? "" : "    ,cast((sum([total_elapsed_time]/1000.0) - sum([total_worker_time]/1000.0)) * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgEstWaitTimeMs]              \n";
		String col_AvgPhysicalReads                     = !dummyRstm.hasColumnNoCase("total_physical_reads"           ) ? "" : "    ,cast( sum([total_physical_reads])                           * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgPhysicalReads]           \n";
		String col_AvgPhysicalReadsMb                   = !dummyRstm.hasColumnNoCase("total_physical_reads"           ) ? "" : "    ,cast( sum([total_physical_reads]) / 128.0                   * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgPhysicalReadsMb]         \n";
		String col_AvgLogicalWrites                     = !dummyRstm.hasColumnNoCase("total_logical_writes"           ) ? "" : "    ,cast( sum([total_logical_writes])                           * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgLogicalWrites]           \n";
		String col_AvgLogicalWritesMb                   = !dummyRstm.hasColumnNoCase("total_logical_writes"           ) ? "" : "    ,cast( sum([total_logical_writes]) / 128.0                   * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgLogicalWritesMb]         \n";
		String col_AvgLogicalReads                      = !dummyRstm.hasColumnNoCase("total_logical_reads"            ) ? "" : "    ,cast( sum([total_logical_reads])                            * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgLogicalReads]            \n";
		String col_AvgLogicalReadsMb                    = !dummyRstm.hasColumnNoCase("total_logical_reads"            ) ? "" : "    ,cast( sum([total_logical_reads]) / 128.0                    * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgLogicalReadsMb]          \n";
		String col_AvgClrTimeMs                         = !dummyRstm.hasColumnNoCase("total_clr_time"                 ) ? "" : "    ,cast( sum([total_clr_time]/1000.0)                          * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgClrTimeMs]               \n";
		String col_AvgRows                              = !dummyRstm.hasColumnNoCase("total_rows"                     ) ? "" : "    ,cast( sum([total_rows])                                     * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgRows]                    \n";
		String col_AvgDop                               = !dummyRstm.hasColumnNoCase("total_dop"                      ) ? "" : "    ,cast( sum([total_dop])                                      * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgDop]                     \n";
		String col_AvgGrantKb                           = !dummyRstm.hasColumnNoCase("total_grant_kb"                 ) ? "" : "    ,cast( sum([total_grant_kb])                                 * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgGrantKb]                 \n";
		String col_AvgGrantMb                           = !dummyRstm.hasColumnNoCase("total_grant_kb"                 ) ? "" : "    ,cast( sum([total_grant_kb]/1024.0)                          * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgGrantMb]                 \n";
		String col_AvgUsedGrantKb                       = !dummyRstm.hasColumnNoCase("total_used_grant_kb"            ) ? "" : "    ,cast( sum([total_used_grant_kb])                            * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgUsedGrantKb]             \n";
		String col_AvgIdealGrantKb                      = !dummyRstm.hasColumnNoCase("total_ideal_grant_kb"           ) ? "" : "    ,cast( sum([total_ideal_grant_kb])                           * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgIdealGrantKb]            \n";
		String col_AvgReservedThreads                   = !dummyRstm.hasColumnNoCase("total_reserved_threads"         ) ? "" : "    ,cast( sum([total_reserved_threads])                         * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgReservedThreads]         \n";
		String col_AvgUsedThreads                       = !dummyRstm.hasColumnNoCase("total_used_threads"             ) ? "" : "    ,cast( sum([total_used_threads])                             * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgUsedThreads]             \n";
		String col_AvgColumnstoreSegmentReads           = !dummyRstm.hasColumnNoCase("total_columnstore_segment_reads") ? "" : "    ,cast( sum([total_columnstore_segment_reads])                * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgColumnstoreSegmentReads] \n"; 
		String col_AvgColumnstoreSegmentSkips           = !dummyRstm.hasColumnNoCase("total_columnstore_segment_skips") ? "" : "    ,cast( sum([total_columnstore_segment_skips])                * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgColumnstoreSegmentSkips] \n"; 
		String col_AvgSpills                            = !dummyRstm.hasColumnNoCase("total_spills"                   ) ? "" : "    ,cast( sum([total_spills])                                   * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgSpills]                  \n";
		String col_AvgPageServerReads                   = !dummyRstm.hasColumnNoCase("total_page_server_reads"        ) ? "" : "    ,cast( sum([total_page_server_reads])                        * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgPageServerReads]         \n"; 

		String col_elapsed_time__chart                  = !dummyRstm.hasColumnNoCase("total_elapsed_time"             ) ? "" : "    ,cast('' as varchar(512))  as [elapsed_time__chart]   \n"; 
		String col_worker_time__chart                   = !dummyRstm.hasColumnNoCase("total_worker_time"              ) ? "" : "    ,cast('' as varchar(512))  as [worker_time__chart]    \n"; 
		String col_est_wait_time__chart                 = !dummyRstm.hasColumnNoCase("total_elapsed_time"             ) ? "" : "    ,cast('' as varchar(512))  as [est_wait_time__chart]  \n"; 
		String col_physical_reads__chart                = !dummyRstm.hasColumnNoCase("total_physical_reads"           ) ? "" : "    ,cast('' as varchar(512))  as [physical_reads__chart] \n"; 
		String col_physical_reads_mb__chart             = !dummyRstm.hasColumnNoCase("total_physical_reads"           ) ? "" : "    ,cast('' as varchar(512))  as [physical_reads_mb__chart] \n"; 
		String col_logical_reads__chart                 = !dummyRstm.hasColumnNoCase("total_logical_reads"            ) ? "" : "    ,cast('' as varchar(512))  as [logical_reads__chart]  \n"; 
		String col_logical_reads_mb__chart              = !dummyRstm.hasColumnNoCase("total_logical_reads"            ) ? "" : "    ,cast('' as varchar(512))  as [logical_reads_mb__chart] \n"; 
		String col_logical_writes__chart                = !dummyRstm.hasColumnNoCase("total_logical_writes"           ) ? "" : "    ,cast('' as varchar(512))  as [logical_writes__chart] \n"; 
		String col_logical_writes_mb__chart             = !dummyRstm.hasColumnNoCase("total_logical_writes"           ) ? "" : "    ,cast('' as varchar(512))  as [logical_writes_mb__chart] \n"; 
		String col_clr_time__chart                      = !dummyRstm.hasColumnNoCase("total_clr_time"                 ) ? "" : "    ,cast('' as varchar(512))  as [clr_time__chart]       \n";
		String col_rows__chart                          = !dummyRstm.hasColumnNoCase("total_rows"                     ) ? "" : "    ,cast('' as varchar(512))  as [rows__chart]           \n"; 
		String col_dop__chart                           = !dummyRstm.hasColumnNoCase("total_dop"                      ) ? "" : "    ,cast('' as varchar(512))  as [dop__chart]            \n"; 
		String col_grant_kb__chart                      = !dummyRstm.hasColumnNoCase("total_grant_kb"                 ) ? "" : "    ,cast('' as varchar(512))  as [grant_kb__chart]       \n"; 
		String col_spills__chart                        = !dummyRstm.hasColumnNoCase("total_spills"                   ) ? "" : "    ,cast('' as varchar(512))  as [spills__chart]         \n"; 

		// Reset "HAVING ..." is SQL if we cant find some columns
		if (!dummyRstm.hasColumnNoCase("total_spills"))
			_sqlHaving = "";

		int topRows = getTopRows();

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


//FIXME; group by 'query_hash' or 'query_plan_hash' ... also check that we pick up the correct PLAN in below table
//Possibly; add link directly in the table for 'plan_handle' instead of the below table!!!

		String sql = getCmDiffColumnsAsSqlComment("CmActiveStatements")
			    + "select top " + topRows + " \n"
			    + "     [dbname] \n"
			    + "    ,[query_hash] \n"
			    + "    ,max([" + col_SqlText +"])              as [SqlText] \n"
			    + "    ,cast('' as varchar(30))                as [ExecPlan] \n"
			    + "    \n"
			    + "    ,count(DISTINCT [query_plan_hash])      as [plan_hash__count] \n" 
			    + "    ,count(DISTINCT [plan_handle])          as [plan_handle__count] \n" 
//			    + "    ,count([query_plan_hash])               as [plan__count] \n" 
//			    + "    ,count(DISTINCT [query_plan_hash])      as [distinct_plan__count] \n" 
			    // or should the above be be:
			    		// number_of_plans     = COUNT_BIG(qs.query_plan_hash)          // from https://github.com/BrentOzarULTD/SQL-Server-First-Responder-Kit/blob/dev/sp_BlitzCache.sql
			    		// distinct_plan_count = COUNT_BIG(DISTINCT qs.query_plan_hash) // from https://github.com/BrentOzarULTD/SQL-Server-First-Responder-Kit/blob/dev/sp_BlitzCache.sql
			    + "    \n"
			    + col_efficiency_pct
			    + "    ,cast('' as varchar(512))               as [execution_count__chart] \n"
			    + "    ,sum([execution_count])                 as [execution_count__sum] \n"
			    + "    ,min([creation_time])                   as [creation_time__min] \n"
			    + "    ,max([last_execution_time])             as [last_execution_time__max] \n"
			    + "    \n"
			    + col_elapsed_time__chart
			    + col_total_elapsed_time_ms__sum           + col_AvgElapsedTimeMs           

			    + col_worker_time__chart            
			    + col_total_worker_time_ms__sum            + col_AvgWorkerTimeMs

			    + col_est_wait_time__chart
			    + col_total_est_wait_time_ms__sum          + col_AvgEstWaitTimeMs            

			    + col_physical_reads__chart
			    + col_total_physical_reads__sum            + col_AvgPhysicalReads           

			    + col_physical_reads_mb__chart
			    + col_total_physical_reads_mb__sum         + col_AvgPhysicalReadsMb           

			    + col_logical_writes__chart
			    + col_total_logical_writes__sum            + col_AvgLogicalWrites           

			    + col_logical_writes_mb__chart
			    + col_total_logical_writes_mb__sum         + col_AvgLogicalWritesMb           

			    + col_logical_reads__chart
			    + col_total_logical_reads__sum             + col_AvgLogicalReads            

			    + col_logical_reads_mb__chart
			    + col_total_logical_reads_mb__sum          + col_AvgLogicalReadsMb            

			    + col_clr_time__chart
			    + col_total_clr_time_ms__sum               + col_AvgClrTimeMs               

			    + col_rows__chart
//			    + col_avg_rows__chart
			    + col_total_rows__sum                      + col_AvgRows                    

			    + col_dop__chart
			    + col_total_dop__sum                       + col_AvgDop                     

			    + col_grant_kb__chart
			    + col_total_grant_kb__sum                  + col_AvgGrantKb                 
			    + col_total_grant_mb__sum                  + col_AvgGrantMb
			    + col_total_used_grant_kb__sum             + col_AvgUsedGrantKb             
			    + col_total_ideal_grant_kb__sum            + col_AvgIdealGrantKb            

			    + col_total_reserved_threads__sum          + col_AvgReservedThreads         
			    + col_total_used_threads__sum              + col_AvgUsedThreads             
			    + col_total_columnstore_segment_reads__sum + col_AvgColumnstoreSegmentReads 
			    + col_total_columnstore_segment_skips__sum + col_AvgColumnstoreSegmentSkips 

			    + col_spills__chart
			    + col_total_spills__sum                    + col_AvgSpills                  

			    + col_total_page_server_reads__sum         + col_AvgPageServerReads         

			    + "    ,max([plan_handle])                     as [plan_handle] \n"
			    + "    ,count(*)                               as [samples__count] \n"
			    + "    ,min([SessionSampleTime])               as [SessionSampleTime__min] \n"
			    + "    ,max([SessionSampleTime])               as [SessionSampleTime__max] \n"
			    + "    ,cast('' as varchar(30))                as [Duration] \n"
//				+ col_newDiffRow_sum

				+ "from [CmExecQueryStats_diff] \n"
				+ "where 1 = 1 \n"
				+ sql_and_skipNewOrDiffRateRows // [CmNewDiffRateRow] = 0 -- only records that has been diff calculations (not first time seen, when it swaps in/out due to execution every x minute)
				+ "  and [execution_count] > 0 \n"
//				+ "  and [AvgServ_ms] > " + _aboveServiceTime + " \n"
//				+ "  and [TotalIOs]   > " + _aboveTotalIos    + " \n"
				+ getReportPeriodSqlWhere()
				+ "group by [dbname], [query_hash] \n"
				+ _sqlHaving
				+ "order by " + _sqlOrderByCol + " desc \n"
			    + "";

		// For some REPORT TYPES, we need specififc columns... if not found... simply DO NOT RUN!
		String noNeedToExecMessage = "";
		if ( ReportType.TEMPDB_SPILLS.equals(_reportType) && !dummyRstm.hasColumnNoCase("total_spills")   )  noNeedToExecMessage = "Column 'total_spills' was introduced in SQL Server 2016 SP2 and 2017. The sampled data does NOT have this column.";
		if ( ReportType.MEMORY_GRANTS.equals(_reportType) && !dummyRstm.hasColumnNoCase("total_grant_kb") )  noNeedToExecMessage = "Column 'total_grant_kb' was introduced in SQL Server 2016. The sampled data does NOT have this column.";

		// Execute or NOT
		if (StringUtil.hasValue(noNeedToExecMessage))
			setProblemMsg(noNeedToExecMessage);
		else
			_shortRstm = executeQuery(conn, sql, false, "TopCmExecQueryStats");
		
		// FAILURE or OK
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("TopCmExecQueryStats");
			return;
		}
		else
		{
			// Highlight sort column
			String sqlOrderByCol_noBrackets = _sqlOrderByCol.replace("[", "").replace("]", "");
			_shortRstm.setHighlightSortColumns(sqlOrderByCol_noBrackets);


			// Describe the table
			setSectionDescription(_shortRstm);

			setDurationColumn(_shortRstm, "SessionSampleTime__min", "SessionSampleTime__max", "Duration");

//			calculateAvg(_shortRstm);

			// get Dictionary Compressed values for column: SqlText
			if (hasDictCompCols)
			{
				updateDictionaryCompressedColumn(_shortRstm, conn, null, "CmExecQueryStats", "SqlText", null);
			}

			// - Get all "plann_handle" in table '_shortRstm'
			// - Get the Execution Plan all the "plann_handle"s
			// - In the table substitute the "plann_handle"s with a link that will display the XML Plan on the HTML Page
			_planCollection = new ExecutionPlanCollection(this, _shortRstm, this.getClass().getSimpleName() + "_" + _reportType);
//			_planCollection = new ExecutionPlanCollection(this, _shortRstm, this.getClass().getSimpleName() + "_" + _reportType)
//			{
//				@Override
//				public Object renderCellView(Object val)
//				{
////					return "view plan";
//					return super.renderCellView(val);
//				}
//			};
			// Get the plans
			_planCollection.getPlans(conn, null, "plan_handle");

			// Fill in a link with 'ExecPlan' to show the plan.
			// If no plans was found substitute to: '--not-found--'
//			_planCollection.getPlansAndSubstituteWithLinks(conn, null, "plan_handle", "ExecPlan", "view plan", "--not-found--");
			_planCollection.substituteWithLinks(null, "plan_handle", "ExecPlan", "view plan", "--not-found--");

			// Fill in a link with 'plan_handle' to show the plan.
//			_planCollection.getPlansAndSubstituteWithLinks(conn, null, "plan_handle");
			_planCollection.substituteWithLinks(null, "plan_handle");


			//----------------------------------------------------
			// Mini Chart on "..."
			//----------------------------------------------------
//			String whereKeyColumn = "dbname, query_hash"; 
			String whereKeyColumn = "query_hash, dbname";

//			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
//					SparkLineParams.create       (DataSource.CounterModel)
//					.setHtmlChartColumnName      ("execution_count__chart")
//					.setHtmlWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsTableName            ("CmExecQueryStats_diff")
//					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("execution_count")
//					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setSparklineTooltipPostfix  ("Total 'execution_count' in below period")
//					.validate()));
//
//			if (StringUtil.hasValue(col_total_worker_time__chart))
//			{
//				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
//					SparkLineParams.create       (DataSource.CounterModel)
//					.setHtmlChartColumnName      ("total_worker_time__chart")
//					.setHtmlWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsTableName            ("CmExecQueryStats_diff")
//					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("total_worker_time")   
//					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setSparklineTooltipPostfix  ("Total 'worker_time' in below period")
//					.validate()));
//			}
//
//			if (StringUtil.hasValue(col_total_wait_time__chart))
//			{
//				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
//					SparkLineParams.create       (DataSource.CounterModel)
//					.setHtmlChartColumnName      ("total_wait_time__chart")
//					.setHtmlWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsTableName            ("CmExecQueryStats_diff")
//					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("sum([total_elapsed_time]) - sum([total_worker_time])").setGroupDataAggregationType(AggType.USER_PROVIDED)
//					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setSparklineTooltipPostfix  ("Total 'wait_time' in below period")
//					.validate()));
//			}
//			
//			if (StringUtil.hasValue(col_total_physical_reads__chart))
//			{
//				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
//					SparkLineParams.create       (DataSource.CounterModel)
//					.setHtmlChartColumnName      ("total_physical_reads__chart")
//					.setHtmlWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsTableName            ("CmExecQueryStats_diff")
//					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("total_physical_reads")   
//					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setSparklineTooltipPostfix  ("Number of 'physical_reads' in below period")
//					.validate()));
//			}
//
//			if (StringUtil.hasValue(col_total_logical_reads__chart))
//			{
//				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
//					SparkLineParams.create       (DataSource.CounterModel)
//					.setHtmlChartColumnName      ("total_logical_reads__chart")
//					.setHtmlWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsTableName            ("CmExecQueryStats_diff")
//					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("total_logical_reads")   
//					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setSparklineTooltipPostfix  ("Number of 'logical_reads' in below period")
//					.validate()));
//			}
//
//			if (StringUtil.hasValue(col_total_logical_writes__chart))
//			{
//				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
//					SparkLineParams.create       (DataSource.CounterModel)
//					.setHtmlChartColumnName      ("total_logical_writes__chart")
//					.setHtmlWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsTableName            ("CmExecQueryStats_diff")
//					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("total_logical_writes")   
//					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setSparklineTooltipPostfix  ("Number of 'logical_writes' in below period")
//					.validate()));
//			}
//
//			if (StringUtil.hasValue(col_total_elapsed_time__chart))
//			{
//				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
//					SparkLineParams.create       (DataSource.CounterModel)
//					.setHtmlChartColumnName      ("total_elapsed_time__chart")
//					.setHtmlWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsTableName            ("CmExecQueryStats_diff")
//					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("total_elapsed_time")   
//					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setSparklineTooltipPostfix  ("Number of 'elapsed_time' in below period")
//					.validate()));
//			}
//
//			if (StringUtil.hasValue(col_total_rows__chart))
//			{
//				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
//					SparkLineParams.create       (DataSource.CounterModel)
//					.setHtmlChartColumnName      ("total_rows__chart")
//					.setHtmlWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsTableName            ("CmExecQueryStats_diff")
//					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("total_rows")
//					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setSparklineTooltipPostfix  ("Number of 'total_rows' in below period")
//					.validate()));
//			}
//
//			if (StringUtil.hasValue(col_avg_rows__chart))
//			{
//				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
//					SparkLineParams.create       (DataSource.CounterModel)
//					.setHtmlChartColumnName      ("avg_rows__chart")
//					.setHtmlWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsTableName            ("CmExecQueryStats_diff")
//					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("CASE WHEN sum([execution_count]) = 0 THEN 0.0 ELSE sum([total_rows]*1.0)/sum([execution_count]*1.0) END").setGroupDataAggregationType(AggType.USER_PROVIDED)
//					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setSparklineTooltipPostfix  ("Average of 'total_rows' in below period (total_rows/execution_count)")
//					.validate()));
//			}
//
//			if (StringUtil.hasValue(col_max_dop__chart))
//			{
//				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
//					SparkLineParams.create       (DataSource.CounterModel)
//					.setHtmlChartColumnName      ("max_dop__chart")
//					.setHtmlWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsTableName            ("CmExecQueryStats_diff")
//					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("CASE WHEN sum([execution_count]) = 0 THEN 0.0 ELSE sum([total_dop]*1.0)/sum([execution_count]*1.0) END").setGroupDataAggregationType(AggType.USER_PROVIDED)
//					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setSparklineTooltipPostfix  ("MAX 'DOP - Degree Of Paralism' in below period")
//					.validate()));
//			}
//
//			if (StringUtil.hasValue(col_max_grant_kb__chart))
//			{
//				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
//					SparkLineParams.create       (DataSource.CounterModel)
//					.setHtmlChartColumnName      ("max_grant_kb__chart")
//					.setHtmlWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsTableName            ("CmExecQueryStats_diff")
//					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("CASE WHEN sum([execution_count]) = 0 THEN 0.0 ELSE sum([total_grant_kb]*1.0)/sum([execution_count]*1.0) END").setGroupDataAggregationType(AggType.USER_PROVIDED)
//					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setSparklineTooltipPostfix  ("MAX 'grant_kb' in below period")
//					.validate()));
//			}
//
//			if (StringUtil.hasValue(col_total_spills__chart))
//			{
//				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
//					SparkLineParams.create       (DataSource.CounterModel)
//					.setHtmlChartColumnName      ("total_spills__chart")
//					.setHtmlWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsTableName            ("CmExecQueryStats_diff")
//					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("total_spills")
//					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setSparklineTooltipPostfix  ("Number of 'spills' in below period")
//					.validate()));
//			}

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("execution_count__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecQueryStats_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("execution_count")
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
					.setSparklineTooltipPostfix  ("Total 'execution_count' in below period")
					.validate()));

			if (StringUtil.hasValue(col_elapsed_time__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("elapsed_time__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecQueryStats_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("total_elapsed_time")
					.setDbmsDataValueColumnName  ("sum([total_elapsed_time]/1000.0) / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1) // MS
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
					.setSparklineTooltipPostfix  ("Average 'elapsed_time' in milliseconds for below period")
					.validate()));
			}

			if (StringUtil.hasValue(col_worker_time__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("worker_time__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecQueryStats_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("total_worker_time")
					.setDbmsDataValueColumnName  ("sum([total_worker_time]/1000.0) / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1) // MS
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
					.setSparklineTooltipPostfix  ("Average 'worker_time' in milliseconds for below period")
					.validate()));
			}

			if (StringUtil.hasValue(col_est_wait_time__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("est_wait_time__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecQueryStats_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("(sum([total_elapsed_time]/1000.0) - sum([total_worker_time]/1000.0)) / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
					.setSparklineTooltipPostfix  ("Average 'ESTIMATED wait_time' in below period")
					.validate()));
			}
			
			if (StringUtil.hasValue(col_physical_reads__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("physical_reads__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecQueryStats_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("total_physical_reads")
					.setDbmsDataValueColumnName  ("sum(1.0*[total_physical_reads]) / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
					.setSparklineTooltipPostfix  ("Average Number of 'physical_reads' in below period")
					.validate()));
			}

			if (StringUtil.hasValue(col_physical_reads_mb__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("physical_reads_mb__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecQueryStats_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("total_physical_reads")
					.setDbmsDataValueColumnName  ("sum(1.0*[total_physical_reads]) / 128.0 / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
					.setSparklineTooltipPostfix  ("Average Number of 'physical_reads_mb' in below period")
					.validate()));
			}

			if (StringUtil.hasValue(col_logical_reads__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("logical_reads__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecQueryStats_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("total_logical_reads")
					.setDbmsDataValueColumnName  ("sum(1.0*[total_logical_reads]) / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
					.setSparklineTooltipPostfix  ("Average Number of 'logical_reads' in below period")
					.validate()));
			}

			if (StringUtil.hasValue(col_logical_reads_mb__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("logical_reads_mb__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecQueryStats_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("total_logical_reads")
					.setDbmsDataValueColumnName  ("sum(1.0*[total_logical_reads]) / 128.0 / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
					.setSparklineTooltipPostfix  ("Average Number of 'logical_reads_mb' in below period")
					.validate()));
			}

			if (StringUtil.hasValue(col_logical_writes__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("logical_writes__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecQueryStats_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("total_logical_writes")
					.setDbmsDataValueColumnName  ("sum(1.0*[total_logical_writes]) / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
					.setSparklineTooltipPostfix  ("Average Number of 'logical_writes' in below period")
					.validate()));
			}

			if (StringUtil.hasValue(col_logical_writes_mb__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("logical_writes_mb__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecQueryStats_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("total_logical_writes")
					.setDbmsDataValueColumnName  ("sum(1.0*[total_logical_writes]) / 128.0 / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
					.setSparklineTooltipPostfix  ("Average Number of 'logical_writes_mb' in below period")
					.validate()));
			}

			if (StringUtil.hasValue(col_clr_time__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("clr_time__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecQueryStats_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("total_clr_time")
					.setDbmsDataValueColumnName  ("sum([total_clr_time]/1000.0) / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1) // MS
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
					.setSparklineTooltipPostfix  ("Average 'clr_time' in milliseconds for below period")
					.validate()));
			}

			if (StringUtil.hasValue(col_rows__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("rows__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecQueryStats_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("total_rows")
					.setDbmsDataValueColumnName  ("sum(1.0*[total_rows]) / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
					.setSparklineTooltipPostfix  ("Average Number of 'rows' in below period")
					.validate()));
			}

//			if (StringUtil.hasValue(col_avg_rows__chart))
//			{
//				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
//					SparkLineParams.create       (DataSource.CounterModel)
//					.setHtmlChartColumnName      ("avg_rows__chart")
//					.setHtmlWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsTableName            ("CmExecQueryStats_diff")
//					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("CASE WHEN sum([execution_count]) = 0 THEN 0.0 ELSE sum([total_rows]*1.0)/sum([execution_count]*1.0) END").setGroupDataAggregationType(AggType.USER_PROVIDED)
//					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
//					.setSparklineTooltipPostfix  ("Average of 'rows' in below period (total_rows/execution_count)")
//					.validate()));
//			}

			if (StringUtil.hasValue(col_dop__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("dop__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecQueryStats_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("CASE WHEN sum([execution_count]) = 0 THEN 0.0 ELSE sum([total_dop]*1.0)/sum([execution_count]*1.0) END").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsDataValueColumnName  ("sum(1.0*[total_dop]) / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
					.setSparklineTooltipPostfix  ("Average 'DOP - Degree Of Paralism' in below period")
					.validate()));
			}

			if (StringUtil.hasValue(col_grant_kb__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("grant_kb__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecQueryStats_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("CASE WHEN sum([execution_count]) = 0 THEN 0.0 ELSE sum([total_grant_kb]*1.0)/sum([execution_count]*1.0) END").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsDataValueColumnName  ("sum(1.0*[total_grant_kb]) / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(3)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
					.setSparklineTooltipPostfix  ("Average 'grant_kb' in below period")
					.validate()));
			}

			if (StringUtil.hasValue(col_spills__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("spills__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecQueryStats_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("total_spills")
					.setDbmsDataValueColumnName  ("sum(1.0*[total_spills]) / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
					.setSparklineTooltipPostfix  ("Average Number of 'spills' in below period")
					.validate()));
			}

			

			//----------------------------------------------------
			// Create a SQL-Details ResultSet based on values in _shortRstm
			//----------------------------------------------------
			if (   ReportType.CPU_TIME     .equals(_reportType) 
				|| ReportType.EST_WAIT_TIME.equals(_reportType) 
			   )
			{
				SimpleResultSet srs = new SimpleResultSet();

				srs.addColumn("dbname"     , Types.VARCHAR,       60, 0);
				srs.addColumn("query_hash" , Types.VARCHAR,       60, 0);
				srs.addColumn("sparklines" , Types.VARCHAR,      512, 0); 
				srs.addColumn("query"      , Types.VARCHAR, 1024*128, 0); // this is 'text' in the origin table

				// Position in the "source" _shortRstm table (values we will fetch)
				int pos_dbname     = _shortRstm.findColumn("dbname");
				int pos_query_hash = _shortRstm.findColumn("query_hash");
				int pos_query      = _shortRstm.findColumn("SqlText");


				ColumnCopyRender msToHMS    = HtmlTableProducer.MS_TO_HMS;
				ColumnCopyRender oneDecimal = HtmlTableProducer.ONE_DECIMAL;
				
				HtmlTableProducer htp = new HtmlTableProducer(_shortRstm, "dsr-sub-table-chart");
				htp.setTableHeaders("Charts at 10 minute interval", "Total;style='text-align:right!important'", "Avg per exec;style='text-align:right!important'", "");
				                                                       htp.add("exec-cnt" , new ColumnCopyRow().add( new ColumnCopyDef("execution_count__chart"  ) ).add(new ColumnCopyDef("execution_count__sum").setColBold())    .addEmptyCol()                                                         .addEmptyCol() );
				if (StringUtil.hasValue(col_elapsed_time__chart     )) htp.add("exec-time", new ColumnCopyRow().add( new ColumnCopyDef("elapsed_time__chart"     ) ).add(new ColumnCopyDef("total_elapsed_time_ms__sum" , msToHMS) ).add(new ColumnCopyDef("AvgElapsedTimeMs"   , oneDecimal).setColBold()).add(new ColumnStatic("ms"  )) );
				if (StringUtil.hasValue(col_worker_time__chart      )) htp.add("cpu-time" , new ColumnCopyRow().add( new ColumnCopyDef("worker_time__chart"      ) ).add(new ColumnCopyDef("total_worker_time_ms__sum"  , msToHMS) ).add(new ColumnCopyDef("AvgWorkerTimeMs"    , oneDecimal).setColBold()).add(new ColumnStatic("ms"  )) );
				if (StringUtil.hasValue(col_est_wait_time__chart    )) htp.add("est-wait" , new ColumnCopyRow().add( new ColumnCopyDef("est_wait_time__chart"    ) ).add(new ColumnCopyDef("total_est_wait_time_ms__sum", msToHMS) ).add(new ColumnCopyDef("AvgEstWaitTimeMs"   , oneDecimal).setColBold()).add(new ColumnStatic("ms"  )) );   // This will ONLY work for SERIAL Statements
				if (StringUtil.hasValue(col_dop__chart              )) htp.add("dop"      , new ColumnCopyRow().add( new ColumnCopyDef("dop__chart"              ) ).addEmptyCol()                                                  .add(new ColumnCopyDef("AvgDop"             , oneDecimal).setColBold()).add(new ColumnStatic("#"   )) );
				if (StringUtil.hasValue(col_rows__chart             )) htp.add("rows"     , new ColumnCopyRow().add( new ColumnCopyDef("rows__chart"             ) ).add(new ColumnCopyDef("total_rows__sum"                     ) ).add(new ColumnCopyDef("AvgRows"            , oneDecimal).setColBold()).add(new ColumnStatic("rows")) );
				if (StringUtil.hasValue(col_logical_reads__chart    )) htp.add("l-read"   , new ColumnCopyRow().add( new ColumnCopyDef("logical_reads__chart"    ) ).add(new ColumnCopyDef("total_logical_reads__sum"            ) ).add(new ColumnCopyDef("AvgLogicalReads"    , oneDecimal).setColBold()).add(new ColumnStatic("pgs" )) );
				if (StringUtil.hasValue(col_logical_reads_mb__chart )) htp.add("l-read-mb", new ColumnCopyRow().add( new ColumnCopyDef("logical_reads_mb__chart" ) ).add(new ColumnCopyDef("total_logical_reads_mb__sum"         ) ).add(new ColumnCopyDef("AvgLogicalReadsMb"  , oneDecimal).setColBold()).add(new ColumnStatic("mb"  )) );
				if (StringUtil.hasValue(col_physical_reads__chart   )) htp.add("p-read"   , new ColumnCopyRow().add( new ColumnCopyDef("physical_reads__chart"   ) ).add(new ColumnCopyDef("total_physical_reads__sum"           ) ).add(new ColumnCopyDef("AvgPhysicalReads"   , oneDecimal).setColBold()).add(new ColumnStatic("pgs" )) );
				if (StringUtil.hasValue(col_physical_reads_mb__chart)) htp.add("p-read-mb", new ColumnCopyRow().add( new ColumnCopyDef("physical_reads_mb__chart") ).add(new ColumnCopyDef("total_physical_reads_mb__sum"        ) ).add(new ColumnCopyDef("AvgPhysicalReadsMb" , oneDecimal).setColBold()).add(new ColumnStatic("mb" )) );
				if (StringUtil.hasValue(col_logical_writes__chart   )) htp.add("l-write"  , new ColumnCopyRow().add( new ColumnCopyDef("logical_writes__chart"   ) ).add(new ColumnCopyDef("total_logical_writes__sum"           ) ).add(new ColumnCopyDef("AvgLogicalWrites"   , oneDecimal).setColBold()).add(new ColumnStatic("pgs" )) );
				if (StringUtil.hasValue(col_logical_writes_mb__chart)) htp.add("l-write-mb",new ColumnCopyRow().add( new ColumnCopyDef("logical_writes_mb__chart") ).add(new ColumnCopyDef("total_logical_writes_mb__sum"        ) ).add(new ColumnCopyDef("AvgLogicalWritesMb" , oneDecimal).setColBold()).add(new ColumnStatic("mb" )) );
				if (StringUtil.hasValue(col_spills__chart           )) htp.add("spills"   , new ColumnCopyRow().add( new ColumnCopyDef("spills__chart"           ) ).add(new ColumnCopyDef("total_spills__sum"                   ) ).add(new ColumnCopyDef("AvgSpills"          , oneDecimal).setColBold()).add(new ColumnStatic("#"   )) );
				if (StringUtil.hasValue(col_grant_kb__chart         )) htp.add("mem-grant", new ColumnCopyRow().add( new ColumnCopyDef("grant_kb__chart"         ) ).add(new ColumnCopyDef("total_grant_kb__sum"                 ) ).add(new ColumnCopyDef("AvgGrantKb"         , oneDecimal).setColBold()).add(new ColumnStatic("kb"  )) );
				htp.validate();

				// Filter out some rows...
				htp.setRowFilter(new HtmlTableProducer.RowFilter()
				{
					@Override
					public boolean include(ResultSetTableModel rstm, int rstmRow, String rowKey)
					{
						// Only show "est-wait" if the query is "single threaded"
						if ("est-wait".equals(rowKey))
						{
//							return rstm.hasColumn("AvgDop") && rstm.getValueAsInteger(rstmRow, "AvgDop") == 1;
							return rstm.hasColumn("AvgDop") && rstm.getValueAsDouble(rstmRow, "AvgDop", true, -1d) == 1.0d;
						}
						return true;
					}
				});

				
				if (pos_dbname >= 0 && pos_query_hash >= 0 && pos_query >= 0)
				{
					for (int r=0; r<_shortRstm.getRowCount(); r++)
					{
						String dbname     = _shortRstm.getValueAsString    (r, pos_dbname);
						String query_hash = _shortRstm.getValueAsString    (r, pos_query_hash);
						String sqlText    = _shortRstm.getValueAsString    (r, pos_query);
						
						// Parse the 'sqlText' and extract Table Names, then get various table and index information
						String tableInfo = getDbmsTableInformationFromSqlText(conn, dbname, sqlText, DbUtils.DB_PROD_NAME_MSSQL);

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
//							// Get information about ALL tables in list 'tableList' from the DDL Storage (or other counter collectors)
//							Set<SqlServerTableInfo> tableInfoSet = getTableInformationFromMonDdlStorage(conn, tableList);
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
						
						// add record to SimpleResultSet
						srs.addRow(dbname, query_hash, sparklines, sqlText);
					}
				}

				// GET SQLTEXT (only)
				try
				{
					// Note the 'srs' is populated when reading above ResultSet from query
					_sqTextRstm = createResultSetTableModel(srs, "Top SQL TEXT", null, false); // DO NOT TRUNCATE COLUMNS
					srs.close();

					// Mini Chart on "total_elapsed_time"
					// COPY Cell data from the "details" table
//					_sqTextRstm.copyCellContentFrom(_shortRstm, whereKeyColumn, "total_elapsed_time__chart", whereKeyColumn, "total_elapsed_time__chart");
				}
				catch (SQLException ex)
				{
					setProblemException(ex);
		
					_sqTextRstm = ResultSetTableModel.createEmpty("Top SQL TEXT");
					_logger.warn("Problems getting Top SQL TEXT: " + ex);
				}
			}
		}
	}

//	private void calculateAvg(ResultSetTableModel rstm)
//	{
//		for (int r=0; r<rstm.getRowCount(); r++)
//		{
//			calculateAvg(rstm, r, "execution_count__sum", "total_worker_time__sum"              , "AvgWorkerTimeUs");           
//			calculateAvg(rstm, r, "execution_count__sum", "total_wait_time__sum"                , "AvgWaitTimeUs");           
//			calculateAvg(rstm, r, "execution_count__sum", "total_physical_reads__sum"           , "AvgPhysicalReads");          
//			calculateAvg(rstm, r, "execution_count__sum", "total_logical_writes__sum"           , "AvgLogicalWrites");          
//			calculateAvg(rstm, r, "execution_count__sum", "total_logical_reads__sum"            , "AvgLogicalReads");           
//			calculateAvg(rstm, r, "execution_count__sum", "total_clr_time__sum"                 , "AvgClrTimeUs");              
//			calculateAvg(rstm, r, "execution_count__sum", "total_elapsed_time__sum"             , "AvgElapsedTimeUs");          
//			calculateAvg(rstm, r, "execution_count__sum", "total_rows__sum"                     , "AvgRows");                   
//			calculateAvg(rstm, r, "execution_count__sum", "total_dop__sum"                      , "AvgDop");                    
//			calculateAvg(rstm, r, "execution_count__sum", "total_grant_kb__sum"                 , "AvgGrantKb");                
//			calculateAvg(rstm, r, "execution_count__sum", "total_used_grant_kb__sum"            , "AvgUsedGrantKb");            
//			calculateAvg(rstm, r, "execution_count__sum", "total_ideal_grant_kb__sum"           , "AvgIdealGrantKb");           
//			calculateAvg(rstm, r, "execution_count__sum", "total_reserved_threads__sum"         , "AvgReservedThreads");        
//			calculateAvg(rstm, r, "execution_count__sum", "total_used_threads__sum"             , "AvgUsedThreads");            
//			calculateAvg(rstm, r, "execution_count__sum", "total_columnstore_segment_reads__sum", "AvgColumnstoreSegmentReads");
//			calculateAvg(rstm, r, "execution_count__sum", "total_columnstore_segment_skips__sum", "AvgColumnstoreSegmentSkips");
//			calculateAvg(rstm, r, "execution_count__sum", "total_spills__sum"                   , "AvgSpills");                 
//			calculateAvg(rstm, r, "execution_count__sum", "total_page_server_reads__sum"        , "AvgPageServerReads");        
//		}
//	}
//
//	private void calculateAvg(ResultSetTableModel rstm, int r, String cntColName, String srcColName, String destColName)
//	{
//		int pos_cnt  = rstm.findColumnNoCase(cntColName);
//		int pos_src  = rstm.findColumnNoCase(srcColName);
//		int pos_dest = rstm.findColumnNoCase(destColName);
//		
//		// Any of the columns was NOT found
//		if (pos_cnt == -1 || pos_src == -1 || pos_dest == -1)
//		{
//			if (_logger.isDebugEnabled())
//				_logger.debug("calculateAvg(): Some columns was NOT Found when calculation average value for: row="+r+", cntColName["+cntColName+"]="+pos_cnt+", srcColName["+srcColName+"]="+pos_src+", destColName["+destColName+"]="+pos_dest+"."); 
//			return;
//		}
//
//		long cnt = rstm.getValueAsLong(r, pos_cnt);
//		long src = rstm.getValueAsLong(r, pos_src);
//
//		BigDecimal calc;
//		if (cnt > 0)
//		{
//			calc = new BigDecimal( (src*1.0) / (cnt*1.0) ).setScale(1, RoundingMode.HALF_EVEN);
//		}
//		else
//		{
//			calc = new BigDecimal(-1);
//		}
//		
//		rstm.setValueAtWithOverride(calc, r, pos_dest);
//	}
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
