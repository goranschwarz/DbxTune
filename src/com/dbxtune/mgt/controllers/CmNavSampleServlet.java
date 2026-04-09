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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.pcs.PersistWriterJdbc;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.TimeUtils;

import com.dbxtune.central.controllers.Helper;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Collector-side servlet: GET /api/mgt/cm/navSample?time=YYYY-MM-DD HH:mm:ss&amp;cm=CmName&amp;dir=prev|next
 * <p>
 * Navigates to the previous or next sample that has data for the given CM.
 * Mirrors {@code PersistReader.getPrevSample()} / {@code getNextSample()}.
 * <p>
 * Returns JSON: {@code {"found": true, "sampleTime": "2025-01-15 14:32:10.123"}}
 * or {@code {"found": false}} when there is no adjacent sample.
 */
public class CmNavSampleServlet
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		ObjectMapper om  = Helper.createObjectMapper();
		PrintWriter  out = resp.getWriter();

		// --- Parse parameters ---
		String timeParam = Helper.getParameter(req, "time");
		String cmName    = Helper.getParameter(req, "cm");
		String dir       = Helper.getParameter(req, "dir");

		if (timeParam == null || timeParam.trim().isEmpty())
		{
			om.writeValue(out, errMap("missing-param", "Missing required parameter: time"));
			out.flush(); out.close();
			return;
		}
		if (cmName == null || cmName.trim().isEmpty())
		{
			om.writeValue(out, errMap("missing-param", "Missing required parameter: cm"));
			out.flush(); out.close();
			return;
		}
		if (dir == null || dir.trim().isEmpty())
			dir = "next";
		dir = dir.trim().toLowerCase();
		if (!"prev".equals(dir) && !"next".equals(dir))
		{
			om.writeValue(out, errMap("invalid-param", "Parameter 'dir' must be 'prev' or 'next'"));
			out.flush(); out.close();
			return;
		}

		timeParam = timeParam.trim();
		cmName    = cmName.trim();

		// --- Parse timestamp — delegates to TimeUtils.parseToTimestampX() which tries
		// multiple formats (ISO-8601, with/without millis, date-only, …)
		Timestamp ts;
		try
		{
			ts = TimeUtils.parseToTimestampX(timeParam);
		}
		catch (ParseException ex)
		{
			om.writeValue(out, errMap("invalid-param", "Invalid time format: " + timeParam));
			out.flush(); out.close();
			return;
		}

		// --- Get DB connection ---
		DbxConnection conn = null;
		if (PersistentCounterHandler.hasInstance())
		{
			PersistWriterJdbc writer = PersistentCounterHandler.getInstance().getPersistWriterJdbc();
			if (writer != null)
				conn = CmDataServlet.getConnectionForTimestamp(ts, writer);
		}

		if (conn == null)
		{
			om.writeValue(out, errMap("no-data", "No storage connection available"));
			out.flush(); out.close();
			return;
		}

		// --- Query for prev/next sample ---
		boolean isPrev = "prev".equals(dir);
		String operator = isPrev ? "<" : ">";
		String order    = isPrev ? "desc" : "asc";

		// SQL with [bracket] quoting — translated by quotifySqlString() to the actual DBMS dialect
		String sql = conn.quotifySqlString(
				  "SELECT TOP 1 [SessionSampleTime] "
				+ "FROM [MonSessionSampleDetailes] "
				+ "WHERE [SessionSampleTime] " + operator + " ? "
				+ "  AND [CmName] = ? "
				+ "  AND ([absRows] > 0 OR [diffRows] > 0 OR [rateRows] > 0) "
				+ "ORDER BY [SessionSampleTime] " + order);

		SimpleDateFormat tsFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		Map<String, Object> result = new LinkedHashMap<>();

		try (PreparedStatement pstmt = conn.prepareStatement(sql))
		{
			pstmt.setTimestamp(1, ts);
			pstmt.setString(2, cmName);

			try (ResultSet rs = pstmt.executeQuery())
			{
				if (rs.next())
				{
					Timestamp sampleTime = rs.getTimestamp(1);
					result.put("found",      true);
					result.put("sampleTime", tsFmt.format(sampleTime));
				}
				else
				{
					result.put("found", false);
				}
			}
		}
		catch (Exception ex)
		{
			_logger.warn("CmNavSampleServlet: error querying MonSessionSampleDetailes: {}", ex.getMessage(), ex);
			om.writeValue(out, errMap("query-error", "Error navigating samples: " + ex.getMessage()));
			out.flush(); out.close();
			return;
		}

		om.writeValue(out, result);
		out.flush();
		out.close();
	}

	/** Builds a simple error response map for Jackson serialisation. */
	private static Map<String, Object> errMap(String code, String message)
	{
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("error",   code);
		m.put("message", message);
		return m;
	}
}
