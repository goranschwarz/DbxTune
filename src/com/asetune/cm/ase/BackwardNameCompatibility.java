package com.asetune.cm.ase;

import com.asetune.cm.os.CmOsIostat;
import com.asetune.cm.os.CmOsMpstat;
import com.asetune.cm.os.CmOsUptime;
import com.asetune.cm.os.CmOsVmstat;

/**
 * This one is used to get old Performance Counter Names into the new naming convention.
 * <p>
 * The old one: <code>CMsomeName</code><br> 
 * The new one: <code>CmNameOfSomething</code><br>
 * <p>
 * This class just holds a translation map: name->newName<br>
 * It's main use is when reading old database recordings.
 *  
 * @author gorans
 *
 */
public class BackwardNameCompatibility
{
	//////////////////////////////////////////////
	// OLD - CM Names
	//////////////////////////////////////////////
	private static final String CM_NAME__SUMMARY                 = "CMsummary";//SummaryPanel.CM_NAME;
	private static final String CM_NAME__OBJECT_ACTIVITY         = "CMobjectActivity";
	private static final String CM_NAME__PROCESS_ACTIVITY        = "CMprocessActivity";
	private static final String CM_NAME__PROCESS_WAIT            = "CMspidWait";
	private static final String CM_NAME__OPEN_DATABASES          = "CMopenDatabases";
	private static final String CM_NAME__TEMPDB_ACTIVITY         = "CMtempdbActivity";
	private static final String CM_NAME__SYS_WAIT                = "CMsysWait";
	private static final String CM_NAME__ENGINE                  = "CMengine";
	private static final String CM_NAME__SYS_LOAD                = "CMsysLoad";
	private static final String CM_NAME__DATA_CACHE              = "CMdataCache";
	private static final String CM_NAME__CACHE_POOL              = "CMcachePool";
	private static final String CM_NAME__DEVICE_IO               = "CMdeviceIo";
	private static final String CM_NAME__IO_QUEUE_SUM            = "CMioQueueSum";
	private static final String CM_NAME__IO_QUEUE                = "CMioQueue";
	private static final String CM_NAME__SPINLOCK_SUM            = "CMspinlockSum";
	private static final String CM_NAME__SYSMON                  = "CMsysmon";
	private static final String CM_NAME__REP_AGENT               = "CMrepAgent";
	private static final String CM_NAME__CACHED_PROC             = "CMcachedProcs";
	private static final String CM_NAME__PROC_CACHE_LOAD         = "CMprocCacheLoad";
	private static final String CM_NAME__PROC_CALL_STACK         = "CMprocCallStack";
	private static final String CM_NAME__CACHED_OBJECTS          = "CMcachedObjects";
	private static final String CM_NAME__ERRORLOG                = "CMerrolog";
	private static final String CM_NAME__DEADLOCK                = "CMdeadlock";
	private static final String CM_NAME__LOCK_TIMEOUT            = "CMlockTimeout";
	private static final String CM_NAME__PROC_CACHE_MODULE_USAGE = "CMpCacheModuleUsage";
	private static final String CM_NAME__PROC_CACHE_MEMORY_USAGE = "CMpCacheMemoryUsage";
	private static final String CM_NAME__STATEMENT_CACHE         = "CMstatementCache";
	private static final String CM_NAME__STATEMENT_CACHE_DETAILS = "CMstmntCacheDetails";
	private static final String CM_NAME__ACTIVE_OBJECTS          = "CMactiveObjects";
	private static final String CM_NAME__ACTIVE_STATEMENTS       = "CMactiveStatements";
	private static final String CM_NAME__BLOCKING                = "CMblocking";
	private static final String CM_NAME__MISSING_STATISTICS      = "CMmissingStats";
	private static final String CM_NAME__QP_METRICS              = "CMqpMetrics";
	private static final String CM_NAME__SP_MONITOR_CONFIG       = "CMspMonitorConfig";
	private static final String CM_NAME__OS_IOSTAT               = "CMosIostat";
	private static final String CM_NAME__OS_VMSTAT               = "CMosVmstat";
	private static final String CM_NAME__OS_MPSTAT               = "CMosMpstat";
	private static final String CM_NAME__OS_UPTIME               = "CMosUptime";

	
	//////////////////////////////////////////////
	// OLD - GRAPHS Names
	//////////////////////////////////////////////
	private static final String CM_GRAPH_NAME__SUMMARY__AA_CPU                    = "aaCpuGraph";
	private static final String CM_GRAPH_NAME__SUMMARY__TRANSACTION               = "TransGraph";
	private static final String CM_GRAPH_NAME__SUMMARY__CONNECTION                = "ConnectionsGraph";
	private static final String CM_GRAPH_NAME__SUMMARY__AA_DISK_READ_WRITE        = "aaReadWriteGraph";
	private static final String CM_GRAPH_NAME__SUMMARY__AA_NW_PACKET              = "aaPacketGraph";

	private static final String CM_GRAPH_NAME__PROCESS_ACTIVITY__CHKPT_HK         = "ChkptHkGraph";

	private static final String CM_GRAPH_NAME__OPEN_DATABASES__LOGSEMAPHORE_CONT  = "DbLogSemapContGraph";
	private static final String CM_GRAPH_NAME__OPEN_DATABASES__LOGSIZE_LEFT       = "DbLogSizeLeftGraph";
	private static final String CM_GRAPH_NAME__OPEN_DATABASES__LOGSIZE_USED_PCT   = "DbLogSizeUsedPctGraph";

	private static final String CM_GRAPH_NAME__TEMPDB_ACTIVITY__LOGSEMAPHORE_CONT = "TempDbLogSemapContGraph";
	
	private static final String CM_GRAPH_NAME__ENGINE__CPU_SUM                    = "cpuSum";
	private static final String CM_GRAPH_NAME__ENGINE__CPU_ENG                    = "cpuEng";
	
	private static final String CM_GRAPH_NAME__SYS_LOAD__AVG_RUN_QUEUE_LENTH      = "AvgRunQLengthGraph";
	private static final String CM_GRAPH_NAME__SYS_LOAD__ENGINE_RUN_QUEUE_LENTH   = "EngineRunQLengthGraph";

	private static final String CM_GRAPH_NAME__DATA_CACHE__ACTIVITY               = "CacheGraph";
	
	private static final String CM_GRAPH_NAME__IO_QUEUE_SUM__DISK_IO_OPS          = "diskIo";

	private static final String CM_GRAPH_NAME__IO_QUEUE__DEVICE_SERVICE_TIME      = "devSvcTime";

	private static final String CM_GRAPH_NAME__PROC_CACHE_LOAD__REQUEST_PER_SEC   = "ProcCacheGraph";

	private static final String CM_GRAPH_NAME__PROC_CACHE_MODULE_USAGE__ABS_USAGE = "ProcCacheModuleUsageGraph";
	
	private static final String CM_GRAPH_NAME__STATEMENT_CACHE__REQUEST_PER_SEC   = "StatementCacheGraph";

	/**
	 * Translate old name into new ones.
	 * 
	 * @param name old CM Name
	 * @param notFoundReturn name to return if not found in mapping
	 * 
	 * @return new name, or <code>notFoundReturn</code> if not in the old naming
	 */
	public static String getOldToNew(String name, String notFoundReturn)
	{
		if (name.equals(CM_NAME__SUMMARY                )) return CmSummary          .CM_NAME;
		if (name.equals(CM_NAME__OBJECT_ACTIVITY        )) return CmObjectActivity   .CM_NAME;
		if (name.equals(CM_NAME__PROCESS_ACTIVITY       )) return CmProcessActivity  .CM_NAME;
		if (name.equals(CM_NAME__PROCESS_WAIT           )) return CmSpidWait         .CM_NAME;
		if (name.equals(CM_NAME__OPEN_DATABASES         )) return CmOpenDatabases    .CM_NAME;
		if (name.equals(CM_NAME__TEMPDB_ACTIVITY        )) return CmTempdbActivity   .CM_NAME;
		if (name.equals(CM_NAME__SYS_WAIT               )) return CmSysWaits         .CM_NAME;
		if (name.equals(CM_NAME__ENGINE                 )) return CmEngines          .CM_NAME;
		if (name.equals(CM_NAME__SYS_LOAD               )) return CmSysLoad          .CM_NAME;
		if (name.equals(CM_NAME__DATA_CACHE             )) return CmDataCaches       .CM_NAME;
		if (name.equals(CM_NAME__CACHE_POOL             )) return CmCachePools       .CM_NAME;
		if (name.equals(CM_NAME__DEVICE_IO              )) return CmDeviceIo         .CM_NAME;
		if (name.equals(CM_NAME__IO_QUEUE_SUM           )) return CmIoQueueSum       .CM_NAME;
		if (name.equals(CM_NAME__IO_QUEUE               )) return CmIoQueue          .CM_NAME;
		if (name.equals(CM_NAME__SPINLOCK_SUM           )) return CmSpinlockSum      .CM_NAME;
		if (name.equals(CM_NAME__SYSMON                 )) return CmSysmon           .CM_NAME;
		if (name.equals(CM_NAME__REP_AGENT              )) return CmRaSysmon         .CM_NAME;
		if (name.equals(CM_NAME__CACHED_PROC            )) return CmCachedProcs      .CM_NAME;
		if (name.equals(CM_NAME__PROC_CACHE_LOAD        )) return CmProcCacheLoad    .CM_NAME;
		if (name.equals(CM_NAME__PROC_CALL_STACK        )) return CmProcCallStack    .CM_NAME;
		if (name.equals(CM_NAME__CACHED_OBJECTS         )) return CmCachedObjects    .CM_NAME;
		if (name.equals(CM_NAME__ERRORLOG               )) return CmErrorLog         .CM_NAME;
		if (name.equals(CM_NAME__DEADLOCK               )) return CmDeadlock         .CM_NAME;
		if (name.equals(CM_NAME__LOCK_TIMEOUT           )) return CmLockTimeout      .CM_NAME;
		if (name.equals(CM_NAME__PROC_CACHE_MODULE_USAGE)) return CmPCacheModuleUsage.CM_NAME;
		if (name.equals(CM_NAME__PROC_CACHE_MEMORY_USAGE)) return CmPCacheMemoryUsage.CM_NAME;
		if (name.equals(CM_NAME__STATEMENT_CACHE        )) return CmStatementCache   .CM_NAME;
		if (name.equals(CM_NAME__STATEMENT_CACHE_DETAILS)) return CmStmntCacheDetails.CM_NAME;
		if (name.equals(CM_NAME__ACTIVE_OBJECTS         )) return CmActiveObjects    .CM_NAME;
		if (name.equals(CM_NAME__ACTIVE_STATEMENTS      )) return CmActiveStatements .CM_NAME;
		if (name.equals(CM_NAME__BLOCKING               )) return CmBlocking         .CM_NAME;
		if (name.equals(CM_NAME__MISSING_STATISTICS     )) return CmMissingStats     .CM_NAME;
		if (name.equals(CM_NAME__QP_METRICS             )) return CmQpMetrics        .CM_NAME;
		if (name.equals(CM_NAME__SP_MONITOR_CONFIG      )) return CmSpMonitorConfig  .CM_NAME;
		if (name.equals(CM_NAME__OS_IOSTAT              )) return CmOsIostat         .CM_NAME;
		if (name.equals(CM_NAME__OS_VMSTAT              )) return CmOsVmstat         .CM_NAME;
		if (name.equals(CM_NAME__OS_MPSTAT              )) return CmOsMpstat         .CM_NAME;
		if (name.equals(CM_NAME__OS_UPTIME              )) return CmOsUptime         .CM_NAME;

		return notFoundReturn;
	}

	/**
	 * Translate new name into old ones.
	 * 
	 * @param name old CM Name
	 * @param notFoundReturn name to return if not found in mapping
	 * 
	 * @return new name, or <code>notFoundReturn</code> if not in the new naming
	 */
	public static String getNewToOld(String name, String notFoundReturn)
	{
		if (name.equals(CmSummary          .CM_NAME)) return CM_NAME__SUMMARY;
		if (name.equals(CmObjectActivity   .CM_NAME)) return CM_NAME__OBJECT_ACTIVITY;
		if (name.equals(CmProcessActivity  .CM_NAME)) return CM_NAME__PROCESS_ACTIVITY;
		if (name.equals(CmSpidWait         .CM_NAME)) return CM_NAME__PROCESS_WAIT;
		if (name.equals(CmOpenDatabases    .CM_NAME)) return CM_NAME__OPEN_DATABASES;
		if (name.equals(CmTempdbActivity   .CM_NAME)) return CM_NAME__TEMPDB_ACTIVITY;
		if (name.equals(CmSysWaits         .CM_NAME)) return CM_NAME__SYS_WAIT;
		if (name.equals(CmEngines          .CM_NAME)) return CM_NAME__ENGINE;
		if (name.equals(CmSysLoad          .CM_NAME)) return CM_NAME__SYS_LOAD;
		if (name.equals(CmDataCaches       .CM_NAME)) return CM_NAME__DATA_CACHE;
		if (name.equals(CmCachePools       .CM_NAME)) return CM_NAME__CACHE_POOL;
		if (name.equals(CmDeviceIo         .CM_NAME)) return CM_NAME__DEVICE_IO;
		if (name.equals(CmIoQueueSum       .CM_NAME)) return CM_NAME__IO_QUEUE_SUM;
		if (name.equals(CmIoQueue          .CM_NAME)) return CM_NAME__IO_QUEUE;
		if (name.equals(CmSpinlockSum      .CM_NAME)) return CM_NAME__SPINLOCK_SUM;
		if (name.equals(CmSysmon           .CM_NAME)) return CM_NAME__SYSMON;
		if (name.equals(CmRaSysmon         .CM_NAME)) return CM_NAME__REP_AGENT;
		if (name.equals(CmCachedProcs      .CM_NAME)) return CM_NAME__CACHED_PROC;
		if (name.equals(CmProcCacheLoad    .CM_NAME)) return CM_NAME__PROC_CACHE_LOAD;
		if (name.equals(CmProcCallStack    .CM_NAME)) return CM_NAME__PROC_CALL_STACK;
		if (name.equals(CmCachedObjects    .CM_NAME)) return CM_NAME__CACHED_OBJECTS;
		if (name.equals(CmErrorLog         .CM_NAME)) return CM_NAME__ERRORLOG;
		if (name.equals(CmDeadlock         .CM_NAME)) return CM_NAME__DEADLOCK;
		if (name.equals(CmLockTimeout      .CM_NAME)) return CM_NAME__LOCK_TIMEOUT;
		if (name.equals(CmPCacheModuleUsage.CM_NAME)) return CM_NAME__PROC_CACHE_MODULE_USAGE;
		if (name.equals(CmPCacheMemoryUsage.CM_NAME)) return CM_NAME__PROC_CACHE_MEMORY_USAGE;
		if (name.equals(CmStatementCache   .CM_NAME)) return CM_NAME__STATEMENT_CACHE;
		if (name.equals(CmStmntCacheDetails.CM_NAME)) return CM_NAME__STATEMENT_CACHE_DETAILS;
		if (name.equals(CmActiveObjects    .CM_NAME)) return CM_NAME__ACTIVE_OBJECTS;
		if (name.equals(CmActiveStatements .CM_NAME)) return CM_NAME__ACTIVE_STATEMENTS;
		if (name.equals(CmBlocking         .CM_NAME)) return CM_NAME__BLOCKING;
		if (name.equals(CmMissingStats     .CM_NAME)) return CM_NAME__MISSING_STATISTICS;
		if (name.equals(CmQpMetrics        .CM_NAME)) return CM_NAME__QP_METRICS;
		if (name.equals(CmSpMonitorConfig  .CM_NAME)) return CM_NAME__SP_MONITOR_CONFIG;
		if (name.equals(CmOsIostat         .CM_NAME)) return CM_NAME__OS_IOSTAT;
		if (name.equals(CmOsVmstat         .CM_NAME)) return CM_NAME__OS_VMSTAT;
		if (name.equals(CmOsMpstat         .CM_NAME)) return CM_NAME__OS_MPSTAT;
		if (name.equals(CmOsUptime         .CM_NAME)) return CM_NAME__OS_UPTIME;

		return notFoundReturn;
	}

	/**
	 * Translate old CM Graph name into new ones.
	 * 
	 * @param name old CM Graph Name
	 * @param notFoundReturn name to return if not found in mapping
	 * 
	 * @return new name, or <code>notFoundReturn</code> if not in the old naming
	 */
	public static String getOldToNewGraph(String name, String notFoundReturn)
	{
		if (name.equals(CM_GRAPH_NAME__SUMMARY__AA_CPU                   )) return CmSummary          .GRAPH_NAME_AA_CPU;
		if (name.equals(CM_GRAPH_NAME__SUMMARY__TRANSACTION              )) return CmSummary          .GRAPH_NAME_TRANSACTION;
		if (name.equals(CM_GRAPH_NAME__SUMMARY__CONNECTION               )) return CmSummary          .GRAPH_NAME_CONNECTION;
		if (name.equals(CM_GRAPH_NAME__SUMMARY__AA_DISK_READ_WRITE       )) return CmSummary          .GRAPH_NAME_AA_DISK_READ_WRITE;
		if (name.equals(CM_GRAPH_NAME__SUMMARY__AA_NW_PACKET             )) return CmSummary          .GRAPH_NAME_AA_NW_PACKET;

		if (name.equals(CM_GRAPH_NAME__PROCESS_ACTIVITY__CHKPT_HK        )) return CmProcessActivity  .GRAPH_NAME_CHKPT_HK;

		if (name.equals(CM_GRAPH_NAME__OPEN_DATABASES__LOGSEMAPHORE_CONT )) return CmOpenDatabases    .GRAPH_NAME_LOGSEMAPHORE_CONT;
		if (name.equals(CM_GRAPH_NAME__OPEN_DATABASES__LOGSIZE_LEFT      )) return CmOpenDatabases    .GRAPH_NAME_LOGSIZE_LEFT_MB;
		if (name.equals(CM_GRAPH_NAME__OPEN_DATABASES__LOGSIZE_USED_PCT  )) return CmOpenDatabases    .GRAPH_NAME_LOGSIZE_USED_PCT;

		if (name.equals(CM_GRAPH_NAME__TEMPDB_ACTIVITY__LOGSEMAPHORE_CONT)) return CmTempdbActivity   .GRAPH_NAME_LOGSEMAPHORE_CONT;

		if (name.equals(CM_GRAPH_NAME__ENGINE__CPU_SUM                   )) return CmEngines          .GRAPH_NAME_CPU_SUM;
		if (name.equals(CM_GRAPH_NAME__ENGINE__CPU_ENG                   )) return CmEngines          .GRAPH_NAME_CPU_ENG;

		if (name.equals(CM_GRAPH_NAME__SYS_LOAD__AVG_RUN_QUEUE_LENTH     )) return CmSysLoad          .GRAPH_NAME_AVG_RUN_QUEUE_LENTH;
		if (name.equals(CM_GRAPH_NAME__SYS_LOAD__ENGINE_RUN_QUEUE_LENTH  )) return CmSysLoad          .GRAPH_NAME_ENGINE_RUN_QUEUE_LENTH;

		if (name.equals(CM_GRAPH_NAME__DATA_CACHE__ACTIVITY              )) return CmDataCaches       .GRAPH_NAME_CACHE_ACTIVITY;

		if (name.equals(CM_GRAPH_NAME__IO_QUEUE_SUM__DISK_IO_OPS         )) return CmIoQueueSum       .GRAPH_NAME_DISK_IO_OPS;

		if (name.equals(CM_GRAPH_NAME__IO_QUEUE__DEVICE_SERVICE_TIME     )) return CmIoQueue          .GRAPH_NAME_DEVICE_SERVICE_TIME;

		if (name.equals(CM_GRAPH_NAME__PROC_CACHE_LOAD__REQUEST_PER_SEC  )) return CmProcCacheLoad    .GRAPH_NAME_REQUEST_PER_SEC;

		if (name.equals(CM_GRAPH_NAME__PROC_CACHE_MODULE_USAGE__ABS_USAGE)) return CmPCacheModuleUsage.GRAPH_NAME_MODULE_USAGE;

		if (name.equals(CM_GRAPH_NAME__STATEMENT_CACHE__REQUEST_PER_SEC  )) return CmStatementCache   .GRAPH_NAME_REQUEST_PER_SEC;

		return notFoundReturn;
	}

	/**
	 * Translate new CM Graph name into old ones.
	 * 
	 * @param name new CM Graph Name
	 * @param notFoundReturn name to return if not found in mapping
	 * 
	 * @return old name, or <code>notFoundReturn</code> if not in the new naming
	 */
	public static String getNewToOldGraph(String name, String notFoundReturn)
	{
		if (name.equals(CmSummary          .GRAPH_NAME_AA_CPU                )) return CM_GRAPH_NAME__SUMMARY__AA_CPU                   ;
		if (name.equals(CmSummary          .GRAPH_NAME_TRANSACTION           )) return CM_GRAPH_NAME__SUMMARY__TRANSACTION              ;
		if (name.equals(CmSummary          .GRAPH_NAME_CONNECTION            )) return CM_GRAPH_NAME__SUMMARY__CONNECTION               ;
		if (name.equals(CmSummary          .GRAPH_NAME_AA_DISK_READ_WRITE    )) return CM_GRAPH_NAME__SUMMARY__AA_DISK_READ_WRITE       ;
		if (name.equals(CmSummary          .GRAPH_NAME_AA_NW_PACKET          )) return CM_GRAPH_NAME__SUMMARY__AA_NW_PACKET             ;

		if (name.equals(CmProcessActivity  .GRAPH_NAME_CHKPT_HK              )) return CM_GRAPH_NAME__PROCESS_ACTIVITY__CHKPT_HK        ;

		if (name.equals(CmOpenDatabases    .GRAPH_NAME_LOGSEMAPHORE_CONT     )) return CM_GRAPH_NAME__OPEN_DATABASES__LOGSEMAPHORE_CONT ;
		if (name.equals(CmOpenDatabases    .GRAPH_NAME_LOGSIZE_LEFT_MB       )) return CM_GRAPH_NAME__OPEN_DATABASES__LOGSIZE_LEFT      ;
		if (name.equals(CmOpenDatabases    .GRAPH_NAME_LOGSIZE_USED_PCT      )) return CM_GRAPH_NAME__OPEN_DATABASES__LOGSIZE_USED_PCT  ;

		if (name.equals(CmTempdbActivity   .GRAPH_NAME_LOGSEMAPHORE_CONT     )) return CM_GRAPH_NAME__TEMPDB_ACTIVITY__LOGSEMAPHORE_CONT;

		if (name.equals(CmEngines          .GRAPH_NAME_CPU_SUM               )) return CM_GRAPH_NAME__ENGINE__CPU_SUM                   ;
		if (name.equals(CmEngines          .GRAPH_NAME_CPU_ENG               )) return CM_GRAPH_NAME__ENGINE__CPU_ENG                   ;

		if (name.equals(CmSysLoad          .GRAPH_NAME_AVG_RUN_QUEUE_LENTH   )) return CM_GRAPH_NAME__SYS_LOAD__AVG_RUN_QUEUE_LENTH     ;
		if (name.equals(CmSysLoad          .GRAPH_NAME_ENGINE_RUN_QUEUE_LENTH)) return CM_GRAPH_NAME__SYS_LOAD__ENGINE_RUN_QUEUE_LENTH  ;

		if (name.equals(CmDataCaches       .GRAPH_NAME_CACHE_ACTIVITY        )) return CM_GRAPH_NAME__DATA_CACHE__ACTIVITY              ;

		if (name.equals(CmIoQueueSum       .GRAPH_NAME_DISK_IO_OPS           )) return CM_GRAPH_NAME__IO_QUEUE_SUM__DISK_IO_OPS         ;

		if (name.equals(CmIoQueue          .GRAPH_NAME_DEVICE_SERVICE_TIME   )) return CM_GRAPH_NAME__IO_QUEUE__DEVICE_SERVICE_TIME     ;

		if (name.equals(CmProcCacheLoad    .GRAPH_NAME_REQUEST_PER_SEC       )) return CM_GRAPH_NAME__PROC_CACHE_LOAD__REQUEST_PER_SEC  ;

		if (name.equals(CmPCacheModuleUsage.GRAPH_NAME_MODULE_USAGE          )) return CM_GRAPH_NAME__PROC_CACHE_MODULE_USAGE__ABS_USAGE;

		if (name.equals(CmStatementCache   .GRAPH_NAME_REQUEST_PER_SEC       )) return CM_GRAPH_NAME__STATEMENT_CACHE__REQUEST_PER_SEC  ;

		return notFoundReturn;
	}
}
