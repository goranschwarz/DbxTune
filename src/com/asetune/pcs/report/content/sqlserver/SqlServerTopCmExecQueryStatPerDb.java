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
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.h2.tools.SimpleResultSet;

import com.asetune.cm.CountersModel;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.IReportChart;
import com.asetune.pcs.report.content.SparklineHelper;
import com.asetune.pcs.report.content.SparklineHelper.AggType;
import com.asetune.pcs.report.content.SparklineHelper.DataSource;
import com.asetune.pcs.report.content.SparklineHelper.SparkLineParams;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.HtmlTableProducer;
import com.asetune.utils.HtmlTableProducer.ColumnCopyDef;
import com.asetune.utils.HtmlTableProducer.ColumnCopyRender;
import com.asetune.utils.HtmlTableProducer.ColumnCopyRow;
import com.asetune.utils.HtmlTableProducer.ColumnStatic;
import com.asetune.utils.StringUtil;

public class SqlServerTopCmExecQueryStatPerDb
extends SqlServerAbstract
{
	private static Logger _logger = Logger.getLogger(SqlServerTopCmExecQueryStatPerDb.class);

	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _sparklineRstm;
	private List<String>        _miniChartJsList = new ArrayList<>();

	private IReportChart _CmExecQueryStatPerDb_DbExecCnt;
	private IReportChart _CmExecQueryStatPerDb_DbElapsedTime;
	private IReportChart _CmExecQueryStatPerDb_DbLWrite;
	private IReportChart _CmExecQueryStatPerDb_DbPlanCnt;
	private IReportChart _CmExecQueryStatPerDb_DbPlanDiff;
	private IReportChart _CmExecQueryStatPerDb_DbRemovedPlans;
	private IReportChart _CmExecQueryStatPerDb_DbNewPlans;
	private IReportChart _CmExecQueryStatPerDb_DbReUsedPlans;
	
	private String _sqlOrderByCol = "-unknown-";
	private String _sqlHaving     = "";

	public SqlServerTopCmExecQueryStatPerDb(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);

		_sqlOrderByCol = "[total_worker_time_ms__sum]";
		_sqlHaving     = "";
	}

	@Override
	public boolean hasShortMessageText()
	{
		return true;
	}

	@Override
	public void writeMessageText(Writer sb, MessageType messageType)
	throws IOException
	{
		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are 'Total Statements Executed/sec', 'Sum Execution Time in ms' and 'Sum Logical Reads' charts for each Database during the day.",
				"CmExecQueryStatPerDb_DbExecCnt",
				"CmExecQueryStatPerDb_DbElapsedTime",
				"CmExecQueryStatPerDb_DbLWrite",
				"CmExecQueryStatPerDb_DbPlanCnt",
				"CmExecQueryStatPerDb_DbPlanDiff",
				"CmExecQueryStatPerDb_DbRemovedPlans",
				"CmExecQueryStatPerDb_DbNewPlans",
				"CmExecQueryStatPerDb_DbReUsedPlans"
				));

		_CmExecQueryStatPerDb_DbExecCnt     .writeHtmlContent(sb, null, null);
		_CmExecQueryStatPerDb_DbElapsedTime .writeHtmlContent(sb, null, null);
		_CmExecQueryStatPerDb_DbLWrite      .writeHtmlContent(sb, null, null);
		_CmExecQueryStatPerDb_DbPlanCnt     .writeHtmlContent(sb, null, null);
		_CmExecQueryStatPerDb_DbPlanDiff    .writeHtmlContent(sb, null, null);
		_CmExecQueryStatPerDb_DbNewPlans    .writeHtmlContent(sb, null, null);
		_CmExecQueryStatPerDb_DbRemovedPlans.writeHtmlContent(sb, null, null);
		_CmExecQueryStatPerDb_DbReUsedPlans .writeHtmlContent(sb, null, null);

		if (isFullMessageType())
		{
			// Get a description of this section, and column names
			sb.append(getSectionDescriptionHtml(_shortRstm, true));

			sb.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
			sb.append(toHtmlTable(_shortRstm));
		}

		if (_sparklineRstm != null)
		{
			if (isFullMessageType())
			{
				sb.append("<br>\n");
				sb.append("<details open> \n");
//				sb.append("<details> \n");
				sb.append("<summary>Details for above Databases (click to collapse) </summary> \n");
				
				sb.append("<br>\n");
				sb.append("Activity by 'dbname', Row Count: " + _sparklineRstm.getRowCount() + "\n");
				sb.append(toHtmlTable(_sparklineRstm));

				sb.append("\n");
				sb.append("</details> \n");
			}
			else
			{
				sb.append("<br>\n");
				ResultSetTableModel firstRow = _sparklineRstm.copy(0, 1, null);
				sb.append(toHtmlTable(firstRow));
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
		return "Top Database Activity (order by: " + _sqlOrderByCol + ", origin: CmExecQueryStatPerDb/dm_exec_query_stats)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmExecQueryStatPerDb_diff" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		// just to get Column names
		String dummySql = "select * from [CmExecQueryStatPerDb_diff] where 1 = 2";
		ResultSetTableModel dummyRstm = executeQuery(conn, dummySql, false, "metadata");
		if (dummyRstm == null)
		{
			String msg = "Table 'CmExecQueryStatPerDb_diff' did not exist. So Performance Counters for this hasn't been sampled during this period.";

			//addMessage(msg);
			setProblemException(new Exception(msg));

			_shortRstm = ResultSetTableModel.createEmpty("CmExecQueryStatPerDb");
			return;
		}

		//  SQL for: only records that has been diff calculations (not first time seen, some ASE Versions has a bug that do not clear counters on reuse)
		String sql_and_skipNewOrDiffRateRows = "  and [CmNewDiffRateRow] = 0 \n"; // This is the "old" way... and used for backward compatibility
//		String sql_and_onlyNewOrDiffRateRows = "  and [CmNewDiffRateRow] = 1 \n"; // This is the "old" way... and used for backward compatibility
		if (dummyRstm.hasColumn("CmRowState")) // New column name for 'CmNewDiffRateRow' (which is a bitwise state column)
		{
			// the below will produce for H2:     and  BITAND([CmRowState], 1) = ???   
			//                        for OTHERS: and  ([CmRowState] & 1) = ???
			sql_and_skipNewOrDiffRateRows = "  and " + conn.toBitAnd("[CmRowState]", CountersModel.ROW_STATE__IS_DIFF_OR_RATE_ROW) + " = 0 \n";
//			sql_and_onlyNewOrDiffRateRows = "  and " + conn.toBitAnd("[CmRowState]", CountersModel.ROW_STATE__IS_DIFF_OR_RATE_ROW) + " = " + CountersModel.ROW_STATE__IS_DIFF_OR_RATE_ROW + " \n";
		}
//FIXME; double check the code for "CmNewDiffRateRow and CmRowState"

//		String col_total_elapsed_time_ms__sum           = !dummyRstm.hasColumnNoCase("total_elapsed_time"             ) ? "" : "    ,cast(sum([total_elapsed_time]/1000.0)       as bigint)        AS [total_elapsed_time_ms__sum]           \n"; 
//		String col_total_worker_time_ms__sum            = !dummyRstm.hasColumnNoCase("total_worker_time"              ) ? "" : "    ,cast(sum([total_worker_time]/1000.0)        as bigint)        AS [total_worker_time_ms__sum]            \n"; 
//		String col_total_est_wait_time_ms__sum          = !dummyRstm.hasColumnNoCase("total_elapsed_time"             ) ? "" : "    ,cast(sum([total_elapsed_time]/1000.0) - sum([total_worker_time]/1000.0) as bigint) AS [total_est_wait_time_ms__sum]   \n"; 
		String col_total_elapsed_time_ms__sum           = !dummyRstm.hasColumnNoCase("total_elapsed_time_ms"          ) ? "" : "    ,cast(sum([total_elapsed_time_ms])           as bigint)        AS [total_elapsed_time_ms__sum]           \n"; 
		String col_total_worker_time_ms__sum            = !dummyRstm.hasColumnNoCase("total_worker_time_ms"           ) ? "" : "    ,cast(sum([total_worker_time_ms])            as bigint)        AS [total_worker_time_ms__sum]            \n"; 
		String col_total_est_wait_time_ms__sum          = !dummyRstm.hasColumnNoCase("total_elapsed_time_ms"          ) ? "" : "    ,cast(sum([total_elapsed_time_ms]) - sum([total_worker_time_ms]) as bigint) AS [total_est_wait_time_ms__sum]   \n"; 
		String col_total_physical_reads__sum            = !dummyRstm.hasColumnNoCase("total_physical_reads"           ) ? "" : "    ,cast(sum([total_physical_reads])            as bigint)        AS [total_physical_reads__sum]            \n"; 
		String col_total_logical_writes__sum            = !dummyRstm.hasColumnNoCase("total_logical_writes"           ) ? "" : "    ,cast(sum([total_logical_writes])            as bigint)        AS [total_logical_writes__sum]            \n"; 
		String col_total_logical_reads__sum             = !dummyRstm.hasColumnNoCase("total_logical_reads"            ) ? "" : "    ,cast(sum([total_logical_reads])             as bigint)        AS [total_logical_reads__sum]             \n"; 
		String col_total_logical_reads_mb__sum          = !dummyRstm.hasColumnNoCase("total_logical_reads"            ) ? "" : "    ,cast(sum([total_logical_reads]) / 128.0     as numeric(19,1)) AS [total_logical_reads_mb__sum]          \n"; // h2 bigint/int seems to produce decimal values...
		String col_total_clr_time_ms__sum               = !dummyRstm.hasColumnNoCase("total_clr_time"                 ) ? "" : "    ,cast(sum([total_clr_time]/1000.0)           as bigint       ) AS [total_clr_time_ms__sum]               \n"; 
		String col_total_rows__sum                      = !dummyRstm.hasColumnNoCase("total_rows"                     ) ? "" : "    ,cast(sum([total_rows])                      as bigint       ) AS [total_rows__sum]                      \n"; 
		String col_total_dop__sum                       = !dummyRstm.hasColumnNoCase("total_dop"                      ) ? "" : "    ,cast(sum([total_dop])                       as bigint       ) AS [total_dop__sum]                       \n"; 
//		String col_dop__avg                             = !dummyRstm.hasColumnNoCase("dop"                            ) ? "" : "    ,cast(avg([dop])                             as numeric(10,1)) AS [dop__avg]                             \n"; 
		String col_total_grant_kb__sum                  = !dummyRstm.hasColumnNoCase("total_grant_kb"                 ) ? "" : "    ,cast(sum([total_grant_kb])                  as bigint       ) AS [total_grant_kb__sum]                  \n"; 
		String col_total_grant_mb__sum                  = !dummyRstm.hasColumnNoCase("total_grant_kb"                 ) ? "" : "    ,cast(sum([total_grant_kb]/1024.0)           as numeric(19,1)) AS [total_grant_mb__sum]                  \n"; 
		String col_total_used_grant_kb__sum             = !dummyRstm.hasColumnNoCase("total_used_grant_kb"            ) ? "" : "    ,cast(sum([total_used_grant_kb])             as bigint       ) AS [total_used_grant_kb__sum]             \n"; 
		String col_total_ideal_grant_kb__sum            = !dummyRstm.hasColumnNoCase("total_ideal_grant_kb"           ) ? "" : "    ,cast(sum([total_ideal_grant_kb])            as bigint       ) AS [total_ideal_grant_kb__sum]            \n"; 
		String col_total_reserved_threads__sum          = !dummyRstm.hasColumnNoCase("total_reserved_threads"         ) ? "" : "    ,cast(sum([total_reserved_threads])          as bigint       ) AS [total_reserved_threads__sum]          \n"; 
		String col_total_used_threads__sum              = !dummyRstm.hasColumnNoCase("total_used_threads"             ) ? "" : "    ,cast(sum([total_used_threads])              as bigint       ) AS [total_used_threads__sum]              \n"; 
		String col_total_columnstore_segment_reads__sum = !dummyRstm.hasColumnNoCase("total_columnstore_segment_reads") ? "" : "    ,cast(sum([total_columnstore_segment_reads]) as bigint       ) AS [total_columnstore_segment_reads__sum] \n"; 
		String col_total_columnstore_segment_skips__sum = !dummyRstm.hasColumnNoCase("total_columnstore_segment_skips") ? "" : "    ,cast(sum([total_columnstore_segment_skips]) as bigint       ) AS [total_columnstore_segment_skips__sum] \n"; 
		String col_total_spills__sum                    = !dummyRstm.hasColumnNoCase("total_spills"                   ) ? "" : "    ,cast(sum([total_spills])                    as bigint       ) AS [total_spills__sum]                    \n"; 
		String col_total_spills_mb__sum                 = !dummyRstm.hasColumnNoCase("total_spills"                   ) ? "" : "    ,cast(sum([total_spills]) / 128.0            as numeric(19,1)) AS [total_spills_mb__sum]                 \n"; 
		String col_total_page_server_reads__sum         = !dummyRstm.hasColumnNoCase("total_page_server_reads"        ) ? "" : "    ,cast(sum([total_page_server_reads])         as bigint       ) AS [total_page_server_reads__sum]         \n"; 

		String col_AvgElapsedTimeMs                     = !dummyRstm.hasColumnNoCase("total_elapsed_time_ms"          ) ? "" : "    ,cast( sum([total_elapsed_time_ms])                          * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgElapsedTimeMs]           \n";
		String col_AvgWorkerTimeMs                      = !dummyRstm.hasColumnNoCase("total_worker_time_ms"           ) ? "" : "    ,cast( sum([total_worker_time_ms])                           * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgWorkerTimeMs]            \n";
		String col_AvgEstWaitTimeMs                     = !dummyRstm.hasColumnNoCase("total_elapsed_time_ms"          ) ? "" : "    ,cast((sum([total_elapsed_time_ms]) - sum([total_worker_time_ms])) * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgEstWaitTimeMs]     \n";
		String col_AvgEstWaitTimePct                    = !dummyRstm.hasColumnNoCase("est_wait_time_pct"              ) ? "" : "    ,cast( avg([est_wait_time_pct])                                                                        as numeric(19,1)) AS [AvgEstWaitTimePct]          \n";
		String col_AvgPhysicalReads                     = !dummyRstm.hasColumnNoCase("total_physical_reads"           ) ? "" : "    ,cast( sum([total_physical_reads])                           * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgPhysicalReads]           \n";
		String col_AvgLogicalWrites                     = !dummyRstm.hasColumnNoCase("total_logical_writes"           ) ? "" : "    ,cast( sum([total_logical_writes])                           * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgLogicalWrites]           \n";
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
		String col_AvgSpillsMb                          = !dummyRstm.hasColumnNoCase("total_spills"                   ) ? "" : "    ,cast( sum([total_spills]) / 128.0                           * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgSpillsMb]                \n";
		String col_AvgPageServerReads                   = !dummyRstm.hasColumnNoCase("total_page_server_reads"        ) ? "" : "    ,cast( sum([total_page_server_reads])                        * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgPageServerReads]         \n"; 

		String col_elapsed_time_ms__chart               = !dummyRstm.hasColumnNoCase("total_elapsed_time_ms"          ) ? "" : "    ,cast('' as varchar(512))  as [elapsed_time_ms__chart]   \n"; 
		String col_worker_time_ms__chart                = !dummyRstm.hasColumnNoCase("total_worker_time_ms"           ) ? "" : "    ,cast('' as varchar(512))  as [worker_time_ms__chart]    \n"; 
		String col_est_wait_time_ms__chart              = !dummyRstm.hasColumnNoCase("total_elapsed_time_ms"          ) ? "" : "    ,cast('' as varchar(512))  as [est_wait_time_ms__chart]  \n"; 
		String col_est_wait_time_pct__chart             = !dummyRstm.hasColumnNoCase("est_wait_time_pct"              ) ? "" : "    ,cast('' as varchar(512))  as [est_wait_time_pct__chart] \n"; 
		String col_physical_reads__chart                = !dummyRstm.hasColumnNoCase("total_physical_reads"           ) ? "" : "    ,cast('' as varchar(512))  as [physical_reads__chart]    \n"; 
		String col_logical_reads__chart                 = !dummyRstm.hasColumnNoCase("total_logical_reads"            ) ? "" : "    ,cast('' as varchar(512))  as [logical_reads__chart]     \n"; 
		String col_logical_reads_mb__chart              = !dummyRstm.hasColumnNoCase("total_logical_reads"            ) ? "" : "    ,cast('' as varchar(512))  as [logical_reads_mb__chart]  \n"; 
		String col_logical_writes__chart                = !dummyRstm.hasColumnNoCase("total_logical_writes"           ) ? "" : "    ,cast('' as varchar(512))  as [logical_writes__chart]    \n"; 
		String col_clr_time__chart                      = !dummyRstm.hasColumnNoCase("total_clr_time"                 ) ? "" : "    ,cast('' as varchar(512))  as [clr_time__chart]          \n";
		String col_rows__chart                          = !dummyRstm.hasColumnNoCase("total_rows"                     ) ? "" : "    ,cast('' as varchar(512))  as [rows__chart]              \n"; 
		String col_dop__chart                           = !dummyRstm.hasColumnNoCase("total_dop"                      ) ? "" : "    ,cast('' as varchar(512))  as [dop__chart]               \n"; 
		String col_grant_kb__chart                      = !dummyRstm.hasColumnNoCase("total_grant_kb"                 ) ? "" : "    ,cast('' as varchar(512))  as [grant_kb__chart]          \n"; 
		String col_spills__chart                        = !dummyRstm.hasColumnNoCase("total_spills"                   ) ? "" : "    ,cast('' as varchar(512))  as [spills__chart]            \n"; 
		String col_spills_mb__chart                     = !dummyRstm.hasColumnNoCase("total_spills"                   ) ? "" : "    ,cast('' as varchar(512))  as [spills_mb__chart]         \n"; 

		
		String col_plan_count_abs__chart                = !dummyRstm.hasColumnNoCase("plan_count_abs"                 ) ? "" : "    ,cast(''                    as varchar(512))  as [plan_count_abs__chart]  \n"; 
		String col_plan_count_abs                       = !dummyRstm.hasColumnNoCase("plan_count_abs"                 ) ? "" : "    ,cast(avg([plan_count_abs]) as bigint)        as [plan_count_abs]         \n"; 

		String col_plan_count_diff__chart               = !dummyRstm.hasColumnNoCase("plan_count"                     ) ? "" : "    ,cast(''                    as varchar(512))  as [plan_count_diff__chart] \n"; 
		String col_plan_count_diff                      = !dummyRstm.hasColumnNoCase("plan_count"                     ) ? "" : "    ,cast(sum([plan_count])     as bigint)        as [plan_count_diff]        \n"; 

//TODO; Test the new stuff;

		String sql = getCmDiffColumnsAsSqlComment("CmActiveStatements")
			    + "select \n"
			    + "     [dbname] \n"
			    + "    \n"

			    + col_plan_count_abs__chart 
				+ col_plan_count_abs 
			    + "    \n"

			    + col_plan_count_diff__chart 
				+ col_plan_count_diff 
			    + "    \n"

			    + "    ,cast('' as varchar(512))               as [execution_count__chart] \n"
			    + "    ,sum([execution_count])                 as [execution_count__sum] \n"
			    + "    \n"

				+ col_elapsed_time_ms__chart
			    + col_total_elapsed_time_ms__sum           + col_AvgElapsedTimeMs           

			    + col_worker_time_ms__chart            
			    + col_total_worker_time_ms__sum            + col_AvgWorkerTimeMs

			    + col_est_wait_time_ms__chart
			    + col_total_est_wait_time_ms__sum          + col_AvgEstWaitTimeMs            

			    + col_est_wait_time_pct__chart
			    + col_AvgEstWaitTimePct // NOTE: This do NOT have a SUM

			    + col_logical_reads__chart
			    + col_total_logical_reads__sum             + col_AvgLogicalReads            

			    + col_logical_reads_mb__chart
			    + col_total_logical_reads_mb__sum          + col_AvgLogicalReadsMb            

			    + col_physical_reads__chart
			    + col_total_physical_reads__sum            + col_AvgPhysicalReads           

			    + col_logical_writes__chart
			    + col_total_logical_writes__sum            + col_AvgLogicalWrites           

			    + col_rows__chart
//			    + col_avg_rows__chart
			    + col_total_rows__sum                      + col_AvgRows                    

			    + col_spills__chart
			    + col_total_spills__sum                    + col_AvgSpills                  

			    + col_spills_mb__chart
			    + col_total_spills_mb__sum                 + col_AvgSpillsMb                  

			    + col_dop__chart
			    + col_total_dop__sum                       + col_AvgDop                     

			    + col_grant_kb__chart
			    + col_total_grant_kb__sum                  + col_AvgGrantKb                 
			    + col_total_grant_mb__sum                  + col_AvgGrantMb
			    + col_total_used_grant_kb__sum             + col_AvgUsedGrantKb             
			    + col_total_ideal_grant_kb__sum            + col_AvgIdealGrantKb            

			    + col_total_reserved_threads__sum          + col_AvgReservedThreads         
			    + col_total_used_threads__sum              + col_AvgUsedThreads             

			    + col_clr_time__chart
			    + col_total_clr_time_ms__sum               + col_AvgClrTimeMs               

			    + col_total_columnstore_segment_reads__sum + col_AvgColumnstoreSegmentReads 
			    + col_total_columnstore_segment_skips__sum + col_AvgColumnstoreSegmentSkips 

			    + col_total_page_server_reads__sum         + col_AvgPageServerReads         

//			    + "    ,max([plan_handle])                     as [plan_handle] \n"
			    + "    ,count(*)                               as [samples__count] \n"
			    + "    ,min([SessionSampleTime])               as [SessionSampleTime__min] \n"
			    + "    ,max([SessionSampleTime])               as [SessionSampleTime__max] \n"
			    + "    ,cast('' as varchar(30))                as [Duration] \n"

				+ "from [CmExecQueryStatPerDb_diff] \n"
				+ "where 1 = 1 \n"
				+ sql_and_skipNewOrDiffRateRows // [CmNewDiffRateRow] = 0 -- only records that has been diff calculations (not first time seen, when it swaps in/out due to execution every x minute)
				+ getReportPeriodSqlWhere()
				+ "group by [dbname] \n"
				+ _sqlHaving
				+ "order by " + _sqlOrderByCol + " desc \n"
			    + "";

		// Execute
		_shortRstm = executeQuery(conn, sql, false, "TopCmExecQueryStatPerDb");

		// FAILURE or OK
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("TopCmExecQueryStatPerDb");
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

			
			//----------------------------------------------------
			// Mini Chart on "..."
			//----------------------------------------------------
			String whereKeyColumn = "dbname";
			String dbmsTableName  = "CmExecQueryStatPerDb_diff";
			String dbmsSampleTimeColumnName = "SessionSampleTime";

			if (StringUtil.hasValue(col_plan_count_abs__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("plan_count_abs__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            (dbmsTableName)
					.setDbmsSampleTimeColumnName (dbmsSampleTimeColumnName)
					.setDbmsDataValueColumnName  ("plan_count_abs").setGroupDataAggregationType(AggType.AVG).setDecimalScale(0)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Plan Count Abs")
					.validate()));
			}
			
			if (StringUtil.hasValue(col_plan_count_diff__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("plan_count_diff__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            (dbmsTableName)
					.setDbmsSampleTimeColumnName (dbmsSampleTimeColumnName)
					.setDbmsDataValueColumnName  ("plan_count").setGroupDataAggregationType(AggType.SUM).setDecimalScale(0)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Plan Count Diff")
					.validate()));
			}
			
			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("execution_count__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            (dbmsTableName)
					.setDbmsSampleTimeColumnName (dbmsSampleTimeColumnName)
					.setDbmsDataValueColumnName  ("execution_count")
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Total 'execution_count' in below period")
					.validate()));

			if (StringUtil.hasValue(col_elapsed_time_ms__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("elapsed_time_ms__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            (dbmsTableName)
					.setDbmsSampleTimeColumnName (dbmsSampleTimeColumnName)
//					.setDbmsDataValueColumnName  ("total_elapsed_time")
					.setDbmsDataValueColumnName  ("sum([total_elapsed_time_ms]) / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1) // MS
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Average 'elapsed_time' in milliseconds for below period")
					.validate()));
			}

			if (StringUtil.hasValue(col_worker_time_ms__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("worker_time_ms__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            (dbmsTableName)
					.setDbmsSampleTimeColumnName (dbmsSampleTimeColumnName)
//					.setDbmsDataValueColumnName  ("total_worker_time")
					.setDbmsDataValueColumnName  ("sum([total_worker_time_ms]) / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1) // MS
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Average 'worker_time' in milliseconds for below period")
					.validate()));
			}

			if (StringUtil.hasValue(col_est_wait_time_ms__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("est_wait_time_ms__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            (dbmsTableName)
					.setDbmsSampleTimeColumnName (dbmsSampleTimeColumnName)
					.setDbmsDataValueColumnName  ("(sum([total_elapsed_time_ms]) - sum([total_worker_time_ms])) / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1) // MS
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Average ESTIMATED 'wait_time' in below period, only valid if DOP is 1")
					.validate()));
			}
			
			if (StringUtil.hasValue(col_est_wait_time_pct__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("est_wait_time_pct__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            (dbmsTableName)
					.setDbmsSampleTimeColumnName (dbmsSampleTimeColumnName)
					.setDbmsDataValueColumnName  ("est_wait_time_pct").setGroupDataAggregationType(AggType.AVG).setDecimalScale(1) // PCT
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Average ESTIMATED 'wait_time_pct' in below period, only valid if DOP is 1")
					.validate()));
			}
			
			if (StringUtil.hasValue(col_physical_reads__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("physical_reads__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            (dbmsTableName)
					.setDbmsSampleTimeColumnName (dbmsSampleTimeColumnName)
//					.setDbmsDataValueColumnName  ("total_physical_reads")
					.setDbmsDataValueColumnName  ("sum(1.0*[total_physical_reads]) / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Average Number of 'physical_reads' in below period")
					.validate()));
			}

			if (StringUtil.hasValue(col_logical_reads__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("logical_reads__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            (dbmsTableName)
					.setDbmsSampleTimeColumnName (dbmsSampleTimeColumnName)
//					.setDbmsDataValueColumnName  ("total_logical_reads")
					.setDbmsDataValueColumnName  ("sum(1.0*[total_logical_reads]) / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Average Number of 'logical_reads' in below period")
					.validate()));
			}

			if (StringUtil.hasValue(col_logical_reads_mb__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("logical_reads_mb__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            (dbmsTableName)
					.setDbmsSampleTimeColumnName (dbmsSampleTimeColumnName)
//					.setDbmsDataValueColumnName  ("total_logical_reads")
					.setDbmsDataValueColumnName  ("sum(1.0*[total_logical_reads]) / 128.0 / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Average Number of 'logical_reads_mb' in below period")
					.validate()));
			}

			if (StringUtil.hasValue(col_logical_writes__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("logical_writes__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            (dbmsTableName)
					.setDbmsSampleTimeColumnName (dbmsSampleTimeColumnName)
//					.setDbmsDataValueColumnName  ("total_logical_writes")
					.setDbmsDataValueColumnName  ("sum(1.0*[total_logical_writes]) / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Average Number of 'logical_writes' in below period")
					.validate()));
			}

			if (StringUtil.hasValue(col_clr_time__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("clr_time__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            (dbmsTableName)
					.setDbmsSampleTimeColumnName (dbmsSampleTimeColumnName)
//					.setDbmsDataValueColumnName  ("total_clr_time")
					.setDbmsDataValueColumnName  ("sum([total_clr_time]/1000.0) / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1) // MS
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Average 'clr_time' in milliseconds for below period")
					.validate()));
			}

			if (StringUtil.hasValue(col_rows__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("rows__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            (dbmsTableName)
					.setDbmsSampleTimeColumnName (dbmsSampleTimeColumnName)
//					.setDbmsDataValueColumnName  ("total_rows")
					.setDbmsDataValueColumnName  ("sum(1.0*[total_rows]) / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Average Number of 'rows' in below period")
					.validate()));
			}

//			if (StringUtil.hasValue(col_avg_rows__chart))
//			{
//				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
//					SparkLineParams.create       (DataSource.CounterModel)
//					.setHtmlChartColumnName      ("avg_rows__chart")
//					.setHtmlWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsTableName            (dbmsTableName)
//					.setDbmsSampleTimeColumnName (dbmsSampleTimeColumnName)
//					.setDbmsDataValueColumnName  ("CASE WHEN sum([execution_count]) = 0 THEN 0.0 ELSE sum([total_rows]*1.0)/sum([execution_count]*1.0) END").setGroupDataAggregationType(AggType.USER_PROVIDED)
//					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setSparklineTooltipPostfix  ("Average of 'rows' in below period (total_rows/execution_count)")
//					.validate()));
//			}

			if (StringUtil.hasValue(col_dop__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("dop__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            (dbmsTableName)
					.setDbmsSampleTimeColumnName (dbmsSampleTimeColumnName)
//					.setDbmsDataValueColumnName  ("CASE WHEN sum([execution_count]) = 0 THEN 0.0 ELSE sum([total_dop]*1.0)/sum([execution_count]*1.0) END").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsDataValueColumnName  ("sum(1.0*[total_dop]) / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Average 'DOP - Degree Of Paralism' in below period")
					.validate()));
			}

			if (StringUtil.hasValue(col_grant_kb__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("grant_kb__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            (dbmsTableName)
					.setDbmsSampleTimeColumnName (dbmsSampleTimeColumnName)
//					.setDbmsDataValueColumnName  ("CASE WHEN sum([execution_count]) = 0 THEN 0.0 ELSE sum([total_grant_kb]*1.0)/sum([execution_count]*1.0) END").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsDataValueColumnName  ("sum(1.0*[total_grant_kb]) / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(3)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Average 'grant_kb' in below period")
					.validate()));
			}

			if (StringUtil.hasValue(col_spills__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("spills__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            (dbmsTableName)
					.setDbmsSampleTimeColumnName (dbmsSampleTimeColumnName)
//					.setDbmsDataValueColumnName  ("total_spills")
					.setDbmsDataValueColumnName  ("sum(1.0*[total_spills]) / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Average Number of 'spills' (pages) in below period")
					.validate()));
			}

			if (StringUtil.hasValue(col_spills_mb__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("spills_mb__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            (dbmsTableName)
					.setDbmsSampleTimeColumnName (dbmsSampleTimeColumnName)
//					.setDbmsDataValueColumnName  ("total_spills")
					.setDbmsDataValueColumnName  ("sum([total_spills]/128.0) / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Average Number of 'spills' (MB) in below period")
					.validate()));
			}

			

			//----------------------------------------------------
			// Create a Sparkline table
			//----------------------------------------------------
			if ( true )
			{
				SimpleResultSet srs = new SimpleResultSet();

				srs.addColumn("dbname"     , Types.VARCHAR,       60, 0);
				srs.addColumn("sparklines" , Types.VARCHAR,      512, 0); 

				// Position in the "source" _shortRstm table (values we will fetch)
				int pos_dbname = _shortRstm.findColumn("dbname");


				ColumnCopyRender msToHMS    = HtmlTableProducer.MS_TO_HMS;
				ColumnCopyRender oneDecimal = HtmlTableProducer.ONE_DECIMAL;
				
				HtmlTableProducer htp = new HtmlTableProducer(_shortRstm, "dsr-sub-table-chart");
				htp.setTableHeaders("Charts at 10 minute interval", "Total;style='text-align:right!important'", "Avg per exec;style='text-align:right!important'", "");
				                                                       htp.add("exec-cnt" , new ColumnCopyRow().add( new ColumnCopyDef("execution_count__chart" ) ).add(new ColumnCopyDef("execution_count__sum").setColBold()).addEmptyCol()                                                         .addEmptyCol() );
				if (StringUtil.hasValue(col_elapsed_time_ms__chart  )) htp.add("exec-time", new ColumnCopyRow().add( new ColumnCopyDef("elapsed_time_ms__chart" ) ).add(new ColumnCopyDef("total_elapsed_time_ms__sum", msToHMS) ).add(new ColumnCopyDef("AvgElapsedTimeMs"   , oneDecimal).setColBold()).add(new ColumnStatic("ms"  )) );
				if (StringUtil.hasValue(col_worker_time_ms__chart   )) htp.add("cpu-time" , new ColumnCopyRow().add( new ColumnCopyDef("worker_time_ms__chart"  ) ).add(new ColumnCopyDef("total_worker_time_ms__sum" , msToHMS) ).add(new ColumnCopyDef("AvgWorkerTimeMs"    , oneDecimal).setColBold()).add(new ColumnStatic("ms"  )) );
				if (StringUtil.hasValue(col_est_wait_time_ms__chart )) htp.add("est-wait" , new ColumnCopyRow().add( new ColumnCopyDef("est_wait_time_ms__chart") ).add(new ColumnCopyDef("total_est_wait_time_ms__sum",msToHMS) ).add(new ColumnCopyDef("AvgEstWaitTimeMs"   , oneDecimal).setColBold()).add(new ColumnStatic("ms"  )) );   // This will ONLY work for SERIAL Statements
				if (StringUtil.hasValue(col_dop__chart              )) htp.add("dop"      , new ColumnCopyRow().add( new ColumnCopyDef("dop__chart"             ) ).addEmptyCol()                                                 .add(new ColumnCopyDef("AvgDop"             , oneDecimal).setColBold()).add(new ColumnStatic("#"   )) );
				if (StringUtil.hasValue(col_rows__chart             )) htp.add("rows"     , new ColumnCopyRow().add( new ColumnCopyDef("rows__chart"            ) ).add(new ColumnCopyDef("total_rows__sum"                    ) ).add(new ColumnCopyDef("AvgRows"            , oneDecimal).setColBold()).add(new ColumnStatic("rows")) );
				if (StringUtil.hasValue(col_logical_reads__chart    )) htp.add("l-read"   , new ColumnCopyRow().add( new ColumnCopyDef("logical_reads__chart"   ) ).add(new ColumnCopyDef("total_logical_reads__sum"           ) ).add(new ColumnCopyDef("AvgLogicalReads"    , oneDecimal).setColBold()).add(new ColumnStatic("pgs" )) );
				if (StringUtil.hasValue(col_logical_reads_mb__chart )) htp.add("l-read-mb", new ColumnCopyRow().add( new ColumnCopyDef("logical_reads_mb__chart") ).add(new ColumnCopyDef("total_logical_reads_mb__sum"        ) ).add(new ColumnCopyDef("AvgLogicalReadsMb"  , oneDecimal).setColBold()).add(new ColumnStatic("mb"  )) );
				if (StringUtil.hasValue(col_physical_reads__chart   )) htp.add("p-read"   , new ColumnCopyRow().add( new ColumnCopyDef("physical_reads__chart"  ) ).add(new ColumnCopyDef("total_physical_reads__sum"          ) ).add(new ColumnCopyDef("AvgPhysicalReads"   , oneDecimal).setColBold()).add(new ColumnStatic("pgs" )) );
				if (StringUtil.hasValue(col_logical_writes__chart   )) htp.add("l-write"  , new ColumnCopyRow().add( new ColumnCopyDef("logical_writes__chart"  ) ).add(new ColumnCopyDef("total_logical_writes__sum"          ) ).add(new ColumnCopyDef("AvgLogicalWrites"   , oneDecimal).setColBold()).add(new ColumnStatic("pgs" )) );
				if (StringUtil.hasValue(col_spills__chart           )) htp.add("spills"   , new ColumnCopyRow().add( new ColumnCopyDef("spills__chart"          ) ).add(new ColumnCopyDef("total_spills__sum"                  ) ).add(new ColumnCopyDef("AvgSpills"          , oneDecimal).setColBold()).add(new ColumnStatic("pgs" )) );
				if (StringUtil.hasValue(col_spills_mb__chart        )) htp.add("spills-mb", new ColumnCopyRow().add( new ColumnCopyDef("spills_mb__chart"       ) ).add(new ColumnCopyDef("total_spills_mb__sum"               ) ).add(new ColumnCopyDef("AvgSpillsMb"        , oneDecimal).setColBold()).add(new ColumnStatic("mb"  )) );
				if (StringUtil.hasValue(col_grant_kb__chart         )) htp.add("mem-grant", new ColumnCopyRow().add( new ColumnCopyDef("grant_kb__chart"        ) ).add(new ColumnCopyDef("total_grant_kb__sum"                ) ).add(new ColumnCopyDef("AvgGrantKb"         , oneDecimal).setColBold()).add(new ColumnStatic("kb"  )) );
				if (StringUtil.hasValue(col_plan_count_abs__chart   )) htp.add("plan-cnt" , new ColumnCopyRow().add( new ColumnCopyDef("plan_count_abs__chart"  ) ).add(new ColumnCopyDef("plan_count_abs"                     ) ).addEmptyCol()                                                         .add(new ColumnStatic("#"   )) );
				if (StringUtil.hasValue(col_plan_count_diff__chart  )) htp.add("plan-diff", new ColumnCopyRow().add( new ColumnCopyDef("plan_count_diff__chart" ) ).add(new ColumnCopyDef("plan_count_diff"                    ) ).addEmptyCol()                                                         .add(new ColumnStatic("#"   )) );
				htp.validate();
				
				// Filter out some rows... Only show "est-wait" if the query is "single threaded"
				htp.setRowFilter(new HtmlTableProducer.RowFilter()
				{
					@Override
					public boolean include(ResultSetTableModel rstm, int rstmRow, String rowKey)
					{
						// Only show "est-wait" if the query is "single threaded"
						if ("est-wait".equals(rowKey))
						{
							return rstm.hasColumn("AvgDop") && rstm.getValueAsDouble(rstmRow, "AvgDop", true, -1d) == 1.0d;
						}
						return true;
					}
				});

				if (pos_dbname >= 0)
				{
					for (int r=0; r<_shortRstm.getRowCount(); r++)
					{
						String dbname = _shortRstm.getValueAsString(r, pos_dbname);
						
						// Grab all SparkLines we defined in 'subTableRowSpec'
						String sparklines = htp.getHtmlTextForRow(r);

						// add record to SimpleResultSet
						srs.addRow(dbname, sparklines);
					}
				}

				// GET SQLTEXT (only)
				try
				{
					// Note the 'srs' is populated when reading above ResultSet from query
					_sparklineRstm = createResultSetTableModel(srs, "DB Activity", null, false); // DO NOT TRUNCATE COLUMNS
					srs.close();

					// Mini Chart on "total_elapsed_time"
					// COPY Cell data from the "details" table
//					_sqTextRstm.copyCellContentFrom(_shortRstm, whereKeyColumn, "total_elapsed_time__chart", whereKeyColumn, "total_elapsed_time__chart");
				}
				catch (SQLException ex)
				{
					setProblemException(ex);
		
					_sparklineRstm = ResultSetTableModel.createEmpty("DB Activity");
					_logger.warn("Problems getting DB Activity: " + ex);
				}
			} // end: Create a Sparkline table
			
			String schema = getReportingInstance().getDbmsSchemaName();

			// Add Charts
			_CmExecQueryStatPerDb_DbExecCnt      = createTsLineChart(conn, schema, "CmExecQueryStatPerDb", "DbExecCnt"     , -1, true, null, "SQL Statements Per DB - Execution Count/sec");
			_CmExecQueryStatPerDb_DbElapsedTime  = createTsLineChart(conn, schema, "CmExecQueryStatPerDb", "DbElapsedTime" , -1, true, null, "SQL Statements Per DB - Sum Elapsed Time in ms");
			_CmExecQueryStatPerDb_DbLWrite       = createTsLineChart(conn, schema, "CmExecQueryStatPerDb", "DbLWrite"      , -1, true, null, "SQL Statements Per DB - Sum Logical Reads/sec");
			_CmExecQueryStatPerDb_DbPlanCnt      = createTsLineChart(conn, schema, "CmExecQueryStatPerDb", "DbPlanCnt"     , -1, true, null, "SQL Statements Per DB - Plan Count");
			_CmExecQueryStatPerDb_DbPlanDiff     = createTsLineChart(conn, schema, "CmExecQueryStatPerDb", "DbPlanDiff"    , -1, true, null, "SQL Statements Per DB - Plan Count Diff");
			_CmExecQueryStatPerDb_DbRemovedPlans = createTsLineChart(conn, schema, "CmExecQueryStatPerDb", "DbRemovedPlans", -1, true, null, "SQL Statements Per DB - Removed Plans Since Last Sample");
			_CmExecQueryStatPerDb_DbNewPlans     = createTsLineChart(conn, schema, "CmExecQueryStatPerDb", "DbNewPlans"    , -1, true, null, "SQL Statements Per DB - New Plans Since Last Sample");
			_CmExecQueryStatPerDb_DbReUsedPlans  = createTsLineChart(conn, schema, "CmExecQueryStatPerDb", "DbReUsedPlans" , -1, true, null, "SQL Statements Per DB - (Re)Used Plans Since Last Sample");

		} // end: has rstm
	} // end: method

	/**
	 * Set descriptions for the table, and the columns
	 */
	private void setSectionDescription(ResultSetTableModel rstm)
	{
		if (rstm == null)
			return;
		
		// Section description
		rstm.setDescription(
				"Top Database Activity...  (ordered by: " + _sqlOrderByCol + ") <br>" +
				"<br>" +
				"SqlServer Source table is 'dm_exec_query_stats'. <br>" +
				"PCS Source table is 'CmExecQueryStatPerDb_diff'. (PCS = Persistent Counter Store) <br>" +
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
