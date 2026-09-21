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
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.ICounterController;
import com.dbxtune.central.controllers.Helper;
import com.dbxtune.cm.CountersModel;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Collector-side servlet: POST /api/mgt/cm/force-refresh?cm=CmName[&amp;refreshNow=true]
 * <p>
 * Requests that the CM is refreshed on the <b>next</b> regular sample, ignoring any postpone time (one-shot).
 * Uses the CM "demand refresh list" via {@link ICounterController#requestCmRefreshOnNextSample(String)}.
 * <p>
 * With {@code refreshNow=true} the collector's sleep is also interrupted (same as /api/mgt/refresh),
 * so the next sample starts immediately. NOTE: that sample is taken early for ALL CMs (shorter sample interval).
 * <p>
 * Returns JSON: {@code {"cm": "CmName", "requested": true, "refreshNow": false}}
 * or {@code {"error": "...", "message": "..."}}
 */
public class CmForceRefreshServlet
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		ObjectMapper om  = Helper.createObjectMapper();
		PrintWriter  out = resp.getWriter();

		String cmName = Helper.getParameter(req, "cm");
		if (cmName == null || cmName.trim().isEmpty())
		{
			om.writeValue(out, errMap("missing-param", "Missing required parameter: cm"));
			out.flush(); out.close();
			return;
		}
		cmName = cmName.trim();

		if ( ! CounterController.hasInstance() )
		{
			om.writeValue(out, errMap("no-counter-controller", "No Counter Controller was found"));
			out.flush(); out.close();
			return;
		}

		ICounterController cc = CounterController.getInstance();
		CountersModel cm = cc.getCmByName(cmName);
		if (cm == null)
		{
			om.writeValue(out, errMap("cm-not-found", "CM '" + cmName + "' was not found"));
			out.flush(); out.close();
			return;
		}

		boolean refreshNow = "true".equalsIgnoreCase(Helper.getParameter(req, "refreshNow", "false").trim());

		// Mark first, then wake up: the new sample loop moves the mark into the demand list
		cc.requestCmRefreshOnNextSample(cm.getName());
		if (refreshNow)
			cc.doInterruptSleep();
		_logger.info("Force refresh " + (refreshNow ? "NOW" : "on next sample") + " was requested by the NO-GUI Management interface for CM '" + cm.getName() + "' (remote=" + req.getRemoteAddr() + ").");

		Map<String, Object> m = new LinkedHashMap<>();
		m.put("cm",         cm.getName());
		m.put("requested",  true);
		m.put("refreshNow", refreshNow);
		om.writeValue(out, m);
		out.flush();
		out.close();
	}

	private static Map<String, Object> errMap(String code, String message)
	{
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("error",   code);
		m.put("message", message);
		return m;
	}
}
