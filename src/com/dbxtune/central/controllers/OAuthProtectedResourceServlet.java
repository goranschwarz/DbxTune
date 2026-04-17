/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
 *
 * This file is part of DbxTune
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 ******************************************************************************/
package com.dbxtune.central.controllers;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * RFC 9728 — OAuth 2.0 Protected Resource Metadata.
 *
 * <pre>GET /.well-known/oauth-protected-resource</pre>
 *
 * <p>Tells MCP clients (e.g. {@code mcp-remote}) that this server accepts
 * Bearer tokens via the {@code Authorization} request header — no OAuth
 * authorization server or token exchange is required.
 *
 * <p>When {@code mcp-remote} sees {@code bearer_methods_supported: ["header"]}
 * with an empty {@code authorization_servers} list it skips the OAuth flow
 * and uses the static {@code Authorization: Bearer <token>} header supplied
 * via its {@code --header} flag.
 *
 * <p>Example Claude Desktop config (using mcp-remote):
 * <pre>{@code
 * "dbxcentral": {
 *   "command": "npx",
 *   "args": ["-y", "mcp-remote",
 *            "http://your-host:8080/mcp",
 *            "--header", "Authorization: Bearer your-token"]
 * }
 * }</pre>
 */
public class OAuthProtectedResourceServlet
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		// Build the resource URI — the MCP endpoint
		String baseUrl  = buildBaseUrl(req);
		String resource = baseUrl + "/mcp";

		Map<String, Object> meta = new LinkedHashMap<>();
		meta.put("resource",                 resource);
		// RFC 9728 §3: omitting authorization_servers entirely (rather than [])
		// signals "no OAuth server" — mcp-remote should use the --header Bearer token directly.
		// We also provide the OAuth AS endpoints as a fallback for clients that still attempt discovery.
		meta.put("authorization_servers",    new String[]{ resource.replace("/mcp", "") });
		meta.put("bearer_methods_supported", new String[]{"header"});

		_logger.debug("OAuthProtectedResourceServlet: serving metadata for resource={}", resource);

		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		// Allow cross-origin fetches so browser-based MCP clients can read this
		resp.setHeader("Access-Control-Allow-Origin", "*");

		new ObjectMapper().writeValue(resp.getOutputStream(), meta);
	}

	/** OPTIONS pre-flight for CORS */
	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		resp.setHeader("Access-Control-Allow-Origin",  "*");
		resp.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
		resp.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
		resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}

	// ── Helpers ──────────────────────────────────────────────────────────────

	private static String buildBaseUrl(HttpServletRequest req)
	{
		// When behind a reverse proxy (nginx, Apache, AWS ALB, etc.) the raw
		// request sees the internal address.  Prefer the forwarded headers so
		// the returned "resource" URI matches what the MCP client connected to.

		// 1. Scheme
		String scheme = req.getHeader("X-Forwarded-Proto");
		if (scheme == null || scheme.isEmpty())
			scheme = req.getScheme();
		// X-Forwarded-Proto may be a comma-separated list; take the first value
		int comma = scheme.indexOf(',');
		if (comma > 0)
			scheme = scheme.substring(0, comma).trim();

		// 2. Host (may already include port, e.g. "example.com:8443")
		String host = req.getHeader("X-Forwarded-Host");
		if (host == null || host.isEmpty())
		{
			host = req.getServerName();
			int     port = req.getServerPort();
			boolean dflt = ("http".equals(scheme) && port == 80)
			            || ("https".equals(scheme) && port == 443);
			if (!dflt)
				host = host + ":" + port;
		}
		else
		{
			// X-Forwarded-Host may also be comma-separated; take the first value
			int c = host.indexOf(',');
			if (c > 0)
				host = host.substring(0, c).trim();
		}

		return scheme + "://" + host;
	}
}
