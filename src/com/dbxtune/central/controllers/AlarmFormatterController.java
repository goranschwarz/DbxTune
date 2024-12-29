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
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.dbxtune.alarm.writers.AlarmWriterToMail;
import com.dbxtune.alarm.writers.WriterUtils;
import com.dbxtune.central.pcs.DbxTuneSample;
import com.dbxtune.central.pcs.DbxTuneSample.AlarmEntry;
import com.dbxtune.central.pcs.DbxTuneSample.MissingFieldException;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.HtmlQueryString;
import com.dbxtune.utils.StringUtil;


public class AlarmFormatterController 
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

	
	public static final String  PROPKEY_template = "AlarmFormatter.template";
	public static final String  DEFAULT_template = ""; // If not specified use: AlarmWriterToMail.PROPKEY_msgBodyTemplate, AlarmWriterToMail.DEFAULT_msgBodyTemplate
	
//	/**
//	 * Take a JSON input and using a HTML Template to get a formatted Alarm Message (that can be used "somewhere")
//	 */
//	@Override
//	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
//	{
////		JsonObject data = new Gson().fromJson(request.getReader(), JsonObject.class);
//	}
	public static final String PROPKEY_HOSTS_ALLOWED = "AlarmFormatter.hosts.allowed";
	public static final String DEFAULT_HOSTS_ALLOWED = "";

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

	/**
	 * Take a JSON input and using a HTML Template to get a formatted Alarm Message (that can be used "somewhere")
	 */
	// curl -X POST -d @/mnt/c/tmp/Alarm.tmp.json http://localhost:8080/api/alarm/formatter
	@SuppressWarnings("deprecation")
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		String remoteHost = req.getRemoteHost();
		String remoteAddr = req.getRemoteAddr();
		int    remotePort = req.getRemotePort();
		String remoteUser = req.getRemoteUser();

		// Parse the Query String
		Map<String, String> queryStringMap = HtmlQueryString.parseQueryString(req.getQueryString());

		// Check QueryString for various parameters
		boolean keepHtml = StringUtil.equalsAnyIgnoreCase(queryStringMap.getOrDefault("keepHtml", "false"), "", "true");;
		boolean logInput = StringUtil.equalsAnyIgnoreCase(queryStringMap.getOrDefault("logInput", "false"), "", "true");;

		if (_logger.isDebugEnabled())
			_logger.debug("/api/alarm/formatter: received request from: remoteHost='" + remoteHost + "', remoteAddr='" + remoteAddr + "', remotePort='" + remotePort + "', remoteUser='" + remoteUser + "'.");

		// Check if the remote host is allowed to send data.
		String allowedHosts = Configuration.getCombinedConfiguration().getProperty(PROPKEY_HOSTS_ALLOWED, DEFAULT_HOSTS_ALLOWED);
		if (StringUtil.hasValue(allowedHosts))
		{
			List<String> allowedHostList = StringUtil.commaStrToList(allowedHosts);

			// Check all entries in the allowedHostList using regex 
			boolean isAllowed = false;
			for (String regex : allowedHostList)
			{
				if (remoteAddr.matches(regex))
					isAllowed = true;
			}
			// If no match, then do NOT allow the hosts to enter data
			if ( ! isAllowed )
			{
				_logger.warn("The hostname '" + remoteHost + "' is NOT allowed to Format Alarm Data. allowedHostList=" + allowedHostList);
				resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "The hostname '" + remoteHost + "' is NOT allowed to Format Alarm Data.");
				return;
			}
		}

		// Get the JSON String
		String payload = getBody(req);
		if (StringUtil.isNullOrBlank(payload))
		{
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Expecting a JSON Payload, but an empty string was sent.");
			return;
		}
		if (logInput)
		{
			System.out.println("Received JSON Payload=|" + payload + "|.");
			_logger.info("Received JSON Payload=|" + payload + "|.");
		}

		// Parse the JSON String and return a formatted message
		String formattedOutput = "";
		try
		{
			formattedOutput = parseJsonAndFormatMessage( payload );
		}
		catch (MissingFieldException ex) 
		{
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "MissingFieldException: " + ex.getMessage());
			//throw new ServletException(ex.getMessage(), ex);
		}
		catch (IOException ex) 
		{
			throw new ServletException("Problem parsing incoming JSON message for AlarmEntry", ex);
		}
		
		// Remove some HTML TAGS... '<html>' & '</html>' and everything between '<head>...</head>'
		if (keepHtml == false && formattedOutput.startsWith("<html>"))
		{
			formattedOutput = formattedOutput.trim();
			formattedOutput = formattedOutput.substring("<html>".length(), formattedOutput.length()-"</html>".length()).trim();

			// Remove "head" tag
			formattedOutput = StringUtil.removeBetweenIgnoreCase(formattedOutput, "<head>", "</head>", true);
		}
		
		// Send the formatted text to caller
		ServletOutputStream out = resp.getOutputStream();
		out.print(formattedOutput);
		out.flush();
		out.close();
	}


	/**
	 * Parse JSON String into a DbxTuneSample Object and post that to CentralPcsWriterHandler, which writes it to all PCS Writers
	 * @param jsonPayload
	 * @throws ServletException 
	 */
	public static String parseJsonAndFormatMessage(String jsonPayload) 
	throws MissingFieldException, IOException
	{
//System.out.println("JSON PAYLOAD=|" + jsonPayload+ "|.");
		// Parse JSON
		AlarmEntry alarmEntry = DbxTuneSample.parseJsonAlarmEntry(jsonPayload);
//System.out.println("ALARM_ENTRY=|" + alarmEntry + "|.");

		// Get what template to use for the message... If not a local was found. use: AlarmWriterToMail.PROPKEY_msgBodyTemplate
		String msgTemplate = Configuration.getCombinedConfiguration().getProperty(PROPKEY_template, DEFAULT_template);
		if (StringUtil.isNullOrBlank(msgTemplate))
			msgTemplate = Configuration.getCombinedConfiguration().getProperty(AlarmWriterToMail.PROPKEY_msgBodyTemplate, AlarmWriterToMail.DEFAULT_msgBodyTemplate);

		String dbxCentralUrl = ""; // getDbxCentralUrl();

		// Transform Alarm into a Formatted Message
		String action = alarmEntry.isActive() ? "RAISE" : "CANCEL";
		String msg = WriterUtils.createMessageFromTemplate(action, alarmEntry, msgTemplate, true, null, dbxCentralUrl);
		
//System.out.println("formattedOutput=|" + msg+ "|.");

		return msg;
	}
}
