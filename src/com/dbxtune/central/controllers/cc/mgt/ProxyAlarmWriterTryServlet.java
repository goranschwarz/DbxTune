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
package com.dbxtune.central.controllers.cc.mgt;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.controllers.Helper;
import com.dbxtune.central.controllers.cc.ProxyHelper;
import com.dbxtune.central.pcs.DbxCentralRealm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Proxy for a Collector's {@code /api/mgt/writer/try} (try an AlarmWriter setting without saving it, eg "who would get this alarm mail?").
 * <p>
 * {@code POST /api/cc/mgt/writer/try?srvName=NAME}. It saves nothing, and needs no login: for anybody but an admin the collector
 * checks/renders the SAVED value, never text from the request (see handleRequest).
 */
public class ProxyAlarmWriterTryServlet
extends ProxyHelper
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		// One object PER REQUEST: ProxyHelper keeps the server name, host/port and token in instance fields,
		// and the servlet instance itself is shared by all concurrent requests.
		new ProxyAlarmWriterTryServlet().handleRequest(req, resp);
	}

	private void handleRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		String requestBody = IOUtils.toString(req.getReader());
		ObjectNode body;
		try { body = (ObjectNode) new ObjectMapper().readTree(requestBody); }
		catch (Exception ex) { sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "bad-request", "The body must be a JSON object."); return; }
		String op = body.path("op").asText("");

		// Anyone may look (the page is read only for them), but ONLY an admin may have text of his own evaluated, as only an admin may save it:
		// - a template can reach Java classes ($System ...), so rendering one is running code on the collector
		// - a mail rule is a regexp, and one from anybody could keep a collector thread busy
		// So for everybody else the collector uses the SAVED value ('savedOnly'), never the one in the request.
		// templateInfo / mailRoutingInfo evaluate nothing (variable list, built-in templates, alarm names): anyone.
		boolean isAdmin = req.isUserInRole(DbxCentralRealm.ROLE_ADMIN) || "admin".equals(req.getRemoteUser());
		if ( ! isAdmin && ("templatePreview".equals(op) || "mailRouting".equals(op)) )
		{
			body.put("savedOnly", true);
			body.put("template", ""); // a collector that does not know 'savedOnly' evaluates nothing
			body.put("value",    "");
			body.remove("values");
			requestBody = body.toString();
		}
		else if ( ! isAdmin && ! "templateInfo".equals(op) && ! "mailRoutingInfo".equals(op) )
		{
			if ( ! Helper.isAuthorized(req, resp) )
				return;
		}

		try
		{
			getSrvInfo(req);
		}
		catch (IOException ex)
		{
			sendJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "collector-not-found", ex.getMessage());
			return;
		}

		String collectorBaseUrl = getCollectorBaseUrl();
		if (collectorBaseUrl == null)
		{
			sendJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "collector-not-found", "No management URL found for the collector '" + getSrvName() + "'.");
			return;
		}

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(collectorBaseUrl + "/api/mgt/writer/try"))
				.timeout(Duration.ofSeconds(15))
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.header("Authorization", getMgtAuthentication())
				.POST(HttpRequest.BodyPublishers.ofByteArray(requestBody.getBytes(StandardCharsets.UTF_8)))
				.build();

		try
		{
			HttpResponse<byte[]> httpResponse = _httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

			// A collector older than this endpoint answers 404
			if (httpResponse.statusCode() == HttpServletResponse.SC_NOT_FOUND)
			{
				sendJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "collector-too-old", "The collector '" + getSrvName() + "' does not support this (it is an older version).");
				return;
			}
			resp.setStatus(httpResponse.statusCode());
			sendResult(httpResponse, resp, APPLICATION_JSON);
		}
		catch (InterruptedException ex)
		{
			Thread.currentThread().interrupt();
			throw new IOException("HTTP request was interrupted for URL: " + collectorBaseUrl + "/api/mgt/writer/try", ex);
		}
		catch (IOException ex)
		{
			_logger.info("Could not reach the collector '" + getSrvName() + "' at '" + collectorBaseUrl + "/api/mgt/writer/try'. Caught: " + ex);
			sendJsonError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "collector-offline", "Could not reach the collector '" + getSrvName() + "': " + ex.getMessage());
		}
	}
}
