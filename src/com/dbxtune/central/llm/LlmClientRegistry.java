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
package com.dbxtune.central.llm;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

/**
 * Singleton registry of all known {@link LlmClient} implementations.
 * <p>
 * Mirrors {@code com.dbxtune.central.oauth.OAuthProviderRegistry}.
 * <p>
 * To add a new provider, register it in {@link #buildAllClients()}.
 */
public class LlmClientRegistry
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * Master feature toggle for the whole "LLM Optimization Advice" feature. Defaults to {@code false} -
	 * this feature is still rough around the edges in places, so it must be explicitly opted into.
	 * When disabled: the API servlets reject requests, {@code /llm-advice} shows a "not enabled" page,
	 * and no trigger UI (DSR links, Active Statements buttons, Showplan section) renders at all.
	 */
	public static final String  PROPKEY_ENABLED = "DbxCentral.llm.enabled";
	public static final boolean DEFAULT_ENABLED = false;

	/** Whether the LLM feature is enabled at all, per {@link #PROPKEY_ENABLED}. Check this first. */
	public static boolean isFeatureEnabled()
	{
		return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_ENABLED, DEFAULT_ENABLED);
	}

	public static final String  PROPKEY_DEFAULT_PROVIDER = "DbxCentral.llm.provider";

	/** If true, the LLM endpoints can be used by non-logged-in callers. Defaults to false (login required). */
	public static final String  PROPKEY_ALLOW_ANONYMOUS = "DbxCentral.llm.allowAnonymous";
	public static final boolean DEFAULT_ALLOW_ANONYMOUS  = false;

	/** Whether the LLM endpoints should be reachable without a logged-in user, per {@link #PROPKEY_ALLOW_ANONYMOUS}. */
	public static boolean isAnonymousAccessAllowed()
	{
		return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_ALLOW_ANONYMOUS, DEFAULT_ALLOW_ANONYMOUS);
	}

	private static final LlmClientRegistry _instance = new LlmClientRegistry();

	/** All known clients, keyed by provider id. Preserves registration order. */
	private final Map<String, LlmClient> _all;

	private LlmClientRegistry()
	{
		_all = buildAllClients();
	}

	public static LlmClientRegistry getInstance()
	{
		return _instance;
	}

	// -----------------------------------------------------------------------
	// Public API
	// -----------------------------------------------------------------------

	/** Returns all clients whose {@link LlmClient#isEnabled()} returns {@code true}. */
	public List<LlmClient> getEnabledClients()
	{
		List<LlmClient> result = new ArrayList<>();
		for (LlmClient c : _all.values())
		{
			if (c.isEnabled())
				result.add(c);
		}
		return Collections.unmodifiableList(result);
	}

	/**
	 * Returns the client for the given provider id. If {@code providerId} is blank, falls back to:
	 * <ol>
	 *   <li>the client configured as {@link #PROPKEY_DEFAULT_PROVIDER}, if set</li>
	 *   <li>otherwise, if exactly one client is enabled (has a valid API key/URL configured), that one -
	 *       so a single-provider setup works without also having to set {@link #PROPKEY_DEFAULT_PROVIDER}</li>
	 * </ol>
	 * Returns {@code null} if unknown, or if none/multiple providers are enabled and no default was set.
	 */
	public LlmClient getProvider(String providerId)
	{
		if (StringUtil.hasValue(providerId))
			return _all.get(providerId);

		String defaultId = Configuration.getCombinedConfiguration().getProperty(PROPKEY_DEFAULT_PROVIDER, "");
		if (StringUtil.hasValue(defaultId))
			return _all.get(defaultId);

		List<LlmClient> enabled = getEnabledClients();
		if (enabled.size() == 1)
			return enabled.get(0);

		return null;
	}

	// -----------------------------------------------------------------------
	// Client registration
	// -----------------------------------------------------------------------

	private static Map<String, LlmClient> buildAllClients()
	{
		Map<String, LlmClient> map = new LinkedHashMap<>();

		register(map, new LlmClientClaude());
		register(map, new LlmClientOpenAi());
		register(map, new LlmClientGemini());
		register(map, new LlmClientOllama());

		_logger.debug("LlmClientRegistry: registered {} client(s): {}", map.size(), map.keySet());
		return Collections.unmodifiableMap(map);
	}

	private static void register(Map<String, LlmClient> map, LlmClient client)
	{
		map.put(client.getProviderId(), client);
	}
}
