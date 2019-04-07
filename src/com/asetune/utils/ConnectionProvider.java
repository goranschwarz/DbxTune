/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.utils;

import com.asetune.sql.conn.DbxConnection;

public interface ConnectionProvider
{
	/**
	 * Returns a connection, which is currently used...
	 * @return
	 */
	public DbxConnection getConnection();
	
	/**
	 * Creates a new connection
	 * @param appname
	 * @return
	 */
	public DbxConnection getNewConnection(String appname);

	/**
	 * If we have a connection pool, we might want to release the connection
	 * @param conn
	 */
	default void releaseConnection(DbxConnection conn)
	{
		// do nothing
	}
}
