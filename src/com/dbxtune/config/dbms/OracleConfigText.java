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
		,OraAudit
		,OraResourceManager
		,OraDataGuard
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
		DbmsConfigTextManager.addInstance(new OracleConfigText.Audit());
		DbmsConfigTextManager.addInstance(new OracleConfigText.ResourceManager());
		DbmsConfigTextManager.addInstance(new OracleConfigText.DataGuard());
		DbmsConfigTextManager.addInstance(new OracleConfigText.Containers());
		DbmsConfigTextManager.addInstance(new OracleConfigText.DbLinks());
		DbmsConfigTextManager.addInstance(new OracleConfigText.NlsParams());
	}

	/**
	 * One part of a tab: a title, then the result of the SQL as a text table.
	 * <p>
	 * If the SQL fails (a view that this Oracle version does not have, a missing privilege...) the error text is put there instead, so the rest of the tab is still shown.
	 *
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

		sb.append("\n");
		sb.append("--######################################################################################\n");
		sb.append("--## ").append(title).append("\n");
		sb.append("--######################################################################################\n");

		try
		{
			ResultSetTableModel rstm = ResultSetTableModel.executeQuery(conn, sql, 30, title);

			// A '|' in a value would be taken as a column separator when the text is read back (and that row is then skipped)
			// So show it as the "broken bar" character. Seen in DBA_REGISTRY_HISTORY.COMMENTS on Oracle 23.
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
			sb.append("Could not get this: ").append(ex.getMessage().trim()).append("\n");

			// 942 = table or view does not exist, 1031 = insufficient privileges
			if (ex.getErrorCode() == 942 || ex.getErrorCode() == 1031)
				sb.append("If the view exists in this Oracle version, the login is not allowed to read it. Fix: GRANT SELECT_CATALOG_ROLE TO <user>; (or the role that the title above names)\n");
		}

		return sb.toString();
	}

	/**
	 * The feature names in DBA_FEATURE_USAGE_STATISTICS that belong to an Enterprise Edition OPTION or a MANAGEMENT PACK: those are licensed separately.
	 * <p>
	 * { license, feature name, condition }<br>
	 * The condition says when the usage counts:
	 * <ul>
	 *   <li>ANY         - always</li>
	 *   <li>COMPRESSION - only when FEATURE_INFO says that compression was used (Data Pump and Data Guard are free without it)</li>
	 *   <li>ENCRYPTION  - only when FEATURE_INFO says that encryption was used</li>
	 *   <li>PDBS        - only with more user PDBs than what is free: 3 from 19c, 1 before that (AUX_COUNT is the number of PDBs)</li>
	 *   <li>V11         - only what version 11 has recorded (later versions record it for things the database does by itself)</li>
	 *   <li>V12_2       - only what version 12.2 and above has recorded (earlier versions recorded it wrongly)</li>
	 * </ul>
	 * Sources:
	 * <ul>
	 *   <li>Oracle's manual "Database Licensing Information", 19c and 23ai: the tables with the options and the management packs (which products there are, and what belongs to them)</li>
	 *   <li>The list of features per product, and the conditions, in Oracle's own script <code>options_packs_usage_statistics.sql</code> (My Oracle Support, Document ID KB143588, earlier 1317265.1),
	 *       as it was for 12.2 (read from a published copy: the script itself needs a My Oracle Support login)</li>
	 *   <li>Added from the manuals: 'Oracle SQL Firewall' (it is a part of Database Vault), and the In-Memory features that came in 18c and later</li>
	 * </ul>
	 * NOT verified against a real database: the texts in FEATURE_INFO for Data Guard, and for Data Pump with compression or encryption (only without),
	 * and everything that is not in the Express / Free editions the tests ran on.
	 * Not in the list, on purpose:
	 * <ul>
	 *   <li>What does not need a license (any more): Spatial, Machine Learning ('Data Mining'), Privilege Analysis ('Privilege Capture'), Heat Map, SQL Tuning Sets, network encryption, 'In-Memory Base Level'</li>
	 *   <li>What every database does by itself: 'Automatic Maintenance - SQL Tuning Advisor', 'Automatic SQL Tuning Advisor' (it is using its advice, a SQL Profile, that needs the Tuning Pack)</li>
	 *   <li>What Oracle's script leaves out because it is recorded wrongly: 'Advanced Index Compression'</li>
	 *   <li>Other products than the database options and packs: GoldenGate, Gateways, Exadata and other Oracle storage</li>
	 * </ul>
	 */
	private static final String[][] EXTRA_COST_FEATURES =
	{
		{ "Diagnostics Pack"                    , "ADDM"                                                    , "ANY"         },
		{ "Diagnostics Pack"                    , "AWR Baseline"                                            , "ANY"         },
		{ "Diagnostics Pack"                    , "AWR Baseline Template"                                   , "ANY"         },
		{ "Diagnostics Pack"                    , "AWR Report"                                              , "ANY"         },
		{ "Diagnostics Pack"                    , "Automatic Workload Repository"                           , "ANY"         },
		{ "Diagnostics Pack"                    , "Baseline Adaptive Thresholds"                            , "ANY"         },
		{ "Diagnostics Pack"                    , "Baseline Static Computations"                            , "ANY"         },
		{ "Diagnostics Pack"                    , "Diagnostic Pack"                                         , "ANY"         },
		{ "Diagnostics Pack"                    , "EM Performance Page"                                     , "ANY"         },

		{ "Tuning Pack"                         , "SQL Access Advisor"                                      , "ANY"         },
		{ "Tuning Pack"                         , "SQL Monitoring and Tuning pages"                         , "ANY"         },
		{ "Tuning Pack"                         , "SQL Profile"                                             , "ANY"         },
		{ "Tuning Pack"                         , "SQL Tuning Advisor"                                      , "ANY"         },
		{ "Tuning Pack"                         , "Tuning Pack"                                             , "ANY"         },
		{ "Tuning Pack"                         , "Real-Time SQL Monitoring"                                , "V11"         },

		{ "Data Masking and Subsetting Pack"    , "Data Masking Pack"                                       , "ANY"         },
		{ "Database Lifecycle Management Pack"  , "Change Management Pack"                                  , "ANY"         },
		{ "Database Lifecycle Management Pack"  , "EM Config Management Pack"                               , "ANY"         },
		{ "Database Lifecycle Management Pack"  , "EM Database Provisioning and Patch Automation Pack"      , "ANY"         },

		{ "Partitioning"                        , "Partitioning (user)"                                     , "ANY"         },
		{ "Partitioning"                        , "Zone maps"                                               , "ANY"         },

		{ "Advanced Compression"                , "Backup HIGH Compression"                                 , "ANY"         },
		{ "Advanced Compression"                , "Backup LOW Compression"                                  , "ANY"         },
		{ "Advanced Compression"                , "Backup MEDIUM Compression"                               , "ANY"         },
		{ "Advanced Compression"                , "Backup ZLIB Compression"                                 , "ANY"         },
		{ "Advanced Compression"                , "HeapCompression"                                         , "V12_2"       },
		{ "Advanced Compression"                , "Hybrid Columnar Compression Row Level Locking"           , "ANY"         },
		{ "Advanced Compression"                , "Information Lifecycle Management"                        , "ANY"         },
		{ "Advanced Compression"                , "Oracle Advanced Network Compression Service"             , "ANY"         },
		{ "Advanced Compression"                , "SecureFile Compression (user)"                           , "ANY"         },
		{ "Advanced Compression"                , "SecureFile Deduplication (user)"                         , "ANY"         },
		{ "Advanced Compression"                , "Data Guard"                                              , "COMPRESSION" },
		{ "Advanced Compression"                , "Oracle Utility Datapump (Export)"                        , "COMPRESSION" },
		{ "Advanced Compression"                , "Oracle Utility Datapump (Import)"                        , "COMPRESSION" },

		{ "Advanced Security"                   , "Data Redaction"                                          , "ANY"         },
		{ "Advanced Security"                   , "Encrypted Tablespaces"                                   , "ANY"         },
		{ "Advanced Security"                   , "SecureFile Encryption (user)"                            , "ANY"         },
		{ "Advanced Security"                   , "Transparent Data Encryption"                             , "ANY"         },
		{ "Advanced Security"                   , "Backup Encryption"                                       , "V11"         },
		{ "Advanced Security"                   , "Oracle Utility Datapump (Export)"                        , "ENCRYPTION"  },
		{ "Advanced Security"                   , "Oracle Utility Datapump (Import)"                        , "ENCRYPTION"  },

		{ "Database In-Memory"                  , "In-Memory ADO Policies"                                  , "ANY"         },
		{ "Database In-Memory"                  , "In-Memory Aggregation"                                   , "ANY"         },
		{ "Database In-Memory"                  , "In-Memory Column Store"                                  , "ANY"         },
		{ "Database In-Memory"                  , "In-Memory Distribute For Service (User Defined)"         , "ANY"         },
		{ "Database In-Memory"                  , "In-Memory Expressions"                                   , "ANY"         },
		{ "Database In-Memory"                  , "In-Memory FastStart"                                     , "ANY"         },
		{ "Database In-Memory"                  , "In-Memory Join Groups"                                   , "ANY"         },

		{ "Active Data Guard"                   , "Active Data Guard - Real-Time Query on Physical Standby" , "ANY"         },
		{ "Active Data Guard"                   , "Global Data Services"                                    , "ANY"         },

		{ "Database Vault"                      , "Oracle Database Vault"                                   , "ANY"         },
		{ "Database Vault"                      , "Oracle SQL Firewall"                                     , "ANY"         },

		{ "Label Security"                      , "Label Security"                                          , "ANY"         },

		{ "Multitenant"                         , "Oracle Multitenant"                                      , "PDBS"        },
		{ "Multitenant"                         , "Oracle Pluggable Databases"                              , "PDBS"        },

		{ "OLAP"                                , "OLAP - Analytic Workspaces"                              , "ANY"         },
		{ "OLAP"                                , "OLAP - Cubes"                                            , "ANY"         },

		{ "Real Application Clusters"           , "Real Application Clusters (RAC)"                         , "ANY"         },
		{ "Real Application Clusters"           , "Quality of Service Management"                           , "ANY"         },
		{ "RAC One Node"                        , "Real Application Cluster One Node"                       , "ANY"         },

		{ "Real Application Testing"            , "Database Replay: Workload Capture"                       , "ANY"         },
		{ "Real Application Testing"            , "Database Replay: Workload Replay"                        , "ANY"         },
		{ "Real Application Testing"            , "SQL Performance Analyzer"                                , "ANY"         },
	};

	/**
	 * The features that need an extra-cost license, that Oracle has recorded the use of (see {@link #EXTRA_COST_FEATURES}).<br>
	 * Used by the tab "Options and Feature Usage" and by the check in <code>OracleConfig.checkConfig()</code>
	 *
	 * @return SQL with the columns: [CON_NAME, when connected to CDB$ROOT] LICENSE, FEATURE, VERSION, DETECTED_USAGES, CURRENTLY_USED, FIRST_USED, LAST_USED
	 */
	public static String getExtraCostFeatureUsageSql(DbxConnection conn)
	{
		// Connected to CDB$ROOT: the CDB_ view has the usage of all containers
		boolean isCdbRoot = ((DbmsVersionInfoOracle) conn.getDbmsVersionInfo()).isCdbRoot();
		String  view      = isCdbRoot ? "CDB_FEATURE_USAGE_STATISTICS" : "DBA_FEATURE_USAGE_STATISTICS";
		String  conName   = isCdbRoot ? "(select c.NAME from V$CONTAINERS c where c.CON_ID = x.CON_ID) as CON_NAME, " : "";
		String  conOrder  = isCdbRoot ? "x.CON_ID, " : "";

		// The list above as rows
		String map = "";
		for (String[] f : EXTRA_COST_FEATURES)
			map += (map.isEmpty() ? "" : " union all \n") + "      select '" + f[0] + "' as LICENSE, '" + f[1] + "' as NAME, '" + f[2] + "' as COND from DUAL";

		String featureInfo  = "to_char(substr(x.FEATURE_INFO, 1, 2000))";
		String majorVersion = "to_number(regexp_substr(x.VERSION, '^[0-9]+'))";

		// FEATURE_INFO: 'compression used: 3 times' or 'compression used: TRUE' counts, 'compression used: 0 times' and 'compression used: FALSE' does not
		return ""
			+ "select " + conName + "m.LICENSE, x.NAME as FEATURE, x.VERSION, x.DETECTED_USAGES, x.CURRENTLY_USED, to_char(x.FIRST_USAGE_DATE, 'YYYY-MM-DD') as FIRST_USED, to_char(x.LAST_USAGE_DATE, 'YYYY-MM-DD') as LAST_USED \n"
			+ "from " + view + " x \n"
			+ "join ( \n"
			+ map + " \n"
			+ "     ) m on m.NAME = x.NAME \n"
			+ "where x.DETECTED_USAGES > 0 \n"
			+ "  and (   m.COND = 'ANY' \n"
			+ "       or (m.COND = 'COMPRESSION' and regexp_like(" + featureInfo + ", 'compression used: *(TRUE|[0-9 ]*[1-9])', 'i')) \n"
			+ "       or (m.COND = 'ENCRYPTION'  and regexp_like(" + featureInfo + ", 'encryption used: *(TRUE|[0-9 ]*[1-9])', 'i')) \n"
			+ "       or (m.COND = 'PDBS'        and x.AUX_COUNT > case when " + majorVersion + " >= 19 then 3 else 1 end) \n"
			+ "       or (m.COND = 'V11'         and x.VERSION like '11.%') \n"
			+ "       or (m.COND = 'V12_2'       and (x.VERSION like '12.2%' or " + majorVersion + " >= 18)) \n"
			+ "      ) \n"
			+ "order by " + conOrder + "m.LICENSE, x.NAME, x.VERSION";
	}

	/** The Express and the Free edition: they are free to use, there is nothing extra to pay for (V$INSTANCE.EDITION is XE or FREE) */
	public static boolean isFreeEdition(String edition)
	{
		return "XE".equalsIgnoreCase(edition) || "FREE".equalsIgnoreCase(edition);
	}

	/** Standard Edition: options and management packs can not be licensed for it (V$INSTANCE.EDITION is SE or SE2) */
	public static boolean isStandardEdition(String edition)
	{
		return "SE".equalsIgnoreCase(edition) || "SE2".equalsIgnoreCase(edition);
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
				+ textTable(conn, "Database (V$DATABASE)",
					"select NAME, DB_UNIQUE_NAME, to_char(DBID) as DBID, CREATED, LOG_MODE, OPEN_MODE, DATABASE_ROLE" + cdb + ", PROTECTION_MODE, SWITCHOVER_STATUS, DATAGUARD_BROKER, FORCE_LOGGING, FLASHBACK_ON, SUPPLEMENTAL_LOG_DATA_MIN, PLATFORM_NAME, RESETLOGS_TIME, CONTROLFILE_TYPE from V$DATABASE")

				+ textTable(conn, "Instance (V$INSTANCE)",
					"select * from V$INSTANCE")

				+ textTable(conn, "Version (V$VERSION)",
					"select BANNER from V$VERSION")

				+ textTable(conn, "Host (V$OSSTAT)",
					"select STAT_NAME, case when STAT_NAME = 'PHYSICAL_MEMORY_BYTES' then round(VALUE / 1048576) || ' MB' else to_char(VALUE) end as VALUE, COMMENTS from V$OSSTAT where STAT_NAME in ('NUM_CPUS', 'NUM_CPU_CORES', 'NUM_CPU_SOCKETS', 'PHYSICAL_MEMORY_BYTES') order by STAT_NAME")

				+ textTable(conn, "Memory parameters (V$SYSTEM_PARAMETER)",
					"select NAME, DISPLAY_VALUE as VALUE, ISDEFAULT from V$SYSTEM_PARAMETER where NAME in ('memory_target', 'memory_max_target', 'sga_target', 'sga_max_size', 'pga_aggregate_target', 'pga_aggregate_limit', 'db_cache_size', 'shared_pool_size', 'use_large_pages', 'inmemory_size')" + andOurContainer(conn) + "order by NAME")

				+ textTable(conn, "Memory, the SGA right now (V$SGAINFO)",
					"select NAME, round(BYTES / 1048576) as MB, RESIZEABLE from V$SGAINFO order by BYTES desc")

				+ textTable(conn, "Character set and time zone\n--## Note: the character set is fixed when the database is created",
					"select (select VALUE from NLS_DATABASE_PARAMETERS where PARAMETER = 'NLS_CHARACTERSET') as CHARACTERSET, (select VALUE from NLS_DATABASE_PARAMETERS where PARAMETER = 'NLS_NCHAR_CHARACTERSET') as NCHAR_CHARACTERSET, DBTIMEZONE as DB_TIMEZONE, (select VERSION from V$TIMEZONE_FILE) as TIMEZONE_FILE_VERSION, to_char(SYSTIMESTAMP, 'YYYY-MM-DD HH24:MI:SS TZH:TZM') as SERVER_TIME from dual")

				+ textTable(conn, "Where the alert log and the trace files are (V$DIAG_INFO)",
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
				+ textTable(conn, "The server parameter file that the instance was started with",
					"select nvl(VALUE, '(none: the instance was started with a text parameter file, a pfile)') as SPFILE from V$SYSTEM_PARAMETER where NAME = 'spfile'" + andOurContainer(conn))

				+ textTable(conn, "What is set in the spfile (V$SPPARAMETER where ISSPECIFIED = 'TRUE')"
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
				+ textTable(conn, "Version (V$VERSION)",
					"select BANNER from V$VERSION")

				+ textTable(conn, "Installed components (DBA_REGISTRY)\n--## STATUS should be VALID (or 'OPTION OFF'). INVALID is normally what is left after an upgrade or a patch that did not finish.",
					"select COMP_ID, COMP_NAME, VERSION, STATUS, MODIFIED from DBA_REGISTRY order by COMP_ID");

			// SQL patches: 12.1 and above
			if (conn.getDbmsVersionInfo().getLongVersion() >= Ver.ver(12,1))
			{
				out += textTable(conn, "Patches that are applied in the database (DBA_REGISTRY_SQLPATCH)\n--## Note: A patch must be applied both in the Oracle Home (opatch) and in the database (datapatch). This is the database part.",
					"select to_char(PATCH_ID) as PATCH_ID, ACTION, STATUS, to_char(ACTION_TIME, 'YYYY-MM-DD HH24:MI:SS') as ACTION_TIME, DESCRIPTION from DBA_REGISTRY_SQLPATCH order by ACTION_TIME");
			}

			out += textTable(conn, "Upgrade and patch history (DBA_REGISTRY_HISTORY)",
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
				+ textTable(conn, "Tablespaces (" + views + "TABLESPACES)",
					"select " + conName + "x.TABLESPACE_NAME, x.CONTENTS, x.STATUS, x.BLOCK_SIZE, x.EXTENT_MANAGEMENT, x.SEGMENT_SPACE_MANAGEMENT, x.BIGFILE, x.LOGGING, x.FORCE_LOGGING, x.ENCRYPTED from " + views + "TABLESPACES x order by " + conOrder + "x.TABLESPACE_NAME")

				+ textTable(conn, "Data files (" + views + "DATA_FILES)\n--## AUTOEXT = the file grows by itself, up to MAX_MB. INCR_BLOCKS = how many blocks it grows with each time.",
					"select " + conName + "x.TABLESPACE_NAME, x.FILE_ID, x.FILE_NAME, round(x.BYTES / 1048576) as MB, x.AUTOEXTENSIBLE as AUTOEXT, round(x.MAXBYTES / 1048576) as MAX_MB, x.INCREMENT_BY as INCR_BLOCKS, x.STATUS, x.ONLINE_STATUS from " + views + "DATA_FILES x order by " + conOrder + "x.TABLESPACE_NAME, x.FILE_ID")

				+ textTable(conn, "Temp files (" + views + "TEMP_FILES)",
					"select " + conName + "x.TABLESPACE_NAME, x.FILE_ID, x.FILE_NAME, round(x.BYTES / 1048576) as MB, x.AUTOEXTENSIBLE as AUTOEXT, round(x.MAXBYTES / 1048576) as MAX_MB, x.INCREMENT_BY as INCR_BLOCKS, x.STATUS from " + views + "TEMP_FILES x order by " + conOrder + "x.TABLESPACE_NAME, x.FILE_ID")

				+ textTable(conn, "Redo log groups (V$LOG)\n--## There should be at least 3 groups, all of the same size.",
					"select THREAD#, GROUP#, round(BYTES / 1048576) as MB, MEMBERS, STATUS, ARCHIVED, SEQUENCE# from V$LOG order by THREAD#, GROUP#")

				+ textTable(conn, "Redo log files (V$LOGFILE)\n--## More than one member per group, on different disks, protects against the loss of a file.",
					"select GROUP#, TYPE, MEMBER, STATUS, IS_RECOVERY_DEST_FILE from V$LOGFILE order by GROUP#, MEMBER")

				+ textTable(conn, "Control files (V$CONTROLFILE)\n--## There should be at least 2, on different disks.",
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

			// What the edition means for the first list
			String edition     = ((DbmsVersionInfoOracle) conn.getDbmsVersionInfo()).getEdition();
			String editionNote = "The edition of this database is not known: on Enterprise Edition every LICENSE in the list must be in your license agreement.";
			if      (isFreeEdition(edition))     editionNote = "This is a FREE edition (" + edition + "): there is nothing extra to pay for, so a row here is NOT a license problem.";
			else if (isStandardEdition(edition)) editionNote = "This is STANDARD Edition (" + edition + "): options and management packs can NOT be licensed for it, so a row here is a license problem.";
			else if ("EE".equalsIgnoreCase(edition)) editionNote = "This is ENTERPRISE Edition: every LICENSE in the list must be in your license agreement.";

			return ""
				+ textTable(conn, "Features that need an EXTRA-COST license, that Oracle has seen being USED (the rows of the next list that do)"
				                       + "\n--## An Enterprise Edition OPTION (Partitioning, Advanced Compression...) or a MANAGEMENT PACK (Diagnostics, Tuning...) is licensed separately."
				                       + "\n--## " + editionNote
				                       + "\n--## No rows = Oracle has not recorded the use of any of them."
				                       + "\n--## It tells what was USED, not what you have a license for: compare it with your license agreement. It is information, not a license statement."
				                       + "\n--## Oracle samples the usage about once a week. And it does not see everything: a plain SELECT from a view that belongs to a pack"
				                       + "\n--## (V$ACTIVE_SESSION_HISTORY, DBA_HIST_...) is not recorded."
				                       + "\n--## Some usage only counts in some cases, and then only those are listed: Data Pump and Data Guard with compression or encryption,"
				                       + "\n--## Multitenant with more than 3 PDBs (more than 1 before 19c)."
				                       + "\n--## Not listed, as in Oracle's own script: what every database does by itself (like 'Automatic Maintenance - SQL Tuning Advisor')."
				                       + "\n--## Oracle's own check is the script options_packs_usage_statistics.sql (My Oracle Support, Document ID KB143588): run that before a license review.",
					getExtraCostFeatureUsageSql(conn))

				+ textTable(conn, "Features that Oracle has seen being USED (" + view + ")"
				                       + "\n--## This is what Oracle's own license review looks at. Oracle samples it about once a week, so something new takes a while to show."
				                       + "\n--## It tells what was used, NOT what you have a license for: compare it with your license."
				                       + "\n--## Many rows are features that are part of every edition (and some are used by Oracle itself).",
					"select " + conName + "x.NAME, x.VERSION, x.DETECTED_USAGES, x.CURRENTLY_USED, to_char(x.FIRST_USAGE_DATE, 'YYYY-MM-DD') as FIRST_USED, to_char(x.LAST_USAGE_DATE, 'YYYY-MM-DD') as LAST_USED from " + view + " x where x.DETECTED_USAGES > 0 order by " + conOrder + "x.NAME, x.VERSION")

				+ textTable(conn, "Which management packs the instance lets you use (parameter control_management_pack_access)"
				                       + "\n--## DIAGNOSTIC+TUNING is the default. It does NOT mean that you have the licenses for the Diagnostics Pack and the Tuning Pack."
				                       + "\n--## Without those licenses: ALTER SYSTEM SET control_management_pack_access = 'NONE'",
					"select NAME, VALUE from V$SYSTEM_PARAMETER where NAME = 'control_management_pack_access'" + andOurContainer(conn))

				+ textTable(conn, "Sessions and CPUs, as the license views see them (V$LICENSE)",
					"select * from V$LICENSE")

				+ textTable(conn, "Options: what this installation can do (V$OPTION)\n--## TRUE = the option is installed. It does not say that it is used, or licensed.",
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
				+ textTable(conn, "Archive log mode (V$DATABASE)\n--## NOARCHIVELOG = the database can only be restored to the time of the last backup",
					"select LOG_MODE, FORCE_LOGGING, FLASHBACK_ON from V$DATABASE")

				+ textTable(conn, "RMAN settings that are NOT the default (V$RMAN_CONFIGURATION)\n--## No rows = everything is default: retention REDUNDANCY 1, no control file autobackup (before 12.2)...\n--## When connected to a PDB: nothing is shown here",
					"select CONF#, NAME, VALUE from V$RMAN_CONFIGURATION order by CONF#")

				+ textTable(conn, "Block change tracking (V$BLOCK_CHANGE_TRACKING)\n--## ENABLED makes incremental backups much faster (Enterprise Edition)",
					"select STATUS, FILENAME, round(BYTES / 1048576) as MB from V$BLOCK_CHANGE_TRACKING")

				+ textTable(conn, "Recovery area (V$RECOVERY_FILE_DEST)",
					"select NAME, round(SPACE_LIMIT / 1048576) as LIMIT_MB, round(SPACE_USED / 1048576) as USED_MB, round(SPACE_RECLAIMABLE / 1048576) as RECLAIMABLE_MB, NUMBER_OF_FILES from V$RECOVERY_FILE_DEST")

				+ textTable(conn, "Archive destinations that are in use (V$ARCHIVE_DEST)",
					"select DEST_ID, DEST_NAME, STATUS, BINDING, TARGET, DESTINATION, ERROR from V$ARCHIVE_DEST where STATUS <> 'INACTIVE' order by DEST_ID")

				+ textTable(conn, "Parameters for backup and recovery (V$SYSTEM_PARAMETER)",
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
				+ textTable(conn, "Automatic maintenance tasks (DBA_AUTOTASK_CLIENT)\n--## 'auto optimizer stats collection' should be ENABLED, unless you gather the optimizer statistics in another way",
					"select CLIENT_NAME, STATUS from DBA_AUTOTASK_CLIENT order by CLIENT_NAME")

				+ textTable(conn, "Maintenance windows: when the automatic tasks are allowed to run (DBA_SCHEDULER_WINDOWS)",
					"select WINDOW_NAME, ENABLED, ACTIVE, REPEAT_INTERVAL, to_char(DURATION) as DURATION, to_char(NEXT_START_DATE, 'YYYY-MM-DD HH24:MI TZH:TZM') as NEXT_START, to_char(LAST_START_DATE, 'YYYY-MM-DD HH24:MI TZH:TZM') as LAST_START from DBA_SCHEDULER_WINDOWS order by WINDOW_NAME")

				+ textTable(conn, "Scheduler jobs (DBA_SCHEDULER_JOBS)\n--## FAILURE_COUNT = how many times the job has failed since it was created",
					"select OWNER, JOB_NAME, ENABLED, STATE, substr(REPEAT_INTERVAL, 1, 60) as REPEAT_INTERVAL, to_char(LAST_START_DATE, 'YYYY-MM-DD HH24:MI TZH:TZM') as LAST_START, to_char(LAST_RUN_DURATION) as LAST_DURATION, to_char(NEXT_RUN_DATE, 'YYYY-MM-DD HH24:MI TZH:TZM') as NEXT_RUN, RUN_COUNT, FAILURE_COUNT from DBA_SCHEDULER_JOBS order by OWNER, JOB_NAME")

				+ textTable(conn, "Jobs of the old kind, DBMS_JOB (DBA_JOBS)\n--## BROKEN = Y: the job is not run any more (Oracle sets that after 16 failures)",
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
				+ textTable(conn, "Password rules per profile (DBA_PROFILES)"
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

				+ textTable(conn, "Who can connect AS SYSDBA and the like, with a password (V$PWFILE_USERS)",
					"select * from V$PWFILE_USERS")

				+ textTable(conn, "Who has the DBA role (DBA_ROLE_PRIVS)",
					"select GRANTEE, ADMIN_OPTION, DEFAULT_ROLE from DBA_ROLE_PRIVS where GRANTED_ROLE = 'DBA' order by GRANTEE")

				+ textTable(conn, "Accounts with '... ANY ...' system privileges granted directly, not through a role (DBA_SYS_PRIVS)\n--## An application account should normally not be in this list",
					"select s.GRANTEE, count(*) as ANY_PRIVILEGES from DBA_SYS_PRIVS s where s.PRIVILEGE like '% ANY %' and s.GRANTEE in (select u.USERNAME from DBA_USERS u) group by s.GRANTEE order by 2 desc, 1")

				+ textTable(conn, "Parameters for logins and auditing (V$SYSTEM_PARAMETER)",
					"select NAME, DISPLAY_VALUE as VALUE, ISDEFAULT from V$SYSTEM_PARAMETER where NAME in ('audit_trail', 'audit_sys_operations', 'sec_case_sensitive_logon', 'sec_max_failed_login_attempts', 'remote_login_passwordfile', 'remote_os_authent', 'os_authent_prefix', 'O7_DICTIONARY_ACCESSIBILITY', 'sql92_security')" + andOurContainer(conn) + "order by NAME")
				;
		}
	}

	//---------------------------------------------------------------------------------------------
	public static class Audit extends OracleTab
	{
		@Override public String getTabLabel() { return "Audit"; }
		@Override public String getName()     { return ConfigType.OraAudit.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			String out = ""
				+ textTable(conn, "How big the audit trail is (DBA_SEGMENTS)"
				              + "\n--## Oracle never removes audit records by itself: without a purge job (DBMS_AUDIT_MGMT) these tables grow for ever"
				              + "\n--## SYS.AUD$ = the traditional audit trail, SYS.FGA_LOG$ = fine grained auditing, AUDSYS = the unified audit trail (12.1 and above)",
					"select OWNER, case when OWNER = 'AUDSYS' then 'unified audit trail (all segments)' else SEGMENT_NAME end as AUDIT_TRAIL, TABLESPACE_NAME, count(*) as SEGMENTS, round(sum(BYTES) / 1048576, 1) as MB "
					+ "from DBA_SEGMENTS where (OWNER = 'SYS' and SEGMENT_NAME in ('AUD$', 'FGA_LOG$')) or OWNER = 'AUDSYS' "
					+ "group by OWNER, case when OWNER = 'AUDSYS' then 'unified audit trail (all segments)' else SEGMENT_NAME end, TABLESPACE_NAME order by 5 desc")

				+ textTable(conn, "Audit parameters (V$SYSTEM_PARAMETER)\n--## audit_trail = NONE: the traditional auditing is off (in 12.1 and above the unified auditing can still be on)",
					"select NAME, DISPLAY_VALUE as VALUE, ISDEFAULT from V$SYSTEM_PARAMETER where (NAME like 'audit%' or NAME like 'unified_audit%')" + andOurContainer(conn) + "order by NAME")

				+ textTable(conn, "Traditional auditing: which statements are audited (DBA_STMT_AUDIT_OPTS)",
					"select USER_NAME, AUDIT_OPTION, SUCCESS, FAILURE from DBA_STMT_AUDIT_OPTS order by USER_NAME, AUDIT_OPTION")

				+ textTable(conn, "Traditional auditing: which system privileges are audited (DBA_PRIV_AUDIT_OPTS)",
					"select USER_NAME, PRIVILEGE, SUCCESS, FAILURE from DBA_PRIV_AUDIT_OPTS order by USER_NAME, PRIVILEGE");

			// Unified auditing: 12.1 and above
			if (conn.getDbmsVersionInfo().getLongVersion() >= Ver.ver(12,1))
			{
				out += textTable(conn, "Unified auditing (V$OPTION)\n--## TRUE = only unified auditing ('pure' mode).  FALSE = mixed mode: both the traditional and the unified audit trail are written",
					"select PARAMETER, VALUE from V$OPTION where PARAMETER = 'Unified Auditing'");

				out += textTable(conn, "Unified auditing: the policies that are enabled (AUDIT_UNIFIED_ENABLED_POLICIES)"
				                     + "\n--## NOTE: this view is NOT part of SELECT_CATALOG_ROLE. It needs: GRANT AUDIT_VIEWER TO <user>",
					"select * from AUDIT_UNIFIED_ENABLED_POLICIES");
			}

			return out;
		}
	}

	//---------------------------------------------------------------------------------------------
	public static class ResourceManager extends OracleTab
	{
		@Override public String getTabLabel() { return "Resource Manager"; }
		@Override public String getName()     { return ConfigType.OraResourceManager.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			DbmsVersionInfoOracle oraVersionInfo = (DbmsVersionInfoOracle) conn.getDbmsVersionInfo();

			// The limit on CPU usage got a new column name in 12.1
			String utilizationLimit = oraVersionInfo.getLongVersion() >= Ver.ver(12,1) ? "UTILIZATION_LIMIT" : "MAX_UTILIZATION_LIMIT as UTILIZATION_LIMIT";

			String out = ""
				+ textTable(conn, "The resource plan that is active right now (V$RSRC_PLAN)"
				              + "\n--## INTERNAL_PLAN = no plan is set: Oracle does not limit anybody"
				              + "\n--## A plan like DEFAULT_MAINTENANCE_PLAN is switched in by the nightly maintenance window",
					"select NAME, IS_TOP_PLAN, CPU_MANAGED, INSTANCE_CAGING, PARALLEL_EXECUTION_MANAGED from V$RSRC_PLAN")

				+ textTable(conn, "Parameters (V$SYSTEM_PARAMETER)\n--## Instance caging = 'cpu_count' is set AND a resource plan is active: then the instance uses at most that many CPUs",
					"select NAME, DISPLAY_VALUE as VALUE, ISDEFAULT from V$SYSTEM_PARAMETER where NAME in ('resource_manager_plan', 'resource_limit', 'cpu_count', 'parallel_max_servers', 'parallel_degree_policy')" + andOurContainer(conn) + "order by NAME");

			// In CDB$ROOT: how the CPU is shared between the PDBs
			if (oraVersionInfo.isCdbRoot())
			{
				out += textTable(conn, "CDB plans: how the resources are shared between the PDBs (DBA_CDB_RSRC_PLAN_DIRECTIVES)\n--## SHARES = the weight of the PDB.  UTILIZATION_LIMIT = max percent of the CPU",
					"select PLAN, PLUGGABLE_DATABASE, SHARES, UTILIZATION_LIMIT, PARALLEL_SERVER_LIMIT from DBA_CDB_RSRC_PLAN_DIRECTIVES order by PLAN, PLUGGABLE_DATABASE");
			}

			out += ""
				+ textTable(conn, "Plans (DBA_RSRC_PLANS)",
					"select PLAN, CPU_METHOD, STATUS, MANDATORY, substr(COMMENTS, 1, 80) as COMMENTS from DBA_RSRC_PLANS order by PLAN")

				+ textTable(conn, "What each plan gives each consumer group (DBA_RSRC_PLAN_DIRECTIVES)"
				              + "\n--## MGMT_P1..P3 = share of the CPU at level 1..3.  UTILIZATION_LIMIT = max percent of the CPU.  ACTIVE_SESS_POOL_P1 = max active sessions, the rest are queued",
					"select PLAN, GROUP_OR_SUBPLAN, TYPE, MGMT_P1, MGMT_P2, MGMT_P3, " + utilizationLimit + ", ACTIVE_SESS_POOL_P1, PARALLEL_DEGREE_LIMIT_P1, SWITCH_GROUP, SWITCH_TIME, MAX_IDLE_TIME from DBA_RSRC_PLAN_DIRECTIVES order by PLAN, GROUP_OR_SUBPLAN")

				+ textTable(conn, "Consumer groups (DBA_RSRC_CONSUMER_GROUPS)",
					"select CONSUMER_GROUP, STATUS, MANDATORY, substr(COMMENTS, 1, 80) as COMMENTS from DBA_RSRC_CONSUMER_GROUPS order by CONSUMER_GROUP")

				+ textTable(conn, "Which sessions goes to which consumer group (DBA_RSRC_GROUP_MAPPINGS)",
					"select ATTRIBUTE, VALUE, CONSUMER_GROUP, STATUS from DBA_RSRC_GROUP_MAPPINGS order by ATTRIBUTE, VALUE");

			return out;
		}
	}

	//---------------------------------------------------------------------------------------------
	public static class DataGuard extends OracleTab
	{
		@Override public String getTabLabel() { return "Data Guard"; }
		@Override public String getName()     { return ConfigType.OraDataGuard.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			// NOTE: This tab has only been seen on databases WITHOUT a standby (there most of the parts are empty). The SQL is the same from 11.2
			return ""
				+ textTable(conn, "Role and protection (V$DATABASE)"
				              + "\n--## DATABASE_ROLE = PRIMARY, PHYSICAL STANDBY, LOGICAL STANDBY or SNAPSHOT STANDBY"
				              + "\n--## A database without a standby is a PRIMARY with PROTECTION_MODE = MAXIMUM PERFORMANCE"
				              + "\n--## FORCE_LOGGING should be YES on a primary that has a standby: otherwise NOLOGGING operations never reach the standby",
					"select DATABASE_ROLE, PROTECTION_MODE, PROTECTION_LEVEL, SWITCHOVER_STATUS, DATAGUARD_BROKER, FORCE_LOGGING, FLASHBACK_ON, GUARD_STATUS, LOG_MODE, OPEN_MODE from V$DATABASE")

				+ textTable(conn, "The databases in the Data Guard configuration (V$DATAGUARD_CONFIG)\n--## From the parameter 'log_archive_config'. Empty (or only this database) = no Data Guard configuration",
					"select * from V$DATAGUARD_CONFIG")

				+ textTable(conn, "Standby destinations: where the redo is sent (V$ARCHIVE_DEST)"
				              + "\n--## TRANSMIT_MODE: SYNCHRONOUS = a commit waits for the standby.  ASYNCHRONOUS = it does not"
				              + "\n--## How far behind each standby is: see the tab 'Data Guard' among the Performance Counters",
					"select DEST_ID, DEST_NAME, STATUS, DESTINATION, DB_UNIQUE_NAME, TRANSMIT_MODE, AFFIRM, VALID_TYPE, VALID_ROLE, DELAY_MINS, REOPEN_SECS, NET_TIMEOUT, COMPRESSION from V$ARCHIVE_DEST where TARGET = 'STANDBY' and STATUS <> 'INACTIVE' order by DEST_ID")

				+ textTable(conn, "Standby redo logs (V$STANDBY_LOG)"
				              + "\n--## Needed on a standby for real time apply, and on a primary so it is ready to become a standby"
				              + "\n--## Rule of thumb: the same size as the online redo logs, and one more group per thread",
					"select GROUP#, THREAD#, SEQUENCE#, CAST(BYTES / 1048576 as NUMBER(18,1)) as SIZE_MB, USED, ARCHIVED, STATUS from V$STANDBY_LOG order by THREAD#, GROUP#")

				+ textTable(conn, "Parameters (V$SYSTEM_PARAMETER)",
					"select NAME, DISPLAY_VALUE as VALUE, ISDEFAULT from V$SYSTEM_PARAMETER where (NAME in ('db_unique_name', 'log_archive_config', 'fal_server', 'fal_client', 'standby_file_management', 'dg_broker_start', 'archive_lag_target', 'log_archive_max_processes', 'db_file_name_convert', 'log_file_name_convert', 'remote_login_passwordfile', 'redo_transport_user') or (NAME like 'log_archive_dest%' and VALUE is not null and ISDEFAULT = 'FALSE'))" + andOurContainer(conn) + "order by NAME")

				+ textTable(conn, "The last messages from Data Guard and the archiver (V$DATAGUARD_STATUS)\n--## The newest first, max 30. ERROR_CODE = the ORA- error, 0 = no error",
					"select * from (select TIMESTAMP, FACILITY, SEVERITY, DEST_ID, ERROR_CODE, MESSAGE from V$DATAGUARD_STATUS order by MESSAGE_NUM desc) where ROWNUM <= 30");
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
				+ textTable(conn, "Containers (V$CONTAINERS)\n--## A PDB in OPEN_MODE 'MOUNTED' is closed: nobody can connect to it\n--## When connected to a PDB: only that PDB is shown",
					"select CON_ID, NAME, to_char(DBID) as DBID, OPEN_MODE, RESTRICTED, to_char(OPEN_TIME, 'YYYY-MM-DD HH24:MI:SS') as OPEN_TIME, round(TOTAL_SIZE / 1048576) as TOTAL_MB from V$CONTAINERS order by CON_ID")

				+ textTable(conn, "Pluggable Databases (CDB_PDBS)",
					"select PDB_ID, PDB_NAME, STATUS from CDB_PDBS order by PDB_ID")

				+ textTable(conn, "Saved states: the PDBs that are opened by themselves when the instance starts (CDB_PDB_SAVED_STATES)"
				                       + "\n--## A PDB that is NOT in this list stays MOUNTED (closed) after a restart of the instance."
				                       + "\n--## To put it in the list: ALTER PLUGGABLE DATABASE <name> SAVE STATE (when it's open)",
					"select CON_NAME, INSTANCE_NAME, STATE from CDB_PDB_SAVED_STATES order by CON_NAME");

			// From CDB$ROOT we see the parameters of all containers
			if (oraVersionInfo.isCdbRoot())
			{
				out += textTable(conn, "Parameters where a PDB has another value than the CDB (V$SYSTEM_PARAMETER)\n--## CDB_VALUE is the value for the whole CDB (CON_ID 0). Note: sga_target = 0 in a PDB means 'no limit for this PDB'",
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
		@Override public String getTabLabel() { return "DB Links and Directories"; }
		@Override public String getName()     { return ConfigType.OraDbLinks.toString(); }

		@Override
		protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
		{
			return ""
				+ textTable(conn, "Database links: the other databases that this one connects to (DBA_DB_LINKS)\n--## OWNER 'PUBLIC' = every user can use the link.  USERNAME = the account it logs in with on the other side (empty = the same user as here)",
					"select OWNER, DB_LINK, USERNAME, HOST, to_char(CREATED, 'YYYY-MM-DD') as CREATED from DBA_DB_LINKS order by OWNER, DB_LINK")

				+ textTable(conn, "Directories: the folders on the server that the database is allowed to read and write (DBA_DIRECTORIES)\n--## Used by Data Pump, external tables, UTL_FILE. Most of them comes with Oracle.",
					"select OWNER, DIRECTORY_NAME, DIRECTORY_PATH from DBA_DIRECTORIES order by DIRECTORY_NAME")
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
				+ textTable(conn, "The database (NLS_DATABASE_PARAMETERS)\n--## This is what the database was created with. NLS_CHARACTERSET is the character set of the data: it can't be changed with a parameter.",
					"select PARAMETER, VALUE from NLS_DATABASE_PARAMETERS order by PARAMETER")

				+ textTable(conn, "The instance (NLS_INSTANCE_PARAMETERS)\n--## What is set with initialization parameters. A client that sets its own language and territory (most do) overrides these for its session.",
					"select PARAMETER, VALUE from NLS_INSTANCE_PARAMETERS order by PARAMETER")
				;
		}
	}
}
