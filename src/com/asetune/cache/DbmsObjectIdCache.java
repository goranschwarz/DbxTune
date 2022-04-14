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
package com.asetune.cache;

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.asetune.Version;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.TimeUtils;

public abstract class DbmsObjectIdCache
{
	private static Logger _logger = Logger.getLogger(DbmsObjectIdCache.class);

	public static final String  PROPKEY_lowOnMememory_removePct  = "DbmsObjectIdCache.lowOnMememory.removePct";
	public static final int     DEFAULT_lowOnMememory_removePct  = 20;

	public static final String SCHEMA_DBO = "dbo";
	public static final String SCHEMA_SYS = "sys";

	public enum LookupType
	{
		/** Object ID */
		ObjectId, 

		/** Partitioned Table ID */
		PartitionId, 
		
		/** Heap O BinaryTree ID */
		HobtId
	};
	
	/** Data Record -- ObjectInfo */
	public static class ObjectInfo
	{
		private int    _dbid;
		private String _dbname;

		private int    _schemaId;
		private String _schemaName;

		private int    _objectId;
		private String _objectName;
		
		// possibly also Index names
		private Map<Integer, String> _indexNames;
		
		// Partitions
		private Set<Long> _partitionIds;
		
		// Partitions
		private Set<Long> _hobtIds;
		
//		private long _lastAccessTime;  // not yet implemented
		

		/** get an <b>estimate</b> of how much memory this object consumes */
		public int getMemorySize()
		{
			int size = 0; 
			
			// db(4) + dbname(36=overhead + strLen*2)
			size += 4 + 36 + (_dbname == null ? 0 : _dbname.length() * 2); 

			// schema(4) + schemaName(36=overhead + strLen*2)
			size += 4 + 36 + (_schemaName == null ? 0 : _schemaName.length() * 2); 

			// object(4) + schemaName(36=overhead + strLen*2)
			size += 4 + 36 + (_objectName == null ? 0 : _objectName.length() * 2); 

			// Index info
			for (Entry<Integer, String> entry : _indexNames.entrySet())
			{
				size += 4; // index id
				size += entry.getValue().length() * 2; // index name
			}
			
			size += _partitionIds == null ? 0 : _partitionIds.size() * 8;

			size += _hobtIds      == null ? 0 : _hobtIds.size()      * 8;
			
			return size;
		}
		
//		public ObjectInfo(int dbid, String dbname, int schemaId, String schemaName, int objectId, String objectName, int indexId, String indexName)
		public ObjectInfo(int dbid, String dbname, int schemaId, String schemaName, int objectId, String objectName)
		{
			_dbid       = dbid;       
			_dbname     = dbname;     

			// maybe this helps memory consumption a bit for 'dbo' and 'sys' schemas, since we are making it a "singleton"
			if      (SCHEMA_DBO.equals(schemaName)) schemaName = SCHEMA_DBO;
			else if (SCHEMA_SYS.equals(schemaName)) schemaName = SCHEMA_SYS;
			
			_schemaId   = schemaId;   
			_schemaName = schemaName; 

			_objectId   = objectId;   
			_objectName = objectName; 
			
//			addIndex(indexId, indexName);
		}
		
		public String addIndex(int indexId, String indexName)
		{
			if (indexName == null)
				return null;

			if (_indexNames == null)
				_indexNames = new LinkedHashMap<>();
			
			return _indexNames.put(indexId, indexName);
		}
		
		public boolean addPartition(long partitionId)
		{
			if (_partitionIds == null)
				_partitionIds = new HashSet<>();
			
			return _partitionIds.add(partitionId);
		}
		
		public boolean addHobt(long hobtId)
		{
			if (_hobtIds == null)
				_hobtIds = new HashSet<>();
			
			return _hobtIds.add(hobtId);
		}
		
		public int    getDbid()       { return _dbid;       }
		public String getDBName()     { return _dbname;     }
		public int    getSchemaId()   { return _schemaId;   }
		public String getSchemaName() { return _schemaName; }
		public int    getObjectId()   { return _objectId;   }
		public String getObjectName() { return _objectName; }

		/** Get index Map, which is a Map of IndexId, IndexName */
		public Map<Integer, String> getIndexMap() 
		{
			if (_indexNames == null)
				return Collections.emptyMap();
		
			return _indexNames; 
		}

		/** Get index name for a index ID */
		public String getIndexName(int indexId) 
		{ 
			if (indexId == 0)
				return "HEAP";

			if (_indexNames == null)
				return null;

			return _indexNames.get(indexId);
		}

		public Set<Long> getPartitionSet() { return _partitionIds; }
		public Set<Long> getHobtSet()      { return _hobtIds; }
	}

	private ConnectionProvider _connProvider = null;

	protected Map<Integer, String>                   _dbNamesMap       = new HashMap<>();
	private   Map<Integer, Map<Integer, ObjectInfo>> _dbid_objectId    = new HashMap<>();
	private   Map<Integer, Map<Long, ObjectInfo>>    _dbid_partitionId = new HashMap<>();
	private   Map<Integer, Map<Long, ObjectInfo>>    _dbid_hobtId      = new HashMap<>();
	
	/** Keep a local DBMS Connection for the lookups */
	private DbxConnection _localConnection;

	protected long _statLogicalRead       = 0;    // How many times did we read from the cache 
	protected long _statPhysicalRead      = 0;    // How many times did we read from the back-end storage
	protected long _statBulkPhysicalReads = 0;    // How many times did we call getPlanBulk()
	protected long _statLogicalWrite      = 0;    // How many times did we call setPlan()

	private   long _statReportModulus     = 5000; // When it gets crossed it will double itself, but max is always _statReportModulusMax
	private   long _statReportModulusMax  = 100000;
//	private   long _statReportModulus     = 500;   // <<<--- for TEST purposes
//	private   long _statReportModulusMax  = 10000; // <<<--- for TEST purposes
	// Or possibly change to report when X minutes has passed... since last report print

	protected long _statResetCalls        = 0;    // How many times did we call outOfMemoryHandler()
	protected long _statDecreaseCalls     = 0;    // How many times did we call lowOfMemoryHandler()

	//----------------------------------------------------------------
	// BEGIN: instance
	private static DbmsObjectIdCache _instance = null;
	public static DbmsObjectIdCache getInstance()
	{
		if (_instance == null)
		{
			throw new RuntimeException("DbmsObjectIdCache dosn't have an instance yet, please set with setInstance(instance).");
		}
		return _instance;
	}
	public static void setInstance(DbmsObjectIdCache instance)
	{
		_instance = instance;
	}
	public static boolean hasInstance()
	{
		return _instance != null;
	}
	// END: instance
	//----------------------------------------------------------------

	//----------------------------------------------------------------
	// BEGIN: Constructors
	public DbmsObjectIdCache(ConnectionProvider connProvider)
	{
		_connProvider = connProvider;
	}
	// END: Constructors
	//----------------------------------------------------------------

	/** 
	 * Close and release all object 
	 */
	public void close()
	{
		_dbNamesMap       = null;
		_dbid_objectId    = null;
		_dbid_partitionId = null;
		_dbid_hobtId      = null;
		
		if (_localConnection != null)
			_localConnection.closeNoThrow();
		
		_instance = null;
	}
	
	/** 
	 * How many entries is in the cache
	 */ 
	public int size()
	{
		int size = 0;

		for (Map<Integer, ObjectInfo> e : _dbid_objectId.values())
			size += e.size();

		return size;
	}

	/** 
	 * How many entries do we have per database 
	 */ 
	public Map<String, Integer> sizePerDb()
	{
		Map<String, Integer> dbSizeMap = new LinkedHashMap<>();

		for (Entry<Integer, Map<Integer, ObjectInfo>> e : _dbid_objectId.entrySet())
		{
			Integer dbid = e.getKey();
			Map<Integer, ObjectInfo> objInfoPerDb = e.getValue();

			if (dbid != null)
			{
				String dbname = _dbNamesMap.get(dbid);
				int    size   = objInfoPerDb.size();

				dbSizeMap.put(dbname, size);
			}

		}

		return dbSizeMap;
	}

	/**
	 * Clear all elements from the cache
	 */
	public void clear()
	{
		_dbNamesMap       = new HashMap<>();
		_dbid_objectId    = new HashMap<>();
		_dbid_partitionId = new HashMap<>();
		_dbid_hobtId      = new HashMap<>();

		_statResetCalls++;
	}

	/**
	 * Get some information about how much memory is used by this cache.
	 * @return A string with information.
	 */
	public String getMemoryConsumption()
	{
		int entryCount  = 0;
		int usedMemory  = 0;
		int avgPerEntry = 0;

		if (_dbid_objectId != null)
		{
			for (Entry<Integer, Map<Integer, ObjectInfo>> dbidEntry : _dbid_objectId.entrySet())
			{
				for (Entry<Integer, ObjectInfo> objIdEntry : dbidEntry.getValue().entrySet())
				{
					entryCount++;
					usedMemory += objIdEntry.getValue().getMemorySize();
				}
			}
		}
		
		if (entryCount > 0) 
			avgPerEntry = usedMemory / entryCount;

		return this.getClass().getSimpleName() + ": entryCount=" + entryCount + ", usedMem=[" + usedMemory + "b, " + (usedMemory/1024) + "KB, " + (usedMemory/1024/1024) + "MB], avgPerEntry=" + avgPerEntry;
	}

	/**
	 * In case someone finds out that we are low on memory... 
	 */
	public void outOfMemoryHandler()
	{
		_logger.info("Clearing all content from the DbmsObjectIdCache. Number of entries before clean was " + size());
		_statResetCalls++;

		// Simply create a new _cache to clear it.
	//	_dbNamesMap    = new HashMap<>();
		_dbid_objectId = new HashMap<>();
	}

	/**
	 * In case someone finds out that we are low on memory... 
	 */
	public void lowOnMemoryHandler()
	{
		// Get remove percent from configuration
		int removePct = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_lowOnMememory_removePct, DEFAULT_lowOnMememory_removePct);
		
		int countBeforeClean = size();
		int removeCount = (int) (countBeforeClean * (removePct / 100.0));

		// Since this structure isn't really protected, we can still get ConcurrentModificationException
		// if other threads accesses this structure at the same time...
		// Don't care about this, just catch it and continue...
		try
		{
			if (removeCount < 20)
			{
				// If small enough chunk, lets just create a new cache...
			//	_dbNamesMap       = new HashMap<>();
				_dbid_objectId    = new HashMap<>();
				_dbid_partitionId = new HashMap<>();
				_dbid_hobtId      = new HashMap<>();
			}
			else
			{
				// Outer "dbid entries" wont be removed (we could probably do this better by "randomize" what DBID we visit (or have some MRU strategy)
				for (Map<Integer, ObjectInfo> dbidEntry : _dbid_objectId.values())
				{
					// Just the "inner" entries... and for that we need a Iterator (to call remove) 
					Iterator<ObjectInfo> it = dbidEntry.values().iterator();
					while(it.hasNext())
					{
						ObjectInfo oi = it.next(); // Must be called before we can call remove()

						// Remove entries from the PartitionMap
						if (oi.getPartitionSet() != null)
						{
							Map<Long, ObjectInfo> partitionMap = _dbid_partitionId.get(oi.getDbid());
							if (partitionMap != null)
							{
								for (Long partitionId : oi.getPartitionSet())
									partitionMap.remove(partitionId);
							}
						}

						// Remove entries from the HobtMap
						if (oi.getHobtSet() != null)
						{
							Map<Long, ObjectInfo> hobtMap = _dbid_hobtId.get(oi.getDbid());
							if (hobtMap != null)
							{
								for (Long hobtId : oi.getHobtSet())
									hobtMap.remove(hobtId);
							}
						}

						// remove entry from ObjectIdMap
						it.remove();
						removeCount--;
						if (removeCount <= 0)
							break;
					}
				}
			}
		}
		catch(ConcurrentModificationException ex)
		{
			_logger.warn("DbmsObjectIdCache, lowOnMemoryHandler(): when removing entries from the cache, we caught ConcurrentModificationException... lets just continue... Exception: "+ex);
		}

		int countAfterClean  = size();
		removeCount = countBeforeClean - countAfterClean;

		_logger.warn("DbmsObjectIdCache, lowOnMemoryHandler() was called. Removed "+removeCount+" entries from the DbmsObjectIdCache (config: '"+PROPKEY_lowOnMememory_removePct+"'="+removePct+"). Number of entries before clean was "+countBeforeClean+", after clean was "+countAfterClean+".");
		_statDecreaseCalls++;
	}

	/**
	 * Check if the ID is Cached (does not do any physical IO)
	 * @param dbid
	 * @param objectid
	 * @return true or false
	 */
	public boolean isCached(int dbid, int objectid)
	{
		_statLogicalRead++;
		if ( (_statLogicalRead % _statReportModulus) == 0 )
			printStatistics();

		Map<Integer, ObjectInfo> objIdMap = _dbid_objectId.get(dbid);
		if (objIdMap == null)
			return false;

		return objIdMap.containsKey(objectid);
	}

	/**
	 * Get the DBName from cache 
	 * @param dbid
	 * @return
	 */
	public String getDBName(int dbid)
	{
//		if (dbid == 32767)
//			return "mssqlsystemresource";

		return _dbNamesMap.get(dbid);
	}

	/**
	 * Set the DBName in cache 
	 * @param dbid
	 * @param dbname
	 * @return
	 */
	public String setDBName(int dbid, String dbname)
	{
		return _dbNamesMap.put(dbid, dbname);
	}

	/**
	 * 
	 * @param dbid
	 * @param objectid
	 * @param indexid
	 * @return IndexName if it was found. Null if the ID wasn't found
	 * @throws TimeoutException
	 */
	public String getIndexName(int dbid, int objectid, int indexid) 
	throws TimeoutException
	{
		ObjectInfo objectInfo = getByObjectId(dbid, objectid);
		
		if (objectInfo == null)
			return null;

		return objectInfo.getIndexName(indexid);
	}

	/**
	 * 
	 * @param dbid
	 * @param objectid
	 * @return SchemaName if it was found. Null if the ID wasn't found
	 * @throws TimeoutException
	 */
	public String getSchemaName(int dbid, int objectid) 
	throws TimeoutException
	{
		ObjectInfo objectInfo = getByObjectId(dbid, objectid);
		
		if (objectInfo == null)
			return null;

		return objectInfo.getSchemaName();
	}

	/**
	 * 
	 * @param dbid
	 * @param objectid
	 * @return ObjectName if it was found. Null if the ID wasn't found
	 * @throws TimeoutException
	 */
	public String getObjectName(int dbid, int objectid) 
	throws TimeoutException
	{
		ObjectInfo objectInfo = getByObjectId(dbid, objectid);
		
		if (objectInfo == null)
			return null;

		return objectInfo.getObjectName();
	}
	
	/**
	 * Get the content from cache, if it's not in the cache it will call the back-end to get the information 
	 * @param dbid
	 * @param objectid
	 * @return
	 * @throws TimeoutException 
	 */
	public ObjectInfo getByObjectId(int dbid, int objectid) 
	throws TimeoutException
	{
		_statLogicalRead++;
		
		Map<Integer, ObjectInfo> map = _dbid_objectId.get(dbid);
		ObjectInfo entry = (map == null) ? null : map.get(objectid);

		if (entry == null)
		{
			// Get the information from the DBMS
			_statPhysicalRead++;
			entry = get(getConnection(), LookupType.ObjectId, dbid, objectid);
//			if (entry != null)
//			{
//				if (objIdMap == null)
//					objIdMap = new HashMap<Integer, ObjectInfo>();
//
//				objIdMap.put(objectid, entry);
//			}
		}
		if ( (_statLogicalRead % _statReportModulus) == 0 )
			printStatistics();

		return entry;
	}

	public ObjectInfo getByPartitionId(int dbid, long partitionId)
	throws TimeoutException
	{
		_statLogicalRead++;
		
		Map<Long, ObjectInfo> map = _dbid_partitionId.get(dbid);
		ObjectInfo entry = (map == null) ? null : map.get(partitionId);

		if (entry == null)
		{
			// Get the information from the DBMS
			_statPhysicalRead++;
			entry = get(getConnection(), LookupType.PartitionId, dbid, partitionId);
		}
		if ( (_statLogicalRead % _statReportModulus) == 0 )
			printStatistics();

		return entry;
	}

	public ObjectInfo getByHobtId(int dbid, long hobtId)
	throws TimeoutException
	{
		_statLogicalRead++;
		
		Map<Long, ObjectInfo> map = _dbid_hobtId.get(dbid);
		ObjectInfo entry = (map == null) ? null : map.get(hobtId);

		if (entry == null)
		{
			// Get the information from the DBMS
			_statPhysicalRead++;
			entry = get(getConnection(), LookupType.HobtId, dbid, hobtId);
		}
		if ( (_statLogicalRead % _statReportModulus) == 0 )
			printStatistics();

		return entry;
	}

	/**
	 * Get only from cache (do not lookup in DBMS)
	 * 
	 * @param dbid
	 * @param objectid
	 * @return null if not found
	 */
	protected ObjectInfo internal_getByObjectId(int dbid, int objectid) 
	{
		Map<Integer, ObjectInfo> objIdMap = _dbid_objectId.get(dbid);
		if (objIdMap == null)
			return null;
		
		return objIdMap.get(objectid);
	}

	/**
	 * get the connection used to lookup things in the DBMS
	 * <p>
	 * If no connection is "cached", get a new connection using the connection provider 
	 */
	private DbxConnection getConnection()
	{
		if (_localConnection == null)
		{
			_logger.info("No local connection was found for the DBMS ObjectID Cache, creating a new using the Connection Provider. A Seperate connection is needed so we can do ObjectID lookups concurrently while we get counter data.");
			_localConnection = _connProvider.getNewConnection(Version.getAppName() + "-" + this.getClass().getSimpleName());

			// make DBMS specific settings
			onConnect(_localConnection);
		}
		
		if (_localConnection == null)
		{
			throw new RuntimeException("Not possible to grab a DBMS connection. _localConnection == null");
		}
		
		// Check if connection is "alive", and possibly "re-connect"
		if ( ! _localConnection.isConnectionOk() )
		{
			_logger.info("Cached Connection was broken, closing the connection and creating a new using the Connection Provider.");

			_localConnection.closeNoThrow();

			// _localConnection.reConnect(null);
			_localConnection = _connProvider.getNewConnection(Version.getAppName() + "-" + this.getClass().getSimpleName());
		}
		
		return _localConnection;
	}

	/**
	 * Set a new ObjectInfo for a specific dbid, objectid. <br>
	 * This would be used by getPlanBulk()
	 * 
	 * @param dbid
	 * @param objectid
	 * @param objectInfo
	 * @return The old content, if not previously cached it will return NULL
	 */
	public ObjectInfo setObjectInfo(int dbid, int objectid, ObjectInfo objectInfo)
	{
		_statLogicalWrite++;

		Map<Integer, ObjectInfo> objIdMap = _dbid_objectId.get(dbid);
		if (objIdMap == null)
		{
			objIdMap = new HashMap<Integer, ObjectInfo>();
			_dbid_objectId.put(dbid, objIdMap);
		}

		return objIdMap.put(objectid, objectInfo);
	}
	
	public String addIndex(int dbid, int objectid, int indexId, String indexName)
	{
		ObjectInfo oi = internal_getByObjectId(dbid, objectid);
		if (oi == null)
			throw new RuntimeException("addIndex(dbid=" + dbid + ", objectid=" + objectid + ", indexId=" + indexId + ", indexName='" + indexName + "'): ObjectInfo was not found.");

		return oi.addIndex(indexId, indexName);
	}
	
	public boolean addPartition(int dbid, int objectid, long partitionId)
	{
		ObjectInfo oi = internal_getByObjectId(dbid, objectid);
		if (oi == null)
			throw new RuntimeException("addPartition(dbid=" + dbid + ", objectid=" + objectid + ", partitionId=" + partitionId + "): ObjectInfo was not found.");

		Map<Long, ObjectInfo> partitionMap = _dbid_partitionId.get(dbid);
		if (partitionMap == null)
		{
			partitionMap = new HashMap<Long, ObjectInfo>();
			_dbid_partitionId.put(dbid, partitionMap);
		}
		
		partitionMap.put(partitionId, oi);
		return oi.addPartition(partitionId);
	}
	
	public boolean addHobt(int dbid, int objectid, long hobtId)
	{
		ObjectInfo oi = internal_getByObjectId(dbid, objectid);
		if (oi == null)
			throw new RuntimeException("addHobt(dbid=" + dbid + ", objectid=" + objectid + ", hobtId=" + hobtId + "): ObjectInfo was not found.");

		Map<Long, ObjectInfo> hobtMap = _dbid_hobtId.get(dbid);
		if (hobtMap == null)
		{
			hobtMap = new HashMap<Long, ObjectInfo>();
			_dbid_hobtId.put(dbid, hobtMap);
		}
		
		hobtMap.put(hobtId, oi);
		return oi.addHobt(hobtId);
	}
	
	/**
	 * Print statistics
	 */
	public void printStatistics()
	{
		// Change how often we should execute this report... At first, do it more often... 
		_statReportModulus = _statReportModulus * 2;
		if (_statReportModulus > _statReportModulusMax)
			_statReportModulus = _statReportModulusMax;

//		_logger.info("DbmsObjectIdCache Statistics: Size=" + size() + ", ResetCalls=" + _statResetCalls + ", DecreaseCalls=" + _statDecreaseCalls + ", LogicalRead=" + _statLogicalRead + ", LogicalWrite=" + _statLogicalWrite + ", BulkPhysicalReads=" + _statBulkPhysicalReads + ", PhysicalRead=" + _statPhysicalRead);
		_logger.info("STAT [DbmsObjectIdCache] Size=" + size() + ", ResetCalls=" + _statResetCalls + ", DecreaseCalls=" + _statDecreaseCalls + ", LogicalRead=" + _statLogicalRead + ", LogicalWrite=" + _statLogicalWrite + ", BulkPhysicalReads=" + _statBulkPhysicalReads + ", PhysicalRead=" + _statPhysicalRead);
	}

	/**
	 * Populate the cache from "somewhere" in an efficient way!
	 * @param dbnameSet This is entries you need to fetch. If NULL, you need to fetch/populate "everything"
	 */
	public void getBulk(Set<String> dbnameSet)
	{
		long startTime = System.currentTimeMillis();
		_logger.info("Begin: getBulk() in DbmsObjectIdCache. Populating local caching for looking up objectId->names for databases: " + (dbnameSet == null ? "-all-dbs-" : dbnameSet) );
		
		getBulk(getConnection(), dbnameSet);
		
		long execTimeMs = TimeUtils.msDiffNow(startTime);
		_logger.info("DONE: getBulk() in DbmsObjectIdCache. execTimeMs=" + execTimeMs + ", execTimeStr=" + TimeUtils.msToTimeStrShort(execTimeMs) + " ([HH:]%MM:%SS.%ms), TotalEntries=" + size() + ", EntriesPerDb=" + sizePerDb());
	}

	/**
	 * If "bulk load on start" is enabled, then try to populate all/as-many-as-possible on startup 
	 * @return true if we should call <code>getBulk()</code> on startup
	 */
	public abstract boolean isBulkLoadOnStartEnabled();
	
	/**
	 * Someone subclass to implement this method
	 * 
	 * @param connection     Database connection
	 * @param lookupType     Type of lookup that we want to do
	 * @param dbid           Database ID
	 * @param lookupId       ID of the above lookupType
	 * @return 
	 * @throws TimeoutException
	 */
	protected abstract ObjectInfo get(DbxConnection connection, LookupType lookupType, int dbid, Number lookupId) 
	throws TimeoutException;

	/**
	 * Populate the cache from "somewhere" in an efficient way!
	 * @param dbnameSet This is entries you need to fetch. If NULL, you need to fetch/populate "everything"
	 */
	protected abstract void getBulk(DbxConnection connection, Set<String> dbnameSet);

	/**
	 * Do DBMS specific settings after a new connection has been esablished
	 * @param conn The newly Connection handle
	 */
	protected abstract void onConnect(DbxConnection conn);
}
