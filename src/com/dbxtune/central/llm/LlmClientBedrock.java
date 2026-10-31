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
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;

/**
 * LLM client for <b>Amazon Bedrock</b>.
 * <ul>
 *   <li>Claude ({@code anthropic.*} model ids): the Messages API, {@code https://bedrock-mantle.<region>.api.aws/anthropic/v1/messages}</li>
 *   <li>Every other Bedrock model (inference profiles like {@code global.anthropic.claude-opus-4-6-v1}, {@code meta.*},
 *       {@code amazon.nova-*}, ARNs, ...): the Converse API, {@code https://bedrock-runtime.<region>.amazonaws.com/model/<id>/converse}</li>
 * </ul>
 *
 * <h3>Configuration properties</h3>
 * <pre>
 * DbxCentral.llm.bedrock.region  = us-east-1                    (required)
 * DbxCentral.llm.bedrock.model   = anthropic.claude-opus-5-5    (optional, default shown; per model 'format' overrides: messages|converse)
 * DbxCentral.llm.bedrock.profile = &lt;AWS profile name&gt;          (optional)
 * DbxCentral.llm.bedrock.apiKey  = &lt;Bedrock API key&gt;           (optional, instead of IAM credentials; env AWS_BEARER_TOKEN_BEDROCK also works)
 * DbxCentral.llm.bedrock.url     = &lt;https://host&gt;               (optional, replaces the host: VPC endpoint, proxy)
 * </pre>
 * Without an API key, the request is signed (SigV4) with the credentials of the standard AWS credential chain (env
 * AWS_ACCESS_KEY_ID..., ~/.aws/credentials and config incl. SSO and assume-role, web identity (EKS), the ECS task
 * role, the EC2 instance profile), or of the named 'profile'.
 */
public class LlmClientBedrock
extends LlmClientAbstract
{
	public static final String TYPE            = "bedrock";
	public static final String DEFAULT_MODEL   = "anthropic.claude-opus-5-5";
	public static final String FORMAT_MESSAGES = "messages";
	public static final String FORMAT_CONVERSE = "converse";

	private AwsCredentialsProvider _credentialsProvider;
	private String                 _credentialsProviderFor;

	public LlmClientBedrock(String providerId)
	{
		super(providerId);
	}

	@Override public    String getType()               { return TYPE; }
	@Override protected String getDefaultDisplayName() { return "Amazon Bedrock"; }
	@Override protected String getDefaultModel()       { return DEFAULT_MODEL; }

	/** A region (the credentials are checked when called). */
	@Override
	public boolean isEnabled()
	{
		return StringUtil.hasValue(cfg(key("region")));
	}

	/** The 'apiKey' setting, or env AWS_BEARER_TOKEN_BEDROCK. */
	@Override
	protected String getApiKey()
	{
		String apiKey = super.getApiKey();
		if (StringUtil.isNullOrBlank(apiKey))
			apiKey = System.getenv("AWS_BEARER_TOKEN_BEDROCK");
		return apiKey == null ? "" : apiKey.trim();
	}

	@Override
	public LlmOptimizeResponse optimize(LlmOptimizeRequest request, LlmModelEntry model, LlmProgress progress) throws Exception
	{
		checkModelEntry(model);
		String prompt = buildPrompt(request);

		String modelId = model.getModelId();
		String format  = model.getFormat() != null ? model.getFormat() : (modelId.startsWith("anthropic.") ? FORMAT_MESSAGES : FORMAT_CONVERSE);
		String region  = cfg(key("region"));

		// The Messages format (anthropic.*) is streamed (the default): progress while we wait. The thinking summary only for a
		// model seen thinking, and sent again without it if the model does not take it (see askThinkingSummary(),
		// isThinkingSummaryRejected()). The Converse format is not streamed (for now).
		boolean stream             = FORMAT_MESSAGES.equals(format) && model.isStream();
		boolean askThinkingSummary = FORMAT_MESSAGES.equals(format) && askThinkingSummary(model);
		while (true)
		{
			ObjectNode body;
			String     url;
			String     signingName;
			Map<String, String> headers = new LinkedHashMap<>();
			headers.put("Content-Type", "application/json");
			if (FORMAT_MESSAGES.equals(format))
			{
				body        = buildMessagesBody(modelId, model.getMaxTokens(), prompt);
				url         = getUrl("https://bedrock-mantle." + region + ".api.aws") + "/anthropic/v1/messages";
				signingName = "bedrock-mantle";
				headers.put("anthropic-version", LlmClientClaude.ANTHROPIC_VERSION);
			}
			else if (FORMAT_CONVERSE.equals(format))
			{
				body = new ObjectMapper().createObjectNode();
				ObjectNode userMsg = body.putArray("messages").addObject();
				userMsg.put("role", "user");
				userMsg.putArray("content").addObject().put("text", prompt);
				body.putObject("inferenceConfig").put("maxTokens", model.getMaxTokens());

				// The model id goes in the path: an ARN or a 'model:version' id has ':' and '/' that must be encoded
				url         = getUrl("https://bedrock-runtime." + region + ".amazonaws.com") + "/model/" + URLEncoder.encode(modelId, StandardCharsets.UTF_8) + "/converse";
				signingName = "bedrock";
			}
			else
			{
				throw new Exception("Unknown 'format' '" + format + "' for model '" + model.getName() + "' of LLM provider '" + getProviderId() + "'. Use '" + FORMAT_MESSAGES + "' or '" + FORMAT_CONVERSE + "'.");
			}
			if (askThinkingSummary)
				body.putObject("thinking").put("type", "adaptive").put("display", "summarized");
			applyExtraBody(body, model);
			if (stream)
				body.put("stream", true); // before signing: the signature covers the body bytes
			headers.putAll(model.getExtraHeaders()); // before signing: a signed request must not get headers changed afterwards

			byte[] bodyBytes = new ObjectMapper().writeValueAsBytes(body);

			String apiKey = getApiKey();
			if (StringUtil.hasValue(apiKey))
			{
				if (FORMAT_MESSAGES.equals(format))
					headers.put("x-api-key", apiKey);
				else
					headers.put("Authorization", "Bearer " + apiKey);
			}
			else
			{
				headers = signRequest(url, headers, bodyBytes, signingName, region);
			}

			HttpRequest.Builder builder = HttpRequest.newBuilder()
					.timeout(model.getTimeout())
					.uri(URI.create(url))
					.POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes));
			for (Map.Entry<String, String> e : headers.entrySet())
			{
				// Set by the Java HTTP client itself (and not allowed to set): the same values as signed
				if ( ! "host".equalsIgnoreCase(e.getKey()) && ! "content-length".equalsIgnoreCase(e.getKey()) )
					builder.setHeader(e.getKey(), e.getValue());
			}

			if ( ! stream )
			{
				// Not streamed: wait for the whole answer, at most 'timeout' in total, and stop the call when nobody waits any more
				HttpResponse<String> httpResponse = sendAndWait(builder.build(), model, progress);
				String rawResponse = httpResponse.body();

				if (httpResponse.statusCode() != 200)
					throw new Exception("Amazon Bedrock returned HTTP " + httpResponse.statusCode() + " for " + url + ": " + rawResponse);

				if (FORMAT_MESSAGES.equals(format))
					return parseMessagesResponse(rawResponse, prompt, model);

				// Converse: the text blocks of the answer (a reasoning block, if any, is skipped)
				JsonNode root = new ObjectMapper().readTree(rawResponse);
				StringBuilder answerText = new StringBuilder();
				for (JsonNode block : root.path("output").path("message").path("content"))
				{
					if (block.has("text"))
						answerText.append(block.path("text").asText(""));
				}
				LlmOptimizeResponse response = parseModelAnswer(answerText.toString(), rawResponse, prompt, model);
				response.setInputTokens (intOrNull(root.path("usage").path("inputTokens")));
				response.setOutputTokens(intOrNull(root.path("usage").path("outputTokens")));
				return response;
			}

			StreamedAnswer answer = sendMessagesStream(builder.build(), model, progress);
			if (askThinkingSummary && isThinkingSummaryRejected(answer.statusCode, answer.body, model))
			{
				askThinkingSummary = false;
				continue;
			}
			if (answer.statusCode != 200)
				throw new Exception("Amazon Bedrock returned HTTP " + answer.statusCode + " for " + url + ": " + answer.body);
			return parseMessagesResponse(answer.body, prompt, model);
		}
	}

	/**
	 * Sign the request with AWS SigV4 (exactly these headers and body bytes) and return the headers to send:
	 * the given ones plus the signature ('Authorization', 'X-Amz-Date', 'X-Amz-Security-Token' for temporary credentials).
	 */
	Map<String, String> signRequest(String url, Map<String, String> headers, byte[] bodyBytes, String signingName, String region)
	throws Exception
	{
		AwsCredentials credentials = getCredentials();

		SdkHttpRequest.Builder sdkRequest = SdkHttpRequest.builder()
				.uri(URI.create(url))
				.method(SdkHttpMethod.POST);
		for (Map.Entry<String, String> e : headers.entrySet())
			sdkRequest.putHeader(e.getKey(), e.getValue());

		SignedRequest signed = AwsV4HttpSigner.create().sign(r -> r
				.identity(credentials)
				.request(sdkRequest.build())
				.payload(ContentStreamProvider.fromByteArray(bodyBytes))
				.putProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, signingName)
				.putProperty(AwsV4HttpSigner.REGION_NAME, region));

		Map<String, String> signedHeaders = new LinkedHashMap<>();
		for (Map.Entry<String, List<String>> e : signed.request().headers().entrySet())
			signedHeaders.put(e.getKey(), String.join(",", e.getValue()));
		return signedHeaders;
	}

	/** Credentials from the AWS credential chain (or the named 'profile'). The provider caches and refreshes them. */
	private synchronized AwsCredentials getCredentials()
	throws Exception
	{
		String profile = cfg(key("profile"));
		String source  = StringUtil.hasValue(profile) ? "the AWS profile '" + profile + "'" : "the AWS default credential chain";
		try
		{
			if (_credentialsProvider == null || ! profile.equals(_credentialsProviderFor))
			{
				_credentialsProvider = StringUtil.hasValue(profile)
						? ProfileCredentialsProvider.builder().profileName(profile).build()
						: DefaultCredentialsProvider.builder().build();
				_credentialsProviderFor = profile;
			}
			return _credentialsProvider.resolveCredentials();
		}
		catch (Exception ex)
		{
			throw new Exception("No 'apiKey' is set for LLM provider '" + getProviderId() + "', and getting AWS credentials from " + source + " failed: " + ex.getMessage(), ex);
		}
	}
}
