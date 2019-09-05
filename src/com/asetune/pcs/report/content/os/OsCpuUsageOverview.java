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
package com.asetune.pcs.report.content.os;

import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.ReportChartObject;
import com.asetune.pcs.report.content.ase.AseAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class OsCpuUsageOverview extends AseAbstract
{
//	private static Logger _logger = Logger.getLogger(OsCpuUsageOverview.class);

	public OsCpuUsageOverview(DailySummaryReportAbstract reportingInstance)
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

		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are CPU Graphs/Charts with various information that can help you decide how the DBMS is handling the load on the Operating System Level.",
				"CmOsMpstat_MpSum",
				"CmOsMpstat_MpCpu",
				"CmOsUptime_AdjLoadAverage"
				));

		sb.append(_CmOsMpstat_MpSum         .getHtmlContent(null, null));
		sb.append(_CmOsMpstat_MpCpu         .getHtmlContent(null, null));
		sb.append(_CmOsUptime_AdjLoadAverage.getHtmlContent(null, null));
		
		if (hasProblem())
			sb.append("<pre>").append(getProblem()).append("</pre> \n");

		sb.append("\n<br>");

		return sb.toString();
	}

	@Override
	public String getSubject()
	{
		return "OS CPU Usage graph of the full day (origin: CmOsMpstat_abs,CmOsUptime_abs / os-cmd:mpstat,uptime)";
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
		_CmOsMpstat_MpSum          = createChart(conn, "CmOsMpstat", "MpSum",          maxValue, null, "mpstat: CPU usage Summary (Host Monitor->OS CPU(mpstat))");
		_CmOsMpstat_MpCpu          = createChart(conn, "CmOsMpstat", "MpCpu",          maxValue, null, "mpstat: CPU usage per core (usr+sys+iowait) (Host Monitor->OS CPU(mpstat))");
		_CmOsUptime_AdjLoadAverage = createChart(conn, "CmOsUptime", "AdjLoadAverage", -1,       null, "uptime: Adjusted Load Average (Host Monitor->OS Load Average(uptime))");
	}

	private ReportChartObject _CmOsMpstat_MpSum;
	private ReportChartObject _CmOsMpstat_MpCpu;
	private ReportChartObject _CmOsUptime_AdjLoadAverage;
}
