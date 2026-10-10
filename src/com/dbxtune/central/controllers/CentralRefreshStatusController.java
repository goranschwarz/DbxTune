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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Receives "what is the collector doing right now" from a collector, when a sample takes long time.<br>
 * The message is forwarded (as is) to the web browsers that show graphs for that server (WebSocket: /api/chart/broadcast-ws)
 * <p>
 * Sent by: RefreshStatusNoGui, using PersistWriterToDbxCentral.sendRefreshStatus()
 * <pre>
 * POST /api/pcs/refresh-status
 * {"type":"refreshStatus","serverName":"PROD_A","refreshing":true,"status":"Refreshing... CmIndexUsage","statusMs":42000,
 *  "subStatus":"for db 'sales'","subStatusMs":3000,"sampleMs":65000,"sampleIntervalSec":60}
 * </pre>
 * When the sample has ended, the collector sends <code>"refreshing":false</code>
 */
public class CentralRefreshStatusController
extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	private static final ObjectMapper _mapper = new ObjectMapper();

	// curl -X POST -d '{"type":"refreshStatus","serverName":"PROD_A","refreshing":true,"status":"Refreshing... CmXxx","statusMs":12000,"subStatus":"","subStatusMs":0,"sampleMs":15000,"sampleIntervalSec":60}' http://localhost:8080/api/pcs/refresh-status
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		// Same check as when the collector sends counter data
		if ( ! CentralPcsReceiverController.checkRemoteHostAllowed(req, resp) )
			return;

		String payload = CentralPcsReceiverController.getBody(req);

		String  srvName;
		boolean refreshing;
		try
		{
			JsonNode root = _mapper.readTree(payload);
			if (root == null || ! root.isObject() || ! "refreshStatus".equals(root.path("type").asText()) || StringUtil.isNullOrBlank(root.path("serverName").asText()))
			{
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Expected a JSON object with fields: \"type\":\"refreshStatus\" and \"serverName\"");
				return;
			}
			srvName    = root.path("serverName").asText();
			refreshing = root.path("refreshing").asBoolean(false);
		}
		catch (JsonProcessingException ex)
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Not a valid JSON message. " + ex.getOriginalMessage());
			return;
		}

		ChartBroadcastWebSocket.fireRefreshStatus(srvName, refreshing, payload);

		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		resp.getOutputStream().print("{\"status\":\"ok\"}");
	}
}
