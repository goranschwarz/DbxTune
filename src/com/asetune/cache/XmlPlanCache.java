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

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.asetune.Version;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.ConnectionProvider;

public abstract class XmlPlanCache
{
	private static Logger _logger = Logger.getLogger(XmlPlanCache.class);

	public static final String  PROPKEY_lowOnMememory_removePct  = "XmlPlanCache.lowOnMememory.removePct";
	public static final int     DEFAULT_lowOnMememory_removePct  = 20;

	private ConnectionProvider _connProvider = null;
//	private HashMap<String, String> _cache = new HashMap<String, String>();
	private LinkedHashMap<String, String> _cache = new LinkedHashMap<String, String>();

	/** Keep a local DBMS Connection for the lookups */
	private DbxConnection _localConnection;

	protected long _statLogicalRead       = 0;    // How many times did we read from the cache 
	protected long _statPhysicalRead      = 0;    // How many times did we read from the back-end storage
	protected long _statBulkPhysicalReads = 0;    // How many times did we call getPlanBulk()
	protected long _statLogicalWrite      = 0;    // How many times did we call setPlan()

	private   long _statReportModulus     = 5000; // When it gets crossed it will double itself, but max is always _statReportModulusMax
	private   long _statReportModulusMax  = 100000;

	protected long _statResetCalls        = 0;    // How many times did we call outOfMemoryHandler()
	protected long _statDecreaseCalls     = 0;    // How many times did we call lowOfMemoryHandler()

	//----------------------------------------------------------------
	// BEGIN: instance
	private static XmlPlanCache _instance = null;
	public static XmlPlanCache getInstance()
	{
		if (_instance == null)
		{
			throw new RuntimeException("XmlPlanCache dosn't have an instance yet, please set with setInstance(instance).");
		}
		return _instance;
	}
	public static void setInstance(XmlPlanCache instance)
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
	public XmlPlanCache(ConnectionProvider connProvider)
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
		_cache    = null;

		if (_localConnection != null)
			_localConnection.closeNoThrow();
		
		_instance = null;
	}
	

	/**
	 * Get some information about how much memory is used by this cache.
	 * @return A string with information.
	 */
	public String getMemoryConsumption()
	{
		int entryCount  = _cache == null ? 0 : _cache.size();
		int usedMemory  = 0;
		int avgPerEntry = 0;
		
		for (Entry<String, String> entry : _cache.entrySet())
		{
//			usedMemory += entry.getKey()  .length();
//			usedMemory += entry.getValue().length();

			// algo: 36          + (2 * str.length)
			//       VM overhead + (every char is 16 bits, since internal of strings is UTF-16)
			usedMemory += 36 + (2 * entry.getKey()  .length());
			usedMemory += 36 + (2 * entry.getValue().length());
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
		_logger.info("Clearing all content from the XmlPlanCache. Number of entries before clean was "+_cache.size());
		_statResetCalls++;
		// Simply create a new _cache to clear it.
//		_cache = new HashMap<String, String>();
		_cache = new LinkedHashMap<String, String>();
	}

	/**
	 * In case someone finds out that we are low on memory... 
	 */
	public void lowOnMemoryHandler()
	{
		// Get remove percent from configuration
		int removePct = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_lowOnMememory_removePct, DEFAULT_lowOnMememory_removePct);
		
		int countBeforeClean = _cache.size();
		int removeCount = (int) (countBeforeClean * (removePct / 100.0));
//		for (String key : _cache.keySet())
//		{
//			_cache.remove(key);
//			removeCount--;
//			if (removeCount <= 0)
//				break;
//		}
// The above fails with some concurrent access exception, the below works better
// OR simply add the entries to a list and remove the content of the list from the cache later on...

		// SInce this structure isn't really protected, we can still get ConcurrentModificationException
		// if other threads accesses this structure at the same time...
		// Don't care about this, just catch it and continue...
		try
		{
			if (removeCount < 20)
			{
				// If small enough chunk, lets just create a new cache...
				_cache = new LinkedHashMap<String, String>();
			}
			else
			{
	    		Iterator<String> it = _cache.values().iterator();
	    		while(it.hasNext())
	    		{
	    			it.next(); // Must be called before we can call remove()
	    			it.remove();
	    			removeCount--;
	    			if (removeCount <= 0)
	    				break;
	    		}
			}
		}
		catch(ConcurrentModificationException ex)
		{
			_logger.info("XmlPlanCache, lowOnMemoryHandler(): when removing entries from the cache, we caught ConcurrentModificationException... lets just continue... Exception: "+ex);
		}

		int countAfterClean  = _cache.size();
		removeCount = countBeforeClean - countAfterClean;

		_logger.info("XmlPlanCache, lowOnMemoryHandler() was called. Removed "+removeCount+" entries from the XmlPlanCache (config: '"+PROPKEY_lowOnMememory_removePct+"'="+removePct+"). Number of entries before clean was "+countBeforeClean+", after clean was "+countAfterClean+".");
		_statDecreaseCalls++;
	}

	/**
	 * Internal method to compose the key used in the HashMap, this to not spread out the implementation in case of changes
	 * @param planName
	 * @param planId
	 * @return
	 */
	private String composeKey(String planName, int planId)
	{
		String key = planName;
		if (planId > 0)
			key = planName + "|" + planId;

		return key;
	}

	/**
	 * Check if the Plan is Cached (does not do any physical IO)
	 * @param planName
	 * @return true or false
	 */
	public boolean isPlanCached(String planName)
	{
		return isPlanCached(planName, 0);
	}

	/**
	 * Check if the Plan is Cached (does not do any physical IO)
	 * @param planName
	 * @param planId
	 * @return true or false
	 */
	public boolean isPlanCached(String planName, int planId)
	{
		_statLogicalRead++;
		if ( (_statLogicalRead % _statReportModulus) == 0 )
			printStatistics();

		return _cache.containsKey(composeKey(planName, planId));
	}

	/**
	 * Get the content from cache, if it's not in the cache it will call the back-end to get the information 
	 * @param planName
	 * @return
	 */
	public String getPlan(String planName)
	{
		return getPlan(planName, 0);
	}

	/**
	 * Get the content from cache, if it's not in the cache it will call the back-end to get the information 
	 * @param planName
	 * @param planId
	 * @return
	 */
	public String getPlan(String planName, int planId)
	{
		String key = composeKey(planName, planId);

		_statLogicalRead++;
		String entry = _cache.get(key);
		if (entry == null)
		{
			// If the Connection in the ConnectionProvider is *busy* executing a query this might Fail...
			// And that in a ugly way (getting "stuck") -->> at com.sybase.jdbc4.utils.SyncQueue.take(SyncQueue.java:93)
			// Is there any way we could have our own Connection here... OR wait for the Connection to finish/complete
			
			_statPhysicalRead++;
//			entry = getPlan(_connProvider.getConnection(), planName, planId);
			entry = getPlan(getConnection(), planName, planId);
			if (entry != null)
			{
				_cache.put(key, entry);
			}
		}
		if ( (_statLogicalRead % _statReportModulus) == 0 )
			printStatistics();

		return entry;
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
			_logger.info("No connection was found, creating a new using the Connection Provider.");
			_localConnection = _connProvider.getNewConnection(Version.getAppName() + "-" + this.getClass().getSimpleName());
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
	 * Set a new Plan for a specific key. <br>
	 * This would be used by getPlanBulk()
	 * 
	 * @param planName
	 * @param planContent
	 * @return The old content, if not previously cached it will return NULL
	 */
	public String setPlan(String planName, String planContent)
	{
		return setPlan(planName, 0, planContent);
	}

	/**
	 * Set a new Plan for a specific key. <br>
	 * This would be used by getPlanBulk()
	 * 
	 * @param planName
	 * @param planId
	 * @param planContent
	 * @return The old content, if not previously cached it will return NULL
	 */
	public String setPlan(String planName, int planId, String planContent)
	{
		_statLogicalWrite++;
		return _cache.put(composeKey(planName, planId), planContent);
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

		_logger.info("XmlPlanCache Statistics: Size="+_cache.size()+", ResetCalls="+_statResetCalls+", DecreaseCalls="+_statDecreaseCalls+", LogicalRead="+_statLogicalRead+", LogicalWrite="+_statLogicalWrite+", BulkPhysicalReads="+_statBulkPhysicalReads+", PhysicalRead="+_statPhysicalRead);
	}

	/**
	 * Populate the cache from "somewhere" in an efficient way!
	 * @param list This is entries you need to fetch. If NULL, you need to fetch/populate "everything"
	 */
	public void getPlanBulk(List<String> list)
	{
//		getPlanBulk(_connProvider.getConnection(), list);
		getPlanBulk(getConnection(), list);
	}

	/**
	 * Someone subclass to implement this method
	 * @param connection
	 * @param key
	 * @return
	 */
	protected abstract String getPlan(DbxConnection connection, String planName, int planId);

	/**
	 * Populate the cache from "somewhere" in an efficient way!
	 * @param list This is entries you need to fetch. If NULL, you need to fetch/populate "everything"
	 */
	protected abstract void getPlanBulk(DbxConnection connection, List<String> list);
}
