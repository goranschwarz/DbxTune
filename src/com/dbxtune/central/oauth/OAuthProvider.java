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
package com.dbxtune.central.oauth;

/**
 * Abstraction for an OAuth2/OIDC login provider (Entra ID, Google, GitHub, …).
 * <p>
 * Adding a new OIDC-compatible provider requires only a small subclass of
 * {@link AbstractOAuthProvider} - about 50 lines - that sets the provider's
 * endpoints and config-property keys.
 */
public interface OAuthProvider
{
	/** Short stable identifier used in URLs, e.g. {@code "entra"} or {@code "google"}. */
	String getProviderId();

	/** Human-readable name shown on the login button, e.g. {@code "Microsoft"}. */
	String getDisplayName();

	/**
	 * Small HTML snippet for the button icon, e.g. an {@code <img>} or an
	 * {@code <i class="fa …">} element.  Must be XSS-safe (no user input).
	 */
	String getIconHtml();

	/** Returns {@code true} when this provider is configured and enabled. */
	boolean isEnabled();

	/**
	 * Builds the provider's authorization URL that the browser should be
	 * redirected to in order to start the login flow.
	 *
	 * @param state       CSRF-protection nonce (store in the user's session before redirecting)
	 * @param redirectUri Absolute URL pointing back to our {@code /oauth/callback} servlet
	 */
	String buildAuthorizationUrl(String state, String redirectUri);

	/**
	 * Exchanges the one-time authorization {@code code} (returned by the provider
	 * after the user consented) for the authenticated user's e-mail address.
	 * <p>
	 * For OIDC providers this is done by calling the token endpoint and decoding
	 * the {@code email} claim from the returned ID-token JWT.
	 * For plain OAuth2 providers (e.g. GitHub) it may require an additional
	 * call to the provider's user-info API.
	 *
	 * @param code        Authorization code from the callback query parameter
	 * @param redirectUri The exact same redirect URI that was used in {@link #buildAuthorizationUrl}
	 * @return The authenticated user's e-mail address, or {@code null} on failure
	 */
	String exchangeCodeForEmail(String code, String redirectUri);
}
