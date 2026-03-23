/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.central.controllers.cc.report;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
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
import com.dbxtune.utils.StringUtil;

public class ProxyReportsServlet
extends ProxyHelper
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		// Get "basic" stuff... implemented in parent class
		getSrvInfo(request);

		String collectorBaseUrl = getCollectorBaseUrl();
		if (collectorBaseUrl == null)
		{
			_logger.error("Can't find Base URL for Counter Collector named '" + getSrvName() + "'.");
			// TODO: send some status message and possibly a JSON "error" object
			return;
		}
		
		String reportName = Helper.getParameter(request, "reportName");

		String urlStr = null;
		if ("sqlserver-job-scheduler-report".equals(reportName))
		{
			urlStr = "sqlserver/job-scheduler-report";
		}
		else if ("sqlserver-job-scheduler-timeline".equals(reportName))
		{
			urlStr = "sqlserver/job-scheduler-timeline";
		}
		else
		{
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Report name '" + reportName + "' was NOT found.");
			return;
		}

		// Build a query String (if any)
		// "remove" some entries we passed: "srvName", "reportName", which we no longer need (or want to pass on)
		String queryString = HtmlQueryString.removeParameter(request.getQueryString(), "srvName", "reportName");

		if (StringUtil.isNullOrBlank(queryString))
			queryString = "";
		else
			queryString = "?" + queryString;
		

		// Call the remote URL
		String targetUrl = getCollectorBaseUrl() + "/api/reports/" + urlStr + queryString;

		if (_logger.isDebugEnabled())
			_logger.debug("ProxyReportsServlet: CALLING URL: " + targetUrl);

		// GET request
		HttpRequest.Builder builder = HttpRequest.newBuilder()
				.uri(URI.create(targetUrl))
				.GET();

		// Forward incoming request headers
		forwardRequestHeaders(request, builder);

		// set request header if required
		builder.header("Authentication", getMgtAuthentication());

		try
		{
			HttpResponse<byte[]> httpResponse = _httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
			sendResult(httpResponse, response, TEXT_HTML);
		}
		catch (InterruptedException ex)
		{
			Thread.currentThread().interrupt();
			throw new IOException("HTTP request was interrupted for URL: " + targetUrl, ex);
		}
		

//		// PROXY: Transfer data from the HTTP Call to the caller 
//		try
//		{
//			// Get response from target server
//			int    responseCode = httpConn.getResponseCode();
//
//			// NOTE: if respoceCode is 400 (or above) then forward: ERROR Stream
//			// NOTE: if respoceCode is less than 400  then forward: INPUT Stream
//			boolean transferErrors = responseCode >= 400;
//			
//			// Use direct stream copying to avoid charset issues
//			try (InputStream  inputStream  = (transferErrors ? httpConn.getErrorStream() : httpConn.getInputStream()); 
//			     OutputStream outputStream = response.getOutputStream())
//			{
//				byte[] buffer = new byte[4096];
//				int    bytesRead;
//				while ((bytesRead = inputStream.read(buffer)) != -1)
//				{
//					outputStream.write(buffer, 0, bytesRead);
//				}
//				outputStream.flush();
//			}
//		}
//		finally
//		{
//			httpConn.disconnect();
//		}
	}
}
