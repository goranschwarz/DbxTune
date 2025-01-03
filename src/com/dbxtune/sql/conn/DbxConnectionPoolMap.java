/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.sql.conn;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class DbxConnectionPoolMap
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

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
