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

		// Streamed (the default): progress while we wait, and the model's thought summaries; sent again without asking for
		// them if the model does not take it (see askGeminiThoughts(), isThinkingSummaryRejected())
		boolean askThoughts = askGeminiThoughts(model);
		while (true)
		{
			ObjectNode body = buildGenerateContentBody(prompt);
			if (askThoughts)
				body.putObject("generationConfig").putObject("thinkingConfig").put("includeThoughts", true);
			applyExtraBody(body, model);

			HttpRequest.Builder builder = HttpRequest.newBuilder()
					.timeout(model.getTimeout())
					.uri(URI.create(getUrl(DEFAULT_URL) + "/models/" + model.getModelId() + (model.isStream() ? ":streamGenerateContent?alt=sse" : ":generateContent")))
					.header("x-goog-api-key", getApiKey())
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(body), StandardCharsets.UTF_8));
			applyExtraHeaders(builder, model);

			if ( ! model.isStream() )
			{
				// Not streamed: wait for the whole answer, at most 'timeout' in total, and stop the call when nobody waits any more
				HttpResponse<String> httpResponse = sendAndWait(builder.build(), model, progress);
				if (httpResponse.statusCode() != 200)
					throw new Exception("Gemini API returned HTTP " + httpResponse.statusCode() + ": " + httpResponse.body());
				return parseGenerateContentResponse(httpResponse.body(), prompt, model);
			}

			StreamedAnswer answer = sendGenerateContentStream(builder.build(), model, progress);
			if (askThoughts && isThinkingSummaryRejected(answer.statusCode, answer.body, model))
			{
				askThoughts = false;
				continue;
			}
			if (answer.statusCode != 200)
				throw new Exception("Gemini API returned HTTP " + answer.statusCode + ": " + answer.body);
			return parseGenerateContentResponse(answer.body, prompt, model);
		}
	}
}
