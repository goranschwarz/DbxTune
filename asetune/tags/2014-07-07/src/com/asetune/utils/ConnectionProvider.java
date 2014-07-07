/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.utils;

import java.sql.Connection;

public interface ConnectionProvider
{
	/**
	 * Returns a connection, which is currently used...
	 * @return
	 */
	public Connection getConnection();
	
	/**
	 * Creates a new connection
	 * @param appname
	 * @return
	 */
	public Connection getNewConnection(String appname);
}
