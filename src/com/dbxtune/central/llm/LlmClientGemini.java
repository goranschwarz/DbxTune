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
 * LLM client for <b>Gemini</b> (Google Generative Language API).
 *
 * <h3>Required configuration properties</h3>
 * <pre>
 * DbxCentral.llm.gemini.apiKey = &lt;Google AI Studio API key&gt;
 * DbxCentral.llm.gemini.model  = gemini-2.0-flash   (optional, defaults shown)
 * </pre>
 * Verify the model id against Google's current documentation.
 */
public class LlmClientGemini
extends LlmClientAbstract
{
	public static final String PROPKEY_API_KEY = "DbxCentral.llm.gemini.apiKey";
	public static final String PROPKEY_MODEL   = "DbxCentral.llm.gemini.model";
	public static final String DEFAULT_MODEL   = "gemini-2.0-flash";

	private static final String API_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

	@Override public String getProviderId()  { return "gemini"; }
	@Override public String getDisplayName() { return "Gemini (Google)"; }

	@Override protected String getPropApiKey()   { return PROPKEY_API_KEY; }
	@Override protected String getPropModel()    { return PROPKEY_MODEL; }
	@Override protected String getDefaultModel() { return DEFAULT_MODEL; }

	@Override
	public LlmOptimizeResponse optimize(LlmOptimizeRequest request) throws Exception
	{
		String prompt = buildPrompt(request);

		ObjectMapper om = new ObjectMapper();
		ObjectNode body = om.createObjectNode();
		ArrayNode contents = body.putArray("contents");
		ObjectNode content = contents.addObject();
		ArrayNode parts = content.putArray("parts");
		parts.addObject().put("text", prompt);

		String url = String.format(API_URL_TEMPLATE, getModel());

		HttpRequest httpRequest = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("x-goog-api-key", getApiKey())
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(body), StandardCharsets.UTF_8))
				.build();

		HttpResponse<String> httpResponse = _httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		String rawResponse = httpResponse.body();

		if (httpResponse.statusCode() != 200)
			throw new Exception("Gemini API returned HTTP " + httpResponse.statusCode() + ": " + rawResponse);

		JsonNode root = om.readTree(rawResponse);
		JsonNode candidates = root.get("candidates");
		if (candidates == null || !candidates.isArray() || candidates.isEmpty())
			throw new Exception("Gemini API response had no 'candidates': " + rawResponse);

		String answerText = candidates.get(0).path("content").path("parts").path(0).path("text").asText("");

		return parseModelAnswer(answerText, rawResponse, prompt);
	}
}
