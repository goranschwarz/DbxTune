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
import java.net.HttpURLConnection;
import java.net.URL;

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
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
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
		
		String reportName = Helper.getParameter(req, "reportName");

		String urlStr = null;
		if ("sqlserver-job-scheduler-report".equals(reportName))
		{
			urlStr = "sqlserver/job-scheduler-report";
		}
		else if ("sqlserver-job-scheduler-activity".equals(reportName))
		{
			urlStr = "sqlserver/job-scheduler-activity";
		}
		else
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Report name '" + reportName + "' was NOT found.");
			return;
		}

		// Build a query String (if any)
		// "remove" some entries we passed: "srvName", "reportName", which we no longer need (or want to pass on)
		String queryString = HtmlQueryString.removeParameter(req.getQueryString(), "srvName", "reportName");

		if (StringUtil.isNullOrBlank(queryString))
			queryString = "";
		else
			queryString = "?" + queryString;
		

		// Call the remote URL
		URL url = new URL(getCollectorBaseUrl() + "/api/reports/" + urlStr + queryString);
System.out.println("ProxyReportsServlet: CALLING URL: " + url);
		if (_logger.isDebugEnabled())
			_logger.debug("ProxyReportsServlet: CALLING URL: " + url);
		HttpURLConnection httpConn = (HttpURLConnection)url.openConnection();

		// set request header if required
		httpConn.setRequestProperty("Authentication", getMgtAuthentication());
//		httpConn.setRequestProperty("header1", "value1");

		httpConn.setRequestMethod("GET");

//		// PROXY: Transfer data from the Caller to the HTTP end point 
//		sendData(req, httpConn);

		// PROXY: Transfer data from the HTTP Call to the caller 
		sendResult(httpConn, resp, TEXT_HTML);
	}
}
