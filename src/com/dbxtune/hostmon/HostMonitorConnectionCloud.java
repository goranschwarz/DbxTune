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
package com.dbxtune.hostmon;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cloud.CloudConfig;
import com.dbxtune.cloud.CloudLogEntry;
import com.dbxtune.cloud.CloudMetricsSnapshot;
import com.dbxtune.cloud.CloudProvider;
import com.dbxtune.config.dbms.HostMonOsInfoConfigText;
import com.dbxtune.ssh.SshConnection.LinuxUtilType;
import com.dbxtune.utils.StringUtil;

/**
 * Base {@link HostMonitorConnection} for managed Cloud databases (GCP Cloud SQL, AWS RDS, Azure Database).
 * <p>
 * Maintains a thread-safe cached {@link CloudMetricsSnapshot} (default TTL 30 seconds) so all
 * <code>Monitor*Cloud</code> CounterModels share a single batched API refresh per sample cycle.
 */
public abstract class HostMonitorConnectionCloud
extends HostMonitorConnection
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	protected final CloudConfig          _config;
	protected       boolean              _connected    = false;
	protected       CloudMetricsSnapshot _lastSnapshot = null;
	protected       long                 _lastFetchMs  = 0L;

	public HostMonitorConnectionCloud(CloudConfig config)
	{
		super(ConnectionType.CLOUD);
		_config = config;
	}

	/**
	 * Factory method to instantiate the appropriate provider connection.
	 */
	public static HostMonitorConnectionCloud create(CloudConfig config)
	{
		if (config == null)
			throw new IllegalArgumentException("CloudConfig cannot be null");

		CloudProvider provider = config.getProvider();
		if (provider == CloudProvider.GCP)
		{
			return new HostMonitorConnectionGcp(config);
		}
		throw new UnsupportedOperationException("Cloud provider '" + provider + "' is not yet implemented (Phase 1 supports GCP Cloud SQL).");
	}

	public CloudConfig   getCloudConfig()   { return _config; }
	public CloudProvider getCloudProvider() { return _config.getProvider(); }

	/**
	 * Fetch a fresh metrics snapshot from the Cloud provider API.
	 */
	protected abstract CloudMetricsSnapshot fetchMetricsFromProvider() throws Exception;

	/**
	 * Fetch new error-log entries since the last poll watermark from the Cloud provider API.
	 */
	public abstract List<CloudLogEntry> fetchNewLogEntries() throws Exception;

	/**
	 * Test connectivity to both Cloud Monitoring and Cloud Logging APIs.
	 */
	public abstract String testCloudConnection() throws Exception;

	/**
	 * Get the latest {@link CloudMetricsSnapshot}, refreshing from the Cloud API only if the cache TTL has expired.
	 */
	public synchronized CloudMetricsSnapshot getMetricsSnapshot()
	throws Exception
	{
		long now = System.currentTimeMillis();
		if (_lastSnapshot == null || (now - _lastFetchMs) >= _config.getCacheTtlMs())
		{
			_lastSnapshot = fetchMetricsFromProvider();
			_lastFetchMs  = System.currentTimeMillis();
		}
		return _lastSnapshot;
	}

	@Override
	public String getOsName()
	{
		return "Cloud-" + _config.getProvider().getCode();
	}

	@Override
	public boolean hasVeritas()
	{
		return false;
	}

	@Override
	public int getLinuxUtilVersion(LinuxUtilType utilType)
	{
		return 0;
	}

	@Override
	public String getHostname()
	{
		return _config.getFullResourceName();
	}

	@Override
	public String getUsername()
	{
		return StringUtil.hasValue(_config.getCredentialsFile()) ? _config.getCredentialsFile() : "ADC";
	}

	@Override
	public int getOsCoreCount()
	{
		CloudMetricsSnapshot snap = _lastSnapshot;
		if (snap != null && snap.getCpuReservedCores() > 0)
			return snap.getCpuReservedCores();
		return -1;
	}

	@Override
	public String getOsCharset()
	{
		return "UTF-8";
	}

	@Override
	public boolean isConnected()
	{
		return _connected;
	}

	@Override
	public boolean isConnectionClosed()
	{
		return !_connected;
	}

	@Override
	public void closeConnection()
	{
		_connected = false;
	}

	@Override
	public boolean handleException(Exception ex)
	{
		_logger.warn("Exception in Cloud HostMonitorConnection (" + getHostname() + "): " + ex.getMessage());
		return true;
	}

	@Override
	public ExecutionWrapper executeCommand(String cmd)
	throws Exception
	{
		return executeCommand(cmd, false);
	}

	@Override
	public ExecutionWrapper executeCommand(String cmd, boolean isStreamingCommand)
	throws Exception
	{
		final String output = execCommandOutputAsStr(cmd);
		return new ExecutionWrapper()
		{
			private boolean _closed = false;
			private final InputStream _stdout = new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8));
			private final InputStream _stderr = new ByteArrayInputStream(new byte[0]);

			@Override public void        executeCommand(String c) throws Exception {}
			@Override public void        executeCommand(String c, boolean s) throws Exception {}
			@Override public InputStream getStdout()     { return _stdout; }
			@Override public InputStream getStderr()     { return _stderr; }
			@Override public Integer     getExitStatus() { return 0; }
			@Override public int         waitForData()   { return 0; }
			@Override public boolean     isClosed()      { return _closed; }
			@Override public void        close()         { _closed = true; }
		};
	}

	@Override
	public String execCommandOutputAsStr(String cmd)
	throws Exception
	{
		if ("CLOUD_OS_INFO".equals(cmd))
		{
			return formatCloudOsInfoText();
		}
		return "";
	}

	/**
	 * Format the normalized <code>Key: value</code> lines expected by {@link HostMonOsInfoConfigText#parseKeyValues(String)}
	 * so Daily Summary Reports and the DBMS Config "OS Info" tab display full Cloud SQL instance details.
	 */
	public String formatCloudOsInfoText()
	{
		CloudMetricsSnapshot snap = _lastSnapshot;
		if (snap == null)
		{
			try
			{
				snap = getMetricsSnapshot();
			}
			catch (Exception ex)
			{
				_logger.debug("Could not fetch snapshot for formatCloudOsInfoText(): " + ex.getMessage());
			}
		}

		String providerName = _config.getProvider().getDisplayName();
		String dbVersion    = snap != null && StringUtil.hasValue(snap.getDatabaseVersion()) ? snap.getDatabaseVersion() : "";
		String tier         = snap != null && StringUtil.hasValue(snap.getInstanceTier())    ? snap.getInstanceTier()    : "Managed Cloud Instance";
		String region       = snap != null && StringUtil.hasValue(snap.getRegion())          ? snap.getRegion()          : _config.getRegion();
		int    cores        = snap != null ? snap.getCpuReservedCores() : 0;
		long   memBytes     = snap != null ? snap.getMemoryQuotaBytes() : 0L;
		long   diskBytes    = snap != null ? snap.getDiskQuotaBytes()   : 0L;
		long   uptimeSec    = snap != null ? snap.getUptimeSeconds()    : 0L;

		String memStr = memBytes > 0 ? String.format("%.1f GB", memBytes / (1024.0 * 1024.0 * 1024.0)) : "";
		String bootStr = "";
		if (uptimeSec > 0)
		{
			long bootMs = System.currentTimeMillis() - (uptimeSec * 1000L);
			bootStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Timestamp(bootMs));
		}

		String hardware = tier;
		if (StringUtil.hasValue(region))
			hardware += " (region: " + region + ")";

		StringBuilder sb = new StringBuilder();
		sb.append(HostMonOsInfoConfigText.KEY_OS_NAME       ).append(":        ").append(providerName).append("\n");
		sb.append(HostMonOsInfoConfigText.KEY_OS_VERSION    ).append(":     ")   .append(dbVersion).append("\n");
		sb.append(HostMonOsInfoConfigText.KEY_KERNEL_BUILD  ).append(":   ")     .append(_config.getFullResourceName()).append("\n");
		sb.append(HostMonOsInfoConfigText.KEY_ARCHITECTURE  ).append(":   ")     .append("64-bit (Managed Cloud)").append("\n");
		sb.append(HostMonOsInfoConfigText.KEY_HARDWARE      ).append(":       ") .append(hardware).append("\n");
		sb.append(HostMonOsInfoConfigText.KEY_VIRTUALIZATION).append(": ")       .append(providerName).append(" (").append(_config.getProvider().getCode()).append(")\n");
		sb.append(HostMonOsInfoConfigText.KEY_CPU_MODEL     ).append(":      ")  .append(providerName).append(" vCPU\n");
		sb.append(HostMonOsInfoConfigText.KEY_CPU_SOCKETS   ).append(":    ")    .append(cores > 0 ? "1" : "").append("\n");
		sb.append(HostMonOsInfoConfigText.KEY_CPU_CORES     ).append(":      ")  .append(cores > 0 ? Integer.toString(cores) : "").append("\n");
		sb.append(HostMonOsInfoConfigText.KEY_CPU_THREADS   ).append(":    ")    .append(cores > 0 ? Integer.toString(cores) : "").append("\n");
		sb.append(HostMonOsInfoConfigText.KEY_MEMORY_TOTAL  ).append(":   ")     .append(memStr).append("\n");
		sb.append(HostMonOsInfoConfigText.KEY_LAST_BOOT     ).append(":      ")  .append(bootStr).append("\n");
		sb.append("\n");
		sb.append("----- Cloud Instance Details\n");
		sb.append("Provider:         ").append(providerName).append("\n");
		sb.append("Project ID:       ").append(_config.getProjectId()).append("\n");
		sb.append("Region:           ").append(region).append("\n");
		sb.append("Instance ID:      ").append(_config.getInstanceId()).append("\n");
		sb.append("Instance Tier:    ").append(tier).append("\n");
		sb.append("Database Version: ").append(dbVersion).append("\n");
		if (diskBytes > 0)
			sb.append("Storage Quota:    ").append(String.format("%.1f GB", diskBytes / (1024.0 * 1024.0 * 1024.0))).append("\n");
		return sb.toString();
	}
}
