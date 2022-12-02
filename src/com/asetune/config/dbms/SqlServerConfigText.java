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
package com.asetune.config.dbms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.AlarmEventCertificateExpiry;
import com.asetune.cm.sqlserver.CmSummary;
import com.asetune.config.dbms.DbmsConfigIssue.Severity;
import com.asetune.config.dict.SqlServerTraceFlagsDictionary;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.sql.conn.info.DbmsVersionInfoSqlServer;
import com.asetune.utils.SqlServerUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

public abstract class SqlServerConfigText
{
	/** What sub types exists */
	public enum ConfigType
	{
		 SqlServerHelpDb
		,SqlServerSysDatabases
		,SqlServerSysMasterFiles
		,SqlServerTempdb
		,SqlServerTraceflags
		,SqlServerLinkedServers
		,SqlServerHelpSort
		,SqlServerHostInfo
		,SqlServerSysInfo
		,SqlServerSuspectPages
		,SqlServerCertificates
		,SqlServerServices
		,SqlServerResourceGovernor
		,SqlServerServerRegistry
		,SqlServerClusterNodes
		,SqlServerAgentJobs
		};

	/** Log4j logging. */
	private static Logger _logger = Logger.getLogger(SqlServerConfigText.class);

	public static void createAndRegisterAllInstances()
	{
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.HelpDb());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.SysDatabases());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.SysMasterFiles());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.Tempdb());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.Traceflags());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.LinkedServers());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.HelpSort());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.HostInfo());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.SysInfo());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.SuspectPages());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.SqlServices());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.Certificates());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.ResourceGovernor());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.ServerRegistry());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.ClusterNodes());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.AgentJobs());
	}

	/*-----------------------------------------------------------
	**-----------------------------------------------------------
	**-----------------------------------------------------------
	**----- SUB CLASSES ----- SUB CLASSES ----- SUB CLASSES -----
	**-----------------------------------------------------------
	**-----------------------------------------------------------
	**-----------------------------------------------------------
	*/
	public static class HelpDb extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                          { return "sp_helpdb"; }
		@Override public    String     getName()                              { return ConfigType.SqlServerHelpDb.toString(); }
		@Override public    String     getConfigType()                        { return getName(); }
//		@Override protected String     getSqlCurrentConfig(long srvVersion)   { return "exec sp_helpdb"; }
		@Override protected String     getSqlCurrentConfig(DbmsVersionInfo v) { return "exec sp_helpdb"; }
		
		@Override
		public String checkRequirements(DbxConnection conn)
		{
			DbmsVersionInfoSqlServer versionInfo = (DbmsVersionInfoSqlServer) conn.getDbmsVersionInfo();

			// In Azure Database, skip this 
			if (versionInfo.isAzureDb() || versionInfo.isAzureSynapseAnalytics())
				return "sp_helpdb: is NOT supported in Azure SQL Database.";

			return super.checkRequirements(conn);
		}
		
		/** Check 'compatibility_level' for all databases */
		@Override
		public void checkConfig(DbxConnection conn)
		{
			// no nothing, if we havn't got an instance
			if ( ! DbmsConfigManager.hasInstance() )
				return;

			String    srvName    = "-UNKNOWN-";
			Timestamp srvRestart = null;
			try { srvName    = conn.getDbmsServerName();          } catch (SQLException ex) { _logger.info("Problems getting SQL-Server instance name. ex="+ex);}
			try { srvRestart = SqlServerUtils.getStartDate(conn); } catch (SQLException ex) { _logger.info("Problems getting SQL-Server start date. ex="+ex);}

			// Get databaseName and compatibility_level for databases that are below the current SQL-Server's compatibility_level
			String sql = ""
					  // get server level compatibility_level
					+ "declare @compatLevelTmp smallint \n"
					+ "select @compatLevelTmp = compatibility_level from sys.databases where name = 'tempdb' \n"
					
					  // get what databases that are below the servers default, exclude some system databases 
					+ "select name, compatibility_level, @compatLevelTmp as srvCompatLevel \n"
					+ "from sys.databases \n"
					+ "where compatibility_level < @compatLevelTmp \n"
					+ "  and name not in('msdb') \n"
					+ "";

			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
				{
					String dbname         = rs.getString(1);
					int    dbCompatlevel  = rs.getInt   (2);
					int    srvCompatLevel = rs.getInt   (3);
					
					String key = "DbmsConfigIssue." + srvName + "." + dbname + ".compatibility_level";
					
					DbmsConfigIssue issue = new DbmsConfigIssue(srvRestart, key, "compatibility_level", Severity.WARNING, 
							"The 'compatibility_level=" + dbCompatlevel + "' for database '" + dbname + "' is lower than the SQL-Servers default level (" + srvCompatLevel + "). New performance improvements will NOT be used.", 
							"Fix this using: ALTER DATABASE [" + dbname + "] SET COMPATIBILITY_LEVEL = " + srvCompatLevel);

					DbmsConfigManager.getInstance().addConfigIssue(issue);
				}
			}
			catch (SQLException ex)
			{
				_logger.error("Problems getting SQL-Server 'compatibility_level', using sql '"+sql+"'. Caught: "+ex, ex);
			}
		}
	}

	public static class SysDatabases extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                        { return "sys.databases"; }
		@Override public    String     getName()                            { return ConfigType.SqlServerSysDatabases.toString(); }
		@Override public    String     getConfigType()                      { return getName(); }
//		@Override protected String     getSqlCurrentConfig(long srvVersion) 
		@Override protected String     getSqlCurrentConfig(DbmsVersionInfo v)
		{
			DbmsVersionInfoSqlServer versionInfo = (DbmsVersionInfoSqlServer) v;

			String sql = 
					"select has_dbaccess(name) AS has_dbaccess, * from sys.databases \n" +
					"go \n" + 
					"print '' \n" + 
					"print '' \n" + 
					"print '' \n" + 
					"print '===============================================================================' \n" + 
					"print ' Below are: Database Scoped Configurations for each database in the SQL-Server' \n" + 
					"print '-------------------------------------------------------------------------------' \n" + 
					"print '' \n" + 
					"go \n" +
					"";

			if (versionInfo.isAzureDb() || versionInfo.isAzureSynapseAnalytics())
			{
				sql += "" +
						"select db_name() as dbname, * from sys.database_scoped_configurations \n" +
						"go \n" +
						"";
			}
			else
			{
				sql += "" +
					"exec sys.sp_MSforeachdb 'select convert(varchar(60),''?'') as dbname, * from sys.database_scoped_configurations' \n" +
					"go \n" +
					"";
			}
			return sql; 
			
			// Should we somewhere indicate DEFAILT values
			// NOTE: Table 'database_scoped_configurations' has a column 'is_value_default' which does this for us :)
			// 
			//  id ConfigName                           defVal    defValOnSecondary
			//  -- ------------------------------------ --------- -------------------
			//   1 MAXDOP                               0         NULL
			//   2 LEGACY_CARDINALITY_ESTIMATION        0         NULL
			//   3 PARAMETER_SNIFFING                   1         NULL
			//   4 QUERY_OPTIMIZER_HOTFIXES             0         NULL
			//   6 IDENTITY_CACHE                       1         NULL
			//   7 INTERLEAVED_EXECUTION_TVF            1         NULL
			//   8 BATCH_MODE_MEMORY_GRANT_FEEDBACK     1         NULL
			//   9 BATCH_MODE_ADAPTIVE_JOINS            1         NULL
			//  10 TSQL_SCALAR_UDF_INLINING             1         NULL
			//  11 ELEVATE_ONLINE                       OFF       NULL
			//  12 ELEVATE_RESUMABLE                    OFF       NULL
			//  13 OPTIMIZE_FOR_AD_HOC_WORKLOADS        0         NULL
			//  14 XTP_PROCEDURE_EXECUTION_STATISTICS   0         NULL
			//  15 XTP_QUERY_EXECUTION_STATISTICS       0         NULL
			//  16 ROW_MODE_MEMORY_GRANT_FEEDBACK       1         NULL
			//  17 ISOLATE_SECURITY_POLICY_CARDINALITY  0         NULL
			//  18 BATCH_MODE_ON_ROWSTORE               1         NULL
			//  19 DEFERRED_COMPILATION_TV              1         NULL
			//  20 ACCELERATED_PLAN_FORCING             1         NULL
			//  21 GLOBAL_TEMPORARY_TABLE_AUTO_DROP     1         NULL
			//  22 LIGHTWEIGHT_QUERY_PROFILING          1         NULL
			//  23 VERBOSE_TRUNCATION_WARNINGS          1         NULL
			//  24 LAST_QUERY_PLAN_STATS                0         NULL
		}
	}

	public static class SysMasterFiles extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                        { return "sys.master_files"; }
		@Override public    String     getName()                            { return ConfigType.SqlServerSysMasterFiles.toString(); }
		@Override public    String     getConfigType()                      { return getName(); }
//		@Override protected String     getSqlCurrentConfig(long srvVersion) 
		@Override protected String     getSqlCurrentConfig(DbmsVersionInfo v) 
		{ 
			String sql1 = "select dbname = db_name(database_id), SizeInMb = size/128.0, * from sys.master_files";
			
			String sql2 = ""
				    + "SELECT DISTINCT \n"
				    + "     ovs.logical_volume_name            AS VolumeName \n"
				    + "    ,ovs.volume_mount_point             AS MountPoint \n"
				    + "    ,CAST(ovs.available_bytes/1024.0/1024.0 as numeric(25,1)) AS FreeSpaceMb \n"
				    + "    ,CAST(ovs.total_bytes    /1024.0/1024.0 as numeric(25,1)) AS SizeMb \n"
				    + "    ,CAST((ovs.available_bytes*1.0 / ovs.total_bytes*1.0) * 100.0 as numeric(6,1)) AS UsedPct \n"
				    + "FROM sys.master_files mf \n"
				    + "CROSS APPLY sys.dm_os_volume_stats(mf.database_id, mf.FILE_ID) ovs; \n"
				    + "";

			return ""
			     + sql1
			     + "\ngo\n"
			     + sql2
			     + "\ngo\n"
			     ;
		}
	}

	public static class Tempdb extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                        { return "tempdb"; }
		@Override public    String     getName()                            { return ConfigType.SqlServerTempdb.toString(); }
		@Override public    String     getConfigType()                      { return getName(); }
//		@Override protected String     getSqlCurrentConfig(long srvVersion) 
		@Override protected String     getSqlCurrentConfig(DbmsVersionInfo v)
		{
			DbmsVersionInfoSqlServer versionInfo = (DbmsVersionInfoSqlServer) v;

			// ---------------------------------------------------------
			// WARNING: do not use 'sys.master_files' instead use 'tempdb.sys.database_files'
			//          master_files only shows INITIAL SIZE and not the "actual" to which it has been grown to
			// ---------------------------------------------------------
			String sql = "";
			
			if (versionInfo.isAzureDb() || versionInfo.isAzureSynapseAnalytics())
			{
				// Should we do something special here...
			}
			else
			{
				sql += ""
					+ "print '--#######################################' \n"
					+ "print '--## sp_helpdb tempdb' \n"
					+ "print '--#######################################' \n"
				    + "exec sp_helpdb tempdb \n"
				    + "go \n"
				    + "";
			}

			// For all versions
			sql += ""
				+ "print '' \n"
			    + "print '--#######################################' \n"
				+ "print '--## Summary Pages and MB' \n"
				+ "print '--#######################################' \n"
			    + "SELECT \n"
			    + "      type     = ISNULL(type_desc, 'TOTAL_SIZE') \n"
			    + "     ,size_mb  = cast(SUM(size) / 128.0 as numeric(12,1)) \n"
			    + "     ,size_pgs = SUM(size) \n"
			    + "FROM tempdb.sys.database_files \n"
			    + "GROUP BY GROUPING SETS ( (type_desc), () ) \n"
			    + "go \n"

				+ "print '' \n"
				+ "print '--#######################################' \n"
				+ "print '--## tempdb LOG files. There should be 1 transaction log file in total.' \n"
				+ "print '--#######################################' \n"
			    + "SELECT \n"
			    + "      file_id \n"
			    + "     ,type_desc \n"
			    + "     ,data_space_id \n"
			    + "     ,name \n"
			    + "     ,physical_name \n"
			    + "     ,size_pgs     = size \n"
			    + "     ,size_mb      = cast(size / 128.0 as numeric(12,1)) \n"
			    + "     ,max_size_pgs = max_size \n"
			    + "     ,max_size_mb  = cast(nullif(max_size, -1) / 128.0 as numeric(12,1)) \n"
			    + "     ,growth_pgs   = growth \n"
			    + "     ,growth_mb    = cast(growth / 128.0 as numeric(12,1)) \n"
			    + "FROM tempdb.sys.database_files \n"
			    + "WHERE type_desc = 'LOG' \n"
				+ "go \n"

				+ "print '' \n"
				+ "print '--#######################################' \n"
				+ "print '--## tempdb DATA files. There should be 1 data file per scheduler/core, so 4 schedulers 4 tempdb data files.' \n"
				+ "print '--#######################################' \n"
			    + "SELECT \n"
			    + "      file_id \n"
			    + "     ,type_desc \n"
			    + "     ,data_space_id \n"
			    + "     ,name \n"
			    + "     ,physical_name \n"
			    + "     ,size_pgs     = size \n"
			    + "     ,size_mb      = cast(size / 128.0 as numeric(12,1)) \n"
			    + "     ,max_size_pgs = max_size \n"
			    + "     ,max_size_mb  = cast(nullif(max_size, -1) / 128.0 as numeric(12,1)) \n"
			    + "     ,growth_pgs   = growth \n"
			    + "     ,growth_mb    = cast(growth / 128.0 as numeric(12,1)) \n"
			    + "FROM tempdb.sys.database_files \n"
			    + "WHERE type_desc = 'ROWS' \n"
				+ "go \n"
				+ "\n"

				+ "print '' \n"
				+ "print '--#######################################' \n"
				+ "print '--## Scheduler count from: sys.dm_os_schedulers' \n"
				+ "print '--#######################################' \n"
				+ "select scheduler_count = count(*) from sys.dm_os_schedulers where status = 'VISIBLE ONLINE' \n"
				+ "go \n"

				+ "print '' \n"
				+ "print '--#######################################' \n"
				+ "print '--## Core count from: sys.dm_os_sys_info' \n"
				+ "print '--#######################################' \n"
				+ "select cpu_count from sys.dm_os_sys_info \n"
				+ "go \n"
				+ "";

			if (versionInfo.isAzureDb() || versionInfo.isAzureSynapseAnalytics())
			{
				// Should we do something special here...
			}
			else
			{
//				if (srvVersion < Ver.ver(2019))
				if (versionInfo.getLongVersion() < Ver.ver(2019))
				{
					sql += ""
						+ "\n"
						+ "print '' \n"
						+ "print '--#######################################' \n"
						+ "print '--## Traceflags - that is good to enable in PRE SQL Server 2019.' \n"
						+ "print '--## 1118 - Full Extents Only' \n"
						+ "print '--## 1117 - Grow All Files in a FileGroup Equally' \n"
						+ "print '--## 2453 - Improve Table Variable Performance -- Allows table variables to trigger recompile (above: 2012 SP2, 2014 CU3, 2016 RTM)' \n"
						+ "print '--#######################################' \n"
						+ "go \n"
						+ "DBCC TRACESTATUS (1118, -1) WITH NO_INFOMSGS -- Full Extents Only (in PRE 2019) \n"
						+ "go \n"
						+ "DBCC TRACESTATUS (1117, -1) WITH NO_INFOMSGS -- Grow All Files in a FileGroup Equally (in PRE 2019) \n"
						+ "go \n"
						+ "DBCC TRACESTATUS (2453, -1) WITH NO_INFOMSGS -- Improve Table Variable Performance -- Allows table variables to trigger recompile (in PRE 2019) \n"
						+ "go \n"
						+ "";
				}
			}

			return sql; 
		}

		/** 
		 * Check: 
		 *  - Number of tempdb files (LOG) 
		 *  - Number of tempdb files (ROW) 
		 *  - Number of cores (or schedulers ONLINE)
		 *  - compare cores and number or ROW - files
		 *  - Trace flags: 1117, 1118 (if version is below 2019) 
		 * */
		@Override
		public void checkConfig(DbxConnection conn)
		{
			// no nothing, if we havn't got an instance
			if ( ! DbmsConfigManager.hasInstance() )
				return;

			int numOfOnlineSchedulers = -1;
			int tempdbRowsFileCount   = -1;
		//	int tempdbLogFileCount    = -1;

			// Do not do this check for Azure SQL Database
//			DbmsVersionInfoSqlServer versionInfo = (DbmsVersionInfoSqlServer) conn.getDbmsVersionInfo();
//			if (versionInfo.isAzureDb() || versionInfo.isAzureSynapseAnalytics())
//				return;

			String    srvName    = "-UNKNOWN-";
			Timestamp srvRestart = null;
			try { srvName    = conn.getDbmsServerName();          } catch (SQLException ex) { _logger.info("Problems getting SQL-Server instance name. ex="+ex);}
			try { srvRestart = SqlServerUtils.getStartDate(conn); } catch (SQLException ex) { _logger.info("Problems getting SQL-Server start date. ex="+ex);}

			//---------------------------------------------------------------------------
			// Check for match of "data file count" and "cores"
			//---------------------------------------------------------------------------
			String sql = ""
					+ "DECLARE @numOfOnlineSchedulers int \n"
					+ "DECLARE @tempdbRowsFileCount   int \n"
					+ "DECLARE @tempdbLogFileCount    int \n"

					// get: cores OR online-schedulers
					+ "SELECT @numOfOnlineSchedulers = COUNT(*) FROM sys.dm_os_schedulers WHERE status = 'VISIBLE ONLINE' \n"
					
					// get: tempdb files
					+ "SELECT @tempdbRowsFileCount = COUNT(*) FROM tempdb.sys.database_files WHERE type_desc = 'ROWS' \n"
					+ "SELECT @tempdbLogFileCount  = COUNT(*) FROM tempdb.sys.database_files WHERE type_desc = 'LOG' \n"
					
					  // get what databases that are below the servers default, exclude some system databases 
					+ "SELECT  \n"
					+ "     numOfOnlineSchedulers = @numOfOnlineSchedulers \n"
					+ "    ,tempdbRowsFileCount   = @tempdbRowsFileCount \n"
					+ "    ,tempdbLogFileCount    = @tempdbLogFileCount \n"
					+ "";

			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
				{
					numOfOnlineSchedulers = rs.getInt(1);
					tempdbRowsFileCount   = rs.getInt(2);
				//	tempdbLogFileCount    = rs.getInt(3);

//					if (tempdbRowsFileCount < numOfOnlineSchedulers && tempdbRowsFileCount <= 8)
					if (tempdbRowsFileCount < Math.min(8, numOfOnlineSchedulers)) // 8 should be considered as MAX (if we got more than 8 cores, the file count do not really matter)
					{
						String key = "DbmsConfigIssue." + srvName + ".tempdb.filecount_vs_schedulers";
						
						DbmsConfigIssue issue = new DbmsConfigIssue(srvRestart, key, "tempdb_data_files", Severity.INFO, 
								"SQL Server 'tempdb' number of data files (" + tempdbRowsFileCount + ") is lower than schedulers/cores (" + numOfOnlineSchedulers + "). This may lead to latch contention on PFS/GAM/SGAM pages and improper tempdb performance.", 
								"Fix this using: google 'sql server tempdb multiple files'");

						DbmsConfigManager.getInstance().addConfigIssue(issue);
					}
				}
			}
			catch (SQLException ex)
			{
				_logger.error("Problems getting SQL-Server 'tempdb' settings, using sql '"+sql+"'. Caught: "+ex, ex);
			}

			//---------------------------------------------------------------------------
			// If we HAVE multiple tempdb files, they should all be of the same size
			//---------------------------------------------------------------------------
			if (tempdbRowsFileCount > 1)
			{
				sql = ""
						+ "SELECT \n"
						+ "      file_id \n"
						+ "     ,type_desc \n"
						+ "     ,data_space_id \n"
						+ "     ,name \n"
						+ "     ,physical_name \n"
						+ "     ,size \n"
						+ "     ,size_mb = size / 128 \n"
						+ "     ,max_size \n"
						+ "     ,growth \n"
						+ "FROM tempdb.sys.database_files \n"
						+ "WHERE type_desc = 'ROWS' \n"
						+ "";

				try
				{
					int prevFileSizeMb = -1;
					int prevGrowth     = -1;

					int diffCountFileSize = 0;
					int diffCountGrowth = 0;

					ResultSetTableModel tempdbRowsRstm = ResultSetTableModel.executeQuery(conn, sql, "tempdb data files");
					if ( tempdbRowsRstm.hasRows() )
					{
						for (int r=0; r<tempdbRowsRstm.getRowCount(); r++)
						{
							int fileSize = tempdbRowsRstm.getValueAsInteger(r, "size_mb");
							int growth   = tempdbRowsRstm.getValueAsInteger(r, "growth");

							// No need to check FIRST row
							if (prevFileSizeMb != -1)
							{
								if (prevFileSizeMb != fileSize) diffCountFileSize++;
								if (prevGrowth     != growth  ) diffCountGrowth++;
							}

							prevFileSizeMb = fileSize;
							prevGrowth     = growth;
						}
					}
					
					if (diffCountFileSize > 0)
					{
						int maxSizeMb = 0;
						String ddlFix = "";
						
						List<String> list = new ArrayList<>();
						for (int r=0; r<tempdbRowsRstm.getRowCount(); r++)
						{
							String name = tempdbRowsRstm.getValueAsString(r, "name");
							int    val  = tempdbRowsRstm.getValueAsInteger(r, "size_mb");
							
							maxSizeMb = Math.max(val, maxSizeMb);

							list.add(name + "=" + val + " MB");
						}

						// Compose DDL statement to set all tempdb files to same size
						int smallestMbUnit = 1024; // 1GB
						int newSizeMb = smallestMbUnit - (maxSizeMb % smallestMbUnit) + maxSizeMb;
						for (int r=0; r<tempdbRowsRstm.getRowCount(); r++)
						{
							String name = tempdbRowsRstm.getValueAsString(r, "name");
							
							ddlFix += "ALTER DATABASE tempdb MODIFY FILE (NAME = " + name + ", SIZE = " + newSizeMb + "MB); ";
						}

						String key = "DbmsConfigIssue." + srvName + ".tempdb.datafiles_equal_size";
						
						DbmsConfigIssue issue = new DbmsConfigIssue(srvRestart, key, "tempdb_datafiles_equal_size", Severity.INFO, 
								"SQL Server 'tempdb' datafiles are NOT equally sized. This will lead to un-evenly usage or 'monopolizing' the largest datafile(s) and therefore may lead to latch contention on PFS/GAM/SGAM pages and improper tempdb performance. FileSizes=" + list, 
								ddlFix);

						DbmsConfigManager.getInstance().addConfigIssue(issue);
					}

					if (diffCountGrowth > 0)
					{
						List<String> list = new ArrayList<>();
						for (int r=0; r<tempdbRowsRstm.getRowCount(); r++)
						{
							String name = tempdbRowsRstm.getValueAsString(r, "name");
							int    val  = tempdbRowsRstm.getValueAsInteger(r, "growth");

							list.add(name + "=" + val);
						}

						String key = "DbmsConfigIssue." + srvName + ".tempdb.datafiles_equal_grow_size";
						
						DbmsConfigIssue issue = new DbmsConfigIssue(srvRestart, key, "tempdb_datafiles_equal_grow_size", Severity.INFO, 
								"SQL Server 'tempdb' datafiles 'auto grow size' are NOT equally sized. This will lead to un-evenly usage or 'monopolizing' the largest datafile(s) and therefore may lead to latch contention on PFS/GAM/SGAM pages and improper tempdb performance. GrowSizes=" + list, 
								"Fix this using: google 'sql server tempdb multiple files'");

						DbmsConfigManager.getInstance().addConfigIssue(issue);
					}
				}
				catch (Exception ex)
				{
					_logger.error("Problems getting SQL-Server 'tempdb data files', using sql '"+sql+"'. Caught: "+ex, ex);
				}
			}

			//---------------------------------------------------------------------------
			// If we HAVE multiple tempdb files, check for traceflag: 1117 & 1118 (if Version is below 2019)
			// 1118 (Full Extents Only), 1117 (Grow All Files in a FileGroup Equally)
			//---------------------------------------------------------------------------
			if (tempdbRowsFileCount > 1)
			{
				// FIXME: 
			}
		}
	}

	public static class Traceflags extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                          { return "traceflags"; }
		@Override public    String     getName()                              { return ConfigType.SqlServerTraceflags.toString(); }
		@Override public    String     getConfigType()                        { return getName(); }
//		@Override protected String     getSqlCurrentConfig(long srvVersion)   { return "DBCC TRACESTATUS(-1) WITH NO_INFOMSGS"; }
		@Override protected String     getSqlCurrentConfig(DbmsVersionInfo v) { return "DBCC TRACESTATUS(-1) WITH NO_INFOMSGS"; }

		@Override
		public void refresh(DbxConnection conn, Timestamp ts)
		throws SQLException
		{
			if ( isOffline() )
			{
				super.refresh(conn, ts);
				return;
			}
			
			// ONLINE: 
			// - Refresh to get config using normal method
			// - if we get result: enrich every TraceFlag Number with a description 

			StringBuilder sb = new StringBuilder();

			super.refresh(conn, ts);
			
			String superConfigStr = getConfig(); 

			// If we HAVE GOT anything... then get details...
			if (StringUtil.hasValue(superConfigStr))
			{
				// Reuse the config from "super"... and then get the TraceFlags again, and add a desription message
				sb.append(superConfigStr);
				
				//  1> DBCC TRACESTATUS WITH NO_INFOMSGS
	            //  RS> Col# Label     JDBC Type Name          Guessed DBMS type Source Table
	            //  RS> ---- --------- ----------------------- ----------------- ------------
	            //  RS> 1    TraceFlag java.sql.Types.SMALLINT smallint          -none-      
	            //  RS> 2    Status    java.sql.Types.SMALLINT smallint          -none-      
	            //  RS> 3    Global    java.sql.Types.SMALLINT smallint          -none-      
	            //  RS> 4    Session   java.sql.Types.SMALLINT smallint          -none-      
	            //  +---------+------+------+-------+
	            //  |TraceFlag|Status|Global|Session|
	            //  +---------+------+------+-------+
	            //  |3604     |1     |0     |1      |
	            //  +---------+------+------+-------+
	            //  (1 rows affected)

				// Get the SQL to execute.
				//long         srvVersion = conn.getDbmsVersionNumber();
				String sql = "DBCC TRACESTATUS(-1) WITH NO_INFOMSGS"; //getSqlCurrentConfig(srvVersion);

				ResultSetTableModel rstm = null;
				
				sql = conn.quotifySqlString(sql);
				try ( Statement stmnt = conn.createStatement() )
				{
					// 10 seconds timeout
					stmnt.setQueryTimeout(10);
					try ( ResultSet rs = stmnt.executeQuery(sql) )
					{
						rstm = new ResultSetTableModel(rs, "traceflags");

						// build the descriptive table 
						sb.append("\n\n");
						sb.append("===========================================================================\n");
						sb.append("Explanation of the active trace flags:\n");
						sb.append("---------------------------------------------------------------------------\n");
						for (int r=0; r<rstm.getRowCount(); r++)
						{
							int traceflag  = rstm.getValueAsInteger(r, 0); // 0 = first col
							String desc    = SqlServerTraceFlagsDictionary.getInstance().getDescription(traceflag);

							sb.append("###########################################################################\n");
							sb.append(desc);
						}
					}
				}
				catch(SQLException ex)
				{
					sb.append(StringUtil.exceptionToString(ex));
				}
			}
			else
			{
				sb.append("INFO: NO Trace Flags has been set.\n");
			}

			
			sb.append("\n\n");
			sb.append("===========================================================================\n");
			sb.append("Explanation of the trace flags was found here:\n");
			sb.append("---------------------------------------------------------------------------\n");
			sb.append("https://docs.microsoft.com/en-us/sql/t-sql/database-console-commands/dbcc-traceon-trace-flags-transact-sql \n");
			
			// Now set the config
			setConfig(sb.toString());
		}
	}

	public static class LinkedServers extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                          { return "sp_linkedservers"; }
		@Override public    String     getName()                              { return ConfigType.SqlServerLinkedServers.toString(); }
		@Override public    String     getConfigType()                        { return getName(); }
//		@Override protected String     getSqlCurrentConfig(long srvVersion)   { return "exec sp_linkedservers"; }
		@Override protected String     getSqlCurrentConfig(DbmsVersionInfo v) { return "exec sp_linkedservers"; }
		@Override
		public String checkRequirements(DbxConnection conn)
		{
			DbmsVersionInfoSqlServer versionInfo = (DbmsVersionInfoSqlServer) conn.getDbmsVersionInfo();

			// In Azure Database, skip this 
			if (versionInfo.isAzureDb() || versionInfo.isAzureSynapseAnalytics())
				return "sp_linkedservers: is NOT supported in Azure SQL Database.";

			return super.checkRequirements(conn);
		}
	}

	public static class HelpSort extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                          { return "sp_helpsort"; }
		@Override public    String     getName()                              { return ConfigType.SqlServerHelpSort.toString(); }
		@Override public    String     getConfigType()                        { return getName(); }
//		@Override protected String     getSqlCurrentConfig(long srvVersion)   { return "exec sp_helpsort   SELECT SERVERPROPERTY_Collation = convert(varchar(255), SERVERPROPERTY('Collation'))"; }
		@Override protected String     getSqlCurrentConfig(DbmsVersionInfo v) { return "exec sp_helpsort   SELECT SERVERPROPERTY_Collation = convert(varchar(255), SERVERPROPERTY('Collation'))"; }
	}

	public static class HostInfo extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                          { return "Host Info"; }
		@Override public    String     getName()                              { return ConfigType.SqlServerHostInfo.toString(); }
		@Override public    String     getConfigType()                        { return getName(); }
//		@Override protected String     getSqlCurrentConfig(long srvVersion)   { return "select * from sys.dm_os_host_info"; }
		@Override protected String     getSqlCurrentConfig(DbmsVersionInfo v) { return "select * from sys.dm_os_host_info"; }
		@Override public    long       needVersion()                          { return Ver.ver(2017); }
		@Override
		public String checkRequirements(DbxConnection conn)
		{
			DbmsVersionInfoSqlServer versionInfo = (DbmsVersionInfoSqlServer) conn.getDbmsVersionInfo();

			// In Azure Database, skip this 
			if (versionInfo.isAzureDb() || versionInfo.isAzureSynapseAnalytics())
				return "sys.dm_os_host_info: is NOT supported in Azure SQL Database.";

			return super.checkRequirements(conn);
		}
	}

	public static class SysInfo extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                          { return "System Info"; }
		@Override public    String     getName()                              { return ConfigType.SqlServerSysInfo.toString(); }
		@Override public    String     getConfigType()                        { return getName(); }
//		@Override protected String     getSqlCurrentConfig(long srvVersion)   { return "select * from sys.dm_os_sys_info"; }
		@Override protected String     getSqlCurrentConfig(DbmsVersionInfo v) { return "select * from sys.dm_os_sys_info"; }
	}

	public static class SuspectPages extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                          { return "Suspect Pages"; }
		@Override public    String     getName()                              { return ConfigType.SqlServerSuspectPages.toString(); }
		@Override public    String     getConfigType()                        { return getName(); }
		@Override protected String     getSqlCurrentConfig(DbmsVersionInfo v) { return CmSummary.getSql_suspectPageInfo(v); }

		/** 
		 * Check for 'suspect pages' 
		 */
		@Override
		public void checkConfig(DbxConnection conn)
		{
			// no nothing, if we havn't got an instance
			if ( ! DbmsConfigManager.hasInstance() )
				return;

			String    srvName    = "-UNKNOWN-";
			Timestamp srvRestart = null;
			try { srvName    = conn.getDbmsServerName();          } catch (SQLException ex) { _logger.info("Problems getting SQL-Server instance name. ex="+ex);}
			try { srvRestart = SqlServerUtils.getStartDate(conn); } catch (SQLException ex) { _logger.info("Problems getting SQL-Server start date. ex="+ex);}

			String sql = "select count(*), sum(error_count) from msdb.dbo.suspect_pages";
			
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
				{
					int suspectPageCount  = rs.getInt(1);
					int suspectPageErrors = rs.getInt(2);
					
					if (suspectPageCount > 0)
					{
						String key = "DbmsConfigIssue." + srvName + ".suspectPageCount";

						DbmsConfigIssue issue = new DbmsConfigIssue(srvRestart, key, "Suspect Page Count", Severity.ERROR, 
								suspectPageCount + " Suspect pages, with " + suspectPageErrors + " errors of some form was found in 'msdb.dbo.suspect_pages'. The instance may have a severy corruption issue, please investigate.", 
								"If the errors has already been resolved, please remove those records from table 'msdb.dbo.suspect_pages'. "
										+ "Resources: 'https://learn.microsoft.com/en-us/sql/relational-databases/backup-restore/manage-the-suspect-pages-table-sql-server' "
										+ "and possibly: 'https://www.brentozar.com/go/corruption'");

						DbmsConfigManager.getInstance().addConfigIssue(issue);
					}
				}
			}
			catch (SQLException ex)
			{
				_logger.error("Problems getting SQL-Server 'Suspect Page Count', using sql '"+sql+"'. Caught: "+ex, ex);
			}
		}
	}

	public static class Certificates extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                          { return "Certificates"; }
		@Override public    String     getName()                              { return ConfigType.SqlServerCertificates.toString(); }
		@Override public    String     getConfigType()                        { return getName(); }
		@Override protected String     getSqlCurrentConfig(DbmsVersionInfo v) 
		{
			String sql = ""
				    + "print '------------------------------------------------------------------------------------------------------------' \n"
				    + "print '-- NOTE: Certificates starting with ## are SQL Server internal certificates, which we should NOT care about.' \n"
				    + "print '------------------------------------------------------------------------------------------------------------' \n"
				    + "print '' \n"
				    + "\n"
				    + "select datediff(day, getdate(), expiry_date) AS days_to_expiry, * from sys.certificates \n"
				    + "";

			return sql;
		}

		/** 
		 * Check for 'soon to be expired certificates' 
		 */
		@Override
		public void checkConfig(DbxConnection conn)
		{
			// no nothing, if we havn't got an instance
			if ( ! DbmsConfigManager.hasInstance() )
				return;

			String    srvName    = "-UNKNOWN-";
			Timestamp srvRestart = null;
			try { srvName    = conn.getDbmsServerName();          } catch (SQLException ex) { _logger.info("Problems getting SQL-Server instance name. ex="+ex);}
			try { srvRestart = SqlServerUtils.getStartDate(conn); } catch (SQLException ex) { _logger.info("Problems getting SQL-Server start date. ex="+ex);}

			String sql = ""
				    + "SELECT \n"
				    + "     name \n"
				    + "    ,expiry_date \n"
				    + "    ,datediff(day, getdate(), expiry_date) AS days_to_expiry \n"
				    + "FROM sys.certificates \n"
				    + "WHERE name NOT LIKE '##%' \n"
				    + "";
			
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
				{
					String    name           = rs.getString   (1);
					Timestamp expiry_date    = rs.getTimestamp(2);
					int       days_to_expiry = rs.getInt      (3);
					
					int threshold = 30;
					if (days_to_expiry < threshold)
					{
						String key = "DbmsConfigIssue." + srvName + ".certificates";

						DbmsConfigIssue issue = new DbmsConfigIssue(srvRestart, key, "Certificate", Severity.WARNING, 
								"Certificate named '" + name + "' will exire in " + days_to_expiry + " days, at '" + expiry_date + "'.", 
								"Renew the certificate.");

						// Add issue
						DbmsConfigManager.getInstance().addConfigIssue(issue);
						
						// Send an alarm about this as well
						if (AlarmHandler.hasInstance())
						{
							// Set time to live as 25 hours... next day recording will send a new alarm
							long ttl = TimeUnit.HOURS.toMillis(25);     // 25 hours to milliseconds.
							AlarmEvent ae = new AlarmEventCertificateExpiry(srvName, name, days_to_expiry, expiry_date, ttl, threshold);
							
							// Send the alarm
							AlarmHandler.getInstance().addAlarm(ae);
						}
					}
				}
			}
			catch (SQLException ex)
			{
				_logger.error("Problems getting SQL-Server 'Suspect Page Count', using sql '"+sql+"'. Caught: "+ex, ex);
			}
		}
	}

	public static class SqlServices extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                          { return "SQL Services"; }
		@Override public    String     getName()                              { return ConfigType.SqlServerServices.toString(); }
		@Override public    String     getConfigType()                        { return getName(); }
		@Override protected String     getSqlCurrentConfig(DbmsVersionInfo v) { return "select * from sys.dm_server_services"; }

		/** 
		 * Check for 'instant file initialization' 
		 */
		@Override
		public void checkConfig(DbxConnection conn)
		{
			// no nothing, if we havn't got an instance
			if ( ! DbmsConfigManager.hasInstance() )
				return;

			String    srvName    = "-UNKNOWN-";
			Timestamp srvRestart = null;
			try { srvName    = conn.getDbmsServerName();          } catch (SQLException ex) { _logger.info("Problems getting SQL-Server instance name. ex="+ex);}
			try { srvRestart = SqlServerUtils.getStartDate(conn); } catch (SQLException ex) { _logger.info("Problems getting SQL-Server start date. ex="+ex);}

			String sql = "SELECT instant_file_initialization_enabled FROM sys.dm_server_services WHERE servicename LIKE 'SQL Server (%'";
			
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
				{
					String instFileInit = rs.getString(1);
					
					if ( ! "Y".equalsIgnoreCase(instFileInit) )
					{
						String key = "DbmsConfigIssue." + srvName + ".instant_file_initialization";

						DbmsConfigIssue issue = new DbmsConfigIssue(srvRestart, key, "instant_file_initialization", Severity.INFO, 
								"Instant file initialization on the OS is NOT enabled. Growing of database files will be slower.", 
								"https://docs.microsoft.com/en-us/sql/relational-databases/databases/database-instant-file-initialization");

						DbmsConfigManager.getInstance().addConfigIssue(issue);
					}
				}
			}
			catch (SQLException ex)
			{
				_logger.error("Problems getting SQL-Server 'instant_file_initialization', using sql '"+sql+"'. Caught: "+ex, ex);
			}
		}
	}

	public static class ResourceGovernor extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                        { return "Resource Governor"; }
		@Override public    String     getName()                            { return ConfigType.SqlServerResourceGovernor.toString(); }
		@Override public    String     getConfigType()                      { return getName(); }
//		@Override protected String     getSqlCurrentConfig(long srvVersion) 
		@Override protected String     getSqlCurrentConfig(DbmsVersionInfo v)
		{
			String sql = ""
				    + "print '' \n"
				    + "print 'INFO: Below is from table: resource_governor_configuration' \n"
				    + "select *, active_classifier_function_name = object_name(classifier_function_id) from sys.resource_governor_configuration \n"
				    + " \n"
				    + "print '' \n"
				    + "print 'INFO: Below is from table: sys.resource_governor_workload_groups' \n"
				    + "select * from sys.resource_governor_workload_groups \n"
				    + " \n"
				    + "print '' \n"
				    + "print 'INFO: Below is from table: resource_governor_resource_pools' \n"
				    + "select * from sys.resource_governor_resource_pools \n"
				    + " \n"
				    + "print '' \n"
				    + "print 'INFO: Below is from table: resource_governor_resource_pool_affinity' \n"
				    + "select * from sys.resource_governor_resource_pool_affinity \n"
				    + " \n"
				    + "declare @active_classifier_function_name varchar(128) \n"
				    + "select @active_classifier_function_name = object_name(classifier_function_id) from sys.resource_governor_configuration \n"
				    + "if (@active_classifier_function_name is not null) \n"
				    + "begin \n"
				    + "    print '' \n"
				    + "    print 'INFO: SQL Text for the classifier_function: ' + @active_classifier_function_name \n"
				    + "    exec sp_helptext @active_classifier_function_name \n"
				    + "end \n"
				    + "";
			return sql;
		}
	}

	public static class ServerRegistry extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                          { return "Server Registry"; }
		@Override public    String     getName()                              { return ConfigType.SqlServerServerRegistry.toString(); }
		@Override public    String     getConfigType()                        { return getName(); }
//		@Override protected String     getSqlCurrentConfig(long srvVersion)   { return "select * from sys.dm_server_registry"; }
		@Override protected String     getSqlCurrentConfig(DbmsVersionInfo v) { return "select * from sys.dm_server_registry"; }
		@Override
		public String checkRequirements(DbxConnection conn)
		{
			DbmsVersionInfoSqlServer versionInfo = (DbmsVersionInfoSqlServer) conn.getDbmsVersionInfo();

			// In Azure Database, skip this 
			if (versionInfo.isAzureDb() || versionInfo.isAzureSynapseAnalytics())
				return "sys.dm_server_registry: is NOT supported in Azure SQL Database.";

			return super.checkRequirements(conn);
		}
	}

	public static class ClusterNodes extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                          { return "Cluster Nodes"; }
		@Override public    String     getName()                              { return ConfigType.SqlServerClusterNodes.toString(); }
		@Override public    String     getConfigType()                        { return getName(); }
//		@Override protected String     getSqlCurrentConfig(long srvVersion)   { return "select * from sys.dm_os_cluster_nodes"; }
		@Override protected String     getSqlCurrentConfig(DbmsVersionInfo v) { return "select * from sys.dm_os_cluster_nodes"; }
		@Override
		public String checkRequirements(DbxConnection conn)
		{
			DbmsVersionInfoSqlServer versionInfo = (DbmsVersionInfoSqlServer) conn.getDbmsVersionInfo();

			// In Azure Database, skip this 
			if (versionInfo.isAzureDb() || versionInfo.isAzureSynapseAnalytics())
				return "sys.dm_os_cluster_nodes: is NOT supported in Azure SQL Database.";

			return super.checkRequirements(conn);
		}
	}

	public static class AgentJobs extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                          { return "Agent Jobs"; }
		@Override public    String     getName()                              { return ConfigType.SqlServerAgentJobs.toString(); }
		@Override public    String     getConfigType()                        { return getName(); }
		@Override
		public String checkRequirements(DbxConnection conn)
		{
			DbmsVersionInfoSqlServer versionInfo = (DbmsVersionInfoSqlServer) conn.getDbmsVersionInfo();

			// In Azure Database, skip this 
			if (versionInfo.isAzureDb() || versionInfo.isAzureSynapseAnalytics())
				return "NOT supported in Azure SQL Database.";

			return super.checkRequirements(conn);
		}
		@Override protected String getSqlCurrentConfig(DbmsVersionInfo v) 
		{ 
			// from : https://glennsqlperformance.com/resources/
			// And slightly modified
			String sql = ""
				    + "-- Get SQL Server Agent jobs and Category information (Query 10) (SQL Server Agent Jobs) \n"
				    + "print '-------------------------------------------------------------------------------' \n"
				    + "print '-- Get SQL Server Agent jobs ' \n"
				    + "print '-------------------------------------------------------------------------------' \n"
				    + "print '' \n"
				    + "SELECT \n"
				    + "     sj.name                      AS [Job Name] \n"
				    + "    ,sj.[description]             AS [Job Description] \n"
				    + "    ,sc.name                      AS [CategoryName] \n"
				    + "    ,SUSER_SNAME(sj.owner_sid)    AS [Job Owner] \n"
				    + "    ,sj.date_created              AS [Date Created] \n"
				    + "    ,sj.[enabled]                 AS [Job Enabled] \n"
				    + "    ,sj.notify_email_operator_id \n"
				    + "    ,sj.notify_level_email \n"
				    + "    ,h.run_status \n"
				    + "    ,CASE WHEN h.run_status=0 THEN 'FAILED' \n"
				    + "          WHEN h.run_status=1 THEN 'Success' \n"
				    + "          WHEN h.run_status=2 THEN 'Retry' \n"
				    + "          WHEN h.run_status=3 THEN 'Canceled' \n"
				    + "          WHEN h.run_status=4 THEN 'In Progress' \n"
				    + "          ELSE 'unknown' \n"
				    + "     END AS run_status_desc \n"
				    + "    ,RIGHT(STUFF(STUFF(REPLACE(STR(h.run_duration, 7, 0), ' ', '0'), 4, 0, ':'), 7, 0, ':'),8) AS [Last Duration - HHMMSS] \n"
				    + "    ,CONVERT(DATETIME, RTRIM(h.run_date) + ' ' + STUFF(STUFF(REPLACE(STR(RTRIM(h.run_time),6,0),' ','0'),3,0,':'),6,0,':')) AS [Last Start Date] \n"
				    + "    ,datediff(hour, CONVERT(DATETIME, RTRIM(h.run_date) + ' ' + STUFF(STUFF(REPLACE(STR(RTRIM(h.run_time),6,0),' ','0'),3,0,':'),6,0,':')), getdate()) AS [Last Start Age In Hours] \n"
				    + "FROM msdb.dbo.sysjobs AS sj WITH (NOLOCK) \n"
				    + "INNER JOIN \n"
				    + "    (SELECT job_id, instance_id = MAX(instance_id) \n"
				    + "     FROM msdb.dbo.sysjobhistory WITH (NOLOCK) \n"
				    + "     GROUP BY job_id) AS l \n"
				    + "  ON sj.job_id = l.job_id \n"
				    + "INNER JOIN msdb.dbo.syscategories AS sc WITH (NOLOCK) ON sj.category_id = sc.category_id \n"
				    + "INNER JOIN msdb.dbo.sysjobhistory AS  h WITH (NOLOCK) ON h.job_id       = l.job_id  AND  h.instance_id = l.instance_id \n"
				    + "ORDER BY CONVERT(INT, h.run_duration) DESC, [Last Start Date] DESC \n"
				    + "OPTION (RECOMPILE); \n"
				    + " \n"
				    + "-- Get SQL Server Agent Alert Information (Query 11) (SQL Server Agent Alerts) \n"
				    + "print '' \n"
				    + "print '' \n"
				    + "print '-------------------------------------------------------------------------------' \n"
				    + "print '-- Get SQL Server Agent Alert Information' \n"
				    + "print '-------------------------------------------------------------------------------' \n"
				    + "print '' \n"
				    + "SELECT \n"
				    + "     name \n"
				    + "    ,event_source \n"
				    + "    ,message_id \n"
				    + "    ,severity \n"
				    + "    ,[enabled] \n"
				    + "    ,has_notification \n"
				    + "    ,delay_between_responses \n"
				    + "    ,occurrence_count \n"
				    + "    ,last_occurrence_date \n"
				    + "    ,last_occurrence_time \n"
				    + "FROM msdb.dbo.sysalerts WITH (NOLOCK) \n"
				    + "ORDER BY name \n"
				    + "OPTION (RECOMPILE); \n"
				    + "";

			return sql; 
		}
	}

}
