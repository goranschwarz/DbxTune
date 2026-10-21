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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.mysql.CmGlobalStatus;
import com.dbxtune.cm.mysql.CmInnoDbMetrics;
import com.dbxtune.cm.mysql.CmInnodbBpStats;
import com.dbxtune.cm.mysql.CmReplication;
import com.dbxtune.cm.mysql.CmSummary;
import com.dbxtune.cm.mysql.CmSysDiskIo;
import com.dbxtune.cm.mysql.CmSysIndexStats;
import com.dbxtune.cm.mysql.CmSysSession;
import com.dbxtune.cm.mysql.CmSysTableStats;
import com.dbxtune.cm.mysql.CmUserStatus;
import com.dbxtune.cm.os.CmOsDiskSpace;
import com.dbxtune.cm.os.CmOsIostat;
import com.dbxtune.cm.os.CmOsMeminfo;
import com.dbxtune.cm.os.CmOsMpstat;
import com.dbxtune.cm.os.CmOsNwInfo;
import com.dbxtune.cm.os.CmOsPs;
import com.dbxtune.cm.os.CmOsUptime;
import com.dbxtune.cm.os.CmOsVmstat;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.pcs.PersistContainer;
import com.dbxtune.pcs.PersistContainer.HeaderInfo;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfoMySql;
import com.dbxtune.utils.AseConnectionUtils;
import com.dbxtune.utils.Ver;


public class CounterControllerMySql 
extends CounterControllerAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final int	   NUMBER_OF_PERFORMANCE_COUNTERS	= 54 + 10 + 20; 

	/**
	 * The default constructor
	 * @param hasGui should we create a GUI or NoGUI collection thread
	 */
	public CounterControllerMySql(boolean hasGui)
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

		// Summary panel (the counters/values on the left side)
		CmSummary           .create(counterController, guiController);

		// Server
		CmSysSession        .create(counterController, guiController);
		CmGlobalStatus      .create(counterController, guiController);
		CmUserStatus        .create(counterController, guiController);
		CmInnoDbMetrics     .create(counterController, guiController);
		CmReplication       .create(counterController, guiController);

		// Object Access
		CmSysTableStats     .create(counterController, guiController);
		CmSysIndexStats     .create(counterController, guiController);
		

		// Cache
		CmInnodbBpStats     .create(counterController, guiController);

		// Disk
		CmSysDiskIo         .create(counterController, guiController);

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

		// What can this login do? The CM's only need: PROCESS, REPLICATION CLIENT, SELECT on performance_schema, SELECT + EXECUTE on sys (see DbmsSetup/mysql.txt)
		// Tested by doing it (not by reading SHOW GRANTS), so privileges that comes from a ROLE are also counted
		DbmsVersionInfoMySql versionInfo = (conn.getDbmsVersionInfo() instanceof DbmsVersionInfoMySql) ? (DbmsVersionInfoMySql) conn.getDbmsVersionInfo() : new DbmsVersionInfoMySql(srvVersion);
		if ( ! versionInfo.isMariaDb() )
		{
			String replicaStatusCmd = srvVersion >= Ver.ver(8,0,22) ? "SHOW REPLICA STATUS" : "SHOW SLAVE STATUS";

			// NOTE: 'where 1=2' can NOT be used for the INNODB_* views: the PROCESS privilege is only checked when the view is filled
			boolean hasProcess    = canExecute(conn, "select count(*) from information_schema.INNODB_TRX");
			boolean hasReplClient = canExecute(conn, replicaStatusCmd);
			boolean hasPerfSchema = canExecute(conn, "select 1 from performance_schema.threads where 1=2");
			boolean hasSysSelect  = canExecute(conn, "select 1 from sys.version where 1=2");
			boolean hasSysExecute = canExecute(conn, "select sys.format_bytes(0)");

			String has = "PROCESS=" + hasProcess + ", REPLICATION CLIENT=" + hasReplClient + ", SELECT on performance_schema=" + hasPerfSchema + ", SELECT on sys=" + hasSysSelect + ", EXECUTE on sys=" + hasSysExecute + ".";
			if (hasProcess && hasReplClient && hasPerfSchema && hasSysSelect && hasSysExecute)
				_logger.info("The MySQL login has all it needs: " + has);
			else
				_logger.warn("The MySQL login is missing privileges, CM's that can't read their views will be DISABLED. The login has: " + has
						+ " Fix: GRANT PROCESS, REPLICATION CLIENT ON *.* TO <user>; GRANT SELECT ON performance_schema.* TO <user>; GRANT SELECT, EXECUTE ON sys.* TO <user>;");

			// The performance_schema consumers that the CM's depends on (they are ON by default)
			String sql = "select NAME from performance_schema.setup_consumers where ENABLED = 'NO' and NAME in ('global_instrumentation', 'thread_instrumentation', 'statements_digest', 'events_statements_current', 'events_transactions_current')";
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
					_logger.warn("The performance_schema consumer '" + rs.getString(1) + "' is disabled, so some Counter Models will show less (or no) data. Fix: UPDATE performance_schema.setup_consumers SET ENABLED='YES' WHERE NAME='" + rs.getString(1) + "'; (and add 'performance-schema-consumer-" + rs.getString(1).replace('_', '-') + "=ON' to my.cnf, so it survives a restart)");
			}
			catch (SQLException ex)
			{
				_logger.info("Problems checking the performance_schema consumers. sql=|" + sql + "|, Caught: " + ex);
			}
		}

		_logger.info("Initializing all CM objects, using MySQL version number " + srvVersion + ". (" + Ver.versionNumToStr(srvVersion) + ")");

		// initialize all the CM's
		for (CountersModel cm : getCmList())
		{
			_logger.debug("Initializing CM named '" + cm.getName() + "', display name '" + cm.getDisplayName() + "', using MySQL version number " + srvVersion + ".");

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

	/** Can the login execute this? (any error is a NO) */
	private static boolean canExecute(DbxConnection conn, String sql)
	{
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while (rs.next())
				;
			return true;
		}
		catch (SQLException ex)
		{
			_logger.debug("canExecute(): sql=|" + sql + "|, Caught: " + ex);
			return false;
		}
	}

	@Override
	public void checkServerSpecifics()
	{
	}

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
//		String sql = "select current_timestamp, @@hostname as Instance, @@hostname as onHost";
		String sql = "select current_timestamp, @@hostname as onHost, @@port as portNum";

		try
		{
			if ( ! isMonConnected(true, true) ) // forceConnectionCheck=true, closeConnOnFailure=true
				return null;
				
			Statement stmt = getMonConnection().createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				Timestamp ts   = rs.getTimestamp(1);
				String    host = rs.getString(2).trim();
				String    port = rs.getString(3).trim();
//				counterClearTime = rs.getTimestamp(4);

				mainSampleTime   = ts;
				dbmsServerName   = DbxTune.stripSrvName(host + ":" + port);
				dbmsHostname     = host;
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException sqlex)
		{
			// Is the connection lost?
			boolean forceConnectionCheck = true;
			boolean closeConnOnFailure   = true;
			if ( ! isMonConnected(forceConnectionCheck, closeConnOnFailure) )
			{
				_logger.info("Problems getting basic status info in 'Counter get loop', the connection is lost. So lets start from the top. Caught: " + sqlex);
				return null;
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
		return "SELECT CURRENT_TIMESTAMP; \n";
	}

	@Override
	protected String getIsClosedSql()
	{
		return "SELECT 'MySqlTune-check:isClosed(conn)'";
	}

	@Override
	public boolean isSqlBatchingSupported()
	{
		return false;
	}

	@Override
	public void setInRefresh(boolean enterRefreshMode)
	{
		try
		{
			DbxConnection dbxConn = getMonConnection();
			
			if (enterRefreshMode)
			{
				// Lets use ONE transaction for every refresh
				if (dbxConn.getAutoCommit() == true)
					dbxConn.setAutoCommit(false);
			}
			else
			{
				// When leaving refresh mode... set AutoCommit to true -- so we don't hold anything while sleeping
				if (dbxConn.getAutoCommit() == false)
					dbxConn.setAutoCommit(true);
			}
		}
		catch(SQLException e)
		{
			_logger.info("Problem when changing the MySQL Connection autocommit mode. Caught: " + e);
		}

		super.setInRefresh(enterRefreshMode);
	}

	@Override
	public void noGuiConnectErrorHandler(SQLException ex, String dbmsUsername, String dbmsPassword, String dbmsServer, String dbmsHostPortStr, String jdbcUrlOptions) 
	throws Exception
	{
		// Wrong user or password: MySQL error 1045, SQLState 28000 -- "Access denied for user 'xxx'@'host' (using password: YES)"
		// This is a non-recoverable error (do NOT print the password)
		if (ex.getErrorCode() == 1045 || "28000".equals(ex.getSQLState()))
		{
			throw new Exception("The error message suggest that the wrong USER '" + dbmsUsername + "' or PASSWORD to DBMS server '" + dbmsServer + "' was entered. This is a non-recovarable error. DBMS Error Message='" + ex.getMessage() + "'.", ex);
		}
	}
}
