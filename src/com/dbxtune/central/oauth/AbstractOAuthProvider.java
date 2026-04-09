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

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.utils.Configuration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base class for OIDC-compatible OAuth2 providers.
 * <p>
 * Uses Java 11's built-in {@link java.net.http.HttpClient} - no extra
 * dependencies required beyond what is already in the classpath.
 * <p>
 * Handles:
 * <ul>
 *   <li>Config-property reading via the existing {@link Configuration} singleton</li>
 *   <li>Authorization URL construction (RFC 6749)</li>
 *   <li>Authorization-code → token exchange</li>
 *   <li>ID-token JWT payload decode (Base64URL + Jackson) to extract the email claim</li>
 * </ul>
 * Concrete subclasses only need to supply endpoints and config-property keys.
 */
public abstract class AbstractOAuthProvider
implements OAuthProvider
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	// Shared HttpClient instance - thread-safe and reusable
	private static final HttpClient _httpClient = HttpClient.newHttpClient();

	// -----------------------------------------------------------------------
	// Abstract template methods - implemented by each provider subclass
	// -----------------------------------------------------------------------

	/** OAuth2 authorization endpoint URL. */
	protected abstract String getAuthorizationEndpoint();

	/** OAuth2 token endpoint URL. */
	protected abstract String getTokenEndpoint();

	/** OAuth2 scope string, e.g. {@code "openid email profile"}. */
	protected abstract String getScope();

	/** Config-property key for the client ID, e.g. {@code "DbxCentral.login.entra.clientId"}. */
	protected abstract String getPropClientId();

	/** Config-property key for the client secret. */
	protected abstract String getPropClientSecret();

	/** Config-property key for the {@code enabled} flag. */
	protected abstract String getPropEnabled();

	// -----------------------------------------------------------------------
	// Config helpers
	// -----------------------------------------------------------------------

	protected String cfg(String key)
	{
		return Configuration.getCombinedConfiguration().getProperty(key, "").trim();
	}

	protected boolean cfgBool(String key)
	{
		return "true".equalsIgnoreCase(cfg(key));
	}

	@Override
	public boolean isEnabled()
	{
		return cfgBool(getPropEnabled())
		       && !cfg(getPropClientId()).isEmpty()
		       && !cfg(getPropClientSecret()).isEmpty();
	}

	// -----------------------------------------------------------------------
	// OAuthProvider default implementations
	// -----------------------------------------------------------------------

	@Override
	public String buildAuthorizationUrl(String state, String redirectUri)
	{
		String clientId = cfg(getPropClientId());
		return getAuthorizationEndpoint()
		     + "?response_type=code"
		     + "&client_id="    + urlEnc(clientId)
		     + "&redirect_uri=" + urlEnc(redirectUri)
		     + "&scope="        + urlEnc(getScope())
		     + "&state="        + urlEnc(state)
		     + "&prompt=select_account";
	}

	@Override
	public String exchangeCodeForEmail(String code, String redirectUri)
	{
		String clientId     = cfg(getPropClientId());
		String clientSecret = cfg(getPropClientSecret());

		// Build application/x-www-form-urlencoded body
		Map<String, String> params = new LinkedHashMap<>();
		params.put("grant_type",    "authorization_code");
		params.put("code",          code);
		params.put("redirect_uri",  redirectUri);
		params.put("client_id",     clientId);
		params.put("client_secret", clientSecret);
		params.put("scope",         getScope());

		String body = params.entrySet().stream()
		    .map(e -> urlEnc(e.getKey()) + "=" + urlEnc(e.getValue()))
		    .collect(Collectors.joining("&"));

		try
		{
			HttpRequest request = HttpRequest.newBuilder()
			    .uri(URI.create(getTokenEndpoint()))
			    .header("Content-Type", "application/x-www-form-urlencoded")
			    .header("Accept", "application/json")
			    .POST(HttpRequest.BodyPublishers.ofString(body))
			    .build();

			HttpResponse<String> response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			String json = response.body();
			_logger.debug("exchangeCodeForEmail({}): status={}, body={}", getProviderId(), response.statusCode(), json);

			if (response.statusCode() != 200)
			{
				_logger.warn("exchangeCodeForEmail({}): token endpoint returned HTTP {}: {}", getProviderId(), response.statusCode(), json);
				return null;
			}

			return extractEmailFromTokenResponse(json);
		}
		catch (Exception ex)
		{
			_logger.error("exchangeCodeForEmail({}): HTTP call failed", getProviderId(), ex);
			return null;
		}
	}

	// -----------------------------------------------------------------------
	// JWT / ID-token parsing
	// -----------------------------------------------------------------------

	/**
	 * Extracts the {@code email} claim from the OIDC token response JSON.
	 * Sub-classes may override this (e.g. for providers that need a separate
	 * userinfo API call instead of an ID token).
	 */
	protected String extractEmailFromTokenResponse(String tokenResponseJson)
	{
		try
		{
			ObjectMapper om = new ObjectMapper();
			JsonNode root = om.readTree(tokenResponseJson);

			// ---- try id_token (OIDC) first ----
			JsonNode idTokenNode = root.get("id_token");
			if (idTokenNode != null && !idTokenNode.isNull())
			{
				String email = extractEmailFromJwt(idTokenNode.asText());
				if (email != null)
					return email;
			}

			// ---- fallback: email field directly in token response ----
			JsonNode emailNode = root.get("email");
			if (emailNode != null && !emailNode.isNull())
				return emailNode.asText().trim().toLowerCase();

			_logger.warn("extractEmailFromTokenResponse({}): no email found in token response", getProviderId());
			return null;
		}
		catch (Exception ex)
		{
			_logger.error("extractEmailFromTokenResponse({}): failed to parse token response", getProviderId(), ex);
			return null;
		}
	}

	/**
	 * Decodes the payload section of a JWT (header.payload.sig) and returns
	 * the {@code email} claim.  We trust the HTTPS token-endpoint response so
	 * signature verification is not required here.
	 */
	protected String extractEmailFromJwt(String jwt)
	{
		if (jwt == null || jwt.isEmpty())
			return null;
		try
		{
			String[] parts = jwt.split("\\.");
			if (parts.length < 2)
			{
				_logger.warn("extractEmailFromJwt({}): JWT has fewer than 2 parts", getProviderId());
				return null;
			}
			// Java 11 Base64.getUrlDecoder() handles Base64URL without padding
			byte[] payloadBytes = Base64.getUrlDecoder().decode(padBase64Url(parts[1]));
			String payloadJson  = new String(payloadBytes, StandardCharsets.UTF_8);
			_logger.debug("extractEmailFromJwt({}): payload={}", getProviderId(), payloadJson);

			ObjectMapper om = new ObjectMapper();
			JsonNode payload = om.readTree(payloadJson);

			// email claim (standard OIDC)
			JsonNode emailNode = payload.get("email");
			if (emailNode != null && !emailNode.isNull())
				return emailNode.asText().trim().toLowerCase();

			// upn - User Principal Name used by Entra ID
			JsonNode upnNode = payload.get("upn");
			if (upnNode != null && !upnNode.isNull())
				return upnNode.asText().trim().toLowerCase();

			// preferred_username - another Entra ID claim
			JsonNode prefNode = payload.get("preferred_username");
			if (prefNode != null && !prefNode.isNull())
			{
				String pref = prefNode.asText().trim().toLowerCase();
				if (pref.contains("@"))
					return pref;
			}

			_logger.warn("extractEmailFromJwt({}): no email/upn/preferred_username in JWT payload", getProviderId());
			return null;
		}
		catch (Exception ex)
		{
			_logger.error("extractEmailFromJwt({}): JWT decode failed", getProviderId(), ex);
			return null;
		}
	}

	// -----------------------------------------------------------------------
	// Utility
	// -----------------------------------------------------------------------

	protected static String urlEnc(String value)
	{
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	/** Adds Base64 padding characters if needed so Java's decoder accepts it. */
	private static String padBase64Url(String s)
	{
		switch (s.length() % 4)
		{
			case 2:  return s + "==";
			case 3:  return s + "=";
			default: return s;
		}
	}
}
