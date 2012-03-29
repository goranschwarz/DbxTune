package com.asetune;

import java.sql.Connection;

import com.asetune.cm.CountersModel;
import com.asetune.gui.ISummaryPanel;

public interface ICounterController
{
	void addCm(CountersModel cm);

	/**
	 * Set the <code>Connection</code> to use for monitoring.
	 */
	public void setMonConnection(Connection conn);

	/**
	 * Gets the <code>Connection</code> to the monitored server.
	 */
	public Connection getMonConnection();

	/**
	 * Do we have a connection to the database?
	 * This one probably calls the isMonConnected(false, true)
	 * @return true or false
	 */
	boolean isMonConnected();

	/**
	 * Do we have a connection to the database?
	 * @param forceConnectionCheck   Force the check, otherwise it's just checks if the re-check time has expired
	 * @param closeConnOnFailure     Close the Connection if the check fails
	 * @return true or false
	 */
	boolean isMonConnected(boolean forceConnectionCheck, boolean closeConnOnFailure);

	/** 
	 * Gets the <code>Connection</code> to the monitored server. 
	 */
	public void closeMonConnection();

	/** 
	 * Cleanup stuff on disconnect 
	 */
	public void cleanupMonConnection();


	/** 
	 * Get <code>CountersModel</code> object for a CM that has the "short" name for example CMprocCallStack 
	 * @return if the CM is not found a null will be return
	 */
	public CountersModel getCmByName(String name);
	
	/** 
	 * Get <code>CountersModel</code> object for a CM that has the "long" name for example 'Procedure Call Stack' for CMprocCallStack
	 * @return if the CM is not found a null will be return
	 */
	public CountersModel getCmByDisplayName(String name);

	/**
	 * Get CountersModel for the Summary
	 */
	public CountersModel getSummaryCm();
	public void          setSummaryCm(CountersModel cm);

	public ISummaryPanel getSummaryPanel();
	public void          setSummaryPanel(ISummaryPanel summaryPanel);

	/**
	 * Get Name for the Summary
	 */
	public String getSummaryCmName();
}
