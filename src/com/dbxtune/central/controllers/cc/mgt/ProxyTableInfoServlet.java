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
 * Central-side proxy for the vendor-generic table-info endpoint on the collector.
 *
 * <pre>GET /api/cc/mgt/table-info?srv=SRVNAME&amp;dbVendor=X&amp;dbname=Y&amp;tables=t1,t2
 *                              [&amp;format=html|text][&amp;ts=YYYY-MM-DD+HH:mm:ss]</pre>
 *
 * <p>Forwards to the collector's {@code /mgt/table-info} endpoint ({@code TableInfoServlet}).
 * Used by the ASE Showplan dialog's "Table Information" and "LLM Optimization Advice" sections
 * (mirrors {@link ProxyQueryStoreServlet}, which does the same for SQL Server's Query-Store-backed
 * {@code action=tableInfo} - this endpoint is the vendor-agnostic counterpart, not a replacement).
 */
public class ProxyTableInfoServlet
extends ProxyHelper
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		try
		{
			getSrvInfo(req);
		}
		catch (IOException ex)
		{
			_logger.warn("ProxyTableInfoServlet.getSrvInfo failed: " + ex.getMessage());
			sendJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "srv-not-found", ex.getMessage());
			return;
		}

		String collectorBaseUrl = getCollectorBaseUrl();
		if (collectorBaseUrl == null)
		{
			_logger.error("ProxyTableInfoServlet: Can't find Base URL for server '" + getSrvName() + "'.");
			sendJsonError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
					"collector-offline", "Cannot determine collector URL for server: " + getSrvName());
			return;
		}

		String dbVendor = Helper.getParameter(req, "dbVendor", "");
		if (dbVendor.isEmpty())
		{
			sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST,
					"bad-param", "Parameter 'dbVendor' is required.");
			return;
		}

		String dbname = Helper.getParameter(req, "dbname", "");
		String tables = Helper.getParameter(req, "tables", "");
		String format = Helper.getParameter(req, "format", "");
		String ts     = Helper.getParameter(req, "ts",     "");

		HtmlQueryString qs = new HtmlQueryString(collectorBaseUrl + "/api/mgt/table-info");
		qs.add          ("dbVendor", dbVendor);
		qs.addIfNotEmpty("dbname",   dbname);
		qs.addIfNotEmpty("tables",   tables);
		qs.addIfNotEmpty("format",   format);
		qs.addIfNotEmpty("ts",       ts);

		String url = qs.toString();

		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(url)).GET();

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
			_logger.warn("ProxyTableInfoServlet: Collector at " + collectorBaseUrl + " is offline: " + ex.getMessage());
			sendJsonError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
					"collector-offline", "Collector at " + collectorBaseUrl + " is not reachable");
		}
		catch (InterruptedException ex)
		{
			Thread.currentThread().interrupt();
			throw new IOException("HTTP request was interrupted for URL: " + url, ex);
		}
	}
}
