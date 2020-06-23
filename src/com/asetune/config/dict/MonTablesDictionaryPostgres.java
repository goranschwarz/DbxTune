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

			//---------------------------------------------------------------------------------------------------------------
			// pg_stat_activity
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_stat_activity",            "One row per server process, showing information related to the current activity of that process, such as state and current query. See pg_stat_activity for details.");

			mtd.addColumn("pg_stat_activity", "datid",            "OID of the database this backend is connected to");
			mtd.addColumn("pg_stat_activity", "datname",          "Name of the database this backend is connected to");
			mtd.addColumn("pg_stat_activity", "pid",              "Process ID of this backend");
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
			mtd.addColumn("pg_stat_activity", "query",            "Text of this backend's most recent query. If state is active this field shows the currently executing query. In all other states, it shows the last query that was executed.");

			

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
			// pg_stat_database
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_stat_database",            "One row per database, showing database-wide statistics. See pg_stat_database for details.");

			mtd.addColumn("pg_stat_database", "datid",          "OID of a database");
			mtd.addColumn("pg_stat_database", "datname",        "Name of this database");
			mtd.addColumn("pg_stat_database", "numbackends",    "Number of backends currently connected to this database. This is the only column in this view that returns a value reflecting current state; all other columns return the accumulated values since the last reset.");
			mtd.addColumn("pg_stat_database", "xact_commit",    "Number of transactions in this database that have been committed");
			mtd.addColumn("pg_stat_database", "xact_rollback",  "Number of transactions in this database that have been rolled back");
			mtd.addColumn("pg_stat_database", "blks_read",      "Number of disk blocks read in this database");
			mtd.addColumn("pg_stat_database", "blks_hit",       "Number of times disk blocks were found already in the buffer cache, so that a read was not necessary (this only includes hits in the PostgreSQL buffer cache, not the operating system's file system cache)");
			mtd.addColumn("pg_stat_database", "tup_returned",   "Number of rows returned by queries in this database (rows read from memory/disk)");
			mtd.addColumn("pg_stat_database", "tup_fetched",    "Number of rows fetched by queries in this database (rows sent to clients)");
			mtd.addColumn("pg_stat_database", "tup_inserted",   "Number of rows inserted by queries in this database");
			mtd.addColumn("pg_stat_database", "tup_updated",    "Number of rows updated by queries in this database");
			mtd.addColumn("pg_stat_database", "tup_deleted",    "Number of rows deleted by queries in this database");
			mtd.addColumn("pg_stat_database", "conflicts",      "Number of queries canceled due to conflicts with recovery in this database. (Conflicts occur only on standby servers; see pg_stat_database_conflicts for details.)");
			mtd.addColumn("pg_stat_database", "temp_files",     "Number of temporary files created by queries in this database. All temporary files are counted, regardless of why the temporary file was created (e.g., sorting or hashing), and regardless of the log_temp_files setting.");
			mtd.addColumn("pg_stat_database", "temp_bytes",     "Total amount of data written to temporary files by queries in this database. All temporary files are counted, regardless of why the temporary file was created, and regardless of the log_temp_files setting.");
			mtd.addColumn("pg_stat_database", "deadlocks",      "Number of deadlocks detected in this database");
			mtd.addColumn("pg_stat_database", "blk_read_time",  "Time spent reading data file blocks by backends in this database, in milliseconds");
			mtd.addColumn("pg_stat_database", "blk_write_time", "Time spent writing data file blocks by backends in this database, in milliseconds");
			mtd.addColumn("pg_stat_database", "stats_reset",    "Time at which these statistics were last reset");

			

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
			mtd.addColumn("pg_stat_replication", "sync_priority",    "Priority of this standby server for being chosen as the synchronous standby");
			mtd.addColumn("pg_stat_replication", "sync_state",       "Synchronous state of this standby server");
			

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
			// pg_stat_statements
			// NOTE: this "table" is NOT present by default: 
			//       The module must be loaded by adding pg_stat_statements to shared_preload_libraries in postgresql.conf, because it requires additional shared memory. This means that a server restart is needed to add or remove the module.
			// Source: http://www.postgresql.org/docs/9.4/static/pgstatstatements.html
// maybe usefull: https://github.com/pganalyze/pganalyze-collector/blob/master/pgacollector
// http://www.postgresql.org/docs/current/static/auto-explain.html   --- The auto_explain module provides a means for logging execution plans of slow statements automatically, without having to run EXPLAIN by hand. This is especially helpful for tracking down un-optimized queries in large applications.
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("pg_stat_statements",  "The pg_stat_statements module provides a means for tracking execution statistics of all SQL statements executed by a server");

			mtd.addColumn("pg_stat_statements", "userid",              "OID of user who executed the statement");
			mtd.addColumn("pg_stat_statements", "dbid",                "OID of database in which the statement was executed");
			mtd.addColumn("pg_stat_statements", "queryid",             "Internal hash code, computed from the statement's parse tree");
			mtd.addColumn("pg_stat_statements", "query",               "Text of a representative statement");
			mtd.addColumn("pg_stat_statements", "calls",               "Number of times executed");
			mtd.addColumn("pg_stat_statements", "total_time",          "Total time spent in the statement, in milliseconds");
			mtd.addColumn("pg_stat_statements", "rows",                "Total number of rows retrieved or affected by the statement");
			mtd.addColumn("pg_stat_statements", "shared_blks_hit",     "Total number of shared block cache hits by the statement");
			mtd.addColumn("pg_stat_statements", "shared_blks_read",    "Total number of shared blocks read by the statement");
			mtd.addColumn("pg_stat_statements", "shared_blks_dirtied", "Total number of shared blocks dirtied by the statement");
			mtd.addColumn("pg_stat_statements", "shared_blks_written", "Total number of shared blocks written by the statement");
			mtd.addColumn("pg_stat_statements", "local_blks_hit",      "Total number of local block cache hits by the statement");
			mtd.addColumn("pg_stat_statements", "local_blks_read",     "Total number of local blocks read by the statement");
			mtd.addColumn("pg_stat_statements", "local_blks_dirtied",  "Total number of local blocks dirtied by the statement");
			mtd.addColumn("pg_stat_statements", "local_blks_written",  "Total number of local blocks written by the statement");
			mtd.addColumn("pg_stat_statements", "temp_blks_read",      "Total number of temp blocks read by the statement");
			mtd.addColumn("pg_stat_statements", "temp_blks_written",   "Total number of temp blocks written by the statement");
			mtd.addColumn("pg_stat_statements", "blk_read_time",       "Total time the statement spent reading blocks, in milliseconds (if track_io_timing is enabled, otherwise zero)");
			mtd.addColumn("pg_stat_statements", "blk_write_time",      "Total time the statement spent writing blocks, in milliseconds (if track_io_timing is enabled, otherwise zero)");

			
			//---------------------------------------------------------------------------------------------------------------
			// pg_stat_wait_{profile|current|history}
			//---------------------------------------------------------------------------------------------------------------
			// http://postgresql.nabble.com/Waits-monitoring-td5857215.html
		}
		catch (NameNotFoundException e)
		{
			/* ignore */
		}
	}
}
