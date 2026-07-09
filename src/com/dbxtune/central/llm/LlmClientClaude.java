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

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * LLM client for <b>Claude</b> (Anthropic Messages API).
 *
 * <h3>Configuration properties</h3>
 * <pre>
 * DbxCentral.llm.claude.apiKey    = &lt;Anthropic API key&gt;                (required)
 * DbxCentral.llm.claude.model     = claude-sonnet-4-5                    (optional, default shown)
 * DbxCentral.llm.claude.maxTokens = 8192                                 (optional, default shown)
 * </pre>
 * Verify the model id against Anthropic's current documentation - model
 * names are versioned and get retired over time.
 * <p>
 * {@code maxTokens} caps the model's output; Anthropic hard-truncates generation once it's hit,
 * wherever that lands - including mid-string inside the JSON answer this client asks for. Raise it
 * if {@link LlmClientAbstract#parseModelAnswer} logs truncated/malformed JSON for real-world queries
 * with substantial DDL/index context.
 */
public class LlmClientClaude
extends LlmClientAbstract
{
	public static final String PROPKEY_API_KEY    = "DbxCentral.llm.claude.apiKey";
	public static final String PROPKEY_MODEL      = "DbxCentral.llm.claude.model";
	public static final String DEFAULT_MODEL      = "claude-sonnet-4-5";
	public static final String PROPKEY_MAX_TOKENS = "DbxCentral.llm.claude.maxTokens";
	public static final int    DEFAULT_MAX_TOKENS = 8192;

	private static final String API_URL           = "https://api.anthropic.com/v1/messages";
	private static final String ANTHROPIC_VERSION = "2023-06-01";

	@Override public String getProviderId()  { return "claude"; }
	@Override public String getDisplayName() { return "Claude (Anthropic)"; }

	@Override protected String getPropApiKey()   { return PROPKEY_API_KEY; }
	@Override protected String getPropModel()    { return PROPKEY_MODEL; }
	@Override protected String getDefaultModel() { return DEFAULT_MODEL; }

	@Override
	public LlmOptimizeResponse optimize(LlmOptimizeRequest request) throws Exception
	{
		String prompt = buildPrompt(request);

		ObjectMapper om = new ObjectMapper();
		ObjectNode body = om.createObjectNode();
		body.put("model", getModel());
		body.put("max_tokens", cfgInt(PROPKEY_MAX_TOKENS, DEFAULT_MAX_TOKENS));

		ArrayNode messages = body.putArray("messages");
		ObjectNode userMsg = messages.addObject();
		userMsg.put("role", "user");
		userMsg.put("content", prompt);

		HttpRequest httpRequest = HttpRequest.newBuilder()
				.uri(URI.create(API_URL))
				.header("x-api-key", getApiKey())
				.header("anthropic-version", ANTHROPIC_VERSION)
				.header("content-type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(body), StandardCharsets.UTF_8))
				.build();

		HttpResponse<String> httpResponse = _httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		String rawResponse = httpResponse.body();

		if (httpResponse.statusCode() != 200)
			throw new Exception("Claude API returned HTTP " + httpResponse.statusCode() + ": " + rawResponse);

		JsonNode root = om.readTree(rawResponse);
		JsonNode contentArr = root.get("content");
		if (contentArr == null || !contentArr.isArray() || contentArr.isEmpty())
			throw new Exception("Claude API response had no 'content': " + rawResponse);

		String answerText = contentArr.get(0).path("text").asText("");

		return parseModelAnswer(answerText, rawResponse, prompt);
	}
}
