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
				"CmSummary_aaReadWriteGraph",
				"CmPerfCounters_CacheReads",
				"CmPerfCounters_CachePhyReads",
				"CmPerfCounters_CacheWrites",
				"CmPerfCounters_CacheHitRate",
				"CmMemoryClerks_MemoryClerkBp",
				"CmMemoryClerks_MemoryClerksTop",
				"CmMemoryClerks_TTMemVsAllClerks",
//				"CmPerfCounters_OsCpuEffective",
				"CmMemoryGrantsSum_GrantedMemSum",
				"CmSummary_OsMemoryFreeMb",
				"CmPerfCounters_TransWriteSec",
				"CmSchedulers_RunQLengthSum",
				"CmSchedulers_RunQLengthEng"
				));

		_CmSummary_aaCpuGraph           .writeHtmlContent(w, null, null);
		_CmOsMpstat_MpSum               .writeHtmlContent(w, null, null);
		_CmSummary_aaReadWriteGraph     .writeHtmlContent(w, null, null);
		_CmPerfCounters_CacheReads      .writeHtmlContent(w, null, null);
		_CmPerfCounters_CachePhyReads   .writeHtmlContent(w, null, null);
		_CmPerfCounters_CacheWrites     .writeHtmlContent(w, null, null);
		_CmPerfCounters_CacheHitRate    .writeHtmlContent(w, null, null);
//		_CmPerfCounters_OsCpuEffective  .writeHtmlContent(sb, null, null);
		_CmMemoryClerks_MemoryClerkBp   .writeHtmlContent(w, null, null);
		_CmMemoryClerks_MemoryClerksTop .writeHtmlContent(w, null, null);
		_CmMemoryClerks_TTMemVsAllClerks.writeHtmlContent(w, null, null);
		_CmMemoryGrantsSum_GrantedMemSum.writeHtmlContent(w, null, null);
		_CmMemoryGrantsSum_GrantWaitCnt .writeHtmlContent(w, null, null);
		_CmSummary_OsMemoryFreeMb       .writeHtmlContent(w, null, null);
		_CmPerfCounters_SqlBatch        .writeHtmlContent(w, null, null);
		_CmPerfCounters_TransWriteSec   .writeHtmlContent(w, null, null);

		if (isFullMessageType())
		{
			_CmSchedulers_RunQLengthSum   .writeHtmlContent(w, null, null);
			_CmSchedulers_RunQLengthEng   .writeHtmlContent(w, null, null);
		}
	}

	@Override
	public String getSubject()
	{
		return "CPU Usage graph of the full day (origin: CmSummary,CmPerfCounters,CmMemoryClerks,CmMemoryGrantsSum,CmSchedulers / @@cpu_xxx,dm_os_schedulers)";
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
		if (isWindows())
		{
			idlePct = "% Idle Time";
		}
//		String dbmsVerStr = getReportingInstance().getDbmsVersionStr();
//		if (StringUtil.hasValue(dbmsVerStr))
//		{
//			if (dbmsVerStr.contains("Windows"))
//			{
//				idlePct = "% Idle Time";
//			}
//		}
		
		String schema = getReportingInstance().getDbmsSchemaName();

		int maxValue = 100;
		_CmSummary_aaCpuGraph            = createTsLineChart(conn, schema, "CmSummary",         "aaCpuGraph",       maxValue, false, null,    "CPU Summary for all Schedulers (using @@cpu_busy, @@cpu_io) (Summary)");
		_CmOsMpstat_MpSum                = createTsLineChart(conn, schema, "CmOsMpstat",        "MpSum",            maxValue, false, idlePct, "OS: CPU usage Summary (Host Monitor->OS CPU(mpstat))");
		_CmSummary_aaReadWriteGraph      = createTsLineChart(conn, schema, "CmSummary",         "aaReadWriteGraph", -1,       false, null,    "Disk read/write per second, using @@total_read, @@total_write (Summary)");
//		_CmPerfCounters_OsCpuEffective   = createTsLineChart(conn, schema, "CmPerfCounters",    "OsCpuEffective",   maxValue, false, null,    "CPU Usage Effective in Percent (Perf Counters)");
		_CmPerfCounters_CacheReads       = createTsLineChart(conn, schema, "CmPerfCounters",    "CacheReads",       -1,       false, null,    "Buffer Cache Reads per Sec (Server->Perf Counters)");
		_CmPerfCounters_CachePhyReads    = createTsLineChart(conn, schema, "CmPerfCounters",    "CachePhyReads",    -1,       false, null,    "Buffer Cache Physical Reads per Sec (Server->Perf Counters)");
		_CmPerfCounters_CacheWrites      = createTsLineChart(conn, schema, "CmPerfCounters",    "CacheWrites",      -1,       false, null,    "Buffer Cache Writes Sec (Server->Perf Counters)");
		_CmPerfCounters_CacheHitRate     = createTsLineChart(conn, schema, "CmPerfCounters",    "CacheHitRate",     -1,       false, null,    "Buffer Cache Hit Rate, in Percent (Server->Perf Counters)");
		_CmMemoryClerks_MemoryClerkBp    = createTsLineChart(conn, schema, "CmMemoryClerks",    "MemoryClerkBp",    -1,       false, null,    "Buffer Pool Memory Clerk, in MB (Server->Memory)");
		_CmMemoryClerks_MemoryClerksTop  = createTsLineChart(conn, schema, "CmMemoryClerks",    "MemoryClerksTop",  -1,       true , null,    "Top ## Memory Clerks, in MB (Server->Memory)");
		_CmMemoryClerks_TTMemVsAllClerks = createTsLineChart(conn, schema, "CmMemoryClerks",    "TTMemVsAllClerks", -1,       false, null,    "All Memory Clerks vs Target & Total Memory, in MB (Server->Memory)");
		_CmMemoryGrantsSum_GrantedMemSum = createTsLineChart(conn, schema, "CmMemoryGrantsSum", "GrantedMemSum",    -1,       false, null,    "Memory Grant Summary in MB (Server->Memory Grants Sum)");
		_CmMemoryGrantsSum_GrantWaitCnt  = createTsLineChart(conn, schema, "CmMemoryGrantsSum", "GrantWaitCnt",     -1,       false, null,    "Memory Grants and Wait Count (Server->Memory Grants Sum)");
		_CmSummary_OsMemoryFreeMb        = createTsLineChart(conn, schema, "CmSummary",         "OsMemoryFreeMb",   -1,       false, null,    "OS Free/Available Memory in MB (Summary)");
		_CmPerfCounters_SqlBatch         = createTsLineChart(conn, schema, "CmPerfCounters",    "SqlBatch",         -1,       false, null,    "SQL Batches Received per Sec (Server->Perf Counters)");
		_CmPerfCounters_TransWriteSec    = createTsLineChart(conn, schema, "CmPerfCounters",    "TransWriteSec",    -1,       true , null,    "Write Transactions per Sec (Server->Perf Counters)");
		_CmSchedulers_RunQLengthSum      = createTsLineChart(conn, schema, "CmSchedulers",      "RunQLengthSum",    -1,       false, null,    "Runnable Queue Length, Summary (using dm_os_schedulers.runnable_tasks_count)");
		_CmSchedulers_RunQLengthEng      = createTsLineChart(conn, schema, "CmSchedulers",      "RunQLengthEng",    -1,       false, null,    "Runnable Queue Length, per Scheduler (using dm_os_schedulers.runnable_tasks_count)");
	}
	
	private IReportChart _CmSummary_aaCpuGraph;
	private IReportChart _CmOsMpstat_MpSum;
	private IReportChart _CmSummary_aaReadWriteGraph;
	private IReportChart _CmPerfCounters_CacheReads;
	private IReportChart _CmPerfCounters_CachePhyReads;
	private IReportChart _CmPerfCounters_CacheWrites;
	private IReportChart _CmPerfCounters_CacheHitRate;
//	private IReportChart _CmPerfCounters_OsCpuEffective;
	private IReportChart _CmMemoryClerks_MemoryClerkBp;
	private IReportChart _CmMemoryClerks_MemoryClerksTop;
	private IReportChart _CmMemoryClerks_TTMemVsAllClerks;
	private IReportChart _CmMemoryGrantsSum_GrantedMemSum;
	private IReportChart _CmMemoryGrantsSum_GrantWaitCnt;
	private IReportChart _CmSummary_OsMemoryFreeMb;
	private IReportChart _CmPerfCounters_SqlBatch;
	private IReportChart _CmPerfCounters_TransWriteSec;
	private IReportChart _CmSchedulers_RunQLengthSum;
	private IReportChart _CmSchedulers_RunQLengthEng;
}
