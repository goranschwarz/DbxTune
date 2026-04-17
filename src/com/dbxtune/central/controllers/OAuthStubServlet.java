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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.mcp.McpController;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Minimal OAuth 2.0 Authorization Server stubs that let {@code mcp-remote}
 * complete its mandatory OAuth dance and obtain the configured MCP API token
 * as the resulting access token — without requiring a real OAuth provider.
 *
 * <p>Endpoints (all mapped in {@code web.xml}):
 * <pre>
 *   GET  /.well-known/oauth-authorization-server  — AS discovery metadata (RFC 8414)
 *   POST /register                                — dynamic client registration (RFC 7591)
 *   GET  /oauth/authorize                         — auto-approve; redirect with code immediately
 *   POST /oauth/token                             — PKCE verify (S256); return configured API token
 * </pre>
 *
 * <p>Flow:
 * <ol>
 *   <li>{@code mcp-remote} discovers {@code /.well-known/oauth-protected-resource}
 *       (served by {@link OAuthProtectedResourceServlet})</li>
 *   <li>With empty {@code authorization_servers} it falls back to the resource server
 *       origin and looks for {@code /.well-known/oauth-authorization-server} here.</li>
 *   <li>Registers a client via {@code POST /register} → gets a dummy {@code client_id}.</li>
 *   <li>Redirects the user's browser to {@code GET /oauth/authorize} → we auto-approve
 *       and redirect back to the mcp-remote callback URL with an authorization code.</li>
 *   <li>Exchanges the code for a token via {@code POST /oauth/token} with PKCE verifier
 *       → we verify S256 and return the configured {@code DbxTuneCentral.mcp.apiToken}
 *       as the {@code access_token}.  mcp-remote caches this token.</li>
 * </ol>
 *
 * <p>Security notes: this is intentionally a stub — it is only reachable when
 * {@code DbxTuneCentral.mcp.enabled=true} and is gated by the same
 * {@code DbxTuneCentral.mcp.allowedHosts} IP check in {@link McpController}.
 */
public class OAuthStubServlet
extends HttpServlet
{
	private static final long   serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	// ── In-memory auth-code store ─────────────────────────────────────────────
	// Keyed by authorization code; entries expire after 5 minutes.
	private static final ConcurrentHashMap<String, CodeEntry> _codes = new ConcurrentHashMap<>();

	static final class CodeEntry
	{
		final String codeChallenge;   // BASE64URL(SHA256(verifier))
		final String redirectUri;
		final long   expiresAt;

		CodeEntry(String cc, String ru)
		{
			this.codeChallenge = cc;
			this.redirectUri   = ru != null ? ru : "";
			this.expiresAt     = System.currentTimeMillis() + 5 * 60 * 1_000L;
		}
		boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
	}

	// ── Servlet state ─────────────────────────────────────────────────────────
	private String       _apiToken;
	private ObjectMapper _om = new ObjectMapper();

	// ── Lifecycle ─────────────────────────────────────────────────────────────

	@Override
	public void init()
	throws ServletException
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		_apiToken = conf.getProperty(McpController.PROPKEY_apiToken, McpController.DEFAULT_apiToken);
		_logger.info("OAuthStubServlet initialized (minimal OAuth 2.0 stub for mcp-remote)");
	}

	// ── Routing ───────────────────────────────────────────────────────────────

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		String uri = req.getRequestURI();
		if (uri.contains("oauth-authorization-server"))
			serveAsMetadata(req, resp);
		else if (uri.contains("/oauth/authorize"))
			handleAuthorize(req, resp);
		else
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		String uri = req.getRequestURI();
		if (uri.endsWith("/register"))
			handleRegister(req, resp);
		else if (uri.endsWith("/oauth/token"))
			handleToken(req, resp);
		else
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

	/** OPTIONS pre-flight for any CORS-aware client */
	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		resp.setHeader("Access-Control-Allow-Origin",  "*");
		resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
		resp.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
		resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}

	// ── Handler methods ───────────────────────────────────────────────────────

	/**
	 * GET /.well-known/oauth-authorization-server
	 * RFC 8414 — Authorization Server Metadata.
	 */
	private void serveAsMetadata(HttpServletRequest req, HttpServletResponse resp)
	throws IOException
	{
		String base = buildBaseUrl(req);

		Map<String, Object> meta = new LinkedHashMap<>();
		meta.put("issuer",                                  base);
		meta.put("authorization_endpoint",                  base + "/oauth/authorize");
		meta.put("token_endpoint",                          base + "/oauth/token");
		meta.put("registration_endpoint",                   base + "/register");
		meta.put("response_types_supported",                new String[]{"code"});
		meta.put("grant_types_supported",                   new String[]{"authorization_code"});
		meta.put("code_challenge_methods_supported",        new String[]{"S256"});
		meta.put("token_endpoint_auth_methods_supported",   new String[]{"none"});

		_logger.debug("OAuthStubServlet: serving AS metadata for issuer={}", base);
		writeJson(resp, HttpServletResponse.SC_OK, meta);
	}

	/**
	 * POST /register
	 * RFC 7591 — Dynamic Client Registration.
	 * We accept any client and echo its redirect_uris back without validation.
	 */
	private void handleRegister(HttpServletRequest req, HttpServletResponse resp)
	throws IOException
	{
		String body = req.getReader().lines().collect(Collectors.joining());
		String redirectUri = "";
		try
		{
			var node = _om.readTree(body);
			var uris = node.path("redirect_uris");
			if (uris.isArray() && uris.size() > 0)
				redirectUri = uris.get(0).asText();
		}
		catch (Exception ignore) { /* treat as no redirect_uris */ }

		Map<String, Object> reg = new LinkedHashMap<>();
		reg.put("client_id",            "dbxcentral-mcp-client");
		reg.put("client_id_issued_at",  System.currentTimeMillis() / 1_000L);
		reg.put("redirect_uris",        redirectUri.isEmpty() ? new String[0] : new String[]{redirectUri});
		reg.put("token_endpoint_auth_method", "none");
		reg.put("grant_types",          new String[]{"authorization_code"});
		reg.put("response_types",       new String[]{"code"});

		_logger.debug("OAuthStubServlet: registered client redirect_uri={}", redirectUri);
		writeJson(resp, HttpServletResponse.SC_CREATED, reg);
	}

	/**
	 * GET /oauth/authorize
	 * Auto-approves every request: stores the PKCE code_challenge and immediately
	 * redirects to the redirect_uri with a fresh authorization code.
	 */
	private void handleAuthorize(HttpServletRequest req, HttpServletResponse resp)
	throws IOException
	{
		String redirectUri   = req.getParameter("redirect_uri");
		String state         = req.getParameter("state");
		String codeChallenge = req.getParameter("code_challenge");
		// code_challenge_method must be S256 per our metadata; we don't validate the method
		// param here since we always use S256 in the token step.

		String code = UUID.randomUUID().toString().replace("-", "");
		_codes.put(code, new CodeEntry(codeChallenge, redirectUri));

		// Purge expired codes opportunistically (no separate cleanup thread needed)
		_codes.entrySet().removeIf(e -> e.getValue().isExpired());

		String callback = (redirectUri != null ? redirectUri : "/")
				+ "?code=" + code
				+ (state != null ? "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8) : "");

		_logger.debug("OAuthStubServlet: auto-approved authorize; redirecting to {}", redirectUri);
		resp.sendRedirect(callback);
	}

	/**
	 * POST /oauth/token
	 * Verifies the PKCE code_verifier (S256) for the authorization code, then
	 * returns the configured MCP API token as the access_token.
	 */
	private void handleToken(HttpServletRequest req, HttpServletResponse resp)
	throws IOException
	{
		String code         = req.getParameter("code");
		String codeVerifier = req.getParameter("code_verifier");
		String grantType    = req.getParameter("grant_type");

		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		resp.setHeader("Access-Control-Allow-Origin", "*");

		if (!"authorization_code".equals(grantType))
		{
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			_om.writeValue(resp.getOutputStream(),
					Map.of("error", "unsupported_grant_type"));
			return;
		}

		CodeEntry entry = (code != null) ? _codes.remove(code) : null;
		if (entry == null || entry.isExpired())
		{
			_logger.warn("OAuthStubServlet: token request with invalid/expired code={}", code);
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			_om.writeValue(resp.getOutputStream(),
					Map.of("error", "invalid_grant", "error_description", "Unknown or expired authorization code"));
			return;
		}

		// Verify PKCE S256: SHA-256(code_verifier) == BASE64URL_DECODE(code_challenge)
		if (codeVerifier != null && !entry.codeChallenge.isEmpty())
		{
			try
			{
				MessageDigest sha = MessageDigest.getInstance("SHA-256");
				byte[]  hash     = sha.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
				String  computed = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
				if (!computed.equals(entry.codeChallenge))
				{
					_logger.warn("OAuthStubServlet: PKCE verification failed for code={}", code);
					resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					_om.writeValue(resp.getOutputStream(),
							Map.of("error", "invalid_grant", "error_description", "PKCE code_verifier mismatch"));
					return;
				}
			}
			catch (NoSuchAlgorithmException e)
			{
				// SHA-256 is mandatory in every Java SE / Jakarta EE runtime; cannot happen
				throw new IOException("SHA-256 unavailable", e);
			}
		}

		String token = StringUtil.isNullOrBlank(_apiToken) ? "no-token-configured" : _apiToken;

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("access_token", token);
		result.put("token_type",   "Bearer");
		result.put("expires_in",   86400 * 365);   // "never" — 1 year; mcp-remote caches this

		_logger.info("OAuthStubServlet: issued access_token to mcp-remote client");
		writeJson(resp, HttpServletResponse.SC_OK, result);
	}

	// ── Helpers ───────────────────────────────────────────────────────────────

	private void writeJson(HttpServletResponse resp, int status, Object body)
	throws IOException
	{
		resp.setStatus(status);
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		resp.setHeader("Access-Control-Allow-Origin", "*");
		_om.writeValue(resp.getOutputStream(), body);
	}

	/**
	 * Build the public base URL, honoring reverse-proxy headers.
	 * Identical logic to {@link OAuthProtectedResourceServlet#buildBaseUrl}.
	 */
	private static String buildBaseUrl(HttpServletRequest req)
	{
		String scheme = req.getHeader("X-Forwarded-Proto");
		if (scheme == null || scheme.isEmpty())
			scheme = req.getScheme();
		int comma = scheme.indexOf(',');
		if (comma > 0)
			scheme = scheme.substring(0, comma).trim();

		String host = req.getHeader("X-Forwarded-Host");
		if (host == null || host.isEmpty())
		{
			host = req.getServerName();
			int     port = req.getServerPort();
			boolean dflt = ("http".equals(scheme)  && port == 80)
			            || ("https".equals(scheme) && port == 443);
			if (!dflt)
				host = host + ":" + port;
		}
		else
		{
			int c = host.indexOf(',');
			if (c > 0)
				host = host.substring(0, c).trim();
		}
		return scheme + "://" + host;
	}
}
