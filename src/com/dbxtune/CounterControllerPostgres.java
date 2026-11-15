/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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
package com.dbxtune;

import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cache.DbmsObjectIdCache;
import com.dbxtune.cache.DbmsObjectIdCachePostgres;
import com.dbxtune.cache.XmlPlanCache;
import com.dbxtune.cache.XmlPlanCachePostgres;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.os.CmOsDiskSpace;
import com.dbxtune.cm.os.CmOsIostat;
import com.dbxtune.cm.os.CmOsMeminfo;
import com.dbxtune.cm.os.CmOsMpstat;
import com.dbxtune.cm.os.CmOsNwInfo;
import com.dbxtune.cm.os.CmOsPs;
import com.dbxtune.cm.os.CmOsUptime;
import com.dbxtune.cm.os.CmOsVmstat;
import com.dbxtune.cm.postgres.CmActiveStatements;
import com.dbxtune.cm.postgres.CmErrorLog;
import com.dbxtune.cm.postgres.CmPgActivity;
import com.dbxtune.cm.postgres.CmPgArchiver;
import com.dbxtune.cm.postgres.CmPgBgWriter;
import com.dbxtune.cm.postgres.CmPgBufferCacheACnt;
import com.dbxtune.cm.postgres.CmPgBufferCacheDet;
import com.dbxtune.cm.postgres.CmPgBufferCacheSum;
import com.dbxtune.cm.postgres.CmPgDatabase;
import com.dbxtune.cm.postgres.CmPgFunctions;
import com.dbxtune.cm.postgres.CmPgIndexes;
import com.dbxtune.cm.postgres.CmPgIndexesIo;
import com.dbxtune.cm.postgres.CmPgIo;
import com.dbxtune.cm.postgres.CmPgLocks;
import com.dbxtune.cm.postgres.CmPgPidWait;
import com.dbxtune.cm.postgres.CmPgProgAnalyze;
import com.dbxtune.cm.postgres.CmPgProgBaseBackup;
import com.dbxtune.cm.postgres.CmPgProgCluster;
import com.dbxtune.cm.postgres.CmPgProgCopy;
import com.dbxtune.cm.postgres.CmPgProgIndex;
import com.dbxtune.cm.postgres.CmPgProgVacuum;
import com.dbxtune.cm.postgres.CmPgRepSlots;
import com.dbxtune.cm.postgres.CmPgSubscription;
import com.dbxtune.cm.postgres.CmPgRepSlotStats;
import com.dbxtune.cm.postgres.CmPgAutovacuumScores;
import com.dbxtune.cm.postgres.CmPgLockStats;
import com.dbxtune.cm.postgres.CmPgReplication;
import com.dbxtune.cm.postgres.CmPgSequencesIo;
import com.dbxtune.cm.postgres.CmPgSlru;
import com.dbxtune.cm.postgres.CmPgSrvWait;
import com.dbxtune.cm.postgres.CmPgStatements;
import com.dbxtune.cm.postgres.CmPgStatementsSumDb;
import com.dbxtune.cm.postgres.CmPgTableBloat;
import com.dbxtune.cm.postgres.CmPgTableSize;
import com.dbxtune.cm.postgres.CmPgTables;
import com.dbxtune.cm.postgres.CmPgTablesIo;
import com.dbxtune.cm.postgres.CmPgWal;
import com.dbxtune.cm.postgres.CmPgWalReceiver;
import com.dbxtune.cm.postgres.CmSummary;
import com.dbxtune.cm.postgres.PostgresCmHelper;
import com.dbxtune.cm.postgres.PostgresCmUtils;
import com.dbxtune.config.dict.PostgresWaitTypeDictionary;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.pcs.PersistContainer;
import com.dbxtune.pcs.PersistContainer.HeaderInfo;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfoPostgres;
import com.dbxtune.utils.AseConnectionUtils;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.ConnectionProvider;
import com.dbxtune.utils.TimeUtils;
import com.dbxtune.utils.Ver;


public class CounterControllerPostgres 
extends CounterControllerAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

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
		CmPgLockStats       .create(counterController, guiController); // 19 and above
		CmPgBgWriter        .create(counterController, guiController);
		CmPgIo              .create(counterController, guiController);
		CmPgWal             .create(counterController, guiController);
		CmPgArchiver        .create(counterController, guiController);
		CmErrorLog          .create(counterController, guiController);

		// Object Access
		CmActiveStatements  .create(counterController, guiController);
//		CmPgBlockingLocks   .create(counterController, guiController); // nearly the same as CmActiveStatements -->> possibly do: sel * pg_locks instead???
		CmPgTables          .create(counterController, guiController);
		CmPgTablesIo        .create(counterController, guiController);
		CmPgTableBloat      .create(counterController, guiController);
		CmPgAutovacuumScores.create(counterController, guiController); // 19 and above
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
		CmPgRepSlotStats    .create(counterController, guiController); // 14 and above
		CmPgWalReceiver     .create(counterController, guiController);
		CmPgSubscription    .create(counterController, guiController); // 15 and above

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

		// What kind of server is this, and what can this login do? The CM's only need 'pg_monitor' (see DbmsSetup/postgres.txt)
		// The roles are read recursively (so roles granted through other roles are counted), the server files are tested by doing it.
		DbmsVersionInfoPostgres versionInfo = (conn.getDbmsVersionInfo() instanceof DbmsVersionInfoPostgres) ? (DbmsVersionInfoPostgres) conn.getDbmsVersionInfo() : new DbmsVersionInfoPostgres(srvVersion);

		boolean hasMonitor     = versionInfo.hasMonitorRole();
		boolean hasWalDir      = srvVersion < Ver.ver(10) ? versionInfo.isSuperuser() : PostgresCmUtils.canExecute(conn, "select 1 from pg_ls_waldir() limit 1");
		boolean hasServerFiles = srvVersion < Ver.ver(10) ? versionInfo.isSuperuser() : PostgresCmUtils.canExecute(conn, "select pg_read_file('PG_VERSION', 0, 1), pg_current_logfile()");

		String has = "superuser=" + versionInfo.isSuperuser() + ", pg_monitor (or pg_read_all_stats + pg_read_all_settings)=" + hasMonitor + ", pg_ls_waldir()=" + hasWalDir
				+ ", read the error log (pg_read_server_files)=" + hasServerFiles + ". Roles: " + versionInfo.getRoles();

		if (srvVersion < Ver.ver(10) && ! versionInfo.isSuperuser())
			_logger.warn("The Postgres login is not a superuser, and Postgres " + Ver.versionNumToStr(srvVersion) + " has no 'pg_monitor' role: other sessions' SQL text and some views are not visible, CM's that can't read their views will be DISABLED. The login has: " + has + " Fix: use a superuser login (or upgrade to Postgres 10 or above).");
		else if (hasMonitor && hasWalDir)
			_logger.info("The Postgres login has all it needs: " + has);
		else
			_logger.warn("The Postgres login is missing privileges: other sessions' SQL text, wait events and some views are not visible, CM's that can't read their views will be DISABLED. The login has: " + has
					+ " Fix: GRANT pg_monitor TO <user>; See DbmsSetup/postgres.txt");

		if ( ! hasServerFiles )
			_logger.info("The Postgres login can't read the server's error log, so the Counter Model 'CmErrorLog' (if enabled) is DISABLED. Optional fix: GRANT pg_read_server_files TO <user>; (and log_destination 'csvlog' or 'jsonlog')");

		if (versionInfo.isCloudManagedDbms())
			_logger.info("This is " + versionInfo.getFlavor() + ". A managed service may not allow some functions (pg_ls_waldir(), reading the error log, CREATE EXTENSION...): the Counter Models that can't read their data are DISABLED with the reason, the rest works as usual.");
		else if (versionInfo.isYugabyte())
			_logger.warn("This is YugabyteDB. PostgresTune is made for PostgreSQL: YugabyteDB has its own storage engine, so many pg_stat_* views are empty or missing. Counter Models that can't read their views are DISABLED, and some may show less data.");

		if (versionInfo.isInRecovery())
			_logger.info("This server is a standby (in recovery). Replication is shown from the standby's side (CmPgWalReceiver), and nothing is written to it.");

		// NOTE: The "track_*" settings that are OFF by default (track_io_timing, track_wal_io_timing, track_functions...) are reported as DBMS Configuration Issues (PostgresConfig.checkConfig)

		// Postgres 17 and above: the wait event descriptions from the server itself (pg_wait_events), instead of the built-in list
		PostgresWaitTypeDictionary.loadFromServer(conn, srvVersion);

		// pg_wait_sampling: the query id makes the profile bigger (and every read costs server CPU per row). The CM's do not use it.
		if (versionInfo.hasExtension("pg_wait_sampling"))
		{
			String profileQueries = PostgresCmUtils.getFirstString(conn, "select setting from pg_settings where name = 'pg_wait_sampling.profile_queries'");
			if (profileQueries != null && ! "none".equalsIgnoreCase(profileQueries) && ! "off".equalsIgnoreCase(profileQueries))
				_logger.info("pg_wait_sampling.profile_queries='" + profileQueries + "': the profile has a row per query id too, which PostgresTune does not use (more rows = more server CPU at every read). "
						+ "Optional fix: ALTER SYSTEM SET pg_wait_sampling.profile_queries = 'none'; SELECT pg_reload_conf();");
		}

		// PostgresTune may write to the monitored server: tell what, and how to turn it off.
		_logger.info("PostgresTune may CREATE some EXTENSIONs in the monitored server if they are missing: pg_stat_statements (turn off with '" + PostgresCmHelper.PROPKEY_pgStatements_createExtension + "=false'), "
				+ "pg_wait_sampling (turn off with '" + PostgresCmHelper.PROPKEY_pgWaitSampling_createExtension + "=false'), pg_buffercache (only when a CmPgBufferCache* Counter Model is enabled), "
				+ "file_fdw with the foreign table 'pg_srv_errorlog' (only when CmErrorLog is enabled and the log is CSV). "
				+ "On a primary with standbys, CmPgReplication creates and updates the table 'postgrestune_ha_dummy_update', so the standby's replay lag can be measured (turn off with '" + CmPgReplication.PROPKEY_update_primary + "=false').");

		_logger.info("Initializing all CM objects, using Postgres version number " + srvVersion + ". (" + Ver.versionNumToStr(srvVersion) + ")");

		// initialize all the CM's
		for (CountersModel cm : getCmList())
		{
			_logger.debug("Initializing CM named '" + cm.getName() + "', display name '" + cm.getDisplayName() + "', using Postgres version number " + srvVersion + ".");

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
				
				_logger.debug("createPcsHeaderInfo(): dbmsServerName='" + dbmsServerName + "', dbmsHostname='" + dbmsHostname + "', mainSampleTime='" + mainSampleTime + "'.");
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
			
			_logger.warn("Problems getting basic status info in 'Counter get loop', reverting back to 'static values'. SQL '" + sql + "', Caught: " + sqlex.toString() );
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
			_logger.info("onMonConnect(): pid=" + conn.getDbmsSessionId() + ", dbname='" + dbname + "'. Setting Postgres session option '" + optName + "' using sql: " + sql);
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
					_logger.info("onMonConnect(): pid=" + conn.getDbmsSessionId() + ", dbname='" + dbname + "'. current setting for '" + optName + "' is '" + ret + "'.");
			}
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems in onMonConnect(): pid=" + conn.getDbmsSessionId() + ", dbname='" + dbname + "'. When GET property '" + optName + "', using sql='" + sql + "'. Continuing... Caught: MsgNum=" + ex.getErrorCode() + ": " + ex);
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
					_logger.error("Problems getting a new connection. Caught: " + e, e);
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
		// SqlState 28P01 = invalid_password, 28000 = invalid_authorization_specification (for example: no pg_hba.conf entry)
		// NOTE: Never put the password in the message (it ends up in the log)
		if ("28P01".equals(ex.getSQLState()) || ex.getMessage().contains("password authentication failed for user"))
		{
			throw new Exception("The error message suggest that the wrong USER '" + dbmsUsername + "' or PASSWORD to DBMS server '" + dbmsServer + "' was entered. This is a non-recovarable error. DBMS Error Message='" + ex.getMessage() + "'.", ex);
		}
	}
}
