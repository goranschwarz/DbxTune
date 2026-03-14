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
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.sql.Timestamp;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.controllers.ud.action.IUserDefinedAction;
import com.dbxtune.central.controllers.ud.action.UserDefinedActionAbstract;
import com.dbxtune.central.controllers.ud.action.UserDefinedActionManager;
import com.dbxtune.central.pcs.DbxCentralRealm;
import com.dbxtune.utils.StringUtil;

public class UserDefinedActionServlet
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * First check if the ROLE is OK<br>
	 * Then (if Users is NOT empty). Do extra check and only allow specific users (within that ROLE)  
	 * 
	 * @param req
	 * @param udAction
	 * @return
	 */
	private boolean isAuthorized(HttpServletRequest req, IUserDefinedAction udAction)
	{
		String userName = req.getRemoteUser();
		if (StringUtil.isNullOrBlank(userName))
		{
			return false;
		}

		// Special case for ADMIN -- Always ALLOW
		boolean isAdmin = req.isUserInRole(DbxCentralRealm.ROLE_ADMIN);
		if (isAdmin)
		{
			_logger.info("UserDefinedAction '" + udAction.getName() + "' and the user '" + userName + "' was authenticated as role '" + DbxCentralRealm.ROLE_ADMIN + "'.");
			return true;
		}

		boolean isAuthorized = false;
		
		List<String> authorizedUsers = udAction.getAuthorizedUsers();
		List<String> authorizedRoles = udAction.getAuthorizedRoles();

		// Check ROLES
		boolean hasDesiredRole = false;
		for (String role : authorizedRoles)
		{
			if (req.isUserInRole(role))
			{
				hasDesiredRole = true;
				isAuthorized = true;
				_logger.info("UserDefinedAction '" + udAction.getName() + "' and the user '" + userName + "' was authenticated as role '" + role + "'.");
			}
		}

		// If we HAS the correct ROLE, then possibly check for specific users.
		if (hasDesiredRole && !authorizedUsers.isEmpty())
		{
			// Check USERS
			if ( ! authorizedUsers.contains(userName) )
			{
				isAuthorized = false;
				_logger.info("UserDefinedAction '" + udAction.getName() + "'. We have the desired ROLE, but the user '" + userName + "' was NOT part of the '" + UserDefinedActionAbstract.PROPKEY_authorizedUsers+ "' " + authorizedUsers);
			}
		}

		if ( ! isAuthorized)
		{
			_logger.info("UserDefinedAction '" + udAction.getName() + "' and the user '" + userName + "' was NOT authenticated.");
		}

		return isAuthorized;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
	throws ServletException, IOException
	{
		resp.setContentType("text/html;charset=UTF-8");
		PrintWriter out = resp.getWriter();
//		ServletOutputStream out = resp.getOutputStream();
//		resp.setContentType("text/html");
//		resp.setCharacterEncoding("UTF-8");

		// Get generic input parameters
		String name      = Helper.getParameter(req, "name"    ,"");
//		String srvName   = Helper.getParameter(req, "srvName" ,"");
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
//		IUserDefinedAction udAction = UserDefinedActionManager.getInstance().getAction(name, srvName);
		IUserDefinedAction udAction = UserDefinedActionManager.getInstance().getAction(name);
		if (udAction == null)
		{
//			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No User Defined Action with the name='" + name + "', srvName='" + srvName + "'.");
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No User Defined Action with the name='" + name + "'.");
			return;
		}

		
		// Check for Authorization
//		String currentUsername = "-no-principal-";
//
//		Principal principal = req.getUserPrincipal();
//		if (principal != null)
//			currentUsername = principal.getName();

//		String from = "from getRemoteHost='" + req.getRemoteHost() + "', currentUsername='" + currentUsername + "', by user '" + req.getRemoteUser() + "', UserAgent='" + req.getHeader("User-Agent") + "'.";

		//-----------------------------------------
		// Check Authorization
		//-----------------------------------------
		String userName = req.getRemoteUser();
		if (StringUtil.isNullOrBlank(userName))
		{
			_logger.warn("Unauthorized UserDefinedAction of for '" + udAction.getName() + "' detected from '" + req.getRemoteAddr() + "' [" + StringUtil.getHostnameWithDomain(req.getRemoteAddr()) + "], UserAgent='" + req.getHeader("User-Agent") + "'.");
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not logged in when trying to execute User Defined Action '" + udAction.getName() + "'. Please login.");
			return;
		}

		if ( ! isAuthorized(req, udAction) )
		{
			// Write information about this in the log
			_logger.warn("Unauthorized UserDefinedAction of for '" + udAction.getName() + "' detected from '" + req.getRemoteAddr() + "' [" + StringUtil.getHostnameWithDomain(req.getRemoteAddr()) + "], UserAgent='" + req.getHeader("User-Agent") + "'.");
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not Authorized to execute User Defined Action '" + udAction.getName() + "'. Please login with an authorized user.");
			return;
		}
		
		// Check for known input parameters
		String[] udcKnownParams = udAction.getKnownParameters();
		if (Helper.hasUnKnownParameters(req, resp, ArrayUtils.addAll(knownParams, udcKnownParams)))
			return;
		

		// Get KNOWN Parameters for the User Defined Action producer
		udAction.setUrlParameters(Helper.getParameterMap(req));
		try 
		{
			udAction.checkUrlParameters(udAction.getUrlParameters());
		}
		catch (Exception ex) 
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
			return;
		}
		
		udAction.setPageRefreshTime(refresh);
		


////		String username = System.getProperty("user.name");
////		String hostname = InetAddress.getLocalHost().getHostName();
//		
//		out.println("<!DOCTYPE html>");
//		out.println("<html>");
//		out.println("<head>");
//
//		out.println("<title>" + udAction.getOnServerName() + " - " + udAction.getName() + "</title> ");
//		
//		if (refresh > 0)
//			out.println("<meta http-equiv='refresh' content='"+refresh+"' />");
//
//		out.println(HtmlStatic.getUserDefinedContentHead());
//		
////		out.println("<style type='text/css'>");
////		out.println("  table {border-collapse: collapse;}");
////		out.println("  th {border: 1px solid black; text-align: left; padding: 2px; white-space: nowrap; background-color:gray; color:white;}");
////		out.println("  td {border: 1px solid black; text-align: left; padding: 2px; white-space: nowrap; }");
////		out.println("  tr:nth-child(even) {background-color: #f2f2f2;}");
//////		out.println("  .topright { position: absolute; top: 8px; right: 16px; font-size: 14px; }"); // topright did not work with bootstrap (and navigation bar) 
////		out.println("</style>");
//
//		out.println("<style type='text/css'>");
//		out.println("    /* The below data-tooltip is used to show Actual exected SQL Text, as a tooltip where a normalized text is in a table cell */ ");
//		out.println("    [data-tooltip] { ");
//		out.println("        position: relative; ");
//		out.println("    } ");
//		out.println();
//		out.println("    /* 'tooltip' CSS settings for SQL Text... */ ");
//		out.println("    [data-tooltip]:hover::before { ");
//		out.println("        content: attr(data-tooltip);		 ");
////		out.println("        content: 'Click to Open Text Dialog...'; ");
//		out.println("        position: absolute; ");
//		out.println("        z-index: 103; ");
//		out.println("        top: 20px; ");
//		out.println("        left: 30px; ");
//		out.println("        width: 1000px; ");
//		out.println("        height: 900px; ");
////		out.println("        width: 220px; ");
//		out.println("        padding: 10px; ");
//		out.println("        background: #454545; ");
//		out.println("        color: #fff; ");
//		out.println("        font-size: 11px; ");
//		out.println("        font-family: Courier; ");
//		out.println("        white-space: pre-wrap; ");
//		out.println("    } ");
//		out.println("    [data-title]:hover::after { ");
//		out.println("        content: ''; ");
//		out.println("        position: absolute; ");
//		out.println("        bottom: -12px; ");
//		out.println("        left: 8px; ");
//		out.println("        border: 8px solid transparent; ");
//		out.println("        border-bottom: 8px solid #000; ");
//		out.println("    } ");
//		out.println();
////		out.println("    table, th, td { ");
////		out.println("    parameter-description, parameter-description th, parameter-description td { ");
//		out.println("    table.parameter-description th, ");
//		out.println("    table.parameter-description td { ");
//		out.println("        border: 1px solid black; ");
//		out.println("        border-collapse: collapse; ");
//		out.println("        padding: 5px; ");
//		out.println("    } ");
//		out.println("</style>");
//
//		out.println("</head>");
//		
//		out.println("<body onload='updateLastUpdatedClock()'>");
//
//		out.println(HtmlStatic.getUserDefinedContentNavbar());
//
//		out.println("<script>");
//		out.println("function updateLastUpdatedClock() {                   ");
//		out.println("    var ageInSec = Math.floor((new Date() - lastUpdateTime) / 1000);");
//		out.println("    document.getElementById('last-update-ts').innerHTML = ageInSec + ' seconds ago'; ");
////		out.println("    console.log('updateLastUpdatedClock(): ' + document.getElementById('last-update-ts'));");
////		out.println("    console.log('updateLastUpdatedClock(): ' + ageInSec + ' seconds ago');");
//		out.println("    setTimeout(updateLastUpdatedClock, 1000); ");
//		out.println("}                                                     ");
//		out.println("var lastUpdateTime = new Date();");
//		out.println("</script>");
//		out.println("");
//		
//		for (String cssLocation : udAction.getCssList())
//		{
//			out.println("<link rel='stylesheet' href='" + cssLocation + "'>");
//		}
//
//		for (String scriptLocation : udAction.getJavaScriptList())
//		{
//			out.println("<script type='text/javascript' src='" + scriptLocation + "'></script>");
//		}
//
//		out.println("<div class='container-fluid'>");
////		out.println("<div class='container mt-5'>");
//
////		String ver = "Version: " + Version.getVersionStr() + ", Build: " + Version.getBuildStr();
////		out.println("<h1>DbxTune - Central - " + username + "@" + hostname + "</h1>");
////		out.println("<div class='topright'>"+ver+"</div>");
//
//		// Create a "drop down section" where we will have
//		// - When was the page loaded
//		// - And various User Defined Information content
//		out.println("<details>");
//		out.println("<summary>");
//		out.println("Show parameters, Page loaded: <span id='last-update-ts'>" + (new Timestamp(System.currentTimeMillis())) + "</span>, ");
//		if (refresh > 0)
//			out.println("This page will 'auto-refresh' every " + refresh + " second. This can be changed with URL parameter 'refresh=##' (where ## is seconds)<br>" );
//		else
//			out.println("To 'auto-refresh' this page every ## second. This can be changed with URL parameter 'refresh=##' (where ## is seconds)<br>" );
//		out.println("</summary>");
//		udAction.createInfoContent(out);
//		out.println("</details>");
//
//		out.println("<div id='ud-content'>");
//
//		out.println("<h3 class='mb-4'>" + udAction.getActionType() + " Action: " + udAction.getName() + "</h2>");
//		out.println("<div id='dbx-uda-output'></div>"); // We will ADD stuff in here via JavaScript
//
//		//-------------------------------------------------------
//		// BEGIN: Produce the User Defined Content
//		//-------------------------------------------------------
//		try
//		{
//			// Execute the User Defined Logic
////			udAction.produce(out);
//			udAction.createContent(out);
//		}
//		catch (Exception ex)
//		{
//			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error when creating User Defined Action. Caught: " + ex.getMessage());
//			return;
//		}
//		//-------------------------------------------------------
//		// END: Produce the User Defined Content
//		//-------------------------------------------------------
//		
//		out.println("</div>"); // ud-content
//		
//		out.println("</div>"); // container-fluid
//
//		// Write some JavaScript code
//		out.println(HtmlStatic.getJavaScriptAtEnd(true));
//
//		out.println("</body>");
//		out.println("</html>");

		//-------------------------------------------------------
		// BEGIN: Produce the User Defined Content
		//-------------------------------------------------------
		try
		{
			udAction.produce(out);
		}
		catch (Exception ex)
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error when creating User Defined Action. Caught: " + ex.getMessage());
			return;
		}
		//-------------------------------------------------------
		// END: Produce the User Defined Content
		//-------------------------------------------------------
		
		out.flush();
		out.close();

	} // end: doGet
}
