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

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.IReportChart;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class AseCmSqlStatement extends AseAbstract
{
//	private static Logger _logger = Logger.getLogger(AseCmSqlStatement.class);

	private ResultSetTableModel _shortRstm;

	public AseCmSqlStatement(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public void writeMessageText(Writer sb)
	throws IOException
	{
		// Get a description of this section, and column names
		sb.append(getSectionDescriptionHtml(_shortRstm, true));

		// Last sample Database Size info
		sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
		sb.append(toHtmlTable(_shortRstm));
		
		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are SQL Statements Graphs/Charts with various information that can help you set a baseline or to  find out where we have performance issues.",
				"CmSqlStatement_SqlStmnt",
				"CmSqlStatement_SqlStmntTSpanAll",
				"CmSqlStatement_SqlStmntSumExecMs",
				"CmSqlStatement_SqlStmntSumLRead",
				"CmSqlStatement_SqlStmntSumPRead",
				"CmSqlStatement_SqlStmntSumCpuTime",
				"CmSqlStatement_SqlStmntSumWaitTime",
				"CmSqlStatement_SqlStmntSumRowsAfct",
				"CmSqlStatement_SqlStmntSumErrorCnt"
				));

		_CmSqlStatement_SqlStmnt            .writeHtmlContent(sb, null, null);
		_CmSqlStatement_SqlStmntTSpanAll    .writeHtmlContent(sb, null, null);

		_CmSqlStatement_SqlStmntSumExecMs   .writeHtmlContent(sb, null, null);
		_CmSqlStatement_SqlStmntSumLRead    .writeHtmlContent(sb, null, null);
		_CmSqlStatement_SqlStmntSumPRead    .writeHtmlContent(sb, null, null);
		_CmSqlStatement_SqlStmntSumCpuTime  .writeHtmlContent(sb, null, null);
		_CmSqlStatement_SqlStmntSumWaitTime .writeHtmlContent(sb, null, null);
		_CmSqlStatement_SqlStmntSumRowsAfct .writeHtmlContent(sb, null, null);
		_CmSqlStatement_SqlStmntSumErrorCnt .writeHtmlContent(sb, null, null);
	}

//	@Override
//	public String getMessageText()
//	{
//		StringBuilder sb = new StringBuilder();
//
//		// Get a description of this section, and column names
//		sb.append(getSectionDescriptionHtml(_shortRstm, true));
//
//		// Last sample Database Size info
//		sb.append("Row Count: ").append(_shortRstm.getRowCount()).append("<br>\n");
//		sb.append(toHtmlTable(_shortRstm));
//		
//		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are SQL Statements Graphs/Charts with various information that can help you set a baseline or to  find out where we have performance issues.",
//				"CmSqlStatement_SqlStmnt",
//				"CmSqlStatement_SqlStmntTSpanAll",
//				"CmSqlStatement_SqlStmntSumExecMs",
//				"CmSqlStatement_SqlStmntSumLRead",
//				"CmSqlStatement_SqlStmntSumPRead",
//				"CmSqlStatement_SqlStmntSumCpuTime",
//				"CmSqlStatement_SqlStmntSumWaitTime",
//				"CmSqlStatement_SqlStmntSumRowsAfct",
//				"CmSqlStatement_SqlStmntSumErrorCnt"
//				));
//
//		sb.append(_CmSqlStatement_SqlStmnt            .getHtmlContent(null, null));
//		sb.append(_CmSqlStatement_SqlStmntTSpanAll    .getHtmlContent(null, null));
//
//		sb.append(_CmSqlStatement_SqlStmntSumExecMs   .getHtmlContent(null, null));
//		sb.append(_CmSqlStatement_SqlStmntSumLRead    .getHtmlContent(null, null));
//		sb.append(_CmSqlStatement_SqlStmntSumPRead    .getHtmlContent(null, null));
//		sb.append(_CmSqlStatement_SqlStmntSumCpuTime  .getHtmlContent(null, null));
//		sb.append(_CmSqlStatement_SqlStmntSumWaitTime .getHtmlContent(null, null));
//		sb.append(_CmSqlStatement_SqlStmntSumRowsAfct .getHtmlContent(null, null));
//		sb.append(_CmSqlStatement_SqlStmntSumErrorCnt .getHtmlContent(null, null));
//
//		return sb.toString();
//	}

	@Override
	public String getSubject()
	{
		return "SQL Statement Execution Statistics (origin: CmSqlStatement / monSysStatement)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmSqlStatement_diff" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		String sql = ""
			    + "select \n"
			    + "     [statId]                    AS [id] \n"
			    + "    ,[name] \n"
			    + " \n"
			    + "    ,sum([totalCount])           AS [totalCount] \n"
			    + "    ,sum([sqlBatchCount])        AS [sqlBatchCount] \n"
			    + "    ,sum([errorCount])           AS [errorCount] \n"
			    + "    ,sum([inStmntCacheCount])    AS [inStmntCacheCount] \n"
			    + "    ,sum([dynamicStmntCount])    AS [dynamicStmntCount] \n"
			    + "    ,sum([inProcedureCount])     AS [inProcedureCount] \n"
			    + "    ,sum([inProcNameNullCount])  AS [inProcNameNullCount] \n"
			    + " \n"
			    + "    ,sum([sumExecTimeMs])        AS [sumExecTimeMs] \n"
			    + "    ,CASE WHEN sum([totalCount]) = 0 THEN 0 ELSE cast(sum([sumExecTimeMs])    / sum([totalCount]) as int) END  AS [avgExecTimeMs] \n"
			    + " \n"
			    + "    ,sum([sumCpuTime])           AS [sumCpuTime] \n"
			    + "    ,CASE WHEN sum([totalCount]) = 0 THEN 0 ELSE cast(sum([sumCpuTime])       / sum([totalCount]) as int) END  AS [avgCpuTime] \n"
			    + " \n"
			    + "    ,sum([sumWaitTime])          AS [sumWaitTime] \n"
			    + "    ,CASE WHEN sum([totalCount]) = 0 THEN 0 ELSE cast(sum([sumWaitTime])      / sum([totalCount]) as int) END  AS [avgWaitTime] \n"
			    + " \n"
			    + "    ,sum([sumLogicalReads])      AS [sumLogicalReads] \n"
			    + "    ,CASE WHEN sum([totalCount]) = 0 THEN 0 ELSE cast(sum([sumLogicalReads])  / sum([totalCount]) as int) END  AS [avgLogicalReads] \n"
			    + " \n"
			    + "    ,sum([sumPhysicalReads])     AS [sumPhysicalReads] \n"
			    + "    ,CASE WHEN sum([totalCount]) = 0 THEN 0 ELSE cast(sum([sumPhysicalReads]) / sum([totalCount]) as int) END  AS [avgPhysicalReads] \n"
			    + " \n"
			    + "    ,sum([sumRowsAffected])      AS [sumRowsAffected] \n"
			    + "    ,CASE WHEN sum([totalCount]) = 0 THEN 0 ELSE cast(sum([sumRowsAffected])  / sum([totalCount]) as int) END  AS [avgRowsAffected] \n"
			    + " \n"
			    + "    ,CASE WHEN sum([sumRowsAffected]) = 0 THEN 0 ELSE cast(sum([sumLogicalReads]) / sum([sumRowsAffected]) as int) END  AS [LogicalReadsPerRowsAffected] \n"
			    + " \n"
			    + "from [CmSqlStatement_diff] \n"
			    + "where 1 = 1 \n"
				+ getReportPeriodSqlWhere()
			    + "group by [statId] \n"
			    + "order by [statId] \n"
			    + " \n"
			    + "";
		
		_shortRstm = executeQuery(conn, sql, true, "CmSqlStatement_diff");

		// Describe the table
		setSectionDescription(_shortRstm);

		_CmSqlStatement_SqlStmnt              = createTsLineChart(conn, "CmSqlStatement",  "SqlStmnt",            -1, null, "SQL Statements Executed per Sec (Object/Access->SQL Statements)");
		_CmSqlStatement_SqlStmntTSpanAll      = createTsLineChart(conn, "CmSqlStatement",  "SqlStmntTSpanAll",    -1, null, "SQL Statements (all) In Time Span Received per Sec (Object/Access->SQL Statements)");

		_CmSqlStatement_SqlStmntSumExecMs     = createTsLineChart(conn, "CmSqlStatement",  "SqlStmntSumExecMs",   -1, null, "Sum 'Exec Time' per sec Over SQL Response Time (Object/Access->SQL Statements)");
		_CmSqlStatement_SqlStmntSumLRead      = createTsLineChart(conn, "CmSqlStatement",  "SqlStmntSumLRead",    -1, null, "Sum 'Logical Reads' per sec Over SQL Response Time (Object/Access->SQL Statements)");
		_CmSqlStatement_SqlStmntSumPRead      = createTsLineChart(conn, "CmSqlStatement",  "SqlStmntSumPRead",    -1, null, "Sum 'Physical Reads' per sec Over SQL Response Time (Object/Access->SQL Statements)");
		_CmSqlStatement_SqlStmntSumCpuTime    = createTsLineChart(conn, "CmSqlStatement",  "SqlStmntSumCpuTime",  -1, null, "Sum 'CPU Time' per sec Over SQL Response Time (Object/Access->SQL Statements)");
		_CmSqlStatement_SqlStmntSumWaitTime   = createTsLineChart(conn, "CmSqlStatement",  "SqlStmntSumWaitTime", -1, null, "Sum 'Wait Time' per sec Over SQL Response Time (Object/Access->SQL Statements)");
		_CmSqlStatement_SqlStmntSumRowsAfct   = createTsLineChart(conn, "CmSqlStatement",  "SqlStmntSumRowsAfct", -1, null, "Sum 'Rows Affected' per sec Over SQL Response Time (Object/Access->SQL Statements)");
		_CmSqlStatement_SqlStmntSumErrorCnt   = createTsLineChart(conn, "CmSqlStatement",  "SqlStmntSumErrorCnt", -1, null, "Sum SQL Statements Error Count Per Sec (Object/Access->SQL Statements)");
	}

	private IReportChart _CmSqlStatement_SqlStmnt           ;
	private IReportChart _CmSqlStatement_SqlStmntTSpanAll   ;
	private IReportChart _CmSqlStatement_SqlStmntSumExecMs  ;
	private IReportChart _CmSqlStatement_SqlStmntSumLRead   ;
	private IReportChart _CmSqlStatement_SqlStmntSumPRead   ;
	private IReportChart _CmSqlStatement_SqlStmntSumCpuTime ;
	private IReportChart _CmSqlStatement_SqlStmntSumWaitTime;
	private IReportChart _CmSqlStatement_SqlStmntSumRowsAfct;
	private IReportChart _CmSqlStatement_SqlStmntSumErrorCnt;

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
				"PCS Source table is 'CmSqlStatement_diff'. (PCS = Persistent Counter Store) <br>" +
				"");

		// Columns description
		rstm.setColumnDescription("name"                  , "Execution time span for this statistics");
		rstm.setColumnDescription("totalCount"            , "Summary value for 'inSqlBatchCount', 'inStmntCacheCount', 'dynamicStmntCount' and 'inProcedureCount'.");
		rstm.setColumnDescription("sqlBatchCount"         , "Estimated SQL Batch requests. <br><b>Algorithm:</b> if columns 'ProcName' is NULL from monSysStatement <br><b>Note:</b> If the SqlBatch contains several statements, the counter will be incremented for every statement within the SQL Batch.");
		rstm.setColumnDescription("errorCount"            , "Summary value for number of records that had a 'ErrorStatus' greater than 0.");
		rstm.setColumnDescription("inStmntCacheCount"     , "Estimated SQL Statements Batch requests executed as a Statement Cache (compiled). <br><b>Algorithm:</b> if columns 'ProcName' starts with '*ss' from monSysStatement");
		rstm.setColumnDescription("dynamicStmntCount"     , "Estimated Dynamic SQL Statements (ct_dynamic or PreparedStatement using a LWP). <br><b>Algorithm:</b> if columns 'ProcName' starts with '*sq' from monSysStatement");
		rstm.setColumnDescription("inProcedureCount"      , "Estimated SQL Statements requests executed from withing a Stored Proc. <br><b>Algorithm:</b> if columns 'ProcName' does NOT start with '*sq' or '*ss' from monSysStatement");
		rstm.setColumnDescription("inProcNameNullCount"   , "Estimated SQL Statements requests executed where ProcedureId was not 0, but the ProcedureName could NOT be found.. <br><b>Algorithm:</b> if columns 'ProcedureId' != 0 and 'ProcName' is NULL from monSysStatement");

		rstm.setColumnDescription("sumExecTimeMs"         , "Summary of all Executions for this time span");
		rstm.setColumnDescription("avgExecTimeMs"         , "Average execution time for this time span");

		rstm.setColumnDescription("sumLogicalReads"       , "Summary of all LogicalReads for this time span");
		rstm.setColumnDescription("avgLogicalReads"       , "Average LogicalReads for this time span");

		rstm.setColumnDescription("sumPhysicalReads"      , "Summary of all PhysicalReads for this time span");
		rstm.setColumnDescription("avgPhysicalReads"      , "Average PhysicalReads for this time span");

		rstm.setColumnDescription("sumCpuTime"            , "Summary of all CpuTime for this time span");
		rstm.setColumnDescription("avgCpuTime"            , "Average CpuTime for this time span");

		rstm.setColumnDescription("sumWaitTime"           , "Summary of all WaitTime for this time span");
		rstm.setColumnDescription("avgWaitTime"           , "Average WaitTime for this time span");

		rstm.setColumnDescription("sumRowsAffected"       , "Summary of all RowsAffected for this time span");
		rstm.setColumnDescription("avgRowsAffected"       , "Average RowsAffected for this time span");

		rstm.setColumnDescription("LogicalReadsPerRowsAffected", "How efficient we are (low value is better), simply: sumLogicalReads / sumRowsAffected");
	}
}

