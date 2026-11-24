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

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * REST client for Google Cloud Monitoring v3 (<code>monitoring.googleapis.com</code>)
 * and optional Cloud SQL Admin metadata (<code>sqladmin.googleapis.com</code>).
 * <p>
 * Uses Java 11+ {@link HttpClient} with HTTP/2 multiplexing so all Cloud SQL metric series
 * are fetched concurrently over a single connection in one poll cycle.
 */
public class GcpMonitoringClient
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final String MONITORING_BASE_URL = "https://monitoring.googleapis.com/v3/projects/";
	private static final String SQLADMIN_BASE_URL   = "https://sqladmin.googleapis.com/sql/v1beta4/projects/";

	/** Cloud SQL metric types queried in a snapshot refresh */
	public static final String[] CLOUDSQL_METRICS = new String[] {
		"cloudsql.googleapis.com/database/cpu/utilization",
		"cloudsql.googleapis.com/database/cpu/reserved_cores",
		"cloudsql.googleapis.com/database/cpu/usage_time",
		"cloudsql.googleapis.com/database/memory/quota",
		"cloudsql.googleapis.com/database/memory/total_usage",
		"cloudsql.googleapis.com/database/memory/components",
		"cloudsql.googleapis.com/database/memory/swap_utilization",
		"cloudsql.googleapis.com/database/disk/read_ops_count",
		"cloudsql.googleapis.com/database/disk/write_ops_count",
		"cloudsql.googleapis.com/database/disk/read_ops_quota",
		"cloudsql.googleapis.com/database/disk/write_ops_quota",
		"cloudsql.googleapis.com/database/disk/read_bytes_count",
		"cloudsql.googleapis.com/database/disk/write_bytes_count",
		"cloudsql.googleapis.com/database/disk/read_bytes_quota",
		"cloudsql.googleapis.com/database/disk/write_bytes_quota",
		"cloudsql.googleapis.com/database/disk/quota",
		"cloudsql.googleapis.com/database/disk/bytes_used",
		"cloudsql.googleapis.com/database/disk/bytes_used_by_data_type",
		"cloudsql.googleapis.com/database/network/received_bytes_count",
		"cloudsql.googleapis.com/database/network/sent_bytes_count",
		"cloudsql.googleapis.com/database/network/connections",
		"cloudsql.googleapis.com/database/state"
		// Not 'database/uptime': it is a DELTA metric (seconds up per minute). The uptime is from the DBMS start time (see fetchMetricsSnapshot)
	};

	private final CloudConfig          _config;
	private final GcpCredentialsHelper _credentialsHelper;
	private final HttpClient           _httpClient;

	// Cached instance metadata from Cloud SQL Admin API (queried once if permitted)
	private boolean _sqlAdminChecked     = false;
	private String  _cachedTier          = "";
	private String  _cachedDbVersion     = "";
	private String  _cachedRegion        = "";

	// true when the API refused one call for all metrics (HTTP 400/404): then one call per metric
	private boolean _useOneCallPerMetric = false;

	// Synthetic monotonic counters for network bytes so DIFF/RATE CounterModels work naturally
	private long    _lastNetUpdateMs     = 0L;
	private long    _cumulativeRxBytes   = 0L;
	private long    _cumulativeTxBytes   = 0L;

	public GcpMonitoringClient(CloudConfig config, GcpCredentialsHelper credentialsHelper)
	{
		_config            = config;
		_credentialsHelper = credentialsHelper;
		_httpClient        = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();
	}

	/**
	 * Verify connectivity and permissions to Cloud Monitoring for the configured project &amp; instance.
	 * @return A human-readable status summary on success
	 * @throws IOException on authentication, permission, or network error
	 */
	public String testConnection()
	throws IOException
	{
		CloudMetricsSnapshot snap = fetchMetricsSnapshot();
		StringBuilder sb = new StringBuilder();
		sb.append("Connected to GCP Cloud Monitoring for '").append(_config.getGcpDatabaseId()).append("'.\n");
		if (StringUtil.hasValue(snap.getRegion()))
			sb.append("  Region:           ").append(snap.getRegion()).append("\n");
		if (StringUtil.hasValue(snap.getInstanceTier()))
			sb.append("  Instance Tier:    ").append(snap.getInstanceTier()).append("\n");
		if (StringUtil.hasValue(snap.getDatabaseVersion()))
			sb.append("  Database Version: ").append(snap.getDatabaseVersion()).append("\n");
		sb.append("  Instance State:   ").append(snap.getInstanceState()).append("\n");
		sb.append("  Reserved vCPUs:   ").append(snap.getCpuReservedCores()).append("\n");
		sb.append("  CPU Utilization:  ").append(String.format("%.1f%%", snap.getCpuUtilizationPct())).append("\n");
		sb.append("  Memory Quota:     ").append(String.format("%.2f GB", snap.getMemoryQuotaBytes() / (1024.0 * 1024.0 * 1024.0))).append("\n");
		sb.append("  Storage Quota:    ").append(String.format("%.2f GB", snap.getDiskQuotaBytes() / (1024.0 * 1024.0 * 1024.0))).append("\n");
		return sb.toString();
	}

	/**
	 * Fetch a complete {@link CloudMetricsSnapshot} for the configured Cloud SQL instance.
	 */
	public CloudMetricsSnapshot fetchMetricsSnapshot()
	throws IOException
	{
		String token = _credentialsHelper.getAccessToken();

		if (!_sqlAdminChecked)
		{
			_sqlAdminChecked = true;
			fetchInstanceMetadataOptional(token);
		}

		Instant now   = Instant.now();
		// Look back 5 minutes because Cloud SQL metrics are sampled every 60s with a ~60-120s ingestion delay
		Instant start = now.minusSeconds(300);

		String startTimeIso = start.toString();
		String endTimeIso   = now.toString();
		String databaseId   = _config.getGcpDatabaseId();
		String projectId    = _config.getProjectId();

		// metricType -> timeSeries.list JSON body. One call for all metrics (each API call is counted, and billed above the free allowance),
		// or one call per metric if the API refuses the combined filter
		Map<String, String> bodies = null;
		if ( ! _useOneCallPerMetric )
			bodies = fetchAllMetricsInOneCall(token, projectId, databaseId, startTimeIso, endTimeIso);
		if (bodies == null)
			bodies = fetchOneCallPerMetric(token, projectId, databaseId, startTimeIso, endTimeIso);

		CloudMetricsSnapshot snap = new CloudMetricsSnapshot();
		long fetchMs = System.currentTimeMillis();
		snap.setFetchTimeMs(fetchMs);
		snap.setProviderName(_config.getProvider().getDisplayName());
		snap.setProjectId(projectId);
		snap.setInstanceId(_config.getInstanceId());
		snap.setRegion(StringUtil.hasValue(_cachedRegion) ? _cachedRegion : _config.getRegion());
		snap.setInstanceTier(_cachedTier);
		snap.setDatabaseVersion(_cachedDbVersion);

		Instant latestPointInstant = null;
		for (Map.Entry<String, String> entry : bodies.entrySet())
		{
			Instant pointTime = parseMetricResponse(entry.getKey(), entry.getValue(), snap);
			if (pointTime != null && (latestPointInstant == null || pointTime.isAfter(latestPointInstant)))
				latestPointInstant = pointTime;
		}

		if (latestPointInstant != null)
		{
			snap.setMetricTimestamp(Timestamp.from(latestPointInstant));
		}

		// Update synthetic monotonic network and uptime counters
		long elapsedMs = _lastNetUpdateMs > 0 ? Math.max(1000L, fetchMs - _lastNetUpdateMs) : 10000L;
		_lastNetUpdateMs = fetchMs;
		double elapsedSec = elapsedMs / 1000.0;

		_cumulativeRxBytes += Math.round(snap.getNetRxBytesPerSec() * elapsedSec);
		_cumulativeTxBytes += Math.round(snap.getNetTxBytesPerSec() * elapsedSec);
		snap.setNetRxBytesTotal(_cumulativeRxBytes);
		snap.setNetTxBytesTotal(_cumulativeTxBytes);

		// Uptime: 'database/uptime' is a DELTA metric (about 60 seconds of uptime per 1-minute point), not the time since the start.
		// Summing it up only gives the time since the collector started, and a restart is never seen.
		// The database knows when it was started: CmSummary (pg_postmaster_start_time()), which is refreshed first in every sample. 0 = not known yet.
		Timestamp dbmsStartTime = null;
		if (CounterController.hasInstance() && CounterController.getInstance().getSummaryCm() != null)
			dbmsStartTime = CounterController.getInstance().getSummaryCm().getDbmsStartTime();
		snap.setUptimeSeconds(dbmsStartTime == null ? 0L : (fetchMs - dbmsStartTime.getTime()) / 1000L);

		snap.computeDerivedFields();
		return snap;
	}

	/**
	 * All metrics in ONE <code>timeSeries.list</code> call (<code>metric.type = one_of(...)</code>), following <code>nextPageToken</code>.<br>
	 * The time series are split per metric type into a body each, so {@link #parseMetricResponse} is the same as for one call per metric.
	 *
	 * @return metricType -&gt; JSON body, or <code>null</code> if the API refused the combined filter (HTTP 400/404): then call per metric
	 */
	private Map<String, String> fetchAllMetricsInOneCall(String token, String projectId, String databaseId, String startTimeIso, String endTimeIso)
	throws IOException
	{
		StringBuilder oneOf = new StringBuilder();
		for (String metricType : CLOUDSQL_METRICS)
			oneOf.append(oneOf.length() == 0 ? "" : ", ").append("\"").append(metricType).append("\"");

		String filter = "resource.type = \"cloudsql_database\""
			+ " AND resource.labels.database_id = \"" + databaseId + "\""
			+ " AND metric.type = one_of(" + oneOf + ")";

		Map<String, ArrayNode> seriesByType = new LinkedHashMap<>();
		String pageToken = null;
		int    pages     = 0;
		do
		{
			String url = MONITORING_BASE_URL + encode(projectId) + "/timeSeries"
				+ "?filter=" + encode(filter)
				+ "&interval.startTime=" + encode(startTimeIso)
				+ "&interval.endTime=" + encode(endTimeIso)
				+ "&pageSize=1000"
				+ (StringUtil.hasValue(pageToken) ? "&pageToken=" + encode(pageToken) : "");

			HttpResponse<String> resp = send(url, token);
			if (resp.statusCode() == 400 || resp.statusCode() == 404)
			{
				// For example a metric that does not exist on this edition. Remember it: no need to try again every sample
				_useOneCallPerMetric = true;
				String apiMsg = "";
				try { apiMsg = MAPPER.readTree(resp.body()).path("error").path("message").asText(""); } catch (Exception ignore) {}
				_logger.info("GCP Cloud Monitoring: fetching all " + CLOUDSQL_METRICS.length + " metrics in ONE call is not accepted by this API/instance (HTTP " + resp.statusCode() + ": " + (StringUtil.hasValue(apiMsg) ? apiMsg : resp.body()) + "). "
						+ "This is NOT a problem: falling back to one call per metric (sent in parallel), which is used from now on for this collector. The metrics are collected as usual.");
				return null;
			}
			if (resp.statusCode() != 200)
				throw new IOException("GCP Cloud Monitoring API failed: HTTP " + resp.statusCode() + ": " + resp.body());

			JsonNode root = MAPPER.readTree(resp.body());
			for (JsonNode ts : root.path("timeSeries"))
			{
				String type = ts.path("metric").path("type").asText("");
				seriesByType.computeIfAbsent(type, k -> MAPPER.createArrayNode()).add(ts);
			}
			pageToken = root.path("nextPageToken").asText("");
			pages++;
		}
		while (StringUtil.hasValue(pageToken) && pages < 20);

		Map<String, String> bodies = new LinkedHashMap<>();
		for (Map.Entry<String, ArrayNode> e : seriesByType.entrySet())
		{
			ObjectNode body = MAPPER.createObjectNode();
			body.set("timeSeries", e.getValue());
			bodies.put(e.getKey(), MAPPER.writeValueAsString(body));
		}
		return bodies;
	}

	/**
	 * One <code>timeSeries.list</code> call per metric (in parallel). A metric that does not exist on all tiers/editions (HTTP 400/404) is skipped.
	 * @return metricType -&gt; JSON body
	 */
	private Map<String, String> fetchOneCallPerMetric(String token, String projectId, String databaseId, String startTimeIso, String endTimeIso)
	throws IOException
	{
		Map<String, CompletableFuture<HttpResponse<String>>> futures = new LinkedHashMap<>();
		for (String metricType : CLOUDSQL_METRICS)
		{
			String filter = "resource.type = \"cloudsql_database\""
				+ " AND resource.labels.database_id = \"" + databaseId + "\""
				+ " AND metric.type = \"" + metricType + "\"";

			String url = MONITORING_BASE_URL + encode(projectId) + "/timeSeries"
				+ "?filter=" + encode(filter)
				+ "&interval.startTime=" + encode(startTimeIso)
				+ "&interval.endTime=" + encode(endTimeIso)
				+ "&pageSize=10";

			futures.put(metricType, _httpClient.sendAsync(newGet(url, token), HttpResponse.BodyHandlers.ofString()));
		}

		// Wait for all multiplexed HTTP/2 requests
		try
		{
			CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0])).join();
		}
		catch (Exception ex)
		{
			throw new IOException("Failed fetching GCP Cloud Monitoring metrics for '" + databaseId + "': " + ex.getMessage(), ex);
		}

		Map<String, String> bodies = new LinkedHashMap<>();
		String firstError = null;
		for (Map.Entry<String, CompletableFuture<HttpResponse<String>>> entry : futures.entrySet())
		{
			String metricType = entry.getKey();
			HttpResponse<String> resp = entry.getValue().join();
			int status = resp.statusCode();

			if (status == 200)
				bodies.put(metricType, resp.body());
			else if (status == 404 || status == 400)
			{
				// Some metrics (e.g. disk/read_bytes_count or swap_utilization) may not exist on all tiers/editions
				if (_logger.isDebugEnabled())
					_logger.debug("Metric '" + metricType + "' returned HTTP " + status + " (ignored).");
			}
			else if (firstError == null)
				firstError = "HTTP " + status + " for " + metricType + ": " + resp.body();
		}

		if (bodies.isEmpty() && firstError != null)
			throw new IOException("GCP Cloud Monitoring API failed: " + firstError);

		return bodies;
	}

	private HttpRequest newGet(String url, String token)
	{
		return HttpRequest.newBuilder()
			.uri(URI.create(url))
			.timeout(Duration.ofSeconds(15))
			.header("Authorization", "Bearer " + token)
			.header("Accept", "application/json")
			.GET()
			.build();
	}

	private HttpResponse<String> send(String url, String token)
	throws IOException
	{
		try
		{
			return _httpClient.send(newGet(url, token), HttpResponse.BodyHandlers.ofString());
		}
		catch (InterruptedException ex)
		{
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while fetching GCP Cloud Monitoring metrics", ex);
		}
	}

	/**
	 * Parse a single <code>timeSeries.list</code> JSON response into {@link CloudMetricsSnapshot}.
	 * Package-private for unit testing.
	 */
	Instant parseMetricResponse(String metricType, String jsonBody, CloudMetricsSnapshot snap)
	{
		if (StringUtil.isNullOrBlank(jsonBody))
			return null;

		try
		{
			JsonNode root = MAPPER.readTree(jsonBody);
			JsonNode timeSeriesArr = root.path("timeSeries");
			if (!timeSeriesArr.isArray() || timeSeriesArr.isEmpty())
				return null;

			Instant latestTime = null;

			for (JsonNode ts : timeSeriesArr)
			{
				// Extract region from resource.labels.region if not yet set
				String region = ts.path("resource").path("labels").path("region").asText("");
				if (StringUtil.hasValue(region) && StringUtil.isNullOrBlank(snap.getRegion()))
				{
					snap.setRegion(region);
					_cachedRegion = region;
				}

				String valueKind = ts.path("metricKind").asText("GAUGE");
				JsonNode metricLabels = ts.path("metric").path("labels");
				JsonNode points = ts.path("points");
				if (!points.isArray() || points.isEmpty())
					continue;

				// Points are ordered newest-first (index 0 is most recent)
				JsonNode latestPoint = points.get(0);
				JsonNode interval    = latestPoint.path("interval");
				String   startStr    = interval.path("startTime").asText("");
				String   endStr      = interval.path("endTime").asText("");

				Instant endInstant = StringUtil.hasValue(endStr) ? Instant.parse(endStr) : null;
				if (endInstant != null && (latestTime == null || endInstant.isAfter(latestTime)))
				{
					latestTime = endInstant;
				}

				double periodSec = 60.0;
				if (StringUtil.hasValue(startStr) && StringUtil.hasValue(endStr))
				{
					long diffSec = Duration.between(Instant.parse(startStr), endInstant).getSeconds();
					if (diffSec > 0)
						periodSec = diffSec;
				}

				JsonNode valNode = latestPoint.path("value");
				double doubleVal = extractDoubleValue(valNode);
				long   longVal   = extractLongValue(valNode);

				double ratePerSec = "DELTA".equalsIgnoreCase(valueKind) ? (doubleVal / periodSec) : doubleVal;

				switch (metricType)
				{
					case "cloudsql.googleapis.com/database/cpu/utilization":
						// GCP returns 0.0 - 1.0 fraction
						snap.setCpuUtilizationPct(doubleVal <= 1.5 ? doubleVal * 100.0 : doubleVal);
						break;

					case "cloudsql.googleapis.com/database/cpu/reserved_cores":
						snap.setCpuReservedCores((int) Math.round(doubleVal));
						break;

					case "cloudsql.googleapis.com/database/cpu/usage_time":
						snap.setCpuUsageTimeSecPerS(ratePerSec);
						break;

					case "cloudsql.googleapis.com/database/memory/quota":
						snap.setMemoryQuotaBytes(longVal);
						break;

					case "cloudsql.googleapis.com/database/memory/total_usage":
						snap.setMemoryTotalUsageBytes(longVal);
						break;

					case "cloudsql.googleapis.com/database/memory/components":
					{
						String comp = metricLabels.path("component").asText("");
						// Note: memory/components reports percentage of memory quota (0-100) or bytes depending on metric version;
						// if <= 100 and quota > 0, convert % to bytes, else treat as bytes
						long bytes = (doubleVal <= 100.0 && snap.getMemoryQuotaBytes() > 0)
							? Math.round((doubleVal / 100.0) * snap.getMemoryQuotaBytes())
							: longVal;
						if ("Usage".equalsIgnoreCase(comp))
							snap.setMemoryUsageBytes(bytes);
						else if ("Cache".equalsIgnoreCase(comp))
							snap.setMemoryCacheBytes(bytes);
						else if ("Free".equalsIgnoreCase(comp))
							snap.setMemoryFreeBytes(bytes);
						break;
					}

					case "cloudsql.googleapis.com/database/memory/swap_utilization":
						snap.setSwapUsedBytes(longVal);
						break;

					case "cloudsql.googleapis.com/database/disk/read_ops_count":
						snap.setDiskReadOpsPerSec(ratePerSec);
						break;

					case "cloudsql.googleapis.com/database/disk/write_ops_count":
						snap.setDiskWriteOpsPerSec(ratePerSec);
						break;

					case "cloudsql.googleapis.com/database/disk/read_ops_quota":
						snap.setDiskReadOpsQuota(longVal);
						break;

					case "cloudsql.googleapis.com/database/disk/write_ops_quota":
						snap.setDiskWriteOpsQuota(longVal);
						break;

					case "cloudsql.googleapis.com/database/disk/read_bytes_count":
						snap.setDiskReadBytesPerSec(ratePerSec);
						break;

					case "cloudsql.googleapis.com/database/disk/write_bytes_count":
						snap.setDiskWriteBytesPerSec(ratePerSec);
						break;

					case "cloudsql.googleapis.com/database/disk/read_bytes_quota":
						snap.setDiskReadBytesQuota(longVal);
						break;

					case "cloudsql.googleapis.com/database/disk/write_bytes_quota":
						snap.setDiskWriteBytesQuota(longVal);
						break;

					case "cloudsql.googleapis.com/database/disk/quota":
						snap.setDiskQuotaBytes(longVal);
						break;

					case "cloudsql.googleapis.com/database/disk/bytes_used":
						snap.setDiskBytesUsed(longVal);
						break;

					case "cloudsql.googleapis.com/database/disk/bytes_used_by_data_type":
					{
						String dataType = metricLabels.path("data_type").asText("");
						if ("data".equalsIgnoreCase(dataType))
							snap.setDiskDataBytes(longVal);
						else if ("binlog".equalsIgnoreCase(dataType) || "wal".equalsIgnoreCase(dataType))
							snap.setDiskBinlogBytes(longVal);
						else if ("tmp_data".equalsIgnoreCase(dataType) || "tmp".equalsIgnoreCase(dataType))
							snap.setDiskTmpBytes(longVal);
						else if ("other".equalsIgnoreCase(dataType))
							snap.setDiskOtherBytes(longVal);
						break;
					}

					case "cloudsql.googleapis.com/database/network/received_bytes_count":
						snap.setNetRxBytesPerSec(ratePerSec);
						break;

					case "cloudsql.googleapis.com/database/network/sent_bytes_count":
						snap.setNetTxBytesPerSec(ratePerSec);
						break;

					case "cloudsql.googleapis.com/database/network/connections":
						snap.setActiveConnections((int) longVal);
						break;

					case "cloudsql.googleapis.com/database/state":
					{
						String strVal = valNode.path("stringValue").asText("");
						if (StringUtil.hasValue(strVal))
							snap.setInstanceState(strVal);
						break;
					}
				}
			}

			return latestTime;
		}
		catch (Exception ex)
		{
			_logger.debug("Failed parsing metric response for '" + metricType + "': " + ex.getMessage());
			return null;
		}
	}

	/**
	 * Optionally query Cloud SQL Admin API for instance tier, databaseVersion, and region.
	 * Silently ignored if the service account only has monitoring.viewer / logging.viewer roles.
	 */
	private void fetchInstanceMetadataOptional(String token)
	{
		try
		{
			String url = SQLADMIN_BASE_URL + encode(_config.getProjectId())
				+ "/instances/" + encode(_config.getInstanceId());

			HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.timeout(Duration.ofSeconds(8))
				.header("Authorization", "Bearer " + token)
				.header("Accept", "application/json")
				.GET()
				.build();

			HttpResponse<String> resp = _httpClient.send(req, HttpResponse.BodyHandlers.ofString());
			if (resp.statusCode() == 200)
			{
				JsonNode root = MAPPER.readTree(resp.body());
				_cachedDbVersion = root.path("databaseVersion").asText("");
				_cachedRegion    = root.path("region").asText("");
				String tier      = root.path("settings").path("tier").asText("");
				String edition   = root.path("settings").path("edition").asText("");
				if (StringUtil.hasValue(tier))
				{
					_cachedTier = StringUtil.hasValue(edition) ? (tier + " (" + edition + ")") : tier;
				}
			}
		}
		catch (Exception ex)
		{
			if (_logger.isDebugEnabled())
				_logger.debug("Optional Cloud SQL Admin metadata lookup skipped: " + ex.getMessage());
		}
	}

	private static double extractDoubleValue(JsonNode valNode)
	{
		if (valNode == null || valNode.isMissingNode())
			return 0.0;
		if (valNode.has("doubleValue"))
			return valNode.get("doubleValue").asDouble(0.0);
		if (valNode.has("int64Value"))
			return Double.parseDouble(valNode.get("int64Value").asText("0"));
		return 0.0;
	}

	private static long extractLongValue(JsonNode valNode)
	{
		if (valNode == null || valNode.isMissingNode())
			return 0L;
		if (valNode.has("int64Value"))
			return Long.parseLong(valNode.get("int64Value").asText("0"));
		if (valNode.has("doubleValue"))
			return Math.round(valNode.get("doubleValue").asDouble(0.0));
		return 0L;
	}

	private static String encode(String s)
	{
		return URLEncoder.encode(s != null ? s : "", StandardCharsets.UTF_8);
	}
}
