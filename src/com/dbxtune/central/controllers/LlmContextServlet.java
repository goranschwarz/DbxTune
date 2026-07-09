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

import com.dbxtune.central.llm.LlmClientRegistry;
import com.dbxtune.central.llm.LlmSqlContextBuilder;
import com.dbxtune.sql.conn.ConnectionProp;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <pre>GET|POST /api/llm/context</pre>
 * Params: {@code jdbcUrl, jdbcUser (default "sa"), jdbcPass, sql, dbVendor, dbname}
 * <p>
 * Builds the plain-text DDL/index/stats bundle (from the DDL Storage
 * subsystem, i.e. {@code MonDdlStorage}) for the tables referenced by
 * {@code sql}, for callers that don't already have this pre-built (the
 * Active Statements web dialog, the Showplan viewer). Callers that already
 * hold a PCS connection and a fetched table-info set (the Daily Summary
 * Report, at generation time) should call the vendor's
 * {@code getTableInfoAsPlainText(...)} directly instead of this servlet.
 * <p>
 * {@code jdbcUrl} is the same H2 recording URL used by {@code /api/dsr} -
 * this servlet does not itself resolve a server name to a recording file.
 */
public class LlmContextServlet
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException
	{
		handle(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException
	{
		handle(request, response);
	}

	private void handle(HttpServletRequest request, HttpServletResponse response)
	throws IOException
	{
		response.setContentType("application/json;charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();
		ObjectMapper om = Helper.createObjectMapper();

		if ( ! LlmClientRegistry.isFeatureEnabled() )
		{
			writeError(om, out, response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "feature-disabled",
					"The LLM Optimization Advice feature is not enabled. Set 'DbxCentral.llm.enabled=true' in DBX_CENTRAL.conf to turn it on.");
			return;
		}

		// NOTE: Deliberately NOT using Helper.isAuthorized(request, response) / response.sendError(...)
		// here - those produce Jetty's default HTML error page, which breaks the JSON-only contract
		// this endpoint is supposed to have (the caller is always a fetch() expecting JSON back).
		if (StringUtil.isNullOrBlank(request.getRemoteUser()) && ! LlmClientRegistry.isAnonymousAccessAllowed())
		{
			writeError(om, out, response, HttpServletResponse.SC_UNAUTHORIZED, "not-logged-in",
					"Not logged in! (Set 'DbxCentral.llm.allowAnonymous=true' in DBX_CENTRAL.conf to allow this without login.)");
			return;
		}

		String jdbcUrl  = Helper.getParameter(request, "jdbcUrl", "");
		String jdbcUser = Helper.getParameter(request, "jdbcUser", "sa");
		String jdbcPass = Helper.getParameter(request, "jdbcPass", "");
		String sql      = Helper.getParameter(request, "sql", "");
		String dbVendor = Helper.getParameter(request, "dbVendor", "");
		String dbname   = Helper.getParameter(request, "dbname", "");

		if (StringUtil.isNullOrBlank(jdbcUrl) || StringUtil.isNullOrBlank(sql) || StringUtil.isNullOrBlank(dbVendor))
		{
			writeError(om, out, response, HttpServletResponse.SC_BAD_REQUEST, "bad-request", "Expected non-blank 'jdbcUrl', 'sql' and 'dbVendor' parameters.");
			return;
		}

		ConnectionProp cp = new ConnectionProp();
		cp.setUrl(jdbcUrl);
		cp.setUsername(jdbcUser);
		cp.setPassword(jdbcPass);

		DbxConnection conn = null;
		try
		{
			conn = DbxConnection.connect(null, cp);

			String ddlContext = LlmSqlContextBuilder.buildDdlContext(conn, dbname, sql, dbVendor);

			Map<String, String> result = new LinkedHashMap<>();
			result.put("ddlContext", ddlContext);
			om.writeValue(out, result);
		}
		catch (Exception ex)
		{
			_logger.warn("LlmContextServlet: problem building DDL context for jdbcUrl='" + jdbcUrl + "', dbVendor='" + dbVendor + "'.", ex);
			writeError(om, out, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "context-lookup-failed", ex.getMessage());
			return;
		}
		finally
		{
			if (conn != null)
				conn.closeNoThrow();
		}

		out.flush();
		out.close();
	}

	private void writeError(ObjectMapper om, PrintWriter out, HttpServletResponse response, int statusCode, String error, String message)
	throws IOException
	{
		response.setStatus(statusCode);

		Map<String, String> err = new LinkedHashMap<>();
		err.put("error", error);
		err.put("message", message);
		om.writeValue(out, err);

		out.flush();
		out.close();
	}
}
