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
package com.dbxtune.central.mcp;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manages active MCP Server-Sent Events (SSE) connections.
 * <p>
 * Clients connect via {@code GET /mcp} and are registered here. When DbxCentral
 * wants to push a notification (e.g. a new alarm), it calls {@link #broadcast(String, String)}.
 * <p>
 * Stale connections are cleaned up automatically when a write fails.
 */
public class McpSseManager
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private static final McpSseManager _instance = new McpSseManager();

	public static McpSseManager getInstance() { return _instance; }

	// -------------------------------------------------------------------------

	private static class SseClient
	{
		final AsyncContext       async;
		final ServletOutputStream out;

		SseClient(AsyncContext async, ServletOutputStream out)
		{
			this.async = async;
			this.out   = out;
		}
	}

	private final CopyOnWriteArrayList<SseClient> _clients = new CopyOnWriteArrayList<>();

	// -------------------------------------------------------------------------

	private McpSseManager() {}

	/**
	 * Register a new SSE client connection.
	 */
	public void register(AsyncContext async, ServletOutputStream out)
	{
		_clients.add(new SseClient(async, out));
		_logger.debug("MCP SSE client registered. Total active: {}", _clients.size());
	}

	/**
	 * Broadcast a server-sent event to all connected MCP clients.
	 *
	 * @param eventType  SSE event type field (e.g. "notification")
	 * @param jsonData   JSON payload for the data field
	 */
	public void broadcast(String eventType, String jsonData)
	{
		if (_clients.isEmpty())
			return;

		String sseMessage = "event: " + eventType + "\ndata: " + jsonData + "\n\n";

		Iterator<SseClient> it = _clients.iterator();
		while (it.hasNext())
		{
			SseClient client = it.next();
			try
			{
				client.out.print(sseMessage);
				client.out.flush();
			}
			catch (IOException e)
			{
				// Client disconnected — remove and close
				_logger.debug("MCP SSE client disconnected: {}", e.getMessage());
				_clients.remove(client);
				try { client.async.complete(); } catch (Exception ignored) {}
			}
		}
	}

	/**
	 * Returns the number of currently connected SSE clients.
	 */
	public int getClientCount()
	{
		return _clients.size();
	}
}
