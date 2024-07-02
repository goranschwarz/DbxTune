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

import java.sql.SQLException;
import java.sql.Types;
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
import org.h2.tools.SimpleResultSet;

import com.asetune.Version;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.TimeUtils;

public abstract class DbmsObjectIdCache
{
	private static Logger _logger = Logger.getLogger(DbmsObjectIdCache.class);

	public static final String  PROPKEY_lowOnMememory_removePct  = "DbmsObjectIdCache.lowOnMememory.removePct";
	public static final int     DEFAULT_lowOnMememory_removePct  = 20;

	public static final String  PROPKEY_DbmsLookupNotFoundThreshold = "DbmsObjectIdCache.dbms.lookup.notFound.threshold";
	public static final int     DEFAULT_DbmsLookupNotFoundThreshold = 5;

	public static final String  PROPKEY_DbmsLookupNotFound_callCreateObject = "DbmsObjectIdCache.dbms.lookup.notFound.call.createObject";
	public static final boolean DEFAULT_DbmsLookupNotFound_callCreateObject = false;
	
	public enum LookupType
	{
		/** Object ID */
		ObjectId, 

		/** Partitioned Table ID */
		PartitionId, 
		
		/** Heap O BinaryTree ID */
		HobtId
	};
	
	public enum ObjectType
	{
		/**
		 * Normal Table
		 */
		BASE_TABLE, 

		/**
		 * System Table
		 */
		SYSTEM_TABLE, 

		/**
		 * LOB = Large OBject
		 */
		LOB_TABLE, 

		/**
		 * A Normal table that is physically partitioned in different physical areas
		 */
		PARTITIONED_TABLE,

		/** 
		 * In Sybase a Proxy Table <br>
		 * In Postgres a Foreign Data Wrapper <br> 
		 * In SQL Server a (not 100% sure how this is done oe if it exists) <br> 
		 */
		REMOTE_TABLE, 

		/** In some databases like Postgres, an index should be considered as it's own object */
		INDEX,
		
		/** A basic view on table(s) */
		VIEW,

		/** A view that is materialized and "possibly" needs to be refreshed (a static point in time of data) */
		MATERIALIZED_VIEW,

		/** A Sequence object to generate new ID's */
		SEQUENCE,

		/** Unspecified object type */
		UNKNOWN
	};
	
	/** Data Record -- ObjectInfo */
	public static class ObjectInfo
	{
		private long   _dbid;
		private String _dbname;

		private long   _schemaId;
		private String _schemaName;

		private long   _objectId;
		private String _objectName;
		
		// possibly also Index names
		private Map<Long, String> _indexNames;
		
		// Partitions
		private Set<Long> _partitionIds;
		
		// Partitions
		private Set<Long> _hobtIds;

		private ObjectType _objectType;
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
			if (_indexNames != null)
			{
				for (Entry<Long, String> entry : _indexNames.entrySet())
				{
					size += 4; // index id
					size += entry.getValue().length() * 2; // index name
				}
			}
			
			size += _partitionIds == null ? 0 : _partitionIds.size() * 8;

			size += _hobtIds      == null ? 0 : _hobtIds.size()      * 8;
			
			return size;
		}
		
		public ObjectInfo(long dbid, String dbname, long schemaId, String schemaName, long objectId, String objectName, ObjectType objectType)
		{
			_dbid       = dbid;       
			_dbname     = dbname;     

			// maybe this helps memory consumption a bit for 'dbo' and 'sys' schemas, since we are making it a "singleton"
			//schemaName = normalizeSchemaName(schemaName);
			if (_staticSchemaNames != null)
			{
				String staticSchemaName = _staticSchemaNames.get(schemaName);
				if (staticSchemaName != null)
					schemaName = staticSchemaName;
			}
//			if      (SCHEMA_DBO        .equals(schemaName)) schemaName = SCHEMA_DBO;
//			else if (SCHEMA_SYS        .equals(schemaName)) schemaName = SCHEMA_SYS;
//			else if (SCHEMA_PUBLIC     .equals(schemaName)) schemaName = SCHEMA_PUBLIC;
//			else if (SCHEMA_PG_CATALOG .equals(schemaName)) schemaName = SCHEMA_PG_CATALOG;
//			else if (SCHEMA_INFORMATION.equals(schemaName)) schemaName = SCHEMA_INFORMATION;
			
			_schemaId   = schemaId;   
			_schemaName = schemaName; 

			_objectId   = objectId;   
			_objectName = objectName; 
			
			_objectType = objectType;
//			addIndex(indexId, indexName);
		}
		
		public String addIndex(long indexId, String indexName)
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
		
		public long       getDbid()          { return _dbid;       }
		public String     getDBName()        { return _dbname;     }
		public long       getSchemaId()      { return _schemaId;   }
		public String     getSchemaName()    { return _schemaName; }
		public long       getObjectId()      { return _objectId;   }
		public String     getObjectName()    { return _objectName; }
		public ObjectType getObjectType()    { return _objectType; }
		public String     getObjectTypeStr() { return _objectType + ""; }

		/** Get index Map, which is a Map of IndexId, IndexName */
		public Map<Long, String> getIndexMap() 
		{
			if (_indexNames == null)
				return Collections.emptyMap();
		
			return _indexNames; 
		}

		/** Get index name for a index ID */
		public String getIndexName(long indexId) 
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

	// This can also be used as a schema name translator. 
	// But the main purpose is to use Singleton object, so we don't "pollute" the memory (with multiple Strings of "dbo", "sys", "pg_catalog"...)
	private static Map<String, String> _staticSchemaNames;
	
	protected Map<Long, String>                _dbNamesMap       = new HashMap<>();
	private   Map<Long, Map<Long, ObjectInfo>> _dbid_objectId    = new HashMap<>();
	private   Map<Long, Map<Long, ObjectInfo>> _dbid_partitionId = new HashMap<>();
	private   Map<Long, Map<Long, ObjectInfo>> _dbid_hobtId      = new HashMap<>();

	private   Map<Long, Map<Long, Integer>> _dbid_objectId_dbmsLookupNotFoundCounter = new HashMap<>();
	
	
	/** Keep a local DBMS Connection for the lookups */
	private DbxConnection _localConnection;

	protected long _statLogicalRead       = 0;    // How many times did we read from the cache 
	protected long _statPhysicalRead      = 0;    // How many times did we read from the back-end storage
	protected long _statBulkPhysicalReads = 0;    // How many times did we call getPlanBulk()
	protected long _statLogicalWrite      = 0;    // How many times did we call setPlan()

	protected long _statDbmsLookupNotFoundCount         = 0; // How many times did we do a DBMS lookup, and found NO object.
	protected long _statDbmsLookupNotFoundCountExceeded = 0; // How many times did we SKIP DBMS call due to "not-found-count-threshold" was crossed for the object
	
	private   long _statReportModulus     = 5000; // When it gets crossed it will double itself, but max is always _statReportModulusMax
	private   long _statReportModulusMax  = 100000;
//	private   long _statReportModulus     = 500;   // <<<--- for TEST purposes
//	private   long _statReportModulusMax  = 10000; // <<<--- for TEST purposes
	// Or possibly change to report when X minutes has passed... since last report print

	protected long _statResetCalls        = 0;    // How many times did we call outOfMemoryHandler()
	protected long _statDecreaseCalls     = 0;    // How many times did we call lowOfMemoryHandler()

//	protected int     _dbmsLookupNotFoundThreshold = -1; 
//	protected boolean _onDbmsLookupNotFound_callCreateObject = false;

	//----------------------------------------------------------------
	// BEGIN: instance
	private static DbmsObjectIdCache _instance = null;
	public static DbmsObjectIdCache getInstance()
	{
		if (_instance == null)
		{
			throw new RuntimeException("DbmsObjectIdCache doesn't have an instance yet, please set with setInstance(instance).");
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
		
		if (_staticSchemaNames == null)
			_staticSchemaNames = createStaticSchemaNames();
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

		_dbid_objectId_dbmsLookupNotFoundCounter = null;
		
		
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

		for (Map<Long, ObjectInfo> e : _dbid_objectId.values())
			size += e.size();

		return size;
	}

	/** 
	 * How many entries do we have per database 
	 */ 
	public Map<String, Integer> sizePerDb()
	{
		Map<String, Integer> dbSizeMap = new LinkedHashMap<>();

		for (Entry<Long, Map<Long, ObjectInfo>> e : _dbid_objectId.entrySet())
		{
			Long dbid = e.getKey();
			Map<Long, ObjectInfo> objInfoPerDb = e.getValue();

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

		_dbid_objectId_dbmsLookupNotFoundCounter = new HashMap<>();
		
		_statResetCalls++;
	}

	/**
	 * Remove cached objects for a specific database id
	 * 
	 * @param dbid
	 */
	public void clearCacheForDbid(long dbid)
	{
		if (_dbNamesMap       != null) _dbNamesMap       .remove(dbid);
		if (_dbid_objectId    != null) _dbid_objectId    .remove(dbid);
		if (_dbid_partitionId != null) _dbid_partitionId .remove(dbid);
		if (_dbid_hobtId      != null) _dbid_hobtId      .remove(dbid);

		if (_dbid_objectId_dbmsLookupNotFoundCounter != null) _dbid_objectId_dbmsLookupNotFoundCounter.remove(dbid);
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
			for (Entry<Long, Map<Long, ObjectInfo>> dbidEntry : _dbid_objectId.entrySet())
			{
				for (Entry<Long, ObjectInfo> objIdEntry : dbidEntry.getValue().entrySet())
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
		_dbid_objectId_dbmsLookupNotFoundCounter = new HashMap<>();
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
				for (Map<Long, ObjectInfo> dbidEntry : _dbid_objectId.values())
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
	 * Check if the Object has entries
	 * @return
	 */
	public boolean hasDbIds()
	{
		if (_dbNamesMap == null)
			return false;
		
		return ! _dbNamesMap.isEmpty();
	}

	/**
	 * Check if the Object has entries
	 * @return
	 */
	public boolean hasObjectIds()
	{
		if (_dbid_objectId == null)
			return false;
		
		return ! _dbid_objectId.isEmpty();
	}

	/**
	 * Check if the ID is Cached (does not do any physical IO)
	 * @param dbid
	 * @param objectid
	 * @return true or false
	 */
	public boolean isCached(long dbid, long objectid)
	{
		_statLogicalRead++;
		if ( (_statLogicalRead % _statReportModulus) == 0 )
			printStatistics();

		Map<Long, ObjectInfo> objIdMap = _dbid_objectId.get(dbid);
		if (objIdMap == null)
			return false;

		return objIdMap.containsKey(objectid);
	}

	/**
	 * Get the DBName from cache 
	 * @param dbid
	 * @return
	 */
	public String getDBName(long dbid)
	{
		return _dbNamesMap.get(dbid);
	}

	/**
	 * Get the dbid from cache 
	 * @param dbname
	 * @return
	 */
	public Long getDbid(String dbname)
	{
		for (Entry<Long, String> entry : _dbNamesMap.entrySet())
		{
			if (dbname.equals(entry.getValue()))
				return entry.getKey();
		}
		return null;
	}

	/**
	 * Set the DBName in cache 
	 * @param dbid
	 * @param dbname
	 * @return
	 */
	public String setDBName(long dbid, String dbname)
	{
		return _dbNamesMap.put(dbid, dbname);
	}

	public String debugPrintAllObject()
	{
		SimpleResultSet rs = new SimpleResultSet();
		rs.addColumn("dbid",                   Types.BIGINT,     0, 0);
		rs.addColumn("dbname",                 Types.VARCHAR,   80, 0);

		rs.addColumn("schemaId",               Types.BIGINT,     0, 0);
		rs.addColumn("schemaName",             Types.VARCHAR,   80, 0);

		rs.addColumn("objectId",               Types.BIGINT,     0, 0);
		rs.addColumn("objectName",             Types.VARCHAR,   80, 0);
		
		rs.addColumn("objectType",             Types.VARCHAR,   80, 0);

		for (Entry<Long, Map<Long, ObjectInfo>> e1 : _dbid_objectId.entrySet())
		{
//			Long dbid                    = e1.getKey();
			Map<Long, ObjectInfo> objMap = e1.getValue();
			
			for (Entry<Long, ObjectInfo> e2 : objMap.entrySet())
			{
//				Long objid    = e2.getKey();
				ObjectInfo oi = e2.getValue();

				rs.addRow(
						 oi._dbid
						,oi._dbname
						,oi._schemaId
						,oi._schemaName
						,oi._objectId
						,oi._objectName
						,oi._objectType
				);
			}
		}
		
		try
		{
			ResultSetTableModel rstm = new ResultSetTableModel(rs, "debugPrint");
			return rstm.toAsciiTableString();
		}
		catch (SQLException ex)
		{
			return "" + ex;
		}
	}
	
	/**
	 * 
	 * @param dbid
	 * @param objectid
	 * @param indexid
	 * @return IndexName if it was found. Null if the ID wasn't found
	 * @throws TimeoutException
	 */
	public String getIndexName(long dbid, long objectid, long indexid) 
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
	public String getSchemaName(long dbid, long objectid) 
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
	public String getObjectName(long dbid, long objectid) 
	throws TimeoutException
	{
		ObjectInfo objectInfo = getByObjectId(dbid, objectid);
		
		if (objectInfo == null)
			return null;

		return objectInfo.getObjectName();
	}
	
	/**
	 * 
	 * @param dbid
	 * @param objectid
	 * @return ObjectName if it was found. Null if the ID wasn't found
	 * @throws TimeoutException
	 */
	public String getObjectType(long dbid, long objectid) 
	throws TimeoutException
	{
		ObjectInfo objectInfo = getByObjectId(dbid, objectid);
		
		if (objectInfo == null)
			return null;

		return objectInfo.getObjectTypeStr();
	}
	
	/**
	 * Make a DBMS call to get the object which wasn't found in the cache
	 * <p>
	 * Note: The DBMS lookup for each "dbid" and "lookupId" will only be attempted 5 times (by default, or override method {@link #getConfig_dbmsLookupNotFoundThreshold()}, 
	 *       or change property <code>DbmsObjectIdCache.dbms.lookup.notFound.threshold=##</code><br>
	 * <p>
	 * When the "not-found-count" for this object has been reached, we wont try again and a NULL value value will be returned, or possible call {@link #onDbmsLookupNotFound_createObject()}.
	 * 
	 * @param connection
	 * @param lookupType
	 * @param dbid
	 * @param lookupId
	 * @return
	 * @throws TimeoutException
	 */
	protected ObjectInfo getFromDbWrapper(DbxConnection connection, LookupType lookupType, long dbid, Number lookupId)
	throws TimeoutException
	{
		// Check how many times we have *failed* to get the: dbid, lookupId
		// If the "not-found-count" is above the "threshold"... Simply return a null value without doing database lookup
		Map<Long, Integer> map = _dbid_objectId_dbmsLookupNotFoundCounter.get(dbid);
		Integer notFoundCount = (map == null) ? null : map.get(lookupId);

		if (notFoundCount != null && notFoundCount > getConfig_dbmsLookupNotFoundThreshold())
		{
			_statDbmsLookupNotFoundCountExceeded++;

			if (getConfig_onDbmsLookupNotFound_callCreateObject())
			{
				// Note: This "fake" object will be cached
				ObjectInfo tmpEntry = onDbmsLookupNotFound_createObject(lookupType, dbid, lookupId);
				setObjectInfo(dbid, dbid, tmpEntry);

				return tmpEntry;
			}
			return null;
		}
		
		// Do the lookup in the DBMS
		_statPhysicalRead++;
		ObjectInfo entry = get(getConnection(), lookupType, dbid, lookupId);

		// If we did NOT find a object, then increment the "fail-counter"
		if (entry == null)
		{
			_statDbmsLookupNotFoundCount++;

			// Get map, if it do not exists, create one
			Map<Long, Integer> objErrorMap = _dbid_objectId_dbmsLookupNotFoundCounter.get(dbid);
			if (objErrorMap == null)
			{
				objErrorMap = new HashMap<Long, Integer>();
				_dbid_objectId_dbmsLookupNotFoundCounter.put(dbid, objErrorMap);
			}

			// Get counter and increment
			Integer tmpNotFoundCount = objErrorMap.get(lookupId);
			if (tmpNotFoundCount == null)
				tmpNotFoundCount = 0;
			tmpNotFoundCount++;

			// Put the "fail-count" back in the map
			objErrorMap.put(lookupId.longValue(), tmpNotFoundCount);
		}
		
		return entry;
	}

	/**
	 * Get the content from cache, if it's not in the cache it will call the back-end to get the information 
	 * @param dbid
	 * @param objectid
	 * @return
	 * @throws TimeoutException 
	 */
	public ObjectInfo getByObjectId(long dbid, long objectid) 
	throws TimeoutException
	{
		_statLogicalRead++;
		
		Map<Long, ObjectInfo> map = _dbid_objectId.get(dbid);
		ObjectInfo entry = (map == null) ? null : map.get(objectid);

		if (entry == null)
		{
			// Get the information from the DBMS
			entry = getFromDbWrapper(getConnection(), LookupType.ObjectId, dbid, objectid);
		}
		if ( (_statLogicalRead % _statReportModulus) == 0 )
			printStatistics();

		return entry;
	}

	public ObjectInfo getByPartitionId(long dbid, long partitionId)
	throws TimeoutException
	{
		_statLogicalRead++;
		
		Map<Long, ObjectInfo> map = _dbid_partitionId.get(dbid);
		ObjectInfo entry = (map == null) ? null : map.get(partitionId);

		if (entry == null)
		{
			// Get the information from the DBMS
			entry = getFromDbWrapper(getConnection(), LookupType.PartitionId, dbid, partitionId);
		}
		if ( (_statLogicalRead % _statReportModulus) == 0 )
			printStatistics();

		return entry;
	}

	public ObjectInfo getByHobtId(long dbid, long hobtId)
	throws TimeoutException
	{
		_statLogicalRead++;
		
		Map<Long, ObjectInfo> map = _dbid_hobtId.get(dbid);
		ObjectInfo entry = (map == null) ? null : map.get(hobtId);

		if (entry == null)
		{
			// Get the information from the DBMS
			entry = getFromDbWrapper(getConnection(), LookupType.HobtId, dbid, hobtId);
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
	protected ObjectInfo internal_getByObjectId(long dbid, long objectid) 
	{
		Map<Long, ObjectInfo> objIdMap = _dbid_objectId.get(dbid);
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
	public ObjectInfo setObjectInfo(long dbid, long objectid, ObjectInfo objectInfo)
	{
		_statLogicalWrite++;

		Map<Long, ObjectInfo> objIdMap = _dbid_objectId.get(dbid);
		if (objIdMap == null)
		{
			objIdMap = new HashMap<Long, ObjectInfo>();
			_dbid_objectId.put(dbid, objIdMap);
		}

		return objIdMap.put(objectid, objectInfo);
	}
	
	public String addIndex(long dbid, long objectid, long indexId, String indexName)
	{
		ObjectInfo oi = internal_getByObjectId(dbid, objectid);
		if (oi == null)
			throw new RuntimeException("addIndex(dbid=" + dbid + ", objectid=" + objectid + ", indexId=" + indexId + ", indexName='" + indexName + "'): ObjectInfo was not found.");

		return oi.addIndex(indexId, indexName);
	}
	
	public boolean addPartition(long dbid, long objectid, long partitionId)
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
	
	public boolean addHobt(long dbid, long objectid, long hobtId)
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

//	/**
//	 * Checks if we have done bulk load for a specific database<br>
//	 * It might be useful if we can't do in initially, and has to defer it to a later stage<br>
//	 * For example in Postgres we might want to setup connections in a Connection Pool and use that...
//	 * 
//	 * @param dbname
//	 * @return true if Bulk load has been done.
//	 */
//	public boolean hasBuldLoadBeenDone(String dbname)
//	{
//	}

	/**
	 * Get configuration for...
	 */
	protected int getConfig_dbmsLookupNotFoundThreshold()
	{
		return Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_DbmsLookupNotFoundThreshold, DEFAULT_DbmsLookupNotFoundThreshold);
	}

	/**
	 * Get configuration for...
	 */
	protected boolean getConfig_onDbmsLookupNotFound_callCreateObject()
	{
		return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_DbmsLookupNotFound_callCreateObject, DEFAULT_DbmsLookupNotFound_callCreateObject);
	}

	/**
	 * Create a Dummy Object that can be cached instead of the "real" DBMS object, which could not be found. 
	 * 
	 * @param lookupType
	 * @param dbid
	 * @param lookupId
	 * @return
	 */
	protected ObjectInfo onDbmsLookupNotFound_createObject(LookupType lookupType, long dbid, Number lookupId)
	{
		String cachedDbname = getDBName(dbid);
		
		String dbname         = cachedDbname != null ? cachedDbname : "-unknown-dbname-";
		long   schemaId       = -1;
		String schemaName     = "-unknown-schema-"; 
		long   objectId       = lookupId.longValue();
		String objectName     = "-unknown-object-";
		ObjectType objectType = ObjectType.UNKNOWN;

//		public ObjectInfo(long dbid, String dbname, long schemaId, String schemaName, long objectId, String objectName, ObjectType objectType)

		return new ObjectInfo(dbid, dbname, schemaId, schemaName, objectId, objectName, objectType);
	}
	

	/**
	 * @return return a Map with some statics/well-known SCHEMA names for this DBMS Vendor
	 */
	protected abstract Map<String, String> createStaticSchemaNames();

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
	protected abstract ObjectInfo get(DbxConnection connection, LookupType lookupType, long dbid, Number lookupId) 
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
