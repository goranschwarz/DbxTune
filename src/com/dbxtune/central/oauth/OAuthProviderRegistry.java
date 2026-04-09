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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Singleton registry of all known {@link OAuthProvider} implementations.
 * <p>
 * {@link #getEnabledProviders()} returns only providers that have
 * {@code enabled=true} <em>and</em> a valid clientId + clientSecret set
 * in {@code dbxtune.properties} (or the combined config).
 * <p>
 * To add a new provider, register it in {@link #buildAllProviders()}.
 */
public class OAuthProviderRegistry
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private static final OAuthProviderRegistry _instance = new OAuthProviderRegistry();

	/** All known providers, keyed by provider ID. Preserves registration order. */
	private final Map<String, OAuthProvider> _all;

	private OAuthProviderRegistry()
	{
		_all = buildAllProviders();
	}

	public static OAuthProviderRegistry getInstance()
	{
		return _instance;
	}

	// -----------------------------------------------------------------------
	// Public API
	// -----------------------------------------------------------------------

	/**
	 * Returns all providers whose {@link OAuthProvider#isEnabled()} returns
	 * {@code true} (i.e. configured and enabled in {@code dbxtune.properties}).
	 */
	public List<OAuthProvider> getEnabledProviders()
	{
		List<OAuthProvider> result = new ArrayList<>();
		for (OAuthProvider p : _all.values())
		{
			if (p.isEnabled())
				result.add(p);
		}
		return Collections.unmodifiableList(result);
	}

	/**
	 * Returns the provider with the given ID, or {@code null} if unknown.
	 * Does <em>not</em> check whether the provider is enabled.
	 */
	public OAuthProvider getProvider(String providerId)
	{
		return _all.get(providerId);
	}

	// -----------------------------------------------------------------------
	// Provider registration
	// -----------------------------------------------------------------------

	/**
	 * Returns a map of all supported providers in the desired display order.
	 * Add new provider implementations here.
	 */
	private static Map<String, OAuthProvider> buildAllProviders()
	{
		Map<String, OAuthProvider> map = new LinkedHashMap<>();

		register(map, new EntraIdOAuthProvider());
		register(map, new GoogleOAuthProvider());
		// To add more: register(map, new GitHubOAuthProvider());

		_logger.debug("OAuthProviderRegistry: registered {} provider(s): {}", map.size(), map.keySet());
		return Collections.unmodifiableMap(map);
	}

	private static void register(Map<String, OAuthProvider> map, OAuthProvider provider)
	{
		map.put(provider.getProviderId(), provider);
	}
}
