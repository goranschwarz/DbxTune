package com.asetune.gui;

import java.sql.Connection;

public interface ConnectionProgressExtraActions
{
	// Just get ASE Version, this will be good for error messages, sent to WEB server, this will write ASE Version in the info...
	/**
	 * Get DB Server Version, this will be good for error messages, sent to WEB server, this will write DB Server Version in the info...
	 * 
	 * @param conn The Connection 
	 * @param cpd The ConnectionProgressDialog object
	 * @return true on success, false on failure (and the connection sequence will be aborted)
	 * @throws Exception If it's thrown, the Connection sequence will be aborted.
	 */
	public boolean initializeVersionInfo(Connection conn, ConnectionProgressDialog cpd) throws Exception;

	/**
	 * Check if the Server is Configured properly for Monitoring, If it's not you may want to initialize/configure it before we continue.
	 * 
	 * @param conn The Connection 
	 * @param cpd The ConnectionProgressDialog object
	 * @return true on success, false on failure (and the connection sequence will be aborted)
	 * @throws Exception If it's thrown, the Connection sequence will be aborted.
	 */
	public boolean checkMonitorConfig(Connection conn, ConnectionProgressDialog cpd) throws Exception;
	
	/**
	 * Initialize the Monitor Dictionary, that will be used to display tool tips on the column headers
	 * 
	 * @param conn The Connection 
	 * @param cpd The ConnectionProgressDialog object
	 * @return true on success, false on failure (and the connection sequence will be aborted)
	 * @throws Exception If it's thrown, the Connection sequence will be aborted.
	 */
	public boolean initMonitorDictionary(Connection conn, ConnectionProgressDialog cpd) throws Exception;
	
	/**
	 * Initialize the Servers Configuration Dictionary
	 * 
	 * @param conn The Connection 
	 * @param cpd The ConnectionProgressDialog object
	 * @return true on success, false on failure (and the connection sequence will be aborted)
	 * @throws Exception If it's thrown, the Connection sequence will be aborted.
	 */
	public boolean initDbServerConfigDictionary(Connection conn, ConnectionProgressDialog cpd) throws Exception;
	
	/**
	 * Initialize the Counter Collector
	 * 
	 * @param conn The Connection 
	 * @param cpd The ConnectionProgressDialog object
	 * @return true on success, false on failure (and the connection sequence will be aborted)
	 * @throws Exception If it's thrown, the Connection sequence will be aborted.
	 */
	public boolean initCounterCollector(Connection conn, ConnectionProgressDialog cpd) throws Exception;
}
