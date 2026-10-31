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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * LLM client for <b>Gemini</b> (Google AI Studio, Generative Language API).
 * For Gemini on Google Cloud (Vertex AI), see {@link LlmClientVertex}.
 *
 * <h3>Configuration properties</h3>
 * <pre>
 * DbxCentral.llm.gemini.apiKey = &lt;Google AI Studio API key&gt;
 * DbxCentral.llm.gemini.model  = gemini-2.0-flash                                      (optional, default shown)
 * DbxCentral.llm.gemini.url    = https://generativelanguage.googleapis.com/v1beta     (optional)
 * </pre>
 */
public class LlmClientGemini
extends LlmClientAbstract
{
	public static final String TYPE          = "gemini";
	public static final String DEFAULT_MODEL = "gemini-2.0-flash";
	public static final String DEFAULT_URL   = "https://generativelanguage.googleapis.com/v1beta";

	public LlmClientGemini(String providerId)
	{
		super(providerId);
	}

	@Override public    String getType()               { return TYPE; }
	@Override protected String getDefaultDisplayName() { return "Gemini (Google AI Studio)"; }
	@Override protected String getDefaultModel()       { return DEFAULT_MODEL; }

	@Override
	public LlmOptimizeResponse optimize(LlmOptimizeRequest request, LlmModelEntry model, LlmProgress progress) throws Exception
	{
		checkModelEntry(model);
		String prompt = buildPrompt(request);

		ObjectNode body = buildGenerateContentBody(prompt);
		applyExtraBody(body, model);

		HttpRequest.Builder builder = HttpRequest.newBuilder()
				.timeout(model.getTimeout())
				.uri(URI.create(getUrl(DEFAULT_URL) + "/models/" + model.getModelId() + ":generateContent"))
				.header("x-goog-api-key", getApiKey())
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(body), StandardCharsets.UTF_8));
		applyExtraHeaders(builder, model);

		HttpResponse<String> httpResponse = _httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		String rawResponse = httpResponse.body();

		if (httpResponse.statusCode() != 200)
			throw new Exception("Gemini API returned HTTP " + httpResponse.statusCode() + ": " + rawResponse);

		return parseGenerateContentResponse(rawResponse, prompt, model);
	}
}
