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

import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.IReportChart;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public class SqlServerCpuUsageOverview 
extends SqlServerAbstract
{
//	private static Logger _logger = Logger.getLogger(SqlServerCpuUsageOverview.class);

	public SqlServerCpuUsageOverview(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public boolean hasShortMessageText()
	{
		return true;
	}

	@Override
	public void writeShortMessageText(Writer w)
	throws IOException
	{
		writeMessageText(w, false);
	}

	@Override
	public void writeMessageText(Writer w)
	throws IOException
	{
		writeMessageText(w, true);
	}

//	@Override
	public void writeMessageText(Writer w, boolean isFullText)
	throws IOException
	{
		w.append(getDbxCentralLinkWithDescForGraphs(false, "Below are CPU Graphs/Charts with various information that can help you decide how the DBMS is handling the load.",
				"CmSummary_aaCpuGraph",
				"CmOsMpstat_MpSum",
				"CmSummary_aaReadWriteGraph",
				"CmPerfCounters_CacheReads",
				"CmPerfCounters_CacheHitRate",
//				"CmPerfCounters_OsCpuEffective",
				"CmPerfCounters_SqlBatch",
				"CmPerfCounters_TransWriteSec",
				"CmSchedulers_RunQLengthSum",
				"CmSchedulers_RunQLengthEng"
				));

		_CmSummary_aaCpuGraph         .writeHtmlContent(w, null, null);
		_CmOsMpstat_MpSum             .writeHtmlContent(w, null, null);
		_CmSummary_aaReadWriteGraph   .writeHtmlContent(w, null, null);
		_CmPerfCounters_CacheReads    .writeHtmlContent(w, null, null);
		_CmPerfCounters_CacheHitRate  .writeHtmlContent(w, null, null);
//		_CmPerfCounters_OsCpuEffective.writeHtmlContent(sb, null, null);
		_CmPerfCounters_SqlBatch      .writeHtmlContent(w, null, null);
		_CmPerfCounters_TransWriteSec .writeHtmlContent(w, null, null);
		if (isFullText)
		{
			_CmSchedulers_RunQLengthSum   .writeHtmlContent(w, null, null);
			_CmSchedulers_RunQLengthEng   .writeHtmlContent(w, null, null);
		}
	}

	@Override
	public String getSubject()
	{
		return "CPU Usage graph of the full day (origin: CmSummary,CmPerfCounters,CmSchedulers / @@cpu_xxx,dm_os_schedulers)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		// For Linux/Unix do NOT show chart-line for "idlePct"
		String idlePct = "idlePct";

		// If the SQL Server is hosted on Windows... remove chart-line: "% Idle Time" 
		String dbmsVerStr = getReportingInstance().getDbmsVersionStr();
		if (StringUtil.hasValue(dbmsVerStr))
		{
			if (dbmsVerStr.contains("Windows"))
			{
				idlePct = "% Idle Time";
			}
		}
		

		int maxValue = 100;
		_CmSummary_aaCpuGraph          = createTsLineChart(conn, "CmSummary",      "aaCpuGraph",       maxValue, null,    "CPU Summary for all Schedulers (using @@cpu_busy, @@cpu_io) (Summary)");
		_CmOsMpstat_MpSum              = createTsLineChart(conn, "CmOsMpstat",     "MpSum",            maxValue, idlePct, "OS: CPU usage Summary (Host Monitor->OS CPU(mpstat))");
		_CmSummary_aaReadWriteGraph    = createTsLineChart(conn, "CmSummary",      "aaReadWriteGraph", -1,       null,    "Disk read/write per second, using @@total_read, @@total_write");
//		_CmPerfCounters_OsCpuEffective = createTsLineChart(conn, "CmPerfCounters", "OsCpuEffective",   maxValue, null,    "CPU Usage Effective in Percent (Perf Counters)");
		_CmPerfCounters_CacheReads     = createTsLineChart(conn, "CmPerfCounters", "CacheReads",       -1,       null,    "Buffer Cache Reads per Sec (Server->Perf Counters)");
		_CmPerfCounters_CacheHitRate   = createTsLineChart(conn, "CmPerfCounters", "CacheHitRate",     -1,       null,    "Buffer Cache Hit Rate, in Percent (Server->Perf Counters)");
		_CmPerfCounters_SqlBatch       = createTsLineChart(conn, "CmPerfCounters", "SqlBatch",         -1,       null,    "SQL Batches Received per Sec (Perf Counters)");
		_CmPerfCounters_TransWriteSec  = createTsLineChart(conn, "CmPerfCounters", "TransWriteSec",    -1,       null,    "Write Transactions per Sec (Perf Counters)");
		_CmSchedulers_RunQLengthSum    = createTsLineChart(conn, "CmSchedulers",   "RunQLengthSum",    -1,       null,    "Runnable Queue Length, Summary (using dm_os_schedulers.runnable_tasks_count)");
		_CmSchedulers_RunQLengthEng    = createTsLineChart(conn, "CmSchedulers",   "RunQLengthEng",    -1,       null,    "Runnable Queue Length, per Scheduler (using dm_os_schedulers.runnable_tasks_count)");
	}

	private IReportChart _CmSummary_aaCpuGraph;
	private IReportChart _CmOsMpstat_MpSum;
	private IReportChart _CmSummary_aaReadWriteGraph;
	private IReportChart _CmPerfCounters_CacheReads;
	private IReportChart _CmPerfCounters_CacheHitRate;
//	private IReportChart _CmPerfCounters_OsCpuEffective;
	private IReportChart _CmPerfCounters_SqlBatch;
	private IReportChart _CmPerfCounters_TransWriteSec;
	private IReportChart _CmSchedulers_RunQLengthSum;
	private IReportChart _CmSchedulers_RunQLengthEng;
}
