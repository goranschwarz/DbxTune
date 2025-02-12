/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.dbxtune.cm.CmSummaryAbstract;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.ISummaryPanel;
import com.dbxtune.gui.swing.GTable.ITableTooltip;
import com.dbxtune.hostmon.HostMonitorConnection;
import com.dbxtune.pcs.PersistContainer.HeaderInfo;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

import it.sauronsoftware.cron4j.Scheduler;

public interface ICounterController
{
	public enum DbmsOption
	{
		
		/**
		 * For SQL-Server 2019, check if option to get Query Plan from <code>sys.dm_exec_query_plan</code> (the estimated query plan) or <code>sys.dm_exec_query_plan_stats</code> (last actual query plan) 
		 */
		SQL_SERVER__LAST_QUERY_PLAN_STATS
	};
	
	void addCm(CountersModel cm);

	/**
	 * Set the <code>Connection</code> to use for monitoring.
	 */
//	public void setMonConnection(Connection conn);
	public void setMonConnection(DbxConnection conn);

	/**
	 * Gets the <code>Connection</code> to the monitored server.
	 */
//	public Connection getMonConnection();
	public DbxConnection getMonConnection();

	/**
	 * Clone the Monitor connection...
	 * 
	 * @param appName
	 * @return
	 * @throws Exception
	 */
	public DbxConnection cloneMonConnection(String appName)
	throws Exception;

	/**
	 * After a Monitor Connection has been created, this is called so we can do various static settings on the newly created connection
	 */
	public void onMonConnect(DbxConnection conn);

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
	 * Get a List of all CM's 
	 * @return a list of all CM's
	 */
	public List<CountersModel> getCmList();
	
	/** 
	 * Get <code>CountersModel</code> object for a CM that has the "long" name for example 'Procedure Call Stack' for CMprocCallStack
	 * @return if the CM is not found a null will be return
	 */
	public CountersModel getCmByDisplayName(String name);

	/**
	 * Get CountersModel for the Summary
	 */
//	public CountersModel getSummaryCm();
//	public void          setSummaryCm(CountersModel cm);
	public CmSummaryAbstract getSummaryCm();
	public void          setSummaryCm(CmSummaryAbstract cm);

	public ISummaryPanel getSummaryPanel();
	public void          setSummaryPanel(ISummaryPanel summaryPanel);

	/**
	 * Get Name for the Summary
	 */
	public String getSummaryCmName();

	/** initialize the collector */
	public void init()
	throws Exception;

	/** start the collector */
	public void start();

	/** shutdown or stop any collector */
	public void shutdown();

	
	
	
	boolean isHostMonConnected();
//	SshConnection getHostMonConnection();
	HostMonitorConnection getHostMonConnection();
//	void setHostMonConnection(SshConnection sshConn);
	void setHostMonConnection(HostMonitorConnection hostMonConn);
	void closeHostMonConnection();

	boolean isRefreshing();
	void doRefresh();
	void enableRefresh();
	void disableRefresh();
	void doInterruptSleep();

	Timestamp getStatisticsFirstSampleTime();
	Timestamp getStatisticsLastSampleTime();
	void resetStatisticsTime();

	void initCounters(DbxConnection conn, boolean b, long srvExecutableVersionNum, boolean clusterEnabled, long mdaVersion) throws Exception;

	void reset(boolean b);

	String getSupportedProductName();
	void setSupportedProductName(String supportedProductName);


	
//	List<CountersModel> getCmListDependsOnConfig(String srvConfig, DbxConnection conn, long srvVersionNum, boolean isClusterEnabled);
	List<CountersModel> getCmListDependsOnConfig(String srvConfig, DbxConnection conn);
	void setWaitEvent(String string);
	void createCounters(boolean hasGui);
	boolean sleep(int i);
	void setSleeping(boolean b);
	boolean isSleeping();
	String getWaitEvent();
	boolean isRefreshEnabled();
	boolean isInitialized();
	void setInRefresh(boolean b);
	void setStatisticsTime(Timestamp _mainSampleTime);
	HeaderInfo createPcsHeaderInfo();
	void checkServerSpecifics();
	String getServerTimeCmd();
	boolean isSqlBatchingSupported();

	ITableTooltip createCmToolTipSupplier(CountersModel cm);

	boolean     isCmInDemandRefreshList(String name);
	void        addCmToDemandRefreshList(String name);
	void        removeCmFromDemandRefreshList(String name);
	void        clearCmDemandRefreshList();
	Set<String> getCmDemandRefreshList();
	int         getCmDemandRefreshListCount();
	int         getCmDemandRefreshSleepTime(int suggestedSleepTime, long lastRefreshTimeInMs);

	/** How many milliseconds did we spend in last refresh. diff between setInRefresh(true) -> setInRefresh(false) */
	public long getLastRefreshTimeInMs();
	
	/** Descide if the Connection wachdog should be started or not for GUI mode monitoring */
	public boolean shouldWeStart_connectionWatchDog();

	/**
	 * no-gui: get a new connection and check "stuff"<br>
	 * This will be called from the CounterCollectorThreadNoGui when a new connection to the DBMS is needed (on startup, or if the connection is lost)
	 * <p>
	 * If you want it to reconnect automatically: create a <code>ConnectionProp() & DbxConnection.setDefaultConnProp(cp)</code> and set the desired properties
	 * <p>
	 * 
	 * @param dbmsHostPortStr 
	 * @param dbmsServer 
	 * @param dbmsPassword 
	 * @param dbmsUsername 
	 * @param jdbcUrlOptions 
	 * 
	 * @throws SQLException if Connection was NOT ok, but we want to retry the connection later
	 * @throws Exception if Connection was NOT ok, and we wont do not want to retry, the application will Exit/STOP
	 */
	public DbxConnection noGuiConnect(String dbmsUsername, String dbmsPassword, String dbmsServer, String dbmsHostPortStr, String jdbcUrlOptions) throws SQLException, Exception;

	/**
	 * no-gui: On Connection error, this will be called<br>
	 * In here every DBMS implementor can check for <i>invalid password</i> or other <i>unrecoverable errors</i> and in those 
	 * cases throw an exception so the server wont start... and just goes into a re-connect state that will continue forever<br>
	 * 
	 * @param ex              Exception that occurred on Connection Failure
	 * @param dbmsUsername
	 * @param dbmsPassword
	 * @param dbmsServer
	 * @param dbmsHostPortStr
	 * @param jdbcUrlOptions
	 */
	void noGuiConnectErrorHandler(SQLException ex, String dbmsUsername, String dbmsPassword, String dbmsServer, String dbmsHostPortStr, String jdbcUrlOptions) throws Exception;


	/** Set the normal sleep time between refresh */
	void setDefaultSleepTimeInSec(int sleepTime);
	/** Get the normal sleep time between refresh */
	int  getDefaultSleepTimeInSec();
	
	/** 
	 * Set a DBMS property, Would normally be done when connecting to it.<br>
	 * We would get some extra information that is NOT generic over all databases, like 'Edition', or 'PageSize'...<br>
	 * or some other specifics for that DBMS Vendor that is NOT populated by another vendor.<br> 
	 * 
	 * Initial use of this would be to check if we connected to a SQL-Server 'Azure' instance<br>
	 * In that case the Dynamic Management Views will an alternate name.
	 * 
	 * @return The previous value stored here (null if no value existed)
	 */
	Object  setDbmsProperty(String name, String  value);
	int     setDbmsProperty(String name, int     value);
	boolean setDbmsProperty(String name, boolean value);
	
	/** Check if we have a DBMS property, This is DBMS Vendor Specific Settings... but NOT DBMS Configurations */
	boolean hasDbmsProperty(String name);
	
	/** Get a DBMS property, This is DBMS Vendor Specific Settings... but NOT DBMS Configurations */
	String  getDbmsProperty    (String name, String  defaultValue);
	int     getDbmsPropertyInt (String name, int     defaultValue);
	boolean getDbmsPropertyBool(String name, boolean defaultValue);

	/** Get a DBMS property, This is DBMS Vendor Specific Settings... but NOT DBMS Configurations */
	Configuration getDbmsProperty();

	/**
	 * Check if a specific DBMS Option is enabled 
	 * 
	 * @return true if it's enabled... false if it's disabled
	 * @throws RuntimeException if the Counter Cotroller is not yet initialized
	 */
	boolean isDbmsOptionEnabled(DbmsOption dbmsOption);

	/**
	 * This should call all Counter Models and prepare for a PCS Database Rollover
	 */
	void prepareForPcsDatabaseRollover();

	/**
	 * This is called from PCS (in it's own thread) right before 'Daily Summary Report' followed by 'recording database shutdown'
	 * <p>
	 * In here we could do various stuff that we want to extract from the monitored server<br>
	 * An example could be:
	 * <ul>
	 *    <li>SQL-Server: capture (part of) any database(s) Query Store into the Recording database</li>
	 * </ul>
	 * 
	 * @param recordingDbConn Connection to the recording database!
	 */
	void doLastRecordingActionBeforeDatabaseRollover(DbxConnection recordingDbConn);

	/**
	 * Create any cron4j.Scheduler
	 * @param hasGui
	 * @return
	 */
	Scheduler createScheduler(boolean hasGui);

	/** Get the installed scheduler instance */
	Scheduler getScheduler();

	/** Set the installed scheduler instance */
	void setScheduler(Scheduler scheduler);

	/** Starts the installed scheduler, if we have any installed */
	void startScheduler();

	/** Stop the installed scheduler, if we have any installed */
	void stopScheduler();

	/** (in no-gui mode) the Collector can specify '-A' or '--aliasName' this would be that value  */
	String getServerAliasName();

	/** (in no-gui mode) the Collector can specify '-A' or '--aliasName' this would be that value  */
	void setServerAliasName(String aliasName);

	/** (in no-gui mode) the Collector can specify '-N' or '--displayName' this would be that value  */
	String getServerDisplayName();

	/** (in no-gui mode) the Collector can specify '-N' or '--displayName' this would be that value  */
	void setServerDisplayName(String displayName);

	/** Get the server name, which depends on dbms/aliasName and displayName */
	String getServerName();

	/** Get the DBMS Server name */
	String getDbmsServerName();

	/** Set the last known header info, which we can extract server name etc... */
	void setLastKnownHeaderInfo(HeaderInfo headerInfo);

	/** Get the last known header info */
	HeaderInfo getLastKnownHeaderInfo();
}
