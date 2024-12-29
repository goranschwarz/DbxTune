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

import org.apache.log4j.Logger;

import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.ConnectionProvider;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

public class DbmsObjectIdCacheSqlServer 
extends DbmsObjectIdCache
{
	private static Logger _logger = Logger.getLogger(DbmsObjectIdCacheSqlServer.class);

	public static final String  PROPKEY_BulkLoadOnStart = "SqlServerTune.objectIdCahe.bulkLoadOnStart";
	public static final boolean DEFAULT_BulkLoadOnStart = true;  // for testing just now...
//	public static final boolean DEFAULT_BulkLoadOnStart = false; // if we want to revert back for X number of CM's to use SQL Server direct lookups of: sys.objects, etc...

	public static final String  PROPKEY_lockTimeoutMs = "SqlServerTune.objectIdCahe.lockTimeout";
	public static final int     DEFAULT_lockTimeoutMs = 1000;

	public DbmsObjectIdCacheSqlServer(ConnectionProvider connProvider)
	{
		super(connProvider);
	}

	@Override
	public boolean isBulkLoadOnStartEnabled()
	{
		return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_BulkLoadOnStart, DEFAULT_BulkLoadOnStart);
	}

	public static final String SCHEMA_DBO = "dbo";
	public static final String SCHEMA_SYS = "sys";

	@Override
	protected Map<String, String> createStaticSchemaNames()
	{
		Map<String, String> map = new HashMap<>();

		map.put(SCHEMA_DBO, SCHEMA_DBO);
		map.put(SCHEMA_SYS, SCHEMA_SYS);
		
		return map;
	}

	@Override
	protected void onConnect(DbxConnection conn)
	{
		int desiredLockTimeoutMs = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_lockTimeoutMs, DEFAULT_lockTimeoutMs);

		String getSql = "select @@lock_timeout";
		String setSql = "SET LOCK_TIMEOUT " + desiredLockTimeoutMs;
		String sql;

		// Make a default value, that we are NOT in "dirty read"
		int currentLockTimeout = Integer.MIN_VALUE;

		//------- GET/CHECK
		sql = getSql;
		try(Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
				currentLockTimeout = rs.getInt(1); // <0 = Unlimited, 0=TimeoutImmediately, >0 = Timeout after #### ms
		}
		catch(SQLException e)
		{
			_logger.error("Problem when CHECKING the SQL-Server 'lock timeout' for DbmsObjectIdCacheSqlServer.onConnect(). SQL="+sql);
		}
		
		//------- SET LOCK_TIMEOUT
		if ( currentLockTimeout != desiredLockTimeoutMs )
		{
			sql = setSql;
			try(Statement stmnt = conn.createStatement())
			{
				_logger.info("SETTING 'lock timeout' from " + currentLockTimeout + " to " + desiredLockTimeoutMs + " for DbmsObjectIdCacheSqlServer.onConnect(). executing SQL="+sql);
				stmnt.executeUpdate(sql);
			}
			catch(SQLException e)
			{
				_logger.error("Problem when SETTING the SQL-Server 'lock timeout' for DbmsObjectIdCacheSqlServer.onConnect(). SQL="+sql);
			}
		}
	}
	
	@Override
//	protected ObjectInfo get(DbxConnection conn, int dbid, int objectid) 
	protected ObjectInfo get(DbxConnection conn, LookupType lookupType, long dbid, Number lookupId) 
	throws TimeoutException
	{
		if (conn == null)
			return null;

		//  dbid == 32768 -- is "mssqlsystemresource", which is a hidden database
		if (dbid == 32767)
			return null;

		// Get database name from ID, if nor found... go and get it
		String dbname = getDBName(dbid);
		if (StringUtil.isNullOrBlank(dbname))
		{
			String sql = "select db_name(" + dbid + ")";
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
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

		String whereClause = "Unknown-Lookup-Type-" + lookupType;
		if      (LookupType.ObjectId   .equals(lookupType)) whereClause = "where o.object_id    = ? \n";
		else if (LookupType.PartitionId.equals(lookupType)) whereClause = "where p.partition_id = ? \n";
		else if (LookupType.HobtId     .equals(lookupType)) whereClause = "where p.hobt_id      = ? \n";
		else throw new RuntimeException("Unknown Lookup Type '" + lookupType + "'.");
		
		String sql = ""
			    + "select \n"
			    + "     s.schema_id \n"
			    + "    ,s.name AS SchemaName \n"
			    + "    ,o.object_id \n"
			    + "    ,o.name AS ObjectName \n"
			    + "    ,i.index_id \n"
			    + "    ,i.name AS IndexName \n"
			    + "    ,p.partition_id \n"
			    + "    ,p.hobt_id \n"
			    + "from            [" + dbname + "].sys.objects    o WITH (READUNCOMMITTED) \n"
			    + "inner join      [" + dbname + "].sys.partitions p WITH (READUNCOMMITTED) ON o.object_id = p.object_id \n"
			    + "inner join      [" + dbname + "].sys.schemas    s WITH (READUNCOMMITTED) ON o.schema_id = s.schema_id \n"
			    + "left outer join [" + dbname + "].sys.indexes    i WITH (READUNCOMMITTED) ON o.object_id = i.object_id \n"
//			    + "where o.object_id = ? \n"
			    + whereClause
			    + "";
//FIXME; we also need to get it by 'hobt_id' or 'partition_id'
//should we add 3 new parameters or a HashMap with key value (to get it a bit LESS DBMS Specific)
		
		// return object
		ObjectInfo objectInfo = null;
		long execStartTime = System.currentTimeMillis();

		try (PreparedStatement pstmnt = conn.prepareStatement(sql)) // Auto CLOSE
		{
			// Timeout after 2 second --- if we get blocked when doing: object_name()
			pstmnt.setQueryTimeout(2);
			
			// set lookup ID
			if      (LookupType.ObjectId   .equals(lookupType)) pstmnt.setInt (1, lookupId.intValue());
			else if (LookupType.PartitionId.equals(lookupType)) pstmnt.setLong(1, lookupId.longValue());
			else if (LookupType.HobtId     .equals(lookupType)) pstmnt.setLong(1, lookupId.longValue());


			try (ResultSet rs = pstmnt.executeQuery()) // Auto CLOSE
			{
				while(rs.next())
				{
					int    schemaId    = rs.getInt   (1);
					String schemaName  = rs.getString(2);
					int    objectId    = rs.getInt   (3);
					String objectName  = rs.getString(4);
					int    indexId     = rs.getInt   (5);
					String indexName   = rs.getString(6);
					long   partitionId = rs.getInt   (7);
					long   hobtId      = rs.getInt   (8);

					if (objectInfo == null)
					{
//						objectInfo = new ObjectInfo(dbid, dbname, schemaId, schemaName, objectId, objectName, indexId, indexName);
						objectInfo = new ObjectInfo(dbid, dbname, schemaId, schemaName, objectId, objectName, ObjectType.BASE_TABLE);
						super.setObjectInfo(dbid, objectId, objectInfo);
					}

					// On consequent rows: Just add the indexes
//					objectInfo.addIndex(indexId, indexName);
//					objectInfo.addPartition(partitionId);
//					objectInfo.addHobt(hobtId);
					super.addIndex    (dbid, objectId, indexId, indexName);
					super.addPartition(dbid, objectId, partitionId);
					super.addHobt     (dbid, objectId, hobtId);
					
				} // end: rs.next()
				
				// Add the ObjectInfo to parent (which holds the cache)
				// NOTE: This is done at the "super"
				//if (objectInfo != null)
				//	set(dbid, objectid, objectInfo);
			}
		}
		catch (SQLException ex)
		{
			long execTime = TimeUtils.msDiffNow(execStartTime);
			
			// SET LOCK_TIMEOUT ### causes:   ErrorCode=1222, MsgText=Lock request time out period exceeded.
			// jdbc.setQueryTimeout() causes:                 MsgText=...query has timed out...
			if ( ex.getErrorCode() == 1222 || (ex.getMessage() != null && ex.getMessage().contains("query has timed out")) )
			{
				_logger.warn("DbmsObjectIdCacheSqlServer.get(conn, lookupType=" + lookupType + ", dbid=" + dbid + " [dbname=" + dbname + "], lookupId=" + lookupId + "): Problems getting schema/table/index name. The query has timed out (after " + execTime + " ms). But the lock information will still be returned (but without the schema/table/index name.");
				throw new TimeoutException();
			}
			// Msg 208, Text='Invalid object name '${dbname}.sys.objects'.'
			else if ( ex.getErrorCode() == 208 )
			{
				_logger.warn("DbmsObjectIdCacheSqlServer.get(conn, lookupType=" + lookupType + ", dbid=" + dbid + " [dbname=" + dbname + "], lookupId=" + lookupId + "): Problems when executing sql: " + sql + ". SQLException Error=" + ex.getErrorCode() + ", Msg='" + StringUtil.stripNewLine(ex.getMessage()) + "', execTime=" + execTime + " ms. Clear the ObjectIdCache for dbid=" + dbid, ex);
				clearCacheForDbid(dbid);
			}
			else
			{
				_logger.warn("DbmsObjectIdCacheSqlServer.get(conn, lookupType=" + lookupType + ", dbid=" + dbid + " [dbname=" + dbname + "], lookupId=" + lookupId + ")): Problems when executing sql: " + sql + ". SQLException Error=" + ex.getErrorCode() + ", Msg='" + StringUtil.stripNewLine(ex.getMessage()) + "', execTime=" + execTime + " ms.", ex);
			}
		}
		
		return objectInfo;
	}

	@Override
	protected void getBulk(DbxConnection conn, Set<String> dbnameSet)
	{
//		throw new RuntimeException("DbmsObjectIdCacheSqlServer.getBulk(...): NOT YET IMPLEMENTED");
		
		if (conn == null)
			throw new RuntimeException("DbmsObjectIdCacheSqlServer.getBulk(...): Connection can't be null.");

//		if (dbnameSet != null)
//		{
//			_logger.warn("Sorry: Bulk refresh on individual databases are not supported. ALL Databases will be loaded into cache!");
//		}
		
		// Clear all cached values
		super.clear();
		
		// First: get all databases
		Map<Long, String> dbNameMap = new LinkedHashMap<>();
		String sql = "select database_id, name from sys.databases WITH (READUNCOMMITTED)";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
			{
				long   dbid   = rs.getLong  (1);
				String dbname = rs.getString(2);
				
				dbNameMap.put(dbid, dbname);
			}
		}
		catch(SQLException ex)
		{
			_logger.error("Problems when executing sql: " + sql + ". SQLException Error=" + ex.getErrorCode() + ", Msg='" + StringUtil.stripNewLine(ex.getMessage()) + "'.", ex);
			return;
		}
		
		// Set the DB Map in super
		super._dbNamesMap = dbNameMap;
		
		// Second: Loop all databases and get info...
		for (Entry<Long, String> e : dbNameMap.entrySet())
		{
			long   dbid   = e.getKey();
			String dbname = e.getValue();

			// If we just wanted to fetch SOME databases
			if (dbnameSet != null && !dbnameSet.contains(dbname))
				continue;
			
			int objectIdsRead = 0;
			int totalRowsRead = 0;
//			_logger.info("Getting Object Names in BULK mode from database '" + dbname + "' (only for System and User Tables).");
			long startTime = System.currentTimeMillis();
			
			sql = ""
				    + "select \n"
				    + "     s.schema_id \n"
				    + "    ,s.name         AS SchemaName \n"
				    + "    ,o.object_id \n"
				    + "    ,o.name         AS ObjectName \n"
				    + "    ,i.index_id \n"
				    + "    ,i.name         AS IndexName \n"
				    + "    ,p.partition_id \n"
				    + "    ,p.hobt_id \n"
				    + "from            [" + dbname + "].sys.objects    o WITH (READUNCOMMITTED) \n"
				    + "inner join      [" + dbname + "].sys.partitions p WITH (READUNCOMMITTED) ON o.object_id = p.object_id \n"
				    + "inner join      [" + dbname + "].sys.schemas    s WITH (READUNCOMMITTED) ON o.schema_id = s.schema_id \n"
				    + "left outer join [" + dbname + "].sys.indexes    i WITH (READUNCOMMITTED) ON o.object_id = i.object_id \n"
				    + "where o.type in ('U', 'S') \n" // only User Tables and System Tables
				   	+ "order by o.object_id \n"       // The order by is probably redundant (since it comes in this order without the order by... it's just for safety
				   	+ "";
			
			long objectId_save = Long.MIN_VALUE; // Changed when a new ObjectId is found
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
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
					long   partitionId = rs.getLong  (7);
					long   hobtId      = rs.getLong  (8);

					// First row for each ObjectId
					if (objectId != objectId_save)
					{
						objectIdsRead++;
						objectId_save = objectId;

						// Create a new ObjectInfo
						objectInfo = new ObjectInfo(dbid, dbname, schemaId, schemaName, objectId, objectName, ObjectType.BASE_TABLE);

//						fixme; this needs more work 
						
						// In the super (which is/holds the cache)... add the info...
						// and yes we can do that because the Index Id/Name has it's own Map
		    			super.setObjectInfo(dbid, objectId, objectInfo);
					}

					// On consequent rows: Just add the indexes
//					objectInfo.addIndex(indexId, indexName);
//					objectInfo.addPartition(partitionId);
//					objectInfo.addHobt(hobtId);
					super.addIndex    (dbid, objectId, indexId, indexName);
					super.addPartition(dbid, objectId, partitionId);
					super.addHobt     (dbid, objectId, hobtId);

				} // end: rs.next()

	    		_statBulkPhysicalReads++;
				long execTimeMs = TimeUtils.msDiffNow(startTime);
				
	    		_logger.info("DONE: Getting ObjectNames in BULK mode from database '" + dbname + "'. Read " + objectIdsRead + " Obects/Tables, Index Names " + (totalRowsRead - objectIdsRead) + " and totally " + totalRowsRead + " was read. execTimeMs=" + execTimeMs);
			}
			catch(SQLException ex)
			{
				if (ex.getErrorCode() == 40515)
				{
					// Azure: Error=40515, Msg='Reference to database and/or server name in 'master.sys.objects' is not supported in this version of SQL Server.'
					_logger.info("Skipping BULK load of ObjectId's for database '" + dbname + "', Error 40515 should only happen in Azure environments where we dont have access to all databases. Error=" + ex.getErrorCode() + ", Msg=|" + ex.getMessage() + "|.");
				}
				else if (ex.getErrorCode() == 976)
				{
					// Azure: Error=976, Msg='The target database, 'xxx', is participating in an availability group and is currently not accessible for queries. Either data movement is suspended or the availability replica is not enabled for read access. To allow read-only access to this and other databases in the availability group, enable read access to one or more secondary availability replicas in the group.  For more information, see the ALTER AVAILABILITY GROUP statement in SQL Server Books Online.'
					_logger.info("Skipping BULK load of ObjectId's for database '" + dbname + "', Error 976 should only happen in Availability Group where we dont have access to standby/failover database. Error=" + ex.getErrorCode() + ", Msg=|" + ex.getMessage() + "|.");
				}
				else
				{
					// Unknown, LOG the Exception
					_logger.error("Skipping BULK load of ObjectId's for database '" + dbname + "', continuing with next database. Problems when executing sql: " + sql + ". SQLException Error=" + ex.getErrorCode() + ", Msg='" + StringUtil.stripNewLine(ex.getMessage()) + "'.", ex);
				}

//				return;
			}
		}
	}
}
