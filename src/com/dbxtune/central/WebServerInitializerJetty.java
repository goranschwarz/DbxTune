package com.dbxtune.central;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.RequestLogWriter;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.authentication.FormAuthenticator;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.FragmentConfiguration;
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.webapp.MetaInfConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;

import com.dbxtune.Version;
import com.dbxtune.alarm.writers.AlarmWriterAbstract;
import com.dbxtune.central.controllers.*;
import com.dbxtune.central.controllers.cc.mgt.*;
import com.dbxtune.central.controllers.cc.report.*;
import com.dbxtune.central.mcp.McpController;
import com.dbxtune.central.pcs.DbxCentralRealm;
import com.dbxtune.mgt.controllers.NoGuiRestartServlet;
import com.dbxtune.mgt.controllers.NoGuiShutdownServlet;
import com.dbxtune.utils.Configuration;

public class WebServerInitializerJetty
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	// Flip to false to switch from web.xml to programmatic servlet registration.
	// Both paths produce identical servlet mappings — web.xml remains on disk as fallback.
//	private static final boolean USE_WEB_XML = true;
//	private static final boolean USE_WEB_XML = false; // NOTE: '/WEB-INF/web.xml' has been renamed to '/WEB-INF/web.xml.NOT_USED' -- So we cant simply set this to TRUE anymore

	public static Server createServer()
	throws IOException
	{
		_logger.info("jetty.home    = '" + System.getProperty("jetty.home")     + "'.");
		_logger.info("jetty.VERSION = '" + org.eclipse.jetty.util.Jetty.VERSION + "'.");

		int port    = DbxTuneCentral.getWebHttpPort();
		int sslPort = DbxTuneCentral.getWebHttpsPort();

		Server server = new Server(port);
		
		// Add SSL if it can be done
		addSll(server, sslPort);

		// Where is the WEB DIR located
		String webDir = DbxTuneCentral.getAppWebDir();

		// Check if "webDir" contains any symbolic links
		Path webDirOriginPath   = Paths.get(webDir);
		Path webDirResolvedPath = Paths.get(webDir).toRealPath();
		if ( ! webDirOriginPath.equals(webDirResolvedPath) )
		{
			webDir = webDirResolvedPath.toString();
			_logger.info("Found that 'WebAppDir' contains symlinks, which was resolved to '" + webDir + "'. Origin value was '" + webDirOriginPath + "'.");
		}

		// Create Servlets etc...
		WebAppContext webapp = createWebApp(webDir);

		// Creating the LoginService for the realm
		DbxCentralRealm loginService = new DbxCentralRealm("DbxTuneCentralRealm");
		
		// Appending the loginService to the Server
		server.addBean(loginService);

		// Adding the handlers to the server
		server.setHandler(webapp);

		// Configuring Jetty Request Logs
		// If we only want to log ERRORS look at: https://stackoverflow.com/questions/68737248/how-to-override-request-logging-mechanism-in-jetty-11-0-6
		boolean createRequestLog = Configuration.getCombinedConfiguration().getBooleanProperty(DbxTuneCentral.PROPKEY_web_createRequestLog, DbxTuneCentral.DEFAULT_web_createRequestLog);
		if (createRequestLog)
		{
			String requestLogFormat = "%{client}a - %u %{yyyy-MM-dd HH:mm:ss.SSS XXX}t '%r' %s %O '%{Referer}i' '%{User-Agent}i' '%C'";
//			String requestLogFormat = CustomRequestLog.EXTENDED_NCSA_FORMAT;
			CustomRequestLog customRequestLog = new CustomRequestLog(DbxTuneCentral.getAppLogDir() + File.separatorChar + "DbxCentral.web.request.yyyy_mm_dd.log", requestLogFormat);
			server.setRequestLog(customRequestLog);
			
			org.eclipse.jetty.server.RequestLog.Writer tmpWriter = customRequestLog.getWriter();
			if (tmpWriter instanceof RequestLogWriter)
			{
				RequestLogWriter writer = (RequestLogWriter) tmpWriter;
				
				int retainDays = Configuration.getCombinedConfiguration().getIntProperty(DbxTuneCentral.PROPKEY_web_createRequestLog_retainDays, DbxTuneCentral.DEFAULT_web_createRequestLog_retainDays);
				writer.setRetainDays(retainDays);

				_logger.info("Web Access/Request log file: name='"      + writer.getFileName() + "'.");
				_logger.info("Web Access/Request log file: retainDays=" + writer.getRetainDays());
				_logger.info("Web Access/Request log file: can be disabled using: " + DbxTuneCentral.PROPKEY_web_createRequestLog + " = false");
			}
		}

		// In Jetty, setStopAtShutdown(true) registers a JVM shutdown hook. 
		// When the application closes or the JVM terminates, this method ensures the Jetty server stops cleanly, 
		// freeing up ports and allowing in-flight requests to finish safely before the process completely dies
		server.setStopAtShutdown(true);

		return server;
	}

	public static WebAppContext createWebApp(String webDir)
	{
		WebAppContext webapp = new WebAppContext();
		webapp.setResourceBase(webDir);
		webapp.setContextPath("/");
		webapp.setWelcomeFiles(new String[]{"index.html"});
		webapp.getInitParams().put("org.eclipse.jetty.servlet.Default.useFileMappedBuffer", "false");
		webapp.getInitParams().put("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
		webapp.getServletContext().getContextHandler().setMaxFormContentSize(-1);
		webapp.setParentLoaderPriority(true);
		webapp.addFilter(MandatoryLoginFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

//		if (USE_WEB_XML)
//		{
//			webapp.setDescriptor(webDir + "/WEB-INF/web.xml");
//		}
//		else
//		{
			// WebAppContext auto-scans and loads WEB-INF/web.xml via WebXmlConfiguration.
			// Exclude it so programmatic registrations below are the only source of mappings.
			// OR: Simply remove the file: /WEB-INF/web.xml
			webapp.setConfigurations(new org.eclipse.jetty.webapp.Configuration[] {
				new WebInfConfiguration(),
				new MetaInfConfiguration(),
				new FragmentConfiguration(),
				new JettyWebXmlConfiguration()
				// new WebXmlConfiguration() // intentionally omitted -- This will read: /WEB-INF/web.xml
			});
			
			// Add Servlets and other functionality
			registerServlets(webapp);
			configureFormAuth(webapp);

			// Set error handler. (mapping away some "sensitive" output)
			DbxErrorHandler errorHandler = new DbxErrorHandler();
	        errorHandler.setShowServlet   (false);
	        errorHandler.setShowStacks    (false);
	        errorHandler.setShowPoweredBy (false);
	        errorHandler.setShowDbxVersion(true);

	        if (_logger.isDebugEnabled())
	        {
		        errorHandler.setShowServlet  (true);
		        errorHandler.setShowStacks   (true);
		        errorHandler.setShowPoweredBy(true);
	        }

			webapp.setErrorHandler(new DbxErrorHandler());
//		}

		return webapp;
	}

	private static void configureFormAuth(WebAppContext webapp)
	{
		ConstraintSecurityHandler security = (ConstraintSecurityHandler) webapp.getSecurityHandler();

		// Equivalent to <login-config> in web.xml
		security.setRealmName("DbxTuneCentralRealm");
		security.setAuthenticator(new FormAuthenticator("/index.html?login=open", "/index.html?login=failed", false));

		// Equivalent to <security-constraint> for /admin/*
		Constraint adminConstraint = new Constraint();
		adminConstraint.setName("admin");
		adminConstraint.setRoles(new String[]{"admin"});
		adminConstraint.setAuthenticate(true);

		ConstraintMapping adminMapping = new ConstraintMapping();
		adminMapping.setConstraint(adminConstraint);
		adminMapping.setPathSpec("/admin/*");

		security.addConstraintMapping(adminMapping);
	}

	private static void registerServlets(WebAppContext webapp)
	{
		// Static file serving -- replaces the DefaultServlet registered by Jetty's webdefault.xml
		// Normal files...
		webapp.addServlet(DefaultServlet.class, "/");

		// Login (and User admin)
		webapp.addServlet(LogoutServlet.class,                        "/logout");
		webapp.addServlet(LoginCheckServlet.class,                    "/login-check");
		webapp.addServlet(LoginProvidersServlet.class,                "/api/login/providers");             // What Login Providers are enabled
		webapp.addServlet(LoginConfigServlet.class,                   "/api/login/config");
		webapp.addServlet(UserRegistrationServlet.class,              "/api/user/register");               // User Registration
		webapp.addServlet(UserRecoverPasswordServlet.class,           "/api/user/forgot-password");        // Generate a new password and send it via mail
		webapp.addServlet(UserSettingsServlet.class,                  "/api/user/settings");               // So a user can change some settings
		webapp.addServlet(RequestAccessServlet.class,                 "/api/user/request-access");         // User Requests Access (send a mail to all admins, which needs to authorize the user)
		
		// NavBar: Servers
		webapp.addServlet(OverviewServlet.class,                      "/overview");

		// NavBar: Admin
		webapp.addServlet(AdminServlet.class,                         "/admin");
		webapp.addServlet(AlarmLogServlet.class,                      "/alarmLog");
		webapp.addServlet(DbxTuneLogServlet.class,                    "/log");
		webapp.addServlet(DbxTuneLogTailWebSocketServlet.class,       "/logtail");
		webapp.addServlet(DbxTuneConfServlet.class,                   "/conf");
		webapp.addServlet(DbxTuneReportsServlet.class,                "/report");
		webapp.addServlet(DownloadRecordingServlet.class,             "/download-recording");               // Download a specific H2 database file (needs login)
		webapp.addServlet(ShutdownServlet.class,                      "/admin/shutdown");
		webapp.addServlet(UsersAdminServlet.class,                    "/admin/users");                      // Goto the "admin" -- "users" section (needs login)
		webapp.addServlet(H2WriterStatServlet.class,                  "/h2ws");

		// NavBar: Desktop App
		webapp.addServlet(DownloadServlet.class,                      "/download-local-dbxtune");           // Download the "public" or "local" DbxTune.zip this server is Using (so we can install it at any client)

		// Others
		webapp.addServlet(DummyServlet.class,                         "/dummy");                            // "Hello" -- can be used to check connectivity...
		webapp.addServlet(ShowplanPostgresServlet.class,              "/showplan/postgres");                // Local PEV2 Implementation 
		webapp.addServlet(ShowplanSqlServerServlet.class,             "/showplan/sqlserver");               // Local html-query-plan Implementation
		webapp.addServlet(LoadPageProgress.class,                     "/lpp/*");                            // Load Page Progress
		webapp.addServlet(LetsEncryptAcmeChallengeServlet.class,      "/.well-known/acme-challenge/*");

		// API
		webapp.addServlet(AlarmActiveController.class,                "/api/alarm/active");                 // Get all active alarms
		webapp.addServlet(AlarmMuteServlet.class,                     "/api/alarm/mute");                   // Get muted alarms, or SET what alarmId's that are muted
		webapp.addServlet(AlarmFormatterController.class,             "/api/alarm/formatter");              // Format a JSON entry into HTML based on a template
		webapp.addServlet(AlarmHistoryController.class,               "/api/alarm/history");
		webapp.addServlet(LastSampleForCmController.class,            "/api/last-sample");
		webapp.addServlet(HistorySampleForCmController.class,         "/api/history-sample");
		webapp.addServlet(HistoryActiveSamplesForCmController.class,  "/api/history-active-samples");
		webapp.addServlet(DailySummartReportServlet.class,            "/api/dsr");                          // Create a Daily Summary Report 
		webapp.addServlet(DsrSkipEntriesController.class,             "/api/dsr/skip");                     // Add "skip sql-id" entries to Daily Summary Report 
		webapp.addServlet(CentralPcsReceiverController.class,         "/api/pcs/receiver");                 // endpoint used by Collectors to SEND data to DbxCentral
		webapp.addServlet(PcsQueueInfoController.class,               "/api/pcs/queueInfo");                // if the PCS "bussy" at DbxCental -- {"queueSize":#,"lastPersistedSampleTime":"YYYY-MM-DD hh:mm:ss.ms"}
		webapp.addServlet(ServerLayoutController.class,               "/api/server-layout");                // How the Server "layout" should be presented at the Landing Page
		webapp.addServlet(SessionsController.class,                   "/api/sessions");
		webapp.addServlet(GraphPropertiesController.class,            "/api/graphs");
		webapp.addServlet(GraphProfilesController.class,              "/api/graph/profiles");
		webapp.addServlet(GraphDescriptionController.class,           "/api/graph/description");
		webapp.addServlet(GraphDataController.class,                  "/api/graph/data");
		webapp.addServlet(HealthCheckController.class,                "/api/healthcheck");
		webapp.addServlet(ChartBroadcastWebSocketServlet.class,       "/api/chart/broadcast-ws");           // Notifies WebBrowser that we have received new data for a Collector
		webapp.addServlet(UserDefinedContentServlet.class,            "/api/udc");                          // User defined Content in -- NavBar: Servers
		webapp.addServlet(UserDefinedActionServlet.class,             "/api/udaction");                     // User defined Actions in -- NavBar: Servers
		webapp.addServlet(SpaceForecastServlet.class,                 "/api/space/forecast");               // Get a Space Forecast (for example: used by Daily Summary Reports)
		webapp.addServlet(NoGuiShutdownServlet.class,                 "/api/mgt/shutdown");                 // Shutdown DbxCentral or a Collector -- Needs: TokenAuthentication
		webapp.addServlet(NoGuiRestartServlet.class,                  "/api/mgt/restart");                  // Restart  DbxCentral or a Collector -- Needs: TokenAuthentication

		// API -- Collector Collector (various API's that simply talks to the Collector Instance)
		webapp.addServlet(CollectorRefreshController.class,           "/api/collector-refresh");            // Send request to refresh data at a Collector
		webapp.addServlet(CollectorRestartController.class,           "/api/collector-restart");            // Send request to restart a Collector
		webapp.addServlet(ProxyConfigGetServlet.class,                "/api/cc/mgt/config/get");            // Get how a Collector is Configured
		webapp.addServlet(ProxyConfigSetServlet.class,                "/api/cc/mgt/config/set");            // Set Configuration changes at the Collector
		webapp.addServlet(ProxyReportsServlet.class,                  "/api/cc/reports");                   // This is for example: sqlserver-job-scheduler-report, sqlserver-job-scheduler-timeline
		webapp.addServlet(ProxyCmListServlet.class,                   "/api/cc/mgt/cm/list");               // Ask a collector What CM "Properties" it has for a specific time
		webapp.addServlet(ProxyCmDataServlet.class,                   "/api/cc/mgt/cm/data");               // Get CM data for a specific CM at a time
		webapp.addServlet(ProxyCmNavSampleServlet.class,              "/api/cc/mgt/cm/navSample");          // get next/prev sample time for a CM
		webapp.addServlet(CollectorRegisterServlet.class,             "/api/cc/mgt/collector/register");    // Called by any collector to register information that normally is kept in: ${HOME}/.dbxtune/dbxc/info/
		webapp.addServlet(CmIconServlet.class,                        "/api/cc/mgt/cm/icon");               // Get Icon File(s) from the Collector instance
		webapp.addServlet(ProxyDbmsConfigServlet.class,               "/api/cc/mgt/dbms-config");           // Get DBMS Config from the Collector instance
		webapp.addServlet(ProxyDdlStorageServlet.class,               "/api/cc/mgt/ddl-storage");           // Get DDL Information from the Collector instance
		webapp.addServlet(ProxyRecordingDatabasesServlet.class,       "/api/cc/mgt/recording-databases");   // Get what PCS Databases that the Collector holds
		webapp.addServlet(ProxyQueryStoreServlet.class,               "/api/cc/mgt/query-store");           // SQL Server: Extract/Get Query Store  Information from the Collector instance
		webapp.addServlet(ProxyDeadlockServlet.class,                 "/api/cc/mgt/deadlock");              // SQL Server: Extract/Get Deadlock     Information from the Collector instance
		webapp.addServlet(ProxyJobSchedulerServlet.class,             "/api/cc/mgt/job-scheduler");         // SQL Server: Extract/Get JobScheduler Information from the Collector instance

		// Async-supported servlets -- NOT SURE THI IS USED ANYMORE - We started to use WebSockets MANY years ago
//		ServletHolder sseBroadcast = new ServletHolder("ChartBroadcastServlet", ChartBroadcastServlet.class);
//		sseBroadcast.setAsyncSupported(true);
//		webapp.addServlet(sseBroadcast, "/api/chart/broadcast-sse");

		// MCP
		ServletHolder mcp = new ServletHolder("McpController", McpController.class);
		mcp.setAsyncSupported(true);
		webapp.addServlet(mcp, "/mcp");

		// OAUTH (AD-Login, Google Login, etc)
		webapp.addServlet(OAuthStartServlet.class,                    "/oauth/start");
		webapp.addServlet(OAuthCallbackServlet.class,                 "/oauth/callback");
		webapp.addServlet(OAuthProtectedResourceServlet.class,        "/.well-known/oauth-protected-resource");

		// OAuthStubServlet maps to multiple URL patterns
		ServletHolder oauthStub = new ServletHolder("OAuthStubServlet", OAuthStubServlet.class);
		webapp.addServlet(oauthStub, "/.well-known/oauth-authorization-server");
		webapp.addServlet(oauthStub, "/register");
		webapp.addServlet(oauthStub, "/oauth/authorize");
		webapp.addServlet(oauthStub, "/oauth/token");
	}




	
	
	
	
	
	
	private static void addSll(Server server, int sslPort)
	{
		HttpConfiguration httpConfig = new HttpConfiguration();
		httpConfig.setSendServerVersion(false); // disables 'Server' header
		httpConfig.setSendXPoweredBy(false);    // disables 'X-Powered-By' header
		
		// Configure SSL with PEM files
		String sslFilePath = DbxTuneCentral.getAppConfDir() + "/ssl"; // must contain: 'cert.pem', 'key.pem', (optional) 'chain.pem'
		File   sslFileFile = new File(sslFilePath);
		if (sslFileFile.exists())
		{
			File sslFileCertFile  = new File(sslFilePath + "/cert.pem");
			File sslFileKeyFile   = new File(sslFilePath + "/key.pem");
			File sslFileChainFile = new File(sslFilePath + "/chain.pem");

			boolean doSslConfig = true;
			if (sslFileCertFile.exists())
			{
				_logger.info("Found SSL 'Certificate' file 'cert.pem' at '" + sslFileCertFile + "'.");
			}
			else
			{
				_logger.info("SSL will NOT be configured. Missing 'Certificate' file '" + sslFileCertFile + "'.");
				doSslConfig = false;
			}

			if (sslFileKeyFile.exists())
			{
				_logger.info("Found SSL 'Private Key' file 'key.pem' at '" + sslFileKeyFile + "'.");
			}
			else
			{
				_logger.info("SSL will NOT be configured. Missing 'Private Key' file '" + sslFileKeyFile + "'.");
				doSslConfig = false;
			}

			if (sslFileChainFile.exists())
			{
				_logger.info("Found optional SSL 'CA Chain' file '" + sslFileChainFile + "', which also will be used for CA Chain.");
			}

			if (doSslConfig)
			{
				_logger.info("SSL will be configured at port " + sslPort + ", using files ('cert.pem', 'key.pem', optional:'chain.pem') in directory '" + sslFileFile + "'.");

				File keystoreFile        = new File(sslFilePath + "/keystore.p12");
				File keystoreRefreshFile = new File(sslFilePath + "/keystore.p12.refresh");
				String keystorePassword  = "SmslTVkv21GS5NJdQXxP"; // Consider making this configurable

				try
				{
					boolean createNewKeyStoreFile = false;

					// If we do NOT have a KeyStore file
					if ( ! keystoreFile.exists() )
					{
						createNewKeyStoreFile = true;
					}
					
					// Or if the PEM file has changed...
					// FIXME: We need to save the last date of 'cert.pem' and check if we got any new file (save it in a properties file)
					//  DONE: I did a simpler solution, if the file 'keystore.p12.refresh' exists... Then do refresh... Not as good as saving the time... But it's at least something...
					if (keystoreRefreshFile.exists())
					{
						_logger.info("Found signal file '" + keystoreRefreshFile + "' to recreate SSL Certificate File '" + sslFileCertFile + ". So lets convert the new PEM files to PKCS12 keystore...");

						// Delete the 'signal/refresh' and 'keystoreFile' file: 
						keystoreRefreshFile.delete();
						keystoreFile.delete();

//						_logger.info("The SSL Certificate File '" + sslFileCertFile + "' has changed (lastKnownDate='', newDate=''). So lets convert the new PEM files to PKCS12 keystore...");
						createNewKeyStoreFile = true;
					}

					// Convert PEM to PKCS12 if not already done
					if ( createNewKeyStoreFile )
					{
						_logger.info("Converting PEM files to PKCS12 keystore...");
						convertPemToPkcs12(sslFileCertFile, sslFileKeyFile, sslFileChainFile, keystoreFile, keystorePassword);
					}

					SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
					sslContextFactory.setKeyStorePath(keystoreFile.getAbsolutePath());
					sslContextFactory.setKeyStorePassword(keystorePassword);
					sslContextFactory.setKeyStoreType("PKCS12");
					sslContextFactory.setKeyManagerPassword(keystorePassword);

					httpConfig.addCustomizer(new SecureRequestCustomizer());

					ServerConnector sslConnector = new ServerConnector(
							server,
							new SslConnectionFactory(sslContextFactory, "http/1.1"),
							new HttpConnectionFactory(httpConfig)
					);
					sslConnector.setPort(sslPort);
					server.addConnector(sslConnector);
					
				}
				catch (Exception ex)
				{
					_logger.error("Failed to configure SSL", ex);
				}

				boolean useOldCode = false;
				if (useOldCode)
				{
					SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
			        sslContextFactory.setKeyStoreType("PEM");
			        sslContextFactory.setKeyStorePath(sslFilePath);  // must contain cert.pem, key.pem, (optional) chain.pem
//			        sslContextFactory.setCertChainPath(sslFileCertFile);      // your public cert or full chain "cert.pem"
//			        sslContextFactory.setKeyPath(sslFileKeyFile);             // your private key "key.pem"
//			        if (sslFileChainFile.exists())
//			        {
//				        sslContextFactory.setTrustStoreType("PEM");               // optional for client auth
//				        sslContextFactory.setTrustStorePath(sslFileChainFile);    // CA chain, optional "chain.pem"
//			        }
					
					httpConfig.addCustomizer(new SecureRequestCustomizer());

					ServerConnector sslConnector = new ServerConnector(
							server,
							new SslConnectionFactory(sslContextFactory, "http/1.1"),
							new HttpConnectionFactory(httpConfig)
		            );
					sslConnector.setPort(sslPort);
					server.addConnector(sslConnector);
				}
			}
		}
		else
		{
			_logger.info("SSL will NOT be configured. The directory '" + sslFileFile + "' does NOT EXISTS. Which should hold 'Certificate' file 'cert.pem' and 'Private Key' file 'key.pem' (possibly 'CA Chain' file 'chain.pem').");
		}
	}

	private static void convertPemToPkcs12(File certFile, File keyFile, File chainFile, File outputFile, String password) 
	throws Exception
	{
		ProcessBuilder pb = new ProcessBuilder(
				"openssl", "pkcs12", "-export",
				"-in", certFile.getAbsolutePath(),
				"-inkey", keyFile.getAbsolutePath(),
				"-out", outputFile.getAbsolutePath(),
				"-name", "jetty",
				"-passout", "pass:" + password
		);

		if (chainFile.exists())
		{
			pb.command().add("-certfile");
			pb.command().add(chainFile.getAbsolutePath());
		}

		Process process = pb.start();
		int exitCode = process.waitFor();

		if (exitCode != 0)
		{
			throw new RuntimeException("Failed to convert PEM to PKCS12. Exit code: " + exitCode);
		}

		_logger.info("Successfully converted PEM files to PKCS12 keystore: " + outputFile);
	}
	

	// ---------------------------------------------------------------------------
	// Custom error handler — hides "Powered by Jetty" from error pages.
	// writeErrorPageBody is copied verbatim from Jetty 9.4 source so it can be
	// adjusted (e.g. restore the footer when debug mode is enabled).
	// ---------------------------------------------------------------------------
	static class DbxErrorHandler extends ErrorHandler
	{
		private boolean _showServlet    = false;
		private boolean _showStacks     = false;
		private boolean _showPoweredBy  = false;
		private boolean _showDbxVersion = true;
		
		
		@Override public boolean isShowServlet()    { return _showServlet; }
		@Override public boolean isShowStacks()     { return _showStacks; }
		          public boolean isShowDbxVersion() { return _showDbxVersion; }
		          public boolean isShowPoweredBy()  { return _showPoweredBy; }

		
		@Override public void setShowServlet   (boolean showServlet)    { _showServlet    = showServlet; }
		@Override public void setShowStacks    (boolean showStacks)     { _showStacks     = showStacks; }
		          public void setShowDbxVersion(boolean showDbxVersion) { _showDbxVersion = showDbxVersion; }
		          public void setShowPoweredBy (boolean showPoweredBy)  { _showPoweredBy  = showPoweredBy; }
		
		
		@Override
		protected void writeErrorPageBody(HttpServletRequest request, Writer writer, int code, String message, boolean showStacks)
		throws IOException
		{
			String uri = request.getRequestURI();

			writeErrorPageMessage(request, writer, code, message, uri);

			if (isShowStacks())
			{
				writeErrorPageStacks(request, writer);
			}

			if (isShowPoweredBy())
			{
				// Original Jetty footer
				writer.write("<hr/><a href='https://jetty.org/'>DEBUG: Powered by Jetty:// " + org.eclipse.jetty.util.Jetty.VERSION + "</a><hr/>\n");
			}

			if (isShowDbxVersion())
			{
				writer.write("<hr/><a href='https://dbxtune.com/'>" + Version.getAppName() + "</a> -- Version: " + Version.getVersionStr() + ", Build: " + Version.getBuildStr() + " <hr/>\n");
			}

			// Back to DbxCentral (base url)
			String dbxCentralBaseUrl = AlarmWriterAbstract.static_getDbxCentralUrl();
			writer.write("<br>\n");
			writer.write("Back to: <a href='" + dbxCentralBaseUrl + "'>" + dbxCentralBaseUrl + "</a><br>\n");
			writer.write("<br>\n");
		}
	}
}
