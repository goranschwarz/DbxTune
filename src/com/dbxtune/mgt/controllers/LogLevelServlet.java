/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
import com.dbxtune.Version;
import com.dbxtune.central.controllers.Helper;
import com.dbxtune.utils.Log4jUtils;
import com.dbxtune.utils.Logging;
import com.dbxtune.utils.ServletUtils;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Collector-side servlet: /api/mgt/log-level -- View and change Log4j log levels at runtime.
 * <p>
 * Both GET and POST needs the management "Authorization" token (same as /api/mgt/restart).
 * <p>
 * <b>GET</b> returns JSON:
 * <pre>
 * {"srvName":"PROD_A_ASE", "appName":"AseTune", "logFile":"/.../PROD_A_ASE.log", "saveFile":"/.../servers/PROD_A_ASE.save.conf",
 *  "loggers":[ {"name":"ROOT", "level":"INFO", "nonDefault":false, "saved":""}, {"name":"com.dbxtune.cm.ase.CmSummary", "level":"DEBUG", "nonDefault":true, "saved":"DEBUG"}, ... ]}
 * </pre>
 *
 * <b>POST</b> with the JSON body:
 * <pre>
 * {"saveType":"IN_MEMORY|THIS_SERVER", "changes":[ {"logger":"com.dbxtune.cm.ase.CmSummary", "level":"DEBUG"}, ... ]}
 * </pre>
 * <ul>
 *   <li>logger   - class name, package name or 'ROOT'</li>
 *   <li>level    - OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE, ALL ... or DEFAULT (or empty) to go back to the default level.<br>
 *                  The level it would inherit anyway is the same as DEFAULT (set DEBUG and then INFO again, and it's not "nonDefault" any more)</li>
 *   <li>saveType - IN_MEMORY (default): until the restart. THIS_SERVER: also saved, so it's used after a restart</li>
 * </ul>
 * All changes are checked before anything is changed. Returns JSON:
 * <pre>
 * {"status":"success", "saveType":"IN_MEMORY", "changed":[ {"logger":"com.dbxtune.cm.ase.CmSummary", "from":"INFO", "to":"DEBUG"} ]}
 * {"status":"FAILED",  "error":"bad-request", "message":"...", "errors":{"com.dbxtune.cm.ase.CmSummary":"Unknown log level 'DEBUGGG'..."}}
 * </pre>
 *
 * DbxCentral uses {@link #getLogLevels(String)} and {@link #setLogLevels(String, String)} for it's own log levels (see LogLevelAdminServlet)
 */
public class LogLevelServlet
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		ServletOutputStream out = resp.getOutputStream();

		boolean isAuthorized = ServletUtils.checkBasicOrTokenAuthentication(req, resp, out);
		if ( ! isAuthorized )
			return;

		String srvName = CounterController.hasInstance() ? CounterController.getInstance().getServerName() : "";

		Helper.createObjectMapper().writeValue(out, getLogLevels(srvName));
		out.flush();
		out.close();
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		ServletOutputStream out = resp.getOutputStream();

		boolean isAuthorized = ServletUtils.checkBasicOrTokenAuthentication(req, resp, out);
		if ( ! isAuthorized )
			return;

		String requestBody = IOUtils.toString(req.getReader());

		Map<String, Object> result = setLogLevels(requestBody, "the NO-GUI Management interface (remote=" + req.getRemoteAddr() + ")");
		if ( ! "success".equals(result.get("status")) )
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

		Helper.createObjectMapper().writeValue(out, result);
		out.flush();
		out.close();
	}

	/**
	 * What we return on a GET
	 * @param srvName    Name of the server, which is just passed back in the result
	 */
	public static Map<String, Object> getLogLevels(String srvName)
	{
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("srvName" , srvName);
		result.put("appName" , Version.getAppName());
		result.put("logFile" , Logging.getLogFilename());
		result.put("saveFile", Log4jUtils.getSavedLogLevelFilename());
		result.put("loggers" , Log4jUtils.getLoggerLevels());
		return result;
	}

	/**
	 * What we do on a POST
	 * @param requestBody    The JSON body, see the class description
	 * @param requestedBy    Who asked for it, only used in the log message
	 * @return The result, see the class description
	 */
	public static Map<String, Object> setLogLevels(String requestBody, String requestedBy)
	{
		String              saveType = "IN_MEMORY";
		List<String[]>      changes  = new ArrayList<>();     // [0]=logger, [1]=level
		Map<String, String> errors   = new LinkedHashMap<>(); // logger -> error message
		String              message  = null;

		// Parse and check everything, before we change anything
		try
		{
			if (StringUtil.isNullOrBlank(requestBody))
				throw new IOException("The request body is empty, expecting a JSON String.");

			JsonNode root = new ObjectMapper().readTree(requestBody);

			if (root.hasNonNull("saveType"))
				saveType = root.get("saveType").asText();

			if ( ! "IN_MEMORY".equals(saveType) && ! "THIS_SERVER".equals(saveType) )
				throw new IOException("Unknown saveType '" + saveType + "', expecting 'IN_MEMORY' or 'THIS_SERVER'.");

			if ("THIS_SERVER".equals(saveType) && Log4jUtils.getSavedLogLevelFilename() == null)
				throw new IOException("Log levels can't be saved in this server (no save configuration file), use 'IN_MEMORY'.");

			JsonNode changesNode = root.get("changes");
			if (changesNode == null || ! changesNode.isArray() || changesNode.size() == 0)
				throw new IOException("No 'changes' was found, expecting: \"changes\":[{\"logger\":\"name\", \"level\":\"DEBUG\"}]");

			for (JsonNode change : changesNode)
			{
				String logger = change.path("logger").asText("").trim();
				String level  = change.path("level" ).asText("").trim();

				if (logger.isEmpty())
				{
					errors.put("", "A change without a 'logger' name was found.");
					continue;
				}

				try { Log4jUtils.toLevel(level); }
				catch (IllegalArgumentException ex) { errors.put(logger, ex.getMessage()); }

				changes.add(new String[] {logger, level});
			}

			if ( ! errors.isEmpty() )
				message = errors.size() == 1 ? errors.values().iterator().next() : errors.size() + " changes are not valid.";
		}
		catch (Exception ex)
		{
			message = ex.getMessage();
		}

		Map<String, Object> result = new LinkedHashMap<>();

		if (message != null)
		{
			_logger.warn("Log level change requested by " + requestedBy + " was REJECTED, nothing was changed. " + message + " requestBody=|" + requestBody + "|");

			result.put("status" , "FAILED");
			result.put("error"  , "bad-request");
			result.put("message", message + " Nothing was changed.");
			result.put("errors" , errors);
			return result;
		}

		_logger.info("Log level change requested by " + requestedBy + ", saveType='" + saveType + "', number of changes: " + changes.size());

		List<Map<String, String>> changed = new ArrayList<>();
		for (String[] change : changes)
		{
			String logger = change[0];
			String level  = change[1];

			String from = Log4jUtils.setLoggerLevel(logger, level, "THIS_SERVER".equals(saveType));

			Map<String, String> entry = new LinkedHashMap<>();
			entry.put("logger", logger);
			entry.put("from"  , from);
			entry.put("to"    , Log4jUtils.getLoggerLevel(logger)); // the level it has now, which for DEFAULT is the inherited level
			changed.add(entry);
		}

		result.put("status"  , "success");
		result.put("saveType", saveType);
		result.put("changed" , changed);
		return result;
	}
}
