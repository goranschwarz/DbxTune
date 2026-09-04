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
package com.dbxtune.central.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.llm.LlmClient;
import com.dbxtune.central.llm.LlmClientRegistry;
import com.dbxtune.central.llm.LlmOptimizeRequest;
import com.dbxtune.central.llm.LlmOptimizeResponse;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <pre>POST /api/llm/optimize-sql</pre>
 * Body: {@code {"sql": "...", "ddlContext": "...", "plan": "...", "dbVendor": "...", "provider": "claude"}}
 * <p>
 * Sends the SQL statement (plus whatever DDL/stats/plan context is supplied)
 * to the requested (or configured default) LLM provider and returns its
 * optimization suggestion.
 */
public class LlmSqlOptimizeServlet
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * <pre>POST /api/llm/optimize-sql</pre>
	 * Body: {@code {"sql": "...", "ddlContext": "...", "plan": "...", "dbVendor": "...", "provider": "claude", "preview": false}}
	 * <p>
	 * When {@code preview} is {@code true}, returns {@code {"promptSent": "..."}} only - the prompt
	 * text that would be sent to an LLM, built entirely locally with no network call and no cost -
	 * instead of actually calling a provider. Checked, and returned, before the
	 * {@link LlmClientRegistry#isFeatureEnabled()}/login/provider checks below (formatting the
	 * prompt is a pure string operation, see
	 * {@link com.dbxtune.central.llm.LlmClientAbstract#buildPrompt}, shared by every provider and
	 * independent of any API key), so this stays available even when the real feature is fully
	 * turned off, letting a user copy the prompt into any LLM chat UI by hand as a manual
	 * workaround/shortcut. The "claude" provider id is used only to obtain an {@code LlmClient}
	 * instance to call the shared {@code buildPrompt()} on - which provider is irrelevant here,
	 * none of its config/credentials are touched. A GET-based preview (query-string params) was
	 * tried first and dropped: a full plan/DDL context routinely blows past the server's max URI
	 * length (HTTP 414) - POST with a JSON body has no such limit.
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException
	{
		response.setContentType("application/json;charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();
		ObjectMapper om = Helper.createObjectMapper();

		LlmOptimizeRequest llmRequest;
		try
		{
			llmRequest = om.readValue(request.getReader(), LlmOptimizeRequest.class);
		}
		catch (Exception ex)
		{
			writeError(om, out, response, HttpServletResponse.SC_BAD_REQUEST, "bad-request", "Could not parse JSON request body: " + ex.getMessage());
			return;
		}

		if (StringUtil.isNullOrBlank(llmRequest.getSql()))
		{
			writeError(om, out, response, HttpServletResponse.SC_BAD_REQUEST, "bad-request", "Expected a non-blank 'sql' field in the request body.");
			return;
		}

		if (llmRequest.isPreview())
		{
			LlmClient previewClient = LlmClientRegistry.getInstance().getProvider("claude");
			Map<String, String> result = new LinkedHashMap<>();
			result.put("promptSent", previewClient.buildPrompt(llmRequest));
			om.writeValue(out, result);
			out.flush();
			out.close();
			return;
		}

		if ( ! LlmClientRegistry.isFeatureEnabled() )
		{
			writeError(om, out, response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "feature-disabled",
					"The LLM Optimization Advice feature is not enabled. Set 'DbxCentral.llm.enabled=true' in DBX_CENTRAL.conf to turn it on.");
			return;
		}

		LlmClient client = LlmClientRegistry.getInstance().getProvider(llmRequest.getProvider());
		if (client == null)
		{
			String requestedProvider = StringUtil.hasValue(llmRequest.getProvider()) ? "'" + llmRequest.getProvider() + "'" : "(none requested)";
			writeError(om, out, response, HttpServletResponse.SC_BAD_REQUEST, "unknown-provider",
					"No usable LLM provider found. Requested provider: " + requestedProvider + ". "
					+ "Either set 'DbxCentral.llm.provider=claude|openai|gemini|ollama' explicitly, or configure exactly one provider's apiKey/URL, in DBX_CENTRAL.conf.");
			return;
		}
		if ( ! client.isEnabled() )
		{
			writeError(om, out, response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "provider-not-configured",
					"LLM provider '" + client.getProviderId() + "' is not configured (missing API key / URL) in DBX_CENTRAL.conf.");
			return;
		}

		// NOTE: Deliberately NOT using Helper.isAuthorized(request, response) / response.sendError(...)
		// here - those produce Jetty's default HTML error page, which breaks the JSON-only contract
		// this endpoint is supposed to have (the caller is always a fetch() expecting JSON back).
		//
		// Checked after parsing the request/resolving the provider (rather than first, as it used to
		// be) so the "not logged in" response can still carry a promptSent preview below - building the
		// prompt is a pure, local, no-cost string operation (unlike actually calling the LLM provider,
		// which is what login is meant to gate), and the caller already possesses every input that goes
		// into it (sql/ddlContext/plan came from their own request body), so there's nothing new to leak.
		if (StringUtil.isNullOrBlank(request.getRemoteUser()) && ! LlmClientRegistry.isAnonymousAccessAllowed())
		{
			Map<String, String> extra = new LinkedHashMap<>();
			extra.put("providerId", client.getProviderId());
			try { extra.put("promptSent", client.buildPrompt(llmRequest)); }
			catch (Exception buildEx) { _logger.warn("LlmSqlOptimizeServlet: failed building the prompt preview for the not-logged-in response.", buildEx); }

			writeError(om, out, response, HttpServletResponse.SC_UNAUTHORIZED, "not-logged-in",
					"Not logged in! (Set 'DbxCentral.llm.allowAnonymous=true' in DBX_CENTRAL.conf to allow this without login.)", extra);
			return;
		}

		try
		{
			LlmOptimizeResponse llmResponse = client.optimize(llmRequest);
			om.writeValue(out, llmResponse);
		}
		catch (Exception ex)
		{
			_logger.warn("LlmSqlOptimizeServlet: problem calling LLM provider '" + client.getProviderId() + "'.", ex);

			// Best-effort: the prompt is a pure function of the request, so it can still be shown as
			// "what we tried to send" even though the actual call to the provider failed.
			String promptSent = null;
			try { promptSent = client.buildPrompt(llmRequest); }
			catch (Exception buildEx) { _logger.warn("LlmSqlOptimizeServlet: also failed building the prompt preview for the error response.", buildEx); }

			Map<String, String> extra = new LinkedHashMap<>();
			extra.put("providerId", client.getProviderId());
			if (promptSent != null)
				extra.put("promptSent", promptSent);

			writeError(om, out, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "llm-call-failed", ex.getMessage(), extra);
			return;
		}

		out.flush();
		out.close();
	}

	private void writeError(ObjectMapper om, PrintWriter out, HttpServletResponse response, int statusCode, String error, String message)
	throws IOException
	{
		writeError(om, out, response, statusCode, error, message, null);
	}

	private void writeError(ObjectMapper om, PrintWriter out, HttpServletResponse response, int statusCode, String error, String message, Map<String, String> extraFields)
	throws IOException
	{
		response.setStatus(statusCode);

		Map<String, String> err = new LinkedHashMap<>();
		err.put("error", error);
		err.put("message", message);
		if (extraFields != null)
			err.putAll(extraFields);
		om.writeValue(out, err);

		out.flush();
		out.close();
	}
}
