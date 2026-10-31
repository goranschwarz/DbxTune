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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.auth.oauth2.ExternalAccountCredentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ImpersonatedCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.auth.oauth2.UserCredentials;

/**
 * LLM client for <b>Google Cloud</b>: Claude and Gemini models billed to your GCP project.
 * <ul>
 *   <li>Claude, "Google managed" (Vertex AI / Model Garden): {@code .../publishers/anthropic/models/<model>:rawPredict}</li>
 *   <li>Claude, "Anthropic managed" (when {@code claudeWorkspaceId} is set): {@code claude.googleapis.com/.../invoke/v1/messages}</li>
 *   <li>Gemini (Vertex AI): {@code .../publishers/google/models/<model>:generateContent}</li>
 * </ul>
 *
 * <h3>Configuration properties</h3>
 * <pre>
 * DbxCentral.llm.vertex.projectId         = &lt;GCP project id&gt;            (required)
 * DbxCentral.llm.vertex.region            = global                      (optional, default shown; us / eu = multi-region, e.g. us-east5 = regional)
 * DbxCentral.llm.vertex.model             = claude-opus-5-5, gemini-2.5-pro   (claude* = Claude, other = Gemini; per model 'format' overrides: anthropic|gemini)
 * DbxCentral.llm.vertex.credentialsFile   = &lt;path to a credentials JSON file&gt;   (optional, see below)
 * DbxCentral.llm.vertex.claudeWorkspaceId = &lt;workspace id&gt;               (optional, Claude through "Anthropic managed")
 * DbxCentral.llm.vertex.url               = &lt;https://host&gt;                (optional, replaces the host: Private Service Connect, proxy)
 * </pre>
 * Credentials: Google's <i>Application Default Credentials</i> (env GOOGLE_APPLICATION_CREDENTIALS, the
 * 'gcloud auth application-default login' file, or the metadata server of a GCE VM / GKE / Cloud Run), unless
 * 'credentialsFile' is set: a service-account key, a gcloud login, a Workload Identity Federation config
 * (keyless, from AWS / Azure / any OIDC provider) or an impersonated service account. The token is refreshed
 * by google-auth-library when it expires.
 */
public class LlmClientVertex
extends LlmClientAbstract
{
	public static final String TYPE                     = "vertex";
	public static final String DEFAULT_MODEL            = "claude-opus-5-5";
	public static final String DEFAULT_REGION           = "global";
	public static final String VERTEX_ANTHROPIC_VERSION = "vertex-2023-10-16";
	public static final String CLOUD_PLATFORM_SCOPE     = "https://www.googleapis.com/auth/cloud-platform";

	public static final String FORMAT_ANTHROPIC = "anthropic";
	public static final String FORMAT_GEMINI    = "gemini";

	private GoogleCredentials _credentials;
	private String            _credentialsFor;

	public LlmClientVertex(String providerId)
	{
		super(providerId);
	}

	@Override public    String getType()               { return TYPE; }
	@Override protected String getDefaultDisplayName() { return "Google Cloud Vertex AI"; }
	@Override protected String getDefaultModel()       { return DEFAULT_MODEL; }

	/** A project id (the credentials are checked when called). */
	@Override
	public boolean isEnabled()
	{
		return StringUtil.hasValue(cfg(key("projectId")));
	}

	@Override
	public LlmOptimizeResponse optimize(LlmOptimizeRequest request, LlmModelEntry model) throws Exception
	{
		checkModelEntry(model);
		String prompt = buildPrompt(request);

		String modelId   = model.getModelId();
		String format    = model.getFormat() != null ? model.getFormat() : (modelId.startsWith("claude") ? FORMAT_ANTHROPIC : FORMAT_GEMINI);
		String projectId = cfg(key("projectId"));
		String region    = cfg(key("region"), DEFAULT_REGION).trim();
		String workspace = cfg(key("claudeWorkspaceId"));

		// The Vertex AI host depends on the endpoint type: global, multi-region (us / eu) or a region
		String vertexHost = "global".equals(region) ? "https://aiplatform.googleapis.com"
		                  : ("us".equals(region) || "eu".equals(region)) ? "https://aiplatform." + region + ".rep.googleapis.com"
		                  : "https://" + region + "-aiplatform.googleapis.com";

		ObjectNode body;
		String     url;
		boolean    anthropicManaged = false;
		if (FORMAT_ANTHROPIC.equals(format) && StringUtil.hasValue(workspace))
		{
			// "Claude Models on Google Cloud (Anthropic managed)": the plain Claude API, under the project's workspace
			anthropicManaged = true;
			body = buildMessagesBody(modelId, model.getMaxTokens(), prompt);
			url  = getUrl("https://claude.googleapis.com") + "/v1/projects/" + projectId + "/locations/global/workspaces/" + workspace + "/invoke/v1/messages";
		}
		else if (FORMAT_ANTHROPIC.equals(format))
		{
			// Vertex AI (Google managed): the model is in the URL, the API version in the body
			body = buildMessagesBody(null, model.getMaxTokens(), prompt);
			body.put("anthropic_version", VERTEX_ANTHROPIC_VERSION);
			url  = getUrl(vertexHost) + "/v1/projects/" + projectId + "/locations/" + region + "/publishers/anthropic/models/" + modelId + ":rawPredict";
		}
		else if (FORMAT_GEMINI.equals(format))
		{
			body = buildGenerateContentBody(prompt);
			url  = getUrl(vertexHost) + "/v1/projects/" + projectId + "/locations/" + region + "/publishers/google/models/" + modelId + ":generateContent";
		}
		else
		{
			throw new Exception("Unknown 'format' '" + format + "' for model '" + model.getName() + "' of LLM provider '" + getProviderId() + "'. Use '" + FORMAT_ANTHROPIC + "' or '" + FORMAT_GEMINI + "'.");
		}
		applyExtraBody(body, model);

		HttpRequest.Builder builder = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("Authorization", "Bearer " + getAccessToken())
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(body), StandardCharsets.UTF_8));
		if (anthropicManaged)
			builder.header("anthropic-version", LlmClientClaude.ANTHROPIC_VERSION);
		applyExtraHeaders(builder, model);

		HttpResponse<String> httpResponse = _httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		String rawResponse = httpResponse.body();

		if (httpResponse.statusCode() != 200)
			throw new Exception("Google Cloud returned HTTP " + httpResponse.statusCode() + " for " + url + ": " + rawResponse);

		return FORMAT_ANTHROPIC.equals(format)
				? parseMessagesResponse(rawResponse, prompt, model)
				: parseGenerateContentResponse(rawResponse, prompt, model);
	}

	/**
	 * A Google Cloud access token. The credentials are created once (again if 'credentialsFile' changes), and
	 * google-auth-library refreshes the token when it is about to expire.
	 */
	private synchronized String getAccessToken()
	throws Exception
	{
		String file   = cfg(key("credentialsFile"));
		String source = StringUtil.hasValue(file) ? "the credentials file '" + file + "'" : "Google Application Default Credentials";
		try
		{
			if (_credentials == null || ! file.equals(_credentialsFor))
			{
				GoogleCredentials credentials = StringUtil.hasValue(file) ? loadCredentialsFile(file) : GoogleCredentials.getApplicationDefault();
				_credentials    = credentials.createScoped(Collections.singletonList(CLOUD_PLATFORM_SCOPE));
				_credentialsFor = file;
			}
			_credentials.refreshIfExpired();
			return _credentials.getAccessToken().getTokenValue();
		}
		catch (Exception ex)
		{
			_credentials = null; // try again next time (a fixed file, a 'gcloud auth application-default login', ...)
			throw new Exception("Could not get a Google Cloud access token from " + source + " for LLM provider '" + getProviderId() + "': " + ex, ex);
		}
	}

	/** Load a credentials file by its 'type', with the class for that type (Google's advice: do not let the file pick an unexpected kind). */
	private static GoogleCredentials loadCredentialsFile(String fileName)
	throws Exception
	{
		File file = new File(fileName);
		String type = new ObjectMapper().readTree(file).path("type").asText("");
		try (InputStream in = new FileInputStream(file))
		{
			switch (type)
			{
			case "service_account":              return ServiceAccountCredentials .fromStream(in);
			case "authorized_user":              return UserCredentials           .fromStream(in);
			case "external_account":             return ExternalAccountCredentials.fromStream(in);
			case "impersonated_service_account": return ImpersonatedCredentials   .fromStream(in);
			default:
				throw new Exception("Unsupported credentials 'type' '" + type + "' (supported: service_account, authorized_user, external_account, impersonated_service_account).");
			}
		}
	}
}
