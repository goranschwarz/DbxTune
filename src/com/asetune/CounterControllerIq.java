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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import org.apache.log4j.Logger;

import com.asetune.cm.CountersModel;
import com.asetune.cm.iq.CmIqCache2;
import com.asetune.cm.iq.CmIqConnection;
import com.asetune.cm.iq.CmIqContext;
import com.asetune.cm.iq.CmIqDbspace;
import com.asetune.cm.iq.CmIqDiskActivity2;
import com.asetune.cm.iq.CmIqFile;
import com.asetune.cm.iq.CmIqLocks;
import com.asetune.cm.iq.CmIqMpxIncStatistics;
import com.asetune.cm.iq.CmIqMpxInfo;
import com.asetune.cm.iq.CmIqStatistics;
import com.asetune.cm.iq.CmIqStatus;
import com.asetune.cm.iq.CmIqTransaction;
import com.asetune.cm.iq.CmIqVersionUse;
import com.asetune.cm.iq.CmSaConnActivity;
import com.asetune.cm.iq.CmSaConnInfo;
import com.asetune.cm.iq.CmSaConnProperties;
import com.asetune.cm.iq.CmSaDbProperties;
import com.asetune.cm.iq.CmSaEngProperties;
import com.asetune.cm.iq.CmSummary;
import com.asetune.cm.os.CmOsDiskSpace;
import com.asetune.cm.os.CmOsIostat;
import com.asetune.cm.os.CmOsMeminfo;
import com.asetune.cm.os.CmOsMpstat;
import com.asetune.cm.os.CmOsNwInfo;
import com.asetune.cm.os.CmOsPs;
import com.asetune.cm.os.CmOsUptime;
import com.asetune.cm.os.CmOsVmstat;
import com.asetune.gui.MainFrame;
import com.asetune.pcs.PersistContainer;
import com.asetune.pcs.PersistContainer.HeaderInfo;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Ver;


public class CounterControllerIq 
extends CounterControllerAbstract
{
	private static Logger _logger = Logger.getLogger(CounterControllerIq.class);

	public static final int	   NUMBER_OF_PERFORMANCE_COUNTERS	= 54 + 10 + 20; 

	/**
	 * The default constructor
	 * @param hasGui should we create a GUI or NoGUI collection thread
	 */
	public CounterControllerIq(boolean hasGui)
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
		MainFrame           guiController     = hasGui ? MainFrame.getInstance() : null;

		CmSummary           .create(counterController, guiController);

		// IQ
		CmIqStatus          .create(counterController, guiController);
		// CmIqStatusParsed    .create(counterController, guiController);		
		CmIqStatistics      .create(counterController, guiController);  
		CmIqConnection      .create(counterController, guiController);
		CmIqTransaction     .create(counterController, guiController);
		CmIqVersionUse      .create(counterController, guiController);
		CmIqLocks           .create(counterController, guiController);
		CmIqContext         .create(counterController, guiController);
		// Catalog
		CmSaEngProperties   .create(counterController, guiController);
		CmSaDbProperties    .create(counterController, guiController);
		CmSaConnProperties  .create(counterController, guiController);
		CmSaConnInfo	    .create(counterController, guiController);
		CmSaConnActivity    .create(counterController, guiController);
		// Multiplex
		CmIqMpxInfo         .create(counterController, guiController);
		CmIqMpxIncStatistics.create(counterController, guiController);
		// Files
		CmIqFile            .create(counterController, guiController);
		CmIqDbspace			.create(counterController, guiController);
		CmIqDiskActivity2.create(counterController, guiController);
		// Cache
		CmIqCache2		.create(counterController, guiController);
		// Objects/Statements tab
		// CmIqDan             .create(counterController, guiController); // temporary removed by mdan  
		
//		CmIqMsgLogParser    .create(counterController, guiController);

//		CmObjectActivity   .create(counterController, guiController);
//		CmProcessActivity  .create(counterController, guiController);
//		CmSpidWait         .create(counterController, guiController);
//		CmSpidCpuWait      .create(counterController, guiController);
//		CmOpenDatabases    .create(counterController, guiController);
//		CmTempdbActivity   .create(counterController, guiController);
//		CmSysWaits         .create(counterController, guiController);
//		CmExecutionTime    .create(counterController, guiController);
//		CmEngines          .create(counterController, guiController);
//		CmThreads          .create(counterController, guiController);
//		CmSysLoad          .create(counterController, guiController);
//		CmDataCaches       .create(counterController, guiController);
//		CmCachePools       .create(counterController, guiController);
//		CmDeviceIo         .create(counterController, guiController);
//		CmIoQueueSum       .create(counterController, guiController);
//		CmIoQueue          .create(counterController, guiController);
//		CmIoControllers    .create(counterController, guiController);
//		CmSpinlockActivity .create(counterController, guiController);
//		CmSpinlockSum      .create(counterController, guiController);
//		CmSysmon           .create(counterController, guiController);
//		CmMemoryUsage      .create(counterController, guiController);
//		CmRaSenders        .create(counterController, guiController);
//		CmRaLogActivity    .create(counterController, guiController);
//		CmRaScanners       .create(counterController, guiController);
//		CmRaScannersTime   .create(counterController, guiController);
////		CmRaSqlActivity    .create(counterController, guiController);
////		CmRaSqlMisses      .create(counterController, guiController);
//		CmRaStreamStats    .create(counterController, guiController);
//		CmRaSyncTaskStats  .create(counterController, guiController);
//		CmRaTruncPoint     .create(counterController, guiController);
//		CmRaSysmon         .create(counterController, guiController);
//		CmCachedProcs      .create(counterController, guiController);
//		CmCachedProcsSum   .create(counterController, guiController);
//		CmProcCacheLoad    .create(counterController, guiController);
//		CmProcCallStack    .create(counterController, guiController);
//		CmCachedObjects    .create(counterController, guiController);
//		CmErrorLog         .create(counterController, guiController);
//		CmDeadlock         .create(counterController, guiController);
//		CmLockTimeout      .create(counterController, guiController);
//		CmThresholdEvent   .create(counterController, guiController);
//		CmPCacheModuleUsage.create(counterController, guiController);
//		CmPCacheMemoryUsage.create(counterController, guiController);
//		CmStatementCache   .create(counterController, guiController);
//		CmStmntCacheDetails.create(counterController, guiController);
//		CmActiveObjects    .create(counterController, guiController);
//		CmActiveStatements .create(counterController, guiController);
//		CmBlocking         .create(counterController, guiController);
//		CmTableCompression .create(counterController, guiController);
//		CmMissingStats     .create(counterController, guiController);
//		CmQpMetrics        .create(counterController, guiController);
//		CmSpMonitorConfig  .create(counterController, guiController);
//		CmWorkQueues       .create(counterController, guiController);

		// OS HOST Monitoring
		CmOsIostat         .create(counterController, guiController);
		CmOsVmstat         .create(counterController, guiController);
		CmOsMpstat         .create(counterController, guiController);
		CmOsUptime         .create(counterController, guiController);
		CmOsMeminfo        .create(counterController, guiController);
		CmOsNwInfo         .create(counterController, guiController);
		CmOsDiskSpace      .create(counterController, guiController);
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
		
		_logger.info("Initializing all CM objects, using IQ server version number "+srvVersion+" ("+Ver.versionNumToStr(srvVersion)+"), isClusterEnabled="+isClusterEnabled+" with monTables Install version "+monTablesVersion+" ("+Ver.versionNumToStr(monTablesVersion)+").");

		// initialize all the CM's
		for (CountersModel cm : getCmList())
		{
			_logger.debug("Initializing CM named '"+cm.getName()+"', display name '"+cm.getDisplayName()+"', using IQ server version number "+srvVersion+".");

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

	@Override
	public PersistContainer.HeaderInfo createPcsHeaderInfo()
	{
		// Get session/head info
		String    aseServerName    = null;
		String    aseHostname      = null;
		Timestamp mainSampleTime   = null;
		Timestamp counterClearTime = null;

		String sql = "select getdate(), @@servername, @@servername, CountersCleared='2000-01-01 00:00:00'";

		try
		{
			if ( ! isMonConnected(true, true) ) // forceConnectionCheck=true, closeConnOnFailure=true
//				continue; // goto: while (_running)
				return null;
				
			Statement stmt = getMonConnection().createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				mainSampleTime   = rs.getTimestamp(1);
				aseServerName    = rs.getString(2);
				aseHostname      = rs.getString(3);
				counterClearTime = rs.getTimestamp(4);
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
//					continue; // goto: while (_running)
					return null;
				}
			}
			
			_logger.warn("Problems getting basic status info in 'Counter get loop', reverting back to 'static values'. SQL '"+sql+"', Caught: " + sqlex.toString() );
			mainSampleTime   = new Timestamp(System.currentTimeMillis());
			aseServerName    = "unknown";
			aseHostname      = "unknown";
			counterClearTime = new Timestamp(0);
		}
		
		return new HeaderInfo(mainSampleTime, aseServerName, aseHostname, counterClearTime);
	}

	@Override
	public String getServerTimeCmd()
	{
		return "select getdate() \n";
	}

	@Override
	protected String getIsClosedSql()
	{
		return "select 'IqTune-check:isClosed(conn)'";
	}
	
	@Override
	public void setInRefresh(boolean enterRefreshMode)
	{
		try
		{
			DbxConnection dbxConn = getMonConnection();
			
			if (enterRefreshMode)
			{
				// IQ is better with Versioning if we have it as one big transaction... but lets close the transaction once in a while...
				if (dbxConn.getAutoCommit() == true)
					dbxConn.setAutoCommit(false);
				
				// So when entering a refresh: just finishing off the current transaction.
				dbxConn.commit(); 
			}
		}
		catch(SQLException e)
		{
			_logger.info("Problem when changing the IQ Connection autocommit mode.");
		}

		super.setInRefresh(enterRefreshMode);
	}

	@Override
	public void noGuiConnectErrorHandler(SQLException ex, String dbmsUsername, String dbmsPassword, String dbmsServer, String dbmsHostPortStr, String jdbcUrlOptions) 
	throws Exception
	{
		// Error checking for "invalid password" or other "unrecoverable errors"
		if ("JZ00L".equals(ex.getSQLState()))
		{
			throw new Exception("The error message suggest that the wrong USER '" + dbmsUsername + "' or PASSWORD '" + dbmsPassword + "' to DBMS server '" + dbmsServer + "' was entered. This is a non-recovarable error. DBMS Error Message='" + ex.getMessage() + "'.", ex);
		}
	}
}
