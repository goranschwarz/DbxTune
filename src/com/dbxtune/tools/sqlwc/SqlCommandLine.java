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
package com.dbxtune.tools.sqlwc;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import com.dbxtune.DebugOptions;
import com.dbxtune.NormalExitException;
import com.dbxtune.Version;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Debug;
import com.dbxtune.utils.JavaVersion;
import com.dbxtune.utils.StringUtil;

public class SqlCommandLine
{
	private static Logger _logger = Logger.getLogger(SqlCommandLine.class);

	
	public SqlCommandLine(CommandLine cmd)
	throws Exception
	{
		// -----------------------------------------------------------------
		// CHECK JAVA JVM VERSION
		// -----------------------------------------------------------------
		int javaVersionInt = JavaVersion.getVersion();
		if (   javaVersionInt != JavaVersion.VERSION_NOTFOUND 
		    && javaVersionInt <  JavaVersion.VERSION_7
		   )
		{
			System.out.println("");
			System.out.println("===============================================================");
			System.out.println(" "+Version.getAppName()+" needs a runtime Java 7 or higher.");
			System.out.println(" java.version = " + System.getProperty("java.version"));
			System.out.println(" which is parsed into the number: " + JavaVersion.getVersion());
			System.out.println("---------------------------------------------------------------");
			System.out.println("");
			throw new Exception(Version.getAppName()+" needs a runtime Java 7 or higher.");
		}

		//---------------------------------------------------------------
		// OK, lets get ASE user/passwd/server/dbname
		//---------------------------------------------------------------
		String aseUsername  = System.getProperty("user.name"); 
		String asePassword  = null;
		String aseServer    = System.getenv("DSQUERY");
		String aseDbname    = "";
		String jdbcUsername = System.getProperty("user.name");
		String jdbcPassword = null;
		String jdbcUrl      = "";
		String jdbcDriver   = "";
		String connProfile  = "";
		String sqlQuery     = "";
		String sqlFile      = "";
		if (cmd.hasOption('U'))	aseUsername  = cmd.getOptionValue('U');
		if (cmd.hasOption('P'))	asePassword  = cmd.getOptionValue('P');
		if (cmd.hasOption('S'))	aseServer    = cmd.getOptionValue('S');
		if (cmd.hasOption('D'))	aseDbname    = cmd.getOptionValue('D');
		if (cmd.hasOption('q'))	sqlQuery     = cmd.getOptionValue('q');
		if (cmd.hasOption('U'))	jdbcUsername = cmd.getOptionValue('U');
		if (cmd.hasOption('P'))	jdbcPassword = cmd.getOptionValue('P');
		if (cmd.hasOption('u'))	jdbcUrl      = cmd.getOptionValue('u');
		if (cmd.hasOption('d'))	jdbcDriver   = cmd.getOptionValue('d');
		if (cmd.hasOption('p'))	connProfile  = cmd.getOptionValue('p');
		if (cmd.hasOption('i'))	sqlFile      = cmd.getOptionValue('i');

		if (aseServer == null)
			aseServer = "SYBASE";

		DebugOptions.init();
		if (cmd.hasOption('x'))
		{
			String cmdLineDebug = cmd.getOptionValue('x');
			String[] sa = cmdLineDebug.split(",");
			for (int i=0; i<sa.length; i++)
			{
				String str = sa[i].trim();

				if (str.equalsIgnoreCase("list"))
				{
					System.out.println();
					System.out.println(" Option          Description");
					System.out.println(" --------------- -------------------------------------------------------------");
					for (Map.Entry<String,String> entry : Debug.getKnownDebugs().entrySet()) 
					{
						String debugOption = entry.getKey();
						String description = entry.getValue();

						System.out.println(" "+StringUtil.left(debugOption, 15, true) + " " + description);
					}
					System.out.println();
					// Get of of here if it was a list option
					throw new NormalExitException("List of debug options");
				}
				else
				{
					// add debug option
					Debug.addDebug(str);
				}
			}
		}

//		System.setProperty("Logging.print.noDefaultLoggerMessage", "false");
//		Logging.init("sqlw.", propFile);

		// Print out the memory configuration
		// And the JVM info
		_logger.debug("Starting "+Version.getAppName()+", version "+Version.getVersionStr()+", build "+Version.getBuildStr());
		_logger.debug("Debug Options enabled: "+Debug.getDebugsString());

		_logger.debug("Using Java Runtime Environment Version: "+System.getProperty("java.version"));
		_logger.debug("Using Java VM Implementation  Version: "+System.getProperty("java.vm.version"));
		_logger.debug("Using Java VM Implementation  Vendor:  "+System.getProperty("java.vm.vendor"));
		_logger.debug("Using Java VM Implementation  Name:    "+System.getProperty("java.vm.name"));
		_logger.debug("Using Java VM Home:    "+System.getProperty("java.home"));
		_logger.debug("Java class format version number: " +System.getProperty("java.class.version"));
		_logger.debug("Java class path: " +System.getProperty("java.class.path"));
		_logger.debug("List of paths to search when loading libraries: " +System.getProperty("java.library.path"));
		_logger.debug("Name of JIT compiler to use: " +System.getProperty("java.compiler"));
		_logger.debug("Path of extension directory or directories: " +System.getProperty("java.ext.dirs"));

		_logger.debug("Maximum memory is set to:  "+Runtime.getRuntime().maxMemory() / 1024 / 1024 + " MB. this could be changed with  -Xmx###m (where ### is number of MB)"); // jdk 1.4 or higher
		_logger.debug("Running on Operating System Name:  "+System.getProperty("os.name"));
		_logger.debug("Running on Operating System Version:  "+System.getProperty("os.version"));
		_logger.debug("Running on Operating System Architecture:  "+System.getProperty("os.arch"));
		_logger.debug("The application was started by the username:  "+System.getProperty("user.name"));
		_logger.debug("The application was started in the directory:   "+System.getProperty("user.dir"));

//		_logger.debug("System configuration file is '"+propFile+"'.");
//		_logger.debug("User configuration file is '"+userPropFile+"'.");
//		_logger.debug("Storing temporary configurations in file '"+tmpPropFile+"'.");
//		_logger.debug("Combined Configuration Search Order '"+StringUtil.toCommaStr(Configuration.getSearchOrder())+"'.");
//		_logger.info("Combined Configuration Search Order, With file names: "+StringUtil.toCommaStr(Configuration.getSearchOrder(true)));



//		String hostPortStr = "";
//		if (aseServer.indexOf(":") == -1)
//			hostPortStr = AseConnectionFactory.getIHostPortStr(aseServer);
//		else
//			hostPortStr = aseServer;

		// use IGNORE_DONE_IN_PROC=true, if not set in the options in the connection dialog
//		AseConnectionFactory.setPropertiesForAppname(APP_NAME, "IGNORE_DONE_IN_PROC", "true");

//		// Try make an initial connection...
		DbxConnection conn = connect();

		String sql = getSql();
//		execSql(conn, sql);

	}

	private String getSql()
	{
		return "select @@servername";
	}

	private DbxConnection connect()
	throws Exception
	{
		DbxConnection conn = null;

		conn.connect(null);
		_connectedToProductName = conn.getDatabaseProductName();

		return conn;
	}

	private void output(String str)
	{
		System.out.println(str);
	}

	private static final String REGEXP_MLC_SLC = "(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?:--.*)"; // SLC=SingleLineComment, MLC=MultiLineComment

	private String  _connectedToProductName = "";
	private boolean _abortOnDbMessages  = false;
	private String  _sqlBatchTerminator = "go";
	private boolean _useSemicolonHack   = false;
	private boolean _sendCommentsOnly   = false;

//	private void execSql(Connection conn)
//	{
//		// Setup a message handler
//		// Set an empty Message handler
//		SybMessageHandler curMsgHandler = null;
//		if (conn instanceof SybConnection)
//		{
//			curMsgHandler = ((SybConnection)conn).getSybMessageHandler();
////			((SybConnection)conn).setSybMessageHandler(null);
//			((SybConnection)conn).setSybMessageHandler(new SybMessageHandler()
//			{
//				@Override
//				public SQLException messageHandler(SQLException sqle)
//				{
//					// If we want to STOP if we get any errors...
//					// Then we should return the origin Exception
//					// SQLException will abort current SQL Batch, while SQLWarnings will continue to execute
//					if (_abortOnDbMessages)
//						return sqle;
//
//					// Downgrade ALL messages to SQLWarnings, so executions wont be interrupted.
//					return AseConnectionUtils.sqlExceptionToWarning(sqle);
//				}
//			});
//		}
//	}

//	private void execSql(Connection conn, String goSql)
//	throws Exception
//	{
//		int startRowInSelection = 0;
//
//		// If we've called close(), then we can't call this method
//		if (conn == null)
//			throw new IllegalStateException("Connection already closed.");
//
//		// Setup a message handler
//		// Set an empty Message handler
//		SybMessageHandler curMsgHandler = null;
//		if (conn instanceof SybConnection || conn instanceof TdsConnection)
//		{
//			SybMessageHandler newMessageHandler = new SybMessageHandler()
//			{
//				@Override
//				public SQLException messageHandler(SQLException sqle)
//				{
//					// If we want to STOP if we get any errors...
//					// Then we should return the origin Exception
//					// SQLException will abort current SQL Batch, while SQLWarnings will continue to execute
//					if (_abortOnDbMessages)
//						return sqle;
//
//					// Downgrade ALL messages to SQLWarnings, so executions wont be interrupted.
//					return AseConnectionUtils.sqlExceptionToWarning(sqle);
//				}
//			};
//			
//			if (conn instanceof SybConnection)
//			{
//				curMsgHandler = ((SybConnection)conn).getSybMessageHandler();
//				((SybConnection)conn).setSybMessageHandler(newMessageHandler);
//			}
//			// Set a TDS Message Handler
//			if (conn instanceof TdsConnection)
//				((TdsConnection)conn).setSybMessageHandler(newMessageHandler);
//		}
//
//		// Vendor specific setting before we start to execute
////		enableOrDisableVendorSpecifics(conn);
//
//		
//		// The script reader might throw Exception that we want to abort the whole executions on
//		try
//		{
//			// a linked list where to "store" result sets or messages
//			// before "displaying" them
////			_resultCompList = new ArrayList<JComponent>();
//
//			String sql = "";
//
//			// Get SQL Batch Terminator
//			String sqlBatchTerminator = _sqlBatchTerminator;//Configuration.getCombinedConfiguration().getProperty(PROPKEY_sqlBatchTerminator, DEFAULT_sqlBatchTerminator);
//				
//			// treat each 'go' rows as a individual execution
//			// readCommand(), does the job
//			AseSqlScriptReader sr = new AseSqlScriptReader(goSql, true, sqlBatchTerminator);
//			if (_useSemicolonHack)
//				sr.setSemiColonHack(true);
//
//			// Set a Vendor specific SQL Execution string (default is null, Oracle & HANA: it's "/"
////			sr.setAlternativeGoTerminator(getVendorSpecificSqlExecTerminatorString(_connectedToProductName));
//
//			int batchCount = sr.getSqlTotalBatchCount();
//
//			boolean isConnectionOk = true;
//
//			// loop all batches
//			for (sql = sr.getSqlBatchString(); sql != null; sql = sr.getSqlBatchString())
//			{
//				// This can't be part of the for loop, then it just stops if empty row
//				if ( StringUtil.isNullOrBlank(sql) )
//					continue;
//
//				// Remove SQL SingleLine and MultiLine Comments
//				// if Option() is true, simply do not send
//				String originSqlWithoutComments = sql.replaceAll(REGEXP_MLC_SLC, "").trim(); 
//				if ( StringUtil.isNullOrBlank(originSqlWithoutComments) && !_sendCommentsOnly )
//				{
////					_resultCompList.add( new JSkipSendSqlStatement(sql));
//					continue;
//				}
//				
////				progress.setState("Sending SQL to server for statement " + (sr.getSqlBatchNumber()+1) + " of "+batchCount+", starting at row "+(sr.getSqlBatchStartLine()+1) );
//
//				if (! isConnectionOk)
//					break;
//
//				// if 'go 10' we need to execute this 10 times
//				for (int execCnt=0; execCnt<sr.getMultiExecCount(); execCnt++)
//				{
//					// If cancel has been pressed, do not continue to repeat the command
////					if (progress.isCancelled())
////						break;
//
//					try
//					{
//						int rowsAffected = 0;
//
//						// RPC handling if the text starts with '\exec '
//						// The for of this would be: {?=call procName(parameters)}
//						SqlStatementInfo sqlStmntInfo = new SqlStatementInfo(conn, sql, _connectedToProductName, null);
//
////						if (_showSentSql_chk.isSelected() || sr.hasOption_printSql())
////							_resultCompList.add( new JSentSqlStatement(sql, sr.getSqlBatchStartLine() + startRowInSelection) );
//
//						// remember the start time
//						long execStartTime = System.currentTimeMillis();
//
////						// Get the Statement used for execution, which is used below when reading result sets etc
//						Statement stmnt = sqlStmntInfo.getStatement();
////						progress.setSqlStatement(stmnt); // Used to cancel() on the statement level
//
//						// Execute the SQL
//						boolean hasRs = sqlStmntInfo.execute();
//	
//						// calculate the execution time
//						long execStopTime = System.currentTimeMillis();
//						
//						// Keep a summary of the time to read ResultSet
//						long execReadRsSum = 0;
//	
//						// iterate through each result set
//						int rsCount = 0;
//						int loopCount = 0;
//						do
//						{
//							loopCount++; // used for debugging
//
//							// Append, messages and Warnings to _resultCompList, if any
//							putSqlWarningMsgs(stmnt, sr.getPipeCmd(), "-before-hasRs-", sr.getSqlBatchStartLine(), startRowInSelection, sql);
//	
//							if(hasRs)
//							{
//								rsCount++;
//			
//								// Get next resultset to work with
//								ResultSet rs = stmnt.getResultSet();
//
//								ResultSetTableModel rstm = null;
//			
//								// Append, messages and Warnings to _resultCompList, if any
//								putSqlWarningMsgs(stmnt, sr.getPipeCmd(), "-after-getResultSet()-Statement-", sr.getSqlBatchStartLine(), startRowInSelection, sql);
//								putSqlWarningMsgs(rs,    sr.getPipeCmd(), "-after-getResultSet()-ResultSet-", sr.getSqlBatchStartLine(), startRowInSelection, sql);
//	
//								// Check for BCP pipe command
//								if (false)
//								{
//								}
//								else
//								{
//									int limitRsRowsCount = -1; // do not limit
////									if (_limitRsRowsRead_chk.isSelected())
////										limitRsRowsCount = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_limitRsRowsReadCount, DEFAULT_limitRsRowsReadCount);
//									if (sr.hasOption_topRows())
//										limitRsRowsCount = sr.getOption_topRows();
//									
//									boolean asPlainText = true;
////									boolean asPlainText = _asPlainText_chk.isSelected();
////									if (sr.hasOption_asPlaintText())
////										asPlainText = sr.getOption_asPlaintText();
//
//									boolean noData = false;
//									if (sr.hasOption_noData())
//										noData = sr.getOption_noData();
//									
//									if (asPlainText)
//									{
//										rstm = new ResultSetTableModel(rs, true, sql, limitRsRowsCount, noData, sr.getPipeCmd(), null);
//										putSqlWarningMsgs(rstm.getSQLWarning(), sr.getPipeCmd(), "-after-ResultSetTableModel()-tm.getSQLWarningList()-", sr.getSqlBatchStartLine(), startRowInSelection, sql);
//
//										execReadRsSum += rstm.getResultSetReadTime();
//										
////										if (_printRsInfo_chk.isSelected() || sr.hasOption_printRsi())
////											_resultCompList.add( new JResultSetInfo(rstm, sql, sr.getSqlBatchStartLine() + startRowInSelection) );
//
//										//_resultCompList.add(new JPlainResultSet(rstm));
//										output(rstm.toTableString());
//
//										// FIXME: use a callback interface instead
//										
////										if (rstm.isCancelled())
////											_resultCompList.add(new JAseCancelledResultSet(sql));
//
////										if (rstm.wasAbortedAfterXRows())
////										_resultCompList.add(new JAseLimitedResultSetTop(rstm.getAbortedAfterXRows(), sql));
//										if (rstm.wasAbortedAfterXRows())
//											System.out.println("Reading the ResultSet was stopped after "+rstm.getAbortedAfterXRows()+" rows.");
//									}
//									else
//									{
////										// Convert the ResultSet into a TableModel, which fits on a JTable
////										rstm = new ResultSetTableModel(rs, true, sql, limitRsRowsCount, noData, sr.getPipeCmd(), progress);
////										putSqlWarningMsgs(rstm.getSQLWarning(), _resultCompList, sr.getPipeCmd(), "-after-ResultSetTableModel()-tm.getSQLWarningList()-", sr.getSqlBatchStartLine(), startRowInSelection, sql);
////					
////										execReadRsSum += rstm.getResultSetReadTime();
////
////										// Create the JTable, using the just created TableModel/ResultSet
//////										JXTable tab = new ResultSetJXTable(rstm);
//////										tab.setSortable(true);
//////										tab.setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);
//////										tab.packAll(); // set size so that all content in all cells are visible
//////										tab.setColumnControlVisible(true);
////////										tab.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//////										tab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
//////		
//////										// Add a popup menu
//////										tab.setComponentPopupMenu( createDataTablePopupMenu(tab) );
////		
////										if (_printRsInfo_chk.isSelected() || sr.hasOption_printRsi())
////											_resultCompList.add( new JResultSetInfo(rstm, sql, sr.getSqlBatchStartLine() + startRowInSelection) );
////
////										// Add the JTable to a list for later use
//////										_resultCompList.add(tab);
////										_resultCompList.add(new JTableResultSet(rstm));
////										// FIXME: use a callback interface instead
////										
////										if (rstm.isCancelled())
////											_resultCompList.add(new JAseCancelledResultSet(sql));
////
////										if (rstm.wasAbortedAfterXRows())
////											_resultCompList.add(new JAseLimitedResultSetTop(rstm.getAbortedAfterXRows(), sql));
//									}
//								} // end: normal read result set
//			
//								// Append, messages and Warnings to _resultCompList, if any
//								putSqlWarningMsgs(stmnt, sr.getPipeCmd(), "-before-rs.close()-", sr.getSqlBatchStartLine(), startRowInSelection, sql);
//	
//								// Get rowcount from the ResultSetTableModel
//								if (rstm != null)
//								{
//									int readCount = rstm.getReadCount();
//        							if (readCount >= 0)
//        							{
////        								if (_showRowCount_chk.isSelected() || sr.hasOption_rowCount() || sr.hasOption_noData())
////        									_resultCompList.add( new JAseRowCount(readCount, sql) );
//        							}
//								}
//
//								// Close it
//								rs.close();
//
//							} // end: hasResultSets 
//							else // Treat update/row count(s) for NON RESULTSETS
//							{
//								// Java DOC: getUpdateCount() Retrieves the current result as an update count; if the result is a ResultSet object 
//								//           or there are no more results, -1 is returned. This method should be called only once per result.
//								// Without this else statement, some drivers maight fail... (MS-SQL actally did)
//
//								rowsAffected = stmnt.getUpdateCount();
//
//								if (rowsAffected >= 0)
//								{
////									if (_showRowCount_chk.isSelected() || sr.hasOption_rowCount() || sr.hasOption_noData())
////										_resultCompList.add( new JAseRowCount(rowsAffected, sql) );
//								}
//								else
//								{
//									_logger.debug("---- No more results to process.");
//								}
//							} // end: no-resultset
//			
//							// Append, messages and Warnings to _resultCompList, if any
//							putSqlWarningMsgs(stmnt, sr.getPipeCmd(), "-before-rs.close()-", sr.getSqlBatchStartLine(), startRowInSelection, sql);
//			
//							// Check if we have more resultsets
//							// If any SQLWarnings has not been found above, it will throw one here
//							// so catch raiserrors or other stuff that is not SQLWarnings.
//							hasRs = stmnt.getMoreResults();
//			
//							// Append, messages and Warnings to _resultCompList, if any
//							putSqlWarningMsgs(stmnt, sr.getPipeCmd(), "-before-rs.close()-", sr.getSqlBatchStartLine(), startRowInSelection, sql);
//
//							if (_logger.isTraceEnabled())
//								_logger.trace( "--loopCount="+loopCount+", hasRs="+hasRs+", rowsAffected="+rowsAffected+", "+((hasRs || rowsAffected != -1) ? "continue-to-loop" : "<<< EXIT LOOP <<<") );
//						}
//						while (hasRs || rowsAffected != -1);
//			
//						// Read RPC returnCode and output parameters, if it was a RPC and any retCode and/or params exists
//						sqlStmntInfo.readRpcReturnCodeAndOutputParameters(_resultCompList, true);
//	
//						// Append, messages and Warnings to _resultCompList, if any
//						putSqlWarningMsgs(stmnt, sr.getPipeCmd(), "-before-stmnt.close()-", sr.getSqlBatchStartLine(), startRowInSelection, sql);
//						
//						// Close the statement
//						stmnt.close();
//
//						// Connection level WARNINGS, Append, messages and Warnings to _resultCompList, if any
//						putSqlWarningMsgs(conn, sr.getPipeCmd(), "-before-stmnt.close()-", sr.getSqlBatchStartLine(), startRowInSelection, sql);
//	
//						// How long did it take
//						long execFinnishTime = System.currentTimeMillis();
////						if (_clientTiming_chk.isSelected() || sr.hasOption_printClientTiming())
////							_resultCompList.add( new JClientExecTime(execStartTime, execStopTime, execFinnishTime, execReadRsSum, startRowInSelection + sr.getSqlBatchStartLine() + 1, sql));
//	
//						// Sleep for a while, if that's enabled
//						if (sr.getMultiExecWait() > 0)
//						{
//							//System.out.println("WAITING for multi exec sleep: "+sr.getMultiExecWait());
//							Thread.sleep(sr.getMultiExecWait());
//						}
//					}
//					catch (SQLException ex)
//					{
//						_logger.debug("Caught SQL Exception, get the stacktrace if in debug mode...", ex);
//
////						incSqlExceptionCount();
////						progress.setSqlStatement(null);
//
//						// If something goes wrong, clear the message line
////						_statusBar.setMsg("Error: "+ex.getMessage());
//
//						// when NOT using jConnect, I can't downgrade a SQLException to a SQLWarning
//						// so we will always end up here (for the moment)
//						// Try to read the SQLException and figure out on what line it happened + mark that line with an error
//						errorReportingVendorSqlException(ex, startRowInSelection, sr.getSqlBatchStartLine(), sql);
//
//						// Add the information to the output window
//						// This is done in: errorReportingVendorSqlException()
//						
//						
//						// Check if we are still connected...
//						if ( ! AseConnectionUtils.isConnectionOk(conn) )
//						{
//							isConnectionOk = false;
//							break;
//						}
//
//						// If we want to STOP if we get any errors...
//						// Then we should return the origin Exception
//						// NOTE: THIS HAS NOT BEEN TESTED
////						if (_abortOnDbMessages)
////							throw ex;
//					}
//					finally
//					{
//						// Read some extra stuff, yes do this even if a SQLException was thrown
////						readVendorSpecificResults(conn, progress, _resultCompList, startRowInSelection, sr.getSqlBatchStartLine(), sql);
//					}
//
//				} // end: 'go 10'
//				
//			} // end: read batches
//
//			// Close the script reader
//			sr.close();
//
//
//			
//			// Finally, add all the results to the output
////			addToResultsetPanel(_resultCompList, (_appendResults_chk.isSelected() || _appendResults_scriptReader), _asPlainText_chk.isSelected());
//		}
//		catch (IOException ex)
//		{
//			_logger.error("While reading the input SQL 'go' String, caught: "+ex, ex);
//			throw ex;
//		}
//		catch (PipeCommandException ex)
//		{
////			_logger.warn("Problems creating the 'go | pipeCommand'. Caught: "+ex, ex);
////			if (guiShowErrors)
////			{
////				SwingUtils.showWarnMessage("Problems creating PipeCommand", ex.getMessage(), ex);
////			}
//			throw ex;
//		}
//		catch (GoSyntaxException ex)
//		{
////			_logger.warn("Problems parsing the SQL Batch Terminator ('go'). Caught: "+ex, ex);
////			if (guiShowErrors)
////			{
////				String htmlMsg = "<html>" + ex.getMessage().replace("\n", "<br>") + "</html>";
////				SwingUtils.showWarnMessage("Problems interpreting a SQL Batch Terminator", htmlMsg, ex);
////			}
//			throw ex;
//		}
//		finally
//		{
//			// restore old message handler
//			if (curMsgHandler != null)
//			{
//				((SybConnection)conn).setSybMessageHandler(curMsgHandler);
//			}
//			// Restore old message handler
//			if (conn instanceof TdsConnection)
//				((TdsConnection)conn).restoreSybMessageHandler();
//		}
//	}
//
//	private void putSqlWarningMsgs(ResultSet rs, PipeCommand pipeCommand, String debugStr, int batchStartRow, int startRowInSelection, String currentSql)
//	{
//		if (rs == null)
//			return;
//		try
//		{
//			putSqlWarningMsgs(rs.getWarnings(), pipeCommand, debugStr, batchStartRow, startRowInSelection, currentSql);
//			rs.clearWarnings();
//		}
//		catch (SQLException e)
//		{
//		}
//	}
//	private void putSqlWarningMsgs(Statement stmnt, PipeCommand pipeCommand, String debugStr, int batchStartRow, int startRowInSelection, String currentSql)
//	{
//		if (stmnt == null)
//			return;
//		try
//		{
//			putSqlWarningMsgs(stmnt.getWarnings(), pipeCommand, debugStr, batchStartRow, startRowInSelection, currentSql);
//			stmnt.clearWarnings();
//		}
//		catch (SQLException e)
//		{
//		}
//	}
//	private void putSqlWarningMsgs(Connection conn, PipeCommand pipeCommand, String debugStr, int batchStartRow, int startRowInSelection, String currentSql)
//	{
//		if (conn == null)
//			return;
//		try
//		{
//			putSqlWarningMsgs(conn.getWarnings(), pipeCommand, debugStr, batchStartRow, startRowInSelection, currentSql);
//			conn.clearWarnings();
//		}
//		catch (SQLException e)
//		{
//		}
//	}
//
//	private void putSqlWarningMsgs(SQLException sqe, PipeCommand pipeCommand, String debugStr, int batchStartRow, int startRowInSelection, String currentSql)
//	{
//		if (startRowInSelection < 0)
//			startRowInSelection = 0;
//
//		while (sqe != null)
//		{
//			int    msgNum      = sqe.getErrorCode();
//			String msgText     = StringUtil.removeLastNewLine(sqe.getMessage());
//			int    msgSeverity = -1;
//			String objectText  = null;
//			
//			StringBuilder sb = new StringBuilder();
//			int scriptRow = -1;
//			if(sqe instanceof EedInfo) // Message from jConnect
//			{
//				// Error is using the addtional TDS error data.
//				EedInfo eedi = (EedInfo) sqe;
//				msgSeverity  = eedi.getSeverity();
//				
//				// Try to figgure out what we should write out in the 'script row'
//				// Normally it's *nice* to print out *where* in the "whole" document the error happened, especially syntax errors etc (and not just "within" the SQL batch, because you would have several in a file)
//				// BUT: if we *call* a stored procedure, and that stored procedure produces errors, then we want to write from what "script line" (or where) the procedure was called at
//				// BUT: if we are developing creating procedures/functions etc we would want *where* in the "script line" (within the prcedure text) the error is produced (easier to find syntax errors, faulty @var names, table nemaes etc...)
//				int lineNumber = eedi.getLineNumber();
//				int lineNumberAdjust = 0;
//
//				// for some product LineNumber starts at 0, so lets just adjust for this in the calculated (script row ###)
//				if (    DbUtils.DB_PROD_NAME_SYBASE_ASA.equals(_connectedToProductName)
//				     || DbUtils.DB_PROD_NAME_SYBASE_IQ .equals(_connectedToProductName) )
//				{
//					lineNumberAdjust = 1;
//
//					// Parse SQL Anywhere messages that looks like: 
//					//     Msg 102, Level 15, State 0:
//					//     Line 0 (script row 884), Status 0, TranState 1:
//					//     SQL Anywhere Error -131: Syntax error near 'x' on line 4
//					// Get the last part 'on line #' as the lineNumberAdjust
//					if (msgText.matches(".*on line [0-9]+[ ]*.*"))
//					{
//						int startPos = msgText.indexOf("on line ");
//						if (startPos >= 0)
//						{
//							startPos += "on line ".length();
//							int endPos = msgText.indexOf(" ", startPos);
//							if (endPos <= 0)
//								endPos = msgText.length();
//							
//							String lineNumStr = msgText.substring(startPos, endPos);
//
//							try { lineNumberAdjust = Integer.parseInt(lineNumStr); }
//							catch(NumberFormatException ignore) {}
//						}
//					}
//				}
//
//				// print messages, take some specific actions
//				if (msgSeverity <= 10)
//				{
//					// If message originates from a Stored Procedures
//					// do not use the Line Number from the Stored Procs, instead use the SQL Batch start...
//					// ERROR messages get's handle in a TOTAL different way
//					if(eedi.getProcedureName() != null)
//					{
//						lineNumber = 1;
//
//						// If batch starts with empty lines, increment the lineNumber...
//						lineNumber += StringUtil.getFirstInputLine(currentSql);
//					}
//				}
//
//				// which row in the script was this at... not this might change if (msgSeverity > 10)
//				scriptRow = startRowInSelection + batchStartRow + lineNumber + lineNumberAdjust;
//
//				// Fill in some extra information for error messages
//				if (msgSeverity > 10)
//				{
//					boolean firstOnLine = true;
//					sb.append("Msg " + sqe.getErrorCode() +
//							", Level " + eedi.getSeverity() + ", State " +
//							eedi.getState() + ":\n");
//
//					if( eedi.getServerName() != null)
//					{
//						sb.append("Server '" + eedi.getServerName() + "'");
//						firstOnLine = false;
//					}
//					if(eedi.getProcedureName() != null)
//					{
//						sb.append( (firstOnLine ? "" : ", ") +
//								"Procedure '" + eedi.getProcedureName() + "'");
//						firstOnLine = false;
//					}
//
//					// If message is from a procedure, get some extra...
//					String extraDesc  = "";
//					if ( StringUtil.hasValue(eedi.getProcedureName()) )
//					{
//						String regex    = "(create|alter)\\s+(procedure|proc|trigger|view|function)";
//						Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
//
//						// SQL has create proc etc in it... figgure out the line number...
//						if (pattern.matcher(currentSql).find())
//						{
//							// Keep current scriptRow (line number in the procedure)
//							lineNumber = eedi.getLineNumber();
//							extraDesc  = "";
//						}
//						// we are only executing the procedure, so take another approach
//						// lets line number for first occurance of the procname in current SQL... 
//						else 
//						{
//							String searchForName = eedi.getProcedureName();
//
//							// also try to get the procedure text, which will be added to the message
//							// but not for print statement
////							if (_getObjectTextOnError_chk.isSelected() && eedi.getSeverity() > 10)
////							{
////								objectText = AseConnectionUtils.getObjectText(_conn, null, searchForName, null, _srvVersion);
////								if (objectText == null && searchForName.startsWith("sp_"))
////									objectText = AseConnectionUtils.getObjectText(_conn, "sybsystemprocs", searchForName, null, _srvVersion);
////								objectText = StringUtil.markTextAtLine(objectText, lineNumber, true);
////							}
//
//							// loop the sql and find the first row that matches the procedure text
//							String procRegex = searchForName + "[^a-z,^A-Z,^0-9]";
//							Pattern procPat  = Pattern.compile(procRegex);
//
//							Scanner scanner = new Scanner(currentSql);
//							int rowNumber = 0;
//							while (scanner.hasNextLine()) 
//							{
//								rowNumber++;
//								String line = scanner.nextLine();
//								// stop at first match
//								if (procPat.matcher(line).find())
//									break;
//							}
//
//							lineNumber = rowNumber;
//							extraDesc  = "Called from ";
//						}
//					}
//					scriptRow = startRowInSelection + batchStartRow + lineNumber + lineNumberAdjust;
//					String scriptRowStr = (batchStartRow >= 0 ? " ("+extraDesc+"script row "+scriptRow+")" : "");
//
//					sb.append( (firstOnLine ? "" : ", ") +
//							"Line " + eedi.getLineNumber() + scriptRowStr +
//							", Status " + eedi.getStatus() + 
//							", TranState " + eedi.getTranState() + ":\n");
//				}
//
//				// Now print the error or warning
//				String msg = sqe.getMessage();
//				if (msg.endsWith("\n"))
//					sb.append(msg);
//				else
//					sb.append(msg+"\n");
//
//			} // end: if(sqe instanceof EedInfo) -- jConnect message
//			else
//			{
//				// jConnect: SqlState: 010P4 java.sql.SQLWarning: 010P4: An output parameter was received and ignored.
//				if ( "010P4".equals(sqe.getSQLState()) )
//				{
//					// Simply ignore: 010P4: An output parameter was received and ignored.
//					// This is when a Stored Procedure return code is returned, which is Output Parameter 1
//				}
//				else if ( "010SL".equals(sqe.getSQLState()) )
//				{
//					// IGNORE: 010SL: Out-of-date metadata accessor information was found on this database.  Ask your database administrator to load the latest scripts.
//				}
//				// OK, jTDS drivers etc, will have warnings etc in print statements
//				// Lets try to see if it's one of those.
//				else if (sqe.getErrorCode() == 0 && sqe instanceof SQLWarning)
//				{
//					if (StringUtil.isNullOrBlank(msgText))
//						sb.append(" ");
//					else
//						sb.append(msgText);
//				}
//				else
//				{
//					String msg = "Unexpected SQLException: " +
//						_connectedToProductName + ": ErrorCode "+sqe.getErrorCode()+", SQLState "+sqe.getSQLState()+", ExceptionClass: " + sqe.getClass().getName() + "\n"
//						+ sqe.getMessage();
//					sb.append(msg);
//
//					// Get Oracle ERROR Messages
//					if (DbUtils.isProductName(_connectedToProductName, DbUtils.DB_PROD_NAME_ORACLE))
//					{
//						oracleShowErrors(_conn, startRowInSelection, batchStartRow, currentSql);
//
//						// also try to get the procedure text, which will be added to the message
//						// but not for print statement
////						if (_getObjectTextOnError_chk.isSelected())
////						{
//////							String searchForName = "";
//////							int    lineNumber    = -1;
//////
//////							objectText = DbUtils.getOracleObjectText(_conn, searchForName);
//////							objectText = StringUtil.markTextAtLine(objectText, lineNumber, true);
////						}
//					}
//				}
//			}
//			
//			// Add the info to the list
//			if (sb.length() > 0)
//			{
//				// If new-line At the end, remove it
//				if ( sb.charAt(sb.length()-1) == '\n' )
//					sb.deleteCharAt(sb.length()-1);
//
//				String aseMsg = sb.toString();
//
//				// Grep on individual rows in the Message
//				if (pipeCommand != null && pipeCommand.isGrep())
//				{
//					PipeCommandGrep grep = (PipeCommandGrep)pipeCommand.getCmd();
//					// If VALID for Message
//					if ( grep.isValidForType(PipeCommandGrep.TYPE_MSG) )
//					{
//						String regexpStr = ".*" + grep.getConfig() + ".*";
//						String newMessage = "";
//				
//						Scanner scanner = new Scanner(aseMsg);
//						while (scanner.hasNextLine()) 
//						{
//							String line = scanner.nextLine();
//				
//							boolean aMatch = line.matches(regexpStr);
//							System.out.println(">>>>> aMatch="+aMatch+", isOptV="+grep.isOptV()+", regexp='"+regexpStr+"'.");
//							if (grep.isOptV())
//								aMatch = !aMatch;
//							if (aMatch)
//								newMessage += line + "\n";
//						}
//
//						aseMsg = StringUtil.removeLastNewLine(newMessage);
//					}
//				}
//				
//				
//				// Dummy to just KEEP the text message, and strip off Msg, Severity etc...
//				// but only if Msg > 20000 and Severity == 16
//				if (pipeCommand != null && pipeCommand.isGrep())
//				{
//					PipeCommandGrep grep = (PipeCommandGrep)pipeCommand.getCmd();
//					// X option, mage the message a "one line"
//					if ( grep.isOptX() )
//					{
////						if (msgNum > 20000 && msgSeverity == 16)
//							aseMsg = "Msg "+msgNum+": " + msgText;
//					}
//				}
//
////				if ( ! StringUtil.isNullOrBlank(aseMsg))
////				resultCompList.add( new JAseMessage(aseMsg, msgNum, msgText, msgSeverity, scriptRow, currentSql, objectText, _query_txt) );
//				output(aseMsg);
//
//				if (_logger.isTraceEnabled())
//					_logger.trace("ASE Msg("+debugStr+"): "+aseMsg);
//			}
//
//			sqe = sqe.getNextException();
//		}
//	}
//
//	/**
//	 * This simple main method tests the class.  It expects four command-line
//	 * arguments: the driver classname, the database URL, the username, and
//	 * the password
//	 **/
//	public static void test_main(String args[]) throws Exception
//	{
//		// FIXME: parse input parameters
//
//		Properties log4jProps = new Properties();
//		//log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
//		log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
//		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
//		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
//		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
//		PropertyConfigurator.configure(log4jProps);
//
////		// Set configuration, right click menus are in there...
////		Configuration conf = new Configuration("c:\\projects\\dbxtune\\dbxtune.properties");
////		Configuration.setInstance(Configuration.SYSTEM_CONF, conf);
//
////		String server = "GORAN_1_DS";
//////		String host = AseConnectionFactory.getIHost(server);
//////		int    port = AseConnectionFactory.getIPort(server);
////		String hostPortStr = AseConnectionFactory.getIHostPortStr(server);
////		System.out.println("Connectiong to server='"+server+"'. Which is located on '"+hostPortStr+"'.");
////		Connection conn = null;
////		try
////		{
////			Properties props = new Properties();
////			props.put("CHARSET", "iso_1");
////			AseConnectionFactory.setPropertiesForAppname(Version.getAppName()+"-QueryWindow", "IGNORE_DONE_IN_PROC", "true");
////			
////			conn = AseConnectionFactory.getConnection(hostPortStr, null, "sa", "", Version.getAppName()+"-QueryWindow", null, props, null);
////		}
////		catch (SQLException e)
////		{
////			System.out.println("Problems connecting: " + AseConnectionUtils.sqlExceptionToString(e));
//////			AseConnectionUtils.sqlWarningToString(e);
////			throw e;
////		}
//
//
////		// Create a QueryWindow component that uses the factory object.
////		QueryWindow qw = new QueryWindow(conn, 
////				"print 'a very long string that starts here.......................and continues,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,with some more characters------------------------and some more++++++++++++++++++++++++++++++++++++ yes even more 00000 0 0 0 0 0 000000000 0 00000000 00000, lets do some more.......................... end it ends here. -END-'\n" +
////				"print '11111111'\n" +
////				"select getdate()\n" +
////				"exec sp_whoisw2\n" +
////				"select \"ServerName\" = @@servername\n" +
////				"raiserror 123456 'A dummy message by raiserror'\n" +
////				"exec sp_help sysobjects\n" +
////				"select \"Current Date\" = getdate()\n" +
////				"print '222222222'\n" +
////				"select * from master..sysdatabases\n" +
////				"print '|3-33333333'\n" +
////				"print '|4-33333333'\n" +
////				"print '|5-33333333'\n" +
////				"print '|6-33333333'\n" +
////				"print '|7-33333333'\n" +
////				"print '|8-33333333'\n" +
////				"print '|9-33333333'\n" +
////				"print '|10-33333333'\n" +
////				"                             exec sp_opentran \n" +
////				"print '|11-33333333'\n" +
////				"print '|12-33333333'\n" +
////				"print '|13-33333333'\n" +
////				"print '|14-33333333'\n" +
////				"print '|15-33333333'\n" +
////				"print '|16-33333333'\n" +
////				"print '|17-33333333'\n" +
////				"select * from sysobjects \n" +
////				"select * from sysprocesses ",
////				null, true, WindowType.JFRAME, null);
////		qw.openTheWindow();
//	}	
	/**
	 * Print command line options.
	 * @param options
	 */
	private static void printHelp(Options options, String errorStr)
	{
		PrintWriter pw = new PrintWriter(System.out);

		if (errorStr != null)
		{
			pw.println();
			pw.println(errorStr);
			pw.println();
		}

//		pw.println("usage: sqlc [-U <user>] [-P <passwd>] [-S <server>] [-D <dbname>]");
//		pw.println("            [-u <jdbcUrl>] [-d <jdbcDriver>]");
//		pw.println("            [-q <sqlStatement>] [-h] [-v] [-x] <debugOptions> ");
		pw.println("usage: sqlc [-U <user>] [-P <passwd>]");
		pw.println("            [-u <jdbcUrl>] [-d <jdbcDriver>]");
		pw.println("            [-i <input_file>");
		pw.println("            [-h] [-v] [-x] <debugOptions> ");
		pw.println("  ");
		pw.println("options:");
		pw.println("  -h,--help                 Usage information.");
		pw.println("  -v,--version              Display Application and JVM Version.");
		pw.println("  -x,--debug <dbg1,dbg2>    Debug options: a comma separated string");
		pw.println("                            To get available option, do -x list");
		pw.println("  ");
		pw.println("  -U,--user <user>          Username when connecting to server.");
		pw.println("  -P,--passwd <passwd>      Password when connecting to server. null=noPasswd");
//		pw.println("  -S,--server <server>      Server to connect to.");
//		pw.println("  -D,--dbname <dbname>      Database to use when connecting");
		pw.println("  -u,--jdbcUrl <url>        JDBC URL. if not a sybase/TDS server");
		pw.println("  -d,--jdbcDriver <driver>  JDBC Driver. if not a sybase/TDS server");
		pw.println("                            If the JDBC drivers is registered with the ");
		pw.println("                            DriverManager, this is NOT needed");
//		pw.println("  -p,--connProfile <name>   Connect using an existing Connection Profile");
//		pw.println("  -q,--query <sqlStatement> SQL Statement to execute");
		pw.println("  -i,--inputFile <filename> Input File");
		pw.println("");
		pw.flush();
	}

	/**
	 * Build the options com.dbxtune.parser. Has to be synchronized because of the way
	 * Options are constructed.
	 * 
	 * @return an options com.dbxtune.parser.
	 */
	private static synchronized Options buildCommandLineOptions()
	{
		Options options = new Options();

		// create the Options
		options.addOption( "h", "help",        false, "Usage information." );
		options.addOption( "v", "version",     false, "Display Application and JVM Version." );
		options.addOption( "x", "debug",       true,  "Debug options: a comma separated string dbg1,dbg2,dbg3" );

		options.addOption( "U", "user",        true, "Username when connecting to server." );
		options.addOption( "P", "passwd",      true, "Password when connecting to server. (null=noPasswd)" );
//		options.addOption( "S", "server",      true, "Server to connect to." );
//		options.addOption( "D", "dbname",      true, "Database use when connecting" );
		options.addOption( "u", "jdbcUrl",     true, "JDBC URL. if not a sybase/TDS server" );
		options.addOption( "d", "jdbcDriver",  true, "JDBC Driver. if not a sybase/TDS server. If the JDBC drivers is registered with the DriverManager, this is NOT needed." );
//		options.addOption( "p", "connProfile", true, "Connect using an existing Connection Profile");
//		options.addOption( "q", "sqlStatement",true, "SQL statement to execute" );
		options.addOption( "i", "inputFile",   true, "Input File to open in editor" );

		return options;
	}


	//---------------------------------------------------
	// Command Line Parsing
	//---------------------------------------------------
	private static CommandLine parseCommandLine(String[] args, Options options)
	throws ParseException
	{
		// create the command line com.dbxtune.parser
		CommandLineParser parser = new DefaultParser();	
	
		// parse the command line arguments
		CommandLine cmd = parser.parse( options, args );

		// Validate any mandatory options or dependencies of switches
		

		if (_logger.isDebugEnabled())
		{
			for (@SuppressWarnings("unchecked") Iterator<Option> it=cmd.iterator(); it.hasNext();)
			{
				Option opt = it.next();
				_logger.debug("parseCommandLine: swith='"+opt.getOpt()+"', value='"+opt.getValue()+"'.");
			}
		}

		return cmd;
	}

	//---------------------------------------------------
	// MAIN
	//---------------------------------------------------
	public static void main(String[] args)
	{
//		jConnectEnableLogging();

		Options options = buildCommandLineOptions();
		try
		{
			CommandLine cmd = parseCommandLine(args, options);

			//-------------------------------
			// HELP
			//-------------------------------
			if ( cmd.hasOption("help") )
			{
				printHelp(options, "The option '--help' was passed.");
			}
			//-------------------------------
			// VERSION
			//-------------------------------
			else if ( cmd.hasOption("version") )
			{
				System.out.println();
//				System.out.println(Version.getAppName()+" Version: " + Version.getVersionStr() + " JVM: " + System.getProperty("java.version"));
				System.out.println("sqlc Version: 1.0.0, JVM: " + System.getProperty("java.version"));
				System.out.println();
			}
			//-------------------------------
			// Check for correct number of cmd line parameters
			//-------------------------------
			else if ( cmd.getArgs() != null && cmd.getArgs().length > 0 )
			{
				String error = "Unknown options: " + StringUtil.toCommaStr(cmd.getArgs());
				printHelp(options, error);
			}
			//-------------------------------
			// Start GUI/NOGUI will be determined later on.
			//-------------------------------
			else
			{
				new SqlCommandLine(cmd);
			}
		}
		catch (ParseException pe)
		{
			String error = "Error: " + pe.getMessage();
			printHelp(options, error);
		}
		catch (NormalExitException e)
		{
			// This was probably throws when checking command line parameters
			// do normal exit
		}
		catch (Exception e)
		{
			System.out.println();
			System.out.println("Error: " + e.getMessage());
			System.out.println();
			System.out.println("Printing a stacktrace, where the error occurred.");
			System.out.println("--------------------------------------------------------------------");
			e.printStackTrace();
			System.out.println("--------------------------------------------------------------------");
		}
	}
}
