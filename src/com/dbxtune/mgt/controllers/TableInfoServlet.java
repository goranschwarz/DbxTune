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
package com.dbxtune.mgt.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.controllers.Helper;
import com.dbxtune.pcs.PersistWriterJdbc;
import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.pcs.report.content.ase.AseAbstract;
import com.dbxtune.pcs.report.content.postgres.PostgresAbstract;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Vendor-generic "table/index info from DDL Storage, by table name list" endpoint.
 *
 * <pre>GET /mgt/table-info?dbVendor=X&amp;dbname=Y&amp;tables=t1,t2[&amp;format=html|text|json][&amp;ts=YYYY-MM-DD+HH:mm:ss]</pre>
 *
 * <p>Dispatches to {@link AseAbstract}/{@link SqlServerAbstract}/{@link PostgresAbstract}'s
 * {@code getTableInfoHtml(...)}/{@code getTableInfoPlainText(...)} static convenience methods,
 * mirroring the vendor dispatch already used by
 * {@code com.dbxtune.central.llm.LlmSqlContextBuilder#buildDdlContext}.
 *
 * <p>Unlike {@link QueryStoreServlet}'s {@code action=tableInfo} (SQL Server only, tied to that
 * class's Query-Store-specific concerns), this endpoint is vendor-agnostic from the start - built
 * for the ASE Showplan dialog's "Table Information"/"LLM Optimization Advice" sections and since also
 * used by the SQL Server Showplan dialog's equivalents, but usable
 * by any future caller/vendor without duplicating this glue. {@code QueryStoreServlet}'s own
 * {@code action=tableInfo} is left in place untouched - the Showplan dialogs no longer call it, but it
 * is still reachable, so nothing else that may depend on it breaks.
 */
public class TableInfoServlet
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		ObjectMapper om  = Helper.createObjectMapper();
		PrintWriter  out = resp.getWriter();

		String dbVendor   = Helper.getParameter(req, "dbVendor", "");
		String dbname     = Helper.getParameter(req, "dbname",   "");
		String tablesParam = Helper.getParameter(req, "tables",  "");
		String format     = Helper.getParameter(req, "format",   "html").trim().toLowerCase();
		String tsParam    = Helper.getParameter(req, "ts",       null);

		if (dbVendor.isBlank())
		{
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			om.writeValue(out, errMap("bad-param", "Parameter 'dbVendor' is required."));
			out.flush(); out.close();
			return;
		}

		Timestamp ts = new Timestamp(System.currentTimeMillis());
		if (StringUtil.hasValue(tsParam))
		{
			try
			{
				Date d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(tsParam.trim().replace('+', ' '));
				ts = new Timestamp(d.getTime());
			}
			catch (ParseException ex)
			{
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				om.writeValue(out, errMap("bad-param", "Cannot parse ts: " + tsParam));
				out.flush(); out.close();
				return;
			}
		}

		DbxConnection conn = getConnectionForTimestamp(ts);
		if (conn == null)
		{
			resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			om.writeValue(out, errMap("no-data", "No storage connection available for timestamp: "
					+ new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(ts)));
			out.flush(); out.close();
			return;
		}

		try
		{
			om.writeValue(out, actionTableInfo(conn, dbVendor.trim(), dbname.trim(), tablesParam, format));
		}
		catch (Exception ex)
		{
			_logger.warn("TableInfoServlet: error for dbVendor={} dbname={} tables={}: {}", dbVendor, dbname, tablesParam, ex.getMessage(), ex);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			om.writeValue(out, errMap("query-error", ex.getMessage()));
		}
		out.flush(); out.close();
	}

	/**
	 * format=html (default) returns HTML for display; format=text returns a plain-text bundle
	 * (used as LLM prompt context); format=json returns structured numeric fields per table (used
	 * by the ASE and SQL Server Showplan graphical plans' per-operator tooltips, which need to compare
	 * the table size against a threshold client-side rather than just display it).
	 *
	 * <p>format=json is implemented for ASE and SQL Server; Postgres still falls through to the
	 * empty-map branch below - {@link PostgresAbstract} has no {@code getTableInfoFields()} yet.
	 */
	private Map<String, Object> actionTableInfo(DbxConnection conn, String dbVendor, String dbname, String tablesParam, String format)
	{
		Map<String, Object> result = new LinkedHashMap<>();
		boolean asText = "text".equals(format);
		boolean asJson = "json".equals(format);
		String  resultKey = asJson ? "tables" : (asText ? "text" : "html");

		Set<String> tableList = new LinkedHashSet<>();
		for (String t : tablesParam.split(","))
		{
			String trimmed = t.trim();
			if (!trimmed.isEmpty())
				tableList.add(trimmed);
		}

		if (tableList.isEmpty())
		{
			result.put(resultKey, asJson ? Collections.emptyMap() : (asText ? "" : "<em>No tables provided.</em>"));
			return result;
		}

		try
		{
			if (DbUtils.isProductName(dbVendor, DbUtils.DB_PROD_NAME_SYBASE_ASE))
			{
				if (asJson)
					result.put(resultKey, AseAbstract.getTableInfoFields(conn, dbname, tableList));
				else
					result.put(resultKey, asText
							? AseAbstract.getTableInfoPlainText(conn, dbname, tableList)
							: fallbackIfBlank(AseAbstract.getTableInfoHtml(conn, dbname, tableList, true, "qs-tableinfo"), tableList));
			}
			else if (DbUtils.isProductName(dbVendor, DbUtils.DB_PROD_NAME_MSSQL))
			{
				if (asJson)
					result.put(resultKey, SqlServerAbstract.getTableInfoFields(conn, dbname, tableList));
				else
					result.put(resultKey, asText
							? SqlServerAbstract.getTableInfoPlainText(conn, dbname, tableList)
							: fallbackIfBlank(SqlServerAbstract.getTableInfoHtml(conn, dbname, tableList, true, "qs-tableinfo"), tableList));
			}
			else if (DbUtils.isProductName(dbVendor, DbUtils.DB_PROD_NAME_POSTGRES))
			{
				if (asJson)
				{
					_logger.info("actionTableInfo(): format=json is only implemented for dbVendor='Sybase ASE' and 'Microsoft SQL Server', skipping for '{}'.", dbVendor);
					result.put(resultKey, Collections.emptyMap());
				}
				else
					result.put(resultKey, asText
							? PostgresAbstract.getTableInfoPlainText(conn, dbname, tableList)
							: fallbackIfBlank(PostgresAbstract.getTableInfoHtml(conn, dbname, tableList, true, "qs-tableinfo"), tableList));
			}
			else
			{
				_logger.info("actionTableInfo(): no DDL Storage lookup implemented for dbVendor='{}', skipping.", dbVendor);
				result.put(resultKey, asJson ? Collections.emptyMap() : (asText ? "" : "<em>Table info not supported for dbVendor '" + dbVendor + "'.</em>"));
			}
		}
		catch (Exception ex)
		{
			_logger.warn("actionTableInfo: error fetching table info for dbVendor={} dbname={} tables={}: {}", dbVendor, dbname, tableList, ex.getMessage(), ex);
			result.put(resultKey, asJson ? Collections.emptyMap() : (asText ? "" : "<span class='text-danger'>Error fetching table info: " + ex.getMessage() + "</span>"));
		}

		return result;
	}

	private static String fallbackIfBlank(String html, Set<String> tableList)
	{
		if (StringUtil.hasValue(html))
			return html;

		StringBuilder sb = new StringBuilder();
		for (String t : tableList)
			sb.append("&emsp;&bull; Table <code>").append(t).append("</code> was not found in the DDL Storage.<br>\n");
		return sb.toString();
	}

	private static DbxConnection getConnectionForTimestamp(Timestamp ts)
	{
		if (!PersistentCounterHandler.hasInstance())
			return null;
		PersistWriterJdbc writer = PersistentCounterHandler.getInstance().getPersistWriterJdbc();
		if (writer == null)
			return null;
		return CmDataServlet.getConnectionForTimestamp(ts, writer);
	}

	private static Map<String, Object> errMap(String code, String message)
	{
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("error",   code);
		m.put("message", message);
		return m;
	}
}
