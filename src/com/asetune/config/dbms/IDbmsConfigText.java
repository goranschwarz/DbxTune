package com.asetune.config.dbms;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

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

	/** check if the AseConfig is initialized or not */
	public boolean isInitialized();

	/**
	 * Initialize 
	 * @param conn
	 * @throws Exception when severe errors like Not Connected anymore, so we can stop initializing...
	 */
	public void initialize(DbxConnection conn, boolean hasGui, boolean offline, Timestamp ts) 
	throws SQLException;

	/**
	 * refresh 
	 * @param conn
	 * @throws Exception when severe errors like Not Connected anymore, so we can stop refreshing...
	 */
	public void refresh(DbxConnection conn, Timestamp ts) 
	throws SQLException;

	/**
	 * Reset ALL configurations, this so we can get new ones later<br>
	 * Most possible called from disconnect() or similar
	 */
	public void reset();


	/**
	 * Check the configuration collected in refresh() and add/create a issue/anomaly if anything strange was found. 
	 */
	public void checkConfig(DbxConnection conn);

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
}
