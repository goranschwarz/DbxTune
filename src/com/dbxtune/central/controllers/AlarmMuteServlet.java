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
import java.lang.invoke.MethodHandles;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.alarm.AlarmMuteManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * REST endpoint for alarm mute/unmute.
 *
 *  GET  /api/alarm/mute           -> JSON map of all current mutes
 *  POST /api/alarm/mute           -> body: { alarmId, srvName, action:"mute"|"unmute",
 *                                            reason, mutedByUser, expiresHours }
 *                                  -> { "status": "ok" }
 */
public class AlarmMuteServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private final ObjectMapper _om = Helper.createObjectMapper();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		try
		{
			resp.getOutputStream().println(_om.writeValueAsString(AlarmMuteManager.getInstance().getAllMutes()));
		}
		catch (Exception e)
		{
			_logger.error("AlarmMuteServlet.doGet: error", e);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		try
		{
			JsonNode body    = _om.readTree(req.getInputStream());
			String alarmId   = body.has("alarmId")  ? body.get("alarmId") .asText(null) : null;
			String action    = body.has("action")   ? body.get("action")  .asText("mute") : "mute";

			if (alarmId == null || alarmId.isEmpty())
			{
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required field: alarmId");
				return;
			}

			AlarmMuteManager mgr = AlarmMuteManager.getInstance();
			if ("unmute".equalsIgnoreCase(action))
			{
				mgr.unmute(alarmId);
			}
			else
			{
				String  srvName      = body.has("srvName")      ? body.get("srvName")     .asText("") : "";
				String  reason       = body.has("reason")       ? body.get("reason")      .asText("") : "";
				String  mutedByUser  = body.has("mutedByUser")  ? body.get("mutedByUser") .asText("") : "";
				Integer expiresHours = body.has("expiresHours") && !body.get("expiresHours").isNull()
				                       ? body.get("expiresHours").asInt(0) : null;
				mgr.mute(alarmId, srvName, reason, mutedByUser, expiresHours);
			}
			resp.getOutputStream().println("{\"status\":\"ok\"}");
		}
		catch (Exception e)
		{
			_logger.error("AlarmMuteServlet.doPost: error", e);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}
}
