package com.asetune.central.pcs;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asetune.DbxTune;
import com.asetune.Version;
import com.asetune.central.DbxTuneCentral;
import com.asetune.central.cleanup.CentralH2Defrag;
import com.asetune.central.cleanup.DataDirectoryCleaner;
import com.asetune.central.pcs.CentralPcsWriterHandler.NotificationType;
import com.asetune.central.pcs.DbxTuneSample.AlarmEntry;
import com.asetune.central.pcs.DbxTuneSample.AlarmEntryWrapper;
import com.asetune.central.pcs.DbxTuneSample.CmEntry;
import com.asetune.central.pcs.DbxTuneSample.GraphEntry;
import com.asetune.cm.CountersModel;
import com.asetune.gui.MainFrame;
import com.asetune.sql.conn.ConnectionProp;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.AseUrlHelper;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;
import com.asetune.utils.H2UrlHelper;
import com.asetune.utils.ShutdownHandler;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;
import com.asetune.utils.TimeUtils;
import com.asetune.utils.Ver;

public class CentralPersistWriterJdbc 
extends CentralPersistWriterBase
{
	private final Logger _logger = LoggerFactory.getLogger(this.getClass().getName());

	public static final String PROPKEY_BASE          = "CentralPersistWriterJdbc.";

	public static final String PROPKEY_JDBC_DRIVER   = "CentralPersistWriterJdbc.driver";
	public static final String DEFAULT_JDBC_DRIVER   = "org.h2.Driver";
	
	public static final String PROPKEY_JDBC_URL      = "CentralPersistWriterJdbc.url";
	public static final String DEFAULT_JDBC_URL      = "jdbc:h2:file:${DBXTUNE_SAVE_DIR}/DBXTUNE_CENTRAL_DB";
	
	public static final String PROPKEY_JDBC_USERNAME = "CentralPersistWriterJdbc.username";
	public static final String DEFAULT_JDBC_USERNAME = "sa";

	public static final String PROPKEY_JDBC_PASSWORD = "CentralPersistWriterJdbc.password";
	public static final String DEFAULT_JDBC_PASSWORD = "";
	
	public static final String PROPKEY_ALARM_EVENTS_STORE = "CentralPersistWriterJdbc.alarm.events.store";
	public static final String DEFAULT_ALARM_EVENTS_STORE = "RAISE, CANCEL";
	
//	public static final String  PROPKEY_H2_AUTO_DEFRAG_TIME = "CentralPersistWriterJdbc.h2.auto.defrag.time";
////	public static final int     DEFAULT_H2_AUTO_DEFRAG_TIME = 400; // 400 = 04:00 == 4 in the morning (NOTE: do not specify 0400, then it will be in "octal" and the value 256 )
//	public static final int     DEFAULT_H2_AUTO_DEFRAG_TIME = -1; // -1 == DISABLED
	
	public static final String  PROPKEY_H2_CLEANUP_THRESHOLD_MB = "CentralPersistWriterJdbc.h2.cleanup.threshold.mb";
	public static final int     DEFAULT_H2_CLEANUP_THRESHOLD_MB = 250;

	public static final String  PROPKEY_SAVE_SESSION_SAMPLE_DETAILS = "CentralPersistWriterJdbc.save.SESSION_SAMPLE_DETAILS";
	public static final boolean DEFAULT_SAVE_SESSION_SAMPLE_DETAILS = true;

	
	private String        _jdbcDriver   = null;
	private String        _jdbcUrl      = null;
	private String        _jdbcUsername = null;
	private String        _jdbcPassword = null;
	
	private boolean       _keepConnOpen = true;
	private DbxConnection _mainConn     = null;

	
	private   String _lastUsedUrl = null;
	private   boolean _connStatus_inProgress = false;
	protected boolean _jdbcDriverInfoHasBeenWritten = false;

	private boolean _inSaveSample;
	private boolean _saveCounterData = false;

	private boolean _shutdownWithNoWait;
//	private boolean _isSevereProblem = false;
	
//	private int    _h2AutoDefragTime   = DEFAULT_H2_AUTO_DEFRAG_TIME;
//	private Thread _h2AutoDefragThread = null;

	private int    _h2CleanupThresholdMb = DEFAULT_H2_CLEANUP_THRESHOLD_MB;
	
	/** use to check if Graph Properties exists for a specific session */
//	private Set<String> _graphPropertiesSet = new HashSet<>();
	private Map<String, Integer> _graphPropertiesCountMap = new HashMap<>();

	/** A set of actions which we want to store, for example "RAISE", "CANCEL", "END-OF-SCAN" or "RE-RAISE", the default is only "RAISE", "CANCEL" */
	Set<String> _alarmEventActionStore = new HashSet<>();

	/** used is serviceStart/Stop to check if we already has done stop */
	private String _servicesStoppedByThread = null;

//	public boolean checkIfSchemaExists(DbxConnection conn, String schema, boolean create)
//	{
//		// Use JDBC MetaData to check this
//		return true;
//	}
//	
//	public boolean createSchema(DbxConnection conn, String schema)
//	{
//		// Use JDBC MetaData to check this
//		return true;
//	}

	@Override
	public synchronized void init(Configuration conf) throws Exception
	{
		super.init(conf);

		// WRITE init message, jupp a little late, but I wanted to grab the _name
		_logger.info("Initializing the Central PersistentCounterHandler.WriterClass component named '"+this.getClass().getName()+"'.");
		
//		// Create a connection pool (probably move this to CentralPcs(Reader|Writer)Jdbc or some other place)
//		ConnectionProp cp = new ConnectionProp();
//		cp.setDriverClass(conf.getProperty(CentralPersistWriterJdbc.PROPKEY_JDBC_DRIVER,   DEFAULT_JDBC_DRIVER));
//		cp.setUrl        (conf.getProperty(CentralPersistWriterJdbc.PROPKEY_JDBC_URL,      DEFAULT_JDBC_URL));
//		cp.setUsername   (conf.getProperty(CentralPersistWriterJdbc.PROPKEY_JDBC_USERNAME, DEFAULT_JDBC_USERNAME));
//		cp.setPassword   (conf.getProperty(CentralPersistWriterJdbc.PROPKEY_JDBC_PASSWORD, DEFAULT_JDBC_PASSWORD));
//		cp.setAppName    (Version.getAppName());
//		DbxConnectionPool connPool = new DbxConnectionPool(cp, 30);
//		DbxConnectionPool.setInstance(connPool);

		_jdbcDriver   = conf.getProperty(PROPKEY_JDBC_DRIVER,   DEFAULT_JDBC_DRIVER);
		_jdbcUrl      = conf.getProperty(PROPKEY_JDBC_URL,      DEFAULT_JDBC_URL);
		_jdbcUsername = conf.getProperty(PROPKEY_JDBC_USERNAME, DEFAULT_JDBC_USERNAME);
		_jdbcPassword = conf.getProperty(PROPKEY_JDBC_PASSWORD, DEFAULT_JDBC_PASSWORD);
		if (_jdbcPassword.equalsIgnoreCase("null"))
			_jdbcPassword="";
		
		_alarmEventActionStore = StringUtil.commaStrToSet( conf.getProperty(PROPKEY_ALARM_EVENTS_STORE, DEFAULT_ALARM_EVENTS_STORE) );
		
//		_h2AutoDefragTime = conf.getIntProperty(PROPKEY_H2_AUTO_DEFRAG_TIME, DEFAULT_H2_AUTO_DEFRAG_TIME);
		
		_h2CleanupThresholdMb = conf.getIntProperty(PROPKEY_H2_CLEANUP_THRESHOLD_MB, DEFAULT_H2_CLEANUP_THRESHOLD_MB);
		
		// Create common tables (which for example: is used by the reader)
		checkAndCreateCommonTables();
	}
	
	public void close(boolean force)
	{
		if (_mainConn == null)
			return;

		if ( ! _keepConnOpen || force)
		{
			try { _mainConn.close(); } catch(Exception ignore) {}
			_mainConn = null; 
			
			// NOTE: Should we STOP any of the (H2) services???
			stopServices(5000);
		}
	}

	//	@Override
	private DbxConnection open(DbxTuneSample cont)
	{
//		Timestamp newDate = (cont != null) ? cont.getSessionSampleTime() : new Timestamp(System.currentTimeMillis());

		// Reset the severe problem flag
//		_isSevereProblem = false;
		
		// for H2 Databases: check DATA_DIR for enough space
		if ( _jdbcUrl.startsWith("jdbc:h2:"))
		{
			checkH2DataDirSpace();
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
		
		// Get a URL which might be changed or not...
		String localJdbcUrl = _lastUsedUrl == null ? _jdbcUrl : _lastUsedUrl;

		try
		{
			_connStatus_inProgress = true;

//			Class.forName(_jdbcDriver).newInstance();

			_logger.debug("Try getConnection to counterStore");

			if (_keepConnOpen)
				_logger.info("Open a new connection to the Persistent Counter Storage. 'keep conn open=true'");

			// Get a URL which might be changed or not...
			//String localJdbcUrl = _lastUsedUrl == null ? _jdbcUrl : _lastUsedUrl;

//			// Look for variables in the URL and change them into runtime
//			if ( localJdbcUrl.indexOf("${") >= 0)
//			{
//				localJdbcUrl = urlSubstitution(cont, localJdbcUrl);
//				_logger.info("Found variables in the URL '"+_jdbcUrl+"', the new URL will be '"+localJdbcUrl+"'.");
//				if (_h2NewDbOnDateChange)
//					_logger.info("When a new ${DATE} of the format '"+_h2DbDateParseFormat+"' has been reached, a new database will be opened using that timestamp.");
//
//				// This will be used to determine if a new DB should be opened with a new TIMESTAMP.
//				String dateStr = new SimpleDateFormat(_h2DbDateParseFormat).format(newDate);
//				_h2LastDateChange = dateStr;
//
//				// Also clear what DDL's has been created, so they can be recreated.
//				clearIsDdlCreatedCache();
//				
//				// Also clear what what DDL information that has been stored, so we can add them to the new database again
//				clearDdlDetailesCache();
//
//				// Indicate the we need to start a new session...
//				setSessionStarted(false);
//			}

			// IF H2, add hard coded stuff to URL
			//if ( _jdbcDriver.equals("org.h2.Driver") )
			if ( _jdbcUrl.startsWith("jdbc:h2:"))
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
					change = true;
					_logger.info("H2 URL add option: WRITE_DELAY=30000");
					urlMap.put("WRITE_DELAY",  "30000");
				}

//				// DATABASE_EVENT_LISTENER mode
//				if ( ! urlMap.containsKey("DATABASE_EVENT_LISTENER") )
//				{
//					change = true;
//					_logger.info("H2 URL add option: DATABASE_EVENT_LISTENER="+H2DatabaseEventListener.class.getName());
//					urlMap.put("DATABASE_EVENT_LISTENER",  H2DatabaseEventListener.class.getName());
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

//			Connection conn = DriverManager.getConnection(localJdbcUrl, _jdbcUser, _jdbcPasswd);
//			_mainConn = DbxConnection.createDbxConnection(conn);
			// The below connection properties will be used if/when doing reConnect()
			ConnectionProp connProp = new ConnectionProp();
			connProp.setDriverClass(_jdbcDriver);
			connProp.setUsername(_jdbcUsername);
			connProp.setPassword(_jdbcPassword);
			connProp.setUrl(localJdbcUrl);
			connProp.setAppName(Version.getAppName()+"-pcsWriter");
			connProp.setAppVersion(Version.getVersionStr());
			// Now Connect
			_mainConn = DbxConnection.connect(null, connProp);

			// Remember the last used URL (in case of H2 spill over database), the _mainConn is null at that time, so we cant use _mainConn.getConnProp().getUrl()
			_lastUsedUrl = localJdbcUrl;
			
			_logger.info("A Database connection to URL '"+localJdbcUrl+"' has been opened, using driver '"+_jdbcDriver+"'.");
			_logger.debug("The connection has property auto-commit set to '"+_mainConn.getAutoCommit()+"'.");

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

					String getIdentifierQuoteString  = "\"";

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

					try	{ getIdentifierQuoteString  = dbmd.getIdentifierQuoteString();  } catch (Throwable ignore) {}
					
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

					// Get and set the QuotedIdentifier Character
					setQuotedIdentifierChar(getIdentifierQuoteString == null ? "\"" : getIdentifierQuoteString);
				}
			}

			// if H2
			// Set some specific stuff
			if ( DbUtils.DB_PROD_NAME_H2.equals(getDatabaseProductName()) )
			{
				_logger.info("Do H2 Specific settings for the database.");
				setH2SpecificSettings(_mainConn);
			}

			// if ASE, turn off error message like: Scale error during implicit conversion of NUMERIC value '1.2920528650283813' to a NUMERIC field.
			// or fix the actual values to be more correct when creating graph data etc.
			if ( DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(getDatabaseProductName()) )
			{
				_logger.info("Do Sybase ASE Specific settings for the database.");
				setAseSpecificSettings(_mainConn);
			}

			// GET information already stored in the DDL Details storage
//			populateDdlDetailesCache();
			
			// Start any services that depended on open() to complete
//			startServices();
		}
		catch (SQLException ev)
		{
			boolean h2DbIsAlreadyOpen = false;
			
			StringBuffer sb = new StringBuffer();
			while (ev != null)
			{
				// DATABASE_ALREADY_OPEN_1 = 90020
				if (ev.getErrorCode() == 90020)
					h2DbIsAlreadyOpen = true;

				sb.append("ErrorCode=").append( ev.getErrorCode() ).append(", ");
				sb.append("SQLState=" ).append( ev.getSQLState()  ).append(", ");
				sb.append("Message="  ).append( ev.getMessage()   );
				sb.append( "\n" );
				ev = ev.getNextException();
			}
			String errorMessages = sb.toString().trim();
			
			_logger.error("Problems when connecting to a datastore Server. "+errorMessages);
			_mainConn = closeConn(_mainConn);

			// IF H2, make special things
			if ( _jdbcDriver.equals("org.h2.Driver") && _mainConn != null)
			{
				// Try to close ALL H2 servers, many might be active due to AUTO_SERVER=true
				if (h2DbIsAlreadyOpen)
				{
					// param 1: 0 = all servers
					// param 3: 0 = SHUTDOWN_NORMAL, 1 = SHUTDOWN_FORCE
//					TcpServer.stopServer(0, _jdbcPassword, 1);
					
					// Can we try to restart the DbxCentral...
					//_logger.error("FIXME: RESTART DbxCentral: -NOT-YET-IMPLEMENTED-");
					
					// Lets try to RESTART DbxCentral... and hope that it clears the error...
					Configuration shutdownConfig = new Configuration();
					shutdownConfig.setProperty("h2.shutdown.type", H2ShutdownType.IMMEDIATELY.toString());  // IMMEDIATELY, COMPACT, DEFRAG

					String reason = "Restart Requested from CentralPersistWriterJdbc.open(): DATABASE_ALREADY_OPEN_1(90020), url='"+localJdbcUrl+"', errors='"+errorMessages+"'.";
					boolean doRestart = true;
					ShutdownHandler.shutdown(reason, doRestart, shutdownConfig);
				}
			}
		}
		catch (Exception ev)
		{
			_logger.error("openConnection", ev);
			_mainConn = closeConn(_mainConn);
		}
		finally
		{
			_connStatus_inProgress = false;
		}

		// if H2
		// Set some specific stuff
		if ( _mainConn != null && _mainConn.isDatabaseProduct(DbUtils.DB_PROD_NAME_H2) )
		{
//			if (_h2AutoDefragTime >= 0 && _h2AutoDefragThread == null)
//			{
//				createH2AutoDefragThread();
//			}
		}

		
		return _mainConn;
	}

//	/**
//	 * This is <b>probably</b> better to do with <code>ScheduledExecutorService</code> but... I did it "quick and dirty"
//	 */
//	protected void createH2AutoDefragThread()
//	{
//		if (_h2AutoDefragThread != null)
//		{
//			_logger.info("H2 'auto defrag' wont be installed. Reason: It's already installed.");
//			return;
//		}
//		if (_h2AutoDefragTime < 0 || _h2AutoDefragTime > 2359 || (_h2AutoDefragTime%100) >= 60)
//		{
//			_logger.info("H2 'auto defrag' wont be installed. Reason: config parameter '"+PROPKEY_H2_AUTO_DEFRAG_TIME+"' must be between '0000' and '2359'. current values is '"+_h2AutoDefragTime+"'.");
//			return;
//		}
//
//		_logger.info("Installing H2 'auto defrag' thread, which will 'defrag' the database at '"+_h2AutoDefragTime+"' every day.");
//		_h2AutoDefragThread = new Thread()
//		{
//			@Override
//			public void run()
//			{
//				boolean running = true;
//				_logger.info("Started H2 'auto defrag' thread, which will 'defrag' the database at '"+_h2AutoDefragTime+"' every day.");
//				while(running)
//				{
//					try
//					{
//						int atHour   = _h2AutoDefragTime / 100;
//						int atMinute = _h2AutoDefragTime % 100;
//						Calendar now = Calendar.getInstance();
//						if (now.get(Calendar.HOUR_OF_DAY) == atHour && now.get(Calendar.MINUTE) == atMinute)
//						{
//							// Do "shutdown defrag"
//							// - The inbound queue will hold entries while the "defrag"
//							// - next open() will open a new connection... 
//							// - Then the queue is drained... (and inserted into the database)
//							_logger.info("Waking up to do 'H2 defrag'.");
//							h2Shutdown(H2ShutdownType.DEFRAG);
//						}
//						Thread.sleep(60*1000);
//					}
//					catch (Throwable t)
//					{
//						_logger.error("Auto-Defrag-Thread. Excpeption, but continuing... Caught: "+t);
//					}
//				}
//				_logger.info("Ending H2 'auto defrag' thread.");
//			}
//		};
//		_h2AutoDefragThread.setName("DbxTune-H2-auto-defrag-thread");
//		_h2AutoDefragThread.setDaemon(true);
//		_h2AutoDefragThread.start();
//	}

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

//			// DYNAMIC_PREPARE
//			if ( aseDynamicPrepare && ! urlMap.containsKey("DYNAMIC_PREPARE") )
//			{
//				change = true;
//				_logger.info("ASE URL add option: DYNAMIC_PREPARE=true");
//				_logger.info("Setting '"+PROPKEY_cachePreparedStatements+"' to 'true' when DYNAMIC_PREPARE is enabled. This means most SQL PreparedStatements (and the server side lightweight stored procedure) in the Writer is reused. And the statements are not closed when pstmnt.close() is called.");
//				
//				_cachePreparedStatements = true;
//				urlMap.put("DYNAMIC_PREPARE", "true");
//			}

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
		int aseVersion = conn.getDbmsVersionNumber();
		if (aseVersion < Ver.ver(15,0))
		{
			String msg = "The PCS storage is ASE Version '"+Ver.versionIntToStr(aseVersion)+"', which is NOT a good idea. This since it can't handle table names longer than 30 characters and the PCS uses longer name. There for I only support ASE 15.0 or higher for the PCS storage. I recommend to use H2 database as the PCS instead (http://www.h2database.com), which is included in the "+Version.getAppName()+" package.";
			_logger.error(msg);
//			close(true); // Force close, will set various connections to NULL which is required by: MainFrame.getInstance().action_disconnect() 

			Exception ex =  new Exception(msg);
			{
				MainFrame.getInstance().action_disconnect();
				SwingUtils.showErrorMessage("CentralPersistWriterJdbc", msg, ex);
			}
			throw ex;
		}

		// Get current dbname, if master, then do not allow...
		String dbname = conn.getCatalog();
		if ("master".equals(dbname))
		{
			String msg = "Current ASE Database is '"+dbname+"'. "+Version.getAppName()+" does NOT support storage off Performance Counters in the '"+dbname+"' database.";
			_logger.error(msg);
//			close(true); // Force close, will set various connections to NULL which is required by: MainFrame.getInstance().action_disconnect() 
			
			Exception ex =  new Exception(msg);
			if (DbxTune.hasGui())
			{
				MainFrame.getInstance().action_disconnect();
				SwingUtils.showErrorMessage("CentralPersistWriterJdbc", msg, ex);
			}
			throw ex;
		}

		if (aseVersion >= Ver.ver(15,0))
		{
			_logger.debug("Connected to ASE (above 15.0), do some specific settings 'set delayed_commit on'.");
			dbExecSetting(_mainConn, "set delayed_commit on ", logExtraInfo);
		}

		int asePageSize = AseConnectionUtils.getAsePageSize(_mainConn);
		if (asePageSize < 4096)
		{
			_logger.warn("The ASE Servers Page Size is '"+asePageSize+"', to the connected server version '"+Ver.versionIntToStr(aseVersion)+"', which is probably NOT a good idea. The PCS storage will use rows wider than that... which will be reported as errors. However I will let this continue. BUT you can just hope for the best.");
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
	/**
	 * Get the JDBC Connection Properties used to store data in the database.<br>
	 * @return null if no connection, otherwise the ConnectionProp used while connectiong to database
	 */
	public ConnectionProp getStorageConnectionProps()
	{
		if (_mainConn == null)
			return null;
		
		return _mainConn.getConnProp();
	}

	/**
	 * Checks SQL Exceptions for specific errors when storing counters
	 * @return true if a severe problem
	 */
	protected boolean isSevereProblem(DbxConnection conn, SQLException e)
	throws SQLException
	{
		SQLException originException = e;

		if (e == null)
			return false;
		boolean doThrow    = false;
		boolean doShutdown = false;
		boolean dbIsFull   = false;

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
					doThrow    = true;
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
					doThrow    = true;
				}

				// Check if it looks like a CORRUPT H2 database
				Throwable rootCauseEx = ExceptionUtils.getRootCause(e);
				if (rootCauseEx != null && rootCauseEx.getClass().getSimpleName().equals("IllegalStateException"))
				{
					_logger.error("ATTENTION: ErrorCode="+e.getErrorCode()+", SQLState="+e.getSQLState()+", ex.toString="+e.toString()+", rootCauseEx="+rootCauseEx);
					_logger.error("ATTENTION: The H2 database looks CORRUPT, the rootCauseEx is 'IllegalStateException', which might indicate a corrupt database. rootCauseEx="+rootCauseEx, rootCauseEx);

					doThrow = true;
				}
				
				// The object is already closed == 90007
				// The database has been closed == 90098
				// MVStore: This map is closed == 90028
				// IllegalExceptionState: Transaction is closed == 50000
				if (error == 90007 || error == 90098 || error == 90028 || error == 50000)
				{
					doThrow = true;

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
					doThrow    = true;
				}
			}

			e = e.getNextException();
		}

		_logger.debug("isSevereProblem(): doShutdown='"+doShutdown+"', dbIsFull='"+dbIsFull+"', retCode='"+doThrow+"', _shutdownWithNoWait='"+_shutdownWithNoWait+"'.");

		// Actions
		if (dbIsFull)
		{
			if ( DbUtils.DB_PROD_NAME_H2.equals(getDatabaseProductName()) )
			{
				_logger.warn("No disk space available for the H2 Database... Trying to cleanup...");
				boolean freedSpace = checkH2DataDirSpace();

				// Check AFTER the cleanup, if we got more space...
				if (freedSpace)
				{
					_logger.info("Cleanup looked like it freed up some space. disable the shutdown flag and continuing.");
					doShutdown = false;
				}
			}
		}

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
						CentralPcsWriterHandler.getInstance().stop(true, 0);
						CentralPcsWriterHandler.setInstance(null);					
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
					SwingUtils.showErrorMessage("CentralPersistWriterJdbc", msg, originException);
				}
			}
		}

//		_isSevereProblem = retCode;
		if (doThrow)
			throw originException;

		return doThrow;
	}

	private boolean checkH2DataDirSpace()
	{
		boolean didCleanup = false;
//		String localJdbcUrl = _lastUsedUrl == null ? _jdbcUrl : _lastUsedUrl;
//		H2UrlHelper urlHelper = new H2UrlHelper(localJdbcUrl);
//		File dbFile = urlHelper.getDbFile();

		File dataDir = new File(DbxTuneCentral.getAppDataDir());
		File dataDirRes = null;
		try { dataDirRes = dataDir.toPath().toRealPath().toFile(); } catch(IOException ex) { _logger.warn("Problems resolving File->Path->File");}
		
		double freeGb   = dataDir.getUsableSpace() / 1024.0 / 1024.0 / 1024.0;
//		double freeGb   = dataDir.getFreeSpace()   / 1024.0 / 1024.0 / 1024.0;
//		double usableGb = dataDir.getUsableSpace() / 1024.0 / 1024.0 / 1024.0;
		double totalGb  = dataDir.getTotalSpace()  / 1024.0 / 1024.0 / 1024.0;
		double pctUsed  = 100.0 - (freeGb / totalGb * 100.0);

//		double freeMb   = dataDir.getFreeSpace()   / 1024.0 / 1024.0;
////		double usableMb = dataDir.getUsableSpace() / 1024.0 / 1024.0;
//		double totalMb  = dataDir.getTotalSpace()  / 1024.0 / 1024.0;

		long thresholdInMb    = _h2CleanupThresholdMb;
		long thresholdInBytes = thresholdInMb * 1024 * 1024; // 100 MB

//		dataDir.toPath().toRealPath().toFile()
		boolean doCleanup = false;
//		long freeSpaceBefore = dataDir.getFreeSpace();
		long freeSpaceBefore = dataDir.getUsableSpace();
		if (freeSpaceBefore < thresholdInBytes)
		{
			doCleanup = true;
		}
		
		if (_logger.isDebugEnabled())
		{
			_logger.debug(String.format("checkH2DataDirSpace: doCleanup="+doCleanup+", Free = %.1f GB, Total = %.1f GB, Percent Used = %.1f %%, thresholdInMb = %d, dir='%s', resolvedDir='%s'", freeGb, totalGb, pctUsed, thresholdInMb, dataDir, dataDirRes));
		}

		if (doCleanup)
		{
			_logger.warn("Low on disk space available for the H2 Database... at '"+dataDir+"', resolved to '"+dataDirRes+"'.");
			_logger.warn(String.format("Free = %.1f GB, Total = %.1f GB, Percent Used = %.1f %%, thresholdInMb = %d", freeGb, totalGb, pctUsed, thresholdInMb));
			
			_logger.info("Trying to cleanup older H2 Database files, by calling DataDirectoryCleaner.execute(null)");
			
			// TRY TO: CLEANUP OLDER H2 DATABASES
			DataDirectoryCleaner ddc = new DataDirectoryCleaner();
			ddc.execute(null);

			// TRY TO: "shutdown defrag" of DBX_CENTRAL and see if that helps...

			
			// Check AFTER the cleanup, if we got more space...
//			long freeSpaceAfter = dataDir.getFreeSpace();
			long freeSpaceAfter = dataDir.getUsableSpace();
			if (freeSpaceAfter > thresholdInBytes)
			{
				didCleanup = true;
				double freedMb = (freeSpaceAfter - freeSpaceBefore) / 1024.0 / 1024.0;
				double freedGb = freedMb / 1024.0;
				_logger.info("Cleanup looked like it freed up some space. "+String.format("%.1f GB, %.1f MB", freedGb, freedMb));
			}
		}

		return didCleanup;
	}

	private boolean dbExecSetting(DbxConnection conn, String sql, boolean logInfo)
	throws SQLException
	{
		if (logInfo)
			_logger.info(getDatabaseProductName()+": "+sql);
		return conn.dbExec(sql, true) >= 0;
	}

	private boolean dbDdlExec(DbxConnection conn, String sql)
	throws SQLException
	{
		if (_logger.isDebugEnabled())
		{
			_logger.debug("SEND DDL SQL: " + sql);
		}
//System.out.println("dbDdlExec(): SEND DDL SQL: " + sql);

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
			// throws Exception if it's a severe problem
			isSevereProblem(conn, e);
			throw e;
		}

		return true;
	}


//	private void insertSessionParam(DbxConnection conn, Timestamp sessionsStartTime, String type, String key, String val)
//	throws SQLException
//	{
////		String tabName = getTableName(SESSION_PARAMS, null, true);
////
////		// make string a "safe" string, escape all ' (meaning with '')
////		if (key != null) key = key.replaceAll("'", "''");
////		if (val != null) val = val.replaceAll("'", "''");
////
////		// insert into MonSessionParams(SessionStartTime, Type, ParamName, ParamValue) values(...)
////		StringBuffer sbSql = new StringBuffer();
////		sbSql.append(" insert into ").append(tabName);
////		sbSql.append(" values('")
////			.append(sessionsStartTime).append("', '")
////			.append(type)             .append("', '")
////			.append(key)              .append("', '")
////			.append(val)              .append("')");
////
////		dbExec(sbSql.toString());
//
////		if (val != null && val.length() >= SESSION_PARAMS_VAL_MAXLEN)
////			val = val.substring(0, SESSION_PARAMS_VAL_MAXLEN - 1);
//
//		try
//		{
//			// Get the SQL statement
//			String sql = getTableInsertStr(SESSION_PARAMS, null, true);
////			PreparedStatement pst = conn.prepareStatement( sql );
//			PreparedStatement pst = _cachePreparedStatements ? PreparedStatementCache.getPreparedStatement(conn, sql) : conn.prepareStatement(sql);
//	
//			// Set values
//			pst.setString(1, sessionsStartTime.toString());
//			pst.setString(2, type);
//			pst.setString(3, key);
//			pst.setString(4, val);
//	
//			// EXECUTE
//			pst.executeUpdate();
//			pst.close();
//			getStatistics().incInserts();
//		}
//		catch (SQLException e)
//		{
//			_logger.warn("Error in insertSessionParam() writing to Persistent Counter Store. insertSessionParam(sessionsStartTime='"+sessionsStartTime+"', type='"+type+"', key='"+key+"', val='"+val+"')", e);
// 			// throws Exception if it's a severe problem
//			if (isSevereProblem(conn, e))
//				throw e;
//		}
//	}

	/** Used to check if a table is created or not */
	private Set<String> _ddlSet = new HashSet<>();

	public void clearIsDdlCreatedCache()
	{
		_logger.info("Clearing the in-memory cache, which tells us if a specific table has been created or not.");
		_ddlSet.clear();
	}

	public boolean isDdlCreated(String tabName)
	{
		return _ddlSet.contains(tabName);
	}

	public void markDdlAsCreated(String tabName)
	{
		_ddlSet.add(tabName);
	}
	
	/** 
	 * Check if table has been created, if not create it.
	 * @param table 
	 * @return True if table was created
	 * @throws SQLException
	 */
	private boolean checkAndCreateTable(DbxConnection conn, String schemaName, Table table)
	throws SQLException
	{
		String fullTabName = getTableName(schemaName, table, null, false);
		String onlyTabName = getTableName(null,       table, null, false);

		String q = getQuotedIdentifierChar();
		String sql = "";
		
		if ( ! isDdlCreated(fullTabName) )
		{
			// Obtain a DatabaseMetaData object from our current connection        
			DatabaseMetaData dbmd = conn.getMetaData();
	
			ResultSet rs = dbmd.getColumns(null, schemaName, onlyTabName, "%");
			boolean tabExists = rs.next();
			rs.close();
	
			if( tabExists )
			{
				// Check some various things.
				if (Table.CENTRAL_VERSION_INFO.equals(table))
				{
					// FIXME: check if "VersionString" is the same as Version.getVersionStr()
					//        if not, just throw a WARNING message to the log
					int currentDbVersion = -1;
					sql = "select "+q+"DbVersion"+q+" from "+q+onlyTabName+q; // possibly: where ProductString = Version.getAppName()
					Statement stmnt = conn.createStatement();
					rs = stmnt.executeQuery(sql);
					while (rs.next())
						currentDbVersion = rs.getInt(1);
					rs.close();
					stmnt.close();

					String infoStr = (currentDbVersion == DBX_CENTRAL_DB_VERSION) ? "which is the latest version." : "latest version is "+DBX_CENTRAL_DB_VERSION+", so we need to take actions...";
					_logger.info("Internal Dbx Central database version number is "+currentDbVersion+", "+infoStr);

					if (currentDbVersion != DBX_CENTRAL_DB_VERSION)
					{
						_logger.warn("Internal Dbx Central database version is not in sync. It needs to be upgraded from current version "+currentDbVersion+" to "+DBX_CENTRAL_DB_VERSION);

						// Upgrade to later DBX_CENTRAL_LAYOUT
						int newDbxCentralVersion = internalDbUpgrade(conn, currentDbVersion, DBX_CENTRAL_DB_VERSION);
						
						if (newDbxCentralVersion != currentDbVersion)
						{
							sql = "update " + q+onlyTabName+q + "set " + q+"DbVersion"+q+ " = " + newDbxCentralVersion + " where " + q+"DbVersion"+q+ " = " + currentDbVersion;
							stmnt = conn.createStatement();
							stmnt.executeUpdate(sql);
							stmnt.close();
							
							if (newDbxCentralVersion > currentDbVersion)
								_logger.info("Upgraded Dbx Central database tables from version '"+currentDbVersion+"' to version '"+newDbxCentralVersion+"'.");
							else
								_logger.info("Downgraded Dbx Central database tables from version '"+currentDbVersion+"' to version '"+newDbxCentralVersion+"'.");
						}
					}
				}
				
				// FIXME: Check if all desired columns exists
//				xxx: check how I do this in the other PCS
			}
			else
			{
				_logger.info("Creating table '" + fullTabName + "'.");
				getStatistics().incCreateTables();
				
				// Create the table
				sql = getTableDdlString(schemaName, table, null);
				dbDdlExec(conn, sql);

				// Create indexes
				sql = getIndexDdlString(schemaName, table, null);
				if (sql != null)
				{
					dbDdlExec(conn, sql);
				}
				
				// Do some extra stuff after table creation
				if (Table.CENTRAL_VERSION_INFO.equals(table))
				{
					String dbProductName = "unknown";
					try { dbProductName = conn.getDatabaseProductName(); }
					catch(Throwable t) { _logger.warn("Problems getting Database Product Name from the Storage connection. Caught: "+t); }
					
					StringBuffer sbSql = new StringBuffer();
					sbSql.append(getTableInsertStr(schemaName, Table.CENTRAL_VERSION_INFO, null, false));
					sbSql.append(" values(");
					sbSql.append("  '").append(Version.getAppName()       ).append("'");
					sbSql.append(", '").append(Version.getVersionStr()    ).append("'");
					sbSql.append(", '").append(Version.getBuildStr()      ).append("'");
					sbSql.append(",  ").append(DBX_CENTRAL_DB_VERSION     );
					sbSql.append(", '").append(dbProductName              ).append("'");
					sbSql.append(" )");
					sql = sbSql.toString();
					
					try
					{
						conn.dbExec(sql);
						getStatistics().incInserts();
					}
					catch(SQLException ex)
					{
						_logger.warn("Problems inserting values to '"+getTableName(schemaName, Table.CENTRAL_VERSION_INFO, null, false)+"'. Continuing anyway. sql='"+sql+"', Caught: "+ex);
					}
				}

				// Insert some default values into the table
				if (Table.CENTRAL_GRAPH_PROFILES.equals(table))
				{
					List<String> list = new ArrayList<>();

					// AseTune
					StringBuffer sbSql = new StringBuffer();
					sbSql.append(getTableInsertStr(schemaName, Table.CENTRAL_GRAPH_PROFILES, null, false));
					sbSql.append(" values(");
					sbSql.append("  '").append("AseTune").append("'");
					sbSql.append(", '").append("").append("'"); // UserName           '' = default
					sbSql.append(", '").append("").append("'"); // ProfileName        '' = default
					sbSql.append(", '").append("").append("'"); // ProfileDescription '' = default
					sbSql.append(", '").append("[{\"graph\":\"CmSummary_aaCpuGraph\"},{\"graph\":\"CmSummary_aaReadWriteGraph\"},{\"graph\":\"CmSummary_OldestTranInSecGraph\"},{\"graph\":\"CmSummary_SumLockCountGraph\"},{\"graph\":\"CmSummary_BlockingLocksGraph\"},{\"graph\":\"CmProcessActivity_BatchCountGraph\"},{\"graph\":\"CmProcessActivity_ExecTimeGraph\"},{\"graph\":\"CmSpidWait_spidClassName\"},{\"graph\":\"CmSpidWait_spidWaitName\"},{\"graph\":\"CmExecutionTime_TimeGraph\"},{\"graph\":\"CmSysLoad_EngineRunQLengthGraph\"},{\"graph\":\"CmStatementCache_RequestPerSecGraph\"},{\"graph\":\"CmOsIostat_IoWait\"},{\"graph\":\"CmOsUptime_AdjLoadAverage\"}]").append("'");
					sbSql.append(", '").append("").append("'"); // ProfileUrlOptions  '' = default
					sbSql.append(" )");
					sql = sbSql.toString();
					list.add(sbSql.toString());

					// SqlServerTune
//					sbSql = new StringBuffer();
//					sbSql.append(getTableInsertStr(schemaName, Table.CENTRAL_GRAPH_PROFILES, null, false));
//					sbSql.append(" values(");
//					sbSql.append("  '").append("SqlServerTune").append("'");
//					sbSql.append(", '").append(""       ).append("'");
//					sbSql.append(", '").append("default").append("'");
//					sbSql.append(", '").append("[{\"graph\":\"CmSummary_aaCpuGraph\"},{\"graph\":\"CmSummary_OldestTranInSecGraph\"},{\"graph\":\"CmSummary_SumLockCountGraph\"},{\"graph\":\"CmSummary_BlockingLocksGraph\"},{\"graph\":\"CmProcessActivity_BatchCountGraph\"},{\"graph\":\"CmProcessActivity_ExecTimeGraph\"},{\"graph\":\"CmSpidWait_spidClassName\"},{\"graph\":\"CmSpidWait_spidWaitName\"},{\"graph\":\"CmExecutionTime_TimeGraph\"},{\"graph\":\"CmSysLoad_EngineRunQLengthGraph\"},{\"graph\":\"CmStatementCache_RequestPerSecGraph\"},{\"graph\":\"CmOsIostat_IoWait\"},{\"graph\":\"CmOsUptime_AdjLoadAverage\"}]").append("'");
//					sbSql.append(" )");
//					sql = sbSql.toString();
//					list.add(sbSql.toString());

					// Insert the records
					for (String insertStmnt : list)
					{
						sql = insertStmnt;
						try
						{
							_logger.warn("Inserting base data into '"+getTableName(schemaName, Table.CENTRAL_GRAPH_PROFILES, null, false)+"'. Using SQL: "+sql);
							conn.dbExec(sql);
							getStatistics().incInserts();
						}
						catch(SQLException ex)
						{
							_logger.warn("Problems inserting values to '"+getTableName(schemaName, Table.CENTRAL_GRAPH_PROFILES, null, false)+"'. Continuing anyway. sql='"+sql+"', Caught: "+ex);
						}
					}
				}
			}
			
			markDdlAsCreated(fullTabName);

			return true;
		}
		return false;
	}

	/**
	 * Initialize some common tables, that may/will be used from some other modules. For example the CentralPcsReader
	 */
	public void checkAndCreateCommonTables()
	throws Exception
	{
		// Open connection to db
		_logger.info("Open initial connection to Checking/creating common DbxCentral tables. ");
		open(null);

		DbxConnection conn = _mainConn;
		if (conn == null)
		{
			_logger.error("No database connection to Persistent Storage DB, in checkAndCreateCommonTables().'");
			return;
		}

		_logger.info("Checking/creating common DbxCentral tables. ");

		// Check/Create
		checkAndCreateTable(conn, null,       Table.CENTRAL_VERSION_INFO);
		checkAndCreateTable(conn, null,       Table.CENTRAL_SESSIONS);
		checkAndCreateTable(conn, null,       Table.CENTRAL_GRAPH_PROFILES);
		
		// Close connection to db
		close();
	}

	private int internalDbUpgrade(DbxConnection conn, int fromDbVersion, int toDbVersion)
	throws SQLException
	{
		if (fromDbVersion > toDbVersion)
		{
			_logger.warn("----------------------------------------------------------------------------");
			_logger.warn("------------ Internal database DOWNGRADE is not-yet-implemented ------------");
			_logger.warn("----------------------------------------------------------------------------");
			return fromDbVersion;
		}

		String q    = getQuotedIdentifierChar();
		String sql  = "";
		int    step = -1;

		_logger.info("Start - Internal Upgrade of Dbx Central database tables from version '"+fromDbVersion+"' to version '"+toDbVersion+"'.");

		if (fromDbVersion <= 1)
		{
			//--------------------------------
			step = 1;
			// Add column 'CollectorSampleInterval' to table 'DbxCentralSessions'
			String onlyTabName = getTableName(null, Table.CENTRAL_SESSIONS, null, false);
			sql = "alter table " + q+onlyTabName+q + " add column " + q+"CollectorSampleInterval"+q + " int null"; // NOTE: 'not null' is not supported at upgrades
			internalDbUpgradeDdlExec(conn, step, sql);
		}

		if (fromDbVersion <= 2)
		{
			//--------------------------------
			step = 2;
			// Add column 'ProfileDescription' to table 'DbxCentralGraphProfiles'
			String onlyTabName = getTableName(null, Table.CENTRAL_GRAPH_PROFILES, null, false);
			sql = "alter table " + q+onlyTabName+q + " add column " + q+"ProfileDescription"+q + " text null"; // NOTE: 'not null' is not supported at upgrades
			internalDbUpgradeDdlExec(conn, step, sql);
		}

		if (fromDbVersion <= 3)
		{
			//--------------------------------
			step = 3;
			// Add column 'CollectorCurrentUrl' to table 'DbxCentralSessions'
			String onlyTabName = getTableName(null, Table.CENTRAL_SESSIONS, null, false);
			sql = "alter table " + q+onlyTabName+q + " add column " + q+"CollectorCurrentUrl"+q + " varchar(80) null"; // NOTE: 'not null' is not supported at upgrades
			internalDbUpgradeDdlExec(conn, step, sql);

			step = 4;
			// Add column 'Status' to table 'DbxCentralSessions'
			sql = "alter table " + q+onlyTabName+q + " add column " + q+"Status"+q + " int null"; // NOTE: 'not null' is not supported at upgrades
			internalDbUpgradeDdlExec(conn, step, sql);

			step = 5;
			// Set column 'Status' to be 0
			sql = "update " + q+onlyTabName+q + " set " + q+"Status"+q + " = 0"; 
			internalDbUpgradeDdlExec(conn, step, sql);
		}

		if (fromDbVersion <= 4)
		{
			//--------------------------------
			step = 6;
			// Add column 'CollectorInfoFile' to table 'DbxCentralSessions'
			String onlyTabName = getTableName(null, Table.CENTRAL_SESSIONS, null, false);
			sql = "alter table " + q+onlyTabName+q + " add column " + q+"CollectorInfoFile"+q + " varchar(256) null"; // NOTE: 'not null' is not supported at upgrades
			internalDbUpgradeDdlExec(conn, step, sql);
		}
		
		if (fromDbVersion <= 5)
		{
			//--------------------------------
			step = 7;
			// Add column 'category' to table 'ALARM_ACTIVE, ALARM_HISTORY' in all "user" schemas
			
			// Get schemas
			Set<String> schemaSet = new LinkedHashSet<>();
			ResultSet schemaRs = conn.getMetaData().getSchemas();
			while (schemaRs.next())
				schemaSet.add(schemaRs.getString(1));
			schemaRs.close();

			// Loop schemas: if table exists
			for (String schemaName : schemaSet)
			{
				internalDbUpgradeAlarmActiveHistory(conn, step, schemaName, Table.ALARM_ACTIVE);
				internalDbUpgradeAlarmActiveHistory(conn, step, schemaName, Table.ALARM_HISTORY);
			}
		}

		if (fromDbVersion <= 6)
		{
			//--------------------------------
			step = 8;
			// Add column 'ProfileDescription' to table 'DbxCentralGraphProfiles'
			String onlyTabName = getTableName(null, Table.CENTRAL_GRAPH_PROFILES, null, false);
			sql = "alter table " + q+onlyTabName+q + " add column " + q+"ProfileUrlOptions"+q + " varchar(1024) null"; // NOTE: 'not null' is not supported at upgrades
			internalDbUpgradeDdlExec(conn, step, sql);
		}

		if (fromDbVersion <= 7)
		{
			//--------------------------------
			step = 9;

			// Get schemas
			Set<String> schemaSet = new LinkedHashSet<>();
			ResultSet schemaRs = conn.getMetaData().getSchemas();
			while (schemaRs.next())
				schemaSet.add(schemaRs.getString(1));
			schemaRs.close();

			// Loop schemas: if table exists in schema, make the alter
			//               in some schemas, the table simply do not exists (H2 INFORMATION for example)
			for (String schemaName : schemaSet)
			{
    			// Add column 'GraphCategory' to table 'DbxGraphProperties' in all "user" schemas
    			String onlyTabName = getTableName(null, Table.GRAPH_PROPERTIES, null, false);

    			// get all columns, this to check if TABLE EXISTS
    			Set<String> colNames = new LinkedHashSet<>();
    			ResultSet colRs = conn.getMetaData().getColumns(null, schemaName, onlyTabName, "%");
    			while (colRs.next())
    				colNames.add(colRs.getString("COLUMN_NAME").toLowerCase()); // to lowercase in case the DBMS stores them in-another-way
    			colRs.close();

    			// IF the table TABLE EXISTS 'GraphCategory' do NOT exists, add it
    			if ( colNames.size() > 0 )
    			{
    				if ( ! colNames.contains("GraphCategory"))
    				{
    					sql = "alter table " + q+schemaName+q + "." + q + onlyTabName + q + " add column " + q+"GraphCategory"+q + " varchar(30) null"; // NOTE: 'not null' is not supported at upgrades
    					internalDbUpgradeDdlExec(conn, step, sql);
    				}
    			}
			}
		}

		_logger.info("End - Internal Upgrade of Dbx Central database tables from version '"+fromDbVersion+"' to version '"+toDbVersion+"'.");
		return toDbVersion;
	}

	private void internalDbUpgradeAlarmActiveHistory(DbxConnection conn, int step, String schemaName, Table table) 
	throws SQLException
	{
		String q = getQuotedIdentifierChar();

		// get table name
		String onlyTabName = getTableName(null, table, null, false);

		// get all columns
		Set<String> colNames = new LinkedHashSet<>();
		ResultSet colRs = conn.getMetaData().getColumns(null, schemaName, onlyTabName, "%");
		while (colRs.next())
			colNames.add(colRs.getString("COLUMN_NAME").toLowerCase()); // to lowercase in case the DBMS stores them in-another-way
		colRs.close();

		// IF the table exists & column 'category' do NOT exists, add it
		if ( colNames.size() > 0 )
		{
			if ( ! colNames.contains("category"))
			{
				String sql = "alter table " + q+schemaName+q + "." + q+onlyTabName+q + " add column " + q+"category"+q + " varchar(20) null"; // NOTE: 'not null' is not supported at upgrades
				internalDbUpgradeDdlExec(conn, step, sql);
			}
		}
	}

	private void internalDbUpgradeDdlExec(DbxConnection conn, int step, String sql)
	throws SQLException
	{
		_logger.info("Internal Dbx Cental Db upgrade, step["+step+"]: Executing SQL: "+sql);
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
			s.execute(sql);
			s.close();

			if (autoCommitWasChanged)
			{
				conn.setAutoCommit(false);
			}
		}
		catch(SQLException e)
		{
			_logger.warn("Problems during upgrade step["+step+"] of Internal Dbx Central Database when executing DDL sql statement: "+sql);
			throw e;
		}
	}

	
	/**
	 * Called from the {@link CentralPcsWriterHandler#consume} to start a session if it's needed.
	 * @see CentralPcsWriterHandler#consume
	 */
	@Override
	public void startSession(String sessionName, DbxTuneSample cont)
	throws Exception
	{
		// Open connection to db
		open(cont);

		DbxConnection conn = _mainConn;
		if (conn == null)
		{
			_logger.error("No database connection to Persistent Storage DB (in startSession).'");
			return;
		}

//		if (cont._counterObjects == null)
//		{
//			_logger.error("Input parameter PersistContainer._counterObjects can't be null. Can't continue startSession()...");
//			return;
//		}

		
		try
		{
//			String schemaName = cont.getServerName();
			String schemaName = sessionName;
			_logger.info("Starting a new Storage Session for '"+schemaName+"' with the start date '"+cont.getSessionStartTime()+"'.");

			// Create a schema
			conn.createSchemaIfNotExists(schemaName);

			//
			// FIRST CHECK IF THE TABLE EXISTS, IF NOT CREATE IT
			//
			checkAndCreateTable(conn, null,       Table.CENTRAL_VERSION_INFO);
			checkAndCreateTable(conn, null,       Table.CENTRAL_SESSIONS);
			checkAndCreateTable(conn, null,       Table.CENTRAL_GRAPH_PROFILES);
//			checkAndCreateTable(conn, schemaName, Table.SESSION_PARAMS);
			checkAndCreateTable(conn, schemaName, Table.SESSION_SAMPLES);
			checkAndCreateTable(conn, schemaName, Table.SESSION_SAMPLE_SUM);
			checkAndCreateTable(conn, schemaName, Table.SESSION_SAMPLE_DETAILS);
			checkAndCreateTable(conn, schemaName, Table.GRAPH_PROPERTIES);
//			checkAndCreateTable(conn, schemaName, Table.SESSION_MON_TAB_DICT);
//			checkAndCreateTable(conn, schemaName, Table.SESSION_MON_TAB_COL_DICT);
//			checkAndCreateTable(conn, schemaName, Table.SESSION_DBMS_CONFIG);
//			checkAndCreateTable(conn, schemaName, Table.SESSION_DBMS_CONFIG_TEXT);
//			checkAndCreateTable(conn, schemaName, Table.DDL_STORAGE);
//			checkAndCreateTable(conn, schemaName, Table.SQL_CAPTURE_SQLTEXT);
//			checkAndCreateTable(conn, schemaName, Table.SQL_CAPTURE_STATEMENTS);
//			checkAndCreateTable(conn, schemaName, Table.SQL_CAPTURE_PLANS);
			checkAndCreateTable(conn, schemaName, Table.ALARM_ACTIVE);
			checkAndCreateTable(conn, schemaName, Table.ALARM_HISTORY);

			// Create tables for SQL Capture... they can have more (or less) tables than SQL_CAPTURE_SQLTEXT, SQL_CAPTURE_STATEMENTS, SQL_CAPTURE_PLANS
//			if (CentralPcsWriterHandler.hasInstance())
//			{
//				ISqlCaptureBroker sqlCapBroker = CentralPcsWriterHandler.getInstance().getSqlCaptureBroker();
//				if (sqlCapBroker != null)
//				{
//					List<String> tableNames = sqlCapBroker.getTableNames();
//					for (String tabName : tableNames)
//					{
//						checkAndCreateTable(conn, schemaName, tabName, sqlCapBroker);
//					}
//				}
//			}
			
			//--------------------------
			// Now fill in some data
			// NOTE: Table.CENTRAL_VERSION_INFO data is inserted in checkAndCreateTable()
			
//			String tabName = getTableName(VERSION_INFO, null, true);

//			String dbProductName = "unknown";
//			try { dbProductName = CounterController.getInstance().getMonConnection().getDatabaseProductName(); }
//			catch(Throwable t) { _logger.warn("Problems getting Database Product Name from the monitor connection. Caught: "+t); }
			
//			StringBuffer sbSql = new StringBuffer();
//			sbSql.append(getTableInsertStr(schemaName, Table.VERSION_INFO, null, false));
//			sbSql.append(" values(");
//			sbSql.append("  '").append(cont.getSessionStartTime() ).append("'");
//			sbSql.append(", '").append(Version.getAppName()       ).append("'");
//			sbSql.append(", '").append(Version.getVersionStr()    ).append("'");
//			sbSql.append(", '").append(Version.getBuildStr()      ).append("'");
//			sbSql.append(", '").append(Version.getSourceDate()    ).append("'");
//			sbSql.append(",  ").append(Version.getSourceRev()     ).append(" ");
//			sbSql.append(" )");
//			StringBuffer sbSql = new StringBuffer();
//			sbSql.append(getTableInsertStr(schemaName, Table.VERSION_INFO, null, false));
//			sbSql.append(" values(");
//			sbSql.append("  '").append(cont.getSessionStartTime() ).append("'");
//			sbSql.append(", '").append(cont.getAppName()          ).append("'");
//			sbSql.append(", '").append(cont.getAppVersion()       ).append("'");
//			sbSql.append(", '").append(cont.getAppBuildStr()      ).append("'");
//			sbSql.append(" )");
//
//			try
//			{
//				conn.dbExec(sbSql.toString());
//				getStatistics().incInserts();
//			}
//			catch(SQLException ex)
//			{
//				_logger.warn("Problems inserting values to '"+getTableName(schemaName, Table.VERSION_INFO, null, false)+"'. Continuing anyway. sql='"+sbSql.toString()+"', Caught: "+ex);
//			}

			String sql = "";
			StringBuffer sbSql;

			// First check if the value exists in CENTRAL_SESSIONS... THEN do the Insert...
			// This because if the Central Instance restarts and the collector has not been restarted, a valid session already exists
			boolean sessionExists = false;
			sbSql = new StringBuffer();
			sbSql.append(" select * \n");
			sbSql.append(" from ").append(getTableName(schemaName, Table.CENTRAL_SESSIONS, null, true)).append("\n");
			sbSql.append(" where ").append(_qic).append("SessionStartTime").append(_qic).append(" = '").append(cont.getSessionStartTime()).append("'").append("\n");

			sql = sbSql.toString();
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
					sessionExists = true;
			}
			catch(SQLException ex)
			{
				_logger.warn("Problems getting/checking existence from table '"+getTableName(schemaName, Table.CENTRAL_SESSIONS, null, false)+"'. Continuing anyway. sql='"+sql+"', Caught: "+ex);
			}
			
			// Now insert the record to CENTRAL_SESSIONS if it do NOT exist
			if ( ! sessionExists )
			{
				//StringBuffer sbSql = new StringBuffer();
				sbSql = new StringBuffer();
				sbSql.append(getTableInsertStr(schemaName, Table.CENTRAL_SESSIONS, null, false));
				sbSql.append(" values(");
				sbSql.append("  ").append(DbUtils.safeStr(cont.getSessionStartTime()       ));
				sbSql.append(", ").append(DbUtils.safeStr(0                                )); // Status is not in the container, so always add 0
				sbSql.append(", ").append(DbUtils.safeStr(cont.getServerName()             ));
				sbSql.append(", ").append(DbUtils.safeStr(cont.getOnHostname()             ));
				sbSql.append(", ").append(DbUtils.safeStr(cont.getAppName()                ));
				sbSql.append(", ").append(DbUtils.safeStr(cont.getAppVersion()             ));
				sbSql.append(", ").append(DbUtils.safeStr(cont.getAppBuildStr()            ));
				sbSql.append(", ").append(DbUtils.safeStr(cont.getCollectorHostname()      ));
				sbSql.append(", ").append(DbUtils.safeStr(cont.getCollectorSampleInterval()));
				sbSql.append(", ").append(DbUtils.safeStr(cont.getCollectorCurrentUrl()    ));
				sbSql.append(", ").append(DbUtils.safeStr(cont.getCollectorInfoFile()      ));
				sbSql.append(", 0");     // NumOfSamples   always starts at 0
				sbSql.append(", null"); // LastSampleTime is null at start
				sbSql.append(")"); // end-of-values
				sql = sbSql.toString();
				
				try
				{
					conn.dbExec(sql);
					getStatistics().incInserts();
				}
				catch(SQLException ex)
				{
					_logger.warn("Problems inserting values to '"+getTableName(schemaName, Table.CENTRAL_SESSIONS, null, false)+"'. Continuing anyway. sql='"+sql+"', Caught: "+ex);
				}
			}
			

//			_logger.info("Storing CounterModel information in table "+getTableName(SESSION_PARAMS, null, false));
//			//--------------------------------
//			// LOOP ALL THE CM's and store some information
////			tabName = getTableName(SESSION_PARAMS, null, true);
//			Timestamp ts = cont.getSessionStartTime();
//
//			for (CountersModel cm : cont._counterObjects)
//			{
//				String prefix = cm.getName();
//
//				insertSessionParam(conn, ts, "cm", prefix+".name",     cm.getName());
//				insertSessionParam(conn, ts, "cm", prefix+".sqlInit",  cm.getSqlInit());
//				insertSessionParam(conn, ts, "cm", prefix+".sql",      cm.getSql());
//				insertSessionParam(conn, ts, "cm", prefix+".sqlClose", cm.getSqlClose());
//
//				insertSessionParam(conn, ts, "cm", prefix+".pk",       cm.getPk()==null ? null : cm.getPk().toString());
//				insertSessionParam(conn, ts, "cm", prefix+".diff",     Arrays.deepToString(cm.getDiffColumns()));
//				insertSessionParam(conn, ts, "cm", prefix+".diffDiss", Arrays.deepToString(cm.getDiffDissColumns()));
//				insertSessionParam(conn, ts, "cm", prefix+".pct",      Arrays.deepToString(cm.getPctColumns()));
//
//				insertSessionParam(conn, ts, "cm", prefix+".graphNames",Arrays.deepToString(cm.getTrendGraphNames()));
//			}
//			
//			_logger.info("Storing "+Version.getAppName()+" configuration information in table "+getTableName(SESSION_PARAMS, null, false));
//			//--------------------------------
//			// STORE the configuration file
//			Configuration conf;
//			conf = Configuration.getInstance(Configuration.SYSTEM_CONF);
//			if (conf != null)
//			{
//				for (String key : conf.getKeys())
//				{
//					String val = conf.getPropertyRaw(key);
//	
//					insertSessionParam(conn, ts, "system.config", key, val);
//				}
//			}
//
//			conf = Configuration.getInstance(Configuration.USER_CONF);
//			if (conf != null)
//			{
//				for (String key : conf.getKeys())
//				{
//					String val = conf.getPropertyRaw(key);
//	
//					insertSessionParam(conn, ts, "user.config", key, val);
//				}
//			}
//
//			conf = Configuration.getInstance(Configuration.USER_TEMP);
//			if (conf != null)
//			{
//				for (String key : conf.getKeys())
//				{
//					String val = conf.getPropertyRaw(key);
//	
//					// Skip some key values... just because they are probably to long...
//					if (key.indexOf(".gui.column.header.props") >= 0)
//						continue;
//
//					insertSessionParam(conn, ts, "temp.config", key, val);
//				}
//			}
//
//			Properties systemProps = System.getProperties();
//			for (Object key : systemProps.keySet())
//			{
//				String val = systemProps.getProperty(key.toString());
//
//				insertSessionParam(conn, ts, "system.properties", key.toString(), val);
//			}
//
//			// Storing the MonTablesDictionary(monTables & monTableColumns), 
//			// this so we can restore the proper Column ToolTip for this ASE version.
//			if (MonTablesDictionaryManager.hasInstance())
//				saveMonTablesDictionary(conn, MonTablesDictionaryManager.getInstance(), cont._sessionStartTime);
//
//			// Storing the AseConfig and AseCacheConfig
//   			if (DbmsConfigManager.hasInstance())
//   				saveDbmsConfig(conn, DbmsConfigManager.getInstance(), cont._sessionStartTime);
//    
//   			if (DbmsConfigTextManager.hasInstances())
//   				saveDbmsConfigText(conn, cont._sessionStartTime);

			// Mark that this session HAS been started.
			_logger.info("Marking the session as STARTED.");
			setSessionStarted(sessionName, true);
		}
		catch (SQLException e)
		{
			_logger.warn("Error when startSession() writing to Persistent Counter Store.", e);
			// throws Exception if it's a severe problem
			isSevereProblem(conn, e);
		}
		
		// Close connection to db
		close();
	}




	@Override
	public void close()
	{
		// Check if we have a connection, and if it's OK
		// On errors simply close/invalidate the connection
		if (_mainConn != null)
		{
			if (_mainConn.isConnectionOk())
			{
				// OK: Do nothing
			}
			else
			{
				// PROBLEM: close & set to null... a new connection will be attempted on next open()
				_logger.info("Closing the PCS Storage connection, since it's no longer valid.");
				_mainConn.closeNoThrow();
				_mainConn = null;
			}
		}
	}

	@Override
	public String getConfigStr()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void printConfig()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notification(NotificationType type, String notifyStr)
	{
		if (NotificationType.DROP_SERVER.equals(type))
		{
			_logger.info("Received notification '"+type+"'. I will remove schema '"+notifyStr+"' from DDL cache.");
			
			List<String> removeEntries = new ArrayList<>();
			for (String str : _ddlSet)
			{
				if (str.startsWith(notifyStr + ".") || str.startsWith(_qic + notifyStr + _qic + "."))
					removeEntries.add(str);
			}
			_ddlSet.removeAll(removeEntries);
		}
	}

	@Override
	public void saveSample(DbxTuneSample cont)
	throws Exception
	{
		DbxConnection conn = _mainConn;
		String qic = getQuotedIdentifierChar();
		
		if (conn == null)
		{
			_logger.error("No database connection to Persistent Storage DB (in saveSample).'");
			return;
		}
//		if (_shutdownWithNoWait)
//		{
//			_logger.info("Save Sample: Discard entry due to 'ShutdownWithNoWait'.");
//			return;
//		}


		String    schemaName          = cont.getServerName();
		Timestamp sessionStartTime    = cont.getSessionStartTime();
		Timestamp sessionSampleTime   = cont.getSessionSampleTime();
		String    collectorCurrentUrl = cont.getCollectorCurrentUrl();

		
		StringBuffer sbSql = null;

		try
		{
			// STATUS, that we are saving right now
			_inSaveSample = true;

			// Save any alarms
			saveAlarms(conn, schemaName, sessionStartTime, sessionSampleTime, cont);

			// In case the DbxTuneSample/container only contains alarms (which only happends if it's a SrvDown Alarm)
			// Then: the collectors List is EMPTY, so do not try to save stuff
			// ----
			// SO: only save "stuff" in the DbxTune MetaData tables if we have any actual data to save...
			if ( ! cont.getCollectors().isEmpty() )
			{
				// START a transaction
				// This will lower number of IO's to the transaction log
				if (conn.getAutoCommit() == true)
					conn.setAutoCommit(false);

				//
				// INSERT THE ROW
				//
				sbSql = new StringBuffer();
				sbSql.append(getTableInsertStr(schemaName, Table.SESSION_SAMPLES, null, false));
				sbSql.append(" values('").append(sessionStartTime).append("', '").append(sessionSampleTime).append("')");

				conn.dbExec(sbSql.toString());
				getStatistics().incInserts();

				// Increment the "counter" column and set LastSampleTime in the SESSIONS table
				String tabName = getTableName(schemaName, Table.CENTRAL_SESSIONS, null, true);
				sbSql = new StringBuffer();
				sbSql.append(" update ").append(tabName).append("\n");
				sbSql.append("    set  ").append(qic).append("NumOfSamples")       .append(qic).append(" = ").append(qic).append("NumOfSamples").append(qic).append(" + 1").append("\n");
				sbSql.append("        ,").append(qic).append("LastSampleTime")     .append(qic).append(" = '").append(sessionSampleTime).append("'").append("\n");

				if (StringUtil.hasValue(collectorCurrentUrl))
					sbSql.append("        ,").append(qic).append("CollectorCurrentUrl").append(qic).append(" = '").append(collectorCurrentUrl).append("'").append("\n");

				sbSql.append("  where ").append(qic).append("SessionStartTime")   .append(qic).append(" = '").append(sessionStartTime).append("'").append("\n");

				conn.dbExec(sbSql.toString());
				getStatistics().incUpdates();

				//--------------------------------------
				// COUNTERS
				//--------------------------------------
//				for (CountersModel cm : cont._counterObjects)
//				{
//					saveCounterData(conn, cm, sessionStartTime, sessionSampleTime);
//				}
				for (CmEntry cme : cont._collectors)
				{
					saveCounterData(conn, cme, schemaName, sessionStartTime, sessionSampleTime);
				}

				// check and save graph properties (like Graphs Order and Graph Headings/labels)
				// if we send more graphs than previously: then remove and insert the latest properties
				checkSaveGraphProperties(conn, cont, schemaName, sessionStartTime, sessionSampleTime);

				// CLOSE the transaction
				conn.commit();
			}
		}
		catch (SQLException e)
		{
			try 
			{
				if (conn.isConnectionOk() && conn.getAutoCommit() == true)
					conn.rollback();
			}
			catch (SQLException e2) {}

			_logger.warn("Error writing to Persistent Counter Store. getErrorCode()="+e.getErrorCode()+", SQL: "+sbSql.toString(), e);
			// throws Exception if it's a severe problem
			isSevereProblem(conn, e);
		}
		finally
		{
			try 
			{
				if (conn.isConnectionOk())
					conn.setAutoCommit(true); 
			}
			catch (SQLException e2) { _logger.error("Problems when setting AutoCommit to true.", e2); }

			_inSaveSample = false;
		}
	}

	private void saveAlarms(DbxConnection conn, String schemaName, Timestamp sessionStartTime, Timestamp sessionSampleTime, DbxTuneSample cont)
	throws SQLException
	{
//		System.out.println("saveAlarms(schemaName='"+schemaName+"', sessionStartTime='"+sessionStartTime+"', sessionSampleTime='"+sessionSampleTime+"'): ActiveAlarmCnt="+cont.getActiveAlarms().size()+", AlarmEntriesCnt="+cont.getAlarmEntries().size());

		// Delete all ACTIVE alarms
		String sql = "delete from " + getTableName(schemaName, Table.ALARM_ACTIVE, null, true);
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
		if ( ! cont.getActiveAlarms().isEmpty() )
		{
			for (AlarmEntry ae : cont.getActiveAlarms())
			{
				// Store some info
				StringBuilder sbSql = new StringBuilder();

				try
				{
					sbSql.append(getTableInsertStr(schemaName, Table.ALARM_ACTIVE, null, false));
					sbSql.append(" values(");
					sbSql.append("  ").append(safeStr( ae.getAlarmClassAbriviated() ));
					sbSql.append(", ").append(safeStr( ae.getServiceType() ));
					sbSql.append(", ").append(safeStr( ae.getServiceName() ));
					sbSql.append(", ").append(safeStr( ae.getServiceInfo() ));
					sbSql.append(", ").append(safeStr( ae.getExtraInfo() ));
					sbSql.append(", ").append(safeStr( ae.getCategory() ));
					sbSql.append(", ").append(safeStr( ae.getSeverity() ));
					sbSql.append(", ").append(safeStr( ae.getState() ));
					sbSql.append(", ").append(safeStr( ae.getRepeatCnt() ));
					sbSql.append(", ").append(safeStr( ae.getDuration() ));
					sbSql.append(", ").append(safeStr( ae.getCreationTime() ));
					sbSql.append(", ").append(safeStr( ae.getCancelTime() ));
					sbSql.append(", ").append(safeStr( ae.getTimeToLive() ));
					sbSql.append(", ").append(safeStr( ae.getThreshold() ));
					sbSql.append(", ").append(safeStr( ae.getData() ));
					sbSql.append(", ").append(safeStr( ae.getReRaiseData() ));
					sbSql.append(", ").append(safeStr( ae.getDescription() ));
					sbSql.append(", ").append(safeStr( ae.getReRaiseDescription() ));
					sbSql.append(", ").append(safeStr( ae.getExtendedDescription() ));
					sbSql.append(", ").append(safeStr( ae.getReRaiseExtendedDescription() ));
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
		if ( ! cont.getAlarmEntries().isEmpty() )
		{
			for (AlarmEntryWrapper aew : cont.getAlarmEntries())
			{
				// do not store some stuff
				if ( ! _alarmEventActionStore.contains(aew.getAction()) )
					continue;
				
				AlarmEntry ae = aew.getAlarmEntry();
				
				// Store some info
				StringBuilder sbSql = new StringBuilder();

				try
				{
					sbSql.append(getTableInsertStr(schemaName, Table.ALARM_HISTORY, null, false));
					sbSql.append(" values(");
					sbSql.append("  ").append(safeStr( sessionStartTime ));
					sbSql.append(", ").append(safeStr( sessionSampleTime ));

					sbSql.append(", ").append(safeStr( aew.getEventTime() ));
					sbSql.append(", ").append(safeStr( aew.getAction() ));

					sbSql.append(", ").append(safeStr( ae.getAlarmClassAbriviated() ));
					sbSql.append(", ").append(safeStr( ae.getServiceType() ));
					sbSql.append(", ").append(safeStr( ae.getServiceName() ));
					sbSql.append(", ").append(safeStr( ae.getServiceInfo() ));
					sbSql.append(", ").append(safeStr( ae.getExtraInfo() ));
					sbSql.append(", ").append(safeStr( ae.getCategory() ));
					sbSql.append(", ").append(safeStr( ae.getSeverity() ));
					sbSql.append(", ").append(safeStr( ae.getState() ));
					sbSql.append(", ").append(safeStr( ae.getRepeatCnt() ));
					sbSql.append(", ").append(safeStr( ae.getDuration() ));
					sbSql.append(", ").append(safeStr( ae.getCreationTime() ));
					sbSql.append(", ").append(safeStr( ae.getCancelTime() ));
					sbSql.append(", ").append(safeStr( ae.getTimeToLive() ));
					sbSql.append(", ").append(safeStr( ae.getThreshold() ));
					sbSql.append(", ").append(safeStr( ae.getData() ));
					sbSql.append(", ").append(safeStr( ae.getReRaiseData() ));
					sbSql.append(", ").append(safeStr( ae.getDescription() ));
					sbSql.append(", ").append(safeStr( ae.getReRaiseDescription() ));
					sbSql.append(", ").append(safeStr( ae.getExtendedDescription() ));
					sbSql.append(", ").append(safeStr( ae.getReRaiseExtendedDescription() ));
					sbSql.append(")");

					sql = sbSql.toString();
					conn.dbExec(sql);
					getStatistics().incInserts();
				}
				catch (SQLException e)
				{
					_logger.warn("Error writing History Alarm Event(s) to Persistent Counter Store. getErrorCode()="+e.getErrorCode()+", SQL: "+sql, e);
					// throws Exception if it's a severe problem
					isSevereProblem(conn, e);
				}
			}
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
//	public static String safeStr(Object obj)
//	{
//		if (obj == null)
//			return "NULL";
//
//		if (obj instanceof Number)
//		{
//			return obj.toString();
//		}
//		else
//		{
//			String str = obj.toString();
//    		StringBuilder sb = new StringBuilder();
//    
//    		// add ' around the string...
//    		// and replace all ' into ''
//    		sb.append("'");
//    		sb.append(str.replace("'", "''"));
//    		sb.append("'");
//    		return sb.toString();
//		}
//	}
	public static String safeStr(Object obj)
	{
		return DbUtils.safeStr(obj);
	}

	private void saveCounterData(DbxConnection conn, CmEntry cme, String schemaName, Timestamp sessionStartTime, Timestamp sessionSampleTime)
	throws SQLException
	{
		String q = getQuotedIdentifierChar();

		if (cme == null)
		{
			_logger.debug("saveCounterData: cme == null.");
			return;
		}

//		if (cme instanceof CountersModelAppend) 
//			return;

		if (_shutdownWithNoWait)
		{
			_logger.info("SaveCounterData: Discard entry due to 'ShutdownWithNoWait'.");
			return;
		}

		_logger.debug("Persisting Counters for CounterModel='"+cme.getName()+"'.");

		int counterType  = 0;
		int absRecvRows  = 0;
		int diffRecvRows = 0;
		int rateRecvRows = 0;
		int absSaveRows  = 0;
		int diffSaveRows = 0;
		int rateSaveRows = 0;
		if (cme.hasValidSampleData())
		{
			absRecvRows  = cme.getAbsCounters() .getRowCount();
			diffRecvRows = cme.getDiffCounters().getRowCount();
			rateRecvRows = cme.getRateCounters().getRowCount();
			
			if (_saveCounterData)
			{
				if (cme.hasAbsData() ) { counterType += 1; absSaveRows  = save(conn, cme, Table.ABS,  schemaName, sessionStartTime, sessionSampleTime); }
				if (cme.hasDiffData()) { counterType += 2; diffSaveRows = save(conn, cme, Table.DIFF, schemaName, sessionStartTime, sessionSampleTime); }
				if (cme.hasRateData()) { counterType += 4; rateSaveRows = save(conn, cme, Table.RATE, schemaName, sessionStartTime, sessionSampleTime); }
			}
		}
		
		if (_shutdownWithNoWait)
		{
			_logger.info("SaveCounterData:1: Discard entry due to 'ShutdownWithNoWait'.");
			return;
		}

		int graphRecvCount = 0;
		int graphSaveCount = 0;
		if (cme.hasValidSampleData())
		{
			Map<String, GraphEntry> tgdMap = cme._graphMap;
			if (tgdMap != null)
			{
				for (Map.Entry<String,GraphEntry> entry : tgdMap.entrySet()) 
				{
				//	String     key  = entry.getKey();
					GraphEntry tgdp = entry.getValue();

					graphRecvCount++;

//					saveGraphData(conn, cme, tgdp, schemaName, sessionStartTime, sessionSampleTime);
					graphSaveCount += saveGraphData(conn, cme, tgdp, schemaName, sessionStartTime, sessionSampleTime);
				}
			}
		}
		

		if (_shutdownWithNoWait)
		{
			_logger.info("saveCounterData:2: Discard entry due to 'ShutdownWithNoWait'.");
			return;
		}

		// Store some info
		StringBuilder sbSql = new StringBuilder();

		boolean save_SESSION_SAMPLE_DETAILS = getConfig().getBooleanProperty(PROPKEY_SAVE_SESSION_SAMPLE_DETAILS, DEFAULT_SAVE_SESSION_SAMPLE_DETAILS);;
		if (save_SESSION_SAMPLE_DETAILS)
		{
			try
			{
				sbSql.append(getTableInsertStr(schemaName, Table.SESSION_SAMPLE_DETAILS, null, false));
				sbSql.append(" values('").append(sessionStartTime).append("'");
				sbSql.append(", '").append(sessionSampleTime).append("'");
				sbSql.append(", '").append(cme.getName()).append("'");
				sbSql.append(", ") .append(counterType);
				sbSql.append(", ") .append(graphRecvCount);
				sbSql.append(", ") .append(absRecvRows);
				sbSql.append(", ") .append(diffRecvRows);
				sbSql.append(", ") .append(rateRecvRows);
				sbSql.append(", ") .append(graphSaveCount);
				sbSql.append(", ") .append(absSaveRows);
				sbSql.append(", ") .append(diffSaveRows);
				sbSql.append(", ") .append(rateSaveRows);
				sbSql.append(", ") .append(cme.getSqlRefreshTime());
				sbSql.append(", ") .append(cme.getGuiRefreshTime());
				sbSql.append(", ") .append(cme.getLcRefreshTime());
				sbSql.append(", ") .append(cme.hasNonConfiguredMonitoringHappened() ? 1 : 0);
				sbSql.append(", ") .append(safeStr(cme.getNonConfiguredMonitoringMissingParams()));
				sbSql.append(", ") .append(safeStr(cme.getNonConfiguredMonitoringMessage()));
				sbSql.append(", ") .append(cme.isCountersCleared() ? 1 : 0);
				sbSql.append(", ") .append(cme.hasValidSampleData() ? 1 : 0);
				sbSql.append(", ") .append(safeStr(cme.getSampleExceptionMsg()));
				sbSql.append(", ") .append(safeStr(cme.getSampleExceptionFullText())); 
				sbSql.append(")");

				conn.dbExec(sbSql.toString());
				getStatistics().incInserts();
			}
			catch (SQLException e)
			{
				_logger.warn("Error writing to Persistent Counter Store. getErrorCode()="+e.getErrorCode()+", SQL: "+sbSql.toString(), e);
				// throws Exception if it's a severe problem
				isSevereProblem(conn, e); 
			}
		}


		if (_shutdownWithNoWait)
		{
			_logger.info("saveCounterData:3: Discard entry due to 'ShutdownWithNoWait'.");
			return;
		}

		// SUMMARY INFO for the whole session
		String tabName = getTableName(schemaName, Table.SESSION_SAMPLE_SUM, null, true);

		sbSql = new StringBuilder();
		sbSql.append(" update ").append(tabName);
		sbSql.append(" set   "+q+"graphSamples"+q+" = "+q+"graphSamples"+q+" + ").append( (graphSaveCount > 0 ? 1 : 0) ).append(", ");
		sbSql.append("       "+q+"absSamples"+q+"   = "+q+"absSamples"+q+"   + ").append( (absSaveRows    > 0 ? 1 : 0) ).append(", ");
		sbSql.append("       "+q+"diffSamples"+q+"  = "+q+"diffSamples"+q+"  + ").append( (diffSaveRows   > 0 ? 1 : 0) ).append(", ");
		sbSql.append("       "+q+"rateSamples"+q+"  = "+q+"rateSamples"+q+"  + ").append( (rateSaveRows   > 0 ? 1 : 0) ).append("");
		sbSql.append(" where "+q+"SessionStartTime"+q+" = '").append(sessionStartTime).append("'");
		sbSql.append("   and "+q+"CmName"+q+" = '").append(cme.getName()).append("'");

		try
		{
			Statement stmnt = conn.createStatement();
			int updCount = stmnt.executeUpdate(sbSql.toString());
			
			if (updCount == 0)
			{
				sbSql = new StringBuilder();
				sbSql.append(getTableInsertStr(schemaName, Table.SESSION_SAMPLE_SUM, null, false));
				sbSql.append(" values('").append(sessionStartTime).append("'");
				sbSql.append(", '").append(cme.getName()).append("'");
				sbSql.append(", ").append(graphSaveCount > 0 ? 1 : 0);
				sbSql.append(", ").append(absSaveRows    > 0 ? 1 : 0);
				sbSql.append(", ").append(diffSaveRows   > 0 ? 1 : 0);
				sbSql.append(", ").append(rateSaveRows   > 0 ? 1 : 0);
				sbSql.append(")");

				updCount = stmnt.executeUpdate(sbSql.toString());
				getStatistics().incInserts();
			}
			else
			{
				getStatistics().incUpdates();
			}
		}
		catch (SQLException e)
		{
			_logger.warn("Error writing to Persistent Counter Store. getErrorCode()="+e.getErrorCode()+", SQL: "+sbSql.toString(), e);
			// throws Exception if it's a severe problem
			isSevereProblem(conn, e);
		}
	}

	private int save(DbxConnection conn, CmEntry cme, Table whatData, String schemaName, Timestamp sessionStartTime, Timestamp sessionSampleTime)
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
		if      (Table.ABS .equals(whatData)) { cmWhatData = CountersModel.DATA_ABS;  cmWhatDataStr = "ABS";  }
		else if (Table.DIFF.equals(whatData)) { cmWhatData = CountersModel.DATA_DIFF; cmWhatDataStr = "DIFF"; }
		else if (Table.RATE.equals(whatData)) { cmWhatData = CountersModel.DATA_RATE; cmWhatDataStr = "RATE"; }
		else
		{
			_logger.error("Type of data is unknown, only 'ABS', 'DIFF' and 'RATE' is handled.");
			return -1;
		}
System.out.println("save("+whatData+"): ----------------------------NOT-YET-IMPLEMENTED--------------------------------------");
return -1;
//		List<List<Object>> rows = cme.getDataCollection(cmWhatData);
//		List<String>       cols = cme.getColNames(cmWhatData);
//
//		if (rows == null || cols == null)
//		{
//			_logger.error("Rows or Columns can't be null. rows='"+rows+"', cols='"+cols+"'");
//			return -1;
//		}
//
//		String tabName = getTableName(schemaName, whatData, cme, false);
////		String tabName = cm.getName();
////		if      (whatData == CountersModel.DATA_ABS)  tabName += "_abs";
////		else if (whatData == CountersModel.DATA_DIFF) tabName += "_diff";
////		else if (whatData == CountersModel.DATA_RATE) tabName += "_rate";
////		else
////		{
////			_logger.error("Type of data is unknown, only 'ABS', 'DIFF' and 'RATE' is handled.");
////			return -1;
////		}
//
//		int rowsCount = rows.size();
//		int colsCount = cols.size();
//		
////System.out.println("Counter '"+cmWhatDataStr+"' data for CM '"+cm.getName()+"'. (rowsCount="+rowsCount+", colsCount="+colsCount+") cols="+cols);
//		if (rowsCount == 0 || colsCount == 0)
//		{
//			_logger.debug("Skipping Storing Counter '"+cmWhatDataStr+"' data for CM '"+cme.getName()+"'. Rowcount or column count is 0 (rowsCount="+rowsCount+", colsCount="+colsCount+")");
//			return 0;
//		}
//		
//		// First BUILD up SQL statement used for the insert
////		sqlSb.append("insert into ").append(qic).append(tabName).append(qic);
////		sqlSb.append(" values(?, ?, ?, ?");
////		for (int c=0; c<colsCount; c++)
////			sqlSb.append(", ?");
////		sqlSb.append(")");
//
//		ResultSetMetaData rsmd = null;
//		try
//		{
//			rsmd = cme.getResultSetMetaData();
//			
//			if ( rsmd != null && rsmd.getColumnCount() == 0 )
//				rsmd = null;
//		}
//		catch (SQLException ignore) { /*ignore*/ }
//		
//		// TODO: Instead of getting the CM.getResultSetMetaData() we might want to use
////		TableInfo storageTableInfo = new TableInfo(); // note TableInfo needs to be created
////		try
////		{
////			ResultSet colRs = conn.getMetaData().getColumns(null, null, tabName, "%");
////			while (colRs.next())
////			{
////				String colName = colRs.getString(4); // COLUMN_NAME 
////				int    colType = colRs.getInt   (5); // DATA_TYPE:  SQL type from java.sql.Types 
////				int    colSize = colRs.getInt   (7); // COLUMN_SIZE
////				
////				tableInfo.add(colName, colType, colSize);
////			}
////			colRs.close();
////		}
////		catch (SQLException ignore) { /*ignore*/ }
//
//		String sql = "";
//		try
//		{
//			sql = getTableInsertStr(schemaName, whatData, cme, true, cols);
////			PreparedStatement pstmt = conn.prepareStatement(sql);
////			PreparedStatement pstmt = PreparedStatementCache.getPreparedStatement(conn, sql);
//			PreparedStatement pstmt = _cachePreparedStatements ? PreparedStatementCache.getPreparedStatement(conn, sql) : conn.prepareStatement(sql);
//
//			// Loop all rows, and ADD them to the Prepared Statement
//			for (int r=0; r<rowsCount; r++)
//			{
//				if (_shutdownWithNoWait)
//				{
//					_logger.info("saveCm:inRowLoop: Discard entry due to 'ShutdownWithNoWait'.");
//					return -1;
//				}
//
//				int col = 1;
//				// Add sessionStartTime as the first column
////				pstmt.setTimestamp(col++, sessionStartTime);
//				pstmt.setString(col++, sessionStartTime.toString());
//
//				// Add sessionSampleTime as the first column
////				pstmt.setTimestamp(col++, sessionSampleTime);
//				pstmt.setString(col++, sessionSampleTime.toString());
//
//				// When THIS sample was taken
//				// probably the same time as parentSampleTime, but it can vary some milliseconds or so
////				pstmt.setTimestamp(col++, cm.getTimestamp());
//				pstmt.setString(col++, cme.getTimestamp().toString());
//
//				// How long the sample was for, in Milliseconds
//				pstmt.setInt(col++, cme.getLastSampleInterval());
//
//				// How long the sample was for, in Milliseconds
//				pstmt.setInt(col++, cme.isNewDeltaOrRateRow(r) ? 1 : 0);
//
//				// loop all columns
//				for (int c=0; c<colsCount; c++)
//				{
//					colObj =  rows.get(r).get(c);
//
//					// Timestamp is stored with appending nanoseconds etc in a strange format
//					// if you are using setObject() so use setString() instead...
//					if (colObj != null && colObj instanceof Timestamp)
//					{
//						// Also try to parse the date to see if it's ok...
//						// some timestamp seems to be "corrupt"...
//						Timestamp ts = (Timestamp) colObj;
//						String dateStr = colObj.toString();
//
//						Calendar cal = Calendar.getInstance();
//						cal.setTime(ts);
//						int year = cal.get(Calendar.YEAR);
//						if (year > 9999)
//						{
//							String colName = cme.getColumnName(c); // cm colname starts at 0
//							_logger.warn("Date problems for table '"+tabName+"', column '"+colName+"', Timestamp value '"+dateStr+"', Year seems to be out of whack, replacing this with NULL.");
//							pstmt.setString(col++, null);
//						}
//						else
//						{
//							pstmt.setString(col++, dateStr);
//						}
//					}
//					// STRING values, check if we need to truncate trailing/overflowing space. 
//					else if (colObj != null && colObj instanceof String)
//					{
//						String str = (String)colObj;
//
//						// TODO: instead of the cm.rsmd we can use "storageTableInfo"
//						// this needs to be implemented first
//
//						// if str length is longer than column length, truncate the value...
//						if (rsmd != null)
//						{
//							// Should we check for storage type as well???, lets skip this for now...
////							String typeDatatype = rsmd.getColumnTypeName( c + 1 );
////							if ( type.equals("char") || type.equals("varchar") )
////							int allowedLength = rsmd.getPrecision( c + 1 ); // seems to be zero for strings
//
//							// NOTE: column in JDBC starts at 1, not 0
//							int allowedLength = rsmd.getColumnDisplaySize( c + 1 ); // getColumnDisplaySize() is used when creating the tables, so this should hopefully work
//							int jdbcDataType  = rsmd.getColumnType(c + 1);
////							if (jdbcDataType == Types.BINARY || jdbcDataType == Types.VARBINARY)
////								allowedLength += 2; // binary may need the extra 2 chars if it's prefixed with a 0x
//
//							// If a hex string starts with 0x chop that off, H2 doesn't seem to like it
//							if (str != null && (jdbcDataType == Types.BINARY || jdbcDataType == Types.VARBINARY))
//							{
//								if (str.startsWith("0x"))
//									str = str.substring("0x".length());
//							}
//
//							if ( allowedLength > 0  &&  str != null  &&  str.length() > allowedLength )
//							{
//								int dataLength = str.length();
//								String colName = cols.get(c);
//
//								// Add '...' at the end if it's a long string, or simply "chop it of" if it's a very short string.
//								String truncStr = "";
//								if (allowedLength <= 3 || (jdbcDataType == Types.BINARY || jdbcDataType == Types.VARBINARY) ) // Binary data types can contain '...'
//									truncStr = str.substring(0, allowedLength);
//								else
//									truncStr = str.substring(0, allowedLength - 3) + "...";
//
//								_logger.info("save(): Truncating a Overflowing String value. table='"+tabName+"', column='"+colName+"', allowedLength="+allowedLength+", dataLength="+dataLength+", newStr["+truncStr.length()+"]='"+truncStr+"', originStr["+str.length()+"]='"+str+"'.");
//
//								str = truncStr;
//							}
//						}
//
//						pstmt.setString(col++, str);
//					}
//					else
//						pstmt.setObject(col++, colObj);
//				}
//				
//				// ADD the row to the BATCH
//				pstmt.addBatch();
//				getStatistics().incInserts();
//			} // end: loop rows
//	
//			if (_shutdownWithNoWait)
//			{
//				_logger.info("saveCm:exexuteBatch: Discard entry due to 'ShutdownWithNoWait'.");
//				return -1;
//			}
//
//			pstmt.executeBatch();
//			pstmt.close();
//
//			return rowsCount;
//		}
//		catch (SQLException e)
//		{
//			_logger.warn("Error writing to Persistent Counter Store. to table name '"+tabName+"'. getErrorCode()="+e.getErrorCode()+" SQL="+sql, e);
//			// throws Exception if it's a severe problem
//			isSevereProblem(conn, e);
//			return -1;
//		}
	}

	/** Helper method to get a table name for GRAPH tables */
	public static String getTableName(String schemaName, CmEntry cme, GraphEntry ge, boolean addQuotedIdentifierChar)
	{
		String q = "";
		if (addQuotedIdentifierChar)
			q = getQuotedIdentifierChar();

		String tabName = q + schemaName + q + "." + q + cme.getName() + "_" + ge.getName() + q;
		return tabName;
	}

	private int saveGraphData(DbxConnection conn, CmEntry cme, GraphEntry ge, String schemaName, Timestamp sessionStartTime, Timestamp sessionSampleTime)
	throws SQLException
	{
		if (cme == null) throw new IllegalArgumentException("The passed CmEntry can't be null");
		if (ge  == null) throw new IllegalArgumentException("The passed GraphEntry can't be null");

		String q = getQuotedIdentifierChar();
		String onlyTabName = cme.getName() + "_" + ge.getName();
		String fullTabName = q + schemaName + q + "." + q + cme.getName() + "_" + ge.getName() + q;

		if ( ! ge.hasData() )
		{
			_logger.debug("The graph '"+ge.getName()+"' has NO DATA for this sample time, so write will be skipped. TrendGraphDataPoint="+ge);
			return 0;
		}
		if (_shutdownWithNoWait)
		{
			_logger.info("saveGraphData: Discard entry due to 'ShutdownWithNoWait'.");
			return 0;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("insert into ").append(fullTabName);
//		String tabInsStr = getTableInsertStr(cm, tgdp, false);
//		sb.append(tabInsStr);
		sb.append("(");
		sb.append(q).append("SessionStartTime").append(q).append(", ");
		sb.append(q).append("SessionSampleTime").append(q).append(", ");
		sb.append(q).append("CmSampleTime").append(q);
		for (String label : ge._labelValue.keySet())
			sb.append(", ").append(q).append(label).append(q);
		sb.append(")\n");
			
		sb.append(" values(");


		// Add sessionStartTime as the first column
		sb.append("'").append(sessionStartTime).append("', ");

		// Add sessionSampleTime as the first column
		sb.append("'").append(sessionSampleTime).append("', ");

		sb.append("'").append(cme.getCmSampleTime()).append("'");

		// loop all data
		for (Double dataPoint : ge._labelValue.values())
			sb.append(", ").append(dataPoint);
		sb.append(")");
		
		String sqlInsertGraphData = sb.toString();

		//--------------------
		// Send the SQL to the database.
		//--------------------

		// CHECK/Create table
		try
		{
			if ( ! isDdlCreated(fullTabName) )
				saveGraphDataDdl(conn, schemaName, onlyTabName, ge);
			markDdlAsCreated(fullTabName);
		}
		catch (SQLException e)
		{
			_logger.info("Problems writing Graph '"+ge.getName()+"' information to table '"+fullTabName+"', Problems when creating the table or checked if it existed. Caught: "+e);
			// throws Exception if it's a severe problem
			isSevereProblem(conn, e);
		}

		// this call is self handled and do not throw any exception
//		saveGraphProperties(conn, schemaName, sessionStartTime, cme, ge);

		// Add rows...
		try
		{
			conn.dbExec(sqlInsertGraphData, false);
			getStatistics().incInserts();
			return 1;
		}
		catch (SQLException e)
		{
			_logger.info("Problems writing Graph '"+ge.getName()+"' information to table '"+fullTabName+"', This probably happens if series has been added to the graph, I will checking/create/alter the table and try again.");
			try
			{
				// we probably need to alter the table...
				saveGraphDataDdl(conn, schemaName, onlyTabName, ge);

				conn.dbExec(sqlInsertGraphData);
				getStatistics().incInserts();
				return 1;
			}
			catch (SQLException e2)
			{
				_logger.warn("Error writing to Persistent Counter Store. getErrorCode()="+e2.getErrorCode()+", getSQLState()="+e2.getSQLState()+", ex.toString='"+e2.toString()+"', SQL: "+sb.toString(), e2);
				// throws Exception if it's a severe problem
				isSevereProblem(conn, e2);
				return 0;
			}
		}
	}

//	private void saveGraphProperties(DbxConnection conn, String schemaName, Timestamp sessionStartTime, CmEntry cme, GraphEntry ge)
//	{
//		// key = schemaName|############|cmName_graphName
//		String key = new StringBuffer().append(schemaName).append("|").append(sessionStartTime == null ? 0 : sessionStartTime.getTime()).append("|").append(cme.getName()).append("_").append(ge.getName()).toString();
//		if (_graphPropertiesSet.contains(key))
//			return;
//		
//		// Then add some "properties" like Graph Label etc...
//		String sql = "";
//		try
//		{
//			StringBuilder sb = new StringBuilder();
//
//			// Check if we need to do insert
//			String q = getQuotedIdentifierChar();
//
//			String tabName = getTableName(schemaName, Table.GRAPH_PROPERTIES, null, true);
//			sb.append("select count(*)");
//			sb.append(" from ").append(tabName);
//			sb.append(" where ").append(q).append("SessionStartTime").append(q).append(" = '").append(sessionStartTime).append("' ");
//			sb.append("   and ").append(q).append("CmName")          .append(q).append(" = '").append(cme.getName()   ).append("' ");
//			sb.append("   and ").append(q).append("GraphName")       .append(q).append(" = '").append(ge.getName()    ).append("' ");
//			sql = sb.toString();
//
//			Statement stmnt = conn.createStatement();
//			ResultSet rs = stmnt.executeQuery(sql);
//			int exists = -1;
//			while (rs.next())
//				exists = rs.getInt(1);
//			rs.close();
//			stmnt.close();
//
//			// If the record do not exists, add it (yes there is a "race condition", but I dont care about that)
//			if ( exists <= 0 )
//			{
//    			//--------------------------
//    			// first get current count of graphs in session...
//				// This to determen 'initialOrder' value
//    			//--------------------------
//    			sb = new StringBuilder();
//    			tabName = getTableName(schemaName, Table.GRAPH_PROPERTIES, null, true);
//    			sb.append("select count(*)");
//    			sb.append(" from ").append(tabName);
//    			sb.append(" where ").append(q).append("SessionStartTime").append(q).append(" = '").append(sessionStartTime).append("' ");
//    			sql = sb.toString();
//
//    			stmnt = conn.createStatement();
//    			rs = stmnt.executeQuery(sql);
//    			int initialOrder = 0;
//    			while (rs.next())
//    				initialOrder = rs.getInt(1);
//    			rs.close();
//    			stmnt.close();
//
//    			//--------------------------
//    			// Insert
//    			//--------------------------
//    			sb = new StringBuilder();
//    			sb.append(getTableInsertStr(schemaName, Table.GRAPH_PROPERTIES, null, false));
//    			sb.append(" values('").append(sessionStartTime).append("'");
//    			sb.append(", '").append(cme.getName()).append("'");
//    			sb.append(", '").append(ge.getName()).append("'");
//    			sb.append(", '").append(cme.getName()).append("_").append(ge.getName()).append("'"); // cmName_graphName
//    			sb.append(", ") .append(safeStr(ge.getGraphLabel()));
//    			sb.append(", ") .append(safeStr(ge.getGraphCategory()));
//    			sb.append(", ") .append(ge.isPercentGraph());
//    			sb.append(", ") .append(ge.isVisibleAtStart());
//    			sb.append(", ") .append(initialOrder);
//    			sb.append(")");
//    
//    			sql = sb.toString();
//    			
//    			conn.dbExec(sql, false);
//    			getStatistics().incInserts();
//			}
//			
//			_graphPropertiesSet.add(key);
//		}
//		catch(SQLException ex)
//		{
//			_logger.warn("Problems saving Graph Properties for cm='"+cme.getName()+"', graphName='"+ge.getName()+"', sql='"+sql+"', Caught: "+ex);
//		}
//	}
	
	private void checkSaveGraphProperties(DbxConnection conn, DbxTuneSample cont, String schemaName, Timestamp sessionStartTime, Timestamp sessionSampleTime)
	throws SQLException
	{
		if (_shutdownWithNoWait)
		{
			_logger.info("checkSaveGraphProperties: Discard entry due to 'ShutdownWithNoWait'.");
			return;
		}

		StringBuilder sb = new StringBuilder();
		String q = getQuotedIdentifierChar();

		// Get number of graphs in the container
		int receivedGraphCount = 0;
		for (CmEntry cme : cont._collectors)
		{
			if ( ! cme.hasValidSampleData() )
				continue;

			Map<String, GraphEntry> tgdMap = cme._graphMap;
			if (tgdMap != null)
				receivedGraphCount += tgdMap.size();
		}

		// Get SAVED number of graphs in PCS (from cachedMap or the PCS DB)
		int savedGraphPropsCount = 0;
		
		String key = new StringBuffer().append(schemaName).append("|").append(sessionStartTime == null ? 0 : sessionStartTime.getTime()).append("|").toString();
		if (_graphPropertiesCountMap.get(key) == null)
		{
			sb = new StringBuilder();
			String tabName = getTableName(schemaName, Table.GRAPH_PROPERTIES, null, true);
			sb.append("select count(*)");
			sb.append(" from ").append(tabName);
			sb.append(" where ").append(q).append("SessionStartTime").append(q).append(" = '").append(sessionStartTime).append("' ");
			String sql = sb.toString();

			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql);)
			{
				while (rs.next())
					savedGraphPropsCount = rs.getInt(1);
			}
			if (_logger.isDebugEnabled())
				_logger.debug("checkSaveGraphProperties(): GET rowcount from PCS: savedGraphPropsCount="+savedGraphPropsCount+", sql="+sql);
		}
		else
		{
			savedGraphPropsCount = _graphPropertiesCountMap.get(key);
			if (_logger.isDebugEnabled())
				_logger.debug("checkSaveGraphProperties(): GET MAP: key='"+key+"', value(savedGraphPropsCount)="+savedGraphPropsCount);
		}

		if (_logger.isDebugEnabled())
			_logger.debug("checkSaveGraphProperties(): receivedGraphCount="+receivedGraphCount+", savedGraphPropsCount="+savedGraphPropsCount);

		// DELETE old records and INSERT new records
		if (receivedGraphCount > savedGraphPropsCount)
		{
			// DELETE old records
			sb = new StringBuilder();
			String tabName = getTableName(schemaName, Table.GRAPH_PROPERTIES, null, true);
			sb.append("delete from ").append(tabName);
			sb.append(" where ").append(q).append("SessionStartTime").append(q).append(" = '").append(sessionStartTime).append("' ");
			String sql = sb.toString();

			if (_logger.isDebugEnabled())
				_logger.debug("checkSaveGraphProperties(): DELETE PCS RECORD: sql="+sql);

			int delCount = conn.dbExec(sql, false);
			getStatistics().incDeletes(delCount);

			// INSERT new records
			int initialOrder = 0;
			for (CmEntry cme : cont._collectors)
			{
				if ( ! cme.hasValidSampleData() )
					continue;

				Map<String, GraphEntry> tgdMap = cme._graphMap;
				if (tgdMap != null)
				{
					for (GraphEntry ge : tgdMap.values()) 
					{
						sb = new StringBuilder();
						sb.append(getTableInsertStr(schemaName, Table.GRAPH_PROPERTIES, null, false));
						sb.append(" values('").append(sessionStartTime).append("'");
						sb.append(", '").append(cme.getName()).append("'");
						sb.append(", '").append(ge.getName()).append("'");
						sb.append(", '").append(cme.getName()).append("_").append(ge.getName()).append("'"); // cmName_graphName
						sb.append(", ") .append(safeStr(ge.getGraphLabel()));
						sb.append(", ") .append(safeStr(ge.getGraphCategory()));
						sb.append(", ") .append(ge.isPercentGraph());
						sb.append(", ") .append(ge.isVisibleAtStart());
						sb.append(", ") .append(initialOrder);
						sb.append(")");

						sql = sb.toString();
						
						if (_logger.isTraceEnabled())
							_logger.trace("checkSaveGraphProperties(): INSERT PCS RECORD: sql="+sql);
						
						conn.dbExec(sql, false);
						getStatistics().incInserts();

						initialOrder++;
					} // end: graphEntry
				} // end: has tgdMap
			} // end: loop Collectors

			if (_logger.isDebugEnabled())
				_logger.debug("checkSaveGraphProperties(): INSERTED PCS RECORD: count="+initialOrder);
			
			// save the receivedGraphCount in the cached Map
			_graphPropertiesCountMap.put(key, receivedGraphCount);

			if (_logger.isDebugEnabled())
				_logger.debug("checkSaveGraphProperties(): PUT MAP: key='"+key+"', value(receivedGraphCount)="+receivedGraphCount);
			
		} // end: DELETE old records and INSERT new records
	} // end: method

	private void saveGraphDataDdl(DbxConnection conn, String schemaName, String tabName, GraphEntry ge)
	throws SQLException
	{
//System.out.println("########################################## saveGraphDataDdl(): schemaName='"+schemaName+"', tabName='"+tabName+"', GraphEntry='"+ge+"'.");
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

			boolean tabExists = false;
			rs = dbmd.getColumns(null, schemaName, tabName, "%");
			while(rs.next())
			{
				tabExists = true;
//				System.out.println("getColumns(): TABLE_SCHEM=|"+rs.getString("TABLE_SCHEM")+"|, TABLE_NAME=|"+rs.getString("TABLE_NAME")+"|, COLUMN_NAME=|"+rs.getString("COLUMN_NAME")+"|.");
			}
			rs.close();

			if( ! tabExists )
			{
//System.out.println("########################################## saveGraphDataDdl(): ---NOT-EXISTS--- schemaName='"+schemaName+"', tabName='"+tabName+"', GraphEntry='"+ge+"'.");
				_logger.info("Persistent Counter DB: Creating table "+StringUtil.left("'"+schemaName+"."+tabName+"'", schemaName.length()+50, true)+" for CounterModel graph '" + ge.getName() + "'.");

				String sqlTable = getGraphTableDdlString(schemaName, tabName, ge);
				String sqlIndex = getGraphIndexDdlString(schemaName, tabName, ge);

				dbDdlExec(conn, sqlTable);
				dbDdlExec(conn, sqlIndex);
				
				getStatistics().incCreateTables();
			}
			else // Check if we need to add any new columns
			{
//System.out.println("########################################## saveGraphDataDdl(): ---EXISTS--- schemaName='"+schemaName+"', tabName='"+tabName+"', GraphEntry='"+ge+"'.");
//				String sqlAlterTable = getGraphAlterTableDdlString(conn, tabName, tgdp);
//				if ( ! sqlAlterTable.trim().equals("") )
//				{
//					_logger.info("Persistent Counter DB: Altering table '"+tabName+"' for CounterModel graph '" + tgdp.getName() + "'.");
//
//					dbDdlExec(sqlAlterTable);
//					incAlterTables();
//				}
				List<String> sqlAlterList = getGraphAlterTableDdlString(conn, schemaName, tabName, ge);
//System.out.println("########################################## saveGraphDataDdl(): sqlAlterList="+sqlAlterList);
				if ( ! sqlAlterList.isEmpty() )
				{
					for (String sqlAlterTable : sqlAlterList)
					{
						_logger.info("Persistent Counter DB: Altering table '"+tabName+"' for CounterModel graph '" + ge.getName() + "'. sql="+sqlAlterTable);

						dbDdlExec(conn, sqlAlterTable);
						getStatistics().incAlterTables();
					}
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
				if (conn.isConnectionOk())
					conn.setAutoCommit(false);
			}
		}
	}

	
	
	
//	@Override
//	public boolean isDdlCreated(CmEntry cme)
//	{
//		return isDdlCreated(cme.getName());
//	}
//
//	@Override
//	public void markDdlAsCreated(CmEntry cme)
//	{
//		markDdlAsCreated(cme.getName());
//	}
//
//	@Override
//	public boolean saveDdl(CmEntry cme)
//	{
//		saveD
//		// TODO Auto-generated method stub
//		return false;
//	}

	@Override
	public void saveCounters(CmEntry cme)
	{
		// empty, all done in: saveSample()
	}

	@Override
	public boolean beginOfSample(DbxTuneSample cont)
	{
		DbxConnection conn = open(cont); 
		return conn != null;
	}

	@Override
	public void endOfSample(DbxTuneSample cont, boolean caughtErrors)
	{
		close();
	}

	@Override
	public void startServices() throws Exception
	{
		// No extra sevices is started/stopped
		_servicesStoppedByThread = null;
	}

	@Override
	public synchronized void stopServices(int maxWaitTimeInMs)
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
			// TODO: Probably better to use 'shutdownConfig' than the File 'H2_SHUTDOWN_WITH_DEFRAG_FILENAME'
			Configuration shutdownConfig = ShutdownHandler.getShutdownConfiguration();
			
//			H2ShutdownType h2ShutdownType  = H2ShutdownType.valueOf( shutdownConfig.getProperty("h2.shutdown.type", H2ShutdownType.IMMEDIATELY.toString()) );
			H2ShutdownType h2ShutdownType = H2ShutdownType.IMMEDIATELY;
			String h2ShutdownTypeStr = shutdownConfig.getProperty("h2.shutdown.type", H2ShutdownType.IMMEDIATELY.toString());
			_logger.info("Received a H2 database shutdown request with 'h2.shutdown.type' of '"+h2ShutdownTypeStr+"'.");
			try { 
				h2ShutdownType = H2ShutdownType.valueOf( h2ShutdownTypeStr ); 
			} catch(RuntimeException ex) { 
				_logger.info("Shutdown type '"+h2ShutdownTypeStr+"' is unknown value, supported values: "+StringUtil.toCommaStr(H2ShutdownType.values())+". ex="+ex);
			}
			
			File shutdownWithDefrag = new File(H2_SHUTDOWN_WITH_DEFRAG_FILENAME);
			_logger.info("Checking for override of H2 shutdown type. Checking for file '"+shutdownWithDefrag+"'. exists="+shutdownWithDefrag.exists());
			if (shutdownWithDefrag.exists())
			{
				// get the reason why we shutdown
				String shutdownReason = "";
				try { shutdownReason = FileUtils.readFileToString(shutdownWithDefrag, StandardCharsets.UTF_8).trim(); }
				catch(IOException ignore) {}

				shutdownWithDefrag.delete();
				_logger.info("Found file '"+shutdownWithDefrag+"'. So 'SHUTDOWN DEFRAG' will be used to stop/shutdown H2 database. reason='"+shutdownReason+"'.");
				
				h2ShutdownType = H2ShutdownType.DEFRAG;
			}
			
			h2Shutdown( h2ShutdownType );
		}
	}
	public static final String H2_SHUTDOWN_WITH_DEFRAG_FILENAME = System.getProperty("java.io.tmpdir", "/tmp") + File.separatorChar + "dbxcentral.shutdown.with.defrag";


	public enum H2ShutdownType
	{
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
	 */
	private void h2Shutdown(H2ShutdownType shutdownType)
	{
		if (_mainConn == null)
		{
			_logger.info("h2ShutdownCompact(): will do nothing, _mainConn was null");
			return;
		}

		// Try to get the FILES SIZE before we do 'shutdown compact'
		// Then after 'shutdown compact', we can compare the difference / db file shrinkage
		H2UrlHelper urlHelper = new H2UrlHelper(_lastUsedUrl);
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

		// Issue a dummy command to see if the connection is still alive
		try (Statement stmnt = _mainConn.createStatement();) {
			stmnt.execute("select 1111");
		} catch(SQLException ex) {
			_logger.error("Problem when executing DUMMY SQL 'select 1111' before "+shutdownCmd+". SqlException: ErrorCode="+ex.getErrorCode()+", SQLState="+ex.getSQLState()+", toString="+ex.toString());
		}

		try (Statement stmnt = _mainConn.createStatement();) 
		{
			_logger.info("Sending "+shutdownCmd+" to H2 database.");
			stmnt.execute(shutdownCmd);
			_logger.info("Shutdown H2 database using '"+shutdownCmd+"', took "+TimeUtils.msDiffNowToTimeStr("%MM:%SS.%ms", startTime)+ " (MM:SS.ms)");
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
			
			// if file size is smaller: 
			// - Save the NEW file size after SHUTDOWN has been executed
			// - This will help CentralH2Defrag (otherwise it needs one check just to determen that the file is smaller) 
			int sizeDiffMb = (int) (sizeDiff /1024.0/1024.0);
			if (sizeDiffMb < 0)
			{
				int dbFileSizeAfterMb = (int) (dbFileSizeAfter /1024.0/1024.0); 
				CentralH2Defrag.saveH2StorageInfoFile(dbFileSizeAfterMb, dbFile.toString());
			}
			
			// Check if we have a ".tempFile"
			// Then it's probably 'SHUTDOWN DEFRAG' that didn't work... (and no exception was thrown)
			File shutdownTempFile = new File(dbFile.getAbsolutePath() + ".tempFile");
			if (shutdownTempFile.exists())
			{
				_logger.warn("After Shutdown H2 database using '"+shutdownCmd+"', the file '"+shutdownTempFile+"' exists. sizeMb("+(shutdownTempFile.length()/1024/1024)+"), sizeB("+shutdownTempFile.length()+"). This is probably due to a 'incomplete' shutdown (defrag). REMOVING THIS FILE.");
				shutdownTempFile.delete();
			}
		}

		// Close the connection
		try { _mainConn.close(); } catch(Exception ignore) {}
		_mainConn = null; 
	}


	@Override
	public String getName()
	{
		return this.getClass().getSimpleName();
	}

	@Override
	public void storageQueueSizeWarning(int queueSize, int thresholdSize)
	{
		// TODO Auto-generated method stub
		
	}
}
