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

import java.util.HashMap;

import com.dbxtune.utils.StringUtil;

public class PostgresWaitTypeDictionary
{
	/** Instance variable */
	private static PostgresWaitTypeDictionary _instance = null;

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


	public PostgresWaitTypeDictionary()
	{
		init();
	}

	public static PostgresWaitTypeDictionary getInstance()
	{
		if (_instance == null)
			_instance = new PostgresWaitTypeDictionary();
		return _instance;
	}

	/**
	 * Strips out all HTML and return it as a "plain" text
	 * @param waitName
	 * @return
	 */
	public String getDescriptionPlain(String waitName, boolean doPrefix)
	{
		WaitTypeRecord rec = _waitTypes.get(waitName);
		if (rec != null)
		{
			if (doPrefix)
				return StringUtil.stripHtml(rec._description.replace("[ID]", waitName));
			else
				return StringUtil.stripHtml(rec._description.replace("<html><b>[ID]</b> - ", ""));
		}

		// Compose an empty one
		return "";
//		return "WaitName '"+waitName+"' not found in dictionary.";
	}


	public String getDescriptionHtml(String waitName)
	{
		String extraInfo = "";
//		String extraInfo = "<br><hr>External Description, from: Paul Randal, www.sqlskills.com<br>"
//				+ "Open in Tooltip Window:   <A HREF='https://www.sqlskills.com/help/waits/"+waitName+"'>https://www.sqlskills.com/help/waits/"+waitName+"</A><br>"
//				+ "Open in External Browser: <A HREF='"+CmToolTipSupplierDefault.OPEN_IN_EXTERNAL_BROWSER+"https://www.sqlskills.com/help/waits/"+waitName+"'>https://www.sqlskills.com/help/waits/"+waitName+"</A><br>"
//				+ "</html>";

		WaitTypeRecord rec = _waitTypes.get(waitName);
		if (rec != null)
		{
			String str = rec._description.replace("[ID]", waitName);
			
//			str = str.replace("</html>", extraInfo);
			return str;
		}

		// Compose an empty one
		return "<html><code>"+waitName+"</code> not found in dictionary."+extraInfo;
	}


	private void set(WaitTypeRecord rec)
	{
		if ( _waitTypes.containsKey(rec._id))
			System.out.println("ID '"+rec._id+"' already exists. It will be overwritten.");

		_waitTypes.put(rec._id, rec);
	}

	private void add(String id, String description)
	{
		set(new WaitTypeRecord(id, description));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// NOTE: fields 'SyncRep' and 'WALWrite' are NOT unique... So I did the easy way... comment one of them out
	// Long term solution: add 'EventType' as a "prefix" (in it's own field) but then we need to specify what "section" we want to look in when we get the description...
	// Example:	add("IPC"   , "SyncRep",  "<html><b>[ID]</b> - Waiting for confirmation from a remote server during synchronous replication.</html>");
	//          add("LWLock", "SyncRep",  "<html><b>[ID]</b> - Waiting to read or update information about the state of synchronous replication.</html>");
	// Lets start simple for now: one of the duplicate entries was commented out :) 
	//-----------------------------------------------------------------------------------------------------------------
	private void init()
	{
		// Table 28.5. Wait Events of Type Activity
		add("ArchiverMain",                  "<html><b>[ID]</b> - Waiting in main loop of archiver process.</html>");
		add("AutoVacuumMain",                "<html><b>[ID]</b> - Waiting in main loop of autovacuum launcher process.</html>");
		add("BgWriterHibernate",             "<html><b>[ID]</b> - Waiting in background writer process, hibernating.</html>");
		add("BgWriterMain",                  "<html><b>[ID]</b> - Waiting in main loop of background writer process.</html>");
		add("CheckpointerMain",              "<html><b>[ID]</b> - Waiting in main loop of checkpointer process.</html>");
		add("LogicalApplyMain",              "<html><b>[ID]</b> - Waiting in main loop of logical replication apply process.</html>");
		add("LogicalLauncherMain",           "<html><b>[ID]</b> - Waiting in main loop of logical replication launcher process.</html>");
		add("LogicalParallelApplyMain",      "<html><b>[ID]</b> - Waiting in main loop of logical replication parallel apply process</html>");
		add("RecoveryWalStream",             "<html><b>[ID]</b> - Waiting in main loop of startup process for WAL to arrive, during streaming recovery.</html>");
		add("SysLoggerMain",                 "<html><b>[ID]</b> - Waiting in main loop of syslogger process.</html>");
		add("WalReceiverMain",               "<html><b>[ID]</b> - Waiting in main loop of WAL receiver process.</html>");
		add("WalSenderMain",                 "<html><b>[ID]</b> - Waiting in main loop of WAL sender process.</html>");
		add("WalWriterMain",                 "<html><b>[ID]</b> - Waiting in main loop of WAL writer process.</html>");

		// Table 28.6. Wait Events of Type BufferPin
		add("BufferPin",                     "<html>Waiting to acquire an exclusive pin on a buffer.</html>");

		// Table 28.7. Wait Events of Type Client
		add("ClientRead",                    "<html><b>[ID]</b> - Waiting to read data from the client.</html>");
		add("ClientWrite",                   "<html><b>[ID]</b> - Waiting to write data to the client.</html>");
		add("GSSOpenServer",                 "<html><b>[ID]</b> - Waiting to read data from the client while establishing a GSSAPI session.</html>");
		add("LibPQWalReceiverConnect",       "<html><b>[ID]</b> - Waiting in WAL receiver to establish connection to remote server.</html>");
		add("LibPQWalReceiverReceive",       "<html><b>[ID]</b> - Waiting in WAL receiver to receive data from remote server.</html>");
		add("SSLOpenServer",                 "<html><b>[ID]</b> - Waiting for SSL while attempting connection.</html>");
		add("WalSenderWaitForWAL",           "<html><b>[ID]</b> - Waiting for WAL to be flushed in WAL sender process.</html>");
		add("WalSenderWriteData",            "<html><b>[ID]</b> - Waiting for any activity when processing replies from WAL receiver in WAL sender process.</html>");

		// Table 28.8. Wait Events of Type Extension
		add("Extension",                     "<html>Waiting in an extension.</html>");

		// Table 28.9. Wait Events of Type IO
		add("BaseBackupRead",                "<html><b>[ID]</b> - Waiting for base backup to read from a file.</html>");
		add("BaseBackupSync",                "<html><b>[ID]</b> - Waiting for data written by a base backup to reach durable storage.</html>");
		add("BaseBackupWrite",               "<html><b>[ID]</b> - Waiting for base backup to write to a file.</html>");
		add("BufFileRead",                   "<html><b>[ID]</b> - Waiting for a read from a buffered file.</html>");
		add("BufFileTruncate",               "<html><b>[ID]</b> - Waiting for a buffered file to be truncated.</html>");
		add("BufFileWrite",                  "<html><b>[ID]</b> - Waiting for a write to a buffered file.</html>");
		add("ControlFileRead",               "<html><b>[ID]</b> - Waiting for a read from the pg_control file.</html>");
		add("ControlFileSync",               "<html><b>[ID]</b> - Waiting for the pg_control file to reach durable storage.</html>");
		add("ControlFileSyncUpdate",         "<html><b>[ID]</b> - Waiting for an update to the pg_control file to reach durable storage.</html>");
		add("ControlFileWrite",              "<html><b>[ID]</b> - Waiting for a write to the pg_control file.</html>");
		add("ControlFileWriteUpdate",        "<html><b>[ID]</b> - Waiting for a write to update the pg_control file.</html>");
		add("CopyFileRead",                  "<html><b>[ID]</b> - Waiting for a read during a file copy operation.</html>");
		add("CopyFileWrite",                 "<html><b>[ID]</b> - Waiting for a write during a file copy operation.</html>");
		add("DSMAllocate",                   "<html><b>[ID]</b> - Waiting for a dynamic shared memory segment to be allocated.</html>");
		add("DSMFillZeroWrite",              "<html><b>[ID]</b> - Waiting to fill a dynamic shared memory backing file with zeroes.</html>");
		add("DataFileExtend",                "<html><b>[ID]</b> - Waiting for a relation data file to be extended.</html>");
		add("DataFileFlush",                 "<html><b>[ID]</b> - Waiting for a relation data file to reach durable storage.</html>");
		add("DataFileImmediateSync",         "<html><b>[ID]</b> - Waiting for an immediate synchronization of a relation data file to durable storage.</html>");
		add("DataFilePrefetch",              "<html><b>[ID]</b> - Waiting for an asynchronous prefetch from a relation data file.</html>");
		add("DataFileRead",                  "<html><b>[ID]</b> - Waiting for a read from a relation data file.</html>");
		add("DataFileSync",                  "<html><b>[ID]</b> - Waiting for changes to a relation data file to reach durable storage.</html>");
		add("DataFileTruncate",              "<html><b>[ID]</b> - Waiting for a relation data file to be truncated.</html>");
		add("DataFileWrite",                 "<html><b>[ID]</b> - Waiting for a write to a relation data file.</html>");
		add("LockFileAddToDataDirRead",      "<html><b>[ID]</b> - Waiting for a read while adding a line to the data directory lock file.</html>");
		add("LockFileAddToDataDirSync",      "<html><b>[ID]</b> - Waiting for data to reach durable storage while adding a line to the data directory lock file.</html>");
		add("LockFileAddToDataDirWrite",     "<html><b>[ID]</b> - Waiting for a write while adding a line to the data directory lock file.</html>");
		add("LockFileCreateRead",            "<html><b>[ID]</b> - Waiting to read while creating the data directory lock file.</html>");
		add("LockFileCreateSync",            "<html><b>[ID]</b> - Waiting for data to reach durable storage while creating the data directory lock file.</html>");
		add("LockFileCreateWrite",           "<html><b>[ID]</b> - Waiting for a write while creating the data directory lock file.</html>");
		add("LockFileReCheckDataDirRead",    "<html><b>[ID]</b> - Waiting for a read during recheck of the data directory lock file.</html>");
		add("LogicalRewriteCheckpointSync",  "<html><b>[ID]</b> - Waiting for logical rewrite mappings to reach durable storage during a checkpoint.</html>");
		add("LogicalRewriteMappingSync",     "<html><b>[ID]</b> - Waiting for mapping data to reach durable storage during a logical rewrite.</html>");
		add("LogicalRewriteMappingWrite",    "<html><b>[ID]</b> - Waiting for a write of mapping data during a logical rewrite.</html>");
		add("LogicalRewriteSync",            "<html><b>[ID]</b> - Waiting for logical rewrite mappings to reach durable storage.</html>");
		add("LogicalRewriteTruncate",        "<html><b>[ID]</b> - Waiting for truncate of mapping data during a logical rewrite.</html>");
		add("LogicalRewriteWrite",           "<html><b>[ID]</b> - Waiting for a write of logical rewrite mappings.</html>");
		add("RelationMapRead",               "<html><b>[ID]</b> - Waiting for a read of the relation map file.</html>");
		add("RelationMapSync",               "<html><b>[ID]</b> - Waiting for the relation map file to reach durable storage.</html>");
		add("RelationMapWrite",              "<html><b>[ID]</b> - Waiting for a write to the relation map file.</html>");
		add("ReorderBufferRead",             "<html><b>[ID]</b> - Waiting for a read during reorder buffer management.</html>");
		add("ReorderBufferWrite",            "<html><b>[ID]</b> - Waiting for a write during reorder buffer management.</html>");
		add("ReorderLogicalMappingRead",     "<html><b>[ID]</b> - Waiting for a read of a logical mapping during reorder buffer management.</html>");
		add("ReplicationSlotRead",           "<html><b>[ID]</b> - Waiting for a read from a replication slot control file.</html>");
		add("ReplicationSlotRestoreSync",    "<html><b>[ID]</b> - Waiting for a replication slot control file to reach durable storage while restoring it to memory.</html>");
		add("ReplicationSlotSync",           "<html><b>[ID]</b> - Waiting for a replication slot control file to reach durable storage.</html>");
		add("ReplicationSlotWrite",          "<html><b>[ID]</b> - Waiting for a write to a replication slot control file.</html>");
		add("SLRUFlushSync",                 "<html><b>[ID]</b> - Waiting for SLRU data to reach durable storage during a checkpoint or database shutdown.</html>");
		add("SLRURead",                      "<html><b>[ID]</b> - Waiting for a read of an SLRU page.</html>");
		add("SLRUSync",                      "<html><b>[ID]</b> - Waiting for SLRU data to reach durable storage following a page write.</html>");
		add("SLRUWrite",                     "<html><b>[ID]</b> - Waiting for a write of an SLRU page.</html>");
		add("SnapbuildRead",                 "<html><b>[ID]</b> - Waiting for a read of a serialized historical catalog snapshot.</html>");
		add("SnapbuildSync",                 "<html><b>[ID]</b> - Waiting for a serialized historical catalog snapshot to reach durable storage.</html>");
		add("SnapbuildWrite",                "<html><b>[ID]</b> - Waiting for a write of a serialized historical catalog snapshot.</html>");
		add("TimelineHistoryFileSync",       "<html><b>[ID]</b> - Waiting for a timeline history file received via streaming replication to reach durable storage.</html>");
		add("TimelineHistoryFileWrite",      "<html><b>[ID]</b> - Waiting for a write of a timeline history file received via streaming replication.</html>");
		add("TimelineHistoryRead",           "<html><b>[ID]</b> - Waiting for a read of a timeline history file.</html>");
		add("TimelineHistorySync",           "<html><b>[ID]</b> - Waiting for a newly created timeline history file to reach durable storage.</html>");
		add("TimelineHistoryWrite",          "<html><b>[ID]</b> - Waiting for a write of a newly created timeline history file.</html>");
		add("TwophaseFileRead",              "<html><b>[ID]</b> - Waiting for a read of a two phase state file.</html>");
		add("TwophaseFileSync",              "<html><b>[ID]</b> - Waiting for a two phase state file to reach durable storage.</html>");
		add("TwophaseFileWrite",             "<html><b>[ID]</b> - Waiting for a write of a two phase state file.</html>");
		add("VersionFileWrite",              "<html><b>[ID]</b> - Waiting for the version file to be written while creating a database.</html>");
		add("WALBootstrapSync",              "<html><b>[ID]</b> - Waiting for WAL to reach durable storage during bootstrapping.</html>");
		add("WALBootstrapWrite",             "<html><b>[ID]</b> - Waiting for a write of a WAL page during bootstrapping.</html>");
		add("WALCopyRead",                   "<html><b>[ID]</b> - Waiting for a read when creating a new WAL segment by copying an existing one.</html>");
		add("WALCopySync",                   "<html><b>[ID]</b> - Waiting for a new WAL segment created by copying an existing one to reach durable storage.</html>");
		add("WALCopyWrite",                  "<html><b>[ID]</b> - Waiting for a write when creating a new WAL segment by copying an existing one.</html>");
		add("WALInitSync",                   "<html><b>[ID]</b> - Waiting for a newly initialized WAL file to reach durable storage.</html>");
		add("WALInitWrite",                  "<html><b>[ID]</b> - Waiting for a write while initializing a new WAL file.</html>");
		add("WALRead",                       "<html><b>[ID]</b> - Waiting for a read from a WAL file.</html>");
		add("WALSenderTimelineHistoryRead",  "<html><b>[ID]</b> - Waiting for a read from a timeline history file during a walsender timeline command.</html>");
		add("WALSync",                       "<html><b>[ID]</b> - Waiting for a WAL file to reach durable storage.</html>");
		add("WALSyncMethodAssign",           "<html><b>[ID]</b> - Waiting for data to reach durable storage while assigning a new WAL sync method.</html>");
//		add("WALWrite",                      "<html><b>[ID]</b> - Waiting for a write to a WAL file.</html>"); // This will be a duplicate

		// Table 28.10. Wait Events of Type IPC
		add("AppendReady",                   "<html><b>[ID]</b> - Waiting for subplan nodes of an Append plan node to be ready.</html>");
		add("ArchiveCleanupCommand",         "<html><b>[ID]</b> - Waiting for archive_cleanup_command to complete.</html>");
		add("ArchiveCommand",                "<html><b>[ID]</b> - Waiting for archive_command to complete.</html>");
		add("BackendTermination",            "<html><b>[ID]</b> - Waiting for the termination of another backend.</html>");
		add("BackupWaitWalArchive",          "<html><b>[ID]</b> - Waiting for WAL files required for a backup to be successfully archived.</html>");
		add("BgWorkerShutdown",              "<html><b>[ID]</b> - Waiting for background worker to shut down.</html>");
		add("BgWorkerStartup",               "<html><b>[ID]</b> - Waiting for background worker to start up.</html>");
		add("BtreePage",                     "<html><b>[ID]</b> - Waiting for the page number needed to continue a parallel B-tree scan to become available.</html>");
		add("BufferIO",                      "<html><b>[ID]</b> - Waiting for buffer I/O to complete.</html>");
		add("CheckpointDone",                "<html><b>[ID]</b> - Waiting for a checkpoint to complete.</html>");
		add("CheckpointStart",               "<html><b>[ID]</b> - Waiting for a checkpoint to start.</html>");
		add("ExecuteGather",                 "<html><b>[ID]</b> - Waiting for activity from a child process while executing a Gather plan node.</html>");
		add("HashBatchAllocate",             "<html><b>[ID]</b> - Waiting for an elected Parallel Hash participant to allocate a hash table.</html>");
		add("HashBatchElect",                "<html><b>[ID]</b> - Waiting to elect a Parallel Hash participant to allocate a hash table.</html>");
		add("HashBatchLoad",                 "<html><b>[ID]</b> - Waiting for other Parallel Hash participants to finish loading a hash table.</html>");
		add("HashBuildAllocate",             "<html><b>[ID]</b> - Waiting for an elected Parallel Hash participant to allocate the initial hash table.</html>");
		add("HashBuildElect",                "<html><b>[ID]</b> - Waiting to elect a Parallel Hash participant to allocate the initial hash table.</html>");
		add("HashBuildHashInner",            "<html><b>[ID]</b> - Waiting for other Parallel Hash participants to finish hashing the inner relation.</html>");
		add("HashBuildHashOuter",            "<html><b>[ID]</b> - Waiting for other Parallel Hash participants to finish partitioning the outer relation.</html>");
		add("HashGrowBatchesAllocate",       "<html><b>[ID]</b> - Waiting for an elected Parallel Hash participant to allocate more batches.</html>");
		add("HashGrowBatchesDecide",         "<html><b>[ID]</b> - Waiting to elect a Parallel Hash participant to decide on future batch growth.</html>");
		add("HashGrowBatchesElect",          "<html><b>[ID]</b> - Waiting to elect a Parallel Hash participant to allocate more batches.</html>");
		add("HashGrowBatchesFinish",         "<html><b>[ID]</b> - Waiting for an elected Parallel Hash participant to decide on future batch growth.</html>");
		add("HashGrowBatchesRepartition",    "<html><b>[ID]</b> - Waiting for other Parallel Hash participants to finish repartitioning.</html>");
		add("HashGrowBucketsAllocate",       "<html><b>[ID]</b> - Waiting for an elected Parallel Hash participant to finish allocating more buckets.</html>");
		add("HashGrowBucketsElect",          "<html><b>[ID]</b> - Waiting to elect a Parallel Hash participant to allocate more buckets.</html>");
		add("HashGrowBucketsReinsert",       "<html><b>[ID]</b> - Waiting for other Parallel Hash participants to finish inserting tuples into new buckets.</html>");
		add("LogicalApplySendData",          "<html><b>[ID]</b> - Waiting for a logical replication leader apply process to send data to a parallel apply process.</html>");
		add("LogicalParallelApplyStateChange", "<html><b>[ID]</b> - Waiting for a logical replication parallel apply process to change state.</html>");
		add("LogicalSyncData",               "<html><b>[ID]</b> - Waiting for a logical replication remote server to send data for initial table synchronization.</html>");
		add("LogicalSyncStateChange",        "<html><b>[ID]</b> - Waiting for a logical replication remote server to change state.</html>");
		add("MessageQueueInternal",          "<html><b>[ID]</b> - Waiting for another process to be attached to a shared message queue.</html>");
		add("MessageQueuePutMessage",        "<html><b>[ID]</b> - Waiting to write a protocol message to a shared message queue.</html>");
		add("MessageQueueReceive",           "<html><b>[ID]</b> - Waiting to receive bytes from a shared message queue.</html>");
		add("MessageQueueSend",              "<html><b>[ID]</b> - Waiting to send bytes to a shared message queue.</html>");
		add("ParallelBitmapScan",            "<html><b>[ID]</b> - Waiting for parallel bitmap scan to become initialized.</html>");
		add("ParallelCreateIndexScan",       "<html><b>[ID]</b> - Waiting for parallel CREATE INDEX workers to finish heap scan.</html>");
		add("ParallelFinish",                "<html><b>[ID]</b> - Waiting for parallel workers to finish computing.</html>");
		add("ProcArrayGroupUpdate",          "<html><b>[ID]</b> - Waiting for the group leader to clear the transaction ID at end of a parallel operation.</html>");
		add("ProcSignalBarrier",             "<html><b>[ID]</b> - Waiting for a barrier event to be processed by all backends.</html>");
		add("Promote",                       "<html><b>[ID]</b> - Waiting for standby promotion.</html>");
		add("RecoveryConflictSnapshot",      "<html><b>[ID]</b> - Waiting for recovery conflict resolution for a vacuum cleanup.</html>");
		add("RecoveryConflictTablespace",    "<html><b>[ID]</b> - Waiting for recovery conflict resolution for dropping a tablespace.</html>");
		add("RecoveryEndCommand",            "<html><b>[ID]</b> - Waiting for recovery_end_command to complete.</html>");
		add("RecoveryPause",                 "<html><b>[ID]</b> - Waiting for recovery to be resumed.</html>");
		add("ReplicationOriginDrop",         "<html><b>[ID]</b> - Waiting for a replication origin to become inactive so it can be dropped.</html>");
		add("ReplicationSlotDrop",           "<html><b>[ID]</b> - Waiting for a replication slot to become inactive so it can be dropped.</html>");
		add("RestoreCommand",                "<html><b>[ID]</b> - Waiting for restore_command to complete.</html>");
		add("SafeSnapshot",                  "<html><b>[ID]</b> - Waiting to obtain a valid snapshot for a READ ONLY DEFERRABLE transaction.</html>");
		add("SyncRep",                       "<html><b>[ID]</b> - Waiting for confirmation from a remote server during synchronous replication.</html>");
		add("WalReceiverExit",               "<html><b>[ID]</b> - Waiting for the WAL receiver to exit.</html>");
		add("WalReceiverWaitStart",          "<html><b>[ID]</b> - Waiting for startup process to send initial data for streaming replication.</html>");
		add("XactGroupUpdate",               "<html><b>[ID]</b> - Waiting for the group leader to update transaction status at end of a parallel operation.</html>");

		// Table 28.11. Wait Events of Type Lock
		add("advisory",                      "<html><b>[ID]</b> - Waiting to acquire an advisory user lock.</html>");
		add("applytransaction",              "<html><b>[ID]</b> - Waiting to acquire a lock on a remote transaction being applied by a logical replication subscriber.</html>");
		add("extend",                        "<html><b>[ID]</b> - Waiting to extend a relation.</html>");
		add("frozenid",                      "<html><b>[ID]</b> - Waiting to update pg_database.datfrozenxid and pg_database.datminmxid.</html>");
		add("object",                        "<html><b>[ID]</b> - Waiting to acquire a lock on a non-relation database object.</html>");
		add("page",                          "<html><b>[ID]</b> - Waiting to acquire a lock on a page of a relation.</html>");
		add("relation",                      "<html><b>[ID]</b> - Waiting to acquire a lock on a relation.</html>");
		add("spectoken",                     "<html><b>[ID]</b> - Waiting to acquire a speculative insertion lock.</html>");
		add("transactionid",                 "<html><b>[ID]</b> - Waiting for a transaction to finish.</html>");
		add("tuple",                         "<html><b>[ID]</b> - Waiting to acquire a lock on a tuple.</html>");
		add("userlock",                      "<html><b>[ID]</b> - Waiting to acquire a user lock.</html>");
		add("virtualxid",                    "<html><b>[ID]</b> - Waiting to acquire a virtual transaction ID lock.</html>");

		// Table 28.12. Wait Events of Type LWLock
		add("AddinShmemInit",                "<html><b>[ID]</b> - Waiting to manage an extension's space allocation in shared memory.</html>");
		add("AutoFile",                      "<html><b>[ID]</b> - Waiting to update the postgresql.auto.conf file.</html>");
		add("Autovacuum",                    "<html><b>[ID]</b> - Waiting to read or update the current state of autovacuum workers.</html>");
		add("AutovacuumSchedule",            "<html><b>[ID]</b> - Waiting to ensure that a table selected for autovacuum still needs vacuuming.</html>");
		add("BackgroundWorker",              "<html><b>[ID]</b> - Waiting to read or update background worker state.</html>");
		add("BtreeVacuum",                   "<html><b>[ID]</b> - Waiting to read or update vacuum-related information for a B-tree index.</html>");
		add("BufferContent",                 "<html><b>[ID]</b> - Waiting to access a data page in memory.</html>");
		add("BufferMapping",                 "<html><b>[ID]</b> - Waiting to associate a data block with a buffer in the buffer pool.</html>");
		add("CheckpointerComm",              "<html><b>[ID]</b> - Waiting to manage fsync requests.</html>");
		add("CommitTs",                      "<html><b>[ID]</b> - Waiting to read or update the last value set for a transaction commit timestamp.</html>");
		add("CommitTsBuffer",                "<html><b>[ID]</b> - Waiting for I/O on a commit timestamp SLRU buffer.</html>");
		add("CommitTsSLRU",                  "<html><b>[ID]</b> - Waiting to access the commit timestamp SLRU cache.</html>");
		add("ControlFile",                   "<html><b>[ID]</b> - Waiting to read or update the pg_control file or create a new WAL file.</html>");
		add("DynamicSharedMemoryControl",    "<html><b>[ID]</b> - Waiting to read or update dynamic shared memory allocation information.</html>");
		add("LockFastPath",                  "<html><b>[ID]</b> - Waiting to read or update a process' fast-path lock information.</html>");
		add("LockManager",                   "<html><b>[ID]</b> - Waiting to read or update information about \"heavyweight\" locks.</html>");
		add("LogicalRepLauncherDSA",         "<html><b>[ID]</b> - Waiting to access logical replication launcher's dynamic shared memory allocator.</html>");
		add("LogicalRepLauncherHash",        "<html><b>[ID]</b> - Waiting to access logical replication launcher's shared hash table.</html>");
		add("LogicalRepWorker",              "<html><b>[ID]</b> - Waiting to read or update the state of logical replication workers.</html>");
		add("MultiXactGen",                  "<html><b>[ID]</b> - Waiting to read or update shared multixact state.</html>");
		add("MultiXactMemberBuffer",         "<html><b>[ID]</b> - Waiting for I/O on a multixact member SLRU buffer.</html>");
		add("MultiXactMemberSLRU",           "<html><b>[ID]</b> - Waiting to access the multixact member SLRU cache.</html>");
		add("MultiXactOffsetBuffer",         "<html><b>[ID]</b> - Waiting for I/O on a multixact offset SLRU buffer.</html>");
		add("MultiXactOffsetSLRU",           "<html><b>[ID]</b> - Waiting to access the multixact offset SLRU cache.</html>");
		add("MultiXactTruncation",           "<html><b>[ID]</b> - Waiting to read or truncate multixact information.</html>");
		add("NotifyBuffer",                  "<html><b>[ID]</b> - Waiting for I/O on a NOTIFY message SLRU buffer.</html>");
		add("NotifyQueue",                   "<html><b>[ID]</b> - Waiting to read or update NOTIFY messages.</html>");
		add("NotifyQueueTail",               "<html><b>[ID]</b> - Waiting to update limit on NOTIFY message storage.</html>");
		add("NotifySLRU",                    "<html><b>[ID]</b> - Waiting to access the NOTIFY message SLRU cache.</html>");
		add("OidGen",                        "<html><b>[ID]</b> - Waiting to allocate a new OID.</html>");
		add("OldSnapshotTimeMap",            "<html><b>[ID]</b> - Waiting to read or update old snapshot control information.</html>");
		add("ParallelAppend",                "<html><b>[ID]</b> - Waiting to choose the next subplan during Parallel Append plan execution.</html>");
		add("ParallelHashJoin",              "<html><b>[ID]</b> - Waiting to synchronize workers during Parallel Hash Join plan execution.</html>");
		add("ParallelQueryDSA",              "<html><b>[ID]</b> - Waiting for parallel query dynamic shared memory allocation.</html>");
		add("PerSessionDSA",                 "<html><b>[ID]</b> - Waiting for parallel query dynamic shared memory allocation.</html>");
		add("PerSessionRecordType",          "<html><b>[ID]</b> - Waiting to access a parallel query's information about composite types.</html>");
		add("PerSessionRecordTypmod",        "<html><b>[ID]</b> - Waiting to access a parallel query's information about type modifiers that identify anonymous record types.</html>");
		add("PerXactPredicateList",          "<html><b>[ID]</b> - Waiting to access the list of predicate locks held by the current serializable transaction during a parallel query.</html>");
		add("PgStatsData",                   "<html><b>[ID]</b> - Waiting for shared memory stats data access.</html>");
		add("PgStatsDSA",                    "<html><b>[ID]</b> - Waiting for stats dynamic shared memory allocator access.</html>");
		add("PgStatsHash",                   "<html><b>[ID]</b> - Waiting for stats shared memory hash table access.</html>");
		add("PredicateLockManager",          "<html><b>[ID]</b> - Waiting to access predicate lock information used by serializable transactions.</html>");
		add("ProcArray",                     "<html><b>[ID]</b> - Waiting to access the shared per-process data structures (typically, to get a snapshot or report a session's transaction ID).</html>");
		add("RelationMapping",               "<html><b>[ID]</b> - Waiting to read or update a pg_filenode.map file (used to track the filenode assignments of certain system catalogs).</html>");
		add("RelCacheInit",                  "<html><b>[ID]</b> - Waiting to read or update a pg_internal.init relation cache initialization file.</html>");
		add("ReplicationOrigin",             "<html><b>[ID]</b> - Waiting to create, drop or use a replication origin.</html>");
		add("ReplicationOriginState",        "<html><b>[ID]</b> - Waiting to read or update the progress of one replication origin.</html>");
		add("ReplicationSlotAllocation",     "<html><b>[ID]</b> - Waiting to allocate or free a replication slot.</html>");
		add("ReplicationSlotControl",        "<html><b>[ID]</b> - Waiting to read or update replication slot state.</html>");
		add("ReplicationSlotIO",             "<html><b>[ID]</b> - Waiting for I/O on a replication slot.</html>");
		add("SerialBuffer",                  "<html><b>[ID]</b> - Waiting for I/O on a serializable transaction conflict SLRU buffer.</html>");
		add("SerializableFinishedList",      "<html><b>[ID]</b> - Waiting to access the list of finished serializable transactions.</html>");
		add("SerializablePredicateList",     "<html><b>[ID]</b> - Waiting to access the list of predicate locks held by serializable transactions.</html>");
		add("SerializableXactHash",          "<html><b>[ID]</b> - Waiting to read or update information about serializable transactions.</html>");
		add("SerialSLRU",                    "<html><b>[ID]</b> - Waiting to access the serializable transaction conflict SLRU cache.</html>");
		add("SharedTidBitmap",               "<html><b>[ID]</b> - Waiting to access a shared TID bitmap during a parallel bitmap index scan.</html>");
		add("SharedTupleStore",              "<html><b>[ID]</b> - Waiting to access a shared tuple store during parallel query.</html>");
		add("ShmemIndex",                    "<html><b>[ID]</b> - Waiting to find or allocate space in shared memory.</html>");
		add("SInvalRead",                    "<html><b>[ID]</b> - Waiting to retrieve messages from the shared catalog invalidation queue.</html>");
		add("SInvalWrite",                   "<html><b>[ID]</b> - Waiting to add a message to the shared catalog invalidation queue.</html>");
		add("SubtransBuffer",                "<html><b>[ID]</b> - Waiting for I/O on a sub-transaction SLRU buffer.</html>");
		add("SubtransSLRU",                  "<html><b>[ID]</b> - Waiting to access the sub-transaction SLRU cache.</html>");
//		add("SyncRep",                       "<html><b>[ID]</b> - Waiting to read or update information about the state of synchronous replication.</html>"); // This will be a duplicate
		add("SyncScan",                      "<html><b>[ID]</b> - Waiting to select the starting location of a synchronized table scan.</html>");
		add("TablespaceCreate",              "<html><b>[ID]</b> - Waiting to create or drop a tablespace.</html>");
		add("TwoPhaseState",                 "<html><b>[ID]</b> - Waiting to read or update the state of prepared transactions.</html>");
		add("WALBufMapping",                 "<html><b>[ID]</b> - Waiting to replace a page in WAL buffers.</html>");
		add("WALInsert",                     "<html><b>[ID]</b> - Waiting to insert WAL data into a memory buffer.</html>");
		add("WALWrite",                      "<html><b>[ID]</b> - Waiting for WAL buffers to be written to disk.</html>");
		add("WrapLimitsVacuum",              "<html><b>[ID]</b> - Waiting to update limits on transaction id and multixact consumption.</html>");
		add("XactBuffer",                    "<html><b>[ID]</b> - Waiting for I/O on a transaction status SLRU buffer.</html>");
		add("XactSLRU",                      "<html><b>[ID]</b> - Waiting to access the transaction status SLRU cache.</html>");
		add("XactTruncation",                "<html><b>[ID]</b> - Waiting to execute pg_xact_status or update the oldest transaction ID available to it.</html>");
		add("XidGen",                        "<html><b>[ID]</b> - Waiting to allocate a new transaction ID.</html>");

		// Table 28.13. Wait Events of Type Timeout
		add("BaseBackupThrottle",            "<html><b>[ID]</b> - Waiting during base backup when throttling activity.</html>");
		add("CheckpointWriteDelay",          "<html><b>[ID]</b> - Waiting between writes while performing a checkpoint.</html>");
		add("PgSleep",                       "<html><b>[ID]</b> - Waiting due to a call to pg_sleep or a sibling function.</html>");
		add("RecoveryApplyDelay",            "<html><b>[ID]</b> - Waiting to apply WAL during recovery because of a delay setting.</html>");
		add("RecoveryRetrieveRetryInterval", "<html><b>[ID]</b> - Waiting during recovery when WAL data is not available from any source (pg_wal, archive or stream).</html>");
		add("RegisterSyncRequest",           "<html><b>[ID]</b> - Waiting while sending synchronization requests to the checkpointer, because the request queue is full.</html>");
		add("SpinDelay",                     "<html><b>[ID]</b> - Waiting while acquiring a contended spinlock.</html>");
		add("VacuumDelay",                   "<html><b>[ID]</b> - Waiting in a cost-based vacuum delay point.</html>");
		add("VacuumTruncate",                "<html><b>[ID]</b> - Waiting to acquire an exclusive lock to truncate off any empty pages at the end of a table vacuumed.</html>");
	}
	
	

	public static String getWaitEventDescriptionPlain(String name, boolean doPrefix)
	{
		return getInstance().getDescriptionPlain(name, doPrefix);
	}

	public static String getWaitEventDescription(String name)
	{
		return getInstance().getDescriptionHtml(name);
	}

	public static String getWaitEventTypeDescription(String name)
	{
		if      (name.equals("Activity" )) return "<html><b>Activity </b> - The server process is idle. This event type indicates a process waiting for activity in its main processing loop. wait_event will identify the specific wait point</html>";
		else if (name.equals("BufferPin")) return "<html><b>BufferPin</b> - The server process is waiting for exclusive access to a data buffer. Buffer pin waits can be protracted if another process holds an open cursor that last read data from the buffer in question</html>";
		else if (name.equals("Client"   )) return "<html><b>Client   </b> - The server process is waiting for activity on a socket connected to a user application. Thus, the server expects something to happen that is independent of its internal processes. wait_event will identify the specific wait point</html>";
		else if (name.equals("Extension")) return "<html><b>Extension</b> - The server process is waiting for some condition defined by an extension module.</html>";
		else if (name.equals("IO"       )) return "<html><b>IO       </b> - The server process is waiting for an I/O operation to complete. wait_event will identify the specific wait point</html>";
		else if (name.equals("IPC"      )) return "<html><b>IPC      </b> - The server process is waiting for some interaction with another server process. wait_event will identify the specific wait point</html>";
		else if (name.equals("Lock"     )) return "<html><b>Lock     </b> - The server process is waiting for a heavyweight lock. Heavyweight locks, also known as lock manager locks or simply locks, primarily protect SQL-visible objects such as tables. However, they are also used to ensure mutual exclusion for certain internal operations such as relation extension. wait_event will identify the type of lock awaited</html>";
		else if (name.equals("LWLock"   )) return "<html><b>LWLock   </b> - The server process is waiting for a lightweight lock. Most such locks protect a particular data structure in shared memory. wait_event will contain a name identifying the purpose of the lightweight lock. (Some locks have specific names; others are part of a group of locks each with a similar purpose.)</html>";
		else if (name.equals("Timeout"  )) return "<html><b>Timeout  </b> - The server process is waiting for a timeout to expire. wait_event will identify the specific wait point</html>";

		return null;
	}

}
