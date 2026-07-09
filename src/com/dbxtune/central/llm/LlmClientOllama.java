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
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * LLM client for a local <b>Ollama</b> instance.
 * <p>
 * No API key needed - useful for users who don't want SQL/DDL text leaving
 * their network.
 *
 * <h3>Configuration properties</h3>
 * <pre>
 * DbxCentral.llm.ollama.url   = http://localhost:11434   (optional, default shown)
 * DbxCentral.llm.ollama.model = llama3   (required - whatever model you've pulled locally)
 * </pre>
 */
public class LlmClientOllama
extends LlmClientAbstract
{
	public static final String PROPKEY_URL   = "DbxCentral.llm.ollama.url";
	public static final String DEFAULT_URL   = "http://localhost:11434";

	public static final String PROPKEY_MODEL = "DbxCentral.llm.ollama.model";
	public static final String DEFAULT_MODEL = "llama3";

	@Override public String getProviderId()  { return "ollama"; }
	@Override public String getDisplayName() { return "Ollama (local)"; }

	@Override protected String getPropApiKey()   { return null; } // no API key needed
	@Override protected String getPropModel()    { return PROPKEY_MODEL; }
	@Override protected String getDefaultModel() { return DEFAULT_MODEL; }

	protected String getBaseUrl()
	{
		return cfg(PROPKEY_URL, DEFAULT_URL);
	}

	@Override
	public LlmOptimizeResponse optimize(LlmOptimizeRequest request) throws Exception
	{
		String prompt = buildPrompt(request);

		ObjectMapper om = new ObjectMapper();
		ObjectNode body = om.createObjectNode();
		body.put("model", getModel());
		body.put("prompt", prompt);
		body.put("stream", false);
		body.put("format", "json");

		HttpRequest httpRequest = HttpRequest.newBuilder()
				.uri(URI.create(getBaseUrl() + "/api/generate"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(body), StandardCharsets.UTF_8))
				.build();

		HttpResponse<String> httpResponse = _httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		String rawResponse = httpResponse.body();

		if (httpResponse.statusCode() != 200)
			throw new Exception("Ollama API returned HTTP " + httpResponse.statusCode() + ": " + rawResponse);

		JsonNode root = om.readTree(rawResponse);
		String answerText = root.path("response").asText("");

		return parseModelAnswer(answerText, rawResponse, prompt);
	}
}
