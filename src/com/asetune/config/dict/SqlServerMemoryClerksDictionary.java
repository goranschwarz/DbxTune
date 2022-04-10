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

import java.util.HashMap;

import com.asetune.utils.StringUtil;

public class SqlServerMemoryClerksDictionary
{
	/** Instance variable */
	private static SqlServerMemoryClerksDictionary _instance = null;

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


	public SqlServerMemoryClerksDictionary()
	{
		init();
	}

	public static SqlServerMemoryClerksDictionary getInstance()
	{
		if (_instance == null)
			_instance = new SqlServerMemoryClerksDictionary();
		return _instance;
	}

	/**
	 * Strips out all HTML and return it as a "plain" text
	 * @param name
	 * @return
	 */
	public String getDescriptionPlain(String name)
	{
		TypeRecord rec = _types.get(name);
		if (rec != null)
			return StringUtil.stripHtml(rec._description);

		// Compose an empty one
		return "";
//		return "WaitName '"+name+"' not found in dictionary.";
	}


	public String getDescriptionHtml(String name)
	{
		TypeRecord rec = _types.get(name);
		if (rec != null)
			return rec._description;

		// Compose an empty one
		return "<html><code>"+name+"</code> not found in dictionary.</html>";
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
		add("CACHESTORE_BROKERDSH"                 , "<html>This cache store is used to store allocations by Service Broker Dialog Security Header Cache</html>");
		add("CACHESTORE_BROKERKEK"                 , "<html>This cache store is used to store allocations by Service Broker Key Exchange Key Cache</html>");
		add("CACHESTORE_BROKERREADONLY"            , "<html>This cache store is used to store allocations by Service Broker Read Only Cache</html>");
		add("CACHESTORE_BROKERRSB"                 , "<html>This cache store is used to store allocations by Service Broker Remote Service Binding Cache.</html>");
		add("CACHESTORE_BROKERTBLACS"              , "<html>This cache store is used to store allocations by Service Broker for security access structures.</html>");
		add("CACHESTORE_BROKERTO"                  , "<html>This cache store is used to store allocations by Service Broker Transmission Object Cache</html>");
		add("CACHESTORE_BROKERUSERCERTLOOKUP"      , "<html>This cache store is used to store allocations by Service Broker user certificates lookup cache</html>");
		add("CACHESTORE_COLUMNSTOREOBJECTPOOL"     , "<html>This cache store is used for allocations by Columnstore Indexes for segments and dictionaries</html>");
		add("CACHESTORE_CONVPRI"                   , "<html>This cache store is used to store allocations by Service Broker to keep track of Conversations priorities</html>");
		add("CACHESTORE_EVENTS"                    , "<html>This cache store is used to store allocations by Service Broker Event Notifications</html>");
		add("CACHESTORE_FULLTEXTSTOPLIST"          , "<html>This memory clerk is used for allocations by Full-Text engine for stoplist functionality.</html>");
		add("CACHESTORE_NOTIF"                     , "<html>This cache store is used for allocations by Query Notification functionality</html>");
		add("CACHESTORE_OBJCP"                     , "<html>This cache store is used for caching objects with compiled plans (CP): stored procedures, functions, triggers. To illustrate, after a query plan for a stored procedure is created, its plan is stored in this cache.</html>");
		add("CACHESTORE_PHDR"                      , "<html>This cache store is used for temporary memory caching during parsing for views, constraints, and defaults algebrizer trees during compilation of a query. Once query is parsed, the memory should be released. Some examples include: many statements in one batch - thousands of inserts or updates into one batch, a T-SQL batch that contains a large dynamically generated query, a large number of values in an IN clause.</html>");
		add("CACHESTORE_QDSRUNTIMESTATS"           , "<html>This cache store is used to cache Query Store runtime statistics</html>");
		add("CACHESTORE_SEARCHPROPERTYLIST"        , "<html>This cache store is used for allocations by Full-Text engine for Property List Cache</html>");
		add("CACHESTORE_SEHOBTCOLUMNATTRIBUTE"     , "<html>This cache store is used by storage engine for caching Heap or B-Tree (HoBT) column metadata structures.</html>");
		add("CACHESTORE_SQLCP"                     , "<html>This cache store is used for caching ad hoc queries, prepared statements, and server-side cursors in plan cache. Ad hoc queries are commonly language-event T-SQL statements submitted to the server without explicit parameterization. Prepared statements also use this cache store - they are submitted by the application using API calls like SQLPrepare()/ SQLExecute (ODBC) or SqlCommand.Prepare/SqlCommand.ExecuteNonQuery (ADO.NET) and will appear on the server as sp_prepare/sp_execute or sp_prepexec system procedure executions. Also, server-side cursors would consume from this cache store (sp_cursoropen, sp_cursorfetch, sp_cursorclose).</html>");
		add("CACHESTORE_STACKFRAMES"               , "<html>This cache store is used for allocations of internal SQL OS structures related to stack frames.</html>");
		add("CACHESTORE_SYSTEMROWSET"              , "<html>This cache store is used for allocations of internal structures related to transaction logging and recovery.</html>");
		add("CACHESTORE_TEMPTABLES"                , "<html>This cache store is used for allocations related to temporary tables and table variables caching - part of plan cache.</html>");
		add("CACHESTORE_VIEWDEFINITIONS"           , "<html>This cache store is used for caching view definitions as part of query optimization.</html>");
		add("CACHESTORE_XML_SELECTIVE_DG"          , "<html>This cache store is used to cache XML structures for XML processing.</html>");
		add("CACHESTORE_XMLDBATTRIBUTE"            , "<html>This cache store is used to cache XML attribute structures for XML activity like XQuery.</html>");
		add("CACHESTORE_XMLDBELEMENT"              , "<html>This cache store is used to cache XML element structures for XML activity like XQuery.</html>");
		add("CACHESTORE_XMLDBTYPE"                 , "<html>This cache store is used to cache XML structures for XML activity like XQuery.</html>");
		add("CACHESTORE_XPROC"                     , "<html>This cache store is used for caching structures for Extended Stored procedures (Xprocs) in plan cache.</html>");
		add("MEMORYCLERK_BACKUP"                   , "<html>This memory clerk is used for various allocations by Backup functionality</html>");
		add("MEMORYCLERK_BHF"                      , "<html>This memory clerk is used for allocations for binary large objects (BLOB) management during query execution (Blob Handle support)</html>");
		add("MEMORYCLERK_BITMAP"                   , "<html>This memory clerk is used for allocations by SQL OS functionality for bitmap filtering</html>");
		add("MEMORYCLERK_CSILOBCOMPRESSION"        , "<html>This memory clerk is used for allocations by Columnstore Index binary large objects (BLOB) Compression</html>");
		add("MEMORYCLERK_DRTLHEAP"                 , "<html>This memory clerk is used for allocations by SQL OS functionality<br>Applies to: SQL Server 2019 (15.x) and later</html>");
		add("MEMORYCLERK_EXPOOL"                   , "<html>This memory clerk is used for allocations by SQL OS functionality<br>Applies to: SQL Server 2019 (15.x) and later</html>");
		add("MEMORYCLERK_EXTERNAL_EXTRACTORS"      , "<html>This memory clerk is used for allocations by query execution engine for batch mode operations<br>Applies to: SQL Server 2019 (15.x) and later</html>");
		add("MEMORYCLERK_FILETABLE"                , "<html>This memory clerk is used for various allocations by FileTables functionality.</html>");
		add("MEMORYCLERK_FSAGENT"                  , "<html>This memory clerk is used for various allocations by FILESTREAM functionality.</html>");
		add("MEMORYCLERK_FSCHUNKER"                , "<html>This memory clerk is used for various allocations by FILESTREAM functionality for creating filestream chunks.</html>");
		add("MEMORYCLERK_FULLTEXT"                 , "<html>This memory clerk is used for allocations by Full-Text engine structures.</html>");
		add("MEMORYCLERK_FULLTEXT_SHMEM"           , "<html>This memory clerk is used for allocations by Full-Text engine structures related to Shared memory connectivity with the Full Text Daemon process.</html>");
		add("MEMORYCLERK_HADR"                     , "<html>This memory clerk is used for memory allocations by AlwaysOn functionality</html>");
		add("MEMORYCLERK_HOST"                     , "<html>This memory clerk is used for allocations by SQL OS functionality.</html>");
		add("MEMORYCLERK_LANGSVC"                  , "<html>This memory clerk is used for allocations by SQL T-SQL statements and commands (parser, algebrizer, etc.)</html>");
		add("MEMORYCLERK_LWC"                      , "<html>This memory clerk is used for allocations by Full-Text Semantic Search engine</html>");
		add("MEMORYCLERK_POLYBASE"                 , "<html>This memory clerk keeps track of memory allocations for Polybase functionality inside SQL Server.</html>");
		add("MEMORYCLERK_QSRANGEPREFETCH"          , "<html>This memory clerk is used for allocations during query execution for query scan range prefetch.</html>");
		add("MEMORYCLERK_QUERYDISKSTORE"           , "<html>This memory clerk is used by Query Store memory allocations inside SQL Server.</html>");
		add("MEMORYCLERK_QUERYDISKSTORE_HASHMAP"   , "<html>This memory clerk is used by Query Store memory allocations inside SQL Server.</html>");
		add("MEMORYCLERK_QUERYDISKSTORE_STATS"     , "<html>This memory clerk is used by Query Store memory allocations inside SQL Server.</html>");
		add("MEMORYCLERK_QUERYPROFILE"             , "<html>This memory clerk is used for during server startup to enable query profiling<br>Applies to: SQL Server 2019 (15.x) and later</html>");
		add("MEMORYCLERK_RTLHEAP"                  , "<html>This memory clerk is used for allocations by SQL OS functionality.<br>Applies to: SQL Server 2019 (15.x) and later</html>");
		add("MEMORYCLERK_SECURITYAPI"              , "<html>This memory clerk is used for allocations by SQL OS functionality.<br>Applies to: SQL Server 2019 (15.x) and later</html>");
		add("MEMORYCLERK_SERIALIZATION"            , "<html>Internal use only</html>");
		add("MEMORYCLERK_SLOG"                     , "<html>This memory clerk is used for allocations by sLog (secondary in-memory log stream) in Accelerated Database Recovery<br>Applies to: SQL Server 2019 (15.x) and later</html>");
		add("MEMORYCLERK_SNI"                      , "<html>This memory clerk allocates memory for the Server Network Interface (SNI) components. SNI manages connectivity and TDS packets for SQL Server</html>");
		add("MEMORYCLERK_SOSMEMMANAGER"            , "<html>This memory clerk allocates structures for SQLOS (SOS) thread scheduling and memory and I/O management..</html>");
		add("MEMORYCLERK_SOSNODE"                  , "<html>This memory clerk allocates structures for SQLOS (SOS) thread scheduling and memory and I/O management.</html>");
		add("MEMORYCLERK_SOSOS"                    , "<html>This memory clerk allocates structures for SQLOS (SOS) thread scheduling and memory and I/O management..</html>");
		add("MEMORYCLERK_SPATIAL"                  , "<html>This memory clerk is used by Spatial Data components for memory allocations.</html>");
		add("MEMORYCLERK_SQLBUFFERPOOL"            , "<html>This memory clerk keeps track of commonly the largest memory consumer inside SQL Server - data and index pages. Buffer Pool or data cache keeps data and index pages loaded in memory to provide fast access to data. For more information, see Buffer Management.</html>");
		add("MEMORYCLERK_SQLCLR"                   , "<html>This memory clerk is used for allocations by SQLCLR .</html>");
		add("MEMORYCLERK_SQLCLRASSEMBLY"           , "<html>This memory clerk is used for allocations for SQLCLR assemblies.</html>");
		add("MEMORYCLERK_SQLCONNECTIONPOOL"        , "<html>This memory clerk caches information on the server that the client application may need the server to keep track of. One example is an application that creates prepare handles via sp_prepexecrpc. The application should properly unprepare (close) those handles after execution.</html>");
		add("MEMORYCLERK_SQLEXTENSIBILITY"         , "<html>This memory clerk is used for allocations by the Extensibility Framework for running external Python or R scripts on SQL Server.<br>Applies to: SQL Server 2019 (15.x) and later</html>");
		add("MEMORYCLERK_SQLGENERAL"               , "<html>This memory clerk could be used by multiple consumers inside SQL engine. Examples include replication memory, internal debugging/diagnostics, some SQL Server startup functionality, some SQL parser functionality, building system indexes, initialize global memory objects, Create OLEDB connection inside the server and Linked Server queries, Server-side Profiler tracing, creating showplan data, some security functionality, compilation of computed columns, memory for Parallelism structures, memory for some XML functionality</html>");
		add("MEMORYCLERK_SQLHTTP"                  , "<html>Deprecated</html>");
		add("MEMORYCLERK_SQLLOGPOOL"               , "<html>This memory clerk is used by SQL Server Log Pool. Log Pool is a cache used to improve performance when reading the transaction log. Specifically it improves log cache utilization during multiple log reads, reduces disk I/O log reads and allows sharing of log scans. Primary consumers of log pool are AlwaysOn (Change Capture and Send), Redo Manager, Database Recovery - Analysis/Redo/Undo, Transaction Runtime Rollback, Replication/CDC, Backup/Restore.</html>");
		add("MEMORYCLERK_SQLOPTIMIZER"             , "<html>This memory clerk is used for memory allocations during different phases of compiling a query. Some uses include query optimization, index statistics manager, view definitions compilation, histogram generation.</html>");
		add("MEMORYCLERK_SQLQERESERVATIONS"        , "<html>This memory clerk is used for Memory Grant allocations, that is memory allocated to queries to perform sort and hash operations during query execution. For more information on Query Execution reservations (memory grants), see this blog</html>");
		add("MEMORYCLERK_SQLQUERYCOMPILE"          , "<html>This memory clerk is used by Query optimizer for allocating memory during query compiling.</html>");
		add("MEMORYCLERK_SQLQUERYEXEC"             , "<html>This memory clerk is used for allocations in the following areas: Batch mode processing, Parallel query execution, query execution context, spatial index tessellation, sort and hash operations (sort tables, hash tables), some DVM processing, update statistics execution</html>");
		add("MEMORYCLERK_SQLQUERYPLAN"             , "<html>This memory clerk is used for allocations by Heap page management, DBCC CHECKTABLE allocations, and sp_cursor* stored procedure allocations</html>");
		add("MEMORYCLERK_SQLSERVICEBROKER"         , "<html>This memory clerk is used by SQL Server Service Broker memory allocations.</html>");
		add("MEMORYCLERK_SQLSERVICEBROKERTRANSPORT", "<html>This memory clerk is used by SQL Server Service Broker transport memory allocations.</html>");
		add("MEMORYCLERK_SQLSLO_OPERATIONS"        , "<html>This memory clerk is used to gather performance statistics<br>Applies to: Azure SQL Database</html>");
		add("MEMORYCLERK_SQLSOAP"                  , "<html>Deprecated</html>");
		add("MEMORYCLERK_SQLSOAPSESSIONSTORE"      , "<html>Deprecated</html>");
		add("MEMORYCLERK_SQLSTORENG"               , "<html>This memory clerk is used for allocations by multiple storage engine components. Examples of components include structures for database files, database snapshot replica file manager, deadlock monitor, DBTABLE structures, Log manager structures, some tempdb versioning structures, some server startup functionality, execution context for child threads in parallel queries.</html>");
		add("MEMORYCLERK_SQLTRACE"                 , "<html>This memory clerk is used for server-side SQL Trace memory allocations.</html>");
		add("MEMORYCLERK_SQLUTILITIES"             , "<html>This memory clerk can be used by multiple allocators inside SQL Server. Examples include Backup and Restore, Log Shipping, Database Mirroring, DBCC commands, BCP code on the server side, some query parallelism work, Log Scan buffers.</html>");
		add("MEMORYCLERK_SQLXML"                   , "<html>This memory clerk is used for memory allocations when performing XML operations.</html>");
		add("MEMORYCLERK_SQLXP"                    , "<html>This memory clerk is used for memory allocations when calling SQL Server Extended Stored procedures.</html>");
		add("MEMORYCLERK_SVL"                      , "<html>This memory clerk is used used for allocations of internal SQL OS structures</html>");
		add("MEMORYCLERK_TEST"                     , "<html>Internal use only</html>");
		add("MEMORYCLERK_UNITTEST"                 , "<html>Internal use only</html>");
		add("MEMORYCLERK_WRITEPAGERECORDER"        , "<html>This memory clerk is used for allocations by Write Page Recorder.</html>");
		add("MEMORYCLERK_XE"                       , "<html>This memory clerk is used for Extended Events memory allocations</html>");
		add("MEMORYCLERK_XE_BUFFER"                , "<html>This memory clerk is used for Extended Events memory allocations</html>");
		add("MEMORYCLERK_XLOG_SERVER"              , "<html>This memory clerk is used for allocations by Xlog used for log file management in SQL Azure Database<br>Applies to: Azure SQL Database</html>");
		add("MEMORYCLERK_XTP"                      , "<html>This memory clerk is used for In-Memory OLTP memory allocations.</html>");
		add("OBJECTSTORE_LBSS"                     , "<html>This object store is used to allocate temporary LOBs - variables, parameters, and intermediate results for expressions. An example that uses this store is table-valued parameters (TVP) . See the KB article 4468102 and KB article 4051359 for more information on fixes in this space.</html>");
		add("OBJECTSTORE_LOCK_MANAGER"             , "<html>This memory clerk keeps track of allocations made by the Lock Manager in SQL Server.</html>");
		add("OBJECTSTORE_SECAUDIT_EVENT_BUFFER"    , "<html>This object store is used for SQL Server Audit memory allocations.</html>");
		add("OBJECTSTORE_SERVICE_BROKER"           , "<html>This object store is used by Service Broker</html>");
		add("OBJECTSTORE_SNI_PACKET"               , "<html>This object store is used by Server Network Interface (SNI) components which manage connectivity</html>");
		add("OBJECTSTORE_XACT_CACHE"               , "<html>This object store is used to cache transactions information</html>");
		add("USERSTORE_DBMETADATA"                 , "<html>This object store is used for metadata structures</html>");
		add("USERSTORE_OBJPERM"                    , "<html>This store is used for structures keeping track of object security/permission</html>");
		add("USERSTORE_QDSSTMT"                    , "<html>This cache store is used to cache Query Store statements</html>");
		add("USERSTORE_SCHEMAMGR"                  , "<html>Schema manager cache stores different types of metadata information about the database objects in memory (e.g tables). A common user of this store could be the tempdb database with objects like tables, temp procedures, table variables, table-valued parameters, worktables, workfiles, version store.</html>");
		add("USERSTORE_SXC"                        , "<html>This user store is used for allocations to store all RPC parameters.</html>");
		add("USERSTORE_TOKENPERM"                  , "<html>TokenAndPermUserStore is a single SOS user store that keeps track of security entries for security context, login, user, permission, and audit. Multiple hash tables are allocated to store these objects.</html>");
	}

}
