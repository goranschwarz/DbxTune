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
package com.dbxtune.pcs.report.content.sqlserver;

import java.io.IOException;
import java.io.Writer;

import com.dbxtune.cm.sqlserver.CmIndexOpStatSum;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

public class SqlServerIndexOpSumPerDb
extends SqlServerAbstract
{
//	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private IReportChart _CmIndexOpStatSum_OpIudInstance;
	private IReportChart _CmIndexOpStatSum_OpIudTempdb;
	
	private IReportChart _CmIndexOpStatSum_OpLeafIudCount;
	private IReportChart _CmIndexOpStatSum_OpLeafInsertCount;
	private IReportChart _CmIndexOpStatSum_OpLeafUpdateCount;
	private IReportChart _CmIndexOpStatSum_OpLeafDeleteCount;
	private IReportChart _CmIndexOpStatSum_OpLeafGhostCount;

	private IReportChart _CmIndexOpStatSum_OpLeafAllocationCount;
	private IReportChart _CmIndexOpStatSum_OpLeafPageMergeCount;
	private IReportChart _CmIndexOpStatSum_OpRangeScanCount;
	private IReportChart _CmIndexOpStatSum_OpSingletonLookupCount;
	private IReportChart _CmIndexOpStatSum_OpForwardFetchCount;
	private IReportChart _CmIndexOpStatSum_OpLobFetchInBytes;
	private IReportChart _CmIndexOpStatSum_OpRowOverflowFetchInBytes;
	private IReportChart _CmIndexOpStatSum_OpColumnValuePushOffRowCount;
	private IReportChart _CmIndexOpStatSum_OpColumnValuePullInRowCount;

	private IReportChart _CmIndexOpStatSum_OpRowLockCount;
	private IReportChart _CmIndexOpStatSum_OpRowLockWaitCount;
	private IReportChart _CmIndexOpStatSum_OpRowLockWaitMs;
	private IReportChart _CmIndexOpStatSum_OpRowLockWaitMsPerCount;
                                                                             
	private IReportChart _CmIndexOpStatSum_OpPageLockCount;
	private IReportChart _CmIndexOpStatSum_OpPageLockWaitCount;
	private IReportChart _CmIndexOpStatSum_OpPageLockWaitMs;
	private IReportChart _CmIndexOpStatSum_OpPageLockWaitMsPerCount;
                                                                             
	private IReportChart _CmIndexOpStatSum_OpPageLatchWaitCount;
	private IReportChart _CmIndexOpStatSum_OpPageLatchWaitMs;
	private IReportChart _CmIndexOpStatSum_OpPageLatchWaitMsPerCount;
                                                                             
	private IReportChart _CmIndexOpStatSum_OpPageIoLatchWaitCount;
	private IReportChart _CmIndexOpStatSum_OpPageIoLatchWaitMs;
	private IReportChart _CmIndexOpStatSum_OpPageIoLatchWaitMsPerCount;
                                                                             
	private IReportChart _CmIndexOpStatSum_OpIndexLockPromotionAttemptCount;
	private IReportChart _CmIndexOpStatSum_OpIndexLockPromotionCount;
                                                                             
	private IReportChart _CmIndexOpStatSum_OpPageCompressionAttemptCount;
	private IReportChart _CmIndexOpStatSum_OpPageCompressionSuccessCount;

	
	public SqlServerIndexOpSumPerDb(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public boolean hasMinimalMessageText()
	{
		return false;
	}

	@Override
	public boolean hasShortMessageText()
	{
//		return false; // IUD at Instance level is already in: CPU Usage Overview
		return true; // Lets add some charts: Blocking/and-some-others Count, Avg Xxx in MS Per Count 
	}

	@Override
	public void writeMessageText(Writer sb, MessageType messageType)
	throws IOException
	{
		if (isFullMessageType())
		{
			sb.append("<p>");
			sb.append("This Section holds Operational Stats on a database level.<br>");
			sb.append("Information is basically grouped by dbname from the SQL Server table: sys.dm_db_index_operational_stats<br>");
			sb.append("Here are some usefull graphs, if you find them high drill down on a object level in the Counter Model 'Index Operational' or CmName='CmIndexOpStat'.<br>");
			sb.append("You will typically find<br>");
			sb.append("<ul>");
			sb.append("  <li>How many Inserts/Updates/Deletes. At Instance Level or Per database name</li>");
			sb.append("  <li>Page Allocations and Merges</li>");
			sb.append("  <li>Range Scans and Bookmark/Singleton Lookups</li>");
			sb.append("  <li>Row Forward Lookups</li>");
			sb.append("  <li>LOB and Row-Overflow Fetch (how many bytes was read)</li>");
			sb.append("  <li>How many Columns that was pushed Off-Row or Pulled back In-Row again</li>");
			sb.append("  <li>Row/Page Lock Count and Lock Wait Count, and how long we waited</li>");
			sb.append("  <li>Page Latch Count and Wait Count, and how long we waited</li>");
			sb.append("  <li>Page IO Latch Count (Physical Disk IO) and Wait Count, and how long we waited</li>");
			sb.append("  <li>Lock Promotion Attempts, and Success</li>");
			sb.append("  <li>Page Compression Attempts, and Success</li>");
			sb.append("</ul>");
			sb.append("</p>");
		}
		if (isShortMessageType())
		{
			sb.append("<p>");
			sb.append("This Section holds Operational Stats on a database level.<br>");
			sb.append("Information is basically grouped by dbname from the SQL Server table: sys.dm_db_index_operational_stats<br>");
			sb.append("Here are some usefull graphs, if you find them high drill down on a object level in the Counter Model 'Index Operational' or CmName='CmIndexOpStat'.<br>");
			sb.append("You will typically find<br>");
			sb.append("<ul>");
			sb.append("  <li>How many Inserts/Updates/Deletes Per database</li>");
			sb.append("  <li>Row/Page Lock Wait Count and how long we waited Per Count</li>");
			sb.append("  <li>Page Latch Wait Count and how long we waited Per Count</li>");
			sb.append("  <li>Page IO Latch Count (Physical Disk IO) and how long we waited Per Count</li>");
			sb.append("</ul>");
			sb.append("Check the full report, to see more detailed Operations...");
			sb.append("</p>");
		}
		
//		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are 'Total Statements Executed/sec', 'Sum Execution Time in ms' and 'Sum Logical Reads' charts for each Database during the day.",
//				"CmName_GraphName1",
//				"CmName_GraphName2"
//				));

		if (isFullMessageType())
		{
			_CmIndexOpStatSum_OpIudInstance                    .writeHtmlContent(sb, null, null);
			_CmIndexOpStatSum_OpIudTempdb                      .writeHtmlContent(sb, null, null);

			_CmIndexOpStatSum_OpLeafIudCount                   .writeHtmlContent(sb, null, null);
			_CmIndexOpStatSum_OpLeafInsertCount                .writeHtmlContent(sb, null, null);
			_CmIndexOpStatSum_OpLeafUpdateCount                .writeHtmlContent(sb, null, null);
			_CmIndexOpStatSum_OpLeafDeleteCount                .writeHtmlContent(sb, null, null);
			_CmIndexOpStatSum_OpLeafGhostCount                 .writeHtmlContent(sb, null, null);

			_CmIndexOpStatSum_OpLeafAllocationCount            .writeHtmlContent(sb, null, null);
			_CmIndexOpStatSum_OpLeafPageMergeCount             .writeHtmlContent(sb, null, null);
			_CmIndexOpStatSum_OpRangeScanCount                 .writeHtmlContent(sb, null, null);
			_CmIndexOpStatSum_OpSingletonLookupCount           .writeHtmlContent(sb, null, null);
			_CmIndexOpStatSum_OpForwardFetchCount              .writeHtmlContent(sb, null, null);
			_CmIndexOpStatSum_OpLobFetchInBytes                .writeHtmlContent(sb, null, null);
			_CmIndexOpStatSum_OpRowOverflowFetchInBytes        .writeHtmlContent(sb, null, null);
			_CmIndexOpStatSum_OpColumnValuePushOffRowCount     .writeHtmlContent(sb, null, null);
			_CmIndexOpStatSum_OpColumnValuePullInRowCount      .writeHtmlContent(sb, null, null);

			_CmIndexOpStatSum_OpRowLockCount                   .writeHtmlContent(sb, null, null);
			_CmIndexOpStatSum_OpRowLockWaitCount               .writeHtmlContent(sb, null, null);
			_CmIndexOpStatSum_OpRowLockWaitMs                  .writeHtmlContent(sb, null, null);
			_CmIndexOpStatSum_OpRowLockWaitMsPerCount          .writeHtmlContent(sb, null, null);

			_CmIndexOpStatSum_OpPageLockCount                  .writeHtmlContent(sb, null, null);
			_CmIndexOpStatSum_OpPageLockWaitCount              .writeHtmlContent(sb, null, null);
			_CmIndexOpStatSum_OpPageLockWaitMs                 .writeHtmlContent(sb, null, null);
			_CmIndexOpStatSum_OpPageLockWaitMsPerCount         .writeHtmlContent(sb, null, null);

			_CmIndexOpStatSum_OpPageLatchWaitCount             .writeHtmlContent(sb, null, null);
			_CmIndexOpStatSum_OpPageLatchWaitMs                .writeHtmlContent(sb, null, null);
			_CmIndexOpStatSum_OpPageLatchWaitMsPerCount        .writeHtmlContent(sb, null, null);

			_CmIndexOpStatSum_OpPageIoLatchWaitCount           .writeHtmlContent(sb, null, null);
			_CmIndexOpStatSum_OpPageIoLatchWaitMs              .writeHtmlContent(sb, null, null);
			_CmIndexOpStatSum_OpPageIoLatchWaitMsPerCount      .writeHtmlContent(sb, null, null);

			_CmIndexOpStatSum_OpIndexLockPromotionAttemptCount .writeHtmlContent(sb, null, null);
			_CmIndexOpStatSum_OpIndexLockPromotionCount        .writeHtmlContent(sb, null, null);

			_CmIndexOpStatSum_OpPageCompressionAttemptCount    .writeHtmlContent(sb, null, null);
			_CmIndexOpStatSum_OpPageCompressionSuccessCount    .writeHtmlContent(sb, null, null);
		}

		if (isShortMessageType())
		{
			_CmIndexOpStatSum_OpLeafIudCount                   .writeHtmlContent(sb, null, null);

			_CmIndexOpStatSum_OpRowLockWaitCount               .writeHtmlContent(sb, null, null);
			_CmIndexOpStatSum_OpRowLockWaitMsPerCount          .writeHtmlContent(sb, null, null);

			_CmIndexOpStatSum_OpPageLockWaitCount              .writeHtmlContent(sb, null, null);
			_CmIndexOpStatSum_OpPageLockWaitMsPerCount         .writeHtmlContent(sb, null, null);

			_CmIndexOpStatSum_OpPageLatchWaitCount             .writeHtmlContent(sb, null, null);
			_CmIndexOpStatSum_OpPageLatchWaitMsPerCount        .writeHtmlContent(sb, null, null);

			_CmIndexOpStatSum_OpPageIoLatchWaitCount           .writeHtmlContent(sb, null, null);
			_CmIndexOpStatSum_OpPageIoLatchWaitMsPerCount      .writeHtmlContent(sb, null, null);
		}

//		if (_sparklineRstm != null)
//		{
//			if (isFullMessageType())
//			{
//				sb.append("<br>\n");
//				sb.append("<details open> \n");
////				sb.append("<details> \n");
//				sb.append("<summary>Details for above Databases (click to collapse) </summary> \n");
//				
//				sb.append("<br>\n");
//				sb.append("Activity by 'dbname', Row Count: " + _sparklineRstm.getRowCount() + "\n");
//				sb.append(toHtmlTable(_sparklineRstm));
//
//				sb.append("\n");
//				sb.append("</details> \n");
//			}
//			else
//			{
//				sb.append("<br>\n");
//				ResultSetTableModel firstRow = _sparklineRstm.copy(0, 1, null);
//				sb.append(toHtmlTable(firstRow));
//			}
//		}
//
//		// Write JavaScript code for CPU SparkLine
//		if (isFullMessageType())
//		{
//			for (String str : _miniChartJsList)
//				sb.append(str);
//		}
	}

	@Override
	public String getSubject()
	{
		return "Database Operationals Summary (origin: CmIndexOpStatSum/dm_db_index_operational_stats)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmIndexOpStatSum_diff" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{

		String schema = getReportingInstance().getDbmsSchemaName();

		String labelPostFix = "";
//		String labelPostFix = " (Object/Access->Index Op Sum)";
		
		// Add Charts
		_CmIndexOpStatSum_OpIudInstance                    = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_IUD_INSTANCE                             , -1, true, null, "Insert/Update/Delete Rows Per Second at Instance Level" + labelPostFix);
		_CmIndexOpStatSum_OpIudTempdb                      = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_IUD_TEMPDB                               , -1, true, null, "Insert/Update/Delete Rows Per Second for tempdb" + labelPostFix);
		                                                                                                                                                                                    
		_CmIndexOpStatSum_OpLeafIudCount                   = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_LEAF_IUD_COUNT                    , -1, true, null, "Insert/Update/Delete Rows Per Second per Database [leaf_IUD_count]" + labelPostFix);
		_CmIndexOpStatSum_OpLeafInsertCount                = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_LEAF_INSERT_COUNT                 , -1, true, null, "Insert Rows Per Second per Database [leaf_insert_count]" + labelPostFix);
		_CmIndexOpStatSum_OpLeafUpdateCount                = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_LEAF_UPDATE_COUNT                 , -1, true, null, "Update Rows Per Second per Database [leaf_update_count]" + labelPostFix);
		_CmIndexOpStatSum_OpLeafDeleteCount                = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_LEAF_DELETE_COUNT                 , -1, true, null, "Delete Rows Per Second per Database [leaf_delete_count]" + labelPostFix);
		_CmIndexOpStatSum_OpLeafGhostCount                 = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_LEAF_GHOST_COUNT                  , -1, true, null, "Ghost Delete Rows Per Second per Database [leaf_ghost_count]" + labelPostFix);
                                                                                                                                                                                            
		_CmIndexOpStatSum_OpLeafAllocationCount            = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_LEAF_ALLOCATION_COUNT             , -1, true, null, "Page Allocation/Split Per Second per Database [leaf_allocation_count]" + labelPostFix);
		_CmIndexOpStatSum_OpLeafPageMergeCount             = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_LEAF_PAGE_MERGE_COUNT             , -1, true, null, "Page Merges Per Second per Database [leaf_page_merge_count]" + labelPostFix);
		_CmIndexOpStatSum_OpRangeScanCount                 = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_RANGE_SCAN_COUNT                  , -1, true, null, "Range Scans Per Second per Database [range_scan_count]" + labelPostFix);
		_CmIndexOpStatSum_OpSingletonLookupCount           = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_SINGLETON_LOOKUP_COUNT            , -1, true, null, "Bookmark/Singleton Lookups Per Second per Database [singleton_lookup_count]" + labelPostFix);
		_CmIndexOpStatSum_OpForwardFetchCount              = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_FORWARD_FETCH_COUNT               , -1, true, null, "Forward Fetch Count Rows Per Second per Database [forwarded_fetch_count]" + labelPostFix);
		_CmIndexOpStatSum_OpLobFetchInBytes                = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_LOB_FETCH_IN_BYTES                , -1, true, null, "LOB Fetch In Bytes Per Second per Database [lob_fetch_in_bytes]" + labelPostFix);
		_CmIndexOpStatSum_OpRowOverflowFetchInBytes        = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_ROW_OVERFLOW_FETCH_IN_BYTES       , -1, true, null, "Row Overflow Fetch In Bytes Per Second per Database [row_overflow_fetch_in_bytes]" + labelPostFix);
		_CmIndexOpStatSum_OpColumnValuePushOffRowCount     = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_COLUMN_VALUE_PUSH_OFF_ROW_COUNT   , -1, true, null, "Column Pushed Off-Row Count Per Second per Database [column_value_push_off_row_count]" + labelPostFix);
		_CmIndexOpStatSum_OpColumnValuePullInRowCount      = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_COLUMN_VALUE_PULL_IN_ROW_COUNT    , -1, true, null, "Column Pulled In-Row Count Per Second per Database [column_value_pull_in_row_count]" + labelPostFix);
                                                                                                                                                                                            
		_CmIndexOpStatSum_OpRowLockCount                   = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_ROW_LOCK_COUNT                    , -1, true, null, "Row Lock Count Per Second per Database [row_lock_count]" + labelPostFix);
		_CmIndexOpStatSum_OpRowLockWaitCount               = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_ROW_LOCK_WAIT_COUNT               , -1, true, null, "Row Lock Wait Count Per Second per Database [row_lock_count]" + labelPostFix);
		_CmIndexOpStatSum_OpRowLockWaitMs                  = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_ROW_LOCK_WAIT_MS                  , -1, true, null, "Row Lock Wait Time in MS Per Second per Database [row_lock_wait_in_ms]" + labelPostFix);
		_CmIndexOpStatSum_OpRowLockWaitMsPerCount          = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_ROW_LOCK_WAIT_MS_PER_COUNT        , -1, true, null, "Avg Row Lock Wait Time in MS Per Count per Database [row_lock_wait_in_ms_per_count]" + labelPostFix);
	                                                                                                                                                                                        
		_CmIndexOpStatSum_OpPageLockCount                  = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_PAGE_LOCK_COUNT                   , -1, true, null, "Page Lock Count Per Second per Database [page_lock_count]" + labelPostFix);
		_CmIndexOpStatSum_OpPageLockWaitCount              = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_PAGE_LOCK_WAIT_COUNT              , -1, true, null, "Page Lock Wait Count Per Second per Database [page_lock_count]" + labelPostFix);
		_CmIndexOpStatSum_OpPageLockWaitMs                 = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_PAGE_LOCK_WAIT_MS                 , -1, true, null, "Page Lock Wait Time in MS Per Second per Database [page_lock_wait_in_ms]" + labelPostFix);
		_CmIndexOpStatSum_OpPageLockWaitMsPerCount         = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_PAGE_LOCK_WAIT_MS_PER_COUNT       , -1, true, null, "Avg Page Lock Wait Time in MS Per Count per Database [page_lock_wait_in_ms_per_count]" + labelPostFix);
	                                                                                                                                                                                        
		_CmIndexOpStatSum_OpPageLatchWaitCount             = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_PAGE_LATCH_WAIT_COUNT             , -1, true, null, "Page Latch Wait Count Per Second per Database [page_latch_wait_count]" + labelPostFix);
		_CmIndexOpStatSum_OpPageLatchWaitMs                = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_PAGE_LATCH_WAIT_MS                , -1, true, null, "Page Latch Wait Time in MS Per Second per Database [page_latch_wait_in_ms]" + labelPostFix);
		_CmIndexOpStatSum_OpPageLatchWaitMsPerCount        = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_PAGE_LATCH_WAIT_MS_PER_COUNT      , -1, true, null, "Avg Page Latch Wait Time in MS Per Count per Database [page_latch_wait_in_ms_per_count]" + labelPostFix);
	                                                                                                                                                                                        
		_CmIndexOpStatSum_OpPageIoLatchWaitCount           = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_PAGE_IO_LATCH_WAIT_COUNT          , -1, true, null, "Page IO Latch Wait Count Per Second per Database [page_io_latch_wait_count]" + labelPostFix);
		_CmIndexOpStatSum_OpPageIoLatchWaitMs              = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_PAGE_IO_LATCH_WAIT_MS             , -1, true, null, "Page IO Latch Wait Time in MS Per Second per Database [page_io_latch_wait_in_ms]" + labelPostFix);
		_CmIndexOpStatSum_OpPageIoLatchWaitMsPerCount      = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_PAGE_IO_LATCH_WAIT_MS_PER_COUNT   , -1, true, null, "Avg Page IO Latch Wait Time in MS Per Count per Database [page_io_latch_wait_in_ms_per_count]" + labelPostFix);
	                                                                                                                                                                                        
		_CmIndexOpStatSum_OpIndexLockPromotionAttemptCount = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_INDEX_LOCK_PROMATION_ATTEMPT_COUNT, -1, true, null, "Lock Promotion Attempt Count Per Second per Database [index_lock_promotion_attempt_count]" + labelPostFix);
		_CmIndexOpStatSum_OpIndexLockPromotionCount        = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_INDEX_LOCK_PROMATION_COUNT        , -1, true, null, "Lock Promotion Success Count Per Second per Database [index_lock_promotion_count]" + labelPostFix);
	                                                                                                                                                                                        
		_CmIndexOpStatSum_OpPageCompressionAttemptCount    = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_PAGE_COMPRESSION_ATTEMPT_COUNT    , -1, true, null, "Page Compression Attempt Count Per Second per Database [page_compression_attempt_count]" + labelPostFix);
		_CmIndexOpStatSum_OpPageCompressionSuccessCount    = createTsLineChart(conn, schema, CmIndexOpStatSum.CM_NAME, CmIndexOpStatSum.GRAPH_NAME_PER_DB_PAGE_COMPRESSION_SUCCESS_COUNT    , -1, true, null, "Page Compression Success Count Per Second per Database [page_compression_success_count]" + labelPostFix);

	} // end: method
}


