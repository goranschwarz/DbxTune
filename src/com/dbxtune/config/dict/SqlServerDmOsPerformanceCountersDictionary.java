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

public class SqlServerDmOsPerformanceCountersDictionary
{
	/** Instance variable */
	private static SqlServerDmOsPerformanceCountersDictionary _instance = null;

	private HashMap<String, Record> _map = new HashMap<>();

	public static class Record
	{
		private String _srvType         = null;
		private String _section         = null;
		private String _field           = null;
		private String _description     = null;

		public Record(String srvType, String section, String field, String description)
		{
			_srvType     = srvType;
			_section     = section;
			_field       = field;
			_description = description;
		}
		
		@Override
		public String toString()
		{
			return "srvType='"+_srvType+"', section='"+_section+"', field='"+_field+"', description='"+_description+"'.";
//			return StringUtil.left(_id, 50) + " - " + _description;
		}
	}


	public SqlServerDmOsPerformanceCountersDictionary()
	{
		init();
	}

	public static SqlServerDmOsPerformanceCountersDictionary getInstance()
	{
		if (_instance == null)
			_instance = new SqlServerDmOsPerformanceCountersDictionary();
		return _instance;
	}

	/**
	 * Strips out all HTML and return it as a "plain" text
	 * @param waitName
	 * @return
	 */
	public String getDescriptionPlain(String section, String field)
	{
		Record rec = _map.get(section+"|"+field);
		if (rec != null)
			return StringUtil.stripHtml(rec._description);

		// Compose an empty one
		return "";
//		return "WaitName '"+waitName+"' not found in dictionary.";
	}


	public String getDescriptionHtml(String section, String field, String instance, String calculated_value)
	{
		Record rec = _map.get(section+"|"+field);
		if (rec != null)
		{
			StringBuilder sb = new StringBuilder();
			sb.append("<html>");
			sb.append("<table cellpadding='0'>");
			sb.append("    <tr> <td>    <b>Section               </b></td> <td>"    ).append(section)         .append("</td></tr>");
			sb.append("    <tr> <td>    <b>Field                 </b></td> <td>"    ).append(field)           .append("</td></tr>");
			if (StringUtil.hasValue(instance)) 
				sb.append("<tr> <td>    <b>Instance              </b></td> <td>"    ).append(instance)        .append("</td></tr>");
			sb.append("    <tr> <td><br><b>Description           </b></td> <td><br>").append(rec._description).append("</td></tr>");
			sb.append("    <tr> <td>    <b>Calculated Value&nbsp;</b></td> <td>"    ).append(calculated_value).append("</td></tr>");
			sb.append("</table>");
			sb.append("<br>");
			sb.append("<hr>");
			sb.append("The above information was found at:<br>");
			sb.append("<a href='https://docs.microsoft.com/en-us/sql/relational-databases/performance-monitor/use-sql-server-objects'>https://docs.microsoft.com/en-us/sql/relational-databases/performance-monitor/use-sql-server-objects</a>");
			sb.append("</html>");
			return sb.toString();
		}

		// Compose an empty one
		return "<html><code>" + section + " - " + field + "</code> <br><b>not found in dictionary.</b></html>";
	}


	private void set(Record rec)
	{
		String pk = rec._section+"|"+rec._field;
		
		Record old = _map.get(pk);
		if ( old != null)
		{
			System.out.println("Field '"+pk+"' already exists. It will be overwritten.");
			System.out.println("      >>> new record: "+rec);
			System.out.println("      >>> old record: "+old);
		}

		_map.put(pk, rec);
	}

	private void add(String srvType, String section, String field, String desc)
	{
		set(new Record(srvType, section, field, desc));
	}

	private void init()
	{
		// Below counters was copied from - https://docs.microsoft.com/en-us/sql/relational-databases/performance-monitor/use-sql-server-objects

		add("SQL Server Agent", "Alerts", "Activated alerts",        "This counter reports the total number of alerts that SQL Server Agent has activated since the last time that SQL Server Agent restarted.");
		add("SQL Server Agent", "Alerts", "Alerts activated/minute", "This counter reports the number of alerts that SQL Server Agent activated within the last minute.");

		add("SQL Server Agent", "Jobs", "Active Jobs",           "This counter reports the number of jobs currently running.");
		add("SQL Server Agent", "Jobs", "Failed jobs",           "This counter reports the number of jobs that exited with failure.");
		add("SQL Server Agent", "Jobs", "Job success rate",      "This counter reports the percentage of executed jobs that completed successfully.");
		add("SQL Server Agent", "Jobs", "Jobs activated/minute", "This counter reports the number of jobs launched within the last minute.");
		add("SQL Server Agent", "Jobs", "Queued jobs",           "This counter reports the number of jobs that are ready for SQL Server Agent to run, but which have not yet started running.");
		add("SQL Server Agent", "Jobs", "Successful jobs",       "This counter reports the number of jobs that exited with success.");

		add("SQL Server Agent", "JobSteps", "Active steps",       "This counter reports the number of job steps currently running.");
		add("SQL Server Agent", "JobSteps", "Queued steps",       "This counter reports the number of job steps that are ready for SQL Server Agent to run, but which have not yet started running.");
		add("SQL Server Agent", "JobSteps", "Total step retries", "This counter reports the total number of times that Microsoft SQL Server has retried a job step since the last server restart.");

		add("SQL Server Agent", "Statistics", "SQL Server Restarted", "The number of times the Microsoft SQL Server has been successfully restarted by SQL Server Agent since the last time that SQL Server Agent started.");

		add("SQL Server", "Access Methods", "AU cleanup batches/sec",          "The number of batches per second that were completed successfully by the background task that cleans up deferred dropped allocation units.");
		add("SQL Server", "Access Methods", "AU cleanups/sec",                 "The number of allocation units per second that were successfully dropped the background task that cleans up deferred dropped allocation units. Each allocation unit drop requires multiple batches.");
		add("SQL Server", "Access Methods", "By-reference Lob Create Count",   "Count of large object (lob) values that were passed by reference. By-reference lobs are used in certain bulk operations to avoid the cost of passing them by value.");
		add("SQL Server", "Access Methods", "By-reference Lob Use Count",      "Count of by-reference lob values that were used. By-reference lobs are used in certain bulk operations to avoid the cost of passing them by-value.");
		add("SQL Server", "Access Methods", "Count Lob Readahead",             "Count of lob pages on which readahead was issued.");
		add("SQL Server", "Access Methods", "Count Pull In Row",               "Count of column values that were pulled in-row from off-row.");
		add("SQL Server", "Access Methods", "Count Push Off Row",              "Count of column values that were pushed from in-row to off-row.");
		add("SQL Server", "Access Methods", "Deferred Dropped Aus",            "The number of allocation units waiting to be dropped by the background task that cleans up deferred dropped allocation units.");
		add("SQL Server", "Access Methods", "Deferred Dropped rowsets",        "The number of rowsets created as a result of aborted online index build operations that are waiting to be dropped by the background task that cleans up deferred dropped rowsets.");
		add("SQL Server", "Access Methods", "Dropped rowset cleanups/sec",     "The number of rowsets per second created as a result of aborted online index build operations that were successfully dropped by the background task that cleans up deferred dropped rowsets.");
		add("SQL Server", "Access Methods", "Dropped rowsets skipped/sec",     "The number of rowsets per second created as a result of aborted online index build operations that were skipped by the background task that cleans up deferred dropped rowsets created.");
		add("SQL Server", "Access Methods", "Extent Deallocations/sec",        "Number of extents deallocated per second in all databases in this instance of SQL Server.");
		add("SQL Server", "Access Methods", "Extents Allocated/sec",           "Number of extents allocated per second in all databases in this instance of SQL Server.");
		add("SQL Server", "Access Methods", "Failed AU cleanup batches/sec",   "The number of batches per second that failed and required retry, by the background task that cleans up deferred dropped allocation units. Failure could be due to lack of memory or disk space, hardware failure and other reasons.");
		add("SQL Server", "Access Methods", "Failed leaf page cookie",         "The number of times that a leaf page cookie could not be used during an index search since changes happened on the leaf page. The cookie is used to speed up index search.");
		add("SQL Server", "Access Methods", "Failed tree page cookie",         "The number of times that a tree page cookie could not be used during an index search since changes happened on the parent pages of those tree pages. The cookie is used to speed up index search.");
		add("SQL Server", "Access Methods", "Forwarded Records/sec",           "Number of records per second fetched through forwarded record pointers.");
		add("SQL Server", "Access Methods", "FreeSpace Page Fetches/sec",      "Number of pages fetched per second by free space scans. These scans search for free space within pages already allocated to an allocation unit, to satisfy requests to insert or modify record fragments.");
		add("SQL Server", "Access Methods", "FreeSpace Scans/sec",             "Number of scans per second that were initiated to search for free space within pages already allocated to an allocation unit to insert or modify record fragment. Each scan may find multiple pages.");
		add("SQL Server", "Access Methods", "Full Scans/sec",                  "Number of unrestricted full scans per second. These can be either base-table or full-index scans.");
		add("SQL Server", "Access Methods", "Index Searches/sec",              "Number of index searches per second. These are used to start a range scan, reposition a range scan, revalidate a scan point, fetch a single index record, and search down the index to locate where to insert a new row.");
		add("SQL Server", "Access Methods", "InSysXact waits/sec",             "Number of times a reader needs to wait for a page because the InSysXact bit is set.");
		add("SQL Server", "Access Methods", "LobHandle Create Count",          "Count of temporary lobs created.");
		add("SQL Server", "Access Methods", "LobHandle Destroy Count",         "Count of temporary lobs destroyed.");
		add("SQL Server", "Access Methods", "LobSS Provider Create Count",     "Count of LOB Storage Service Providers (LobSSP) created. One worktable created per LobSSP.");
		add("SQL Server", "Access Methods", "LobSS Provider Destroy Count",    "Count of LobSSP destroyed.");
		add("SQL Server", "Access Methods", "LobSS Provider Truncation Count", "Count of LobSSP truncated.");
		add("SQL Server", "Access Methods", "Mixed page allocations/sec",      "Number of pages allocated per second from mixed extents. These could be used for storing the IAM pages and the first eight pages that are allocated to an allocation unit.");
		add("SQL Server", "Access Methods", "Page compression attempts/sec",   "Number of pages evaluated for page-level compression. Includes pages that were not compressed because significant savings could be achieved. Includes all objects in the instance of SQL Server. For information about specific objects, see�sys.dm_db_index_operational_stats (Transact-SQL).");
		add("SQL Server", "Access Methods", "Page Deallocations/sec",          "Number of pages deallocated per second in all databases in this instance of SQL Server. These include pages from mixed extents and uniform extents.");
		add("SQL Server", "Access Methods", "Page Splits/sec",                 "Number of page splits per second that occur as the result of overflowing index pages.");
		add("SQL Server", "Access Methods", "Pages Allocated/sec",             "Number of pages allocated per second in all databases in this instance of SQL Server. These include pages allocations from both mixed extents and uniform extents.");
		add("SQL Server", "Access Methods", "Pages compressed/sec",            "Number of data pages that are compressed by using PAGE compression. Includes all objects in the instance of SQL Server. For information about specific objects, see�sys.dm_db_index_operational_stats (Transact-SQL).");
		add("SQL Server", "Access Methods", "Probe Scans/sec",                 "Number of probe scans per second that are used to find at most one single qualified row in an index or base table directly.");
		add("SQL Server", "Access Methods", "Range Scans/sec",                 "Number of qualified range scans through indexes per second.");
		add("SQL Server", "Access Methods", "Scan Point Revalidations/sec",    "Number of times per second that the scan point had to be revalidated to continue the scan.");
		add("SQL Server", "Access Methods", "Skipped Ghosted Records/sec",     "Number of ghosted records per second skipped during scans.");
		add("SQL Server", "Access Methods", "Table Lock Escalations/sec",      "Number of times locks on a table were escalated to the TABLE or HoBT granularity.");
		add("SQL Server", "Access Methods", "Used leaf page cookie",           "Number of times a leaf page cookie is used successfully during an index search since no change happened on the leaf page. The cookie is used to speed up index search.");
		add("SQL Server", "Access Methods", "Used tree page cookie",           "Number of times a tree page cookie is used successfully during an index search since no change happened on the parent page of the tree page. The cookie is used to speed up index search.");
		add("SQL Server", "Access Methods", "Workfiles Created/sec",           "Number of work files created per second. For example, work files could be used to store temporary results for hash joins and hash aggregates.");
		add("SQL Server", "Access Methods", "Worktables Created/sec",          "Number of work tables created per second. For example, work tables could be used to store temporary results for query spool, lob variables, XML variables, and cursors.");
		add("SQL Server", "Access Methods", "Worktables From Cache Base",      "For internal use only.");
		add("SQL Server", "Access Methods", "Worktables From Cache Ratio",     "Percentage of work tables created where the initial two pages of the work table were not allocated but were immediately available from the work table cache. (When a work table is dropped, two pages may remain allocated and they are returned to the work table cache. This increases performance.)");

		add("SQL Server", "Availability Replica", "Bytes Received from Replica/sec", "Number of bytes received from the availability replica per second. Pings and status updates will generate network traffic even on databases with no user updates.");
		add("SQL Server", "Availability Replica", "Bytes Sent to Replica/sec",       "Number of bytes sent to the remote availability replica per second. On the primary replica this is the number of bytes sent to the secondary replica. On the secondary replica this is the number of bytes sent to the primary replica.");
		add("SQL Server", "Availability Replica", "Bytes Sent to Transport/sec",     "Actual number of bytes sent per second over the network to the remote availability replica. On the primary replica this is the number of bytes sent to the secondary replica. On the secondary replica this is the number of bytes sent to the primary replica.");
		add("SQL Server", "Availability Replica", "Flow Control Time (ms/sec)",      "Time in milliseconds that log stream messages waited for send flow control, in the last second.");
		add("SQL Server", "Availability Replica", "Flow Control/sec",                "Number of times flow-control initiated in the last second.�Flow Control Time (ms/sec)�divided by�Flow Control/sec�is the average time per wait.");
		add("SQL Server", "Availability Replica", "Receives from Replica/sec",       "Number of Always On messages received from thereplica per second.");
		add("SQL Server", "Availability Replica", "Resent Messages/sec",             "Number of Always On messages resent in the last second.");
		add("SQL Server", "Availability Replica", "Sends to Replica/sec",            "Number of Always On messages sent to this availability replica per second.");
		add("SQL Server", "Availability Replica", "Sends to Transport/sec",          "Actual number of Always On messages sent per second over the network to the remote availability replica. On the primary replica this is the number of messages sent to the secondary replica. On the secondary replica this is the number of messages sent to the primary replica.");

		add("SQL Server", "Backup Device", "Device Throughput Bytes/sec", "Throughput of read and write operations (in bytes per second) for a backup device used when backing up or restoring databases. This counter exists only while the backup or restore operation is executing.");

		add("SQL Server", "Batch Resp Statistics", "Batches >=000000ms & <000001ms", "Number of SQL Batches having response time greater than or equal to 0ms but less than 1ms");
		add("SQL Server", "Batch Resp Statistics", "Batches >=000001ms & <000002ms", "Number of SQL Batches having response time greater than or equal to 1ms but less than 2ms");
		add("SQL Server", "Batch Resp Statistics", "Batches >=000002ms & <000005ms", "Number of SQL Batches having response time greater than or equal to 2ms but less than 5ms");
		add("SQL Server", "Batch Resp Statistics", "Batches >=000005ms & <000010ms", "Number of SQL Batches having response time greater than or equal to 5ms but less than 10ms");
		add("SQL Server", "Batch Resp Statistics", "Batches >=000010ms & <000020ms", "Number of SQL Batches having response time greater than or equal to 10ms but less than 20ms");
		add("SQL Server", "Batch Resp Statistics", "Batches >=000020ms & <000050ms", "Number of SQL Batches having response time greater than or equal to 20ms but less than 50ms");
		add("SQL Server", "Batch Resp Statistics", "Batches >=000050ms & <000100ms", "Number of SQL Batches having response time greater than or equal to 50ms but less than 100ms");
		add("SQL Server", "Batch Resp Statistics", "Batches >=000100ms & <000200ms", "Number of SQL Batches having response time greater than or equal to 100ms but less than 200ms");
		add("SQL Server", "Batch Resp Statistics", "Batches >=000200ms & <000500ms", "Number of SQL Batches having response time greater than or equal to 200ms but less than 500ms");
		add("SQL Server", "Batch Resp Statistics", "Batches >=000500ms & <001000ms", "Number of SQL Batches having response time greater than or equal to 500ms but less than 1,000ms");
		add("SQL Server", "Batch Resp Statistics", "Batches >=001000ms & <002000ms", "Number of SQL Batches having response time greater than or equal to 1,000ms but less than 2,000ms");
		add("SQL Server", "Batch Resp Statistics", "Batches >=002000ms & <005000ms", "Number of SQL Batches having response time greater than or equal to 2,000ms but less than 5,000ms");
		add("SQL Server", "Batch Resp Statistics", "Batches >=005000ms & <010000ms", "Number of SQL Batches having response time greater than or equal to 5,000ms but less than 10,000ms");
		add("SQL Server", "Batch Resp Statistics", "Batches >=010000ms & <020000ms", "Number of SQL Batches having response time greater than or equal to 10,000ms but less than 20,000ms");
		add("SQL Server", "Batch Resp Statistics", "Batches >=020000ms & <050000ms", "Number of SQL Batches having response time greater than or equal to 20,000ms but less than 50,000ms");
		add("SQL Server", "Batch Resp Statistics", "Batches >=050000ms & <100000ms", "Number of SQL Batches having response time greater than or equal to 50,000ms but less than 100,000ms");
		add("SQL Server", "Batch Resp Statistics", "Batches >=100000ms",             "Number of SQL Batches having response time greater than or equal to 100,000ms");

		add("SQL Server", "Broker Activation", "Stored Procedures Invoked/sec", "This counter reports the total number of activation stored procedures invoked by all queue monitors in the instance per second.");
		add("SQL Server", "Broker Activation", "Task Limit Reached",            "This counter reports the total number of times that a queue monitor would have started a new task, but did not because the maximum number of tasks for the queue is already running.");
		add("SQL Server", "Broker Activation", "Task Limit Reached/sec",        "This counter reports the number of times per second that a queue monitor would have started a new task, but did not because the maximum number of tasks for the queue is already running.");
		add("SQL Server", "Broker Activation", "Tasks Aborted/sec",             "This counter reports the number of activation stored procedure tasks that end with an error, or are aborted by a queue monitor for failing to receive messages.");
		add("SQL Server", "Broker Activation", "Tasks Running",                 "This counter reports the number of activation stored procedures that are currently running.");
		add("SQL Server", "Broker Activation", "Tasks Started/sec",             "This counter reports the number of activation stored procedures started per second by all queue monitors in the instance.");

		add("SQL Server", "Broker Statistics", "Activation Errors Total",          "The number of times a Service Broker activation stored procedure exited with an error.");
		add("SQL Server", "Broker Statistics", "Broker Transaction Rollbacks",     "The number of rolled-back transactions that contained DML statements related to Service Broker, such as SEND and RECEIVE.");
		add("SQL Server", "Broker Statistics", "Corrupted Messages Total",         "The number of corrupted messages that were received by the instance.");
		add("SQL Server", "Broker Statistics", "Dequeued Transmission Msgs/sec",   "The number of messages that have been removed from the Service Broker transmission queue per second.");
		add("SQL Server", "Broker Statistics", "Dialog timer event count",         "The number of timers active in the dialog protocol layer. This number corresponds to the number of active dialogs.");
		add("SQL Server", "Broker Statistics", "Dropped Messages Total",           "The number of messages that were received by the instance, but could not be delivered to a queue.");
		add("SQL Server", "Broker Statistics", "Enqueued Local Messages Total",    "The number of messages that have been put into the queues in the instance, counting only messages that did not arrive through the network.");
		add("SQL Server", "Broker Statistics", "Enqueued Local Messages/sec",      "The number of messages per second that have been put into the queues in the instance, counting only messages that did not arrive through the network.");
		add("SQL Server", "Broker Statistics", "Enqueued Messages Total",          "The total number of messages that have been put into the queues in the instance.");
		add("SQL Server", "Broker Statistics", "Enqueued Messages/sec",            "The number of messages per second that have been put into the queues in the instance. This includes messages of all priority levels.");
		add("SQL Server", "Broker Statistics", "Enqueued P1 Msgs/sec",             "The number of priority 1 messages per second that have been put into the queues in the instance.");
		add("SQL Server", "Broker Statistics", "Enqueued P2 Msgs/sec",             "The number of priority 2 messages per second that have been put into the queues in the instance.");
		add("SQL Server", "Broker Statistics", "Enqueued P3 Msgs/sec",             "The number of priority 3 messages per second that have been put into the queues in the instance.");
		add("SQL Server", "Broker Statistics", "Enqueued P4 Msgs/sec",             "The number of priority 4 messages per second that have been put into the queues in the instance.");
		add("SQL Server", "Broker Statistics", "Enqueued P5 Msgs/sec",             "The number of priority 5 messages per second that have been put into the queues in the instance.");
		add("SQL Server", "Broker Statistics", "Enqueued P6 Msgs/sec",             "The number of priority 6 messages per second that have been put into the queues in the instance.");
		add("SQL Server", "Broker Statistics", "Enqueued P7 Msgs/sec",             "The number of priority 7 messages per second that have been put into the queues in the instance.");
		add("SQL Server", "Broker Statistics", "Enqueued P8 Msgs/sec",             "The number of priority 8 messages per second that have been put into the queues in the instance.");
		add("SQL Server", "Broker Statistics", "Enqueued P9 Msgs/sec",             "The number of priority 9 messages per second that have been put into the queues in the instance.");
		add("SQL Server", "Broker Statistics", "Enqueued P10 Msgs/sec",            "The number of priority 10 messages per second that have been put into the queues in the instance.");
		add("SQL Server", "Broker Statistics", "Enqueued Transmission Msgs/sec",   "The number of messages that have been placed in the Service Broker transmission queue per second.");
		add("SQL Server", "Broker Statistics", "Enqueued Transport Msg Frag Tot",  "The number of message fragments that have been put into the queues in the instance, counting only messages that arrived through the network.");
		add("SQL Server", "Broker Statistics", "Enqueued Transport Msg Frags/sec", "The number of message fragments per second that have been put into the queues in the instance.");
		add("SQL Server", "Broker Statistics", "Enqueued Transport Msgs Total",    "The number of messages that have been put into the queues in the instance, counting only messages that arrived through the network.");
		add("SQL Server", "Broker Statistics", "Enqueued Transport Msgs/sec",      "The number of messages per second that have been put into the queues in the instance, counting only messages that arrived through the network.");
		add("SQL Server", "Broker Statistics", "Forwarded Messages Total",         "The total number of Service Broker messages forwarded by this computer.");
		add("SQL Server", "Broker Statistics", "Forwarded Messages/sec",           "The number of messages per second forwarded by this computer.");
		add("SQL Server", "Broker Statistics", "Forwarded Msg Byte Total",         "The total size, in bytes, of the messages forwarded by this computer.");
		add("SQL Server", "Broker Statistics", "Forwarded Msg Bytes/sec",          "The size, in bytes, of messages per second forwarded by this computer.");
		add("SQL Server", "Broker Statistics", "Forwarded Msg Discarded Total",    "The number of messages that this computer received for forwarding, but did not successfully forward.");
		add("SQL Server", "Broker Statistics", "Forwarded Msg Discarded/sec",      "The number of messages per second that this computer received for forwarding, but did not successfully forward.");
		add("SQL Server", "Broker Statistics", "Forwarded Pending Msg Bytes",      "The total size of the messages currently held for forwarding.");
		add("SQL Server", "Broker Statistics", "Forwarded Pending Msg Count",      "The total number of messages currently held for forwarding.");
		add("SQL Server", "Broker Statistics", "SQL RECEIVE Total",                "The total number of Transact-SQL RECEIVE statements processed.");
		add("SQL Server", "Broker Statistics", "SQL RECEIVEs/sec",                 "The number of Transact-SQL RECEIVE statements processed per second.");
		add("SQL Server", "Broker Statistics", "SQL SEND Total",                   "The total number of Transact-SQL SEND statements executed.");
		add("SQL Server", "Broker Statistics", "SQL SENDs/sec",                    "The number of Transact-SQL SEND statements executed per second.");

		add("SQL Server", "Broker TO Statistics", "Avg. Length of Batched Writes",  "The average number of transmission objects saved in a batch.");
		add("SQL Server", "Broker TO Statistics", "Avg. Time To Write Batch (ms)",  "The average number of milliseconds required to save a batch of transmission objects.");
		add("SQL Server", "Broker TO Statistics", "Avg. Time to Write Batch Base",  "For internal use only.");
		add("SQL Server", "Broker TO Statistics", "Avg. Time Between Batches (ms)", "The average number of milliseconds between writes of transmission object batches.");
		add("SQL Server", "Broker TO Statistics", "Avg. Time Between Batches Base", "For internal use only.");
		add("SQL Server", "Broker TO Statistics", "Tran Object Gets/sec",           "The number of times per second that dialogs requested transmission objects.");
		add("SQL Server", "Broker TO Statistics", "Tran Objects Marked Dirty/sec",  "The number of times per second that transmission objects were marked as dirty. Transmission objects are marked as dirty by the first modification that causes the in-memory copy to differ from the copy stored in�tempdb. Transmission objects are modified when Service Broker has to record a change in the state of message transmissions for the dialog.");
		add("SQL Server", "Broker TO Statistics", "Tran Object Writes/sec",         "The number of times per second that a batch of transmission objects were written to�tempdb�work tables. Large numbers of writes could indicate that SQL Server memory is being stressed.");

		add("SQL Server", "Broker - DBM Transport", "Current Bytes for Recv I/O",       "This counter reports the number of bytes to be read by the currently running transport receive operations.");
		add("SQL Server", "Broker - DBM Transport", "Current Bytes for Send I/O",       "This counter reports the number of bytes in message fragments that are currently in the process of being sent over the network.");
		add("SQL Server", "Broker - DBM Transport", "Current Msg Frags for Send I/O",   "This counter reports the total number of message fragments that are in the process of being sent over the network.");
		add("SQL Server", "Broker - DBM Transport", "Message Fragment P1 Sends/sec",    "This counter reports the number of priority 1 message fragments sent over the network per second.");
		add("SQL Server", "Broker - DBM Transport", "Message Fragment P2 Sends/sec",    "This counter reports the number of priority 2 message fragments sent over the network per second.");
		add("SQL Server", "Broker - DBM Transport", "Message Fragment P3 Sends/sec",    "This counter reports the number of priority 3 message fragments sent over the network per second.");
		add("SQL Server", "Broker - DBM Transport", "Message Fragment P4 Sends/sec",    "This counter reports the number of priority 4 message fragments sent over the network per second.");
		add("SQL Server", "Broker - DBM Transport", "Message Fragment P5 Sends/sec",    "This counter reports the number of priority 5 message fragments sent over the network per second.");
		add("SQL Server", "Broker - DBM Transport", "Message Fragment P6 Sends/sec",    "This counter reports the number of priority 6 message fragments sent over the network per second.");
		add("SQL Server", "Broker - DBM Transport", "Message Fragment P7 Sends/sec",    "This counter reports the number of priority 7 message fragments sent over the network per second.");
		add("SQL Server", "Broker - DBM Transport", "Message Fragment P8 Sends/sec",    "This counter reports the number of priority 8 message fragments sent over the network per second.");
		add("SQL Server", "Broker - DBM Transport", "Message Fragment P9 Sends/sec",    "This counter reports the number of priority 9 message fragments sent over the network per second.");
		add("SQL Server", "Broker - DBM Transport", "Message Fragment P10 Sends/sec",   "This counter reports the number of priority 10 message fragments sent over the network per second.");
		add("SQL Server", "Broker - DBM Transport", "Message Fragment Receives/sec",    "This counter reports the number of message fragments received over the network per second.");
		add("SQL Server", "Broker - DBM Transport", "Message Fragment Sends/sec",       "This counter reports the number of message fragments of all priorities sent over the network per second.");
		add("SQL Server", "Broker - DBM Transport", "Msg Fragment Recv Size Avg",       "This counter reports the average size of message fragments received over the network.");
		add("SQL Server", "Broker - DBM Transport", "Msg Fragment Recv Size Avg Base",  "For internal use only.");
		add("SQL Server", "Broker - DBM Transport", "Msg Fragment Send Size Avg",       "This counter reports the average size of the message fragments sent over the network.");
		add("SQL Server", "Broker - DBM Transport", "Msg Fragment Send Size Avg Base",  "For internal use only.");
		add("SQL Server", "Broker - DBM Transport", "Open Connection Count",            "This counter reports the number of network connections that Service Broker currently has open.");
		add("SQL Server", "Broker - DBM Transport", "Pending Bytes for Recv I/O",       "This counter reports the number of bytes contained in message fragments that have been received from the network but have not yet been placed on a queue or discarded.");
		add("SQL Server", "Broker - DBM Transport", "Pending Bytes for Send I/O",       "This counter reports the total number of bytes in message fragments that are ready to be sent over the network.");
		add("SQL Server", "Broker - DBM Transport", "Pending Msg Frags for Recv I/O",   "This counter reports the number of message fragments that have been received from the network, but that have not yet been placed on a queue or discarded.");
		add("SQL Server", "Broker - DBM Transport", "Pending Msg Frags for Send I/O",   "This counter reports the total number of message fragments that are ready to be sent over the network.");
		add("SQL Server", "Broker - DBM Transport", "Receive I/O bytes/sec",            "This counter reports the number of bytes per second received over the network by Service Broker endpoints and Database Mirroring endpoints.");
		add("SQL Server", "Broker - DBM Transport", "Receive I/O Bytes Total",          "This counter reports the total number of bytes received over the network by Service Broker endpoints and Database Mirroring endpoints.");
		add("SQL Server", "Broker - DBM Transport", "Receive I/O Len Avg",              "This counter reports the average number of bytes for a transport receive operation.");
		add("SQL Server", "Broker - DBM Transport", "Receive I/O Len Avg Base",         "For internal use only.");
		add("SQL Server", "Broker - DBM Transport", "Receive I/Os/sec",                 "This counter reports the number of transport receive I/O operations per second that the Service Broker / DBM transport layer has completed. Notice that a transport receive operation may contain more than one message fragment.");
		add("SQL Server", "Broker - DBM Transport", "Recv I/O Buffer Copies bytes/sec", "The rate at which transport receive I/O operations had to move buffer fragments in memory.");
		add("SQL Server", "Broker - DBM Transport", "Recv I/O Buffer Copies Count",     "The number of times when transport receive I/O operations had to move buffer fragments in memory.");
		add("SQL Server", "Broker - DBM Transport", "Send I/O bytes/sec",               "This counter reports the number of bytes per second sent over the network by Service Broker endpoints and Database Mirroring endpoints.");
		add("SQL Server", "Broker - DBM Transport", "Send I/O Bytes Total",             "This counter reports the total number of bytes sent over the network by Service Broker endpoints and Database Mirroring endpoints.");
		add("SQL Server", "Broker - DBM Transport", "Send I/O Len Avg",                 "This counter reports the average size in bytes of each transport send operation. Notice that a transport send operation may contain more than one message fragment.");
		add("SQL Server", "Broker - DBM Transport", "Send I/O Len Avg Base",            "For internal use only.");
		add("SQL Server", "Broker - DBM Transport", "Send I/Os/sec",                    "This counter reports the number of transport send I/O operations per second that have completed. Notice that a transport send operation may contain more than one message fragment.");

		add("SQL Server", "Buffer Manager", "Background writer pages/sec",      "Number of pages flushed to enforce the recovery interval settings.");
		add("SQL Server", "Buffer Manager", "Buffer cache hit ratio",           "Indicates the percentage of pages found in the buffer cache without having to read from disk. The ratio is the total number of cache hits divided by the total number of cache lookups over the last few thousand page accesses. After a long period of time, the ratio moves very little. Because reading from the cache is much less expensive than reading from disk, you want this ratio to be high. Generally, you can increase the buffer cache hit ratio by increasing the amount of memory available to SQL Server or by using the buffer pool extension feature.");
		add("SQL Server", "Buffer Manager", "Buffer cache hit ratio base",      "For internal use only.");
		add("SQL Server", "Buffer Manager", "Checkpoint pages/sec",             "Indicates the number of pages flushed to disk per second by a checkpoint or other operation that require all dirty pages to be flushed.");
		add("SQL Server", "Buffer Manager", "Database pages",                   "Indicates the number of pages in the buffer pool with database content.");
		add("SQL Server", "Buffer Manager", "Extension allocated pages",        "Total number of non-free cache pages in the buffer pool extension file.");
		add("SQL Server", "Buffer Manager", "Extension free pages",             "Total number of free cache pages in the buffer pool extension file.");
		add("SQL Server", "Buffer Manager", "Extension in use as percentage",   "Percentage of the buffer pool extension paging file occupied by buffer manager pages.");
		add("SQL Server", "Buffer Manager", "Extension outstanding IO counter", "I/O queue length for the buffer pool extension file.");
		add("SQL Server", "Buffer Manager", "Extension page evictions/sec",     "Number of pages evicted from the buffer pool extension file per second.");
		add("SQL Server", "Buffer Manager", "Extension page reads/sec",         "Number of pages read from the buffer pool extension file per second.");
		add("SQL Server", "Buffer Manager", "Extension page unreferenced time", "Average seconds a page will stay in the buffer pool extension without references to it.");
		add("SQL Server", "Buffer Manager", "Extension pages writes/sec",       "Number of pages written to the buffer pool extension file per second.");
		add("SQL Server", "Buffer Manager", "Free list stalls/sec",             "Indicates the number of requests per second that had to wait for a free page.");
		add("SQL Server", "Buffer Manager", "Integral Controller Slope",        "The slope that integral controller for the buffer pool last used, times -10 billion.");
		add("SQL Server", "Buffer Manager", "Lazy writes/sec",                  "Indicates the number of buffers written per second by the buffer manager's lazy writer. The�lazy writer�is a system process that flushes out batches of dirty, aged buffers (buffers that contain changes that must be written back to disk before the buffer can be reused for a different page) and makes them available to user processes. The lazy writer eliminates the need to perform frequent checkpoints in order to create available buffers.");
		add("SQL Server", "Buffer Manager", "Page life expectancy",             "Indicates the number of seconds a page will stay in the buffer pool without references.");
		add("SQL Server", "Buffer Manager", "Page lookups/sec",                 "Indicates the number of requests per second to find a page in the buffer pool.");
		add("SQL Server", "Buffer Manager", "Page reads/sec",                   "Indicates the number of physical database page reads that are issued per second. This statistic displays the total number of physical page reads across all databases. Because physical I/O is expensive, you may be able to minimize the cost, either by using a larger data cache, intelligent indexes, and more efficient queries, or by changing the database design.");
		add("SQL Server", "Buffer Manager", "Page writes/sec",                  "Indicates the number of physical database page writes that are issued per second.");
		add("SQL Server", "Buffer Manager", "Readahead pages/sec",              "Indicates the number of pages read per second in anticipation of use.");
		add("SQL Server", "Buffer Manager", "Readahead time/sec",               "Time (microseconds) spent issuing readahead.");
		add("SQL Server", "Buffer Manager", "Target pages",                     "Ideal number of pages in the buffer pool.");

		add("SQL Server", "Buffer Node", "Database pages",               "Indicates the number of pages in the buffer pool on this node with database content.");
		add("SQL Server", "Buffer Node", "Page life expectancy",         "Indicates the minimum number of seconds a page will stay in the buffer pool on this node without references.");
		add("SQL Server", "Buffer Node", "Local Node page lookups/sec",  "Indicates the number of lookup requests from this node which were satisfied from this node.");
		add("SQL Server", "Buffer Node", "Remote Note page lookups/sec", "Indicates the number of lookup requests from this node which were satisfied from other nodes.");

		add("SQL Server", "Catalog Metadata", "Cache Entries Count",        "Number of entries in the catalog metadata cache.");
		add("SQL Server", "Catalog Metadata", "Cache Entries Pinned Count", "Number of catalog metadata cache entries that are pinned.");
		add("SQL Server", "Catalog Metadata", "Cache Hit Ratio",            "Ratio between catalog metadata cache hits and lookups.");
		add("SQL Server", "Catalog Metadata", "Cache Hit Ratio Base",       "For internal use only.");

		add("SQL Server", "CLR", "CLR Execution", "Total execution time in CLR (microseconds)");

		add("SQL Server", "Columnstore", "Delta Rowgroups Closed",           "Number of delta rowgroups closed.");
		add("SQL Server", "Columnstore", "Delta Rowgroups Compressed",       "Number of delta rowgroups compressed.");
		add("SQL Server", "Columnstore", "Delta Rowgroups Created",          "Number of delta rowgroups created.");
		add("SQL Server", "Columnstore", "Segment Cache Hit Raio",           "Percentage of column segments that were found in the columnstore pool without having to incur a read from disk.");
		add("SQL Server", "Columnstore", "Segment Cache Hit Ratio Base",     "For internal use only.");
		add("SQL Server", "Columnstore", "Segment Reads/Sec",                "Number of physical segment reads issued.");
		add("SQL Server", "Columnstore", "Total Delete Buffers Migrated",    "Number of times tuple mover has cleaned up the delete buffer.");
		add("SQL Server", "Columnstore", "Total Merge Policy Evaluations",   "Number of times the merge policy for columnstore was evaluated.");
		add("SQL Server", "Columnstore", "Total Rowgroups Compressed",       "Total number of rowgroups compressed.");
		add("SQL Server", "Columnstore", "Total Rowgroups Fit For Merge",    "Number of source rowgroups fit for MERGE since the start of SQL Server.");
		add("SQL Server", "Columnstore", "Total Rowgroups Merge Compressed", "Number of compressed target rowgroups created with MERGE since the start of SQL Server.");
		add("SQL Server", "Columnstore", "Total Source Rowgroups Merged",    "Number of source rowgroups merged since the start of SQL Server.");

		add("SQL Server", "Cursor Manager by Type", "Active cursors",                "Number of active cursors.");
		add("SQL Server", "Cursor Manager by Type", "Cache Hit Ratio",               "Ratio between cache hits and lookups.");
		add("SQL Server", "Cursor Manager by Type", "Cache Hit Ratio Base",          "For internal use only.");
		add("SQL Server", "Cursor Manager by Type", "Cached Cursor Counts",          "Number of cursors of a given type in the cache.");
		add("SQL Server", "Cursor Manager by Type", "Cursor Cache Use Count/sec",    "Times each type of cached cursor has been used.");
		add("SQL Server", "Cursor Manager by Type", "Cursor memory usage",           "Amount of memory consumed by cursors in kilobytes (KB).");
		add("SQL Server", "Cursor Manager by Type", "Cursor Requests/sec",           "Number of SQL cursor requests received by server.");
		add("SQL Server", "Cursor Manager by Type", "Cursor worktable usage",        "Number of worktables used by cursors.");
		add("SQL Server", "Cursor Manager by Type", "Number of active cursor plans", "Number of cursor plans.");

		add("SQL Server", "Cursor Manager Total", "Async population count", "Number of cursors being populated asynchronously.");
		add("SQL Server", "Cursor Manager Total", "Cursor conversion rate", "Number of cursor conversions per second.");
		add("SQL Server", "Cursor Manager Total", "Cursor flushes",         "Total number of run-time statement recreations by cursors.");

		add("SQL Server", "Database Mirroring", "Bytes Received/sec",              "Number of bytes received per second.");
		add("SQL Server", "Database Mirroring", "Bytes Sent/sec",                  "Number of bytes sent per second.");
		add("SQL Server", "Database Mirroring", "Log Bytes Received/sec",          "Number of bytes of log received per second.");
		add("SQL Server", "Database Mirroring", "Log Bytes Redone from Cache/sec", "Number of redone log bytes that were obtained from the mirroring log cache, in the last second. <br>This counter is used on only the mirror server. On the principal server the value is always 0.");
		add("SQL Server", "Database Mirroring", "Log Bytes Sent from Cache/sec",   "Number of sent log bytes that were obtained from the mirroring log cache, in the last second. <br>This counter is used on only the principal server. On the mirror server the value is always 0.");
		add("SQL Server", "Database Mirroring", "Log Bytes Sent/sec",              "Number of bytes of log sent per second.");
		add("SQL Server", "Database Mirroring", "Log Compressed Bytes Rcvd/sec",   "Number of compressed bytes of log received, in the last second.");
		add("SQL Server", "Database Mirroring", "Log Compressed Bytes Sent/sec",   "Number of compressed bytes of log sent, in the last second.");
		add("SQL Server", "Database Mirroring", "Log Harden Time (ms)",            "Milliseconds that log blocks waited to be hardened to disk, in the last second.");
		add("SQL Server", "Database Mirroring", "Log Remaining for Undo KB",       "Total kilobytes of log that remain to be scanned by the new mirror server after failover. <br>This counter is used on only the mirror server during the undo phase. After the undo phase completes, the counter is reset to 0. On the principal server the value is always 0.");
		add("SQL Server", "Database Mirroring", "Log Scanned for Undo KB",         "Total kilobytes of log that have been scanned by the new mirror server since failover.<br> This counter is used on only the mirror server during the undo phase. After the undo phase completes, the counter is reset to 0. On the principal server the value is always 0.");
		add("SQL Server", "Database Mirroring", "Log Send Flow Control Time (ms)", "Milliseconds that log stream messages waited for send flow control, in the last second<br>Sending log data and metadata to the mirroring partner is the most data-intensive operation in database mirroring and might monopolize the database mirroring and Service Broker send buffers. Use this counter to monitor the use of this buffer by the database mirroring session.");
		add("SQL Server", "Database Mirroring", "Log Send Queue KB",               "Total number of kilobytes of log that have not yet been sent to the mirror server.");
		add("SQL Server", "Database Mirroring", "Mirrored Write Transactions/sec", "Number of transactions that wrote to the mirrored database and waited for the log to be sent to the mirror in order to commit, in the last second.<br>This counter is incremented only when the principal server is actively sending log records to the mirror server.");
		add("SQL Server", "Database Mirroring", "Pages Sent/sec",                  "Number of pages sent per second.");
		add("SQL Server", "Database Mirroring", "Receives/sec",                    "Number of mirroring messages received per second.");
		add("SQL Server", "Database Mirroring", "Redo Bytes/sec",                  "Number of bytes of log rolled forward on the mirror database per second.");
		add("SQL Server", "Database Mirroring", "Redo Queue KB",                   "Total number of kilobytes of hardened log that currently remain to be applied to the mirror database to roll it forward. This is sent to the Principal from the Mirror.");
		add("SQL Server", "Database Mirroring", "Send/Receive Ack Time",           "Milliseconds that messages waited for acknowledgement from the partner, in the last second.<br>This counter is helpful in troubleshooting a problem that might be caused by a network bottleneck, such as unexplained failovers, a large send queue, or high transaction latency. In such cases, you can analyze the value of this counter to determine whether the network is causing the problem.");
		add("SQL Server", "Database Mirroring", "Sends/sec",                       "Number of mirroring messages sent per second.");
		add("SQL Server", "Database Mirroring", "Transaction Delay",               "Delay in waiting for unterminated commit acknowledgement.");

		add("SQL Server", "Database Replica", "File Bytes Received/sec", "Amount of FILESTREAM data received by the secondary replica for the secondary database in the last second.");
		add("SQL Server", "Database Replica", "Log Apply Pending Queue", "Number of log blocks that is waiting to be applied to the database replica.");
		add("SQL Server", "Database Replica", "Log Apply Ready Queue", "Number of log blocks that is waiting and ready to be applied to the database replica.");
		add("SQL Server", "Database Replica", "Log Bytes Received/sec", "Amount of log records received by the secondary replica for the database in the last second.");
		add("SQL Server", "Database Replica", "Log remaining for undo", "The amount of log in kilobytes remaining to complete the undo phase.");
		add("SQL Server", "Database Replica", "Log Send Queue", "Amount of log records in the log files of the primary database, in kilobytes, that has not yet been sent to the secondary replica. This value is sent to the secondary replica from the primary replica. Queue size does not include FILESTREAM files that are sent to a secondary.");
		add("SQL Server", "Database Replica", "Mirrored Write Transaction/sec", "Number of transactions that were written to the primary database and then waited to commit until the log was sent to the secondary database, in the last second.");
		add("SQL Server", "Database Replica", "Recovery Queue", "Amount of log records in the log files of the secondary replica that has not yet been redone.");
		add("SQL Server", "Database Replica", "Redo blocked/sec", "Number of times the redo thread is blocked on locks held by readers of the database.");
		add("SQL Server", "Database Replica", "Redo Bytes Remaining", "The amount of log in kilobytes remaining to be redone to finish the reverting phase.");
		add("SQL Server", "Database Replica", "Redone Bytes/sec", "Amount of log records redone on the secondary database in the last second.");
		add("SQL Server", "Database Replica", "Total Log requiring undo", "Total kilobytes of log that must be undone.");
		add("SQL Server", "Database Replica", "Transaction Delay", "Delay in waiting for unterminated commit acknowledgement, in milliseconds.");

		add("SQL Server", "Databases", "Active Transactions", "Number of active transactions for the database.");
		add("SQL Server", "Databases", "Avg Dist From EOL/LP Request", "Average distance in bytes from end of log per log pool request, for requests in the last VLF.");
		add("SQL Server", "Databases", "Backup/Restore Throughput/sec", "Read/write throughput for backup and restore operations of a database per second. For example, you can measure how the performance of the database backup operation changes when more backup devices are used in parallel or when faster devices are used. Throughput of a database backup or restore operation allows you to determine the progress and performance of your backup and restore operations.");
		add("SQL Server", "Databases", "Bulk Copy Rows/sec", "Number of rows bulk copied per second.");
		add("SQL Server", "Databases", "Bulk Copy Throughput/sec", "Amount of data bulk copied (in kilobytes) per second.");
		add("SQL Server", "Databases", "Commit table entries", "The size of the in-memory portion of the commit table for the database. For more information, see�sys.dm_tran_commit_table (Transact-SQL).");
		add("SQL Server", "Databases", "Data File(s) Size (KB)", "Cumulative size (in kilobytes) of all the data files in the database including any automatic growth. Monitoring this counter is useful, for example, for determining the correct size of�tempdb.");
		add("SQL Server", "Databases", "DBCC Logical Scan Bytes/sec", "Number of logical read scan bytes per second for database console commands (DBCC).");
		add("SQL Server", "Databases", "Group Commit Time/sec", "Group stall time (microseconds) per second.");
		add("SQL Server", "Databases", "Log Bytes Flushed/sec", "Total number of log bytes flushed.");
		add("SQL Server", "Databases", "Log Cache Hit Ratio", "Percentage of log cache reads satisfied from the log cache.");
		add("SQL Server", "Databases", "Log Cache Hit Ratio Base", "For internal use only.");
		add("SQL Server", "Databases", "Log Cache Reads/sec", "Reads performed per second through the log manager cache.");
		add("SQL Server", "Databases", "Log File(s) Size (KB)", "Cumulative size (in kilobytes) of all the transaction log files in the database.");
		add("SQL Server", "Databases", "Log File(s) Used Size (KB)", "The cumulative used size of all the log files in the database.");
		add("SQL Server", "Databases", "Log Flush Wait Time", "Total wait time (in milliseconds) to flush the log. On an Always On secondary database, this value indicates the wait time for log records to be hardened to disk.");
		add("SQL Server", "Databases", "Log Flush Waits/sec", "Number of commits per second waiting for the log flush.");
		add("SQL Server", "Databases", "Log Flush Write Time (ms)", "Time in milliseconds for performing writes of log flushes that were completed in the last second.");
		add("SQL Server", "Databases", "Log Flushes/sec", "Number of log flushes per second.");
		add("SQL Server", "Databases", "Log Growths", "Total number of times the transaction log for the database has been expanded.");
		add("SQL Server", "Databases", "Log Pool Cache Misses/sec", "Number of requests for which the log block was not available in the log pool. The�log pool�is an in-memory cache of the transaction log. This cache is used to optimize reading the log for recovery, transaction replication, rollback, and Always On availability groups.");
		add("SQL Server", "Databases", "Log Pool Disk Reads/sec", "Number of disk reads that the log pool issued to fetch log blocks.");
		add("SQL Server", "Databases", "Log Pool Hash Deletes/sec", "Rate of raw hash entry deletes from the Log Pool.");
		add("SQL Server", "Databases", "Log Pool Hash Inserts/sec", "Rate of raw hash entry inserts into the Log Pool.");
		add("SQL Server", "Databases", "Log Pool Invalid Hash Entry/sec", "Rate of hash lookups failing due to being invalid.");
		add("SQL Server", "Databases", "Log Pool Log Scan Pushes/sec", "Rate of Log block pushes by log scans, which may come from disk or memory.");
		add("SQL Server", "Databases", "Log Pool LogWriter Pushes/sec", "Rate of Log block pushes by log writer thread.");
		add("SQL Server", "Databases", "Log Pool Push Empty FreePool/sec", "Rate of Log block push fails due to empty free pool.");
		add("SQL Server", "Databases", "Log Pool Push Low Memory/sec", "Rate of Log block push fails due to being low on memory.");
		add("SQL Server", "Databases", "Log Pool Push No Free Buffer/sec", "Rate of Log block push fails due to free buffer unavailable.");
		add("SQL Server", "Databases", "Log Pool Req. Behind Trunc/sec", "Log pool cache misses due to block requested being behind truncation LSN.");
		add("SQL Server", "Databases", "Log Pool Requests Base", "For internal use only.");
		add("SQL Server", "Databases", "Log Pool Requests Old VLF/sec", "Log Pool requests that were not in the last VLF of the log.");
		add("SQL Server", "Databases", "Log Pool Requests/sec", "The number of log-block requests processed by the log pool.");
		add("SQL Server", "Databases", "Log Pool Total Active Log Size", "Current total active log stored in the shared cache buffer manager in bytes.");
		add("SQL Server", "Databases", "Log Pool Total Shared Pool Size", "Current total memory usage of the shared cache buffer manager in bytes.");
		add("SQL Server", "Databases", "Log Shrinks", "Total number of log shrinks for this database.");
		add("SQL Server", "Databases", "Log Truncations", "The number of times the transaction log has been shrunk.");
		add("SQL Server", "Databases", "Percent Log Used", "Percentage of space in the log that is in use.");
		add("SQL Server", "Databases", "Repl. Pending Xacts", "Number of transactions in the transaction log of the publication database marked for replication, but not yet delivered to the distribution database.");
		add("SQL Server", "Databases", "Repl. Trans. Rate", "Number of transactions per second read out of the transaction log of the publication database and delivered to the distribution database.");
		add("SQL Server", "Databases", "Shrink Data Movement Bytes/sec", "Amount of data being moved per second by autoshrink operations, or DBCC SHRINKDATABASE or DBCC SHRINKFILE statements.");
		add("SQL Server", "Databases", "Tracked transactions/sec", "Number of committed transactions recorded in the commit table for the database.");
		add("SQL Server", "Databases", "Transactions/sec", "Number of transactions started for the database per second.<br>Transactions/sec�does not count XTP-only transactions (transactions started by a natively compiled stored procedure)..");
		add("SQL Server", "Databases", "Write Transactions/sec", "Number of transactions that wrote to the database and committed, in the last second.");
		add("SQL Server", "Databases", "XTP Controller DLC Latency Base", "For internal use only.");
		add("SQL Server", "Databases", "XTP Controller DLC Latency/Fetch", "Average latency in microseconds between log blocks entering the Direct Log Consumer and being retrieved by the XTP controller, per second.");
		add("SQL Server", "Databases", "XTP Controller DLC Peak Latency", "The largest recorded latency, in microseconds, of a fetch from the Direct Log Consumer by the XTP controller.");
		add("SQL Server", "Databases", "XTP Controller Log Processed/sec", "The amount of log bytes processed by the XTP controller thread, per second.");
		add("SQL Server", "Databases", "XTP Memory Used (KB)", "The amount of memory used by XTP in the database.");

		add("SQL Server", "Deprecated Features", "'#' and '##' as the name of temporary tables and stored procedures", "An identifier was encountered that did not contain any characters other than #. Use at least one additional character. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "'::' function calling syntax", "The :: function calling syntax was encountered for a table-valued function. Replace with�SELECT column_list FROM�< function_name>(). For example, replace�SELECT * FROM ::fn_virtualfilestats(2,1)with�SELECT * FROM sys.fn_virtualfilestats(2,1). Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "'@' and names that start with '@@' as Transact-SQL identifiers", "An identifier was encountered that began with @ or @@. Do not use @ or @@ or names that begin with @@ as identifiers. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "ADDING TAPE DEVICE", "The deprecated feature sp_addumpdevice'tape' was encountered. Use sp_addumpdevice'disk' instead. Occurs once per use.");
		add("SQL Server", "Deprecated Features", "ALL Permission", "Total number of times the GRANT ALL, DENY ALL, or REVOKE ALL syntax was encountered. Modify the syntax to deny specific permissions. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "ALTER DATABASE WITH TORN_PAGE_DETECTION", "Total number of times the deprecated feature TORN_PAGE_DETECTION option of ALTER DATABASE has been used since the server instance was started. Use the PAGE_VERIFY syntax instead. Occurs once per use in a DDL statement.");
		add("SQL Server", "Deprecated Features", "ALTER LOGIN WITH SET CREDENTIAL", "The deprecated feature syntax ALTER LOGIN WITH SET CREDENTIAL or ALTER LOGIN WITH NO CREDENTIAL was encountered. Use ADD or DROP CREDENTIAL syntax instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "Azeri_Cyrilllic_90", "Event occurs once per database start and once per collation use. Plan to modify applications that use this collation.");
		add("SQL Server", "Deprecated Features", "Azeri_Latin_90", "Event occurs once per database start and once per collation use. Plan to modify applications that use this collation.");
		add("SQL Server", "Deprecated Features", "BACKUP DATABASE or LOG TO TAPE", "The deprecated feature BACKUP { DATABASE | LOG } TO TAPE or BACKUP { DATABASE | LOG } TO�device_that_is_a_tape�was encountered.<br>Use BACKUP { DATABASE | LOG } TO DISK or BACKUP { DATABASE | LOG } TO�device_that_is_a_disk, instead. Occurs once per use.");
		add("SQL Server", "Deprecated Features", "BACKUP DATABASE or LOG WITH MEDIAPASSWORD", "The deprecated feature BACKUP DATABASE WITH MEDIAPASSWORD or BACKUP LOG WITH MEDIAPASSWORD was encountered. Do not use WITH MEDIAPASSWORD.");
		add("SQL Server", "Deprecated Features", "BACKUP DATABASE or LOG WITH PASSWORD", "The deprecated feature BACKUP DATABASE WITH PASSWORD or BACKUP LOG WITH PASSWORD was encountered. Do not use WITH PASSWORD.");
		add("SQL Server", "Deprecated Features", "COMPUTE [BY]", "The COMPUTE or COMPUTE BY syntax was encountered. Rewrite the query to use GROUP BY with ROLLUP. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "CREATE FULLTEXT CATLOG IN PATH", "A CREATE FULLTEXT CATLOG statement with the IN PATH clause was encountered. This clause has no effect in this version of SQL Server. Occurs once per use.");
		add("SQL Server", "Deprecated Features", "CREATE TRIGGER WITH APPEND", "A CREATE TRIGGER statement with the WITH APPEND clause was encountered. Re-create the whole trigger instead. Occurs once per use in a DDL statement.");
		add("SQL Server", "Deprecated Features", "CREATE_DROP_DEFAULT", "The CREATE DEFAULT or DROP DEFAULT syntax was encountered. Rewrite the command by using the DEFAULT option of CREATE TABLE or ALTER TABLE. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "CREATE_DROP_RULE", "The CREATE RULE syntax was encountered. Rewrite the command by using constraints. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "Data types: text ntext or image", "A�text,�ntext, or�image�data types was encountered. Rewrite applications to use the�varchar(max)�data type and removed�text,�ntext, and�image�data type syntax. Occurs once per query.<br>The total number of times a database was changed to compatibility level 80. Plan to upgrade the database and application before the next release. Also occurs when a database at compatibility level 80 is started.");
		add("SQL Server", "Deprecated Features", "Database compatibility level 100, 110. 120", "The total number of times a database compatibility level was changed. Plan to upgrade the database and application for a future release. Also occurs when a database at a deprecated compatibility level is started.");
		add("SQL Server", "Deprecated Features", "DATABASE_MIRRORING", "References to database mirroring feature were encountered. Plan to upgrade to Always On Availability Groups, or if you are running an edition of SQL Server that does not support Always On Availability Groups, plan to migrate to log shipping.");
		add("SQL Server", "Deprecated Features", "database_principal_aliases", "References to the deprecated sys.database_principal_aliases were encountered. Use roles instead of aliases. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "DATABASEPROPERTY", "A statement referenced DATABASEPROPERTY. Update the statement DATABASEPROPERTY to DATABASEPROPERTYEX. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "DATABASEPROPERTYEX('IsFullTextEnabled')", "A statement referenced the DATABASEPROPERTYEX IsFullTextEnabled property. The value of this property has no effect. User databases are always enabled for full-text search. Do not use this property. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "DBCC [UN]PINTABLE", "The DBCC PINTABLE or DBCC UNPINTABLE statement was encountered. This statement has no effect and should be removed. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "DBCC DBREINDEX", "The DBCC DBREINDEX statement was encountered. Rewrite the statement to use the REBUILD option of ALTER INDEX. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "DBCC INDEXDEFRAG", "The DBCC INDEXDEFRAG statement was encountered. Rewrite the statement to use the REORGANIZE option of ALTER INDEX. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "DBCC SHOWCONTIG", "The DBCC SHOWCONTIG statement was encountered. Query sys.dm_db_index_physical_stats for this information. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "DEFAULT keyword as a default value", "Syntax that uses the DEFAULT keyword as a default value was encountered. Do not use. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "Deprecated encryption algorithm", "Deprecated encryption algorithm rc4 will be removed in the next version of SQL Server. Avoid using this feature in new development work, and plan to modify applications that currently use it. The RC4 algorithm is weak and is only supported for backward compatibility. New material can only be encrypted using RC4 or RC4_128 when the database is in compatibility level 90 or 100. (Not recommended.) Use a newer algorithm such as one of the AES algorithms instead. In SQL Server 2012 and higher material encrypted using RC4 or RC4_128 can be unencrypted in any compatibility level.");
		add("SQL Server", "Deprecated Features", "Deprecated hash algorithm", "Use of the MD2, MD4, MD5, SHA, or SHA1 algorithms.");
		add("SQL Server", "Deprecated Features", "DESX algorithm", "Syntax that uses the DESX encryption algorithm was encountered. Use another algorithm for encryption. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "dm_fts_active_catalogs", "The dm_fts_active_catalogs counter always remains at 0 because some columns of the sys.dm_fts_active_catalogs view are not deprecated. To monitor a deprecated column, use the column-specific counter; for example, dm_fts_active_catalogs.is_paused.");
		add("SQL Server", "Deprecated Features", "dm_fts_active_catalogs.is_paused", "The is_paused column of the�sys.dm_fts_active_catalogs�dynamic management view was encountered. Avoid using this column. Occurs every time the server instance detects a reference to the column.");
		add("SQL Server", "Deprecated Features", "dm_fts_active_catalogs.previous_status", "The previous_status column of the sys.dm_fts_active_catalogs dynamic management view was encountered. Avoid using this column. Occurs every time the server instance detects a reference to the column.");
		add("SQL Server", "Deprecated Features", "dm_fts_active_catalogs.previous_status_description", "The previous_status_description column of the sys.dm_fts_active_catalogs dynamic management view was encountered. Avoid using this column. Occurs every time the server instance detects a reference to the column.");
		add("SQL Server", "Deprecated Features", "dm_fts_active_catalogs.row_count_in_thousands", "The row_count_in_thousands column of the sys.dm_fts_active_catalogs dynamic management view was encountered. Avoid using this column. Occurs every time the server instance detects a reference to the column.");
		add("SQL Server", "Deprecated Features", "dm_fts_active_catalogs.status", "The status column of the sys.dm_fts_active_catalogs dynamic management view was encountered. Avoid using this column. Occurs every time the server instance detects a reference to the column.");
		add("SQL Server", "Deprecated Features", "dm_fts_active_catalogs.status_description", "The status_description column of the sys.dm_fts_active_catalogs dynamic management view was encountered. Avoid using this column. Occurs every time the server instance detects a reference to the column.");
		add("SQL Server", "Deprecated Features", "dm_fts_active_catalogs.worker_count", "The worker_count column of the sys.dm_fts_active_catalogs dynamic management view was encountered. Avoid using this column. Occurs every time the server instance detects a reference to the column.");
		add("SQL Server", "Deprecated Features", "dm_fts_memory_buffers", "The dm_fts_memory_buffers counter always remains at 0 because most columns of the sys.dm_fts_memory_buffers view are not deprecated. To monitor the deprecated column, use the column-specific counter: dm_fts_memory_buffers.row_count.");
		add("SQL Server", "Deprecated Features", "dm_fts_memory_buffers.row_count", "The row_count column of the�sys.dm_fts_memory_buffers�dynamic management view was encountered. Avoid using this column. Occurs every time the server instance detects a reference to the column.");
		add("SQL Server", "Deprecated Features", "DROP INDEX with two-part name", "The DROP INDEX syntax contained the format�table_name.index_name�syntax in DROP INDEX. Replace with�index_name�ON�table_name�syntax in the DROP INDEX statement. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "EXT_CREATE_ALTER_SOAP_ENDPOINT", "The CREATE or ALTER ENDPOINT statement with the FOR SOAP option was encountered. Native XML Web Services is deprecated. Use Windows Communications Foundation (WCF) or ASP.NET instead.");
		add("SQL Server", "Deprecated Features", "EXT_endpoint_webmethods", "sys.endpoint_webmethods was encountered. Native XML Web Services is deprecated. Use Windows Communications Foundation (WCF) or ASP.NET instead.");
		add("SQL Server", "Deprecated Features", "EXT_soap_endpoints", "sys.soap_endpoints was encountered. Native XML Web Services is deprecated. Use Windows Communications Foundation (WCF) or ASP.NET instead.");
		add("SQL Server", "Deprecated Features", "EXTPROP_LEVEL0TYPE", "TYPE was encountered at a level0type. Use SCHEMA as the level0type, and TYPE as the level1type. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "EXTPROP_LEVEL0USER", "A level0type USER when a level1type was also specified. Use USER only as a level0type for extended properties directly on a user. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "FASTFIRSTROW", "The FASTFIRSTROW syntax was encountered. Rewrite statements to use the OPTION (FAST�n) syntax. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "FILE_ID", "The FILE_ID syntax was encountered. Rewrite statements to use FILE_IDEX. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "fn_get_sql", "The fn_get_sql function was compiled. Use sys.dm_exec_sql_text instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "fn_servershareddrives", "The fn_servershareddrives function was compiled. Use sys.dm_io_cluster_shared_drives instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "fn_virtualservernodes", "The fn_virtualservernodes function was compiled. Use sys.dm_os_cluster_nodes instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "fulltext_catalogs", "The fulltext_catalogs counter always remains at 0 because some columns of the sys.fulltext_catalogs view are not deprecated. To monitor a deprecated column, use its column-specific counter; for example, fulltext_catalogs.data_space_id. Occurs every time the server instance detects a reference to the column.");
		add("SQL Server", "Deprecated Features", "fulltext_catalogs.data_space_id", "The data_space_id column of the�sys.fulltext_catalogs�catalog view was encountered. Do not use this column. Occurs every time the server instance detects a reference to the column.");
		add("SQL Server", "Deprecated Features", "fulltext_catalogs.file_id", "The file_id column of the sys.fulltext_catalogs catalog view was encountered. Do not use this column. Occurs every time the server instance detects a reference to the column.");
		add("SQL Server", "Deprecated Features", "fulltext_catalogs.path", "The path column of the sys.fulltext_catalogs catalog view was encountered. Do not use this column. Occurs every time the server instance detects a reference to the column.");
		add("SQL Server", "Deprecated Features", "FULLTEXTCATALOGPROPERTY('LogSize')", "The LogSize property of the FULLTEXTCATALOGPROPERTY function was encountered. Avoid using this property.");
		add("SQL Server", "Deprecated Features", "FULLTEXTCATALOGPROPERTY('PopulateStatus')", "The PopulateStatus property of the FULLTEXTCATALOGPROPERTY function was encountered. Avoid using this property.");
		add("SQL Server", "Deprecated Features", "FULLTEXTSERVICEPROPERTY('ConnectTimeout')", "The ConnectTimeout property of the FULLTEXTSERVICEPROPERTY function was encountered. Avoid using this property.");
		add("SQL Server", "Deprecated Features", "FULLTEXTSERVICEPROPERTY('DataTimeout')", "The DataTimeout property of the FULLTEXTSERVICEPROPERTY function was encountered. Avoid using this property.");
		add("SQL Server", "Deprecated Features", "FULLTEXTSERVICEPROPERTY('ResourceUsage')", "The ResourceUsage property of the FULLTEXTSERVICEPROPERTY function was encountered. Avoid using this property.");
		add("SQL Server", "Deprecated Features", "GROUP BY ALL", "Total number of times the GROUP BY ALL syntax was encountered. Modify the syntax to group by specific tables.");
		add("SQL Server", "Deprecated Features", "Hindi", "Event occurs once per database start and once per collation use. Plan to modify applications that use this collation. Use Indic_General_90 instead.");
		add("SQL Server", "Deprecated Features", "HOLDLOCK table hint without parentheses", "");
		add("SQL Server", "Deprecated Features", "IDENTITYCOL", "The INDENTITYCOL syntax was encountered. Rewrite statements to use the $identity syntax. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "Index view select list without COUNT_BIG(*)", "The select list of an aggregate indexed view must contain COUNT_BIG (*) .");
		add("SQL Server", "Deprecated Features", "INDEX_OPTION", "Encountered CREATE TABLE, ALTER TABLE, or CREATE INDEX syntax without parentheses around the options. Rewrite the statement to use the current syntax. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "INDEXKEY_PROPERTY", "The INDEXKEY_PROPERTY syntax was encountered. Rewrite statements to query sys.index_columns. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "Indirect TVF hints", "The indirect application, through a view, of table hints to an invocation of a multistatement table-valued function (TVF) will be removed in a future version of SQL Server.");
		add("SQL Server", "Deprecated Features", "INSERT NULL into TIMESTAMP columns", "A NULL value was inserted to a TIMESTAMP column. Use a default value instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "INSERT_HINTS", "");
		add("SQL Server", "Deprecated Features", "Korean_Wansung_Unicode", "Event occurs once per database start and once per collation use. Plan to modify applications that use this collation.");
		add("SQL Server", "Deprecated Features", "Lithuanian_Classic", "Event occurs once per database start and once per collation use. Plan to modify applications that use this collation.");
		add("SQL Server", "Deprecated Features", "Macedonian", "Event occurs once per database start and once per collation use. Plan to modify applications that use this collation. Use Macedonian_FYROM_90 instead.");
		add("SQL Server", "Deprecated Features", "MODIFY FILEGROUP READONLY", "The MODIFY FILEGROUP READONLY syntax was encountered. Rewrite statements to use the READ_ONLY syntax. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "MODIFY FILEGROUP READWRITE", "The MODIFY FILEGROUP READWRITE syntax was encountered. Rewrite statements to use the READ_WRITE syntax. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "More than two-part column name", "A query used a 3-part or 4-part name in the column list. Change the query to use the standard-compliant 2-part names. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "Multiple table hints without comma", "A space was used as the separator between table hints. Use a comma instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "NOLOCK or READUNCOMMITTED in UPDATE or DELETE", "NOLOCK or READUNCOMMITTED was encountered in the FROM clause of an UPDATE or DELETE statement. Remove the NOLOCK or READUNCOMMITTED table hints from the FROM clause.");
		add("SQL Server", "Deprecated Features", "Non-ANSI *= or =* outer join operators", "A statement that uses the *= or =* join syntax was encountered. Rewrite the statement to use the ANSI join syntax. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "numbered_stored_procedures", "");
		add("SQL Server", "Deprecated Features", "numbered_procedure_parameters", "References to the deprecated sys.numbered_procedure_parameters were encountered. Do not use. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "numbered_procedures", "References to the deprecated sys.numbered_procedures were encountered. Do not use. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "Oldstyle RAISEERROR", "The deprecated RAISERROR (Format: RAISERROR integer string) syntax was encountered. Rewrite the statement using the current RAISERROR syntax. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "OLEDB for ad hoc connections", "SQLOLEDB is not a supported provider. Use SQL Server Native Client for ad hoc connections.");
		add("SQL Server", "Deprecated Features", "PERMISSIONS", "References to the PERMISSIONS intrinsic function were encountered. Query sys.fn_my_permissions instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "ProcNums", "The deprecated ProcNums syntax was encountered. Rewrite statements to remove the references. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "READTEXT", "The READTEXT syntax was encountered. Rewrite applications to use the�varchar(max)�data type and removed�text�data type syntax. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "RESTORE DATABASE or LOG WITH DBO_ONLY", "The RESTORE � WITH DBO_ONLY syntax was encountered. Use RESTORE � RESTRICTED_USER instead.");
		add("SQL Server", "Deprecated Features", "RESTORE DATABASE or LOG WITH MEDIAPASSWORD", "The RESTORE � WITH MEDIAPASSWORD syntax was encountered. WITH MEDIAPASSWORD provides weak security and should be removed.");
		add("SQL Server", "Deprecated Features", "RESTORE DATABASE or LOG WITH PASSWORD", "The RESTORE � WITH PASSWORD syntax was encountered. WITH PASSWORD provides weak security and should be removed.");
		add("SQL Server", "Deprecated Features", "Returning results from trigger", "This event occurs once per trigger invocation. Rewrite the trigger so that it does not return result sets.");
		add("SQL Server", "Deprecated Features", "ROWGUIDCOL", "The ROWGUIDCOL syntax was encountered. Rewrite statements to use the $rowguid syntax. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "SET ANSI_NULLS OFF", "The SET ANSI_NULLS OFF syntax was encountered. Remove this deprecated syntax. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "SET ANSI_PADDING OFF", "The SET ANSI_PADDING OFF syntax was encountered. Remove this deprecated syntax. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "SET CONCAT_NULL_YIELDS_NULL OFF", "The SET CONCAT_NULL_YIELDS_NULL OFF syntax was encountered. Remove this deprecated syntax. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "SET DISABLE_DEF_CNST_CHK", "The SET DISABLE_DEF_CNST_CHK syntax was encountered. This has no effect. Remove this deprecated syntax. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "SET FMTONLY ON", "The SET FMTONLY syntax was encountered. Remove this deprecated syntax. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "SET OFFSETS", "The SET OFFSETS syntax was encountered. Remove this deprecated syntax. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "SET REMOTE_PROC_TRANSACTIONS", "The SET REMOTE_PROC_TRANSACTIONS syntax was encountered. Remove this deprecated syntax. Use linked servers and sp_serveroption instead.");
		add("SQL Server", "Deprecated Features", "SET ROWCOUNT", "The SET ROWCOUNT syntax was encountered in a DELETE, INSERT, or UPDATE statement. Rewrite the statement by using TOP. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "SETUSER", "The SET USER statement was encountered. Use EXECUTE AS instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_addapprole", "The sp_addapprole procedure was encountered. Use CREATE APPLICATION ROLE instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_addextendedproc", "The sp_addextendedproc procedure was encountered. Use CLR instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sp_addlogin", "The sp_addlogin procedure was encountered. Use CREATE LOGIN instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_addremotelogin", "The sp_addremotelogin procedure was encountered. Use linked servers instead.");
		add("SQL Server", "Deprecated Features", "sp_addrole", "The sp_addrole procedure was encountered. Use CREATE ROLE instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_addserver", "The sp_addserver procedure was encountered. Use linked servers instead.");
		add("SQL Server", "Deprecated Features", "sp_addtype", "The sp_addtype procedure was encountered. Use CREATE TYPE instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sp_adduser", "The sp_adduser procedure was encountered. Use CREATE USER instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_approlepassword", "The sp_approlepassword procedure was encountered. Use ALTER APPLICATION ROLE instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_attach_db", "The sp_attach_db procedure was encountered. Use CREATE DATABASE FOR ATTACH instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_attach_single_file_db", "The sp_single_file_db procedure was encountered. Use CREATE DATABASE FOR ATTACH_REBUILD_LOG instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_bindefault", "The sp_bindefault procedure was encountered. Use the DEFAULT keyword of ALTER TABLE or CREATE TABLE instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sp_bindrule", "The sp_bindrule procedure was encountered. Use check constraints instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sp_bindsession", "The sp_bindsession procedure was encountered. Use Multiple Active Result Sets (MARS) or distributed transactions instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sp_certify_removable", "The sp_certify_removable procedure was encountered. Use sp_detach_db instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_changeobjectowner", "The sp_changeobjectowner procedure was encountered. Use ALTER SCHEMA or ALTER AUTHORIZATION instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_change_users_login", "The sp_change_users_login procedure was encountered. Use ALTER USER instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_configure 'allow updates'", "The allow updates option of sp_configure was encountered. System tables are no longer updatable. Do not use. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_configure 'disallow results from triggers'", "The disallow result sets from triggers option of sp_configure was encountered. To disallow result sets from triggers, use sp_configure to set the option to 1. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_configure 'ft crawl bandwidth (max)'", "The ft crawl bandwidth (max) option of sp_configure was encountered. Do not use. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_configure 'ft crawl bandwidth (min)'", "The ft crawl bandwidth (min) option of sp_configure was encountered. Do not use. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_configure 'ft notify bandwidth (max)'", "The ft notify bandwidth (max) option of sp_configure was encountered. Do not use. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_configure 'ft notify bandwidth (min)'", "The ft notify bandwidth (min) option of sp_configure was encountered. Do not use. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_configure 'locks'", "The locks option of sp_configure was encountered. Locks are no longer configurable. Do not use. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_configure 'open objects'", "The open objects option of sp_configure was encountered. The number of open objects is no longer configurable. Do not use. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_configure 'priority boost'", "The priority boost option of sp_configure was encountered. Do not use. Occurs once per query. Use the Windows start /high � program.exe option instead.");
		add("SQL Server", "Deprecated Features", "sp_configure 'remote proc trans'", "The remote proc trans option of sp_configure was encountered. Do not use. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_configure 'set working set size'", "The set working set size option of sp_configure was encountered. The working set size is no longer configurable. Do not use. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_control_dbmasterkey_password", "The sp_control_dbmasterkey_password stored procedure does not check whether a master key exists. This is permitted for backward compatibility, but displays a warning. This behavior is deprecated. In a future release the master key must exist and the password used in the stored procedure sp_control_dbmasterkey_password must be the same password as one of the passwords used to encrypt the database master key.");
		add("SQL Server", "Deprecated Features", "sp_create_removable", "The sp_create_removable procedure was encountered. Use CREATE DATABASE instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_db_vardecimal_storage_format", "Use of�vardecimal�storage format was encountered. Use data compression instead.");
		add("SQL Server", "Deprecated Features", "sp_dbcmptlevel", "The sp_dbcmptlevel procedure was encountered. Use ALTER DATABASE � SET COMPATIBILITY_LEVEL instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_dbfixedrolepermission", "The sp_dbfixedrolepermission procedure was encountered. Do not use. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_dboption", "The sp_dboption procedure was encountered. Use ALTER DATABASE and DATABASEPROPERTYEX instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sp_dbremove", "The sp_dbremove procedure was encountered. Use DROP DATABASE instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_defaultdb", "The sp_defaultdb procedure was encountered. Use ALTER LOGIN instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sp_defaultlanguage", "The sp_defaultlanguage procedure was encountered. Use ALTER LOGIN instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sp_denylogin", "The sp_denylogin procedure was encountered. Use ALTER LOGIN DISABLE instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_depends", "The sp_depends procedure was encountered. Use sys.dm_sql_referencing_entities and sys.dm_sql_referenced_entities instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_detach_db @keepfulltextindexfile", "The @keepfulltextindexfile argument was encountered in a sp_detach_db statement. Do not use this argument.");
		add("SQL Server", "Deprecated Features", "sp_dropalias", "The sp_dropalias procedure was encountered. Replace aliases with a combination of user accounts and database roles. Use sp_dropalias to remove aliases in upgraded databases. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sp_dropapprole", "The sp_dropapprole procedure was encountered. Use DROP APPLICATION ROLE instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_dropextendedproc", "The sp_dropextendedproc procedure was encountered. Use CLR instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sp_droplogin", "The sp_droplogin procedure was encountered. Use DROP LOGIN instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_dropremotelogin", "The sp_dropremotelogin procedure was encountered. Use linked servers instead.");
		add("SQL Server", "Deprecated Features", "sp_droprole", "The sp_droprole procedure was encountered. Use DROP ROLE instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_droptype", "The sp_droptype procedure was encountered. Use DROP TYPE instead.");
		add("SQL Server", "Deprecated Features", "sp_dropuser", "The sp_dropuser procedure was encountered. Use DROP USER instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_estimated_rowsize_reduction_for_vardecimal", "Use of�vardecimal�storage format was encountered. Use data compression and sp_estimate_data_compression_savings instead.");
		add("SQL Server", "Deprecated Features", "sp_fulltext_catalog", "The sp_fulltext_catalog procedure was encountered. Use CREATE/ALTER/DROP FULLTEXT CATALOG instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sp_fulltext_column", "The sp_fulltext_column procedure was encountered. Use ALTER FULLTEXT INDEX instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sp_fulltext_database", "The sp_fulltext_database procedure was encountered. Use ALTER DATABASE instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sp_fulltext_service @action=clean_up", "The clean_up option of the sp_fulltext_service procedure was encountered. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_fulltext_service @action=connect_timeout", "The connect_timeout option of the sp_fulltext_service procedure was encountered. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_fulltext_service @action=data_timeout", "The data_timeout option of the sp_fulltext_service procedure was encountered. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_fulltext_service @action=resource_usage", "The resource_usage option of the sp_fulltext_service procedure was encountered. This option has no function. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_fulltext_table", "The sp_fulltext_table procedure was encountered. Use CREATE/ALTER/DROP FULLTEXT INDEX instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sp_getbindtoken", "The sp_getbindtoken procedure was encountered. Use Multiple Active Result Sets (MARS) or distributed transactions instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sp_grantdbaccess", "The sp_grantdbaccess procedure was encountered. Use CREATE USER instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_grantlogin", "The sp_grantlogin procedure was encountered. Use CREATE LOGIN instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_help_fulltext_catalog_components", "The sp_help_fulltext_catalog_components procedure was encountered. This procedure returns empty rows. Do not use this procedure. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sp_help_fulltext_catalogs", "The sp_help_fulltext_catalogs procedure was encountered. Query sys.fulltext_catalogs instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sp_help_fulltext_catalogs_cursor", "The sp_help_fulltext_catalogs_cursor procedure was encountered. Query sys.fulltext_catalogs instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sp_help_fulltext_columns", "The sp_help_fulltext_columns procedure was encountered. Query sys.fulltext_index_columns instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sp_help_fulltext_columns_cursor", "The sp_help_fulltext_columns_cursor procedure was encountered. Query sys.fulltext_index_columns instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sp_help_fulltext_tables", "The sp_help_fulltext_tables procedure was encountered. Query sys.fulltext_indexes instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sp_help_fulltext_tables_cursor", "The sp_help_fulltext_tables_cursor procedure was encountered. Query sys.fulltext_indexes instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sp_helpdevice", "The sp_helpdevice procedure was encountered. Query sys.backup_devices instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_helpextendedproc", "The sp_helpextendedproc procedure was encountered. Use CLR instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sp_helpremotelogin", "The sp_helpremotelogin procedure was encountered. Use linked servers instead.");
		add("SQL Server", "Deprecated Features", "sp_indexoption", "The sp_indexoption procedure was encountered. Use ALTER INDEX instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sp_lock", "The sp_lock procedure was encountered. Query sys.dm_tran_locks instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_password", "The sp_password procedure was encountered. Use ALTER LOGIN instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_remoteoption", "The sp_remoteoption procedure was encountered. Use linked servers instead.");
		add("SQL Server", "Deprecated Features", "sp_renamedb", "The sp_renamedb procedure was encountered. Use ALTER DATABASE instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_resetstatus", "The sp_resetstatus procedure was encountered. Use ALTER DATABASE instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_revokedbaccess", "The sp_revokedbaccess procedure was encountered. Use DROP USER instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_revokelogin", "The sp_revokelogin procedure was encountered. Use DROP LOGIN instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_srvrolepermission", "The deprecated sp_srvrolepermission procedure was encountered. Do not use. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "sp_unbindefault", "The sp_unbindefault procedure was encountered. Use the DEFAULT keyword in CREATE TABLE or ALTER TABLE statements instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sp_unbindrule", "The sp_unbindrule procedure was encountered. Use check constraints instead of rules. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "SQL_AltDiction_CP1253_CS_AS", "Event occurs once per database start and once per collation use. Plan to modify applications that use this collation.");
		add("SQL Server", "Deprecated Features", "String literals as column aliases", "Syntax that contains a string that is used as a column alias in a SELECT statement, such as�'string' = expression, was encountered. Do not use. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sys.sql_dependencies", "References to sys.sql_dependencies were encountered. Use sys.sql_expression_dependencies instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sysaltfiles", "References to sysaltfiles were encountered. Use sys.master_files instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "syscacheobjects", "References to syscacheobjects were encountered. Use sys.dm_exec_cached_plans, sys.dm_exec_plan_attributes, and sys.dm_exec_sql_text instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "syscolumns", "References to syscolumns were encountered. Use sys.columns instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "syscomments", "References to syscomments were encountered. Use sys.sql_modules instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sysconfigures", "References to the sysconfigures table were encountered. Reference the sys.sysconfigures view instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sysconstraints", "References to sysconstraints were encountered Use sys.check_constraints, sys.default_constraints, sys.key_constraints, sys.foreign_keys instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "syscurconfigs", "References to syscurconfigs were encountered. Use sys.configurations instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sysdatabases", "References to sysdatabases were encountered. Use sys.databases instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sysdepends", "References to sysdepends were encountered. Use sys.sql_dependencies instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sysdevices", "References to sysdevices were encountered. Use sys.backup_devices instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sysfilegroups", "References to sysfilegroups were encountered. Use sys.filegroups instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sysfiles", "References to sysfiles were encountered. Use sys.database_files instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sysforeignkeys", "References to sysforeignkeys were encountered. Use sys.foreign_keys instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sysfulltextcatalogs", "References to sysfulltextcatalogs were encountered. Use sys.fulltext_catalogs instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sysindexes", "References to sysindexes were encountered. Use sys.indexes, sys.partitions, sys.allocation_units, and sys.dm_db_partition_stats instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sysindexkeys", "References to sysindexkeys were encountered. Use sys.index_columns instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "syslockinfo", "References to syslockinfo were encountered. Use sys.dm_tran_locks instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "syslogins", "References to syslogins were encountered. Use sys.server_principals and sys.sql_logins instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sysmembers", "References to sysmembers were encountered. Use sys.database_role_members instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sysmessages", "References to sysmessages were encountered. Use sys.messages instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sysobjects", "References to sysobjects were encountered. Use sys.objects instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sysoledbusers", "References to sysoledbusers were encountered. Use sys.linked_logins instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sysopentapes", "References to sysopentapes were encountered. Use sys.dm_io_backup_tapes instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sysperfinfo", "References to sysperfinfo were encountered. Use sys.dm_os_performance_counters. instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "syspermissions", "References to syspermissions were encountered. Use sys.database_permissions and sys.server_permissions instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sysprocesses", "References to sysprocesses were encountered. Use sys.dm_exec_connections, sys.dm_exec_sessions, and sys.dm_exec_requests instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sysprotects", "References to sysprotects were encountered. Use sys.database_permissions and sys.server_permissions instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sysreferences", "References to sysreferences were encountered. Use sys.foreign_keys instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sysremotelogins", "References to sysremotelogins were encountered. Use sys.remote_logins instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sysservers", "References to sysservers were encountered. Use sys.servers instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "systypes", "References to systypes were encountered. Use sys.types instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "sysusers", "References to sysusers were encountered. Use sys.database_principals instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "Table hint without WITH", "A statement that used table hints but did not use the WITH keyword was encountered. Modify statements to include the word WITH. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "Text in row table option", "References to the 'text in row' table option were encountered. Use sp_tableoption 'large value types out of row' instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "TEXTPTR", "References to the TEXTPTR function were encountered. Rewrite applications to use the�varchar(max)�data type and removed�text,�ntext, and�image�data type syntax. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "TEXTVALID", "References to the TEXTVALID function were encountered. Rewrite applications to use the�varchar(max)�data type and removed�text,�ntext, and�image�data type syntax. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "TIMESTAMP", "Total number of times the deprecated�timestamp�data type was encountered in a DDL statement. Use the�rowversion�data type instead.");
		add("SQL Server", "Deprecated Features", "UPDATETEXT or WRITETEXT", "The UPDATETEXT or WRITETEXT statement was encountered. Rewrite applications to use the�varchar(max)�data type and removed�text,�ntext, and�image�data type syntax. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "USER_ID", "References to the USER_ID function were encountered. Use the DATABASE_PRINCIPAL_ID function instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "Using OLEDB for linked servers", "");
		add("SQL Server", "Deprecated Features", "Vardecimal storage format", "Use of�vardecimal�storage format was encountered. Use data compression instead.");
		add("SQL Server", "Deprecated Features", "XMLDATA", "The FOR XML syntax was encountered. Use XSD generation for RAW and AUTO modes. There is no replacement for the explicit mode. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "XP_API", "An extended stored procedure statement was encountered. Do not use.");
		add("SQL Server", "Deprecated Features", "xp_grantlogin", "The xp_grantlogin procedure was encountered. Use CREATE LOGIN instead. Occurs once per compilation.");
		add("SQL Server", "Deprecated Features", "xp_loginconfig", "The xp_loginconfig procedure was encountered. Use the IsIntegratedSecurityOnly argument of SERVERPROPERTY instead. Occurs once per query.");
		add("SQL Server", "Deprecated Features", "xp_revokelogin", "The xp_revokelogin procedure was encountered. Use ALTER LOGIN DISABLE or DROP LOGIN instead. Occurs once per compilation.");

		add("SQL Server", "ExecStatistics", "Distributed Query", "Statistics relevant to execution of distributed queries.");
		add("SQL Server", "ExecStatistics", "DTC calls", "Statistics relevant to execution of DTC calls.");
		add("SQL Server", "ExecStatistics", "Extended Procedures", "Statistics relevant to execution of extended procedures.");
		add("SQL Server", "ExecStatistics", "OLEDB calls", "Statistics relevant to execution of OLEDB calls.");

		add("SQL Server", "External Scripts", "Execution Errors", "The number of errors in executing external scripts.");
		add("SQL Server", "External Scripts", "Implied Auth. Logins", "The number of logins from satellite processes authenticated by using implied authentication.");
		add("SQL Server", "External Scripts", "Parallel Executions", "The number of external scripts executed with @parallel = 1.");
		add("SQL Server", "External Scripts", "SQL CC Executions", "The number of external scripts executed using SQL Compute Context.");
		add("SQL Server", "External Scripts", "Streaming Executions", "The number of external scripts executed with the @r_rowsPerRead parameter.");
		add("SQL Server", "External Scripts", "Total Execution Time (ms)", "The total time spent in executing external scripts.");
		add("SQL Server", "External Scripts", "Total Executions", "The number of external scripts executed.");

		add("SQL Server", "FileTable", "Avg time delete FileTable item", "Average time (in milliseconds) taken to delete a FileTable item.");
		add("SQL Server", "FileTable", "Avg time FileTable enumeration", "Average time (in milliseconds) taken for a FileTable enumeration request.");
		add("SQL Server", "FileTable", "Avg time FileTable handle kill", "Average time (in milliseconds) taken to kill a FileTable handle.");
		add("SQL Server", "FileTable", "Avg time move FileTable item", "Average time (in milliseconds) taken to move a FileTable item.");
		add("SQL Server", "FileTable", "Avg time per file I/O request", "Average time (in milliseconds) spent handling an incoming file I/O request.");
		add("SQL Server", "FileTable", "Avg time per file I/O response", "Average time (in milliseconds) spent handling an outgoing file I/O response.");
		add("SQL Server", "FileTable", "Avg time rename FileTable item", "Average time (in milliseconds) taken to rename a FileTable item.");
		add("SQL Server", "FileTable", "Avg time to get FileTable item", "Average time (in milliseconds) taken to retrieve a FileTable item.");
		add("SQL Server", "FileTable", "Avg time update FileTable item", "Average time (in milliseconds) taken to update a FileTable item.");
		add("SQL Server", "FileTable", "FileTable db operations/sec", "Total number of database operational events processed by the FileTable store component per second.");
		add("SQL Server", "FileTable", "FileTable enumeration reqs/sec", "Total number of FileTable enumeration requests per second.");
		add("SQL Server", "FileTable", "FileTable file I/O requests/sec", "Total number of incoming FileTable file I/O requests per second.");
		add("SQL Server", "FileTable", "FileTable file I/O response/sec", "Total number of outgoing file I/O responses per second.");
		add("SQL Server", "FileTable", "FileTable item delete reqs/sec", "Total number of FileTable delete item requests per second.");
		add("SQL Server", "FileTable", "FileTable item get requests/sec", "Total number of FileTable retrieve item requests per second.");
		add("SQL Server", "FileTable", "FileTable item move reqs/sec", "Total number of FileTable move item requests per second.");
		add("SQL Server", "FileTable", "FileTable item rename reqs/sec", "Total number of FileTable rename item requests per second.");
		add("SQL Server", "FileTable", "FileTable item update reqs/sec", "Total number of FileTable update item requests per second.");
		add("SQL Server", "FileTable", "FileTable kill handle ops/sec", "Total number of FileTable handle kill operations per second.");
		add("SQL Server", "FileTable", "FileTable table operations/sec", "Total number of table operational events processed by the FileTable store component per second.");
		add("SQL Server", "FileTable", "Time delete FileTable item BASE", "For internal use only.");
		add("SQL Server", "FileTable", "Time FileTable enumeration BASE", "For internal use only.");
		add("SQL Server", "FileTable", "Time FileTable handle kill BASE", "For internal use only.");
		add("SQL Server", "FileTable", "Time move FileTable item BASE", "For internal use only.");
		add("SQL Server", "FileTable", "Time per file I/O request BASE", "For internal use only.");
		add("SQL Server", "FileTable", "Time per file I/O response BASE", "For internal use only.");
		add("SQL Server", "FileTable", "Time rename FileTable item BASE", "For internal use only.");
		add("SQL Server", "FileTable", "Time to get FileTable item BASE", "For internal use only.");
		add("SQL Server", "FileTable", "Time update FileTable item BASE", "For internal use only.");

		add("SQL Server", "General Statistics", "Active Temp Tables", "Number of temporary tables/table variables in use.");
		add("SQL Server", "General Statistics", "Connection resets/sec", "Total number of logins started from the connection pool.");
		add("SQL Server", "General Statistics", "Event Notifications Delayed Drop", "Number of event notifications waiting to be dropped by a system thread.");
		add("SQL Server", "General Statistics", "HTTP Authenticated Requests", "Number of authenticated HTTP requests started per second.");
		add("SQL Server", "General Statistics", "Logical Connections", "Number of logical connections to the system.<br>The main purpose of logical connections is to service multiple active result sets (MARS) requests. For MARS requests, every time that an application makes a connection to SQL Server, there may be more than one logical connection that corresponds to a physical connection.<br>When MARS is not used, the ratio between physical and logical connections is 1:1. Therefore, every time that an application makes a connection to SQL Server, logical connections will increase by 1.");
		add("SQL Server", "General Statistics", "Logins/sec", "Total number of logins started per second. This does not include pooled connections.");
		add("SQL Server", "General Statistics", "Logouts/sec", "Total number of logout operations started per second.");
		add("SQL Server", "General Statistics", "Mars Deadlocks", "Number of MARS deadlocks detected.");
		add("SQL Server", "General Statistics", "Non-atomic yield rate", "Number of non-atomic yields per second.");
		add("SQL Server", "General Statistics", "Processes blocked", "Number of currently blocked processes.");
		add("SQL Server", "General Statistics", "SOAP Empty Requests", "Number of empty SOAP requests started per second.");
		add("SQL Server", "General Statistics", "SOAP Method Invocations", "Number of SOAP method invocations started per second.");
		add("SQL Server", "General Statistics", "SOAP Session Initiate Requests", "Number of SOAP Session initiate requests started per second.");
		add("SQL Server", "General Statistics", "SOAP Session Terminate Requests", "Number of SOAP Session terminate requests started per second.");
		add("SQL Server", "General Statistics", "SOAP SQL Requests", "Number of SOAP SQL requests started per second.");
		add("SQL Server", "General Statistics", "SOAP WSDL Requests", "Number of SOAP Web Service Description Language requests started per second.");
		add("SQL Server", "General Statistics", "SQL Trace IO Provider Lock Waits", "Number of waits for the File IO Provider lock per second.");
		add("SQL Server", "General Statistics", "Temp Tables Creation Rate", "Number of temporary tables/table variables created per second.");
		add("SQL Server", "General Statistics", "Temp Tables For Destruction", "Number of temporary tables/table variables waiting to be destroyed by the cleanup system thread.");
		add("SQL Server", "General Statistics", "Tempdb recovery unit id", "Number of duplicate tempdb recovery unit id generated.");
		add("SQL Server", "General Statistics", "Tempdb rowset id", "Number of duplicate tempdb rowset id generated.");
		add("SQL Server", "General Statistics", "Trace Event Notifications Queue", "Number of trace event notification instances waiting in the internal queue to be sent through Service Broker.");
		add("SQL Server", "General Statistics", "Transactions", "Number of transaction enlistments (local, DTC, bound all combined).");
		add("SQL Server", "General Statistics", "User Connections", "Counts the number of users currently connected to SQL Server.");

		add("SQL Server", "HTTP Storage", "Read Bytes/sec", "Amount of data being transferred from the HTTP storage per second during read operations.");
		add("SQL Server", "HTTP Storage", "Write Bytes/sec", "Amount of data being transferred from the HTTP storage per second during write operations.");
		add("SQL Server", "HTTP Storage", "Total Bytes/sec", "Amount of data being transferred from the HTTP storage per second during read or write operations.");
		add("SQL Server", "HTTP Storage", "Reads/sec", "Number of reads per second on the HTTP storage.");
		add("SQL Server", "HTTP Storage", "Writes/sec", "Number of writer per second on the HTTP storage.");
		add("SQL Server", "HTTP Storage", "Transfers/sec", "Number of read and write operations per second on the HTTP storage.");
		add("SQL Server", "HTTP Storage", "Avg. Bytes/Read", "Average number of bytes transferred from the HTTP storage per read.");
		add("SQL Server", "HTTP Storage", "Avg. Bytes/Read BASE", "For internal use only.");
		add("SQL Server", "HTTP Storage", "Avg. Bytes/Transfer", "Average number of bytes transferred from the HTTP storage during read or write operations.");
		add("SQL Server", "HTTP Storage", "Avg. Bytes/Transfer BASE", "For internal use only.");
		add("SQL Server", "HTTP Storage", "Avg. Bytes/Write", "Average number of bytes transferred from the HTTP storage per write.");
		add("SQL Server", "HTTP Storage", "Avg. Bytes/Write BASE", "For internal use only.");
		add("SQL Server", "HTTP Storage", "Avg. microsec/Read", "The average number of microseconds it takes to do each read from the HTTP storage.");
		add("SQL Server", "HTTP Storage", "Avg. microsec/Read BASE", "For internal use only.");
		add("SQL Server", "HTTP Storage", "Avg. microsec/Read Comp", "The average number of microseconds it takes for HTTP to complete the read to storage.");
		add("SQL Server", "HTTP Storage", "Avg. microsec/Read Comp BASE", "For internal use only.");
		add("SQL Server", "HTTP Storage", "Avg. microsec/Write", "The average number of microseconds it takes to do each write to the HTTP storage.");
		add("SQL Server", "HTTP Storage", "Avg. microsec/Transfer", "The average number of microseconds it takes to do each transfer to the HTTP storage.");
		add("SQL Server", "HTTP Storage", "Avg. microsec/Transfer BASE", "For internal use only.");
		add("SQL Server", "HTTP Storage", "Avg. microsec/Write BASE", "For internal use only.");
		add("SQL Server", "HTTP Storage", "Avg. microsec/Write Comp", "The average number of microseconds it takes for HTTP to complete the write to storage.");
		add("SQL Server", "HTTP Storage", "Avg. microsec/Write Comp BASE", "For internal use only.");
		add("SQL Server", "HTTP Storage", "Outstanding HTTP Storage I/O", "The total number of outstanding I/Os towards a HTTP storage.");
		add("SQL Server", "HTTP Storage", "HTTP Storage IO failed/sec", "Number of failed write requests sent to the HTTP storage per second.");
		add("SQL Server", "HTTP Storage", "HTTP Storage I/O Retry/sec", "Number of retry requests sent to the HTTP storage per second.");

		add("SQL Server", "Latches", "Average Latch Wait Time (ms)", "Average latch wait time (in milliseconds) for latch requests that had to wait.");
		add("SQL Server", "Latches", "Average Latch Wait Time Base", "For internal use only.");
		add("SQL Server", "Latches", "Latch Waits/sec", "Number of latch requests that could not be granted immediately.");
		add("SQL Server", "Latches", "Number of SuperLatches", "Number of latches that are currently SuperLatches.");
		add("SQL Server", "Latches", "SuperLatch Demotions/sec", "Number of SuperLatches that have been demoted to regular latches in the last second.");
		add("SQL Server", "Latches", "SuperLatch Promotions/sec", "Number of latches that have been promoted to SuperLatches in the last second.");
		add("SQL Server", "Latches", "Total Latch Wait Time (ms)", "Total latch wait time (in milliseconds) for latch requests in the last second.");

		add("SQL Server", "Locks", "Average Wait Time (ms)", "Average amount of wait time (in milliseconds) for each lock request that resulted in a wait.");
		add("SQL Server", "Locks", "Average Wait Time Base", "For internal use only.");
		add("SQL Server", "Locks", "Lock Requests/sec", "Number of new locks and lock conversions per second requested from the lock manager.");
		add("SQL Server", "Locks", "Lock Timeouts (timeout > 0)/sec", "Number of lock requests per second that timed out, but excluding requests for NOWAIT locks.");
		add("SQL Server", "Locks", "Lock Timeouts/sec", "Number of lock requests per second that timed out, including requests for NOWAIT locks.");
		add("SQL Server", "Locks", "Lock Wait Time (ms)", "Total wait time (in milliseconds) for locks in the last second.");
		add("SQL Server", "Locks", "Lock Waits/sec", "Number of lock requests per second that required the caller to wait.");
		add("SQL Server", "Locks", "Number of Deadlocks/sec", "Number of lock requests per second that resulted in a deadlock.");

		add("SQL Server", "LogPool FreePool", "Free Buffer Refills/sec", "Number of buffers being allocated for refill, per second.");
		add("SQL Server", "LogPool FreePool", "Free List Length", "Length of the free list.");

		add("SQL Server", "Memory Broker Clerks", "Internal benefit", "The internal value of memory for entry count pressure, in ms per page per ms, multiplied by 10 billion and truncated to an integer.");
		add("SQL Server", "Memory Broker Clerks", "Memory broker clerk size", "The size of the the clerk, in pages.");
		add("SQL Server", "Memory Broker Clerks", "Periodic evictions (pages)", "The number of pages evicted from the broker clerk by last periodic eviction.");
		add("SQL Server", "Memory Broker Clerks", "Pressure evictions (pages/sec)", "TThe number of pages per second evicted from the broker clerk by memory pressure.");
		add("SQL Server", "Memory Broker Clerks", "Simulation benefit", "The value of memory to the clerk, in ms per page per ms, multiplied by 10 billion and truncated to an integer.");
		add("SQL Server", "Memory Broker Clerks", "Simulation size", "The current size of the clerk simulation, in pages.");

		add("SQL Server", "Memory Manager", "Connection Memory (KB)", "Specifies the total amount of dynamic memory the server is using for maintaining connections.");
		add("SQL Server", "Memory Manager", "Database Cache Memory (KB)", "Specifies the amount of memory the server is currently using for the database pages cache.");
		add("SQL Server", "Memory Manager", "External benefit of memory", "The external value of memory, in ms per page per ms, multiplied by 10 billion and truncated to an integer.");
		add("SQL Server", "Memory Manager", "Free Memory (KB)", "Specifies the amount of committed memory currently not used by the server.");
		add("SQL Server", "Memory Manager", "Granted Workspace Memory (KB)", "Specifies the total amount of memory currently granted to executing processes, such as hash, sort, bulk copy, and index creation operations.");
		add("SQL Server", "Memory Manager", "Lock Blocks", "Specifies the current number of lock blocks in use on the server (refreshed periodically). A lock block represents an individual locked resource, such as a table, page, or row.");
		add("SQL Server", "Memory Manager", "Lock Blocks Allocated", "Specifies the current number of allocated lock blocks. At server startup, the number of allocated lock blocks plus the number of allocated lock owner blocks depends on the SQL Server�Locks�configuration option. If more lock blocks are needed, the value increases.");
		add("SQL Server", "Memory Manager", "Lock Memory (KB)", "Specifies the total amount of dynamic memory the server is using for locks.");
		add("SQL Server", "Memory Manager", "Lock Owner Blocks", "Specifies the number of lock owner blocks currently in use on the server (refreshed periodically). A lock owner block represents the ownership of a lock on an object by an individual thread. Therefore, if three threads each have a shared (S) lock on a page, there will be three lock owner blocks.");
		add("SQL Server", "Memory Manager", "Lock Owner Blocks Allocated", "Specifies the current number of allocated lock owner blocks. At server startup, the number of allocated lock owner blocks and the number of allocated lock blocks depend on the SQL Server�Locks�configuration option. If more lock owner blocks are needed, the value increases dynamically.");
		add("SQL Server", "Memory Manager", "Log Pool Memory (KB)", "Total amount of dynamic memory the server is using for Log Pool.");
		add("SQL Server", "Memory Manager", "Maximum Workspace Memory (KB)", "Indicates the maximum amount of memory available for executing processes, such as hash, sort, bulk copy, and index creation operations.");
		add("SQL Server", "Memory Manager", "Memory Grants Outstanding", "Specifies the total number of processes that have successfully acquired a workspace memory grant.");
		add("SQL Server", "Memory Manager", "Memory Grants Pending", "Specifies the total number of processes waiting for a workspace memory grant.");
		add("SQL Server", "Memory Manager", "Optimizer Memory (KB)", "Specifies the total amount of dynamic memory the server is using for query optimization.");
		add("SQL Server", "Memory Manager", "Reserved Server Memory (KB)", "Indicates the amount of memory the server has reserved for future usage. This counter shows the current unused amount of memory initially granted that is shown in�Granted Workspace Memory (KB).");
		add("SQL Server", "Memory Manager", "SQL Cache Memory (KB)", "Specifies the total amount of dynamic memory the server is using for the dynamic SQL cache.");
		add("SQL Server", "Memory Manager", "Stolen Server Memory (KB)", "Specifies the amount of memory the server is using for purposes other than database pages.");
		add("SQL Server", "Memory Manager", "Target Server Memory (KB)", "Indicates the ideal amount of memory the server can consume.");
		add("SQL Server", "Memory Manager", "Total Server Memory (KB)", "Specifies the amount of memory the server has committed using the memory manager.");

		add("SQL Server", "Memory Node", "Database Node Memory (KB)", "Specifies the amount of memory the server is currently using on this node for database pages.");
		add("SQL Server", "Memory Node", "Free Node Memory (KB)", "Specifies the amount of memory the server is not using on this node.");
		add("SQL Server", "Memory Node", "Foreign Node Memory (KB)", "Specifies the amount of non NUMA-local memory on this node.");
		add("SQL Server", "Memory Node", "Stolen Memory Node (KB)", "Specifies the amount of memory the server is using on this node for purposes other than database pages.");
		add("SQL Server", "Memory Node", "Target Node Memory", "Specifies the ideal amount of memory for this node.");
		add("SQL Server", "Memory Node", "Total Node Memory", "Indicates the total amount of memory the server has committed on this node.");

		add("SQL Server", "Plan Cache", "Cache Hit Ratio", "Ratio between cache hits and lookups.");
		add("SQL Server", "Plan Cache", "Cache Hit Ratio Base", "For internal use only.");
		add("SQL Server", "Plan Cache", "Cache Object Counts", "Number of cache objects in the cache.");
		add("SQL Server", "Plan Cache", "Cache Pages", "Number of 8-kilobyte (KB) pages used by cache objects.");
		add("SQL Server", "Plan Cache", "Cache Objects in use", "Number of cache objects in use.");

		add("SQL Server", "Query Store", "Query Store CPU usage", "Indicates Query Stores usage of the CPU.");
		add("SQL Server", "Query Store", "Query Store logical reads", "Indicates the number of logical reads made by the Query Store.");
		add("SQL Server", "Query Store", "Query Store logical writes", "Indicates how much data is being queued to be flushed from the Query Store. The frequency and delay of adding items (that represent runtime stats) to the queue is controlled by Data Flush Interval setting.");
		add("SQL Server", "Query Store", "Query Store physical reads", "Indicates the number of physical reads made by the Query Store.");

		add("SQL Server", "Resource Pool Stats", "Active memory grant amount (KB)", "The current total amount, in kilobytes (KB), of granted memory. This information is also available in�sys.dm_exec_query_resource_semaphores.");
		add("SQL Server", "Resource Pool Stats", "Active memory grants count", "Current total count of memory grants. This information is also available in�sys.dm_exec_query_memory_grants.");
		add("SQL Server", "Resource Pool Stats", "Avg Disk Read IO (ms)", "Average time, in milliseconds, of a read operation from the disk.");
		add("SQL Server", "Resource Pool Stats", "Avg Disk Read IO (ms) Base", "For internal use only.");
		add("SQL Server", "Resource Pool Stats", "Avg Disk Write IO (ms)", "Average time, in milliseconds, of a write operation to the disk.");
		add("SQL Server", "Resource Pool Stats", "Avg Disk Write IO (ms) Base", "For internal use only.");
		add("SQL Server", "Resource Pool Stats", "Cache memory target (KB)", "The current memory broker target, in kilobytes (KB), for cache.");
		add("SQL Server", "Resource Pool Stats", "Compile memory target (KB)", "The current memory broker target, in kilobytes (KB), for query compiles.");
		add("SQL Server", "Resource Pool Stats", "CPU control effect %", "The effect of Resource Governor on the resource pool. Calculated as (CPU usage %) / (CPU usage % without Resource Governor.");
		add("SQL Server", "Resource Pool Stats", "CPU delayed %", "System CPU delayed for all requests in the specified instance of the performance object as a percentage of the total time active.");
		add("SQL Server", "Resource Pool Stats", "CPU delayed % base", "For internal use only.");
		add("SQL Server", "Resource Pool Stats", "CPU effective %", "System CPU usage by all requests in the specified instance of the performance object as a percentage of the total time active.");
		add("SQL Server", "Resource Pool Stats", "CPU effective % base", "For internal use only.");
		add("SQL Server", "Resource Pool Stats", "CPU usage %", "The CPU bandwidth usage by all requests in all workload groups belonging to this pool. This is measured relative to the computer and normalized to all CPUs on the system. This value will change as the amount of CPU available to the SQL Server process changes. It is not normalized to what the SQL Server process receives.");
		add("SQL Server", "Resource Pool Stats", "CPU usage % base", "For internal use only.");
		add("SQL Server", "Resource Pool Stats", "CPU usage target %", "The target value of CPU usage % for the resource pool based on the resource pool configuration settings and system load.");
		add("SQL Server", "Resource Pool Stats", "CPU violated %", "The difference between the CPU reservation and the effective scheduling percentage.");
		add("SQL Server", "Resource Pool Stats", "Disk Read Bytes/sec", "Number of bytes read from the disk in the last second.");
		add("SQL Server", "Resource Pool Stats", "Disk Read IO Throttled/sec", "Number of read operations throttled in the last second.");
		add("SQL Server", "Resource Pool Stats", "Disk Read IO/sec", "Number of read operations from the disk in the last second.");
		add("SQL Server", "Resource Pool Stats", "Disk Write Bytes/sec", "Number of bytes written to the disk in the last second.");
		add("SQL Server", "Resource Pool Stats", "Disk Write IO Throttled/sec", "Number of write operations throttled in the last second.");
		add("SQL Server", "Resource Pool Stats", "Disk Write IO/sec", "Number of write operations to the disk in the last second.");
		add("SQL Server", "Resource Pool Stats", "Max memory (KB)", "The maximum amount, in kilobytes (KB), of memory that the resource pool can have based on the resource pool settings and server state.");
		add("SQL Server", "Resource Pool Stats", "Memory grant timeouts/sec", "The number of memory grant time-outs per second.");
		add("SQL Server", "Resource Pool Stats", "Memory grants/sec", "The number of memory grants occurring in this resource pool per second.");
		add("SQL Server", "Resource Pool Stats", "Pending memory grant count", "The number of requests for memory grants pending in the queues. This information is also available in�sys.dm_exec_query_resource_semaphores.");
		add("SQL Server", "Resource Pool Stats", "Query exec memory target (KB)", "The current memory broker target, in kilobytes (KB), for query execution memory grant. This information is also available in�sys.dm_exec_query_memory_grants.");
		add("SQL Server", "Resource Pool Stats", "Target memory (KB)", "The target amount, in kilobytes (KB), of memory the resource pool is trying to obtain based on the resource pool settings and server state.");
		add("SQL Server", "Resource Pool Stats", "Used memory (KB)", "The amount of memory used, in kilobytes (KB), for the resource pool.");

		add("SQL Server", "SQL Errors", "Errors/sec", "Number of errors/sec.");

		add("SQL Server", "SQL Statistics", "Auto-Param Attempts/sec", "Number of auto-parameterization attempts per second. Total should be the sum of the failed, safe, and unsafe auto-parameterizations. Auto-parameterization occurs when an instance of SQL Server tries to parameterize a Transact-SQL request by replacing some literals with parameters so that reuse of the resulting cached execution plan across multiple similar-looking requests is possible. Note that auto-parameterizations are also known as simple parameterizations in newer versions of SQL Server. This counter does not include forced parameterizations.");
		add("SQL Server", "SQL Statistics", "Batch Requests/sec", "Number of Transact-SQL command batches received per second. This statistic is affected by all constraints (such as I/O, number of users, cache size, complexity of requests, and so on). High batch requests mean good throughput.");
		add("SQL Server", "SQL Statistics", "Failed Auto-Params/sec", "Number of failed auto-parameterization attempts per second. This should be small. Note that auto-parameterizations are also known as simple parameterizations in later versions of SQL Server.");
		add("SQL Server", "SQL Statistics", "Forced Parameterizations/sec", "Number of successful forced parameterizations per second.");
		add("SQL Server", "SQL Statistics", "Guided Plan Executions/sec", "Number of plan executions per second in which the query plan has been generated by using a plan guide.");
		add("SQL Server", "SQL Statistics", "Misguided Plan Executions/sec", "Number of plan executions per second in which a plan guide could not be honored during plan generation. The plan guide was disregarded and normal compilation was used to generate the executed plan.");
		add("SQL Server", "SQL Statistics", "Safe Auto-Params/sec", "Number of safe auto-parameterization attempts per second. Safe refers to a determination that a cached execution plan can be shared between different similar-looking Transact-SQL statements. SQL Server makes many auto-parameterization attempts some of which turn out to be safe and others fail. Note that auto-parameterizations are also known as simple parameterizations in later versions of SQL Server. This does not include forced parameterizations.");
		add("SQL Server", "SQL Statistics", "SQL Attention rate", "Number of attentions per second. An attention is a request by the client to end the currently running request.");
		add("SQL Server", "SQL Statistics", "SQL Compilations/sec", "Number of SQL compilations per second. Indicates the number of times the compile code path is entered. Includes compiles caused by statement-level recompilations in SQL Server. After SQL Server user activity is stable, this value reaches a steady state.");
		add("SQL Server", "SQL Statistics", "SQL Re-Compilations/sec", "Number of statement recompiles per second. Counts the number of times statement recompiles are triggered. Generally, you want the recompiles to be low.");
		add("SQL Server", "SQL Statistics", "Unsafe Auto-Params/sec", "Number of unsafe auto-parameterization attempts per second. For example, the query has some characteristics that prevent the cached plan from being shared. These are designated as unsafe. This does not count the number of forced parameterizations.");

		add("SQL Server", "Transactions", "Free Space in tempdb (KB)", "The amount of space (in kilobytes) available in�tempdb. There must be enough free space to hold both the snapshot isolation level version store and all new temporary objects created in this instance of the Database Engine.");
		add("SQL Server", "Transactions", "Longest Transaction Running Time", "The length of time (in seconds) since the start of the transaction that has been active longer than any other current transaction. This counter only shows activity when the database is under read committed snapshot isolation level. It does not log any activity if the database is in any other isolation level.");
		add("SQL Server", "Transactions", "NonSnapshot Version Transactions", "The number of currently active transactions that are not using snapshot isolation level and have made data modifications that have generated row versions in the�tempdb�version store.");
		add("SQL Server", "Transactions", "Snapshot Transactions", "The number of currently active transactions using the snapshot isolation level.<br>Note: The�Snapshot Transactions�object counter responds when the first data access occurs, not when the�BEGIN TRANSACTION�statement is issued.");
		add("SQL Server", "Transactions", "Transactions", "The number of currently active transactions of all types.");
		add("SQL Server", "Transactions", "Update conflict ratio", "The percentage of those transactions using the snapshot isolation level that have encountered update conflicts within the last second. An update conflict occurs when a snapshot isolation level transaction attempts to modify a row that last was modified by another transaction that was not committed when the snapshot isolation level transaction started.");
		add("SQL Server", "Transactions", "Update conflict ratio base", "For internal use only.");
		add("SQL Server", "Transactions", "Update Snapshot Transactions", "The number of currently active transactions using the snapshot isolation level and have modified data.");
		add("SQL Server", "Transactions", "Version Cleanup rate (KB/s)", "The rate (in kilobytes per second) at which row versions are removed from the snapshot isolation version store in�tempdb.");
		add("SQL Server", "Transactions", "Version Generation rate (KB/s)", "The rate (in kilobytes per second) at which new row versions are added to the snapshot isolation version store in�tempdb.");
		add("SQL Server", "Transactions", "Version Store Size (KB)", "The amount of space (in kilobytes) in�tempdb�being used to store snapshot isolation level row versions.");
		add("SQL Server", "Transactions", "Version Store unit count", "The number of active allocation units in the snapshot isolation version store in�tempdb.");
		add("SQL Server", "Transactions", "Version Store unit creation", "The number of allocation units that have been created in the snapshot isolation store since the instance of the Database Engine was started.");
		add("SQL Server", "Transactions", "Version Store unit truncation", "The number of allocation units that have been removed from the snapshot isolation store since the instance of the Database Engine was started.");

		add("SQL Server", "User Settable", "Query", "The�User Settable�object contains the query counter. Users configure the�User counters�within the query object.");

		add("SQL Server", "Wait Statistics", "Lock waits", "Statistics for processes waiting on a lock.");
		add("SQL Server", "Wait Statistics", "Log buffer waits", "Statistics for processes waiting for log buffer to be available.");
		add("SQL Server", "Wait Statistics", "Log write waits", "Statistics for processes waiting for log buffer to be written.");
		add("SQL Server", "Wait Statistics", "Memory grant queue waits", "Statistics for processes waiting for memory grant to become available.");
		add("SQL Server", "Wait Statistics", "Network IO waits", "Statistics relevant to wait on network I/O.");
		add("SQL Server", "Wait Statistics", "Non-Page latch waits", "Statistics relevant to non-page latches.");
		add("SQL Server", "Wait Statistics", "Page IO latch waits", "Statistics relevant to page I/O latches.");
		add("SQL Server", "Wait Statistics", "Page latch waits", "Statistics relevant to page latches, not including I/O latches.");
		add("SQL Server", "Wait Statistics", "Thread-safe memory objects waits", "Statistics for processes waiting on thread-safe memory allocators.");
		add("SQL Server", "Wait Statistics", "Transaction ownership waits", "Statistics relevant to processes synchronizing access to transaction.");
		add("SQL Server", "Wait Statistics", "Wait for the worker", "Statistics relevant to processes waiting for worker to become available.");
		add("SQL Server", "Wait Statistics", "Workspace synchronization waits", "Statistics relevant to processes synchronizing access to workspace.");

		add("SQL Server", "Workload Group Stats", "Active parallel threads", "The current count of parallel threads usage.");
		add("SQL Server", "Workload Group Stats", "Active requests", "The number of requests that are currently running in this workload group. This should be equivalent to the count of rows from sys.dm_exec_requests filtered by group ID.");
		add("SQL Server", "Workload Group Stats", "Blocked requests", "The current number of blocked requests in the workload group. This can be used to determine workload characteristics.");
		add("SQL Server", "Workload Group Stats", "CPU delayed %", "System CPU delayed for all requests in the specified instance of the performance object as a percentage of the total time active.");
		add("SQL Server", "Workload Group Stats", "CPU delayed % base", "For internal use only.");
		add("SQL Server", "Workload Group Stats", "CPU effective %", "System CPU usage by all requests in the specified instance of the performance object as a percentage of the total time active.");
		add("SQL Server", "Workload Group Stats", "CPU effective % base", "For internal use only.");
		add("SQL Server", "Workload Group Stats", "CPU usage %", "The CPU bandwidth usage by all requests in this workload group measured relative to the computer and normalized to all the CPUs on the system. This value will change as the amount of CPU available to the SQL Server process changes. It is not normalized to what the SQL Server process receives.");
		add("SQL Server", "Workload Group Stats", "CPU usage % base", "For internal use only.");
		add("SQL Server", "Workload Group Stats", "CPU violated %", "The difference between the CPU reservation and the effective scheduling percentage.");
		add("SQL Server", "Workload Group Stats", "Max request CPU time (ms)", "The maximum CPU time, in milliseconds, used by a request currently running in the workload group.");
		add("SQL Server", "Workload Group Stats", "Max request memory grant (KB)", "The maximum value of memory grant, in kilobytes (KB), for a query.");
		add("SQL Server", "Workload Group Stats", "Query optimizations/sec", "The number of query optimizations that have happened in this workload group per second. This can be used to determine workload characteristics.");
		add("SQL Server", "Workload Group Stats", "Queued requests", "The current number of queued requests that is waiting to be picked up. This count can be non-zero if throttling occurs after the GROUP_MAX_REQUESTS limit is reached.");
		add("SQL Server", "Workload Group Stats", "Reduced memory grants/sec", "The number of queries that are getting less than ideal amount of memory grants per second.");
		add("SQL Server", "Workload Group Stats", "Requests completed/sec", "The number of requests that have completed in this workload group. This number is cumulative.");
		add("SQL Server", "Workload Group Stats", "Suboptimal plans/sec", "The number of suboptimal plans that are generated in this workload group per second.");



		add("SQL Server", "XTP Cursors", "Cursor deletes/sec", "The number of cursor deletes (on average), per second.");
		add("SQL Server", "XTP Cursors", "Cursor inserts/sec", "The number of cursor inserts (on average), per second.");
		add("SQL Server", "XTP Cursors", "Cursor scans started /sec", "The number of cursor scans started (on average), per second.");
		add("SQL Server", "XTP Cursors", "Cursor unique violations/sec", "The number of unique-constraint violations (on average), per second.");
		add("SQL Server", "XTP Cursors", "Cursor updates/sec", "The number of cursor updates (on average), per second.");
		add("SQL Server", "XTP Cursors", "Cursor write conflicts/sec", "The number of write-write conflicts to the same row version (on average), per second.");
		add("SQL Server", "XTP Cursors", "Dusty corner scan retries/sec (user-issued)", "The number of scan retries due to write conflicts during dusty corner sweeps issued by a user's full-table scan (on average), per second. This is a very low-level counter, not intended for customer use.");
		add("SQL Server", "XTP Cursors", "Expired rows removed/sec", "The number of expired rows removed by cursors (on average), per second.");
		add("SQL Server", "XTP Cursors", "Expired rows touched/sec", "The number of expired rows touched by cursors (on average), per second.");
		add("SQL Server", "XTP Cursors", "Rows returned/sec", "The number of rows returned by cursors (on average), per second.");
		add("SQL Server", "XTP Cursors", "Rows touched/sec", "The number of rows touched by cursors (on average), per second.");
		add("SQL Server", "XTP Cursors", "Tentatively-deleted rows touched/sec", "The number of expiring rows touched by cursors (on average), per second. A row is expiring if the transaction that deleted it is still active (i.e. has not yet committed or aborted.)");

		add("SQL Server", "XTP Databases", "Avg Transaction Segment Large Data Size", "Average size of transaction segment large data payload. This is a very low-level counter, not intended for customer use.");
		add("SQL Server", "XTP Databases", "Avg Transaction Segment Size", "Average size of transaction segment payload. If this value goes to zero, more pages are allocated from the backend allocator. This is a very low-level counter, not intended for customer use.");
		add("SQL Server", "XTP Databases", "Flush Thread 256K Queue Depth", "Flush Thread queue depth for 256K IO requests.");
		add("SQL Server", "XTP Databases", "Flush Thread 4K Queue Depth", "Flush Thread queue depth for 4K IO requests.");
		add("SQL Server", "XTP Databases", "Flush Thread 64K Queue Depth", "Flush Thread queue depth for 64K IO requests.");
		add("SQL Server", "XTP Databases", "Flush Thread Frozen IOs/sec (256K)", "The number of 256K IO requests encountered during flush page processing that are above the freeze threshold and thus cannot be issued.");
		add("SQL Server", "XTP Databases", "Flush Thread Frozen IOs/sec (4K)", "The number of 4K IO requests encountered during flush page processing that are above the freeze threshold and thus cannot be issued.");
		add("SQL Server", "XTP Databases", "Flush Thread Frozen IOs/sec (64K)", "The number of 64K IO requests encountered during flush page processing that are above the freeze threshold and thus cannot be issued.");
		add("SQL Server", "XTP Databases", "IoPagePool256K Free List Count", "Number of pages in the the 256K IO page pool free list. If this value goes to zero, more pages are allocated from the backend allocator. This is a very low-level counter, not intended for customer use.");
		add("SQL Server", "XTP Databases", "IoPagePool256K Total Allocated", "Total number of pages allocated and held by the 256K IO page pool from the backend allocator. This is a very low-level counter, not intended for customer use.");
		add("SQL Server", "XTP Databases", "IoPagePool4K Free List Count", "Number of pages in the the 4K IO page pool free list. If this value goes to zero, more pages are allocated from the backend allocator. This is a very low-level counter, not intended for customer use.");
		add("SQL Server", "XTP Databases", "IoPagePool4K Total Allocated", "Total number of pages allocated and held by the 4K IO page pool from the backend allocator. This is a very low-level counter, not intended for customer use.");
		add("SQL Server", "XTP Databases", "IoPagePool64K Free List Count", "Number of pages in the the 64K IO page pool free list. If this value goes to zero, more pages are allocated from the backend allocator. This is a very low-level counter, not intended for customer use.");
		add("SQL Server", "XTP Databases", "IoPagePool64K Total Allocated", "Total number of pages allocated and held by the 64K IO page pool from the backend allocator. This is a very low-level counter, not intended for customer use.");
		add("SQL Server", "XTP Databases", "MtLog 256K Expand Count", "Number of times a 256K MtLog was expanded. This is a very low-level counter, not intended for customer use.");
		add("SQL Server", "XTP Databases", "MtLog 256K IOs Outstanding", "The number of outstanding 256K IO requests issued by MtLog.");
		add("SQL Server", "XTP Databases", "MtLog 256K Page Fill %/Page Flushed", "Average fill percentage of each 256K MtLog page flushed. This is a very low-level counter, not intended for customer use.");
		add("SQL Server", "XTP Databases", "MtLog 256K Write Bytes/sec", "Write bytes per second on 256K MtLog objects. This is a very low-level counter, not intended for customer use.");
		add("SQL Server", "XTP Databases", "MtLog 4K Expand Count", "Number of times a 4K MtLog was expanded. This is a very low-level counter, not intended for customer use.");
		add("SQL Server", "XTP Databases", "MtLog 4K IOs Outstanding", "The number of outstanding 4K IO requests issued by MtLog.");
		add("SQL Server", "XTP Databases", "MtLog 4K Page Fill %/Page Flushed", "Average fill percentage of each 4K MtLog page flushed. This is a very low-level counter, not intended for customer use.");
		add("SQL Server", "XTP Databases", "MtLog 4K Write Bytes/sec", "Write bytes per second on 4K MtLog objects. This is a very low-level counter, not intended for customer use.");
		add("SQL Server", "XTP Databases", "MtLog 64K Expand Count", "Number of times a 64K MtLog was expanded. This is a very low-level counter, not intended for customer use.");
		add("SQL Server", "XTP Databases", "MtLog 64K IOs Outstanding", "The number of outstanding 64K IO requests issued by MtLog.");
		add("SQL Server", "XTP Databases", "MtLog 64K Page Fill %/Page Flushed", "Average fill percentage of each 64K MtLog page flushed. This is a very low-level counter, not intended for customer use.");
		add("SQL Server", "XTP Databases", "MtLog 64K Write Bytes/sec", "Write bytes per second on 64K MtLog objects. This is a very low-level counter, not intended for customer use.");
		add("SQL Server", "XTP Databases", "Num Merges", "The number of merges in flight.");
		add("SQL Server", "XTP Databases", "Num Merges/sec", "The number of merges created per second (on average).");
		add("SQL Server", "XTP Databases", "Num Serializations", "The number of serializations in flight.");
		add("SQL Server", "XTP Databases", "Num Serializations/sec", "The number of serializations created per second (on average).");
		add("SQL Server", "XTP Databases", "Tail Cache Page Count", "Number of pages allocated in the Tail Cache. This is a very low-level counter, not intended for customer use.");
		add("SQL Server", "XTP Databases", "Tail Cache Page Count Peak", "Highest number of pages allocated in the Tail Cache. This is a very low-level counter, not intended for customer use.");

		add("SQL Server", "XTP Garbage Collection", "Dusty corner scan retries/sec (GC-issued)", "The number of scan retries due to write conflicts during dusty corner sweeps issued by the garbage collector (on average), per second. This is a very low-level counter, not intended for customer use.");
		add("SQL Server", "XTP Garbage Collection", "Main GC work items/sec", "The number of work items processed by the main GC thread.");
		add("SQL Server", "XTP Garbage Collection", "Parallel GC work item/sec", "The number of times a parallel thread has executed a GC work item.");
		add("SQL Server", "XTP Garbage Collection", "Rows processed/sec", "The number of rows processed by the garbage collector (on average), per second.");
		add("SQL Server", "XTP Garbage Collection", "Rows processed/sec (first in bucket and removed)", "The number of rows processed by the garbage collector that were first in the corresponding hash bucket, and were able to be removed immediately (on average), per second.");
		add("SQL Server", "XTP Garbage Collection", "Rows processed/sec (first in bucket)", "The number of rows processed by the garbage collector that were first in the corresponding hash bucket (on average), per second.");
		add("SQL Server", "XTP Garbage Collection", "Rows processed/sec (marked for unlink)", "The number of rows processed by the garbage collector that were already marked for unlink (on average), per second.");
		add("SQL Server", "XTP Garbage Collection", "Rows processed/sec (no sweep needed)", "The number of rows processed by the garbage collector that will not require a dusty corner sweep (on average), per second.");
		add("SQL Server", "XTP Garbage Collection", "Sweep expired rows removed/sec", "The number of expired rows removed during dusty corner sweeps (on average), per second.");
		add("SQL Server", "XTP Garbage Collection", "Sweep expired rows touched/sec", "The number of expired rows touched during dusty corner sweeps (on average), per second.");
		add("SQL Server", "XTP Garbage Collection", "Sweep expiring rows touched/sec", "The number of expiring rows touched during dusty corner sweeps (on average), per second.");
		add("SQL Server", "XTP Garbage Collection", "Sweep rows touched/sec", "The number of rows touched during dusty corner sweeps (on average), per second.");
		add("SQL Server", "XTP Garbage Collection", "Sweep scans started/sec", "The number of dusty corner sweep scans started (on average), per second.");

		add("SQL Server", "XTP IO Governor", "Insufficient Credits Waits/sec", "Number of waits due to insufficient credits in the rate objects (per second).");
		add("SQL Server", "XTP IO Governor", "Io Issued/sec", "Number of Io issued per second by flush threads.");
		add("SQL Server", "XTP IO Governor", "Log Blocks/sec", "Number of log blocks processed by controller per second.");
		add("SQL Server", "XTP IO Governor", "Missed Credit Slots", "Number of credit slots missed because of wait for credits from rate object.");
		add("SQL Server", "XTP IO Governor", "Stale Rate Object Waits/sec", "Number of waits due to stale rate objects (per second).");
		add("SQL Server", "XTP IO Governor", "Total Rate Objects Published", "Total number of Rate objects published.");

		add("SQL Server", "XTP Phantom Processor", "Dusty corner scan retries/sec (Phantom-issued)", "The number of scan retries due to write conflicts during dusty corner sweeps issued by the phantom processor (on average), per second. This is a very low-level counter, not intended for customer use.");
		add("SQL Server", "XTP Phantom Processor", "Phantom expired rows removed/sec", "The number of expired rows removed by phantom scans (on average), per second.");
		add("SQL Server", "XTP Phantom Processor", "Phantom expired rows touched/sec", "The number of expired rows touched by phantom scans (on average), per second.");
		add("SQL Server", "XTP Phantom Processor", "Phantom expiring rows touched/sec", "The number of expiring rows touched by phantom scans (on average), per second.");
		add("SQL Server", "XTP Phantom Processor", "Phantom rows touched/sec", "The number of rows touched by phantom scans (on average), per second.");
		add("SQL Server", "XTP Phantom Processor", "Phantom scans started/sec", "The number of phantom scans started (on average), per second.");

		add("SQL Server", "XTP Storage", "Checkpoints Closed", "Count of checkpoints closed done by online agent.");
		add("SQL Server", "XTP Storage", "Checkpoints Completed", "Count of checkpoints processed by offline checkpoint thread.");
		add("SQL Server", "XTP Storage", "Core Merges Completed", "The number of core merges completed by the merge worker thread. These merges still need to be installed.");
		add("SQL Server", "XTP Storage", "Merge Policy Evaluations", "The number of merge policy evaluations since the server started.");
		add("SQL Server", "XTP Storage", "Merge Requests Outstanding", "The number of merge requests outstanding since the server started.");
		add("SQL Server", "XTP Storage", "Merges Abandoned", "The number of merges abandoned due to failure.");
		add("SQL Server", "XTP Storage", "Merges Installed", "The number of merges successfully installed.");
		add("SQL Server", "XTP Storage", "Total Files Merged", "The total number of source files merged. This count can be used to find the average number of source files in the merge.");

		add("SQL Server", "XTP Transaction Log", "Log bytes written/sec", "The number of bytes written to the SQL Server transaction log by the In-Memory OLTP engine (on average), per second.");
		add("SQL Server", "XTP Transaction Log", "Log records written/sec", "The number of records written to the SQL Server transaction log by the In-Memory OLTP engine (on average), per second.");

		add("SQL Server", "XTP Transactions", "Cascading aborts/sec", "The number of transactions that rolled back to due a commit dependency rollback (on average), per second.");
		add("SQL Server", "XTP Transactions", "Commit dependencies taken/sec", "The number of commit dependencies taken by transactions (on average), per second.");
		add("SQL Server", "XTP Transactions", "Read-only transactions prepared/sec", "The number of read-only transactions that were prepared for commit processing, per second.");
		add("SQL Server", "XTP Transactions", "Save point refreshes/sec", "The number of times a savepoint was \"refreshed\", (on average), per second. A savepoint refresh is when an existing savepoint is reset to the current point in the transaction's lifetime.");
		add("SQL Server", "XTP Transactions", "Save point rollbacks/sec", "The number of times a transaction rolled back to a save point (on average), per second.");
		add("SQL Server", "XTP Transactions", "Save points created /sec", "The number of save points created (on average), per second.");
		add("SQL Server", "XTP Transactions", "Transaction validation failure/sec", "The number of transactions that failed validation processing (on average), per second.");
		add("SQL Server", "XTP Transactions", "Transactions aborted by user/sec", "The number of transactions that were aborted by the user (on average), per second.");
		add("SQL Server", "XTP Transactions", "Transactions aborted/sec", "The number of transactions that aborted (both by the user and the system, on average), per second.");
		add("SQL Server", "XTP Transactions", "Transactions created/sec", "The number of transactions created in the system (on average), per second.");
	}
}
