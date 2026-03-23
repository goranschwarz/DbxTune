/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
 *
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 *
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.controllers.cc.ProxyHelper;

/**
 * Central-side proxy: GET /api/cc/mgt/dbms-config?srv=SRVNAME&amp;ts=YYYY-MM-DD+HH:mm:ss
 * <p>
 * Forwards the request to the collector's /api/mgt/dbms-config endpoint and
 * returns the JSON response to the browser.
 */
public class ProxyDbmsConfigServlet
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
			_logger.warn("ProxyDbmsConfigServlet.getSrvInfo failed: " + ex.getMessage());
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			resp.setContentType(APPLICATION_JSON);
			resp.setCharacterEncoding("UTF-8");
			resp.getOutputStream().println("{\"error\":\"srv-not-found\",\"message\":" + jsonStr(ex.getMessage()) + "}");
			return;
		}

		String collectorBaseUrl = getCollectorBaseUrl();
		if (collectorBaseUrl == null)
		{
			_logger.error("ProxyDbmsConfigServlet: Can't find Base URL for server '" + getSrvName() + "'.");
			resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			resp.setContentType(APPLICATION_JSON);
			resp.setCharacterEncoding("UTF-8");
			resp.getOutputStream().println("{\"error\":\"collector-offline\",\"message\":\"Cannot determine collector URL for server: " + getSrvName() + "\"}");
			return;
		}

		String srvParam = req.getParameter("srv") != null ? req.getParameter("srv") : getSrvName();
		String tsParam  = req.getParameter("ts")  != null ? req.getParameter("ts")  : "";

		String url = collectorBaseUrl + "/api/mgt/dbms-config"
				+ "?srv=" + URLEncoder.encode(srvParam, StandardCharsets.UTF_8)
				+ "&ts="  + URLEncoder.encode(tsParam,  StandardCharsets.UTF_8);

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
			_logger.warn("ProxyDbmsConfigServlet: Collector at " + collectorBaseUrl + " is offline: " + ex.getMessage());
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
}
