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
import java.lang.invoke.MethodHandles;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.oauth.OAuthProvider;
import com.dbxtune.central.oauth.OAuthProviderRegistry;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

/**
 * Initiates the OAuth2/OIDC login flow.
 * <p>
 * Mapped to {@code GET /oauth/start?provider=&lt;id&gt;}.
 * <ol>
 *   <li>Looks up the named provider in {@link OAuthProviderRegistry}.</li>
 *   <li>Generates a random CSRF-protection {@code state} nonce and stores it in
 *       the HTTP session under {@code oauth.state}.</li>
 *   <li>Also stores the {@code returnUrl} (if supplied by the caller) so the
 *       callback can redirect the user back to their original page.</li>
 *   <li>Redirects the browser to the provider's authorization URL.</li>
 * </ol>
 */
public class OAuthStartServlet
extends HttpServlet
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long serialVersionUID = 1L;

	/** Session attribute key for the CSRF state nonce. */
	public static final String SESSION_OAUTH_STATE    = "oauth.state";

	/** Session attribute key for the provider ID being used. */
	public static final String SESSION_OAUTH_PROVIDER = "oauth.provider";

	/** Session attribute key for the redirect URI that was used (must match callback). */
	public static final String SESSION_OAUTH_REDIRECT = "oauth.redirectUri";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		String providerId = req.getParameter("provider");
		if (providerId == null || providerId.isEmpty())
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'provider' parameter");
			return;
		}

		OAuthProvider provider = OAuthProviderRegistry.getInstance().getProvider(providerId);
		if (provider == null || !provider.isEnabled())
		{
			_logger.warn("OAuthStartServlet: unknown or disabled provider '{}'", providerId);
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "OAuth provider not available: " + providerId);
			return;
		}

		// Generate CSRF state nonce
		String state = UUID.randomUUID().toString();

		// Build the redirect URI pointing back to our callback servlet
		String redirectUri = buildRedirectUri(req);

		// Store state + provider + redirectUri in the session (callback will verify)
		HttpSession session = req.getSession(true);
		session.setAttribute(SESSION_OAUTH_STATE,    state);
		session.setAttribute(SESSION_OAUTH_PROVIDER, providerId);
		session.setAttribute(SESSION_OAUTH_REDIRECT, redirectUri);

		String authUrl = provider.buildAuthorizationUrl(state, redirectUri);
		_logger.debug("OAuthStartServlet: redirecting to {} auth URL for provider '{}'", provider.getDisplayName(), providerId);

		resp.sendRedirect(authUrl);
	}

	/**
	 * Builds the absolute callback URL for the current request's scheme/host/port.
	 * Example: {@code https://myserver.example.com/oauth/callback}
	 * <p>
	 * If {@code DbxCentral.baseUrl} is set in the config it is used instead of
	 * auto-detection (useful when running behind a reverse proxy).
	 */
	static String buildRedirectUri(HttpServletRequest req)
	{
//		String baseUrl = Configuration.getCombinedConfiguration().getProperty("DbxCentral.baseUrl", "").trim();

		// First get property 'DbxCentral.public.base.url', override with 'DbxCentral.baseUrl'
		String baseUrl = Configuration.getCombinedConfiguration().getProperty("DbxCentral.public.base.url", "");
		baseUrl        = Configuration.getCombinedConfiguration().getProperty("DbxCentral.baseUrl"        , baseUrl);

		if (!baseUrl.isEmpty())
		{
			// Strip trailing slash then append callback path
			return baseUrl.replaceAll("/+$", "") + "/oauth/callback";
		}

		// Auto-detect from the incoming request
		StringBuilder sb = new StringBuilder();
		sb.append(req.getScheme()).append("://").append(req.getServerName());
		int port = req.getServerPort();
		boolean isDefaultPort = ("https".equals(req.getScheme()) && port == 443)
		                     || ("http" .equals(req.getScheme()) && port == 80);
		if (!isDefaultPort)
			sb.append(':').append(port);
		sb.append(req.getContextPath()).append("/oauth/callback");

		return sb.toString();
	}
}
