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
 * Central-side proxy: GET /api/cc/mgt/ddl-storage?srv=SRVNAME[&amp;objectName=X][&amp;dbname=Y][&amp;type=U,V][&amp;includeText=true][&amp;limit=200]
 * <p>
 * Forwards the request to the collector's /api/mgt/ddl-storage endpoint and
 * returns the JSON response to the caller.
 *
 * <p>The collector stores DDL definitions, table/index metadata, execution plans
 * and other object details in its MonDdlStorage H2 table.  This proxy exposes
 * that data from DbxCentral without requiring direct access to the collector's
 * embedded database.
 */
public class ProxyDdlStorageServlet
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
			_logger.warn("ProxyDdlStorageServlet.getSrvInfo failed: " + ex.getMessage());
			sendJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "srv-not-found", ex.getMessage());
			return;
		}

		String collectorBaseUrl = getCollectorBaseUrl();
		if (collectorBaseUrl == null)
		{
			_logger.error("ProxyDdlStorageServlet: Can't find Base URL for server '" + getSrvName() + "'.");
			sendJsonError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
					"collector-offline", "Cannot determine collector URL for server: " + getSrvName());
			return;
		}

		// Forward optional filter parameters
		String objectName  = Helper.getParameter(req, "objectName",  "");
		String dbname      = Helper.getParameter(req, "dbname",      "");
		String type        = Helper.getParameter(req, "type",        "");
		String includeText = Helper.getParameter(req, "includeText", "true");
		String limit       = Helper.getParameter(req, "limit",       "200");
		String ts          = Helper.getParameter(req, "ts",          "");

		HtmlQueryString qs = new HtmlQueryString(collectorBaseUrl + "/api/mgt/ddl-storage");
		qs.add(          "includeText", includeText);
		qs.add(          "limit",       limit);
		qs.addIfNotEmpty("objectName",  objectName);
		qs.addIfNotEmpty("dbname",      dbname);
		qs.addIfNotEmpty("type",        type);
		qs.addIfNotEmpty("ts",          ts);

		String url = qs.toString();

		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.GET();

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
			_logger.warn("ProxyDdlStorageServlet: Collector at " + collectorBaseUrl + " is offline: " + ex.getMessage());
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
