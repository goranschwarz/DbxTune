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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.controllers.HtmlStatic.PageSection;
import com.dbxtune.central.llm.LlmClientRegistry;
import com.dbxtune.utils.HtmlUtils;
import com.dbxtune.utils.StringUtil;


public class ShowplanSqlServerServlet 
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		resp.setContentType("text/html; charset=UTF-8");
		resp.setCharacterEncoding("UTF-8");
		PrintWriter out = resp.getWriter();
		out.print(createPasteFormOutput());
		out.flush();
		out.close();
	}

	/**
	 * The paste-and-submit form shown for a plain {@code GET /showplan/sqlserver} - shares the same
	 * navbar/head chrome as {@link #createShowplanOutput(String, String, String)} so landing on this
	 * page and viewing a submitted plan look like the same section, not two different pages.
	 */
	public static String createPasteFormOutput()
	{
		String str = "" +
				"<!DOCTYPE html> \n" +
				"<html lang='en'> \n" +
				" \n" +
				"<head> \n" +
				"    <meta charset='UTF-8'> \n" +
				"    <meta name='viewport' content='width=device-width, initial-scale=1'> \n" +
				"    <title>DbxTune - SQL Server Showplan</title> \n" +
				"    <meta name='robots' content='max-image-preview:large' /> \n" +
				" \n" +
				HtmlStatic.getUserDefinedContentHead() +
				"</head> \n" +
				" \n" +
				"<body> \n" +
				HtmlStatic.getHtmlNavbar(PageSection.None, "<li class='nav-item'><a class='nav-link' href='/showplan/'>All Showplan Viewers</a></li>", true) +
				"    <div class='container-fluid px-4 py-3' style='max-width: 900px;'> \n" +
				"      <h2>SQL Server Showplan Viewer</h2> \n" +
				"      <p class='text-muted'>Paste an XML execution plan below and press Submit.</p> \n" +
				"      <div class='card shadow-sm'> \n" +
				"        <div class='card-body'> \n" +
				"          <form id='showplan-form' action='/showplan/sqlserver' method='post'> \n" +
				"            <textarea class='form-control mb-3' id='plan' name='plan' rows='20' placeholder='Paste the XML Showplan here...'></textarea> \n" +
				"            <button type='submit' class='btn btn-primary'>Submit</button> \n" +
				"          </form> \n" +
				"        </div> \n" +
				"      </div> \n" +
				"    </div> \n" +
				" \n" +
				HtmlStatic.getJavaScriptAtEnd(true) +
				"</body> \n" +
				" \n" +
				"</html> \n" +
				"";
		return str;
	}

	/**
	 * Take a STRING input and build up a SHOWPLAN page
	 */
	// curl -X POST -d @/mnt/c/tmp/Alarm.tmp.json http://localhost:8080/pev
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String remoteHost = request.getRemoteHost();
		String remoteAddr = request.getRemoteAddr();
		int    remotePort = request.getRemotePort();
		String remoteUser = request.getRemoteUser();
		
		String plan     = request.getParameter("plan");
		String sql      = request.getParameter("sql");
		String dbVendor = request.getParameter("dbVendor");

//System.out.println("PLAN=|" + plan + "|");

		// Parse the Query String
//		Map<String, String> queryStringMap = HtmlQueryString.parseQueryString(req.getQueryString());

		// Check QueryString for various parameters
//		boolean keepHtml = StringUtil.equalsAnyIgnoreCase(queryStringMap.getOrDefault("keepHtml", "false"), "", "true");;
//		boolean logInput = StringUtil.equalsAnyIgnoreCase(queryStringMap.getOrDefault("logInput", "false"), "", "true");;

		if (_logger.isDebugEnabled())
			_logger.debug("/showplan/sqlserver: received request from: remoteHost='" + remoteHost + "', remoteAddr='" + remoteAddr + "', remotePort='" + remotePort + "', remoteUser='" + remoteUser + "'.");

		// Get the JSON String
//		String payload = getBody(req);
		String payload = plan;
		if (StringUtil.isNullOrBlank(payload))
		{
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Expecting a SQL Server XML Execution Plan as Payload, but an empty string was sent.");
			return;
		}
//System.out.println("Received Payload=|" + payload + "|.");

		// Parse the JSON String and return a formatted message
		String formattedOutput = "";
		formattedOutput = createShowplanOutput( payload, sql, dbVendor );


		response.setContentType("text/html; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();
		out.print(formattedOutput);

		// Send the formatted text to caller
//		ServletOutputStream out = response.getOutputStream();
//		out.print(formattedOutput.getBytes("UTF-8"));
		out.flush();
		out.close();
	}

	
	public static String createShowplanOutput(String payload)
	{
		return createShowplanOutput(payload, null, null);
	}

	/**
	 * @param payload  the XML execution plan
	 * @param sql      the SQL statement this plan belongs to, or null if unknown - when present,
	 *                 a "Get LLM Optimization Advice" button is shown
	 * @param dbVendor DBMS product name for {@code sql}, or null
	 */
	public static String createShowplanOutput(String payload, String sql, String dbVendor)
	{
		boolean hasSql = StringUtil.hasValue(sql) && LlmClientRegistry.isFeatureEnabled();

		String str = "" +
				"<!DOCTYPE html> \n" +
				"<html lang='en'> \n" +
				" \n" +
				"<head> \n" +
				"    <meta charset='UTF-8'> \n" +
				"    <meta name='viewport' content='width=device-width, initial-scale=1'> \n" +
				"    <title>SQL Server Execution Plan Viewer</title> \n" +
                "     \n" +
				"    <meta http-equiv='Cache-Control' content='no-cache, no-store, must-revalidate' /> \n" +
				"    <meta http-equiv='Pragma' content='no-cache' /> \n" +
				"    <meta http-equiv='Expires' content='0' /> \n" +
				"     \n" +

				// Pulls in jQuery, Bootstrap 4.6.2 (CSS+JS), Font Awesome, dbxcentral.css,
				// dbxcentral.utils.js and dbxLoginModal.js - the same shared head/navbar/login-wiring
				// every other DbxCentral page uses (see LlmAdviceServlet for the identical pattern),
				// rather than this page rolling its own Bootstrap include and a one-off <nav>.
				HtmlStatic.getUserDefinedContentHead() +

				// Possibly use local install:
//				"    <script src='https://cdn.jsdelivr.net/npm/html-query-plan@2.6.1/dist/qp.min.js'></script> \n" +
//				"    <script src='https://www.dbxtune.com/sqlserver_showplan/dist/qp.js' type='text/javascript'></script> \n" +
//				"    <script src='/scripts/showplan/sqlserver/dist/qp.js' type='text/javascript'></script> \n" +
				HtmlUtils.createJsScriptTag("/scripts/showplan/sqlserver/dist/qp.js", "https://www.dbxtune.com/sqlserver_showplan/dist/qp.js") +

				// Possibly use local install:
//				"    <link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/html-query-plan@2.6.1/css/qp.min.css' /> \n" +
//				"    <link rel='stylesheet' type='text/css' href='https://www.dbxtune.com/sqlserver_showplan/css/qp.css'> \n" +
//				"    <link rel='stylesheet' type='text/css' href='/scripts/showplan/sqlserver/css/qp.css'> \n" +
				HtmlUtils.createCssLinkTag("/scripts/showplan/sqlserver/css/qp.css", "https://www.dbxtune.com/sqlserver_showplan/css/qp.css") +

				(hasSql
					? "    <script src='/scripts/marked/18.0.5/marked.min.js'></script> \n"
					+ "    <script src='/scripts/dompurify/3.4.11/purify.min.js'></script> \n"
					+ "    <script src='/scripts/dbxtune/js/dbxLlmAdvice.js'></script> \n"
					: "") +

				"    <style> #ss-showplan { overflow: auto; } </style> \n" +

				"</head> \n" +
				" \n" +
				"<body> \n" +
				HtmlStatic.getHtmlNavbar(PageSection.None, "<li class='nav-item'><a class='nav-link' href='/showplan/'>All Showplan Viewers</a></li>", true) +
				"    <div class='container-fluid px-4 py-3'> \n" +
				"      <h2>&#128202; SQL Server Execution Plan Viewer</h2> \n" +
				"      <div class='card shadow-sm'> \n" +
				"        <div class='card-body'> \n" +
				(hasSql
					? "          <button type='button' class='btn btn-primary mb-3' onclick='dbxLlmAdvice.open({sql: dbxLlmSql, plan: dbxLlmPlan, dbVendor: dbxLlmDbVendor});'>&#129302; Get LLM Optimization Advice</button> \n"
					: "") +
				"          <div id='ss-showplan'>                       \n" +
				"          </div>                                       \n" +
				"        </div> \n" +
				"      </div> \n" +
				"    </div> \n" +
				"                                                       \n" +
				"    <script>                                           \n" +
				"        const plan = '" + StringEscapeUtils.escapeEcmaScript(payload) + "'; \n" +
				(hasSql
					? "        const dbxLlmSql = '"      + StringEscapeUtils.escapeEcmaScript(sql) + "'; \n"
					+ "        const dbxLlmPlan = '"     + StringEscapeUtils.escapeEcmaScript(payload) + "'; \n"
					+ "        const dbxLlmDbVendor = '" + StringEscapeUtils.escapeEcmaScript(StringUtil.nullToValue(dbVendor, "")) + "'; \n"
					: "") +
				"                                                       \n" +
				"        QP.showPlan(document.getElementById('ss-showplan'), '" + StringEscapeUtils.escapeEcmaScript(payload) + "'); \n" +
				"    </script>		                                    \n" +
				HtmlStatic.getJavaScriptAtEnd(true) +
				"</body>                                                \n" +
				"";
		return str;
	}
}
