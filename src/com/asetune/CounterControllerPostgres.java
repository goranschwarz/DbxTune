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
package com.asetune;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.asetune.cache.DbmsObjectIdCache;
import com.asetune.cache.DbmsObjectIdCachePostgres;
import com.asetune.cache.XmlPlanCache;
import com.asetune.cache.XmlPlanCachePostgres;
import com.asetune.cm.CountersModel;
import com.asetune.cm.os.CmOsDiskSpace;
import com.asetune.cm.os.CmOsIostat;
import com.asetune.cm.os.CmOsMeminfo;
import com.asetune.cm.os.CmOsMpstat;
import com.asetune.cm.os.CmOsNwInfo;
import com.asetune.cm.os.CmOsPs;
import com.asetune.cm.os.CmOsUptime;
import com.asetune.cm.os.CmOsVmstat;
import com.asetune.cm.postgres.CmActiveStatements;
import com.asetune.cm.postgres.CmErrorLog;
import com.asetune.cm.postgres.CmPgActivity;
import com.asetune.cm.postgres.CmPgArchiver;
import com.asetune.cm.postgres.CmPgBgWriter;
import com.asetune.cm.postgres.CmPgBufferCacheACnt;
import com.asetune.cm.postgres.CmPgBufferCacheDet;
import com.asetune.cm.postgres.CmPgBufferCacheSum;
import com.asetune.cm.postgres.CmPgDatabase;
import com.asetune.cm.postgres.CmPgFunctions;
import com.asetune.cm.postgres.CmPgIndexes;
import com.asetune.cm.postgres.CmPgIndexesIo;
import com.asetune.cm.postgres.CmPgLocks;
import com.asetune.cm.postgres.CmPgPidWait;
import com.asetune.cm.postgres.CmPgProgAnalyze;
import com.asetune.cm.postgres.CmPgProgBaseBackup;
import com.asetune.cm.postgres.CmPgProgCluster;
import com.asetune.cm.postgres.CmPgProgCopy;
import com.asetune.cm.postgres.CmPgProgIndex;
import com.asetune.cm.postgres.CmPgProgVacuum;
import com.asetune.cm.postgres.CmPgRepSlots;
import com.asetune.cm.postgres.CmPgReplication;
import com.asetune.cm.postgres.CmPgSequencesIo;
import com.asetune.cm.postgres.CmPgSlru;
import com.asetune.cm.postgres.CmPgSrvWait;
import com.asetune.cm.postgres.CmPgStatements;
import com.asetune.cm.postgres.CmPgStatementsSumDb;
import com.asetune.cm.postgres.CmPgTableSize;
import com.asetune.cm.postgres.CmPgTables;
import com.asetune.cm.postgres.CmPgTablesIo;
import com.asetune.cm.postgres.CmPgWal;
import com.asetune.cm.postgres.CmPgWalReceiver;
import com.asetune.cm.postgres.CmSummary;
import com.asetune.gui.MainFrame;
import com.asetune.pcs.PersistContainer;
import com.asetune.pcs.PersistContainer.HeaderInfo;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.TimeUtils;
import com.asetune.utils.Ver;


public class CounterControllerPostgres 
extends CounterControllerAbstract
{
	private static Logger _logger = Logger.getLogger(CounterControllerPostgres.class);

	public static final int	   NUMBER_OF_PERFORMANCE_COUNTERS	= 54 + 10 + 20; 

	/**
	 * The default constructor
	 * @param hasGui should we create a GUI or NoGUI collection thread
	 */
	public CounterControllerPostgres(boolean hasGui)
	{
		super(hasGui);
	}


	/**
	 * This will be used to create all the CountersModel objects.
	 * <p>
	 * If we are in GUI mode the graphical objects would also be 
	 * created and added to the GUI system, this so they will be showned 
	 * in the GUI before we are connected to any ASE Server.
	 *  <p>
	 * initCounters() would be called on "connect" to be able to 
	 * initialize the counter object using a specific ASE release
	 * this so we can decide what monitor tables and columns we could use.
	 */
	@Override
	public void createCounters(boolean hasGui)
	{
		if (isCountersCreated())
			return;

		_logger.info("Creating ALL CM Objects.");

		ICounterController counterController = this;
		MainFrame          guiController     = hasGui ? MainFrame.getInstance() : null;

		CmSummary           .create(counterController, guiController);

		// Server
		CmPgActivity        .create(counterController, guiController);
		CmPgPidWait         .create(counterController, guiController);
		CmPgDatabase        .create(counterController, guiController);
		CmPgSrvWait         .create(counterController, guiController);
		CmPgBgWriter        .create(counterController, guiController);
		CmPgWal             .create(counterController, guiController);
		CmPgArchiver        .create(counterController, guiController);
		CmErrorLog          .create(counterController, guiController);

		// Object Access
		CmActiveStatements  .create(counterController, guiController);
//		CmPgBlockingLocks   .create(counterController, guiController); // nearly the same as CmActiveStatements -->> possibly do: sel * pg_locks instead???
		CmPgTables          .create(counterController, guiController);
		CmPgTablesIo        .create(counterController, guiController);
		CmPgIndexes         .create(counterController, guiController);
		CmPgIndexesIo       .create(counterController, guiController);
		CmPgFunctions       .create(counterController, guiController);
		CmPgSequencesIo     .create(counterController, guiController);
		CmPgStatements      .create(counterController, guiController);
		CmPgStatementsSumDb .create(counterController, guiController);
		CmPgTableSize       .create(counterController, guiController);

		// NOTE: This should be AFTER 'CmPgTables' since we depends on that to store information about Postgres 'relationid' --> 'tablename'
		CmPgLocks           .create(counterController, guiController);
		
		
		// Cache
		CmPgSlru            .create(counterController, guiController);
		CmPgBufferCacheSum  .create(counterController, guiController);
		CmPgBufferCacheACnt .create(counterController, guiController); // pg_buffercache_usage_counts() Function  -- CmPgBufferCacheAccessCount
		CmPgBufferCacheDet  .create(counterController, guiController);

		// Disk

		// Progress
		CmPgProgCluster     .create(counterController, guiController);
		CmPgProgVacuum      .create(counterController, guiController);
		CmPgProgAnalyze     .create(counterController, guiController);
		CmPgProgIndex       .create(counterController, guiController);
		CmPgProgCopy        .create(counterController, guiController);
		CmPgProgBaseBackup  .create(counterController, guiController);
		
		// Replication
		CmPgReplication     .create(counterController, guiController);
		CmPgRepSlots        .create(counterController, guiController);
		CmPgWalReceiver     .create(counterController, guiController);

		// OS HOST Monitoring
		CmOsIostat          .create(counterController, guiController);
		CmOsVmstat          .create(counterController, guiController);
		CmOsMpstat          .create(counterController, guiController);
		CmOsUptime          .create(counterController, guiController);
		CmOsMeminfo         .create(counterController, guiController);
		CmOsNwInfo          .create(counterController, guiController);
		CmOsDiskSpace       .create(counterController, guiController);
		CmOsPs              .create(counterController, guiController);

		// USER DEFINED COUNTERS
		createUserDefinedCounterModels(counterController, guiController);
		createUserDefinedCounterModelHostMonitors(counterController, guiController);

		// done
		setCountersIsCreated(true);
	}

	/**
	 * When we have a database connection, lets do some extra init this
	 * so all the CountersModel can decide what SQL to use. <br>
	 * SQL statement usually depends on what ASE Server version we monitor.
	 * 
	 * @param conn
	 * @param hasGui              is this initialized with a GUI?
	 * @param srvVersion          what is the Servers Executable version
	 * @param isClusterEnabled    is it a cluster ASE
	 * @param monTablesVersion    what version of the MDA tables should we use
	 */
	@Override
//	public void initCounters(Connection conn, boolean hasGui, long srvVersion, boolean isClusterEnabled, long monTablesVersion)
//	throws Exception
	public void initCounters(DbxConnection conn, boolean hasGui, long srvVersion, boolean isClusterEnabled, long monTablesVersion)
	throws Exception
	{
		if (isInitialized())
		return;

		if ( ! AseConnectionUtils.isConnectionOk(conn, hasGui, null))
			throw new Exception("Trying to initialize the counters with a connection this seems to be broken.");

			
		if (! isCountersCreated())
			createCounters(hasGui);
		
		_logger.info("Initializing all CM objects, using Postgres version number "+srvVersion+". ("+Ver.versionNumToStr(srvVersion)+")");

		// initialize all the CM's
		for (CountersModel cm : getCmList())
		{
			_logger.debug("Initializing CM named '"+cm.getName()+"', display name '"+cm.getDisplayName()+"', using Postgres version number "+srvVersion+".");

			// set the version
			cm.setServerVersion(monTablesVersion);
			cm.setClusterEnabled(isClusterEnabled);

			// set the active roles, so it can be used in initSql()
//			cm.setActiveRoles(_activeRoleList);

			// set the ASE Monitor Configuration, so it can be used in initSql() and elsewhere
//			cm.setMonitorConfigs(monitorConfigMap);

			// Now when we are connected to a server, and properties are set in the CM, 
			// mark it as runtime initialized (or late initialization)
			// do NOT do this at the end of this method, because some of the above stuff
			// will be used by the below method calls.
			cm.setRuntimeInitialized(true);

			// Initializes SQL, use getServerVersion to check what we are connected to.
			cm.initSql(conn);

			// Use this method if we need to check anything in the database.
			// for example "deadlock pipe" may not be active...
			// If server version is below 15.0.2 statement cache info should not be VISABLE
			cm.init(conn);
			
			// Initialize graphs for the version you just connected to
			// This will simply enable/disable graphs that should not be visible for the ASE version we just connected to
			cm.initTrendGraphForVersion(monTablesVersion);
		}

		setInitialized(true);
	}

	@Override
	public void checkServerSpecifics()
	{
	}

//	/** If DNS Lookups takes to long, cache the host names in here */
	private boolean                 _ipToHostCache_isEnabled = true; // enables/disables if lookup takes to long.
	private HashMap<String, String> _ipToHostCache = new HashMap<>();  // key=IP, Value=HostName
	private long                    _ipToHostLastLookup    = 0;        // Time stamp when we did last getHostName()
	private long                    _ipToHostLastLookupTtl = Configuration.getCombinedConfiguration().getLongProperty(PROPKEY_ipToHostCache_ttl, DEFAULT_ipToHostCache_ttl);

	public static final String PROPKEY_ipToHostCache_ttl = "CounterControllerPostgres.ipToHostCache.ttl.ms";
	public static final long   DEFAULT_ipToHostCache_ttl = 600 * 1000; // 10 minutes
	
	@Override
	public PersistContainer.HeaderInfo createPcsHeaderInfo()
	{
		// Get session/head info
		String    dbmsServerName   = null;
		String    dbmsHostname     = null;
		Timestamp mainSampleTime   = null;
		Timestamp counterClearTime = new Timestamp(0);

//		String sql = "select current_timestamp, sys_context('USERENV','INSTANCE_NAME') as Instance, sys_context('USERENV','SERVER_HOST') as onHost from dual";
//		String sql = "select current_timestamp, 'DUMMY_INSTANCE' as Instance, 'DUMMY_HOSTNAME' as onHost";
		String sql = "select current_timestamp, inet_server_addr() as onIp, inet_server_port() as portNum";

		try
		{
			if ( ! isMonConnected(true, true) ) // forceConnectionCheck=true, closeConnOnFailure=true
				return null;
				
			Statement stmt = getMonConnection().createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				Timestamp ts   = rs.getTimestamp(1);
				String    ip   = rs.getString(2).trim();
				String    port = rs.getString(3).trim();
//				counterClearTime = rs.getTimestamp(4);

				String hostname = ip;
				
				if (_ipToHostCache_isEnabled && _ipToHostCache.containsKey(ip) && TimeUtils.msDiffNow(_ipToHostLastLookup) < _ipToHostLastLookupTtl)
				{
					hostname = _ipToHostCache.get(ip);
//System.out.println("IP to HOSTNAME lookup. Got hostname '" + hostname + "' for ip '" + ip + "' from cache. CacheAge=" + TimeUtils.msDiffNow(_ipToHostLastLookup) + ", TTL can be changed via Property '" + PROPKEY_ipToHostCache_ttl + "', defaultTtl=" + DEFAULT_ipToHostCache_ttl + ".");
					if (_logger.isDebugEnabled())
						_logger.debug("IP to HOSTNAME lookup. Got hostname '" + hostname + "' for ip '" + ip + "' from cache. CacheAge=" + TimeUtils.msDiffNow(_ipToHostLastLookup) + ", TTL can be changed via Property '" + PROPKEY_ipToHostCache_ttl + "', defaultTtl=" + DEFAULT_ipToHostCache_ttl + ".");
				}
				else
				{
					try
					{
						long startTime = System.currentTimeMillis();

						// Do the lookup
						InetAddress addr = InetAddress.getByName(ip);
						hostname = addr.getHostName();

						// Cache: IP -> Hostname
						_ipToHostCache.put(ip, hostname);
						_ipToHostLastLookup = System.currentTimeMillis();
						_ipToHostCache_isEnabled = false; // turn OFF caching... if we had a long lookup time, the cache will be ENABLED

						// If this takes to long...
						long execTimeMs = TimeUtils.msDiffNow(startTime);
						if (execTimeMs > 2_000)
						{
							_ipToHostCache_isEnabled = true; // ENABLE cache
							_logger.warn("createPcsHeaderInfo(): This takes to long time, ENABLING 'ip-to-hostname' local cache. java.net.InetAddress.getHostName() took " + execTimeMs + " ms to compleate. Check your DNS Reverse Lookup settings.");

							// Possible fix: CACHE the DNS lookup in a HashMap<IP, hostname>, also set a TTL for 1 hour
						}
					}
					catch(UnknownHostException ex)
					{
						_logger.info("Problems looking up the IP '" + ip + "' into a real name, using InetAddress.getByName(ip). So lets use the IP adress '" + hostname + "' as the name. Caught: " + ex);
					}
				}

				mainSampleTime   = ts;
				dbmsServerName   = DbxTune.stripSrvName(hostname + ":" + port);
				dbmsHostname     = hostname;
				
				_logger.debug("createPcsHeaderInfo(): dbmsServerName='"+dbmsServerName+"', dbmsHostname='"+dbmsHostname+"', mainSampleTime='"+mainSampleTime+"'.");
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException sqlex)
		{
			// Connection is already closed.
			if ( "JZ0C0".equals(sqlex.getSQLState()) )
			{
				boolean forceConnectionCheck = true;
				boolean closeConnOnFailure   = true;
				if ( ! isMonConnected(forceConnectionCheck, closeConnOnFailure) )
				{
					_logger.info("Problems getting basic status info in 'Counter get loop'. SQL State 'JZ0C0', which means 'Connection is already closed'. So lets start from the top." );
					return null;
				}
			}
			
			_logger.warn("Problems getting basic status info in 'Counter get loop', reverting back to 'static values'. SQL '"+sql+"', Caught: " + sqlex.toString() );
			mainSampleTime   = new Timestamp(System.currentTimeMillis());
			dbmsServerName   = "unknown";
			dbmsHostname     = "unknown";
			counterClearTime = new Timestamp(0);
		}
		
		return new HeaderInfo(mainSampleTime, dbmsServerName, dbmsHostname, counterClearTime);
	}

	
	@Override
	public String getServerTimeCmd()
	{
//		return "SELECT CURRENT_TIMESTAMP; \n";
		return "select CLOCK_TIMESTAMP(); \n";
	}

	@Override
	protected String getIsClosedSql()
	{
		return "SELECT 'PostgresTune-check:isClosed(conn)'";
	}

//	@Override
//	public boolean isSqlBatchingSupported()
//	{
//		return false;
//	}

	public static final String  PROPKEY_postgres_setDbmsOption_prefix   = Version.getAppName() + ".onMonConnect.set.dbms.option."; // DEFAULT -> DEFAULT; val -> 'val'; noop == DO-NOT-SET

	@Override
	public void onMonConnect(DbxConnection conn)
	{
		String dbname = "-unknown-";
		try { dbname = conn.getCatalog(); } catch (SQLException ignore) {}
		
		//------------------------------------------------
		// Set Some Options
		Configuration tmpConf = new Configuration();
		tmpConf.setProperty(PROPKEY_postgres_setDbmsOption_prefix + "work_mem",     "128MB");
		tmpConf.setProperty(PROPKEY_postgres_setDbmsOption_prefix + "temp_buffers", "128MB");
		// set
		setPostgresOptions(conn, dbname, tmpConf);

		//------------------------------------------------
		// User Provided Options (from any of the configuration files)
		setPostgresOptions(conn, dbname, Configuration.getCombinedConfiguration());

		//------------------------------------------------
		// Print Options...
		printPostgresOptions(conn, dbname, tmpConf);
		printPostgresOptions(conn, dbname, Configuration.getCombinedConfiguration());
	}

	/** Set options */
	private void setPostgresOptions(DbxConnection conn, String dbname, Configuration conf)
	{
		for (String key : conf.getKeys(PROPKEY_postgres_setDbmsOption_prefix))
		{
			String val = conf.getProperty(key);

			// Take away the prefix
			String optName = key.replaceFirst(PROPKEY_postgres_setDbmsOption_prefix, "");

			// Set
			setPostgresOption(conn, dbname, optName, val);
		}
	}

	/** Set option */
	private String setPostgresOption(DbxConnection conn, String dbname, String optName, String confVal)
	{
		if ( "noop".equalsIgnoreCase(confVal) )
			return null;

		if ( ! "DEFAULT".equalsIgnoreCase(confVal) )
			confVal = "'" + confVal + "'"; // quote the string if content is NOT "DEFAULT"/"default"

		String sql = "SET SESSION " + optName + " = " + confVal;
		try (Statement stmnt = conn.createStatement() )
		{
			_logger.info("onMonConnect(): pid="+conn.getDbmsSessionId()+", dbname='" + dbname + "'. Setting Postgres session option '" + optName + "' using sql: " + sql);
			stmnt.executeUpdate(sql);

			return confVal;
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems in onMonConnect(): pid=" + conn.getDbmsSessionId() + ", dbname='" + dbname + "'. When Initializing DBMS SET Property '" + optName + "', using sql='" + sql + "'. Continuing... Caught: MsgNum=" + ex.getErrorCode() + ": " + ex);
			return null;
		}
	}

	/** Print option */
	private void printPostgresOptions(DbxConnection conn, String dbname, Configuration conf)
	{
		for (String key : conf.getKeys(PROPKEY_postgres_setDbmsOption_prefix))
		{
			// Take away the prefix
			String optName = key.replaceFirst(PROPKEY_postgres_setDbmsOption_prefix, "");

			// Show
			showPostgresOption(conn, dbname, optName, true);
		}
	}

	/** Show/Get/print option */
	private String showPostgresOption(DbxConnection conn, String dbname, String optName, boolean print)
	{
		String ret = "";

		String sql = "SHOW " + optName;
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql) )
		{
			while(rs.next())
			{
				ret = rs.getString(1);
				if (print)
					_logger.info("onMonConnect(): pid="+conn.getDbmsSessionId()+", dbname='" + dbname + "'. current setting for '" + optName + "' is '" + ret + "'.");
			}
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems in onMonConnect(): pid="+conn.getDbmsSessionId()+", dbname='" + dbname + "'. When GET property '" + optName + "', using sql='" + sql + "'. Continuing... Caught: MsgNum=" + ex.getErrorCode() + ": " + ex);
		}
		return ret;
	}
	
	@Override
	public void setInRefresh(boolean enterRefreshMode)
	{
		try
		{
			DbxConnection dbxConn = getMonConnection();
			if (dbxConn != null)
			{
				if (enterRefreshMode)
				{
					// Lets use ONE transaction for every refresh
					if (dbxConn.getAutoCommit() == true)
						dbxConn.setAutoCommit(false);
				}
				else
				{
					// When leaving refresh mode... set AutoCommit to true -- so we don't end up 'idle in transaction' while sleeping
					if (dbxConn.getAutoCommit() == false)
						dbxConn.setAutoCommit(true);
				}
			}
		}
		catch(SQLException e)
		{
			_logger.info("Problem when changing the Postgres Connection autocommit mode.");
		}
		finally 
		{
			super.setInRefresh(enterRefreshMode);
		}
	}

	//==================================================================
	// BEGIN: NO-GUI methods
	//==================================================================
	@Override
	public DbxConnection noGuiConnect(String dbmsUsername, String dbmsPassword, String dbmsServer, String dbmsHostPortStr, String jdbcUrlOptions) throws SQLException, Exception
	{
		DbxConnection conn = super.noGuiConnect(dbmsUsername, dbmsPassword, dbmsServer, dbmsHostPortStr, jdbcUrlOptions);
		
		ConnectionProvider connectionProvider = new ConnectionProvider()
		{
			@Override
			public DbxConnection getNewConnection(String appname)
			{
				try 
				{
					return DbxConnection.connect(null, appname);
				} 
				catch(Exception e) 
				{
					_logger.error("Problems getting a new connection. Caught: "+e, e);
					return null;
				}
			}
			
			@Override
			public DbxConnection getConnection()
			{
//				return getCounterController().getMonConnection();
				return getMonConnection();
			}
		};
				
		// DBMS ObjectID --> ObjectName Cache... maybe it's not the perfect place to initialize this...
		DbmsObjectIdCache.setInstance( new DbmsObjectIdCachePostgres(connectionProvider) );

		// Populate Object ID Cache
		if (DbmsObjectIdCache.hasInstance() && DbmsObjectIdCache.getInstance().isBulkLoadOnStartEnabled())
			DbmsObjectIdCache.getInstance().getBulk(null); // null == ALL Databases
		else
			_logger.info("Skipping BULK load of ObjectId's at noGuiConnect(), isBulkLoadOnStartEnabled() was NOT enabled. Property '" + DbmsObjectIdCachePostgres.PROPKEY_BulkLoadOnStart + "=true|false'.");

		// "XML" Plan Cache... maybe it's not the perfect place to initialize this...
		XmlPlanCache.setInstance( new XmlPlanCachePostgres(connectionProvider) );
		
		// Return the connection
		return conn;
	}

	@Override
	public void noGuiConnectErrorHandler(SQLException ex, String dbmsUsername, String dbmsPassword, String dbmsServer, String dbmsHostPortStr, String jdbcUrlOptions) 
	throws Exception
	{
		// Error checking for "invalid password" or other "unrecoverable errors"
		if (ex.getMessage().contains("password authentication failed for user"))
		{
			throw new Exception("The error message suggest that the wrong USER '" + dbmsUsername + "' or PASSWORD '" + dbmsPassword + "' to DBMS server '" + dbmsServer + "' was entered. This is a non-recovarable error. DBMS Error Message='" + ex.getMessage() + "'.", ex);
		}
	}
}
