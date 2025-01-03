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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.controllers.cc.ProxyHelper;

public class ProxyConfigGetServlet 
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
		
//		ServletOutputStream out = resp.getOutputStream();
////		resp.setContentType("text/html");
////		resp.setCharacterEncoding("UTF-8");
//		resp.setContentType("application/json");
//		resp.setCharacterEncoding("UTF-8");
//		resp.setHeader("Access-Control-Allow-Origin", "*");

		// Check for known input parameters
//		if (Helper.hasUnKnownParameters(req, resp, "sessionName", "srv", "srvName",    "cmName", "graphName", "startTime", "endTime", "sampleType", "sampleValue"))
//			return;

//		String sessionName    = Helper.getParameter(req, new String[] {"sessionName", "srv", "srvName"} );
//		String cmName         = Helper.getParameter(req, "cmName");
//		String graphName      = Helper.getParameter(req, "graphName");
//		String startTime      = Helper.getParameter(req, "startTime");
//		String endTime        = Helper.getParameter(req, "endTime");
//		String sampleTypeStr  = Helper.getParameter(req, "sampleType");
//		String sampleValueStr = Helper.getParameter(req, "sampleValue");

System.out.println();
System.out.println("PROXY-ConfigGetServlet --- XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX: getContextPath()=" + req.getContextPath());
System.out.println("PROXY-ConfigGetServlet --- XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX: getPathInfo   ()=" + req.getPathInfo());
System.out.println("PROXY-ConfigGetServlet --- XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX: getQueryString()=" + req.getQueryString());
System.out.println("PROXY-ConfigGetServlet --- XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX: getParameterMap()=" + req.getParameterMap());

final String auth = req.getHeader("Authorization");
System.out.println(">>>>>>>>>>>> NoGuiConfigGetServlet.GET.HEADER: Authorization = |" + auth + "|.");
if (auth != null)
{
	int firstSpace = auth.indexOf(" ");
	String authMethod = auth.substring(0, firstSpace).trim();
	String authToken  = auth.substring(firstSpace).trim();
	
	if ("Basic".equals(authMethod))
	{
		byte[] decodedBytes = Base64.getDecoder().decode(authToken);
		authToken = new String(decodedBytes);
	}
	
//	String decoded = new String( Base64.getDecoder().decode( auth.substring(firstSpace) ), StandardCharsets.UTF_8 );
	System.out.println(">>>>>>>>>>>> NoGuiConfigGetServlet.GET.HEADER: Authorization.method = |" + authMethod + "|.");
	System.out.println(">>>>>>>>>>>> NoGuiConfigGetServlet.GET.HEADER: Authorization.token  = |" + authToken  + "|.");
}

		// Get "stuff" from the PCS or LOCAL-INFO-FILE (based on the Server Name)


//		URL url = new URL("http://otherserver:otherport/url");
		URL url = new URL(getCollectorBaseUrl() + "/api/mgt/config/get");
		HttpURLConnection httpConn = (HttpURLConnection)url.openConnection();
		httpConn.setRequestMethod("GET");

		// set request header if required
//		httpConn.setRequestProperty("Authentication", "value1");
//		httpConn.setRequestProperty("header1", "value1");

		sendResult(httpConn, resp, APPLICATION_JSON);
	}

}
