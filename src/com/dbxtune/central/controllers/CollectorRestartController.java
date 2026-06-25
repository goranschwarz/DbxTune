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

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
 * Sends a "restart" request to one or more collectors.
 * Registered at {@code /api/collector-restart}.
 *
 * <p>Uses the collector's management REST endpoint
 * {@code http://host:port/api/mgt/restart} (preferred).
 * No file-based fallback — restart cannot be signalled via a sentinel file.
 */
public class CollectorRestartController
extends HttpServlet
{
	private static final long   serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private static final HttpClient _httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.build();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		ServletOutputStream out = resp.getOutputStream();

		if (!CentralPersistReader.hasInstance())
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No PCS Reader to: DBX Central Database.");
			return;
		}

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

				String result = restartCollector(srvName);
				resultMap.put(srvName, result);
			}
		}
		catch (Exception e)
		{
			_logger.warn("CollectorRestartController: unexpected error: " + e.getMessage(), e);
			throw new ServletException("Problem in CollectorRestartController: " + e, e);
		}

		ObjectMapper om = Helper.createObjectMapper();
		out.println(om.writeValueAsString(resultMap));
		out.flush();
		out.close();
	}

	private String restartCollector(String srvName)
	{
		// Look up management host/port from the collector's info file
		String mgtHost = null;
		int    mgtPort = -1;
		String authStr = null;

		List<String> infoFiles = OverviewServlet.getInfoFilesDbxTune();
		for (String filePath : infoFiles)
		{
			java.io.File f = new java.io.File(filePath);
			String fileSrvName = f.getName().split("\\.")[0];
			if (!fileSrvName.equals(srvName))
				continue;

			Configuration conf = new Configuration(filePath);
			mgtHost = conf.getProperty   ("dbxtune.management.host", null);
			mgtPort = conf.getIntProperty("dbxtune.management.port", -1);

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

		if (!StringUtil.hasValue(mgtHost) || mgtPort <= 0)
		{
			String msg = "No management host/port found for collector '" + srvName + "' — restart requires a running collector with a registered management endpoint.";
			_logger.warn("CollectorRestartController: " + msg);
			return "ERROR: " + msg;
		}

		String url = "http://" + mgtHost + ":" + mgtPort + "/api/mgt/restart";
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
				_logger.info("CollectorRestartController: REST restart OK for srv='" + srvName + "' via " + url);
				return "SUCCESS";
			}
			else
			{
				String msg = "REST API returned HTTP " + httpResp.statusCode() + " for srv='" + srvName + "' at " + url;
				_logger.warn("CollectorRestartController: " + msg);
				return "ERROR: " + msg;
			}
		}
		catch (Exception ex)
		{
			String msg = "Could not reach collector '" + srvName + "' at " + url + ": " + ex.getMessage();
			_logger.warn("CollectorRestartController: " + msg);
			return "ERROR: " + msg;
		}
	}
}
