/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.dbxtune.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class MaxmPerfTest
{
	private static int workerCount = 5;
//	private static final CyclicBarrier barrier = new CyclicBarrier(workerCount);

//	private static boolean waitForBarier        = true;  // Allan want to wait...
//	private static boolean purgeQueueRecord     = false; // Allan want to keep records in queue...
	private static boolean waitForBarier        = false; // Do not wait...
	private static boolean purgeQueueRecord     = true;  // Remove records from queue
//	private static boolean truncateTableAtStart = true;  // Cleanup...
//	private static boolean hideError_547        = true;  // Do not print on error 547 -- ERROR: 547 -- The INSERT statement conflicted with the CHECK constraint "check_mapped_to_max_one_individual_id". The conflict occurred in database "customerapi_gorans2", table "dbo.individual_id".
//	private static boolean useTabLockHint       = true;  // Use 'WITH (TABLOCKX)' at table level when doing insert
	private static boolean useTabLockHint       = false;  // Use 'WITH (TABLOCKX)' at table level when doing insert
	private static boolean truncateTableAtStart = true;  // Cleanup...
	private static boolean hideError_547        = false;  // Do not print on error 547 -- ERROR: 547 -- The INSERT statement conflicted with the CHECK constraint "check_mapped_to_max_one_individual_id". The conflict occurred in database "customerapi_gorans2", table "dbo.individual_id".

	public static void main(String[] args)
	{
		//-------------------------------------------------------------
		// What Connection Profile --  is the test for
		//-------------------------------------------------------------
		// 1 - MS SQL Server -- localhost:1433
		// 2 - Sybase ASE    -- dba-1-ase:5000
		// 3 - Azure DB      -- gorans-srv.database.windows.net:1433
		//-------------------------------------------------------------
		int connProfile = 3;
		
		String csvfile  = "";
		String username = "";
		String password = "";
		String dbname   = "";
		String appname  = "Repro-CustomerApi-IndividualExternalIdMapping";
		String url      = "";

		if (connProfile == 1)
		{
			//-------------------------------------------------------------
			// 1 - SQL Server - localhost:1433
			//-------------------------------------------------------------
			csvfile  = "C:\\projects\\MaxM\\mssql__prod_a1_mssql__customerapi\\valid_personal_numbers_from_dev_small.csv"; // 100_000 records
			useTabLockHint = true;  // Use 'WITH (TABLOCKX)' at table level when doing insert
			useTabLockHint = false;  // Lets try with: 'ALTER DATABASE customerapi_gorans2 SET READ_COMMITTED_SNAPSHOT ON' for higher concurrency 
			workerCount = 20;

			username = "gorans_sa";
			password = "xxxxxxxxx";
			dbname   = "customerapi_gorans2";
			url      = "jdbc:sqlserver://localhost:1433;databaseName=" + dbname + ";applicationName=" + appname + ";encrypt=true;trustServerCertificate=true";
		}
		else if (connProfile == 2)
		{
			//-------------------------------------------------------------
			// 2 - Sybase ASE    -- dba-1-ase:5000
			//-------------------------------------------------------------
			csvfile  = "C:\\projects\\MaxM\\mssql__prod_a1_mssql__customerapi\\valid_personal_numbers_from_dev_small.csv"; // 100_000 records
			useTabLockHint = false;  // not supported by Sybase ASE
			workerCount = 50;

			username = "sa";
			password = "xxxxxxxxx";
			dbname   = "customerapi_gorans2";
			url      = "jdbc:sybase:Tds:dba-1-ase:5000/" + dbname + "?ENCRYPT_PASSWORD=true&APPLICATIONNAME=" + appname + "";
		}
		else if (connProfile == 3)
		{
			//-------------------------------------------------------------
			// 3 - Azure DB      -- gorans-srv.database.windows.net:1433
			//-------------------------------------------------------------
			csvfile  = "C:\\projects\\MaxM\\mssql__prod_a1_mssql__customerapi\\valid_personal_numbers_from_dev_small.csv"; // 100_000 records
			useTabLockHint = false;  // not supported by Sybase ASE
			workerCount = 25;

			username = "gorans_sa@gorans-srv";
			password = "xxxxxxxxxxx";
			dbname   = "gorans";
			url      = "jdbc:sqlserver://gorans-srv.database.windows.net:1433;database=" + dbname + ";hostNameInCertificate=*.database.windows.net;encrypt=true;trustServerCertificate=false;applicationName=" + appname;
		}
		else
		{
			throw new RuntimeException("Unsupported connProfile number...");
		}

		// Create a Queue
		DataQueue dataQueue = new DataQueue();

		// Read the file
		dataQueue.readFile(csvfile);

		
		try
		{
			// do setup/cleanup in DBMS (truncate tables or whatever we need to do)
			setupDbms(username, password, url, truncateTableAtStart);

			System.out.println("Starting " + workerCount + " DBMS Workers...");

			List<DbmsWorker> workerList = new ArrayList<>();
			for (int w = 0; w < workerCount; w++)
			{
				DbmsWorker dbmsWorker = new DbmsWorker(dataQueue, w + 1);

				// Connect
				dbmsWorker.connect(username, password, url);

				// Start
				dbmsWorker.startWorker();

				workerList.add(dbmsWorker);
			}
			
			
			// Wait for threads to finish 
			for (DbmsWorker w : workerList)
			{
				w._thread.join();
//				if (w._thread.isAlive())
//				{
//					System.out.println("Waiting for thread '" + w._thread.getName() + "' to end...");
//					w._thread.join();
//				}
			}


			// Summary 
			long maxExecTimeSec    = 0;
			long sumProcessCount   = 0;
			long sumFailCount      = 0;
			long sumFailCount_547  = 0;
			long sumFailCount_1205 = 0;
			long sumFuccessCount   = 0;
			for (DbmsWorker w : workerList)
			{
				long execTimeSec = (w._endTime - w._startTime) / 1000;
				long opPerSec    = w._processCount / execTimeSec;
				
				maxExecTimeSec    =  Math.max(maxExecTimeSec, execTimeSec);
				sumProcessCount   += w._processCount;
				sumFailCount      += w._failCount;
				sumFailCount_547  += w._failCount_547;
				sumFailCount_1205 += w._failCount_1205;
				sumFuccessCount   += w._successCount;
				
				System.out.println("STATS: Worker[" + w._workerId + "]: "
						+ "execTimeSec="        + execTimeSec 
						+ ", opPerSec="         + opPerSec 
						+ ", processCount = "   + w._processCount 
						+ ", failCount = "      + w._failCount 
						+ ", failCount_547 = "  + w._failCount_547 
						+ ", failCount_1205 = " + w._failCount_1205 
						+ ", successCount = "   + w._successCount 
						);
			}
			System.out.println("STATS: - ALL - "
					+ "maxExecTimeSec="     + maxExecTimeSec 
					+ ", opPerSec="         + sumProcessCount / maxExecTimeSec 
					+ ", processCount = "   + sumProcessCount 
					+ ", failCount = "      + sumFailCount 
					+ ", failCount_547 = "  + sumFailCount_547 
					+ ", failCount_1205 = " + sumFailCount_1205 
					+ ", successCount = "   + sumFuccessCount 
					);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	private static void setupDbms(String username, String password, String url, boolean doCleanup) throws SQLException
	{
		System.out.println("DBMS SETUP: connecting to URL: " + url);

		// Using autoclose... to close connection after try block
		try (Connection conn = DriverManager.getConnection(url, username, password))
		{
			// Just check that we ended up in the correct server/dbname
			String sql = "select @@spid, @@servername, db_name()";
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
				{
					int    spid    = rs.getInt(1);
					String srvname = rs.getString(2);
					String dbname  = rs.getString(3);

					System.out.println("DBMS SETUP: Connected as SPID=" + spid + ", to servername='" + srvname + "', dbname='" + dbname + "'.");
				}
			}

			// DO Cleanup
			if ( doCleanup )
			{
//				// truncate table:
//				sql = "TRUNCATE TABLE individual_external_id_mapping";
//				try (Statement stmnt = conn.createStatement())
//				{
//					System.out.println("DBMS SETUP: Executing: " + sql);
//					stmnt.executeUpdate(sql);
//				}

				// truncate table:
				sql = "TRUNCATE TABLE individual_id";
				try (Statement stmnt = conn.createStatement())
				{
					System.out.println("DBMS SETUP: Executing: " + sql);
					stmnt.executeUpdate(sql);
				}
			}

			// DO MORE STUFF IN HERE

			System.out.println("DBMS SETUP: ---DONE---");
		}
	}

	/**
	 * DataQueue
	 */
	private static class DbmsWorker implements Runnable
	{
		DataQueue   _dataQueue;
		Thread      _thread;
		Connection  _conn;

		int         _spid;

		private int _workerId;

		private long _processCount;
		private long _successCount;
		private long _failCount;
		private long _failCount_1205;
		private long _failCount_547;

		private long _startTime;
		private long _endTime;
		
		public DbmsWorker(DataQueue dataQueue, int workerId)
		{
			_dataQueue = dataQueue;
			_workerId = workerId;
		}

		public void connect(String user, String passwd, String url) throws SQLException
		{
			// System.out.println("DBMS Worker " + _workerId + " connecting to URL: " + url);
			_conn = DriverManager.getConnection(url, user, passwd);

			String sql = "select @@spid, @@servername, db_name()";
			try (Statement stmnt = _conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
				{
					int    spid    = rs.getInt(1);
					String srvname = rs.getString(2);
					String dbname  = rs.getString(3);

					_spid = spid;

					System.out.println("DBMS Worker " + _workerId + " Connected as SPID=" + spid + ", to servername='" + srvname + "', dbname='" + dbname + "'.");
				}
			}
		}

		public void startWorker()
		{
			_thread = new Thread(this);
			_thread.setName("DbmsWorker-" + _workerId);
			_thread.setDaemon(false);

			_thread.start();
		}

		@Override
		public void run()
		{
			System.out.println("Starting thread: " + _thread.getName());
			_startTime = System.currentTimeMillis();
		
			try
			{
				int row = 0;

				while (true)
				{
					DataRecord datRecord = _dataQueue.getRecord(row);
					if ( datRecord == null )
						break;

					// Wait for all threads to process data at the same time
					if (waitForBarier)
					{
//						try { barrier.await(); }
//						catch (Exception e) { e.printStackTrace(); }
					}

					doWork(datRecord);

					row++;
					if (row % 10_000 == 0)
					{
						System.out.println(">>> DBMS Worker[wid=" + _workerId + ", spid=" + _spid + "]: Has processed " + row + " entries.");
					}
				}

				try { _conn.close(); }
				catch(SQLException ex) {}
			}
			catch(Throwable t)
			{
				t.printStackTrace();
			}

			_endTime = System.currentTimeMillis();
			System.out.println("Ending thread: " + _thread.getName());
		}

		// ---------------------------------------------
		// DO THE SQL WORK IN HERE
		// ---------------------------------------------
		private void doWork(DataRecord dataRecord)
		{
			// for now just do "select" on what we have in the queue, to chat
			// that "something" works...
			
			try
			{
				_processCount++;
				
//				doWork_dummy_justDoSelect(dataRecord);
//				doWork_insert(dataRecord);
				doWork_insertPreparedStatement(dataRecord);

				_successCount++;
			}
			catch(SQLException ex)
			{
				_failCount++;
				if (547  == ex.getErrorCode()) _failCount_547++;
				if (1205 == ex.getErrorCode()) _failCount_1205++;
			}
		}

		private void doWork_insertPreparedStatement(DataRecord dataRecord)
		throws SQLException
		{
			String id          = UUID.randomUUID().toString();
			String indiviualId = UUID.randomUUID().toString();

			String sql = ""
			        + "INSERT INTO dbo.individual_id     \n "
			        + (useTabLockHint ? "WITH (TABLOCKX) \n " : "")        /// LOCK THE WHOLE TABLE WHILE INSERT
			        + "(                                 \n"
			        + "    individual_id,                \n "
			        + "    pml_id,                       \n "
			        + "    id,                           \n "
			        + "    mapping_version,              \n "
			        + "    created,                      \n "
			        + "    modified,                     \n "
			        + "    valid_from,                   \n "
			        + "    valid_to,                     \n "
			        + "    personal_identity_number      \n "
			        + " )                                \n "
			        + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?) \n "
			;

			try
			{
				try (PreparedStatement pstmnt = _conn.prepareStatement(sql))
				{
//					System.out.println(">>> DBMS Worker[wid=" + _workerId + ", spid=" + _spid + "]: xID=|" + dataRecord._xID + "|, personnummer=|" + dataRecord._personnummer + "|.");

					Timestamp ts = new Timestamp(System.currentTimeMillis());
					
					pstmnt.setString   (1, indiviualId);              // individual_id
					pstmnt.setInt      (2, dataRecord._xID);          // pml_id
					pstmnt.setString   (3, id);                       // id
					pstmnt.setInt      (4, 0);                        // mapping_version
					pstmnt.setTimestamp(5, ts);                       // created
					pstmnt.setString   (6, null);                     // modified
					pstmnt.setTimestamp(7, ts);                       // valid_from
					pstmnt.setString   (8, null);                     // valid_to
					pstmnt.setString   (9, dataRecord._personnummer); // personal_identity_number
					
					pstmnt.executeUpdate();

//					System.out.println("<<< DBMS Worker[wid=" + _workerId + ", spid=" + _spid + "]: xID=|" + dataRecord._xID + "|, personnummer=|" + dataRecord._personnummer + "|.");
				}
			}
			catch (SQLException ex)
			{
				if (ex.getErrorCode() == 547 && hideError_547)
					/* do not print */;
				else if (ex.getErrorCode() == 548 && hideError_547)  // In Sybase ASE it's 548
					/* do not print */;
				else if (ex.getErrorCode() == 1205)
					System.out.println("******* DEADLOCK ****** DBMS Worker[wid=" + _workerId + ", spid=" + _spid + "]: xID=|" + dataRecord._xID + "|, personnummer=|" + dataRecord._personnummer + "| ERROR: " +  ex.getErrorCode() + " -- " + ex.getMessage().trim() + ".");
				else
					System.out.println("                    *** DBMS Worker[wid=" + _workerId + ", spid=" + _spid + "]: xID=|" + dataRecord._xID + "|, personnummer=|" + dataRecord._personnummer + "| ERROR: " +  ex.getErrorCode() + " -- " + ex.getMessage().trim() + ".");
				throw ex;
			}
		}
		
		private void doWork_insert(DataRecord dataRecord)
		throws SQLException
		{
			String id          = UUID.randomUUID().toString();
			String indiviualId = UUID.randomUUID().toString();

			String sql = ""
			        + "INSERT INTO dbo.individual_id     \n "
			        + (useTabLockHint ? "WITH (TABLOCKX) \n " : "")        /// LOCK THE WHOLE TABLE WHILE INSERT
			        + "(                                 \n"
			        + "    individual_id,                \n "
			        + "    pml_id,                       \n "
			        + "    id,                           \n "
			        + "    mapping_version,              \n "
			        + "    created,                      \n "
			        + "    modified,                     \n "
			        + "    valid_from,                   \n "
			        + "    valid_to,                     \n "
			        + "    personal_identity_number      \n "
			        + " )                                \n "
			        + "VALUES                            \n "
			        + "(                                 \n"
			        + "    '" + indiviualId     + "',    \n "
			        + "    "  + dataRecord._xID + ",     \n "
			        + "    '" + id              + "',    \n "
			        + "    0,                            \n "
			        + "    '2025-02-24 13:21:10.2850000',\n "
			        + "    NULL,                         \n "
			        + "    '2025-02-24 13:21:10.2850000',\n "
			        + "    NULL,                         \n "
			        + "    '" + dataRecord._personnummer + "' \n "
			        + ");                                \n "
			;

			try
			{
				try (Statement stmnt = _conn.createStatement())
				{
//					System.out.println(">>> DBMS Worker[wid=" + _workerId + ", spid=" + _spid + "]: xID=|" + dataRecord._xID + "|, personnummer=|" + dataRecord._personnummer + "|.");

					stmnt.executeUpdate(sql);

//					System.out.println("<<< DBMS Worker[wid=" + _workerId + ", spid=" + _spid + "]: xID=|" + dataRecord._xID + "|, personnummer=|" + dataRecord._personnummer + "|.");
				}
			}
			catch (SQLException ex)
			{
				if (ex.getErrorCode() == 547 && hideError_547)
					/* do not print */;
				else if (ex.getErrorCode() == 548 && hideError_547)  // In Sybase ASE it's 548
					/* do not print */;
				else if (ex.getErrorCode() == 1205)
					System.out.println("******* DEADLOCK ****** DBMS Worker[wid=" + _workerId + ", spid=" + _spid + "]: xID=|" + dataRecord._xID + "|, personnummer=|" + dataRecord._personnummer + "| ERROR: " +  ex.getErrorCode() + " -- " + ex.getMessage().trim() + ".");
				else
					System.out.println("                    *** DBMS Worker[wid=" + _workerId + ", spid=" + _spid + "]: xID=|" + dataRecord._xID + "|, personnummer=|" + dataRecord._personnummer + "| ERROR: " +  ex.getErrorCode() + " -- " + ex.getMessage().trim() + ".");
				throw ex;
			}
		}
		
		private void doWork_dummy_justDoSelect(DataRecord dataRecord)
		throws SQLException
		{
			String sql = "select xID='" + dataRecord._xID + "', personnummer='" + dataRecord._personnummer + "' \n"
			// + "waitfor delay '00:00:01'"
			;

			try
			{
				try (Statement stmnt = _conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
				{
					while (rs.next())
					{
						String xID          = rs.getString(1);
						String personnummer = rs.getString(2);

						System.out.println("DBMS Worker[wid=" + _workerId + ", spid=" + _spid + "]: xID=|" + xID + "|, personnummer=|" + personnummer + "|.");
					}
				}
			}
			catch (SQLException ex)
			{
				ex.printStackTrace();
				throw ex;
			}
		}
	}

	/**
	 * DataQueue
	 */
	private static class DataQueue
	{

		ArrayList<DataRecord> _dataRecords;

		public void readFile(String file)
		{
			_dataRecords = new ArrayList<>();

			try
			{
				List<String> allLines = Files.readAllLines(Paths.get(file));

				int row = -1;
				for (String line : allLines)
				{
					row++;

					// Skip header
					if ( row == 0 )
						continue;

					String[] sa = line.split(",");
					if ( sa.length <= 3 )
					{
						String xidStr = sa[0];
						String pnrStr = sa[1].replace("\"", "").trim();

						if ( pnrStr.length() < 12 )
						{
							System.out.println("SKIPPING PNR: |" + pnrStr + "|... to short");
							continue;
						}

						int xid = 0;
						try
						{
							xid = Integer.parseInt(xidStr);
						}
						catch (NumberFormatException nfe)
						{
							System.out.println("SKIPPING XID: |" + xidStr + "|... not a number");
						}

						_dataRecords.add(new DataRecord(xid, pnrStr, 0));
					}
					else
					{
						System.out.println("SKIPPING LINE: |" + line + "|");
					}
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

			System.out.println("DataQueue: added " + _dataRecords.size() + " records.");
		}

		public synchronized DataRecord getRecord(int i)
		{
			if ( _dataRecords.isEmpty() )
				return null;
			
			if ( i >= _dataRecords.size())
				return null;

			DataRecord entry = null;

			if (purgeQueueRecord)
			{
				entry = _dataRecords.get(0);
				_dataRecords.remove(0);
			}
			else
			{
				entry = _dataRecords.get(i);
			}

			
			return entry;
		}

	}

	/**
	 * DataRecord
	 */
	private static class DataRecord
	{
		int    _xID;
		String _personnummer;
		int    _status;

		public DataRecord(int xId, String personnummer, int status)
		{
			_xID = xId;
			_personnummer = personnummer;
			_status = status;
		}
	}

}
