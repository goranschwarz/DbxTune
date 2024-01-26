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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.config.dict;

import java.lang.invoke.MethodHandles;

import javax.naming.NameNotFoundException;

import com.asetune.sql.conn.DbxConnection;


public class MonTablesDictionaryPostgres
extends MonTablesDictionaryDefault
{
    /** Log4j logging. */
//	private static Logger _logger          = Logger.getLogger(MonTablesDictionaryPostgres.class);

	@Override
	public void initialize(DbxConnection conn, boolean hasGui)
	{
		// TODO Auto-generated method stub
		super.initialize(conn, hasGui);

		initExtraMonTablesDictionary();
	}

	
	/**
	 * NO, do not save MonTableDictionary in PCS
	 */
	@Override
	public boolean isSaveMonTablesDictionaryInPcsEnabled()
	{
		return false;
	}
	
	/**
	 * Add some information to the MonTablesDictionary<br>
	 * This will serv as a dictionary for ToolTip
	 */
	public static void initExtraMonTablesDictionary()
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			
			if (mtd == null)
				return;

			// NOTE: the below information on all pg_stat_* tables are copied from: http://www.postgresql.org/docs/current/static/monitoring-stats.html
//TODO; update all below tables to reflect Postgres 15

			//---------------------------------------------------------------------------------------------------------------
			// pg_stat_activity
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_stat_activity",            "One row per server process, showing information related to the current activity of that process, such as state and current query. See pg_stat_activity for details.");

			mtd.addColumn("pg_stat_activity", "datid",            "OID of the database this backend is connected to");
			mtd.addColumn("pg_stat_activity", "datname",          "Name of the database this backend is connected to");
			mtd.addColumn("pg_stat_activity", "pid",              "Process ID of this backend");
			mtd.addColumn("pg_stat_activity", "leader_pid",       "Process ID of the parallel group leader, if this process is a parallel query worker. NULL if this process is a parallel group leader or does not participate in parallel query.");
			mtd.addColumn("pg_stat_activity", "usesysid",         "OID of the user logged into this backend");
			mtd.addColumn("pg_stat_activity", "usename",          "Name of the user logged into this backend");
			mtd.addColumn("pg_stat_activity", "application_name", "Name of the application that is connected to this backend");
			mtd.addColumn("pg_stat_activity", "client_addr",      "IP address of the client connected to this backend. If this field is null, it indicates either that the client is connected via a Unix socket on the server machine or that this is an internal process such as autovacuum.");
			mtd.addColumn("pg_stat_activity", "client_hostname",  "Host name of the connected client, as reported by a reverse DNS lookup of client_addr. This field will only be non-null for IP connections, and only when log_hostname is enabled.");
			mtd.addColumn("pg_stat_activity", "client_port",      "TCP port number that the client is using for communication with this backend, or -1 if a Unix socket is used");
			mtd.addColumn("pg_stat_activity", "backend_start",    "Time when this process was started, i.e., when the client connected to the server");
			mtd.addColumn("pg_stat_activity", "xact_start",       "Time when this process' current transaction was started, or null if no transaction is active. If the current query is the first of its transaction, this column is equal to the query_start column.");
			mtd.addColumn("pg_stat_activity", "query_start",      "Time when the currently active query was started, or if state is not active, when the last query was started");
			mtd.addColumn("pg_stat_activity", "state_change",     "Time when the state was last changed");
			mtd.addColumn("pg_stat_activity", "wait_event_type",  "The type of event for which the backend is waiting, if any; otherwise NULL");
			mtd.addColumn("pg_stat_activity", "wait_event",       "Wait event name if backend is currently waiting, otherwise NULL");
			mtd.addColumn("pg_stat_activity", "waiting",          "True if this backend is currently waiting on a lock");
			mtd.addColumn("pg_stat_activity", "state",            "<html>" + 
			                                                      "Current overall state of this backend. Possible values are:" +
			                                                      "<ul>" +
			                                                      "   <li> <b>active</b>:                        The backend is executing a query. </li>" +
			                                                      "   <li> <b>idle</b>:                          The backend is waiting for a new client command. </li>" +
			                                                      "   <li> <b>idle in transaction</b>:           The backend is in a transaction, but is not currently executing a query. </li>" +
			                                                      "   <li> <b>idle in transaction (aborted)</b>: This state is similar to idle in transaction, except one of the statements in the transaction caused an error. </li>" +
			                                                      "   <li> <b>fastpath function call</b>:        The backend is executing a fast-path function. </li>" +
			                                                      "   <li> <b>disabled</b>:                      This state is reported if track_activities is disabled in this backend. </li>" +
			                                                      "</ul>" +
			                                                      "</html>");
			mtd.addColumn("pg_stat_activity", "backend_xid",      "Top-level transaction identifier of this backend, if any.");
			mtd.addColumn("pg_stat_activity", "backend_xmin",     "The current backend's xmin horizon.");
			mtd.addColumn("pg_stat_activity", "query_id",         "Identifier of this backend's most recent query. If state is active this field shows the identifier of the currently executing query. In all other states, it shows the identifier of last query that was executed. Query identifiers are not computed by default so this field will be null unless compute_query_id parameter is enabled or a third-party module that computes query identifiers is configured.");
			mtd.addColumn("pg_stat_activity", "query",            "Text of this backend's most recent query. If state is active this field shows the currently executing query. In all other states, it shows the last query that was executed.");
			mtd.addColumn("pg_stat_activity", "backend_type",     "Type of current backend. Possible types are autovacuum launcher, autovacuum worker, logical replication launcher, logical replication worker, parallel worker, background writer, client backend, checkpointer, archiver, startup, walreceiver, walsender and walwriter. In addition, background workers registered by extensions may have additional types.");

			

			//---------------------------------------------------------------------------------------------------------------
			// pg_stat_archiver
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_stat_archiver",            "One row only, showing statistics about the WAL archiver process's activity. See pg_stat_archiver for details.");

			mtd.addColumn("pg_stat_archiver", "archived_count",     "Number of WAL files that have been successfully archived");
			mtd.addColumn("pg_stat_archiver", "last_archived_wal",  "Name of the last WAL file successfully archived");
			mtd.addColumn("pg_stat_archiver", "last_archived_time", "Time of the last successful archive operation");
			mtd.addColumn("pg_stat_archiver", "failed_count",       "Number of failed attempts for archiving WAL files");
			mtd.addColumn("pg_stat_archiver", "last_failed_wal",    "Name of the WAL file of the last failed archival operation");
			mtd.addColumn("pg_stat_archiver", "last_failed_time",   "Time of the last failed archival operation");
			mtd.addColumn("pg_stat_archiver", "stats_reset",        "Time at which these statistics were last reset");

			

			//---------------------------------------------------------------------------------------------------------------
			// pg_stat_bgwriter
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_stat_bgwriter",            "One row only, showing statistics about the background writer process's activity. See pg_stat_bgwriter for details.");

			mtd.addColumn("pg_stat_bgwriter", "checkpoints_timed",     "Number of scheduled checkpoints that have been performed");
			mtd.addColumn("pg_stat_bgwriter", "checkpoints_req",       "Number of requested checkpoints that have been performed");
			mtd.addColumn("pg_stat_bgwriter", "checkpoint_write_time", "Total amount of time that has been spent in the portion of checkpoint processing where files are written to disk, in milliseconds");
			mtd.addColumn("pg_stat_bgwriter", "checkpoint_sync_time",  "Total amount of time that has been spent in the portion of checkpoint processing where files are synchronized to disk, in milliseconds");
			mtd.addColumn("pg_stat_bgwriter", "buffers_checkpoint",    "Number of buffers written during checkpoints");
			mtd.addColumn("pg_stat_bgwriter", "buffers_clean",         "Number of buffers written by the background writer");
			mtd.addColumn("pg_stat_bgwriter", "maxwritten_clean",      "Number of times the background writer stopped a cleaning scan because it had written too many buffers");
			mtd.addColumn("pg_stat_bgwriter", "buffers_backend",       "Number of buffers written directly by a backend");
			mtd.addColumn("pg_stat_bgwriter", "buffers_backend_fsync", "Number of times a backend had to execute its own fsync call (normally the background writer handles those even when the backend does its own write)");
			mtd.addColumn("pg_stat_bgwriter", "buffers_alloc",         "Number of buffers allocated");
			mtd.addColumn("pg_stat_bgwriter", "stats_reset",           "Time at which these statistics were last reset");
			

			//---------------------------------------------------------------------------------------------------------------
			// pg_stat_wal
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_stat_wal",  "The pg_stat_wal view will always have a single row, containing data about WAL activity of the cluster.");

			mtd.addColumn("pg_stat_wal", "wal_records",          "Total number of WAL records generated");
			mtd.addColumn("pg_stat_wal", "wal_fpi",              "Total number of WAL full page images generated");
			mtd.addColumn("pg_stat_wal", "wal_bytes",            "Total amount of WAL generated in bytes");
			mtd.addColumn("pg_stat_wal", "wal_buffers_full",     "Number of times WAL data was written to disk because WAL buffers became full");
			mtd.addColumn("pg_stat_wal", "wal_write",            "Number of times WAL buffers were written out to disk via XLogWrite request. See Section 30.5 for more information about the internal WAL function XLogWrite.");
			mtd.addColumn("pg_stat_wal", "wal_sync",             "Number of times WAL files were synced to disk via issue_xlog_fsync request (if fsync is on and wal_sync_method is either fdatasync, fsync or fsync_writethrough, otherwise zero). See Section 30.5 for more information about the internal WAL function issue_xlog_fsync.");
			mtd.addColumn("pg_stat_wal", "wal_write_time",       "Total amount of time spent writing WAL buffers to disk via XLogWrite request, in milliseconds (if track_wal_io_timing is enabled, otherwise zero). This includes the sync time when wal_sync_method is either open_datasync or open_sync.");
			mtd.addColumn("pg_stat_wal", "wal_sync_time",        "Total amount of time spent syncing WAL files to disk via issue_xlog_fsync request, in milliseconds (if track_wal_io_timing is enabled, fsync is on, and wal_sync_method is either fdatasync, fsync or fsync_writethrough, otherwise zero).");
			mtd.addColumn("pg_stat_wal", "stats_reset",          "Time at which these statistics were last reset");

			
			//---------------------------------------------------------------------------------------------------------------
			// pg_stat_database
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_stat_database",            "One row per database, showing database-wide statistics. See pg_stat_database for details.");

			mtd.addColumn("pg_stat_database", "datid",                    "OID of a database");
			mtd.addColumn("pg_stat_database", "datname",                  "Name of this database");
			mtd.addColumn("pg_stat_database", "numbackends",              "Number of backends currently connected to this database. This is the only column in this view that returns a value reflecting current state; all other columns return the accumulated values since the last reset.");
			mtd.addColumn("pg_stat_database", "xact_commit",              "Number of transactions in this database that have been committed");
			mtd.addColumn("pg_stat_database", "xact_rollback",            "Number of transactions in this database that have been rolled back");
			mtd.addColumn("pg_stat_database", "blks_read",                "Number of disk blocks read in this database");
			mtd.addColumn("pg_stat_database", "blks_hit",                 "Number of times disk blocks were found already in the buffer cache, so that a read was not necessary (this only includes hits in the PostgreSQL buffer cache, not the operating system's file system cache)");
			mtd.addColumn("pg_stat_database", "tup_returned",             "Number of rows returned by queries in this database (rows read from memory/disk)");
			mtd.addColumn("pg_stat_database", "tup_fetched",              "Number of rows fetched by queries in this database (rows sent to clients)");
			mtd.addColumn("pg_stat_database", "tup_inserted",             "Number of rows inserted by queries in this database");
			mtd.addColumn("pg_stat_database", "tup_updated",              "Number of rows updated by queries in this database");
			mtd.addColumn("pg_stat_database", "tup_deleted",              "Number of rows deleted by queries in this database");
			mtd.addColumn("pg_stat_database", "conflicts",                "Number of queries canceled due to conflicts with recovery in this database. (Conflicts occur only on standby servers; see pg_stat_database_conflicts for details.)");
			mtd.addColumn("pg_stat_database", "temp_files",               "Number of temporary files created by queries in this database. All temporary files are counted, regardless of why the temporary file was created (e.g., sorting or hashing), and regardless of the log_temp_files setting.");
			mtd.addColumn("pg_stat_database", "temp_bytes",               "Total amount of data written to temporary files by queries in this database. All temporary files are counted, regardless of why the temporary file was created, and regardless of the log_temp_files setting.");
			mtd.addColumn("pg_stat_database", "deadlocks",                "Number of deadlocks detected in this database");
			mtd.addColumn("pg_stat_database", "checksum_failures",        "Number of data page checksum failures detected in this database (or on a shared object), or NULL if data checksums are not enabled.");
			mtd.addColumn("pg_stat_database", "checksum_last_failure",    "Time at which the last data page checksum failure was detected in this database (or on a shared object), or NULL if data checksums are not enabled.");
			mtd.addColumn("pg_stat_database", "blk_read_time",            "Time spent reading data file blocks by backends in this database, in milliseconds (if track_io_timing is enabled, otherwise zero)");
			mtd.addColumn("pg_stat_database", "blk_write_time",           "Time spent writing data file blocks by backends in this database, in milliseconds (if track_io_timing is enabled, otherwise zero)");
			mtd.addColumn("pg_stat_database", "session_time",             "Time spent by database sessions in this database, in milliseconds (note that statistics are only updated when the state of a session changes, so if sessions have been idle for a long time, this idle time won't be included)");
			mtd.addColumn("pg_stat_database", "active_time",              "Time spent executing SQL statements in this database, in milliseconds (this corresponds to the states active and fastpath function call in pg_stat_activity)");
			mtd.addColumn("pg_stat_database", "idle_in_transaction_time", "Time spent idling while in a transaction in this database, in milliseconds (this corresponds to the states idle in transaction and idle in transaction (aborted) in pg_stat_activity)");
			mtd.addColumn("pg_stat_database", "sessions",                 "Total number of sessions established to this database");
			mtd.addColumn("pg_stat_database", "sessions_abandoned",       "Number of database sessions to this database that were terminated because connection to the client was lost");
			mtd.addColumn("pg_stat_database", "sessions_fatal",           "Number of database sessions to this database that were terminated by fatal errors");
			mtd.addColumn("pg_stat_database", "sessions_killed",          "Number of database sessions to this database that were terminated by operator intervention");
			mtd.addColumn("pg_stat_database", "stats_reset",              "Time at which these statistics were last reset");


			//---------------------------------------------------------------------------------------------------------------
			// pg_stat_database_conflicts
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_stat_database_conflicts",  "One row per database, showing database-wide statistics about query cancels due to conflict with recovery on standby servers. See pg_stat_database_conflicts for details.");

			mtd.addColumn("pg_stat_database_conflicts", "datid",            "OID of a database");
			mtd.addColumn("pg_stat_database_conflicts", "datname",          "Name of this database");
			mtd.addColumn("pg_stat_database_conflicts", "confl_tablespace", "Number of queries in this database that have been canceled due to dropped tablespaces");
			mtd.addColumn("pg_stat_database_conflicts", "confl_lock",       "Number of queries in this database that have been canceled due to lock timeouts");
			mtd.addColumn("pg_stat_database_conflicts", "confl_snapshot",   "Number of queries in this database that have been canceled due to old snapshots");
			mtd.addColumn("pg_stat_database_conflicts", "confl_bufferpin",  "Number of queries in this database that have been canceled due to pinned buffers");
			mtd.addColumn("pg_stat_database_conflicts", "confl_deadlock",   "Number of queries in this database that have been canceled due to deadlocks");
			

			//---------------------------------------------------------------------------------------------------------------
			// pg_stat_all_tables
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_stat_all_tables",          "One row for each table in the current database, showing statistics about accesses to that specific table. See pg_stat_all_tables for details.");
			mtd.addTable("pg_stat_sys_tables",          "Same as pg_stat_all_tables, except that only system tables are shown.");
			mtd.addTable("pg_stat_user_tables",         "Same as pg_stat_all_tables, except that only user tables are shown.");

			mtd.addColumn("pg_stat_all_tables", "relid",               "OID of a table");
			mtd.addColumn("pg_stat_all_tables", "schemaname",          "Name of the schema that this table is in");
			mtd.addColumn("pg_stat_all_tables", "relname",             "Name of this table");
			mtd.addColumn("pg_stat_all_tables", "seq_scan",            "Number of sequential scans initiated on this table");
			mtd.addColumn("pg_stat_all_tables", "seq_tup_read",        "Number of live rows fetched by sequential scans");
			mtd.addColumn("pg_stat_all_tables", "idx_scan",            "Number of index scans initiated on this table");
			mtd.addColumn("pg_stat_all_tables", "idx_tup_fetch",       "Number of live rows fetched by index scans");
			mtd.addColumn("pg_stat_all_tables", "n_tup_ins",           "Number of rows inserted");
			mtd.addColumn("pg_stat_all_tables", "n_tup_upd",           "Number of rows updated");
			mtd.addColumn("pg_stat_all_tables", "n_tup_del",           "Number of rows deleted");
			mtd.addColumn("pg_stat_all_tables", "n_tup_hot_upd",       "Number of rows HOT updated (i.e., with no separate index update required)");
			mtd.addColumn("pg_stat_all_tables", "n_live_tup",          "Estimated number of live rows");
			mtd.addColumn("pg_stat_all_tables", "n_dead_tup",          "Estimated number of dead rows");
			mtd.addColumn("pg_stat_all_tables", "n_mod_since_analyze", "Estimated number of rows modified since this table was last analyzed");
			mtd.addColumn("pg_stat_all_tables", "n_ins_since_vacuum",  "Estimated number of rows inserted since this table was last vacuumed");
			mtd.addColumn("pg_stat_all_tables", "last_vacuum",         "Last time at which this table was manually vacuumed (not counting VACUUM FULL)");
			mtd.addColumn("pg_stat_all_tables", "last_autovacuum",     "Last time at which this table was vacuumed by the autovacuum daemon");
			mtd.addColumn("pg_stat_all_tables", "last_analyze",        "Last time at which this table was manually analyzed");
			mtd.addColumn("pg_stat_all_tables", "last_autoanalyze",    "Last time at which this table was analyzed by the autovacuum daemon");
			mtd.addColumn("pg_stat_all_tables", "vacuum_count",        "Number of times this table has been manually vacuumed (not counting VACUUM FULL)");
			mtd.addColumn("pg_stat_all_tables", "autovacuum_count",    "Number of times this table has been vacuumed by the autovacuum daemon");
			mtd.addColumn("pg_stat_all_tables", "analyze_count",       "Number of times this table has been manually analyzed");
			mtd.addColumn("pg_stat_all_tables", "autoanalyze_count",   "Number of times this table has been analyzed by the autovacuum daemon");

			mtd.addColumn("pg_stat_sys_tables", "relid",               "OID of a table");
			mtd.addColumn("pg_stat_sys_tables", "schemaname",          "Name of the schema that this table is in");
			mtd.addColumn("pg_stat_sys_tables", "relname",             "Name of this table");
			mtd.addColumn("pg_stat_sys_tables", "seq_scan",            "Number of sequential scans initiated on this table");
			mtd.addColumn("pg_stat_sys_tables", "seq_tup_read",        "Number of live rows fetched by sequential scans");
			mtd.addColumn("pg_stat_sys_tables", "idx_scan",            "Number of index scans initiated on this table");
			mtd.addColumn("pg_stat_sys_tables", "idx_tup_fetch",       "Number of live rows fetched by index scans");
			mtd.addColumn("pg_stat_sys_tables", "n_tup_ins",           "Number of rows inserted");
			mtd.addColumn("pg_stat_sys_tables", "n_tup_upd",           "Number of rows updated");
			mtd.addColumn("pg_stat_sys_tables", "n_tup_del",           "Number of rows deleted");
			mtd.addColumn("pg_stat_sys_tables", "n_tup_hot_upd",       "Number of rows HOT updated (i.e., with no separate index update required)");
			mtd.addColumn("pg_stat_sys_tables", "n_live_tup",          "Estimated number of live rows");
			mtd.addColumn("pg_stat_sys_tables", "n_dead_tup",          "Estimated number of dead rows");
			mtd.addColumn("pg_stat_sys_tables", "n_mod_since_analyze", "Estimated number of rows modified since this table was last analyzed");
			mtd.addColumn("pg_stat_sys_tables", "n_ins_since_vacuum",  "Estimated number of rows inserted since this table was last vacuumed");
			mtd.addColumn("pg_stat_sys_tables", "last_vacuum",         "Last time at which this table was manually vacuumed (not counting VACUUM FULL)");
			mtd.addColumn("pg_stat_sys_tables", "last_autovacuum",     "Last time at which this table was vacuumed by the autovacuum daemon");
			mtd.addColumn("pg_stat_sys_tables", "last_analyze",        "Last time at which this table was manually analyzed");
			mtd.addColumn("pg_stat_sys_tables", "last_autoanalyze",    "Last time at which this table was analyzed by the autovacuum daemon");
			mtd.addColumn("pg_stat_sys_tables", "vacuum_count",        "Number of times this table has been manually vacuumed (not counting VACUUM FULL)");
			mtd.addColumn("pg_stat_sys_tables", "autovacuum_count",    "Number of times this table has been vacuumed by the autovacuum daemon");
			mtd.addColumn("pg_stat_sys_tables", "analyze_count",       "Number of times this table has been manually analyzed");
			mtd.addColumn("pg_stat_sys_tables", "autoanalyze_count",   "Number of times this table has been analyzed by the autovacuum daemon");

			mtd.addColumn("pg_stat_user_tables", "relid",               "OID of a table");
			mtd.addColumn("pg_stat_user_tables", "schemaname",          "Name of the schema that this table is in");
			mtd.addColumn("pg_stat_user_tables", "relname",             "Name of this table");
			mtd.addColumn("pg_stat_user_tables", "seq_scan",            "Number of sequential scans initiated on this table");
			mtd.addColumn("pg_stat_user_tables", "seq_tup_read",        "Number of live rows fetched by sequential scans");
			mtd.addColumn("pg_stat_user_tables", "idx_scan",            "Number of index scans initiated on this table");
			mtd.addColumn("pg_stat_user_tables", "idx_tup_fetch",       "Number of live rows fetched by index scans");
			mtd.addColumn("pg_stat_user_tables", "n_tup_ins",           "Number of rows inserted");
			mtd.addColumn("pg_stat_user_tables", "n_tup_upd",           "Number of rows updated");
			mtd.addColumn("pg_stat_user_tables", "n_tup_del",           "Number of rows deleted");
			mtd.addColumn("pg_stat_user_tables", "n_tup_hot_upd",       "Number of rows HOT updated (i.e., with no separate index update required)");
			mtd.addColumn("pg_stat_user_tables", "n_live_tup",          "Estimated number of live rows");
			mtd.addColumn("pg_stat_user_tables", "n_dead_tup",          "Estimated number of dead rows");
			mtd.addColumn("pg_stat_user_tables", "n_mod_since_analyze", "Estimated number of rows modified since this table was last analyzed");
			mtd.addColumn("pg_stat_user_tables", "n_ins_since_vacuum",  "Estimated number of rows inserted since this table was last vacuumed");
			mtd.addColumn("pg_stat_user_tables", "last_vacuum",         "Last time at which this table was manually vacuumed (not counting VACUUM FULL)");
			mtd.addColumn("pg_stat_user_tables", "last_autovacuum",     "Last time at which this table was vacuumed by the autovacuum daemon");
			mtd.addColumn("pg_stat_user_tables", "last_analyze",        "Last time at which this table was manually analyzed");
			mtd.addColumn("pg_stat_user_tables", "last_autoanalyze",    "Last time at which this table was analyzed by the autovacuum daemon");
			mtd.addColumn("pg_stat_user_tables", "vacuum_count",        "Number of times this table has been manually vacuumed (not counting VACUUM FULL)");
			mtd.addColumn("pg_stat_user_tables", "autovacuum_count",    "Number of times this table has been vacuumed by the autovacuum daemon");
			mtd.addColumn("pg_stat_user_tables", "analyze_count",       "Number of times this table has been manually analyzed");
			mtd.addColumn("pg_stat_user_tables", "autoanalyze_count",   "Number of times this table has been analyzed by the autovacuum daemon");

			

			//---------------------------------------------------------------------------------------------------------------
			// pg_stat_xact_all_tables
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_stat_xact_all_tables",     "Similar to pg_stat_all_tables, but counts actions taken so far within the current transaction (which are not yet included in pg_stat_all_tables and related views). The columns for numbers of live and dead rows and vacuum and analyze actions are not present in this view.");
			mtd.addTable("pg_stat_xact_sys_tables",     "Same as pg_stat_xact_all_tables, except that only system tables are shown.");
			mtd.addTable("pg_stat_xact_user_tables",    "Same as pg_stat_xact_all_tables, except that only user tables are shown.");

			mtd.addColumn("pg_stat_xact_all_tables", "relid",               "OID of a table");
			mtd.addColumn("pg_stat_xact_all_tables", "schemaname",          "Name of the schema that this table is in");
			mtd.addColumn("pg_stat_xact_all_tables", "relname",             "Name of this table");
			mtd.addColumn("pg_stat_xact_all_tables", "seq_scan",            "Number of sequential scans initiated on this table");
			mtd.addColumn("pg_stat_xact_all_tables", "seq_tup_read",        "Number of live rows fetched by sequential scans");
			mtd.addColumn("pg_stat_xact_all_tables", "idx_scan",            "Number of index scans initiated on this table");
			mtd.addColumn("pg_stat_xact_all_tables", "idx_tup_fetch",       "Number of live rows fetched by index scans");
			mtd.addColumn("pg_stat_xact_all_tables", "n_tup_ins",           "Number of rows inserted");
			mtd.addColumn("pg_stat_xact_all_tables", "n_tup_upd",           "Number of rows updated");
			mtd.addColumn("pg_stat_xact_all_tables", "n_tup_del",           "Number of rows deleted");
			mtd.addColumn("pg_stat_xact_all_tables", "n_tup_hot_upd",       "Number of rows HOT updated (i.e., with no separate index update required)");
			mtd.addColumn("pg_stat_xact_all_tables", "n_live_tup",          "Estimated number of live rows");
			mtd.addColumn("pg_stat_xact_all_tables", "n_dead_tup",          "Estimated number of dead rows");
			mtd.addColumn("pg_stat_xact_all_tables", "n_mod_since_analyze", "Estimated number of rows modified since this table was last analyzed");
			mtd.addColumn("pg_stat_xact_all_tables", "last_vacuum",         "Last time at which this table was manually vacuumed (not counting VACUUM FULL)");
			mtd.addColumn("pg_stat_xact_all_tables", "last_autovacuum",     "Last time at which this table was vacuumed by the autovacuum daemon");
			mtd.addColumn("pg_stat_xact_all_tables", "last_analyze",        "Last time at which this table was manually analyzed");
			mtd.addColumn("pg_stat_xact_all_tables", "last_autoanalyze",    "Last time at which this table was analyzed by the autovacuum daemon");
			mtd.addColumn("pg_stat_xact_all_tables", "vacuum_count",        "Number of times this table has been manually vacuumed (not counting VACUUM FULL)");
			mtd.addColumn("pg_stat_xact_all_tables", "autovacuum_count",    "Number of times this table has been vacuumed by the autovacuum daemon");
			mtd.addColumn("pg_stat_xact_all_tables", "analyze_count",       "Number of times this table has been manually analyzed");
			mtd.addColumn("pg_stat_xact_all_tables", "autoanalyze_count",   "Number of times this table has been analyzed by the autovacuum daemon");

			mtd.addColumn("pg_stat_xact_sys_tables", "relid",               "OID of a table");
			mtd.addColumn("pg_stat_xact_sys_tables", "schemaname",          "Name of the schema that this table is in");
			mtd.addColumn("pg_stat_xact_sys_tables", "relname",             "Name of this table");
			mtd.addColumn("pg_stat_xact_sys_tables", "seq_scan",            "Number of sequential scans initiated on this table");
			mtd.addColumn("pg_stat_xact_sys_tables", "seq_tup_read",        "Number of live rows fetched by sequential scans");
			mtd.addColumn("pg_stat_xact_sys_tables", "idx_scan",            "Number of index scans initiated on this table");
			mtd.addColumn("pg_stat_xact_sys_tables", "idx_tup_fetch",       "Number of live rows fetched by index scans");
			mtd.addColumn("pg_stat_xact_sys_tables", "n_tup_ins",           "Number of rows inserted");
			mtd.addColumn("pg_stat_xact_sys_tables", "n_tup_upd",           "Number of rows updated");
			mtd.addColumn("pg_stat_xact_sys_tables", "n_tup_del",           "Number of rows deleted");
			mtd.addColumn("pg_stat_xact_sys_tables", "n_tup_hot_upd",       "Number of rows HOT updated (i.e., with no separate index update required)");
			mtd.addColumn("pg_stat_xact_sys_tables", "n_live_tup",          "Estimated number of live rows");
			mtd.addColumn("pg_stat_xact_sys_tables", "n_dead_tup",          "Estimated number of dead rows");
			mtd.addColumn("pg_stat_xact_sys_tables", "n_mod_since_analyze", "Estimated number of rows modified since this table was last analyzed");
			mtd.addColumn("pg_stat_xact_sys_tables", "last_vacuum",         "Last time at which this table was manually vacuumed (not counting VACUUM FULL)");
			mtd.addColumn("pg_stat_xact_sys_tables", "last_autovacuum",     "Last time at which this table was vacuumed by the autovacuum daemon");
			mtd.addColumn("pg_stat_xact_sys_tables", "last_analyze",        "Last time at which this table was manually analyzed");
			mtd.addColumn("pg_stat_xact_sys_tables", "last_autoanalyze",    "Last time at which this table was analyzed by the autovacuum daemon");
			mtd.addColumn("pg_stat_xact_sys_tables", "vacuum_count",        "Number of times this table has been manually vacuumed (not counting VACUUM FULL)");
			mtd.addColumn("pg_stat_xact_sys_tables", "autovacuum_count",    "Number of times this table has been vacuumed by the autovacuum daemon");
			mtd.addColumn("pg_stat_xact_sys_tables", "analyze_count",       "Number of times this table has been manually analyzed");
			mtd.addColumn("pg_stat_xact_sys_tables", "autoanalyze_count",   "Number of times this table has been analyzed by the autovacuum daemon");

			mtd.addColumn("pg_stat_xact_user_tables", "relid",               "OID of a table");
			mtd.addColumn("pg_stat_xact_user_tables", "schemaname",          "Name of the schema that this table is in");
			mtd.addColumn("pg_stat_xact_user_tables", "relname",             "Name of this table");
			mtd.addColumn("pg_stat_xact_user_tables", "seq_scan",            "Number of sequential scans initiated on this table");
			mtd.addColumn("pg_stat_xact_user_tables", "seq_tup_read",        "Number of live rows fetched by sequential scans");
			mtd.addColumn("pg_stat_xact_user_tables", "idx_scan",            "Number of index scans initiated on this table");
			mtd.addColumn("pg_stat_xact_user_tables", "idx_tup_fetch",       "Number of live rows fetched by index scans");
			mtd.addColumn("pg_stat_xact_user_tables", "n_tup_ins",           "Number of rows inserted");
			mtd.addColumn("pg_stat_xact_user_tables", "n_tup_upd",           "Number of rows updated");
			mtd.addColumn("pg_stat_xact_user_tables", "n_tup_del",           "Number of rows deleted");
			mtd.addColumn("pg_stat_xact_user_tables", "n_tup_hot_upd",       "Number of rows HOT updated (i.e., with no separate index update required)");
			mtd.addColumn("pg_stat_xact_user_tables", "n_live_tup",          "Estimated number of live rows");
			mtd.addColumn("pg_stat_xact_user_tables", "n_dead_tup",          "Estimated number of dead rows");
			mtd.addColumn("pg_stat_xact_user_tables", "n_mod_since_analyze", "Estimated number of rows modified since this table was last analyzed");
			mtd.addColumn("pg_stat_xact_user_tables", "last_vacuum",         "Last time at which this table was manually vacuumed (not counting VACUUM FULL)");
			mtd.addColumn("pg_stat_xact_user_tables", "last_autovacuum",     "Last time at which this table was vacuumed by the autovacuum daemon");
			mtd.addColumn("pg_stat_xact_user_tables", "last_analyze",        "Last time at which this table was manually analyzed");
			mtd.addColumn("pg_stat_xact_user_tables", "last_autoanalyze",    "Last time at which this table was analyzed by the autovacuum daemon");
			mtd.addColumn("pg_stat_xact_user_tables", "vacuum_count",        "Number of times this table has been manually vacuumed (not counting VACUUM FULL)");
			mtd.addColumn("pg_stat_xact_user_tables", "autovacuum_count",    "Number of times this table has been vacuumed by the autovacuum daemon");
			mtd.addColumn("pg_stat_xact_user_tables", "analyze_count",       "Number of times this table has been manually analyzed");
			mtd.addColumn("pg_stat_xact_user_tables", "autoanalyze_count",   "Number of times this table has been analyzed by the autovacuum daemon");

			

			//---------------------------------------------------------------------------------------------------------------
			// pg_stat_all_indexes
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_stat_all_indexes",         "One row for each index in the current database, showing statistics about accesses to that specific index. See pg_stat_all_indexes for details.");
			mtd.addTable("pg_stat_sys_indexes",         "Same as pg_stat_all_indexes, except that only indexes on system tables are shown.");
			mtd.addTable("pg_stat_user_indexes",        "Same as pg_stat_all_indexes, except that only indexes on user tables are shown.");

			mtd.addColumn("pg_stat_all_indexes", "relid",         "OID of the table for this index");
			mtd.addColumn("pg_stat_all_indexes", "indexrelid",    "OID of this index");
			mtd.addColumn("pg_stat_all_indexes", "schemaname",    "Name of the schema this index is in");
			mtd.addColumn("pg_stat_all_indexes", "relname",       "Name of the table for this index");
			mtd.addColumn("pg_stat_all_indexes", "indexrelname",  "Name of this index");
			mtd.addColumn("pg_stat_all_indexes", "idx_scan",      "Number of index scans initiated on this index");
			mtd.addColumn("pg_stat_all_indexes", "idx_tup_read",  "Number of index entries returned by scans on this index");
			mtd.addColumn("pg_stat_all_indexes", "idx_tup_fetch", "Number of live table rows fetched by simple index scans using this index");

			mtd.addColumn("pg_stat_sys_indexes", "relid",         "OID of the table for this index");
			mtd.addColumn("pg_stat_sys_indexes", "indexrelid",    "OID of this index");
			mtd.addColumn("pg_stat_sys_indexes", "schemaname",    "Name of the schema this index is in");
			mtd.addColumn("pg_stat_sys_indexes", "relname",       "Name of the table for this index");
			mtd.addColumn("pg_stat_sys_indexes", "indexrelname",  "Name of this index");
			mtd.addColumn("pg_stat_sys_indexes", "idx_scan",      "Number of index scans initiated on this index");
			mtd.addColumn("pg_stat_sys_indexes", "idx_tup_read",  "Number of index entries returned by scans on this index");
			mtd.addColumn("pg_stat_sys_indexes", "idx_tup_fetch", "Number of live table rows fetched by simple index scans using this index");

			mtd.addColumn("pg_stat_user_indexes", "relid",         "OID of the table for this index");
			mtd.addColumn("pg_stat_user_indexes", "indexrelid",    "OID of this index");
			mtd.addColumn("pg_stat_user_indexes", "schemaname",    "Name of the schema this index is in");
			mtd.addColumn("pg_stat_user_indexes", "relname",       "Name of the table for this index");
			mtd.addColumn("pg_stat_user_indexes", "indexrelname",  "Name of this index");
			mtd.addColumn("pg_stat_user_indexes", "idx_scan",      "Number of index scans initiated on this index");
			mtd.addColumn("pg_stat_user_indexes", "idx_tup_read",  "Number of index entries returned by scans on this index");
			mtd.addColumn("pg_stat_user_indexes", "idx_tup_fetch", "Number of live table rows fetched by simple index scans using this index");

			

			//---------------------------------------------------------------------------------------------------------------
			// pg_statio_all_tables
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_statio_all_tables",        "One row for each table in the current database, showing statistics about I/O on that specific table. See pg_statio_all_tables for details.");
			mtd.addTable("pg_statio_sys_tables",        "Same as pg_statio_all_tables, except that only system tables are shown.");
			mtd.addTable("pg_statio_user_tables",       "Same as pg_statio_all_tables, except that only user tables are shown.");

			mtd.addColumn("pg_statio_all_tables", "relid",           "OID of a table");
			mtd.addColumn("pg_statio_all_tables", "schemaname",      "Name of the schema that this table is in");
			mtd.addColumn("pg_statio_all_tables", "relname",         "Name of this table");
			mtd.addColumn("pg_statio_all_tables", "heap_blks_read",  "Number of disk blocks read from this table");
			mtd.addColumn("pg_statio_all_tables", "heap_blks_hit",   "Number of buffer hits in this table");
			mtd.addColumn("pg_statio_all_tables", "idx_blks_read",   "Number of disk blocks read from all indexes on this table");
			mtd.addColumn("pg_statio_all_tables", "idx_blks_hit",    "Number of buffer hits in all indexes on this table");
			mtd.addColumn("pg_statio_all_tables", "toast_blks_read", "Number of disk blocks read from this table's TOAST table (if any)");
			mtd.addColumn("pg_statio_all_tables", "toast_blks_hit",  "Number of buffer hits in this table's TOAST table (if any)");
			mtd.addColumn("pg_statio_all_tables", "tidx_blks_read",  "Number of disk blocks read from this table's TOAST table indexes (if any)");
			mtd.addColumn("pg_statio_all_tables", "tidx_blks_hit",   "Number of buffer hits in this table's TOAST table indexes (if any)");

			mtd.addColumn("pg_statio_sys_tables", "relid",           "OID of a table");
			mtd.addColumn("pg_statio_sys_tables", "schemaname",      "Name of the schema that this table is in");
			mtd.addColumn("pg_statio_sys_tables", "relname",         "Name of this table");
			mtd.addColumn("pg_statio_sys_tables", "heap_blks_read",  "Number of disk blocks read from this table");
			mtd.addColumn("pg_statio_sys_tables", "heap_blks_hit",   "Number of buffer hits in this table");
			mtd.addColumn("pg_statio_sys_tables", "idx_blks_read",   "Number of disk blocks read from all indexes on this table");
			mtd.addColumn("pg_statio_sys_tables", "idx_blks_hit",    "Number of buffer hits in all indexes on this table");
			mtd.addColumn("pg_statio_sys_tables", "toast_blks_read", "Number of disk blocks read from this table's TOAST table (if any)");
			mtd.addColumn("pg_statio_sys_tables", "toast_blks_hit",  "Number of buffer hits in this table's TOAST table (if any)");
			mtd.addColumn("pg_statio_sys_tables", "tidx_blks_read",  "Number of disk blocks read from this table's TOAST table indexes (if any)");
			mtd.addColumn("pg_statio_sys_tables", "tidx_blks_hit",   "Number of buffer hits in this table's TOAST table indexes (if any)");

			mtd.addColumn("pg_statio_user_tables", "relid",           "OID of a table");
			mtd.addColumn("pg_statio_user_tables", "schemaname",      "Name of the schema that this table is in");
			mtd.addColumn("pg_statio_user_tables", "relname",         "Name of this table");
			mtd.addColumn("pg_statio_user_tables", "heap_blks_read",  "Number of disk blocks read from this table");
			mtd.addColumn("pg_statio_user_tables", "heap_blks_hit",   "Number of buffer hits in this table");
			mtd.addColumn("pg_statio_user_tables", "idx_blks_read",   "Number of disk blocks read from all indexes on this table");
			mtd.addColumn("pg_statio_user_tables", "idx_blks_hit",    "Number of buffer hits in all indexes on this table");
			mtd.addColumn("pg_statio_user_tables", "toast_blks_read", "Number of disk blocks read from this table's TOAST table (if any)");
			mtd.addColumn("pg_statio_user_tables", "toast_blks_hit",  "Number of buffer hits in this table's TOAST table (if any)");
			mtd.addColumn("pg_statio_user_tables", "tidx_blks_read",  "Number of disk blocks read from this table's TOAST table indexes (if any)");
			mtd.addColumn("pg_statio_user_tables", "tidx_blks_hit",   "Number of buffer hits in this table's TOAST table indexes (if any)");

			

			//---------------------------------------------------------------------------------------------------------------
			// pg_statio_all_indexes
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_statio_all_indexes",       "One row for each index in the current database, showing statistics about I/O on that specific index. See pg_statio_all_indexes for details.");
			mtd.addTable("pg_statio_sys_indexes",       "Same as pg_statio_all_indexes, except that only indexes on system tables are shown.");
			mtd.addTable("pg_statio_user_indexes",      "Same as pg_statio_all_indexes, except that only indexes on user tables are shown.");
			
			mtd.addColumn("pg_statio_all_indexes", "relid",         "OID of the table for this index");
			mtd.addColumn("pg_statio_all_indexes", "indexrelid",    "OID of this index");
			mtd.addColumn("pg_statio_all_indexes", "schemaname",    "Name of the schema this index is in");
			mtd.addColumn("pg_statio_all_indexes", "relname",       "Name of the table for this index");
			mtd.addColumn("pg_statio_all_indexes", "indexrelname",  "Name of this index");
			mtd.addColumn("pg_statio_all_indexes", "idx_blks_read", "Number of disk blocks read from this index");
			mtd.addColumn("pg_statio_all_indexes", "idx_blks_hit",  "Number of buffer hits in this index");
			
			mtd.addColumn("pg_statio_sys_indexes", "relid",         "OID of the table for this index");
			mtd.addColumn("pg_statio_sys_indexes", "indexrelid",    "OID of this index");
			mtd.addColumn("pg_statio_sys_indexes", "schemaname",    "Name of the schema this index is in");
			mtd.addColumn("pg_statio_sys_indexes", "relname",       "Name of the table for this index");
			mtd.addColumn("pg_statio_sys_indexes", "indexrelname",  "Name of this index");
			mtd.addColumn("pg_statio_sys_indexes", "idx_blks_read", "Number of disk blocks read from this index");
			mtd.addColumn("pg_statio_sys_indexes", "idx_blks_hit",  "Number of buffer hits in this index");
			
			mtd.addColumn("pg_statio_user_indexes", "relid",         "OID of the table for this index");
			mtd.addColumn("pg_statio_user_indexes", "indexrelid",    "OID of this index");
			mtd.addColumn("pg_statio_user_indexes", "schemaname",    "Name of the schema this index is in");
			mtd.addColumn("pg_statio_user_indexes", "relname",       "Name of the table for this index");
			mtd.addColumn("pg_statio_user_indexes", "indexrelname",  "Name of this index");
			mtd.addColumn("pg_statio_user_indexes", "idx_blks_read", "Number of disk blocks read from this index");
			mtd.addColumn("pg_statio_user_indexes", "idx_blks_hit",  "Number of buffer hits in this index");
			

			//---------------------------------------------------------------------------------------------------------------
			// pg_statio_all_sequences
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_statio_all_sequences",     "One row for each sequence in the current database, showing statistics about I/O on that specific sequence. See pg_statio_all_sequences for details.");
			mtd.addTable("pg_statio_sys_sequences",     "Same as pg_statio_all_sequences, except that only system sequences are shown. (Presently, no system sequences are defined, so this view is always empty.)");
			mtd.addTable("pg_statio_user_sequences",    "Same as pg_statio_all_sequences, except that only user sequences are shown.");

			mtd.addColumn("pg_statio_all_sequences", "relid",      "OID of a sequence");
			mtd.addColumn("pg_statio_all_sequences", "schemaname", "Name of the schema this sequence is in");
			mtd.addColumn("pg_statio_all_sequences", "relname",    "Name of this sequence");
			mtd.addColumn("pg_statio_all_sequences", "blks_read",  "Number of disk blocks read from this sequence");
			mtd.addColumn("pg_statio_all_sequences", "blks_hit",   "Number of buffer hits in this sequence");

			mtd.addColumn("pg_statio_sys_sequences", "relid",      "OID of a sequence");
			mtd.addColumn("pg_statio_sys_sequences", "schemaname", "Name of the schema this sequence is in");
			mtd.addColumn("pg_statio_sys_sequences", "relname",    "Name of this sequence");
			mtd.addColumn("pg_statio_sys_sequences", "blks_read",  "Number of disk blocks read from this sequence");
			mtd.addColumn("pg_statio_sys_sequences", "blks_hit",   "Number of buffer hits in this sequence");

			mtd.addColumn("pg_statio_user_sequences", "relid",      "OID of a sequence");
			mtd.addColumn("pg_statio_user_sequences", "schemaname", "Name of the schema this sequence is in");
			mtd.addColumn("pg_statio_user_sequences", "relname",    "Name of this sequence");
			mtd.addColumn("pg_statio_user_sequences", "blks_read",  "Number of disk blocks read from this sequence");
			mtd.addColumn("pg_statio_user_sequences", "blks_hit",   "Number of buffer hits in this sequence");

			

			//---------------------------------------------------------------------------------------------------------------
			// pg_stat_user_functions
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_stat_user_functions", "One row for each tracked function, showing statistics about executions of that function. See pg_stat_user_functions for details.");

			mtd.addColumn("pg_stat_user_functions", "funcid",     "OID of a function");
			mtd.addColumn("pg_stat_user_functions", "schemaname", "Name of the schema this function is in");
			mtd.addColumn("pg_stat_user_functions", "funcname",   "Name of this function");
			mtd.addColumn("pg_stat_user_functions", "calls",      "Number of times this function has been called");
			mtd.addColumn("pg_stat_user_functions", "total_time", "Total time spent in this function and all other functions called by it, in milliseconds");
			mtd.addColumn("pg_stat_user_functions", "self_time",  "Total time spent in this function itself, not including other functions called by it, in milliseconds");
			

			//---------------------------------------------------------------------------------------------------------------
			// pg_stat_xact_user_functions
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_stat_xact_user_functions", "Similar to pg_stat_user_functions, but counts only calls during the current transaction (which are not yet included in pg_stat_user_functions).");

			mtd.addColumn("pg_stat_xact_user_functions", "funcid",     "OID of a function");
			mtd.addColumn("pg_stat_xact_user_functions", "schemaname", "Name of the schema this function is in");
			mtd.addColumn("pg_stat_xact_user_functions", "funcname",   "Name of this function");
			mtd.addColumn("pg_stat_xact_user_functions", "calls",      "Number of times this function has been called");
			mtd.addColumn("pg_stat_xact_user_functions", "total_time", "Total time spent in this function and all other functions called by it, in milliseconds");
			mtd.addColumn("pg_stat_xact_user_functions", "self_time",  "Total time spent in this function itself, not including other functions called by it, in milliseconds");
			
			
			//---------------------------------------------------------------------------------------------------------------
			// pg_stat_slru
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_stat_slru", "PostgreSQL accesses certain on-disk information via SLRU (simple least-recently-used) caches. The pg_stat_slru view will contain one row for each tracked SLRU cache, showing statistics about access to cached pages.");

			mtd.addColumn("pg_stat_slru", "name",            "Name of the SLRU");
			mtd.addColumn("pg_stat_slru", "blks_zeroed",     "Number of blocks zeroed during initializations");
			mtd.addColumn("pg_stat_slru", "blks_hit",        "Number of times disk blocks were found already in the SLRU, so that a read was not necessary (this only includes hits in the SLRU, not the operating system's file system cache)");
			mtd.addColumn("pg_stat_slru", "blks_read",       "Number of disk blocks read for this SLRU");
			mtd.addColumn("pg_stat_slru", "blks_written",    "Number of disk blocks written for this SLRU");
			mtd.addColumn("pg_stat_slru", "blks_exists",     "Number of blocks checked for existence for this SLRU");
			mtd.addColumn("pg_stat_slru", "flushes",         "Number of flushes of dirty data for this SLRU");
			mtd.addColumn("pg_stat_slru", "truncates",       "Number of truncates for this SLRU");
			mtd.addColumn("pg_stat_slru", "stats_reset",     "Time at which these statistics were last reset");
			
			
			
			//---------------------------------------------------------------------------------------------------------------
			// pg_stat_replication
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_stat_replication",         "One row per WAL sender process, showing statistics about replication to that sender's connected standby server. See pg_stat_replication for details.");

			mtd.addColumn("pg_stat_replication", "pid",              "Process ID of a WAL sender process");
			mtd.addColumn("pg_stat_replication", "usesysid",         "OID of the user logged into this WAL sender process");
			mtd.addColumn("pg_stat_replication", "usename",          "Name of the user logged into this WAL sender process");
			mtd.addColumn("pg_stat_replication", "application_name", "Name of the application that is connected to this WAL sender");
			mtd.addColumn("pg_stat_replication", "client_addr",      "IP address of the client connected to this WAL sender. If this field is null, it indicates that the client is connected via a Unix socket on the server machine.");
			mtd.addColumn("pg_stat_replication", "client_hostname",  "Host name of the connected client, as reported by a reverse DNS lookup of client_addr. This field will only be non-null for IP connections, and only when log_hostname is enabled.");
			mtd.addColumn("pg_stat_replication", "client_port",      "TCP port number that the client is using for communication with this WAL sender, or -1 if a Unix socket is used");
			mtd.addColumn("pg_stat_replication", "backend_start",    "Time when this process was started, i.e., when the client connected to this WAL sender");
			mtd.addColumn("pg_stat_replication", "backend_xmin",     "This standby's xmin horizon reported by hot_standby_feedback.");
			mtd.addColumn("pg_stat_replication", "state",            "Current WAL sender state");
			mtd.addColumn("pg_stat_replication", "sent_location",    "Last transaction log position sent on this connection");
			mtd.addColumn("pg_stat_replication", "write_location",   "Last transaction log position written to disk by this standby server");
			mtd.addColumn("pg_stat_replication", "flush_location",   "Last transaction log position flushed to disk by this standby server");
			mtd.addColumn("pg_stat_replication", "replay_location",  "Last transaction log position replayed into the database on this standby server");
			mtd.addColumn("pg_stat_replication", "sent_lsn",         "Last write-ahead log location sent on this connection");
			mtd.addColumn("pg_stat_replication", "write_lsn",        "Last write-ahead log location written to disk by this standby server");
			mtd.addColumn("pg_stat_replication", "flush_lsn",        "Last write-ahead log location flushed to disk by this standby server");
			mtd.addColumn("pg_stat_replication", "replay_lsn",       "Last write-ahead log location replayed into the database on this standby server");
			mtd.addColumn("pg_stat_replication", "write_lag",        "Time elapsed between flushing recent WAL locally and receiving notification that this standby server has written it (but not yet flushed it or applied it). This can be used to gauge the delay that synchronous_commit level remote_write incurred while committing if this server was configured as a synchronous standby.");
			mtd.addColumn("pg_stat_replication", "flush_lag",        "Time elapsed between flushing recent WAL locally and receiving notification that this standby server has written and flushed it (but not yet applied it). This can be used to gauge the delay that synchronous_commit level on incurred while committing if this server was configured as a synchronous standby.");
			mtd.addColumn("pg_stat_replication", "replay_lag",       "Time elapsed between flushing recent WAL locally and receiving notification that this standby server has written, flushed and applied it. This can be used to gauge the delay that synchronous_commit level remote_apply incurred while committing if this server was configured as a synchronous standby.");
			mtd.addColumn("pg_stat_replication", "xxx",    "xxx");
			
			mtd.addColumn("pg_stat_replication", "sync_priority",    "Priority of this standby server for being chosen as the synchronous standby in a priority-based synchronous replication. This has no effect in a quorum-based synchronous replication.");
			mtd.addColumn("pg_stat_replication", "sync_state",       "<html>Synchronous state of this standby server. Possible values are:"
			                                                              + "<ul>"
			                                                              + "  <li>async: This standby server is asynchronous.</li>"
			                                                              + "  <li>potential: This standby server is now asynchronous, but can potentially become synchronous if one of current synchronous ones fails.</li>"
			                                                              + "  <li>sync: This standby server is synchronous.</li>"
			                                                              + "  <li>quorum: This standby server is considered as a candidate for quorum standbys.</li>"
			                                                              + "</ul>"
			                                                              + "</html");
			mtd.addColumn("pg_stat_replication", "reply_time",       "Send time of last reply message received from standby server");
			

			//---------------------------------------------------------------------------------------------------------------
			// pg_stat_replication_slots
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_stat_replication_slots",         "The pg_stat_replication_slots view will contain one row per logical replication slot, showing statistics about its usage.");

			mtd.addColumn("pg_stat_replication_slots", "slot_name",       "A unique, cluster-wide identifier for the replication slot");
			mtd.addColumn("pg_stat_replication_slots", "spill_txns",      "Number of transactions spilled to disk once the memory used by logical decoding to decode changes from WAL has exceeded logical_decoding_work_mem. The counter gets incremented for both top-level transactions and subtransactions.");
			mtd.addColumn("pg_stat_replication_slots", "spill_count",     "Number of times transactions were spilled to disk while decoding changes from WAL for this slot. This counter is incremented each time a transaction is spilled, and the same transaction may be spilled multiple times.");
			mtd.addColumn("pg_stat_replication_slots", "spill_bytes",     "Amount of decoded transaction data spilled to disk while performing decoding of changes from WAL for this slot. This and other spill counters can be used to gauge the I/O which occurred during logical decoding and allow tuning logical_decoding_work_mem.");
			mtd.addColumn("pg_stat_replication_slots", "stream_txns",     "Number of in-progress transactions streamed to the decoding output plugin after the memory used by logical decoding to decode changes from WAL for this slot has exceeded logical_decoding_work_mem. Streaming only works with top-level transactions (subtransactions can't be streamed independently), so the counter is not incremented for subtransactions.");
			mtd.addColumn("pg_stat_replication_slots", "stream_count",    "Number of times in-progress transactions were streamed to the decoding output plugin while decoding changes from WAL for this slot. This counter is incremented each time a transaction is streamed, and the same transaction may be streamed multiple times.");
			mtd.addColumn("pg_stat_replication_slots", "stream_bytes",    "Amount of transaction data decoded for streaming in-progress transactions to the decoding output plugin while decoding changes from WAL for this slot. This and other streaming counters for this slot can be used to tune logical_decoding_work_mem.");
			mtd.addColumn("pg_stat_replication_slots", "total_txns",      "Number of decoded transactions sent to the decoding output plugin for this slot. This counts top-level transactions only, and is not incremented for subtransactions. Note that this includes the transactions that are streamed and/or spilled.");
			mtd.addColumn("pg_stat_replication_slots", "total_bytes",     "Amount of transaction data decoded for sending transactions to the decoding output plugin while decoding changes from WAL for this slot. Note that this includes data that is streamed and/or spilled.");
			mtd.addColumn("pg_stat_replication_slots", "stats_reset",     "Time at which these statistics were last reset");


			//---------------------------------------------------------------------------------------------------------------
			// pg_stat_wal_receiver
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_stat_wal_receiver",         "The pg_stat_wal_receiver view will contain only one row, showing statistics about the WAL receiver from that receiver's connected server.");

			mtd.addColumn("pg_stat_wal_receiver", "pid",                    "Process ID of the WAL receiver process");
			mtd.addColumn("pg_stat_wal_receiver", "status",                 "Activity status of the WAL receiver process");
			mtd.addColumn("pg_stat_wal_receiver", "receive_start_lsn",      "First write-ahead log location used when WAL receiver is started");
			mtd.addColumn("pg_stat_wal_receiver", "receive_start_tli",      "First timeline number used when WAL receiver is started");
			mtd.addColumn("pg_stat_wal_receiver", "written_lsn",            "Last write-ahead log location already received and written to disk, but not flushed. This should not be used for data integrity checks.");
			mtd.addColumn("pg_stat_wal_receiver", "flushed_lsn",            "Last write-ahead log location already received and flushed to disk, the initial value of this field being the first log location used when WAL receiver is started");
			mtd.addColumn("pg_stat_wal_receiver", "received_tli",           "Timeline number of last write-ahead log location received and flushed to disk, the initial value of this field being the timeline number of the first log location used when WAL receiver is started");
			mtd.addColumn("pg_stat_wal_receiver", "last_msg_send_time",     "Send time of last message received from origin WAL sender");
			mtd.addColumn("pg_stat_wal_receiver", "last_msg_receipt_time",  "Receipt time of last message received from origin WAL sender");
			mtd.addColumn("pg_stat_wal_receiver", "latest_end_lsn",         "Last write-ahead log location reported to origin WAL sender");
			mtd.addColumn("pg_stat_wal_receiver", "latest_end_time",        "Time of last write-ahead log location reported to origin WAL sender");
			mtd.addColumn("pg_stat_wal_receiver", "slot_name",              "Replication slot name used by this WAL receiver");
			mtd.addColumn("pg_stat_wal_receiver", "sender_host",            "Host of the PostgreSQL instance this WAL receiver is connected to. This can be a host name, an IP address, or a directory path if the connection is via Unix socket. (The path case can be distinguished because it will always be an absolute path, beginning with /.)");
			mtd.addColumn("pg_stat_wal_receiver", "sender_port",            "Port number of the PostgreSQL instance this WAL receiver is connected to.");
			mtd.addColumn("pg_stat_wal_receiver", "conninfo",               "Connection string used by this WAL receiver, with security-sensitive fields obfuscated.");


			//---------------------------------------------------------------------------------------------------------------
			// pg_stat_recovery_prefetch
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_stat_recovery_prefetch",         "The pg_stat_recovery_prefetch view will contain only one row. The columns wal_distance, block_distance and io_depth show current values, and the other columns show cumulative counters that can be reset with the pg_stat_reset_shared function.");

			mtd.addColumn("pg_stat_recovery_prefetch", "stats_reset",      "Time at which these statistics were last reset");
			mtd.addColumn("pg_stat_recovery_prefetch", "prefetch",         "Number of blocks prefetched because they were not in the buffer pool");
			mtd.addColumn("pg_stat_recovery_prefetch", "hit",              "Number of blocks not prefetched because they were already in the buffer pool");
			mtd.addColumn("pg_stat_recovery_prefetch", "skip_init",        "Number of blocks not prefetched because they would be zero-initialized");
			mtd.addColumn("pg_stat_recovery_prefetch", "skip_new",         "Number of blocks not prefetched because they didn't exist yet");
			mtd.addColumn("pg_stat_recovery_prefetch", "skip_fpw",         "Number of blocks not prefetched because a full page image was included in the WAL");
			mtd.addColumn("pg_stat_recovery_prefetch", "skip_rep",         "Number of blocks not prefetched because they were already recently prefetched");
			mtd.addColumn("pg_stat_recovery_prefetch", "wal_distance",     "How many bytes ahead the prefetcher is looking");
			mtd.addColumn("pg_stat_recovery_prefetch", "block_distance",   "How many blocks ahead the prefetcher is looking");
			mtd.addColumn("pg_stat_recovery_prefetch", "io_depth",         "How many prefetches have been initiated but are not yet known to have completed");


			//---------------------------------------------------------------------------------------------------------------
			// pg_stat_subscription
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_stat_subscription",         "The pg_stat_subscription ");

			mtd.addColumn("pg_stat_subscription", "subid",                  "OID of the subscription");
			mtd.addColumn("pg_stat_subscription", "subname",                "Name of the subscription");
			mtd.addColumn("pg_stat_subscription", "pid",                    "Process ID of the subscription worker process");
			mtd.addColumn("pg_stat_subscription", "relid",                  "OID of the relation that the worker is synchronizing; null for the main apply worker");
			mtd.addColumn("pg_stat_subscription", "received_lsn",           "Last write-ahead log location received, the initial value of this field being 0");
			mtd.addColumn("pg_stat_subscription", "last_msg_send_time",     "Send time of last message received from origin WAL sender");
			mtd.addColumn("pg_stat_subscription", "last_msg_receipt_time",  "Receipt time of last message received from origin WAL sender");
			mtd.addColumn("pg_stat_subscription", "latest_end_lsn",         "Last write-ahead log location reported to origin WAL sender");
			mtd.addColumn("pg_stat_subscription", "latest_end_time",        "Time of last write-ahead log location reported to origin WAL sender");


			//---------------------------------------------------------------------------------------------------------------
			// pg_stat_subscription
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_stat_subscription_stats",         "The pg_stat_subscription_stats view will contain one row per subscription.");

			mtd.addColumn("pg_stat_subscription_stats", "subid",                 "OID of the subscription");
			mtd.addColumn("pg_stat_subscription_stats", "subname",               "Name of the subscription");
			mtd.addColumn("pg_stat_subscription_stats", "apply_error_count",     "Number of times an error occurred while applying changes");
			mtd.addColumn("pg_stat_subscription_stats", "sync_error_count",      "Number of times an error occurred during the initial table synchronization");
			mtd.addColumn("pg_stat_subscription_stats", "stats_reset",           "Time at which these statistics were last reset");


			//---------------------------------------------------------------------------------------------------------------
			// pg_stat_ssl
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_stat_ssl",         "The pg_stat_ssl view will contain one row per backend or WAL sender process, showing statistics about SSL usage on this connection. It can be joined to pg_stat_activity or pg_stat_replication on the pid column to get more details about the connection.");

			mtd.addColumn("pg_stat_ssl", "pid",                 "Process ID of a backend or WAL sender process");
			mtd.addColumn("pg_stat_ssl", "ssl",                 "True if SSL is used on this connection");
			mtd.addColumn("pg_stat_ssl", "version",             "Version of SSL in use, or NULL if SSL is not in use on this connection");
			mtd.addColumn("pg_stat_ssl", "cipher",              "Name of SSL cipher in use, or NULL if SSL is not in use on this connection");
			mtd.addColumn("pg_stat_ssl", "bits",                "Number of bits in the encryption algorithm used, or NULL if SSL is not used on this connection");
			mtd.addColumn("pg_stat_ssl", "client_dn",           "Distinguished Name (DN) field from the client certificate used, or NULL if no client certificate was supplied or if SSL is not in use on this connection. This field is truncated if the DN field is longer than NAMEDATALEN (64 characters in a standard build).");
			mtd.addColumn("pg_stat_ssl", "client_serial",       "Serial number of the client certificate, or NULL if no client certificate was supplied or if SSL is not in use on this connection. The combination of certificate serial number and certificate issuer uniquely identifies a certificate (unless the issuer erroneously reuses serial numbers).");
			mtd.addColumn("pg_stat_ssl", "issuer_dn",           "DN of the issuer of the client certificate, or NULL if no client certificate was supplied or if SSL is not in use on this connection. This field is truncated like client_dn.");


			//---------------------------------------------------------------------------------------------------------------
			// pg_stat_gssapi
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_stat_gssapi",         "The pg_stat_gssapi view will contain one row per backend, showing information about GSSAPI usage on this connection. It can be joined to pg_stat_activity or pg_stat_replication on the pid column to get more details about the connection.");

			mtd.addColumn("pg_stat_gssapi", "pid",                 "Process ID of a backend");
			mtd.addColumn("pg_stat_gssapi", "gss_authenticated",   "True if GSSAPI authentication was used for this connection");
			mtd.addColumn("pg_stat_gssapi", "principal",           "Principal used to authenticate this connection, or NULL if GSSAPI was not used to authenticate this connection. This field is truncated if the principal is longer than NAMEDATALEN (64 characters in a standard build).");
			mtd.addColumn("pg_stat_gssapi", "encrypted",           "True if GSSAPI encryption is in use on this connection");


			//---------------------------------------------------------------------------------------------------------------
			// pg_stat_progress_analyze
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_stat_progress_analyze",         "Whenever ANALYZE is running, the pg_stat_progress_analyze view will contain a row for each backend that is currently running that command. The tables below describe the information that will be reported and provide information about how to interpret it.");

			String phaseDesc = "" +
					"<table> " +
					"  <thead> " +
					"    <tr> <th>Phase</th> <th>Description</th> </tr> " +
					"  </thead> " +
					"  <tbody> " +
					"    <tr> <td>initializing	                 </td> <td> The command is preparing to begin scanning the heap. This phase is expected to be very brief. </td> </tr> " +
					"    <tr> <td>acquiring sample rows	         </td> <td> The command is currently scanning the table given by relid to obtain sample rows. </td> </tr> " +
					"    <tr> <td>acquiring inherited sample rows </td> <td> The command is currently scanning child tables to obtain sample rows. Columns child_tables_total, child_tables_done, and current_child_table_relid contain the progress information for this phase. </td> </tr> " +
					"    <tr> <td>computing statistics	         </td> <td> The command is computing statistics from the sample rows obtained during the table scan. </td> </tr> " +
					"    <tr> <td>computing extended statistics	 </td> <td> The command is computing extended statistics from the sample rows obtained during the table scan. </td> </tr> " +
					"    <tr> <td>finalizing analyze	             </td> <td> The command is updating pg_class. When this phase is completed, ANALYZE will end. </td> </tr> " +
					"  </tbody> " +
					"</table> " +
					"";
			
			mtd.addColumn("pg_stat_progress_analyze", "pid"                      , "Process ID of backend.");
			mtd.addColumn("pg_stat_progress_analyze", "datid"                    , "OID of the database to which this backend is connected.");
			mtd.addColumn("pg_stat_progress_analyze", "datname"                  , "Name of the database to which this backend is connected.");
			mtd.addColumn("pg_stat_progress_analyze", "relid"                    , "OID of the table being analyzed.");
			mtd.addColumn("pg_stat_progress_analyze", "phase"                    , "<html>Current processing phase<br>" + phaseDesc + "</html>");
			mtd.addColumn("pg_stat_progress_analyze", "sample_blks_total"        , "Total number of heap blocks that will be sampled.");
			mtd.addColumn("pg_stat_progress_analyze", "sample_blks_scanned"      , "Number of heap blocks scanned.");
			mtd.addColumn("pg_stat_progress_analyze", "ext_stats_total"          , "Number of extended statistics.");
			mtd.addColumn("pg_stat_progress_analyze", "ext_stats_computed"       , "Number of extended statistics computed. This counter only advances when the phase is computing extended statistics.");
			mtd.addColumn("pg_stat_progress_analyze", "child_tables_total"       , "Number of child tables.");
			mtd.addColumn("pg_stat_progress_analyze", "child_tables_done"        , "Number of child tables scanned. This counter only advances when the phase is acquiring inherited sample rows.");
			mtd.addColumn("pg_stat_progress_analyze", "current_child_table_relid", "OID of the child table currently being scanned. This field is only valid when the phase is acquiring inherited sample rows.");


			//---------------------------------------------------------------------------------------------------------------
			// pg_stat_progress_create_index
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_stat_progress_create_index",         "Whenever CREATE INDEX or REINDEX is running, the pg_stat_progress_create_index view will contain one row for each backend that is currently creating indexes. The tables below describe the information that will be reported and provide information about how to interpret it.");

			phaseDesc = "" +
					"<table> " +
					"  <thead> " +
					"    <tr> <th>Phase</th> <th>Description</th> </tr> " +
					"  </thead> " +
					"  <tbody> " +
					"    <tr> <td>initializing                             </td> <td> CREATE INDEX or REINDEX is preparing to create the index. This phase is expected to be very brief. </td> </tr> " +
					"    <tr> <td>waiting for writers before build         </td> <td> CREATE INDEX CONCURRENTLY or REINDEX CONCURRENTLY is waiting for transactions with write locks that can potentially see the table to finish. This phase is skipped when not in concurrent mode. Columns lockers_total, lockers_done and current_locker_pid contain the progress information for this phase. </td> </tr> " +
					"    <tr> <td>building index                           </td> <td> The index is being built by the access method-specific code. In this phase, access methods that support progress reporting fill in their own progress data, and the subphase is indicated in this column. Typically, blocks_total and blocks_done will contain progress data, as well as potentially tuples_total and tuples_done. </td> </tr> " +
					"    <tr> <td>waiting for writers before validation    </td> <td> CREATE INDEX CONCURRENTLY or REINDEX CONCURRENTLY is waiting for transactions with write locks that can potentially write into the table to finish. This phase is skipped when not in concurrent mode. Columns lockers_total, lockers_done and current_locker_pid contain the progress information for this phase. </td> </tr> " +
					"    <tr> <td>index validation: scanning index         </td> <td> CREATE INDEX CONCURRENTLY is scanning the index searching for tuples that need to be validated. This phase is skipped when not in concurrent mode. Columns blocks_total (set to the total size of the index) and blocks_done contain the progress information for this phase. </td> </tr> " +
					"    <tr> <td>index validation: sorting tuples         </td> <td> CREATE INDEX CONCURRENTLY is sorting the output of the index scanning phase. </td> </tr> " +
					"    <tr> <td>index validation: scanning table         </td> <td> CREATE INDEX CONCURRENTLY is scanning the table to validate the index tuples collected in the previous two phases. This phase is skipped when not in concurrent mode. Columns blocks_total (set to the total size of the table) and blocks_done contain the progress information for this phase. </td> </tr> " +
					"    <tr> <td>waiting for old snapshots                </td> <td> CREATE INDEX CONCURRENTLY or REINDEX CONCURRENTLY is waiting for transactions that can potentially see the table to release their snapshots. This phase is skipped when not in concurrent mode. Columns lockers_total, lockers_done and current_locker_pid contain the progress information for this phase. </td> </tr> " +
					"    <tr> <td>waiting for readers before marking dead  </td> <td> REINDEX CONCURRENTLY is waiting for transactions with read locks on the table to finish, before marking the old index dead. This phase is skipped when not in concurrent mode. Columns lockers_total, lockers_done and current_locker_pid contain the progress information for this phase. </td> </tr> " +
					"    <tr> <td>waiting for readers before dropping      </td> <td> REINDEX CONCURRENTLY is waiting for transactions with read locks on the table to finish, before dropping the old index. This phase is skipped when not in concurrent mode. Columns lockers_total, lockers_done and current_locker_pid contain the progress information for this phase. </td> </tr> " +
					"  </tbody> " +
					"</table> " +
					"";

			mtd.addColumn("pg_stat_progress_create_index", "pid"               , "Process ID of backend.");
			mtd.addColumn("pg_stat_progress_create_index", "datid"             , "OID of the database to which this backend is connected.");
			mtd.addColumn("pg_stat_progress_create_index", "datname"           , "Name of the database to which this backend is connected.");
			mtd.addColumn("pg_stat_progress_create_index", "relid"             , "OID of the table on which the index is being created.");
			mtd.addColumn("pg_stat_progress_create_index", "index_relid"       , "OID of the index being created or reindexed. During a non-concurrent CREATE INDEX, this is 0.");
			mtd.addColumn("pg_stat_progress_create_index", "command"           , "The command that is running: CREATE INDEX, CREATE INDEX CONCURRENTLY, REINDEX, or REINDEX CONCURRENTLY.");
			mtd.addColumn("pg_stat_progress_create_index", "phase"             , "<html>Current processing phase of index creation<br>" + phaseDesc + "</html>");
			mtd.addColumn("pg_stat_progress_create_index", "lockers_total"     , "Total number of lockers to wait for, when applicable.");
			mtd.addColumn("pg_stat_progress_create_index", "lockers_done"      , "Number of lockers already waited for.");
			mtd.addColumn("pg_stat_progress_create_index", "current_locker_pid", "Process ID of the locker currently being waited for.");
			mtd.addColumn("pg_stat_progress_create_index", "blocks_total"      , "Total number of blocks to be processed in the current phase.");
			mtd.addColumn("pg_stat_progress_create_index", "blocks_done"       , "Number of blocks already processed in the current phase.");
			mtd.addColumn("pg_stat_progress_create_index", "tuples_total"      , "Total number of tuples to be processed in the current phase.");
			mtd.addColumn("pg_stat_progress_create_index", "tuples_done"       , "Number of tuples already processed in the current phase.");
			mtd.addColumn("pg_stat_progress_create_index", "partitions_total"  , "When creating an index on a partitioned table, this column is set to the total number of partitions on which the index is to be created. This field is 0 during a REINDEX.");
			mtd.addColumn("pg_stat_progress_create_index", "partitions_done"   , "When creating an index on a partitioned table, this column is set to the number of partitions on which the index has been created. This field is 0 during a REINDEX.");


			//---------------------------------------------------------------------------------------------------------------
			// pg_stat_progress_vacuum
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_stat_progress_vacuum",         "Whenever VACUUM is running, the pg_stat_progress_vacuum view will contain one row for each backend (including autovacuum worker processes) that is currently vacuuming. The tables below describe the information that will be reported and provide information about how to interpret it. Progress for VACUUM FULL commands is reported via pg_stat_progress_cluster because both VACUUM FULL and CLUSTER rewrite the table, while regular VACUUM only modifies it in place");

			phaseDesc = "" +
					"<table> " +
					"  <thead> " +
					"    <tr> <th>Phase</th> <th>Description</th> </tr> " +
					"  </thead> " +
					"  <tbody> " +
					"    <tr> <td>initializing              </td> <td> VACUUM is preparing to begin scanning the heap. This phase is expected to be very brief. </td> </tr> " +
					"    <tr> <td>scanning heap             </td> <td> VACUUM is currently scanning the heap. It will prune and defragment each page if required, and possibly perform freezing activity. The heap_blks_scanned column can be used to monitor the progress of the scan. </td> </tr> " +
					"    <tr> <td>vacuuming indexes         </td> <td> VACUUM is currently vacuuming the indexes. If a table has any indexes, this will happen at least once per vacuum, after the heap has been completely scanned. It may happen multiple times per vacuum if maintenance_work_mem (or, in the case of autovacuum, autovacuum_work_mem if set) is insufficient to store the number of dead tuples found. </td> </tr> " +
					"    <tr> <td>vacuuming heap            </td> <td> VACUUM is currently vacuuming the heap. Vacuuming the heap is distinct from scanning the heap, and occurs after each instance of vacuuming indexes. If heap_blks_scanned is less than heap_blks_total, the system will return to scanning the heap after this phase is completed; otherwise, it will begin cleaning up indexes after this phase is completed. </td> </tr> " +
					"    <tr> <td>cleaning up indexes       </td> <td> VACUUM is currently cleaning up indexes. This occurs after the heap has been completely scanned and all vacuuming of the indexes and the heap has been completed. </td> </tr> " +
					"    <tr> <td>truncating heap           </td> <td> VACUUM is currently truncating the heap so as to return empty pages at the end of the relation to the operating system. This occurs after cleaning up indexes. </td> </tr> " +
					"    <tr> <td>performing final cleanup  </td> <td> VACUUM is performing final cleanup. During this phase, VACUUM will vacuum the free space map, update statistics in pg_class, and report statistics to the cumulative statistics system. When this phase is completed, VACUUM will end. </td> </tr> " +
					"  </tbody> " +
					"</table> " +
					"";

			mtd.addColumn("pg_stat_progress_vacuum", "pid"               , "Process ID of backend.");
			mtd.addColumn("pg_stat_progress_vacuum", "datid"             , "OID of the database to which this backend is connected.");
			mtd.addColumn("pg_stat_progress_vacuum", "datname"           , "Name of the database to which this backend is connected.");
			mtd.addColumn("pg_stat_progress_vacuum", "relid"             , "OID of the table being vacuumed.");
			mtd.addColumn("pg_stat_progress_vacuum", "phase"             , "<html>Current processing phase of vacuum.<br>" + phaseDesc + "</html>");
			mtd.addColumn("pg_stat_progress_vacuum", "heap_blks_total"   , "Total number of heap blocks in the table. This number is reported as of the beginning of the scan; blocks added later will not be (and need not be) visited by this VACUUM.");
			mtd.addColumn("pg_stat_progress_vacuum", "heap_blks_scanned" , "Number of heap blocks scanned. Because the visibility map is used to optimize scans, some blocks will be skipped without inspection; skipped blocks are included in this total, so that this number will eventually become equal to heap_blks_total when the vacuum is complete. This counter only advances when the phase is scanning heap.");
			mtd.addColumn("pg_stat_progress_vacuum", "heap_blks_vacuumed", "Number of heap blocks vacuumed. Unless the table has no indexes, this counter only advances when the phase is vacuuming heap. Blocks that contain no dead tuples are skipped, so the counter may sometimes skip forward in large increments.");
			mtd.addColumn("pg_stat_progress_vacuum", "index_vacuum_count", "Number of completed index vacuum cycles.");
			mtd.addColumn("pg_stat_progress_vacuum", "max_dead_tuples"   , "Number of dead tuples that we can store before needing to perform an index vacuum cycle, based on maintenance_work_mem.");
			mtd.addColumn("pg_stat_progress_vacuum", "num_dead_tuples"   , "Number of dead tuples collected since the last index vacuum cycle");


			//---------------------------------------------------------------------------------------------------------------
			// pg_stat_progress_cluster
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_stat_progress_cluster",         "Whenever CLUSTER or VACUUM FULL is running, the pg_stat_progress_cluster view will contain a row for each backend that is currently running either command. The tables below describe the information that will be reported and provide information about how to interpret it.");

			phaseDesc = "" +
					"<table> " +
					"  <thead> " +
					"    <tr> <th>Phase</th> <th>Description</th> </tr> " +
					"  </thead> " +
					"  <tbody> " +
					"    <tr> <td>initializing               </td> <td> The command is preparing to begin scanning the heap. This phase is expected to be very brief. </td> </tr> " +
					"    <tr> <td>seq scanning heap          </td> <td> The command is currently scanning the table using a sequential scan. </td> </tr> " +
					"    <tr> <td>index scanning heap        </td> <td> CLUSTER is currently scanning the table using an index scan. </td> </tr> " +
					"    <tr> <td>sorting tuples             </td> <td> CLUSTER is currently sorting tuples. </td> </tr> " +
					"    <tr> <td>writing new heap           </td> <td> CLUSTER is currently writing the new heap. </td> </tr> " +
					"    <tr> <td>swapping relation files    </td> <td> The command is currently swapping newly-built files into place. </td> </tr> " +
					"    <tr> <td>rebuilding index           </td> <td> The command is currently rebuilding an index. </td> </tr> " +
					"    <tr> <td>performing final cleanup   </td> <td> The command is performing final cleanup. When this phase is completed, CLUSTER or VACUUM FULL will end. </td> </tr> " +
					"  </tbody> " +
					"</table> " +
					"";

			mtd.addColumn("pg_stat_progress_cluster", "pid"                , "Process ID of backend.");
			mtd.addColumn("pg_stat_progress_cluster", "datid"              , "OID of the database to which this backend is connected.");
			mtd.addColumn("pg_stat_progress_cluster", "datname"            , "Name of the database to which this backend is connected.");
			mtd.addColumn("pg_stat_progress_cluster", "relid"              , "OID of the table being clustered.");
			mtd.addColumn("pg_stat_progress_cluster", "command"            , "The command that is running. Either CLUSTER or VACUUM FULL.");
			mtd.addColumn("pg_stat_progress_cluster", "phase"              , "<html>Current processing phase.<br>" + phaseDesc + "</html>");
			mtd.addColumn("pg_stat_progress_cluster", "cluster_index_relid", "If the table is being scanned using an index, this is the OID of the index being used; otherwise, it is zero.");
			mtd.addColumn("pg_stat_progress_cluster", "heap_tuples_scanned", "Number of heap tuples scanned. This counter only advances when the phase is seq scanning heap, index scanning heap or writing new heap.");
			mtd.addColumn("pg_stat_progress_cluster", "heap_tuples_written", "Number of heap tuples written. This counter only advances when the phase is seq scanning heap, index scanning heap or writing new heap.");
			mtd.addColumn("pg_stat_progress_cluster", "heap_blks_total"    , "Total number of heap blocks in the table. This number is reported as of the beginning of seq scanning heap.");
			mtd.addColumn("pg_stat_progress_cluster", "heap_blks_scanned"  , "Number of heap blocks scanned. This counter only advances when the phase is seq scanning heap.");
			mtd.addColumn("pg_stat_progress_cluster", "index_rebuild_count", "Number of indexes rebuilt. This counter only advances when the phase is rebuilding index.");


			//---------------------------------------------------------------------------------------------------------------
			// pg_stat_progress_basebackup
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_stat_progress_basebackup",         "Whenever an application like pg_basebackup is taking a base backup, the pg_stat_progress_basebackup view will contain a row for each WAL sender process that is currently running the BASE_BACKUP replication command and streaming the backup. The tables below describe the information that will be reported and provide information about how to interpret it.");

			phaseDesc = "" +
					"<table> " +
					"  <thead> " +
					"    <tr> <th>Phase</th> <th>Description</th> </tr> " +
					"  </thead> " +
					"  <tbody> " +
					"    <tr> <td>initializing                          </td> <td> The WAL sender process is preparing to begin the backup. This phase is expected to be very brief. </td> </tr> " +
					"    <tr> <td>waiting for checkpoint to finish      </td> <td> The WAL sender process is currently performing pg_backup_start to prepare to take a base backup, and waiting for the start-of-backup checkpoint to finish. </td> </tr> " +
					"    <tr> <td>estimating backup size                </td> <td> The WAL sender process is currently estimating the total amount of database files that will be streamed as a base backup. </td> </tr> " +
					"    <tr> <td>streaming database files              </td> <td> The WAL sender process is currently streaming database files as a base backup. </td> </tr> " +
					"    <tr> <td>waiting for wal archiving to finish   </td> <td> The WAL sender process is currently performing pg_backup_stop to finish the backup, and waiting for all the WAL files required for the base backup to be successfully archived. If either --wal-method=none or --wal-method=stream is specified in pg_basebackup, the backup will end when this phase is completed. </td> </tr> " +
					"    <tr> <td>transferring wal files                </td> <td> The WAL sender process is currently transferring all WAL logs generated during the backup. This phase occurs after waiting for wal archiving to finish phase if --wal-method=fetch is specified in pg_basebackup. The backup will end when this phase is completed. </td> </tr> " +
					"  </tbody> " +
					"</table> " +
					"";

			mtd.addColumn("pg_stat_progress_basebackup", "pid"                 , "Process ID of a WAL sender process.");
			mtd.addColumn("pg_stat_progress_basebackup", "phase"               , "<html>Current processing phase.<br>" + phaseDesc + "</html>");
			mtd.addColumn("pg_stat_progress_basebackup", "backup_total"        , "Total amount of data that will be streamed. This is estimated and reported as of the beginning of streaming database files phase. Note that this is only an approximation since the database may change during streaming database files phase and WAL log may be included in the backup later. This is always the same value as backup_streamed once the amount of data streamed exceeds the estimated total size. If the estimation is disabled in pg_basebackup (i.e., --no-estimate-size option is specified), this is NULL.");
			mtd.addColumn("pg_stat_progress_basebackup", "backup_streamed"     , "Amount of data streamed. This counter only advances when the phase is streaming database files or transferring wal files.");
			mtd.addColumn("pg_stat_progress_basebackup", "tablespaces_total"   , "Total number of tablespaces that will be streamed.");
			mtd.addColumn("pg_stat_progress_basebackup", "tablespaces_streamed", "Number of tablespaces streamed. This counter only advances when the phase is streaming database files.");


			//---------------------------------------------------------------------------------------------------------------
			// pg_stat_progress_copy
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_stat_progress_copy",         "Whenever COPY is running, the pg_stat_progress_copy view will contain one row for each backend that is currently running a COPY command. The table below describes the information that will be reported and provides information about how to interpret it.");

			mtd.addColumn("pg_stat_progress_copy", "pid"             , "Process ID of backend.");
			mtd.addColumn("pg_stat_progress_copy", "datid"           , "OID of the database to which this backend is connected.");
			mtd.addColumn("pg_stat_progress_copy", "datname"         , "Name of the database to which this backend is connected.");
			mtd.addColumn("pg_stat_progress_copy", "relid"           , "OID of the table on which the COPY command is executed. It is set to 0 if copying from a SELECT query.");
			mtd.addColumn("pg_stat_progress_copy", "command"         , "The command that is running: COPY FROM, or COPY TO.");
			mtd.addColumn("pg_stat_progress_copy", "type"            , "The io type that the data is read from or written to: FILE, PROGRAM, PIPE (for COPY FROM STDIN and COPY TO STDOUT), or CALLBACK (used for example during the initial table synchronization in logical replication).");
			mtd.addColumn("pg_stat_progress_copy", "bytes_processed" , "Number of bytes already processed by COPY command.");
			mtd.addColumn("pg_stat_progress_copy", "bytes_total"     , "Size of source file for COPY FROM command in bytes. It is set to 0 if not available.");
			mtd.addColumn("pg_stat_progress_copy", "tuples_processed", "Number of tuples already processed by COPY command.");
			mtd.addColumn("pg_stat_progress_copy", "tuples_excluded" , "Number of tuples not processed because they were excluded by the WHERE clause of the COPY command.");


			//---------------------------------------------------------------------------------------------------------------
			// pg_xxx
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_xxx",         "xxx");

			mtd.addColumn("pg_xxx", "xxx",                 "xxx");


			//---------------------------------------------------------------------------------------------------------------
			// pg_xxx
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_xxx",         "xxx");

			mtd.addColumn("pg_xxx", "xxx",                 "xxx");


			//---------------------------------------------------------------------------------------------------------------
			// pg_stat_statements
			// NOTE: this "table" is NOT present by default: 
			//       The module must be loaded by adding pg_stat_statements to shared_preload_libraries in postgresql.conf, because it requires additional shared memory. This means that a server restart is needed to add or remove the module.
			// Source: http://www.postgresql.org/docs/9.4/static/pgstatstatements.html
// maybe usefull: https://github.com/pganalyze/pganalyze-collector/blob/master/pgacollector
// http://www.postgresql.org/docs/current/static/auto-explain.html   --- The auto_explain module provides a means for logging execution plans of slow statements automatically, without having to run EXPLAIN by hand. This is especially helpful for tracking down un-optimized queries in large applications.
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_stat_statements",  "The pg_stat_statements module provides a means for tracking execution statistics of all SQL statements executed by a server");

			mtd.addColumn("pg_stat_statements", "userid",                  "OID of user who executed the statement");
			mtd.addColumn("pg_stat_statements", "dbid",                    "OID of database in which the statement was executed");
			mtd.addColumn("pg_stat_statements", "toplevel",                "True if the query was executed as a top-level statement (always true if pg_stat_statements.track is set to top)");
			mtd.addColumn("pg_stat_statements", "queryid",                 "Internal hash code, computed from the statement's parse tree");
			mtd.addColumn("pg_stat_statements", "query",                   "Text of a representative statement");
			mtd.addColumn("pg_stat_statements", "plans",                   "Number of times the statement was planned (if pg_stat_statements.track_planning is enabled, otherwise zero)");
			mtd.addColumn("pg_stat_statements", "total_plan_time",         "Total time spent planning the statement, in milliseconds (if pg_stat_statements.track_planning is enabled, otherwise zero)");
			mtd.addColumn("pg_stat_statements", "min_plan_time",           "Minimum time spent planning the statement, in milliseconds (if pg_stat_statements.track_planning is enabled, otherwise zero)");
			mtd.addColumn("pg_stat_statements", "max_plan_time",           "Maximum time spent planning the statement, in milliseconds (if pg_stat_statements.track_planning is enabled, otherwise zero)");
			mtd.addColumn("pg_stat_statements", "mean_plan_time",          "Mean time spent planning the statement, in milliseconds (if pg_stat_statements.track_planning is enabled, otherwise zero)");
			mtd.addColumn("pg_stat_statements", "stddev_plan_time",        "Population standard deviation of time spent planning the statement, in milliseconds (if pg_stat_statements.track_planning is enabled, otherwise zero)");
			mtd.addColumn("pg_stat_statements", "calls",                   "Number of times executed");
			mtd.addColumn("pg_stat_statements", "total_time",              "Total time spent in the statement, in milliseconds");
			mtd.addColumn("pg_stat_statements", "total_exec_time",         "Total time spent executing the statement, in milliseconds");
			mtd.addColumn("pg_stat_statements", "min_exec_time",           "Minimum time spent executing the statement, in milliseconds");
			mtd.addColumn("pg_stat_statements", "max_exec_time",           "Maximum time spent executing the statement, in milliseconds");
			mtd.addColumn("pg_stat_statements", "mean_exec_time",          "Mean time spent executing the statement, in milliseconds");
			mtd.addColumn("pg_stat_statements", "stddev_exec_time",        "Population standard deviation of time spent executing the statement, in milliseconds");
			mtd.addColumn("pg_stat_statements", "rows",                    "Total number of rows retrieved or affected by the statement");
			mtd.addColumn("pg_stat_statements", "shared_blks_hit",         "Total number of shared block cache hits by the statement");
			mtd.addColumn("pg_stat_statements", "shared_blks_read",        "Total number of shared blocks read by the statement");
			mtd.addColumn("pg_stat_statements", "shared_blks_dirtied",     "Total number of shared blocks dirtied by the statement");
			mtd.addColumn("pg_stat_statements", "shared_blks_written",     "Total number of shared blocks written by the statement");
			mtd.addColumn("pg_stat_statements", "local_blks_hit",          "Total number of local block cache hits by the statement");
			mtd.addColumn("pg_stat_statements", "local_blks_read",         "Total number of local blocks read by the statement");
			mtd.addColumn("pg_stat_statements", "local_blks_dirtied",      "Total number of local blocks dirtied by the statement");
			mtd.addColumn("pg_stat_statements", "local_blks_written",      "Total number of local blocks written by the statement");
			mtd.addColumn("pg_stat_statements", "temp_blks_read",          "Total number of temp blocks read by the statement");
			mtd.addColumn("pg_stat_statements", "temp_blks_written",       "Total number of temp blocks written by the statement");
			mtd.addColumn("pg_stat_statements", "blk_read_time",           "Total time the statement spent reading blocks, in milliseconds (if track_io_timing is enabled, otherwise zero)");
			mtd.addColumn("pg_stat_statements", "blk_write_time",          "Total time the statement spent writing blocks, in milliseconds (if track_io_timing is enabled, otherwise zero)");
			mtd.addColumn("pg_stat_statements", "temp_blk_read_time",      "Total time the statement spent reading temporary file blocks, in milliseconds (if track_io_timing is enabled, otherwise zero)");
			mtd.addColumn("pg_stat_statements", "temp_blk_write_time",     "Total time the statement spent writing temporary file blocks, in milliseconds (if track_io_timing is enabled, otherwise zero)");
			mtd.addColumn("pg_stat_statements", "wal_records",             "Total number of WAL records generated by the statement");
			mtd.addColumn("pg_stat_statements", "wal_fpi",                 "Total number of WAL full page images generated by the statement");
			mtd.addColumn("pg_stat_statements", "wal_bytes",               "Total amount of WAL generated by the statement in bytes");
			mtd.addColumn("pg_stat_statements", "jit_functions",           "Total number of functions JIT-compiled by the statement");
			mtd.addColumn("pg_stat_statements", "jit_generation_time",     "Total time spent by the statement on generating JIT code, in milliseconds");
			mtd.addColumn("pg_stat_statements", "jit_inlining_count",      "Number of times functions have been inlined");
			mtd.addColumn("pg_stat_statements", "jit_inlining_time",       "Total time spent by the statement on inlining functions, in milliseconds");
			mtd.addColumn("pg_stat_statements", "jit_optimization_count",  "Number of times the statement has been optimized");
			mtd.addColumn("pg_stat_statements", "jit_optimization_time",   "Total time spent by the statement on optimizing, in milliseconds");
			mtd.addColumn("pg_stat_statements", "jit_emission_count",      "Number of times code has been emitted");
			mtd.addColumn("pg_stat_statements", "jit_emission_time",       "Total time spent by the statement on emitting code, in milliseconds");
			
			//---------------------------------------------------------------------------------------------------------------
			// pg_buffercache
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_buffercache",         "The pg_buffercache module provides a means for examining what's happening in the shared buffer cache in real time.");

			mtd.addColumn("pg_buffercache", "bufferid",          "ID, in the range 1..shared_buffers");
			mtd.addColumn("pg_buffercache", "relfilenode",       "Filenode number of the relation");
			mtd.addColumn("pg_buffercache", "reltablespace",     "Tablespace OID of the relation");
			mtd.addColumn("pg_buffercache", "reldatabase",       "Database OID of the relation");
			mtd.addColumn("pg_buffercache", "relforknumber",     "Fork number within the relation; see common/relpath.h");
			mtd.addColumn("pg_buffercache", "relblocknumber",    "Page number within the relation");
			mtd.addColumn("pg_buffercache", "isdirty",           "Is the page dirty?");
			mtd.addColumn("pg_buffercache", "usagecount",        "Clock-sweep access count");
			mtd.addColumn("pg_buffercache", "pinning_backends",  "Number of backends pinning this buffer");


			//---------------------------------------------------------------------------------------------------------------
			// pg_stat_wait_{profile|current|history}
			//---------------------------------------------------------------------------------------------------------------
			// http://postgresql.nabble.com/Waits-monitoring-td5857215.html
		}
		catch (NameNotFoundException e) 
		{
		//	_logger.warn("Problems in '" + MethodHandles.lookup().lookupClass() + "', adding addMonTableDictForVersion. Caught: " + e); 
			System.out.println("Problems in cm='" + MethodHandles.lookup().lookupClass() + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}
}
