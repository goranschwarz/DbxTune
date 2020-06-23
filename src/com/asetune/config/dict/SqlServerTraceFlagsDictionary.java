/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.config.dict;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.asetune.utils.StringUtil;


public class SqlServerTraceFlagsDictionary
{
	/** Instance variable */
	private static SqlServerTraceFlagsDictionary _instance = null;

	private HashMap<Integer,TraceFlagRecord> _traceflags = new HashMap<Integer,TraceFlagRecord>();

	public class TraceFlagRecord
	{
		private int    _id              = -1;
		private String _description     = null;
		private List<String> _descriptionList = new ArrayList<>();

		public TraceFlagRecord(int id, String... description)
		{
			_id          = id;
//			_description = description;
			
			StringBuilder sb = new StringBuilder();
			for (String str : description)
			{
				sb.append(str).append("\n");
				_descriptionList.add(str);
			}
			_description = sb.toString();
		}
		
		@Override
		public String toString()
		{
			if (_descriptionList.size() == 1)
			{
				return StringUtil.left(_id+"", 5) + " - " + _description;
			}
			else
			{
				StringBuilder sb = new StringBuilder();
				
				String prefix = "       ";
				sb.append(StringUtil.left(_id+"", prefix.length())).append(_descriptionList.get(0)).append("\n");
				for (int r=1; r<_descriptionList.size(); r++)
				{
					sb.append(prefix).append(_descriptionList.get(r)).append("\n");
				}
				sb.append("\n");
				
				return sb.toString();
			}
		}
	}


	public SqlServerTraceFlagsDictionary()
	{
		init();
	}

	public static SqlServerTraceFlagsDictionary getInstance()
	{
		if (_instance == null)
			_instance = new SqlServerTraceFlagsDictionary();
		return _instance;
	}

	public String getDescription(int traceflag)
	{
		TraceFlagRecord rec = _traceflags.get(traceflag);
		if (rec != null)
			return rec.toString();

		// Compose an empty one
		return "Trace flag '"+traceflag+"' not found in dictionary.\n"
				+ "Note, it may be described in below sites: \n"
				+ "   https://www.sqlservercentral.com/articles/sql-server-trace-flags-complete-list-3 \n"
				+ "   http://www.sqlservice.se/updated-microsoft-sql-server-trace-flag-list \n";
	}


	private void set(TraceFlagRecord rec)
	{
		if ( _traceflags.containsKey(rec._id))
			System.out.println("Trace flag '"+rec._id+"' already exists. It will be overwritten.");

		_traceflags.put(rec._id, rec);
	}

	private void set(int id, String... description)
	{
		set(new TraceFlagRecord(id, description));
	}

	private void init()
	{
		/*----------------------------------------------------------------------------
		 * Below values was extracted from: 
		 *      https://docs.microsoft.com/en-us/sql/t-sql/database-console-commands/dbcc-traceon-trace-flags-transact-sql?view=sql-server-2017
		 * at 2019-09-15
		 *----------------------------------------------------------------------------
		 */

		set(139, "Forces correct conversion semantics in the scope of DBCC check commands like DBCC CHECKDB, DBCC CHECKTABLE and DBCC CHECKCONSTRAINTS, "
				,"when analyzing the improved precision and conversion logic introduced with compatibility level 130 for specific data types, "
				,"on a database that has a lower compatibility level. For more information, see this Microsoft Support article."
				,""
				,"Note: This trace flag applies to SQL Server 2016 (13.x) RTM CU3, SQL Server 2016 (13.x) SP1 and higher builds."
				,""
				,"WARNING: Trace flag 139 is not meant to be enabled continuously in a production environment, "
				,"and should be used for the sole purpose of performing database validation checks described in this Microsoft Support article. "
				,"It should be immediately disabled after validation checks are completed."
				,""
				,"Scope: global only"
				);


		set(174, "Increases the SQL Server Database Engine plan cache bucket count from 40,009 to 160,001 on 64-bit systems. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Note: Please ensure that you thoroughly test this option, before rolling it into a production environment."
				,""
				,"Scope: global only"
				);


		set(176, "Enables a fix to address errors when rebuilding partitions online for tables that contain a computed partitioning column. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Scope: global or session,"
				);


		set(205, "Reports to the error log when a statistics-dependent stored procedure is being recompiled as a result of auto-update statistics. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Scope: global only"
				);


		set(260,	"Prints versioning information about extended stored procedure dynamic-link libraries (DLLs). "
				,"For more information about GetXpVersion(), see Creating Extended Stored Procedures."
				,""
				,"Scope: global or session"
				);


		set(272, "Disables identity pre-allocation to avoid gaps in the values of an identity column in cases where the server restarts unexpectedly or fails over to a secondary server. "
				,"Note that identity caching is used to improve INSERT performance on tables with identity columns."
				,""
				,"Note: Starting with SQL Server 2017 (14.x), to accomplish this at the database level, see the IDENTITY_CACHE option in ALTER DATABASE SCOPED CONFIGURATION (Transact-SQL)."
				,""
				,"Scope: global only"
				);


		set(460, "Replaces data truncation message ID 8152 with message ID 2628. For more information, see this Microsoft Support article."
				,""
				,"Starting with SQL Server 2019 CTP 2.4, to accomplish this at the database level, see the VERBOSE_TRUNCATION_WARNINGS option in ALTER DATABASE SCOPED CONFIGURATION (Transact-SQL)."
				,""
				,"Note: This trace flag applies to SQL Server 2017 (14.x) CU12, and higher builds."
				,""
				,"Note: Starting with database compatibility level 150, message ID 2628 is the default and this trace flag has no effect."
				,""
				,"Scope: global or session"
				);


		set(610, "Controls minimally logged inserts into indexed tables. "
				,"This trace flag is not required starting SQL Server 2016 as minimal logging is turned on by default for indexed tables. "
				,"In SQL Server 2016, when the bulk load operation causes a new page to be allocated, all of the rows sequentially filling that new page are "
				,"minimally logged if all the other pre-requisites for minimal logging are met. "
				,"Rows inserted into existing pages (no new page allocation) to maintain index order are still fully logged, as are rows that are moved as a "
				,"result of page splits during the load. It is also important to have ALLOW_PAGE_LOCKS turned ON for indexes (which is ON by default) for "
				,"minimal logging operation to work as page locks are acquired during allocation and thereby only page or extent allocations are logged."
				,"For more information, see Data Loading Performance Guide."
				,""
				,"Scope: global or session"
				);


		set(634, "Disables the background columnstore compression task. "
				,"SQL Server periodically runs the Tuple Mover background task that compresses columnstore index rowgroups with uncompressed data, one such rowgroup at a time."
				,""
				,"Columnstore compression improves query performance but also consumes system resources. "
				,"You can control the timing of columnstore compression manually, by disabling the background compression task with trace "
				,"flag 634, and then explicitly invoking ALTER INDEX...REORGANIZE or ALTER INDEX...REBUILD at the time of your choice."
				,""
				,"Scope: global only"
				);


		set(652, "Disables page pre-fetching scans. For more information, see this Microsoft Support article."
				,""
				,"Scope: global or session"
				);


		set(661, "Disables the ghost record removal process. For more information, see this Microsoft Support article."
				,""
				,"Scope: global only"
				);


		set(692, "Disables fast inserts while bulk loading data into heap or clustered index. "
				,"Starting SQL Server 2016 (13.x), fast inserts is enabled by default leveraging minimal logging when database is in simple or bulk logged"
				," recovery model to optimize insert performance for records inserted into new pages. "
				,"With fast inserts, each bulk load batch acquires new extent(s) bypassing the allocation lookup for existing extent with available free space to optimize insert performance."
				,""
				,"With fast inserts, bulk loads with small batch sizes can lead to increased unused space consumed by objects hence it is recommended to use large batchsize for each batch to fill the extent completely. "
				,"If increasing batchsize is not feasible, this trace flag can help reduce unused space reserved at the expense of performance."
				,""
				,"Note: This trace flag applies to SQL Server 2016 (13.x) RTM and higher builds."
				,""
				,"Scope: global or session"
				);


		set(715, "Enables table lock for bulk load operations into a heap with no nonclustered indexes. "
				,"When this trace flag is enabled, bulk load operations acquire bulk update (BU) locks when bulk copying data into a table. "
				,"Bulk update (BU) locks allow multiple threads to bulk load data concurrently into the same table, while preventing other processes that are not bulk loading data from accessing the table."
				,""
				,"The behavior is similar to when the user explicitly specifies TABLOCK hint while performing bulk load, or when the sp_tableoption table lock on bulk load is enabled for a given table. "
				,"However, when this trace flag is enabled, this behavior becomes default without any query or database changes."
				,""
				,"Scope: global or session"
				);


		set(834, "Uses large-page allocations for the buffer pool, columnstore, and in-memory tables. For more information, see this Microsoft Support article."
				,""
				,"Note: When enabled, the large-page memory model pre-allocates all SQLOS memory at instance startup and does not return that memory to the OS."
				,""
				,"Note: If you are using the Columnstore Index feature of SQL Server 2012 (11.x) to SQL Server 2017, we do not recommend turning on trace flag 834."
				,""
				,"Scope: global only"
				);


		set(845, "Enables locked pages on Standard SKUs of SQL Server, when the service account for SQL Server has the Lock Pages in Memory privilege enabled. "
				,"For more information, see this Microsoft Support article and the documentation page on Server Memory Server Configuration Options."
				,""
				,"Note: Starting with SQL Server 2012 (11.x) this behavior is enabled by default for Standard SKUs, and trace flag 845 must not be used."
				,""
				,"Scope: global only"
				);


		set(902, "Bypasses execution of database upgrade script when installing a Cumulative Update or Service Pack. "
				,"If you encounter an error during script upgrade mode, it is recommended to contact Microsoft SQL Customer Service and Support (CSS) for further guidance. For more information, see this Microsoft Support article."
				,""
				,"WARNING: This trace flag is meant for troubleshooting of failed updates during script upgrade mode, and it is not supported to run it continuously in a production environment. "
				,"Database upgrade scripts needs to execute successfully for a complete install of Cumulative Updates and Service Packs. Not doing so can cause unexpected issues with your SQL Server instance."
				,""
				,"Scope: global only"
				);


		set(1117, "When a file in the filegroup meets the autogrow threshold, all files in the filegroup grow. "
				,"This trace flag affects all databases and is recommended only if every database is safe to be grow all files in a filegroup by the same amount."
				,""
				,"Note: Starting with SQL Server 2016 (13.x) this behavior is controlled by the AUTOGROW_SINGLE_FILE and AUTOGROW_ALL_FILES option "
				,"of ALTER DATABASE, and trace flag 1117 has no effect. For more information, see ALTER DATABASE File and Filegroup Options (Transact-SQL)."
				,""
				,"Scope: global only"
				);


		set(1118, "Forces page allocations on uniform extents instead of mixed extents, reducing contention on the SGAM page. "
				,"When a new object is created, by default, the first eight pages are allocated from different extents (mixed extents). "
				,"Afterwards, when more pages are needed, those are allocated from that same extent (uniform extent). "
				,"The SGAM page is used to track these mixed extents, so can quickly become a bottleneck when numerous mixed page allocations are occurring. "
				,"This trace flag allocates all eight pages from the same extent when creating new objects, minimizing the need to scan the SGAM page. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Note: Starting with SQL Server 2016 (13.x) this behavior is controlled by the SET MIXED_PAGE_ALLOCATION "
				,"option of ALTER DATABASE, and trace flag 1118 has no effect. "
				,"For more information, see ALTER DATABASE SET Options (Transact-SQL)."
				,""
				,"Scope: global only"
				);


		set(1204, "Returns the resources and types of locks participating in a deadlock and also the current command affected. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Scope: global only"
				);


		set(1211, "Disables lock escalation based on memory pressure, or based on number of locks. "
				,"The SQL Server Database Engine will not escalate row or page locks to table locks."
				,""
				,"Using this trace flag can generate excessive number of locks and if the lock memory grows large enough, attempts to allocate additional locks for any query may fail. "
				,"This can slow the performance of the Database Engine, or cause 1204 errors (unable to allocate lock resource) because of insufficient memory."
				,""
				,"If both trace flag 1211 and 1224 are set, 1211 takes precedence over 1224. However, because trace flag 1211 prevents escalation in every case, even under memory pressure, "
				,"we recommend that you use 1224 instead. This helps avoid 'out-of-locks' errors when many locks are being used."
				,""
				,"For more information on how to resolve blocking problems that are caused by lock escalation in SQL Server, see this Microsoft Support Article."
				,""
				,"Scope: global or session"
				);


		set(1222, "Returns the resources and types of locks that are participating in a deadlock and also the current command affected, in an XML format that does not comply with any XSD schema."
				,""
				,"Scope: global only"
				);


		set(1224, "Disables lock escalation based on the number of locks. "
				,"However, memory pressure can still activate lock escalation. "
				,"The Database Engine escalates row or page locks to table (or partition) locks if the amount of memory used by lock objects exceeds one of the following conditions:"
				,""
				,"    * Forty percent of the memory that is used by Database Engine. This is applicable only when the locks parameter of sp_configure is set to 0."
				,"    * Forty percent of the lock memory that is configured by using the locks parameter of sp_configure. For more information, see Server Configuration Options (SQL Server)."
				,""
				,"If both trace flag 1211 and 1224 are set, 1211 takes precedence over 1224. "
				,"However, because trace flag 1211 prevents escalation in every case, even under memory pressure, we recommend that you use 1224. "
				,"This helps avoid 'out-of-locks' errors when many locks are being used."
				,""
				,"Note: Lock escalation to the table-level or HoBT-level granularity can also be controlled by using the LOCK_ESCALATION option of the ALTER TABLE statement."
				,""
				,"For more information on how to resolve blocking problems that are caused by lock escalation in SQL Server, see this Microsoft Support Article"
				,""
				,"Scope: global or session"
				);


		set(1229, "Disables all lock partitioning regardless of the number of CPUs. "
				,"By default, SQL Server enables lock partitioning when a server has 16 or more CPUs, to improve the scalability characteristics of larger systems. "
				,"For more information on lock partitioning, see the Transaction Locking and Row Versioning Guide."
				,""
				,"WARNING: Trace flag 1229 can cause spinlock contention and poor performance, or unexpected behaviors when switching partitions."
				,""
				,"Scope: global only"
				);


		set(1236, "Enables database lock partitioning. For more information, see this Microsoft Support article."
				,""
				,"Note: Starting with SQL Server 2012 (11.x) SP3 and SQL Server 2014 (12.x) SP1 this behavior is controlled by the engine and trace flag 1236 has no effect."
				,""
				,"Scope: global only"
				);


		set(1237, "Allows the ALTER PARTITION FUNCTION statement to honor the current user-defined session deadlock priority instead of being the likely deadlock victim by default. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Note: Starting with SQL Server 2017 (14.x) and database compatibility level 140 this is the default behavior and trace flag 1237 has no effect."
				,""
				,"Scope: global or session or query"
				);


		set(1260, "Disable scheduler monitor dumps."
				,""
				,"Scope: global only"
				);


		set(1448, "Enables the replication log reader to move forward even if the async secondaries have not acknowledged the reception of a change. "
				,"Even with this trace flag enabled the log reader always waits for the sync secondaries. "
				,"The log reader will not go beyond the min ack of the sync secondaries. "
				,"This trace flag applies to the instance of SQL Server, not just an availability group, an availability database, or a log reader instance. "
				,"Takes effect immediately without a restart. "
				,"This trace flag can be activated ahead of time or when an async secondary fails. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Scope: global only"
				);


		set(1462, "Disables log stream compression for asynchronous availability groups. "
				,"This feature is enabled by default on asynchronous availability groups in order to optimize network bandwidth. "
				,"For more information, see Tune compression for availability group."
				,""
				,"Scope: global only"
				);


		set(1800, "Enables SQL Server optimization when disks of different sector sizes are used for primary and secondary replica log files, in SQL Server Always On and Log Shipping environments. "
				,"This trace flag is only required to be enabled on SQL Server instances with transaction log file residing on disk with sector size of 512 bytes. "
				,"It is not required to be enabled on disk with 4k sector sizes. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Scope: global only"
				);


		set(2301, "Enable advanced decision support optimizations. For more information, see this Microsoft Support article."
				,""
				,"Scope: global and session and query"
				);


		set(2312, "Sets the query optimizer cardinality estimation model to the SQL Server 2014 (12.x) through SQL Server 2017 versions, dependent of the compatibility level of the database."
				,""
				,"Note: If the database compatibility level is lower than 120, enabling trace flag 2312 uses the cardinality estimation model of SQL Server 2014 (12.x) (120). "
				,"For more information, see Microsoft Support article."
				,""
				,"Starting with SQL Server 2016 (13.x) SP1, to accomplish this at the query level, add the USE HINT 'FORCE_DEFAULT_CARDINALITY_ESTIMATION' query hint instead of using this trace flag."
				,""
				,"Scope: global or session or query"
				);


		set(2335, "Causes SQL Server to assume a fixed amount of memory is available during query optimization. "
				,"It does not limit the memory SQL Server grants to execute the query. "
				,"The memory configured for SQL Server will still be used by data cache, query execution and other consumers. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Note: Please ensure that you thoroughly test this option, before rolling it into a production environment."
				,""
				,"Scope: global or session or query"
				);


		set(2340, "Causes SQL Server not to use a sort operation (batch sort) for optimized Nested Loops joins when generating a plan. "
				,"By default, SQL Server can use an optimized Nested Loops join instead of a full scan or a Nested Loops join with an explicit Sort, "
				,"when the Query Optimizer concludes that a sort is most likely not required, but still a possibility in the event that the cardinality or cost estimates are incorrect. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Starting with SQL Server 2016 (13.x) SP1, to accomplish this at the query level, add the USE HINT 'DISABLE_OPTIMIZED_NESTED_LOOP' query hint instead of using this trace flag."
				,""
				,"Note: Please ensure that you thoroughly test this option, before rolling it into a production environment."
				,""
				,"Scope: global or session or query"
				);


		set(2371, "Changes the fixed update statistics threshold to a linear update statistics threshold. For more information, see this Microsoft Support article."
				,""
				,"Note: Starting with SQL Server 2016 (13.x) and under the database compatibility level 130 or above, this behavior is controlled by the engine and trace flag 2371 has no effect."
				,""
				,"Scope: global only"
				);


		set(2389, "Enable automatically generated quick statistics for ascending keys (histogram amendment). "
				,"If trace flag 2389 is set, and a leading statistics column is marked as ascending, then the histogram used to estimate cardinality will be adjusted at query compile time. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Note: Please ensure that you thoroughly test this option, before rolling it into a production environment."
				,""
				,"Note: This trace flag does not apply to CE version 120 or above. Use trace flag 4139 instead."
				,""
				,"Scope: global or session or query"
				);


		set(2390, "Enable automatically generated quick statistics for ascending or unknown keys (histogram amendment). "
				,"If trace flag 2390 is set, and a leading statistics column is marked as ascending or unknown, then the histogram used to estimate cardinality will be adjusted at query compile time. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Note: Please ensure that you thoroughly test this option, before rolling it into a production environment."
				,""
				,"Note: This trace flag does not apply to CE version 120 or above. Use trace flag 4139 instead."
				,""
				,"Scope: global or session or query"
				);


		set(2422, "Enables the SQL Server Database Engine to abort a request when the maximum time set by Resource Governor REQUEST_MAX_CPU_TIME_SEC configuration is exceeded. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Note: This trace flag applies to SQL Server 2016 (13.x) SP2, SQL Server 2017 (14.x) CU3, and higher builds."
				,""
				,"Scope: global"
				);


		set(2430, "Enables alternate lock class cleanup. For more information, see this Microsoft Support article."
				,""
				,"Scope: global only"
				);


		set(2451, "Enables the equivalent of the last actual execution plan in sys.dm_exec_query_plan_stats."
				,""
				,"Note: This trace flag applies to SQL Server 2019 CTP 2.4 and higher builds."
				,""
				,"Note: Starting with SQL Server 2019 CTP 2.5, to accomplish this at the database level, see the LAST_QUERY_PLAN_STATS option in ALTER DATABASE SCOPED CONFIGURATION (Transact-SQL)."
				,""
				,"Scope: global only"
				);


		set(2453, "Allows a table variable to trigger recompile when enough number of rows are changed. For more information, see this Microsoft Support article."
				,""
				,"Note: Please ensure that you thoroughly test this option, before rolling it into a production environment."
				,""
				,"Scope: global or session or query"
				);


		set(2467, "Enables an alternate parallel worker thread allocation policy, based on which node has the least allocated threads. "
				,"For more information, see Parallel Query Processing. "
				,"Refer to Configure the max worker threads Server Configuration Option for information on configuring the max worker threads server option."
				,""
				,"Note: Query degree of parallelism (DOP) has to fit into a single node for this alternate policy to be used, "
				,"or the default thread allocation policy is used instead. Using this trace flag, "
				,"it is not recommended to execute queries specifying a DOP over the number of schedulers in a single node, "
				,"as this could interfere with queries specifying a DOP below or equal to the number of schedulers in a single node."
				,""
				,"Note: Please ensure that you thoroughly test this option, before rolling it into a production environment."
				,""
				,"Scope: global only"
				);


		set(2469, "Enables alternate exchange for INSERT INTO ... SELECT into a partitioned columnstore index. For more information, see this Microsoft Support article."
				,""
				,"Scope: global or session or query"
				);


		set(2528, "Disables parallel checking of objects by DBCC CHECKDB, DBCC CHECKFILEGROUP, and DBCC CHECKTABLE. "
				,"By default, the degree of parallelism is automatically determined by the query processor. "
				,"The maximum degree of parallelism is configured just like that of parallel queries. "
				,"For more information, see Configure the max degree of parallelism Server Configuration Option."
				,""
				,"Note: Parallel DBCC checks should typically be enabled (default). "
				,"The query processor reevaluates and automatically adjusts parallelism for each table or batch of tables checked by DBCC CHECKDB."
				,""
				,"The typical use scenario is when a system administrator knows that server load will increase before DBCC CHECKDB completes, "
				,"and so chooses to manually decrease or disable parallelism, in order to increase concurrency with other user workload. However, "
				,"disabling parallel checks in DBCC CHECKDB can cause it to take longer to complete."
				,""
				,"Note: If DBCC CHECKDB is executed using the TABLOCK option and parallelism is disabled, tables may be locked for longer periods of time."
				,""
				,"Note: Starting with SQL Server 2014 (12.x) SP2, a MAXDOP option is available to override the max degree of parallelism configuration option of sp_configure for the DBCC statements."
				,""
				,"Scope: global or session"
				);


		set(2549, "Forces the DBCC CHECKDB command to assume each database file is on a unique disk drive but treating different physical files as one logical file. "
				,"DBCC CHECKDB command builds an internal list of pages to read per unique disk drive across all database files. "
				,"This logic determines unique disk drives based on the drive letter of the physical file name of each file."
				,""
				,"Note: Do not use this trace flag unless you know that each file is based on a unique physical disk."
				,""
				,"Note: Although this trace flag improve the performance of the DBCC CHECKDB commands which target usage of the PHYSICAL_ONLY option, "
				,"some users may not see any improvement in performance. "
				,"While this trace flag improves disk I/O resources usage, the underlying performance of disk resources may limit "
				,"the overall performance of the DBCC CHECKDB command. For more information, see this Microsoft Support article."
				,""
				,"Scope: global only"
				);


		set(2562, "Runs the DBCC CHECKDB command in a single 'batch' regardless of the number of indexes in the database. "
				,"By default, the DBCC CHECKDB command tries to minimize TempDB resources by limiting the number of indexes or 'facts' that it generates by using a 'batches' concept. "
				,"But this trace flag forces all processing into one batch."
				,""
				,"One effect of using this trace flag is that the space requirements for TempDB may increase. "
				,"TempDB may grow to as much as 5% or more of the user database that is being processed by the DBCC CHECKDB command."
				,""
				,"Note: Although this trace flag improve the performance of the DBCC CHECKDB commands which target usage of the PHYSICAL_ONLY option, "
				,"some users may not see any improvement in performance. While this trace flag improves disk I/O resources usage, "
				,"the underlying performance of disk resources may limit the overall performance of the DBCC CHECKDB command. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Scope: global only"
				);


		set(2566, "Runs the DBCC CHECKDB command without data purity check unless the DATA_PURITY option is specified."
				,""
				,"Note: Column-value integrity checks are enabled by default and do not require the DATA_PURITY option. "
				,"For databases upgraded from earlier versions of SQL Server, column-value checks are not enabled by default "
				,"until DBCC CHECKDB WITH DATA_PURITY has been run error free on the database at least once. "
				,"After this, DBCC CHECKDB checks column-value integrity by default. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Scope: global only"
				);


		set(3023, "Enables CHECKSUM option as default for BACKUP command. For more information, see this Microsoft Support article."
				,""
				,"Note: Starting with SQL Server 2014 (12.x) this behavior is controlled by setting the backup checksum default configuration option. "
				,"For more information, see Server Configuration Options (SQL Server)."
				,""
				,"Scope: global and session"
				);


		set(3042, "Bypasses the default backup compression pre-allocation algorithm to allow the backup file to grow only as needed to reach its final size. "
				,"This trace flag is useful if you need to save on space by allocating only the actual size required for the compressed backup. "
				,"Using this trace flag might cause a slight performance penalty (a possible increase in the duration of the backup operation). "
				,"For more information about the pre-allocation algorithm, see Backup Compression (SQL Server)."
				,""
				,"Scope: global only"
				);


		set(3051, "Enables SQL Server Backup to URL logging to a specific error log file. For more information, see SQL Server Backup to URL Best Practices and Troubleshooting."
				,""
				,"Scope: global only"
				);


		set(3205, "By default, if a tape drive supports hardware compression, either the DUMP or BACKUP statement uses it. "
				,"With this trace flag, you can disable hardware compression for tape drivers. "
				,"This is useful when you want to exchange tapes with other sites or tape drives that do not support compression."
				,""
				,"Scope: global or session"
				);


		set(3226, "By default, every successful backup operation adds an entry in the SQL Server error log and in the system event log. "
				,"If you create very frequent log backups, these success messages accumulate quickly, resulting in huge error logs in which finding other messages is problematic."
				,""
				,"With this trace flag, you can suppress these log entries. This is useful if you are running frequent log backups and if none of your scripts depend on those entries."
				,""
				,"Scope: global only"
				);


		set(3427, "Enables fix for issue when many consecutive transactions insert data into temp tables in SQL Server 2016 (13.x) "
				,"or SQL Server 2017 (14.x) consumes more CPU than in SQL Server 2014 (12.x). For more information, see this Microsoft Support article"
				,""
				,"Note: This trace flag applies to SQL Server 2016 (13.x) SP1 CU2 and higher builds. Starting with SQL Server 2017 (14.x) this trace flag has no effect."
				,""
				,"Scope: global only"
				);


		set(3459, "Disables parallel redo. For more information, see this Microsoft Support article and Microsoft Support article."
				,""
				,"Note: This trace flag applies to SQL Server 2016 (13.x) and SQL Server 2017 (14.x)."
				,""
				,"Scope: global only"
				);


		set(3468, "Disables indirect checkpoints on TempDB."
				,""
				,"Note: This trace flag applies to SQL Server 2016 (13.x) SP1 CU5, SQL Server 2017 (14.x) CU1 and higher builds."
				,""
				,"Scope: global only"
				);


		set(3608, "Prevents SQL Server from automatically starting and recovering any database except the master database. "
				,"If activities that require TempDB are initiated, then model is recovered and TempDB is created. "
				,"Other databases will be started and recovered when accessed. "
				,"Some features, such as snapshot isolation and read committed snapshot, might not work. "
				,"Use for Move System Databases and Move User Databases."
				,""
				,"Note: Do not use during normal operation."
				,""
				,"Scope: global only"
				);


		set(3625, "Limits the amount of information returned to users who are not members of the sysadmin fixed server role, "
				,"by masking the parameters of some error messages using '******'. This can help prevent disclosure of sensitive information."
				,""
				,"Scope: global only"
				);


		set(3656, "Enables symbol resolution on stack dumps when the Debugging Tools for Windows are installed. For more information, see this Microsoft Whitepaper."
				,""
				,"WARNING: This is a debugging trace flag and not meant for production environment use."
				,""
				,"Scope: global and session"
				);


		set(4136, "Disables parameter sniffing unless OPTION(RECOMPILE), WITH RECOMPILE or OPTIMIZE FOR <value> is used. For more information, see this Microsoft Support article."
				,""
				,"Starting with SQL Server 2016 (13.x), to accomplish this at the database level, see the PARAMETER_SNIFFING option in ALTER DATABASE SCOPED CONFIGURATION (Transact-SQL)."
				,""
				,"To accomplish the same result at the query level, add the OPTIMIZE FOR UNKNOWN query hint. "
				,"The OPTIMIZE FOR UNKNOWN hint doesn't disable the parameter sniffing mechanism, but effectively bypasses it to achieve the same intended result."
				,"Starting with SQL Server 2016 (13.x) SP1, a second option to accomplish this at the query level is to add the USE HINT 'DISABLE_PARAMETER_SNIFFING' query hint instead of using this trace flag."
				,""
				,"Note: Please ensure that you thoroughly test this option, before rolling it into a production environment."
				,""
				,"Scope: global or session"
				);


		set(4137, "Causes SQL Server to generate a plan using minimum selectivity when estimating AND predicates for filters to account for "
				,"partial correlation instead of independence, under the query optimizer cardinality estimation model "
				,"of SQL Server 2012 (11.x) and earlier versions (70). For more information, see this Microsoft Support article."
				,""
				,"Starting with SQL Server 2016 (13.x) SP1, to accomplish this at the query level, "
				,"add the USE HINT 'ASSUME_MIN_SELECTIVITY_FOR_FILTER_ESTIMATES' query hint instead of using this trace flag when using the CE 70."
				,""
				,"Note: Please ensure that you thoroughly test this option, before rolling it into a production environment."
				,""
				,"Note: This trace flag does not apply to CE version 120 or above. Use trace flag 9471 instead."
				,""
				,"Scope: global or session or query"
				);


		set(4138, "Causes SQL Server to generate a plan that does not use row goal adjustments with queries that contain TOP, OPTION (FAST N), IN, or EXISTS keywords. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Starting with SQL Server 2016 (13.x) SP1, to accomplish this at the query level, add the USE HINT 'DISABLE_OPTIMIZER_ROWGOAL' query hint instead of using this trace flag."
				,""
				,"Note: Please ensure that you thoroughly test this option, before rolling it into a production environment."
				,""
				,"Scope: global or session or query"
				);


		set(4139, "Enable automatically generated quick statistics (histogram amendment) regardless of key column status. "
				,"If trace flag 4139 is set, regardless of the leading statistics column status (ascending, descending, or stationary), "
				,"the histogram used to estimate cardinality will be adjusted at query compile time. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Starting with SQL Server 2016 (13.x) SP1, to accomplish this at the query level, "
				,"add the USE HINT 'ENABLE_HIST_AMENDMENT_FOR_ASC_KEYS' query hint instead of using this trace flag."
				,""
				,"Note: Please ensure that you thoroughly test this option, before rolling it into a production environment."
				,""
				,"Note: This trace flag does not apply to CE version 70. Use trace flags 2389 and 2390 instead."
				,""
				,"Scope: global or session or query"
				);


		set(4199, "Enables query optimizer (QO) fixes released in SQL Server Cumulative Updates and Service Packs."
				,""
				,"QO changes that are made to previous releases of SQL Server are enabled by default under the latest database compatibility level in a given product release, without trace flag 4199 being enabled."
				,""
				,"The following table summarizes the behavior when using specific database compatibility levels and trace flag 4199. "
				,"For more information, see this Microsoft Support article."
				,""
				,"----------------------------- -------- ------------------------------------------------------- ----------------------------------------"
				,"Database compatibility level  TF 4199  QO changes from previous database compatibility levels  QO changes for current version post-RTM"
				,"----------------------------- -------- ------------------------------------------------------- ----------------------------------------"
				,"100 to 120                    Off      Disabled                                                Disabled"
				,"                              On       Enabled                                                 Enabled"
				,"130                           Off      Enabled                                                 Disabled"
				,"                              On       Enabled                                                 Enabled"
				,"140                           Off      Enabled                                                 Disabled"
				,"                              On       Enabled                                                 Enabled"
				,"150                           Off      Enabled                                                 Disabled"
				,"                              On       Enabled                                                 Enabled"
				,""
				,""
				,"Starting with SQL Server 2016 (13.x), to accomplish this at the database level, see the QUERY_OPTIMIZER_HOTFIXES option in ALTER DATABASE SCOPED CONFIGURATION (Transact-SQL)."
				,""
				,"Starting with SQL Server 2016 (13.x) SP1, to accomplish this at the query level, add the USE HINT 'ENABLE_QUERY_OPTIMIZER_HOTFIXES' query hint instead of using this trace flag."
				,""
				,"Scope: global or session or query"
				);


		set(4610, "Increases the size of the hash table that stores the cache entries by a factor of 8. "
				,"When used together with trace flag 4618 increases the number of entries in the TokenAndPermUserStore cache store to 8,192. "
				,"For more information, see this Microsoft Support article and this Microsoft Support article."
				,""
				,"Scope: global only"
				);


		set(4616, "Makes server-level metadata visible to application roles. "
				,"In SQL Server, an application role cannot access metadata outside its own database because application roles are not associated with a server-level principal. "
				,"This is a change of behavior from earlier versions of SQL Server. "
				,"Setting this global flag disables the new restrictions, and allows for application roles to access server-level metadata."
				,""
				,"Scope: global only"
				);


		set(4618, "Limits the number of entries in the TokenAndPermUserStore cache store to 1,024. "
				,"When used together with trace flag 4610 increases the number of entries in the TokenAndPermUserStore cache store to 8,192. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Scope: global only"
				);


		set(4621, "Limits the number of entries in the TokenAndPermUserStore cache store to the number specified by the user in a registry key. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Scope: global only"
				);


		set(5004, "Pauses TDE encryption scan and causes encryption scan worker to exit without doing any work. "
				,"The database will continue to be in encrypting state (encryption in progress). "
				,"To resume re-encryption scan, disable trace flag 5004 and run ALTER DATABASE <database_name> SET ENCRYPTION ON."
				,""
				,"Scope: global only"
				);


		set(6498, "Enables more than one large query compilation to gain access to the big gateway when there is sufficient memory available. "
				,"This trace flag can be used to keep memory usage for the compilation of incoming queries under control, avoiding compilation waits for concurrent large queries. "
				,"It is based on the 80 percentage of SQL Server Target Memory, and it allows for one large query compilation per 25 gigabytes (GB) of memory. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Note: Starting with SQL Server 2014 (12.x) SP2 and SQL Server 2016 (13.x) this behavior is controlled by the engine and trace flag 6498 has no effect."
				,""
				,"Scope: global only"
				);


		set(6527, "Disables generation of a memory dump on the first occurrence of an out-of-memory exception in CLR integration. "
				,"By default, SQL Server generates a small memory dump on the first occurrence of an out-of-memory exception in the CLR. "
				,"The behavior of the trace flag is as follows:"
				,"    * If this is used as a startup trace flag, a memory dump is never generated. "
				,"      However, a memory dump may be generated if other trace flags are used."
				,"    * If this trace flag is enabled on a running server, a memory dump will not be automatically generated from that point on. "
				,"      However, if a memory dump has already been generated due to an out-of-memory exception in the CLR, this trace flag will have no effect."
				,""
				,"Scope: global only"
				);


		set(6532, "Enables performance improvement of query operations with spatial data types in SQL Server 2012 (11.x) and SQL Server 2014 (12.x). "
				,"The performance gain will vary, depending on the configuration, the types of queries, and the objects. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Note: Starting with SQL Server 2016 (13.x) this behavior is controlled by the engine and trace flag 6532 has no effect."
				,""
				,"Scope: global and session"
				);


		set(6533, "Enables performance improvement of query operations with spatial data types in SQL Server 2012 (11.x) and SQL Server 2014 (12.x). "
				,"The performance gain will vary, depending on the configuration, the types of queries, and the objects. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Note: Starting with SQL Server 2016 (13.x) this behavior is controlled by the engine and trace flag 6533 has no effect."
				,""
				,"Scope: global and session"
				);


		set(6534, "Enables performance improvement of query operations with spatial data types in SQL Server 2012 (11.x), SQL Server 2014 (12.x) and SQL Server 2016 (13.x). "
				,"The performance gain will vary, depending on the configuration, the types of queries, and the objects. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Scope: global only"
				);


		set(7314, "Forces NUMBER values with unknown precision/scale to be treated as double values with OLE DB provider. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Scope: global and session"
				);


		set(7412, "Enables the lightweight query execution statistics profiling infrastructure. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Note: This trace flag applies to SQL Server 2016 (13.x) SP1 and higher builds. "
				,"Starting with SQL Server 2019 this trace flag has no effect because lightweight profiling is enabled by default."
				,""
				,"Scope: global only"
				);


		set(7471, "Enables running multiple UPDATE STATISTICS for different statistics on a single table concurrently. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Note: This trace flag applies to SQL Server 2014 (12.x) SP1 and higher builds."
				,""
				,"Scope: global only"
				);


		set(7745, "Forces Query Store to not flush data to disk on database shutdown."
				,""
				,"Note: Using this trace may cause Query Store data not previously flushed to disk to be lost in case of shutdown. "
				,"For a SQL Server shutdown, the command SHUTDOWN WITH NOWAIT can be used instead of this trace flag to force an immediate shutdown."
				,""
				,"Scope: global only"
				);


		set(7752, "Enables asynchronous load of Query Store."
				,""
				,"Note: Use this trace flag if SQL Server is experiencing high number of QDS_LOADDB waits related to Query Store synchronous load (default behavior during database recovery)."
				,""
				,"Note: Starting with SQL Server 2019 this behavior is controlled by the engine and trace flag 7752 has no effect."
				,""
				,"Scope: global only"
				);


		set(7806, "Enables a dedicated administrator connection (DAC) on SQL Server Express. By default, no DAC resources are reserved on SQL Server Express. "
				,"For more information, see Diagnostic Connection for Database Administrators."
				,""
				,"Scope: global only"
				);


		set(8011, "Disable the ring buffer for Resource Monitor. For more information, see this Microsoft Support article."
				,""
				,"Scope: global and session"
				);


		set(8012, "Disable the ring buffer for schedulers. For more information, see this Microsoft Support article."
				,""
				,"Scope: global only"
				);


		set(8015, "Disable auto-detection and NUMA setup. For more information, see this Microsoft Support article."
				,""
				,"Scope: global only"
				);


		set(8018, "Disable the exception ring buffer. For more information, see this Microsoft Support article."
				,""
				,"Scope: global only"
				);


		set(8019, "Disable stack collection for the exception ring buffer. For more information, see this Microsoft Support article."
				,""
				,"Scope: global only"
				);


		set(8020, "Disable working set monitoring. For more information, see this Microsoft Support article."
				,""
				,"Scope: global only"
				);


		set(8032, "Reverts the cache limit parameters to the SQL Server 2005 (9.x) RTM setting which in general allows caches to be larger. "
				,"Use this setting when frequently reused cache entries do not fit into the cache and when the optimize "
				,"for ad hoc workloads Server Configuration Option has failed to resolve the problem with plan cache."
				,""
				,"WARNING: Trace flag 8032 can cause poor performance if large caches make less memory available for other memory consumers, such as the buffer pool."
				,""
				,"Scope: global only"
				);


		set(8048, "Converts NUMA partitioned memory objects into CPU partitioned. For more information, see this Microsoft Support article."
				,""
				,"Note: Starting with SQL Server 2014 (12.x) SP2 and SQL Server 2016 (13.x) this behavior is dynamic and controlled by the engine."
				,""
				,"Scope: global only"
				);


		set(8075, "Reduces VAS fragmentation when you receive memory page allocation errors on a 64-bit SQL Server 2012 (11.x) or SQL Server 2014 (12.x). "
				,"For more information, see this Microsoft Support article."
				,""
				,"Note: This trace flag applies to SQL Server 2012 (11.x), SQL Server 2014 (12.x) RTM CU10, and SQL Server 2014 (12.x) SP1 CU3. "
				,"Starting with SQL Server 2016 (13.x) this behavior is controlled by the engine and trace flag 8075 has no effect."
				,""
				,"Scope: global only"
				);


		set(8079, "Allows SQL Server 2014 (12.x) SP2 to interrogate the hardware layout and automatically configure Soft-NUMA on systems reporting 8 or more CPUs per NUMA node. "
				,"The automatic Soft-NUMA behavior is Hyperthread (HT/logical processor) aware. "
				,"The partitioning and creation of additional nodes scales background processing by increasing the number of listeners, scaling and network and encryption capabilities."
				,""
				,"Note: This trace flag applies to SQL Server 2014 (12.x) SP2. Starting with SQL Server 2016 (13.x) this behavior is controlled by the engine and trace flag 8079 has no effect."
				,""
				,"Scope: global only"
				);


		set(8207, "Enables singleton updates for Transactional Replication and CDC. Updates to subscribers can be replicated as a DELETE and INSERT pair. "
				,"This might not meet business rules, such as firing an UPDATE trigger. "
				,"With trace flag 8207, an update to a unique column that affects only one row (a singleton update) is replicated as an UPDATE and not as a DELETE or INSERT pair. "
				,"If the update affects a column on which a unique constraint exists, or if the update affects multiple rows, the update is still replicated as a DELETE or INSERT pair. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Scope: global only"
				);


		set(8721, "Reports to the error log when auto-update statistics executes. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Scope: global only"
				);


		set(8744, "Disable pre-fetching for the Nested Loop operator. "
				,"For more information, see this Microsoft Support article."
				,""
				,"WARNING: Incorrect use of this trace flag may cause additional physical reads when SQL Server executes plans that contain the Nested Loops operator."
				,""
				,"Scope: global and session"
				);


		set(9024, "Converts a global log pool memory object into NUMA node partitioned memory object. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Note: Starting with SQL Server 2012 (11.x) SP3 and SQL Server 2014 (12.x) SP1 this behavior is controlled by the engine and trace flag 9024 has no effect."
				,""
				,"Scope: global only"
				);


		set(9347, "Disables batch mode for sort operator. SQL Server 2016 (13.x) introduced a new batch mode sort operator that boosts performance for many analytical queries. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Scope: global or session or query"
				);


		set(9349, "Disables batch mode for top N sort operator. "
				,"SQL Server 2016 (13.x) introduced a new batch mode top sort operator that boosts performance for many analytical queries."
				,""
				,"Scope: global or session or query"
				);


		set(9389, "Enables additional dynamic memory grant for batch mode operators. "
				,"If a query does not get all the memory it needs, it spills data to TempDB, incurring additional I/O and potentially impacting query performance. "
				,"If the dynamic memory grant trace flag is enabled, a batch mode operator may ask for additional memory and avoid spilling to TempDB if additional memory is available. "
				,"For more information, see the Effects of min memory per query section of the Memory Management Architecture Guide."
				,""
				,"Scope: global or session"
				);


		set(9398, "Disables Adaptive Join operator that enables the choice of a Hash join or Nested Loops join method to be deferred "
				,"until the after the first input has been scanned, as introduced in SQL Server 2017 (14.x). "
				,"For more information, see this Microsoft Support article."
				,""
				,"Note: Please ensure that you thoroughly test this option, before rolling it into a production environment."
				,""
				,"Scope: global and session and query"
				);


		set(9453, "Disables batch mode execution. For more information, see this Microsoft Support article."
				,""
				,"Note: Please ensure that you thoroughly test this option, before rolling it into a production environment."
				,""
				,"Scope: global and session and query"
				);


		set(9471, "Causes SQL Server to generate a plan using minimum selectivity for single-table filters, "
				,"under the query optimizer cardinality estimation model of SQL Server 2014 (12.x) through SQL Server 2017 versions."
				,""
				,"Starting with SQL Server 2016 (13.x) SP1, to accomplish this at the query level, "
				,"add the USE HINT 'ASSUME_MIN_SELECTIVITY_FOR_FILTER_ESTIMATES' query hint instead of using this trace flag."
				,""
				,"Note: Please ensure that you thoroughly test this option, before rolling it into a production environment."
				,""
				,"Note: This trace flag does not apply to CE version 70. Use trace flag 4137 instead."
				,""
				,"Scope: global or session or query"
				);


		set(9476, "Causes SQL Server to generate a plan using the Simple Containment assumption instead of the default Base Containment assumption, "
				,"under the query optimizer cardinality estimation model of SQL Server 2014 (12.x) through SQL Server 2017 versions. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Starting with SQL Server 2016 (13.x) SP1, to accomplish this at the query level, "
				,"add the USE HINT 'ASSUME_JOIN_PREDICATE_DEPENDS_ON_FILTERS' query hint instead of using this trace flag."
				,""
				,"Note: Please ensure that you thoroughly test this option, before rolling it into a production environment."
				,""
				,"Scope: global or session or query"
				);


		set(9481, "Enables you to set the query optimizer cardinality estimation model to the SQL Server 2012 (11.x) and earlier versions, "
				,"irrespective of the compatibility level of the database. For more information, see Microsoft Support article."
				,""
				,"Starting with SQL Server 2016 (13.x), to accomplish this at the database level, "
				,"see the LEGACY_CARDINALITY_ESTIMATION option in ALTER DATABASE SCOPED CONFIGURATION (Transact-SQL)."
				,""
				,"Starting with SQL Server 2016 (13.x) SP1, to accomplish this at the query level, "
				,"add the USE HINT 'FORCE_LEGACY_CARDINALITY_ESTIMATION' query hint instead of using this trace flag."
				,""
				,"Scope: global or session or query"
				);


		set(9485, "Disables SELECT permission for DBCC SHOW_STATISTICS."
				,""
				,"Scope: global only"
				);


		set(9488, "Sets the fixed estimation for Table Valued Functions to the default of 1 (corresponding to the default under the query optimizer "
				,"cardinality estimation model of SQL Server 2008 R2 and earlier versions), when using the query optimizer cardinality "
				,"estimation model of SQL Server 2012 (11.x) through SQL Server 2017 versions."
				,""
				,"Scope: global or session or query"
				);


		set(9495, "Disables parallelism during insertion for INSERT...SELECT operations and it applies to both user and temporary tables. "
				,"For more information, see Microsoft Support article"
				,""
				,"Scope: global or session"
				);


		set(9567, "Enables compression of the data stream for Always On Availability Groups during automatic seeding. "
				,"Compression can significantly reduce the transfer time during automatic seeding and will increase the load on the processor. "
				,"For more information, see Automatically initialize Always On availability group and Tune compression for availability group."
				,""
				,"Scope: global or session"
				);


		set(9571, "Disables Availability Groups Auto seeding to the default database path. For more information see Disk Layout."
				,""
				,"Scope: global or session"
				);


		set(9591, "Disables log block compression in Always On Availability Groups. "
				,"Log block compression is the default behavior used with both synchronous and asynchronous replicas in SQL Server 2012 (11.x) and SQL Server 2014 (12.x). "
				,"In SQL Server 2016 (13.x), compression is only used with asynchronous replica."
				,""
				,"Scope: global or session"
				);


		set(9592, "Enables log stream compression for synchronous availability groups. "
				,"This feature is disabled by default on synchronous availability groups because compression adds latency. "
				,"For more information, see Tune compression for availability group."
				,""
				,"Scope: global or session"
				);


		set(9929, "Reduces the In-Memory checkpoint files to 1 MB each. For more information, see this Microsoft Support article."
				,""
				,"Scope: global only"
				);


		set(9939, "Enables parallel plans and parallel scan of memory-optimized tables and table variables in DML operations that reference memory-optimized tables or table variables, "
				,"as long as they are not the target of the DML operation in SQL Server 2016 (13.x). "
				,"For more information, see this Microsoft Support article."
				,""
				,"Note: Trace flag 9939 is not needed if trace flag 4199 is also explicitly enabled."
				,""
				,"Scope: global or session or query"
				);


		set(10204, "Disables merge/recompress during columnstore index reorganization. "
				,"In SQL Server 2016 (13.x), when a columnstore index is reorganized, there is new functionality to automatically merge any small "
				,"compressed rowgroups into larger compressed rowgroups, as well as recompressing any rowgroups that have a large number of deleted rows."
				,""
				,"Note: Trace flag 10204 does not apply to columnstore indexes which are created on memory-optimized tables."
				,""
				,"Scope: global or session"
				);


		set(10316, "Enables creation of additional indexes on internal memory-optimized staging temporal table, beside the default one. "
				,"If you have specific query pattern that includes columns which are not covered by the default index you may consider adding additional ones."
				,""
				,"Note: System-versioned temporal tables for Memory-Optimized Tables are designed to provide high transactional throughput. "
				,"Please be aware that creating additional indexes may introduce overhead for DML operations that update or delete rows in the current table. "
				,"With the additional indexes you should aim to find the right balance between performance of temporal queries and additional DML overhead."
				,""
				,"Scope: global or session"
				);


		set(11023, "Disables the use of the last persisted sample rate for all subsequent statistics update, "
				,"where a sample rate is not specified explicitly as part of the UPDATE STATISTICS statement. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Scope: global or session"
				);


		set(11024, "Enables triggering the auto update of statistics when the modification count of any partition exceeds the local threshold. "
				,"For more information, see this Microsoft Support article."
				,""
				,"Note: This trace flag applies to SQL Server 2016 (13.x) SP2, SQL Server 2017 (14.x) CU3, and higher builds."
				,""
				,"Scope: global or session"
				);
	}
}
