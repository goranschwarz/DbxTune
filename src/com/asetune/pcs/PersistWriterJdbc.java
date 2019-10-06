/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.pcs;

import java.io.File;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.h2.server.TcpServer;
import org.h2.tools.Server;

import com.asetune.CounterController;
import com.asetune.DbxTune;
import com.asetune.ICounterController;
import com.asetune.Version;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.writers.AlarmWriterToPcsJdbc;
import com.asetune.alarm.writers.AlarmWriterToPcsJdbc.AlarmEventWrapper;
import com.asetune.central.pcs.H2WriterStat;
import com.asetune.cm.CountersModel;
import com.asetune.cm.CountersModelAppend;
import com.asetune.config.dbms.DbmsConfigManager;
import com.asetune.config.dbms.DbmsConfigTextManager;
import com.asetune.config.dbms.IDbmsConfig;
import com.asetune.config.dbms.IDbmsConfigText;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionary.MonTableColumnsEntry;
import com.asetune.config.dict.MonTablesDictionary.MonTableEntry;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.gui.MainFrame;
import com.asetune.pcs.report.DailySummaryReportFactory;
import com.asetune.pcs.report.IDailySummaryReport;
import com.asetune.pcs.sqlcapture.ISqlCaptureBroker;
import com.asetune.pcs.sqlcapture.SqlCaptureDetails;
import com.asetune.sql.PreparedStatementCache;
import com.asetune.sql.conn.ConnectionProp;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.AseUrlHelper;
import com.asetune.utils.Configuration;
import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.DbUtils;
import com.asetune.utils.H2UrlHelper;
import com.asetune.utils.NetUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;
import com.asetune.utils.TimeUtils;
import com.asetune.utils.Ver;


public class PersistWriterJdbc
    extends PersistWriterBase
{
    /** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(PersistWriterJdbc.class);

	/*---------------------------------------------------
	** DEFINITIONS
	**---------------------------------------------------
	*/
	public static final String PROPKEY_BASE                      = "PersistWriterJdbc.";
	public static final String PROPKEY_PART_jdbcDriver           = "jdbcDriver";
	public static final String PROPKEY_PART_jdbcUrl              = "jdbcUrl";
	public static final String PROPKEY_PART_jdbcUsername         = "jdbcUser";
	public static final String PROPKEY_PART_jdbcPassword         = "jdbcPasswd";
	public static final String PROPKEY_PART_startH2NetworkServer = "startH2NetworkServer";

	public static final String PROPKEY_jdbcDriver                = "PersistWriterJdbc.jdbcDriver";
	public static final String PROPKEY_jdbcUrl                   = "PersistWriterJdbc.jdbcUrl";
	public static final String PROPKEY_jdbcUsername              = "PersistWriterJdbc.jdbcUser";
	public static final String PROPKEY_jdbcPassword              = "PersistWriterJdbc.jdbcPasswd";
	public static final String PROPKEY_startH2NetworkServer      = "PersistWriterJdbc.startH2NetworkServer";

	public static final String PROPKEY_cachePreparedStatements   = "PersistWriterJdbc.cachePreparedStatements";

	public static final String PROPKEY_jdbcKeepConnOpen          = "PersistWriterJdbc.jdbcKeepConnOpen";
	public static final String PROPKEY_h2NewDbOnDateChange       = "PersistWriterJdbc.h2NewDbOnDateChange";
	public static final String PROPKEY_h2DateParseFormat         = "PersistWriterJdbc.h2DateParseFormat";

	public static final String PROPKEY_h2_queueSizeWarning_createNewDbThreshold = "PersistWriterJdbc.h2.queueSizeWarning.createNewDbThreshold";
	public static final int    DEFAULT_h2_queueSizeWarning_createNewDbThreshold = 40;

	public static final String  PROPKEY_ddlInfoStorage_usePrivateConnection     = "PersistWriterJdbc.ddlInfoStorage.usePrivateConnection";
	public static final boolean DEFAULT_ddlInfoStorage_usePrivateConnection     = true;

	public static final String  PROPKEY_sqlCaptureStorage_usePrivateConnection  = "PersistWriterJdbc.sqlCaptureStorage.usePrivateConnection";
	public static final boolean DEFAULT_sqlCaptureStorage_usePrivateConnection  = true;


	public static final String  PROPKEY_h2_shutdown_dueToDatabaseRollover_type       = "PersistWriterJdbc.h2.shutdown.dueToDatabaseRollover.type";
	public static final String  DEFAULT_h2_shutdown_dueToDatabaseRollover_type       = H2ShutdownType.DEFRAG.toString();

	public static final String  PROPKEY_h2_shutdown_dueToDatabaseRollover_inBgThread = "PersistWriterJdbc.h2.shutdown.dueToDatabaseRollover.inBgThread";
	public static final boolean DEFAULT_h2_shutdown_dueToDatabaseRollover_inBgThread = true;
	
	public static final String  PROPKEY_h2_allowSpilloverDb              = "PersistWriterJdbc.h2.allow.spilloverDb";
	public static final boolean DEFAULT_h2_allowSpilloverDb              = false;

	public static final String  PROPKEY_isSevereProblem_simulateDiskFull = "DbxTune.pcs.isSevereProblem.simulateDiskFull.enabled";
	public static final boolean DEFAULT_isSevereProblem_simulateDiskFull = false;
	
	/*---------------------------------------------------
	** class members
	**---------------------------------------------------
	*/

	// Persistent Counter CONNection
	protected DbxConnection _mainConn = null;
	private boolean _keepConnOpen = true;

	protected DbxConnection _ddlStorageConn = null;
	protected DbxConnection _sqlCaptureStorageConn = null;

	protected String _jdbcDriver = "org.h2.Driver";
	protected String _jdbcUrl    = "jdbc:h2:pcdb_yyy";
	protected String _jdbcUser   = "sa";
	protected String _jdbcPasswd = "";

	private   boolean _connStatus_inProgress = false;
	
	private   String _lastUsedUrl = null;
	private   boolean _startH2NetworkServer = false;

	protected String _name       = "PersistWriterJdbc";

	private boolean _cachePreparedStatements = false;
	protected boolean _jdbcDriverInfoHasBeenWritten = false;

	private Server _h2TcpServer = null;
	private Server _h2WebServer = null;
	private Server _h2PgServer  = null;
	private File   _h2ServiceInfoFile = null;

	private boolean _h2NewDbOnDateChange = false;
	private String  _h2LastDateChange    = null;
	private String  _h2DbDateParseFormat = "yyyy-MM-dd";

	private boolean _h2AllowSpilloverDb               = DEFAULT_h2_allowSpilloverDb; // set to FALSE... later on we can REMOVE the SPILL functionality
	private boolean _h2CreateNewSpilloverDbOnNextSave = false;
	private long    _h2NewSpilloverDbCreateTime       = 0;

	//private boolean _h2NewDbOnDateChange = true;
	//private String  _h2DbDateParseFormat = "yyyy-MM-dd_HH.mm";

	private Configuration _config    = null;
	private String        _configStr = null;

	/** just a flag that we are currently persisting, stopService() has to wait for a while */
	private boolean _inSaveSample = false;
	
	/** If the stopService() timed out, or wiatTime <= 0 */
	private boolean _shutdownWithNoWait = false;
	
	/** used is serviceStart/Stop to check if we already has done stop */
	private String _servicesStoppedByThread = null;

	/*---------------------------------------------------
	** Constructors
	**---------------------------------------------------
	*/
	public PersistWriterJdbc()
	{
	}


	/*---------------------------------------------------
	** Methods
	**---------------------------------------------------
	*/
	/**
	 * Checks SQL Exceptions for specific errors when storing counters
	 * @return true if a severe problem
	 */
	protected boolean isSevereProblem(DbxConnection conn, SQLException e)
	{
		SQLException originException = e;

		if (e == null)
			return false;

		boolean retCode    = false;
		boolean doShutdown = false;
		boolean dbIsFull   = false;

		//---------------------------------------------------------------
		// For TESTING: Simulate a: DiskFull-->Shutdown...
		//---------------------------------------------------------------
		boolean checkForDummyShutdown = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_isSevereProblem_simulateDiskFull, DEFAULT_isSevereProblem_simulateDiskFull);
		if (checkForDummyShutdown)
		{
			String tmpDir = System.getProperty("java.io.tmpdir", "/tmp/");
			if ( ! (tmpDir.endsWith("/") || tmpDir.endsWith("\\")) )
				tmpDir += File.separatorChar;
			
			String tmpFilename = tmpDir + "DbxTune.pcs.isSevereProblem.simulateDiskFull.deleteme";
			File probeFile1 = new File(tmpFilename);

			_logger.info("SIMULATE-DISK-FULL: checking for probe file '"+probeFile1+"'. exists: " + probeFile1.exists() );
			if (probeFile1.exists())
			{
				_logger.info("SIMULATE-DISK-FULL: found-file('"+probeFile1+"')... setting: doShutdown=true, dbIsFull=true, retCode=true");

				doShutdown = true;
				dbIsFull   = true;
				retCode    = true;

				_logger.info("SIMULATE-DISK-FULL: removing file('"+probeFile1+"').");
				try { probeFile1.delete(); }
				catch(Exception ex) { _logger.error("SIMULATE-DISK-FULL: Problems removing file '"+probeFile1+"'. Caught: "+ex);}
			}
		}

		// Loop all nested SQLExceptions
		int exLevel = 0;
		while (e != null)
		{
			exLevel++;
			int    error    = e.getErrorCode();
			String msgStr   = e.getMessage();
			String sqlState = e.getSQLState();
	
			_logger.debug("isSevereProblem(): execptionLevel='"+exLevel+"', sqlState='"+sqlState+"', error='"+error+"', msgStr='"+msgStr+"'.");

			// H2 Messages
			if ( DbUtils.DB_PROD_NAME_H2.equals(getDatabaseProductName()) )
			{				
				// database is full: 
				//   NO_DISK_SPACE_AVAILABLE = 90100
				//   IO_EXCEPTION_2 = 90031
				if (error == 90100 || error == 90031)
				{
					// Mark it for shutdown
					doShutdown = true;
					dbIsFull   = true;
					retCode    = true;
				}

				// The MVStore doesn't give the same error on "disk full"
				// Lets examine the Exception chain, and look for "There is not enough space on the disk"
				String rootCauseMsg = ExceptionUtils.getRootCauseMessage(e);
				if (    rootCauseMsg.indexOf("There is not enough space") >= 0
					 || rootCauseMsg.indexOf("No space left on device")   >= 0
				   )
				{
					// Mark it for shutdown
					doShutdown = true;
					dbIsFull   = true;
					retCode    = true;
				}

				// Check if it looks like a CORRUPT H2 database
				Throwable rootCauseEx = ExceptionUtils.getRootCause(e);
				if (rootCauseEx != null && rootCauseEx.getClass().getSimpleName().equals("IllegalStateException"))
				{
					_logger.error("ATTENTION: ErrorCode="+e.getErrorCode()+", SQLState="+e.getSQLState()+", ex.toString="+e.toString()+", rootCauseEx="+rootCauseEx);
					_logger.error("ATTENTION: The H2 database looks CORRUPT, the rootCauseEx is 'IllegalStateException', which might indicate a corrupt database. rootCauseEx="+rootCauseEx, rootCauseEx);
				}
				
				// The database has been closed == 90098
				// MVStore: This map is closed == 90028
				// IllegalExceptionState: Transaction is closed == 50000
				if (error == 90098 || error == 90028 || error == 50000)
				{
					// What should we do here... close the connection or what...
					// Meaning closing the connection would result in a "reopen" on next save!
					if (conn != null)
					{
						_logger.error("checking for connection errors in: 'isSevereProblem()'. Caught error="+error+", rootCauseMsg='"+rootCauseMsg+"'. which is mapped to 'close-connection'. A new connection will be opened on next save attempt.");
						
						try { conn.close(); }
						catch (SQLException closeEx)
						{
							_logger.error("Error when closing the Counter Storage Database Connection (in isSevereProblem()) due to original error="+error+", '"+msgStr+"'.", closeEx);
						}
					}
				}
			}
	
			// ASE messages
			if ( DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(getDatabaseProductName()) )
			{
				// 1105: Can't allocate space for object '%.*s' in database '%.*s' because '%.*s' segment is full/has no free extents. If you ran out of space in syslogs, dump the transaction log. Otherwise, use ALTER DATABASE to increase the size of the segment.
				if (error == 1105 || (msgStr != null && msgStr.startsWith("Can't allocate space for object")) )
				{
					// Mark it for shutdown
					doShutdown = true;
					dbIsFull   = true;
					retCode    = true;
				}
			}

			e = e.getNextException();
		}

		_logger.debug("isSevereProblem(): doShutdown='"+doShutdown+"', dbIsFull='"+dbIsFull+"', retCode='"+retCode+"', _shutdownWithNoWait='"+_shutdownWithNoWait+"'.");

		// Actions
		if (doShutdown)
		{
			if ( ! _shutdownWithNoWait)
			{
				String extraMessage = "";
				if (dbIsFull)
					extraMessage = " (Database seems to be out of space) ";

				_logger.warn(getDatabaseProductName()+": Severe problems "+extraMessage+"when storing Performance Counters, Marking the writes for SHUTDOWN.");
				_shutdownWithNoWait = true;

				// Then STOP the service
				// NOTE: This needs it's own Thread, otherwise it's the PersistCounterHandler thread
				//       that will issue the shutdown, meaning store() will be "put on hold"
				//       until this method call is done, and continue from that point. 
				Runnable shutdownPcs = new Runnable()
				{
					@Override
					public void run()
					{
						_logger.info("Issuing STOP on the Persistent Counter Storage Handler");
						PersistentCounterHandler.getInstance().stop(true, 0);
						PersistentCounterHandler.setInstance(null);
						
						// if we are in NO-GUI mode... should we STOP DBXTUNE or should we continue...
						if ( ! DbxTune.hasGui() )
						{
							_logger.info("Since this is a NO-GUI instance, and we have stopped the PCS hander... I see no reason to continue... So Issuing STOP on the Counter Controller also, which should stop the whole '"+Version.getAppName()+"' process.");
							ICounterController cc = CounterController.getInstance();
							cc.shutdown();
						}
					}
				};
				Thread shutdownThread = new Thread(shutdownPcs);
				shutdownThread.setDaemon(true);
				shutdownThread.setName("StopPcs");
				shutdownThread.start();

				if (DbxTune.hasGui())
				{
					String msg = getDatabaseProductName()+": Severe problems "+extraMessage+"when storing Performance Counters, Disconnected from monitored server.";
					_logger.info(msg);

					MainFrame.getInstance().action_disconnect();
					SwingUtils.showErrorMessage("PersistWriterJdbc", msg, originException);
				}
			}
		}
		return retCode;
	}


	/**
	 * Called from the {@link PersistentCounterHandler#consume} as the first thing it does.
	 * 
	 * @param cont 
	 * @see PersistentCounterHandler#consume
	 */
	@Override
	public void beginOfSample(PersistContainer cont)
	{
		open(cont);
	}

	@Override
	public void endOfSample(PersistContainer cont, boolean caughtErrors)
	{
		close();
	}

//	@Override
//	public void startServices()
//	throws Exception
//	{
//		// Everything could NOT be done with the jdbcUrl... so here goes some special
//		// start the H2 TCP Server
//		if ( _jdbcDriver.equals("org.h2.Driver") && _startH2NetworkServer )
//		{
//			_logger.info("Starting a H2 TCP server.");
//			_h2ServerTcp = org.h2.tools.Server.createTcpServer("-tcpAllowOthers");
//			_h2ServerTcp.start();
//
////			_logger.info("H2 TCP server, listening on port='"+h2Server.getPort()+"', url='"+h2Server.getURL()+"', service='"+h2Server.getService()+"'.");
//			_logger.info("H2 TCP server, url='"+_h2ServerTcp.getURL()+"', service='"+_h2ServerTcp.getService()+"'.");
//
//			if (true)
//			{
//				try
//				{
//					_logger.info("Starting a H2 WEB server.");
//					_h2ServerWeb = org.h2.tools.Server.createWebServer();
//					_h2ServerWeb.start();
//
//					_logger.info("H2 WEB server, url='"+_h2ServerWeb.getURL()+"', service='"+_h2ServerWeb.getService()+"'.");
//				}
//				catch (Exception e)
//				{
//					_logger.info("H2 WEB server, failed to start, but I will continue anyway... Caught: "+e);
//				}
//			}
//
//			if (true)
//			{
//				try
//				{
//					_logger.info("Starting a H2 Postgres server.");
//					_h2ServerPg = org.h2.tools.Server.createPgServer("-pgAllowOthers");
//					_h2ServerPg.start();
//	
//					_logger.info("H2 Postgres server, url='"+_h2ServerPg.getURL()+"', service='"+_h2ServerPg.getService()+"'.");
//				}
//				catch (Exception e)
//				{
//					_logger.info("H2 Postgres server, failed to start, but I will continue anyway... Caught: "+e);
//				}
//			}
//		}
//	}

	@Override
	public void startServices()
	throws Exception
	{
		_servicesStoppedByThread = null;

		if (_lastUsedUrl == null)
		{
			_logger.info("The services will be started later. After A proper database URL has been determened in the open() method.");
			return;
		}

		// Everything could NOT be done with the jdbcUrl... so here goes some special
		// start the H2 TCP Server
		if ( _jdbcDriver.equals("org.h2.Driver") && _startH2NetworkServer )
		{
			try
			{
				boolean writeDbxTuneServiceFile = false;
				String baseDir = StringUtil.getEnvVariableValue("DBXTUNE_SAVE_DIR");

				List<String> tcpSwitches = new ArrayList<>();
				List<String> webSwitches = new ArrayList<>();
				List<String> pgSwitches  = new ArrayList<>();

				boolean startTcpServer = Configuration.getCombinedConfiguration().getBooleanProperty("h2.tcp.startServer", true);
				boolean startWebServer = Configuration.getCombinedConfiguration().getBooleanProperty("h2.web.startServer", true);
				boolean startPgServer  = Configuration.getCombinedConfiguration().getBooleanProperty("h2.pg.startServer",  true);

				int     tcpBasePortNumber  = Configuration.getCombinedConfiguration().getIntProperty("h2.tcp.port", 19092);
				int     webBasePortNumber  = Configuration.getCombinedConfiguration().getIntProperty("h2.web.port", 18082);
				int     pgBasePortNumber   = Configuration.getCombinedConfiguration().getIntProperty("h2.pg.port",  15435);

				tcpBasePortNumber = NetUtils.getFirstFreeLocalPortNumber(tcpBasePortNumber);
				webBasePortNumber = NetUtils.getFirstFreeLocalPortNumber(webBasePortNumber);
				pgBasePortNumber  = NetUtils.getFirstFreeLocalPortNumber(pgBasePortNumber);

				// If we couldn't get a port number... do not start that specific service
				if (tcpBasePortNumber == -1) { startTcpServer = false; _logger.warn("Could not get a valid port for H2 TCP Network Service, which wont be started."); }
				if (webBasePortNumber == -1) { startWebServer = false; _logger.warn("Could not get a valid port for H2 WEB Network Service, which wont be started."); }
				if (pgBasePortNumber  == -1) { startPgServer  = false; _logger.warn("Could not get a valid port for H2 Postgres Network Service, which wont be started."); }

				//-------------------------------------------
				// Switches to TCP server
				tcpSwitches.add("-tcpDaemon");         // Start the service thread as a daemon
				tcpSwitches.add("-tcpAllowOthers");    // Allow other that the localhost to connect
				tcpSwitches.add("-tcpPort");           // Try this port as a base, if it's bussy, H2 will grab "next" available
				tcpSwitches.add(""+tcpBasePortNumber); // Try this port as a base, if it's bussy, H2 will grab "next" available
				tcpSwitches.add("-ifExists");          // If the database file DO NOT exists, DO NOT CREATE one
				if (StringUtil.hasValue(baseDir))
				{
					tcpSwitches.add("-baseDir");
					tcpSwitches.add(baseDir);
					
					writeDbxTuneServiceFile = true;
				}
				
				//-------------------------------------------
				// Switches to WEB server
				webSwitches.add("-webDaemon");         // Start the service thread as a daemon
				webSwitches.add("-webAllowOthers");    // Allow other that the localhost to connect
				webSwitches.add("-webPort");           // Try this port as a base, if it's bussy, H2 will grab "next" available
				webSwitches.add(""+webBasePortNumber); // Try this port as a base, if it's bussy, H2 will grab "next" available
				webSwitches.add("-ifExists");          // If the database file DO NOT exists, DO NOT CREATE one
				if (StringUtil.hasValue(baseDir))
				{
					webSwitches.add("-baseDir");
					webSwitches.add(baseDir);
				}

				//-------------------------------------------
				// Switches to POSTGRES server
				pgSwitches.add("-pgDaemon");         // Start the service thread as a daemon
				pgSwitches.add("-pgAllowOthers");    // Allow other that the localhost to connect
				pgSwitches.add("-pgPort");           // Try this port as a base, if it's bussy, H2 will grab "next" available
				pgSwitches.add(""+pgBasePortNumber); // Try this port as a base, if it's bussy, H2 will grab "next" available
				pgSwitches.add("-ifExists");          // If the database file DO NOT exists, DO NOT CREATE one
				if (StringUtil.hasValue(baseDir))
				{
					pgSwitches.add("-baseDir");
					pgSwitches.add(baseDir);
				}
				
				
				//java -cp ${H2_JAR} org.h2.tools.Server -tcp -tcpAllowOthers -tcpPort ${portStart} -ifExists -baseDir ${baseDir} &

				
				if (startTcpServer)
				{
					_logger.info("Starting a H2 TCP server. Switches: "+tcpSwitches);
					_h2TcpServer = Server.createTcpServer(tcpSwitches.toArray(new String[0]));
					_h2TcpServer.start();
		
		//			_logger.info("H2 TCP server, listening on port='"+h2TcpServer.getPort()+"', url='"+h2TcpServer.getURL()+"', service='"+h2TcpServer.getService()+"'.");
					_logger.info("H2 TCP server, url='"+_h2TcpServer.getURL()+"', Status='"+_h2TcpServer.getStatus()+"'.");
				}
	
				if (startWebServer)
				{
					try
					{
						_logger.info("Starting a H2 WEB server. Switches: "+webSwitches);
						_h2WebServer = Server.createWebServer(webSwitches.toArray(new String[0]));
						_h2WebServer.start();

						_logger.info("H2 WEB server, url='"+_h2WebServer.getURL()+"', Status='"+_h2WebServer.getStatus()+"'.");
					}
					catch (Exception e)
					{
						_logger.info("H2 WEB server, failed to start, but I will continue anyway... Caught: "+e);
					}
				}

				if (startPgServer)
				{
					try
					{
						_logger.info("Starting a H2 Postgres server. Switches: "+pgSwitches);
						_h2PgServer = Server.createPgServer(pgSwitches.toArray(new String[0]));
						_h2PgServer.start();
		
						_logger.info("H2 Postgres server, url='"+_h2PgServer.getURL()+"', Status='"+_h2PgServer.getStatus()+"'.");
					}
					catch (Exception e)
					{
						_logger.info("H2 Postgres server, failed to start, but I will continue anyway... Caught: "+e);
					}
				}
				
				if (writeDbxTuneServiceFile)
				{
					Configuration conf = Configuration.getInstance(DbxTune.DBXTUNE_NOGUI_INFO_CONFIG);
					H2UrlHelper urlHelper = new H2UrlHelper(_lastUsedUrl);

					conf.setProperty("pcs.last.url", _lastUsedUrl);
					
					if (_h2TcpServer != null)
					{
						conf.setProperty("pcs.h2.tcp.port" ,_h2TcpServer.getPort());
						conf.setProperty("pcs.h2.tcp.url"  ,_h2TcpServer.getURL());
						conf.setProperty("pcs.h2.jdbc.url" ,"jdbc:h2:" + _h2TcpServer.getURL() + "/" + urlHelper.getFile().getName() );
					}
					if (_h2WebServer != null)
					{
						conf.setProperty("pcs.h2.web.port", _h2WebServer.getPort());
						conf.setProperty("pcs.h2.web.url",  _h2WebServer.getURL());
					}
					if (_h2PgServer != null)
					{
						conf.setProperty("pcs.h2.pg.port", _h2PgServer.getPort());
						conf.setProperty("pcs.h2.pg.url",  _h2PgServer.getURL());
					}
					
					conf.save(true);

//					try
//					{
//						// Create a file like: $DBXTUNE_SAVE_DIR/H2DBNAME.dbxtune
//						File f = new File(baseDir + File.separatorChar + urlHelper.getFile().getName() + ".dbxtune");
//						_logger.info("Creating DbxTune - H2 Service information file '" + f.getAbsolutePath() + "'.");
//
//						f.createNewFile();
//						f.deleteOnExit();
//						_h2ServiceInfoFile = f;
//
//						PrintStream w = new PrintStream( new FileOutputStream(f) );
//						w.println("dbxtune.pid = " + JavaUtils.getProcessId("-1"));
//						w.println("dbxtune.last.url = " + _lastUsedUrl);
//
//						if (_h2TcpServer != null)
//						{
//							w.println("h2.tcp.port = " + _h2TcpServer.getPort());
//							w.println("h2.tcp.url  = " + _h2TcpServer.getURL());
//							w.println("h2.jdbc.url = " + "jdbc:h2:" + _h2TcpServer.getURL() + "/" + urlHelper.getFile().getName() );
//						}
//						if (_h2WebServer != null)
//						{
//							w.println("h2.web.port = " + _h2WebServer.getPort());
//							w.println("h2.web.url  = " + _h2WebServer.getURL());
//						}
//						if (_h2PgServer != null)
//						{
//							w.println("h2.pg.port  = " + _h2PgServer.getPort());
//							w.println("h2.pg.url   = " + _h2PgServer.getURL());
//						}
//						w.close();
//					}
//					catch (Exception ex)
//					{
//						_logger.warn("Problems creating DbxTune H2 internal service file, continuing anyway. Caught: "+ex);
//					}
				}
			}
			catch (SQLException e) 
			{
				_logger.warn("Problem starting H2 network service", e);
			}
		}
	}

	@Override
	public void stopServices(int maxWaitTimeInMs)
	{
		stopServices_private(maxWaitTimeInMs, false);
	}

	private void stopServices_private(int maxWaitTimeInMs, boolean dueToDatabaseRollover)
	{
		if ( _servicesStoppedByThread != null )
		{
			_logger.info("Services has already been stopped by thread '"+_servicesStoppedByThread+"'... the stopServices() will do nothing.");
			return;
		}
		_servicesStoppedByThread = Thread.currentThread().getName();

		if (maxWaitTimeInMs <= 0)
			_shutdownWithNoWait = true;

		// Wait X seconds for Current container to be persisted.
		if (_inSaveSample && maxWaitTimeInMs > 0)
		{
			int  sleepTime = 500;
//			int  maxWaitTimeInMs = 10 * 1000;
			long startTime = System.currentTimeMillis();

			_logger.info("Waiting for "+getName()+" to persist current container. Max wait time is "+maxWaitTimeInMs+" milliseconds.");
			
			while (_inSaveSample)
			{
				long waitSoFar = System.currentTimeMillis() - startTime;
				if (waitSoFar > maxWaitTimeInMs)
				{
					_logger.info("Aborting waiting for "+getName()+" to persist current container. Waited "+waitSoFar+", now stopping the service without waiting for last persist to finish.");
					_shutdownWithNoWait = true;
					break;
				}
				if ( ! _inSaveSample)
				{
					_logger.info("Done waiting for "+getName()+" to persist current container. Waited "+waitSoFar+" until the last save was finished.");
					break;
				}
				
				try { Thread.sleep(sleepTime); }
				catch(InterruptedException e) { break; }
			}
			// Sleep just a little time if the wait time expired
			if (_shutdownWithNoWait)
			{
				try { Thread.sleep(100); }
				catch(InterruptedException ignore) { }
			}
		}

		// IF H2, make special shutdown
		if ( _jdbcDriver.equals("org.h2.Driver") && _mainConn != null)
		{
			// Get shutdown type from the Configuration.
//			H2ShutdownType h2ShutdownType       = H2ShutdownType.IMMEDIATELY;
			H2ShutdownType h2ShutdownType       = H2ShutdownType.DEFAULT;
			boolean        h2ShutdownInBgThread = false;  // a normal shutdown will wait for the H2 shutdown to take affect
			
			// Decide how we want to behave if it's a Database Rollover (then we might want to change it to a SHUTDOWN DEFRAG & do it in background)
			if (dueToDatabaseRollover)
			{
				_logger.info("Intention to Shutdown current H2 database due to a Database Rollover.");

				h2ShutdownInBgThread      = getConfig().getBooleanProperty(PROPKEY_h2_shutdown_dueToDatabaseRollover_inBgThread, DEFAULT_h2_shutdown_dueToDatabaseRollover_inBgThread);

				// Get the configured shutdown type (into a local String, which is then parsed into a H2ShutdownType)
				String H2ShutdownTypeConf = getConfig().getProperty       (PROPKEY_h2_shutdown_dueToDatabaseRollover_type,       DEFAULT_h2_shutdown_dueToDatabaseRollover_type);
				try { 
					h2ShutdownType = H2ShutdownType.valueOf( H2ShutdownTypeConf ); 
				} catch(RuntimeException ex) { 
					_logger.info("Shutdown type '"+H2ShutdownTypeConf+"' is unknown value, supported values: "+StringUtil.toCommaStr(H2ShutdownType.values())+". ");
				}
			}

			// Create a Runnable to do the shutdown work, either with a thread (in background) or directly from this thread.
			final H2ShutdownType final_h2ShutdownType = h2ShutdownType;
			final DbxConnection  final_mainConn       = _mainConn;
			Runnable h2Shutdown = new Runnable()
			{
				@Override
				public void run()
				{
					h2Shutdown(final_mainConn, final_h2ShutdownType);
				}
			};
			
			// Now execute h2Shutdown "target" (in foreground or background)
			if (h2ShutdownInBgThread)
			{
				Thread h2ShutdownThread = new Thread(h2Shutdown);
				h2ShutdownThread.setName("H2ShutdownThread");
				h2ShutdownThread.setDaemon(false); // I don't think we should run it as a daemon...
				_logger.info("Shutting down current H2 database using a BACKGROUND thread.");
				h2ShutdownThread.start();

				// 
				int sleepTimeSec = 5;
				_logger.info("Waiting for "+sleepTimeSec+" seconds, to let the H2 database shutdown THREAD to be started/initiated and do initial work before continuing.");
				try { Thread.sleep(sleepTimeSec * 1000); }
				catch (InterruptedException ignore) {}
			}
			else
			{
				h2Shutdown.run();
			}
			
			// Reset the current connection WITHOUT closing it
			// _mainConn.close() seems to block until the shutdown is complete, and since the connection wont be usefull after 
			// the shutdown we can just as easily "throw the handle", lets just hope that it do not give use any memory leaks
			_mainConn = null; 
		}

		// Close the connection
		if (_mainConn != null)
		{
			_mainConn.closeNoThrow();
			_mainConn = null; 
		}
		
		// Stop any other services
		if (_h2TcpServer != null)
		{
			_logger.info("Stopping H2 TCP Service. at port="+_h2TcpServer.getPort()+", URL: "+_h2TcpServer.getURL());
			_h2TcpServer.stop();
			_h2TcpServer = null;
		}

		if (_h2WebServer != null)
		{
			_logger.info("Stopping H2 WEB Service. at port="+_h2WebServer.getPort()+", URL: "+_h2WebServer.getURL());
			_h2WebServer.stop();
			_h2WebServer = null;
		}

		if (_h2PgServer != null)
		{
			_logger.info("Stopping H2 Postgres Service. at port="+_h2PgServer.getPort()+", URL: "+_h2PgServer.getURL());
			_h2PgServer.stop();
			_h2PgServer = null;
		}

		if (_h2ServiceInfoFile != null)
		{
			_logger.info("Deleting DbxTune - H2 Service Info File: " + _h2ServiceInfoFile.getAbsolutePath());
			_h2ServiceInfoFile.delete();
			_h2ServiceInfoFile = null;
		}
	}


	public enum H2ShutdownType
	{
		/** normal SHUTDOWN without any option. just does 'SHUTDOWN' */
		DEFAULT, 
		
		/** closes the database files without any cleanup and without compacting. */
		IMMEDIATELY, 
		
		/** fully compacts the database (re-creating the database may further reduce the database size). 
		 * If the database is closed normally (using SHUTDOWN or by closing all connections), then the database is also compacted, 
		 * but only for at most the time defined by the database setting h2.maxCompactTime (see there).*/
		COMPACT, 

		/** re-orders the pages when closing the database so that table scans are faster. */
		DEFRAG
	};

	/**
	 * Helper method to shutdown H2 
	 * 
	 * @param h2Conn
	 * @param shutdownType
	 */
	private void h2Shutdown(DbxConnection h2Conn, H2ShutdownType shutdownType)
	{
		if (h2Conn == null)
		{
			_logger.info("h2ShutdownCompact(): will do nothing, h2Conn was null");
			return;
		}
		
		String currentUrl = null;
		try { currentUrl = h2Conn.getMetaData().getURL(); }
		catch(SQLException ex) { _logger.warn("Problems getting current URL from the H2 Connection. Caught: "+ex); }
		
		// Try to get the FILES SIZE before we do 'shutdown compact'
		// Then after 'shutdown compact', we can compare the difference / db file shrinkage
		H2UrlHelper urlHelper = new H2UrlHelper(currentUrl);
		File dbFile = urlHelper.getDbFile();
		long dbFileSizeBefore = -1;
		if (dbFile != null)
			dbFileSizeBefore = dbFile.length();

		// This statement closes all open connections to the database and closes the database. 
		// This command is usually not required, as the database is closed automatically when 
		// the last connection to it is closed.
		//
		// If no option is used, then the database is closed normally. All connections are 
		// closed, open transactions are rolled back.
		// 
		// SHUTDOWN             Normal shutdown without any options 
		// SHUTDOWN COMPACT     fully compacts the database (re-creating the database may 
		//                      further reduce the database size). If the database is closed 
		//                      normally (using SHUTDOWN or by closing all connections), then 
		//                      the database is also compacted, but only for at most the time 
		//                      defined by the database setting h2.maxCompactTime (see there).
		// SHUTDOWN IMMEDIATELY closes the database files without any cleanup and without compacting.
		// SHUTDOWN DEFRAG      re-orders the pages when closing the database so that table 
		//                      scans are faster.
		//
		// Admin rights are required to execute this command.
		long startTime = System.currentTimeMillis();
		
		String shutdownCmd = "SHUTDOWN " + shutdownType;
		if (H2ShutdownType.DEFAULT.equals(shutdownType))
			shutdownCmd = "SHUTDOWN";

		// Issue a dummy command to see if the connection is still alive
		try (Statement stmnt = h2Conn.createStatement();) {
			stmnt.execute("select 1111");
		} catch(SQLException ex) {
			_logger.error("Problem when executing DUMMY SQL 'select 1111' before "+shutdownCmd+". SqlException: ErrorCode="+ex.getErrorCode()+", SQLState="+ex.getSQLState()+", toString="+ex.toString());
		}

		try (Statement stmnt = h2Conn.createStatement();) 
		{
			_logger.info("Sending Command '"+shutdownCmd+"' to H2 database.");
			stmnt.execute(shutdownCmd);
			_logger.info("Shutdown H2 database using Command '"+shutdownCmd+"', took "+TimeUtils.msDiffNowToTimeStr("%MM:%SS.%ms", startTime)+ " (MM:SS.ms)");
		} 
		catch(SQLException ex) 
		{
			// during shutdown we would expect: ErrorCode=90121, SQLState=90121, toString=org.h2.jdbc.JdbcSQLException: Database is already closed (to disable automatic closing at VM shutdown, add ";DB_CLOSE_ON_EXIT=FALSE" to the db URL)
			if ( ex.getErrorCode() == 90121 )
				_logger.info("Shutdown H2 database using '"+shutdownCmd+"', took "+TimeUtils.msDiffNowToTimeStr("%MM:%SS.%ms", startTime)+ " (MM:SS.ms)");
			else
			{
				Throwable rootCauseEx  = ExceptionUtils.getRootCause(ex);
				// String    rootCauseMsg = ExceptionUtils.getRootCauseMessage(ex);

				_logger.error("Problem when shutting down H2 using command '"+shutdownCmd+"'. SqlException: ErrorCode="+ex.getErrorCode()+", SQLState="+ex.getSQLState()+", ex.toString="+ex.toString()+", rootCauseEx="+rootCauseEx);
				
				// Check if the H2 database seems CORRUPT...
				// Caused by: java.lang.IllegalStateException: Reading from nio:/opt/dbxtune_data/DBXTUNE_CENTRAL_DB.mv.db failed; file length -1 read length 768 at 2026700328 [1.4.197/1]
				if (rootCauseEx != null && rootCauseEx.getClass().getSimpleName().equals("IllegalStateException"))
				{
					_logger.error("ATTENTION: The H2 database looks CORRUPT, the rootCauseEx is 'IllegalStateException', which might indicate a corrupt database. rootCauseEx="+rootCauseEx, rootCauseEx);
				}
			}
		}
		
		// Possibly: print information about H2 DB File SIZE information before and after shutdown compress
		if (dbFileSizeBefore > 0)
		{
			long dbFileSizeAfter = dbFile.length();
			long sizeDiff = dbFileSizeAfter - dbFileSizeBefore;
			
			_logger.info("Shutdown H2 database file size info, after '"+shutdownCmd+"'. " 
					+ "DiffMb="     + String.format("%.1f", (sizeDiff        /1024.0/1024.0))
					+ ", BeforeMb=" + String.format("%.1f", (dbFileSizeBefore/1024.0/1024.0))
					+ ", AfterMb="  + String.format("%.1f", (dbFileSizeAfter /1024.0/1024.0))
					+ ", Filename='"+dbFile.getAbsolutePath()
					+ "'.");
			
			// Check if we have a ".tempFile"
			// Then it's probably 'SHUTDOWN DEFRAG' that didn't work... (and no exception was thrown)
			File shutdownTempFile = new File(dbFile.getAbsolutePath() + ".tempFile");
			if (shutdownTempFile.exists())
			{
				_logger.warn("After Shutdown H2 database using '"+shutdownCmd+"', the file '"+shutdownTempFile+"' exists. sizeMb("+(shutdownTempFile.length()/1024/1024)+"), sizeB("+shutdownTempFile.length()+"). This is probably due to a 'incomplete' shutdown (defrag). REMOVING THIS FILE.");
				shutdownTempFile.delete();
			}
			
			// Should we write any statistics about this...
		}

		// Close the connection (it would already be closed, due to the shutdown, but anyway...)
		try { h2Conn.close(); } catch(Exception ignore) {}
	}


	/*---------------------------------------------------
	** Methods
	**---------------------------------------------------
	*/

	@Override
	public String getName()
	{
		return _name;
	}

	@Override
	public Configuration getConfig()
	{
		return _config;
	}

	@Override
	public String getConfigStr()
	{
		return _configStr;
	}

	@Override
	public void init(Configuration props) throws Exception
	{
		_config = props;

		String propPrefix = "PersistWriterJdbc.";
		String propname = null;

		// property: name
		propname = propPrefix+"name";
		_name = props.getProperty(propname, _name);

		// WRITE init message, jupp a little late, but I wanted to grab the _name
		_logger.info("Initializing the PersistentCounterHandler.WriterClass component named '"+_name+"'.");
		
		_jdbcDriver = props.getPropertyRaw(PROPKEY_jdbcDriver,   "");
		_jdbcUrl    = props.getPropertyRaw(PROPKEY_jdbcUrl,      "");
		_jdbcUser   = props.getPropertyRaw(PROPKEY_jdbcUsername, _jdbcUser);
		_jdbcPasswd = props.getProperty   (PROPKEY_jdbcPassword, "");
		if (_jdbcPasswd.equalsIgnoreCase("null"))
			_jdbcPasswd="";

		_cachePreparedStatements = props.getBooleanProperty(PROPKEY_cachePreparedStatements, _cachePreparedStatements);

		_keepConnOpen         = props.getBooleanProperty(PROPKEY_jdbcKeepConnOpen,     _keepConnOpen);
		_h2NewDbOnDateChange  = props.getBooleanProperty(PROPKEY_h2NewDbOnDateChange,  _h2NewDbOnDateChange);
		_h2DbDateParseFormat  = props.getPropertyRaw(    PROPKEY_h2DateParseFormat,    _h2DbDateParseFormat);
		_startH2NetworkServer = props.getBooleanProperty(PROPKEY_startH2NetworkServer, _startH2NetworkServer);
		
		_h2AllowSpilloverDb   = props.getBooleanProperty(PROPKEY_h2_allowSpilloverDb,  DEFAULT_h2_allowSpilloverDb);

		// Set _h2DbDateParseFormat, _h2NewDbOnDateChange if the URL has variable ${DATE:format=someFormat;roll=true|false}
		urlSubstitution(null, _jdbcUrl);

//		_logger.info("Configuration for PersistentCounterHandler.WriterClass component named '"+_name+"': "+configStr);
		_logger.info ("Configuration for PersistentCounterHandler.WriterClass component named '"+_name+"'.");
		_logger.info ("                  "+propPrefix+"jdbcDriver           = " + _jdbcDriver);
		_logger.info ("                  "+propPrefix+"jdbcUrl              = " + _jdbcUrl);
		_logger.info ("                  "+propPrefix+"jdbcUser             = " + _jdbcUser);
		_logger.info ("                  "+propPrefix+"jdbcPasswd           = " + "*hidden*" + ("".equals(_jdbcPasswd) ? " (no-passwd/blank)" : "") );
		_logger.debug(" *not-encrypted*  "+propPrefix+"jdbcPasswd           = " + _jdbcPasswd);
		_logger.info ("                  "+propPrefix+"jdbcKeepConnOpen     = " + _keepConnOpen);
		_logger.info ("                  "+propPrefix+"h2NewDbOnDateChange  = " + _h2NewDbOnDateChange);
		_logger.info ("                  "+propPrefix+"h2DateParseFormat    = " + _h2DbDateParseFormat);
		_logger.info ("                  "+propPrefix+"startH2NetworkServer = " + _startH2NetworkServer);
		_logger.info ("                  "+propPrefix+"cachePreparedStatements = " + _cachePreparedStatements);

//FIXME: write new configurations...

		if ("org.h2.Driver".equals(_jdbcDriver))
		{
			_configStr = 
				"jdbcDriver="            + _jdbcDriver +
				",jdbcUrl="              + _jdbcUrl +
				",jdbcUser="             + _jdbcUser +
				",jdbcKeepConnOpen="     + _keepConnOpen +
				",h2NewDbOnDateChange="  + _h2NewDbOnDateChange +
				",h2DateParseFormat="    + _h2DbDateParseFormat +
				",startH2NetworkServer=" + _startH2NetworkServer +
				"";
		}
		else
		{
			_configStr = 
				"jdbcDriver="            + _jdbcDriver +
				",jdbcUrl="              + _jdbcUrl +
				",jdbcUser="             + _jdbcUser +
				",jdbcKeepConnOpen="     + _keepConnOpen +
				"";
		}
}

	@Override
	protected void finalize() throws Throwable
	{
		super.finalize();
		
		boolean force = true;
		boolean dueToDatabaseRollover = false;

		close(force, dueToDatabaseRollover);
	}

	private void close(boolean force, boolean dueToDatabaseRollover)
	{
		if (_mainConn == null)
			return;

		if ( ! _keepConnOpen || force)
		{
			if (_ddlStorageConn != null)
			{
				try { _ddlStorageConn.close(); } catch(Exception ignore) {}
				_ddlStorageConn = null;
			}

			if (_sqlCaptureStorageConn != null)
			{
				try { _sqlCaptureStorageConn.close(); } catch(Exception ignore) {}
				_sqlCaptureStorageConn = null;
			}

//			if (_mainConn.isDatabaseProduct(DbUtils.DB_PROD_NAME_H2) && _keepConnOpen) // Do shutdown on H2 if we have _keepConnOpen=true... then the database needs to be campacted
//			{
//				h2ShutdownCompact();
//			}

			// Shutdown and stop any of the (H2) services
			stopServices_private(5000, dueToDatabaseRollover);
		}
	}

	@Override
	public void close()
	{
		boolean force = false;
		boolean dueToDatabaseRollover = false;

		close(force, dueToDatabaseRollover);
	}

	@Override
	public void storageQueueSizeWarning(int queueSize, int thresholdSize)
	{
		// Create a new database if the queue "backlog" is to large
		// NOTE: This only works for H2 (or other databases that has a file storage which automatically creates new databases)
		if ( _h2AllowSpilloverDb && DbUtils.isProductName(getDatabaseProductName(), DbUtils.DB_PROD_NAME_H2) )
		{
			int createNewDbThreshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_h2_queueSizeWarning_createNewDbThreshold, DEFAULT_h2_queueSizeWarning_createNewDbThreshold);
			if (queueSize > createNewDbThreshold)
			{
				long lastSpillOverCreationAgeThreshold = 60 * 60 * 1000; // 1 hour
				long lastSpillOverCreationAge = System.currentTimeMillis() - _h2NewSpilloverDbCreateTime;

//System.out.println("DEBUG: storageQueueSizeWarning() lastSpillOverCreationAge="+lastSpillOverCreationAge+", _h2NewSpilloverDbCreateTime="+_h2NewSpilloverDbCreateTime);
//System.out.println("DEBUG: storageQueueSizeWarning() (lastSpillOverCreationAge < lastSpillOverCreationAgeThreshold) = "+(lastSpillOverCreationAge < lastSpillOverCreationAgeThreshold));
				if (_connStatus_inProgress)
				{
					_logger.warn("DISREGARD-REQUEST(SPILL-OVER-DB): Storage queue size is "+queueSize+", which is above the threshold 'configuration: '"+PROPKEY_h2_queueSizeWarning_createNewDbThreshold+"="+createNewDbThreshold+"'. BUT the connection status is already 'in progress' so a new database is created right now. Create new 'spill-over-database' request is DISREGARDED.");
				}
				else if (lastSpillOverCreationAge < lastSpillOverCreationAgeThreshold)
				{
					_logger.warn("DISREGARD-REQUEST(SPILL-OVER-DB): Storage queue size is "+queueSize+", which is above the threshold 'configuration: '"+PROPKEY_h2_queueSizeWarning_createNewDbThreshold+"="+createNewDbThreshold+"'. BUT the spillover database was created "+TimeUtils.msToTimeStr(lastSpillOverCreationAge)+" ago and the create-new-limit is "+TimeUtils.msToTimeStr(lastSpillOverCreationAgeThreshold)+". Create new 'spill-over-database' request is DISREGARDED.");
				}
				else
				{
					_logger.warn("REQUEST(SPILL-OVER-DB): Storage queue size is "+queueSize+", which is above the threshold 'configuration: '"+PROPKEY_h2_queueSizeWarning_createNewDbThreshold+"="+createNewDbThreshold+"'. So lets resolve the situation by creating a new H2 database to store the recording. This will be done on next store iteration.");
					_h2CreateNewSpilloverDbOnNextSave = true;
				}
			}
		}
	}

	/**
	 * 
	 * @return
	 */
	private String urlSubstitution(PersistContainer cont, String inputUrl)
	{
		if (inputUrl == null) throw new RuntimeException("urlSubstitution(): inputUrl is null");

		Timestamp newDate = (cont != null) ? cont.getMainSampleTime() : new Timestamp(System.currentTimeMillis());

//		String val = "abc ${DATE:format=yyyyMMdd.HHmm; roll=true} xxx ${SERVERNAME}:${VAR_IS_UNKNOWN}";
		String val = inputUrl; 
		_logger.debug("urlSubstitution(): INPUT: inputUrl='"+val+"'.");
//System.out.println("PCS: urlSubstitution: INPUT: inputUrl='"+val+"'.");

//		Pattern compiledRegex = Pattern.compile("\\$\\{.*\\}");
		Pattern compiledRegex = Pattern.compile("\\$\\{.+\\}");
		while( compiledRegex.matcher(val).find() )
		{
			String varVal      = null;
			String varStr      = val.substring( val.indexOf("${")+2, val.indexOf("}") );
			String varName     = varStr;
			Configuration varConf = null;
//System.out.println("PCS: urlSubstitution: LOOP: varName='"+varName+"', varStr='"+varStr+"'.");
			if (varStr.indexOf(':') >= 0)
			{
				int firstColon = varStr.indexOf(':');
				varName = varStr.substring(0, firstColon);
				String varModifier = varStr.substring(firstColon + 1);
				
				try { varConf = Configuration.parse(varModifier, ";"); }
				catch (ParseException pe) 
				{
					_logger.error("Problems parsing variables in the H2 URL '"+inputUrl+"' at the variable '"+varName+"' with the modifier '"+varModifier+"'. Caught: "+pe);
				}
			}
//System.out.println("PCS: urlSubstitution: LOOP-2: varName='"+varName+"', varStr='"+varStr+"'.");

			_logger.debug("urlSubstitution(): varName='"+varName+"', varModifyer='"+varConf+"'.");
			
			if ( "DATE".equals(varName) )
			{
				// Get variable modifiers
				String  dateFormat = _h2DbDateParseFormat;
				boolean roll       = _h2NewDbOnDateChange;
				if (varConf != null)
				{
					dateFormat = varConf.getProperty(       "format", dateFormat);
					roll       = varConf.getBooleanProperty("roll",   roll);
				}

				// GET a new Date String
				varVal = new SimpleDateFormat(dateFormat).format(newDate);
				
				// if modifier changed the defaults... set the defaults to the new values
				if ( ! _h2DbDateParseFormat.equals(dateFormat))
				{
					_logger.info("Changing Property: 'h2DateParseFormat' from '"+_h2DbDateParseFormat+"' to '"+dateFormat+"'. The URL Variable '${DATE}' modifier 'format' overrides the default value.");
					_h2DbDateParseFormat = dateFormat;
				}

				if ( _h2NewDbOnDateChange != roll )
				{
					_logger.info("Changing Property: 'h2NewDbOnDateChange' from '"+_h2NewDbOnDateChange+"' to '"+roll+"'. The URL Variable '${DATE}' modifier 'roll' overrides the default value.");
					_h2NewDbOnDateChange = roll;
				}
			}
			else if ( "SERVERNAME".equals(varName) )
			{
//				varVal = AseConnectionUtils.getAseServername(conn);
				varVal = "";
				if (cont != null)
					varVal = DbxTune.stripSrvName(cont.getServerNameOrAlias());
			}
			else if ( "ASEHOSTNAME".equals(varName) || "SRVHOSTNAME".equals(varName) || "HOSTNAME".equals(varName) )
			{
//				varVal = AseConnectionUtils.getAseHostname(conn, true);
				varVal = "";
				if (cont != null)
					varVal = cont.getOnHostname();
			}
//			else if ( "ASETUNE_HOME"    .equals(varName) ) { varVal = System.getProperty("ASETUNE_HOME",     ""); }
//			else if ( "ASETUNE_SAVE_DIR".equals(varName) ) { varVal = System.getProperty("ASETUNE_SAVE_DIR", ""); }
//			else if ( "IQTUNE_HOME"     .equals(varName) ) { varVal = System.getProperty("IQTUNE_HOME",      ""); }
//			else if ( "IQTUNE_SAVE_DIR" .equals(varName) ) { varVal = System.getProperty("IQTUNE_SAVE_DIR",  ""); }
//			else if ( "RSTUNE_HOME"     .equals(varName) ) { varVal = System.getProperty("RSTUNE_HOME",      ""); }
//			else if ( "RSTUNE_SAVE_DIR" .equals(varName) ) { varVal = System.getProperty("RSTUNE_SAVE_DIR",  ""); }
//			else if ( varName != null && varName.endsWith("TUNE_SAVE_DIR")) 
//			{
//				varName = DbxTune.getInstance().getAppSaveDirEnvName();
//				varVal  = StringUtil.getEnvVariableValue(varName);
//System.out.println("PCS: urlSubstitution: varName='"+varName+"', varVal='"+varVal+"'.");
//			}
//			else if ( varName != null && varName.endsWith("TUNE_HOME")) 
//			{
//				varName = DbxTune.getInstance().getAppHomeEnvName();
//				varVal  = StringUtil.getEnvVariableValue(varName);
////System.out.println("PCS: urlSubstitution: varName='"+varName+"', varVal='"+varVal+"'.");
//			}
			else if ( varName != null && "DBXTUNE_SAVE_DIR".equals(varName)) 
			{
				varVal  = StringUtil.getEnvVariableValue("DBXTUNE_SAVE_DIR");
			}
			else if ( varName != null && "DBXTUNE_HOME".equals(varName)) 
			{
				varVal  = StringUtil.getEnvVariableValue("DBXTUNE_HOME");
			}
			else
			{
				_logger.warn("PCS: urlSubstitution(): WARNING: Unknown variable '"+varName+"', simply removing it from the output.");
				varVal = "";
				//varVal = "$"+varStr+"";
			}

			if (varVal == null)
				varVal = "";

//System.out.println("PCS: urlSubstitution(): Substituting varName '${"+varStr+"}' with value '"+varVal+"'.");
			_logger.debug("urlSubstitution(): Substituting varName '${"+varStr+"}' with value '"+varVal+"'.");

			// NOW substitute the ENVVARIABLE with a real value...
			val = val.replace("${"+varStr+"}", varVal);
		}
		_logger.debug("urlSubstitution(): AFTER: val='"+val+"'.");
		
		return val;
	}

	private String getNewH2SpillOverUrl(String lastUsedUrl)
	{
		H2UrlHelper urlHelper = new H2UrlHelper(lastUsedUrl);
		
		String currentFilename = urlHelper.getFilename();
		int spillDbId = 1;
		if (currentFilename.matches(".*-SPILL-OVER-DB-[0-9]+"))
		{
			int startPos = currentFilename.lastIndexOf("-SPILL-OVER-DB-");
			int endPos = currentFilename.length();
			if (startPos >= 0)
			{
				String filePrefix = currentFilename.substring(0, startPos);
				startPos += "-SPILL-OVER-DB-".length();

				String spillDbIdStr = currentFilename.substring(startPos, endPos);

				// Remove everything but numbers (if there are any)
				spillDbIdStr = spillDbIdStr.replaceAll("[^0-9]", "");

				// Get the new number as an INT
				spillDbId = StringUtil.parseInt(spillDbIdStr, spillDbId) + 1;
				
				// Strip off the "-SPILL-OVER-DB-" from the origin file
				currentFilename = filePrefix;
			}
		}
		currentFilename += "-SPILL-OVER-DB-" + spillDbId;
		
		String localJdbcUrl = urlHelper.getNewUrl(currentFilename);
//System.out.println("---");
//System.out.println(">>> input >>>> |"+lastUsedUrl+"|.");
//System.out.println("<<< output <<< |"+localJdbcUrl+"|.");

		return localJdbcUrl;
	}

	//	@Override
	private DbxConnection open(PersistContainer cont)
	{
		Timestamp newDate = (cont != null) ? cont.getMainSampleTime() : new Timestamp(System.currentTimeMillis());

		// Try to resolve LONG Storage Queue sizes by creating a new H2 database.
		if (_h2CreateNewSpilloverDbOnNextSave && _mainConn != null)
		{
			_logger.info("SPILL-OVER-DB: Closing the old database, a new database will be created/opened, This due to a high Storage queue... and the current H2 database does not seem to keep up...");

			boolean force = true;
			boolean dueToDatabaseRollover = false;

			close(force, dueToDatabaseRollover);
		}

		// for H2, we have a "new database option" if the "timestamp" has changed.
		if (_h2NewDbOnDateChange && _mainConn != null)
		{
			String dateStr = new SimpleDateFormat(_h2DbDateParseFormat).format(newDate);
			// If new Date or first time...
			if ( ! dateStr.equals(_h2LastDateChange) )
			{
				_lastUsedUrl = null;

				// Create a "daily" report, with:
				//  - Alarms that has happened
				//  - A summary of Performance issues that has happened the day
				//  - etc...
				// Do this in a try/catch so if we have issues: We would still continue with the "db-file-roll-over"
				try { createDailySummaryReport(_mainConn, cont.getServerNameOrAlias()); }
				catch (Exception ex)
				{
					_logger.error("Problems creating Daily Report, skipping this.", ex);
				}

				boolean force = true;
				boolean dueToDatabaseRollover = false;

				if ( _h2LastDateChange != null)
				{
					dueToDatabaseRollover = true;
					_logger.info("Closing the old database with ${DATE} marked as '"+_h2LastDateChange+"', a new database will be opened using ${DATE} marker '"+dateStr+"'.");
				}

				close(force, dueToDatabaseRollover);
			}
		}

		// If we already has a valid connection, lets reuse it...
		if (_keepConnOpen && _mainConn != null)
		{
			try 
			{
				if ( ! _mainConn.isClosed() )
					return _mainConn; // return the GOOD connection

				// Try the connection (some JDBC providers the above isClosed() isn't enough)
				// do some simple select... (for H2, ASE, ASA, SQL-Server, possible more)
				// Some other databases needs to have a 'from DUAL|orSomeDummyTable', lets implement that later
				String sql = null;
				if ( DbUtils.isProductName(getDatabaseProductName(), 
						DbUtils.DB_PROD_NAME_H2, 
						DbUtils.DB_PROD_NAME_MSSQL, 
						DbUtils.DB_PROD_NAME_SYBASE_ASA, 
						DbUtils.DB_PROD_NAME_SYBASE_ASE))
				{
					sql = "select 1";
				}

				// Execute the simple select, to check if we have a good connection, then return the connection
				// if we get error: we will end up in the catch(SQLException) and later on we will try to make a new connection
				if (sql != null)
				{
					Statement stmnt = _mainConn.createStatement();
					ResultSet rs = stmnt.executeQuery(sql);
					while(rs.next())
						/* do nothing */;
					rs.close();
					stmnt.close();
					
					// return the GOOD connection
					return _mainConn;
				}
			}
			catch (SQLException e) 
			{
				/* do nothing: later on we will create a new connection */
			}
		}
		
		try
		{
			_connStatus_inProgress = true;

//			Class.forName(_jdbcDriver).newInstance();

			_logger.debug("Try getConnection to counterStore");

			if (_keepConnOpen)
				_logger.info("Open a new connection to the Persistent Counter Storage. 'keep conn open=true'");

			// Get a URL which might be changed or not...
			String localJdbcUrl = _lastUsedUrl == null ? _jdbcUrl : _lastUsedUrl;

			// Look for variables in the URL and change them into runtime
			if ( localJdbcUrl.indexOf("${") >= 0)
			{
				localJdbcUrl = urlSubstitution(cont, localJdbcUrl);
				_logger.info("Found variables in the URL '"+_jdbcUrl+"', the new URL will be '"+localJdbcUrl+"'.");
				if (_h2NewDbOnDateChange)
					_logger.info("When a new ${DATE} of the format '"+_h2DbDateParseFormat+"' has been reached, a new database will be opened using that timestamp.");

				// This will be used to determine if a new DB should be opened with a new TIMESTAMP.
				String dateStr = new SimpleDateFormat(_h2DbDateParseFormat).format(newDate);
				_h2LastDateChange = dateStr;

				// Also clear what DDL's has been created, so they can be recreated.
				clearIsDdlCreatedCache();
				
				// Also clear what what DDL information that has been stored, so we can add them to the new database again
				clearDdlDetailesCache();

				// Indicate the we need to start a new session...
				setSessionStarted(false);
			}

			if (_h2CreateNewSpilloverDbOnNextSave)
			{
				// Also clear what DDL's has been created, so they can be recreated.
				clearIsDdlCreatedCache();
				
				// Also clear what what DDL information that has been stored, so we can add them to the new database again
				clearDdlDetailesCache();

				// Indicate the we need to start a new session...
				setSessionStarted(false);
			}

			// IF H2, add hard coded stuff to URL
			if ( _jdbcDriver.equals("org.h2.Driver") )
			{
				H2UrlHelper urlHelper = new H2UrlHelper(localJdbcUrl);
				Map<String, String> urlMap = urlHelper.getUrlOptionsMap();
				if (urlMap == null)
					urlMap = new LinkedHashMap<String, String>();

				boolean change = false;

				// Database short names are converted to uppercase for the DATABASE() function, 
				// and in the CATALOG column of all database meta data methods. 
				// Setting this to "false" is experimental. 
				// When set to false, all identifier names (table names, column names) are case 
				// sensitive (except aggregate, built-in functions, data types, and keywords).
				if ( ! urlMap.containsKey("DATABASE_TO_UPPER") )
				{
					change = true;
					_logger.info("H2 URL add option: DATABASE_TO_UPPER=false");
					urlMap.put("DATABASE_TO_UPPER", "false");
				}

				// This sis a test to see if H2 behaves better with AseTune
				// SInce AseTune only *writes* to the database... we do not need to ReUseSpace (nothing gets deleted)
				// and the hope is that the function MVStore.java - freeUnusedChunks() wont get called
				// It seems to be the cleanup that causes us problem...
// This made the files *much* bigger... So I had to turn REUSE_SPACE=true (which is the default) back on again 
//				if ( ! urlMap.containsKey("REUSE_SPACE") )
//				{
//					change = true;
//					_logger.info("H2 URL add option: REUSE_SPACE=FALSE");
//					urlMap.put("REUSE_SPACE",  "FALSE");
//				}
				if ( ! urlMap.containsKey("REUSE_SPACE") )
				{
					String h2ReuseSpace = getConfig().getProperty("dbxtune.h2.REUSE_SPACE", null);
					if (h2ReuseSpace != null)
					{
						h2ReuseSpace = h2ReuseSpace.toUpperCase().trim();
						if (h2ReuseSpace.equals("TRUE") || h2ReuseSpace.equals("FALSE"))
						{
							change = true;
							_logger.info("H2 URL add option: REUSE_SPACE="+h2ReuseSpace);
							urlMap.put("REUSE_SPACE",  h2ReuseSpace);
						}
						else
						{
							_logger.warn("H2 URL OPTION 'dbxtune.h2.reuseSpace' must be 'TRUE' or 'FALSE'. the passed option was '" + h2ReuseSpace + "'.");
						}
					}
				}

				// This property is only used when using the MVStore storage engine. How long to retain old, persisted data, in milliseconds. 
				// The default is 45000 (45 seconds), 0 means overwrite data as early as possible. 
				// It is assumed that a file system and hard disk will flush all write buffers within this time. 
				// Using a lower value might be dangerous, unless the file system and hard disk flush the buffers earlier. 
				// To manually flush the buffers, use CHECKPOINT SYNC, however please note that according to various tests this 
				// does not always work as expected depending on the operating system and hardware.
				//
				// Admin rights are required to execute this command, as it affects all connections. 
				// This command commits an open transaction in this connection. This setting is persistent. 
				// This setting can be appended to the database URL: jdbc:h2:test;RETENTION_TIME=0
				if ( ! urlMap.containsKey("RETENTION_TIME") )
				{
//					int h2RetentionTime = getConfig().getIntProperty("dbxtune.h2.RETENTION_TIME", -1); // default 10 minutes
//					int h2RetentionTime = getConfig().getIntProperty("dbxtune.h2.RETENTION_TIME", 45_000); // This is the H2 DEFAULT value
//TODO: set this to 45_000 but right now we seems to have a problem so REVERT back to what we know "worked"
					int h2RetentionTime = getConfig().getIntProperty("dbxtune.h2.RETENTION_TIME", 600_000); // default 10 minutes
					if (h2RetentionTime > -1) // set to -1 to disable this option
					{
						change = true;
						_logger.info("H2 URL add option: RETENTION_TIME="+h2RetentionTime);
						urlMap.put("RETENTION_TIME",  Integer.toString(h2RetentionTime));
					}
				}
				

				// If we want to bump up the cache... Default is 64M per GB the JVM has
				if ( ! urlMap.containsKey("CACHE_SIZE") )
				{
					int h2CacheInMb = getConfig().getIntProperty("dbxtune.h2.cacheSizeInMb", -1);
					if (h2CacheInMb > 0)
					{
    					int h2CacheInKb = h2CacheInMb * 1024;
    					change = true;
    					_logger.info("H2 URL add option: CACHE_SIZE="+h2CacheInKb);
    					urlMap.put("REUSE_SPACE",  h2CacheInKb+"");
					}
				}

				// The maximum time in milliseconds used to compact a database when closing.
				if ( ! urlMap.containsKey("MAX_COMPACT_TIME") )
				{
					change = true;
					_logger.info("H2 URL add option: MAX_COMPACT_TIME=2000");
					urlMap.put("MAX_COMPACT_TIME",  "2000");
				}

				// in H2 1.4 LOB Compression is done via an URL Option
				if ( ! urlMap.containsKey("COMPRESS") )
				{
					change = true;
					_logger.info("H2 URL add option: COMPRESS=TRUE");
					urlMap.put("COMPRESS",  "TRUE");
				}

				// AutoServer mode
//				if ( ! urlMap.containsKey("AUTO_SERVER") )
//				{
//					change = true;
//					_logger.info("H2 URL add option: AUTO_SERVER=TRUE");
//					urlMap.put("AUTO_SERVER",  "TRUE");
//				}

				// WRITE_DELAY mode, hopefully this will help to shorten commit time, and we don't really need the durability...
				// Maybe MVStore has some more specific options RETENTION_TIME... may be something to look at
				if ( ! urlMap.containsKey("WRITE_DELAY") )
				{
//					int h2WriteDelay = getConfig().getIntProperty("dbxtune.h2.WRITE_DELAY", -1);
//					int h2WriteDelay = getConfig().getIntProperty("dbxtune.h2.WRITE_DELAY", 2_000);
					int h2WriteDelay = getConfig().getIntProperty("dbxtune.h2.WRITE_DELAY", 30_000);
//TODO: set this to 2_000 but right now we seems to have a problem so REVERT back to what we know "worked"
					if (h2WriteDelay > -1) // set to -1 to disable this option
					{
						change = true;
						_logger.info("H2 URL add option: WRITE_DELAY="+h2WriteDelay);
						urlMap.put("WRITE_DELAY",  h2WriteDelay+"");
					}
				}

				// DATABASE_EVENT_LISTENER mode
//				if ( ! urlMap.containsKey("DATABASE_EVENT_LISTENER") )
//				{
//					change = true;
//					_logger.info("H2 URL add option: DATABASE_EVENT_LISTENER="+H2DatabaseEventListener.class.getName());
//					urlMap.put("DATABASE_EVENT_LISTENER",  "'" + H2DatabaseEventListener.class.getName() + "'");
//				}

				// DB_CLOSE_ON_EXIT = if we have our of SHUTDOWN hook thats closing H2
				boolean isShutdownHookInstalled = System.getProperty("dbxtune.isShutdownHookInstalled", "false").trim().equalsIgnoreCase("true");
				if (isShutdownHookInstalled)
				{
					if ( ! urlMap.containsKey("DB_CLOSE_ON_EXIT") )
					{
						change = true;
						_logger.info("H2 URL add option: DB_CLOSE_ON_EXIT=false  (due to isShutdownHookInstalled="+isShutdownHookInstalled+")");
						urlMap.put("DB_CLOSE_ON_EXIT", "FALSE");
					}
				}


				if (change)
				{
					urlHelper.setUrlOptionsMap(urlMap);
					localJdbcUrl = urlHelper.getUrl();
					
					_logger.info("Added some options to the H2 URL. New URL is '"+localJdbcUrl+"'.");
				}
			}
			
			if (_h2CreateNewSpilloverDbOnNextSave && _lastUsedUrl != null)
			{
				// Do: _h2CreateNewSpilloverDbOnNextSave = false
				// AFTER the connection has been done.
				
				String lastUsedUrl = _lastUsedUrl;

				H2UrlHelper urlHelper = new H2UrlHelper(lastUsedUrl);
				if ( ! urlHelper.isUrlTypeFile() )
				{
					_logger.error("SPILL-OVER-DB: H2 database type is not 'file'. Can't create a 'Spill over database'. Trying to reconnect to the old database (or if 'rollover' created a new database, that will be used.).");
					_h2CreateNewSpilloverDbOnNextSave = false;
				}
				else
				{
					// get a new URL for the new "spill-over-db-#"
					// The new filename would be: -SPILL-OVER-DB-#
					// and the '#' will be incremented if one already exists
					localJdbcUrl = getNewH2SpillOverUrl(lastUsedUrl);
					_logger.info("SPILL-OVER-DB: H2 'Spill-over-database' will be created using the URL='"+localJdbcUrl+"'.");

					// Set this early, but also after the connection is made, if it takes a long time to connect...
					_h2NewSpilloverDbCreateTime = System.currentTimeMillis();
				}
			}

//			Connection conn = DriverManager.getConnection(localJdbcUrl, _jdbcUser, _jdbcPasswd);
//			_mainConn = DbxConnection.createDbxConnection(conn);
			// The below connection properties will be used if/when doing reConnect()
			ConnectionProp connProp = new ConnectionProp();
			connProp.setDriverClass(_jdbcDriver);
			connProp.setUsername(_jdbcUser);
			connProp.setPassword(_jdbcPasswd);
			connProp.setUrl(localJdbcUrl);
			connProp.setAppName(Version.getAppName()+"-pcsWriter");
			connProp.setAppVersion(Version.getVersionStr());

			// Now Connect
			long connectStartTime = System.currentTimeMillis();
			_mainConn = DbxConnection.connect(null, connProp);
			String connectTimeStr = TimeUtils.msDiffNowToTimeStr(connectStartTime);

			// Remember the last used URL (in case of H2 spill over database), the _mainConn is null at that time, so we cant use _mainConn.getConnProp().getUrl()
			_lastUsedUrl = localJdbcUrl;
			
			// Remember when we last did this so we can CANCEL any new requests to do so in the near future
			if (_h2CreateNewSpilloverDbOnNextSave)
			{
				_h2NewSpilloverDbCreateTime = System.currentTimeMillis();
				_h2CreateNewSpilloverDbOnNextSave = false;
			}
			
			_logger.info("A Database connection has been opened. connectTime='"+connectTimeStr+"', to URL '"+localJdbcUrl+"', using driver '"+_jdbcDriver+"'.");
			_logger.debug("The connection has property auto-commit set to '"+_mainConn.getAutoCommit()+"'.");

			// DDL Information Capture connection
			if (Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_ddlInfoStorage_usePrivateConnection, DEFAULT_ddlInfoStorage_usePrivateConnection))
			{
				String appName = Version.getAppName()+"-pcsWriterDdlInfo";
				ConnectionProp cp = new ConnectionProp(connProp);
				cp.setAppName(appName);

				_logger.info("Open a dedicated connection to store 'DDL Information' at '"+cp.getUrl()+"' with application name '"+appName+"'.");
				_ddlStorageConn = DbxConnection.connect(null, connProp);
			}

			// SQL Capture connection
			if (Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sqlCaptureStorage_usePrivateConnection, DEFAULT_sqlCaptureStorage_usePrivateConnection))
			{
				String appName = Version.getAppName()+"-pcsWriterSqlCap";
				ConnectionProp cp = new ConnectionProp(connProp);
				cp.setAppName(appName);

				_logger.info("Open a dedicated connection to store 'SQL Capture Information' at '"+cp.getUrl()+"' with application name '"+appName+"'.");
				_sqlCaptureStorageConn = DbxConnection.connect(null, connProp);
			}

			// Write info about what JDBC driver we connects via.
			if ( ! _jdbcDriverInfoHasBeenWritten )
			{
            	_jdbcDriverInfoHasBeenWritten = true;

            	if (_logger.isDebugEnabled()) 
                {
    				_logger.debug("The following drivers have been loaded:");
    				Enumeration<Driver> drvEnum = DriverManager.getDrivers();
    				while( drvEnum.hasMoreElements() )
    				{
    					_logger.debug("    " + drvEnum.nextElement().toString());
    				}
    			}
    			
            	DatabaseMetaData dbmd = _mainConn.getMetaData();
				if (dbmd != null)
				{
					String getDriverName             = "-";
					String getDriverVersion          = "-";
					int    getDriverMajorVersion     = -1;
					int    getDriverMinorVersion     = -1;
					int    getJDBCMajorVersion       = -1;
					int    getJDBCMinorVersion       = -1;

					String getDatabaseProductName    = "-";
					String getDatabaseProductVersion = "-";
					int    getDatabaseMajorVersion   = -1;
					int    getDatabaseMinorVersion   = -1;

//					String getIdentifierQuoteString  = "\"";

					try	{ getDriverName             = dbmd.getDriverName();             } catch (Throwable ignore) {}
					try	{ getDriverVersion          = dbmd.getDriverVersion();          } catch (Throwable ignore) {}
					try	{ getDriverMajorVersion     = dbmd.getDriverMajorVersion();     } catch (Throwable ignore) {}
					try	{ getDriverMinorVersion     = dbmd.getDriverMinorVersion();     } catch (Throwable ignore) {}
					try	{ getJDBCMajorVersion       = dbmd.getJDBCMajorVersion();       } catch (Throwable ignore) {}
					try	{ getJDBCMinorVersion       = dbmd.getJDBCMinorVersion();       } catch (Throwable ignore) {}

					try	{ getDatabaseProductName    = dbmd.getDatabaseProductName();    } catch (Throwable ignore) {}
					try	{ getDatabaseProductVersion = dbmd.getDatabaseProductVersion(); } catch (Throwable ignore) {}
					try	{ getDatabaseMajorVersion   = dbmd.getDatabaseMajorVersion();   } catch (Throwable ignore) {}
					try	{ getDatabaseMinorVersion   = dbmd.getDatabaseMinorVersion();   } catch (Throwable ignore) {}

//					try	{ getIdentifierQuoteString  = dbmd.getIdentifierQuoteString();  } catch (Throwable ignore) {}

					_logger.info("Connected using JDBC driver Name='"+getDriverName
							+"', Version='"         +getDriverVersion
							+"', MajorVersion='"    +getDriverMajorVersion
							+"', MinorVersion='"    +getDriverMinorVersion
							+"', JdbcMajorVersion='"+getJDBCMajorVersion
							+"', JdbcMinorVersion='"+getJDBCMinorVersion
							+"'.");
					_logger.info("Connected to Database Product Name='"+getDatabaseProductName
							+"', Version='"     +getDatabaseProductVersion
							+"', MajorVersion='"+getDatabaseMajorVersion
							+"', MinorVersion='"+getDatabaseMinorVersion
							+"'.");

					// Set what type of database we are connected to.
					setDatabaseProductName(getDatabaseProductName == null ? "" : getDatabaseProductName);

//					// Get and set the QuotedIdentifier Character
//					setQuotedIdentifierChar(getIdentifierQuoteString == null ? "\"" : getIdentifierQuoteString);
				}
			}

			// if H2
			// Set some specific stuff
			if ( DbUtils.DB_PROD_NAME_H2.equals(getDatabaseProductName()) )
			{
				_logger.info("Do H2 Specific settings for the database.");
				setH2SpecificSettings(_mainConn);
				setH2SpecificSettings(_ddlStorageConn);
				setH2SpecificSettings(_sqlCaptureStorageConn);
				
				// instantiate a small H2 Performance Counter Collector
//				H2WriterStat h2stat = new H2WriterStat(new H2WriterStat.DbxTuneH2PerfCounterConnectionProvider());
				H2WriterStat h2stat = new H2WriterStat(new ConnectionProvider()
				{
					@Override
					public DbxConnection getConnection()
					{
						// NOTE: Will this work if the _mainConn is already in use...
						return _mainConn;
					}

					@Override
					public DbxConnection getNewConnection(String appname)
					{
						throw new RuntimeException("getNewConnection(appname): -NOT-IMPLEMENTED-");
					}
					
				});
				H2WriterStat.setInstance(h2stat);
			}

			// if ASE, turn off error message like: Scale error during implicit conversion of NUMERIC value '1.2920528650283813' to a NUMERIC field.
			// or fix the actual values to be more correct when creating graph data etc.
			if ( DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(getDatabaseProductName()) )
			{
				_logger.info("Do Sybase ASE Specific settings for the database.");
				setAseSpecificSettings(_mainConn);
				setAseSpecificSettings(_ddlStorageConn);
				setAseSpecificSettings(_sqlCaptureStorageConn);
			}

			// GET information already stored in the DDL Details storage
			populateDdlDetailesCache();
			
			// Start any services that depended on open() to complete
			startServices();
		}
		catch (SQLException ev)
		{
			boolean h2IsServerAlreadyRunning = false;
			
			StringBuffer sb = new StringBuffer();
			while (ev != null)
			{
				// DATABASE_ALREADY_OPEN_1 = 90020
				if (ev.getErrorCode() == 90020)
					h2IsServerAlreadyRunning = true;

				sb.append( "\n" );
				sb.append("ErrorCode=").append( ev.getErrorCode() ).append(", ");
				sb.append("SQLState=" ).append( ev.getSQLState()  ).append(", ");
				sb.append("Message="  ).append( ev.getMessage()   );
				ev = ev.getNextException();
			}
			_logger.error("Problems when connecting to a datastore Server. "+sb.toString());
			_mainConn = closeConn(_mainConn);
			_ddlStorageConn = closeConn(_ddlStorageConn);;
			_sqlCaptureStorageConn = closeConn(_sqlCaptureStorageConn);;

			// IF H2, make special things
			if ( _jdbcDriver.equals("org.h2.Driver") && _mainConn != null)
			{
				// Try to close ALL H2 servers, many might be active due to AUTO_SERVER=true
				if (h2IsServerAlreadyRunning)
				{
					// param 1: 0 = all servers
					// param 3: 0 = SHUTDOWN_NORMAL, 1 = SHUTDOWN_FORCE
					TcpServer.stopServer(0, _jdbcPasswd, 1);
				}
			}
		}
		catch (Exception ev)
		{
			_logger.error("openConnection", ev);
			_mainConn = closeConn(_mainConn);
			_ddlStorageConn = closeConn(_ddlStorageConn);;
			_sqlCaptureStorageConn = closeConn(_sqlCaptureStorageConn);;
		}
		finally
		{
			_connStatus_inProgress = false;
		}
		
		return _mainConn;
	}

	private DbxConnection closeConn(DbxConnection conn)
	{
		if (conn == null)
			return null;

		try { conn.close(); }
		catch (Exception ignore) {}
		return null;
	}


	private void setH2SpecificSettings(DbxConnection conn)
	throws SQLException
	{
		if (conn == null)
			return;

		boolean logExtraInfo = _mainConn == conn;
		
		// Sets the size of the cache in KB (each KB being 1024 bytes) for the current database. 
		// The default value is 16384 (16 MB). The value is rounded to the next higher power 
		// of two. Depending on the virtual machine, the actual memory required may be higher.
		//dbExecSetting(conn, "SET CACHE_SIZE int", logExtraInfo);

		// Sets the compression algorithm for BLOB and CLOB data. Compression is usually slower, 
		// but needs less disk space. LZF is faster but uses more space.
		// Admin rights are required to execute this command, as it affects all connections. 
		// This command commits an open transaction. This setting is persistent.
		// SET COMPRESS_LOB { NO | LZF | DEFLATE }
		dbExecSetting(conn, "SET COMPRESS_LOB DEFLATE", logExtraInfo);

		// Sets the default lock timeout (in milliseconds) in this database that is used for 
		// the new sessions. The default value for this setting is 1000 (one second).
		// Admin rights are required to execute this command, as it affects all connections. 
		// This command commits an open transaction. This setting is persistent.
		// SET DEFAULT LOCK_TIMEOUT int
		dbExecSetting(conn, "SET DEFAULT_LOCK_TIMEOUT 30000", logExtraInfo);

		// If IGNORECASE is enabled, text columns in newly created tables will be 
		// case-insensitive. Already existing tables are not affected. 
		// The effect of case-insensitive columns is similar to using a collation with 
		// strength PRIMARY. Case-insensitive columns are compared faster than when 
		// using a collation. String literals and parameters are however still considered 
		// case sensitive even if this option is set.
		// Admin rights are required to execute this command, as it affects all connections. 
		// This command commits an open transaction. This setting is persistent. 
		// This setting can be appended to the database URL: jdbc:h2:test;IGNORECASE=TRUE
		// SET IGNORECASE { TRUE | FALSE }
		//dbExecSetting(conn, "SET IGNORECASE TRUE", logExtraInfo);

		// Sets the transaction log mode. The values 0, 1, and 2 are supported, 
		// the default is 2. This setting affects all connections.
		// LOG 0 means the transaction log is disabled completely. It is the fastest mode, 
		//       but also the most dangerous: if the process is killed while the database is open
		//       in this mode, the data might be lost. It must only be used if this is not a 
		//       problem, for example when initially loading a database, or when running tests.
		// LOG 1 means the transaction log is enabled, but FileDescriptor.sync is disabled. 
		//       This setting is about half as fast as with LOG 0. This setting is useful if 
		//       no protection against power failure is required, but the data must be protected 
		//       against killing the process.
		// LOG 2 (the default) means the transaction log is enabled, and FileDescriptor.sync 
		//       is called for each checkpoint. This setting is about half as fast as LOG 1. 
		//       Depending on the file system, this will also protect against power failure 
		//       in the majority if cases.
		// Admin rights are required to execute this command, as it affects all connections. 
		// This command commits an open transaction. This setting is not persistent. 
		// This setting can be appended to the database URL: jdbc:h2:test;LOG=0
		// SET LOG int
		//dbExecSetting(conn, "SET LOG 1", logExtraInfo);

		// Sets the maximum size of an in-place LOB object. LOB objects larger that this size 
		// are stored in a separate file, otherwise stored directly in the database (in-place). 
		// The default max size is 1024. This setting has no effect for in-memory databases.
		// Admin rights are required to execute this command, as it affects all connections. 
		// This command commits an open transaction. This setting is persistent.
		// SET MAX_LENGTH_INPLACE_LOB int
		//dbExecSetting(conn, "SET MAX_LENGTH_INPLACE_LOB 4096", logExtraInfo);

		// Set the query timeout of the current session to the given value. The timeout is 
		// in milliseconds. All kinds of statements will throw an exception if they take 
		// longer than the given value. The default timeout is 0, meaning no timeout.
		// This command does not commit a transaction, and rollback does not affect it.
		// SET QUERY_TIMEOUT int
		dbExecSetting(conn, "SET QUERY_TIMEOUT 30000", logExtraInfo);

		// Sets the trace level for file the file or system out stream. Levels are: 0=off, 
		// 1=error, 2=info, 3=debug. The default level is 1 for file and 0 for system out. 
		// To use SLF4J, append ;TRACE_LEVEL_FILE=4 to the database URL when opening the database.
		// This setting is not persistent. Admin rights are required to execute this command, 
		// as it affects all connections. This command does not commit a transaction, 
		// and rollback does not affect it. This setting can be appended to the 
		// database URL: jdbc:h2:test;TRACE_LEVEL_SYSTEM_OUT=3
		// SET { TRACE_LEVEL_FILE | TRACE_LEVEL_SYSTEM_OUT } int
		//dbExecSetting(conn, "", logExtraInfo);

		// 
		//dbExec(conn, "", logExtraInfo);

		// MAX_MEMORY_ROWS
		// The maximum number of rows in a result set that are kept in-memory. 
		// If more rows are read, then the rows are buffered to disk. 
		// The default is 40000 per GB of available RAM.
		// 
		// Admin rights are required to execute this command, as it affects all connections. 
		// This command commits an open transaction in this connection. This setting is persistent. It has no effect for in-memory databases.
		//
		// Example: SET MAX_MEMORY_ROWS 1000
		// 
		dbExecSetting(conn, "SET MAX_MEMORY_ROWS 2500", logExtraInfo);
	}

	private void setAseSpecificSettings(DbxConnection conn)
	throws Exception
	{
		if (conn == null)
			return;

		boolean logExtraInfo = _mainConn == conn;
		
		// for Better/other Performance: lets reconnect, using an alternate URL
		// DYNAMIC_PREPARE=true  or  ENABLE_BULK_LOAD=true
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean aseDynamicPrepare = conf.getBooleanProperty(PROPKEY_BASE+"ase.option.DYNAMIC_PREPARE",  true);
		boolean aseEnableBulkLoad = conf.getBooleanProperty(PROPKEY_BASE+"ase.option.ENABLE_BULK_LOAD", false);

//		// use BULK_LOAD over DYNAMIC_PREPARE
//		if (aseEnableBulkLoad)
//			aseDynamicPrepare = false;

		if (aseDynamicPrepare || aseEnableBulkLoad)
		{
			String localJdbcUrl = conn.getConnProp().getUrl();
			AseUrlHelper urlHelper = AseUrlHelper.parseUrl(localJdbcUrl);
			Map<String, String> urlMap = urlHelper.getUrlOptionsMap();
			if (urlMap == null)
				urlMap = new LinkedHashMap<String, String>();

			boolean change = false;

			// DYNAMIC_PREPARE
			if ( aseDynamicPrepare && ! urlMap.containsKey("DYNAMIC_PREPARE") )
			{
				change = true;
				_logger.info("ASE URL add option: DYNAMIC_PREPARE=true");
				_logger.info("Setting '"+PROPKEY_cachePreparedStatements+"' to 'true' when DYNAMIC_PREPARE is enabled. This means most SQL PreparedStatements (and the server side lightweight stored procedure) in the Writer is reused. And the statements are not closed when pstmnt.close() is called.");
				
				_cachePreparedStatements = true;
				urlMap.put("DYNAMIC_PREPARE", "true");
			}

//			// ENABLE_BULK_LOAD
//			if ( aseEnableBulkLoad && ! urlMap.containsKey("ENABLE_BULK_LOAD") )
//			{
//				change = true;
//				_logger.info("ASE URL add option: ENABLE_BULK_LOAD=true");
//				urlMap.put("ENABLE_BULK_LOAD", "true");
//			}

			if (change)
			{
				urlHelper.setUrlOptionsMap(urlMap);
				localJdbcUrl = urlHelper.getUrl();
				
				_logger.info("Closing ASE connection, Now that I know It's ASE, I want to add specific URL options for load performance.");
				try { conn.close(); } catch(SQLException ignore) {}

				_logger.info("Added some options to the ASE URL. New URL is '"+localJdbcUrl+"'.");
				// Set the new URL for the connection to be made.
				conn.getConnProp().setUrl(localJdbcUrl);

				// do the connect again.
				_logger.info("Re-connecting to ASE after fixing the URL options. New URL is '"+localJdbcUrl+"'.");
//				_mainConn = DriverManager.getConnection(localJdbcUrl, _jdbcUser, _jdbcPasswd);
				conn.reConnect(null);
			}
		}
		
		// now, set various OPTIONS, using SQL statements
		_logger.debug("Connected to ASE, do some specific settings 'set arithabort numeric_truncation off'.");
		dbExecSetting(conn, "set arithabort numeric_truncation off ", logExtraInfo);

		// only 15.0 or above is supported
		long srvVersion = conn.getDbmsVersionNumber();
		if (srvVersion < Ver.ver(15,0))
		{
			String msg = "The PCS storage is ASE Version '"+Ver.versionNumToStr(srvVersion)+"', which is NOT a good idea. This since it can't handle table names longer than 30 characters and the PCS uses longer name. There for I only support ASE 15.0 or higher for the PCS storage. I recommend to use H2 database as the PCS instead (http://www.h2database.com), which is included in the "+Version.getAppName()+" package.";
			_logger.error(msg);

			boolean force = true; // Force close, will set various connections to NULL which is required by: MainFrame.getInstance().action_disconnect()
			boolean dueToDatabaseRollover = false;

			close(force, dueToDatabaseRollover);

			Exception ex =  new Exception(msg);
			{
				MainFrame.getInstance().action_disconnect();
				SwingUtils.showErrorMessage("PersistWriterJdbc", msg, ex);
			}
			throw ex;
		}

		// Get current dbname, if master, then do not allow...
		String dbname = conn.getCatalog();
		if ("master".equals(dbname))
		{
			String msg = "Current ASE Database is '"+dbname+"'. "+Version.getAppName()+" does NOT support storage off Performance Counters in the '"+dbname+"' database.";
			_logger.error(msg);

			boolean force = true; // Force close, will set various connections to NULL which is required by: MainFrame.getInstance().action_disconnect()
			boolean dueToDatabaseRollover = false;

			close(force, dueToDatabaseRollover);

			Exception ex =  new Exception(msg);
			if (DbxTune.hasGui())
			{
				MainFrame.getInstance().action_disconnect();
				SwingUtils.showErrorMessage("PersistWriterJdbc", msg, ex);
			}
			throw ex;
		}

		if (srvVersion < Ver.ver(15,0))
		{
			_logger.debug("Connected to ASE (above 15.0), do some specific settings 'set delayed_commit on'.");
			dbExecSetting(_mainConn, "set delayed_commit on ", logExtraInfo);
		}

		int asePageSize = AseConnectionUtils.getAsePageSize(_mainConn);
		if (asePageSize < 4096)
		{
			_logger.warn("The ASE Servers Page Size is '"+asePageSize+"', to the connected server version '"+Ver.versionNumToStr(srvVersion)+"', which is probably NOT a good idea. The PCS storage will use rows wider than that... which will be reported as errors. However I will let this continue. BUT you can just hope for the best.");
		}
	}


	/**
	 * Get the JDBC Connection used to store data in the database.<br>
	 * NOTE: This might be dangerous to use (especially for long running queries), since it will potentially block the Storage Thread from working (if you use the connection at the same time as the Storge Thread)
	 * @return
	 */
	public DbxConnection getStorageConnection()
	{
		return _mainConn;
	}

	private boolean dbExecSetting(DbxConnection conn, String sql, boolean logInfo)
	throws SQLException
	{
		if (logInfo)
			_logger.info(getDatabaseProductName()+": "+sql);
		return dbExec(conn, sql, true);
	}

	private boolean dbExecNoException(DbxConnection conn, String sql)
	{
		try
		{
			return dbExec(conn, sql, true);
		}
		catch (SQLException e)
		{
			return false;
		}
	}

	private boolean dbExec(DbxConnection conn, String sql)
	throws SQLException
	{
		return dbExec(conn, sql, true);
	}

	private boolean dbExec(DbxConnection conn, String sql, boolean printErrors)
	throws SQLException
	{
		if (_logger.isDebugEnabled())
		{
			_logger.debug("SEND SQL: " + sql);
		}

		try
		{
			Statement s = conn.createStatement();
			s.execute(sql);
			s.close();
		}
		catch(SQLException e)
		{
			if (printErrors)
				_logger.warn("Problems when executing sql statement: "+sql+" SqlException: ErrorCode="+e.getErrorCode()+", SQLState="+e.getSQLState()+", toString="+e.toString());
			throw e;
		}

		return true;
	}
	
	private boolean dbDdlExec(DbxConnection conn, String sql)
	throws SQLException
	{
		if (_logger.isDebugEnabled())
		{
			_logger.debug("SEND DDL SQL: " + sql);
		}

		try
		{
			boolean autoCommitWasChanged = false;

			if (conn.getAutoCommit() != true)
			{
				autoCommitWasChanged = true;
				
				// In ASE the above conn.getAutoCommit() does execute 'select @@tranchained' in the ASE
				// Which causes the conn.setAutoCommit(true) -> set CHAINED off
				// to fail with error: Msg 226, SET CHAINED command not allowed within multi-statement transaction
				//
				// In the JDBC documentation it says:
				// NOTE: If this method is called during a transaction, the transaction is committed.
				//
				// So it should be safe to do a commit here, that is what jConnect should have done...
				conn.commit();

				conn.setAutoCommit(true);
			}

			Statement s = conn.createStatement();
			s.setQueryTimeout(0); // Do not timeout on DDL 

			s.execute(sql);
			s.close();

			if (autoCommitWasChanged)
			{
				conn.setAutoCommit(false);
			}
		}
		catch(SQLException e)
		{
			_logger.warn("Problems when executing DDL sql statement: "+sql);
			isSevereProblem(conn, e);
			throw e;
		}

		return true;
	}

	/** 
	 * Check if table has been created, if not create it.
	 * @param tabId 
	 * @return True if table was created
	 * @throws SQLException
	 */
	private boolean checkAndCreateTable(DbxConnection conn, int tabId)
	throws SQLException
	{
		String tabName = getTableName(conn, tabId, null, false);

		if ( ! isDdlCreated(tabName) )
		{
			// Obtain a DatabaseMetaData object from our current connection        
			DatabaseMetaData dbmd = conn.getMetaData();
	
			ResultSet rs = dbmd.getColumns(null, null, tabName, "%");
			boolean tabExists = rs.next();
			rs.close();
	
			if( tabExists )
			{
				// Check some various things.
				if (tabId == VERSION_INFO)
				{
					// FIXME: check if "VersionString" is the same as Version.getVersionStr()
					//        if not, just throw a WARNING message to the log
				}
			}
			else
			{
				_logger.info("Creating table '" + tabName + "'.");
				getStatistics().incCreateTables();
				
				String sql = getTableDdlString(conn, tabId, null);
				dbDdlExec(conn, sql);

				sql = getIndexDdlString(conn, tabId, null);
				if (sql != null)
				{
					dbDdlExec(conn, sql);
				}				
			}
			
			markDdlAsCreated(tabName);

			return true;
		}
		return false;
	}

	/** 
	 * Check if table has been created, if not create it.
	 * @param tabId 
	 * @return True if table was created
	 * @throws SQLException
	 */
	private boolean checkAndCreateTable(DbxConnection conn, String tabName, ISqlCaptureBroker sqlCapBroker)
	throws SQLException
	{
		if ( ! isDdlCreated(tabName) )
		{
			// Obtain a DatabaseMetaData object from our current connection        
			DatabaseMetaData dbmd = conn.getMetaData();
	
			ResultSet rs = dbmd.getColumns(null, null, tabName, "%");
			boolean tabExists = rs.next();
			rs.close();
	
			if( tabExists )
			{
				_logger.info("Checking table '" + tabName + "'.");
				
				List <String> alterList = sqlCapBroker.checkTableDdl(conn, dbmd, tabName);
				if (alterList != null && !alterList.isEmpty())
				{
					for (String sql : alterList)
					{
						_logger.info("Altering table '" + tabName + "', using sql: " + sql);
						dbDdlExec(conn, sql);
					}
				}
			}
			else
			{
				_logger.info("Creating table '" + tabName + "'.");
				getStatistics().incCreateTables();
				
				String sql = sqlCapBroker.getTableDdlString(conn, dbmd, tabName);
				if (sql != null)
				{
					dbDdlExec(conn, sql);
				}

				List<String> list = sqlCapBroker.getIndexDdlString(conn, dbmd, tabName);
				if (list != null && !list.isEmpty())
				{
					for (String indexDdl : list)
					{
						dbDdlExec(conn, indexDdl);
					}
				}
//				sql = sqlCapBroker.getIndexDdlString(dbmd, tabName);
//				if (sql != null)
//				{
//					dbDdlExec(conn, sql);
//				}				
			}
			
			markDdlAsCreated(tabName);

			return true;
		}
		return false;
	}

	private void insertSessionParam(DbxConnection conn, Timestamp sessionsStartTime, String type, String key, String val)
	throws SQLException
	{
//		String tabName = getTableName(SESSION_PARAMS, null, true);
//
//		// make string a "safe" string, escape all ' (meaning with '')
//		if (key != null) key = key.replaceAll("'", "''");
//		if (val != null) val = val.replaceAll("'", "''");
//
//		// insert into MonSessionParams(SessionStartTime, Type, ParamName, ParamValue) values(...)
//		StringBuffer sbSql = new StringBuffer();
//		sbSql.append(" insert into ").append(tabName);
//		sbSql.append(" values('")
//			.append(sessionsStartTime).append("', '")
//			.append(type)             .append("', '")
//			.append(key)              .append("', '")
//			.append(val)              .append("')");
//
//		dbExec(sbSql.toString());

//		if (val != null && val.length() >= SESSION_PARAMS_VAL_MAXLEN)
//			val = val.substring(0, SESSION_PARAMS_VAL_MAXLEN - 1);

		try
		{
			// Get the SQL statement
			String sql = getTableInsertStr(conn, SESSION_PARAMS, null, true);
//			PreparedStatement pst = conn.prepareStatement( sql );
			PreparedStatement pst = _cachePreparedStatements ? PreparedStatementCache.getPreparedStatement(conn, sql) : conn.prepareStatement(sql);
	
			// Set values
			pst.setString(1, sessionsStartTime.toString());
			pst.setString(2, type);
			pst.setString(3, key);
			pst.setString(4, val);
	
			// EXECUTE
			pst.executeUpdate();
			pst.close();
			getStatistics().incInserts();
		}
		catch (SQLException e)
		{
			_logger.warn("Error in insertSessionParam() writing to Persistent Counter Store. insertSessionParam(sessionsStartTime='"+sessionsStartTime+"', type='"+type+"', key='"+key+"', val='"+val+"')", e);
			if (isSevereProblem(conn, e))
				throw e;
		}
	}

	/**
	 * Called from the {@link PersistentCounterHandler#consume} to start a session if it's needed.
	 * @see PersistentCounterHandler#consume
	 */
	@Override
	public void startSession(PersistContainer cont)
	{
		// Open connection to db
		open(cont);

		DbxConnection conn = _mainConn;
		if (conn == null)
		{
			_logger.error("No database connection to Persistent Storage DB.'");
			return;
		}

//		if (cont._counterObjects == null)
		if (cont.isEmpty())
		{
			_logger.error("Input parameter PersistContainer.isEmpty()==true. Can't continue startSession()...");
			return;
		}

		
		try
		{
			_logger.info("Starting a new Storage Session with the start date '"+cont._sessionStartTime+"'.");

			//
			// FIRST CHECK IF THE TABLE EXISTS, IF NOT CREATE IT
			//
			checkAndCreateTable(conn, VERSION_INFO);
			checkAndCreateTable(conn, SESSIONS);
			checkAndCreateTable(conn, SESSION_PARAMS);
			checkAndCreateTable(conn, SESSION_SAMPLES);
			checkAndCreateTable(conn, SESSION_SAMPLE_SUM);
			checkAndCreateTable(conn, SESSION_SAMPLE_DETAILES);
			checkAndCreateTable(conn, SESSION_MON_TAB_DICT);
			checkAndCreateTable(conn, SESSION_MON_TAB_COL_DICT);
			checkAndCreateTable(conn, SESSION_DBMS_CONFIG);
			checkAndCreateTable(conn, SESSION_DBMS_CONFIG_TEXT);
			checkAndCreateTable(conn, DDL_STORAGE);
//			checkAndCreateTable(conn, SQL_CAPTURE_SQLTEXT);
//			checkAndCreateTable(conn, SQL_CAPTURE_STATEMENTS);
//			checkAndCreateTable(conn, SQL_CAPTURE_PLANS);
			checkAndCreateTable(conn, ALARM_ACTIVE);
			checkAndCreateTable(conn, ALARM_HISTORY);
			// Create tables for SQL Capture... they can have more (or less) tables than SQL_CAPTURE_SQLTEXT, SQL_CAPTURE_STATEMENTS, SQL_CAPTURE_PLANS
			if (PersistentCounterHandler.hasInstance())
			{
				ISqlCaptureBroker sqlCapBroker = PersistentCounterHandler.getInstance().getSqlCaptureBroker();
				if (sqlCapBroker != null)
				{
					List<String> tableNames = sqlCapBroker.getTableNames();
					for (String tabName : tableNames)
					{
						checkAndCreateTable(conn, tabName, sqlCapBroker);
					}
				}
			}
			
			//--------------------------
			// Now fill in some data
//			String tabName = getTableName(VERSION_INFO, null, true);

//			String dbProductName = "unknown";
//			try { dbProductName = CounterController.getInstance().getMonConnection().getDatabaseProductName(); }
//			catch(Throwable t) { _logger.warn("Problems getting Database Product Name from the monitor connection. Caught: "+t); }
			
			StringBuffer sbSql = new StringBuffer();
//			sbSql.append(" insert into ").append(tabName);
			sbSql.append(getTableInsertStr(conn, VERSION_INFO, null, false));
			sbSql.append(" values(");
			sbSql.append("  '").append(cont.getSessionStartTime() ).append("'");
			sbSql.append(", '").append(Version.getAppName()       ).append("'");
			sbSql.append(", '").append(Version.getVersionStr()    ).append("'");
			sbSql.append(", '").append(Version.getBuildStr()      ).append("'");
			sbSql.append(", '").append(Version.getSourceDate()    ).append("'");
			sbSql.append(",  ").append(Version.getSourceRev()     ).append(" ");
//			sbSql.append(", '").append(dbProductName              ).append("'");
			sbSql.append(" )");

			dbExec(conn, sbSql.toString());
			getStatistics().incInserts();


//			tabName = getTableName(SESSIONS, null, true);

			sbSql = new StringBuffer();
//			sbSql.append(" insert into ").append(tabName);
			sbSql.append(getTableInsertStr(conn, SESSIONS, null, false));
			sbSql.append(" values('").append(cont.getSessionStartTime()).append("', '").append(cont.getServerName()).append("', 0, null)");

			dbExec(conn, sbSql.toString());
			getStatistics().incInserts();
			

			_logger.info("Storing CounterModel information in table "+getTableName(conn, SESSION_PARAMS, null, false));
			//--------------------------------
			// LOOP ALL THE CM's and store some information
//			tabName = getTableName(SESSION_PARAMS, null, true);
			Timestamp ts = cont.getSessionStartTime();

			for (CountersModel cm : cont.getCounterObjects())
			{
				String prefix = cm.getName();

				insertSessionParam(conn, ts, "cm", prefix+".name",     cm.getName());
				insertSessionParam(conn, ts, "cm", prefix+".sqlInit",  cm.getSqlInit());
				insertSessionParam(conn, ts, "cm", prefix+".sql",      cm.getSql());
				insertSessionParam(conn, ts, "cm", prefix+".sqlClose", cm.getSqlClose());

				insertSessionParam(conn, ts, "cm", prefix+".pk",       cm.getPk()==null ? null : cm.getPk().toString());
				insertSessionParam(conn, ts, "cm", prefix+".diff",     Arrays.deepToString(cm.getDiffColumns()));
				insertSessionParam(conn, ts, "cm", prefix+".diffDiss", Arrays.deepToString(cm.getDiffDissColumns()));
				insertSessionParam(conn, ts, "cm", prefix+".pct",      Arrays.deepToString(cm.getPctColumns()));

				insertSessionParam(conn, ts, "cm", prefix+".graphNames",Arrays.deepToString(cm.getTrendGraphNames()));
			}
			
			_logger.info("Storing "+Version.getAppName()+" configuration information in table "+getTableName(conn, SESSION_PARAMS, null, false));
			//--------------------------------
			// STORE the configuration file
			Configuration conf;

			// --------- SYSTEM_CONF
			conf = Configuration.getInstance(Configuration.SYSTEM_CONF);
			if (conf != null)
			{
				for (String key : conf.getKeys())
				{
					String val = conf.getPropertyRaw(key);

					insertSessionParam(conn, ts, "system.config", key, val);
				}
			}

			// --------- USER_CONF
			conf = Configuration.getInstance(Configuration.USER_CONF);
			if (conf != null)
			{
				for (String key : conf.getKeys())
				{
					String val = conf.getPropertyRaw(key);

					insertSessionParam(conn, ts, "user.config", key, val);
				}
			}

			// --------- USER_TEMP
			conf = Configuration.getInstance(Configuration.USER_TEMP);
			if (conf != null)
			{
				for (String key : conf.getKeys())
				{
					String val = conf.getPropertyRaw(key);
	
					// Skip some key values... just because they are probably to long...
					if (key.indexOf(".gui.column.header.props") >= 0)
						continue;

					insertSessionParam(conn, ts, "temp.config", key, val);
				}
			}

			// --------- PCS
			conf = Configuration.getInstance(Configuration.PCS);
			if (conf != null)
			{
				for (String key : conf.getKeys())
				{
					String val = conf.getPropertyRaw(key);

					insertSessionParam(conn, ts, "pcs.config", key, val);
				}
			}

			// --------- CombinedConfiguration
			conf = Configuration.getCombinedConfiguration();
			if (conf != null)
			{
				for (String key : conf.getKeys())
				{
					String val = conf.getPropertyRaw(key);

					// Skip some key values... just because they are probably to long...
					if (key.indexOf(".gui.column.header.props") >= 0)
						continue;

					insertSessionParam(conn, ts, "combined.config", key, val);
				}
			}

			// --------- System Properties
			Properties systemProps = System.getProperties();
			for (Object key : systemProps.keySet())
			{
				String val = systemProps.getProperty(key.toString());

				insertSessionParam(conn, ts, "system.properties", key.toString(), val);
			}


			// Storing the MonTablesDictionary(monTables & monTableColumns), 
			// this so we can restore the proper Column ToolTip for this ASE version.
			if (MonTablesDictionaryManager.hasInstance())
				saveMonTablesDictionary(conn, MonTablesDictionaryManager.getInstance(), cont._sessionStartTime);

			// Storing the AseConfig and AseCacheConfig
   			if (DbmsConfigManager.hasInstance())
   				saveDbmsConfig(conn, DbmsConfigManager.getInstance(), cont._sessionStartTime);
    
   			if (DbmsConfigTextManager.hasInstances())
   				saveDbmsConfigText(conn, cont._sessionStartTime);

			// Mark that this session HAS been started.
			_logger.info("Marking the session as STARTED.");
			setSessionStarted(true);
		}
		catch (SQLException e)
		{
			_logger.warn("Error when startSession() writing to Persistent Counter Store.", e);
			isSevereProblem(conn, e);
		}
		
		// Close connection to db
		close();
	}

	public void saveDbmsConfigText(DbxConnection conn, Timestamp sessionStartTime)
	{
		_logger.info("Storing Various DBMS Text Configuration in table "+getTableName(conn, SESSION_DBMS_CONFIG_TEXT, null, false));

		if (conn == null)
		{
			_logger.error("No database connection to Persistent Storage DB.'");
			return;
		}

		if ( ! DbmsConfigTextManager.hasInstances() )
		{
			_logger.info("No DBMS Text configuration was avaibale in saveDbmsConfigText()");
			return;
		}

		StringBuffer sbSql = null;

		try
		{
			// START a transaction
			// This will lower number of IO's to the transaction log
			if (conn.getAutoCommit() == true)
				conn.setAutoCommit(false);

//			//
//			// Do it for all types
//			//
//			for (ConfigType t : AseConfigText.ConfigType.values())
//			{
//				AseConfigText aseConfigText = AseConfigText.getInstance(t);
//
//				_logger.info("Storing DB Server Configuration Text for '"+aseConfigText.getConfigType().toString()+"' in table "+getTableName(SESSION_DBMS_CONFIG_TEXT, null, false));
//
//				sbSql = new StringBuffer();
//				sbSql.append(getTableInsertStr(SESSION_DBMS_CONFIG_TEXT, null, false));
//				sbSql.append(" values('").append(sessionStartTime).append("' \n");
//				sbSql.append("       ,'"+aseConfigText.getConfigType().toString()+"' \n");
//				sbSql.append("       ,").append(safeStr(aseConfigText.getConfig()))  .append(" \n");
//				sbSql.append("       )\n");
//
//				dbExec(sbSql.toString());
//				incInserts();
//			}
			//
			// Do it for all types
			//
			for (IDbmsConfigText dbmsConfigText : DbmsConfigTextManager.getInstanceList())
			{
				_logger.info("Storing DBMS Configuration Text for '"+dbmsConfigText.getName()+"' in table "+getTableName(conn, SESSION_DBMS_CONFIG_TEXT, null, false));

				sbSql = new StringBuffer();
				sbSql.append(getTableInsertStr(conn, SESSION_DBMS_CONFIG_TEXT, null, false));
				sbSql.append(" values('").append(sessionStartTime).append("' \n");
				sbSql.append("       ,'"+dbmsConfigText.getName()+"' \n");
				sbSql.append("       ,").append(safeStr(dbmsConfigText.getConfig()))  .append(" \n");
				sbSql.append("       )\n");

				dbExec(conn, sbSql.toString());
				getStatistics().incInserts();
			}

			// CLOSE the transaction
			conn.commit();
		}
		catch (SQLException e)
		{
			try 
			{
				if (conn.getAutoCommit() == true)
					conn.rollback();
			}
			catch (SQLException e2) {}

			_logger.warn("Error writing to Persistent Counter Store. getErrorCode()="+e.getErrorCode()+", SQL: "+sbSql.toString(), e);
			isSevereProblem(conn, e);
		}
		finally
		{
			try { conn.setAutoCommit(true); }
			catch (SQLException e2) { _logger.error("Problems when setting AutoCommit to true.", e2); }
		}
	}

	public void saveDbmsConfig(DbxConnection conn, IDbmsConfig dbmsCfg, Timestamp sessionStartTime)
	{
		_logger.info("Storing DBMS Server Configuration in table "+getTableName(conn, SESSION_DBMS_CONFIG, null, false));

		if (conn == null)
		{
			_logger.error("No database connection to Persistent Storage DB.'");
			return;
		}
		if (dbmsCfg == null)
		{
			_logger.info("No DBMS configuration was passed to saveDbmsConfig()");
			return;
		}

		StringBuffer sbSql = null;

		try
		{
			// START a transaction
			// This will lower number of IO's to the transaction log
			if (conn.getAutoCommit() == true)
				conn.setAutoCommit(false);

			//----------------------------------------------
			// SESSION_MON_TAB_COL_DICT
//			String tabName = getTableName(SESSION_DBMS_CONFIG, null, true);

			for (int r=0; r<dbmsCfg.getRowCount(); r++)
			{
				sbSql = new StringBuffer();
//				sbSql.append(" insert into ").append(tabName).append(" \n");
				sbSql.append(getTableInsertStr(conn, SESSION_DBMS_CONFIG, null, false));
				sbSql.append(" values('").append(sessionStartTime).append("' \n");

				for (int c=0; c<dbmsCfg.getColumnCount(); c++)
				{
					// Get value
					Object o = dbmsCfg.getValueAt(r, c);

					// if it's a string, surround it with '' or NULL
					if (o instanceof String)
						o = safeStr( (String)o );

					// ASE and ASA does not cope with true/false, so lets use 1 and 0 instead
					if (o instanceof Boolean)
						o = ((Boolean)o).booleanValue() ? "1" : "0";

					sbSql.append("        ,").append(o).append(" \n");
				}
				sbSql.append("       )\n");

				dbExec(conn, sbSql.toString());
				getStatistics().incInserts();
			}


			// CLOSE the transaction
			conn.commit();
		}
		catch (SQLException e)
		{
			try 
			{
				if (conn.getAutoCommit() == true)
					conn.rollback();
			}
			catch (SQLException e2) {}

			_logger.warn("Error writing to Persistent Counter Store. getErrorCode()="+e.getErrorCode()+", SQL: "+sbSql.toString(), e);
			isSevereProblem(conn, e);
		}
		finally
		{
			try { conn.setAutoCommit(true); }
			catch (SQLException e2) { _logger.error("Problems when setting AutoCommit to true.", e2); }
		}
	}



	/**
	 * Return a SQL safe string
	 * <p>
	 * if str is null, "NULL" will be returned<br>
	 * else all "'" chars will be translated into "''" 
	 * AND ' will be appended at the start/end of the string 
	 * @param str
	 * @return  input="it's a string" output="'it''s a string'"
	 */
//	public static String safeStr(String str)
//	{
//		if (str == null)
//			return "NULL";
//
//		StringBuilder sb = new StringBuilder();
//
//		// add ' around the string...
//		// and replace all ' into ''
//		sb.append("'");
//		sb.append(str.replace("'", "''"));
//		sb.append("'");
//		return sb.toString();
//	}
	public static String safeStr(Object obj)
	{
		return DbUtils.safeStr(obj);
	}
	public static String safeStr(Object obj, int maxLen)
	{
		return DbUtils.safeStr(obj, maxLen);
	}

	public void saveMonTablesDictionary(DbxConnection conn, MonTablesDictionary mtd, Timestamp sessionStartTime)
	{
		if ( ! mtd.isSaveMonTablesDictionaryInPcsEnabled())
		{
			_logger.info("Storing monTables & monTableColumns dictionary IS DISABLED by the MonTablesDictionary provider: "+mtd);
			return;
		}

		_logger.info("Storing monTables & monTableColumns dictionary in table "+getTableName(conn, SESSION_MON_TAB_DICT, null, false)+" and "+getTableName(conn, SESSION_MON_TAB_COL_DICT, null, false));
		if (conn == null)
		{
			_logger.error("No database connection to Persistent Storage DB.'");
			return;
		}

		StringBuffer sbSql = null;

		try
		{
			// START a transaction
			// This will lower number of IO's to the transaction log
			if (conn.getAutoCommit() == true)
				conn.setAutoCommit(false);

			//----------------------------------------------
			// SESSION_MON_TAB_DICT
			// SESSION_MON_TAB_COL_DICT
//			String monTabName    = getTableName(SESSION_MON_TAB_DICT,     null, true);
//			String monTabColName = getTableName(SESSION_MON_TAB_COL_DICT, null, true);

			Map<String,MonTableEntry> monTablesDictMap = mtd.getMonTablesDictionaryMap();

			for (MonTableEntry mte : monTablesDictMap.values())
			{
				sbSql = new StringBuffer();
//				sbSql.append(" insert into ").append(monTabName).append(" \n");
				sbSql.append(getTableInsertStr(conn, SESSION_MON_TAB_DICT, null, false));
				sbSql.append(" values('").append(sessionStartTime).append("', \n");
				sbSql.append("         ").append(mte._tableID)    .append(", \n");
				sbSql.append("         ").append(mte._columns)    .append(", \n");
				sbSql.append("         ").append(mte._parameters) .append(", \n");
				sbSql.append("         ").append(mte._indicators) .append(", \n");
				sbSql.append("         ").append(mte._size)       .append(", \n");
				sbSql.append("         ").append(safeStr(mte._tableName))  .append(", \n");
				sbSql.append("         ").append(safeStr(mte._description)).append("\n");
				sbSql.append("       )\n");

				dbExec(conn, sbSql.toString());
				getStatistics().incInserts();

				for (MonTableColumnsEntry mtce : mte._monTableColumns.values())
				{
					sbSql = new StringBuffer();
//					sbSql.append(" insert into ").append(monTabColName).append(" \n");
					sbSql.append(getTableInsertStr(conn, SESSION_MON_TAB_COL_DICT, null, false));
					sbSql.append(" values('").append(sessionStartTime) .append("', \n");
					sbSql.append("         ").append(mtce._tableID)    .append(", \n");
					sbSql.append("         ").append(mtce._columnID)   .append(", \n");
					sbSql.append("         ").append(mtce._typeID)     .append(", \n");
					sbSql.append("         ").append(mtce._precision)  .append(", \n");
					sbSql.append("         ").append(mtce._scale)      .append(", \n");
					sbSql.append("         ").append(mtce._length)     .append(", \n");
					sbSql.append("         ").append(mtce._indicators) .append(", \n");
					sbSql.append("         ").append(safeStr(mtce._tableName))  .append(", \n");
					sbSql.append("         ").append(safeStr(mtce._columnName)) .append(", \n");
					sbSql.append("         ").append(safeStr(mtce._typeName))   .append(", \n");
					sbSql.append("         ").append(safeStr(mtce._description)).append(" \n");
					sbSql.append("       )\n");

					dbExec(conn, sbSql.toString());
					getStatistics().incInserts();
				}
			}


			// CLOSE the transaction
			conn.commit();
		}
		catch (SQLException e)
		{
			try 
			{
				if (conn.getAutoCommit() == true)
					conn.rollback();
			}
			catch (SQLException e2) {}

			_logger.warn("Error writing to Persistent Counter Store. getErrorCode()="+e.getErrorCode()+", SQL: "+sbSql.toString(), e);
			isSevereProblem(conn, e);
		}
		finally
		{
			try { conn.setAutoCommit(true); }
			catch (SQLException e2) { _logger.error("Problems when setting AutoCommit to true.", e2); }
		}
	}

	/**
	 * Save any alarms
	 * 
	 * @param conn                Database connection 
	 * @param cont 
	 * @throws SQLException 
	 */
	private void saveAlarms(DbxConnection conn, PersistContainer cont) 
	throws SQLException
	{
		if (cont == null)
			return;

		if ( ! AlarmWriterToPcsJdbc.hasInstance() )
			return;

		if ( ! AlarmHandler.hasInstance() )
			return;

		Timestamp sessionStartTime  = cont.getSessionStartTime();
		Timestamp sessionSampleTime = cont.getMainSampleTime();
		
		// Delete all ACTIVE alarms
		String sql = "delete from " + getTableName(conn, ALARM_ACTIVE, null, true);
		try
		{
			int count = conn.dbExec(sql);
			getStatistics().incDeletes(count);
		}
		catch (SQLException e)
		{
			_logger.warn("Error deleting Active Alarm(s) to Persistent Counter Store. getErrorCode()="+e.getErrorCode()+", SQL: "+sql, e);
			// throws Exception if it's a severe problem
			isSevereProblem(conn, e);
		}

		// Save ACTIVE Alarms
		List<AlarmEvent> activeAlarms = cont.getAlarmList();
		if ( ! activeAlarms.isEmpty() )
		{
			for (AlarmEvent ae : activeAlarms)
			{
				// Store some info
				StringBuilder sbSql = new StringBuilder();

				try
				{
					sbSql.append(getTableInsertStr(conn, ALARM_ACTIVE, null, false));
					sbSql.append(" values(");
					sbSql.append("  ").append(safeStr( ae.getAlarmClassAbriviated()                                         ,80  )); // "alarmClass"              varchar(80)   null false   - 1
					sbSql.append(", ").append(safeStr( ae.getServiceType()                                                  ,80  )); // "serviceType"             varchar(80)   null false   - 2
					sbSql.append(", ").append(safeStr( ae.getServiceName()                                                  ,30  )); // "serviceName"             varchar(30)   null false   - 3
					sbSql.append(", ").append(safeStr( ae.getServiceInfo()                                                  ,80  )); // "serviceInfo"             varchar(80)   null false   - 4
					sbSql.append(", ").append(safeStr( ae.getExtraInfo()                                                    ,80  )); // "extraInfo"               varchar(80)   null true    - 5
					sbSql.append(", ").append(safeStr( ae.getCategory()                                                     ,20  )); // "category"                varchar(20)   null false   - 6
					sbSql.append(", ").append(safeStr( ae.getSeverity()                                                     ,10  )); // "severity"                varchar(10)   null false   - 7
					sbSql.append(", ").append(safeStr( ae.getState()                                                        ,10  )); // "state"                   varchar(10)   null false   - 8
					sbSql.append(", ").append(safeStr( ae.getReRaiseCount()                                                      )); // "repeatCnt"               int           null false   - 9
					sbSql.append(", ").append(safeStr( ae.getDuration()                                                     ,10  )); // "duration"                varchar(10)   null false   - 10
					sbSql.append(", ").append(safeStr( ae.getCrTime()     == -1 ? null : new Timestamp(ae.getCrTime())           )); // "createTime"              datetime      null false   - 11
					sbSql.append(", ").append(safeStr( ae.getCancelTime() == -1 ? null : new Timestamp(ae.getCancelTime())       )); // "cancelTime"              datetime      null true    - 12
					sbSql.append(", ").append(safeStr( ae.getTimeToLive()                                                        )); // "timeToLive"              int           null true    - 13
					sbSql.append(", ").append(safeStr( ae.getCrossedThreshold() == null ? null : ae.getCrossedThreshold()+"",15  )); // "threshold"               varchar(15)   null true    - 14
					sbSql.append(", ").append(safeStr( ae.getData()                                                         ,160 )); // "data"                    varchar(80)   null true    - 15
					sbSql.append(", ").append(safeStr( ae.getReRaiseData()                                                  ,160 )); // "lastData"                varchar(80)   null true    - 16
					sbSql.append(", ").append(safeStr( ae.getDescription()                                                  ,512 )); // "description"             varchar(512)  null false   - 17
					sbSql.append(", ").append(safeStr( ae.getReRaiseDescription()                                           ,512 )); // "lastDescription"         varchar(512)  null false   - 18
					sbSql.append(", ").append(safeStr( ae.getExtendedDescription()                                               )); // "extendedDescription"     text          null true    - 19
					sbSql.append(", ").append(safeStr( ae.getReRaiseExtendedDescription()                                        )); // "lastExtendedDescription" text          null true    - 20
					sbSql.append(")");

					sql = sbSql.toString();
					conn.dbExec(sql);
					getStatistics().incInserts();
				}
				catch (SQLException e)
				{
					_logger.warn("Error writing Active Alarm(s) to Persistent Counter Store. getErrorCode()="+e.getErrorCode()+", SQL: "+sql, e);
					// throws Exception if it's a severe problem
					isSevereProblem(conn, e);
				}
			}
		}

		// Save HISTORY/EVENTS
		
		
		// Get the list... and start a new list...
//		List<AlarmEventWrapper> alarmList = AlarmWriterToPcsJdbc.getInstance().getList(true);
		List<AlarmEventWrapper> alarmList = AlarmWriterToPcsJdbc.getInstance().getList();
//		AlarmWriterToPcsJdbc.getInstance().clear();
		
		if (alarmList.isEmpty())
			return;

		// Loop all the alarms and insert them into the database...
		for (AlarmEventWrapper aew : alarmList)
		{
			AlarmEvent ae = aew.getAlarmEvent();

			try
			{
				// Get the SQL statement
				sql = getTableInsertStr(conn, ALARM_HISTORY, null, true);
				PreparedStatement pst = _cachePreparedStatements ? PreparedStatementCache.getPreparedStatement(conn, sql) : conn.prepareStatement(sql);
		
//System.out.println("saveAlarms(): SQL="+sql);
				// Set values
				int i=1;
				pst.setTimestamp(i++, sessionStartTime                                                     ); // sessionStartTime        - datetime    , Nullable = false
				pst.setTimestamp(i++, sessionSampleTime                                                    ); // sessionSampleTime       - datetime    , Nullable = false
				pst.setTimestamp(i++, aew.getAddDate()                                                     ); // eventTime	            - datetime    , Nullable = false
				pst.setString   (i++, aew.getAction()                                                      ); // action                  - varchar(15) , Nullable = false
//				pst.setBoolean  (i++, ae.isActive()                                                        ); // isActive                - bit         , Nullable = false
				pst.setString   (i++, ae.getAlarmClassAbriviated()                                         ); // alarmClass              - varchar(80) , Nullable = false
				pst.setString   (i++, ae.getServiceType()                                                  ); // serviceType             - varchar(80) , Nullable = false
				pst.setString   (i++, ae.getServiceName()                                                  ); // serviceName             - varchar(30) , Nullable = false
				pst.setString   (i++, ae.getServiceInfo()                                                  ); // serviceInfo             - varchar(80) , Nullable = false
				pst.setString   (i++, ae.getExtraInfo() == null ? null : ae.getExtraInfo().toString()      ); // extraInfo               - varchar(80) , Nullable = true 
				pst.setString   (i++, ae.getCategory()+""                                                  ); // category                - varchar(20) , Nullable = false
				pst.setString   (i++, ae.getSeverity()+""                                                  ); // severity                - varchar(10) , Nullable = false
				pst.setString   (i++, ae.getState()+""                                                     ); // state                   - varchar(10) , Nullable = false
				pst.setInt      (i++, ae.getReRaiseCount()                                                 ); // repeatCnt               - int         , Nullable = false
				pst.setString   (i++, ae.getDuration()                                                     ); // duration                - varchar(10) , Nullable = false
//				pst.setTimestamp(i++, ae.getCrTimeStr()                                                    ); // createTime              - datetime    , Nullable = false
//				pst.setTimestamp(i++, ae.getCancelTimeStr()                                                ); // cancelTime              - datetime    , Nullable = true 
				pst.setTimestamp(i++, ae.getCrTime()     == -1 ? null : new Timestamp(ae.getCrTime())      ); // createTime              - datetime    , Nullable = false
				pst.setTimestamp(i++, ae.getCancelTime() == -1 ? null : new Timestamp(ae.getCancelTime())  ); // cancelTime              - datetime    , Nullable = true 
//				pst.setInt      (i++, ae.getTimeToLive() == -1 ? null : ae.getTimeToLive()                 ); // timeToLive              - int         , Nullable = true 
				pst.setInt      (i++, ae.getTimeToLive()                                                   ); // timeToLive              - int         , Nullable = true 
				pst.setString   (i++, ae.getCrossedThreshold() == null ? null : ae.getCrossedThreshold()+""); // threshold      - varchar(15) , Nullable = true 
				pst.setString   (i++, ae.getData()        == null ? null : ae.getData().toString()         ); // data                    - varchar(80) , Nullable = true 
				pst.setString   (i++, ae.getReRaiseData() == null ? null : ae.getReRaiseData().toString()  ); // lastData                - varchar(80) , Nullable = true 
				pst.setString   (i++, ae.getDescription()                                                  ); // description             - varchar(512), Nullable = false
				pst.setString   (i++, ae.getReRaiseDescription()                                           ); // lastDescription         - varchar(512), Nullable = false
				pst.setString   (i++, ae.getExtendedDescription()                                          ); // extendedDescription     - text        , Nullable = true 
				pst.setString   (i++, ae.getReRaiseExtendedDescription()                                   ); // lastExtendedDescription - text        , Nullable = true 
		
				// EXECUTE
				pst.executeUpdate();
				pst.close();
				getStatistics().incInserts();
			}
			catch (SQLException ex)
			{
				_logger.warn("Error in saveAlarms() writing to Persistent Counter Store.", ex);
				if (isSevereProblem(conn, ex))
					throw ex;
			}
			catch (RuntimeException rte)
			{
				_logger.warn("Error in saveAlarms() writing to Persistent Counter Store. which is disregarded... Caught a RuntimeException="+rte, rte);
			}
		}
	}



	@Override
	public void saveSample(PersistContainer cont)
	{
		DbxConnection conn = _mainConn;

		if (conn == null)
		{
			_logger.error("No database connection to Persistent Storage DB.'");
			return;
		}
		if (_shutdownWithNoWait)
		{
			_logger.info("Save Sample: Discard entry due to 'ShutdownWithNoWait'.");
			return;
		}

		String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
		String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection
		
		boolean checkForDummyShutdown = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_isSevereProblem_simulateDiskFull, DEFAULT_isSevereProblem_simulateDiskFull);
		if (checkForDummyShutdown)
		{
			isSevereProblem(conn, new SQLException("Dummy Exception..."));
		}
		

		Timestamp sessionStartTime  = cont.getSessionStartTime();
		Timestamp sessionSampleTime = cont.getMainSampleTime();

		
		StringBuffer sbSql = null;

		try
		{
			// START a transaction
			// This will lower number of IO's to the transaction log
			if (conn.getAutoCommit() == true)
				conn.setAutoCommit(false);

			// STATUS, that we are saving right now
			_inSaveSample = true;

			// Save any alarms
			saveAlarms(conn, cont);

			// Save counters
			if ( ! cont.getCounterObjects().isEmpty() )
			{
				//
				// INSERT THE ROW
				//
//				String tabName = getTableName(SESSION_SAMPLES, null, true);

				sbSql = new StringBuffer();
//				sbSql.append(" insert into ").append(tabName);
				sbSql.append(getTableInsertStr(conn, SESSION_SAMPLES, null, false));
				sbSql.append(" values('").append(sessionStartTime).append("', '").append(sessionSampleTime).append("')");

				dbExec(conn, sbSql.toString());
				getStatistics().incInserts();

				// Increment the "counter" column and set LastSampleTime in the SESSIONS table
				String tabName = getTableName(conn, SESSIONS, null, true);
				sbSql = new StringBuffer();
				sbSql.append(" update ").append(tabName);
				sbSql.append("    set ").append(lq).append("NumOfSamples")    .append(rq).append(" = ").append(lq).append("NumOfSamples").append(rq).append(" + 1,");
				sbSql.append("        ").append(lq).append("LastSampleTime")  .append(rq).append(" = '").append(sessionSampleTime).append("'");
				sbSql.append("  where ").append(lq).append("SessionStartTime").append(rq).append(" = '").append(sessionStartTime).append("'");

				dbExec(conn, sbSql.toString());
				getStatistics().incUpdates();

				//--------------------------------------
				// COUNTERS
				//--------------------------------------
				for (CountersModel cm : cont.getCounterObjects())
				{
					saveCounterData(conn, cm, sessionStartTime, sessionSampleTime);
				}
			}
				
			// CLOSE the transaction
			conn.commit();
		}
		catch (SQLException e)
		{
			try 
			{
				if (conn.getAutoCommit() == true)
					conn.rollback();
			}
			catch (SQLException e2) {}

			isSevereProblem(conn, e);
			_logger.warn("Error writing to Persistent Counter Store. getErrorCode()="+e.getErrorCode()+", SQL: "+sbSql.toString(), e);
		}
		finally
		{
			try { conn.setAutoCommit(true); }
			catch (SQLException e2) { _logger.error("Problems when setting AutoCommit to true.", e2); }

			_inSaveSample = false;
		}
	}

	private void saveDdl(DbxConnection conn, int type, CountersModel cm)
	throws SQLException
	{
		ResultSet rs = null;
		String tabName;

		// Obtain a DatabaseMetaData object from our current connection
		DatabaseMetaData dbmd = conn.getMetaData();


		tabName = getTableName(conn, type, cm, false);

		ArrayList<String> existingCols = new ArrayList<String>();
		rs = dbmd.getColumns(null, null, tabName, "%");
		while (rs.next())
			existingCols.add(rs.getString("COLUMN_NAME"));
		boolean tabExists = existingCols.size() > 0;
		rs.close();

		// If table exists, check for missing columns, and add missing ones
		// If table doesn't exists, create the table
		if( tabExists )
		{
			ArrayList<String> missingCols = new ArrayList<String>();

			// Get the CM's columns
			ResultSetMetaData rsmd = cm.getResultSetMetaData();
			
			if ( rsmd == null )
				throw new SQLException("ResultSetMetaData for CM '"+cm.getName()+"' was null.");
			if ( rsmd.getColumnCount() == 0 )
				throw new SQLException("NO Columns was found for CM '"+cm.getName()+"'.");

			// Loop and add missing cols to missingCols array
			int cols = rsmd.getColumnCount();
			for (int c=1; c<=cols; c++) 
			{
				String colName = rsmd.getColumnLabel(c);
				if ( ! existingCols.contains(colName) )
					missingCols.add(colName);
			}

			// Well the storage table are missing some columns
			// Lets alter the table and add them
			if (missingCols.size() > 0)
			{
				_logger.info("Persistent Counter DB: Altering table "+StringUtil.left("'"+tabName+"'", 37, true)+" for CounterModel '" + cm.getName() + "' The following "+missingCols.size()+" column(s) where missing '"+missingCols+"', so lets add them.");

				List<String> alterTableDdlList = getAlterTableDdlString(conn, tabName, missingCols, type, cm);
				for (String sqlAlterTable : alterTableDdlList)
				{
					_logger.info("Persistent Counter DB: Altering table "+StringUtil.left("'"+tabName+"'", 37, true)+" for CounterModel '" + cm.getName() + "'. Executing SQL: "+sqlAlterTable);

					dbDdlExec(conn, sqlAlterTable);

					getStatistics().incAlterTables();
				}
			}
		}
		else
		{
			_logger.info("Persistent Counter DB: Creating table "+StringUtil.left("'"+tabName+"'", 37, true)+" for CounterModel '" + cm.getName() + "'.");

			String sqlTable = getTableDdlString(conn, type, cm);
			String sqlIndex = getIndexDdlString(conn, type, cm);

			dbDdlExec(conn, sqlTable);
			dbDdlExec(conn, sqlIndex);
			
			getStatistics().incCreateTables();
		}
		
	}
	
	@Override
	public boolean saveDdl(CountersModel cm)
  	{
		DbxConnection conn = _mainConn;

		if (cm == null)
		{
			_logger.debug("saveDdl: cm == null.");
			return false;
		}

		ResultSetMetaData rsmd = cm.getResultSetMetaData();
		if (rsmd == null)
		{
			_logger.debug("saveDdl: rsmd == null.");
			return false;
		}

		// Write SQL Table definition
		if (conn == null)
		{
			_logger.debug("saveDdl: conn == null.");
			return false;
		}

		
		//------------------------------
		// Write SQL table definition file
		//------------------------------
		try
		{
			saveDdl(conn, ABS, cm);
			saveDdl(conn, DIFF, cm);
			saveDdl(conn, RATE, cm);
		}
		catch (SQLException e)
		{
			_logger.warn("SQLException, Error writing DDL to Persistent Counter DB.", e);
			isSevereProblem(conn, e);

			throw new RuntimeException("SQLException, Error writing DDL to Persistent Counter DB. Caught: "+e);
			//return false;
		}

		return true;
  	} // end: method

	
	/**
	 * Save the counters in the database
	 * 
	 * @param cm
	 * @param sessionStartTime
	 * @param sessionSampleTime
	 */
	private void saveCounterData(DbxConnection conn, CountersModel cm, Timestamp sessionStartTime, Timestamp sessionSampleTime)
	{
		if (cm == null)
		{
			_logger.debug("saveCounterData: cm == null.");
			return;
		}

//		if (cm instanceof CountersModelAppend) 
//			return;

//		if ( ! cm.hasDiffData() && ( cm.isPersistCountersDiffEnabled() || cm.isPersistCountersRateEnabled() ) )
//		{
//			_logger.info("No diffData is available, skipping writing Counters for name='"+cm.getName()+"'.");
//			return;
//		}

		if (_shutdownWithNoWait)
		{
			_logger.info("SaveCounterData: Discard entry due to 'ShutdownWithNoWait'.");
			return;
		}

		_logger.debug("Persisting Counters for CounterModel='"+cm.getName()+"'.");

		int counterType = 0;
		int absRows     = 0;
		int diffRows    = 0;
		int rateRows    = 0;
		if (cm.hasValidSampleData())
		{
    		if (cm.hasAbsData()  && cm.isPersistCountersAbsEnabled())  {counterType += 1; absRows  = save(conn, cm, ABS,  sessionStartTime, sessionSampleTime);}
    		if (cm.hasDiffData() && cm.isPersistCountersDiffEnabled()) {counterType += 2; diffRows = save(conn, cm, DIFF, sessionStartTime, sessionSampleTime);}
    		if (cm.hasRateData() && cm.isPersistCountersRateEnabled()) {counterType += 4; rateRows = save(conn, cm, RATE, sessionStartTime, sessionSampleTime);}
//    		if (cm.hasAbsData()  && cm.getAbsColumnCount()  > 0 && cm.getAbsRowCount()  > 0 && cm.isPersistCountersAbsEnabled())  {counterType += 1; absRows  = save(conn, cm, ABS,  sessionStartTime, sessionSampleTime);}
//    		if (cm.hasDiffData() && cm.getDiffColumnCount() > 0 && cm.getDiffRowCount() > 0 && cm.isPersistCountersDiffEnabled()) {counterType += 2; diffRows = save(conn, cm, DIFF, sessionStartTime, sessionSampleTime);}
//    		if (cm.hasRateData() && cm.getRateColumnCount() > 0 && cm.getRateRowCount() > 0 && cm.isPersistCountersRateEnabled()) {counterType += 4; rateRows = save(conn, cm, RATE, sessionStartTime, sessionSampleTime);}
		}
		
		if (_shutdownWithNoWait)
		{
			_logger.info("SaveCounterData:1: Discard entry due to 'ShutdownWithNoWait'.");
			return;
		}

		int graphCount = 0;
		if (cm.hasValidSampleData())
		{
			Map<String,TrendGraphDataPoint> tgdMap = cm.getTrendGraphData();
			if (tgdMap != null)
			{
				for (Map.Entry<String,TrendGraphDataPoint> entry : tgdMap.entrySet()) 
				{
				//	String              key  = entry.getKey();
					TrendGraphDataPoint tgdp = entry.getValue();

					saveGraphData(conn, cm, tgdp, sessionStartTime, sessionSampleTime);
					graphCount++;
				}

			}
		}

		if (_shutdownWithNoWait)
		{
			_logger.info("saveCounterData:2: Discard entry due to 'ShutdownWithNoWait'.");
			return;
		}

		// Store some info
		String sql = "";

//		String tabName = getTableName(SESSION_SAMPLE_DETAILES, null, true);

		try
		{
			StringBuilder sbSql = new StringBuilder();
			
//			sbSql.append(" insert into ").append(tabName);
			sbSql.append(getTableInsertStr(conn, SESSION_SAMPLE_DETAILES, null, false));
			sbSql.append(" values('").append(sessionStartTime).append("'");
			sbSql.append(", '").append(sessionSampleTime).append("'");
			sbSql.append(", '").append(cm.getName()).append("'");
			sbSql.append(", ") .append(counterType);
			sbSql.append(", ") .append(graphCount);
			sbSql.append(", ") .append(absRows);
			sbSql.append(", ") .append(diffRows);
			sbSql.append(", ") .append(rateRows);
			sbSql.append(", ") .append(cm.getSqlRefreshTime());
			sbSql.append(", ") .append(cm.getGuiRefreshTime());
			sbSql.append(", ") .append(cm.getLcRefreshTime());
			sbSql.append(", ") .append(cm.hasNonConfiguredMonitoringHappened() ? 1 : 0);
			sbSql.append(", ") .append(safeStr(cm.getNonConfiguredMonitoringMissingParams()));
			sbSql.append(", ") .append(safeStr(cm.getNonConfiguredMonitoringMessage(false)));
			sbSql.append(", ") .append(cm.isCountersCleared() ? 1 : 0);
			sbSql.append(", ") .append(cm.hasValidSampleData() ? 1 : 0);
			sbSql.append(", ") .append(cm.getSampleException() == null ? "NULL" : safeStr(cm.getSampleException().toString()));
			sbSql.append(", ") .append(safeStr(StringUtil.exceptionToString(cm.getSampleException()))); // FIXME: maybe we can store it as a serialized object... and deserialize it on retrival 
			sbSql.append(")");

			sql = sbSql.toString();
			
			dbExec(conn, sql);
			getStatistics().incInserts();
		}
		catch (SQLException e)
		{
			_logger.warn("Error writing to Persistent Counter Store. getErrorCode()="+e.getErrorCode()+", SQL: "+sql, e);
			isSevereProblem(conn, e);
		}


		if (_shutdownWithNoWait)
		{
			_logger.info("saveCounterData:3: Discard entry due to 'ShutdownWithNoWait'.");
			return;
		}

		// SUMMARY INFO for the whole session
		String tabName = getTableName(conn, SESSION_SAMPLE_SUM, null, true);

		StringBuilder sbSql = new StringBuilder();
		sbSql.append(" update ").append(tabName);
		sbSql.append(" set [absSamples]  = [absSamples]  + ").append( (absRows  > 0 ? 1 : 0) ).append(", ");
		sbSql.append("     [diffSamples] = [diffSamples] + ").append( (diffRows > 0 ? 1 : 0) ).append(", ");
		sbSql.append("     [rateSamples] = [rateSamples] + ").append( (rateRows > 0 ? 1 : 0) ).append("");
		sbSql.append(" where [SessionStartTime] = '").append(sessionStartTime).append("'");
		sbSql.append("   and [CmName] = '").append(cm.getName()).append("'");

		sql = sbSql.toString();

		// replace all '[' and ']' into DBMS Vendor Specific Chars
		sql = conn.quotifySqlString(sql);

		try
		{
			Statement stmnt = conn.createStatement();
			int updCount = stmnt.executeUpdate(sql);
			
			if (updCount == 0)
			{
				StringBuilder sbSql2 = new StringBuilder();

//				sbSql2.append(" insert into ").append(tabName);
				sbSql2.append(getTableInsertStr(conn, SESSION_SAMPLE_SUM, null, false));
				sbSql2.append(" values('").append(sessionStartTime).append("'");
				sbSql2.append(", '").append(cm.getName()).append("', 1, 1, 1)");

				sql = sbSql2.toString();

				// replace all '[' and ']' into DBMS Vendor Specific Chars
				sql = conn.quotifySqlString(sql);
				
				updCount = stmnt.executeUpdate(sql);
				getStatistics().incInserts();
			}
			else
			{
				getStatistics().incUpdates();
			}
		}
		catch (SQLException e)
		{
			_logger.warn("Error writing to Persistent Counter Store. getErrorCode()="+e.getErrorCode()+", SQL: "+sql, e);
			isSevereProblem(conn, e);
		}
	}

	private int save(DbxConnection conn, CountersModel cm, int whatData, Timestamp sessionStartTime, Timestamp sessionSampleTime)
	{
		if (_shutdownWithNoWait)
		{
			_logger.info("saveCm: Discard entry due to 'ShutdownWithNoWait'.");
			return -1;
		}
		if (conn == null)
		{
			//_logger.error("No database connection to Persistent Storage DB.'");
			return -1;
		}
		
		Object       colObj    = null;
//		StringBuffer sqlSb     = new StringBuffer();
//		StringBuffer rowSb     = new StringBuffer();

		int    cmWhatData    = -99;
		String cmWhatDataStr = "UNKNOWN";
		if      (whatData == ABS)  { cmWhatData = CountersModel.DATA_ABS;  cmWhatDataStr = "ABS"; }
		else if (whatData == DIFF) { cmWhatData = CountersModel.DATA_DIFF; cmWhatDataStr = "DIFF"; }
		else if (whatData == RATE) { cmWhatData = CountersModel.DATA_RATE; cmWhatDataStr = "RATE"; }
		else
		{
			_logger.error("Type of data is unknown, only 'ABS', 'DIFF' and 'RATE' is handled.");
			return -1;
		}
		List<List<Object>> rows = cm.getDataCollection(cmWhatData);
		List<String>       cols = cm.getColNames(cmWhatData);

		// If it's an Append only CM, then grab just the LAST available entries (if any)
		if (cm instanceof CountersModelAppend) 
		{
			rows = ((CountersModelAppend)cm).getDataCollectionForLastRefresh();
			cols = ((CountersModelAppend)cm).getColNames(cmWhatData);
		}
		
		if (rows == null || cols == null)
		{
			_logger.error("Rows or Columns can't be null. rows='"+rows+"', cols='"+cols+"'");
			return -1;
		}

		String tabName = getTableName(conn, whatData, cm, false);
//		String tabName = cm.getName();
//		if      (whatData == CountersModel.DATA_ABS)  tabName += "_abs";
//		else if (whatData == CountersModel.DATA_DIFF) tabName += "_diff";
//		else if (whatData == CountersModel.DATA_RATE) tabName += "_rate";
//		else
//		{
//			_logger.error("Type of data is unknown, only 'ABS', 'DIFF' and 'RATE' is handled.");
//			return -1;
//		}

		int rowsCount = rows.size();
		int colsCount = cols.size();
		
//System.out.println("Counter '"+cmWhatDataStr+"' data for CM '"+cm.getName()+"'. (rowsCount="+rowsCount+", colsCount="+colsCount+") cols="+cols);
		if (rowsCount == 0 || colsCount == 0)
		{
			_logger.debug("Skipping Storing Counter '"+cmWhatDataStr+"' data for CM '"+cm.getName()+"'. Rowcount or column count is 0 (rowsCount="+rowsCount+", colsCount="+colsCount+")");
			return 0;
		}
		
		// First BUILD up SQL statement used for the insert
//		sqlSb.append("insert into ").append(qic).append(tabName).append(qic);
//		sqlSb.append(" values(?, ?, ?, ?");
//		for (int c=0; c<colsCount; c++)
//			sqlSb.append(", ?");
//		sqlSb.append(")");

		ResultSetMetaData rsmd = null;
		try
		{
			rsmd = cm.getResultSetMetaData();
			
			if ( rsmd != null && rsmd.getColumnCount() == 0 )
				rsmd = null;
		}
		catch (SQLException ignore) { /*ignore*/ }
		
		// TODO: Instead of getting the CM.getResultSetMetaData() we might want to use
//		TableInfo storageTableInfo = new TableInfo(); // note TableInfo needs to be created
//		try
//		{
//			ResultSet colRs = conn.getMetaData().getColumns(null, null, tabName, "%");
//			while (colRs.next())
//			{
//				String colName = colRs.getString(4); // COLUMN_NAME 
//				int    colType = colRs.getInt   (5); // DATA_TYPE:  SQL type from java.sql.Types 
//				int    colSize = colRs.getInt   (7); // COLUMN_SIZE
//				
//				tableInfo.add(colName, colType, colSize);
//			}
//			colRs.close();
//		}
//		catch (SQLException ignore) { /*ignore*/ }

		String sql = "";
		try
		{
			sql = getTableInsertStr(conn, whatData, cm, true, cols);
//			PreparedStatement pstmt = conn.prepareStatement(sql);
//			PreparedStatement pstmt = PreparedStatementCache.getPreparedStatement(conn, sql);
			PreparedStatement pstmt = _cachePreparedStatements ? PreparedStatementCache.getPreparedStatement(conn, sql) : conn.prepareStatement(sql);

			// Loop all rows, and ADD them to the Prepared Statement
			for (int r=0; r<rowsCount; r++)
			{
				if (_shutdownWithNoWait)
				{
					_logger.info("saveCm:inRowLoop: Discard entry due to 'ShutdownWithNoWait'.");
					return -1;
				}

				int col = 1;
				// Add sessionStartTime as the first column
//				pstmt.setTimestamp(col++, sessionStartTime);
				pstmt.setString(col++, sessionStartTime.toString());

				// Add sessionSampleTime as the first column
//				pstmt.setTimestamp(col++, sessionSampleTime);
				pstmt.setString(col++, sessionSampleTime.toString());

				// When THIS sample was taken
				// probably the same time as parentSampleTime, but it can vary some milliseconds or so
//				pstmt.setTimestamp(col++, cm.getTimestamp());
				pstmt.setString(col++, cm.getTimestamp().toString());

				// How long the sample was for, in Milliseconds
				pstmt.setInt(col++, cm.getLastSampleInterval());

				// How long the sample was for, in Milliseconds
				pstmt.setInt(col++, cm.isNewDeltaOrRateRow(r) ? 1 : 0);

//if (r==0) System.out.println("PersistWriterJdbc: save(): tabName="+StringUtil.left(tabName, 30)+", getLastSampleInterval()="+cm.getLastSampleInterval()+", getSampleInterval()="+cm.getSampleInterval());
				
				// loop all columns
				for (int c=0; c<colsCount; c++)
				{
					colObj =  rows.get(r).get(c);

					// Timestamp is stored with appending nanoseconds etc in a strange format
					// if you are using setObject() so use setString() instead...
					if (colObj != null && colObj instanceof Timestamp)
					{
						// Also try to parse the date to see if it's ok...
						// some timestamp seems to be "corrupt"...
						Timestamp ts = (Timestamp) colObj;
						String dateStr = colObj.toString();

						Calendar cal = Calendar.getInstance();
						cal.setTime(ts);
						int year = cal.get(Calendar.YEAR);
						if (year > 9999)
						{
							String colName = cm.getColumnName(c); // cm colname starts at 0
							_logger.warn("Date problems for table '"+tabName+"', column '"+colName+"', Timestamp value '"+dateStr+"', Year seems to be out of whack, replacing this with NULL.");
							pstmt.setString(col++, null);
						}
						else
						{
							pstmt.setString(col++, dateStr);
						}
					}
					// STRING values, check if we need to truncate trailing/overflowing space. 
					else if (colObj != null && colObj instanceof String)
					{
						String str = (String)colObj;

						// TODO: instead of the cm.rsmd we can use "storageTableInfo"
						// this needs to be implemented first

						// if str length is longer than column length, truncate the value...
						if (rsmd != null)
						{
							// Should we check for storage type as well???, lets skip this for now...
//							String typeDatatype = rsmd.getColumnTypeName( c + 1 );
//							if ( type.equals("char") || type.equals("varchar") )
//							int allowedLength = rsmd.getPrecision( c + 1 ); // seems to be zero for strings

							// NOTE: column in JDBC starts at 1, not 0
//							int allowedLength = rsmd.getColumnDisplaySize( c + 1 ); // getColumnDisplaySize() is used when creating the tables, so this should hopefully work
							int allowedLength = Math.max(rsmd.getColumnDisplaySize(c+1), rsmd.getPrecision(c+1)); // getColumnDisplaySize() is used when creating the tables, so this should hopefully work
							int jdbcDataType  = rsmd.getColumnType(c + 1);
//							if (jdbcDataType == Types.BINARY || jdbcDataType == Types.VARBINARY)
//								allowedLength += 2; // binary may need the extra 2 chars if it's prefixed with a 0x

							// If a hex string starts with 0x chop that off, H2 doesn't seem to like it
							if (str != null && (jdbcDataType == Types.BINARY || jdbcDataType == Types.VARBINARY))
							{
								if (str.startsWith("0x"))
									str = str.substring("0x".length());
							}

							if ( allowedLength > 0  &&  str != null  &&  str.length() > allowedLength )
							{
								int dataLength = str.length();
								String colName = cols.get(c);

								// Add '...' at the end if it's a long string, or simply "chop it of" if it's a very short string.
								String truncStr = "";
								if (allowedLength <= 3 || (jdbcDataType == Types.BINARY || jdbcDataType == Types.VARBINARY) ) // Binary data types can contain '...'
									truncStr = str.substring(0, allowedLength);
								else
									truncStr = str.substring(0, allowedLength - 3) + "...";

								_logger.info("save(): Truncating a Overflowing String value. table='"+tabName+"', column='"+colName+"', allowedLength="+allowedLength+", dataLength="+dataLength+", newStr["+truncStr.length()+"]='"+truncStr+"', originStr["+str.length()+"]='"+str+"'.");

								str = truncStr;
							}
						}

						pstmt.setString(col++, str);
					}
					else
						pstmt.setObject(col++, colObj);
				}
				
				// ADD the row to the BATCH
				pstmt.addBatch();
				getStatistics().incInserts();
			} // end: loop rows
	
			if (_shutdownWithNoWait)
			{
				_logger.info("saveCm:exexuteBatch: Discard entry due to 'ShutdownWithNoWait'.");
				return -1;
			}

			pstmt.executeBatch();
			pstmt.close();

			return rowsCount;
		}
		catch (SQLException e)
		{
			_logger.warn("Error writing to Persistent Counter Store. to table name '"+tabName+"'. getErrorCode()="+e.getErrorCode()+" SQL="+sql, e);
			isSevereProblem(conn, e);
			return -1;
		}
	}

	private void saveGraphData(DbxConnection conn, CountersModel cm, TrendGraphDataPoint tgdp, Timestamp sessionStartTime, Timestamp sessionSampleTime)
	{
		if (cm   == null) throw new IllegalArgumentException("The passed CM can't be null");
		if (tgdp == null) throw new IllegalArgumentException("The passed TGDP can't be null");

		String tabName = getTableName(conn, cm, tgdp, false);

		if ( ! tgdp.hasData() )
		{
			_logger.debug("The graph '"+tgdp.getName()+"' has NO DATA for this sample time, so write will be skipped. TrendGraphDataPoint="+tgdp);
			return;
		}
		if (_shutdownWithNoWait)
		{
			_logger.info("saveGraphData: Discard entry due to 'ShutdownWithNoWait'.");
			return;
		}

		StringBuilder sb = new StringBuilder();
//		sb.append("insert into ").append(qic).append(tabName).append(qic);
		String tabInsStr = getTableInsertStr(conn, cm, tgdp, false);
		sb.append(tabInsStr);
		sb.append(" values(");


		// Add sessionStartTime as the first column
		sb.append("'").append(sessionStartTime).append("', ");

		// Add sessionSampleTime as the first column
		sb.append("'").append(sessionSampleTime).append("', ");

		sb.append("'").append(tgdp.getDate()).append("', ");

		// loop all data
		Double[] dataArr  = tgdp.getData();
		String[] labelArr = tgdp.getLabel();
		if (dataArr  == null) throw new IllegalArgumentException("The CM '"+cm.getName()+"', graph '"+tgdp.getName()+"' has a null pointer for it's DATA array.");
		if (labelArr == null) throw new IllegalArgumentException("The CM '"+cm.getName()+"', graph '"+tgdp.getName()+"' has a null pointer for it's LABEL array.");
		for (int d=0; d<dataArr.length; d++)
		{
			Double data  = dataArr[d];
			String label = null;
			if (d < labelArr.length)
				label = labelArr[d];

			if (label == null)
				sb.append("NULL, ");
			else
			{
				// Should we check the length of label and truncate it if above ### (100 for the moment)
				// Should we do safeStr() to escape single quote as well
				sb.append("'").append(label).append("', "); 
			}

			if (data == null)
				sb.append("NULL");
			else
				sb.append(data);

			// No colSep on last column
			if ( (d+1) == dataArr.length )
				sb.append(")");
			else
				sb.append(", ");
		}
		//--------------------
		// Send the SQL to the database.
		//--------------------

		// CHECK/Create table
		try
		{
			if ( ! isDdlCreated(tabName) )
				saveGraphDataDdl(conn, tabName, tgdp);
			markDdlAsCreated(tabName);
		}
		catch (SQLException e)
		{
			isSevereProblem(conn, e);
			_logger.info("Problems writing Graph '"+tgdp.getName()+"' information to table '"+tabName+"', Problems when creating the table or checked if it existed. Caught: "+e);
		}

		// Add rows...
		try
		{
			dbExec(conn, sb.toString(), false);
			getStatistics().incInserts();
		}
		catch (SQLException e)
		{
			_logger.info("Problems writing Graph '"+tgdp.getName()+"' information to table '"+tabName+"', This probably happens if series has been added to the graph, I will checking/create/alter the table and try again.");
			try
			{
				// we probably need to alter the table...
				saveGraphDataDdl(conn, tabName, tgdp);
				dbExec(conn, sb.toString());
				getStatistics().incInserts();
			}
			catch (SQLException e2)
			{
				isSevereProblem(conn, e2);
				_logger.warn("Error writing to Persistent Counter Store. getErrorCode()="+e2.getErrorCode()+", getSQLState()="+e2.getSQLState()+", ex.toString='"+e2.toString()+"', SQL: "+sb.toString(), e2);
			}
		}
	}

	private void saveGraphDataDdl(DbxConnection conn, String tabName, TrendGraphDataPoint tgdp)
	throws SQLException
	{
		ResultSet rs = null;

		// Obtain a DatabaseMetaData object from our current connection
		DatabaseMetaData dbmd = conn.getMetaData();

		// If NOT in autocommit (it means that we are in a transaction)
		// Creating tables and some other operations is NOT allowed in a transaction
		// So:
		//  - commit the transaction
		//  - do the work (create table)
		//  - start a new transaction again
		// Yes, this is a bit uggly, but lets do it anyway...
		boolean inTransaction = (conn.getAutoCommit() == false);
		try
		{
			if (inTransaction)
			{
				_logger.debug("Looks like we are in a transaction, temporary committing it, then create the table and start a new transaction.");
				conn.commit();
				conn.setAutoCommit(true);
			}
			
			rs = dbmd.getColumns(null, null, tabName, "%");
			boolean tabExists = rs.next();
			rs.close();

			if( ! tabExists )
			{
				_logger.info("Persistent Counter DB: Creating table "+StringUtil.left("'"+tabName+"'", 37, true)+" for CounterModel graph '" + tgdp.getName() + "'.");

				String sqlTable = getGraphTableDdlString(conn, tabName, tgdp);
				String sqlIndex = getGraphIndexDdlString(conn, tabName, tgdp);

				dbDdlExec(conn, sqlTable);
				dbDdlExec(conn, sqlIndex);
				
				getStatistics().incCreateTables();
			}
			else // Check if we need to add any new columns
			{
//				String sqlAlterTable = getGraphAlterTableDdlString(conn, tabName, tgdp);
//				if ( ! sqlAlterTable.trim().equals("") )
//				{
//					_logger.info("Persistent Counter DB: Altering table '"+tabName+"' for CounterModel graph '" + tgdp.getName() + "'.");
//
//					dbDdlExec(sqlAlterTable);
//					incAlterTables();
//				}
				List<String> sqlAlterList = getGraphAlterTableDdlString(conn, tabName, tgdp);
				if ( ! sqlAlterList.isEmpty() )
				{
					_logger.info("Persistent Counter DB: Altering table '"+tabName+"' for CounterModel graph '" + tgdp.getName() + "'.");

					for (String sqlAlterTable : sqlAlterList)
						dbDdlExec(conn, sqlAlterTable);

					getStatistics().incAlterTables();
				}
			}
		}
		catch (SQLException e)
		{
			throw e;
		}
		finally
		{
			// Start the transaction again
			if (inTransaction)
			{
				_logger.debug("Looks like we are in a transaction. Done with creating the table, so starting a transaction again.");
				conn.setAutoCommit(false);
			}
		}
	}



	//---------------------------------------------
	// BEGIN: DDL Lookup
	//---------------------------------------------
	/** what entries has already been stored in the back-end, this so we can sort out fast if wee need to store or not */
	// TODO: maybe as HashMap<String(dbname), HasSet<String(objectName)>>, which would be faster.
	private Set<String> _ddlDetailsCache = Collections.synchronizedSet(new HashSet<String>());

	/** check if this DDL is stored in the DDL storage, implementer should hold all stored DDL's in a cache */
	@Override
	public boolean isDdlDetailsStored(String dbname, String objectName)
	{
		String key = dbname + ":" + objectName;
		return _ddlDetailsCache.contains(key);
	}

	@Override
	public void markDdlDetailsAsStored(String dbname, String objectName)
	{
		String key = dbname + ":" + objectName;
		_ddlDetailsCache.add(key);
		
//		System.out.println("markDdlDetailsAsStored(): "+dbname+"."+objectName+"    TOTAL MARKED SIZE IS NOW: "+_ddlDetailsCache.size());

//System.out.println("========== markDdlDetailsAsStored() ===============================");
//System.out.println("dbname                        objectname");
//System.out.println("----------------------------- -------------------------------------");
//for (String str : _ddlDetailsCache)
//{
//	String[] sa = str.split(":");
//	System.out.println(StringUtil.left(sa[0], 30) + " " + sa[1]);
//}
//System.out.println("-------------------------------------------------------------------");
//System.out.println("Last added record: "+dbname+"."+objectName);
//System.out.println("TOTAL SIZE IS NOW: "+_ddlDetailsCache.size());
//System.out.println("-------------------------------------------------------------------");
	}

	@Override
	public void clearDdlDetailesCache()
	{
		_ddlDetailsCache.clear();
	}

	@Override
	public void populateDdlDetailesCache()
	{
		DbxConnection conn = _mainConn;
		if (conn == null)
		{
			_logger.info("populateDdlDetailesCache(): No database connection to Persistent Storage DB.");
			return;
		}

		String tabName = getTableName(conn, DDL_STORAGE, null, false);

		String sql = 
			" select [dbname], [objectName] " +
			" from " + tabName;

		// replace all '[' and ']' into DBMS Vendor Specific Chars
		sql = conn.quotifySqlString(sql);

		int rows = 0;
		try
		{
			// First CHECK IF Table EXISTS
			DatabaseMetaData dbmd = conn.getMetaData();
			ResultSet rs = dbmd.getColumns(null, null, tabName, "%");
			boolean tabExists = rs.next();
			rs.close();

			if( tabExists )
			{
				Statement stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);

				while (rs.next())
				{
					rows++;
					String dbname     = rs.getString(1);
					String objectName = rs.getString(2);

					markDdlDetailsAsStored(dbname, objectName);
				}
				rs.close();
				stmt.close();
			}
		}
		catch (SQLException e)
		{
			// If NEW database, the "MonDdlStorage" is probably not there...
			// The SQLState from H2 is "42S02", but I if it's the same from other vendors I dont know. 
			// org.h2.jdbc.JdbcSQLException: Table "MonDdlStorage" not found; SQL statement:
			if ( ! "42S02".equals(e.getSQLState()) )
				_logger.error("Problems loading 'Stored DDL detailes'. SqlState='"+e.getSQLState()+"', sql="+sql, e);
		}
		
		String str = "Loaded "+rows+" 'Stored DDL detailes' from the PCS database.";
		_logger.debug(str);
		
	}

	/** Get the connection used for storing DDL Information data */
	private DbxConnection getDdlDetailsStorageConnection()
	{
		boolean waitedForConnectionProgress = false;
		while (_connStatus_inProgress)
		{
			waitedForConnectionProgress = true;
			_logger.info("DDL Lookup  Storage: Connection status 'in progress'... waiting for a connection to be established.");
			try { Thread.sleep(2000); }
			catch (InterruptedException ignore) {_logger.info("interupted: waiting for connection to be established.");}
		}
		if (waitedForConnectionProgress)
		{
			_logger.info("DDL Lookup  Storage: Continuing after waiting for: Connection status 'in progress'...");
//			while ( ! isSessionStarted() )
//			{
//				_logger.info("DDL Lookup  Storage: waiting for the session to be fully started... (Storage tables might not be there yet).");
//				try { Thread.sleep(2000); }
//				catch (InterruptedException ignore) {_logger.info("interupted: waiting for isSessionStarted().");}
//			}
		}

		if (_mainConn == null)
			return null;
		
		// If a specific connection is NOT configured, lets use the MAIN 
		boolean useOwnConnection = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_ddlInfoStorage_usePrivateConnection, DEFAULT_ddlInfoStorage_usePrivateConnection);
		if ( ! useOwnConnection )
			return _mainConn;

		// Create a new Connection if one isn't connected.
		if (_ddlStorageConn == null)
		{
			// This should be done in open()
		}
		else if ( ! _ddlStorageConn.isConnectionOk() )
		{
			try 
			{
				_ddlStorageConn.reConnect(null);
			}
			catch (Exception ex)
			{
				_logger.warn("The Re-Connect to 'DDL Information Storage' failed.");
				return null;
			}
		}

		_logger.debug("Use private connection for 'DDL Information Storage'");
		return _ddlStorageConn;
	}

	private boolean printMessage(long lastTime, long limit)
	{
		return System.currentTimeMillis() - lastTime > limit;
	}
	
	private long _lastSaveDdlDetailMessageTime = 0;
	private long _lastSaveDdlDetailMessageLimit = 5000;

	/** save DDL into "any" storage */
	@Override
	public void saveDdlDetails(DdlDetails ddlDetails)
	{
		if (ddlDetails == null)
			throw new RuntimeException("saveDdl(), input 'ddlDetails', can't be null.");
			
		DbxConnection conn = getDdlDetailsStorageConnection();
		if (conn == null)
		{
			if (printMessage(_lastSaveDdlDetailMessageTime, _lastSaveDdlDetailMessageLimit))
			{
				_logger.info("DDL Lookup Storage: No database connection to Persistent Storage DB.");
				_lastSaveDdlDetailMessageTime = System.currentTimeMillis();
			}
			return;
		}

		if (_shutdownWithNoWait)
		{
			if (printMessage(_lastSaveDdlDetailMessageTime, _lastSaveDdlDetailMessageLimit))
			{
				_logger.info("DDL Lookup Storage: Discard entry due to 'ShutdownWithNoWait'.");
				_lastSaveDdlDetailMessageTime = System.currentTimeMillis();
			}
			return;
		}

		// Check that we have started and all table has been created...
		if ( ! isSessionStarted() )
		{
			if (printMessage(_lastSaveDdlDetailMessageTime, _lastSaveDdlDetailMessageLimit))
			{
				_logger.warn("DDL Lookup Storage: Discard entry due to PCS '"+getName()+"' has not yet been fully started'. (Storage tables might not be there yet).");
				_lastSaveDdlDetailMessageTime = System.currentTimeMillis();
			}
			return;
		}

		// check AGAIN if DDL has NOT been saved in any writer class
		if ( isDdlDetailsStored(ddlDetails.getDbname(), ddlDetails.getObjectName()) )
		{
			_logger.debug("saveDdlDetails(): The DDL for dbname '"+ddlDetails.getDbname()+"', objectName '"+ddlDetails.getObjectName()+"' has already been stored by all the writers.");
			return;
		}
		if ( isDdlDetailsStored(ddlDetails.getSearchDbname(), ddlDetails.getObjectName()) )
		{
			_logger.debug("saveDdlDetails(): The DDL for searchDbname '"+ddlDetails.getSearchDbname()+"', objectName '"+ddlDetails.getObjectName()+"' has already been stored by all the writers.");
			return;
		}

		try
		{
			_logger.debug("DEBUG: saveDdlDetails() SAVING " + ddlDetails.getFullObjectName());

			String sql = getTableInsertStr(conn, DDL_STORAGE, null, true);
//			PreparedStatement pstmt = conn.prepareStatement(sql);
//			PreparedStatement pstmt = PreparedStatementCache.getPreparedStatement(conn, sql);
			PreparedStatement pstmt = _cachePreparedStatements ? PreparedStatementCache.getPreparedStatement(conn, sql) : conn.prepareStatement(sql);

			// TODO: check MetaData for length of 'dependList' column
			String dependList = StringUtil.toCommaStr(ddlDetails.getDependList());
			if (dependList.length() > 1500)
				dependList = dependList.substring(0, 1497) + "...";

			int col = 1;

			pstmt.setString(col++, ddlDetails.getDbname());
			pstmt.setString(col++, ddlDetails.getOwner());
			pstmt.setString(col++, ddlDetails.getObjectName());
			pstmt.setString(col++, ddlDetails.getType());
			pstmt.setString(col++, ddlDetails.getCrdate()     == null ? null : ddlDetails.getCrdate()    .toString() );
			pstmt.setString(col++, ddlDetails.getSampleTime() == null ? null : ddlDetails.getSampleTime().toString() );
			pstmt.setString(col++, ddlDetails.getSource());
			pstmt.setString(col++, ddlDetails.getDependParent());
			pstmt.setInt   (col++, ddlDetails.getDependLevel());
			pstmt.setString(col++, dependList);
			pstmt.setString(col++, ddlDetails.getObjectText());
			pstmt.setString(col++, ddlDetails.getDependsText());
			pstmt.setString(col++, ddlDetails.getOptdiagText());
			pstmt.setString(col++, ddlDetails.getExtraInfoText());

			pstmt.executeUpdate();
			pstmt.close();
			
			// update statistics that we have stored a DDL entry
			getStatistics().incDdlSaveCount();

			// ADD IT TO HE CACHE AS SAVED, no need to save again.
			markDdlDetailsAsStored(ddlDetails.getSearchDbname(), ddlDetails.getObjectName());
			markDdlDetailsAsStored(ddlDetails.getDbname(),       ddlDetails.getObjectName());

//			return rowsCount;
		}
		catch (SQLException e)
		{
			_logger.warn("Error writing DDL Information to Persistent Counter Store. for table dbname='"+ddlDetails.getDbname()+"', objectName='"+ddlDetails.getObjectName()+"'.", e);
			isSevereProblem(conn, e);
//			return -1;
		}
	}
	//---------------------------------------------
	// END: DDL Lookup
	//---------------------------------------------

	//---------------------------------------------
	// BEGIN: SQL Capture
	//---------------------------------------------
	/** Get the connection used for storing SQL Capture data */
	private DbxConnection getSqlCaptureStorageConnection()
	{
		boolean waitedForConnectionProgress = false;
		while (_connStatus_inProgress)
		{
			waitedForConnectionProgress = true;
			_logger.info("SQL Capture Storage: Connection status 'in progress'... waiting for a connection to be established.");
			try { Thread.sleep(2000); }
			catch (InterruptedException ignore) {_logger.info("interupted: waiting for connection to be established.");}
		}
		if (waitedForConnectionProgress)
		{
			_logger.info("SQL Capture Storage: Continuing after waiting for: Connection status 'in progress'...");
//			while ( ! isSessionStarted() )
//			{
//				_logger.info("SQL Capture Storage: waiting for the session to be fully started... (Storage tables might not be there yet).");
//				try { Thread.sleep(2000); }
//				catch (InterruptedException ignore) {_logger.info("interupted: waiting for isSessionStarted().");}
//			}
		}

		if (_mainConn == null)
			return null;
		
		// If a specific connection is NOT configured, lets use the MAIN 
		boolean useOwnConnection = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sqlCaptureStorage_usePrivateConnection, DEFAULT_sqlCaptureStorage_usePrivateConnection);
		if ( ! useOwnConnection )
			return _mainConn;

		// Create a new Connection if one isn't connected.
		if (_sqlCaptureStorageConn == null)
		{
			// This should be done in open()
		}
		else if ( ! _sqlCaptureStorageConn.isConnectionOk() )
		{
			try 
			{
				_sqlCaptureStorageConn.reConnect(null);
			}
			catch (Exception ex)
			{
				_logger.warn("The Re-Connect to 'SQL Capture Storage' failed.");
				return null;
			}
		}

		_logger.debug("Use private connection for 'SQL Capture Storage'");
		return _sqlCaptureStorageConn;
	}

	private long _lastSaveSqlCaptureDetailsMessageTime = 0;
	private long _lastSaveSqlCaptureDetailsMessageLimit = 5000;

	@Override
	public void saveSqlCaptureDetails(SqlCaptureDetails sqlCaptureDetails)
	{
		if (sqlCaptureDetails == null)
			throw new RuntimeException("saveSqlCaptureDetails(), input 'ddlDesqlCaptureDetailstails', can't be null.");
			
		
		if ( ! PersistentCounterHandler.hasInstance() )
		{
			_logger.info("SQL Capture Storage: No PersistentCounterHandler has yet been installed.");
			return;
		}

		ISqlCaptureBroker sqlCaptureBroker = PersistentCounterHandler.getInstance().getSqlCaptureBroker();
		if (sqlCaptureBroker == null)
		{
			_logger.info("SQL Capture Storage: No SQL Capture Broker was specified in the PersistentCounterHandler.");
			return;
		}

		DbxConnection conn = getSqlCaptureStorageConnection();
		if (conn == null)
		{
			if (printMessage(_lastSaveSqlCaptureDetailsMessageTime, _lastSaveSqlCaptureDetailsMessageLimit))
			{
				_logger.info("SQL Capture Storage: No database connection to Persistent Storage DB.");
				_lastSaveSqlCaptureDetailsMessageTime = System.currentTimeMillis();
			}
			return;
		}

		if (_shutdownWithNoWait)
		{
			if (printMessage(_lastSaveSqlCaptureDetailsMessageTime, _lastSaveSqlCaptureDetailsMessageLimit))
			{
				_logger.info("SQL Capture Storage: Discard entry due to 'ShutdownWithNoWait'.");
				_lastSaveSqlCaptureDetailsMessageTime = System.currentTimeMillis();
			}
			return;
		}

		// Check that we have started and all table has been created...
		if ( ! isSessionStarted() )
		{
			if (printMessage(_lastSaveSqlCaptureDetailsMessageTime, _lastSaveSqlCaptureDetailsMessageLimit))
			{
				_logger.warn("SQL Capture Storage: Discard entry due to PCS '"+getName()+"' has not yet been fully started'. (Storage tables might not be there yet).");
				_lastSaveSqlCaptureDetailsMessageTime = System.currentTimeMillis();
			}
			return;
		}

		try
		{
			// START a transaction
			// This will lower number of IO's to the transaction log
			if (conn.getAutoCommit() == true)
				conn.setAutoCommit(false);

			_logger.debug("DEBUG: saveSqlCaptureDetails() SAVING " + sqlCaptureDetails.size() + " entries in this 'batch'.");
//System.out.println("DEBUG: saveSqlCaptureDetails() SAVING " + sqlCaptureDetails.size() + " entries in this 'batch'.");

			HashMap<String, PreparedStatement> pstmntMap = new HashMap<String, PreparedStatement>();
			
			for (List<Object> row : sqlCaptureDetails.getList())
			{
				// FIRST entry in each of the lists should be the table name where to store the information.
				String tabName = (String) row.get(0);
				String sql = sqlCaptureBroker.getInsertStatement(conn, tabName);
//System.out.println("saveSqlCaptureDetails(): tabName=|"+tabName+"|.");
//System.out.println("saveSqlCaptureDetails(): sql=|"+sql+"|.");
//System.out.println("saveSqlCaptureDetails(): row="+row);
//if (true)
//	continue;
//System.out.println("saveSqlCaptureDetails(): tabName=|"+tabName+"|, SQL=|"+sql+"|");
				
				if (sql == null)
				{
					_logger.warn("SQL Capture Storage: Discard entry due to getInsertStatement(tabName='"+tabName+"') returned null, so we do not know how to store the information in the PCS database.");
					continue;
				}

				PreparedStatement pstmt = pstmntMap.get(tabName);
				if (pstmt == null)
				{
					pstmt = _cachePreparedStatements ? PreparedStatementCache.getPreparedStatement(conn, sql) : conn.prepareStatement(sql);
					pstmntMap.put(tabName, pstmt);
				}
//				PreparedStatement pstmt = _cachePreparedStatements ? PreparedStatementCache.getPreparedStatement(conn, sql) : conn.prepareStatement(sql);

//if (tabName.equals("MonSqlCapSqlText") || tabName.equals("MonSqlCapPlans"))
//	System.out.println("saveSqlCaptureDetails(): tabName=|"+tabName+"|, TEXT=|"+row.get(row.size()-1)+"|)");
				// Loop all columns, and SET it at the Prepared Statement
				for (int c=1; c<row.size(); c++) // Skip entry 0, where the table name is stored
				{
					Object colObj =  row.get(c);
//System.out.println("saveSqlCaptureDetails(): tabName=|"+tabName+"|, setObject(parameterIndex="+c+", val='"+colObj+"')");
					pstmt.setObject(c, colObj);
				} // end: loop cols
		
				// ADD the row to the BATCH
				pstmt.addBatch();
				
				// update statistics that we have stored a SQL Capture entry
				getStatistics().incSqlCaptureEntryCount();
			}
			
			for (String key : pstmntMap.keySet())
			{
				PreparedStatement pstmt = pstmntMap.get(key);
				if (_logger.isDebugEnabled())
					_logger.debug("saveSqlCaptureDetails(): executeBatch & close for table '"+key+"'.");

				pstmt.executeBatch();
				pstmt.close();
			}

			// CLOSE the transaction
			conn.commit();

			// update statistics that we have stored a SQL Capture Batch
			getStatistics().incSqlCaptureBatchCount();
		}
		catch (SQLException e)
		{
			try 
			{
				if (conn.getAutoCommit() == true)
					conn.rollback();
			}
			catch (SQLException e2) {}

			_logger.warn("Error writing SQL Capture Information to Persistent Counter Store. getErrorCode()="+e.getErrorCode()+", Caught: "+e, e);
			isSevereProblem(conn, e);
		}
		finally
		{
			try { conn.setAutoCommit(true); }
			catch (SQLException e2) { _logger.error("Problems when setting AutoCommit to true.", e2); }
		}
	}
	//---------------------------------------------
	// END: SQL Capture
	//---------------------------------------------
	
	
	
	//---------------------------------------------
	// BEGIN: Daily Summary Report
	//---------------------------------------------
	private void createDailySummaryReport(DbxConnection conn, String serverName)
	{
		if ( ! DailySummaryReportFactory.isCreateReportEnabled() )
		{
			_logger.info("Daily Summary Report is NOT Enabled, this can be enabled using property '"+DailySummaryReportFactory.PROPKEY_create+"=true'.");
			return;
		}

		if ( ! DailySummaryReportFactory.isCreateReportEnabledForServer(serverName) )
		{
			_logger.info("Daily Summary Report is NOT Enabled, for serverName '"+serverName+"'. Check property '"+DailySummaryReportFactory.PROPKEY_filter_keep_servername+"' or '"+DailySummaryReportFactory.PROPKEY_filter_skip_servername+"'.");
			return;
		}

		IDailySummaryReport report = DailySummaryReportFactory.createDailySummaryReport(serverName);
		if (report == null)
		{
			_logger.info("Daily Summary Report: create did not pass a valid report instance, skipping report creation.");
			return;
		}

		report.setConnection(conn);
		report.setServerName(serverName);
		try
		{
			// Initialize the Report, which also initialized the ReportSender
			report.init();

			// Create & and Send the report
			report.create();
			report.send();

			// Save the report
			report.save();

			// remove/ old reports from the "archive"
			report.removeOldReports();
		}
		catch(Exception ex)
		{
			_logger.error("Problems Sending Daily Summary Report. Caught: "+ex, ex);
		}
	}
	//---------------------------------------------
	// END: Daily Summary Report
	//---------------------------------------------

}
