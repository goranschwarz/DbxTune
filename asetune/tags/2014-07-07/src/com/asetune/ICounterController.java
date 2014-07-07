package com.asetune;

import java.sql.Connection;
import java.util.Date;

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
	 * Do we have a connection to the database?<br>
	 * <b>NOTE:</b> Do NOT call the database to check it, just use the last information we got.
	 * The last status should be maintained everytime a physical check is done via isMonConnected().<br>
	 * On SQLExceptions, we should check if the database connection is still open/valid.
	 * <p>
	 * This is probably called from GUI places where we dont want a fast answer.
	 * @return true or false
	 */
	public boolean isMonConnectedStatus();

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
	 * What time did we do the last connection
	 * @return
	 */
	public Date getMonConnectionTime();

	/**
	 * Time when any counter controller would disconnect/stop collecting data
	 * @param time 
	 */
	public void setMonDisConnectTime(Date time);

	/**
	 * Time when any counter controller would disconnect/stop collecting data
	 * @return null if not set
	 */
	public Date getMonDisConnectTime();

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
