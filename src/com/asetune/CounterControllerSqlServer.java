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

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.cm.CountersModel;
import com.asetune.cm.os.CmOsDiskSpace;
import com.asetune.cm.os.CmOsIostat;
import com.asetune.cm.os.CmOsMeminfo;
import com.asetune.cm.os.CmOsMpstat;
import com.asetune.cm.os.CmOsNwInfo;
import com.asetune.cm.os.CmOsUptime;
import com.asetune.cm.os.CmOsVmstat;
import com.asetune.cm.sqlserver.CmActiveStatements;
import com.asetune.cm.sqlserver.CmDatabases;
import com.asetune.cm.sqlserver.CmDbIo;
import com.asetune.cm.sqlserver.CmDeviceIo;
import com.asetune.cm.sqlserver.CmExecCursors;
import com.asetune.cm.sqlserver.CmExecFunctionStats;
import com.asetune.cm.sqlserver.CmExecProcedureStats;
import com.asetune.cm.sqlserver.CmExecQueryStats;
import com.asetune.cm.sqlserver.CmExecRequests;
import com.asetune.cm.sqlserver.CmExecSessions;
import com.asetune.cm.sqlserver.CmExecTriggerStats;
import com.asetune.cm.sqlserver.CmIndexOpStat;
import com.asetune.cm.sqlserver.CmIndexUsage;
import com.asetune.cm.sqlserver.CmOpenTransactions;
import com.asetune.cm.sqlserver.CmOptimizer;
import com.asetune.cm.sqlserver.CmOsLatchStats;
import com.asetune.cm.sqlserver.CmPerfCounters;
import com.asetune.cm.sqlserver.CmProcedureStats;
import com.asetune.cm.sqlserver.CmSchedulers;
import com.asetune.cm.sqlserver.CmSpidWait;
import com.asetune.cm.sqlserver.CmSpinlocks;
import com.asetune.cm.sqlserver.CmSummary;
import com.asetune.cm.sqlserver.CmTempdbSpidUsage;
import com.asetune.cm.sqlserver.CmTempdbUsage;
import com.asetune.cm.sqlserver.CmWaitStats;
import com.asetune.cm.sqlserver.CmWaitingTasks;
import com.asetune.cm.sqlserver.CmWho;
import com.asetune.cm.sqlserver.ToolTipSupplierSqlServer;
import com.asetune.gui.MainFrame;
import com.asetune.gui.swing.GTable.ITableTooltip;
import com.asetune.pcs.PersistContainer;
import com.asetune.pcs.PersistContainer.HeaderInfo;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Ver;


public class CounterControllerSqlServer 
extends CounterControllerAbstract
{
	private static Logger _logger = Logger.getLogger(CounterControllerSqlServer.class);

	public static final int	   NUMBER_OF_PERFORMANCE_COUNTERS	= 54 + 10 + 20; 

	/**
	 * The default constructor
	 * @param hasGui should we create a GUI or NoGUI collection thread
	 */
	public CounterControllerSqlServer(boolean hasGui)
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
                            
		CmWho               .create(counterController, guiController);
		CmSpidWait          .create(counterController, guiController);
		CmExecSessions      .create(counterController, guiController);
		CmExecRequests      .create(counterController, guiController);
		CmDatabases         .create(counterController, guiController);
		CmTempdbUsage       .create(counterController, guiController);
		CmTempdbSpidUsage   .create(counterController, guiController);
		CmSchedulers        .create(counterController, guiController);
		CmWaitStats         .create(counterController, guiController);
		CmWaitingTasks      .create(counterController, guiController);
		CmOsLatchStats      .create(counterController, guiController);
		CmPerfCounters      .create(counterController, guiController);
		CmOptimizer         .create(counterController, guiController);
		CmSpinlocks         .create(counterController, guiController);
                            
		CmActiveStatements  .create(counterController, guiController);
		CmOpenTransactions  .create(counterController, guiController);
		CmIndexUsage        .create(counterController, guiController);
		CmIndexOpStat       .create(counterController, guiController);
		CmExecQueryStats    .create(counterController, guiController);
		CmExecProcedureStats.create(counterController, guiController);
		CmExecFunctionStats .create(counterController, guiController);
		CmExecTriggerStats  .create(counterController, guiController);
		CmExecCursors       .create(counterController, guiController);

		CmProcedureStats    .create(counterController, guiController);

		CmDeviceIo          .create(counterController, guiController);
		CmDbIo              .create(counterController, guiController);


		// OS HOST Monitoring
		CmOsIostat          .create(counterController, guiController);
		CmOsVmstat          .create(counterController, guiController);
		CmOsMpstat          .create(counterController, guiController);
		CmOsUptime          .create(counterController, guiController);
		CmOsMeminfo         .create(counterController, guiController);
		CmOsNwInfo          .create(counterController, guiController);
		CmOsDiskSpace       .create(counterController, guiController);

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
	public void initCounters(DbxConnection conn, boolean hasGui, long srvVersion, boolean isAzure, long monTablesVersion)
	throws Exception
	{
		if (isInitialized())
			return;

		if ( ! AseConnectionUtils.isConnectionOk(conn, hasGui, null))
			throw new Exception("Trying to initialize the counters with a connection this seems to be broken.");

			
		if (! isCountersCreated())
			createCounters(hasGui);
		
		// Get active SQL Server Roles/Permissions
		List<String> activeServerPermissionList = conn.getActiveServerRolesOrPermissions();

		_logger.info("Initializing all CM objects, using MS SQL-Server version number "+srvVersion+" ("+Ver.versionNumToStr(srvVersion)+").");

		// get SQL-Server Specific properties and store them in setDbmsProperties() 
		initializeDbmsProperties(conn);
		
		isAzure = isAzure();

		// initialize all the CM's
		for (CountersModel cm : getCmList())
		{
			_logger.debug("Initializing CM named '"+cm.getName()+"', display name '"+cm.getDisplayName()+"', using MS SQL-Server version number "+srvVersion+".");

			// set the version
			cm.setServerVersion(monTablesVersion);
			cm.setClusterEnabled(isAzure);

			// set the active roles, so it can be used in initSql()
			cm.setActiveServerRolesOrPermissions(activeServerPermissionList);

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

	private void initializeDbmsProperties(DbxConnection conn)
	{
		String sql = "select ServerProperty('Edition')";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql) )
		{
			while(rs.next())
			{
				setDbmsProperty(PROPKEY_Edition, rs.getString(1));
			}
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems Initializing DBMS Properties, using sql='"+sql+"'. Caught: "+ex);
		}

		sql = "select @@version";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql) )
		{
			while(rs.next())
			{
				setDbmsProperty(PROPKEY_VersionStr, rs.getString(1));
			}
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems Initializing DBMS Properties, using sql='"+sql+"'. Caught: "+ex);
		}
	}
	
	public static final String PROPKEY_Edition    = CounterControllerSqlServer.class.getSimpleName() + ".Edition";
	public static final String PROPKEY_VersionStr = CounterControllerSqlServer.class.getSimpleName() + ".VersionStr";

	public boolean isAzure()
	{
		String edition = getDbmsProperty(PROPKEY_Edition, "");
		
		return (edition != null && edition.toLowerCase().indexOf("azure") >= 0);
	}

	public boolean isLinux()
	{
		String versionStr = getDbmsProperty(PROPKEY_VersionStr, "");
		
		return (versionStr != null && versionStr.indexOf("on Linux") >= 0);
	}

	@Override
	public void checkServerSpecifics()
	{
	}

	@Override
	public PersistContainer.HeaderInfo createPcsHeaderInfo()
	{
//		return new HeaderInfo(new Timestamp(System.currentTimeMillis()), "DUMMY_HANA", "DUMMY_HANA", new Timestamp(System.currentTimeMillis()));
		// Get session/head info
		String    sqlServerName    = null;
		String    sqlHostname      = null;
		Timestamp mainSampleTime   = null;
		Timestamp counterClearTime = null;

//		String sql = "select getdate(), @@servername, @@servername, CountersCleared='2000-01-01 00:00:00'";
		String sql = "select getdate(), convert(varchar(100),isnull(SERVERPROPERTY('InstanceName'), @@servername)), convert(varchar(100),SERVERPROPERTY('MachineName')), CountersCleared='2000-01-01 00:00:00'";

		try
		{
			if ( ! isMonConnected(true, true) ) // forceConnectionCheck=true, closeConnOnFailure=true
				return null;
				
			Statement stmt = getMonConnection().createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				mainSampleTime   = rs.getTimestamp(1);
				sqlServerName    = rs.getString(2);
				sqlHostname      = rs.getString(3);
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
					return null;
				}
			}
			
			_logger.warn("Problems getting basic status info in 'Counter get loop', reverting back to 'static values'. SQL '"+sql+"', Caught: " + sqlex.toString() );
			mainSampleTime   = new Timestamp(System.currentTimeMillis());
			sqlServerName    = "unknown";
			sqlHostname      = "unknown";
			counterClearTime = new Timestamp(0);
		}
//System.out.println("SQLSERVER: createPcsHeaderInfo(): mainSampleTime='"+mainSampleTime+"', sqlServerName='"+sqlServerName+"', sqlHostname='"+sqlHostname+"', counterClearTime='"+counterClearTime+"'");
		return new HeaderInfo(mainSampleTime, sqlServerName, sqlHostname, counterClearTime);
	}

	@Override
	public String getServerTimeCmd()
	{
		return "select getdate() \n";
	}

	@Override
	protected String getIsClosedSql()
	{
		return "SELECT 'SqlServerTune-check:isClosed(conn)'";
	}

	@Override
	public ITableTooltip createCmToolTipSupplier(CountersModel cm)
	{
		return new ToolTipSupplierSqlServer(cm);
	}


	/**
	 * From a data/log file... get the directory. (If it's a SQL-Server on Linux: Remove drive letter (if it exists) and change any bask slash '\' to forward slashes '/' 
	 * @param osFileName
	 * @return
	 */
	public static String resolvFileNameToDirectory(String osFileName)
	{
		if (CounterController.hasInstance())
		{
			CounterControllerSqlServer instance = (CounterControllerSqlServer) CounterController.getInstance();

			File f = new File(osFileName);
			String dir = f.getParent();

			// If not windows: remove drive letter, and fix backslash '\' to forward slash '/'
			if ( instance.isLinux() )
			{
				// if starts with 'c:', then remove the drive, since it's Linux 
				if (dir.matches("(?is)[A-Z]:.*"))
					dir = dir.substring(2);
				
				dir = dir.replace('\\', '/');
			}
			
			return dir;
		}
		else
		{
			File f = new File(osFileName);
			return f.getParent();
		}
	}
}
