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

import com.dbxtune.cm.os.CmOsIostat;
import com.dbxtune.cm.os.CmOsMpstat;
import com.dbxtune.cm.sqlserver.CmIndexOpStatSum;
import com.dbxtune.cm.sqlserver.CmMemoryClerks;
import com.dbxtune.cm.sqlserver.CmMemoryGrantsSum;
import com.dbxtune.cm.sqlserver.CmPerfCounters;
import com.dbxtune.cm.sqlserver.CmSchedulers;
import com.dbxtune.cm.sqlserver.CmSummary;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

public class SqlServerCpuUsageOverview 
extends SqlServerAbstract
{
//	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public SqlServerCpuUsageOverview(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public boolean hasMinimalMessageText()
	{
		return true;
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
				"CmSchedulers_CpuComboSch",
				"CmOsMpstat_MpSum",
				"CmSummary_aaReadWriteGraph",
				"CmOsIostat_IoBusyPct",
				"CmPerfCounters_SqlBatch",
				"CmIndexOpStatSum_OpIudInstance",
				"CmIndexOpStatSum_OpIudTempdb",
				"CmIndexOpStatSum_OpSingletonLookupCount",
				"CmIndexOpStatSum_OpRangeScanCount",
				"CmPerfCounters_CacheReads",
				"CmPerfCounters_CachePhyReads",
				"CmPerfCounters_CacheWrites",
				"CmPerfCounters_CacheHitRate",
				"CmPerfCounters_OsCpuEffective",
				"CmMemoryClerks_MemoryClerkBp",
				"CmMemoryClerks_MemoryClerksTop",
				"CmMemoryClerks_TTMemVsAllClerks",
				"CmMemoryGrantsSum_GrantedMemSum",
				"CmMemoryGrantsSum_GrantWaitCnt",
				"CmSummary_OsMemoryFreeMb",
				"CmPerfCounters_TransWriteSec",
				"CmSchedulers_RunQLengthSum",
				"CmSchedulers_RunQLengthEng"
				));

		_CmSummary_aaCpuGraph                   .writeHtmlContent(w, null, "<b>Note</b>: The above graph may show less usage, especially if the server has been up for some time... <i>(due to counter wrapping in @@cpu_busy)</i>");
		_CmSchedulers_CpuComboSch               .writeHtmlContent(w, null, null);
		_CmOsMpstat_MpSum                       .writeHtmlContent(w, null, null);
		_CmSummary_aaReadWriteGraph             .writeHtmlContent(w, null, null);
		_CmOsIostat_IoBusyPct                   .writeHtmlContent(w, null, null);
		_CmPerfCounters_SqlBatch                .writeHtmlContent(w, null, null);
		_CmIndexOpStatSum_OpIudInstance         .writeHtmlContent(w, null, null);
		_CmIndexOpStatSum_OpIudTempdb           .writeHtmlContent(w, null, null);
		_CmIndexOpStatSum_OpSingletonLookupCount.writeHtmlContent(w, null, null);
		_CmIndexOpStatSum_OpRangeScanCount      .writeHtmlContent(w, null, "From the above 'Range Scan' and 'Singleton Lookups', comparing the below 'Buffer Cache Reads' you may possibly figure out if we are doing large Table/Index Scans...");
		_CmPerfCounters_CacheReads              .writeHtmlContent(w, null, null);
		_CmPerfCounters_CachePhyReads           .writeHtmlContent(w, null, null);
		_CmPerfCounters_CacheWrites             .writeHtmlContent(w, null, null);
		_CmPerfCounters_CacheHitRate            .writeHtmlContent(w, null, null);
		_CmMemoryClerks_MemoryClerkBp           .writeHtmlContent(w, null, null);
		_CmMemoryClerks_MemoryClerksTop         .writeHtmlContent(w, null, null);
		_CmMemoryClerks_TTMemVsAllClerks        .writeHtmlContent(w, null, null);
		_CmMemoryGrantsSum_GrantedMemSum        .writeHtmlContent(w, null, null);
		_CmMemoryGrantsSum_GrantWaitCnt         .writeHtmlContent(w, null, null);
		_CmSummary_OsMemoryFreeMb               .writeHtmlContent(w, null, null);
		_CmPerfCounters_TransWriteSec           .writeHtmlContent(w, null, null);

		if (isFullMessageType())
		{
			_CmSchedulers_RunQLengthSum         .writeHtmlContent(w, null, null);
			_CmSchedulers_RunQLengthEng         .writeHtmlContent(w, null, null);
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
		
		String schema = getReportingInstance().getDbmsSchemaName();

		int maxValue = 100;
		_CmSummary_aaCpuGraph                    = createTsLineChart(conn, schema, CmSummary        .CM_NAME, CmSummary        .GRAPH_NAME_AA_CPU,                        maxValue, false, null,    "CPU Summary for all Schedulers (using @@cpu_busy, @@cpu_io) (Summary)");
		_CmSchedulers_CpuComboSch                = createTsLineChart(conn, schema, CmSchedulers     .CM_NAME, CmSchedulers     .GRAPH_NAME_CPU_COMBO_SCHEDULERS,          maxValue, false, null,    "CPU Usage in Percent, Both Average and Per Schedulers (using dm_os_schedulers.total_cpu_usage_ms)");
		_CmOsMpstat_MpSum                        = createTsLineChart(conn, schema, CmOsMpstat       .CM_NAME, CmOsMpstat       .GRAPH_NAME_MpSum,                         maxValue, false, idlePct, "OS: CPU usage Summary (Host Monitor->OS CPU(mpstat))");
		_CmSummary_aaReadWriteGraph              = createTsLineChart(conn, schema, CmSummary        .CM_NAME, CmSummary        .GRAPH_NAME_AA_DISK_READ_WRITE,            -1,       false, null,    "Disk read/write per second, using @@total_read, @@total_write (Summary)");
		_CmOsIostat_IoBusyPct                    = createTsLineChart(conn, schema, CmOsIostat       .CM_NAME, CmOsIostat       .GRAPH_NAME_BusyPct,                       maxValue, false, null,    "OS: Disk Busy Percent(utilPct) per Device (Host Monitor->OS Disk Stat(iostat))");
		_CmPerfCounters_SqlBatch                 = createTsLineChart(conn, schema, CmPerfCounters   .CM_NAME, CmPerfCounters   .GRAPH_NAME_SQL_BATCHES_SEC,               -1,       false, null,    "SQL Batches Received per Sec (Server->Perf Counters)");
		_CmIndexOpStatSum_OpIudInstance          = createTsLineChart(conn, schema, CmIndexOpStatSum .CM_NAME, CmIndexOpStatSum .GRAPH_NAME_IUD_INSTANCE,                  -1,       false, null,    "Insert/Update/Delete Rows Per Second at Instance Level (Object/Access->Index Op Sum)");
		_CmIndexOpStatSum_OpIudTempdb            = createTsLineChart(conn, schema, CmIndexOpStatSum .CM_NAME, CmIndexOpStatSum .GRAPH_NAME_IUD_TEMPDB,                    -1,       false, null,    "Insert/Update/Delete Rows Per Second for tempdb (Object/Access->Index Op Sum)");
		_CmIndexOpStatSum_OpSingletonLookupCount = createTsLineChart(conn, schema, CmIndexOpStatSum .CM_NAME, CmIndexOpStatSum .GRAPH_NAME_PER_DB_SINGLETON_LOOKUP_COUNT, -1,       true,  null,    "Bookmark/Singleton Lookups Per Second per Database [singleton_lookup_count] (Object/Access->Index Op Sum)");
		_CmIndexOpStatSum_OpRangeScanCount       = createTsLineChart(conn, schema, CmIndexOpStatSum .CM_NAME, CmIndexOpStatSum .GRAPH_NAME_PER_DB_RANGE_SCAN_COUNT,       -1,       true,  null,    "Range Scans Per Second per Database [range_scan_count] (Object/Access->Index Op Sum)");
		_CmPerfCounters_CacheReads               = createTsLineChart(conn, schema, CmPerfCounters   .CM_NAME, CmPerfCounters   .GRAPH_NAME_CACHE_READS,                   -1,       false, null,    "Buffer Cache Reads per Sec (Server->Perf Counters)");
		_CmPerfCounters_CachePhyReads            = createTsLineChart(conn, schema, CmPerfCounters   .CM_NAME, CmPerfCounters   .GRAPH_NAME_CACHE_PHY_READS,               -1,       false, null,    "Buffer Cache Physical Reads per Sec (Server->Perf Counters)");
		_CmPerfCounters_CacheWrites              = createTsLineChart(conn, schema, CmPerfCounters   .CM_NAME, CmPerfCounters   .GRAPH_NAME_CACHE_WRITES,                  -1,       false, null,    "Buffer Cache Writes Sec (Server->Perf Counters)");
		_CmPerfCounters_CacheHitRate             = createTsLineChart(conn, schema, CmPerfCounters   .CM_NAME, CmPerfCounters   .GRAPH_NAME_CACHE_HIT_RATE,                -1,       false, null,    "Buffer Cache Hit Rate, in Percent (Server->Perf Counters)");
		_CmMemoryClerks_MemoryClerkBp            = createTsLineChart(conn, schema, CmMemoryClerks   .CM_NAME, CmMemoryClerks   .GRAPH_NAME_MEMORY_CLERK_BUFFER_POOL,      -1,       false, null,    "Buffer Pool Memory Clerk, in MB (Server->Memory)");
		_CmMemoryClerks_MemoryClerksTop          = createTsLineChart(conn, schema, CmMemoryClerks   .CM_NAME, CmMemoryClerks   .GRAPH_NAME_MEMORY_CLERKS_TOP,             -1,       true , null,    "Top ## Memory Clerks, in MB (Server->Memory)");
		_CmMemoryClerks_TTMemVsAllClerks         = createTsLineChart(conn, schema, CmMemoryClerks   .CM_NAME, CmMemoryClerks   .GRAPH_NAME_MEMORY_TTM_VS_ALL_CLERKS,      -1,       false, null,    "All Memory Clerks vs Target & Total Memory, in MB (Server->Memory)");
		_CmMemoryGrantsSum_GrantedMemSum         = createTsLineChart(conn, schema, CmMemoryGrantsSum.CM_NAME, CmMemoryGrantsSum.GRAPH_NAME_GRANTED_MEMORY_SUM,            -1,       false, null,    "Memory Grant Summary in MB (Server->Memory Grants Sum)");
		_CmMemoryGrantsSum_GrantWaitCnt          = createTsLineChart(conn, schema, CmMemoryGrantsSum.CM_NAME, CmMemoryGrantsSum.GRAPH_NAME_GRANTEE_WAITER_COUNT,          -1,       false, null,    "Memory Grants and Wait Count (Server->Memory Grants Sum)");
		_CmSummary_OsMemoryFreeMb                = createTsLineChart(conn, schema, CmSummary        .CM_NAME, CmSummary        .GRAPH_NAME_OS_MEMORY_FREE_MB,             -1,       false, null,    "OS Free/Available Memory in MB (Summary)");
		_CmPerfCounters_TransWriteSec            = createTsLineChart(conn, schema, CmPerfCounters   .CM_NAME, CmPerfCounters   .GRAPH_NAME_TRANS_WRITE_SEC,               -1,       true , null,    "Write Transactions per Sec (Server->Perf Counters)");
		_CmSchedulers_RunQLengthSum              = createTsLineChart(conn, schema, CmSchedulers     .CM_NAME, CmSchedulers     .GRAPH_NAME_RUN_QUEUE_LENGTH_SUM,          -1,       false, null,    "Runnable Queue Length, Summary (using dm_os_schedulers.runnable_tasks_count)");
		_CmSchedulers_RunQLengthEng              = createTsLineChart(conn, schema, CmSchedulers     .CM_NAME, CmSchedulers     .GRAPH_NAME_RUN_QUEUE_LENGTH_ENG,          -1,       false, null,    "Runnable Queue Length, per Scheduler (using dm_os_schedulers.runnable_tasks_count)");
	}
	
	private IReportChart _CmSummary_aaCpuGraph;
	private IReportChart _CmSchedulers_CpuComboSch;
	private IReportChart _CmOsMpstat_MpSum;
	private IReportChart _CmSummary_aaReadWriteGraph;
	private IReportChart _CmOsIostat_IoBusyPct;
	private IReportChart _CmPerfCounters_SqlBatch;
	private IReportChart _CmIndexOpStatSum_OpIudInstance;
	private IReportChart _CmIndexOpStatSum_OpIudTempdb;
	private IReportChart _CmIndexOpStatSum_OpSingletonLookupCount;
	private IReportChart _CmIndexOpStatSum_OpRangeScanCount;
	private IReportChart _CmPerfCounters_CacheReads;
	private IReportChart _CmPerfCounters_CachePhyReads;
	private IReportChart _CmPerfCounters_CacheWrites;
	private IReportChart _CmPerfCounters_CacheHitRate;
	private IReportChart _CmMemoryClerks_MemoryClerkBp;
	private IReportChart _CmMemoryClerks_MemoryClerksTop;
	private IReportChart _CmMemoryClerks_TTMemVsAllClerks;
	private IReportChart _CmMemoryGrantsSum_GrantedMemSum;
	private IReportChart _CmMemoryGrantsSum_GrantWaitCnt;
	private IReportChart _CmSummary_OsMemoryFreeMb;
	private IReportChart _CmPerfCounters_TransWriteSec;
	private IReportChart _CmSchedulers_RunQLengthSum;
	private IReportChart _CmSchedulers_RunQLengthEng;
}
