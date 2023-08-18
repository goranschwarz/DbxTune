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
package com.asetune.config.dbms;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import com.asetune.hostmon.HostMonitorConnection;
import com.asetune.sql.conn.DbxConnection;

public interface IDbmsConfigText
{
//	/** What type is this specific Configuration of */
//	public String getConfigType();
	
//	/** get SQL statement to be executed to GET current configuration string 
//	 * @param srvVersion */
//	public String getSqlCurrentConfig(long srvVersion);

	/**
	 * get SQL Statement used to get information from the offline storage
	 * @param ts What Session timestamp are we looking for, null = last Session timestamp
	 * @return
	 */
	public String getSqlOffline(DbxConnection conn, Timestamp ts);


	// ------------------------------------------
	// ---- LOCAL METHODS, probably NOT to be overridden
	// ---- probably NOT to be overridden by implementors
	// ------------------------------------------

	/** get the Config String */
	public String getConfig();

//	/** set the Config String */
//	public void setConfig(String str);

	/** If we have a <i>static</i> comment, that we want to append at the end when displaying the configuration */
	public String getComment();

	/** check if the AseConfig is initialized or not */
	public boolean isInitialized();

	/**
	 * Initialize 
	 * @param conn
	 * @param hostMonConn 
	 * @throws Exception when severe errors like Not Connected anymore, so we can stop initializing...
	 */
	public void initialize(DbxConnection conn, HostMonitorConnection hostMonConn, boolean hasGui, boolean offline, Timestamp ts) 
	throws SQLException;

	/**
	 * refresh 
	 * @param conn
	 * @param hostMonConn 
	 * @throws Exception when severe errors like Not Connected anymore, so we can stop refreshing...
	 */
	public void refresh(DbxConnection conn, HostMonitorConnection hostMonConn, Timestamp ts) 
	throws SQLException;

	/**
	 * Reset ALL configurations, this so we can get new ones later<br>
	 * Most possible called from disconnect() or similar
	 */
	public void reset();


	/**
	 * Check the configuration collected in refresh() and add/create a issue/anomaly if anything strange was found. 
	 */
	public void checkConfig(DbxConnection conn, HostMonitorConnection hostMonConn);

	/**
	 * What server version do we need to get this information.
	 * @return an integer version in the form 12549 for version 12.5.4.9, 0 = all version
	 */
	public long needVersion();

	/**
	 * If server needs to be Cluster Edition to get this information.
	 * @return true or false
	 */
	public boolean needCluster();

	/**
	 * We need any of the roles to access information.
	 * @return List<String> of role(s) we must be apart of to get config. null = do not need any role.
	 */
	public List<String> needRole();

	/**
	 * 
	 * @return List<String> of configurations(s) that must be true.
	 */
	public List<String> needConfig();

	/**
	 * Get the name of the class the implements the IDmbsConfigText
	 * @return
	 */
	public String getName();

	/**
	 * Get name for the Label of the TabbedPane
	 * @return
	 */
	public String getTabLabel();


	public String getSyntaxEditingStyle();

	/**
	 * When executing the statements, should we remember the various states like: AutoCommit, CurrentCatalog
	 * @return true to remember set back the states, false if we dont care
	 */
	public boolean getKeepDbmsState();


	/**
	 * Timeout value that can be used when communicating with the server
	 * @return
	 */
	public int getSqlTimeout();

	/**
	 * Get a list of numbers to discard, null if this isn't used.
	 * @return A list of numbers to discard, null if this isn't used.
	 */
	public List<Integer> getDiscardDbmsErrorList();

	/**
	 * Should we get the configuration or not for this specific configuration
	 * @return true if it's enabled or false if not
	 */
	boolean isEnabled();


//	/**
//	 * Like 'isEnabled()' but in here we can check for various things on the DbxConnection and return a message why this was skipped if ww can't do the check!
//	 * @param conn
//	 * @return null or empty string if we should proceed, otherwise a message why this config check cant be done.
//	 */
//	String getSkipReason(DbxConnection conn);


	/**
	 * Checks if we meet all the requirements for this configuration check
	 * <p>
	 * This typically does
	 * <ul>
	 *   <li>Check if we can get the configuration, due to compatible version</li>
	 *   <li>Check if we can get the configuration, due to cluster</li>
	 *   <li>Check if we can get the configuration, due to enough rights/role based.</li>
	 *   <li>etc</li>
	 * </ul>
	 * 
	 * Override this if you have special needs...
	 * 
	 * @param conn
	 * @return null or empty string if we should proceed, otherwise a message why this configuration check can't be checked.
	 */
	String checkRequirements(DbxConnection conn, HostMonitorConnection hostMonConn);
}
