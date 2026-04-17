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
 * Central-side proxy for the SQL Server Query Store endpoint on the collector.
 *
 * <pre>GET /api/cc/mgt/query-store?srv=SRVNAME&amp;action=X[&amp;dbname=Y][&amp;queryId=Z]
 *                               [&amp;rankBy=duration][&amp;topN=25][&amp;minExecutions=2]
 *                               [&amp;includePlan=false][&amp;ts=YYYY-MM-DD+HH:mm:ss]</pre>
 *
 * <p>Forwards to the collector's {@code /api/mgt/query-store} endpoint, which reads
 * the {@code qs:<DatabaseName>} H2 schemas populated by {@code SqlServerQueryStoreExtractor}
 * during the nightly database rollover.
 *
 * <p>Actions:
 * <ul>
 *   <li>{@code listDatabases} – list all databases with Query Store data + health metrics</li>
 *   <li>{@code topQueries}    – top N queries ranked by duration/cpu/reads/executions/memory</li>
 *   <li>{@code queryDetail}   – full detail for a specific query_id: plans, wait stats, timeline, regressions</li>
 * </ul>
 */
public class ProxyQueryStoreServlet
extends ProxyHelper
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/** Shared forwarding logic: resolves collector URL, builds query string, sends request. */
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
			_logger.warn("ProxyQueryStoreServlet.getSrvInfo failed: " + ex.getMessage());
			sendJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "srv-not-found", ex.getMessage());
			return;
		}

		String collectorBaseUrl = getCollectorBaseUrl();
		if (collectorBaseUrl == null)
		{
			_logger.error("ProxyQueryStoreServlet: Can't find Base URL for server '" + getSrvName() + "'.");
			sendJsonError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
					"collector-offline", "Cannot determine collector URL for server: " + getSrvName());
			return;
		}

		// Mandatory action parameter
		String action = Helper.getParameter(req, "action", "");
		if (action.isEmpty())
		{
			sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST,
					"bad-param", "Parameter 'action' is required: listDatabases | topQueries | queryDetail | lastActualPlan | extractStatus | extract");
			return;
		}

		// Forward all supported parameters
		String dbname        = Helper.getParameter(req, "dbname",        "");
		String queryId       = Helper.getParameter(req, "queryId",       "");
		String planId        = Helper.getParameter(req, "planId",        "");
		String rankBy        = Helper.getParameter(req, "rankBy",        "");
		String topN          = Helper.getParameter(req, "topN",          "");
		String minExecutions = Helper.getParameter(req, "minExecutions", "");
		String includePlan   = Helper.getParameter(req, "includePlan",   "");
		String ts            = Helper.getParameter(req, "ts",            "");
		String type          = Helper.getParameter(req, "type",          "");
		String tables        = Helper.getParameter(req, "tables",        "");

		HtmlQueryString qs = new HtmlQueryString(collectorBaseUrl + "/api/mgt/query-store");
		qs.add          ("action",        action);
		qs.addIfNotEmpty("dbname",        dbname);
		qs.addIfNotEmpty("queryId",       queryId);
		qs.addIfNotEmpty("planId",        planId);
		qs.addIfNotEmpty("rankBy",        rankBy);
		qs.addIfNotEmpty("topN",          topN);
		qs.addIfNotEmpty("minExecutions", minExecutions);
		qs.addIfNotEmpty("includePlan",   includePlan);
		qs.addIfNotEmpty("ts",            ts);
		qs.addIfNotEmpty("type",          type);
		qs.addIfNotEmpty("tables",        tables);

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
			_logger.warn("ProxyQueryStoreServlet: Collector at " + collectorBaseUrl + " is offline: " + ex.getMessage());
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
