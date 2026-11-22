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

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cloud.CloudConfig;
import com.dbxtune.cloud.CloudLogEntry;
import com.dbxtune.cloud.CloudMetricsSnapshot;
import com.dbxtune.cloud.GcpCredentialsHelper;
import com.dbxtune.cloud.GcpLoggingClient;
import com.dbxtune.cloud.GcpMonitoringClient;
import com.dbxtune.utils.StringUtil;

/**
 * {@link HostMonitorConnectionCloud} implementation for Google Cloud SQL (GCP).
 * <p>
 * Uses {@link GcpMonitoringClient} for infrastructure metrics and {@link GcpLoggingClient}
 * for tailing <code>cloudsql.googleapis.com/postgres.log</code>.
 */
public class HostMonitorConnectionGcp
extends HostMonitorConnectionCloud
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private final GcpCredentialsHelper _credentialsHelper;
	private final GcpMonitoringClient  _monitoringClient;
	private final GcpLoggingClient     _loggingClient;

	public HostMonitorConnectionGcp(CloudConfig config)
	{
		super(config);

		// If projectId was not explicitly provided, attempt to detect it from the Service Account / ADC file or GCE Metadata Server
		if (StringUtil.isNullOrBlank(_config.getProjectId()) && StringUtil.hasValue(_config.getCredentialsFile()))
		{
			String detectedProject = GcpCredentialsHelper.detectProjectIdFromFile(_config.getCredentialsFile());
			if (StringUtil.hasValue(detectedProject))
			{
				_config.setProjectId(detectedProject);
			}
		}
		if (StringUtil.isNullOrBlank(_config.getProjectId()))
		{
			String detectedProject = GcpCredentialsHelper.detectProjectIdFromEnvironment(true);
			if (StringUtil.hasValue(detectedProject))
			{
				_config.setProjectId(detectedProject);
			}
		}

		_credentialsHelper = new GcpCredentialsHelper(_config.getCredentialsFile());
		_monitoringClient  = new GcpMonitoringClient(_config, _credentialsHelper);
		_loggingClient     = new GcpLoggingClient(_config, _credentialsHelper);
	}

	@Override
	public void connect()
	throws Exception
	{
		if (!_config.isValid())
		{
			throw new IllegalArgumentException("Invalid GCP Cloud SQL configuration: both Project ID and Instance ID are required. Current config: " + _config);
		}

		_logger.info("Connecting to GCP Cloud Monitoring & Cloud Logging for instance '" + _config.getGcpDatabaseId() + "'...");
		_lastSnapshot = _monitoringClient.fetchMetricsSnapshot();
		_lastFetchMs  = System.currentTimeMillis();
		_connected    = true;
		_logger.info("Connected to GCP Cloud SQL instance '" + _config.getGcpDatabaseId()
			+ "' (vCPUs=" + _lastSnapshot.getCpuReservedCores()
			+ ", memoryQuotaGB=" + String.format("%.1f", _lastSnapshot.getMemoryQuotaBytes() / (1024.0 * 1024.0 * 1024.0))
			+ ", region=" + _lastSnapshot.getRegion() + ").");
	}

	@Override
	protected CloudMetricsSnapshot fetchMetricsFromProvider()
	throws Exception
	{
		return _monitoringClient.fetchMetricsSnapshot();
	}

	@Override
	public List<CloudLogEntry> fetchNewLogEntries()
	throws Exception
	{
		return _loggingClient.fetchNewLogEntries();
	}

	@Override
	public String testCloudConnection()
	throws Exception
	{
		if (!_config.isValid())
		{
			throw new IllegalArgumentException("Both GCP Project ID and Cloud SQL Instance ID are required.");
		}

		StringBuilder sb = new StringBuilder();
		String monMsg = _monitoringClient.testConnection();
		sb.append(monMsg).append("\n");

		try
		{
			String logMsg = _loggingClient.testConnection();
			sb.append(logMsg);
		}
		catch (Exception logEx)
		{
			sb.append("WARNING: Cloud Logging check failed (roles/logging.viewer may be missing): ").append(logEx.getMessage());
		}

		return sb.toString();
	}

	public GcpMonitoringClient getMonitoringClient() { return _monitoringClient; }
	public GcpLoggingClient    getLoggingClient()    { return _loggingClient; }
}
