/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import com.asetune.central.pcs.DbxTuneSample;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;


//@ServerEndpoint("/clock")
@WebSocket
public class ChartBroadcastWebSocket
{
	private static final Logger _logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

//	private static ScheduledExecutorService	_timer = null;

//	private static Set<Session> _sessions = new ConcurrentHashSet<>();;
//	private static Set<Session> allSessions;
//	private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
	private SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");


//	private static final Map<String, Emitter> sseMap = new ConcurrentHashMap<>();
	private static final Map<Session, ClientSubscription> _subsMap = new ConcurrentHashMap<>();

	/**
	 * Object to hold some extra information for a session
	 */
	private static class ClientSubscription
	{
//		String _serverName = null; // to be removed when the below list functionality has been implemented
		
		/** What "server names" do the client subscribe on, if null or empty then it's all servers */
		List<String>  _serverNameList;
		
		/** What "graph names" do the client subscribe on, if null or empty then it's all graphs */
		List<String>  _graphNameList;
		
		/** If we want to keep track of what hosts that are subscribing */
		String _remoteHost = null;
		
		public ClientSubscription(String serverName, String graphNames, String remoteHost)
		{
//			_serverName = serverName;
			_serverNameList = StringUtil.commaStrToList(serverName);
			_graphNameList  = StringUtil.commaStrToList(graphNames);
			_remoteHost = remoteHost;
		}
	}


	@OnWebSocketMessage
	public void onText(Session session, String message) throws IOException
	{
		ClientSubscription cs = _subsMap.get(session);
		if (cs != null)
			_logger.info("Message Received from a Web Subscriber. remoteHost='"+cs._remoteHost+"', serverList='"+cs._serverNameList+"', graphList='"+cs._graphNameList+"', session='"+session+"'. message=|"+message+"|.");

		// Just responce with the same message, in uppercase
		if ( session.isOpen() )
		{
			String response = message.toUpperCase();
			session.getRemote().sendString(response);
		}
	}

	@OnWebSocketConnect
	public void onConnect(Session session) throws IOException
	{
		Map<String, List<String>> parameterMap = session.getUpgradeRequest().getParameterMap();
//		System.out.println("xxx: parameterMap='"+parameterMap+"'.");

		String serverList = parameterMap.get("serverList").get(0);
		String graphList  = parameterMap.get("graphList").get(0);
		String remoteHost = session.getRemoteAddress().getHostString(); // or getHostName();

//		System.out.println("xxx: serverList='"+serverList+"', graphList='"+graphList+"'.");
		
		ClientSubscription cs = new ClientSubscription(serverList, graphList, remoteHost);

		_subsMap.put(session, cs);
//		if ( _timer == null )
//		{
//			_timer = Executors.newSingleThreadScheduledExecutor();
//			Runnable run = new Runnable()
//			{
//				@Override
//				public void run()
//				{
//					sendTimeToAll();
//				}
//			};
//			_timer.scheduleAtFixedRate(run, 0, 1, TimeUnit.SECONDS);
//		}
		_logger.info("Adding a Web Subscriber. remoteHost='"+remoteHost+"', serverList='"+serverList+"', graphList='"+graphList+"', session='"+session+"'.");
	}

	@OnWebSocketClose
	public void onClose(Session session, int status, String reason)
	{
		ClientSubscription cs = _subsMap.remove(session);

		if (cs != null)
			_logger.info("Removed a Web Subscriber. remoteHost='"+cs._remoteHost+"', serverList='"+cs._serverNameList+"', graphList='"+cs._graphNameList+"', session='"+session+"'.");
	}
	
//	private void sendTimeToAll()
//	{
//		System.out.println("+++++++++++++++++++++++++++");
//		int c = 0;
//		for (Session sess : _subsMap.keySet())
//		{
//			c++;
//			try
//			{
//				String now = timeFormatter.format(new Date());
//				System.out.println("      ---("+c+")--->" + sess.getRemoteAddress().getHostString() + " send '"+now+"'.");
//				sess.getRemote().sendString("Local time: " + now);
//			}
//			catch (IOException ioe)
//			{
//				System.out.println(ioe.getMessage());
//				_subsMap.remove(sess);
//			}
//		}
//	}


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
		
		for (Session session : _subsMap.keySet())
		{
			ClientSubscription cs = _subsMap.get(session);
//			Emitter sse = sseMap.get(key);
//			String  subscribeToServer = cs._serverName;
			List<String> srvNameList   = cs._serverNameList;
			List<String> graphNameList = cs._graphNameList;
			List<String> countersNameList = Arrays.asList(new String[] {"CmActiveStatements"});

			if (_logger.isDebugEnabled())
				_logger.debug("fireGraphData(session='"+session+"', remoteHost='"+cs._remoteHost+"', srvNameList='"+srvNameList+"', graphNameList='"+graphNameList+"'): name=|"+sample.getServerName()+"|.");

			// If we are NOT subscribing on this server... -get-out-of-here-
			if ( ! srvNameList.contains(sample.getServerName()) )
			{
				if (_logger.isDebugEnabled())
					_logger.debug("<<<<<----fireGraphData -NOT-A-SUBSCRIBER----DO-RETURN---- sample.getServerName()='"+sample.getServerName()+"': (session='"+session+"', srvNameList='"+srvNameList+"', graphNameList='"+graphNameList+"'.");
				continue;
			}
			
			String payload;
			try
			{
				payload = sample.getJsonForWebSubscribers(graphNameList, countersNameList);
				
				// DEBUG PRINT
				String debugSrvName = Configuration.getCombinedConfiguration().getProperty("ChartBroadcastWebSocket.debug.sendJson.srvName", "");
				if (debugSrvName.equals(sample.getServerName()))
				{
					System.out.println("ChartBroadcastWebSocket.debug.sendJson.srvName='" + sample.getServerName() + "', JSON: " + payload);
				}
			}
			catch (IOException e)
			{
				_logger.error("Problems generating JSON.", e);
				continue;
			}

			try
			{
				session.getRemote().sendString(payload);

				// sent for this remote connection
				subsSentList.add(cs._remoteHost);
			}
//			catch (IOException e)
			catch (Exception e) // This will also catch: IllegalStateException and other RuntimeException... otherwise caller might have: HTTP error code : 500 (Internal Server Error) 
			{
				_logger.error("Problems sending data to subscriber '"+session+"'.", e);
				_subsMap.remove(session);
			}
		}

		if ( subsSentList.size() > 0 )
			_logger.info("Sent subscription data for server '"+sample.getServerName()+"' to "+subsSentList.size()+" Web Subscribers "+subsSentList+". subsMap.size="+_subsMap.size());

	}
}
