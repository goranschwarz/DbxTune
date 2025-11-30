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
package com.dbxtune.mgt.controllers;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.central.controllers.Helper;
import com.dbxtune.central.pcs.DbxCentralRealm;
import com.dbxtune.central.pcs.DbxTuneSample;
import com.dbxtune.central.pcs.DbxTuneSample.MissingFieldException;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSettingsHelper.ValidationException;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NoGuiConfigSetServlet 
extends HttpServlet
//extends AbstractHandler
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

//	private boolean hasCorrectSecurityToken(HttpServletRequest request)
//	{
//		Configuration conf = Configuration.getInstance(DbxTune.DBXTUNE_NOGUI_INFO_CONFIG);
//		if (conf == null)
//		{
//			// FIXME;
//		}
//		if ( ! NoGuiManagementServer.hasInstance() )
//		{
//			// FIXME: send error
//		}
//		NoGuiManagementServer noGuiMngmntSrv = NoGuiManagementServer.getInstance();
//		String authTokenString = noGuiMngmntSrv.getAuthTokenString(false);
//		
//		// Get Header!
//		final String auth = request.getHeader( "Authorization" );
//System.out.println(">>>>>>>>>>>> NoGuiConfigSetServlet.hasCorrectSecurityToken(): Authorization = |" + auth + "|, noGuiMngmntSrv.getAuthTokenString()=|" + authTokenString + "|.");
//
//		boolean authorized = false;
//		if (auth != null && auth.equals(authTokenString))
//		{
//			authorized = true;
//		}
//
//		if ( ! authorized )
//		{
//			// FIXME: send error
//		}
//		return authorized;
//		
////		conf.getProperty(getServletInfo())
//		// If this is a Basic Authentication, we might want to do:
//		// - Generate a UUID (or similar random string), that we store in INFO file: ${HOME}/.dbxtune/info/SERVER_NAME.dbxtune
//		// - Possibly send that via the JSON (in PersistWriterToHttpJson)
//		// - Possibly store it in DbxCentral database (just like 'collectorMgtHostname' and 'collectorMgtPort'
////		return _xxxToken.equals(request.getParameter("password"));
//	}

	/**
	 * Send a JSON Response to the caller
	 * 
	 * @param response               The response object (can be null if you DO NOT want the JSON to be sent, but just return the JSON String) 
	 * @param httpErrorCode          The error code normally <code>HttpServletResponse.SC_OK</code> (200) 
	 * @param success                if the request succeeded or not
	 * @param infoMsg                Send info message in the JSON object
	 * @param errorMsg               Send error message in the JSON object
	 * @param collectorNeedsReboot   If the Collector needs to be rebooted after this change
	 * 
	 * @return A JSON String like: {"success":true|false, "info":"...", "error":"...", "isBootNeeded":true|false}
	 * @throws IOException
	 */
	private String sendJsonResponce(HttpServletResponse response, int httpErrorCode, boolean success, String infoMsg, String errorMsg, boolean collectorNeedsReboot)
	throws IOException
	{
		// Setup a JSON "writer"
		StringWriter sw = new StringWriter();
		JsonFactory jfactory = new JsonFactory();
		JsonGenerator gen = jfactory.createGenerator(sw);
		gen.setPrettyPrinter(new DefaultPrettyPrinter());
		gen.setCodec(new ObjectMapper(jfactory));

		// Return JSON Object
		gen.writeStartObject();
		gen.writeBooleanField("success"      , success);
//		gen.writeStringField ("status"      , statusMessage);

		if (StringUtil.hasValue(infoMsg))  
			gen.writeStringField ("info" , infoMsg);

		if (StringUtil.hasValue(errorMsg)) 
			gen.writeStringField ("error", errorMsg);
		
		gen.writeBooleanField("isBootNeeded", collectorNeedsReboot);
		gen.writeEndObject();
		gen.close();
		
		// Generate PAYLOAD
		String payload = sw.toString();

		// If we have passed a 'response' object
		if (response != null)
		{
			if (httpErrorCode != HttpServletResponse.SC_OK)
			{
				String respMsg = "";
				if (StringUtil.hasValue(infoMsg))  respMsg += "info: '" + infoMsg   + "'. ";
				if (StringUtil.hasValue(errorMsg)) respMsg += "error: '" + errorMsg + "'. ";

				response.sendError(httpErrorCode, respMsg);
			}
			else
			{
				response.sendError(httpErrorCode);
			}

			// Set content type etc...
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			response.setHeader("Access-Control-Allow-Origin", "*");

			// Send JSON payload
			ServletOutputStream out = response.getOutputStream();
			out.println(payload);
		}
		
		return payload;
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
//		resp.setContentType("text/html");
//		resp.setCharacterEncoding("UTF-8");
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		resp.setHeader("Access-Control-Allow-Origin", "*");
		ServletOutputStream out = resp.getOutputStream();

		// Check for known input parameters
		if (Helper.hasUnKnownParameters(req, resp, "cmName", "alarmName", "key", "val"))
			return;

//		String sessionName    = Helper.getParameter(req, new String[] {"sessionName", "srv", "srvName"} );
		String cmName         = Helper.getParameter(req, "cmName");
		String optName        = Helper.getParameter(req, "alarmName");
		String propertyName   = Helper.getParameter(req, "key");
		String propertyValue  = Helper.getParameter(req, "val");
//		String validatorName  = Helper.getParameter(req, "validator");

System.out.println();
System.out.println(">>>>>>>>>>>> NoGuiConfigSetServlet.PUT: getContextPath()=" + req.getContextPath());
System.out.println(">>>>>>>>>>>> NoGuiConfigSetServlet.PUT: getPathInfo   ()=" + req.getPathInfo());
System.out.println(">>>>>>>>>>>> NoGuiConfigSetServlet.PUT: getQueryString()=" + req.getQueryString());
System.out.println(">>>>>>>>>>>> NoGuiConfigSetServlet.PUT: getParameterMap()=" + req.getParameterMap());

System.out.println(">>>>>>>>>>>> NoGuiConfigSetServlet.PUT.PARAMS: cmName   = |" + cmName        + "|.");
System.out.println(">>>>>>>>>>>> NoGuiConfigSetServlet.PUT.PARAMS: alarmName= |" + optName     + "|.");
System.out.println(">>>>>>>>>>>> NoGuiConfigSetServlet.PUT.PARAMS: key      = |" + propertyName  + "|.");
System.out.println(">>>>>>>>>>>> NoGuiConfigSetServlet.PUT.PARAMS: val      = |" + propertyValue + "|.");

		final String auth = req.getHeader( "Authorization" );
System.out.println(">>>>>>>>>>>> NoGuiConfigSetServlet.PUT.HEADER: Authorization = |" + auth + "|.");

		boolean hasAdminRole = false;
		String currentUsername = "-no-principal-";

		Principal principal = req.getUserPrincipal();
		if (principal != null)
			currentUsername = principal.getName();

		System.out.println(">>>>>>>>>>>> currentUsername = |" + currentUsername + "|.");
		String from = "from getRemoteHost='" + req.getRemoteHost() + "', currentUsername='" + currentUsername + "', by user '" + req.getRemoteUser() + "', req.getUserPrincipal()=" + req.getUserPrincipal() + ".";
		Principal userPrincipal = req.getUserPrincipal();
		if (userPrincipal != null)
		{
			hasAdminRole = req.isUserInRole(DbxCentralRealm.ROLE_ADMIN);
			System.out.println(">>>>>>>>>>>> userPrincipal.getName() = |" + userPrincipal.getName() + "|.");
			System.out.println(">>>>>>>>>>>> req.isUserInRole(DbxCentralRealm.ROLE_ADMIN) = |" + req.isUserInRole(DbxCentralRealm.ROLE_ADMIN) + "|."); 
		}

		if (!hasAdminRole)
		{
			_logger.warn("Unauthorized SET attempt. " + from);
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}
		
//		if ( ! hasCorrectSecurityToken(req) )
//		{
//			_logger.warn("Unauthorized SET Config attempt " + from);
//			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
//			return;
//		}

		String sendType = null;
		Map<String, String> changeMap = Collections.emptyMap();
		
		try
		{
			String requestBody = IOUtils.toString(req.getReader());
System.out.println("---- requestBody: " + requestBody);
			if (StringUtil.isNullOrBlank(requestBody))
				throw new IOException("RequestBody is empty, expecting a JSON String to be passed.");

			ObjectMapper reqMapper = new ObjectMapper();
			JsonNode rootNode = reqMapper.readTree(requestBody);

			sendType  = DbxTuneSample.getString(rootNode, "type");
			cmName    = DbxTuneSample.getString(rootNode, "cmName");
			optName   = DbxTuneSample.getString(rootNode, "optName");
			changeMap = DbxTuneSample.getStringMap(rootNode, "change");

System.out.println("---- json.sendType  = '" + sendType  + "'.");
System.out.println("---- json.cmName    = '" + cmName    + "'.");
System.out.println("---- json.optName   = '" + optName + "'.");
System.out.println("---- json.changeMap = '" + changeMap + "'.");
		}
		catch (MissingFieldException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		int     respStatus = HttpServletResponse.SC_OK;
		String  statusMessage = "success";
		boolean isBootNeeded = false;
		String  infoMessage = null;
		String  errorMessage = null;


		CountersModel cm = CounterController.getInstance().getCmByName(cmName);
System.out.println("---- found-Cm: " + cm);
		CmSettingsHelper cfgEntry = null;
		if (cm == null)
		{
			respStatus    = HttpServletResponse.SC_NOT_FOUND;
			statusMessage = "FAILED";
			infoMessage   = "cmName '" + cmName + "' not found.";
		}
		else
		{
			if ("alarmSettings".equals(sendType))
			{
				if (changeMap.containsKey("timeRangeCron"))
				{
					String timeRangeCron = changeMap.get("timeRangeCron");
					
					try
					{
						CmSettingsHelper.CronTimeRangeInputValidator validator = new CmSettingsHelper.CronTimeRangeInputValidator();
						validator.isValid(null, timeRangeCron);
						
						System.out.println("-----------DATA IS VALID------------ |" + timeRangeCron + "|.");
						System.out.println("CHANGE IS NOT IMPLEMENTED");
					}
					catch (ValidationException ex)
					{
//						respStatus    = HttpServletResponse.SC_NOT_MODIFIED; // JSON body/payload wont be available by the caller 
//						respStatus    = HttpServletResponse.SC_BAD_REQUEST;
						respStatus    = HttpServletResponse.SC_OK;
//						respStatus    = 422; // ???
						statusMessage = "FAILED";
						infoMessage   = "Validation failed";
						errorMessage  = ex.getMessage();

						ex.printStackTrace();
					}
				}

				if (changeMap.containsKey("isAlarmEnabled"))
				{
					boolean isAlarmEnabled = "true".equalsIgnoreCase(changeMap.get("isAlarmEnabled"));
				}
			}
			else
			{
				String newValue = changeMap.get("value");
				
				// FIRST: Alarms
				cfgEntry = CmSettingsHelper.getByName(optName, cm.getLocalAlarmSettings());

				// SECOND: LocalSettings
				if (cfgEntry == null)
					cfgEntry = CmSettingsHelper.getByName(optName, cm.getLocalSettings());

				// Validate data
				if (cfgEntry != null)
				{
System.out.println("---- found-cfgEntry: " + cfgEntry);
					try
					{
						cfgEntry.isValidInput(newValue);
System.out.println("---- isValid-cfgEntry: " + cfgEntry);
					}
					catch (ValidationException ex) 
					{
//						respStatus    = HttpServletResponse.SC_NOT_MODIFIED;
						respStatus    = HttpServletResponse.SC_OK;
						statusMessage = "FAILED";
						infoMessage   = "Validation failed";
						errorMessage  = ex.getMessage();

						ex.printStackTrace();
					}
				}
				else // NOT FOUND
				{
//					respStatus    = HttpServletResponse.SC_NOT_FOUND;
					respStatus    = HttpServletResponse.SC_OK;
					statusMessage = "FAILED";
					infoMessage   = "optName '" + optName + "' for cmName '" + cmName + "' not found.";
				}
			}
		}
		
//		String  statusMessage = "success";
//		boolean isBootNeeded = false;
//		String  infoMessage = null;
//		String  errorMessage = null;

		// Send response status
System.out.println("SET Response status: " + respStatus);
		resp.setStatus(respStatus);

		// Setup a JSON "writer"
		StringWriter sw = new StringWriter();
		JsonFactory jfactory = new JsonFactory();
		JsonGenerator gen = jfactory.createGenerator(sw);
		gen.setPrettyPrinter(new DefaultPrettyPrinter());
		gen.setCodec(new ObjectMapper(jfactory));

		
		gen.writeStartObject();
		gen.writeStringField ("status"      , statusMessage);
		if (StringUtil.hasValue(infoMessage))  gen.writeStringField ("info" , infoMessage);
		if (StringUtil.hasValue(errorMessage)) gen.writeStringField ("error", errorMessage);
		gen.writeBooleanField("isBootNeeded", isBootNeeded);
		gen.writeEndObject();
		gen.close();
		
		// Generate PAYLOAD
		String payload = sw.toString();
		out.println(payload);
System.out.println("SEND Payload: " + payload);
		
		out.flush();
		out.close();
	}
}
