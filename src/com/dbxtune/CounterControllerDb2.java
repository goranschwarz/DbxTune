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
package com.dbxtune;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.db2.CmConnectionSummary;
import com.dbxtune.cm.db2.CmErrorLog;
import com.dbxtune.cm.db2.CmMonGetBufferPool;
import com.dbxtune.cm.db2.CmMonGetConnection;
import com.dbxtune.cm.db2.CmMonGetIndex;
import com.dbxtune.cm.db2.CmMonGetTable;
import com.dbxtune.cm.db2.CmSummary;
import com.dbxtune.cm.db2.CmSystemResourses;
import com.dbxtune.cm.db2.CmTableSpace;
import com.dbxtune.cm.db2.CmTableSpaceAll;
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
import com.dbxtune.utils.AseConnectionUtils;
import com.dbxtune.utils.Ver;


public class CounterControllerDb2 
extends CounterControllerAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final int	   NUMBER_OF_PERFORMANCE_COUNTERS	= 54 + 10 + 20; 

	/**
	 * The default constructor
	 * @param hasGui should we create a GUI or NoGUI collection thread
	 */
	public CounterControllerDb2(boolean hasGui)
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
		CmConnectionSummary .create(counterController, guiController);
		CmMonGetConnection  .create(counterController, guiController);
		CmSystemResourses   .create(counterController, guiController);
//		CmGlobalStatus      .create(counterController, guiController);
//		CmUserStatus        .create(counterController, guiController);
		CmTableSpace        .create(counterController, guiController);
		CmTableSpaceAll     .create(counterController, guiController);
		CmErrorLog          .create(counterController, guiController);

		// Object Access
		CmMonGetTable       .create(counterController, guiController);
		CmMonGetIndex       .create(counterController, guiController);
//		CmSysIndexStats     .create(counterController, guiController);
		

		// Cache
		CmMonGetBufferPool  .create(counterController, guiController);
//		CmInnodbBpStats     .create(counterController, guiController);

		// Disk
//		CmSysDiskIo         .create(counterController, guiController);

		// OS HOST Monitoring
		CmOsIostat          .create(counterController, guiController);
		CmOsVmstat          .create(counterController, guiController);
		CmOsMpstat          .create(counterController, guiController);
		CmOsUptime          .create(counterController, guiController);
		CmOsMeminfo         .create(counterController, guiController);
		CmOsNwInfo          .create(counterController, guiController);
		CmOsDiskSpace       .create(counterController, guiController);
		CmOsPs             .create(counterController, guiController);

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
		
		_logger.info("Initializing all CM objects, using DB2 version number "+srvVersion+" ("+Ver.versionNumToStr(srvVersion)+").");

		// initialize all the CM's
		for (CountersModel cm : getCmList())
		{
			_logger.debug("Initializing CM named '"+cm.getName()+"', display name '"+cm.getDisplayName()+"', using DB2 version number "+srvVersion+".");

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
		// If we want to check any DBMS specifics at the TOP of every "check loop"
	}

	@Override
	public PersistContainer.HeaderInfo createPcsHeaderInfo()
	{
		// Get session/head info
		Timestamp mainSampleTime   = null;
		String    dbmsServerName   = null;     // Used when storing information in Persistent Counter Store 
		String    dbmsHostname     = null;     // Used when storing information in Persistent Counter Store
		Timestamp counterClearTime = new Timestamp(0);

//		String sql = "select current_timestamp, CURRENT_SERVER as Instance, HOST_NAME as onHost from table(SYSPROC.ENV_GET_SYS_INFO())";
		String sql = "" 
				+ "select \n"
				+ "    current_timestamp, \n"
				+ "    (select INST_NAME from table(SYSPROC.ENV_GET_INST_INFO())) as Instance, \n"
				+ "    (select HOST_NAME from table(SYSPROC.ENV_GET_SYS_INFO()) ) as onHost \n"
				+ "from SYSIBM.SYSDUMMY1";

		try
		{
			if ( ! isMonConnected(true, true) ) // forceConnectionCheck=true, closeConnOnFailure=true
				return null;
				
			Statement stmt = getMonConnection().createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				mainSampleTime   = rs.getTimestamp(1);
				dbmsServerName   = rs.getString(2);
				dbmsHostname     = rs.getString(3);
//				counterClearTime = rs.getTimestamp(4);
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
		return "SELECT CURRENT_TIMESTAMP from SYSIBM.SYSDUMMY1 \n";
	}

	@Override
	protected String getIsClosedSql()
	{
		return "SELECT 'Db2Tune-check:isClosed(conn)' from SYSIBM.SYSDUMMY1";
	}

	@Override
	public boolean isSqlBatchingSupported()
	{
		return false;
	}

//	@Override
//	public void setInRefresh(boolean enterRefreshMode)
//	{
//		try
//		{
//			DbxConnection dbxConn = getMonConnection();
//			
//			if (enterRefreshMode)
//			{
//				// Lets use ONE transaction for every refresh
//				if (dbxConn.getAutoCommit() == true)
//					dbxConn.setAutoCommit(false);
//				
//				// So when entering a refresh: just finishing off the current transaction.
//				dbxConn.commit(); 
//			}
//		}
//		catch(SQLException e)
//		{
//			_logger.info("Problem when changing the IQ Connection autocommit mode.");
//		}
//
//		super.setInRefresh(enterRefreshMode);
//	}

	@Override
	public void noGuiConnectErrorHandler(SQLException ex, String dbmsUsername, String dbmsPassword, String dbmsServer, String dbmsHostPortStr, String jdbcUrlOptions) 
	throws Exception
	{
		// Error checking for "invalid password" or other "unrecoverable errors"
		if (ex.getMessage().contains("ERRORCODE=-4499"))  // NOT Sure if this is correct... need to be tested
		{
			throw new Exception("The error message suggest that the wrong USER '" + dbmsUsername + "' or PASSWORD '" + dbmsPassword + "' to DBMS server '" + dbmsServer + "' was entered. This is a non-recovarable error. DBMS Error Message='" + ex.getMessage() + "'.", ex);
		}
	}
}
