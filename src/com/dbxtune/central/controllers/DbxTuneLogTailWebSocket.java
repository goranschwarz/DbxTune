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

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import com.dbxtune.central.DbxTuneCentral;


@WebSocket
public class DbxTuneLogTailWebSocket
{
	private static final Logger _logger = Logger.getLogger(MethodHandles.lookup().lookupClass());
	
	private FileTailer _fileTailer;
	
	@OnWebSocketMessage
	public void onText(Session session, String message) throws IOException
	{
		_logger.info("log-tail: Message Received from a Web Subscriber. session='"+session+"'. message=|"+message+"|.");

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

		String filename = parameterMap.get("name").get(0);
		String remoteHost = session.getRemoteAddress().getHostString(); // or getHostName();

//		System.out.println("xxx: filename='"+filename+"', remoteHost='"+remoteHost+"'.");
		
		String fullFilename = DbxTuneCentral.getAppLogDir() + File.separatorChar + filename;
		File f = new File(fullFilename);
		if ( ! f.exists() )
		{
			System.out.println("DbxTuneLogTailWebSocket.onConnect(): File not found: '"+fullFilename+"'.");
//			throw new IOException("File not found: '"+fullFilename+"'.");
		}

		_fileTailer = new FileTailer(session, fullFilename);
		_fileTailer.start();

//		ClientSubscription cs = new ClientSubscription(serverList, graphList, remoteHost);
//
//		_subsMap.put(session, cs);
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
		_logger.info("log-tail: Adding a Web Subscriber. remoteHost='"+remoteHost+"', filename='"+filename+"', session='"+session+"'.");
	}

	@OnWebSocketClose
	public void onClose(Session session, int status, String reason)
	{
		if (_fileTailer != null)
		{
			_fileTailer.stop();
			_fileTailer = null;;
		}

//		ClientSubscription cs = _subsMap.remove(session);
//
//		if (cs != null)
//			_logger.info("Removed a Web Subscriber. remoteHost='"+cs._remoteHost+"', serverList='"+cs._serverNameList+"', graphList='"+cs._graphNameList+"', session='"+session+"'.");
		_logger.info("log-tail: Removed a Web Subscriber. session='"+session+"'.");
	}
	
	
	// session.getRemote().sendString(payload);
	private static class FileTailer implements TailerListener
	{
		private String _filename;
		private Tailer _tailer;
		private Thread _tailerThread;

		private Session _session;

		public FileTailer(Session session, String filename)
		{
//			super(new File(filename), this, 500);

			_filename = filename;
			final File f = new File(filename);
			final long pollIntervalMs = 500;
//			final long pollIntervalMs = 1000;
			boolean startAtEnd = true;
			
			_tailer = new Tailer(f, this, pollIntervalMs, startAtEnd);

			_session = session;
		}

		public void start()
		{
			_tailerThread = new Thread(_tailer);
			_tailerThread.setDaemon(true);
			_tailerThread.setName("tail:"+_filename);
			_tailerThread.start();
		}

		public void stop()
		{
			_logger.info("Stopping tail thread for '"+_filename+"'. session='"+_session+"'.");
			_tailer.stop();
		}

		@Override
		public void init(Tailer tailer)
		{
			_logger.info("TailerListener.init(): tailer="+tailer);
		}

		@Override
		public void fileNotFound()
		{
			_logger.info("TailerListener.fileNotFound()");
			sendToClient("TailerListener.fileNotFound(): "+_filename);
		}

		@Override
		public void fileRotated()
		{
			_logger.info("TailerListener.fileRotated()");
		}

		@Override
		public void handle(String line)
		{
//			_logger.info("TailerListener.handle(): line='"+line+"'.");
			sendToClient(line);
		}

		@Override
		public void handle(Exception ex)
		{
			_logger.info("TailerListener.handle(): ex='"+ex+"'.");
		}

		private void sendToClient(String msg)
		{
			try
			{
				msg = DbxTuneLogServlet.htmlColorizeLogLine(msg);
				
				_session.getRemote().sendString(msg);
			}
			catch (IOException e)
			{
				_logger.warn("Problem sending log line to client. filename='"+_filename+"'.", e);
				e.printStackTrace();
			}
		}
	}

}
