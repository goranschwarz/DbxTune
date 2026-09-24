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
import com.dbxtune.utils.StringUtil;

/**
 * <pre>GET /api/llm/config</pre>
 * Returns {@code {"enabled": true|false}} - whether the LLM Optimization Advice feature is turned
 * on ({@link LlmClientRegistry#PROPKEY_ENABLED}). Deliberately does NOT require login: client-side
 * trigger points (Active Statements dialogs, Showplan viewer) need to check this before deciding
 * whether to render any LLM-related button/link at all, and a plain "is this feature on" read
 * carries no sensitive data.
 * <p>
 * Also returns what the Showplan dialogs show next to their "LLM Optimization Advice" heading -
 * the same checks {@code LlmSqlOptimizeServlet} makes for a real (non-preview) request, in the same
 * order, but without calling anything:
 * <ul>
 *   <li>{@code configured}    - enabled AND a default provider resolves AND it has its API key/URL</li>
 *   <li>{@code provider}, {@code providerName}, {@code model} - only when {@code configured}</li>
 *   <li>{@code loginRequired} - {@code DbxCentral.llm.allowAnonymous} is off</li>
 *   <li>{@code loggedIn}      - the caller has a logged-in user</li>
 * </ul>
 * Still nothing sensitive: never an API key or URL, only the model name.
 */
public class LlmConfigServlet
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException
	{
		response.setContentType("application/json;charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();

		boolean enabled = LlmClientRegistry.isFeatureEnabled();

		// Same provider resolution as LlmSqlOptimizeServlet does for a request without an explicit provider
		LlmClient client     = enabled ? LlmClientRegistry.getInstance().getProvider(null) : null;
		boolean   configured = client != null && client.isEnabled();

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("enabled",       enabled);
		result.put("configured",    configured);
		if (configured)
		{
			result.put("provider",     client.getProviderId());
			result.put("providerName", client.getDisplayName());
			result.put("model",        client.getModel());
		}
		result.put("loginRequired", ! LlmClientRegistry.isAnonymousAccessAllowed());
		result.put("loggedIn",      StringUtil.hasValue(request.getRemoteUser()));

		Helper.createObjectMapper().writeValue(out, result);

		out.flush();
		out.close();
	}
}
