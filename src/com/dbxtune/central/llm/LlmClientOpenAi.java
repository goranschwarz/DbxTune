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
 * LLM client for <b>ChatGPT</b> (OpenAI Chat Completions API).
 *
 * <h3>Required configuration properties</h3>
 * <pre>
 * DbxCentral.llm.openai.apiKey = &lt;OpenAI API key&gt;
 * DbxCentral.llm.openai.model  = gpt-4o   (optional, defaults shown)
 * </pre>
 * Verify the model id against OpenAI's current documentation.
 */
public class LlmClientOpenAi
extends LlmClientAbstract
{
	public static final String PROPKEY_API_KEY = "DbxCentral.llm.openai.apiKey";
	public static final String PROPKEY_MODEL   = "DbxCentral.llm.openai.model";
	public static final String DEFAULT_MODEL   = "gpt-4o";

	private static final String API_URL = "https://api.openai.com/v1/chat/completions";

	@Override public String getProviderId()  { return "openai"; }
	@Override public String getDisplayName() { return "ChatGPT (OpenAI)"; }

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

		ArrayNode messages = body.putArray("messages");
		ObjectNode userMsg = messages.addObject();
		userMsg.put("role", "user");
		userMsg.put("content", prompt);

		ObjectNode responseFormat = body.putObject("response_format");
		responseFormat.put("type", "json_object");

		HttpRequest httpRequest = HttpRequest.newBuilder()
				.uri(URI.create(API_URL))
				.header("Authorization", "Bearer " + getApiKey())
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(body), StandardCharsets.UTF_8))
				.build();

		HttpResponse<String> httpResponse = _httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		String rawResponse = httpResponse.body();

		if (httpResponse.statusCode() != 200)
			throw new Exception("OpenAI API returned HTTP " + httpResponse.statusCode() + ": " + rawResponse);

		JsonNode root = om.readTree(rawResponse);
		JsonNode choicesArr = root.get("choices");
		if (choicesArr == null || !choicesArr.isArray() || choicesArr.isEmpty())
			throw new Exception("OpenAI API response had no 'choices': " + rawResponse);

		String answerText = choicesArr.get(0).path("message").path("content").asText("");

		return parseModelAnswer(answerText, rawResponse, prompt);
	}
}
