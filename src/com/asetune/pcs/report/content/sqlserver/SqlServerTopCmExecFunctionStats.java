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
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.cm.CountersModel;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.DictCompression;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.SparklineHelper;
import com.asetune.pcs.report.content.SparklineHelper.AggType;
import com.asetune.pcs.report.content.SparklineHelper.DataSource;
import com.asetune.pcs.report.content.SparklineHelper.SparkLineParams;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public class SqlServerTopCmExecFunctionStats
extends SqlServerAbstract
{
	private static Logger _logger = Logger.getLogger(SqlServerTopCmExecFunctionStats.class);

	private ResultSetTableModel _shortRstm;
	private ExecutionPlanCollection _planCollection;
	private List<String>        _miniChartJsList = new ArrayList<>();

	public SqlServerTopCmExecFunctionStats(DailySummaryReportAbstract reportingInstance)
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
		// Get a description of this section, and column names
		sb.append(getSectionDescriptionHtml(_shortRstm, true));

//		sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
		sb.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
		sb.append(toHtmlTable(_shortRstm));

		// Write HTML/JavaScript Code for the Execution Plan...
		if (isFullMessageType())
		{
			if (_planCollection != null)
				_planCollection.writeMessageText(sb);
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
		return "Top Function Calls (order by: total_worker_time, origin: CmExecFunctionStats/dm_exec_function_stats)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmExecFunctionStats_diff" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		 // just to get Column names
		String dummySql = "select * from [CmExecFunctionStats_diff] where 1 = 2";
		ResultSetTableModel dummyRstm = executeQuery(conn, dummySql, false, "metadata");
		if (dummyRstm == null)
		{
			String msg = "Table 'CmExecFunctionStats_diff' did not exist. So Performance Counters for this hasn't been sampled during this period.";

			//addMessage(msg);
			setProblemException(new Exception(msg));

			_shortRstm = ResultSetTableModel.createEmpty("CmExecFunctionStats");
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
		
//		RS> Col# Label                JDBC Type Name           Guessed DBMS type Source Table
//		RS> ---- -------------------- ------------------------ ----------------- ------------
//		RS> 1    DbName               java.sql.Types.NVARCHAR  nvarchar(128)     -none-      
//		RS> 2    SchemaName           java.sql.Types.NVARCHAR  nvarchar(128)     -none-      
//		RS> 3    ObjectName           java.sql.Types.NVARCHAR  nvarchar(128)     -none-      
//		RS> 4    database_id          java.sql.Types.INTEGER   int               -none-      
//		RS> 5    object_id            java.sql.Types.INTEGER   int               -none-      
//		RS> 6    type                 java.sql.Types.CHAR      char(2)           -none-      
//		RS> 7    type_desc            java.sql.Types.NVARCHAR  nvarchar(60)      -none-      
//		RS> 8    sql_handle           java.sql.Types.VARBINARY varbinary(128)    -none-      
//		RS> 9    plan_handle          java.sql.Types.VARBINARY varbinary(128)    -none-      
//		RS> 10   cached_time          java.sql.Types.TIMESTAMP datetime          -none-      
//		RS> 11   last_execution_time  java.sql.Types.TIMESTAMP datetime          -none-      
//		RS> 12   execution_count      java.sql.Types.BIGINT    bigint            -none-      
//		RS> 13   total_worker_time    java.sql.Types.BIGINT    bigint            -none-      
//		RS> 14   last_worker_time     java.sql.Types.BIGINT    bigint            -none-      
//		RS> 15   min_worker_time      java.sql.Types.BIGINT    bigint            -none-      
//		RS> 16   max_worker_time      java.sql.Types.BIGINT    bigint            -none-      
//		RS> 17   total_physical_reads java.sql.Types.BIGINT    bigint            -none-      
//		RS> 18   last_physical_reads  java.sql.Types.BIGINT    bigint            -none-      
//		RS> 19   min_physical_reads   java.sql.Types.BIGINT    bigint            -none-      
//		RS> 20   max_physical_reads   java.sql.Types.BIGINT    bigint            -none-      
//		RS> 21   total_logical_writes java.sql.Types.BIGINT    bigint            -none-      
//		RS> 22   last_logical_writes  java.sql.Types.BIGINT    bigint            -none-      
//		RS> 23   min_logical_writes   java.sql.Types.BIGINT    bigint            -none-      
//		RS> 24   max_logical_writes   java.sql.Types.BIGINT    bigint            -none-      
//		RS> 25   total_logical_reads  java.sql.Types.BIGINT    bigint            -none-      
//		RS> 26   last_logical_reads   java.sql.Types.BIGINT    bigint            -none-      
//		RS> 27   min_logical_reads    java.sql.Types.BIGINT    bigint            -none-      
//		RS> 28   max_logical_reads    java.sql.Types.BIGINT    bigint            -none-      
//		RS> 29   total_elapsed_time   java.sql.Types.BIGINT    bigint            -none-      
//		RS> 30   last_elapsed_time    java.sql.Types.BIGINT    bigint            -none-      
//		RS> 31   min_elapsed_time     java.sql.Types.BIGINT    bigint            -none-      
//		RS> 32   max_elapsed_time     java.sql.Types.BIGINT    bigint            -none-      
//		RS> 33   total_spills         java.sql.Types.BIGINT    bigint            -none-      
//		RS> 34   last_spills          java.sql.Types.BIGINT    bigint            -none-      
//		RS> 35   min_spills           java.sql.Types.BIGINT    bigint            -none-      
//		RS> 36   max_spills           java.sql.Types.BIGINT    bigint            -none-      		
		
		String col_total_elapsed_time_ms__sum           = !dummyRstm.hasColumnNoCase("total_elapsed_time"             ) ? "" : "    ,sum([total_elapsed_time]/1000.0)       as [total_elapsed_time_ms__sum]           \n"; 
		String col_total_worker_time_ms__sum            = !dummyRstm.hasColumnNoCase("total_worker_time"              ) ? "" : "    ,sum([total_worker_time]/1000.0)        as [total_worker_time_ms__sum]            \n"; 
		String col_total_physical_reads__sum            = !dummyRstm.hasColumnNoCase("total_physical_reads"           ) ? "" : "    ,sum([total_physical_reads])            as [total_physical_reads__sum]            \n"; 
		String col_total_logical_writes__sum            = !dummyRstm.hasColumnNoCase("total_logical_writes"           ) ? "" : "    ,sum([total_logical_writes])            as [total_logical_writes__sum]            \n"; 
		String col_total_logical_reads__sum             = !dummyRstm.hasColumnNoCase("total_logical_reads"            ) ? "" : "    ,sum([total_logical_reads])             as [total_logical_reads__sum]             \n"; 
//		String col_total_clr_time_ms__sum               = !dummyRstm.hasColumnNoCase("total_clr_time"                 ) ? "" : "    ,sum([total_clr_time]/1000.0)           as [total_clr_time_ms__sum]               \n"; 
//		String col_total_rows__sum                      = !dummyRstm.hasColumnNoCase("total_rows"                     ) ? "" : "    ,sum([total_rows])                      as [total_rows__sum]                      \n"; 
//		String col_total_dop__sum                       = !dummyRstm.hasColumnNoCase("total_dop"                      ) ? "" : "    ,sum([total_dop])                       as [total_dop__sum]                       \n"; 
//		String col_total_grant_kb__sum                  = !dummyRstm.hasColumnNoCase("total_grant_kb"                 ) ? "" : "    ,sum([total_grant_kb])                  as [total_grant_kb__sum]                  \n"; 
//		String col_total_used_grant_kb__sum             = !dummyRstm.hasColumnNoCase("total_used_grant_kb"            ) ? "" : "    ,sum([total_used_grant_kb])             as [total_used_grant_kb__sum]             \n"; 
//		String col_total_ideal_grant_kb__sum            = !dummyRstm.hasColumnNoCase("total_ideal_grant_kb"           ) ? "" : "    ,sum([total_ideal_grant_kb])            as [total_ideal_grant_kb__sum]            \n"; 
//		String col_total_reserved_threads__sum          = !dummyRstm.hasColumnNoCase("total_reserved_threads"         ) ? "" : "    ,sum([total_reserved_threads])          as [total_reserved_threads__sum]          \n"; 
//		String col_total_used_threads__sum              = !dummyRstm.hasColumnNoCase("total_used_threads"             ) ? "" : "    ,sum([total_used_threads])              as [total_used_threads__sum]              \n"; 
//		String col_total_columnstore_segment_reads__sum = !dummyRstm.hasColumnNoCase("total_columnstore_segment_reads") ? "" : "    ,sum([total_columnstore_segment_reads]) as [total_columnstore_segment_reads__sum] \n"; 
//		String col_total_columnstore_segment_skips__sum = !dummyRstm.hasColumnNoCase("total_columnstore_segment_skips") ? "" : "    ,sum([total_columnstore_segment_skips]) as [total_columnstore_segment_skips__sum] \n"; 
//		String col_total_spills__sum                    = !dummyRstm.hasColumnNoCase("total_spills"                   ) ? "" : "    ,sum([total_spills])                    as [total_spills__sum]                    \n"; 
//		String col_total_page_server_reads__sum         = !dummyRstm.hasColumnNoCase("total_page_server_reads"        ) ? "" : "    ,sum([total_page_server_reads])         as [total_page_server_reads__sum]         \n"; 

//		String col_AvgElapsedTimeMs                     = !dummyRstm.hasColumnNoCase("total_elapsed_time"             ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgElapsedTimeMs]           \n";
//		String col_AvgWorkerTimeMs                      = !dummyRstm.hasColumnNoCase("total_worker_time"              ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgWorkerTimeMs]            \n";
//		String col_AvgPhysicalReads                     = !dummyRstm.hasColumnNoCase("total_physical_reads"           ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgPhysicalReads]           \n";
//		String col_AvgLogicalWrites                     = !dummyRstm.hasColumnNoCase("total_logical_writes"           ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgLogicalWrites]           \n";
//		String col_AvgLogicalReads                      = !dummyRstm.hasColumnNoCase("total_logical_reads"            ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgLogicalReads]            \n";
////		String col_AvgClrTimeMs                         = !dummyRstm.hasColumnNoCase("total_clr_time"                 ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgClrTimeMs]               \n";
////		String col_AvgRows                              = !dummyRstm.hasColumnNoCase("total_rows"                     ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgRows]                    \n";
////		String col_AvgDop                               = !dummyRstm.hasColumnNoCase("total_dop"                      ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgDop]                     \n";
////		String col_AvgGrantKb                           = !dummyRstm.hasColumnNoCase("total_grant_kb"                 ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgGrantKb]                 \n";
////		String col_AvgUsedGrantKb                       = !dummyRstm.hasColumnNoCase("total_used_grant_kb"            ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgUsedGrantKb]             \n";
////		String col_AvgIdealGrantKb                      = !dummyRstm.hasColumnNoCase("total_ideal_grant_kb"           ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgIdealGrantKb]            \n";
////		String col_AvgReservedThreads                   = !dummyRstm.hasColumnNoCase("total_reserved_threads"         ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgReservedThreads]         \n";
////		String col_AvgUsedThreads                       = !dummyRstm.hasColumnNoCase("total_used_threads"             ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgUsedThreads]             \n";
////		String col_AvgColumnstoreSegmentReads           = !dummyRstm.hasColumnNoCase("total_columnstore_segment_reads") ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgColumnstoreSegmentReads] \n"; 
////		String col_AvgColumnstoreSegmentSkips           = !dummyRstm.hasColumnNoCase("total_columnstore_segment_skips") ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgColumnstoreSegmentSkips] \n"; 
////		String col_AvgSpills                            = !dummyRstm.hasColumnNoCase("total_spills"                   ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgSpills]                  \n";
////		String col_AvgPageServerReads                   = !dummyRstm.hasColumnNoCase("total_page_server_reads"        ) ? "" : "    ,cast(-1 as numeric(16,1)) as [AvgPageServerReads]         \n"; 

		String col_AvgElapsedTimeMs                     = !dummyRstm.hasColumnNoCase("total_elapsed_time"             ) ? "" : "    ,cast( sum([total_elapsed_time]/1000.0)                      * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgElapsedTimeMs]           \n";
		String col_AvgWorkerTimeMs                      = !dummyRstm.hasColumnNoCase("total_worker_time"              ) ? "" : "    ,cast( sum([total_worker_time]/1000.0)                       * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgWorkerTimeMs]            \n";
		String col_AvgPhysicalReads                     = !dummyRstm.hasColumnNoCase("total_physical_reads"           ) ? "" : "    ,cast( sum([total_physical_reads])                           * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgPhysicalReads]           \n";
		String col_AvgLogicalWrites                     = !dummyRstm.hasColumnNoCase("total_logical_writes"           ) ? "" : "    ,cast( sum([total_logical_writes])                           * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgLogicalWrites]           \n";
		String col_AvgLogicalReads                      = !dummyRstm.hasColumnNoCase("total_logical_reads"            ) ? "" : "    ,cast( sum([total_logical_reads])                            * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgLogicalReads]            \n";
//		String col_AvgClrTimeMs                         = !dummyRstm.hasColumnNoCase("total_clr_time"                 ) ? "" : "    ,cast( sum([total_clr_time]/1000.0)                          * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgClrTimeMs]               \n";
//		String col_AvgRows                              = !dummyRstm.hasColumnNoCase("total_rows"                     ) ? "" : "    ,cast( sum([total_rows])                                     * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgRows]                    \n";
//		String col_AvgDop                               = !dummyRstm.hasColumnNoCase("total_dop"                      ) ? "" : "    ,cast( sum([total_dop])                                      * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgDop]                     \n";
//		String col_AvgGrantKb                           = !dummyRstm.hasColumnNoCase("total_grant_kb"                 ) ? "" : "    ,cast( sum([total_grant_kb])                                 * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgGrantKb]                 \n";
//		String col_AvgUsedGrantKb                       = !dummyRstm.hasColumnNoCase("total_used_grant_kb"            ) ? "" : "    ,cast( sum([total_used_grant_kb])                            * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgUsedGrantKb]             \n";
//		String col_AvgIdealGrantKb                      = !dummyRstm.hasColumnNoCase("total_ideal_grant_kb"           ) ? "" : "    ,cast( sum([total_ideal_grant_kb])                           * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgIdealGrantKb]            \n";
//		String col_AvgReservedThreads                   = !dummyRstm.hasColumnNoCase("total_reserved_threads"         ) ? "" : "    ,cast( sum([total_reserved_threads])                         * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgReservedThreads]         \n";
//		String col_AvgUsedThreads                       = !dummyRstm.hasColumnNoCase("total_used_threads"             ) ? "" : "    ,cast( sum([total_used_threads])                             * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgUsedThreads]             \n";
//		String col_AvgColumnstoreSegmentReads           = !dummyRstm.hasColumnNoCase("total_columnstore_segment_reads") ? "" : "    ,cast( sum([total_columnstore_segment_reads])                * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgColumnstoreSegmentReads] \n"; 
//		String col_AvgColumnstoreSegmentSkips           = !dummyRstm.hasColumnNoCase("total_columnstore_segment_skips") ? "" : "    ,cast( sum([total_columnstore_segment_skips])                * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgColumnstoreSegmentSkips] \n"; 
//		String col_AvgSpills                            = !dummyRstm.hasColumnNoCase("total_spills"                   ) ? "" : "    ,cast( sum([total_spills])                                   * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgSpills]                  \n";
//		String col_AvgPageServerReads                   = !dummyRstm.hasColumnNoCase("total_page_server_reads"        ) ? "" : "    ,cast( sum([total_page_server_reads])                        * 1.0 / nullif(sum([execution_count]), 0) as numeric(19,1)) as [AvgPageServerReads]         \n"; 

		String col_elapsed_time__chart                  = !dummyRstm.hasColumnNoCase("total_elapsed_time"             ) ? "" : "    ,cast('' as varchar(512))  as [elapsed_time__chart]   \n"; 
		String col_worker_time__chart                   = !dummyRstm.hasColumnNoCase("total_worker_time"              ) ? "" : "    ,cast('' as varchar(512))  as [worker_time__chart]    \n"; 
		String col_physical_reads__chart                = !dummyRstm.hasColumnNoCase("total_physical_reads"           ) ? "" : "    ,cast('' as varchar(512))  as [physical_reads__chart] \n"; 
		String col_logical_reads__chart                 = !dummyRstm.hasColumnNoCase("total_logical_reads"            ) ? "" : "    ,cast('' as varchar(512))  as [logical_reads__chart]  \n"; 
//		String col_dop__chart                           = !dummyRstm.hasColumnNoCase("total_dop"                      ) ? "" : "    ,cast('' as varchar(512))  as [dop__chart]            \n"; 
//		String col_grant_kb__chart                      = !dummyRstm.hasColumnNoCase("total_grant_kb"                 ) ? "" : "    ,cast('' as varchar(512))  as [grant_kb__chart]       \n"; 
//		String col_spills__chart                        = !dummyRstm.hasColumnNoCase("total_spills"                   ) ? "" : "    ,cast('' as varchar(512))  as [spills__chart]         \n"; 
		

//		int topRows = localConf.getIntProperty(this.getClass().getSimpleName()+".top", 20);
		int topRows = getTopRows();

		String orderByCol = "[total_worker_time_ms__sum]";
//		String orderByCol = "[samples__count]";
//		if (dummyRstm.hasColumnNoCase("total_worker_time"))     { orderByCol = "[total_worker_time__sum]";     }

		// Check if table "CmPgStatements_diff" has Dictionary Compressed Columns (any columns ends with "$dcc$")
		boolean hasDictCompCols = false;
		try {
			hasDictCompCols = DictCompression.hasCompressedColumnNames(conn, null, "CmExecFunctionStats_diff");
		} catch (SQLException ex) {
			_logger.error("Problems checking for Dictionary Compressed Columns in table 'CmExecFunctionStats_diff'.", ex);
		}
		
		String col_SqlText = "SqlText";
		if (hasDictCompCols)
			col_SqlText = "SqlText$dcc$";


		String sql = getCmDiffColumnsAsSqlComment("CmExecFunctionStats")
			    + "select top " + topRows + " \n"
			    + "     [DbName] \n"
			    + "    ,[SchemaName] \n"
			    + "    ,[ObjectName] \n"
			    + "    ,cast('' as varchar(30))                as [ExecPlan] \n"
//			    + "    ,max([query_plan_hash])                 as [query_plan_hash] \n"
			    + "    \n"
			    + "    ,cast('' as varchar(512))               as [execution_count__chart] \n"
			    + "    ,sum([execution_count])                 as [execution_count__sum] \n"
			    + "    ,min([cached_time])                     as [cached_time__min] \n"
			    + "    ,max([last_execution_time])             as [last_execution_time__max] \n"
			    + "    \n"
			    + col_elapsed_time__chart            
			    + col_total_elapsed_time_ms__sum           + col_AvgElapsedTimeMs           
			    + col_worker_time__chart            
			    + col_total_worker_time_ms__sum            + col_AvgWorkerTimeMs            
			    + col_physical_reads__chart
			    + col_total_physical_reads__sum            + col_AvgPhysicalReads           
			    + col_total_logical_writes__sum            + col_AvgLogicalWrites           
			    + col_logical_reads__chart
			    + col_total_logical_reads__sum             + col_AvgLogicalReads            
//			    + col_total_clr_time_ms__sum               + col_AvgClrTimeMs               
//			    + col_total_rows__sum                      + col_AvgRows                    
//			    + col_dop__chart
//			    + col_total_dop__sum                       + col_AvgDop                     
//			    + col_grant_kb__chart
//			    + col_total_grant_kb__sum                  + col_AvgGrantKb                 
//			    + col_total_used_grant_kb__sum             + col_AvgUsedGrantKb             
//			    + col_total_ideal_grant_kb__sum            + col_AvgIdealGrantKb            
//			    + col_total_reserved_threads__sum          + col_AvgReservedThreads         
//			    + col_total_used_threads__sum              + col_AvgUsedThreads             
//			    + col_total_columnstore_segment_reads__sum + col_AvgColumnstoreSegmentReads 
//			    + col_total_columnstore_segment_skips__sum + col_AvgColumnstoreSegmentSkips 
//			    + col_spills__chart
//			    + col_total_spills__sum                    + col_AvgSpills                  
//			    + col_total_page_server_reads__sum         + col_AvgPageServerReads         
			    + "    \n"
			    
			    + "    ,max([plan_handle])                     as [plan_handle] \n"
			    
				+ "    ,count(*)                               as [samples__count] \n"
			    + "    ,min([SessionSampleTime])               as [SessionSampleTime__min] \n"
			    + "    ,max([SessionSampleTime])               as [SessionSampleTime__max] \n"
			    + "    ,cast('' as varchar(30))                as [Duration] \n"
//				+ col_newDiffRow_sum
//			    + "    ,sum([CmSampleMs])                      as [CmSampleMs__sum] \n"

//			    + "    ,max([SqlText])                         as [SqlText] \n"
//			    + "    ,max([" + col_SqlText +"])                    as [SqlText] \n"
				+ "from [CmExecFunctionStats_diff] \n"
				+ "where 1 = 1 \n"
				+ sql_and_skipNewOrDiffRateRows // [CmNewDiffRateRow] = 0 -- only records that has been diff calculations (not first time seen, when it swaps in/out due to execution every x minute)
				+ "  and [execution_count] > 0 \n"
//				+ "  and [AvgServ_ms] > " + _aboveServiceTime + " \n"
//				+ "  and [TotalIOs]   > " + _aboveTotalIos    + " \n"
				+ getReportPeriodSqlWhere()
				+ "group by [DbName], [SchemaName], [ObjectName] \n"
				+ "order by " + orderByCol + " desc \n"
			    + "";		
		
		_shortRstm = executeQuery(conn, sql, false, "CmExecFunctionStats");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("CmExecFunctionStats");
			return;
		}
		else
		{
			// Highlight sort column
			String orderByCol_noBrackets = orderByCol.replace("[", "").replace("]", "");
			_shortRstm.setHighlightSortColumns(orderByCol_noBrackets);

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
			_planCollection = new ExecutionPlanCollection(this, _shortRstm, this.getClass().getSimpleName());
			_planCollection.getPlans(conn, null, "plan_handle");
			_planCollection.substituteWithLinks(null, "plan_handle", "ExecPlan", "view plan", "--not-found--"); // this needs to be first, no modification of plan_handle cell content
			_planCollection.substituteWithLinks(null, "plan_handle");                                         // this needs to be "last", since it changes of 'plan_handle' cell content
			

			//-----------------------------------------------
			// Mini Chart on "..."
			//-----------------------------------------------
			String whereKeyColumn = "DbName, SchemaName, ObjectName"; 

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("execution_count__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecFunctionStats_diff")
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
					.setDbmsTableName            ("CmExecFunctionStats_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("total_elapsed_time")   
					.setDbmsDataValueColumnName  ("sum([total_elapsed_time]/1000.0) / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1) // MS
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
					.setSparklineTooltipPostfix  ("Average 'elapsed_time' in in milliseconds for below period")
					.validate()));
			}

			if (StringUtil.hasValue(col_worker_time__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("worker_time__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecFunctionStats_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("total_worker_time")   
					.setDbmsDataValueColumnName  ("sum([total_worker_time]/1000.0) / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1) // MS
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
					.setSparklineTooltipPostfix  ("Average 'worker_time' in in milliseconds for below period")
					.validate()));
			}

			if (StringUtil.hasValue(col_physical_reads__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("physical_reads__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecFunctionStats_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("total_physical_reads")
					.setDbmsDataValueColumnName  ("sum(1.0*[total_physical_reads]) / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
					.setSparklineTooltipPostfix  ("Average of 'physical_reads' in below period")
					.validate()));
			}

			if (StringUtil.hasValue(col_logical_reads__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("logical_reads__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecFunctionStats_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("total_logical_reads")
					.setDbmsDataValueColumnName  ("sum(1.0*[total_logical_reads]) / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
					.setSparklineTooltipPostfix  ("Average of 'logical_reads' in below period")
					.validate()));
			}

//			if (StringUtil.hasValue(col_dop__chart))
//			{
//				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
//					SparkLineParams.create       (DataSource.CounterModel)
//					.setHtmlChartColumnName      ("dop__chart")
//					.setHtmlWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsTableName            ("CmExecFunctionStats_diff")
//					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("CASE WHEN sum([execution_count]) = 0 THEN 0.0 ELSE sum([total_dop]*1.0)/sum([execution_count]*1.0) END").setGroupDataAggregationType(AggType.USER_PROVIDED)
//					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
//					.setSparklineTooltipPostfix  ("MAX 'DOP - Degree Of Paralism' in below period")
//					.validate()));
//			}
//
//			if (StringUtil.hasValue(col_grant_kb__chart))
//			{
//				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
//					SparkLineParams.create       (DataSource.CounterModel)
//					.setHtmlChartColumnName      ("grant_kb__chart")
//					.setHtmlWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsTableName            ("CmExecFunctionStats_diff")
//					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("CASE WHEN sum([execution_count]) = 0 THEN 0.0 ELSE sum([total_grant_kb]*1.0)/sum([execution_count]*1.0) END").setGroupDataAggregationType(AggType.USER_PROVIDED)
//					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
//					.setSparklineTooltipPostfix  ("MAX 'grant_kb' in below period")
//					.validate()));
//			}
//
//			if (StringUtil.hasValue(col_spills__chart))
//			{
//				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
//					SparkLineParams.create       (DataSource.CounterModel)
//					.setHtmlChartColumnName      ("spills__chart")
//					.setHtmlWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsTableName            ("CmExecFunctionStats_diff")
//					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("total_spills")
//					.setDbmsDataValueColumnName  ("sum(1.0*[total_spills]) / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
//					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
//					.setSparklineTooltipPostfix  ("Average of 'spills' in below period")
//					.validate()));
//			}
		}
	}

//	private void calculateAvg(ResultSetTableModel rstm)
//	{
//		for (int r=0; r<rstm.getRowCount(); r++)
//		{
//			calculateAvg(rstm, r, "execution_count__sum", "total_elapsed_time_ms__sum"          , "AvgElapsedTimeMs");          
//			calculateAvg(rstm, r, "execution_count__sum", "total_worker_time_ms__sum"           , "AvgWorkerTimeMs");           
//			calculateAvg(rstm, r, "execution_count__sum", "total_physical_reads__sum"           , "AvgPhysicalReads");          
//			calculateAvg(rstm, r, "execution_count__sum", "total_logical_writes__sum"           , "AvgLogicalWrites");          
//			calculateAvg(rstm, r, "execution_count__sum", "total_logical_reads__sum"            , "AvgLogicalReads");           
//			calculateAvg(rstm, r, "execution_count__sum", "total_clr_time_ms__sum"              , "AvgClrTimeMs");              
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
	

	/**
	 * Set descriptions for the table, and the columns
	 */
	private void setSectionDescription(ResultSetTableModel rstm)
	{
		if (rstm == null)
			return;
		
		// Section description
		rstm.setDescription(
				"Top Function (and it's Query Plan)...  (ordered by: total_worker_time__sum) <br>" +
				"<br>" +
				"SqlServer Source table is 'dm_exec_function_stats'. <br>" +
				"PCS Source table is 'CmExecFunctionStats_diff'. (PCS = Persistent Counter Store) <br>" +
				"");
	}
}

