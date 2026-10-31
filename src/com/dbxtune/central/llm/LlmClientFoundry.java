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

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * LLM client for <b>Claude in Microsoft Foundry</b> (Azure): the Claude Messages API on your Foundry resource,
 * billed through the Azure Marketplace. (Other models in Foundry/Azure OpenAI: use the {@code openai} type with
 * the Azure OpenAI v1 URL and {@code authHeader=api-key}.)
 *
 * <h3>Configuration properties</h3>
 * <pre>
 * DbxCentral.llm.foundry.resource = &lt;Foundry resource name&gt;                  (or 'url')
 * DbxCentral.llm.foundry.url      = https://&lt;resource&gt;.services.ai.azure.com/anthropic   (optional, instead of 'resource')
 * DbxCentral.llm.foundry.model    = claude-opus-5-5                            (your DEPLOYMENT names, default shown)
 * DbxCentral.llm.foundry.apiKey   = &lt;key from the deployment's Details tab&gt;   (the default way to log in)
 * </pre>
 * Without 'apiKey' a Microsoft Entra ID token is used (scope {@code https://ai.azure.com/.default}), from the first of:
 * <ol>
 *   <li>a service principal: {@code tenantId} + {@code clientId} + {@code clientSecret} (or env AZURE_TENANT_ID, AZURE_CLIENT_ID, AZURE_CLIENT_SECRET)</li>
 *   <li>workload identity (AKS): env AZURE_FEDERATED_TOKEN_FILE + tenant + client id</li>
 *   <li>managed identity: App Service/Functions (env IDENTITY_ENDPOINT + IDENTITY_HEADER) or an Azure VM (IMDS);
 *       {@code clientId} selects a user-assigned identity</li>
 * </ol>
 * {@code authorityHost} (or env AZURE_AUTHORITY_HOST) is for sovereign clouds, default {@code https://login.microsoftonline.com}.
 */
public class LlmClientFoundry
extends LlmClientAbstract
{
	public static final String TYPE                   = "foundry";
	public static final String DEFAULT_MODEL          = "claude-opus-5-5";
	public static final String DEFAULT_AUTHORITY_HOST = "https://login.microsoftonline.com";
	public static final String ENTRA_RESOURCE         = "https://ai.azure.com";

	private String _entraToken;
	private long   _entraTokenExpiresAt;

	public LlmClientFoundry(String providerId)
	{
		super(providerId);
	}

	@Override public    String getType()               { return TYPE; }
	@Override protected String getDefaultDisplayName() { return "Microsoft Foundry"; }
	@Override protected String getDefaultModel()       { return DEFAULT_MODEL; }

	/** A resource name or a URL (how to log in is checked when called: an API key, or Entra ID). */
	@Override
	public boolean isEnabled()
	{
		return StringUtil.hasValue(cfg(key("resource"))) || StringUtil.hasValue(cfg(key("url")));
	}

	@Override
	public LlmOptimizeResponse optimize(LlmOptimizeRequest request, LlmModelEntry model, LlmProgress progress) throws Exception
	{
		checkModelEntry(model);
		String prompt = buildPrompt(request);

		// Streamed (the default): progress while we wait. The thinking summary only for a model seen thinking, and
		// sent again without it if the model does not take it (see askThinkingSummary(), isThinkingSummaryRejected()).
		boolean askThinkingSummary = askThinkingSummary(model);
		while (true)
		{
			ObjectNode body = buildMessagesBody(model.getModelId(), model.getMaxTokens(), prompt);
			if (askThinkingSummary)
				body.putObject("thinking").put("type", "adaptive").put("display", "summarized");
			applyExtraBody(body, model);
			if (model.isStream())
				body.put("stream", true);

			HttpRequest.Builder builder = HttpRequest.newBuilder()
					.timeout(model.getTimeout())
					.uri(URI.create(getUrl("https://" + cfg(key("resource")) + ".services.ai.azure.com/anthropic") + "/v1/messages"))
					.header("anthropic-version", LlmClientClaude.ANTHROPIC_VERSION)
					.header("content-type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(body), StandardCharsets.UTF_8));
			if (StringUtil.hasValue(getApiKey()))
				builder.header("api-key", getApiKey());
			else
				builder.header("Authorization", "Bearer " + getEntraToken());
			applyExtraHeaders(builder, model);

			if ( ! model.isStream() )
			{
				// Not streamed: wait for the whole answer, at most 'timeout' in total, and stop the call when nobody waits any more
				HttpResponse<String> httpResponse = sendAndWait(builder.build(), model, progress);
				if (httpResponse.statusCode() != 200)
					throw new Exception("Microsoft Foundry returned HTTP " + httpResponse.statusCode() + ": " + httpResponse.body());
				return parseMessagesResponse(httpResponse.body(), prompt, model);
			}

			StreamedAnswer answer = sendMessagesStream(builder.build(), model, progress);
			if (askThinkingSummary && isThinkingSummaryRejected(answer.statusCode, answer.body, model))
			{
				askThinkingSummary = false;
				continue;
			}
			if (answer.statusCode != 200)
				throw new Exception("Microsoft Foundry returned HTTP " + answer.statusCode + ": " + answer.body);
			return parseMessagesResponse(answer.body, prompt, model);
		}
	}

	/** A setting, or the environment variable when the setting is not there. */
	private String cfgOrEnv(String name, String envName)
	{
		String val = cfg(key(name));
		if (StringUtil.isNullOrBlank(val))
			val = System.getenv(envName);
		return val == null ? "" : val.trim();
	}

	/** A Microsoft Entra ID access token for Foundry, cached until 5 minutes before it expires. */
	private synchronized String getEntraToken()
	throws Exception
	{
		if (_entraToken != null && System.currentTimeMillis() < _entraTokenExpiresAt - 5 * 60_000L)
			return _entraToken;

		String tenantId      = cfgOrEnv("tenantId",      "AZURE_TENANT_ID");
		String clientId      = cfgOrEnv("clientId",      "AZURE_CLIENT_ID");
		String clientSecret  = cfgOrEnv("clientSecret",  "AZURE_CLIENT_SECRET");
		String authorityHost = cfgOrEnv("authorityHost", "AZURE_AUTHORITY_HOST");
		String tokenFile     = System.getenv("AZURE_FEDERATED_TOKEN_FILE");
		if (StringUtil.isNullOrBlank(authorityHost))
			authorityHost = DEFAULT_AUTHORITY_HOST;
		while (authorityHost.endsWith("/"))
			authorityHost = authorityHost.substring(0, authorityHost.length() - 1);

		String source;
		HttpRequest tokenRequest;
		if (StringUtil.hasValue(tenantId) && StringUtil.hasValue(clientId) && (StringUtil.hasValue(clientSecret) || StringUtil.hasValue(tokenFile)))
		{
			Map<String, String> form = new LinkedHashMap<>();
			form.put("grant_type", "client_credentials");
			form.put("client_id",  clientId);
			form.put("scope",      ENTRA_RESOURCE + "/.default");
			if (StringUtil.hasValue(clientSecret))
			{
				source = "service principal '" + clientId + "' (client secret)";
				form.put("client_secret", clientSecret);
			}
			else
			{
				source = "workload identity '" + clientId + "' (AZURE_FEDERATED_TOKEN_FILE)";
				form.put("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
				form.put("client_assertion",      new String(Files.readAllBytes(Paths.get(tokenFile)), StandardCharsets.UTF_8).trim());
			}
			String formBody = form.entrySet().stream()
					.map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
					.collect(Collectors.joining("&"));

			tokenRequest = HttpRequest.newBuilder()
					.uri(URI.create(authorityHost + "/" + tenantId + "/oauth2/v2.0/token"))
					.timeout(Duration.ofSeconds(30))
					.header("Content-Type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString(formBody))
					.build();
		}
		else
		{
			String clientIdParam    = StringUtil.hasValue(clientId) ? "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) : "";
			String identityEndpoint = System.getenv("IDENTITY_ENDPOINT");
			String identityHeader   = System.getenv("IDENTITY_HEADER");
			if (StringUtil.hasValue(identityEndpoint) && StringUtil.hasValue(identityHeader))
			{
				source = "managed identity (App Service, IDENTITY_ENDPOINT)";
				tokenRequest = HttpRequest.newBuilder()
						.uri(URI.create(identityEndpoint + "?api-version=2019-08-01&resource=" + URLEncoder.encode(ENTRA_RESOURCE, StandardCharsets.UTF_8) + clientIdParam))
						.timeout(Duration.ofSeconds(10))
						.header("X-IDENTITY-HEADER", identityHeader)
						.GET()
						.build();
			}
			else
			{
				source = "managed identity (Azure VM, IMDS)";
				tokenRequest = HttpRequest.newBuilder()
						.uri(URI.create("http://169.254.169.254/metadata/identity/oauth2/token?api-version=2018-02-01&resource=" + URLEncoder.encode(ENTRA_RESOURCE, StandardCharsets.UTF_8) + clientIdParam))
						.timeout(Duration.ofSeconds(5))
						.header("Metadata", "true")
						.GET()
						.build();
			}
		}

		HttpResponse<String> resp;
		try
		{
			resp = _httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		}
		catch (Exception ex)
		{
			throw new Exception("No 'apiKey' is set for LLM provider '" + getProviderId() + "', and getting a Microsoft Entra ID token from " + source + " failed: " + ex);
		}
		if (resp.statusCode() != 200)
			throw new Exception("No 'apiKey' is set for LLM provider '" + getProviderId() + "', and getting a Microsoft Entra ID token from " + source + " failed: HTTP " + resp.statusCode() + ": " + resp.body());

		JsonNode root = new ObjectMapper().readTree(resp.body());
		String token = root.path("access_token").asText("");
		if (StringUtil.isNullOrBlank(token))
			throw new Exception("Microsoft Entra ID token from " + source + " had no 'access_token'.");

		// 'expires_in' (seconds from now) from Entra and IMDS, 'expires_on' (epoch seconds) from App Service - both may be strings
		long now = System.currentTimeMillis();
		if (root.hasNonNull("expires_in"))
			_entraTokenExpiresAt = now + Long.parseLong(root.get("expires_in").asText()) * 1000L;
		else if (root.hasNonNull("expires_on"))
			_entraTokenExpiresAt = Long.parseLong(root.get("expires_on").asText()) * 1000L;
		else
			_entraTokenExpiresAt = now + 3600_000L;

		_entraToken = token;
		return token;
	}
}
