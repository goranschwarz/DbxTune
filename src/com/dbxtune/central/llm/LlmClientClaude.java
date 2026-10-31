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
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * LLM client for <b>Claude</b> (Anthropic Messages API) - or any Anthropic-compatible endpoint (a company gateway) via 'url'.
 *
 * <h3>Configuration properties</h3>
 * <pre>
 * DbxCentral.llm.claude.apiKey    = &lt;Anthropic API key&gt;                (required, unless 'url' points at a gateway that needs none)
 * DbxCentral.llm.claude.model     = claude-opus-5-5, claude-sonnet-5-5   (optional, default claude-opus-5-5; the first is the default)
 * DbxCentral.llm.claude.url       = https://api.anthropic.com            (optional)
 * DbxCentral.llm.claude.maxTokens = 16000                                (optional, default shown)
 * </pre>
 * Plus the per-model settings described in {@link LlmClientAbstract#getModels()} (label, extraBody, extraHeaders, ...).
 * <p>
 * {@code maxTokens} caps the model's output (thinking included); Anthropic hard-truncates generation once it's hit,
 * wherever that lands - including mid-string inside the JSON answer this client asks for.
 */
public class LlmClientClaude
extends LlmClientAbstract
{
	public static final String TYPE              = "claude";
	public static final String DEFAULT_MODEL     = "claude-opus-5-5";
	public static final String DEFAULT_URL       = "https://api.anthropic.com";
	public static final String ANTHROPIC_VERSION = "2023-06-01";

	public LlmClientClaude(String providerId)
	{
		super(providerId);
	}

	@Override public    String getType()               { return TYPE; }
	@Override protected String getDefaultDisplayName() { return "Claude (Anthropic)"; }
	@Override protected String getDefaultModel()       { return DEFAULT_MODEL; }

	/** An API key, or a 'url' (an Anthropic-compatible gateway may not need a key). */
	@Override
	public boolean isEnabled()
	{
		return StringUtil.hasValue(getApiKey()) || StringUtil.hasValue(cfg(key("url")));
	}

	@Override
	public LlmOptimizeResponse optimize(LlmOptimizeRequest request, LlmModelEntry model) throws Exception
	{
		checkModelEntry(model);
		String prompt = buildPrompt(request);

		ObjectNode body = buildMessagesBody(model.getModelId(), model.getMaxTokens(), prompt);
		applyExtraBody(body, model);

		HttpRequest.Builder builder = HttpRequest.newBuilder()
				.uri(URI.create(getUrl(DEFAULT_URL) + "/v1/messages"))
				.header("anthropic-version", ANTHROPIC_VERSION)
				.header("content-type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(body), StandardCharsets.UTF_8));
		if (StringUtil.hasValue(getApiKey()))
			builder.header("x-api-key", getApiKey());
		applyExtraHeaders(builder, model);

		HttpResponse<String> httpResponse = _httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		String rawResponse = httpResponse.body();

		if (httpResponse.statusCode() != 200)
			throw new Exception("Claude API returned HTTP " + httpResponse.statusCode() + ": " + rawResponse);

		return parseMessagesResponse(rawResponse, prompt, model);
	}
}
