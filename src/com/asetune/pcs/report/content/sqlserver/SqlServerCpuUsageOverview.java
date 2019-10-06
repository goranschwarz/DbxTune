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
package com.asetune.pcs.report.content.sqlserver;

import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.ReportChartObject;
import com.asetune.pcs.report.content.ase.AseAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class SqlServerCpuUsageOverview extends AseAbstract
{
//	private static Logger _logger = Logger.getLogger(SqlServerCpuUsageOverview.class);

	public SqlServerCpuUsageOverview(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public String getMessageText()
	{
		StringBuilder sb = new StringBuilder();

		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are CPU Graphs/Charts with various information that can help you decide how the DBMS is handling the load.",
				"CmSummary_aaCpuGraph",
				"CmSchedulers_RunQLengthSum",
				"CmSchedulers_RunQLengthEng"
				));

		sb.append(_CmSummary_aaCpuGraph      .getHtmlContent(null, null));
		sb.append(_CmSchedulers_RunQLengthSum.getHtmlContent(null, null));
		sb.append(_CmSchedulers_RunQLengthEng.getHtmlContent(null, null));

		return sb.toString();
	}

	@Override
	public String getSubject()
	{
		return "CPU Usage graph of the full day (origin: CmSummary,CmSchedulers / @@cpu_xxx,dm_os_schedulers)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		int maxValue = 100;
		_CmSummary_aaCpuGraph       = createChart(conn, "CmSummary",    "aaCpuGraph",    maxValue, null, "CPU Summary for all Schedulers (using @@cpu_busy, @@cpu_io) (Summary)");
		_CmSchedulers_RunQLengthSum = createChart(conn, "CmSchedulers", "RunQLengthSum", -1,       null, "Runnable Queue Length, Summary (using dm_os_schedulers.runnable_tasks_count)");
		_CmSchedulers_RunQLengthEng = createChart(conn, "CmSchedulers", "RunQLengthEng", -1,       null, "Runnable Queue Length, per Scheduler (using dm_os_schedulers.runnable_tasks_count)");
	}

	private ReportChartObject _CmSummary_aaCpuGraph;
	private ReportChartObject _CmSchedulers_RunQLengthSum;
	private ReportChartObject _CmSchedulers_RunQLengthEng;
}
