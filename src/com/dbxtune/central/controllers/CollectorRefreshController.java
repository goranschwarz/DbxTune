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
package com.dbxtune.central.controllers;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Sends a "force refresh now" request to one or more collectors.
 *
 * <h3>Strategy (most reliable first)</h3>
 * <ol>
 *   <li><b>REST API</b> — if the collector's management host/port are known
 *       (read from the {@code SRVNAME.dbxtune} info file), a GET request is
 *       sent to {@code http://host:port/mgt/refresh}.  This works for both
 *       local and remote collectors and is the preferred method.</li>
 *   <li><b>Temp-file fallback</b> — if the info file is not found (e.g. the
 *       collector has not yet registered with Central), a sentinel file is
 *       written to {@code java.io.tmpdir}.  The local collector polls for
 *       this file and triggers a refresh when it sees it.  Remote collectors
 *       cannot be reached this way.</li>
 * </ol>
 */
public class CollectorRefreshController
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	// Legacy file-based signalling (kept as fallback)
	public static final String REFRESH_REQUEST_SERVERNAME_TEMPLATE = "SERVERNAME";
	public static final String REFRESH_REQUEST_FILE_NAME_TEMPLATE  =
			System.getProperty("java.io.tmpdir") + File.separatorChar
			+ "DbxTune.refresh." + REFRESH_REQUEST_SERVERNAME_TEMPLATE;

	private static final HttpClient _httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.build();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		ServletOutputStream out = resp.getOutputStream();

		// Check that we have a READER
		if ( ! CentralPersistReader.hasInstance() )
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No PCS Reader to: DBX Central Database.");
			return;
		}

		// SRVNAME → result message
		Map<String, String> resultMap = new LinkedHashMap<>();

		try
		{
			if (Helper.hasUnKnownParameters(req, resp, "srv", "srvName"))
				return;

			String input_srv = Helper.getParameter(req, new String[] {"srv", "srvName"});

			for (String srvName : StringUtil.parseCommaStrToList(input_srv))
			{
				if (!StringUtil.hasValue(srvName))
					continue;

				String result = refreshCollector(srvName);
				resultMap.put(srvName, result);
			}
		}
		catch (Exception e)
		{
			_logger.warn("CollectorRefreshController: unexpected error: " + e.getMessage(), e);
			throw new ServletException("Problem in CollectorRefreshController: " + e, e);
		}

		ObjectMapper om = Helper.createObjectMapper();
		out.println(om.writeValueAsString(resultMap));
		out.flush();
		out.close();
	}

	/**
	 * Tries to refresh the named collector, preferring the REST API.
	 * Returns a human-readable result string.
	 */
	private String refreshCollector(String srvName)
	{
		// ── 1. Look up the collector's management endpoint from the info file ──
		String mgtHost = null;
		int    mgtPort = -1;
		String authStr = null;

		List<String> infoFiles = OverviewServlet.getInfoFilesDbxTune();
		for (String filePath : infoFiles)
		{
			File f = new File(filePath);
			String fileSrvName = f.getName().split("\\.")[0];
			if (!fileSrvName.equals(srvName))
				continue;

			Configuration conf = new Configuration(filePath);
			mgtHost = conf.getProperty   ("dbxtune.management.host", null);
			mgtPort = conf.getIntProperty("dbxtune.management.port", -1);

			// Optional bearer / Basic auth stored in management.info JSON
			String mgtInfo = conf.getProperty("dbxtune.management.info", null);
			if (StringUtil.hasValue(mgtInfo))
			{
				try
				{
					com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
					com.fasterxml.jackson.databind.JsonNode root = om.readTree(mgtInfo);
					if (root.has("authorization"))
						authStr = root.get("authorization").asText(null);
				}
				catch (Exception ignored) {}
			}
			break;
		}

		// ── 2. Try REST API if we have host + port ──────────────────────────
		if (StringUtil.hasValue(mgtHost) && mgtPort > 0)
		{
			String url = "http://" + mgtHost + ":" + mgtPort + "/api/mgt/refresh";
			try
			{
				HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
						.uri(URI.create(url))
						.timeout(Duration.ofSeconds(5))
						.GET();

				if (StringUtil.hasValue(authStr))
					reqBuilder.header("Authorization", authStr);

				HttpResponse<String> httpResp = _httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

				if (httpResp.statusCode() >= 200 && httpResp.statusCode() < 300)
				{
					_logger.info("CollectorRefreshController: REST refresh OK for srv='" + srvName + "' via " + url);
					return "SUCCESS (REST API)";
				}
				else
				{
					String msg = "REST API returned HTTP " + httpResp.statusCode() + " for srv='" + srvName + "' at " + url;
					_logger.warn("CollectorRefreshController: " + msg);
					return "ERROR: " + msg;
				}
			}
			catch (Exception ex)
			{
				_logger.warn("CollectorRefreshController: REST refresh failed for srv='" + srvName
						+ "' at " + url + " — falling back to temp-file method. Reason: " + ex.getMessage());
				// fall through to file-based fallback
			}
		}

		// ── 3. Fallback: temp-file sentinel (local collector only) ──────────
		String filename = REFRESH_REQUEST_FILE_NAME_TEMPLATE.replace(REFRESH_REQUEST_SERVERNAME_TEMPLATE, srvName);
		_logger.info("CollectorRefreshController: writing refresh sentinel file '" + filename + "' for srv='" + srvName + "'.");
		try
		{
			Files.createFile(Paths.get(filename));
			return "SUCCESS (file fallback)";
		}
		catch (FileAlreadyExistsException ignore)
		{
			return "SUCCESS (file already pending)";
		}
		catch (Exception ex)
		{
			String msg = "Could not write sentinel file '" + filename + "': " + ex.getMessage();
			_logger.warn("CollectorRefreshController: " + msg);
			return "ERROR: " + msg;
		}
	}
}
