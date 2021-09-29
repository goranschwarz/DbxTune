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

import org.apache.log4j.Logger;

import com.asetune.config.dbms.DbmsConfigIssue.Severity;
import com.asetune.config.dict.SqlServerTraceFlagsDictionary;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.sql.conn.DbxConnection;
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
		,SqlServerTraceflags
		,SqlServerLinkedServers
		,SqlServerHelpSort
		,SqlServerHostInfo
		,SqlServerSysInfo
		,SqlServerResourceGovernor
		,SqlServerServerRegistry
		,SqlServerClusterNodes
		};

	/** Log4j logging. */
	private static Logger _logger = Logger.getLogger(SqlServerConfigText.class);

	public static void createAndRegisterAllInstances()
	{
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.HelpDb());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.SysDatabases());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.Traceflags());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.LinkedServers());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.HelpSort());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.HostInfo());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.SysInfo());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.ResourceGovernor());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.ServerRegistry());
		DbmsConfigTextManager.addInstance(new SqlServerConfigText.ClusterNodes());
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
		@Override public    String     getTabLabel()                        { return "sp_helpdb"; }
		@Override public    String     getName()                            { return ConfigType.SqlServerHelpDb.toString(); }
		@Override public    String     getConfigType()                      { return getName(); }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "exec sp_helpdb"; }
		
		
		/** Check 'compatibility_level' for all databases */
		@Override
		public void checkConfig(DbxConnection conn)
		{
			// no nothing, if we havnt got an instance
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
							"Fix this using: ALTER DATABASE " + dbname + " SET COMPATIBILITY_LEVEL = " + srvCompatLevel);

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
		@Override protected String     getSqlCurrentConfig(long srvVersion) 
		{
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
					"exec sys.sp_MSforeachdb 'select convert(varchar(60),''?'') as dbname, * from sys.database_scoped_configurations' \n" +
					"go \n" +
					"";
			return sql; 
		}
	}

	public static class Traceflags extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                        { return "traceflags"; }
		@Override public    String     getName()                            { return ConfigType.SqlServerTraceflags.toString(); }
		@Override public    String     getConfigType()                      { return getName(); }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "DBCC TRACESTATUS(-1) WITH NO_INFOMSGS"; }

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
		@Override public    String     getTabLabel()                        { return "sp_linkedservers"; }
		@Override public    String     getName()                            { return ConfigType.SqlServerLinkedServers.toString(); }
		@Override public    String     getConfigType()                      { return getName(); }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "exec sp_linkedservers"; }
	}

	public static class HelpSort extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                        { return "sp_helpsort"; }
		@Override public    String     getName()                            { return ConfigType.SqlServerHelpSort.toString(); }
		@Override public    String     getConfigType()                      { return getName(); }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "exec sp_helpsort   SELECT SERVERPROPERTY_Collation = convert(varchar(255), SERVERPROPERTY('Collation'))"; }
	}

	public static class HostInfo extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                        { return "Host Info"; }
		@Override public    String     getName()                            { return ConfigType.SqlServerHostInfo.toString(); }
		@Override public    String     getConfigType()                      { return getName(); }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "select * from sys.dm_os_host_info"; }
		@Override public    long       needVersion()                        { return Ver.ver(2017); }
//		@Override public    long       needVersion()                        { return Ver.ver(14,0); } // 14 == 2017
	}

	public static class SysInfo extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                        { return "System Info"; }
		@Override public    String     getName()                            { return ConfigType.SqlServerSysInfo.toString(); }
		@Override public    String     getConfigType()                      { return getName(); }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "select * from sys.dm_os_sys_info"; }
	}

	public static class ResourceGovernor extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                        { return "Resource Governor"; }
		@Override public    String     getName()                            { return ConfigType.SqlServerResourceGovernor.toString(); }
		@Override public    String     getConfigType()                      { return getName(); }
		@Override protected String     getSqlCurrentConfig(long srvVersion) 
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
		@Override public    String     getTabLabel()                        { return "Server Registry"; }
		@Override public    String     getName()                            { return ConfigType.SqlServerServerRegistry.toString(); }
		@Override public    String     getConfigType()                      { return getName(); }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "select * from sys.dm_server_registry"; }
	}

	public static class ClusterNodes extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                        { return "Cluster Nodes"; }
		@Override public    String     getName()                            { return ConfigType.SqlServerClusterNodes.toString(); }
		@Override public    String     getConfigType()                      { return getName(); }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "select * from sys.dm_os_cluster_nodes"; }
	}

}
