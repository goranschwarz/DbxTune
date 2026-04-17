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
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.controllers.Helper;
import com.dbxtune.central.controllers.cc.ProxyHelper;
import com.dbxtune.utils.HtmlQueryString;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Central-side proxy: GET /api/cc/mgt/cm/navSample?srv=SRVNAME&amp;time=...&amp;cm=...&amp;dir=prev|next
 * <p>
 * Forwards the request to the collector's /api/mgt/cm/navSample endpoint and
 * returns the JSON response to the browser.
 */
public class ProxyCmNavSampleServlet
extends ProxyHelper
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		ObjectMapper om = Helper.createObjectMapper();

		try
		{
			getSrvInfo(req);
		}
		catch (IOException ex)
		{
			_logger.warn("ProxyCmNavSampleServlet.getSrvInfo failed: " + ex.getMessage());
			sendJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "srv-not-found", ex.getMessage());
			return;
		}

		if (_isLocalMetrics)
		{
			serveLocalMetrics(req, resp, om);
			return;
		}

		String collectorBaseUrl = getCollectorBaseUrl();
		if (collectorBaseUrl == null)
		{
			_logger.error("ProxyCmNavSampleServlet: Can't find Base URL for server '" + getSrvName() + "'.");
			sendJsonError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
					"collector-offline", "Cannot determine collector URL for server: " + getSrvName());
			return;
		}

		String timeParam = req.getParameter("time") != null ? req.getParameter("time") : "";
		String cmName    = req.getParameter("cm")   != null ? req.getParameter("cm")   : "";
		String dir       = req.getParameter("dir")  != null ? req.getParameter("dir")  : "next";

		HtmlQueryString qs = new HtmlQueryString(collectorBaseUrl + "/api/mgt/cm/navSample");
		qs.add("time", timeParam);
		qs.add("cm",   cmName);
		qs.add("dir",  dir);
		String url = qs.toString();

		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.GET();

		String auth = getMgtAuthentication();
		if (auth != null && !auth.isEmpty())
			requestBuilder.header("Authorization", auth);

		HttpRequest request = requestBuilder.build();

		try
		{
			HttpResponse<byte[]> httpResponse = _httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
			sendResult(httpResponse, resp, APPLICATION_JSON);
		}
		catch (ConnectException ex)
		{
			_logger.warn("ProxyCmNavSampleServlet: Collector at " + collectorBaseUrl + " is offline: " + ex.getMessage());
			sendJsonError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
					"collector-offline", "Collector at " + collectorBaseUrl + " is not reachable");
		}
		catch (InterruptedException ex)
		{
			Thread.currentThread().interrupt();
			throw new IOException("HTTP request was interrupted for URL: " + url, ex);
		}
	}

	// -----------------------------------------------------------------------
	// Local-metrics path (DbxcLocalMetrics / DbxCentral self-monitoring)
	// -----------------------------------------------------------------------

	private void serveLocalMetrics(HttpServletRequest req, HttpServletResponse resp, ObjectMapper om)
	throws IOException
	{
		String timeParam = req.getParameter("time") != null ? req.getParameter("time") : "";
		String cmName    = req.getParameter("cm")   != null ? req.getParameter("cm")   : "";
		String dir       = req.getParameter("dir")  != null ? req.getParameter("dir")  : "next";

		Timestamp requested;
		try
		{
			requested = Timestamp.valueOf(timeParam.trim());
		}
		catch (IllegalArgumentException ex)
		{
			sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "bad-time-param", "Cannot parse time parameter: " + timeParam);
			return;
		}

		if (!CentralPersistReader.hasInstance())
		{
			sendJsonError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "no-reader", "CentralPersistReader not available");
			return;
		}

		Timestamp navTime;
		try
		{
			navTime = CentralPersistReader.getInstance().getLocalMetricsCmNavTime(cmName, requested, dir);
		}
		catch (SQLException ex)
		{
			_logger.error("ProxyCmNavSampleServlet.serveLocalMetrics: DB error for CM '{}': {}", cmName, ex.getMessage(), ex);
			sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "db-error", "Database error reading local metrics");
			return;
		}

		Map<String, Object> response = new LinkedHashMap<>();
		if (navTime != null)
		{
			response.put("found",      true);
			response.put("sampleTime", navTime.toString());
		}
		else
		{
			response.put("found", false);
		}

		resp.setContentType(APPLICATION_JSON);
		resp.setCharacterEncoding("UTF-8");
		om.writeValue(resp.getOutputStream(), response);
	}
}
