/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.pcs;

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

import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import org.apache.log4j.Logger;

import asemon.GetCounters;
import asemon.cm.CountersModel;
import asemon.gui.MainFrame;
import asemon.gui.SummaryPanel;
import asemon.gui.TabularCntrPanel;
import asemon.gui.TrendGraph;
import asemon.utils.StringUtil;
import asemon.utils.TimeUtils;

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
	private HashMap _currentIndicatorMap = new HashMap();

	/** hold information about if current session */
	private HashMap _sessionIndicatorMap = new HashMap();
	
	/** Execution mode, this means if we do the fetch in a background thread or directly in current thread */
	private             int _execMode        = EXEC_MODE_BG;
//	private             int _execMode        = EXEC_MODE_DIRECT;
	public static final int EXEC_MODE_BG     = 0;
	public static final int EXEC_MODE_DIRECT = 1;

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
		CmIndicator cmInd = (CmIndicator) _currentIndicatorMap.get(name);
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
		SessionIndicator ind = (SessionIndicator) _sessionIndicatorMap.get(name);
		return ind;
	}

	public SessionIndicator[] getSessionIndicators()
	{
		SessionIndicator[] si = new SessionIndicator[_sessionIndicatorMap.size()];
		int i=0;
		for (Iterator it = _sessionIndicatorMap.entrySet().iterator(); it.hasNext();)
		{
			SessionIndicator ind = (SessionIndicator) it.next();
			si[i++] = ind;
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
	private BlockingQueue  _cmdQueue   = new LinkedBlockingQueue();
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

		while(_running)
		{
			if (_logger.isDebugEnabled())
				_logger.debug("Thread '"+threadName+"', waiting on queue...");

			try 
			{
				QueueCommand qo = (QueueCommand)_cmdQueue.take();

				// Make sure the container isn't empty.
				if (qo == null)
					continue;
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

	/*---------------------------------------------------
	** END: public execution/access methods
	**---------------------------------------------------
	*/

	
	
	/*---------------------------------------------------
	** BEGIN: 
	**---------------------------------------------------
	*/
	/** containes OfflineCm */
	private LinkedHashMap _offlineCmMap = null;

	private class OfflineCm
	{
		public String     name      = "";
		public boolean    hasAbs    = false;
		public boolean    hasDiff   = false;
		public boolean    hasRate   = false;
		public LinkedList graphList = null;

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
					graphList = new LinkedList();
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
		OfflineCm ocm = (OfflineCm)_offlineCmMap.get(name);
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

		_offlineCmMap = new LinkedHashMap();
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
		LinkedHashMap originOfflineCmMap = (LinkedHashMap)_offlineCmMap.clone();
		LinkedHashMap sortedOfflineCmMap = new LinkedHashMap();

		// Holds 'names' that wasn't found in the TAB, add those at the end
		LinkedList    misses = new LinkedList();

		int tabCount = tabPane.getTabCount();
		for (int t=0; t<tabCount; t++)
		{
			Component comp = tabPane.getComponentAt(t);
			String name = comp.getName();
			
			_logger.debug("sortOfflineCm() Working on tab "+t+": "+name);

			// Get the OfflineCm for this TAB and store it in the new Map
			OfflineCm ocm = (OfflineCm)originOfflineCmMap.get(name);
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
							Map graphs = cm.getTrendGraphs();
							if (graphs != null && graphs.size() > 1)
							{
								LinkedList originGraphList = (LinkedList) ocm.graphList.clone();
								LinkedList sortedGraphList = new LinkedList();

								// Loop how the Graphs was initially added to the CM
								for (Iterator it = graphs.keySet().iterator(); it.hasNext();)
								{
									String     key = (String)it.next();
									//TrendGraph val = (TrendGraph) graphs.get(key);
									
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
			for (Iterator it = misses.iterator(); it.hasNext();)
			{
				String name = (String) it.next();
				OfflineCm ocm = (OfflineCm)originOfflineCmMap.get(name);

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
			_logger.warn("Cant find any CM named '"+cmName+"'.");
			return 0;
		}
		TrendGraph tg = cm.getTrendGraph(graphName);
		if (tg == null)
		{
			_logger.warn("Cant find any TrendGraph named '"+graphName+"', for the CM '"+cmName+"'.");
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

		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		String graphWatermark = sdf.format(startTime) + " - " + sdf.format(endTime);
		MainFrame.setOfflineSamplePeriodText(graphWatermark);

		// Now loop the _offlineCmMap
		for (Iterator it1 = _offlineCmMap.keySet().iterator(); it1.hasNext();) 
		{
			String cmName = (String) it1.next();
			OfflineCm ocm = (OfflineCm) _offlineCmMap.get(cmName);

//System.out.println("loadSessionGraphs(): LOOP, cmName='"+cmName+"', ocm='"+ocm+"'.");
			if (ocm == null)           continue; // why should this happen
			if (ocm.graphList == null) continue;

			for (Iterator it2 = ocm.graphList.iterator(); it2.hasNext();) 
			{
				String graphName = (String) it2.next();
				
				loadEveryXRow = loadSessionGraph(cmName, graphName, sampleId, startTime, endTime, expectedRows);
			}
		}
		String str = "Loading all TrendGraphs took '"+TimeUtils.msToTimeStr("%SS.%ms", System.currentTimeMillis()-xStartTime)+"' seconds.";
		if (loadEveryXRow > 1)
			str += " Loaded every "+(loadEveryXRow-1)+" row, graphs was to big.";
		setStatusText(str);
		setWatermark();
	}

	private void execLoadTimelineSlider(Timestamp sampleId, Timestamp startTime, Timestamp endTime)
	{
		long xStartTime = System.currentTimeMillis();
		setWatermark("Refreshing Timeline slider...");

		//AseMonSessionSamples  SessionStartTime    TIMESTAMP
		//AseMonSessionSamples  SessionSampleTime   TIMESTAMP
		
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

			ArrayList tsList = new ArrayList();
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
		for (Iterator it1 = _offlineCmMap.keySet().iterator(); it1.hasNext();) 
		{
			String cmName = (String) it1.next();
			OfflineCm ocm = (OfflineCm) _offlineCmMap.get(cmName);

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
			List colHead = new ArrayList(cols);

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
			throw new IllegalArgumentException("sampleTs cant be null");

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
	
	private void execLoadSessionIndicators(Timestamp sessionStartTs)
	{
		setWatermark("Loading Session Indicators...");
//		System.out.println("execLoadSessionIndicators(sessionStartTime='"+sessionStartTs+"')");

		long fetchStartTime = System.currentTimeMillis();
		setStatusText("Loading Session indicators for sessionStartTime '"+sessionStartTs+"'.");

		// Reset the Map...
		_sessionIndicatorMap.clear();

		//----------------------------------------
		// TYPICAL look of a graph table
		//----------------------------------------
		//create table MonSessionSampleDetailes
		//(
		//   SessionStartTime  datetime  not null,
		//   CmName            varchar   not null,
		//   absSamples        int           null,
		//   diffSamples       int           null,
		//   rateSamples       int           null
		//)
		String sql = "select * from " + PersistWriterBase.getTableName(PersistWriterBase.SESSION_SAMPLE_SUM, null, true) + " " +
		             "where \"SessionStartTime\" = ? ";

		try
		{
			PreparedStatement pstmnt = _conn.prepareStatement(sql);
//			pstmnt.setTimestamp(1, sampleTs);
			pstmnt.setString(1, sessionStartTs.toString());
			

			_logger.debug("loadSessionIndicators(sampleTs='"+sessionStartTs+"'): "+pstmnt);

			ResultSet rs = pstmnt.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();
			int cols = rsmd.getColumnCount();
			int row  = 0;

			// Get Rows
			while (rs.next())
			{
				Timestamp sessionStartTime  = rs.getTimestamp(1);
				String    cmName            = rs.getString(2);
				int       absSamples        = rs.getInt(3);
				int       diffSamples       = rs.getInt(4);
				int       rateSamples       = rs.getInt(5);

				SessionIndicator sessInd = new SessionIndicator(sessionStartTime, cmName, absSamples, diffSamples, rateSamples);

				// Add it to the indicators map
				_sessionIndicatorMap.put(cmName, sessInd);
				
				row++;
			}
			rs.close();
			pstmnt.close();

//System.out.println("Loaded "+row+" indicators for ts '"+sampleTs+"' , which took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.");
			_logger.debug("Loaded "+row+" session indicators for ts '"+sessionStartTs+"' , which took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.");
			setStatusText("");
		}
		catch (SQLException e)
		{
			_logger.error("Problems loading session indicators for ts '"+sessionStartTs+"'.", e);
		}

		String str = "Loading session indicators took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.";
		_logger.debug(str);
		setStatusText(str);
		setWatermark();
	}

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

				CmIndicator cmInd = new CmIndicator(sessionStartTime, sessionSampleTime, cmName, type, graphCount, absRows, diffRows, rateRows);

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
				for (Iterator it = _currentIndicatorMap.keySet().iterator(); it.hasNext();)
				{
					String key = (String) it.next();
					CmIndicator val = (CmIndicator) _currentIndicatorMap.get(key);
					
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

		// Remove all the rows in the CM, so that new can be added
		// if this is not done, all the old rows will still be visible when displaying it in the JTable
		cm.clearForRead();

		if (cmInd._absRows  > 0) loadSessionCm(cm, CountersModel.DATA_ABS,  sampleTs);
		if (cmInd._diffRows > 0) loadSessionCm(cm, CountersModel.DATA_DIFF, sampleTs);
		if (cmInd._rateRows > 0) loadSessionCm(cm, CountersModel.DATA_RATE, sampleTs);

		cm.setDataInitialized(true);
		cm.fireTableStructureChanged();
		if (cm.getTabPanel() != null)
			cm.getTabPanel().adjustTableColumnWidth();
		
		return cm;
	}

	
	private void execLoadSessions()
	{
		setWatermark("Loading Sessions...");

		long fetchStartTime = System.currentTimeMillis();
		setStatusText("Loading Sessions....");

		// get a LIST of sessions
		List sessionList = getSessionList();
	
		// Loop the sessions and load all samples 
		for (Iterator it = sessionList.iterator(); it.hasNext();) 
		{
			SessionInfo sessionInfo = (SessionInfo) it.next();
			
			// Load all samples for this sampleId
			sessionInfo._sampleList = getSessionSamplesList(sessionInfo._sessionId);
		}

		_logger.debug("loadSessions(COMPLETED) calling listeners");
		for (Iterator it = _notificationListeners.iterator(); it.hasNext();)
		{
			NofificationListener nl = (NofificationListener) it.next();
			nl.setSessionList(sessionList);
		}

		String str = "Loading Sessions took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.";
		_logger.debug(str);
		setStatusText(str);
		setWatermark();
	}

	public List getSessionList()
	{
		setWatermark("Loading Sessions..."); // if the window wasn't visible, set the watermark now
		long fetchStartTime = System.currentTimeMillis();
		setStatusText("Loading Session List....");

		List sessions = new LinkedList();

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

	public List getSessionSamplesList(Timestamp sessionId)
	{
		setWatermark("Loading Sessions...");  // if the window wasn't visible, set the watermark now
		long fetchStartTime = System.currentTimeMillis();
		setStatusText("Loading Session Samples for sessionId '"+sessionId+"'.");

		List list = new LinkedList();

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
	/*---------------------------------------------------
	** END: 
	**---------------------------------------------------
	*/
	public static class SessionInfo
	{
		public Timestamp _sessionId      = null;
		public Timestamp _lastSampleTime = null;
		public int       _numOfSamples   = -1;
		public List      _sampleList     = null;

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

		public CmIndicator(Timestamp sessionStartTime, Timestamp sessionSampleTime, 
		                   String cmName, int type, int graphCount, int absRows, int diffRows, int rateRows)
		{
			_sessionStartTime  = sessionStartTime;
			_sessionSampleTime = sessionSampleTime;
			_cmName            = cmName;
			_type              = type;
			_graphCount        = graphCount;
			_absRows           = absRows;
			_diffRows          = diffRows;
			_rateRows          = rateRows;
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
			return sb.toString();
		}
	}

	/*---------------------------------------------------
	** BEGIN: notification listener stuff
	**---------------------------------------------------
	*/
	/** List of any subscribers for notifications <code>NofificationListener</code>*/
	private LinkedList _notificationListeners = new LinkedList();

	/**
	 * Interface that the listener has to implement
	 */
	public interface NofificationListener
	{
		public void setWatermark(String text);
		public void setStatusText(String status);
		public void setSessionList(List sessionList);
	}

	/** Add a listener component */
	public void addNofificationListener(NofificationListener comp)
	{
		_notificationListeners.add(comp);
	}

	/** Remove a listener component */
	public void removeNofificationListener(NofificationListener comp)
	{
		_notificationListeners.remove(comp);
	}

	/** calls all listeners using the method <code>setWatermark(String text)</code> */
	private void setWatermark(String text)
	{
		_logger.debug("PersistentReader.setWatermark(text='"+text+"')");
		for (Iterator it = _notificationListeners.iterator(); it.hasNext();)
		{
			NofificationListener nl = (NofificationListener) it.next();
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
		for (Iterator it = _notificationListeners.iterator(); it.hasNext();)
		{
			NofificationListener nl = (NofificationListener) it.next();
			nl.setStatusText(status);
		}
	}

	/*---------------------------------------------------
	** END: notification listener stuff
	**---------------------------------------------------
	*/
}
