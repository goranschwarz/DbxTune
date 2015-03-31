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
//	public Connection getConnection();
	public DbxConnection getConnection();
	
	/**
	 * Creates a new connection
	 * @param appname
	 * @return
	 */
//	public Connection getNewConnection(String appname);
	public DbxConnection getNewConnection(String appname);
}
