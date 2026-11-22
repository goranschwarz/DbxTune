/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.cloud;

import java.sql.Timestamp;

/**
 * Normalized snapshot of Cloud SQL / RDS / Azure DB infrastructure metrics for a single poll window.
 * <p>
 * Populated by a single batched refresh in the cloud client and consumed by:
 * <ul>
 *   <li><code>MonitorMpstatCloud</code> (CPU utilization &amp; reserved cores)</li>
 *   <li><code>MonitorIoCloud</code> (Disk read/write IOPS &amp; throughput vs quota)</li>
 *   <li><code>MonitorMeminfoCloud</code> (Memory quota, usage, cache, free, swap)</li>
 *   <li><code>MonitorDiskSpaceCloud</code> (Storage quota, total used, data, WAL, tmp breakdown)</li>
 *   <li><code>MonitorNwInfoCloud</code> (Network received/transmitted bytes &amp; active connections)</li>
 *   <li><code>MonitorUpTimeCloud</code> (Instance uptime &amp; synthetic load indicators)</li>
 *   <li><code>HostMonOsInfoConfigText</code> (Recording Information header in Daily Summary Report)</li>
 * </ul>
 */
public class CloudMetricsSnapshot
{
	private long      _fetchTimeMs         = System.currentTimeMillis();
	private Timestamp _metricTimestamp     = new Timestamp(_fetchTimeMs);

	// Instance Metadata (from metric resource/metric labels or SQL Admin metadata)
	private String    _providerName        = "Google Cloud SQL";
	private String    _projectId           = "";
	private String    _region              = "";
	private String    _instanceId          = "";
	private String    _databaseVersion     = "";
	private String    _instanceTier        = "";
	private String    _instanceState       = "RUNNABLE";

	// CPU metrics
	private double    _cpuUtilizationPct   = 0.0;  // 0.0 - 100.0
	private int       _cpuReservedCores    = 0;    // vCPUs
	private double    _cpuUsageTimeSecPerS = 0.0;  // core-seconds per second

	// Memory metrics (in Bytes)
	private long      _memoryQuotaBytes    = 0L;
	private long      _memoryTotalUsageBytes = 0L; // usage + cache
	private long      _memoryUsageBytes    = 0L;   // active resident memory
	private long      _memoryCacheBytes    = 0L;   // page cache
	private long      _memoryFreeBytes     = 0L;   // quota - total_usage
	private long      _swapUsedBytes       = 0L;
	private long      _swapFreeBytes       = 0L;

	// Disk I/O metrics (Rates per second and Quotas)
	private double    _diskReadOpsPerSec   = 0.0;
	private double    _diskWriteOpsPerSec  = 0.0;
	private double    _diskReadBytesPerSec = 0.0;
	private double    _diskWriteBytesPerSec= 0.0;
	private long      _diskReadOpsQuota    = 0L;
	private long      _diskWriteOpsQuota   = 0L;
	private long      _diskReadBytesQuota  = 0L;
	private long      _diskWriteBytesQuota = 0L;

	// Disk Space metrics (in Bytes)
	private long      _diskQuotaBytes      = 0L;
	private long      _diskBytesUsed       = 0L;
	private long      _diskDataBytes       = 0L;
	private long      _diskBinlogBytes     = 0L;   // WAL / binlog
	private long      _diskTmpBytes        = 0L;
	private long      _diskOtherBytes      = 0L;

	// Network metrics
	private long      _netRxBytesTotal     = 0L;   // Cumulative counter or synthesized monotonic counter from rate
	private long      _netTxBytesTotal     = 0L;
	private double    _netRxBytesPerSec    = 0.0;
	private double    _netTxBytesPerSec    = 0.0;
	private int       _activeConnections   = 0;

	// Uptime
	private long      _uptimeSeconds       = 0L;

	public long      getFetchTimeMs()                         { return _fetchTimeMs; }
	public void      setFetchTimeMs(long fetchTimeMs)         { _fetchTimeMs = fetchTimeMs; }

	public Timestamp getMetricTimestamp()                     { return _metricTimestamp; }
	public void      setMetricTimestamp(Timestamp ts)         { _metricTimestamp = ts != null ? ts : new Timestamp(System.currentTimeMillis()); }

	public String    getProviderName()                        { return _providerName; }
	public void      setProviderName(String providerName)     { _providerName = providerName != null ? providerName : ""; }

	public String    getProjectId()                           { return _projectId; }
	public void      setProjectId(String projectId)           { _projectId = projectId != null ? projectId : ""; }

	public String    getRegion()                              { return _region; }
	public void      setRegion(String region)                 { _region = region != null ? region : ""; }

	public String    getInstanceId()                          { return _instanceId; }
	public void      setInstanceId(String instanceId)         { _instanceId = instanceId != null ? instanceId : ""; }

	public String    getDatabaseVersion()                     { return _databaseVersion; }
	public void      setDatabaseVersion(String version)       { _databaseVersion = version != null ? version : ""; }

	public String    getInstanceTier()                        { return _instanceTier; }
	public void      setInstanceTier(String tier)             { _instanceTier = tier != null ? tier : ""; }

	public String    getInstanceState()                       { return _instanceState; }
	public void      setInstanceState(String state)           { _instanceState = state != null ? state : ""; }

	public double    getCpuUtilizationPct()                   { return _cpuUtilizationPct; }
	public void      setCpuUtilizationPct(double pct)         { _cpuUtilizationPct = Math.max(0.0, Math.min(100.0, pct)); }

	public int       getCpuReservedCores()                    { return _cpuReservedCores; }
	public void      setCpuReservedCores(int cores)           { _cpuReservedCores = Math.max(0, cores); }

	public double    getCpuUsageTimeSecPerS()                 { return _cpuUsageTimeSecPerS; }
	public void      setCpuUsageTimeSecPerS(double val)       { _cpuUsageTimeSecPerS = Math.max(0.0, val); }

	public long      getMemoryQuotaBytes()                    { return _memoryQuotaBytes; }
	public void      setMemoryQuotaBytes(long bytes)          { _memoryQuotaBytes = Math.max(0L, bytes); }

	public long      getMemoryTotalUsageBytes()               { return _memoryTotalUsageBytes; }
	public void      setMemoryTotalUsageBytes(long bytes)     { _memoryTotalUsageBytes = Math.max(0L, bytes); }

	public long      getMemoryUsageBytes()                    { return _memoryUsageBytes; }
	public void      setMemoryUsageBytes(long bytes)          { _memoryUsageBytes = Math.max(0L, bytes); }

	public long      getMemoryCacheBytes()                    { return _memoryCacheBytes; }
	public void      setMemoryCacheBytes(long bytes)          { _memoryCacheBytes = Math.max(0L, bytes); }

	public long      getMemoryFreeBytes()                     { return _memoryFreeBytes; }
	public void      setMemoryFreeBytes(long bytes)           { _memoryFreeBytes = Math.max(0L, bytes); }

	public long      getSwapUsedBytes()                       { return _swapUsedBytes; }
	public void      setSwapUsedBytes(long bytes)             { _swapUsedBytes = Math.max(0L, bytes); }

	public long      getSwapFreeBytes()                       { return _swapFreeBytes; }
	public void      setSwapFreeBytes(long bytes)             { _swapFreeBytes = Math.max(0L, bytes); }

	public double    getDiskReadOpsPerSec()                   { return _diskReadOpsPerSec; }
	public void      setDiskReadOpsPerSec(double ops)         { _diskReadOpsPerSec = Math.max(0.0, ops); }

	public double    getDiskWriteOpsPerSec()                  { return _diskWriteOpsPerSec; }
	public void      setDiskWriteOpsPerSec(double ops)        { _diskWriteOpsPerSec = Math.max(0.0, ops); }

	public double    getDiskReadBytesPerSec()                 { return _diskReadBytesPerSec; }
	public void      setDiskReadBytesPerSec(double bytes)     { _diskReadBytesPerSec = Math.max(0.0, bytes); }

	public double    getDiskWriteBytesPerSec()                { return _diskWriteBytesPerSec; }
	public void      setDiskWriteBytesPerSec(double bytes)    { _diskWriteBytesPerSec = Math.max(0.0, bytes); }

	public long      getDiskReadOpsQuota()                    { return _diskReadOpsQuota; }
	public void      setDiskReadOpsQuota(long quota)          { _diskReadOpsQuota = Math.max(0L, quota); }

	public long      getDiskWriteOpsQuota()                   { return _diskWriteOpsQuota; }
	public void      setDiskWriteOpsQuota(long quota)         { _diskWriteOpsQuota = Math.max(0L, quota); }

	public long      getDiskReadBytesQuota()                  { return _diskReadBytesQuota; }
	public void      setDiskReadBytesQuota(long quota)        { _diskReadBytesQuota = Math.max(0L, quota); }

	public long      getDiskWriteBytesQuota()                 { return _diskWriteBytesQuota; }
	public void      setDiskWriteBytesQuota(long quota)       { _diskWriteBytesQuota = Math.max(0L, quota); }

	public long      getDiskQuotaBytes()                      { return _diskQuotaBytes; }
	public void      setDiskQuotaBytes(long bytes)            { _diskQuotaBytes = Math.max(0L, bytes); }

	public long      getDiskBytesUsed()                       { return _diskBytesUsed; }
	public void      setDiskBytesUsed(long bytes)             { _diskBytesUsed = Math.max(0L, bytes); }

	public long      getDiskDataBytes()                       { return _diskDataBytes; }
	public void      setDiskDataBytes(long bytes)             { _diskDataBytes = Math.max(0L, bytes); }

	public long      getDiskBinlogBytes()                     { return _diskBinlogBytes; }
	public void      setDiskBinlogBytes(long bytes)           { _diskBinlogBytes = Math.max(0L, bytes); }

	public long      getDiskTmpBytes()                        { return _diskTmpBytes; }
	public void      setDiskTmpBytes(long bytes)              { _diskTmpBytes = Math.max(0L, bytes); }

	public long      getDiskOtherBytes()                      { return _diskOtherBytes; }
	public void      setDiskOtherBytes(long bytes)            { _diskOtherBytes = Math.max(0L, bytes); }

	public long      getNetRxBytesTotal()                     { return _netRxBytesTotal; }
	public void      setNetRxBytesTotal(long bytes)           { _netRxBytesTotal = Math.max(0L, bytes); }

	public long      getNetTxBytesTotal()                     { return _netTxBytesTotal; }
	public void      setNetTxBytesTotal(long bytes)           { _netTxBytesTotal = Math.max(0L, bytes); }

	public double    getNetRxBytesPerSec()                    { return _netRxBytesPerSec; }
	public void      setNetRxBytesPerSec(double bytesPerSec)  { _netRxBytesPerSec = Math.max(0.0, bytesPerSec); }

	public double    getNetTxBytesPerSec()                    { return _netTxBytesPerSec; }
	public void      setNetTxBytesPerSec(double bytesPerSec)  { _netTxBytesPerSec = Math.max(0.0, bytesPerSec); }

	public int       getActiveConnections()                   { return _activeConnections; }
	public void      setActiveConnections(int conns)          { _activeConnections = Math.max(0, conns); }

	public long      getUptimeSeconds()                       { return _uptimeSeconds; }
	public void      setUptimeSeconds(long sec)               { _uptimeSeconds = Math.max(0L, sec); }

	/**
	 * Finalize derived fields after raw metrics have been populated.
	 */
	public void computeDerivedFields()
	{
		if (_memoryTotalUsageBytes <= 0 && (_memoryUsageBytes > 0 || _memoryCacheBytes > 0))
		{
			_memoryTotalUsageBytes = _memoryUsageBytes + _memoryCacheBytes;
		}
		if (_memoryFreeBytes <= 0 && _memoryQuotaBytes > _memoryTotalUsageBytes)
		{
			_memoryFreeBytes = _memoryQuotaBytes - _memoryTotalUsageBytes;
		}
		if (_memoryUsageBytes <= 0 && _memoryTotalUsageBytes > _memoryCacheBytes)
		{
			_memoryUsageBytes = _memoryTotalUsageBytes - _memoryCacheBytes;
		}
		if (_diskBytesUsed <= 0 && (_diskDataBytes > 0 || _diskBinlogBytes > 0 || _diskTmpBytes > 0 || _diskOtherBytes > 0))
		{
			_diskBytesUsed = _diskDataBytes + _diskBinlogBytes + _diskTmpBytes + _diskOtherBytes;
		}
		if (_diskOtherBytes <= 0 && _diskBytesUsed > (_diskDataBytes + _diskBinlogBytes + _diskTmpBytes))
		{
			_diskOtherBytes = _diskBytesUsed - (_diskDataBytes + _diskBinlogBytes + _diskTmpBytes);
		}
		if (_cpuReservedCores <= 0 && _cpuUtilizationPct > 0.5 && _cpuUsageTimeSecPerS > 0.0)
		{
			int estimated = (int) Math.round(_cpuUsageTimeSecPerS / (_cpuUtilizationPct / 100.0));
			if (estimated > 0)
				_cpuReservedCores = estimated;
		}
	}
}
