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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * LLM client for <b>ChatGPT</b> (OpenAI Chat Completions API) - and every server that speaks the same API:
 * company LLM gateways (LiteLLM, ...), Azure OpenAI (v1 API), vLLM, LM Studio, and many cloud services
 * (Mistral, DeepSeek, Groq, OpenRouter, ... - add those as extra providers, see {@link LlmClientRegistry}).
 *
 * <h3>Configuration properties</h3>
 * <pre>
 * DbxCentral.llm.openai.apiKey     = &lt;API key&gt;                  (required, unless 'url' points at a server that needs none)
 * DbxCentral.llm.openai.model      = gpt-4o                       (optional, default shown)
 * DbxCentral.llm.openai.url        = https://api.openai.com/v1    (optional; '/chat/completions' is added)
 * DbxCentral.llm.openai.authHeader = Authorization                (optional; 'Authorization' sends 'Bearer &lt;key&gt;', any other header the key as is - Azure OpenAI: api-key)
 * </pre>
 * The request asks for JSON mode ({@code response_format}); for a server that does not support it, remove it with
 * {@code extraBody={"response_format":null}} (see {@link LlmClientAbstract#getModels()}).
 */
public class LlmClientOpenAi
extends LlmClientAbstract
{
	public static final String TYPE          = "openai";
	public static final String DEFAULT_MODEL = "gpt-4o";
	public static final String DEFAULT_URL   = "https://api.openai.com/v1";

	public LlmClientOpenAi(String providerId)
	{
		super(providerId);
	}

	@Override public    String getType()               { return TYPE; }
	@Override protected String getDefaultDisplayName() { return "ChatGPT (OpenAI)"; }
	@Override protected String getDefaultModel()       { return DEFAULT_MODEL; }

	/** An API key, or a 'url' (a local or company server may not need a key). */
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

		ObjectMapper om = new ObjectMapper();
		ObjectNode body = om.createObjectNode();
		body.put("model", model.getModelId());

		ArrayNode messages = body.putArray("messages");
		ObjectNode userMsg = messages.addObject();
		userMsg.put("role", "user");
		userMsg.put("content", prompt);

		ObjectNode responseFormat = body.putObject("response_format");
		responseFormat.put("type", "json_object");

		applyExtraBody(body, model);

		HttpRequest.Builder builder = HttpRequest.newBuilder()
				.uri(URI.create(getUrl(DEFAULT_URL) + "/chat/completions"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(body), StandardCharsets.UTF_8));
		if (StringUtil.hasValue(getApiKey()))
		{
			String authHeader = cfg(key("authHeader"), "Authorization").trim();
			builder.header(authHeader, "Authorization".equalsIgnoreCase(authHeader) ? "Bearer " + getApiKey() : getApiKey());
		}
		applyExtraHeaders(builder, model);

		HttpResponse<String> httpResponse = _httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		String rawResponse = httpResponse.body();

		if (httpResponse.statusCode() != 200)
			throw new Exception("OpenAI API returned HTTP " + httpResponse.statusCode() + ": " + rawResponse);

		JsonNode root = om.readTree(rawResponse);
		JsonNode choicesArr = root.get("choices");
		if (choicesArr == null || !choicesArr.isArray() || choicesArr.isEmpty())
			throw new Exception("OpenAI API response had no 'choices': " + rawResponse);

		String answerText = choicesArr.get(0).path("message").path("content").asText("");

		LlmOptimizeResponse response = parseModelAnswer(answerText, rawResponse, prompt, model);
		response.setInputTokens (intOrNull(root.path("usage").path("prompt_tokens")));
		response.setOutputTokens(intOrNull(root.path("usage").path("completion_tokens")));
		return response;
	}
}
