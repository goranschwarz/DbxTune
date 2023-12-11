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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.regex.PatternSyntaxException;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import com.asetune.cache.DbmsObjectIdCache;
import com.asetune.cache.DbmsObjectIdCacheSqlServer;
import com.asetune.cache.SqlAgentJobInfoCache;
import com.asetune.central.cleanup.DataDirectoryCleaner;
import com.asetune.cm.CountersModel;
import com.asetune.cm.os.CmOsDiskSpace;
import com.asetune.cm.os.CmOsIostat;
import com.asetune.cm.os.CmOsMeminfo;
import com.asetune.cm.os.CmOsMpstat;
import com.asetune.cm.os.CmOsNwInfo;
import com.asetune.cm.os.CmOsPs;
import com.asetune.cm.os.CmOsUptime;
import com.asetune.cm.os.CmOsVmstat;
import com.asetune.cm.sqlserver.CmActiveStPlanStats;
import com.asetune.cm.sqlserver.CmActiveStatements;
import com.asetune.cm.sqlserver.CmAlwaysOn;
import com.asetune.cm.sqlserver.CmDatabases;
import com.asetune.cm.sqlserver.CmDbIo;
import com.asetune.cm.sqlserver.CmDeviceIo;
import com.asetune.cm.sqlserver.CmErrorLog;
import com.asetune.cm.sqlserver.CmExecCursors;
import com.asetune.cm.sqlserver.CmExecFunctionStats;
import com.asetune.cm.sqlserver.CmExecProcedureStats;
import com.asetune.cm.sqlserver.CmExecQueryStatPerDb;
import com.asetune.cm.sqlserver.CmExecQueryStats;
import com.asetune.cm.sqlserver.CmExecTriggerStats;
import com.asetune.cm.sqlserver.CmIndexMissing;
import com.asetune.cm.sqlserver.CmIndexOpStat;
import com.asetune.cm.sqlserver.CmIndexPhysical;
import com.asetune.cm.sqlserver.CmIndexUnused;
import com.asetune.cm.sqlserver.CmIndexUsage;
import com.asetune.cm.sqlserver.CmMemoryClerks;
import com.asetune.cm.sqlserver.CmMemoryGrants;
import com.asetune.cm.sqlserver.CmMemoryGrantsSum;
import com.asetune.cm.sqlserver.CmOpenTransactions;
import com.asetune.cm.sqlserver.CmOptimizer;
import com.asetune.cm.sqlserver.CmOsLatchStats;
import com.asetune.cm.sqlserver.CmPerfCounters;
import com.asetune.cm.sqlserver.CmPlanCacheHistory;
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
import com.asetune.cm.sqlserver.CmVersionStore;
import com.asetune.cm.sqlserver.CmWaitStats;
import com.asetune.cm.sqlserver.CmWaitStatsPerDb;
import com.asetune.cm.sqlserver.CmWaitingTasks;
import com.asetune.cm.sqlserver.CmWho;
import com.asetune.cm.sqlserver.CmWhoIsActive;
import com.asetune.cm.sqlserver.CmWorkers;
import com.asetune.cm.sqlserver.ToolTipSupplierSqlServer;
import com.asetune.gui.MainFrame;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.gui.swing.GTable.ITableTooltip;
import com.asetune.pcs.PersistContainer;
import com.asetune.pcs.PersistContainer.HeaderInfo;
import com.asetune.pcs.PersistWriterJdbc;
import com.asetune.pcs.SqlServerQueryStoreDdlExtractor;
import com.asetune.pcs.SqlServerQueryStoreExtractor;
import com.asetune.sql.conn.ConnectionProp;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfoSqlServer;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.AseSqlScript;
import com.asetune.utils.Configuration;
import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.CronUtils;
import com.asetune.utils.SqlServerUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;
import com.asetune.utils.Ver;

import it.sauronsoftware.cron4j.Scheduler;


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


	public static final String  PROPKEY_NoGui_AutoEnable_TraceFlag_LastQueryPlanStats = "SqlServerTune.nogui.autoEnable.traceflag.lastQueryPlanStats";
	public static final boolean DEFAULT_NoGui_AutoEnable_TraceFlag_LastQueryPlanStats = false;
	
	public static final String  PROPKEY_NoGui_AutoEnable_TraceFlag_LiveQueryPlans = "SqlServerTune.nogui.autoEnable.traceflag.liveQueryPlans";
	public static final boolean DEFAULT_NoGui_AutoEnable_TraceFlag_LiveQueryPlans = false;
	
	public static final String  PROPKEY_NoGui_onConnectEnableConnProp_encrypt = "SqlServerTune.nogui.onConnect.enable.conn.prop.encrypt";
	public static final boolean DEFAULT_NoGui_onConnectEnableConnProp_encrypt = true;

	public static final String  PROPKEY_NoGui_onConnectEnableConnProp_trustServerCertificate = "SqlServerTune.nogui.onConnect.enable.conn.prop.trustServerCertificate";
	public static final boolean DEFAULT_NoGui_onConnectEnableConnProp_trustServerCertificate = true;

	public static final String  PROPKEY_onConnect_setNetworkTimeout_ms = "SqlServerTune.onConnect.setNetworkTimeout.ms";
	public static final int     DEFAULT_onConnect_setNetworkTimeout_ms = 60_000;

	
	
	public static final String  PCS_KEY_VALUE_deadlockCountOverRecordingPeriod = "deadlock.count.over.recording.period";
	public static final String  PCS_KEY_VALUE_deadlockReport_blitzLock         = "deadlock.report.blitzLock";
	public static final String  PCS_KEY_VALUE_deadlockReport_xxx               = "deadlock.report.xxx";
	
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

		// Server
		CmSessions           .create(counterController, guiController);
		CmWho                .create(counterController, guiController);
		CmWhoIsActive        .create(counterController, guiController);
		CmSpidWait           .create(counterController, guiController);
//		CmExecSessions       .create(counterController, guiController);
//		CmExecRequests       .create(counterController, guiController);
		CmDatabases          .create(counterController, guiController);
		CmTempdbUsage        .create(counterController, guiController);
		CmTempdbSpidUsage    .create(counterController, guiController);
		CmVersionStore       .create(counterController, guiController);
		CmSchedulers         .create(counterController, guiController);
		CmWaitStats          .create(counterController, guiController);
		CmWaitStatsPerDb     .create(counterController, guiController);
		CmWaitingTasks       .create(counterController, guiController);
		CmMemoryClerks       .create(counterController, guiController);
		CmMemoryGrantsSum    .create(counterController, guiController);
		CmMemoryGrants       .create(counterController, guiController);
		CmErrorLog           .create(counterController, guiController);
		CmOsLatchStats       .create(counterController, guiController);
		CmPerfCounters       .create(counterController, guiController);
		CmWorkers            .create(counterController, guiController);
		CmAlwaysOn           .create(counterController, guiController);
		CmOptimizer          .create(counterController, guiController);
		CmQueryTransformStat .create(counterController, guiController);
		CmSpinlocks          .create(counterController, guiController);
                             
		// Object Access
		CmActiveStatements   .create(counterController, guiController);
		CmActiveStPlanStats  .create(counterController, guiController);
		CmOpenTransactions   .create(counterController, guiController);
		CmIndexUsage         .create(counterController, guiController);
		CmIndexOpStat        .create(counterController, guiController);
		CmTableSize          .create(counterController, guiController);
		CmIndexPhysical      .create(counterController, guiController);
		CmIndexMissing       .create(counterController, guiController);
		CmIndexUnused        .create(counterController, guiController);
		CmPlanCacheHistory   .create(counterController, guiController);
		CmExecQueryStatPerDb .create(counterController, guiController);
		CmExecQueryStats     .create(counterController, guiController);
		CmExecProcedureStats .create(counterController, guiController);
		CmExecFunctionStats  .create(counterController, guiController);
		CmExecTriggerStats   .create(counterController, guiController);
		CmExecCursors        .create(counterController, guiController);
                             
		// Cache
		CmProcedureStats     .create(counterController, guiController);
                             
		// Disk
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
		CmOsPs               .create(counterController, guiController);

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
	public void initCounters(DbxConnection conn, boolean hasGui, long srvVersion, boolean isAzureAnalytics, long monTablesVersion)
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
		
		isAzureAnalytics = isAzureAnalytics();

		// initialize all the CM's
		for (CountersModel cm : getCmList())
		{
			_logger.debug("Initializing CM named '"+cm.getName()+"', display name '"+cm.getDisplayName()+"', using MS SQL-Server version number "+srvVersion+".");

			// set the version
			cm.setServerVersion(monTablesVersion);
			cm.setClusterEnabled(isAzureAnalytics); // use the "cluster" option for "Azure Analytics"

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
			_logger.warn("Problems getting SQL-Server 'Edition', using sql='"+sql+"'. Caught: "+ex);
		}

		//------------------------------------------------
		// Get server ENGINE EDITION  --- https://docs.microsoft.com/en-us/sql/t-sql/functions/serverproperty-transact-sql
		sql = "select ServerProperty('EngineEdition')";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql) )
		{
			while(rs.next())
			{
				setDbmsProperty(PROPKEY_EngineEdition, rs.getString(1));
			}
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems geting SQL-Server 'EngineEdition', using sql='"+sql+"'. Caught: "+ex);
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
		// If we are running some AZURE we can drop out of here!
		// In the future there might be things we want to check
		DbmsVersionInfoSqlServer versionInfo = (DbmsVersionInfoSqlServer) conn.getDbmsVersionInfo();
		if (versionInfo.isAzureDb() || versionInfo.isAzureSynapseAnalytics() || versionInfo.isAzureManagedInstance())
		{
			_logger.info("Skipping checking/initialize DbmsProperties since we connected to some Azure instance and we dont want to check for various traceflags/settings/etc. This since we can't change them in an Azure Managed environment.");
			return;
		}

		// Warn user if it do NOT have proper permissions or proper roles
		// Basically: select * from sys.fn_my_permissions(default,default)
		if (true)
		{
			List<String> activeRoles = conn.getActiveServerRolesOrPermissions();
			if (activeRoles != null)
			{
				boolean hasViewServerState    = activeRoles.contains("VIEW SERVER STATE");
				boolean hasConnectAnyDatabase = activeRoles.contains("CONNECT ANY DATABASE");
			//	boolean hasViewAnyDatabase    = activeRoles.contains("VIEW ANY DATABASE");
				boolean hasViewAnyDefinition  = activeRoles.contains("VIEW ANY DEFINITION");

				boolean missingPermissions = (!hasViewServerState || !hasConnectAnyDatabase || !hasViewAnyDefinition);

				if (hasGui && missingPermissions)
				{
					String dbmsServerName = conn.getDbmsServerNameNoThrow();

					String sqlVersionStr = "-unknown-";
					try { sqlVersionStr = conn.getDbmsVersionStr(); } catch (SQLException ignore) {}

					String username = conn.getConnPropOrDefault() == null ? "-unknown-" : conn.getConnPropOrDefault().getUsername();
					
					String sqlFixCmds = "";
					if ( ! hasViewServerState    ) sqlFixCmds += "GRANT VIEW SERVER STATE    TO [" + username + "];\n";
					if ( ! hasConnectAnyDatabase ) sqlFixCmds += "GRANT CONNECT ANY DATABASE TO [" + username + "];\n";
					if ( ! hasViewAnyDefinition  ) sqlFixCmds += "GRANT VIEW ANY DEFINITION  TO [" + username + "];\n";

					String guiHtmlMessages = "";
					if ( ! hasViewServerState    ) guiHtmlMessages += "<li> VIEW SERVER STATE    </li>";
					if ( ! hasConnectAnyDatabase ) guiHtmlMessages += "<li> CONNECT ANY DATABASE </li>";
					if ( ! hasViewAnyDefinition  ) guiHtmlMessages += "<li> VIEW ANY DEFINITION  </li>";

					String htmlMsg = "<html>"
							+ "<h2>Some permission(s) are missing for user '" + username + "' in server '" + dbmsServerName + "'.</h2>"
							+ "You just connected to <b>Server</b>: " + dbmsServerName + " <br>"
							+ "With <b>Version</b>: " + sqlVersionStr + " <br>"
							+ "As <b>User Name</b>: " + username + " <br>"
							+ "<br>"
							+ "For optimal experiance and usability the following permissions is needed: <br>"
							+ "<ul>" + guiHtmlMessages + "</ul>"
							+ "<br>"
							+ Version.getAppName() + " will work without the above permissions, but some functionality might not be available.<br>"
							+ "<b>Note:</b> The SQL Command(s) to grant permissions has been put in the <i>Copy Paste Buffer</i>, for easy access.<br>"
							+ "</html>"
							;
					Object[] options = {"Connect: and Expect a lot of Errors", "CANCEL: Do not connect"};

					int answer = JOptionPane.showOptionDialog(null, //_window, 
							htmlMsg,
							"DBMS permission(s) Notice", // title
							JOptionPane.YES_NO_CANCEL_OPTION,
							JOptionPane.WARNING_MESSAGE,
							null,     //do not use a custom Icon
							options,  //the titles of buttons
							options[0]); //default button title

					// Copy command to Copy/Paste buffer
					SwingUtils.setClipboardContents(sqlFixCmds);

					if (answer == 0) // Connect
					{
						// Do nothing
					}
					if (answer == 1) // DO NOT Connect
					{
						conn.closeNoThrow();
						throw new RuntimeException("Connect attempt was aborted by user.");
					}
				} // end: hasGui
			}
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
		String sqlFixCmds      = "";
		Map<String, String> noGuiSqlFixCmdList = new LinkedHashMap<>();
		
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
					
					sqlFixCmds = ""
							+ "/*** To enable 'Actual-Query-Plans', you may execute one of the below statements ***/ \n"
							+ "-- dbcc traceon(2451, -1) \n"
							+ "-- use dbname; alter database scoped configuration set LAST_QUERY_PLAN_STATS = ON \n"
							+ "\n";
					
					if (!hasGui)
					{
						if (Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_NoGui_AutoEnable_TraceFlag_LastQueryPlanStats, DEFAULT_NoGui_AutoEnable_TraceFlag_LastQueryPlanStats))
							noGuiSqlFixCmdList.put(PROPKEY_NoGui_AutoEnable_TraceFlag_LastQueryPlanStats, "dbcc traceon(2451, -1)");
						else
							_logger.info("TIP: in no-gui mode you can set property '" + PROPKEY_NoGui_AutoEnable_TraceFlag_LastQueryPlanStats + "=true' to automatically enable 'Actual-Query-Plans'.");
					}
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
			if ( activeGlobalTraceFlagList.contains(7412) )
			{
				_logger.info("For SQL-Server between 2016 SP1 and below 2019, Live-Query-Plans is ENABLED, found Global Trace Flag 7412." );
			}
			else
			{
				_logger.warn("For SQL-Server between 2016 SP1 and below 2019, Trace flag 7412 is needed to view Live-Query-Plans for other sessions. This Trace Flag is MISSING so Live-Query-Plans may be missing." );

				guiHtmlMessages += "<li>For SQL-Server between 2016 SP1 and up to 2019, Global Trace flag 7412 is needed to view <b>Live-Query-Plans</b> for <b>other sessions</b>. <br>"
				                 + "This Trace Flag is <b>missing</b> so Live-Query-Plans not be visible."
					             + "<ul>"
					             + "   <li>Set Trace Flag: in startup file add <code>-T7412</code> or temporary set it using <code>dbcc traceon(7412, -1)</code>, wich will <b>not</b> survive a restart!</li>"
					             + "</ul>"
				                 + "</li>";
				
				sqlFixCmds += ""
						+ "/*** for SQL Server between 2016 SP1 and 2019. To enable 'Live-Query-Plans', you may execute the below statement ***/ \n"
						+ "-- dbcc traceon(7412, -1) \n"
						+ "\n";

				if (!hasGui)
				{
					if (Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_NoGui_AutoEnable_TraceFlag_LiveQueryPlans, DEFAULT_NoGui_AutoEnable_TraceFlag_LiveQueryPlans))
						noGuiSqlFixCmdList.put(PROPKEY_NoGui_AutoEnable_TraceFlag_LiveQueryPlans, "dbcc traceon(7412, -1)");
					else
						_logger.info("TIP: in no-gui mode you can set property '" + PROPKEY_NoGui_AutoEnable_TraceFlag_LastQueryPlanStats + "=true' to automatically enable 'Live-Query-Plans'.");
				}
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
						+ Version.getAppName() + " will work without the above changes, but some functionality might not be available.<br>"
						+ "<b>Note:</b> The above SQL Command(s) has been put in the <i>Copy Paste Buffer</i>, for easy access.<br>"
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

				// Copy command to Copy/Paste buffer
				SwingUtils.setClipboardContents(sqlFixCmds);

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

		// In NOGUI Mode, execute SQL Statements (which was added in above logic) 
		if (!hasGui && !noGuiSqlFixCmdList.isEmpty())
		{
			for (Entry<String, String> entry : noGuiSqlFixCmdList.entrySet())
			{
				String propKey    = entry.getKey();
				String sloganName = "-unknown-";

				sql = entry.getValue();

				if (propKey.equals(PROPKEY_NoGui_AutoEnable_TraceFlag_LastQueryPlanStats)) sloganName = "Actual-Query-Plans";
				if (propKey.equals(PROPKEY_NoGui_AutoEnable_TraceFlag_LiveQueryPlans))     sloganName = "Live-Query-Plans";
				
				try (Statement stmnt = conn.createStatement())
				{
					stmnt.executeUpdate(sql);
					_logger.info("Success setting SQL Server traceflag for '" + sloganName + "'. In NO-GUI mode the property '" + propKey + "' was true. The following SQL Command was issued '" + sql + "'.");
					
					// Set specific internal options...
					if (propKey.equals(PROPKEY_NoGui_AutoEnable_TraceFlag_LastQueryPlanStats))
					{
						_useLastQueryPlanStats = true;
					}
					
				}
				catch(SQLException ex)
				{
					_logger.error("FAILED setting SQL Server traceflag for '" + sloganName + "'. In NO-GUI mode the property '" + propKey + "' was true. The following SQL Command was issued '" + sql + "', but we had problems. Continuing anyway. DBMS Error=" + ex.getErrorCode() + ", SQLState=" + ex.getSQLState()+ ", Caught: " + ex);
				}
				
			}
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
	public static final String PROPKEY_EngineEdition             = CounterControllerSqlServer.class.getSimpleName() + ".EngineEdition";
	public static final String PROPKEY_VersionStr                = CounterControllerSqlServer.class.getSimpleName() + ".VersionStr";
	public static final String PROPKEY_DisableLastQueryPlanStats = CounterControllerSqlServer.class.getSimpleName() + ".disable.LastQueryPlanStats";

	// doc on @@version --- https://docs.microsoft.com/sv-se/sql/t-sql/functions/version-transact-sql-configuration-functions?view=sql-server-ver15&viewFallbackFrom=sqlallproducts-allversions
	// says that "To programmatically determine the engine edition, use SELECT SERVERPROPERTY('EngineEdition'). This query will return '5' for Azure SQL Database and '8' for Azure SQL Managed Instance."
	// 
	// and: https://docs.microsoft.com/en-us/sql/t-sql/functions/serverproperty-transact-sql?view=sql-server-ver15
	// Says that: EngineEdition (integer) will have the following values
	// Database Engine edition of the instance of SQL Server installed on the server.
	//    1 = Personal or Desktop Engine (Not available in SQL Server 2005 (9.x) and later versions.)
	//    2 = Standard (This is returned for Standard, Web, and Business Intelligence.)
	//    3 = Enterprise (This is returned for Evaluation, Developer, and Enterprise editions.)
	//    4 = Express (This is returned for Express, Express with Tools, and Express with Advanced Services)
	//    5 = SQL Database
	//    6 = Microsoft Azure Synapse Analytics
	//    8 = Azure SQL Managed Instance
	//    9 = Azure SQL Edge (This is returned for all editions of Azure SQL Edge)
	//    11 = Azure Synapse serverless SQL pool
	// Base data type: int
	
	
	public boolean isAzureAnalytics()
	{
//		String edition = getDbmsProperty(PROPKEY_Edition, "");
//		
//		return (edition != null && edition.toLowerCase().indexOf("azure") >= 0);

		String engineEditionStr = getDbmsProperty(PROPKEY_EngineEdition, "3");
		int engineEdition = 3;
		try {
			engineEdition = Integer.parseInt(engineEditionStr);
		} catch (NumberFormatException ex) {
			_logger.error("Problems Parsing 'EngineEdition' String (" + engineEditionStr + ") into a number. Asuming this is a NORMAL SQL-Server...");
		}
		
		if (engineEdition == 1 ) return false; //  1 = Personal or Desktop Engine (Not available in SQL Server 2005 (9.x) and later versions.)
		if (engineEdition == 2 ) return false; //  2 = Standard (This is returned for Standard, Web, and Business Intelligence.)
		if (engineEdition == 3 ) return false; //  3 = Enterprise (This is returned for Evaluation, Developer, and Enterprise editions.)
		if (engineEdition == 4 ) return false; //  4 = Express (This is returned for Express, Express with Tools, and Express with Advanced Services)
		if (engineEdition == 5 ) return false; //  5 = SQL Database
		if (engineEdition == 6 ) return true ; //  6 = Microsoft Azure Synapse Analytics
		if (engineEdition == 8 ) return false; //  8 = Azure SQL Managed Instance
		if (engineEdition == 9 ) return false; //  9 = Azure SQL Edge (This is returned for all editions of Azure SQL Edge)
		if (engineEdition == 11) return true ; // 11 = Azure Synapse serverless SQL pool

		return false;
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
		String sql = ""
				+ "select \n"
				+ "     getdate() \n"
				+ "    ,convert(varchar(100),isnull(SERVERPROPERTY('InstanceName'), @@servername)) \n"
				+ "    ,convert(varchar(100),SERVERPROPERTY('MachineName')) \n"
				+ "    ,CountersCleared='2000-01-01 00:00:00' \n"
				+ "";

		try
		{
			if ( ! isMonConnected(true, true) ) // forceConnectionCheck=true, closeConnOnFailure=true
				return null;
				
			DbxConnection conn = getMonConnection();

			// Azure SQL (Database / ManagedInstance and possibly SynapsAnalytics) do NOT support 'SERVERPROPERTY('MachineName')'
			// so lets use the @@servername + __azure ...
			DbmsVersionInfoSqlServer versionInfo = (DbmsVersionInfoSqlServer) conn.getDbmsVersionInfo();
			if (versionInfo.isAzureDb() || versionInfo.isAzureSynapseAnalytics() || versionInfo.isAzureManagedInstance())
			{
				sql = ""
						+ "select \n"
						+ "     getdate() \n"
						+ "    ,convert(varchar(100),isnull(SERVERPROPERTY('InstanceName'), @@servername)) \n"
						+ "    ,convert(varchar(100), @@servername + '__azure' ) \n"
						+ "    ,CountersCleared='2000-01-01 00:00:00' \n"
						+ "";
			}
			
			Statement stmt = conn.createStatement();
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
	public void onMonConnect(DbxConnection conn)
	{
		//------------------------------------------------
		// Set: NetworkTimeout
		try
		{
		//	int newNwTimeout = 60_000;
			int newNwTimeout = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_onConnect_setNetworkTimeout_ms, DEFAULT_onConnect_setNetworkTimeout_ms);
			
			if (newNwTimeout > 0)
			{
				int curNwTimeout = conn.getNetworkTimeout();

				_logger.info("Setting JDBC Connection NetworkTimeout to " + newNwTimeout + " ms. (current value was " + curNwTimeout + ")");
				conn.setNetworkTimeout(Executors.newSingleThreadExecutor(), newNwTimeout);
			}
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems in onMonConnect(): When setting NetworkTimeout. Continuing... Caught: MsgNum=" + ex.getErrorCode() + ": " + ex);
		}

		//------------------------------------------------
		// Set some options
		//   -- set deadlock priority LOW or similar... (SET DEADLOCK_PRIORITY LOW)
		//      if SqlServerTune is involved in a DEADLOCK, then the "other" SPID will have a higher chance to "win"
		//      SET DEADLOCK_PRIORITY { LOW | NORMAL | HIGH | <numeric-priority> | @deadlock_var | @deadlock_intvar }  
		//          <numeric-priority> ::= { -10 | -9 | -8 | ... | 0 | ... | 8 | 9 | 10 }
		//          LOW=-5, NORMAL=9, HIGH=5
		// see: https://docs.microsoft.com/en-us/sql/t-sql/statements/set-deadlock-priority-transact-sql?view=sql-server-ver15
		String sql = "SET DEADLOCK_PRIORITY LOW"; // should we go for -7 or -8 to be even more submissive
		try (Statement stmnt = conn.createStatement() )
		{
			_logger.info("onMonConnect(): Setting SQL-Server session option 'DEADLOCK_PRIORITY' using sql: " + sql);
			stmnt.executeUpdate(sql);
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems in onMonConnect(): When Initializing DBMS SET Properties, using sql='" + sql + "'. Continuing... Caught: MsgNum=" + ex.getErrorCode() + ": " + ex);
		}
	}

	/** override this method to cleanup stuff on disconnect */
	@Override
	public void cleanupMonConnection()
	{
		super.cleanupMonConnection();
		
		if (SqlAgentJobInfoCache.hasInstance())
		{
			SqlAgentJobInfoCache.getInstance().reset();
		}
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

			boolean refreshSqlAgentCache = true;
			if (refreshSqlAgentCache)
			{
				SqlAgentJobInfoCache sqlAgentCache = SqlAgentJobInfoCache.getInstance();
				
				sqlAgentCache.refresh(getMonConnection());
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
		if (StringUtil.isNullOrBlank(osFileName))
			return "";

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
		String srvName = getMonConnection().getDbmsServerNameNoThrow();

		// Query Store
		queryStoreExtractor(pcsConn, srvName);


		// Deadlock Extraction
		deadlockExtractor(pcsConn, srvName);


		//---------------------------------------------------------
		// BEGIN: DbxTune - Specific Extended Event Sessions
		//---------------------------------------------------------
		//>>>> In the future we will possibly have Extended Events that we want to "pull" data from at the -end-of-day- so we can include them in some DSR Section
		//>>>> So this would be a GOOD place to do that!
		//---------------------------------------------------------
		// END: DbxTune - Specific Extended Event Sessions
		//---------------------------------------------------------
	}
	
	//------------------------------------------------------------------
	// Query Store - Extractor
	//------------------------------------------------------------------
	private void queryStoreExtractor(DbxConnection pcsConn, String srvName)
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
			_logger.info("On PCS Database Rollover: Creating a new connection to server '" + srvName + "' for extracting 'Query Store'.");
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
			    + "exec sys.sp_MSforeachdb 'select ''?'' from [?].sys.database_query_store_options where desired_state != 0' \n"
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
			_logger.info("On PCS Database Rollover: Query Store Extractor - Closing connection to server '" + srvName + "'..");
			conn.closeNoThrow();
		}
	}

	//------------------------------------------------------------------
	// Deadlock - Extractor
	//------------------------------------------------------------------
	private void deadlockExtractor(DbxConnection pcsConn, String srvName)
	{
		CountersModel cmSummary = CounterController.getInstance().getCmByName(CmSummary.CM_NAME);
		if (cmSummary != null)
		{
			Object o_deadlockCountOverRecordingPeriod = cmSummary.getClientProperty(CmSummary.PROPKEY_clientProp_deadlockCountOverRecordingPeriod);
			if (o_deadlockCountOverRecordingPeriod != null && o_deadlockCountOverRecordingPeriod instanceof Number)
			{
				int deadlockCountOverRecordingPeriod = ((Number)o_deadlockCountOverRecordingPeriod).intValue();
				
				// deadlock count from the "chart/graph" table
				String sqlGetDeadlockCount = "";
				String dummySql = "select * from [CmSummary_DeadlockCountSum] where 1 = 2";
				ResultSetTableModel dummyRstm = ResultSetTableModel.executeQuery(pcsConn, dummySql, true, "metadata");
				if (dummyRstm.hasColumn("label_0") && dummyRstm.hasColumn("data_0"))
				{
					 // OLD Style: Label in separate column ("label_#")
					sqlGetDeadlockCount = pcsConn.quotifySqlString("select sum([data_0]) from [CmSummary_DeadlockCountSum] where [label_0] = 'Deadlock Count'");
				}
				else
				{
					// NEW Style: ColumnName is Label
					sqlGetDeadlockCount = pcsConn.quotifySqlString("select sum([Deadlock Count]) from [CmSummary_DeadlockCountSum]"); 
				}

				try (Statement stmnt = pcsConn.createStatement(); ResultSet rs = stmnt.executeQuery(sqlGetDeadlockCount))
				{
					int deadlockCount = -1;
					while(rs.next())
						deadlockCount = rs.getInt(1);

					deadlockCountOverRecordingPeriod = deadlockCount;
				}
				catch (SQLException ex)
				{
					_logger.warn("Problems getting Deadlock Count from 'CmSummary_DeadlockCountSum' using SQL=|" + sqlGetDeadlockCount + "|, continuing anyway.", ex);
				}

				// Save Number of deadlocks
				try
				{
					Timestamp sessionStartTime = null;
					String    keyName          = PCS_KEY_VALUE_deadlockCountOverRecordingPeriod;
					PersistWriterJdbc.saveKeyValueStore(pcsConn, sessionStartTime, keyName, deadlockCountOverRecordingPeriod, PersistWriterJdbc.PcsKeyValueStoreType.NUMBER);
				}
				catch (SQLException ex)
				{
					_logger.error("Problems saving 'Deadlock Count Over Recording Period', continuing anyway...", ex);
				}
				
				// Create some kind of report
				if (deadlockCountOverRecordingPeriod > 0)
				{
					_logger.info("On PCS Database Rollover: Found " + deadlockCountOverRecordingPeriod + " DEADLOCKS was found in the period on server '" + srvName + "'. I will try to get more information about those DEADLOCKS using procedure 'sp_blitzLock'.");
					// Get DEADLOCK info using: sp_blitzLock

					// Open a new connection to the monitored server
					ConnectionProp connProp = getMonConnection().getConnPropOrDefault();
					DbxConnection conn = null;
					try
					{
						_logger.info("On PCS Database Rollover: Creating a new connection to server '" + srvName + "' for extracting 'DEADLOCKS'.");
						conn = DbxConnection.connect(null, connProp);
					}
					catch (Exception ex)
					{
						_logger.error("On PCS Database Rollover: Failed to establish a new connection to server '" + srvName + "'. SKIPPING ' Extracting DEADLOCKS'.", ex);
						return;
					}

					boolean tryBlitzLock = true;
					if (tryBlitzLock)
					{
						// sp_blitzLock has 3 ResultSets... How do we store that?
						//
						//  * as a ResultSetTableModel (table or JSON) output
						//       - THEN: we still need a "storage" table for it that holds a "CLOB"
						//
						//  * as REAL tables (like we do in SqlServerQueryStoreExtractor)
						//       - THEN: we still need a "storage schema" like "deadlock:"
						//
						// Find the proc in "any" database. (first: 'master', then all databases in descending database_id order)
						String inDbname  = SqlServerUtils.findProcNameInAnyDatabases(conn, "sp_BlitzLock", "master", true);

						// get Start/end of the "previous day"
						LocalDateTime startTimeLdt = LocalDateTime.now().minusHours(4).withHour(0).withMinute(0).withSecond(0).withNano(0);
//						LocalDateTime endTimeLdt   = startTimeLdt.plusHours(24);

						String startTimeStr = startTimeLdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
//						String endTimeStr   = endTimeLdt  .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

						String sql           = "exec " + inDbname + ".dbo.sp_BlitzLock @StartDate='" + startTimeStr + "'";
//						String sql           = "exec " + inDbname + ".dbo.sp_BlitzLock @StartDate='" + startTimeStr + "', @EndDate='" + endTimeStr + "'";
//						String sql           = "exec " + inDbname + ".dbo.sp_BlitzLock";
						String reportContent = "";

						_logger.info("Extracting DEADLOCK information on '" + srvName + "' by executing SQL=|" + sql + "|.");
						try (AseSqlScript sqlScript = new AseSqlScript(conn, 0))
						{
							//sqlScript.setRsAsAsciiTable(true);
							sqlScript.setRsAsJson(true);

							reportContent = sqlScript.executeSqlStr(sql, true);
						}
						catch (SQLException ex)
						{
							reportContent = "ERROR: ErrorCode=" + ex.getErrorCode() + ", SQLState=" + ex.getSQLState() + ", Msg=|" + ex.getMessage() + "|."; 
							_logger.error("Problems executing '" + sql + "'. (" + reportContent + "), continuing anyway...", ex);
						}
						
						// Save deadlocks report
						try
						{
							_logger.info("Saving 'Deadlock Report - sp_BlitzLock'.");

							Timestamp sessionStartTime = null;
    						String    keyName          = PCS_KEY_VALUE_deadlockReport_blitzLock;
    						PersistWriterJdbc.saveKeyValueStore(pcsConn, sessionStartTime, keyName, reportContent, PersistWriterJdbc.PcsKeyValueStoreType.LONG_STR);
						}
						catch (SQLException ex)
						{
							_logger.error("Problems saving 'Deadlock Report - sp_BlitzLock', continuing anyway...", ex);
						}
					}

					boolean tryXxx = true;
					if (tryXxx)
					{
						// Implement some "other" report
						// Possibly if the above fails...
					}
					
					// Close
					if (conn != null)
					{
						_logger.info("On PCS Database Rollover: DEADLOCK Extractor - Closing connection to server '" + srvName + "'..");
						conn.closeNoThrow();
					}
				}
				else
				{
					_logger.info("On PCS Database Rollover: No DEADLOCKS was found in the period on server '" + srvName + "'. Skipping this.");
				}
			}
			else
			{
				_logger.info("On PCS Database Rollover: Cant find client property '" + CmSummary.PROPKEY_clientProp_deadlockCountOverRecordingPeriod + "' in CmSummary. Skipping DEADLOCK extractions.");
			}
		}
	}


	//==================================================================
	// BEGIN: NO-GUI methods
	//==================================================================
	@Override
	public DbxConnection noGuiConnect(String dbmsUsername, String dbmsPassword, String dbmsServer, String dbmsHostPortStr, String jdbcUrlOptions) throws SQLException, Exception
	{
		// Parse 'jdbcUrlOptions'... check for 'trustServerCertificate=true'
		if (true)
		{
			Map<String, String> jdbcUrlOptionsMap = StringUtil.parseCommaStrToMap(jdbcUrlOptions, "=", ";");

			if ( Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_NoGui_onConnectEnableConnProp_encrypt, DEFAULT_NoGui_onConnectEnableConnProp_encrypt) )
			{
				if ( ! jdbcUrlOptionsMap.containsKey("encrypt") )
				{
					jdbcUrlOptionsMap.put("encrypt", "true");
					_logger.info("onConnect: SQL Server URL Options, could not find 'encrypt', adding option: encrypt=true");
				}
			}

			if ( Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_NoGui_onConnectEnableConnProp_trustServerCertificate, DEFAULT_NoGui_onConnectEnableConnProp_trustServerCertificate) )
			{
				if ( ! jdbcUrlOptionsMap.containsKey("trustServerCertificate") )
				{
					jdbcUrlOptionsMap.put("trustServerCertificate", "true");
					_logger.info("onConnect: SQL Server URL Options, could not find 'trustServerCertificate', adding option: trustServerCertificate=true");
				}
			}

			jdbcUrlOptions = StringUtil.toCommaStr(jdbcUrlOptionsMap, "=", ";");
			if (StringUtil.hasValue(jdbcUrlOptions))
				_logger.info("onConnect: This SQL Server URL Options will be used: " + jdbcUrlOptions);
		}

		// Let's connect
		DbxConnection conn = super.noGuiConnect(dbmsUsername, dbmsPassword, dbmsServer, dbmsHostPortStr, jdbcUrlOptions);
		
		// DBMS ObjectID --> ObjectName Cache... maybe it's not the perfect place to initialize this...
		DbmsObjectIdCache.setInstance( new DbmsObjectIdCacheSqlServer( new ConnectionProvider()
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
		}) );

		
		// Populate Object ID Cache
		if (DbmsObjectIdCache.hasInstance() && DbmsObjectIdCache.getInstance().isBulkLoadOnStartEnabled())
			DbmsObjectIdCache.getInstance().getBulk(null); // null == ALL Databases
		else
			_logger.info("Skipping BULK load of ObjectId's at noGuiConnect(), isBulkLoadOnStartEnabled() was NOT enabled. Property '" + DbmsObjectIdCacheSqlServer.PROPKEY_BulkLoadOnStart + "=true|false'.");
		
		// Return the connection
		return conn;
	}

	@Override
	public void noGuiConnectErrorHandler(SQLException ex, String dbmsUsername, String dbmsPassword, String dbmsServer, String dbmsHostPortStr, String jdbcUrlOptions) 
	throws Exception
	{
		String msg    = ex.getMessage();
		int errorCode = ex.getErrorCode();

		//----------------------------------------------------
		// RETRYABLE ERRORS -- The following messages we can do login RETRY on
		//----------------------------------------------------
		// * Msg=18401 = Login failed for user 'dbxtune'. Reason: Server is in script upgrade mode. Only administrator can connect at this time. ClientConnectionId:cc3d9fdd-7920-40e1-a234-70041b0a81ef
		// * Msg=6005  = SHUTDOWN is in progress
		if (   errorCode == 18401 || msg.contains("Reason: Server is in script upgrade mode") 
		    || errorCode ==  6005 || msg.contains("SHUTDOWN is in progress")
		   )
		{
			_logger.info("Received SQL Server Error=" + ex.getErrorCode() + ", Message='" + msg + "', this is a RETRYABLE message. So lets retry at next connect attempt.");
			return;
		}
		
		//----------------------------------------------------
		// UNRECOVERABLE ERRORS
		//----------------------------------------------------
		// Error checking for "invalid password" or other "unrecoverable errors"
		//
		// 1> select * from sys.messages where text like '%Login failed for user%'
		// +----------+-----------+--------+---------------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------+
		// |message_id|language_id|severity|is_event_logged|text                                                                                                                                                                 |
		// +----------+-----------+--------+---------------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------+
		// |    18 401|      1 033|      14|true           |Login failed for user '%.*ls'. Reason: Server is in script upgrade mode. Only administrator can connect at this time.%.*ls                                           |
		// |    18 451|      1 033|      14|true           |Login failed for user '%.*ls'. Only administrators may connect at this time.%.*ls                                                                                    |
		// |    18 456|      1 033|      14|true           |Login failed for user '%.*ls'.%.*ls%.*ls                                                                                                                             |
		// |    18 461|      1 033|      14|true           |Login failed for user '%.*ls'. Reason: Server is in single user mode. Only one administrator can connect at this time.%.*ls                                          |
		// |    18 462|      1 033|      14|false          |The login failed for user "%.*ls". The password change failed. The password for the user is too recent to change. %.*ls                                              |
		// |    18 463|      1 033|      14|false          |The login failed for user "%.*ls". The password change failed. The password cannot be used at this time. %.*ls                                                       |
		// |    18 464|      1 033|      14|false          |Login failed for user '%.*ls'. Reason: Password change failed. The password does not meet operating system policy requirements because it is too short.%.*ls         |
		// |    18 465|      1 033|      14|false          |Login failed for user '%.*ls'. Reason: Password change failed. The password does not meet operating system policy requirements because it is too long.%.*ls          |
		// |    18 466|      1 033|      14|false          |Login failed for user '%.*ls'. Reason: Password change failed. The password does not meet operating system policy requirements because it is not complex enough.%.*ls|
		// |    18 467|      1 033|      14|false          |The login failed for user "%.*ls". The password change failed. The password does not meet the requirements of the password filter DLL. %.*ls                         |
		// |    18 468|      1 033|      14|false          |The login failed for user "%.*ls". The password change failed. An unexpected error occurred during password validation. %.*ls                                        |
		// |    18 470|      1 033|      14|true           |Login failed for user '%.*ls'. Reason: The account is disabled.%.*ls                                                                                                 |
		// |    18 471|      1 033|      14|false          |The login failed for user "%.*ls". The password change failed. The user does not have permission to change the password. %.*ls                                       |
		// |    18 486|      1 033|      14|true           |Login failed for user '%.*ls' because the account is currently locked out. The system administrator can unlock it. %.*ls                                             |
		// |    18 487|      1 033|      14|true           |Login failed for user '%.*ls'.  Reason: The password of the account has expired.%.*ls                                                                                |
		// |    18 488|      1 033|      14|true           |Login failed for user '%.*ls'.  Reason: The password of the account must be changed.%.*ls                                                                            |
		// |    40 620|      1 033|      16|false          |The login failed for user "%.*ls". The password change failed. Password change during login is not supported in this version of SQL Server.                          |
		// |    40 697|      1 033|      16|false          |Login failed for user '%.*ls'.                                                                                                                                       |
		// +----------+-----------+--------+---------------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------+
		
		if (msg.contains("Login failed for user"))
		{
			throw new Exception("The error message suggest that the wrong USER '" + dbmsUsername + "' or PASSWORD '" + dbmsPassword + "' to DBMS server '" + dbmsServer + "' was entered. This is a non-recovarable error. DBMS Error Message='" + ex.getMessage() + "'.", ex);
		}
	}


	@Override
	public Scheduler createScheduler(boolean hasGui)
	{
		if (hasGui)
			return null;

		Scheduler scheduler = null;

		//--------------------------------------------
		// Get top ## SQL Statements, extract Table Names and send those for DDL Lookup/Storage
		//--------------------------------------------
		boolean queryStoreDdlExtractionStart = Configuration.getCombinedConfiguration().getBooleanProperty(DataDirectoryCleaner.PROPKEY_start, DataDirectoryCleaner.DEFAULT_start);
		if (queryStoreDdlExtractionStart)
		{
			if (scheduler == null)
				scheduler = new Scheduler();

			String cron  = Configuration.getCombinedConfiguration().getProperty(SqlServerQueryStoreDdlExtractor.PROPKEY_cron,  SqlServerQueryStoreDdlExtractor.DEFAULT_cron);
			_logger.info("Adding 'Query Store Extract Table Names from Top SQL Statements' scheduling with cron entry '" + cron + "', human readable '" + CronUtils.getCronExpressionDescription(cron) + "'.");
			scheduler.schedule(cron, new SqlServerQueryStoreDdlExtractor());
		}

		return scheduler;
	}
}
