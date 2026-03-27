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
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.text.StringEscapeUtils;
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
		String reason    = Helper.getParameter(req, "reason"  ,"");

		String[] knownParams = new String[] {"name", "srvName", "refresh", "reason"};
		
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
			renderLoginRequired(out, udAction);
			out.flush();
			out.close();
			return;
		}

		if ( ! isAuthorized(req, udAction) )
		{
			// Write information about this in the log
			_logger.warn("Unauthorized UserDefinedAction of for '" + udAction.getName() + "' detected from '" + req.getRemoteAddr() + "' [" + StringUtil.getHostnameWithDomain(req.getRemoteAddr()) + "], UserAgent='" + req.getHeader("User-Agent") + "'.");
			renderNotAuthorized(out, udAction, userName);
			out.flush();
			out.close();
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
		udAction.setExecutedByUser(userName);

		// If this action requires a reason and none was supplied, show the reason-entry form
		if (udAction.isReasonMessageRequired() && StringUtil.isNullOrBlank(reason))
		{
			renderReasonForm(out, udAction, refresh);
			out.flush();
			out.close();
			return;
		}
		udAction.setExecutionReason(reason);


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

	/**
	 * Renders a Bootstrap-styled "reason required" form page.<br>
	 * When submitted the browser re-calls the same servlet with {@code reason=...} appended,
	 * so the action executes normally on the second pass.
	 */
	private void renderReasonForm(PrintWriter out, IUserDefinedAction udAction, int refresh)
	{
		String safeName = StringEscapeUtils.escapeHtml4(udAction.getName());
		String safeDesc = StringUtil.hasValue(udAction.getDescription()) ? StringEscapeUtils.escapeHtml4(udAction.getDescription()) : "";

		out.println("<!DOCTYPE html>");
		out.println("<html>");
		out.println("<head>");
		out.println("  <title>Reason Required \u2014 " + safeName + "</title>");
		out.println(HtmlStatic.getUserDefinedContentHead());
		out.println("</head>");
		out.println("<body>");
		out.println(HtmlStatic.getUserDefinedContentNavbar());

		out.println("<div class='container mt-5' style='max-width:640px;'>");
		out.println("  <div class='card border-warning shadow-sm'>");
		out.println("    <div class='card-header bg-warning text-dark'>");
		out.println("      <h5 class='mb-0'>&#9888; Reason Required &mdash; Execute Action: <strong>" + safeName + "</strong></h5>");
		out.println("    </div>");
		out.println("    <div class='card-body'>");

		if (StringUtil.hasValue(safeDesc))
			out.println("      <p class='text-muted mb-3'>" + safeDesc + "</p>");

		out.println("      <p>This action requires a reason before it can be executed.</p>");

		out.println("      <form method='get' action='/api/udaction'>");
		out.println("        <input type='hidden' name='name' value='" + safeName + "'>");
		if (refresh > 0)
			out.println("        <input type='hidden' name='refresh' value='" + refresh + "'>");

		out.println("        <div class='form-group'>");
		out.println("          <label for='uda-reason'><strong>Reason for Execution:</strong></label>");
		out.println("          <textarea class='form-control mt-1' id='uda-reason' name='reason'");
		out.println("                    rows='4' required autofocus");
		out.println("                    placeholder='Enter your reason for executing this action...'></textarea>");
		out.println("        </div>");
		out.println("        <button type='submit' class='btn btn-warning'>Execute Action</button>");
		out.println("        <a href='javascript:history.back()' class='btn btn-secondary ml-2'>Cancel</a>");
		out.println("      </form>");

		out.println("    </div>"); // card-body
		out.println("  </div>"); // card
		out.println("</div>"); // container

		out.println(HtmlStatic.getJavaScriptAtEnd(true));
		out.println("</body>");
		out.println("</html>");
	}

	/**
	 * Renders a Bootstrap-styled "login required" page.<br>
	 * The page includes {@code dbxLoginModal.js} (via HtmlStatic head) which will open
	 * the login modal automatically.  After a successful login Jetty redirects to {@code /},
	 * and {@code dbxLoginModal.js} reads {@code sessionStorage.dbxReturnAfterLogin} to
	 * bring the user back to the original action URL.
	 */
	private void renderLoginRequired(PrintWriter out, IUserDefinedAction udAction)
	{
		String safeName = StringEscapeUtils.escapeHtml4(udAction.getName());
		String safeDesc = StringUtil.hasValue(udAction.getDescription()) ? StringEscapeUtils.escapeHtml4(udAction.getDescription()) : "";

		out.println("<!DOCTYPE html>");
		out.println("<html>");
		out.println("<head>");
		out.println("  <title>Login Required \u2014 " + safeName + "</title>");
		out.println(HtmlStatic.getUserDefinedContentHead());
		out.println("</head>");
		out.println("<body>");
		out.println(HtmlStatic.getUserDefinedContentNavbar());

		out.println("<div class='container mt-5' style='max-width:640px;'>");
		out.println("  <div class='card border-primary shadow-sm'>");
		out.println("    <div class='card-header bg-primary text-white'>");
		out.println("      <h5 class='mb-0'>&#128274; Login Required &mdash; Execute Action: <strong>" + safeName + "</strong></h5>");
		out.println("    </div>");
		out.println("    <div class='card-body'>");

		if (StringUtil.hasValue(safeDesc))
			out.println("      <p class='text-muted mb-3'>" + safeDesc + "</p>");

		out.println("      <p>You must be logged in to execute this action.</p>");
		out.println("      <button class='btn btn-primary' onclick='dbxOpenLogin();'>Login</button>");
		out.println("      <a href='javascript:history.back()' class='btn btn-secondary ml-2'>Cancel</a>");

		out.println("    </div>"); // card-body
		out.println("  </div>"); // card
		out.println("</div>"); // container

		// On page-load: open the login modal automatically.
		// dbxOpenLogin() will save window.location.href (= the udaction URL) to
		// sessionStorage.dbxReturnAfterLogin so the user is brought back here after login.
		out.println("<script>");
		out.println("$(document).ready(function() {");
		out.println("  if (typeof dbxOpenLogin === 'function') { dbxOpenLogin(); }");
		out.println("});");
		out.println("</script>");

		out.println(HtmlStatic.getJavaScriptAtEnd(true));
		out.println("</body>");
		out.println("</html>");
	}

	/**
	 * Renders a Bootstrap-styled "not authorized" page.<br>
	 * Shows who the current user is, which roles/users are allowed, and offers
	 * a "Login as different user" button that opens the login modal.
	 */
	private void renderNotAuthorized(PrintWriter out, IUserDefinedAction udAction, String currentUser)
	{
		String safeName = StringEscapeUtils.escapeHtml4(udAction.getName());
		String safeDesc = StringUtil.hasValue(udAction.getDescription()) ? StringEscapeUtils.escapeHtml4(udAction.getDescription()) : "";
		String safeUser = StringEscapeUtils.escapeHtml4(currentUser);

		List<String> authorizedRoles = udAction.getAuthorizedRoles();
		List<String> authorizedUsers = udAction.getAuthorizedUsers();

		out.println("<!DOCTYPE html>");
		out.println("<html>");
		out.println("<head>");
		out.println("  <title>Not Authorized \u2014 " + safeName + "</title>");
		out.println(HtmlStatic.getUserDefinedContentHead());
		out.println("</head>");
		out.println("<body>");
		out.println(HtmlStatic.getUserDefinedContentNavbar());

		out.println("<div class='container mt-5' style='max-width:640px;'>");
		out.println("  <div class='card border-danger shadow-sm'>");
		out.println("    <div class='card-header bg-danger text-white'>");
		out.println("      <h5 class='mb-0'>&#128683; Not Authorized &mdash; Execute Action: <strong>" + safeName + "</strong></h5>");
		out.println("    </div>");
		out.println("    <div class='card-body'>");

		if (StringUtil.hasValue(safeDesc))
			out.println("      <p class='text-muted mb-3'>" + safeDesc + "</p>");

		out.println("      <p>You are logged in as <strong>" + safeUser + "</strong> but are not authorized to execute this action.</p>");

		if (!authorizedRoles.isEmpty())
		{
			out.println("      <p class='mb-1'><strong>Required role(s):</strong></p>");
			out.println("      <ul>");
			for (String role : authorizedRoles)
				out.println("        <li>" + StringEscapeUtils.escapeHtml4(role) + "</li>");
			out.println("      </ul>");
		}

		if (!authorizedUsers.isEmpty())
		{
			out.println("      <p class='mb-1'><strong>Authorized user(s):</strong></p>");
			out.println("      <ul>");
			for (String user : authorizedUsers)
				out.println("        <li>" + StringEscapeUtils.escapeHtml4(user) + "</li>");
			out.println("      </ul>");
		}

		out.println("      <button class='btn btn-warning' onclick='dbxOpenLogin();'>Login as Different User</button>");
		out.println("      <a href='javascript:history.back()' class='btn btn-secondary ml-2'>Cancel</a>");

		out.println("    </div>"); // card-body
		out.println("  </div>"); // card
		out.println("</div>"); // container

		out.println(HtmlStatic.getJavaScriptAtEnd(true));
		out.println("</body>");
		out.println("</html>");
	}
}
