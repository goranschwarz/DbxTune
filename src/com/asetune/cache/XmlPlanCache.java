package com.asetune.cache;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.log4j.Logger;

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
			throw new RuntimeException("XmlPlanCache dosn't have an instace yet, please set with setInstance(instance).");
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
			_statPhysicalRead++;
			entry = getPlan(_connProvider.getConnection(), planName, planId);
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
		getPlanBulk(_connProvider.getConnection(), list);
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