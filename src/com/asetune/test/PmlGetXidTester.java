/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class PmlGetXidTester
{
//	private final static String URL_BASE = "jdbc:sybase:Tds:mig1-sybase.maxm.se:5000/PML?ENCRYPT_PASSWORD=true";
	private String _url      = "";
	private String _username = "sa";
	private String _password = "";

	private List<DbWorker> _dbWorkers = new ArrayList<>();
//	public static StatisticsThread _stat;
	
	private ConcurrentHashMap<Integer, String> _idMap = new ConcurrentHashMap<>();

	private int     _maxWorkers = 0;
	private int     _numOfCalls = 0;
	private int     _sleepTimeInMs = 100;
	private boolean _autoCommit;

	public PmlGetXidTester(String username, String password, String url, int workers, int numOfCalls, int sleepTimeInMs, boolean autoCommit)
	{
		_username      = username;
		_password      = password;
		_url           = url;
		_maxWorkers    = workers;
		_numOfCalls    = numOfCalls;
		_sleepTimeInMs = sleepTimeInMs;
		_autoCommit    = autoCommit;

		_idMap = new ConcurrentHashMap<>(_maxWorkers * _numOfCalls);
	}
	
	private void start()
	{
		for (int w=0; w<_maxWorkers; w++)
		{
			boolean autoCommit = _autoCommit;
//			autoCommit = (w % 2 == 0); // Use autocommit for some workers and 'chained mode' for some workers
			DbWorker dbWorker = new DbWorker(w, _numOfCalls, _sleepTimeInMs, autoCommit);
			try
			{
				dbWorker.connect(_url, _username, _password);
				dbWorker.onConnect();
				dbWorker.start();
				
				_dbWorkers.add(dbWorker);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				System.exit(1);
			}
		}
		
		if (_dbWorkers.size() > 0)
		{
//			_stat = new StatisticsThread();
//			_stat.start();
		}
		
		// Waitfor workers to finish
		System.out.println("Waitfor workers to finish...");

		// Waitfor workes to finish
		long startTime = System.currentTimeMillis();
		try
		{
			for (DbWorker dbWorker : _dbWorkers)
				dbWorker._thread.join();
		}
		catch (InterruptedException ignore) {}
		long execTime = System.currentTimeMillis() - startTime;
		
//		System.out.println("All Workers are done... execTimeInMs="+execTime+", execTimeInMsPerWorker=" + (execTime/_maxWorkers) + ", avgExecTimeInMsPerCall=" + (execTime*1.0/_maxWorkers/_numOfCalls));
		System.out.println("All Workers are done... execTimeInMs="+execTime);
		
		// Print min/max/avg execution times for ALL Workers
		long sumWorkersExecTime = 0;
		long minWorkersExecTime = Integer.MAX_VALUE;
		long maxWorkersExecTime = 0;
		for (DbWorker dbWorker : _dbWorkers)
		{
			sumWorkersExecTime += dbWorker._sumExecTime;
			minWorkersExecTime = Math.min(minWorkersExecTime, dbWorker._minExecTime);
			maxWorkersExecTime = Math.max(maxWorkersExecTime, dbWorker._maxExecTime);
		}
		System.out.println("    sumWorkersExecTimeInMs="+sumWorkersExecTime);
		System.out.println("    AvgWorkersExecTimeInMs="+sumWorkersExecTime*1.0/_dbWorkers.size()/_numOfCalls);
		System.out.println("    minWorkersExecTimeInMs="+minWorkersExecTime);
		System.out.println("    maxWorkersExecTimeInMs="+maxWorkersExecTime);

		// Check that we managed to check ALL the XID's
		int expectedEntries = _maxWorkers * _numOfCalls;
		if (expectedEntries != _idMap.size())
		{
			System.out.println("ERROR: expectedEntries="+expectedEntries+", genereatedXidCount="+_idMap.size());
		}
		else
		{
			System.out.println("OK, worked as expected.  genereatedXidCount="+_idMap.size());
		}
		
		// Close
		System.out.println("Closing workers...");
		for (DbWorker dbWorker : _dbWorkers)
			dbWorker.close();

		System.out.println("DONE...");

//		while(true)
//		{
//			// maybe check if we should STOP running...
//			//FIXME:
//			
//			// sleep for a while
//			try { Thread.sleep(1000); }
//			catch (InterruptedException ex) {}
//		}
	}


	
	/*
	 * ----------------------------------------------------------------------------------
	 * ----------------------------------------------------------------------------------
	 * DATABASE WORKER
	 * ----------------------------------------------------------------------------------
	 * ----------------------------------------------------------------------------------
	 */
	private class DbWorker
	implements Runnable
	{
		private Connection _conn;
		private Thread     _thread;
		private int        _workerId;
		private int        _numOfCalls;
		private boolean    _running;
		private int        _sleepTimeMs = 10;
		private boolean    _autoCommit  = true;
		
		private long       _sumExecTime = 0;
		private long       _minExecTime = Integer.MAX_VALUE;
		private long       _maxExecTime = 0;

		public DbWorker(int workerId, int numOfCalls, int sleepTimeMs, boolean autoCommit)
		{
			_workerId    = workerId;
			_numOfCalls  = numOfCalls;
			_sleepTimeMs = sleepTimeMs;
			_autoCommit  = autoCommit;
		}
		
		public void connect(String url, String user, String passwd)
		throws SQLException
		{
			System.out.println("DBWORKER["+_workerId+"]: Connecting to URL='"+url+"'. numOfCalls="+_numOfCalls+", sleepTimeMs="+_sleepTimeMs+", autocommit="+_autoCommit);
			_conn = DriverManager.getConnection(url, user, passwd);
//			System.out.println("DBWORKER["+_workerId+"]: Connected to url: "+url);
		}
		
		public void close()
		{
			try { _conn.close(); }
			catch(SQLException ignore) {}
		}
		
		public void onConnect()
		{
			try {
				_conn.setAutoCommit(_autoCommit);
			} catch(SQLException ex) {
				ex.printStackTrace();
			}
			
			// Only first worker should create tables etc...
			if (_workerId != 0)
				return;

			// CHECK if we ended up in the correct database or similar
			String sql = "select @@servername, db_name()";
			try (Statement stmnt = _conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
				{
					String srvName = rs.getString(1);
					String dbname  = rs.getString(2);
					
					System.out.println("DBWORKER["+_workerId+"]: srvName='"+srvName+"', dbname='"+dbname+"'.");
				}
			}
			catch (SQLException ex) 
			{
				ex.printStackTrace();
			}
		}
		
		public void doWork(int callId)
		{
//			System.out.println("ERROR: DBWORKER["+_workerId+"]: doWork(callId="+callId+")");
			String sql = "" 
					+ "declare @ret_id int \n"
//					+ "exec get_xid_tmp 1, @last_id=@ret_id out \n"
					+ "exec get_xid 1, @last_id=@ret_id out \n"
					+ "select @ret_id as RET_ID \n"
					;
//			boolean asSingelSqlBatch = false;
//			if (asSingelSqlBatch)
//			{
//				sql = "" 
//					+ "create table #xxx (xid int) \n"
//					+ "declare @cnt int \n"
//					+ "set @cnt=1000 \n"
//					+ "while (@cnt > 0) \n"
//					+ "begin \n"
//					+ "    declare @ret_id int \n"
//					+ "    exec get_xid_tmp 1, @last_id=@ret_id out \n"
//					+ "    insert into #xxx values(@ret_id) \n"
//					+ "    set @cnt = @cnt -1 \n"
//					+ "end \n"
//					+ "select xid from #xxx \n"
//					+ "drop table #xxx \n"
//					;
//			}

			long startTime = System.currentTimeMillis();
			
			try (Statement stmnt = _conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
				{
					int xid = rs.getInt(1);
					if (xid == 0)
					{
						System.out.println("ERROR: DBWORKER["+_workerId+"]: XID == 0");
					}
					else
					{
						String exists = _idMap.put(xid, "WorkerId=" + _workerId + ", callId=" + callId);
						if (exists != null)
							System.out.println("ERROR: DBWORKER["+_workerId+"]: XID="+xid+", already exists... added by: "+exists);
					}
				}
				if ( ! _autoCommit )
					_conn.commit();
			}
			catch (SQLException ex) 
			{
				//ex.printStackTrace();
				System.out.println("MsgNum="+ex.getErrorCode()+", SqlState="+ex.getSQLState()+", MsgStr='"+ex.getMessage().trim()+"'.");
			}

			long execTime = System.currentTimeMillis() - startTime;
			
			_sumExecTime += execTime;
			_minExecTime = Math.min(_minExecTime, execTime);
			_maxExecTime = Math.max(_maxExecTime, execTime);
		}
		
		public void start()
		{
			_thread = new Thread(this);
			_thread.setName("DbWorker-"+_workerId);
//			_thread.setDaemon(true);
			_thread.start();
		}
		
		public void stop()
		{
			_running = false;
			_thread.interrupt();
		}

		@Override
		public void run()
		{
			_running = true;
			try
			{
				for (int i=0; i<_numOfCalls; i++)
				{
					doWork(i);

					// sleep some time (I guess sleep 0 will cause the thread to yeild if local CPU is bussy, otherwiese next NetWork Call would probably yeild as well)
					Thread.sleep(_sleepTimeMs);

					if ( ! _running )
						break;
				}
			}
			catch (InterruptedException e)
			{
			}
			_running = false;
		}
	}


	/*
	 * ----------------------------------------------------------------------------------
	 * ----------------------------------------------------------------------------------
	 * STATISTICS... prints out how things are going...
	 * ----------------------------------------------------------------------------------
	 * ----------------------------------------------------------------------------------
	 */
//	private class StatisticsThread 
//	implements Runnable
//	{
//		private Thread _thread;
//		private boolean _running;
//		private File _h2Dbfile;
//
//		public int _crTable;
//		public int _crIndex;
//		public int _inserts;
//		
//		private long _lastDbFileSize = -1;
//		private long _lastProbeTime  = -1;
//		
//		private int _sleepTime = 5000;
//
//		public StatisticsThread()
//		{
//		}
//
//		public void incInserts() { _inserts++; }
//		public void incCrTable() { _crTable++; }
//		public void incCrIndex() { _crIndex++; }
//
//		public void printStats()
//		{
//			if (_h2Dbfile.exists())
//			{
//				if (_lastProbeTime == -1)
//				{
//					_lastDbFileSize = _h2Dbfile.length();
//					_lastProbeTime  = System.currentTimeMillis();
//					return;
//				}
//
//				long msSinceLastCheck = System.currentTimeMillis() - _lastProbeTime;
//				long fileDiffKb       = (int) (_h2Dbfile.length() - _lastDbFileSize) / 1024;
//				
//				int dbFileSizeKb = (int) (_h2Dbfile.length() / 1024); 
//				int dbFileSizeMb = (int) (_h2Dbfile.length() / 1024); 
//
//				System.out.println("STATS: "
//						+ "fileDiffKb="    + fileDiffKb
//						+", dbFileSizeKb=" + dbFileSizeKb
//						+", inserts="      + _inserts
//						+", crTable="      + _crTable
//						+", crIndex="      + _crIndex
//						);
//
//				
//				_lastDbFileSize = _h2Dbfile.length();
//				_lastProbeTime  = System.currentTimeMillis();
//			}
//			else
//			{
//				System.out.println("STATS: File does not exist: _h2Dbfile="+_h2Dbfile);
//			}
//		}
//		
//		public void start()
//		{
//			_thread = new Thread(this);
//			_thread.setName("StatisticsThread");
//			_thread.setDaemon(true);
//			_thread.start();
//		}
//
//		public void stop()
//		{
//			_running = false;
//			_thread.interrupt();
//		}
//
//		@Override
//		public void run()
//		{
//			_running = true;
//			try
//			{
//				while (_running)
//				{
//					printStats();
//					
//					Thread.sleep(_sleepTime);
//				}
//			}
//			catch (InterruptedException e)
//			{
//				_running = false;
//			}
//		}
//	}

	
	public static void printUsage(String msg)
	{
		System.out.println("");
		System.out.println("Usage: [user] [passwd] [url] [numOfWorkers] [numOfCallsPerWorker] [sleepTime] [autoCommit]");
		System.out.println("");
	}
	
	public static void main(String[] args)
	{
		String  username    = "sa";
		String password     = "dummy";
		String  url         = "jdbc:sybase:Tds:mig1-sybase.maxm.se:5000/PML";
		int     workerCount = 20;
		int     numOfCalls  = 2000;
		int     sleepTime   = 0;
		boolean autoCommit  = true;

		if (args.length < 1)
		{
			printUsage("");
			System.exit(1);
		}

		if (args.length >= 1) { username    = args[1 -1]; }
		if (args.length >= 2) { password    = args[2 -1]; }
		if (args.length >= 3) { url         = args[3 -1]; }
		if (args.length >= 4) { workerCount = Integer.parseInt(args[4 -1]); }
		if (args.length >= 5) { numOfCalls  = Integer.parseInt(args[5 -1]); }
		if (args.length >= 6) { sleepTime   = Integer.parseInt(args[6 -1]); }
		if (args.length >= 7) { autoCommit  = args[7 -1].trim().equalsIgnoreCase("true"); }
		
		System.out.println("===================================================");
		System.out.println("INFO:");
		System.out.println("      username    = '"+username+"'.");
		System.out.println("      password    = '*secret*'.");
		System.out.println("      url         = '"+url+"'.");
		System.out.println("      workerCount = "+workerCount);
		System.out.println("      numOfCalls  = "+numOfCalls);
		System.out.println("      sleepTime   = "+sleepTime);
		System.out.println("      autoCommit  = "+autoCommit);
		System.out.println("===================================================");
//		String[] sa = System.getProperty("java.class.path").split(";");
//		for (int i=0; i<sa.length; i++)
//		{
//			System.out.println(" classpath["+i+"]="+sa[i]);
//		}
//		System.out.println(" java.version="+System.getProperty("java.version"));
//		System.out.println("===================================================");
		
		PmlGetXidTester tester = new PmlGetXidTester(username, password, url, workerCount, numOfCalls, sleepTime, autoCommit);
		tester.start();
	}

}

/*
cd C:\projects\AseTune
java -cp C:\projects\AseTune\classes;C:\projects\AseTune\lib\jconn4.jar com.asetune.test.PmlGetXidTester
*/
