/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.cache;

import java.lang.invoke.MethodHandles;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.cm.CounterSampleCatalogIteratorPostgres;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.DbxConnectionPoolMap;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.ConnectionProvider;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

public class DbmsObjectIdCachePostgres 
extends DbmsObjectIdCache
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String  PROPKEY_BulkLoadOnStart = "PostgresTune.objectIdCahe.bulkLoadOnStart";
	public static final boolean DEFAULT_BulkLoadOnStart = true;  // for testing just now...
//	public static final boolean DEFAULT_BulkLoadOnStart = false; // if we want to revert back for X number of CM's to use direct lookups of: pg_class, etc...

	public DbmsObjectIdCachePostgres(ConnectionProvider connProvider)
	{
		super(connProvider);
	}

	@Override
	protected void onConnect(DbxConnection conn)
	{
	}

	@Override
	public boolean isBulkLoadOnStartEnabled()
	{
		return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_BulkLoadOnStart, DEFAULT_BulkLoadOnStart);
//		return false;
	}

	public static final String SCHEMA_PUBLIC      = "public";
	public static final String SCHEMA_PG_CATALOG  = "pg_catalog";
	public static final String SCHEMA_INFORMATION = "information_schema";

	@Override
	protected Map<String, String> createStaticSchemaNames()
	{
		Map<String, String> map = new HashMap<>();

		map.put(SCHEMA_PUBLIC     , SCHEMA_PUBLIC);
		map.put(SCHEMA_PG_CATALOG , SCHEMA_PG_CATALOG);
		map.put(SCHEMA_INFORMATION, SCHEMA_INFORMATION);
		
		return map;
	}

	/**
	 * SINGLE OBJECT LOAD/REQUEST for Postgres... 
	 */
	@Override
	protected ObjectInfo get(DbxConnection templateConn, LookupType lookupType, long dbid, Number lookupId) 
	throws TimeoutException
	{
		if (templateConn == null)
			return null;

		// Get database name from ID, if nor found... go and get it
		String dbname = getDBName(dbid);
		if (StringUtil.isNullOrBlank(dbname))
		{
			String sql = "SELECT datname FROM pg_database WHERE oid = " + dbid;
			try (Statement stmnt = templateConn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
				{
					dbname = rs.getString(1);
					setDBName(dbid, dbname);
				}
			}
			catch(SQLException ex)
			{
				_logger.error("Problems when executing sql: " + sql + ". SQLException Error=" + ex.getErrorCode() + ", Msg='" + StringUtil.stripNewLine(ex.getMessage()) + "'.", ex);
				return null;
			}
		}
		
		// If we do not have a dbname we can't continue.
		if (StringUtil.isNullOrBlank(dbname))
		{
			_logger.warn("Can't lookup ObjectName reason: dbid=" + dbid + " was resolved to null or blank (dbname='" + dbname+ "') for lookupType=" + lookupType + ", dbid=" + dbid + ", lookupId=" + lookupId + ".");
			return null;
		}

		// If we do not have a dbname we can't continue.
		if (dbname.startsWith("template"))
		{
			_logger.warn("Can't lookup ObjectName reason: dbid=" + dbid + " dbname='" + dbname + "' is a template database, which we can't connect to.");
			return null;
		}

		if ( ! CounterController.hasInstance() )
			return null;

		if      (LookupType.ObjectId   .equals(lookupType)) ;
//		else if (LookupType.PartitionId.equals(lookupType)) ;
//		else if (LookupType.HobtId     .equals(lookupType)) ;
		else throw new RuntimeException("Unknown Lookup Type '" + lookupType + "'.");


		// Since we need a connection to each of the databases... setup a connection pool, which will hold connections!
		// Note: This ConnectionPool will be reused by some CM's later on
		if ( ! DbxConnectionPoolMap.hasInstance() )
			DbxConnectionPoolMap.setInstance(new DbxConnectionPoolMap());

		String sql = ""
			    + "SELECT \n"
			    + "     ns.oid       AS schema_id \n"    // 1
			    + "    ,ns.nspname   AS schema_name \n"  // 2
			    + "    ,tc.oid       AS object_id \n"    // 3
			    + "    ,tc.relname   AS object_name \n"  // 4
			    + "    ,i.indexrelid AS index_id \n"     // 5
			    + "    ,ic.relname   AS index_name \n"   // 6
			    + "    ,tc.relkind   AS object_type \n"  // 7
			    + "FROM pg_class              tc \n"
			    + "  JOIN pg_namespace        ns ON ns.oid = tc.relnamespace \n"
			    + "  LEFT OUTER JOIN pg_index i  ON tc.oid       = i.indrelid \n"
			    + "  LEFT OUTER JOIN pg_class ic ON i.indexrelid = ic.oid \n"
			    + "WHERE 1 = 1 \n"
//			    + "  AND tc.relkind in('r', 'm', 'p') \n"
			    + "  AND tc.oid = ? \n"
			    + "ORDER BY tc.oid, i.indexrelid \n"
			    + "";

		try
		{
			// CONNECT/GRAB a connection TO the database (use some Connection Pool for the dbname)
			DbxConnection dbConn = CounterSampleCatalogIteratorPostgres.getConnection(null, templateConn, dbname);

			// return object
			ObjectInfo objectInfo = null;
			long execStartTime = System.currentTimeMillis();

			try (PreparedStatement pstmnt = dbConn.prepareStatement(sql)) // Auto CLOSE
			{
				// Timeout after 2 second --- if we get blocked when doing: object_name()
				pstmnt.setQueryTimeout(2);
				
				// set lookup ID
				pstmnt.setLong(1, lookupId.longValue());

				// Execute and read results
				try (ResultSet rs = pstmnt.executeQuery()) // Auto CLOSE
				{
					while(rs.next())
					{
						long   schemaId    = rs.getLong  (1);
						String schemaName  = rs.getString(2);
						long   objectId    = rs.getLong  (3);
						String objectName  = rs.getString(4);
						long   indexId     = rs.getLong  (5);
						String indexName   = rs.getString(6);
						String relKind     = rs.getString(7);

						// Transform to a normalized ObjectType
						ObjectType objectType = getObjectType(schemaName, relKind);

						// Create a new ObjectInfo
						objectInfo = new ObjectInfo(dbid, dbname, schemaId, schemaName, objectId, objectName, objectType);

						// In the super (which is/holds the cache)... add the info...
						// and yes we can do that because the Index Id/Name has it's own Map
						super.setObjectInfo(dbid, objectId, objectInfo);

						// On consequent rows: Just add the indexes (if we got any)
						if (StringUtil.hasValue(indexName)) 
						{
							super.addIndex(dbid, objectId, indexId, indexName);
							
							// In Postgres an index is it's OWN relation
							// We need to add it as an Object as well
							ObjectInfo indexObjectInfo = new ObjectInfo(dbid, dbname, schemaId, schemaName, indexId, indexName, ObjectType.INDEX);
							super.setObjectInfo(dbid, indexId, indexObjectInfo);
						}
						
					} // end: rs.next()
				}
			}
			catch (SQLException ex)
			{
				long execTime = TimeUtils.msDiffNow(execStartTime);

				// jdbc.setQueryTimeout() causes: MsgText=...query has timed out...
				// (NOTE: This is for SQL Server... need to find/test similar for Postgres)
				if ( ex.getErrorCode() == 1222 || (ex.getMessage() != null && ex.getMessage().contains("query has timed out")) )
				{
					_logger.warn("DbmsObjectIdCachePostgres.get(conn, lookupType=" + lookupType + ", dbid=" + dbid + " [dbname=" + dbname + "], lookupId=" + lookupId + "): Problems getting schema/table/index name. The query has timed out (after " + execTime + " ms). But the lock information will still be returned (but without the schema/table/index name.");
					throw new TimeoutException();
				}
				else
				{
					_logger.warn("DbmsObjectIdCachePostgres.get(conn, lookupType=" + lookupType + ", dbid=" + dbid + " [dbname=" + dbname + "], lookupId=" + lookupId + ")): Problems when executing sql: " + sql + ". SQLException Error=" + ex.getErrorCode() + ", Msg='" + StringUtil.stripNewLine(ex.getMessage()) + "', execTime=" + execTime + " ms.", ex);
				}
			}
			finally 
			{
				// IMPORTANT: Give the connection back to the Connection Pool
				CounterSampleCatalogIteratorPostgres.releaseConnection(null, dbConn, dbname);
			}

			if (_logger.isDebugEnabled())
			{
				_logger.debug(" << DbmsObjectIdCachePostgres.get(SINGEL-OBJ): Lookup for: dbid=" + dbid +", lookupId=" + lookupId + ". Returning: objectInfo=" + objectInfo);
			}
//System.out.println("        << DbmsObjectIdCachePostgres.get(SINGEL-OBJ): Lookup for: dbid=" + dbid +", lookupId=" + lookupId + ". Returning: objectInfo=" + objectInfo);

			// Finally: return the object... although the 'super.setObjectInfo(...)' saves/set the mapping to the in-memory Cache
			return objectInfo;
		}
		catch (SQLException ex)
		{
			_logger.error("Problems connecting to Postgres for database '" + dbname + "'. Skipping (SINGEL-OBJ) Lookup for: database='" + dbname + "', dbid=" + dbid +", lookupId=" + lookupId + "'.", ex);
			return null;
		}
	}

//	FIXME; test multidatabase;
//	FIXME: possibly implement; Integer-AS-NULL in CounterSample (but in a first stage, only if option 'isKeepNullFor(jdbType)' is set);
	
	
	
	
	/**
	 * BULK LOAD for Postgres... 
	 */
	@Override
	protected void getBulk(DbxConnection templateConn, Set<String> dbnameSet)
	{
		if (templateConn == null)
			throw new RuntimeException("DbmsObjectIdCacheSqlServer.getBulk(...): Connection can't be null.");

		// Clear all cached values
		super.clear();
		
		// First: get all databases
		Map<Long, String> dbNameMap = new LinkedHashMap<>();
//		String sql = "SELECT oid, datname FROM pg_database";
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
				
				dbNameMap.put(dbid, dbname);
				
				// Special case... 
				// If database is 'postgres' should we add a entry 'dbid=0', so we can lookup system tables (in case the datid in pg_locks is 0)
				// ---- Lest do this in the future if we KEEP seeing this behavior (because I don't know the side effect right now)
				//if ("postgres".equals(dbname))
				//	dbNameMap.put(0L, dbname);
			}
		}
		catch(SQLException ex)
		{
			_logger.error("Problems when executing sql: " + sql + ". SQLException Error=" + ex.getErrorCode() + ", Msg='" + StringUtil.stripNewLine(ex.getMessage()) + "'.", ex);
			return;
		}
		
		// Set the DB Map in super
		super._dbNamesMap = dbNameMap;
//System.out.println(">>>>>>>>> DbmsObjectIdCachePostgres.getBulk(): dbNameMap.size()="+dbNameMap.size()+", dbNameMap="+dbNameMap);


		// Since we need a connection to each of the databases... setup a connection pool, which will hold connections!
		// Note: This ConnectionPool will be reused by some CM's later on
		if ( ! DbxConnectionPoolMap.hasInstance() )
			DbxConnectionPoolMap.setInstance(new DbxConnectionPoolMap());


		// Second: Loop all databases and get info...
		boolean doLookup = true;
		if (doLookup)
		{
			for (Entry<Long, String> e : dbNameMap.entrySet())
			{
				long   dbid   = e.getKey();
				String dbname = e.getValue();

				// If we just wanted to fetch SOME databases
				if (dbnameSet != null && !dbnameSet.contains(dbname))
					continue;
				
				int objectIdsRead = 0;
				int totalRowsRead = 0;

				long startTime = System.currentTimeMillis();
				
				// Get a Connection Pool for this specific database
//				DbxConnectionPool connPool = DbxConnectionPoolMap.getInstance().getPool(dbname);
//				if (connPool == null)
				if ( ! DbxConnectionPoolMap.hasInstance() )
				{
					_logger.info("Skipping BULK load of ObjectId's for database '" + dbname + "', no Connection Pool Map was found.");
					continue;
				}

				// CONNECT TO the database (use some Connection Pool)
				try
				{
					DbxConnection dbConn = CounterSampleCatalogIteratorPostgres.getConnection(null, templateConn, dbname);
//					sql = ""
//							+ "SELECT \n"
//							+ "     nsp.oid as schema_id \n"
//							+ "    ,nsp.nspname as schema_name \n"
//							+ "    ,tbl.oid as relid \n"
//							+ "    ,tbl.relname as table_name \n"
//							+ "    ,tbl.relkind \n"
//							+ "FROM pg_namespace nsp \n"
//							+ "JOIN pg_class tbl ON nsp.oid = tbl.relnamespace \n"
//							+ "WHERE tbl.relkind IN('r', 'm', 'p') \n"
//							+ "";
					sql = ""
						    + "SELECT \n"
						    + "     ns.oid       AS schema_id \n"   // 1
						    + "    ,ns.nspname   AS schema_name \n" // 2
						    + "    ,tc.oid       AS object_id \n"   // 3
						    + "    ,tc.relname   AS object_name \n" // 4
						    + "    ,i.indexrelid AS index_id \n"    // 5
						    + "    ,ic.relname   AS index_name \n"  // 6
						    + "    ,tc.relkind   AS object_type \n" // 7
						    + "FROM pg_class              tc \n"
						    + "  JOIN pg_namespace        ns ON ns.oid = tc.relnamespace \n"
						    + "  LEFT OUTER JOIN pg_index i  ON tc.oid       = i.indrelid \n"
						    + "  LEFT OUTER JOIN pg_class ic ON i.indexrelid = ic.oid \n"
						    + "WHERE 1 = 1 \n"
//						    + "  AND tc.relkind in('r', 'm', 'p') \n"
//						    + "   OR tc.relname = 'pg_locks' \n" // This one will be visible in Counter 'CmPgLocks'
						    + "ORDER BY tc.oid, i.indexrelid \n"
						    + "";
							// ----------------------------------------
							// relkind: 
							// ----------------------------------------
							// 'r' = ordinary table
							// 'i' = index, S = sequence, 
							// 't' = TOAST table, 
							// 'v' = view, 
							// 'm' = materialized view, 
							// 'c' = composite type, 
							// 'f' = foreign table, 
							// 'p' = partitioned table, 
							// 'I' = partitioned index
							// ----------------------------------------

					long objectId_save = Long.MIN_VALUE; // Changed when a new ObjectId is found
					try (Statement stmnt = dbConn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
					{
						ObjectInfo objectInfo = null;

						while(rs.next())
						{
							totalRowsRead++;

							long   schemaId    = rs.getLong  (1);
							String schemaName  = rs.getString(2);
							long   objectId    = rs.getLong  (3);
							String objectName  = rs.getString(4);
							long   indexId     = rs.getLong  (5);
							String indexName   = rs.getString(6);
							String relKind     = rs.getString(7);

							// First row for each ObjectId
							if (objectId != objectId_save)
							{
								objectIdsRead++;
								objectId_save = objectId;

								// Transform to a normalized ObjectType
								ObjectType objectType = getObjectType(schemaName, relKind);
								
								// Create a new ObjectInfo
								objectInfo = new ObjectInfo(dbid, dbname, schemaId, schemaName, objectId, objectName, objectType);

//								fixme; this needs more work 
								
								// In the super (which is/holds the cache)... add the info...
								// and yes we can do that because the Index Id/Name has it's own Map
								super.setObjectInfo(dbid, objectId, objectInfo);
							}

							// On consequent rows: Just add the indexes (if we got any)
							if (StringUtil.hasValue(indexName)) 
							{
								super.addIndex(dbid, objectId, indexId, indexName);
								
								// In Postgres an index is it's OWN relation
								// We need to add it as an Object as well
								ObjectInfo indexObjectInfo = new ObjectInfo(dbid, dbname, schemaId, schemaName, indexId, indexName, ObjectType.INDEX);
								super.setObjectInfo(dbid, indexId, indexObjectInfo);
							}
						} // end: rs.next()

						_statBulkPhysicalReads++;
						long execTimeMs = TimeUtils.msDiffNow(startTime);
						
						_logger.info("DONE: Getting ObjectNames in BULK mode from database '" + dbname + "'. Read " + objectIdsRead + " Obects/Tables, Index Names " + (totalRowsRead - objectIdsRead) + " and totally " + totalRowsRead + " was read. execTimeMs=" + execTimeMs);
						
						if (_logger.isDebugEnabled())
							_logger.debug("Entries in DBMS ObjectId Cache after loading database '" + dbname + "'. \n" + debugPrintAllObject());

//System.out.println("Entries in DBMS ObjectId Cache after loading database '" + dbname + "'. \n" + debugPrintAllObject());
					}
					catch(SQLException ex)
					{
						// Azure: Error=40515, Msg='Reference to database and/or server name in 'master.sys.objects' is not supported in this version of SQL Server.'
						if (ex.getErrorCode() == 40515)
							_logger.info("Skipping BULK load of ObjectId's for database '" + dbname + "', Error 40515 should only happen in Azure environments where we dont have access to all databases. Error=" + ex.getErrorCode() + ", Msg=|" + ex.getMessage() + "|.");
						else
							_logger.error("Skipping BULK load of ObjectId's for database '" + dbname + "', continuing with next database. Problems when executing sql: " + sql + ". SQLException Error=" + ex.getErrorCode() + ", Msg='" + StringUtil.stripNewLine(ex.getMessage()) + "'.", ex);

//						return;
					}
					finally 
					{
						CounterSampleCatalogIteratorPostgres.releaseConnection(null, dbConn, dbname);
					}
				}
				catch (SQLException ex)
				{
					_logger.error("Problems connecting to Postgres for database '" + dbname + "'. Skipping BULK load of ObjectId's for database '" + dbname + "'.", ex);
				}
			} // end: DB-Loop
		} // end: Do lookup
	} // end: method

	
	/**
	 * Transform a Postgres schemaName and relKind to a more "normalized" object type
	 * 
	 * @param schemaName
	 * @param relKind
	 * @return
	 */
	public static ObjectType getObjectType(String schemaName, String relKind)
	{
		// System tables... can be figured out from schema name
		if ("pg_catalog"        .equals(schemaName)) return ObjectType.SYSTEM_TABLE;
		if ("information_schema".equals(schemaName)) return ObjectType.SYSTEM_TABLE;

		// Postgres 'relkind'
		//   r = ordinary table
		//   i = index
		//   S = sequence
		//   t = TOAST table
		//   v = view
		//   m = materialized view
		//   c = composite type
		//   f = foreign table
		//   p = partitioned table
		//   I = partitioned index
		if ("r".equals(relKind)) return ObjectType.BASE_TABLE;
		if ("i".equals(relKind)) return ObjectType.INDEX;
		if ("S".equals(relKind)) return ObjectType.SEQUENCE;
		if ("t".equals(relKind)) return ObjectType.LOB_TABLE;
		if ("v".equals(relKind)) return ObjectType.VIEW;
		if ("m".equals(relKind)) return ObjectType.MATERIALIZED_VIEW;
//		if ("c".equals(relKind)) return ObjectType.XXX;
		if ("f".equals(relKind)) return ObjectType.REMOTE_TABLE;
		if ("p".equals(relKind)) return ObjectType.PARTITIONED_TABLE;
		if ("I".equals(relKind)) return ObjectType.INDEX;

		return ObjectType.UNKNOWN;
	}
}
