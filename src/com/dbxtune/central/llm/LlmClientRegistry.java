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
package com.dbxtune.central.llm;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Singleton registry of all known {@link LlmClient} implementations.
 * <p>
 * Mirrors {@code com.dbxtune.central.oauth.OAuthProviderRegistry}.
 * <p>
 * Holds one instance of each built-in type ({@link #BUILT_IN_TYPES}), plus the extra providers configured in
 * {@link #PROPKEY_EXTRA_PROVIDERS}. To add a new type: add it to {@link #BUILT_IN_TYPES} and {@code createClient()}.
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

	private static volatile boolean _remoteEnabledCache   = false;
	private static volatile long    _remoteEnabledCacheAt = 0L;
	private static final    long    REMOTE_ENABLED_CACHE_MS = 60_000L;

	/**
	 * Same as {@link #isFeatureEnabled()}, but for callers that might be running inside a Collector
	 * process rather than DbxCentral itself - Daily Summary Report generation is the case in point:
	 * it can happen either on-demand from within DbxCentral ({@code DailySummartReportServlet}) or on
	 * schedule from within a Collector during rollover ({@code PersistWriterJdbc.createDailySummaryReport()}).
	 * <p>
	 * {@code DbxCentral.llm.*} properties are meant to be configured centrally, on DbxCentral - not
	 * duplicated into every Collector's own config file. So if the LOCAL config doesn't have the
	 * feature enabled, this asks DbxCentral itself, via the same unauthenticated {@code /api/llm/config}
	 * endpoint the browser-side {@code dbxLlmAdvice.js} already polls (see {@code LlmConfigServlet}),
	 * rather than requiring the property to be set again per-Collector.
	 * <p>
	 * Local {@code true} always wins immediately, with no remote call - this is both the fast path and
	 * the correct one when already running inside DbxCentral itself (its own {@code /api/llm/config}
	 * handler, and the other LLM servlets, all gate on the plain {@link #isFeatureEnabled()} before ever
	 * reaching code that calls this method, so there's no self-call risk there). A remote check is only
	 * attempted when local reads {@code false}, and its result is cached briefly ({@link #REMOTE_ENABLED_CACHE_MS})
	 * since Daily Summary Report generation can call this once per top-N row - without caching, that
	 * would mean one HTTP round-trip to DbxCentral per row. Fails closed (disabled) if DbxCentral can't
	 * be reached, consistent with this feature defaulting to disabled until explicitly opted into.
	 */
	public static boolean isFeatureEnabledViaDbxCentral()
	{
		if (isFeatureEnabled())
			return true;

		long now = System.currentTimeMillis();
		if (now - _remoteEnabledCacheAt < REMOTE_ENABLED_CACHE_MS)
			return _remoteEnabledCache;

		boolean remoteEnabled = fetchRemoteEnabled();
		_remoteEnabledCache   = remoteEnabled;
		_remoteEnabledCacheAt = now;
		return remoteEnabled;
	}

	private static boolean fetchRemoteEnabled()
	{
		String baseUrl = DailySummaryReportAbstract.getDbxCentralInternalBaseUrl();
		if (StringUtil.isNullOrBlank(baseUrl))
			return false;

		String url = baseUrl + "/api/llm/config";
		try
		{
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(url))
					.timeout(Duration.ofSeconds(5))
					.GET()
					.build();

			// Reuses the shared HttpClient LlmClientAbstract already uses to call out to LLM
			// providers - same package, protected access, no reason for a second client instance.
			HttpResponse<String> response = LlmClientAbstract._httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200)
			{
				_logger.warn("isFeatureEnabledViaDbxCentral(): {} returned HTTP {}, treating the feature as disabled.", url, response.statusCode());
				return false;
			}

			JsonNode node = new ObjectMapper().readTree(response.body());
			return node.path("enabled").asBoolean(false);
		}
		catch (Exception ex)
		{
			_logger.warn("isFeatureEnabledViaDbxCentral(): could not reach {} ({}), treating the feature as disabled.", url, ex.toString());
			return false;
		}
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

	/**
	 * More providers of a built-in type, for services that speak the same API as one of them (Mistral, DeepSeek, Groq,
	 * OpenRouter, a company gateway, a second Azure resource, ...). A comma separated list of names of our own; each one
	 * is configured like a built-in provider, under its own name, plus its type:
	 * <pre>
	 * DbxCentral.llm.extraProviders=mistral
	 * DbxCentral.llm.mistral.type=openai
	 * DbxCentral.llm.mistral.url=https://api.mistral.ai/v1
	 * DbxCentral.llm.mistral.apiKey=...
	 * DbxCentral.llm.mistral.model=mistral-large-latest
	 * </pre>
	 */
	public static final String  PROPKEY_EXTRA_PROVIDERS = "DbxCentral.llm.extraProviders";

	/** The built-in types, in dropdown order. Each has a built-in instance with the type name as its id. */
	public static final List<String> BUILT_IN_TYPES = Collections.unmodifiableList(Arrays.asList(
			LlmClientClaude .TYPE,
			LlmClientOpenAi .TYPE,
			LlmClientGemini .TYPE,
			LlmClientVertex .TYPE,
			LlmClientBedrock.TYPE,
			LlmClientFoundry.TYPE,
			LlmClientOllama .TYPE));

	private static final LlmClientRegistry _instance = new LlmClientRegistry();

	/** All known clients, keyed by provider id, in dropdown order. Rebuilt when the 'extraProviders' setting changes. */
	private volatile Map<String, LlmClient> _all;
	private volatile String                 _allBuiltForExtraProviders;

	private LlmClientRegistry()
	{
	}

	public static LlmClientRegistry getInstance()
	{
		return _instance;
	}

	// -----------------------------------------------------------------------
	// Public API
	// -----------------------------------------------------------------------

	/** Returns all clients whose {@link LlmClient#isEnabled()} returns {@code true}, in dropdown order. */
	public List<LlmClient> getEnabledClients()
	{
		List<LlmClient> result = new ArrayList<>();
		for (LlmClient c : getAll().values())
		{
			if (c.isEnabled())
				result.add(c);
		}
		return Collections.unmodifiableList(result);
	}

	/**
	 * Returns the client for the given provider id. If {@code providerId} is blank, falls back to:
	 * <ol>
	 *   <li>the first enabled client listed in {@link #PROPKEY_DEFAULT_PROVIDER} (one name, or a comma separated
	 *       list in order of preference)</li>
	 *   <li>otherwise the first enabled client (has its API key / URL / ... configured), in dropdown order</li>
	 * </ol>
	 * A name that is not a provider, or a list where none is configured, is logged as a warning (once per value).
	 * Returns {@code null} if unknown, or if no provider is enabled.
	 */
	public LlmClient getProvider(String providerId)
	{
		if (StringUtil.hasValue(providerId))
			return getAll().get(providerId);

		List<LlmClient> enabled = getEnabledClients();
		LlmClient firstEnabled = enabled.isEmpty() ? null : enabled.get(0);

		String defaultIds = Configuration.getCombinedConfiguration().getProperty(PROPKEY_DEFAULT_PROVIDER, "").trim();
		if (StringUtil.isNullOrBlank(defaultIds))
			return firstEnabled;

		List<String> unknown = new ArrayList<>();
		LlmClient    chosen  = null;
		for (String id : StringUtil.commaStrToList(defaultIds))
		{
			LlmClient c = getAll().get(id);
			if (c == null && ! id.isEmpty())
				unknown.add(id);
			else if (c != null && c.isEnabled() && chosen == null)
				chosen = c;
		}

		// A typo, or none of them configured: say so in the log (once per value, this is called on every page load)
		LlmClient result = chosen != null ? chosen : firstEnabled;
		String    using  = result == null ? "none" : result.getProviderId();
		if (( ! unknown.isEmpty() || chosen == null ) && _warnedDefaultProviders.add(defaultIds))
		{
			if ( ! unknown.isEmpty() )
				_logger.warn("'{}={}': {} is not an LLM provider (known: {}). Using: {}.", PROPKEY_DEFAULT_PROVIDER, defaultIds, unknown, getAll().keySet(), using);
			else
				_logger.warn("'{}={}': not configured (API key, URL, ... missing). Using the first configured provider instead: {}.", PROPKEY_DEFAULT_PROVIDER, defaultIds, using);
		}
		return result;
	}

	/** Values of PROPKEY_DEFAULT_PROVIDER already warned about in getProvider() */
	private static final Set<String> _warnedDefaultProviders = ConcurrentHashMap.newKeySet();

	// -----------------------------------------------------------------------
	// Client registration
	// -----------------------------------------------------------------------

	private Map<String, LlmClient> getAll()
	{
		String extraProviders = Configuration.getCombinedConfiguration().getProperty(PROPKEY_EXTRA_PROVIDERS, "").trim();
		Map<String, LlmClient> all = _all;
		if (all == null || ! extraProviders.equals(_allBuiltForExtraProviders))
		{
			synchronized (this)
			{
				all = buildAllClients(extraProviders);
				_all = all;
				_allBuiltForExtraProviders = extraProviders;
			}
		}
		return all;
	}

	private static Map<String, LlmClient> buildAllClients(String extraProviders)
	{
		Map<String, LlmClient> map = new LinkedHashMap<>();

		for (String type : BUILT_IN_TYPES)
			map.put(type, createClient(type, type));

		for (String id : StringUtil.parseCommaStrToList(extraProviders, true))
		{
			String type = Configuration.getCombinedConfiguration().getProperty(LlmClientAbstract.PROPKEY_PREFIX + id + ".type", "").trim();

			if ( ! id.matches("[A-Za-z0-9_-]+") )
				_logger.warn("LLM extra provider '{}' in '{}' is skipped: use only letters, digits, '-' and '_' in the name.", id, PROPKEY_EXTRA_PROVIDERS);
			else if (map.containsKey(id))
				_logger.warn("LLM extra provider '{}' in '{}' is skipped: the name is already used (by a built-in provider, or listed twice).", id, PROPKEY_EXTRA_PROVIDERS);
			else if ( ! BUILT_IN_TYPES.contains(type) )
				_logger.warn("LLM extra provider '{}' is skipped: '{}{}.type' must be one of {}, it is '{}'.", id, LlmClientAbstract.PROPKEY_PREFIX, id, BUILT_IN_TYPES, type);
			else
				map.put(id, createClient(type, id));
		}

		_logger.debug("LlmClientRegistry: registered {} client(s): {}", map.size(), map.keySet());
		return Collections.unmodifiableMap(map);
	}

	private static LlmClient createClient(String type, String id)
	{
		switch (type)
		{
		case LlmClientClaude .TYPE: return new LlmClientClaude (id);
		case LlmClientOpenAi .TYPE: return new LlmClientOpenAi (id);
		case LlmClientGemini .TYPE: return new LlmClientGemini (id);
		case LlmClientVertex .TYPE: return new LlmClientVertex (id);
		case LlmClientBedrock.TYPE: return new LlmClientBedrock(id);
		case LlmClientFoundry.TYPE: return new LlmClientFoundry(id);
		case LlmClientOllama .TYPE: return new LlmClientOllama (id);
		default: throw new IllegalArgumentException("Unknown LLM provider type '" + type + "'.");
		}
	}
}
