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
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.h2.tools.SimpleResultSet;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.IReportChart;
import com.asetune.pcs.report.content.SparklineHelper;
import com.asetune.pcs.report.content.SparklineHelper.AggType;
import com.asetune.pcs.report.content.SparklineHelper.DataSource;
import com.asetune.pcs.report.content.SparklineHelper.SparkLineParams;
import com.asetune.pcs.report.content.postgres.PostgresAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.HtmlTableProducer;
import com.asetune.utils.HtmlTableProducer.ColumnCopyDef;
import com.asetune.utils.HtmlTableProducer.ColumnCopyRender;
import com.asetune.utils.HtmlTableProducer.ColumnCopyRow;
import com.asetune.utils.HtmlTableProducer.ColumnStatic;

public class AseCmSqlStatementPerDb 
extends PostgresAbstract
{
	private static Logger _logger = Logger.getLogger(AseCmSqlStatementPerDb.class);

	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _miniChartRstm;
	private List<String>        _miniChartJsList = new ArrayList<>();

	private IReportChart _CmSqlStatementPerDb_SsDbTotExecCnt;
	private IReportChart _CmSqlStatementPerDb_SsDbSumExecTime;
	private IReportChart _CmSqlStatementPerDb_SsDbSumLRead;

	public AseCmSqlStatementPerDb(DailySummaryReportAbstract reportingInstance)
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
		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are 'Total Statements Executed/sec', 'Sum Execution Time in ms' and 'Sum Logical Reads' charts for each Database during the day.",
				"CmSqlStatementPerDb_SsDbTotExecCnt",
				"CmSqlStatementPerDb_SsDbSumExecTime",
				"CmSqlStatementPerDb_SsDbSumLRead"
				));

		_CmSqlStatementPerDb_SsDbTotExecCnt .writeHtmlContent(sb, null, null);
		_CmSqlStatementPerDb_SsDbSumExecTime.writeHtmlContent(sb, null, null);
		_CmSqlStatementPerDb_SsDbSumLRead   .writeHtmlContent(sb, null, null);

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
		return "SQL Statement Execution Statistics per Database (origin: CmSqlStatementPerDb / monSysStatement)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmSqlStatementPerDb_diff" };
	}


	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		//-------------------------------------------------------
		// Get ASE Page Size
		//-------------------------------------------------------
		String asePageSizeDivDec = "512.0"; // 2K PageSize
		String asePageSizeDivInt = "512";   // 2K PageSize
		String sql = "select top 1 [asePageSize] from [CmSummary_abs]";
		
		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement() )
		{
			int asePageSize = -1;

			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				while(rs.next())
					asePageSize = rs.getInt(1);
			}
			
			asePageSizeDivDec = "" + 1024.0 * 1024.0 / asePageSize;
			asePageSizeDivInt = "" + 1024   * 1024   / asePageSize;
		}
		catch(Exception ex)
		{
			_logger.warn("Problems getting ASE Page Size from DDL Storage. Using 2K Pages as the default. asePageSizeDivDec=" + asePageSizeDivDec + ", asePageSizeDivInt="+asePageSizeDivInt, ex);
		}

		//-------------------------------------------------------
		// Compose SQL Statement
		//-------------------------------------------------------
		sql = ""
			    + "select \n"
			    + "     [dbid] \n"
			    + "    ,[dbname] \n"
			    + " \n"
			    + "    ,cast('' as varchar(512))    AS [totalCount__chart] \n"
			    + "    ,sum([totalCount])           AS [totalCount__sum] \n"
			    + " \n"
			    + "    ,cast('' as varchar(512))    AS [sqlBatchCount__chart] \n"
			    + "    ,sum([sqlBatchCount])        AS [sqlBatchCount__sum] \n"
			    + " \n"
			    + "    ,cast('' as varchar(512))    AS [errorCount__chart] \n"
			    + "    ,sum([errorCount])           AS [errorCount__sum] \n"
			    + " \n"
			    + "    ,cast('' as varchar(512))    AS [inStmntCacheCount__chart] \n"
			    + "    ,sum([inStmntCacheCount])    AS [inStmntCacheCount__sum] \n"
			    + " \n"
			    + "    ,cast('' as varchar(512))    AS [dynamicStmntCount__chart] \n"
			    + "    ,sum([dynamicStmntCount])    AS [dynamicStmntCount__sum] \n"
			    + " \n"
			    + "    ,cast('' as varchar(512))    AS [inProcedureCount__chart] \n"
			    + "    ,sum([inProcedureCount])     AS [inProcedureCount__sum] \n"
			    + " \n"
			    + "    ,cast('' as varchar(512))    AS [inProcNameNullCount__chart] \n"
			    + "    ,sum([inProcNameNullCount])  AS [inProcNameNullCount__sum] \n"
			    + " \n"
			    + "    ,cast('' as varchar(512))                                                                             AS [ExecTimeMs__chart] \n"
			    + "    ,sum([sumExecTimeMs])                                                                                 AS [ExecTimeMs__sum] \n"
//			    + "    ,CASE WHEN sum([totalCount]) = 0 THEN 0 ELSE cast(sum([sumExecTimeMs])    / sum([totalCount]) as int) END  AS [avgExecTimeMs] \n"
			    + "    ,CAST( sum([sumExecTimeMs]) / nullif(sum([totalCount]), 0) AS DECIMAL(19,1) )                         AS [ExecTimeMs__per_call] \n"
			    + " \n"
			    + "    ,cast('' as varchar(512))                                                                             AS [CpuTime__chart] \n"
			    + "    ,sum([sumCpuTime])                                                                                    AS [CpuTime__sum] \n"
//			    + "    ,CASE WHEN sum([totalCount]) = 0 THEN 0 ELSE cast(sum([sumCpuTime])       / sum([totalCount]) as int) END  AS [avgCpuTime] \n"
			    + "    ,CAST( sum([sumCpuTime]) / nullif(sum([totalCount]), 0) AS DECIMAL(19,1) )                            AS [CpuTime__per_call] \n"
			    + " \n"
			    + "    ,cast('' as varchar(512))                                                                             AS [WaitTime__chart] \n"
			    + "    ,sum([sumWaitTime])                                                                                   AS [WaitTime__sum] \n"
//			    + "    ,CASE WHEN sum([totalCount]) = 0 THEN 0 ELSE cast(sum([sumWaitTime])      / sum([totalCount]) as int) END  AS [avgWaitTime] \n"
			    + "    ,CAST( sum([sumWaitTime]) / nullif(sum([totalCount]), 0) AS DECIMAL(19,1) )                           AS [WaitTime__per_call] \n"
			    + " \n"
			    + "    ,cast('' as varchar(512))                                                                             AS [LogicalReads__chart] \n"
			    + "    ,sum([sumLogicalReads])                                                                               AS [LogicalReads__sum] \n"
//			    + "    ,CASE WHEN sum([totalCount]) = 0 THEN 0 ELSE cast(sum([sumLogicalReads])  / sum([totalCount]) as int) END  AS [avgLogicalReads] \n"
			    + "    ,CAST( sum([sumLogicalReads]) / nullif(sum([totalCount]), 0) AS DECIMAL(19,1) )                       AS [LogicalReads__per_call] \n"
			    + " \n"
				+ "    ,cast('' as varchar(512))                                                                             AS [LogicalReadsMb__chart] \n"
				+ "    ,cast(sum([sumLogicalReads])/" + asePageSizeDivInt + " as bigint)                                     AS [LogicalReadsMb__sum] \n"
				+ "    ,CAST( sum([sumLogicalReads]) / " + asePageSizeDivDec + " / nullif(sum([totalCount]), 0) AS DECIMAL(19,1) ) AS [LogicalReadsMb__per_call] \n"
			    + " \n"
			    + "    ,cast('' as varchar(512))                                                                             AS [PhysicalReads__chart] \n"
			    + "    ,sum([sumPhysicalReads])                                                                              AS [PhysicalReads__sum] \n"
//			    + "    ,CASE WHEN sum([totalCount]) = 0 THEN 0 ELSE cast(sum([sumPhysicalReads]) / sum([totalCount]) as int) END  AS [avgPhysicalReads] \n"
			    + "    ,CAST( sum([sumPhysicalReads]) / nullif(sum([totalCount]), 0) AS DECIMAL(19,1) )                      AS [PhysicalReads__per_call] \n"
			    + " \n"
				+ "    ,cast('' as varchar(512))                                                                             AS [PhysicalReadsMb__chart] \n"
				+ "    ,cast(sum([sumLogicalReads])/" + asePageSizeDivInt + " as bigint)                                     AS [PhysicalReadsMb__sum] \n"
				+ "    ,CAST( sum([sumLogicalReads]) / " + asePageSizeDivDec + " / nullif(sum([totalCount]), 0) AS DECIMAL(19,1) ) AS [PhysicalReadsMb__per_call] \n"
			    + " \n"
			    + "    ,cast('' as varchar(512))                                                                             AS [RowsAffected__chart] \n"
			    + "    ,sum([sumRowsAffected])                                                                               AS [RowsAffected__sum] \n"
//			    + "    ,CASE WHEN sum([totalCount]) = 0 THEN 0 ELSE cast(sum([sumRowsAffected])  / sum([totalCount]) as int) END  AS [avgRowsAffected] \n"
			    + "    ,CAST( sum([sumRowsAffected]) / nullif(sum([totalCount]), 0) AS DECIMAL(19,1) )                       AS [RowsAffected__per_call] \n"
			    + " \n"
			    + "    ,cast('' as varchar(512))                                                                             AS [LogicalReadsPerRowsAffected__chart] \n"
//			    + "    ,CASE WHEN sum([sumRowsAffected]) = 0 THEN 0 ELSE cast(sum([sumLogicalReads]) / sum([sumRowsAffected]) as int) END  AS [LogicalReadsPerRowsAffected] \n"
			    + "    ,CAST( sum([sumLogicalReads]) / nullif(sum([sumRowsAffected]), 0) AS DECIMAL(19,1) )                  AS [LogicalReadsPerRowsAffected] \n"
			    + " \n"
			    + "from [CmSqlStatementPerDb_diff] \n"
			    + "where 1 = 1 \n"
				+ getReportPeriodSqlWhere()
			    + "group by [dbid] \n"
//			    + "order by [dbid] \n"
//			    + "order by sum([sumCpuTime]) desc \n"
			    + "order by [CpuTime__sum] desc \n"
			    + " \n"
			    + "";

//System.out.println("----------------------------- SQL: \n" + sql);
		
		_shortRstm = executeQuery(conn, sql, true, "CmSqlStatementPerDb_diff");

		// Highlight sort column
		_shortRstm.setHighlightSortColumns("CpuTime__sum");

		// Describe the table
		setSectionDescription(_shortRstm);
		
//		System.out.println("-----------------" + _miniChartRstm.getName() + "----------------\n"+_miniChartRstm.toAsciiTableString());

//		String tabWhereKeyColumn  = "id"; 
//		String dbmsWhereKeyColumn = "statId"; 
		String tabWhereKeyColumn  = "dbname"; 
		String dbmsWhereKeyColumn = "dbname"; 

		_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
				SparkLineParams.create       (DataSource.CounterModel)
				.setHtmlChartColumnName      ("totalCount__chart")
				.setHtmlWhereKeyColumnName   (tabWhereKeyColumn)
				.setDbmsTableName            ("CmSqlStatementPerDb_diff")
				.setDbmsSampleTimeColumnName ("SessionSampleTime")
				.setDbmsDataValueColumnName  ("totalCount")
				.setDbmsWhereKeyColumnName   (dbmsWhereKeyColumn)
				.validate()));
		
		_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
				SparkLineParams.create       (DataSource.CounterModel)
				.setHtmlChartColumnName      ("sqlBatchCount__chart")
				.setHtmlWhereKeyColumnName   (tabWhereKeyColumn)
				.setDbmsTableName            ("CmSqlStatementPerDb_diff")
				.setDbmsSampleTimeColumnName ("SessionSampleTime")
				.setDbmsDataValueColumnName  ("sqlBatchCount")
				.setDbmsWhereKeyColumnName   (dbmsWhereKeyColumn)
				.validate()));
		
		_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
				SparkLineParams.create       (DataSource.CounterModel)
				.setHtmlChartColumnName      ("errorCount__chart")
				.setHtmlWhereKeyColumnName   (tabWhereKeyColumn)
				.setDbmsTableName            ("CmSqlStatementPerDb_diff")
				.setDbmsSampleTimeColumnName ("SessionSampleTime")
				.setDbmsDataValueColumnName  ("errorCount")
				.setDbmsWhereKeyColumnName   (dbmsWhereKeyColumn)
				.validate()));
		
		_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
				SparkLineParams.create       (DataSource.CounterModel)
				.setHtmlChartColumnName      ("inStmntCacheCount__chart")
				.setHtmlWhereKeyColumnName   (tabWhereKeyColumn)
				.setDbmsTableName            ("CmSqlStatementPerDb_diff")
				.setDbmsSampleTimeColumnName ("SessionSampleTime")
				.setDbmsDataValueColumnName  ("inStmntCacheCount")
				.setDbmsWhereKeyColumnName   (dbmsWhereKeyColumn)
				.validate()));
		
		_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
				SparkLineParams.create       (DataSource.CounterModel)
				.setHtmlChartColumnName      ("dynamicStmntCount__chart")
				.setHtmlWhereKeyColumnName   (tabWhereKeyColumn)
				.setDbmsTableName            ("CmSqlStatementPerDb_diff")
				.setDbmsSampleTimeColumnName ("SessionSampleTime")
				.setDbmsDataValueColumnName  ("dynamicStmntCount")
				.setDbmsWhereKeyColumnName   (dbmsWhereKeyColumn)
				.validate()));
		
		_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
				SparkLineParams.create       (DataSource.CounterModel)
				.setHtmlChartColumnName      ("inProcedureCount__chart")
				.setHtmlWhereKeyColumnName   (tabWhereKeyColumn)
				.setDbmsTableName            ("CmSqlStatementPerDb_diff")
				.setDbmsSampleTimeColumnName ("SessionSampleTime")
				.setDbmsDataValueColumnName  ("inProcedureCount")
				.setDbmsWhereKeyColumnName   (dbmsWhereKeyColumn)
				.validate()));
		
		_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
				SparkLineParams.create       (DataSource.CounterModel)
				.setHtmlChartColumnName      ("inProcNameNullCount__chart")
				.setHtmlWhereKeyColumnName   (tabWhereKeyColumn)
				.setDbmsTableName            ("CmSqlStatementPerDb_diff")
				.setDbmsSampleTimeColumnName ("SessionSampleTime")
				.setDbmsDataValueColumnName  ("inProcNameNullCount")
				.setDbmsWhereKeyColumnName   (dbmsWhereKeyColumn)
				.validate()));
		
		
		
		_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
				SparkLineParams.create       (DataSource.CounterModel)
				.setHtmlChartColumnName      ("ExecTimeMs__chart")
				.setHtmlWhereKeyColumnName   (tabWhereKeyColumn)
				.setDbmsTableName            ("CmSqlStatementPerDb_diff")
				.setDbmsSampleTimeColumnName ("SessionSampleTime")
				.setDbmsDataValueColumnName  ("sumExecTimeMs")
				.setDbmsWhereKeyColumnName   (dbmsWhereKeyColumn)
				.validate()));
		
		_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
				SparkLineParams.create       (DataSource.CounterModel)
				.setHtmlChartColumnName      ("CpuTime__chart")
				.setHtmlWhereKeyColumnName   (tabWhereKeyColumn)
				.setDbmsTableName            ("CmSqlStatementPerDb_diff")
				.setDbmsSampleTimeColumnName ("SessionSampleTime")
				.setDbmsDataValueColumnName  ("sumCpuTime")
				.setDbmsWhereKeyColumnName   (dbmsWhereKeyColumn)
				.validate()));
		
		_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
				SparkLineParams.create       (DataSource.CounterModel)
				.setHtmlChartColumnName      ("WaitTime__chart")
				.setHtmlWhereKeyColumnName   (tabWhereKeyColumn)
				.setDbmsTableName            ("CmSqlStatementPerDb_diff")
				.setDbmsSampleTimeColumnName ("SessionSampleTime")
				.setDbmsDataValueColumnName  ("sumWaitTime")
				.setDbmsWhereKeyColumnName   (dbmsWhereKeyColumn)
				.validate()));
		
		_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
				SparkLineParams.create       (DataSource.CounterModel)
				.setHtmlChartColumnName      ("LogicalReads__chart")
				.setHtmlWhereKeyColumnName   (tabWhereKeyColumn)
				.setDbmsTableName            ("CmSqlStatementPerDb_diff")
				.setDbmsSampleTimeColumnName ("SessionSampleTime")
				.setDbmsDataValueColumnName  ("sumLogicalReads")
				.setDbmsWhereKeyColumnName   (dbmsWhereKeyColumn)
				.validate()));
		
		_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
				SparkLineParams.create       (DataSource.CounterModel)
				.setHtmlChartColumnName      ("PhysicalReads__chart")
				.setHtmlWhereKeyColumnName   (tabWhereKeyColumn)
				.setDbmsTableName            ("CmSqlStatementPerDb_diff")
				.setDbmsSampleTimeColumnName ("SessionSampleTime")
				.setDbmsDataValueColumnName  ("sumPhysicalReads")
				.setDbmsWhereKeyColumnName   (dbmsWhereKeyColumn)
				.validate()));
		
		_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
				SparkLineParams.create       (DataSource.CounterModel)
				.setHtmlChartColumnName      ("LogicalReadsMb__chart")
				.setHtmlWhereKeyColumnName   (tabWhereKeyColumn)
				.setDbmsTableName            ("CmSqlStatementPerDb_diff")
				.setDbmsSampleTimeColumnName ("SessionSampleTime")
//				.setDbmsDataValueColumnName  ("sumLogicalReads")
//				.setDbmsDataValueColumnName  ("sum(1.0*[sumLogicalReads]) / 128.0 / nullif(sum([totalCount]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
				.setDbmsDataValueColumnName  ("sum(1.0*[sumLogicalReads]) / " + asePageSizeDivDec).setGroupDataAggregationType(AggType.USER_PROVIDED)
				.setDbmsWhereKeyColumnName   (dbmsWhereKeyColumn)
				.validate()));
		
		_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
				SparkLineParams.create       (DataSource.CounterModel)
				.setHtmlChartColumnName      ("PhysicalReadsMb__chart")
				.setHtmlWhereKeyColumnName   (tabWhereKeyColumn)
				.setDbmsTableName            ("CmSqlStatementPerDb_diff")
				.setDbmsSampleTimeColumnName ("SessionSampleTime")
//				.setDbmsDataValueColumnName  ("sumPhysicalReads")
//				.setDbmsDataValueColumnName  ("sum(1.0*[sumPhysicalReads]) / 128.0 / nullif(sum([totalCount]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
				.setDbmsDataValueColumnName  ("sum(1.0*[sumPhysicalReads]) / " + asePageSizeDivDec).setGroupDataAggregationType(AggType.USER_PROVIDED)
				.setDbmsWhereKeyColumnName   (dbmsWhereKeyColumn)
				.validate()));
		
		_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
				SparkLineParams.create       (DataSource.CounterModel)
				.setHtmlChartColumnName      ("RowsAffected__chart")
				.setHtmlWhereKeyColumnName   (tabWhereKeyColumn)
				.setDbmsTableName            ("CmSqlStatementPerDb_diff")
				.setDbmsSampleTimeColumnName ("SessionSampleTime")
				.setDbmsDataValueColumnName  ("sumRowsAffected")
				.setDbmsWhereKeyColumnName   (dbmsWhereKeyColumn)
				.validate()));
		
		_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
				SparkLineParams.create       (DataSource.CounterModel)
				.setHtmlChartColumnName      ("LogicalReadsPerRowsAffected__chart")
				.setHtmlWhereKeyColumnName   (tabWhereKeyColumn)
				.setDbmsTableName            ("CmSqlStatementPerDb_diff")
				.setDbmsSampleTimeColumnName ("SessionSampleTime")
				.setDbmsDataValueColumnName  ("sum([sumLogicalReads]) * 1.0 / nullif(sum([sumRowsAffected]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
				.setDbmsWhereKeyColumnName   (dbmsWhereKeyColumn)
				.setDecimalScale(1)
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
		htp.add("total-cnt"     , new ColumnCopyRow().add( new ColumnCopyDef("totalCount__chart"         ) ).add(new ColumnCopyDef("totalCount__sum"         ).setColBold() ).addEmptyCol()                                                               .add(new ColumnStatic("#")) );
		htp.add("batch-cnt"     , new ColumnCopyRow().add( new ColumnCopyDef("sqlBatchCount__chart"      ) ).add(new ColumnCopyDef("sqlBatchCount__sum"      )              ).addEmptyCol()                                                               .add(new ColumnStatic("#")) );
		htp.add("error-cnt"     , new ColumnCopyRow().add( new ColumnCopyDef("errorCount__chart"         ) ).add(new ColumnCopyDef("errorCount__sum"         )              ).addEmptyCol()                                                               .add(new ColumnStatic("#")) );
		htp.add("stmntCache-cnt", new ColumnCopyRow().add( new ColumnCopyDef("inStmntCacheCount__chart"  ) ).add(new ColumnCopyDef("inStmntCacheCount__sum"  )              ).addEmptyCol()                                                               .add(new ColumnStatic("#")) );
		htp.add("dynStmnt-cnt"  , new ColumnCopyRow().add( new ColumnCopyDef("dynamicStmntCount__chart"  ) ).add(new ColumnCopyDef("dynamicStmntCount__sum"  )              ).addEmptyCol()                                                               .add(new ColumnStatic("#")) );
		htp.add("inProc-cnt"    , new ColumnCopyRow().add( new ColumnCopyDef("inProcedureCount__chart"   ) ).add(new ColumnCopyDef("inProcedureCount__sum"   )              ).addEmptyCol()                                                               .add(new ColumnStatic("#")) );
		htp.add("inProcNull-cnt", new ColumnCopyRow().add( new ColumnCopyDef("inProcNameNullCount__chart") ).add(new ColumnCopyDef("inProcNameNullCount__sum")              ).addEmptyCol()                                                               .add(new ColumnStatic("#")) );

		htp.add("exec-time"     , new ColumnCopyRow().add( new ColumnCopyDef("ExecTimeMs__chart"         ) ).add(new ColumnCopyDef("ExecTimeMs__sum", msToHMS).setColBold() ).add(new ColumnCopyDef("ExecTimeMs__per_call"       , oneDecimal).setColBold()).add(new ColumnStatic("ms")) );
		htp.add("cpu-time"      , new ColumnCopyRow().add( new ColumnCopyDef("CpuTime__chart"            ) ).add(new ColumnCopyDef("CpuTime__sum"   , msToHMS              )).add(new ColumnCopyDef("CpuTime__per_call"          , oneDecimal).setColBold()).add(new ColumnStatic("ms")) );
		htp.add("wait-time"     , new ColumnCopyRow().add( new ColumnCopyDef("WaitTime__chart"           ) ).add(new ColumnCopyDef("WaitTime__sum"  , msToHMS              )).add(new ColumnCopyDef("WaitTime__per_call"         , oneDecimal).setColBold()).add(new ColumnStatic("ms")) );
		htp.add("l-reads"       , new ColumnCopyRow().add( new ColumnCopyDef("LogicalReads__chart"       ) ).add(new ColumnCopyDef("LogicalReads__sum"                     )).add(new ColumnCopyDef("LogicalReads__per_call"     , oneDecimal).setColBold()).add(new ColumnStatic("pgs")) );
		htp.add("l-reads-mb"    , new ColumnCopyRow().add( new ColumnCopyDef("LogicalReadsMb__chart"     ) ).add(new ColumnCopyDef("LogicalReadsMb__sum"                   )).add(new ColumnCopyDef("LogicalReadsMb__per_call"   , oneDecimal).setColBold()).add(new ColumnStatic("mb")) );
		htp.add("p-reads"       , new ColumnCopyRow().add( new ColumnCopyDef("PhysicalReads__chart"      ) ).add(new ColumnCopyDef("PhysicalReads__sum"                    )).add(new ColumnCopyDef("PhysicalReads__per_call"    , oneDecimal).setColBold()).add(new ColumnStatic("pgs")) );
		htp.add("p-reads-mb"    , new ColumnCopyRow().add( new ColumnCopyDef("PhysicalReadsMb__chart"    ) ).add(new ColumnCopyDef("PhysicalReadsMb__sum"                  )).add(new ColumnCopyDef("PhysicalReadsMb__per_call"  , oneDecimal).setColBold()).add(new ColumnStatic("mb")) );
		htp.add("rows-affected" , new ColumnCopyRow().add( new ColumnCopyDef("RowsAffected__chart"       ) ).add(new ColumnCopyDef("RowsAffected__sum"                     )).add(new ColumnCopyDef("RowsAffected__per_call"     , oneDecimal).setColBold()).add(new ColumnStatic("rows")) );
		htp.add("l-read/row"    , new ColumnCopyRow().add( new ColumnCopyDef("LogicalReadsPerRowsAffected__chart" ) ).add(new ColumnStatic ("n/a").setColAlign("right")     ).add(new ColumnCopyDef("LogicalReadsPerRowsAffected", oneDecimal).setColBold()).add(new ColumnStatic("pgs")) );
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
//			_logger.warn("Problems getting Top SQL TEXT: " + ex);
		}

//		// Create a copy of the table and add columns for "spark-lines" or "mini-charts"
//		_miniChartRstm = new ResultSetTableModel(_shortRstm, "CmSqlStatementPerDb_diff_sparkline", true);
//
//		_miniChartRstm.addColumn("totalCount"          + "__chart", _miniChartRstm.findColumn("totalCount"         ), Types.VARCHAR, "varchar", "varchar(512)", 512, -1, "", String.class);
//		_miniChartRstm.addColumn("sqlBatchCount"       + "__chart", _miniChartRstm.findColumn("sqlBatchCount"      ), Types.VARCHAR, "varchar", "varchar(512)", 512, -1, "", String.class);
//		_miniChartRstm.addColumn("errorCount"          + "__chart", _miniChartRstm.findColumn("errorCount"         ), Types.VARCHAR, "varchar", "varchar(512)", 512, -1, "", String.class);
//		_miniChartRstm.addColumn("inStmntCacheCount"   + "__chart", _miniChartRstm.findColumn("inStmntCacheCount"  ), Types.VARCHAR, "varchar", "varchar(512)", 512, -1, "", String.class);
//		_miniChartRstm.addColumn("dynamicStmntCount"   + "__chart", _miniChartRstm.findColumn("dynamicStmntCount"  ), Types.VARCHAR, "varchar", "varchar(512)", 512, -1, "", String.class);
//		_miniChartRstm.addColumn("inProcedureCount"    + "__chart", _miniChartRstm.findColumn("inProcedureCount"   ), Types.VARCHAR, "varchar", "varchar(512)", 512, -1, "", String.class);
//		_miniChartRstm.addColumn("inProcNameNullCount" + "__chart", _miniChartRstm.findColumn("inProcNameNullCount"), Types.VARCHAR, "varchar", "varchar(512)", 512, -1, "", String.class);
//
//		_miniChartRstm.addColumn("sumExecTimeMs"       + "__chart", _miniChartRstm.findColumn("sumExecTimeMs"      ), Types.VARCHAR, "varchar", "varchar(512)", 512, -1, "", String.class);
//		_miniChartRstm.addColumn("sumCpuTime"          + "__chart", _miniChartRstm.findColumn("sumCpuTime"         ), Types.VARCHAR, "varchar", "varchar(512)", 512, -1, "", String.class);
//		_miniChartRstm.addColumn("sumWaitTime"         + "__chart", _miniChartRstm.findColumn("sumWaitTime"        ), Types.VARCHAR, "varchar", "varchar(512)", 512, -1, "", String.class);
//		_miniChartRstm.addColumn("sumLogicalReads"     + "__chart", _miniChartRstm.findColumn("sumLogicalReads"    ), Types.VARCHAR, "varchar", "varchar(512)", 512, -1, "", String.class);
//		_miniChartRstm.addColumn("sumPhysicalReads"    + "__chart", _miniChartRstm.findColumn("sumPhysicalReads"   ), Types.VARCHAR, "varchar", "varchar(512)", 512, -1, "", String.class);
//		_miniChartRstm.addColumn("sumRowsAffected"     + "__chart", _miniChartRstm.findColumn("sumRowsAffected"    ), Types.VARCHAR, "varchar", "varchar(512)", 512, -1, "", String.class);
//
		String schema = getReportingInstance().getDbmsSchemaName();
		
		// Add Charts
		_CmSqlStatementPerDb_SsDbTotExecCnt  = createTsLineChart(conn, schema, "CmSqlStatementPerDb", "SsDbTotExecCnt"  , -1, true, null, "SQL Statements Per DB - Total Statements Executed/sec");
		_CmSqlStatementPerDb_SsDbSumExecTime = createTsLineChart(conn, schema, "CmSqlStatementPerDb", "SsDbSumExecTime" , -1, true, null, "SQL Statements Per DB - Sum Execution Time in ms");
		_CmSqlStatementPerDb_SsDbSumLRead    = createTsLineChart(conn, schema, "CmSqlStatementPerDb", "SsDbSumLRead"    , -1, true, null, "SQL Statements Per DB - Sum Logical Reads");

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
				"SQL Statements captured by AseTune <i>SQL Capture Subsystem</i>, which grabs information from monSysStatement. <br>" +
				"Note: if ASE config 'statement pipe max messages' is configured to low, these counters is not trustworthy <br>" +
				"<br>" +
				"ASE Source table is 'master.dbo.monSysStatement'. <br>" +
				"PCS Source table is 'CmSqlStatementPerDb_diff'. (PCS = Persistent Counter Store) <br>" +
				"");

		// Columns description
		rstm.setColumnDescription("dbname"                , "Database name");
		rstm.setColumnDescription("totalCount"            , "Summary value for 'inSqlBatchCount', 'inStmntCacheCount', 'dynamicStmntCount' and 'inProcedureCount'.");
		rstm.setColumnDescription("sqlBatchCount"         , "Estimated SQL Batch requests. <br><b>Algorithm:</b> if columns 'ProcName' is NULL from monSysStatement <br><b>Note:</b> If the SqlBatch contains several statements, the counter will be incremented for every statement within the SQL Batch.");
		rstm.setColumnDescription("errorCount"            , "Summary value for number of records that had a 'ErrorStatus' greater than 0.");
		rstm.setColumnDescription("inStmntCacheCount"     , "Estimated SQL Statements Batch requests executed as a Statement Cache (compiled). <br><b>Algorithm:</b> if columns 'ProcName' starts with '*ss' from monSysStatement");
		rstm.setColumnDescription("dynamicStmntCount"     , "Estimated Dynamic SQL Statements (ct_dynamic or PreparedStatement using a LWP). <br><b>Algorithm:</b> if columns 'ProcName' starts with '*sq' from monSysStatement");
		rstm.setColumnDescription("inProcedureCount"      , "Estimated SQL Statements requests executed from withing a Stored Proc. <br><b>Algorithm:</b> if columns 'ProcName' does NOT start with '*sq' or '*ss' from monSysStatement");
		rstm.setColumnDescription("inProcNameNullCount"   , "Estimated SQL Statements requests executed where ProcedureId was not 0, but the ProcedureName could NOT be found.. <br><b>Algorithm:</b> if columns 'ProcedureId' != 0 and 'ProcName' is NULL from monSysStatement");

		rstm.setColumnDescription("sumExecTimeMs"         , "Summary of all Executions for this Database Name");
		rstm.setColumnDescription("avgExecTimeMs"         , "Average execution time for this Database Name");

		rstm.setColumnDescription("sumLogicalReads"       , "Summary of all LogicalReads for this Database Name");
		rstm.setColumnDescription("avgLogicalReads"       , "Average LogicalReads for this Database Name");

		rstm.setColumnDescription("sumPhysicalReads"      , "Summary of all PhysicalReads for this Database Name");
		rstm.setColumnDescription("avgPhysicalReads"      , "Average PhysicalReads for this Database Name");

		rstm.setColumnDescription("sumCpuTime"            , "Summary of all CpuTime for this Database Name");
		rstm.setColumnDescription("avgCpuTime"            , "Average CpuTime for this Database Name");

		rstm.setColumnDescription("sumWaitTime"           , "Summary of all WaitTime for this Database Name");
		rstm.setColumnDescription("avgWaitTime"           , "Average WaitTime for this Database Name");

		rstm.setColumnDescription("sumRowsAffected"       , "Summary of all RowsAffected for this Database Name");
		rstm.setColumnDescription("avgRowsAffected"       , "Average RowsAffected for this Database Name");

		rstm.setColumnDescription("LogicalReadsPerRowsAffected", "How efficient we are (low value is better), simply: sumLogicalReads / sumRowsAffected");
	}
//	private void setSectionDescription(ResultSetTableModel rstm)
//	{
//		if (rstm == null)
//			return;
//		
//		// Section description
//		rstm.setDescription(
//				"How many statements has been executed per database are presented here (ordered by: total_time__sum) <br>" +
//				"<br>" +
//				"Postgres Source table is 'pg_stat_statements'. <br>" +
//				"PCS Source table is 'CmPgStatementsSumDb_diff'. (PCS = Persistent Counter Store) <br>" +
//				"The report <i>summarizes</i> (min/max/count/sum/avg) all entries/samples from the <i>CmPgStatementsSumDb_diff</i> table grouped by 'dbname'. <br>" +
//				"Typically the column name <i>postfix</i> will tell you what aggregate function was used. <br>" +
//				"Sparkline Graphs grouped by DB is also be displayed in a separate table below the <i>summary</i> table.<br>" +
//				"");
//
//		// Columns description
//		rstm.setColumnDescription("dbname"                      , "Database name Statements was executed in.");
//		rstm.setColumnDescription("samples_count"               , "Number of entries for this 'dbname' in the report period");
//		rstm.setColumnDescription("SessionSampleTime__min"      , "First entry was sampled for this entry");
//		rstm.setColumnDescription("SessionSampleTime__max"      , "Last entry was sampled for this entry");
//		rstm.setColumnDescription("Duration"                    , "Start/end time presented as HH:MM:SS, so we can see if this entry is just for a short time or if it spans over a long period of time.");
//		rstm.setColumnDescription("CmSampleMs__sum"             , "Number of milliseconds this object has been available for sampling");
//
//		rstm.setColumnDescription("new_stmnts__sum"            , "Number of Statements created");
//		rstm.setColumnDescription("calls__sum"                  , "Number of times executed");
//		rstm.setColumnDescription("avg_time_per_call__avg"      , "Average Execution Time per call              (Algorithm: total_time / calls)");
//		rstm.setColumnDescription("total_time__sum"             , "Total time spent in the statement, in milliseconds");
//		rstm.setColumnDescription("avg_rows_per_call__avg"      , "Average 'number of rows retrived' per call   (Algorithm: rows / calls)");
//		rstm.setColumnDescription("rows_sum"                    , "Total number of rows retrieved or affected by the statement");
//
//		rstm.setColumnDescription("cache_hit_pct__avg"          , "Average blockes found in cache by the statement (Algorithm: 100.0 * shared_blks_hit / (shared_blks_hit + shared_blks_read)");
////		rstm.setColumnDescription("shared_blks_hit_per_row__avg", "Average 'number of cache reads' per call     (Algorithm: shared_blks_hit / calls)");
//		rstm.setColumnDescription("shared_blks_hit__sum"        , "Total number of shared block cache hits by the statement");
//		rstm.setColumnDescription("shared_blks_read__sum"       , "Total number of shared blocks read by the statement");
//		rstm.setColumnDescription("shared_blks_dirtied__sum"    , "Total number of shared blocks dirtied by the statement");
//		rstm.setColumnDescription("shared_blks_written__sum"    , "Total number of shared blocks written by the statement");
//		
//		rstm.setColumnDescription("local_blks_hit__sum"         , "Total number of local block cache hits by the statement");
//		rstm.setColumnDescription("local_blks_read__sum"        , "Total number of local blocks read by the statement");
//		rstm.setColumnDescription("local_blks_dirtied__sum"     , "Total number of local blocks dirtied by the statement");
//		rstm.setColumnDescription("local_blks_written__sum"     , "Total number of local blocks written by the statement");
//
//		rstm.setColumnDescription("temp_blks_read__sum"         , "Total number of temp blocks read by the statement");
//		rstm.setColumnDescription("temp_blks_written__sum"      , "Total number of temp blocks written by the statement");
//
//		rstm.setColumnDescription("blks_read_time__sum"         , "Total time the statement spent reading blocks, in milliseconds (if track_io_timing is enabled, otherwise zero)");
//		rstm.setColumnDescription("blks_write_time__sum"        , "Total time the statement spent writing blocks, in milliseconds (if track_io_timing is enabled, otherwise zero)");
//	}
}
