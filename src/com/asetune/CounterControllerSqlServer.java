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

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

import javax.swing.JOptionPane;

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
import com.asetune.cm.sqlserver.CmAlwaysOn;
import com.asetune.cm.sqlserver.CmDatabases;
import com.asetune.cm.sqlserver.CmDbIo;
import com.asetune.cm.sqlserver.CmDeviceIo;
import com.asetune.cm.sqlserver.CmErrorLog;
import com.asetune.cm.sqlserver.CmExecCursors;
import com.asetune.cm.sqlserver.CmExecFunctionStats;
import com.asetune.cm.sqlserver.CmExecProcedureStats;
import com.asetune.cm.sqlserver.CmExecQueryStats;
import com.asetune.cm.sqlserver.CmExecTriggerStats;
import com.asetune.cm.sqlserver.CmIndexMissing;
import com.asetune.cm.sqlserver.CmIndexOpStat;
import com.asetune.cm.sqlserver.CmIndexPhysical;
import com.asetune.cm.sqlserver.CmIndexUnused;
import com.asetune.cm.sqlserver.CmIndexUsage;
import com.asetune.cm.sqlserver.CmMemoryClerks;
import com.asetune.cm.sqlserver.CmMemoryGrants;
import com.asetune.cm.sqlserver.CmOpenTransactions;
import com.asetune.cm.sqlserver.CmOptimizer;
import com.asetune.cm.sqlserver.CmOsLatchStats;
import com.asetune.cm.sqlserver.CmPerfCounters;
import com.asetune.cm.sqlserver.CmProcedureStats;
import com.asetune.cm.sqlserver.CmQueryTransformStat;
import com.asetune.cm.sqlserver.CmSchedulers;
import com.asetune.cm.sqlserver.CmSessions;
import com.asetune.cm.sqlserver.CmSpidWait;
import com.asetune.cm.sqlserver.CmSpinlocks;
import com.asetune.cm.sqlserver.CmSummary;
import com.asetune.cm.sqlserver.CmTableSize;
import com.asetune.cm.sqlserver.CmTempdbSpidUsage;
import com.asetune.cm.sqlserver.CmTempdbUsage;
import com.asetune.cm.sqlserver.CmWaitStats;
import com.asetune.cm.sqlserver.CmWaitingTasks;
import com.asetune.cm.sqlserver.CmWho;
import com.asetune.cm.sqlserver.CmWorkers;
import com.asetune.cm.sqlserver.ToolTipSupplierSqlServer;
import com.asetune.gui.MainFrame;
import com.asetune.gui.swing.GTable.ITableTooltip;
import com.asetune.pcs.PersistContainer;
import com.asetune.pcs.PersistContainer.HeaderInfo;
import com.asetune.pcs.SqlServerQueryStoreExtractor;
import com.asetune.sql.conn.ConnectionProp;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.SqlServerUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;


public class CounterControllerSqlServer 
extends CounterControllerAbstract
{
	private static Logger _logger = Logger.getLogger(CounterControllerSqlServer.class);

	public static final int	   NUMBER_OF_PERFORMANCE_COUNTERS	= 54 + 10 + 20; 

	public static final String  PROPKEY_onRefresh_setDirtyReads = "SqlServerTune.onRefresh.setDirtyReads";
	public static final boolean DEFAULT_onRefresh_setDirtyReads = true;
	
	public static final String  PROPKEY_onRefresh_setLockTimeout = "SqlServerTune.onRefresh.setLockTimeout";
	public static final boolean DEFAULT_onRefresh_setLockTimeout = true;
	
	public static final String  PROPKEY_onRefresh_setLockTimeout_ms = "SqlServerTune.onRefresh.setLockTimeout.ms";
	public static final int     DEFAULT_onRefresh_setLockTimeout_ms = 3000;
	
	public static final String  PROPKEY_onPcsDatabaseRollover_captureQueryStore = "SqlServerTune.onPcsDatabaseRollover.captureQueryStore";
	public static final boolean DEFAULT_onPcsDatabaseRollover_captureQueryStore = true;

	public static final String  PROPKEY_onPcsDatabaseRollover_captureQueryStore_skipServer = "SqlServerTune.onPcsDatabaseRollover.captureQueryStore.skipServer";
	public static final String  DEFAULT_onPcsDatabaseRollover_captureQueryStore_skipServer = "";
	
	public static final String  PROPKEY_onPcsDatabaseRollover_captureQueryStore_daysToCopy = "SqlServerTune.onPcsDatabaseRollover.captureQueryStore.daysToCopy";
	public static final int     DEFAULT_onPcsDatabaseRollover_captureQueryStore_daysToCopy = 1; // -1=ALL, 1=OneDay, 2=TwoDays...

	
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
		MainFrame guiController = hasGui ? MainFrame.getInstance() : null;

		CmSummary            .create(counterController, guiController);
                             
		CmWho                .create(counterController, guiController);
		CmSessions           .create(counterController, guiController);
		CmSpidWait           .create(counterController, guiController);
//		CmExecSessions       .create(counterController, guiController);
//		CmExecRequests       .create(counterController, guiController);
		CmDatabases          .create(counterController, guiController);
		CmTempdbUsage        .create(counterController, guiController);
		CmTempdbSpidUsage    .create(counterController, guiController);
		CmSchedulers         .create(counterController, guiController);
		CmWaitStats          .create(counterController, guiController);
		CmWaitingTasks       .create(counterController, guiController);
		CmMemoryClerks       .create(counterController, guiController);
		CmMemoryGrants       .create(counterController, guiController);
		CmErrorLog           .create(counterController, guiController);
		CmOsLatchStats       .create(counterController, guiController);
		CmPerfCounters       .create(counterController, guiController);
		CmWorkers            .create(counterController, guiController);
		CmAlwaysOn           .create(counterController, guiController);
		CmOptimizer          .create(counterController, guiController);
		CmQueryTransformStat .create(counterController, guiController);
		CmSpinlocks          .create(counterController, guiController);
                             
		CmActiveStatements   .create(counterController, guiController);
		CmOpenTransactions   .create(counterController, guiController);
		CmIndexUsage         .create(counterController, guiController);
		CmIndexOpStat        .create(counterController, guiController);
		CmTableSize          .create(counterController, guiController);
		CmIndexPhysical      .create(counterController, guiController);
		CmIndexMissing       .create(counterController, guiController);
		CmIndexUnused        .create(counterController, guiController);
		CmExecQueryStats     .create(counterController, guiController);
		CmExecProcedureStats .create(counterController, guiController);
		CmExecFunctionStats  .create(counterController, guiController);
		CmExecTriggerStats   .create(counterController, guiController);
		CmExecCursors        .create(counterController, guiController);
                             
		CmProcedureStats     .create(counterController, guiController);
                             
		CmDeviceIo           .create(counterController, guiController);
		CmDbIo               .create(counterController, guiController);


		// OS HOST Monitoring
		CmOsIostat           .create(counterController, guiController);
		CmOsVmstat           .create(counterController, guiController);
		CmOsMpstat           .create(counterController, guiController);
		CmOsUptime           .create(counterController, guiController);
		CmOsMeminfo          .create(counterController, guiController);
		CmOsNwInfo           .create(counterController, guiController);
		CmOsDiskSpace        .create(counterController, guiController);

		// USER DEFINED COUNTERS
		createUserDefinedCounterModels(counterController, guiController);
		createUserDefinedCounterModelHostMonitors(counterController, guiController);

		// done
		setCountersIsCreated(true);
	}

	/**
	 * Reset All CM's etc, this so we build new SQL statements if we connect to a different ASE version<br>
	 * Most possible called from disconnect() or similar
	 * 
	 * @param resetAllCms call reset() on all cm's
	 */
	@Override
	public void reset(boolean resetAllCms)
	{
		super.reset(resetAllCms);

		_useLastQueryPlanStats = false;
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
		initializeDbmsProperties(conn, srvVersion, hasGui);
		
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

	private void initializeDbmsProperties(DbxConnection conn, long srvVersion, boolean hasGui)
	{
		//------------------------------------------------
		// Get server EDITION
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
			_logger.warn("Problems geting SQL-Server 'Edition', using sql='"+sql+"'. Caught: "+ex);
		}

		//------------------------------------------------
		// Get VERSION
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
			_logger.warn("Problems getting @@version, using sql='"+sql+"'. Caught: "+ex);
		}


		//------------------------------------------------
		// Get Global Trace Flags
		List<Integer> activeGlobalTraceFlagList = Collections.emptyList(); 
		try 
		{ 
			activeGlobalTraceFlagList = SqlServerUtils.getGlobalTraceFlags(conn); 
		}
		catch(SQLException ex)
		{
			_logger.error("Problems getting SQL-Server 'Global Trace Flag List'. Caught: " + ex);
		}

		// Below is used to build GUI Messages on LAST_QUERY_PLAN_STATS and "missing" Trace Flags
		String guiHtmlMessages = "";
		
		//------------------------------------------------
		// for SQL-Server 2019 - Check if TraceFlag 2451 is set or if any databases has database scoped configuration 'LAST_QUERY_PLAN_STATS'
		_useLastQueryPlanStats = false;
		if (srvVersion >= Ver.ver(2019))
		{
			boolean disableLastQueryPlanStats = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_DisableLastQueryPlanStats, false);

			// If not MANUALLY Disabled... write info abut that!
			if ( disableLastQueryPlanStats )
			{
				_logger.info("For SQL-Server 2019, Actual-Query-Plans was DISABLED using property '" + PROPKEY_DisableLastQueryPlanStats + "=true'. " + Version.getAppName() + " will use 'sys.dm_exec_query_plan' to get Estimated-Query-Plans.");
			}
			else
			{
				// Check if GlobalTraceFlag=2451
				if (activeGlobalTraceFlagList.contains(2451))
				{
					_useLastQueryPlanStats = true;
					_logger.info("For SQL-Server 2019, Actual-Query-Plans is ENABLED, found Global Trace Flag 2451. This can be overridden/disabled by specifying property '" + PROPKEY_DisableLastQueryPlanStats + "=true'. Actual-Query-Plans WILL be attemted using 'sys.dm_exec_query_plan_stats' instead of 'sys.dm_exec_query_plan'" );
				}
				
				// Check if DB Scoped Configuration 'LAST_QUERY_PLAN_STATS' in any databases
				try
				{
//					List<String> dbnames = SqlServerUtils.getDatabasesWithLastActualQueryPlansCapability(conn);
					Map<String, Map<String, Object>> dbCfgMap = SqlServerUtils.getDatabasesScopedConfigNonDefaults(conn, "LAST_QUERY_PLAN_STATS");
					if ( ! dbCfgMap.isEmpty() )
					{
						_useLastQueryPlanStats = true;
						_logger.info("For SQL-Server 2019, Actual-Query-Plans is ENABLED, found Database Scoped Configuration 'LAST_QUERY_PLAN_STATS' in database(s) " + dbCfgMap.keySet() + ". This can be overridden/disabled by specifying property '" + PROPKEY_DisableLastQueryPlanStats + "=true'. Actual-Query-Plans WILL be attemted using 'sys.dm_exec_query_plan_stats' instead of 'sys.dm_exec_query_plan'" );
					}
				}
				catch(SQLException ex)
				{
					_logger.error("Problems getting SQL-Server 'Database Scoped Configuration - LAST_QUERY_PLAN_STATS'. So we will NOT use Actual-Query-Plans (sys.dm_exec_query_plan_stats). Instead Estimated-Query-Plans will be fetched using 'sys.dm_exec_query_plan'", ex);
					_useLastQueryPlanStats = false;
				}
				
				if (_useLastQueryPlanStats == false)
				{
					_logger.warn("For SQL-Server 2019, Actual-Query-Plans will NOT be used/available. " + Version.getAppName() + " will use 'sys.dm_exec_query_plan' to get Estimated-Query-Plans. To enable Actual-Query-Plans please set Trace Flag 2451 or set Database Scoped Configuration 'LAST_QUERY_PLAN_STATS' in the database(s) where you need this.");

					guiHtmlMessages += "<li>For SQL-Server 2019, <b>Actual-Query-Plans</b> will NOT be available. " + Version.getAppName() + " will use 'sys.dm_exec_query_plan' to get Estimated-Query-Plans. <br>"
					             + "To enable Actual-Query-Plans please set Trace Flag 2451 on a server level or set Database Scoped Configuration 'LAST_QUERY_PLAN_STATS' in the database(s) where you need this."
					             + "<ul>"
					             + "   <li>Set Trace Flag: in startup file add <code>-T2451</code> or temporary set it using <code>dbcc traceon(2451, -1)</code>, wich will <b>not</b> survive a restart!</li>"
					             + "   <li>Or issue command <code>alter database scoped configuration set LAST_QUERY_PLAN_STATS = ON</code> for the database(s) you want to enable Actual-Query-Plans.</li>"
					             + "</ul>"
					             + "</li>";
				}
			}
		}
		
		// Check for trace flag 7412 (between 2016 SP1 & 2019)
		//
		// 7412: Enables the lightweight query execution statistics profiling infrastructure. For more information, see this Microsoft Support article.
		// Note: This trace flag applies to SQL Server 2016 (13.x) SP1 and higher builds. Starting with SQL Server 2019 (15.x) this trace flag has no effect because lightweight profiling is enabled by default.
		// Scope: global only
		if (srvVersion >= Ver.ver(2016,0,0, 1) && srvVersion < Ver.ver(2019))
		{
			if ( ! activeGlobalTraceFlagList.contains(7412) )
			{
				_logger.warn("For SQL-Server between 2016 SP1 and below 2019, Trace flag 7412 is needed to view Live-Query-Plans for other sessions. This Trace Flag is MISSING so Live-Query-Plans may be missing." );

				guiHtmlMessages += "<li>For SQL-Server between 2016 SP1 and up to 2019, Global Trace flag 7412 is needed to view <b>Live-Query-Plans</b> for <b>other sessions</b>. <br>"
				                 + "This Trace Flag is <b>missing</b> so Live-Query-Plans not be visible."
					             + "<ul>"
					             + "   <li>Set Trace Flag: in startup file add <code>-T7412</code> or temporary set it using <code>dbcc traceon(7412, -1)</code>, wich will <b>not</b> survive a restart!</li>"
					             + "</ul>"
				                 + "</li>";
			}
		}

		// GUI Message -- if we are not running in NO-GUI mode
		if (hasGui && StringUtil.hasValue(guiHtmlMessages))
		{
			String dbmsServerName = conn.getDbmsServerNameNoThrow();
			
			String  PROPKEY_showInfo = Version.getAppName() + ".show.info." + dbmsServerName + ".traceFlag_4712_2451_or_LAST_QUERY_PLAN_STATS";
			boolean DEFAULT_showInfo = true;
			
			boolean showInfoPopup = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_showInfo, DEFAULT_showInfo);
			if (showInfoPopup)
			{
				String sqlVersionStr = "-unknown-";
				try { sqlVersionStr = conn.getDbmsVersionStr(); } catch (SQLException ignore) {}
				
				String htmlMsg = "<html>"
						+ "<h2>Some properties in server '" + dbmsServerName + "' might need adjustment!</h2>"
						+ "You just connected to: " + sqlVersionStr + " <br>"
						+ "<br>"
						+ "For optimal experiance and usability the following options may need to be changed: <br>"
						+ "<ul>" + guiHtmlMessages + "</ul>"
						+ "<br>"
						+ Version.getAppName() + " will work without the above changes, but some functionality might not be available."
						+ "</html>"
						;

				Object[] options = {"Connect: and SHOW this msg next time", "Connect: and do NOT show this msg again", "CANCEL: Do not connect"};

				int answer = JOptionPane.showOptionDialog(null, //_window, 
						htmlMsg,
						"DBMS Properties Notice", // title
						JOptionPane.YES_NO_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE,
						null,     //do not use a custom Icon
						options,  //the titles of buttons
						options[0]); //default button title

				if (answer == 0)
				{
					// Do nothing
				}
				// Save "DO NOT SHOW AGAIN"
				if (answer == 1)
				{
					Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
					if (tmpConf != null)
					{
						tmpConf.setProperty(PROPKEY_showInfo, ! DEFAULT_showInfo);
						tmpConf.save();
					}
				}
				if (answer == 2) // DO NOT Connect
				{
					conn.closeNoThrow();
					throw new RuntimeException("Connect attempt was aborted by user.");
				}
			} // end: show info message
		}
	}

	@Override
	public boolean isDbmsOptionEnabled(DbmsOption dbmsOption)
	{
		// call super (which does some checks)... probably always returns false
		super.isDbmsOptionEnabled(dbmsOption);

		if (DbmsOption.SQL_SERVER__LAST_QUERY_PLAN_STATS.equals(dbmsOption))
			return _useLastQueryPlanStats;

		return false;
	}
	
	// used when checking LAST_QUERY_PLAN_STATS is set in any database, or TF 2541 
	private boolean _useLastQueryPlanStats = false;
	
	public static final String PROPKEY_Edition                   = CounterControllerSqlServer.class.getSimpleName() + ".Edition";
	public static final String PROPKEY_VersionStr                = CounterControllerSqlServer.class.getSimpleName() + ".VersionStr";
	public static final String PROPKEY_DisableLastQueryPlanStats = CounterControllerSqlServer.class.getSimpleName() + ".disable.LastQueryPlanStats";

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

	/**
	 * NOTE: This method IS/MUST bee called from both GUI & NO-GUI: CounterCollectorThreadGui AND CounterCollectorThreadNoGui
	 */
	@Override
	public void setInRefresh(boolean enterRefreshMode)
	{
		if (enterRefreshMode)
		{
			// Check if we are configured to do: SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED
			boolean onRefreshSetDirtyReads    = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_onRefresh_setDirtyReads    , DEFAULT_onRefresh_setDirtyReads);
			boolean onRefreshSetLockTimeout   = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_onRefresh_setLockTimeout   , DEFAULT_onRefresh_setLockTimeout);
			int     onRefreshSetLockTimeoutMs = Configuration.getCombinedConfiguration().getIntProperty    (PROPKEY_onRefresh_setLockTimeout_ms, DEFAULT_onRefresh_setLockTimeout_ms);

			//----------------------------------------------------------
			// Many of the collectors will probably be helped if we are in "Dirty Read" mode (so that they are NOT blocked...)
			// So lets set "Dirty Reads" at the session level
			//----------------------------------------------------------
			if (onRefreshSetDirtyReads)
			{
				String getSql = "select transaction_isolation_level from sys.dm_exec_sessions where session_id = @@SPID";
				String setSql = "SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED"; // DIRTY READS
				String sql;

				// get connection
				DbxConnection dbxConn = getMonConnection();
				
				// Make a default value, that we are NOT in "dirty read"
				boolean isDirtyReadsEnabled = false;

				//------- GET/CHECK
				sql = getSql;
				try(Statement stmnt = dbxConn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
				{
					while(rs.next())
						isDirtyReadsEnabled = rs.getInt(1) == 1; // 1==ReadUnCommited, 2=RaedCommitted, 3=RepeatableRead, 4=Serializable, 5=Snapshot 
				}
				catch(SQLException e)
				{
					_logger.error("Problem when CHECKING the SQL-Server 'isolation level' before entering refresh mode. SQL="+sql);
				}
				
				//------- SET DirtyReads
				if ( ! isDirtyReadsEnabled )
				{
					sql = setSql;
					try(Statement stmnt = dbxConn.createStatement())
					{
						_logger.info("SETTING 'isolation level' to 'dirty reads' before entering refresh mode. executing SQL="+sql);
						stmnt.executeUpdate(sql);
					}
					catch(SQLException e)
					{
						_logger.error("Problem when SETTING the SQL-Server isolation level before entering refresh mode. SQL="+sql);
					}
				}
			}

			//----------------------------------------------------------
			// Although if we are getting blocked (if we for instance is using object_name(id, dbid)... which is done in some places...
			// Set maximum time that we can be blocked by issuing: SET LOCK_TIMEOUT ####
			// This will case the following error message:
			//     Msg 1222, Level 16, State 56:
			//     Server 'gorans-ub2', Line 1 (script row 8397)
			//     Lock request time out period exceeded.
			//----------------------------------------------------------
			if (onRefreshSetLockTimeout)
			{
				String getSql = "select @@lock_timeout";
				String setSql = "SET LOCK_TIMEOUT " + onRefreshSetLockTimeoutMs;
				String sql;

				// get connection
				DbxConnection dbxConn = getMonConnection();
				
				// Make a default value, that we are NOT in "dirty read"
				int currentLockTimeout = Integer.MIN_VALUE;

				//------- GET/CHECK
				sql = getSql;
				try(Statement stmnt = dbxConn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
				{
					while(rs.next())
						currentLockTimeout = rs.getInt(1); // <0 = Unlimited, 0=TimeoutImmediately, >0 = Timeout after #### ms
				}
				catch(SQLException e)
				{
					_logger.error("Problem when CHECKING the SQL-Server 'lock timeout' before entering refresh mode. SQL="+sql);
				}
				
				//------- SET LOCK_TIMEOUT
				if ( currentLockTimeout != onRefreshSetLockTimeoutMs )
				{
					sql = setSql;
					try(Statement stmnt = dbxConn.createStatement())
					{
						_logger.info("SETTING 'lock timeout' from " + currentLockTimeout + " to " + onRefreshSetLockTimeoutMs + " before entering refresh mode. executing SQL="+sql);
						stmnt.executeUpdate(sql);
					}
					catch(SQLException e)
					{
						_logger.error("Problem when SETTING the SQL-Server 'lock timeout' before entering refresh mode. SQL="+sql);
					}
				}
			}
		}	

		super.setInRefresh(enterRefreshMode);
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
			if (dir == null)
				dir = osFileName;

			// If not windows: remove drive letter, and fix backslash '\' to forward slash '/'
			if ( dir != null && instance.isLinux() )
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
			String dir = f.getParent();
			if (dir == null)
				dir = osFileName;
			return dir;
		}
	}
	
	
	@Override
	public void doLastRecordingActionBeforeDatabaseRollover(DbxConnection pcsConn)
	{
		int daysToCopy = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_onPcsDatabaseRollover_captureQueryStore_daysToCopy, DEFAULT_onPcsDatabaseRollover_captureQueryStore_daysToCopy);
		
		// Check if Capture Query Store is DISABLED 
		boolean captureQueryStore = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_onPcsDatabaseRollover_captureQueryStore, DEFAULT_onPcsDatabaseRollover_captureQueryStore);
		if ( ! captureQueryStore)
		{
			_logger.info("On PCS Database Rollover: Skipping 'Capture Query Store', due to Configuration '" + PROPKEY_onPcsDatabaseRollover_captureQueryStore + "' is set to FALSE.");
			return;
		}
		
		// Check if Capture Query Store is DISABLED for a specific server 
		String srvName = getMonConnection().getDbmsServerNameNoThrow();
		String captureQueryStoreSkipServer = Configuration.getCombinedConfiguration().getProperty(PROPKEY_onPcsDatabaseRollover_captureQueryStore_skipServer, DEFAULT_onPcsDatabaseRollover_captureQueryStore_skipServer);
		if (StringUtil.hasValue(captureQueryStoreSkipServer) && StringUtil.hasValue(srvName))
		{
			try
			{
				if ( srvName.matches(captureQueryStoreSkipServer) )
				{
					_logger.info("On PCS Database Rollover: Skipping 'Capture Query Store' for Server Name '" + srvName + "', due to Configuration '" + PROPKEY_onPcsDatabaseRollover_captureQueryStore_skipServer + "' is set to '" + captureQueryStoreSkipServer + "'.");
				}
			}
			catch (PatternSyntaxException ex) 
			{
				_logger.error("On PCS Database Rollover: Problems with regular expression '" + captureQueryStoreSkipServer + "' in cofiguration '" + PROPKEY_onPcsDatabaseRollover_captureQueryStore_skipServer + "'. Skipping this and Continuing...", ex);
			}
		}
		
		// Open a new connection to the monitored server
		ConnectionProp connProp = getMonConnection().getConnPropOrDefault();
		DbxConnection conn = null;
		try
		{
			_logger.info("On PCS Database Rollover: Creating a new connection to server '" + srvName+ "' for extracting 'Query Store'.");
			conn = DbxConnection.connect(null, connProp);
		}
		catch (Exception ex)
		{
			_logger.error("On PCS Database Rollover: Failed to establish a new connection to server '" + srvName + "'. SKIPPING 'Capture Query Store'.", ex);
			return;
		}

		// Get databases which has Query Store Enabled
		List<String> enabledDatabases = new ArrayList<>();
		String sql = ""
			    + "declare @dbnames table(dbame nvarchar(128)) \n"
			    + "INSERT INTO @dbnames \n"
			    + "exec sys.sp_MSforeachdb 'select ''?'' from ?.sys.database_query_store_options where desired_state != 0' \n"
			    + "SELECT * FROM @dbnames \n"
			    + "";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while (rs.next())
				enabledDatabases.add(rs.getString(1));
		}
		catch (SQLException ex)
		{
			_logger.error("On PCS Database Rollover: Problems when getting list of databases that has 'Query Store' enabled on '" + srvName + "'. SKIPPING 'Capture Query Store'.", ex);
		}

		// if we have "Query Store enabled" in any databases Extract them
		if (enabledDatabases.isEmpty())
		{
			_logger.info("On PCS Database Rollover: No databases had 'Query Store' enabled on server '" + srvName + "'. Skipping this.");
		}
		else
		{
			_logger.info("On PCS Database Rollover: Extracting 'Query Store' On server '" + srvName+ "' for the following " + enabledDatabases.size() + " database(s): " + enabledDatabases);

			// loop and extract each of the databases
			for (String dbname : enabledDatabases)
			{
				try
				{
					SqlServerQueryStoreExtractor qse = new SqlServerQueryStoreExtractor(dbname, daysToCopy, conn, pcsConn);
					qse.transfer();
				}
				catch (Exception ex)
				{
					_logger.error("On PCS Database Rollover: Problems extracting 'Query Store' from server '" + srvName + "', database '" + dbname + "'.", ex);
				}
			}
		}

		// Close
		if (conn != null)
		{
			_logger.info("On PCS Database Rollover: Closing connection to server '" + srvName + "'..");
			conn.closeNoThrow();
		}
	}
}
