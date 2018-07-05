package com.asetune.config.dict;

import java.util.HashMap;

import com.asetune.utils.StringUtil;

public class SqlServerLatchClassDictionary
{
	/** Instance variable */
	private static SqlServerLatchClassDictionary _instance = null;

	private HashMap<String, TypeRecord> _types = new HashMap<String, TypeRecord>();

	public class TypeRecord
	{
		private String _id              = null;
		private String _description     = null;

		public TypeRecord(String id, String description)
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


	public SqlServerLatchClassDictionary()
	{
		init();
	}

	public static SqlServerLatchClassDictionary getInstance()
	{
		if (_instance == null)
			_instance = new SqlServerLatchClassDictionary();
		return _instance;
	}

	/**
	 * Strips out all HTML and return it as a "plain" text
	 * @param waitName
	 * @return
	 */
	public String getDescriptionPlain(String waitName)
	{
		TypeRecord rec = _types.get(waitName);
		if (rec != null)
			return StringUtil.stripHtml(rec._description);

		// Compose an empty one
		return "";
//		return "WaitName '"+waitName+"' not found in dictionary.";
	}


	public String getDescriptionHtml(String waitName)
	{
		TypeRecord rec = _types.get(waitName);
		if (rec != null)
			return rec._description;

		// Compose an empty one
		return "<html><code>"+waitName+"</code> not found in dictionary.</html>";
	}


	private void set(TypeRecord rec)
	{
		if ( _types.containsKey(rec._id))
			System.out.println("ID '"+rec._id+"' already exists. It will be overwritten.");

		_types.put(rec._id, rec);
	}

	private void add(String id, String description)
	{
		set(new TypeRecord(id, description));
	}

	private void init()
	{
		add("ALLOC_CREATE_RINGBUF                        ".trim(), "<html>Used internally by SQL Server to initialize the synchronization of the creation of an allocation ring buffer.</html>");
		add("ALLOC_CREATE_FREESPACE_CACHE                ".trim(), "<html>Used to initialize the synchronization of internal freespace caches for heaps.</html>");
		add("ALLOC_CACHE_MANAGER                         ".trim(), "<html>Used to synchronize internal coherency tests.</html>");
		add("ALLOC_FREESPACE_CACHE                       ".trim(), "<html>Used to synchronize the access to a cache of pages with available space for heaps and binary large objects (BLOBs). Contention on latches of this class can occur when multiple connections try to insert rows into a heap or BLOB at the same time. You can reduce this contention by partitioning the object. Each partition has its own latch. Partitioning will distribute the inserts across multiple latches.</html>");
		add("ALLOC_EXTENT_CACHE                          ".trim(), "<html>Used to synchronize the access to a cache of extents that contains pages that are not allocated. Contention on latches of this class can occur when multiple connections try to allocate data pages in the same allocation unit at the same time. This contention can be reduced by partitioning the object of which this allocation unit is a part.</html>");
		add("ACCESS_METHODS_DATASET_PARENT               ".trim(), "<html>Used to synchronize child dataset access to the parent dataset during parallel operations.</html>");
		add("ACCESS_METHODS_HOBT_FACTORY                 ".trim(), "<html>Used to synchronize access to an internal hash table.</html>");
		add("ACCESS_METHODS_HOBT                         ".trim(), "<html>Used to synchronize access to the in-memory representation of a HoBt.</html>");
		add("ACCESS_METHODS_HOBT_COUNT                   ".trim(), "<html>Used to synchronize access to a HoBt page and row counters.</html>");
		add("ACCESS_METHODS_HOBT_VIRTUAL_ROOT            ".trim(), "<html>Used to synchronize access to the root page abstraction of an internal B-tree.</html>");
		add("ACCESS_METHODS_CACHE_ONLY_HOBT_ALLOC        ".trim(), "<html>Used to synchronize worktable access.</html>");
		add("ACCESS_METHODS_BULK_ALLOC                   ".trim(), "<html>Used to synchronize access within bulk allocators.</html>");
		add("ACCESS_METHODS_SCAN_RANGE_GENERATOR         ".trim(), "<html>Used to synchronize access to a range generator during parallel scans.</html>");
		add("ACCESS_METHODS_KEY_RANGE_GENERATOR          ".trim(), "<html>Used to synchronize access to read-ahead operations during key range parallel scans.</html>");
		add("APPEND_ONLY_STORAGE_INSERT_POINT            ".trim(), "<html>Used to synchronize inserts in fast append-only storage units.</html>");
		add("APPEND_ONLY_STORAGE_FIRST_ALLOC             ".trim(), "<html>Used to synchronize the first allocation for an append-only storage unit.</html>");
		add("APPEND_ONLY_STORAGE_UNIT_MANAGER            ".trim(), "<html>Used for internal data structure access synchronization within the fast append-only storage unit manager.</html>");
		add("APPEND_ONLY_STORAGE_MANAGER                 ".trim(), "<html>Used to synchronize shrink operations in the fast append-only storage unit manager.</html>");
		add("BACKUP_RESULT_SET                           ".trim(), "<html>Used to synchronize parallel backup result sets.</html>");
		add("BACKUP_TAPE_POOL                            ".trim(), "<html>Used to synchronize backup tape pools.</html>");
		add("BACKUP_LOG_REDO                             ".trim(), "<html>Used to synchronize backup log redo operations.</html>");
		add("BACKUP_INSTANCE_ID                          ".trim(), "<html>Used to synchronize the generation of instance IDs for backup performance monitor counters.</html>");
		add("BACKUP_MANAGER                              ".trim(), "<html>Used to synchronize the internal backup manager.</html>");
		add("BACKUP_MANAGER_DIFFERENTIAL                 ".trim(), "<html>Used to synchronize differential backup operations with DBCC.</html>");
		add("BACKUP_OPERATION                            ".trim(), "<html>Used for internal data structure synchronization within a backup operation, such as database, log, or file backup.</html>");
		add("BACKUP_FILE_HANDLE                          ".trim(), "<html>Used to synchronize file open operations during a restore operation.</html>");
		add("BUFFER                                      ".trim(), "<html>Used to synchronize short term access to database pages. A buffer latch is required before reading or modifying any database page. Buffer latch contention can indicate several issues, including hot pages and slow I/Os. This latch class covers all possible uses of page latches. sys.dm_os_wait_stats makes a difference between page latch waits that are caused by I/O operations and read and write operations on the page.</html>");
		add("BUFFER_POOL_GROW                            ".trim(), "<html>Used for internal buffer manager synchronization during buffer pool grow operations.</html>");
		add("DATABASE_CHECKPOINT                         ".trim(), "<html>Used to serialize checkpoints within a database.</html>");
		add("CLR_PROCEDURE_HASHTABLE                     ".trim(), "<html>Internal use only.</html>");
		add("CLR_UDX_STORE                               ".trim(), "<html>Internal use only.</html>");
		add("CLR_DATAT_ACCESS                            ".trim(), "<html>Internal use only.</html>");
		add("CLR_XVAR_PROXY_LIST                         ".trim(), "<html>Internal use only.</html>");
		add("DBCC_CHECK_AGGREGATE                        ".trim(), "<html>Internal use only.</html>");
		add("DBCC_CHECK_RESULTSET                        ".trim(), "<html>Internal use only.</html>");
		add("DBCC_CHECK_TABLE                            ".trim(), "<html>Internal use only.</html>");
		add("DBCC_CHECK_TABLE_INIT                       ".trim(), "<html>Internal use only.</html>");
		add("DBCC_CHECK_TRACE_LIST                       ".trim(), "<html>Internal use only.</html>");
		add("DBCC_FILE_CHECK_OBJECT                      ".trim(), "<html>Internal use only.</html>");
		add("DBCC_PERF                                   ".trim(), "<html>Used to synchronize internal performance monitor counters.</html>");
		add("DBCC_PFS_STATUS                             ".trim(), "<html>Internal use only.</html>");
		add("DBCC_OBJECT_METADATA                        ".trim(), "<html>Internal use only.</html>");
		add("DBCC_HASH_DLL                               ".trim(), "<html>Internal use only.</html>");
		add("EVENTING_CACHE                              ".trim(), "<html>Internal use only.</html>");
		add("FCB                                         ".trim(), "<html>Used to synchronize access to the file control block.</html>");
		add("FCB_REPLICA                                 ".trim(), "<html>Internal use only.</html>");
		add("FGCB_ALLOC                                  ".trim(), "<html>Use to synchronize access to round robin allocation information within a filegroup.</html>");
		add("FGCB_ADD_REMOVE                             ".trim(), "<html>Use to synchronize access to filegroups for ADD and DROP file operations.</html>");
		add("FILEGROUP_MANAGER                           ".trim(), "<html>Internal use only.</html>");
		add("FILE_MANAGER                                ".trim(), "<html>Internal use only.</html>");
		add("FILESTREAM_FCB                              ".trim(), "<html>Internal use only.</html>");
		add("FILESTREAM_FILE_MANAGER                     ".trim(), "<html>Internal use only.</html>");
		add("FILESTREAM_GHOST_FILES                      ".trim(), "<html>Internal use only.</html>");
		add("FILESTREAM_DFS_ROOT                         ".trim(), "<html>Internal use only.</html>");
		add("LOG_MANAGER                                 ".trim(), "<html>Internal use only.</html>");
		add("FULLTEXT_DOCUMENT_ID                        ".trim(), "<html>Internal use only.</html>");
		add("FULLTEXT_DOCUMENT_ID_TRANSACTION            ".trim(), "<html>Internal use only.</html>");
		add("FULLTEXT_DOCUMENT_ID_NOTIFY                 ".trim(), "<html>Internal use only.</html>");
		add("FULLTEXT_LOGS                               ".trim(), "<html>Internal use only.</html>");
		add("FULLTEXT_CRAWL_LOG                          ".trim(), "<html>Internal use only.</html>");
		add("FULLTEXT_ADMIN                              ".trim(), "<html>Internal use only.</html>");
		add("FULLTEXT_AMDIN_COMMAND_CACHE                ".trim(), "<html>Internal use only.</html>");
		add("FULLTEXT_LANGUAGE_TABLE                     ".trim(), "<html>Internal use only.</html>");
		add("FULLTEXT_CRAWL_DM_LIST                      ".trim(), "<html>Internal use only.</html>");
		add("FULLTEXT_CRAWL_CATALOG                      ".trim(), "<html>Internal use only.</html>");
		add("FULLTEXT_FILE_MANAGER                       ".trim(), "<html>Internal use only.</html>");
		add("DATABASE_MIRRORING_REDO                     ".trim(), "<html>Internal use only.</html>");
		add("DATABASE_MIRRORING_SERVER                   ".trim(), "<html>Internal use only.</html>");
		add("DATABASE_MIRRORING_CONNECTION               ".trim(), "<html>Internal use only.</html>");
		add("DATABASE_MIRRORING_STREAM                   ".trim(), "<html>Internal use only.</html>");
		add("QUERY_OPTIMIZER_VD_MANAGER                  ".trim(), "<html>Internal use only.</html>");
		add("QUERY_OPTIMIZER_ID_MANAGER                  ".trim(), "<html>Internal use only.</html>");
		add("QUERY_OPTIMIZER_VIEW_REP                    ".trim(), "<html>Internal use only.</html>");
		add("RECOVERY_BAD_PAGE_TABLE                     ".trim(), "<html>Internal use only.</html>");
		add("RECOVERY_MANAGER                            ".trim(), "<html>Internal use only.</html>");
		add("SECURITY_OPERATION_RULE_TABLE               ".trim(), "<html>Internal use only.</html>");
		add("SECURITY_OBJPERM_CACHE                      ".trim(), "<html>Internal use only.</html>");
		add("SECURITY_CRYPTO                             ".trim(), "<html>Internal use only.</html>");
		add("SECURITY_KEY_RING                           ".trim(), "<html>Internal use only.</html>");
		add("SECURITY_KEY_LIST                           ".trim(), "<html>Internal use only.</html>");
		add("SERVICE_BROKER_CONNECTION_RECEIVE           ".trim(), "<html>Internal use only.</html>");
		add("SERVICE_BROKER_TRANSMISSION                 ".trim(), "<html>Internal use only.</html>");
		add("SERVICE_BROKER_TRANSMISSION_UPDATE          ".trim(), "<html>Internal use only.</html>");
		add("SERVICE_BROKER_TRANSMISSION_STATE           ".trim(), "<html>Internal use only.</html>");
		add("SERVICE_BROKER_TRANSMISSION_ERRORS          ".trim(), "<html>Internal use only.</html>");
		add("SSBXmitWork                                 ".trim(), "<html>Internal use only.</html>");
		add("SERVICE_BROKER_MESSAGE_TRANSMISSION         ".trim(), "<html>Internal use only.</html>");
		add("SERVICE_BROKER_MAP_MANAGER                  ".trim(), "<html>Internal use only.</html>");
		add("SERVICE_BROKER_HOST_NAME                    ".trim(), "<html>Internal use only.</html>");
		add("SERVICE_BROKER_READ_CACHE                   ".trim(), "<html>Internal use only.</html>");
		add("SERVICE_BROKER_WAITFOR_MANAGER              ".trim(), "<html>Used to synchronize an instance level map of waiter queues. One queue exists per database ID, Database Version, and Queue ID tuple. Contention on latches of this class can occur when many connections are");
		add("SERVICE_BROKER_WAITFOR_TRANSACTION_DATA     ".trim(), "<html>Internal use only.</html>");
		add("SERVICE_BROKER_TRANSMISSION_TRANSACTION_DATA".trim(), "<html>Internal use only.</html>");
		add("SERVICE_BROKER_TRANSPORT                    ".trim(), "<html>Internal use only.</html>");
		add("SERVICE_BROKER_MIRROR_ROUTE                 ".trim(), "<html>Internal use only.</html>");
		add("TRACE_ID                                    ".trim(), "<html>Internal use only.</html>");
		add("TRACE_AUDIT_ID                              ".trim(), "<html>Internal use only.</html>");
		add("TRACE                                       ".trim(), "<html>Internal use only.</html>");
		add("TRACE_CONTROLLER                            ".trim(), "<html>Internal use only.</html>");
		add("TRACE_EVENT_QUEUE                           ".trim(), "<html>Internal use only.</html>");
		add("TRANSACTION_DISTRIBUTED_MARK                ".trim(), "<html>Internal use only.</html>");
		add("TRANSACTION_OUTCOME                         ".trim(), "<html>Internal use only.</html>");
		add("NESTING_TRANSACTION_READONLY                ".trim(), "<html>Internal use only.</html>");
		add("NESTING_TRANSACTION_FULL                    ".trim(), "<html>Internal use only.</html>");
		add("MSQL_TRANSACTION_MANAGER                    ".trim(), "<html>Internal use only.</html>");
		add("DATABASE_AUTONAME_MANAGER                   ".trim(), "<html>Internal use only.</html>");
		add("UTILITY_DYNAMIC_VECTOR                      ".trim(), "<html>Internal use only.</html>");
		add("UTILITY_SPARSE_BITMAP                       ".trim(), "<html>Internal use only.</html>");
		add("UTILITY_DATABASE_DROP                       ".trim(), "<html>Internal use only.</html>");
		add("UTILITY_DYNAMIC_MANAGER_VIEW                ".trim(), "<html>Internal use only.</html>");
		add("UTILITY_DEBUG_FILESTREAM                    ".trim(), "<html>Internal use only.</html>");
		add("UTILITY_LOCK_INFORMATION                    ".trim(), "<html>Internal use only.</html>");
		add("VERSIONING_TRANSACTION                      ".trim(), "<html>Internal use only.</html>");
		add("VERSIONING_TRANSACTION_LIST                 ".trim(), "<html>Internal use only.</html>");
		add("VERSIONING_TRANSACTION_CHAIN                ".trim(), "<html>Internal use only.</html>");
		add("VERSIONING_STATE                            ".trim(), "<html>Internal use only.</html>");
		add("VERSIONING_STATE_CHANGE                     ".trim(), "<html>Internal use only.</html>");
		add("KTM_VIRTUAL_CLOCK                           ".trim(), "<html>Internal use only.</html>");
	}

}
