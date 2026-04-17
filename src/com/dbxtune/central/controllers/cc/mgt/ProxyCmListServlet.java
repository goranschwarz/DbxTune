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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.ICounterController;
import com.dbxtune.cm.CmHighlighterDescriptor;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.CountersModelAppend;
import com.dbxtune.central.controllers.Helper;
import com.dbxtune.central.controllers.cc.ProxyHelper;
import com.dbxtune.utils.HtmlQueryString;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Central-side proxy: GET /api/cc/mgt/cm/list?srv=SRVNAME&amp;time=YYYY-MM-DD HH:mm:ss
 * <p>
 * Forwards the request to the collector's /api/mgt/cm/list endpoint and
 * returns the JSON response to the browser.
 */
public class ProxyCmListServlet
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
			_logger.warn("ProxyCmListServlet.getSrvInfo failed: " + ex.getMessage());
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
			_logger.error("ProxyCmListServlet: Can't find Base URL for server '" + getSrvName() + "'.");
			sendJsonError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
					"collector-offline", "Cannot determine collector URL for server: " + getSrvName());
			return;
		}

		String timeParam = req.getParameter("time");
		if (timeParam == null) timeParam = "";

		HtmlQueryString qs = new HtmlQueryString(collectorBaseUrl + "/api/mgt/cm/list");
		qs.add("time", timeParam);
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
			_logger.warn("ProxyCmListServlet: Collector at " + collectorBaseUrl + " is offline: " + ex.getMessage());
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
		String timeParam = req.getParameter("time");
		if (timeParam == null || timeParam.isEmpty())
			timeParam = new Timestamp(System.currentTimeMillis()).toString();

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

		// ---- Get CM metadata from the live CounterController ----
		if (!CounterController.hasInstance())
		{
			sendJsonError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "no-counter-controller", "LocalMetrics CounterController not available");
			return;
		}

		ICounterController cc = CounterController.getInstance();
		List<CountersModel> cmList = cc.getCmList();
		if (cmList == null || cmList.isEmpty())
		{
			sendJsonError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "no-cms", "No CounterModels registered");
			return;
		}

		// Build ordered CM name list for DB query
		List<String> cmNames = new ArrayList<>();
		for (CountersModel cm : cmList)
			cmNames.add(cm.getName());

		// ---- Get resolved time + row counts from DB ----
		if (!CentralPersistReader.hasInstance())
		{
			sendJsonError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "no-reader", "CentralPersistReader not available");
			return;
		}

		CentralPersistReader.LocalMetricsCmListResult dbResult;
		try
		{
			dbResult = CentralPersistReader.getInstance().getLocalMetricsCmsNearTime(cmNames, requested, 60);
		}
		catch (SQLException ex)
		{
			_logger.error("ProxyCmListServlet.serveLocalMetrics: DB error: " + ex.getMessage(), ex);
			sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "db-error", "Database error reading local metrics");
			return;
		}

		if (dbResult == null)
		{
			sendJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "no-data-in-window", "No local metrics data near requested time");
			return;
		}

		// Build a map of cmName -> CmInfo for quick lookup
		Map<String, CentralPersistReader.LocalMetricsCmInfo> countMap = new LinkedHashMap<>();
		for (CentralPersistReader.LocalMetricsCmInfo info : dbResult.cms)
			countMap.put(info.cmName, info);

		// ---- Group CMs preserving CounterController registration order ----
		Map<String, List<CountersModel>> groups = new LinkedHashMap<>();
		for (CountersModel cm : cmList)
		{
			String groupName = cm.getGroupName();
			if (groupName == null || groupName.isEmpty())
				groupName = "Other";
			groups.computeIfAbsent(groupName, k -> new ArrayList<>()).add(cm);
		}

		SimpleDateFormat tsFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

		// ---- Build response ----
		List<Map<String, Object>> groupList = new ArrayList<>();
		for (Map.Entry<String, List<CountersModel>> entry : groups.entrySet())
		{
			String             groupName = entry.getKey();
			List<CountersModel> groupCms = entry.getValue();

			// Pick group icon from first CM that has one
			String groupIcon = null;
			for (CountersModel cm : groupCms)
			{
				String f = cm.getIconFile();
				if (f != null && !f.trim().isEmpty()) { groupIcon = f.trim(); break; }
			}

			List<Map<String, Object>> cmsList = new ArrayList<>();
			for (CountersModel cm : groupCms)
			{
				String cmName      = cm.getName();
				String displayName = cm.getDisplayName();
				if (displayName == null || displayName.trim().isEmpty())
					displayName = cmName;

				CentralPersistReader.LocalMetricsCmInfo info = countMap.get(cmName);
				int     absRows  = info != null ? info.absRows  : 0;
				int     diffRows = info != null ? info.diffRows : 0;
				int     rateRows = info != null ? info.rateRows : 0;
				boolean hasData  = absRows > 0 || diffRows > 0 || rateRows > 0;

				List<CmHighlighterDescriptor> highlighterDescriptors = cm.getHighlighterDescriptors();
				String description = cm.getDescription();

				Map<String, Object> cmEntry = new LinkedHashMap<>();
				cmEntry.put("cmName",      cmName);
				cmEntry.put("displayName", displayName);
				cmEntry.put("iconFile",    cm.getIconFile());
				cmEntry.put("isActive",    cm.isActive());
				cmEntry.put("isAppend",    cm instanceof CountersModelAppend);
				cmEntry.put("hasData",     hasData);
				cmEntry.put("absRows",     absRows);
				cmEntry.put("diffRows",    diffRows);
				cmEntry.put("rateRows",    rateRows);
				if (description            != null) cmEntry.put("description",            description);
				if (highlighterDescriptors != null) cmEntry.put("highlighterDescriptors", highlighterDescriptors);
				cmsList.add(cmEntry);
			}

			Map<String, Object> groupMap = new LinkedHashMap<>();
			groupMap.put("groupName", groupName);
			groupMap.put("groupIcon", groupIcon);
			groupMap.put("cms",       cmsList);
			groupList.add(groupMap);
		}

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("resolvedTime", tsFmt.format(dbResult.resolvedTime));
		response.put("groups",       groupList);

		resp.setContentType(APPLICATION_JSON);
		resp.setCharacterEncoding("UTF-8");
		om.writeValue(resp.getOutputStream(), response);
	}
}
