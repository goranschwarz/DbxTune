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

import java.lang.invoke.MethodHandles;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cm.db2.Db2ExplainUtils;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Base class for {@link LlmClient} implementations.
 * <p>
 * Uses Java 11's built-in {@link java.net.http.HttpClient} - no extra
 * dependencies required beyond what is already in the classpath (same
 * approach as {@code com.dbxtune.central.oauth.AbstractOAuthProvider}).
 * <p>
 * Handles: config-property reading via the existing {@link Configuration}
 * singleton, the shared prompt text, and tolerant parsing of the model's
 * JSON answer (models sometimes wrap it in a markdown code fence, or use
 * slightly different key casing).
 */
public abstract class LlmClientAbstract
implements LlmClient
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	protected static final HttpClient _httpClient = HttpClient.newHttpClient();

	/** Output token cap for the formats that need one (Claude Messages API, Bedrock Converse). Thinking tokens count against it. */
	public static final int DEFAULT_MAX_TOKENS = 16000;

	/** Seconds to wait for an LLM answer (setting 'timeout'): high/max effort and big plans can take minutes */
	public static final int DEFAULT_TIMEOUT_SECONDS = 600;

	/** Config-property prefix: every setting of a provider is {@code DbxCentral.llm.<providerId>.<setting>} */
	public static final String PROPKEY_PREFIX = "DbxCentral.llm.";

	/** Bad extraBody/extraHeaders JSON is logged once per property (getModels() runs on every /api/llm/config call). */
	private static final Set<String> _warnedConfigProblems = ConcurrentHashMap.newKeySet();

	private final String _providerId;

	/**
	 * @param providerId  the instance id: the type name for the built-in providers ("claude", "openai", ...),
	 *                    or the name given in {@code DbxCentral.llm.extraProviders} (see {@link LlmClientRegistry})
	 */
	protected LlmClientAbstract(String providerId)
	{
		_providerId = providerId;
	}

	// -----------------------------------------------------------------------
	// Abstract template methods - implemented by each provider subclass
	// -----------------------------------------------------------------------

	/** The provider type, e.g. "claude" - also the id of the built-in instance of this type. */
	public abstract String getType();

	/** Display name of the built-in instance, e.g. "Claude (Anthropic)". */
	protected abstract String getDefaultDisplayName();

	/** Model to use if the 'model' setting isn't set in the configuration. */
	protected abstract String getDefaultModel();

	// -----------------------------------------------------------------------
	// Config helpers
	// -----------------------------------------------------------------------

	/** Full property name of a setting of THIS provider instance: {@code DbxCentral.llm.<providerId>.<name>} */
	protected String key(String name)
	{
		return PROPKEY_PREFIX + _providerId + "." + name;
	}

	protected String cfg(String key)
	{
		return Configuration.getCombinedConfiguration().getProperty(key, "").trim();
	}

	protected String cfg(String key, String defaultValue)
	{
		return Configuration.getCombinedConfiguration().getProperty(key, defaultValue);
	}

	protected int cfgInt(String key, int defaultValue)
	{
		return Configuration.getCombinedConfiguration().getIntProperty(key, defaultValue);
	}

	protected boolean cfgBool(String key, boolean defaultValue)
	{
		return Configuration.getCombinedConfiguration().getBooleanProperty(key, defaultValue);
	}

	protected String getApiKey()
	{
		return cfg(key("apiKey"));
	}

	/** The 'url' setting (an endpoint override: gateway, proxy, private endpoint), or the default - without a trailing '/'. */
	protected String getUrl(String defaultUrl)
	{
		String url = cfg(key("url"));
		if (StringUtil.isNullOrBlank(url))
			url = defaultUrl;
		while (url.endsWith("/"))
			url = url.substring(0, url.length() - 1);
		return url;
	}

	@Override
	public String getProviderId()
	{
		return _providerId;
	}

	@Override
	public String getDisplayName()
	{
		// An extra provider (see LlmClientRegistry) is shown by its own name unless 'displayName' is set
		String defaultName = _providerId.equals(getType()) ? getDefaultDisplayName() : _providerId;
		return cfg(key("displayName"), defaultName);
	}

	/** Default: enabled when the API key is set. Providers without an API key, or with other ways to log in, override this. */
	@Override
	public boolean isEnabled()
	{
		return StringUtil.hasValue(getApiKey());
	}

	// -----------------------------------------------------------------------
	// Models: the dropdown entries and their settings
	// -----------------------------------------------------------------------

	@Override
	public String getModel()
	{
		return getModels().get(0).getModelId();
	}

	/**
	 * The entries of the comma separated 'model' setting (the default model if it is not set). A name in the list is
	 * the model id, unless {@code <name>.model} is set - then the name is one of our own, and that setting holds the
	 * model id. This way the same model can be offered twice with different settings (for example two 'effort' levels).
	 * <p>
	 * Settings for a name: {@code DbxCentral.llm.<providerId>.<name>.<setting>}; the same settings without the name
	 * apply to all models of the provider:
	 * <ul>
	 *   <li>{@code label}        - text in the dropdown (default: the name)</li>
	 *   <li>{@code maxTokens}    - output token cap for the Claude format and Bedrock Converse (default {@link #DEFAULT_MAX_TOKENS})</li>
	 *   <li>{@code timeout}      - seconds to wait for the answer (default {@link #DEFAULT_TIMEOUT_SECONDS})</li>
	 *   <li>{@code stream}       - ask for a streamed answer, for progress while waiting (default true; Claude format only, for now)</li>
	 *   <li>{@code extraBody}    - JSON deep-merged into the request body (provider level first, then the model's)</li>
	 *   <li>{@code extraHeaders} - JSON object with extra or replacing HTTP headers</li>
	 *   <li>{@code format}       - request format, where it can't be derived from the model id (Vertex, Bedrock)</li>
	 * </ul>
	 * Everything is read from the configuration on each call, so a changed setting needs no restart.
	 */
	@Override
	public List<LlmModelEntry> getModels()
	{
		List<String> names = StringUtil.parseCommaStrToList(cfg(key("model"), getDefaultModel()), true);
		if (names.isEmpty())
			names = Collections.singletonList(getDefaultModel());

		List<LlmModelEntry> entries = new ArrayList<>();
		for (String name : names)
			entries.add(createModelEntry(name));
		return entries;
	}

	@Override
	public LlmModelEntry getModelEntry(String name)
	{
		List<LlmModelEntry> entries = getModels();
		if (StringUtil.isNullOrBlank(name))
			return entries.get(0);

		for (LlmModelEntry entry : entries)
		{
			if (entry.getName().equals(name))
				return entry;
		}
		return null;
	}

	private LlmModelEntry createModelEntry(String name)
	{
		String modelId   = cfg(key(name + ".model"), name);
		String label     = cfg(key(name + ".label"), name);
		int    maxTokens = cfgInt(key(name + ".maxTokens"), cfgInt(key("maxTokens"), DEFAULT_MAX_TOKENS));
		int    timeout   = cfgInt(key(name + ".timeout"),   cfgInt(key("timeout"),   DEFAULT_TIMEOUT_SECONDS));
		boolean stream   = cfgBool(key(name + ".stream"),  cfgBool(key("stream"),  true));
		String format    = cfg(key(name + ".format"), cfg(key("format"), "")).trim();

		List<ObjectNode>    extraBodies  = new ArrayList<>();
		Map<String, String> extraHeaders = new LinkedHashMap<>();
		String configProblem = null;
		try
		{
			for (String propName : new String[] { key("extraBody"), key(name + ".extraBody") })
			{
				ObjectNode node = readJsonObjectSetting(propName);
				if (node != null)
					extraBodies.add(node);
			}
			for (String propName : new String[] { key("extraHeaders"), key(name + ".extraHeaders") })
			{
				ObjectNode node = readJsonObjectSetting(propName);
				if (node != null)
					node.fields().forEachRemaining(e -> extraHeaders.put(e.getKey(), e.getValue().asText()));
			}
		}
		catch (Exception ex)
		{
			configProblem = ex.getMessage();
			if (_warnedConfigProblems.add(configProblem))
				_logger.warn("LLM provider '{}', model '{}': {}", _providerId, name, configProblem);
		}

		return new LlmModelEntry(name, modelId, label, maxTokens, timeout, stream, StringUtil.hasValue(format) ? format : null, extraBodies, extraHeaders, configProblem);
	}

	/** A setting holding a JSON object, or null when not set. Throws with the property name in the message on bad JSON. */
	private ObjectNode readJsonObjectSetting(String propName)
	throws Exception
	{
		String json = cfg(propName);
		if (StringUtil.isNullOrBlank(json))
			return null;

		JsonNode node;
		try
		{
			node = new ObjectMapper().readTree(json);
		}
		catch (JsonProcessingException ex)
		{
			throw new Exception("'" + propName + "' is not valid JSON: " + ex.getOriginalMessage());
		}
		if ( ! (node instanceof ObjectNode) )
			throw new Exception("'" + propName + "' must be a JSON object, like {\"name\": \"value\"}");
		return (ObjectNode) node;
	}

	/**
	 * Throws when the entry has a configuration problem - called first thing in every optimize(), so the user sees why.
	 */
	protected void checkModelEntry(LlmModelEntry model)
	throws Exception
	{
		if (model.getConfigProblem() != null)
			throw new Exception("Configuration problem for model '" + model.getName() + "' of LLM provider '" + _providerId + "': " + model.getConfigProblem());
	}

	/** Deep-merge the entry's extraBody settings into the request body: objects merge, other values replace, JSON null removes. */
	protected void applyExtraBody(ObjectNode body, LlmModelEntry model)
	{
		for (ObjectNode extra : model.getExtraBodies())
			deepMerge(body, extra);
	}

	private static void deepMerge(ObjectNode target, ObjectNode source)
	{
		source.fields().forEachRemaining(e ->
		{
			String   field = e.getKey();
			JsonNode value = e.getValue();
			JsonNode old   = target.get(field);

			if (value.isNull())
				target.remove(field);
			else if (value.isObject() && old != null && old.isObject())
				deepMerge((ObjectNode) old, (ObjectNode) value);
			else
				target.set(field, value.deepCopy());
		});
	}

	/** Set the entry's extraHeaders on the request (replacing headers with the same name). */
	protected void applyExtraHeaders(HttpRequest.Builder builder, LlmModelEntry model)
	{
		for (Map.Entry<String, String> e : model.getExtraHeaders().entrySet())
			builder.setHeader(e.getKey(), e.getValue());
	}

	// -----------------------------------------------------------------------
	// The Claude Messages API format - used by every provider that serves Claude
	// (Anthropic, Google Cloud, Amazon Bedrock, Microsoft Foundry, Anthropic-compatible gateways)
	// -----------------------------------------------------------------------

	/** {@code {"model": ..., "max_tokens": ..., "messages": [{"role": "user", "content": prompt}]}} - modelId null leaves out 'model' (Vertex has it in the URL). */
	protected static ObjectNode buildMessagesBody(String modelId, int maxTokens, String prompt)
	{
		ObjectNode body = new ObjectMapper().createObjectNode();
		if (modelId != null)
			body.put("model", modelId);
		body.put("max_tokens", maxTokens);

		ObjectNode userMsg = body.putArray("messages").addObject();
		userMsg.put("role", "user");
		userMsg.put("content", prompt);
		return body;
	}

	/**
	 * Parse a Messages API answer. The text is in the "text" content blocks - NOT necessarily the first block:
	 * current models think by default, and then the answer starts with a "thinking" block.
	 * A refusal (stop_reason "refusal") is thrown, with the model's explanation.
	 */
	protected LlmOptimizeResponse parseMessagesResponse(String rawResponse, String promptSent, LlmModelEntry model)
	throws Exception
	{
		JsonNode root = new ObjectMapper().readTree(rawResponse);

		if ("refusal".equals(root.path("stop_reason").asText()))
		{
			JsonNode details = root.path("stop_details");
			String why = details.path("explanation").asText("");
			String category = details.path("category").asText("");
			throw new Exception("The model declined to answer" + (StringUtil.hasValue(category) ? " (category '" + category + "')" : "") + (StringUtil.hasValue(why) ? ": " + why : "."));
		}

		JsonNode contentArr = root.get("content");
		if (contentArr == null || !contentArr.isArray())
			throw new Exception("The response had no 'content': " + rawResponse);

		StringBuilder answerText = new StringBuilder();
		for (JsonNode block : contentArr)
		{
			if ("text".equals(block.path("type").asText()))
				answerText.append(block.path("text").asText(""));
		}

		// No text at all: with high/max effort the model can spend all of 'maxTokens' on thinking, before it writes the answer
		String stopReason = root.path("stop_reason").asText("");
		if (StringUtil.isNullOrBlank(answerText.toString()))
		{
			if ("max_tokens".equals(stopReason))
				throw new Exception(maxTokensAdvice(model, false));
			throw new Exception("The model returned no answer text (stop_reason '" + stopReason + "').");
		}

		// Cut off in the middle of the answer (its JSON is not complete): the same advice, based on what was sent
		if ("max_tokens".equals(stopReason))
		{
			try { new ObjectMapper().readTree(stripMarkdownCodeFence(answerText.toString())); }
			catch (JsonProcessingException ex) { throw new Exception(maxTokensAdvice(model, true)); }
		}

		LlmOptimizeResponse response = parseModelAnswer(answerText.toString(), rawResponse, promptSent, model);
		response.setInputTokens (intOrNull(root.path("usage").path("input_tokens")));
		response.setOutputTokens(intOrNull(root.path("usage").path("output_tokens")));

		// The thinking text: only there when its summary was asked for (see askThinkingSummary())
		StringBuilder thinkingText = new StringBuilder();
		for (JsonNode block : contentArr)
		{
			if ("thinking".equals(block.path("type").asText()) && StringUtil.hasValue(block.path("thinking").asText("")))
				thinkingText.append(block.path("thinking").asText()).append("\n\n");
		}
		if (thinkingText.length() > 0)
			response.setThinkingSummary(thinkingText.toString().trim());
		return response;
	}

	/**
	 * What to do when the model used all of 'maxTokens' on thinking, before it wrote an answer - based on what was sent:
	 * below 64000, raise it; at 64000 or more, more tokens alone may not help (the model can think until they are all
	 * used), so: a lower 'effort' when it is 'max'/'xhigh', else ask again or send less.
	 */
	private String maxTokensAdvice(LlmModelEntry model, boolean cutInTheAnswer)
	{
		int    maxTokens = model.getMaxTokens();
		String effort    = null; // as sent: from 'extraBody' (provider level first, then the model's - the last one wins)
		for (ObjectNode extraBody : model.getExtraBodies())
		{
			if (extraBody.path("output_config").has("effort"))
				effort = extraBody.path("output_config").path("effort").asText();
		}
		boolean highestEffort = "max".equals(effort) || "xhigh".equals(effort);

		StringBuilder sb = new StringBuilder(cutInTheAnswer
				? "The answer was cut off: the model used all of its output tokens (maxTokens=" + maxTokens + ", thinking included) before it finished. "
				: "The model used all of its output tokens (maxTokens=" + maxTokens + ", thinking included) before it wrote an answer. ");
		if (maxTokens < 64000)
			sb.append("Raise it in DBX_CENTRAL.conf, for example: " + key(model.getName() + ".maxTokens") + "=64000");
		else
			sb.append("It is already " + maxTokens + ", so more tokens alone may not help: the model can keep thinking until they are all used.");

		if (highestEffort)
		{
			sb.append(maxTokens < 64000 ? ". " : " ");
			sb.append("The effort is '" + effort + "': a lower effort makes the model think less, for example "
					+ key(model.getName() + ".extraBody") + "={\"output_config\":{\"effort\":\"high\"}}");
		}
		else if (maxTokens >= 64000)
		{
			sb.append(" Ask again (the thinking differs from one run to the next), or send less: a shorter plan, fewer tables.");
		}
		return sb.toString();
	}

	// -----------------------------------------------------------------------
	// The Claude format, streamed: progress while we wait (thinking, writing the answer)
	// -----------------------------------------------------------------------

	/** provider/modelId of the models seen thinking in a streamed answer: their thinking summary is asked for from then on */
	private static final Set<String> _modelsSeenThinking = ConcurrentHashMap.newKeySet();

	/** provider/modelId of the models (or platforms) that rejected the request for a thinking summary */
	private static final Set<String> _modelsWithoutThinkingSummary = ConcurrentHashMap.newKeySet();

	private String thinkingKey(LlmModelEntry model)
	{
		return _providerId + "/" + model.getModelId();
	}

	/**
	 * Ask for the model's thinking as a summary: {@code {"thinking":{"type":"adaptive","display":"summarized"}}}?
	 * Only for a model that was seen thinking anyway: for it this is the same request plus the text, while for a model that
	 * does NOT think by default, 'adaptive' would turn thinking on (slower and costlier). And only when the answer is
	 * streamed (nobody would see it otherwise), not after it was rejected, and not when 'extraBody' has its own 'thinking'.
	 */
	protected boolean askThinkingSummary(LlmModelEntry model)
	{
		if ( ! model.isStream() || ! _modelsSeenThinking.contains(thinkingKey(model)) || _modelsWithoutThinkingSummary.contains(thinkingKey(model)) )
			return false;

		for (ObjectNode extraBody : model.getExtraBodies())
		{
			if (extraBody.has("thinking"))
				return false;
		}
		return true;
	}

	/**
	 * A HTTP 400 about 'thinking': the model (or the platform) does not take the request for a thinking summary. It is
	 * remembered (until the next restart), and the caller sends the request again without it - a 400 is rejected before
	 * the model does any work, so it costs nothing.
	 */
	protected boolean isThinkingSummaryRejected(int statusCode, String errorBody, LlmModelEntry model)
	{
		if (statusCode != 400 || errorBody == null || ! errorBody.contains("thinking"))
			return false;

		if (_modelsWithoutThinkingSummary.add(thinkingKey(model)))
			_logger.info("LLM provider '{}', model '{}' does not take a request for its thinking summary (HTTP 400), asking without it from now on. The answer was: {}", _providerId, model.getModelId(), errorBody);
		return true;
	}

	/** What {@link #sendMessagesStream} returns: the HTTP status, and the Messages response rebuilt from the stream (200) or the error body. */
	protected static class MessagesStream
	{
		public final int    statusCode;
		public final String body;

		MessagesStream(int statusCode, String body)
		{
			this.statusCode = statusCode;
			this.body       = body;
		}
	}

	/**
	 * Send a streamed Messages API request ({@code "stream": true} in the body) and read the answer's events, telling
	 * 'progress' what the model is doing: "thinking" (with the latest line of its thinking summary, when there is one,
	 * at most once a second) and "writing the answer". A model seen thinking is remembered (see {@link #askThinkingSummary}).
	 * <p>
	 * Returns the plain (not streamed) Messages response, rebuilt from the events - for {@link #parseMessagesResponse} -
	 * or, when the status is not 200, the error body as it came. A 200 answer that is not 'text/event-stream' (a gateway
	 * or proxy that ignored "stream": true) is returned as it came too: it is then the plain response. The whole call is limited by the model's 'timeout':
	 * then the download is cancelled (which also stops the model) and {@link HttpTimeoutException} is thrown.
	 */
	protected MessagesStream sendMessagesStream(HttpRequest request, LlmModelEntry model, LlmProgress progress)
	throws Exception
	{
		long deadline = System.currentTimeMillis() + model.getTimeoutSeconds() * 1000L;

		// The lines of the answer go to a queue that is read below with the time that is left: a blocked read can not be
		// stopped at the deadline, a poll() can. END: the answer is complete, a Throwable: it failed.
		Object                               END          = new Object();
		BlockingQueue<Object>                lines        = new LinkedBlockingQueue<>();
		AtomicReference<Flow.Subscription>   subscription = new AtomicReference<>();
		AtomicInteger                        statusCode   = new AtomicInteger();
		AtomicReference<String>              contentType  = new AtomicReference<>("");
		Flow.Subscriber<String> subscriber = new Flow.Subscriber<String>()
		{
			@Override public void onSubscribe(Flow.Subscription s) { subscription.set(s); s.request(Long.MAX_VALUE); }
			@Override public void onNext(String line)              { lines.add(line); }
			@Override public void onError(Throwable t)             { lines.add(t); }
			@Override public void onComplete()                     { lines.add(END); }
		};
		CompletableFuture<HttpResponse<Void>> call = _httpClient.sendAsync(request, responseInfo ->
				{
					statusCode.set(responseInfo.statusCode());
					contentType.set(responseInfo.headers().firstValue("Content-Type").orElse(""));
					return HttpResponse.BodySubscribers.fromLineSubscriber(subscriber, s -> null, StandardCharsets.UTF_8, null);
				});
		call.whenComplete((response, ex) -> { if (ex != null) lines.add(ex); });

		ObjectMapper  om        = new ObjectMapper();
		ObjectNode    message   = om.createObjectNode(); // the plain Messages response, rebuilt from the events
		ArrayNode     content   = message.putArray("content");
		List<StringBuilder> blockTexts = new ArrayList<>(); // the text (or thinking) of each content block, by index
		StringBuilder plainBody = new StringBuilder();     // not a stream: an error, or a gateway/proxy that ignored "stream": true
		String        phase     = null;
		long          lastThinkingUpdate = 0;
		String        lastSentenceShown  = null;
		boolean       complete  = false;
		try
		{
			while (true)
			{
				long   timeLeft = deadline - System.currentTimeMillis();
				Object item     = timeLeft > 0 ? lines.poll(timeLeft, TimeUnit.MILLISECONDS) : null;
				if (item == null)
					throw new HttpTimeoutException("No answer within " + model.getTimeoutSeconds() + " seconds.");
				if (item == END)
					break;
				if (item instanceof Throwable)
				{
					Throwable t = (item instanceof CompletionException && ((Throwable) item).getCause() != null) ? ((Throwable) item).getCause() : (Throwable) item;
					if (t instanceof Exception)
						throw (Exception) t;
					throw new Exception(t);
				}

				String line = (String) item;
				if (statusCode.get() != 200 || ! contentType.get().startsWith("text/event-stream"))
				{
					plainBody.append(line).append('\n');
					continue;
				}
				// Each event is 'event: <type>' + 'data: <JSON with the same type>' + a blank line: the data line is enough
				if ( ! line.startsWith("data:") )
					continue;

				JsonNode event = om.readTree(line.substring("data:".length()).trim());
				switch (event.path("type").asText())
				{
				case "message_start":
					event.path("message").fields().forEachRemaining(e -> { if ( ! "content".equals(e.getKey()) ) message.set(e.getKey(), e.getValue()); });
					break;

				case "content_block_start":
				{
					ObjectNode block = event.path("content_block").isObject() ? event.path("content_block").deepCopy() : om.createObjectNode();
					content.add(block);
					blockTexts.add(new StringBuilder());
					if ("thinking".equals(block.path("type").asText()))
					{
						_modelsSeenThinking.add(thinkingKey(model));
						if ( ! "thinking".equals(phase) )
							progress.update(phase = "thinking", null);
					}
					break;
				}

				case "content_block_delta":
				{
					int           index = event.path("index").asInt();
					JsonNode      delta = event.path("delta");
					StringBuilder text  = index < blockTexts.size() ? blockTexts.get(index) : null;
					if (text == null)
						break;
					String deltaType = delta.path("type").asText();
					if ("text_delta".equals(deltaType))
					{
						text.append(delta.path("text").asText(""));
						if ( ! "writing the answer".equals(phase) )
							progress.update(phase = "writing the answer", null);
					}
					else if ("thinking_delta".equals(deltaType))
					{
						// Only when there is a sentence to show: without a summary (not asked for) the deltas carry no text
						text.append(delta.path("thinking").asText(""));
						String sentence = lastSentence(text);
						if (sentence != null && ! sentence.equals(lastSentenceShown) && System.currentTimeMillis() - lastThinkingUpdate >= 1000)
						{
							lastThinkingUpdate = System.currentTimeMillis();
							lastSentenceShown  = sentence;
							progress.update(phase = "thinking", sentence);
						}
					}
					else if ("signature_delta".equals(deltaType))
					{
						((ObjectNode) content.get(index)).put("signature", delta.path("signature").asText(""));
					}
					break;
				}

				case "message_delta":
					// stop_reason, stop_details, ... and the final output token count
					event.path("delta").fields().forEachRemaining(e -> message.set(e.getKey(), e.getValue()));
					if (event.has("usage"))
					{
						ObjectNode usage = message.has("usage") && message.get("usage").isObject() ? (ObjectNode) message.get("usage") : message.putObject("usage");
						event.path("usage").fields().forEachRemaining(e -> usage.set(e.getKey(), e.getValue()));
					}
					break;

				case "error":
					// For example 'overloaded_error' in the middle of the answer
					throw new Exception("The LLM stopped with an error: " + event.path("error").path("type").asText("") + " - " + event.path("error").path("message").asText(""));

				default:
					// ping, content_block_stop, message_stop
				}
			}
			complete = true;
		}
		finally
		{
			// Not read to the end (timeout, error): stop the download and close the connection, which also stops the
			// model's work (and its cost). Cancelling the subscription alone may leave the connection open; cancelling
			// the call aborts the exchange (Java 16+).
			if ( ! complete )
			{
				Flow.Subscription s = subscription.get();
				if (s != null)
					s.cancel();
				call.cancel(true);
			}
		}

		// An error, or a plain (not streamed) answer: as it came
		if (statusCode.get() != 200 || ! contentType.get().startsWith("text/event-stream"))
			return new MessagesStream(statusCode.get(), plainBody.toString().trim());

		for (int i = 0; i < content.size(); i++)
		{
			ObjectNode block = (ObjectNode) content.get(i);
			String     type  = block.path("type").asText();
			if ("text".equals(type))
				block.put("text", blockTexts.get(i).toString());
			else if ("thinking".equals(type))
				block.put("thinking", blockTexts.get(i).toString());
		}
		return new MessagesStream(200, om.writeValueAsString(message));
	}

	/**
	 * The last COMPLETE sentence (or heading line) of the thinking text so far, for the progress line: the text streams in
	 * pieces that end mid-word, so the unfinished end is skipped. Without Markdown, at most 150 characters (cut at the end).
	 * Null when no sentence is complete yet.
	 */
	private static String lastSentence(StringBuilder text)
	{
		// Complete = ended by . ! ? followed by white space, or by a line break; whatever comes after the last end is unfinished
		String s   = text.toString();
		int    end = -1;
		for (int i = 0; i < s.length(); i++)
		{
			char ch = s.charAt(i);
			if (ch == '\n' || ((ch == '.' || ch == '!' || ch == '?') && i + 1 < s.length() && Character.isWhitespace(s.charAt(i + 1))))
				end = i;
		}
		if (end < 0)
			return null;

		// The sentences up to that end, last one first: the first that is not empty after removing Markdown
		String[] sentences = s.substring(0, end + 1).split("(?<=[.!?])\\s+|\\n");
		for (int i = sentences.length - 1; i >= 0; i--)
		{
			String sentence = sentences[i].replace("**", "").replaceAll("^[#>*\\-\\s]+", "").trim();
			if ( ! sentence.isEmpty() )
				return sentence.length() <= 150 ? sentence : sentence.substring(0, 147) + "...";
		}
		return null;
	}

	// -----------------------------------------------------------------------
	// The Gemini generateContent format - Google AI Studio and Google Cloud Vertex AI
	// -----------------------------------------------------------------------

	/**
	 * {@code {"contents": [{"role": "user", "parts": [{"text": prompt}]}]}} - the 'role' is optional on Google AI Studio,
	 * but Vertex AI requires it ("Please use a valid role: user, model").
	 */
	protected static ObjectNode buildGenerateContentBody(String prompt)
	{
		ObjectNode body    = new ObjectMapper().createObjectNode();
		ObjectNode content = body.putArray("contents").addObject();
		content.put("role", "user");
		content.putArray("parts").addObject().put("text", prompt);
		return body;
	}

	/** Parse a generateContent answer: the text parts of the first candidate, skipping 'thought' parts (thinking summaries). */
	protected LlmOptimizeResponse parseGenerateContentResponse(String rawResponse, String promptSent, LlmModelEntry model)
	throws Exception
	{
		JsonNode root = new ObjectMapper().readTree(rawResponse);
		JsonNode candidates = root.get("candidates");
		if (candidates == null || !candidates.isArray() || candidates.isEmpty())
			throw new Exception("The response had no 'candidates': " + rawResponse);

		StringBuilder answerText = new StringBuilder();
		for (JsonNode part : candidates.get(0).path("content").path("parts"))
		{
			if ( ! part.path("thought").asBoolean(false) )
				answerText.append(part.path("text").asText(""));
		}

		// No text at all: cut off (MAX_TOKENS: thinking counts too), blocked (SAFETY, ...), ...
		if (StringUtil.isNullOrBlank(answerText.toString()))
		{
			String finishReason = candidates.get(0).path("finishReason").asText("");
			if ("MAX_TOKENS".equals(finishReason))
			{
				// What was sent: 'generationConfig.maxOutputTokens' from 'extraBody' (the last one wins), else the model's default
				int sent = 0;
				for (ObjectNode extraBody : model.getExtraBodies())
				{
					if (extraBody.path("generationConfig").has("maxOutputTokens"))
						sent = extraBody.path("generationConfig").path("maxOutputTokens").asInt();
				}
				if (sent >= 65536)
					throw new Exception("The model used all of its output tokens (maxOutputTokens=" + sent + ", thinking included) before it wrote an answer. "
							+ "It is already " + sent + ", so more tokens alone may not help: the model can keep thinking until they are all used. "
							+ "Lower the thinking with 'generationConfig.thinkingConfig' in 'extraBody', ask again, or send less: a shorter plan, fewer tables.");
				throw new Exception("The model used all of its output tokens (" + (sent > 0 ? "maxOutputTokens=" + sent : "the model's default limit") + ", thinking included) before it wrote an answer. "
						+ "Raise 'generationConfig.maxOutputTokens' with 'extraBody' in DBX_CENTRAL.conf, for example: "
						+ key(model.getName() + ".extraBody") + "={\"generationConfig\":{\"maxOutputTokens\":65536}}");
			}
			throw new Exception("The model returned no answer text (finishReason '" + finishReason + "').");
		}

		LlmOptimizeResponse response = parseModelAnswer(answerText.toString(), rawResponse, promptSent, model);
		response.setInputTokens (intOrNull(root.path("usageMetadata").path("promptTokenCount")));
		response.setOutputTokens(intOrNull(root.path("usageMetadata").path("candidatesTokenCount")));
		return response;
	}

	// -----------------------------------------------------------------------
	// Prompt building
	// -----------------------------------------------------------------------

	/**
	 * Build the prompt text sent to the LLM: the SQL statement, DDL/index/stats
	 * context (if any), the execution plan (if any), and an instruction to
	 * respond with a single JSON object.
	 */
	@Override
	public String buildPrompt(LlmOptimizeRequest request)
	{
		StringBuilder sb = new StringBuilder();
		
		// Captured before the ASE display-name rewrite below, so buildDbmsSpecificDirections() (and
		// any other DbUtils.isProductName(...) check added later) keeps matching against the real
		// product name rather than the human-friendly label built from it.
		String rawDbVendor = request.getDbVendor();
		String dbVendor = StringUtil.hasValue(rawDbVendor) ? rawDbVendor : "the target database";

		if (dbVendor.startsWith("Adaptive Server Enterprise"))
			dbVendor = "SAP Sybase ASE (Adaptive Server Enterprise)";

		if (dbVendor.startsWith("Microsoft SQL Server"))
			dbVendor = "Microsoft SQL Server and Azure SQL DB";

		// The product name of Db2 LUW is 'DB2/' + the platform (DB2/LINUXX8664)
		if (DbUtils.isProductName(dbVendor, DbUtils.DB_PROD_NAME_DB2_LUW))
			dbVendor = "IBM Db2 for Linux, UNIX and Windows (Db2 LUW)";

		// Build initial Prompt
//		sb.append("You are an expert database performance tuner for ").append(dbVendor).append(".\n\n");
		sb.append("You are a very senior database developer working with ").append(dbVendor).append(".\n");

		// The version string is collapsed to ONE line: SQL Server's @@version spans several lines (build
		// date, copyright, edition/OS), which would otherwise read like separate instructions in the prompt.
		if (StringUtil.hasValue(request.getDbmsVersion()))
		{
			sb.append("The exact server version is: ").append(request.getDbmsVersion().replaceAll("\\s+", " ").trim()).append("\n");
			sb.append("Only suggest syntax, features and index options that are available in this version.\n");
		}

		sb.append("You focus on real-world, actionable advice that will make a big difference, quickly.\n");
		sb.append("You value everyone's time, and while you are friendly and courteous, you do not waste time with pleasantries or emoji because you work in a fast-paced corporate environment.\n");
		sb.append("You have a query that isn't performing to end user expectations.\n");
		sb.append("You have been tasked with making serious improvements to it, quickly.\n");
		sb.append("You are not allowed to change server-level settings or make frivolous suggestions like updating statistics.\n");
		sb.append("Instead, you need to focus on query changes or index changes.\n");
		sb.append("If execution statistics are supplied, weigh your advice by how often and at what time of day the statement actually runs: a statement executed thousands of times, or one that concentrates its whole load into a short window, deserves different advice from one that runs occasionally and evenly.\n");
		sb.append("\n");
		sb.append("Do not offer followup options: the customer can only contact you once, so include all necessary information, tasks, and scripts in your initial reply.\n");
		sb.append("\n");

		String dbmsDirections = buildDbmsSpecificDirections(rawDbVendor);
		if (StringUtil.hasValue(dbmsDirections))
		{
			sb.append("Important ").append(dbVendor).append(" specifics to keep in mind - do not suggest anything incompatible with these:\n").append(dbmsDirections).append("\n");
			sb.append("\n");
		}

		// The strict "reply with ONLY a JSON object" contract only exists so the automated caller
		// (LlmSqlOptimizeServlet's POST path) can parse the reply into {optimized_sql, explanation}
		// - a human pasting a preview prompt into an LLM chat UI by hand wants the
		// opposite: a normal, readable answer, not a raw JSON blob to squint at. request.isPreview()
		// (only ever true for the "no exec" GET /api/llm/optimize-sql path) swaps one instruction
		// block for the other; everything else in the prompt (context, DBMS specifics, SQL/DDL/plan)
		// is identical either way.
		if (request.isPreview())
		{
			sb.append("Suggest how to optimize this SQL statement (rewritten SQL, and/or missing indexes, and/or other changes) and explain your reasoning.\n");
			sb.append("\n");
		}
		else
		{
			// No "origin_sql" (the SQL written back): it cost as many output tokens as the SQL is long - the slowest and most
			// expensive part - and a long statement could use up 'maxTokens' before the model got to the advice
			sb.append("Respond with ONLY a single JSON object of the form: {\"optimized_sql\": \"<rewritten SQL, empty if not optimized>\", \"explanation\": \"<your reasoning and any index/other recommendations>\"}\n");
			sb.append("Do not repeat the original SQL in your answer.\n");
			sb.append("Leave 'optimized_sql' empty if the statement is already fine as-is - do not echo the original SQL back into it.\n");
			sb.append("Format the 'explanation' field's text using simple Markdown for readability: bullet lists ('- item') or numbered lists for multiple recommendations, **bold** for emphasis, and `backticks` around identifiers/SQL fragments.\n");
			sb.append("Keep it to plain Markdown text (no headings, tables or nested lists).\n");
			sb.append("The overall reply must be ONLY the JSON object itself - no markdown code fences and no text outside the JSON.\n");
			sb.append("\n");
		}

		if (StringUtil.hasValue(request.getWorkloadProfile()))
		{
			sb.append("Execution statistics and workload profile for this statement over the reported period:\n");
			sb.append(request.getWorkloadProfile()).append("\n");
			sb.append("\n");
		}

		sb.append("SQL statement to optimize:\n").append(request.getSql()).append("\n");
		sb.append("\n");

		if (StringUtil.hasValue(request.getDdlContext()))
		{
			sb.append("Table/index DDL and statistics referenced by the statement:\n").append(request.getDdlContext()).append("\n");
			sb.append("\n");
		}

		if (StringUtil.hasValue(request.getPlan()))
		{
			String plan = request.getPlan();
			if (DbUtils.isProductName(rawDbVendor, DbUtils.DB_PROD_NAME_MSSQL))
				plan = SqlServerPlanXmlShrinker.shrink(plan);

			// Db2: the JSON Db2Tune records (the explain tables) is long and hard to read: the tree as db2exfmt shows it says the same
			if (DbUtils.isProductName(rawDbVendor, DbUtils.DB_PROD_NAME_DB2_LUW) && plan.trim().startsWith("{"))
				plan = Db2ExplainUtils.planJsonToText(plan);

			sb.append("Execution plan for the statement:\n").append(plan).append("\n");
			sb.append("\n");
		}

		return sb.toString();
	}

	/**
	 * Vendor-specific dialect/feature notes to steer the model away from suggesting syntax the
	 * target DBMS doesn't actually support - an LLM's default "T-SQL" knowledge is usually shaped by
	 * SQL Server specifically, which doesn't automatically carry over to Sybase/SAP ASE despite the
	 * shared T-SQL lineage.
	 *
	 * @return bullet-point lines (each ending in \n), or {@code null} if there's nothing specific to
	 *         add for this vendor.
	 */
	private String buildDbmsSpecificDirections(String dbVendor)
	{
		if (DbUtils.isProductName(dbVendor, DbUtils.DB_PROD_NAME_SYBASE_ASE))
		{
			return "- This DBMS does not support CTEs (Common Table Expressions / \"WITH ... AS (...)\" syntax) - do not suggest rewrites that rely on them.\n"
			     + "- Indexes in this DBMS cannot have INCLUDE columns - do not suggest CREATE INDEX ... INCLUDE (...).\n";
		}
		if (DbUtils.isProductName(dbVendor, DbUtils.DB_PROD_NAME_ORACLE))
		{
			// The first one is about money: an LLM happily suggests the SQL Tuning Advisor, a SQL Profile or partitioning,
			// and each of those needs a license that the customer may not have (using it is what makes them liable).
			return "- Do not suggest anything that needs an extra-cost Oracle license, unless the execution plan shows that it is already in use: the Diagnostics Pack and the Tuning Pack (AWR, ASH, ADDM, SQL Tuning Advisor, SQL Profiles, SQL Monitor, DBMS_SQLTUNE), and the options Partitioning, Advanced Compression and Database In-Memory.\n"
			     + "- The execution plan is the text of DBMS_XPLAN. 'Rows' / 'E-Rows' are the optimizer's estimates and 'A-Rows' the actual rows (only present when the plan has run-time statistics). 'E-Rows' is for ONE execution of the operation: multiply it with 'Starts' before comparing it with 'A-Rows'. 'Cost', 'A-Time' and 'Buffers' include the operation's children.\n"
			     + "- If the execution plan starts with 'SQL Monitoring Report' it is a SQL Monitor report instead: ONE execution, possibly still running (Status EXECUTING: the numbers are those so far, '->' marks the operations at work, and fewer rows than estimated then only means 'not yet'). 'Rows (Estim)' is for one execution of the operation, 'Rows (Actual)' the sum over 'Execs'. 'Activity (%)' is the share of the statement's time spent in the operation itself, NOT including its children. It lists both alternatives of an adaptive join: the lines without 'Execs' were not executed.\n"
			     + "- Indexes cannot have INCLUDE columns - add the extra columns to the index key instead.\n";
		}
		if (DbUtils.isProductName(dbVendor, DbUtils.DB_PROD_NAME_MYSQL))
		{
			// What an LLM gets wrong for MySQL: it mixes in MariaDB, SQL Server and old (5.x) MySQL, and it reads a normalized statement as the real one
			return "- This is MySQL (Oracle's MySQL, the InnoDB storage engine), not MariaDB: do not suggest MariaDB-only syntax or features. Use the syntax of the server version.\n"
			     + "- Indexes cannot have INCLUDE columns and there are no filtered/partial indexes: add the extra columns to the index key (a covering index) instead. A functional index (an index on an expression, 8.0.13 and above) and an invisible index (to test dropping one) are available.\n"
			     + "- Every InnoDB secondary index has the primary key columns at its end: do not add them to an index to make it covering.\n"
			     + "- A SQL text with '?' instead of the values (and backticks around the names) is a normalized statement (a digest of performance_schema): the '?' are values the application sent, not parameters to keep.\n"
			     + "- The execution plan is the JSON of EXPLAIN FORMAT=JSON. Format 1 ('query_block'): a table is named by its alias in the statement; 'rows_examined_per_scan' is for ONE scan of the table (once per row of the tables before it in 'nested_loop'), 'filtered' is the percent of those rows that pass the conditions, 'prefix_cost' includes the tables before it, and 'using_join_buffer' (hash join, Block Nested Loop) means there is no index for the join on that table. Format 2 ('query_plan', 8.3 and above): the tree of iterators, 'estimated_total_cost' includes the inputs; with EXPLAIN ANALYZE, 'actual_rows' is the average per loop ('actual_loops').\n"
			     + "- Optimizer hints are written as /*+ ... */ right after SELECT/UPDATE/DELETE (JOIN_ORDER, INDEX, NO_INDEX, ...). Index hints (USE INDEX / FORCE INDEX) still work, but suggest them only when an index or a rewrite cannot fix the plan.\n"
			     + "- Histograms (ANALYZE TABLE ... UPDATE HISTOGRAM ON col) give the optimizer the distribution of a column WITHOUT an index: suggest it when the row estimate of a condition on such a column is wrong.\n";
		}
		if (DbUtils.isProductName(dbVendor, DbUtils.DB_PROD_NAME_DB2_LUW))
		{
			// What an LLM gets wrong for Db2: it mixes in Db2 for z/OS, Oracle and SQL Server syntax, and expects hints
			return "- This is Db2 for Linux, UNIX and Windows (Db2 LUW), not Db2 for z/OS or IBM i: use Db2 LUW syntax for the server version. Use FETCH FIRST n ROWS ONLY (not LIMIT or TOP).\n"
			     + "- Only a UNIQUE index can have INCLUDE columns (CREATE UNIQUE INDEX ... INCLUDE (...)): for a non-unique index add the extra columns to the key instead.\n"
			     + "- The execution plan is either the output of db2exfmt, or Db2Tune's text of the plan Db2 really uses (EXPLAIN_FROM_SECTION of the statement in the package cache): one line per operator (RETURN, TBSCAN, IXSCAN, FETCH, NLJOIN, HSJOIN, MSJOIN, SORT, GRPBY, TEMP...) with its number in parentheses, 'rows' = the optimizer's ESTIMATE of the rows that come OUT of the operator, 'cost' in timerons including the operator's inputs, the lines in [ ] = its predicates (START / STOP = the range of an index scan, SARG = applied while the data is read, RESID = applied after, JOIN = the join predicate), and the tables and indexes it reads with their statistics. In db2exfmt the rows of an operator on the inner side of a nested loop join are per row of the outer side; marks on a join: ^ = early out, x = antijoin, > / < = outer join; 'Rows Actual' = section actuals (what really happened).\n"
			     + "- A table with 'NO statistics' has never had RUNSTATS, so the optimizer guesses its size and the plan can be far off: that is not a frivolous point, say it in one line (RUNSTATS ON TABLE ... WITH DISTRIBUTION AND SAMPLED DETAILED INDEXES ALL), then go on with the query and index changes.\n"
			     + "- Db2 has no optimizer hints in the SQL. The tools are statistics, indexes, statistical views, the optimization class (CURRENT QUERY OPTIMIZATION), REOPT for parameter markers, and as a last resort an optimization guideline (an optimization profile): do not suggest Oracle or SQL Server hint syntax.\n"
			     + "- '?' in the SQL text are parameter markers. With the statement concentrator (STMT_CONC LITERALS) the literals of the application were replaced by markers too.\n";
		}
		return null;
	}

	// -----------------------------------------------------------------------
	// Response parsing
	// -----------------------------------------------------------------------

	/**
	 * Parse the model's answer text (expected to be the JSON object described
	 * in {@link #buildPrompt}) into a {@link LlmOptimizeResponse}. Tolerates a
	 * surrounding markdown code fence, which some models add despite being
	 * asked not to.
	 */
	protected LlmOptimizeResponse parseModelAnswer(String answerText, String rawResponse, String promptSent, LlmModelEntry model) throws Exception
	{
		if (StringUtil.isNullOrBlank(answerText))
			throw new Exception("The model returned an empty answer.");

		String json = stripMarkdownCodeFence(answerText);

		String originSql;
		String optimizedSql;
		String explanation;

		ObjectMapper om = new ObjectMapper();
		try
		{
			JsonNode root = om.readTree(json);
			originSql    = firstText(root, "origin_sql", "originSql", "original_sql");
			optimizedSql = firstText(root, "optimized_sql", "optimizedSql");
			explanation  = firstText(root, "explanation");

			// The prompt asks the model to leave 'optimized_sql' empty (not echo the original back)
			// when it has no rewrite to suggest - normalize blank to null so callers only ever need
			// a single "is there a suggestion" check.
			if (StringUtil.isNullOrBlank(optimizedSql))
				optimizedSql = null;

			if (optimizedSql == null && explanation == null)
			{
				_logger.warn("{}: model answer did not contain 'optimized_sql'/'explanation', returning raw text as explanation. answer={}", getProviderId(), answerText);
				explanation = answerText;
			}
		}
		catch (JsonProcessingException ex)
		{
			// Most likely the model's response was cut off before finishing (hit the configured output
			// token limit mid-JSON) - degrade to showing the raw (partial) text instead of failing the
			// whole request with an opaque JSON-parse error.
			_logger.warn("{}: model answer was not valid JSON (likely truncated - consider raising the output token limit for this provider). answer={}", getProviderId(), answerText, ex);
			originSql    = null;
			optimizedSql = null;
			explanation  = "(The model's response appears to have been cut off before finishing - try again, or increase this provider's configured output token limit.)\n\n" + answerText;
		}

		LlmOptimizeResponse response = new LlmOptimizeResponse(optimizedSql, explanation, rawResponse, getProviderId());
		response.setOriginSql(originSql);
		response.setPromptSent(promptSent);
		response.setModel(model.getModelId());
		return response;
	}

	/** A numeric JSON value as Integer, or null when missing/not a number (for example token counts a provider did not report). */
	protected static Integer intOrNull(JsonNode node)
	{
		return (node != null && node.isNumber()) ? node.asInt() : null;
	}

	private static String firstText(JsonNode root, String... fieldNames)
	{
		for (String name : fieldNames)
		{
			JsonNode node = root.get(name);
			if (node != null && !node.isNull())
				return node.asText();
		}
		return null;
	}

	private static String stripMarkdownCodeFence(String text)
	{
		if (text == null)
			return null;

		String trimmed = text.trim();
		if (trimmed.startsWith("```"))
		{
			int firstNewline = trimmed.indexOf('\n');
			if (firstNewline >= 0)
				trimmed = trimmed.substring(firstNewline + 1);

			int lastFence = trimmed.lastIndexOf("```");
			if (lastFence >= 0)
				trimmed = trimmed.substring(0, lastFence);
		}
		return trimmed.trim();
	}
}
