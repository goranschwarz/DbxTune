/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.central.controllers;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.Timestamp;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;

import com.asetune.Version;
import com.asetune.central.controllers.ud.chart.IUserDefinedChart;
import com.asetune.central.controllers.ud.chart.UserDefinedChartManager;
import com.asetune.utils.StringUtil;

public class UserDefinedContentServlet
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
//	private static final Logger _logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
	throws ServletException, IOException
	{
		ServletOutputStream out = resp.getOutputStream();
		resp.setContentType("text/html");
		resp.setCharacterEncoding("UTF-8");

		// Get generic input parameters
		String name      = Helper.getParameter(req, "name"    ,"");
		String srvName   = Helper.getParameter(req, "srvName" ,"");
		int    refresh   = StringUtil.parseInt( Helper.getParameter(req, "refresh",  "-1"),  -1);

		String[] knownParams = new String[] {"name", "srvName", "refresh"};
		
		// Check MANDATORY generic input parameters
		if (StringUtil.isNullOrBlank(name)) 
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Expected parameter 'name', which wasn't passed or blank.");
			return;
		}
		if (StringUtil.isNullOrBlank(name)) 
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Expected parameter 'srvName', which wasn't passed or blank.");
			return;
		}

		// Get the User Defined Content object, which is responsible for producing the content
		IUserDefinedChart udc = UserDefinedChartManager.getInstance().getChart(name, srvName);
		if (udc == null)
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No User Defined Content with the name='" + name + "', srvName='" + srvName + "'.");
			return;
		}

		
		// Check for known input parameters
		String[] udcKnownParams = udc.getKnownParameters();
		if (Helper.hasUnKnownParameters(req, resp, ArrayUtils.addAll(knownParams, udcKnownParams)))
			return;
		

		// Get KNOWN Parameters for the User Defined Content producer
		udc.setUrlParameters(Helper.getParameterMap(req));
		try 
		{
			udc.checkUrlParameters(udc.getUrlParameters());
		}
		catch (Exception ex) 
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
			return;
		}
		

		try
		{
			// Execute the User Defined Logic
			udc.produce();
		}
		catch (Exception ex)
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error when creating User Defined Content. Caught: " + ex.getMessage());
			return;
		}


//		String username = System.getProperty("user.name");
//		String hostname = InetAddress.getLocalHost().getHostName();
		
		out.println("<html>");
		out.println("<head>");

		out.println("<title>" + udc.getDbmsServerName() + " - " + udc.getName() + "</title> ");
		
		if (refresh > 0)
			out.println("<meta http-equiv='refresh' content='"+refresh+"' />");

		out.println(HtmlStatic.getUserDefinedContentHead());
		
//		out.println("<style type='text/css'>");
//		out.println("  table {border-collapse: collapse;}");
//		out.println("  th {border: 1px solid black; text-align: left; padding: 2px; white-space: nowrap; background-color:gray; color:white;}");
//		out.println("  td {border: 1px solid black; text-align: left; padding: 2px; white-space: nowrap; }");
//		out.println("  tr:nth-child(even) {background-color: #f2f2f2;}");
////		out.println("  .topright { position: absolute; top: 8px; right: 16px; font-size: 14px; }"); // topright did not work with bootstrap (and navigation bar) 
//		out.println("</style>");

		out.println("</head>");
		
		out.println("<body onload='updateLastUpdatedClock()'>");

		out.println(HtmlStatic.getUserDefinedContentNavbar());

		out.println("<div class='container-fluid'>");

//		String ver = "Version: " + Version.getVersionStr() + ", Build: " + Version.getBuildStr();
//		out.println("<h1>DbxTune - Central - " + username + "@" + hostname + "</h1>");
//		out.println("<div class='topright'>"+ver+"</div>");

//		out.println("<p>");
		out.println("Page loaded: <span id='last-update-ts'>" + (new Timestamp(System.currentTimeMillis())) + "</span>, ");
		if (refresh > 0)
			out.println("This page will 'auto-refresh' every " + refresh + " second. This can be changed with URL parameter 'refresh=##' (where ## is seconds)<br>" );
		else
			out.println("To 'auto-refresh' this page every ## second. This can be changed with URL parameter 'refresh=##' (where ## is seconds)<br>" );
//		out.println("</p>");

		out.println("<script>");
		out.println("function updateLastUpdatedClock() {                   ");
		out.println("    var ageInSec = Math.floor((new Date() - lastUpdateTime) / 1000);");
		out.println("    document.getElementById('last-update-ts').innerHTML = ageInSec + ' seconds ago'; ");
//		out.println("    console.log('updateLastUpdatedClock(): ' + document.getElementById('last-update-ts'));");
//		out.println("    console.log('updateLastUpdatedClock(): ' + ageInSec + ' seconds ago');");
		out.println("    setTimeout(updateLastUpdatedClock, 1000); ");
		out.println("}                                                     ");
		out.println("var lastUpdateTime = new Date();");
		out.println("</script>");
		out.println("");
		
		for (String scriptLocation : udc.getJaveScriptList())
		{
			out.println("<script type='text/javascript' src='" + scriptLocation + "'></script>");
		}

		out.println("<div id='ud-content'>");

		// Get the User Defined Content
		out.println( udc.getContent() );
		
		out.println("</div>"); // ud-content
		
		out.println("</div>"); // container-fluid

		// Write some JavaScript code
		out.println(HtmlStatic.getJavaScriptAtEnd(true));

		out.println("</body>");
		out.println("</html>");
		
		out.flush();
		out.close();

	} // end: doGet
}
