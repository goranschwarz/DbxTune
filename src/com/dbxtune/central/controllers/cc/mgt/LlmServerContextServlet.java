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
package com.dbxtune.central.controllers.cc.mgt;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.controllers.Helper;
import com.dbxtune.central.controllers.cc.ProxyHelper;
import com.dbxtune.central.llm.LlmClientRegistry;
import com.dbxtune.central.llm.LlmServerContextBuilder;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.HtmlQueryString;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@code GET /api/llm/server-context?srv=<name>&dbVendor=<vendor>&dbname=<db>&ts=<yyyy-MM-dd HH:mm:ss>}: the "Server
 * configuration and hardware" block of the LLM prompt (see {@link LlmServerContextBuilder}), from the DBMS configuration the
 * server's Collector stored at its start ({@code /api/mgt/dbms-config}). ASE also reads CmSummary (the page size) and the host
 * monitoring CMs (cores, memory) from the Collector. SQL Server and ASE, for now.
 * <p>
 * Answers JSON {@code {serverContext, resolvedTs}}, or {@code {serverContext:"", message}} when there is none (turned
 * off, Collector not reachable, nothing stored): the advice is then asked for without it - never an error for the caller.
 */
public class LlmServerContextServlet
extends ProxyHelper
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		if ( ! LlmClientRegistry.isFeatureEnabled() )
		{
			sendJsonError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "feature-disabled", "The LLM Optimization Advice feature is not enabled.");
			return;
		}
		if (StringUtil.isNullOrBlank(req.getRemoteUser()) && ! LlmClientRegistry.isAnonymousAccessAllowed())
		{
			sendJsonError(resp, HttpServletResponse.SC_UNAUTHORIZED, "not-logged-in", "Not logged in!");
			return;
		}

		// ProxyHelper keeps the Collector's address in fields, and a servlet instance is shared by all requests:
		// one instance per request, so two requests for different servers can not mix their configurations
		new LlmServerContextServlet().handle(req, resp);
	}

	private void handle(HttpServletRequest req, HttpServletResponse resp)
	throws IOException
	{
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("serverContext", "");

		if ( ! LlmServerContextBuilder.isEnabled() )
		{
			sendResult(resp, result, "Turned off by '" + LlmServerContextBuilder.PROPKEY_ENABLED + "=false'.");
			return;
		}

		String  dbVendor    = Helper.getParameter(req, "dbVendor", "");
		boolean isAse       = DbUtils.isProductName(dbVendor, DbUtils.DB_PROD_NAME_SYBASE_ASE);
		boolean isSqlServer = DbUtils.isProductName(dbVendor, DbUtils.DB_PROD_NAME_MSSQL);
		if ( ! isAse && ! isSqlServer )
		{
			sendResult(resp, result, "Not available for the DBMS vendor '" + dbVendor + "'.");
			return;
		}

		try
		{
			getSrvInfo(req);
		}
		catch (IOException ex)
		{
			sendResult(resp, result, ex.getMessage());
			return;
		}
		if (getCollectorBaseUrl() == null)
		{
			sendResult(resp, result, "No Collector address is known for server '" + getSrvName() + "'.");
			return;
		}

		String dbname = Helper.getParameter(req, "dbname", "");
		String ts     = Helper.getParameter(req, "ts", "");
		if (StringUtil.isNullOrBlank(ts))
			ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

		HtmlQueryString qs = new HtmlQueryString(getCollectorBaseUrl() + "/api/mgt/dbms-config");
		qs.add("srv", getSrvName());
		qs.add("ts",  ts);

		HttpRequest.Builder rb = HttpRequest.newBuilder().uri(URI.create(qs.toString())).timeout(Duration.ofSeconds(30)).GET();
		if (StringUtil.hasValue(getMgtAuthentication()))
			rb.header("Authorization", getMgtAuthentication());

		try
		{
			HttpResponse<String> collectorResp = _httpClient.send(rb.build(), HttpResponse.BodyHandlers.ofString());
			JsonNode config = new ObjectMapper().readTree(collectorResp.body());
			if (collectorResp.statusCode() != 200 || config.has("error"))
			{
				sendResult(resp, result, "The Collector has no DBMS configuration: " + config.path("message").asText(collectorResp.statusCode() + ""));
				return;
			}

			result.put("serverContext", isAse
					? LlmServerContextBuilder.buildAse(config, readCm("CmSummary", ts), readCm("CmOsMpstat", ts), readCm("CmOsMeminfo", ts), dbname)
					: LlmServerContextBuilder.buildSqlServer(config, dbname));
			result.put("resolvedTs",    config.path("resolvedTs").asText(""));
			sendResult(resp, result, null);
		}
		catch (InterruptedException ex)
		{
			Thread.currentThread().interrupt();
			sendResult(resp, result, "Interrupted.");
		}
		catch (Exception ex)
		{
			_logger.info("LlmServerContextServlet: could not get the DBMS configuration of '" + getSrvName() + "' from its Collector: " + ex);
			sendResult(resp, result, "The Collector of '" + getSrvName() + "' could not be reached: " + ex.getMessage());
		}
	}

	/**
	 * The Collector's latest sample of a CM at or before 'ts': {columns, rows}.
	 * @return null when there is none (the CM is not on, no host monitoring, the Collector does not answer): that part is left out
	 */
	private JsonNode readCm(String cmName, String ts)
	throws InterruptedException
	{
		try
		{
			HtmlQueryString navQs = new HtmlQueryString(getCollectorBaseUrl() + "/api/mgt/cm/navSample");
			navQs.add("cm",   cmName);
			navQs.add("time", ts);
			navQs.add("dir",  "prev");
			JsonNode nav = getJsonFromCollector(navQs);
			if (nav == null || ! nav.path("found").asBoolean(false))
				return null;

			HtmlQueryString dataQs = new HtmlQueryString(getCollectorBaseUrl() + "/api/mgt/cm/data");
			dataQs.add("cm",   cmName);
			dataQs.add("time", nav.path("sampleTime").asText(""));
			dataQs.add("type", "abs");
			JsonNode data = getJsonFromCollector(dataQs);
			return (data == null || data.path("rows").size() == 0) ? null : data;
		}
		catch (IOException ex)
		{
			_logger.debug("LlmServerContextServlet: could not read " + cmName + " of '" + getSrvName() + "' from its Collector: " + ex);
			return null;
		}
	}

	/** @return the answer, or null when it is not a 200 */
	private JsonNode getJsonFromCollector(HtmlQueryString qs)
	throws IOException, InterruptedException
	{
		HttpRequest.Builder rb = HttpRequest.newBuilder().uri(URI.create(qs.toString())).timeout(Duration.ofSeconds(10)).GET();
		if (StringUtil.hasValue(getMgtAuthentication()))
			rb.header("Authorization", getMgtAuthentication());

		HttpResponse<String> collectorResp = _httpClient.send(rb.build(), HttpResponse.BodyHandlers.ofString());
		return collectorResp.statusCode() == 200 ? new ObjectMapper().readTree(collectorResp.body()) : null;
	}

	private void sendResult(HttpServletResponse resp, Map<String, Object> result, String message)
	throws IOException
	{
		if (message != null)
			result.put("message", message);
		resp.setContentType(APPLICATION_JSON);
		resp.setCharacterEncoding("UTF-8");
		Helper.createObjectMapper().writeValue(resp.getWriter(), result);
	}
}
