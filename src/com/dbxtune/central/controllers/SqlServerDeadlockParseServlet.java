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
package com.dbxtune.central.controllers;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cm.sqlserver.CmDeadlocks.Deadlock;
import com.dbxtune.cm.sqlserver.CmDeadlocks.DlProcess;
import com.dbxtune.cm.sqlserver.CmDeadlocks.DlResource;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <pre>POST /api/sqlserver/deadlock-parse   (form params: xml, eventTime)</pre>
 * Parses a SQL Server deadlock report (the <code>DeadlockXml</code> of a <code>CmDeadlocks</code> row, or anything pasted
 * into the Deadlock Viewer: <code>&lt;deadlock&gt;</code>, <code>&lt;deadlock-list&gt;</code>, a XE event) with
 * {@link Deadlock#parseDeadlock(String, Timestamp)}, the same parser as the DSR and the DeadlockCount alarm,
 * and returns it as JSON. The "Row Detail" dialog in Counter Details draws it (SVG, with tooltips).
 * <p>
 * Times are 'yyyy-MM-dd HH:mm:ss.SSS' (server local time, as in the report). On a parse error: <code>{"error": "..."}</code>.
 */
public class SqlServerDeadlockParseServlet
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private static final Pattern DEADLOCK_START   = Pattern.compile("<deadlock[ >]");
	private static final Pattern DEADLOCK_ELEMENT = Pattern.compile("<deadlock[ >].*?</deadlock>", Pattern.DOTALL); // the first one
	private static final Pattern XE_EVENT         = Pattern.compile("<event [^>]*xml_deadlock_report.*?</event>", Pattern.DOTALL);

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		String xml          = req.getParameter("xml");
		String eventTimeStr = req.getParameter("eventTime");

		if (StringUtil.isNullOrBlank(xml))
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter 'xml' is required.");
			return;
		}

		// EventTime as the Counter Details data has it: 'yyyy-MM-dd HH:mm:ss.SSS'
		Timestamp eventTime = null;
		try { if (StringUtil.hasValue(eventTimeStr)) eventTime = Timestamp.valueOf(eventTimeStr.trim()); }
		catch (IllegalArgumentException ignore) {}

		Map<String, Object> out = new LinkedHashMap<>();
		try
		{
			// Any paste (Deadlock Viewer): a <deadlock> (CmDeadlocks, SSMS .xdl), a <deadlock-list> (trace flag 1222 / Profiler),
			// or a XE event (<event name="xml_deadlock_report" timestamp="...">). Several: the first one is shown.
			Deadlock d;
			String eventTimeNote = null;
			int deadlockCount = 0;
			for (Matcher m = DEADLOCK_START.matcher(xml); m.find(); )
				deadlockCount++;

			Matcher ev = XE_EVENT.matcher(xml);
			Matcher dl = DEADLOCK_ELEMENT.matcher(xml);
			if (eventTime == null && ev.find())
			{
				d = Deadlock.parseEvent(ev.group(), 0); // the server's UTC offset is not in the event
				eventTimeNote = d.eventTime == null ? null : "UTC";
			}
			else if (dl.find())
				d = Deadlock.parseDeadlock(dl.group(), eventTime);
			else
				throw new Exception("No <deadlock> element found. Paste a deadlock report: <deadlock>...</deadlock> (.xdl), <deadlock-list> or a 'xml_deadlock_report' event.");

			// No time in the report: the deadlock was found after every session's transaction start + its wait time
			if (d.eventTime == null)
			{
				for (DlProcess p : d.processes)
					if (p.tranStarted != null && (d.eventTime == null || p.tranStarted.getTime() + p.waitTimeMs > d.eventTime.getTime()))
						d.eventTime = new Timestamp(p.tranStarted.getTime() + p.waitTimeMs);
				eventTimeNote = d.eventTime == null ? null : "estimated";
			}

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

			out.put("eventTime"    , d.eventTime == null ? null : sdf.format(d.eventTime));
			out.put("eventTimeNote", eventTimeNote);
			out.put("deadlockCount", deadlockCount);
			out.put("parallel"     , d.parallel);

			// The same values as the CmDeadlocks columns (the Deadlock Viewer shows them like the Counter Details "Row Detail")
			DlProcess v = d.getVictim();
			Map<String, Object> cols = new LinkedHashMap<>();
			cols.put("EventTime"      , d.eventTime == null ? null : sdf.format(d.eventTime) + (eventTimeNote == null ? "" : " (" + eventTimeNote + ")"));
			cols.put("DeadlockId"     , d.getDeadlockId());
			cols.put("DbName"         , d.getVictimDbName());
			cols.put("ProcessCount"   , d.getSpids().size());
			cols.put("IsParallel"     , d.parallel);
			cols.put("VictimSpid"     , v == null ? null : v.spid);
			cols.put("VictimLogin"    , v == null ? null : v.login);
			cols.put("VictimHost"     , v == null ? null : v.host);
			cols.put("VictimApp"      , v == null ? null : v.app);
			cols.put("VictimProc"     , v == null ? null : v.getProcNameAndLine());
			cols.put("VictimStatement", v == null ? null : v.getStatement());
			cols.put("OtherSpids"     , d.getOtherSpids());
			cols.put("OtherProcs"     , d.getOtherProcs());
			cols.put("OtherStatements", d.getOtherStatements());
			cols.put("Objects"        , d.getObjects());
			cols.put("LockTypes"      , d.getLockTypes());
			cols.put("IsolationLevels", d.getIsolationLevels());
			cols.put("MaxWaitTimeMs"  , d.getMaxWaitTimeMs());
			cols.put("ShapeKey"       , d.getShapeKey());
			out.put("columns", cols);
			out.put("xml"    , d.xml);

			List<Map<String, Object>> processes = new ArrayList<>();
			for (DlProcess p : d.processes)
			{
				Map<String, Object> m = new LinkedHashMap<>();
				m.put("id"            , p.id);
				m.put("spid"          , p.spid);
				m.put("ecid"          , p.ecid);
				m.put("victim"        , p.victim);
				m.put("login"         , p.login);
				m.put("host"          , p.host);
				m.put("app"           , p.app);
				m.put("dbName"        , p.dbName);
				m.put("isolationLevel", p.isolationLevel);
				m.put("waitResource"  , p.waitResource);
				m.put("lockMode"      , p.lockMode);
				m.put("waitTimeMs"    , p.waitTimeMs);
				m.put("tranStarted"   , p.tranStarted == null ? null : sdf.format(p.tranStarted));
				m.put("tranName"      , p.tranName);
				m.put("procName"      , p.procName);
				m.put("procLine"      , p.procLine);
				m.put("statement"     , p.getStatement());
				m.put("waitResourceSql", d.getWaitResourceSql(p));
				processes.add(m);
			}
			out.put("processes", processes);

			List<Map<String, Object>> resources = new ArrayList<>();
			for (DlResource r : d.resources)
			{
				Map<String, Object> m = new LinkedHashMap<>();
				m.put("type"      , r.type);
				m.put("objectName", r.objectName);
				m.put("indexName" , r.indexName);
				m.put("mode"      , r.mode);

				List<Map<String, String>> owners = new ArrayList<>();
				for (int i = 0; i < r.ownerIds.size(); i++)
					owners.add(Map.of("id", r.ownerIds.get(i), "mode", r.ownerModes.get(i)));
				m.put("owners", owners);

				List<Map<String, String>> waiters = new ArrayList<>();
				for (int i = 0; i < r.waiterIds.size(); i++)
					waiters.add(Map.of("id", r.waiterIds.get(i), "mode", r.waiterModes.get(i)));
				m.put("waiters", waiters);

				resources.add(m);
			}
			out.put("resources", resources);
		}
		catch (Exception ex)
		{
			_logger.info("Could not parse the deadlock XML: " + ex);
			out.clear();
			out.put("error", "Could not parse the deadlock XML: " + ex);
		}

		resp.setContentType("application/json; charset=UTF-8");
		resp.setCharacterEncoding("UTF-8");
		new ObjectMapper().writeValue(resp.getWriter(), out);
	}
}
