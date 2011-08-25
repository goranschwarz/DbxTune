/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */


package asemon;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.IconHighlighter;

import asemon.cm.CmSybMessageHandler;
import asemon.cm.CountersModel;
import asemon.cm.CountersModelAppend;
import asemon.cm.CountersModelUserDefined;
import asemon.cm.SamplingCnt;
import asemon.cm.sql.VersionInfo;
import asemon.gui.MainFrame;
import asemon.gui.SplashWindow;
import asemon.gui.SummaryPanel;
import asemon.gui.TabularCntrPanel;
import asemon.gui.TrendGraph;
import asemon.utils.AseConnectionUtils;
import asemon.utils.Configuration;
import asemon.utils.SwingUtils;


/*
 ***********************************************************
 *  Below is from: http://www.sypron.nl/mda.html 
 ***********************************************************
 *  
Enhancements to MDA tables in subsequent ASE versions
The MDA tables were first introduced in ASE 12.5.0.3. This section lists the enhancements to the MDA tables in subsequent versions of ASE. I'll try to keep this list up-to-date.

ASE 12.5.1 IR -- 5 new columns:

    * monErrorLog.State - 'state' of an error
    * monOpenDatabases.QuiesceTag - the tag specified with 'quiesce database' (if any)
    * monOpenDatabases.SuspendedProcesses - number of currently suspended processes due to log-full condition in this database
    * monProcessWorkerThread.FamilyID - for parallel queries, the spid of parent process
    * monProcessWorkerThread.ParallelQueries - total # parallel queries attempted


ASE 12.5.2 IR -- 2 new columns:

    * monProcessObject.TableSize - table size in Kbyte
    * monProcessActivity.WorkTables - total number of work tables created by the process
    * Note: the uninitialized milliseconds in monSysStatement.StartTime / EndTime have been fixed in 12.5.2


ASE 12.5.3 IR -- 4 new columns:

    * A column ServerUserID has been added to monProcessActivity, monProcessSQLText and monSysSQLText; this column is the login's 'suid'.
    * monProcessProcedures.LineNumber - the line in the procedure currently being executed

In addition, as of 12.5.3, monOpenObjectActivity contains details about tables and indexes only. Prior to 12.5.3, this table could contain rowsa row for an executed stored procedure, but these details (like the Operations column) were not reliable.

ASE 12.5.3 ESD#2 -- 4 new columns:

    * monEngine.Yields - #times this engine yielded to the Operating System
    * monEngine.DiskIOChecks - #times this engine checked for asynchronous disk I/O
    * monEngine.DiskIOPolled - #times this engine polled for completion of outstanding asynchronous disk I/O.
    * monEngine.DiskIOCompleted - #asynchronous disk I/Os that were completed when this engine polled


ASE 15.0 -- 2 new tables and various new/changed columns:

    * The new table monOpenPartitionActivity reports monitoring statistics at partitition level
    * The new table monLicense shows the details for the license keys that are active in this server

New columns in monEngine:

    * HkgcMaxQSize - maximum #items that can be queued for HK garbage collection in this engine
    * HkgcPendingItems - #items yet to be garbage-collected by the HK in this engine
    * HkgcHWMItems - maximum #pending items queued for HK garbage collection at any instance of time since server restarted
    * HkgcOverflows - #items that could not be queued for HK garbage collection due to queue overflows

New columns in monCachedObject:

    * PartitionID, PartitionName - partition name and ID
    * TotalSizeKB - the total size of the object (table or index)

New columns in monOpenObjectActivity:

    * DBName - the databasename corresponding to DBID

New/changed columns in monProcessObject:

    * PartitionID, PartitionName - partition name and ID
    * TableSize has been changed to PartitionSize - this reflects the size of the partition for the object

ASE 15.0 ESD#2 -- 5 new columns
Perhaps the most important enhancement in ASE 15.0 ESD#2 is the new 'materialized' option with which the MDA proxy tables are created. In 15.0 ESD#2, the MDA tables no longer use the 'backdoor' connection back into to the server itself and consequently, the 'loopback' server name alias is no longer needed either. This new feature reduces some of the overhead of querying the MDA tables. There's nothing you have to do to benefit from this new feature other than running the 'installmontables' script that comes with 15.0 ESD#2.

New columns in monLocks:

    * BlockedState - identifies whether a lock is being blocked or is blocking others
    * BlockedBy - for blocked locks, identifies the session this lock is being blocked by

New columns in monSysStatement:

    * RowsAffected - the number of rows affected by the statement, similar to @@rowcount
    * ErrorStatus - the SQL return status of the statement, similar to @@error

New column in monProcessStatement:

    * RowsAffected - the number of rows affected by the statement, similar to @@rowcount


ASE 15.0.1 -- no changes were made in 15.0.1


***********************************************************
 ***********************************************************
  ***********************************************************
  *  Below is from: Whats new in 15.0.2 
  ***********************************************************
 ***********************************************************
***********************************************************
Adaptive Server 15.0.2 adds information to the following monitoring tables.

--------------------------
monOpenPartitionActivity
--------------------------
monOpenPartitionActivity reports information about the events for each partition
in the housekeeper garbage collection queues. The columns added are:

- HkgcRequests
reports the total number of events queued for the partition.
Updates to monitoring tables
200 Adaptive Server Enterprise
A large value reported by HkgcRequests implies the sytem is generating
lot of garbage for that particular partition or object.

- HkgcPending
reports the number of pending events for the partition.
Large value reported by HkgcPending implies a lot of garbage is yet to be
collected, although the housekeeper will clean the garbage up. However,
if Adaptive Server is rebooted, all the entries in the housekeeper queues
are lost and the garbage from those page is not collected when you next
start Adaptive Server.

- HkgcOverflow
reports the number of partition events that overflowed
A large value reported by HkgcOverflow implies the housekeeper queues
are getting full and the garbage that is generated will not be cleaned
because the housekeep can not schedule the job.
These columns report on per-partition basis, and give an idea of the amount of
garbage generated in each partition for different objects.


--------------------------
monOpenObectActivity
--------------------------
monOpenObjectActivity reports information about the events for each object in
the housekeeper garbage collection queues. The columns added are:

- HkgcRequests
reports the total number of events queued for the object.
A large value reported by HkgcRequests implies the sytem is generating
lot of garbage for that particular partition or object.

- HkgcPending
reports the number of pending events for the object.
Large value reported by HkgcPending implies a lot of garbage is yet to be
collected, although the housekeeper will clean the garbage up. However,
if Adaptive Server is rebooted, all the entries in the housekeeper queues
are lost and the garbage from those page is not collected when you next
start Adaptive Server.

- HkgcOverflow
reports the number of object events that overflowed
A large value reported by HkgcOverflow implies the housekeeper queues
are getting full and the garbage that is generated will not be cleaned
because the housekeep can not schedule the job.

--------------------------
monDeadLock 
--------------------------
monDeadLock reports the locking information (filename and line number)
collected from the lock record deadlock chain. The columns added are:

- HeldSrcCodeID
filename and line number where Adaptive Server requests the holding lock

- WaitSrcCodeID
filename and line where Adaptive Server requests the waiting lock.

--------------------------
monLocks
--------------------------
monLocks adds the SourceCodeID column, which reports locking information
recorded in the lock record structure:

- SourceCodeID
Filename and line at which the lock is requested









***********************************************************
 ***********************************************************
  ***********************************************************
  *  Below is from: Whats new in 15.0.2.5
  ***********************************************************
 ***********************************************************
***********************************************************
ASE 15.0.2 #esd2, 15.0.2 esd#4 -- no changes (NB: 15.0.2 esd#3 was not released)
ASE 15.0.2 #esd5 -- 12 new columns

New column in monEngine:

    * MaxOutstandingIOs - the max.# of I/Os pending for each engine

New column in monProcessNetIO:

    * NetworkEngineNumber - engine handling the network IO for this SPID

New column in monProcessProcedures:

    * StatementNumber - the statement in the stored procedure currently being executed

New columns in monOpenDatabases:

    * LastCheckpointTime - date/time of the start of the last checkpoint for this database
    * LastTranLogDumpTime - date/time of the start of the last log dump for this database

New column DBName was added to the following tables:

    * monLocks
    * monProcessStatement
    * monSysStatement
    * monSysPlanText
    * monCachedStatement

New column ServerUserID was added to the following tables:

    * monProcess
    * monProcessWaits

*/

public abstract class GetCounters 
extends Thread
{
	/** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(GetCounters.class);

	/** Instance variable */
	private static GetCounters _instance = null;

	protected Thread _thread = null;

	/** The counterModels has been created */
	private static boolean _countersIsCreated  = false; 

	/** have we been initialized or not */
	private static boolean _isInitialized      = false; 

	/** if we should do refreshing of the counters, or is refreshing paused */
	private static boolean _refreshIsEnabled   = false;

	/** if any monitoring thread is currently getting information from the monitored server */
	private static boolean _refreshingCounters = false;

	
	private static String _waitEvent = "";
	
	/** Keep a list of all CounterModels that you have initialized */
	protected static List<CountersModel> _CMList = new ArrayList<CountersModel>();

	public static List<CountersModel> getCmList() { return _CMList; }

	public static CountersModel getCmByName(String name) 
	{
		for (CountersModel cm : _CMList)
		{
			if (cm != null && cm.getName().equalsIgnoreCase(name))
			{
				return cm;
			}
		}
		return null;
	}
	
	public static CountersModel getCmByDisplayName(String name) 
	{
		for (CountersModel cm : _CMList)
		{
			if (cm != null && cm.getDisplayName().equalsIgnoreCase(name))
			{
				return cm;
			}
		}
		return null;
	}
	
	public static boolean hasInstance()
	{
		return (_instance != null);
	}

	public static GetCounters getInstance()
	{
		if (_instance == null)
			throw new RuntimeException("No GetCounters instance exists.");
		return _instance;
	}

	public static void setInstance(GetCounters cnt)
	{
		_instance = cnt;
	}

	/** Init needs to be implemented by any subclass */
	public abstract void init()
	throws Exception;

	/** The run is located in any implementing subclass */
	public abstract void run();

	
	public static void setWaitEvent(String str)
	{
		_waitEvent = str;
	}
	public static String getWaitEvent()
	{
		return _waitEvent;
	}

	public boolean isInitialized()
	{
		return _isInitialized;
	}

	/**
	 * When we have a database connection, lets do some extra init this
	 * so all the CountersModel can decide what SQL to use. <br>
	 * SQL statement usually depends on what ASE Server version we monitor.
	 * 
	 * @param conn
	 * @param aseVersion
	 * @param isClusterEnabled
	 * @param monTablesVersion
	 */
	public void initCounters(Connection conn, int aseVersion, boolean isClusterEnabled, int monTablesVersion)
	{
		if (_isInitialized)
			return;

		if (! _countersIsCreated)
			createCounters();
		
		_logger.info("Initializing all CM objects, using ASE server version number "+aseVersion+", isClusterEnabled="+isClusterEnabled+" with monTables Install version "+monTablesVersion+".");

		// Get active ASE Roles
		List<String> activeRoleList = AseConnectionUtils.getActiveRoles(conn);
		
		// Get active Monitor Configuration
		Map<String,Integer> monitorConfigMap = AseConnectionUtils.getMonitorConfigs(conn);

		// in version 15.0.3.1 compatibility_mode was introduced, this to use 12.5.4 optimizer & exec engine
		// This will hurt performance, especially when querying sysmonitors table, so set this to off
		if (aseVersion >= 15031)
			AseConnectionUtils.setCompatibilityMode(conn, false);

		// initialize all the CM's
		Iterator<CountersModel> i = _CMList.iterator();
		while (i.hasNext())
		{
			CountersModel cm = i.next();
			
			if (cm != null)
			{
				_logger.debug("Initializing CM named '"+cm.getName()+"', display name '"+cm.getDisplayName()+"', using ASE server version number "+aseVersion+".");

				// set the version
				cm.setServerVersion(aseVersion);
				cm.setClusterEnabled(isClusterEnabled);
				
				// set the active roles, so it can be used in initSql()
				cm.setActiveRoles(activeRoleList);

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

			}
		}

		// Make some specific Cluster settings
		if (isClusterEnabled)
		{
			int systemView = AseConnectionUtils.CE_SYSTEM_VIEW_UNKNOWN;

			if (Asemon.hasGUI())
			{
				systemView = SummaryPanel.getInstance().getClusterView();
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
			
		_isInitialized = true;
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
		//----------------------
		// In some versions we need to do some checks before we refresh the counters
		// - 15.0.3 ESD#1, 15.0.3.ESD#2, 15.0.3.ESD#3 (fix is estimated for the 'bharani' release)
		//   if: sp_configure 'capture missing statistics' is true, then the sysstatistics table in
		//       the master database will be updated on mon* tables, so the transaction log might get full
		// CR# 581760
		// "capture missing statistics" will capture statistics for MDA Tables
		// http://www-dse/cgi-bin/websql/websql.dir/QTS/bugsheet.hts?GO=GO&bugid=581760
		//----------------------
		try
		{
			int aseVersion = 0;
			if (MonTablesDictionary.getInstance() != null)
				aseVersion = MonTablesDictionary.getInstance().aseVersionNum;

//			if (    aseVersion >= 15031 && aseVersion <= 15033
//				 || aseVersion >= 15500 && aseVersion <= 15501
//			   )
			if ( aseVersion >= 15031 && aseVersion < 16000 )
			{
//				Connection conn = MainFrame.getMonConnection();
//				boolean captureMissingStatistics =AseConnectionUtils.getAseConfigRunValueBooleanNoEx(conn, "capture missing statistics");
//				if (captureMissingStatistics)
				if (true)
				{
					_logger.debug("Checking for full transaction log in the master database.");
	
					String sql = "select 'isLoggFull' = lct_admin('logfull', db_id('master'))," +
					             "       'spaceLeft'  = lct_admin('logsegment_freepages', db_id('master')) " +      // Describes the free space available for a database
					             "                    - lct_admin('reserve', 0) " +                                 // Returns the current last-chance threshold of the transaction log in the database from which the command was issued
					             "                    - lct_admin('reserved_for_rollbacks', db_id('master'), 0) " + // Determines the number of pages reserved for rollbacks in the master database
					             "                    - @@thresh_hysteresis";                                       // Take away hysteresis
	
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
	
					if (isLogFull > 0  ||  spaceLeft <= 20) // 20 pages ? (well int's NOT MB at least)
					{
						_logger.warn("Truncating the transaction log in the master database. Issuing SQL 'dump tran master with truncate_only'. isLogFull='"+isLogFull+"', spaceLeft='"+spaceLeft+"'.");
						stmt = conn.createStatement();
						stmt.execute("dump tran master with truncate_only");
						stmt.close();
					}
				}
			}
		}
		catch (SQLException e)
		{
			_logger.warn("When checking for a full transaction log in master, we got problem, but skipping this problem and continuing.", e);
		}
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
	public void createCounters()
	{
		if (_countersIsCreated)
			return;

		_logger.info("Creating ALL CM Objects.");
		
		String colorStr = null;
		Configuration conf = Configuration.getInstance(Configuration.CONF);

		CountersModel tmp = null;

		String   name;
		String   displayName;
		String   description;
		int      needVersion   = 0; // Minimum ASE Version without the Cluster Edition
		int      needCeVersion = 0; // Minimum ASE Version WITH the Cluster Edition

		String[] needRole;
		String[] monTables;
		String[] needConfig;  //could look like: "config option 1[=setToValue]"
		                      //Example: needsConfig = {"statement cache size", "enable stmt cache monitoring=1"};
		                      //Example: needsConfig = {"deadlock pipe active=1", "deadlock pipe max=500"};

		String[] colsCalcDiff;
		String[] colsCalcPCT;
		List<String> pkList;

		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// SUMMARY Panel
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------

		name         = SummaryPanel.CM_NAME; //"CMsummary";
		displayName  = "Summary";
		description  = "Overview of how the system performs.";

		SplashWindow.drawProgress("Loading: Counter Model '"+SummaryPanel.CM_NAME + "'");
		
		needVersion  = 0;
		needCeVersion= 0;
		monTables    = new String[] {"monState"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {};
		colsCalcDiff = new String[] {"LockWaits", "CheckPoints", "NumDeadlocks", "Connections", "Transactions", 
		                             "cpu_busy", "cpu_io", "cpu_idle", "io_total_read", "io_total_write", 
		                             "aaConnections", "distinctLogins", 
		                             "pack_received", "pack_sent", "packet_errors", "total_errors"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList<String>();

		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion,
				false, true)
		{
			private static final long serialVersionUID = -4476798027541082165L;

			@Override
			public boolean isRefreshable()
			{
				// The SUMMARY should ALWAYS be refresched
				return true;
			}
			
			@Override
			public void initSql(Connection conn)
			{
				int aseVersion = getServerVersion();

				String nwAddrInfo = "'no sa_role'";
				if (isRoleActive(AseConnectionUtils.SA_ROLE))
				{
//					nwAddrInfo = "(select min(convert(varchar(255),address_info)) from syslisteners where address_info not like 'localhost%')";
					nwAddrInfo = "'" + AseConnectionUtils.getListeners(conn, false, true, MainFrame.getInstance()) + "'";
				}

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				// get all the columns for SMP servers
				// if Cluster is enabled, one row for every instance will appear
				//                        so lets try to min, max or sum the rows into 1 row...
				//                        which will "simulate" a SMP environment
				cols1 = "*";
				if (isClusterEnabled())
				{
					cols1 = "";
					cols1 += "  LockWaitThreshold   = max(LockWaitThreshold) \n";
					cols1 += ", LockWaits           = sum(LockWaits) \n";
					cols1 += ", DaysRunning         = max(DaysRunning) \n";
					cols1 += ", CheckPoints         = sum(CheckPoints) \n";
					cols1 += ", NumDeadlocks        = sum(NumDeadlocks) \n";
					cols1 += ", DiagnosticDumps     = sum(DiagnosticDumps) \n";
					cols1 += ", Connections         = sum(Connections) \n";
					cols1 += ", MaxRecovery         = avg(MaxRecovery) \n";
					cols1 += ", Transactions        = sum(Transactions) \n";
					cols1 += ", StartDate           = min(StartDate) \n";
					cols1 += ", CountersCleared     = max(CountersCleared) \n";
				}

				cols2 = ", aseVersion         = @@version" +
						", atAtServerName     = @@servername" +
						", clusterInstanceId  = " + (isClusterEnabled() ? "convert(varchar(15),@@instanceid)"     : "'Not Enabled'") + 
						", clusterCoordId     = " + (isClusterEnabled() ? "convert(varchar(3), @@clustercoordid)" : "'Not Enabled'") + 
						", timeIsNow          = getdate()" +
						", NetworkAddressInfo = " + nwAddrInfo +

						", bootcount          = @@bootcount" + // from 12.5.0.3
						", recovery_state     = "+ (aseVersion >= 12510 ? "@@recovery_state" : "'Introduced in ASE 12.5.1'") + 

						", cpu_busy           = @@cpu_busy" +
						", cpu_io             = @@io_busy" +
						", cpu_idle           = @@idle" +
						", io_total_read      = @@total_read" +
						", io_total_write     = @@total_write" +

						", aaConnections      = @@connections" +
						", distinctLogins     = (select count(distinct suid) from sysprocesses)" +

						", pack_received      = @@pack_received" +
						", pack_sent          = @@pack_sent" +

						", packet_errors      = @@packet_errors" +
						", total_errors       = @@total_errors" +
						"";

				cols3 = "";

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" + 
					"from monState A \n";

				setSql(sql);
			}

			@Override
			public void updateGraphData(TrendGraphDataPoint tgdp)
			{
				int aseVersion = getServerVersion();

				if ("aaCpuGraph".equals(tgdp.getName()))
				{
					Double cpuUser        = getDiffValueAsDouble(0, "cpu_busy");
					Double cpuSystem      = getDiffValueAsDouble(0, "cpu_io");
					Double cpuIdle        = getDiffValueAsDouble(0, "cpu_idle");
					if (cpuUser != null && cpuSystem != null && cpuIdle != null)
					{
						double CPUTime   = cpuUser  .doubleValue() + cpuSystem.doubleValue() + cpuIdle.doubleValue();
						double CPUUser   = cpuUser  .doubleValue();
						double CPUSystem = cpuSystem.doubleValue();
//						double CPUIdle   = cpuIdle  .doubleValue();

						BigDecimal calcCPUTime       = new BigDecimal( ((1.0 * (CPUUser + CPUSystem)) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
						BigDecimal calcUserCPUTime   = new BigDecimal( ((1.0 * (CPUUser            )) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
						BigDecimal calcSystemCPUTime = new BigDecimal( ((1.0 * (CPUSystem          )) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//						BigDecimal calcIdleCPUTime   = new BigDecimal( ((1.0 * (CPUIdle            )) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

//						_cpuTime_txt          .setText(calcCPUTime      .toString());
//						_cpuUser_txt          .setText(calcUserCPUTime  .toString());
//						_cpuSystem_txt        .setText(calcSystemCPUTime.toString());
//						_cpuIdle_txt          .setText(calcIdleCPUTime  .toString());

						Double[] arr = new Double[3];

						arr[0] = calcCPUTime      .doubleValue();
						arr[1] = calcSystemCPUTime.doubleValue();
						arr[2] = calcUserCPUTime  .doubleValue();
						_logger.debug("updateGraphData(aaCpuGraph): @@cpu_busy+@@cpu_io='"+arr[0]+"', @@cpu_io='"+arr[1]+"', @@cpu_busy='"+arr[2]+"'.");

						// Set the values
						tgdp.setDate(this.getTimestamp());
						tgdp.setData(arr);
					}
				}

				if ("TransGraph".equals(tgdp.getName()))
				{
					if (aseVersion < 15033)
					{
						// disable the transactions graph checkbox...
						TrendGraph tg = getTrendGraph("TransGraph");
						if (tg != null)
						{
							JCheckBoxMenuItem menuItem = tg.getViewMenuItem();
							if (menuItem.isSelected())
								menuItem.doClick();
						}
					}
					else
					{
						Double[] arr = new Double[1];
	
						arr[0] = this.getRateValueSum("Transactions");
						_logger.debug("updateGraphData(TransGraph): Transactions='"+arr[0]+"'.");
	
						// Set the values
						tgdp.setDate(this.getTimestamp());
						tgdp.setData(arr);
					}
				}

				if ("ConnectionsGraph".equals(tgdp.getName()))
				{	
					Double[] arr = new Double[3];

					arr[0] = this.getAbsValueAsDouble (0, "Connections");
					arr[1] = this.getAbsValueAsDouble (0, "distinctLogins");
					arr[2] = this.getDiffValueAsDouble(0, "aaConnections");
					_logger.debug("updateGraphData(ConnectionsGraph): Connections='"+arr[0]+"', distinctLogins='"+arr[1]+"', aaConnections='"+arr[2]+"'.");

					// Set the values
					tgdp.setDate(this.getTimestamp());
					tgdp.setData(arr);
				}

				if ("aaReadWriteGraph".equals(tgdp.getName()))
				{	
					Double[] arr = new Double[2];

					arr[0] = this.getRateValueAsDouble (0, "io_total_read");
					arr[1] = this.getRateValueAsDouble (0, "io_total_write");
					_logger.debug("updateGraphData(aaReadWriteGraph): io_total_read='"+arr[0]+"', io_total_write='"+arr[1]+"'.");

					// Set the values
					tgdp.setDate(this.getTimestamp());
					tgdp.setData(arr);
				}

				if ("aaPacketGraph".equals(tgdp.getName()))
				{	
					Double[] arr = new Double[3];

					arr[0] = this.getRateValueAsDouble (0, "pack_received");
					arr[1] = this.getRateValueAsDouble (0, "pack_sent");
					arr[2] = this.getRateValueAsDouble (0, "packet_errors");
					_logger.debug("updateGraphData(aaPacketGraph): packet_errors='"+arr[0]+"', total_errors='"+arr[1]+"', packet_errors='"+arr[2]+"'.");

					// Set the values
					tgdp.setDate(this.getTimestamp());
					tgdp.setData(arr);
				}
			}
		};
		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		tmp.setTabPanel(null);
		tmp.addTrendGraphData("aaCpuGraph",       new TrendGraphDataPoint("aaCpuGraph",       new String[] { "System+User CPU (@@cpu_busy + @@cpu_io)", "System CPU (@@cpu_io)", "User CPU (@@cpu_busy)" }));
		tmp.addTrendGraphData("TransGraph",       new TrendGraphDataPoint("TransGraph",       new String[] { "Transactions" }));
		tmp.addTrendGraphData("ConnectionsGraph", new TrendGraphDataPoint("ConnectionsGraph", new String[] { "Connections", "distinctLogins", "@@connections" }));
		tmp.addTrendGraphData("aaReadWriteGraph", new TrendGraphDataPoint("aaReadWriteGraph", new String[] { "@@total_read", "@@total_write" }));
		tmp.addTrendGraphData("aaPacketGraph",    new TrendGraphDataPoint("aaPacketGraph",    new String[] { "@@pack_received", "@@pack_sent", "@@packet_errors" }));
		if (Asemon.hasGUI())
		{
			tmp.addTableModelListener( MainFrame.getSummaryPanel() );

			TrendGraph tg = null;

			// GRAPH: aaCpuGraph
			tg = new TrendGraph("aaCpuGraph",
					"CPU Summary, Global Variables", 	// Menu Checkbox text
					"CPU Summary for all Engines (using @@cpu_busy, @@cpu_io)", // Label 
					new String[] { "System+User CPU (@@cpu_busy + @@cpu_io)", "System CPU (@@cpu_io)", "User CPU (@@cpu_busy)" }, 
					true, tmp, false);
			tmp.addTrendGraph("aaCpuGraph", tg, true);

			// GRAPH: Transaction Graph
			tg = new TrendGraph("TransGraph",
					// Menu Checkbox text
					"Transaction per second",
					// Label 
					"Number of Transaction per Second (only in 15.0.3 esd#3 and later)", // Label 
					new String[] { "Transactions" }, 
					false, tmp, false);
			tmp.addTrendGraph("TransGraph", tg, true);

			// GRAPH: Connections Graph
			tg = new TrendGraph("ConnectionsGraph",
					// Menu Checkbox text
					"Connections/Users in ASE",
					// Label 
					"Connections/Users connected to the ASE", // Label 
					new String[] { "Connections", "distinctLogins", "@@connections" }, 
					false, tmp, false);
			tmp.addTrendGraph("ConnectionsGraph", tg, true);

			// GRAPH: @@total_read, @@total_write
			tg = new TrendGraph("aaReadWriteGraph",
					// Menu Checkbox text
					"Disk read/write, Global Variables",
					// Label 
					"Disk read/write per second, using @@total_read, @@total_write", // Label 
					new String[] { "@@total_read", "@@total_write" }, 
					false, tmp, false);
			tmp.addTrendGraph("aaReadWriteGraph", tg, true);

			// GRAPH: @@pack_received, @@pack_sent
			tg = new TrendGraph("aaPacketGraph",
					// Menu Checkbox text
					"Network Packets received/sent, Global Variables",
					// Label 
					"Network Packets received/sent per second, using @@pack_received, @@pack_sent", // Label 
					new String[] { "@@pack_received", "@@pack_sent", "@@packet_errors" }, 
					false, tmp, false);
			tmp.addTrendGraph("aaPacketGraph", tg, true);
		}

		_CMList.add(tmp);




		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// Objects Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// 12.5.4 version
		//DBID        ObjectID    IndexID     LogicalReads PhysicalReads APFReads    PagesRead   PhysicalWrites PagesWritten OptSelectCount UsedCount   RowsInserted RowsDeleted RowsUpdated Operations  LockRequests LockWaits   LastOptSelectDate              LastUsedDate                   
		//----------- ----------- ----------- ------------ ------------- ----------- ----------- -------------- ------------ -------------- ----------- ------------ ----------- ----------- ----------- ------------ ----------- -----------------              ------------                   
//		-----------------------------------------
//		15.0.3 Cluster Edition.... from Oslo....
//		15.5.0 SMP.... gorans-xp.... looks the same
//		-----------------------------------------
//
//
//
//		1> select * from monTableColumns where TableName = 'monOpenObjectActivity'
//		2> go
//		 TableID     ColumnID    TypeID      Precision Scale Length Indicators  TableName                      ColumnName                     TypeName             Description
//		 ----------- ----------- ----------- --------- ----- ------ ----------- ------------------------------ ------------------------------ -------------------- ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//		          27           0          56         0     0      4           0 monOpenObjectActivity          DBID                           int                  Unique identifier for the database
//		          27           1          56         0     0      4           0 monOpenObjectActivity          ObjectID                       int                  Unique identifier for the object
//		          27           2          56         0     0      4           0 monOpenObjectActivity          IndexID                        int                  Unique identifier for the index
//		          27           3          48         0     0      1           0 monOpenObjectActivity          InstanceID                     tinyint              The Server Instance Identifier (cluster only)
//		          27           4          39         0     0     30           0 monOpenObjectActivity          DBName                         varchar              Name of the database in which the object resides
//		          27           5          39         0     0     30           0 monOpenObjectActivity          ObjectName                     varchar              Name of the object
//		          27           6          38         0     0      4           1 monOpenObjectActivity          LogicalReads                   intn                 Total number of buffers read
//		          27           7          38         0     0      4           1 monOpenObjectActivity          PhysicalReads                  intn                 Number of buffers read from disk
//		          27           8          38         0     0      4           1 monOpenObjectActivity          APFReads                       intn                 Number of APF buffers read
//		          27           9          38         0     0      4           1 monOpenObjectActivity          PagesRead                      intn                 Total number of pages read
//		          27          10          38         0     0      4           1 monOpenObjectActivity          PhysicalWrites                 intn                 Total number of buffers written to disk
//		          27          11          38         0     0      4           1 monOpenObjectActivity          PagesWritten                   intn                 Total number of pages written to disk
//		          27          12          38         0     0      4           1 monOpenObjectActivity          RowsInserted                   intn                 Number of rows inserted
//		          27          13          38         0     0      4           1 monOpenObjectActivity          RowsDeleted                    intn                 Number of rows deleted
//		          27          14          38         0     0      4           1 monOpenObjectActivity          RowsUpdated                    intn                 Number of updates
//		          27          15          38         0     0      4           1 monOpenObjectActivity         +Operations                     intn                 Number of times that the object was accessed
//		          27          16          38         0     0      4           1 monOpenObjectActivity         +LockRequests                   intn                 Number of requests for a lock on the object
//		          27          17          38         0     0      4           1 monOpenObjectActivity         +LockWaits                      intn                 Number of times a task waited for a lock for the object
//		          27          18          38         0     0      4           1 monOpenObjectActivity          OptSelectCount                 intn                 Number of times object was selected for plan during compilation
//		          27          19         111         0     0      8           0 monOpenObjectActivity          LastOptSelectDate              datetimn             Last date the index was selected for plan during compilation
//		          27          20          38         0     0      4           1 monOpenObjectActivity          UsedCount                      intn                 Number of times object was used in plan during execution
//		          27          21         111         0     0      8           0 monOpenObjectActivity          LastUsedDate                   datetimn             Last date the index was used in plan during execution
//		          27          22          38         0     0      4           1 monOpenObjectActivity          HkgcRequests                   intn                 Total number of events of the object that were queued in Housekeeper Garbage Collection(HKGC) queues
//		          27          23          38         0     0      4           1 monOpenObjectActivity          HkgcPending                    intn                 Number of events of the object that are still pending in Housekeeper Garbage Collection(HKGC) queues
//		          27          24          38         0     0      4           1 monOpenObjectActivity          HkgcOverflows                  intn                 Number of events of the object that overflowed due to lack of space in Housekeeper Garbage Collection(HKGC) queues
//		          27          25          38         0     0      4           1 monOpenObjectActivity          PhysicalLocks                  intn                 Number of physical locks requested
//		          27          26          38         0     0      4           1 monOpenObjectActivity          PhysicalLocksRetained          intn                 Number of physical locks retained
//		          27          27          38         0     0      4           1 monOpenObjectActivity          PhysicalLocksRetainWaited      intn                 Number of physical lock requests waited before lock is retained
//		          27          28          38         0     0      4           1 monOpenObjectActivity          PhysicalLocksDeadlocks         intn                 Number of times physical lock requests returned deadlock
//		          27          29          38         0     0      4           1 monOpenObjectActivity          PhysicalLocksWaited            intn                 Number of times physical lock requests waited
//		          27          30          38         0     0      4           1 monOpenObjectActivity          PhysicalLocksPageTransfer      intn                 Number of times page transfer was requested by a client at this instance
//		          27          31          38         0     0      4           1 monOpenObjectActivity          TransferReqWaited              intn                 Number of times physical lock requests waited to receive page transfers
//		          27          32         109         0     0      4           1 monOpenObjectActivity          AvgPhysicalLockWaitTime        floatn               Average time waited to get physical lock granted
//		          27          33         109         0     0      4           1 monOpenObjectActivity          AvgTransferReqWaitTime         floatn               Average time physical lock requests waited to receive page transfers
//		          27          34          38         0     0      4           1 monOpenObjectActivity          TotalServiceRequests           intn                 Number of physical lock requests serviced by Cluster Cache Manager
//		          27          35          38         0     0      4           1 monOpenObjectActivity          PhysicalLocksDowngraded        intn                 Number of physical lock downgrade requests serviced by Cluster Cache Manager
//		          27          36          38         0     0      4           1 monOpenObjectActivity          PagesTransferred               intn                 Number of pages transferred by Cluster Cache Manager
//		          27          37          38         0     0      4           1 monOpenObjectActivity          ClusterPageWrites              intn                 Number of pages written to disk by Cluster Cache Manager
//		          27          38         109         0     0      4           0 monOpenObjectActivity          AvgServiceTime                 floatn               Average service time taken by Cluster Cache Manager
//		          27          39         109         0     0      4           0 monOpenObjectActivity          AvgTimeWaitedOnLocalUsers      floatn               Average time taken to service requests due to page in use by local users
//		          27          40         109         0     0      4           0 monOpenObjectActivity          AvgTransferSendWaitTime        floatn               Average time waited by Cluster Cache Manager for page transfer
//		          27          41         109         0     0      4           0 monOpenObjectActivity          AvgIOServiceTime               floatn               Average service time taken by Cluster Cache Manager for writing page to disk
//		          27          42         109         0     0      4           0 monOpenObjectActivity          AvgDowngradeServiceTime        floatn               Average service time taken by Cluster Cache Manager for downgrading physical lock
//
//		(43 rows affected)

		name         = "CMobjActivity";
		displayName  = "Objects";
		description  = "Performance information about object/tables.";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 0;
		needCeVersion= 0;
		monTables    = new String[] {"monOpenObjectActivity"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring=1", "object lockwait timing=1", "per object statistics active=1"};
		colsCalcDiff = new String[] {"LogicalReads","PhysicalReads", "APFReads", "PagesRead", "PhysicalWrites", "PagesWritten", "UsedCount", "RowsInsUpdDel", "RowsInserted", "RowsDeleted", "RowsUpdated", "Operations", "LockRequests", "LockWaits", "HkgcRequests", "HkgcPending", "HkgcOverflows", "OptSelectCount", "PhysicalLocks", "PhysicalLocksRetained", "PhysicalLocksRetainWaited", "PhysicalLocksDeadlocks", "PhysicalLocksWaited", "PhysicalLocksPageTransfer", "TransferReqWaited", "TotalServiceRequests", "PhysicalLocksDowngraded", "PagesTransferred", "ClusterPageWrites"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList<String>();
			pkList.add("DBName");
			pkList.add("ObjectName");
			pkList.add("IndexID");


		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				true, true)
		{
			private static final long serialVersionUID = 6315460036795875430L;

			@Override
			public void initSql(Connection conn)
			{
				// Override defaults from monTableColumns descriptions
				try 
				{
					MonTablesDictionary mtd = MonTablesDictionary.getInstance();
//					mtd.addColumn("monOpenObjectActivity", "PhysicalLocks"                  ,"<html>Number of physical locks requested per object.</html>");
					mtd.addColumn("monOpenObjectActivity", "PhysicalLocksRetained"          ,"<html>Number of physical locks retained. <br>You can use this to identify the lock hit ratio for each object. " +
					                                                                           "<br>Good hit ratios imply balanced partitioning for this object.</html>");
//					mtd.addColumn("monOpenObjectActivity", "PhysicalLocksRetainWaited"      ,"<html>Number of physical lock requests waiting before a lock is retained.</html>");
					mtd.addColumn("monOpenObjectActivity", "PhysicalLocksDeadlocks"         ,"<html>Number of times a physical lock requested returned a deadlock. " +
					                                                                           "<br>The Cluster Physical Locks subsection of sp_sysmon uses this counter to report deadlocks while acquiring physical locks for each object.</html>");
//					mtd.addColumn("monOpenObjectActivity", "PhysicalLocksWaited"            ,"<html>Number of times an instance waits for a physical lock request.</html>");
					mtd.addColumn("monOpenObjectActivity", "PhysicalLocksPageTransfer"      ,"<html>Number of page transfers that occurred when an instance requests a physical lock. " +
					                                                                           "<br>The Cluster Physical Locks subsection of sp_sysmon uses this counter to report the node-to-node transfer and physical-lock acquisition as a node affinity ratio for this object.</html>");
					mtd.addColumn("monOpenObjectActivity", "TransferReqWaited"              ,"<html>Number of times physical lock requests waiting before receiving page transfers.</html>");
					mtd.addColumn("monOpenObjectActivity", "AvgPhysicalLockWaitTime"        ,"<html>The average amount of time clients spend before the physical lock is granted.</html>");
					mtd.addColumn("monOpenObjectActivity", "AvgTransferReqWaitTime"         ,"<html>The average amount of time physical lock requests wait before receiving page transfers.</html>");
					mtd.addColumn("monOpenObjectActivity", "TotalServiceRequests"           ,"<html>Number of physical lock requests serviced by the Cluster Cache Manager of an instance.</html>");
					mtd.addColumn("monOpenObjectActivity", "PhysicalLocksDowngraded"        ,"<html>Number of physical lock downgrade requests serviced by the Cluster Cache Manager of an instance.</html>");
					mtd.addColumn("monOpenObjectActivity", "PagesTransferred"               ,"<html>Number of pages transferred at an instance by the Cluster Cache Manager.</html>");
					mtd.addColumn("monOpenObjectActivity", "ClusterPageWrites"              ,"<html>Number of pages written to disk by the Cluster Cache Manager of an instance.</html>");
					mtd.addColumn("monOpenObjectActivity", "AvgServiceTime"                 ,"<html>The average amount of service time spent by the Cluster Cache Manager of an instance.</html>");
					mtd.addColumn("monOpenObjectActivity", "AvgTimeWaitedOnLocalUsers"      ,"<html>The average amount of service time an instance’s Cluster Cache Manager waits due to page use by users on this instance.</html>");
					mtd.addColumn("monOpenObjectActivity", "AvgTransferSendWaitTime"        ,"<html>The average amount of service time an instance’s Cluster Cache Manager spends for page transfer.</html>");
					mtd.addColumn("monOpenObjectActivity", "AvgIOServiceTime"               ,"<html>The average amount of service time used by an instance’s Cluster Cache Manager for page transfer.</html>");
					mtd.addColumn("monOpenObjectActivity", "AvgDowngradeServiceTime"        ,"<html>The average amount of service time the Cluster Cache Manager uses to downgrade physical locks.</html>");
				}
				catch (NameNotFoundException e) {/*ignore*/}

				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				String dbNameCol  = "DBName=db_name(A.DBID)";
				String objNameCol = "ObjectName=isnull(object_name(A.ObjectID, A.DBID), 'ObjId='+convert(varchar(30),A.ObjectID))"; // if user is not a valid user in A.DBID, then object_name() will return null
				if (aseVersion >= 15000)
				{
					dbNameCol  = "DBName";
					objNameCol = "ObjectName";
					objNameCol = "ObjectName=isnull(object_name(A.ObjectID, A.DBID), 'ObjId='+convert(varchar(30),A.ObjectID))"; // if user is not a valid user in A.DBID, then object_name() will return null
// debug/trace
if (System.getProperty("CMobjActivity.ObjectName", "false").equalsIgnoreCase("true"))
{
	objNameCol = "ObjectName";
	System.out.println("CMobjActivity.ObjectName=true, using the string '"+objNameCol+"' for ObjectName lookup.");
}
				}

				if (isClusterEnabled())
				{
					cols1 += "InstanceID, ";
					this.getPk().add("InstanceID");
				}

				cols1 += dbNameCol + ", " +
				         objNameCol +  ", " + 
				         "A.IndexID, " +
				         "LockScheme = lockscheme(A.ObjectID, A.DBID)," +
				         "LockRequests=isnull(LockRequests,0), LockWaits=isnull(LockWaits,0), " +
				         "LogicalReads, PhysicalReads, APFReads, PagesRead, " +
				         "PhysicalWrites, PagesWritten, UsedCount, " +
				         "RowsInsUpdDel=RowsInserted+RowsDeleted+RowsUpdated, " +
				         "RowsInserted, RowsDeleted, RowsUpdated, Operations, ";
				cols2 += "";
				cols3 += "OptSelectCount, LastOptSelectDate, LastUsedDate";
			//	cols3 = "OptSelectCount, LastOptSelectDate, LastUsedDate, LastOptSelectDateDiff=datediff(ss,LastOptSelectDate,getdate()), LastUsedDateDiff=datediff(ss,LastUsedDate,getdate())";
			// it looked like we got "overflow" in the datediff sometimes... And I have newer really used these cols, so lets take them out for a while...
				if (aseVersion >= 15020)
				{
					cols2 += "HkgcRequests, HkgcPending, HkgcOverflows, ";
				}
				if ( (aseVersion >= 15030 && isClusterEnabled()) || aseVersion >= 15500)
				{
					cols2 += "PhysicalLocks, PhysicalLocksRetained, PhysicalLocksRetainWaited, ";
					cols2 += "PhysicalLocksDeadlocks, PhysicalLocksWaited, PhysicalLocksPageTransfer, ";
					cols2 += "TransferReqWaited, ";
					cols2 += "AvgPhysicalLockWaitTime, AvgTransferReqWaitTime, ";
					cols2 += "TotalServiceRequests, ";
					cols2 += "PhysicalLocksDowngraded, ";
					cols2 += "PagesTransferred, ";
					cols2 += "ClusterPageWrites, ";
					cols2 += "AvgServiceTime, AvgTimeWaitedOnLocalUsers, AvgTransferSendWaitTime, ";
					cols2 += "AvgIOServiceTime, AvgDowngradeServiceTime, ";
				}

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from monOpenObjectActivity A \n" +
					"where UsedCount > 0 OR LockRequests > 0 OR LogicalReads > 100 OR PagesWritten > 100 --OR Operations > 1 \n" +
					(isClusterEnabled() ? "order by 2,3,4" : "order by 1,2,3") + "\n";

				setSql(sql);
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_object_activity.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );

			tcp.setTableToolTipText(
				    "<html>" +
				        "Background colors:" +
				        "<ul>" +
				        "    <li>ORANGE - An Index.</li>" +
				        "</ul>" +
				    "</html>");

			// ORANGE = Index id > 0
			if (conf != null) colorStr = conf.getProperty(name+".color.index");
			tcp.addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					Number indexId = (Number) adapter.getValue(adapter.getColumnIndex("IndexID"));
					if ( indexId != null && indexId.intValue() > 0)
						return true;
					return false;
				}
			}, SwingUtils.parseColor(colorStr, Color.ORANGE), null));
		}

		_CMList.add(tmp);





		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// Process Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = "CMprocActivity";
		displayName  = "Processes";
		description  = "<html><p>What SybasePIDs are doing what.</p>Tip:<br>sort by 'BatchIdDiff', will give you the one that executes the most SQL Batches.<br>Or check 'WaitEventDesc' to find out when the SPID is waiting for.</html>";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 0;
		needCeVersion= 0;
		monTables    = new String[] {"monProcessActivity", "monProcess", "sysprocesses", "monProcessNetIO"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring=1", "object lockwait timing=1", "wait event timing=1"};
		colsCalcDiff = new String[] {"BatchIdDiff", "cpu", "physical_io", "CPUTime", "WaitTime", "LogicalReads", "PhysicalReads", "PagesRead", "PhysicalWrites", "PagesWritten", "TableAccesses","IndexAccesses", "TempDbObjects", "ULCBytesWritten", "ULCFlushes", "ULCFlushFull", "Transactions", "Commits", "Rollbacks", "PacketsSent", "PacketsReceived", "BytesSent", "BytesReceived", "WorkTables", "pssinfo_tempdb_pages"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList<String>();
		     pkList.add("SPID");
		     pkList.add("KPID");

		
		
		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				true, true)
		{
			private static final long serialVersionUID = 161126670167334279L;
			private HashMap<Number,Object> _blockingSpids = new HashMap<Number,Object>(); // <(SPID)Integer> <null> indicator that the SPID is BLOCKING some other SPID

//			private Configuration _conf = Configuration.getInstance(Configuration.CONF);
//			private boolean _sampleSystemThreads = _conf.getBooleanProperty("CMprocActivity.sample.systemThreads", true);

			@Override
			public void initSql(Connection conn)
			{
				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				String optGoalPlan = "";
				if (aseVersion >= 15020)
				{
					optGoalPlan = "plan '(use optgoal allrows_dss)' \n";
				}

				if (isClusterEnabled())
				{
					cols1 += "MP.InstanceID, ";
					this.getPk().add("InstanceID");
				}

				cols1+=" MP.FamilyID, MP.SPID, MP.KPID, MP.NumChildren, \n"
					+ "  SP.status, MP.WaitEventID, \n"
					+ "  WaitClassDesc=convert(varchar(50),''), " // value will be replaced in method localCalculation()
					+ "  WaitEventDesc=convert(varchar(50),''), " // value will be replaced in method localCalculation()
					+ "  MP.SecondsWaiting, MP.BlockingSPID, MP.Command, \n"
					+ "  MP.BatchID, BatchIdDiff=convert(int,MP.BatchID), \n" // BatchIdDiff diff calculated
					+ "  procName = object_name(SP.id, SP.dbid), SP.stmtnum, SP.linenum, \n"
					+ "  MP.Application, SP.hostname, SP.ipaddr, SP.hostprocess, MP.DBName, MP.Login, SP.suid, MP.SecondsConnected, \n"
					+ "  SP.tran_name, SP.cpu, SP.physical_io, \n"
					+ "  A.CPUTime, A.WaitTime, A.LogicalReads, \n"
					+ "  A.PhysicalReads, A.PagesRead, A.PhysicalWrites, A.PagesWritten, \n";
				cols2 += "";
				if (aseVersion >= 12520)
				{
					cols2+="  A.WorkTables,  \n";
				}
				if (aseVersion >= 15020 || (aseVersion >= 12540 && aseVersion <= 15000) )
				{
					cols2+="  tempdb_name = db_name(tempdb_id(SP.spid)), pssinfo_tempdb_pages = convert(int, pssinfo(SP.spid, 'tempdb_pages')), \n";
				}
				if (aseVersion >= 15025)
				{
					cols2+="  N.NetworkEngineNumber, MP.ServerUserID, \n";
				}
				cols3+=" A.TableAccesses, A.IndexAccesses, A.TempDbObjects, \n"
					+ "  A.ULCBytesWritten, A.ULCFlushes, A.ULCFlushFull, \n"
					+ "  A.Transactions, A.Commits, A.Rollbacks, \n"
					+ "  MP.EngineNumber, MP.Priority, \n"
					+ "  N.PacketsSent, N.PacketsReceived, N.BytesSent, N.BytesReceived, \n"
					+ "  MP.ExecutionClass, MP.EngineGroupName";
				
				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from monProcessActivity A, monProcess MP, sysprocesses SP, monProcessNetIO N \n" +
					"where MP.KPID = SP.kpid \n" +
					"  and MP.KPID = A.KPID \n" +
					"  and MP.KPID = N.KPID \n" +
					"  RUNTIME_REPLACE::SAMPLE_SYSTEM_THREADS \n"; // this is replaced in getSql()
//				if (_sampleSystemThreads == false)
//					sql += "  and MP.ServerUserID > 0 \n";
				if (isClusterEnabled())
					sql +=
						"  and MP.InstanceID = SP.instanceid \n" + // FIXME: Need to check if this is working...
						"  and MP.InstanceID = A.InstanceID \n" +
						"  and MP.InstanceID = N.InstanceID \n";

				sql += "order by MP.SPID \n" + 
				       optGoalPlan;


				setSql(sql);
			}
			@Override
			public String getSql()
			{
				String sql = super.getSql();
				
				Configuration conf = Configuration.getInstance(Configuration.TEMP);
				boolean sampleSystemThreads_chk = (conf == null) ? true : conf.getBooleanProperty("CMprocActivity.sample.systemThreads", true);

				String sampleSystemThreadSql = "";
				if ( sampleSystemThreads_chk == false )
					sampleSystemThreadSql = "  and SP.suid > 0 ";

				sql = sql.replace("RUNTIME_REPLACE::SAMPLE_SYSTEM_THREADS", sampleSystemThreadSql);
				
				return sql;
			}

			/** 
			 * Fill in the WaitEventDesc column with data from
			 * MonTableDictionary.. transforms a WaitEventId -> text description
			 * This so we do not have to do a subselect in the query that gets data
			 * doing it this way, means better performance, since the values are cached locally in memory
			 */
			@Override
			public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
			{
				// Where are various columns located in the Vector 
				int pos_WaitEventID = -1, pos_WaitEventDesc = -1, pos_WaitClassDesc = -1, pos_BlockingSPID = -1;
				int waitEventID = 0;
				String waitEventDesc = "";
				String waitClassDesc = "";
				SamplingCnt counters = diffData;
//				SamplingCnt counters = chosenData;
			
				MonTablesDictionary mtd = MonTablesDictionary.getInstance();
				if (mtd == null)
					return;

				if (counters == null)
					return;

				// Reset the blockingSpids Map
				_blockingSpids.clear();
				
				// put the pointer to the Map in the Client Property of the JTable, which should be visible for various places
				if (getTabPanel() != null)
					getTabPanel().putTableClientProperty("blockingSpidMap", _blockingSpids);

				// Find column Id's
				List<String> colNames = counters.getColNames();
				if (colNames==null) return;

				for (int colId=0; colId < colNames.size(); colId++) 
				{
					String colName = (String)colNames.get(colId);
					if (colName.equals("WaitEventID"))   pos_WaitEventID   = colId;
					if (colName.equals("WaitEventDesc")) pos_WaitEventDesc = colId;
					if (colName.equals("WaitClassDesc")) pos_WaitClassDesc = colId;
					if (colName.equals("BlockingSPID"))  pos_BlockingSPID  = colId;

					// Noo need to continue, we got all our columns
					if (pos_WaitEventID >= 0 && pos_WaitEventDesc >= 0 && pos_WaitClassDesc >= 0 && pos_BlockingSPID >= 0)
						break;
				}

				if (pos_WaitEventID < 0 || pos_WaitEventDesc < 0 || pos_WaitClassDesc < 0)
				{
					_logger.debug("Cant find the position for columns ('WaitEventID'="+pos_WaitEventID+", 'WaitEventDesc'="+pos_WaitEventDesc+", 'WaitClassDesc'="+pos_WaitClassDesc+")");
					return;
				}
				
				if (pos_BlockingSPID < 0)
				{
					_logger.debug("Cant find the position for column ('BlockingSPID'="+pos_BlockingSPID+")");
					return;
				}
				
				// Loop on all diffData rows
				for (int rowId=0; rowId < counters.getRowCount(); rowId++) 
				{
//					Vector row = (Vector)counters.rows.get(rowId);
//					Object o = row.get(pos_WaitEventID);
					Object o_waitEventId  = counters.getValueAt(rowId, pos_WaitEventID);
					Object o_blockingSpid = counters.getValueAt(rowId, pos_BlockingSPID);

					if (o_waitEventId instanceof Number)
					{
						waitEventID	  = ((Number)o_waitEventId).intValue();

						if (mtd.hasWaitEventDescription(waitEventID))
						{
							waitEventDesc = mtd.getWaitEventDescription(waitEventID);
							waitClassDesc = mtd.getWaitEventClassDescription(waitEventID);
						}
						else
						{
							waitEventDesc = "";
							waitClassDesc = "";
						}
	
						//row.set( pos_WaitEventDesc, waitEventDesc);
						counters.setValueAt(waitEventDesc, rowId, pos_WaitEventDesc);
						counters.setValueAt(waitClassDesc, rowId, pos_WaitClassDesc);
					}

					// Add any blocking SPIDs to the MAP
					if (o_blockingSpid instanceof Number)
					{
						if (o_blockingSpid != null && ((Number)o_blockingSpid).intValue() != 0 )
							_blockingSpids.put((Number)o_blockingSpid, null);
					}
				}
			}

			@Override
			public void updateGraphData(TrendGraphDataPoint tgdp)
			{
				//
				// Get column 'PhysicalWrites' for the 4 rows, which has the column 
				// Command = 'CHECKPOINT SLEEP', 'HK WASH', 'HK GC', 'HK CHORES'
				//
				// FIXME: this will probably NOT work on Cluster Edition...
				//        Should we just check the local InstanceID or should we do "sum"
				//
				if ("ChkptHkGraph".equals(tgdp.getName()))
				{
					int pos_Command = -1, pos_PhysicalWrites = -1;

					SamplingCnt counters = getCounterData(DATA_RATE);
					if (counters == null) 
						return;

					// Find column Id's
					List<String> colNames = counters.getColNames();
					if (colNames == null) 
						return;

					for (int colId=0; colId < colNames.size(); colId++) 
					{
						String colName = (String)colNames.get(colId);
						if (colName.equals("Command"))        pos_Command        = colId;
						if (colName.equals("PhysicalWrites")) pos_PhysicalWrites = colId;

						// Noo need to continue, we got all our columns
						if (pos_Command >= 0 && pos_PhysicalWrites >= 0)
							break;
					}

					if (pos_Command < 0 || pos_PhysicalWrites < 0)
					{
						_logger.debug("Cant find the position for column ('Command'="+pos_Command+", 'PhysicalWrites'="+pos_PhysicalWrites+")");
						return;
					}
					
					Object o_CheckpointWrite = null;
					Object o_HkWashWrite     = null;
					Object o_HkGcWrite       = null;
					Object o_HkChoresWrite   = null;

					// Loop rows
					for (int rowId=0; rowId < counters.getRowCount(); rowId++)
					{
						Object o_Command = counters.getValueAt(rowId, pos_Command);

						if (o_Command instanceof String)
						{
							String command = (String)o_Command;

							if (command.startsWith("CHECKPOINT"))
								o_CheckpointWrite = counters.getValueAt(rowId, pos_PhysicalWrites);
	
							if (command.startsWith("HK WASH"))
								o_HkWashWrite = counters.getValueAt(rowId, pos_PhysicalWrites);
	
							if (command.startsWith("HK GC"))
								o_HkGcWrite = counters.getValueAt(rowId, pos_PhysicalWrites);
	
							if (command.startsWith("HK CHORES"))
								o_HkChoresWrite = counters.getValueAt(rowId, pos_PhysicalWrites);
						}
						// if we have found all of our rows/values we need, end the loop
						if (o_CheckpointWrite != null && o_HkWashWrite != null && o_HkGcWrite != null && o_HkChoresWrite != null)
							break;
					} // end loop rows

					// Now fix the vaulues into a structure and send it of to the graph
					Double[] arr = new Double[4];

					arr[0] = new Double(o_CheckpointWrite == null ? "0" : o_CheckpointWrite.toString());
					arr[1] = new Double(o_HkWashWrite     == null ? "0" : o_HkWashWrite    .toString());
					arr[2] = new Double(o_HkGcWrite       == null ? "0" : o_HkGcWrite      .toString());
					arr[3] = new Double(o_HkChoresWrite   == null ? "0" : o_HkChoresWrite  .toString());
					_logger.debug("updateGraphData(ChkptHkGraph): o_CheckpointWrite='"+o_CheckpointWrite+"', o_HkWashWrite='"+o_HkWashWrite+"', o_HkGcWrite='"+o_HkGcWrite+"', o_HkChoresWrite='"+o_HkChoresWrite+"'.");

					// Set the values
					tgdp.setDate(this.getTimestamp());
					tgdp.setData(arr);
				}
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		tmp.setDiffDissColumns( new String[] {"WaitTime"} );
		tmp.addTrendGraphData("ChkptHkGraph", new TrendGraphDataPoint("ChkptHkGraph", new String[] { "Checkpoint Writes", "HK Wash Writes", "HK GC Writes", "HK Chores Writes" }));
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName())
			{
				private static final long	serialVersionUID	= 1L;

				protected JPanel createLocalOptionsPanel()
				{
					JPanel panel = SwingUtils.createPanel("Local Options", true);
					panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

					Configuration conf = Configuration.getInstance(Configuration.TEMP);
					JCheckBox sampleSystemThreads_chk = new JCheckBox("Show system processes", conf == null ? true : conf.getBooleanProperty("CMprocActivity.sample.systemThreads", true));

					sampleSystemThreads_chk.setName("CMprocActivity.sample.systemThreads");
					sampleSystemThreads_chk.setToolTipText("<html>Sample System SPID's that executes in the ASE Server.<br>Note: This is not a filter, you will have to wait for next sample time for this option to take effect.</html>");
					panel.add(sampleSystemThreads_chk, "wrap");

					sampleSystemThreads_chk.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							Configuration conf = Configuration.getInstance(Configuration.TEMP);
							if (conf == null) return;
							conf.setProperty("CMprocActivity.sample.systemThreads", ((JCheckBox)e.getSource()).isSelected());
							conf.save();
						}
					});
					
					return panel;
				}
			};
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_process_activity.png") );
			tcp.setTableToolTipText(
			    "<html>" +
//			        "Double-click a row to get the details for the SPID.<br>" +
//			        "<br>" +
			        "Background colors:" +
			        "<ul>" +
			        "    <li>YELLOW - SPID is a System Processes</li>" +
			        "    <li>GREEN  - SPID is Executing(running) or are in the Run Queue Awaiting a time slot to Execute (runnable)</li>" +
			        "    <li>PINK   - SPID is Blocked by some other SPID that holds a Lock on a database object Table, Page or Row. This is the Lock Victim.</li>" +
			        "    <li>RED    - SPID is Blocking other SPID's from running, this SPID is Responslibe or the Root Cause of a Blocking Lock.</li>" +
			        "</ul>" +
			    "</html>");
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );

			// GRAPH
			TrendGraph chkptHkGraph = new TrendGraph("ChkptHkGraph",
					"Chekpoint and HK Writes", 	// Menu Checkbox text
					"Checkpoint and Housekeeper Writes Per Second", // Label 
					new String[] { "Checkpoint Writes", "HK Wash Writes", "HK GC Writes", "HK Chores Writes" }, 
					false, tmp, false);
			tmp.addTrendGraph("ChkptHkGraph", chkptHkGraph, true);

			// YELLOW = SYSTEM process
			if (conf != null) colorStr = conf.getProperty(name+".color.system");
			tcp.addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					String login = (String) adapter.getValue(adapter.getColumnIndex("Login"));
					if ("".equals(login) || "probe".equals(login))
						return true;
					return false;
				}
			}, SwingUtils.parseColor(colorStr, Color.YELLOW), null));

			// GREEN = RUNNING or RUNNABLE process
			if (conf != null) colorStr = conf.getProperty(name+".color.running");
			tcp.addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					String status = (String) adapter.getValue(adapter.getColumnIndex("status"));
					if ( status != null && (status.startsWith("running") || status.startsWith("runnable")) )
						return true;
					return false;
				}
			}, SwingUtils.parseColor(colorStr, Color.GREEN), null));

			// PINK = spid is BLOCKED by some other user
			if (conf != null) colorStr = conf.getProperty(name+".color.blocked");
			tcp.addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
//					Number blockingSpid = (Number) adapter.getValue(adapter.getColumnIndex("BlockingSPID"));
					Number blockingSpid = (Number) adapter.getValue(adapter.getColumnIndex("BlockingSPID"));
					if ( blockingSpid != null && blockingSpid.intValue() != 0 )
						return true;
					return false;
				}
			}, SwingUtils.parseColor(colorStr, Color.PINK), null));

			// RED = spid is BLOCKING other spids from running
			if (conf != null) colorStr = conf.getProperty(name+".color.blocking");
			tcp.addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				@SuppressWarnings("unchecked")
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					boolean isBlocking                     = false;
					Number  thisSpid                       = (Number) adapter.getValue(adapter.getColumnIndex("SPID"));
					HashMap<Number,Object> blockingSpidMap = (HashMap) adapter.getComponent().getClientProperty("blockingSpidMap");

					if (blockingSpidMap != null && thisSpid != null)
						isBlocking = blockingSpidMap.containsKey(thisSpid);

					if (isBlocking)
					{
						// Check that the SPID is not the victim of another blocked SPID
						Number blockingSpid = (Number) adapter.getValue(adapter.getColumnIndex("BlockingSPID"));
						if ( blockingSpid != null && blockingSpid.intValue() == 0 )
							return true;
					}
					return false;
				}
			}, SwingUtils.parseColor(colorStr, Color.RED), null));
		}

		_CMList.add(tmp);






		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// Databases Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = "CMdbActivity";
		displayName  = "Databases";
		description  = "Various information on a database level.";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 0;
		needCeVersion= 0;
		monTables    = new String[] {"monOpenDatabases"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring=1"};
		colsCalcDiff = new String[] {"AppendLogRequests", "AppendLogWaits"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList<String>();
		     pkList.add("DBName");


		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				true, true)
		{
			private static final long serialVersionUID = 5078336367667465709L;

			@Override
			public void initSql(Connection conn)
			{
				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				if (isClusterEnabled())
				{
					cols1 += "InstanceID, ";
					this.getPk().add("InstanceID");
				}

				String QuiesceTag         = "";
				String SuspendedProcesses = "";
				if (aseVersion >= 12510)
				{
					QuiesceTag         = "QuiesceTag, ";
					SuspendedProcesses = "SuspendedProcesses, ";
				}
				
				cols1 += "DBName, DBID, AppendLogRequests, AppendLogWaits, TransactionLogFull, " + SuspendedProcesses + "BackupInProgress, LastBackupFailed, BackupStartTime, ";
				cols2 += "";
				cols3 += QuiesceTag;
				if (aseVersion >= 15010 || (aseVersion >= 12540 && aseVersion <= 15000) )
				{
				}
				if (aseVersion >= 15025)
				{
					cols2 += "LastTranLogDumpTime, LastCheckpointTime, ";
				}
				String cols = cols1 + cols2 + cols3;
				if (cols.endsWith(", "))
					cols = cols.substring(0, cols.length()-2);

				String sql = 
					"select " + cols + "\n" +
					"from monOpenDatabases \n" +
					"order by DBName \n";

				setSql(sql);
			}
			
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_db_activity.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );
		}

		_CMList.add(tmp);




		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// Databases Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		//
		// TableName                      Description
		// ------------------------------ -------------------------------------------------------------------------------------------------------
		// monTempdbActivity              Provides statistics for all local temporary databases.
		//
		// ColumnName                     TypeName  Description
		// ------------------------------ --------- ---------------------------------------------------------------------------------------------
		// DBID                           int       Unique identifier for the database
		// InstanceID                     tinyint   The Server Instance Identifier (cluster only)
		// AppendLogRequests              int       Number of semaphore requests when attempting to append to the database transaction log
		// AppendLogWaits                 int       Number of times a task had to wait for the append log semaphore to be granted
		// DBName                         varchar   Name of the database in which the object resides
		// LogicalReads                   intn      Total number of buffers read
		// PhysicalReads                  intn      Number of buffers read from disk
		// APFReads                       intn      Number of APF buffers read
		// PagesRead                      intn      Total number of pages read
		// PhysicalWrites                 intn      Total number of buffers written to disk
		// PagesWritten                   intn      Total number of pages written to disk
		// LockRequests                   intn      Number of requests for a lock on the object
		// LockWaits                      intn      Number of times a task waited for a lock for the object
		// CatLockRequests                intn      Number of requests for a lock on the system catalog
		// CatLockWaits                   intn      Number of times a task waited for a lock for the system catalog
		// AssignedCnt                    intn      Number of times assigned to a user task
		// SharableTabCnt                 intn      Number of sharable tables created
		//
		// ParameterName                  TypeName  Description
		// ------------------------------ --------- ---------------------------------------------------------------------------------------------
		// DBID                           int       Unique identifier for the database
		// InstanceID                     tinyint   The Server Instance Identifier (cluster only)
		//

		name         = "CMTmpdbActivity";
		displayName  = "Temp Db";
		description  = "Provides statistics for all local temporary databases.";
		
		needVersion  = 15500;
		needCeVersion= 15020;
//		needVersion  = 15020;
//		needCeVersion= 0;
		monTables    = new String[] {"monTempdbActivity"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring=1", "object lockwait timing=1", "per object statistics active=1"};
		colsCalcDiff = new String[] {"AppendLogRequests", "AppendLogWaits", "LogicalReads", "PhysicalReads", "APFReads", "PagesRead", "PhysicalWrites", "PagesWritten", "LockRequests", "LockWaits", "CatLockRequests", "CatLockWaits", "AssignedCnt", "SharableTabCnt"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList<String>();
	     pkList.add("DBID");


		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				true, true)
		{
			private static final long serialVersionUID = 5078336367667465709L;

			@Override
			public void initSql(Connection conn)
			{
//				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				if (isClusterEnabled())
				{
					cols1 += "InstanceID, ";
					this.getPk().add("InstanceID");
				}

				cols1 += "DBID, DBName, " +
				         "SharableTabCnt, " +
				         "AppendLogRequests, AppendLogWaits, " +
				         "LogicalReads, PhysicalReads, APFReads, " +
				         "PagesRead, PhysicalWrites, PagesWritten, " +
				         "LockRequests, LockWaits, " +
				         "CatLockRequests, CatLockWaits, AssignedCnt";
				cols2 += "";
				cols3 += "";

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from monTempdbActivity \n" +
					"order by DBName \n";

				setSql(sql);
			}
			
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_db_activity.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );
		}

		_CMList.add(tmp);




		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// Waits Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = "CMsysWaitActivity";
		displayName  = "Waits";
		description  = "What different resources are the ASE Server waiting for.";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 0;
		needCeVersion= 0;
		monTables    = new String[] {"monSysWaits", "monWaitEventInfo", "monWaitClassInfo"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring=1", "wait event timing=1"};
		colsCalcDiff = new String[] {"WaitTime", "Waits"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList<String>();
		     pkList.add("WaitEventID");

	
		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				true, true)
		{
			private static final long serialVersionUID = -945885495581439333L;

			@Override
			protected CmSybMessageHandler createSybMessageHandler()
			{
				CmSybMessageHandler msgHandler = super.createSybMessageHandler();

				// If ASE is above 15.0.3 esd#1, and dbcc traceon(3604) is given && 'capture missing stats' is 
				// on the 'CMsysWaitActivity' CM will throw an warning which should NOT be throws...
				//if (getServerVersion() >= 15031) // NOTE this is done early in initialization, so getServerVersion() can't be used
				msgHandler.addDiscardMsgStr("WaitClassID, WaitEventID");

				return msgHandler;
			}

			@Override
			public void initSql(Connection conn)
			{
				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";


				if (isClusterEnabled())
				{
					cols1 += "InstanceID, ";
					this.getPk().add("InstanceID");
				}

				cols1 += "Class=C.Description, Event=I.Description, W.WaitEventID, WaitTime, Waits";
				if (aseVersion >= 15010 || (aseVersion >= 12540 && aseVersion <= 15000) )
				{
				}

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from monSysWaits W, monWaitEventInfo I, monWaitClassInfo C \n" +
					"where W.WaitEventID=I.WaitEventID and I.WaitClassID=C.WaitClassID \n" +
					"order by " + (isClusterEnabled() ? "W.WaitEventID, InstanceID" : "W.WaitEventID") + "\n";

				setSql(sql);
			}
			
		};
		
		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_wait_activity.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );
		}
		
		_CMList.add(tmp);





		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// Engines Activity
		// EngineNumber CurrentKPID PreviousKPID CPUTime     SystemCPUTime UserCPUTime IdleCPUTime Yields      Connections DiskIOChecks DiskIOPolled DiskIOCompleted ProcessesAffinitied ContextSwitches HkgcMaxQSize HkgcPendingItems HkgcHWMItems HkgcOverflows Status               StartTime                      StopTime                       AffinitiedToCPU OSPID       
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = "CMengineActivity";
		displayName  = "Engines";
		description  = "What ASE Server engine is working. In here we can also se what engines are doing/checking for IO's.";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 0;
		needCeVersion= 0;
		monTables    = new String[] {"monEngine"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring=1"};
		colsCalcDiff = new String[] {"CPUTime", "SystemCPUTime", "UserCPUTime", "IdleCPUTime", "IOCPUTime", "Yields", "DiskIOChecks", "DiskIOPolled", "DiskIOCompleted", "ContextSwitches", "HkgcPendingItems", "HkgcOverflows"};
		colsCalcPCT  = new String[] {"CPUTime", "SystemCPUTime", "UserCPUTime", "IdleCPUTime", "IOCPUTime"};
		pkList       = new LinkedList<String>();
		     pkList.add("EngineNumber");


		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				true, true)
		// OVERRIDE SOME DEFAULT METHODS
		{
			private static final long serialVersionUID = 3975695722601723795L;

			@Override
			public void initSql(Connection conn)
			{
				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				if (isClusterEnabled())
				{
					cols1 += "InstanceID, ";
					this.getPk().add("InstanceID");
				}

				cols1 += "EngineNumber, CurrentKPID, PreviousKPID, CPUTime, SystemCPUTime, UserCPUTime, ";
				if (aseVersion >= 15500 || (aseVersion >= 15030 && isClusterEnabled()) )
					cols1 += "IOCPUTime, ";
				cols1 += "IdleCPUTime, ContextSwitches, Connections, ";

				cols2 += "";
				cols3 += "ProcessesAffinitied, Status, StartTime, StopTime, AffinitiedToCPU, OSPID";

				if (aseVersion >= 12532)
				{
					cols2 += "Yields, DiskIOChecks, DiskIOPolled, DiskIOCompleted, ";
				}
				if (aseVersion >= 15025)
				{
					cols2 += "MaxOutstandingIOs, ";
				}
				if (aseVersion >= 15000)
				{
					cols2 += "HkgcMaxQSize, HkgcPendingItems, HkgcHWMItems, HkgcOverflows, ";
				}

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from monEngine \n" +
					"where Status = 'online' \n" +
					"order by 1,2\n";

				setSql(sql);
			}

			@Override
			public void updateGraphData(TrendGraphDataPoint tgdp)
			{
				int aseVersion = getServerVersion();
				// NOTE: IOCPUTime was introduced in ASE 15.5

				if ("cpuSum".equals(tgdp.getName()))
				{
					Double[] dataArray  = new Double[3];
					String[] labelArray = new String[3];
					if (aseVersion >= 15500)
					{
						dataArray  = new Double[4];
						labelArray = new String[4];
					}

					labelArray[0] = "System+User CPU";
					labelArray[1] = "System CPU";
					labelArray[2] = "User CPU";

					dataArray[0] = this.getDiffValueAvg("CPUTime");
					dataArray[1] = this.getDiffValueAvg("SystemCPUTime");
					dataArray[2] = this.getDiffValueAvg("UserCPUTime");

					if (aseVersion >= 15500)
					{
						labelArray[0] = "System+User+IO CPU";

						dataArray[3]  = this.getDiffValueAvg("IOCPUTime");
						labelArray[3] = "IO CPU";

						_logger.debug("updateGraphData(cpuSum): CPUTime='"+dataArray[0]+"', SystemCPUTime='"+dataArray[1]+"', UserCPUTime='"+dataArray[2]+"', IoCPUTime='"+dataArray[3]+"'.");
					}
					else
						_logger.debug("updateGraphData(cpuSum): CPUTime='"+dataArray[0]+"', SystemCPUTime='"+dataArray[1]+"', UserCPUTime='"+dataArray[2]+"'.");

					// Set the values
					tgdp.setDate(this.getTimestamp());
					tgdp.setLabel(labelArray);
					tgdp.setData(dataArray);
				}
				if ("cpuEng".equals(tgdp.getName()))
				{
					// Set label on the TrendGraph if we are above 15.5
					if (aseVersion >= 15500)
					{
						TrendGraph tg = getTrendGraph(tgdp.getName());
						if (tg != null)
							tg.setLabel("CPU Usage per Engine (System + User + IO)");
					}
					
					Double[] engCpuArray = new Double[this.size()];
					String[] engNumArray = new String[engCpuArray.length];
					for (int i = 0; i < engCpuArray.length; i++)
					{
						String instanceId   = null;
						if (isClusterEnabled())
							instanceId = this.getAbsString(i, "InstanceID");
						String engineNumber = this.getAbsString(i, "EngineNumber");

						engCpuArray[i] = this.getDiffValueAsDouble(i, "CPUTime");
						if (instanceId == null)
							engNumArray[i] = "eng-" + engineNumber;
						else
							engNumArray[i] = "eng-" + instanceId + ":" + engineNumber;
						
						// in Cluster Edition the labels will look like 'eng-{InstanceId}:{EngineNumber}'
						// in ASE SMP Version the labels will look like 'eng-{EngineNumber}'
					}

					// Set the values
					tgdp.setDate(this.getTimestamp());
					tgdp.setLabel(engNumArray);
					tgdp.setData(engCpuArray);
				}
			}


			/** 
			 * Compute the CPU times in pct instead of numbers of usage seconds since last sample
			 */
			@Override
			public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
			{
				int aseVersion = getServerVersion();
				// NOTE: IOCPUTime was introduced in ASE 15.5
				
				// Compute the avgServ column, which is IOTime/(Reads+APFReads+Writes)
				int CPUTime,       SystemCPUTime,       UserCPUTime,       IOCPUTime,       IdleCPUTime;
				int CPUTimeId = 0, SystemCPUTimeId = 0, UserCPUTimeId = 0, IOCPUTimeId = 0, IdleCPUTimeId = 0;
			
				// Find column Id's
				List<String> colNames = diffData.getColNames();
				if (colNames==null) return;
			
				for (int colId=0; colId < colNames.size(); colId++) 
				{
					String colName = (String)colNames.get(colId);
					if (colName.equals("CPUTime"))       CPUTimeId       = colId;
					if (colName.equals("SystemCPUTime")) SystemCPUTimeId = colId;
					if (colName.equals("UserCPUTime"))   UserCPUTimeId   = colId;
					if (colName.equals("IOCPUTime"))     IOCPUTimeId     = colId;
					if (colName.equals("IdleCPUTime"))   IdleCPUTimeId   = colId;
				}
			
				// Loop on all diffData rows
				for (int rowId=0; rowId < diffData.getRowCount(); rowId++) 
				{
					CPUTime	=       ((Number)diffData.getValueAt(rowId, CPUTimeId      )).intValue();
					SystemCPUTime = ((Number)diffData.getValueAt(rowId, SystemCPUTimeId)).intValue();
					UserCPUTime =   ((Number)diffData.getValueAt(rowId, UserCPUTimeId  )).intValue();
					IdleCPUTime =   ((Number)diffData.getValueAt(rowId, IdleCPUTimeId  )).intValue();

					IOCPUTime = 0;
					if (aseVersion >= 15500)
					{
						IOCPUTime =   ((Number)diffData.getValueAt(rowId, IOCPUTimeId  )).intValue();
					}

					if (_logger.isDebugEnabled())
						_logger.debug("----CPUTime = "+CPUTime+", SystemCPUTime = "+SystemCPUTime+", UserCPUTime = "+UserCPUTime+", IOCPUTime = "+IOCPUTime+", IdleCPUTime = "+IdleCPUTime);

					// Handle divided by 0... (this happens if a engine goes offline
					BigDecimal calcCPUTime       = null;
					BigDecimal calcSystemCPUTime = null;
					BigDecimal calcUserCPUTime   = null;
					BigDecimal calcIoCPUTime     = null;
					BigDecimal calcIdleCPUTime   = null;

					if( CPUTime == 0 )
					{
						calcCPUTime       = new BigDecimal( 0 );
						calcSystemCPUTime = new BigDecimal( 0 );
						calcUserCPUTime   = new BigDecimal( 0 );
						calcIoCPUTime     = new BigDecimal( 0 );
						calcIdleCPUTime   = new BigDecimal( 0 );
					}
					else
					{
						int sumSystemUserIo = SystemCPUTime + UserCPUTime + IOCPUTime;
						calcCPUTime       = new BigDecimal( ((1.0 * sumSystemUserIo) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
						calcSystemCPUTime = new BigDecimal( ((1.0 * SystemCPUTime  ) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
						calcUserCPUTime   = new BigDecimal( ((1.0 * UserCPUTime    ) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
						calcIoCPUTime     = new BigDecimal( ((1.0 * IOCPUTime      ) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
						calcIdleCPUTime   = new BigDecimal( ((1.0 * IdleCPUTime    ) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
					}

					if (_logger.isDebugEnabled())
						_logger.debug("++++CPUTime = "+calcCPUTime+", SystemCPUTime = "+calcSystemCPUTime+", UserCPUTime = "+calcUserCPUTime+", IoCPUTime = "+calcIoCPUTime+", IdleCPUTime = "+calcIdleCPUTime);
			
					diffData.setValueAt(calcCPUTime,       rowId, CPUTimeId       );
					diffData.setValueAt(calcSystemCPUTime, rowId, SystemCPUTimeId );
					diffData.setValueAt(calcUserCPUTime,   rowId, UserCPUTimeId   );
					diffData.setValueAt(calcIdleCPUTime,   rowId, IdleCPUTimeId   );
			
					if (aseVersion >= 15500)
					{
						diffData.setValueAt(calcIoCPUTime, rowId, IOCPUTimeId);
					}
				}
			}
		};
		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		tmp.addTrendGraphData("cpuSum", new TrendGraphDataPoint("cpuSum", new String[] { "System+User CPU", "System CPU", "User CPU" }));
		tmp.addTrendGraphData("cpuEng", new TrendGraphDataPoint("cpuEng", new String[] { "eng-0" }));
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_engine_activity.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );

			// GRAPH
			TrendGraph tgCpuSum = new TrendGraph("cpuSum",
					"CPU Summary", 	// Menu Checkbox text
					"CPU Summary for all Engines (using monEngine)", // Label 
					new String[] { "System+User CPU", "System CPU", "User CPU" }, 
					true, tmp, true);
			tmp.addTrendGraph("cpuSum", tgCpuSum, true);

			TrendGraph tgCpuEng = new TrendGraph("cpuEng",
					"CPU per Engine", // Menu Checkbox text
					"CPU Usage per Engine (System + User)", // Label 
					new String[] { "eng-0" }, 
					true, tmp, true);
			tmp.addTrendGraph("cpuEng", tgCpuEng, true);
		}
		
		_CMList.add(tmp);





		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// System LOAD
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = "CMSysLoad";
		displayName  = "System Load";
		description  = "<html>" +
		                   "The SYSTEM load.<br>" +
		                   "Here you can see the balancing between engines<br>" +
		                   "Check 'run queue length', which is almost the same as 'load average' on Unix systems.<br>" +
		                   "Meaning: How many threads/SPIDs are currently waiting on the run queue before they could be scheduled for execution." +
		               "</html>";
		
		needVersion  = 15500; // monSysLoad is new in 15.5
		needCeVersion= 15020;
		monTables    = new String[] {"monSysLoad"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {""};
		colsCalcDiff = new String[] {};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList<String>();
		    pkList.add("StatisticID");
		    pkList.add("EngineNumber");

		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				false, true)
		{
			private static final long serialVersionUID = -1842749890597642593L;

			@Override
			public void initSql(Connection conn)
			{
				//int aseVersion = getServerVersion();

				String cols1 = "";
				if (isClusterEnabled())
				{
					cols1 += "InstanceID, ";
					this.getPk().add("InstanceID");
				}

				String sql = 
					"select "+cols1+"StatisticID, Statistic, EngineNumber, \n" +
					"       Sample, \n" +
					"       Avg_1min, Avg_5min, Avg_15min, " +
					"       SteadyState, \n" +
					"       Peak_Time, Peak, \n" +
					"       Max_1min_Time,  Max_1min, \n" + // try add a SQl that shows number of minutes ago this happened, "Days-HH:MM:SS
					"       Max_5min_Time,  Max_5min, \n" +
					"       Max_15min_Time, Max_15min \n" +
					"from monSysLoad \n" +
					"order by StatisticID, EngineNumber" + (isClusterEnabled() ? ", InstanceID" : "");

				setSql(sql);
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_sysload_activity.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );
		}
		
		_CMList.add(tmp);



		
		
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// DataCaches Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = "CMcacheActivity";
		displayName  = "Data Caches";
		description  = "What (user defined) data caches have we got and how many 'chache misses' goes out to disk...";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 0;
		needCeVersion= 0;
		monTables    = new String[] {"monDataCache"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring=1"};
		colsCalcDiff = new String[] {"CacheHitRate", "CacheSearches", "PhysicalReads", "LogicalReads", "PhysicalWrites", "Stalls"};
		colsCalcPCT  = new String[] {"CacheHitRate"};
		pkList       = new LinkedList<String>();
		     pkList.add("CacheName");

		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				true, true)
		// OVERRIDE SOME DEFAULT METHODS
		{
			private static final long serialVersionUID = -2185972180915326688L;

			@Override
			public void initSql(Connection conn)
			{
				try 
				{
					MonTablesDictionary mtd = MonTablesDictionary.getInstance();
					mtd.addColumn("monDataCache",  "Stalls",       "Number of times I/O operations were delayed because no clean buffers were available in the wash area");

					mtd.addColumn("monDataCache",  "CacheHitRate", "<html>" +
					                                               "Percent calculation of how many pages was fetched from the cache.<br>" +
					                                               "Note: APF reads could already be in memory, counted as a 'cache hit', check also 'devices' and APFReads.<br>" +
					                                               "Formula: 100 - (PhysicalReads/CacheSearches) * 100.0" +
					                                               "</html>");
//					mtd.addColumn("monDataCache",  "Misses",       "fixme");
//					mtd.addColumn("monDataCache",  "Volatility",   "fixme");
				}
				catch (NameNotFoundException e) {/*ignore*/}

				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				// 1> select * from monTableColumns where TableName = 'monDataCache'
				// 2> go
				// ColumnName          TypeName  Description
				// ------------------- --------- --------------------------------------------------------------
				// CacheID             int       Unique identifier for the cache
				// RelaxedReplacement  int       Whether the cache is using Relaxed cached replacement strategy
				// BufferPools         int       The number of buffer pools within the cache
				// CacheSearches       int       Cache searches directed to the cache
				// PhysicalReads       int       Number of buffers read into the cache from disk
				// LogicalReads        int       Number of buffers retrieved from the cache
				// PhysicalWrites      int       Number of buffers written from the cache to disk
				// Stalls              int       Number of 'dirty' buffer retrievals
				// CachePartitions     smallint  Number of partitions currently configured for the cache
				// CacheName           varchar   Name of the cache
				// 
				//------------- NEEDS TO BE CALCULATED AFTER EACH SAMPLE
				// HitRate    = CacheSearches  / Logical Reads
				// Misses     = CacheSearches  / Physical Reads
				// Volatility = PhysicalWrites / (PhysicalReads + LogicalReads)


				
				if (isClusterEnabled())
				{
					cols1 += "InstanceID, ";
					this.getPk().add("InstanceID");
				}

				cols1 += "CacheName, CacheID, RelaxedReplacement, CachePartitions, BufferPools" +
				         ", CacheSearches, PhysicalReads, LogicalReads, PhysicalWrites, Stalls" +
				         ", CacheHitRate = convert(numeric(10,1), 100 - (PhysicalReads*1.0/(CacheSearches+1)) * 100.0)" +
//				         ", HitRate    = convert(numeric(10,1), (CacheSearches * 1.0 / LogicalReads) * 100)" +
//				         ", Misses     = convert(numeric(10,1), (CacheSearches * 1.0 / PhysicalReads) * 1)" +
//				         ", Volatility = convert(numeric(10,1), PhysicalWrites * 1.0 / (PhysicalReads + LogicalReads)* 1)"
				         "";
				if (aseVersion >= 15010 || (aseVersion >= 12540 && aseVersion <= 15000) )
				{
				}

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from monDataCache \n" +
					"order by 1,2\n";

				setSql(sql);
			}

			@Override
			public void updateGraphData(TrendGraphDataPoint tgdp)
			{
				if ("CacheGraph".equals(tgdp.getName()))
				{
					Double[] arr = new Double[3];

					arr[0] = this.getRateValueSum("LogicalReads");
					arr[1] = this.getRateValueSum("PhysicalReads");
					arr[2] = this.getRateValueSum("PhysicalWrites");
					_logger.debug("updateGraphData(CacheGraph): LogicalReads='"+arr[0]+"', PhysicalReads='"+arr[1]+"', PhysicalWrites='"+arr[2]+"'.");

					// Set the values
					tgdp.setDate(this.getTimestamp());
					tgdp.setData(arr);
				}
			}
			/** 
			 * Compute 
			 */
			@Override
			public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
			{
//				int CacheSearches,       LogicalReads,       PhysicalReads,       PhysicalWrites;
//				int CacheSearchesId = 0, LogicalReadsId = 0, PhysicalReadsId = 0, PhysicalWritesId = 0;
//	
//				int CacheHitRateId = 0, MissesId = 0, VolatilityId = 0;
			
				int CacheSearches,       PhysicalReads;
				int CacheSearchesId = 0, PhysicalReadsId = 0;
	
				int CacheHitRateId = 0;

				// Find column Id's
				List<String> colNames = diffData.getColNames();
				if (colNames==null) return;
			
				for (int colId=0; colId < colNames.size(); colId++) 
				{
					String colName = (String)colNames.get(colId);
					if (colName.equals("CacheSearches"))  CacheSearchesId   = colId;
//					if (colName.equals("LogicalReads"))   LogicalReadsId    = colId;
					if (colName.equals("PhysicalReads"))  PhysicalReadsId   = colId;
//					if (colName.equals("PhysicalWrites")) PhysicalWritesId  = colId;

					if (colName.equals("CacheHitRate"))   CacheHitRateId    = colId;
//					if (colName.equals("Misses"))         MissesId          = colId;
//					if (colName.equals("Volatility"))     VolatilityId      = colId;
				}
			
				// Loop on all diffData rows
				for (int rowId=0; rowId < diffData.getRowCount(); rowId++) 
				{
					CacheSearches  = ((Number)diffData.getValueAt(rowId, CacheSearchesId )).intValue();
//					LogicalReads   = ((Number)diffData.getValueAt(rowId, LogicalReadsId  )).intValue();
					PhysicalReads  = ((Number)diffData.getValueAt(rowId, PhysicalReadsId )).intValue();
//					PhysicalWrites = ((Number)diffData.getValueAt(rowId, PhysicalWritesId)).intValue();

//					CacheHitRate   = ((Number)diffData.getValueAt(rowId, CacheHitRateId  )).intValue();
//					Misses         = ((Number)diffData.getValueAt(rowId, MissesId        )).intValue();
//					Volatility     = ((Number)diffData.getValueAt(rowId, VolatilityId    )).intValue();

//					if (_logger.isDebugEnabled())
//						_logger.debug("----CacheSearches = "+CacheSearches+", LogicalReads = "+LogicalReads+", PhysicalReads = "+PhysicalReads+", PhysicalWrites = "+PhysicalWrites);
					if (_logger.isDebugEnabled())
						_logger.debug("----CacheSearches = "+CacheSearches+", PhysicalReads = "+PhysicalReads);

					// Handle divided by 0... (this happens if a engine goes offline
					BigDecimal calcCacheHitRate = null;
//					BigDecimal calcMisses       = null;
//					BigDecimal calcVolatility   = null;

//					", CacheHitRate    = convert(numeric(4,1), 100 - (PhysicalReads*1.0/CacheSearches) * 100.0)" +
//					", CacheHitRate    = convert(numeric(4,1), (CacheSearches * 1.0 / LogicalReads) * 100)" +
//					", Misses     = CacheSearches  * 1.0 / PhysicalReads" +
//					", Volatility = PhysicalWrites * 1.0 / (PhysicalReads + LogicalReads)";

					if (CacheSearches == 0)
						calcCacheHitRate    = new BigDecimal( 0 );
					else
						calcCacheHitRate    = new BigDecimal( 100.0 - (PhysicalReads*1.0/CacheSearches) * 100.0 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//						calcCacheHitRate    = new BigDecimal( ((1.0 * CacheSearches) / LogicalReads) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
					
//					if (PhysicalReads == 0)
//						calcMisses     = new BigDecimal( 0 );
//					else
//						calcMisses     = new BigDecimal( ((1.0 * CacheSearches) / PhysicalReads) * 1 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//					
//					if ((PhysicalReads + LogicalReads) == 0)
//						calcVolatility = new BigDecimal( 0 );
//					else
//						calcVolatility = new BigDecimal( ((1.0 * PhysicalWrites) / (PhysicalReads + LogicalReads)) * 1 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//
//					if (_logger.isDebugEnabled())
//						_logger.debug("++++CacheHitRate = "+calcCacheHitRate+", Misses = "+calcMisses+", Volatility = "+calcVolatility);
					
					if (_logger.isDebugEnabled())
						_logger.debug("++++CacheHitRate = "+calcCacheHitRate);
			
					diffData.setValueAt(calcCacheHitRate, rowId, CacheHitRateId );
//					diffData.setValueAt(calcMisses,       rowId, MissesId       );
//					diffData.setValueAt(calcVolatility,   rowId, VolatilityId   );

				}
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		tmp.addTrendGraphData("CacheGraph", new TrendGraphDataPoint("CacheGraph", new String[] { "Logical Reads", "Physical Reads", "Writes" }));
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_cache_activity.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );

			// GRAPH
			TrendGraph tg = new TrendGraph("CacheGraph",
					// Menu Checkbox text
					"Data Caches Activity",
					// Label 
					"Activity for All Data Caches per Second", // Label 
					new String[] { "Logical Reads", "Physical Reads", "Writes" }, 
					false, tmp, true);
			tmp.addTrendGraph("CacheGraph", tg, true);
		}

		_CMList.add(tmp);
		



		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// Pools Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = "CMpoolActivity";
		displayName  = "Pools";
		description  = "The cahces has 2K or 16K pools, how are they behaving?";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 0;
		needCeVersion= 0;
		monTables    = new String[] {"monCachePool"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring=1"};
		colsCalcDiff = new String[] {"PagesRead", "PhysicalReads", "Stalls", "PagesTouched", "BuffersToMRU", "BuffersToLRU"};
		colsCalcPCT  = new String[] {"CacheUtilization", "CacheEfficiency"};
		pkList       = new LinkedList<String>();
		     pkList.add("CacheName");
		     pkList.add("IOBufferSize");


		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				true, true)
		{
			private static final long serialVersionUID = -6929175820005726806L;

			@Override
			public void initSql(Connection conn)
			{
				try 
				{
					MonTablesDictionary mtd = MonTablesDictionary.getInstance();
					mtd.addColumn("monCachePool",  "Stalls",         "Number of times I/O operations were delayed because no clean buffers were available in the wash area");

					mtd.addColumn("monCachePool",  "SrvPageSize",    "ASE Servers page size (@@maxpagesize)");
					mtd.addColumn("monCachePool",  "PagesPerIO",     "This pools page size (1=SinglePage, 2=, 4=, 8=Extent IO)");
					mtd.addColumn("monCachePool",  "AllocatedPages", "Number of actual pages allocated to this pool. same as 'AllocatedKB' but in pages instead of KB.");
					
					mtd.addColumn("monCachePool",  "CacheUtilization", "<html>" +
					                                                       "If not 100% the cache has to much memory allocated to it.<br>" +
					                                                       "Formula: abs.PagesTouched / abs.AllocatedPages * 100<br>" +
					                                                   "</html>");
					mtd.addColumn("monCachePool",  "CacheEfficiency",  "<html>" +
					                                                       "If less than 100, the cache is to small (pages has been flushed ou from the cache).<br> " +
					                                                       "Pages are read in from the disk, could be by APF Reads (so cacheHitRate is high) but the pages still had to be read from disk.<br>" +
					                                                       "Formula: abs.AllocatedPages / diff.PagesRead * 100<br>" +
					                                                   "</html>");
				}
				catch (NameNotFoundException e) {/*ignore*/}

				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				// 1> select * from monTableColumns where TableName = 'monCachePool'
				// 2> go
				// ColumnName      TypeName  Description
				// --------------- --------- ------------------------------------------------------------------------------------------------------------------------
				// CacheID         int       Unique identifier for the cache
				// IOBufferSize    int       Size (in bytes) of the I/O buffer for the pool
				// AllocatedKB     int       Number of kilobytes that have been allocated for the pool
				// PhysicalReads   int       The number of buffers that have been read from disk into the pool
				// Stalls          int       Number of 'dirty' buffer retrievals
				// PagesTouched    int       Number of pages used within the pool
				// PagesRead       int       Number of pages read into the pool
				// BuffersToMRU    int       The number of buffers that were fetched and re-placed at the most recently used portion of the pool
				// BuffersToLRU    int       The number of buffers that were fetched and re-placed at the least recently used portion of the pool: fetch-and-discard
				// CacheName       varchar   Name of the cache

				//------------- NEEDS TO BE CALCULATED AFTER EACH SAMPLE
				// CacheUsage%      = AllocatedKB / (PagesTouched * @@maxpagesize)
				// CacheEfficiency% = PagesRead   / (PagesTouched * @@mxapagesize)

				if (isClusterEnabled())
				{
					cols1 += "InstanceID, ";
					this.getPk().add("InstanceID");
				}

				cols1 += "CacheName, CacheID \n" +
				         ", SrvPageSize = @@maxpagesize \n" +
				         ", IOBufferSize \n" +
				         ", PagesPerIO = IOBufferSize/@@maxpagesize \n" +
				         ", AllocatedKB \n" +
				         ", AllocatedPages = convert(int,AllocatedKB*(1024.0/@@maxpagesize)) \n" +
				         ", PagesRead, PhysicalReads, Stalls, PagesTouched, BuffersToMRU, BuffersToLRU \n" +
				         ", CacheUtilization = convert(numeric(12,1), PagesTouched / (AllocatedKB*(1024.0/@@maxpagesize)) * 100.0) \n" +
				         ", CacheEfficiency  = CASE WHEN PagesRead > 0 THEN convert(numeric(12,1), (AllocatedKB*(1024.0/@@maxpagesize)) / PagesRead    * 100.0) ELSE 0.0 END\n" +
				         "";
				if (aseVersion >= 15010 || (aseVersion >= 12540 && aseVersion <= 15000) )
				{
				}

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from monCachePool \n" +
					"order by CacheName, IOBufferSize\n";

				setSql(sql);
			}
			
			/** 
			 * Compute 
			 */
			@Override
			public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
			{
				int AllocatedKB,       PagesTouched,       PagesRead,       SrvPageSize;
				int AllocatedKBId = 0, PagesTouchedId = 0, PagesReadId = 0, SrvPageSizeId = 0;
	
				int CacheUtilizationId = 0, CacheEfficiencyId = 0;
			
				// Find column Id's
				List<String> colNames = diffData.getColNames();
				if (colNames==null) return;
			
				for (int colId=0; colId < colNames.size(); colId++) 
				{
					String colName = (String)colNames.get(colId);
					if (colName.equals("AllocatedKB"))        AllocatedKBId        = colId;
					if (colName.equals("PagesTouched"))       PagesTouchedId       = colId;
					if (colName.equals("PagesRead"))          PagesReadId          = colId;
					if (colName.equals("SrvPageSize"))        SrvPageSizeId        = colId;

					if (colName.equals("CacheUtilization"))   CacheUtilizationId   = colId;
					if (colName.equals("CacheEfficiency"))    CacheEfficiencyId    = colId;
				}
			
				// Loop on all diffData rows
				for (int rowId=0; rowId < diffData.getRowCount(); rowId++) 
				{
					AllocatedKB  = ((Number)newSample.getValueAt(rowId, AllocatedKBId )).intValue();
					PagesTouched = ((Number)newSample.getValueAt(rowId, PagesTouchedId)).intValue();
					PagesRead    = ((Number)diffData .getValueAt(rowId, PagesReadId   )).intValue();
					SrvPageSize  = ((Number)newSample.getValueAt(rowId, SrvPageSizeId )).intValue();

					if (_logger.isDebugEnabled())
						_logger.debug("----AllocatedKB = "+AllocatedKB+", PagesTouched = "+PagesTouched+", PagesRead = "+PagesRead+", SrvPageSize = "+SrvPageSize);

					// Handle divided by 0... (this happens if a engine goes offline
					BigDecimal calcCacheUtilization = null;
					BigDecimal calcCacheEfficiency  = null;

//					", CacheUtilization = convert(numeric( 4,1), PagesTouched / (AllocatedKB*(1024.0/@@maxpagesize)) * 100.0)" +
//					", CacheEfficiency  = convert(numeric(12,1), (AllocatedKB*(1024.0/@@maxpagesize)) / PagesRead    * 100.0)" +

					if( AllocatedKB == 0 )
					{
						calcCacheUtilization = new BigDecimal( 0 );
						calcCacheEfficiency  = new BigDecimal( 0 );
					}
					else
					{
						double dCacheUtilization = PagesTouched / (AllocatedKB*(1024.0/SrvPageSize)) * 100.0;
						double dCacheEfficiency  = (AllocatedKB*(1024.0/SrvPageSize))    / PagesRead * 100.0;

						if ( dCacheEfficiency > 100.0 )
							dCacheEfficiency = 100.0;

						calcCacheUtilization = new BigDecimal( dCacheUtilization ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
						calcCacheEfficiency  = new BigDecimal( dCacheEfficiency  ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
					}

					if (_logger.isDebugEnabled())
						_logger.debug("++++calcCacheUtilization = "+calcCacheUtilization+", calcCacheEfficiency = "+calcCacheEfficiency);
			
					diffData.setValueAt(calcCacheUtilization, rowId, CacheUtilizationId );
					diffData.setValueAt(calcCacheEfficiency,  rowId, CacheEfficiencyId  );
				}
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_pool_activity.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );
		}

		_CMList.add(tmp);





		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// Devices Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = "CMdeviceActivity";
		displayName  = "Devices";
		description  = "<html><p>What devices are doing IO's and what's the approximare service time on the disk.</p>" +
				"Do not trust the service time <b>too</b> much...</html>";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 0;
		needCeVersion= 0;
		monTables    = new String[] {"monDeviceIO"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring=1"};
		colsCalcDiff = new String[] {"Reads", "APFReads", "Writes", "DevSemaphoreRequests", "DevSemaphoreWaits", "IOTime"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList<String>();
		     pkList.add("LogicalName");

		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				true, true)
		// OVERRIDE SOME DEFAULT METHODS
		{
			private static final long serialVersionUID = 688571813560006014L;

			@Override
			public void initSql(Connection conn)
			{
				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				if (isClusterEnabled())
				{
					cols1 += "InstanceID, ";
					this.getPk().add("InstanceID");
				}

				cols1 += "LogicalName, Reads, APFReads, Writes, DevSemaphoreRequests, DevSemaphoreWaits, IOTime, \n";
//				cols2 += "AvgServ_ms = convert(numeric(10,1),null)";
				cols2 += "AvgServ_ms = \n" +
						 "case \n" +
						 "  when Reads+Writes>0 then convert(numeric(10,1), IOTime/convert(numeric(10,0),Reads+Writes)) \n" +
						 "  else                     convert(numeric(10,1), null) \n" +
						 "end";
				cols3 += ", PhysicalName";
				if (aseVersion >= 15010 || (aseVersion >= 12540 && aseVersion <= 15000) )
				{
				}

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from monDeviceIO\n" +
					"order by LogicalName" + (isClusterEnabled() ? ", InstanceID" : "") + "\n";

				setSql(sql);
			}
				
			/** 
			 * Compute the avgServ column, which is IOTime/(Reads+Writes)
			 */
			@Override
			public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
			{
				double AvgServ_ms;
				//int Reads, APFReads, Writes, IOTime;
				//int ReadsColId = 0, APFReadsColId = 0, WritesColId = 0, IOTimeColId = 0, AvgServ_msColId = 0;
				int Reads, Writes, IOTime;
				int ReadsColId = 0, WritesColId = 0, IOTimeColId = 0, AvgServ_msColId = 0;

				// Find column Id's
				List<String> colNames = diffData.getColNames();
				if (colNames == null)
					return;
				for (int colId = 0; colId < colNames.size(); colId++)
				{
					String colName = (String) colNames.get(colId);
					if (colName.equals("Reads"))      ReadsColId      = colId;
					if (colName.equals("Writes"))     WritesColId     = colId;
					if (colName.equals("IOTime"))     IOTimeColId     = colId;
					if (colName.equals("AvgServ_ms")) AvgServ_msColId = colId;
				}

				// Loop on all diffData rows
				for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
				{
					Reads    = ((Number) diffData.getValueAt(rowId, ReadsColId))   .intValue();
					Writes   = ((Number) diffData.getValueAt(rowId, WritesColId))  .intValue();
					IOTime   = ((Number) diffData.getValueAt(rowId, IOTimeColId))  .intValue();

					// int totIo = Reads + APFReads + Writes;
					int totIo = Reads + Writes;
					if (totIo != 0)
					{
						// AvgServ_ms = (IOTime * 1000) / ( totIo);
						AvgServ_ms = IOTime / totIo;

						// Keep only 3 decimals
						// row.set(AvgServ_msColId, new Double (AvgServ_ms/1000) );
						BigDecimal newVal = new BigDecimal(AvgServ_ms);
						diffData.setValueAt(newVal, rowId, AvgServ_msColId);
					}
					else
						diffData.setValueAt(new BigDecimal(0), rowId, AvgServ_msColId);
				}
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_device_activity.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );
		}

		_CMList.add(tmp);
		




		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// IOQueue SUMMARY Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = "CMioQueueSumAct";
		displayName  = "IO Sum";
		description  = "<html><p>A <b>Summary</b> of how many IO's have the ASE Server done on various segments (UserDb/Tempdb/System)</p>" +
				"For ASE 15.0.2 or so, we will have 'System' segment covered aswell.<br>" +
				"Do not trust the service time <b>too</b> much...</html>";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 0;
		needCeVersion= 0;
		monTables    = new String[] {"monIOQueue"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring=1"};
		colsCalcDiff = new String[] {"IOs", "IOTime"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList<String>();
		     pkList.add("IOType");

		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				true, true)
		// OVERRIDE SOME DEFAULT METHODS
		{ 
			private static final long serialVersionUID = -8676587973889879799L;

			@Override
			public void initSql(Connection conn)
			{
				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				cols1 = "IOType, " +
				        "IOs        = sum(convert(numeric(18,0), IOs)), " +
				        "IOTime     = sum(convert(numeric(18,0), IOTime)), " +
//				        "AvgServ_ms = convert(numeric(10,1), null)";
				        "AvgServ_ms = \n" +
				        "case \n" +
				        "  when sum(convert(numeric(18,0), IOs)) > 0 then convert(numeric(18,1), sum(convert(numeric(18,0), IOTime))/sum(convert(numeric(18,0), IOs))) \n" +
				        "  else                                           convert(numeric(18,1), null) \n" +
				        "end";
				if (aseVersion >= 15010 || (aseVersion >= 12540 && aseVersion <= 15000) )
				{
				}

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from monIOQueue \n" +
					"group by IOType \n" +
					"order by 1\n";

				setSql(sql);
			}

			@Override
			public void updateGraphData(TrendGraphDataPoint tgdp)
			{
				if ("diskIo".equals(tgdp.getName()))
				{
					Double[] arr = new Double[5];
					arr[0] = this.getRateValue("User Data",   "IOs");
					arr[1] = this.getRateValue("User Log",    "IOs");
					arr[2] = this.getRateValue("Tempdb Data", "IOs");
					arr[3] = this.getRateValue("Tempdb Log",  "IOs");
					arr[4] = this.getRateValue("System",      "IOs");
					_logger.debug("updateGraphData(diskIo): User Data='"+arr[0]+"', User Log='"+arr[1]+"', Tempdb Data='"+arr[2]+"', Tempdb Log='"+arr[3]+"', System='"+arr[4]+"'.");

					// Set the values
					tgdp.setDate(this.getTimestamp());
					tgdp.setData(arr);
				}
			}
			@Override
			public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
			{
				double AvgServ_ms;
				long   IOs, IOTime;
				int    IOsColId = 0, IOTimeColId = 0, AvgServ_msColId = 0;

				// Find column Id's
				List<String> colNames = diffData.getColNames();
				if (colNames == null)
					return;
				for (int colId = 0; colId < colNames.size(); colId++)
				{
					String colName = (String) colNames.get(colId);
					if (colName.equals("IOs"))        IOsColId        = colId;
					if (colName.equals("IOTime"))     IOTimeColId     = colId;
					if (colName.equals("AvgServ_ms")) AvgServ_msColId = colId;
				}

				// Loop on all diffData rows
				for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
				{
					IOs    = ((Number) diffData.getValueAt(rowId, IOsColId))   .longValue();
					IOTime = ((Number) diffData.getValueAt(rowId, IOTimeColId)).longValue();

					if (IOs != 0)
					{
						AvgServ_ms = IOTime / IOs;
						BigDecimal newVal = new BigDecimal(AvgServ_ms);
						diffData.setValueAt(newVal, rowId, AvgServ_msColId);
					}
					else
						diffData.setValueAt(new BigDecimal(0), rowId, AvgServ_msColId);
				}
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		tmp.addTrendGraphData("diskIo", new TrendGraphDataPoint("diskIo", new String[] { "User Data", "User Log", "Tempdb Data", "Tempdb Log", "System" }));
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_io_queue_sum_activity.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );

			// GRAPH
			TrendGraph tg = new TrendGraph("diskIo",
					"Disk IO", 	// Menu Checkbox text
					"Number of Disk IO Operations per Second", // Label 
					new String[] { "User Data", "User Log", "Tempdb Data", "Tempdb Log", "System" }, 
					false, tmp, true);
			tmp.addTrendGraph("diskIo", tg, true);
		}
		
		_CMList.add(tmp);





		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// IOQueue Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = "CMioQueueActivity";
		displayName  = "IO Queue";
		description  = "<html><p>How many IO's have the ASE Server done on various segments (UserDb/Tempdb/System) on specific devices.</p>" +
			"For ASE 15.0.2 or so, we will have 'System' segment covered aswell.<br>" +
			"Do not trust the service time <b>too</b> much...</html>";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 0;
		needCeVersion= 0;
		monTables    = new String[] {"monIOQueue"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring=1"};
		colsCalcDiff = new String[] {"IOs", "IOTime"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList<String>();
		     pkList.add("LogicalName");
		     pkList.add("IOType");

		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				true, true)
		// OVERRIDE SOME DEFAULT METHODS
		{
			private static final long serialVersionUID = 989816816267986305L;

			@Override
			public void initSql(Connection conn)
			{
				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				if (isClusterEnabled())
				{
					cols1 += "InstanceID, ";
					this.getPk().add("InstanceID");
				}

				cols1 += "LogicalName, IOType, IOs, IOTime, \n" +
//				         "AvgServ_ms = convert(numeric(10,1),null)"
		                 "AvgServ_ms = \n" +
		                 "case \n" +
		                 "  when IOs > 0 then convert(numeric(10,1), IOTime/convert(numeric(10,0),IOs)) \n" +
		                 "  else               convert(numeric(10,1), null) \n" +
		                 "end";
				if (aseVersion >= 15010 || (aseVersion >= 12540 && aseVersion <= 15000) )
				{
				}

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from monIOQueue \n" +
					"order by LogicalName, IOType" + (isClusterEnabled() ? ", InstanceID" : "") + "\n";

				setSql(sql);
			}

			@Override
			public void updateGraphData(TrendGraphDataPoint tgdp)
			{
				if ("devSvcTime".equals(tgdp.getName()))
				{
					Double[] arr = new Double[2];
					arr[0] = this.getDiffValueMax("AvgServ_ms"); // MAX
					arr[1] = this.getDiffValueAvgGtZero("AvgServ_ms"); // AVG
					_logger.debug("devSvcTime: MaxServiceTime=" + arr[0] + ", AvgServiceTime=" + arr[1] + ".");
					_logger.debug("updateGraphData(devSvcTime): MaxServiceTime='"+arr[0]+"', AvgServiceTime='"+arr[1]+"'.");

					// Set the values
					tgdp.setDate(this.getTimestamp());
					tgdp.setData(arr);
				}
			}
			@Override
			public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
			{
				double AvgServ_ms;
				long   IOs, IOTime;
				int    IOsColId = 0, IOTimeColId = 0, AvgServ_msColId = 0;

				// Find column Id's
				List<String> colNames = diffData.getColNames();
				if (colNames == null)
					return;
				for (int colId = 0; colId < colNames.size(); colId++)
				{
					String colName = (String) colNames.get(colId);
					if (colName.equals("IOs"))        IOsColId        = colId;
					if (colName.equals("IOTime"))     IOTimeColId     = colId;
					if (colName.equals("AvgServ_ms")) AvgServ_msColId = colId;
				}

				// Loop on all diffData rows
				for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
				{
					IOs    = ((Number) diffData.getValueAt(rowId, IOsColId))   .longValue();
					IOTime = ((Number) diffData.getValueAt(rowId, IOTimeColId)).longValue();

					if (IOs != 0)
					{
						AvgServ_ms = IOTime / IOs;
						BigDecimal newVal = new BigDecimal(AvgServ_ms);
						diffData.setValueAt(newVal, rowId, AvgServ_msColId);
					}
					else
						diffData.setValueAt(new BigDecimal(0), rowId, AvgServ_msColId);
				}
			}
			
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		tmp.addTrendGraphData("devSvcTime", new TrendGraphDataPoint("devSvcTime", new String[] { "Max", "Average" }));
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_io_queue_activity.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );

			// GRAPH
			TrendGraph tg = new TrendGraph("devSvcTime",
					"Device IO Service Time", 	// Menu Checkbox text
					"Device IO Service Time in Milliseconds", // Label 
					new String[] { "Max", "Average" }, 
					false, tmp, true);
			tmp.addTrendGraph("devSvcTime", tg, true);
		}
		
		_CMList.add(tmp);




		
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// sysmonitors... SPINLOCKS
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = "CMspinlockSum";
		displayName  = "Spinlock Sum";
		description  = "<html><p>What spinlocks do we have contention on.</p>" +
			"This could be a bit heavy to use when there is a 'low' refresh interwall.<br>" +
			"For the moment considder this as <b>experimental</b>.</html>";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 0;
		needCeVersion= 0;
		monTables    = new String[] {"sysmonitors"};
		needRole     = new String[] {"sa_role"};
		needConfig   = new String[] {};
		colsCalcDiff = new String[] {"grabs", "waits", "spins"};
		colsCalcPCT  = new String[] {"contention"};
		pkList       = new LinkedList<String>();
		     pkList.add("name");

		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				true, true, 300) // Does the spinlock wrap to 0 or does it wrap to -hugeNumber
		// OVERRIDE SOME DEFAULT METHODS
		{
			private static final long serialVersionUID = -925512251960490929L;

			@Override
			public void initSql(Connection conn)
			{
				int aseVersion = getServerVersion();

				//String cols1, cols2, cols3;
				//cols1 = cols2 = cols3 = "";

				if (isClusterEnabled())
					this.getPk().add("instanceid");

//				String compatibilityModeCheck = "";
//				String compatibilityModeRestore = "";
				String optGoalPlan = "";
				String datatype    = "int";
				if (aseVersion >= 15000)
				{
					datatype    = "bigint";
				}
				if (aseVersion >= 15020)
				{
					optGoalPlan = "plan '(use optgoal allrows_dss)' \n";
				}
//				if (aseVersion >= 15031)
//				{
//					// move this to the ASE CONNECT instead
//					compatibilityModeCheck = 
//						"-- check if we are in 'compatibility_mode', introduced in 15.0.3 esd#1 \n" +
//						"declare @compatibility_mode_is_on int, @compatibility_mode_restore int \n" +
//						"select @compatibility_mode_is_on = 0,  @compatibility_mode_restore = 0 \n" +
//						"    \n" +
//						"select @compatibility_mode_is_on = sign(convert(tinyint,substring(@@options, c.low, 1)) & c.high) \n" +
//						"  from master.dbo.spt_values a, master.dbo.spt_values c \n" +
//						"  where a.number = c.number \n" +
//						"    and c.type = 'P' \n" +
//						"    and a.type = 'N' \n" +
//						"    and c.low <= datalength(@@options) \n" +
//						"    and a.number = 93 \n" +
//						"    and a.name = 'compatibility_mode' \n" +
//						"if (@compatibility_mode_is_on = 1) \n" +
//						"begin \n" +
//						"    set compatibility_mode off\n" +
//						"    select @compatibility_mode_restore = 1 \n" +
//						"end \n" +
//						"\n";
//
//					compatibilityModeRestore = "\n" +
//						"if (@compatibility_mode_restore = 1) \n" +
//						"begin \n" +
//						"    set compatibility_mode on\n" +
//						"end \n" +
//						"\n";
//				}

				String sql =   
					"DBCC monitor('sample', 'all',        'on') \n" +
					"DBCC monitor('sample', 'spinlock_s', 'on') \n" +
					"\n" +
//					compatibilityModeCheck +
					"SELECT \n" +
					(isClusterEnabled() ? "P.instanceid, \n" : "") +
					"  name         = convert(varchar(40), P.field_name), \n" +
					"  instances    = count(P.field_id), \n" +
					"  grabs        = sum(convert("+datatype+",P.value)), \n" +
					"  waits        = sum(convert("+datatype+",W.value)), \n" +
					"  spins        = sum(convert("+datatype+",S.value)), \n" +
					"  contention   = convert(numeric(4,1), null), \n" +
					"  spinsPerWait = convert(numeric(9,1), null), \n" +
					"  description  = convert(varchar(100), '') \n" +
					"FROM master..sysmonitors P, master..sysmonitors W, master..sysmonitors S \n" +
					"WHERE P.group_name = 'spinlock_p_0' \n" +
					"  AND W.group_name = 'spinlock_w_0' \n" +
					"  AND S.group_name = 'spinlock_s_0' \n" +
					"  AND P.field_id = W.field_id \n" +
					"  AND P.field_id = S.field_id \n";
				if (isClusterEnabled()) 
				{
					sql +=
					"  AND P.instanceid = W.instanceid \n" +
					"  AND P.instanceid = S.instanceid \n" +
					"GROUP BY P.instanceid, P.field_name \n" +
					"ORDER BY 2, 1 \n" +
					optGoalPlan;
	//				compatibilityModeRestore;
				}
				else 
				{
					sql +=
					"GROUP BY P.field_name \n" +
					"ORDER BY 1 \n" +
					optGoalPlan;
//					compatibilityModeRestore;
				}

				setSql(sql);


				// SQL INIT (executed first time only)
				// AFTER 12.5.2 traceon(8399) is no longer needed
				String sqlInit = "DBCC traceon(3604) \n";
				if (aseVersion < 12520)
					sqlInit += "DBCC traceon(8399) \n";
				if (aseVersion >= 15020 || (aseVersion >= 12541 && aseVersion <= 15000) )
				{
					sqlInit = "set switch on 3604 with no_info \n";
				}

				sqlInit += "DBCC monitor('select', 'all',        'on') \n" +
				           "DBCC monitor('select', 'spinlock_s', 'on') \n";

				setSqlInit(sqlInit);
			}

			@Override
			public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
			{
				MonTablesDictionary mtd = MonTablesDictionary.getInstance();

				long grabs,          waits,          spins;
				int  grabsColId = 0, waitsColId = 0, spinsColId = 0, contentionColId = 0, spinsPerWaitColId = 0;
				int  pos_name = -1,  pos_desc = -1;

				// Find column Id's
				List<String> colNames = diffData.getColNames();
				if (colNames == null)
					return;
				for (int colId = 0; colId < colNames.size(); colId++)
				{
					String colName = (String) colNames.get(colId);
					if      (colName.equals("grabs"))        grabsColId        = colId;
					else if (colName.equals("waits"))        waitsColId        = colId;
					else if (colName.equals("spins"))        spinsColId        = colId;
					else if (colName.equals("contention"))   contentionColId   = colId;
					else if (colName.equals("spinsPerWait")) spinsPerWaitColId = colId;
					else if (colName.equals("name"))         pos_name          = colId;
					else if (colName.equals("description"))  pos_desc          = colId;
				}

				// Loop on all diffData rows
				for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
				{
					grabs = ((Number) diffData.getValueAt(rowId, grabsColId)).longValue();
					waits = ((Number) diffData.getValueAt(rowId, waitsColId)).longValue();
					spins = ((Number) diffData.getValueAt(rowId, spinsColId)).longValue();

					// contention
					if (grabs > 0)
					{
						BigDecimal contention = new BigDecimal( ((1.0 * (waits)) / grabs) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

						// Keep only 3 decimals
						// row.set(AvgServ_msColId, new Double (AvgServ_ms/1000) );
						diffData.setValueAt(contention, rowId, contentionColId);
					}
					else
						diffData.setValueAt(new BigDecimal(0), rowId, contentionColId);

					// spinsPerWait
					if (waits > 0)
					{
						BigDecimal spinWarning = new BigDecimal( ((1.0 * (spins)) / waits) ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

						diffData.setValueAt(spinWarning, rowId, spinsPerWaitColId);
					}
					else
						diffData.setValueAt(new BigDecimal(0), rowId, spinsPerWaitColId);

					// set description
					if (mtd != null && pos_name >= 0 && pos_desc >= 0)
					{
						Object o = diffData.getValueAt(rowId, pos_name);

						if (o instanceof String)
						{
							String name = (String)o;

							String desc = mtd.getSpinlockDescription(name);
							if (desc != null)
							{
								newSample.setValueAt(desc, rowId, pos_desc);
								diffData .setValueAt(desc, rowId, pos_desc);
							}
						}
					}
				}
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_spinlock_activity.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );
		}
		
		_CMList.add(tmp);






		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// sysmonitors... SYSMON (reuses data from SPINLOCK, which is still in sysmonitors)
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = "CMsysmon";
		displayName  = "Sysmon Raw";
		description  = "<html><p>Just grabs the raw counters used in sp_sysmon.</p>" +
			"NOTE: reuses data from 'Spinlock Sum', so this needs to be running as well.<br>" +
			"For the moment consider this as <b>very experimental</b>.</html>";

		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 0;
		needCeVersion= 0;
		monTables    = new String[] {"sysmonitors"};
		needRole     = new String[] {"sa_role"};
		needConfig   = new String[] {};
		colsCalcDiff = new String[] {"value"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList<String>();
		     pkList.add("field_name");
		     pkList.add("group_name");

		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				true, true, 300) // Does the spinlock wrap to 0 or does it wrap to -hugeNumber
		// OVERRIDE SOME DEFAULT METHODS
		{
			private static final long serialVersionUID = -925512251960490929L;

			@Override
			public void initSql(Connection conn)
			{
				int aseVersion = getServerVersion();

				if (isClusterEnabled())
					this.getPk().add("instanceid");

				String optGoalPlan = "";
				if (aseVersion >= 15020)
				{
					optGoalPlan = "plan '(use optgoal allrows_dss)' \n";
				}

				String sql =   
					"SELECT \n" +
					(isClusterEnabled() ? "instanceid, \n" : "") +
					"  field_name = convert(varchar(100),field_name), \n" +
					"  group_name = convert(varchar(30),group_name), \n" +
					"  field_id, \n" +
					"  value, \n" +
					"  description  = convert(varchar(255), description) \n" +
					"FROM master..sysmonitors \n" +
					"WHERE group_name not in ('spinlock_p_0', 'spinlock_w_0', 'spinlock_s_0') \n" +
					"  AND value > 100 \n" +
					"ORDER BY group_name, field_name" + (isClusterEnabled() ? ", instanceid" : "") + "\n" +
					optGoalPlan;

				setSql(sql);
			}
		};

		tmp.addDependsOnCm("CMspinlockSum"); // CMspinlockSum must have been executed before this cm
		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_sysmon_activity.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );
		}
		
		_CMList.add(tmp);






		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// ProcedureCache Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = "CMcachedProcs";
		displayName  = "Cached Procedures";
		description  = "What Objects is located in the 'procedure cache'.";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 0;
		needCeVersion= 0;
		monTables    = new String[] {"monCachedProcedures"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"per object statistics active=1"};
		colsCalcDiff = new String[] {"RequestCnt", "TempdbRemapCnt"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList<String>();
		     pkList.add("PlanID");


		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				true, true)
		// OVERRIDE SOME DEFAULT METHODS
		{
			private static final long serialVersionUID = 7929613701950650791L;

			@Override
			public void initSql(Connection conn)
			{
				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				if (isClusterEnabled())
				{
					cols1 += "InstanceID, ";
					this.getPk().add("InstanceID");
				}

				cols1 += "PlanID, DBName, ObjectName, ObjectType, MemUsageKB, CompileDate";
				if (aseVersion >= 15010 || (aseVersion >= 12540 && aseVersion <= 15000) )
				{
				}
				if (aseVersion >= 15500 || (aseVersion >= 15030 && isClusterEnabled()) )
				{
					cols2 += ", RequestCnt, TempdbRemapCnt, AvgTempdbRemapTime";
				}

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from monCachedProcedures \n" +
					"order by DBName, ObjectName, ObjectType\n";

				setSql(sql);
			}

//			public boolean persistCounters()
//			{
//				return false;
//			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_cached_procedures_activity.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );

			// PROCEDURE ICON
			tcp.addHighlighter( new IconHighlighter(new HighlightPredicate()
			{
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					int modelCol = adapter.getColumnIndex("ObjectType");
					if (modelCol == adapter.convertColumnIndexToModel(adapter.column))
					{
						String objectType = adapter.getString(modelCol);
						if (objectType != null)
							objectType = objectType.trim();
						if ( objectType.startsWith("stored procedure"))
							return true;
					}
					return false;
				}
			}, SwingUtils.readImageIcon(Version.class, "images/highlighter_procedure.png")));
						
			// TRIGGER ICON
			tcp.addHighlighter( new IconHighlighter(new HighlightPredicate()
			{
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					int modelCol = adapter.getColumnIndex("ObjectType");
					if (modelCol == adapter.convertColumnIndexToModel(adapter.column))
					{
						String objectType = adapter.getString(modelCol);
						if (objectType != null)
							objectType = objectType.trim();
						if ( objectType.startsWith("trigger"))
							return true;
					}
					return false;
				}
			}, SwingUtils.readImageIcon(Version.class, "images/highlighter_trigger.png")));

			// VIEW ICON
			tcp.addHighlighter( new IconHighlighter(new HighlightPredicate()
			{
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					int modelCol = adapter.getColumnIndex("ObjectType");
					if (modelCol == adapter.convertColumnIndexToModel(adapter.column))
					{
						String objectType = adapter.getString(modelCol);
						if (objectType != null)
							objectType = objectType.trim();
						if ( objectType.startsWith("view"))
							return true;
					}
					return false;
				}
			}, SwingUtils.readImageIcon(Version.class, "images/highlighter_view.png")));

			// DEFAULT VALUE icon
			tcp.addHighlighter( new IconHighlighter(new HighlightPredicate()
			{
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					int modelCol = adapter.getColumnIndex("ObjectType");
					if (modelCol == adapter.convertColumnIndexToModel(adapter.column))
					{
						String objectType = adapter.getString(modelCol);
						if (objectType != null)
							objectType = objectType.trim();
						if ( objectType.startsWith("default value spec"))
							return true;
					}
					return false;
				}
			}, SwingUtils.readImageIcon(Version.class, "images/highlighter_default_value.png")));

			// RULE icon
			tcp.addHighlighter( new IconHighlighter(new HighlightPredicate()
			{
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					int modelCol = adapter.getColumnIndex("ObjectType");
					if (modelCol == adapter.convertColumnIndexToModel(adapter.column))
					{
						String objectType = adapter.getString(modelCol);
						if (objectType != null)
							objectType = objectType.trim();
						if ( objectType.startsWith("rule"))
							return true;
					}
					return false;
				}
			}, SwingUtils.readImageIcon(Version.class, "images/highlighter_rule.png")));
		}
		
		_CMList.add(tmp);






		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// ProcedureCache Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = "CMprocCache";
		displayName  = "Procedure Cache";
		description  = "This is just a short one to see if we have 'stored procedure' swapping for some reason. The reason is for the moment not known.";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 0;
		needCeVersion= 0;
		monTables    = new String[] {"monProcedureCache"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring=1"};
		colsCalcDiff = new String[] {"Requests", "Loads", "Writes", "Stalls"};
		colsCalcPCT  = new String[] {};
		pkList     = new LinkedList<String>();
		//     pkList.add("PlanID");

		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				true, true)
		{
			private static final long serialVersionUID = -6467250604457739178L;

			@Override
			public void initSql(Connection conn)
			{
				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				cols2 += "Requests, Loads, Writes, Stalls";
				if (isClusterEnabled())
				{
					cols1 = "InstanceID, ";
					this.getPk().add("InstanceID");
				}

				//cols1 = "Requests, Loads, Writes, Stalls";
				if (aseVersion >= 15010 || (aseVersion >= 12540 && aseVersion <= 15000) )
				{
				}

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from monProcedureCache \n";

				setSql(sql);
			}
			
			@Override
			public void updateGraphData(TrendGraphDataPoint tgdp)
			{
				if ("ProcCacheGraph".equals(tgdp.getName()))
				{
					Double[] arr = new Double[2];

					arr[0] = this.getRateValueSum("Requests");
					arr[1] = this.getRateValueSum("Loads");
					_logger.debug("updateGraphData(ProcCacheGraph): Requests='"+arr[0]+"', Loads='"+arr[1]+"'.");

					// Set the values
					tgdp.setDate(this.getTimestamp());
					tgdp.setData(arr);
				}
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		tmp.addTrendGraphData("ProcCacheGraph", new TrendGraphDataPoint("ProcCacheGraph", new String[] { "Requests", "Loads" }));
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_procedure_cache_activity.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );

			// GRAPH
			TrendGraph tg = new TrendGraph("ProcCacheGraph",
					// Menu Checkbox text
					"Procedure Cache Requests",
					// Label 
					"Number of Procedure Requests per Second (procs,triggers,views)", // Label 
					new String[] { "Requests", "Loads" }, 
					false, tmp, true);
			tmp.addTrendGraph("ProcCacheGraph", tg, true);
		}
		
		_CMList.add(tmp);







		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// Procedure Stack
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------

		//==================================================================================================
		// 12.5.0.3: Description of: monProcessProcedures
		// 
		// Returns a list of all procedures that are being executed by processes. 
		// monProcessProcedures does not require any configuration options. 
		// 
		// Name             Datatype     Attributes  Description
		// ---------------- ------------ ----------- ------------------------------------------------------------
		// SPID             smallint                 Session process identifier
		// KPID             int                      Kernel process identifier
		// DBID             int                      Unique identifier for the database
		// OwnerUID         int                      Unique identifier for the object owner
		// ObjectID         int                      Unique identifier for the procedure
		// PlanID           int                      Unique identifier for the query plan
		// MemUsageKB       int                      Number of kilobytes of memory used by the procedure
		// CompileDate      datetime                 Compile date of the procedure
		// ContextID        int                      Stack frame of the procedure
		// DBName           varchar(30)  null        Name of the database that contains the procedure
		// OwnerName        varchar(30)  null        Name of the object owner
		// ObjectName       varchar(30)  null        Name of the procedure
		// ObjectType       varchar(32)  null        Type of procedure (stored procedure, trigger procedure, and so on)
		//---------------------------------------------------------------------------------------------------
		// Column changes in various versions:
		//
		// Version     Action  Name            Description
		// ----------- ------- --------------- ---------------------------------
		// 
		// 12.5.3      added   LineNumber      the line in the procedure currently being executed 
		// 15.0.2.5    added   StatementNumber the statement in the stored procedure currently being executed
		// 15.0.2(CE)  added   InstanceID      Cluster instance ID
		// 15.5        added   InstanceID      Cluster instance ID
		//---------------------------------------------------------------------------------------------------

		name         = "CMprocCallStack";
		displayName  = "Procedure Call Stack";
		description  = "Nesting levels of currenty executed procedures, this can be used as a light 'profiler'";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 0;
		needCeVersion= 0;
		monTables    = new String[] {"monProcessProcedures"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {};
		colsCalcDiff = new String[] {};
		colsCalcPCT  = new String[] {};
//		pkList       = new LinkedList<String>();
		pkList       = null;


		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				true, true)
		// OVERRIDE SOME DEFAULT METHODS
		{
			private static final long serialVersionUID = 7929613701950650791L;

			@Override
			public void initSql(Connection conn)
			{
				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				String InstanceID      = "";
				String StatementNumber = "";
				String LineNumber      = "";

				if (isClusterEnabled())  InstanceID      = "InstanceID, ";
				if (aseVersion >= 12530) LineNumber      = "LineNumber, ";
				if (aseVersion >= 15025) StatementNumber = "StatementNumber, ";

				cols1 += "SPID, " + InstanceID + "DBName, OwnerName, ObjectName, " + 
				         LineNumber + StatementNumber + "ContextID, ObjectType, " +
				         "PlanID, MemUsageKB, CompileDate, KPID, DBID, OwnerUID, ObjectID, " +
				         "MaxContextID = convert(int, -1)";
				//"MaxContextID = (select max(ContextID) from monProcessProcedures i where o.SPID = i.SPID)";

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from monProcessProcedures o\n" +
					"order by SPID, ContextID desc\n" +
					"";

				setSql(sql);
			}

			/** 
			 * Fill in the MaxContextID column...
			 * basically do SQL: MaxContextID = (select max(ContextID) from monProcessProcedures i where o.SPID = i.SPID)
			 * But this the SQL is not working very good, since the table is changing to fast.
			 */
			@Override
			public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
			{
				// Where are various columns located in the Vector 
				int pos_SPID = -1, pos_MaxContextID = -1, pos_ContextID = -1;
			
				// Find column Id's
				List<String> colNames = newSample.getColNames();
				if (colNames==null) return;

				for (int colId=0; colId < colNames.size(); colId++) 
				{
					String colName = colNames.get(colId);
					if (colName.equals("SPID"))         pos_SPID         = colId;
					if (colName.equals("ContextID"))    pos_ContextID    = colId;
					if (colName.equals("MaxContextID")) pos_MaxContextID = colId;

					// Noo need to continue, we got all our columns
					if (pos_SPID >= 0 && pos_ContextID >= 0 && pos_MaxContextID >= 0)
						break;
				}

				if (pos_SPID < 0 || pos_ContextID < 0 || pos_MaxContextID < 0)
				{
					_logger.debug("Cant find the position for columns ('SPID'"+pos_SPID+", 'ContextID'="+pos_ContextID+", 'MaxContextID'="+pos_MaxContextID+")");
					return;
				}
				
				// a map that holds <SPID, MaxContextID>
				Map<Integer,Integer> _maxContextIdPerSpid = new HashMap<Integer,Integer>();

				// Loop on all newSample rows, figgure out the MAX ContextID for each SPID 
				for (int rowId=0; rowId < newSample.getRowCount(); rowId++) 
				{
					Object o_SPID         = newSample.getValueAt(rowId, pos_SPID);
					Object o_ContextID    = newSample.getValueAt(rowId, pos_ContextID);

					if (o_SPID instanceof Number && o_ContextID instanceof Number)
					{
						Integer spid         = (Integer)o_SPID;
						Integer contextId    = (Integer)o_ContextID;
						Integer maxContextId = _maxContextIdPerSpid.get(spid);

						if (maxContextId == null)
							maxContextId = 0;

						// write the MAX ContextID in the Map
						_maxContextIdPerSpid.put(spid, Math.max(contextId, maxContextId));
					}
				}

				// Loop on all newSample rows, SET the MaxContextID for each SPID
				for (int rowId=0; rowId < newSample.getRowCount(); rowId++) 
				{
					Object o_SPID = newSample.getValueAt(rowId, pos_SPID);
					
					if (o_SPID instanceof Number)
					{
						Integer spid         = (Integer)o_SPID;
						Integer maxContextId = _maxContextIdPerSpid.get(spid);

						newSample.setValueAt(maxContextId, rowId, pos_MaxContextID);
					}
				}
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_procedure_call_stack_activity.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );

			tcp.setTableToolTipText(
				    "<html>" +
				        "Background colors:" +
				        "<ul>" +
				        "    <li>GREEN - Procedure/Trigger/View that is currently executing (end of the stack).</li>" +
				        "</ul>" +
				    "</html>");

			// Mark the row as ORANGE if PK has been visible on more than 1 sample
			if (conf != null) colorStr = conf.getProperty(name+".color.xxxxxxxxxx");
			tcp.addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					String maxContextID = adapter.getString(adapter.getColumnIndex("MaxContextID"));
					String contextID    = adapter.getString(adapter.getColumnIndex("ContextID"));
					if (maxContextID != null) maxContextID = maxContextID.trim();
					if (contextID    != null) contextID    = contextID   .trim();
					if ( maxContextID != null && contextID != null && maxContextID.equals(contextID))
						return true;
					return false;
				}
			}, SwingUtils.parseColor(colorStr, Color.GREEN), null));

			// Procedure ICON
			tcp.addHighlighter( new IconHighlighter(new HighlightPredicate()
			{
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					int modelCol = adapter.getColumnIndex("ObjectType");
					if (modelCol == adapter.convertColumnIndexToModel(adapter.column))
					{
						String objectType = adapter.getString(modelCol);
						if (objectType != null)
							objectType = objectType.trim();
						if ( objectType.startsWith("stored procedure"))
							return true;
					}
					return false;
				}
			}, SwingUtils.readImageIcon(Version.class, "images/highlighter_procedure.png")));
						
			// Trigger ICON
			tcp.addHighlighter( new IconHighlighter(new HighlightPredicate()
			{
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					int modelCol = adapter.getColumnIndex("ObjectType");
					if (modelCol == adapter.convertColumnIndexToModel(adapter.column))
					{
						String objectType = adapter.getString(modelCol);
						if (objectType != null)
							objectType = objectType.trim();
						if ( objectType.startsWith("trigger"))
							return true;
					}
					return false;
				}
			}, SwingUtils.readImageIcon(Version.class, "images/highlighter_trigger.png")));

			// View ICON
			tcp.addHighlighter( new IconHighlighter(new HighlightPredicate()
			{
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					int modelCol = adapter.getColumnIndex("ObjectType");
					if (modelCol == adapter.convertColumnIndexToModel(adapter.column))
					{
						String objectType = adapter.getString(modelCol);
						if (objectType != null)
							objectType = objectType.trim();
						if ( objectType.startsWith("view"))
							return true;
					}
					return false;
				}
			}, SwingUtils.readImageIcon(Version.class, "images/highlighter_view.png")));
		}
		
		_CMList.add(tmp);






		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// ObjectsInCache Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = "CMcachedObjects";
		displayName  = "Cached Objects";
		description  = "";
		description  = "<html><p>What Tables is located in the 'data cache' and to what cache is it bound?</p>" +
			"<b>This could be a bit heavy to sample, so use with caution.</b><br>" +
			"Tip:<br>" +
			"- Use ABSolute counter values to check how many MB the table are using." +
			"- Use Diff or Rate to check if the memory print is increasing or decreasing.<br>" +
			"- Use the checkpox 'Pause Data Polling' wait for a minute, then enable polling again, that will give you a longer sample period!</html>";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 0;
		needCeVersion= 0;
		monTables    = new String[] {"monCachedObject"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {};
		colsCalcDiff = new String[] {"CachedKB", "TotalSizeKB"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList<String>();
		     pkList.add("DBID");
		     pkList.add("ObjectID");
		     pkList.add("IndexID");


		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				false, true) // "CachedKB", "TotalSizeKB" goes UP and DOWN: so wee want negative count values
		// OVERRIDE SOME DEFAULT METHODS
		{
			private static final long serialVersionUID = 1725558024113511209L;

			@Override
			public void initSql(Connection conn)
			{
				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				if (isClusterEnabled())
				{
					cols1 += "InstanceID, ";
					this.getPk().add("InstanceID");
				}

				cols1 += "DBID, ObjectID, IndexID, DBName,  ObjectName, ObjectType, ";
				cols2 += "";
				cols3 += "CachedKB, CacheName";
				if (aseVersion >= 15000)
				{
					cols2 += "PartitionID, PartitionName, TotalSizeKB, ";
					getPk().add("PartitionID");
				}

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from monCachedObject \n" +
					"order by DBName,  ObjectName";

				setSql(sql);
			}

//			public boolean persistCounters()
//			{
//				return false;
//			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		// If POSTPONE time is not set, set this to 10 minutes (600 sec), because this is HEAVY 
		if (tmp.getPostponeTime() == 0)
			tmp.setPostponeTime(600);

		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_objects_in_cache_activity.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );

			tcp.setTableToolTipText(
				    "<html>" +
				        "Background colors:" +
				        "<ul>" +
				        "    <li>ORANGE - An Index.</li>" +
				        "</ul>" +
				    "</html>");

			// ORANGE = Index id > 0
			if (conf != null) colorStr = conf.getProperty(name+".color.index");
			tcp.addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					Number indexId = (Number) adapter.getValue(adapter.getColumnIndex("IndexID"));
					if ( indexId != null && indexId.intValue() > 0)
						return true;
					return false;
				}
			}, SwingUtils.parseColor(colorStr, Color.ORANGE), null));
		}
		
		_CMList.add(tmp);
		




		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// errorlog Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = "CMerrolog";
		displayName  = "Errorlog";
		description  = "Look at the ASE Servers errorlog.";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 0;
		needCeVersion= 0;
		monTables    = new String[] {"monErrorLog"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring=1", "errorlog pipe active=1", "errorlog pipe max=200"};
		
		tmp = new CountersModelAppend( name, null, monTables, needRole, needConfig, needVersion, needCeVersion, true)
		// OVERRIDE SOME DEFAULT METHODS
		{
			private static final long serialVersionUID = 1227895347277630659L;

			@Override
			public void initSql(Connection conn)
			{
				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				String instanceId = "";
				if (isClusterEnabled())
				{
					instanceId = "InstanceID, ";
				}

				cols1 = "Time, "+instanceId+"SPID, KPID, FamilyID, EngineNumber, ErrorNumber, Severity, ";
				cols2 = "";
				cols3 = "ErrorMessage";
				if (aseVersion >= 12510)
				{
					cols2 = "State, ";
				}
			
				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from monErrorLog\n";

				setSql(sql);
			}

//			public boolean persistCounters()
//			{
//				return false;
//			}
		};
	
		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_errorlog_activity.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );
		}

		_CMList.add(tmp);





		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// DeadLock Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = "CMdeadlock";
		displayName  = "Deadlock";
		description  = "Have we had any deadlocks in the system?";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 0;
		needCeVersion= 0;
		monTables    = new String[] {"monDeadLock"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring=1", "deadlock pipe active=1", "deadlock pipe max=500"};

		tmp = new CountersModelAppend( name, null, monTables, needRole, needConfig, needVersion, needCeVersion, true)
		// OVERRIDE SOME DEFAULT METHODS
		{
			private static final long serialVersionUID = -5448698796472216270L;

			@Override
			public void initSql(Connection conn)
			{
				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				// 12.5.4 version
				//		cols1 = "DeadlockID, VictimKPID, ResolveTime, ObjectDBID, PageNumber, RowNumber, HeldFamilyID, HeldSPID, HeldKPID, HeldProcDBID, HeldProcedureID, HeldBatchID, HeldContextID, HeldLineNumber, WaitFamilyID, WaitSPID, WaitKPID, WaitTime, ObjectName, HeldUserName, HeldApplName, HeldTranName, HeldLockType, HeldCommand, WaitUserName, WaitLockType";

				cols1 = "*";
				cols2 = "";
				cols3 = "";
				if (aseVersion >= 15020)
				{
				}

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from monDeadLock\n";

				setSql(sql);
			}

//			public boolean persistCounters()
//			{
//				return false;
//			}
		};
	
		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_deadlock_activity.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );
		}

		_CMList.add(tmp);

		




		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// proc Cache Module Usage Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = "CMpCacheModuleUsage";
		displayName  = "Proc Cache Module Usage";
		description  = "What module of the ASE Server is using the 'procedure cache' or 'dynamic memory pool'";
		
		needVersion  = 15010; // monProcedureCache*Usage where intruduced in 15.0.1
		needCeVersion= 0;
		monTables    = new String[] {"monProcedureCacheModuleUsage"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {};
		colsCalcDiff = new String[] {"Active", "NumPagesReused"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList<String>();
		     pkList.add("ModuleID");

		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				false, true)  // "Active", "NumPagesReused" goes UP and DOWN: so wee want negative count values
		{
			private static final long serialVersionUID = -2606894492200318775L;

			@Override
			public void initSql(Connection conn)
			{
				int aseVersion = getServerVersion();

				//String cols1, cols2, cols3;
				//cols1 = cols2 = cols3 = "";

				String instanceId = "";
				if (isClusterEnabled() && aseVersion >= 15500)
				{
					instanceId = "InstanceID, ";
					this.getPk().add("InstanceID");
				}

				String sql = 
					"select "+instanceId+"ModuleName, ModuleID, Active, HWM, NumPagesReused \n" +
					"from monProcedureCacheModuleUsage\n" +
					"order by ModuleID";

				setSql(sql);
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_proc_cache_module_usage.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );
		}
		
		_CMList.add(tmp);





		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// proc Cache Memory Usage Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = "CMpCacheMemoryUsage";
		displayName  = "Proc Cache Memory Usage";
		description  = "What module and what 'part' of the modules are using the 'procedure cache' or 'dynamic memory pool'.";
		
		needVersion  = 15010; // monProcedureCache*Usage where intruduced in 15.0.1
		needCeVersion= 0;
		monTables    = new String[] {"monProcedureCacheMemoryUsage", "monProcedureCacheModuleUsage"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {};
		colsCalcDiff = new String[] {"Active", "NumReuseCaused"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList<String>();
		     pkList.add("AllocatorID");


		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				false, true)  // "Active", "NumReuseCaused" goes UP and DOWN: so wee want negative count values
		{
			private static final long serialVersionUID = -5474676196893459453L;

			@Override
			public void initSql(Connection conn)
			{
				int aseVersion = getServerVersion();

				String optGoalPlan = "";
				if (aseVersion >= 15020)
				{
					optGoalPlan = "plan '(use optgoal allrows_dss)' \n";
				}

				//String cols1, cols2, cols3;
				//cols1 = cols2 = cols3 = "";

				String instanceId     = "";
				String instanceIdJoin = "";
				if (isClusterEnabled() && aseVersion >= 15500)
				{
					instanceId = "C.InstanceID, ";
					instanceIdJoin = "  and C.InstanceID = M.InstanceID \n";
					this.getPk().add("InstanceID");
				}

				String sql = 
					"select M.ModuleName, "+instanceId+"C.ModuleID, C.AllocatorName, C.AllocatorID, C.Active, C.HWM, C.ChunkHWM, C.NumReuseCaused \n" +
					"from monProcedureCacheMemoryUsage C, monProcedureCacheModuleUsage M \n" +
					"where C.ModuleID = M.ModuleID \n" +
					instanceIdJoin +
					"order by C.ModuleID, C.AllocatorID \n" +
					optGoalPlan;

				setSql(sql);
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_proc_cache_memory_usage.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );
		}
		
		_CMList.add(tmp);




		
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// Statement Cache Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = "CMstatementCache";
		displayName  = "Statement Cache";
		description  = "FIXME";
		
		needVersion  = 15020; // monStatementCache where intruduced in 15.0.2
		needCeVersion= 0;
		monTables    = new String[] {"monStatementCache"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring=1", "enable stmt cache monitoring=1", "statement cache size"}; // NO default for 'statement cache size' configuration
		colsCalcDiff = new String[] {"NumStatements", "NumSearches", "HitCount", "NumInserts", "NumRemovals", "NumRecompilesSchemaChanges", "NumRecompilesPlanFlushes"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList<String>();
		     pkList.add("TotalSizeKB");

		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				false, true) // true|false i'm not sure
		{
			private static final long serialVersionUID = -9084781196460687664L;

			@Override
			public void initSql(Connection conn)
			{
				//int aseVersion = getServerVersion();

				//String cols1, cols2, cols3;
				//cols1 = cols2 = cols3 = "";

				if (isClusterEnabled())
				{
					this.getPk().add("InstanceID");
				}

				String sql = 
					"select * \n" +
					"from monStatementCache\n";

				setSql(sql);
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_statement_cache_activity.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );
		}
		
		_CMList.add(tmp);





		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// Statement Cache Details Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = "CMstmntCacheDetails";
		displayName  = "Statement Cache Details";
		description  = "FIXME";
		
		needVersion  = 15020; // monCachedStatement where intruduced in 15.0.2
		needCeVersion= 0;
		monTables    = new String[] {"monCachedStatement"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring=1", "enable stmt cache monitoring=1", "statement cache size"}; // NO default for 'statement cache size' configuration
		colsCalcDiff = new String[] {"UseCount", "CurrentUsageCount", "MaxUsageCount", "NumRecompilesPlanFlushes", "NumRecompilesSchemaChanges"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList<String>();
		     pkList.add("SSQLID");

		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				false, true)
		{
			private static final long serialVersionUID = -1842749890597642593L;

			@Override
			public void initSql(Connection conn)
			{
				//int aseVersion = getServerVersion();

				//String cols1, cols2, cols3;
				//cols1 = cols2 = cols3 = "";

				if (isClusterEnabled())
				{
					this.getPk().add("InstanceID");
				}

				String sql = 
					"select * \n" +
					"from monCachedStatement\n";

				setSql(sql);
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_statement_cache_detail_activity.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );
		}
		
		_CMList.add(tmp);


		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// ProcessObject Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------

		//==================================================================================================
		// 12.5.0.3: Description of: monProcessObject
		// 
		// Provides statistical information regarding objects that have been accessed by processes. 
		// monProcessObject requires the enable monitoring and per object statistics active configuration parameters to be enabled.
		// 
		// Name             Datatype Attributes  Description
		// ---------------- -------- ----------- ------------------------------------------------------------
		// SPID             smallint             Session process identifier
		// KPID             int                  Kernel process identifier
		// DBID             int                  Unique identifier for the database where the object resides
		// ObjectID         int                  Unique identifier for the object
		// IndexID          int                  Unique identifier for the index
		// OwnerUserID      int                  User identifier for the object owner
		// LogicalReads     int          counter Number of buffers read from cache
		// PhysicalReads    int          counter Number of buffers read from disk
		// PhysicalAPFReads int          counter Number of APF buffers read from disk
		// DBName           varchar(30)  null    Name of database
		// ObjectName       varchar(30)  null    Name of the object
		// ObjectType       varchar(30)  null    Type of object
		//---------------------------------------------------------------------------------------------------
		// Column changes in various versions:
		//
		// Version     Action  Name            Description
		// ----------- ------- --------------- ---------------------------------
		// 12.5.2      added   TableSize       table size in Kbyte
		// 15.0        changed TableSize       This column was deleted/cahnged into PartitionSize
		// 15.0        added   PartitionSize   this reflects the size of the partition for the object
		// 15.0        added   PartitionID     partition name
		// 15.0        added   PartitionName   partition ID
		// 15.0.2(CE)  added   InstanceID      Cluster instance ID
		// 15.5        added   InstanceID      Cluster instance ID
		//---------------------------------------------------------------------------------------------------

		name         = "CMActiveObjects";
		displayName  = "Active Objects";
		description  = "Objects that are currently accessed by any active Statements in the ASE.";

		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 0;
		needCeVersion= 0;
		monTables    = new String[] {"monProcessObject"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring=1", "per object statistics active=1"};
		colsCalcDiff = new String[] {"LogicalReads", "PhysicalReads", "PhysicalAPFReads"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList<String>();
			pkList.add("SPID");
			pkList.add("DBID"); 
			pkList.add("OwnerUserID");
			pkList.add("ObjectID"); 
			pkList.add("IndexID");
			// NOTE: PK is NOT unique, so therefore 'dupMergeCount' column is added to the SQL Query
			//       This can happen for example on self joins (or when the same table is refrerenced more than once in the a SQL Statement)
	

		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				false, true)
		{
			private static final long serialVersionUID = -1842749890866142593L;

			@Override
			public void initSql(Connection conn)
			{
				try 
				{
					MonTablesDictionary mtd = MonTablesDictionary.getInstance();
					mtd.addColumn("monProcessObject",  "dupMergeCount", "<html>" +
					                                                       "If more than <b>one</b> row was fetched for this \"Primary Key\".<br>" +
					                                                       "Then this column will hold number of rows merged into this row. 0=No Merges(only one row for this PK), 1=One Merge accurred(two rows was seen for this PK), etc...<br>" +
					                                                       "This means that the non-diff columns will be from the first row fetched,<br>" +
					                                                       "then all columns which is marked for difference calculation will be a summary of all the rows (so it's basically a SQL SUM(colName) operation)." +
					                                                   "</html>");
				}
				catch (NameNotFoundException e) {/*ignore*/}

				int aseVersion = getServerVersion();

				String cols1 = "";

				if (isClusterEnabled())
				{
					cols1 += "InstanceID, ";
					this.getPk().add("InstanceID");
				}

				if (aseVersion >= 15000)
				{
					cols1 += "SPID, KPID, ObjectType, DBName, ObjectName, IndexID, PartitionName, PartitionSize, OwnerUserID, LogicalReads, PhysicalReads, PhysicalAPFReads, dupMergeCount=convert(int,0), DBID, ObjectID, PartitionID";
					this.getPk().add("PartitionID");
				}
				else
				{
					String TableSize = "";
					if (aseVersion >= 12520)
						TableSize = "TableSize, ";

					cols1 += "SPID, KPID, ObjectType, DBName, ObjectName, IndexID, "+TableSize+"                 OwnerUserID, LogicalReads, PhysicalReads, PhysicalAPFReads, dupMergeCount=convert(int,0), DBID, ObjectID";
				}
			
				// in 12.5.4 (esd#9) will produce an "empty" resultset using "S.SPID != @@spid"
				//                   so this will be a workaround for those releses below 15.0.0
				String whereSpidNotMe = "SPID != @@spid";
//				if (aseVersion >= 12540 && aseVersion <= 15000)
				if (aseVersion <= 15000)
				{
					whereSpidNotMe = "SPID != convert(int,@@spid)";
				}

				String sql = 
					"select " + cols1 + "\n" +
					"from monProcessObject\n" +
					"where "+whereSpidNotMe;

				setSql(sql);
			}
		};
	
		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_process_object_activity.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );

			tcp.setTableToolTipText(
				    "<html>" +
				        "Background colors:" +
				        "<ul>" +
				        "    <li>ORANGE - An Index.</li>" +
				        "</ul>" +
				    "</html>");

			// ORANGE = Index id > 0
			if (conf != null) colorStr = conf.getProperty(name+".color.index");
			tcp.addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					Number indexId = (Number) adapter.getValue(adapter.getColumnIndex("IndexID"));
					if ( indexId != null && indexId.intValue() > 0)
						return true;
					return false;
				}
			}, SwingUtils.parseColor(colorStr, Color.ORANGE), null));
		}

		_CMList.add(tmp);
		


		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// Active Statements Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = "CMActiveStatements";
		displayName  = "Active Statements";
		description  = "Statemenets that are currently executing in the ASE.";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 0;
		needCeVersion= 0;
		monTables    = new String[] {"monProcessStatement", "monProcess"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring=1", "statement statistics active=1", "wait event timing=1"};
		colsCalcDiff = new String[] {"MemUsageKB", "PhysicalReads", "LogicalReads", "PagesModified", "PacketsSent", "PacketsReceived", "pssinfo_tempdb_pages"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList<String>();
		     pkList.add("SPID");
		     pkList.add("monSource");

		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				true, true)
		{
			private static final long serialVersionUID = 5078336367667465709L;

			@Override
			public void initSql(Connection conn)
			{
				try 
				{
					MonTablesDictionary mtd = MonTablesDictionary.getInstance();
					mtd.addColumn("monProcess",  "BlockingOtherSpids", "This SPID is Blocking other SPID's from executing, because this SPID hold lock(s), that some other SPID wants to grab.");
					mtd.addColumn("monProcess",  "multiSampled",       "<html>" +
					                                                       "This indicates that the PrimaryKey (SPID, monSource[, InstanceID]) has been in this table for more than one sample.<br>" +
					                                                       "Also 'StartTime' has to be the <b>same</b> as in the previous sample.<br>" +
					                                                       "The StartTime is the columns, which tells the start time for current SQL statement, or the individual statement inside a Stored Procedure." +
					                                                   "</html>");

					mtd.addColumn("monProcess",  "HasMonSqlText",      "Has values for: select SQLText from master..monProcessSQLText where SPID = <SPID>");
					mtd.addColumn("monProcess",  "MonSqlText",                         "select SQLText from master..monProcessSQLText where SPID = <SPID>");
					mtd.addColumn("monProcess",  "HasDbccSqlText",     "Has values for: DBCC sqltext(<SPID>)");
					mtd.addColumn("monProcess",  "DbccSqlText",                        "DBCC sqltext(<SPID>)");
					mtd.addColumn("monProcess",  "HasShowPlan",        "Has values for: sp_showplan <SPID>, null, null, null");
					mtd.addColumn("monProcess",  "ShowPlanText",                       "sp_showplan <SPID>, null, null, null");
					mtd.addColumn("monProcess",  "HasStacktrace",      "Has values for: DBCC stacktrace(<SPID>)");
					mtd.addColumn("monProcess",  "DbccStacktrace",                     "DBCC stacktrace(<SPID>)");
					mtd.addColumn("monProcess",  "HasProcCallStack",   "Has values for: select * from monProcessProcedures where SPID = <SPID>");
					mtd.addColumn("monProcess",  "ProcCallStack",                      "select * from monProcessProcedures where SPID = <SPID>");
				}
				catch (NameNotFoundException e) {/*ignore*/}

				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				////////////////////////////
				// NOTE: This Counter Model has 2 separete SQL selects
				//       1: The rows in: monProcessStatement
				//       2: The rows in: SPID's that might NOT be in monProcessStatement, BUT still are BLOCKING other spids
				// IMPORTANT: when you add a column to the FIRST you need to add it to the SECOND as well
				////////////////////////////

				String optGoalPlan = "";
				if (aseVersion >= 15020)
				{
					optGoalPlan = "plan '(use optgoal allrows_dss)' \n";
				}

				if (isClusterEnabled())
				{
					cols1 += "S.InstanceID, ";
					this.getPk().add("InstanceID");
				}

				String dbNameCol  = "dbname=db_name(S.DBID)";
				if (aseVersion >= 15020) // just a guess what release this was intruduced
				{
					dbNameCol  = "dbname=S.DBName";
				}

				cols1 += "monSource=convert(varchar(10),'ACTIVE'), \n" +
				         "P.SPID, P.KPID, \n" +
				         "multiSampled=convert(varchar(10),''), \n" +
				         "S.BatchID, S.LineNumber, \n" +
				         dbNameCol+", procname=isnull(object_name(S.ProcedureID,S.DBID),''), linenum=S.LineNumber, \n" +
				         "P.Command, P.Application, \n" +
				         "S.CpuTime, S.WaitTime, ExecTimeInMs=datediff(ms, S.StartTime, getdate()), \n" +
				         "UsefullExecTime = (datediff(ms, S.StartTime, getdate()) - S.WaitTime), \n" +
				         "BlockingOtherSpids=convert(varchar(255),''), P.BlockingSPID, \n" +
				         "P.SecondsWaiting, P.WaitEventID, \n" +
				         "WaitClassDesc=convert(varchar(50),''), \n" +
				         "WaitEventDesc=convert(varchar(50),''), \n" +
				         "HasMonSqlText=convert(bit,0), HasDbccSqlText=convert(bit,0), HasProcCallStack=convert(bit,0), HasShowPlan=convert(bit,0), HasStacktrace=convert(bit,0), \n" +
				         "S.MemUsageKB, S.PhysicalReads, S.LogicalReads, \n";
				cols2 += "";
				cols3 += "S.PagesModified, S.PacketsSent, S.PacketsReceived, S.NetworkPacketSize, \n" +
				         "S.PlansAltered, S.StartTime, S.PlanID, S.DBID, S.ProcedureID, \n" +
				         "P.SecondsConnected, P.EngineNumber, P.NumChildren, \n" +
				         "MonSqlText=convert(text,null), \n" +
				         "DbccSqlText=convert(text,null), \n" +
				         "ProcCallStack=convert(text,null), \n" +
				         "ShowPlanText=convert(text,null), \n" +
				         "DbccStacktrace=convert(text,null) \n";
				if (aseVersion >= 15020 || (aseVersion >= 12540 && aseVersion <= 15000) )
				{
					cols2 += "S.RowsAffected, " +
					         "tempdb_name = db_name(tempdb_id(S.SPID)), " +
					         "pssinfo_tempdb_pages = convert(int, pssinfo(S.SPID, 'tempdb_pages')), \n";
				}

				// in 12.5.4 (esd#9) will produce an "empty" resultset using "S.SPID != @@spid"
				//                   so this will be a workaround for those releses below 15.0.0
				String whereSpidNotMe = "S.SPID != @@spid";
//				if (aseVersion >= 12540 && aseVersion <= 15000)
				if (aseVersion <= 15000)
				{
					whereSpidNotMe = "S.SPID != convert(int,@@spid)";
				}

				// The above I had in the Properties file for a long time
				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from monProcessStatement S, monProcess P \n" +
					"where S.KPID = P.KPID \n" +
					(isClusterEnabled() ? "  and S.InstanceID = P.InstanceID \n" : "") +
					"  and "+whereSpidNotMe+"\n" +
					"order by S.LogicalReads desc \n" +
					optGoalPlan;

				//-------------------------------------------
				// Build SECOND SQL, which gets SPID's that blocks other, and might not be within the ABOVE ststement
				//-------------------------------------------
				cols1 = cols2 = cols3 = "";

				if (isClusterEnabled())
				{
					cols1 += "P.InstanceID, ";
				}

				dbNameCol  = "dbname=db_name(P.DBID)";
				if (aseVersion >= 15020) // just a guess what release this was intruduced
				{
					dbNameCol  = "dbname=P.DBName";
				}

				cols1 += "monSource=convert(varchar(10),'BLOCKER'), \n" +
				         "P.SPID, P.KPID, \n" +
				         "multiSampled=convert(varchar(10),''), \n" +
				         "P.BatchID, P.LineNumber, \n" +
				         dbNameCol+", procname='', linenum=P.LineNumber, \n" +
				         "P.Command, P.Application, \n" +
				         "CpuTime=-1, WaitTime=-1, ExecTimeInMs=-1, \n" +
				         "UsefullExecTime = -1, \n" +
				         "BlockingOtherSpids=convert(varchar(255),''), P.BlockingSPID, \n" +
				         "P.SecondsWaiting, P.WaitEventID, \n" +
				         "WaitClassDesc=convert(varchar(50),''), \n" +
				         "WaitEventDesc=convert(varchar(50),''), \n" +
				         "HasMonSqlText=convert(bit,0), HasDbccSqlText=convert(bit,0), HasProcCallStack=convert(bit,0), HasShowPlan=convert(bit,0), HasStacktrace=convert(bit,0), \n" +
				         "MemUsageKB=-1, PhysicalReads=-1, LogicalReads=-1, \n";
				cols2 += "";
				cols3 += "PagesModified=-1, PacketsSent=-1, PacketsReceived=-1, NetworkPacketSize=-1, \n" +
				         "PlansAltered=-1, StartTime=convert(datetime,NULL), PlanID=-1, P.DBID, ProcedureID=-1, \n" +
				         "P.SecondsConnected, P.EngineNumber, P.NumChildren, \n" +
				         "MonSqlText=convert(text,null), \n" +
				         "DbccSqlText=convert(text,null), \n" +
				         "ProcCallStack=convert(text,null), \n" +
				         "ShowPlanText=convert(text,null), \n" +
				         "DbccStacktrace=convert(text,null) \n";
				if (aseVersion >= 15020 || (aseVersion >= 12540 && aseVersion <= 15000) )
				{
					cols2 += "RowsAffected=-1, " +
					         "tempdb_name = db_name(tempdb_id(P.SPID)), " +
					         "pssinfo_tempdb_pages = convert(int, pssinfo(P.SPID, 'tempdb_pages')), \n";
				}


				// The above I had in the Properties file for a long time
				String x_sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from monProcess P \n" +
					"where P.SPID in (select blocked from sysprocesses where blocked > 0) \n" +
					optGoalPlan;


				//------------------------------------------------
				// Finally set the SQL
				//------------------------------------------------
				setSql(  sql + "\n\n"
				       + "  /* UNION ALL */ \n\n"
				       + x_sql);
			}

			@Override
			public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
			{
				// SHOWPLAN STUFF
				if ("HasShowPlan".equals(colName))
				{
					// Find 'ShowPlanText' column, is so get it and set it as the tool tip
					int pos_ShowPlanText = findColumn("ShowPlanText");
					if (pos_ShowPlanText > 0)
					{
						Object cellVal = getValueAt(modelRow, pos_ShowPlanText);
						if (cellVal instanceof String)
							return (String) cellVal;
					}
				}
				if ("ShowPlanText".equals(colName))
				{
					return cellValue == null ? null : cellValue.toString();
				}
				
				// MON SQL TEXT
				if ("HasMonSqlText".equals(colName))
				{
					// Find 'MonSqlText' column, is so get it and set it as the tool tip
					int pos_MonSqlText = findColumn("MonSqlText");
					if (pos_MonSqlText > 0)
					{
						Object cellVal = getValueAt(modelRow, pos_MonSqlText);
						if (cellVal instanceof String)
						{
							return (String) cellVal;
						}
					}
				}
				if ("MonSqlText".equals(colName))
				{
					return cellValue == null ? null : cellValue.toString();
				}
				
				// DBCC SQL TEXT
				if ("HasDbccSqlText".equals(colName))
				{
					// Find 'DbccSqlText' column, is so get it and set it as the tool tip
					int pos_DbccSqlText = findColumn("DbccSqlText");
					if (pos_DbccSqlText > 0)
					{
						Object cellVal = getValueAt(modelRow, pos_DbccSqlText);
						if (cellVal instanceof String)
						{
							return (String) cellVal;
						}
					}
				}
				if ("DbccSqlText".equals(colName))
				{
					return cellValue == null ? null : cellValue.toString();
				}
				
				// PROC CALL STACK
				if ("HasProcCallStack".equals(colName))
				{
					// Find 'DbccStacktrace' column, is so get it and set it as the tool tip
					int pos_ProcCallStack = findColumn("ProcCallStack");
					if (pos_ProcCallStack > 0)
					{
						Object cellVal = getValueAt(modelRow, pos_ProcCallStack);
						if (cellVal instanceof String)
						{
							return (String) cellVal;
						}
					}
				}
				if ("ProcCallStack".equals(colName))
				{
					return cellValue == null ? null : cellValue.toString();
				}
				
				// DBCC STACKTRACE
				if ("HasStacktrace".equals(colName))
				{
					// Find 'DbccStacktrace' column, is so get it and set it as the tool tip
					int pos_DbccStacktrace = findColumn("DbccStacktrace");
					if (pos_DbccStacktrace > 0)
					{
						Object cellVal = getValueAt(modelRow, pos_DbccStacktrace);
						if (cellVal instanceof String)
						{
							return (String) cellVal;
						}
					}
				}
				if ("DbccStacktrace".equals(colName))
				{
					return cellValue == null ? null : cellValue.toString();
				}
				
				return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
			}

			@Override
			public Class<?> getColumnClass(int columnIndex)
			{
				// use CHECKBOX for column "HasShowPlan"
				String colName = getColumnName(columnIndex);
				if      ("HasShowPlan"     .equals(colName)) return Boolean.class;
				else if ("HasMonSqlText"   .equals(colName)) return Boolean.class;
				else if ("HasDbccSqlText"  .equals(colName)) return Boolean.class;
				else if ("HasProcCallStack".equals(colName)) return Boolean.class;
				else if ("HasStacktrace"   .equals(colName)) return Boolean.class;
				else return super.getColumnClass(columnIndex);				
			}

			/** 
			 * Fill in the WaitEventDesc column with data from
			 * MonTableDictionary.. transforms a WaitEventId -> text description
			 * This so we do not have to do a subselect in the query that gets data
			 * doing it this way, means better performance, since the values are cached locally in memory
			 * Also do post lookups of dbcc sqltext, sp_showplan, dbcc stacktrace
			 */
			@Override
			public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
			{
//				long startTime = System.currentTimeMillis();

				Configuration conf = Configuration.getInstance(Configuration.TEMP);
				boolean getShowplan       = conf == null ? true : conf.getBooleanProperty("CMActiveStatements.sample.showplan",       true);
				boolean getMonSqltext     = conf == null ? true : conf.getBooleanProperty("CMActiveStatements.sample.monSqltext",     true);
				boolean getDbccSqltext    = conf == null ? true : conf.getBooleanProperty("CMActiveStatements.sample.dbccSqltext",    true);
				boolean getProcCallStack  = conf == null ? true : conf.getBooleanProperty("CMActiveStatements.sample.procCallStack",  true);
				boolean getDbccStacktrace = conf == null ? true : conf.getBooleanProperty("CMActiveStatements.sample.dbccStacktrace", true);

				// Where are various columns located in the Vector 
				int pos_WaitEventID        = -1, pos_WaitEventDesc  = -1, pos_WaitClassDesc = -1, pos_SPID = -1;
				int pos_HasShowPlan        = -1, pos_ShowPlanText   = -1;
				int pos_HasMonSqlText      = -1, pos_MonSqlText     = -1;
				int pos_HasDbccSqlText     = -1, pos_DbccSqlText    = -1;
				int pos_HasProcCallStack   = -1, pos_ProcCallStack  = -1;
				int pos_HasStacktrace      = -1, pos_DbccStacktrace = -1;
				int pos_BlockingOtherSpids = -1, pos_BlockingSPID   = -1;
				int pos_multiSampled       = -1;
				int pos_StartTime          = -1;
				int waitEventID = 0;
				String waitEventDesc = "";
				String waitClassDesc = "";
				SamplingCnt counters = diffData;
//				SamplingCnt prevSample = this.oldSample;
//				SamplingCnt counters = chosenData;

				MonTablesDictionary mtd = MonTablesDictionary.getInstance();
				if (mtd == null)
					return;

				if (counters == null)
					return;

				// Find column Id's
				List<String> colNames = counters.getColNames();
				if (colNames==null) return;

				for (int colId=0; colId < colNames.size(); colId++) 
				{
					String colName = (String)colNames.get(colId);
					if (colName.equals("WaitEventID"))        pos_WaitEventID        = colId;
					if (colName.equals("WaitEventDesc"))      pos_WaitEventDesc      = colId;
					if (colName.equals("WaitClassDesc"))      pos_WaitClassDesc      = colId;
					if (colName.equals("SPID"))               pos_SPID               = colId;
					if (colName.equals("HasShowPlan"))        pos_HasShowPlan        = colId;
					if (colName.equals("ShowPlanText"))       pos_ShowPlanText       = colId;
					if (colName.equals("HasMonSqlText"))      pos_HasMonSqlText      = colId;
					if (colName.equals("MonSqlText"))         pos_MonSqlText         = colId;
					if (colName.equals("HasDbccSqlText"))     pos_HasDbccSqlText     = colId;
					if (colName.equals("DbccSqlText"))        pos_DbccSqlText        = colId;
					if (colName.equals("HasProcCallStack"))   pos_HasProcCallStack   = colId;
					if (colName.equals("ProcCallStack"))      pos_ProcCallStack      = colId;
					if (colName.equals("HasStacktrace"))      pos_HasStacktrace      = colId;
					if (colName.equals("DbccStacktrace"))     pos_DbccStacktrace     = colId;
					if (colName.equals("BlockingOtherSpids")) pos_BlockingOtherSpids = colId;
					if (colName.equals("BlockingSPID"))       pos_BlockingSPID       = colId;
					if (colName.equals("multiSampled"))       pos_multiSampled       = colId;
					if (colName.equals("StartTime"))          pos_StartTime          = colId;

					// Noo need to continue, we got all our columns
					if (    pos_WaitEventID        >= 0 && pos_WaitEventDesc  >= 0 
					     && pos_WaitClassDesc      >= 0 && pos_SPID >= 0 
					     && pos_HasShowPlan        >= 0 && pos_ShowPlanText   >= 0 
					     && pos_HasMonSqlText      >= 0 && pos_MonSqlText     >= 0 
					     && pos_HasDbccSqlText     >= 0 && pos_DbccSqlText    >= 0 
					     && pos_HasProcCallStack   >= 0 && pos_ProcCallStack  >= 0 
					     && pos_HasStacktrace      >= 0 && pos_DbccStacktrace >= 0 
					     && pos_BlockingOtherSpids >= 0 && pos_BlockingSPID   >= 0
					     && pos_multiSampled       >= 0
					     && pos_StartTime          >= 0
					   )
						break;
				}

				if (pos_WaitEventID < 0 || pos_WaitEventDesc < 0 || pos_WaitClassDesc < 0)
				{
					_logger.debug("Cant find the position for columns ('WaitEventID'="+pos_WaitEventID+", 'WaitEventDesc'="+pos_WaitEventDesc+", 'WaitClassDesc'="+pos_WaitClassDesc+")");
					return;
				}
				
				if (pos_SPID < 0 || pos_HasShowPlan < 0 || pos_ShowPlanText < 0)
				{
					_logger.debug("Cant find the position for columns ('SPID'="+pos_SPID+", 'HasShowPlan'="+pos_HasShowPlan+", 'ShowPlanText'="+pos_ShowPlanText+")");
					return;
				}

				if (pos_HasDbccSqlText < 0 || pos_DbccSqlText < 0)
				{
					_logger.debug("Cant find the position for columns ('HasDbccSqlText'="+pos_HasDbccSqlText+", 'DbccSqlText'="+pos_DbccSqlText+")");
					return;
				}

				if (pos_HasProcCallStack < 0 || pos_ProcCallStack < 0)
				{
					_logger.debug("Cant find the position for columns ('HasProcCallStack'="+pos_HasProcCallStack+", 'ProcCallStack'="+pos_ProcCallStack+")");
					return;
				}

				if (pos_HasMonSqlText < 0 || pos_MonSqlText < 0)
				{
					_logger.debug("Cant find the position for columns (''HasMonSqlText'="+pos_HasMonSqlText+", 'MonSqlText'="+pos_MonSqlText+")");
					return;
				}

				if (pos_HasStacktrace < 0 || pos_DbccStacktrace < 0)
				{
					_logger.debug("Cant find the position for columns ('HasShowplan'="+pos_HasStacktrace+", 'DbccStacktrace'="+pos_DbccStacktrace+")");
					return;
				}
				
				if (pos_BlockingOtherSpids < 0 || pos_BlockingSPID < 0)
				{
					_logger.debug("Cant find the position for columns ('BlockingOtherSpids'="+pos_BlockingOtherSpids+", 'BlockingSPID'="+pos_BlockingSPID+")");
					return;
				}
				
				if (pos_multiSampled < 0)
				{
					_logger.debug("Cant find the position for columns ('multiSampled'="+pos_multiSampled+")");
					return;
				}
				
				if (pos_StartTime < 0)
				{
					_logger.debug("Cant find the position for columns ('StartTime'="+pos_StartTime+")");
					return;
				}
				
				// Loop on all diffData rows
				for (int rowId=0; rowId < counters.getRowCount(); rowId++) 
				{
					String thisRowPk = counters.getPkValue(rowId);
					int prevPkRowId = (prevSample == null) ? -1 : prevSample.getRowNumberForPkValue(thisRowPk);
					boolean prevPkExists = prevPkRowId >= 0;

					Object o_waitEventId = counters.getValueAt(rowId, pos_WaitEventID);
					Object o_SPID        = counters.getValueAt(rowId, pos_SPID);

					if (prevPkExists)
					{
						Object o_this_StartTime = counters  .getValueAt(rowId,       pos_StartTime);
						Object o_prev_StartTime = prevSample.getValueAt(prevPkRowId, pos_StartTime);

						if (o_this_StartTime instanceof Timestamp && o_prev_StartTime instanceof Timestamp)
						{
							if (o_this_StartTime.equals(o_prev_StartTime))
								counters.setValueAt("YES", rowId, pos_multiSampled);
						}
					}

					if (o_waitEventId instanceof Number)
					{
						waitEventID	  = ((Number)o_waitEventId).intValue();

						if (mtd.hasWaitEventDescription(waitEventID))
						{
							waitEventDesc = mtd.getWaitEventDescription(waitEventID);
							waitClassDesc = mtd.getWaitEventClassDescription(waitEventID);
						}
						else
						{
							waitEventDesc = "";
							waitClassDesc = "";
						}
	
						//row.set( pos_WaitEventDesc, waitEventDesc);
						counters.setValueAt(waitEventDesc, rowId, pos_WaitEventDesc);
						counters.setValueAt(waitClassDesc, rowId, pos_WaitClassDesc);
					}

					if (o_SPID instanceof Number)
					{
						int spid = ((Number)o_SPID).intValue();

						String monSqlText    = "Not properly configured (need 'SQL batch capture' & 'max SQL text monitored').";
						String dbccSqlText   = "User does not have: sa_role";
						String procCallStack = "User does not have: sa_role";
						String showplan      = "User does not have: sa_role";
						String stacktrace    = "User does not have: sa_role";

						if (getMonitorConfig("SQL batch capture") > 0 && getMonitorConfig("max SQL text monitored") > 0)
						{
							// monProcessSQLText; needs 'enable monitoring', 'SQL batch capture' and 'max SQL text monitored' configuration parameters for this monitoring table to collect data.
							if (getMonSqltext)
								monSqlText  = AseConnectionUtils.monSqlText(getMonConnection(), spid, true);
							else
								monSqlText = "This was disabled";
							if (monSqlText == null)
								monSqlText = "Not Available";
						}
						if (isRoleActive(AseConnectionUtils.SA_ROLE))
						{
							if (getDbccSqltext)
								dbccSqlText  = AseConnectionUtils.dbccSqlText(getMonConnection(), spid, true);
							else
								dbccSqlText = "This was disabled";
							if (dbccSqlText == null)
								dbccSqlText = "Not Available";
	
							if (getProcCallStack)
								procCallStack  = AseConnectionUtils.monProcCallStack(getMonConnection(), spid, true);
							else
								procCallStack = "This was disabled";
							if (procCallStack == null)
								procCallStack = "Not Available";
	
							if (getShowplan)
								showplan = AseConnectionUtils.getShowplan(getMonConnection(), spid, "Showplan:", true);
							else
								showplan = "This was disabled";
							if (showplan == null)
								showplan = "Not Available";

							if (getDbccStacktrace)
								stacktrace = AseConnectionUtils.dbccStacktrace(getMonConnection(), spid, true, waitEventID);
							else
								stacktrace = "This was disabled";
							if (showplan == null)
								stacktrace = "Not Available";
						}
						boolean b = true;
						b = !"This was disabled".equals(monSqlText)    && !"Not Available".equals(monSqlText)    && !monSqlText   .startsWith("Not properly configured");
						counters.setValueAt(new Boolean(b), rowId, pos_HasMonSqlText);
						counters.setValueAt(monSqlText,     rowId, pos_MonSqlText);

						b = !"This was disabled".equals(dbccSqlText)   && !"Not Available".equals(dbccSqlText)   && !dbccSqlText  .startsWith("User does not have");
						counters.setValueAt(new Boolean(b), rowId, pos_HasDbccSqlText);
						counters.setValueAt(dbccSqlText,    rowId, pos_DbccSqlText);

						b = !"This was disabled".equals(procCallStack) && !"Not Available".equals(procCallStack) && !procCallStack.startsWith("User does not have");
						counters.setValueAt(new Boolean(b), rowId, pos_HasProcCallStack);
						counters.setValueAt(procCallStack,  rowId, pos_ProcCallStack);

						b = !"This was disabled".equals(showplan)      && !"Not Available".equals(showplan)      && !showplan     .startsWith("User does not have");
						counters.setValueAt(new Boolean(b), rowId, pos_HasShowPlan);
						counters.setValueAt(showplan,       rowId, pos_ShowPlanText);

						b = !"This was disabled".equals(stacktrace)    && !"Not Available".equals(stacktrace)    && !stacktrace   .startsWith("User does not have");
						counters.setValueAt(new Boolean(b), rowId, pos_HasStacktrace);
						counters.setValueAt(stacktrace,     rowId, pos_DbccStacktrace);

						// Get LIST of SPID's that I'm blocking
						String blockingList = getBlockingListStr(counters, spid, pos_BlockingSPID, pos_SPID);

						counters.setValueAt(blockingList,      rowId, pos_BlockingOtherSpids);
					}
				}

//				long execTime = System.currentTimeMillis() - startTime;
//				System.out.println(getName()+".localCalculation(): execTime in ms = "+execTime);
			}
			
//			@Override
//			/** dummy wrapper just to get execution time */
//			protected int refreshGetData(Connection conn) throws Exception
//			{
//				long startTime = System.currentTimeMillis();
//
//				int retVal = super.refreshGetData(conn);
//
//				long execTime = System.currentTimeMillis() - startTime;
//				System.out.println(getName()+".refreshGetData(): execTime in ms = "+execTime);
//				
//				return retVal;
//			}

			@Override
			protected Object clone() throws CloneNotSupportedException
			{
				// TODO Auto-generated method stub
				return super.clone();
			}

			private String getBlockingListStr(SamplingCnt counters, int spid, int pos_BlockingSPID, int pos_SPID)
			{
				StringBuilder sb = new StringBuilder();

				// Loop on all diffData rows
				int rows = counters.getRowCount();
				for (int rowId=0; rowId < rows; rowId++)
				{
					Object o_BlockingSPID = counters.getValueAt(rowId, pos_BlockingSPID);
					if (o_BlockingSPID instanceof Number)
					{
						Number thisRow = (Number)o_BlockingSPID;
						if (thisRow.intValue() == spid)
						{
							Object o_SPID = counters.getValueAt(rowId, pos_SPID);
							if (sb.length() == 0)
								sb.append(o_SPID);
							else
								sb.append(", ").append(o_SPID);
						}
					}
				}
				return sb.toString();
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName())
			{
				private static final long	serialVersionUID	= 1L;

				@Override
				protected JPanel createLocalOptionsPanel()
				{
					JPanel panel = SwingUtils.createPanel("Local Options", true);
					panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));
					panel.setToolTipText(
						"<html>" +
							"All the options in this panel executes additional SQL lookups in the database <b>after</b> the result set has been delivered.<br>" +
							"This means that we are doing 1 extra SQL lookup for every checkbox option per row on the result set table.<br>" +
							"<br>" +
							"NOTE: So if you check all the options, the time to do refresh on this tab will <b>increase</b>." +
						"</html>");

					Configuration conf = Configuration.getInstance(Configuration.TEMP);
					JCheckBox sampleMonSqltext_chk     = new JCheckBox("Get Monitored SQL Text",   conf == null ? true : conf.getBooleanProperty("CMActiveStatements.sample.monSqltext",     true));
					JCheckBox sampleDbccSqltext_chk    = new JCheckBox("Get DBCC SQL Text",        conf == null ? true : conf.getBooleanProperty("CMActiveStatements.sample.dbccSqltext",    true));
					JCheckBox sampleProcCallStack_chk  = new JCheckBox("Get Procedure Call Stack", conf == null ? true : conf.getBooleanProperty("CMActiveStatements.sample.procCallStack",  true));
					JCheckBox sampleShowplan_chk       = new JCheckBox("Get Showplan",             conf == null ? true : conf.getBooleanProperty("CMActiveStatements.sample.showplan",       true));
					JCheckBox sampleDbccStacktrace_chk = new JCheckBox("Get ASE Stacktrace",       conf == null ? true : conf.getBooleanProperty("CMActiveStatements.sample.dbccStacktrace", true));

					sampleMonSqltext_chk    .setName("CMActiveStatements.sample.monSqltext");
					sampleDbccSqltext_chk   .setName("CMActiveStatements.sample.dbccSqltext");
					sampleProcCallStack_chk .setName("CMActiveStatements.sample.procCallStack");
					sampleShowplan_chk      .setName("CMActiveStatements.sample.showplan");
					sampleDbccStacktrace_chk.setName("CMActiveStatements.sample.dbccStacktrace");
					
					sampleMonSqltext_chk    .setToolTipText("<html>Do 'select SQLText from monProcessSQLText where SPID=spid' on every row in the table.<br>    This will help us to diagnose what SQL the client sent to the server.</html>");
					sampleDbccSqltext_chk   .setToolTipText("<html>Do 'dbcc sqltext(spid)' on every row in the table.<br>     This will help us to diagnose what SQL the client sent to the server.</html>");
					sampleProcCallStack_chk .setToolTipText("<html>Do 'select * from monProcessProcedures where SPID=spid.<br>This will help us to diagnose what stored procedure called before we ended up here.</html>");
					sampleShowplan_chk      .setToolTipText("<html>Do 'sp_showplan spid' on every row in the table.<br>       This will help us to diagnose if the current SQL statement is doing something funky.</html>");
					sampleDbccStacktrace_chk.setToolTipText("<html>do 'dbcc stacktrace(spid)' on every row in the table.<br>  This will help us to diagnose what peace of code the ASE Server is currently executing.</html>");

					panel.add(sampleMonSqltext_chk,     "");
					panel.add(sampleProcCallStack_chk,  "wrap");
					panel.add(sampleDbccSqltext_chk,    "wrap");
					panel.add(sampleShowplan_chk,       "wrap");
					panel.add(sampleDbccStacktrace_chk, "wrap");

					sampleMonSqltext_chk.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							Configuration conf = Configuration.getInstance(Configuration.TEMP);
							if (conf == null) return;
							conf.setProperty("CMActiveStatements.sample.monSqltext", ((JCheckBox)e.getSource()).isSelected());
							conf.save();
						}
					});
					sampleDbccSqltext_chk.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							Configuration conf = Configuration.getInstance(Configuration.TEMP);
							if (conf == null) return;
							conf.setProperty("CMActiveStatements.sample.dbccSqltext", ((JCheckBox)e.getSource()).isSelected());
							conf.save();
						}
					});
					sampleProcCallStack_chk.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							Configuration conf = Configuration.getInstance(Configuration.TEMP);
							if (conf == null) return;
							conf.setProperty("CMActiveStatements.sample.procCallStack", ((JCheckBox)e.getSource()).isSelected());
							conf.save();
						}
					});
					sampleShowplan_chk.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							Configuration conf = Configuration.getInstance(Configuration.TEMP);
							if (conf == null) return;
							conf.setProperty("CMActiveStatements.sample.showplan", ((JCheckBox)e.getSource()).isSelected());
							conf.save();
						}
					});
					sampleDbccStacktrace_chk.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							Configuration conf = Configuration.getInstance(Configuration.TEMP);
							if (conf == null) return;
							conf.setProperty("CMActiveStatements.sample.dbccStacktrace", ((JCheckBox)e.getSource()).isSelected());
							conf.save();
						}
					});
					
					return panel;
				}
			};
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_active_statements_activity.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );

			tcp.setTableToolTipText(
				    "<html>" +
				        "Background colors:" +
				        "<ul>" +
				        "    <li>ORANGE - SPID was visible in previous sample as well.</li>" +
				        "    <li>PINK   - SPID is Blocked by another SPID from running, this SPID is the Victim of a Blocking Lock, which is showned in RED.</li>" +
				        "    <li>RED    - SPID is Blocking other SPID's from running, this SPID is Responslibe or the Root Cause of a Blocking Lock.</li>" +
				        "</ul>" +
				    "</html>");

			// Mark the row as ORANGE if PK has been visible on more than 1 sample
			if (conf != null) colorStr = conf.getProperty(name+".color.multiSampled");
			tcp.addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					String multiSampled = adapter.getString(adapter.getColumnIndex("multiSampled"));
					if (multiSampled != null)
						multiSampled = multiSampled.trim();
					if ( ! "".equals(multiSampled))
						return true;
					return false;
				}
			}, SwingUtils.parseColor(colorStr, Color.ORANGE), null));

			// Mark the row as PINK if this SPID is BLOCKED by another thread
			if (conf != null) colorStr = conf.getProperty(name+".color.blocked");
			tcp.addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					String blockingSpid = adapter.getString(adapter.getColumnIndex("BlockingSPID"));
					if (blockingSpid != null)
						blockingSpid = blockingSpid.trim();
					if ( ! "0".equals(blockingSpid))
						return true;
					return false;
				}
			}, SwingUtils.parseColor(colorStr, Color.PINK), null));

			// Mark the row as RED if blocks other users from working
			if (conf != null) colorStr = conf.getProperty(name+".color.blocking");
			tcp.addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					String listOfBlockedSpids = adapter.getString(adapter.getColumnIndex("BlockingOtherSpids"));
					if (listOfBlockedSpids != null)
						listOfBlockedSpids = listOfBlockedSpids.trim();
					if ( ! "".equals(listOfBlockedSpids))
						return true;
					return false;
				}
			}, SwingUtils.parseColor(colorStr, Color.RED), null));
		}

		_CMList.add(tmp);


		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// Blocking/Blocked SPIDS
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = "CMblocking";
		displayName  = "Blocking";
		description  = "Are there any SPIDS blocked/blocking for more than 'Lock wait threshold' in the system";
		
		needVersion  = 15002;
		needCeVersion= 0;
		monTables    = new String[] {"monLocks"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring=1", "wait event timing=1"};
		colsCalcDiff = new String[] {};
		colsCalcPCT  = new String[] {};
		pkList       = null;  // no need to have PK, since we are NOT using "diff" counters
//		pkList       = new LinkedList<String>();
//		     pkList.add("LockID");

		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion,
				false, true)
		// OVERRIDE SOME DEFAULT METHODS
		{

			private static final long serialVersionUID = -5587478527730346195L;

			@Override
			public void initSql(Connection conn)
			{
//				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				cols1 = "*";
				cols2 = ", ObjectName = isnull(object_name(ObjectID, DBID), 'ObjId='+convert(varchar(30),ObjectID))"; // if user is not a valid user in A.DBID, then object_name() will return null
				cols3 = "";
//				if (aseVersion >= 15020)
//				{
//				}
				
				String whereArgs = "where BlockedState = 'Blocked' or BlockedState = 'Blocking'";

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from monLocks\n" +
					whereArgs + "\n";

				setSql(sql);
			}

		};
	
		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_blocking_spid.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );
			
			tcp.setTableToolTipText(
				    "<html>" +
				        "Background colors:" +
				        "<ul>" +
				        "    <li>PINK   - SPID is Blocked by some other SPID that holds a Lock on a database object Table, Page or Row. This is the Lock Victim.</li>" +
				        "    <li>RED    - SPID is Blocking other SPID's from running, this SPID is Responslibe or the Root Cause of a Blocking Lock.</li>" +
				        "</ul>" +
				    "</html>");

			// PINK = spid is BLOCKED by some other user
			if (conf != null) colorStr = conf.getProperty(name+".color.blocked");
			tcp.addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					String blockedState = adapter.getString(adapter.getColumnIndex("BlockedState"));
					if ("Blocked".equals(blockedState))
						return true;
					return false;
				}
			}, SwingUtils.parseColor(colorStr, Color.PINK), null));

			// Mark the row as RED if blocks other users from working
			if (conf != null) colorStr = conf.getProperty(name+".color.blocking");
			tcp.addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					String blockedState = adapter.getString(adapter.getColumnIndex("BlockedState"));
					if ("Blocking".equals(blockedState))
						return true;
					return false;
				}
			}, SwingUtils.parseColor(colorStr, Color.RED), null));
		}

		_CMList.add(tmp);
		

		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// Missing Statistics Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = "CMmissingStats";
		displayName  = "Missing Statistics";
		description  = "The optimizer executed queries where it didnt find statistics for this objects";
		
		needVersion  = 15031; // sp_configure "capture missing statistics": where intruduced in 15.0.3 ESD#1
		needCeVersion= 0;
		monTables    = new String[] {"sysstatistics"};
		needRole     = new String[] {};
		needConfig   = new String[] {"capture missing statistics"}; // NO default for this configuration
		colsCalcDiff = new String[] {"miss_counter"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList<String>();
		    pkList.add("DBName");
		    pkList.add("ObjectName");
		    pkList.add("colList");

		// NOTE: se after new CountersModel(name......):
		//       ADD dependencies for 'sp_missing_stats'

		    
		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				false, true)
		{
			private static final long serialVersionUID = -1842749890597642593L;

			@Override
			public void initSql(Connection conn)
			{
				//int aseVersion = getServerVersion();

				String sql = "exec sp_missing_stats";
				setSql(sql);
			}
		};

		// Need stored proc 'sp_missing_stats', check if it exists: if not it will be created in super.init(conn)
		tmp.addDependsOnStoredProc("sybsystemprocs", "sp_missing_stats", VersionInfo.SP_MISSING_STATS_CRDATE, VersionInfo.class, "sp_missing_stats.sql", AseConnectionUtils.SA_ROLE);

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_missing_stats_activity.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );
		}
		
		_CMList.add(tmp);


		
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// Monitor Config
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = "CMspMonitorConfig";
		displayName  = "sp_monitorconfig";
		description  = "Executes: sp_monitorconfig 'all'... Note: set postpone time to 10 minutes or so, we dont need to sample this that often.";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 0; // works on all versions
		needCeVersion= 0;
		monTables    = new String[] {"monitorconfig"};
		needRole     = new String[] {};
		needConfig   = new String[] {};
		colsCalcDiff = new String[] {};
		colsCalcPCT  = new String[] {};
		pkList       = null;
//		pkList       = new LinkedList<String>();
//		    pkList.add("Name");

		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				false, true, 600)
		{
			private static final long serialVersionUID = -1842749890597642593L;

			@Override
			public void initSql(Connection conn)
			{
				//int aseVersion = getServerVersion();

				String sql = "exec sp_monitorconfig 'all'";
				setSql(sql);
			}

			@Override
			protected CmSybMessageHandler createSybMessageHandler()
			{
				CmSybMessageHandler msgHandler = super.createSybMessageHandler();

				//msgHandler.addDiscardMsgStr("Usage information at date and time");
				msgHandler.addDiscardMsgNum(0);

				return msgHandler;
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_sp_monitorconfig.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );
		}
		
		_CMList.add(tmp);


		
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// sp_configure
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
//		name         = "CMspConfig";
//		displayName  = "sp_configure";
//		description  = "Executes: sp_configure... Note: set postpone time to 1 Hour or so, we dont need to sample this often.";
//		
//		SplashWindow.drawSplashProgress("Loading: Counter Model '"+name+"'");
//		
//		needVersion  = 0; // works on all versions
//		needCeVersion= 0;
//		monTables    = new String[] {"sysconfig"};
//		needRole     = new String[] {};
//		needConfig   = new String[] {};
//		colsCalcDiff = new String[] {};
//		colsCalcPCT  = new String[] {};
//		pkList       = null;
////		pkList       = new LinkedList<String>();
////		    pkList.add("Name");
//
//		tmp = new CountersModel(name, null, 
//				pkList, colsCalcDiff, colsCalcPCT, 
//				monTables, needRole, needConfig, needVersion, needCeVersion, 
//				false, true, 3600)
//		{
//			private static final long serialVersionUID = -1842749890597642593L;
//
//			@Override
//			public void initSql(Connection conn)
//			{
//				try 
//				{
//					MonTablesDictionary mtd = MonTablesDictionary.getInstance();
//					mtd.addTable("sysconfig",  "more or less thing as sp_configure.");
//					mtd.addColumn("sysconfig",  "NON_DEFAULT",   "True if the value is not configured to the default value. (same as for sp_configure 'nondefault')");
//					mtd.addColumn("sysconfig",  "SECTION",       "Configuration Group");
//					mtd.addColumn("sysconfig",  "NAME",          "Name of the configuration, same as the name in sp_configure.");
//					mtd.addColumn("sysconfig",  "VALUE",         "Value of the configuration.");
//					mtd.addColumn("sysconfig",  "PENDING",       "The Configuration has not yet taken effect, probably needs restart to take effect.");
//					mtd.addColumn("sysconfig",  "PENDING_VALUE", "The value which will be confihured on next restart, if PENDING is true.");
//					mtd.addColumn("sysconfig",  "DEFAULT_VALUE", "The default configuration value.");
//					mtd.addColumn("sysconfig",  "RESTART_REQ",   "ASE needs to be rebooted for the configuration to take effect.");
//					mtd.addColumn("sysconfig",  "CHAR_VALUE",    "The char value of the configuration.");
//					mtd.addColumn("sysconfig",  "CHAR_PENDING",  "The pending char configuration.");
//					mtd.addColumn("sysconfig",  "READONLY",      "This config is a read only value");
//					mtd.addColumn("sysconfig",  "TYPE",          "");
//					mtd.addColumn("sysconfig",  "MIN_VALUE",     "integer minimum value of the configuration");
//					mtd.addColumn("sysconfig",  "MAX_VALUE",     "integer maximum value of the configuration");
//					mtd.addColumn("sysconfig",  "DESCRIPTION",   "Description of the configuration.");
//					mtd.addColumn("sysconfig",  "CONFIG_ID",     "Internal ID number for this configuration.");
//					mtd.addColumn("sysconfig",  "PARENT_ID",     "Configuration Group ID");
//					mtd.addColumn("sysconfig",  "DISP_LEVEL",    "");
//					mtd.addColumn("sysconfig",  "DATA_TYPE",     "");
//				}
//				catch (NameNotFoundException e) {/*ignore*/}
//
//				
//				int aseVersion = getServerVersion();
//
//				
//				String sql = "exec sp_configure";
//				
//				// in what version was the 'display nondefault settings' introduced
//				if (aseVersion >= 15020)
//					sql = "exec sp_configure 'nondefault'";
//
//				sql = 
//					"select \n" +
//					"    NON_DEFAULT   = case when (b.defvalue != isnull(a.value2, convert(char(32), a.value)) or b.defvalue != isnull(b.value2, convert(char(32), b.value))) \n" +
//					"                               and b.config != 114 /* Exclude option 'configuration file' */ \n" +
//					"                               and b.type != 'read-only' \n" +
//					"                               and b.display_level <= 10 \n" +
//					"	                      then convert(bit, 1) \n" +
//					"	                      else convert(bit, 0) \n" +
//					"                    end, \n" +
//					"    SECTION       = (select convert(char(35),x.name) from master.dbo.sysconfigures x where a.parent = x.config), \n" + 
//					"    NAME          = convert(char(35),a.name),  \n" + 
//					"    VALUE         = b.value,  \n" + 
//					"    PENDING       = case when    a.value = b.value \n" +
//					"                              or b.type = 'read-only' \n" +
//					"                              or convert(bit,(b.status&0x10)|(b.status&0x20)) = 1 \n" +
//					"                         then convert(bit,0) \n" +
//					"                         else convert(bit,1) \n" +
//					"                    end, \n" + 
//					"    PENDING_VALUE = convert(varchar(30),nullif(a.value, b.value)), \n" + 
//					"    DEFAULT_VALUE = b.defvalue,  \n" + 
//					"    RESTART_REQ   = convert(bit, 1-convert(bit, a.status&8)),  \n" + 
//					"    CHAR_VALUE    = b.value2,  \n" + 
//					"    CHAR_PENDING  = a.value2,  \n" + 
//					"    READONLY      = convert(bit,(b.status&0x10)|(b.status&0x20)),  \n" + 
//					"    TYPE          = b.type, \n" + 
//					"    MIN_VALUE     = b.minimum_value,  \n" + 
//					"    MAX_VALUE     = b.maximum_value,  \n" + 
//					"    DESCRIPTION   = (select m.description from master.dbo.sysmessages m where m.error = b.message_num and m.langid = NULL),  \n" + 
//					"    CONFIG_ID     = a.config, \n" +
//					"    PARENT_ID     = a.parent,  \n" + 
//					"    DISP_LEVEL    = b.display_level,  \n" + 
//					"    DATA_TYPE     = b.datatype  \n" + 
//					"from master.dbo.sysconfigures a,  \n" + 
//					"     master.dbo.syscurconfigs b  \n" + 
//					"where a.config=b.config  \n" + 
//					"  and b.display_level<>null  \n" + 
//					"  and a.parent!= 19  \n" + 
//					"  and a.parent > 1  \n" + 
////					"			      and (b.defvalue != isnull(a.value2, convert(char(32), a.value))  \n" + 
////					"			        or b.defvalue != isnull(b.value2, convert(char(32), b.value)))  \n" + 
////					"			      and b.config != 114 /* Exclude option 'configuration file' */ \n" + 
////					"			      and b.type != 'read-only' \n" + 
////					"			      and b.display_level <= 10 \n" + 
//					"order by a.parent, a.name \n";
//				
//				setSql(sql);
//			}
//
//			@Override
//			protected CmSybMessageHandler createSybMessageHandler()
//			{
//				CmSybMessageHandler msgHandler = super.createSybMessageHandler();
//
//				msgHandler.addDiscardMsgNum(0);
//
//				return msgHandler;
//			}
//		};
//
//		tmp.setDisplayName(displayName);
//		tmp.setDescription(description);
//		if (Asemon.hasGUI())
//		{
//			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
//			tcp.setToolTipText( description );
//			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_sp_configure.png") );
//			tmp.setTabPanel( tcp );
//			MainFrame.addTcp( tcp );
//		}
//		
//		_CMList.add(tmp);


		



		
		
		
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// USER DEFINED COUNTERS
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		createUserDefinedCounterModels();

		// done
		_countersIsCreated = true;
	}

	/**
	 * A User Defined Counter
	 * 
	 * Can be defined from the properties file:
	 * 
	 * #-----------------------------------------------------------
	 * # Go and get information about you'r own thing
	 * # Normally used to get a some application specific counters
	 * # If it's a "load" application how many XXX have we loaded so far
	 * #-----------------------------------------------------------
	 * udc.appSpecificCounter.name=App1 loading
	 * udc.appSpecificCounter.sql=select appName, loadCount=count(*) from appTable
	 * udc.appSpecificCounter.pkPos=1
	 * udc.appSpecificCounter.diff=loadCount
	 * 
	 * 
	 * #-----------------------------------------------------------
	 * # Here is a example where we are not satisfied with the current ASEmon counters
	 * # so lets make "a new and improved" tab.
	 * #
	 * # In this example we are also using ASE version dependensies
	 * # meaning newer versions of the ASE server might have "extra" columns in there MDA tables
	 * #
	 * #
	 * # Here is a example where we are not satisfied with the current ASEmon counters
	 * # so lets make "a new and improved" tab.
	 * #
	 * # In this example we are also using ASE version dependensies
	 * # meaning newer versions of the ASE server might have "extra" columns in there MDA tables
	 * #-----------------------------------------------------------
	 * udc.procAcct.name=EXTRA PROCESS ACTIVITY
	 * udc.procAcct.sql=      select SPID, KPID, CPUTime, LogicalReads, PhysicalReads       from monProcessActivity
	 * udc.procAcct.sql.12510=select SPID, KPID, CPUTime, LogicalReads, PhysicalReads, WaitTime       from monProcessActivity
	 * udc.procAcct.sql.12520=select SPID, KPID, CPUTime, LogicalReads, PhysicalReads, WaitTime, PagesRead       from monProcessActivity
	 * udc.procAcct.sql.12530=select SPID, KPID, CPUTime, LogicalReads, PhysicalReads, WaitTime, PagesRead, PhysicalWrites, PagesWritten       from monProcessActivity
	 * udc.procAcct.sql.12540=select SPID, KPID, CPUTime, LogicalReads, PhysicalReads, WaitTime, PagesRead, PhysicalWrites, PagesWritten, MemUsageKB       from monProcessActivity
	 * udc.procAcct.sql.15010=select SPID, KPID, CPUTime, LogicalReads, PhysicalReads, WaitTime, PagesRead, PhysicalWrites, PagesWritten, MemUsageKB, NewCol1=getdate()       from monProcessActivity
	 * udc.procAcct.sql.15020=select SPID, KPID, CPUTime, LogicalReads, PhysicalReads, WaitTime, PagesRead, PhysicalWrites, PagesWritten, MemUsageKB, NewCol1=getdate(), AndAotherNewCol=getdate()       from monProcessActivity
	 * udc.procAcct.pkPos=1, 2 
	 * udc.procAcct.diff=PhysicalReads, LogicalReads, PagesRead, PhysicalWrites, PagesWritten
	 * udc.procAcct.toolTipMonTables=monProcessActivity
	 */
//	private void initUserDefinedCounterModels(int aseVersion, int monTablesVersion)
	private void createUserDefinedCounterModels()
	{
		Configuration conf = null;
		if(Asemon.hasGUI())
		{
//			conf = Asemon.getProps();
			conf = Configuration.getInstance(Configuration.CONF);
		}
		else
		{
//			conf = Asemon.getStoreProps();	
			conf = Configuration.getInstance(Configuration.PCS);
		}
		
		if (conf == null)
			return;

		createUserDefinedCounterModels(conf);
	}

	public static int createUserDefinedCounterModels(Configuration conf)
	{
		if (conf == null)
			return 1;

		int failCount = 0;

		String prefix = "udc.";

//		Vector<String> uniqueUdcNames = new Vector<String>();
//
//		// Compose a list of unique udc.xxxx. strings
//		Enumeration<String> enumL1 = conf.getKeys(prefix);
//		while(enumL1.hasMoreElements())
//		{
//			String key = enumL1.nextElement();
//			_logger.debug("Reading UDC key='"+key+"'.");
//			String name = key.substring(prefix.length(), key.indexOf(".", prefix.length()));
//			
//			if ( ! uniqueUdcNames.contains(name) )
//			{
//				uniqueUdcNames.add(name);
//			}
//		}
//
//		// Loop the above unique "names"
//		enumL1 = uniqueUdcNames.elements();
//		while(enumL1.hasMoreElements())
//		{
//			String name = enumL1.nextElement();
		for (String name : conf.getUniqueSubKeys(prefix, false))
		{
			SplashWindow.drawProgress("Loading: User Defined Counter Model '"+name+"'");
			
			String startKey = prefix + name + ".";
			
			Map<Integer, String> sqlVer = null;

			_logger.debug("STARTING TO Initializing User Defined Counter '"+name+"'.");

			// Get the individual properties
			String  udcName          = conf.getProperty(startKey    + "name");
			String  udcDisplayName   = conf.getProperty(startKey    + "displayName", udcName);
			String  udcDescription   = conf.getProperty(startKey    + "description", "");
			String  udcSqlInit       = conf.getProperty(startKey    + "sqlInit");
			String  udcSqlClose      = conf.getProperty(startKey    + "sqlClose");
			String  udcSql           = conf.getProperty(startKey    + "sql");
			String  udcPk            = conf.getProperty(startKey    + "pk");
			String  udcPkPos         = conf.getProperty(startKey    + "pkPos");
			String  udcDiff          = conf.getProperty(startKey    + "diff");
			boolean udcNDCT0  = conf.getBooleanProperty(startKey    + "negativeDiffCountersToZero", false);
			String  udcNeedRole      = conf.getProperty(startKey    + "needRole");
			String  udcNeedConfig    = conf.getProperty(startKey    + "needConfig");
			int     udcNeedVersion   = conf.getIntProperty(startKey + "needVersion", 0);
			int     udcNeedCeVersion = conf.getIntProperty(startKey + "needCeVersion", 0);
			String  udcTTMT          = conf.getProperty(startKey    + "toolTipMonTables");

			int     udcGraphType        = -1;
			boolean udcHasGraph  = conf.getBooleanProperty(startKey + "graph", false);
			String  udcGraphTypeStr     = conf.getProperty(startKey + "graph.type", "byCol");
			String  udcGraphName        = conf.getProperty(startKey + "graph.name");
			String  udcGraphLabel       = conf.getProperty(startKey + "graph.label");
			String  udcGraphMenuLabel   = conf.getProperty(startKey + "graph.menuLabel");
			String  udcGraphDataCols    = conf.getProperty(startKey + "graph.data.cols");
			String  udcGraphDataMethods = conf.getProperty(startKey + "graph.data.methods");
			String  udcGraphDataLabels  = conf.getProperty(startKey + "graph.data.labels");

			// CHECK for mandatory properties
			if (udcName == null)
			{
				_logger.error("Cant initialize User Defined Counter '"+name+"', no 'name' has been defined.");
				failCount++;
				continue;
			}
			if (udcSql == null)
			{
				_logger.error("Cant initialize User Defined Counter '"+name+"', no 'sql' has been defined.");
				failCount++;
				continue;
			}
			if (udcPkPos != null)
			{
				_logger.error("Cant initialize User Defined Counter '"+name+"', 'pkPos' are not longer supported, please use 'pk' instead.");
				failCount++;
				continue;
			}

			if (udcPk == null)
			{
				_logger.error("Cant initialize User Defined Counter '"+name+"', no 'pk' has been defined.");
				failCount++;
				continue;
			}

			// Check/get version specific SQL strings
			String sqlVersionPrefix = startKey + "sql" + ".";
			//int     sqlVersionHigh = -1;
			for (String key : conf.getKeys(sqlVersionPrefix))
			{
				String sqlVersionStr = key.substring(sqlVersionPrefix.length());
				int    sqlVersionNumInKey = 0;

				try
				{
					sqlVersionNumInKey = Integer.parseInt(sqlVersionStr);
				}
				catch(NumberFormatException ex)
				{
					_logger.warn("Problems initialize User Defined Counter '"+name+"', a sql.##### where ##### should specify ASE server version if faulty in the string '"+sqlVersionStr+"'.");
					failCount++;
					continue;
				}
				
				// Add all the udName.sql.#VERSION# to the map.
				// we will descide what version to use later on.
				if (sqlVer == null)
					sqlVer = new HashMap<Integer, String>();

				sqlVer.put(
					new Integer(sqlVersionNumInKey), 
					conf.getProperty(sqlVersionPrefix + sqlVersionNumInKey) );

//				// connected aseVersion, go and get the highest/closest sql version string 
//				if (aseVersion >= sqlVersionNumInKey)
//				{
//					if (sqlVersionNumInKey < 12503)
//					{
//						_logger.warn("Reading User Defined Counter '"+name+"' with specialized sql for version number '"+sqlVersionNumInKey+"'. First version number that we support is 12503 (which is Ase Version 12.5.0.3 in a numbered format), disregarding this entry.");
//					}
//					else
//					{
//						if (sqlVersionHigh <= sqlVersionNumInKey)
//						{
//							sqlVersionHigh = sqlVersionNumInKey;
//						}
//					}
//				}
			}
//			if (sqlVersionHigh > 0)
//			{
//				_logger.info("Initializing User Defined Counter '"+name+"' with sql using version number '"+sqlVersionHigh+"'.");
//				udcSql = conf.getProperty(sqlVersionPrefix + sqlVersionHigh);
//			}

			// Get me some array variables, that we will "spit" properties into
			List<String>     udcPkList          = new LinkedList<String>();
			String[] udcPkArray         = {};
//			String[] udcPkPosArray      = {};
			String[] udcDiffArray       = {};
			String[] udcTTMTArray       = {};
			String[] udcNeedRoleArray   = {};
			String[] udcNeedConfigArray = {};
			String[] udcPctArray        = {}; // not used, just initialized

			// Split some properties using commas "," as the delimiter  
			if (udcPk         != null) udcPkArray          = udcPk        .split(",");
//			if (udcPkPos      != null) udcPkPosArray       = udcPkPos     .split(",");
			if (udcDiff       != null) udcDiffArray        = udcDiff      .split(",");
			if (udcTTMT       != null) udcTTMTArray        = udcTTMT      .split(",");
			if (udcNeedRole   != null) udcNeedRoleArray    = udcNeedRole  .split(",");
			if (udcNeedConfig != null) udcNeedConfigArray  = udcNeedConfig.split(",");

			

			
			// Get rid of extra " " spaces from the split 
			for (int i=0; i<udcPkArray        .length; i++) udcPkArray        [i] = udcPkArray        [i].trim(); 
//			for (int i=0; i<udcPkPosArray     .length; i++) udcPkPosArray     [i] = udcPkPosArray     [i].trim(); 
			for (int i=0; i<udcDiffArray      .length; i++) udcDiffArray      [i] = udcDiffArray      [i].trim(); 
			for (int i=0; i<udcTTMTArray      .length; i++) udcTTMTArray      [i] = udcTTMTArray      [i].trim(); 
			for (int i=0; i<udcNeedRoleArray  .length; i++) udcNeedRoleArray  [i] = udcNeedRoleArray  [i].trim(); 
			for (int i=0; i<udcNeedConfigArray.length; i++) udcNeedConfigArray[i] = udcNeedConfigArray[i].trim(); 

			for (int i=0; i<udcPkArray   .length; i++) udcPkList.add( udcPkArray[i] ); 
//			int pk1 = 0;
//			int pk2 = 0;
//			int pk3 = 0;
//
//			try
//			{
//				if (udcPkPosArray.length >= 1) pk1 = Integer.parseInt(udcPkPosArray[0]);
//				if (udcPkPosArray.length >= 2) pk2 = Integer.parseInt(udcPkPosArray[1]);
//				if (udcPkPosArray.length >= 3) pk3 = Integer.parseInt(udcPkPosArray[2]);
//			}
//			catch(NumberFormatException ex)
//			{
//				_logger.error("Cant initialize User Defined Counter '"+name+"', 'pk' should be numbers pointing out the position (first col is specified with the number '1').");
//				continue;
//				failCount++;
//			}

			_logger.info("Creating User Defined Counter '"+name+"' with sql '"+udcSql+"'.");

			// Finally create the Counter model and all it's surondings...
			CountersModel cm = new CountersModelUserDefined( name, udcSql, sqlVer,
					udcPkList, //pk1, pk2, pk3, 
					udcDiffArray, udcPctArray, 
					udcTTMTArray, udcNeedRoleArray, udcNeedConfigArray, 
					udcNeedVersion, udcNeedCeVersion, 
					udcNDCT0);

			TabularCntrPanel tcp = null;
			if (Asemon.hasGUI())
			{
				tcp = new TabularCntrPanel(udcDisplayName);
				tcp.setToolTipText( udcDescription );
				tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/ud_counter_activity.png") );
				cm.setTabPanel( tcp );
				MainFrame.addTcp( tcp );
			}
			cm.setTabPanel(tcp);

			cm.setSqlInit(udcSqlInit);
			cm.setSqlClose(udcSqlClose);
			cm.setDisplayName(udcDisplayName);
			cm.setDescription(udcDescription);

			//
			// User defined graphs
			//
			if (udcHasGraph)
			{
				boolean addGraph = true;
				if (udcGraphName == null)
					udcGraphName = udcName + "Graph";

				if (udcGraphLabel == null)
					udcGraphLabel = udcName + " Graph";

				if (udcGraphMenuLabel == null)
					udcGraphMenuLabel = udcGraphLabel;

				if (udcGraphDataCols == null)
				{
					_logger.error("Cant add a graph to the User Defined Counter '"+name+"', no 'graph.data.cols' has been defined.");
					addGraph = false;
				}
				if (udcGraphDataMethods == null)
				{
					_logger.error("Cant add a graph to the User Defined Counter '"+name+"', no 'graph.data.methods' has been defined.");
					addGraph = false;
				}
				if (udcGraphDataLabels == null)
					udcGraphDataLabels = udcGraphDataCols;

				if      (udcGraphTypeStr.equalsIgnoreCase("byCol")) udcGraphType = TrendGraph.TYPE_BY_COL;
				else if (udcGraphTypeStr.equalsIgnoreCase("byRow"))	udcGraphType = TrendGraph.TYPE_BY_ROW;
				else
				{
					_logger.error("Cant add a graph to the User Defined Counter '"+name+"', no 'graph.type' can only be 'byCol' or 'byRow'.");
					addGraph = false;
				}

				String[] udcGraphDataColsArr    = {};
				String[] udcGraphDataMethodsArr = {};
				String[] udcGraphDataLabelsArr  = {};

				// Split some properties using commas "," as the delimiter  
				if (udcGraphDataCols    != null) udcGraphDataColsArr    = udcGraphDataCols   .split(",");
				if (udcGraphDataMethods != null) udcGraphDataMethodsArr = udcGraphDataMethods.split(",");
				if (udcGraphDataLabels  != null) udcGraphDataLabelsArr  = udcGraphDataLabels .split(",");

				// Get rid of extra " " spaces from the split 
				for (int i=0; i<udcGraphDataColsArr   .length; i++) udcGraphDataColsArr   [i] = udcGraphDataColsArr   [i].trim(); 
				for (int i=0; i<udcGraphDataMethodsArr.length; i++) udcGraphDataMethodsArr[i] = udcGraphDataMethodsArr[i].trim(); 
				for (int i=0; i<udcGraphDataLabelsArr .length; i++) udcGraphDataLabelsArr [i] = udcGraphDataLabelsArr [i].trim(); 

				if (udcGraphDataColsArr.length != udcGraphDataMethodsArr.length)
				{
					_logger.error("Cant add a graph to the User Defined Counter '"+name+"'. 'graph.data.cols' has "+udcGraphDataColsArr.length+" entries while 'graph.data.methods' has "+udcGraphDataMethodsArr.length+" entries, they has to be equal.");
					addGraph = false;
				}
				if (udcGraphDataColsArr.length != udcGraphDataLabelsArr.length)
				{
					_logger.error("Cant add a graph to the User Defined Counter '"+name+"'. 'graph.data.cols' has "+udcGraphDataColsArr.length+" entries while 'graph.data.labels' has "+udcGraphDataLabelsArr.length+" entries, they has to be equal.");
					addGraph = false;
				}

				for (int i=0; i<udcGraphDataMethodsArr.length; i++) 
				{
					if ( ! CountersModel.isValidGraphMethod(udcGraphDataMethodsArr[i], true))
					{
						_logger.error("Can't add a graph to the User Defined Counter '"+name+"'. 'The graph method '"+udcGraphDataMethodsArr[i]+"' is unknown.");
						_logger.error("Valid method names is: "+CountersModel.getValidGraphMethodsString(true));
						addGraph = false;
					}
				}
				if (udcGraphType == TrendGraph.TYPE_BY_ROW)
				{
					if (udcGraphDataColsArr.length > 1)
					{
						_logger.warn("Add a graph using type 'byRow' to the User Defined Counter '"+name+"'. Only the first entry in 'graph.data.cols', 'graph.data.labels', 'graph.data.methods' will be used");
					}
				}

				if (addGraph)
				{
					if (Asemon.hasGUI())
					{
						// GRAPH
						TrendGraph tg = new TrendGraph(
								udcGraphName,      // Name of the raph
								udcGraphMenuLabel, // Menu Checkbox text
								udcGraphLabel,     // Label on the graph
								udcGraphDataLabelsArr, // Labels for each plotline
								false, cm, true);
						tg.setGraphType(udcGraphType);
						tg.setGraphCalculations(udcGraphDataColsArr, udcGraphDataMethodsArr);
						cm.addTrendGraph(udcGraphName, tg, true);
					}
					
					// Data Point
					cm.setGraphType(udcGraphType);
					cm.setGraphCalculations(udcGraphDataColsArr, udcGraphDataMethodsArr);
					cm.addTrendGraphData(udcGraphName, new TrendGraphDataPoint(udcGraphName, udcGraphDataLabelsArr));
				}
			}

			_CMList.add(cm);
		}

		return failCount;
	}

	
	// Add some information to the MonTablesDictionary
	// This will serv as a dictionary for ToolTip
	public static void initExtraMonTablesDictionary()
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionary.getInstance();
			
			if (mtd == null)
				return;

			mtd.addTable("sysprocesses",  "Holds information about SybasePID or threads/users logged in to the ASE.");
			mtd.addColumn("sysprocesses", "kpid",         "Kernel Process Identifier.");
			mtd.addColumn("sysprocesses", "fid",          "SPID of the parent process, Family ID.");
			mtd.addColumn("sysprocesses", "spid",         "Session Process Identifier.");
			mtd.addColumn("sysprocesses", "program_name", "Name of the client program, the client application has to set this to a value otherwise it will be empty.");
			mtd.addColumn("sysprocesses", "dbname",       "Name of the database this user is currently using.");
			mtd.addColumn("sysprocesses", "login",        "Username that is logged in.");
			mtd.addColumn("sysprocesses", "status",       "Status that the SPID is currently in. \n'recv sleep'=waiting for incomming network trafic, \n'sleep'=various reasons but usually waiting for disk io, \n'running'=currently executing on a engine, \n'runnable'=waiting for an engine to execute its work, \n'lock sleep'=waiting for table/page/row locks, \n'sync sleep'=waiting for childs to finnish when parallel execution. \n'...'=and more statuses");
			mtd.addColumn("sysprocesses", "cmd",          "What are we doing. \n'SELECT/INSERT/UPDATE/DELETE'=quite obvious, \n'LOG SUSP'=waiting for log space to be available, \n'COND'=On a IF or equivalent SQL statement, \n'...'=and more");
			mtd.addColumn("sysprocesses", "tran_name",    "More info about what the ASE is doing. \nIf we are in CREATE INDEX it will tell you the index name. \nIf we are in BCP it will give you the tablename etc. \nThis is a good place to look at when you issue ASE administrational commands and want to know whet it really does.");
			mtd.addColumn("sysprocesses", "physical_io",  "Total number of reads/writes, this is flushed in a strange way, so do not trust the Absolute value to much...");
			mtd.addColumn("sysprocesses", "procName",     "If a procedure is executing, this will be the name of the proc. \nif you execute a sp_ proc, it could give you a procedure name that is uncorrect. \nThis due to the fact that I'm using object_name(id,dbid) and dbid is the database the SPID is currently in, while the procId is really reflecting the id of the sp_ proc which usually is located in sybsystemprocs.");
			mtd.addColumn("sysprocesses", "stmtnum",      "Statement number of the SQL batch or the procedure that is currently executing. \nThis might be faulty but it's usually a good indicator.");
			mtd.addColumn("sysprocesses", "linenum",      "Line number of the SQL batch or the procedure that is currently executing. \nThis might be faulty but it's usually a good indicator. \nIf this does NOT move between samples you may have a HEAVY SQL statement to optimize or you may waiting for a blocking lock.");
			mtd.addColumn("sysprocesses", "blocked",      "0 is a good value, otherwise it will be the SPID that we are blocked by, meaning we are waiting for that SPID to release it's locks on some objetc.");
			mtd.addColumn("sysprocesses", "time_blocked", "Number of seconds we have been blocked by other SPID's. \nThis is not a summary, it shows you how many seconds we have been waiting since we started to wait for the other SPID to finnish.");

			mtd.addColumn("sysprocesses", "tempdb_name",          "What tempdb is this SPID using for temporary storage.");
			mtd.addColumn("sysprocesses", "pssinfo_tempdb_pages", "<html>Number of pages that this SPID is using in the tempdb.<br><b>NOTE:</b> When 'ordinary user' tables are shared between users in tempdb, this counter can be faulty,<br> this due to that all spids has a local counter (in the pss structure) which is NOT inc/decremented by other users working on the same 'global' temp table.</html>");

			mtd.addColumn("monProcess",   "tempdb_name",          "What tempdb is this SPID using for temporary storage.");
			mtd.addColumn("monProcess",   "pssinfo_tempdb_pages", "<html>Number of pages that this SPID is using in the tempdb.<br><b>NOTE:</b> When 'ordinary user' tables are shared between users in tempdb, this counter can be faulty,<br> this due to that all spids has a local counter (in the pss structure) which is NOT inc/decremented by other users working on the same 'global' temp table.</html>");

			
			mtd.addColumn("monDeviceIO", "AvgServ_ms", "Calculated column, Service time on the disk. Formula is: AvgServ_ms = IOTime / (Reads + Writes). If there is few I/O's this value might be a bit off, this due to 'click ticks' is 100 ms by default");

			mtd.addColumn("monProcessStatement", "ExecTime", "The statement has been executing for ## seconds. Calculated value. Formula: ExecTime=datediff(ss, StartTime, getdate())");
			mtd.addColumn("monProcessStatement", "ExecTimeInMs", "The statement has been executing for ## milliseconds. Calculated value. Formula: ExecTimeInMs=datediff(ms, StartTime, getdate())");

			mtd.addTable("sysmonitors",   "This is basically where sp_sysmon gets it's counters.");
			mtd.addColumn("sysmonitors",  "name",         "The internal name of the counter, this is strictly Sybase ASE INTERNAL name. If the description is NOT set it's probably an 'unknown' or not that important counter.");
			mtd.addColumn("sysmonitors",  "instances",    "How many instances of this spinslock actually exists. For examples for 'default data cache' it's the number of 'cache partitions' the cache has.");
			mtd.addColumn("sysmonitors",  "grabs",        "How many times we where able to succesfuly where able to GET the spinlock.");
			mtd.addColumn("sysmonitors",  "waits",        "How many times we had to wait for other engines before we where able to grab the spinlock.");
			mtd.addColumn("sysmonitors",  "spins",        "How many times did we 'wait/spin' for other engies to release the lock. This is basically too many engines spins of the same resource.");
			mtd.addColumn("sysmonitors",  "contention",   "waits / grabs, but in the percentage form. If this goes beond 10-20% then try to add more spinlock instances.");
			mtd.addColumn("sysmonitors",  "spinsPerWait", "spins / waits, so this is numer of times we had to 'spin' before cound grab the spinlock. If we had to 'spin/wait' for other engines that hold the spinlock. Then how many times did we wait/spin' for other engies to release the lock. If this is high (more than 100 or 200) we might have to lower the numer of engines.");
			mtd.addColumn("sysmonitors",  "description",  "If it's a known sppinlock, this field would have a valid description.");
			mtd.addColumn("sysmonitors",  "id",           "The internal ID of the spinlock, for most cases this would just be a 'number' that identifies the spinlock if the spinlock itself are 'partitioned', meaning the spinlocks itselv are partitioned using some kind of 'hash' algorithm or simular.");

			// Add all "known" counter name descriptions
			mtd.addSpinlockDescription("tablockspins",           "xxxx: tablockspins,  'lock table spinlock ratio'");
			mtd.addSpinlockDescription("fglockspins",            "xxxx: fglockspins,   'lock spinlock ratio'");
			mtd.addSpinlockDescription("addrlockspins",          "xxxx: addrlockspins, 'lock address spinlock ratio'");
			mtd.addSpinlockDescription("Resource->rdesmgr_spin", "xxxx: Object Manager Spinlock Contention");
			mtd.addSpinlockDescription("Des Upd Spinlocks",      "xxxx: Object Spinlock Contention");
			mtd.addSpinlockDescription("Ides Spinlocks",         "xxxx: Index Spinlock Contention");
			mtd.addSpinlockDescription("Ides Chain Spinlocks",   "xxxx: Index Hash Spinlock Contention");
			mtd.addSpinlockDescription("Pdes Spinlocks",         "xxxx: Partition Spinlock Contention");
			mtd.addSpinlockDescription("Pdes Chain Spinlocks",   "xxxx: Partition Hash Spinlock Contention");
//			mtd.addSpinlockDescription("xxx",      "xxxx: xxx");
//			mtd.addSpinlockDescription("xxx",      "xxxx: xxx");
//			mtd.addSpinlockDescription("xxx",      "xxxx: xxx");
//			mtd.addSpinlockDescription("xxx",      "xxxx: xxx");
			
			
//
//			sp_configure "spinlock"
//			go
//
//			Parameter Name                 Default     Memory Used Config Value Run Value    Unit                 Type       
//			--------------                 -------     ----------- ------------ ---------    ----                 ----       
//			lock address spinlock ratio            100           0          100          100 ratio                static     
//			lock spinlock ratio                     85           0           85           85 ratio                static     
//			lock table spinlock ratio               20           0           20           20 ratio                static     
//			open index hash spinlock ratio         100           0          100          100 ratio                dynamic    
//			open index spinlock ratio              100           0          100          100 ratio                dynamic    
//			open object spinlock ratio             100           0          100          100 ratio                dynamic    
//			partition spinlock ratio                10           6           10           10 ratio                dynamic    
//			user log cache spinlock ratio           20           0           20           20 ratio                dynamic    
		}
		catch (NameNotFoundException e)
		{
			/* ignore */
		}
	}

	/**
	 * calls interrupt() on the refresh thread.<br>
	 * So if the refresh thread is asleap waiting for next refresh it will be interupted and
	 * start to do a new refresh of data.
	 */
	public void doInterrupt()
	{
		// interrupt the collector thread
		if (_thread != null)
		{
			_logger.debug("Sending 'interrupt' to the thread '"+_thread.getName()+"', this was done by thread '"+Thread.currentThread().getName()+"'.");
			_thread.interrupt();
		}
	}

	/** enable/continue refreshing of monitor counters from the monitored server */
	public void enableRefresh()
	{
		_refreshIsEnabled = true;

		// If we where in sleep at the getCounters loop
		if (_thread != null)
		{
			_logger.debug("Sending 'interrupt' to the thread '"+_thread.getName()+"' if it was sleeping... This was done by thread '"+Thread.currentThread().getName()+"'.");
			_thread.interrupt();
		}
	}

	/** pause/disable refreshing of monitor counters from the monitored server */
	public void disableRefresh()
	{
		_refreshIsEnabled = false;
	}

	/** if we refreshing of the counters is enabled, or paused */
	public boolean isRefreshEnabled()
	{
		return _refreshIsEnabled;
	}


	/**
	 * Are we currently getting counter information from the monitored server
	 * @return yes or no
	 */
	public boolean isRefreshingCounters()
	{
		return _refreshingCounters;
	}

	/**
	 * Indicate the we are currently in the process of getting counter information from the monitored server
	 * @param true=isRefresing
	 */
	public static void setRefreshingCounters(boolean s)
	{
		_refreshingCounters = s;
	}

	public void clearComponents()
	{
		if ( ! _isInitialized )
			return;

		if (!isRefreshingCounters())
		{
			MainFrame.clearSummaryData();

			Iterator<CountersModel> i = _CMList.iterator();
			while (i.hasNext())
			{
				CountersModel cm = i.next();
				
				if (cm != null)
				{
					cm.clear();
				}
			}
			
			SummaryPanel.getInstance().clearGraph();

//			MainFrame.statusFld.setText("");
		}
	}

	
	////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////
	//// database connection
	////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////
	private Connection         _conn                      = null;
	private long               _lastIsClosedCheck         = 0;
	private long               _lastIsClosedRefreshTime   = 1200;

	/**
	 * Do we have a connection to the database?
	 * @return true or false
	 */
	public boolean isMonConnected()
	{
		return isMonConnected(false, false);
	}
	/**
	 * Do we have a connection to the database?
	 * @return true or false
	 */
	public boolean isMonConnected(boolean forceConnectionCheck, boolean closeConnOnFailure)
	{
		if (_conn == null) 
			return false;

		// Cache the last call for X ms (default 1200 ms)
		if ( ! forceConnectionCheck )
		{
			long diff = System.currentTimeMillis() - _lastIsClosedCheck;
			if ( diff < _lastIsClosedRefreshTime)
			{
				return true;
			}
		}

		// check the connection itself
		try
		{
			// jConnect issues RPC: sp_mda 0, 7 on isClosed()
			if (_conn.isClosed())
			{
				if (closeConnOnFailure)
					closeMonConnection();
				return false;
			}
		}
		catch (SQLException e)
		{
			return false;
		}

		_lastIsClosedCheck = System.currentTimeMillis();
		return true;
	}

	/**
	 * Set the <code>Connection</code> to use for monitoring.
	 */
	public void setMonConnection(Connection conn)
	{
		_conn = conn;
//		MainFrame.setStatus(MainFrame.ST_CONNECT);
	}

	/**
	 * Gets the <code>Connection</code> to the monitored server.
	 */
	public Connection getMonConnection()
	{
		return _conn;
	}

	/** Gets the <code>Connection</code> to the monitored server. */
	public void closeMonConnection()
	{
		if (_conn == null) 
			return;

		try
		{
			if ( ! _conn.isClosed() )
			{
				_conn.close();
				if (_logger.isDebugEnabled())
				{
					_logger.debug("Connection closed");
				}
			}
		}
		catch (SQLException ev)
		{
			_logger.error("closeMonConnection", ev);
		}
		_conn = null;
	}
}
