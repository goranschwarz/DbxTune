/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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
package com.dbxtune.config.dbms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.hostmon.HostMonitorConnection;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoMySql;
import com.dbxtune.utils.Ver;

/**
 * The text tabs in the "DBMS Configuration" dialog for MySQL.
 * <p>
 * Every tab is built from several SQL statements, each with a title (as OracleConfigText): see <code>doOnlineRefresh()</code> in each tab, and <code>textTable()</code> below.<br>
 * All of it is readable by the monitoring login of DbmsSetup/mysql.txt (PROCESS, REPLICATION CLIENT, SELECT on performance_schema): it has no access to the user data.
 * <p>
 * The type names SqlMode, HelpDb and Replicas are the ones older versions had, so the text in older recordings is still shown.
 */
public abstract class MySqlConfigText
{
	/** What sub types exists */
	public enum ConfigType
	{
		 MyServer
		,SqlMode
		,MyChangedVariables
		,HelpDb
		,Replicas
		,MyBinlogs
		,MyPlugins
		,MyPfsSetup
		,MyInnodbStatus
		};

	public static void createAndRegisterAllInstances()
	{
		DbmsConfigTextManager.addInstance(new MySqlConfigText.Server());
		DbmsConfigTextManager.addInstance(new MySqlConfigText.SqlMode());
		DbmsConfigTextManager.addInstance(new MySqlConfigText.ChangedVariables());
		DbmsConfigTextManager.addInstance(new MySqlConfigText.HelpDb());
		DbmsConfigTextManager.addInstance(new MySqlConfigText.Replicas());
		DbmsConfigTextManager.addInstance(new MySqlConfigText.Binlogs());
		DbmsConfigTextManager.addInstance(new MySqlConfigText.Plugins());
		DbmsConfigTextManager.addInstance(new MySqlConfigText.PfsSetup());
		DbmsConfigTextManager.addInstance(new MySqlConfigText.InnodbStatus());
	}

	/**
	 * One part of a tab: a title, then the result of the SQL as a text table. (the same as <code>OracleConfigText.textTable()</code>, with MySQL's errors)
	 * <p>
	 * If the SQL fails (a view that this MySQL version does not have, a missing privilege...) the error text is put there instead, so the rest of the tab is still shown.
	 * <p>
	 * It is ALWAYS the normal text table, also for a result with 0 rows or 1 row: then the text can be read back with <code>ResultSetTableModel.parseTextTables()</code><br>
	 * For the same reason a '|' inside a value is shown as the broken bar character (U+00A6).
	 *
	 * @param conn
	 * @param title      What this is (a second line with an explanation can be added with: "\n--## ...")
	 * @param sql
	 */
	private static String textTable(DbxConnection conn, String title, String sql)
	{
		StringBuilder sb = new StringBuilder();

		sb.append(header(title));

		try
		{
			ResultSetTableModel rstm = ResultSetTableModel.executeQuery(conn, sql, 30, title);

			// A '|' in a value would be taken as a column separator when the text is read back (and that row is then skipped)
			for (int r=0; r<rstm.getRowCount(); r++)
			{
				for (int c=0; c<rstm.getColumnCount(); c++)
				{
					Object val = rstm.getValueAt(r, c);
					if (val instanceof String && ((String) val).indexOf('|') >= 0)
						rstm.setValueAtWithOverride(((String) val).replace('|', '¦'), r, c);
				}
			}

			sb.append(rstm.toAsciiTableString());
		}
		catch (SQLException ex)
		{
			sb.append(couldNotGet(ex));
		}

		return sb.toString();
	}

	/** The title lines of a part */
	private static String header(String title)
	{
		return ""
			+ "\n"
			+ "--######################################################################################\n"
			+ "--## " + title + "\n"
			+ "--######################################################################################\n";
	}

	/** The error, and what to do about it */
	private static String couldNotGet(SQLException ex)
	{
		String msg = "Could not get this: " + ex.getMessage().trim() + "\n";

		// 1142 = SELECT denied for a table, 1227 = a privilege is needed (PROCESS, REPLICATION CLIENT...), 1146 = the table does not exist
		if (ex.getErrorCode() == 1142 || ex.getErrorCode() == 1227)
			msg += "The login is missing a privilege. Fix: see DbmsSetup/mysql.txt (PROCESS, REPLICATION CLIENT, SELECT ON performance_schema.*)\n";
		if (ex.getErrorCode() == 1146)
			msg += "This MySQL version (or MariaDB) does not have it.\n";

		return msg;
	}

	/** SQL for some variables, in the order they are given: VARIABLE_NAME, VARIABLE_VALUE */
	private static String variablesSql(String... names)
	{
		String list = "";
		for (String name : names)
			list += (list.isEmpty() ? "" : ", ") + "'" + name + "'";

		return "select VARIABLE_NAME, VARIABLE_VALUE from performance_schema.global_variables where VARIABLE_NAME in (" + list + ") order by FIELD(VARIABLE_NAME, " + list + ")";
	}

	private static boolean isMariaDb(DbxConnection conn)
	{
		return (conn.getDbmsVersionInfo() instanceof DbmsVersionInfoMySql) && ((DbmsVersionInfoMySql) conn.getDbmsVersionInfo()).isMariaDb();
	}


	/*-----------------------------------------------------------
	**-----------------------------------------------------------
	**-----------------------------------------------------------
	**----- SUB CLASSES ----- SUB CLASSES ----- SUB CLASSES -----
	**-----------------------------------------------------------
	**-----------------------------------------------------------
	**-----------------------------------------------------------
	*/

	/** What all the MySQL tabs have in common: the text is built in doOnlineRefresh(), so there is no single SQL statement */
	private abstract static class MySqlTab extends DbmsConfigTextAbstract
	{
		@Override public    String getConfigType()                        { return getName(); }
		@Override protected String getSqlCurrentConfig(DbmsVersionInfo v) { return null; } // not used: doOnlineRefresh() is overridden
	}

	//---------------------------------------------------------------------------------------------
	public static class Server extends MySqlTab
	{
		@Override public String getTabLabel() { return "Server"; }
		@Override public String getName()     { return ConfigType.MyServer.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			return ""
				+ textTable(conn, "Version and host",
					variablesSql("version", "version_comment", "version_compile_os", "version_compile_machine", "hostname", "port", "server_id", "server_uuid", "read_only", "super_read_only"))

				+ textTable(conn, "Started\n--## The times are the server's (SERVER_TIME is in the time zone below)",
					"select DATE_FORMAT(NOW() - INTERVAL VARIABLE_VALUE SECOND, '%Y-%m-%d %H:%i:%s') as STARTED, ROUND(VARIABLE_VALUE / 86400, 1) as UPTIME_DAYS, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s') as SERVER_TIME, DATE_FORMAT(UTC_TIMESTAMP(), '%Y-%m-%d %H:%i:%s') as UTC_NOW from performance_schema.global_status where VARIABLE_NAME = 'Uptime'")

				+ textTable(conn, "Memory\n--## innodb_buffer_pool_size is the big one. The buffers per connection (sort, join, read) are only used when needed, by each connection that needs them.",
					"select VARIABLE_NAME, VARIABLE_VALUE, IF(VARIABLE_VALUE regexp '^[0-9]+$' and VARIABLE_NAME not in ('max_connections', 'thread_cache_size', 'table_open_cache', 'table_definition_cache', 'innodb_buffer_pool_instances'), ROUND(VARIABLE_VALUE / 1048576, 1), NULL) as MB "
					+ "from performance_schema.global_variables "
					+ "where VARIABLE_NAME in ('innodb_buffer_pool_size', 'innodb_buffer_pool_instances', 'innodb_dedicated_server', 'innodb_log_buffer_size', 'innodb_redo_log_capacity', 'key_buffer_size', 'tmp_table_size', 'max_heap_table_size', 'temptable_max_ram', 'temptable_max_mmap', "
					+ "'sort_buffer_size', 'join_buffer_size', 'read_buffer_size', 'read_rnd_buffer_size', 'binlog_cache_size', 'max_connections', 'thread_cache_size', 'thread_stack', 'table_open_cache', 'table_definition_cache') "
					+ "order by VARIABLE_NAME")

				+ textTable(conn, "Durability: can a crash lose committed transactions?\n--## The safe values: innodb_flush_log_at_trx_commit = 1, sync_binlog = 1, innodb_doublewrite = ON",
					variablesSql("innodb_flush_log_at_trx_commit", "sync_binlog", "innodb_doublewrite", "innodb_flush_method", "log_bin", "binlog_format", "gtid_mode"))

				+ textTable(conn, "Character set and time zone",
					variablesSql("character_set_server", "collation_server", "character_set_database", "system_time_zone", "time_zone", "default_storage_engine", "transaction_isolation", "lower_case_table_names"))

				+ textTable(conn, "Files and directories",
					variablesSql("datadir", "basedir", "tmpdir", "log_error", "socket", "pid_file", "plugin_dir", "secure_file_priv", "log_bin_basename", "relay_log_basename"))
				;
		}
	}

	//---------------------------------------------------------------------------------------------
	public static class SqlMode extends MySqlTab
	{
		@Override public String getTabLabel() { return "SQL Mode"; }
		@Override public String getName()     { return ConfigType.SqlMode.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			// The modes as rows (JSON_TABLE: 8.0 and above), with what each of them does
			return ""
				+ textTable(conn, "The SQL mode of the server (@@GLOBAL.sql_mode)\n--## A connection can change it for itself (SET SESSION sql_mode), so an application can run with another one.",
					"select @@GLOBAL.sql_mode as SQL_MODE")

				+ textTable(conn, "What each mode does\n--## MySQL 8.0 default: ONLY_FULL_GROUP_BY, STRICT_TRANS_TABLES, NO_ZERO_IN_DATE, NO_ZERO_DATE, ERROR_FOR_DIVISION_BY_ZERO, NO_ENGINE_SUBSTITUTION",
					"select m.MODE, "
					+ "IF(m.MODE in ('ONLY_FULL_GROUP_BY', 'STRICT_TRANS_TABLES', 'NO_ZERO_IN_DATE', 'NO_ZERO_DATE', 'ERROR_FOR_DIVISION_BY_ZERO', 'NO_ENGINE_SUBSTITUTION'), 'YES', 'NO') as IN_DEFAULT, "
					+ "CASE m.MODE "
					+ "  WHEN 'STRICT_TRANS_TABLES'        THEN 'A too long or wrong value gives an ERROR (not a warning and a changed value), for transactional tables' "
					+ "  WHEN 'STRICT_ALL_TABLES'          THEN 'A too long or wrong value gives an ERROR, for all tables' "
					+ "  WHEN 'ONLY_FULL_GROUP_BY'         THEN 'A select list column must be in the GROUP BY (or depend on it): no random value from the group' "
					+ "  WHEN 'NO_ZERO_IN_DATE'            THEN 'A date with a zero month or day (2026-00-10) is not allowed' "
					+ "  WHEN 'NO_ZERO_DATE'               THEN 'The date 0000-00-00 is not allowed' "
					+ "  WHEN 'ERROR_FOR_DIVISION_BY_ZERO' THEN 'Division by zero in INSERT/UPDATE is an error (not NULL and a warning)' "
					+ "  WHEN 'NO_ENGINE_SUBSTITUTION'     THEN 'CREATE TABLE with an engine that is not there gives an error (not silently InnoDB)' "
					+ "  WHEN 'ANSI_QUOTES'                THEN 'A \"text\" in double quotes is a NAME (an identifier), not a string' "
					+ "  WHEN 'PIPES_AS_CONCAT'            THEN '|| is string concatenation, not OR' "
					+ "  WHEN 'NO_BACKSLASH_ESCAPES'       THEN 'A backslash is a normal character in strings' "
					+ "  WHEN 'NO_AUTO_VALUE_ON_ZERO'      THEN 'Inserting 0 in an AUTO_INCREMENT column stores 0 (not the next number)' "
					+ "  WHEN 'ALLOW_INVALID_DATES'        THEN 'Dates are only checked loosely (2026-02-31 is allowed)' "
					+ "  WHEN 'PAD_CHAR_TO_FULL_LENGTH'    THEN 'CHAR values are not trimmed when read (deprecated)' "
					+ "  WHEN 'TIME_TRUNCATE_FRACTIONAL'   THEN 'Too many decimals in a time are cut, not rounded' "
					+ "  WHEN 'TRADITIONAL'                THEN 'Strict, as a traditional SQL database' "
					+ "  ELSE '' END as WHAT_IT_DOES "
					+ "from JSON_TABLE(CONCAT('[\"', REPLACE(@@GLOBAL.sql_mode, ',', '\",\"'), '\"]'), '$[*]' COLUMNS (MODE varchar(64) PATH '$')) m "
					+ "where m.MODE <> ''")

				+ textTable(conn, "Default modes that are NOT set",
					"select d.MODE from JSON_TABLE('[\"ONLY_FULL_GROUP_BY\",\"STRICT_TRANS_TABLES\",\"NO_ZERO_IN_DATE\",\"NO_ZERO_DATE\",\"ERROR_FOR_DIVISION_BY_ZERO\",\"NO_ENGINE_SUBSTITUTION\"]', '$[*]' COLUMNS (MODE varchar(64) PATH '$')) d "
					+ "where FIND_IN_SET(d.MODE, @@GLOBAL.sql_mode) = 0")
				;
		}
	}

	//---------------------------------------------------------------------------------------------
	public static class ChangedVariables extends MySqlTab
	{
		@Override public String getTabLabel() { return "Changed Variables"; }
		@Override public String getName()     { return ConfigType.MyChangedVariables.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			return ""
				+ textTable(conn, "Saved with SET PERSIST (performance_schema.persisted_variables, the file mysqld-auto.cnf in the data directory)"
				                       + "\n--## They are used at every restart. If PERSISTED_VALUE and RUNNING_VALUE differ (SET PERSIST_ONLY): the persisted value is used at the next restart."
				                       + "\n--## Remove one with: RESET PERSIST <name>",
					"select p.VARIABLE_NAME, p.VARIABLE_VALUE as PERSISTED_VALUE, g.VARIABLE_VALUE as RUNNING_VALUE, i.VARIABLE_SOURCE as SOURCE, DATE_FORMAT(i.SET_TIME, '%Y-%m-%d %H:%i:%s') as SET_TIME, i.SET_USER, i.SET_HOST "
					+ "from performance_schema.persisted_variables p "
					+ "left join performance_schema.global_variables g on g.VARIABLE_NAME = p.VARIABLE_NAME "
					+ "left join performance_schema.variables_info   i on i.VARIABLE_NAME = p.VARIABLE_NAME "
					+ "order by p.VARIABLE_NAME")

				+ textTable(conn, "Set in a configuration file, or on the command line (performance_schema.variables_info)",
					"select i.VARIABLE_NAME, g.VARIABLE_VALUE, i.VARIABLE_SOURCE as SOURCE, i.VARIABLE_PATH as FILE "
					+ "from performance_schema.variables_info i "
					+ "join performance_schema.global_variables g on g.VARIABLE_NAME = i.VARIABLE_NAME "
					+ "where i.VARIABLE_SOURCE not in ('COMPILED', 'DYNAMIC', 'PERSISTED') "
					+ "order by i.VARIABLE_SOURCE, i.VARIABLE_NAME")

				+ textTable(conn, "Changed after the server started (SET GLOBAL, SET PERSIST, or SET SESSION), the last change first"
				                       + "\n--## MySQL remembers the last SET of a variable, also a SET SESSION of one connection: so drivers that set autocommit, character_set_results... are here too."
				                       + "\n--## PERSISTED = NO: a SET GLOBAL is lost at the next restart (unless the same value is in my.cnf).",
					"select i.VARIABLE_NAME, g.VARIABLE_VALUE as GLOBAL_VALUE, IF(p.VARIABLE_NAME is null, 'NO', 'YES') as PERSISTED, DATE_FORMAT(i.SET_TIME, '%Y-%m-%d %H:%i:%s') as SET_TIME, i.SET_USER, i.SET_HOST "
					+ "from performance_schema.variables_info i "
					+ "join performance_schema.global_variables g on g.VARIABLE_NAME = i.VARIABLE_NAME "
					+ "left join performance_schema.persisted_variables p on p.VARIABLE_NAME = i.VARIABLE_NAME "
					+ "where i.VARIABLE_SOURCE = 'DYNAMIC' "
					+ "order by i.SET_TIME desc")
				;
		}
	}

	//---------------------------------------------------------------------------------------------
	public static class HelpDb extends MySqlTab
	{
		@Override public String getTabLabel() { return "Schemas"; }
		@Override public String getName()     { return ConfigType.HelpDb.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			// The login can't see the user's schemas in SHOW DATABASES or information_schema.SCHEMATA (no privileges on them), but it sees the InnoDB tables (PROCESS)
			return ""
				+ textTable(conn, "Schemas, from the InnoDB tables (INNODB_TABLES, INNODB_TABLESPACES: the PROCESS privilege, no access to the tables)"
				                       + "\n--## TABLES counts every partition. Only InnoDB tables: a schema with only other tables (MyISAM...) is not here."
				                       + "\n--## FILE_MB is for the tables in their own file (innodb_file_per_table). TABLES_NOT_IN_OWN_FILE: in the system or a general tablespace (not in FILE_MB).",
					"select SUBSTRING_INDEX(t.NAME, '/', 1) as SCHEMA_NAME, count(*) as TABLES, ROUND(SUM(ts.FILE_SIZE) / 1048576, 1) as FILE_MB, SUM(ts.SPACE is null) as TABLES_NOT_IN_OWN_FILE "
					+ "from information_schema.INNODB_TABLES t "
					+ "left join information_schema.INNODB_TABLESPACES ts on ts.SPACE = t.SPACE and ts.SPACE_TYPE = 'Single' "
					+ "where t.NAME like '%/%' "
					+ "group by SUBSTRING_INDEX(t.NAME, '/', 1) "
					+ "order by FILE_MB desc, SCHEMA_NAME")

				+ textTable(conn, "The InnoDB tablespaces that are not one table (system, undo, temporary, general)",
					"select NAME, SPACE_TYPE, ROUND(FILE_SIZE / 1048576, 1) as FILE_MB, ROUND(ALLOCATED_SIZE / 1048576, 1) as ALLOCATED_MB, ROW_FORMAT, STATE from information_schema.INNODB_TABLESPACES where SPACE_TYPE <> 'Single' order by FILE_SIZE desc")
				;
		}
	}

	//---------------------------------------------------------------------------------------------
	public static class Replicas extends MySqlTab
	{
		@Override public String getTabLabel() { return "Replication"; }
		@Override public String getName()     { return ConfigType.Replicas.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			return ""
				+ textTable(conn, "This server as a replica: one row per channel (performance_schema.replication_*)\n--## No rows: this server is not a replica. The tab 'Replication' (Performance Counters) has the lag.",
					"select c.CHANNEL_NAME, c.HOST, c.PORT, c.USER, c.AUTO_POSITION, c.SSL_ALLOWED, s.SERVICE_STATE as RECEIVER_STATE, a.SERVICE_STATE as APPLIER_STATE, ac.DESIRED_DELAY, "
					+ "IF(s.LAST_ERROR_NUMBER <> 0, CONCAT(s.LAST_ERROR_NUMBER, ': ', s.LAST_ERROR_MESSAGE), '') as RECEIVER_LAST_ERROR "
					+ "from performance_schema.replication_connection_configuration c "
					+ "left join performance_schema.replication_connection_status s  on s.CHANNEL_NAME  = c.CHANNEL_NAME "
					+ "left join performance_schema.replication_applier_status    a  on a.CHANNEL_NAME  = c.CHANNEL_NAME "
					+ "left join performance_schema.replication_applier_configuration ac on ac.CHANNEL_NAME = c.CHANNEL_NAME "
					+ "order by c.CHANNEL_NAME")

				+ textTable(conn, "Replicas connected to this server (a 'Binlog Dump' thread for each)",
					"select PROCESSLIST_ID, PROCESSLIST_USER as USER, PROCESSLIST_HOST as HOST, PROCESSLIST_COMMAND as COMMAND, PROCESSLIST_TIME as SECONDS, PROCESSLIST_STATE as STATE "
					+ "from performance_schema.threads where PROCESSLIST_COMMAND like 'Binlog Dump%' order by PROCESSLIST_HOST")

				+ textTable(conn, "Replication settings",
					"select VARIABLE_NAME, VARIABLE_VALUE from performance_schema.global_variables "
					+ "where VARIABLE_NAME in ('server_id', 'gtid_mode', 'enforce_gtid_consistency', 'log_bin', 'log_replica_updates', 'log_slave_updates', 'binlog_format', 'read_only', 'super_read_only', "
					+ "'replica_parallel_workers', 'slave_parallel_workers', 'replica_preserve_commit_order', 'slave_preserve_commit_order', 'relay_log_recovery', 'sync_binlog', 'binlog_expire_logs_seconds') "
					+ "   or VARIABLE_NAME like 'rpl_semi_sync%' "
					+ "order by VARIABLE_NAME")

				+ textTable(conn, "GTIDs: what this server has executed, and what is no longer in its binary logs",
					"select @@GLOBAL.gtid_executed as GTID_EXECUTED, @@GLOBAL.gtid_purged as GTID_PURGED")
				;
		}
	}

	//---------------------------------------------------------------------------------------------
	public static class Binlogs extends MySqlTab
	{
		@Override public String getTabLabel() { return "Binary Logs"; }
		@Override public String getName()     { return ConfigType.MyBinlogs.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			// The total size goes in the title (SHOW can't be summed in SQL)
			String total = "";
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery("SHOW BINARY LOGS"))
			{
				int  files = 0;
				long bytes = 0;
				while (rs.next())
				{
					files++;
					bytes += rs.getLong(2);
				}
				total = ": " + files + " files, " + (bytes / 1048576) + " MB";
			}
			catch (SQLException ex)
			{
				// The text table below shows the error
			}

			// 8.2 renamed SHOW MASTER STATUS
			String statusCmd = (conn.getDbmsVersionInfo().getLongVersion() >= Ver.ver(8,2) && ! isMariaDb(conn)) ? "SHOW BINARY LOG STATUS" : "SHOW MASTER STATUS";

			return ""
				+ textTable(conn, "Binary log files (SHOW BINARY LOGS)" + total + "\n--## They are removed by the server when they are older than binlog_expire_logs_seconds.",
					"SHOW BINARY LOGS")

				+ textTable(conn, "Where the server writes now (" + statusCmd + ")",
					statusCmd)

				+ textTable(conn, "Binary log settings",
					variablesSql("log_bin", "log_bin_basename", "binlog_format", "binlog_row_image", "sync_binlog", "binlog_expire_logs_seconds", "binlog_expire_logs_auto_purge", "expire_logs_days",
					             "max_binlog_size", "binlog_cache_size", "binlog_transaction_compression", "binlog_encryption", "gtid_mode"))
				;
		}
	}

	//---------------------------------------------------------------------------------------------
	public static class Plugins extends MySqlTab
	{
		@Override public String getTabLabel() { return "Engines and Plugins"; }
		@Override public String getName()     { return ConfigType.MyPlugins.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			return ""
				+ textTable(conn, "Storage engines (information_schema.ENGINES)\n--## SUPPORT = DEFAULT: what a new table gets when it does not say ENGINE=",
					"select ENGINE, SUPPORT, TRANSACTIONS, XA, SAVEPOINTS, COMMENT from information_schema.ENGINES order by ENGINE")

				+ textTable(conn, "Plugins (information_schema.PLUGINS), the ones loaded from a library first"
				                       + "\n--## Components (INSTALL COMPONENT) are not here: they are in mysql.component, which the login can't read.",
					"select PLUGIN_NAME, PLUGIN_VERSION, PLUGIN_STATUS, PLUGIN_TYPE, PLUGIN_LIBRARY, LOAD_OPTION from information_schema.PLUGINS order by PLUGIN_LIBRARY is null, PLUGIN_TYPE, PLUGIN_NAME")
				;
		}
	}

	//---------------------------------------------------------------------------------------------
	public static class PfsSetup extends MySqlTab
	{
		@Override public String getTabLabel() { return "Performance Schema Setup"; }
		@Override public String getName()     { return ConfigType.MyPfsSetup.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			return ""
				+ textTable(conn, "Consumers (setup_consumers): what performance_schema keeps\n--## Turn one on: UPDATE performance_schema.setup_consumers SET ENABLED = 'YES' WHERE NAME = '...'; (and in my.cnf, so it survives a restart)",
					"select NAME, ENABLED, "
					+ "CASE NAME "
					+ "  WHEN 'global_instrumentation'      THEN 'Needed for everything' "
					+ "  WHEN 'thread_instrumentation'      THEN 'Needed for everything per connection' "
					+ "  WHEN 'statements_digest'           THEN 'Tab: Statements (Top SQL)' "
					+ "  WHEN 'events_statements_current'   THEN 'Tab: Active Statements (the running statement)' "
					+ "  WHEN 'events_transactions_current' THEN 'Tab: Active Statements (the open transaction)' "
					+ "  WHEN 'events_statements_cpu'       THEN 'Tab: Statements, column CpuTimeMs (OFF by default)' "
					+ "  ELSE '' END as USED_BY_MYSQLTUNE "
					+ "from performance_schema.setup_consumers order by NAME")

				+ textTable(conn, "Instruments (setup_instruments) per kind: how many are ON (ENABLED) and timed (TIMED)\n--## wait/synch and wait/io/socket are OFF by default: they cost CPU. The tab 'Wait Events' only counts what is ON.",
					"select CASE WHEN NAME like 'wait/%' THEN SUBSTRING_INDEX(NAME, '/', 3) ELSE SUBSTRING_INDEX(NAME, '/', 1) END as KIND, count(*) as INSTRUMENTS, SUM(ENABLED = 'YES') as ENABLED, SUM(TIMED = 'YES') as TIMED "
					+ "from performance_schema.setup_instruments "
					+ "group by CASE WHEN NAME like 'wait/%' THEN SUBSTRING_INDEX(NAME, '/', 3) ELSE SUBSTRING_INDEX(NAME, '/', 1) END "
					+ "order by KIND")

				+ textTable(conn, "Lost data (Performance_schema_*_lost): should all be 0\n--## Not 0: a performance_schema table was full, so what MySqlTune shows for that part is not complete. Increase the matching size below.",
					"select VARIABLE_NAME, VARIABLE_VALUE from performance_schema.global_status where VARIABLE_NAME like 'Performance_schema_%_lost' and VARIABLE_VALUE <> '0' order by VARIABLE_NAME")

				+ textTable(conn, "Sizes (performance_schema_* variables). -1 = MySQL sizes it by itself",
					"select VARIABLE_NAME, VARIABLE_VALUE from performance_schema.global_variables where VARIABLE_NAME like 'performance_schema%' order by VARIABLE_NAME")

				+ textTable(conn, "Memory used by performance_schema itself",
					"select ROUND(SUM(CURRENT_NUMBER_OF_BYTES_USED) / 1048576, 1) as CURRENT_MB, ROUND(SUM(HIGH_NUMBER_OF_BYTES_USED) / 1048576, 1) as HIGH_MB from performance_schema.memory_summary_global_by_event_name where EVENT_NAME like 'memory/performance_schema/%'")
				;
		}
	}

	//---------------------------------------------------------------------------------------------
	public static class InnodbStatus extends MySqlTab
	{
		@Override public String getTabLabel() { return "InnoDB Status"; }
		@Override public String getName()     { return ConfigType.MyInnodbStatus.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			// One big text (the column 'Status'): shown as it is, not as a table
			String out = header("InnoDB status (SHOW ENGINE INNODB STATUS), when the configuration was read"
			                       + "\n--## LATEST DETECTED DEADLOCK: the last deadlock since the server started (all of them are only in the error log with innodb_print_all_deadlocks = ON)."
			                       + "\n--## TRANSACTIONS: 'History list length' is the undo that is not purged yet (a long open transaction keeps it growing).");
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery("SHOW ENGINE INNODB STATUS"))
			{
				while (rs.next())
					out += rs.getString(3) + "\n";
			}
			catch (SQLException ex)
			{
				out += couldNotGet(ex);
			}
			return out;
		}
	}
}
