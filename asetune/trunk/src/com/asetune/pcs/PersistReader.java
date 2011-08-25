/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.pcs;

import java.awt.Component;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import org.apache.log4j.Logger;

import com.asetune.GetCounters;
import com.asetune.cm.CountersModel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.SummaryPanel;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.TrendGraph;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;


public class PersistReader
implements Runnable
{
	private static Logger _logger = Logger.getLogger(PersistReader.class);

	private static final String CMD_loadTimelineSlider      = "loadTimelineSlider";
	private static final String CMD_loadSessionGraphs       = "loadSessionGraphs";
	private static final String CMD_loadSessionCms          = "loadSessionCms";
	private static final String CMD_loadSessionCmIndicators = "loadSessionCmIndicators";
	private static final String CMD_loadSummaryCm           = "loadSummaryCm";
	private static final String CMD_loadSessions            = "loadSessions";
	
	/** A connection to the PersitentCounter Storage back end */
	private Connection _conn = null;
	
	// implements singleton pattern
	private static PersistReader _instance = null;

	/** hold information about if current selected sample */
	private HashMap<String,CmIndicator> _currentIndicatorMap = new HashMap<String,CmIndicator>();

	/** hold information about if current session */
	private HashMap<String,SessionIndicator> _sessionIndicatorMap = new HashMap<String,SessionIndicator>();
	
	/** Execution mode, this means if we do the fetch in a background thread or directly in current thread */
	private             int _execMode        = EXEC_MODE_BG;
//	private             int _execMode        = EXEC_MODE_DIRECT;
	public static final int EXEC_MODE_BG     = 0;
	public static final int EXEC_MODE_DIRECT = 1;

	private int _numOfSamplesOnLastRefresh = -1;
	private int _numOfSamplesNow           = -1;
	
	private static String GET_ALL_SESSIONS = 
		"select \"SessionStartTime\", \"ServerName\", \"NumOfSamples\", \"LastSampleTime\" " +
		"from " + PersistWriterBase.getTableName(PersistWriterBase.SESSIONS, null, true) + " "+
		"order by \"SessionStartTime\"";

	private static String GET_SESSION = 
		"select \"SessionStartTime\", \"SessionSampleTime\" " +
		"from " + PersistWriterBase.getTableName(PersistWriterBase.SESSION_SAMPLES, null, true) + " "+
		"where \"SessionStartTime\" = ? " +
		"order by \"SessionSampleTime\"";

	//////////////////////////////////////////////
	//// Constructors
	//////////////////////////////////////////////
//	static
//	{
//		_logger.setLevel(Level.DEBUG);
//	}
	public PersistReader(Connection conn)
	{
		_conn = conn;

		// Start the COMMAND READER THREAD
		start();
	}

	
	/*---------------------------------------------------
	** BEGIN: Instance stuff
	**---------------------------------------------------
	*/
	public static PersistReader getInstance()
	{
		return _instance;
	}

	public static boolean hasInstance()
	{
		return (_instance != null);
	}

	public static void setInstance(PersistReader inst)
	{
		_instance = inst;
	}
	/*---------------------------------------------------
	** END: Instance stuff
	**---------------------------------------------------
	*/

	/*---------------------------------------------------
	** BEGIN: Connection methods
	**---------------------------------------------------
	*/
	/**
	 * Set the <code>Connection</code> to use for getting offline sessions.
	 */
	public void setConnection(Connection conn)
	{
		_conn = conn;
	}

	/**
	 * Gets the <code>Connection</code> to the monitored server.
	 */
	public Connection getConnection()
	{
		return _conn;
	}
	
	/** Close the offline connection */
	public void closeConnection()
	{
		// Close the Offline database connection
		try
		{
			if (isConnected())
			{
				_conn.close();
				if (_logger.isDebugEnabled())
				{
					_logger.debug("Offline connection closed");
				}
			}
		}
		catch (SQLException ev)
		{
			_logger.error("Closing Offline connection", ev);
		}
		_conn = null;
	}

	/**
	 * Are we connected to a offline storage
	 * @return true or false
	 */
	public boolean isConnected()
	{
		if (_conn == null) 
			return false;

		// check the connection itself
		try
		{
			if (_conn.isClosed())
				return false;
		}
		catch (SQLException e)
		{
			return false;
		}

		return true;
	}
	/*---------------------------------------------------
	** END: Connection methods
	**---------------------------------------------------
	*/


	/**
	 * Checks if this looks like a offline database
	 * <p>
	 * Just go and check for a 'set' of known tables that should be there
	 * 
	 * @return true if this looks like a offline db, otherwise false
	 */
	public static boolean isOfflineDb(Connection conn)
	{
		// First check if we are connected
		if (conn == null) 
			return false;
		try
		{
			if (conn.isClosed())
				return false;
		}
		catch (SQLException e)
		{
			return false;
		}

		// Go and check for Tables
		try
		{
			String tabName = PersistWriterBase.getTableName(PersistWriterBase.SESSIONS, null, false);

			// Obtain a DatabaseMetaData object from our current connection        
			DatabaseMetaData dbmd = conn.getMetaData();
	
			ResultSet rs = dbmd.getColumns(null, null, tabName, "%");
			boolean tabExists = rs.next();
			rs.close();

			return tabExists;
		}
		catch (SQLException e)
		{
			_logger.error("Problems checking if this is a valid 'offline' database.", e);
			return false;
		}
	}

	/**
	 * Check if the last fetch from Counter Storage database has any counters
	 * for the CounterModel named
	 * 
	 * @param name Name of the CM to check for
	 */
	public boolean hasCountersForCm(String name)
	{
		CmIndicator cmInd = getIndicatorForCm(name);
		if (cmInd == null)
			return false;

		int rows = cmInd._absRows + cmInd._diffRows + cmInd._rateRows;
		return (rows > 0);
	}

	public CmIndicator getIndicatorForCm(String name)
	{
		CmIndicator cmInd = _currentIndicatorMap.get(name);
		return cmInd;
	}
	

	/**
	 */
	public boolean hasSessionCountersForCm(String name)
	{
		SessionIndicator ind = getSessionIndicatorForCm(name);
		if (ind == null)
			return false;

		int rows = ind._absSamples + ind._diffSamples + ind._rateSamples;
		return (rows > 0);
	}

	public SessionIndicator getSessionIndicatorForCm(String name)
	{
		SessionIndicator ind = _sessionIndicatorMap.get(name);
		return ind;
	}

	public SessionIndicator[] getSessionIndicators()
	{
		SessionIndicator[] si = new SessionIndicator[_sessionIndicatorMap.size()];
		int i=0;
		for (Map.Entry<String,SessionIndicator> entry : _sessionIndicatorMap.entrySet()) 
		{
			//String           key = entry.getKey();
			SessionIndicator val = entry.getValue();
			si[i++] = val;
		}
		return si;
	}
	

	/*---------------------------------------------------
	** BEGIN: Listener stuff
	**---------------------------------------------------
	*/
	EventListenerList   _listenerList  = new EventListenerList();

	/** Add any listeners that want to see changes */
	public void addChangeListener(ChangeListener l)
	{
		_listenerList.add(ChangeListener.class, l);
	}

	/** Remove the listener */
	public void removeChangeListener(ChangeListener l)
	{
		_listenerList.remove(ChangeListener.class, l);
	}

	/** Kicked off when new entries are added */
//	protected void fireStateChanged()
	protected void fireNewSessionsIsAvalilable()
	{
		Object aobj[] = _listenerList.getListenerList();
		for (int i = aobj.length - 2; i >= 0; i -= 2)
		{
			if (aobj[i] == ChangeListener.class)
			{
				ChangeEvent changeEvent = new ChangeEvent(this);
				((ChangeListener) aobj[i + 1]).stateChanged(changeEvent);
			}
		}
	}
	/*---------------------------------------------------
	** END: Listener stuff
	**---------------------------------------------------
	*/

	
	/*---------------------------------------------------
	** BEGIN: Command Execution
	**---------------------------------------------------
	*/
	////.... hmm... this looks a bit dodgy....
	/// mayby rethink this

	private boolean _asynchExec = false;
	public void doXXX()
	{
		doXXX(_asynchExec);
	}
	public void doXXX(/*params*/ boolean asynchExec)
	{
		// Execute later
		if ( asynchExec )
		{
			// prepare a reflection object and post on queue.
			return;
		}

		// Execute at once
		//-do the stuff
	}
	/*---------------------------------------------------
	** END: Command Execution
	**---------------------------------------------------
	*/




	/*---------------------------------------------------
	** BEGIN: background thread
	**---------------------------------------------------
	*/
	// start()
	// shutdown()
	// run()
	//   - command executor (a method execution (using reflection) will be posted on the queue)
	//       - readFromQueue (with timeout of X ms)
	//   - look for new sessions
	//       - if (foundNewOnes) fireNewSessionsIsAvalilable();
	/*---------------------------------------------------
	** END: background thread
	**---------------------------------------------------
	*/
	/*---------------------------------------------------
	** BEGIN: Command thread stuff
	**---------------------------------------------------
	*/
	/** Queue that holds commands for the _readThread */
	private BlockingQueue<QueueCommand> _cmdQueue   = new LinkedBlockingQueue<QueueCommand>();
	private Thread         _readThread = null;
	private boolean        _running    = false;
	private int            _warnQueueSizeThresh = 10;

	/** Place holder object for commands to be executed be the readThread */
	private static class QueueCommand
	{
		private String    _cmd  = null;
		private Timestamp _ts1  = null;
		private Timestamp _ts2  = null;
		private Timestamp _ts3  = null;
		private int       _int1 = -1;
		
		QueueCommand(String cmd, Timestamp ts1, Timestamp ts2, Timestamp ts3, int int1) 
		{
			_cmd  = cmd;
			_ts1  = ts1;
			_ts2  = ts2;
			_ts3  = ts3;
			_int1 = int1;
		}
		QueueCommand(String cmd, Timestamp ts1, Timestamp ts2, Timestamp ts3) 
		{
			this(cmd, ts1, ts2, ts3, -1);
		}
//		QueueCommand(String cmd, Timestamp ts1, Timestamp ts2) 
//		{
//			this(cmd, ts1, ts2, null, -1);
//		}
		QueueCommand(String cmd, Timestamp ts1) 
		{
			this(cmd, ts1, null, null, -1);
		}
		QueueCommand(String cmd) 
		{
			this(cmd, null, null, null, -1);
		}
	}

	private void addCommand(QueueCommand qcmd)
	{
		int qsize = _cmdQueue.size();
		if (qsize > _warnQueueSizeThresh)
		{
			_logger.warn("The Command queue has "+qsize+" entries. The CommandExecutor might not keep in pace.");
		}

		_cmdQueue.add(qcmd);
	}

	public void shutdown()
	{
		_running = false;
		if (_readThread != null)
			_readThread.interrupt();
		_readThread = null;
	}
	public void start()
	{
		if (_readThread != null)
		{
			_logger.info("The thread '"+_readThread.getName()+"' is already running. Skipping the start.");
			return;
		}

		_readThread = new Thread(this);
		_readThread.setName("OfflineSessionReader");
		_readThread.setDaemon(true);
		_readThread.start();
//		SwingUtilities.invokeLater(this);
	}
	public boolean isRunning()
	{
		return _running;
	}

	public void run()
	{
		String threadName = _readThread.getName();
		_logger.info("Starting a thread for the module '"+threadName+"'.");

		_running = true;

		Configuration conf = Configuration.getCombinedConfiguration();
		boolean checkforNewSamples = conf.getBooleanProperty("pcs.read.checkForNewSessions", false);

		while(_running)
		{
			if (_logger.isDebugEnabled())
				_logger.debug("Thread '"+threadName+"', waiting on queue...");

			try 
			{
//				QueueCommand qo = _cmdQueue.take();
				QueueCommand qo = _cmdQueue.poll(2000, TimeUnit.MILLISECONDS);

				// Make sure the container isn't empty.
				// If poll is used, then NULL means poll-timeout
				// lets go and check for new samples (if data is inserted to the PCS at the same time we are reading from it)
				if (qo == null)
				{
					if (checkforNewSamples && isConnected() && _running)
					{
						_numOfSamplesNow = getNumberOfSamplesInLastSession(false);
						if (_numOfSamplesNow > _numOfSamplesOnLastRefresh)
						{
							if (_logger.isDebugEnabled())
								_logger.debug("New samples is available: '"+(_numOfSamplesNow-_numOfSamplesOnLastRefresh)+"'. _numOfSamplesNow='"+_numOfSamplesNow+"', _numOfSamplesOnLastRefresh='"+_numOfSamplesOnLastRefresh+"'.");
							setWatermark( (_numOfSamplesNow-_numOfSamplesOnLastRefresh) + " New samples is available.");
						}
					}
					continue;
				}
				long xStartTime = System.currentTimeMillis();

				// Known commands
				// addCommand(new QueueCommand("loadTimelineSlider", sl.getSampleId(), sl.getPeriodStartTime(), sl.getPeriodEndTime()));
				// addCommand(new QueueCommand("loadSessionGraphs",  sl.getSampleId(), sl.getPeriodStartTime(), sl.getPeriodEndTime()));
				// addCommand(new QueueCommand("loadSessionCms", ts, null, null));

				// DO WORK
				String cmdStr = qo._cmd + "(ts1="+qo._ts1+", ts2="+qo._ts2+", ts3="+qo._ts3+")";
				if      (CMD_loadTimelineSlider     .equals(qo._cmd)) execLoadTimelineSlider     (qo._ts1, qo._ts2, qo._ts3);
				else if (CMD_loadSessionGraphs      .equals(qo._cmd)) execLoadSessionGraphs      (qo._ts1, qo._ts2, qo._ts3, qo._int1);
				else if (CMD_loadSessionCms         .equals(qo._cmd)) execLoadSessionCms         (qo._ts1);
				else if (CMD_loadSessionCmIndicators.equals(qo._cmd)) execLoadSessionCmIndicators(qo._ts1);
				else if (CMD_loadSummaryCm          .equals(qo._cmd)) execLoadSummaryCm          (qo._ts1);
				else if (CMD_loadSessions           .equals(qo._cmd)) execLoadSessions();
				else _logger.error("Unknown command '"+qo._cmd+"' was taken from the queue.");

				long xStopTime = System.currentTimeMillis();
				long consumeTimeMs = xStopTime-xStartTime;
				_logger.debug("It took "+TimeUtils.msToTimeStr(consumeTimeMs)+" to execute: "+cmdStr);
				
			} 
			catch (InterruptedException ex) 
			{
				_running = false;
			}
			catch (Throwable t)
			{
				_logger.error("The thread '"+threadName+"' ran into unexpected problems, but continues, Caught: "+t, t);
			}
		}

		_logger.info("Emptying the queue for module '"+threadName+"', which had "+_cmdQueue.size()+" entries.");
		_cmdQueue.clear();

		_logger.info("Thread '"+threadName+"' was stopped.");
	}

//	public void run() 
//	{
//		_running = true;
//		while(true)
//		{
//			try
//			{
//			}
//			catch (Throwable t)
//			{
//			}
//		}
//	}

	/**
	 * Set execution mode for this module
	 * @param execMode EXEC_MODE_DIRECT to execute in current thread, EXEC_MODE_BG to execute on the commands event thread
	 */
	public void setExecMode(int execMode)
	{
		if (execMode != EXEC_MODE_BG && execMode != EXEC_MODE_DIRECT)
			throw new IllegalArgumentException("Setting exec mode to a unknow value of '"+execMode+"'.");
		_execMode = execMode;
	}

	/**
	 * What execution mode are we in.
	 * @return EXEC_MODE_BG     if we the executor thread is running and execMode is in EXEC_MODE_BG
	 *         EXEC_MODE_DIRECT if we are in direct mode.
	 */
	public int getExecMode()
	{
		if (_running && _execMode == EXEC_MODE_BG)
			return EXEC_MODE_BG;
		else
			return EXEC_MODE_DIRECT;
	}


	/*---------------------------------------------------
	** END: Command thread stuff
	**---------------------------------------------------
	*/





	/*---------------------------------------------------
	** BEGIN: public execution/access methods
	**---------------------------------------------------
	*/
	public void loadTimelineSlider(Timestamp sampleId, Timestamp startTime, Timestamp endTime)
	{
		if (getExecMode() == EXEC_MODE_DIRECT)
		{
			execLoadTimelineSlider(sampleId, startTime, endTime);
			return;
		}
		
		addCommand(new QueueCommand(CMD_loadTimelineSlider,  sampleId, startTime, endTime));
	}

	public void loadSessionGraphs(Timestamp sampleId, Timestamp startTime, Timestamp endTime, int expectedRows)
	{
		if (getExecMode() == EXEC_MODE_DIRECT)
		{
			execLoadSessionGraphs(sampleId, startTime, endTime, expectedRows);
			return;
		}
		
		addCommand(new QueueCommand(CMD_loadSessionGraphs,  sampleId, startTime, endTime, expectedRows));
	}

	public void loadSessionCms(Timestamp sampleId)
	{
		if (getExecMode() == EXEC_MODE_DIRECT)
		{
			execLoadSessionCms(sampleId);
			return;
		}

		addCommand(new QueueCommand(CMD_loadSessionCms, sampleId));
	}
	
	/** 
	 * Load indicators for this sample<br>
	 * NOTE: This will not use the background thread to get data, since various timing issues.
	 * If we are doing it with the background thread, the data might not show up "in time"
	 * so the GUI will simply say "no data for this sample". 
	 * Another thing is if you issue a lot of request if would end up in a loop on the slider 
	 * so it looks like it's just "hopping around" in a indefinite loop due to various 
	 * component actions get's initiated and triggers new action events which then is
	 * handled in a differed way/mode.
	 */
	public void loadSessionCmIndicators(Timestamp sampleId)
	{
		execLoadSessionCmIndicators(sampleId);
//		if (getExecMode() == EXEC_MODE_DIRECT)
//		{
//			execLoadSessionCmIndicators(sampleId);
//			return;
//		}
//
//		addCommand(new QueueCommand(CMD_loadSessionCmIndicators, sampleId));
	}

	public void loadSummaryCm(Timestamp sampleId)
	{
		if (getExecMode() == EXEC_MODE_DIRECT)
		{
			execLoadSummaryCm(sampleId);
			return;
		}

		addCommand(new QueueCommand(CMD_loadSummaryCm, sampleId));
	}

	public void loadSessions()
	{
		if (getExecMode() == EXEC_MODE_DIRECT)
		{
			execLoadSessions();
			return;
		}

		addCommand(new QueueCommand(CMD_loadSessions));
	}

	/**
	 * Get timestamp for PREVIOUS (rewind) cmName that has any data 
	 * @param sampleId
	 * @param currentSampleTime
	 * @param cmName
	 * @return
	 */
	public Timestamp getPrevSample(Timestamp sampleId, Timestamp currentSampleTime, String cmName)
	{
		// Rewind to previous cmName that has data
//		String sql = 
//			"select top 1 \"SessionSampleTime\" \n" +
//			"from \"MonSessionSampleDetailes\" \n" +
////			"where \"SessionStartTime\"  = ? \n" +
//			"where 1=1 \n" +
//			"  and \"SessionSampleTime\" < ? \n" +
//			"  and \"CmName\"            = ? \n" +
//			"  and (\"absRows\" > 0 or \"diffRows\" > 0 or \"rateRows\" > 0) \n" +
//			"order by \"SessionSampleTime\" desc";
		String sql = 
			"select top 1 \"SessionSampleTime\" \n" +
			"from \"MonSessionSampleDetailes\" \n" +
//			"where \"SessionStartTime\"  = '"+sampleId+"' \n" +
			"where 1=1 \n" +
			"  and \"SessionSampleTime\" < '"+currentSampleTime+"' \n" +
			"  and \"CmName\"            = '"+cmName+"' \n" +
			"  and (\"absRows\" > 0 or \"diffRows\" > 0 or \"rateRows\" > 0) \n" +
			"order by \"SessionSampleTime\" desc";
//System.out.println("getPrevSample.sql=\n"+sql);
		try
		{
//			PreparedStatement pstmnt = _conn.prepareStatement(sql);
////			pstmnt.setString(1, sampleId.toString());
////			pstmnt.setString(2, currentSampleTime.toString());
////			pstmnt.setString(3, cmName);
//			pstmnt.setString(1, currentSampleTime.toString());
//			pstmnt.setString(2, cmName);
//
//			ResultSet rs = pstmnt.executeQuery();
			Statement stmnt = _conn.createStatement();
			ResultSet rs = stmnt.executeQuery(sql);

			// Only get first row
			Timestamp ts = null;
			if (rs.next())
				ts = rs.getTimestamp(1);

			rs.close();
			stmnt.close();
			
			return ts;
		}
		catch (SQLException e)
		{
			_logger.error("Problems getting previous sample for '"+cmName+"'.", e);
			return null;
		}
	}
//	-- REWIND
//	select top 1 'rew' as "xxx","SessionSampleTime" from "MonSessionSampleDetailes"
//	where "SessionStartTime" = '2010-05-03 11:51:29.513'
//	  and "SessionSampleTime" < '2010-05-03 13:00:06.166'
//	  and "CmName"='CMActiveStatements'
//	  and ("absRows" > 0 or "diffRows" > 0 or "rateRows" > 0)
//	order by "SessionSampleTime" desc
//
//	-- FAST-FORWARD
//	select top 1 'ff' as "xxx","SessionSampleTime" from "MonSessionSampleDetailes"
//	where "SessionStartTime" = '2010-05-03 11:51:29.513'
//	  and "SessionSampleTime" > '2010-05-03 13:00:06.166'
//	  and "CmName"='CMActiveStatements'
//	  and ("absRows" > 0 or "diffRows" > 0 or "rateRows" > 0)
//	order by "SessionSampleTime" asc

	/**
	 * Get timestamp for NEXT (fast forward) cmName that has any data 
	 * @param sampleId
	 * @param currentSampleTime
	 * @param cmName
	 * @return
	 */
	public Timestamp getNextSample(Timestamp sampleId, Timestamp currentSampleTime, String cmName)
	{
		// Fast Forward to next cmName that has data
//		String sql = 
//			"select top 1 \"SessionSampleTime\" \n" +
//			"from \"MonSessionSampleDetailes\" \n" +
////			"where \"SessionStartTime\"  = ? \n" +
//			"where 1=1 \n" +
//			"  and \"SessionSampleTime\" > ? \n" +
//			"  and \"CmName\"            = ? \n" +
//			"  and (\"absRows\" > 0 or \"diffRows\" > 0 or \"rateRows\" > 0) \n" +
//			"order by \"SessionSampleTime\" asc";
		String sql = 
			"select top 1 \"SessionSampleTime\" \n" +
			"from \"MonSessionSampleDetailes\" \n" +
//			"where \"SessionStartTime\"  = '"+sampleId+"' \n" +
			"where 1=1 \n" +
			"  and \"SessionSampleTime\" > '"+currentSampleTime+"' \n" +
			"  and \"CmName\"            = '"+cmName+"' \n" +
			"  and (\"absRows\" > 0 or \"diffRows\" > 0 or \"rateRows\" > 0) \n" +
			"order by \"SessionSampleTime\" asc";
//System.out.println("getNextSample.sql=\n"+sql);

		try
		{
//			PreparedStatement pstmnt = _conn.prepareStatement(sql);
////			pstmnt.setString(1, sampleId.toString());
////			pstmnt.setString(2, currentSampleTime.toString());
////			pstmnt.setString(3, cmName);
//			pstmnt.setString(1, currentSampleTime.toString());
//			pstmnt.setString(2, cmName);
//
//			ResultSet rs = pstmnt.executeQuery();
			Statement stmnt = _conn.createStatement();
			ResultSet rs = stmnt.executeQuery(sql);

			// Only get first row
			Timestamp ts = null;
			if (rs.next())
				ts = rs.getTimestamp(1);

			rs.close();
			stmnt.close();
			
			return ts;
		}
		catch (SQLException e)
		{
			_logger.error("Problems getting next sample for '"+cmName+"'.", e);
			return null;
		}
	}

	@SuppressWarnings("unused")
	public int getNumberOfSamplesInLastSession(boolean printErrors)
	{
//		 SessionStartTime              ServerName                     NumOfSamples LastSampleTime
//		 ----------------------------- ------------------------------ ------------ -----------------------------
//		 Apr 11 2011  4:53:09.006000PM GORAN_1_DS                              410 Apr 11 2011  5:28:40.066000PM
//		 Apr 11 2011  5:29:09.256000PM GORAN_1_DS                               13 Apr 11 2011  5:30:17.286000PM

		String sql = 
			"select * \n" +
			"from \"MonSessions\" \n" +
			"order by \"LastSampleTime\" \n";
		try
		{
			Statement stmnt = _conn.createStatement();
			ResultSet rs = stmnt.executeQuery(sql);

			// Get all rows, but we only save the last rows...
			// I could have done: select top 1 * from MonSessions  order by LastSampleTime desc
			// but that isn't that portal as getting all records... 
			// Note: It should only be a few rows in the table anyway... 
			Timestamp sessionStartTime = null;
			String    serverName       = null;
			int       numOfSamples     = -1;
			Timestamp lastSampleTime = null;

			while (rs.next())
			{
				sessionStartTime = rs.getTimestamp(1);
				serverName       = rs.getString   (2);
				numOfSamples     = rs.getInt      (3);
				lastSampleTime   = rs.getTimestamp(4);
			}

			rs.close();
			stmnt.close();
			
			return numOfSamples;
		}
		catch (SQLException e)
		{
			String msg = e.getMessage();
			if (msg != null && msg.toLowerCase().indexOf("timeout") >= 0)
			{
				_logger.info ("Got 'timeout' when reading the MonSessions table, retrying this later.");
				_logger.debug("Got 'timeout' when reading the MonSessions table, retrying this later. Message: "+msg);
				return -1;
			}

			if (printErrors)
				_logger.error("Problems getting number of samples from last session sample.", e);
			return -1;
		}
	}
	/*---------------------------------------------------
	** END: public execution/access methods
	**---------------------------------------------------
	*/

	
	
	/*---------------------------------------------------
	** BEGIN: 
	**---------------------------------------------------
	*/
	/** containes OfflineCm */
	private LinkedHashMap<String,OfflineCm> _offlineCmMap = null;

	private class OfflineCm
	{
		public String             name      = "";
		public boolean            hasAbs    = false;
		public boolean            hasDiff   = false;
		public boolean            hasRate   = false;
		public LinkedList<String> graphList = null;

		public OfflineCm(String name) 
		{
			this.name = name;
		}
		public void add(String name, String type)
		{
			if (type == null)
				return;
			if      (type.equals("abs"))  hasAbs  = true;
			else if (type.equals("diff")) hasDiff = true;
			else if (type.equals("rate")) hasRate = true;
			else
			{
				if (graphList == null)
					graphList = new LinkedList<String>();
				graphList.add(type);
			}
		}
		public String toString()
		{
			return "OfflineCm(name='"+name+"', hasAbs="+hasAbs+", hasDiff="+hasDiff+", hasRate="+hasRate+", graphList="+graphList+")";
		}
	}
	private void addOfflineCm(String name, String type)
	{
		OfflineCm ocm = _offlineCmMap.get(name);
		if (ocm == null)
		{
			ocm = new OfflineCm(name);
			_offlineCmMap.put(ocm.name, ocm);
		}
		ocm.add(name, type);
	}

	private void getStoredCms(boolean refresh)
	{
		// No need to refresh this list
		if (_offlineCmMap != null && !refresh)
			return;

		_offlineCmMap = new LinkedHashMap<String,OfflineCm>();
		ResultSet rs = null;

		try 
		{
			// Obtain a DatabaseMetaData object from our current connection
			DatabaseMetaData dbmd = _conn.getMetaData();
	
	//		rs = dbmd.getTables(null, null, "%", null);
			rs = dbmd.getTables(null, null, "%", new String[] { "TABLE" });

			while(rs.next())
			{
				String tableName = rs.getString("TABLE_NAME");
				String tableType = rs.getString("TABLE_TYPE");
	
				int sepPos = tableName.indexOf("_");
				if (sepPos < 0)
					continue;
				String name = tableName.substring(0, sepPos);
				String type = tableName.substring(sepPos+1);
				if (_logger.isDebugEnabled())
					_logger.debug("getStoredCms()-rs-row- TYPE='"+tableType+"', NAME='"+tableName+"'. name='"+name+"', type='"+type+"'");
				
				addOfflineCm(name, type);
			}
//			ResultSetTableModel tab = new ResultSetTableModel(rs);
//			System.out.println("getStoredCms()-3\n" + tab.toTableString());
			rs.close();

			// Sort the information according to the Counter Tab's in MainFarme
			sortOfflineCm();

			if (_logger.isDebugEnabled())
				_logger.debug("_offlineCmMap="+_offlineCmMap);
		}
		catch (SQLException e)
		{
			_logger.error("Problems getting Offlined table names.", e);
		}
	}

	/**
	 * Sort all available tables in the same order as the "tab" placement in the Main GUI
	 */
	private void sortOfflineCm()
	{
		JTabbedPane tabPane = MainFrame.getTabbedPane();
		if (tabPane == null)
			return;

		// New Map that we will add the sorted rows into
//		LinkedHashMap<String,OfflineCm> originOfflineCmMap = _offlineCmMap.clone();
		LinkedHashMap<String,OfflineCm> originOfflineCmMap = new LinkedHashMap<String,OfflineCm>(_offlineCmMap);
		LinkedHashMap<String,OfflineCm> sortedOfflineCmMap = new LinkedHashMap<String,OfflineCm>();

		// Holds 'names' that wasn't found in the TAB, add those at the end
		LinkedList<String> misses = new LinkedList<String>();

		int tabCount = tabPane.getTabCount();
		for (int t=0; t<tabCount; t++)
		{
			Component comp = tabPane.getComponentAt(t);
			String name = comp.getName();
			
			_logger.debug("sortOfflineCm() Working on tab "+t+": "+name);

			// Get the OfflineCm for this TAB and store it in the new Map
			OfflineCm ocm = originOfflineCmMap.get(name);
			if (ocm == null)
			{
				// If the OCM can't be found in the list, it just means that
				// The CM has no tables in the Counter Storage
				// So we dont really need the code for "misses", but lets keep it for now.
				//misses.add(name);
			}
			else
			{
				// move it over to the new sorted Map
				sortedOfflineCmMap.put(name, ocm);
				originOfflineCmMap.remove(name);

				// If we have more than 1 graph, go and sort those things as well
				if (ocm.graphList != null && ocm.graphList.size() > 1)
				{
					if (comp instanceof TabularCntrPanel)
					{
						TabularCntrPanel tcp = (TabularCntrPanel) comp;
						CountersModel    cm  = tcp.getCm();
						if (cm != null)
						{
							Map<String,TrendGraph> graphs = cm.getTrendGraphs();
							if (graphs != null && graphs.size() > 1)
							{
//								LinkedList<String> originGraphList = (LinkedList) ocm.graphList.clone();
								LinkedList<String> originGraphList = new LinkedList<String>(ocm.graphList);
								LinkedList<String> sortedGraphList = new LinkedList<String>();

								// Loop how the Graphs was initially added to the CM
								for (Map.Entry<String,TrendGraph> entry : graphs.entrySet()) 
								{
									String     key = entry.getKey();
									//TrendGraph val = entry.getValue();

									// move it over to the new sorted List
									sortedGraphList.add(key);
									originGraphList.remove(key);
								}
								// Check for errors, all entries should have been removed from the "work" map
								if (originGraphList.size() > 0)
								{
									_logger.warn("The sorting of 'ocm.graphList' for ocm '"+name+"' failed. Continuing with old/unsorted List");
									_logger.debug("sortOfflineCm() originGraphList: "+originGraphList);
									_logger.debug("sortOfflineCm() sortedGraphList: "+sortedGraphList);
									_logger.debug("sortOfflineCm() ocm.graphList:   "+ocm.graphList);
								}
								else
								{
									ocm.graphList = sortedGraphList;
								}
							}
						}
					}
				}
			}
		}
		// Now move the ones left over to the 
		if (misses.size() > 0)
		{
			for (Iterator<String> it = misses.iterator(); it.hasNext();)
			{
				String name = it.next();
				OfflineCm ocm = originOfflineCmMap.get(name);

				sortedOfflineCmMap.put(name, ocm);
				originOfflineCmMap.remove(name);
				it.remove();
			}
		}
		// Check for errors, all entries should have been removed from the "work" map
		if (originOfflineCmMap.size() > 0)
		{
			_logger.warn("The sorting of '_offlineCmMap' failed. Continuing with old/unsorted Map");
			_logger.debug("sortOfflineCm() originOfflineCmMap: "+originOfflineCmMap);
			_logger.debug("sortOfflineCm() sortedOfflineCmMap: "+sortedOfflineCmMap);
			_logger.debug("sortOfflineCm() misses:             "+misses);
			_logger.debug("sortOfflineCm() _offlineCmMap:      "+_offlineCmMap);
		}
		else
		{
			_offlineCmMap = sortedOfflineCmMap;
		}
		return;
	}


	private int loadSessionGraph(String cmName, String graphName, Timestamp sampleId, Timestamp startTime, Timestamp endTime, int expectedRows)
	{
		CountersModel cm = GetCounters.getCmByName(cmName);
		if (cm == null)
		{
			_logger.warn("Can't find any CM named '"+cmName+"'.");
			return 0;
		}
		TrendGraph tg = cm.getTrendGraph(graphName);
		if (tg == null)
		{
			_logger.warn("Can't find any TrendGraph named '"+graphName+"', for the CM '"+cmName+"'.");
			return 0;
		}
		tg.clearGraph();

		//----------------------------------------
		// TYPICAL look of a graph table
		//----------------------------------------
		// CREATE TABLE "CMengineActivity_cpuSum"
		//   "SessionStartTime"   DATETIME        NOT NULL,
		//   "SessionSampleTime"  DATETIME        NOT NULL,
		//   "SampleTime"         DATETIME        NOT NULL,
		//   "label_0"            VARCHAR(30)         NULL,
		//   "data_0"             NUMERIC(10, 1)      NULL,
		//   "label_1"            VARCHAR(30)         NULL,
		//   "data_1"             NUMERIC(10, 1)      NULL,
		//   "label_2"            VARCHAR(30)         NULL,
		//   "data_2"             NUMERIC(10, 1)      NULL

		String sql = "select * from \""+cmName+"_"+graphName+"\" " +
		             "where \"SessionStartTime\" = ? " +
		             "  AND \"SessionSampleTime\" >= ? " +
		             "  AND \"SessionSampleTime\" <= ? " +
		             "order by \"SessionSampleTime\"";

		// If we expect a big graph, load only every X row
		// if we add to many to the graph, the JVM takes 100% CPU, I'm guessing it 
		// has to do too many repaints, we could do an "average" of X rows during the load
		// but I took the easy way out... (or figure out why it's taking all the CPU)
		int loadEveryXRow = expectedRows / 1000 + 1; // 1 = load every row

		try
		{
			long fetchStartTime = System.currentTimeMillis();
			setStatusText("Loading graph '"+graphName+"' for '"+cmName+"'.");

			PreparedStatement pstmnt = _conn.prepareStatement(sql);
//			pstmnt.setTimestamp(1, sampleId);
//			pstmnt.setTimestamp(2, startTime);
//			pstmnt.setTimestamp(3, endTime);
			pstmnt.setString(1, sampleId.toString());
			pstmnt.setString(2, startTime.toString());
			pstmnt.setString(3, endTime.toString());

			_logger.debug("loadSessionGraph(cmName='"+cmName+"', graphName='"+graphName+"') loadEveryXRow="+loadEveryXRow+": "+pstmnt);

			ResultSet rs = pstmnt.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();

			int cols = rsmd.getColumnCount();
			String[] labels = new String[(cols-3)/2];
			Double[] datapt = new Double[(cols-3)/2];
			boolean firstRow = true;
			int row = 0;

//			Timestamp sessionStartTime  = null;
			Timestamp sessionSampleTime = null;
//			Timestamp sampleTime        = null;

			while (rs.next())
			{
//				sessionStartTime  = rs.getTimestamp(1);
				sessionSampleTime = rs.getTimestamp(2);
//				sampleTime        = rs.getTimestamp(3);

				// Start to read column 4
				// move c (colIntex) 2 cols at a time, move ca (ArrayIndex) by one
				for (int c=4, ca=0; c<=cols; c+=2, ca++)
				{
					labels[ca] = rs.getString(c);
					datapt[ca] = new Double(rs.getDouble(c+1));					
				}
				// Add a extra record at the BEGINING of the traces... using 0 data values
				if (firstRow)
				{
					firstRow = false;
					Double[] firstDatapt = new Double[datapt.length];
					for (int d=0; d<firstDatapt.length; d++)
						firstDatapt[d] = new Double(0);
					tg.addPoint(new Timestamp(sessionSampleTime.getTime()-10),  // - 10 millisec
							firstDatapt, 
							labels, startTime, endTime);
				}

				// If we expect a big graph, load only every X row
				// if we add to many to the graph, the JVM takes 100% CPU, I'm guessing it 
				// has to do too many repaints, we could do an "average" of X rows during the load
				// but I took the easy way out... (or figure out why it's taking all the CPU)
				if ( row % loadEveryXRow == 0 )
					tg.addPoint(sessionSampleTime, datapt, labels, startTime, endTime);

				row++;
			}
			rs.close();
			pstmnt.close();

			// Add a extra record at the end of the traces... using 0 data values
			if (sessionSampleTime != null)
			{
				Double[] lastDatapt = new Double[datapt.length];
				for (int d=0; d<lastDatapt.length; d++)
					lastDatapt[d] = new Double(0);
				tg.addPoint(new Timestamp(sessionSampleTime.getTime()+10), // + 10 millisec
						lastDatapt, 
						labels, startTime, endTime);
			}
//System.out.println("Loaded "+row+" rows into TrendGraph named '"+graphName+"', for the CM '"+cmName+"', which took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.");
			_logger.debug("Loaded "+row+" rows into TrendGraph named '"+graphName+"', for the CM '"+cmName+"', which took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.");
			setStatusText("");
			
			return loadEveryXRow;
		}
		catch (SQLException e)
		{
			_logger.error("Problems loading graph for cm='"+cmName+"', graph='"+graphName+"'.", e);
		}
		return 0;
	}

	private void execLoadSessionGraphs(Timestamp sampleId, Timestamp startTime, Timestamp endTime, int expectedRows)
	{
		setWatermark("Loading graphs...");
//		System.out.println("loadSession(sampleId='"+sampleId+"', startTime='"+startTime+"', endTime='"+endTime+"')");
		long xStartTime = System.currentTimeMillis();

		// Populate _offlineCmMap
		getStoredCms(false);

		int loadEveryXRow = 0;

		// Write "HH:mm - HH:mm" of what we are watching in the MainFrame's watermark
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		String graphWatermark = sdf.format(startTime) + " - " + sdf.format(endTime);
		MainFrame.setOfflineSamplePeriodText(graphWatermark);

		// Now loop the _offlineCmMap
		for (Map.Entry<String,OfflineCm> entry : _offlineCmMap.entrySet())
		{
			String cmName = entry.getKey();
			OfflineCm ocm = entry.getValue();

			//System.out.println("loadSessionGraphs(): LOOP, cmName='"+cmName+"', ocm='"+ocm+"'.");
			if (ocm == null)           continue; // why should this happen
			if (ocm.graphList == null) continue;

			for (String  graphName : ocm.graphList)
			{
				loadEveryXRow = loadSessionGraph(cmName, graphName, sampleId, startTime, endTime, expectedRows);
			}
		}
		String str = "Loading all TrendGraphs took '"+TimeUtils.msToTimeStr("%SS.%ms", System.currentTimeMillis()-xStartTime)+"' seconds.";
		if (loadEveryXRow > 1)
			str += " Loaded every "+(loadEveryXRow-1)+" row, graphs was to big.";
		setStatusText(str);
		setWatermark();
	}

	@SuppressWarnings("unused")
	private void execLoadTimelineSlider(Timestamp sampleId, Timestamp startTime, Timestamp endTime)
	{
		long xStartTime = System.currentTimeMillis();
		setWatermark("Refreshing Timeline slider...");

		MainFrame mf = MainFrame.getInstance();
		if ( mf == null )
			return;

		mf.resetOfflineSlider();
		
		String sql = "select \"SessionSampleTime\" " +
		             "from " + PersistWriterBase.getTableName(PersistWriterBase.SESSION_SAMPLES, null, true) + " "+
		             "where \"SessionStartTime\" = ? " +
		             "  AND \"SessionSampleTime\" >= ? " +
		             "  AND \"SessionSampleTime\" <= ? " +
		             "order by \"SessionSampleTime\"";
		try
		{
			PreparedStatement pstmnt = _conn.prepareStatement(sql);
//			pstmnt.setTimestamp(1, sampleId);
//			pstmnt.setTimestamp(2, startTime);
//			pstmnt.setTimestamp(3, endTime);
			pstmnt.setString(1, sampleId.toString());
			pstmnt.setString(2, startTime.toString());
			pstmnt.setString(3, endTime.toString());

			ResultSet rs = pstmnt.executeQuery();

			ArrayList<Timestamp> tsList = new ArrayList<Timestamp>();
			while (rs.next())
			{
				Timestamp sessionSampleTime = rs.getTimestamp(1);
				tsList.add(sessionSampleTime);
//				mf.addOfflineSliderEntry(sessionSampleTime);
			}

			rs.close();
			pstmnt.close();

			mf.addOfflineSliderEntryList(tsList);
		}
		catch (SQLException e)
		{
			_logger.error("Problems loading Timeline slider.", e);
		}
//System.out.println("loadTimelineSlider(sampleId='"+sampleId+"', startTime='"+startTime+"', endTime='"+endTime+"'), which took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-xStartTime)+"'.");
		setWatermark();
	}

	private void execLoadSessionCms(Timestamp sampleId)
	{
		setWatermark("Loading Counters...");
//		System.out.println("loadSessionCms(sampleId='"+sampleId+"')");

		long fetchStartTime = System.currentTimeMillis();
		setStatusText("Loading all counters for time '"+sampleId+"'.");

		// Populate _offlineCmMap
		getStoredCms(false);

		// Now loop the _offlineCmMap
		for (Map.Entry<String,OfflineCm> entry : _offlineCmMap.entrySet()) 
		{
			//String cmName = entry.getKey();
			OfflineCm ocm = entry.getValue();

			loadSessionCm(ocm, sampleId);
		}

		String str = "Loading took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.";
		_logger.debug(str);
		setStatusText(str);
		setWatermark();
	}
	private void loadSessionCm(OfflineCm ocm, Timestamp sampleTs)
	{
		if (ocm == null || sampleTs == null)
			throw new IllegalArgumentException("OfflineCm or sampleTs cant be null");

		String cmName = ocm.name;
		CountersModel cm = GetCounters.getCmByName(cmName);
		if (cm == null)
		{
			_logger.warn("Cant find any CM named '"+cmName+"'.");
			return;
		}
		
		// Remove all the rows in the CM, so that new can be added
		// if this is not done, all the old rows will still be visible when displaying it in the JTable
		cm.clearForRead();

		if (ocm.hasAbs)  loadSessionCm(cm, CountersModel.DATA_ABS,  sampleTs);
		if (ocm.hasDiff) loadSessionCm(cm, CountersModel.DATA_DIFF, sampleTs);
		if (ocm.hasRate) loadSessionCm(cm, CountersModel.DATA_RATE, sampleTs);

		cm.setDataInitialized(true);
		cm.fireTableStructureChanged();
		if (cm.getTabPanel() != null)
			cm.getTabPanel().adjustTableColumnWidth();
	}
	@SuppressWarnings("unused")
	private void loadSessionCm(CountersModel cm, int type, Timestamp sampleTs)
	{
		if (cm       == null) throw new IllegalArgumentException("CountersModel can't be null");
		if (sampleTs == null) throw new IllegalArgumentException("sampleTs can't be null");
		
		String cmName = cm.getName();

		String typeStr = null;
		if      (type == CountersModel.DATA_ABS)  typeStr = "abs";
		else if (type == CountersModel.DATA_DIFF) typeStr = "diff";
		else if (type == CountersModel.DATA_RATE) typeStr = "rate";
		else throw new IllegalArgumentException("Unknown type of "+type+".");

		long fetchStartTime = System.currentTimeMillis();
		setStatusText("Loading '"+typeStr+"' counters for '"+cmName+"'.");

		//----------------------------------------
		// TYPICAL look of a graph table
		//----------------------------------------
		// CREATE TABLE "CMengineActivity_abs"  << abs|diff|rate
		//     "SessionStartTime"  DATETIME NOT NULL,
		//     "SessionSampleTime" DATETIME NOT NULL,
		//     "SampleTime"        DATETIME NOT NULL,
		//     "SampleMs"          INT      NOT NULL,
		//     "col1"              datatype     null,
		//     "col2"              datatype     null,
		//     "...."              datatype     null,
		//
		String sql = "select * from \""+cmName+"_"+typeStr+"\" " +
		             "where \"SessionSampleTime\" = ? ";
		String sql2 = "select * from \""+cmName+"_"+typeStr+"\" " +
		             "where \"SessionSampleTime\" = '"+sampleTs+"' ";

		try
		{
//			PreparedStatement pstmnt = _conn.prepareStatement(sql);
//			pstmnt.setTimestamp(1, sampleTs);
//			pstmnt.setObject(1, sampleTs);
			Statement pstmnt = _conn.createStatement();
			

			_logger.debug("loadSessionCm(cmName='"+cmName+"', type='"+typeStr+"', sampleTs='"+sampleTs+"'): "+pstmnt);

//			ResultSet rs = pstmnt.executeQuery();
			ResultSet rs = pstmnt.executeQuery(sql2);
			ResultSetMetaData rsmd = rs.getMetaData();
			int cols = rsmd.getColumnCount();
			int row  = 0;

//			Object oa[] = new Object[cols-4]; // Object Array
//			Object ha[] = new String[cols-4]; // Header Array  (Column Names)
			int colSqlDataType[] = new int[cols];
			List<String> colHead = new ArrayList<String>(cols);

			// Get headers / colNames
			for (int c=5; c<=cols; c++)
			{
//				ha[c-4] = rsmd.getColumnLabel(c);
				colHead.add(rsmd.getColumnLabel(c));
				colSqlDataType[c-1] = rsmd.getColumnType(c);
			}
			cm.setColumnNames(type, colHead);
//			cm.setColumnName(type, c);
//			cm.setColumnClass(type, c);

			// Get Rows
			while (rs.next())
			{
				Timestamp sessionStartTime  = rs.getTimestamp(1);
				Timestamp sessionSampleTime = rs.getTimestamp(2);
				Timestamp sampleTime        = rs.getTimestamp(3);
				int       sampleMs          = rs.getInt(4);

//				cm.setSampleTimeHead(sessionStartTime);
				cm.setSampleTimeHead(sessionSampleTime);
				cm.setSampleTime(sampleTime);
				cm.setSampleInterval(sampleMs);
				
				for (int c=5,col=0; c<=cols; c++,col++)
				{
//					oa[col] = rs.getObject(c);
					Object colVal = rs.getObject(c);
					
					// Some datatypes we need to take extra care of
					if (colSqlDataType[c-1] == Types.CLOB)
						colVal = rs.getString(c);

					cm.setValueAt(type, colVal, row, col);
				}
				
				row++;
			}
			rs.close();
			pstmnt.close();

			// now rows was found, do something?
			if (row == 0)
			{
			}

//System.out.println(Thread.currentThread().getName()+": Loaded "+row+" rows into for the CM '"+cmName+"', type='"+typeStr+"', which took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"' for sampleTs '"+sampleTs+"'.");
			_logger.debug("Loaded "+row+" rows into for the CM '"+cmName+"', type='"+typeStr+"', which took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"' for sampleTs '"+sampleTs+"'.");
			setStatusText("");
		}
		catch (SQLException e)
		{
			_logger.error("Problems loading cm='"+cmName+"', type='"+typeStr+"'.", e);
		}
	}

	
	private void execLoadSummaryCm(Timestamp sampleTs)
	{
		if (sampleTs == null)
			throw new IllegalArgumentException("sampleTs can't be null");

		String cmName = SummaryPanel.CM_NAME;
		CountersModel cm = GetCounters.getCmByName(cmName);
		if (cm == null)
		{
			_logger.warn("Cant find any CM named '"+cmName+"'.");
			return;
		}
		if (true) loadSessionCm(cm, CountersModel.DATA_ABS,  sampleTs);
		if (true) loadSessionCm(cm, CountersModel.DATA_DIFF, sampleTs);
		if (true) loadSessionCm(cm, CountersModel.DATA_RATE, sampleTs);

		cm.setDataInitialized(true);
		cm.fireTableStructureChanged();
		if (cm.getTabPanel() != null)
			cm.getTabPanel().adjustTableColumnWidth();

		// Load/mark everything else...
		MainFrame.getInstance().setTimeLinePoint(sampleTs);
	}
	
//	private void execLoadSessionIndicators(Timestamp sessionStartTs)
//	{
//		setWatermark("Loading Session Indicators...");
////		System.out.println("execLoadSessionIndicators(sessionStartTime='"+sessionStartTs+"')");
//
//		long fetchStartTime = System.currentTimeMillis();
//		setStatusText("Loading Session indicators for sessionStartTime '"+sessionStartTs+"'.");
//
//		// Reset the Map...
//		_sessionIndicatorMap.clear();
//
//		//----------------------------------------
//		// TYPICAL look of a graph table
//		//----------------------------------------
//		//create table MonSessionSampleDetailes
//		//(
//		//   SessionStartTime  datetime  not null,
//		//   CmName            varchar   not null,
//		//   absSamples        int           null,
//		//   diffSamples       int           null,
//		//   rateSamples       int           null
//		//)
//		String sql = "select * from " + PersistWriterBase.getTableName(PersistWriterBase.SESSION_SAMPLE_SUM, null, true) + " " +
//		             "where \"SessionStartTime\" = ? ";
//
//		try
//		{
//			PreparedStatement pstmnt = _conn.prepareStatement(sql);
////			pstmnt.setTimestamp(1, sampleTs);
//			pstmnt.setString(1, sessionStartTs.toString());
//			
//
//			_logger.debug("loadSessionIndicators(sampleTs='"+sessionStartTs+"'): "+pstmnt);
//
//			ResultSet rs = pstmnt.executeQuery();
//			ResultSetMetaData rsmd = rs.getMetaData();
//			int cols = rsmd.getColumnCount();
//			int row  = 0;
//
//			// Get Rows
//			while (rs.next())
//			{
//				Timestamp sessionStartTime  = rs.getTimestamp(1);
//				String    cmName            = rs.getString(2);
//				int       absSamples        = rs.getInt(3);
//				int       diffSamples       = rs.getInt(4);
//				int       rateSamples       = rs.getInt(5);
//
//				SessionIndicator sessInd = new SessionIndicator(sessionStartTime, cmName, absSamples, diffSamples, rateSamples);
//
//				// Add it to the indicators map
//				_sessionIndicatorMap.put(cmName, sessInd);
//				
//				row++;
//			}
//			rs.close();
//			pstmnt.close();
//
////System.out.println("Loaded "+row+" indicators for ts '"+sampleTs+"' , which took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.");
//			_logger.debug("Loaded "+row+" session indicators for ts '"+sessionStartTs+"' , which took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.");
//			setStatusText("");
//		}
//		catch (SQLException e)
//		{
//			_logger.error("Problems loading session indicators for ts '"+sessionStartTs+"'.", e);
//		}
//
//		String str = "Loading session indicators took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.";
//		_logger.debug(str);
//		setStatusText(str);
//		setWatermark();
//	}

	private void execLoadSessionCmIndicators(Timestamp sampleTs)
	{
		setWatermark("Loading Counter Indicators...");
//		System.out.println("loadSessionCmIndicators(sampleId='"+sampleTs+"')");

		long fetchStartTime = System.currentTimeMillis();
		setStatusText("Loading all counter indicators for time '"+sampleTs+"'.");

		// Reset the Map...
		_currentIndicatorMap.clear();

		//----------------------------------------
		// TYPICAL look of a graph table
		//----------------------------------------
		//create table MonSessionSampleDetailes
		//(
		//   SessionStartTime  datetime  not null,
		//   SessionSampleTime datetime  not null,
		//   CmName            varchar   not null,
		//   type              int           null,
		//   graphCount        int           null,
		//   absRows           int           null,
		//   diffRows          int           null,
		//   rateRows          int           null
		//)
		String sql = "select * from " + PersistWriterBase.getTableName(PersistWriterBase.SESSION_SAMPLE_DETAILES, null, true) + " " +
		             "where \"SessionSampleTime\" = ? ";

		try
		{
			PreparedStatement pstmnt = _conn.prepareStatement(sql);
//			pstmnt.setTimestamp(1, sampleTs);
			pstmnt.setString(1, sampleTs.toString());
			

			_logger.debug("loadSessionCmIndicators(sampleTs='"+sampleTs+"'): "+pstmnt);

			ResultSet rs = pstmnt.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();
			int cols = rsmd.getColumnCount();
			int row  = 0;
			boolean hasSqlGuiRefreshTime = cols >= 9;

			// Get Rows
			while (rs.next())
			{
				Timestamp sessionStartTime  = rs.getTimestamp(1);
				Timestamp sessionSampleTime = rs.getTimestamp(2);
				String    cmName            = rs.getString(3);
				int       type              = rs.getInt(4);
				int       graphCount        = rs.getInt(5);
				int       absRows           = rs.getInt(6);
				int       diffRows          = rs.getInt(7);
				int       rateRows          = rs.getInt(8);
				int       sqlRefreshTime    = hasSqlGuiRefreshTime ? rs.getInt(9)  : -1;
				int       guiRefreshTime    = hasSqlGuiRefreshTime ? rs.getInt(10) : -1;
				int       lcRefreshTime     = hasSqlGuiRefreshTime ? rs.getInt(11) : -1;

				CmIndicator cmInd = new CmIndicator(sessionStartTime, sessionSampleTime, cmName, type, graphCount, absRows, diffRows, rateRows, sqlRefreshTime, guiRefreshTime, lcRefreshTime);

				// Add it to the indicators map
				_currentIndicatorMap.put(cmName, cmInd);
				
				row++;
			}
			rs.close();
			pstmnt.close();

//System.out.println("Loaded "+row+" indicators for ts '"+sampleTs+"' , which took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.");
			_logger.debug("Loaded "+row+" indicators for ts '"+sampleTs+"' , which took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.");
			setStatusText("");
		}
		catch (SQLException e)
		{
			_logger.error("Problems loading cm indicators for ts '"+sampleTs+"'.", e);
		}

		String str = "Loading indicators took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.";
		_logger.debug(str);
		setStatusText(str);
		setWatermark();
	}

	public void setCurrentSampleTime(Timestamp ts)
	{
		loadSessionCmIndicators(ts);
	}

	public CountersModel getCmForSample(String name, Timestamp sampleTs)
	{
		CmIndicator cmInd = getIndicatorForCm(name);
		if (cmInd == null)
		{
			if (_logger.isDebugEnabled())
			{
				_logger.debug("No CmIndicator was found in 'map' for cm named '"+name+"' with ts '"+sampleTs+"'.");
				// Print the content for this sample
				for (Map.Entry<String,CmIndicator> entry : _currentIndicatorMap.entrySet()) 
				{
					String      key = entry.getKey();
					CmIndicator val = entry.getValue();

					_logger.debug("IndicatorMap: key="+StringUtil.left(key, 20)+": CmIndicator="+val);
				}
			}
			return null;
		//	throw new RuntimeException("No CmIndicator was found in 'map' for cm named '"+name+"'.");
		}

		return loadSessionCm(cmInd, sampleTs);
	}

	public CountersModel loadSessionCm(CmIndicator cmInd, Timestamp sampleTs)
	{
		if (cmInd    == null) throw new IllegalArgumentException("CmIndicator can't be null");
		if (sampleTs == null) throw new IllegalArgumentException("sampleTs can't be null");

		String cmName = cmInd._cmName;
		CountersModel cm = GetCounters.getCmByName(cmName);
		if (cm == null)
		{
			_logger.warn("Cant find any CM named '"+cmName+"'.");
			return null;
		}

		// Make a new object, which the data will be attached to
		// current CM is reused, then the fireXXX will be done and TableModel.get* will fail.
		cm = cm.copyForOfflineRead();

		if (cmInd._absRows  > 0) loadSessionCm(cm, CountersModel.DATA_ABS,  sampleTs);
		if (cmInd._diffRows > 0) loadSessionCm(cm, CountersModel.DATA_DIFF, sampleTs);
		if (cmInd._rateRows > 0) loadSessionCm(cm, CountersModel.DATA_RATE, sampleTs);

		cm.setSqlRefreshTime(cmInd._sqlRefreshTime);
		cm.setGuiRefreshTime(cmInd._guiRefreshTime);
		cm.setLcRefreshTime (cmInd._lcRefreshTime);

		cm.setDataInitialized(true);
//		cm.fireTableStructureChanged();
//		if (cm.getTabPanel() != null)
//			cm.getTabPanel().adjustTableColumnWidth();
		
		return cm;
	}

	
	private void execLoadSessions()
	{
		setWatermark("Loading Sessions...");

		long fetchStartTime = System.currentTimeMillis();
		setStatusText("Loading Sessions....");

		// get a LIST of sessions
		List<SessionInfo> sessionList = getSessionList();
	
		// Loop the sessions and load all samples 
		for (SessionInfo sessionInfo : sessionList)
		{
			// Load all samples for this sampleId
			sessionInfo._sampleList         = getSessionSamplesList       (sessionInfo._sessionId);
			sessionInfo._sampleCmNameSumMap = getSessionSampleCmNameSumMap(sessionInfo._sessionId);
		}

		// Get number of samples in LAST session
		_numOfSamplesOnLastRefresh = getNumberOfSamplesInLastSession(true);

		_logger.debug("loadSessions(COMPLETED) calling listeners");
		for (INotificationListener nl : _notificationListeners)
		{
			nl.setSessionList(sessionList);
		}

		String str = "Loading Sessions took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.";
		_logger.debug(str);
		setStatusText(str);
		setWatermark();
	}

	public List<SessionInfo> getSessionList()
	{
		setWatermark("Loading Sessions..."); // if the window wasn't visible, set the watermark now
		long fetchStartTime = System.currentTimeMillis();
		setStatusText("Loading Session List....");

		List<SessionInfo> sessions = new LinkedList<SessionInfo>();

		try
		{
			Statement stmnt   = _conn.createStatement();
			ResultSet rs      = stmnt.executeQuery(GET_ALL_SESSIONS);
			while (rs.next())
			{
				Timestamp sessionId      = rs.getTimestamp("SessionStartTime");
				Timestamp lastSampleTime = rs.getTimestamp("LastSampleTime");
				int       numOfSamples   = rs.getInt("NumOfSamples");

				sessions.add( new SessionInfo(sessionId, lastSampleTime, numOfSamples) );
			}
			rs.close();
			stmnt.close();
		}
		catch (SQLException e)
		{
			_logger.error("Problems inititialize...", e);
		}

		String str = "Loading Session List took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.";
		_logger.debug(str);
		setStatusText(str);
		
		return sessions;
	}

	public List<Timestamp> getSessionSamplesList(Timestamp sessionId)
	{
		setWatermark("Loading Sessions...");  // if the window wasn't visible, set the watermark now
		long fetchStartTime = System.currentTimeMillis();
		setStatusText("Loading Session Samples for sessionId '"+sessionId+"'.");

		List<Timestamp> list = new LinkedList<Timestamp>();

		try
		{
			PreparedStatement pstmnt = _conn.prepareStatement(GET_SESSION);
//			pstmnt.setTimestamp(1, sessionId);
			pstmnt.setString(1, sessionId.toString());

			ResultSet rs = pstmnt.executeQuery();
			while (rs.next())
			{
				list.add(rs.getTimestamp("SessionSampleTime"));
			}
			rs.close();
			pstmnt.close();
		}
		catch (SQLException e)
		{
			_logger.error("Problems inititialize...", e);
		}
		
		String str = "Loading Session Samples for sessionId '"+sessionId+"' took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.";
		_logger.debug(str);
		setStatusText(str);
		
		return list;
	}

	public Map<String, CmNameSum> getSessionSampleCmNameSumMap(Timestamp sessionStartTime)
	{
		setWatermark("Loading Sessions...");  // if the window wasn't visible, set the watermark now
		long fetchStartTime = System.currentTimeMillis();
		setStatusText("Loading Session Samples CM Summy for sessionId '"+sessionStartTime+"'.");

		// create table MonSessionSampleSum
		// (
		//    SessionStartTime datetime     not null,
		//    CmName           varchar(30)  not null,
		//    absSamples       int              null,
		//    diffSamples      int              null,
		//    rateSamples      int              null,
		// 
		//    PRIMARY KEY (SessionStartTime, CmName)
		// )

		Map<String, CmNameSum> map = new LinkedHashMap<String, CmNameSum>();

		String sql = 
			"select * " +
			"from " + PersistWriterBase.getTableName(PersistWriterBase.SESSION_SAMPLE_SUM, null, true) + " " +
			"where \"SessionStartTime\" = ? ";

		try
		{
			PreparedStatement pstmnt = _conn.prepareStatement(sql);
//			pstmnt.setTimestamp(1, sessionStartTime);
			pstmnt.setString(1, sessionStartTime.toString());

			ResultSet rs = pstmnt.executeQuery();
			while (rs.next())
			{
				Timestamp ssTime      = rs.getTimestamp(1);
				String    cmName      = rs.getString   (2);
				int       absSamples  = rs.getInt      (3);
				int       diffSamples = rs.getInt      (4);
				int       rateSamples = rs.getInt      (5);
				
				map.put(cmName, new CmNameSum(ssTime, cmName, absSamples, diffSamples, rateSamples));
			}
			rs.close();
			pstmnt.close();
		}
		catch (SQLException e)
		{
			_logger.error("Problems loading 'session sample cm summary' for ts '"+sessionStartTime+"'. sql="+sql, e);
		}
		
		String str = "Loading Session Samples for sessionStartTime '"+sessionStartTime+"' took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.";
		_logger.debug(str);
		setStatusText(str);
		
		return map;
	}
	
	public Map<Timestamp, SampleCmCounterInfo> getSessionSampleCmCounterInfoMap(Timestamp inSessionStartTime)
	{
		setWatermark("Loading Sessions...");  // if the window wasn't visible, set the watermark now
		long fetchStartTime = System.currentTimeMillis();
		setStatusText("Loading 'Session Samples CM Counter Info' for sessionId '"+inSessionStartTime+"'.");

		// CREATE TABLE "MonSessionSampleDetailes"
		// (
		//     "SessionStartTime"  DATETIME    NOT NULL,
		//     "SessionSampleTime" DATETIME    NOT NULL,
		//     "CmName"            VARCHAR(30) NOT NULL,
		//     "type"              INT,
		//     "graphCount"        INT,
		//     "absRows"           INT,
		//     "diffRows"          INT,
		//     "rateRows"          INT
		// )

		Map<Timestamp, SampleCmCounterInfo> map = new LinkedHashMap<Timestamp, SampleCmCounterInfo>();

		String sql = 
			"select * " +
			"from " + PersistWriterBase.getTableName(PersistWriterBase.SESSION_SAMPLE_DETAILES, null, true) + " " +
			"where \"SessionStartTime\" = ? " +
			"order by \"SessionSampleTime\" ";

		SampleCmCounterInfo scmci = null;
		try
		{
			PreparedStatement pstmnt = _conn.prepareStatement(sql);
//			pstmnt.setTimestamp(1, sessionStartTime);
			pstmnt.setString(1, inSessionStartTime.toString());

			long lastSampleTime = 0;
			ResultSet rs = pstmnt.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();
			int cols = rsmd.getColumnCount();
			boolean hasSqlGuiRefreshTime = cols >= 9;

			while (rs.next())
			{
				CmCounterInfo cmci = new CmCounterInfo();
				cmci._sessionStartTime  = rs.getTimestamp(1);
				cmci._sessionSampleTime = rs.getTimestamp(2);
				cmci._cmName            = rs.getString   (3);
				cmci._type              = rs.getInt      (4);
				cmci._graphCount        = rs.getInt      (5);
				cmci._absRows           = rs.getInt      (6);
				cmci._diffRows          = rs.getInt      (7);
				cmci._rateRows          = rs.getInt      (8);
				cmci._sqlRefreshTime    = hasSqlGuiRefreshTime ? rs.getInt(9)  : -1;
				cmci._guiRefreshTime    = hasSqlGuiRefreshTime ? rs.getInt(10) : -1;
				cmci._lcRefreshTime     = hasSqlGuiRefreshTime ? rs.getInt(11) : -1;

				if (lastSampleTime != cmci._sessionSampleTime.getTime())
				{
					lastSampleTime = cmci._sessionSampleTime.getTime();
//if (scmci != null)
//	System.out.println("XXXX: scmci._ciMap.size()="+scmci._ciMap.size()+" entries in: ts='"+scmci._sessionSampleTime+"'.");

					scmci = new SampleCmCounterInfo(cmci._sessionStartTime, cmci._sessionSampleTime);
					//scmci._ciMap = new..done in obj

					// Add this new entry to the OUTER map
					map.put(scmci._sessionSampleTime, scmci);
//System.out.println("getSessionSampleCmCounterInfoMap: NEW: ts='"+scmci._sessionSampleTime+"'.");
				}
				// Now add the row(CmCounterInfo) to the sample/agregate object (SampleCmCounterInfo)
				scmci._ciMap.put(cmci._cmName, cmci);
			}
			rs.close();
			pstmnt.close();
		}
		catch (SQLException e)
		{
			_logger.error("Problems loading 'session sample cm counter info' for ts '"+inSessionStartTime+"'. sql="+sql, e);
		}
		
		String str = "Loading 'Session Samples CM Counter Info' for sessionId '"+inSessionStartTime+"' took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.";
System.out.println(str);
		_logger.debug(str);
		setStatusText(str);
		
		return map;
	}
	
	public MonVersionInfo getMonVersionInfo(Timestamp sessionStartTime)
	{
		// CREATE TABLE "MonVersionInfo"
		// (
		//    "SessionStartTime" DATETIME    NOT NULL,
		//    "ProductString"    VARCHAR(30) NOT NULL,
		//    "VersionString"    VARCHAR(30) NOT NULL,
		//    "BuildString"      VARCHAR(30) NOT NULL,
		//    "SourceDate"       VARCHAR(30) NOT NULL,
		//    "SourceRev"        INT         NOT NULL
		// )

		String sql = 
			"select * " +
			"from " + PersistWriterBase.getTableName(PersistWriterBase.VERSION_INFO, null, true) + " " +
			"where \"SessionStartTime\" = ? ";

		try
		{
			PreparedStatement pstmnt = _conn.prepareStatement(sql);
//			pstmnt.setTimestamp(1, sessionStartTime);
			pstmnt.setString(1, sessionStartTime.toString());

			MonVersionInfo monVersionInfo = new MonVersionInfo();
			ResultSet rs = pstmnt.executeQuery();
			while (rs.next())
			{
				monVersionInfo._sessionStartTime = rs.getTimestamp(1);
				monVersionInfo._productString    = rs.getString   (2);
				monVersionInfo._versionString    = rs.getString   (3);
				monVersionInfo._buildString      = rs.getString   (4);
				monVersionInfo._sourceDate       = rs.getString   (5);
				monVersionInfo._sourceRev        = rs.getInt      (6);
			}
			rs.close();
			pstmnt.close();
			
			return monVersionInfo;
		}
		catch (SQLException e)
		{
			_logger.error("Problems loading 'MonVersionInfo' for ts '"+sessionStartTime+"'. sql="+sql, e);
			return null;
		}
	}


	/*---------------------------------------------------
	** END: 
	**---------------------------------------------------
	*/
	/** a reflection of one row in: MonVersionInfo */
	public static class MonVersionInfo
	{
		public Timestamp _sessionStartTime = null;
		public String    _productString    = null;
		public String    _versionString    = null;
		public String    _buildString      = null;
		public String    _sourceDate       = null;
		public int       _sourceRev        = 0;
	}

	/** a reflection of one row in: MonSessionSampleDetailes */
	public static class CmCounterInfo
	{
		public Timestamp _sessionStartTime  = null;
		public Timestamp _sessionSampleTime = null;
		public String    _cmName            = null;
		public int       _type              = -1;
		public int       _graphCount        = -1;
		public int       _absRows           = -1;
		public int       _diffRows          = -1;
		public int       _rateRows          = -1;
		public int       _sqlRefreshTime    = -1;
		public int       _guiRefreshTime    = -1;
		public int       _lcRefreshTime     = -1;

		public CmCounterInfo()
		{
		}
		public CmCounterInfo(CmCounterInfo copyMe)
		{
			_sessionStartTime  = copyMe._sessionStartTime;
			_sessionSampleTime = copyMe._sessionSampleTime;
			_cmName            = copyMe._cmName;
			_type              = copyMe._type;
			_graphCount        = copyMe._graphCount;
			_absRows           = copyMe._absRows;
			_diffRows          = copyMe._diffRows;
			_rateRows          = copyMe._rateRows;
			_sqlRefreshTime    = copyMe._sqlRefreshTime;
			_guiRefreshTime    = copyMe._guiRefreshTime;
			_lcRefreshTime     = copyMe._lcRefreshTime;
		}
	}
	public static class SampleCmCounterInfo
	{
		public Timestamp _sessionStartTime  = null;
		public Timestamp _sessionSampleTime = null;
		public Map<String, CmCounterInfo> _ciMap = new LinkedHashMap<String, PersistReader.CmCounterInfo>();

		public SampleCmCounterInfo(Timestamp sessionStartTime, Timestamp sessionSampleTime)
		{
			_sessionStartTime  = sessionStartTime;
			_sessionSampleTime = sessionSampleTime;
		}

		public void merge(SampleCmCounterInfo in)
		{
			if (in == null)
				return;
			
			// loop input 
			for (Map.Entry<String,CmCounterInfo> entry : in._ciMap.entrySet()) 
			{
				String        cmName = entry.getKey();
				CmCounterInfo cmci   = entry.getValue();
				if ( ! this._ciMap.containsKey(cmName) )
				{
					this._ciMap.put(cmName, new CmCounterInfo(cmci));
				}
				else
				{
					CmCounterInfo localCmci = this._ciMap.get(cmName);
					localCmci._graphCount += cmci._graphCount;
					localCmci._absRows    += cmci._absRows;
					localCmci._diffRows   += cmci._diffRows;
					localCmci._rateRows   += cmci._rateRows;

					localCmci._sqlRefreshTime += cmci._sqlRefreshTime;
					localCmci._guiRefreshTime += cmci._guiRefreshTime;
					localCmci._lcRefreshTime  += cmci._lcRefreshTime;
				}
			}
		}
	}

	/** a reflection of one row in: MonSessionSampleSum */
	public static class CmNameSum
	{
		public Timestamp _sessionStartTime = null;
		public String    _cmName           = null;
		public int       _absSamples       = 0;
		public int       _diffSamples      = 0;
		public int       _rateSamples      = 0;

		public CmNameSum(Timestamp sessionStartTime, String cmName, int absSamples, int diffSamples, int rateSamples)
		{
			_sessionStartTime = sessionStartTime;
			_cmName           = cmName;
			_absSamples       = absSamples;
			_diffSamples      = diffSamples;
			_rateSamples      = rateSamples;
		}
	}

	public static class SessionInfo
	{
		public Timestamp              _sessionId          = null;
		public Timestamp              _lastSampleTime     = null;
		public int                    _numOfSamples       = -1;
		public List<Timestamp>        _sampleList         = null;
		public Map<String, CmNameSum> _sampleCmNameSumMap = null;
		public Map<Timestamp, SampleCmCounterInfo> _sampleCmCounterInfoMap = null;

		public SessionInfo(Timestamp sessionId, Timestamp lastSampleTime, int numOfSamples)
		{
			_sessionId      = sessionId;
			_lastSampleTime = lastSampleTime;
			_numOfSamples   = numOfSamples;
		}
	}

	public static class SessionIndicator
	{
		public Timestamp _sessionStartTime  = null;
		public String    _cmName            = null;
		public int       _absSamples        = -1;
		public int       _diffSamples       = -1;
		public int       _rateSamples       = -1;

		public SessionIndicator(Timestamp sessionStartTime, String cmName, 
				int absSamples, int diffSamples, int rateSamples)
		{
			_sessionStartTime  = sessionStartTime;
			_cmName            = cmName;
			_absSamples        = absSamples;
			_diffSamples       = diffSamples;
			_rateSamples       = rateSamples;
		}
		
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			sb.append("sessionStartTime='").append(_sessionStartTime) .append("'");
			sb.append(", cmName='")        .append(_cmName)           .append("'");
			sb.append(", absSamples=")     .append(_absSamples);
			sb.append(", diffSamples=")    .append(_diffSamples);
			sb.append(", rateSamples=")    .append(_rateSamples);
			return sb.toString();
		}
	}

	public static class CmIndicator
	{
		public Timestamp _sessionStartTime  = null;
		public Timestamp _sessionSampleTime = null;
		public String    _cmName            = null;
		public int       _type              = -1;
		public int       _graphCount        = -1;
		public int       _absRows           = -1;
		public int       _diffRows          = -1;
		public int       _rateRows          = -1;
		public int       _sqlRefreshTime    = -1;
		public int       _guiRefreshTime    = -1;
		public int       _lcRefreshTime     = -1;

		public CmIndicator(Timestamp sessionStartTime, Timestamp sessionSampleTime, 
		                   String cmName, int type, int graphCount, int absRows, int diffRows, int rateRows,
		                   int sqlRefreshTime, int guiRefreshTime, int lcRefreshTime)
		{
			_sessionStartTime  = sessionStartTime;
			_sessionSampleTime = sessionSampleTime;
			_cmName            = cmName;
			_type              = type;
			_graphCount        = graphCount;
			_absRows           = absRows;
			_diffRows          = diffRows;
			_rateRows          = rateRows;
			_sqlRefreshTime    = sqlRefreshTime;
			_guiRefreshTime    = guiRefreshTime;
			_lcRefreshTime     = lcRefreshTime;
		}
		
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			sb.append("sessionStartTime='")   .append(_sessionStartTime) .append("'");
			sb.append(", sessionSampleTime='").append(_sessionSampleTime).append("'");
			sb.append(", cmName='")           .append(_cmName)           .append("'");
			sb.append(", type=")              .append(_type);
			sb.append(", graphCount=")        .append(_graphCount);
			sb.append(", absRows=")           .append(_absRows);
			sb.append(", diffRows=")          .append(_diffRows);
			sb.append(", rateRows=")          .append(_rateRows);
			sb.append(", sqlRefreshTime=")    .append(_sqlRefreshTime);
			sb.append(", guiRefreshTime=")    .append(_guiRefreshTime);
			sb.append(", lcRefreshTime=")     .append(_lcRefreshTime);
			return sb.toString();
		}
	}

	/*---------------------------------------------------
	** BEGIN: notification listener stuff
	**---------------------------------------------------
	*/
	/** List of any subscribers for notifications <code>NotificationListener</code>*/
	private LinkedList<INotificationListener> _notificationListeners = new LinkedList<INotificationListener>();

	/**
	 * Interface that the listener has to implement
	 */
	public interface INotificationListener
	{
		public void setWatermark(String text);
		public void setStatusText(String status);
		public void setSessionList(List<SessionInfo> sessionList);
	}

	/** Add a listener component */
	public void addNotificationListener(INotificationListener comp)
	{
		_notificationListeners.add(comp);
	}

	/** Remove a listener component */
	public void removeNotificationListener(INotificationListener comp)
	{
		_notificationListeners.remove(comp);
	}

	/** calls all listeners using the method <code>setWatermark(String text)</code> */
	private void setWatermark(String text)
	{
		_logger.debug("PersistentReader.setWatermark(text='"+text+"')");
		for (INotificationListener nl : _notificationListeners)
		{
			nl.setWatermark(text);
		}
	}
	/** Sets a watermark to null */
	private void setWatermark()
	{
		setWatermark(null);
	}

	/** calls all listeners using the method <code>setStatusText(String status)</code> */
	private void setStatusText(String status)
	{
		_logger.debug("setStatusText(status='"+status+"')");
		for (INotificationListener nl : _notificationListeners)
		{
			nl.setStatusText(status);
		}
	}
	/*---------------------------------------------------
	** END: notification listener stuff
	**---------------------------------------------------
	*/
}
