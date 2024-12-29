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
import java.util.ArrayList;
import java.util.List;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.SparklineHelper;
import com.dbxtune.pcs.report.content.SparklineHelper.AggType;
import com.dbxtune.pcs.report.content.SparklineHelper.DataSource;
import com.dbxtune.pcs.report.content.SparklineHelper.SparkLineParams;
import com.dbxtune.pcs.report.content.ase.AseAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

public class SqlServerTopCmIndexOpStat extends AseAbstract
{
//	private static Logger _logger = Logger.getLogger(AseTopCmCachedProcs.class);

	private ResultSetTableModel _shortRstm;
	private List<String>        _miniChartJsList = new ArrayList<>();

	public enum ReportType
	{
		BY_LOCKS, 
		BY_WAITS, 
		BY_CRUD,
		BY_IO
	};
	private ReportType _reportType;
	private String _orderByCol = "-unknown-";
	
	public SqlServerTopCmIndexOpStat(DailySummaryReportAbstract reportingInstance, ReportType reportType)
	{
		super(reportingInstance);

		_reportType = reportType;

		if (ReportType.BY_LOCKS.equals(_reportType)) _orderByCol = "lock_count__sum";
		if (ReportType.BY_WAITS.equals(_reportType)) _orderByCol = "lock_wait_in_ms__sum";
		if (ReportType.BY_CRUD .equals(_reportType)) _orderByCol = "leaf_ins_upd_del_count__sum";
		if (ReportType.BY_IO   .equals(_reportType)) _orderByCol = "page_io_latch_wait_count__sum";
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
		if (_shortRstm.getRowCount() == 0)
		{
			sb.append("No rows found <br>\n");
		}
		else
		{
			// Get a description of this section, and column names
			sb.append(getSectionDescriptionHtml(_shortRstm, true));

//			sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
			sb.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
			sb.append(toHtmlTable(_shortRstm));
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
		return "Top TABLE/INDEX Activity - " + _reportType + " (order by: " + _orderByCol + ", origin: CmIndexOpStat / dm_db_index_operational_stats)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmIndexOpStat_diff" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		int topRows = getTopRows();
//		int havingAbove = 1000;

//		 // just to get Column names
//		String dummySql = "select * from [CmIndexOpStat_diff] where 1 = 2";
//		ResultSetTableModel dummyRstm = executeQuery(conn, dummySql, true, "metadata");

		// Create Column selects, but only if the column exists in the PCS Table
//		String Inserts_sum               = !dummyRstm.hasColumnNoCase("Inserts"              ) ? "" : "    ,sum([Inserts])                                        as [Inserts_sum]               -- 16.0 \n"; 
//		String Updates_sum               = !dummyRstm.hasColumnNoCase("Updates"              ) ? "" : "    ,sum([Updates])                                        as [Updates_sum]               -- 16.0 \n"; 
//		String Deletes_sum               = !dummyRstm.hasColumnNoCase("Deletes"              ) ? "" : "    ,sum([Deletes])                                        as [Deletes_sum]               -- 16.0 \n"; 
//		String Scans_sum                 = !dummyRstm.hasColumnNoCase("Scans"                ) ? "" : "    ,sum([Scans])                                          as [Scans_sum]                 -- 16.0 \n"; 

		String havingSql = "having [" + _orderByCol + "] > 0 \n";
		
		String sql = getCmDiffColumnsAsSqlComment("CmIndexOpStat")
			    + "select top " + topRows + " \n"
			    + "     [DbName]                                                                        as [DbName] \n"
//			    + "    ,[SchemaName]                                                                    as [SchemaName] \n"
//			    + "    ,[TableName]                                                                     as [TableName] \n"
//			    + "    ,[IndexName]                                                                     as [IndexName] \n"
			    + "    ,coalesce([SchemaName], '-unknown-')                                             as [SchemaName] \n"
			    + "    ,coalesce([TableName] , 'id:'||cast([object_id] as varchar(20)))                 as [TableName] \n"
			    + "    ,coalesce([IndexName] , 'id:'||cast([index_id] as varchar(20)))                  as [IndexName] \n"
                                                                                                        
			    + "    ,cast('' as varchar(512))                                                        as [lock_count__chart] \n"
			    + "    ,sum([row_lock_count]) + sum([page_lock_count])                                  as [lock_count__sum] \n"
			    + "    ,sum([row_lock_count])                                                           as [row_lock_count] \n"
			    + "    ,sum([page_lock_count])                                                          as [page_lock_count] \n"

			    + "    ,cast('' as varchar(512))                                                        as [leaf_ins_upd_del_count__chart] \n"
			    + "    ,sum([leaf_insert_count]) + sum([leaf_update_count]) + sum([leaf_delete_count])  as [leaf_ins_upd_del_count__sum] \n"
			    + "    ,sum([leaf_insert_count])                                                        as [leaf_insert_count__sum] \n"
			    + "    ,sum([leaf_update_count])                                                        as [leaf_update_count__sum] \n"
			    + "    ,sum([leaf_delete_count])                                                        as [leaf_delete_count__sum] \n"
			    + "    ,sum([leaf_ghost_count])                                                         as [leaf_ghost_count__sum] \n"
			    
			    + "    ,cast('' as varchar(512))                                                        as [range_scan_count__chart] \n"
			    + "    ,sum([range_scan_count])                                                         as [range_scan_count__sum] \n"
			    
			    + "    ,cast('' as varchar(512))                                                        as [singleton_lookup_count__chart] \n"
			    + "    ,sum([singleton_lookup_count])                                                   as [singleton_lookup_count__sum] \n"

			    + "    ,sum([forwarded_fetch_count])                                                    as [forwarded_fetch_count__sum] \n"

			    + "    ,sum([row_overflow_fetch_in_pages])                                              as [row_overflow_fetch_in_pages__sum] \n"

			    + "    ,sum([lob_fetch_in_pages])                                                       as [lob_fetch_in_pages__sum] \n"
			    + "    ,sum([column_value_push_off_row_count])                                          as [column_value_push_off_row_count__sum] \n"
			    + "    ,sum([column_value_pull_in_row_count])                                           as [column_value_pull_in_row_count__sum] \n"

			    + "    ,cast('' as varchar(512))                                                        as [lock_wait_in_ms__chart] \n"
			    + "    ,sum([row_lock_wait_count]) + sum([page_lock_wait_count])                        as [lock_wait_count__sum] \n"
			    + "    ,sum([row_lock_wait_in_ms]) + sum([page_lock_wait_in_ms])                        as [lock_wait_in_ms__sum] \n"

			    + "    ,sum([row_lock_wait_count])                                                      as [row_lock_wait_count__sum] \n"
			    + "    ,sum([row_lock_wait_in_ms])                                                      as [row_lock_wait_in_ms__sum] \n"
			    + "    ,CASE WHEN sum([row_lock_wait_count]) = 0 THEN 0 \n"
			    + "          ELSE sum([row_lock_wait_in_ms]) / sum([row_lock_wait_count]) END           as [avg_row_lock_wait_in_ms] \n"

			    + "    ,sum([page_lock_wait_count])                                                     as [page_lock_wait_count__sum] \n"
			    + "    ,sum([page_lock_wait_in_ms])                                                     as [page_lock_wait_in_ms__sum] \n"
			    + "    ,CASE WHEN sum([page_lock_wait_count]) = 0 THEN 0 \n"
			    + "          ELSE sum([page_lock_wait_in_ms]) / sum([page_lock_wait_count]) END         as [avg_row_lock_wait_in_ms] \n"
			    
			    + "    ,cast('' as varchar(512))                                                        as [page_latch_wait_in_ms__chart] \n"
			    + "    ,sum([page_latch_wait_count])                                                    as [page_latch_wait_count__sum] \n"
			    + "    ,sum([page_latch_wait_in_ms])                                                    as [page_latch_wait_in_ms__sum] \n"
			    + "    ,CASE WHEN sum([page_latch_wait_count]) = 0 THEN 0 \n"
			    + "          ELSE sum([page_latch_wait_in_ms]) / sum([page_latch_wait_count]) END       as [avg_page_latch_wait_in_ms] \n"

			    + "    ,cast('' as varchar(512))                                                        as [page_io_latch_wait_in_ms__chart] \n"
			    + "    ,sum([page_io_latch_wait_count])                                                 as [page_io_latch_wait_count__sum] \n"
			    + "    ,sum([page_io_latch_wait_in_ms])                                                 as [page_io_latch_wait_in_ms__sum] \n"
			    + "    ,CASE WHEN sum([page_io_latch_wait_count]) = 0 THEN 0 \n"
			    + "          ELSE sum([page_io_latch_wait_in_ms]) / sum([page_io_latch_wait_count]) END as [avg_page_io_latch_wait_in_ms] \n"
			    
			    + "    ,sum([tree_page_latch_wait_count])                                               as [tree_page_latch_wait_count__sum] \n"
			    + "    ,sum([tree_page_latch_wait_in_ms])                                               as [tree_page_latch_wait_in_ms__sum] \n"
			    + "    ,sum([tree_page_io_latch_wait_count])                                            as [tree_page_io_latch_wait_count__sum] \n"
			    + "    ,sum([tree_page_io_latch_wait_in_ms])                                            as [tree_page_io_latch_wait_in_ms__sum] \n"

			    + "    ,sum([page_compression_attempt_count])                                           as [page_compression_attempt_count__sum] \n"
			    + "    ,sum([page_compression_success_count])                                           as [page_compression_success_count__sum] \n"

			    + "    ,sum([index_lock_promotion_attempt_count])                                       as [index_lock_promotion_attempt_count__sum] \n"
			    + "    ,sum([index_lock_promotion_count])                                               as [index_lock_promotion_count__sum] \n"
//			    + "    ,sum([])                                           as [__sum] \n"
//			    + "    ,sum([])                                           as [__sum] \n"
			    
			    + "    ,count(*)                                                                        as [samples_count] \n"
			    + "    ,min([CmSampleTime])                                                             as [CmSampleTime_min] \n"
			    + "    ,max([CmSampleTime])                                                             as [CmSampleTime_max] \n"
			    + "    ,cast('' as varchar(30))                                                         as [Duration] \n"
			    + "from [CmIndexOpStat_diff] x \n"
			    + "where 1 = 1 \n"
				+ getReportPeriodSqlWhere()
//			    + "group by [DbName], [SchemaName], [TableName], [IndexName] \n"
				+ "group by [DbName] \n"
				+ "         ,coalesce([SchemaName], '-unknown-') \n"
				+ "         ,coalesce([TableName] , 'id:'||cast([object_id] as varchar(20))) \n"
				+ "         ,coalesce([IndexName] , 'id:'||cast([index_id] as varchar(20))) \n"
			    + havingSql
			    + "order by [" + _orderByCol + "] desc \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, false, "TopTableAccess");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("TopTableAccess");
			return;
		}
		else
		{
			// Highlight sort column
			_shortRstm.setHighlightSortColumns(_orderByCol);

			// Describe the table
			setSectionDescription(_shortRstm);

			// Calculate Duration
			setDurationColumn(_shortRstm, "CmSampleTime_min", "CmSampleTime_max", "Duration");
			
			// Mini Chart on: 
			String whereKeyColumn = "DbName, SchemaName, TableName, IndexName"; 

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("lock_count__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmIndexOpStat_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("sum([row_lock_count]) + sum([page_lock_count])").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Number of '(row+page)_lock_count' in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("leaf_ins_upd_del_count__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmIndexOpStat_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("sum([leaf_insert_count]) + sum([leaf_update_count]) + sum([leaf_delete_count])").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Number of 'leaf_(insert+update+delete)_count' in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("range_scan_count__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmIndexOpStat_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("range_scan_count")   
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("singleton_lookup_count__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmIndexOpStat_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("singleton_lookup_count")   
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("lock_wait_in_ms__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmIndexOpStat_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("sum([row_lock_wait_in_ms]) + sum([page_lock_wait_in_ms])").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Number of '(row+page)_lock_wait_in_ms' in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("page_latch_wait_in_ms__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmIndexOpStat_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("page_latch_wait_in_ms")
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("page_io_latch_wait_in_ms__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmIndexOpStat_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("page_io_latch_wait_in_ms")
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));
		}
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
				"Table/Index Activity (ordered by: " + _orderByCol + ") <br>" +
				"<br>" +
				"What tables/indexes is accessed (locked) the most <br>" +
				"<br>" +
				"ASE Source table is '<dbname>.sys.dm_db_index_operational_stats(DEFAULT, DEFAULT, DEFAULT, DEFAULT)'. <br>" +
				"PCS Source table is 'CmIndexOpStat_diff'. (PCS = Persistent Counter Store) <br>" +
				"<br>" +
				"The report <i>summarizes</i> (min/max/count/sum/avg) all entries/samples from the <i>source_DIFF</i> table. <br>" +
				"Typically the column name <i>postfix</i> will tell you what aggregate function was used. <br>" +
				"");

		// Columns description
		rstm.setColumnDescription("CmSampleTime_min"                        , "First entry was sampled.");
		rstm.setColumnDescription("CmSampleTime_max"                        , "Last entry was sampled.");
		rstm.setColumnDescription("Duration"                                , "Difference between first/last sample");
		rstm.setColumnDescription("DbName"                                  , "Database name");
		rstm.setColumnDescription("SchemaName"                              , "Schema Name");
		rstm.setColumnDescription("TableName"                               , "Table Name");
		rstm.setColumnDescription("IndexName"                               , "HEAP if it's DATA Pages access (without any Clustered Index). otherwise the IndexName. Note: If IndexID = 1, it's DATA Pages but leaf level of a Clustered Index.");

		rstm.setColumnDescription("lock_count__sum"                         , "Cumulative number of Row and Page locks requested. (this is a SUMMARY for the whole recording period)");
		rstm.setColumnDescription("row_lock_count"                          , "Cumulative number of row locks requested. (this is a SUMMARY for the whole recording period)");
		rstm.setColumnDescription("page_lock_count"                         , "Cumulative number of page locks requested. (this is a SUMMARY for the whole recording period)");

		rstm.setColumnDescription("leaf_ins_upd_del_count__sum"             , "Numer of Inserts/Updates/Deletes at the leaf-level (or heap if no clustered index). (this is a SUMMARY for the whole recording period)");
		rstm.setColumnDescription("leaf_insert_count__sum"                  , "Numer of Inserts at the leaf-level (or heap if no clustered index). (this is a SUMMARY for the whole recording period)");
		rstm.setColumnDescription("leaf_update_count__sum"                  , "Numer of Updates at the leaf-level (or heap if no clustered index). (this is a SUMMARY for the whole recording period)");
		rstm.setColumnDescription("leaf_delete_count__sum"                  , "Numer of Deletes at the leaf-level (or heap if no clustered index). (this is a SUMMARY for the whole recording period)");
		rstm.setColumnDescription("leaf_ghost_count__sum"                   , "Cumulative count of leaf-level rows that are marked as deleted, but not yet removed. This count does not include records that are immediately deleted without being marked as ghost. These rows are removed by a cleanup thread at set intervals. This value does not include rows that are retained, because of an outstanding snapshot isolation transaction. (this is a SUMMARY for the whole recording period)");

		rstm.setColumnDescription("range_scan_count__sum"                   , "Cumulative count of range and table scans started on the index or heap. (this is a SUMMARY for the whole recording period)");
		rstm.setColumnDescription("singleton_lookup_count__sum"             , "Cumulative count of single row retrievals from the index or heap. (this is a SUMMARY for the whole recording period)");
		rstm.setColumnDescription("forwarded_fetch_count__sum"              , "Count of rows that were fetched through a forwarding record. (this is a SUMMARY for the whole recording period)");

		rstm.setColumnDescription("row_overflow_fetch_in_pages__sum"        , "Cumulative count of row-overflow data pages retrieved from the ROW_OVERFLOW_DATA allocation unit. These pages contain data stored in columns of type varchar(n), nvarchar(n), varbinary(n), and sql_variant that has been pushed off-row. (this is a SUMMARY for the whole recording period)");

		rstm.setColumnDescription("lob_fetch_in_pages__sum"                 , "Cumulative count of large object (LOB) pages retrieved from the LOB_DATA allocation unit. These pages contain data that is stored in columns of type text, ntext, image, varchar(max), nvarchar(max), varbinary(max), and xml. (this is a SUMMARY for the whole recording period)");
		rstm.setColumnDescription("column_value_push_off_row_count__sum"    , "Cumulative count of column values for LOB data and row-overflow data that is pushed off-row to make an inserted or updated row fit within a page. (this is a SUMMARY for the whole recording period)");
		rstm.setColumnDescription("column_value_pull_in_row_count__sum"     , "Cumulative count of column values for LOB data and row-overflow data that is pulled in-row. This occurs when an update operation frees up space in a record and provides an opportunity to pull in one or more off-row values from the LOB_DATA or ROW_OVERFLOW_DATA allocation units to the IN_ROW_DATA allocation unit. (this is a SUMMARY for the whole recording period)");

		rstm.setColumnDescription("row_lock_wait_count__sum"                , "Cumulative number of times the Database Engine waited on a row lock. (this is a SUMMARY for the whole recording period)");
		rstm.setColumnDescription("row_lock_wait_in_ms__sum"                , "Total number of milliseconds the Database Engine waited on a row lock. (this is a SUMMARY for the whole recording period)");
		rstm.setColumnDescription("avg_row_lock_wait_in_ms"                 , "Average Row Lock Time in milliseconds. [Algorithm: sum([row_lock_wait_in_ms]) / sum([row_lock_wait_count])] (this is a AVERAGE for the whole recording period)");

		rstm.setColumnDescription("page_lock_wait_count__sum"               , "Cumulative number of times the Database Engine waited on a page lock. (this is a SUMMARY for the whole recording period)");
		rstm.setColumnDescription("page_lock_wait_in_ms__sum"               , "Total number of milliseconds the Database Engine waited on a page lock. (this is a SUMMARY for the whole recording period)");
		rstm.setColumnDescription("avg_page_lock_wait_in_ms"                , "Average Page Lock Time in milliseconds. [Algorithm: sum([page_lock_wait_in_ms]) / sum([page_lock_wait_count])] (this is a AVERAGE for the whole recording period)");

		rstm.setColumnDescription("page_latch_wait_count__sum"              , "Cumulative number of times the Database Engine waited, because of latch contention. (this is a SUMMARY for the whole recording period)");
		rstm.setColumnDescription("page_latch_wait_in_ms__sum"              , "Cumulative number of milliseconds the Database Engine waited, because of latch contention. (this is a SUMMARY for the whole recording period)");
		rstm.setColumnDescription("avg_page_latch_wait_in_ms"               , "[Average Page Latch Time in milliseconds. Algorithm: sum([page_latch_wait_in_ms]) / sum([page_latch_wait_count])] (this is a AVERAGE for the whole recording period)");

		rstm.setColumnDescription("page_io_latch_wait_count__sum"           , "(Physical IO)... Cumulative number of times the Database Engine waited on an I/O page latch. (this is a SUMMARY for the whole recording period)");
		rstm.setColumnDescription("page_io_latch_wait_in_ms__sum"           , "(Physical IO Time)... Cumulative number of milliseconds the Database Engine waited on a page I/O latch. (this is a SUMMARY for the whole recording period)");
		rstm.setColumnDescription("avg_page_io_latch_wait_in_ms"            , "(Physical IO Time)... Average IO time in milliseconds. [Algorithm: sum([page_io_latch_wait_in_ms]) / sum([page_io_latch_wait_count])] (this is a AVERAGE for the whole recording period)");

		rstm.setColumnDescription("tree_page_latch_wait_count__sum"         , "Subset of page_latch_wait_count that includes only the upper-level B-tree pages. Always 0 for a heap or columnstore index. (this is a SUMMARY for the whole recording period)");
		rstm.setColumnDescription("tree_page_latch_wait_in_ms__sum"         , "Subset of page_latch_wait_in_ms that includes only the upper-level B-tree pages. Always 0 for a heap or columnstore index. (this is a SUMMARY for the whole recording period)");
		rstm.setColumnDescription("tree_page_io_latch_wait_count__sum"      , "Subset of page_io_latch_wait_count that includes only the upper-level B-tree pages. Always 0 for a heap or columnstore index. (this is a SUMMARY for the whole recording period)");
		rstm.setColumnDescription("tree_page_io_latch_wait_in_ms__sum"      , "Subset of page_io_latch_wait_in_ms that includes only the upper-level B-tree pages. Always 0 for a heap or columnstore index. (this is a SUMMARY for the whole recording period)");

		rstm.setColumnDescription("page_compression_attempt_count__sum"     , "Number of pages that were evaluated for PAGE level compression for specific partitions of a table, index, or indexed view. Includes pages that were not compressed because significant savings could not be achieved. Always 0 for columnstore index. (this is a SUMMARY for the whole recording period)");
		rstm.setColumnDescription("page_compression_success_count__sum"     , "Number of data pages that were compressed by using PAGE compression for specific partitions of a table, index, or indexed view. Always 0 for columnstore index. (this is a SUMMARY for the whole recording period)");

		rstm.setColumnDescription("index_lock_promotion_attempt_count__sum" , "Cumulative number of times the Database Engine tried to escalate locks. (this is a SUMMARY for the whole recording period)");
		rstm.setColumnDescription("index_lock_promotion_count__sum"         , "Cumulative number of times the Database Engine escalated locks. (this is a SUMMARY for the whole recording period)");
	}
}

