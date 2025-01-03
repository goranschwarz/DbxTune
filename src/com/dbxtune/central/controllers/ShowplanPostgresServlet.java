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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.utils.StringUtil;


public class ShowplanPostgresServlet 
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		resp.sendRedirect(req.getContextPath() + "/showplan/postgres.html");
	}

	/**
	 * Take a STRING input and build up a PEV2 page
	 */
	// curl -X POST -d @/mnt/c/tmp/Alarm.tmp.json http://localhost:8080/pev
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String remoteHost = request.getRemoteHost();
		String remoteAddr = request.getRemoteAddr();
		int    remotePort = request.getRemotePort();
		String remoteUser = request.getRemoteUser();
		
		String plan = request.getParameter("plan");

//System.out.println("PLAN=|" + plan + "|");

		// Parse the Query String
//		Map<String, String> queryStringMap = HtmlQueryString.parseQueryString(req.getQueryString());

		// Check QueryString for various parameters
//		boolean keepHtml = StringUtil.equalsAnyIgnoreCase(queryStringMap.getOrDefault("keepHtml", "false"), "", "true");;
//		boolean logInput = StringUtil.equalsAnyIgnoreCase(queryStringMap.getOrDefault("logInput", "false"), "", "true");;

		if (_logger.isDebugEnabled())
			_logger.debug("/showplan/postgres: received request from: remoteHost='" + remoteHost + "', remoteAddr='" + remoteAddr + "', remotePort='" + remotePort + "', remoteUser='" + remoteUser + "'.");

		// Get the JSON String
//		String payload = getBody(req);
		String payload = plan;
		if (StringUtil.isNullOrBlank(payload))
		{
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Expecting a Postgres Explain Plan as Payload, but an empty string was sent.");
			return;
		}
//System.out.println("Received Payload=|" + payload + "|.");

		// Parse the JSON String and return a formatted message
		String formattedOutput = "";
		formattedOutput = createShowplanOutput( payload );


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
		String str = "" +
				"<!DOCTYPE html> \n" +
				"<html lang='en'> \n" +
				" \n" +
				"<head> \n" +
				"    <meta charset='UTF-8'> \n" +
				"    <title>Postgres Execution Plan Viewer -- PEV2</title> \n" +
                "     \n" +
				"    <meta http-equiv='Cache-Control' content='no-cache, no-store, must-revalidate' /> \n" +
				"    <meta http-equiv='Pragma' content='no-cache' /> \n" +
				"    <meta http-equiv='Expires' content='0' /> \n" +
				"     \n" +
				"    <script src='https://unpkg.com/vue@3.2.45/dist/vue.global.prod.js'></script>                 \n" +
				"    <script src='https://unpkg.com/pev2/dist/pev2.umd.js'></script>                              \n" +
				"    <link rel='stylesheet' href='https://unpkg.com/bootstrap@5.3.2/dist/css/bootstrap.min.css'/> \n" +
				"    <link rel='stylesheet' href='https://unpkg.com/pev2/dist/style.css' />                       \n" +
				"</head> \n" +
				" \n" +
				"<body> \n" +
				"    <div id='app'>                                     \n" +
				"      <pev2 style='min-height: 100vh;' :plan-source='plan' plan-query='' /> \n" +
				"    </div>                                             \n" +
				"                                                       \n" +
				"    <script>                                           \n" +
				"        const { createApp } = Vue;                     \n" +
				"                                                       \n" +
				"        const plan = '" + StringEscapeUtils.escapeEcmaScript(payload) + "'; \n" +
				"                                                       \n" +
				"        const app = createApp({                        \n" +
				"            data() {                                   \n" +
				"                return {                               \n" +
				"                    plan: plan                         \n" +
				"                }                                      \n" +
				"            },                                         \n" +
				"        });                                            \n" +
				"        app.component('pev2', pev2.Plan);              \n" +
				"        app.mount('#app');                             \n" +
				"    </script>		                                    \n" +
				"</body>                                                \n" +
				"";
		return str;
	}

	public static String getBody(HttpServletRequest request) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		BufferedReader br = null;

		try
		{
			InputStream inputStream = request.getInputStream();
			if ( inputStream != null )
			{
				br = new BufferedReader(new InputStreamReader(inputStream));
				char[] charBuffer = new char[128];
				int bytesRead = -1;
				while ((bytesRead = br.read(charBuffer)) > 0)
					sb.append(charBuffer, 0, bytesRead);
			}
			else
				sb.append("");
		}
		catch (IOException ex)
		{
			throw ex;
		}
		finally
		{
			if ( br != null )
			{
				try { br.close(); }
				catch (IOException ex) { throw ex; }
			}
		}

		return sb.toString();
	}

}
