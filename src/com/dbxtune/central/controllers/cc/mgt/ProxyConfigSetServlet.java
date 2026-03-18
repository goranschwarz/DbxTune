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
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.controllers.cc.ProxyHelper;

public class ProxyConfigSetServlet 
extends ProxyHelper
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		// Get "basic" stuff... implemented in parent class
		getSrvInfo(req);
		
		String collectorBaseUrl = getCollectorBaseUrl();
		if (collectorBaseUrl == null)
		{
			_logger.error("Can't find Base URL for Counter Collector named '" + getSrvName() + "'.");
			// TODO: send some status message and possibly a JSON "error" object
			return;
		}
		

		// Read request body from the incoming servlet request
		String requestBody = IOUtils.toString(req.getReader());
		byte[] requestBodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(getCollectorBaseUrl() + "/api/mgt/config/set"))
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.header("Authentication", getMgtAuthentication())
				.POST(HttpRequest.BodyPublishers.ofByteArray(requestBodyBytes))
				.build();

		try
		{
			HttpResponse<byte[]> httpResponse = _httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
			sendResult(httpResponse, resp, APPLICATION_JSON);
		}
		catch (InterruptedException ex)
		{
			Thread.currentThread().interrupt();
			throw new IOException("HTTP request was interrupted for URL: " + getCollectorBaseUrl() + "/api/mgt/config/set", ex);
		}
	}

}
