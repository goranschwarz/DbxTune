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
package com.dbxtune.central.controllers.cc.mgt;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.controllers.Helper;
import com.dbxtune.central.controllers.cc.ProxyHelper;
import com.dbxtune.utils.HtmlQueryString;

/**
 * Central-side proxy for the SQL Server Agent job-scheduler endpoint on the collector.
 *
 * <pre>GET  /api/cc/mgt/job-scheduler?srv=SRVNAME&amp;action=overview|jobs|history|errors|outliers|extractStatus
 *          [&amp;jobName=PATTERN][&amp;status=succeeded|failed|...][&amp;limit=200][&amp;ts=YYYY-MM-DD+HH:mm:ss]
 * POST /api/cc/mgt/job-scheduler?srv=SRVNAME&amp;action=extract[&amp;type=JOB_SCHEDULER]</pre>
 *
 * <p>Forwards to the collector's {@code /api/mgt/job-scheduler} endpoint which queries
 * the {@code job_scheduler} H2 schema populated by the nightly job-scheduler extractor.
 *
 * <p>Actions:
 * <ul>
 *   <li>{@code overview}      – aggregated per-job statistics from job_history_overview_all</li>
 *   <li>{@code jobs}          – list of SQL Server Agent jobs from sysjobs</li>
 *   <li>{@code history}       – individual run history with optional jobName and status filters</li>
 *   <li>{@code errors}        – failed runs from job_history_errors view</li>
 *   <li>{@code outliers}      – unusually long runs from job_history_outliers view</li>
 *   <li>{@code extractStatus} – current extraction progress/state (GET)</li>
 *   <li>{@code extract}       – trigger on-demand extraction (POST)</li>
 * </ul>
 */
public class ProxyJobSchedulerServlet
extends ProxyHelper
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/** Shared forwarding logic for both GET and POST. */
	private void forwardToCollector(HttpServletRequest req, HttpServletResponse resp,
			boolean usePost)
	throws ServletException, IOException
	{
		try
		{
			getSrvInfo(req);
		}
		catch (IOException ex)
		{
			_logger.warn("ProxyJobSchedulerServlet.getSrvInfo failed: " + ex.getMessage());
			sendJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "srv-not-found", ex.getMessage());
			return;
		}

		String collectorBaseUrl = getCollectorBaseUrl();
		if (collectorBaseUrl == null)
		{
			_logger.error("ProxyJobSchedulerServlet: Can't find Base URL for server '" + getSrvName() + "'.");
			sendJsonError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
					"collector-offline", "Cannot determine collector URL for server: " + getSrvName());
			return;
		}

		String action = Helper.getParameter(req, "action", "");
		if (action.isEmpty())
		{
			sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST,
					"bad-param", "Parameter 'action' is required: overview | jobs | history | errors | outliers | extractStatus | extract");
			return;
		}

		String jobName = Helper.getParameter(req, "jobName", "");
		String status  = Helper.getParameter(req, "status",  "");
		String limit   = Helper.getParameter(req, "limit",   "");
		String ts      = Helper.getParameter(req, "ts",      "");
		String type    = Helper.getParameter(req, "type",    "");

		HtmlQueryString qs = new HtmlQueryString(collectorBaseUrl + "/api/mgt/job-scheduler");
		qs.add          ("action",  action);
		qs.addIfNotEmpty("jobName", jobName);
		qs.addIfNotEmpty("status",  status);
		qs.addIfNotEmpty("limit",   limit);
		qs.addIfNotEmpty("ts",      ts);
		qs.addIfNotEmpty("type",    type);

		String url = qs.toString();

		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(url));
		if (usePost)
			requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
		else
			requestBuilder.GET();

		String auth = getMgtAuthentication();
		if (auth != null && !auth.isEmpty())
			requestBuilder.header("Authorization", auth);

		try
		{
			HttpResponse<byte[]> httpResponse = _httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
			sendResult(httpResponse, resp, APPLICATION_JSON);
		}
		catch (ConnectException ex)
		{
			_logger.warn("ProxyJobSchedulerServlet: Collector at " + collectorBaseUrl + " is offline: " + ex.getMessage());
			sendJsonError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
					"collector-offline", "Collector at " + collectorBaseUrl + " is not reachable");
		}
		catch (InterruptedException ex)
		{
			Thread.currentThread().interrupt();
			throw new IOException("HTTP request was interrupted for URL: " + url, ex);
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		forwardToCollector(req, resp, false);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		forwardToCollector(req, resp, true);
	}
}
