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
package com.asetune.gui;

import com.asetune.hostmon.HostMonitorConnection;
import com.asetune.sql.conn.DbxConnection;

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
	public boolean initializeVersionInfo(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception;
	/** If this part should be executed */
	public boolean doInitializeVersionInfo();

	/**
	 * Check if the Server is Configured properly for Monitoring, If it's not you may want to initialize/configure it before we continue.
	 * 
	 * @param conn The Connection 
	 * @param cpd The ConnectionProgressDialog object
	 * @return true on success, false on failure (and the connection sequence will be aborted)
	 * @throws Exception If it's thrown, the Connection sequence will be aborted.
	 */
	public boolean checkMonitorConfig(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception;
	/** If this part should be executed */
	public boolean doCheckMonitorConfig();
	
	/**
	 * Initialize the Monitor Dictionary, that will be used to display tool tips on the column headers
	 * 
	 * @param conn The Connection 
	 * @param cpd The ConnectionProgressDialog object
	 * @return true on success, false on failure (and the connection sequence will be aborted)
	 * @throws Exception If it's thrown, the Connection sequence will be aborted.
	 */
	public boolean initMonitorDictionary(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception;
	/** If this part should be executed */
	public boolean doInitMonitorDictionary();
	
	/**
	 * Initialize the Servers Configuration Dictionary
	 * 
	 * @param conn         The JDBC Connection 
	 * @param hostMonConn  The HostMonitor Connection if any
	 * @param cpd The ConnectionProgressDialog object
	 * @return true on success, false on failure (and the connection sequence will be aborted)
	 * @throws Exception If it's thrown, the Connection sequence will be aborted.
	 */
	public boolean initDbServerConfigDictionary(DbxConnection conn, HostMonitorConnection hostMonConn, ConnectionProgressDialog cpd) throws Exception;
	/** If this part should be executed */
	public boolean doInitDbServerConfigDictionary();
	
	/**
	 * Initialize the Counter Collector
	 * 
	 * @param conn The Connection 
	 * @param cpd The ConnectionProgressDialog object
	 * @return true on success, false on failure (and the connection sequence will be aborted)
	 * @throws Exception If it's thrown, the Connection sequence will be aborted.
	 */
	public boolean initCounterCollector(DbxConnection conn, ConnectionProgressDialog cpd) throws Exception;
	/** If this part should be executed */
	public boolean doInitCounterCollector();
}
