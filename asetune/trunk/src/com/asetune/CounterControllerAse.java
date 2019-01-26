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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import com.asetune.cache.XmlPlanCache;
import com.asetune.cache.XmlPlanCacheAse;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.CmActiveObjects;
import com.asetune.cm.ase.CmActiveStatements;
import com.asetune.cm.ase.CmBlocking;
import com.asetune.cm.ase.CmCachePools;
import com.asetune.cm.ase.CmCachedObjects;
import com.asetune.cm.ase.CmCachedProcs;
import com.asetune.cm.ase.CmCachedProcsSum;
import com.asetune.cm.ase.CmDataCaches;
import com.asetune.cm.ase.CmDeadlock;
import com.asetune.cm.ase.CmDeviceIo;
import com.asetune.cm.ase.CmDeviceSegIO;
import com.asetune.cm.ase.CmDeviceSegUsage;
import com.asetune.cm.ase.CmEngines;
import com.asetune.cm.ase.CmErrorLog;
import com.asetune.cm.ase.CmExecutionTime;
import com.asetune.cm.ase.CmIoControllers;
import com.asetune.cm.ase.CmIoQueue;
import com.asetune.cm.ase.CmIoQueueSum;
import com.asetune.cm.ase.CmLockTimeout;
import com.asetune.cm.ase.CmLocks;
import com.asetune.cm.ase.CmMemoryUsage;
import com.asetune.cm.ase.CmMissingStats;
import com.asetune.cm.ase.CmObjectActivity;
import com.asetune.cm.ase.CmOpenDatabases;
import com.asetune.cm.ase.CmPCacheMemoryUsage;
import com.asetune.cm.ase.CmPCacheModuleUsage;
import com.asetune.cm.ase.CmProcCacheLoad;
import com.asetune.cm.ase.CmProcCallStack;
import com.asetune.cm.ase.CmProcessActivity;
import com.asetune.cm.ase.CmQpMetrics;
import com.asetune.cm.ase.CmRaLogActivity;
import com.asetune.cm.ase.CmRaMemoryStat;
import com.asetune.cm.ase.CmRaScanners;
import com.asetune.cm.ase.CmRaScannersTime;
import com.asetune.cm.ase.CmRaSenders;
import com.asetune.cm.ase.CmRaStreamStats;
import com.asetune.cm.ase.CmRaSyncTaskStats;
import com.asetune.cm.ase.CmRaSysmon;
import com.asetune.cm.ase.CmRaTruncPoint;
import com.asetune.cm.ase.CmSpMonitorConfig;
import com.asetune.cm.ase.CmSpidCpuWait;
import com.asetune.cm.ase.CmSpidWait;
import com.asetune.cm.ase.CmSpinlockActivity;
import com.asetune.cm.ase.CmSpinlockSum;
import com.asetune.cm.ase.CmSqlStatement;
import com.asetune.cm.ase.CmStatementCache;
import com.asetune.cm.ase.CmStmntCacheDetails;
import com.asetune.cm.ase.CmSummary;
import com.asetune.cm.ase.CmSysLoad;
import com.asetune.cm.ase.CmSysWaits;
import com.asetune.cm.ase.CmSysmon;
import com.asetune.cm.ase.CmTableCompression;
import com.asetune.cm.ase.CmTableStatistics;
import com.asetune.cm.ase.CmTempdbActivity;
import com.asetune.cm.ase.CmThreads;
import com.asetune.cm.ase.CmThresholdEvent;
import com.asetune.cm.ase.CmTransactions;
import com.asetune.cm.ase.CmWorkQueues;
import com.asetune.cm.ase.CmWorkerThread;
import com.asetune.cm.ase.ToolTipSupplierAse;
import com.asetune.cm.os.CmOsDiskSpace;
import com.asetune.cm.os.CmOsIostat;
import com.asetune.cm.os.CmOsMeminfo;
import com.asetune.cm.os.CmOsMpstat;
import com.asetune.cm.os.CmOsNwInfo;
import com.asetune.cm.os.CmOsUptime;
import com.asetune.cm.os.CmOsVmstat;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.gui.MainFrame;
import com.asetune.gui.swing.GTable.ITableTooltip;
import com.asetune.pcs.PersistContainer;
import com.asetune.pcs.PersistContainer.HeaderInfo;
import com.asetune.sql.conn.ConnectionProp;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.TdsConnection;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;
import com.sybase.jdbcx.SybMessageHandler;

public class CounterControllerAse extends CounterControllerAbstract
{
	/** Log4j logging. */
	private static Logger _logger = Logger.getLogger(CounterControllerAse.class);

	/** is sp_configure 'capture missing statistics' set or not */
	protected boolean _config_captureMissingStatistics = false;

	/** is sp_configure 'enable metrics capture' set or not */
	protected boolean _config_enableMetricsCapture = false;
	
	/** is sp_configure 'kernel mode' set to 'process' or 'threaded'. will be false for lower versions of ASE than 15.7, and true if ASE 15.7 and sp_configure 'kernel mode' returns 'threaded' */
	protected boolean _config_threadedKernelMode = false;
	
	/** A list of roles which the connected user has */
	protected List<String> _activeRoleList = null;

	protected String _savedAseServerName = null;
	protected String _savedAseHostName   = null;


	public static final String TRANLOG_DISK_IO_TOOLTIP =
		  "Below is a table that describes how a fast or slow disk affects number of transactions per second.<br>" +
		  "The below description tries to exemplify number of transactions per seconds based on Disk IO responsiveness on the <b>LOG</b> Device.<br>" +
		  
		  "<TABLE ALIGN='left' BORDER=1 CELLSPACING=0 CELLPADDING=0 WIDTH='100%'> " +
		  "  <TR ALIGN='left' VALIGN='left'> <TH>Device Type      </TH> <TH>Typical IOPS </TH> <TH>Typical ms/IO</TH> <TH>Xactn/Sec      </TH> </TR> " +
		  "  <!--                                    -----------------          -------------          -------------          --------------- -->" +
		  "  <TR ALIGN='left' VALIGN='left'> <TD>RAID 6           </TD> <TD>600          </TD> <TD>10-25        </TD> <TD>40-100         </TD> </TR> " +
		  "  <TR ALIGN='left' VALIGN='left'> <TD>RAID 5           </TD> <TD>800          </TD> <TD>2-6          </TD> <TD>166-500        </TD> </TR> " +
		  "  <TR ALIGN='left' VALIGN='left'> <TD>RAID 0 (RAID 10) </TD> <TD>1000         </TD> <TD>1-2          </TD> <TD>500-1000       </TD> </TR> " +
		  "  <TR ALIGN='left' VALIGN='left'> <TD>FC SCSI (tier 1) </TD> <TD>200-400      </TD> <TD>2-6          </TD> <TD>166-500        </TD> </TR> " +
		  "  <TR ALIGN='left' VALIGN='left'> <TD>SAS SCSI (tier 2)</TD> <TD>150-200      </TD> <TD>4-8          </TD> <TD>125-250        </TD> </TR> " +
		  "  <TR ALIGN='left' VALIGN='left'> <TD>SATA (tier 3)    </TD> <TD>100-150      </TD> <TD>6-10         </TD> <TD>100-166        </TD> </TR> " +
		  "  <TR ALIGN='left' VALIGN='left'> <TD>SATA/SAS SSD     </TD> <TD>10000-20000  </TD> <TD>0.05-0.1     </TD> <TD>10,000-20,000  </TD> </TR> " +
		  "  <TR ALIGN='left' VALIGN='left'> <TD>PCIe SSD         </TD> <TD>100000-200000</TD> <TD>0.01-0.005   </TD> <TD>100,000-200,000</TD> </TR> " +
		  "</TABLE> " +
		  "The above table was found in the document: <br> " +
		  "http://www.sybase.com/detail?id=1091630<br>" +
		  "http://www.sybase.com/files/White_Papers/Managing-DBMS-Workloads-v1.0-WP.pdf<br>";


	/** This is a input to the SplashScreen */
	public static final int	   NUMBER_OF_PERFORMANCE_COUNTERS	= 54 + 10 + 20; 
	// 10 extra steps not for performance counters 
	// 20 extra for init time of XX seconds or so

	
	/**
	 * The default constructor
	 * @param hasGui should we create a GUI or NoGUI collection thread
	 */
	public CounterControllerAse(boolean hasGui)
	{
		super(hasGui);
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

		// is sp_configure 'capture missing statistics' set or not
		_config_captureMissingStatistics = false;

		// is sp_configure 'enable metrics capture' set or not
		_config_enableMetricsCapture = false;
		
		// is sp_configure 'kernel mode' is set to 'threaded'
		_config_threadedKernelMode = false;
		
		// A list of roles which the connected user has
		_activeRoleList = null;
		
		_savedAseServerName = null;
		_savedAseHostName   = null;

	}

	/**
	 * When we have a database connection, lets do some extra init this
	 * so all the CountersModel can decide what SQL to use. <br>
	 * SQL statement usually depends on what ASE Server version we monitor.
	 * 
	 * @param conn
	 * @param hasGui              is this initialized with a GUI?
	 * @param srvVersion          what is the ASE Executable version
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
		
		_logger.info("Initializing all CM objects, using ASE server version number "+srvVersion+" ("+Ver.versionNumToStr(srvVersion)+"), isClusterEnabled="+isClusterEnabled+" with monTables Install version "+monTablesVersion+" ("+Ver.versionNumToStr(monTablesVersion)+").");

		// Get active ASE Roles
		//List<String> activeRoleList = AseConnectionUtils.getActiveRoles(conn);
		_activeRoleList = AseConnectionUtils.getActiveSystemRoles(conn);
		
		// Get active Monitor Configuration
		Map<String,Integer> monitorConfigMap = AseConnectionUtils.getMonitorConfigs(conn);

		// Get some specific configurations
		if (srvVersion >= Ver.ver(15,0,3,1))
			_config_captureMissingStatistics = AseConnectionUtils.getAseConfigRunValueBooleanNoEx(conn, "capture missing statistics");

		if (srvVersion >= Ver.ver(15,0,2))
			_config_enableMetricsCapture = AseConnectionUtils.getAseConfigRunValueBooleanNoEx(conn, "enable metrics capture");

		_config_threadedKernelMode = false;
		if (srvVersion >= Ver.ver(15,7))
		{
			String kernelMode = AseConnectionUtils.getAseConfigRunValueStrNoEx(conn, "kernel mode");
			_config_threadedKernelMode = "threaded".equals(kernelMode);
		}

		
		// in version 15.0.3.1 compatibility_mode was introduced, this to use 12.5.4 optimizer & exec engine
		// This will hurt performance, especially when querying sysmonitors table, so set this to off
		if (srvVersion >= Ver.ver(15,0,3,1))
			AseConnectionUtils.setCompatibilityMode(conn, false);

		// initialize all the CM's
		for (CountersModel cm : getCmList())
		{
			_logger.debug("Initializing CM named '"+cm.getName()+"', display name '"+cm.getDisplayName()+"', using ASE server version number "+srvVersion+".");

			// set the version
			cm.setServerVersion(monTablesVersion);
			cm.setClusterEnabled(isClusterEnabled);
			
			// set the active roles, so it can be used in initSql()
			cm.setActiveServerRolesOrPermissions(_activeRoleList);

			// set the ASE Monitor Configuration, so it can be used in initSql() and elsewhere
			cm.setMonitorConfigs(monitorConfigMap);

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

		// Make some specific Cluster settings
		if (isClusterEnabled)
		{
			int systemView = AseConnectionUtils.CE_SYSTEM_VIEW_UNKNOWN;

			if (DbxTune.hasGui())
			{
				systemView = CounterController.getSummaryPanel().getClusterView();
			}
			else
			{
				String clusterView = "cluster";

				// hmmm would this be Configuration.PCS or Configuration.CONF, lets try PCS
				Configuration conf = Configuration.getInstance(Configuration.PCS);
				if (conf != null)
					clusterView = conf.getProperty("cluster.system_view", clusterView);

				if (clusterView.equalsIgnoreCase("instance"))
					systemView = AseConnectionUtils.CE_SYSTEM_VIEW_INSTANCE;
				else
					systemView = AseConnectionUtils.CE_SYSTEM_VIEW_CLUSTER;
			}

			// make the setting
			AseConnectionUtils.setClusterEditionSystemView(conn, systemView);
		}
			
		setInitialized(true);
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
                           
		CmSummary          .create(counterController, guiController);
                           
		// tab: Server
		CmProcessActivity  .create(counterController, guiController);
		CmSpidWait         .create(counterController, guiController);
		CmSpidCpuWait      .create(counterController, guiController);
		CmOpenDatabases    .create(counterController, guiController);
		CmTempdbActivity   .create(counterController, guiController);
		CmSysWaits         .create(counterController, guiController);
		CmExecutionTime    .create(counterController, guiController);
		CmEngines          .create(counterController, guiController);
		CmThreads          .create(counterController, guiController);
		CmSysLoad          .create(counterController, guiController);
		CmSpinlockActivity .create(counterController, guiController);
		CmSpinlockSum      .create(counterController, guiController);
		CmSysmon           .create(counterController, guiController);
		CmWorkerThread     .create(counterController, guiController);
		CmMemoryUsage      .create(counterController, guiController);
		CmErrorLog         .create(counterController, guiController);
		CmDeadlock         .create(counterController, guiController);
		CmLockTimeout      .create(counterController, guiController);
		CmThresholdEvent   .create(counterController, guiController);
		CmSpMonitorConfig  .create(counterController, guiController);
		CmWorkQueues       .create(counterController, guiController);

		// tab: Object/Access
		CmActiveStatements .create(counterController, guiController);
		CmObjectActivity   .create(counterController, guiController);
		CmSqlStatement     .create(counterController, guiController);
		CmActiveObjects    .create(counterController, guiController);
		CmProcCallStack    .create(counterController, guiController);
		CmLocks            .create(counterController, guiController);
		CmBlocking         .create(counterController, guiController);
		CmTransactions     .create(counterController, guiController);
		CmTableCompression .create(counterController, guiController);
		CmTableStatistics  .create(counterController, guiController);
		CmMissingStats     .create(counterController, guiController);
		CmQpMetrics        .create(counterController, guiController);

		// tab: Cache
		CmDataCaches       .create(counterController, guiController);
		CmCachePools       .create(counterController, guiController);
		CmCachedProcs      .create(counterController, guiController);
		CmCachedProcsSum   .create(counterController, guiController);
		CmProcCacheLoad    .create(counterController, guiController);
		CmCachedObjects    .create(counterController, guiController);
		CmPCacheModuleUsage.create(counterController, guiController);
		CmPCacheMemoryUsage.create(counterController, guiController);
		CmStatementCache   .create(counterController, guiController);
		CmStmntCacheDetails.create(counterController, guiController);

		// tab: Disk
		CmDeviceIo         .create(counterController, guiController);
		CmIoQueueSum       .create(counterController, guiController);
		CmIoQueue          .create(counterController, guiController);
		CmIoControllers    .create(counterController, guiController);
		CmDeviceSegIO      .create(counterController, guiController);
		CmDeviceSegUsage   .create(counterController, guiController);

		// tab: RepAgent
		CmRaSenders        .create(counterController, guiController);
		CmRaLogActivity    .create(counterController, guiController);
		CmRaScanners       .create(counterController, guiController);
		CmRaScannersTime   .create(counterController, guiController);
//		CmRaSqlActivity    .create(counterController, guiController);
//		CmRaSqlMisses      .create(counterController, guiController);
		CmRaStreamStats    .create(counterController, guiController);
		CmRaSyncTaskStats  .create(counterController, guiController);
		CmRaTruncPoint     .create(counterController, guiController);
		CmRaMemoryStat     .create(counterController, guiController);
		CmRaSysmon         .create(counterController, guiController);

		// OS HOST Monitoring
		CmOsIostat         .create(counterController, guiController);
		CmOsVmstat         .create(counterController, guiController);
		CmOsMpstat         .create(counterController, guiController);
		CmOsUptime         .create(counterController, guiController);
		CmOsMeminfo        .create(counterController, guiController);
		CmOsNwInfo         .create(counterController, guiController);
		CmOsDiskSpace      .create(counterController, guiController);

		// USER DEFINED COUNTERS
		createUserDefinedCounterModels(counterController, guiController);
		createUserDefinedCounterModelHostMonitors(counterController, guiController);

		// done
		setCountersIsCreated(true);
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
		if (_activeRoleList != null && _activeRoleList.contains(AseConnectionUtils.MON_ROLE))
		{
			sql = "select getdate(), @@servername, @@servername, CountersCleared from master..monState";
			// If version is above 15.0.2 and you have 'sa_role' 
			// then: use ASE function asehostname() to get on which OSHOST the ASE is running
//			if (MonTablesDictionary.getInstance().getAseExecutableVersionNum() >= 15020)
//			if (MonTablesDictionary.getInstance().getAseExecutableVersionNum() >= 1502000)
			if (MonTablesDictionaryManager.getInstance().getDbmsExecutableVersionNum() >= Ver.ver(15,0,2))
			{
				if (_activeRoleList != null && _activeRoleList.contains(AseConnectionUtils.SA_ROLE))
					sql = "select getdate(), @@servername, asehostname(), CountersCleared from master..monState";
			}
		}

		
		try
		{
			if ( ! isMonConnected(true, true) ) // forceConnectionCheck=true, closeConnOnFailure=true
//				continue; // goto: while (_running)
				return null;
			
			// Install: message handler
			//
			// 1> select getdate(), @@servername, asehostname(), CountersCleared from master..monState
			//
			// Msg 937, Level 14, State 1:
			// Server 'SYS_ASE', Line 1 (script row 3052), Status 0, TranState 1:
			// Database 'xxx' is unavailable. It is undergoing LOAD DATABASE.
			//
			// To workaround the above error, when a ASE is undergoing 'LOAD DATABASE', install a MsgHandler
			//
			((TdsConnection)getMonConnection()).setSybMessageHandler(new SybMessageHandler()
			{
				@Override
				public SQLException messageHandler(SQLException sqle)
				{
					int    code   = sqle.getErrorCode();
					String msgStr = sqle.getMessage();
					
					// Remove any newlines etc...
					if (msgStr != null)
						msgStr = msgStr.trim();
					
					// Msg 937: Database 'PML' is unavailable. It is undergoing LOAD DATABASE.
					if (code == 937)
					{
						_logger.debug("createPcsHeaderInfo(): Discarding Msg "+code+", Str '"+msgStr+"'.");
						
						return null; // do NOT threat this as an error... An exception will NOT be thrown to the caller
					}
					
					return sqle;
				}
			});

			
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
		//	stmt.close();

			_savedAseServerName = aseServerName;
			_savedAseHostName   = aseHostname;

			
			// CHECK IF ASE is in SHUTDOWN mode...
			boolean aseInShutdown = false;
			SQLWarning w = stmt.getWarnings();
			while(w != null)
			{
				// Msg=6002: A SHUTDOWN command is already in progress. Please log off.
				if (w.getErrorCode() == 6002)
				{
					aseInShutdown = true;
					break;
				}
				
				w = w.getNextWarning();
			}
			if (aseInShutdown)
			{
				String msgShort = "ASE in SHUTDOWN mode...";
				String msgLong  = "The ASE Server is waiting for a SHUTDOWN, data collection is put on hold...";

				_logger.info(msgLong);
				MainFrame.getInstance().setStatus(MainFrame.ST_STATUS_FIELD, msgLong);
//				SummaryPanel.getInstance().setWatermarkText(msgShort);
				CounterController.getSummaryPanel().setWatermarkText(msgShort);

				for (CountersModel cm : getCmList())
				{
					if (cm == null)
						continue;

					cm.setState(CountersModel.State.SRV_IN_SHUTDOWN);
				}

//				continue; // goto: while (_running)
				return null;
			}
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
			aseServerName    = _savedAseServerName;
			aseHostname      = _savedAseHostName;
			counterClearTime = new Timestamp(0);
			
			if (StringUtil.isNullOrBlank(aseServerName)) aseServerName = "unknown";
			if (StringUtil.isNullOrBlank(aseHostname))   aseHostname   = "unknown";
		}
		finally
		{
			((TdsConnection)getMonConnection()).restoreSybMessageHandler();			
		}
		
		return new HeaderInfo(mainSampleTime, aseServerName, aseHostname, counterClearTime);
	}


	@Override
	public void checkServerSpecifics()
	{
		//----------------------
		// In some versions we need to check if the transaction log is full to some reasons
		// If it is full it will be truncated.
		//----------------------
		checkForFullTransLogInMaster(getMonConnection());
	}



	/**
	 *
	 * In some versions we need to do some checks before we refresh the counters
	 * <ul>
	 * <li> 15.5, 15.0.3 ESD#1, 15.0.3.ESD#2, 15.0.3.ESD#3<br>
	 *   if: sp_configure 'capture missing statistics' is true, then the sysstatistics table in
	 *       the master database will be updated on mon* tables, so the transaction log might get full<br>
	 *   <br>
	 *   CR# 581760:<br>
	 *   "capture missing statistics" will capture statistics for MDA Tables<br>
	 *   http://www-dse/cgi-bin/websql/websql.dir/QTS/bugsheet.hts?GO=GO&bugid=581760<br>
	 * </li>
	 * </ul>
	 * 
	 * @param conn a Connection to the database
	 */
	public void checkForFullTransLogInMaster(Connection conn)
	{
		if (conn == null)
			return;

		//----------------------
		// In some versions we need to do some checks before we refresh the counters
		// - 15.0.3 ESD#1, 15.0.3.ESD#2, 15.0.3.ESD#3 (fix is estimated for the 'bharani' release)
		//   if: sp_configure 'capture missing statistics' is true, then the sysstatistics table in
		//       the master database will be updated on mon* tables, so the transaction log might get full
		// CR# 581760
		// "capture missing statistics" will capture statistics for MDA Tables
		// http://www-dse/cgi-bin/websql/websql.dir/QTS/bugsheet.hts?GO=GO&bugid=581760
		//
		// Change the version check when the bug is fixed. In 16.0 it's not fixed.
		//----------------------
		try
		{
			long srvVersion = 0;
			if (MonTablesDictionaryManager.hasInstance())
				srvVersion = MonTablesDictionaryManager.getInstance().getDbmsExecutableVersionNum();

			if ( srvVersion >= Ver.ver(15,0,3,1) && srvVersion < Ver.ver(17,0) )
			{
				if (_config_captureMissingStatistics || _config_enableMetricsCapture)
				{
					_logger.debug("Checking for full transaction log in the master database.");
	
//					String sql = "select 'isLoggFull' = lct_admin('logfull', db_id('master'))," +
//					             "       'spaceLeft'  = lct_admin('logsegment_freepages', db_id('master')) " +      // Describes the free space available for a database
//					             "                    - lct_admin('reserve', 0) " +                                 // Returns the current last-chance threshold of the transaction log in the database from which the command was issued
//					             "                    - lct_admin('reserved_for_rollbacks', db_id('master'), 0) " + // Determines the number of pages reserved for rollbacks in the master database
//					             "                    - @@thresh_hysteresis";                                       // Take away hysteresis
					String sql = "select 'isLoggFull' = lct_admin('logfull', db_id('master'))," +
					             "       'spaceLeft'  = CASE WHEN lct_admin('logfull', db_id('master')) >= 1 " +
					             "                           THEN 0 " +
					             "                           ELSE   lct_admin('logsegment_freepages', db_id('master')) " +      // Describes the free space available for a database
					             "                                - lct_admin('reserve', 0) " +                                 // Returns the current last-chance threshold of the transaction log in the database from which the command was issued
					             "                                - lct_admin('reserved_for_rollbacks', db_id('master'), 0) " + // Determines the number of pages reserved for rollbacks in the master database
					             "                                - @@thresh_hysteresis" +                                      // Take away hysteresis
					             "                      END";
	
					int isLogFull = 0;
					int spaceLeft = 0;
	
					Statement stmt = conn.createStatement();
					ResultSet rs = stmt.executeQuery(sql);
					while (rs.next())
					{
						isLogFull   = rs.getInt(1);
						spaceLeft   = rs.getInt(2);
					}
					rs.close();
					stmt.close();
	
					_logger.debug("Checking for full transaction log in the master database. Results: isLogFull='"+isLogFull+"', spaceLeft='"+spaceLeft+"'.");
	
					if (isLogFull > 0  ||  spaceLeft <= 50) // 50 pages, is 100K in a 2K ASE (well int's NOT MB at least)
					{
						_logger.warn("Truncating the transaction log in the master database. Issuing SQL 'dump tran master with truncate_only'. isLogFull='"+isLogFull+"', spaceLeftInPages='"+spaceLeft+"'.");
						stmt = conn.createStatement();
						stmt.execute("dump tran master with truncate_only");
						stmt.close();
						
						if (DbxTune.hasGui())
						{
							String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());

							JOptionPane optionPane = new JOptionPane(
									"<html>" +
									"Transaction log in the master database has been <b>truncated</b>. <br>" +
									"<br>" +
									"Space left in the master database transaction log was: spaceLeftInPages='"+spaceLeft+"'.<br>" +
									"If the transaction log in the master database becomes full, the system <i>could</i> <b>halt</b>...<br>" +
									"<br>" +
									"SQL Issued: <code>dump tran master with truncate_only</code><br>" +
									"</html>",
									JOptionPane.INFORMATION_MESSAGE);
							JDialog dialog = optionPane.createDialog(MainFrame.getInstance(), "ASE master transaction log was truncated @ "+dateStr);
							dialog.setModal(false);
							dialog.setVisible(true);
						}
					}
				}
			}
		}
		catch (SQLException e)
		{
			_logger.warn("When checking for a full transaction log in master, we got problem, but skipping this problem and continuing.", e);
		}
	}


	@Override
	public String getServerTimeCmd()
	{
		return "select getdate() \n";
	}

	@Override
	protected String getIsClosedSql()
	{
		return "select 'AseTune-check:isClosed(conn)'";
	}

	@Override
	public ITableTooltip createCmToolTipSupplier(CountersModel cm)
	{
		return new ToolTipSupplierAse(cm);
	}

	
	//==================================================================
	// BEGIN: NO-GUI methods
	//==================================================================
	@Override
	public DbxConnection noGuiConnect(String dbmsUsername, String dbmsPassword, String dbmsServer, String dbmsHostPortStr, String jdbcUrlOptions) throws SQLException, Exception
	{
		// get a connection
		try
		{
			// FIXME: this doesn't work for OTHER DBMS than Sybase...
			AseConnectionFactory.setUser(dbmsUsername); // Set this just for SendConnectInfo uses it
			AseConnectionFactory.setHostPort(dbmsHostPortStr);

			Properties props = new Properties();
			props.putAll(StringUtil.parseCommaStrToMap(jdbcUrlOptions, "=", ";"));
			
			// ENCRYPT_PASSWORD=true
			if ( ! props.containsKey("ENCRYPT_PASSWORD") )
			{
				props.setProperty("ENCRYPT_PASSWORD", "true");
				_logger.info("Adding jConnect JDBC connection property 'ENCRYPT_PASSWORD=true'");
			}
			
			Connection jdbcConn = AseConnectionFactory.getConnection(dbmsHostPortStr, null, dbmsUsername, dbmsPassword, Version.getAppName()+"-nogui", Version.getVersionStr(), null, props, null);
			DbxConnection conn = DbxConnection.createDbxConnection(jdbcConn);

			// set the connection props so it can be reused...
			// FIXME: This is very ASE Specific right now... it needs to be more generic for DbxTune (sqlServerTune, oracleTune, etc)
			ConnectionProp cp = new ConnectionProp();
			cp.setLoginTimeout ( 20 );
			cp.setDriverClass  ( AseConnectionFactory.getDriver() );
			cp.setUrl          ( AseConnectionFactory.getUrlTemplateBase() + AseConnectionFactory.getHostPortStr() );
//			cp.setUrlOptions   ( jdbcUrlOptions );
			cp.setUrlOptions   ( props );
			cp.setUsername     ( dbmsUsername );
			cp.setPassword     ( dbmsPassword );
			cp.setAppName      ( Version.getAppName() );
			cp.setAppVersion   ( Version.getVersionStr() );
//			cp.setHostPort     ( hosts, ports );
//			cp.setSshTunnelInfo( sshTunnelInfo );

			DbxConnection.setDefaultConnProp(cp);
			
			// ASE: 
			// XML Plan Cache... maybe it's not the perfect place to initialize this...
			XmlPlanCache.setInstance( new XmlPlanCacheAse( new ConnectionProvider()
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
//					return getCounterController().getMonConnection();
					return getMonConnection();
				}
			}) );

			
			// CHECK the connection for proper configuration.
			// If failure, go and FIX
			// FIXME: implement the below "set minimal logging options"
			if ( ! AseConnectionUtils.checkForMonitorOptions(conn, dbmsUsername, false, null) )
			{
				AseConnectionUtils.setBasicAseConfigForMonitoring(conn);
			}

			// CHECK the connection for proper configuration.
			// The fix did not work, so lets get out of here
			if ( ! AseConnectionUtils.checkForMonitorOptions(conn, dbmsUsername, false, null) )
			{
				_logger.error("Problems when checking the ASE Server for 'proper monitoring configuration'.");

				// Disconnect, and get out of here...
				conn.closeNoThrow();
				
				// STOP: throw Exception() will case use to "stop/exit"
				throw new Exception("Problems when checking the ASE Server for 'proper monitoring configuration'.");
			}

			// Finally return the connection
			return conn;
		}
		catch (SQLException e)
		{
			String msg = AseConnectionUtils.getMessageFromSQLException(e, false); 
			_logger.error("Problems when connecting to a ASE Server. "+msg);

			// JZ00L: Login failed
			if (e.getSQLState().equals("JZ00L"))
			{
				_logger.error("Faulty PASSWORD when connecting to the server '"+dbmsServer+"' at '"+dbmsHostPortStr+"', with user '"+dbmsUsername+"', I cant recover from this... exiting...");

				// STOP: throw Exception() will case use to "stop/exit"
				throw new Exception("Faulty PASSWORD when connecting to the server '"+dbmsServer+"' at '"+dbmsHostPortStr+"', with user '"+dbmsUsername+"', I cant recover from this... exiting...");
			}
			
			_logger.info("Connection failed, right now... A new connection attempt will done soon...");
			throw e;
		}
		catch (Exception e)
		{
			_logger.error("Problems when connecting to a ASE Server. Caught: "+e);

			// STOP: throw Exception() will case use to "stop/exit"
			throw new Exception("Problems when connecting to a ASE Server. Caught: "+e, e);
		}
//		// we should never get here
//		throw new Exception("We should never get here...");
	}
	//==================================================================
	// END: NO-GUI methods
	//==================================================================
}
