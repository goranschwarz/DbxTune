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
package com.dbxtune.config.dict;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.dbxtune.cm.CmToolTipSupplierDefault;
import com.dbxtune.utils.StringUtil;

public class SqlServerWaitTypeDictionary
{
	/** Instance variable */
	private static SqlServerWaitTypeDictionary _instance = null;

	private HashMap<String, WaitTypeRecord> _waitTypes = new HashMap<String, WaitTypeRecord>();

	public class WaitTypeRecord
	{
		private String _id              = null;
		private String _description     = null;

		public WaitTypeRecord(String id, String description)
		{
			_id          = id;
			_description = description;
		}
		
		@Override
		public String toString()
		{
			return StringUtil.left(_id, 50) + " - " + _description;
		}
	}


	public SqlServerWaitTypeDictionary()
	{
		init();
	}

	public static SqlServerWaitTypeDictionary getInstance()
	{
		if (_instance == null)
			_instance = new SqlServerWaitTypeDictionary();
		return _instance;
	}

	/**
	 * Strips out all HTML and return it as a "plain" text
	 * @param waitName
	 * @return
	 */
	public String getDescriptionPlain(String waitName)
	{
		WaitTypeRecord rec = _waitTypes.get(waitName);
		if (rec != null)
			return StringUtil.stripHtml(rec._description);

		// Compose an empty one
		return "";
//		return "WaitName '"+waitName+"' not found in dictionary.";
	}


	public String getDescriptionHtml(String waitName)
	{
		String extraInfo = "<br><hr>External Description, from: Paul Randal, www.sqlskills.com<br>"
				+ "Open in Tooltip Window:   <A HREF='https://www.sqlskills.com/help/waits/"+waitName+"'>https://www.sqlskills.com/help/waits/"+waitName+"</A><br>"
				+ "Open in External Browser: <A HREF='"+CmToolTipSupplierDefault.OPEN_IN_EXTERNAL_BROWSER+"https://www.sqlskills.com/help/waits/"+waitName+"'>https://www.sqlskills.com/help/waits/"+waitName+"</A><br>"
				+ "</html>";

		WaitTypeRecord rec = _waitTypes.get(waitName);
		if (rec != null)
		{
			String str = rec._description;
			
			str = str.replace("</html>", extraInfo);
			return str;
		}

		// Compose an empty one
		return "<html><code>"+waitName+"</code> not found in dictionary."+extraInfo;
	}


	private void set(WaitTypeRecord rec)
	{
		if ( _waitTypes.containsKey(rec._id))
			System.out.println("Trace flag '"+rec._id+"' already exists. It will be overwritten.");

		_waitTypes.put(rec._id, rec);
	}

	private void add(String id, String description)
	{
		set(new WaitTypeRecord(id, description));
	}

	private void init()
	{
		add("ABR",                                              "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("ASSEMBLY_LOAD",                                    "<html><P>Occurs during exclusive access to assembly loading.</P></html>");
		add("ASYNC_DISKPOOL_LOCK",                              "<html><P>Occurs when there is an attempt to synchronize parallel threads that are performing tasks such as creating or initializing a file.</P></html>");
		add("ASYNC_IO_COMPLETION",                              "<html><P>Occurs when a task is waiting for I/Os to finish.</P></html>");
		add("ASYNC_NETWORK_IO",                                 "<html><P>Occurs on network writes when the task is blocked behind the network. Verify that the client is processing data from the server.</P></html>");
		add("AUDIT_GROUPCACHE_LOCK",                            "<html><P>Occurs when there is a wait on a lock that controls access to a special cache. The cache contains information about which audits are being used to audit each audit action group.</P></html>");
		add("AUDIT_LOGINCACHE_LOCK",                            "<html><P>Occurs when there is a wait on a lock that controls access to a special cache. The cache contains information about which audits are being used to audit login audit action groups.</P></html>");
		add("AUDIT_ON_DEMAND_TARGET_LOCK",                      "<html><P>Occurs when there is a wait on a lock that is used to ensure single initialization of audit related Extended Event targets.</P></html>");
		add("AUDIT_XE_SESSION_MGR",                             "<html><P>Occurs when there is a wait on a lock that is used to synchronize the starting and stopping of audit related Extended Events sessions.</P></html>");
		add("BACKUP",                                           "<html><P>Occurs when a task is blocked as part of backup processing.</P></html>");
		add("BACKUP_OPERATOR",                                  "<html><P>Occurs when a task is waiting for a tape mount. To view the tape status, query <A href=\"https://msdn.microsoft.com/en-us/library/ms176071.aspx\">sys.dm_io_backup_tapes</A>. If a mount operation is not pending, this wait type may indicate a hardware problem with the tape drive.</P></html>");
		add("BACKUPBUFFER",                                     "<html><P>Occurs when a backup task is waiting for data, or is waiting for a buffer in which to store data. This type is not typical, except when a task is waiting for a tape mount.</P></html>");
		add("BACKUPIO",                                         "<html><P>Occurs when a backup task is waiting for data, or is waiting for a buffer in which to store data. This type is not typical, except when a task is waiting for a tape mount.</P></html>");
		add("BACKUPTHREAD",                                     "<html><P>Occurs when a task is waiting for a backup task to finish. Wait times may be long, from several minutes to several hours. If the task that is being waited on is in an I/O process, this type does not indicate a problem.</P></html>");
		add("BAD_PAGE_PROCESS",                                 "<html><P>Occurs when the background suspect page logger is trying to avoid running more than every five seconds. Excessive suspect pages cause the logger to run frequently.</P></html>");
		add("BROKER_CONNECTION_RECEIVE_TASK",                   "<html><P>Occurs when waiting for access to receive a message on a connection endpoint. Receive access to the endpoint is serialized.</P></html>");
		add("BROKER_ENDPOINT_STATE_MUTEX",                      "<html><P>Occurs when there is contention to access the state of a Service Broker connection endpoint. Access to the state for changes is serialized.</P></html>");
		add("BROKER_EVENTHANDLER",                              "<html><P>Occurs when a task is waiting in the primary event handler of the Service Broker. This should occur very briefly.</P></html>");
		add("BROKER_INIT",                                      "<html><P>Occurs when initializing Service Broker in each active database. This should occur infrequently.</P></html>");
		add("BROKER_MASTERSTART",                               "<html><P>Occurs when a task is waiting for the primary event handler of the Service Broker to start. This should occur very briefly.</P></html>");
		add("BROKER_RECEIVE_WAITFOR",                           "<html><P>Occurs when the RECEIVE WAITFOR is waiting. This is typical if no messages are ready to be received.</P></html>");
		add("BROKER_REGISTERALLENDPOINTS",                      "<html><P>Occurs during the initialization of a Service Broker connection endpoint. This should occur very briefly.</P></html>");
		add("BROKER_SERVICE",                                   "<html><P>Occurs when the Service Broker destination list that is associated with a target service is updated or re-prioritized.</P></html>");
		add("BROKER_SHUTDOWN",                                  "<html><P>Occurs when there is a planned shutdown of Service Broker. This should occur very briefly, if at all.</P></html>");
		add("BROKER_TASK_STOP",                                 "<html><P>Occurs when the Service Broker queue task handler tries to shut down the task. The state check is serialized and must be in a running state beforehand.</P></html>");
		add("BROKER_TO_FLUSH",                                  "<html><P>Occurs when the Service Broker lazy flusher flushes the in-memory transmission objects to a work table.</P></html>");
		add("BROKER_TRANSMITTER",                               "<html><P>Occurs when the Service Broker transmitter is waiting for work.</P></html>");
		add("BUILTIN_HASHKEY_MUTEX",                            "<html><P>May occur after startup of instance, while internal data structures are initializing. Will not recur once data structures have initialized.</P></html>");
		add("CHECK_PRINT_RECORD",                               "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("CHECKPOINT_QUEUE",                                 "<html><P>Occurs while the checkpoint task is waiting for the next checkpoint request.</P></html>");
		add("CHKPT",                                            "<html><P>Occurs at server startup to tell the checkpoint thread that it can start.</P></html>");
		add("CLEAR_DB",                                         "<html><P>Occurs during operations that change the state of a database, such as opening or closing a database.</P></html>");
		add("CLR_AUTO_EVENT",                                   "<html><P>Occurs when a task is currently performing common language runtime (CLR) execution and is waiting for a particular autoevent to be initiated. Long waits are typical, and do not indicate a problem.</P></html>");
		add("CLR_CRST",                                         "<html><P>Occurs when a task is currently performing CLR execution and is waiting to enter a critical section of the task that is currently being used by another task.</P></html>");
		add("CLR_JOIN",                                         "<html><P>Occurs when a task is currently performing CLR execution and waiting for another task to end. This wait state occurs when there is a join between tasks.</P></html>");
		add("CLR_MANUAL_EVENT",                                 "<html><P>Occurs when a task is currently performing CLR execution and is waiting for a specific manual event to be initiated.</P></html>");
		add("CLR_MEMORY_SPY",                                   "<html><P>Occurs during a wait on lock acquisition for a data structure that is used to record all virtual memory allocations that come from CLR. The data structure is locked to maintain its integrity if there is parallel access. </P></html>");
		add("CLR_MONITOR",                                      "<html><P>Occurs when a task is currently performing CLR execution and is waiting to obtain a lock on the monitor.</P></html>");
		add("CLR_RWLOCK_READER",                                "<html><P>Occurs when a task is currently performing CLR execution and is waiting for a reader lock.</P></html>");
		add("CLR_RWLOCK_WRITER",                                "<html><P>Occurs when a task is currently performing CLR execution and is waiting for a writer lock.</P></html>");
		add("CLR_SEMAPHORE",                                    "<html><P>Occurs when a task is currently performing CLR execution and is waiting for a semaphore.</P></html>");
		add("CLR_TASK_START",                                   "<html><P>Occurs while waiting for a CLR task to complete startup.</P></html>");
		add("CLRHOST_STATE_ACCESS",                             "<html><P>Occurs where there is a wait to acquire exclusive access to the CLR-hosting data structures. This wait type occurs while setting up or tearing down the CLR runtime.</P></html>");
		add("CMEMTHREAD",                                       "<html><P>Occurs when a task is waiting on a thread-safe memory object. The wait time might increase when there is contention caused by multiple tasks trying to allocate memory from the same memory object.</P></html>");
		add("CXPACKET",                                         "<html><P>Occurs with parallel query plans when trying to synchronize the query processor exchange iterator. If waiting is excessive and cannot be reduced by tuning the query (such as adding indexes), consider adjusting the cost threshold for parallelism or lowering the degree of parallelism.</P></html>");
		add("CXROWSET_SYNC",                                    "<html><P>Occurs during a parallel range scan.</P></html>");
		add("DAC_INIT",                                         "<html><P>Occurs while the dedicated administrator connection is initializing.</P></html>");
		add("DBMIRROR_DBM_EVENT",                               "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("DBMIRROR_DBM_MUTEX",                               "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("DBMIRROR_EVENTS_QUEUE",                            "<html><P>Occurs when database mirroring waits for events to process.</P></html>");
		add("DBMIRROR_SEND",                                    "<html><P>Occurs when a task is waiting for a communications backlog at the network layer to clear to be able to send messages. Indicates that the communications layer is starting to become overloaded and affect the database mirroring data throughput.</P></html>");
		add("DBMIRROR_WORKER_QUEUE",                            "<html><P>Indicates that the database mirroring worker task is waiting for more work.</P></html>");
		add("DBMIRRORING_CMD",                                  "<html><P>Occurs when a task is waiting for log records to be flushed to disk. This wait state is expected to be held for long periods of time.</P></html>");
		add("DEADLOCK_ENUM_MUTEX",                              "<html><P>Occurs when the deadlock monitor and <CODE>sys.dm_os_waiting_tasks</CODE> try to make sure that SQL Server is not running multiple deadlock searches at the same time.</P></html>");
		add("DEADLOCK_TASK_SEARCH",                             "<html><P>Large waiting time on this resource indicates that the server is executing queries on top of <CODE>sys.dm_os_waiting_tasks</CODE>, and these queries are blocking deadlock monitor from running deadlock search. This wait type is used by deadlock monitor only. Queries on top of <CODE>sys.dm_os_waiting_tasks</CODE> use DEADLOCK_ENUM_MUTEX.</P></html>");
		add("DEBUG",                                            "<html><P>Occurs during Transact-SQL and CLR debugging for internal synchronization.</P></html>");
		add("DISABLE_VERSIONING",                               "<html><P>Occurs when SQL Server polls the version transaction manager to see whether the timestamp of the earliest active transaction is later than the timestamp of when the state started changing. If this is this case, all the snapshot transactions that were started before the ALTER DATABASE statement was run have finished. This wait state is used when SQL Server disables versioning by using the ALTER DATABASE statement.</P></html>");
		add("DISKIO_SUSPEND",                                   "<html><P>Occurs when a task is waiting to access a file when an external backup is active. This is reported for each waiting user process. A count larger than five per user process may indicate that the external backup is taking too much time to finish.</P></html>");
		add("DISPATCHER_QUEUE_SEMAPHORE",                       "<html><P>Occurs when a thread from the dispatcher pool is waiting for more work to process. The wait time for this wait type is expected to increase when the dispatcher is idle.</P></html>");
		add("DLL_LOADING_MUTEX",                                "<html><P>Occurs once while waiting for the XML parser DLL to load.</P></html>");
		add("DROPTEMP",                                         "<html><P>Occurs between attempts to drop a temporary object if the previous attempt failed. The wait duration grows exponentially with each failed drop attempt.</P></html>");
		add("DTC",                                              "<html><P>Occurs when a task is waiting on an event that is used to manage state transition. This state controls when the recovery of Microsoft Distributed Transaction Coordinator (MS DTC) transactions occurs after SQL Server receives notification that the MS DTC service has become unavailable.</P><P>This state also describes a task that is waiting when a commit of a MS DTC transaction is initiated by SQL Server and SQL Server is waiting for the MS DTC commit to finish.</P></html>");
		add("DTC_ABORT_REQUEST",                                "<html><P>Occurs in a MS DTC worker session when the session is waiting to take ownership of a MS DTC transaction. After MS DTC owns the transaction, the session can roll back the transaction. Generally, the session will wait for another session that is using the transaction.</P></html>");
		add("DTC_RESOLVE",                                      "<html><P>Occurs when a recovery task is waiting for the <CODE>master</CODE> database in a cross-database transaction so that the task can query the outcome of the transaction.</P></html>");
		add("DTC_STATE",                                        "<html><P>Occurs when a task is waiting on an event that protects changes to the internal MS DTC global state object. This state should be held for very short periods of time.</P></html>");
		add("DTC_TMDOWN_REQUEST",                               "<html><P>Occurs in a MS DTC worker session when SQL Server receives notification that the MS DTC service is not available. First, the worker will wait for the MS DTC recovery process to start. Then, the worker waits to obtain the outcome of the distributed transaction that the worker is working on. This may continue until the connection with the MS DTC service has been reestablished.</P></html>");
		add("DTC_WAITFOR_OUTCOME",                              "<html><P>Occurs when recovery tasks wait for MS DTC to become active to enable the resolution of prepared transactions.</P></html>");
		add("DUMP_LOG_COORDINATOR",                             "<html><P>Occurs when a main task is waiting for a subtask to generate data. Ordinarily, this state does not occur. A long wait indicates an unexpected blockage. The subtask should be investigated.</P></html>");
		add("DUMPTRIGGER",                                      "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("EC",                                               "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("EE_PMOLOCK",                                       "<html><P>Occurs during synchronization of certain types of memory allocations during statement execution.</P></html>");
		add("EE_SPECPROC_MAP_INIT",                             "<html><P>Occurs during synchronization of internal procedure hash table creation. This wait can only occur during the initial accessing of the hash table after the SQL Server instance starts.</P></html>");
		add("ENABLE_VERSIONING",                                "<html><P>Occurs when SQL Server waits for all update transactions in this database to finish before declaring the database ready to transition to snapshot isolation allowed state. This state is used when SQL Server enables snapshot isolation by using the ALTER DATABASE statement.</P></html>");
		add("ERROR_REPORTING_MANAGER",                          "<html><P>Occurs during synchronization of multiple concurrent error log initializations.</P></html>");
		add("EXCHANGE",                                         "<html><P>Occurs during synchronization in the query processor exchange iterator during parallel queries.</P></html>");
		add("EXECSYNC",                                         "<html><P>Occurs during parallel queries while synchronizing in query processor in areas not related to the exchange iterator. Examples of such areas are bitmaps, large binary objects (LOBs), and the spool iterator. LOBs may frequently use this wait state.</P></html>");
		add("EXECUTION_PIPE_EVENT_INTERNAL",                    "<html><P>Occurs during synchronization between producer and consumer parts of batch execution that are submitted through the connection context.</P></html>");
		add("FAILPOINT",                                        "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("FCB_REPLICA_READ",                                 "<html><P>Occurs when the reads of a snapshot (or a temporary snapshot created by DBCC) sparse file are synchronized.</P></html>");
		add("FCB_REPLICA_WRITE",                                "<html><P>Occurs when the pushing or pulling of a page to a snapshot (or a temporary snapshot created by DBCC) sparse file is synchronized.</P></html>");
		add("FS_FC_RWLOCK",                                     "<html><P>Occurs when there is a wait by the FILESTREAM garbage collector to do either of the following:</P><UL><LI><P>Disable garbage collection (used by backup and restore).</P></LI><LI><P>Execute one cycle of the FILESTREAM garbage collector.</P></LI></UL></html>");
		add("FS_GARBAGE_COLLECTOR_SHUTDOWN",                    "<html><P>Occurs when the FILESTREAM garbage collector is waiting for cleanup tasks to be completed.</P></html>");
		add("FS_HEADER_RWLOCK",                                 "<html><P>Occurs when there is a wait to acquire access to the FILESTREAM header of a FILESTREAM data container to either read or update contents in the FILESTREAM header file (Filestream.hdr).</P></html>");
		add("FS_LOGTRUNC_RWLOCK",                               "<html><P>Occurs when there is a wait to acquire access to FILESTREAM log truncation to do either of the following:</P><UL><LI><P>Temporarily disable FILESTREAM log (FSLOG) truncation (used by backup and restore).</P></LI><LI><P>Execute one cycle of FSLOG truncation.</P></LI></UL></html>");
		add("FSA_FORCE_OWN_XACT",                               "<html><P>Occurs when a FILESTREAM file I/O operation needs to bind to the associated transaction, but the transaction is currently owned by another session.</P></html>");
		add("FSAGENT",                                          "<html><P>Occurs when a FILESTREAM file I/O operation is waiting for a FILESTREAM agent resource that is being used by another file I/O operation.</P></html>");
		add("FSTR_CONFIG_MUTEX",                                "<html><P>Occurs when there is a wait for another FILESTREAM feature reconfiguration to be completed.</P></html>");
		add("FSTR_CONFIG_RWLOCK",                               "<html><P>Occurs when there is a wait to serialize access to the FILESTREAM configuration parameters.</P></html>");
		add("FT_COMPROWSET_RWLOCK ",                            "<html><P>Full-text is waiting on fragment metadata operation. Documented for informational purposes only. Not supported. Future compatibility is not guaranteed. </P></html>");
		add("FT_IFTS_RWLOCK",                                   "<html><P>Full-text is waiting on internal synchronization. Documented for informational purposes only. Not supported. Future compatibility is not guaranteed. </P></html>");
		add("FT_IFTS_SCHEDULER_IDLE_WAIT",                      "<html><P>Full-text scheduler sleep wait type. The scheduler is idle. </P></html>");
		add("FT_IFTSHC_MUTEX",                                  "<html><P>Full-text is waiting on an fdhost control operation. Documented for informational purposes only. Not supported. Future compatibility is not guaranteed. </P></html>");
		add("FT_IFTSISM_MUTEX",                                 "<html><P>Full-text is waiting on communication operation. Documented for informational purposes only. Not supported. Future compatibility is not guaranteed.  </P></html>");
		add("FT_MASTER_MERGE",                                  "<html><P>Full-text is waiting on master merge operation. Documented for informational purposes only. Not supported. Future compatibility is not guaranteed. </P></html>");
		add("FT_METADATA_MUTEX",                                "<html><P>Documented for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("FT_RESTART_CRAWL",                                 "<html><P>Occurs when a full-text crawl needs to restart from a last known good point to recover from a transient failure. The wait lets the worker tasks currently working on that population to complete or exit the current step.</P></html>");
		add("FULLTEXT GATHERER",                                "<html><P>Occurs during synchronization of full-text operations.</P></html>");
		add("GUARDIAN",                                         "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("HADR_AG_MUTEX",                                    "<html><P>Occurs when an AlwaysOn DDL statement or Windows Server Failover Clustering command is waiting for exclusive read/write access to the configuration of an availability group.�</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_AR_CRITICAL_SECTION_ENTRY",                   "<html><P>Occurs when an AlwaysOn DDL statement or Windows Server Failover Clustering command is waiting for exclusive read/write access to the runtime state of the local replica of the associated availability group.��</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_AR_MANAGER_MUTEX",                            "<html><P>Occurs when an availability replica shutdown is waiting for startup to complete or an availability replica startup is waiting for shutdown to complete.  Internal use only. </P><TABLE><TBODY><TR><TH align=\"left\"><SPAN><IMG title=\"System_CAPS_note\" id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" xmlns=\"\"></CODE><SPAN>Note </CODE></TH></TR><TR><TD><P>Availability replica shutdown is initiated either by SQL Server shutdown or by SQL Server handling the loss of quorum by the Windows Server Failover Clustering node. Availability replica startup is initiated either by SQL Server startup or by SQL Server recovering from the loss of quorum by the Windows Server Failover Clustering node. �</P></TD></TR></TBODY></TABLE><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_ARCONTROLLER_NOTIFICATIONS_SUBSCRIBER_LIST",  "<html><P>The publisher for an availability replica event (such as a state change or configuration change) is waiting for exclusive read/write access to the list of event subscribers.� Internal use only. </P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_BACKUP_BULK_LOCK",                            "<html><P>The AlwaysOn primary database received a backup request from a secondary database and is waiting for the background thread to finish processing the request on acquiring or releasing the BulkOp lock.��</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_BACKUP_QUEUE",                                "<html><P>The backup background thread of the AlwaysOn primary database is waiting for a new work request from the secondary database. (typically, this occurs when the primary database is holding the BulkOp log and is waiting for the secondary database to indicate that the primary database can release the lock).��</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_CLUSAPI_CALL",                                "<html><P>A SQL Server thread is waiting to switch from non-preemptive mode (scheduled by SQL Server) to preemptive mode (scheduled by the operating system) in order to invoke Windows Server Failover Clustering APIs.��</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_COMPRESSED_CACHE_SYNC",                       "<html><P>Waiting for access to the cache of compressed log blocks that is used to avoid redundant compression of the log blocks sent to multiple secondary databases.��</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_DATABASE_FLOW_CONTROL",                       "<html><P>Waiting for messages to be sent to the partner when the maximum number of queued messages has been reached.  Indicates that the log scans are running faster than the network sends. This is an issue only if network sends are slower than expected. �</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_DATABASE_VERSIONING_STATE",                   "<html><P>Occurs on the versioning state change of an AlwaysOn secondary database. This wait is for internal data structures and is usually is very short with no direct effect on data access. �</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_DATABASE_WAIT_FOR_RESTART",                   "<html><P>Waiting for the database to restart under AlwaysOn Availability Groups control.  Under normal conditions, this is not a customer issue because waits are expected here. �</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_DATABASE_WAIT_FOR_TRANSITION_TO_VERSIONING",  "<html><P>A query on object(s) in a readable secondary database of an AlwaysOn availability group is blocked on row versioning while waiting for commit or rollback of all transactions that were in-flight when the secondary replica was enabled for read workloads. This wait type guarantees that row versions are available before execution of a query under snapshot isolation. ���</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_DB_COMMAND",                                  "<html><P>Waiting for responses to conversational messages (which require an explicit response from the other side, using the AlwaysOn conversational message infrastructure).  A number of different message types use this wait type.��</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_DB_OP_COMPLETION_SYNC",                       "<html><P>Waiting for responses to conversational messages (which require an explicit response from the other side, using the AlwaysOn conversational message infrastructure).  A number of different message types use this wait type.��</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_DB_OP_START_SYNC",                            "<html><P>An AlwaysOn DDL statement or a Windows Server Failover Clustering command is waiting for serialized access to an availability database and its runtime state.��</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_DBR_SUBSCRIBER",                              "<html><P>The publisher for an availability replica event (such as a state change or configuration change) is waiting for exclusive read/write access to the runtime state of an event subscriber that corresponds to an availability database.� Internal use only. </P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_DBR_SUBSCRIBER_FILTER_LIST",                  "<html><P>The publisher for an availability replica event (such as a state change or configuration change) is waiting for exclusive read/write access to the list of event subscribers that correspond to availability databases.� Internal use only. </P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_DBSTATECHANGE_SYNC",                          "<html><P>Concurrency control wait for updating the internal state of the database replica. �</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_FILESTREAM_BLOCK_FLUSH",                      "<html><P>The FILESTREAM AlwaysOn transport manager is waiting until processing of a log block is finished.��</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_FILESTREAM_FILE_CLOSE",                       "<html><P>The FILESTREAM AlwaysOn transport manager is waiting until the next FILESTREAM file gets processed and its handle gets closed.��</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_FILESTREAM_FILE_REQUEST",                     "<html><P>An AlwaysOn secondary replica is waiting for the primary replica to send all requested FILESTREAM files during UNDO.��</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_FILESTREAM_IOMGR",                            "<html><P>The FILESTREAM AlwaysOn transport manager is waiting for R/W lock that protects the FILESTREAM AlwaysOn I/O manager during startup or shutdown.��</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_FILESTREAM_IOMGR_IOCOMPLETION",               "<html><P>The FILESTREAM AlwaysOn I/O manager is waiting for I/O completion.��</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_FILESTREAM_MANAGER",                          "<html><P>The FILESTREAM AlwaysOn transport manager is waiting for the R/W lock that protects the FILESTREAM AlwaysOn transport manager during startup or shutdown.��</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_GROUP_COMMIT",                                "<html><P>Transaction commit processing is waiting to allow a group commit so that multiple commit log records can be put into a single log block. This wait is an expected condition that optimizes the log I/O, capture, and send operations. �</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_LOGCAPTURE_SYNC",                             "<html><P>Concurrency control around the log capture or apply object when creating or destroying scans.  This is an expected wait when partners change state or connection status. �</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_LOGCAPTURE_WAIT",                             "<html><P>Waiting for log records to become available. Can occur either when waiting for new log records to be generated by connections or for I/O completion when reading log not in the cache.  This is an expected wait if the log scan is caught up to the end of log or is reading from disk. �</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_LOGPROGRESS_SYNC",                            "<html><P>Concurrency control wait when updating the log progress status of database replicas. �</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_NOTIFICATION_DEQUEUE",                        "<html><P>A background task that processes Windows Server Failover Clustering notifications is waiting for the next notification.� Internal use only. </P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_NOTIFICATION_WORKER_EXCLUSIVE_ACCESS",        "<html><P>The AlwaysOn availability replica manager is waiting for serialized access to the runtime state of a background task that processes Windows Server Failover Clustering notifications.� Internal use only. </P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_NOTIFICATION_WORKER_STARTUP_SYNC",            "<html><P>A background task is waiting for the completion of the startup of a background task that processes Windows Server Failover Clustering notifications.� Internal use only. </P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_NOTIFICATION_WORKER_TERMINATION_SYNC",        "<html><P>A background task is waiting for the termination of a background task that processes Windows Server Failover Clustering notifications.� Internal use only. </P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_PARTNER_SYNC",                                "<html><P>Concurrency control wait on the partner list. �</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_READ_ALL_NETWORKS",                           "<html><P>Waiting to get read or write access to the list of WSFC networks. Internal use only. </P><TABLE><TBODY><TR><TH align=\"left\"><SPAN><IMG title=\"System_CAPS_note\" id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" xmlns=\"\"></CODE><SPAN>Note </CODE></TH></TR><TR><TD><P>The engine keeps a list of WSFC networks that is used in dynamic management views (such as <A href=\"https://msdn.microsoft.com/en-us/library/hh213657.aspx\">sys.dm_hadr_cluster_networks</A>) or to validate AlwaysOn Transact-SQL statements that reference WSFC network information. This list is updated upon engine startup, WSFC related notifications, and internal AlwaysOn restart (for example, losing and regaining of WSFC quorum). Tasks will usually be blocked when an update in that list is in progress. �</P></TD></TR></TBODY></TABLE><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_RECOVERY_WAIT_FOR_CONNECTION",                "<html><P>Waiting for the secondary database to connect to the primary database before running recovery. This is an expected wait, which can lengthen if the connection to the primary is slow to establish. �</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_RECOVERY_WAIT_FOR_UNDO",                      "<html><P>Database recovery is waiting for the secondary database to finish the reverting and initializing phase to bring it back to the common log point with the primary database.  This is an expected wait after failovers.Undo progress can be tracked through the Windows System Monitor (perfmon.exe) and dynamic management views. �</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_REPLICAINFO_SYNC",                            "<html><P>Waiting for concurrency control to update the current replica state. �</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_SYNC_COMMIT",                                 "<html><P>Waiting for transaction commit processing for the synchronized secondary databases to harden the log. This wait is also reflected by the Transaction Delay performance counter. This wait type is expected for synchronized availability groups and indicates the time to send, write, and acknowledge log to the secondary databases. �</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_SYNCHRONIZING_THROTTLE",                      "<html><P>Waiting for transaction commit processing to allow a synchronizing secondary database to catch up to the primary end of log in order to transition to the synchronized state.  This is an expected wait when a secondary database is catching up. �</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_TDS_LISTENER_SYNC",                           "<html><P>Either the internal AlwaysOn system or the WSFC cluster will request that listeners are started or stopped. The processing of this request is always asynchronous, and there is a mechanism to remove redundant requests. There are also moments that this process is suspended because of configuration changes. All waits related with this listener synchronization mechanism use this wait type.  Internal use only. </P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_TDS_LISTENER_SYNC_PROCESSING",                "<html><P>Used at the end of an AlwaysOn Transact-SQL statement that requires starting and/or stopping an�<A href=\"https://msdn.microsoft.com/en-us/library/hh213417.aspx\">availability group listener</A>. Since the start/stop operation is done asynchronously, the user thread will block using this wait type until the situation of the listener is known.  </P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_TIMER_TASK",                                  "<html><P>Waiting to get the lock on the timer task object and is also used for the actual waits between times that work is being performed.  For example, for a task that runs every 10 seconds, after one execution, AlwaysOn Availability Groups waits  about 10 seconds to reschedule the task, and the wait is included here.���</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_TRANSPORT_DBRLIST",                           "<html><P>Waiting for access to the transport layer's database replica list.  Used for the spinlock that grants access to it.��</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_TRANSPORT_FLOW_CONTROL",                      "<html><P>Waiting when the number of outstanding unacknowledged AlwaysOn messages is over the out flow control threshold. This is on an availability replica-to-replica basis (not on a database-to-database basis).��</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_TRANSPORT_SESSION",                           "<html><P>AlwaysOn Availability Groups is waiting while changing or accessing the underlying transport state.��</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_WORK_POOL",                                   "<html><P>Concurrency control wait on the AlwaysOn Availability Groups background work task object. �</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_WORK_QUEUE",                                  "<html><P>AlwaysOn Availability Groups background worker thread waiting for new work to be assigned. This is an expected wait when there are ready workers waiting for new work, which is the normal state. �</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HADR_XRF_STACK_ACCESS",                            "<html><P>Accessing (look up, add, and delete) the extended recovery fork stack for an AlwaysOn availability database. �</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("HTTP_ENUMERATION",                                 "<html><P>Occurs at startup to enumerate the HTTP endpoints to start HTTP.</P></html>");
		add("HTTP_START",                                       "<html><P>Occurs when a connection is waiting for HTTP to complete initialization.</P></html>");
		add("IMPPROV_IOWAIT",                                   "<html><P>Occurs when SQL Server waits for a bulkload I/O to finish.</P></html>");
		add("INTERNAL_TESTING",                                 "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("IO_AUDIT_MUTEX",                                   "<html><P>Occurs during synchronization of trace event buffers.</P></html>");
		add("IO_COMPLETION",                                    "<html><P>Occurs while waiting for I/O operations to complete. This wait type generally represents non-data page I/Os. Data page I/O completion waits appear as PAGEIOLATCH_* waits.</P></html>");
		add("IO_RETRY",                                         "<html><P>Occurs when an I/O operation such as a read or a write to disk fails because of insufficient resources, and is then retried.</P></html>");
		add("IOAFF_RANGE_QUEUE",                                "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("KSOURCE_WAKEUP",                                   "<html><P>Used by the service control task while waiting for requests from the Service Control Manager. Long waits are expected and do not indicate a problem.</P></html>");
		add("KTM_ENLISTMENT",                                   "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("KTM_RECOVERY_MANAGER",                             "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("KTM_RECOVERY_RESOLUTION",                          "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("LATCH_DT",                                         "<html><P>Occurs when waiting for a DT (destroy) latch. This does not include buffer latches or transaction mark latches. A listing of LATCH_* waits is available in <CODE>sys.dm_os_latch_stats</CODE>. Note that <CODE>sys.dm_os_latch_stats</CODE> groups LATCH_NL, LATCH_SH, LATCH_UP, LATCH_EX, and LATCH_DT waits together.</P></html>");
		add("LATCH_EX",                                         "<html><P>Occurs when waiting for an EX (exclusive) latch. This does not include buffer latches or transaction mark latches. A listing of LATCH_* waits is available in <CODE>sys.dm_os_latch_stats</CODE>. Note that <CODE>sys.dm_os_latch_stats</CODE> groups LATCH_NL, LATCH_SH, LATCH_UP, LATCH_EX, and LATCH_DT waits together.</P></html>");
		add("LATCH_KP",                                         "<html><P>Occurs when waiting for a KP (keep) latch. This does not include buffer latches or transaction mark latches. A listing of LATCH_* waits is available in <CODE>sys.dm_os_latch_stats</CODE>. Note that <CODE>sys.dm_os_latch_stats</CODE> groups LATCH_NL, LATCH_SH, LATCH_UP, LATCH_EX, and LATCH_DT waits together.</P></html>");
		add("LATCH_NL",                                         "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("LATCH_SH",                                         "<html><P>Occurs when waiting for an SH (share) latch. This does not include buffer latches or transaction mark latches. A listing of LATCH_* waits is available in <CODE>sys.dm_os_latch_stats</CODE>. Note that <CODE>sys.dm_os_latch_stats</CODE> groups LATCH_NL, LATCH_SH, LATCH_UP, LATCH_EX, and LATCH_DT waits together.</P></html>");
		add("LATCH_UP",                                         "<html><P>Occurs when waiting for an UP (update) latch. This does not include buffer latches or transaction mark latches. A listing of LATCH_* waits is available in <CODE>sys.dm_os_latch_stats</CODE>. Note that <CODE>sys.dm_os_latch_stats</CODE> groups LATCH_NL, LATCH_SH, LATCH_UP, LATCH_EX, and LATCH_DT waits together.</P></html>");
		add("LAZYWRITER_SLEEP",                                 "<html><P>Occurs when lazywriter tasks are suspended. This is a measure of the time spent by background tasks that are waiting. Do not consider this state when you are looking for user stalls.</P></html>");
		add("LCK_M_BU",                                         "<html><P>Occurs when a task is waiting to acquire a Bulk Update (BU) lock.</P></html>");
		add("LCK_M_BU_ABORT_BLOCKERS",                          "<html><P>Occurs when a task is waiting to acquire a Bulk Update (BU) lock with Abort Blockers. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_BU_LOW_PRIORITY",                            "<html><P>Occurs when a task is waiting to acquire a Bulk Update (BU) lock with Low Priority. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_IS",                                         "<html><P>Occurs when a task is waiting to acquire an Intent Shared (IS) lock.</P></html>");
		add("LCK_M_IS_ABORT_BLOCKERS",                          "<html><P>Occurs when a task is waiting to acquire an Intent Shared (IS) lock with Abort Blockers. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_IS_LOW_PRIORITY",                            "<html><P>Occurs when a task is waiting to acquire an Intent Shared (IS) lock with Low Priority. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_IU",                                         "<html><P>Occurs when a task is waiting to acquire an Intent Update (IU) lock.</P></html>");
		add("LCK_M_IU_ABORT_BLOCKERS",                          "<html><P>Occurs when a task is waiting to acquire an Intent Update (IU) lock with Abort Blockers. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_IU_LOW_PRIORITY",                            "<html><P>Occurs when a task is waiting to acquire an Intent Update (IU) lock with Low Priority. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_IX",                                         "<html><P>Occurs when a task is waiting to acquire an Intent Exclusive (IX) lock.</P></html>");
		add("LCK_M_IX_ABORT_BLOCKERS",                          "<html><P>Occurs when a task is waiting to acquire an Intent Exclusive (IX) lock with Abort Blockers. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_IX_LOW_PRIORITY",                            "<html><P>Occurs when a task is waiting to acquire an Intent Exclusive (IX) lock with Low Priority. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_RIn_NL",                                     "<html><P>Occurs when a task is waiting to acquire a NULL lock on the current key value, and an Insert Range lock between the current and previous key. A NULL lock on the key is an instant release lock.</P></html>");
		add("LCK_M_RIn_NL_ABORT_BLOCKERS",                      "<html><P>Occurs when a task is waiting to acquire a NULL lock with Abort Blockers on the current key value, and an Insert Range lock with Abort Blockers between the current and previous key. A NULL lock on the key is an instant release lock. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_RIn_NL_LOW_PRIORITY",                        "<html><P>Occurs when a task is waiting to acquire a NULL lock with Low Priority on the current key value, and an Insert Range lock with Low Priority between the current and previous key. A NULL lock on the key is an instant release lock. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_RIn_S",                                      "<html><P>Occurs when a task is waiting to acquire a shared lock on the current key value, and an Insert Range lock between the current and previous key.</P></html>");
		add("LCK_M_RIn_S_ABORT_BLOCKERS",                       "<html><P>Occurs when a task is waiting to acquire a shared lock with Abort Blockers on the current key value, and an Insert Range lock with Abort Blockers between the current and previous key. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_RIn_S_LOW_PRIORITY",                         "<html><P>Occurs when a task is waiting to acquire a shared lock with Low Priority on the current key value, and an Insert Range lock with Low Priority between the current and previous key. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_RIn_U",                                      "<html><P>Task is waiting to acquire an Update lock on the current key value, and an Insert Range lock between the current and previous key.</P></html>");
		add("LCK_M_RIn_U_ABORT_BLOCKERS",                       "<html><P>Task is waiting to acquire an Update lock with Abort Blockers on the current key value, and an Insert Range lock with Abort Blockers between the current and previous key. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_RIn_U_LOW_PRIORITY",                         "<html><P>Task is waiting to acquire an Update lock with Low Priority on the current key value, and an Insert Range lock with Low Priority between the current and previous key. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_RIn_X",                                      "<html><P>Occurs when a task is waiting to acquire an Exclusive lock on the current key value, and an Insert Range lock between the current and previous key.</P></html>");
		add("LCK_M_RIn_X_ABORT_BLOCKERS",                       "<html><P>Occurs when a task is waiting to acquire an Exclusive lock with Abort Blockers on the current key value, and an Insert Range lock with Abort Blockers between the current and previous key. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_RIn_X_LOW_PRIORITY",                         "<html><P>Occurs when a task is waiting to acquire an Exclusive lock with Low Priority on the current key value, and an Insert Range lock with Low Priority between the current and previous key. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_RS_S",                                       "<html><P>Occurs when a task is waiting to acquire a Shared lock on the current key value, and a Shared Range lock between the current and previous key.</P></html>");
		add("LCK_M_RS_S_ABORT_BLOCKERS",                        "<html><P>Occurs when a task is waiting to acquire a Shared lock with Abort Blockers on the current key value, and a Shared Range lock with Abort Blockers between the current and previous key. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_RS_S_LOW_PRIORITY",                          "<html><P>Occurs when a task is waiting to acquire a Shared lock with Low Priority on the current key value, and a Shared Range lock with Low Priority between the current and previous key. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_RS_U",                                       "<html><P>Occurs when a task is waiting to acquire an Update lock on the current key value, and an Update Range lock between the current and previous key.</P></html>");
		add("LCK_M_RS_U_ABORT_BLOCKERS",                        "<html><P>Occurs when a task is waiting to acquire an Update lock with Abort Blockers on the current key value, and an Update Range lock with Abort Blockers between the current and previous key. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_RS_U_LOW_PRIORITY",                          "<html><P>Occurs when a task is waiting to acquire an Update lock with Low Priority on the current key value, and an Update Range lock with Low Priority between the current and previous key. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_RX_S",                                       "<html><P>Occurs when a task is waiting to acquire a Shared lock on the current key value, and an Exclusive Range lock between the current and previous key.</P></html>");
		add("LCK_M_RX_S_ABORT_BLOCKERS",                        "<html><P>Occurs when a task is waiting to acquire a Shared lock with Abort Blockers on the current key value, and an Exclusive Range with Abort Blockers lock between the current and previous key. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_RX_S_LOW_PRIORITY",                          "<html><P>Occurs when a task is waiting to acquire a Shared lock with Low Priority on the current key value, and an Exclusive Range with Low Priority lock between the current and previous key. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_RX_U",                                       "<html><P>Occurs when a task is waiting to acquire an Update lock on the current key value, and an Exclusive range lock between the current and previous key.</P></html>");
		add("LCK_M_RX_U_ABORT_BLOCKERS",                        "<html><P>Occurs when a task is waiting to acquire an Update lock with Abort Blockers on the current key value, and an Exclusive range lock with Abort Blockers between the current and previous key. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_RX_U_LOW_PRIORITY",                          "<html><P>Occurs when a task is waiting to acquire an Update lock with Low Priority on the current key value, and an Exclusive range lock with Low Priority between the current and previous key. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_RX_X",                                       "<html><P>Occurs when a task is waiting to acquire an Exclusive lock on the current key value, and an Exclusive Range lock between the current and previous key.</P></html>");
		add("LCK_M_RX_X_ABORT_BLOCKERS",                        "<html><P>Occurs when a task is waiting to acquire an Exclusive lock with Abort Blockers on the current key value, and an Exclusive Range lock with Abort Blockers between the current and previous key. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_RX_X_LOW_PRIORITY",                          "<html><P>Occurs when a task is waiting to acquire an Exclusive lock with Low Priority on the current key value, and an Exclusive Range lock with Low Priority between the current and previous key. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_S",                                          "<html><P>Occurs when a task is waiting to acquire a Shared lock.</P></html>");
		add("LCK_M_S_ABORT_BLOCKERS",                           "<html><P>Occurs when a task is waiting to acquire a Shared lock with Abort Blockers. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_S_LOW_PRIORITY",                             "<html><P>Occurs when a task is waiting to acquire a Shared lock with Low Priority. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_SCH_M",                                      "<html><P>Occurs when a task is waiting to acquire a Schema Modify lock.</P></html>");
		add("LCK_M_SCH_M_ABORT_BLOCKERS",                       "<html><P>Occurs when a task is waiting to acquire a Schema Modify lock with Abort Blockers. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_SCH_M_LOW_PRIORITY",                         "<html><P>Occurs when a task is waiting to acquire a Schema Modify lock with Low Priority. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_SCH_S",                                      "<html><P>Occurs when a task is waiting to acquire a Schema Share lock.</P></html>");
		add("LCK_M_SCH_S_ABORT_BLOCKERS",                       "<html><P>Occurs when a task is waiting to acquire a Schema Share lock with Abort Blockers. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_SCH_S_LOW_PRIORITY",                         "<html><P>Occurs when a task is waiting to acquire a Schema Share lock with Low Priority. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_SIU",                                        "<html><P>Occurs when a task is waiting to acquire a Shared With Intent Update lock.</P></html>");
		add("LCK_M_SIU_ABORT_BLOCKERS",                         "<html><P>Occurs when a task is waiting to acquire a Shared With Intent Update lock with Abort Blockers. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_SIU_LOW_PRIORITY",                           "<html><P>Occurs when a task is waiting to acquire a Shared With Intent Update lock with Low Priority. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_SIX",                                        "<html><P>Occurs when a task is waiting to acquire a Shared With Intent Exclusive lock.</P></html>");
		add("LCK_M_SIX_ABORT_BLOCKERS",                         "<html><P>Occurs when a task is waiting to acquire a Shared With Intent Exclusive lock with Abort Blockers. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_SIX_LOW_PRIORITY",                           "<html><P>Occurs when a task is waiting to acquire a Shared With Intent Exclusive lock with Low Priority. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_U",                                          "<html><P>Occurs when a task is waiting to acquire an Update lock.</P></html>");
		add("LCK_M_U_ABORT_BLOCKERS",                           "<html><P>Occurs when a task is waiting to acquire an Update lock with Abort Blockers. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_U_LOW_PRIORITY",                             "<html><P>Occurs when a task is waiting to acquire an Update lock with Low Priority. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_UIX",                                        "<html><P>Occurs when a task is waiting to acquire an Update With Intent Exclusive lock.</P></html>");
		add("LCK_M_UIX_ABORT_BLOCKERS",                         "<html><P>Occurs when a task is waiting to acquire an Update With Intent Exclusive lock with Abort Blockers. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_UIX_LOW_PRIORITY",                           "<html><P>Occurs when a task is waiting to acquire an Update With Intent Exclusive lock with Low Priority. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_X",                                          "<html><P>Occurs when a task is waiting to acquire an Exclusive lock.</P></html>");
		add("LCK_M_X_ABORT_BLOCKERS",                           "<html><P>Occurs when a task is waiting to acquire an Exclusive lock with Abort Blockers. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LCK_M_X_LOW_PRIORITY",                             "<html><P>Occurs when a task is waiting to acquire an Exclusive lock with Low Priority. (Related to the low priority wait option of ALTER TABLE and ALTER INDEX.)</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("LOGBUFFER",                                        "<html><P>Occurs when a task is waiting for space in the log buffer to store a log record. Consistently high values may indicate that the log devices cannot keep up with the amount of log being generated by the server.</P></html>");
		add("LOGGENERATION",                                    "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("LOGMGR",                                           "<html><P>Occurs when a task is waiting for any outstanding log I/Os to finish before shutting down the log while closing the database.</P></html>");
		add("LOGMGR_FLUSH",                                     "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("LOGMGR_QUEUE",                                     "<html><P>Occurs while the log writer task waits for work requests.</P></html>");
		add("LOGMGR_RESERVE_APPEND",                            "<html><P>Occurs when a task is waiting to see whether log truncation frees up log space to enable the task to write a new log record. Consider increasing the size of the log file(s) for the affected database to reduce this wait.</P></html>");
		add("LOWFAIL_MEMMGR_QUEUE",                             "<html><P>Occurs while waiting for memory to be available for use.</P></html>");
		add("MEMORY_ALLOCATION_EXT",                            "<html><P>Occurs while allocating memory from either the internal SQL Server memory pool or the operation system.</P></html>");
		add("MISCELLANEOUS",                                    "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("MSQL_DQ",                                          "<html><P>Occurs when a task is waiting for a distributed query operation to finish. This is used to detect potential Multiple Active Result Set (MARS) application deadlocks. The wait ends when the distributed query call finishes.</P></html>");
		add("MSQL_XACT_MGR_MUTEX",                              "<html><P>Occurs when a task is waiting to obtain ownership of the session transaction manager to perform a session level transaction operation.</P></html>");
		add("MSQL_XACT_MUTEX",                                  "<html><P>Occurs during synchronization of transaction usage. A request must acquire the mutex before it can use the transaction.</P></html>");
		add("MSQL_XP",                                          "<html><P>Occurs when a task is waiting for an extended stored procedure to end. SQL Server uses this wait state to detect potential MARS application deadlocks. The wait stops when the extended stored procedure call ends.</P></html>");
		add("MSSEARCH",                                         "<html><P>Occurs during Full-Text Search calls. This wait ends when the full-text operation completes. It does not indicate contention, but rather the duration of full-text operations.</P></html>");
		add("NET_WAITFOR_PACKET",                               "<html><P>Occurs when a connection is waiting for a network packet during a network read.</P></html>");
		add("OLEDB",                                            "<html><P>Occurs when SQL Server calls the SQL Server Native Client OLE DB Provider. This wait type is not used for synchronization. Instead, it indicates the duration of calls to the OLE DB provider.</P></html>");
		add("ONDEMAND_TASK_QUEUE",                              "<html><P>Occurs while a background task waits for high priority system task requests. Long wait times indicate that there have been no high priority requests to process, and should not cause concern.</P></html>");
		add("PAGEIOLATCH_DT",                                   "<html><P>Occurs when a task is waiting on a latch for a buffer that is in an I/O request. The latch request is in Destroy mode. Long waits may indicate problems with the disk subsystem.</P></html>");
		add("PAGEIOLATCH_EX",                                   "<html><P>Occurs when a task is waiting on a latch for a buffer that is in an I/O request. The latch request is in Exclusive mode. Long waits may indicate problems with the disk subsystem.</P></html>");
		add("PAGEIOLATCH_KP",                                   "<html><P>Occurs when a task is waiting on a latch for a buffer that is in an I/O request. The latch request is in Keep mode. Long waits may indicate problems with the disk subsystem.</P></html>");
		add("PAGEIOLATCH_NL",                                   "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("PAGEIOLATCH_SH",                                   "<html><P>Occurs when a task is waiting on a latch for a buffer that is in an I/O request. The latch request is in Shared mode. Long waits may indicate problems with the disk subsystem.</P></html>");
		add("PAGEIOLATCH_UP",                                   "<html><P>Occurs when a task is waiting on a latch for a buffer that is in an I/O request. The latch request is in Update mode. Long waits may indicate problems with the disk subsystem.</P></html>");
		add("PAGELATCH_DT",                                     "<html><P>Occurs when a task is waiting on a latch for a buffer that is not in an I/O request. The latch request is in Destroy mode.</P></html>");
		add("PAGELATCH_EX",                                     "<html><P>Occurs when a task is waiting on a latch for a buffer that is not in an I/O request. The latch request is in Exclusive mode.</P></html>");
		add("PAGELATCH_KP",                                     "<html><P>Occurs when a task is waiting on a latch for a buffer that is not in an I/O request. The latch request is in Keep mode.</P></html>");
		add("PAGELATCH_NL",                                     "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("PAGELATCH_SH",                                     "<html><P>Occurs when a task is waiting on a latch for a buffer that is not in an I/O request. The latch request is in Shared mode.</P></html>");
		add("PAGELATCH_UP",                                     "<html><P>Occurs when a task is waiting on a latch for a buffer that is not in an I/O request. The latch request is in Update mode.</P></html>");
		add("PARALLEL_BACKUP_QUEUE",                            "<html><P>Occurs when serializing output produced by RESTORE HEADERONLY, RESTORE FILELISTONLY, or RESTORE LABELONLY.</P></html>");
		add("PREEMPTIVE_ABR",                                   "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("PREEMPTIVE_AUDIT_ACCESS_EVENTLOG",                 "<html><P>Occurs when the SQL Server Operating System (SQLOS) scheduler switches to preemptive mode to write an audit event to the Windows event log.</P></html>");
		add("PREEMPTIVE_AUDIT_ACCESS_SECLOG",                   "<html><P>Occurs when the SQLOS scheduler switches to preemptive mode to write an audit event to the Windows Security log.</P></html>");
		add("PREEMPTIVE_CLOSEBACKUPMEDIA",                      "<html><P>Occurs when the SQLOS scheduler switches to preemptive mode to close backup media.</P></html>");
		add("PREEMPTIVE_CLOSEBACKUPTAPE",                       "<html><P>Occurs when the SQLOS scheduler switches to preemptive mode to close a tape backup device.</P></html>");
		add("PREEMPTIVE_CLOSEBACKUPVDIDEVICE",                  "<html><P>Occurs when the SQLOS scheduler switches to preemptive mode to close a virtual backup device.</P></html>");
		add("PREEMPTIVE_CLUSAPI_CLUSTERRESOURCECONTROL",        "<html><P>Occurs when the SQLOS scheduler switches to preemptive mode to perform Windows failover cluster operations.</P></html>");
		add("PREEMPTIVE_COM_COCREATEINSTANCE",                  "<html><P>Occurs when the SQLOS scheduler switches to preemptive mode to create a COM object.</P></html>");
		add("PREEMPTIVE_HADR_LEASE_MECHANISM",                  "<html><P>AlwaysOn Availability Groups lease manager scheduling for CSS diagnostics.��</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("PREEMPTIVE_SOSTESTING",                            "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("PREEMPTIVE_STRESSDRIVER",                          "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("PREEMPTIVE_TESTING",                               "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("PREEMPTIVE_XETESTING",                             "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("PRINT_ROLLBACK_PROGRESS",                          "<html><P>Used to wait while user processes are ended in a database that has been transitioned by using the ALTER DATABASE termination clause. For more information, see <A href=\"https://msdn.microsoft.com/en-us/library/ms174269.aspx\">ALTER DATABASE (Transact-SQL)</A>.</P></html>");
		add("PWAIT_HADR_CHANGE_NOTIFIER_TERMINATION_SYNC",      "<html><P>Occurs when a background task is waiting for the termination of the background task that receives (via polling) Windows Server Failover Clustering notifications.</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("PWAIT_HADR_CLUSTER_INTEGRATION",                   "<html><P>An append, replace, and/or remove operation is waiting to grab a write lock on an AlwaysOn internal list (such as a list of networks, network addresses, or availability group listeners).  Internal use only</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("PWAIT_HADR_OFFLINE_COMPLETED",                     "<html><P>An AlwaysOn drop availability group operation is waiting for the target availability group to go offline before destroying Windows Server Failover Clustering objects.��</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("PWAIT_HADR_ONLINE_COMPLETED",                      "<html><P>An AlwaysOn create or failover availability group operation is waiting for the target availability group to come online.��</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("PWAIT_HADR_POST_ONLINE_COMPLETED",                 "<html><P>An AlwaysOn drop availability group operation is waiting for the termination of any background task that was scheduled as part of a previous command. For example, there may be a background task that is transitioning availability databases to the primary role. The DROP AVAILABILITY GROUP DDL must wait for this background task to terminate in order to avoid race conditions.��</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("PWAIT_HADR_WORKITEM_COMPLETED",                    "<html><P>Internal wait by a thread waiting for an async work task to complete. This is an expected wait and is for CSS use. </P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2012 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("PWAIT_MD_LOGIN_STATS",                             "<html><P>Occurs during internal synchronization in metadata on login stats.</P></html>");
		add("PWAIT_MD_RELATION_CACHE",                          "<html><P>Occurs during internal synchronization in metadata on table or index.</P></html>");
		add("PWAIT_MD_SERVER_CACHE",                            "<html><P>Occurs during internal synchronization in metadata on linked servers.</P></html>");
		add("PWAIT_MD_UPGRADE_CONFIG",                          "<html><P>Occurs during internal synchronization in upgrading server wide configurations.</P></html>");
		add("PWAIT_METADATA_LAZYCACHE_RWLOCk",                  "<html><P>Occurs during internal synchronization in metadata cache along with iterating index or stats in a table.</P></html>");
		add("QPJOB_KILL",                                       "<html><P>Indicates that an asynchronous automatic statistics update was canceled by a call to KILL as the update was starting to run. The terminating thread is suspended, waiting for it to start listening for KILL commands. A good value is less than one second.</P></html>");
		add("QPJOB_WAITFOR_ABORT",                              "<html><P>Indicates that an asynchronous automatic statistics update was canceled by a call to KILL when it was running. The update has now completed but is suspended until the terminating thread message coordination is complete. This is an ordinary but rare state, and should be very short. A good value is less than one second.</P></html>");
		add("QRY_MEM_GRANT_INFO_MUTEX",                         "<html><P>Occurs when Query Execution memory management tries to control access to static grant information list. This state lists information about the current granted and waiting memory requests. This state is a simple access control state. There should never be a long wait on this state. If this mutex is not released, all new memory-using queries will stop responding.</P></html>");
		add("QUERY_ERRHDL_SERVICE_DONE",                        "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("QUERY_EXECUTION_INDEX_SORT_EVENT_OPEN",            "<html><P>Occurs in certain cases when offline create index build is run in parallel, and the different worker threads that are sorting synchronize access to the sort files.</P></html>");
		add("QUERY_NOTIFICATION_MGR_MUTEX",                     "<html><P>Occurs during synchronization of the garbage collection queue in the Query Notification Manager.</P></html>");
		add("QUERY_NOTIFICATION_SUBSCRIPTION_MUTEX",            "<html><P>Occurs during state synchronization for transactions in Query Notifications. </P></html>");
		add("QUERY_NOTIFICATION_TABLE_MGR_MUTEX",               "<html><P>Occurs during internal synchronization within the Query Notification Manager.</P></html>");
		add("QUERY_NOTIFICATION_UNITTEST_MUTEX",                "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("QUERY_OPTIMIZER_PRINT_MUTEX",                      "<html><P>Occurs during synchronization of query optimizer diagnostic output production. This wait type only occurs if diagnostic settings have been enabled under direction of Microsoft Product Support.</P></html>");
		add("QUERY_TRACEOUT",                                   "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("QUERY_WAIT_ERRHDL_SERVICE",                        "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("RECOVER_CHANGEDB",                                 "<html><P>Occurs during synchronization of database status in warm standby database.</P></html>");
		add("REPL_CACHE_ACCESS",                                "<html><P>Occurs during synchronization on a replication article cache. During these waits, the replication log reader stalls, and data definition language (DDL) statements on a published table are blocked.</P></html>");
		add("REPL_SCHEMA_ACCESS",                               "<html><P>Occurs during synchronization of replication schema version information. This state exists when DDL statements are executed on the replicated object, and when the log reader builds or consumes versioned schema based on DDL occurrence.</P><P>This wait type is also used by log reader agent to synchronize memory access.�If a publisher has large number of very active published databases and log reader agents, contention can be seen on this wait type.</P></html>");
		add("REPLICA_WRITES",                                   "<html><P>Occurs while a task waits for completion of page writes to database snapshots or DBCC replicas.</P></html>");
		add("REQUEST_DISPENSER_PAUSE",                          "<html><P>Occurs when a task is waiting for all outstanding I/O to complete, so that I/O to a file can be frozen for snapshot backup.</P></html>");
		add("REQUEST_FOR_DEADLOCK_SEARCH",                      "<html><P>Occurs while the deadlock monitor waits to start the next deadlock search. This wait is expected between deadlock detections, and lengthy total waiting time on this resource does not indicate a problem.</P></html>");
		add("RESMGR_THROTTLED",                                 "<html><P>Occurs when a new request comes in and is throttled based on the GROUP_MAX_REQUESTS setting.</P></html>");
		add("RESOURCE_QUEUE",                                   "<html><P>Occurs during synchronization of various internal resource queues.</P></html>");
		add("RESOURCE_SEMAPHORE",                               "<html><P>Occurs when a query memory request cannot be granted immediately due to other concurrent queries. High waits and wait times may indicate excessive number of concurrent queries, or excessive memory request amounts.</P></html>");
		add("RESOURCE_SEMAPHORE_MUTEX",                         "<html><P>Occurs while a query waits for its request for a thread reservation to be fulfilled. It also occurs when synchronizing query compile and memory grant requests.</P></html>");
		add("RESOURCE_SEMAPHORE_QUERY_COMPILE",                 "<html><P>Occurs when the number of concurrent query compilations reaches a throttling limit. High waits and wait times may indicate excessive compilations, recompiles, or uncachable plans.</P></html>");
		add("RESOURCE_SEMAPHORE_SMALL_QUERY",                   "<html><P>Occurs when memory request by a small query cannot be granted immediately due to other concurrent queries. Wait time should not exceed more than a few seconds, because the server transfers the request to the main query memory pool if it fails to grant the requested memory within a few seconds. High waits may indicate an excessive number of concurrent small queries while the main memory pool is blocked by waiting queries.</P></html>");
		add("SEC_DROP_TEMP_KEY",                                "<html><P>Occurs after a failed attempt to drop a temporary security key before a retry attempt.</P></html>");
		add("SECURITY_MUTEX",                                   "<html><P>Occurs when there is a wait for mutexes that control access to the global list of Extensible Key Management (EKM) cryptographic providers and the session-scoped list of EKM sessions.</P></html>");
		add("SEQUENTIAL_GUID",                                  "<html><P>Occurs while a new sequential GUID is being obtained.</P></html>");
		add("SERVER_IDLE_CHECK",                                "<html><P>Occurs during synchronization of SQL Server instance idle status when a resource monitor is attempting to declare a SQL Server instance as idle or trying to wake up.</P></html>");
		add("SHUTDOWN",                                         "<html><P>Occurs while a shutdown statement waits for active connections to exit.</P></html>");
		add("SLEEP_BPOOL_FLUSH",                                "<html><P>Occurs when a checkpoint is throttling the issuance of new I/Os in order to avoid flooding the disk subsystem.</P></html>");
		add("SLEEP_DBSTARTUP",                                  "<html><P>Occurs during database startup while waiting for all databases to recover.</P></html>");
		add("SLEEP_DCOMSTARTUP",                                "<html><P>Occurs once at most during SQL Server instance startup while waiting for DCOM initialization to complete.</P></html>");
		add("SLEEP_MSDBSTARTUP",                                "<html><P>Occurs when SQL Trace waits for the <CODE>msdb</CODE> database to complete startup.</P></html>");
		add("SLEEP_SYSTEMTASK",                                 "<html><P>Occurs during the start of a background task while waiting for <CODE>tempdb</CODE> to complete startup.</P></html>");
		add("SLEEP_TASK",                                       "<html><P>Occurs when a task sleeps while waiting for a generic event to occur.</P></html>");
		add("SLEEP_TEMPDBSTARTUP",                              "<html><P>Occurs while a task waits for <CODE>tempdb</CODE> to complete startup.</P></html>");
		add("SNI_CRITICAL_SECTION",                             "<html><P>Occurs during internal synchronization within SQL Server networking components.</P></html>");
		add("SNI_HTTP_WAITFOR_0_DISCON",                        "<html><P>Occurs during SQL Server shutdown, while waiting for outstanding HTTP connections to exit.</P></html>");
		add("SNI_LISTENER_ACCESS",                              "<html><P>Occurs while waiting for non-uniform memory access (NUMA) nodes to update state change. Access to state change is serialized.</P></html>");
		add("SNI_TASK_COMPLETION",                              "<html><P>Occurs when there is a wait for all tasks to finish during a NUMA node state change.</P></html>");
		add("SOAP_READ",                                        "<html><P>Occurs while waiting for an HTTP network read to complete.</P></html>");
		add("SOAP_WRITE",                                       "<html><P>Occurs while waiting for an HTTP network write to complete.</P></html>");
		add("SOS_CALLBACK_REMOVAL",                             "<html><P>Occurs while performing synchronization on a callback list in order to remove a callback. It is not expected for this counter to change after server initialization is completed.</P></html>");
		add("SOS_DISPATCHER_MUTEX",                             "<html><P>Occurs during internal synchronization of the dispatcher pool. This includes when the pool is being adjusted.</P></html>");
		add("SOS_LOCALALLOCATORLIST",                           "<html><P>Occurs during internal synchronization in the SQL Server memory manager.</P></html>");
		add("SOS_MEMORY_USAGE_ADJUSTMENT",                      "<html><P>Occurs when memory usage is being adjusted among pools.</P></html>");
		add("SOS_OBJECT_STORE_DESTROY_MUTEX",                   "<html><P>Occurs during internal synchronization in memory pools when destroying objects from the pool.</P></html>");
		add("SOS_PHYS_PAGE_CACHE",                              "<html><P>Accounts for the time a thread waits to acquire the mutex it must acquire before it allocates physical pages or before it returns those pages to the operating system. Waits on this type only appear if the instance of SQL Server uses AWE memory. </P></html>");
		add("SOS_PROCESS_AFFINITY_MUTEX",                       "<html><P>Occurs during synchronizing of access to process affinity settings.</P></html>");
		add("SOS_RESERVEDMEMBLOCKLIST",                         "<html><P>Occurs during internal synchronization in the SQL Server memory manager.</P></html>");
		add("SOS_SCHEDULER_YIELD",                              "<html><P>Occurs when a task voluntarily yields the scheduler for other tasks to execute. During this wait the task is waiting for its quantum to be renewed.</P></html>");
		add("SOS_SMALL_PAGE_ALLOC",                             "<html><P>Occurs during the allocation and freeing of memory that is managed by some memory objects.</P></html>");
		add("SOS_STACKSTORE_INIT_MUTEX",                        "<html><P>Occurs during synchronization of internal store initialization.</P></html>");
		add("SOS_SYNC_TASK_ENQUEUE_EVENT",                      "<html><P>Occurs when a task is started in a synchronous manner. Most tasks in SQL Server are started in an asynchronous manner, in which control returns to the starter immediately after the task request has been placed on the work queue.</P></html>");
		add("SOS_VIRTUALMEMORY_LOW",                            "<html><P>Occurs when a memory allocation waits for a resource manager to free up virtual memory.</P></html>");
		add("SOSHOST_EVENT",                                    "<html><P>Occurs when a hosted component, such as CLR, waits on a SQL Server event synchronization object.</P></html>");
		add("SOSHOST_INTERNAL",                                 "<html><P>Occurs during synchronization of memory manager callbacks used by hosted components, such as CLR.</P></html>");
		add("SOSHOST_MUTEX",                                    "<html><P>Occurs when a hosted component, such as CLR, waits on a SQL Server mutex synchronization object.</P></html>");
		add("SOSHOST_RWLOCK",                                   "<html><P>Occurs when a hosted component, such as CLR, waits on a SQL Server reader-writer synchronization object.</P></html>");
		add("SOSHOST_SEMAPHORE",                                "<html><P>Occurs when a hosted component, such as CLR, waits on a SQL Server semaphore synchronization object.</P></html>");
		add("SOSHOST_SLEEP",                                    "<html><P>Occurs when a hosted task sleeps while waiting for a generic event to occur. Hosted tasks are used by hosted components such as CLR.</P></html>");
		add("SOSHOST_TRACELOCK",                                "<html><P>Occurs during synchronization of access to trace streams.</P></html>");
		add("SOSHOST_WAITFORDONE",                              "<html><P>Occurs when a hosted component, such as CLR, waits for a task to complete.</P></html>");
		add("SQLCLR_APPDOMAIN",                                 "<html><P>Occurs while CLR waits for an application domain to complete startup.</P></html>");
		add("SQLCLR_ASSEMBLY",                                  "<html><P>Occurs while waiting for access to the loaded assembly list in the appdomain.</P></html>");
		add("SQLCLR_DEADLOCK_DETECTION",                        "<html><P>Occurs while CLR waits for deadlock detection to complete.</P></html>");
		add("SQLCLR_QUANTUM_PUNISHMENT",                        "<html><P>Occurs when a CLR task is throttled because it has exceeded its execution quantum. This throttling is done in order to reduce the effect of this resource-intensive task on other tasks.</P></html>");
		add("SQLSORT_NORMMUTEX",                                "<html><P>Occurs during internal synchronization, while initializing internal sorting structures.</P></html>");
		add("SQLSORT_SORTMUTEX",                                "<html><P>Occurs during internal synchronization, while initializing internal sorting structures.</P></html>");
		add("SQLTRACE_BUFFER_FLUSH",                            "<html><P>Occurs when a task is waiting for a background task to flush trace buffers to disk every four seconds. </P></html>");
		add("SQLTRACE_FILE_BUFFER",                             "<html><P>Occurs during synchronization on trace buffers during a file trace.</P></html>");
		add("SQLTRACE_SHUTDOWN",                                "<html><P>Occurs while trace shutdown waits for outstanding trace events to complete.</P></html>");
		add("SQLTRACE_WAIT_ENTRIES",                            "<html><P>Occurs while a SQL Trace event queue waits for packets to arrive on the queue.</P></html>");
		add("SRVPROC_SHUTDOWN",                                 "<html><P>Occurs while the shutdown process waits for internal resources to be released to shutdown cleanly.</P></html>");
		add("TEMPOBJ",                                          "<html><P>Occurs when temporary object drops are synchronized. This wait is rare, and only occurs if a task has requested exclusive access for <CODE>temp</CODE> table drops.</P></html>");
		add("THREADPOOL",                                       "<html><P>Occurs when a task is waiting for a worker to run on. This can indicate that the maximum worker setting is too low, or that batch executions are taking unusually long, thus reducing the number of workers available to satisfy other batches.</P></html>");
		add("TIMEPRIV_TIMEPERIOD",                              "<html><P>Occurs during internal synchronization of the Extended Events timer.</P></html>");
		add("TRACEWRITE",                                       "<html><P>Occurs when the SQL Trace rowset trace provider waits for either a free buffer or a buffer with events to process.</P></html>");
		add("TRAN_MARKLATCH_DT",                                "<html><P>Occurs when waiting for a destroy mode latch on a transaction mark latch. Transaction mark latches are used for synchronization of commits with marked transactions.</P></html>");
		add("TRAN_MARKLATCH_EX",                                "<html><P>Occurs when waiting for an exclusive mode latch on a marked transaction. Transaction mark latches are used for synchronization of commits with marked transactions.</P></html>");
		add("TRAN_MARKLATCH_KP",                                "<html><P>Occurs when waiting for a keep mode latch on a marked transaction. Transaction mark latches are used for synchronization of commits with marked transactions.</P></html>");
		add("TRAN_MARKLATCH_NL",                                "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("TRAN_MARKLATCH_SH",                                "<html><P>Occurs when waiting for a shared mode latch on a marked transaction. Transaction mark latches are used for synchronization of commits with marked transactions.</P></html>");
		add("TRAN_MARKLATCH_UP",                                "<html><P>Occurs when waiting for an update mode latch on a marked transaction. Transaction mark latches are used for synchronization of commits with marked transactions.</P></html>");
		add("TRANSACTION_MUTEX",                                "<html><P>Occurs during synchronization of access to a transaction by multiple batches.</P></html>");
		add("UTIL_PAGE_ALLOC",                                  "<html><P>Occurs when transaction log scans wait for memory to be available during memory pressure.</P></html>");
		add("VIA_ACCEPT",                                       "<html><P>Occurs when a Virtual Interface Adapter (VIA) provider connection is completed during startup.</P></html>");
		add("VIEW_DEFINITION_MUTEX",                            "<html><P>Occurs during synchronization on access to cached view definitions.</P></html>");
		add("WAIT_FOR_RESULTS",                                 "<html><P>Occurs when waiting for a query notification to be triggered.</P></html>");
		add("WAIT_XTP_CKPT_CLOSE",                              "<html><P>Occurs when waiting for a checkpoint to complete.</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("WAIT_XTP_CKPT_ENABLED",                            "<html><P>Occurs when checkpointing is disabled, and waiting for checkpointing to be enabled.</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("WAIT_XTP_CKPT_STATE_LOCK",                         "<html><P>Occurs when synchronizing checking of checkpoint state.</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("WAIT_XTP_GUEST",                                   "<html><P>Occurs when the database memory allocator needs to stop receiving low-memory notifications.</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("WAIT_XTP_HOST_WAIT",                               "<html><P>Occurs when waits are triggered by the database engine and implemented by the host.</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("WAIT_XTP_OFFLINE_CKPT_LOG_IO",                     "<html><P>Occurs when offline checkpoint is waiting for a log read IO to complete.</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("WAIT_XTP_OFFLINE_CKPT_NEW_LOG",                    "<html><P>Occurs when offline checkpoint is waiting for new log records to scan.</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("WAIT_XTP_PROCEDURE_ENTRY",                         "<html><P>Occurs when a drop procedure is waiting for all current executions of that procedure to complete.</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("WAIT_XTP_TASK_SHUTDOWN",                           "<html><P>Occurs when waiting for an In-Memory OLTP thread to complete.</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("WAIT_XTP_TRAN_COMMIT",                             "<html><P>Occurs when execution of a natively compiled stored procedure is waiting for an XTP transaction to commit (waiting for transactions dependent on for instance).</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("WAIT_XTP_TRAN_DEPENDENCY",                         "<html><P>Occurs when waiting for transaction dependencies.</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("WAITFOR",                                          "<html><P>Occurs as a result of a WAITFOR Transact-SQL statement. The duration of the wait is determined by the parameters to the statement. This is a user-initiated wait.</P></html>");
		add("WAITFOR_TASKSHUTDOWN",                             "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("WAITSTAT_MUTEX",                                   "<html><P>Occurs during synchronization of access to the collection of statistics used to populate <CODE>sys.dm_os_wait_stats</CODE>.</P></html>");
		add("WCC",                                              "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("WORKTBL_DROP",                                     "<html><P>Occurs while pausing before retrying, after a failed worktable drop.</P></html>");
		add("WRITE_COMPLETION",                                 "<html><P>Occurs when a write operation is in progress.</P></html>");
		add("WRITELOG",                                         "<html><P>Occurs while waiting for a log flush to complete. Common operations that cause log flushes are checkpoints and transaction commits.</P></html>");
		add("XACT_OWN_TRANSACTION",                             "<html><P>Occurs while waiting to acquire ownership of a transaction.</P></html>");
		add("XACT_RECLAIM_SESSION",                             "<html><P>Occurs while waiting for the current owner of a session to release ownership of the session.</P></html>");
		add("XACTLOCKINFO",                                     "<html><P>Occurs during synchronization of access to the list of locks for a transaction. In addition to the transaction itself, the list of locks is accessed by operations such as deadlock detection and lock migration during page splits.</P></html>");
		add("XACTWORKSPACE_MUTEX",                              "<html><P>Occurs during synchronization of defections from a transaction, as well as the number of database locks between enlist members of a transaction.</P></html>");
		add("XE_BUFFERMGR_ALLPROCESSED_EVENT",                  "<html><P>Occurs when Extended Events session buffers are flushed to targets. This wait occurs on a background thread.</P></html>");
		add("XE_BUFFERMGR_FREEBUF_EVENT",                       "<html><P>Occurs when either of the following conditions is true:</P><UL><LI><P>An Extended Events session is configured for no event loss, and all buffers in the session are currently full. This can indicate that the buffers for an Extended Events session are too small, or should be partitioned.</P></LI><LI><P>Audits experience a delay. This can indicate a disk bottleneck on the drive where the audits are written.</P></LI></UL></html>");
		add("XE_DISPATCHER_CONFIG_SESSION_LIST",                "<html><P>Occurs when an Extended Events session that is using asynchronous targets is started or stopped. This wait indicates either of the following:</P><UL><LI><P>An Extended Events session is registering with a background thread pool.</P></LI><LI><P>The background thread pool is calculating the required number of threads based on current load.</P></LI></UL></html>");
		add("XE_DISPATCHER_JOIN",                               "<html><P>Occurs when a background thread that is used for Extended Events sessions is terminating.  </P></html>");
		add("XE_DISPATCHER_WAIT",                               "<html><P>Occurs when a background thread that is used for Extended Events sessions is waiting for event buffers to process.  </P></html>");
		add("XE_MODULEMGR_SYNC",                                "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("XE_OLS_LOCK",                                      "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("XE_PACKAGE_LOCK_BACKOFF",                          "<html><P>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</P></html>");
		add("XTPPROC_CACHE_ACCESS",                             "<html><P>Occurs when for accessing all natively compiled stored procedure cache objects.</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
		add("XTPPROC_PARTITIONED_STACK_CREATE",                 "<html><P>Occurs when allocating per-NUMA node natively compiled stored procedure cache structures (must be done single threaded) for a given procedure.</P><TABLE><TBODY><TR><TD><P><STRONG>Applies to</STRONG>: SQL Server 2014 through SQL Server 2016.</P></TD></TR></TBODY></TABLE></html>");
	}
	
	
	public String getWaitClassForWaitType(String waitName)
	{
//		Record rec = _waitTypes.get(waitName);
//		if (rec != null)
//			return rec._className;

        //--------------------------------------------------------------------
        //  https://docs.microsoft.com/en-us/sql/relational-databases/system-catalog-views/sys-query-store-wait-stats-transact-sql?view=sql-server-ver15		
        //--------------------------------------------------------------------
        //		Wait categories mapping table
        //		"%" is used as a wildcard
        //--------------------------------------------------------------------
        
        // +---------------+-------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
        // | Integer value |   Wait category   | Wait types include in the category                                                                                                                                                                                                                              |
        // +---------------+-------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
        // |             0 | Unknown           | Unknown                                                                                                                                                                                                                                                         |
        // |             1 | CPU               | SOS_SCHEDULER_YIELD                                                                                                                                                                                                                                             |
        // |             2 | Worker Thread     | THREADPOOL                                                                                                                                                                                                                                                      |
        // |             3 | Lock              | LCK_M_%                                                                                                                                                                                                                                                         |
        // |             4 | Latch             | LATCH_%                                                                                                                                                                                                                                                         |
        // |             5 | Buffer Latch      | PAGELATCH_%                                                                                                                                                                                                                                                     |
        // |             6 | Buffer IO         | PAGEIOLATCH_%                                                                                                                                                                                                                                                   |
        // |             7 | Compilation*      | RESOURCE_SEMAPHORE_QUERY_COMPILE                                                                                                                                                                                                                                |
        // |             8 | SQL CLR           | CLR%, SQLCLR%                                                                                                                                                                                                                                                   |
        // |             9 | Mirroring         | DBMIRROR%                                                                                                                                                                                                                                                       |
        // |            10 | Transaction       | XACT%, DTC%, TRAN_MARKLATCH_%, MSQL_XACT_%, TRANSACTION_MUTEX                                                                                                                                                                                                   |
        // |            11 | Idle              | SLEEP_%, LAZYWRITER_SLEEP, SQLTRACE_BUFFER_FLUSH, SQLTRACE_INCREMENTAL_FLUSH_SLEEP, SQLTRACE_WAIT_ENTRIES, FT_IFTS_SCHEDULER_IDLE_WAIT, XE_DISPATCHER_WAIT, REQUEST_FOR_DEADLOCK_SEARCH, LOGMGR_QUEUE, ONDEMAND_TASK_QUEUE, CHECKPOINT_QUEUE, XE_TIMER_EVENT    |
        // |            12 | Preemptive        | PREEMPTIVE_%                                                                                                                                                                                                                                                    |
        // |            13 | Service Broker    | BROKER_% (but not BROKER_RECEIVE_WAITFOR)                                                                                                                                                                                                                       |
        // |            14 | Tran Log IO       | LOGMGR, LOGBUFFER, LOGMGR_RESERVE_APPEND, LOGMGR_FLUSH, LOGMGR_PMM_LOG, CHKPT, WRITELOG                                                                                                                                                                         |
        // |            15 | Network IO        | ASYNC_NETWORK_IO, NET_WAITFOR_PACKET, PROXY_NETWORK_IO, EXTERNAL_SCRIPT_NETWORK_IOF                                                                                                                                                                             |
        // |            16 | Parallelism       | CXPACKET, EXCHANGE, HT%, BMP%, BP%                                                                                                                                                                                                                              |
        // |            17 | Memory            | RESOURCE_SEMAPHORE, CMEMTHREAD, CMEMPARTITIONED, EE_PMOLOCK, MEMORY_ALLOCATION_EXT, RESERVED_MEMORY_ALLOCATION_EXT, MEMORY_GRANT_UPDATE                                                                                                                         |
        // |            18 | User Wait         | WAITFOR, WAIT_FOR_RESULTS, BROKER_RECEIVE_WAITFOR                                                                                                                                                                                                               |
        // |            19 | Tracing           | TRACEWRITE, SQLTRACE_LOCK, SQLTRACE_FILE_BUFFER, SQLTRACE_FILE_WRITE_IO_COMPLETION, SQLTRACE_FILE_READ_IO_COMPLETION, SQLTRACE_PENDING_BUFFER_WRITERS, SQLTRACE_SHUTDOWN, QUERY_TRACEOUT, TRACE_EVTNOTIFF                                                       |
        // |            20 | Full Text Search  | FT_RESTART_CRAWL, FULLTEXT GATHERER, MSSEARCH, FT_METADATA_MUTEX, FT_IFTSHC_MUTEX, FT_IFTSISM_MUTEX, FT_IFTS_RWLOCK, FT_COMPROWSET_RWLOCK, FT_MASTER_MERGE, FT_PROPERTYLIST_CACHE, FT_MASTER_MERGE_COORDINATOR, PWAIT_RESOURCE_SEMAPHORE_FT_PARALLEL_QUERY_SYNC |
        // |            21 | Other Disk IO     | ASYNC_IO_COMPLETION, IO_COMPLETION, BACKUPIO, WRITE_COMPLETION, IO_QUEUE_LIMIT, IO_RETRY                                                                                                                                                                        |
        // |            22 | Replication       | SE_REPL_%, REPL_%, HADR_% (but not HADR_THROTTLE_LOG_RATE_GOVERNOR), PWAIT_HADR_%, REPLICA_WRITES, FCB_REPLICA_WRITE, FCB_REPLICA_READ, PWAIT_HADRSIM                                                                                                           |
        // |            23 | Log Rate Governor | LOG_RATE_GOVERNOR, POOL_LOG_RATE_GOVERNOR, HADR_THROTTLE_LOG_RATE_GOVERNOR, INSTANCE_LOG_RATE_GOVERNOR                                                                                                                                                          |
        // +---------------+-------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
        // * Compilation wait category is currently not supported.

		if (   waitName.equals    ("SOS_SCHEDULER_YIELD"))                  return WAIT_CLASS_CPU;
		if (   waitName.equals    ("THREADPOOL"))                           return WAIT_CLASS_Worker_Thread;
		if (   waitName.startsWith("LCK_M_"))                               return WAIT_CLASS_Lock;
		if (   waitName.startsWith("LATCH_"))                               return WAIT_CLASS_Latch;
		if (   waitName.startsWith("PAGELATCH_"))                           return WAIT_CLASS_Buffer_Latch;
		if (   waitName.startsWith("PAGEIOLATCH_"))                         return WAIT_CLASS_Buffer_IO;
		if (   waitName.equals    ("RESOURCE_SEMAPHORE_QUERY_COMPILE"))     return WAIT_CLASS_Compilation;
		if (   waitName.startsWith("CLR") 
		    || waitName.startsWith("SQLCLR")
		   )                                                                return WAIT_CLASS_SQL_CLR;
		if (   waitName.startsWith("DBMIRROR"))                             return WAIT_CLASS_Mirroring;
		if (   waitName.startsWith("XACT") 
		    || waitName.startsWith("DTC")
		    || waitName.startsWith("TRAN_MARKLATCH_")
		    || waitName.startsWith("MSQL_XACT_")
		    || waitName.equals    ("TRANSACTION_MUTEX")
		   )                                                                return WAIT_CLASS_Transaction;
		if (   waitName.startsWith("SLEEP_") 
		    || waitName.equals    ("LAZYWRITER_SLEEP")
		    || waitName.equals    ("SQLTRACE_BUFFER_FLUSH")
		    || waitName.equals    ("SQLTRACE_INCREMENTAL_FLUSH_SLEEP")
		    || waitName.equals    ("SQLTRACE_WAIT_ENTRIES")
		    || waitName.equals    ("FT_IFTS_SCHEDULER_IDLE_WAIT")
		    || waitName.equals    ("XE_DISPATCHER_WAIT")
		    || waitName.equals    ("REQUEST_FOR_DEADLOCK_SEARCH")
		    || waitName.equals    ("LOGMGR_QUEUE")
		    || waitName.equals    ("ONDEMAND_TASK_QUEUE")
		    || waitName.equals    ("CHECKPOINT_QUEUE")
		    || waitName.equals    ("XE_TIMER_EVENT")
		   )                                                                return WAIT_CLASS_Idle;
		if (   waitName.startsWith("PREEMPTIVE_"))                          return WAIT_CLASS_Preemptive;
		if (   waitName.startsWith("BROKER_") 
		     && !waitName.equals("BROKER_RECEIVE_WAITFOR")
		   )                                                                return WAIT_CLASS_Service_Broker;
		if (   waitName.equals    ("LOGMGR")
		    || waitName.equals    ("LOGBUFFER")
		    || waitName.equals    ("LOGMGR_RESERVE_APPEND")
		    || waitName.equals    ("LOGMGR_FLUSH")
		    || waitName.equals    ("LOGMGR_PMM_LOG")
		    || waitName.equals    ("CHKPT")
		    || waitName.equals    ("WRITELOG")
		   )                                                                return WAIT_CLASS_Tran_Log_IO;
		if (   waitName.equals    ("ASYNC_NETWORK_IO")
		    || waitName.equals    ("NET_WAITFOR_PACKET")
		    || waitName.equals    ("PROXY_NETWORK_IO")
		    || waitName.equals    ("EXTERNAL_SCRIPT_NETWORK_IOF")
		   )                                                                return WAIT_CLASS_Network_IO;
		if (   waitName.equals    ("CXPACKET") 
		    || waitName.equals    ("EXCHANGE")
		    || waitName.startsWith("HT")
		    || waitName.startsWith("BMP")
		    || waitName.startsWith("BP")
		   )                                                                return WAIT_CLASS_Parallelism;
		if (   waitName.equals    ("RESOURCE_SEMAPHORE")
		    || waitName.equals    ("CMEMTHREAD")
		    || waitName.equals    ("CMEMPARTITIONED")
		    || waitName.equals    ("EE_PMOLOCK")
		    || waitName.equals    ("MEMORY_ALLOCATION_EXT")
		    || waitName.equals    ("RESERVED_MEMORY_ALLOCATION_EXT")
		    || waitName.equals    ("MEMORY_GRANT_UPDATE")
		   )                                                                return WAIT_CLASS_Memory;
		if (   waitName.equals    ("WAITFOR")
		    || waitName.equals    ("WAIT_FOR_RESULTS")
		    || waitName.equals    ("BROKER_RECEIVE_WAITFOR")
		   )                                                                return WAIT_CLASS_User_Wait;
		if (   waitName.equals    ("TRACEWRITE")
		    || waitName.equals    ("SQLTRACE_LOCK")
		    || waitName.equals    ("SQLTRACE_FILE_BUFFER")
		    || waitName.equals    ("SQLTRACE_FILE_WRITE_IO_COMPLETION")
		    || waitName.equals    ("SQLTRACE_FILE_READ_IO_COMPLETION")
		    || waitName.equals    ("SQLTRACE_PENDING_BUFFER_WRITERS")
		    || waitName.equals    ("SQLTRACE_SHUTDOWN")
		    || waitName.equals    ("QUERY_TRACEOUT")
		    || waitName.equals    ("TRACE_EVTNOTIFF")
		   )                                                                return WAIT_CLASS_Tracing;
		if (   waitName.equals    ("FT_RESTART_CRAWL")
		    || waitName.equals    ("FULLTEXT GATHERER")
		    || waitName.equals    ("MSSEARCH")
		    || waitName.equals    ("FT_METADATA_MUTEX")
		    || waitName.equals    ("FT_IFTSHC_MUTEX")
		    || waitName.equals    ("FT_IFTSISM_MUTEX")
		    || waitName.equals    ("FT_IFTS_RWLOCK")
		    || waitName.equals    ("FT_COMPROWSET_RWLOCK")
		    || waitName.equals    ("FT_MASTER_MERGE")
		    || waitName.equals    ("FT_PROPERTYLIST_CACHE")
		    || waitName.equals    ("FT_MASTER_MERGE_COORDINATOR")
		    || waitName.equals    ("PWAIT_RESOURCE_SEMAPHORE_FT_PARALLEL_QUERY_SYNC")
		   )                                                                return WAIT_CLASS_Full_Text_Search;
		if (   waitName.equals    ("ASYNC_IO_COMPLETION")
		    || waitName.equals    ("IO_COMPLETION")
		    || waitName.equals    ("BACKUPIO")
		    || waitName.equals    ("WRITE_COMPLETION")
		    || waitName.equals    ("IO_QUEUE_LIMIT")
		    || waitName.equals    ("IO_RETRY")
		   )                                                                return WAIT_CLASS_Other_Disk_IO;
		if (   waitName.startsWith("SE_REPL_")
		    || waitName.startsWith("REPL_")
		    ||(waitName.startsWith("HADR_") && !waitName.equals("HADR_THROTTLE_LOG_RATE_GOVERNOR"))
		    || waitName.startsWith("PWAIT_HADR_")
		    || waitName.equals    ("REPLICA_WRITES")
		    || waitName.equals    ("FCB_REPLICA_WRITE")
		    || waitName.equals    ("FCB_REPLICA_READ")
		    || waitName.equals    ("PWAIT_HADRSIM")
		   )                                                                return WAIT_CLASS_Replication;
		if (   waitName.equals    ("LOG_RATE_GOVERNOR")
		    || waitName.equals    ("POOL_LOG_RATE_GOVERNOR")
		    || waitName.equals    ("HADR_THROTTLE_LOG_RATE_GOVERNOR")
		    || waitName.equals    ("INSTANCE_LOG_RATE_GOVERNOR")
		   )                                                                return WAIT_CLASS_Log_Rate_Governor;

		return WAIT_CLASS_Unknown;
	}
	

	public static String WAIT_CLASS_Unknown           = "Unknown";
	public static String WAIT_CLASS_CPU               = "CPU";
	public static String WAIT_CLASS_Worker_Thread     = "Worker Thread";
	public static String WAIT_CLASS_Lock              = "Lock";
	public static String WAIT_CLASS_Latch             = "Latch";
	public static String WAIT_CLASS_Buffer_Latch      = "Buffer Latch";
	public static String WAIT_CLASS_Buffer_IO         = "Buffer IO";
	public static String WAIT_CLASS_Compilation       = "Compilation";
	public static String WAIT_CLASS_SQL_CLR           = "SQL CLR";
	public static String WAIT_CLASS_Mirroring         = "Mirroring";
	public static String WAIT_CLASS_Transaction       = "Transaction";
	public static String WAIT_CLASS_Idle              = "Idle";
	public static String WAIT_CLASS_Preemptive        = "Preemptive";
	public static String WAIT_CLASS_Service_Broker    = "Service Broker";
	public static String WAIT_CLASS_Tran_Log_IO       = "Tran Log IO";
	public static String WAIT_CLASS_Network_IO        = "Network IO";
	public static String WAIT_CLASS_Parallelism       = "Parallelism";
	public static String WAIT_CLASS_Memory            = "Memory";
	public static String WAIT_CLASS_User_Wait         = "User Wait";
	public static String WAIT_CLASS_Tracing           = "Tracing";
	public static String WAIT_CLASS_Full_Text_Search  = "Full Text Search";
	public static String WAIT_CLASS_Other_Disk_IO     = "Other Disk IO";
	public static String WAIT_CLASS_Replication       = "Replication";
	public static String WAIT_CLASS_Log_Rate_Governor = "Log Rate Governor";

	public static String[] WAIT_CLASS_ALL_ARRAY       = new String[] 
	{
		 WAIT_CLASS_Unknown          
		,WAIT_CLASS_CPU              
		,WAIT_CLASS_Worker_Thread    
		,WAIT_CLASS_Lock             
		,WAIT_CLASS_Latch            
		,WAIT_CLASS_Buffer_Latch     
		,WAIT_CLASS_Buffer_IO        
		,WAIT_CLASS_Compilation      
		,WAIT_CLASS_SQL_CLR          
		,WAIT_CLASS_Mirroring        
		,WAIT_CLASS_Transaction      
		,WAIT_CLASS_Idle             
		,WAIT_CLASS_Preemptive       
		,WAIT_CLASS_Service_Broker   
		,WAIT_CLASS_Tran_Log_IO      
		,WAIT_CLASS_Network_IO       
		,WAIT_CLASS_Parallelism      
		,WAIT_CLASS_Memory           
		,WAIT_CLASS_User_Wait        
		,WAIT_CLASS_Tracing          
		,WAIT_CLASS_Full_Text_Search 
		,WAIT_CLASS_Other_Disk_IO    
		,WAIT_CLASS_Replication      
		,WAIT_CLASS_Log_Rate_Governor
	};

	public static List<String> WAIT_CLASS_ALL_LIST = Arrays.asList(WAIT_CLASS_ALL_ARRAY);
}
