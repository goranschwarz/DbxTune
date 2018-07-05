package com.asetune.sql.conn;

import java.awt.Window;
import java.sql.SQLException;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.asetune.utils.Configuration;
import com.asetune.utils.TimeUtils;

public class DbxConnectionPool
{
	private static Logger _logger = Logger.getLogger(DbxConnectionPool.class);

	private static DbxConnectionPool _instance = null;

	private LinkedList<DbxConnection> _free  = new LinkedList<>();
	private LinkedList<DbxConnection> _bussy = new LinkedList<>();

	private ConnectionProp _connProp = null;
	private int    _maxSize = 30;
	private String _name    = ""; // Name of the connection pool 

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
			
			_bussy.addLast(conn);
			return conn;
		}
		
		// If all connections are bussy... throw exception
		int bussySize = _bussy.size();
		if (bussySize > _maxSize)
		{
			throw new SQLException("All connection in the pool '"+_name+"' are used. MaxSize="+_maxSize);
		}
		
		// Create a new connection
		try
		{
			DbxConnection conn = DbxConnection.connect(guiOwner, _connProp);
			_bussy.addLast(conn);
			return conn;
		}
		catch (SQLException ex) 
		{
			throw ex;
		}
		catch (Exception ex) 
		{
			throw new SQLException("Problems creating a new connection for the pool '"+_name+"'. Caught: "+ex, ex);
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
		_bussy.remove(conn);

		// Check if the connection is OK (before we put it in the free pool)
		try
		{
			if ( ! conn.isValid(1) )
			{
				_logger.warn("When returning the connection '"+conn+"' to the connection pool '"+_name+"', the connection was no longer valid.");
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

			_logger.warn("When returning the connection '"+conn+"' to the connection pool '"+_name+"', the connection was no longer valid.");
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
				if (secondsSinceConnect > closeThreshold)
				{
					_logger.info("When returning the connection '"+conn+"' to the connection pool '"+_name+"', the 'keepalive' expired, CLOSING the connection. A new connection will be made on next attempt. (secondsSinceConnect="+secondsSinceConnect+", threshold="+closeThreshold+"). This can be changed with the property '"+propName+"=secondsToLive'.");
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
