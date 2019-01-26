/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.central.controllers;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.servlets.EventSource;
import org.eclipse.jetty.servlets.EventSource.Emitter;
import org.eclipse.jetty.servlets.EventSourceServlet;

import com.asetune.central.pcs.DbxTuneSample;
import com.asetune.utils.StringUtil;

public class ChartBroadcastServlet
extends EventSourceServlet
{
	private static final long serialVersionUID = 1L;
//	private final static Logger _logger = LoggerFactory.getLogger(ChartBroadcastServlet.class);
	private static final Logger _logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

//	private static final Map<String, Emitter> sseMap = new ConcurrentHashMap<>();
	private static final Map<String, ClientSubscription> _subsMap = new ConcurrentHashMap<>();
	
	private static class ClientSubscription
	{
//		String _serverName = null; // to be removed when the below list functionality has been implemented
		
		/** What "server names" do the client subscribe on, if null or empty then it's all servers */
		List<String>  _serverNameList;
		
		/** What "graph names" do the client subscribe on, if null or empty then it's all graphs */
		List<String>  _graphNameList;
		
		/** The Clients data handler */
		Emitter _emitter = null;

		/** If we want to keep track of what hosts that are subscribing */
		String _remoteHost = null;
		
		public ClientSubscription(String serverName, String graphNames, String remoteHost, Emitter emitter)
		{
//			_serverName = serverName;
			_serverNameList = StringUtil.commaStrToList(serverName);
			_graphNameList  = StringUtil.commaStrToList(graphNames);
			_remoteHost = remoteHost;
			_emitter    = emitter;
		}
	}

//	/**
//	 * Send a Server Sent Event to any clients that has registered on: /api/pcs/graph-data/{name}
//	 * @param payload what to send
//	 * @param payload2 
//	 */
//	public static void fireGraphData(String name, String payload)
//	{
//		System.out.println("fireGraphData: sseMap.size() = " + sseMap.size());
//		for (String key : sseMap.keySet())
//		{
//			Emitter sse = sseMap.get(key);
//
//			System.out.println("fireGraphData(key='"+key+"', emitter='"+sse+"'): name=|"+name+"|, payload=|"+payload+"|.");
//
//			try
//			{
//				//sse.event(name, payload);
//				sse.data(payload);
//			}
//			catch (IOException e)
//			{
//				e.printStackTrace();
//				System.out.println("Removing key='"+key+"' from client map.");
//				sseMap.remove(key);
//			}
//		};
//	}

	@Override
	protected EventSource newEventSource(final HttpServletRequest request)
	{
		return new EventSource()
		{
			private String _uuid = null;
			
			@Override
			public void onOpen(Emitter emitter) throws IOException
			{
				_uuid = UUID.randomUUID().toString();
				String serverList = request.getParameter("serverList");
				String graphList  = request.getParameter("graphList");
				String remoteHost = request.getRemoteHost();

				_logger.info("Adding a Web Subscriber. remoteHost='"+remoteHost+"', serverList='"+serverList+"', graphList='"+graphList+"', uuid='"+_uuid+"'.");
				
				_subsMap.put(_uuid, new ClientSubscription(serverList, graphList, remoteHost, emitter));
			}

			@Override
			public void onClose()
			{
				_logger.debug("ChartBroadcastServlet.newEventSource(): onClose(): REMOVING - uuid='"+_uuid+"'");
				_subsMap.remove(_uuid);
			}
		};
	}

	/**
	 * Send "live" data to any web "subscribers", so the subscibing browsers can be updated there graphs/charts
	 * @param sample
	 */
	public static void fireGraphData(DbxTuneSample sample)
	{
		if (_subsMap.isEmpty())
		{
			return;
		}

		List<String> subsSentList = new ArrayList<>();
		
		for (String key : _subsMap.keySet())
		{
			ClientSubscription cs = _subsMap.get(key);
//			Emitter sse = sseMap.get(key);
//			String  subscribeToServer = cs._serverName;
			List<String> srvNameList   = cs._serverNameList;
			List<String> graphNameList = cs._graphNameList;
			Emitter      emitter       = cs._emitter;

			if (_logger.isDebugEnabled())
				_logger.debug("fireGraphData(key='"+key+"', remoteHost='"+cs._remoteHost+"', srvNameList='"+srvNameList+"', graphNameList='"+graphNameList+"', emitter='"+cs._emitter+"'): name=|"+sample.getServerName()+"|.");

			// If we are NOT subscribing on this server... -get-out-of-here-
			if ( ! srvNameList.contains(sample.getServerName()) )
			{
				if (_logger.isDebugEnabled())
					_logger.debug("<<<<<----fireGraphData -NOT-A-SUBSCRIBER----DO-RETURN---- sample.getServerName()='"+sample.getServerName()+"': (key='"+key+"', srvNameList='"+srvNameList+"', graphNameList='"+graphNameList+"'.");
				continue;
			}
			
			String payload;
			try
			{
				payload = sample.getJsonForGraphs(graphNameList);
			}
			catch (IOException e)
			{
				_logger.error("Problems generating JSON.", e);
				return;
			}

			try
			{
				//emitter.event(name, payload);
				emitter.data(payload);

				// sent for this remote connection
				subsSentList.add(cs._remoteHost);
			}
			catch (EofException e) 
			{
				_logger.debug("EofException from subscriber.");
				_subsMap.remove(key);
			}
			catch (IOException e)
			{
				_logger.error("Problems sending data to subscriber '"+key+"'.", e);
				_subsMap.remove(key);
			}
		}

		_logger.info("Sent subscription data for server '"+sample.getServerName()+"' to "+subsSentList.size()+" Web Subscribers "+subsSentList+". subsMap.size="+_subsMap.size());

	}

}
