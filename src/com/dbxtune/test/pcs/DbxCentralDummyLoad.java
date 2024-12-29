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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.test.pcs;

import java.io.File;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import com.dbxtune.test.pcs.H2WriterStat.ConnectionProvider;

/**
 * Simulate a DbxCentral
 * <p>
 * The idea is that we can simulate what a DbxCentral will do over a long time... but in a "short" time... 
 * 
 * <p>
 * Here is the big picture of the "store" 
 * <ul>
 * 		<li>One thread will do all database write/store operations</li>
 * 		<li>One list/queue will be used to "post" storage requests to the above thread</li>
 * </ul>
 * 
 * <p>
 * There will be X number of threads that simulates a "performance collector"<br>
 * Normally a "performance collector" sends it's data via a REST call, which is posted on the "storage queue"<br>
 * But in this dummy implementation, the collectors will do:
 * <ul>
 * 		<li>Build a emulated performance collector object</li>
 * 		<li>Put it on the "storage queue" so it can be persisted.</li>
 * </ul>
 * 
 * @author gorans
 */
public class DbxCentralDummyLoad
implements Runnable
{
	private static Logger _logger = Logger.getLogger("DbxCentralDummyLoad");

	public static final String PROPKEY_JDBC_URL      = "url";
	public static final String DEFAULT_JDBC_URL      = "jdbc:h2:file:${DBXTUNE_SAVE_DIR}/DBXTUNE_CENTRAL_DB_DUMMY;DB_CLOSE_ON_EXIT=FALSE";
//	public static final String DEFAULT_JDBC_URL      = "jdbc:h2:file:${DBXTUNE_SAVE_DIR}/DBXTUNE_CENTRAL_DB_DUMMY";
	
	// also test with X other URL settings:
	//  - REUSE_SPACE=false
	//  - RETENTION_TIME=600_000     // default 45_000
	//  - WRITE_DELAY=30_000         // default 2_000 (if I remember correctly)
	//  - MAX_COMPACT_COUNT=100      // Special undocumented value 100
	//  - AUTO_COMPACT_FILL_RATE=##  // new in 1.4.200 at master autumn 2019, default 90%
	
	public static final String PROPKEY_JDBC_USERNAME = "username";
	public static final String DEFAULT_JDBC_USERNAME = "sa";

	public static final String PROPKEY_JDBC_PASSWORD = "password";
	public static final String DEFAULT_JDBC_PASSWORD = "";
	
	public enum StopType
	{
		STOP_AND_EXIT,
		STOP_WITH_H2_SHUTDOWN_NORMAL_AND_RESTART,
		STOP_WITH_H2_SHUTDOWN_DEFRAG_AND_RESTART
	};

	
	private boolean _debug = false;
//	private Properties _conf;
	
	private Thread _storeThread;
	private boolean _running;
	private boolean _runningStatsThread;
	
	private BlockingQueue<PerfCollectorContainer> _storeQueue = new LinkedBlockingQueue<>();
	private int _warnQueueSizeThresh = 1000;

	private Connection _storeConn;
	private boolean    _inH2ShutdownCode = false;
	private Set<String> _dbSchemas = new HashSet<>(); // Entry if the DB has this schema name (if not create it)
	private Set<String> _dbTables  = new HashSet<>(); // Entry if the DB has this schema.table name (if not create it)


	private List<PerfCollector> _dummyWorkers = new ArrayList<>();

	private final AtomicLong _countContSave = new AtomicLong();
	private final AtomicLong _countRowSave  = new AtomicLong();

	// Write statistics about how many Containers/Rows we have saved 
	private long _lastCountTime     = -1;
	private long _lastCountContSave = -1;
	private long _lastCountRowSave  = -1;

	private long _consumeCount    = 0;
	private long _sumConsumeTime  = 0;
	private long _maxConsumeTime  = 0;

	private Options _options;
	public DbxCentralDummyLoad(Options options)
	{
		_options = options;
		
		if (_options.h2Stats > 0)
		{
			initH2WriterStat();
		}
	}

	private void initH2WriterStat()
	{
		if ( H2WriterStat.hasInstance() )
			return;

		// Create a Connection Provider and a H2WriterStat instance
		ConnectionProvider connProvider = new ConnectionProvider()
		{
			@Override
			public Connection getConnection()
			{
				if (_storeConn != null)
				{
					// If the connection is already closed... the return null
					try 
					{
						if (_storeConn.isClosed())
							return null;
					}
					catch (SQLException ex)
					{
						return null;
					}
				}
				_logger.fine("ConnectionProvider(): _storeConn="+_storeConn);
				return _storeConn;
			}
		};
		H2WriterStat h2WriterStat = new H2WriterStat(connProvider);
//		H2WriterStat h2WriterStat = new H2WriterStat(this);
		H2WriterStat.setInstance(h2WriterStat);

		// Create a BG thread that writes statistics every X second
		Thread h2WiterStatThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				_runningStatsThread = true;
				_logger.info("H2WriterStat - STARTING");

				try
				{
					// initial sleep if we are not yet connected
					if (_storeConn == null)
					{
						try { Thread.sleep(5_000); }
						catch(InterruptedException ignore) {}
					}

					do
					{
						H2WriterStat stat = H2WriterStat.getInstance();
						if (stat != null)
						{
							// H2 Counters
							String h2StatStr = "";
							Connection conn = H2WriterStat.getInstanceConnectionProvider().getConnection();
							if (conn == null)
							{
								h2StatStr = "No valid H2 Connection";
							}
							else
							{
								stat.refreshCounters();
								h2StatStr = stat.getStatString();
								if (h2StatStr != null && h2StatStr.trim().equals(""))
									h2StatStr = "EMPTY H2 STATISTICS ENTRY... maybe first refresh...";
							}

							// DummyLoad... how many Containers and Rows have we done...
							// NO String: First time...
							String dlStatStr = "";
							if (_inH2ShutdownCode)
							{
								dlStatStr += "H2 Storage in Shutdown mode. ";
							}
							if (_lastCountTime > 0 && _consumeCount > 0)
							{
								long timeDiff = System.currentTimeMillis() - _lastCountTime;
//								String timeDiffStr = (new Timestamp(timeDiff)).toString().substring(11);
								String timeDiffStr = msToHms(timeDiff);
								
								long       avgConsumeTime = _consumeCount == 0 ? -1 : _sumConsumeTime/_consumeCount;

								long       cAbs  = _countContSave.get();
								long       cDiff = cAbs - _lastCountContSave;
								BigDecimal cRate = calcRate(timeDiff, cDiff);
								
								long       rAbs  = _countRowSave.get();
								long       rDiff = rAbs - _lastCountRowSave;
								BigDecimal rRate = calcRate(timeDiff, rDiff);
								
								dlStatStr += "DummyLoad{sampleTime="+timeDiffStr
										+ ", Consumer"   + "[cnt="+_consumeCount+", usedMs="+_sumConsumeTime+", maxMs="+_maxConsumeTime+", avgMs="+avgConsumeTime+"]"
										+ ", Containers" + "[abs="+cAbs+", diff="+cDiff+", rate="+cRate+"]"
										+ ", Rows"       + "[abs="+rAbs+", diff="+rDiff+", rate="+rRate+"]"
										+ "}, ";
							}

							_logger.info("H2WriterStat - " + dlStatStr + h2StatStr);

							_lastCountTime     = System.currentTimeMillis();
							_lastCountContSave = _countContSave.get();
							_lastCountRowSave  = _countRowSave .get();
							
							// Reset "consume" counters (not diff calculated, but reset on every refresh)
							_consumeCount      = 0;
							_sumConsumeTime    = 0;
							_maxConsumeTime    = 0;
							
							try { Thread.sleep(_options.h2Stats * 1000); }
							catch(InterruptedException ignore) {}
						}
					} while(_runningStatsThread);
				}
				catch(RuntimeException ex)
				{
					ex.printStackTrace();
				}
				
				_logger.info("H2WriterStat - STOPPED");
			}
		});
		h2WiterStatThread.setDaemon(true);
		h2WiterStatThread.setName("H2WiterStat");
		h2WiterStatThread.start();
	}
	private BigDecimal calcRate(long timeDiffInMs, long diffVal)
	{
		BigDecimal rate = null;
		try 
		{
			rate = new BigDecimal( ((diffVal*1.0) / (timeDiffInMs*1.0)) * 1000 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
		} 
		catch (NumberFormatException ex) 
		{
			_logger.warning("Calculating RATE value had problems. diffVal="+diffVal+" (divided by) timeDiffInMs="+timeDiffInMs+". Setting rate to 0.0  Caught: " + ex);
			rate = new BigDecimal(0.0);
		}
		return rate;
	}
	private String msToHms(long ts)
	{
		String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(ts),
			    TimeUnit.MILLISECONDS.toMinutes(ts) % TimeUnit.HOURS.toMinutes(1),
			    TimeUnit.MILLISECONDS.toSeconds(ts) % TimeUnit.MINUTES.toSeconds(1));
		return hms;
	}

	public boolean openDatabase() 
	{
//		String url      = _conf.getProperty(PROPKEY_JDBC_URL,      DEFAULT_JDBC_URL);
//		String username = _conf.getProperty(PROPKEY_JDBC_USERNAME, DEFAULT_JDBC_USERNAME);
//		String password = _conf.getProperty(PROPKEY_JDBC_PASSWORD, DEFAULT_JDBC_PASSWORD);
		String url      = _options.url;
		String username = _options.username;
		String password = _options.password;

		
		long startTime = System.currentTimeMillis();
		if (_debug)
			System.out.println("DEBUG: Connecting to URL '" + url + "'.");

		try
		{
			Connection conn = DriverManager.getConnection(url, username, password);

			long execTime = System.currentTimeMillis() - startTime;
			_logger.info("Connect succeeded. ConnectTimeMs=" + execTime + ". url='" + url + "'.");
	
			pupulateSchemaTab(conn);
			createDbxCentralCommonTables(conn);
			
			_storeConn = conn;
			return true;
		}
		catch(SQLException ex)
		{
			_logger.severe("ERROR: problems connectiong to URL '" + url + "'. Caught: " + ex);
			return false;
		}
	}

	public void closeDatabase()
	{
		if (_storeConn == null)
			return;
		
		try 
		{ 
			_storeConn.close(); 
			_storeConn = null; 
		}
		catch(SQLException ignore) {}
		
		if ( H2WriterStat.hasInstance() )
		{
			H2WriterStat.getInstance().resetCounters();
		}
	}

	public void shutdownNormal()
	{
		shutdownCmd("SHUTDOWN");
	}
	public void shutdownDefrag()
	{
		shutdownCmd("SHUTDOWN DEFRAG");
	}
	public void shutdownCmd(String sql)
	{
		if (_storeConn == null)
			return;
		
		try (Statement stmnt = _storeConn.createStatement())
		{
			_inH2ShutdownCode = true;
			
			_logger.info("Sending '" + sql + "' to H2.");

			File dbFile = _options._h2DbFile;
			long dbFileSizeBefore = -1;
			if (dbFile != null && dbFile.exists())
				dbFileSizeBefore = dbFile.length();
				
			long startTime = System.currentTimeMillis();
			
			stmnt.executeUpdate(sql);

			long execTime = System.currentTimeMillis() - startTime;
//			_logger.info("" + sql + " - EXECUTION TIME -------------------- TimeMs=" + execTime + ", as HH:mm:ss.SSS [" + ((new Timestamp(execTime)).toString().substring(11)) + "].");
			_logger.info("" + sql + " - EXECUTION TIME -------------------- TimeMs=" + execTime + ", as HH:mm:ss [" + msToHms(execTime) + "].");

			
			if (dbFile != null && dbFile.exists())
			{
				long dbFileSizeAfter = dbFile.length();
				long sizeDiff = dbFileSizeAfter - dbFileSizeBefore;

				_logger.info("Shutdown H2 database file size info, after '"+sql+"'. " 
						+ "DiffMb="     + String.format("%.1f", (sizeDiff        /1024.0/1024.0))
						+ ", BeforeMb=" + String.format("%.1f", (dbFileSizeBefore/1024.0/1024.0))
						+ ", AfterMb="  + String.format("%.1f", (dbFileSizeAfter /1024.0/1024.0))
						+ ", Filename='"+dbFile.getAbsolutePath()
						+ "'.");
			}

			closeDatabase();
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
		}
		finally
		{
			_inH2ShutdownCode = false;			
		}
	}
	
	private void pupulateSchemaTab(Connection conn) 
	{
		// Get schemas
		try (ResultSet rs = conn.getMetaData().getSchemas())
		{
			while (rs.next())
				_dbSchemas.add(rs.getString("TABLE_SCHEM")); 			
		}
		catch(SQLException ex)
		{
			_logger.severe("Problems getting SCHEMA's, conn.getMetaData().getSchemas().");
			ex.printStackTrace();
		}

		// Get tables
//		try (ResultSet rs = conn.getMetaData().getTables(null, null, "%", new String[] {"TABLE"}))
		try (ResultSet rs = conn.getMetaData().getTables(null, null, "%", new String[] {"TABLE", "BASE TABLE"}))
		{
			while (rs.next())
			{
				String schemaName = rs.getString("TABLE_SCHEM");
				String tableName  = rs.getString("TABLE_NAME");

				String fullTableName = schemaName + "." + tableName;
				if ("PUBLIC".equalsIgnoreCase(schemaName))
					fullTableName = tableName;
				
				_dbTables.add(fullTableName);
			}
		}
		catch(SQLException ex)
		{
			_logger.severe("Problems getting TABLES, conn.getMetaData().getTables(null, null, \"%\", null).");
			ex.printStackTrace();
		}
		
		if (_debug)
		{
			System.out.println("SCHEMA EXISTS:" + _dbSchemas);
			System.out.println("TABLES EXISTS:" + _dbTables);
		}
	}

	private void createDbxCentralCommonTables(Connection conn) 
	throws SQLException
	{
		String sql;
		
		if ( ! _dbTables.contains("DbxCentralSessions") )
		{
			_logger.info("Creating common table 'DbxCentralSessions'.");
			sql = "create table #DbxCentralSessions# \n" +
					"( \n" +
					"     #SessionStartTime#        datetime          NOT NULL \n" +
					"    ,#Status#                  int               NOT NULL \n" +
					"    ,#ServerName#              varchar(30)       NOT NULL \n" +
					"    ,#OnHostname#              varchar(30)       NOT NULL \n" +
					"    ,#ProductString#           varchar(30)       NOT NULL \n" +
					"    ,#VersionString#           varchar(30)       NOT NULL \n" +
					"    ,#BuildString#             varchar(30)       NOT NULL \n" +
					"    ,#CollectorHostname#       varchar(30)       NOT NULL \n" +
					"    ,#CollectorSampleInterval# int               NOT NULL \n" +
					"    ,#CollectorCurrentUrl#     varchar(80)           NULL \n" +
					"    ,#CollectorInfoFile#       varchar(256)          NULL \n" +
					"    ,#NumOfSamples#            int               NOT NULL \n" +
					"    ,#LastSampleTime#          datetime              NULL \n" +
					"    ,PRIMARY KEY (#ServerName#, #SessionStartTime#) \n" +
					") \n";
			sql = sql.replace('#', '"');

			try(Statement stmnt = conn.createStatement())
			{
				stmnt.executeUpdate(sql);
			}

			// Create Indexes -- if we want any extra indexes
			sql = "create index #DbxCentralSessions_ix1# on #DbxCentralSessions#(#ServerName#, #LastSampleTime#)";
			sql = sql.replace('#', '"');
			try(Statement stmnt = conn.createStatement())
			{
				stmnt.executeUpdate(sql);
			}
		}

//		sql = "create table #schema#.#DbxSessionSamples# \n" +
//				"( \n" +
//				"    #SessionStartTime#    datetime    NOT NULL \n" +
//				"   ,#SessionSampleTime#   datetime    NOT NULL \n" +
//				"   ,PRIMARY KEY (#SessionSampleTime#, #SessionStartTime# \n" +
//				") \n";
	}
	/**
	 */
	public void startSession(String sessionName, PerfCollectorContainer pcc)
	throws Exception
	{
		// Check/Create the SCHEMA 
		String schemaName = pcc._name;
		if ( ! _dbSchemas.contains(schemaName) )
		{
			// Create the schema if it dosn't exists
			String sql = "CREATE SCHEMA #" + schemaName + "#";
			sql = sql.replace('#', '"');
			try (Statement stmnt = _storeConn.createStatement())
			{
				stmnt.executeUpdate(sql);
				_dbSchemas.add(schemaName);
			}
			catch(SQLException ex)
			{
				_logger.severe("When creating schema '" + schemaName + "'. skipping and continuing. SQL='" + sql + "', Caught: " + ex);
			}
		}

		// Check/insert record from "DbxCentralSessions"
		Connection conn = _storeConn;
		boolean sessionExists = false;
		Timestamp sessionStartTime = pcc._sessionStartTime;
		String    serverName       = pcc._name;

		String sql = " select * from #DbxCentralSessions# where #SessionStartTime# = '" + sessionStartTime + "' and #ServerName# = '" + serverName + "'";
		sql = sql.replace('#', '"');

		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
				sessionExists = true;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
		}
		
		// Now insert the record to CENTRAL_SESSIONS if it do NOT exist
		if ( ! sessionExists )
		{
			// Normally all columns will be filled with "relevant" information... but in this test.. just dummy values
			sql = "insert into #DbxCentralSessions#(#SessionStartTime#, #Status#, #ServerName#, #OnHostname#, #ProductString#, #VersionString#, #BuildString#, #CollectorHostname#, #CollectorSampleInterval#, #CollectorCurrentUrl#, #CollectorInfoFile#, #NumOfSamples#, #LastSampleTime#)"
			    + " values('"                       +sessionStartTime+"', 0, '"   +serverName+"', 'dummyHost', 'dummyTune',    'dummy',         'dummy',       'localhost',         -1,                        'dummyUrl',            'dummyFile',         0,              '2001-01-01 00:00')";
			sql = sql.replace('#', '"');

			try(Statement stmnt = conn.createStatement())
			{
				stmnt.executeUpdate(sql);
			}
			catch(SQLException ex)
			{
				ex.printStackTrace();
			}
		}
	
		// MARK the session as started
		setSessionStarted(sessionName, true);
		_logger.info("Marking session '" + sessionName + "' as STARTED.");
	}

	public void updateDbxCentralSessions(Connection conn, String serverName, Timestamp sessionStartTime, Timestamp sessionSampleTime)
	throws SQLException
	{
		char q = '"';
		
		StringBuffer sb = new StringBuffer();
		sb.append(" update ").append(q).append("DbxCentralSessions").append(q).append("\n");
		sb.append("    set  ").append(q).append("NumOfSamples")   .append(q).append(" = ").append(q).append("NumOfSamples").append(q).append(" + 1").append("\n");
		sb.append("        ,").append(q).append("LastSampleTime") .append(q).append(" = '").append(sessionSampleTime).append("'").append("\n");
		sb.append("  where ").append(q).append("SessionStartTime").append(q).append(" = '").append(sessionStartTime) .append("'").append("\n");
		sb.append("    and ").append(q).append("ServerName")      .append(q).append(" = '").append(serverName)       .append("'").append("\n");

		String sql = sb.toString();

		try(Statement stmnt = conn.createStatement())
		{
			stmnt.executeUpdate(sql);
		}
	}

	/**
	 * Start the Dummy Load
	 */
	public void start()
	{
		_storeThread = new Thread(this);
		_storeThread.setName("DbxCentral - Dummy Load");
		
		_storeThread.start();
	}
	public void stop(boolean waitForQueueToBeEmpty)
	{
		if ( ! waitForQueueToBeEmpty )
		{
			_storeQueue.clear();
			_running = false;
			return;
		}

		while ( ! _storeQueue.isEmpty() )
		{
			int sleepTime = 1000;

			_logger.info("Stop DbxCentral - REQUEST, but queue still has " + _storeQueue.size() + " entries. Sleeping for " + sleepTime + "ms to let queue drain.");

			try { Thread.sleep(sleepTime); }
			catch(InterruptedException ignore) {}
		}
		
		// Mark it internally as "not running" (This needs to be done AFTER the queue is drained)
		_running = false;
		
		// Wait for the store thread to end
		while ( _storeThread.isAlive() )
		{
			int sleepTime = 500;

			_logger.info("Stop DbxCentral - Waiting for the 'Store-thread to end'. Sleeping for " + sleepTime + "ms to let it stop.");

			try { Thread.sleep(sleepTime); }
			catch(InterruptedException ignore) {}
		}
		
	}

	public void startDummyCollectors()
	{
		for (PerfCollector collector : _dummyWorkers)
		{
			collector.start();
		}
	}
	
	/**
	 * Wait for all Collecor workers to "stop"
	 * @return 0=normal-stop, 1=shutdown-defrag-and-restart
	 */
	public StopType waitForCollectorsToStop()
	{
		// Should we do this in a loop.. until _dummyWorkers.isEmpty()
		for (PerfCollector collector : _dummyWorkers)
		{
			try { collector._thread.join(); }
			catch (InterruptedException ignore) {}
		}

		StopType stopType = StopType.STOP_AND_EXIT;
		int stopTypeInt = StopType.STOP_AND_EXIT.ordinal();
		// get the MAX value of the ordinal from ANY of the workers to decide STOP REASON
		for (PerfCollector collector : _dummyWorkers)
		{
			stopTypeInt = Math.max(stopTypeInt, collector._stopReason.ordinal());
		}
		if (stopTypeInt == StopType.STOP_AND_EXIT                           .ordinal()) stopType = StopType.STOP_AND_EXIT;
		if (stopTypeInt == StopType.STOP_WITH_H2_SHUTDOWN_NORMAL_AND_RESTART.ordinal()) stopType = StopType.STOP_WITH_H2_SHUTDOWN_NORMAL_AND_RESTART;
		if (stopTypeInt == StopType.STOP_WITH_H2_SHUTDOWN_DEFRAG_AND_RESTART.ordinal()) stopType = StopType.STOP_WITH_H2_SHUTDOWN_DEFRAG_AND_RESTART;
		
		return stopType;
	}

	/**
	 * Add to STORE QUEUE
	 * 
	 * @param pcd
	 */
	public void add(PerfCollectorContainer pcc)
	{		
		if (_storeQueue.size() > _warnQueueSizeThresh)
			System.out.println("WARNING: Central Storge Queue Size is above the threshold... Database Storage might not keep up... size=" + _storeQueue.size() + ", threshold=" + _warnQueueSizeThresh);

		_storeQueue.add(pcc);
	}

	/**
	 * Add a Worker implementation
	 * 
	 * @param pcd
	 */
	public void addDummyCollecto(PerfCollector worker)
	{
		_dummyWorkers.add(worker);
	}

	/**
	 * MAIN THREAD
	 */
	@Override
	public void run()
	{
		_running = true;
		_logger.info("Starting DbxCentral - Dummy Load");

		while (_running)
		{
			PerfCollectorContainer pcc = null;
			try
			{
				// WAIT on the queue for DATA to process
//				pcc = _storeQueue.take();
				pcc = _storeQueue.poll(500, TimeUnit.MILLISECONDS);

				// if we are about to STOP the service
				if ( ! _running )
				{
					_logger.info("The service is about to stop, discarding a consume(DbxTuneSample) queue entry.");
					continue;
				}

				// "poll" did a timeout...
				if (pcc == null)
					continue;
				
				if (_storeConn == null)
				{
					_running = false;
					continue; // or break;
				}

				// SAVE THE DATA
				long startTime = System.currentTimeMillis();
				consume( pcc );
				long thisConsumeTime = System.currentTimeMillis() - startTime;
				
				_consumeCount++;
				_sumConsumeTime += thisConsumeTime;
				_maxConsumeTime = Math.max(thisConsumeTime, _maxConsumeTime);

				if (thisConsumeTime > _options.infoConsumeAboveMs)
				{
					// FIXME: probably write Avg/MAX consume time...
					_logger.info(String.format("Consumed container for: %-30s, sampleTime: %s. queueSize=%d, cCnt=%d, rCnt=%d, consumeTimeMs=%d",
						pcc._name, pcc._ts, _storeQueue.size(), _countContSave.get(), _countRowSave.get(), thisConsumeTime));
				}
			}
			catch(Throwable t)
			{
				_logger.severe("(continuing with next)... Caught Exception: " + t);
				t.printStackTrace();
			}
		}

		_logger.info("DbxCentral - Dummy Load... has been STOPPED.");
	}

	private void consume(PerfCollectorContainer pcc)
	throws Exception
	{
		String serverName = pcc._name;
		if ( ! isSessionStarted(serverName) )
		{
			setSessionStartTime(serverName, pcc._sessionStartTime);

			startSession(serverName, pcc);
			// note: the above pw.startSession() must call: pw.setSessionStarted(true);
			// otherwise we will run this everytime
		}

		if (_storeConn.isClosed())
		{
			_logger.info("DbxCentral - DB is CLOSED.... returning from consume()");
			return;
		}
		
		// Final block at the end to close the transaction
		try
		{
			// START a transaction
			// This will lower number of IO's to the transaction log (if the log is synchronous committed on every transaction, which is done in most databases)
			if (_storeConn.getAutoCommit() == true)
				_storeConn.setAutoCommit(false);
			
			// Maintain metadata
			updateDbxCentralSessions(_storeConn, serverName, pcc._sessionStartTime, pcc._ts);
			
			// store the individual Collector DATA 
			for (PerfCollectorData pcd : pcc._pcdList)
			{
				// CHECK if tables exists, if NOT Create 
				String schemaName = serverName;
				String tableName = pcd._name;

				String fullTableName = schemaName + "." + tableName;
				if ( ! _dbTables.contains(fullTableName) )
				{
					// Create the schema if it dosn't exists
					String sql;
					String sqlTable = pcd.getCreateTable(); // "create table " + fullTableName + "()";
					String sqlIndex = pcd.getCreateIndex(); // "create index xxx on " + fullTableName + "(yyyy)";

					// Create TABLE
					sql = sqlTable;
					try (Statement stmnt = _storeConn.createStatement())
					{
						stmnt.executeUpdate(sql);

						_dbTables.add(fullTableName);
					}
					catch(SQLException ex)
					{
						_logger.severe("When creating table '" + fullTableName + "'. skipping and continuing. SQL='" + sql + "', Caught: " + ex);
					}

					// Create INDEX
					sql = sqlIndex;
					try (Statement stmnt = _storeConn.createStatement())
					{
						stmnt.executeUpdate(sql);
					}
					catch(SQLException ex)
					{
						_logger.severe("When creating index on '" + fullTableName + "'. skipping and continuing. SQL='" + sql + "', Caught: " + ex);
					}
				}

				
				if ( ! _dbTables.contains(fullTableName) )
				{
					System.out.println("WARNING: Skipping storing data for table '" + fullTableName + "'. Because the table didn't exist.. and we could NOT create it...");
				}
				else
				{
					// SAVE/PERSIST THE DATA
					boolean usePreparedStatementInsert = false;
					if (usePreparedStatementInsert)
					{
						String sql = "";
						try
						{
							sql = pcd.doPreparedInsertStatement(_storeConn);
						}
						catch (SQLException ex)
						{
							_logger.severe("When inserting into table '" + fullTableName + "'. skipping and continuing. SQL='" + sql + "', Caught: " + ex);
						}
					}
					else
					{
						String sql = pcd.getInsertStatement();

						try (Statement stmnt = _storeConn.createStatement())
						{
							stmnt.executeUpdate(sql);
						}
						catch (SQLException ex)
						{
							_logger.severe("When inserting into table '" + fullTableName + "'. skipping and continuing. SQL='" + sql + "', Caught: " + ex);
						}
					}
				}
			}

			// maintain some counters...
			_countContSave.incrementAndGet();
			_countRowSave.addAndGet(pcc._pcdList.size());
		}
		finally
		{
			try 
			{
				if (_storeConn.isValid(10))
					_storeConn.setAutoCommit(true); 
			}
			catch (SQLException e2) 
			{ 
				_logger.severe("Problems when setting AutoCommit to true. Caught:" + e2); 
			}
		}
	}








	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------
	/** Dummy object to simulate collecting data by any Performance Collector */
	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------
	private static class PerfCollector
	implements Runnable
	{
		private DbxCentralDummyLoad _dbxCentral;
		private String    _name;
		private int       _chartCount = 120;
		private int       _startTime;
		private Timestamp _sessionStartTime = null;
		private int       _waitTimeMs = 30 * 1000;
		private boolean    _stopOnHalfDay;
		private boolean   _stopOnNewDay;
		private boolean   _running;
		private Thread    _thread;
		private StopType _stopReason = StopType.STOP_AND_EXIT;

		public PerfCollector(DbxCentralDummyLoad dbxCentral, String name, int chartCount, int startTime, int waitTimeMs, boolean stopOnHalfDay, boolean stopOnNewDay)
		{
			_dbxCentral    = dbxCentral;
			_name          = name;
			_chartCount    = chartCount;
			_startTime     = startTime;
			_waitTimeMs    = waitTimeMs;
			_stopOnHalfDay = stopOnHalfDay;
			_stopOnNewDay  = stopOnNewDay;
		}
	
		public Timestamp getOldestSavedSample()
		{
			Timestamp ts = null;
			
			String fullTableName = "#DbxCentralSessions#";
			String sql = "select max(#LastSampleTime#) from " + fullTableName + " where #ServerName# = '" + _name + "'";
			sql = sql.replace('#', '"');

			try (Statement stmnt = _dbxCentral._storeConn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
					ts = rs.getTimestamp(1);
			}
			catch (SQLException ex) 
			{
				_logger.severe("When getting max SessionSampleTime from '" + fullTableName + "'. skipping and continuing. SQL='" + sql + "', Caught: " + ex);
			}
			return ts;
		}
		
		public void start()
		{
			_thread = new Thread(this);
			_thread.setName("PerfCollector - srvName="+_name);
			_thread.setDaemon(true);
			
			_thread.start();
		}
		
		public void stop()
		{
			_running = false;
		}

		@Override
		public void run()
		{
			_running = true;
//			_logger.info("Starting Performance Collector '" + _name + "'.");

//			int chartCount = 120;
//			String propName = _name + ".chart.count";
//			String chartCountStr = _dbxCentral._conf.getProperty(propName, chartCount+"");
//			
//			try { chartCount = Integer.parseInt(chartCountStr); } catch(NumberFormatException ex) { System.out.println("WARNING: Problems parsing config '"+propName+"' with value '"+chartCountStr+"'. Caught: "+ex);}
//			System.out.println("CONFIG: "+propName+" = " + chartCount);

			// This will be set to the "start" time where we generate data
			Timestamp nextTs  = null;
			
			if (_startTime == 0)
			{
				nextTs = new Timestamp(System.currentTimeMillis());
			}
			else
			{
				// Possibly parse the "_startTime" to get an exact start time...
				
				Timestamp firstTs = getOldestSavedSample();
				
				if (firstTs == null)
				{
					long day = (1000 * 60 * 60 * 24); // 24 hours in milliseconds
					long oneYear = day * _startTime;
					firstTs = new Timestamp(System.currentTimeMillis() - oneYear);
				}
				nextTs = new Timestamp(firstTs.getTime() + _waitTimeMs);
			}
			
			if (_sessionStartTime == null)
				_sessionStartTime = new Timestamp(nextTs.getTime());

			_logger.info("Starting Performance Collector: " + String.format("%-30s", _name) + " with startDate='" + nextTs + "', chartCount=" + _chartCount + ", waitTimeMs=" + _waitTimeMs);
			
			while (_running)
			{
				try
				{
					// Create a Container and generate DUMMY Performance Counter data
					PerfCollectorContainer pcc = new PerfCollectorContainer(_name, _sessionStartTime, _chartCount);
					pcc.generate(nextTs);
					
					// add the entry to the central queue
					_dbxCentral.add(pcc);

					// Sleep a while
					// - in "generate old data", sleep for a short period
					// - if we are at "current time", then sleep the full "sleep time" (30 sec) to simulate a "steady state" DbxCollector
					long timeDiffMs = System.currentTimeMillis() - nextTs.getTime();
					if (timeDiffMs > _waitTimeMs)
					{
						// If the storage queue has a bunch of rows to persist... wait a while...
						// NOTE: It might be better to do this in '_dbxCentral.add(pcc)'...
						while (_dbxCentral._storeQueue.size() > 100)
							Thread.sleep(500);
					}
					else
					{
						Thread.sleep(_waitTimeMs);
					}

					// Increment the nextTs
					Timestamp tmpTs = nextTs;
					nextTs = new Timestamp(nextTs.getTime() + _waitTimeMs);

					// Do: SHUTDOWN DEFRAG when 'YYYY-MM-DD' changes
					boolean actionTaken = false;
					if (_stopOnNewDay && !actionTaken)
					{
						String tmpTsDay      = tmpTs .toString().substring(0, "YYYY-MM-DD".length());
						String nextTsDay     = nextTs.toString().substring(0, "YYYY-MM-DD".length());
//						String tmpTsDay  = tmpTs .toString().substring(0, "YYYY-MM-DD hh".length());  // USED for test
//						String nextTsDay = nextTs.toString().substring(0, "YYYY-MM-DD hh".length());  // USED for test

						if ( ! tmpTsDay.equals(nextTsDay) )
						{
							actionTaken = true;
							_running = false;
							_stopReason = StopType.STOP_WITH_H2_SHUTDOWN_DEFRAG_AND_RESTART;
							_logger.info("STOPPING Performance Collector '" + _name + "' due to NEW-DAY. thisDay='" + tmpTsDay + "', nextDay='" + nextTsDay + "'.");
						}
					}

					// Do: SHUTDOWN when 'YYYY-MM-DD h' changes
					if (_stopOnHalfDay && !actionTaken)
					{
						String tmpTsHalfDay  = tmpTs .toString().substring(0, "YYYY-MM-DD h".length());
						String nextTsHalfDay = nextTs.toString().substring(0, "YYYY-MM-DD h".length());
//						String tmpTsHalfDay  = tmpTs .toString().substring(0, "YYYY-MM-DD hh".length());  // USED for test
//						String nextTsHalfDay = nextTs.toString().substring(0, "YYYY-MM-DD hh".length());  // USED for test
						
						if ( ! tmpTsHalfDay.equals(nextTsHalfDay) )
						{
							actionTaken = true;
							_running = false;
							_stopReason = StopType.STOP_WITH_H2_SHUTDOWN_NORMAL_AND_RESTART;
							_logger.info("STOPPING Performance Collector '" + _name + "' due to NEW-HALF-DAY. thisHalfDay='" + tmpTsHalfDay + "', nextHalfDay='" + nextTsHalfDay + "'.");
						}
					}
				}
				catch(Throwable t)
				{
					t.printStackTrace();
				}
			}

			// Remove "me" from the Workers list
//			_dbxCentral._dummyWorkers.remove(this);

			_logger.info("Performance Collector '" + _name + "' has been STOPPED.");
		}
	}


	
	
	
	
	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------
	/** This will hold data for each "server name" we collect Performance Counters from */
	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------
	private static class PerfCollectorContainer
	{
		private Timestamp _sessionStartTime;
		private String   _name;
		private Timestamp _ts;
		private int       _size; // number of graphs/charts for this container, each chart will contain X number of DataPoints (with between 1 and 60 pints, algorithm: chartNum/2)
		
		List<PerfCollectorData> _pcdList = new ArrayList<>();
		
		public PerfCollectorContainer(String name, Timestamp sessionStartTime, int chartCount)
		{
			_name             = name;
			_sessionStartTime = sessionStartTime;
			_size             = chartCount;
		}
		
		public void generate(Timestamp ts)
		{
			_ts = (ts != null) ? ts : new Timestamp(System.currentTimeMillis());
			
			for (int c=0; c<_size; c++)
			{
				PerfCollectorData pcd = new PerfCollectorData(this, "CmDummy_chart_"+c, c);
				pcd.generate();
				
				_pcdList.add(pcd);
			}
		}
	}



	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------
	/** Dummy object to simulate collected data by any Performance Collector */
	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------
	private static class PerfCollectorData
	{
		private PerfCollectorContainer _cont;
		private String    _name;
		private int       _id;
		private int       _dataSize;
		private Double[]  _dataArray;

		char _qic = '"';

		public PerfCollectorData(PerfCollectorContainer cont, String name, int id)
		{
			int dataPointCount = Math.max(1, id / 2);

			_cont     = cont;
			_name     = name;
			_id       = id;
			_dataSize = dataPointCount;
			_dataArray = new Double[_dataSize];
		}
		
		public String getInsertStatement()
		{
			StringBuilder sb = new StringBuilder();

			Timestamp SessionStartTime  = _cont._sessionStartTime;
			Timestamp SessionSampleTime = _cont._ts;
			Timestamp CmSampleTime      = new Timestamp( SessionSampleTime.getTime() + _id); // Simulate that the CM's sample time is slightly *after* the SessionSampleTime
			
			// SQL: INSERT INTO "SCHEMA"."TABNAME" values('ts', 'ts', 'ts', ##, ##, ##...)
			sb.append("INSERT INTO ").append(_qic).append(_cont._name).append(_qic).append(".").append(_qic).append(_name).append(_qic).append(" values");
			sb.append("('" ) .append(SessionStartTime) .append("'"); // col: SessionStartTime
			sb.append(", '" ).append(SessionSampleTime).append("'"); // col: SessionSampleTime
			sb.append(", '" ).append(CmSampleTime)     .append("'"); // col: CmSampleTime
			for (int c=0; c<_dataArray.length; c++)
				sb.append(", ").append(_dataArray[c]); // ColumnData
			sb.append(")").append("\n");

			return sb.toString();
		}

		public String doPreparedInsertStatement(Connection conn)
		throws SQLException
		{
			StringBuilder sb = new StringBuilder();

			Timestamp SessionStartTime  = _cont._sessionStartTime;
			Timestamp SessionSampleTime = _cont._ts;
			Timestamp CmSampleTime      = new Timestamp( SessionSampleTime.getTime() + _id); // Simulate that the CM's sample time is slightly *after* the SessionSampleTime
			
			// SQL: INSERT INTO "SCHEMA"."TABNAME" values('ts', 'ts', 'ts', ##, ##, ##...)
			sb.append("INSERT INTO ").append(_qic).append(_cont._name).append(_qic).append(".").append(_qic).append(_name).append(_qic).append(" values(?, ?, ?");
			for (int c=0; c<_dataArray.length; c++)
				sb.append(", ?");
			sb.append(")").append("\n");
			
			String sql = sb.toString();
			try (PreparedStatement pstmnt = conn.prepareStatement(sql))
			{
				pstmnt.setTimestamp(1, SessionStartTime);
				pstmnt.setTimestamp(2, SessionSampleTime);
				pstmnt.setTimestamp(3, CmSampleTime);

				for (int c=0; c<_dataArray.length; c++)
					pstmnt.setDouble((c+4), _dataArray[c]); // ColumnData
				
				pstmnt.execute();
			}
			
			return sql;
		}

		public String getCreateTable()
		{
			String fullTableName = "#" + _cont._name + "#.#" + _name + "#";

			String sql = "create table " + fullTableName + "( \n";
			sql += "    #SessionStartTime#      datetime   NOT NULL \n";
			sql += "   ,#SessionSampleTime#     datetime   NOT NULL \n";
			sql += "   ,#CmSampleTime#          datetime   NOT NULL \n";
			
			for (int c=0; c<_dataArray.length; c++)
			{
				sql += "   ,#columnName-"+c+"#   numeric(16,2)   NULL \n";
			}
			sql += ") \n";
			
			return sql.replace('#', _qic);
		}

		public String getCreateIndex()
		{
			String fullTableName = "#" + _cont._name + "#.#" + _name + "#";

			String sql = "create index #" + _name + "_ix1# on " + fullTableName + "(#SessionSampleTime#)";
			
			return sql.replace('#', _qic);
		}

		public void generate()
		{
			for (int c=0; c<_dataSize; c++)
			{
				_dataArray[c] = ThreadLocalRandom.current().nextDouble(0, Integer.MAX_VALUE);
			}
		}
	}

	
	private HashMap<String, SessionInfo> _sessionInfoMap = new HashMap<>();
	private static class SessionInfo
	{
    	/** Determines if a session is started or not, or needs initialization */
    	private boolean _isSessionStarted = false;
    
    	/** Session start time is maintained from PersistCounterHandler */
    	private Timestamp _sessionStartTime = null;
	}
	
	private SessionInfo getSessionInfo(String sessionName)
	{
		// Create the Map if it do not exist
		if (_sessionInfoMap == null)
			_sessionInfoMap = new HashMap<>();
		
		// Get the session map, if it dosnt exists: create it
		SessionInfo si = _sessionInfoMap.get(sessionName);
		if (si == null)
		{
			si = new SessionInfo();
			_sessionInfoMap.put(sessionName, si);
		}

		return si;
	}

//	@Override
	public boolean isSessionStarted(String sessionName)
	{
		return getSessionInfo(sessionName)._isSessionStarted;
	}
	
//	@Override
	public void setSessionStarted(String sessionName, boolean isSessionStarted)
	{
		getSessionInfo(sessionName)._isSessionStarted = isSessionStarted;
	}

	/**
	 * Used by PersistCounterHandler to set a new session start time
	 */
//	@Override
	public void setSessionStartTime(String sessionName, Timestamp sessionStartTime)
	{
		getSessionInfo(sessionName)._sessionStartTime  = sessionStartTime;
	}


	/**
	 * Used by PersistCounterHandler to get the session start time
	 */
//	@Override
	public Timestamp getSessionStartTime(String sessionName)
	{
		return getSessionInfo(sessionName)._sessionStartTime;
	}	
	

	
	
	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------
	/** Configuration object, which also does CmdlLine parsing */
	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------
	private static class Options
	{
		String  url      = DEFAULT_JDBC_URL;
		String  username = DEFAULT_JDBC_USERNAME;
		String  password = DEFAULT_JDBC_PASSWORD;

		File    _h2DbFile = null; 
		
		int     chartCount         = 120;
		int     startTime          = 365;  // -1 = start NOW, 0 = continue generate where we left off (if we havn't got any records inserted... start 365 days "back in time")
		int     sleepTime          = 30;
		boolean defragOnNewDay     = true;
		boolean shutdownOnHalfDay  = true;
		int     infoConsumeAboveMs = 0;
		String  serverNames = "PROD_1A_ASE,"   + "PROD_1B_ASE,"   + "DEV_ASE,"   + "SYS_ASE,"   + "INT_ASE,"   + "STAGE_ASE, "
		                    + "prod-1a-pg,"    + "prod-1b-pg,"    + "dev-pg,"    + "sys-pg,"    + "int-pg,"    + "stage-pg, "
		                    + "prod-1a-mssql," + "prod-1b-mssql," + "dev-mssql," + "sys-mssql," + "int-mssql," + "stage-mssql";

		int     h2Stats = 300;

		public void printOptions(PrintStream ps)
		{
			ps.println("");
			ps.println("#################################################################");
			ps.println("  url                = '" + url + "'");
			ps.println("  username           = '" + username + "'");
			ps.println("  password           = '" + password + "'");
			ps.println("");
			ps.println("  chartCount         = "  + chartCount);
			ps.println("  startTime          = "  + startTime + " -->> 0=continue, ###=start maximum ### days back in time");
			ps.println("  sleepTime          = "  + sleepTime + " seconds.");
			ps.println("  shutdownOnHalfDay  = "  + shutdownOnHalfDay);
			ps.println("  defragOnNewDay     = "  + defragOnNewDay);
			ps.println("  infoConsumeAboveMs = "  + infoConsumeAboveMs);
			ps.println("  serverNames        = '" + serverNames + "'");
			ps.println("  h2Stats            = "  + h2Stats);
			ps.println("#################################################################");
			ps.println("");
		}

		public void evaluate()
		{
			// replace environment variable 'DBXTUNE_SAVE_DIR' in URL
			if (url.indexOf("${DBXTUNE_SAVE_DIR}") != -1)
			{
				String envVal = System.getenv("DBXTUNE_SAVE_DIR");
				if (envVal == null)
					envVal = System.getProperty("user.home", "~").replace('\\', '/');
				
				url = url.replace("${DBXTUNE_SAVE_DIR}", envVal);
			}

			if ( url != null && url.startsWith("jdbc:h2:") )
			{
				// The below are a **SIMPLE** H2UrlHelper implementation
				String h2UrlStart = "jdbc:h2:file:";
				String urlVal  = url.substring(h2UrlStart.length());

				if (urlVal.indexOf(';') >= 0)
				{
					int index = urlVal.indexOf(';');
					urlVal = urlVal.substring(0, index);
				}

				_h2DbFile = new File(urlVal +  ".mv.db");
				if ( ! _h2DbFile.exists() )
					_h2DbFile = null;
			}
		}

		private static int toInt(String val, int defValue)
		{
			try
			{
				return Integer.parseInt(val);
			}
			catch (NumberFormatException ex)
			{
				_logger.warning("Problems parsing value '" + val + "', which is not an integer, returning default value '" + defValue + "' instead.");
				return defValue;
			}
		}
		private static Options parseCmdLine(String[] args)
		{
////			conf.setProperty("startTime", "-365"); // generate 365 days "back in time" (no sleep time will be applied while we are "behind" and generating "older" records )
//			conf.setProperty("startTime", "0");    // start NOW
//			conf.setProperty("startTime", "");     // (empty) is same as: continue generate where we left off (if we havn't got any records inserted... start 365 days "back in time")
//			conf.setProperty("sleepTime", "30");
//			conf.setProperty("serverNames", 
//					  "PROD_1A_ASE,   PROD_1B_ASE,   DEV_ASE,   SYS_ASE,   INT_ASE,   STAGE_ASE, "
//					+ "prod-1a-pg,    prod-1b-pg,    dev-pg,    sys-pg,    int-pg,    stage-pg, "
//					+ "prod-1a-mssql, prod-1b-mssql, dev-mssql, sys-mssql, int-mssql, stage-mssql");
	//
//			conf.setProperty(PROPKEY_JDBC_URL,      DEFAULT_JDBC_URL);
//			conf.setProperty(PROPKEY_JDBC_USERNAME, DEFAULT_JDBC_USERNAME);
//			conf.setProperty(PROPKEY_JDBC_PASSWORD, DEFAULT_JDBC_PASSWORD);
			
			
			Options opt = new Options();

			for (int i = 0; args != null && i < args.length; i++) 
			{
				String arg = args[i];
				if      (arg.equals("-url")                     ) { opt.url                = args[++i]; } 
				else if (arg.equals("-username")                ) { opt.username           = args[++i]; }
				else if (arg.equals("-password")                ) { opt.password           = args[++i]; }
				else if (arg.equals("-chartCount")              ) { opt.chartCount         = toInt(args[++i], opt.chartCount); }
				else if (arg.equals("-startTime")               ) { opt.startTime          = toInt(args[++i], opt.startTime); }
				else if (arg.equals("-sleepTime")               ) { opt.sleepTime          = toInt(args[++i], opt.sleepTime); }
				else if (arg.equals("-shutdownOnHalfDay")       ) { opt.shutdownOnHalfDay  = "true".equalsIgnoreCase(args[++i]); }
				else if (arg.equals("-defragOnNewDay")          ) { opt.defragOnNewDay     = "true".equalsIgnoreCase(args[++i]); }
				else if (arg.equals("-infoConsumeAboveMs")      ) { opt.infoConsumeAboveMs = toInt(args[++i], opt.infoConsumeAboveMs); }
				else if (arg.equals("-serverNames")             ) { opt.serverNames        = args[++i]; }
				else if (arg.equals("-h2Stats")                 ) { opt.h2Stats            = toInt(args[++i], opt.h2Stats); }
				else if (arg.equals("-help") || arg.equals("-?")) { showUsage(opt, "", null); return null; } 
				else                                              { showUsage(opt, "Unknown cmdline switch", arg);  return null; }
			}

			if ( opt.url == null || (opt.url != null && opt.url.trim().equals("")) ) 
			{
				showUsage(opt, "URL not set", null);
				return null;
			}
			
			if ( opt.serverNames == null || (opt.serverNames != null && opt.serverNames.trim().equals("")) )
			{
				showUsage(opt, "serverNames not set", null);
				return null;
			}
			
			opt.evaluate();
			return opt;
		}

		private static void showUsage(Options opt, String msg, String unknownOption)
		{
			if (unknownOption != null)
			{
				System.out.println("");
				System.out.println("ERROR: unknwon command line switch: " + unknownOption);
			}

			if (msg != null)
			{
				System.out.println("");
				System.out.println("ERROR: " + msg);
			}

			System.out.println("");
			System.out.println("Command Line switches:");
			System.out.println("  -url                <h2-url>      ");
			System.out.println("  -username           <user>        ");
			System.out.println("  -password           <passwd>      ");
			System.out.println("  -startTime          <###>         0 = start NOW, 365 = continue generate where we left off (if we havn't got any records inserted... start 365 days 'back in time'.");
			System.out.println("  -sleepTime          <sec>         Seconds to sleep between inserts... when we reach CURRENT-TIME");
			System.out.println("  -shutdownOnHalfDay  <true|false>  Do H2 'shutdown' when a sampleTime flips over 'YYYY-MM-DD h' so at 10:00 and 20:00");
			System.out.println("  -defragOnNewDay     <true|false>  Do H2 'shutdown defarg' when a sampleTime passes midnight");
			System.out.println("  -infoConsumeAboveMs <###>         Only Write INFO messages where 'consume' takes above ### ms");
			System.out.println("  -serverNames        <csv>         Comma Separated list of Values, which is server names to insert values for.");
			System.out.println("  -h2Stats            <##>          Write some statistics about H2 every ## second. 0=disabled");
			System.out.println("");
			
			opt.evaluate();
			opt.printOptions(System.out);
		}
		
		public List<String> getServerNames()
		{
			List<String> list = new ArrayList<>();
			
			String[] sna = serverNames.split(",");
			for (int i=0; i<sna.length; i++)
			{
				String name = sna[i].trim();
				if (name.equals(""))
					continue;
				
				list.add(name);
			}
			
			return list;
		}
	}

	
	
	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------
	// MAIN
	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------
	public static void main(String[] args)
	{
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT - %4$-7s - %5$s %n");

		Options opt = Options.parseCmdLine(args);
		if (opt == null)
			return;

		StopType stopType = StopType.STOP_AND_EXIT;

		// Create this up here instead of on every loop, otherwise some statistics will be off
		DbxCentralDummyLoad dl = new DbxCentralDummyLoad(opt);

		// and ADD Dummy Workers/Collectors
		for (String name : opt.getServerNames())
		{
			PerfCollector dummyCollector = new PerfCollector(dl, name, opt.chartCount, opt.startTime, opt.sleepTime * 1000, opt.shutdownOnHalfDay, opt.defragOnNewDay);
			dl.addDummyCollecto(dummyCollector);
		}
		

		// Loop until: STOP_AND_EXIT
		do
		{
			// Print current options we are running with
			opt.printOptions(System.out);
			
			if (dl.openDatabase())
			{
				dl.start();
				dl.startDummyCollectors();

				// Wait for collectors to finish
				stopType = dl.waitForCollectorsToStop();
				dl.stop(true); // true == WaitForQueueToBeEmpty

				if (StopType.STOP_WITH_H2_SHUTDOWN_NORMAL_AND_RESTART.equals(stopType))
				{
					dl.shutdownNormal();
				}
				else if (StopType.STOP_WITH_H2_SHUTDOWN_DEFRAG_AND_RESTART.equals(stopType))
				{
					dl.shutdownDefrag();
				}
				
				dl.closeDatabase();
			}
			
		}
		while ( ! StopType.STOP_AND_EXIT.equals(stopType) );
	}
}
