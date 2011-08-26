/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.pcs;

import java.sql.Timestamp;

import com.asetune.cm.CountersModel;
import com.asetune.utils.Configuration;


/**
 * Implement you'r own forwarding of Alarms to various subsystems.
 * 
 * @author Goran Schwarz
 */
public interface IPersistWriter 
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
	 * Checks if a Session has been started or not, if not started we should call startSession(PersistContainer)
	 */
	public boolean isSessionStarted();

	/**
	 * Set when a sessions has been started and should not be called anymore
	 * @param isSessionStarted
	 */
	public void setSessionStarted(boolean isSessionStarted);
	
	/**
	 * When a new session is started, this method should be used to set the session start time<br>
	 * The PersistCountainerHandler will use {@link #getSessionStartTime()} to set the session start time in {@link PersistContainer#setSessionStartTime(Timestamp)}
	 * @param sessionStartTime
	 */
	public void setSessionStartTime(Timestamp sessionStartTime);

	/**
	 * The PersistCountainerHandler will use {@link #getSessionStartTime()} to set the session start time in {@link PersistContainer#setSessionStartTime(Timestamp)}
	 * @param sessionStartTime
	 */
	public Timestamp getSessionStartTime();

	/** 
	 * When we start a new session, lets call this method to get some 
	 * idea what we are about to sample.
	 * <p>
	 * NOTE: do not forger to set setSessionStarted(true) at the end of this method.
	 * @param cont a PersistContainer filled with <b>all</b> the available
	 *             CounterModels we could sample.
	 */
	public void startSession(PersistContainer cont);
	
	/**
	 * Save a bunch of CM's that we have sampled during this sample interval.
	 * @param cont
	 */
	public void saveSample(PersistContainer cont);

	public boolean isDdlCreated(CountersModel cm); 
	public void    markDdlAsCreated(CountersModel cm);
	public boolean saveDdl(CountersModel cm);

	/** save counters for this cm */
	public void saveCounters(CountersModel cm);

	/**
	 * Called from the {@link PersistentCounterHandler#consume} as the first thing it does.
	 * @param cont 
	 * @see PersistentCounterHandler#consume
	 */
	public void beginOfSample(PersistContainer cont);

	/**
	 * Called from the {@link PersistentCounterHandler#consume} as the last thing it does.
	 * @param cont 
	 * @see PersistentCounterHandler#consume
	 */
	public void endOfSample(PersistContainer cont, boolean caughtErrors);


	/**
	 * Start various service threads etc that this module needs
	 * @throws Exception
	 */
	public void startServices() throws Exception;

	/**
	 * Stop various service threads etc that this module started in startServices()
	 */
	public void stopServices();

	/**
	 * The writer has to have some cind of name...
	 * 
	 * @return name of the Writer
	 */
	public String getName();
	
	
	/*---------------------------------------------------
	** Methods handling counters
	**---------------------------------------------------
	*/
	public void incInserts();
	public void incUpdates();
	public void incDeletes();

	public void incInserts(int cnt);
	public void incUpdates(int cnt);
	public void incDeletes(int cnt);

	public void incCreateTables();
	public void incAlterTables();
	public void incDropTables();

	public void incCreateTables(int cnt);
	public void incAlterTables (int cnt);
	public void incDropTables  (int cnt);

	
	public int getInserts();
	public int getUpdates();
	public int getDeletes();

	public int getCreateTables();
	public int getAlterTables();
	public int getDropTables();

	
	public void resetCounters();
}
