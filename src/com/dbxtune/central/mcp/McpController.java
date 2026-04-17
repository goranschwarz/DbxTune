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
package com.dbxtune.central.mcp;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP (Model Context Protocol) endpoint for DbxCentral.
 * <p>
 * Exposes performance counter data to AI assistants (Claude, etc.) so they can
 * explore monitored servers, query metrics, and help diagnose performance problems.
 * <p>
 * Endpoints:
 * <ul>
 *   <li>{@code POST /mcp} — JSON-RPC 2.0 request/response (main MCP channel)</li>
 *   <li>{@code GET  /mcp} — Server-Sent Events stream for server-push notifications</li>
 * </ul>
 * <p>
 * Enable via {@code DBX_CENTRAL.conf}:
 * <pre>
 *   DbxTuneCentral.mcp.enabled    = true
 *   DbxTuneCentral.mcp.apiToken   = your-secret-token
 *   DbxTuneCentral.mcp.allowedHosts = 127.0.0.1
 * </pre>
 */
public class McpController
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	// -------------------------------------------------------------------------
	// Configuration property keys
	// -------------------------------------------------------------------------
	public static final String  PROPKEY_enabled            = "DbxTuneCentral.mcp.enabled";
	public static final boolean DEFAULT_enabled            = false;

	public static final String  PROPKEY_apiToken           = "DbxTuneCentral.mcp.apiToken";
	public static final String  DEFAULT_apiToken           = "";

	public static final String  PROPKEY_allowedHosts       = "DbxTuneCentral.mcp.allowedHosts";
//	public static final String  DEFAULT_allowedHosts       = "127.0.0.1";
	public static final String  DEFAULT_allowedHosts       = "*";

	public static final String  PROPKEY_maxTimeRangeHours  = "DbxTuneCentral.mcp.maxTimeRangeHours";
	public static final int     DEFAULT_maxTimeRangeHours  = 168;

	public static final String  PROPKEY_exposeDbmsConfig   = "DbxTuneCentral.mcp.exposeDbmsConfig";
	public static final boolean DEFAULT_exposeDbmsConfig   = true;

	public static final String  PROPKEY_sseNotifications   = "DbxTuneCentral.mcp.sseNotificationsEnabled";
	public static final boolean DEFAULT_sseNotifications   = false;

	// -------------------------------------------------------------------------
	// Runtime config (read once at servlet init)
	// -------------------------------------------------------------------------
	private boolean      _enabled;
	private String       _apiToken;
	private Set<String>  _allowedHosts;
	private int          _maxTimeRangeHours;
	private boolean      _exposeDbmsConfig;
	private boolean      _sseNotifications;

	private McpHandler   _handler;
	private ObjectMapper _om = new ObjectMapper();

	// -------------------------------------------------------------------------

	@Override
	public void init() 
	throws ServletException
	{
		Configuration conf = Configuration.getCombinedConfiguration();

		_enabled           = conf.getBooleanProperty(PROPKEY_enabled,            DEFAULT_enabled);
		_apiToken          = conf.getProperty       (PROPKEY_apiToken,           DEFAULT_apiToken);
		_maxTimeRangeHours = conf.getIntProperty    (PROPKEY_maxTimeRangeHours,  DEFAULT_maxTimeRangeHours);
		_exposeDbmsConfig  = conf.getBooleanProperty(PROPKEY_exposeDbmsConfig,   DEFAULT_exposeDbmsConfig);
		_sseNotifications  = conf.getBooleanProperty(PROPKEY_sseNotifications,   DEFAULT_sseNotifications);

		String allowedHostsStr = conf.getProperty(PROPKEY_allowedHosts, DEFAULT_allowedHosts);
		_allowedHosts = parseHostList(allowedHostsStr);

		_handler = new McpHandler(_maxTimeRangeHours, _exposeDbmsConfig);

		_logger.info("McpController initialized: enabled={}, apiToken={}, allowedHosts={}, maxTimeRangeHours={}, exposeDbmsConfig={}, sseNotifications={}",
				_enabled, _apiToken, _allowedHosts, _maxTimeRangeHours, _exposeDbmsConfig, _sseNotifications);
	}

	// -------------------------------------------------------------------------
	// POST /mcp  — JSON-RPC 2.0
	// -------------------------------------------------------------------------

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
	throws ServletException, IOException
	{
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");

		if (!checkEnabled(resp))   return;
		if (!checkAuth(req, resp)) return;
		if (!checkIp(req, resp))   return;

		String body;
		try
		{
			body = req.getReader().lines().collect(Collectors.joining("\n"));
		}
		catch (Exception e)
		{
			sendParseError(resp, null, "Failed to read request body: " + e.getMessage());
			return;
		}

		JsonNode requestNode;
		try
		{
			requestNode = _om.readTree(body);
		}
		catch (Exception e)
		{
			sendParseError(resp, null, "Invalid JSON: " + e.getMessage());
			return;
		}

		// Handle batch requests (array) — not commonly used but spec allows it
		if (requestNode.isArray())
		{
			// Return error for batch — not supported in v1
			sendError(resp, null, -32600, "Batch requests not supported");
			return;
		}

		JsonNode idNode     = requestNode.get("id");
		String   id         = (idNode != null && !idNode.isNull()) ? idNode.asText() : null;
		String   method     = requestNode.has("method") ? requestNode.get("method").asText() : null;
		JsonNode params     = requestNode.has("params") ? requestNode.get("params") : _om.createObjectNode();

		if (method == null)
		{
			sendError(resp, id, -32600, "Missing 'method' field");
			return;
		}

		try
		{
			JsonNode result = _handler.handle(method, params);
			writeSuccess(resp, id, result);
		}
		catch (McpHandler.McpInvalidParamsException e)
		{
			sendError(resp, id, -32602, e.getMessage());
		}
		catch (McpHandler.McpMethodNotFoundException e)
		{
			sendError(resp, id, -32601, "Method not found: " + method);
		}
		catch (Exception e)
		{
			_logger.warn("MCP internal error handling method '{}': {}", method, e.getMessage(), e);
			sendError(resp, id, -32603, "Internal error: " + e.getMessage());
		}
	}

	// -------------------------------------------------------------------------
	// GET /mcp  — Server-Sent Events (SSE) for notifications
	// -------------------------------------------------------------------------

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
	throws ServletException, IOException
	{
		if (!checkEnabled(resp))   return;
		if (!checkAuth(req, resp)) return;
		if (!checkIp(req, resp))   return;

		if (!_sseNotifications)
		{
			// SSE not enabled — return 204 No Content so clients know it's there but empty
			resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
			return;
		}

		resp.setContentType("text/event-stream");
		resp.setCharacterEncoding("UTF-8");
		resp.setHeader("Cache-Control", "no-cache");
		resp.setHeader("Connection", "keep-alive");
		resp.setHeader("X-Accel-Buffering", "no");

		AsyncContext async = req.startAsync();
		async.setTimeout(0); // no timeout — client controls lifetime

		ServletOutputStream out = resp.getOutputStream();

		// Send initial ping comment to confirm connection
		out.println(": DbxCentral MCP SSE connected");
		out.println();
		out.flush();

		McpSseManager.getInstance().register(async, out);
	}

	// -------------------------------------------------------------------------
	// Auth & access checks
	// -------------------------------------------------------------------------

	private boolean checkEnabled(HttpServletResponse resp) 
	throws IOException
	{
		if (!_enabled)
		{
			resp.sendError(HttpServletResponse.SC_NOT_FOUND,
					"MCP endpoint is disabled. Set DbxTuneCentral.mcp.enabled=true in DBX_CENTRAL.conf to enable.");
			return false;
		}
		return true;
	}

	private boolean checkAuth(HttpServletRequest req, HttpServletResponse resp) 
	throws IOException
	{
		if (StringUtil.isNullOrBlank(_apiToken))
			return true; // no token configured: open (rely on IP allowlist)

		String authHeader = req.getHeader("Authorization");
		if (authHeader == null || !authHeader.startsWith("Bearer "))
		{
			resp.setHeader("WWW-Authenticate", "Bearer realm=\"DbxCentral MCP\"");
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header. Expected: Bearer <token>");
			return false;
		}

		String token = authHeader.substring("Bearer ".length()).trim();
		if (!_apiToken.equals(token))
		{
			resp.setHeader("WWW-Authenticate", "Bearer realm=\"DbxCentral MCP\"");
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API token.");
			return false;
		}
		return true;
	}

	private boolean checkIp(HttpServletRequest req, HttpServletResponse resp) 
	throws IOException
	{
		if (_allowedHosts.isEmpty() || _allowedHosts.contains("*"))
			return true; // empty list = allow all

		String remoteAddr = req.getRemoteAddr();

		// Also check X-Forwarded-For if behind a proxy
		String forwarded = req.getHeader("X-Forwarded-For");
		String clientIp  = (forwarded != null) ? forwarded.split(",")[0].trim() : remoteAddr;

		if (!_allowedHosts.contains(clientIp) && !_allowedHosts.contains(remoteAddr))
		{
			_logger.warn("MCP access denied from IP '{}' (remoteAddr='{}'). Not in allowedHosts: {}", clientIp, remoteAddr, _allowedHosts);
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied: IP '" + clientIp + "' not in DbxTuneCentral.mcp.allowedHosts");
			return false;
		}
		return true;
	}

	// -------------------------------------------------------------------------
	// JSON-RPC response helpers
	// -------------------------------------------------------------------------

	private void writeSuccess(HttpServletResponse resp, String id, JsonNode result) 
	throws IOException
	{
		ObjectNode response = _om.createObjectNode();
		response.put("jsonrpc", "2.0");
		if (id != null)
			response.put("id", id);
		else
			response.putNull("id");
		response.set("result", result);

		resp.getWriter().write(_om.writeValueAsString(response));
	}

	private void sendError(HttpServletResponse resp, String id, int code, String message)
	throws IOException
	{
		ObjectNode error = _om.createObjectNode();
		error.put("code", code);
		error.put("message", message);

		ObjectNode response = _om.createObjectNode();
		response.put("jsonrpc", "2.0");
		if (id != null)
			response.put("id", id);
		else
			response.putNull("id");
		response.set("error", error);

		resp.getWriter().write(_om.writeValueAsString(response));
	}

	private void sendParseError(HttpServletResponse resp, String id, String message) 
	throws IOException
	{
		sendError(resp, id, -32700, message);
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private static Set<String> parseHostList(String csv)
	{
		if (csv == null || csv.trim().isEmpty())
			return new HashSet<>();
		return Arrays.stream(csv.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toSet());
	}
}
