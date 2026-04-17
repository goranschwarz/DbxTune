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
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.controllers.Helper;
import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.pcs.PersistWriterJdbc;
import com.dbxtune.sql.conn.DbxConnection;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Serves object DDL and metadata from MonDdlStorage for a given server.
 *
 * <pre>GET /api/mgt/ddl-storage</pre>
 *
 * <p>Query parameters:
 * <ul>
 *   <li>{@code objectName} – partial / exact object name (case-insensitive LIKE), default: all</li>
 *   <li>{@code dbname}     – database name filter (case-insensitive LIKE), default: all</li>
 *   <li>{@code type}       – comma-separated object type codes to include, e.g. {@code U,V,P,SS}.
 *       Supported values: U=Table, V=View, P=Procedure/Function, TR=Trigger, SS=Execution plan.
 *       Default: all types.</li>
 *   <li>{@code includeText} – {@code true/false}: include CLOB columns (objectText, extraInfoText,
 *       dependsText, optdiagText). Default: {@code true}.</li>
 *   <li>{@code limit}      – max rows returned. Default: 200.</li>
 *   <li>{@code ts}         – timestamp ({@code YYYY-MM-DD HH:mm:ss}) selecting which H2 recording
 *       to query. With date-rolling enabled each day is a separate database file; pass the date
 *       of the recording you want to inspect. Default: current time (live recording).</li>
 * </ul>
 *
 * <p>Response JSON:
 * <pre>{
 *   "count": N,
 *   "includeText": true,
 *   "items": [
 *     {
 *       "dbname": "...",
 *       "owner": "...",
 *       "objectName": "...",
 *       "type": "U",
 *       "crdate": "YYYY-MM-DD HH:mm:ss",
 *       "sampleTime": "YYYY-MM-DD HH:mm:ss",
 *       "source": "...",
 *       "dependParent": "...",
 *       "dependLevel": 0,
 *       "dependList": "...",
 *       "objectText": "CREATE TABLE ...",
 *       "extraInfoText": "...",
 *       "dependsText": "...",
 *       "optdiagText": "..."
 *     }, ...
 *   ]
 * }</pre>
 */
public class DdlStorageServlet 
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private static final int     DEFAULT_LIMIT = 200;
	private static final String  DDL_TABLE     = "MonDdlStorage";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		ObjectMapper om  = Helper.createObjectMapper();
		PrintWriter  out = resp.getWriter();

		// ---- Parse parameters ------------------------------------------------
		String objectNameParam  = Helper.getParameter(req, "objectName",  "");
		String dbnameParam      = Helper.getParameter(req, "dbname",      "");
		String typeParam        = Helper.getParameter(req, "type",        "");
		String includeTextParam = Helper.getParameter(req, "includeText", "true");
		String limitParam       = Helper.getParameter(req, "limit",       String.valueOf(DEFAULT_LIMIT));
		String tsParam          = Helper.getParameter(req, "ts",          null); // null = use current time

		boolean includeText = !"false".equalsIgnoreCase(includeTextParam.trim());
		int     limit       = DEFAULT_LIMIT;
		try { limit = Integer.parseInt(limitParam.trim()); } catch (NumberFormatException ignored) {}

		// Resolve timestamp: parse supplied value or default to now
		Timestamp ts = new Timestamp(System.currentTimeMillis());
		if (tsParam != null && !tsParam.isBlank())
		{
			try
			{
				Date d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(tsParam.trim().replace('+', ' '));
				ts = new Timestamp(d.getTime());
			}
			catch (ParseException ex)
			{
				om.writeValue(out, errMap("bad-param", "Cannot parse ts: " + tsParam));
				out.flush(); out.close();
				return;
			}
		}

		// Parse comma-separated type filter → list of uppercase tokens
		List<String> typeFilter = new ArrayList<>();
		if (!typeParam.isBlank())
		{
			for (String t : typeParam.split(","))
			{
				String trimmed = t.trim().toUpperCase();
				if (!trimmed.isEmpty())
					typeFilter.add(trimmed);
			}
		}

		// ---- Resolve storage connection for the requested timestamp ----------
		// With H2 date-rolling each day is a separate database file;
		// getConnectionForTimestamp opens the right one based on ts.
		DbxConnection conn = getConnectionForTimestamp(ts);
		if (conn == null)
		{
			resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			om.writeValue(out, errMap("no-data", "No storage connection available for timestamp: "
					+ new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(ts)));
			out.flush(); out.close();
			return;
		}

		// ---- Build query ------------------------------------------------------
		try
		{
			List<Map<String, Object>> items = queryDdlStorage(
					conn, objectNameParam.trim(), dbnameParam.trim(),
					typeFilter, includeText, limit);

			Map<String, Object> root = new LinkedHashMap<>();
			root.put("count",       items.size());
			root.put("includeText", includeText);
			root.put("items",       items);

			om.writeValue(out, root);
			out.flush(); out.close();
		}
		catch (Exception ex)
		{
			_logger.warn("DdlStorageServlet: query error: " + ex.getMessage(), ex);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			om.writeValue(out, errMap("query-error", ex.getMessage()));
			out.flush(); out.close();
		}
	}

	// -------------------------------------------------------------------------
	// Query helpers
	// -------------------------------------------------------------------------

	private List<Map<String, Object>> queryDdlStorage(
			DbxConnection conn,
			String objectName,
			String dbname,
			List<String> typeFilter,
			boolean includeText,
			int limit)
	throws Exception
	{
		String blobCols = includeText
				? ", [objectText], [extraInfoText], [dependsText], [optdiagText] "
				: "";

		StringBuilder sql = new StringBuilder();
		sql.append(" select [dbname], [owner], [objectName], [type], [crdate], [sampleTime],");
		sql.append("        [source], [dependParent], [dependLevel], [dependList] ");
		sql.append(blobCols);
		sql.append(" from ").append(DDL_TABLE).append(" ");
		sql.append(" where 1=1 ");

		List<Object> params = new ArrayList<>();

		if (!objectName.isEmpty())
		{
			sql.append(" and lower([objectName]) like lower(?) ");
			// Wrap with % if no wildcard already present
			params.add(objectName.contains("%") ? objectName : "%" + objectName + "%");
		}
		if (!dbname.isEmpty())
		{
			sql.append(" and lower([dbname]) like lower(?) ");
			params.add(dbname.contains("%") ? dbname : "%" + dbname + "%");
		}
		if (!typeFilter.isEmpty())
		{
			String placeholders = typeFilter.stream().map(t -> "?").collect(Collectors.joining(","));
			sql.append(" and upper([type]) in (").append(placeholders).append(") ");
			params.addAll(typeFilter);
		}

		sql.append(" order by [dbname], [type], [objectName], [owner] ");
		sql.append(" limit ").append(limit);

		String quotedSql = conn.quotifySqlString(sql.toString());

		_logger.debug("DdlStorageServlet.queryDdlStorage: sql={} params={}", quotedSql, params);

		List<Map<String, Object>> items = new ArrayList<>();

		PreparedStatement pstmt = conn.prepareStatement(quotedSql);
		try
		{
			for (int i = 0; i < params.size(); i++)
				pstmt.setString(i + 1, (String) params.get(i));

			ResultSet rs = pstmt.executeQuery();
			java.text.SimpleDateFormat tsFmt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			while (rs.next())
			{
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("dbname",       rs.getString("dbname"));
				row.put("owner",        rs.getString("owner"));
				row.put("objectName",   rs.getString("objectName"));
				row.put("type",         rs.getString("type"));

				Timestamp crdate     = rs.getTimestamp("crdate");
				Timestamp sampleTime = rs.getTimestamp("sampleTime");
				row.put("crdate",      crdate     != null ? tsFmt.format(crdate)     : null);
				row.put("sampleTime",  sampleTime != null ? tsFmt.format(sampleTime) : null);

				row.put("source",       rs.getString("source"));
				row.put("dependParent", rs.getString("dependParent"));
				row.put("dependLevel",  rs.getInt   ("dependLevel"));
				row.put("dependList",   rs.getString("dependList"));

				if (includeText)
				{
					row.put("objectText",    clobToString(rs, "objectText"));
					row.put("extraInfoText", clobToString(rs, "extraInfoText"));
					row.put("dependsText",   clobToString(rs, "dependsText"));
					row.put("optdiagText",   clobToString(rs, "optdiagText"));
				}

				items.add(row);
			}
			rs.close();
		}
		finally
		{
			pstmt.close();
		}

		return items;
	}

	// -------------------------------------------------------------------------
	// Utility helpers
	// -------------------------------------------------------------------------

	/**
	 * Resolve the H2 connection for the given timestamp.
	 * <p>
	 * When H2 date-rolling is enabled each day is stored in a separate database
	 * file. {@link CmDataServlet#getConnectionForTimestamp} opens the correct
	 * historical file based on {@code ts}, or returns the live connection when
	 * {@code ts} falls in the current recording period.
	 */
	private static DbxConnection getConnectionForTimestamp(Timestamp ts)
	{
		if (!PersistentCounterHandler.hasInstance())
			return null;
		PersistWriterJdbc writer = PersistentCounterHandler.getInstance().getPersistWriterJdbc();
		if (writer == null)
			return null;
		return CmDataServlet.getConnectionForTimestamp(ts, writer);
	}

	/** Read a potentially-CLOB column as a plain String (handles both CLOB and VARCHAR). */
	private static String clobToString(ResultSet rs, String col) 
	throws Exception
	{
		Object obj = rs.getObject(col);
		if (obj == null)
			return null;
		if (obj instanceof Clob)
		{
			Clob clob = (Clob) obj;
			return clob.getSubString(1, (int) clob.length());
		}
		return obj.toString();
	}

	private static Map<String, Object> errMap(String code, String message)
	{
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("error",   code);
		m.put("message", message);
		return m;
	}
}
