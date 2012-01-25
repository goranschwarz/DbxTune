/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */


package com.asetune;
import java.awt.BorderLayout;
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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.IconHighlighter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.TextAnchor;

import com.asetune.cm.CmSybMessageHandler;
import com.asetune.cm.CounterModelHostMonitor;
import com.asetune.cm.CounterTableModel;
import com.asetune.cm.CountersModel;
import com.asetune.cm.CountersModelAppend;
import com.asetune.cm.CountersModelUserDefined;
import com.asetune.cm.SamplingCnt;
import com.asetune.cm.sql.VersionInfo;
import com.asetune.gui.AseConfigMonitoringDialog;
import com.asetune.gui.ChangeToJTabDialog;
import com.asetune.gui.DbSelectionForGraphsDialog;
import com.asetune.gui.MainFrame;
import com.asetune.gui.SplashWindow;
import com.asetune.gui.SummaryPanel;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.TrendGraph;
import com.asetune.hostmon.HostMonitor;
import com.asetune.hostmon.SshConnection;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.sp_sysmon.SpSysmon;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.MathUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;



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
	private static boolean _isRefreshing = false;

	
	private static String _waitEvent = "";
	
	/** Keep a list of all CounterModels that you have initialized */
	protected static List<CountersModel> _CMList = new ArrayList<CountersModel>();

	/** is sp_configure 'capture missing statistics' set or not */
	protected boolean _config_captureMissingStatistics = false;

	/** is sp_configure 'enable metrics capture' set or not */
	protected boolean _config_enableMetricsCapture = false;
	
	/** A list of roles which the connected user has */
	protected List<String> _activeRoleList = null;

	/** Statistic field: first sample time */
	private Timestamp _statFirstSampleTime = null;
	/** Statistic field: last sample time */
	private Timestamp _statLastSampleTime  = null;

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

	
	////////////////////////////////////////////////////////////////////////////////
	// NOTE: if you add a CM, also add it to class: CounterSetTemplates
	////////////////////////////////////////////////////////////////////////////////
	public static final String CM_NAME__SUMMARY                 = SummaryPanel.CM_NAME;
	public static final String CM_DESC__SUMMARY                 = "Summary";

	public static final String CM_NAME__OBJECT_ACTIVITY         = "CMobjectActivity";
	public static final String CM_DESC__OBJECT_ACTIVITY         = "Objects";

	public static final String CM_NAME__PROCESS_ACTIVITY        = "CMprocessActivity";
	public static final String CM_DESC__PROCESS_ACTIVITY        = "Processes";

	public static final String CM_NAME__PROCESS_WAIT            = "CMspidWait";
	public static final String CM_DESC__PROCESS_WAIT            = "SPID Wait";

	public static final String CM_NAME__OPEN_DATABASES          = "CMopenDatabases";
	public static final String CM_DESC__OPEN_DATABASES          = "Databases";

	public static final String CM_NAME__TEMPDB_ACTIVITY         = "CMtempdbActivity";
	public static final String CM_DESC__TEMPDB_ACTIVITY         = "Temp Db";

	public static final String CM_NAME__SYS_WAIT                = "CMsysWait";
	public static final String CM_DESC__SYS_WAIT                = "Waits";

	public static final String CM_NAME__ENGINE                  = "CMengine";
	public static final String CM_DESC__ENGINE                  = "Engines";

	public static final String CM_NAME__SYS_LOAD                = "CMsysLoad";
	public static final String CM_DESC__SYS_LOAD                = "System Load";

	public static final String CM_NAME__DATA_CACHE              = "CMdataCache";
	public static final String CM_DESC__DATA_CACHE              = "Caches";

	public static final String CM_NAME__CACHE_POOL              = "CMcachePool";
	public static final String CM_DESC__CACHE_POOL              = "Pools";

	public static final String CM_NAME__DEVICE_IO               = "CMdeviceIo";
	public static final String CM_DESC__DEVICE_IO               = "Devices";

	public static final String CM_NAME__IO_QUEUE_SUM            = "CMioQueueSum";
	public static final String CM_DESC__IO_QUEUE_SUM            = "IO Sum";

	public static final String CM_NAME__IO_QUEUE                = "CMioQueue";
	public static final String CM_DESC__IO_QUEUE                = "IO Queue";

	public static final String CM_NAME__SPINLOCK_SUM            = "CMspinlockSum";
	public static final String CM_DESC__SPINLOCK_SUM            = "Spinlock Sum";

	public static final String CM_NAME__SYSMON                  = "CMsysmon";
	public static final String CM_DESC__SYSMON                  = "Sysmon Raw";

	public static final String CM_NAME__REP_AGENT               = "CMrepAgent";
	public static final String CM_DESC__REP_AGENT               = "RepAgent";

	public static final String CM_NAME__CACHED_PROC             = "CMcachedProcs";
	public static final String CM_DESC__CACHED_PROC             = "Cached Procedures";

	public static final String CM_NAME__PROC_CACHE_LOAD         = "CMprocCacheLoad";
	public static final String CM_DESC__PROC_CACHE_LOAD         = "Procedure Cache Load";

	public static final String CM_NAME__PROC_CALL_STACK         = "CMprocCallStack";
	public static final String CM_DESC__PROC_CALL_STACK         = "Procedure Call Stack";

	public static final String CM_NAME__CACHED_OBJECTS          = "CMcachedObjects";
	public static final String CM_DESC__CACHED_OBJECTS          = "Cached Objects";

	public static final String CM_NAME__ERRORLOG                = "CMerrolog";
	public static final String CM_DESC__ERRORLOG                = "Errorlog";

	public static final String CM_NAME__DEADLOCK                = "CMdeadlock";
	public static final String CM_DESC__DEADLOCK                = "Deadlock";

	public static final String CM_NAME__LOCK_TIMEOUT            = "CMlockTimeout";
	public static final String CM_DESC__LOCK_TIMEOUT            = "Lock Timeout";

	public static final String CM_NAME__PROC_CACHE_MODULE_USAGE = "CMpCacheModuleUsage";
	public static final String CM_DESC__PROC_CACHE_MODULE_USAGE = "Proc Cache Module Usage";

	public static final String CM_NAME__PROC_CACHE_MEMORY_USAGE = "CMpCacheMemoryUsage";
	public static final String CM_DESC__PROC_CACHE_MEMORY_USAGE = "Proc Cache Memory Usage";

	public static final String CM_NAME__STATEMENT_CACHE         = "CMstatementCache";
	public static final String CM_DESC__STATEMENT_CACHE         = "Statement Cache";

	public static final String CM_NAME__STATEMENT_CACHE_DETAILS = "CMstmntCacheDetails";
	public static final String CM_DESC__STATEMENT_CACHE_DETAILS = "Statement Cache Details";

	public static final String CM_NAME__ACTIVE_OBJECTS          = "CMactiveObjects";
	public static final String CM_DESC__ACTIVE_OBJECTS          = "Active Objects";

	public static final String CM_NAME__ACTIVE_STATEMENTS       = "CMactiveStatements";
	public static final String CM_DESC__ACTIVE_STATEMENTS       = "Active Statements";

	public static final String CM_NAME__BLOCKING                = "CMblocking";
	public static final String CM_DESC__BLOCKING                = "Blocking";

	public static final String CM_NAME__MISSING_STATISTICS      = "CMmissingStats";
	public static final String CM_DESC__MISSING_STATISTICS      = "Missing Statistics";

	public static final String CM_NAME__QP_METRICS              = "CMqpMetrics";
	public static final String CM_DESC__QP_METRICS              = "QP Metrics";

	public static final String CM_NAME__SP_MONITOR_CONFIG       = "CMspMonitorConfig";
	public static final String CM_DESC__SP_MONITOR_CONFIG       = "sp_monitorconfig";

	public static final String CM_NAME__OS_IOSTAT               = "CMosIostat";
	public static final String CM_DESC__OS_IOSTAT               = "OS Disk Stat";

	public static final String CM_NAME__OS_VMSTAT               = "CMosVmstat";
	public static final String CM_DESC__OS_VMSTAT               = "OS CPU(vmstat)";

	public static final String CM_NAME__OS_MPSTAT               = "CMosMpstat";
	public static final String CM_DESC__OS_MPSTAT               = "OS CPU(mpstat)";
	
	
	// GRAPHS
	public static final String CM_GRAPH_NAME__SUMMARY__AA_CPU                    = "aaCpuGraph";
	public static final String CM_GRAPH_NAME__SUMMARY__TRANSACTION               = "TransGraph";
	public static final String CM_GRAPH_NAME__SUMMARY__CONNECTION                = "ConnectionsGraph";
	public static final String CM_GRAPH_NAME__SUMMARY__AA_DISK_READ_WRITE        = "aaReadWriteGraph";
	public static final String CM_GRAPH_NAME__SUMMARY__AA_NW_PACKET              = "aaPacketGraph";

	public static final String CM_GRAPH_NAME__PROCESS_ACTIVITY__CHKPT_HK         = "ChkptHkGraph";

	public static final String CM_GRAPH_NAME__OPEN_DATABASES__LOGSEMAPHORE_CONT  = "DbLogSemapContGraph";
	public static final String CM_GRAPH_NAME__OPEN_DATABASES__LOGSIZE_LEFT       = "DbLogSizeLeftGraph";
	public static final String CM_GRAPH_NAME__OPEN_DATABASES__LOGSIZE_USED_PCT   = "DbLogSizeUsedPctGraph";

	public static final String CM_GRAPH_NAME__TEMPDB_ACTIVITY__LOGSEMAPHORE_CONT = "TempDbLogSemapContGraph";
	
	public static final String CM_GRAPH_NAME__ENGINE__CPU_SUM                    = "cpuSum";
	public static final String CM_GRAPH_NAME__ENGINE__CPU_ENG                    = "cpuEng";
	
	public static final String CM_GRAPH_NAME__SYS_LOAD__AVG_RUN_QUEUE_LENTH      = "AvgRunQLengthGraph";
	public static final String CM_GRAPH_NAME__SYS_LOAD__ENGINE_RUN_QUEUE_LENTH   = "EngineRunQLengthGraph";

	public static final String CM_GRAPH_NAME__DATA_CACHE__ACTIVITY               = "CacheGraph";
	
	public static final String CM_GRAPH_NAME__IO_QUEUE_SUM__DISK_IO_OPS          = "diskIo";

	public static final String CM_GRAPH_NAME__IO_QUEUE__DEVICE_SERVICE_TIME      = "devSvcTime";

	public static final String CM_GRAPH_NAME__PROC_CACHE_LOAD__REQUEST_PER_SEC   = "ProcCacheGraph";

	public static final String CM_GRAPH_NAME__PROC_CACHE_MODULE_USAGE__ABS_USAGE = "ProcCacheModuleUsageGraph";
	
	public static final String CM_GRAPH_NAME__STATEMENT_CACHE__REQUEST_PER_SEC   = "StatementCacheGraph";

	
	/** Get a list of all available <code>CountersModel</code> that exists. System and UDC */
	public static List<CountersModel> getCmList()
	{ 
		return _CMList; 
	}

	 // Get any <code>CountersModel</code> that does NOT starts with 'CM*', which then is a UDC Used Defined Counter 
	/**
	 * Get any <code>CountersModel</code> that does NOT starts with 'CM*', which then is a UDC Used Defined Counter
	 * @return a List of CountersModel objects, if NO UDC exists, an empty list will be returned.
	 */
	public static List<CountersModel> getCmListUdc() 
	{
		ArrayList<CountersModel> cmList = new ArrayList<CountersModel>();
		for (CountersModel cm : _CMList)
		{
			if ( ! cm.getName().startsWith("CM") )
				cmList.add(cm);
		}
		return cmList; 
	}
	/** 
	 * Get any <code>CountersModel</code> that starts with the name 'CM*', which is s System CM 
	 * @return a List of System CountersModel objects
	 */
	public static List<CountersModel> getCmListSystem() 
	{
		ArrayList<CountersModel> cmList = new ArrayList<CountersModel>();
		for (CountersModel cm : _CMList)
		{
			if ( cm.getName().startsWith("CM") )
				cmList.add(cm);
		}
		return cmList; 
	}

	/** 
	 * Get any <code>CountersModel</code> that depends on a specific ASE configuration 
	 * @return a List of CountersModel objects
	 */
	public static List<CountersModel> getCmListDependsOnConfig(String cfgName, Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		ArrayList<CountersModel> cmList = new ArrayList<CountersModel>();
		for (CountersModel cm : _CMList)
		{
			String[] sa = cm.getDependsOnConfigForVersion(conn, aseVersion, isClusterEnabled);
//			String[] sa = cm.getDependsOnConfig();
			if ( sa == null )
				continue;

			for (String cfg : sa)
			{
				// remove any default values '=value'
				int index = cfg.indexOf("=");
				if (index >= 0)
					cfg = cfg.substring(0, index).trim();
					
				if ( cfgName.equals(cfg) && ! cmList.contains(cm) )
					cmList.add(cm);
			}
		}
		return cmList; 
	}
	/** simply do: return getCmListDependsOnConfig(cfg, null, 0, false); */
	public static List<CountersModel> getCmListDependsOnConfig(String cfg)
	{
		return getCmListDependsOnConfig(cfg, null, 0, false);
	}

	/** 
	 * Get <code>CountersModel</code> object for a CM that has the "short" name for example CMprocCallStack 
	 * @return if the CM is not found a null will be return
	 */
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
	
	/** 
	 * Get <code>CountersModel</code> object for a CM that has the "long" name for example 'Procedure Call Stack' for CMprocCallStack
	 * @return if the CM is not found a null will be return
	 */
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

	/** 
	 * have we got set a singleton object to be used. (set with setInstance() ) 
	 */
	public static boolean hasInstance()
	{
		return (_instance != null);
	}

	/** 
	 * Get the singleton object
	 * @throws RuntimeException if no singleton has been set. set with setInstance(), check with hasInstance() 
	 */
	public static GetCounters getInstance()
	{
		if (_instance == null)
			throw new RuntimeException("No GetCounters instance exists.");
		return _instance;
	}

	/**
	 * Set a specific GetCounter object to be used as the singleton.
	 * @param cnt
	 */
	public static void setInstance(GetCounters cnt)
	{
		_instance = cnt;
	}

	/**
	 * Reset All CM's etc, this so we build new SQL statements if we connect to a different ASE version<br>
	 * Most possible called from disconnect() or similar
	 */
	public void reset()
	{
		reset(true);
	}
	/**
	 * Reset All CM's etc, this so we build new SQL statements if we connect to a different ASE version<br>
	 * Most possible called from disconnect() or similar
	 * 
	 * @param resetAllCms call reset() on all cm's
	 */
	public void reset(boolean resetAllCms)
	{
		// have we been initialized or not
		_isInitialized      = false; 

		// if we should do refreshing of the counters, or is refreshing paused
		_refreshIsEnabled   = false;

		_waitEvent = "";
		
		// is sp_configure 'capture missing statistics' set or not
		_config_captureMissingStatistics = false;

		// is sp_configure 'enable metrics capture' set or not
		_config_enableMetricsCapture = false;
		
		// A list of roles which the connected user has
		_activeRoleList = null;


		if (resetAllCms)
		{
			for (CountersModel cm : getCmList())
				cm.reset();
		}
	}


	/** Init needs to be implemented by any subclass */
	public abstract void init()
	throws Exception;

	/** The run is located in any implementing subclass */
	public abstract void run();

	/** shutdown or stop any collector */
	public abstract void shutdown();
	
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
	public void initCounters(Connection conn, boolean hasGui, int aseVersion, boolean isClusterEnabled, int monTablesVersion)
	throws Exception
	{
		if (_isInitialized)
			return;

		if ( ! AseConnectionUtils.isConnectionOk(conn, hasGui, null))
			throw new Exception("Trying to initialize the counters with a connection this seems to be broken.");

			
		if (! _countersIsCreated)
			createCounters();
		
		_logger.info("Initializing all CM objects, using ASE server version number "+aseVersion+", isClusterEnabled="+isClusterEnabled+" with monTables Install version "+monTablesVersion+".");

		// Get active ASE Roles
		//List<String> activeRoleList = AseConnectionUtils.getActiveRoles(conn);
		_activeRoleList = AseConnectionUtils.getActiveRoles(conn);
		
		// Get active Monitor Configuration
		Map<String,Integer> monitorConfigMap = AseConnectionUtils.getMonitorConfigs(conn);

		// Get some specific configurations
		if (aseVersion >= 15031)
			_config_captureMissingStatistics = AseConnectionUtils.getAseConfigRunValueBooleanNoEx(conn, "capture missing statistics");

		if (aseVersion >= 15020)
			_config_enableMetricsCapture = AseConnectionUtils.getAseConfigRunValueBooleanNoEx(conn, "enable metrics capture");

		
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
				cm.setActiveRoles(_activeRoleList);

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

			if (AseTune.hasGUI())
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
		//----------------------
		try
		{
			int aseVersion = 0;
			if (MonTablesDictionary.hasInstance())
				aseVersion = MonTablesDictionary.getInstance().aseVersionNum;

//			if (    aseVersion >= 15031 && aseVersion <= 15033
//				 || aseVersion >= 15500 && aseVersion <= 15501
//			   )
			if ( aseVersion >= 15031 && aseVersion < 16000 )
			{
				if (_config_captureMissingStatistics || _config_enableMetricsCapture)
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
	
					if (isLogFull > 0  ||  spaceLeft <= 50) // 50 pages, is 100K in a 2K ASE (well int's NOT MB at least)
					{
						_logger.warn("Truncating the transaction log in the master database. Issuing SQL 'dump tran master with truncate_only'. isLogFull='"+isLogFull+"', spaceLeftInPages='"+spaceLeft+"'.");
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
//		Configuration conf = Configuration.getInstance(Configuration.CONF);
		Configuration conf = Configuration.getCombinedConfiguration();

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

		//==================================================================================================
		// 12.5.0.3: Description of: monState
		// 
		// Description:
		//
		//
		// Needs Configuration:
		// none
		// 
		// Name                           Datatype     Attributes  Ind Description
		// ------------------------------ ------------ ----------- --- ------------------------------------------------------------
		// LockWaitThreshold              int                        0 Time (in seconds) that processes must have waited for locks in order to be reported
		// LockWaits                      int                        0 Number of processes that have waited longer than LockWaitThreshold seconds
		// StartDate                      datetime                   0 Date and time that the ASE was started
		// DaysRunning                    int                        0 Number of days that the ASE has been running for
		// CountersCleared                datetime                   0 Date and time at which the monitor counters were last cleared
		// CheckPoints                    int                        0 Whether any checkpoint is currently running
		// NumDeadlocks                   int                        1 Total number of deadlocks that have occurred
		// DiagnosticDumps                int                        0 Whether the Sybmon diagnostic utility is performing a shared memory dump
		// Connections                    int                        0 Number of active inbound connections
		// MaxRecovery                    int                        0 The maximum time (in minutes), per database, that ASE uses to complete its recovery procedures in case of a system failure, the current 'Run Value' for the 'recovery interval in minutes' configuration option
		//---------------------------------------------------------------------------------------------------
		// Column changes in various versions:
		//
		// Version      Action Name              Datatype   Attributes Description
		// ------------ ------ ----------------- ---------- ---------- ----------------------------------
		// none so far
		// 15.0.1CE/15.5 add   InstanceId        int                   The Server Instance Identifier (cluster only)
		// 15.0.3       add    Transactions      int                   Total number of transactions that have been committed on the server
		//---------------------------------------------------------------------------------------------------

		name         = CM_NAME__SUMMARY;
		displayName  = CM_DESC__SUMMARY;
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
				// The SUMMARY should ALWAYS be refreshed
				return true;
			}
			
			@Override
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return null;
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				List <String> pkCols = new LinkedList<String>();
				return pkCols;
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String nwAddrInfo = "'no sa_role'";
				if (isRuntimeInitialized())
				{
					if (isRoleActive(AseConnectionUtils.SA_ROLE))
					{
						Component guiOwner = AseTune.hasGUI() ? MainFrame.getInstance() : null;
//						nwAddrInfo = "(select min(convert(varchar(255),address_info)) from syslisteners where address_info not like 'localhost%')";
						nwAddrInfo = "'" + AseConnectionUtils.getListeners(conn, false, true, guiOwner) + "'";
					}
				}
				else
					nwAddrInfo = "'tcp listeners goes here, if we are connected'";

				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				// get all the columns for SMP servers
				// if Cluster is enabled, one row for every instance will appear
				//                        so lets try to min, max or sum the rows into 1 row...
				//                        which will "simulate" a SMP environment
				cols1 = "* \n";
				if (isClusterEnabled)
				{
					cols1 = " top 1 \n";
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

				cols2 = ", aseVersion         = @@version \n" +
						", atAtServerName     = @@servername \n" +
						", clusterInstanceId  = " + (isClusterEnabled ? "convert(varchar(15),@@instanceid)"     : "'Not Enabled'") + " \n" + 
						", clusterCoordId     = " + (isClusterEnabled ? "convert(varchar(3), @@clustercoordid)" : "'Not Enabled'") + " \n" +
						", timeIsNow          = getdate() \n" +
						", NetworkAddressInfo = " + nwAddrInfo + " \n" +

						", bootcount          = @@bootcount \n" + // from 12.5.0.3
						", recovery_state     = "+ (aseVersion >= 12510 ? "@@recovery_state" : "'Introduced in ASE 12.5.1'") + " \n" +

						", cpu_busy           = @@cpu_busy \n" +
						", cpu_io             = @@io_busy \n" +
						", cpu_idle           = @@idle \n" +
						", io_total_read      = @@total_read \n" +
						", io_total_write     = @@total_write \n" +

						", aaConnections      = @@connections \n" +
						", distinctLogins     = (select count(distinct suid) from master..sysprocesses) \n" +

						// ------ column 'stat'
						// 32   - Database created with for load option, or crashed while loading database, instructs recovery not to proceed
						// 256  - Database suspect | Not recovered | Cannot be opened or used | Can be dropped only with dbcc dbrepair
						// 1024 - read only
						// 2048 - dbo use only
						// 4096 - single user
						// ------ column 'stat2'
						// 16 - Database is offline
						// 32 - Database is offline until recovery completes
						// model is used during create database... so skip this one to
						", fullTranslogCount  = (select sum(lct_admin('logfull', dbid)) from master..sysdatabases readpast \n" +
						"                        where (status & 32   != 32  ) and (status & 256  != 256 ) \n" +
						"                          and (status & 1024 != 1024) and (status & 2048 != 2048) \n" +
						"                          and (status & 4096 != 4096) \n" +
						"                          and (status2 & 16  != 16  ) and (status2 &  32 != 32  ) \n" +
						"                          and name != 'model') \n" + 

						", pack_received      = @@pack_received \n" +
						", pack_sent          = @@pack_sent \n" +

						", packet_errors      = @@packet_errors \n" +
						", total_errors       = @@total_errors \n" +
						"";

				cols3 = "";

				String fromTable = " from master..monState A \n";

				// if NOT MON_ROLE, revrite the query a bit
				if (isRuntimeInitialized())
				{
					if ( ! isRoleActive(AseConnectionUtils.MON_ROLE))
					{
						cols1     = "dummyColumn = 1 \n";
						fromTable = "";
					}
				}
				
				String sql = "select " + cols1 + cols2 + cols3 + fromTable;
				
				return sql;
			}
			
			/** 
			 * reset some negative counters to 0
			 */
			@Override
			public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
			{
				SamplingCnt counters = diffData;
				
				if (counters == null)
					return;

				// SUMMARY only have 1 row, so this was simplest way, do not do like this on CM's that has many rows
				// Loop on all diffData rows
				for (int rowId=0; rowId < counters.getRowCount(); rowId++) 
				{
					checkAndSetNc20(counters, rowId, "Transactions");
					checkAndSetNc20(counters, rowId, "io_total_read");
					checkAndSetNc20(counters, rowId, "io_total_write");
					checkAndSetNc20(counters, rowId, "pack_received");
					checkAndSetNc20(counters, rowId, "pack_sent");
					checkAndSetNc20(counters, rowId, "packet_errors");
				}
			}
			private void checkAndSetNc20(SamplingCnt counters, int rowId, String columnName)
			{
				int colId = counters.findColumn(columnName);
				if (colId >= 0)
				{
					Object obj  = counters.getValueAt(rowId, colId);
					if (obj instanceof Number)
					{
						//System.out.println("colId="+colId+", name='"+columnName+"', o="+obj);
						if (((Number)obj).intValue() < 0)
						{
							//System.out.println("colId="+colId+", name='"+columnName+"', setting to Integer(0)");
							counters.setValueAt(new Integer(0), rowId, colId);
						}
					}
				}
			}

			@Override
			public void updateGraphData(TrendGraphDataPoint tgdp)
			{
				int aseVersion = getServerVersion();

				if (CM_GRAPH_NAME__SUMMARY__AA_CPU.equals(tgdp.getName()))
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

				if (CM_GRAPH_NAME__SUMMARY__TRANSACTION.equals(tgdp.getName()))
				{
					if (aseVersion < 15033)
					{
						// disable the transactions graph checkbox...
						TrendGraph tg = getTrendGraph(CM_GRAPH_NAME__SUMMARY__TRANSACTION);
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

				if (CM_GRAPH_NAME__SUMMARY__CONNECTION.equals(tgdp.getName()))
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

				if (CM_GRAPH_NAME__SUMMARY__AA_DISK_READ_WRITE.equals(tgdp.getName()))
				{	
					Double[] arr = new Double[2];

					arr[0] = this.getRateValueAsDouble (0, "io_total_read");
					arr[1] = this.getRateValueAsDouble (0, "io_total_write");
					_logger.debug("updateGraphData(aaReadWriteGraph): io_total_read='"+arr[0]+"', io_total_write='"+arr[1]+"'.");

					// Set the values
					tgdp.setDate(this.getTimestamp());
					tgdp.setData(arr);
				}

				if (CM_GRAPH_NAME__SUMMARY__AA_NW_PACKET.equals(tgdp.getName()))
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
		tmp.addTrendGraphData(CM_GRAPH_NAME__SUMMARY__AA_CPU,             new TrendGraphDataPoint(CM_GRAPH_NAME__SUMMARY__AA_CPU,             new String[] { "System+User CPU (@@cpu_busy + @@cpu_io)", "System CPU (@@cpu_io)", "User CPU (@@cpu_busy)" }));
		tmp.addTrendGraphData(CM_GRAPH_NAME__SUMMARY__TRANSACTION,        new TrendGraphDataPoint(CM_GRAPH_NAME__SUMMARY__TRANSACTION,        new String[] { "Transactions" }));
		tmp.addTrendGraphData(CM_GRAPH_NAME__SUMMARY__CONNECTION,         new TrendGraphDataPoint(CM_GRAPH_NAME__SUMMARY__CONNECTION,         new String[] { "UserConnections", "distinctLogins", "@@connections" }));
		tmp.addTrendGraphData(CM_GRAPH_NAME__SUMMARY__AA_DISK_READ_WRITE, new TrendGraphDataPoint(CM_GRAPH_NAME__SUMMARY__AA_DISK_READ_WRITE, new String[] { "@@total_read", "@@total_write" }));
		tmp.addTrendGraphData(CM_GRAPH_NAME__SUMMARY__AA_NW_PACKET,       new TrendGraphDataPoint(CM_GRAPH_NAME__SUMMARY__AA_NW_PACKET,       new String[] { "@@pack_received", "@@pack_sent", "@@packet_errors" }));
		if (AseTune.hasGUI())
		{
			tmp.addTableModelListener( MainFrame.getSummaryPanel() );

			TrendGraph tg = null;

			// GRAPH: aaCpuGraph
			tg = new TrendGraph(CM_GRAPH_NAME__SUMMARY__AA_CPU,
					"CPU Summary, Global Variables", 	// Menu Checkbox text
					"CPU Summary for all Engines (using @@cpu_busy, @@cpu_io)", // Label 
					new String[] { "System+User CPU (@@cpu_busy + @@cpu_io)", "System CPU (@@cpu_io)", "User CPU (@@cpu_busy)" }, 
					true, tmp, false, -1);
			tmp.addTrendGraph(tg.getName(), tg, true);

			// GRAPH: Transaction Graph
			tg = new TrendGraph(CM_GRAPH_NAME__SUMMARY__TRANSACTION,
					// Menu Checkbox text
					"Transaction per second",
					// Label 
					"Number of Transaction per Second (only in 15.0.3 esd#3 and later)", // Label 
					new String[] { "Transactions" }, 
					false, tmp, false, -1);
			tmp.addTrendGraph(tg.getName(), tg, true);

			// GRAPH: Connections Graph
			tg = new TrendGraph(CM_GRAPH_NAME__SUMMARY__CONNECTION,
					// Menu Checkbox text
					"Connections/Users in ASE",
					// Label 
					"Connections/Users connected to the ASE", // Label 
					new String[] { "UserConnections", "distinctLogins", "@@connections" }, 
					false, tmp, false, -1);
			tmp.addTrendGraph(tg.getName(), tg, true);

			// GRAPH: @@total_read, @@total_write
			tg = new TrendGraph(CM_GRAPH_NAME__SUMMARY__AA_DISK_READ_WRITE,
					// Menu Checkbox text
					"Disk read/write, Global Variables",
					// Label 
					"Disk read/write per second, using @@total_read, @@total_write", // Label 
					new String[] { "@@total_read", "@@total_write" }, 
					false, tmp, false, -1);
			tmp.addTrendGraph(tg.getName(), tg, true);

			// GRAPH: @@pack_received, @@pack_sent
			tg = new TrendGraph(CM_GRAPH_NAME__SUMMARY__AA_NW_PACKET,
					// Menu Checkbox text
					"Network Packets received/sent, Global Variables",
					// Label 
					"Network Packets received/sent per second, using @@pack_received, @@pack_sent", // Label 
					new String[] { "@@pack_received", "@@pack_sent", "@@packet_errors" }, 
					false, tmp, false, -1);
			tmp.addTrendGraph(tg.getName(), tg, true);
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

		//==================================================================================================
		// 12.5.0.3: Description of: monOpenObjectActivity
		// 
		// Description:
		//
		//
		// Needs Configuration:
		// none
		// 
		// Name                           Datatype     Attributes  Ind Description
		// ------------------------------ ------------ ----------- --- ------------------------------------------------------------
		// DBID                           int                        0 Unique identifier for the database
		// ObjectID                       int                        0 Unique identifier for the object
		// IndexID                        int                        0 Unique identifier for the index
		// LogicalReads                   int                        1 Total number of buffers read
		// PhysicalReads                  int                        1 Number of buffers read from disk
		// APFReads                       int                        1 Number of APF buffers read
		// PagesRead                      int                        1 Total number of pages read
		// PhysicalWrites                 int                        1 Total number of buffers written to disk
		// PagesWritten                   int                        1 Total number of pages written to disk
		// OptSelectCount                 int                        0 Number of times object was selected for plan during compilation
		// LastOptSelectDate              datetime                   0 Last date the object was selected for plan during compilation
		// UsedCount                      int                        0 Number of times object was used in plan during execution
		// LastUsedDate                   datetime                   0 Last date the object was used in plan during execution
		// RowsInserted                   intn                       1 Number of rows inserted
		// RowsDeleted                    intn                       1 Number of rows deleted
		// RowsUpdated                    intn                       1 Number of updates
		// Operations                     intn                       1 Number of times that the object was accessed
		// LockRequests                   intn                       1 Number of requests for a lock on the object
		// LockWaits                      intn                       1 Number of times a task waited for a lock for the object
		//---------------------------------------------------------------------------------------------------
		// Column changes in various versions:
		//
		// Version      Action Name              Datatype   Attributes Description
		// ------------ ------ ----------------- ---------- ---------- ----------------------------------
		// 12.5.3       >>>>>> change behaviour on the table <<<<<<<<< monOpenObjectActivity contains details about tables and indexes only. Prior to 12.5.3, this table could contain rowsa row for an executed stored procedure, but these details (like the Operations column) were not reliable. 
		// 15.0         add    DBName            varchar(30)           Name of the database in which the object resides
		// 15.0.1CE/15.5 add   InstanceId        int                   The Server Instance Identifier (cluster only)
		// 15.0.2       add    HkgcRequests      int                   Total number of events of the object that were queued in Housekeeper Garbage Collection(HKGC) queues
		// 15.0.2       add    HkgcPending       int                   Number of events of the object that are still pending in Housekeeper Garbage Collection(HKGC) queues
		// 15.0.2       add    HkgcOverflows     int                   Number of events of the object that overflowed due to lack of space in Housekeeper Garbage Collection(HKGC) queues
		// 15.0.2       >>>>>> change behaviour on the table <<<<<<<<< Among various bugfixes in 15.0.2, one that is worth mentioning is the number of table scans or index scans on a table can now be reliably derived from monOpenObjectActivity.UsedCount for rows with IndexID = 0. Previously, this value was not correct as it included accesses via a clustered index as well. 
		// 15.5         add    PhysicalLocks             int           only CE: Number of physical locks requested
		// 15.5         add    PhysicalLocksRetained     int           only CE: Number of physical locks retained
		// 15.5         add    PhysicalLocksRetainWaited int           only CE: Number of physical lock requests waited before lock is retained
		// 15.5         add    PhysicalLocksDeadlocks    int           only CE: Number of times physical lock requests returned deadlock
		// 15.5         add    PhysicalLocksWaited       int           only CE: Number of times physical lock requests waited
		// 15.5         add    PhysicalLocksPageTransfer int           only CE: Number of times page transfer was requested by a client at this instance
		// 15.5         add    TransferReqWaited         int           only CE: Number of times physical lock requests waited to receive page transfers
		// 15.5         add    AvgPhysicalLockWaitTime   float         only CE: Average time waited to get physical lock granted (ms)
		// 15.5         add    AvgTransferReqWaitTime    float         only CE: Average time physical lock requests waited to receive page transfers (ms)
		// 15.5         add    TotalServiceRequests      int           only CE: Number of physical lock requests serviced by Cluster Cache Manager
		// 15.5         add    PhysicalLocksDowngraded   int           only CE: Number of physical lock downgrade requests serviced by Cluster Cache Manager
		// 15.5         add    PagesTransferred          int           only CE: Number of pages transferred by Cluster Cache Manager
		// 15.5         add    ClusterPageWrites         int           only CE: Number of pages written to disk by Cluster Cache Manager
		// 15.5         add    AvgServiceTime            int           only CE: Average service time taken by Cluster Cache Manager (ms)
		// 15.5         add    AvgTimeWaitedOnLocalUsers float         only CE: Average time taken to service requests due to page in use by local users (ms)
		// 15.5         add    AvgTransferSendWaitTime   float         only CE: Average time waited by Cluster Cache Manager for page transfer (ms)
		// 15.5         add    AvgIOServiceTime          float         only CE: Average service time taken by Cluster Cache Manager for writing page to disk (ms)
		// 15.5         add    AvgDowngradeServiceTime   float         only CE: Average service time taken by Cluster Cache Manager for downgrading physical lock (ms)
		// 15.5 esd#1   add    MaxPhysicalLockWaitTime   float         only CE: Maximum time waited to get physical lock granted (ms)
		// 15.5 esd#1   add    MaxTransferReqWaitTime    float         only CE: Maximum time physical lock requests waited to receive page transfers (ms)
		// 15.5 esd#1   add    MaxServiceTime            float         only CE: Maximum service time taken by Cluster Cache Manager for downgrading physical lock (ms)
		// 15.5 esd#1   add    AvgQueueWaitTime          float         only CE: Average wait time in Cluster Cache Manager queue before request is serviced (ms)
		// 15.5 esd#1   add    MaxQueueWaitTime          float         only CE: Maximum wait time in Cluster Cache Manager queue before request is serviced (ms)
		// 15.5 esd#1   add    MaxTimeWaitedOnLocalUsers float         only CE: Maximum time taken to service requests due to page in use by local users (ms)
		// 15.5 esd#1   add    MaxTransferSendWaitTime   float         only CE: Maximum time waited by Cluster Cache Manager for page transfer (ms)
		// 15.5 esd#1   add    MaxIOServiceTime          float         only CE: Maximum service time taken by Cluster Cache Manager for writing page to disk (ms)
		// 15.5 esd#1   add    MaxDowngradeServiceTime   float         only CE: Maximum service time taken by Cluster Cache Manager for downgrading physical lock (ms)
        // 15.7 (3B)    add    SharedLockWaitTime        int           Counter,reset,null  The total amount of time (in milliseconds) that all tasks spent waiting for a shared lock
        // 15.7 (3B)    add    ExclusiveLockWaitTime     int           Counter,reset,null  The total amount of time (in milliseconds) that all tasks spent waiting for an exclusive lock
        // 15.7 (3B)    add    UpdateLockWaitTime        int           Counter,reset,null  The total amount of time (in milliseconds) that all tasks spent waiting for an update lock
        //---------------------------------------------------------------------------------------------------

		name         = CM_NAME__OBJECT_ACTIVITY;
		displayName  = CM_DESC__OBJECT_ACTIVITY;
		description  = "<html>" +
							"Performance information about object/tables." +
							"<br><br>" +
							"Table Background colors:" +
							"<ul>" +
							"    <li>ORANGE - An Index.</li>" +
							"</ul>" +
						"</html>";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 0;
		needCeVersion= 0;
		monTables    = new String[] {"monOpenObjectActivity"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring=1", "object lockwait timing=1", "per object statistics active=1"};
		colsCalcDiff = new String[] {"LogicalReads","PhysicalReads", "APFReads", "PagesRead", "PhysicalWrites", "PagesWritten", "UsedCount", "RowsInsUpdDel", "RowsInserted", "RowsDeleted", "RowsUpdated", "Operations", "LockRequests", "LockWaits", "HkgcRequests", "HkgcPending", "HkgcOverflows", "OptSelectCount", "PhysicalLocks", "PhysicalLocksRetained", "PhysicalLocksRetainWaited", "PhysicalLocksDeadlocks", "PhysicalLocksWaited", "PhysicalLocksPageTransfer", "TransferReqWaited", "TotalServiceRequests", "PhysicalLocksDowngraded", "PagesTransferred", "ClusterPageWrites", "SharedLockWaitTime", "ExclusiveLockWaitTime", "UpdateLockWaitTime"};
		colsCalcPCT  = new String[] {"LockContPct"};
		pkList       = new LinkedList<String>();
//			pkList.add("DBName");
//			pkList.add("ObjectName");
			pkList.add("DBID");
			pkList.add("ObjectID");
			pkList.add("IndexID");


		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				true, true)
		{
			private static final long serialVersionUID = 6315460036795875430L;

			@Override
			public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				try 
				{
					MonTablesDictionary mtd = MonTablesDictionary.getInstance();
					mtd.addColumn("monOpenObjectActivity", "LockContPct"                    ,"<html>How many Lock Requests in percent was Blocked by another concurrent SPID's due to incompatible locking issues.<br><b>Note</b>: Do also considder number of LockWaits and not only the percentage.<br><b>Formula</b>: LockWaits / LockRequests * 100.0<br></html>");
					mtd.addColumn("monOpenObjectActivity", "TabRowCount"                    ,"<html>Table rowcount, using row_count(DBID, ObjectID) to get the count, so it can be a bit off from the actual number of rows.<br><b>Note</b>: If this takes to much resources, it can be disable in any of the configuration files using <code>"+CM_NAME__OBJECT_ACTIVITY+".TabRowCount=false</code>.</html>");
					mtd.addColumn("monOpenObjectActivity", "NumUsedPages"                   ,"<html>Number of Pages used by this table/index, using data_pages(DBID, ObjectID, IndexID) to get the value.<br><b>Note</b>: If this takes to much resources, it can be disable in any of the configuration files using <code>"+CM_NAME__OBJECT_ACTIVITY+".TabRowCount=false</code>.</html>");
					mtd.addColumn("monOpenObjectActivity", "RowsPerPage"                    ,"<html>Number of rows per page.<br><b>Formula</b>: TabRowCount/NumUsedPages</html>");
					mtd.addColumn("monOpenObjectActivity", "RowsInsUpdDel"                  ,"<html>RowsInsUpdDel = RowsInserted + RowsDeleted + RowsUpdated<br>So this is simply a summary of all DML changes on this table.</html>");
					mtd.addColumn("monOpenObjectActivity", "Remark"                         ,"<html>Some tip of what's happening with this table<br><b>Tip</b>: \"Hover\" over the cell to get more information on the Tip.</html>");
					mtd.addColumn("monOpenObjectActivity", "IndexName"                      ,"<html>Name of the index.<br><b>Formula</b>: using ASE Function index_name(DBID, ObjectID, IndexID) to get the name of the index.</html>");

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
			}

			@Override
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return new String[] {"enable monitoring=1", "object lockwait timing=1", "per object statistics active=1"};
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				List <String> pkCols = new LinkedList<String>();

				if (isClusterEnabled)
					pkCols.add("InstanceID");

//				pkCols.add("DBName");
//				pkCols.add("ObjectName");
				pkCols.add("DBID");
				pkCols.add("ObjectID");
				pkCols.add("IndexID");

				return pkCols;
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				// this is used for, sum() or columns concatinated that will overflowing the 4 byte boundary
				//
				// In PRE 15.x, we use numeric, in post 15 we use bigint
				// well max for a bigint (8 byte int) is really: 9223372036854775807
				// which is equivalent to a numeric(19,0), but lets cahnge that in the future
				// I think 10,0 is fine, bacuase int (4 byte int) max is 2147483647 (0x7fffffff)
				// so concatinate at least 3 int should be possible... 
				String bigint = "numeric(10,0)"; // bigint if above 15.0

				String TabRowCount  = "";
				String NumUsedPages = "";
				String RowsPerPage  = "";
				String DBName       = "DBName=db_name(A.DBID), \n";
				String ObjectName   = "ObjectName=isnull(object_name(A.ObjectID, A.DBID), 'ObjId='+convert(varchar(30),A.ObjectID)), \n"; // if user is not a valid user in A.DBID, then object_name() will return null
				String IndexName    = "";

				// ASE 15.7
				String SharedLockWaitTime    = "";
				String ExclusiveLockWaitTime = "";
				String UpdateLockWaitTime    = "";
				String ase15700_nl           = ""; // NL for this section

				if (aseVersion >= 15000)
					bigint = "bigint";

				if (aseVersion >= 15020)
				{
					TabRowCount  = "TabRowCount  = convert(bigint, row_count(A.DBID, A.ObjectID)),             -- Disable col with property: CMobjectActivity.TabRowCount=false\n";
					NumUsedPages = "NumUsedPages = convert(bigint, data_pages(A.DBID, A.ObjectID, A.IndexID)), -- Disable col with property: CMobjectActivity.TabRowCount=false\n";
					RowsPerPage  = "RowsPerPage  = convert(numeric(6,1), 0),                                   -- Disable col with property: CMobjectActivity.TabRowCount=false\n";
					DBName       = "A.DBName, \n";
//					ObjectName   = "A.ObjectName, \n";
					ObjectName   = "ObjectName = isnull(object_name(A.ObjectID, A.DBID), 'Obj='+A.ObjectName), \n"; // if user is not a valid user in A.DBID, then object_name() will return null
					IndexName    = "IndexName = CASE WHEN IndexID=0 THEN convert(varchar(30),'DATA') \n" +
					               "                 ELSE convert(varchar(30), isnull(index_name(DBID, ObjectID, IndexID), '-unknown-')) \n" +
					               "            END, \n";

					// debug/trace
					Configuration conf = Configuration.getCombinedConfiguration();
					if (conf.getBooleanProperty(getName()+".ObjectName", false))
					{
						ObjectName = "ObjectName=isnull(object_name(A.ObjectID, A.DBID), 'ObjId='+convert(varchar(30),A.ObjectID))"; // if user is not a valid user in A.DBID, then object_name() will return null
						_logger.info(getName()+".ObjectName=true, using the string '"+ObjectName+"' for ObjectName lookup.");
						ObjectName += ", \n";
					}
					if (conf.getBooleanProperty(getName()+".TabRowCount", true) == false)
					{
						TabRowCount  = "TabRowCount  = convert(bigint,-1), -- column is disabled, enable col with property: CMobjectActivity.TabRowCount=true\n";
						NumUsedPages = "NumUsedPages = convert(bigint,-1), -- column is disabled, enable col with property: CMobjectActivity.TabRowCount=true\n";
						RowsPerPage  = "RowsPerPage  = convert(bigint,-1), -- column is disabled, enable col with property: CMobjectActivity.TabRowCount=true\n";
						_logger.info(getName()+".TabRowCount=false, Disabling the column 'TabRowCount', 'NumUsedPages', 'RowsPerPage'.");
					}
				}
				if (aseVersion >= 15700)
				{
					SharedLockWaitTime    = "SharedLockWaitTime, ";
					ExclusiveLockWaitTime = "ExclusiveLockWaitTime, ";
					UpdateLockWaitTime    = "UpdateLockWaitTime, ";

					ase15700_nl           = "\n"; // NL for this section
				}

				if (isClusterEnabled)
				{
					cols1 += "InstanceID, ";
				}

				cols1 += "A.DBID, A.ObjectID, \n" +
				         DBName +
				         ObjectName + 
				         "A.IndexID, \n" +
				         IndexName +
				         "LockScheme = lockscheme(A.ObjectID, A.DBID), \n" +
				         "Remark = convert(varchar(60), ''), \n" + // this would be a good position after X tests has been done, but put it at the end right now
				         "LockRequests=isnull(LockRequests,0), LockWaits=isnull(LockWaits,0), \n" +
				         "LockContPct = CASE WHEN isnull(LockRequests,0) > 0 \n" +
				         "                   THEN convert(numeric(10,1), ((LockWaits+0.0)/(LockRequests+0.0)) * 100.0) \n" +
				         "                   ELSE convert(numeric(10,1), 0.0) \n" +
				         "              END, \n" +
				         SharedLockWaitTime + ExclusiveLockWaitTime + UpdateLockWaitTime + ase15700_nl +
				         "LogicalReads, PhysicalReads, APFReads, PagesRead, \n" +
				         "PhysicalWrites, PagesWritten, UsedCount, Operations, \n" +
				         TabRowCount +
				         NumUsedPages +
				         RowsPerPage +
				         // RowsInserted + RowsDeleted + RowsUpdated : will overflow if much changes, so individual converts are neccecary
				         "RowsInsUpdDel=convert("+bigint+",RowsInserted) + convert("+bigint+",RowsDeleted) + convert("+bigint+",RowsUpdated), \n" +
				         "RowsInserted, RowsDeleted, RowsUpdated, OptSelectCount, \n";
				cols2 += "";
				cols3 += "LastOptSelectDate, LastUsedDate";
			//	cols3 = "OptSelectCount, LastOptSelectDate, LastUsedDate, LastOptSelectDateDiff=datediff(ss,LastOptSelectDate,getdate()), LastUsedDateDiff=datediff(ss,LastUsedDate,getdate())";
			// it looked like we got "overflow" in the datediff sometimes... And I have newer really used these cols, so lets take them out for a while...
				if (aseVersion >= 15020)
				{
					cols2 += "HkgcRequests, HkgcPending, HkgcOverflows, \n";
				}


				//-------------------------------------------
				// Adding Cluster Edition Specific Counters
				//-------------------------------------------

				// ASE 15.0.3 CE
				String PhysicalLocks             = "";
				String PhysicalLocksRetained     = "";
				String PhysicalLocksRetainWaited = "";
				String PhysicalLocksDeadlocks    = "";
				String PhysicalLocksWaited       = "";
				String PhysicalLocksPageTransfer = "";
				String TransferReqWaited         = "";
				String AvgPhysicalLockWaitTime   = "";
				String AvgTransferReqWaitTime    = "";
				String TotalServiceRequests      = "";
				String PhysicalLocksDowngraded   = "";
				String PagesTransferred          = "";
				String ClusterPageWrites         = "";
				String AvgServiceTime            = "";
				String AvgTimeWaitedOnLocalUsers = "";
				String AvgTransferSendWaitTime   = "";
				String AvgIOServiceTime          = "";
				String AvgDowngradeServiceTime   = "";
				String ase15030_ce_nl            = ""; // NL for this section

				// ASE 15.5.0 ESD#1 CE
				String MaxPhysicalLockWaitTime   = "";
				String MaxTransferReqWaitTime    = "";
				String MaxServiceTime            = "";
				String AvgQueueWaitTime          = "";
				String MaxQueueWaitTime          = "";
				String MaxTimeWaitedOnLocalUsers = "";
				String MaxTransferSendWaitTime   = "";
				String MaxIOServiceTime          = "";
				String MaxDowngradeServiceTime   = "";
				String ase15501_ce_nl            = ""; // NL for this section

				if ( aseVersion >= 15030 && isClusterEnabled )
				{
					PhysicalLocks             = "PhysicalLocks, ";
					PhysicalLocksRetained     = "PhysicalLocksRetained, ";
					PhysicalLocksRetainWaited = "PhysicalLocksRetainWaited, ";
					PhysicalLocksDeadlocks    = "PhysicalLocksDeadlocks, ";
					PhysicalLocksWaited       = "PhysicalLocksWaited, ";
					PhysicalLocksPageTransfer = "PhysicalLocksPageTransfer, ";
					TransferReqWaited         = "TransferReqWaited, ";
					AvgPhysicalLockWaitTime   = "AvgPhysicalLockWaitTime, ";
					AvgTransferReqWaitTime    = "AvgTransferReqWaitTime, ";
					TotalServiceRequests      = "TotalServiceRequests, ";
					PhysicalLocksDowngraded   = "PhysicalLocksDowngraded, ";
					PagesTransferred          = "PagesTransferred, ";
					ClusterPageWrites         = "ClusterPageWrites, ";
					AvgServiceTime            = "AvgServiceTime, ";
					AvgTimeWaitedOnLocalUsers = "AvgTimeWaitedOnLocalUsers, ";
					AvgTransferSendWaitTime   = "AvgTransferSendWaitTime, ";
					AvgIOServiceTime          = "AvgIOServiceTime, ";
					AvgDowngradeServiceTime   = "AvgDowngradeServiceTime, ";
					ase15030_ce_nl            = "\n";
				}
				if ( aseVersion >= 15501 && isClusterEnabled )
				{
					MaxPhysicalLockWaitTime   = "MaxPhysicalLockWaitTime, ";
					MaxTransferReqWaitTime    = "MaxTransferReqWaitTime, ";
					MaxServiceTime            = "MaxServiceTime, ";
					AvgQueueWaitTime          = "AvgQueueWaitTime, ";
					MaxQueueWaitTime          = "MaxQueueWaitTime, ";
					MaxTimeWaitedOnLocalUsers = "MaxTimeWaitedOnLocalUsers, ";
					MaxTransferSendWaitTime   = "MaxTransferSendWaitTime, ";
					MaxIOServiceTime          = "MaxIOServiceTime, ";
					MaxDowngradeServiceTime   = "MaxDowngradeServiceTime, ";
					ase15501_ce_nl            = "\n";
				}
				cols2 += PhysicalLocks;
				cols2 += PhysicalLocksRetained;
				cols2 += PhysicalLocksRetainWaited;
				cols2 += PhysicalLocksDeadlocks;
				cols2 += PhysicalLocksWaited;
				cols2 += PhysicalLocksPageTransfer;
					cols2 += ase15030_ce_nl; // NL for this section
				cols2 += TransferReqWaited;
				cols2 += AvgPhysicalLockWaitTime;
				cols2 += MaxPhysicalLockWaitTime;
				cols2 += AvgTransferReqWaitTime;
				cols2 += MaxTransferReqWaitTime;
				cols2 += TotalServiceRequests;
				cols2 += PhysicalLocksDowngraded;
					cols2 += ase15030_ce_nl; // NL for this section
				cols2 += PagesTransferred;
				cols2 += ClusterPageWrites;
				cols2 += AvgServiceTime;
				cols2 += MaxServiceTime;
				cols2 += AvgTimeWaitedOnLocalUsers;
				cols2 += MaxTimeWaitedOnLocalUsers;
				cols2 += AvgTransferSendWaitTime;
				cols2 += MaxTransferSendWaitTime;
					cols2 += ase15501_ce_nl; // NL for this section
				cols2 += AvgIOServiceTime;
				cols2 += MaxIOServiceTime;
				cols2 += AvgDowngradeServiceTime;
				cols2 += MaxDowngradeServiceTime;
				cols2 += AvgQueueWaitTime;
				cols2 += MaxQueueWaitTime;
					cols2 += ase15501_ce_nl; // NL for this section

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from master..monOpenObjectActivity A \n" +
					"where UsedCount > 0 OR LockRequests > 0 OR LogicalReads > 100 \n" +
//					(isClusterEnabled ? "order by 2,3,4" : "order by 1,2,3") + "\n";
					"order by LogicalReads desc \n";

				return sql;
			}

			/** 
			 * Compute the LockContPct for DIFF values
			 */
			public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
			{
				int LockContPct_pos = -1;

				int LockRequests,          LockWaits;
				int LockRequests_pos = -1, LockWaits_pos = -1;
				
				int TabRowCount_pos  = -1; long TabRowCount;
				int NumUsedPages_pos = -1; long NumUsedPages;
				int RowsPerPage_pos  = -1; // we will SET this value, so only need the position

				// set some "Remark", below is columns user to draw some conclutions 
				int Remark_pos       = -1;
				int UsedCount_pos    = -1;
				int IndexID_pos      = -1;
				int LogicalReads_pos = -1;
				int RowsInserted_pos = -1;
				
				// Find column Id's
				List<String> colNames = diffData.getColNames();
				if (colNames == null)
					return;

				for (int colId=0; colId < colNames.size(); colId++) 
				{
					String colName = colNames.get(colId);
					if      (colName.equals("LockRequests")) LockRequests_pos = colId;
					else if (colName.equals("LockWaits"))    LockWaits_pos    = colId;
					else if (colName.equals("LockContPct"))  LockContPct_pos  = colId;
					else if (colName.equals("TabRowCount"))  TabRowCount_pos  = colId;
					else if (colName.equals("NumUsedPages")) NumUsedPages_pos = colId;
					else if (colName.equals("RowsPerPage"))  RowsPerPage_pos  = colId;
					else if (colName.equals("Remark"))       Remark_pos       = colId;
					else if (colName.equals("UsedCount"))    UsedCount_pos    = colId;
					else if (colName.equals("IndexID"))      IndexID_pos      = colId;
					else if (colName.equals("LogicalReads")) LogicalReads_pos = colId;
					else if (colName.equals("RowsInserted")) RowsInserted_pos = colId;

					// Noo need to continue, we got all our columns
					if (    LockRequests_pos  >= 0 
					     && LockWaits_pos     >= 0 
					     && LockContPct_pos   >= 0  
					     && TabRowCount_pos   >= 0  
					     && NumUsedPages_pos  >= 0  
					     && RowsPerPage_pos   >= 0  
					     && Remark_pos        >= 0  
					     && UsedCount_pos     >= 0  
					     && IndexID_pos       >= 0  
					     && LogicalReads_pos  >= 0  
					     && RowsInserted_pos  >= 0  
					   )
						break;
				}

				// Loop on all diffData rows
				for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
				{
					LockRequests = ((Number)diffData.getValueAt(rowId, LockRequests_pos)).intValue();
					LockWaits    = ((Number)diffData.getValueAt(rowId, LockWaits_pos   )).intValue();

					//------------------------------
					// CALC: LockContPct
					int colPos = LockContPct_pos;
					if (LockRequests > 0)
					{
						// LockWaits / LockRequests * 100;
						double calc = ((LockWaits+0.0) / (LockRequests+0.0)) * 100.0;

						BigDecimal newVal = new BigDecimal(calc).setScale(2, BigDecimal.ROUND_HALF_EVEN);
						diffData.setValueAt(newVal, rowId, colPos);
					}
					else
						diffData.setValueAt(new BigDecimal(0).setScale(2, BigDecimal.ROUND_HALF_EVEN), rowId, colPos);

					//------------------------------
					// CALC: RowsPerPage
					// If we got all columns: RowsPerPage, TabRowCount, NumUsedPages
					if (RowsPerPage_pos >= 0 && TabRowCount_pos >= 0 && NumUsedPages_pos >= 0 )
					{
						TabRowCount  = ((Number)diffData.getValueAt(rowId, TabRowCount_pos )).longValue();
						NumUsedPages = ((Number)diffData.getValueAt(rowId, NumUsedPages_pos)).longValue();

						colPos = RowsPerPage_pos;
						if (NumUsedPages > 0)
						{
							// RowsPerPage = TabRowCount / NumUsedPages;
							double calc = ((TabRowCount+0.0) / (NumUsedPages+0.0));

							BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);
							diffData.setValueAt(newVal, rowId, colPos);
						}
						else
							diffData.setValueAt(new BigDecimal(0).setScale(1, BigDecimal.ROUND_HALF_EVEN), rowId, colPos);
					}

					//------------------------------
					// CALC: Remark
					// A LOT of stuff can be included here...
					if (Remark_pos >= 0 && UsedCount_pos >= 0 && IndexID_pos >= 0 )
					{
						int UsedCount    = ((Number)diffData.getValueAt(rowId, UsedCount_pos   )).intValue();;
						int IndexID      = ((Number)diffData.getValueAt(rowId, IndexID_pos     )).intValue();;
						int LogicalReads = ((Number)diffData.getValueAt(rowId, LogicalReads_pos)).intValue();;
						int RowsInserted = ((Number)diffData.getValueAt(rowId, RowsInserted_pos)).intValue();;
						
						String remark = null;
						if (IndexID == 0 && UsedCount > 0 && LogicalReads > 0)
						{
//							remark = RemarkDictionary.T_SCAN_OR_HTAB_INS;
							remark = RemarkDictionary.TABLE_SCAN;

							if ( MathUtils.pctNear(10, UsedCount, RowsInserted) )
								remark = RemarkDictionary.HEAP_TAB_INS;
						}
						
						// If we got any remarks, set it...
						if ( ! StringUtil.isNullOrBlank(remark) )
						{
							colPos = Remark_pos;
							diffData.setValueAt(remark, rowId, colPos);
						}
					}
				}
			} // end localCalculation

			/** 
			 * Get number of rows to save/request ddl information for 
			 */
			@Override
			public int getMaxNumOfDdlsToPersist()
			{
				return 10;
			}
			@Override
			public String[] getDdlDetailsSortOnColName()
			{
				String[] sa = {"LogicalReads", "APFReads", "PhysicalReads", "LockWaits"};
				return sa;
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (AseTune.hasGUI())
		{
			final String localName = name;
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName())
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected JPanel createLocalOptionsPanel()
				{
					JPanel panel = SwingUtils.createPanel("Local Options", true);
					panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

					Configuration conf = Configuration.getCombinedConfiguration();
					boolean defaultOpt = conf == null ? true : conf.getBooleanProperty(localName+".TabRowCount", true);
					JCheckBox sampleRowCount_chk = new JCheckBox("Sample Table Row Count", defaultOpt);

					sampleRowCount_chk.setName(localName+".TabRowCount");
					sampleRowCount_chk.setToolTipText("<html>" +
							"Sample Table Row Count using ASE functions <code>row_count()</code> and <code>data_pages()</code>.<br>" +
							"<b>Note 1</b>: Only in ASE 15.0.2 or higher.<br>" +
							"<b>Note 2</b>: You can also set the property 'CMobjectActivity.TabRowCount=true|false' in the configuration file.<br>" +
							"<b>Note 3</b>: To check if this is enabled or not, use the Properties dialog in this tab pane, right click + properties...<br>" +
							"</html>");
					panel.add(sampleRowCount_chk, "wrap");

					sampleRowCount_chk.addActionListener(new ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent e)
						{
							// Need TMP since we are going to save the configuration somewhere
							Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
							if (conf == null) return;
							conf.setProperty(localName+".TabRowCount", ((JCheckBox)e.getSource()).isSelected());
							conf.save();
							
							// This will force the CM to re-initialize the SQL statement.
							CountersModel cm = getCmByName(localName);
							if (cm != null)
								cm.setSql(null);
						}
					});
					
					return panel;
				}

				@Override
				public boolean ddlRequestInfo()
				{
					return true;
				}
				@Override
				public void ddlRequestInfoSave(JTable table)
				{
					if (table == null)
						return;

					if ( ! PersistentCounterHandler.hasInstance() )
						return;

					PersistentCounterHandler pch = PersistentCounterHandler.getInstance();

					int DBName_pos     = -1;
					int ObjectName_pos = -1;
//					int IndexID_pos    = -1;
					for (int c=0; c<table.getColumnCount(); c++)
					{
						if ( "DBName".equals(table.getColumnName(c)) )
							DBName_pos = c;

						if ( "ObjectName".equals(table.getColumnName(c)) )
							ObjectName_pos = c;

//						if ( "IndexID".equals(table.getColumnName(c)) )
//							IndexID_pos = c;

//						if (DBName_pos >= 0 && ObjectName_pos >= 0 && IndexID_pos >= 0)
//							break;
						if (DBName_pos >= 0 && ObjectName_pos >= 0)
							break;
					}

					// HOW MANY TOP ROWS SHOULD WE GRAB FROM THE JTABLE
					int NUM_OF_DDLS_TO_PERSIST = 10;
					
					int rows = Math.min(NUM_OF_DDLS_TO_PERSIST, table.getRowCount());
					for (int r=0; r<rows; r++)
					{
						Object DBName_obj     = table.getValueAt(r, DBName_pos);
						Object ObjectName_obj = table.getValueAt(r, ObjectName_pos);
//						Object IndexID_obj    = table.getValueAt(r, IndexID_pos);

						// Skip index rows... (change the loop to do this)
//						if (IndexID_obj instanceof Number)
//						{
//							if ( ((Number)IndexID_obj).intValue() > 0 )
//								continue;
//						}
						if (DBName_obj instanceof String && ObjectName_obj instanceof String)
							pch.addDdl((String)DBName_obj, (String)ObjectName_obj, getName()+".guiSorted, row="+r);
					}
					
				}
			};
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_object_activity.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );

//			tcp.setTableToolTipText(
//				    "<html>" +
//				        "Background colors:" +
//				        "<ul>" +
//				        "    <li>ORANGE - An Index.</li>" +
//				        "</ul>" +
//				    "</html>");

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
		
		//==================================================================================================
		// 12.5.0.3: Description of: monProcess
		// 
		// Description:
		//
		//
		// Needs Configuration:
		// none
		// 
		// Name                           Datatype     Attributes  Ind Description
		// ------------------------------ ------------ ----------- --- ------------------------------------------------------------
		// SPID                           smallint                   0 Session Process Identifier
		// KPID                           int                        0 Kernel Process Identifier
		// FamilyID                       smallint                   0 The SPID of the parent process, if this is a worker process
		// BatchID                        int                        0 Unique identifier for the SQL batch containing the statement being executed
		// ContextID                      int                        0 The stack frame of the procedure, if a procedure
		// LineNumber                     int                        0 Line number of the current statement within the SQL Batch
		// SecondsConnected               int                        0 The number of seconds since this connection was established
		// WaitEventID                    smallint                   0 Unique identifier for the event that the process is waiting for, if the process is currently in a wait state
		// BlockingSPID                   smallint                   0 Session Process identifier of the process holding the lock that this process has requested, if waiting for a lock
		// DBID                           int                        0 Unique identifier of the process' current database
		// EngineNumber                   smallint                   0 Unique identifier of the engine that the process is executing on
		// Priority                       int                        0 Priority at which the process is executing
		// Login                          varchar(30)                0 Login user name
		// Application                    varchar(30)                0 Application name
		// Command                        varchar(30)                0 Category of process or command that the process is currently executing
		// NumChildren                    intn                       0 Number of child processes, if executing a parallel query
		// SecondsWaiting                 intn                       0 Amount of time in seconds that process has been waiting, if the process is currently in a wait state
		// BlockingXLOID                  intn                       0 Unique Lock Identifier for the lock that this process has requested, if waiting for a lock
		// DBName                         varchar(30)                0 Name of process' current database
		// EngineGroupName                varchar(30)                0 Engine group for the process
		// ExecutionClass                 varchar(30)                0 Execution class for the process
		// MasterTransactionID            varchar(255)               0 Unique Transaction Identifier for the current transaction, if in a transaction
		//---------------------------------------------------------------------------------------------------
		// Column changes in various versions:
		//
		// Version      Action Name              Datatype   Attributes Description
		// ------------ ------ ----------------- ---------- ---------- ----------------------------------
		// 15.0.1CE/15.5 add   InstanceId        int                   The Server Instance Identifier (cluster only)
		// 15.0.2 esd#5 add    ServerUserID      int                   Server User Identifier of the user running this process. This matches the syslogins.suid column. The corresponding name can be obtained using the 'suser_name' function
		// 15.7 3a      add    ProgramName       varchar(30)      Null Name of the program on which the process is running.
		// 15.7 3b      remove ProgramName       varchar(30)      Null Name of the program on which the process is running.
		// 15.7 3a      add    HostName          varchar(30)      Null Name of the host machine on which the application that started the process is running.
		// 15.7 3a      add    ClientName        varchar(30)      Null Value of the clientname property set by the application.
		// 15.7 3a      add    ClientHostName    varchar(30)      Null Value of the clienthostname property set by the application.
		// 15.7 3a      add    ClientApplName    varchar(30)      Null Value of the clientapplname property set by the application.
		//---------------------------------------------------------------------------------------------------

		name         = CM_NAME__PROCESS_ACTIVITY;
		displayName  = CM_DESC__PROCESS_ACTIVITY;
		description  = "<html>" +
							"<p>What SybasePIDs are doing what.</p>" +
							"Tip:<br>" +
							"sort by 'BatchIdDiff', will give you the one that executes the most SQL Batches.<br>" +
							"Or check 'WaitEventDesc' to find out when the SPID is waiting for." +
							"<br><br>" +
							"Table Background colors:" +
							"<ul>" +
							"    <li>YELLOW - SPID is a System Processes</li>" +
							"    <li>GREEN  - SPID is Executing(running) or are in the Run Queue Awaiting a time slot to Execute (runnable)</li>" +
							"    <li>PINK   - SPID is Blocked by some other SPID that holds a Lock on a database object Table, Page or Row. This is the Lock Victim.</li>" +
							"    <li>RED    - SPID is Blocking other SPID's from running, this SPID is Responslibe or the Root Cause of a Blocking Lock.</li>" +
							"</ul>" +
						"</html>";
		
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
//			private boolean _sampleSystemThreads = _conf.getBooleanProperty(getName()+".sample.systemThreads", true);

			@Override
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return new String[] {"enable monitoring=1", "object lockwait timing=1", "wait event timing=1"};
			}

			@Override
			public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				List <String> pkCols = new LinkedList<String>();

				if (isClusterEnabled)
					pkCols.add("InstanceID");

				pkCols.add("SPID");
				pkCols.add("KPID");
				
				return pkCols;
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				String optGoalPlan = "";
				if (aseVersion >= 15020)
				{
					optGoalPlan = "plan '(use optgoal allrows_dss)' \n";
				}

				if (isClusterEnabled)
				{
					cols1 += "MP.InstanceID, ";
				}

				String preDropTempTables =
					"/*------ drop tempdb objects if we failed doing that in previous execution -------*/ \n" +
					"if ((select object_id('#monProcessActivity')) is not null) drop table #monProcessActivity \n" +
					"if ((select object_id('#monProcess'))         is not null) drop table #monProcess         \n" +
					"if ((select object_id('#monProcessNetIO'))    is not null) drop table #monProcessNetIO    \n" +
					"\n";

				String createTempTables =
					"/*------ Snapshot, the monXXX tables, hopefully this is less expensive than doing the join via CIS -------*/ \n" +
					"select * into #monProcessActivity from master..monProcessActivity \n" +
					"select * into #monProcess         from master..monProcess         \n" +
					"select * into #monProcessNetIO    from master..monProcessNetIO    \n" +
					"\n";

				String dropTempTables = 
					"\n" +
					"/*------ drop tempdb objects -------*/ \n" +
					"drop table #monProcessActivity \n" +
					"drop table #monProcess         \n" +
					"drop table #monProcessNetIO    \n" +
					"\n";

				
				cols1+=" MP.FamilyID, MP.SPID, MP.KPID, MP.NumChildren, \n"
					+ "  SP.status, MP.WaitEventID, \n"
					+ "  WaitClassDesc=convert(varchar(50),''), " // value will be replaced in method localCalculation()
					+ "  WaitEventDesc=convert(varchar(50),''), " // value will be replaced in method localCalculation()
					+ "  MP.SecondsWaiting, MP.BlockingSPID, MP.Command, \n"
					+ "  MP.BatchID, BatchIdDiff=convert(int,MP.BatchID), \n" // BatchIdDiff diff calculated
					+ "  procName = object_name(SP.id, SP.dbid), SP.stmtnum, SP.linenum, \n"
					+ "  MP.Application, SP.clientname, SP.clienthostname, SP.clientapplname, "
					+ "  SP.hostname, SP.ipaddr, SP.hostprocess, \n"
					+ "  MP.DBName, MP.Login, SP.suid, MP.SecondsConnected, \n"
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
					"/*------ SQL to get data -------*/ \n" +
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from #monProcessActivity A, #monProcess MP, master..sysprocesses SP, #monProcessNetIO N \n" +
					"where MP.KPID = SP.kpid \n" +
					"  and MP.KPID = A.KPID \n" +
					"  and MP.KPID = N.KPID \n" +
					"  RUNTIME_REPLACE::SAMPLE_SYSTEM_THREADS \n"; // this is replaced in getSql()

				if (isClusterEnabled)
					sql +=
						"  and MP.InstanceID = SP.instanceid \n" + // FIXME: Need to check if this is working...
						"  and MP.InstanceID = A.InstanceID \n" +
						"  and MP.InstanceID = N.InstanceID \n";

				sql += "order by MP.SPID \n" + 
				       optGoalPlan;

				return preDropTempTables + createTempTables + sql + dropTempTables;
			}

			@Override
			public String getSql()
			{
				String sql = super.getSql();
				
//				Configuration conf = Configuration.getInstance(Configuration.TEMP);
				Configuration conf = Configuration.getCombinedConfiguration();
				boolean sampleSystemThreads_chk = (conf == null) ? true : conf.getBooleanProperty(getName()+".sample.systemThreads", true);

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
					String colName = colNames.get(colId);
					if      (colName.equals("WaitEventID"))   pos_WaitEventID   = colId;
					else if (colName.equals("WaitEventDesc")) pos_WaitEventDesc = colId;
					else if (colName.equals("WaitClassDesc")) pos_WaitClassDesc = colId;
					else if (colName.equals("BlockingSPID"))  pos_BlockingSPID  = colId;

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
				if (CM_GRAPH_NAME__PROCESS_ACTIVITY__CHKPT_HK.equals(tgdp.getName()))
				{
					int pos_Command = -1, pos_PhysicalWrites = -1;

					CounterTableModel counters = getCounterData(DATA_RATE);
					if (counters == null) 
						return;

					// Find column Id's
					List<String> colNames = counters.getColNames();
					if (colNames == null) 
						return;

					for (int colId=0; colId < colNames.size(); colId++) 
					{
						String colName = colNames.get(colId);
						if      (colName.equals("Command"))        pos_Command        = colId;
						else if (colName.equals("PhysicalWrites")) pos_PhysicalWrites = colId;

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
		tmp.addTrendGraphData(CM_GRAPH_NAME__PROCESS_ACTIVITY__CHKPT_HK, new TrendGraphDataPoint(CM_GRAPH_NAME__PROCESS_ACTIVITY__CHKPT_HK, new String[] { "Checkpoint Writes", "HK Wash Writes", "HK GC Writes", "HK Chores Writes" }));
		if (AseTune.hasGUI())
		{
			final String localName = name;
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName())
			{
				private static final long	serialVersionUID	= 1L;

				@Override
				protected JPanel createLocalOptionsPanel()
				{
					JPanel panel = SwingUtils.createPanel("Local Options", true);
					panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

//					Configuration conf = Configuration.getInstance(Configuration.TEMP);
					Configuration conf = Configuration.getCombinedConfiguration();
					JCheckBox sampleSystemThreads_chk = new JCheckBox("Show system processes", conf == null ? true : conf.getBooleanProperty(getName()+".sample.systemThreads", true));

					sampleSystemThreads_chk.setName(getName()+".sample.systemThreads");
					sampleSystemThreads_chk.setToolTipText("<html>Sample System SPID's that executes in the ASE Server.<br><b>Note</b>: This is not a filter, you will have to wait for next sample time for this option to take effect.</html>");
					panel.add(sampleSystemThreads_chk, "wrap");

					sampleSystemThreads_chk.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							// Need TMP since we are going to save the configuration somewhere
							Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
//							Configuration conf = Configuration.getCombinedConfiguration();
							if (conf == null) return;
							conf.setProperty(localName+".sample.systemThreads", ((JCheckBox)e.getSource()).isSelected());
							conf.save();
						}
					});
					
					return panel;
				}
			};
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_process_activity.png") );
//			tcp.setTableToolTipText(
//			    "<html>" +
////			        "Double-click a row to get the details for the SPID.<br>" +
////			        "<br>" +
//			        "Background colors:" +
//			        "<ul>" +
//			        "    <li>YELLOW - SPID is a System Processes</li>" +
//			        "    <li>GREEN  - SPID is Executing(running) or are in the Run Queue Awaiting a time slot to Execute (runnable)</li>" +
//			        "    <li>PINK   - SPID is Blocked by some other SPID that holds a Lock on a database object Table, Page or Row. This is the Lock Victim.</li>" +
//			        "    <li>RED    - SPID is Blocking other SPID's from running, this SPID is Responslibe or the Root Cause of a Blocking Lock.</li>" +
//			        "</ul>" +
//			    "</html>");
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );

			// GRAPH
			TrendGraph tg = new TrendGraph(CM_GRAPH_NAME__PROCESS_ACTIVITY__CHKPT_HK,
					"Chekpoint and HK Writes", 	// Menu Checkbox text
					"Checkpoint and Housekeeper Writes Per Second", // Label 
					new String[] { "Checkpoint Writes", "HK Wash Writes", "HK GC Writes", "HK Chores Writes" }, 
					false, tmp, false, -1);
			tmp.addTrendGraph(tg.getName(), tg, true);

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
					Number  thisSpid                       = (Number)                 adapter.getValue(adapter.getColumnIndex("SPID"));
					HashMap<Number,Object> blockingSpidMap = (HashMap<Number,Object>) adapter.getComponent().getClientProperty("blockingSpidMap");

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
		// Processes Wait
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------

		//==================================================================================================
		// 12.5.0.3: Description of: monProcessWaits
		// 
		// Description: Provides a server-wide view of where processes are waiting for an event. 
		//              monProcessWaits requires the 'enable monitoring' and 'process wait events' 
		//              configuration parameters to be enabled.
		//
		// Needs Configuration:
		// 'enable monitoring' and 'process wait events'
		// 
		// Name                           Datatype     Attributes  Ind Description
		// ------------------------------ ------------ ----------- --- ------------------------------------------------------------
		// SPID                           smallint                   0 Session process identifier
		// KPID                           int                        0 Kernel process identifier
		// WaitEventID                    smallint                   0 Unique identifier for the wait event
		// WaitTime                       int                        1 Amount of time (in seconds) that tasks have spent waiting for the event
		// Waits                          int                        0 Number of times tasks have waited for the event
		//---------------------------------------------------------------------------------------------------
		// Column changes in various versions:
		//
		// Version      Action Name              Datatype   Attributes Description
		// ------------ ------ ----------------- ---------- ---------- ----------------------------------
		// 15.0.2 esd#5 add    ServerUserID      int                   Server User Identifier of the user running this process. This matches the syslogins.suid column. The corresponding name can be obtained using the 'suser_name' function
		// 15.0.1CE/15.5 add   InstanceId        int                   The Server Instance Identifier (cluster only)
		//---------------------------------------------------------------------------------------------------

		name         = CM_NAME__PROCESS_WAIT;
		displayName  = CM_DESC__PROCESS_WAIT;
		description  = "<html>" +
			"What different resources are a Server SPID waiting for.<br>" +
			"<br>" +
			"<br>Note</b>: This is in experimental mode, it mighttake to much resources<br>" +
			"</html>";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 0;
		needCeVersion= 0;
		monTables    = new String[] {"monProcessWaits", "monWaitEventInfo", "monWaitClassInfo"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring=1", "process wait events=1"};
		colsCalcDiff = new String[] {"WaitTime", "Waits"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList<String>();
			pkList.add("SPID");
			pkList.add("KPID");
			pkList.add("WaitEventID");

	
		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				true, true)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return new String[] {"enable monitoring=1", "process wait events=1"};
			}

			@Override
			public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				try 
				{
					MonTablesDictionary mtd = MonTablesDictionary.getInstance();
					mtd.addColumn("monProcessWaits", "WaitTimePerWait", 
					    "<html>" +
					        "Wait time in seconds per wait. formula: diff.WaitTime / diff.Waits<br>" +
					        "Since WaitTime here is in seconds, this value will also be in seconds." +
					    "</html>");
				}
				catch (NameNotFoundException e) {/*ignore*/}
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				List <String> pkCols = new LinkedList<String>();

				if (isClusterEnabled)
					pkCols.add("InstanceID");

				pkCols.add("SPID");
				pkCols.add("KPID");
				pkCols.add("WaitEventID");

				return pkCols;
			}

			@Override
			public String getSqlInitForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String sql =
					"/*------ Create permanent tables for monWaitEventInfo & monWaitClassInfo in tempdb. -------*/ \n" +
					"/*------ hopefully this is less expensive than doing the join via CIS -------*/ \n" +
					"if ((select object_id('tempdb.guest.monWaitEventInfo')) is null) \n" +
					"   exec('select * into tempdb.guest.monWaitEventInfo from master..monWaitEventInfo') \n" +
					"\n" +
					"if ((select object_id('tempdb.guest.monWaitClassInfo')) is null) \n" +
					"   exec('select * into tempdb.guest.monWaitClassInfo from master..monWaitClassInfo') \n" +
					"\n";

				return sql;
			}
			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String cols = "";

				String InstanceID = ""; // in cluster
				String UserName   = ""; // in 15.0.2 esd#5

				if (isClusterEnabled)
					InstanceID = "W.InstanceID, ";

				if (aseVersion >= 15056)
					UserName = "UserName = suser_name(W.ServerUserID), ";

				cols = InstanceID + "W.SPID, W.KPID, " + UserName + "\n" +
					"Class=C.Description, Event=I.Description, \n" +
					"W.WaitEventID, W.WaitTime, W.Waits, \n" +
					"WaitTimePerWait = CASE WHEN W.Waits > 0 \n" +
					"                       THEN convert(numeric(15,3), (W.WaitTime + 0.0) / W.Waits) \n" +
					"                       ELSE convert(numeric(15,3), 0.0) \n" +
					"                  END \n";

				String sql = 
					"select " + cols +
					"from master..monProcessWaits W, tempdb.guest.monWaitEventInfo I, tempdb.guest.monWaitClassInfo C \n" +
					"where W.WaitEventID = I.WaitEventID \n" +
					"  and I.WaitClassID = C.WaitClassID \n" +
					"  and RUNTIME_REPLACE::EXTRA_WHERE_CLAUSE \n" +
					"order by " + (isClusterEnabled ? "W.SPID, W.WaitEventID, W.InstanceID" : "W.SPID, W.WaitEventID") + "\n" +
					"";

				return sql;
			}

			@Override
			public String getSql()
			{
				String sql = super.getSql();

				Configuration conf = Configuration.getCombinedConfiguration();
				String extraWhereClause = (conf == null) ? "" : conf.getProperty(getName()+".sample.extraWhereClause", "");
				if (StringUtil.isNullOrBlank(extraWhereClause))
					extraWhereClause = "1=1";

				sql = sql.replace("RUNTIME_REPLACE::EXTRA_WHERE_CLAUSE", extraWhereClause);
				
				return sql;
			}

			/** 
			 * Compute the WaitTimePerWait for diff values
			 */
			public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
			{
				int WaitTime,        Waits;
				int WaitTimeId = -1, WaitsId = -1;

				double calcWaitTimePerWait;
				int WaitTimePerWaitId = -1;

				// Find column Id's
				List<String> colNames = diffData.getColNames();
				if (colNames == null)
					return;

				for (int colId=0; colId < colNames.size(); colId++) 
				{
					String colName = colNames.get(colId);
					if      (colName.equals("WaitTimePerWait")) WaitTimePerWaitId = colId;
					else if (colName.equals("WaitTime"))        WaitTimeId        = colId;
					else if (colName.equals("Waits"))           WaitsId           = colId;
				}

				// Loop on all diffData rows
				for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
				{
					WaitTime = ((Number)diffData.getValueAt(rowId, WaitTimeId)).intValue();
					Waits    = ((Number)diffData.getValueAt(rowId, WaitsId   )).intValue();

					// int totIo = Reads + APFReads + Writes;
					if (Waits > 0)
					{
						// WaitTimePerWait = WaitTime / Waits;
						calcWaitTimePerWait = WaitTime / (Waits * 1.0);

						BigDecimal newVal = new BigDecimal(calcWaitTimePerWait).setScale(3, BigDecimal.ROUND_HALF_EVEN);;
						diffData.setValueAt(newVal, rowId, WaitTimePerWaitId);
					}
					else
						diffData.setValueAt(new BigDecimal(0), rowId, WaitTimePerWaitId);
				}
			}
		};
		
		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (AseTune.hasGUI())
		{
			final String localName = name;
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName())
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected JPanel createLocalOptionsPanel()
				{
					JPanel panel = SwingUtils.createPanel("Local Options", true);
					panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

					String tooltip = 
						"<html>" +
						"Add extra where clause to the query that fetches WaitTime for SPID's<br>" +
						"To check initial SQL statement that are used: Right click on the 'tab', and choose 'Properties'<br>" +
						"The extra string will replace the string 'RUNTIME_REPLACE::EXTRA_WHERE_CLAUSE'.<br>" +
						"<br>" +
						"<b>Examples:</b><br>" +
						"<b>- Only users with the login 'sa'</b><br>" +
						"<code>SPID in (select spid from master..sysprocesses where suser_name(suid) = 'sa')                     </code><br>" +
						"<br>" +
						"<b>- Same as above, but in a more efficent way (only in ASE 15.0.2 ESD#5 or higher)</b><br>" +
						"<code>suser_name(ServerUserID) = 'sa'                                                                   </code><br>" +
						"<br>" +
						"<b>- Only with programs that has logged in via 'isql'</b><br>" +
						"<code>SPID in (select spid from master..sysprocesses where program_name = 'isql')                       </code><br>" +
						"<br>" +
						"<b>- Only with clients that has logged in from the host 'host99'</b><br>" +
						"<code>SPID in (select spid from master..sysprocesses where hostname = 'host99')                         </code><br>" +
						"<br>" +
						"<b>- Only with clients that has logged in from the IP address '192.168.0.1'</b><br>" +
						"<code>SPID in (select spid from master..sysprocesses where ipaddr = '192.168.0.123')                    </code><br>" +
						"<br>" +
						"<b>- Only with clients that has logged in to ASE in the last 60 seconds</b><br>" +
						"<code>SPID in (select spid from master..sysprocesses where datediff(ss,loggedindatetime,getdate()) < 60)</code><br>" +
						"</html>";
					final RSyntaxTextArea extraWhereClause_txt = new RSyntaxTextArea();
					final JButton         extraWhereClause_but = new JButton("Apply Extra Where Clause");

					Configuration conf = Configuration.getCombinedConfiguration();
					String extraWhereClause = (conf == null ? "" : conf.getProperty(localName+".sample.extraWhereClause", ""));

					extraWhereClause_txt.setText(extraWhereClause);
					extraWhereClause_txt.setHighlightCurrentLine(false);
					extraWhereClause_txt.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
					extraWhereClause_txt.setName(localName+".sample.extraWhereClause");

					extraWhereClause_but.setToolTipText(tooltip);
					extraWhereClause_txt.setToolTipText(tooltip);

					panel.add(extraWhereClause_txt, "grow, push, wrap");
					panel.add(extraWhereClause_but, "wrap");

					extraWhereClause_but.addActionListener(new ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent e)
						{
							// Need TMP since we are going to save the configuration somewhere
							Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
							if (conf == null) return;
							conf.setProperty(localName+".sample.extraWhereClause", extraWhereClause_txt.getText().trim());
							conf.save();
						}
					});
					
					return panel;
				}
			};
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_process_wait_activity.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );
		}
		
		_CMList.add(tmp);





		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// Databases Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------

		//==================================================================================================
		// 12.5.0.3: Description of: monOpenDatabases
		// 
		// Description:
		//
		//
		// Needs Configuration:
		// none
		// 
		// Name                           Datatype     Attributes  Ind Description
		// ------------------------------ ------------ ----------- --- ------------------------------------------------------------
		// DBID                           int                        0 Unique identifier for the database
		// BackupStartTime                datetime                   0 Date that the last backup started for the database
		// BackupInProgress               int                        0 Whether a backup is currently in progress for the database
		// LastBackupFailed               int                        0 Whether the last back-up of the database failed
		// TransactionLogFull             int                        0 Whether the database transaction log is full
		// AppendLogRequests              int                        1 Number of semaphore requests when attempting to append to the database transaction log
		// AppendLogWaits                 int                        1 Number of times a task had to wait for the append log semaphore to be granted
		// DBName                         varchar(30)                0 Name of the database
		//---------------------------------------------------------------------------------------------------
		// Column changes in various versions:
		//
		// Version      Action Name              Datatype   Attributes Description
		// ------------ ------ ----------------- ---------- ---------- ----------------------------------
		// 12.5.1       add    QuiesceTag          varchar(30)         Quiesce Database tag, if applicable
		// 12.5.1       add    SuspendedProcesses  int                 The number of processes currently suspended due to the database transaction log being full
		// 15.0.1CE/15.5 add   InstanceId          int                 The Server Instance Identifier (cluster only)
		// 15.0.2 esd#5 add    LastCheckpointTime  datetime            date/time of the start of the last checkpoint for this database 
		// 15.0.2 esd#5 add    LastTranLogDumpTime datetime            date/time of the start of the last log dump for this database 
		//---------------------------------------------------------------------------------------------------

		name         = CM_NAME__OPEN_DATABASES;
		displayName  = CM_DESC__OPEN_DATABASES;
		description  =
			"<html>" +
			"Various information on a database level.<br>" +
			"<br>" +
			"<b>Note:</b><br>" +
			"Databases in the attached Graphs can be included or excluded<br>" +
			"Click button \"Set 'Graph' databases\"" +
			"<br><br>" +
			"Table Background colors:" +
			"<ul>" +
			"    <li>BLUE - A Database backup is in progress</li>" +
			"    <li>PINK - The transaction log for this database is filled to 90%, and will probably soon be full.</li>" +
			"    <li>RED  - The transaction log for this database is <b>full</b> and users are probably suspended.</li>" +
			"</ul>" +
			"</html>";

		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 0;
		needCeVersion= 0;
		monTables    = new String[] {"monOpenDatabases"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring=1"};
		colsCalcDiff = new String[] {"AppendLogRequests", "AppendLogWaits"};
		colsCalcPCT  = new String[] {"AppendLogContPct", "LogSizeUsedPct"};
		pkList       = new LinkedList<String>();
		     pkList.add("DBName");


		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				true, true)
		{
			private static final long serialVersionUID = 5078336367667465709L;

//			/** databases that should ALWAYS be part of the graphs */
//			private String[] _keepDbsInGraphs = StringUtil.commaStrToArray(Configuration.getCombinedConfiguration()
//					.getProperty(CM_NAME__OPEN_DATABASES+".keepDbsInGraphs", ""));
//
//			/** databases that should be left OUT in the graphs */
//			private String[] _skipDbsInGraphs = StringUtil.commaStrToArray(Configuration.getCombinedConfiguration()
//					.getProperty(CM_NAME__OPEN_DATABASES+".skipDbsInGraphs", 
//					"master, model, pubs2, sybmgmtdb, sybpcidb, sybsecurity, sybsystemdb, sybsystemprocs"));
//
//			/** databases size smaller than this should be left OUT in the graphs */
//			private int _skipDbsWithSizeLtInGraphs = Configuration.getCombinedConfiguration()
//					.getIntProperty(CM_NAME__OPEN_DATABASES+".skipDbsWithSizeLtInGraphs", 300);

			@Override
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return new String[] {"enable monitoring=1"};
			}

			@Override
			public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				try 
				{
					MonTablesDictionary mtd = MonTablesDictionary.getInstance();
					mtd.addColumn("monOpenDatabases", "CeDbRecoveryStatus", "<html>" +
					                                                             "1 = The database is currently undergoing <B>node-failure</B> recovery.<br> " +
					                                                             "0 = Normal, <B>not</B> in node-failure recovery." +
					                                                        "</html>");
					mtd.addColumn("monOpenDatabases", "AppendLogContPct",   "<html>" +
					                                                             "Log Semaphore Contention in percent.<br> " +
					                                                             "<b>Formula</b>: Pct = (AppendLogWaits / AppendLogRequests) * 100<br>" +
					                                                        "</html>");
					mtd.addColumn("monOpenDatabases", "DbSizeInMb",         "<html>Database size in MB</html>");
					mtd.addColumn("monOpenDatabases", "LogSizeInMb",        "<html>" +
					                                                             "Size in MB of the transaction log in the database. <br>" +
					                                                             "<b>Formula</b>: This is simply grabbed by: sum(size) from sysusages where (segmap & 4) = 4<br>" +
					                                                        "</html>");
					mtd.addColumn("monOpenDatabases", "LogSizeFreeInMb",    "<html>" +
					                                                             "How many MB have we got left in the Transaction log.<br> " +
					                                                             "<b>Formula</b>: (lct_admin('logsegment_freepages',DBID)-lct_admin('reserved_for_rollbacks',DBID)) / (1024.0*1024.0/@@maxpagesize)<br>" +
					                                                             "<b>Note 1</b>: This is the same formula as sp_helpdb 'dbname' uses to calculate space left.<br>" +
					                                                             "<b>Note 2</b>: This might not work correct for databases with mixed data and log.<br>" +
					                                                        "</html>");
					mtd.addColumn("monOpenDatabases", "LogSizeUsedPct",     "<html>" +
					                                                            "How many percent have we <b>used</b> of the transaction log. near 100% = Full<br> " +
					                                                            "<b>Formula</b>: Pct = 100.0 - ((oval_LogSizeFreeInMb / oval_LogSizeInMb) * 100.0)<br>" +
					                                                        "</html>");
				}
				catch (NameNotFoundException e) {/*ignore*/}
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				List <String> pkCols = new LinkedList<String>();

				if (isClusterEnabled)
					pkCols.add("InstanceID");

				pkCols.add("DBName");

				return pkCols;
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				if (isClusterEnabled)
				{
					cols1 += "InstanceID, ";
				}

				String ceDbRecoveryStatus = ""; // 
				String QuiesceTag         = "";
				String SuspendedProcesses = "";
				if (aseVersion >= 12510)
				{
					QuiesceTag         = "QuiesceTag, ";
					SuspendedProcesses = "SuspendedProcesses, ";
				}
				if (isClusterEnabled)
				{
					ceDbRecoveryStatus = "CeDbRecoveryStatus = db_recovery_status(DBID), ";
				}

				// If we implement the FreeLogSize, then we need to take away databases that are in recovery etc...
				// Also calculate it into MB...
				// The calculation is stolen from: sp_helpdb dbname
				// declare	@pgsPerMb                int
				// select  @pgsPerMb           = 1024*1024 / @@maxpagesize
				// select @pgsPerMb   : 512=2K, 256=4K, 128=8K, 64=16K 
				// select mbUsed = pagesUsed / @pgsPerMb

				String DbSizeInMb      = "DbSizeInMb      = (select sum(u.size) from master..sysusages u readpast where u.dbid = mond.DBID)                          / (1024*1024/@@maxpagesize), \n";
				String LogSizeInMb     = "LogSizeInMb     = (select sum(u.size) from master..sysusages u readpast where u.dbid = mond.DBID and (segmap & 4) = 4)     / (1024*1024/@@maxpagesize), \n";
				String LogSizeFreeInMb = "LogSizeFreeInMb = convert(numeric(10,1), (lct_admin('logsegment_freepages',DBID)-lct_admin('reserved_for_rollbacks',DBID)) / (1024.0*1024.0/@@maxpagesize)), \n";
				String LogSizeUsedPct  = "LogSizeUsedPct  = convert(numeric(10,1), 0), /* calculated in AseTune */ \n";

				cols1 += "DBName, DBID, " + ceDbRecoveryStatus + "AppendLogRequests, AppendLogWaits, \n" +
				         "AppendLogContPct = CASE \n" +
				         "                      WHEN AppendLogRequests > 0 \n" +
				         "                      THEN convert(numeric(10,2), ((AppendLogWaits+0.0)/AppendLogRequests)*100.0) \n" +
				         "                      ELSE convert(numeric(10,2), 0.0) \n" +
				         "                   END, \n" +
				         DbSizeInMb + LogSizeInMb + LogSizeFreeInMb + LogSizeUsedPct + 
				         "TransactionLogFull, " + SuspendedProcesses + "BackupInProgress, LastBackupFailed, BackupStartTime, ";
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
				cols = StringUtil.removeLastComma(cols);

				String sql = 
					"select " + cols + "\n" +
					"from master..monOpenDatabases mond\n" +
					"where DBID in (select db.dbid from master..sysdatabases db readpast where (db.status & 32 != 32) and (db.status & 256 != 256)) \n" +
					"order by DBName \n";

				return sql;
			}

			/** 
			 * Compute the AppendLogContPct for DIFF values
			 */
			public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
			{
				int AppendLogRequests,         AppendLogWaits;
				int AppendLogRequestsId  = -1, AppendLogWaitsId     = -1;

//				int TransactionLogFull   = 0,  SuspendedProcesses   = 0;
//				int TransactionLogFullId = -1, SuspendedProcessesId = -1;

				double calcAppendLogContPct;
				int AppendLogContPctId = -1;

				int    oval_LogSizeInMb;
				double oval_LogSizeFreeInMb;
				double calc_LogSizeUsedPct;
				int pos_LogSizeInMb     = -1;
				int pos_LogSizeFreeInMb = -1;
				int pos_LogSizeUsedPct  = -1;

				// Find column Id's
				List<String> colNames = diffData.getColNames();
				if (colNames == null)
					return;

				for (int colId=0; colId < colNames.size(); colId++) 
				{
					String colName = colNames.get(colId);
					if      (colName.equals("AppendLogContPct"))   AppendLogContPctId   = colId;
					else if (colName.equals("AppendLogRequests"))  AppendLogRequestsId  = colId;
					else if (colName.equals("AppendLogWaits"))     AppendLogWaitsId     = colId;
					else if (colName.equals("LogSizeInMb"))        pos_LogSizeInMb      = colId;
					else if (colName.equals("LogSizeFreeInMb"))    pos_LogSizeFreeInMb  = colId;
					else if (colName.equals("LogSizeUsedPct"))     pos_LogSizeUsedPct   = colId;
//					else if (colName.equals("TransactionLogFull")) TransactionLogFullId = colId;
//					else if (colName.equals("SuspendedProcesses")) SuspendedProcessesId = colId;
				}

				// Loop on all diffData rows
				for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
				{
					AppendLogRequests    = ((Number)diffData.getValueAt(rowId, AppendLogRequestsId)).intValue();
					AppendLogWaits       = ((Number)diffData.getValueAt(rowId, AppendLogWaitsId   )).intValue();
					oval_LogSizeInMb     = ((Number)diffData.getValueAt(rowId, pos_LogSizeInMb    )).intValue();
					oval_LogSizeFreeInMb = ((Number)diffData.getValueAt(rowId, pos_LogSizeFreeInMb)).doubleValue();

					// COLUMN: AppendLogContPct
					if (AppendLogRequests > 0)
					{
						// Formula: AppendLogContPct = (AppendLogWaits / AppendLogRequests) * 100;
						calcAppendLogContPct = ((AppendLogWaits + 0.0) / AppendLogRequests) * 100.0;

						BigDecimal newVal = new BigDecimal(calcAppendLogContPct).setScale(2, BigDecimal.ROUND_HALF_EVEN);
						diffData.setValueAt(newVal, rowId, AppendLogContPctId);
					}
					else
						diffData.setValueAt(new BigDecimal(0).setScale(2, BigDecimal.ROUND_HALF_EVEN), rowId, AppendLogContPctId);

					// COLUMN: LogSizeUsedPct
					if (oval_LogSizeInMb > 0) // I doubt that oval_LogSizeInMb can be 0
					{
						// Formula: 
						calc_LogSizeUsedPct = 100.0 - (((oval_LogSizeFreeInMb + 0.0) / oval_LogSizeInMb) * 100.0);
						if (calc_LogSizeUsedPct < 0.0)
							calc_LogSizeUsedPct = 0.0;

						BigDecimal newVal = new BigDecimal(calc_LogSizeUsedPct).setScale(1, BigDecimal.ROUND_HALF_EVEN);
						diffData.setValueAt(newVal, rowId, pos_LogSizeUsedPct);
					}
					else
						diffData.setValueAt(new BigDecimal(0).setScale(1, BigDecimal.ROUND_HALF_EVEN), rowId, pos_LogSizeUsedPct);
				}
			}

			@Override
			public void updateGraphData(TrendGraphDataPoint tgdp)
			{
				// Get what databases should be part of the graphs
				Map<String, Integer> dbMap = DbSelectionForGraphsDialog.getDbsInGraphList(this);

				if (CM_GRAPH_NAME__OPEN_DATABASES__LOGSEMAPHORE_CONT.equals(tgdp.getName()))
				{
					// Write 1 "line" for every database
					Double[] dArray = new Double[dbMap.size()];
					String[] lArray = new String[dbMap.size()];
					int d = 0;
					for (int row : dbMap.values())
					{
						String dbname = this.getAbsString        (row, "DBName");
						Double dvalue = this.getDiffValueAsDouble(row, "AppendLogContPct");

						lArray[d] = dbname;
						dArray[d] = dvalue;
						d++;
					}

					// Set the values
					tgdp.setDate(this.getTimestamp());
					tgdp.setLabel(lArray);
					tgdp.setData(dArray);
				}
				if (CM_GRAPH_NAME__OPEN_DATABASES__LOGSIZE_LEFT.equals(tgdp.getName()))
				{
					// Write 1 "line" for every database
					Double[] dArray = new Double[dbMap.size()];
					String[] lArray = new String[dbMap.size()];
					int d = 0;
					for (int row : dbMap.values())
					{
						String dbname = this.getAbsString       (row, "DBName");
						Double dvalue = this.getAbsValueAsDouble(row, "LogSizeFreeInMb");

						lArray[d] = dbname;
						dArray[d] = dvalue;
						d++;
					}

					// Set the values
					tgdp.setDate(this.getTimestamp());
					tgdp.setLabel(lArray);
					tgdp.setData(dArray);
				}
				if (CM_GRAPH_NAME__OPEN_DATABASES__LOGSIZE_USED_PCT.equals(tgdp.getName()))
				{
					// Write 1 "line" for every database
					Double[] dArray = new Double[dbMap.size()];
					String[] lArray = new String[dbMap.size()];
					int d = 0;
					for (int row : dbMap.values())
					{
						String dbname = this.getAbsString        (row, "DBName");
						Double dvalue = this.getDiffValueAsDouble(row, "LogSizeUsedPct");

						lArray[d] = dbname;
						dArray[d] = dvalue;
						d++;
					}

					// Set the values
					tgdp.setDate(this.getTimestamp());
					tgdp.setLabel(lArray);
					tgdp.setData(dArray);
				}
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		tmp.addTrendGraphData(CM_GRAPH_NAME__OPEN_DATABASES__LOGSEMAPHORE_CONT, new TrendGraphDataPoint(CM_GRAPH_NAME__OPEN_DATABASES__LOGSEMAPHORE_CONT, new String[] { "runtime-replaced" }));
		tmp.addTrendGraphData(CM_GRAPH_NAME__OPEN_DATABASES__LOGSIZE_LEFT,      new TrendGraphDataPoint(CM_GRAPH_NAME__OPEN_DATABASES__LOGSIZE_LEFT,      new String[] { "runtime-replaced" }));
		tmp.addTrendGraphData(CM_GRAPH_NAME__OPEN_DATABASES__LOGSIZE_USED_PCT,  new TrendGraphDataPoint(CM_GRAPH_NAME__OPEN_DATABASES__LOGSIZE_USED_PCT,  new String[] { "runtime-replaced" }));
		if (AseTune.hasGUI())
		{
			final String finalDisplayName = displayName;
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName())
			{
				private static final long	serialVersionUID	= 1L;
				
				private CategoryDataset createDataset(JXTable dataTable)
				{
					Configuration conf = Configuration.getCombinedConfiguration();
					boolean generateDummy = conf.getBooleanProperty(CM_NAME__OPEN_DATABASES + ".generateDummyGraphWhenNotConnected", false);

					DefaultCategoryDataset categoryDataset = new DefaultCategoryDataset();

					if (dataTable != null)
					{
						AbstractTableModel tm = (AbstractTableModel)dataTable.getModel();
						int DBName_pos          = tm.findColumn("DBName");
						int LogSizeFreeInMb_pos = tm.findColumn("LogSizeFreeInMb"); // numeric(10,1)
						int LogSizeUsedPct_pos  = tm.findColumn("LogSizeUsedPct");  // numeric(10,1)

						CountersModel cm = getDisplayCm();
						if (cm == null)
							cm = getCm();

						if (cm != null)
						{
							Map<String, Integer> dbList = DbSelectionForGraphsDialog.getDbsInGraphList(cm);
							for(int r=0; r<dataTable.getRowCount(); r++)
							{
								String DBName          = (String)dataTable.getValueAt(r, DBName_pos);
								Number LogSizeFreeInMb = (Number)dataTable.getValueAt(r, LogSizeFreeInMb_pos);
								Number LogSizeUsedPct  = (Number)dataTable.getValueAt(r, LogSizeUsedPct_pos);
	
								if (dbList.keySet().contains(DBName))
									categoryDataset.addValue(LogSizeUsedPct, LogSizeFreeInMb+" MB FREE", DBName);
							}
						}
					}
					else
					{
						if (generateDummy)
						{
							for(int i=1; i<=30; i++)
							{
								double freePct = Math.random() * 100.0;
								double usedPct = 100.0 - freePct;
								BigDecimal freeMb = new BigDecimal(Math.random() * 1000.0).setScale(1, BigDecimal.ROUND_HALF_EVEN);
	
								categoryDataset.addValue(usedPct, freeMb+" MB FREE", "dummy_db_"+i);
							}
						}
					}

					return categoryDataset;
				}
				private JFreeChart createChart(CategoryDataset dataset)
				{
					// Get Graph Orientation: VERTICAL or HORIZONTAL
					Configuration conf = Configuration.getCombinedConfiguration();
					String orientationStr = conf.getProperty(CM_NAME__OPEN_DATABASES+".graph.PlotOrientation", "AUTO");

					PlotOrientation orientation = PlotOrientation.VERTICAL;
					if (orientationStr.equals("VERTICAL"))   orientation = PlotOrientation.VERTICAL;
					if (orientationStr.equals("HORIZONTAL")) orientation = PlotOrientation.HORIZONTAL;
					if (orientationStr.equals("AUTO"))
					{
						if (dataset.getRowCount() <= 10)
							orientation = PlotOrientation.HORIZONTAL;
					}

					JFreeChart chart = ChartFactory.createStackedBarChart(
							null,                     // Title
							null,                     // categoryAxisLabel
							null,                     // valueAxisLabel
							dataset,                  // dataset
							orientation,              // orientation
							false,                    // legend
							true,                     // tooltips
							false);                   // urls

					chart.setBackgroundPaint(getBackground());
					chart.setTitle(new TextTitle("Transaction Log Space Usage in Percent", TextTitle.DEFAULT_FONT));

					//------------------------------------------------
					// code from TabularCntrlPanel
					//------------------------------------------------
					CategoryPlot plot = chart.getCategoryPlot();
					StackedBarRenderer renderer = (StackedBarRenderer) plot.getRenderer();

					if (orientation.equals(PlotOrientation.VERTICAL))
					{
						ItemLabelPosition p1 = new ItemLabelPosition(ItemLabelAnchor.INSIDE12, TextAnchor.CENTER_RIGHT, TextAnchor.CENTER_RIGHT, -Math.PI / 2.0);
						renderer.setBasePositiveItemLabelPosition(p1);
						
						ItemLabelPosition p2 = new ItemLabelPosition(ItemLabelAnchor.OUTSIDE12, TextAnchor.CENTER_LEFT, TextAnchor.CENTER_LEFT, -Math.PI / 2.0);
						renderer.setPositiveItemLabelPositionFallback(p2);

						// Tilt DBName 45 degree left
						CategoryAxis domainAxis = plot.getDomainAxis();
						if (orientation.equals(PlotOrientation.VERTICAL))
							domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
					}
					else
					{
						ItemLabelPosition p1 = new ItemLabelPosition(ItemLabelAnchor.OUTSIDE3, TextAnchor.CENTER_RIGHT, TextAnchor.CENTER_RIGHT, 0);
						renderer.setBasePositiveItemLabelPosition(p1);
						
						ItemLabelPosition p2 = new ItemLabelPosition(ItemLabelAnchor.INSIDE3, TextAnchor.CENTER_LEFT, TextAnchor.CENTER_LEFT, 0);
						renderer.setPositiveItemLabelPositionFallback(p2);
					}

					
					StandardCategoryItemLabelGenerator scilg = new StandardCategoryItemLabelGenerator("{0}", NumberFormat.getInstance());
					renderer.setBaseItemLabelGenerator(scilg);
					
					renderer.setBaseItemLabelsVisible(true);

					NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
					rangeAxis.setUpperBound(100);

//					for (int r=0; r<dataset.getRowCount(); r++)
//						for (int c=0; c<dataset.getColumnCount(); c++)
//							System.out.println("dataset investigate: r="+r+", c="+c+", val="+dataset.getValue(r, c));

					for (int r=0; r<dataset.getRowCount(); r++)
					{
						Number val = dataset.getValue(r, r);
						if (val == null)
							continue;
						if (val.intValue() >= 90)
							renderer.setSeriesPaint(r, Color.RED);
						else if (val.intValue() >= 80)
							renderer.setSeriesPaint(r, Color.ORANGE);
						else
							renderer.setSeriesPaint(r, Color.GREEN);
					}

					return chart;
				}


				@Override
				protected JPanel createExtendedInfoPanel()
				{
					JSplitPane mainSplitPane = getMainSplitPane();
					JPanel panel = SwingUtils.createPanel("Extended Information", false);
					panel.setLayout(new BorderLayout());

					// Create Graph
					JFreeChart chart = createChart(createDataset(null));
					panel.add( new ChartPanel(chart) );

					// Size of the panel
					mainSplitPane.setDividerLocation(150);
					return panel;
				}

				@Override
				protected void updateExtendedInfoPanel()
				{
					JPanel  panel     = getExtendedInfoPanel();
					JXTable dataTable = getDataTable();
					if (panel     == null) return;

					// If the panel is so small, that is not visible, don't update the graph
					int dividerLocation = getMainSplitPane() == null ? 0 : getMainSplitPane().getDividerLocation();
					if (dividerLocation == 0)
						return;
					
//					if ( ! isMonConnected() )
//						dataTable = null;

					panel.removeAll();

					// Create Graph
					JFreeChart chart = createChart(createDataset(dataTable));
					panel.add( new ChartPanel(chart) );
				}

				@Override
				protected JPanel createLocalOptionsPanel()
				{
					JPanel panel = SwingUtils.createPanel("Local Options", true);
					panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

					final JButton resetMoveToTab_but = new JButton("Reset 'Move to Tab' settings");
					final JButton dblist_but         = new JButton("Set 'Graph' databases");

					String[] graphTypeArr = {"Auto", "Vertical", "Horizontal"};
					final JLabel    graphType_lbl    = new JLabel("Graph Orientation");
					final JComboBox graphType_cbx    = new JComboBox(graphTypeArr);

					String tooltip =
						"<html>" +
						"Reset the option: To automatically switch to this tab when any Database(s) Transaction log is <b>full</b>.<br>" +
						"Next time this happens, a popup will ask you what you want to do." +
						"</html>";
					resetMoveToTab_but.setToolTipText(tooltip);

					dblist_but   .setToolTipText("<html>What databases should be apart of the graphs, both in the Summary Panel, and this local graph area.<br></html>");
					tooltip = 
						"<html>" +
						"Do you want the 'Transaction Log Size Graph' to be presented 'Standing' or 'Laying'.<br>" +
						"'Auto' means: " +
						"<ul>" +
						"    <li>'Laying' if <b>less</b> than 10 databases is included.</li>" +
						"    <li>'Standing' if <b>more</b> than 10 databases is included.</li>" +
						"</ul>" +
						"</html>";
					graphType_lbl.setToolTipText(tooltip);
					graphType_cbx.setToolTipText(tooltip);

					// Set initial value for Graph Orientation
					Configuration conf = Configuration.getCombinedConfiguration();
					String orientationStr = conf.getProperty(CM_NAME__OPEN_DATABASES+".graph.PlotOrientation", "AUTO");
					String orientation = graphTypeArr[0]; // set as default
					if (orientationStr.equals("AUTO"))       orientation = graphTypeArr[0];
					if (orientationStr.equals("VERTICAL"))   orientation = graphTypeArr[1];
					if (orientationStr.equals("HORIZONTAL")) orientation = graphTypeArr[2];
					graphType_cbx.setSelectedItem(orientation);
					
					// ACTION events
					resetMoveToTab_but.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							ChangeToJTabDialog.resetSavedSettings(finalDisplayName);
						}
					});
					dblist_but.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							CountersModel cm = getDisplayCm();
							if (cm == null)
								cm = getCm();

							int rc = DbSelectionForGraphsDialog.showDialog(MainFrame.getInstance(), cm);
							if (rc == JOptionPane.OK_OPTION)
								updateExtendedInfoPanel();
						}
					});
					graphType_cbx.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
							
							String type = graphType_cbx.getSelectedItem().toString();
							if (type.equals("Auto"))       conf.setProperty(CM_NAME__OPEN_DATABASES+".graph.PlotOrientation", "AUTO");
							if (type.equals("Vertical"))   conf.setProperty(CM_NAME__OPEN_DATABASES+".graph.PlotOrientation", "VERTICAL");
							if (type.equals("Horizontal")) conf.setProperty(CM_NAME__OPEN_DATABASES+".graph.PlotOrientation", "HORIZONTAL");
							conf.save();
							
							updateExtendedInfoPanel();
						}
					});

					// ADD to panel
					panel.add(resetMoveToTab_but, "wrap 5");
					panel.add(dblist_but,         "wrap 5");
					panel.add(graphType_lbl,      "split");
					panel.add(graphType_cbx,      "wrap 5");

					return panel;
				}
			};
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_db_activity.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );

			// GRAPH
			TrendGraph tg = null;
			tg = new TrendGraph(CM_GRAPH_NAME__OPEN_DATABASES__LOGSEMAPHORE_CONT,
					"DB Transaction Log Semaphore Contention", // Menu Checkbox text
					"DB Transaction Log Semaphore Contention in Percent", // Label on the graph
					new String[] { "runtime-replaced" }, 
					false, tmp, false, -1);
			tmp.addTrendGraph(tg.getName(), tg, true);

			tg = new TrendGraph(CM_GRAPH_NAME__OPEN_DATABASES__LOGSIZE_LEFT,
					"DB Transaction Log Space left in MB", // Menu Checkbox text
					"DB Transaction Log Space left to use in MB", // Label on the graph
					new String[] { "runtime-replaced" }, 
					false, tmp, false, -1);
			tmp.addTrendGraph(tg.getName(), tg, true);
			
			tg = new TrendGraph(CM_GRAPH_NAME__OPEN_DATABASES__LOGSIZE_USED_PCT,
					"DB Transaction Log Space used in PCT", // Menu Checkbox text
					"DB Transaction Log Space used in Percent", // Label on the graph
					new String[] { "runtime-replaced" }, 
					true, tmp, false, -1);
			tmp.addTrendGraph(tg.getName(), tg, true);
			
			// BLUE = in 'DUMP DATABASE'
			if (conf != null) colorStr = conf.getProperty(name+".color.dumpdb");
			tcp.addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					Number backupInProg = (Number) adapter.getValue(adapter.getColumnIndex("BackupInProgress"));
					return backupInProg != null && backupInProg.intValue() > 0;
				}
			}, SwingUtils.parseColor(colorStr, Color.BLUE), null));

			// PINK = TRANSACTION LOG at 90%
			if (conf != null) colorStr = conf.getProperty(name+".color.almostFullTranslog");
			tcp.addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					Number almostLogFull = (Number) adapter.getValue(adapter.getColumnIndex("LogSizeUsedPct"));
					return almostLogFull != null && almostLogFull.doubleValue() > 90.0;
				}
			}, SwingUtils.parseColor(colorStr, Color.PINK), null));

			// RED = FULL TRANSACTION LOG
			if (conf != null) colorStr = conf.getProperty(name+".color.fullTranslog");
			tcp.addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					Number isLogFull = (Number) adapter.getValue(adapter.getColumnIndex("TransactionLogFull"));
					return isLogFull != null && isLogFull.intValue() > 0;
				}
			}, SwingUtils.parseColor(colorStr, Color.RED), null));
		}

		_CMList.add(tmp);



		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// TEMP Databases Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		
		// FIXME: fix description
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

		name         = CM_NAME__TEMPDB_ACTIVITY;
		displayName  = CM_DESC__TEMPDB_ACTIVITY;
		description  = "Provides statistics for all local temporary databases.";
		
		needVersion  = 15500;
		needCeVersion= 15020;
//		needVersion  = 15020;
//		needCeVersion= 0;
		monTables    = new String[] {"monTempdbActivity"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring=1", "object lockwait timing=1", "per object statistics active=1"};
		colsCalcDiff = new String[] {"AppendLogRequests", "AppendLogWaits", "LogicalReads", "PhysicalReads", "APFReads", "PagesRead", "PhysicalWrites", "PagesWritten", "LockRequests", "LockWaits", "CatLockRequests", "CatLockWaits", "AssignedCnt", "SharableTabCnt"};
		colsCalcPCT  = new String[] {"AppendLogContPct"};
		pkList       = new LinkedList<String>();
	     pkList.add("DBID");


		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				true, true)
		{
			private static final long serialVersionUID = 5078336367667465709L;

			@Override
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return new String[] {"enable monitoring=1", "object lockwait timing=1", "per object statistics active=1"};
			}

			@Override
			public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				try 
				{
					MonTablesDictionary mtd = MonTablesDictionary.getInstance();
					mtd.addColumn("monTempdbActivity", "AppendLogContPct",  "<html>" +
					                                                             "Log Semaphore Contention in percent.<br> " +
					                                                             "<b>Formula</b>: Pct = (AppendLogWaits / AppendLogRequests) * 100<br>" +
					                                                        "</html>");
				}
				catch (NameNotFoundException e) {/*ignore*/}
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				List <String> pkCols = new LinkedList<String>();

				if (isClusterEnabled)
					pkCols.add("InstanceID");

				pkCols.add("DBID");

				return pkCols;
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				if (isClusterEnabled)
				{
					cols1 += "InstanceID, ";
				}

				cols1 += "DBID, DBName, \n" +
				         "SharableTabCnt, \n" +
				         "AppendLogRequests, AppendLogWaits, \n" +
				         "AppendLogContPct = CASE \n" +
				         "                       WHEN AppendLogRequests > 0 \n" +
				         "                       THEN convert(numeric(10,2), ((AppendLogWaits+0.0)/AppendLogRequests)*100.0) \n" +
				         "                       ELSE convert(numeric(10,2), 0.0) \n" +
				         "                   END, \n" +
				         "LogicalReads, PhysicalReads, APFReads, \n" +
				         "PagesRead, PhysicalWrites, PagesWritten, \n" +
				         "LockRequests, LockWaits, \n" +
				         "CatLockRequests, CatLockWaits, AssignedCnt ";
				cols2 += "";
				cols3 += "";

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from master..monTempdbActivity \n" +
					"order by DBName \n";

				return sql;
			}

			/** 
			 * Compute the AppendLogContPct for DIFF values
			 */
			public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
			{
				int AppendLogRequests,        AppendLogWaits;
				int AppendLogRequestsId = -1, AppendLogWaitsId = -1;

				double calcAppendLogContPct;
				int AppendLogContPctId = -1;

				// Find column Id's
				List<String> colNames = diffData.getColNames();
				if (colNames == null)
					return;

				for (int colId=0; colId < colNames.size(); colId++) 
				{
					String colName = colNames.get(colId);
					if      (colName.equals("AppendLogContPct"))  AppendLogContPctId  = colId;
					else if (colName.equals("AppendLogRequests")) AppendLogRequestsId = colId;
					else if (colName.equals("AppendLogWaits"))    AppendLogWaitsId    = colId;
				}

				// Loop on all DIFF DATA rows
				for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
				{
					AppendLogRequests = ((Number)diffData.getValueAt(rowId, AppendLogRequestsId)).intValue();
					AppendLogWaits    = ((Number)diffData.getValueAt(rowId, AppendLogWaitsId   )).intValue();

					// int totIo = Reads + APFReads + Writes;
					if (AppendLogRequests > 0)
					{
						// WaitTimePerWait = WaitTime / Waits;
						calcAppendLogContPct = ((AppendLogWaits + 0.0) / AppendLogRequests) * 100.0;

						BigDecimal newVal = new BigDecimal(calcAppendLogContPct).setScale(2, BigDecimal.ROUND_HALF_EVEN);
						diffData.setValueAt(newVal, rowId, AppendLogContPctId);
					}
					else
						diffData.setValueAt(new BigDecimal(0).setScale(2, BigDecimal.ROUND_HALF_EVEN), rowId, AppendLogContPctId);
				}
			}

			@Override
			public void updateGraphData(TrendGraphDataPoint tgdp)
			{
				if (CM_GRAPH_NAME__TEMPDB_ACTIVITY__LOGSEMAPHORE_CONT.equals(tgdp.getName()))
				{
					// Write 1 "line" for every database
					Double[] dArray = new Double[this.size()];
					String[] lArray = new String[dArray.length];
					for (int i = 0; i < dArray.length; i++)
					{
						lArray[i] = this.getAbsString        (i, "DBName");
						dArray[i] = this.getDiffValueAsDouble(i, "AppendLogContPct");
					}

					// Set the values
					tgdp.setDate(this.getTimestamp());
					tgdp.setLabel(lArray);
					tgdp.setData(dArray);
				}
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		tmp.addTrendGraphData(CM_GRAPH_NAME__TEMPDB_ACTIVITY__LOGSEMAPHORE_CONT, new TrendGraphDataPoint(CM_GRAPH_NAME__TEMPDB_ACTIVITY__LOGSEMAPHORE_CONT, new String[] { "runtime-replaced" }));
		if (AseTune.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_db_activity.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );

			// GRAPH
			TrendGraph tg = new TrendGraph(CM_GRAPH_NAME__TEMPDB_ACTIVITY__LOGSEMAPHORE_CONT,
					"TempDB Transaction Log Semaphore Contention", // Menu Checkbox text
					"TempDB Transaction Log Semaphore Contention in Percent", // Label on the graph
					new String[] { "runtime-replaced" }, 
					false, tmp, false, -1);
			tmp.addTrendGraph(tg.getName(), tg, true);
		}

		_CMList.add(tmp);




		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// Waits Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------

		//==================================================================================================
		// 12.5.0.3: Description of: monSysWaits
		// 
		// Description:
		//
		//
		// Needs Configuration:
		// none
		// 
		// Name                           Datatype     Attributes  Ind Description
		// ------------------------------ ------------ ----------- --- ------------------------------------------------------------
		// WaitEventID                    smallint                   0 Unique identifier for the wait event
		// WaitTime                       int                        1 Amount of time (in seconds) that tasks have spent waiting for the event
		// Waits                          int                        0 Number of times tasks have waited for the event
		//---------------------------------------------------------------------------------------------------
		// Column changes in various versions:
		//
		// Version      Action Name              Datatype   Attributes Description
		// ------------ ------ ----------------- ---------- ---------- ----------------------------------
		// 15.0.1CE/15.5 add   InstanceId        int                   The Server Instance Identifier (cluster only)
		//---------------------------------------------------------------------------------------------------

		name         = CM_NAME__SYS_WAIT;
		displayName  = CM_DESC__SYS_WAIT;
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
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return new String[] {"enable monitoring=1", "wait event timing=1"};

			}

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
			public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				try 
				{
					MonTablesDictionary mtd = MonTablesDictionary.getInstance();
					mtd.addColumn("monSysWaits", "WaitTimePerWait", "<html>" +
					                                                   "Wait time in seconds per wait. formula: diff.WaitTime / diff.Waits<br>" +
					                                                   "Since WaitTime here is in seconds, this value will also be in seconds." +
					                                                "</html>");
				}
				catch (NameNotFoundException e) {/*ignore*/}
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				List <String> pkCols = new LinkedList<String>();

				if (isClusterEnabled)
					pkCols.add("InstanceID");

				pkCols.add("WaitEventID");

				return pkCols;
			}

			@Override
			public String getSqlInitForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String sql =
					"/*------ Create permanent tables for monWaitEventInfo & monWaitClassInfo in tempdb. -------*/ \n" +
					"/*------ hopefully this is less expensive than doing the join via CIS -------*/ \n" +
					"if ((select object_id('tempdb.guest.monWaitEventInfo')) is null) \n" +
					"   exec('select * into tempdb.guest.monWaitEventInfo from master..monWaitEventInfo') \n" +
					"\n" +
					"if ((select object_id('tempdb.guest.monWaitClassInfo')) is null) \n" +
					"   exec('select * into tempdb.guest.monWaitClassInfo from master..monWaitClassInfo') \n" +
					"\n";

				return sql;
			}
			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";


				if (isClusterEnabled)
				{
					cols1 += "InstanceID, ";
				}

				cols1 += "Class=C.Description, Event=I.Description, W.WaitEventID, WaitTime, Waits \n";
				if (aseVersion >= 15010 || (aseVersion >= 12540 && aseVersion <= 15000) )
				{
				}
				cols2 += ", WaitTimePerWait = CASE WHEN Waits > 0 \n" +
				         "                         THEN convert(numeric(10,3), (WaitTime + 0.0) / Waits) \n" +
				         "                         ELSE convert(numeric(10,3), 0.0) \n" +
				         "                     END \n";

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from master..monSysWaits W, tempdb.guest.monWaitEventInfo I, tempdb.guest.monWaitClassInfo C \n" +
					"where W.WaitEventID=I.WaitEventID and I.WaitClassID=C.WaitClassID \n" +
					"order by " + (isClusterEnabled ? "W.WaitEventID, InstanceID" : "W.WaitEventID") + "\n";

				return sql;
			}

			/** 
			 * Compute the WaitTimePerWait for diff values
			 */
			public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
			{
				int WaitTime,        Waits;
				int WaitTimeId = -1, WaitsId = -1;

				double calcWaitTimePerWait;
				int WaitTimePerWaitId = -1;

				// Find column Id's
				List<String> colNames = diffData.getColNames();
				if (colNames == null)
					return;

				for (int colId=0; colId < colNames.size(); colId++) 
				{
					String colName = colNames.get(colId);
					if      (colName.equals("WaitTimePerWait")) WaitTimePerWaitId = colId;
					else if (colName.equals("WaitTime"))        WaitTimeId        = colId;
					else if (colName.equals("Waits"))           WaitsId           = colId;
				}

				// Loop on all diffData rows
				for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
				{
					WaitTime = ((Number)diffData.getValueAt(rowId, WaitTimeId)).intValue();
					Waits    = ((Number)diffData.getValueAt(rowId, WaitsId   )).intValue();

					// int totIo = Reads + APFReads + Writes;
					if (Waits > 0)
					{
						// WaitTimePerWait = WaitTime / Waits;
						calcWaitTimePerWait = WaitTime / (Waits * 1.0);

						BigDecimal newVal = new BigDecimal(calcWaitTimePerWait).setScale(3, BigDecimal.ROUND_HALF_EVEN);;
						diffData.setValueAt(newVal, rowId, WaitTimePerWaitId);
					}
					else
						diffData.setValueAt(new BigDecimal(0), rowId, WaitTimePerWaitId);
				}
			}
		};
		
		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (AseTune.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_wait_activity.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );
		}
		
		_CMList.add(tmp);





		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// Engines Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------

		//==================================================================================================
		// 12.5.0.3: Description of: monEngine
		// 
		// Description:
		//
		//
		// Needs Configuration:
		// none
		// 
		// Name                           Datatype     Attributes  Ind Description
		// ------------------------------ ------------ ----------- --- ------------------------------------------------------------
		// EngineNumber                   smallint                   0 Number of the ASE engine
		// StartTime                      datetime                   0 The date that the engine came online
		// StopTime                       datetime                   0 The date that the engine went offline
		// CurrentKPID                    int                        0 Kernel Process Identifier for the currently executing process
		// PreviousKPID                   int                        0 Kernel Process Identifier for the previously executed process
		// CPUTime                        int                        3 Total time (in seconds) the engine has been running
		// SystemCPUTime                  int                        3 Time (in seconds) the engine has been executing database system services
		// UserCPUTime                    int                        3 Time (in seconds) the engine has been executing user commands
		// IdleCPUTime                    int                        3 Time (in seconds) the engine has been in idle spin mode
		// ContextSwitches                int                        3 Number of context switches
		// Connections                    int                        1 Number of connections handled
		// ProcessesAffinitied            int                        0 Number of processes that have been affinitied to this Engine
		// Status                         varchar(20)                0 Status of the engine, e.g. online, offline, e.t.c.
		// AffinitiedToCPU                intn                       0 The number of the CPU that the engine is affinitied to
		// OSPID                          intn                       0 Identifier for the Operating System process executing the engine
		//---------------------------------------------------------------------------------------------------
		// Column changes in various versions:
		//
		// Version      Action Name              Datatype   Attributes Description
		// ------------ ------ ----------------- ---------- ---------- ----------------------------------
		// 12.5.3 esd#2 add    Yields            int                   Number of times this engine yielded to the operating system. The rate of yielding during idle periods can be modified using the 'runnable process search count' configuration option
		// 12.5.3 esd#2 add    DiskIOChecks      int                   Number of times the engine checked for asynchronous disk I/O. The frequency of these checks can be modified using the 'i/o polling process count' configuration option
		// 12.5.3 esd#2 add    DiskIOPolled      int                   Number of times the engine polled for completion of outstanding asynchronous disk I/O. This occurs whenever disk I/O checks indicate that asynchronous I/O has been posted and is not yet completed
		// 12.5.3 esd#2 add    DiskIOCompleted   int                   Number of asynchronous disk I/Os that were completed when the engine polled for outstanding asynchronous disk I/O
		// 15.0         add    HkgcMaxQSize      int                   Maximum number of items that can be queued for housekeeper garbage collection in this engine
		// 15.0         add    HkgcPendingItems  int                   Number of items yet to be garbage collected by housekeeper garbage collector on this engine
		// 15.0         add    HkgcHWMItems      int                   Maximum number of pending items queued for housekeeper garbage collector at any instant of time since server started
		// 15.0         add    HkgcOverflows     int                   Number of items that could not be queued to housekeeper garbage collector due to queue overflows
		// 15.0.1CE/15.5 add   InstanceId        int                   The Server Instance Identifier (cluster only)
		// 15.0.2 esd#5 add    MaxOutstandingIOs int                   The maximum number of I/Os pending for each Adaptive Server engine
		// 15.5         add    IOCPUTime         int                   This contains the time (in seconds) the engine has been waiting for the issued IOs to finish. 
		//---------------------------------------------------------------------------------------------------
		
		name         = CM_NAME__ENGINE;
		displayName  = CM_DESC__ENGINE;
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
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return new String[] {"enable monitoring=1"};

			}

			@Override
			public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				List <String> pkCols = new LinkedList<String>();

				if (isClusterEnabled)
					pkCols.add("InstanceID");

				pkCols.add("EngineNumber");

				return pkCols;
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				if (isClusterEnabled)
				{
					cols1 += "InstanceID, ";
				}

				cols1 += "EngineNumber, CurrentKPID, PreviousKPID, CPUTime, SystemCPUTime, UserCPUTime, \n";
				if (aseVersion >= 15500 || (aseVersion >= 15030 && isClusterEnabled) )
					cols1 += "IOCPUTime, ";
				cols1 += "IdleCPUTime, ContextSwitches, Connections, \n";

				cols2 += "";
				cols3 += "ProcessesAffinitied, Status, StartTime, StopTime, AffinitiedToCPU, OSPID";

				if (aseVersion >= 12532)
				{
					cols2 += "Yields, DiskIOChecks, DiskIOPolled, DiskIOCompleted, \n";
				}
				if (aseVersion >= 15025)
				{
					cols2 += "MaxOutstandingIOs, ";
				}
				if (aseVersion >= 15000)
				{
					cols2 += "HkgcMaxQSize, HkgcPendingItems, HkgcHWMItems, HkgcOverflows, \n";
				}

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from master..monEngine \n" +
					"where Status = 'online' \n" +
					"order by 1,2\n";

				return sql;
			}

			@Override
			public void updateGraphData(TrendGraphDataPoint tgdp)
			{
				int aseVersion = getServerVersion();
				// NOTE: IOCPUTime was introduced in ASE 15.5

				if (CM_GRAPH_NAME__ENGINE__CPU_SUM.equals(tgdp.getName()))
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
				if (CM_GRAPH_NAME__ENGINE__CPU_ENG.equals(tgdp.getName()))
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
				int CPUTime,        SystemCPUTime,        UserCPUTime,        IOCPUTime,        IdleCPUTime;
				int CPUTimeId = -1, SystemCPUTimeId = -1, UserCPUTimeId = -1, IOCPUTimeId = -1, IdleCPUTimeId = -1;
			
				// Find column Id's
				List<String> colNames = diffData.getColNames();
				if (colNames==null) return;
			
				for (int colId=0; colId < colNames.size(); colId++) 
				{
					String colName = colNames.get(colId);
					if      (colName.equals("CPUTime"))       CPUTimeId       = colId;
					else if (colName.equals("SystemCPUTime")) SystemCPUTimeId = colId;
					else if (colName.equals("UserCPUTime"))   UserCPUTimeId   = colId;
					else if (colName.equals("IOCPUTime"))     IOCPUTimeId     = colId;
					else if (colName.equals("IdleCPUTime"))   IdleCPUTimeId   = colId;
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
		tmp.addTrendGraphData(CM_GRAPH_NAME__ENGINE__CPU_SUM, new TrendGraphDataPoint(CM_GRAPH_NAME__ENGINE__CPU_SUM, new String[] { "System+User CPU", "System CPU", "User CPU" }));
		tmp.addTrendGraphData(CM_GRAPH_NAME__ENGINE__CPU_ENG, new TrendGraphDataPoint(CM_GRAPH_NAME__ENGINE__CPU_ENG, new String[] { "eng-0" }));
		if (AseTune.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_engine_activity.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );

			// GRAPH
			TrendGraph tg = null;
			tg = new TrendGraph(CM_GRAPH_NAME__ENGINE__CPU_SUM,
					"CPU Summary", 	// Menu Checkbox text
					"CPU Summary for all Engines (using monEngine)", // Label 
					new String[] { "System+User CPU", "System CPU", "User CPU" }, 
					true, tmp, true, -1);
			tmp.addTrendGraph(tg.getName(), tg, true);

			tg = new TrendGraph(CM_GRAPH_NAME__ENGINE__CPU_ENG,
					"CPU per Engine", // Menu Checkbox text
					"CPU Usage per Engine (System + User)", // Label 
					new String[] { "eng-0" }, 
					true, tmp, true, -1);
			tmp.addTrendGraph(tg.getName(), tg, true);
		}
		
		_CMList.add(tmp);





		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// System LOAD
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		
		// FIXME: table description
		
		name         = CM_NAME__SYS_LOAD;
		displayName  = CM_DESC__SYS_LOAD;
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
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return null;
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				List <String> pkCols = new LinkedList<String>();

				if (isClusterEnabled)
					pkCols.add("InstanceID");

				pkCols.add("StatisticID");
				pkCols.add("EngineNumber");

				return pkCols;
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String cols1 = "";
				if (isClusterEnabled)
				{
					cols1 += "InstanceID, ";
				}

				String sql = 
					"select "+cols1+"StatisticID, Statistic, EngineNumber, \n" +
					"       Sample, \n" +
					"       Avg_1min, Avg_5min, Avg_15min, " +
					"       SteadyState, \n" +
					"       Peak_Time, Peak, \n" +
					"       Max_1min_Time,  Max_1min, \n" + // try add a SQL that shows number of minutes ago this happened, "Days-HH:MM:SS
					"       Max_5min_Time,  Max_5min, \n" +
					"       Max_15min_Time, Max_15min \n" +
					"from master..monSysLoad \n";
				
				// in ASE 15.7, we get problems if we do the order by
				// com.sybase.jdbc3.jdbc.SybSQLException: Domain error occurred.
				if (aseVersion < 15700)
					sql += "order by StatisticID, EngineNumber" + (isClusterEnabled ? ", InstanceID" : "");

				return sql;
			}

			@Override
			public void updateGraphData(TrendGraphDataPoint tgdp)
			{
				int aseVersion = getServerVersion();

				if (CM_GRAPH_NAME__SYS_LOAD__AVG_RUN_QUEUE_LENTH.equals(tgdp.getName()))
				{
					if (aseVersion < 15500)
					{
						// disable the transactions graph checkbox...
						TrendGraph tg = getTrendGraph(CM_GRAPH_NAME__SYS_LOAD__AVG_RUN_QUEUE_LENTH);
						if (tg != null)
						{
							JCheckBoxMenuItem menuItem = tg.getViewMenuItem();
							if (menuItem.isSelected())
								menuItem.doClick();
						}
					}
					else
					{
						int[] rqRows = this.getAbsRowIdsWhere("Statistic", "run queue length");
						Double[] arr = new Double[5];
						arr[0] = this.getAbsValueAvg(rqRows, "Sample");
						arr[1] = this.getAbsValueAvg(rqRows, "Avg_1min");
						arr[2] = this.getAbsValueAvg(rqRows, "Avg_5min");
						arr[3] = this.getAbsValueAvg(rqRows, "Max_1min");
						arr[4] = this.getAbsValueAvg(rqRows, "Max_5min");
						_logger.debug("updateGraphData(AvgRunQLengthGraph): Sample='"+arr[0]+"', Avg_1min='"+arr[1]+"', Avg_5min='"+arr[2]+"', Max_1min='"+arr[3]+"', Max_5min='"+arr[4]+"'.");

						// Set the values
						tgdp.setDate(this.getTimestamp());
						tgdp.setData(arr);
					}
				}

				if (CM_GRAPH_NAME__SYS_LOAD__ENGINE_RUN_QUEUE_LENTH.equals(tgdp.getName()))
				{
					if (aseVersion < 15500)
					{
						// disable the transactions graph checkbox...
						TrendGraph tg = getTrendGraph(CM_GRAPH_NAME__SYS_LOAD__ENGINE_RUN_QUEUE_LENTH);
						if (tg != null)
						{
							JCheckBoxMenuItem menuItem = tg.getViewMenuItem();
							if (menuItem.isSelected())
								menuItem.doClick();
						}
					}
					else
					{
						// Get a array of rowId's where the column 'Statistic' has the value 'run queue length'
						int[] rqRows = this.getAbsRowIdsWhere("Statistic", "run queue length");
						Double[] data  = new Double[rqRows.length];
						String[] label = new String[rqRows.length];
						for (int i=0; i<rqRows.length; i++)
						{
							int rowId = rqRows[i];

							// get LABEL
							String instanceId   = null;
							if (isClusterEnabled())
								instanceId = this.getAbsString(rowId, "InstanceID");
							String engineNumber = this.getAbsString(rowId, "EngineNumber");

							// in Cluster Edition the labels will look like 'eng-{InstanceId}:{EngineNumber}'
							// in ASE SMP Version the labels will look like 'eng-{EngineNumber}'
							if (instanceId == null)
								label[i] = "eng-" + engineNumber;
							else
								label[i] = "eng-" + instanceId + ":" + engineNumber;

							// get DATA
							data[i]  = this.getAbsValueAsDouble(rowId, "Avg_1min");
						}
						if (_logger.isDebugEnabled())
						{
							String debugStr = "";
							for (int i=0; i<data.length; i++)
								debugStr += label[i] + "='"+data[i]+"', ";
							_logger.debug("updateGraphData(EngineRunQLengthGraph): "+debugStr);
						}

						// Set the values
						tgdp.setDate(this.getTimestamp());
						tgdp.setLabel(label);
						tgdp.setData(data);
					}
				}
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		tmp.addTrendGraphData(CM_GRAPH_NAME__SYS_LOAD__AVG_RUN_QUEUE_LENTH,    new TrendGraphDataPoint(CM_GRAPH_NAME__SYS_LOAD__AVG_RUN_QUEUE_LENTH,    new String[] { "Now", "Avg last 1 minute", "Avg last 5 minute", "Max last 1 minute", "Max last 5 minute" }));
		tmp.addTrendGraphData(CM_GRAPH_NAME__SYS_LOAD__ENGINE_RUN_QUEUE_LENTH, new TrendGraphDataPoint(CM_GRAPH_NAME__SYS_LOAD__ENGINE_RUN_QUEUE_LENTH, new String[] { "-runtime-replaced-" }));
		if (AseTune.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_sysload_activity.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );

			// GRAPH
			TrendGraph tg = new TrendGraph(CM_GRAPH_NAME__SYS_LOAD__AVG_RUN_QUEUE_LENTH,
					"Run Queue Length, Server Wide", // Menu Checkbox text
					"Run Queue Length, Average for all instances (only in 15.5 and later)", // Label on the graph
					new String[] { "Now", "Avg last 1 minute", "Avg last 5 minute", "Max last 1 minute", "Max last 5 minute" }, 
					false, tmp, false, -1);
			tmp.addTrendGraph(tg.getName(), tg, true);

			// GRAPH
			tg = new TrendGraph(CM_GRAPH_NAME__SYS_LOAD__ENGINE_RUN_QUEUE_LENTH,
					"Run Queue Length, Per Engine", // Menu Checkbox text
					"Run Queue Length, Average over last minute, Per Engine (only in 15.5 and later)", // Label on the graph
					new String[] { "-runtime-replaced-" }, 
					false, tmp, false, -1);
			tmp.addTrendGraph(tg.getName(), tg, true);
		}
		
		_CMList.add(tmp);



		
		
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// DataCaches Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------

		//==================================================================================================
		// 12.5.0.3: Description of: monDataCache
		// 
		// Description:
		//
		//
		// Needs Configuration:
		// none
		// 
		// Name                           Datatype     Attributes  Ind Description
		// ------------------------------ ------------ ----------- --- ------------------------------------------------------------
		// CacheID                        int                        0 Unique identifier for the cache
		// RelaxedReplacement             int                        0 Whether the cache is using Relaxed cached replacement strategy
		// BufferPools                    int                        0 The number of buffer pools within the cache
		// CacheSearches                  int                        1 Cache searches directed to the cache
		// PhysicalReads                  int                        1 Number of buffers read into the cache from disk
		// LogicalReads                   int                        1 Number of buffers retrieved from the cache
		// PhysicalWrites                 int                        1 Number of buffers written from the cache to disk
		// Stalls                         int                        1 Number of 'dirty' buffer retrievals
		// CachePartitions                smallint                   0 Number of partitions currently configured for the cache
		// CacheName                      varchar(30)                0 Name of the cache
		//---------------------------------------------------------------------------------------------------
		// Column changes in various versions:
		//
		// Version      Action Name              Datatype   Attributes Description
		// ------------ ------ ----------------- ---------- ---------- ----------------------------------
		// 15.0.1CE/15.5 add   InstanceId          int                 The Server Instance Identifier (cluster only)
		// 15.7 (3B)    add    Status              varchar(30)    null Status of cache. One of:* Active, * Pending/Active, * Pending/Delete, * Update Cache, * Cache Create, * Cache Delete, * Cache Skip (Cluster Edition only)
		// 15.7 (3B)    add    Type                varchar(30)    null Type of cache. One of: * Default, * Mixed, * Mixed, HK Ignore, * Log Only, * In-Memory Storage
		// 15.7 (3B)    add    CacheSize           int                 Total size of cache, in kilobytes
		// 15.7 (3B)    add    ReplacementStrategy varchar(30)    null Cache replacement strategy
		// 15.7 (3B)    add    APFReads            int                 Counter Number of asynchronous prefetch (APF) reads for this data cache
		// 15.7 (3B)    add    Overhead            int                 Cache overhead
		//---------------------------------------------------------------------------------------------------

		name         = CM_NAME__DATA_CACHE;
		displayName  = CM_DESC__DATA_CACHE;
		description  = "What (user defined) data caches have we got and how many 'chache misses' goes out to disk...";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 0;
		needCeVersion= 0;
		monTables    = new String[] {"monDataCache"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring=1"};
		colsCalcDiff = new String[] {"CacheHitRate", "CacheSearches", "PhysicalReads", "LogicalReads", "PhysicalWrites", "Stalls", "APFReads"};
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
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return new String[] {"enable monitoring=1"};
			}

			@Override
			public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				try 
				{
					MonTablesDictionary mtd = MonTablesDictionary.getInstance();
					mtd.addColumn("monDataCache",  "Stalls",       "Number of times I/O operations were delayed because no clean buffers were available in the wash area");

					mtd.addColumn("monDataCache",  "CacheHitRate", "<html>" +
					                                               "Percent calculation of how many pages was fetched from the cache.<br>" +
					                                               "<b>Note</b>: APF reads could already be in memory, counted as a 'cache hit', check also 'devices' and APFReads.<br>" +
					                                               "<b>Formula</b>: 100 - (PhysicalReads/CacheSearches) * 100.0" +
					                                               "</html>");
//					mtd.addColumn("monDataCache",  "Misses",       "fixme");
//					mtd.addColumn("monDataCache",  "Volatility",   "fixme");
				}
				catch (NameNotFoundException e) {/*ignore*/}
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				List <String> pkCols = new LinkedList<String>();

				if (isClusterEnabled)
					pkCols.add("InstanceID");

				pkCols.add("CacheName");

				return pkCols;
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				//------------- NEEDS TO BE CALCULATED AFTER EACH SAMPLE
				// HitRate    = CacheSearches  / Logical Reads
				// Misses     = CacheSearches  / Physical Reads
				// Volatility = PhysicalWrites / (PhysicalReads + LogicalReads)
				
				// ASE 15.7
				String Status              = "";
				String Type                = "";
				String CacheSize           = "";
				String ReplacementStrategy = "";
				String APFReads            = "";
				String Overhead            = "";

				if (aseVersion >= 15700)
				{
					Status              = "Status, ";
					Type                = "Type, ";
					CacheSize           = "CacheSize, ";
					ReplacementStrategy = "ReplacementStrategy, ";
					APFReads            = "APFReads, ";
					Overhead            = "Overhead, ";
				}

				if (isClusterEnabled)
				{
					cols1 += "InstanceID, ";
				}

				cols1 += "CacheName, CacheID, " +
				         Status + Type + CacheSize + ReplacementStrategy + "\n" +
				         "RelaxedReplacement, CachePartitions, BufferPools, \n" +
				         "CacheSearches, PhysicalReads, LogicalReads, PhysicalWrites, Stalls, \n" +
				         APFReads + Overhead +
				         "CacheHitRate = convert(numeric(10,1), 100 - (PhysicalReads*1.0/(CacheSearches+1)) * 100.0)" +
//				         ", HitRate    = convert(numeric(10,1), (CacheSearches * 1.0 / LogicalReads) * 100)" +
//				         ", Misses     = convert(numeric(10,1), (CacheSearches * 1.0 / PhysicalReads) * 1)" +
//				         ", Volatility = convert(numeric(10,1), PhysicalWrites * 1.0 / (PhysicalReads + LogicalReads)* 1)"
				         "";

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from master..monDataCache \n" +
					"order by 1,2\n";

				return sql;
			}

			@Override
			public void updateGraphData(TrendGraphDataPoint tgdp)
			{
				if (CM_GRAPH_NAME__DATA_CACHE__ACTIVITY.equals(tgdp.getName()))
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
//				int CacheSearches,        LogicalReads,        PhysicalReads,        PhysicalWrites;
//				int CacheSearchesId = -1, LogicalReadsId = -1, PhysicalReadsId = -1, PhysicalWritesId = -1;
//	
//				int CacheHitRateId = -1, MissesId = -1, VolatilityId = -1;
			
				int CacheSearches,        PhysicalReads;
				int CacheSearchesId = -1, PhysicalReadsId = -1;
	
				int CacheHitRateId = -1;

				// Find column Id's
				List<String> colNames = diffData.getColNames();
				if (colNames==null) return;
			
				for (int colId=0; colId < colNames.size(); colId++) 
				{
					String colName = colNames.get(colId);
					if      (colName.equals("CacheSearches"))  CacheSearchesId   = colId;
//					else if (colName.equals("LogicalReads"))   LogicalReadsId    = colId;
					else if (colName.equals("PhysicalReads"))  PhysicalReadsId   = colId;
//					else if (colName.equals("PhysicalWrites")) PhysicalWritesId  = colId;

					else if (colName.equals("CacheHitRate"))   CacheHitRateId    = colId;
//					else if (colName.equals("Misses"))         MissesId          = colId;
//					else if (colName.equals("Volatility"))     VolatilityId      = colId;
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
		tmp.addTrendGraphData(CM_GRAPH_NAME__DATA_CACHE__ACTIVITY, new TrendGraphDataPoint(CM_GRAPH_NAME__DATA_CACHE__ACTIVITY, new String[] { "Logical Reads", "Physical Reads", "Writes" }));
		if (AseTune.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_cache_activity.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );

			// GRAPH
			TrendGraph tg = new TrendGraph(CM_GRAPH_NAME__DATA_CACHE__ACTIVITY,
					// Menu Checkbox text
					"Data Caches Activity",
					// Label 
					"Activity for All Data Caches per Second", // Label 
					new String[] { "Logical Reads", "Physical Reads", "Writes" }, 
					false, tmp, true, -1);
			tmp.addTrendGraph(tg.getName(), tg, true);
		}

		_CMList.add(tmp);
		



		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// Pools Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------

		//==================================================================================================
		// 12.5.0.3: Description of: monCachePool
		// 
		// Description:
		//
		//
		// Needs Configuration:
		// none
		// 
		// Name                           Datatype     Attributes  Ind Description
		// ------------------------------ ------------ ----------- --- ------------------------------------------------------------
		// CacheID                        int                        0 Unique identifier for the cache
		// IOBufferSize                   int                        0 Size (in bytes) of the I/O buffer for the pool
		// AllocatedKB                    int                        0 Number of kilobytes that have been allocated for the pool
		// PhysicalReads                  int                        1 The number of buffers that have been read from disk into the pool
		// Stalls                         int                        1 Number of 'dirty' buffer retrievals
		// PagesTouched                   int                        1 Number of pages used within the pool
		// PagesRead                      int                        1 Number of pages read into the pool
		// BuffersToMRU                   int                        1 The number of buffers that were fetched and re-placed at the most recently used portion of the pool
		// BuffersToLRU                   int                        1 The number of buffers that were fetched and re-placed at the least recently used portion of the pool: fetch-and-discard
		// CacheName                      varchar(30)                0 Name of the cache
		//---------------------------------------------------------------------------------------------------
		// Column changes in various versions:
		//
		// Version      Action Name              Datatype   Attributes Description
		// ------------ ------ ----------------- ---------- ---------- ----------------------------------
		// 15.0.1CE/15.5 add   InstanceId        int                   The Server Instance Identifier (cluster only)
		// 15.7 (3B)    add    LogicalReads      int                   Counter Number of buffers read from the pool
		// 15.7 (3B)    add    PhysicalWrites    int                   Counter Number of write operations performed for data in this pool (one write operation may include multiple pages)
		// 15.7 (3B)    add    APFReads          int                   Counter Number of APF read operations that loaded pages into this pool
		// 15.7 (3B)    add    APFPercentage     int                   The configured asynchronous prefetch limit for this pool
		// 15.7 (3B)    add    WashSize          int                   The wash size (in kilobytes) for a memory pool
		//---------------------------------------------------------------------------------------------------
		
		name         = CM_NAME__CACHE_POOL;
		displayName  = CM_DESC__CACHE_POOL;
		description  = "The cahces has 2K or 16K pools, how are they behaving?";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 0;
		needCeVersion= 0;
		monTables    = new String[] {"monCachePool"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring=1"};
		colsCalcDiff = new String[] {"PagesRead", "PhysicalReads", "Stalls", "PagesTouched", "BuffersToMRU", "BuffersToLRU", "LogicalReads", "PhysicalWrites", "APFReads"};
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
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return new String[] {"enable monitoring=1"};
			}

			@Override
			public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
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
					                                                       "<b>Formula</b>: abs.PagesTouched / abs.AllocatedPages * 100<br>" +
					                                                   "</html>");
					mtd.addColumn("monCachePool",  "CacheEfficiency",  "<html>" +
					                                                       "If less than 100, the cache is to small (pages has been flushed ou from the cache).<br> " +
					                                                       "Pages are read in from the disk, could be by APF Reads (so cacheHitRate is high) but the pages still had to be read from disk.<br>" +
					                                                       "<b>Formula</b>: abs.AllocatedPages / diff.PagesRead * 100<br>" +
					                                                   "</html>");
				}
				catch (NameNotFoundException e) {/*ignore*/}
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				List <String> pkCols = new LinkedList<String>();

				if (isClusterEnabled)
					pkCols.add("InstanceID");

				pkCols.add("CacheName");
				pkCols.add("IOBufferSize");

				return pkCols;
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				//------------- NEEDS TO BE CALCULATED AFTER EACH SAMPLE
				// CacheUsage%      = AllocatedKB / (PagesTouched * @@maxpagesize)
				// CacheEfficiency% = PagesRead   / (PagesTouched * @@mxapagesize)

				if (isClusterEnabled)
				{
					cols1 += "InstanceID, ";
				}

				// ASE 15.7
				String LogicalReads    = "";
				String PhysicalWrites  = "";
				String APFReads        = "";
				String APFPercentage   = "";
				String WashSize        = "";


				if (aseVersion >= 15700)
				{
					LogicalReads    = "LogicalReads, \n";
					PhysicalWrites  = "PhysicalWrites, \n";
					APFReads        = "APFReads, \n";
					APFPercentage   = "APFPercentage, \n";
					WashSize        = "WashSize, \n";
				}

				cols1 += "CacheName, \n" +
				         "CacheID, \n" +
				         "SrvPageSize = @@maxpagesize, \n" +
				         "IOBufferSize, \n" +
				         WashSize +
				         APFPercentage +
				         "PagesPerIO = IOBufferSize/@@maxpagesize, \n" +
				         "AllocatedKB, \n" +
				         "AllocatedPages = convert(int,AllocatedKB*(1024.0/@@maxpagesize)), \n" +
				         "PagesRead, \n" +
				         LogicalReads + 
				         APFReads +
				         "PhysicalReads, \n" +
				         PhysicalWrites +
				         "Stalls, \n" +
				         "PagesTouched, \n" +
				         "BuffersToMRU, \n" +
				         "BuffersToLRU, \n" +
				         "CacheUtilization = convert(numeric(12,1), PagesTouched / (AllocatedKB*(1024.0/@@maxpagesize)) * 100.0), \n" +
				         "CacheEfficiency  = CASE \n" +
				         "                      WHEN PagesRead > 0 \n" +
				         "                      THEN convert(numeric(12,1), (AllocatedKB*(1024.0/@@maxpagesize)) / PagesRead * 100.0) \n" +
				         "                      ELSE 0.0 \n" +
				         "                   END \n" +
				         "";

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from master..monCachePool \n" +
					"order by CacheName, IOBufferSize\n";

				return sql;
			}

			/** 
			 * Compute 
			 */
			@Override
			public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
			{
				int AllocatedKB,        PagesTouched,        PagesRead,        SrvPageSize;
				int AllocatedKBId = -1, PagesTouchedId = -1, PagesReadId = -1, SrvPageSizeId = -1;
	
				int CacheUtilizationId = -1, CacheEfficiencyId = -1;
			
				// Find column Id's
				List<String> colNames = diffData.getColNames();
				if (colNames==null) return;
			
				for (int colId=0; colId < colNames.size(); colId++) 
				{
					String colName = colNames.get(colId);
					if      (colName.equals("AllocatedKB"))        AllocatedKBId        = colId;
					else if (colName.equals("PagesTouched"))       PagesTouchedId       = colId;
					else if (colName.equals("PagesRead"))          PagesReadId          = colId;
					else if (colName.equals("SrvPageSize"))        SrvPageSizeId        = colId;

					else if (colName.equals("CacheUtilization"))   CacheUtilizationId   = colId;
					else if (colName.equals("CacheEfficiency"))    CacheEfficiencyId    = colId;
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
		if (AseTune.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_pool_activity.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );
		}

		_CMList.add(tmp);





		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// Devices Activity			
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------

		//==================================================================================================
		// 12.5.0.3: Description of: monDeviceIO
		// 
		// Description:
		//
		//
		// Needs Configuration:
		// none
		// 
		// Name                           Datatype     Attributes  Ind Description
		// ------------------------------ ------------ ----------- --- ------------------------------------------------------------
		// Reads                          int                        3 Number of reads from the device (excluding APF)
		// APFReads                       int                        3 Number of APF reads from the device
		// Writes                         int                        3 Number of writes to the device
		// DevSemaphoreRequests           int                        3 Number of I/O requests
		// DevSemaphoreWaits              int                        3 Number of tasks forced to wait for synchronization of an I/O request
		// IOTime                         int                        1 Total amount of time (in milliseconds) spent waiting for I/O requests to be satisfied
		// LogicalName                    varchar(30)                0 Logical name of the device
		// PhysicalName                   varchar(128)               0 Full hierarchic file name of the device
		//---------------------------------------------------------------------------------------------------
		// Column changes in various versions:
		//
		// Version      Action Name              Datatype   Attributes Description
		// ------------ ------ ----------------- ---------- ---------- ----------------------------------
		// 15.0.1CE/15.5 add   InstanceId        int                   The Server Instance Identifier (cluster only)
		//---------------------------------------------------------------------------------------------------
		
		name         = CM_NAME__DEVICE_IO;
		displayName  = CM_DESC__DEVICE_IO;
		description  = 
			"<html>" +
			  "<p>What devices are doing IO's and what's the approximare service time on the disk.</p>" +
			  "Do not trust the service time <b>too</b> much...<br>" +
			  "<br>" +
			  TRANLOG_DISK_IO_TOOLTIP +
			"</html>";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 0;
		needCeVersion= 0;
		monTables    = new String[] {"monDeviceIO"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring=1"};
		colsCalcDiff = new String[] {"TotalIOs", "Reads", "APFReads", "Writes", "DevSemaphoreRequests", "DevSemaphoreWaits", "IOTime"};
		colsCalcPCT  = new String[] {"ReadsPct", "APFReadsPct", "WritesPct"};
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
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return new String[] {"enable monitoring=1"};
			}

			@Override
			public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				try 
				{
					MonTablesDictionary mtd = MonTablesDictionary.getInstance();
					mtd.addColumn("monDeviceIO",  "TotalIOs",     "<html>" +
					                                                   "Total number of IO's issued on this device.<br>" +
					                                                   "<b>Formula</b>: Reads + Writes<br>" +
					                                              "</html>");
					mtd.addColumn("monDeviceIO",  "APFReadsPct",  "<html>" +
					                                                   "Of all the issued Reads, what's the Asynch Prefetch Reads percentage.<br>" +
					                                                   "<b>Formula</b>: APFReads / Reads * 100<br>" +
					                                              "</html>");
					mtd.addColumn("monDeviceIO",  "WritesPct",    "<html>" +
					                                                   "Of all the issued IO's, what's the Write percentage.<br>" +
					                                                   "<b>Formula</b>: Writes / (Reads + Writes) * 100<br>" +
					                                              "</html>");
					mtd.addColumn("monDeviceIO",  "AvgServ_ms",   "<html>" +
					                                                   "Service time on the disk.<br>" +
					                                                   "This is basically the average time it took to make a disk IO on this device.<br>" +
					                                                   "Warning: ASE isn't timing each IO individually, Instead it uses the 'click ticks' to do it... This might change in the future.<br>" +
					                                                   "<b>Formula</b>: IOTime / (Reads + Writes) <br>" +
					                                                   "<b>Note</b>: If there is few I/O's this value might be a bit off, this due to 'click ticks' is 100 ms by default.<br>" +
					                                              "</html>");
				}
				catch (NameNotFoundException e) {/*ignore*/}
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				List <String> pkCols = new LinkedList<String>();

				if (isClusterEnabled)
					pkCols.add("InstanceID");

				pkCols.add("LogicalName");

				return pkCols;
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				if (isClusterEnabled)
				{
					cols1 += "InstanceID, ";
				}

				String TotalIOs = "TotalIOs = (Reads + Writes)";
				if (aseVersion > 15000) 
					TotalIOs = "TotalIOs = convert(bigint,Reads) + convert(bigint,Writes)";

				String DeviceType = "";
				if (aseVersion >= 15020) 
				{
					DeviceType = 
						"DeviceType = CASE \n" +
						"               WHEN getdevicetype(PhysicalName) = 1 THEN 'RAW Device  '\n" +
						"               WHEN getdevicetype(PhysicalName) = 2 THEN 'BLOCK Device'\n" +
						"               WHEN getdevicetype(PhysicalName) = 3 THEN 'File        '\n" +
						"               ELSE '-unknown-'+convert(varchar(5), getdevicetype(PhysicalName))+'-'\n" +
						"             END,\n";
				}
				
				cols1 += "LogicalName, "+TotalIOs+", \n" +
				         "Reads, \n" +
				         "ReadsPct = CASE WHEN Reads + Writes > 0 \n" +
				         "                THEN convert(numeric(10,1), (Reads + 0.0) / (Reads + Writes + 0.0) * 100.0 ) \n" +
				         "                ELSE convert(numeric(10,1), 0.0 ) \n" +
				         "           END, \n" +
				         "APFReads, \n" +
				         "APFReadsPct = CASE WHEN Reads > 0 \n" +
				         "                   THEN convert(numeric(10,1), (APFReads + 0.0) / (Reads + 0.0) * 100.0 ) \n" +
				         "                   ELSE convert(numeric(10,1), 0.0 ) \n" +
				         "              END, \n" +
				         "Writes, \n" +
				         "WritesPct = CASE WHEN Reads + Writes > 0 \n" +
				         "                 THEN convert(numeric(10,1), (Writes + 0.0) / (Reads + Writes + 0.0) * 100.0 ) \n" +
				         "                 ELSE convert(numeric(10,1), 0.0 ) \n" +
				         "            END, \n" +
				         "DevSemaphoreRequests, DevSemaphoreWaits, IOTime, \n";
				cols2 += "AvgServ_ms = CASE \n" +
						 "               WHEN Reads+Writes>0 \n" +
						 "               THEN convert(numeric(10,1), IOTime / convert(numeric(10,0), Reads+Writes)) \n" +
						 "               ELSE convert(numeric(10,1), null) \n" +
						 "             END \n";
				cols3 += ", "+DeviceType+" PhysicalName";
				if (aseVersion >= 15010 || (aseVersion >= 12540 && aseVersion <= 15000) )
				{
				}

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from master..monDeviceIO\n" +
					"order by LogicalName" + (isClusterEnabled ? ", InstanceID" : "") + "\n";

				return sql;
			}

			/** 
			 * Compute the avgServ column, which is IOTime/(Reads+Writes)
			 */
			@Override
			public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
			{
				int AvgServ_msId=-1, ReadsPctId=-1, APFReadsPctId=-1, WritesPctId=-1;

				int Reads,      APFReads,      Writes,      IOTime;
				int ReadsId=-1, APFReadsId=-1, WritesId=-1, IOTimeId=-1;

				// Find column Id's
				List<String> colNames = diffData.getColNames();
				if (colNames == null)
					return;
				for (int colId = 0; colId < colNames.size(); colId++)
				{
					String colName = (String) colNames.get(colId);
					if      (colName.equals("Reads"))       ReadsId       = colId;
					else if (colName.equals("APFReads"))    APFReadsId    = colId;
					else if (colName.equals("Writes"))      WritesId      = colId;
					else if (colName.equals("IOTime"))      IOTimeId      = colId;

					else if (colName.equals("ReadsPct"))    ReadsPctId    = colId;
					else if (colName.equals("APFReadsPct")) APFReadsPctId = colId;
					else if (colName.equals("WritesPct"))   WritesPctId   = colId;
					else if (colName.equals("AvgServ_ms"))  AvgServ_msId  = colId;
				}

				// Loop on all diffData rows
				for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
				{
					Reads    = ((Number) diffData.getValueAt(rowId, ReadsId))   .intValue();
					APFReads = ((Number) diffData.getValueAt(rowId, APFReadsId)).intValue();
					Writes   = ((Number) diffData.getValueAt(rowId, WritesId))  .intValue();
					IOTime   = ((Number) diffData.getValueAt(rowId, IOTimeId))  .intValue();

					//--------------------
					//---- AvgServ_ms
					int totIo = Reads + Writes;
					if (totIo != 0)
					{
						// AvgServ_ms = (IOTime * 1000) / ( totIo);
						double calc = (IOTime + 0.0) / totIo;

						BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);;
						diffData.setValueAt(newVal, rowId, AvgServ_msId);
					}
					else
						diffData.setValueAt(new BigDecimal(0), rowId, AvgServ_msId);

					//--------------------
					//---- ReadsPct
					if (totIo > 0)
					{
						double calc = (Reads + 0.0) / (Reads + Writes + 0.0) * 100.0;

						BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);
						diffData.setValueAt(newVal, rowId, ReadsPctId);
					}
					else
						diffData.setValueAt(new BigDecimal(0), rowId, ReadsPctId);

					//--------------------
					//---- APFReadsPct
					if (Reads > 0)
					{
						double calc = (APFReads + 0.0) / (Reads + 0.0) * 100.0;

						BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);
						diffData.setValueAt(newVal, rowId, APFReadsPctId);
					}
					else
						diffData.setValueAt(new BigDecimal(0), rowId, APFReadsPctId);

					//--------------------
					//---- WritesPct
					if (totIo > 0)
					{
						double calc = (Writes + 0.0) / (Reads + Writes + 0.0) * 100.0;

						BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);
						diffData.setValueAt(newVal, rowId, WritesPctId);
					}
					else
						diffData.setValueAt(new BigDecimal(0), rowId, WritesPctId);
				}
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (AseTune.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_device_activity.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );
		}

		_CMList.add(tmp);
		




		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// IOQueue SUMMARY Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------

		//==================================================================================================
		// 12.5.0.3: Description of: monIOQueue
		// 
		// Description:
		//
		//
		// Needs Configuration:
		// none
		// 
		// Name                           Datatype     Attributes  Ind Description
		// ------------------------------ ------------ ----------- --- ------------------------------------------------------------
		// IOs                            int                        1 Total number of I/O operations
		// IOTime                         int                        1 Total amount of time (in milliseconds) spent waiting for I/O requests to be satisfied
		// LogicalName                    varchar(30)                0 Logical name of the device
		// IOType                         varchar(12)                0 Category for grouping I/O ['UserData', 'UserLog', 'TempdbData', 'TempdbLog']
		//---------------------------------------------------------------------------------------------------
		// Column changes in various versions:
		//
		// Version      Action Name              Datatype   Attributes Description
		// ------------ ------ ----------------- ---------- ---------- ----------------------------------
		// 15.0.1CE/15.5 add   InstanceId        int                   The Server Instance Identifier (cluster only)
		//---------------------------------------------------------------------------------------------------
		
		name         = CM_NAME__IO_QUEUE_SUM;
		displayName  = CM_DESC__IO_QUEUE_SUM;
		description  = 
			"<html><p>A <b>Summary</b> of how many IO's have the ASE Server done on various segments (UserDb/Tempdb/System)</p>" +
			"For ASE 15.0.2 or so, we will have 'System' segment covered aswell.<br>" +
			"Do not trust the service time <b>too</b> much...<br>" +
			"<br>" +
			TRANLOG_DISK_IO_TOOLTIP +
			"</html>";
		
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
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return new String[] {"enable monitoring=1"};
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				List <String> pkCols = new LinkedList<String>();

				if (isClusterEnabled)
					pkCols.add("InstanceID");

				pkCols.add("IOType");

				return pkCols;
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				cols1 = "IOType, \n" +
				        "IOs        = sum(convert(numeric(18,0), IOs)), \n" +
				        "IOTime     = sum(convert(numeric(18,0), IOTime)), \n" +
				        "AvgServ_ms = \n" +
				        "CASE \n" +
				        "  WHEN sum(convert(numeric(18,0), IOs)) > 0 \n" +
				        "  THEN convert(numeric(18,1), sum(convert(numeric(18,0), IOTime))/sum(convert(numeric(18,0), IOs))) \n" +
				        "  ELSE convert(numeric(18,1), null) \n" +
				        "END";
				if (aseVersion >= 15010 || (aseVersion >= 12540 && aseVersion <= 15000) )
				{
				}

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from master..monIOQueue \n" +
					"group by IOType \n" +
					"order by 1\n";

				return sql;
			}

			@Override
			public void updateGraphData(TrendGraphDataPoint tgdp)
			{
				if (CM_GRAPH_NAME__IO_QUEUE_SUM__DISK_IO_OPS.equals(tgdp.getName()))
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
//				SamplingCnt sAbs  = (SamplingCnt)getCounterDataAbs();
//				SamplingCnt sDiff = (SamplingCnt)getCounterDataDiff();
//				SamplingCnt sRate = (SamplingCnt)getCounterDataRate();
//				
//				System.out.println(sAbs .debugToString());
//				System.out.println(sDiff.debugToString());
//				System.out.println(sRate.debugToString());
			}
			@Override
			public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
			{
				double AvgServ_ms;
				long   IOs,        IOTime;
				int    IOsId = -1, IOTimeId = -1, AvgServ_msId = -1;

				// Find column Id's
				List<String> colNames = diffData.getColNames();
				if (colNames == null)
					return;
				for (int colId = 0; colId < colNames.size(); colId++)
				{
					String colName = (String) colNames.get(colId);
					if      (colName.equals("IOs"))        IOsId        = colId;
					else if (colName.equals("IOTime"))     IOTimeId     = colId;
					else if (colName.equals("AvgServ_ms")) AvgServ_msId = colId;
				}

				// Loop on all diffData rows
				for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
				{
					IOs    = ((Number) diffData.getValueAt(rowId, IOsId))   .longValue();
					IOTime = ((Number) diffData.getValueAt(rowId, IOTimeId)).longValue();

					if (IOs != 0)
					{
						AvgServ_ms = IOTime / IOs;
						BigDecimal newVal = new BigDecimal(AvgServ_ms).setScale(1, BigDecimal.ROUND_HALF_EVEN);;
						diffData.setValueAt(newVal, rowId, AvgServ_msId);
					}
					else
						diffData.setValueAt(new BigDecimal(0), rowId, AvgServ_msId);
				}
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		tmp.addTrendGraphData(CM_GRAPH_NAME__IO_QUEUE_SUM__DISK_IO_OPS, new TrendGraphDataPoint(CM_GRAPH_NAME__IO_QUEUE_SUM__DISK_IO_OPS, new String[] { "User Data", "User Log", "Tempdb Data", "Tempdb Log", "System" }));
		if (AseTune.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_io_queue_sum_activity.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );

			// GRAPH
			TrendGraph tg = new TrendGraph(CM_GRAPH_NAME__IO_QUEUE_SUM__DISK_IO_OPS,
					"Disk IO", 	// Menu Checkbox text
					"Number of Disk IO Operations per Second", // Label 
					new String[] { "User Data", "User Log", "Tempdb Data", "Tempdb Log", "System" }, 
					false, tmp, true, -1);
			tmp.addTrendGraph(tg.getName(), tg, true);
		}
		
		_CMList.add(tmp);





		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// IOQueue Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------

		//==================================================================================================
		// 12.5.0.3: Description of: monIOQueue
		// 
		// Description:
		//
		//
		// Needs Configuration:
		// none
		// 
		// Name                           Datatype     Attributes  Ind Description
		// ------------------------------ ------------ ----------- --- ------------------------------------------------------------
		// IOs                            int                        1 Total number of I/O operations
		// IOTime                         int                        1 Total amount of time (in milliseconds) spent waiting for I/O requests to be satisfied
		// LogicalName                    varchar(30)                0 Logical name of the device
		// IOType                         varchar(12)                0 Category for grouping I/O ['UserData', 'UserLog', 'TempdbData', 'TempdbLog']
		//---------------------------------------------------------------------------------------------------
		// Column changes in various versions:
		//
		// Version      Action Name              Datatype   Attributes Description
		// ------------ ------ ----------------- ---------- ---------- ----------------------------------
		// 15.0.1CE/15.5 add   InstanceId        int                   The Server Instance Identifier (cluster only)
		//---------------------------------------------------------------------------------------------------
		
		name         = CM_NAME__IO_QUEUE;
		displayName  = CM_DESC__IO_QUEUE;
		description  = 
			"<html><p>How many IO's have the ASE Server done on various segments (UserDb/Tempdb/System) on specific devices.</p>" +
			"For ASE 15.0.2 or so, we will have 'System' segment covered aswell.<br>" +
			"Do not trust the service time <b>too</b> much...<br>" +
			"<br>" +
			TRANLOG_DISK_IO_TOOLTIP +
			"</html>";
		
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
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return new String[] {"enable monitoring=1"};
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				List <String> pkCols = new LinkedList<String>();

				if (isClusterEnabled)
					pkCols.add("InstanceID");

				pkCols.add("LogicalName");
				pkCols.add("IOType");

				return pkCols;
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				if (isClusterEnabled)
				{
					cols1 += "InstanceID, ";
				}

				cols1 += "LogicalName, IOType, IOs, IOTime, \n" +
		                 "AvgServ_ms = \n" +
		                 "CASE \n" +
		                 "  WHEN IOs > 0 THEN convert(numeric(10,1), IOTime/convert(numeric(10,0),IOs)) \n" +
		                 "  ELSE              convert(numeric(10,1), null) \n" +
		                 "END";

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from master..monIOQueue \n" +
					"order by LogicalName, IOType" + (isClusterEnabled ? ", InstanceID" : "") + "\n";

				return sql;
			}

			@Override
			public void updateGraphData(TrendGraphDataPoint tgdp)
			{
				if (CM_GRAPH_NAME__IO_QUEUE__DEVICE_SERVICE_TIME.equals(tgdp.getName()))
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
				long   IOs,        IOTime;
				int    IOsId = -1, IOTimeId = -1, AvgServ_msId = -1;

				// Find column Id's
				List<String> colNames = diffData.getColNames();
				if (colNames == null)
					return;
				for (int colId = 0; colId < colNames.size(); colId++)
				{
					String colName = (String) colNames.get(colId);
					if      (colName.equals("IOs"))        IOsId        = colId;
					else if (colName.equals("IOTime"))     IOTimeId     = colId;
					else if (colName.equals("AvgServ_ms")) AvgServ_msId = colId;
				}

				// Loop on all diffData rows
				for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
				{
					IOs    = ((Number) diffData.getValueAt(rowId, IOsId))   .longValue();
					IOTime = ((Number) diffData.getValueAt(rowId, IOTimeId)).longValue();

					if (IOs != 0)
					{
						AvgServ_ms = IOTime / IOs;
						BigDecimal newVal = new BigDecimal(AvgServ_ms).setScale(1, BigDecimal.ROUND_HALF_EVEN);;
						diffData.setValueAt(newVal, rowId, AvgServ_msId);
					}
					else
						diffData.setValueAt(new BigDecimal(0), rowId, AvgServ_msId);
				}
			}
			
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		tmp.addTrendGraphData(CM_GRAPH_NAME__IO_QUEUE__DEVICE_SERVICE_TIME, new TrendGraphDataPoint(CM_GRAPH_NAME__IO_QUEUE__DEVICE_SERVICE_TIME, new String[] { "Max", "Average" }));
		if (AseTune.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_io_queue_activity.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );

			// GRAPH
			TrendGraph tg = new TrendGraph(CM_GRAPH_NAME__IO_QUEUE__DEVICE_SERVICE_TIME,
					"Device IO Service Time", 	// Menu Checkbox text
					"Device IO Service Time in Milliseconds", // Label 
					new String[] { "Max", "Average" }, 
					false, tmp, true, -1);
			tmp.addTrendGraph(tg.getName(), tg, true);
		}
		
		_CMList.add(tmp);




		
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// sysmonitors... SPINLOCKS
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = CM_NAME__SPINLOCK_SUM;
		displayName  = CM_DESC__SPINLOCK_SUM;
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
		     pkList.add("spinName");

		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				true, true, 300) // Does the spinlock wrap to 0 or does it wrap to -hugeNumber
		// OVERRIDE SOME DEFAULT METHODS
		{
			private static final long serialVersionUID = -925512251960490929L;

			@Override
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return null;
			}

//			@Override
//			protected CmSybMessageHandler createSybMessageHandler()
//			{
//				CmSybMessageHandler msgHandler = super.createSybMessageHandler();
//
//				msgHandler.addDiscardMsgNum(0);
//
//				return msgHandler;
//			}
			@Override
			protected CmSybMessageHandler createSybMessageHandler()
			{
				CmSybMessageHandler msgHandler = super.createSybMessageHandler();

				// in some ASE 15.5 ESD#4 versions message 'spin_type, spin_name' is thrown...
				// this in conjunction when dbcc traceon(3604) has been enabled.
				// I dont know why this is happening, most likly a bug...
				// So just discard this message
				msgHandler.addDiscardMsgStr("spin_type, spin_name");

				return msgHandler;
			}


			@Override
			public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				List <String> pkCols = new LinkedList<String>();

				if (isClusterEnabled)
					pkCols.add("instanceid");

				pkCols.add("spinName");

				return pkCols;
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				// sum(int) may cause: "Arithmetic overflow occurred"
				// max int is: 2147483647, so if we sum several rows we may overflow the integer
				// so on pre 15.0 use numeric instead, over 15.0 use bigint
				String datatype    = "numeric(12,0)"; 
				String optGoalPlan = "";

				if (aseVersion >= 15000)
				{
					datatype    = "bigint";
				}
				if (aseVersion >= 15020)
				{
					optGoalPlan = "plan '(use optgoal allrows_dss)' \n";
				}

				/*
				 * Retrieve the spinlocks. There are three spinlock counters collected by dbcc monitor:
				 *	- spinlock_p_0 -> Spinlock "grabs" - as in attempted grabs for the spinlock - includes waits
				 *	- spinlock_w_0 -> Spinlock "waits" - usually a good sign of contention
				 *	- spinlock_s_0 -> Spinlock "spins" - this is the CPU spins that drives up CPU utilization
				 *	                  The higher the spin count, the more CPU usage and the more serious
				 *	                  the performance impact of the spinlock on other processes not waiting
				 */
				String preDropTmpTables =
					"\n " +
					"/*------ drop tempdb objects if we failed doing that in previous execution -------*/ \n" +
					"if ((select object_id('#spin_names'))   is not null) drop table #spin_names \n" +
					"if ((select object_id('#sysmonitorsP')) is not null) drop table #sysmonitorsP \n" +
					"if ((select object_id('#sysmonitorsW')) is not null) drop table #sysmonitorsW \n" +
					"if ((select object_id('#sysmonitorsS')) is not null) drop table #sysmonitorsS \n" +
					"\n";
				
				String sqlCreateTmpTabCache = 
					"\n " +
					"/*------ temp table to hold 'cache names' and other 'named' stuff -------*/ \n" +
					"create table #spin_names     \n " +
					"(                            \n " +
					"  spin_name  varchar(50),    \n " +
					"  spin_type  varchar(20),    \n " +
					"  spin_desc  varchar(100),   \n " +
					"  start_id   int,            \n " +
					"  instanceid tinyint         /* if ASE SMP, this will always be value 1 */\n " +
					")                            \n " +
					"\n" +
					"/*------ DATA CACHES -------*/ \n" +
					"insert into #spin_names(spin_name, spin_type, spin_desc, start_id, instanceid) \n" +
					"select comment, 'CACHELET', 'Data cache, spinlock instance', 0, " + (isClusterEnabled ? "instanceid" : "1") + " \n" + 
					"  from master..syscurconfigs \n" +
					" where config=19 and value > 0 \n" +
					"\n" +
					"/*------ SPINLOCK INSTANCES, for other stuff -------*/ \n" +
					"insert into #spin_names(spin_name, spin_type, spin_desc, start_id, instanceid) \n" +
					"select 'Kernel->erunqspinlock', 'KERNEL-INST', 'Engine Runqueue, spinlock instance', 0, " + (isClusterEnabled ? "instanceid" : "1") + " \n" +
					"  from master..syscurconfigs \n" + 
					" where config=19 and value > 0 \n" +
					"   and comment = 'default data cache' /* if ASE CE, then this will produce number of Cluster Instances, ASE SMP = 1 row */ \n" + 
					"\n";

				String sqlUpdateTmpTabCache = 
					"/*------ SET start_id -------*/ \n" +
					"update #spin_names \n" +
					"   set start_id = isnull((select min(M.field_id) \n" +
					"                          from #sysmonitorsP M\n" +
					"                          where #spin_names.spin_name  = M.field_name \n" +
					"                            and #spin_names.instanceid = " + (isClusterEnabled ? "M.instanceid" : "1") + ") \n" +
					"                  ,start_id) \n";

				String sqlDropTmpTabCache = 
					"\n" +
					"drop table #spin_names \n";


				String instanceid = ""; // If cluster, we need to use column 'instanceid' as well
				if (isClusterEnabled) 
					instanceid = ", instanceid";

				String sqlCreateTmpSysmonitors = 
					"/*------ Copy 'spinlock_' rows to local tempdb, this reduces IO in joins below -------*/ \n" +
					"select field_name, field_id, value "+instanceid+" into #sysmonitorsP FROM master..sysmonitors WHERE group_name = 'spinlock_p_0' \n" +
					"select             field_id, value "+instanceid+" into #sysmonitorsW FROM master..sysmonitors WHERE group_name = 'spinlock_w_0' \n" +
					"select             field_id, value "+instanceid+" into #sysmonitorsS FROM master..sysmonitors WHERE group_name = 'spinlock_s_0' \n";
				if (aseVersion >= 15700)
					sqlCreateTmpSysmonitors =
						"/*------ Copy 'spinlock_' rows to local tempdb, this reduces IO in joins below -------*/ \n" +
						"select field_name, field_id, value "+instanceid+" into #sysmonitorsP FROM master..sysmonitors WHERE group_name = 'spinlock_p' \n" +
						"select             field_id, value "+instanceid+" into #sysmonitorsW FROM master..sysmonitors WHERE group_name = 'spinlock_w' \n" +
						"select             field_id, value "+instanceid+" into #sysmonitorsS FROM master..sysmonitors WHERE group_name = 'spinlock_s' \n";

				String sqlDropTmpTabPWS   = 
					"\n" +
					"drop table #sysmonitorsP \n"+
					"drop table #sysmonitorsW \n" +
					"drop table #sysmonitorsS \n";
				
				String sqlSampleSpins =
					"/*------ SAMPLE THE monitors to master..sysmonitors -------*/ \n" +
					"DBCC monitor('sample', 'all',        'on') \n" +
					"DBCC monitor('sample', 'spinlock_s', 'on') \n" +
					"\n" +
					sqlCreateTmpSysmonitors +
					" \n" +
					sqlUpdateTmpTabCache +
					" \n" +
					"/*------ get data: for all spinlocks -------*/ \n" +
					"SELECT \n" +
					"  type         = \n" +
					"    CASE \n" +
					"      WHEN P.field_name like 'Dbtable->%'  THEN convert(varchar(20), 'DBTABLE')  \n" +
					"      WHEN P.field_name like 'Dbt->%'      THEN convert(varchar(20), 'DBTABLE')  \n" +
					"      WHEN P.field_name like 'Dbtable.%'   THEN convert(varchar(20), 'DBTABLE')  \n" +
					"      WHEN P.field_name like 'Resource->%' THEN convert(varchar(20), 'RESOURCE') \n" +
					"      WHEN P.field_name like 'Kernel->%'   THEN convert(varchar(20), 'KERNEL')   \n" +
					"      WHEN P.field_name in (select spin_name from #spin_names where spin_type = 'CACHELET')   \n" +
					"                                           THEN convert(varchar(20), 'CACHE') \n" +
					"      ELSE convert(varchar(20), 'OTHER')  \n" +
					"    END, \n" +
					(isClusterEnabled ? "P.instanceid, \n" : "") +
					"  spinName     = convert(varchar(50), P.field_name), \n" +
					"  instances    = count(P.field_id), \n" +
					"  grabs        = sum(convert("+datatype+",P.value)), \n" +
					"  waits        = sum(convert("+datatype+",W.value)), \n" +
					"  spins        = sum(convert("+datatype+",S.value)), \n" +
					"  contention   = convert(numeric(4,1), null), \n" +
					"  spinsPerWait = convert(numeric(9,1), null), \n" +
					"  description  = convert(varchar(100), '') \n" +
					"FROM #sysmonitorsP P, #sysmonitorsW W, #sysmonitorsS S \n" +
					"WHERE P.field_id = W.field_id \n" +
					"  AND P.field_id = S.field_id \n";
				if (isClusterEnabled) 
				{
					sqlSampleSpins +=
					"  AND P.instanceid = W.instanceid \n" +
					"  AND P.instanceid = S.instanceid \n" +
					"GROUP BY P.instanceid, P.field_name \n" +
					"ORDER BY 3, 2 \n" +
					optGoalPlan;
				}
				else 
				{
					sqlSampleSpins +=
					"GROUP BY P.field_name \n" +
					"ORDER BY 2 \n" +
					optGoalPlan;
				}

				//---------------------------------------------
				// For CACHES and other FINER GRANULARITY spinlocks
				// do the calculation on EACH Cache partition or spinlock instance
				//---------------------------------------------
				String sqlForCaches =
					"\n" +
					"/*------ get data: For some selected spinlocks with multiple instances -------*/ \n" +
					"SELECT \n" + 
					"  type         = N.spin_type, \n" +
					(isClusterEnabled ? "P.instanceid, \n" : "") +
					"  spinName     = convert(varchar(50), convert(varchar(40),P.field_name) + ' # ' + convert(varchar(5), P.field_id-N.start_id)), \n" +
					"  instances    = convert(int,1), \n" +
					"  grabs        = convert("+datatype+",P.value), \n" +
					"  waits        = convert("+datatype+",W.value), \n" +
					"  spins        = convert("+datatype+",S.value), \n" +
					"  contention   = convert(numeric(4,1), null), \n" +
					"  spinsPerWait = convert(numeric(9,1), null), \n" +
					"  description  = N.spin_desc \n" +
					"FROM #sysmonitorsP P, #sysmonitorsW W, #sysmonitorsS S, #spin_names N \n" +
					"WHERE P.field_id = W.field_id \n" +
					"  AND P.field_id = S.field_id \n" +
				    "  AND P.field_name = N.spin_name \n";
				if (isClusterEnabled) 
				{
					sqlForCaches +=
					"  AND P.instanceid = W.instanceid \n" +
					"  AND P.instanceid = S.instanceid \n" +
				    "  AND P.instanceid = N.instanceid \n" +
					"ORDER BY 3, 2 \n" +
					optGoalPlan;
				}
				else 
				{
					sqlForCaches +=
					"  AND N.instanceid = 1 \n" +
					"ORDER BY 2 \n" +
					optGoalPlan;
				}

				String sql = preDropTmpTables + sqlCreateTmpTabCache + sqlSampleSpins + sqlForCaches + sqlDropTmpTabCache + sqlDropTmpTabPWS;

				return sql;
			}

			@Override
			public String getSqlInitForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				//---------------------------------------------
				// SQL INIT (executed first time only)
				// AFTER 12.5.2 traceon(8399) is no longer needed
				//---------------------------------------------
				String sqlInit = "DBCC traceon(3604) \n";

				if (aseVersion < 12520)
					sqlInit += "DBCC traceon(8399) \n";

				if (aseVersion >= 15020 || (aseVersion >= 12541 && aseVersion <= 15000) )
				{
					sqlInit = "set switch on 3604 with no_info \n";
				}

				sqlInit += "DBCC monitor('select', 'all',        'on') \n" +
				           "DBCC monitor('select', 'spinlock_s', 'on') \n";

				return sqlInit;
			}

			@Override
			public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
			{
				MonTablesDictionary mtd = MonTablesDictionary.getInstance();

				long grabs,        waits,        spins;
				int  grabsId = -1, waitsId = -1, spinsId = -1, contentionId = -1, spinsPerWaitId = -1;
				int  pos_name = -1,  pos_desc = -1;

				// Find column Id's
				List<String> colNames = diffData.getColNames();
				if (colNames == null)
					return;
				for (int colId = 0; colId < colNames.size(); colId++)
				{
					String colName = (String) colNames.get(colId);
					if      (colName.equals("grabs"))        grabsId        = colId;
					else if (colName.equals("waits"))        waitsId        = colId;
					else if (colName.equals("spins"))        spinsId        = colId;
					else if (colName.equals("contention"))   contentionId   = colId;
					else if (colName.equals("spinsPerWait")) spinsPerWaitId = colId;
					else if (colName.equals("name"))         pos_name       = colId;
					else if (colName.equals("description"))  pos_desc       = colId;
				}

				// Loop on all diffData rows
				for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
				{
					grabs = ((Number) diffData.getValueAt(rowId, grabsId)).longValue();
					waits = ((Number) diffData.getValueAt(rowId, waitsId)).longValue();
					spins = ((Number) diffData.getValueAt(rowId, spinsId)).longValue();

					// contention
					if (grabs > 0)
					{
						BigDecimal contention = new BigDecimal( ((1.0 * (waits)) / grabs) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

						// Keep only 3 decimals
						// row.set(AvgServ_msId, new Double (AvgServ_ms/1000) );
						diffData.setValueAt(contention, rowId, contentionId);
					}
					else
						diffData.setValueAt(new BigDecimal(0), rowId, contentionId);

					// spinsPerWait
					if (waits > 0)
					{
						BigDecimal spinWarning = new BigDecimal( ((1.0 * (spins)) / waits) ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

						diffData.setValueAt(spinWarning, rowId, spinsPerWaitId);
					}
					else
						diffData.setValueAt(new BigDecimal(0), rowId, spinsPerWaitId);

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

		// If QUERY TIMEOUT is not set, set this to X second, because this can take time. 
		if (tmp.getQueryTimeout() == CountersModel.DEFAULT_sqlQueryTimeout)
			tmp.setQueryTimeout(30);

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (AseTune.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_spinlock_activity.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );
		}
		
		_CMList.add(tmp);






		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// sysmonitors... SYSMON (reuses data from SPINLOCK, which is still in sysmonitors)
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = CM_NAME__SYSMON;
		displayName  = CM_DESC__SYSMON;
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
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return null;
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				List <String> pkCols = new LinkedList<String>();

				if (isClusterEnabled)
					pkCols.add("instanceid");

				pkCols.add("field_name");
				pkCols.add("group_name");

				return pkCols;
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String optGoalPlan = "";
				if (aseVersion >= 15020)
				{
					optGoalPlan = "plan '(use optgoal allrows_dss)' \n";
				}

				String discardSpinlocks = "group_name not in ('spinlock_p_0', 'spinlock_w_0', 'spinlock_s_0')";
				if (aseVersion >= 15700)
					discardSpinlocks = "group_name not in ('spinlock_p', 'spinlock_w', 'spinlock_s')";

				String sql =   
					"SELECT \n" +
					(isClusterEnabled ? "instanceid, \n" : "") +
					"  field_name = convert(varchar(100),field_name), \n" +
					"  group_name = convert(varchar(30),group_name), \n" +
					"  field_id, \n" +
					"  value, \n" +
					"  description  = convert(varchar(255), description) \n" +
					"FROM master..sysmonitors \n" +
					"WHERE " + discardSpinlocks + " \n" +
					"  AND value > 100 \n" +
					"  OR  group_name = 'config' \n" +
					"ORDER BY group_name, field_name" + (isClusterEnabled ? ", instanceid" : "") + "\n" +
					optGoalPlan;

				return sql;
			}
		};

		// If QUERY TIMEOUT is not set, set this to X second, because this can take time. 
		if (tmp.getQueryTimeout() == CountersModel.DEFAULT_sqlQueryTimeout)
			tmp.setQueryTimeout(30);

		tmp.addDependsOnCm(CM_NAME__SPINLOCK_SUM); // CMspinlockSum must have been executed before this cm
		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (AseTune.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName())
			{
				private static final long	serialVersionUID	= 1L;
				
				@Override
				protected void updateExtendedInfoPanel()
				{
					JPanel  panel = getExtendedInfoPanel();
					if (panel == null) 
						return;

					// If the panel is so small, that is not visible, don't update the graph
					int dividerLocation = getMainSplitPane() == null ? 0 : getMainSplitPane().getDividerLocation();
					if (dividerLocation == 0)
						return;
					
//					if ( ! isMonConnected() )
//						return;

					CountersModel cm = getDisplayCm();
					if (cm == null)
						cm = getCm();

//System.out.println(CM_NAME__SYSMON+" -- CM="+cm);
					if (cm == null)
						return;

//System.out.println(CM_NAME__SYSMON+" -- cm.hasDiffData()="+cm.hasDiffData());
					if ( ! cm.hasDiffData() )
						return;

					RTextArea textArea = (RTextArea)getClientProperty("textArea");
					if (textArea == null)
						return;

					int caretPosition = textArea.getCaretPosition();

					SpSysmon sysmon = new SpSysmon(cm);
					sysmon.calc();
//					sysmon.printReport();
					String report = sysmon.getReportText();

					textArea.setText(report);
					if (caretPosition > 0 && caretPosition < textArea.getDocument().getLength())
						textArea.setCaretPosition(caretPosition);
				}

				@Override
				protected JPanel createExtendedInfoPanel()
				{
//					JSplitPane mainSplitPane = getMainSplitPane();
					JPanel panel = SwingUtils.createPanel("Extended Information", false);
	
					panel.setLayout(new BorderLayout());
//					panel.add(new JScrollPane(createTreeSpSysmon()), BorderLayout.CENTER);

					RSyntaxTextArea textArea = new RSyntaxTextArea();
					RTextScrollPane textScroll = new RTextScrollPane(textArea, true);
					textArea.setText("empty sp_sysmon");
//					textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
//					textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
//					textArea.setCodeFoldingEnabled(true);

					putClientProperty("textArea",   textArea);
					putClientProperty("textScroll", textScroll);

					panel.add(textScroll, BorderLayout.CENTER);

//					panel.setPreferredSize(new Dimension(0, 0));
//					panel.setMinimumSize(new Dimension(0, 0));
//					panel.setSize(new Dimension(0, 500));
//					mainSplitPane.setDividerLocation(150);
					return panel;
				}

				@Override
				protected void setSplitPaneOptions(JSplitPane mainSplitPane, JPanel dataPanel, JPanel extendedInfoPanel)
				{
					mainSplitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
					mainSplitPane.setBorder(null);
					mainSplitPane.add(dataPanel,         JSplitPane.LEFT);
					mainSplitPane.add(extendedInfoPanel, JSplitPane.RIGHT);
					mainSplitPane.setDividerSize(3);

					mainSplitPane.setDividerLocation(700);
				}

				private JTree createTreeSpSysmon()
				{
					DefaultMutableTreeNode top = new DefaultMutableTreeNode("sp_sysmon");
					DefaultMutableTreeNode heading = new DefaultMutableTreeNode("");
					DefaultMutableTreeNode subHead = new DefaultMutableTreeNode("");
	
					heading = new DefaultMutableTreeNode("Kernel Utilization");
					top.add(heading);
					subHead = new DefaultMutableTreeNode("Config");
					heading.add(subHead);
					subHead.add(new DefaultMutableTreeNode("Runnable Process Search Count"));
					subHead.add(new DefaultMutableTreeNode("I/O Polling Process Count"));
	
					subHead = new DefaultMutableTreeNode("Engine Busy Utilization");
					heading.add(subHead);
					subHead.add(new DefaultMutableTreeNode("Engine 0"));
	
					subHead = new DefaultMutableTreeNode("CPU Yields by Engine");
					heading.add(subHead);
					subHead.add(new DefaultMutableTreeNode("Engine 0"));
	
					subHead = new DefaultMutableTreeNode("Network Checks");
					heading.add(subHead);
					subHead.add(new DefaultMutableTreeNode("Non-Blocking"));
					subHead.add(new DefaultMutableTreeNode("Blocking"));
					subHead.add(new DefaultMutableTreeNode("Total Network I/O Checks"));
					subHead.add(new DefaultMutableTreeNode("Avg Net I/Os per Check"));
	
					subHead = new DefaultMutableTreeNode("Disk I/O Checks");
					heading.add(subHead);
					subHead.add(new DefaultMutableTreeNode("Total Disk I/O Checks"));
					subHead.add(new DefaultMutableTreeNode("Checks Returning I/O"));
					subHead.add(new DefaultMutableTreeNode("Avg Disk I/Os Returned"));
	
					heading = new DefaultMutableTreeNode("Worker Process Management");
					top.add(heading);
	
					heading = new DefaultMutableTreeNode("Parallel Query Management");
					top.add(heading);
	
					heading = new DefaultMutableTreeNode("Task Management");
					top.add(heading);
	
					subHead = new DefaultMutableTreeNode("Task Context Switches by Engine");
					heading.add(subHead);
					subHead.add(new DefaultMutableTreeNode("Engine 0"));
	
					subHead = new DefaultMutableTreeNode("Task Context Switches Due To");
					heading.add(subHead);
	
					subHead.add(new DefaultMutableTreeNode("Voluntary Yields"));
					subHead.add(new DefaultMutableTreeNode("Cache Search Misses"));
					subHead.add(new DefaultMutableTreeNode("Exceeding I/O batch size"));
					subHead.add(new DefaultMutableTreeNode("System Disk Writes"));
					subHead.add(new DefaultMutableTreeNode("Logical Lock Contention"));
					subHead.add(new DefaultMutableTreeNode("Address Lock Contention"));
					subHead.add(new DefaultMutableTreeNode("Latch Contention"));
					subHead.add(new DefaultMutableTreeNode("Log Semaphore Contention"));
					subHead.add(new DefaultMutableTreeNode("PLC Lock Contention"));
					subHead.add(new DefaultMutableTreeNode("Group Commit Sleeps"));
					subHead.add(new DefaultMutableTreeNode("Last Log Page Writes"));
					subHead.add(new DefaultMutableTreeNode("Modify Conflicts"));
					subHead.add(new DefaultMutableTreeNode("I/O Device Contention"));
					subHead.add(new DefaultMutableTreeNode("Network Packet Received"));
					subHead.add(new DefaultMutableTreeNode("Network Packet Sent"));
					subHead.add(new DefaultMutableTreeNode("Network services"));
					subHead.add(new DefaultMutableTreeNode("Other Causes"));
	
					heading = new DefaultMutableTreeNode("Application Management");
					top.add(heading);
	
					heading = new DefaultMutableTreeNode("ESP Management");
					top.add(heading);
	
					heading = new DefaultMutableTreeNode("Transaction Profile");
					top.add(heading);
	
					heading = new DefaultMutableTreeNode("Transaction Management");
					top.add(heading);
	
					heading = new DefaultMutableTreeNode("Index Management");
					top.add(heading);
	
					heading = new DefaultMutableTreeNode("Metadata Cache Management");
					top.add(heading);
	
					heading = new DefaultMutableTreeNode("Lock Management");
					top.add(heading);
	
					heading = new DefaultMutableTreeNode("Data Cache Management");
					top.add(heading);
	
					heading = new DefaultMutableTreeNode("Procedure Cache Management");
					top.add(heading);
	
					heading = new DefaultMutableTreeNode("Memory Management");
					top.add(heading);
	
					heading = new DefaultMutableTreeNode("Recovery Management");
					top.add(heading);
	
					heading = new DefaultMutableTreeNode("Disk I/O Management");
					top.add(heading);
	
					heading = new DefaultMutableTreeNode("Network I/O Management");
					top.add(heading);
	
					heading = new DefaultMutableTreeNode("Replication Agent");
					top.add(heading);
	
					return new JTree(new DefaultTreeModel(top));
				}
			};
			
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_sysmon_activity.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );
		}
		
		_CMList.add(tmp);






		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// sysmonitors... RepAgent (reuses data from SPINLOCK, which is still in sysmonitors)
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = CM_NAME__REP_AGENT;
		displayName  = CM_DESC__REP_AGENT;
		description  = "<html><p>Grabs the raw counters for RepAgents from sysmonitors.</p>" +
			"NOTE: reuses data from 'Spinlock Sum', so this needs to be running as well.<br>" +
			"For the moment consider this as <b>very experimental</b>.</html>";

		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 12540;
		needCeVersion= 0;
		monTables    = new String[] {"sysmonitors"};
		needRole     = new String[] {"sa_role"};
		needConfig   = new String[] {};
		colsCalcDiff = new String[] {"value", "log_waits", "sum_log_wait", 
			"longest_log_wait", "truncpt_moved", "truncpt_gotten", "rs_connect", 
			"fail_rs_connect", "io_send", "sum_io_send_wait", "longest_io_send_wait", 
			"io_recv", "sum_io_recv_wait", "longest_io_recv_wait", "packets_sent", 
			"full_packets_sent", "sum_packet", "largest_packet",  "log_records_scanned", 
			"log_records_processed", "log_scans", "sum_log_scan", "longest_log_scan", 
			"open_xact", "maintuser_xact", "commit_xact", "abort_xact", "prepare_xact", 
			"xupdate_processed", "xinsert_processed", "xdelete_processed", 
			"xexec_processed", "xcmdtext_processed", "xwrtext_processed", 
			"xrowimage_processed", "xclr_processed", "xckpt_processed", 
			"xckpt_genxactpurge", "sqldml_processed", "bckward_schema", 
			"sum_bckward_wait", "longest_bckward_wait", "forward_schema", 
			"sum_forward_wait", "longest_forward_wait", "delayed_commit_xact", 
			"schema_reuse"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList<String>();
		     pkList.add("dbname");
//		     pkList.add("field_name");

		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				true, true, 300) // Does the spinlock wrap to 0 or does it wrap to -hugeNumber
		// OVERRIDE SOME DEFAULT METHODS
		{
			private static final long serialVersionUID = -925512251960490929L;

			@Override
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return null;
			}

			@Override
			public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				try 
				{
					MonTablesDictionary mtd = MonTablesDictionary.getInstance();
					mtd.addColumn("sysmonitors", "value",                 "<html>The Counter value for this raw counter name.</html>");
					mtd.addColumn("sysmonitors", "log_waits",             "<html>Log Extension Wait:                Count</html>");
					mtd.addColumn("sysmonitors", "sum_log_wait",          "<html>Log Extension Wait:                Amount of time (ms)</html>");
					mtd.addColumn("sysmonitors", "longest_log_wait",      "<html>Log Extension Wait:                Longest Wait (ms)</html>");
					mtd.addColumn("sysmonitors", "truncpt_moved",         "<html>Truncation Point Movement:         Moved</html>");
					mtd.addColumn("sysmonitors", "truncpt_gotten",        "<html>Truncation Point Movement:         Gotten from RS</html>");
					mtd.addColumn("sysmonitors", "rs_connect",            "<html>Connections to Replication Server: Success</html>");
					mtd.addColumn("sysmonitors", "fail_rs_connect",       "<html>Connections to Replication Server: Failed</html>");
					mtd.addColumn("sysmonitors", "io_send",               "<html>I/O Wait from RS:                  Send, Count</html>");
					mtd.addColumn("sysmonitors", "sum_io_send_wait",      "<html>I/O Wait from RS:                  Send, Amount of Time (ms)</html>");
					mtd.addColumn("sysmonitors", "longest_io_send_wait",  "<html>I/O Wait from RS:                  Send, Longest Wait (ms)</html>");
					mtd.addColumn("sysmonitors", "io_recv",               "<html>I/O Wait from RS:                  Receive, Count</html>");
					mtd.addColumn("sysmonitors", "sum_io_recv_wait",      "<html>I/O Wait from RS:                  Receive, Amount of Time (ms)</html>");
					mtd.addColumn("sysmonitors", "longest_io_recv_wait",  "<html>I/O Wait from RS:                  Receive, Longest Wait (ms)</html>");
					mtd.addColumn("sysmonitors", "packets_sent",          "<html>Network Packet Information:        Packets Sent</html>");
					mtd.addColumn("sysmonitors", "full_packets_sent",     "<html>Network Packet Information:        Full Packets Sent</html>");
					mtd.addColumn("sysmonitors", "sum_packet",            "<html>Network Packet Information:        Amount of Bytes Sent</html>");
					mtd.addColumn("sysmonitors", "largest_packet",        "<html>Network Packet Information:        Largest Packet</html>");
					mtd.addColumn("sysmonitors", "log_records_scanned",   "<html>Log Scan Summary:                  Log Records Scanned</html>");
					mtd.addColumn("sysmonitors", "log_records_processed", "<html>Log Scan Summary:                  Log Records Processed</html>");
					mtd.addColumn("sysmonitors", "log_scans",             "<html>Log Scan Summary:                  Number of Log Scans</html>");
					mtd.addColumn("sysmonitors", "sum_log_scan",          "<html>Log Scan Summary:                  Amount of Time for Log Scans (ms)</html>");
					mtd.addColumn("sysmonitors", "longest_log_scan",      "<html>Log Scan Summary:                  Longest Time for Log Scan (ms)</html>");
					mtd.addColumn("sysmonitors", "open_xact",             "<html>Transaction Activity:              Opened</html>");
					mtd.addColumn("sysmonitors", "maintuser_xact",        "<html>Transaction Activity:              Maintenance User</html>");
					mtd.addColumn("sysmonitors", "commit_xact",           "<html>Transaction Activity:              Commited</html>");
					mtd.addColumn("sysmonitors", "abort_xact",            "<html>Transaction Activity:              Aborted</html>");
					mtd.addColumn("sysmonitors", "prepare_xact",          "<html>Transaction Activity:              Prepared</html>");
					mtd.addColumn("sysmonitors", "delayed_commit_xact",   "<html>Transaction Activity:              Delayed Commit</html>");
					mtd.addColumn("sysmonitors", "xupdate_processed",     "<html>Log Scan Activity:                 Updates</html>");
					mtd.addColumn("sysmonitors", "xinsert_processed",     "<html>Log Scan Activity:                 Inserts</html>");
					mtd.addColumn("sysmonitors", "xdelete_processed",     "<html>Log Scan Activity:                 Deletes</html>");
					mtd.addColumn("sysmonitors", "xexec_processed",       "<html>Log Scan Activity:                 Store Procedures</html>");
					mtd.addColumn("sysmonitors", "xcmdtext_processed",    "<html>Log Scan Activity:                 DDL Log Records</html>");
					mtd.addColumn("sysmonitors", "xwrtext_processed",     "<html>Log Scan Activity:                 Writetext Log Records</html>");
					mtd.addColumn("sysmonitors", "xrowimage_processed",   "<html>Log Scan Activity:                 Text/Image Log Records</html>");
					mtd.addColumn("sysmonitors", "xclr_processed",        "<html>Log Scan Activity:                 CLRs</html>");
					mtd.addColumn("sysmonitors", "xckpt_processed",       "<html>Log Scan Activity:                 Checkpoints Processed</html>");
					mtd.addColumn("sysmonitors", "xckpt_genxactpurge",    "<html>Log Scan Activity:                 </html>");
					mtd.addColumn("sysmonitors", "sqldml_processed",      "<html>Log Scan Activity:                 SQL Statements Processed</html>");
					mtd.addColumn("sysmonitors", "bckward_schema",        "<html>Backward Schema Lookups:           Count</html>");
					mtd.addColumn("sysmonitors", "sum_bckward_wait",      "<html>Backward Schema Lookups:           Total Wait (ms)</html>");
					mtd.addColumn("sysmonitors", "longest_bckward_wait",  "<html>Backward Schema Lookups:           Longest Wait (ms)</html>");
					mtd.addColumn("sysmonitors", "forward_schema",        "<html>Schema Cache:                      Count</html>");
					mtd.addColumn("sysmonitors", "sum_forward_wait",      "<html>Schema Cache:                      Total Wait (ms)</html>");
					mtd.addColumn("sysmonitors", "longest_forward_wait",  "<html>Schema Cache:                      Longest Wait (ms)</html>");
					mtd.addColumn("sysmonitors", "schema_reuse",          "<html>Schema Cache:                      Schemas reused</html>");
				}
				catch (NameNotFoundException e) {/*ignore*/}
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				List <String> pkCols = new LinkedList<String>();

//				if (isClusterEnabled)
//					pkCols.add("InstanceID");

				pkCols.add("dbname");

				return pkCols;
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String sql = "exec sp_asetune_ra_stats ";
				return sql;
			}
		};

		// Need stored proc 'sp_asetune_ra_stats'
		// check if it exists: if not it will be created in super.init(conn)
		tmp.addDependsOnStoredProc("sybsystemprocs", "sp_asetune_ra_stats", VersionInfo.SP_ASETUNE_RA_STATS_CRDATE, VersionInfo.class, "sp_asetune_ra_stats.sql", AseConnectionUtils.SA_ROLE);

		// If QUERY TIMEOUT is not set, set this to X second, because this can take time. 
		if (tmp.getQueryTimeout() == CountersModel.DEFAULT_sqlQueryTimeout)
			tmp.setQueryTimeout(30);

		tmp.addDependsOnCm(CM_NAME__SPINLOCK_SUM); // CMspinlockSum must have been executed before this cm
		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (AseTune.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_repagent_activity.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );
		}
		
		_CMList.add(tmp);






		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// ProcedureCache Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		
		//==================================================================================================
		// 12.5.0.3: Description of: monCachedProcedures
		// 
		// Description:
		//
		//
		// Needs Configuration:
		// none
		// 
		// Name                           Datatype     Attributes  Ind Description
		// ------------------------------ ------------ ----------- --- ------------------------------------------------------------
		// ObjectID                       int                        0 Unique identifier for the procedure
		// OwnerUID                       int                        0 Unique identifier for the database owner
		// DBID                           int                        0 Unique identifier for the database
		// PlanID                         int                        0 Unique identifier for the query plan
		// MemUsageKB                     int                        0 Number of kilobytes of memory used by the procedure
		// CompileDate                    datetime                   0 Date that the procedure was compiled
		// ObjectName                     varchar(30)                0 Name of the Procedure
		// ObjectType                     varchar(32)                0 The type of procedure, e.g. 'stored procedure', 'trigger'
		// OwnerName                      varchar(30)                0 The name of the owner of the object
		// DBName                         varchar(30)                0 Name of the database
		//---------------------------------------------------------------------------------------------------
		// Column changes in various versions:
		//
		// Version      Action Name              Datatype   Attributes Description
		// ------------ ------ ----------------- ---------- ---------- ----------------------------------
		// 15.0.1CE/15.5 add   InstanceId        int                   The Server Instance Identifier (cluster only)
		// 15.5         add    RequestCnt        int                   contains the number of times a stored procedure was executed since the plan was compiled
		// 15.5         add    TempdbRemapCnt    int                   contain the number of times an existing stored proc plan was remapped due to being executed in a different temporary database
		// 15.5         add    AvgTempdbRemapTime int                  the average time (in milliseconds) this remapping took, respectively. 
		// 15.7 (3B)    add    ExecutionCount    int                   Counter Number of times Adaptive Server executed the stored procedure plan or tree since it was cached
		// 15.7 (3B)    add    CPUTime           int                   Counter Total number of milliseconds of CPU time used
		// 15.7 (3B)    add    ExecutionTime     int                   Counter Total amount of elapsed time (in milliseconds) Adaptive Server spent executing the stored procedure plan or tree
		// 15.7 (3B)    add    PhysicalReads     int                   Counter Number of physical reads performed
		// 15.7 (3B)    add    LogicalReads      int                   Counter Number of pages read
		// 15.7 (3B)    add    PhysicalWrites    int                   Counter Number of physical writes performed
		// 15.7 (3B)    add    PagesWritten      int                   Counter Number of pages written
		//---------------------------------------------------------------------------------------------------

		name         = CM_NAME__CACHED_PROC;
		displayName  = CM_DESC__CACHED_PROC;
		description  = "What Objects is located in the 'procedure cache'.";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 0;
		needCeVersion= 0;
		monTables    = new String[] {"monCachedProcedures"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"per object statistics active=1", "statement statistics active=1"};
		colsCalcDiff = new String[] {"RequestCnt", "TempdbRemapCnt", "ExecutionCount", "CPUTime", "ExecutionTime", "PhysicalReads", "LogicalReads", "PhysicalWrites", "PagesWritten"};
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
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				if (srvVersion >= 15700)
					return new String[] {"per object statistics active=1", "statement statistics active=1"};

				return new String[] {"per object statistics active=1"};
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				List <String> pkCols = new LinkedList<String>();

				if (isClusterEnabled)
					pkCols.add("InstanceID");

				pkCols.add("PlanID");

				return pkCols;
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String cols = "";

				String orderBy = "order by DBName, ObjectName, ObjectType \n";

				// ASE cluster edition
				String InstanceID = "";

				// ASE 15.5 or (15.0.3 in cluster edition) 
				String RequestCnt         = "";
				String TempdbRemapCnt     = "";
				String AvgTempdbRemapTime = "";
				String ase1550_nl         = "";

				// ASE 15.7
				String ExecutionCount = "";
				String CPUTime        = "";
				String ExecutionTime  = "";
				String PhysicalReads  = "";
				String LogicalReads   = "";
				String PhysicalWrites = "";
				String PagesWritten   = "";
				String ase1570_nl     = "";

				if (isClusterEnabled)
				{
					InstanceID = "InstanceID, ";
				}

				if (aseVersion >= 15500 || (aseVersion >= 15030 && isClusterEnabled) )
				{
					orderBy = "order by RequestCnt desc \n";

					RequestCnt         = "RequestCnt, ";
					TempdbRemapCnt     = "TempdbRemapCnt, ";
					AvgTempdbRemapTime = "AvgTempdbRemapTime, ";
					ase1550_nl         = "\n";
				}

				if (aseVersion >= 15700)
				{
					ExecutionCount = "ExecutionCount, ";
					CPUTime        = "CPUTime, ";
					ExecutionTime  = "ExecutionTime, ";
					PhysicalReads  = "PhysicalReads, ";
					LogicalReads   = "LogicalReads, ";
					PhysicalWrites = "PhysicalWrites, ";
					PagesWritten   = "PagesWritten, ";
					ase1570_nl     = "\n";
				}
				
				cols = 
					InstanceID + 
					"PlanID, DBName, ObjectName, ObjectType, MemUsageKB, CompileDate, " +
					ase1550_nl + RequestCnt + TempdbRemapCnt + AvgTempdbRemapTime +
					ase1570_nl + ExecutionCount + CPUTime + ExecutionTime + PhysicalReads + LogicalReads + PhysicalWrites + PagesWritten +
					"";

				// remove last comma
				cols = StringUtil.removeLastComma(cols);

				String sql = 
					"select " + cols + "\n" +
					"from master..monCachedProcedures \n" +
					orderBy;

				return sql;
			}


			/** 
			 * Get number of rows to save/request ddl information for 
			 */
			@Override
			public int getMaxNumOfDdlsToPersist()
			{
				return 10;
			}
			@Override
			public String[] getDdlDetailsSortOnColName()
			{
				if (getServerVersion() < 15500)
					return null;

				String[] sa = {"RequestCnt"};
				return sa;
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (AseTune.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_cached_procedures_activity.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );

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

		//==================================================================================================
		// 12.5.0.3: Description of: monProcedureCache
		// 
		// Description:
		//
		//
		// Needs Configuration:
		// none
		// 
		// Name                           Datatype     Attributes  Ind Description
		// ------------------------------ ------------ ----------- --- ------------------------------------------------------------
		// Requests                       int                        3 Number of stored procedures requested
		// Loads                          int                        3 Number of stored procedures loaded into cache
		// Writes                         int                        3 Number of times a procedure was normalized and the tree written back to sysprocedures
		// Stalls                         int                        3 Number of times a process had to wait for a free procedure cache buffer when installing a stored procedure into cache
		//---------------------------------------------------------------------------------------------------
		// Column changes in various versions:
		//
		// Version      Action Name              Datatype   Attributes Description
		// ------------ ------ ----------------- ---------- ---------- ----------------------------------
		// 15.0.1CE/15.5 add   InstanceId        int                   The Server Instance Identifier (cluster only)
		//---------------------------------------------------------------------------------------------------
		
		name         = CM_NAME__PROC_CACHE_LOAD;
		displayName  = CM_DESC__PROC_CACHE_LOAD;
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
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return new String[] {"enable monitoring=1"};
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				List <String> pkCols = new LinkedList<String>();

				if (isClusterEnabled)
					pkCols.add("InstanceID");

				// NONE, when not Cluster Edition

				return pkCols;
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				cols2 += "Requests, Loads, Writes, Stalls";
				if (isClusterEnabled)
				{
					cols1 = "InstanceID, ";
				}

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from master..monProcedureCache \n";

				return sql;
			}

			@Override
			public void updateGraphData(TrendGraphDataPoint tgdp)
			{
				if (CM_GRAPH_NAME__PROC_CACHE_LOAD__REQUEST_PER_SEC.equals(tgdp.getName()))
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
		tmp.addTrendGraphData(CM_GRAPH_NAME__PROC_CACHE_LOAD__REQUEST_PER_SEC, new TrendGraphDataPoint(CM_GRAPH_NAME__PROC_CACHE_LOAD__REQUEST_PER_SEC, new String[] { "Requests", "Loads" }));
		if (AseTune.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_procedure_cache_activity.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );

			// GRAPH
			TrendGraph tg = new TrendGraph(CM_GRAPH_NAME__PROC_CACHE_LOAD__REQUEST_PER_SEC,
					// Menu Checkbox text
					"Procedure Cache Requests",
					// Label 
					"Number of Procedure Requests per Second (procs,triggers,views)", // Label 
					new String[] { "Requests", "Loads" }, 
					false, tmp, true, -1);
			tmp.addTrendGraph(tg.getName(), tg, true);
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
		// Description:
		//
		//
		// Needs Configuration:
		// none
		// 
		// Name                           Datatype     Attributes  Ind Description
		// ------------------------------ ------------ ----------- --- ------------------------------------------------------------
		// SPID                           smallint                   0 Session Process Identifier
		// KPID                           int                        0 Kernel Process Identifier
		// DBID                           int                        0 Unique identifier for the database
		// OwnerUID                       int                        0 Unique identifier for the object owner
		// ObjectID                       int                        0 Unique identifier for the procedure
		// PlanID                         int                        0 Unique identifier for the query plan
		// MemUsageKB                     int                        0 Number of kilobytes of memory used by the procedure
		// CompileDate                    datetime                   0 Date that the procedure was compiled
		// ContextID                      int                        0 The stack frame of the procedure
		// DBName                         varchar(30)                0 Name of the database the object resides in
		// OwnerName                      varchar(30)                0 The name of the owner of the object
		// ObjectName                     varchar(30)                0 Name of the procedure
		// ObjectType                     varchar(32)                0 The type of procedure, e.g. 'stored procedure', 'trigger'
		//---------------------------------------------------------------------------------------------------
		// Column changes in various versions:
		//
		// Version      Action Name              Datatype   Attributes Description
		// ------------ ------ ----------------- ---------- ---------- ----------------------------------
		// 12.5.3       add    LineNumber        int                   The line in the procedure currently being executed
		// 15.0.1CE/15.5 add   InstanceId        int                   The Server Instance Identifier (cluster only)
		// 15.0.2 esd#5 add    StatementNumber   int                   the statement in the stored procedure currently being executed 
		// 15.7 (3B)    add    ExecutionCount    int           Counter Number of times Adaptive Server executed this instance of the stored procedure held in the procedure cache
		// 15.7 (3B)    add    CPUTime           int           Counter The amount of CPU time (in milliseconds) that Adaptive Server spent executing the instance of this stored procedure held in the procedure cache
		// 15.7 (3B)    add    ExecutionTime     int           Counter Total amount of time (in milliseconds) Adaptive Server spent executing the instance of this stored procedure held in the procedure cache
		// 15.7 (3B)    add    PhysicalReads     int           Counter Number of physical reads performed by the instance of this stored procedure held in the procedure cache
		// 15.7 (3B)    add    LogicalReads      int           Counter Number of logical reads performed by the instance of this stored procedure held in the procedure cache
		// 15.7 (3B)    add    PhysicalWrites    int           Counter Number of physical writes performed by the instance of this stored procedure held in the procedure cache
		// 15.7 (3B)    add    PagesWritten      int           Counter Number of pages read by the instance of this stored procedure held in the procedure cache
		//---------------------------------------------------------------------------------------------------

		name         = CM_NAME__PROC_CALL_STACK;
		displayName  = CM_DESC__PROC_CALL_STACK;
		description  = "<html>" +
							"Nesting levels of currenty executed procedures, this can be used as a light 'profiler'" +
							"<br><br>" +
							"Table Background colors:" +
							"<ul>" +
							"    <li>GREEN - Procedure/Trigger/View that is currently executing (end of the stack).</li>" +
							"</ul>" +
						"</html>";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 0;
		needCeVersion= 0;
		monTables    = new String[] {"monProcessProcedures"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring=1", "statement statistics active=1"};
		colsCalcDiff = new String[] {};// need to test for 15.7: {"ExecutionCount", "CPUTime", "ExecutionTime", "PhysicalReads", "LogicalReads", "PhysicalWrites", "PagesWritten"};
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
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				if (srvVersion >= 15700)
					return new String[] {"enable monitoring=1", "statement statistics active=1"};

				return new String[] {"enable monitoring=1"};
			}

			@Override
			public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				// this would mean NO PK and NO DIFF
				return null;
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				String InstanceID      = "";
				String StatementNumber = "";
				String LineNumber      = "";
				// ase 15.7
				String ExecutionCount = "";
				String CPUTime        = "";
				String ExecutionTime  = "";
				String PhysicalReads  = "";
				String LogicalReads   = "";
				String PhysicalWrites = "";
				String PagesWritten   = "";
				String ase1570_nl     = "";

				if (isClusterEnabled)    InstanceID      = "InstanceID, ";
				if (aseVersion >= 12530) LineNumber      = "LineNumber, ";
				if (aseVersion >= 15025) StatementNumber = "StatementNumber, ";
				if (aseVersion >= 15700)
				{
					ExecutionCount = "ExecutionCount, ";
					CPUTime        = "CPUTime, ";
					ExecutionTime  = "ExecutionTime, ";
					PhysicalReads  = "PhysicalReads, ";
					LogicalReads   = "LogicalReads, ";
					PhysicalWrites = "PhysicalWrites, ";
					PagesWritten   = "PagesWritten, ";
					ase1570_nl     = "\n";
				}

				cols1 += "SPID, " + InstanceID + "DBName, OwnerName, ObjectName, \n" + 
				         LineNumber + StatementNumber + "ContextID, ObjectType, \n" +
				         ExecutionCount + CPUTime + ExecutionTime + PhysicalReads + LogicalReads + PhysicalWrites + PagesWritten + ase1570_nl +
				         "PlanID, MemUsageKB, CompileDate, KPID, DBID, OwnerUID, ObjectID, \n" +
				         "MaxContextID = convert(int, -1)";
				//"MaxContextID = (select max(ContextID) from master..monProcessProcedures i where o.SPID = i.SPID)";

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from master..monProcessProcedures o\n" +
					"order by SPID, ContextID desc\n" +
					"";

				return sql;
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
					if      (colName.equals("SPID"))         pos_SPID         = colId;
					else if (colName.equals("ContextID"))    pos_ContextID    = colId;
					else if (colName.equals("MaxContextID")) pos_MaxContextID = colId;

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
			} // end: localCalculation


			/** 
			 * Get number of rows to save/request ddl information for 
			 */
			@Override
			public int getMaxNumOfDdlsToPersist()
			{
				return Integer.MAX_VALUE; // Basically ALL Rows
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (AseTune.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_procedure_call_stack_activity.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );

//			tcp.setTableToolTipText(
//				    "<html>" +
//				        "Background colors:" +
//				        "<ul>" +
//				        "    <li>GREEN - Procedure/Trigger/View that is currently executing (end of the stack).</li>" +
//				        "</ul>" +
//				    "</html>");

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
		
		//==================================================================================================
		// 12.5.0.3: Description of: monCachedObject
		// 
		// Description:
		//
		//
		// Needs Configuration:
		// none
		// 
		// Name                           Datatype     Attributes  Ind Description
		// ------------------------------ ------------ ----------- --- ------------------------------------------------------------
		// CacheID                        int                        0 Unique identifier for the cache
		// ObjectID                       int                        0 Unique identifier for the object
		// IndexID                        int                        0 Unique identifier for the index
		// DBID                           int                        0 Unique identifier for the database
		// OwnerUserID                    int                        0 Unique identifier for the user who owns the object
		// CachedKB                       int                        0 Number of kilobytes of the cache that the object is occupying
		// ProcessesAccessing             int                        0 Number of processes currently accessing the object
		// CacheName                      varchar(30)                0 Name of the cache
		// DBName                         varchar(30)                0 Name of the database
		// OwnerName                      varchar(30)                0 Name of the user who owns the object
		// ObjectName                     varchar(30)                0 Name of the object
		// ObjectType                     varchar(30)                0 The type of the object
		//---------------------------------------------------------------------------------------------------
		// Column changes in various versions:
		//
		// Version      Action Name              Datatype   Attributes Description
		// ------------ ------ ----------------- ---------- ---------- ----------------------------------
		// 15.0         add    PartitionID       int                   Unique identifier for the partition. This is the same as ObjectID for non-partitioned objects.
		// 15.0         add    PartitionName     varchar(30)           Name of the object partition (will be NULL if the partition is no longer open)
		// 15.0         add    TotalSizeKB       int                   Partition size in kilobytes (KB)
		// 15.0.1CE/15.5 add   InstanceId        int                   The Server Instance Identifier (cluster only)
		//---------------------------------------------------------------------------------------------------
		
		name         = CM_NAME__CACHED_OBJECTS;
		displayName  = CM_DESC__CACHED_OBJECTS;
		description  = "";
		description  = "<html>" +
							"<p>What Tables is located in the 'data cache' and to what cache is it bound?</p>" +
							"<b>This could be a bit heavy to sample, so use with caution.</b><br>" +
							"Tip:<br>" +
							"<ul>" +
							"    <li>Use ABSolute counter values to check how many MB the table are using.</li>" +
							"    <li>Use Diff or Rate to check if the memory print is increasing or decreasing.</li>" +
							"    <li>Use the checkpox 'Pause Data Polling' wait for a minute, then enable polling again, that will give you a longer sample period!</li>" +
							"</ul>" +
							"<br><br>" +
							"Table Background colors:" +
							"<ul>" +
							"    <li>ORANGE - An Index.</li>" +
							"</ul>" +
						"</html>";
		
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
				false, true, 600) // "CachedKB", "TotalSizeKB" goes UP and DOWN: so wee want negative count values
		// OVERRIDE SOME DEFAULT METHODS
		{
			private static final long serialVersionUID = 1725558024113511209L;

			@Override
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return null;
			}

			@Override
			public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				List <String> pkCols = new LinkedList<String>();

				if (isClusterEnabled)
					pkCols.add("InstanceID");

				pkCols.add("DBID");
				pkCols.add("ObjectID");
				pkCols.add("IndexID");

				if (aseVersion >= 15000)
					pkCols.add("PartitionID");

				return pkCols;
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				if (isClusterEnabled)
				{
					cols1 += "InstanceID, ";
				}

				cols1 += "DBID, ObjectID, IndexID, DBName,  ObjectName, ObjectType, \n";
				cols2 += "";
				cols3 += "CachedKB, CacheName";
				if (aseVersion >= 15000)
				{
					cols2 += "PartitionID, PartitionName, TotalSizeKB, \n";
				}

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from master..monCachedObject \n" +
					"order by CachedKB desc";

				return sql;
			}

			/** 
			 * Get number of rows to save/request ddl information for 
			 */
			@Override
			public int getMaxNumOfDdlsToPersist()
			{
				return 10;
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);

		// If POSTPONE time is not set, set this to 10 minutes (600 sec), because this is HEAVY 
		if (tmp.getPostponeTime() == 0)
			tmp.setPostponeTime(600, false);

		// If QUERY TIMEOUT is not set, set this to X second, because this can take time. 
		if (tmp.getQueryTimeout() == CountersModel.DEFAULT_sqlQueryTimeout)
			tmp.setQueryTimeout(30);

		if (AseTune.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_objects_in_cache_activity.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );

//			tcp.setTableToolTipText(
//				    "<html>" +
//				        "Background colors:" +
//				        "<ul>" +
//				        "    <li>ORANGE - An Index.</li>" +
//				        "</ul>" +
//				    "</html>");

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

		//==================================================================================================
		// 12.5.0.3: Description of: monErrorLog
		// 
		// Description:
		//
		//
		// Needs Configuration:
		// none
		// 
		// Name                           Datatype     Attributes  Ind Description
		// ------------------------------ ------------ ----------- --- ------------------------------------------------------------
		// SPID                           smallint                   0 Session Process Identifier
		// KPID                           int                        0 Kernel Process Identifier
		// FamilyID                       smallint                   0 SPID of the parent process
		// EngineNumber                   smallint                   0 Engine on which process was running
		// ErrorNumber                    int                        0 Error message number
		// Severity                       int                        0 Severity of error
		// Time                           datetime                   0 Timestamp when error occurred
		// ErrorMessage                   varchar(512)               0 Text of the error message
		//---------------------------------------------------------------------------------------------------
		// Column changes in various versions:
		//
		// Version      Action Name              Datatype   Attributes Description
		// ------------ ------ ----------------- ---------- ---------- ----------------------------------
		// 12.5.1       add    State             int                   State of error
		// 15.0.1CE/15.5 add   InstanceId        int                   The Server Instance Identifier (cluster only)
		//---------------------------------------------------------------------------------------------------
		
		name         = CM_NAME__ERRORLOG;
		displayName  = CM_DESC__ERRORLOG;
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
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return new String[] {"enable monitoring=1", "errorlog pipe active=1", "errorlog pipe max=200"};
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				String instanceId = "";
				if (isClusterEnabled)
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
					"from master..monErrorLog\n";

				return sql;
			}
		};
	
		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (AseTune.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_errorlog_activity.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );
		}

		_CMList.add(tmp);





		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// DeadLock Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------

		//==================================================================================================
		// 12.5.0.3: Description of: monDeadLock
		// 
		// Description:
		//
		//
		// Needs Configuration:
		// none
		// 
		// Name                           Datatype     Attributes  Ind Description
		// ------------------------------ ------------ ----------- --- ------------------------------------------------------------
		// DeadlockID                     int                        0 Unique identifier for the deadlock
		// VictimKPID                     int                        0 KPID of the victim process for the deadlock
		// ResolveTime                    datetime                   0 Time at which the deadlock was resolved
		// ObjectDBID                     int                        0 Unique database identifier for database where the object resides
		// PageNumber                     int                        0 Page number for which the lock was requested, if applicable
		// RowNumber                      int                        0 Row number for which the lock was requested, if applicable
		// HeldFamilyID                   smallint                   0 SPID of the parent process of the process holding the lock
		// HeldSPID                       smallint                   0 SPID of process holding the lock
		// HeldKPID                       int                        0 KPID of process holding the lock
		// HeldProcDBID                   int                        0 Unique identifier for the database where the stored procedure that caused the lock to be held resides, if applicable
		// HeldProcedureID                int                        0 Unique object identifier for the stored procedure that caused the lock to be held, if applicable
		// HeldBatchID                    int                        0 Unique batch identifier for the SQL being executed by the process holding the lock when it was blocked by another process (not when it acquired the lock)
		// HeldContextID                  int                        0 Unique context identifier for the process holding the lock when it was blocked by another process (not when it acquired the lock)
		// HeldLineNumber                 int                        0 Line number within the batch of the statement being executed by the process holding the lock when it was blocked by another process (not when it acquired the lock)
		// WaitFamilyID                   smallint                   0 SPID of the parent process of the process waiting for the lock
		// WaitSPID                       smallint                   0 SPID of the process waiting for the lock
		// WaitKPID                       int                        0 KPID of the process waiting for the lock
		// WaitTime                       int                        0 Amount of time in milliseconds that the waiting process was blocked before the deadlock was resolved
		// ObjectName                     varchar(30)                0 Name of the object
		// HeldUserName                   varchar(30)                0 Name of the user for whom the lock is being held
		// HeldApplName                   varchar(30)                0 Name of the application holding the lock
		// HeldTranName                   varchar(255)               0 The name of the transaction in which the lock was acquired
		// HeldLockType                   varchar(20)                0 The type of lock being held
		// HeldCommand                    varchar(30)                0 Category of process or command that the process was executing when it was blocked
		// WaitUserName                   varchar(30)                0 Name of the user for whom the lock is being requested
		// WaitLockType                   varchar(20)                0 The type of lock requested
		//---------------------------------------------------------------------------------------------------
		// Column changes in various versions:
		//
		// Version      Action Name              Datatype   Attributes Description
		// ------------ ------ ----------------- ---------- ---------- ----------------------------------
		// 15.0.1CE/15.5 add   InstanceId        int                   The Server Instance Identifier (cluster only)
		// 15.0.2       add    HeldSourceCodeID  varchar(30)           Location in the source code at which the owning lock was acquired. For internal use only
		// 15.0.2       add    WaitSourceCodeID  varchar(30)           Location in the source code at which the waiting lock was requested. For internal use only
		//---------------------------------------------------------------------------------------------------
		
		name         = CM_NAME__DEADLOCK;
		displayName  = CM_DESC__DEADLOCK;
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
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return new String[] {"enable monitoring=1", "deadlock pipe active=1", "deadlock pipe max=500"};
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
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
					"from master..monDeadLock\n";

				return sql;
			}
		};
	
		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (AseTune.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_deadlock_activity.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );
		}

		_CMList.add(tmp);

		




		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// Lock Timeout Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		
		// FIXME: fix table description
		
		name         = CM_NAME__LOCK_TIMEOUT;
		displayName  = CM_DESC__LOCK_TIMEOUT;
		description  = "What SQL Statements caused a lock timeout";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 15700;
		needCeVersion= 0;
		monTables    = new String[] {"monLockTimeout"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring=1", "lock timeout pipe active=1", "lock timeout pipe max messages=500"};

		tmp = new CountersModelAppend( name, null, monTables, needRole, needConfig, needVersion, needCeVersion, true)
		// OVERRIDE SOME DEFAULT METHODS
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return new String[] {"enable monitoring=1", "lock timeout pipe active=1", "lock timeout pipe max messages=500"};
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				cols1 = "*";
				cols2 = "";
				cols3 = "";

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from master..monLockTimeout\n";

				return sql;
			}
		};
	
		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (AseTune.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_lock_timeout_activity.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );
		}

		_CMList.add(tmp);

		




		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// proc Cache Module Usage Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		
		// FIXME: fix table description
		
		name         = CM_NAME__PROC_CACHE_MODULE_USAGE;
		displayName  = CM_DESC__PROC_CACHE_MODULE_USAGE;
		description  = "What module of the ASE Server is using the 'procedure cache' or 'dynamic memory pool'";
		
		needVersion  = 15010; // monProcedureCache*Usage where intruduced in 15.0.1
		needCeVersion= 0;
		monTables    = new String[] {"monProcedureCacheModuleUsage"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {};
		colsCalcDiff = new String[] {"ActiveDiff", "NumPagesReused"};
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
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return null;
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				List <String> pkCols = new LinkedList<String>();

				if (isClusterEnabled)
					pkCols.add("InstanceID");

				pkCols.add("ModuleID");

				return pkCols;
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String InstanceID = "";
				if (isClusterEnabled && aseVersion >= 15500)
				{
					InstanceID = "InstanceID, ";
				}

				String sql = 
					"select "+InstanceID+"ModuleName, ModuleID, Active, ActiveDiff = Active, HWM, NumPagesReused \n" +
					"from master..monProcedureCacheModuleUsage\n" +
					"order by ModuleID";

				return sql;
			}

			@Override
			public void updateGraphData(TrendGraphDataPoint tgdp)
			{
				if (CM_GRAPH_NAME__PROC_CACHE_MODULE_USAGE__ABS_USAGE.equals(tgdp.getName()))
				{
					// Write 1 "line" for every database
					Double[] dArray = new Double[this.size()];
					String[] lArray = new String[dArray.length];
					for (int i = 0; i < dArray.length; i++)
					{
						lArray[i] = this.getAbsString       (i, "ModuleName");
						dArray[i] = this.getAbsValueAsDouble(i, "Active");
					}

					// Set the values
					tgdp.setDate(this.getTimestamp());
					tgdp.setLabel(lArray);
					tgdp.setData(dArray);
				}
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		tmp.addTrendGraphData(CM_GRAPH_NAME__PROC_CACHE_MODULE_USAGE__ABS_USAGE, new TrendGraphDataPoint(CM_GRAPH_NAME__PROC_CACHE_MODULE_USAGE__ABS_USAGE, new String[] { "runtime-replaced" }));
		if (AseTune.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_proc_cache_module_usage.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );

			// GRAPH
			TrendGraph tg = new TrendGraph(CM_GRAPH_NAME__PROC_CACHE_MODULE_USAGE__ABS_USAGE,
					"Procedure Cache Module Usage", // Menu Checkbox text
					"Procedure Cache Module Usage (in page count)", // Label on the graph
					new String[] { "runtime-replaced" }, 
					false, tmp, false, 300);
			tmp.addTrendGraph(tg.getName(), tg, true);
		}
		
		_CMList.add(tmp);





		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// proc Cache Memory Usage Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		
		// FIXME: fix table description
		
		name         = CM_NAME__PROC_CACHE_MEMORY_USAGE;
		displayName  = CM_DESC__PROC_CACHE_MEMORY_USAGE;
		description  = "What module and what 'part' of the modules are using the 'procedure cache' or 'dynamic memory pool'.";
		
		needVersion  = 15010; // monProcedureCache*Usage where intruduced in 15.0.1
		needCeVersion= 0;
		monTables    = new String[] {"monProcedureCacheMemoryUsage", "monProcedureCacheModuleUsage"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {};
		colsCalcDiff = new String[] {"ActiveDiff", "NumReuseCaused"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList<String>();
		     pkList.add("ModuleID");
		     pkList.add("AllocatorID");


		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				false, true)  // "Active", "NumReuseCaused" goes UP and DOWN: so wee want negative count values
		{
			private static final long serialVersionUID = -5474676196893459453L;

			@Override
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return null;
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				List <String> pkCols = new LinkedList<String>();

				if (isClusterEnabled)
					pkCols.add("InstanceID");

				pkCols.add("ModuleID");
				pkCols.add("AllocatorID");

				return pkCols;
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String optGoalPlan = "";
				if (aseVersion >= 15020)
				{
					optGoalPlan = "plan '(use optgoal allrows_dss)' \n";
				}

				//String cols1, cols2, cols3;
				//cols1 = cols2 = cols3 = "";

				String InstanceID     = "";
				String InstanceIDJoin = "";
				if (isClusterEnabled && aseVersion >= 15500)
				{
					InstanceID     = "M.InstanceID, ";
					InstanceIDJoin = "  and M.InstanceID *= C.InstanceID \n";
				}

				String sql = 
					"select M.ModuleName, "+InstanceID+"M.ModuleID, \n" +
					"       AllocatorName = isnull(C.AllocatorName, '-AT-MODULE-LEVEL-'), \n" +
					"       AllocatorID   = isnull(C.AllocatorID,   -1), \n" +
					"       Active        = isnull(C.Active,        M.Active), \n" +
					"       ActiveDiff    = isnull(C.Active,        M.Active), \n" +
					"       HWM           = isnull(C.HWM,           M.HWM), \n" +
					"       ChunkHWM      = isnull(C.ChunkHWM,      -1), \n" +
					"       NumReuseCaused= isnull(C.NumReuseCaused,-1) \n" +
					"from master..monProcedureCacheModuleUsage M, master..monProcedureCacheMemoryUsage C \n" +
					"where M.ModuleID *= C.ModuleID \n" +
					InstanceIDJoin +
					"order by M.ModuleID, M.AllocatorID \n" +
					optGoalPlan;

				return sql;
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (AseTune.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_proc_cache_memory_usage.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );
		}
		
		_CMList.add(tmp);




		
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// Statement Cache Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------

		// FIXME: fix table description

		//==================================================================================================
		// 15.0.2: monStatementCache  (new in 15.0.2)
		// 
		// Description:
		//
		//
		// Needs Configuration: 'enable stmt cache monitoring'
		// none
		// 
		// Name                           Datatype     Attributes  Ind Description
		// ------------------------------ ------------ ----------- --- ------------------------------------------------------------
		// x                              x            x             x x
		//---------------------------------------------------------------------------------------------------
		// Column changes in various versions:
		//
		// Version      Action Name              Datatype   Attributes Description
		// ------------ ------ ----------------- ---------- ---------- ----------------------------------
		// none so far
		//---------------------------------------------------------------------------------------------------

		//==================================================================================================
		// 15.0.2: Description of: monStatementCache
		// 
		// Provides server-wide aggregated details on all entries in the statement cache.
		//
		// Note: Enable the "enable monitoring" and "enable statement cache monitoring" 
		// configuration parameters, and set the "statement cache size" parameter greater
		// than 0 for this monitoring table to collect data. 
		// 
		// ColumnName                     TypeName  Description
		// ------------------------------ --------- ---------------------------------------------------------------------------
		// TotalSizeKB                    int       Capacity of the statement cache.
		// UsedSizeKB                     int       Amount of cache in use.
		// NumStatements                  int       Number of statements in the cache.
		// NumSearches                    int       Number of statements searched for in the cache.
		// HitCount                       int       Hit count for the statements searched for in the cache.
		// NumInserts                     int       Number of statements inserted into the cache.
		// NumRemovals                    int       Number of statements removed from the cache.
		// NumRecompilesSchemaChanges     int       Number of recompiles due to schema changes in any of the tables referred to
		// NumRecompilesPlanFlushes       int       Number of recompiles due to the plan getting flushed out from the cache.
		//---------------------------------------------------------------------------------------------------
		// Column changes in various versions:
		//
		// Version     Action  Name            Description
		// ----------- ------- --------------- ---------------------------------
		// 15.0.2(CE)  added   InstanceID      Cluster instance ID
		// 15.5        added   InstanceID      Cluster instance ID
		//---------------------------------------------------------------------------------------------------
		
		name         = CM_NAME__STATEMENT_CACHE;
		displayName  = CM_DESC__STATEMENT_CACHE;
		description  = "Get overall statistics on the whole statement cache.";
		
		needVersion  = 15020; // monStatementCache where intruduced in 15.0.2
		needCeVersion= 0;
		monTables    = new String[] {"monStatementCache"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring=1", "enable stmt cache monitoring=1", "statement cache size"}; // NO default for 'statement cache size' configuration
		colsCalcDiff = new String[] {"NumStatementsDiff", "NumSearches", "HitCount", "NumInserts", "NumRemovals", "NumRecompilesSchemaChanges", "NumRecompilesPlanFlushes"};
		colsCalcPCT  = new String[] {"CacheHitPct", "OveralAvgReusePct"};
		pkList       = new LinkedList<String>();
		     pkList.add("TotalSizeKB");

		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				false, true) // true|false i'm not sure
		{
			private static final long serialVersionUID = -9084781196460687664L;

			@Override
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return new String[] {"enable monitoring=1", "enable stmt cache monitoring=1", "statement cache size"}; // NO default for 'statement cache size' configuration
			}

			@Override
			public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				try 
				{
					MonTablesDictionary mtd = MonTablesDictionary.getInstance();
					mtd.addColumn("monStatementCache",  "UnusedSizeKB", "<html>" +
							"Number of KB that is free for usage by any statement.<br>" +
							"<b>Formula</b>: abs.TotalSizeKB - abs.UsedSizeKB<br></html>");
					mtd.addColumn("monStatementCache",  "AvgStmntSizeInKB", "<html>" +
							"Average KB that each compiled SQL Statement are using.<br>" +
							"<b>Formula</b>: abs.UsedSizeKB / abs.NumStatements<br></html>");
					mtd.addColumn("monStatementCache",  "NumStatementsDiff", "<html>" +
							"Simply the difference count from previous sample of 'NumStatements'.<br>" +
							"<b>Formula</b>: this.NumStatements - previous.NumStatements<br></html>");
					mtd.addColumn("monStatementCache",  "CacheHitPct", "<html>" +
							"Percent of Statements that already was in the Statement Cache<br>" +
							"<b>Formula</b>: diff.HitCount / diff.NumSearches * 100 <br></html>");
//					mtd.addColumn("monStatementCache",  "OveralAvgReusePct", "<html>" +
//							"A good indication of overall average reuse for each statement. A 10:1 ratio obviously is much better than 2:1<br>" +
//							"<b>Formula</b>: diff.HitCount / diff.NumStatements * 100 <br>" +
//							"<b>Note</b>: The sampling interval plays a huge role in this metric – during a 1 second sample, not that many statements <br>" +
//							"             may be executed as compared to a 10 minute sample – and could distort the ratio to be viewed as excessively low.<br></html>");
				}
				catch (NameNotFoundException e) {/*ignore*/}
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				List <String> pkCols = new LinkedList<String>();

				if (isClusterEnabled)
					pkCols.add("InstanceID");

				pkCols.add("TotalSizeKB");

				return pkCols;
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String sql = 
					"SELECT \n" +
					(isClusterEnabled ? "  InstanceID, \n" : "") +
					"  TotalSizeKB, \n" +
					"  UsedSizeKB, \n" +
					"  UnusedSizeKB      = TotalSizeKB - UsedSizeKB, \n" +
					"  AvgStmntSizeInKB  = CASE WHEN NumStatements>0 THEN UsedSizeKB / NumStatements ELSE 0 END, \n" +
					"  NumStatements, \n" +
					"  NumStatementsDiff = NumStatements, \n" +
					"  CacheHitPct       = CASE WHEN NumSearches  >0 \n" +
					"                           THEN convert(numeric(10,1), ((HitCount+0.0)/(NumSearches+0.0))  *100.0 ) \n" +
					"                           ELSE convert(numeric(10,1), 0) \n" +
					"                      END, \n" +
//					"  OveralAvgReusePct = CASE WHEN NumStatements>0 THEN convert(numeric(10,1), ((HitCount+0.0)/(NumStatements+0.0))*100.0 ) ELSE convert(numeric(10,1), 0) END, \n" +
					"  NumSearches, \n" +
					"  HitCount, \n" +
					"  NumInserts, \n" +
					"  NumRemovals, \n" +
					"  NumRecompilesSchemaChanges, \n" +
					"  NumRecompilesPlanFlushes \n" +
					"FROM master..monStatementCache \n";

				return sql;
			}

			/** 
			 * Compute the CacheHitPct for DIFF values
			 */
			public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
			{
				int CacheHitPctId = -1, OveralAvgReusePctId = -1;

				int HitCount,        NumSearches;//,        NumStatementsDiff;
				int HitCountId = -1, NumSearchesId = -1;//, NumStatementsDiffId = -1;

				// Find column Id's
				List<String> colNames = diffData.getColNames();
				if (colNames == null)
					return;

				for (int colId=0; colId < colNames.size(); colId++) 
				{
					String colName = colNames.get(colId);
					if      (colName.equals("HitCount"))          HitCountId          = colId;
					else if (colName.equals("NumSearches"))       NumSearchesId       = colId;
//					else if (colName.equals("NumStatementsDiff")) NumStatementsDiffId = colId;
					else if (colName.equals("CacheHitPct"))       CacheHitPctId       = colId;
					else if (colName.equals("OveralAvgReusePct")) OveralAvgReusePctId = colId;

					// Noo need to continue, we got all our columns
					if (    HitCountId          >= 0 
					     && NumSearchesId       >= 0 
//					     && NumStatementsDiffId >= 0  
					     && CacheHitPctId       >= 0  
					     && OveralAvgReusePctId >= 0  
					   )
						break;
				}

				// Loop on all diffData rows
				for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
				{
					HitCount          = ((Number)diffData.getValueAt(rowId, HitCountId         )).intValue();
					NumSearches       = ((Number)diffData.getValueAt(rowId, NumSearchesId      )).intValue();
//					NumStatementsDiff = ((Number)diffData.getValueAt(rowId, NumStatementsDiffId)).intValue();

					//---- CacheHitPct
					int colPos = CacheHitPctId;
					if (NumSearches > 0)
					{
						double calc = ((HitCount+0.0) / (NumSearches+0.0)) * 100.0;

						BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);
						diffData.setValueAt(newVal, rowId, colPos);
					}
					else
						diffData.setValueAt(new BigDecimal(0).setScale(1, BigDecimal.ROUND_HALF_EVEN), rowId, colPos);

//					//---- OveralAvgReusePct
//					colPos = OveralAvgReusePctId;
//					if (NumStatementsDiff > 0)
//					{
//						double calc = ((HitCount+0.0) / (NumStatementsDiff+0.0)) * 100.0;
//
//						BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//						diffData.setValueAt(newVal, rowId, colPos);
//					}
//					else
//						diffData.setValueAt(new BigDecimal(0).setScale(1, BigDecimal.ROUND_HALF_EVEN), rowId, colPos);
				}
			}

			@Override
			public void updateGraphData(TrendGraphDataPoint tgdp)
			{
				if (CM_GRAPH_NAME__STATEMENT_CACHE__REQUEST_PER_SEC.equals(tgdp.getName()))
				{
					Double[] arr = new Double[4];

					arr[0] = this.getRateValueSum("NumSearches");
					arr[1] = this.getRateValueSum("HitCount");
					arr[1] = this.getRateValueSum("NumInserts");
					arr[1] = this.getRateValueSum("NumRemovals");
					_logger.debug("updateGraphData(StatementCacheGraph): NumSearches='"+arr[0]+"', HitCount='"+arr[1]+"', NumInserts='"+arr[2]+"', NumRemovals='"+arr[3]+"'.");

					// Set the values
					tgdp.setDate(this.getTimestamp());
					tgdp.setData(arr);
				}
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		tmp.addTrendGraphData(CM_GRAPH_NAME__STATEMENT_CACHE__REQUEST_PER_SEC, new TrendGraphDataPoint(CM_GRAPH_NAME__STATEMENT_CACHE__REQUEST_PER_SEC, new String[] { "NumSearches", "HitCount", "NumInserts", "NumRemovals" }));
		if (AseTune.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_statement_cache_activity.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );

			// GRAPH
			TrendGraph tg = new TrendGraph(CM_GRAPH_NAME__STATEMENT_CACHE__REQUEST_PER_SEC,
					// Menu Checkbox text
					"Statement Cache Requests",
					// Label 
					"Number of Requests from the Statement Cache, per Second", // Label 
					new String[] { "NumSearches", "HitCount", "NumInserts", "NumRemovals" }, 
					false, tmp, false, -1);
			tmp.addTrendGraph(tg.getName(), tg, true);
		}
		
		_CMList.add(tmp);





		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// Statement Cache Details Activity
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------

		// FIXME: fix table description

		//==================================================================================================
		// 15.0.2: monCachedStatement (new in 15.0.2)
		// 
		// Description:
		//
		//
		// Needs Configuration: 'enable stmt cache monitoring'
		// none
		// 
		// Name                           Datatype     Attributes  Ind Description
		// ------------------------------ ------------ ----------- --- ------------------------------------------------------------
		// x                              x            x             x x
		//---------------------------------------------------------------------------------------------------
		// Column changes in various versions:
		//
		// Version      Action Name              Datatype   Attributes Description
		// ------------ ------ ----------------- ---------- ---------- ----------------------------------
		// 15.0.2 esd#5 add    DBName            varchar(30)           Name of the database
		// 15.0.2 esd#6 remove TableCount
		//---------------------------------------------------------------------------------------------------

		//==================================================================================================
		// 15.0.2: Description of: monCachedStatement
		// 
		// Provides information on individual statements in the statement cache. 
		//
		// Note: Enable the "enable monitoring" and "enable statement cache monitoring" 
		// configuration parameters, and set the "statement cache size" parameter greater
		// than 0 for this monitoring table to collect data. 
		//
		// ColumnName                     TypeName  Description
		// ------------------------------ --------- ------------------------------------------------------------------------------------
		// SSQLID                         int       The unique identifier for a statement.
		// Hashkey                        int       The hashkey over the statement's text.
		// UserID                         int       The user ID for the cached statement.
		// SUserID                        int       The server ID for the cached statement.
		// DBID                           smallint  The database ID from which the statement was cached.
		// UseCount                       int       The number of times this statement was used.
		// StatementSize                  int       The size of the statement's text in bytes.
		// MinPlanSizeKB                  int       The size of a plan associated with this statement when dormant.
		// MaxPlanSizeKB                  int       The size of a plan associated with this statement when it is in use.
		// CurrentUsageCount              int       The number of concurrent uses of this statement.
		// MaxUsageCount                  int       The maximum number of times for which this statement was simultaneously used.
		// NumRecompilesSchemaChanges     int       The number of times this statement was recompiled due to schema changes.
		// NumRecompilesPlanFlushes       int       The number of times this statement was recompiled because no usable plan was found.
		// HasAutoParams                  tinyint   Does this statement have any parameterized literals.
		// ParallelDegree                 tinyint   The parallel-degree session setting.
		// QuotedIdentifier               tinyint   The quoted identifier session setting.
		// TableCount                     tinyint   The table count session setting.
		// TransactionIsolationLevel      tinyint   The transaction isolation level session setting.
		// TransactionMode                tinyint   The transaction mode session setting.
		// SAAuthorization                tinyint   The SA authorization session setting.
		// SystemCatalogUpdate            tinyint   The system catalog update session setting.
		// MetricsCount                   int       Number of executions over which query metrics were captured.
		// MinPIO                         int       The minimum PIO value.
		// MaxPIO                         int       The maximum PIO value.
		// AvgPIO                         int       The average PIO value.
		// MinLIO                         int       The minimum LIO value.
		// MaxLIO                         int       The maximum LIO value.
		// AvgLIO                         int       The average LIO value.
		// MinCpuTime                     int       The minimum execution time value.
		// MaxCpuTime                     int       The maximum execution time value.
		// AvgCpuTime                     int       The average execution time value.
		// MinElapsedTime                 int       The minimum elapsed time value.
		// MaxElapsedTime                 int       The maximum elapsed time value.
		// AvgElapsedTime                 int       The average elapsed time value.
		// CachedDate                     datetimn  Date when this statement was cached.
		// LastUsedDate                   datetimn  Date when this statement was last used.
		// LastRecompiledDate             datetimn  Date when this statement was last recompiled.
		//---------------------------------------------------------------------------------------------------
		// Column changes in various versions:
		//
        // Version       Action  Name                      Datatype     Attributes  Description
        // ------------- ------- ------------------------- ------------ ----------- ----------------------------------
		// 15.0.2 ESD#4  added   DBName                                              Name of the database (will be NULL if the database is no longer open)
		// 15.0.2 ESD#6  removed TableCount                                          in the "SessionSettings" section...
		// 15.0.2(CE)    added   InstanceID                                          Cluster instance ID
		// 15.5          added   InstanceID                                          Cluster instance ID
        // 15.7 (3B)     added   OptimizationGoal          varchar(30)               The optimization goal stored in the statement cache
        // 15.7 (3B)     added   OptimizerLevel            varchar(30)               The optimizer level stored in the statement cache
		//---------------------------------------------------------------------------------------------------

		name         = CM_NAME__STATEMENT_CACHE_DETAILS;
		displayName  = CM_DESC__STATEMENT_CACHE_DETAILS;
		description  = "<html>" +
							"Get detailed statistics about each SQL statement in the cache.<br>" +
							"This also includes the SQL statement itself<br>" +
							"And the execution plan (showplan) for the SQL statement<br>" +
						"</html>";
		
		needVersion  = 15020; // monCachedStatement where intruduced in 15.0.2
		needCeVersion= 0;
		monTables    = new String[] {"monCachedStatement"};
		needRole     = new String[] {"mon_role"};
		needConfig   = new String[] {"enable monitoring=1", "enable stmt cache monitoring=1", "statement cache size"}; // NO default for 'statement cache size' configuration
		colsCalcDiff = new String[] {"UseCountDiff", "NumRecompilesPlanFlushes", "NumRecompilesSchemaChanges"};
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
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return new String[] {"enable monitoring=1", "enable stmt cache monitoring=1", "statement cache size"}; // NO default for 'statement cache size' configuration
			}

			@Override
			protected CmSybMessageHandler createSybMessageHandler()
			{
				CmSybMessageHandler msgHandler = super.createSybMessageHandler();

				//msgHandler.addDiscardMsgStr("Usage information at date and time");
				// Server Message:  Number  11096, Severity  10
				// Server 'GORAN_1_DS', Line 1:
				// The statement id is not valid. 
				msgHandler.addDiscardMsgNum(11096);

				return msgHandler;
			}

			@Override
			public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				try 
				{
					String showplanTooltip = "<html>" +
						"The execution plan this SQL statement has.<br>" +
						"<b>Formula</b>: show_plan(-1,SSQLID,-1,-1)<br>" +
						"<b>Note</b>: It is possible for a single entry in the statement cache to be associated with multiple, and possibly different, SQL plans. <b>show_plan</b> displays only one of them." +
						"</html>";
					String sqltextTooltip =	"<html>" +
						"SQL Statement that is assigned to the SSQLID.<br>" +
						"<b>Formula</b>: show_cached_text(SSQLID)<br>" +
						"</html>";
					String xmlPlanTooltip =	"<html>" +
					    "XML Plan of the Statement that is assigned to the SSQLID.<br>" +
					    "<b>Formula</b>: show_cached_plan_in_xml(SSQLID, 0)<br>" +
					    "<b>Note</b>: If the FULL XML plan is not displayed, try to disable the 'Get Showplan' option.</html>";

					MonTablesDictionary mtd = MonTablesDictionary.getInstance();
					mtd.addColumn("monCachedStatement",  "msgAsColValue", showplanTooltip);
					mtd.addColumn("monCachedStatement",  "HasShowplan",   showplanTooltip);
					mtd.addColumn("monCachedStatement",  "sqltext",       sqltextTooltip);
					mtd.addColumn("monCachedStatement",  "HasSqltext",    sqltextTooltip);
					mtd.addColumn("monCachedStatement",  "xmlPlan",       xmlPlanTooltip);
					mtd.addColumn("monCachedStatement",  "HasXmlPlan",    xmlPlanTooltip);
				}
				catch (NameNotFoundException e) {/*ignore*/}
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				List <String> pkCols = new LinkedList<String>();

				if (isClusterEnabled)
					pkCols.add("InstanceID");

				pkCols.add("SSQLID");

				return pkCols;
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				// NOTE: The function 'show_plan(-1,SSQLID,-1,-1)'
				//       returns a Ase Message (MsgNum=0) within the ResultSet
				//       those messages are placed in the column named 'msgAsColValue'
				//       see SamplingCnt.readResultset() for more details
				String sql = 
					"SELECT \n" +
					(isClusterEnabled ? " InstanceID, \n" : "") + //  The Server Instance Identifier (cluster only)
					" DBID, \n" +                       // The database ID from which the statement was cached.
					(aseVersion >= 15024 ? " DBName, \n" : "DBName = db_name(DBID), \n") + //  Name of the database (will be NULL if the database is no longer open)
					" UserID, SUserID, SUserName = suser_name(SUserID), \n" +
					" SSQLID, \n" +                     // The unique identifier for a statement.
					" Hashkey, \n" +                    // The hashkey over the statement's text.
//					" HasShowplan   = CASE WHEN show_plan(-1,SSQLID,-1,-1) < 0 THEN convert(bit,0) ELSE convert(bit,1) END, \n" +
//					" HasSqltext    = convert(bit,1), \n" +
					(aseVersion >= 15700 ? " OptimizationGoal, " : "") + // The optimization goal stored in the statement cache
					(aseVersion >= 15700 ? " OptimizerLevel, \n" : "") + // The optimizer level stored in the statement cache
					                       " HasSqltext    = RUNTIME_REPLACE::HAS_SQL_TEXT, \n" +
					                       " HasShowplan   = RUNTIME_REPLACE::HAS_SHOWPLAN, \n" +
					(aseVersion >= 15700 ? " HasXmlPlan    = RUNTIME_REPLACE::HAS_XML_PLAN, \n" : "") +
					" UseCount, \n" +                   // The number of times this statement was used.
					" UseCountDiff = UseCount, \n" +    // The number of times this statement was used.
					" MetricsCount, \n" +               // Number of executions over which query metrics were captured.
					" MaxElapsedTime, MinElapsedTime, AvgElapsedTime, \n" + // Elapsed time value.
					" MaxLIO,         MinLIO,         AvgLIO, \n" +         // Logical IO
					" MaxPIO,         MinPIO,         AvgPIO, \n" +         // Physical IO
					" MaxCpuTime,     MinCpuTime,     AvgCpuTime, \n" +     // Execution time.
					" LastUsedDate, \n" +               // Date when this statement was last used.
					" LastRecompiledDate, \n" +         // Date when this statement was last recompiled.
					" CachedDate, \n" +                 // Date when this statement was cached.
					" MinPlanSizeKB, \n" +              // The size of a plan associated with this statement when dormant.
					" MaxPlanSizeKB, \n" +              // The size of a plan associated with this statement when it is in use.
					" CurrentUsageCount, \n" +          // The number of concurrent uses of this statement.
					" MaxUsageCount, \n" +              // The maximum number of times for which this statement was simultaneously used.
					" NumRecompilesSchemaChanges, \n" + // The number of times this statement was recompiled due to schema changes.
					" NumRecompilesPlanFlushes, \n" +   // The number of times this statement was recompiled because no usable plan was found.
					" HasAutoParams       = convert(bit,HasAutoParams), \n" + // Does this statement have any parameterized literals.
					" ParallelDegree, \n" +               // The parallel-degree session setting.
					" QuotedIdentifier    = convert(bit,QuotedIdentifier), \n" + // The quoted identifier session setting.
					(aseVersion < 15026 ? " TableCount, \n" : "") + // describeme
					" TransactionIsolationLevel, \n" +  // The transaction isolation level session setting.
					" TransactionMode, \n" +            // The transaction mode session setting.
					" SAAuthorization     = convert(bit,SAAuthorization), \n" +     // The SA authorization session setting.
					" SystemCatalogUpdate = convert(bit,SystemCatalogUpdate), \n" + // The system catalog update session setting.
					" StatementSize, \n" +              // The size of the statement's text in bytes.
//					" sqltext       = convert(text, show_cached_text(SSQLID)), \n" +
					                       " sqltext       = RUNTIME_REPLACE::DO_SQL_TEXT, \n" +
					(aseVersion >= 15700 ? " xmlPlan       = RUNTIME_REPLACE::DO_XML_PLAN, \n" : "") +
					                       " msgAsColValue = RUNTIME_REPLACE::DO_SHOWPLAN \n" + // this is the column where the show_plan() function will be placed (this is done in SamplingCnt.java:readResultset())
					"FROM master..monCachedStatement \n" +
					"WHERE MetricsCount > 0 \n";

				return sql;
			}

			@Override
			public String getSql()
			{
				String sql = super.getSql();
				
				Configuration conf = Configuration.getCombinedConfiguration();
				boolean sampleSqlText_chk  = (conf == null) ? false : conf.getBooleanProperty(getName()+".sample.sqlText",  false);
				boolean sampleXmlPlan_chk  = (conf == null) ? false : conf.getBooleanProperty(getName()+".sample.xmlPlan",  false);
				boolean sampleShowplan_chk = (conf == null) ? false : conf.getBooleanProperty(getName()+".sample.showplan", false);

				//----- SQL TEXT
				String hasSqlText = "convert(bit,1)";
				String  doSqltext = "convert(text, show_cached_text(SSQLID))";
				if ( ! sampleSqlText_chk )
				{
					hasSqlText = "convert(bit,0)";
					 doSqltext = "convert(text, 'CMstmntCacheDetails.sample.sqlText=false')";
				}
				sql = sql.replace("RUNTIME_REPLACE::HAS_SQL_TEXT", hasSqlText);
				sql = sql.replace("RUNTIME_REPLACE::DO_SQL_TEXT",   doSqltext);

				
				//----- XML PLAN
				if (getServerVersion() >= 15700)
				{
					String hasXmlPlan = "convert(bit,1)";
					String  doXmlPlan = "show_cached_plan_in_xml(SSQLID, 0)";
					if ( ! sampleXmlPlan_chk )
					{
						hasXmlPlan = "convert(bit,0)";
						 doXmlPlan = "convert(text, 'CMstmntCacheDetails.sample.xmlPlan=false')";
					}
					sql = sql.replace("RUNTIME_REPLACE::HAS_XML_PLAN", hasXmlPlan);
					sql = sql.replace("RUNTIME_REPLACE::DO_XML_PLAN",   doXmlPlan);
				}

				
				//----- SHOWPLAN
				String hasShowplan = "CASE WHEN show_plan(-1,SSQLID,-1,-1) < 0 THEN convert(bit,0) ELSE convert(bit,1) END";
				String  doShowplan = "convert(text, '')";
				if ( ! sampleShowplan_chk )
				{
					hasShowplan = "convert(bit,0)";
					 doShowplan = "convert(text, 'CMstmntCacheDetails.sample.showplan=false')";
				}
				sql = sql.replace("RUNTIME_REPLACE::HAS_SHOWPLAN", hasShowplan);
				sql = sql.replace("RUNTIME_REPLACE::DO_SHOWPLAN",   doShowplan);

				
				return sql;
			}

			@Override
			public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
			{
				// SHOWPLAN
				if ("HasShowplan".equals(colName) || "msgAsColValue".equals(colName))
				{
					int pos_showplanText = findColumn("msgAsColValue");
					if (pos_showplanText > 0)
					{
						Object cellVal = getValueAt(modelRow, pos_showplanText);
						if (cellVal instanceof String)
							return toHtmlString((String) cellVal, true);
					}
				}

				// XML PLAN
				if ("HasXmlPlan".equals(colName) || "xmlPlan".equals(colName))
				{
					int pos_xmlPlanText = findColumn("xmlPlan");
					if (pos_xmlPlanText > 0)
					{
						Object cellVal = getValueAt(modelRow, pos_xmlPlanText);
						if (cellVal instanceof String)
							return toHtmlString((String) cellVal, false);
					}
				}

				// SQLTEXT
				if ("HasSqltext".equals(colName) || "sqltext".equals(colName))
				{
					int pos_sqltext = findColumn("sqltext");
					if (pos_sqltext > 0)
					{
						Object cellVal = getValueAt(modelRow, pos_sqltext);
						if (cellVal instanceof String)
							return toHtmlString((String) cellVal, true);
					}
				}

				return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
			}

			/** add HTML around the string, and translate linebreaks into <br> */
			private String toHtmlString(String in, boolean breakLines)
			{
				String str = in;
				str = str.replaceAll("<", "&lt;");
				str = str.replaceAll(">", "&gt;");
				if (breakLines)
					str = StringUtil.makeApproxLineBreak(in, 150, 5, "\n");
				str = str.replaceAll("\\n", "<br>");

				return "<html><pre>" + str + "</pre></html>";
			}

			/** 
			 * Get number of rows to save/request ddl information for 
			 */
			@Override
			public int getMaxNumOfDdlsToPersist()
			{
				return 10;
			}
			@Override
			public String[] getDdlDetailsSortOnColName()
			{
				String[] sa = {"UseCount", "UseCountDiff", "AvgLIO", "AvgElapsedTime"};
				return sa;
			}
		};

		// If QUERY TIMEOUT is not set, set this to X second, because this can take time. 
		if (tmp.getQueryTimeout() == CountersModel.DEFAULT_sqlQueryTimeout)
			tmp.setQueryTimeout(30);

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (AseTune.hasGUI())
		{
//			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			final String localName = name;
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName())
			{
				private static final long	serialVersionUID	= 1L;
				private JCheckBox sampleXmlPlan_chk;

				@Override
				protected JPanel createLocalOptionsPanel()
				{
					JPanel panel = SwingUtils.createPanel("Local Options", true);
					panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

					Configuration conf = Configuration.getCombinedConfiguration();
					
					// SAMPLE SQL TEXT
					JCheckBox sampleSqlText_chk  = new JCheckBox("Get SQL Text", conf == null ? false : conf.getBooleanProperty(localName+".sample.sqlText", false));
					sampleSqlText_chk.setName(localName+".sample.sqlText");
					sampleSqlText_chk.setToolTipText("<html>Get SQL Text (using: show_cached_text(SSQLID)) assosiated with the Cached Statement.<br><b>Note</b>: This is not a filter, you will have to wait for next sample time for this option to take effect.</html>");
					panel.add(sampleSqlText_chk, "wrap");

					sampleSqlText_chk.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							// Need TMP since we are going to save the configuration somewhere
							Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
							if (conf == null) return;
							conf.setProperty(localName+".sample.sqlText", ((JCheckBox)e.getSource()).isSelected());
							conf.save();
						}
					});
					
					// SAMPLE SHOWPLAN
					JCheckBox sampleShowplan_chk = new JCheckBox("Get Showplan", conf == null ? false : conf.getBooleanProperty(localName+".sample.showplan", false));
					sampleShowplan_chk.setName(localName+".sample.showplan");
					sampleShowplan_chk.setToolTipText("<html>Get Showplan assosiated with the Cached Statement.<br><b>Note</b>: This is not a filter, you will have to wait for next sample time for this option to take effect.</html>");
					panel.add(sampleShowplan_chk, "wrap");

					sampleShowplan_chk.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							// Need TMP since we are going to save the configuration somewhere
							Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
							if (conf == null) return;
							conf.setProperty(localName+".sample.showplan", ((JCheckBox)e.getSource()).isSelected());
							conf.save();
						}
					});

					// SAMPLE XML PLAN
//					JCheckBox sampleXmlPlan_chk  = new JCheckBox("Get XML Plan", conf == null ? false : conf.getBooleanProperty(localName+".sample.xmlPlan", false));
					sampleXmlPlan_chk  = new JCheckBox("Get XML Plan", conf == null ? false : conf.getBooleanProperty(localName+".sample.xmlPlan", false));
					sampleXmlPlan_chk.setName(localName+".sample.xmlPlan");
					sampleXmlPlan_chk.setToolTipText("<html>ONLY ON ASE 15.7 and above<br>Get XML Plan (using: show_cached_plan_in_xml(SSQLID, 0)) assosiated with the Cached Statement.<br><b>Note</b>: Try uncheck 'Get Showplan' if not the whole XML is displayed.<br>This is not a filter, you will have to wait for next sample time for this option to take effect.</html>");
					panel.add(sampleXmlPlan_chk, "wrap");

					sampleXmlPlan_chk.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							// Need TMP since we are going to save the configuration somewhere
							Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
							if (conf == null) return;
							conf.setProperty(localName+".sample.xmlPlan", ((JCheckBox)e.getSource()).isSelected());
							conf.save();
						}
					});
					
					return panel;
				}

				@Override
				public void checkLocalComponents()
				{
					CountersModel cm = getCm();
					if (cm != null)
					{
						boolean enabled = false;
						if (cm.isRuntimeInitialized() && cm.getServerVersion() >= 15700)
							enabled = true;

						sampleXmlPlan_chk.setEnabled(enabled);
					}
				}
			};
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_statement_cache_detail_activity.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );
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
		// Description:
		//
		//
		// Needs Configuration:
		// none
		// 
		// Name                           Datatype     Attributes  Ind Description
		// ------------------------------ ------------ ----------- --- ------------------------------------------------------------
		// SPID                           smallint                   0 Session Process Identifier
		// KPID                           int                        0 Kernel Process Identifier
		// DBID                           int                        0 Unique identifier for the database where the object resides
		// ObjectID                       int                        0 Unique identifier for the object
		// IndexID                        int                        0 Unique identifier for the index
		// OwnerUserID                    int                        0 User identifier for the object owner
		// LogicalReads                   int                        1 Number of buffers read from cache
		// PhysicalReads                  int                        1 Number of buffers read from disk
		// PhysicalAPFReads               int                        1 Number of APF buffers read from disk
		// DBName                         varchar(30)                0 Name of database
		// ObjectName                     varchar(30)                0 Name of the object
		// ObjectType                     varchar(30)                0 Type of object
		//---------------------------------------------------------------------------------------------------
		// Column changes in various versions:
		//
		// Version      Action Name              Datatype   Attributes Description
		// ------------ ------ ----------------- ---------- ---------- ----------------------------------
		// 12.5.2       add    TableSize         int                   table size in Kbyte 
		// 15.0         add    PartitionID       int                   Unique identifier for the partition
		// 15.0         add    PartitionName     varchar(30)           Name of the partition
		// 15.0         delete TableSize         int                   >>> has been changed to PartitionSize - this reflects the size of the partition for the object 
		// 15.0         add    PartitionSize     int                   Partition size in kilobytes (KB)
		// 15.0.1CE/15.5 add   InstanceId        int                   The Server Instance Identifier (cluster only)
		//---------------------------------------------------------------------------------------------------

		name         = CM_NAME__ACTIVE_OBJECTS;
		displayName  = CM_DESC__ACTIVE_OBJECTS;
		description  = "<html>" +
							"Objects that are currently accessed by any active Statements in the ASE." +
							"<br><br>" +
							"Table Background colors:" +
							"<ul>" +
							"    <li>ORANGE - An Index.</li>" +
							"</ul>" +
						"</html>";

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
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return new String[] {"enable monitoring=1", "per object statistics active=1"};
			}

			@Override
			public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
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
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				List <String> pkCols = new LinkedList<String>();

				if (isClusterEnabled)
					pkCols.add("InstanceID");

				pkCols.add("SPID");
				pkCols.add("DBID");
				pkCols.add("OwnerUserID");
				pkCols.add("ObjectID");
				pkCols.add("IndexID");

				if (aseVersion >= 15000)
					pkCols.add("PartitionID");

				// NOTE: PK is NOT unique, so therefore 'dupMergeCount' column is added to the SQL Query
				//       This can happen for example on self joins (or when the same table is refrerenced more than once in the a SQL Statement)

				return pkCols;
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String cols = "";

				String InstanceID    = ""; // only in ClusterEdition
				String TableSize     = ""; // between 12.5.2 and < 15.0.0
				String ObjectName    = "ObjectName = isnull(object_name(ObjectID, DBID), 'Obj='+ObjectName), \n"; // if user is not a valid user in A.DBID, then object_name() will return null
				String IndexName     = ""; // 15.0.2 or higher
				String PartitionID   = ""; // 15.0.0 or higher
				String PartitionName = ""; // 15.0.0 or higher
				String PartitionSize = ""; // 15.0.0 or higher

				if (isClusterEnabled)
					InstanceID = "InstanceID, ";

				if (aseVersion >= 12520)
					TableSize = "TableSize, \n";

				if (aseVersion >= 15000)
				{
					TableSize = "";
					PartitionName = "PartitionName, ";
					PartitionSize = "PartitionSize, \n";
					PartitionID   = ", PartitionID";
				}

				if (aseVersion >= 15020)
				{
					IndexName = "IndexName = CASE WHEN IndexID=0 THEN convert(varchar(30),'DATA') \n" +
						        "                 ELSE convert(varchar(30), isnull(index_name(DBID, ObjectID, IndexID), '-unknown-')) \n" +
						        "            END, \n";
				}

				cols = InstanceID + "SPID, KPID, ObjectType, DBName, \n" +
				       ObjectName +
				       "IndexID, \n" + 
				       IndexName + 
				       PartitionName + PartitionSize +
				       TableSize +
				       "OwnerUserID, LogicalReads, PhysicalReads, PhysicalAPFReads, dupMergeCount=convert(int,0), \n" +
				       "DBID, ObjectID" + PartitionID;

				// in 12.5.4 (esd#9) will produce an "empty" resultset using "S.SPID != @@spid"
				//                   so this will be a workaround for those releses below 15.0.0
				String whereSpidNotMe = "SPID != @@spid";
				if (aseVersion <= 15000)
				{
					whereSpidNotMe = "SPID != convert(int,@@spid)";
				}

				String sql = 
					"select " + cols + "\n" +
					"from master..monProcessObject\n" +
					"where "+whereSpidNotMe;

				return sql;
			}

			/** 
			 * Get number of rows to save/request ddl information for 
			 */
			@Override
			public int getMaxNumOfDdlsToPersist()
			{
				return Integer.MAX_VALUE; // Basically ALL Rows
			}
		};
	
		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (AseTune.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_process_object_activity.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );

//			tcp.setTableToolTipText(
//				    "<html>" +
//				        "Background colors:" +
//				        "<ul>" +
//				        "    <li>ORANGE - An Index.</li>" +
//				        "</ul>" +
//				    "</html>");

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

		//==================================================================================================
		// 12.5.0.3: Description of: monProcessStatement
		// 
		// Description:
		//
		//
		// Needs Configuration:
		// none
		// 
		// Name                           Datatype     Attributes  Ind Description
		// ------------------------------ ------------ ----------- --- ------------------------------------------------------------
		// SPID                           smallint                   0 Session Process Identifier
		// KPID                           int                        0 Kernel Process Identifier
		// DBID                           int                        0 Unique identifier for the database
		// ProcedureID                    int                        0 Unique identifier for the procedure
		// PlanID                         int                        0 Unique identifier for the stored plan for the procedure
		// BatchID                        int                        0 Unique identifier for the SQL batch containing the statement
		// ContextID                      int                        0 The stack frame of the procedure, if a procedure
		// LineNumber                     int                        0 Line number of the statement within the SQL Batch
		// StartTime                      datetime                   0 Date when the statement began execution
		// CpuTime                        int                        1 Number of milliseconds of CPU used by the statement
		// WaitTime                       int                        1 Number of milliseconds the task has waited during execution of the statement
		// MemUsageKB                     int                        0 Number of kilobytes of memory used for execution of the statement
		// PhysicalReads                  int                        1 Number of buffers read from disk
		// LogicalReads                   int                        1 Number of buffers read from cache
		// PagesModified                  int                        1 Number of pages modified by the statement
		// PacketsSent                    int                        1 Number of network packets sent by ASE
		// PacketsReceived                int                        1 Number of network packets received by ASE
		// NetworkPacketSize              int                        0 Size (in bytes) of the network packet currently configured for the session
		// PlansAltered                   int                        1 The number of plans altered at  execution time.
		//---------------------------------------------------------------------------------------------------
		// Column changes in various versions:
		//
		// Version      Action Name              Datatype   Attributes Description
		// ------------ ------ ----------------- ---------- ---------- ----------------------------------
		// 15.0 ESD#2   add    RowsAffected      int                   The number of rows affected by the statement.
		// 15.0.1CE/15.5 add   InstanceId        int                   The Server Instance Identifier (cluster only)
		// 15.0.2 esd#5 add    DBName            varchar(30)           Name of the database
		//---------------------------------------------------------------------------------------------------

		//==================================================================================================
		// 12.5.0.3: Description of: monProcess
		// 
		// Description:
		//
		//
		// Needs Configuration:
		// none
		// 
		// Name                           Datatype     Attributes  Ind Description
		// ------------------------------ ------------ ----------- --- ------------------------------------------------------------
		// SPID                           smallint                   0 Session Process Identifier
		// KPID                           int                        0 Kernel Process Identifier
		// FamilyID                       smallint                   0 The SPID of the parent process, if this is a worker process
		// BatchID                        int                        0 Unique identifier for the SQL batch containing the statement being executed
		// ContextID                      int                        0 The stack frame of the procedure, if a procedure
		// LineNumber                     int                        0 Line number of the current statement within the SQL Batch
		// SecondsConnected               int                        0 The number of seconds since this connection was established
		// WaitEventID                    smallint                   0 Unique identifier for the event that the process is waiting for, if the process is currently in a wait state
		// BlockingSPID                   smallint                   0 Session Process identifier of the process holding the lock that this process has requested, if waiting for a lock
		// DBID                           int                        0 Unique identifier of the process' current database
		// EngineNumber                   smallint                   0 Unique identifier of the engine that the process is executing on
		// Priority                       int                        0 Priority at which the process is executing
		// Login                          varchar(30)                0 Login user name
		// Application                    varchar(30)                0 Application name
		// Command                        varchar(30)                0 Category of process or command that the process is currently executing
		// NumChildren                    intn                       0 Number of child processes, if executing a parallel query
		// SecondsWaiting                 intn                       0 Amount of time in seconds that process has been waiting, if the process is currently in a wait state
		// BlockingXLOID                  intn                       0 Unique Lock Identifier for the lock that this process has requested, if waiting for a lock
		// DBName                         varchar(30)                0 Name of process' current database
		// EngineGroupName                varchar(30)                0 Engine group for the process
		// ExecutionClass                 varchar(30)                0 Execution class for the process
		// MasterTransactionID            varchar(255)               0 Unique Transaction Identifier for the current transaction, if in a transaction
		//---------------------------------------------------------------------------------------------------
		// Column changes in various versions:
		//
		// Version      Action Name              Datatype   Attributes Description
		// ------------ ------ ----------------- ---------- ---------- ----------------------------------
		// 15.0.1CE/15.5 add   InstanceId        int                   The Server Instance Identifier (cluster only)
		// 15.0.2 esd#5 add    ServerUserID      int                   Server User Identifier of the user running this process. This matches the syslogins.suid column. The corresponding name can be obtained using the 'suser_name' function
		// 15.7 3a      add    ProgramName       varchar(30)      Null Name of the program on which the process is running.
		// 15.7 3b      remove ProgramName       varchar(30)      Null Name of the program on which the process is running.
		// 15.7 3a      add    HostName          varchar(30)      Null Name of the host machine on which the application that started the process is running.
		// 15.7 3a      add    ClientName        varchar(30)      Null Value of the clientname property set by the application.
		// 15.7 3a      add    ClientHostName    varchar(30)      Null Value of the clienthostname property set by the application.
		// 15.7 3a      add    ClientApplName    varchar(30)      Null Value of the clientapplname property set by the application.
        // 15.7 (3A)    add    ProgramName       varchar(30)      Null Name of the program on which the process is running.
        // 15.7 (3B)    remove ProgramName       varchar(30)      Null Name of the program on which the process is running.
        // 15.7 (3B)    add    HostName          varchar(30)      Null Name of the host machine on which the application that started the process is running.
        // 15.7 (3B)    add    ClientName        varchar(30)      Null Value of the clientname property set by the application.
        // 15.7 (3B)    add    ClientHostName    varchar(30)      Null Value of the clienthostname property set by the application.
        // 15.7 (3B)    add    ClientApplName    varchar(30)      Null Value of the clientapplname property set by the application.
        //---------------------------------------------------------------------------------------------------

		name         = CM_NAME__ACTIVE_STATEMENTS;
		displayName  = CM_DESC__ACTIVE_STATEMENTS;
		description  = "<html>" +
							"Statemenets that are currently executing in the ASE." +
							"<br><br>" +
							"Table Background colors:" +
							"<ul>" +
							"    <li>ORANGE - SPID was visible in previous sample as well.</li>" +
							"    <li>PINK   - SPID is Blocked by another SPID from running, this SPID is the Victim of a Blocking Lock, which is showned in RED.</li>" +
							"    <li>RED    - SPID is Blocking other SPID's from running, this SPID is Responslibe or the Root Cause of a Blocking Lock.</li>" +
							"</ul>" +
						"</html>";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		needVersion  = 0;
		needCeVersion= 0;
		monTables    = new String[] {"monProcessStatement", "monProcess"};
		needRole     = new String[] {"mon_role", "sybase_ts_role"};
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
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return new String[] {"enable monitoring=1", "statement statistics active=1", "wait event timing=1"};
			}

			@Override
			public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
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
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				List <String> pkCols = new LinkedList<String>();

				if (isClusterEnabled)
					pkCols.add("InstanceID");

				pkCols.add("SPID");
				pkCols.add("monSource");

				return pkCols;
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				////////////////////////////
				// NOTE: This Counter Model has 2 separete SQL selects
				//       1: The rows in: monProcessStatement
				//       2: The rows in: SPID's that might NOT be in monProcessStatement, BUT still are BLOCKING other spids
				// IMPORTANT: when you add a column to the FIRST you need to add it to the SECOND as well
				////////////////////////////

				// ASE 15.7
				String HostName       = "";
				String ClientName     = "";
				String ClientHostName = "";
				String ClientApplName = "";
				String ase1570_nl     = "";

				if (aseVersion >= 15700)
				{
					HostName       = "P.HostName, ";
					ClientName     = "P.ClientName, ";
					ClientHostName = "P.ClientHostName, ";
					ClientApplName = "P.ClientApplName, ";
					ase1570_nl     = "\n";
				}

				String optGoalPlan = "";
				if (aseVersion >= 15020)
				{
					optGoalPlan = "plan '(use optgoal allrows_dss)' \n";
				}

				if (isClusterEnabled)
				{
					cols1 += "S.InstanceID, ";
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
				         HostName + ClientName + ClientHostName + ClientApplName + ase1570_nl +
				         "S.CpuTime, S.WaitTime, ExecTimeInMs=datediff(ms, S.StartTime, getdate()), \n" +
				         "UsefullExecTime = (datediff(ms, S.StartTime, getdate()) - S.WaitTime), \n" +
				         "BlockingOtherSpids=convert(varchar(255),''), P.BlockingSPID, \n" +
				         "P.SecondsWaiting, P.WaitEventID, \n" +
				         "WaitClassDesc=convert(varchar(50),''), \n" +
				         "WaitEventDesc=convert(varchar(50),''), \n" +
				         "HasMonSqlText=convert(bit,0), HasDbccSqlText=convert(bit,0), HasProcCallStack=convert(bit,0), \n" +
				         "HasShowPlan=convert(bit,0), HasStacktrace=convert(bit,0), \n" +
				         "S.MemUsageKB, S.PhysicalReads, S.LogicalReads, \n";
				cols2 += "";
				cols3 += "S.PagesModified, S.PacketsSent, S.PacketsReceived, S.NetworkPacketSize, \n" +
				         "S.PlansAltered, S.StartTime, S.PlanID, S.DBID, S.ProcedureID, \n" +
				         "P.SecondsConnected, P.EngineNumber, P.NumChildren, \n" +
				         "MonSqlText=convert(text,null), \n" +
				         "DbccSqlText=convert(text,null), \n" +
				         "ProcCallStack=convert(text,null), \n" +
				         "ShowPlanText=convert(text,null), \n" +
				         "DbccStacktrace=convert(text,null)";
				if (aseVersion >= 15020 || (aseVersion >= 12540 && aseVersion <= 15000) )
				{
					cols2 += "S.RowsAffected, " +
					         "tempdb_name = db_name(tempdb_id(S.SPID)), \n" +
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
					"from master..monProcessStatement S, master..monProcess P \n" +
					"where S.KPID = P.KPID \n" +
					(isClusterEnabled ? "  and S.InstanceID = P.InstanceID \n" : "") +
					"  and "+whereSpidNotMe+"\n" +
					"order by S.LogicalReads desc \n" +
					optGoalPlan;

				//-------------------------------------------
				// Build SECOND SQL, which gets SPID's that blocks other, and might not be within the ABOVE ststement
				//-------------------------------------------
				cols1 = cols2 = cols3 = "";

				if (isClusterEnabled)
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
				         HostName + ClientName + ClientHostName + ClientApplName + ase1570_nl +
				         "P.Command, P.Application, \n" +
				         "CpuTime=-1, WaitTime=-1, ExecTimeInMs=-1, \n" +
				         "UsefullExecTime = -1, \n" +
				         "BlockingOtherSpids=convert(varchar(255),''), P.BlockingSPID, \n" +
				         "P.SecondsWaiting, P.WaitEventID, \n" +
				         "WaitClassDesc=convert(varchar(50),''), \n" +
				         "WaitEventDesc=convert(varchar(50),''), \n" +
				         "HasMonSqlText=convert(bit,0), HasDbccSqlText=convert(bit,0), HasProcCallStack=convert(bit,0), \n" +
				         "HasShowPlan=convert(bit,0), HasStacktrace=convert(bit,0), \n" +
				         "MemUsageKB=-1, PhysicalReads=-1, LogicalReads=-1, \n";
				cols2 += "";
				cols3 += "PagesModified=-1, PacketsSent=-1, PacketsReceived=-1, NetworkPacketSize=-1, \n" +
				         "PlansAltered=-1, StartTime=convert(datetime,NULL), PlanID=-1, P.DBID, ProcedureID=-1, \n" +
				         "P.SecondsConnected, P.EngineNumber, P.NumChildren, \n" +
				         "MonSqlText=convert(text,null), \n" +
				         "DbccSqlText=convert(text,null), \n" +
				         "ProcCallStack=convert(text,null), \n" +
				         "ShowPlanText=convert(text,null), \n" +
				         "DbccStacktrace=convert(text,null)";
				if (aseVersion >= 15020 || (aseVersion >= 12540 && aseVersion <= 15000) )
				{
					cols2 += "RowsAffected=-1, " +
					         "tempdb_name = db_name(tempdb_id(P.SPID)), \n" +
					         "pssinfo_tempdb_pages = convert(int, pssinfo(P.SPID, 'tempdb_pages')), \n";
				}


				// The above I had in the Properties file for a long time
				String x_sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from master..monProcess P \n" +
					"where P.SPID in (select blocked from master..sysprocesses where blocked > 0) \n" +
					optGoalPlan;


				//------------------------------------------------
				// Finally return the SQL
				//------------------------------------------------
				return sql + "\n\n"
			       + "  /* UNION ALL */ \n\n"
			       + x_sql;
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
							return toHtmlString((String) cellVal);
							//return (String) cellVal;
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
							return toHtmlString((String) cellVal);
							//return (String) cellVal;
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
			/** add HTML around the string, and translate linebreaks into <br> */
			private String toHtmlString(String in)
			{
				String str = StringUtil.makeApproxLineBreak(in, 150, 10, "\n");
				str = str.replaceAll("\\n", "<br>");
				if (in.indexOf("<html>")>=0 || in.indexOf("<HTML>")>=0)
					return str;
				return "<html><pre>" + str + "</pre></html>";
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

//				Configuration conf = Configuration.getInstance(Configuration.TEMP);
				Configuration conf = Configuration.getCombinedConfiguration();
				boolean getShowplan       = conf == null ? true : conf.getBooleanProperty(getName()+".sample.showplan",       true);
				boolean getMonSqltext     = conf == null ? true : conf.getBooleanProperty(getName()+".sample.monSqltext",     true);
				boolean getDbccSqltext    = conf == null ? false: conf.getBooleanProperty(getName()+".sample.dbccSqltext",    false);
				boolean getProcCallStack  = conf == null ? true : conf.getBooleanProperty(getName()+".sample.procCallStack",  true);
				boolean getDbccStacktrace = conf == null ? false: conf.getBooleanProperty(getName()+".sample.dbccStacktrace", false);

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

				if ( ! MonTablesDictionary.hasInstance() )
					return;
				MonTablesDictionary mtd = MonTablesDictionary.getInstance();

				if (counters == null)
					return;

				// Find column Id's
				List<String> colNames = counters.getColNames();
				if (colNames==null) return;

				for (int colId=0; colId < colNames.size(); colId++) 
				{
					String colName = colNames.get(colId);
					if      (colName.equals("WaitEventID"))        pos_WaitEventID        = colId;
					else if (colName.equals("WaitEventDesc"))      pos_WaitEventDesc      = colId;
					else if (colName.equals("WaitClassDesc"))      pos_WaitClassDesc      = colId;
					else if (colName.equals("SPID"))               pos_SPID               = colId;
					else if (colName.equals("HasShowPlan"))        pos_HasShowPlan        = colId;
					else if (colName.equals("ShowPlanText"))       pos_ShowPlanText       = colId;
					else if (colName.equals("HasMonSqlText"))      pos_HasMonSqlText      = colId;
					else if (colName.equals("MonSqlText"))         pos_MonSqlText         = colId;
					else if (colName.equals("HasDbccSqlText"))     pos_HasDbccSqlText     = colId;
					else if (colName.equals("DbccSqlText"))        pos_DbccSqlText        = colId;
					else if (colName.equals("HasProcCallStack"))   pos_HasProcCallStack   = colId;
					else if (colName.equals("ProcCallStack"))      pos_ProcCallStack      = colId;
					else if (colName.equals("HasStacktrace"))      pos_HasStacktrace      = colId;
					else if (colName.equals("DbccStacktrace"))     pos_DbccStacktrace     = colId;
					else if (colName.equals("BlockingOtherSpids")) pos_BlockingOtherSpids = colId;
					else if (colName.equals("BlockingSPID"))       pos_BlockingSPID       = colId;
					else if (colName.equals("multiSampled"))       pos_multiSampled       = colId;
					else if (colName.equals("StartTime"))          pos_StartTime          = colId;

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
							if (stacktrace == null)
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

						// This could be used to test that PCS.store() will truncate string size to the tables storage size
						//blockingList += "'1:aaa:0', '1:bbb:0', '1:ccc:0', '1:ddd:0', '1:eee:0', '1:fff:0', '1:ggg:0', '1:hhh:0', '1:iii:0', '1:jjj:0', '1:kkk:0', '1:lll:0', '1:mmm:0', '1:nnn:0', '1:ooo:0', '1:ppp:0', '1:qqq:0', '1:rrr:0', '1:sss:0', '1:ttt:0', '1:uuu:0', '1:vvv:0', '1:wwww:0', '1:xxx:0', '1:yyy:0', '1:zzz:0' -end-";

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

			/** 
			 * Get number of rows to save/request ddl information for 
			 */
			@Override
			public int getMaxNumOfDdlsToPersist()
			{
				return Integer.MAX_VALUE; // Basically ALL Rows
			}

			/** 
			 * Get Column names to where DBName and ObjectName is called, this must always return at least a array with 2 strings. 
			 */
			@Override
			public String[] getDdlDetailsColNames()
			{
				String[] sa = {"dbname", "procname"};
				return sa;
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (AseTune.hasGUI())
		{
			final String localName = name;
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

//					Configuration conf = Configuration.getInstance(Configuration.TEMP);
					Configuration conf = Configuration.getCombinedConfiguration();
					JCheckBox sampleMonSqltext_chk     = new JCheckBox("Get Monitored SQL Text",   conf == null ? true : conf.getBooleanProperty(getName()+".sample.monSqltext",     true));
					JCheckBox sampleDbccSqltext_chk    = new JCheckBox("Get DBCC SQL Text",        conf == null ? true : conf.getBooleanProperty(getName()+".sample.dbccSqltext",    false));
					JCheckBox sampleProcCallStack_chk  = new JCheckBox("Get Procedure Call Stack", conf == null ? true : conf.getBooleanProperty(getName()+".sample.procCallStack",  true));
					JCheckBox sampleShowplan_chk       = new JCheckBox("Get Showplan",             conf == null ? true : conf.getBooleanProperty(getName()+".sample.showplan",       true));
					JCheckBox sampleDbccStacktrace_chk = new JCheckBox("Get ASE Stacktrace",       conf == null ? true : conf.getBooleanProperty(getName()+".sample.dbccStacktrace", false));

					sampleMonSqltext_chk    .setName(getName()+".sample.monSqltext");
					sampleDbccSqltext_chk   .setName(getName()+".sample.dbccSqltext");
					sampleProcCallStack_chk .setName(getName()+".sample.procCallStack");
					sampleShowplan_chk      .setName(getName()+".sample.showplan");
					sampleDbccStacktrace_chk.setName(getName()+".sample.dbccStacktrace");
					
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
							// Need TMP since we are going to save the configuration somewhere
							Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
							if (conf == null) return;
							conf.setProperty(localName+".sample.monSqltext", ((JCheckBox)e.getSource()).isSelected());
							conf.save();
						}
					});
					sampleDbccSqltext_chk.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							// Need TMP since we are going to save the configuration somewhere
							Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
							if (conf == null) return;
							conf.setProperty(localName+".sample.dbccSqltext", ((JCheckBox)e.getSource()).isSelected());
							conf.save();
						}
					});
					sampleProcCallStack_chk.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							// Need TMP since we are going to save the configuration somewhere
							Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
							if (conf == null) return;
							conf.setProperty(localName+".sample.procCallStack", ((JCheckBox)e.getSource()).isSelected());
							conf.save();
						}
					});
					sampleShowplan_chk.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							// Need TMP since we are going to save the configuration somewhere
							Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
							if (conf == null) return;
							conf.setProperty(localName+".sample.showplan", ((JCheckBox)e.getSource()).isSelected());
							conf.save();
						}
					});
					sampleDbccStacktrace_chk.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							// Need TMP since we are going to save the configuration somewhere
							Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
							if (conf == null) return;
							conf.setProperty(localName+".sample.dbccStacktrace", ((JCheckBox)e.getSource()).isSelected());
							conf.save();
						}
					});
					
					return panel;
				}
			};
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_active_statements_activity.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );

//			tcp.setTableToolTipText(
//				    "<html>" +
//				        "Background colors:" +
//				        "<ul>" +
//				        "    <li>ORANGE - SPID was visible in previous sample as well.</li>" +
//				        "    <li>PINK   - SPID is Blocked by another SPID from running, this SPID is the Victim of a Blocking Lock, which is showned in RED.</li>" +
//				        "    <li>RED    - SPID is Blocking other SPID's from running, this SPID is Responslibe or the Root Cause of a Blocking Lock.</li>" +
//				        "</ul>" +
//				    "</html>");

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

		//==================================================================================================
		// 12.5.0.3: Description of: monLocks
		// 
		// Description:
		//
		//
		// Needs Configuration:
		// none
		// 
		// Name                           Datatype     Attributes  Ind Description
		// ------------------------------ ------------ ----------- --- ------------------------------------------------------------
		// SPID                           smallint                   0 Session Process Identifier
		// KPID                           int                        0 Kernel Process Identifier
		// DBID                           int                        0 Unique identifier for the database
		// ParentSPID                     smallint                   0 Parent Process ID
		// LockID                         int                        0 Lock Object ID (loid)
		// Context                        int                        0 Lock context (bitfield)
		// ObjectID                       intn                       0 Unique identifier for the object
		// LockState                      varchar(20)                0 Whether the lock has been granted ['Hold', 'Wait']
		// LockType                       varchar(20)                0 Type of lock ['Exclusive', 'Shared', 'Update', e.t.c]
		// LockLevel                      varchar(30)                0 The type of object for which the lock was requested ['Row', 'Page', 'Table', 'Address']
		// WaitTime                       intn                       0 The time (in seconds) that the lock request has not been granted
		// PageNumber                     intn                       0 Page that is locked when LockLevel = 'Page'
		// RowNumber                      intn                       0 Row that is locked when LockLevel = 'Row'
		//---------------------------------------------------------------------------------------------------
		// Column changes in various versions:
		//
		// Version      Action Name              Datatype   Attributes Description
		// ------------ ------ ----------------- ---------- ---------- ----------------------------------
		// 15.0 ESD#2   add    BlockedState      varchar(64)           Lock status information (bitfield)
		// 15.0 ESD#2   add    BlockedBy         int                   LockID of lock blocking this lock request or zero if not blocked
		// 15.0.1CE/15.5 add   InstanceId        int                   The Server Instance Identifier (cluster only)
		// 15.0.2       add    SourceCodeID      varchar(30)           Location in the source code at which the lock was requested. For internal use only
		// 15.0.2 esd#5 add    DBName            varchar(30)           Name of the database
		//---------------------------------------------------------------------------------------------------
		
		name         = CM_NAME__BLOCKING;
		displayName  = CM_DESC__BLOCKING;
		description  = "<html>" +
							"Are there any SPIDS blocked/blocking for more than 'Lock wait threshold' in the system" +
							"<br><br>" +
							"Background colors:" +
							"<ul>" +
							"    <li>PINK   - SPID is Blocked by some other SPID that holds a Lock on a database object Table, Page or Row. This is the Lock Victim.</li>" +
							"    <li>RED    - SPID is Blocking other SPID's from running, this SPID is Responslibe or the Root Cause of a Blocking Lock.</li>" +
							"</ul>" +
						"</html>";
		
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
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return new String[] {"enable monitoring=1", "wait event timing=1"};
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				// no need to have PK, since we are NOT using "diff" counters
				return null;
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String cols1, cols2, cols3;
				cols1 = cols2 = cols3 = "";

				cols1 = "*";
				cols2 = ", ObjectName = isnull(object_name(ObjectID, DBID), 'ObjId='+convert(varchar(30),ObjectID))"; // if user is not a valid user in A.DBID, then object_name() will return null
				cols3 = "";
				
				String whereArgs = "where BlockedState = 'Blocked' or BlockedState = 'Blocking' or BlockedBy > 0 ";

				String sql = 
					"select " + cols1 + cols2 + cols3 + "\n" +
					"from master..monLocks\n" +
					whereArgs + "\n";

				return sql;
			}

			/** 
			 * Get number of rows to save/request ddl information for 
			 */
			@Override
			public int getMaxNumOfDdlsToPersist()
			{
				return Integer.MAX_VALUE; // Basically ALL Rows
			}
		};
	
		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (AseTune.hasGUI())
		{
			final String finalDisplayName = displayName;
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName())
			{
				private static final long	serialVersionUID	= 1L;

				@Override
				protected JPanel createLocalOptionsPanel()
				{
					JPanel panel = SwingUtils.createPanel("Local Options", true);
					panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

					JButton resetMoveToTab_but = new JButton("Reset 'Move to Tab' settings.");

					resetMoveToTab_but.setToolTipText(
							"<html>" +
							"Reset the option: To automatically switch to this tab when you have <b>blocking locks</b>.<br>" +
							"Next time this happens, a popup will ask you what you want to do." +
							"</html>");
					resetMoveToTab_but.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							ChangeToJTabDialog.resetSavedSettings(finalDisplayName);
						}
					});
					
					panel.add(resetMoveToTab_but, "wrap");

					return panel;
				}
			};
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_blocking_spid.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );
			
//			tcp.setTableToolTipText(
//				    "<html>" +
//				        "Background colors:" +
//				        "<ul>" +
//				        "    <li>PINK   - SPID is Blocked by some other SPID that holds a Lock on a database object Table, Page or Row. This is the Lock Victim.</li>" +
//				        "    <li>RED    - SPID is Blocking other SPID's from running, this SPID is Responslibe or the Root Cause of a Blocking Lock.</li>" +
//				        "</ul>" +
//				    "</html>");

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
		name         = CM_NAME__MISSING_STATISTICS;
		displayName  = CM_DESC__MISSING_STATISTICS;
		description  = "<html>" +
		                   "The optimizer executed queries where it didnt find statistics for this objects<br>" +
		                   "<br>" +
		                   "<b>Note:</b> <code>sp_configure 'capture missing statistics'</code>, must be enabled." +
		           		   "<br><br>" +
		           		   "Background colors:" +
		           		   "<ul>" +
		           		   "    <li>PINK   - The column 'colList' is highlighted if it containes '(not indexed!)'.</li>" +
		           		   "</ul>" +
		               "</html>";
		
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
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return new String[] {"capture missing statistics"}; // NO default for this configuration
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				List <String> pkCols = new LinkedList<String>();

				pkCols.add("DBName");
				pkCols.add("ObjectName");
				pkCols.add("colList");

				return pkCols;
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String sql = "exec sp_missing_stats";
				return sql;
			}
		};

		// Need stored proc 'sp_missing_stats'
		// check if it exists: if not it will be created in super.init(conn)
		tmp.addDependsOnStoredProc("sybsystemprocs", "sp_missing_stats", VersionInfo.SP_MISSING_STATS_CRDATE, VersionInfo.class, "sp_missing_stats.sql", AseConnectionUtils.SA_ROLE);

		// If QUERY TIMEOUT is not set, set this to X second, because this can take time. 
		if (tmp.getQueryTimeout() == CountersModel.DEFAULT_sqlQueryTimeout)
			tmp.setQueryTimeout(30);

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (AseTune.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_missing_stats_activity.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );

			// PINK = NO INDEX in string
			if (conf != null) colorStr = conf.getProperty(name+".color.noindex");
			tcp.addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					if ("colList".equals(adapter.getColumnName(adapter.column)))
					{
						String cellVal = adapter.getString();
						if (cellVal != null && cellVal.indexOf("(not indexed!)") >= 0)
							return true;
					}
					return false;
				}
			}, SwingUtils.parseColor(colorStr, Color.PINK), null));

		}
		
		_CMList.add(tmp);


		
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// Query Plan Metrics
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = CM_NAME__QP_METRICS;
		displayName  = CM_DESC__QP_METRICS;
		description  = "<html>" +
		                   "Query Processing Metrics<br>" +
		                   "<br>" +
		                   "<b>Note:</b> <code>sp_configure 'enable metrics capture'</code>, must be enabled." +
		               "</html>";
		
		needVersion  = 15020;
		needCeVersion= 0;
		monTables    = new String[] {"sysquerymetrics"};
		needRole     = new String[] {"sa_role"};
		needConfig   = new String[] {"enable metrics capture"}; // NO default for this configuration
		colsCalcDiff = new String[] {"cnt", "abort_cnt"};
		colsCalcPCT  = new String[] {};
		pkList       = new LinkedList<String>();
		    pkList.add("DBName");
		    pkList.add("id");
		    pkList.add("sequence");

		tmp = new CountersModel(name, null, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				false, true, 60)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return new String[] {"enable metrics capture"}; // NO default for this configuration
			}

			@Override
			protected CmSybMessageHandler createSybMessageHandler()
			{
				CmSybMessageHandler msgHandler = super.createSybMessageHandler();

				// Msg 2528: DBCC execution completed. If DBCC printed error messages, contact a user with System Administrator (SA) role.
				msgHandler.addDiscardMsgNum(2528);

				// Msg 0: FLUSHMETRICS
				msgHandler.addDiscardMsgNum(0);

				return msgHandler;
			}

			@Override
			public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				try 
				{
					MonTablesDictionary mtd = MonTablesDictionary.getInstance();
					mtd.addTable("sysquerymetrics",  "Holds about Query Plan Metrics.");

					mtd.addColumn("sysquerymetrics", "DBName",       "<html>Database name</html>");
					mtd.addColumn("sysquerymetrics", "uid",          "<html>User ID</html>");
					mtd.addColumn("sysquerymetrics", "gid",          "<html>Group ID</html>");
					mtd.addColumn("sysquerymetrics", "id",           "<html>Unique ID</html>");
					mtd.addColumn("sysquerymetrics", "hashkey",      "<html>The hashkey over the SQL query text</html>");
					mtd.addColumn("sysquerymetrics", "sequence",     "<html>Sequence number for a row when multiple rows are required for the SQL text</html>");
					mtd.addColumn("sysquerymetrics", "exec_min",     "<html>Minimum execution time</html>");
					mtd.addColumn("sysquerymetrics", "exec_max",     "<html>Maximum execution time</html>");
					mtd.addColumn("sysquerymetrics", "exec_avg",     "<html>Average execution time</html>");
					mtd.addColumn("sysquerymetrics", "elap_min",     "<html>Minimum elapsed time</html>");
					mtd.addColumn("sysquerymetrics", "elap_max",     "<html>Maximum elapsed time</html>");
					mtd.addColumn("sysquerymetrics", "elap_avg",     "<html>Average elapsed time</html>");
					mtd.addColumn("sysquerymetrics", "lio_min",      "<html>Minimum logical IO</html>");
					mtd.addColumn("sysquerymetrics", "lio_max",      "<html>Maximum logical IO</html>");
					mtd.addColumn("sysquerymetrics", "lio_avg",      "<html>Average logical IO</html>");
					mtd.addColumn("sysquerymetrics", "pio_min",      "<html>Minumum physical IO</html>");
					mtd.addColumn("sysquerymetrics", "pio_max",      "<html>Maximum physical IO</html>");
					mtd.addColumn("sysquerymetrics", "pio_avg",      "<html>Average physical IO</html>");
					mtd.addColumn("sysquerymetrics", "cnt",          "<html>Number of times the query has been executed.</html>");
					mtd.addColumn("sysquerymetrics", "abort_cnt",    "<html>Number of times a query was aborted by Resource Governor as a resource limit was exceeded.</html>");
					mtd.addColumn("sysquerymetrics", "qtext",        "<html>query text</html>");
				}
				catch (NameNotFoundException e) {/*ignore*/}
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				List <String> pkCols = new LinkedList<String>();

				pkCols.add("DBName");
				pkCols.add("id");
				pkCols.add("sequence");

				return pkCols;
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String sql = "exec sp_asetune_qp_metrics";
				return sql;
			}
		};

		// Need stored proc 'sp_missing_stats'
		// check if it exists: if not it will be created in super.init(conn)
		tmp.addDependsOnStoredProc("sybsystemprocs", "sp_asetune_qp_metrics", VersionInfo.SP_ASETUNE_QP_METRICS_CRDATE, VersionInfo.class, "sp_asetune_qp_metrics.sql", AseConnectionUtils.SA_ROLE);

		// If QUERY TIMEOUT is not set, set this to X second, because this can take time. 
		if (tmp.getQueryTimeout() == CountersModel.DEFAULT_sqlQueryTimeout)
			tmp.setQueryTimeout(30);

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (AseTune.hasGUI())
		{
			final String localName = name;
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName(), tmp)
			{
				private static final long	serialVersionUID	= 1L;

				@Override
				protected JPanel createLocalOptionsPanel()
				{
					JPanel panel = SwingUtils.createPanel("QP Metrics", true);
					panel.setLayout(new MigLayout("ins 5, gap 0", "", "0[0]0"));
					panel.setToolTipText(
						"<html>" +
							"Use this panel to controll the Query Plan Metrics subsystem.<br>" +
						"</html>");

					String filterStr = "lio_avg > 100 and elap_max > 10";
					Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
					if (tmpConf != null)
						filterStr = tmpConf.getProperty(localName+".show.filter", "");

					if ( ! filterStr.trim().equals("") )
						getCm().setSqlWhere("'show', '"+filterStr+"'");
					
					final JButton    drop_but   = new JButton("Reset QP Metrics");
					final JLabel     filter_lbl = new JLabel("Filter:");
					final JTextField filter_txt = new JTextField(filterStr);
					final JButton    aseCfg_but = new JButton("Server Config");

					drop_but   .setToolTipText("<html>This will drop/purge all sampled Query Plan Metrics in all databases<br>execute <code>sp_metrics 'drop', '1'</code> in every database</html>");
					filter_lbl.setToolTipText("<html>Just get some row...<br>This is simply a WHERE Clause to the select...<br>Note: ASE will still write statements below these limits to the system tables, see button 'Server Config' to change capture level at the Server side.<br><b>Example:</b> <code>lio_avg > 100 and elap_max > 10<code> </html>");
					filter_txt.setToolTipText("<html>Just get some row...<br>This is simply a WHERE Clause to the select...<br>Note: ASE will still write statements below these limits to the system tables, see button 'Server Config' to change capture level at the Server side.<br><b>Example:</b> <code>lio_avg > 100 and elap_max > 10<code> </html>");
					aseCfg_but.setToolTipText("<html>Set filter in the ASE Server...<br>This means the ASE does <b>not</b> store/writes unnececary statemenst <br>to the system tables, avoiding excessive catalog writes for simple queries.<br><br>Simply open the Ase Configure dialog.</html>");

					panel.add( drop_but,   "wrap 10");
					panel.add( filter_lbl, "split");
					panel.add( filter_txt, "growx, pushx, wrap 10");
					panel.add( aseCfg_but, "wrap");

					drop_but.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							String sql = "sp_asetune_qp_metrics 'drop'";
							if ( ! isMonConnected() )
							{
								SwingUtils.showInfoMessage("Not Connected", "Sorry not connected to the database.");
								return;
							}
							
							try
							{
								Statement stmnt = getMonConnection().createStatement();
								stmnt.executeUpdate(sql);
								stmnt.close();
							}
							catch (SQLException ex)
							{
								SwingUtils.showErrorMessage("Problems", "Problems when dropping/purging the Query Plab Metrics", ex);
								_logger.warn("Problems execute SQL '"+sql+"', Caught: " + e.toString() );
							}
						}
					});

					filter_txt.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							String filterStr = filter_txt.getText().trim();
							getCm().setSqlWhere("'show', '"+filterStr+"'");
							
							// Save config...
							Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
							if (tmpConf != null)
							{
								tmpConf.setProperty(localName+".show.filter", filterStr);
								tmpConf.save();
							}
						}
					});

					aseCfg_but.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							AseConfigMonitoringDialog.showDialog(MainFrame.getInstance(), getMonConnection(), -1);
						}
					});

					return panel;
				}
			};
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_qp_metrics_activity.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );
		}
		
		_CMList.add(tmp);


		
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// Monitor Config
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = CM_NAME__SP_MONITOR_CONFIG;
		displayName  = CM_DESC__SP_MONITOR_CONFIG;
		description  = "<html>" +
							"Executes: sp_monitorconfig 'all'... <br>" +
							"<b>Note</b>: set postpone time to 10 minutes or so, we dont need to sample this that often." +
						"</html>";
		
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
			public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
			{
				return null;
			}

			@Override
			protected CmSybMessageHandler createSybMessageHandler()
			{
				CmSybMessageHandler msgHandler = super.createSybMessageHandler();

				//msgHandler.addDiscardMsgStr("Usage information at date and time");
				msgHandler.addDiscardMsgNum(0);

				return msgHandler;
			}

			@Override
			public List<String> getPkForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				// NO PK is needed, because NO diff calc is done.
				return null;
			}

			@Override
			public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
			{
				String sql = "exec sp_monitorconfig 'all'";
				return sql;
			}
		};

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (AseTune.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName());
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_sp_monitorconfig.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );
		}
		
		_CMList.add(tmp);


		
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// OS iostat
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = CM_NAME__OS_IOSTAT;
		displayName  = CM_DESC__OS_IOSTAT;
		description  = "Executes: 'iostat' on the Operating System";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		tmp = new CounterModelHostMonitor(name, CounterModelHostMonitor.HOSTMON_IOSTAT, null, true);

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (AseTune.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName())
			{
				private static final long	serialVersionUID	= 1L;

				JLabel  l_hostmonThreadNotInit_lbl;
				JLabel  l_hostmonThreadIsRunning_lbl;
				JLabel  l_hostmonThreadIsStopped_lbl;
				JLabel  l_hostmonHostname_lbl;
				JButton l_hostmonStart_but;
				JButton l_hostmonStop_but;

				@Override
				protected JPanel createLocalOptionsPanel()
				{
					JPanel panel = SwingUtils.createPanel("Host Monitor", true);
					panel.setLayout(new MigLayout("ins 5, gap 0", "", "0[0]0"));
					panel.setToolTipText(
						"<html>" +
							"Use this panel to check or controll the underlying Host Monitoring Thread.<br>" +
							"You can Start and/or Stop the hostmon thread.<br>" +
						"</html>");

					l_hostmonThreadNotInit_lbl    = new JLabel("<html><b>Not yet initialized</b></html>");
					l_hostmonThreadIsRunning_lbl  = new JLabel("<html>Is running</html>");
					l_hostmonThreadIsStopped_lbl  = new JLabel("<html><b>Is stopped</b></html>");
					l_hostmonHostname_lbl         = new JLabel();
					l_hostmonStart_but            = new JButton("Start");
					l_hostmonStop_but             = new JButton("Stop");

					l_hostmonThreadNotInit_lbl  .setToolTipText("<html>Indicates wheather the underlying Host Monitor Thread has not yet been initialized.</html>");
					l_hostmonThreadIsRunning_lbl.setToolTipText("<html>Indicates wheather the underlying Host Monitor Thread is running.</html>");
					l_hostmonThreadIsStopped_lbl.setToolTipText("<html>Indicates wheather the underlying Host Monitor Thread is running.</html>");
					l_hostmonHostname_lbl       .setToolTipText("<html>What hostname are we monitoring.</html>");
					l_hostmonStart_but          .setToolTipText("<html>Start the underlying Host Monitor Thread.</html>");
					l_hostmonStop_but           .setToolTipText("<html>Stop the underlying Host Monitor Thread.</html>");

					l_hostmonThreadNotInit_lbl  .setVisible(true);
					l_hostmonThreadIsRunning_lbl.setVisible(false);
					l_hostmonThreadIsStopped_lbl.setVisible(false);
					l_hostmonStart_but          .setVisible(false);
					l_hostmonStop_but           .setVisible(false);

					panel.add( l_hostmonThreadNotInit_lbl,   "hidemode 3, wrap 10");
					panel.add( l_hostmonThreadIsRunning_lbl, "hidemode 3, wrap 10");
					panel.add( l_hostmonThreadIsStopped_lbl, "hidemode 3, wrap 10");
					panel.add( l_hostmonHostname_lbl,        "hidemode 3, wrap 10");
					panel.add( l_hostmonStart_but,           "hidemode 3, wrap");
					panel.add( l_hostmonStop_but,            "hidemode 3, wrap");

					l_hostmonStart_but.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							CountersModel cm = getCm();
							if (cm != null)
							{
								HostMonitor hostMonitor = (HostMonitor) cm.getClientProperty(HostMonitor.PROPERTY_NAME);
								if (hostMonitor != null)
								{
									try
									{
										hostMonitor.setPaused(false);
										hostMonitor.start();
									}
									catch (Exception ex)
									{
										SwingUtils.showErrorMessage("Start", "Problems Starting the Host Monitoring Thread.", ex);
									}
								}
							}
						}
					});

					l_hostmonStop_but.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							CountersModel cm = getCm();
							if (cm != null)
							{
								HostMonitor hostMonitor = (HostMonitor) cm.getClientProperty(HostMonitor.PROPERTY_NAME);
								if (hostMonitor != null)
								{
									hostMonitor.setPaused(true);
									hostMonitor.shutdown();
								}
							}
						}
					});
					return panel;
				}

				@Override
				public void checkLocalComponents()
				{
					CountersModel cm = getCm();
					if (cm != null)
					{
						HostMonitor hostMonitor = (HostMonitor) cm.getClientProperty(HostMonitor.PROPERTY_NAME);
						if (hostMonitor != null)
						{
							boolean isRunning = hostMonitor.isRunning();
							boolean isPaused  = hostMonitor.isPaused();

							l_hostmonThreadIsRunning_lbl.setText("<html>Command: <b>"+hostMonitor.getCommand()+"</b></html>");
							l_hostmonThreadNotInit_lbl  .setVisible( false );
							l_hostmonThreadIsRunning_lbl.setVisible(   isRunning );
							l_hostmonThreadIsStopped_lbl.setVisible( ! isRunning );
							l_hostmonHostname_lbl       .setText("<html> Host: <b>"+hostMonitor.getHostname()+"</b></html>");
							l_hostmonStart_but          .setVisible( ! isRunning );
							l_hostmonStop_but           .setVisible(   isRunning );

							if (isPaused)
								setWatermarkText("Warning: The host monitoring thread is Stopped/Paused!");
						}
						else
						{
							setWatermarkText("Host Monitoring is Disabled or Initializing at Next sample.");
							l_hostmonThreadNotInit_lbl  .setVisible( true );
							l_hostmonThreadIsRunning_lbl.setVisible( false );
							l_hostmonThreadIsStopped_lbl.setVisible( false );
							l_hostmonStart_but          .setVisible( false );
							l_hostmonStop_but           .setVisible( false );
							if (cm.getSampleException() != null)
								setWatermarkText(cm.getSampleException().toString());
						}
					}
				}
			};
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_hostmon_iostat.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );

			tmp.setTabPanel( tcp );
		}
		
		_CMList.add(tmp);


		
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// OS vmstat
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = CM_NAME__OS_VMSTAT;
		displayName  = CM_DESC__OS_VMSTAT;
		description  = "Executes: 'vmstat' on the Operating System";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		tmp = new CounterModelHostMonitor(name, CounterModelHostMonitor.HOSTMON_VMSTAT, null, true);

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (AseTune.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName())
			{
				private static final long	serialVersionUID	= 1L;

				JLabel  l_hostmonThreadNotInit_lbl;
				JLabel  l_hostmonThreadIsRunning_lbl;
				JLabel  l_hostmonThreadIsStopped_lbl;
				JLabel  l_hostmonHostname_lbl;
				JButton l_hostmonStart_but;
				JButton l_hostmonStop_but;

				@Override
				protected JPanel createLocalOptionsPanel()
				{
					JPanel panel = SwingUtils.createPanel("Host Monitor", true);
					panel.setLayout(new MigLayout("ins 5, gap 0", "", "0[0]0"));
					panel.setToolTipText(
						"<html>" +
							"Use this panel to check or controll the underlying Host Monitoring Thread.<br>" +
							"You can Start and/or Stop the hostmon thread.<br>" +
						"</html>");

					l_hostmonThreadNotInit_lbl    = new JLabel("<html><b>Not yet initialized</b></html>");
					l_hostmonThreadIsRunning_lbl  = new JLabel("<html>Is running</html>");
					l_hostmonThreadIsStopped_lbl  = new JLabel("<html><b>Is stopped</b></html>");
					l_hostmonHostname_lbl         = new JLabel();
					l_hostmonStart_but            = new JButton("Start");
					l_hostmonStop_but             = new JButton("Stop");

					l_hostmonThreadNotInit_lbl  .setToolTipText("<html>Indicates wheather the underlying Host Monitor Thread has not yet been initialized.</html>");
					l_hostmonThreadIsRunning_lbl.setToolTipText("<html>Indicates wheather the underlying Host Monitor Thread is running.</html>");
					l_hostmonThreadIsStopped_lbl.setToolTipText("<html>Indicates wheather the underlying Host Monitor Thread is running.</html>");
					l_hostmonHostname_lbl       .setToolTipText("<html>What hostname are we monitoring.</html>");
					l_hostmonStart_but          .setToolTipText("<html>Start the underlying Host Monitor Thread.</html>");
					l_hostmonStop_but           .setToolTipText("<html>Stop the underlying Host Monitor Thread.</html>");

					l_hostmonThreadNotInit_lbl  .setVisible(true);
					l_hostmonThreadIsRunning_lbl.setVisible(false);
					l_hostmonThreadIsStopped_lbl.setVisible(false);
					l_hostmonStart_but          .setVisible(false);
					l_hostmonStop_but           .setVisible(false);

					panel.add( l_hostmonThreadNotInit_lbl,   "hidemode 3, wrap 10");
					panel.add( l_hostmonThreadIsRunning_lbl, "hidemode 3, wrap 10");
					panel.add( l_hostmonThreadIsStopped_lbl, "hidemode 3, wrap 10");
					panel.add( l_hostmonHostname_lbl,        "hidemode 3, wrap 10");
					panel.add( l_hostmonStart_but,           "hidemode 3, wrap");
					panel.add( l_hostmonStop_but,            "hidemode 3, wrap");

					l_hostmonStart_but.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							CountersModel cm = getCm();
							if (cm != null)
							{
								HostMonitor hostMonitor = (HostMonitor) cm.getClientProperty(HostMonitor.PROPERTY_NAME);
								if (hostMonitor != null)
								{
									try
									{
										hostMonitor.setPaused(false);
										hostMonitor.start();
									}
									catch (Exception ex)
									{
										SwingUtils.showErrorMessage("Start", "Problems Starting the Host Monitoring Thread.", ex);
									}
								}
							}
						}
					});

					l_hostmonStop_but.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							CountersModel cm = getCm();
							if (cm != null)
							{
								HostMonitor hostMonitor = (HostMonitor) cm.getClientProperty(HostMonitor.PROPERTY_NAME);
								if (hostMonitor != null)
								{
									hostMonitor.setPaused(true);
									hostMonitor.shutdown();
								}
							}
						}
					});
					return panel;
				}

				@Override
				public void checkLocalComponents()
				{
					CountersModel cm = getCm();
					if (cm != null)
					{
						HostMonitor hostMonitor = (HostMonitor) cm.getClientProperty(HostMonitor.PROPERTY_NAME);
						if (hostMonitor != null)
						{
							boolean isRunning = hostMonitor.isRunning();
							boolean isPaused  = hostMonitor.isPaused();

							l_hostmonThreadIsRunning_lbl.setText("<html>Command: <b>"+hostMonitor.getCommand()+"</b></html>");
							l_hostmonThreadNotInit_lbl  .setVisible( false );
							l_hostmonThreadIsRunning_lbl.setVisible(   isRunning );
							l_hostmonThreadIsStopped_lbl.setVisible( ! isRunning );
							l_hostmonHostname_lbl       .setText("<html> Host: <b>"+hostMonitor.getHostname()+"</b></html>");
							l_hostmonStart_but          .setVisible( ! isRunning );
							l_hostmonStop_but           .setVisible(   isRunning );

							if (isPaused)
								setWatermarkText("Warning: The host monitoring thread is Stopped/Paused!");
						}
						else
						{
							setWatermarkText("Host Monitoring is Disabled or Initializing at Next sample.");
							l_hostmonThreadNotInit_lbl  .setVisible( true );
							l_hostmonThreadIsRunning_lbl.setVisible( false );
							l_hostmonThreadIsStopped_lbl.setVisible( false );
							l_hostmonStart_but          .setVisible( false );
							l_hostmonStop_but           .setVisible( false );
							if (cm.getSampleException() != null)
								setWatermarkText(cm.getSampleException().toString());
						}
					}
				}
			};
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_hostmon_vmstat.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );
	
			tmp.setTabPanel( tcp );
		}
		
		_CMList.add(tmp);


		
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		// OS mpstat
		//-----------------------------------------
		//-----------------------------------------
		//-----------------------------------------
		name         = CM_NAME__OS_MPSTAT;
		displayName  = CM_DESC__OS_MPSTAT;
		description  = "Executes: 'mpstat' on the Operating System";
		
		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");
		
		tmp = new CounterModelHostMonitor(name, CounterModelHostMonitor.HOSTMON_MPSTAT, null, true);

		tmp.setDisplayName(displayName);
		tmp.setDescription(description);
		if (AseTune.hasGUI())
		{
			TabularCntrPanel tcp = new TabularCntrPanel(tmp.getDisplayName())
			{
				private static final long	serialVersionUID	= 1L;

				JLabel  l_hostmonThreadNotInit_lbl;
				JLabel  l_hostmonThreadIsRunning_lbl;
				JLabel  l_hostmonThreadIsStopped_lbl;
				JLabel  l_hostmonHostname_lbl;
				JButton l_hostmonStart_but;
				JButton l_hostmonStop_but;

				@Override
				protected JPanel createLocalOptionsPanel()
				{
					JPanel panel = SwingUtils.createPanel("Host Monitor", true);
					panel.setLayout(new MigLayout("ins 5, gap 0", "", "0[0]0"));
					panel.setToolTipText(
						"<html>" +
							"Use this panel to check or controll the underlying Host Monitoring Thread.<br>" +
							"You can Start and/or Stop the hostmon thread.<br>" +
						"</html>");

					l_hostmonThreadNotInit_lbl    = new JLabel("<html><b>Not yet initialized</b></html>");
					l_hostmonThreadIsRunning_lbl  = new JLabel("<html>Is running</html>");
					l_hostmonThreadIsStopped_lbl  = new JLabel("<html><b>Is stopped</b></html>");
					l_hostmonHostname_lbl         = new JLabel();
					l_hostmonStart_but            = new JButton("Start");
					l_hostmonStop_but             = new JButton("Stop");

					l_hostmonThreadNotInit_lbl  .setToolTipText("<html>Indicates wheather the underlying Host Monitor Thread has not yet been initialized.</html>");
					l_hostmonThreadIsRunning_lbl.setToolTipText("<html>Indicates wheather the underlying Host Monitor Thread is running.</html>");
					l_hostmonThreadIsStopped_lbl.setToolTipText("<html>Indicates wheather the underlying Host Monitor Thread is running.</html>");
					l_hostmonHostname_lbl       .setToolTipText("<html>What hostname are we monitoring.</html>");
					l_hostmonStart_but          .setToolTipText("<html>Start the underlying Host Monitor Thread.</html>");
					l_hostmonStop_but           .setToolTipText("<html>Stop the underlying Host Monitor Thread.</html>");

					l_hostmonThreadNotInit_lbl  .setVisible(true);
					l_hostmonThreadIsRunning_lbl.setVisible(false);
					l_hostmonThreadIsStopped_lbl.setVisible(false);
					l_hostmonStart_but          .setVisible(false);
					l_hostmonStop_but           .setVisible(false);

					panel.add( l_hostmonThreadNotInit_lbl,   "hidemode 3, wrap 10");
					panel.add( l_hostmonThreadIsRunning_lbl, "hidemode 3, wrap 10");
					panel.add( l_hostmonThreadIsStopped_lbl, "hidemode 3, wrap 10");
					panel.add( l_hostmonHostname_lbl,        "hidemode 3, wrap 10");
					panel.add( l_hostmonStart_but,           "hidemode 3, wrap");
					panel.add( l_hostmonStop_but,            "hidemode 3, wrap");

					l_hostmonStart_but.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							CountersModel cm = getCm();
							if (cm != null)
							{
								HostMonitor hostMonitor = (HostMonitor) cm.getClientProperty(HostMonitor.PROPERTY_NAME);
								if (hostMonitor != null)
								{
									try
									{
										hostMonitor.setPaused(false);
										hostMonitor.start();
									}
									catch (Exception ex)
									{
										SwingUtils.showErrorMessage("Start", "Problems Starting the Host Monitoring Thread.", ex);
									}
								}
							}
						}
					});

					l_hostmonStop_but.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							CountersModel cm = getCm();
							if (cm != null)
							{
								HostMonitor hostMonitor = (HostMonitor) cm.getClientProperty(HostMonitor.PROPERTY_NAME);
								if (hostMonitor != null)
								{
									hostMonitor.setPaused(true);
									hostMonitor.shutdown();
								}
							}
						}
					});
					return panel;
				}

				@Override
				public void checkLocalComponents()
				{
					CountersModel cm = getCm();
					if (cm != null)
					{
						HostMonitor hostMonitor = (HostMonitor) cm.getClientProperty(HostMonitor.PROPERTY_NAME);
						if (hostMonitor != null)
						{
							boolean isRunning = hostMonitor.isRunning();
							boolean isPaused  = hostMonitor.isPaused();

							l_hostmonThreadIsRunning_lbl.setText("<html>Command: <b>"+hostMonitor.getCommand()+"</b></html>");
							l_hostmonThreadNotInit_lbl  .setVisible( false );
							l_hostmonThreadIsRunning_lbl.setVisible(   isRunning );
							l_hostmonThreadIsStopped_lbl.setVisible( ! isRunning );
							l_hostmonHostname_lbl       .setText("<html> Host: <b>"+hostMonitor.getHostname()+"</b></html>");
							l_hostmonStart_but          .setVisible( ! isRunning );
							l_hostmonStop_but           .setVisible(   isRunning );

							if (isPaused)
								setWatermarkText("Warning: The host monitoring thread is Stopped/Paused!");
						}
						else
						{
							setWatermarkText("Host Monitoring is Disabled or Initializing at Next sample.");
							l_hostmonThreadNotInit_lbl  .setVisible( true );
							l_hostmonThreadIsRunning_lbl.setVisible( false );
							l_hostmonThreadIsStopped_lbl.setVisible( false );
							l_hostmonStart_but          .setVisible( false );
							l_hostmonStop_but           .setVisible( false );
							if (cm.getSampleException() != null)
								setWatermarkText(cm.getSampleException().toString());
						}
					}
				}
			};
			tcp.setToolTipText( description );
			tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/cm_hostmon_mpstat.png") );
			tcp.setCm(tmp);
			MainFrame.addTcp( tcp );
		
			tmp.setTabPanel( tcp );
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
		createUserDefinedCounterModelHostMonitors();

		// done
		_countersIsCreated = true;
	}

	/**
	 * A User Defined Counter
	 * <p>
	 * Can be defined from the properties file:
	 * <pre>
	 * #-----------------------------------------------------------
	 * # Go and get information about you'r own thing
	 * # Normally used to get a some application specific counters
	 * # If it's a "load" application how many X have we loaded so far
	 * #-----------------------------------------------------------
	 * udc.appSpecificCounter.name=App1 loading
	 * udc.appSpecificCounter.sql=select appName, loadCount=count(*) from appTable
	 * udc.appSpecificCounter.pkPos=1
	 * udc.appSpecificCounter.diff=loadCount
	 * 
	 * 
	 * #-----------------------------------------------------------
	 * # Here is a example where we are not satisfied with the current AseTune counters
	 * # so lets make "a new and improved" tab.
	 * #
	 * # In this example we are also using ASE version dependensies
	 * # meaning newer versions of the ASE server might have "extra" columns in there MDA tables
	 * #
	 * #
	 * # Here is a example where we are not satisfied with the current AseTune counters
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
	 * </pre>
	 */
	private void createUserDefinedCounterModels()
	{
		Configuration conf = null;
		if(AseTune.hasGUI())
		{
//			conf = Configuration.getInstance(Configuration.CONF);
			conf = Configuration.getCombinedConfiguration();
		}
		else
		{
			conf = Configuration.getInstance(Configuration.PCS);
		}
		
		if (conf == null)
			return;

		createUserDefinedCounterModels(conf);
	}

	public static int createUserDefinedCounterModels(Configuration conf)
	{
		if (conf == null)
			throw new IllegalArgumentException("The passed Configuration can't be null");
			//return 1;

		int failCount = 0;

		String prefix = "udc.";

		for (String name : conf.getUniqueSubKeys(prefix, false))
		{
			if (getCmByName(name) != null)
			{
				_logger.info("Already loaded the UDC named '"+name+"', skipping this and continue with next one.");
				continue;
			}
			SplashWindow.drawProgress("Loading: User Defined Counter Model '"+name+"'");
			
			String startKey = prefix + name + ".";
			
			Map<Integer, String> sqlVer = null;

			_logger.info("Loading/Initializing User Defined Counter '"+name+"'.");

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

			// The below values are read in method: addUdcGraph()
//			int     udcGraphType        = -1;
//			boolean udcHasGraph  = conf.getBooleanProperty(startKey + "graph", false);
//			String  udcGraphTypeStr     = conf.getProperty(startKey + "graph.type", "byCol");
//			String  udcGraphName        = conf.getProperty(startKey + "graph.name");
//			String  udcGraphLabel       = conf.getProperty(startKey + "graph.label");
//			String  udcGraphMenuLabel   = conf.getProperty(startKey + "graph.menuLabel");
//			String  udcGraphDataCols    = conf.getProperty(startKey + "graph.data.cols");
//			String  udcGraphDataMethods = conf.getProperty(startKey + "graph.data.methods");
//			String  udcGraphDataLabels  = conf.getProperty(startKey + "graph.data.labels");

			// CHECK for mandatory properties
			if (udcName == null)
			{
				_logger.error("Can't initialize User Defined Counter '"+name+"', no 'name' has been defined.");
				failCount++;
				continue;
			}
			if (udcSql == null)
			{
				_logger.error("Can't initialize User Defined Counter '"+name+"', no 'sql' has been defined.");
				failCount++;
				continue;
			}
			if (udcPkPos != null)
			{
				_logger.error("Can't initialize User Defined Counter '"+name+"', 'pkPos' are not longer supported, please use 'pk' instead.");
				failCount++;
				continue;
			}

			if (udcPk == null)
			{
				_logger.error("Can't initialize User Defined Counter '"+name+"', no 'pk' has been defined.");
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
			}

			// Get me some array variables, that we will "spit" properties into
			List<String>     udcPkList          = new LinkedList<String>();
			String[] udcPkArray         = {};
			String[] udcDiffArray       = {};
			String[] udcTTMTArray       = {};
			String[] udcNeedRoleArray   = {};
			String[] udcNeedConfigArray = {};
			String[] udcPctArray        = {}; // not used, just initialized

			// Split some properties using commas "," as the delimiter  
			if (udcPk         != null) udcPkArray          = udcPk        .split(",");
			if (udcDiff       != null) udcDiffArray        = udcDiff      .split(",");
			if (udcTTMT       != null) udcTTMTArray        = udcTTMT      .split(",");
			if (udcNeedRole   != null) udcNeedRoleArray    = udcNeedRole  .split(",");
			if (udcNeedConfig != null) udcNeedConfigArray  = udcNeedConfig.split(",");

			

			
			// Get rid of extra " " spaces from the split 
			for (int i=0; i<udcPkArray        .length; i++) udcPkArray        [i] = udcPkArray        [i].trim(); 
			for (int i=0; i<udcDiffArray      .length; i++) udcDiffArray      [i] = udcDiffArray      [i].trim(); 
			for (int i=0; i<udcTTMTArray      .length; i++) udcTTMTArray      [i] = udcTTMTArray      [i].trim(); 
			for (int i=0; i<udcNeedRoleArray  .length; i++) udcNeedRoleArray  [i] = udcNeedRoleArray  [i].trim(); 
			for (int i=0; i<udcNeedConfigArray.length; i++) udcNeedConfigArray[i] = udcNeedConfigArray[i].trim(); 

			for (int i=0; i<udcPkArray   .length; i++) udcPkList.add( udcPkArray[i] ); 

			_logger.info("Creating User Defined Counter '"+name+"' with sql '"+udcSql+"'.");

			// Finally create the Counter model and all it's surondings...
			CountersModel cm = new CountersModelUserDefined( name, udcSql, sqlVer,
					udcPkList, //pk1, pk2, pk3, 
					udcDiffArray, udcPctArray, 
					udcTTMTArray, udcNeedRoleArray, udcNeedConfigArray, 
					udcNeedVersion, udcNeedCeVersion, 
					udcNDCT0);

			TabularCntrPanel tcp = null;
			if (AseTune.hasGUI())
			{
				tcp = new TabularCntrPanel(udcDisplayName);
				tcp.setToolTipText( udcDescription );
				tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/ud_counter_activity.png") );
				tcp.setCm(cm);
				MainFrame.addTcp( tcp );
			}
			cm.setTabPanel(tcp);

			cm.setSqlInit(udcSqlInit);
			cm.setSqlClose(udcSqlClose);
			cm.setDisplayName(udcDisplayName);
			cm.setDescription(udcDescription);

			//
			// User defined graphs, if any
			//
			addUdcGraph(cm, udcName, startKey, conf);

			_CMList.add(cm);
		}

		return failCount;
	}

	private static void addUdcGraph(CountersModel cm, String udcName, String startKey, Configuration conf)
	{
		int     udcGraphType        = -1;
		boolean udcHasGraph  = conf.getBooleanProperty(startKey + "graph", false);
		String  udcGraphTypeStr     = conf.getProperty(startKey + "graph.type", "byCol");
		String  udcGraphName        = conf.getProperty(startKey + "graph.name");
		String  udcGraphLabel       = conf.getProperty(startKey + "graph.label");
		String  udcGraphMenuLabel   = conf.getProperty(startKey + "graph.menuLabel");
		String  udcGraphDataCols    = conf.getProperty(startKey + "graph.data.cols");
		String  udcGraphDataMethods = conf.getProperty(startKey + "graph.data.methods");
		String  udcGraphDataLabels  = conf.getProperty(startKey + "graph.data.labels");

		if ( ! udcHasGraph )
			return;

		String name = udcName;
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
			if (AseTune.hasGUI())
			{
				// GRAPH
				TrendGraph tg = new TrendGraph(
						udcGraphName,      // Name of the raph
						udcGraphMenuLabel, // Menu Checkbox text
						udcGraphLabel,     // Label on the graph
						udcGraphDataLabelsArr, // Labels for each plotline
						false, cm, true, -1);
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
	
	
	/**
	 * 
	 */
	private void createUserDefinedCounterModelHostMonitors()
	{
		Configuration conf = null;
		if(AseTune.hasGUI())
		{
//			conf = Configuration.getInstance(Configuration.CONF);
			conf = Configuration.getCombinedConfiguration();
		}
		else
		{
			conf = Configuration.getInstance(Configuration.PCS);
		}
		
		if (conf == null)
			return;

		createUserDefinedCounterModelHostMonitors(conf);
	}

	/**
	 * 
	 * @param conf
	 * @return
	 */
	public static int createUserDefinedCounterModelHostMonitors(Configuration conf)
	{
		if (conf == null)
			throw new IllegalArgumentException("The passed Configuration can't be null");
			//return 1;

		int failCount = 0;

		String prefix = "hostmon.udc.";

		for (String name : conf.getUniqueSubKeys(prefix, false))
		{
			SplashWindow.drawProgress("Loading: Host Monitor User Defined Counter '"+name+"'");
			
			String startKey = prefix + name + ".";

			_logger.debug("STARTING TO Initializing Host Monitor User Defined Counter '"+name+"'.");

//			String  udcName          = conf.getProperty(startKey    + "name");
//			String  udcDisplayName   = conf.getProperty(startKey    + "displayName", udcName);
			String  udcDisplayName   = conf.getProperty(startKey    + "displayName", name);
			String  udcDescription   = conf.getProperty(startKey    + "description", "");

			// The below values are read in method: addUdcGraph()
//			boolean udcHasGraph  = conf.getBooleanProperty(startKey + "graph", false);
//			String  udcGraphTypeStr     = conf.getProperty(startKey + "graph.type", "byCol");
//			String  udcGraphName        = conf.getProperty(startKey + "graph.name");
//			String  udcGraphLabel       = conf.getProperty(startKey + "graph.label");
//			String  udcGraphMenuLabel   = conf.getProperty(startKey + "graph.menuLabel");
//			String  udcGraphDataCols    = conf.getProperty(startKey + "graph.data.cols");
//			String  udcGraphDataMethods = conf.getProperty(startKey + "graph.data.methods");
//			String  udcGraphDataLabels  = conf.getProperty(startKey + "graph.data.labels");

			_logger.info("Creating User Defined Host Monitor Counter '"+name+"'.");

			CountersModel cm = new CounterModelHostMonitor(name, CounterModelHostMonitor.HOSTMON_UD_CLASS, name, false);

			TabularCntrPanel tcp = null;
			if (AseTune.hasGUI())
			{
				tcp = new TabularCntrPanel(udcDisplayName)
				{
					private static final long	serialVersionUID	= 1L;

					JLabel  l_hostmonThreadNotInit_lbl;
					JLabel  l_hostmonThreadIsRunning_lbl;
					JLabel  l_hostmonThreadIsStopped_lbl;
					JButton l_hostmonStart_but;
					JButton l_hostmonStop_but;

					@Override
					protected JPanel createLocalOptionsPanel()
					{
						JPanel panel = SwingUtils.createPanel("Host Monitor", true);
						panel.setLayout(new MigLayout("ins 5, gap 0", "", "0[0]0"));
						panel.setToolTipText(
							"<html>" +
								"Use this panel to check or controll the underlying Host Monitoring Thread.<br>" +
								"You can Start and/or Stop the hostmon thread.<br>" +
							"</html>");

						l_hostmonThreadNotInit_lbl    = new JLabel("<html><b>Not yet initialized</b></html>");
						l_hostmonThreadIsRunning_lbl  = new JLabel("<html>Is running</html>");
						l_hostmonThreadIsStopped_lbl  = new JLabel("<html><b>Is stopped</b></html>");
						l_hostmonStart_but            = new JButton("Start");
						l_hostmonStop_but             = new JButton("Stop");

						l_hostmonThreadNotInit_lbl  .setToolTipText("<html>Indicates wheather the underlying Host Monitor Thread has not yet been initialized.</html>");
						l_hostmonThreadIsRunning_lbl.setToolTipText("<html>Indicates wheather the underlying Host Monitor Thread is running.</html>");
						l_hostmonThreadIsStopped_lbl.setToolTipText("<html>Indicates wheather the underlying Host Monitor Thread is running.</html>");
						l_hostmonStart_but          .setToolTipText("<html>Start the underlying Host Monitor Thread.</html>");
						l_hostmonStop_but           .setToolTipText("<html>Stop the underlying Host Monitor Thread.</html>");

						l_hostmonThreadNotInit_lbl  .setVisible(true);
						l_hostmonThreadIsRunning_lbl.setVisible(false);
						l_hostmonThreadIsStopped_lbl.setVisible(false);
						l_hostmonStart_but          .setVisible(false);
						l_hostmonStop_but           .setVisible(false);

						panel.add( l_hostmonThreadNotInit_lbl,   "hidemode 3, wrap 10");
						panel.add( l_hostmonThreadIsRunning_lbl, "hidemode 3, wrap 10");
						panel.add( l_hostmonThreadIsStopped_lbl, "hidemode 3, wrap 10");
						panel.add( l_hostmonStart_but,           "hidemode 3, wrap");
						panel.add( l_hostmonStop_but,            "hidemode 3, wrap");

						l_hostmonStart_but.addActionListener(new ActionListener()
						{
							public void actionPerformed(ActionEvent e)
							{
								CountersModel cm = getCm();
								if (cm != null)
								{
									HostMonitor hostMonitor = (HostMonitor) cm.getClientProperty(HostMonitor.PROPERTY_NAME);
									if (hostMonitor != null)
									{
										try
										{
											hostMonitor.setPaused(false);
											hostMonitor.start();
										}
										catch (Exception ex)
										{
											SwingUtils.showErrorMessage("Start", "Problems Starting the Host Monitoring Thread.", ex);
										}
									}
								}
							}
						});

						l_hostmonStop_but.addActionListener(new ActionListener()
						{
							public void actionPerformed(ActionEvent e)
							{
								CountersModel cm = getCm();
								if (cm != null)
								{
									HostMonitor hostMonitor = (HostMonitor) cm.getClientProperty(HostMonitor.PROPERTY_NAME);
									if (hostMonitor != null)
									{
										hostMonitor.setPaused(true);
										hostMonitor.shutdown();
									}
								}
							}
						});
						return panel;
					}

					@Override
					public void checkLocalComponents()
					{
						CountersModel cm = getCm();
						if (cm != null)
						{
							HostMonitor hostMonitor = (HostMonitor) cm.getClientProperty(HostMonitor.PROPERTY_NAME);
							if (hostMonitor != null)
							{
								boolean isRunning = hostMonitor.isRunning();
								boolean isPaused  = hostMonitor.isPaused();

								l_hostmonThreadIsRunning_lbl.setText("<html>Command: <b>"+hostMonitor.getCommand()+"</b></html>");
								l_hostmonThreadNotInit_lbl  .setVisible( false );

								if ( hostMonitor.isOsCommandStreaming() )
								{
									l_hostmonThreadIsRunning_lbl.setVisible(   isRunning );
									l_hostmonThreadIsStopped_lbl.setVisible( ! isRunning );
									l_hostmonStart_but          .setVisible( ! isRunning );
									l_hostmonStop_but           .setVisible(   isRunning );
								}
								else
								{
									l_hostmonThreadIsRunning_lbl.setVisible( true );
									l_hostmonThreadIsStopped_lbl.setText("<html>This module has <b>no</b> background thread.<br>And it's executed on every <b>refresh</b>.</html>");
									l_hostmonThreadIsStopped_lbl.setVisible( true );

								//	l_hostmonThreadIsRunning_lbl.setVisible( false );
								//	l_hostmonThreadIsStopped_lbl.setVisible( false );
									l_hostmonStart_but          .setVisible( false );
									l_hostmonStop_but           .setVisible( false );
								}

								if (isPaused)
									setWatermarkText("Warning: The host monitoring thread is Stopped/Paused!");
							}
							else
							{
								setWatermarkText("Host Monitoring is Disabled or Initializing at Next sample.");
								l_hostmonThreadNotInit_lbl  .setVisible( true );
								l_hostmonThreadIsRunning_lbl.setVisible( false );
								l_hostmonThreadIsStopped_lbl.setVisible( false );
								l_hostmonStart_but          .setVisible( false );
								l_hostmonStop_but           .setVisible( false );
								if (cm.getSampleException() != null)
									setWatermarkText(cm.getSampleException().toString());
							}
						}
					}
				};
				tcp.setToolTipText( udcDescription );
				tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/hostmon_ud_counter_activity.png") );
				tcp.setCm(cm);
				MainFrame.addTcp( tcp );
			}
			cm.setTabPanel(tcp);

			cm.setDisplayName(udcDisplayName);
			cm.setDescription(udcDescription);

			// Add a graph
			addUdcGraph(cm, name, startKey, conf);

			_CMList.add(cm);
		}

		return failCount;
	}

	
	
	
	
	
	
	/**
	 * Add some information to the MonTablesDictionary<br>
	 * This will serv as a dictionary for ToolTip
	 */
	public static void initExtraMonTablesDictionary()
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionary.getInstance();
			
			if (mtd == null)
				return;

			String clientXxx = 
			"<html>" +
				"The clientname, clienthostname and clientapplname is assigned by client code with individual information.<br>" +
				"This is useful for differentiating among clients in a system where many clients connect to Adaptive Server using the same name, host name, or application name.<br>" +
				"It can also be used by the client as a trace to indicate what part of the client code that is executing.<br>" +
				"<br>" +
				"Client code has probably executed:<br>" +
				"<code>set [clientname client_name | clienthostname host_name | clientapplname application_name] value</code>" +
			"</html>";

			
			mtd.addTable("sysprocesses",  "Holds information about SybasePID or threads/users logged in to the ASE.");
			mtd.addColumn("sysprocesses", "kpid",           "Kernel Process Identifier.");
			mtd.addColumn("sysprocesses", "fid",            "SPID of the parent process, Family ID.");
			mtd.addColumn("sysprocesses", "spid",           "Session Process Identifier.");
			mtd.addColumn("sysprocesses", "program_name",   "Name of the client program, the client application has to set this to a value otherwise it will be empty.");
			mtd.addColumn("sysprocesses", "dbname",         "Name of the database this user is currently using.");
			mtd.addColumn("sysprocesses", "login",          "Username that is logged in.");
			mtd.addColumn("sysprocesses", "status",         "Status that the SPID is currently in. \n'recv sleep'=waiting for incomming network trafic, \n'sleep'=various reasons but usually waiting for disk io, \n'running'=currently executing on a engine, \n'runnable'=waiting for an engine to execute its work, \n'lock sleep'=waiting for table/page/row locks, \n'sync sleep'=waiting for childs to finnish when parallel execution. \n'...'=and more statuses");
			mtd.addColumn("sysprocesses", "cmd",            "What are we doing. \n'SELECT/INSERT/UPDATE/DELETE'=quite obvious, \n'LOG SUSP'=waiting for log space to be available, \n'COND'=On a IF or equivalent SQL statement, \n'...'=and more");
			mtd.addColumn("sysprocesses", "tran_name",      "More info about what the ASE is doing. \nIf we are in CREATE INDEX it will tell you the index name. \nIf we are in BCP it will give you the tablename etc. \nThis is a good place to look at when you issue ASE administrational commands and want to know whet it really does.");
			mtd.addColumn("sysprocesses", "physical_io",    "Total number of reads/writes, this is flushed in a strange way, so do not trust the Absolute value to much...");
			mtd.addColumn("sysprocesses", "procName",       "If a procedure is executing, this will be the name of the proc. \nif you execute a sp_ proc, it could give you a procedure name that is uncorrect. \nThis due to the fact that I'm using object_name(id,dbid) and dbid is the database the SPID is currently in, while the procId is really reflecting the id of the sp_ proc which usually is located in sybsystemprocs.");
			mtd.addColumn("sysprocesses", "stmtnum",        "Statement number of the SQL batch or the procedure that is currently executing. \nThis might be faulty but it's usually a good indicator.");
			mtd.addColumn("sysprocesses", "linenum",        "Line number of the SQL batch or the procedure that is currently executing. \nThis might be faulty but it's usually a good indicator. \nIf this does NOT move between samples you may have a HEAVY SQL statement to optimize or you may waiting for a blocking lock.");
			mtd.addColumn("sysprocesses", "blocked",        "0 is a good value, otherwise it will be the SPID that we are blocked by, meaning we are waiting for that SPID to release it's locks on some objetc.");
			mtd.addColumn("sysprocesses", "time_blocked",   "Number of seconds we have been blocked by other SPID's. \nThis is not a summary, it shows you how many seconds we have been waiting since we started to wait for the other SPID to finnish.");
			mtd.addColumn("sysprocesses", "clientname",     clientXxx);
			mtd.addColumn("sysprocesses", "clienthostname", clientXxx);
			mtd.addColumn("sysprocesses", "clientapplname", clientXxx);


			mtd.addColumn("sysprocesses", "tempdb_name",          "What tempdb is this SPID using for temporary storage.");
			mtd.addColumn("sysprocesses", "pssinfo_tempdb_pages", "<html>Number of pages that this SPID is using in the tempdb.<br><b>NOTE:</b> When 'ordinary user' tables are shared between users in tempdb, this counter can be faulty,<br> this due to that all spids has a local counter (in the pss structure) which is NOT inc/decremented by other users working on the same 'global' temp table.</html>");

			mtd.addColumn("monProcess",   "tempdb_name",          "What tempdb is this SPID using for temporary storage.");
			mtd.addColumn("monProcess",   "pssinfo_tempdb_pages", "<html>Number of pages that this SPID is using in the tempdb.<br><b>NOTE:</b> When 'ordinary user' tables are shared between users in tempdb, this counter can be faulty,<br> this due to that all spids has a local counter (in the pss structure) which is NOT inc/decremented by other users working on the same 'global' temp table.</html>");

			
//			mtd.addColumn("monDeviceIO", "AvgServ_ms", "Calculated column, Service time on the disk. Formula is: AvgServ_ms = IOTime / (Reads + Writes). If there is few I/O's this value might be a bit off, this due to 'click ticks' is 100 ms by default");

			mtd.addColumn("monProcessStatement", "ExecTime", "The statement has been executing for ## seconds. Calculated value. <b>Formula</b>: ExecTime=datediff(ss, StartTime, getdate())");
			mtd.addColumn("monProcessStatement", "ExecTimeInMs", "The statement has been executing for ## milliseconds. Calculated value. <b>Formula</b>: ExecTimeInMs=datediff(ms, StartTime, getdate())");

			mtd.addTable("sysmonitors",   "This is basically where sp_sysmon gets it's counters.");
			mtd.addColumn("sysmonitors",  "name",         "The internal name of the counter, this is strictly Sybase ASE INTERNAL name. If the description is NOT set it's probably an 'unknown' or not that important counter.");
			mtd.addColumn("sysmonitors",  "instances",    "How many instances of this spinslock actually exists. For examples for 'default data cache' it's the number of 'cache partitions' the cache has.");
//			mtd.addColumn("sysmonitors",  "grabs",        "How many times we where able to succesfuly where able to GET the spinlock.");
//			mtd.addColumn("sysmonitors",  "waits",        "How many times we had to wait for other engines before we where able to grab the spinlock.");
//			mtd.addColumn("sysmonitors",  "spins",        "How many times did we 'wait/spin' for other engies to release the lock. This is basically too many engines spins of the same resource.");
			mtd.addColumn("sysmonitors",  "grabs",        "Spinlock grabs as in attempted grabs for the spinlock - includes waits");
			mtd.addColumn("sysmonitors",  "waits",        "Spinlock waits - usually a good sign of contention");
			mtd.addColumn("sysmonitors",  "spins",        "Spinlock spins - this is the CPU spins that drives up CPU utilization. The higher the spin count, the more CPU usage and the more serious the performance impact of the spinlock on other processes not waiting");
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
	 * Interrupt the sleep and request a new refresh.<br>
	 * Note: this will be disregarded if we not sleeping waiting for a new refresh.
	 */
	public void doRefresh()
	{
		if ( isRefreshing() )
			_logger.info("Sorry, can't do refresh now, we are already in a Performance Counter Refresh");
		else
			doInterrupt();
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

	private boolean _isSleeping = false;
	/** Check if we are sleeping by calling the method sleep() in this class. */
	public boolean isSleeping()
	{
		return _isSleeping;
	}
	/**
	 * Sleep for X ms, should only be used when GUI should be able to be interrupted.
	 * @param ms sleep time
	 * @return true if we were sleept the whole time, false if we were interrupted.
	 */
	public boolean sleep(int ms)
	{
		try 
		{
			_isSleeping = true;
			Thread.sleep(ms); 
			return true;
		}
		catch (InterruptedException ignore)
		{
			if (_logger.isDebugEnabled())
				_logger.debug("Thread '"+Thread.currentThread().getName()+"' was interrupted.", new Exception("Dummy exception, to get callstack from where this happened."));
			return false;
		}
		finally
		{
			_isSleeping = false;
		}
	}

	/** 
	 * Interrupt the thread if it's sleeping using the sleep() method in this class.<br>
	 * If Thread:sleep() has been used it wont do interrupts, just skipping your request.
	 */
	public void doInterruptSleep()
	{
		if ( isSleeping() )
			doInterrupt();
		else
			_logger.info("Sorry, can't interrupt sleep now, because WE ARE NOT SLEEPING.");
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
	public boolean isRefreshing()
	{
		return _isRefreshing;
	}

	/**
	 * Indicate the we are currently in the process of getting counter information from the monitored server
	 * @param true=isRefresing
	 */
	public static void setInRefresh(boolean s)
	{
		_isRefreshing = s;
	}

//	public void clearComponents()
//	{
//		if ( ! _isInitialized )
//			return;
//
//		if (!isRefreshingCounters())
//		{
//			MainFrame.clearSummaryData();
//
//			Iterator<CountersModel> i = _CMList.iterator();
//			while (i.hasNext())
//			{
//				CountersModel cm = i.next();
//				
//				if (cm != null)
//				{
//					cm.clear();
//				}
//			}
//			
//			SummaryPanel.getInstance().clearGraph();
//
////			MainFrame.statusFld.setText("");
//		}
//	}

	
	////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////
	//// database connection
	////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////
	private Connection         _conn                      = null;
	private long               _lastIsClosedCheck         = 0;
	private long               _lastIsClosedRefreshTime   = 2000;

	/**
	 * Do we have a connection to the database?
	 * @return true or false
	 */
	public boolean isMonConnected()
	{
//		return isMonConnected(false, false);
		return isMonConnected(false, true);
	}
	/**
	 * Do we have a connection to the database?
	 * @return true or false
	 */
	public boolean isMonConnected(boolean forceConnectionCheck, boolean closeConnOnFailure)
	{
		if (_conn == null) 
			return false;

		//DEBUG: System.out.print("isMonConnected(forceConnectionCheck="+forceConnectionCheck+", closeConnOnFailure="+closeConnOnFailure+")");
		// Cache the last call for X ms (default 1200 ms)
		if ( ! forceConnectionCheck )
		{
			long diff = System.currentTimeMillis() - _lastIsClosedCheck;
			if ( diff < _lastIsClosedRefreshTime)
			{
				//DEBUG: System.out.println("    <<--- isMonConnected(): not time for refresh. diff='"+diff+"', _lastIsClosedRefreshTime='"+_lastIsClosedRefreshTime+"'.");
				return true;
			}
		}

		// check the connection itself
		try
		{
			//DEBUG: System.out.println("    DO: isClosed");
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

		cleanupMonConnection();
		_conn = null;
	}

	/** override this method to cleanup stuff on disconnect */
	protected void cleanupMonConnection()
	{
	}

	////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////
	//// SSH connection
	////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////
	private SshConnection      _sshConn                      = null;
	private long               _sshLastIsClosedCheck         = 0;
	private long               _sshLastIsClosedRefreshTime   = 1200;

	/**
	 * Do we have a connection to the HOST?
	 * @return true or false
	 */
	public boolean isHostMonConnected()
	{
		return isHostMonConnected(false, false);
	}
	/**
	 * Do we have a connection to the HOST?
	 * @return true or false
	 */
	public boolean isHostMonConnected(boolean forceConnectionCheck, boolean closeConnOnFailure)
	{
		if (_sshConn == null) 
			return false;

		// Cache the last call for X ms (default 1200 ms)
		if ( ! forceConnectionCheck )
		{
			long diff = System.currentTimeMillis() - _sshLastIsClosedCheck;
			if ( diff < _sshLastIsClosedRefreshTime)
			{
				return true;
			}
		}

		// check the connection itself
		try
		{
			if (_sshConn.isClosed())
			{
				if (closeConnOnFailure)
					closeHostMonConnection();
				return false;
			}
		}
		catch (Exception e)
		{
			return false;
		}

		_sshLastIsClosedCheck = System.currentTimeMillis();
		return true;
	}

	/**
	 * Set the <code>SshConnection</code> to use for monitoring.
	 */
	public void setHostMonConnection(SshConnection sshConn)
	{
		_sshConn = sshConn;
//		MainFrame.setStatus(MainFrame.ST_CONNECT);
	}

	/**
	 * Gets the <code>SshConnection</code> to the monitored server.
	 */
	public SshConnection getHostMonConnection()
	{
		return _sshConn;
	}

	/** Gets the <code>SshConnection</code> to the monitored server. */
	public void closeHostMonConnection()
	{
		if (_sshConn == null) 
			return;

		try
		{
			if ( ! _sshConn.isClosed() )
			{
				_sshConn.close();
				if (_logger.isDebugEnabled())
				{
					_logger.debug("SSH Connection closed");
				}
			}
		}
		catch (Exception ev)
		{
			_logger.error("closeHostMonConnection", ev);
		}
		_conn = null;
	}
	

	//==================================================================
	// BEGIN: statistical mehods
	//==================================================================
	/**
	 * Call this whenever we do a new sample
	 * @param mainSampleTime the time of the sample.
	 */
	public void setStatisticsTime(Timestamp mainSampleTime)
	{
		if (_statFirstSampleTime == null)
			_statFirstSampleTime = mainSampleTime;

		_statLastSampleTime = mainSampleTime;
	}

	/**
	 * Reset statistical times (first/last) sample times<br>
	 * This would be called by the statistical "sender" after a disconnect.
	 */
	public void resetStatisticsTime()
	{
		_statFirstSampleTime = null;
		_statLastSampleTime  = null;
	}

	/**
	 * Get first sample time, used by the statistical send<br>
	 * If no samples has been done it will return null
	 */
	public Timestamp getStatisticsFirstSampleTime()
	{
		return _statFirstSampleTime;
	}

	/**
	 * Get last sample time, used by the statistical send
	 * If no samples has been done it will return null
	 */
	public Timestamp getStatisticsLastSampleTime()
	{
		return _statLastSampleTime;
	}
	//==================================================================
	// END: statistical mehods
	//==================================================================
}
