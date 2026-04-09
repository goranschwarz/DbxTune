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
import java.sql.SQLException;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.security.SecurityHandler;

import com.dbxtune.central.oauth.OAuthProvider;
import com.dbxtune.central.oauth.OAuthProviderRegistry;
import com.dbxtune.central.pcs.CentralPcsWriterHandler;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.central.pcs.CentralPersistWriterJdbc;
import com.dbxtune.central.pcs.DbxCentralRealm;
import com.dbxtune.central.pcs.objects.DbxCentralUser;
import com.dbxtune.central.utils.PasswordHashUtil;
import com.dbxtune.utils.StringUtil;

/**
 * Handles the OAuth2/OIDC callback after the user has authenticated with the
 * external provider.
 * <p>
 * Mapped to {@code GET /oauth/callback}.
 * <p>
 * Flow:
 * <ol>
 *   <li>Validate CSRF {@code state} against the value stored in the session by
 *       {@link OAuthStartServlet}.</li>
 *   <li>Exchange the authorization {@code code} for the user's e-mail address
 *       via the provider's token endpoint.</li>
 *   <li>Look up the user in {@code DbxCentralUsers} by e-mail.
 *       If not found, auto-provision a new account with role {@code user}.</li>
 *   <li>Programmatically establish a Jetty session by calling
 *       {@code request.login(username, oneTimeToken)} after registering the token
 *       in {@link DbxCentralRealm}.</li>
 *   <li>Redirect to {@code /index.html} (or {@code sessionStorage.dbxReturnAfterLogin}
 *       which is handled client-side by {@code dbxLoginModal.js}).</li>
 * </ol>
 */
public class OAuthCallbackServlet
extends HttpServlet
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		// ----------------------------------------------------------------
		// 1. Read callback parameters
		// ----------------------------------------------------------------
		String code      = req.getParameter("code");
		String state     = req.getParameter("state");
		String error     = req.getParameter("error");
		String errorDesc = req.getParameter("error_description");

		// Provider may return an error (e.g. user cancelled)
		if (!StringUtil.isNullOrBlank(error))
		{
			_logger.warn("OAuthCallbackServlet: provider returned error='{}', description='{}'", error, errorDesc);
			resp.sendRedirect(req.getContextPath() + "/index.html?login=failed");
			return;
		}

		if (StringUtil.isNullOrBlank(code) || StringUtil.isNullOrBlank(state))
		{
			_logger.warn("OAuthCallbackServlet: missing 'code' or 'state' parameter");
			resp.sendRedirect(req.getContextPath() + "/index.html?login=failed");
			return;
		}

		// ----------------------------------------------------------------
		// 2. Validate CSRF state
		// ----------------------------------------------------------------
		HttpSession session = req.getSession(false);
		if (session == null)
		{
			_logger.warn("OAuthCallbackServlet: no session found - possible CSRF or expired session");
			resp.sendRedirect(req.getContextPath() + "/index.html?login=failed");
			return;
		}

		String expectedState = (String) session.getAttribute(OAuthStartServlet.SESSION_OAUTH_STATE);
		String providerId    = (String) session.getAttribute(OAuthStartServlet.SESSION_OAUTH_PROVIDER);
		String redirectUri   = (String) session.getAttribute(OAuthStartServlet.SESSION_OAUTH_REDIRECT);

		// Consume the state immediately (one-time use)
		session.removeAttribute(OAuthStartServlet.SESSION_OAUTH_STATE);
		session.removeAttribute(OAuthStartServlet.SESSION_OAUTH_PROVIDER);
		session.removeAttribute(OAuthStartServlet.SESSION_OAUTH_REDIRECT);

		if (!state.equals(expectedState))
		{
			_logger.warn("OAuthCallbackServlet: state mismatch - possible CSRF attack. expected='{}', got='{}'", expectedState, state);
			resp.sendRedirect(req.getContextPath() + "/index.html?login=failed");
			return;
		}

		// ----------------------------------------------------------------
		// 3. Look up provider
		// ----------------------------------------------------------------
		OAuthProvider provider = OAuthProviderRegistry.getInstance().getProvider(providerId);
		if (provider == null || !provider.isEnabled())
		{
			_logger.warn("OAuthCallbackServlet: provider '{}' not found or disabled", providerId);
			resp.sendRedirect(req.getContextPath() + "/index.html?login=failed");
			return;
		}

		// ----------------------------------------------------------------
		// 4. Exchange code -> email
		// ----------------------------------------------------------------
		if (redirectUri == null || redirectUri.isEmpty())
			redirectUri = OAuthStartServlet.buildRedirectUri(req);

		String email = provider.exchangeCodeForEmail(code, redirectUri);
		if (StringUtil.isNullOrBlank(email))
		{
			_logger.warn("OAuthCallbackServlet: could not obtain email from provider '{}'", providerId);
			resp.sendRedirect(req.getContextPath() + "/index.html?login=failed");
			return;
		}
		_logger.info("OAuthCallbackServlet: provider='{}', email='{}'", providerId, email);

		// ----------------------------------------------------------------
		// 5. Look up or auto-provision the user
		// ----------------------------------------------------------------
		String username;
		String[] roles;
		try
		{
			username = resolveOrCreateUser(email, providerId);
			if (username == null)
			{
				_logger.warn("OAuthCallbackServlet: could not resolve/create user for email '{}'", email);
				resp.sendRedirect(req.getContextPath() + "/index.html?login=failed");
				return;
			}

			DbxCentralUser dbUser = CentralPersistReader.getInstance().getDbxCentralUser(username);
			roles = (dbUser != null) ? dbUser.getRoles() : new String[]{ DbxCentralRealm.ROLE_USER };
		}
		catch (SQLException ex)
		{
			_logger.error("OAuthCallbackServlet: DB error resolving user for email '{}'", email, ex);
			resp.sendRedirect(req.getContextPath() + "/index.html?login=failed");
			return;
		}

		// ----------------------------------------------------------------
		// 6. Establish Jetty session via programmatic login
		// ----------------------------------------------------------------
		DbxCentralRealm realm = getRealm();
		if (realm == null)
		{
			_logger.error("OAuthCallbackServlet: could not obtain DbxCentralRealm - cannot complete login");
			resp.sendRedirect(req.getContextPath() + "/index.html?login=failed");
			return;
		}

		String oneTimeToken = UUID.randomUUID().toString();
		realm.prepareOAuthUserRoles(username, roles);
		realm.registerOAuthLogin(username, oneTimeToken);

		try
		{
			req.login(username, oneTimeToken);
			_logger.info("OAuthCallbackServlet: OAuth login SUCCEEDED for user='{}' via provider='{}'", username, providerId);
		}
		catch (ServletException ex)
		{
			_logger.warn("OAuthCallbackServlet: request.login() failed for user='{}': {}", username, ex.getMessage());
			resp.sendRedirect(req.getContextPath() + "/index.html?login=failed");
			return;
		}

		// ----------------------------------------------------------------
		// 7. Redirect to main page - dbxLoginModal.js will handle the return URL
		// ----------------------------------------------------------------
		resp.sendRedirect(req.getContextPath() + "/index.html");
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	/**
	 * Finds an existing user by email or creates a new one.
	 * Returns the username, or {@code null} on error.
	 */
	private String resolveOrCreateUser(String email, String providerId)
	throws SQLException
	{
		if (!CentralPersistReader.hasInstance() || !CentralPcsWriterHandler.hasInstance())
		{
			_logger.error("resolveOrCreateUser: persistence layer not available");
			return null;
		}

		// Try to find by email first
		DbxCentralUser existing = CentralPersistReader.getInstance().getDbxCentralUserByEmail(email);
		if (existing != null)
		{
			_logger.debug("resolveOrCreateUser: found existing user '{}' for email '{}'", existing.getUsername(), email);
			return existing.getUsername();
		}

		// Auto-provision: derive a username from the email local-part
		String username = deriveUsername(email);

		// Ensure uniqueness - append a short random suffix if needed
		DbxCentralUser byName = CentralPersistReader.getInstance().getDbxCentralUser(username);
		if (byName != null)
		{
			// Username already taken by a different email - add a numeric suffix
			username = username + "_" + Math.abs(email.hashCode() % 10000);
		}

		// Placeholder password: non-guessable, BCrypt-hashed - user cannot log in
		// with it directly; they must always use the OAuth flow.
		// NOTE: Do we really need to use PasswordHashUtil.hashPassword(...) here?
		//       It would be nice to see directly in the database that it's a "password" coming from "external authentication", meaning it has "oauth:" as a prefix
		//       which indicates that it can be used as a "password", and also as an admin... would know that the user was created by "external authentication"
		// And in the DbxCentralRealm.loadUserInfo(final String username): if passwd starts with "oauth:", the form-based login for OAuth-provisioned account is BLOCKED
//		String placeholderPwd = PasswordHashUtil.hashPassword("oauth:" + providerId + ":" + UUID.randomUUID());
		String placeholderPwd = "oauth:" + providerId + ":" + UUID.randomUUID();

		CentralPersistWriterJdbc writer = CentralPcsWriterHandler.getInstance().getJdbcWriter();
		if (writer == null)
		{
			_logger.error("resolveOrCreateUser: JdbcWriter not available");
			return null;
		}

		writer.addDbxCentralUser(username, placeholderPwd, email, DbxCentralRealm.ROLE_USER);
		_logger.info("resolveOrCreateUser: auto-provisioned new user '{}' (email='{}', provider='{}')", username, email, providerId);
		return username;
	}

	/**
	 * Derives a clean username from an email address.
	 * Uses the local-part, lowercased, with non-alphanumeric chars replaced by
	 * underscores, and truncated to 64 characters.
	 */
	private static String deriveUsername(String email)
	{
		String local = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
		String clean = local.toLowerCase().replaceAll("[^a-z0-9_.-]", "_");
		if (clean.length() > 64)
			clean = clean.substring(0, 64);
		return clean;
	}

	/**
	 * Retrieves the running {@link DbxCentralRealm} instance from Jetty's
	 * {@link SecurityHandler} - same pattern as {@link Helper#checkForBasicAuthentication}.
	 */
	private DbxCentralRealm getRealm()
	{
		SecurityHandler security = SecurityHandler.getCurrentSecurityHandler();
		if (security == null)
			return null;
		if (security.getLoginService() instanceof DbxCentralRealm)
			return (DbxCentralRealm) security.getLoginService();
		return null;
	}
}
