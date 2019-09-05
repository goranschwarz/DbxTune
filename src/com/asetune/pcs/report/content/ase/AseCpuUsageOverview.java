/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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

import org.jfree.chart.JFreeChart;

import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.ReportChartObject;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class AseCpuUsageOverview extends AseAbstract
{
//	private static Logger _logger = Logger.getLogger(AseCpuUsageOverview.class);

	public AseCpuUsageOverview(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public String getMsgAsText()
	{
		return "Text Report is not implemented";
	}

	@Override
	public String getMsgAsHtml()
	{
		StringBuilder sb = new StringBuilder();

		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are CPU Graphs/Charts with various information that can help you decide how the DBMS is handling the load.",
				"CmSummary_aaCpuGraph",
				"CmEngines_cpuSum",
				"CmEngines_cpuEng",
				"CmSysLoad_EngineRunQLengthGraph"
				));

		sb.append(_CmSummary_aaCpuGraph           .getHtmlContent(null, "The above graph may contain <i>extra</i> CPU Usages, which will be CPU Used during I/O completaion checks."));
		sb.append(_CmEngines_cpuSum               .getHtmlContent(null, "The above graph Will only contain CPU Cyckles used to execute User Work."));
		sb.append(_CmEngines_cpuEng               .getHtmlContent(null, "The above graph Will only contain CPU Cyckles used to execute User Work, but for each ASE Engine.<br>\n"
		                                                              + "So here you can see if you have specififc Engines scheduling work."));
		sb.append(_CmSysLoad_EngineRunQLengthGraph.getHtmlContent(null, "The above graph shows how many task(s) that are in the Schedulers Execution Queue for each ASE Engine.<br>\n"
		                                                              + "Values above 1 shows that we have to many user tasks waiting to be served/executed and potentially that we are at 100% or a high CPU Usage."));

		if (hasProblem())
			sb.append("<pre>").append(getProblem()).append("</pre> \n");

		sb.append("\n<br>");

		return sb.toString();
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


	@Override
	public void create(DbxConnection conn, String srvName, Configuration conf)
	{
		int maxValue = 100;
		_CmSummary_aaCpuGraph            = createChart(conn, "CmSummary", "aaCpuGraph",            maxValue, null, "CPU Summary for all Engines (using @@cpu_busy, @@cpu_io) (Summary)");
		_CmEngines_cpuSum                = createChart(conn, "CmEngines", "cpuSum",                maxValue, null, "CPU Summary for all Engines (Server->Engines)");
		_CmEngines_cpuEng                = createChart(conn, "CmEngines", "cpuEng",                maxValue, null, "CPU Usage per Engine (System + User) (Server->Engines)");
		_CmSysLoad_EngineRunQLengthGraph = createChart(conn, "CmSysLoad", "EngineRunQLengthGraph", -1,       null, "Run Queue Length, Average over last minute, Per Engine (Server->System Load)");
	}

	private ReportChartObject _CmSummary_aaCpuGraph;
	private ReportChartObject _CmEngines_cpuSum;
	private ReportChartObject _CmEngines_cpuEng;
	private ReportChartObject _CmSysLoad_EngineRunQLengthGraph;
}
