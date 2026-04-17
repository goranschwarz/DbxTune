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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.pcs.PersistWriterJdbc;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.dbxtune.central.controllers.Helper;

/**
 * Collector-side servlet: {@code GET /api/mgt/recording-databases}
 * <p>
 * Returns the list of available H2 recording database date keys for this
 * collector, sorted newest-first.  Used by the UI to populate the historical
 * date picker in the Query Store panel (and any other panel that accepts a
 * {@code ts} parameter).
 *
 * <h3>Response</h3>
 * <pre>{@code
 * {
 *   "dateRollingEnabled": true,
 *   "currentDateKey":  "2026-04-13",
 *   "availableDates":  ["2026-04-13", "2026-04-12", "2026-04-11"]
 * }
 * }</pre>
 *
 * When date rolling is not configured {@code dateRollingEnabled} is
 * {@code false} and {@code availableDates} is an empty array.
 */
public class RecordingDatabasesServlet
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

		Map<String, Object> root = new LinkedHashMap<>();

		try
		{
			if (!PersistentCounterHandler.hasInstance())
			{
				root.put("dateRollingEnabled", false);
				root.put("currentDateKey",     null);
				root.put("availableDates",     new java.util.ArrayList<>());
				om.writeValue(out, root);
				return;
			}

			PersistWriterJdbc writer = PersistentCounterHandler.getInstance().getPersistWriterJdbc();
			if (writer == null || !writer.isH2DateRolling())
			{
				root.put("dateRollingEnabled", false);
				root.put("currentDateKey",     null);
				root.put("availableDates",     new java.util.ArrayList<>());
				om.writeValue(out, root);
				return;
			}

			String       currentKey     = writer.getCurrentRecordingDatabaseDateKey();
			List<String> availableDates = writer.getAvailableRecordingDatabasesDateKeys();

			root.put("dateRollingEnabled", true);
			root.put("currentDateKey",     currentKey);
			root.put("availableDates",     availableDates);
			om.writeValue(out, root);
		}
		catch (Exception ex)
		{
			_logger.warn("RecordingDatabasesServlet: error listing recording databases: " + ex.getMessage(), ex);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			Map<String, Object> err = new LinkedHashMap<>();
			err.put("error",   "internal-error");
			err.put("message", ex.getMessage());
			om.writeValue(out, err);
		}
		finally
		{
			out.flush();
			out.close();
		}
	}
}
