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
import java.lang.invoke.MethodHandles;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.ICounterController;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.pcs.PersistWriterJdbc;
import com.dbxtune.sql.conn.DbxConnection;

/**
 * Collector-side servlet: GET /api/mgt/cm/list?time=YYYY-MM-DD HH:mm:ss
 * <p>
 * Returns a JSON list of CM groups and their CMs, with a {@code hasData} flag
 * indicating whether data exists within ±60 seconds of the requested time.
 * <p>
 * The hasData flag — and per-type row counts — are resolved with a single query
 * to {@code MonSessionSampleDetailes}, mirroring exactly how the native GUI's
 * {@code PersistReader} navigates samples.  That table is always present on the
 * collector; without it neither the native GUI nor the web UI can function.
 *
 * <h3>SQL portability / identifier quoting</h3>
 * All SQL strings are written with {@code [bracket]} quoting (SQL Server convention
 * used as the internal standard throughout DbxTune).  Before each SQL string is
 * passed to {@code prepareStatement()}, it is run through
 * {@link DbxConnection#quotifySqlString(String)} which translates the brackets to
 * the correct quote character for the actual DBMS in use
 * (e.g. {@code "} for PostgreSQL, back-tick for MySQL, {@code []} kept for H2/SQL Server).
 */
public class CmListServlet
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		ServletOutputStream out = resp.getOutputStream();

		String timeParam = req.getParameter("time");
		if (timeParam == null || timeParam.trim().isEmpty())
		{
			out.println("{\"error\":\"missing-param\",\"message\":\"Missing required parameter: time\"}");
			out.flush();
			out.close();
			return;
		}
		timeParam = timeParam.trim();

		// Parse timestamp
		Timestamp ts;
		try
		{
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			sdf.setLenient(false);
			Date d = sdf.parse(timeParam);
			ts = new Timestamp(d.getTime());
		}
		catch (ParseException ex)
		{
			out.println("{\"error\":\"invalid-param\",\"message\":\"Invalid time format, expected: yyyy-MM-dd HH:mm:ss\"}");
			out.flush();
			out.close();
			return;
		}

		// Get the appropriate connection — live for today's date, historical H2 for past dates
		DbxConnection conn = null;
		if (PersistentCounterHandler.hasInstance())
		{
			PersistWriterJdbc writer = PersistentCounterHandler.getInstance().getPersistWriterJdbc();
			if (writer != null)
				conn = CmDataServlet.getConnectionForTimestamp(ts, writer);
		}

		if (conn == null)
		{
			out.println("{\"error\":\"no-data\",\"message\":\"No storage connection available\"}");
			out.flush();
			out.close();
			return;
		}

		if (!CounterController.hasInstance())
		{
			out.println("{\"error\":\"no-data\",\"message\":\"No CounterController instance available\"}");
			out.flush();
			out.close();
			return;
		}

		ICounterController cc = CounterController.getInstance();
		List<CountersModel> cmList = cc.getCmList();
		if (cmList == null || cmList.isEmpty())
		{
			out.println("{\"error\":\"no-data\",\"message\":\"No CounterModels registered\"}");
			out.flush();
			out.close();
			return;
		}

		// Window timestamps: ±60 seconds
		Timestamp tsFrom = new Timestamp(ts.getTime() - 60_000L);
		Timestamp tsTo   = new Timestamp(ts.getTime() + 60_000L);

		// Single query to MonSessionSampleDetailes — same approach as the native GUI PersistReader
		SampleDetails sampleDetails = loadSampleDetails(conn, ts, tsFrom, tsTo);

		// Group CMs preserving registration order
		Map<String, List<CountersModel>> groups = new LinkedHashMap<>();
		for (CountersModel cm : cmList)
		{
			String groupName = cm.getGroupName();
			if (groupName == null)
				groupName = "Other";
			groups.computeIfAbsent(groupName, k -> new ArrayList<>()).add(cm);
		}

		// Build JSON
		SimpleDateFormat tsFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append("\"requestedTime\":").append(jsonStr(timeParam)).append(",");
		if (sampleDetails != null && sampleDetails.resolvedTime != null)
			sb.append("\"resolvedTime\":").append(jsonStr(tsFmt.format(sampleDetails.resolvedTime))).append(",");
		sb.append("\"groups\":[");

		boolean firstGroup = true;
		for (Map.Entry<String, List<CountersModel>> entry : groups.entrySet())
		{
			if (!firstGroup) sb.append(",");
			firstGroup = false;

			String groupName = entry.getKey();
			List<CountersModel> cms = entry.getValue();

			// Use the icon of the first CM in the group as the group-level icon
			String groupIcon = null;
			for (CountersModel cm : cms)
			{
				String f = cm.getIconFile();
				if (f != null && !f.trim().isEmpty()) { groupIcon = f.trim(); break; }
			}

			sb.append("{");
			sb.append("\"groupName\":").append(jsonStr(groupName)).append(",");
			sb.append("\"groupIcon\":").append(groupIcon == null ? "null" : jsonStr(groupIcon)).append(",");
			sb.append("\"cms\":[");

			boolean firstCm = true;
			for (CountersModel cm : cms)
			{
				if (!firstCm) sb.append(",");
				firstCm = false;

				String cmName      = cm.getName();
				String displayName = cm.getDisplayName();
				if (displayName == null || displayName.trim().isEmpty())
					displayName = cmName;
				String iconFile = cm.getIconFile();

				// Default: assume no data if we couldn't query MonSessionSampleDetailes
				boolean hasData           = false;
				int     absRows           = 0;
				int     diffRows          = 0;
				int     rateRows          = 0;
				String  exceptionMsg      = null;
				String  exceptionFullText = null;

				if (sampleDetails != null)
				{
					CmSampleInfo info = sampleDetails.cms.get(cmName);
					if (info != null)
					{
						// Mirror PersistReader.getPrevSample()/getNextSample(): a CM "has data" only
						// when it actually saved rows — not just that it ran (hasValidSampleData).
						// CmLocks/CmSqlDynamic etc. may have hasValidSampleData=1 but absRows=0
						// when nothing was found that sample; no point showing an empty table as blue.
						absRows          = info.absRows;
						diffRows         = info.diffRows;
						rateRows         = info.rateRows;
						hasData          = (absRows > 0 || diffRows > 0 || rateRows > 0);
						exceptionMsg     = info.exceptionMsg;
						exceptionFullText = info.exceptionFullText;
					}
				}

				sb.append("{");
				sb.append("\"cmName\":").append(jsonStr(cmName)).append(",");
				sb.append("\"displayName\":").append(jsonStr(displayName)).append(",");
				sb.append("\"iconFile\":").append(iconFile == null ? "null" : jsonStr(iconFile)).append(",");
				sb.append("\"hasData\":").append(hasData).append(",");
				sb.append("\"absRows\":") .append(absRows) .append(",");
				sb.append("\"diffRows\":").append(diffRows).append(",");
				sb.append("\"rateRows\":").append(rateRows);
				if (exceptionMsg      != null) sb.append(",\"exceptionMsg\":")     .append(jsonStr(exceptionMsg));
				if (exceptionFullText != null) sb.append(",\"exceptionFullText\":").append(jsonStr(exceptionFullText));
				sb.append("}");
			}

			sb.append("]}");
		}

		sb.append("]}");

		out.println(sb.toString());
		out.flush();
		out.close();
	}

	// -------------------------------------------------------------------------
	// Inner data holders
	// -------------------------------------------------------------------------

	/** Per-CM info read from one MonSessionSampleDetailes row. */
	private static class CmSampleInfo
	{
		boolean hasValidSampleData;
		int     absRows;
		int     diffRows;
		int     rateRows;
		String  exceptionMsg;      // null when no error
		String  exceptionFullText; // null when no error (may be very long)
	}

	/** Result of a single MonSessionSampleDetailes query covering a time window. */
	private static class SampleDetails
	{
		Timestamp                 resolvedTime; // the SessionSampleTime actually chosen
		Map<String, CmSampleInfo> cms = new LinkedHashMap<>();
	}

	// -------------------------------------------------------------------------
	// MonSessionSampleDetailes lookup
	// -------------------------------------------------------------------------

	/**
	 * Query {@code MonSessionSampleDetailes} within [tsFrom, tsTo], pick the
	 * {@code SessionSampleTime} closest to {@code ts}, and return a
	 * {@link SampleDetails} with one {@link CmSampleInfo} entry per CM.
	 * <p>
	 * Mirrors {@code PersistReader.getPrevSample()} / {@code getNextSample()} which
	 * also query this table — it is guaranteed to exist on every collector instance.
	 *
	 * @return populated {@link SampleDetails}, or {@code null} on unexpected error
	 *         (caller will default all CMs to hasData=false).
	 */
	private SampleDetails loadSampleDetails(DbxConnection conn, Timestamp ts, Timestamp tsFrom, Timestamp tsTo)
	{
		String sql = conn.quotifySqlString(
				  "SELECT [CmName], [SessionSampleTime], [hasValidSampleData], [absRows], [diffRows], [rateRows], [exceptionMsg], [exceptionFullText] "
				+ "FROM [MonSessionSampleDetailes] "
				+ "WHERE [SessionSampleTime] BETWEEN ? AND ? "
				+ "ORDER BY [SessionSampleTime]");

		try (PreparedStatement pstmt = conn.prepareStatement(sql))
		{
			pstmt.setTimestamp(1, tsFrom);
			pstmt.setTimestamp(2, tsTo);

			// Collect rows grouped by SessionSampleTime
			Map<Long, Map<String, CmSampleInfo>> byTs = new LinkedHashMap<>();
			try (ResultSet rs = pstmt.executeQuery())
			{
				while (rs.next())
				{
					String    cmName   = rs.getString("CmName");
					Timestamp sst      = rs.getTimestamp("SessionSampleTime");
					boolean   hasValid = rs.getBoolean("hasValidSampleData");
					int       absR     = rs.getInt("absRows");
					int       diffR    = rs.getInt("diffRows");
					int       rateR    = rs.getInt("rateRows");
					String    excMsg   = rs.getString("exceptionMsg");
					String    excFull  = rs.getString("exceptionFullText");
					if (excMsg  != null && excMsg .trim().isEmpty()) excMsg  = null;
					if (excFull != null && excFull.trim().isEmpty()) excFull = null;

					CmSampleInfo info = new CmSampleInfo();
					info.hasValidSampleData = hasValid;
					info.absRows            = absR;
					info.diffRows           = diffR;
					info.rateRows           = rateR;
					info.exceptionMsg       = excMsg;
					info.exceptionFullText  = excFull;

					byTs.computeIfAbsent(sst.getTime(), k -> new LinkedHashMap<>()).put(cmName, info);
				}
			}

			if (byTs.isEmpty())
				return new SampleDetails(); // no rows in window — all CMs will show hasData=false

			// Pick the SessionSampleTime closest to the requested ts
			long targetMs = ts.getTime();
			long bestMs   = Collections.min(byTs.keySet(), Comparator.comparingLong(t -> Math.abs(t - targetMs)));

			SampleDetails sd = new SampleDetails();
			sd.resolvedTime  = new Timestamp(bestMs);
			sd.cms           = byTs.get(bestMs);
			return sd;
		}
		catch (Exception ex)
		{
			_logger.warn("loadSampleDetails: error querying MonSessionSampleDetailes: {}", ex.getMessage(), ex);
			return null;
		}
	}

	private static String jsonStr(String s)
	{
		if (s == null) return "null";
		return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
	}
}
