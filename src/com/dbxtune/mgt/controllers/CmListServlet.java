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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
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

import com.dbxtune.central.controllers.Helper;
import com.dbxtune.cm.CmHighlighterDescriptor;
import com.fasterxml.jackson.databind.ObjectMapper;

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
		ObjectMapper om  = Helper.createObjectMapper();
		PrintWriter  out = resp.getWriter();

		String timeParam = Helper.getParameter(req, "time");
		if (timeParam == null)
		{
			om.writeValue(out, errMap("missing-param", "Missing required parameter: time"));
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
			om.writeValue(out, errMap("invalid-param", "Invalid time format, expected: yyyy-MM-dd HH:mm:ss"));
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
			om.writeValue(out, errMap("no-data", "No storage connection available"));
			out.flush();
			out.close();
			return;
		}

		if (!CounterController.hasInstance())
		{
			om.writeValue(out, errMap("no-data", "No CounterController instance available"));
			out.flush();
			out.close();
			return;
		}

		ICounterController cc = CounterController.getInstance();
		List<CountersModel> cmList = cc.getCmList();
		if (cmList == null || cmList.isEmpty())
		{
			om.writeValue(out, errMap("no-data", "No CounterModels registered"));
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

		// Build JSON response via Jackson
		SimpleDateFormat tsFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

		Map<String, Object> root = new LinkedHashMap<>();
		root.put("requestedTime", timeParam);
		if (sampleDetails != null && sampleDetails.resolvedTime != null)
			root.put("resolvedTime", tsFmt.format(sampleDetails.resolvedTime));

		List<Map<String, Object>> groupList = new ArrayList<>();
		for (Map.Entry<String, List<CountersModel>> entry : groups.entrySet())
		{
			String groupName = entry.getKey();
			List<CountersModel> cms = entry.getValue();

			String groupIcon = null;
			for (CountersModel cm : cms)
			{
				String f = cm.getIconFile();
				if (f != null && !f.trim().isEmpty()) { groupIcon = f.trim(); break; }
			}

			List<Map<String, Object>> cmList2 = new ArrayList<>();
			for (CountersModel cm : cms)
			{
				String cmName      = cm.getName();
				String displayName = cm.getDisplayName();
				if (displayName == null || displayName.trim().isEmpty())
					displayName = cmName;
				String iconFile = cm.getIconFile();

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
						absRows           = info.absRows;
						diffRows          = info.diffRows;
						rateRows          = info.rateRows;
						hasData           = (absRows > 0 || diffRows > 0 || rateRows > 0);
						exceptionMsg      = info.exceptionMsg;
						exceptionFullText = info.exceptionFullText;
					}
				}

				Map<String, Object> cmMap = new LinkedHashMap<>();
				String description = cm.getDescription();
				List<CmHighlighterDescriptor> highlighterDescriptors = cm.getHighlighterDescriptors();

				cmMap.put("cmName",      cmName);
				cmMap.put("displayName", displayName);
				cmMap.put("iconFile",    iconFile);
				cmMap.put("isActive",    cm.isActive());
				cmMap.put("hasData",     hasData);
				cmMap.put("absRows",     absRows);
				cmMap.put("diffRows",    diffRows);
				cmMap.put("rateRows",    rateRows);
				if (description            != null) cmMap.put("description",            description);
				if (highlighterDescriptors != null) cmMap.put("highlighterDescriptors", highlighterDescriptors);
				if (exceptionMsg      != null) cmMap.put("exceptionMsg",      exceptionMsg);
				if (exceptionFullText != null) cmMap.put("exceptionFullText", exceptionFullText);
				cmList2.add(cmMap);
			}

			Map<String, Object> groupMap = new LinkedHashMap<>();
			groupMap.put("groupName", groupName);
			groupMap.put("groupIcon", groupIcon);
			groupMap.put("cms",       cmList2);
			groupList.add(groupMap);
		}
		root.put("groups", groupList);

		om.writeValue(out, root);
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
					String    cmName   = rs.getString   ("CmName");
					Timestamp sst      = rs.getTimestamp("SessionSampleTime");
					boolean   hasValid = rs.getBoolean  ("hasValidSampleData");
					int       absR     = rs.getInt      ("absRows");
					int       diffR    = rs.getInt      ("diffRows");
					int       rateR    = rs.getInt      ("rateRows");
					String    excMsg   = rs.getString   ("exceptionMsg");
					String    excFull  = rs.getString   ("exceptionFullText");

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

	/** Builds a simple error response map for Jackson serialisation. */
	private static Map<String, Object> errMap(String code, String message)
	{
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("error",   code);
		m.put("message", message);
		return m;
	}
}
