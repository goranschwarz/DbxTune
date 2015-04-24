package com.asetune.config.dbms;

import java.sql.Timestamp;
import java.util.List;

import com.asetune.sql.conn.DbxConnection;
import com.asetune.ui.rsyntaxtextarea.AsetuneSyntaxConstants;

public interface IDbmsConfigText
{
//	/** What type is this specific Configuration of */
//	public String getConfigType();
	
//	/** get SQL statement to be executed to GET current configuration string 
//	 * @param aseVersion */
//	public String getSqlCurrentConfig(int aseVersion);

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
	 */
	public void initialize(DbxConnection conn, boolean hasGui, boolean offline, Timestamp ts);

	/**
	 * Reset ALL configurations, this so we can get new ones later<br>
	 * Most possible called from disconnect() or similar
	 */
	public void reset();


	/**
	 * What server version do we need to get this information.
	 * @return an integer version in the form 12549 for version 12.5.4.9, 0 = all version
	 */
	public int needVersion();

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
	 * refresh 
	 * @param conn
	 */
	public void refresh(DbxConnection conn, Timestamp ts);

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
	 * Timeout value that can be used when comunicationg with the server
	 * @return
	 */
	public int getSqlTimeout();
}
