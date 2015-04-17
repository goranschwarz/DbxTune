package com.asetune.cm.rax;

import java.util.HashMap;

/**
 * Counter descriptions grabbed from the manual: 
 * http://infocenter.sybase.com/help/topic/com.sybase.infocenter.dc00268.1571/doc/html/san1272998275552.html
 */
public class RaxCounterDict
{
	private HashMap<String, String> _counterDesc = new HashMap<String, String>();

	private static RaxCounterDict _instance = null;
	
	public static RaxCounterDict getInstance()
	{
		if (_instance == null)
			_instance = new RaxCounterDict();
		return _instance;
	}
	
	/**
	 * Get description for a specific RAX Counter
	 * @param name counter name
	 * @return Description of the counter, or null if it can't be found.
	 */
	public static String getDesc(String name)
	{
		if (_instance == null)
			_instance = new RaxCounterDict();

		return _instance._counterDesc.get(name);
	}

	public RaxCounterDict()
	{
		init();
	}

	private void addCounter(String name, String desc)
	{
		String oldDesc = _counterDesc.get(name);
		if (oldDesc != null && !oldDesc.equalsIgnoreCase(desc))
		{
			System.out.println("WARNING: adding counter '"+name+"', which already has a description.");
			System.out.println("         old desc='"+oldDesc+"'");
			System.out.println("         new desc='"+desc+"'");
		}

		_counterDesc.put(name, desc);
	}

	private void init()
	{
		// Java VM Statistics
		addCounter("VM maximum memory",                                        "Maximum memory (in bytes) available to the Java VM");
		addCounter("VM total memory allocated",                                "Total memory (in bytes) allocated to the Java VM at start-up");
		addCounter("VM free memory",                                           "Memory (in bytes) allocated but not used by the Java VM");
		addCounter("VM memory usage",                                          "Memory (in bytes) allocated and in use by the Java VM");
		addCounter("VM % max memory used",                                     "Percentage of the maximum memory available to the Java VM, currently in use by the Java VM");

		addCounter("VM maximum memory - KB",                                   "Maximum memory (in KB) available to the Java VM");
		addCounter("VM total memory allocated - KB",                           "Total memory (in KB) allocated to the Java VM at start-up");
		addCounter("VM free memory - KB",                                      "Memory (in KB) allocated but not used by the Java VM");
		addCounter("VM memory usage - KB",                                     "Memory (in KB) allocated and in use by the Java VM");

		addCounter("VM maximum memory - MB",                                   "Maximum memory (in MB) available to the Java VM");
		addCounter("VM total memory allocated - MB",                           "Total memory (in MB) allocated to the Java VM at start-up");
		addCounter("VM free memory - MB",                                      "Memory (in MB) allocated but not used by the Java VM");
		addCounter("VM memory usage - MB",                                     "Memory (in MB) allocated and in use by the Java VM");

		// Log Transfer Manager Statistics
		addCounter("Time statistics obtained",                                 "Day, date, and time when ra_statistics was invoked and information returned");
		addCounter("Time replication last started",                            "Day, date, and time that Replicating state was entered");
		addCounter("Time statistics last reset",                               "Day, date, and time that statistics counters were reset");
		addCounter("Items held in Global LRUCache",                            "Number of object references in the internal Least Recently Used cache");

		// Log Reader Statistics for UDB
		addCounter("Number of transaction logs scanned",                                         "Number of operations read from log devices");
		addCounter("Average unprocessed operations per transaction log scan",                    "Average number of unprocessed operations for each transaction log scan");
		addCounter("Average transaction log scan time",                                          "Average transaction log scan time for operations read from log devices");
		addCounter("Number of operations replicated",                                            "Number of operations that were successfully replicated");
		addCounter("Number of transactions replicated",                                          "Number of transactions that were successfully replicated");
		addCounter("Number of transaction log operations skipped (maint_user, unmarked tables)", "Number of transaction log operations that were skipped");
		addCounter("Average wait time on empty transaction log",                                 "Average time that the transaction log was not in use");
		addCounter("Average PDB Service Time/Operations",                                        "Average service and operations time for each database");
		addCounter("Operation Queue Size",                                                       "The queue size used for the operations");
		addCounter("Operation Data Hash Size",                                                   "The data hash size for the operations");
		addCounter("Number of transactions truncated",                                           "Number of transactions that were truncated");

		// Log Reader Statistics for Microsoft SQL Server
		addCounter("Total operations scanned",                                 "Number of operations read from log devices since last reset");
		addCounter("Total operations processed",                               "Number of operations read from log devices and passed to LTI since last reset");
		addCounter("Total operations skipped",                                 "Number of operations read from log devices and not processed for any reason since last reset");
		addCounter("Total maintenance user operations filtered",               "Number of maintenance user operations read from log devices and skipped since last reset");
		addCounter("Avg operation processing time",                            "Average Log Reader operation processing time (in milliseconds) since last reset");
		addCounter("Total transactions processed",                             "Number of transactions read from log devices since last reset");
		addCounter("Total transactions skipped",                               "Number of transactions read from log devices and not processed for any reason since last reset");
		addCounter("Total transactions opened",                                "Number of begin transaction commands read from log devices since last reset");
		addCounter("Total transactions closed",                                "Number of commit and rollback commands read from log devices since last reset");
		addCounter("Total transactions committed",                             "Number of commit commands read from log devices since last reset");
		addCounter("Total transactions aborted (rolled back)",                 "Number of rollback commands read from log devices since last reset");
		addCounter("Total system transactions skipped",                        "Number of system transactions read from log devices and skipped since last reset");
		addCounter("Avg operations per transaction",                           "Average number of operations in each transaction read from log devices since last reset");
		addCounter("Current scan buffer size",                                 "Current size (in bytes) of the Log Reader scan buffer");
		addCounter("Current operation queue size",                             "Current size (in bytes) of the Log Reader input/operation queue");
		addCounter("Current session cache size",                               "Current size (in bytes) of the session cache");
		addCounter("Log reposition point locator",                             "Locator value of reposition point in log device");
		addCounter("Last processed operation locator",                         "Locator value of most recently processed operation read from log devices");
		addCounter("Average transaction log operation wait time (ms)",         "Average time (in milliseconds) that Log Reader had to wait for each new operation to appear in the log since last reset");
		addCounter("Avg sender operation processing time (ms)",                "Average time (in milliseconds) that Log Reader sender took to process each operation since last reset");
		addCounter("Avg sender operation wait time (ms)",                      "Average time (in milliseconds) that Log Reader sender had to wait to send each processed operation to the LTI input queue since last reset");
		addCounter("Average ChangeSet send time (ms)",                         "Average time (in milliseconds) that Log Reader sender took to send each processed operation to the LTI input queue since last reset");
		addCounter("Total sender operations processed",                        "Number of operations that Log Reader sender processed since last reset");
		addCounter("Current marked objects cache size",                        "Marked objects cache size");

		// Log Reader Statistics for Oracle
		addCounter("Average RBA search time (ms)",                             "The average record byte address (RBA) search time during log scanner positioning");
		addCounter("Total bytes read",                                         "The total number of bytes read from the primary database transaction log");
		addCounter("Total log records read",                                   "The total number of log records read from the primary database transaction log");
		addCounter("Average number of bytes read per second",                  "The average number of bytes read from the primary database transaction log per second");
		addCounter("Average number of bytes per record",                       "The average number of bytes per log record read");
		addCounter("Average time (ms) per log read",                           "The average time per primary database transaction log read");
		addCounter("Total online log read time (ms)",                          "The total time spent reading the primary database online transaction redo log");
		addCounter("Total archive log read time (ms)",                         "The total time spent reading primary database transaction redo log archives");
		addCounter("Average time (ms) per online log device read",             "The average time per online log device read");
		addCounter("Average time (ms) per archive log device read",            "The average time per archive log device read");
		addCounter("Total log records queued",                                 "The total number of log records queued for processing");
		addCounter("Total log records filtered",                               "The total number of log records filtered");
		addCounter("Log scan checkpoint set size",                             "The current number of log records in the checkpoint set");
		addCounter("Average number of log records per checkpoint",             "The average number of log records for each checkpoint log record read");
		addCounter("Average number of seconds between log record checkpoints", "The average number of seconds between reading log record checkpoints");
		addCounter("Total operations scanned",                                 "Number of operations read from log devices since last reset");
		addCounter("Total operations processed",                               "Number of operations read from log devices and passed to LTI since last reset");
		addCounter("Total operations skipped",                                 "Number of operations read from log devices and not processed for any reason since last reset");
		addCounter("Total maintenance user operations filtered",               "Number of maintenance user operations read from log devices and skipped since last reset");
		addCounter("Avg operation processing time",                            "Average Log Reader operation processing time (in milliseconds) since last reset");
		addCounter("Total transactions processed",                             "Number of transactions read from log devices since last reset");
		addCounter("Total transactions skipped",                               "Number of transactions read from log devices and not processed for any reason since last reset");
		addCounter("Total transactions opened",                                "Number of begin transaction commands read from log devices since last reset");
		addCounter("Total transactions closed",                                "Number of commit and rollback commands read from log devices since last reset");
		addCounter("Total transactions committed",                             "Number of commit commands read from log devices since last reset");
		addCounter("Total transactions aborted (rolled back)",                 "Number of rollback commands read from log devices since last reset");
		addCounter("Total system transactions skipped",                        "Number of system transactions read from log devices and skipped since last reset");
		addCounter("Avg ops per transaction",                                  "Average number of operations in each transaction read from log devices since last reset");
		addCounter("Current scan buffer size",                                 "Current size (in bytes) of the Log Reader scan buffer");
		addCounter("Current operation queue size",                             "Current size (in bytes) of the Log Reader input/operation queue");
		addCounter("Current session cache size",                               "Current size (in bytes) of the session cache");
		addCounter("Total LOB operations processed by query data from PDB",    "The total number of LOB operations that have been processed from the primary database");
		addCounter("Avg time used to query PDB for LOB operation processing",  "The average time taken to query the primary database to process a LOB");
		addCounter("Current Op Proc RASD marked object cache size",            "Current size of the operation processor marked object repository cache");
		addCounter("Total number of Op Proc RASD marked object cache hits",    "Total number of operation processor marked object repository cache hits");
		addCounter("Total number of Op Proc RASD marked object cache misses",  "Total number of operation processor marked object repository cache misses");
		addCounter("Log reposition point locator",                             "Locator value of reposition point in log device");
		addCounter("Last processed operation locator",                         "Locator value of most recently processed operation read from log devices");
		addCounter("Avg xlog operation wait time (ms)",                        "Average time (in milliseconds) that Log Reader had to wait for each new operation to appear in the log since last reset");
		addCounter("Avg sender operation processing time (ms)",                "Average time (in milliseconds) that Log Reader sender took to process each operation since last reset");
		addCounter("Avg sender operation wait time (ms)",                      "Average time (in milliseconds) that Log Reader sender had to wait to send each processed operation to the LTI input queue since last reset");
		addCounter("Avg change set send time (ms)",                            "Average time (in milliseconds) that Log Reader sender took to send each processed operation to the LTI input queue since last reset");
		addCounter("Number of sender operations processed",                    "Number of operations that Log Reader sender processed since last reset");
		addCounter("Current marked objects cache size",                        "Marked objects cache size");

		// Additional Statistics for Oracle RAC
		addCounter("Log scan reader current LSN",                              "The current log sequence number of the log being read for each cluster instance");
		addCounter("Log scan reader end-of-log status",                        "The current end of log status for each cluster log scanner");
		addCounter("Log scan reader last read time",                           "The number of seconds since the last read for each cluster scanner");
		addCounter("Log scan record set distribution",                         "Distribution of the log scan checkpoint set across all log scan threads");
		addCounter("Log scan reader last record SCN",                          "The SCN of the last log record read by each cluster scanner");
		addCounter("Log scan reader checkpoints",                              "The checkpoint SCN of the last checkpoint log record read by each cluster scanner");
		addCounter("Log scan checkpoint SCN",                                  "The current checkpoint SCN, based on all cluster scanners");
		addCounter("Log scan active checkpoint SCN",                           "The active checkpoint SCN, based on all cluster scanner");
		addCounter("Total log records read per redo log thread",               "The distribution of total log records read across all log scan threads");
		addCounter("Log scan record set sizes",                                "The current scan record set size for each cluster scanner");
		addCounter("Log scan checkpoint queue sizes",                          "The current checkpoint queue size for each cluster scanner");

		// Log Transfer Interface Statistics
		addCounter("Number of LTL commands sent",                              "Total number of LTL commands sent to Replication Server since last reset");
		addCounter("Avg LTL command size",                                     "Average size (in bytes) of each LTL command sent to Replication Server since last reset");
		addCounter("Avg LTL commands/sec",                                     "Average number of LTL commands sent per second to Replication Server since last reset");
		addCounter("Total bytes sent",                                         "Number of bytes sent to Replication Server since last reset");
		addCounter("Avg Bytes/second during transmission",                     "Average bytes per second sent over connection to Replication Server since last reset");
		addCounter("Avg LTL buffer cache time",                                "Average time (in milliseconds) it takes between placing the LTL commands into the LTL buffer to the time it is actually sent to Replication Server");
		addCounter("Avg Rep Server turnaround time",                           "Average time (in milliseconds) it takes Replication Server to acknowledge each LTL command buffer sent since last reset");
		addCounter("Avg time to create distributes",                           "Average time (in milliseconds) LTI takes to convert a change-set into LTL since last reset");
		addCounter("Avg LTL buffer size",                                      "Average size (in bytes) of each LTL buffer sent to Replication Server since last reset");
		addCounter("Avg LTM buffer utilization (%)",                           "Average utilization (in percentage of LTL buffer size) of each LTL buffer sent to Replication Server since last reset");
		addCounter("Avg LTL commands/buffer",                                  "Average number of LTL commands per buffer sent to Replication Server since last reset");
		addCounter("Encoded column name cache size",                           "Current encoded column name cache size");
		addCounter("Current number of commands in the LTI queue",              "Current number of commands in the LTI queue");
		addCounter("Current number of unformatted commands in the LTI queue",  "Current number of unformatted commands in the LTI queue");
		addCounter("Last QID sent",                                            "Hex value of most recent origin queue ID sent to Replication Server");
		addCounter("Last transaction id sent",                                 "Hex value of most recent transaction ID sent to Replication Server");
	}
	
	public static void main(String[] args)
	{
		// Just a test to see if we are creating duplicate descriptions...
		// If there are duplicates, some System.out.println("WARNING: ...") should be printed 
		System.out.println("XXXXX="+getDesc("xxx"));
	}
}
