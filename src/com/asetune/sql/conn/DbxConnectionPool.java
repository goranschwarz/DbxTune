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
package com.asetune.sql.conn;

import java.awt.Window;
import java.sql.SQLException;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;

public class DbxConnectionPool
{
	private static Logger _logger = Logger.getLogger(DbxConnectionPool.class);

	private static DbxConnectionPool _instance = null;

	private LinkedList<DbxConnection> _free  = new LinkedList<>();
	private LinkedList<DbxConnection> _bussy = new LinkedList<>();

	private ConnectionProp _connProp = null;
	private int    _maxSize = 30;
	private String _name    = "noname"; // Name of the connection pool 

	public ConnectionProp getConnectionProp()   { return _connProp; }
//	public String getDriver()   { return _connProp.getDriverClass(); }
//	public String getUrl()      { return _connProp.getUrl(); }  
//	public String getUsername() { return _connProp.getUsername(); }
//	public String getPassword() { return _connProp.getPassword(); }
//	public String getAppName()  { return _connProp.getAppName(); }
	public int    getMaxSize()  { return _maxSize;  }	
	

	/**
	 * Get the singleton
	 * @return
	 */
	public static DbxConnectionPool getInstance()
	{
		return _instance;
	}
	/**
	 * Set the singleton instance
	 * @param pool
	 */
	public static void setInstance(DbxConnectionPool pool)
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

	/**
	 * Create a new object
	 * @param driver
	 * @param url
	 * @param username
	 * @param password
	 * @param appName
	 * @param maxSize
	 */
	public DbxConnectionPool(String name, String driver, String url, String username, String password, String appName, int maxSize)
	{
		ConnectionProp cp = new ConnectionProp();
		cp.setDriverClass(driver);
		cp.setUrl(url);
		cp.setUsername(username);
		cp.setPassword(password);
		cp.setAppName(appName);

		_name = name;
		if (StringUtil.isNullOrBlank(_name))
			_name = "noname";

		_connProp = cp;
		_maxSize  = maxSize;
	}

	/**
	 * Create a new object
	 * @param connProp
	 * @param maxSize
	 */
	public DbxConnectionPool(String name, ConnectionProp connProp, int maxSize)
	{
		_name = name;
		if (StringUtil.isNullOrBlank(_name))
			_name = "noname";

		_connProp = connProp;
		_maxSize  = maxSize;
	}
	
	/**
	 * Get a connection from the connection pool
	 * @return a DbxConnection
	 * @throws SQLException if all connections are used or if we caught problems when creating a new connection.
	 */
	public synchronized DbxConnection getConnection()
	throws SQLException
	{
		return getConnection(null);
	}
	
	/**
	 * Get a connection from the connection pool
	 * @return a DbxConnection
	 * @throws SQLException if all connections are used or if we caught problems when creating a new connection.
	 */
	public synchronized DbxConnection getConnection(Window guiOwner)
	throws SQLException
	{
		// Get connection from the Free list
		int freeSize = _free.size();
		if (freeSize > 0)
		{
			DbxConnection conn = _free.getFirst();
			_free.removeFirst();

			// Maybe: check if the connection is valid before using it
			if ( ! conn.isValid(1) )
			{
				_logger.warn("When getting a connection '"+conn+"' from the connection pool '"+_name+"', the connection was no longer valid.");
				conn.closeNoThrow();
				throw new SQLException("When getting a connection '"+conn+"' from the connection pool '"+_name+"', the connection was no longer valid.");
			}
			
			_bussy.addLast(conn);
			return conn;
		}
		
		String connPropUrl = "";
		if (_connProp != null)
			connPropUrl = _connProp.getUrl();

		// If all connections are bussy... throw exception
		int bussySize = _bussy.size();
		if (bussySize > _maxSize)
		{
			throw new SQLException("All connection in the pool '"+_name+"' are used. MaxSize="+_maxSize+", URL="+connPropUrl);
		}
		
		// Create a new connection
		try
		{
			DbxConnection conn = DbxConnection.connect(guiOwner, _connProp);
			_bussy.addLast(conn);

			_logger.info("Created a new connection for the pool '"+_name+"'. bussy="+_bussy.size()+", free="+_free.size()+", MaxSize="+_maxSize+", URL="+connPropUrl);
			
			return conn;
		}
		catch (SQLException ex) 
		{
			throw ex;
		}
		catch (Exception ex) 
		{
			throw new SQLException("Problems creating a new connection for the pool '"+_name+"', URL="+connPropUrl+". Caught: "+ex, ex);
		}
	}

	/**
	 * Give back a connection to the connection pool.
	 * @param conn
	 */
	public synchronized void releaseConnection(DbxConnection conn)
	{
		if (conn == null)
			return;

		// Remove the connection from the bussy
		boolean removed = _bussy.remove(conn);
		if ( ! removed )
			_logger.warn("When releasing a connection from connectionPool='"+_name+"'. The connection was not found in the 'bussy' list. releaseConnection(conn='"+conn+"'). After _bussy.remove(conn), the _bussy.size()="+_bussy.size());

		// check that it really was removed, and remove it... MAX attempts = 10, then continue...
		for (int c=10; c>0; c--)
		{
			// If NOT in list... break the loop
			if (_bussy.indexOf(conn) != -1)
				break;

			_bussy.remove(conn);
		}
		
		if (_logger.isDebugEnabled())
			_logger.warn("releaseConnection(conn='"+conn+"') connectionPool='"+_name+"', after _bussy.remove(conn), the _bussy.size()="+_bussy.size());

		// Check if the connection is OK (before we put it in the free pool)
		try
		{
			if ( ! conn.isValid(1) )
			{
				_logger.warn("When returning the connection '"+conn+"' to the connection pool '"+_name+"', the connection was no longer valid, CLOSING THIS connection.");
				conn.closeNoThrow();
				return;				
			}

			// Check that the conn is not still in a transaction.
			if (conn.isInTransaction())
			{
				String msg = "When returning the connection '"+conn+"' to the connection pool '"+_name+"', the connection was in a transaction. CLOSING THIS Connection.";
				_logger.warn(msg, new SQLException(msg));
				conn.closeNoThrow();
				return;				
			}

			// We could check all sorts of stuff here:
			//  * Are we in the same database / Catalog
			//  * Are we still the same user/schema (if the connection did 'setuser xxx' or similar)
			//  * etc etc...
		}
		catch (SQLException e)
		{
			conn.closeNoThrow();

			_logger.warn("When returning the connection '"+conn+"' to the connection pool '"+_name+"', caught exception when checking it. CLOSED THIS Connection.", e);
			return;
		}
		
		// Close the connection after X number of seconds.
		// This is if any connection is holding "whatever" resources on the server-side that isn't released until any connection is closed.
		if (true)
		{
			String propName     = _name+".close.threshold.inSeconds";
			long connectTime    = conn.getConnectTime();
			long closeThreshold = Configuration.getCombinedConfiguration().getLongProperty(propName, 3600*24); // 24 Hours
//			long closeThreshold = Configuration.getCombinedConfiguration().getLongProperty(propName, 3600*24*7); // 7 days
			if (connectTime > 0 && closeThreshold > 0)
			{
				long secondsSinceConnect = TimeUtils.msDiffNow(connectTime) / 1000;
//System.out.println("DEBUG: DbxConnectionPool[name='"+_name+"'].releaseConnection(): secondsSinceConnect="+secondsSinceConnect+", closeThreshold="+closeThreshold+", propName='"+propName+"', conn="+conn+", url="+conn.getConnProp().getUrl()+", propsMap="+conn.getPropertyMap());
				if (secondsSinceConnect > closeThreshold)
				{
					String secondsSinceConnectStr = "secondsSinceConnect="+secondsSinceConnect + " [" + TimeUtils.msToTimeStr("%HH:%MM", secondsSinceConnect*1000) + " HH:MM]";
					String closeThresholdStr      = "threshold="          +closeThreshold      + " [" + TimeUtils.msToTimeStr("%HH:%MM", closeThreshold     *1000) + " HH:MM]";
					ConnectionProp connProp = conn.getConnProp();
					String connInfo = "(url='"+(connProp==null?"unknown":connProp.getUrl())+"', connPropertyMap='"+conn.getPropertyMap()+"')";
					_logger.info("When returning the connection '"+conn+"' "+connInfo+" to the connection pool '"+_name+"', the 'keepalive' expired, "
							+ "CLOSING the connection. A new connection will be made on next attempt. " 
							+ "("+secondsSinceConnectStr+", "+closeThresholdStr+"). "
							+ "This can be changed with the property '"+propName+"=secondsToLive'.");
					conn.closeNoThrow();
					return;
				}
			}
		}

		// put the connection in the free pool
		_free.addFirst(conn);
	}
	
	/**
	 * Close all connection in the pool
	 * @return number of Connections closed
	 */
	public synchronized int close()
	{
		int count = 0;
		
		for (DbxConnection conn : _bussy)
		{
			conn.closeNoThrow();
			count++;
		}
		for (DbxConnection conn : _free)
		{
			conn.closeNoThrow();
			count++;
		}
		
		// Set new lists for free/bussy
		_free  = new LinkedList<>();
		_bussy = new LinkedList<>();

		return count;
	}
}
