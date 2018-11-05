package com.asetune.test;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class H2LongTermTester
{
//	private String URL_BASE = "jdbc:h2:file:/home/sybase/.dbxtune/dbxc/data/DBXTUNE_CENTRAL_DB;DATABASE_TO_UPPER=false;MAX_COMPACT_TIME=2000;COMPRESS=TRUE;WRITE_DELAY=30000;DB_CLOSE_ON_EXIT=FALSE";
	private String URL_BASE = "jdbc:h2:file:<DBNAME>;DATABASE_TO_UPPER=false;MAX_COMPACT_TIME=2000;COMPRESS=TRUE;WRITE_DELAY=30000";
	private String _url      = "";
	private File   _dbFile;
	private String _username = "";
	private String _password = "";

	private List<DbWorker> _dbWorkers = new ArrayList<>();
	public static StatisticsThread _stat;

	private int _maxWorkers = 1;
	private int _tableCount = 10;
	private int _columnCount = 10;

	public H2LongTermTester(String dbname)
	{
		if (dbname == null)
			dbname = "H2_LONG_TERM_TEST";

		_url = URL_BASE.replace("<DBNAME>", dbname);
		_dbFile = new File(dbname + ".mv.db");
	}
	
	private void start()
	{
		for (int w=0; w<_maxWorkers; w++)
		{
			DbWorker dbWorker = new DbWorker(w, _tableCount, _columnCount);
			try
			{
				dbWorker.connect(_url, "sa", "");
				dbWorker.onConnect();
				dbWorker.start();
				
				_dbWorkers.add(dbWorker);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
		
		if (_dbWorkers.size() > 0)
		{
			_stat = new StatisticsThread();
			_stat.start();
		}
		
		while(true)
		{
			// maybe check if we should STOP running...
			//FIXME:
			
			// sleep for a while
			try { Thread.sleep(1000); }
			catch (InterruptedException ex) {}
		}
	}


	/** 
	 * DATABASE Tables
	 */
	private static class DbTable
	{
		protected int    _tabId;
		protected String _tabName;
		protected int    _colCount;
		protected DbWorker _dbWorker;
		
		public char getQuotedIdentifierChar() { return '"'; }

		public DbTable(DbWorker dbWorker, int tabId, int colCount)
		{
			_dbWorker = dbWorker;
			_tabId    = tabId;
			_tabName  = "t" + _tabId;
			_colCount = colCount;
		}
		
		public int dbExec(String sql, boolean printErrors)
//		throws SQLException
		{
//			System.out.println("dbExec(): SEND SQL: " + sql);

			try
			{
				int count = 0;
				Statement s = _dbWorker._conn.createStatement();
				s.execute(sql);
				count = s.getUpdateCount();
				s.close();
				
				return count;
			}
			catch(SQLException e)
			{
				if (printErrors)
					System.out.println("Problems when executing sql statement: "+sql+" SqlException: ErrorCode="+e.getErrorCode()+", SQLState="+e.getSQLState()+", toString="+e.toString());
//				throw e;
				return -1;
			}
		}
		
		private boolean dbDdlExec(Connection conn, String sql)
//		throws SQLException
		{
			System.out.println("dbDdlExec(): SEND DDL SQL: " + sql);

			try
			{
				boolean autoCommitWasChanged = false;

				if (conn.getAutoCommit() != true)
				{
					autoCommitWasChanged = true;
					
					// In ASE the above conn.getAutoCommit() does execute 'select @@tranchained' in the ASE
					// Which causes the conn.setAutoCommit(true) -> set CHAINED off
					// to fail with error: Msg 226, SET CHAINED command not allowed within multi-statement transaction
					//
					// In the JDBC documentation it says:
					// NOTE: If this method is called during a transaction, the transaction is committed.
					//
					// So it should be safe to do a commit here, that is what jConnect should have done...
					conn.commit();

					conn.setAutoCommit(true);
				}

				Statement s = conn.createStatement();
				s.execute(sql);
				s.close();

				if (autoCommitWasChanged)
				{
					conn.setAutoCommit(false);
				}
			}
			catch(SQLException e)
			{
				//_logger.warn("Problems when executing DDL sql statement: "+sql);
				// throws Exception if it's a severe problem
//				throw e;
				e.printStackTrace();
			}

			return true;
		}
		
		public void insert(int rows)
		{
			for (int i=0; i<rows; i++)
			{
				String sql = createInsertSql();
				dbExec(sql, true);
				_stat.incInserts();
//				System.out.println("insert: "+sql);
			}
		}

		public void createTable()
		{
			String sql = createTableSql();
			dbDdlExec(_dbWorker._conn, sql);
			if (_stat != null)
				_stat.incCrTable();
		}

		public void createIndex()
		{
			List<String> list = createIndexeSql();
			for (String sql : list)
			{
				dbDdlExec(_dbWorker._conn, sql);
				if (_stat != null)
					_stat.incCrIndex();
			}
		}

		public String createTableSql()
		{
			char qic = getQuotedIdentifierChar();
			StringBuilder sb = new StringBuilder();

			String ifNotExists = "IF NOT EXISTS ";

			sb.append("create table " + ifNotExists + qic+_tabName+qic + "\n");
			sb.append("( \n");
			sb.append("    "+qic+"SessionStartTime" +qic+" datetime  not null \n");
			sb.append("   ,"+qic+"SessionSampleTime"+qic+" datetime  not null \n");
			sb.append("   ,"+qic+"CmSampleTime"     +qic+" datetime  not null \n");
			sb.append("\n");

			// loop all columns
			for (int c=1; c<=_colCount; c++)
			{
				String colName = "c" + c;
				sb.append("   ," + qic + colName + qic + " numeric(16, 2) null\n");
			}
			sb.append(") \n");

			//System.out.println("getGraphTableDdlString: "+sb.toString());
			return sb.toString();
		}

		public List<String> createIndexeSql()
		{
			char qic = getQuotedIdentifierChar();
			List<String> list = new ArrayList<>();

			String ifNotExists = "IF NOT EXISTS ";
			
			list.add("create index " + ifNotExists + qic+_tabName+"_ix1"+qic + " on " + qic+_tabName+qic + "("+qic+"SessionSampleTime"+qic+")\n");
			
			return list;
		}

//		public String createInsertSql()
//		{
//			char qic = getQuotedIdentifierChar();
//			StringBuilder sb = new StringBuilder();
//			
//			sb.append("insert into " + qic+_tabName+qic + "\n");
//			sb.append("values(?, ?, ? "); //  SessionStartTime, SessionSampleTime, CmSampleTime
//
//			// loop all columns
//			for (int c=1; _colCount<=_colCount; c++)
//				sb.append(", ?");
//			sb.append(")");
//
//			return sb.toString();
//		}
		public String createInsertSql()
		{
			char qic = getQuotedIdentifierChar();
			StringBuilder sb = new StringBuilder();

			sb.append("insert into " + qic+_tabName+qic + " ");
			sb.append("values('2018-01-01 01:01:01', '2018-01-01 01:01:01', '2018-01-01 01:01:01' "); //  SessionStartTime, SessionSampleTime, CmSampleTime

			ThreadLocalRandom generator = ThreadLocalRandom.current();

			// loop all columns
			for (int c=1; c<=_colCount; c++)
				sb.append(", " + generator.nextGaussian() );
			sb.append(")");
			
			return sb.toString();
		}
	}
	
	/*
	 * ----------------------------------------------------------------------------------
	 * ----------------------------------------------------------------------------------
	 * DATABASE WORKER
	 * ----------------------------------------------------------------------------------
	 * ----------------------------------------------------------------------------------
	 */
	private static class DbWorker
	implements Runnable
	{
		private Connection _conn;
		private Thread _thread;
		private int    _workerId;
		private int    _tableCount;
		private boolean _running;
		private List<DbTable> _tableList = new ArrayList<>();

		public DbWorker(int workerId, int tableCount, int colCount)
		{
			_workerId   = workerId;
			_tableCount = tableCount;

			for (int t=0; t<_tableCount; t++)
			{
				_tableList.add( new DbTable(this, t, colCount) );
			}
		}
		
		public void connect(String url, String user, String passwd)
		throws SQLException
		{
			_conn = DriverManager.getConnection(url, user, passwd);
			System.out.println("DBWORKER["+_workerId+"]: Connected to url: "+url);
		}
		
		public void onConnect()
		{
			// Only first worker should create tables etc...
			if (_workerId != 0)
				return;
			
			for (DbTable dbTable : _tableList)
			{
				dbTable.createTable();
				dbTable.createIndex();
			}
		}
		
		public void doWork()
		{
			for (DbTable dbTable : _tableList)
			{
				dbTable.insert(1000);
			}
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
				while (_running)
				{
					doWork();
					
					Thread.sleep(1000);
				}
			}
			catch (InterruptedException e)
			{
				_running = false;
			}
		}
	}


	/*
	 * ----------------------------------------------------------------------------------
	 * ----------------------------------------------------------------------------------
	 * STATISTICS... prints out how things are going...
	 * ----------------------------------------------------------------------------------
	 * ----------------------------------------------------------------------------------
	 */
	private class StatisticsThread 
	implements Runnable
	{
		private Thread _thread;
		private boolean _running;
		private File _h2Dbfile;

		public int _crTable;
		public int _crIndex;
		public int _inserts;
		
		private long _lastDbFileSize = -1;
		private long _lastProbeTime  = -1;
		
		private int _sleepTime = 5000;

		public StatisticsThread()
		{
			_h2Dbfile = _dbFile;
		}

		public void incInserts() { _inserts++; }
		public void incCrTable() { _crTable++; }
		public void incCrIndex() { _crIndex++; }

		public void printStats()
		{
			if (_h2Dbfile.exists())
			{
				if (_lastProbeTime == -1)
				{
					_lastDbFileSize = _h2Dbfile.length();
					_lastProbeTime  = System.currentTimeMillis();
					return;
				}

				long msSinceLastCheck = System.currentTimeMillis() - _lastProbeTime;
				long fileDiffKb       = (int) (_h2Dbfile.length() - _lastDbFileSize) / 1024;
				
				int dbFileSizeKb = (int) (_h2Dbfile.length() / 1024); 
				int dbFileSizeMb = (int) (_h2Dbfile.length() / 1024); 

				System.out.println("STATS: "
						+ "fileDiffKb="    + fileDiffKb
						+", dbFileSizeKb=" + dbFileSizeKb
						+", inserts="      + _inserts
						+", crTable="      + _crTable
						+", crIndex="      + _crIndex
						);

				
				_lastDbFileSize = _h2Dbfile.length();
				_lastProbeTime  = System.currentTimeMillis();
			}
			else
			{
				System.out.println("STATS: File does not exist: _h2Dbfile="+_h2Dbfile);
			}
		}
		
		public void start()
		{
			_thread = new Thread(this);
			_thread.setName("StatisticsThread");
			_thread.setDaemon(true);
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
				while (_running)
				{
					printStats();
					
					Thread.sleep(_sleepTime);
				}
			}
			catch (InterruptedException e)
			{
				_running = false;
			}
		}
	}

	
	public static void printUsage(String msg)
	{
		System.out.println("");
		System.out.println("Usage: dbname");
		System.out.println("");
	}
	
	public static void main(String[] args)
	{
		String dbname;
		
		if (args.length < 1)
		{
			dbname = System.getProperty("java.io.tmpdir") + File.separatorChar + "H2_LTT_DB";
//			printUsage("");
//			System.exit(1);
		}
		else
		{
			dbname = args[0];
		}
		

		System.out.println("INFO: dbname='"+dbname+"'.");
		
		H2LongTermTester ltt = new H2LongTermTester(dbname);
		ltt.start();
	}

}
