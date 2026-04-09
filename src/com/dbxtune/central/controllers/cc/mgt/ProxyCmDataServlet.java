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
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.ICounterController;
import com.dbxtune.cm.CmChartDescriptor;
import com.dbxtune.cm.CmHighlighterDescriptor;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.central.controllers.cc.ProxyHelper;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.gui.swing.ColumnHeaderPropsEntry;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Central-side proxy: GET /api/cc/mgt/cm/data?srv=SRVNAME&cm=CmName&time=YYYY-MM-DD HH:mm:ss&type=abs|diff|rate
 * <p>
 * Forwards the request to the collector's /api/mgt/cm/data endpoint and
 * returns the JSON response to the browser.
 */
public class ProxyCmDataServlet
extends ProxyHelper
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		try
		{
			getSrvInfo(req);
		}
		catch (IOException ex)
		{
			_logger.warn("ProxyCmDataServlet.getSrvInfo failed: " + ex.getMessage());
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			resp.setContentType(APPLICATION_JSON);
			resp.setCharacterEncoding("UTF-8");
			resp.getOutputStream().println("{\"error\":\"srv-not-found\",\"message\":" + jsonStr(ex.getMessage()) + "}");
			return;
		}

		if (_isLocalMetrics)
		{
			serveLocalMetrics(req, resp);
			return;
		}

		String collectorBaseUrl = getCollectorBaseUrl();
		if (collectorBaseUrl == null)
		{
			_logger.error("ProxyCmDataServlet: Can't find Base URL for server '" + getSrvName() + "'.");
			resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			resp.setContentType(APPLICATION_JSON);
			resp.setCharacterEncoding("UTF-8");
			resp.getOutputStream().println("{\"error\":\"collector-offline\",\"message\":\"Cannot determine collector URL for server: " + getSrvName() + "\"}");
			return;
		}

		String cmName     = req.getParameter("cm")      != null ? req.getParameter("cm")      : "";
		String timeParam  = req.getParameter("time")    != null ? req.getParameter("time")    : "";
		String typeParam  = req.getParameter("type")    != null ? req.getParameter("type")    : "abs";
		String showAllParam = req.getParameter("showAll") != null ? req.getParameter("showAll") : "false";

		String url = collectorBaseUrl + "/api/mgt/cm/data"
				+ "?cm="      + URLEncoder.encode(cmName,       StandardCharsets.UTF_8)
				+ "&time="    + URLEncoder.encode(timeParam,    StandardCharsets.UTF_8)
				+ "&type="    + URLEncoder.encode(typeParam,    StandardCharsets.UTF_8)
				+ "&showAll=" + URLEncoder.encode(showAllParam, StandardCharsets.UTF_8);

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
			_logger.warn("ProxyCmDataServlet: Collector at " + collectorBaseUrl + " is offline: " + ex.getMessage());
			resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			resp.setContentType(APPLICATION_JSON);
			resp.setCharacterEncoding("UTF-8");
			resp.getOutputStream().println("{\"error\":\"collector-offline\",\"message\":\"Collector at " + collectorBaseUrl + " is not reachable\"}");
		}
		catch (InterruptedException ex)
		{
			Thread.currentThread().interrupt();
			throw new IOException("HTTP request was interrupted for URL: " + url, ex);
		}
	}

	private static String jsonStr(String s)
	{
		if (s == null) return "null";
		return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
	}

	// -----------------------------------------------------------------------
	// Local-metrics path (DbxcLocalMetrics / DbxCentral self-monitoring)
	// -----------------------------------------------------------------------

	private void serveLocalMetrics(HttpServletRequest req, HttpServletResponse resp)
	throws IOException
	{
		String cmName    = req.getParameter("cm")   != null ? req.getParameter("cm")   : "";
		String timeParam = req.getParameter("time") != null ? req.getParameter("time") : "";
		String typeParam = req.getParameter("type") != null ? req.getParameter("type") : "abs";

		if (cmName.isEmpty())
		{
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			resp.setContentType(APPLICATION_JSON);
			resp.setCharacterEncoding("UTF-8");
			resp.getOutputStream().println("{\"error\":\"missing-cm-param\",\"message\":\"Parameter 'cm' is required\"}");
			return;
		}

		Timestamp requested;
		try
		{
			requested = Timestamp.valueOf(timeParam.trim());
		}
		catch (IllegalArgumentException ex)
		{
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			resp.setContentType(APPLICATION_JSON);
			resp.setCharacterEncoding("UTF-8");
			resp.getOutputStream().println("{\"error\":\"bad-time-param\",\"message\":" + jsonStr(timeParam) + "}");
			return;
		}

		if (!CentralPersistReader.hasInstance())
		{
			resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			resp.setContentType(APPLICATION_JSON);
			resp.setCharacterEncoding("UTF-8");
			resp.getOutputStream().println("{\"error\":\"no-reader\",\"message\":\"CentralPersistReader not available\"}");
			return;
		}

		CentralPersistReader.LocalMetricsCmDataResult result;
		try
		{
			result = CentralPersistReader.getInstance().getLocalMetricsCmDataNearTime(cmName, requested, 60, typeParam);
		}
		catch (SQLException ex)
		{
			_logger.error("ProxyCmDataServlet.serveLocalMetrics: DB error for CM '{}': {}", cmName, ex.getMessage(), ex);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			resp.setContentType(APPLICATION_JSON);
			resp.setCharacterEncoding("UTF-8");
			resp.getOutputStream().println("{\"error\":\"db-error\",\"message\":\"Database error reading local metrics\"}");
			return;
		}

		// Table doesn't exist or no data in window — tell JS to fall back to abs
		if (result == null)
		{
			if (!"abs".equalsIgnoreCase(typeParam))
			{
				resp.setContentType(APPLICATION_JSON);
				resp.setCharacterEncoding("UTF-8");
				resp.getOutputStream().println("{\"error\":\"table-not-found\",\"message\":\"No " + typeParam + " counters for CM: " + cmName + "\"}");
			}
			else
			{
				resp.setContentType(APPLICATION_JSON);
				resp.setCharacterEncoding("UTF-8");
				resp.getOutputStream().println("{\"error\":\"no-data-in-window\",\"message\":\"No local metrics data near requested time for CM: " + cmName + "\"}");
			}
			return;
		}

		// ---- Enrich with CounterController metadata (same strategy as real CmDataServlet) ----
		List<String>  diffColumns            = new ArrayList<>();
		List<String>  pctColumns             = new ArrayList<>();
		List<String>  tooltips               = new ArrayList<>(result.columns.size());
		String        description            = null;
		List<CmChartDescriptor>       chartDescriptors       = null;
		List<CmHighlighterDescriptor> highlighterDescriptors = null;
		List<Map<String, Object>>     preferredColumnOrder   = null;

		if (CounterController.hasInstance())
		{
			ICounterController cc     = CounterController.getInstance();
			CountersModel      cmMeta = cc.getCmByName(cmName);

			if (cmMeta != null)
			{
				// --- Diff / Pct columns ---
				// Strategy A: String[] getters (regular CMs)
				Set<String> diffSet = new HashSet<>();
				Set<String> pctSet  = new HashSet<>();
				if (cmMeta.getDiffColumns() != null)
					for (String d : cmMeta.getDiffColumns()) diffSet.add(d);
				if (cmMeta.getPctColumns() != null)
					for (String p : cmMeta.getPctColumns()) pctSet.add(p);

				// Strategy B fallback: polymorphic per-column methods (HostMonitor CMs)
				if (diffSet.isEmpty() && pctSet.isEmpty())
				{
					try
					{
						List<String> cmColNames = cmMeta.getColNames(CountersModel.DATA_ABS);
						if (cmColNames != null)
						{
							for (int ci = 0; ci < cmColNames.size(); ci++)
							{
								String cn = cmColNames.get(ci);
								try { if (cmMeta.isDiffColumn(ci)) diffSet.add(cn); } catch (Exception ignored) {}
								try { if (cmMeta.isPctColumn(ci))  pctSet.add(cn);  } catch (Exception ignored) {}
							}
						}
					}
					catch (Exception ignored) {}
				}

				// Intersect with actual returned columns
				for (String col : result.columns)
				{
					if (diffSet.contains(col)) diffColumns.add(col);
					if (pctSet.contains(col))  pctColumns.add(col);
				}

				// --- Tooltips ---
				for (String col : result.columns)
				{
					String tt = null;
					try { tt = cmMeta.getToolTipTextOnTableColumnHeader(col); } catch (Exception ignored) {}
					if (tt == null || tt.trim().isEmpty())
					{
						try { tt = cmMeta.getLocalToolTipTextOnTableColumnHeader(col); } catch (Exception ignored) {}
					}
					tooltips.add(tt);
				}

				// --- Descriptors ---
				description            = cmMeta.getDescription();
				chartDescriptors       = cmMeta.getChartDescriptors();
				highlighterDescriptors = cmMeta.getHighlighterDescriptors();

				// --- Preferred column order ---
				LinkedHashMap<String, ColumnHeaderPropsEntry> prefCols = cmMeta.getPreferredColumnProps();
				if (prefCols != null && !prefCols.isEmpty())
				{
					preferredColumnOrder = new ArrayList<>();
					for (ColumnHeaderPropsEntry e : prefCols.values())
					{
						Map<String, Object> entry = new LinkedHashMap<>();
						entry.put("columnName", e.getColumnName());
						entry.put("viewPos",    e.getViewPos());
						entry.put("visible",    e.isVisible());
						preferredColumnOrder.add(entry);
					}
				}
			}
		}
		// Pad tooltips so it is always parallel to columns
		while (tooltips.size() < result.columns.size()) tooltips.add(null);

		// Build and send response
		ObjectMapper om = new ObjectMapper();
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("cmName",       cmName);
		response.put("type",         typeParam);
		response.put("resolvedTime", result.resolvedTime.toString());
		response.put("rowCount",     result.rows.size());
		response.put("columns",      result.columns);
		response.put("rows",         result.rows);
		response.put("diffColumns",  diffColumns);
		response.put("pctColumns",   pctColumns);
		response.put("isNumeric",    result.isNumeric);
		response.put("tooltips",     tooltips);
		if (description            != null) response.put("description",            description);
		if (chartDescriptors       != null) response.put("chartDescriptors",       chartDescriptors);
		if (highlighterDescriptors != null) response.put("highlighterDescriptors", highlighterDescriptors);
		if (preferredColumnOrder   != null) response.put("preferredColumnOrder",   preferredColumnOrder);

		resp.setContentType(APPLICATION_JSON);
		resp.setCharacterEncoding("UTF-8");
		om.writeValue(resp.getOutputStream(), response);
	}
}
