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
package com.asetune.pcs.report.content.postgres;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.h2.tools.SimpleResultSet;

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
import com.asetune.utils.HtmlTableProducer.EmptyColumn;

public class PostgresStatementsPerDb 
extends PostgresAbstract
{
//	private static Logger _logger = Logger.getLogger(PostgresStatementsPerDb.class);

	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _miniChartRstm;
	private List<String>        _miniChartJsList = new ArrayList<>();

	private IReportChart _CmPgStatementsSumDb_CallCnt;
	private IReportChart _CmPgStatementsSumDb_TotalTime;

	public PostgresStatementsPerDb(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
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
		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are calls/total_time charts for each Database during the day.",
				"CmPgStatementsSumDb_CallCnt",
				"CmPgStatementsSumDb_TotalTime"
				));

		_CmPgStatementsSumDb_CallCnt  .writeHtmlContent(sb, null, null);
		_CmPgStatementsSumDb_TotalTime.writeHtmlContent(sb, null, null);

		if (isFullMessageType())
		{
			// Get a description of this section, and column names
			sb.append(getSectionDescriptionHtml(_shortRstm, true));

			// Last sample Database Size info
			sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
			sb.append(toHtmlTable(_shortRstm));
		}
		
		// The "sub table" with pivot info on most mini-charts/sparkline
		if (_miniChartRstm != null)
		{
			if (isFullMessageType())
			{
				sb.append("<br>\n");
				sb.append("<details open> \n");
				sb.append("<summary>Details for above Statements (click to collapse) </summary> \n");

				sb.append("<br>\n");
				sb.append("Statements by dbname, Row Count: " + _miniChartRstm.getRowCount() + " (This is the dbname as the in the above table, but without all counter details).<br>\n");
				sb.append(toHtmlTable(_miniChartRstm));

				sb.append("\n");
				sb.append("</details> \n");
			}
			else
			{
				sb.append("<br>\n");
				ResultSetTableModel firstRow = _miniChartRstm.copy(0, 1, null);
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
		return "Statements per Database (origin: CmPgStatementsSumDb / pg_stat_statements)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmPgStatementsSumDb_diff" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
//get SQL columns from CmPgStatementsSumDb
//Also use TopSql report for template
//		String sql = ""
//			+ "select \n"
//			+ "     * \n"
//			+ "from [CmPgStatementsSumDb_diff] \n"
//			+ "where [SessionStartTime] = (select max([SessionStartTime]) from [CmPgStatementsSumDb_diff]) \n"
//			+ "";

		// GET ALL INFO
		String sql = getCmDiffColumnsAsSqlComment("CmPgStatementsSumDb")
			    + "select \n"
			    + "     [dbname] \n"

			    + "    ,cast('' as varchar(512))                                                             as [new_stmnts__chart] \n"
			    + "    ,sum([stmnt_count_diff])                                                              as [new_stmnts__sum] \n"

			    + "    ,cast('' as varchar(512))                                                             as [calls__chart] \n"
			    + "    ,sum([calls])                                                                         as [calls__sum] \n"

			    + "    ,cast('' as varchar(512))                                                             as [total_time__chart] \n"
			    + "    ,CAST( sum([total_time])        AS DECIMAL(20,0) )                                    as [total_time__sum] \n"					// 0 decimals for readability
//			    + "    ,CAST( avg([avg_time_per_call]) AS DECIMAL(19,1) )                                    as [avg_time_per_call__avg] \n"			// 1 decimal (so we can track sub millisecond executions)
			    + "    ,CAST( sum([total_time])          * 1.0 / nullif(sum([calls]), 0) AS DECIMAL(19,1) )  as [total_time__per_call] \n"			// 1 decimal (so we can track sub millisecond executions)

			    + "    ,cast('' as varchar(512))                                                             as [rows__chart] \n"
			    + "    ,sum([rows])                                                                          as [rows__sum] \n"
//			    + "    ,avg([avg_rows_per_call])                                                             as [avg_rows_per_call__avg] \n"
			    + "    ,CAST( sum([rows]) / nullif(sum([calls]), 0) AS DECIMAL(19,1) )                       as [rows__per_call] \n"
			    + " \n"
				+ "    ,cast('' as varchar(512))                                                             as [cache_hit_pct__chart] \n"
			    + "    ,CAST( avg(100.0 * [shared_blks_hit] / nullif([shared_blks_hit] + [shared_blks_read], 0)) AS DECIMAL(5,1) ) as [cache_hit_pct__avg] \n"

				+ "    ,cast('' as varchar(512))                                                             as [logical_reads__chart] \n"
				+ "    ,sum([shared_blks_hit] + [shared_blks_read])                                          as [logical_reads__sum] \n"
				+ "    ,CAST( sum([shared_blks_hit] + [shared_blks_read]) * 1.0 / nullif(sum([calls]), 0) AS DECIMAL(19,1) )      as [logical_reads__per_call] \n"
			    
				+ "    ,cast('' as varchar(512))                                                             as [logical_reads_mb__chart] \n"
				+ "    ,cast(sum([shared_blks_hit] + [shared_blks_read])/128 as bigint)                      as [logical_reads_mb__sum] \n"
				+ "    ,CAST( sum([shared_blks_hit] + [shared_blks_read]) / 128.0 / nullif(sum([calls]), 0) AS DECIMAL(19,1) )      as [logical_reads_mb__per_call] \n"
			    
				+ "    ,cast('' as varchar(512))                                                             as [shared_blks_hit__chart] \n"
			    + "    ,sum([shared_blks_hit])                                                               as [shared_blks_hit__sum] \n"
			    + "    ,CAST( sum([shared_blks_hit])     * 1.0 / nullif(sum([calls]), 0) AS DECIMAL(19,1) )  as [shared_blks_hit__per_call] \n"

			    + "    ,cast('' as varchar(512))                                                             as [shared_blks_read__chart] \n"
			    + "    ,sum([shared_blks_read])                                                              as [shared_blks_read__sum] \n"
			    + "    ,CAST( sum([shared_blks_read])    * 1.0 / nullif(sum([calls]), 0) AS DECIMAL(19,1) )  as [shared_blks_read__per_call] \n"

			    + "    ,cast('' as varchar(512))                                                             as [shared_blks_dirtied__chart] \n"
			    + "    ,sum([shared_blks_dirtied])                                                           as [shared_blks_dirtied__sum] \n"
			    + "    ,CAST( sum([shared_blks_dirtied]) * 1.0 / nullif(sum([calls]), 0) AS DECIMAL(19,1) )  as [shared_blks_dirtied__per_call] \n"

			    + "    ,cast('' as varchar(512))                                                             as [shared_blks_written__chart] \n"
			    + "    ,sum([shared_blks_written])                                                           as [shared_blks_written__sum] \n"
			    + "    ,CAST( sum([shared_blks_written]) * 1.0 / nullif(sum([calls]), 0) AS DECIMAL(19,1) )  as [shared_blks_written__per_call] \n"
			    + " \n"
			    + "    ,sum([local_blks_hit])                                                                as [local_blks_hit__sum] \n"
			    + "    ,sum([local_blks_read])                                                               as [local_blks_read__sum] \n"
			    + "    ,sum([local_blks_dirtied])                                                            as [local_blks_dirtied__sum] \n"
			    + "    ,sum([local_blks_written])                                                            as [local_blks_written__sum] \n"
			    + " \n"
			    + "    ,cast('' as varchar(512))                                                             as [temp_blks_read__chart] \n"
			    + "    ,sum([temp_blks_read])                                                                as [temp_blks_read__sum] \n"
			    + "    ,CAST( sum([temp_blks_read])      * 1.0 / nullif(sum([calls]), 0) AS DECIMAL(19,1) )  as [temp_blks_read__per_call] \n"
			    
			    + "    ,cast('' as varchar(512))                                                             as [temp_blks_written__chart] \n"
			    + "    ,sum([temp_blks_written])                                                             as [temp_blks_written__sum] \n"
			    + "    ,CAST( sum([temp_blks_written])   * 1.0 / nullif(sum([calls]), 0) AS DECIMAL(19,1) )  as [temp_blks_written__per_call] \n"
			    + " \n"
			    + "    ,cast('' as varchar(512))                                                             as [logical_reads_per_row__chart] \n"
			    + "    ,CAST( sum([shared_blks_hit] + [shared_blks_read]) * 1.0 / nullif(sum([rows]), 0) AS DECIMAL(19,1) )      as [logical_reads__per_row] \n"
			    
			    + "    ,sum([blk_read_time])                                                                 as [blks_read_time__chart] \n"
			    + "    ,sum([blk_read_time])                                                                 as [blks_read_time__sum] \n"
			    + "    ,CAST( sum([blk_read_time])       * 1.0 / nullif(sum([calls]), 0) AS DECIMAL(19,1) )  as [blk_read_time__per_call] \n"
			    
			    + "    ,sum([blk_write_time])                                                                as [blks_write_time__chart] \n"
			    + "    ,sum([blk_write_time])                                                                as [blks_write_time__sum] \n"
			    + "    ,CAST( sum([blk_write_time])      * 1.0 / nullif(sum([calls]), 0) AS DECIMAL(19,1) )  as [blk_write_time__per_call] \n"
			    + " \n"
			    + "    ,count(*)                                                                             as [samples_count] \n"
			    + "    ,min([SessionSampleTime])                                                             as [SessionSampleTime__min] \n"
			    + "    ,max([SessionSampleTime])                                                             as [SessionSampleTime__max] \n"
			    + "    ,cast('' as varchar(30))                                                              as [Duration] \n"
			    + "    ,sum([CmSampleMs])                                                                    as [CmSampleMs__sum] \n"
			    + "from [CmPgStatementsSumDb_diff] x \n"
			    + "where 1 = 1 \n"
				+ getReportPeriodSqlWhere()
			    + "group by [dbname] \n"
			    + "order by [total_time__sum] desc \n"
			    + "";
		
		_shortRstm = executeQuery(conn, sql, true, "Statements Per DB");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("Statements Per DB");
			return;
		}
		else
		{
			// Highlight sort column
			_shortRstm.setHighlightSortColumns("total_time__sum");

			// Describe the table
			setSectionDescription(_shortRstm);

			// set duration
			setDurationColumn(_shortRstm, "SessionSampleTime_min", "SessionSampleTime_max", "Duration");
						
			
			//--------------------------------------------------------------------------------
			// Mini Chart on "..."
			//--------------------------------------------------------------------------------
			String whereKeyColumn = "dbname"; 

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("new_stmnts__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatementsSumDb_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("stmnt_count_diff")
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Number of new statements found in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("calls__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatementsSumDb_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("calls")   
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Number of times executed in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("total_time__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatementsSumDb_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("total_time")
					.setDbmsDataValueColumnName  ("sum(1.0*[total_time]) / nullif(sum([calls]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setDecimalScale(3) // just use whole numbers for this
//					.setDecimalScale(0) // just use whole numbers for this
					.setSparklineTooltipPostfix  ("Total time spent in the statement in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("rows__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatementsSumDb_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("rows")
					.setDbmsDataValueColumnName  ("sum(1.0*[rows]) / nullif(sum([calls]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setDecimalScale(1) // just use whole numbers for this
//					.setDecimalScale(0) // just use whole numbers for this
					.setSparklineTooltipPostfix  ("Total 'rows' in the statement in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("cache_hit_pct__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatementsSumDb_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("100.0 * sum([shared_blks_hit]) / nullif(sum([shared_blks_hit] + [shared_blks_read]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setDecimalScale(2) // just use whole numbers for this
					.setSparklineTooltipPostfix  ("Total 'pages read' from CACHE and DISK for the statement in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("logical_reads__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatementsSumDb_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("sum([shared_blks_hit] + [shared_blks_read])").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsDataValueColumnName  ("sum(1.0*[shared_blks_hit] + [shared_blks_read]) / nullif(sum([calls]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setDecimalScale(0) // just use whole numbers for this
					.setDecimalScale(1)
					.setSparklineTooltipPostfix  ("Total 'pages read' from CACHE and DISK for the statement in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("logical_reads_mb__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatementsSumDb_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("sum([shared_blks_hit] + [shared_blks_read])").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsDataValueColumnName  ("sum(1.0*[shared_blks_hit] + [shared_blks_read]) / 128.0 / nullif(sum([calls]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setDecimalScale(0) // just use whole numbers for this
					.setDecimalScale(1)
					.setSparklineTooltipPostfix  ("Total 'pages read' from CACHE and DISK for the statement in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("shared_blks_hit__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatementsSumDb_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("shared_blks_hit")
					.setDbmsDataValueColumnName  ("sum(1.0*[shared_blks_hit]) / nullif(sum([calls]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setDecimalScale(0) // just use whole numbers for this
					.setDecimalScale(1)
					.setSparklineTooltipPostfix  ("Total 'pages read' from CACHE for the statement in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("shared_blks_read__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatementsSumDb_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("shared_blks_read")
					.setDbmsDataValueColumnName  ("sum(1.0*[shared_blks_read]) / nullif(sum([calls]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setDecimalScale(0) // just use whole numbers for this
					.setDecimalScale(1)
					.setSparklineTooltipPostfix  ("Total 'pages read' from DISK for the statement in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("shared_blks_dirtied__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatementsSumDb_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("shared_blks_dirtied")
					.setDbmsDataValueColumnName  ("sum(1.0*[shared_blks_dirtied]) / nullif(sum([calls]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setDecimalScale(0) // just use whole numbers for this
					.setDecimalScale(1)
					.setSparklineTooltipPostfix  ("Total 'pages' changed by the statement in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("shared_blks_written__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatementsSumDb_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("shared_blks_written")
					.setDbmsDataValueColumnName  ("sum(1.0*[shared_blks_written]) / nullif(sum([calls]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setDecimalScale(0) // just use whole numbers for this
					.setDecimalScale(1)
					.setSparklineTooltipPostfix  ("Total 'pages' synchronously written to disk by the statement in below period")
					.validate()));

			
			
			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("temp_blks_read__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatementsSumDb_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("temp_blks_read")
					.setDbmsDataValueColumnName  ("sum(1.0*[temp_blks_read]) / nullif(sum([calls]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setDecimalScale(0) // just use whole numbers for this
					.setDecimalScale(1)
					.setSparklineTooltipPostfix  ("Total 'temp_blks_read' by the statement in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("temp_blks_written__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatementsSumDb_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("temp_blks_written")
					.setDbmsDataValueColumnName  ("sum(1.0*[temp_blks_written]) / nullif(sum([calls]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setDecimalScale(0) // just use whole numbers for this
					.setDecimalScale(1)
					.setSparklineTooltipPostfix  ("Total 'temp_blks_written' by the statement in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("blks_read_time__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatementsSumDb_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("blk_read_time")
					.setDbmsDataValueColumnName  ("sum(1.0*[blk_read_time]) / nullif(sum([calls]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setDecimalScale(0) // just use whole numbers for this
					.setDecimalScale(1)
					.setSparklineTooltipPostfix  ("Total 'blk_read_time' by the statement in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("blks_write_time__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatementsSumDb_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("blk_write_time")
					.setDbmsDataValueColumnName  ("sum(1.0*[blk_write_time]) / nullif(sum([calls]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setDecimalScale(0) // just use whole numbers for this
					.setDecimalScale(1)
					.setSparklineTooltipPostfix  ("Total 'blk_write_time' by the statement in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("logical_reads_per_row__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatementsSumDb_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("blk_write_time")
					.setDbmsDataValueColumnName  ("sum([shared_blks_hit] + [shared_blks_read]) * 1.0 / nullif(sum([rows]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setDecimalScale(0) // just use whole numbers for this
					.setDecimalScale(1)
//					.setSparklineTooltipPostfix  ("Total 'blk_write_time' by the statement in below period")
					.validate()));

		
			//----------------------------------------------------
			// Create a SQL-Details ResultSet based on values in _shortRstm
			//----------------------------------------------------
			SimpleResultSet srs = new SimpleResultSet();

			srs.addColumn("dbname",     Types.VARCHAR,       60, 0);
			srs.addColumn("sparklines", Types.VARCHAR,      512, 0); 

			// Position in the "source" _shortRstm table (values we will fetch)
			int pos_dbname = _shortRstm.findColumn("dbname");

			ColumnCopyRender msToHMS    = HtmlTableProducer.MS_TO_HMS;
			ColumnCopyRender oneDecimal = HtmlTableProducer.ONE_DECIMAL;
			
			HtmlTableProducer htp = new HtmlTableProducer(_shortRstm, "dsr-sub-table-chart");
			htp.setTableHeaders("Charts at 10 minute interval", "Total;style='text-align:right!important'", "Avg per call;style='text-align:right!important'", "");
			htp.add("new-stmnts"   , new ColumnCopyRow().add( new ColumnCopyDef("new_stmnts__chart"            ) ).add(new ColumnCopyDef("new_stmnts__sum")         ).addEmptyCol()                                                                   .addEmptyCol() );
			htp.add("call-cnt"     , new ColumnCopyRow().add( new ColumnCopyDef("calls__chart"                 ) ).add(new ColumnCopyDef("calls__sum").setColBold() ).addEmptyCol()                                                                   .addEmptyCol() );
			htp.add("exec-time"    , new ColumnCopyRow().add( new ColumnCopyDef("total_time__chart"            ) ).add(new ColumnCopyDef("total_time__sum", msToHMS)).add(new ColumnCopyDef("total_time__per_call"         , oneDecimal).setColBold()).add(new ColumnStatic("ms")) );
			htp.add("rows"         , new ColumnCopyRow().add( new ColumnCopyDef("rows__chart"                  ) ).add(new ColumnCopyDef("rows__sum"               )).add(new ColumnCopyDef("rows__per_call"               , oneDecimal).setColBold()).add(new ColumnStatic("rows")) );
			htp.add("cache-hit"    , new ColumnCopyRow().add( new ColumnCopyDef("cache_hit_pct__chart"         ) ).add(new EmptyColumn(                            )).add(new ColumnCopyDef("cache_hit_pct__avg"           , oneDecimal).setColBold()).add(new ColumnStatic("%")) );
			htp.add("total-read"   , new ColumnCopyRow().add( new ColumnCopyDef("logical_reads__chart"         ) ).add(new ColumnCopyDef("logical_reads__sum"      )).add(new ColumnCopyDef("logical_reads__per_call"      , oneDecimal).setColBold()).add(new ColumnStatic("pgs")) );
			htp.add("total-read-mb", new ColumnCopyRow().add( new ColumnCopyDef("logical_reads_mb__chart"      ) ).add(new ColumnCopyDef("logical_reads_mb__sum"   )).add(new ColumnCopyDef("logical_reads_mb__per_call"   , oneDecimal).setColBold()).add(new ColumnStatic("mb")) );
			htp.add("tot-read/row" , new ColumnCopyRow().add( new ColumnCopyDef("logical_reads_per_row__chart" ) ).add(new ColumnStatic ("n/a").setColAlign("right")).add(new ColumnCopyDef("logical_reads__per_row"       , oneDecimal).setColBold()).add(new ColumnStatic("pgs")) );
//			htp.add("cache-read"   , new ColumnCopyRow().add( new ColumnCopyDef("shared_blks_hit__chart"       ) ).add(new ColumnCopyDef("shared_blks_hit__sum"    )).add(new ColumnCopyDef("shared_blks_hit__per_call"    , oneDec).setColBold()).add(new ColumnStatic("pgs")) );
			htp.add("phys-read"    , new ColumnCopyRow().add( new ColumnCopyDef("shared_blks_read__chart"      ) ).add(new ColumnCopyDef("shared_blks_read__sum"   )).add(new ColumnCopyDef("shared_blks_read__per_call"   , oneDecimal).setColBold()).add(new ColumnStatic("pgs")) );
			htp.add("dirtied"      , new ColumnCopyRow().add( new ColumnCopyDef("shared_blks_dirtied__chart"   ) ).add(new ColumnCopyDef("shared_blks_dirtied__sum")).add(new ColumnCopyDef("shared_blks_dirtied__per_call", oneDecimal).setColBold()).add(new ColumnStatic("pgs")) );
//			htp.add("written"      , new ColumnCopyRow().add( new ColumnCopyDef("shared_blks_written__chart"   ) ).add(new ColumnCopyDef("shared_blks_written__sum")).add(new ColumnCopyDef("shared_blks_written__per_call", oneDec).setColBold()).add(new ColumnStatic("pgs")) );
			htp.add("tmp-read"     , new ColumnCopyRow().add( new ColumnCopyDef("temp_blks_read__chart"        ) ).add(new ColumnCopyDef("temp_blks_read__sum"     )).add(new ColumnCopyDef("temp_blks_read__per_call"     , oneDecimal).setColBold()).add(new ColumnStatic("pgs")) );
			htp.add("tmp-write"    , new ColumnCopyRow().add( new ColumnCopyDef("temp_blks_written__chart"     ) ).add(new ColumnCopyDef("temp_blks_written__sum"  )).add(new ColumnCopyDef("temp_blks_written__per_call"  , oneDecimal).setColBold()).add(new ColumnStatic("pgs")) );
			htp.validate();

			// loop "data table" and create "sparkline table" 
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

			// Create the sparkline table
			try
			{
				// Note the 'srs' is populated when reading above ResultSet from query
				_miniChartRstm = createResultSetTableModel(srs, "Sparkline table", null, false); // DO NOT TRUNCATE COLUMNS
				srs.close();
			}
			catch (SQLException ex)
			{
				setProblemException(ex);
	
				_miniChartRstm = ResultSetTableModel.createEmpty("Statements per DB");
//				_logger.warn("Problems getting Top SQL TEXT: " + ex);
			}

			
			// Create some Overview Charts
			_CmPgStatementsSumDb_CallCnt   = createTsLineChart(conn, "CmPgStatementsSumDb", "CallCnt"  , -1, null, "SQL Statements [calls] per DB per second (Statements by DB)");
			_CmPgStatementsSumDb_TotalTime = createTsLineChart(conn, "CmPgStatementsSumDb", "TotalTime", -1, null, "SQL Statements [total_time] per DB per second (Statements by DB)");

		} // end: has data
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
				"How many statements has been executed per database are presented here (ordered by: total_time__sum) <br>" +
				"<br>" +
				"Postgres Source table is 'pg_stat_statements'. <br>" +
				"PCS Source table is 'CmPgStatementsSumDb_diff'. (PCS = Persistent Counter Store) <br>" +
				"The report <i>summarizes</i> (min/max/count/sum/avg) all entries/samples from the <i>CmPgStatementsSumDb_diff</i> table grouped by 'dbname'. <br>" +
				"Typically the column name <i>postfix</i> will tell you what aggregate function was used. <br>" +
				"Sparkline Graphs grouped by DB is also be displayed in a separate table below the <i>summary</i> table.<br>" +
				"");

		// Columns description
		rstm.setColumnDescription("dbname"                      , "Database name Statements was executed in.");
		rstm.setColumnDescription("samples_count"               , "Number of entries for this 'dbname' in the report period");
		rstm.setColumnDescription("SessionSampleTime__min"      , "First entry was sampled for this entry");
		rstm.setColumnDescription("SessionSampleTime__max"      , "Last entry was sampled for this entry");
		rstm.setColumnDescription("Duration"                    , "Start/end time presented as HH:MM:SS, so we can see if this entry is just for a short time or if it spans over a long period of time.");
		rstm.setColumnDescription("CmSampleMs__sum"             , "Number of milliseconds this object has been available for sampling");

		rstm.setColumnDescription("new_stmnts__sum"            , "Number of Statements created");
		rstm.setColumnDescription("calls__sum"                  , "Number of times executed");
		rstm.setColumnDescription("avg_time_per_call__avg"      , "Average Execution Time per call              (Algorithm: total_time / calls)");
		rstm.setColumnDescription("total_time__sum"             , "Total time spent in the statement, in milliseconds");
		rstm.setColumnDescription("avg_rows_per_call__avg"      , "Average 'number of rows retrived' per call   (Algorithm: rows / calls)");
		rstm.setColumnDescription("rows_sum"                    , "Total number of rows retrieved or affected by the statement");

		rstm.setColumnDescription("cache_hit_pct__avg"          , "Average blockes found in cache by the statement (Algorithm: 100.0 * shared_blks_hit / (shared_blks_hit + shared_blks_read)");
//		rstm.setColumnDescription("shared_blks_hit_per_row__avg", "Average 'number of cache reads' per call     (Algorithm: shared_blks_hit / calls)");
		rstm.setColumnDescription("shared_blks_hit__sum"        , "Total number of shared block cache hits by the statement");
		rstm.setColumnDescription("shared_blks_read__sum"       , "Total number of shared blocks read by the statement");
		rstm.setColumnDescription("shared_blks_dirtied__sum"    , "Total number of shared blocks dirtied by the statement");
		rstm.setColumnDescription("shared_blks_written__sum"    , "Total number of shared blocks written by the statement");
		
		rstm.setColumnDescription("local_blks_hit__sum"         , "Total number of local block cache hits by the statement");
		rstm.setColumnDescription("local_blks_read__sum"        , "Total number of local blocks read by the statement");
		rstm.setColumnDescription("local_blks_dirtied__sum"     , "Total number of local blocks dirtied by the statement");
		rstm.setColumnDescription("local_blks_written__sum"     , "Total number of local blocks written by the statement");

		rstm.setColumnDescription("temp_blks_read__sum"         , "Total number of temp blocks read by the statement");
		rstm.setColumnDescription("temp_blks_written__sum"      , "Total number of temp blocks written by the statement");

		rstm.setColumnDescription("blks_read_time__sum"         , "Total time the statement spent reading blocks, in milliseconds (if track_io_timing is enabled, otherwise zero)");
		rstm.setColumnDescription("blks_write_time__sum"        , "Total time the statement spent writing blocks, in milliseconds (if track_io_timing is enabled, otherwise zero)");
	}
}
