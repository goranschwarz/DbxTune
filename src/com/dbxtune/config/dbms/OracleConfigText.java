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
import com.dbxtune.sql.conn.info.DbmsVersionInfoOracle;
import com.dbxtune.utils.Ver;

/**
 * The text tabs in the "DBMS Configuration" dialog for Oracle.
 * <p>
 * Every tab is built from several SQL statements, each with a title: see <code>doOnlineRefresh()</code> in each tab, and <code>textTable()</code> below.<br>
 * All of it is readable with <code>SELECT_CATALOG_ROLE</code> (or <code>SELECT ANY DICTIONARY</code>), and nothing needs a licensed pack.
 */
public abstract class OracleConfigText
{
	/** What sub types exists */
	public enum ConfigType
	{
		 OraDatabaseInfo
		,OraSpfile
		,OraPatches
		,OraFiles
		,OraFeatures
		,OraBackupConfig
		,OraJobs
		,OraSecurity
		,OraContainers
		,OraDbLinks
		,OraNlsParams
		};

	public static void createAndRegisterAllInstances()
	{
		DbmsConfigTextManager.addInstance(new OracleConfigText.DatabaseInfo());
		DbmsConfigTextManager.addInstance(new OracleConfigText.Spfile());
		DbmsConfigTextManager.addInstance(new OracleConfigText.Patches());
		DbmsConfigTextManager.addInstance(new OracleConfigText.Files());
		DbmsConfigTextManager.addInstance(new OracleConfigText.Features());
		DbmsConfigTextManager.addInstance(new OracleConfigText.BackupConfig());
		DbmsConfigTextManager.addInstance(new OracleConfigText.Jobs());
		DbmsConfigTextManager.addInstance(new OracleConfigText.Security());
		DbmsConfigTextManager.addInstance(new OracleConfigText.Containers());
		DbmsConfigTextManager.addInstance(new OracleConfigText.DbLinks());
		DbmsConfigTextManager.addInstance(new OracleConfigText.NlsParams());
	}

	/**
	 * One part of a tab: a title, then the result of the SQL as a text table.
	 * <p>
	 * If the SQL fails (a view that this Oracle version does not have, a missing privilege...) the error text is put there instead, so the rest of the tab is still shown.
	 *
	 * @param conn
	 * @param vertical   true = one line per column (name: value). For a result with ONE row and many columns
	 * @param title      What this is (a second line with an explanation can be added with: "\n--## ...")
	 * @param sql
	 */
	private static String textTable(DbxConnection conn, boolean vertical, String title, String sql)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("\n");
		sb.append("--######################################################################################\n");
		sb.append("--## ").append(title).append("\n");
		sb.append("--######################################################################################\n");

		try
		{
			ResultSetTableModel rstm = ResultSetTableModel.executeQuery(conn, sql, 30, title);

			if (rstm.getRowCount() == 0)
				sb.append("(no rows)\n");
			else if (vertical)
				sb.append(rstm.toTablesVerticalString());
			else
				sb.append(rstm.toAsciiTableString());
		}
		catch (SQLException ex)
		{
			sb.append("Could not get this: ").append(ex.getMessage().trim()).append("\n");

			// 942 = table or view does not exist, 1031 = insufficient privileges
			if (ex.getErrorCode() == 942 || ex.getErrorCode() == 1031)
				sb.append("If the view exists in this Oracle version, the login is not allowed to read it. Fix: GRANT SELECT_CATALOG_ROLE TO <user>;\n");
		}

		return sb.toString();
	}

	/**
	 * For V$SYSTEM_PARAMETER in Oracle 12.1 and above: the rows for the whole CDB (CON_ID 0) and for the container we are connected to.<br>
	 * (connected to CDB$ROOT that view also has rows for every PDB, and a parameter name would come several times)
	 */
	private static String andOurContainer(DbxConnection conn)
	{
		return conn.getDbmsVersionInfo().getLongVersion() >= Ver.ver(12,1) ? " and CON_ID in (0, sys_context('userenv', 'con_id')) " : " ";
	}


	/*-----------------------------------------------------------
	**-----------------------------------------------------------
	**-----------------------------------------------------------
	**----- SUB CLASSES ----- SUB CLASSES ----- SUB CLASSES -----
	**-----------------------------------------------------------
	**-----------------------------------------------------------
	**-----------------------------------------------------------
	*/

	/** What all the Oracle tabs have in common: the text is built in doOnlineRefresh(), so there is no single SQL statement */
	private abstract static class OracleTab extends DbmsConfigTextAbstract
	{
		@Override public    String getConfigType()                        { return getName(); }
		@Override protected String getSqlCurrentConfig(DbmsVersionInfo v) { return null; } // not used: doOnlineRefresh() is overridden
	}

	//---------------------------------------------------------------------------------------------
	public static class DatabaseInfo extends OracleTab
	{
		@Override public String getTabLabel() { return "Database Info"; }
		@Override public String getName()     { return ConfigType.OraDatabaseInfo.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			// The column CDB came in 12.1
			String cdb = conn.getDbmsVersionInfo().getLongVersion() >= Ver.ver(12,1) ? ", CDB" : "";

			return ""
				+ textTable(conn, true,  "Database (V$DATABASE)",
					"select NAME, DB_UNIQUE_NAME, to_char(DBID) as DBID, CREATED, LOG_MODE, OPEN_MODE, DATABASE_ROLE" + cdb + ", PROTECTION_MODE, SWITCHOVER_STATUS, DATAGUARD_BROKER, FORCE_LOGGING, FLASHBACK_ON, SUPPLEMENTAL_LOG_DATA_MIN, PLATFORM_NAME, RESETLOGS_TIME, CONTROLFILE_TYPE from V$DATABASE")

				+ textTable(conn, true,  "Instance (V$INSTANCE)",
					"select * from V$INSTANCE")

				+ textTable(conn, false, "Version (V$VERSION)",
					"select BANNER from V$VERSION")

				+ textTable(conn, false, "Host (V$OSSTAT)",
					"select STAT_NAME, case when STAT_NAME = 'PHYSICAL_MEMORY_BYTES' then round(VALUE / 1048576) || ' MB' else to_char(VALUE) end as VALUE, COMMENTS from V$OSSTAT where STAT_NAME in ('NUM_CPUS', 'NUM_CPU_CORES', 'NUM_CPU_SOCKETS', 'PHYSICAL_MEMORY_BYTES') order by STAT_NAME")

				+ textTable(conn, false, "Memory parameters (V$SYSTEM_PARAMETER)",
					"select NAME, DISPLAY_VALUE as VALUE, ISDEFAULT from V$SYSTEM_PARAMETER where NAME in ('memory_target', 'memory_max_target', 'sga_target', 'sga_max_size', 'pga_aggregate_target', 'pga_aggregate_limit', 'db_cache_size', 'shared_pool_size', 'use_large_pages', 'inmemory_size')" + andOurContainer(conn) + "order by NAME")

				+ textTable(conn, false, "Memory, the SGA right now (V$SGAINFO)",
					"select NAME, round(BYTES / 1048576) as MB, RESIZEABLE from V$SGAINFO order by BYTES desc")

				+ textTable(conn, true,  "Character set and time zone\n--## Note: the character set is fixed when the database is created",
					"select (select VALUE from NLS_DATABASE_PARAMETERS where PARAMETER = 'NLS_CHARACTERSET') as CHARACTERSET, (select VALUE from NLS_DATABASE_PARAMETERS where PARAMETER = 'NLS_NCHAR_CHARACTERSET') as NCHAR_CHARACTERSET, DBTIMEZONE as DB_TIMEZONE, (select VERSION from V$TIMEZONE_FILE) as TIMEZONE_FILE_VERSION, to_char(SYSTIMESTAMP, 'YYYY-MM-DD HH24:MI:SS TZH:TZM') as SERVER_TIME from dual")

				+ textTable(conn, false, "Where the alert log and the trace files are (V$DIAG_INFO)",
					"select NAME, VALUE from V$DIAG_INFO")
				;
		}
	}

	//---------------------------------------------------------------------------------------------
	public static class Spfile extends OracleTab
	{
		@Override public String getTabLabel() { return "SPFILE"; }
		@Override public String getName()     { return ConfigType.OraSpfile.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			return ""
				+ textTable(conn, false, "The server parameter file that the instance was started with",
					"select nvl(VALUE, '(none: the instance was started with a text parameter file, a pfile)') as SPFILE from V$SYSTEM_PARAMETER where NAME = 'spfile'" + andOurContainer(conn))

				+ textTable(conn, false, "What is set in the spfile (V$SPPARAMETER where ISSPECIFIED = 'TRUE')"
				                       + "\n--## This is the same as you get with: CREATE PFILE FROM SPFILE"
				                       + "\n--## SID            - '*' is for all instances, otherwise for one instance. Names with two underscores is what Oracle saved by itself (automatic memory tuning)"
				                       + "\n--## ORD            - a parameter that is a list (like control_files) has one row per value"
				                       + "\n--## INSTANCE_VALUE - what the instance is running with now. If it differs from SPFILE_VALUE: the spfile value is used at the next restart."
				                       + "\n--##                  But Oracle raises and rounds numbers by itself (memory sizes, 'sessions'), so a small difference in a number is normal."
				                       + "\n--## When connected to a PDB: only what is set for that PDB",
					"select s.SID, s.NAME, s.ORDINAL as ORD, s.VALUE as SPFILE_VALUE, (select p.VALUE from V$SYSTEM_PARAMETER p where p.NAME = s.NAME" + andOurContainer(conn) + ") as INSTANCE_VALUE, s.UPDATE_COMMENT from V$SPPARAMETER s where s.ISSPECIFIED = 'TRUE' order by s.NAME, s.SID, s.ORDINAL")
				;
		}
	}

	//---------------------------------------------------------------------------------------------
	public static class Patches extends OracleTab
	{
		@Override public String getTabLabel() { return "Patches and Components"; }
		@Override public String getName()     { return ConfigType.OraPatches.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			String out = ""
				+ textTable(conn, false, "Version (V$VERSION)",
					"select BANNER from V$VERSION")

				+ textTable(conn, false, "Installed components (DBA_REGISTRY)\n--## STATUS should be VALID (or 'OPTION OFF'). INVALID is normally what is left after an upgrade or a patch that did not finish.",
					"select COMP_ID, COMP_NAME, VERSION, STATUS, MODIFIED from DBA_REGISTRY order by COMP_ID");

			// SQL patches: 12.1 and above
			if (conn.getDbmsVersionInfo().getLongVersion() >= Ver.ver(12,1))
			{
				out += textTable(conn, false, "Patches that are applied in the database (DBA_REGISTRY_SQLPATCH)\n--## Note: A patch must be applied both in the Oracle Home (opatch) and in the database (datapatch). This is the database part.",
					"select to_char(PATCH_ID) as PATCH_ID, ACTION, STATUS, to_char(ACTION_TIME, 'YYYY-MM-DD HH24:MI:SS') as ACTION_TIME, DESCRIPTION from DBA_REGISTRY_SQLPATCH order by ACTION_TIME");
			}

			out += textTable(conn, false, "Upgrade and patch history (DBA_REGISTRY_HISTORY)",
					"select to_char(ACTION_TIME, 'YYYY-MM-DD HH24:MI:SS') as ACTION_TIME, ACTION, NAMESPACE, VERSION, to_char(ID) as ID, COMMENTS from DBA_REGISTRY_HISTORY order by 1");

			return out;
		}
	}

	//---------------------------------------------------------------------------------------------
	public static class Files extends OracleTab
	{
		@Override public String getTabLabel() { return "Files"; }
		@Override public String getName()     { return ConfigType.OraFiles.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			// Connected to CDB$ROOT: the CDB_ views has the files of all containers
			boolean isCdbRoot = ((DbmsVersionInfoOracle) conn.getDbmsVersionInfo()).isCdbRoot();
			String  views     = isCdbRoot ? "CDB_" : "DBA_";
			String  conName   = isCdbRoot ? "(select c.NAME from V$CONTAINERS c where c.CON_ID = x.CON_ID) as CON_NAME, " : "";
			String  conOrder  = isCdbRoot ? "x.CON_ID, " : "";

			return ""
				+ textTable(conn, false, "Tablespaces (" + views + "TABLESPACES)",
					"select " + conName + "x.TABLESPACE_NAME, x.CONTENTS, x.STATUS, x.BLOCK_SIZE, x.EXTENT_MANAGEMENT, x.SEGMENT_SPACE_MANAGEMENT, x.BIGFILE, x.LOGGING, x.FORCE_LOGGING, x.ENCRYPTED from " + views + "TABLESPACES x order by " + conOrder + "x.TABLESPACE_NAME")

				+ textTable(conn, false, "Data files (" + views + "DATA_FILES)\n--## AUTOEXT = the file grows by itself, up to MAX_MB. INCR_BLOCKS = how many blocks it grows with each time.",
					"select " + conName + "x.TABLESPACE_NAME, x.FILE_ID, x.FILE_NAME, round(x.BYTES / 1048576) as MB, x.AUTOEXTENSIBLE as AUTOEXT, round(x.MAXBYTES / 1048576) as MAX_MB, x.INCREMENT_BY as INCR_BLOCKS, x.STATUS, x.ONLINE_STATUS from " + views + "DATA_FILES x order by " + conOrder + "x.TABLESPACE_NAME, x.FILE_ID")

				+ textTable(conn, false, "Temp files (" + views + "TEMP_FILES)",
					"select " + conName + "x.TABLESPACE_NAME, x.FILE_ID, x.FILE_NAME, round(x.BYTES / 1048576) as MB, x.AUTOEXTENSIBLE as AUTOEXT, round(x.MAXBYTES / 1048576) as MAX_MB, x.INCREMENT_BY as INCR_BLOCKS, x.STATUS from " + views + "TEMP_FILES x order by " + conOrder + "x.TABLESPACE_NAME, x.FILE_ID")

				+ textTable(conn, false, "Redo log groups (V$LOG)\n--## There should be at least 3 groups, all of the same size.",
					"select THREAD#, GROUP#, round(BYTES / 1048576) as MB, MEMBERS, STATUS, ARCHIVED, SEQUENCE# from V$LOG order by THREAD#, GROUP#")

				+ textTable(conn, false, "Redo log files (V$LOGFILE)\n--## More than one member per group, on different disks, protects against the loss of a file.",
					"select GROUP#, TYPE, MEMBER, STATUS, IS_RECOVERY_DEST_FILE from V$LOGFILE order by GROUP#, MEMBER")

				+ textTable(conn, false, "Control files (V$CONTROLFILE)\n--## There should be at least 2, on different disks.",
					"select NAME, STATUS, IS_RECOVERY_DEST_FILE, round(BLOCK_SIZE * FILE_SIZE_BLKS / 1048576, 1) as MB from V$CONTROLFILE")
				;
		}
	}

	//---------------------------------------------------------------------------------------------
	public static class Features extends OracleTab
	{
		@Override public String getTabLabel() { return "Options and Feature Usage"; }
		@Override public String getName()     { return ConfigType.OraFeatures.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			// Connected to CDB$ROOT: the CDB_ view has the usage of all containers
			boolean isCdbRoot = ((DbmsVersionInfoOracle) conn.getDbmsVersionInfo()).isCdbRoot();
			String  view      = isCdbRoot ? "CDB_FEATURE_USAGE_STATISTICS" : "DBA_FEATURE_USAGE_STATISTICS";
			String  conName   = isCdbRoot ? "(select c.NAME from V$CONTAINERS c where c.CON_ID = x.CON_ID) as CON_NAME, " : "";
			String  conOrder  = isCdbRoot ? "x.CON_ID, " : "";

			return ""
				+ textTable(conn, false, "Features that Oracle has seen being USED (" + view + ")"
				                       + "\n--## This is what Oracle's own license review looks at. Oracle samples it about once a week, so something new takes a while to show."
				                       + "\n--## It tells what was used, NOT what you have a license for: compare it with your license."
				                       + "\n--## Many rows are features that are part of every edition (and some are used by Oracle itself).",
					"select " + conName + "x.NAME, x.VERSION, x.DETECTED_USAGES, x.CURRENTLY_USED, to_char(x.FIRST_USAGE_DATE, 'YYYY-MM-DD') as FIRST_USED, to_char(x.LAST_USAGE_DATE, 'YYYY-MM-DD') as LAST_USED from " + view + " x where x.DETECTED_USAGES > 0 order by " + conOrder + "x.NAME, x.VERSION")

				+ textTable(conn, false, "Which management packs the instance lets you use (parameter control_management_pack_access)"
				                       + "\n--## DIAGNOSTIC+TUNING is the default. It does NOT mean that you have the licenses for the Diagnostics Pack and the Tuning Pack."
				                       + "\n--## Without those licenses: ALTER SYSTEM SET control_management_pack_access = 'NONE'",
					"select NAME, VALUE from V$SYSTEM_PARAMETER where NAME = 'control_management_pack_access'" + andOurContainer(conn))

				+ textTable(conn, true,  "Sessions and CPUs, as the license views see them (V$LICENSE)",
					"select * from V$LICENSE")

				+ textTable(conn, false, "Options: what this installation can do (V$OPTION)\n--## TRUE = the option is installed. It does not say that it is used, or licensed.",
					"select PARAMETER, VALUE from V$OPTION order by VALUE desc, PARAMETER")
				;
		}
	}

	//---------------------------------------------------------------------------------------------
	public static class BackupConfig extends OracleTab
	{
		@Override public String getTabLabel() { return "Backup Config"; }
		@Override public String getName()     { return ConfigType.OraBackupConfig.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			return ""
				+ textTable(conn, true,  "Archive log mode (V$DATABASE)\n--## NOARCHIVELOG = the database can only be restored to the time of the last backup",
					"select LOG_MODE, FORCE_LOGGING, FLASHBACK_ON from V$DATABASE")

				+ textTable(conn, false, "RMAN settings that are NOT the default (V$RMAN_CONFIGURATION)\n--## No rows = everything is default: retention REDUNDANCY 1, no control file autobackup (before 12.2)...\n--## When connected to a PDB: nothing is shown here",
					"select CONF#, NAME, VALUE from V$RMAN_CONFIGURATION order by CONF#")

				+ textTable(conn, false, "Block change tracking (V$BLOCK_CHANGE_TRACKING)\n--## ENABLED makes incremental backups much faster (Enterprise Edition)",
					"select STATUS, FILENAME, round(BYTES / 1048576) as MB from V$BLOCK_CHANGE_TRACKING")

				+ textTable(conn, false, "Recovery area (V$RECOVERY_FILE_DEST)",
					"select NAME, round(SPACE_LIMIT / 1048576) as LIMIT_MB, round(SPACE_USED / 1048576) as USED_MB, round(SPACE_RECLAIMABLE / 1048576) as RECLAIMABLE_MB, NUMBER_OF_FILES from V$RECOVERY_FILE_DEST")

				+ textTable(conn, false, "Archive destinations that are in use (V$ARCHIVE_DEST)",
					"select DEST_ID, DEST_NAME, STATUS, BINDING, TARGET, DESTINATION, ERROR from V$ARCHIVE_DEST where STATUS <> 'INACTIVE' order by DEST_ID")

				+ textTable(conn, false, "Parameters for backup and recovery (V$SYSTEM_PARAMETER)",
					"select NAME, DISPLAY_VALUE as VALUE, ISDEFAULT from V$SYSTEM_PARAMETER where NAME in ('db_recovery_file_dest', 'db_recovery_file_dest_size', 'db_flashback_retention_target', 'control_file_record_keep_time', 'log_archive_format', 'archive_lag_target', 'undo_retention', 'fast_start_mttr_target')" + andOurContainer(conn) + "order by NAME")
				;
		}
	}

	//---------------------------------------------------------------------------------------------
	public static class Jobs extends OracleTab
	{
		@Override public String getTabLabel() { return "Auto Tasks and Jobs"; }
		@Override public String getName()     { return ConfigType.OraJobs.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			return ""
				+ textTable(conn, false, "Automatic maintenance tasks (DBA_AUTOTASK_CLIENT)\n--## 'auto optimizer stats collection' should be ENABLED, unless you gather the optimizer statistics in another way",
					"select CLIENT_NAME, STATUS from DBA_AUTOTASK_CLIENT order by CLIENT_NAME")

				+ textTable(conn, false, "Maintenance windows: when the automatic tasks are allowed to run (DBA_SCHEDULER_WINDOWS)",
					"select WINDOW_NAME, ENABLED, ACTIVE, REPEAT_INTERVAL, to_char(DURATION) as DURATION, to_char(NEXT_START_DATE, 'YYYY-MM-DD HH24:MI TZH:TZM') as NEXT_START, to_char(LAST_START_DATE, 'YYYY-MM-DD HH24:MI TZH:TZM') as LAST_START from DBA_SCHEDULER_WINDOWS order by WINDOW_NAME")

				+ textTable(conn, false, "Scheduler jobs (DBA_SCHEDULER_JOBS)\n--## FAILURE_COUNT = how many times the job has failed since it was created",
					"select OWNER, JOB_NAME, ENABLED, STATE, substr(REPEAT_INTERVAL, 1, 60) as REPEAT_INTERVAL, to_char(LAST_START_DATE, 'YYYY-MM-DD HH24:MI TZH:TZM') as LAST_START, to_char(LAST_RUN_DURATION) as LAST_DURATION, to_char(NEXT_RUN_DATE, 'YYYY-MM-DD HH24:MI TZH:TZM') as NEXT_RUN, RUN_COUNT, FAILURE_COUNT from DBA_SCHEDULER_JOBS order by OWNER, JOB_NAME")

				+ textTable(conn, false, "Jobs of the old kind, DBMS_JOB (DBA_JOBS)\n--## BROKEN = Y: the job is not run any more (Oracle sets that after 16 failures)",
					"select JOB, SCHEMA_USER, BROKEN, FAILURES, to_char(LAST_DATE, 'YYYY-MM-DD HH24:MI') as LAST_DATE, to_char(NEXT_DATE, 'YYYY-MM-DD HH24:MI') as NEXT_DATE, INTERVAL, substr(WHAT, 1, 80) as WHAT from DBA_JOBS order by JOB")
				;
		}
	}

	//---------------------------------------------------------------------------------------------
	public static class Security extends OracleTab
	{
		@Override public String getTabLabel() { return "Profiles and Privileged Users"; }
		@Override public String getName()     { return ConfigType.OraSecurity.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			return ""
				+ textTable(conn, false, "Password rules per profile (DBA_PROFILES)"
				                       + "\n--## USERS = how many accounts that has the profile.  DEFAULT = the value of the profile named DEFAULT is used"
				                       + "\n--## PASSWORD_LIFE_TIME is in days: when it has passed the account can't log in until the password is changed (see the 'Accounts' tab and its alarm)",
					"select p.PROFILE"
					+ ", (select count(*) from DBA_USERS u where u.PROFILE = p.PROFILE) as USERS"
					+ ", max(case when p.RESOURCE_NAME = 'FAILED_LOGIN_ATTEMPTS'    then p.LIMIT else ' ' end) as FAILED_LOGIN_ATTEMPTS"
					+ ", max(case when p.RESOURCE_NAME = 'PASSWORD_LOCK_TIME'       then p.LIMIT else ' ' end) as PASSWORD_LOCK_TIME"
					+ ", max(case when p.RESOURCE_NAME = 'PASSWORD_LIFE_TIME'       then p.LIMIT else ' ' end) as PASSWORD_LIFE_TIME"
					+ ", max(case when p.RESOURCE_NAME = 'PASSWORD_GRACE_TIME'      then p.LIMIT else ' ' end) as PASSWORD_GRACE_TIME"
					+ ", max(case when p.RESOURCE_NAME = 'PASSWORD_REUSE_TIME'      then p.LIMIT else ' ' end) as PASSWORD_REUSE_TIME"
					+ ", max(case when p.RESOURCE_NAME = 'PASSWORD_REUSE_MAX'       then p.LIMIT else ' ' end) as PASSWORD_REUSE_MAX"
					+ ", max(case when p.RESOURCE_NAME = 'PASSWORD_VERIFY_FUNCTION' then p.LIMIT else ' ' end) as PASSWORD_VERIFY_FUNCTION"
					+ ", max(case when p.RESOURCE_NAME = 'INACTIVE_ACCOUNT_TIME'    then p.LIMIT else ' ' end) as INACTIVE_ACCOUNT_TIME"
					+ " from DBA_PROFILES p where p.RESOURCE_TYPE = 'PASSWORD' group by p.PROFILE order by p.PROFILE")

				+ textTable(conn, false, "Who can connect AS SYSDBA and the like, with a password (V$PWFILE_USERS)",
					"select * from V$PWFILE_USERS")

				+ textTable(conn, false, "Who has the DBA role (DBA_ROLE_PRIVS)",
					"select GRANTEE, ADMIN_OPTION, DEFAULT_ROLE from DBA_ROLE_PRIVS where GRANTED_ROLE = 'DBA' order by GRANTEE")

				+ textTable(conn, false, "Accounts with '... ANY ...' system privileges granted directly, not through a role (DBA_SYS_PRIVS)\n--## An application account should normally not be in this list",
					"select s.GRANTEE, count(*) as ANY_PRIVILEGES from DBA_SYS_PRIVS s where s.PRIVILEGE like '% ANY %' and s.GRANTEE in (select u.USERNAME from DBA_USERS u) group by s.GRANTEE order by 2 desc, 1")

				+ textTable(conn, false, "Parameters for logins and auditing (V$SYSTEM_PARAMETER)",
					"select NAME, DISPLAY_VALUE as VALUE, ISDEFAULT from V$SYSTEM_PARAMETER where NAME in ('audit_trail', 'audit_sys_operations', 'sec_case_sensitive_logon', 'sec_max_failed_login_attempts', 'remote_login_passwordfile', 'remote_os_authent', 'os_authent_prefix', 'O7_DICTIONARY_ACCESSIBILITY', 'sql92_security')" + andOurContainer(conn) + "order by NAME")
				;
		}
	}

	//---------------------------------------------------------------------------------------------
	public static class Containers extends OracleTab
	{
		@Override public String getTabLabel() { return "Containers"; }
		@Override public String getName()     { return ConfigType.OraContainers.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			DbmsVersionInfoOracle oraVersionInfo = (DbmsVersionInfoOracle) conn.getDbmsVersionInfo();

			if ( ! oraVersionInfo.isCdb() )
				return "This is not a Container Database (containers and PDBs exists in Oracle 12.1 and above, when the database was created as a CDB).\n";

			String out = ""
				+ textTable(conn, false, "Containers (V$CONTAINERS)\n--## A PDB in OPEN_MODE 'MOUNTED' is closed: nobody can connect to it\n--## When connected to a PDB: only that PDB is shown",
					"select CON_ID, NAME, to_char(DBID) as DBID, OPEN_MODE, RESTRICTED, to_char(OPEN_TIME, 'YYYY-MM-DD HH24:MI:SS') as OPEN_TIME, round(TOTAL_SIZE / 1048576) as TOTAL_MB from V$CONTAINERS order by CON_ID")

				+ textTable(conn, false, "Pluggable Databases (CDB_PDBS)",
					"select PDB_ID, PDB_NAME, STATUS from CDB_PDBS order by PDB_ID")

				+ textTable(conn, false, "Saved states: the PDBs that are opened by themselves when the instance starts (CDB_PDB_SAVED_STATES)"
				                       + "\n--## A PDB that is NOT in this list stays MOUNTED (closed) after a restart of the instance."
				                       + "\n--## To put it in the list: ALTER PLUGGABLE DATABASE <name> SAVE STATE (when it's open)",
					"select CON_NAME, INSTANCE_NAME, STATE from CDB_PDB_SAVED_STATES order by CON_NAME");

			// From CDB$ROOT we see the parameters of all containers
			if (oraVersionInfo.isCdbRoot())
			{
				out += textTable(conn, false, "Parameters where a PDB has another value than the CDB (V$SYSTEM_PARAMETER)\n--## CDB_VALUE is the value for the whole CDB (CON_ID 0). Note: sga_target = 0 in a PDB means 'no limit for this PDB'",
					"select (select c.NAME from V$CONTAINERS c where c.CON_ID = x.CON_ID) as CON_NAME, x.NAME, x.VALUE, x.CDB_VALUE "
					+ "from (select p.CON_ID, p.NAME, p.DISPLAY_VALUE as VALUE, max(case when p.CON_ID = 0 then p.DISPLAY_VALUE end) over (partition by p.NAME) as CDB_VALUE from V$SYSTEM_PARAMETER p) x "
					+ "where x.CON_ID > 0 and nvl(x.VALUE, '-') <> nvl(x.CDB_VALUE, '-') order by x.CON_ID, x.NAME");
			}

			return out;
		}
	}

	//---------------------------------------------------------------------------------------------
	public static class DbLinks extends OracleTab
	{
		@Override public String getTabLabel() { return "DB Links"; }
		@Override public String getName()     { return ConfigType.OraDbLinks.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			return ""
				+ textTable(conn, false, "Database links: the other databases that this one connects to (DBA_DB_LINKS)\n--## OWNER 'PUBLIC' = every user can use the link.  USERNAME = the account it logs in with on the other side (empty = the same user as here)",
					"select OWNER, DB_LINK, USERNAME, HOST, to_char(CREATED, 'YYYY-MM-DD') as CREATED from DBA_DB_LINKS order by OWNER, DB_LINK")
				;
		}
	}

	//---------------------------------------------------------------------------------------------
	public static class NlsParams extends OracleTab
	{
		@Override public String getTabLabel() { return "NLS Params"; }
		@Override public String getName()     { return ConfigType.OraNlsParams.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			return ""
				+ textTable(conn, false, "The database (NLS_DATABASE_PARAMETERS)\n--## This is what the database was created with. NLS_CHARACTERSET is the character set of the data: it can't be changed with a parameter.",
					"select PARAMETER, VALUE from NLS_DATABASE_PARAMETERS order by PARAMETER")

				+ textTable(conn, false, "The instance (NLS_INSTANCE_PARAMETERS)\n--## What is set with initialization parameters. A client that sets its own language and territory (most do) overrides these for its session.",
					"select PARAMETER, VALUE from NLS_INSTANCE_PARAMETERS order by PARAMETER")
				;
		}
	}
}
