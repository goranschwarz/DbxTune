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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.asetune.mgt;

import java.net.BindException;
import java.util.Base64;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.asetune.mgt.controllers.NoGuiConfigGetServlet;
import com.asetune.mgt.controllers.NoGuiConfigSetServlet;
import com.asetune.mgt.controllers.NoGuiRefreshServlet;
import com.asetune.mgt.controllers.NoGuiRestartServlet;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public class NoGuiManagementServer
{
	private static Logger _logger = Logger.getLogger(NoGuiManagementServer.class);

	public static final String  PROPKEY_NOGUI_MANAGEMENT_START = "DbxTune.nogui.management.start";
	public static final boolean DEFAULT_NOGUI_MANAGEMENT_START = true;

	public static final String  PROPKEY_NOGUI_MANAGEMENT_http_listener_address = "DbxTune.nogui.management.http.listener.address";
	public static final String  DEFAULT_NOGUI_MANAGEMENT_http_listener_address = "127.0.0.1";

	public static final String  PROPKEY_NOGUI_MANAGEMENT_http_listener_port    = "DbxTune.nogui.management.http.listener.port";
	public static final int     DEFAULT_NOGUI_MANAGEMENT_http_listener_port    = 8900;

	public static final String  PROPKEY_NOGUI_MANAGEMENT_http_auth_token       = "DbxTune.nogui.management.http.auth.token";
	public static final String  DEFAULT_NOGUI_MANAGEMENT_http_auth_token       = "";

	public static final String  PROPKEY_NOGUI_MANAGEMENT_http_onStartup_doDump = "DbxTune.nogui.management.http.onStartup.doDump";
	public static final boolean DEFAULT_NOGUI_MANAGEMENT_http_onStartup_doDump = false;

	/** Hold the "Jetty server" instance in a NO-GUI mode */
	private Server _server;

	/** Port number the server was started on. -1 if not started */
	private int _port = -1;

	/** Host name the server was started on. null if not started */
	private String _listenerHost = null;
	
	/** Auth Token. null if not started */
	private String _authorizationToken = null;
	
	//----------------------------------------------------------------
	// BEGIN: instance
	//----------------------------------------------------------------
	private static NoGuiManagementServer _instance = null;
	public static NoGuiManagementServer getInstance()
	{
		if (_instance == null)
		{
			throw new RuntimeException("DbmsObjectIdCache doesn't have an instance yet, please set with setInstance(instance).");
		}
		return _instance;
	}
	public static void setInstance(NoGuiManagementServer instance)
	{
		_instance = instance;
	}
	public static boolean hasInstance()
	{
		return _instance != null;
	}
	//----------------------------------------------------------------
	// END: instance
	//----------------------------------------------------------------

	public NoGuiManagementServer()
	{
	}
	
	/**
	 * Start the Server
	 */
	public void startServer()
	{
		// Start a HTTP Server so DbxTune Central can "talk" to the NOGUI instance
		boolean doStart = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_NOGUI_MANAGEMENT_START, DEFAULT_NOGUI_MANAGEMENT_START);
//		doStart = false;

		_port = -1;
		_listenerHost = null;
		_authorizationToken = null;

		// Exit if we should NOT start
		if ( ! doStart )
		{
			return;
		}

		int     tryNumOfPorts      = 1_000;
		String  listnerAddress     = Configuration.getCombinedConfiguration().getProperty       (PROPKEY_NOGUI_MANAGEMENT_http_listener_address, DEFAULT_NOGUI_MANAGEMENT_http_listener_address);
		int     startPort          = Configuration.getCombinedConfiguration().getIntProperty    (PROPKEY_NOGUI_MANAGEMENT_http_listener_port   , DEFAULT_NOGUI_MANAGEMENT_http_listener_port); // 0 = grab any available port
		String  authorizationToken = Configuration.getCombinedConfiguration().getProperty       (PROPKEY_NOGUI_MANAGEMENT_http_auth_token      , DEFAULT_NOGUI_MANAGEMENT_http_auth_token);
		boolean onStartDoDump      = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_NOGUI_MANAGEMENT_http_onStartup_doDump, DEFAULT_NOGUI_MANAGEMENT_http_onStartup_doDump);
		int     maxPort            = startPort + tryNumOfPorts;

		// Loop a couple of ports (if they are busy)
		for (int i = 0; i < tryNumOfPorts; i++)
		{
			int port = i + startPort;
			if (startPort == 0)
				port = 0;

			try
			{
				Server server = new Server();

				ServerConnector http = new ServerConnector(server);
		        http.setHost(listnerAddress);
		        http.setPort(port);
		        http.setIdleTimeout(30000);

		        // Set the connector
		        server.addConnector(http);

				// Add a Servlet Context Handler
				ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
//				context.setContextPath( "/api/management" ); // or should we use "/api/mgt"
				context.setContextPath( "/api/mgt" ); // or should we use "/api/management"
		        
				server.setHandler( context );

				// Add Servlet's 
//					context.addServlet(new DbxTuneNoGuiManagementHandler(), "/*");
				context.addServlet(new ServletHolder(new NoGuiConfigGetServlet()) , "/config/get"); // can we do regex like: "/config/(get|set)"
				context.addServlet(new ServletHolder(new NoGuiConfigSetServlet()) , "/config/set"); // can we do regex like: "/config/(get|set)"
				context.addServlet(new ServletHolder(new NoGuiRestartServlet())   , "/restart");
				context.addServlet(new ServletHolder(new NoGuiRefreshServlet())   , "/refresh");

				_logger.info("Trying to start NO-GUI Management Server at address '" + listnerAddress + "', port " + port + ".");
				server.start();

				// Generate a random Authorization Token
				if (StringUtil.isNullOrBlank(authorizationToken))
				{
					String authUser   = "admin";
				//	String authPasswd = UUID.randomUUID().toString();
					String authPasswd = RandomStringUtils.randomAlphanumeric(32);

					String encodeThis = authUser + ":" + authPasswd;
					String base64 = Base64.getEncoder().encodeToString(encodeThis.getBytes());
					
					authorizationToken = "Basic " + base64;

					// Possibly the below can be used...
					// https://www.fortytools.com/blog/servlet-filter-for-http-basic-auth
					
					// https://github.com/riversun/jetty-basic-auth-helper/tree/master
					// https://github.com/riversun/jetty-basic-auth-helper-examples
					//BasicAuth basicAuth = new BasicAuth.Builder().setRealm("MgtRealm")
					//		.addUserPath(authUser, authPasswd, "/config/set")
					//		.addUserPath(authUser, authPasswd, "/restart")
					//		.build();
				}
				_listenerHost = listnerAddress;
				_authorizationToken = authorizationToken;
				_port = ((ServerConnector)server.getConnectors()[0]).getLocalPort();
				_logger.info("Succeeded to start Local Web server at address '" + listnerAddress + "', port " + _port + ".");
				
				// Set the server instance so we can do "stop" later on
				_server = server;

				if (onStartDoDump)
					_logger.info("Management Web server 'Jetty' configuration. \n" + _server.dump());

				// Get out of the "start on port number" loop
				break;
			}
			catch(Exception ex)
			{
				if (ex.getCause() instanceof BindException)
				{
					_logger.warn("Failed to start NO-GUI Management Server at address '" + listnerAddress + "', port " + port + ". Lets try another port number.");
				}
				else
				{
					_logger.debug("Starting NO-GUI Management Server failed, but I will continue anyway...", ex);
					
					// Unknown error, get out of the "start on port number" loop
					break;
				}
			}
		} // end: loop "trying to start on free port"
		
		if (_port == -1)
		{
			_logger.warn("Failed to start NO-GUI Management Server (exhausted available ports, startPort=" + startPort + ", maxPort=" + maxPort + ", see messages above). No management via HTTP will be available.");
		}
	}

	/**
	 * Stop the Server
	 */
	public void stopServer()
	{
		if (_server == null)
			return;
		
		try
		{
			_logger.info("Stopping NO-GUI Management server...");
			_server.stop();
		}
		catch (Exception ex)
		{
			_logger.error("Problems Stopping NO-GUI Management server. Continuing anyway...", ex);
		}
	}

	/**
	 * Indicate if we are running or not
	 * @return
	 */
	public boolean isRunning()
	{
		if (_server == null)
			return false;
		
		return _server.isRunning();
	}

	public String getListenerHost()
	{
		return _listenerHost;
	}
	/**
	 * Get port number this server was started on
	 * @return
	 */
	public int getPort()
	{
		return _port;
	}

	/** 
	 * Get Extra Info as a JSON String: {"authorization":"Basic uuEncodedStringWithUserAndPassword"}<br>
	 * */
	public String getExInfo()
	{
		return "{\"authorization\":\"" + _authorizationToken + "\"}";
	}

	/** Get authorization token... <code>Basic uuEncodedStringWithUserAndPassword</code> */
	public String getAuthTokenString()
	{
		return _authorizationToken;
	}
}
