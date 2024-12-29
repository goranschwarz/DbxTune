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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.dbxtune.central.pcs;

import java.sql.Timestamp;

import com.dbxtune.central.pcs.CentralPcsWriterHandler.NotificationType;
import com.dbxtune.central.pcs.DbxTuneSample.CmEntry;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.pcs.PersistContainer;
import com.dbxtune.utils.Configuration;


/**
 * Implement you'r own forwarding of Alarms to various subsystems.
 * 
 * @author Goran Schwarz
 */
public interface ICentralPersistWriter 
{
	/**
	 * The init() method will be called
	 * so it can configure itself. Meaning reading the "props" and initialize itself.
	 * 
	 * @param props The Configuration (basically a Properties object)
	 * @throws Exception when the initialization fails, you will 
	 *         throw an Exception to tell what was wrong
	 */
	public void init(Configuration props)	
	throws Exception;

	/**
	 * 
	 */
	public void close();

	/**
	 * Get the configuration which this writer is using.
	 */
	public Configuration getConfig();

	/**
	 * Get a "public" string of how the the writer is configured, no not reveal
	 * passwords or sensitive information.
	 */
	public String getConfigStr();

	/**
	 * Simply print the configuration to the log
	 */
	public void printConfig();
	
	/**
	 * Checks if a Session has been started or not, if not started we should call startSession(DbxTuneSample)
	 * @param sessionName 
	 */
	public boolean isSessionStarted(String sessionName);

	/**
	 * Set when a sessions has been started and should not be called anymore
	 * @param isSessionStarted
	 */
	public void setSessionStarted(String sessionName, boolean isSessionStarted);
	
	/**
	 * When a new session is started, this method should be used to set the session start time<br>
	 * The PersistCountainerHandler will use {@link #getSessionStartTime()} to set the session start time in {@link DbxTuneSample#setSessionStartTime(Timestamp)}
	 * @param sessionName 
	 * @param sessionStartTime
	 */
	public void setSessionStartTime(String sessionName, Timestamp sessionStartTime);

	/**
	 * The PersistCountainerHandler will use {@link #getSessionStartTime()} to set the session start time in {@link DbxTuneSample#setSessionStartTime(Timestamp)}
	 * @param sessionName 
	 * @param sessionStartTime
	 */
	public Timestamp getSessionStartTime(String sessionName);

	/** 
	 * When we start a new session, lets call this method to get some 
	 * idea what we are about to sample.
	 * <p>
	 * NOTE: do not forger to set setSessionStarted(true) at the end of this method.
	 * @param sessionName 
	 * @param cont a DbxTuneSample filled with <b>all</b> the available
	 *             CounterModels we could sample.
	 */
	public void startSession(String sessionName, DbxTuneSample cont) throws Exception;
	
	/**
	 * Save a bunch of CM's that we have sampled during this sample interval.
	 * @param cont
	 */
	public void saveSample(DbxTuneSample cont) throws Exception;

//	public boolean isDdlCreated(CmEntry cme); 
//	public void    markDdlAsCreated(CmEntry cme);
//	public boolean saveDdl(CmEntry cme);

	/** save counters for this cm */
	public void saveCounters(CmEntry cme) throws Exception;

//	/** save DDL into "any" storage */
//	public void saveDdlDetails(DdlDetails ddlDetails);

//	/** check if this DDL is stored in the DDL storage, implementer should hold all stored DDL's in a cache */
//	public boolean isDdlDetailsStored(String dbname, String objectName);

//	/** Put the dbname, objectname in a structure/cache which isDdlDetailsStored() can check for */
//	public void markDdlDetailsAsStored(String dbname, String objectName);

//	/** Clear the structure/cache holding markers for what has been saved. This should be called if the database is recreated during runtime, this so we can store new DDL Storage requests */
//	public void clearDdlDetailesCache();
	
//	/** If we opens a database, then get information that is already stored in the database, this so we don't store it "again" */
//	public void populateDdlDetailesCache();
	
//	/** Save a batch of SQL Statements that was captured by any SQL Capture provider/broker */
//	public void saveSqlCaptureDetails(SqlCaptureDetails sqlCaptureDetails);

	/**
	 * Called from the {@link CentralPcsWriterHandler#consume} as the first thing it does.
	 * @param cont 
	 * @see CentralPcsWriterHandler#consume
	 */
//	public void beginOfSample(DbxTuneSample cont);
	public boolean beginOfSample(DbxTuneSample cont) throws Exception;

	/**
	 * Called from the {@link CentralPcsWriterHandler#consume} as the last thing it does.
	 * @param cont 
	 * @see CentralPcsWriterHandler#consume
	 */
	public void endOfSample(DbxTuneSample cont, boolean caughtErrors);

	/**
	 * Notify the writers about "stuff", which the individual writers has to take actions upon. 
	 */
	public void notification(NotificationType type, String str);

	/**
	 * Start various service threads etc that this module needs
	 * @throws Exception
	 */
	public void startServices() throws Exception;

	/**
	 * Stop various service threads etc that this module started in startServices()
	 * 
	 * @param maxWaitTimeInMs maximum time that that the service can wait before gracefully shutdown.
	 *                                0 means, do shutdown now, or "without wait"
	 */
	public void stopServices(int maxWaitTimeInMs);

	/**
	 * The writer has to have some cind of name...
	 * 
	 * @return name of the Writer
	 */
	public String getName();

	/**
	 * Called when the Storage queue size is higher that the warning threshold<br>
	 * The idea is that you can do various stuff in the Writes to help resolving this issue.
	 * @param queueSize
	 * @param thresholdSize 
	 */
	public void storageQueueSizeWarning(int queueSize, int thresholdSize);

	/*---------------------------------------------------
	** Methods handling counters
	**---------------------------------------------------
	*/
	public CentralPcsWriterStatistics getStatistics();
	public void resetCounters();


	
	/*---------------------------------------------------
	** Methods handling Local Metrics
	**---------------------------------------------------
	*/
	public void saveLocalMetricsSample(PersistContainer pc);
}
