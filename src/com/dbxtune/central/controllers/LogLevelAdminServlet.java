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
package com.dbxtune.central.controllers;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.controllers.cc.ProxyHelper;
import com.dbxtune.mgt.controllers.LogLevelServlet;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Admin-only servlet to view and change Log4j log levels at runtime, in DbxCentral and in the Collectors.
 * Mapped to {@code /admin/log-level}, protected by the existing {@code /admin/*} security constraint.
 * <p>
 * The server name {@code DbxCentral} is DbxCentral itself, any other name is a Collector, which we
 * reach at it's management interface {@code /api/mgt/log-level} (see {@link LogLevelServlet} for the JSON).
 * <ul>
 *   <li>{@code GET  /admin/log-level?srv=NAME} - the loggers and levels for ONE server (same JSON as the Collector returns)</li>
 *   <li>{@code POST /admin/log-level?srv=NAME1,NAME2,DbxCentral} - set levels in all the named servers.<br>
 *       The body is sent as it is to each Collector: {@code {"saveType":"IN_MEMORY|THIS_SERVER", "changes":[{"logger":"...", "level":"DEBUG"}]}}<br>
 *       Returns one result for each server: {@code {"NAME1":{"status":"success", ...}, "NAME2":{"status":"FAILED", "error":"collector-offline", "message":"..."}}}</li>
 * </ul>
 */
public class LogLevelAdminServlet
extends HttpServlet
{
	private static final long   serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/** The server name used for DbxCentral itself */
	public static final String DBX_CENTRAL = "DbxCentral";

	private static final HttpClient _httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.build();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		String srvName = Helper.getParameter(req, new String[] {"srv", "srvName"});
		if (StringUtil.isNullOrBlank(srvName))
		{
			ProxyHelper.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "bad-param", "Parameter 'srv' is required");
			return;
		}
		srvName = srvName.trim();

		Map<String, Object> result;
		if (DBX_CENTRAL.equals(srvName))
			result = LogLevelServlet.getLogLevels(DBX_CENTRAL);
		else
			result = sendToCollector(srvName, null);

		// A failure from the Collector (or that we couldn't reach it)
		if ("FAILED".equals(result.get("status")))
			resp.setStatus("collector-not-running".equals(result.get("error")) ? HttpServletResponse.SC_NOT_FOUND : HttpServletResponse.SC_SERVICE_UNAVAILABLE);

		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		Helper.createObjectMapper().writeValue(resp.getWriter(), result);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		String srvNames = Helper.getParameter(req, new String[] {"srv", "srvName"});
		if (StringUtil.isNullOrBlank(srvNames))
		{
			ProxyHelper.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "bad-param", "Parameter 'srv' is required (one or several server names, comma separated)");
			return;
		}

		String requestBody = IOUtils.toString(req.getReader());
		String requestedBy = "user '" + req.getRemoteUser() + "' from host '" + req.getRemoteHost() + "'";

		_logger.info("Log level change requested by " + requestedBy + ", for servers '" + srvNames + "'. requestBody=|" + requestBody + "|");

		Map<String, Object> resultMap = new LinkedHashMap<>();
		for (String srvName : StringUtil.parseCommaStrToList(srvNames))
		{
			if ( ! StringUtil.hasValue(srvName) )
				continue;

			if (DBX_CENTRAL.equals(srvName))
				resultMap.put(srvName, LogLevelServlet.setLogLevels(requestBody, requestedBy));
			else
				resultMap.put(srvName, sendToCollector(srvName, requestBody));
		}

		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		Helper.createObjectMapper().writeValue(resp.getWriter(), resultMap);
	}

	/**
	 * Send the request to a Collector's management interface: /api/mgt/log-level
	 *
	 * @param srvName        Name of the Collector
	 * @param requestBody    null = GET, otherwise POST with this body
	 * @return What the Collector answered, or {"status":"FAILED", "error":"collector-...", "message":"..."} if we couldn't get an answer
	 */
	private Map<String, Object> sendToCollector(String srvName, String requestBody)
	{
		// Look up management host/port and the token from the collector's info file
		String mgtHost = null;
		int    mgtPort = -1;
		String authStr = null;

		for (String filePath : OverviewServlet.getInfoFilesDbxTune())
		{
			String fileSrvName = new File(filePath).getName().split("\\.")[0];
			if ( ! fileSrvName.equals(srvName) )
				continue;

			Configuration conf = new Configuration(filePath);
			mgtHost = conf.getProperty   ("dbxtune.management.host", null);
			mgtPort = conf.getIntProperty("dbxtune.management.port", -1);

			// Looks like: {"authorization":"Bearer xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"}
			String mgtInfo = conf.getProperty("dbxtune.management.info", null);
			if (StringUtil.hasValue(mgtInfo))
			{
				try
				{
					JsonNode root = new ObjectMapper().readTree(mgtInfo);
					if (root.has("authorization"))
						authStr = root.get("authorization").asText(null);
				}
				catch (Exception ignored) {}
			}
			break;
		}

		if ( ! StringUtil.hasValue(mgtHost) || mgtPort <= 0 )
			return failed("collector-not-running", "The collector for '" + srvName + "' is not running (no info file with a management host/port was found).");

		String url = "http://" + mgtHost + ":" + mgtPort + "/api/mgt/log-level";
		try
		{
			HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
					.uri(URI.create(url))
					.timeout(Duration.ofSeconds(5))
					.header("Accept", "application/json");

			if (requestBody == null)
				reqBuilder.GET();
			else
				reqBuilder.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(requestBody));

			if (StringUtil.hasValue(authStr))
				reqBuilder.header("Authorization", authStr);

			HttpResponse<String> httpResp = _httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

			// The Collector answers with JSON on success (200) and when it rejects the changes (400)
			JsonNode answer = null;
			try { answer = new ObjectMapper().readTree(httpResp.body()); }
			catch (Exception ignored) {}

			boolean isLogLevelAnswer = answer != null && (answer.has("loggers") || answer.has("status"));
			if ( ! isLogLevelAnswer )
			{
				String msg = "The collector for '" + srvName + "' at " + url + " returned HTTP " + httpResp.statusCode() + ".";
				if (httpResp.statusCode() == HttpServletResponse.SC_NOT_FOUND)
					msg += " It probably runs an older version, which can't change log levels.";

				_logger.warn("LogLevelAdminServlet: " + msg + " body=|" + httpResp.body() + "|");
				return failed("collector-error", msg);
			}

			@SuppressWarnings("unchecked")
			Map<String, Object> result = new ObjectMapper().convertValue(answer, LinkedHashMap.class);
			return result;
		}
		catch (InterruptedException ex)
		{
			Thread.currentThread().interrupt();
			return failed("collector-offline", "Interrupted while waiting for the collector for '" + srvName + "' at " + url + ".");
		}
		catch (Exception ex)
		{
			String msg = "Could not reach the collector for '" + srvName + "' at " + url + ": " + ex;
			_logger.warn("LogLevelAdminServlet: " + msg);
			return failed("collector-offline", msg);
		}
	}

	private static Map<String, Object> failed(String errorCode, String message)
	{
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("status" , "FAILED");
		result.put("error"  , errorCode);
		result.put("message", message);
		return result;
	}
}
