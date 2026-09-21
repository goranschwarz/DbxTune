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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import com.dbxtune.central.pcs.DbxTuneSample;
import com.dbxtune.central.pcs.DbxTuneSample.MissingFieldException;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSettingsHelper.ValidationException;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.mgt.NoGuiManagementServer;
import com.dbxtune.utils.Configuration;
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

		String expectedBasic  = System.getProperty(NoGuiManagementServer.PROPKEY_NOGUI_MANAGEMENT_http_auth_Basic);
		String expectedBearer = System.getProperty(NoGuiManagementServer.PROPKEY_NOGUI_MANAGEMENT_http_auth_Bearer);

		boolean hasAdminRole = (auth != null) && (auth.equals(expectedBasic) || auth.equals(expectedBearer));

		String from = "from getRemoteHost='" + req.getRemoteHost() + "', by user '" + req.getRemoteUser() + "'.";

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
		String saveType = null;
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
			saveType  = DbxTuneSample.getString(rootNode, "saveType");
			cmName    = DbxTuneSample.getString(rootNode, "cmName");
			optName   = DbxTuneSample.getString(rootNode, "optName");
			changeMap = DbxTuneSample.getStringMap(rootNode, "change");

System.out.println("---- json.sendType  = '" + sendType  + "'.");
System.out.println("---- json.saveType  = '" + saveType  + "'.");
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


		int     respStatus    = HttpServletResponse.SC_OK;
		String  statusMessage = "success";
		boolean isBootNeeded  = false;
		String  infoMessage   = null;
		String  errorMessage  = null;
		Map<String, String> fieldErrors = Collections.emptyMap(); // per parameter validation errors (alarmBatch)

//		boolean saveToFile = "THIS_SERVER".equals(saveType) || "SERVER_TEMPLATE".equals(saveType);
		boolean saveToFile = "THIS_SERVER".equals(saveType);

		CountersModel cm = CounterController.getInstance().getCmByName(cmName);
		_logger.debug("doPost(): found cm={}", cm);

		// Right now the "Save for ALL Server sharing the same config file" IS NOT SUPPORTED
		// For that to work: we need to implement "editable" Properties file.
		if ("SERVER_TEMPLATE".equals(saveType))
		{
			String sharedGfgFile = "";
			if (Configuration.hasInstance(Configuration.PCS)) 
				sharedGfgFile = Configuration.getInstance(Configuration.PCS).getFilename();

			respStatus    = HttpServletResponse.SC_OK;
			statusMessage = "FAILED";
			infoMessage   = "Save for ALL Server sharing the same config file -- IS NOT YET Implemented";

			if (StringUtil.hasValue(sharedGfgFile))
				infoMessage += "<br>Instead change the property in the file: " + sharedGfgFile;
		}
		else if (cm == null)
		{
			respStatus    = HttpServletResponse.SC_NOT_FOUND;
			statusMessage = "FAILED";
			infoMessage   = "cmName '" + cmName + "' not found.";
		}
		else if ("alarmSettings".equals(sendType) || "alarmBatch".equals(sendType))
		{
			// "alarmBatch"   : change = { "<paramName>": "<value>", ... } for the alarm 'optName', where paramName is
			//                  as in NoGuiConfigGetServlet 'alarmSettings.alarms[].parameters[].name' (including "<alarm> isAlarmEnabled" and "<alarm> timeRangeCron")
			// "alarmSettings": change = { "isAlarmEnabled": "true|false", "timeRangeCron": "..." } for the alarm 'optName'
			//                  which is the same as a batch with "<optName> isAlarmEnabled" and "<optName> timeRangeCron"
			// ALL values are validated before ANYTHING is saved, and the file is written once.
			Map<String, String> batch = changeMap;
			if ("alarmSettings".equals(sendType))
			{
				batch = new LinkedHashMap<>();
				if (changeMap.containsKey("isAlarmEnabled")) batch.put(optName + IS_ALARM_ENABLED_SUFFIX, changeMap.get("isAlarmEnabled"));
				if (changeMap.containsKey("timeRangeCron" )) batch.put(optName + TIME_RANGE_CRON_SUFFIX , changeMap.get("timeRangeCron"));
			}

			Configuration noguiConf = Configuration.getInstance(Configuration.NOGUI_SAVE);

			// Phase 1: resolve and validate everything
			List<ResolvedChange> resolved = new ArrayList<>();
			Map<String, String> errors = new TreeMap<>();
			for (Map.Entry<String, String> entry : batch.entrySet())
			{
				String paramName = entry.getKey();
				if (StringUtil.hasValue(optName) && !paramName.equals(optName) && !paramName.startsWith(optName + " "))
				{
					errors.put(paramName, "Parameter '" + paramName + "' does not belong to the alarm '" + optName + "'.");
					continue;
				}
				ResolvedChange rc = resolveChange(cm, false, paramName, entry.getValue());
				if (rc.errorMsg != null)
					errors.put(paramName, rc.errorMsg);
				else
					resolved.add(rc);
			}

			if (noguiConf == null)
			{
				statusMessage = "FAILED";
				infoMessage   = "No NOGUI_SAVE config instance available.";
			}
			else if ( ! errors.isEmpty() )
			{
				statusMessage = "FAILED";
				infoMessage   = "Validation failed, nothing was saved.";
				errorMessage  = errors.size() == 1 ? errors.values().iterator().next() : errors.size() + " values failed validation.";
				fieldErrors   = errors;
				_logger.warn("Validation failed for cm '{}', alarm '{}': {}", cmName, optName, errors);
			}
			else
			{
				// Phase 2: everything is valid... save (write the file once at the end)
				List<String> infoList = new ArrayList<>();
				for (ResolvedChange rc : resolved)
				{
					saveConfigProperty(noguiConf, rc.propKey, rc.newValue, rc.defaultValue, false);
					infoList.add(rc.info);
				}
				if (saveToFile && !resolved.isEmpty())
					noguiConf.save();

				// Some CM's parse alarm settings only in initAlarms(), make them use the new values from the next alarm check
				if ( ! resolved.isEmpty() )
					cm.requestInitAlarms();

				infoMessage = resolved.isEmpty() ? "Nothing to change." : String.join("; ", infoList);
			}
		}
		else if ("options".equals(sendType))
		{
			String newValue  = changeMap.get("value");
			CmSettingsHelper cfgEntry = CmSettingsHelper.getByName(optName, cm.getCollectorOptions());

			if (cfgEntry == null)
			{
				respStatus    = HttpServletResponse.SC_OK;
				statusMessage = "FAILED";
				infoMessage   = "optName '" + optName + "' not found in collector options for cmName '" + cmName + "'.";
			}
			else
			{
				try
				{
					cfgEntry.isValidInput(newValue);
					applyCollectorOption(cm, cfgEntry, newValue);

					// CM setters call saveProps() → USER_TEMP, which may be null in NoGui mode.
					// Persist explicitly to NOGUI_SAVE so the value survives restart.
					Configuration noguiConf = Configuration.getInstance(Configuration.NOGUI_SAVE);
					if (noguiConf != null)
						saveConfigProperty(noguiConf, cfgEntry.getPropName(), newValue, cfgEntry.getDefaultValue(), saveToFile);

					infoMessage = "Option '" + optName + "' set to '" + newValue + "'.";
				}
				catch (ValidationException ex)
				{
					respStatus    = HttpServletResponse.SC_OK;
					statusMessage = "FAILED";
					infoMessage   = "Validation failed";
					errorMessage  = ex.getMessage();
					_logger.warn("Validation failed for option '{}' value='{}': {}", optName, newValue, ex.getMessage());
				}
				catch (SaveConfigException ex)
				{
					respStatus    = HttpServletResponse.SC_OK;
					statusMessage = "FAILED";
					infoMessage   = "Apply option failed";
					errorMessage  = ex.getMessage();
					_logger.warn("Failed to apply option '{}' value='{}': {}", optName, newValue, ex.getMessage());
				}
			}
		}
		else // "settings", "alarmParams", or default — look up in LocalSettings / LocalAlarmSettings
		{
			ResolvedChange rc = resolveChange(cm, "settings".equals(sendType), optName, changeMap.get("value"));
			Configuration noguiConf = Configuration.getInstance(Configuration.NOGUI_SAVE);

			if (rc.errorMsg != null)
			{
				respStatus    = HttpServletResponse.SC_OK;
				statusMessage = "FAILED";
				infoMessage   = rc.errorInfo;
				errorMessage  = rc.errorMsg;
				_logger.warn("{} for '{}' value='{}': {}", rc.errorInfo, optName, changeMap.get("value"), rc.errorMsg);
			}
			else if (noguiConf == null)
			{
				statusMessage = "FAILED";
				infoMessage   = "No NOGUI_SAVE config instance available.";
			}
			else
			{
				saveConfigProperty(noguiConf, rc.propKey, rc.newValue, rc.defaultValue, saveToFile);
				infoMessage = rc.info;

				// Local Settings are often used when the SQL is created, and some CM's parse alarm settings only in initAlarms()
				// Make them use the new values from the next refresh / alarm check
				if ( "settings".equals(sendType) )
					cm.requestSqlReInit();
				else
					cm.requestInitAlarms();
			}
		}

		// Send response
		_logger.debug("doPost(): respStatus={}, statusMessage='{}', info='{}', error='{}'", respStatus, statusMessage, infoMessage, errorMessage);
		resp.setStatus(respStatus);

		StringWriter sw = new StringWriter();
		JsonFactory jfactory = new JsonFactory();
		JsonGenerator gen = jfactory.createGenerator(sw);
		gen.setPrettyPrinter(new DefaultPrettyPrinter());
		gen.setCodec(new ObjectMapper(jfactory));

		gen.writeStartObject();
		gen.writeStringField ("status"      , statusMessage);
		if (StringUtil.hasValue(infoMessage))  gen.writeStringField("info" , infoMessage);
		if (StringUtil.hasValue(errorMessage)) gen.writeStringField("error", errorMessage);
		if ( ! fieldErrors.isEmpty() )
		{
			gen.writeObjectFieldStart("errors");
			for (Map.Entry<String, String> e : fieldErrors.entrySet())
				gen.writeStringField(e.getKey(), e.getValue());
			gen.writeEndObject();
		}
		gen.writeBooleanField("isBootNeeded", isBootNeeded);
		gen.writeEndObject();
		gen.close();

		String payload = sw.toString();
		out.println(payload);
		_logger.debug("doPost(): payload={}", payload);

		out.flush();
		out.close();
	}

	private static final String IS_ALARM_ENABLED_SUFFIX = " isAlarmEnabled";
	private static final String TIME_RANGE_CRON_SUFFIX  = " timeRangeCron";

	/** A validated change (propKey/newValue/defaultValue), or why it can't be done (errorInfo/errorMsg) */
	private static class ResolvedChange
	{
		String propKey;
		String newValue;
		String defaultValue;
		String info;       // description of the change, when OK
		String errorInfo;  // short reason, when NOT OK
		String errorMsg;   // detailed message, when NOT OK (null = OK)

		static ResolvedChange ok(String propKey, String newValue, String defaultValue, String info)
		{
			ResolvedChange rc = new ResolvedChange();
			rc.propKey = propKey; rc.newValue = newValue; rc.defaultValue = defaultValue; rc.info = info;
			return rc;
		}
		static ResolvedChange failed(String errorInfo, String errorMsg)
		{
			ResolvedChange rc = new ResolvedChange();
			rc.errorInfo = errorInfo; rc.errorMsg = errorMsg;
			return rc;
		}
	}

	/**
	 * Find and validate a Local Setting / Local Alarm Setting, or the injected "&lt;alarm&gt; isAlarmEnabled" / "&lt;alarm&gt; timeRangeCron"
	 * (those are not in getLocalAlarmSettings(), they live as config properties). Nothing is saved here.
	 *
	 * @param cm                The CM
	 * @param isLocalSettings   true = look in getLocalSettings(), false = getLocalAlarmSettings() with fallback to getLocalSettings()
	 * @param name              Name of the setting (as in NoGuiConfigGetServlet)
	 * @param newValue          The new value
	 */
	private ResolvedChange resolveChange(CountersModel cm, boolean isLocalSettings, String name, String newValue)
	{
		List<CmSettingsHelper> localSettings      = cm.getLocalSettings()      != null ? cm.getLocalSettings()      : Collections.emptyList();
		List<CmSettingsHelper> localAlarmSettings = cm.getLocalAlarmSettings() != null ? cm.getLocalAlarmSettings() : Collections.emptyList();

		CmSettingsHelper cfgEntry = CmSettingsHelper.getByName(name, isLocalSettings ? localSettings : localAlarmSettings);

		// Fallback to LocalSettings when searching alarm params
		if (cfgEntry == null && !isLocalSettings)
			cfgEntry = CmSettingsHelper.getByName(name, localSettings);

		if (cfgEntry != null)
		{
			try
			{
				cfgEntry.isValidInput(newValue);
				return ResolvedChange.ok(cfgEntry.getPropName(), newValue, cfgEntry.getDefaultValue(), "Setting '" + name + "' set to '" + newValue + "'.");
			}
			catch (ValidationException ex)
			{
				return ResolvedChange.failed("Validation failed", ex.getMessage());
			}
		}

		if (name != null && name.endsWith(IS_ALARM_ENABLED_SUFFIX))
		{
			String colname = name.substring(0, name.length() - IS_ALARM_ENABLED_SUFFIX.length());
			if ( ! "true".equalsIgnoreCase(newValue) && ! "false".equalsIgnoreCase(newValue) )
				return ResolvedChange.failed("Validation failed", "Value for '" + name + "' must be 'true' or 'false', not '" + newValue + "'.");

			boolean isAlarmEnabled = "true".equalsIgnoreCase(newValue);
			String propKey = cm.replaceCmAndColName(CountersModel.PROPKEY_ALARM_isSystemAlarmsForColumnEnabled, colname);
			return ResolvedChange.ok(propKey, String.valueOf(isAlarmEnabled), String.valueOf(CountersModel.DEFAULT_ALARM_isSystemAlarmsForColumnEnabled),
					"Alarm '" + colname + "' isAlarmEnabled=" + isAlarmEnabled);
		}

		if (name != null && name.endsWith(TIME_RANGE_CRON_SUFFIX))
		{
			String colname = name.substring(0, name.length() - TIME_RANGE_CRON_SUFFIX.length());
			try
			{
				new CmSettingsHelper.CronTimeRangeInputValidator().isValid(null, newValue);
				String propKey = cm.replaceCmAndColName(CountersModel.PROPKEY_ALARM_isSystemAlarmsForColumnInTimeRange, colname);
				return ResolvedChange.ok(propKey, newValue, CountersModel.DEFAULT_ALARM_isSystemAlarmsForColumnInTimeRange,
						"Alarm '" + colname + "' timeRangeCron='" + newValue + "'");
			}
			catch (ValidationException ex)
			{
				return ResolvedChange.failed("Validation failed for timeRangeCron", ex.getMessage());
			}
		}

		return ResolvedChange.failed("Not found", "optName '" + name + "' for cmName '" + cm.getName() + "' not found.");
	}

	private static class SaveConfigException 
	extends Exception
	{
		private static final long serialVersionUID = 1L;

		public SaveConfigException(String msg)
		{
			super(msg);
		}
	}

	/**
	 * Write a value to the NOGUI_SAVE in-memory config.
	 * If the value equals the default, the stored key is <em>removed</em> so the application
	 * default takes effect cleanly (no stale overrides in the file).
	 * For non-default values, {@code Configuration.setProperty()} is used — it automatically
	 * prefixes registered defaults with {@code USE_DEFAULT:} as a versioning safeguard.
	 */
	private void saveConfigProperty(Configuration noguiConf, String propKey, String newValue, String defaultValue, boolean saveToFile)
	{
		String saveLocation = !saveToFile ? "-in-memory-only-" : "using file '" + noguiConf.getFilename() + "'.";

		if (defaultValue != null && defaultValue.equals(newValue))
		{
			_logger.info("Removing property '" + propKey + "' from cfg=" + (noguiConf.getConfName() + ", " + saveLocation) );
			noguiConf.remove(propKey);
		}
		else
		{
			_logger.info("Changing property '" + propKey + "' to '" + newValue + "', at cfg=" + (noguiConf.getConfName() + ", " + saveLocation) );
			noguiConf.setProperty(propKey, newValue);
		}

		if (saveToFile)
			noguiConf.save();
	}

	private void applyCollectorOption(CountersModel cm, CmSettingsHelper cfgEntry, String newValue)
	throws SaveConfigException
	{
		String propName = cfgEntry.getPropName();
		String cmBase   = cm.getName() + ".";
		String keyPart  = propName.startsWith(cmBase) ? propName.substring(cmBase.length()) : propName;

		try
		{
			if      (CountersModel.PROPKEY_sampleDataIsPaused        .equals(keyPart)) cm.setPauseDataPolling          (Boolean.parseBoolean(newValue), false);
			else if (CountersModel.PROPKEY_postponeTime              .equals(keyPart)) cm.setPostponeTime              (Integer.parseInt    (newValue), false);
			else if (CountersModel.PROPKEY_postponeIsEnabled         .equals(keyPart)) cm.setPostponeIsEnabled         (Boolean.parseBoolean(newValue), false);
			else if (CountersModel.PROPKEY_queryTimeout              .equals(keyPart)) cm.setQueryTimeout              (Integer.parseInt    (newValue), false);
			else if (CountersModel.PROPKEY_negativeDiffCountersToZero.equals(keyPart)) cm.setNegativeDiffCountersToZero(Boolean.parseBoolean(newValue), false);
			else if (CountersModel.PROPKEY_persistCounters           .equals(keyPart)) cm.setPersistCounters           (Boolean.parseBoolean(newValue), false);
			else if (CountersModel.PROPKEY_persistCounters_abs       .equals(keyPart)) cm.setPersistCountersAbs        (Boolean.parseBoolean(newValue), false);
			else if (CountersModel.PROPKEY_persistCounters_diff      .equals(keyPart)) cm.setPersistCountersDiff       (Boolean.parseBoolean(newValue), false);
			else if (CountersModel.PROPKEY_persistCounters_rate      .equals(keyPart)) cm.setPersistCountersRate       (Boolean.parseBoolean(newValue), false);
//			else if (CountersModel.PROPKEY_isEnabled                 .equals(keyPart)) cm.setEnabledByUser             (Boolean.parseBoolean(newValue), false);
			else
				throw new SaveConfigException("Unknown collector option propName '" + propName + "' (keyPart='" + keyPart + "').");
		}
		catch (NumberFormatException ex)
		{
			throw new SaveConfigException("Invalid number for option '" + propName + "' value='" + newValue + "': " + ex.getMessage());
		}
	}
}
