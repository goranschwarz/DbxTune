/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */


package asemon;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import asemon.cm.CountersModelRemoteCPU;
import asemon.cm.CountersModelUserDefined;
import asemon.gui.AseMonitoringConfigDialog;
import asemon.gui.MainFrame;
import asemon.gui.SummaryPanel;
import asemon.gui.TabularCntrPanel;
import asemon.gui.TrendGraph;
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

	protected Thread _thread = null;
	protected static boolean      created      = false; 
	protected static boolean      initialized  = false; 
	protected static boolean      refreshCntr  = false;
	protected static boolean      refreshing   = false;
	
	private static String _waitEvent = "";
	
	/** Keep a list of all CounterModels that you have initialized */
	protected static List _CMList  = new ArrayList();

	public static List getCmList() { return _CMList; }

	public GetCounters() 
	{
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


	/**
	 * When we have a database connection, lets do some extra init this
	 * so all the CountersModel can decide what SQL to use. <br>
	 * SQL statement usually depends on what ASE Server version we monitor.
	 * 
	 * @param conn
	 * @param aseVersion
	 * @param monTablesVersion
	 */
	public void initCounters(Connection conn, int aseVersion, int monTablesVersion)
	{
		if (initialized)
			return;

		if (! created)
			createCounters();
		
		_logger.info("Initializing all CM objects, using ASE server version number "+aseVersion+" with monTables Install version "+monTablesVersion+".");

		Iterator i = _CMList.iterator();
		while (i.hasNext())
		{
			CountersModel cm = (CountersModel) i.next();
			
			if (cm != null)
			{
				_logger.debug("Initializing CM named '"+cm.getName()+"', display name '"+cm.getDisplayName()+"', using ASE server version number "+aseVersion+".");

				// set the version
				cm.setServerVersion(aseVersion);

				// Init SQL, use getServerVersion to check what we are connected to.
				cm.initSql();

				// Use this method if we need to check anything in the database.
				// for example "deadlock pipe" may not be active...
				// If server version is below 15.0.2 statement cache info should not be VISABLE
				cm.init(conn);
			}
		}

		initialized = true;
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
		if (created)
			return;

		_logger.info("Creating ALL CM Objects.");
		
		CountersModel tmp = null;

		String   name;
		String   displayName;
		String   description;
		String[] monTables;
		String[] colsCalcDiff;
		String[] colsCalcPCT;
		List pkList;

		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// SUMMARY Panel
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------

		name         = "CMsummary";
		displayName  = "Summary";
		description  = "Overview of how the system performs.";
		
		monTables    = new String[] {"monState"};
		colsCalcDiff = new String[] {"LockWaits", "CheckPoints", "NumDeadlocks", "Connections"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList();
//		     pkList.add("dbname");

		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, monTables, false)
		{
            private static final long serialVersionUID = -4476798027541082165L;

			public void initSql()
			{
				//int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				cols1 = "aseVersion=@@version, atAtServerName=@@servername, timeIsNow=getdate(), NetworkAddressInfo=(select min(convert(varchar(255),address_info)) from syslisteners), * ";
				cols2 = "";
				cols3 = "";

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" + 
					"from monState A \n";

				setSql(sql);
			}
		};
		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		tmp.setTabPanel(null);
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

		name         = "CMobjActivity";
		displayName  = "Objects";
		description  = "Performance information about object/tables.";
		
		monTables    = new String[] {"monOpenObjectActivity"};
		colsCalcDiff = new String[] {"LogicalReads","PhysicalReads", "APFReads", "PagesRead", "PhysicalWrites", "PagesWritten", "UsedCount", "RowsInsUpdDel", "RowsInserted", "RowsDeleted", "RowsUpdated", "Operations", "LockRequests", "LockWaits", "HkgcRequests", "HkgcPending", "HkgcOverflows", "OptSelectCount"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList();
			pkList.add("DBName");
			pkList.add("ObjectName");
			pkList.add("IndexID");


		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, monTables, true)
		{
            private static final long serialVersionUID = 6315460036795875430L;

			public void initSql()
			{
				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				cols1 = "DBName=db_name(A.DBID), ObjectName=object_name(A.ObjectID, A.DBID), A.IndexID, LogicalReads, PhysicalReads, APFReads, PagesRead, PhysicalWrites, PagesWritten, UsedCount, RowsInsUpdDel=RowsInserted+RowsDeleted+RowsUpdated, RowsInserted, RowsDeleted, RowsUpdated, Operations, LockRequests=isnull(LockRequests,0), LockWaits=isnull(LockWaits,0), ";
				cols2 = "";
				cols3 = "OptSelectCount, LastOptSelectDate, LastUsedDate";
			//	cols3 = "OptSelectCount, LastOptSelectDate, LastUsedDate, LastOptSelectDateDiff=datediff(ss,LastOptSelectDate,getdate()), LastUsedDateDiff=datediff(ss,LastUsedDate,getdate())";
			// it looked like we got "overflow" in the datediff sometimes... And I have newer really used these cols, so lets take them out for a while...
				if (aseVersion >= 15020)
				{
					cols2 = "HkgcRequests, HkgcPending, HkgcOverflows, ";
				}

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from monOpenObjectActivity A \n" +
					"where UsedCount > 0 OR LockRequests > 0 OR LogicalReads > 100 OR PagesWritten > 100 --OR Operations > 1 \n" +
					"order by 1,2,3\n";

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
		
		monTables    = new String[] {"monProcessActivity", "monProcess", "sysprocesses", "monProcessNetIO"};
		colsCalcDiff = new String[] {"BatchIdDiff", "cpu", "physical_io", "CPUTime", "WaitTime", "LogicalReads", "PhysicalReads", "PagesRead", "PhysicalWrites", "PagesWritten", "TableAccesses","IndexAccesses", "TempDbObjects", "ULCBytesWritten", "ULCFlushes", "ULCFlushFull", "Transactions", "Commits", "Rollbacks", "PacketsSent", "PacketsReceived", "BytesSent", "BytesReceived", "WorkTables"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList();
		     pkList.add("SPID");
		     pkList.add("KPID");

		
		
		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, monTables, true)
		{
            private static final long serialVersionUID = 161126670167334279L;

            public void initSql()
			{
				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				cols1="  MP.FamilyID, MP.SPID, MP.KPID, MP.NumChildren, \n"
					+ "  SP.status, MP.WaitEventID, \n"
					+ "  WaitEventDesc=convert(varchar(50),''), " // value will be replaced in method localCalculation()
					+ "--WaitEventDesc=(select Description from monWaitEventInfo x where MP.WaitEventID = x.WaitEventID), \n"
					+ "  MP.SecondsWaiting, MP.BlockingSPID, MP.Command, \n"
					+ "  MP.BatchID, BatchIdDiff=convert(int,MP.BatchID), \n" // BatchIdDiff diff calculated
					+ "  procName = object_name(SP.id, SP.dbid), SP.stmtnum, SP.linenum, \n"
					+ "  MP.Application, MP.DBName, MP.Login, MP.SecondsConnected, \n"
					+ "  SP.tran_name, SP.cpu, SP.physical_io, \n"
					+ "  A.CPUTime, A.WaitTime, A.LogicalReads, \n"
					+ "  A.PhysicalReads, A.PagesRead, A.PhysicalWrites, A.PagesWritten, \n";
				cols2 = "";
				if (aseVersion >= 12520)
				{
					cols2="  A.WorkTables, \n";
				}
				if (aseVersion >= 15025)
				{
					cols2+="  N.NetworkEngineNumber, MP.ServerUserID, \n";
				}
				cols3="  A.TableAccesses, A.IndexAccesses, A.TempDbObjects, \n"
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
					"  and MP.KPID = N.KPID \n";

				setSql(sql);
			}
			/** 
			 * Fill in the WaitEventDesc column with data from
			 * MonTableDictionary.. transforms a WaitEventId -> text description
			 * This so we do not have to do a subselect in the query that gets data
			 * doing it this way, means better performance, since the values are cached locally in memory
			 */
			public void localCalculation()
			{
				// Where are various columns located in the Vector 
				int pos_WaitEventID = -1, pos_WaitEventDesc = -1;
				int waitEventID = 0;
				String waitEventDesc = "";
				SamplingCnt counters = diffData;
//				SamplingCnt counters = chosenData;
			
				MonTablesDictionary mtd = MonTablesDictionary.getInstance();
				if (mtd == null)
					return;

				if (counters == null)
					return;

				// Find column Id's
				Vector colNames = counters.getColNames();
				if (colNames==null) return;

				for (int colId=0; colId < colNames.size(); colId++) 
				{
					String colName = (String)colNames.get(colId);
					if (colName.equals("WaitEventID"))   pos_WaitEventID   = colId;
					if (colName.equals("WaitEventDesc")) pos_WaitEventDesc = colId;
					
					// Noo need to continue, we got all our columns
					if (pos_WaitEventID >= 0 && pos_WaitEventDesc >= 0)
						break;
				}

				if (pos_WaitEventID < 0 || pos_WaitEventDesc < 0)
				{
					_logger.debug("Cant find the position for columns ('WaitEventID'="+pos_WaitEventID+", 'WaitEventDesc'="+pos_WaitEventDesc+")");
					return;
				}
				
				// Loop on all diffData rows
				for (int rowId=0; rowId < counters.getRowCount(); rowId++) 
				{
//					Vector row = (Vector)counters.rows.get(rowId);
//					Object o = row.get(pos_WaitEventID);
					Object o = counters.getValueAt(rowId, pos_WaitEventID);

					if (o instanceof Number)
					{
						waitEventID	  = ((Number)o).intValue();

						if (mtd.hasWaitEventDescription(waitEventID))
							waitEventDesc = mtd.getWaitEventDescription(waitEventID);
						else
							waitEventDesc = "";
	
						//row.set( pos_WaitEventDesc, waitEventDesc);
						counters.setValueAt(waitEventDesc, rowId, pos_WaitEventDesc);
					}
				}
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		tmp.setDiffDissColumns( new String[] {"WaitTime"} );
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_process_activity.png") );
			tcp.setTableToolTipText("Double-click a row to get the details for the SPID.");
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
		name         = "CMdbActivity";
		displayName  = "Databases";
		description  = "Various information on a database level.";
		
		monTables    = new String[] {"monOpenDatabases"};
		colsCalcDiff = new String[] {"AppendLogRequests", "AppendLogWaits"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList();
		     pkList.add("DBName");


		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, monTables, true)
		{
            private static final long serialVersionUID = 5078336367667465709L;

			public void initSql()
			{
				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				cols1 = "DBName, DBID, AppendLogRequests, AppendLogWaits, TransactionLogFull, SuspendedProcesses, BackupInProgress, LastBackupFailed, BackupStartTime, ";
				cols2 = "";
				cols3 = "QuiesceTag";
				if (aseVersion >= 15010 || (aseVersion >= 12540 && aseVersion <= 15000) )
				{
				}
				if (aseVersion >= 15025)
				{
					cols2 += "LastTranLogDumpTime, LastCheckpointTime, ";
				}

				// select DBName, DBID, AppendLogRequests, AppendLogWaits, TransactionLogFull, SuspendedProcesses, BackupInProgress, LastBackupFailed, BackupStartTime, QuiesceTag from monOpenDatabases order by DBName
				// The above I had in the Properties file for a long time
				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
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
		// Waits Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = "CMsysWaitActivity";
		displayName  = "Waits";
		description  = "What different resources are the ASE Server waiting for.";
		
		monTables    = new String[] {"monSysWaits", "monWaitEventInfo", "monWaitClassInfo"};
		colsCalcDiff = new String[] {"WaitTime", "Waits"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList();
		     pkList.add("WaitEventID");

	
		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, monTables, true)
		{
            private static final long serialVersionUID = -945885495581439333L;

			public void initSql()
			{
				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";


				cols1 = "Class=C.Description, Event=I.Description, W.WaitEventID, WaitTime, Waits";
				if (aseVersion >= 15010 || (aseVersion >= 12540 && aseVersion <= 15000) )
				{
				}

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from monSysWaits W, monWaitEventInfo I, monWaitClassInfo C \n" +
					"where W.WaitEventID=I.WaitEventID and I.WaitClassID=C.WaitClassID \n" +
					"order by 1,2,3\n";

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
		
		monTables    = new String[] {"monEngine"};
		colsCalcDiff = new String[] {"CPUTime", "SystemCPUTime", "UserCPUTime", "IdleCPUTime", "Yields", "DiskIOChecks", "DiskIOPolled", "DiskIOCompleted", "ContextSwitches", "HkgcPendingItems", "HkgcOverflows"};
		colsCalcPCT  = new String[] {"CPUTime", "SystemCPUTime", "UserCPUTime", "IdleCPUTime"};
		pkList       = new LinkedList();
		     pkList.add("EngineNumber");


		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, monTables, true)
		// OVERRIDE SOME DEFAULT METHODS
		{
            private static final long serialVersionUID = 3975695722601723795L;

			public void initSql()
			{
				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				cols1 = "EngineNumber, CurrentKPID, PreviousKPID, CPUTime, SystemCPUTime, UserCPUTime, IdleCPUTime, ContextSwitches, Connections, ";
				cols2 = "";
				cols3 = "ProcessesAffinitied, Status, StartTime, StopTime, AffinitiedToCPU, OSPID";
				if (aseVersion >= 12532)
				{
					cols2 = "Yields, DiskIOChecks, DiskIOPolled, DiskIOCompleted, ";
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
					"order by 1\n";

				setSql(sql);
			}

//			/** Check if any graphs is active. */
//			public boolean hasActiveGraphs()
//			{
//				return (MainFrame.summaryPanel.isGraphCpuEnabled() || MainFrame.summaryPanel.isGraphEngineCpuEnabled());
//			}
//			public void updateGraphs()
//			{
//				if ( MainFrame.summaryPanel.isGraphCpuEnabled() )
//				{
//					Double[] cpuArray = new Double[3];
//
//					//String[] seriesName = {"System+User CPU", "System CPU", "User CPU"};
//					cpuArray[0] = this.getDiffValueAvg("CPUTime");
//					cpuArray[1] = this.getDiffValueAvg("SystemCPUTime");
//					cpuArray[2] = this.getDiffValueAvg("UserCPUTime");
//					SummaryPanel.trendCPU.addPoint((java.util.Date) this.getTimestamp(), cpuArray);
//				}
//
//				// Refresh CPU Usage per engines
//				if ( MainFrame.summaryPanel.isGraphEngineCpuEnabled() )
//				{
//					Double[] engCpuArray = new Double[this.size()];
//					String[] engNumArray = new String[engCpuArray.length];
//					for (int i = 0; i < engCpuArray.length; i++)
//					{
//						engCpuArray[i] = this.getDiffValueAsDouble(i, "CPUTime");
//						engNumArray[i] = "eng-" + i;
//					}
//					SummaryPanel.trendEngCPU.addPoint((java.util.Date) this.getTimestamp(), engCpuArray, engNumArray);
//				}
//			}
			// Check if any graphs is active.
			public boolean hasActiveGraphs()
			{
				boolean active = false;
				TrendGraph tgCpuSum = getTrendGraph("cpuSum");
				TrendGraph tgCpuEng = getTrendGraph("cpuEng");
				
				if (tgCpuSum != null && tgCpuSum.isGraphEnabled())
					active = true;
				if (tgCpuEng != null && tgCpuEng.isGraphEnabled())
					active = true;

				return active;
			}
			public void updateGraphs()
			{
				TrendGraph tgCpuSum = getTrendGraph("cpuSum");
				TrendGraph tgCpuEng = getTrendGraph("cpuEng");

				if ( tgCpuSum != null && tgCpuSum.isGraphEnabled() )
				{
					Double[] arr = new Double[3];

					arr[0] = this.getDiffValueAvg("CPUTime");
					arr[1] = this.getDiffValueAvg("SystemCPUTime");
					arr[2] = this.getDiffValueAvg("UserCPUTime");
					_logger.debug("cpuSum.addpoint: CPUTime='"+arr[0]+"', SystemCPUTime='"+arr[1]+"', UserCPUTime='"+arr[2]+"'.");
					tgCpuSum.addPoint((java.util.Date) this.getTimestamp(), arr);
				}

				if ( tgCpuEng != null && tgCpuEng.isGraphEnabled() )
				{
					Double[] engCpuArray = new Double[this.size()];
					String[] engNumArray = new String[engCpuArray.length];
					for (int i = 0; i < engCpuArray.length; i++)
					{
						engCpuArray[i] = this.getDiffValueAsDouble(i, "CPUTime");
						engNumArray[i] = "eng-" + i;
					}
					tgCpuEng.addPoint((java.util.Date) this.getTimestamp(), engCpuArray, engNumArray);
				}
			}

			/** 
			 * Compute the CPU times in pct instead of numbers of usage seconds since last sample
			 */
			public void localCalculation()
			{
				// Compute the avgServ column, which is IOTime/(Reads+APFReads+Writes)
				int CPUTime,       SystemCPUTime,       UserCPUTime,       IdleCPUTime;
				int CPUTimeId = 0, SystemCPUTimeId = 0, UserCPUTimeId = 0, IdleCPUTimeId = 0;
			
				// Find column Id's
				Vector colNames = diffData.getColNames();
				if (colNames==null) return;
			
				for (int colId=0; colId < colNames.size(); colId++) 
				{
					String colName = (String)colNames.get(colId);
					if (colName.equals("CPUTime"))       CPUTimeId       = colId;
					if (colName.equals("SystemCPUTime")) SystemCPUTimeId = colId;
					if (colName.equals("UserCPUTime"))   UserCPUTimeId   = colId;
					if (colName.equals("IdleCPUTime"))   IdleCPUTimeId   = colId;
				}
			
				// Loop on all diffData rows
				for (int rowId=0; rowId < diffData.getRowCount(); rowId++) 
				{
					CPUTime	=       ((Number)diffData.getValueAt(rowId, CPUTimeId      )).intValue();
					SystemCPUTime = ((Number)diffData.getValueAt(rowId, SystemCPUTimeId)).intValue();
					UserCPUTime =   ((Number)diffData.getValueAt(rowId, UserCPUTimeId  )).intValue();
					IdleCPUTime =   ((Number)diffData.getValueAt(rowId, IdleCPUTimeId  )).intValue();

					if (_logger.isDebugEnabled())
						_logger.debug("----CPUTime = "+CPUTime+", SystemCPUTime = "+SystemCPUTime+", UserCPUTime = "+UserCPUTime+", IdleCPUTime = "+IdleCPUTime);

					// Handle divided by 0... (this happens if a engine goes offline
					BigDecimal calcCPUTime       = null;
					BigDecimal calcSystemCPUTime = null;
					BigDecimal calcUserCPUTime   = null;
					BigDecimal calcIdleCPUTime   = null;

					if( CPUTime == 0 )
					{
						calcCPUTime       = new BigDecimal( 0 );
						calcSystemCPUTime = new BigDecimal( 0 );
						calcUserCPUTime   = new BigDecimal( 0 );
						calcIdleCPUTime   = new BigDecimal( 0 );
					}
					else
					{
						calcCPUTime       = new BigDecimal( ((1.0 * (SystemCPUTime + UserCPUTime)) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
						calcSystemCPUTime = new BigDecimal( ((1.0 * (SystemCPUTime              )) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
						calcUserCPUTime   = new BigDecimal( ((1.0 * (UserCPUTime                )) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
						calcIdleCPUTime   = new BigDecimal( ((1.0 * (IdleCPUTime                )) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
					}

					if (_logger.isDebugEnabled())
						_logger.debug("++++CPUTime = "+calcCPUTime+", SystemCPUTime = "+calcSystemCPUTime+", UserCPUTime = "+calcUserCPUTime+", IdleCPUTime = "+calcIdleCPUTime);
			
					diffData.setValueAt(calcCPUTime,       rowId, CPUTimeId       );
					diffData.setValueAt(calcSystemCPUTime, rowId, SystemCPUTimeId );
					diffData.setValueAt(calcUserCPUTime,   rowId, UserCPUTimeId   );
					diffData.setValueAt(calcIdleCPUTime,   rowId, IdleCPUTimeId   );
			
				}
			}
		};
		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
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
					"CPU Summary of all Engines", // Label 
					new String[] { "System+User CPU", "System CPU", "User CPU" }, 
					true, tmp);
			tmp.addTrendGraph("cpuSum", tgCpuSum, true);

			TrendGraph tgCpuEng = new TrendGraph("cpuEng",
					"CPU per Engine", // Menu Checkbox text
					"CPU Usage per Engine (System + User)", // Label 
					new String[] { "eng-0" }, 
					true, tmp);
			tmp.addTrendGraph("cpuEng", tgCpuEng, true);
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
		
		monTables    = new String[] {"monDataCache"};
		colsCalcDiff = new String[] {"CacheSearches", "PhysicalReads", "LogicalReads", "PhysicalWrites", "Stalls"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList();
		     pkList.add("CacheName");


		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, monTables, true)
		// OVERRIDE SOME DEFAULT METHODS
		{
            private static final long serialVersionUID = -2185972180915326688L;

            public void initSql()
			{
				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				cols1 = "CacheName, CacheSearches, PhysicalReads, LogicalReads, PhysicalWrites, Stalls";
				if (aseVersion >= 15010 || (aseVersion >= 12540 && aseVersion <= 15000) )
				{
				}

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from monDataCache \n" +
					"order by 1\n";

				setSql(sql);
			}

			// Check if any graphs is active.
			public boolean hasActiveGraphs()
			{
				boolean active = false;
				TrendGraph tg = getTrendGraph("CacheGraph");
				if (tg != null && tg.isGraphEnabled())
					active = true;

				return active;
			}
			public void updateGraphs()
			{
				TrendGraph tg = getTrendGraph("CacheGraph");
				if ( tg != null && tg.isGraphEnabled() )
				{
					Double[] arr = new Double[3];

					arr[0] = this.getRateValueSum("LogicalReads");
					arr[1] = this.getRateValueSum("PhysicalReads");
					arr[2] = this.getRateValueSum("PhysicalWrites");
					_logger.debug("CacheGraph.addpoint: LogicalReads='"+arr[0]+"', PhysicalReads='"+arr[1]+"', PhysicalWrites='"+arr[2]+"'.");
					tg.addPoint((java.util.Date) this.getTimestamp(), arr);
				}
//				if ( MainFrame.summaryPanel.isGraphDataCachesEnabled() )
//				{
//					Double[] cacheArray = new Double[3];
//
//					cacheArray[0] = this.getRateValueSum("LogicalReads");
//					cacheArray[1] = this.getRateValueSum("PhysicalReads");
//					cacheArray[2] = this.getRateValueSum("PhysicalWrites");
//					SummaryPanel.trendDataCaches.addPoint((java.util.Date) this.getTimestamp(), cacheArray);
//				}
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
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
					false, tmp);
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
		
		monTables    = new String[] {"monCachePool"};
		colsCalcDiff = new String[] {"PagesRead", "PhysicalReads", "Stalls", "PagesTouched", "BuffersToMRU", "BuffersToLRU"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList();
		     pkList.add("CacheName");
		     pkList.add("IOBufferSize");


		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, monTables, true)
		{
            private static final long serialVersionUID = -6929175820005726806L;

			public void initSql()
			{
				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				cols1 = "CacheName, IOBufferSize, AllocatedKB, PagesRead, PhysicalReads, Stalls, PagesTouched, BuffersToMRU, BuffersToLRU";
				if (aseVersion >= 15010 || (aseVersion >= 12540 && aseVersion <= 15000) )
				{
				}

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from monCachePool \n" +
					"order by 1,2,3\n";

				setSql(sql);
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
		
		monTables    = new String[] {"monDeviceIO"};
		colsCalcDiff = new String[] {"Reads", "APFReads", "Writes", "DevSemaphoreRequests", "DevSemaphoreWaits", "IOTime"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList();
		     pkList.add("LogicalName");

		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, monTables, true)
		// OVERRIDE SOME DEFAULT METHODS
		{
            private static final long serialVersionUID = 688571813560006014L;

			public void initSql()
			{
				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				cols1 = "LogicalName, Reads, APFReads, Writes, DevSemaphoreRequests, DevSemaphoreWaits, IOTime, \n";
				cols2 = "AvgServ_ms = convert(numeric(10,1),null), PhysicalName";
//				cols2 = "AvgServ_ms = " +
//						"case " +
//						"  when Reads+Writes>0 " +
//						"    then convert(numeric(18,2),IOTime/convert(numeric(18,0),Reads+Writes)) " +
//						"  else   convert(numeric(18,2), null) " +
//						"end";
				if (aseVersion >= 15010 || (aseVersion >= 12540 && aseVersion <= 15000) )
				{
				}

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from monDeviceIO\n";

				setSql(sql);
			}
				
			/** Check if any graphs is active. */
//--------------------------------------------------------
// BEGIN: THIS SHOULD BE COMMENTED OUT
//--------------------------------------------------------
//			public boolean hasActiveGraphs()
//			{
//				return MainFrame.summaryPanel.isGraphDeviceIoEnabled();
//			}
//			public void updateGraphs()
//			{
//				Double[] srvcTimeArray = new Double[2];
//
//				if ( MainFrame.summaryPanel.isGraphDeviceIoEnabled() )
//				{
//					Double IoNum = CMIOQueueActivity.getDiffValueSum("IOs");
//					Double IoTime = CMIOQueueActivity.getDiffValueSum("IOTime");
//					double avgIoServiceTime = 0;
//					if (IoNum != null && IoNum.doubleValue() > 0)
//					{
//						avgIoServiceTime = IoTime.doubleValue() / IoNum.doubleValue();
//					}
//					srvcTimeArray[0] = this.getDiffValueMax("AvgServ_ms");
//					srvcTimeArray[1] = new Double(avgIoServiceTime);
//					_logger.debug("IoNum=" + IoNum + ", IoTime=" + IoTime + ", IoServiceTime=" + avgIoServiceTime + ", Max AvgServ_ms=" + srvcTimeArray[0]);
//					SummaryPanel.trendDeviceIo.addPoint((java.util.Date) this.getTimestamp(), srvcTimeArray);
//				}
//			}
//--------------------------------------------------------
// END: THIS SHOULD BE COMMENTED OUT
//--------------------------------------------------------
			/** 
			 * Compute the avgServ column, which is IOTime/(Reads+Writes)
			 */
			public void localCalculation()
			{
				double AvgServ_ms;
				//int Reads, APFReads, Writes, IOTime;
				//int ReadsColId = 0, APFReadsColId = 0, WritesColId = 0, IOTimeColId = 0, AvgServ_msColId = 0;
				int Reads, Writes, IOTime;
				int ReadsColId = 0, WritesColId = 0, IOTimeColId = 0, AvgServ_msColId = 0;

				// Find column Id's
				Vector colNames = diffData.getColNames();
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
					Reads    = ((Integer) diffData.getValueAt(rowId, ReadsColId))   .intValue();
					Writes   = ((Integer) diffData.getValueAt(rowId, WritesColId))  .intValue();
					IOTime   = ((Integer) diffData.getValueAt(rowId, IOTimeColId))  .intValue();

					// int totIo = Reads + APFReads + Writes;
					int totIo = Reads + Writes;
					if (totIo != 0)
					{
						// AvgServ_ms = (IOTime * 1000) / ( totIo);
						AvgServ_ms = IOTime / totIo;

						// Keep only 3 decimals
						// row.set(AvgServ_msColId, new Double (AvgServ_ms/1000) );
						diffData.setValueAt(new Double(AvgServ_ms), rowId, AvgServ_msColId);
					}
					else
						diffData.setValueAt(null, rowId, AvgServ_msColId);
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
		
		monTables    = new String[] {"monIOQueue"};
		colsCalcDiff = new String[] {"IOs", "IOTime"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList();
		     pkList.add("IOType");

		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, monTables, true)
		// OVERRIDE SOME DEFAULT METHODS
		{ 
            private static final long serialVersionUID = -8676587973889879799L;

            public void initSql()
			{
				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				cols1 = "IOType, " +
						"IOs        = sum(convert(numeric(18,0), IOs)), " +
						"IOTime     = sum(convert(numeric(18,0), IOTime)), " +
						"AvgServ_ms = convert(numeric(10,1), null)";
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

			// Check if any graphs is active.
			public boolean hasActiveGraphs()
			{
				boolean active = false;
				TrendGraph tg = getTrendGraph("diskIo");
				if (tg != null && tg.isGraphEnabled())
					active = true;

				return active;
			}
			public void updateGraphs()
			{
//				if ( MainFrame.summaryPanel.isGraphOridinaryIoEnabled() )
//				{
//					Double[] ordIoArray = new Double[2];
//
//					ordIoArray[0] = this.getRateValue("User Log", "IOs");
//					ordIoArray[1] = this.getRateValue("User Data", "IOs");
//					SummaryPanel.trendOrdinaryIO.addPoint((java.util.Date) this.getTimestamp(), ordIoArray);
//				}
//
//				if ( MainFrame.summaryPanel.isGraphTempdbIoEnabled() )
//				{
//					Double[] tempdbIoArray = new Double[2];
//
//					tempdbIoArray[0] = this.getRateValue("Tempdb Log", "IOs");
//					tempdbIoArray[1] = this.getRateValue("Tempdb Data", "IOs");
//					SummaryPanel.trendTempdbIO.addPoint((java.util.Date) this.getTimestamp(), tempdbIoArray);
//				}

				TrendGraph tg = getTrendGraph("diskIo");
				if ( tg != null && tg.isGraphEnabled() )
				{
					Double[] ioArray = new Double[5];
					ioArray[0] = this.getRateValue("User Data",   "IOs");
					ioArray[1] = this.getRateValue("User Log",    "IOs");
					ioArray[2] = this.getRateValue("Tempdb Data", "IOs");
					ioArray[3] = this.getRateValue("Tempdb Log",  "IOs");
					ioArray[4] = this.getRateValue("System",      "IOs");
					tg.addPoint((java.util.Date) this.getTimestamp(), ioArray);
				}
			}
			public void localCalculation()
			{
				double AvgServ_ms;
				long   IOs, IOTime;
				int    IOsColId = 0, IOTimeColId = 0, AvgServ_msColId = 0;

				// Find column Id's
				Vector colNames = diffData.getColNames();
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
						diffData.setValueAt(new Double(AvgServ_ms), rowId, AvgServ_msColId);
					}
					else
						diffData.setValueAt(null, rowId, AvgServ_msColId);
				}
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
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
					false, tmp);
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
		
		monTables    = new String[] {"monIOQueue"};
		colsCalcDiff = new String[] {"IOs", "IOTime"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList();
		     pkList.add("LogicalName");
		     pkList.add("IOType");

		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, monTables, true)
		// OVERRIDE SOME DEFAULT METHODS
		{
            private static final long serialVersionUID = 989816816267986305L;

            public void initSql()
			{
				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				cols1 = "LogicalName, IOType, IOs, IOTime, AvgServ_ms = convert(numeric(10,1),null)";
				if (aseVersion >= 15010 || (aseVersion >= 12540 && aseVersion <= 15000) )
				{
				}

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from monIOQueue \n" +
					"order by 1, 2\n";

				setSql(sql);
			}

			public boolean hasActiveGraphs()
			{
				boolean active = false;
				TrendGraph tg = getTrendGraph("devSvcTime");
				if (tg != null && tg.isGraphEnabled())
					active = true;

				return active;
			}
			public void updateGraphs()
			{
//				Double[] srvcTimeArray = new Double[2];
//
//				if ( MainFrame.summaryPanel.isGraphDeviceIoEnabled() )
//				{
//					srvcTimeArray[0] = this.getDiffValueMax("AvgServ_ms"); // MAX
//					srvcTimeArray[1] = this.getDiffValueAvg("AvgServ_ms"); // AVG
//					_logger.debug("GraphDeviceIo: MaxServiceTime=" + srvcTimeArray[0] + ", AvgServiceTime=" + srvcTimeArray[1] + ".");
//					SummaryPanel.trendDeviceIo.addPoint((java.util.Date) this.getTimestamp(), srvcTimeArray);
//				}
				TrendGraph tg = getTrendGraph("devSvcTime");
				if ( tg != null && tg.isGraphEnabled() )
				{
					Double[] srvcTimeArray = new Double[2];
					srvcTimeArray[0] = this.getDiffValueMax("AvgServ_ms"); // MAX
					srvcTimeArray[1] = this.getDiffValueAvgGtZero("AvgServ_ms"); // AVG
					_logger.debug("devSvcTime: MaxServiceTime=" + srvcTimeArray[0] + ", AvgServiceTime=" + srvcTimeArray[1] + ".");
					tg.addPoint((java.util.Date) this.getTimestamp(), srvcTimeArray);
				}
			}
			public void localCalculation()
			{
				double AvgServ_ms;
				long   IOs, IOTime;
				int    IOsColId = 0, IOTimeColId = 0, AvgServ_msColId = 0;

				// Find column Id's
				Vector colNames = diffData.getColNames();
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
						diffData.setValueAt(new Double(AvgServ_ms), rowId, AvgServ_msColId);
					}
					else
						diffData.setValueAt(null, rowId, AvgServ_msColId);
				}
			}
			
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
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
					false, tmp);
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
		
		monTables    = new String[] {"sysmonitors"};
		colsCalcDiff = new String[] {"grabs", "waits", "spins"};
		colsCalcPCT  = new String[] {"contention"};
		pkList       = new LinkedList();
		     pkList.add("name");

		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, monTables, true) // Does the spinlock wrap to 0 or does it wrap to -hugeNumber
		// OVERRIDE SOME DEFAULT METHODS
		{
            private static final long serialVersionUID = -925512251960490929L;

			public void initSql()
			{
				int aseVersion = getServerVersion();

				//String cols1, cols2, cols3;
				//cols1 = cols2 = cols3 = "";

				String datatype = "int";
				if (aseVersion >= 15000)
				{
					datatype = "bigint";
				}

				String sql =   
					"DBCC monitor('sample', 'all',        'on') \n" +
					"DBCC monitor('sample', 'spinlock_s', 'on') \n" +
					"\n" +
					"SELECT \n" +
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
					"  AND P.field_id = S.field_id \n" +
					"GROUP BY P.field_name \n" +
					"ORDER BY 1";

				setSql(sql);
				
				setSqlInit(
					"DBCC traceon(3604,8399)                    \n" +
					"DBCC monitor('select', 'all',        'on') \n" +
					"DBCC monitor('select', 'spinlock_s', 'on') \n");
				
			}

			public void localCalculation()
			{
				MonTablesDictionary mtd = MonTablesDictionary.getInstance();

				long grabs,          waits,          spins;
				int  grabsColId = 0, waitsColId = 0, spinsColId = 0, contentionColId = 0, spinsPerWaitColId = 0;
				int  pos_name = -1,  pos_desc = -1;

				// Find column Id's
				Vector colNames = diffData.getColNames();
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
						diffData.setValueAt(null, rowId, contentionColId);

					// spinsPerWait
					if (waits > 0)
					{
						BigDecimal spinWarning = new BigDecimal( ((1.0 * (spins)) / waits) ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

						diffData.setValueAt(spinWarning, rowId, spinsPerWaitColId);
					}
					else
						diffData.setValueAt(null, rowId, spinsPerWaitColId);

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
		// ProcedureCache Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = "CMcachedProcs";
		displayName  = "Cached Procedures";
		description  = "What Objects is located in the 'procedure cache'.";
		
		monTables    = new String[] {"monCachedProcedures"};
		colsCalcDiff = new String[] {};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList();
		     pkList.add("PlanID");


		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, monTables, true)
		// OVERRIDE SOME DEFAULT METHODS
		{
            private static final long serialVersionUID = 7929613701950650791L;

			public void initSql()
			{
				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				cols1 = "PlanID, DBName, ObjectName, ObjectType, MemUsageKB, CompileDate";
				if (aseVersion >= 15010 || (aseVersion >= 12540 && aseVersion <= 15000) )
				{
				}

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from monCachedProcedures \n" +
					"order by 2,3,4\n";

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
		
		monTables    = new String[] {"monProcedureCache"};
		colsCalcDiff = new String[] {"Requests", "Loads", "Writes", "Stalls"};
		colsCalcPCT  = new String[] {};
		pkList     = new LinkedList();
		//     pkList.add("PlanID");

		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, monTables, true)
		{
            private static final long serialVersionUID = -6467250604457739178L;

			public void initSql()
			{
				int aseVersion = getServerVersion();

				//String cols1, cols2, cols3;
				//cols1 = cols2 = cols3 = "";

				//cols1 = "Requests, Loads, Writes, Stalls";
				if (aseVersion >= 15010 || (aseVersion >= 12540 && aseVersion <= 15000) )
				{
				}

				String sql = 
					"select *\n" +
					"from monProcedureCache \n";

				setSql(sql);
			}
			
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_procedure_cache_activity.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );
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
			"This could be a bit heavy to sample, so use with caution.<br>" +
			"Tip:<br>" +
			"- Use ABSolute counter values to check how many MB the table are using." +
			"- Use Diff or Rate to check if the memory print is increasing or decreasing.<br>" +
			"- Use the checkpox 'Pause Data Polling' wait for a minute, then enable polling again, that will give you a longer sample period!</html>";
		
		monTables    = new String[] {"monCachedObject"};
		colsCalcDiff = new String[] {"CachedKB", "TotalSizeKB"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList();
		     pkList.add("DBID");
		     pkList.add("ObjectID");
		     pkList.add("IndexID");


		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, monTables, false) // "CachedKB", "TotalSizeKB" goes UP and DOWN: so wee want negative count values
		// OVERRIDE SOME DEFAULT METHODS
		{
            private static final long serialVersionUID = 1725558024113511209L;

			public void initSql()
			{
				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				cols1 = "DBID, ObjectID, IndexID, DBName,  ObjectName, ObjectType, ";
				cols2 = "";
				cols3 = "CachedKB, CacheName";
				if (aseVersion >= 15000)
				{
					cols2 = "PartitionID, PartitionName, TotalSizeKB, ";
					getPk().add("PartitionID");
				}

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from monCachedObject \n";

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
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_objects_in_cache_activity.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );
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
		
		monTables = new String[] {"monErrorLog"};
		
		tmp = new CountersModelAppend( name, null, monTables)
		// OVERRIDE SOME DEFAULT METHODS
		{
            private static final long serialVersionUID = 1227895347277630659L;

			public void initSql()
			{
				int aseVersion = getServerVersion();

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				cols1 = "Time, SPID, KPID, FamilyID, EngineNumber, ErrorNumber, Severity, ";
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
			public void init(Connection conn)
			{
				boolean conf = AseMonitoringConfigDialog.getAseConfigRunValueBooleanNoEx(conn, "errorlog pipe active");
				if (conf)
				{
					_logger.debug(getName() + ": should be VISABLE.");
				}
				else
				{
					setActive(false, "sp_configure 'errorlog pipe active' has NOT been enabled.");
					_logger.warn("When trying to initialize Counters Models ("+getName()+") in ASE Version "+getServerVersion()+", sp_configure 'errorlog pipe active' has NOT been enabled.");
					_logger.debug(getName() + ": should be HIDDEN.");
					TabularCntrPanel tcp = getTabPanel();
					if (tcp != null)
					{
						tcp.setToolTipText("This tab will only be visible if: sp_configure 'errorlog pipe active' has been enabled.");
					}
				}
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
		
		monTables = new String[] {"monDeadLock"};

		tmp = new CountersModelAppend( name, null, monTables)
		// OVERRIDE SOME DEFAULT METHODS
		{
            private static final long serialVersionUID = -5448698796472216270L;

			public void initSql()
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

			public void init(Connection conn)
			{
				boolean conf = AseMonitoringConfigDialog.getAseConfigRunValueBooleanNoEx(conn, "deadlock pipe active");
				if (conf)
				{
					_logger.debug(getName() + ": should be VISABLE.");
				}
				else
				{
					setActive(false, "sp_configure 'deadlock pipe active' has NOT been enabled.");
					_logger.warn("When trying to initialize Counters Models ("+getName()+") in ASE Version "+getServerVersion()+", sp_configure 'deadlock pipe active' has NOT been enabled.");
					_logger.debug(getName() + ": should be HIDDEN.");
					TabularCntrPanel tcp = getTabPanel();
					if (tcp != null)
					{
						tcp.setToolTipText("This tab will only be visible if: sp_configure 'deadlock pipe active' has been enabled.");
					}
				}
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
		
		monTables    = new String[] {"monProcedureCacheModuleUsage"};
		colsCalcDiff = new String[] {"Active", "NumPagesReused"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList();
		     pkList.add("ModuleID");

		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, monTables, false)  // "Active", "NumPagesReused" goes UP and DOWN: so wee want negative count values
		{
            private static final long serialVersionUID = -2606894492200318775L;

			public void initSql()
			{
				//int aseVersion = getServerVersion();

				//String cols1, cols2, cols3;
				//cols1 = cols2 = cols3 = "";

				String sql = 
					"select ModuleName, ModuleID, Active, HWM, NumPagesReused \n" +
					"from monProcedureCacheModuleUsage\n";

				setSql(sql);
			}
			
			public void init(Connection conn)
			{
				int aseVersion = getServerVersion();

				if (aseVersion >= 15010)
				{
					_logger.debug(getName() + ": should be VISIBLE.");
				}
				else
				{
					setActive(false, "This info only available if ASE Server Version is over 15.0.1");
					_logger.warn("When trying to initialize Counters Models ("+getName()+") in ASE Version "+getServerVersion()+", I need atleast ASE Version 15.0.1 for that.");
					_logger.debug(getName() + ": should be HIDDEN.");
					TabularCntrPanel tcp = getTabPanel();
					if (tcp != null)
					{
						tcp.setToolTipText("This tab will only be visible if ASE Server Version is over 15.0.1");
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
		
		monTables    = new String[] {"monProcedureCacheMemoryUsage", "monProcedureCacheModuleUsage"};
		colsCalcDiff = new String[] {"Active", "NumReuseCaused"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList();
		     pkList.add("AllocatorID");


		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, monTables, false)  // "Active", "NumReuseCaused" goes UP and DOWN: so wee want negative count values
		{
            private static final long serialVersionUID = -5474676196893459453L;

			public void initSql()
			{
				//int aseVersion = getServerVersion();

				//String cols1, cols2, cols3;
				//cols1 = cols2 = cols3 = "";

				String sql = 
					"select M.ModuleName, C.ModuleID, C.AllocatorName, C.AllocatorID, C.Active, C.HWM, C.ChunkHWM, C.NumReuseCaused \n" +
					"from monProcedureCacheMemoryUsage C, monProcedureCacheModuleUsage M \n" +
					"where C.ModuleID = M.ModuleID \n" +
					"order by C.ModuleID, C.AllocatorID \n";

				setSql(sql);
			}
			
			public void init(Connection conn)
			{
				int aseVersion = getServerVersion();

				if (aseVersion >= 15010)
				{
					_logger.debug(getName() + ": should be VISIBLE.");
				}
				else
				{
					setActive(false, "This info only available if ASE Server Version is over 15.0.1");
					_logger.warn("When trying to initialize Counters Models ("+getName()+") in ASE Version "+getServerVersion()+", I need atleast ASE Version 15.0.1 for that.");
					_logger.debug(getName() + ": should be HIDDEN.");
					TabularCntrPanel tcp = getTabPanel();
					if (tcp != null)
					{
						tcp.setToolTipText("This tab will only be visible if ASE Server Version is over 15.0.1");
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
		
		monTables    = new String[] {"monStatementCache"};
		colsCalcDiff = new String[] {"NumStatements", "NumSearches", "HitCount", "NumInserts", "NumRemovals", "NumRecompilesSchemaChanges", "NumRecompilesPlanFlushes"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList();
		     pkList.add("TotalSizeKB");

		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, monTables, false) // true|false i'm not sure
		{
            private static final long serialVersionUID = -9084781196460687664L;

			public void initSql()
			{
				//int aseVersion = getServerVersion();

				//String cols1, cols2, cols3;
				//cols1 = cols2 = cols3 = "";

				String sql = 
					"select * \n" +
					"from monStatementCache\n";

				setSql(sql);
			}
			
			public void init(Connection conn)
			{
				int aseVersion = getServerVersion();
				
				if (aseVersion >= 15020)
				{
					int statementCacheSize = AseMonitoringConfigDialog.getAseConfigRunValueNoEx(conn, "statement cache size");
					if (statementCacheSize > 0)
					{
						_logger.debug(getName() + ": should be VISABLE.");
					}
					else
					{
						setActive(false, "sp_configure 'statement cache size' has NOT been enabled.");
						_logger.debug(getName() + ": should be HIDDEN.");
						_logger.warn("When trying to initialize Counters Models ("+getName()+") in ASE Version "+getServerVersion()+", I found that 'statement cache size' wasn't configured (which is done with: sp_configure 'statement cache size'), so monitoring information about Statement Caches will NOT be enabled.");
						TabularCntrPanel tcp = getTabPanel();
						if (tcp != null)
						{
							tcp.setToolTipText("This tab will only be visible if: sp_configure 'statement cache size' has NOT been enabled.");
						}
					}
				}
				else
				{
					setActive(false, "This info only available if ASE Server Version is over 15.0.2");
					_logger.warn("When trying to initialize Counters Models ("+getName()+") in ASE Version "+getServerVersion()+", I need atleast ASE Version 15.0.2 for that.");
					_logger.debug(getName() + ": should be HIDDEN.");
					TabularCntrPanel tcp = getTabPanel();
					if (tcp != null)
					{
						tcp.setToolTipText("This tab will only be visible if ASE Server Version is over 15.0.2");
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
		
		monTables    = new String[] {"monCachedStatement"};
		colsCalcDiff = new String[] {"UseCount", "CurrentUsageCount", "MaxUsageCount", "NumRecompilesPlanFlushes", "NumRecompilesSchemaChanges"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList();
		     pkList.add("SSQLID");

		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, monTables, false)
		{
            private static final long serialVersionUID = -1842749890597642593L;

			public void initSql()
			{
				//int aseVersion = getServerVersion();

				//String cols1, cols2, cols3;
				//cols1 = cols2 = cols3 = "";

				String sql = 
					"select * \n" +
					"from monCachedStatement\n";

				setSql(sql);
			}
			
			public void init(Connection conn)
			{
				int aseVersion = getServerVersion();

				if (aseVersion >= 15020)
				{
					int statementCacheSize = AseMonitoringConfigDialog.getAseConfigRunValueNoEx(conn, "statement cache size");
					if (statementCacheSize > 0)
					{
						_logger.info(getName() + ": should be VISABLE.");
					}
					else
					{
						setActive(false, "sp_configure 'statement cache size' has NOT been enabled.");
						_logger.debug(getName() + ": should be HIDDEN.");
						_logger.warn("When trying to initialize Counters Models ("+getName()+") in ASE Version "+getServerVersion()+", I found that 'statement cache size' wasn't configured (which is done with: sp_configure 'statement cache size'), so monitoring information about Statement Caches will NOT be enabled.");
						TabularCntrPanel tcp = getTabPanel();
						if (tcp != null)
						{
							tcp.setToolTipText("This tab will only be visible if: sp_configure 'statement cache size' has NOT been enabled.");
						}
					}
				}
				else
				{
					setActive(false, "This info only available if ASE Server Version is over 15.0.2");
					_logger.debug(getName() + ": should be HIDDEN.");
					_logger.warn("When trying to initialize Counters Models ("+getName()+") in ASE Version "+getServerVersion()+", I need atleast ASE Version 15.0.2 for that.");
					TabularCntrPanel tcp = getTabPanel();
					if (tcp != null)
					{
						tcp.setToolTipText("This tab will only be visible if ASE Server Version is over 15.0.2");
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
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_statement_cache_detail_activity.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );
		}
		
		_CMList.add(tmp);

		name         = "RemoteCPU";
		displayName  = "RemoteCPU";
		description  = "If Rstatserver present on target.";
		
		monTables    = new String[] {};
		colsCalcDiff = new String[] {};
		colsCalcPCT  = new String[] {"CPULoad"};
		pkList       = new LinkedList();

		String[] args         = new String[] {};
		tmp = new CountersModelRemoteCPU(name, null, colsCalcPCT, false);
		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (Asemon.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "./images/cm_object_activity.png") );
			tmp.setTabPanel( tcp );
			MainFrame.addTcp( tcp );
		}
		_CMList.add(tmp);



		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// USER DEFINED COUNTERS
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		createUserDefinedCounterModels();

		// done
		created = true;
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
		Configuration aseMonProps = null;
		if(Asemon.hasGUI())
		{
			aseMonProps = Asemon.getProps();
		}
		else
		{
			aseMonProps = Asemon.getStoreProps();	
		}
		
		if (aseMonProps == null)
			return;
		
		String prefix = "udc.";

		Vector uniqueUdcNames = new Vector();

		// Compose a list of unique udc.xxxx. strings
		Enumeration enumL1 = aseMonProps.getKeys(prefix);
		while(enumL1.hasMoreElements())
		{
			String key = (String) enumL1.nextElement();
			String name = key.substring(prefix.length(), key.indexOf(".", prefix.length()));
			
			if ( ! uniqueUdcNames.contains(name) )
			{
				uniqueUdcNames.add(name);
			}
		}

		// Loop the above unique "names"
		enumL1 = uniqueUdcNames.elements();
		while(enumL1.hasMoreElements())
		{
			String name = (String) enumL1.nextElement();
			String startKey = prefix + name + ".";
			
			Map sqlVer = null;

			_logger.debug("STARTING TO Initializing User Defined Counter '"+name+"'.");

			// Get the individual properties
			String  udcName        = aseMonProps.getProperty(startKey + "name");
			String  udcDisplayName = aseMonProps.getProperty(startKey + "displayName", udcName);
			String  udcDescription = aseMonProps.getProperty(startKey + "description", "");
			String  udcSqlInit     = aseMonProps.getProperty(startKey + "sqlInit");
			String  udcSqlClose    = aseMonProps.getProperty(startKey + "sqlClose");
			String  udcSql         = aseMonProps.getProperty(startKey + "sql");
			String  udcPk          = aseMonProps.getProperty(startKey + "pk");
			String  udcPkPos       = aseMonProps.getProperty(startKey + "pkPos");
			String  udcDiff        = aseMonProps.getProperty(startKey + "diff");
			boolean udcNDCT0 =aseMonProps.getBooleanProperty(startKey + "negativeDiffCountersToZero", false);
			String  udcTTMT        = aseMonProps.getProperty(startKey + "toolTipMonTables");

			boolean udcHasGraph = aseMonProps.getBooleanProperty(startKey + "graph", false);
			String  udcGraphName        = aseMonProps.getProperty(startKey + "graph.name");
			String  udcGraphLabel       = aseMonProps.getProperty(startKey + "graph.label");
			String  udcGraphMenuLabel   = aseMonProps.getProperty(startKey + "graph.menuLabel");
			String  udcGraphDataCols    = aseMonProps.getProperty(startKey + "graph.data.cols");
			String  udcGraphDataMethods = aseMonProps.getProperty(startKey + "graph.data.methods");
			String  udcGraphDataLabels  = aseMonProps.getProperty(startKey + "graph.data.labels");

			// CHECK for mandatory properties
			if (udcName == null)
			{
				_logger.error("Cant initialize User Defined Counter '"+name+"', no 'name' has been defined.");
				continue;
			}
			if (udcSql == null)
			{
				_logger.error("Cant initialize User Defined Counter '"+name+"', no 'sql' has been defined.");
				continue;
			}
			if (udcPkPos != null)
			{
				_logger.error("Cant initialize User Defined Counter '"+name+"', 'pkPos' are not longer supported, please use 'pk' instead.");
				continue;
			}

			if (udcPk == null)
			{
				_logger.error("Cant initialize User Defined Counter '"+name+"', no 'pk' has been defined.");
				continue;
			}

			// Check/get version specific SQL strings
			String sqlVersionPrefix = startKey + "sql" + ".";
			//int     sqlVersionHigh = -1;
			Enumeration enumSqlVersions = aseMonProps.getKeys(sqlVersionPrefix);
			while(enumSqlVersions.hasMoreElements())
			{
				String key = (String) enumSqlVersions.nextElement();
				String sqlVersionStr = key.substring(sqlVersionPrefix.length());
				int    sqlVersionNumInKey = 0;

				try
				{
					sqlVersionNumInKey = Integer.parseInt(sqlVersionStr);
				}
				catch(NumberFormatException ex)
				{
					_logger.warn("Problems initialize User Defined Counter '"+name+"', a sql.##### where ##### should specify ASE server version if faulty in the string '"+sqlVersionStr+"'.");
					continue;
				}
				
				// Add all the udName.sql.#VERSION# to the map.
				// we will descide what version to use later on.
				if (sqlVer == null)
					sqlVer = new HashMap();

				sqlVer.put(
					new Integer(sqlVersionNumInKey), 
					aseMonProps.getProperty(sqlVersionPrefix + sqlVersionNumInKey) );

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
//				udcSql = aseMonProps.getProperty(sqlVersionPrefix + sqlVersionHigh);
//			}

			// Get me some array variables, that we will "spit" properties into
			List     udcPkList    = new LinkedList();
			String[] udcPkArray   = {};
//			String[] udcPkPosArray= {};
			String[] udcDiffArray = {};
			String[] udcTTMTArray = {};
			String[] udcPctArray  = {}; // not used, just initialized

			// Split some properties using commas "," as the delimiter  
			if (udcPk    != null) udcPkArray    = udcPk   .split(",");
//			if (udcPkPos != null) udcPkPosArray = udcPkPos.split(",");
			if (udcDiff  != null) udcDiffArray  = udcDiff .split(",");
			if (udcTTMT  != null) udcTTMTArray  = udcTTMT .split(",");

			

			
			// Get rid of extra " " spaces from the split 
			for (int i=0; i<udcPkArray   .length; i++) udcPkArray   [i] = udcPkArray   [i].trim(); 
//			for (int i=0; i<udcPkPosArray.length; i++) udcPkPosArray[i] = udcPkPosArray[i].trim(); 
			for (int i=0; i<udcDiffArray .length; i++) udcDiffArray [i] = udcDiffArray [i].trim(); 
			for (int i=0; i<udcTTMTArray .length; i++) udcTTMTArray [i] = udcTTMTArray [i].trim(); 

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
//			}

			_logger.info("Creating User Defined Counter '"+name+"' with sql '"+udcSql+"'.");

			// Finally create the Counter model and all it's surondings...
			CountersModel cm = new CountersModelUserDefined( name, udcSql, sqlVer,
					udcPkList, //pk1, pk2, pk3, 
					udcDiffArray, udcPctArray, udcTTMTArray, udcNDCT0);

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
					if ( ! CountersModel.isValidGraphMethod(udcGraphDataMethodsArr[i]))
					{
						_logger.error("Cant add a graph to the User Defined Counter '"+name+"'. 'The graph method '"+udcGraphDataMethodsArr[i]+"' is unknown.");
						_logger.error("Valid method names is: "+CountersModel.getValidGraphMethodsString());
						addGraph = false;
					}
				}
				if (addGraph)
				{
					// GRAPH
					TrendGraph tg = new TrendGraph(
							udcGraphName,      // Name of the raph
							udcGraphMenuLabel, // Menu Checkbox text
							udcGraphLabel,     // Label on the graph
							udcGraphDataLabelsArr, // Labels for each plotline
							false, cm);
					tg.setGraphCalculations(udcGraphDataColsArr, udcGraphDataMethodsArr);
					cm.addTrendGraph(udcGraphName, tg, true);
				}
			}

			_CMList.add(cm);
		}
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
			mtd.addSpinlockDescription("tablockspins",    "xxxx: tablockspins");
			mtd.addSpinlockDescription("fglockspins",     "xxxx: fglockspins");
			mtd.addSpinlockDescription("addrlockspins",   "xxxx: Adress spinlock");
			
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

	public void stopRefresh()
	{
		refreshCntr = false;
	}

	public void startRefresh()
	{
		refreshCntr = true;

		// If we where in sleep at the getCounters loop
		if (_thread != null)
		{
			_logger.debug("Sending 'interrupt' to the thread '"+_thread.getName()+"' if it was sleeping...");
			_thread.interrupt();
		}
	}

	public boolean isRefreshing()
	{
		return refreshing;
	}

	public static void setRefreshing(boolean s)
	{
		refreshing = s;
	}

	public void clearComponents()
	{
		if ( ! initialized )
			return;

		if (!isRefreshing())
		{
			MainFrame.clearSummaryData();

			Iterator i = _CMList.iterator();
			while (i.hasNext())
			{
				CountersModel cm = (CountersModel) i.next();
				
				if (cm != null)
				{
					cm.clear();
				}
			}
			
			SummaryPanel.getInstance().clearGraph();

//			MainFrame.statusFld.setText("");
		}
	}
}
