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


public class AseSpConfigureDictionary
{
	/** Instance variable */
	private static AseSpConfigureDictionary _instance = null;

	private HashMap<String,SpConfigureRecord> _entries = new HashMap<String,SpConfigureRecord>();

	public class SpConfigureRecord
	{
		private String _name;
		private String _description;

		public SpConfigureRecord(String name, String description)
		{
			_name        = name;
			_description = description;
		}
		
		@Override
		public String toString()
		{
			return StringUtil.left(_name, 40) + " - " + _description;
		}
	}


	public AseSpConfigureDictionary()
	{
		init();
	}

	public static AseSpConfigureDictionary getInstance()
	{
		if (_instance == null)
			_instance = new AseSpConfigureDictionary();
		return _instance;
	}

	public String getDescription(String name)
	{
		SpConfigureRecord rec = _entries.get(name.toLowerCase());
		if (rec != null)
			return rec._description;

		// Compose an empty one
		return "sp_configure option '"+name+"' not found in dictionary.";
	}


	private void set(SpConfigureRecord rec)
	{
		if ( _entries.containsKey(rec._name))
			System.out.println("sp_configure option '"+rec._name+"' already exists. It will be overwritten.");

		_entries.put(rec._name.toLowerCase(), rec);
	}

	private void set(String name, String description)
	{
		set(new SpConfigureRecord(name, description));
	}

	private void init()
	{
		/*----------------------------------------------------------------------------
		 * Below was grabbed from: https://help.sap.com/viewer/379424e5820941d0b14683dd3a992d5c/16.0.3.6/en-US/93a74e2abcb94b599aa42ccc526703eb.html
		 * at 2019-10-03
		 *----------------------------------------------------------------------------
		 */
		set("abstract plan cache"                     , "Enables caching of abstract plan hash keys.");
		set("abstract plan dump"                      , "Enables the saving of abstract plans to the 'ap_stdout' abstract plans group.");
		set("abstract plan dynamic replace"           , "The abstract plan dynamic replace configuration parameter will ignore the plan hint in the plan clause, and force the query to use the plan hint in 'sysqueryplans'");
		set("abstract plan load"                      , "Enables association of queries with abstract plans in the 'ap_stdin' abstract plans group.");
		set("abstract plan replace"                   , "Enables plan replacement for abstract plans in the 'ap_stdout' abstract plans group.");
		set("abstract plan sharing"                   , "Enables abstract plan sharing between different users.");
		set("additional network memory"               , "Sets the amount of additional memory allocated to the network memory pool.");
		set("aggressive task stealing"                , "Sets the SAP ASE scheduler task stealing policy to aggressive.");
		set("allocate max shared memory"              , "Determines whether SAP ASE allocates all the memory specified by 'max memory' at start-up, or only the amount of memory the configuration parameter requires.");
		set("allow backward scans"                    , "Controls how the optimizer performs 'select' queries that contain the 'order by...desc' command.");
		set("allow kerberos null password"            , "Enables Kerberos administrators to pass the caller�s password as null so that they can reset the login password with the 'alter login' command.");
		set("allow memory grow at startup"            , "Determines if the value for 'max memory' increases when the server starts.");
		set("allow nested triggers"                   , "Controls the use of nested triggers.");
		set("allow procedure grouping"                , "Controls the ability to group stored procedures of the same name so that they can be dropped with a single 'drop procedure' statement.");
		set("allow remote access"                     , "Controls logins from remote SAP ASE servers.");
		set("allow resource limits"                   , "Controls the use of resource limits.");
		set("allow sendmsg"                           , "Enables or disables sending messages from SAP ASE to a User Datagram Protocol (UDP) port.");
		set("allow sql server async i/o"              , "Enables SAP ASE to run with asynchronous disk I/O.");
		set("allow statement rollback"                , "Enables the server to perform a rollback, even when the query includes a 'convert' function (which can cause errors based on arithmetic overflow)");
		set("allow updates to system tables"          , "Enables users with the system administrator role to make changes to the system tables and to create stored procedures that can modify system tables.");
		set("async poll timeout"                      , "When running in threaded kernel mode, a task may hang on the function kctCheckAsync(). The task can be killed using the 'async poll timeout' configuration parameter.");
		set("average cap size"                        , "Reserved for future use.");
		set("audit queue size"                        , "Determines the size of an audit queue. The in-memory audit queue holds audit records generated by user processes until the records can be processed and written to the audit trail.");
		set("auditing"                                , "Enables or disables auditing for SAP ASE.");
		set("automatic cluster takeover"              , "Determines whether an instance automatically recovers from a cluster failure.");
		set("automatic master key access"             , "Allows SAP ASE to read the key encryption keys from the master key start-up file.");
		set("autotune rule history depth"             , "Determines the amount of history SAP ASE retains about autotuning. SAP ASE internal. Do not change.");
		set("bind backupserver address"               , "Binds the Backup Server listener address to the connection socket when performing a remote dump or load.");
		set("builtin date strings"                    , "Determines whether the server interprets strings given to chronological builtins as bigdatetimes. If a string is given as an argument in place of the chronological value, the server interprets it as a 'datetime' value regardless of its apparent precision");
		set("caps per ccb"                            , "Reserved for future use.");
		set("capture compression statistics"          , "Enables the 'monTableCompression' monitoring table to begin capturing compression statistics.");
		set("capture missing statistics"              , "Enables or disables SAP ASE to capture information about columns that have missing statistics.");
		set("check password for digit"                , "Enables or disables checking for at least one character or digit in a password. If set, this parameter does not affect existing passwords");
		set("cipc large message pool size"            , "Specifies the number of large message buffers allocated by CIPC at start-up time.");
		set("cipc regular message pool size"          , "Specifies the number of regular message buffers allocated by CIPC at start-up time.");
		set("cis bulk insert array size"              , "Determines the size of the array CIS internally buffers (and asks the Open Client bulk library to transfer as a block) when performing a bulk transfer of data from one SAP ASE to another SAP ASE.");
		set("cis bulk insert batch size"              , "Determines how many rows from the source tables are to be bulk copied into the target table as a single batch using 'select into'.");
		set("cis connect timeout"                     , "Determines the wait time, in seconds, for a successful Client-Library connection.");
		set("cis cursor rows"                         , "Specifies the cursor row count for 'cursor open' and 'cursor fetch' operations. Increasing this value means more rows are fetched in one operation, which increases speed but requires more memory.");
		set("cis idle connection timeout"             , "Configures SAP ASE to check for CIS connections to any remote server that have been unused longer than the specified number of seconds. SAP ASE deletes the unused connections and reallocates their resources.");
		set("cis packet size"                         , "Specifies the size of Tabular Data Stream (TDS) packets that are exchanged between the server and a remote server when a connection is initiated.");
		set("cis pushdown for HANA"                   , "(SAP HANA accelerator for SAP ASE only) Enables pushdowns for SQL functions and temporary tables to a SAP HANA server.");
		set("cis rpc handling"                        , "Specifies the default method for remote procedural call (RPC) handling.");
		set("cluster heartbeat interval"              , "Controls the interval that cluster instances use to send and check the heartbeat status.");
		set("cluster heartbeat retries"               , "Controls the number of times an instance retries a failed cluster heartbeat before entering failure mode.");
		set("cluster redundancy level"                , "Controls the number of recoverable failed instances in a shared-disk cluster. It is the maximum number of instances that can fail simultaneously while allowing recovery to proceed concurrently with other activity.");
		set("cluster vote timeout"                    , "Controls the maximum amount of time an instance waits for other instances to vote during the voting period. An instance waits only for those instances which it believes are running.");
		set("column default cache size"               , "Determines the size of the cache that SAP ASE must keep in memory to provide defaults for nonmaterialized columns.");
		set("compression info pool size"              , "Determines the size of the memory pool used for compression.");
		set("compression memory size"                 , "Determines the size (in 2 KB pages) of the memory pool that SAP ASE uses to decompress a compressed dump.");
		set("configuration file"                      , "Specifies the location of the configuration file currently in use.");
		set("cost of a logical io"                    , "Specifies the cost of a single logical I/O.");
		set("cost of a physical io"                   , "Specifies the cost of a single physical I/O.");
		set("cost of a cpu unit"                      , "Specifies the cost of a single CPU operation.");
		set("cpu accounting flush interval"           , "Specifies the amount of time, in machine clock ticks (non-SAP ASE clock ticks), that SAP ASE waits before flushing CPU usage statistics for each user from sysprocesses to syslogins, a procedure used in chargeback accounting.");
		set("cpu grace time"                          , "Together with 'time slice', specifies the maximum amount of time that a user process can run without yielding the CPU before SAP ASE preempts it and terminates it with a timeslice error.");
		set("current audit table"                     , "Establishes the table where SAP ASE writes audit rows.");
		set("deadlock checking period"                , "Specifies the minimum amount of time (in milliseconds) before SAP ASE initiates a deadlock check for a process that is waiting on a lock to be released.");
		set("deadlock pipe active"                    , "Controls whether SAP ASE collects deadlock messages.");
		set("deadlock pipe max messages"              , "Determines the number of deadlock messages SAP ASE stores per engine.");
		set("deadlock retries"                        , "Specifies the number of times a transaction can attempt to acquire a lock when deadlocking occurs during an index 'page split' or 'shrink'.");
		set("decompression row threshold"             , "Determines the maximum number of columns in a table that are uncompressed using column decompression. The server uses row decompression if the number of columns is larger than this value.");
		set("default character set id"                , "Specifies the number of the default character set used by the server.");
		set("default database size"                   , "Sets the default number of megabytes allocated to a new user database if create database is issued without any size parameters.");
		set("default exp_row_size percent"            , "Reserves space for expanding updates in data-only-locked tables, to reduce row forwarding.");
		set("default fill factor percent"             , "Determines how full SAP ASE makes each index page when it is creating a new index on existing data, unless the fill factor is specified in the 'create index' statement.");
		set("default language id"                     , "Specifies the number of the language that is used to display system messages unless a user has chosen another language from those available on the server. us_english always has an ID of NULL.");
		set("default network packet size"             , "Configures the default packet size for all SAP ASE users.");
		set("default sortorder id"                    , "Specifies the number of the sort order that is installed as the default on the server.");
		set("default unicode sortorder"               , "Specifies a string parameter that uniquely defines the default Unicode sort order installed on the server.");
		set("default xml sortorder"                   , "Specifies a string parameter that defines the sort order used by the XML engine. A string parameter is used rather than a numeric parameter to guarantee a unique ID.");
		set("deferred name resolution"                , "Determines whether deferred name resolution is applied globally to server connections.");
		set("disable character set conversions"       , "Enables or disables character set conversion for data moving between clients and SAP ASE.");
		set("disable disk mirroring"                  , "Enables or disables disk mirroring for SAP ASE.");
		set("disable jsagent core dump"               , "Enables or disables JS Agent core dumps.");
		set("disable varbinary truncation"            , "Controls whether SAP ASE includes trailing zeros at the end of 'varbinary' or 'binary' null data.");
		set("disk i/o structures"                     , "Specifies the initial number of disk I/O control blocks SAP ASE allocates at start-up.");
		set("dma object pool size"                    , "Specifies the number of DMA (direct memory access) objects allocated by CIPC at start-up time.");
		set("dtm detach timeout period"               , "Sets the amount of time, in minutes, that a distributed transaction branch can remain in the detached state.");
		set("dtm lock timeout period"                 , "Sets the maximum amount of time, in seconds, that a distributed transaction branch waits for lock resources to become available.");
		set("dump history filename"                   , "Specifies the path and name of your dump history file.");
		set("dump on conditions"                      , "Determines whether SAP ASE generates a dump of data in shared memory when it encounters the conditions specified in 'maximum dump conditions'.");
		set("dynamic allocation on demand"            , "Determines when memory is allocated for changes to dynamic memory configuration parameters.");
		set("dynamic sql plan pinning"                , "Improves performance by reducing the time spent by server connections waiting for access to the query plan manager.");
		set("early row send increment"                , "Configures the additional number of rows sent in the second and subsequent packets of a result set (subject to the maximum packet size).");
		set("enable async database init"              , "Ensures that all 'create database' and 'alter database' commands initialize databases asynchronously by default");
		set("enable backupserver ha"                  , "Enables or disables the high availability Backup Server for the cluster.");
		set("enable buffered io for load"             , "Enables Backup Server to open the database devices using buffered I/O at load time, regardless of the settings for devices being configured to use DSYNC or DIRECTIO.");
		set("enable bulk inserts"                     , "Allows you to perform bulk inserts with the 'merge' command.");
		set("enable cis"                              , "Enables or disables Component Integration Service.");
		set("enable compression"                      , "Enables or disables data compression.");
		set("enable concurrent dump tran"             , "Enables or disables SAP ASE to use concurrent dumps.");
		set("enable console logging"                  , "Determines whether SAP ASE sends messages to the console.");
		set("enable deferred parallel"                , "Configures the optimizer to generate parallel plans based on the best serial plan.");
		set("enable delta dump tran"                  , "Ensures that two sequential transaction dumps do not contain duplicate pages.");
		set("enable dtm"                              , "Enables or disables the SAP ASE distributed transaction management (DTM) feature.");
		set("enforce dump configuration"              , "Enables dump operations to use a dump configuration.");
		set("enable dump history"                     , "Determines whether there are updates to the dump history file at the end of the database dump operation.");
		set("enable encrypted columns"                , "Enables and disables encrypted columns.");
		set("enable file access"                      , "Enables access through proxy tables to the external file system.");
		set("enable full-text search"                 , "Enables Enhanced Full-Text Search services.");
		set("enable functionality group"              , "Enables or disables the changes available for specific features from SAP ASE version 15.7 and later.");
		set("enable granular permissions"             , "Enables or disables grantable system privileges that allow you to enforce separation of duties.");
		set("enable ha"                               , "Enables or disables SAP ASE as a companion server in an active-active high availability subsystem.");
		set("enable HCB index"                        , "Enables or disables SAP ASE for hash-cached BTree indexes.");
		set("enable housekeeper gc"                   , "Configures the housekeeper task.");
		set("enable hp posix async i/o"               , "Enables or disables asynchronous I/O on database devices created on HP-UX 11.31 and later file systems.");
		set("enable hugepages"                        , "Enables and disables the use of huge pages on Linux platforms that support huge pages.");
		set("enable i/o fencing"                      , "Enables or disables I/O fencing for each database device that supports the SCSI-3 Persistent Group Reservation (PGR) standard.");
		set("enable inline default sharing"           , "Enables SAP ASE to share inline defaults.");
		set("enable in-memory row storage"            , "Enables SAP ASE to use in-memory row storage.");
		set("enable ISM"                              , "Enables and disables SAP ASE to use intimate shared memory (ISM) on the Solaris platform.");
		set("enable java"                             , "Enables and disables Java in the SAP ASE database. You cannot install Java classes or perform any Java operations until the server is enabled for Java.");
		set("enable job scheduler"                    , "Determines whether Job Scheduler starts when the SAP ASE server starts.");
		set("enable js restart logging"               , "Enables or disables diagnostics logging after the restart of Job Scheduler.");
		set("enable large chunk elc"                  , "Enables or disables large allocation in the engine local cache.");
		set("enable large pool for load"              , "Configures the use of large buffer pools during the recovery phase for load database and load transaction commands.");
		set("enable ldap user auth"                   , "Enables or disables SAP ASE to authenticate each user on the LDAP server.");
		set("enable LFB index"                        , "Enables or disables Latch-Free Indexes feature (part of the Mem Scale licensable option).");
		set("enable lightweight rvm"                  , "Enables and disables (RVM) Reference Validation Mechanism checks to determine if there are no permission changes between executions of the same compiled plans, streamlining the query execution path.");
		set("enable literal autoparam"                , "Enables and disables literal server-wide parameterization.");
		set("enable lock remastering"                 , "Allows an SAP ASE Cluster Edition background process to locally manage locks once failed-over client connections move back to the original instance.");
		set("enable logins during recovery"           , "Determines whether non-system administrator logins are allowed during database recovery.");
		set("enable mem scale"                        , "Enables or disables the Mem Scale licensable option.");
		set("enable merge join"                       , "Enables or disables merge join at the server level.");
		set("enable metrics capture"                  , "Enables SAP ASE to capture metrics at the server level.");
		set("enable monitoring"                       , "Controls whether SAP ASE collects the monitoring table data.");
		set("enable pam user auth"                    , "Controls the ability to authenticate users using pluggable authentication modules (PAM).");
		set("enable pci"                              , "Enables or disables the Java PCI Bridge for SAP ASE.");
		set("enable permissive unicode"               , "Enables or disables SAP ASE to ignore Unicode noncharacters.");
		set("enable predicated privileges"            , "Enables or disables predicated privileges.");
		set("enable query tuning mem limit"           , "Enables or disables the query tuning memory limit.");
		set("enable query tuning time limit"          , "Enables or disables the query tuning time limit.");
		set("enable rapidlog"                         , "Enables the capture of diagnostic for 'Proc Cache Header' memory pool messages.");
		set("enable rapidtimer"                       , "Enables the collection of SAP ASE diagnostic information. Instructions for 'enable rapidtimer' are provided by SAP Technical Support.");
		set("enable real time messaging"              , "Enables or disables the real time messaging services.");
		set("enable rep agent threads"                , "Enables or disables the RepAgent thread within SAP ASE.");
		set("enable resolve as owner"                 , "Enables the resolve as owner functionality, which allows users to view data from objects they do not own, and without including the owner name prefix with the object.");
		set("enable row level access control"         , "Enables or disables row-level access control.");
		set("enable select into in tran"              , "Enables and disables the ability to use the 'select into' command in a multistatement transaction.");
		set("enable semantic partitioning"            , "Enables or disables partitioning other than round-robin (for example list, hash, and range partitioning) in SAP ASE.");
		set("enable sort-merge join and jtc"          , "Enables or disables the query processor to select a sort merge or a nested loop join when SAP ASE compiles a query in compatibility mode.");
		set("enable sql debugger"                     , "Enables or disables the SAP ASE SQL debugger, which allows you to step through your T-SQL code.");
		set("enable ssl"                              , "Enables or disables Secure Sockets Layer session-based security.");
		set("enable sticky statistics"                , "Allows you to disable stickiness for 'update statistics'.");
		set("enable stmt cache monitoring"            , "Enables or disables SAP ASE to collect monitoring information about the statement cache.");
		set("enable streamlined parallel"             , "Reduces the default parallel plan search space, avoids bad parallel plans, and focuses the parallel optimizer on a more promising parallel plan search space.");
		set("enable surrogate processing"             , "Enables or disables the processing and maintains the integrity of surrogate pairs in Unicode data.");
		set("enable transactional memory"             , "Enables or disables the Transactional Memory feature (part of Mem Scale licensable option).");
		set("enable unicode conversion"               , "Enables or disables character conversion using Unilib for the char, varchar, and text datatypes.");
		set("enable unicode normalization"            , "Enables or disables Unilib character normalization.");
		set("enable utility lvl 0 scan wait"          , "Enables or disables running 'alter table ... add | drop partition' commands while Adaptive Server runs isolation level 0 scans.");
		set("enable webservices"                      , "Enables or disables Webservices.");
		set("enable workload analyzer"                , "Enables and disables the workload analyzer.");
		set("enable xact coordination"                , "Enables or disables SAP ASE transaction coordination services.");
		set("enable xml"                              , "Enables or disables XML services.");
		set("engine local cache percent"              , "Enables or disables modification of the Engine Local Cache (ELC) as a percentage of procedure cache.");
		set("engine memory log size"                  , "For diagnostic use only and has no relevance in a production environment. Keep all default settings unless otherwise requested by SAP Technical Support.");
		set("errorlog pipe active"                    , "Controls whether SAP ASE collects error log messages.");
		set("errorlog pipe max messages"              , "Determines the number of error log messages SAP ASE stores per engine.");
		set("errorlog size"                           , "Sets the threshold for the size of the error log. Once this threshold is reached, SAP ASE dynamically closes the current error log and opens a new one.");
		set("esp execution priority"                  , "Sets the priority of the XP Server thread for ESP execution.");
		set("esp execution stacksize"                 , "Sets the size of the stack, in bytes, to be allocated for ESP execution.");
		set("esp unload dll"                          , "Specifies whether DLLs that support ESPs should be automatically unloaded from XP Server memory after the ESP call has completed.");
		set("event buffers per engine"                , "Controls the number of monitor event buffers. Not used in the current version of SAP ASE.");
		set("event log computer name"                 , "Specifies the name of the Windows machine that logs SAP ASE messages in its Windows Event Log. This feature is available on Windows servers only.");
		set("event logging"                           , "Enables and disables the logging of SAP ASE messages in the Windows Event Log.");
		set("executable codesize + overhead"          , "A calculated value that is not user-configurable, this parameter exports the combined size, in kilobytes, of the SAP ASE executable and overhead.");
		set("extended cache size"                     , "Specifies the size of the secondary cache.");
		set("extend implicit conversion"              , "Controls the use of implicit conversion.");
		set("external keystore"                       , "SAP ASE can use the external keystore to encrypt or decrypt master encryption keys. external keystore enables SAP ASE to store key externally");
		set("fips login password encryption"          , "Provides FIPS 140-2 cryptographic module support for encrypting passwords in transmission, in memory, and on disk.");
		set("FM enabled"                              , "Set or reset by the Fault Manager to indicate to SAP ASE that it, and the HADR setup, is being monitored. Do not change the value of FM enabled unless you are explicitly told to by the HADR product documentation or by SAP Technical Support.");
		set("global async prefetch limit"             , "Specifies the percentage of a buffer pool that can hold the pages brought in by asynchronous prefetch that have not yet been read.");
		set("global cache partition number"           , "Sets the default number of cache partitions for all data caches.");
		set("HADR connect timeout"                    , "Determines the wait time for the successful connection between primary SAP ASE, standby SAP ASE, and SAP ASE on the DR site in an HADR environment.");
		set("HADR distinct server name"               , "Enables the server to append 'DR' to the server name for the HADR_MEMBER server class entries in sysservers. For example, a server named 'PARIS' is listed as 'PARISDR' in sysservers.");
		set("HADR login stall time"                   , "The amount of time (in seconds) the standby server delays before sending a redirect list when it detects that the other server is not in 'primary active' mode.");
		set("HADR mode"                               , "Determines the mode of the instance.");
		set("HADR primary check frequency"            , "Determines how often, in seconds, the standby server checks the primary server�s HADR mode and state.");
		set("HADR primary wait time"                  , "The amount of time, in seconds, the standby server continues to send the redirect list to the clients in absence of primary server before failing the connection.");
		set("HADR remote query timeout"               , "Determines the wait time for the successful query while retrieving the HADR state of the primary SAP ASE from standby SAP ASE or SAP ASE on the DR node.");
		set("hash table hit rate threshold"           , "Sets the percentage threshold for HCB auto tuning hash table scan hit rate.");
		set("HCB index auto tuning"                   , "Enables and disables hash-cache BTree index auto tuning.");
		set("HCB index memory pool size"              , "Determines the size of the memory pool available for hash-cached BTree indexes.");
		set("HCB index tuning interval"               , "Determines the interval (in minutes) after which the HCB index tuning task wakes and tunes the hash-cached B-tree index.");
		set("heap memory per user"                    , "Configures the amount of heap memory per user. A heap memory pool is an internal memory created at startup that tasks use to dynamically allocate memory as needed.");
		set("histogram tuning factor"                 , "Controls the number of steps SAP ASE analyzes per histogram for 'update statistics', 'update index statistics', 'update all statistics', and 'create index'.");
		set("housekeeper free write percent"          , "Specifies the maximum percentage by which the housekeeper wash task can increase database writes.");
		set("i/o accounting flush interval"           , "Specifies the amount of time, in machine clock ticks, that SAP ASE waits before flushing I/O statistics for each user from sysprocesses to syslogins. This is used for charge-back accounting.");
		set("i/o batch size"                          , "Sets the number of writes issued in a batch before the task goes to sleep. Once this batch is completed, the task is woken up, and the next batch of writes are issued, ensuring that the I/O subsystem is not flooded with many simultaneous writes.");
		set("i/o polling process count"               , "Specifies the maximum number of processes that SAP ASE can run before the scheduler checks for disk and network I/O completions. Tuning 'i/o polling process count' affects both the response time and throughput of SAP ASE.");
		set("identity burning set factor"             , "Changes the percentage of potential column values that is made available in a block of column values.");
		set("identity grab size"                      , "Allows each SAP ASE process to reserve a block of IDENTITY column values for inserts into tables that have an IDENTITY column.");
		set("identity reservation size"               , "Sets a limit for the number of identity values.");
		set("idle migration timeout"                  , "Specifies the amount of time after which an idle connection is closed without invalidating the migration request sent to the client, allowing you to stop an instance after a specified period of time without waiting for idle client connections to migrate.");
		set("imrs cache utilization"                  , "Specifies a threshold, as a percentage, for the size of row_storage cache bound to an IMRS- or on disk MVCCenabled database. The server employs heuristics internally to maintain the row storage cache to the value specified by 'imrs cache utilization'.");
		set("inline table functions"                  , "Inlines those inline table UDFs by tranforming them into the parameterized views internally during the execution.");
		set("job scheduler interval"                  , "Sets the interval when the Job Scheduler checks which scheduled jobs are due to be executed.");
		set("job scheduler memory"                    , "Determines the size of the memory pool (which is of type bucketpool) assigned to the Job Scheduler.");
		set("job scheduler tasks"                     , "Sets the maximum number of jobs that can run simultaneously through Job Scheduler.");
		set("js heartbeat interval"                   , "Specifies the intervals between two JS Agent heartbeat checks, in minutes.");
		set("js job output width"                     , "Determines the line width the output uses for jobs stored in the 'js_output' table.");
		set("js restart delay"                        , "Sets the delay period between two Job Scheduler auto restart attempts after abnormal shutdown of Job Scheduler.");
		set("kernel mode"                             , "Determines the mode the SAP ASE kernel uses, threaded or process.");
		set("kernel resource memory"                  , "Determines the size, in 2K pages, of the kernel resource memory pool from which all thread pools and other kernel resources are allocated memory.");
		set("large allocation auto tune"              , "Configures SAP ASE preallocate large amounts of memory for query execution, which reduces procedure cache contention.");
		set("LFB memory size"                         , "Determines the size of the Latch-Free B-tree (LFB) bucket pool.");
		set("license information"                     , "Allows SAP system administrators to monitor the number of user licenses used in SAP ASE. Enabling this parameter only monitors the number of licenses issued; it does not enforce the license agreement.");
		set("lock address spinlock ratio"             , "Sets the number of rows in the internal address locks hash table that are protected by one spinlock for SAP ASEs running with multiple engines.");
		set("lock hashtable size"                     , "Specifies the number of hash buckets in the lock hash table.");
		set("lock scheme"                             , "Sets the default locking scheme to be used by 'create table' and 'select into' commands when a lock scheme is not specified in the command.");
		set("lock shared memory"                      , "Disallows swapping of SAP ASE pages to disk and allows the operating system kernel to avoid the server's internal page locking code. This can reduce disk reads, which are expensive.");
		set("lock spinlock ratio"                     , "For SAP ASEs running with multiple engines, sets a ratio that determines the number of lock hash buckets that are protected by one spinlock. If you increase the value for 'lock hashtable size', the number of spinlocks increases, so the number of hash buckets protected by one spinlock remains the same.");
		set("lock table spinlock ratio"               , "For SAP ASEs running with multiple engines, sets the number of rows in the internal table locks hash table that are protected by one spinlock.");
		set("lock timeout pipe active"                , "Controls whether SAP ASE collects lock timeout messages.");
		set("lock timeout pipe max messages"          , "Controls the maximum number of rows per engine in the lock timeout pipe, which determines the maximum number of rows that can be returned by the monLockTimeout monitoring table.");
		set("lock wait period"                        , "Limits the number of seconds that tasks wait to acquire a lock on a table, data page, or data row. If the task does not acquire the lock within the specified time period, SAP ASE returns error message 12205 to the user and rolls back the transaction.");
		set("log audit logon failure"                 , "Specifies whether to log unsuccessful SAP ASE logins to the SAP ASE error log and, on Windows servers, to the Windows Event Log, if event logging is enabled.");
		set("log audit logon success"                 , "Specifies whether to log successful SAP ASE logins to the SAP ASE error log and, on Windows servers, to the Windows Event Log, if event logging is enabled.");
		set("max async i/os per engine"               , "Specifies the maximum number of outstanding asynchronous disk I/O requests for a single engine at one time.");
		set("max async i/os per server"               , "Specifies the maximum number of asynchronous disk I/O requests that can be outstanding for SAP ASE at one time.");
		set("max buffers per lava operator"           , "Sets an upper limit for the number of buffers used by lava operators that perform sorting or hashing (which are 'expensive' in terms of processing). Lava operators use buffers from the session�s tempdb data cache pool as a work area for processing rows.");
		set("max cis remote connections"              , "Specifies the maximum number of concurrent Client-Library connections that can be made to remote servers by Component Integration Services.");
		set("max concurrently recovered db"           , "Determines the degree of parallelism.");
		set("max js restart attempts"                 , "Restricts the number of restart attempts and prevents the Job Scheduler restart feature from going into an infinite loop.");
		set("max memory"                              , "Specifies the maximum amount of total physical memory that you can configure SAP ASE to allocate. 'max memory' must be greater than the total logical memory consumed by the current configuration of SAP ASE.");
		set("max native threads per engine"           , "Defines the maximum number of native threads the server spawns per engine. (note: 'max native threads per engine' is ignored in threaded mode)");
		set("max nesting level"                       , "Sets the maximum nesting level for stored procedures and triggers. Each increased nesting level requires about 160 bytes of additional memory. For example, if you increase the nesting level from 16 to 26, SAP ASE requires an additional 1600 bytes of memory.");
		set("max network packet size"                 , "Specifies the maximum network packet size that can be requested by clients communicating with SAP ASE.");
		set("max network peek depth"                  , "Specifies how many levels deep SAP ASE peeks into a connections operating system receive buffer for a pending cancel.");
		set("max number network listeners"            , "Specifies the maximum number of network listeners allowed by SAP ASE at one time.");
		set("max number of IN elements"               , "Limits the number of elements in the largest 'in' clause in a query. If the limit is crossed, SAP ASE reports an error.");
		set("max online engines"                      , "Places an upper limit of the number of engine threads that can be brought online. It does not take into account the number of CPUs available at start-up, and allows users to add CPUs at a later date.");
		set("max online q engines"                    , "(Process mode only) Specifies the maximum number of Q engines you can have online, and is required for MQ.");
		set("max parallel degree"                     , "Specifies the server-wide maximum number of worker processes allowed per query. This is called the 'maximum degree of parallelism'.");
		set("max pci slots"                           , "Sets the maximum number of PCI slots SAP ASE allows.");
		set("max query parallel degree"               , "(Used when SAP ASE is in compatibility mode) Defines the number of worker processes to use for a given query.");
		set("max repartition degree"                  , "Configures the amount of dynamic repartitioning SAP ASE requires, which enables SAP ASE to use horizontal parallelism. However, if the number of partitions is too large, the system is flooded with worker processes that compete for resources, which degrades performance.");
		set("max resource granularity"                , "Indicates the maximum percentage of the system�s resources a query can use.");
		set("max scan parallel degree"                , "Specifies the server-wide maximum degree of parallelism for hash-based scans.");
		set("max sql text monitored"                  , "Specifies the amount of memory allocated per user connection for saving SQL text to memory shared by Adaptive Server Monitor.");
		set("max transfer history"                    , "Controls how many transfer history entries SAP ASE retains in the spt_TableTransfer table in each database.");
		set("max utility parallel degree"             , "Specifies the server-wide maximum number of worker processes allowed per query used by the 'create index with consumers' and 'update stats with consumers' commands.");
		set("maximum dump conditions"                 , "Sets the maximum number of conditions you can specify under which SAP ASE generates a dump of data in shared memory.");
		set("maximum failed logins"                   , "Allows you to set the server-wide maximum number of failed login attempts for logins and roles.");
		set("maximum job output"                      , "Sets limit, in bytes, on the maximum output a single job can produce.");
		set("memory alignment boundary"               , "Determines the memory address boundary on which data caches are aligned.");
		set("memory dump compression level"           , "Controls the compression level for shared memory dumps.");
		set("memory per worker process"               , "Specifies the amount of memory, in bytes, used by worker processes.");
		set("messaging memory"                        , "Configures the amount of memory available for SAP messaging.");
		set("metrics elap max"                        , "Configures maximum elapsed time and thresholds for QP metrics.");
		set("metrics exec max"                        , "Configures maximum execution time and thresholds for QP metrics.");
		set("metrics lio max"                         , "Configures maximum logical I/O and thresholds for QP metrics.");
		set("metrics pio max"                         , "Configures maximum physical I/O and thresholds for QP metrics.");
		set("min pages for parallel scan"             , "Controls the number of tables and indexes that SAP ASE can access in parallel. If the number of pages in a table is below the value you set, the table is accessed serially. min pages for parallel scan does not consider page size. If SAP ASE accesses the indexes and tables, SAP ASE attempts to repartition the data, if that is appropriate, and to use parallelism above the scans, if that is appropriate.");
		set("minimum password length"                 , "Allows you to customize the length of server-wide password values or per-login or per-role password values.");
		set("mnc_full_index_filter"                   , "Prevents SAP ASE from considering noncovered indexes that do not have a limiting search argument at the server level, if there is a column in the index or a predicate that does not have a histogram.");
		set("msg confidentiality reqd"                , "Requires that all messages into and out of SAP ASE be encrypted.");
		set("msg integrity reqd"                      , "Requires that all messages be checked for data integrity.");
		set("net password encryption reqd"            , "Restricts login authentication to use only RSA encryption algorithm or the SAP proprietary algorithm.");
		set("network polling mode"                    , "Configures the SAP ASE network polling mode. 'threaded' = SAP ASE spawns a separate thread for each network task configured that performs polling. 'inline' =  one of the engines performs the polling. 'compact' =  each engine creates its own network controller to perform its polling. You should set 'network polling mode' to compact when there are multiple engine groups, and the load is distributed across the engines.");
		set("nonpushdown pipe active"                 , "(SAP HANA accelerator for SAP ASE only) Determines if accelerator for SAP ASE collects historical, nonpushdown statement information for SQL statements sent to the SAP HANA server.");
		set("nonpushdown pipe max messages"           , "(SAP HANA accelerator for SAP ASE only) Determines the maximum number of messages that can be stored in the monHANANonPushdown monitoring table for historical nonpushdown statements for SQL statements sent to the SAP HANA server.");
		set("number of alarms"                        , "Specifies the number of alarm structures allocated by SAP ASE.");
		set("number of aux scan descriptors"          , "Sets the number of auxiliary scan descriptors available in a pool shared by all users on a server.");
		set("number of backup connections"            , "Sets the maximum number of user connections Backup Server establishes to dump or load in-memory databases.");
		set("number of ccbs"                          , "Reserved for future use.");
		set("number of checkpoint tasks"              , "Configures parallel checkpoints.");
		set("number of devices"                       , "Controls the number of database devices SAP ASE can use. It does not include devices used for database or transaction log dumps.");
		set("number of disk tasks"                    , "Controls the number of tasks dedicated to polling and completing disk I/Os.");
		set("number of dtx participants"              , "Sets the total number of remote transactions that the SAP ASE transaction coordination service can propagate and coordinate simultaneously.");
		set("number of dump threads"                  , "Controls the number of threads that SAP ASE spawns to perform a memory dump.");
		set("number of early send rows"               , "Configures the number of rows that are sent to the client in the first packet of a new result set.");
		set("number of engines at startup"            , "Is used exclusively during start-up to set the number of engines brought online. NOTE: When configured for threaded mode, SAP ASE ignores the 'number of engines at startup' configuration parameter. In threaded mode, SAP ASE uses the size of the defined thread pools to determine the number of online engines at startup.");
		set("number of hcb gc tasks per db"           , "Determines the number of garbage collection tasks assigned to reclaim unused memory from the hash nodes.");
		set("number of histogram steps"               , "Specifies the number of steps in a histogram.");
		set("number of imrs gc tasks per db"          , "Determines the default number of IMRS garbage collector tasks for an IMRS-enabled databases or when ondisk MVCC-enabled databases are brought online.");
		set("number of index trips"                   , "Specifies the number of times an aged index page traverses the most recently used/least recently used (MRU/LRU) chain before it is considered for swapping out.");
		set("number of java sockets"                  , "Enables the Java VM and the java.net classes that SAP supports.");
		set("number of large i/o buffers"             , "Sets the number of allocation unit-sized buffers reserved for performing large I/O for certain SAP ASE utilities.");
		set("number of lob gc tasks per db"           , "Determines the default number of LOB garbage collector tasks for an IMRS- or on-disk MVCC-enabled databases.");
		set("number of locks"                         , "Sets the total number of available locks for all users on SAP ASE.");
		set("number of mailboxes"                     , "Specifies the number of mailbox structures allocated by SAP ASE.");
		set("number of messages"                      , "Specifies the number of message structures allocated by SAP ASE.");
		set("number of network tasks"                 , "Controls the number of tasks dedicated to polling and completing network I/Os. Note: 'number of network tasks' functions only when 'network polling mode' is set to threaded.");
		set("number of oam trips"                     , "Specifies the number of times an object allocation map (OAM) page traverses the MRU/LRU chain before it is considered for swapping out. The higher the value, the longer that aged OAM pages stay in cache.");
		set("number of open databases"                , "Sets the maximum number of databases that can be open simultaneously on SAP ASE.");
		set("number of open indexes"                  , "Sets the maximum number of indexes that can be used simultaneously on SAP ASE.");
		set("number of open objects"                  , "Sets the maximum number of objects that can be open simultaneously on SAP ASE. Note: It's not only tables that uses this, for example 'lwp' used by the 'statement cache' and 'dynamic sql' is also using 'open objects'.");
		set("number of open partitions"               , "Specifies the number of partitions that SAP ASE can access at one time.");
		set("number of pack tasks per db"             , "Determines the number of 'imrs_pack' threads per IMRS or on-disk MVCC-enabled database. Increasing the value for 'number of pack tasks per db' also increases the amount of memory the server uses. 'number of pack tasks per db' is dynamic when it is increased. That is, if you increase its value, all existing IMRS-enabled databases are immediately assigned more pack tasks. However, decreasing its value is a static action, and requires restarting the server.");
		set("number of pre-allocated extents"         , "Specifies the number of extents (eight pages) allocated in a single trip to the page manager. Currently, this parameter is used only by bcp to improve performance when copying in large amounts of data. By default, bcp allocates two extents at a time and writes an allocation record to the log each time.");
		set("number of q engines at startup"          , "Specifies the number of Q engines that are online when the server starts, a requirement for MQ.");
		set("number of reexecutions"                  , "Specifies the maximum number of internal re-executions of the DMLs the query processor can attempt when a statement running at 'statement snapshot' isolation level receives 'write' conflicts. Setting the 'number of reexecutions' to -1 indicates that the number of reexecutions is unlimited. The server aborts the statement with error 16873 when it receives more than specified number of write conflicts.");
		set("number of remote connections"            , "Specifies the number of logical connections that can simultaneously be open to and from an SAP ASE.");
		set("number of remote logins"                 , "Controls the number of active user connections from SAP ASE to remote servers.");
		set("number of remote sites"                  , "Determines the maximum number of remote sites that can simultaneously access SAP ASE.");
		set("number of sort buffers"                  , "Specifies the amount of memory allocated for buffers used to hold pages read from input tables and perform index merges during sorts. 'number of sort buffers' is used only for parallel sorting.");
		set("number of user connections"              , "Sets the maximum number of user connections that can simultaneously be connected to SAP ASE. It does not refer to the maximum number of processes; that number depends not only on the value of this parameter but also on other system activity.");
		set("number of worker processes"              , "Specifies the maximum number of worker processes that SAP ASE can use at any one time for all simultaneously running parallel queries.");
		set("NVCache Lazy Cleaner Pool Size"          , "Determines the size of the NV Cache pool cleaner.");
		set("o/s file descriptors"                    , "Indicates the maximum per-process number of file descriptors configured for your operating system. This parameter is read-only and cannot be configured through SAP ASE.");
		set("object lockwait timing"                  , "Controls whether SAP ASE collects timing statistics for requests of locks on objects.");
		set("open index hash spinlock ratio"          , "(Multiprocessing systems only) Sets the number of index metadata descriptor hash tables that are protected by one spinlock.");
		set("open index spinlock ratio"               , "Specifies the number of index metadata descriptors that are protected by one spinlock.");
		set("open object spinlock ratio"              , "(Multiprocessing systems only) Specifies the number of object descriptors that are protected by one spinlock.");
		set("optimization goal"                       , "Determines which optimization goal SAP ASE uses.");
		set("optimize temp table resolution"          , "Allows stored procedures that reference temporary tables created outside the procedure to not require recompiling for each execution.");
		set("optimization timeout limit"              , "Specifies the amount of time, as a fraction of the estimated execution time of the query, that SAP ASE can spend optimizing a query.");
		set("optimize dump for fast load"             , "Optimizes 'dump database' commands for a faster 'load database' time.");
		set("optimizer level"                         , "Determines the level of optimization the query processor uses.");
		set("page lock promotion hwm"                 , "'page lock promotion hwm' (high-water mark), with 'page lock promotion lwm' (low-water mark) and 'page lock promotion pct' (percentage), specifies the number of page locks permitted during a single scan session of a page-locked table or index before SAP ASE attempts to escalate from page locks to a table lock.");
		set("page lock promotion lwm"                 , "'page lock promotion lwm' (low-water mark), with 'page lock promotion hwm' (high-water mark) and 'page lock promotion pct', specify the number of page locks permitted during a single scan session of a page locked table or an index before SAP ASE attempts to promote from page locks to a table lock.");
		set("page lock promotion pct"                 , "Sets the percentage of page locks (based on the table size) above which SAP ASE attempts to acquire a table lock.");
		set("page utilization percent"                , "Is used during page allocations to control whether SAP ASE scans a table�s object allocation map (OAM) to find unused pages or simply allocates a new extent to the table.");
		set("partition groups"                        , "Specifies the maximum number of partition groups that can be allocated by SAP ASE.");
		set("partition spinlock ratio"                , "For SAP ASE servers running with multiple engines, sets the number of rows in the partition descriptors that are protected by one spinlock.");
		set("pci memory size"                         , "Sets the size of the pluggable component interface (PCI) memory pool.");
		set("per object statistics active"            , "Controls whether SAP ASE collects statistics for each object.");
		set("percent database for history"            , "Specifies the percentage of the total space available in sybmgmtdb that is reserved for the js_history table.");
		set("percent database for output"             , "Specifies the percentage of the total space available in sybmgmtdb that is reserved for job output.");
		set("percent history free"                    , "Specifies the percentage of reserved space in sybmgmtdb to be kept free.");
		set("percent output free"                     , "Specifies the percentage of reserved space kept free in sybmgmtdb that is reserved for Job Scheduler output.");
		set("performance monitoring option"           , "Enables the license for the BMC DBXray graphical performance monitoring and diagnostic tool.");
		set("permission cache entries"                , "Determines the number of cache protectors per task, increasing the amount of memory for each user connection and worker process. Information about user permissions is held in the permission cache. When SAP ASE checks permissions, it looks first in the permission cache; if it does not find what it needs, it looks in the sysprotects table. This process is significantly faster if SAP ASE finds the information it needs in the permission cache and does not have to read sysprotects.");
		set("physical lock cushion"                   , "'physical lock cushion' allows you to allocate a sufficient number of locks to prevent out-of-lock conditions, which can occur during instance startup, shutdown, failover, or on badly partitioned environments.");
		set("plan text pipe active"                   , "Determines whether SAP ASE collects query plan text.");
		set("plan text pipe max messages"             , "Determines the number of query plan text messages SAP ASE stores per engine.");
		set("point query rate threshold"              , "Sets the percentage threshold for HCB auto tuning point query rate. A point query rate below the value of 'point query rate threshold' indicates index hash caching may offer little improvement because few queries will benefit from using a hash table, and may cause the HCB auto tuning task to disable index hash caching on those indexes.");
		set("prevent automatic upgrade"               , "Allows you to prevent an upgrade that is triggered when starting an older SAP ASE installation (without the --upgrade-ok parameter) with a newer version of the SAP ASE dataserver binary.");
		set("print deadlock information"              , "Prints deadlock information to the error log.");
		set("print recovery information"              , "Determines what information SAP ASE displays on the console during recovery. (Recovery is performed on each database at SAP ASE start-up and when a database dump is loaded.)");
		set("procedure cache size"                    , "Specifies the size of the procedure cache, in 2 KB pages.");
		set("procedure deferred compilation"          , "Enables or disables compiling statements that reference local variables or temporary tables inside a stored procedure until execution time, so that the optimization of those statements can use runtime values, instead of estimations.");
		set("process wait events"                     , "Controls whether SAP ASE collect statistics for each wait event for every task. You can get wait information for a specific task using monProcessWaits");
		set("prod-consumer overlap factor"            , "Affects optimization. SAP ASE changes the group by algorithm, and you cannot use set statistics I/O with parallel plans.");
		set("quorum heartbeat interval"               , "Specifies the number of seconds between quorum heartbeats.");
		set("quorum heartbeat retries"                , "Specifies the number of times an instance attempts to detect a quorum heartbeat before determining that the quorum device is no longer running, and exiting.");
		set("quoted identifier enhancements"          , "Enables and disables quoted identifiers use in SAP ASE.");
		set("rapidlog buffer size"                    , "Specifies the buffer size for the output of diagnostic for measuring 'Proc Cache Header' performance.");
		set("rapidlog max files"                      , "Specifies the maximum number of files for the output of diagnostic for measuring 'Proc Cache Header' performance.");
		set("read committed with lock"                , "Determines whether an SAP ASE using transaction isolation level 1 (read committed) holds shared locks on rows or pages of data-only-locked tables during select queries.");
		set("recovery interval in minutes"            , "Sets the maximum number of minutes per database that SAP ASE uses to complete its recovery procedures in case of a system failure.");
		set("recovery prefetch size"                  , "Sets the look-ahead size (in numbers of log records) to be used by the recovery prefetch scan. Set to 0 if the scan is to determine the look-ahead size dynamically, or to a value > 0 if the look-ahead size is to be set to a specific number of log records to look-ahead.");
		set("remote server pre-read packets"          , "Determines the number of packets that are pre-read by a site handler during connections with remote servers.");
		set("replication agent memory size"           , "Determines the amount of memory that SAP ASE allocates to the RepAgent thread pool for a multithreaded RepAgent");
		set("restore database options"                , "Restores the database options that are set by 'create database', 'alter database', and 'sp_dboption' when you load a database or transaction dump.");
		set("restricted decrypt permission"           , "Enables or disables restricted decrypt permission in all databases. You must have the sso_role to set this parameter.");
		set("row lock promotion hwm"                  , "'row lock promotion hwm' (high-water mark), with 'row lock promotion lwm' (low-water mark) and 'row lock promotion pct' specifies the maximum number of row locks permitted during a single scan session of a table or an index before SAP ASE attempts to escalate from row locks to a table lock.");
		set("row lock promotion lwm"                  , "'row lock promotion lwm' (low-water mark), with the 'row lock promotion hwm' (high-water mark) and 'row lock promotion pct' specifies the number of row locks permitted during a single scan session of a table or an index before SAP ASE attempts to promote from row locks to a table lock.");
		set("row lock promotion pct"                  , "If the number of locks held on an object is between 'row lock promotion lwm' (low-water mark) and 'row lock promotion hwm' (high-water mark), 'row lock promotion pct' sets the percentage of row locks (based on the number of rows in the table) above which SAP ASE attempts to acquire a table lock.");
		set("rtm thread idle wait period"             , "Defines the time, in seconds, a native thread used by SAP ASE waits when it has no work to do. When the time set for a native thread is reached, the thread automatically fades out.");
		set("rules file"                              , "Specifies the location of the rules file, which is used by the autotuning feature. SAP ASE internal. Do not change.");
		set("runnable process search count"           , "Specifies the number of times an engine loops while looking for a runnable task before relinquishing the CPU to the operating system. NOTE: 'runnable process search count' functions only when you configure SAP ASE for process kernel mode; it is nonfunctional for threaded kernel mode. Use 'alter thread pool <pool_name> with idle timeout = ##' instead.");
		set("sampling percent"                        , "Is the numeric value of the sampling percentage, such as 5 for 5 percent, 10 for 10 percent, and so on.");
		set("scavenge temp objects"                   , "Enables or disables the server from scavenging temporary tables from LRU and MRU page chains.");
		set("secure default login"                    , "Specifies a default login for all users who are preauthenticated but who do not have a login in master..syslogins");
		set("select for update"                       , "Enables SAP ASE to exclusively lock rows for subsequent updates within the same transaction, and for updatable cursors, preventing other concurrent tasks from updating these rows and from blocking the subsequent update.");
		set("select on syscomments.text"              , "Enables protection of the text of database objects through restriction of the select permission on the text column of the syscomments table.");
		set("send doneinproc tokens"                  , "Enables or disables SAP ASE for sending doneinproc packets (these are TDS messages that are sent after various statements, in particular, non-select statements like insert, update, and so on).");
		set("session migration timeout"               , "Specifies the amount of time available for a client to complete a migration by connecting to the target instance. If the client does not migrate to the target instance in the time allotted, SAP ASE fails the connection.");
		set("session tempdb log cache size"           , "Specifies the size for each session tempdb log cache.");
		set("shared memory starting address"          , "Determines the virtual address where SAP ASE starts its shared memory region. It is unlikely that you will ever reconfigure this option; do so only after consulting with SAP Technical Support.");
		set("show deferred compilation text"          , "Enables and disables SAP ASE to display the text of deferred compilation statements as they run.");
		set("sigstack csmd min size"                  , "Configures the minimum amount of stack that is required to handle an address violation signal without stack overflow and captures a configured shared memory dump (CSMD).");
		set("sigstack min size"                       , "Configures the minimum amount of stack that is required to handle an address violation signal without overflowing the stack.");
		set("simplified native access plan"           , "Enables or disables just-in-time compilation of lava execution plans into native code, so that SAP ASE can then invoke these native code plans directly in subsequent executions, allowing for faster execution of extreme online transaction processing (XOLTP) queries. This compiled queries feature is available as part of MemScale licensed option.");
		set("size of auto identity column"            , "Sets the precision of IDENTITY columns that are automatically created with the sp_dboption 'auto identity' and 'unique auto_identity index' options.");
		set("size of global fixed heap"               , "Specifies the memory space for internal data structures and other needs.");
		set("size of process object heap"             , "Specifies the total memory space for all processes using the Java VM.");
		set("size of shared class heap"               , "Specifies the shared memory space for all Java classes that are called into the Java VM. SAP ASE maintains the shared class heap server-wide for both user-defined and system-provided Java classes.");
		set("size of unilib cache"                    , "Specifies the memory used in bytes rounded up to the nearest 1K in addition to the minimum overhead size, which provides enough memory to load a single copy of the largest Unilib conversion table plus the largest Unilib sort table.");
		set("solaris async i/o mode"                  , "Allows you to select various asynchronous IO modes.");
		set("sproc optimize timeout limit"            , "Specifies the amount of time SAP ASE can spend optimizing a stored procedure as a fraction of the estimated execution time.");
		set("sql batch capture"                       , "Controls whether SAP ASE collects SQL text.");
		set("sql perfmon integration"                 , "Enables and disables the ability to monitor SAP ASE statistics from the Windows Performance Monitor.");
		set("sql server clock tick length"            , "Specifies the duration of the server�s clock tick, in microseconds.");
		set("sql text pipe active"                    , "Controls whether SAP ASE collects SQL text.");
		set("sql text pipe max messages"              , "Specifies the number of SQL text messages SAP ASE stores per engine.");
		set("stack guard size"                        , "Sets the size, in bytes, of the stack guard area, which is an overflow stack of configurable size at the end of each stack.");
		set("stack size"                              , "Specifies the size, in bytes, of the execution stacks used by each user process on SAP ASE.");
		set("start xp server during boot"             , "Determines whether XP Server starts when SAP ASE starts.");
		set("startup delay"                           , "Controls when RepAgent is started during the server start.");
		set("statement cache size"                    , "Increases the server allocation of procedure cache memory and limits the amount of memory from the procedure cache pool used for cached statements.");
		set("statement pipe active"                   , "Controls whether SAP ASE collects statement-level statistics.");
		set("statement pipe max messages"             , "Determines the number of statement statistics messages SAP ASE stores per engine.");
		set("statement statistics active"             , "Controls whether SAP ASE collects monitoring table statement-level statistics.");
		set("streamlined dynamic sql"                 , "Enables the statement cache to store dynamic SQL statements.");
		set("strict dtm enforcement"                  , "Determines whether or not SAP ASE transaction coordination services strictly enforce the ACID properties (atomicity, consistency, integrity, and durability) of distributed transactions.");
		set("suppress js max task message"            , "Prevents SAP ASE from printing the Job Scheduler js maxtask error messages to the error log.");
		set("suspend audit when device full"          , "Determines what SAP ASE does when an audit device becomes completely full.");
		set("syb_sendmsg port number"                 , "Specifies the port number that SAP ASE uses to send messages to a User Datagram Protocol (UDP) port with 'sp_sendmsg' or 'syb_sendmsg'.");
		set("sysstatistics flush interval"            , "Determines the length of the interval, in minutes, between flushes of sysstatistics.");
		set("systemwide password expiration"          , "Sets the number of days that passwords remain in effect after they are changed.");
		set("tape retention in days"                  , "Specifies the number of days you intend to retain each tape after it has been used for either a database or a transaction log dump. This parameter can keep you from accidentally overwriting a dump tape.");
		set("tcp no delay"                            , "Controls Transmission Control Protocol (TCP ) packet batching. The default value means that TCP packets are not batched.");
		set("text prefetch size"                      , "Limits the number of pages of text, unitext, and image data that can be prefetched into an existing buffer pool.");
		set("threshold event max messages"            , "Determines the number of events SAP ASE stores in the monThresholdEvent table. Once the number of events in the monThresholdEvent monitoring table exceed this value, SAP ASE overwrites the oldest unread events with new events.");
		set("threshold event monitoring"              , "Enable or disables SAP ASE from recording threshold events.");
		set("time slice"                              , "Sets the number of milliseconds that the SAP ASE scheduler allows a task to run.");
		set("total data cache size"                   , "Reports the amount of memory, in kilobytes, that is currently available for data, index, and log pages. This parameter is a calculated value that is not directly user-configurable.");
		set("total logical memory"                    , "Displays the total logical memory for the current configuration of SAP ASE. The total logical memory is the amount of memory that the SAP ASE current configuration uses. 'total logical memory' displays the memory that is required to be available, but which may or may not be in use at any given moment. For information about the amount of memory in use at a given moment, see 'total physical memory'. You cannot use 'total logical memory' to set any of the memory configuration parameters.");
		set("total physical memory"                   , "Is a read-only configuration parameter that displays the total physical memory for the current configuration of SAP ASE. The total physical memory is the amount of memory that SAP ASE is using at a given moment in time. Configure SAP ASE so that the value for 'max memory' is larger than the value for 'total logical memory', and the value for 'total logical memory' is larger than the value for 'total physical memory'.");
		set("transfer utility memory size"            , "SAP ASE maintains a memory pool for the 'transfer table' command and for tables marked for incremental transfer. This pool provides memory for maintaining state information about current and past transfers, and for memory used to write to and read from transfer files. 'transfer utility memory size' determines the size of this memory pool.");
		set("txn to pss ratio"                        , "Determines the total number of transaction descriptors that are available to the server.");
		set("unified login required"                  , "Requires that all users who log in to SAP ASE be authenticated by a security mechanism.");
		set("update statistics hashing"               , "Enables SAP ASE to gather hash-based statistics.");
		set("upgrade version"                         , "Reports the version of the upgrade utility that upgraded your master device. The upgrade utility checks and modifies this parameter during an upgrade. WARNING: Do not reset upgrade version. Doing so may cause serious problems with SAP ASE.");
		set("use security services"                   , "Specifies that SAP ASE uses network-based security services.");
		set("user log cache queue size"               , "Determines whether a queuing strategy is used for logging. The user log cache is divided into multiple cachelets or not based on the value you set in this configuration parameter.");
		set("user log cache size"                     , "Specifies the size, in bytes, for each user�s log cache. Its size is determined by the server�s logical page size.");
		set("user log cache spinlock ratio"           , "For SAP ASE servers running with multiple engines, 'user log cache spinlock ratio' specifies the ratio of user log caches per user log cache spinlock. There is one user log cache for each configured user connection");
		set("utility statistics hashing"              , "'utility statistics hashing' enables the gathering of index attributes using hash-based statistics when creating an index.");
		set("wait event timing"                       , "Controls whether SAP ASE collects statistics for individual wait events.");
		set("wait on uncommitted insert"              , "Allows you to control the wait behavior of select, update, insert, and delete commands for an uncommitted insert. NOTE: Only on DOL tables. ");
		set("workload manager cache size"             , "Specifies the maximum amount of memory, in 2 KB pages, that the workload manager can use.");
		set("xact coordination interval"              , "Defines the length of time between attempts to resolve transaction branches that have been propagated to remote servers.");
		set("xp_cmdshell context"                     , "Sets the security context for the operating system command to be executed using the xp_cmdshell system ESP.");
	}
}
