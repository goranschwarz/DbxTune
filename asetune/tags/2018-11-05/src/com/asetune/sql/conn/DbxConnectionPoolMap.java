package com.asetune.sql.conn;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class DbxConnectionPoolMap
{
	private static Logger _logger = Logger.getLogger(DbxConnectionPoolMap.class);

	private static DbxConnectionPoolMap _instance = null;

	private Map<String, DbxConnectionPool> _map = new HashMap<>();

	/**
	 * Get the singleton
	 * @return
	 */
	public static DbxConnectionPoolMap getInstance()
	{
		return _instance;
	}
	
	/**
	 * Set the singleton instance
	 * @param pool
	 */
	public static void setInstance(DbxConnectionPoolMap pool)
	{
		_instance = pool;
	}
	
	/**
	 * Checks if we have an instance or if we need to create one
	 * @return
	 */
	public static boolean hasInstance()
	{
		return _instance != null;
	}


	
	public boolean hasMapping(String key)
	{
		return _map.containsKey(key);
	}

	public DbxConnectionPool setPool(String key, DbxConnectionPool pool)
	{
		return _map.put(key, pool);
	}

	public DbxConnectionPool getPool(String key)
	{
		return _map.get(key);
	}



	
	/**
	 * Close all connection in the pool
	 * @return number of Connections closed
	 */
	public synchronized int close()
	{
		int count = 0;
		
		// Close all the connection for each Pool in the Map
		for (String key : _map.keySet())
		{
			DbxConnectionPool cp = _map.get(key);
			if (cp == null)
				continue;
			
			int closeCount = cp.close();
			
			if (closeCount > 0)
				_logger.info("Closed "+closeCount+" connection which was mapped to '"+key+"'.");
			
			count += closeCount;
		}
		
		// Clear the map
		_map = new HashMap<>();

		return count;
	}
}
