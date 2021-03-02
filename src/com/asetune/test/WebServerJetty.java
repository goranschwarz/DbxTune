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
package com.asetune.test;

import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.Server;
//import org.eclipse.jetty.websocket.javax.server.config.JavaxWebSocketServletContainerInitializer;
import org.eclipse.jetty.webapp.WebAppContext;

import com.asetune.central.pcs.DbxCentralRealm;
import com.asetune.utils.Configuration;

public class WebServerJetty
{
	public static void main(String[] args) throws Exception
	{
		Properties log4jProps = new Properties();
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
//		log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		try
		{
			new WebServerJetty().startServer();
		}
		catch (Throwable t)
		{
			t.printStackTrace();
		}
	}

	
	public void startServer() throws Exception
	{
		Server server = new Server(8080);

//		URL webRootLocation = this.getClass().getResource("/index.html");
//		if ( webRootLocation == null )
//		{
//			throw new IllegalStateException("Unable to determine webroot URL location");
//		}

//		URI webRootUri = URI.create("file://C:/projects/DbxTune/resources/WebContent");
////		URI webRootUri = URI.create(webRootLocation.toURI().toASCIIString().replaceFirst("/index.html$", "/"));
////		System.err.printf("Web Root URI: %s%n", webRootUri);
//
//		ServletContextHandler contextHandler = new ServletContextHandler();
//		contextHandler.setContextPath("/");
//		contextHandler.setBaseResource(Resource.newResource(webRootUri));
//		contextHandler.setWelcomeFiles(new String[] { "index.html" });
//
//		contextHandler.getMimeTypes().addMimeMapping("txt", "text/plain;charset=utf-8");
//
//		server.setHandler(contextHandler);
//
//		// Add WebSocket endpoints
////		JavaxWebSocketServletContainerInitializer.configure(contextHandler, (context, wsContainer) -> wsContainer.addEndpoint(TimeSocket.class));
//
//		// Add Servlet endpoints
////		contextHandler.addServlet(TimeServlet.class, "/time/");
////		contextHandler.addServlet(DefaultServlet.class, "/");

		DbxCentralRealm loginService = new DbxCentralRealm("DbxTuneCentralRealm");

//		String userFile = Configuration.getCombinedConfiguration().getProperty("realm.users.file", getAppWebDir()+"/dbxtune_central_users.txt");
//		HashLoginService loginService = new HashLoginService("DbxTuneCentralRealm", userFile);

		server.addBean(loginService);
		
		String webDir = getAppWebDir();
		WebAppContext webapp1 = new WebAppContext();
		webapp1.setDescriptor(webDir+"/WEB-INF/web.xml");
		webapp1.setResourceBase(webDir);
//		webapp1.setContextPath("/");
//		webapp1.getInitParams().put("org.eclipse.jetty.servlet.Default.useFileMappedBuffer", "false");
		webapp1.setParentLoaderPriority(true);
		
		server.setHandler(webapp1);
		
		server.start();
		server.join();

	}	

	/** Where is DbxTune Central Web "content" directory located */
	public static String getAppWebDir()
	{
		return "C:/projects/DbxTune/resources/WebContent";
//		return "file:\\\\C:\\projects\\DbxTune\\resources\\WebContent";
	}
}
