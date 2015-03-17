/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */


package com.asetune;
import java.sql.Connection;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.asetune.cm.CountersModel;
import com.asetune.cm.iq.CmSummary;
import com.asetune.cm.iq.CmIqStatistics;
import com.asetune.cm.os.CmOsIostat;
import com.asetune.cm.os.CmOsMpstat;
import com.asetune.cm.os.CmOsUptime;
import com.asetune.cm.os.CmOsVmstat;
import com.asetune.gui.MainFrame;
import com.asetune.pcs.PersistContainer.HeaderInfo;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;

public abstract class GetCountersIq 
{
	
}

///*
// * MDA Table version information should be in the various Cm* objects
// * Or at http://www.asemon.se/usage_report.php?mda=all
// * This page has approximately 40 versions stored right now (and more is coming as someone starts a new version that we hasn't seen before)
// */
//
//public abstract class GetCountersIq 
////extends Thread
////implements ICounterController
//extends CounterControllerAbstract
//implements Runnable
//{
//	/** Log4j logging. */
//	private static Logger _logger          = Logger.getLogger(GetCountersIq.class);
//
//
//	/** This is a input to the SplashScreen */
//	public static final int	   NUMBER_OF_PERFORMANCE_COUNTERS	= 54 + 10 + 20; 
//	// 10 extra steps not for performance counters 
//	// 20 extra for init time of XX seconds or so
//
//	
//	/**
//	 * Set a specific GetCounter object to be used as the singleton.
//	 * @param cnt
//	 */
//	public static void setInstance(ICounterController cnt)
//	{
//		CounterController.setInstance(cnt);
//	}
//
////	/**
////	 * Reset All CM's etc, this so we build new SQL statements if we connect to a different ASE version<br>
////	 * Most possible called from disconnect() or similar
////	 */
////	public void reset()
////	{
////		reset(true);
////	}
//
//	/**
//	 * Reset All CM's etc, this so we build new SQL statements if we connect to a different ASE version<br>
//	 * Most possible called from disconnect() or similar
//	 * 
//	 * @param resetAllCms call reset() on all cm's
//	 */
//	@Override
//	public void reset(boolean resetAllCms)
//	{
//		super.reset(resetAllCms);
//	}
//
//
//	/** Init needs to be implemented by any subclass */
//	@Override
//	public abstract void init()
//	throws Exception;
//
//	/** The run is located in any implementing subclass */
//	@Override
//	public abstract void run();
//
//	/** shutdown or stop any collector */
//	@Override
//	public abstract void shutdown();
//	
//	/**
//	 * When we have a database connection, lets do some extra init this
//	 * so all the CountersModel can decide what SQL to use. <br>
//	 * SQL statement usually depends on what ASE Server version we monitor.
//	 * 
//	 * @param conn
//	 * @param hasGui              is this initialized with a GUI?
//	 * @param aseVersion          what is the ASE Executable version
//	 * @param isClusterEnabled    is it a cluster ASE
//	 * @param monTablesVersion    what version of the MDA tables should we use
//	 */
//	@Override
//	public void initCounters(Connection conn, boolean hasGui, int aseVersion, boolean isClusterEnabled, int monTablesVersion)
//	throws Exception
//	{
//		if (isInitialized())
//		return;
//
//		if ( ! AseConnectionUtils.isConnectionOk(conn, hasGui, null))
//			throw new Exception("Trying to initialize the counters with a connection this seems to be broken.");
//
//			
//		if (! isCountersCreated())
//			createCounters();
//		
//		_logger.info("Initializing all CM objects, using IQ server version number "+aseVersion+", isClusterEnabled="+isClusterEnabled+" with monTables Install version "+monTablesVersion+".");
//
//		// initialize all the CM's
//		Iterator<CountersModel> i = _CMList.iterator();
//		while (i.hasNext())
//		{
//			CountersModel cm = i.next();
//
//			if (cm != null)
//			{
//				_logger.debug("Initializing CM named '"+cm.getName()+"', display name '"+cm.getDisplayName()+"', using IQ server version number "+aseVersion+".");
//
//				// set the version
//				cm.setServerVersion(monTablesVersion);
//				cm.setClusterEnabled(isClusterEnabled);
//				
//				// set the active roles, so it can be used in initSql()
////				cm.setActiveRoles(_activeRoleList);
//
//				// set the ASE Monitor Configuration, so it can be used in initSql() and elsewhere
////				cm.setMonitorConfigs(monitorConfigMap);
//
//				// Now when we are connected to a server, and properties are set in the CM, 
//				// mark it as runtime initialized (or late initialization)
//				// do NOT do this at the end of this method, because some of the above stuff
//				// will be used by the below method calls.
//				cm.setRuntimeInitialized(true);
//
//				// Initializes SQL, use getServerVersion to check what we are connected to.
//				cm.initSql(conn);
//
//				// Use this method if we need to check anything in the database.
//				// for example "deadlock pipe" may not be active...
//				// If server version is below 15.0.2 statement cache info should not be VISABLE
//				cm.init(conn);
//				
//				// Initialize graphs for the version you just connected to
//				// This will simply enable/disable graphs that should not be visible for the ASE version we just connected to
//				cm.initTrendGraphForVersion(monTablesVersion);
//			}
//		}
//
//		setInitialized(true);
//	}
//
//	
//	/**
//	 * This will be used to create all the CountersModel objects.
//	 * <p>
//	 * If we are in GUI mode the graphical objects would also be 
//	 * created and added to the GUI system, this so they will be showned 
//	 * in the GUI before we are connected to any ASE Server.
//	 *  <p>
//	 * initCounters() would be called on "connect" to be able to 
//	 * initialize the counter object using a specific ASE release
//	 * this so we can decide what monitor tables and columns we could use.
//	 */
//	@Override
//	public void createCounters()
//	{
//		if (isCountersCreated())
//			return;
//
//		_logger.info("Creating ALL CM Objects.");
//
//		GetCountersIq counterController = this;
//		MainFrame   guiController     = Configuration.hasGui() ? MainFrame.getInstance() : null;
//
//		CmSummary          .create(counterController, guiController);
//
//		CmIqStatistics     .create(counterController, guiController);
//
////		CmObjectActivity   .create(counterController, guiController);
////		CmProcessActivity  .create(counterController, guiController);
////		CmSpidWait         .create(counterController, guiController);
////		CmSpidCpuWait      .create(counterController, guiController);
////		CmOpenDatabases    .create(counterController, guiController);
////		CmTempdbActivity   .create(counterController, guiController);
////		CmSysWaits         .create(counterController, guiController);
////		CmExecutionTime    .create(counterController, guiController);
////		CmEngines          .create(counterController, guiController);
////		CmThreads          .create(counterController, guiController);
////		CmSysLoad          .create(counterController, guiController);
////		CmDataCaches       .create(counterController, guiController);
////		CmCachePools       .create(counterController, guiController);
////		CmDeviceIo         .create(counterController, guiController);
////		CmIoQueueSum       .create(counterController, guiController);
////		CmIoQueue          .create(counterController, guiController);
////		CmIoControllers    .create(counterController, guiController);
////		CmSpinlockActivity .create(counterController, guiController);
////		CmSpinlockSum      .create(counterController, guiController);
////		CmSysmon           .create(counterController, guiController);
////		CmMemoryUsage      .create(counterController, guiController);
////		CmRaSenders        .create(counterController, guiController);
////		CmRaLogActivity    .create(counterController, guiController);
////		CmRaScanners       .create(counterController, guiController);
////		CmRaScannersTime   .create(counterController, guiController);
//////		CmRaSqlActivity    .create(counterController, guiController);
//////		CmRaSqlMisses      .create(counterController, guiController);
////		CmRaStreamStats    .create(counterController, guiController);
////		CmRaSyncTaskStats  .create(counterController, guiController);
////		CmRaTruncPoint     .create(counterController, guiController);
////		CmRaSysmon         .create(counterController, guiController);
////		CmCachedProcs      .create(counterController, guiController);
////		CmCachedProcsSum   .create(counterController, guiController);
////		CmProcCacheLoad    .create(counterController, guiController);
////		CmProcCallStack    .create(counterController, guiController);
////		CmCachedObjects    .create(counterController, guiController);
////		CmErrolog          .create(counterController, guiController);
////		CmDeadlock         .create(counterController, guiController);
////		CmLockTimeout      .create(counterController, guiController);
////		CmThresholdEvent   .create(counterController, guiController);
////		CmPCacheModuleUsage.create(counterController, guiController);
////		CmPCacheMemoryUsage.create(counterController, guiController);
////		CmStatementCache   .create(counterController, guiController);
////		CmStmntCacheDetails.create(counterController, guiController);
////		CmActiveObjects    .create(counterController, guiController);
////		CmActiveStatements .create(counterController, guiController);
////		CmBlocking         .create(counterController, guiController);
////		CmTableCompression .create(counterController, guiController);
////		CmMissingStats     .create(counterController, guiController);
////		CmQpMetrics        .create(counterController, guiController);
////		CmSpMonitorConfig  .create(counterController, guiController);
////		CmWorkQueues       .create(counterController, guiController);
//
//		// OS HOST Monitoring
//		CmOsIostat         .create(counterController, guiController);
//		CmOsVmstat         .create(counterController, guiController);
//		CmOsMpstat         .create(counterController, guiController);
//		CmOsUptime         .create(counterController, guiController);
//
//		// USER DEFINED COUNTERS
//		createUserDefinedCounterModels(counterController, guiController);
//		createUserDefinedCounterModelHostMonitors(counterController, guiController);
//
//		// done
//		setCountersIsCreated(true);
//	}
//
//	
//	
//	
//	
//	
//	/**
//	 * Add some information to the MonTablesDictionary<br>
//	 * This will serv as a dictionary for ToolTip
//	 */
//	public static void initExtraMonTablesDictionary()
//	{
////		try
////		{
////			MonTablesDictionary mtd = MonTablesDictionary.getInstance();
////			
////			if (mtd == null)
////				return;
////
////			String clientXxx = 
////			"<html>" +
////				"The clientname, clienthostname and clientapplname is assigned by client code with individual information.<br>" +
////				"This is useful for differentiating among clients in a system where many clients connect to Adaptive Server using the same name, host name, or application name.<br>" +
////				"It can also be used by the client as a trace to indicate what part of the client code that is executing.<br>" +
////				"<br>" +
////				"Client code has probably executed:<br>" +
////				"<code>set [clientname client_name | clienthostname host_name | clientapplname application_name] value</code>" +
////			"</html>";
////
////			
////			mtd.addTable("sysprocesses",  "Holds information about SybasePID or threads/users logged in to the ASE.");
////			mtd.addColumn("sysprocesses", "kpid",           "Kernel Process Identifier.");
////			mtd.addColumn("sysprocesses", "fid",            "SPID of the parent process, Family ID.");
////			mtd.addColumn("sysprocesses", "spid",           "Session Process Identifier.");
////			mtd.addColumn("sysprocesses", "program_name",   "Name of the client program, the client application has to set this to a value otherwise it will be empty.");
////			mtd.addColumn("sysprocesses", "dbname",         "Name of the database this user is currently using.");
////			mtd.addColumn("sysprocesses", "login",          "Username that is logged in.");
////			mtd.addColumn("sysprocesses", "status",         "Status that the SPID is currently in. \n'recv sleep'=waiting for incomming network trafic, \n'sleep'=various reasons but usually waiting for disk io, \n'running'=currently executing on a engine, \n'runnable'=waiting for an engine to execute its work, \n'lock sleep'=waiting for table/page/row locks, \n'sync sleep'=waiting for childs to finnish when parallel execution. \n'...'=and more statuses");
////			mtd.addColumn("sysprocesses", "cmd",            "What are we doing. \n'SELECT/INSERT/UPDATE/DELETE'=quite obvious, \n'LOG SUSP'=waiting for log space to be available, \n'COND'=On a IF or equivalent SQL statement, \n'...'=and more");
////			mtd.addColumn("sysprocesses", "tran_name",      "More info about what the ASE is doing. \nIf we are in CREATE INDEX it will tell you the index name. \nIf we are in BCP it will give you the tablename etc. \nThis is a good place to look at when you issue ASE administrational commands and want to know whet it really does.");
////			mtd.addColumn("sysprocesses", "physical_io",    "Total number of reads/writes, this is flushed in a strange way, so do not trust the Absolute value to much...");
////			mtd.addColumn("sysprocesses", "procName",       "If a procedure is executing, this will be the name of the proc. \nif you execute a sp_ proc, it could give you a procedure name that is uncorrect. \nThis due to the fact that I'm using object_name(id,dbid) and dbid is the database the SPID is currently in, while the procId is really reflecting the id of the sp_ proc which usually is located in sybsystemprocs.");
////			mtd.addColumn("sysprocesses", "stmtnum",        "Statement number of the SQL batch or the procedure that is currently executing. \nThis might be faulty but it's usually a good indicator.");
////			mtd.addColumn("sysprocesses", "linenum",        "Line number of the SQL batch or the procedure that is currently executing. \nThis might be faulty but it's usually a good indicator. \nIf this does NOT move between samples you may have a HEAVY SQL statement to optimize or you may waiting for a blocking lock.");
////			mtd.addColumn("sysprocesses", "blocked",        "0 is a good value, otherwise it will be the SPID that we are blocked by, meaning we are waiting for that SPID to release it's locks on some objetc.");
////			mtd.addColumn("sysprocesses", "time_blocked",   "Number of seconds we have been blocked by other SPID's. \nThis is not a summary, it shows you how many seconds we have been waiting since we started to wait for the other SPID to finnish.");
////			mtd.addColumn("sysprocesses", "hostname",       "hostname of the machine where the clinet was started. (This can be filled in by the client, meaning it could be used for something else)");
////			mtd.addColumn("sysprocesses", "ipaddr",         "IP address of the connected client");
////			mtd.addColumn("sysprocesses", "hostprocess",    "hostprocess on the machine which the clinet was started at. (This can be filled in by the client, meaning it could be used for something else)");
////			mtd.addColumn("sysprocesses", "suid",           "Sybase User ID of the user which the client logged in with.");
////			mtd.addColumn("sysprocesses", "cpu",            "cumulative cpu time used by a process in 'ticks'. This is periodically flushed by the system (see sp_configure 'cpu accounting flush interval'");
////			mtd.addColumn("sysprocesses", "clientname",     clientXxx);
////			mtd.addColumn("sysprocesses", "clienthostname", clientXxx);
////			mtd.addColumn("sysprocesses", "clientapplname", clientXxx);
////
////			mtd.addColumn("sysprocesses", "tempdb_name",          "What tempdb is this SPID using for temporary storage.");
////			mtd.addColumn("sysprocesses", "pssinfo_tempdb_pages", "<html>Number of pages that this SPID is using in the tempdb.<br><b>NOTE:</b> When 'ordinary user' tables are shared between users in tempdb, this counter can be faulty,<br> this due to that all spids has a local counter (in the pss structure) which is NOT inc/decremented by other users working on the same 'global' temp table.</html>");
////			mtd.addColumn("sysprocesses", "WaitClassDesc",        "Short description of what 'group' the WaitEventID is grouped in.");
////			mtd.addColumn("sysprocesses", "WaitEventDesc",        "Short description of what this specific WaitEventID stands for.");
////			mtd.addColumn("sysprocesses", "BatchIdDiff",          "How many 'SQL Batches' has the client sent to the server since the last sample, in Diff or Rate");
////
////			mtd.addColumn("monProcess",   "tempdb_name",          "What tempdb is this SPID using for temporary storage.");
////			mtd.addColumn("monProcess",   "pssinfo_tempdb_pages", "<html>Number of pages that this SPID is using in the tempdb.<br><b>NOTE:</b> When 'ordinary user' tables are shared between users in tempdb, this counter can be faulty,<br> this due to that all spids has a local counter (in the pss structure) which is NOT inc/decremented by other users working on the same 'global' temp table.</html>");
////
////			
//////			mtd.addColumn("monDeviceIO", "AvgServ_ms", "Calculated column, Service time on the disk. Formula is: AvgServ_ms = IOTime / (Reads + Writes). If there is few I/O's this value might be a bit off, this due to 'click ticks' is 100 ms by default");
////
////			mtd.addColumn("monProcessStatement", "ExecTime", "The statement has been executing for ## seconds. Calculated value. <b>Formula</b>: ExecTime=datediff(ss, StartTime, getdate())");
////			mtd.addColumn("monProcessStatement", "ExecTimeInMs", "The statement has been executing for ## milliseconds. Calculated value. <b>Formula</b>: ExecTimeInMs=datediff(ms, StartTime, getdate())");
////
////			mtd.addTable("sysmonitors",   "This is basically where sp_sysmon gets it's counters.");
////			mtd.addColumn("sysmonitors",  "name",         "The internal name of the counter, this is strictly Sybase ASE INTERNAL name. If the description is NOT set it's probably an 'unknown' or not that important counter.");
////			mtd.addColumn("sysmonitors",  "instances",    "How many instances of this spinslock actually exists. For examples for 'default data cache' it's the number of 'cache partitions' the cache has.");
//////			mtd.addColumn("sysmonitors",  "grabs",        "How many times we where able to succesfuly where able to GET the spinlock.");
//////			mtd.addColumn("sysmonitors",  "waits",        "How many times we had to wait for other engines before we where able to grab the spinlock.");
//////			mtd.addColumn("sysmonitors",  "spins",        "How many times did we 'wait/spin' for other engies to release the lock. This is basically too many engines spins of the same resource.");
////			mtd.addColumn("sysmonitors",  "grabs",        "Spinlock grabs as in attempted grabs for the spinlock - includes waits");
////			mtd.addColumn("sysmonitors",  "waits",        "Spinlock waits - usually a good sign of contention");
////			mtd.addColumn("sysmonitors",  "spins",        "Spinlock spins - this is the CPU spins that drives up CPU utilization. The higher the spin count, the more CPU usage and the more serious the performance impact of the spinlock on other processes not waiting");
////			mtd.addColumn("sysmonitors",  "contention",   "waits / grabs, but in the percentage form. If this goes beond 10-20% then try to add more spinlock instances.");
////			mtd.addColumn("sysmonitors",  "spinsPerWait", "spins / waits, so this is numer of times we had to 'spin' before cound grab the spinlock. If we had to 'spin/wait' for other engines that hold the spinlock. Then how many times did we wait/spin' for other engies to release the lock. If this is high (more than 100 or 200) we might have to lower the numer of engines.");
////			mtd.addColumn("sysmonitors",  "description",  "If it's a known sppinlock, this field would have a valid description.");
////			mtd.addColumn("sysmonitors",  "id",           "The internal ID of the spinlock, for most cases this would just be a 'number' that identifies the spinlock if the spinlock itself are 'partitioned', meaning the spinlocks itselv are partitioned using some kind of 'hash' algorithm or simular.");
////
////			// Add all "known" counter name descriptions
////			mtd.addSpinlockDescription("tablockspins",              "xxxx: tablockspins,  'lock table spinlock ratio'");
////			mtd.addSpinlockDescription("fglockspins",               "xxxx: fglockspins,   'lock spinlock ratio'");
////			mtd.addSpinlockDescription("addrlockspins",             "xxxx: addrlockspins, 'lock address spinlock ratio'");
////			mtd.addSpinlockDescription("Resource->rdesmgr_spin",    "Object Manager Spinlock Contention");
////			mtd.addSpinlockDescription("Des Upd Spinlocks",         "Object Spinlock Contention");
////			mtd.addSpinlockDescription("Ides Spinlocks",            "Index Spinlock Contention");
////			mtd.addSpinlockDescription("Ides Chain Spinlocks",      "Index Hash Spinlock Contention");
////			mtd.addSpinlockDescription("Pdes Spinlocks",            "Partition Spinlock Contention");
////			mtd.addSpinlockDescription("Pdes Chain Spinlocks",      "Partition Hash Spinlock Contention");
////			mtd.addSpinlockDescription("Resource->rproccache_spin", "Procedure Cache Spinlock");
////			mtd.addSpinlockDescription("Resource->rprocmgr_spin",   "Procedure Cache Manager Spinlock");
//////			mtd.addSpinlockDescription("xxx",      "xxxx: xxx");
//////			mtd.addSpinlockDescription("xxx",      "xxxx: xxx");
////			
////			
//////
//////			sp_configure "spinlock"
//////			go
//////
//////			Parameter Name                 Default     Memory Used Config Value Run Value    Unit                 Type       
//////			--------------                 -------     ----------- ------------ ---------    ----                 ----       
//////			lock address spinlock ratio            100           0          100          100 ratio                static     
//////			lock spinlock ratio                     85           0           85           85 ratio                static     
//////			lock table spinlock ratio               20           0           20           20 ratio                static     
//////			open index hash spinlock ratio         100           0          100          100 ratio                dynamic    
//////			open index spinlock ratio              100           0          100          100 ratio                dynamic    
//////			open object spinlock ratio             100           0          100          100 ratio                dynamic    
//////			partition spinlock ratio                10           6           10           10 ratio                dynamic    
//////			user log cache spinlock ratio           20           0           20           20 ratio                dynamic    
////		}
////		catch (NameNotFoundException e)
////		{
////			/* ignore */
////		}
//	}
//
//
//
//
//
//
//
//
//
//
//	@Override
//	public void checkServerSpecifics()
//	{
//		throw new RuntimeException("checkServerSpecifics(): THIS SHOULD NOT BE CALLED... will be removed later.");
//	}
//
//	@Override
//	public HeaderInfo createPcsHeaderInfo()
//	{
//		throw new RuntimeException("createPcsHeaderInfo(): THIS SHOULD NOT BE CALLED... will be removed later.");
//	}
//
//	@Override
//	public CounterCollectorThreadAbstract getCounterCollectorThread()
//	{
//		throw new RuntimeException("getCounterCollectorThread(): THIS SHOULD NOT BE CALLED... will be removed later.");
//	}
//
//	@Override
//	public void start()
//	{
//		Thread t = new Thread(this);
//		t.start();
//	}
//}
