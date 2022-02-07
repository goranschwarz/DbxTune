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
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.IReportChart;
import com.asetune.pcs.report.content.ReportChartTimeSeriesLine;
import com.asetune.pcs.report.content.SparklineHelper;
import com.asetune.pcs.report.content.SparklineHelper.DataSource;
import com.asetune.pcs.report.content.SparklineHelper.SparkLineParams;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;
import com.asetune.utils.TimeUtils;

public class AseCpuUsageOverview extends AseAbstract
{
//	private static Logger _logger = Logger.getLogger(AseCpuUsageOverview.class);
	private List<String>        _miniChartJsList = new ArrayList<>();

	public AseCpuUsageOverview(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public boolean hasShortMessageText()
	{
		return true;
	}

//	@Override
//	public void writeShortMessageText(Writer w)
//	throws IOException
//	{
//		writeMessageText(w, false);
//	}
//
//	@Override
//	public void writeMessageText(Writer w)
//	throws IOException
//	{
//		writeMessageText(w, true);
//	}

	@Override
	public void writeMessageText(Writer w, MessageType messageType)
	throws IOException
	{
		w.append(getDbxCentralLinkWithDescForGraphs(false, "Below are CPU Graphs/Charts with various information that can help you decide how the DBMS is handling the load.",
				"CmSummary_aaCpuGraph",
				"CmOsMpstat_MpSum",
				"CmEngines_aaReadWriteGraph",
				"CmEngines_cpuSum",
				"CmEngines_cpuEng",
				"CmSysLoad_EngineRunQLengthGraph",
				"CmExecutionTime_CpuUsagePct",
				"CmExecutionTime_TimeGraph",
				"CmSqlStatement_SqlStmntSumLRead",
				"CmSqlStatement_SqlStmntSumCpuTime",
				"CmSummary_LogicalReadGraph",
				"CmSummary_SelectOperationsGraph",
				"CmSummary_IudmOperationsGraph"
				));

		_CmSummary_aaCpuGraph           .writeHtmlContent(w, null, "The above graph may contain <i>extra</i> CPU Usages, which will be CPU Used during I/O completaion checks.");
		_CmOsMpstat_MpSum               .writeHtmlContent(w, null, null);
		_CmSummary_aaDiskGraph          .writeHtmlContent(w, null, "How many disk I/Os was done... To be used in conjunction with '@@cpu_xxx' to decide if CPU is comming from disk or <i>other</i> DBMS load.");
		if (isFullMessageType())
		{
			_CmEngines_cpuSum               .writeHtmlContent(w, null, "The above graph Will only contain CPU Cyckles used to execute User Work.");
			_CmEngines_cpuEng               .writeHtmlContent(w, null, "The above graph Will only contain CPU Cyckles used to execute User Work, but for each ASE Engine.<br>\n"
			                                                              + "So here you can see if you have specififc Engines scheduling work.");
			_CmSysLoad_EngineRunQLengthGraph.writeHtmlContent(w, null, "The above graph shows how many task(s) that are in the Schedulers Execution Queue for each ASE Engine.<br>\n"
			                                                              + "Values above 1 shows that we have to many user tasks waiting to be served/executed and potentially that we are at 100 or a high CPU Usage.");
			_CmExecutionTime_CpuUsagePct    .writeHtmlContent(w, null, null);
		}
		_CmExecutionTime_TimeGraph      .writeHtmlContent(w, null, "The above graph shows what <i>sub system</i> in ASE where we spend most time (Note: Entries for 'Unknown' with values above 100,000,000 (100 sec) are discarded)<br>\n"
		                                                              + "<ul> \n"
		                                                              + "  <li><b>Compilation</b> - Maybe it's time to consider Statement Cache (if not already done), or increase the size of the statement cache.</li>"
		                                                              + "  <li><b>Sorting    </b> - Find SQL Statement that does a lot of sorting and try to do that on the client side if possible, or add index to support that order.</li>"
		                                                              + "  <li><b>Execution  </b> - Hopefully this is where most CPU Cycles is spent.</li>"
		                                                              + "</ul> \n");
		if (_CmExecutionTime_SUM_rstm != null)
		{
			// Get a description of this section, and column names
			w.append(getSectionDescriptionHtml(_CmExecutionTime_SUM_rstm, true));

			// Last sample Database Size info
//			sb.append(_CmExecutionTime_SUM_rstm.toHtmlTableString("sortable"));
			w.append(toHtmlTable(_CmExecutionTime_SUM_rstm));
		}
			
		if (isFullMessageType())
		{
			_CmSqlStatement_SqlStmntSumLRead  .writeHtmlContent(w, null, null);
			_CmSqlStatement_SqlStmntSumCpuTime.writeHtmlContent(w, null, "The above two graphs, shows what SQL Statements (<b>long or short in responce time</b>) we are spending LogicalReads & CPU Time on.<br>"
			                                                      + "This can be used to figure out if it's <i>short</i> or <i>long</i> running Statements that uses most of the machine power...<br>"
			                                                      + "(where should we start to look)... many 'Logical Reads'; then we might have <i>in memory table scans</i>. Lot of 'CPU Time';  it might be <i>sorting</i> or...");
			_CmSummary_LogicalReadGraph       .writeHtmlContent(w, null, null);
			_CmSummary_SelectOperationsGraph  .writeHtmlContent(w, null, null);
			_CmSummary_IudmOperationsGraph    .writeHtmlContent(w, null, null);
		}

		// Write JavaScript code for CPU SparkLine
		if (isFullMessageType())
		{
			for (String str : _miniChartJsList)
				w.append(str);
		}
	}

	@Override
	public String getSubject()
	{
		return "CPU Usage graph of the full day (origin: CmSummary,CmEngines / @@cpu_xxx,monEngines)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


//	@Override
//	public String[] getMandatoryTables()
//	{
//		return new String[] { "CmExecutionTime_diff" };
//	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		int maxValue = 100;
		_CmSummary_aaCpuGraph            = createTsLineChart(conn, "CmSummary",  "aaCpuGraph",            maxValue, null,      "CPU Summary for all Engines (using @@cpu_busy, @@cpu_io) (Summary)");
		_CmOsMpstat_MpSum                = createTsLineChart(conn, "CmOsMpstat", "MpSum",                 maxValue, "idlePct", "OS: CPU usage Summary (Host Monitor->OS CPU(mpstat))");
		_CmSummary_aaDiskGraph           = createTsLineChart(conn, "CmSummary",  "aaReadWriteGraph",      -1,       null,      "Disk read/write per second, using @@total_read, @@total_write (Summary)");
		_CmEngines_cpuSum                = createTsLineChart(conn, "CmEngines",  "cpuSum",                maxValue, null,      "CPU Summary for all Engines (Server->Engines)");
		_CmEngines_cpuEng                = createTsLineChart(conn, "CmEngines",  "cpuEng",                maxValue, null,      "CPU Usage per Engine (System + User) (Server->Engines)");
		_CmSysLoad_EngineRunQLengthGraph = createTsLineChart(conn, "CmSysLoad",  "EngineRunQLengthGraph", -1,       null,      "Run Queue Length, Average over last minute, Per Engine (Server->System Load)");

		// For CmExecutionTime_TimeGraph do not sample values for "Unknown" which is "out-of-bounds"
		String skip1 = null;
		if (true)
			skip1 = ReportChartTimeSeriesLine.SKIP_COLNAME_WITH_VALUE_ABOVE + "Unknown=" + 100_000_000; // 100 seconds means 100 engines at 100% of "Unknown" usage
		
		// For CmSummary_LogicalReadGraph do not sample values for "Unknown" which is "out-of-bounds"
		String skip2 = null;
		if (true)
			skip2 = ReportChartTimeSeriesLine.SKIP_COLNAME_WITH_VALUE_BELOW + "LogicalReads=" + 0; // if lower than 0 it might be a counter wrap or some other strange thing
		
		_CmExecutionTime_CpuUsagePct       = createTsLineChart(conn, "CmExecutionTime", "CpuUsagePct",        maxValue, null,  "ASE SubSystem Operations - CPU Usage Percent (Server->Execution Time)");
		_CmExecutionTime_TimeGraph         = createTsLineChart(conn, "CmExecutionTime", "TimeGraph",          -1,       skip1, "ASE SubSystem Operations - Execution Time, in Micro Seconds (Server->Execution Time)");

		_CmSqlStatement_SqlStmntSumLRead   = createTsLineChart(conn, "CmSqlStatement",  "SqlStmntSumLRead",   -1,       null,  "Sum Logical Reads per sec Over SQL Response Time (Object/Access->SQL Statements)");
		_CmSqlStatement_SqlStmntSumCpuTime = createTsLineChart(conn, "CmSqlStatement",  "SqlStmntSumCpuTime", -1,       skip1, "Sum CPU Time per sec Over SQL Response Time (Object/Access->SQL Statements)");

		_CmSummary_LogicalReadGraph        = createTsLineChart(conn, "CmSummary",       "LogicalReadGraph",      -1,    skip2, "ASE Operations - Logical Reads per Second (Summary)");
		_CmSummary_SelectOperationsGraph   = createTsLineChart(conn, "CmSummary",       "SelectOperationsGraph", -1,    null,  "ASE Operations - Selects per Second (Summary)");
		_CmSummary_IudmOperationsGraph     = createTsLineChart(conn, "CmSummary",       "IudmOperationsGraph",   -1,    null,  "ASE Operations - Ins/Upd/Del/Merge per Second (Summary)");

		_CmExecutionTime_SUM_rstm = createSum_CmExecutionTime(conn);
	}

	private ResultSetTableModel createSum_CmExecutionTime(DbxConnection conn)
	{
		// If the table do not exists... get out of here
		if ( ! DbUtils.checkIfTableExistsNoThrow(conn, null, null, "CmExecutionTime_diff") )
			return null;

		String sql = ""
			    + "select \n"
			    + "     [OperationName] \n"
			    + "    ,cast(sum([ExecutionTime]) / 1000000 as numeric(36,0)) as [ExecutionTime_InSeconds_sum] \n"
			    + "from [CmExecutionTime_diff] \n"
			    + "where 1=1 \n"
			    + getReportPeriodSqlWhere()
			    + "group by [OperationName] \n"
			    + "order by 2 desc \n"
			    + "";

		ResultSetTableModel rstm = executeQuery(conn, sql, true, "CmExecutionTime_diff");
		if (rstm != null)
		{
			// Highlight sort column
			rstm.setHighlightSortColumns("ExecutionTime_InSeconds_sum");

			// Translate "ExecutionTime_InSeconds_sum" to HH:MM:SS
			rstm.addColumn("ExecutionTime as HH:MM:SS", 2, Types.VARCHAR, "varchar", "varchar(30)", 30, 0, "", String.class);

			// Spark-line
			rstm.addColumn("ExecutionTime__chart", 3, Types.VARCHAR, "varchar", "varchar(512)", 512, 0, "", String.class);

			for (int r=0; r<rstm.getRowCount(); r++)
			{
				int    seconds  = rstm.getValueAsInteger(r, 1);
				String secAsStr = TimeUtils.secToTimeStrLong(seconds);

				rstm.setValueAtWithOverride(secAsStr, r, 2);
			}
			
			rstm.setDescription("Summary information about <b>what</b> we are spending CPU time on for the <b>whole</b> reporting period from the table <code>CmExecutionTime_diff</code><br>");

			
			// Mini Chart
			// Get data for: SparkLine - small chart values ... this will do:
			//  -- fill in the data cell with: <span class='aClassName' values='v1, v2, v2, v3...'>Mini Chart Here</span>
			//  -- return JavaScript Code to initialize the Spark line
			String whereKeyColumn = "OperationName"; 

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, rstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("ExecutionTime__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecutionTime_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("ExecutionTime")
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Number of MicroSeconds for 'ExecutionTime' in below period")
					.validate()));

		}
		return rstm;
	}

	private IReportChart _CmSummary_aaCpuGraph;
	private IReportChart _CmOsMpstat_MpSum;
	private IReportChart _CmSummary_aaDiskGraph;
	private IReportChart _CmEngines_cpuSum;
	private IReportChart _CmEngines_cpuEng;
	private IReportChart _CmSysLoad_EngineRunQLengthGraph;
	private IReportChart _CmExecutionTime_CpuUsagePct;
	private IReportChart _CmExecutionTime_TimeGraph;
	private IReportChart _CmSqlStatement_SqlStmntSumLRead;
	private IReportChart _CmSqlStatement_SqlStmntSumCpuTime;
	private IReportChart _CmSummary_LogicalReadGraph;
	private IReportChart _CmSummary_SelectOperationsGraph;
	private IReportChart _CmSummary_IudmOperationsGraph;
	
	private ResultSetTableModel _CmExecutionTime_SUM_rstm;
}
