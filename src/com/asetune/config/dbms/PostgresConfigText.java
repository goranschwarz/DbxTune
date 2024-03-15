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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.asetune.cm.CounterSampleCatalogIteratorPostgres;
import com.asetune.hostmon.HostMonitorConnection;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.DbxConnectionPoolMap;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;
import com.google.common.collect.Lists;

public abstract class PostgresConfigText
{
	/** What sub types exists */
	public enum ConfigType
	{
		 PostgresDbInfo
		,PostgresHbaConf
		,PostgresPgConfig
		,PostgresExtentions
		,PostgresServers
		};

	/** Log4j logging. */
	private static Logger _logger = Logger.getLogger(PostgresConfigText.class);

	public static void createAndRegisterAllInstances()
	{
		DbmsConfigTextManager.addInstance(new PostgresConfigText.DbInfo());
		DbmsConfigTextManager.addInstance(new PostgresConfigText.HbaConf());
		DbmsConfigTextManager.addInstance(new PostgresConfigText.PgConfig());
		DbmsConfigTextManager.addInstance(new PostgresConfigText.Extentions());
		DbmsConfigTextManager.addInstance(new PostgresConfigText.Servers());
	}

	/*-----------------------------------------------------------
	**-----------------------------------------------------------
	**-----------------------------------------------------------
	**----- SUB CLASSES ----- SUB CLASSES ----- SUB CLASSES -----
	**-----------------------------------------------------------
	**-----------------------------------------------------------
	**-----------------------------------------------------------
	*/
	public static class DbInfo extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                          { return "DB Info"; }
		@Override public    String     getName()                              { return ConfigType.PostgresDbInfo.toString(); }
		@Override public    String     getConfigType()                        { return getName(); }
		@Override protected String     getSqlCurrentConfig(DbmsVersionInfo v) { return "select * from pg_database"; }
	}

	public static class HbaConf extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                          { return "HBA Conf"; }
		@Override public    String     getName()                              { return ConfigType.PostgresHbaConf.toString(); }
		@Override public    String     getConfigType()                        { return getName(); }
		@Override public    long       needVersion()                          { return Ver.ver(10); }
		@Override public  List<String> needRole()                             { return Lists.newArrayList("superuser"); }
		@Override protected String     getSqlCurrentConfig(DbmsVersionInfo v) { return "select * from pg_hba_file_rules"; }
	}

	public static class PgConfig extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                          { return "PG Config"; }
		@Override public    String     getName()                              { return ConfigType.PostgresPgConfig.toString(); }
		@Override public    String     getConfigType()                        { return getName(); }
		@Override public    long       needVersion()                          { return Ver.ver(9,6); } // https://pgpedia.info/p/pg_config-view.html
		@Override protected String     getSqlCurrentConfig(DbmsVersionInfo v) { return "select * from pg_config"; }
	}

	public static class Extentions extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                          { return "Extentions"; }
		@Override public    String     getName()                              { return ConfigType.PostgresExtentions.toString(); }
		@Override public    String     getConfigType()                        { return getName(); }
		@Override protected String     getSqlCurrentConfig(DbmsVersionInfo v) 
		{
			String sql = ""
				    + "DO $$ BEGIN \n"
				    + "    RAISE NOTICE ''; \n"
				    + "    RAISE NOTICE '--###############################################'; \n"
				    + "    RAISE NOTICE '--## Installed Extentions'; \n"
				    + "    RAISE NOTICE '--###############################################'; \n"
				    + "END; $$; \n"
				    + "SELECT \n"
				    + "     e.oid \n"
				    + "    ,e.extname      AS [Name] \n"
				    + "    ,e.extversion   AS [Version] \n"
				    + "    ,extrelocatable AS [ReLocatable] \n"
				    + "    ,n.nspname      AS [Schema] \n"
				    + "    ,c.description  AS [Description] \n"
				    + "    ,e.extconfig    AS [ExtConfig] \n"
				    + "    ,e.extcondition AS [ExtCondition] \n"
				    + "FROM pg_extension e \n"
				    + "LEFT JOIN pg_namespace n ON n.oid = e.extnamespace \n"
				    + "LEFT JOIN pg_description c ON c.objoid = e.oid AND c.classoid = 'pg_catalog.pg_extension'::pg_catalog.regclass \n"
				    + "ORDER BY e.extname; \n"
				    + "go \n"
				    + "DO $$ BEGIN \n"
				    + "    RAISE NOTICE ''; \n"
				    + "    RAISE NOTICE '--###############################################'; \n"
				    + "    RAISE NOTICE '--## Available Extentions'; \n"
				    + "    RAISE NOTICE '--###############################################'; \n"
				    + "END; $$; \n"
				    + "SELECT * FROM pg_available_extensions \n"
				    + "";
			
			// Quote using Postgres Quoted Identifier Char (")
			sql = sql.replace('[', '"');
			sql = sql.replace(']', '"');
			
			return sql; 
		}
	}

	public static class Servers extends DbmsConfigTextDatabaseIteratorAbstract
	{
		@Override public    String     getTabLabel()                          { return "Servers"; }
		@Override public    String     getName()                              { return ConfigType.PostgresServers.toString(); }
		@Override public    String     getConfigType()                        { return getName(); }
		@Override protected String     getSqlCurrentConfig(DbmsVersionInfo v) 
		{
			// NOTE: server info is stored in each database... so we need to loop all databases 

			String sql = ""
				    + "DO $$ BEGIN \n"
				    + "    RAISE NOTICE ''; \n"
				    + "    RAISE NOTICE '--###############################################'; \n"
				    + "    RAISE NOTICE '--## Server Names, with Foreign Data Wrappers'; \n"
				    + "    RAISE NOTICE '--###############################################'; \n"
				    + "END; $$; \n"
				    + "select \n"
				    + "     current_database()  AS at_dbname \n"
				    + "    ,srvname             AS server_name \n"
//				    + "    ,srvowner::regrole   AS owner \n"
				    + "    ,(select a.rolname from pg_authid a where s.oid = a.oid) AS owner"
				    + "    ,fdwname             AS wrapper_name \n"
				    + "    ,srvoptions          AS options \n"
				    + "from pg_foreign_server s \n"
				    + "join pg_foreign_data_wrapper w on w.oid = srvfdw; \n"
				    + "go \n"
				    + " \n"
				    + "DO $$ BEGIN \n"
				    + "    RAISE NOTICE ''; \n"
				    + "    RAISE NOTICE '--###############################################'; \n"
				    + "    RAISE NOTICE '--## Mapped Users'; \n"
				    + "    RAISE NOTICE '--###############################################'; \n"
				    + "END; $$; \n"
				    + "select \n"
				    + "     current_database()  AS at_dbname \n"
				    + "     ,* \n"
				    + "from pg_user_mappings \n"
				    + "go \n"
				    + "";

			return sql;
		}
	}












	/**
	 * Special class for Postgres to iterate over all databases
	 */
	public abstract static class DbmsConfigTextDatabaseIteratorAbstract extends DbmsConfigTextAbstract
	{
		Map<Long, String> _dbNameMap = new LinkedHashMap<>();

		@Override
//		protected String doOnlineRefresh(DbxConnection templateConn, String sql, HostMonitorConnection hostMonConn, String osCmd)
		protected String doOnlineRefresh(DbxConnection templateConn, HostMonitorConnection hostMonConn)
		{
			// Get a list of databases
			refreshDatabases(templateConn);

			// Since we need a connection to each of the databases... setup a connection pool, which will hold connections!
			// Note: This ConnectionPool will be reused by some CM's later on
			if ( ! DbxConnectionPoolMap.hasInstance() )
				DbxConnectionPoolMap.setInstance(new DbxConnectionPoolMap());

			// Store results in here
			String resultsForAllDbs = "";

			// Second: Loop all databases and get info...
			for (Entry<Long, String> e : _dbNameMap.entrySet())
			{
//				long   dbid   = e.getKey();
				String dbname = e.getValue();

//				long startTime = System.currentTimeMillis();

				// Get a Connection Pool for this specific database
				if ( ! DbxConnectionPoolMap.hasInstance() )
				{
					_logger.info("Skipping refresh for database '" + dbname + "', no Connection Pool Map was found.");
					continue;
				}

				resultsForAllDbs += "\n";
				resultsForAllDbs += "--#################################################################################\n";
				resultsForAllDbs += "--#################################################################################\n";
				resultsForAllDbs += "--#### Results for database: '" + dbname + "'\n";
				resultsForAllDbs += "--#################################################################################\n";
				resultsForAllDbs += "--#################################################################################\n";
				resultsForAllDbs += "\n";

				// CONNECT TO the database (use some Connection Pool)
				try
				{
					// Get a connection (for this database, a new will be created if needed)
					DbxConnection dbConn = CounterSampleCatalogIteratorPostgres.getConnection(null, templateConn, dbname);

					try
					{
						// Use "super" to execute the SQL in this database 
//						String result = super.doOnlineRefresh(dbConn, sql, hostMonConn, osCmd);
						String result = super.doOnlineRefresh(dbConn, hostMonConn);

						// Add to "all" results
						resultsForAllDbs += result;
						resultsForAllDbs += "\n\n\n";
					}
					finally 
					{
						// release the connection to the pool again
						CounterSampleCatalogIteratorPostgres.releaseConnection(null, dbConn, dbname);
					}
				}
				catch (SQLException ex)
				{
					resultsForAllDbs += "--#### Problems connecting to Postgres for database '" + dbname + "'. Skipping refresh for database '" + dbname + "'. Caught: " + ex;
					resultsForAllDbs += "\n\n\n";

					_logger.error("Problems connecting to Postgres for database '" + dbname + "'. Skipping refresh for database '" + dbname + "'.", ex);
				}

//				long execTimeMs = TimeUtils.msDiffNow(startTime);

			} // end: DB-Loop
			
			return resultsForAllDbs;
		}

		public void refreshDatabases(DbxConnection templateConn)
		{
			if (templateConn == null)
				throw new RuntimeException("DbmsConfigTextDatabaseIteratorAbstract.refreshDatabases(...): Connection can't be null.");

			// Clear all cached values
			_dbNameMap.clear();

			// First: get all databases
			String sql = "SELECT oid, datname \n"
					+ "FROM pg_database \n"
					+ "WHERE datname not like 'template%' \n"
					+ "  AND has_database_privilege(datname, 'CONNECT') \n" // Possibly add this to only lookup databases that we have access to
					+ "ORDER by 1 \n";
			try (Statement stmnt = templateConn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
				{
					long   dbid   = rs.getLong  (1);
					String dbname = rs.getString(2);
					
					_dbNameMap.put(dbid, dbname);
				}
			}
			catch(SQLException ex)
			{
				_logger.error("Problems when executing sql: " + sql + ". SQLException Error=" + ex.getErrorCode() + ", Msg='" + StringUtil.stripNewLine(ex.getMessage()) + "'.", ex);
				return;
			}
		}

//		public void getDatabases(DbxConnection templateConn, Timestamp ts)
//		{
//			if (templateConn == null)
//				throw new RuntimeException("DbmsConfigTextDatabaseIteratorAbstract.xxx(...): Connection can't be null.");
//
//			// Clear all cached values
//			_dbNameMap.clear();
//			
//			// First: get all databases
//			String sql = "SELECT oid, datname \n"
//					+ "FROM pg_database \n"
//					+ "WHERE datname not like 'template%' \n"
//					+ "  AND has_database_privilege(datname, 'CONNECT') \n" // Possibly add this to only lookup databases that we have access to
//					+ "ORDER by 1 \n";
//			try (Statement stmnt = templateConn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
//			{
//				while(rs.next())
//				{
//					long   dbid   = rs.getLong  (1);
//					String dbname = rs.getString(2);
//					
//					_dbNameMap.put(dbid, dbname);
//				}
//			}
//			catch(SQLException ex)
//			{
//				_logger.error("Problems when executing sql: " + sql + ". SQLException Error=" + ex.getErrorCode() + ", Msg='" + StringUtil.stripNewLine(ex.getMessage()) + "'.", ex);
//				return;
//			}
//			
//			// Since we need a connection to each of the databases... setup a connection pool, which will hold connections!
//			// Note: This ConnectionPool will be reused by some CM's later on
//			if ( ! DbxConnectionPoolMap.hasInstance() )
//				DbxConnectionPoolMap.setInstance(new DbxConnectionPoolMap());
//
//
//			// Second: Loop all databases and get info...
//			boolean doLookup = true;
//			if (doLookup)
//			{
//				for (Entry<Long, String> e : _dbNameMap.entrySet())
//				{
//					long   dbid   = e.getKey();
//					String dbname = e.getValue();
//
//					long startTime = System.currentTimeMillis();
//					
//					// Get a Connection Pool for this specific database
//					if ( ! DbxConnectionPoolMap.hasInstance() )
//					{
//						_logger.info("Skipping refresh for database '" + dbname + "', no Connection Pool Map was found.");
//						continue;
//					}
//
//					// CONNECT TO the database (use some Connection Pool)
//					try
//					{
//						DbxConnection dbConn = CounterSampleCatalogIteratorPostgres.getConnection(null, templateConn, dbname);
//						sql = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";
//TODO; Make getQueries() Map<SQL>... withe storage of <QueryName, ResultSetTableModel>
//Where we can merge RSTM on names;
//
//						ResultSetTableModel.executeQuery(dbConn, sql, "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
//						try (Statement stmnt = dbConn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
//						{
//							while(rs.next())
//							{
//							} // end: rs.next()
//
//							long execTimeMs = TimeUtils.msDiffNow(startTime);
//						}
//						catch(SQLException ex)
//						{
//							_logger.error("Skipping refresh for database '" + dbname + "', continuing with next database. Problems when executing sql: " + sql + ". SQLException Error=" + ex.getErrorCode() + ", Msg='" + StringUtil.stripNewLine(ex.getMessage()) + "'.", ex);
//						}
//						finally 
//						{
//							CounterSampleCatalogIteratorPostgres.releaseConnection(null, dbConn, dbname);
//						}
//					}
//					catch (SQLException ex)
//					{
//						_logger.error("Problems connecting to Postgres for database '" + dbname + "'. Skipping refresh for database '" + dbname + "'.", ex);
//					}
//				} // end: DB-Loop
//			} // end: Do lookup
//		}
	}
		
}
