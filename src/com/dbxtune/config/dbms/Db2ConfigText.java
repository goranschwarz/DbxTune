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

import java.sql.SQLException;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.hostmon.HostMonitorConnection;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;

/**
 * The text tabs in the "DBMS Configuration" dialog for Db2.
 * <p>
 * Every tab is built from several SQL statements, each with a title (as MySqlConfigText / OracleConfigText): see <code>doOnlineRefresh()</code> in each tab, and <code>textTable()</code> below.<br>
 * All of it is readable by the monitoring login of DbmsSetup/db2.txt (the role DBXTUNE_MON, or SQLADM, and what PUBLIC can read): it has no access to the user data.
 */
public abstract class Db2ConfigText
{
	/** What sub types exists */
	public enum ConfigType
	{
		 Db2Server
		,Db2Registry
		,Db2BufferPools
		,Db2TableSpaces
		,Db2Hadr
		,Db2BackupHistory
		,Db2EventMonitors
		,Db2Wlm
		,Db2AutoMaint
		};

	public static void createAndRegisterAllInstances()
	{
		DbmsConfigTextManager.addInstance(new Db2ConfigText.Server());
		DbmsConfigTextManager.addInstance(new Db2ConfigText.Registry());
		DbmsConfigTextManager.addInstance(new Db2ConfigText.BufferPools());
		DbmsConfigTextManager.addInstance(new Db2ConfigText.TableSpaces());
		DbmsConfigTextManager.addInstance(new Db2ConfigText.Hadr());
		DbmsConfigTextManager.addInstance(new Db2ConfigText.BackupHistory());
		DbmsConfigTextManager.addInstance(new Db2ConfigText.EventMonitors());
		DbmsConfigTextManager.addInstance(new Db2ConfigText.Wlm());
		DbmsConfigTextManager.addInstance(new Db2ConfigText.AutoMaint());
	}

	/**
	 * One part of a tab: a title, then the result of the SQL as a text table. (the same as <code>MySqlConfigText.textTable()</code>, with Db2's errors)
	 * <p>
	 * If the SQL fails (a function that this Db2 version does not have, a missing privilege...) the error text is put there instead, so the rest of the tab is still shown.
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

		// -551 = no privilege, -204 = the object does not exist, -440 = no such function (in this version)
		if (ex.getErrorCode() == -551)
			msg += "The login is missing a privilege. Fix: see DbmsSetup/db2.txt (the role DBXTUNE_MON, or SQLADM)\n";
		if (ex.getErrorCode() == -204 || ex.getErrorCode() == -440)
			msg += "This Db2 version does not have it.\n";

		return msg;
	}

	/** SQL for some database parameters (DB CFG), in the order they are given: NAME, VALUE, VALUE_FLAGS, DEFERRED_VALUE */
	private static String dbCfgSql(String... names)
	{
		String list  = "";
		String order = "";
		for (int i = 0; i < names.length; i++)
		{
			list  += (list.isEmpty() ? "" : ", ") + "'" + names[i] + "'";
			order += " WHEN '" + names[i] + "' THEN " + i;
		}

		return "select NAME, VALUE, VALUE_FLAGS, DEFERRED_VALUE from SYSIBMADM.DBCFG where MEMBER = CURRENT MEMBER and NAME in (" + list + ") order by CASE NAME" + order + " END";
	}


	/*-----------------------------------------------------------
	**-----------------------------------------------------------
	**-----------------------------------------------------------
	**----- SUB CLASSES ----- SUB CLASSES ----- SUB CLASSES -----
	**-----------------------------------------------------------
	**-----------------------------------------------------------
	**-----------------------------------------------------------
	*/

	/** What all the Db2 tabs have in common: the text is built in doOnlineRefresh(), so there is no single SQL statement */
	private abstract static class Db2Tab extends DbmsConfigTextAbstract
	{
		@Override public    String getConfigType()                        { return getName(); }
		@Override protected String getSqlCurrentConfig(DbmsVersionInfo v) { return null; } // not used: doOnlineRefresh() is overridden
	}

	//---------------------------------------------------------------------------------------------
	public static class Server extends Db2Tab
	{
		@Override public String getTabLabel() { return "Server"; }
		@Override public String getName()     { return ConfigType.Db2Server.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			return ""
				+ textTable(conn, "Version and fix pack (ENV_GET_INST_INFO)",
					"select INST_NAME, SERVICE_LEVEL, FIXPACK_NUM, BLD_LEVEL, RELEASE_NUM, INST_PTR_SIZE, NUM_MEMBERS, NUM_DBPARTITIONS, IS_INST_PARTITIONABLE "
					+ "from table(SYSPROC.ENV_GET_INST_INFO()) t")

				+ textTable(conn, "Edition and license (ENV_PROD_INFO)\n--## LICENSE_TYPE: COMMUNITY has limits (4 cores, 16 GB memory), check that the installed license is the one you pay for.",
					"select INSTALLED_PROD, INSTALLED_PROD_FULLNAME, LICENSE_INSTALLED, PROD_RELEASE, LICENSE_TYPE from SYSIBMADM.ENV_PROD_INFO")

				+ textTable(conn, "The host (ENV_GET_SYS_INFO)",
					"select OS_NAME, OS_VERSION, OS_RELEASE, HOST_NAME, TOTAL_CPUS, CONFIGURED_CPUS, TOTAL_MEMORY from table(SYSPROC.ENV_GET_SYS_INFO()) t")

				+ textTable(conn, "The database (MON_GET_DATABASE)\n--## DB_CONN_TIME: when the database was activated (the counters are from then). DB_ACTIVATION_STATE EXPLICIT: it stays active without connections.",
					"select CURRENT SERVER as DB_NAME, DB_STATUS, DB_ACTIVATION_STATE, DB_CONN_TIME, LAST_BACKUP, CONNECTIONS_TOP, APPLS_CUR_CONS, MEMBER from table(MON_GET_DATABASE(-2)) t order by MEMBER")

				+ textTable(conn, "Code page, territory and collation (fixed when the database was created)",
					dbCfgSql("codeset", "codepage", "territory", "country", "db_collname", "pagesize", "database_level", "release", "string_units", "dft_table_org", "varchar2_compat", "date_compat", "number_compat"))

				+ textTable(conn, "Database states\n--## Everything should be NO (backup pending, roll forward pending, restore pending...)",
					dbCfgSql("backup_pending", "rollfwd_pending", "restore_pending", "upgrade_pending", "database_consistent", "restrict_access", "suspend_io", "encrypted_database", "hadr_db_role"))

				+ textTable(conn, "Log files\n--## The total log is (LOGPRIMARY + LOGSECOND) * LOGFILSIZ * 4 KB. NUM_LOG_SPAN / MAX_LOG limit one transaction (0 = no limit).",
					dbCfgSql("logpath", "newlogpath", "mirrorlogpath", "overflowlogpath", "logfilsiz", "logprimary", "logsecond", "logbufsz", "num_log_span", "max_log", "blk_log_dsk_ful",
					         "logarchmeth1", "logarchmeth2", "logarchcompr1", "failarchpath", "numarchretry", "archretrydelay", "loghead", "trackmod"))

				+ textTable(conn, "Memory (instance and database)",
					"select 'DBM' as SCOPE, NAME, VALUE, VALUE_FLAGS, DEFERRED_VALUE from SYSIBMADM.DBMCFG where NAME in ('instance_memory', 'sheapthres', 'mon_heap_sz', 'java_heap_sz') "
					+ "union all "
					+ "select 'DB', NAME, VALUE, VALUE_FLAGS, DEFERRED_VALUE from SYSIBMADM.DBCFG where MEMBER = CURRENT MEMBER and NAME in ('self_tuning_mem', 'database_memory', 'db_mem_thresh', 'locklist', 'maxlocks', 'pckcachesz', 'catalogcache_sz', 'sortheap', 'sheapthres_shr', 'dbheap', 'stmtheap', 'util_heap_sz', 'appl_memory', 'applheapsz') "
					+ "order by 1 desc, 2")
				;
		}
	}

	//---------------------------------------------------------------------------------------------
	public static class Registry extends Db2Tab
	{
		@Override public String getTabLabel() { return "Registry"; }
		@Override public String getName()     { return ConfigType.Db2Registry.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			return ""
				+ textTable(conn, "Registry variables that are set (db2set -all, SYSIBMADM.REG_VARIABLES)\n--## LEVEL: I = instance, G = global, E = environment, N = database partition. IS_AGGREGATE: set by an aggregate variable (for example DB2_WORKLOAD).",
					"select REG_VAR_NAME, REG_VAR_VALUE, LEVEL, IS_AGGREGATE, AGGREGATE_NAME, DBPARTITIONNUM from SYSIBMADM.REG_VARIABLES order by REG_VAR_NAME, DBPARTITIONNUM")
				;
		}
	}

	//---------------------------------------------------------------------------------------------
	public static class BufferPools extends Db2Tab
	{
		@Override public String getTabLabel() { return "Buffer Pools"; }
		@Override public String getName()     { return ConfigType.Db2BufferPools.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			return ""
				+ textTable(conn, "Buffer pools (SYSCAT.BUFFERPOOLS)\n--## NPAGES -2 = AUTOMATIC (the self tuning memory manager sizes it). A fixed size is not changed by STMM.",
					"select RTRIM(BPNAME) as BPNAME, BUFFERPOOLID, NPAGES, PAGESIZE, CASE WHEN NPAGES > 0 THEN NPAGES * PAGESIZE / 1048576 END as FIXED_MB, NUMBLOCKPAGES, BLOCKSIZE "
					+ "from SYSCAT.BUFFERPOOLS order by BUFFERPOOLID")

				+ textTable(conn, "Buffer pools now (MON_GET_BUFFERPOOL), since the database was activated\n--## HIT_PCT: logical reads that did not need a physical read (data + index).",
					"select RTRIM(BP_NAME) as BP_NAME, MEMBER, BP_CUR_BUFFSZ as PAGES_NOW, AUTOMATIC, "
					+ "CAST(100.0 - (POOL_DATA_P_READS + POOL_INDEX_P_READS) * 100.0 / NULLIF(POOL_DATA_L_READS + POOL_INDEX_L_READS, 0) AS DECIMAL(5,1)) as HIT_PCT, "
					+ "POOL_DATA_L_READS + POOL_INDEX_L_READS as LOGICAL_READS, POOL_DATA_P_READS + POOL_INDEX_P_READS as PHYSICAL_READS, POOL_DATA_WRITES + POOL_INDEX_WRITES as WRITES "
					+ "from table(MON_GET_BUFFERPOOL('', -2)) t order by BP_NAME, MEMBER")

				+ textTable(conn, "Table spaces per buffer pool",
					"select RTRIM(b.BPNAME) as BPNAME, RTRIM(t.TBSPACE) as TBSPACE, t.PAGESIZE, t.TBSPACETYPE, t.DATATYPE from SYSCAT.TABLESPACES t join SYSCAT.BUFFERPOOLS b on b.BUFFERPOOLID = t.BUFFERPOOLID order by 1, 2")
				;
		}
	}

	//---------------------------------------------------------------------------------------------
	public static class TableSpaces extends Db2Tab
	{
		@Override public String getTabLabel() { return "Table Spaces"; }
		@Override public String getName()     { return ConfigType.Db2TableSpaces.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			return ""
				+ textTable(conn, "Table spaces (MON_GET_TABLESPACE)\n--## AUTO_STORAGE YES: it grows into the storage paths of its storage group. AUTO_RESIZE YES: it grows until MAX_SIZE.",
					"select RTRIM(TBSP_NAME) as TBSP_NAME, TBSP_TYPE, TBSP_CONTENT_TYPE, TBSP_STATE, TBSP_PAGE_SIZE, TBSP_USING_AUTO_STORAGE as AUTO_STORAGE, TBSP_AUTO_RESIZE_ENABLED as AUTO_RESIZE, "
					+ "CAST(TBSP_USED_PAGES * TBSP_PAGE_SIZE / 1048576.0 AS DECIMAL(15,1)) as USED_MB, CAST(TBSP_TOTAL_PAGES * TBSP_PAGE_SIZE / 1048576.0 AS DECIMAL(15,1)) as TOTAL_MB, "
					+ "CAST(TBSP_MAX_SIZE / 1048576.0 AS DECIMAL(15,1)) as MAX_MB, TBSP_EXTENT_SIZE, TBSP_PREFETCH_SIZE, RTRIM(STORAGE_GROUP_NAME) as STOGROUP, MEMBER "
					+ "from table(MON_GET_TABLESPACE('', -2)) t order by TBSP_ID, MEMBER")

				+ textTable(conn, "Storage groups (SYSCAT.STOGROUPS)",
					"select RTRIM(SGNAME) as SGNAME, SGID, DEFAULTSG, OVERHEAD, DEVICEREADRATE, CREATE_TIME from SYSCAT.STOGROUPS order by SGID")

				+ textTable(conn, "Storage paths and their file systems (ADMIN_GET_STORAGE_PATHS)",
					"select STORAGE_GROUP_NAME, DB_STORAGE_PATH, DB_STORAGE_PATH_STATE, CAST(FS_TOTAL_SIZE / 1048576.0 AS DECIMAL(15,1)) as FS_TOTAL_MB, CAST(FS_USED_SIZE / 1048576.0 AS DECIMAL(15,1)) as FS_USED_MB, "
					+ "CAST(STO_PATH_FREE_SIZE / 1048576.0 AS DECIMAL(15,1)) as PATH_FREE_MB, DBPARTITIONNUM from table(ADMIN_GET_STORAGE_PATHS('', -2)) t order by STORAGE_GROUP_NAME, DB_STORAGE_PATH")

				+ textTable(conn, "Containers (MON_GET_CONTAINER)",
					"select RTRIM(TBSP_NAME) as TBSP_NAME, CONTAINER_NAME, CONTAINER_TYPE, CAST(TOTAL_PAGES AS BIGINT) as TOTAL_PAGES, USABLE_PAGES, ACCESSIBLE, MEMBER from table(MON_GET_CONTAINER('', -2)) t order by TBSP_ID, CONTAINER_ID, MEMBER")
				;
		}
	}

	//---------------------------------------------------------------------------------------------
	public static class Hadr extends Db2Tab
	{
		@Override public String getTabLabel() { return "HADR"; }
		@Override public String getName()     { return ConfigType.Db2Hadr.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			return ""
				+ textTable(conn, "HADR configuration (DB CFG)\n--## HADR_DB_ROLE STANDARD = no HADR. Under HADR: LOGINDEXBUILD should be ON and BLOCKNONLOGGED YES.",
					dbCfgSql("hadr_db_role", "hadr_syncmode", "hadr_local_host", "hadr_local_svc", "hadr_remote_host", "hadr_remote_svc", "hadr_remote_inst", "hadr_target_list",
					         "hadr_timeout", "hadr_peer_window", "hadr_replay_delay", "hadr_spool_limit", "hadr_ssl_label", "logindexbuild", "blocknonlogged"))

				+ textTable(conn, "HADR state (MON_GET_HADR): one row per standby\n--## No rows: the database has no HADR.",
					"select HADR_ROLE, STANDBY_ID, HADR_SYNCMODE, HADR_STATE, HADR_CONNECT_STATUS, HADR_CONNECT_STATUS_TIME, PRIMARY_MEMBER_HOST, STANDBY_MEMBER_HOST, STANDBY_INSTANCE, "
					+ "HADR_LOG_GAP, STANDBY_RECV_REPLAY_GAP, PEER_WINDOW, READS_ON_STANDBY_ENABLED, HADR_LAST_TAKEOVER_TIME "
					+ "from table(MON_GET_HADR(-2)) t order by STANDBY_ID")
				;
		}
	}

	//---------------------------------------------------------------------------------------------
	public static class BackupHistory extends Db2Tab
	{
		@Override public String getTabLabel() { return "Backup History"; }
		@Override public String getName()     { return ConfigType.Db2BackupHistory.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			return ""
				+ textTable(conn, "Backups, restores and loads of the last 30 days (SYSIBMADM.DB_HISTORY), the newest first\n--## OPERATION B = backup, R = restore, L = load, F = roll forward. OPERATIONTYPE for B: F/N = offline/online full, I/O = incremental, D/E = delta. SQLCODE < 0 = it failed.",
					"select OPERATION, OPERATIONTYPE, OBJECTTYPE, START_TIME, END_TIME, SQLCODE, DEVICETYPE, LOCATION, FIRSTLOG, LASTLOG, NUM_TBSPS, ENTRY_STATUS "
					+ "from SYSIBMADM.DB_HISTORY where OPERATION in ('B', 'R', 'L', 'F') and START_TIME >= VARCHAR_FORMAT(CURRENT TIMESTAMP - 30 DAYS, 'YYYYMMDDHH24MISS') "
					+ "order by START_TIME desc fetch first 200 rows only")

				+ textTable(conn, "Log archives per day, the last 14 days (OPERATION X, OPERATIONTYPE 1/2 = LOGARCHMETH1/2)",
					"select SUBSTR(START_TIME, 1, 8) as DAY, OPERATIONTYPE, count(*) as ARCHIVED_LOGS, sum(CASE WHEN COALESCE(SQLCODE, 0) < 0 THEN 1 ELSE 0 END) as FAILED "
					+ "from SYSIBMADM.DB_HISTORY where OPERATION = 'X' and OPERATIONTYPE in ('1', '2') and START_TIME >= VARCHAR_FORMAT(CURRENT TIMESTAMP - 14 DAYS, 'YYYYMMDDHH24MISS') "
					+ "group by SUBSTR(START_TIME, 1, 8), OPERATIONTYPE order by 1 desc, 2")

				+ textTable(conn, "Backup settings",
					dbCfgSql("logarchmeth1", "trackmod", "num_db_backups", "rec_his_retentn", "auto_del_rec_obj", "auto_db_backup", "vendoropt", "util_heap_sz"))
				;
		}
	}

	//---------------------------------------------------------------------------------------------
	public static class EventMonitors extends Db2Tab
	{
		@Override public String getTabLabel() { return "Event Monitors"; }
		@Override public String getName()     { return ConfigType.Db2EventMonitors.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			return ""
				+ textTable(conn, "Event monitors (SYSCAT.EVENTMONITORS), and if they are active (EVENT_MON_STATE: 1 = active)\n--## DB2DETAILDEADLOCK is the default deadlock monitor (deprecated: a LOCKING event monitor is the newer way).",
					"select RTRIM(EVMONNAME) as EVMONNAME, TARGET_TYPE, AUTOSTART, EVENT_MON_STATE(EVMONNAME) as ACTIVE, RTRIM(OWNER) as OWNER, CAST(TARGET AS VARCHAR(200)) as TARGET from SYSCAT.EVENTMONITORS order by EVMONNAME")

				+ textTable(conn, "What the event monitors get (SYSCAT.EVENTS)",
					"select RTRIM(EVMONNAME) as EVMONNAME, TYPE, CAST(FILTER AS VARCHAR(200)) as FILTER from SYSCAT.EVENTS order by EVMONNAME, TYPE")

				+ textTable(conn, "Database parameters for the locking and unit of work event monitors",
					dbCfgSql("mon_deadlock", "mon_locktimeout", "mon_lockwait", "mon_lw_thresh", "mon_lck_msg_lvl", "mon_uow_data", "mon_uow_execlist", "mon_uow_pkglist", "mon_req_metrics", "mon_act_metrics", "mon_obj_metrics"))
				;
		}
	}

	//---------------------------------------------------------------------------------------------
	public static class Wlm extends Db2Tab
	{
		@Override public String getTabLabel() { return "Workload Management"; }
		@Override public String getName()     { return ConfigType.Db2Wlm.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			return ""
				+ textTable(conn, "Service classes (SYSCAT.SERVICECLASSES)\n--## Only the default ones (SYSDEFAULT...): workload management is not used.",
					"select RTRIM(SERVICECLASSNAME) as SERVICECLASSNAME, RTRIM(COALESCE(PARENTSERVICECLASSNAME, '')) as PARENT, ENABLED, AGENTPRIORITY, PREFETCHPRIORITY, BUFFERPOOLPRIORITY, CPUSHARES, CPULIMIT "
					+ "from SYSCAT.SERVICECLASSES order by PARENTID, SERVICECLASSID")

				+ textTable(conn, "Workloads (SYSCAT.WORKLOADS): which connections go to which service class",
					"select RTRIM(WORKLOADNAME) as WORKLOADNAME, EVALUATIONORDER, ENABLED, ALLOWACCESS, RTRIM(SERVICECLASSNAME) as SERVICECLASSNAME, RTRIM(COALESCE(PARENTSERVICECLASSNAME, '')) as PARENT "
					+ "from SYSCAT.WORKLOADS order by EVALUATIONORDER")

				+ textTable(conn, "Thresholds (SYSCAT.THRESHOLDS): limits that stop or log statements",
					"select RTRIM(THRESHOLDNAME) as THRESHOLDNAME, THRESHOLDPREDICATE, MAXVALUE, EXECUTION, ENFORCEMENT, DOMAIN, ENABLED from SYSCAT.THRESHOLDS order by THRESHOLDNAME")

				+ textTable(conn, "WLM database parameters",
					dbCfgSql("wlm_admission_ctrl", "wlm_agent_load_trgt", "wlm_cpu_limit", "wlm_cpu_shares", "wlm_cpu_share_mode", "wlm_collect_int"))
				;
		}
	}

	//---------------------------------------------------------------------------------------------
	public static class AutoMaint extends Db2Tab
	{
		@Override public String getTabLabel() { return "Automatic Maintenance"; }
		@Override public String getName()     { return ConfigType.Db2AutoMaint.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			return ""
				+ textTable(conn, "Automatic maintenance (DB CFG)\n--## AUTO_MAINT is the parent of all, AUTO_TBL_MAINT the parent of RUNSTATS and REORG. AUTO_RUNSTATS should be ON (unless a job runs RUNSTATS).",
					dbCfgSql("auto_maint", "auto_db_backup", "auto_tbl_maint", "auto_runstats", "auto_stmt_stats", "auto_sampling", "auto_stats_views", "auto_cg_stats", "auto_reorg", "auto_del_rec_obj", "auto_reval"))

				+ textTable(conn, "Tables with no statistics, or the oldest statistics (user tables, max 50)\n--## STATS_TIME empty: never collected (the optimizer guesses).",
					"select RTRIM(TABSCHEMA) as TABSCHEMA, TABNAME, CARD, STATS_TIME, LASTUSED from SYSCAT.TABLES where TYPE = 'T' and TABSCHEMA not like 'SYS%' and TABSCHEMA not in ('NULLID', 'SQLJ') "
					+ "order by STATS_TIME nulls first fetch first 50 rows only")

				+ textTable(conn, "Utilities that run now (MON_GET_UTILITY)\n--## UTILITY_INVOKER_TYPE AUTO = started by automatic maintenance.",
					"select UTILITY_TYPE, UTILITY_OPERATION_TYPE, UTILITY_START_TIME, UTILITY_INVOKER_TYPE, OBJECT_TYPE, RTRIM(OBJECT_SCHEMA) as OBJECT_SCHEMA, OBJECT_NAME from table(MON_GET_UTILITY(-2)) t order by UTILITY_START_TIME")
				;
		}
	}
}
