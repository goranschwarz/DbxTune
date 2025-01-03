package com.dbxtune.test;

import java.io.FileReader;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.sybase.jdbcx.EedInfo;

public class SixSybHelper
{

//	private static int      MAX_NO_OF_RETRIES = 5;
//	private static long     DEADLOCKSLEEPTIME = 5000;

//	public static final int DEAD_LOCK         = 1205;
//	public static final int NOT_EXISTS        = 2812;
//	public static final int INVALID_OPERATOR  = 403;
//	public static final int INSERT_NULL       = 515;
//	public static final int AVOID_SQLE        = 20001;

	public static Connection getDbConnection(Properties dbprops)
	{
		Connection sybcon = null;

		try
		{

			// Attempt to connect to a driver.
			String server   = dbprops.getProperty("server");
			String port     = dbprops.getProperty("port");
			String database = dbprops.getProperty("database");

			String URL;
			if ( server != null && database != null )
			{
				URL = "jdbc:sybase:Tds:" + server + ":" + port + "/" + database;
			}
			else
			{
				URL = System.getenv("FINBAS_URL");
			}

			String user     = dbprops.getProperty("user");
			String password = dbprops.getProperty("password");

			if ( user == null && password == null )
			{
				user = System.getenv("SYBUSERNAME");
				if ( user != null )
				{
					dbprops.setProperty("user", user);
				}
				password = System.getenv("SYBPASSWD");
				if ( password != null )
				{
					dbprops.setProperty("password", password);
				}
			}
			if ( URL != null && URL.length() > 0 )
			{

				URL = URL.replace("::", ":");
				System.out.println("->URL = " + URL);
				System.out.println("->DB Properties = " + dbprops);
				sybcon = DriverManager.getConnection(URL, dbprops); // establish
				                                                    // connection
				return sybcon;
			}
			else
			{
//				logger.error("Create connection to database failed! No URL.");
				System.out.println("ERROR: Create connection to database failed! No URL.");
				return null;
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		return sybcon;

	}

	/**
	 * Get a message string that looks like the one got from Sybase 'isql' utility
	 * @param sqe a SQLException object
	 * @return A String, even if no SQLException is present, return a empty string.
	 */
	public static String getSqlMsgs(SQLException sqe)
	{
		if (sqe == null)
			return "";

		StringBuilder sb = new StringBuilder();
		while (sqe != null)
		{
			if(sqe instanceof EedInfo)
			{
				// Error is using the additional TDS error data.
				EedInfo eedi = (EedInfo) sqe;
				if(eedi.getSeverity() > 10)
				{
					boolean firstOnLine = true;
					sb.append("Msg " + sqe.getErrorCode() +
							", Level " + eedi.getSeverity() + ", State " +
							eedi.getState() + ":\n");

					if( eedi.getServerName() != null)
					{
						sb.append("Server '" + eedi.getServerName() + "'");
						firstOnLine = false;
					}
					if(eedi.getProcedureName() != null)
	                {
						sb.append( (firstOnLine ? "" : ", ") +
								"Procedure '" + eedi.getProcedureName() + "'");
						firstOnLine = false;
	                }
					sb.append( (firstOnLine ? "" : ", ") +
							"Line " + eedi.getLineNumber() +
							", Status " + eedi.getStatus() + 
							", TranState " + eedi.getTranState() + ":\n");
				}
				// Now "print" the error or warning
				String msg = sqe.getMessage();
				sb.append(msg);
				if (msg != null && !msg.endsWith("\n") )
					sb.append("\n");
			}
			else
			{
				// SqlState: 010P4 java.sql.SQLWarning: 010P4: An output parameter was received and ignored.
				if ( ! sqe.getSQLState().equals("010P4") )
				{
					sb.append("Unexpected exception : " +
							"SqlState: " + sqe.getSQLState()  +
							" " + sqe.toString() +
							", ErrorCode: " + sqe.getErrorCode() + "\n");
				}
			}
			sqe = sqe.getNextException();
		}
		return sb.toString();
	}

	public static ResultSet executeQueryWithRetry(CallableStatement cstmt) throws SQLException
	{
		int       MAX_NO_OF_RETRIES = 5;
		long      DEADLOCKSLEEPTIME = 5000;
		int       execCount         = 0;

		for(int dummy=0; dummy<100; dummy++) // This instead of while(true)... because "endless" loops might sooner or later be re-coded and introduce a bug 
		{
			execCount++;
			try
			{
				return cstmt.executeQuery();
			}
			catch (SQLException originEx)
			{
				boolean isDeadlock = false;

				// catch deadlocks
				SQLException e = originEx;
				while (e != null)
				{
					if ( e.getErrorCode() == 1205 )
					{
						isDeadlock = true;

						if (execCount > MAX_NO_OF_RETRIES)
							throw new SQLException("Caught DEADLOCK after MAX_NO_OF_RETRIES=" + MAX_NO_OF_RETRIES + " the Statement was ABORTED, by: " + getSqlMsgs(originEx),
									e.getSQLState(),
									e.getErrorCode(),
									e);

						String threadName = Thread.currentThread().getName();
						System.out.println(threadName + " >> INFO: Caught DEADLOCK, at execCount=" + execCount + ", which will be retried. " + getSqlMsgs(originEx) );

						try { Thread.sleep(DEADLOCKSLEEPTIME); }
						catch(InterruptedException ignore) {}

						System.out.println(threadName + " >> INFO: victim of deadlock, sleeped for " + DEADLOCKSLEEPTIME + " millis rerun no " + execCount);
					}
					e = e.getNextException();
				}

				if ( isDeadlock == false )
				{
					// Print out ALL messages (but now warnings)
					System.out.println( getSqlMsgs(originEx) );
					throw originEx;
//					throw new Exception("Not Deadlock Exception", e);
				}
			}
		}

		return null;
	}

	public static ResultSet executeWithRetryGoransOnlyFirstRs(CallableStatement cstmt) throws SQLException
	{
		int       MAX_NO_OF_RETRIES = 5;
		long      DEADLOCKSLEEPTIME = 5000;
		boolean   deadlockflag      = true;
		int       i                 = 0;

		while (i++ < MAX_NO_OF_RETRIES && deadlockflag)
		{
			
			deadlockflag = false;

			try
			{
				boolean hasRs = cstmt.execute();
				int rowsAffected = 0;

				// iterate through each result set
				do
				{
					if(hasRs)
					{
						// Get next ResultSet to work with
						ResultSet rs = cstmt.getResultSet();
						
//						ResultSetTableModel tm = new ResultSetTableModel(rs, sql);
//						rsList.add(tm);
//
//						// Close it
//						rs.close();
						
						// Simply RETURN on FIRST found ResultSet
						return rs;
					}
					else
					{
						rowsAffected = cstmt.getUpdateCount();
						if (rowsAffected >= 0)
						{
						}
					}

					// Check if we have more ResultSets
					hasRs = cstmt.getMoreResults();
				}
				while (hasRs || rowsAffected != -1);
				
			}
			catch (SQLException originEx)
			{
				// catch deadlocks
				SQLException e = originEx;
				while (e != null)
				{
					if ( e.getErrorCode() == 1205 )
					{
						try { Thread.sleep(DEADLOCKSLEEPTIME); }
						catch(InterruptedException ignore) {}

						System.out.println("INFO: victim of deadlock, sleeped for " + DEADLOCKSLEEPTIME + " millis rerun no " + i);
						deadlockflag = true;
					}
					e = e.getNextException();
				}

				if ( deadlockflag == false )
				{
					// Print out ALL messages (but now warnings)
					System.out.println( getSqlMsgs(originEx) );
					throw originEx;
//					throw new Exception("Not Deadlock Exception", e);
				}
			}
		}

		return null;
	}

	/**
	 * Callback to handle any ResultSet 
	 */
	public interface ResultSetCallback
	{
		/**
		 * Handle ResultSet
		 * 
		 * @param rsNum   The <i>id</i> of the ResultSet, starting at 1
		 * @param rs      The ResultSet to read from
		 * @return        true: The ResultSet was read... false: ResultSet was not read by the method (the "base implementation" executeWithRetryAndRsCallback will read and throw away the rows then close the ResultSet)
		 * @throws SQLException
		 */
		boolean handleResultSet(int rsNum, ResultSet rs) throws SQLException;
	}

//	public static void executeWithRetryAndRsCallback(CallableStatement cstmt, ResultSetCallback rsc) throws SQLException
//	{
//		int       MAX_NO_OF_RETRIES = 5;
//		long      DEADLOCKSLEEPTIME = 5000;
//		boolean   deadlockflag      = true;
//		int       i                 = 0;
//
//		// FIXME: replace the deadlock retry in the same manner as we do in: executeQueryWithRetry
//		while (i++ < MAX_NO_OF_RETRIES && deadlockflag)
//		{
//			
//			deadlockflag = false;
//
//			try
//			{
//				boolean hasRs = cstmt.execute();
//				int rowsAffected = 0;
//				int rsNum = 0;
//				int updateCountNum = 0;
//
//				// iterate through each result set
//				do
//				{
//					if(hasRs)
//					{
//						rsNum++;
//						
//						// Get next ResultSet to work with
//						ResultSet rs = cstmt.getResultSet();
//
//						// use callback to read ResultSet
//						boolean wasReadByTheCallback = rsc.handleResultSet(rsNum, rs);
//
//						if ( ! wasReadByTheCallback )
//						{
//							// Just call next to "read-out" the Records -- Throw away the rows...
//							while (rs.next())
//								;
//						}
//
//						// Close it
//						try {
//							rs.close();
//						} catch (SQLException ignore) {
//							// Ignore any errors on close (because it may already have been closed by the implementor of the callback!
//						}
//					}
//					else
//					{
//						rowsAffected = cstmt.getUpdateCount();
//						if (rowsAffected >= 0)
//						{
//							updateCountNum++;
//							// POSSIBLY: call a callback... with number of rows affected by an update/delete
//							// rsc.handleUpdateCount(rsNum, upddateCountNum, rowsAffected);
//						}
//					}
//
//					// Check if we have more ResultSets
//					hasRs = cstmt.getMoreResults();
//				}
//				while (hasRs || rowsAffected != -1);
//				
//			}
//			catch (SQLException originEx)
//			{
//				// catch deadlocks
//				SQLException e = originEx;
//				while (e != null)
//				{
//					if ( e.getErrorCode() == 1205 )
//					{
//						try { Thread.sleep(DEADLOCKSLEEPTIME); }
//						catch(InterruptedException ignore) {}
//
//						System.out.println("INFO: victim of deadlock, sleeped for " + DEADLOCKSLEEPTIME + " millis rerun no " + i);
//						deadlockflag = true;
//					}
//					e = e.getNextException();
//				}
//
//				if ( deadlockflag == false )
//				{
//					// Print out ALL messages (but now warnings)
//					System.out.println( getSqlMsgs(originEx) );
//					throw originEx;
////					throw new Exception("Not Deadlock Exception", e);
//				}
//			}
//		}
//	}
	public static void executeWithRetryAndRsCallback(CallableStatement cstmt, ResultSetCallback rsc) throws SQLException
	{
		int       MAX_NO_OF_RETRIES = 5;
		long      DEADLOCKSLEEPTIME = 5000;
		int       execCount         = 0;

		for(int dummy=0; dummy<100; dummy++) // This instead of while(true)... because "endless" loops might sooner or later be re-coded and introduce a bug 
		{
			execCount++;
			try
			{
				boolean hasRs = cstmt.execute();
				int rowsAffected = 0;
				int rsNum = 0;
				int updateCountNum = 0;

				// iterate through each result set
				do
				{
					if(hasRs)
					{
						rsNum++;
						
						// Get next ResultSet to work with
						ResultSet rs = cstmt.getResultSet();

						// use callback to read ResultSet
						boolean wasReadByTheCallback = rsc.handleResultSet(rsNum, rs);

						if ( ! wasReadByTheCallback )
						{
							// Just call next to "read-out" the Records -- Throw away the rows...
							while (rs.next())
								;
						}

						// Close it
						try {
							rs.close();
						} catch (SQLException ignore) {
							// Ignore any errors on close (because it may already have been closed by the implementor of the callback!
						}
					}
					else
					{
						rowsAffected = cstmt.getUpdateCount();
						if (rowsAffected >= 0)
						{
							updateCountNum++;
							// POSSIBLY: call a callback... with number of rows affected by an update/delete
							// rsc.handleUpdateCount(rsNum, upddateCountNum, rowsAffected);
						}
					}

					// Check if we have more ResultSets
					hasRs = cstmt.getMoreResults();
				}
				while (hasRs || rowsAffected != -1);
				
				// NORMAL Exit point
				// if we didn't have any errors.. simply get out of the method
				return;
			}
			catch (SQLException originEx)
			{
				boolean isDeadlock = false;

				// catch deadlocks
				SQLException e = originEx;
				while (e != null)
				{
					if ( e.getErrorCode() == 1205 )
					{
						isDeadlock = true;

						if (execCount > MAX_NO_OF_RETRIES)
							throw new SQLException("Caught DEADLOCK after MAX_NO_OF_RETRIES=" + MAX_NO_OF_RETRIES + " the Statement was ABORTED, by: " + getSqlMsgs(originEx),
									e.getSQLState(),
									e.getErrorCode(),
									e);

						String threadName = Thread.currentThread().getName();
						System.out.println(threadName + " >> INFO: Caught DEADLOCK, at execCount=" + execCount + ", which will be retried. " + getSqlMsgs(originEx) );

						try { Thread.sleep(DEADLOCKSLEEPTIME); }
						catch(InterruptedException ignore) {}

						System.out.println(threadName + " >> INFO: victim of deadlock, sleeped for " + DEADLOCKSLEEPTIME + " millis rerun no " + execCount);
					}
					e = e.getNextException();
				}

				if ( isDeadlock == false )
				{
					// Print out ALL messages (but now warnings)
					System.out.println( getSqlMsgs(originEx) );
					throw originEx;
//					throw new Exception("Not Deadlock Exception", e);
				}
			}
		}
	}
	
	
	public static ResultSet executeWithRetry(CallableStatement cstmt) throws Exception
	{

		int       MAX_NO_OF_RETRIES = 5;
		long      DEADLOCKSLEEPTIME = 5000;
		boolean   res               = false;
		boolean   allreadyfoundrs   = false;
		ResultSet rs                = null;
		int       i                 = 0;
		boolean   deadlockflag      = true;
		while (i++ < MAX_NO_OF_RETRIES && deadlockflag)
		{

			deadlockflag = false;

			try
			{

				res = cstmt.execute();
				int updatecount = 0;
				if ( res )
				{
					rs = cstmt.getResultSet();
					allreadyfoundrs = true;
				}
				else
				{
					updatecount = cstmt.getUpdateCount();
					// logger.debug("Update count = " + updatecount);
				}
				while ((res = cstmt.getMoreResults(Statement.KEEP_CURRENT_RESULT)) == true || (updatecount = cstmt.getUpdateCount()) != -1)
				{
					if ( res )
					{
						if ( allreadyfoundrs )
						{
//							logger.warn("There is another resultset!");
							System.out.println("WARNING: There is another resultset!");
						}
						rs = cstmt.getResultSet();
						allreadyfoundrs = true;
					}
					else
					{
						// logger.debug("Update count = " + updatecount);
					}
				}

				if ( rs != null )
				{
					return rs;

				}
				else
				{
					System.out.println("ResultSet is null");
					return null;
				}

			}
			catch (SQLException e)
			{
				// catch deadlocks
				while (e != null)
				{
					if ( e.getErrorCode() == 1205 )
					{
						Thread.sleep(DEADLOCKSLEEPTIME);
						System.out.println("INFO: victim of deadlock, sleeped for " + DEADLOCKSLEEPTIME + " millis rerun no " + i);
						deadlockflag = true;
					}
					else
					{
//						logger.error("ERROR: Not DeadlockException " + e);
						System.out.println("ERROR: Not DeadlockException " + e);
					}
					e = e.getNextException();
				}
				if ( deadlockflag == false )
				{
					throw new Exception("Not Deadlock Exception");
				}
			}
		}
		if ( deadlockflag )
		{
			throw new Exception("5 consecutive deadlocks");
		}
		return null;
	}

//	public static Integer doAutoRetryingDBUpdate(PreparedStatement statement, Logger mylog) throws SQLException
//	{
//		int nRetries = MAX_NO_OF_RETRIES;
//		int res      = 0;
//		while (nRetries > 0)
//		{
//			res = statement.executeUpdate();
//
//			if ( !isDeadlocked(statement.getWarnings(), mylog) )
//				return res;
//			else
//			{
//				nRetries--;
//				mylog.debug("Encountered deadlock when processing statement: " + statement.toString() + " Retries left: " + nRetries);
//			}
//		}
//		mylog.warn("Deadlock could not be resolved, returning null.");
//		return null;
//	}

//	private static boolean isDeadlocked(SQLWarning warnings, Logger log)
//	{
//		while (warnings != null)
//		{
//			if ( warnings.getErrorCode() == DEAD_LOCK )
//				return true;
//			else
//				log.error("Sybase error code " + warnings.getErrorCode() + " Message: " + warnings.getMessage());
//			warnings = warnings.getNextWarning();
//		}
//		return false;
//	}

	public static void main(String[] args)
	{
		System.out.println("Usage: progname [propsFile]");
		System.out.println("args.length = " + args.length);
		
		// Read Properties from input Properties file, or create basic test entries
		Properties prop = new Properties();
		if (args.length >= 1)
		{
			try
			{
				String filename = args[0];
				System.out.println("Loading properties file '" + filename + "'.");
				prop.load(new FileReader(filename));

				System.out.println("Properties file '" + filename + "' contains the following properties.");
				prop.list(System.out);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				return;
			}
		}
		else
		{
			prop.setProperty("server",   "192.168.0.110");
			prop.setProperty("port",     "1600");
			prop.setProperty("database", "tempdb");
			prop.setProperty("user",     "sa");
			prop.setProperty("password", "sybase");

			System.out.println("Using DEFAULT Properties, which contains the following:");
			prop.list(System.out);
		}

		try
		{
			// Read some properties
			boolean tryDeadlock            = prop.getProperty("tryDeadlock"           , "true") .trim().equalsIgnoreCase("true"); 
			boolean exitAfterTryDeadlock   = prop.getProperty("exitAfterTryDeadlock"  , "false").trim().equalsIgnoreCase("true");
			boolean tryDeadlockWithSixCode = prop.getProperty("tryDeadlockWithSixCode", "false").trim().equalsIgnoreCase("true"); 

			boolean useOrigin            = prop.getProperty("useOrigin"   , "true") .trim().equalsIgnoreCase("true");
			boolean useGorans            = prop.getProperty("useGorans"   , "true") .trim().equalsIgnoreCase("true");;
			boolean useCallback          = prop.getProperty("useCallback" , "true") .trim().equalsIgnoreCase("true");;
			boolean useExecQuery         = prop.getProperty("useExecQuery", "true") .trim().equalsIgnoreCase("true");;

			if (tryDeadlock)
			{
				int numOfThreads  = 5;
				int numOfLoopsPerThread = 10;

				int expectedIdAtEnd = numOfThreads * numOfLoopsPerThread;

				List<DeadlockTester> list = new ArrayList<>();

				System.out.println("START - DEADLOCK TEST: ");

				// Create test objects
				for (int i = 0; i < numOfThreads; i++)
					list.add( new DeadlockTester(prop, "T"+(i+1), numOfLoopsPerThread, tryDeadlockWithSixCode) );

				// Connect
				for (DeadlockTester dlt : list)
					dlt.connect();

				// Setup using first entry in list
				list.get(0).setup();
				
				// Start
				for (DeadlockTester dlt : list)
					dlt.start();

				// Wait for all to complete
				for (DeadlockTester dlt : list)
					dlt.join();

				// Close connections
				for (DeadlockTester dlt : list)
					dlt.close();

				// get MAX id that was generated
				int maxId = -1;
				for (DeadlockTester dlt : list)
					maxId = Math.max(maxId, dlt.getLastId());
				System.out.println("MAX ID: " + maxId + ", Test - " + (expectedIdAtEnd == maxId ? "OK" : "FAILED, expected maxId to be " + expectedIdAtEnd) );

				System.out.println("END - DEADLOCK TEST: ");

				if (exitAfterTryDeadlock)
					return;
			}

			Connection conn = getDbConnection(prop);

			System.out.println("START - SPID: " + getDbmsSpid(conn));

			System.out.println("## Creating proc: gsDummy1");
			createDummyProc(conn);
			
			int procParam   = 1024 - 1; // ALL ResultSets
			int doRaiserror = 0;
//			procParam = 1; // only SystemTables
//			procParam = 2; // only UserTables
//			procParam = 4; // only Procedures
//			procParam = 3; // SystemTables and UserTables
//			procParam = 512; // only raiserror
//			doRaiserror = 1;

			procParam   = Integer.parseInt( prop.getProperty("procParam", "1023") );
			doRaiserror = prop.getProperty("doRaiserror", "true").trim().equalsIgnoreCase("true") ? 1 : 0;

			System.out.println("## Calling proc: gsDummy1, withParameter=" + procParam + ", doRaiserror=" + doRaiserror);
			CallableStatement cstmnt = conn.prepareCall("{call gsDummy1(?, ?)}");
			cstmnt.setInt(1, procParam);
			cstmnt.setInt(2, doRaiserror);

			
			if (useOrigin)
			{
				try
				{
					System.out.println("");
					System.out.println("============================================================");
					System.out.println("## Using ORIGIN --- PLAIN - ResultSet -- Only reads LAST RS, do NOT read RS on raiserror...");
					
					ResultSet rs = executeWithRetry(cstmnt);
					if (rs != null)
					{
						System.out.println("## reading ResultSet...");

						int rowc = 0;
						while(rs.next())
						{
							rowc++;
							int    id   = rs.getInt   (1);
							String name = rs.getString(2);
							String type = rs.getString(3);

							System.out.println("  - RS: row=" + rowc + ", id=" + id + ", type='" + type + "', name='" + name + "'.");
						}
					}
					else
					{
						System.out.println("## ResultSet was null --- no ResultSet ---");
					}
				}
				catch(Exception ex)
				{
					System.out.println("## ORIGIN ##############################################");
					ex.printStackTrace();
					System.out.println("########################################################");
				}
			}

			if (useGorans)
			{
				try
				{
					System.out.println("");
					System.out.println("============================================================");
					System.out.println("## Using GORANS --- PLAIN - ResultSet -- Only Reads FIRST RS, do NOT read raiserror");
					
//					ResultSet rs = executeWithRetry(cstmnt);
					ResultSet rs = executeWithRetryGoransOnlyFirstRs(cstmnt);
					if (rs != null)
					{
						System.out.println("## reading ResultSet...");

						int rowc = 0;
						while(rs.next())
						{
							rowc++;
							int    id   = rs.getInt   (1);
							String name = rs.getString(2);
							String type = rs.getString(3);

							System.out.println("  - RS: row=" + rowc + ", id=" + id + ", type='" + type + "', name='" + name + "'.");
						}
					}
					else
					{
						System.out.println("## ResultSet was null --- no ResultSet ---");
					}
				}
				catch(SQLException ex)
				{
					System.out.println("## GORANS ##############################################");
					ex.printStackTrace();
					System.out.println("########################################################");
				}
			}

			if (useCallback)
			{
				try
				{
					System.out.println("");
					System.out.println("============================================================");
					System.out.println("## Using --- CALLBACK - ResultSet -- Reads ALL ResultSets and raiserror");
					
					executeWithRetryAndRsCallback(cstmnt, new ResultSetCallback()
					{
						@Override
						public boolean handleResultSet(int rsNum, ResultSet rs) throws SQLException
						{
							System.out.println("## reading ResultSet, NUMBER: " + rsNum);

							int rowc = 0;
							while(rs.next())
							{
								rowc++;
								int    id   = rs.getInt   (1);
								String name = rs.getString(2);
								String type = rs.getString(3);

								System.out.println("  - RS: row=" + rowc + ", id=" + id + ", type='" + type + "', name='" + name + "'.");
							}
							
							return true;
						}
					});
				}
				catch(SQLException ex)
				{
					System.out.println("## CALLBACK ############################################");
					ex.printStackTrace();
					System.out.println("########################################################");
				}
			}

			if (useExecQuery)
			{
				try
				{
					System.out.println("");
					System.out.println("============================================================");
					System.out.println("## Using EXEC-QUERY --- PLAIN - ResultSet -- Only Reads FIRST RS, do NOT read raiserror");
					
					ResultSet rs = executeQueryWithRetry(cstmnt);
					if (rs != null)
					{
						System.out.println("## reading ResultSet...");

						int rowc = 0;
						while(rs.next())
						{
							rowc++;
							int    id   = rs.getInt   (1);
							String name = rs.getString(2);
							String type = rs.getString(3);

							System.out.println("  - RS: row=" + rowc + ", id=" + id + ", type='" + type + "', name='" + name + "'.");
						}
					}
					else
					{
						System.out.println("## ResultSet was null --- no ResultSet ---");
					}
				}
				catch(SQLException ex)
				{
					System.out.println("## EXEC-QUERY ##########################################");
					ex.printStackTrace();
					System.out.println("########################################################");
				}
			}

			// Just to check that we can still "talk" to the DBMS
			System.out.println("END - SPID: " + getDbmsSpid(conn));
			
			conn.close();
			System.out.println("---END-CONNECTION-CLOSED--");
		}
		catch (Exception ex)
		{
			System.out.println("## MAIN ################################################");
			ex.printStackTrace();
			System.out.println("########################################################");
		}
	}

	private static int getDbmsSpid(Connection conn)
	throws SQLException
	{
		int spid = -1;
		String sql = "select @@spid";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
				spid = rs.getInt(1);
		}
		return spid;
	}

	private static void createDummyProc(Connection conn)
	throws SQLException
	{
		String sql;

		sql = "drop procedure gsDummy1";
		try (Statement stmnt = conn.createStatement())
		{
			stmnt.executeUpdate(sql);
		}
		catch (SQLException ignore)
		{
		}

		sql = ""
				+ "create procedure gsDummy1 \n"
				+ "( \n"
				+ "    @rs          int = null \n" // what ResultSet do you want to do: 1=S, 2=U, 4=P... 3=S,U, 7=S,U,P
				+ "   ,@doRaiserror int = 0    \n" // what ResultSet do you want to do: 1=S, 2=U, 4=P... 3=S,U, 7=S,U,P
				+ ") \n"
				+ "as \n"
				+ "begin \n"
				+ "    if (@rs is NULL) \n"
				+ "        set @rs = 1024 - 1\n"
				+ "\n"
				+ "    if (@rs & 1 = 1) \n"
				+ "        select id, name, type from sysobjects where type = 'S' \n" // System tables
				+ "\n"
				+ "    if (@rs & 2 = 2) \n"
				+ "        select id, name, type from sysobjects where type = 'U' \n" // User tables
				+ "\n"
				+ "    if (@rs & 4 = 4) \n"
				+ "        select id, name, type from sysobjects where type = 'P' \n" // Procedures
				+ "\n"
				+ "    if (@doRaiserror != 0) \n"
				+ "        raiserror 99999 'dummy raiserror from the proc' \n" // Procedures
				+ "end \n";

		try (Statement stmnt = conn.createStatement())
		{
			stmnt.executeUpdate(sql);
		}
	}

	private static class DeadlockTester
	extends Thread
	{
		private Connection _conn;
		private Properties _prop;
		private String     _name;
		private String     _originName;
		private int        _numOfExec;
		private boolean    _useSixOldCode;
		private int _lastId;
		
		public DeadlockTester(Properties prop, String name, int numOfExec, boolean useSixOldCode)
		{
			super(name);

			_prop          = prop;
			_name          = name + "[-]";
			_originName    = name;
			_numOfExec     = numOfExec;
			_useSixOldCode = useSixOldCode;
		}

		public void connect()
		throws SQLException
		{
			_conn = getDbConnection(_prop);
		}

		public void close()
		{
			if (_conn != null)
			{
				try { _conn.close(); }
				catch(SQLException ignore) {}
			}
		}

		public void setup()
		throws SQLException
		{
   			System.out.println(_name + ": -- Creating table: ");
   			createDeadlockTable();

   			System.out.println(_name + ": -- Creating proc: ");
   			createDeadlockProc();
		}

		public int getLastId()
		{
			return _lastId;
		}

		@Override
		public void run()
		{
			try
			{
				for (int i=0; i<_numOfExec; i++)
				{
					_name = _originName + "[" + (i+1) + "]";
					setName(_name);
					exec();
				}
			}
			catch(SQLException ex)
			{
				System.out.println(_name + ": Exeption...");
				ex.printStackTrace();
			}
		}
				
		
		public void exec()
		throws SQLException
		{
			System.out.println(_name + ": CONN - DeadlockTest - SPID: " + getDbmsSpid(_conn));

			CallableStatement cstmnt = _conn.prepareCall("{call gsDummyDeadLockProc}");

			try
			{
				System.out.println(_name + ": ");
				System.out.println(_name + ": ============================================================");
				System.out.println(_name + ": ## " + _name + " Using EXEC-QUERY... _useSixOldCode=" + _useSixOldCode);
				
				ResultSet rs = _useSixOldCode ? executeWithRetry(cstmnt) : executeQueryWithRetry(cstmnt);
				if (rs != null)
				{
					System.out.println(_name + ": ## reading ResultSet...");

					int rowc = 0;
					while(rs.next())
					{
						rowc++;
						int id = rs.getInt(1);
						
						_lastId = id;

						System.out.println(_name + ": >>>>>>>> RS - " + _name + " - RS: row=" + rowc + ", id=" + id + ".");
					}
				}
				else
				{
					System.out.println(_name + ": ## ResultSet was null --- no ResultSet ---");
				}
			}
//			catch(SQLException ex)
			catch(Exception ex)
			{
				System.out.println(_name + ": ## EXEC-QUERY ##########################################");
				ex.printStackTrace();
				System.out.println(_name + ": ########################################################");
			}
			
			System.out.println(_name + ": END - DeadlockTest - SPID: " + getDbmsSpid(_conn));
		}

		private void createDeadlockTable()
		throws SQLException
		{
			String sql;

			sql = "drop table gsDummyDeadLockTable";
			try (Statement stmnt = _conn.createStatement())
			{
				stmnt.executeUpdate(sql);
			}
			catch (SQLException ignore)
			{
			}

			sql = ""
					+ "create table gsDummyDeadLockTable \n"
					+ "( \n"
					+ "    id int not null\n"
					+ ") -- lock allpages\n"
					+ "";

			try (Statement stmnt = _conn.createStatement())
			{
				stmnt.executeUpdate(sql);
			}

			sql = "insert into gsDummyDeadLockTable values(0)";
			try (Statement stmnt = _conn.createStatement())
			{
				stmnt.executeUpdate(sql);
			}
		}

		private void createDeadlockProc()
		throws SQLException
		{
			String sql;

			sql = "drop procedure gsDummyDeadLockProc";
			try (Statement stmnt = _conn.createStatement())
			{
				stmnt.executeUpdate(sql);
			}
			catch (SQLException ignore)
			{
			}

			sql = ""
					+ "create procedure gsDummyDeadLockProc \n"
					+ "as \n"
					+ "begin \n"
					+ "    declare @id int \n"
					+ "\n"
					+ "    BEGIN TRAN \n"
					+ "\n"
					+ "    select @id = id + 1 from gsDummyDeadLockTable holdlock \n"
					+ "    waitfor delay '00:00:02' \n"
//					+ "    waitfor delay '00:00:00.100' \n"
					+ "    update gsDummyDeadLockTable set id = @id \n"
					+ "\n"
					+ "    COMMIT TRAN \n"
					+ "\n"
					+ "    select @id AS id \n"
					+ "    return 0 \n"
					+ "end \n";

			try (Statement stmnt = _conn.createStatement())
			{
				stmnt.executeUpdate(sql);
			}
		}
	}
}
