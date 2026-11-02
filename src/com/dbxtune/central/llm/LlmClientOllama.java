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
 * DbxCentral.llm.ollama.model = llama3                   (whatever model(s) you've pulled locally)
 * </pre>
 * A model tag with ':' (like {@code qwen2.5-coder:14b}) can be listed as is; to give it settings
 * (label, extraBody, ...) use a name of your own: {@code model=coder} + {@code coder.model=qwen2.5-coder:14b},
 * since ':' can't be part of a property name.
 */
public class LlmClientOllama
extends LlmClientAbstract
{
	public static final String TYPE          = "ollama";
	public static final String DEFAULT_URL   = "http://localhost:11434";
	public static final String DEFAULT_MODEL = "llama3";

	public LlmClientOllama(String providerId)
	{
		super(providerId);
	}

	@Override public    String getType()               { return TYPE; }
	@Override protected String getDefaultDisplayName() { return "Ollama (local)"; }
	@Override protected String getDefaultModel()       { return DEFAULT_MODEL; }

	/**
	 * No API key: enabled when it is configured at all ('url' or 'model' set), or chosen as the default provider.
	 * (It used to be always enabled - which would put it in every model dropdown, also where nobody runs Ollama.)
	 */
	@Override
	public boolean isEnabled()
	{
		return StringUtil.hasValue(cfg(key("url")))
			|| StringUtil.hasValue(cfg(key("model")))
			|| StringUtil.commaStrToList(cfg(LlmClientRegistry.PROPKEY_DEFAULT_PROVIDER)).contains(getProviderId());
	}

	@Override
	public LlmOptimizeResponse optimize(LlmOptimizeRequest request, LlmModelEntry model, LlmProgress progress) throws Exception
	{
		checkModelEntry(model);
		String prompt = buildPrompt(request);

		ObjectMapper om = new ObjectMapper();

		// Streamed (the default): progress while we wait, and the model's thinking - the 'thinking' field ('think' asked for
		// only from a model seen thinking, see askOllamaThink()), or a <think>...</think> it writes first in its answer.
		// Sent again without 'think' if the model does not take it.
		boolean askThink = askOllamaThink(model);
		String  rawResponse;
		while (true)
		{
			ObjectNode body = om.createObjectNode();
			body.put("model", model.getModelId());
			body.put("prompt", prompt);
			body.put("stream", model.isStream());
			body.put("format", "json");
			if (askThink)
				body.put("think", true);
			applyExtraBody(body, model);
			request.setSettingsSent(requestSettings(body)); // for the saved request: what was sent, without the prompt

			HttpRequest.Builder builder = HttpRequest.newBuilder()
					.timeout(model.getTimeout())
					.uri(URI.create(getUrl(DEFAULT_URL) + "/api/generate"))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(body), StandardCharsets.UTF_8));
			applyExtraHeaders(builder, model);

			if ( ! model.isStream() )
			{
				// Not streamed: wait for the whole answer, at most 'timeout' in total, and stop the call when nobody waits any more
				HttpResponse<String> httpResponse = sendAndWait(builder.build(), model, progress);
				if (httpResponse.statusCode() != 200)
					throw new Exception("Ollama API returned HTTP " + httpResponse.statusCode() + ": " + httpResponse.body());
				rawResponse = httpResponse.body();
				break;
			}

			StreamedAnswer answer = sendOllamaStream(builder.build(), model, progress);
			if (askThink && isThinkingSummaryRejected(answer.statusCode, answer.body, model))
			{
				askThink = false;
				continue;
			}
			if (answer.statusCode != 200)
				throw new Exception("Ollama API returned HTTP " + answer.statusCode + ": " + answer.body);
			rawResponse = answer.body;
			break;
		}

		JsonNode root = om.readTree(rawResponse);

		// A <think>...</think> written first in the answer is the model's thinking, not part of the answer
		String[] split    = splitThinkTag(root.path("response").asText(""));
		String   thinking = root.path("thinking").asText("") + (split[0] == null ? "" : split[0]);

		LlmOptimizeResponse response = parseModelAnswer(split[1], rawResponse, prompt, model);
		response.setInputTokens (intOrNull(root.path("prompt_eval_count")));
		response.setOutputTokens(intOrNull(root.path("eval_count")));
		if (StringUtil.hasValue(thinking))
			response.setThinkingSummary(thinking.trim());
		return response;
	}
}
