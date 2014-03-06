/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.asetune.Version;
import com.asetune.gui.ResultSetTableModel;
import com.sybase.jdbcx.EedInfo;
import com.sybase.jdbcx.SybConnection;
import com.sybase.jdbcx.SybMessageHandler;

public class AseSqlScript
implements SybMessageHandler
{
	private static Logger _logger = Logger.getLogger(AseSqlScript.class);

	private Connection                      _conn;

	@SuppressWarnings("unused")
	private String                          _sqlMessage         = null;

	private SybMessageHandler               _saveMsgHandler     = null;
	private boolean                         _saveAutoCommit     = false;
	private String                          _dbnameBeforeScript = null;
//	private ArrayList<ResultSetTableModel>  _resultCompList     = null;
	private String                          _msgPrefix          = "";
	private int                             _queryTimeout       = 0;

	/** 
	 * On open current database and message handler are saved, which is restored by close()
	 * @param conn The Connection 
	 * @param queryTimeout Query timeout in seconds, 0 = no timeout
	 */
	public AseSqlScript(Connection conn, int queryTimeout)
	{
		_conn = conn;
		_queryTimeout = queryTimeout;

		_saveMsgHandler = ((SybConnection)_conn).getSybMessageHandler();
		((SybConnection)_conn).setSybMessageHandler(this);

		try
		{
			_saveAutoCommit = _conn.getAutoCommit();
			if (_saveAutoCommit != true)
				_conn.setAutoCommit(true);

			_dbnameBeforeScript = AseConnectionUtils.getCurrentDbname(_conn);
		}
		catch(SQLException e)
		{
			_logger.warn("Problems when doing set|getAutoCommit on the connection.", e);
		}

	}

	/** SQL Timeout in seconds, 0 = no timeout */
	public void setQueryTimeout(int queryTimeout)
	{
		_queryTimeout = queryTimeout;
	}

	/** SQL Timeout in seconds, 0 = no timeout */
	public int getQueryTimeout()
	{
		return _queryTimeout;
	}

	/**
	 * This just restores the Message Handler, and dbname to what it was previously.<br>
	 * This does NOT close the database connection.
	 */
	public void close()
	{
		// Restore autoCommit
		try
		{
			_conn.setAutoCommit(_saveAutoCommit);
			AseConnectionUtils.useDbname(_conn, _dbnameBeforeScript);
		}
		catch(SQLException e)
		{
			// JZ0C0 = Connection is already closed
			if ( "JZ0C0".equals(e.getSQLState()) )
				_logger.info("Problems when doing close() on AseSqlScript object (restore autoCommit and database context). SQL State 'JZ0C0', Connection is Already closed. JDBC Message: "+e.getMessage());
			else
				_logger.warn("Problems when doing set|getAutoCommit on the connection.", e);
		}

		// Restore message handler
		((SybConnection)_conn).setSybMessageHandler(_saveMsgHandler);
	}

	public void   setMsgPrefix(String prefix) { _msgPrefix = prefix; }
	public String getMsgPrefix()              { return _msgPrefix; }

	
	/**
	 * 
	 * @param className using full package name spec "com.asetune.utils.ClassName"
	 * @param filename
	 * @throws SQLException
	 */
	public void execute(String className, String filename)
	throws SQLException
	{
		try
		{
			Class<?> clazz = Class.forName(className);
			execute(clazz, filename);
		}
		catch(ClassNotFoundException e)
		{
			//return null;
		}
	}

	public void execute(Class<?> clazz, String filename)
	throws SQLException
	{
		//StringBuffer sb = new StringBuffer("");
		try
		{
			URL url = clazz.getResource(filename);
			if(url != null)
			{
				BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
				br = new BufferedReader(new InputStreamReader(url.openStream()));
				execute(br);
				br.close();
			}
		}
		catch(IOException e)
		{
//			return null;
		}
	}

	public void execute(String filename)
	throws SQLException
	{
		try
		{
			File file = new File(filename);
			if(file != null)
			{
//				if(!file.exists())
//					throw new SQLException(BundleManager.getString("ProviderRes", "ERR_SCRIPT_NOT_FND", file.getAbsolutePath()), "_RSM_", 5);
//				if(!file.canRead())
//					throw new SQLException(BundleManager.getString("ProviderRes", "ERR_READ_PERMISSIONS", file.getAbsolutePath()), "_RSM_", 4);

				FileReader fr = new FileReader(file);
				BufferedReader br = new BufferedReader(fr);
				execute(br);
				br.close();
				fr.close();
			}
		}
		catch(IOException e)
		{
			throw new SQLException("Problems loading the file '"+filename+"'. Caught: "+e.getMessage(), e);
		}
	}

	public void executeStr(String sql)
	throws SQLException
	{
		try
		{
			StringReader sr = new StringReader(sql);
			BufferedReader br = new BufferedReader(sr);
			execute(br);
			br.close();
			sr.close();
		}
		catch(IOException e)
		{
		}
	}

	public void execute(BufferedReader br)
	throws SQLException, IOException
	{
//		Dbg.wassert(conn != null, "Null SQL connection parameter.");
//		Dbg.wassert(filename != null && filename.length() > 0, "Null or empty SQL script name.");

		for(String sql=readCommand(br); sql!=null; sql=readCommand(br))
		{
			// This can't be part of the for loop, then it just stops if empty row
			if ( StringUtil.isNullOrBlank(sql) )
				continue;

			try
			{
				SQLWarning sqlw  = null;
				Statement stmnt  = _conn.createStatement();
				ResultSet  rs    = null;
				int rowsAffected = 0;

				if (_queryTimeout > 0)
					stmnt.setQueryTimeout(_queryTimeout);

				if (_logger.isDebugEnabled()) 
					_logger.debug("EXECUTING: "+sql);
//System.out.println("EXECUTING: -------------------------------------------------------------\n"+sql);
//				stmnt.executeUpdate(sql);
				boolean hasRs = stmnt.execute(sql);

				// iterate through each result set
				do
				{
					if(hasRs)
					{
						// Get next resultset to work with
						rs = stmnt.getResultSet();

						// Convert the ResultSet into a TableModel, which fits on a JTable
						ResultSetTableModel tm = new ResultSetTableModel(rs, true, sql);

						// Write ResultSet Content as a "string table"
						_logger.info(getMsgPrefix() + ": produced a ResultSet\n" + tm.toTableString());
//						_resultCompList.add(tm);

						// Check for warnings
						// If warnings found, add them to the LIST
						for (sqlw = rs.getWarnings(); sqlw != null; sqlw = sqlw.getNextWarning())
						{
							_logger.trace("--In loop, sqlw: "+sqlw);
							//compList.add(new JAseMessage(sqlw.getMessage()));
						}

						// Close it
						rs.close();
					}
					else
					{
						// Treat update/row count(s)
						rowsAffected = stmnt.getUpdateCount();
						if (rowsAffected >= 0)
						{
//							rso.add(rowsAffected);
						}
					}

					// Check if we have more resultsets
					hasRs = stmnt.getMoreResults();

					_logger.trace( "--hasRs="+hasRs+", rowsAffected="+rowsAffected );
				}
				while (hasRs || rowsAffected != -1);

				// Check for warnings
				for (sqlw = stmnt.getWarnings(); sqlw != null; sqlw = sqlw.getNextWarning())
				{
					_logger.trace("====After read RS loop, sqlw: "+sqlw);
					//compList.add(new JAseMessage(sqlw.getMessage()));
				}

				// Close the statement
				stmnt.close();
			}
			catch(SQLWarning w)
			{
				_logger.warn("Problems when executing sql: "+sql, w);
//				PluginSupport.LogInfoMessage(sqlwarning.getMessage(), MessageText.formatSQLExceptionDetails(sqlwarning));
			}
		}
	}

	
	
	
	/**
	 * 
	 * @param className using full package name spec "com.asetune.utils.ClassName"
	 * @param filename
	 * @throws SQLException
	 */
	public String executeSql(String className, String filename, final boolean aseExceptionsToWarnings)
	throws SQLException
	{
		try
		{
			Class<?> clazz = Class.forName(className);
			return executeSql(clazz, filename, aseExceptionsToWarnings);
		}
		catch(ClassNotFoundException e)
		{
		}
		return null;
	}

	public String executeSql(Class<?> clazz, String filename, final boolean aseExceptionsToWarnings)
	throws SQLException
	{
		try
		{
			URL url = clazz.getResource(filename);
			if(url != null)
			{
				BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
				br = new BufferedReader(new InputStreamReader(url.openStream()));
				String result = executeSql(br, aseExceptionsToWarnings);
				br.close();
				return result;
			}
		}
		catch(IOException e)
		{
		}
		return null;
	}

	public String executeSql(String filename, final boolean aseExceptionsToWarnings)
	throws SQLException
	{
		try
		{
			File file = new File(filename);
			if(file != null)
			{
//				if(!file.exists())
//					throw new SQLException(BundleManager.getString("ProviderRes", "ERR_SCRIPT_NOT_FND", file.getAbsolutePath()), "_RSM_", 5);
//				if(!file.canRead())
//					throw new SQLException(BundleManager.getString("ProviderRes", "ERR_READ_PERMISSIONS", file.getAbsolutePath()), "_RSM_", 4);

				FileReader fr = new FileReader(file);
				BufferedReader br = new BufferedReader(fr);
				String result = executeSql(br, aseExceptionsToWarnings);
				br.close();
				fr.close();
				return result;
			}
		}
		catch(IOException e)
		{
		}
		return null;
	}

	public String executeSqlStr(String sql, final boolean aseExceptionsToWarnings)
	throws SQLException
	{
		try
		{
			StringReader sr = new StringReader(sql);
			BufferedReader br = new BufferedReader(sr);
			String result = executeSql(br, aseExceptionsToWarnings);
			br.close();
			sr.close();
			return result;
		}
		catch(IOException e)
		{
		}
		return null;
	}

	public String executeSql(BufferedReader br, final boolean aseExceptionsToWarnings)
	throws SQLException, IOException
	{
		StringBuilder sb = new StringBuilder();

		SybMessageHandler oldMsgHandler = ((SybConnection)_conn).getSybMessageHandler();
		((SybConnection)_conn).setSybMessageHandler(new SybMessageHandler()
		{
			@Override
			public SQLException messageHandler(SQLException sqle)
			{
				if (aseExceptionsToWarnings)
					return AseConnectionUtils.sqlExceptionToWarning(sqle);
				else
				{
					if (AseConnectionUtils.isInLoadDbException(sqle))
						return AseConnectionUtils.sqlExceptionToWarning(sqle);
					return sqle;
				}
			}
		});

		String sql = "";
		try
		{
			for(String sqlChunc=readCommand(br); sqlChunc!=null; sqlChunc=readCommand(br))
			{
				// This can't be part of the for loop, then it just stops if empty row
				if ( StringUtil.isNullOrBlank(sqlChunc) )
					continue;

				Statement stmnt  = _conn.createStatement();
				ResultSet  rs    = null;
				int rowsAffected = 0;

				if (_queryTimeout > 0)
					stmnt.setQueryTimeout(_queryTimeout);

				sql = sqlChunc;
				if (_logger.isDebugEnabled()) 
					_logger.debug("EXECUTING: "+sql);

				boolean hasRs = stmnt.execute(sql);

				// iterate through each result set
				do
				{
					// Append, messages and Warnings to output, if any
					sb.append(getSqlWarningMsgs(stmnt, true));

					if(hasRs)
					{
						// Get next result set to work with
						rs = stmnt.getResultSet();

						// Append, messages and Warnings to output, if any
						sb.append(getSqlWarningMsgs(stmnt, true));

						// Convert the ResultSet into a TableModel, which fits on a JTable
						ResultSetTableModel tm = new ResultSetTableModel(rs, true, sql);

						// Write ResultSet Content as a "string table"
						sb.append(tm.toTableString());

						// Append, messages and Warnings to output, if any
						sb.append(getSqlWarningMsgs(stmnt, true));

						// Close it
						rs.close();
					}
					else
					{
    					// Treat update/row count(s)
    					rowsAffected = stmnt.getUpdateCount();
    					if (rowsAffected >= 0)
    					{
    					//	sb.append("("+rowsAffected+" row affected)\n");
    					}
					}

					// Check if we have more result sets
					hasRs = stmnt.getMoreResults(); 

					_logger.trace( "--hasRs="+hasRs+", rowsAffected="+rowsAffected );
				}
				while (hasRs || rowsAffected != -1);

				// Append, messages and Warnings to output, if any
				sb.append(getSqlWarningMsgs(stmnt, true));

				// Close the statement
				stmnt.close();
			}
		}
		catch(SQLWarning w)
		{
			_logger.warn("Problems when executing sql: "+sql, w);
		}
		finally
		{
			((SybConnection)_conn).setSybMessageHandler(oldMsgHandler);
		}
		return sb.toString();
	}

	private String getSqlWarningMsgs(Statement stmnt, boolean clearWawnings)
	{
		try
		{
			String out = getSqlWarningMsgs(stmnt.getWarnings());
			stmnt.clearWarnings();
			return out;
		}
		catch (SQLException e)
		{
		}
		return null;
	}

	private String getSqlWarningMsgs(SQLException sqe)
	{
		StringBuilder sb = new StringBuilder();
		while (sqe != null)
		{
			if(sqe instanceof EedInfo)
			{
				// Error is using the addtional TDS error data.
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
				// Now print the error or warning
				String msg = sqe.getMessage();
				sb.append(msg);
				if ( ! msg.endsWith("\n") )
					sb.append("\n");
			}
			else
			{
				// SqlState: 010P4 java.sql.SQLWarning: 010P4: An output parameter was received and ignored.
				if ( ! "010P4".equals(sqe.getSQLState()) )
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

	/**
	 *  count number of "statements" in a SQL string which constrains 'go' separators.
	 */
	public static int countSqlGoBatches(String sql)
	{
		int cmdCount = 0;
		try
		{
			BufferedReader br = new BufferedReader( new StringReader(sql) );
			for(String s1=AseSqlScript.readCommand(br); s1!=null; s1=AseSqlScript.readCommand(br))
				cmdCount++;
			br.close();
		}
		catch (IOException ex) 
		{
			_logger.error("While reading the input SQL 'go' String, caught: "+ex, ex);
		}
		return cmdCount;
	}


	/**
	 * Read from the buffer streamer untill we see 'go' on a separate row, the return the string<br>
	 * When end of file has been reached, and nothing has been read, return null.
	 * @param br
	 * @return see description above
	 * @throws IOException
	 */
//	public static String readCommand(BufferedReader br) 
//	throws IOException 
//	{
//		String outStr = "";
//		try 
//		{
//			String row = null;
//			for (row=br.readLine(); row!=null && !row.trim().equalsIgnoreCase("go"); row=br.readLine()) 
//			{
//				outStr = outStr + row + "\n";
//			}
//			if ( row == null && outStr.length() == 0)
//				return null;
//		} 
//		catch (IOException e) 
//		{
//			outStr = null;
//			throw e;
//		}
//		return (outStr == null) ? null : outStr.trim();
//	}
	
	public static String readCommand(BufferedReader br) 
	throws IOException 
	{
		StringBuilder sb = new StringBuilder();
		try 
		{
			// FIXME: add ability to support 'go 10', which executes this SQL statement 10 times
			//        also: countSqlGoBatches() needs to be fixed
			//        maybe add a class AseScriptReader, where readCommand() returns sql several times when go ## is present... 
			// SOLVED: in new Class AswSqlScriptReader
			String row = null;
			for (row=br.readLine(); row!=null && !row.trim().equalsIgnoreCase("go"); row=br.readLine()) 
			{
				sb.append(row).append("\n");
			}
			if ( row == null && sb.length() == 0)
				return null;
		} 
		catch (IOException e) 
		{
			sb = null;
			throw e;
		}
		return (sb == null) ? null : sb.toString();
	}

//	private String readCommand(BufferedReader br) 
//	throws IOException 
//	{
//		String s = "";
//		try 
//		{
//			for (String s1=br.readLine(); s1!=null && !s1.trim().equalsIgnoreCase("go"); s1=br.readLine()) 
//			{
//				s = s + s1 + "\n";
//			}
//		} 
//		catch (IOException e) 
//		{
//			s = null;
//			throw e;
//		}
//		return s.trim();
//	}
//	private String readCommand(BufferedReader br)
//	throws IOException
//	{
//		String cmd = "";
//		for(String row = br.readLine(); row != null && !row.trim().equalsIgnoreCase("go"); row = br.readLine())
//		{
//			cmd = (new StringBuilder()).append(cmd).append(row).append("\n").toString();
//		}
//		return cmd.trim();
//	}

//	protected File getFile(String s)
//	throws SQLException
//	{
//		File file = null;
//		if(s.indexOf(File.pathSeparator) == -1)
//		{
//			String s1 = RMEnv.getRMScriptsDir();
//			if(s1 == null)
//				throw new SQLException(BundleManager.getString("ProviderRes", "ERR_SYBROOT_NOT_SET"), "_RSM_", 6);
//			file = new File(s1, s);
//		} else
//		{
//			file = new File(s);
//		}
//		if(file != null)
//		{
//			if(!file.exists())
//				throw new SQLException(BundleManager.getString("ProviderRes", "ERR_SCRIPT_NOT_FND", file.getAbsolutePath()), "_RSM_", 5);
//			if(!file.canRead())
//				throw new SQLException(BundleManager.getString("ProviderRes", "ERR_READ_PERMISSIONS", file.getAbsolutePath()), "_RSM_", 4);
//		}
//		return file;
//	}

//	/**
//	* Message Handler method for a this SybConnection
//	* @param warn   SQLExeception object
//	* @return       SQLExeception object
//	*/
//	public SQLException messageHandlerXXX(SQLException sqle)
//	{
//		StringBuffer sb = new StringBuffer();
//		boolean discard = true;
//
////		if (sqle instanceof SybSQLException) 
//		if (sqle instanceof EedInfo) 
//		{
////			SybSQLException sybsqle = (SybSQLException) sqle;
//			EedInfo sybsqle = (EedInfo) sqle;
//			
//			if (sybsqle.getSeverity() > 10)
//				discard = false;
//
//			sb.append( "Srv=" );
//			sb.append( sybsqle.getServerName() );
//			sb.append( ", Msg=" );
//			sb.append( sqle.getErrorCode() );
//			sb.append( ", Severity=" );
//			sb.append( sybsqle.getSeverity() );
//			sb.append( ": " );
//			sb.append( sqle.getMessage().replaceAll("\n", "") );
//		}
//		else
//		{
//			sb.append( "Msg=" );
//			sb.append( sqle.getErrorCode() );
//			sb.append( ": " );
//			sb.append( sqle.getMessage().replaceAll("\n", "") );
//		}
//
//		if (discard)
//			_logger.info(getMsgPrefix() + sb.toString());
//		else
//			_logger.error(getMsgPrefix() + sb.toString());
//
//		if (discard)
//			return null;
//		return sqle;
//	}

	@Override
	public SQLException messageHandler(SQLException sqe)
	{
		boolean isInformational = false;
		StringBuffer m = new StringBuffer(500);

		@SuppressWarnings("unused")
		String threadName = " ThreadName='"+Thread.currentThread().getName()+"'.";

		@SuppressWarnings("unused")
		String procName = "";

		int msgNumber = sqe.getErrorCode();

		String message = sqe.getMessage();
		if ( message.endsWith("\n") )
		{
			message = message.replaceAll("\n", "");
		}


		isInformational = (msgNumber == 0) ? true : false;

		if (isInformational)
		{
			m.append(message);
		}
		else
		{
			m.append("Msg="); m.append(msgNumber);
			m.append(": "); m.append(message);
		}

		// Get procName
		if ( sqe instanceof EedInfo  && ((EedInfo)sqe).getProcedureName() != null )
		{
			procName = " ProcName='"+((EedInfo)sqe).getProcedureName()+"'.";
		}

		// If not print messages or other trace messages (showplan, statistics io, etc)
		// Add extra information to the message.
		if ( ! isInformational )
		{
			m.append(", SqlState='"); m.append(sqe.getSQLState()); m.append("'");

			if (sqe instanceof EedInfo)
			{
				m.append(", State=");       m.append(((EedInfo)sqe).getState());
				m.append(", Severity=");    m.append(((EedInfo)sqe).getSeverity());
				m.append(", ServerName=");  m.append(((EedInfo)sqe).getServerName());
				m.append(", ProcName=");    m.append(((EedInfo)sqe).getProcedureName());
				m.append(", LineNum=");     m.append(((EedInfo)sqe).getLineNumber());
				//m.append(", TranState=");   m.append(tdsTranStateToString(((EedInfo)sqe).getTranState()));

				// Reset this variable, since we already have it in the string from above
				procName = "";

				if ( ((EedInfo)sqe).getSeverity() <= 10 )
					isInformational = true;
			}

		}
		m.append(".");

		// Write a dummy message so we can trece from where this happened.
		if (_logger.isTraceEnabled())
			_logger.trace("Dummy Trace Message to locate from WHERE this was called.", new Exception("Dummy Trace Message to locate from WHERE this was called."));

		if (sqe instanceof SQLWarning)
		{
			if (isInformational)
			{
				_sqlMessage = "INFO: " + m.toString();
				_logger.info(getMsgPrefix() + m.toString());
			}
			else
			{
				_sqlMessage = "WARNING: " + m.toString();
				_logger.warn(getMsgPrefix() + m.toString());
			}
		}
		else
		{
			_sqlMessage = "ERROR: " + m.toString();
			_logger.error(getMsgPrefix() + m.toString());
		}

		return sqe;
	}
	
	
	
//	/**
//	 * This method uses the supplied SQL query string, and the
//	 * ResultSetTableModelFactory object to create a TableModel that holds
//	 * the results of the database query.  It passes that TableModel to the
//	 * JTable component for display.
//	 **/
//	// from com.asetune.gui.QueryWindow
//	private void displayQueryResults(Connection conn, String sql)
//	{
//		try
//		{
//			// If we've called close(), then we can't call this method
//			if (conn == null)
//				throw new IllegalStateException("Connection already closed.");
//
//			SQLWarning sqlw  = null;
//			Statement  stmnt = conn.createStatement();			
//			ResultSet  rs    = null;
//			int rowsAffected = 0;
//
//			// a linked list where to "store" result sets or messages
//			// before "displaying" them
//			_resultCompList = new ArrayList<ResultSetTableModel>();
//
//			_logger.debug("Executing SQL statement: "+sql);
//			// Execute
//			boolean hasRs = stmnt.execute(sql);
//
//			// iterate through each result set
//			do
//			{
//				if(hasRs)
//				{
//					// Get next resultset to work with
//					rs = stmnt.getResultSet();
//
//					// Convert the ResultSet into a TableModel, which fits on a JTable
//					ResultSetTableModel tm = new ResultSetTableModel(rs, true);
//					_resultCompList.add(tm);
//
//					// Check for warnings
//					// If warnings found, add them to the LIST
//					for (sqlw = rs.getWarnings(); sqlw != null; sqlw = sqlw.getNextWarning())
//					{
//						_logger.trace("--In loop, sqlw: "+sqlw);
//						//compList.add(new JAseMessage(sqlw.getMessage()));
//					}
//
//					// Close it
//					rs.close();
//				}
//
//				// Treat update/row count(s)
//				rowsAffected = stmnt.getUpdateCount();
//				if (rowsAffected >= 0)
//				{
////					rso.add(rowsAffected);
//				}
//
//				// Check if we have more resultsets
//				hasRs = stmnt.getMoreResults();
//
//				_logger.trace( "--hasRs="+hasRs+", rowsAffected="+rowsAffected );
//			}
//			while (hasRs || rowsAffected != -1);
//
//			// Check for warnings
//			for (sqlw = stmnt.getWarnings(); sqlw != null; sqlw = sqlw.getNextWarning())
//			{
//				_logger.trace("====After read RS loop, sqlw: "+sqlw);
//				//compList.add(new JAseMessage(sqlw.getMessage()));
//			}
//
//			// Close the statement
//			stmnt.close();
//		}
//		catch (SQLException ex)
//		{
//		}
//		
//		// In some cases, some of the area in not repainted
//		// example: when no RS, but only messages has been displayed
////		_resPanel.repaint();
//	}
	
//	public boolean executeGoString(String dbname, int timeout, String msgHandlerPrefix, String goStr)
//	{
//		String cmd = "";
//
//		SybMessageHandler oldMsgHandler = ((SybConnection)_conn).getSybMessageHandler();
//		try
//		{
//			// Check that we have a connection
//			if ( ! AseConnectionUtils.isConnectionOk(_conn, false, null) )
//			{
//				_logger.error("Connection is already closed. Can't execute the SQL script.");
//			}
//				
//			if ( dbname != null && !dbname.equals("") )
//			{
//				AseConnectionUtils.useDbname(_conn, dbname);
//			}
//
//			((SybConnection)_conn).setSybMessageHandler(new AseMessageHandlerPrintToLog(msgHandlerPrefix));
//
//			// EXECUTE
//			Statement stmt = _conn.createStatement();
//			
//			if (timeout > 0)
//				stmt.setQueryTimeout( timeout );
//
//			BufferedReader br = new BufferedReader( new StringReader(goStr) );
//			for(String s1=readCommand(br); s1!=null; s1=readCommand(br))
//			{
//				// This can't be part of the for loop, then it just stops if empty row
//				if ( StringUtil.isNullOrBlank(s1) )
//					continue;
//
//				cmd = s1;
//
//				_logger.debug("EXECUTING: "+cmd);
//				stmt.executeUpdate(cmd);
//			}
//			br.close();
//			
//			stmt.close();
//		}
//		catch (IOException e)
//		{
//			String msg = "Problems when executing '"+cmd+"' in DB '"+AseConnectionUtils.getCurrentDbname(_conn)+"'.";
//			_logger.error(msg);
//			return false;
//		}
//		catch (SQLException sqle)
//		{
//			// Check for timeouts etc
//			//checkForProblems(sqle, cmd);
//
//			String msg = "Problems when executing '"+cmd+"' in DB '"+AseConnectionUtils.getCurrentDbname(_conn)+"'.";
//			_logger.error(msg + AseConnectionUtils.sqlExceptionToString(sqle));
//			return false;
//		}
//		finally
//		{
//			((SybConnection)_conn).setSybMessageHandler(oldMsgHandler);
//		}
//		return true;
//	}


//	private class AseMessageHandlerPrintToLog
//	implements SybMessageHandler 
//	{
//		private String _prefix = ""; 
//
//		public AseMessageHandlerPrintToLog(String prefix)
//		{
//			if (prefix != null)
//				_prefix = prefix;
//		}
//		
//		@Override
//		public SQLException messageHandler(SQLException sqle) 
//		{
//			StringBuffer sb = new StringBuffer();
//			boolean discard = true;
//
////			if (sqle instanceof SybSQLException) 
//			if (sqle instanceof EedInfo) 
//			{
////				SybSQLException sybsqle = (SybSQLException) sqle;
//				EedInfo sybsqle = (EedInfo) sqle;
//				
//				if (sybsqle.getSeverity() > 10)
//					discard = false;
//
//				sb.append( "Srv=" );
//				sb.append( sybsqle.getServerName() );
//				sb.append( ", Msg=" );
//				sb.append( sqle.getErrorCode() );
//				sb.append( ", Severity=" );
//				sb.append( sybsqle.getSeverity() );
//				sb.append( ": " );
//				sb.append( sqle.getMessage().replaceAll("\n", "") );
//			}
//			else
//			{
//				sb.append( "Msg=" );
//				sb.append( sqle.getErrorCode() );
//				sb.append( ": " );
//				sb.append( sqle.getMessage().replaceAll("\n", "") );
//			}
//
//			if (discard)
//				_logger.info(_prefix + sb.toString());
//			else
//				_logger.error(_prefix + sb.toString());
//
//			if (discard)
//				return null;
//			return sqle;
//		}
//		
//	}
	
	
	
	//----------------------------------------------------------------------
	//----------------------------------------------------------------------
	//----------------------------------------------------------------------
	// SOME TEST CODE
	//----------------------------------------------------------------------
	//----------------------------------------------------------------------
	//----------------------------------------------------------------------
	public static void main(String[] args)
	{
		Properties log4jProps = new Properties();
		//log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.rootLogger", "DEBUG, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		Configuration conf1 = new Configuration(Version.APP_STORE_DIR + "/asetune.save.properties");
		Configuration.setInstance(Configuration.USER_TEMP, conf1);
		
		Configuration.setSearchOrder(Configuration.USER_TEMP);

		System.setProperty("ASETUNE_SAVE_DIR", "c:/projects/asetune/data");
		
		// DO THE THING
		try
		{
			System.out.println("Open DB connection.");

			AseConnectionFactory.setAppName("xxx");
			AseConnectionFactory.setUser("sa");
			AseConnectionFactory.setPassword("");
//			AseConnectionFactory.setHostPort("sweiq-linux", "2750");
			AseConnectionFactory.setHostPort("gorans-xp", "5000");
//			AseConnectionFactory.setHostPort("gorans-xp", "15700");
			
			final Connection conn = AseConnectionFactory.getConnection();
			
			String dbname  = "perfdemo";
			String owner   = "dbo";
			String objname = "DestTab1";

			String sql;
			sql = "exec sp_who\ngo\n";
			sql = "exec model..sp__optdiag 'dbo.sysobjects'\ngo\n";

			sql="declare @partitions int \n" +
				"select @partitions = count(*) \n" +
				"from "+dbname+"..sysobjects o, "+dbname+"..sysusers u, "+dbname+"..syspartitions p \n" +
				"where o.name = '"+objname+"' \n" +
				"  and u.name = '"+owner+"' \n" +
				"  and o.id  = p.id \n" +
				"  and o.uid = o.uid \n" +
				"  and p.indid = 0 \n" +
				"                  \n" +
				"if (@partitions > 1) \n" +
				"    print 'Table is partitioned, and this is not working so well with sp__optdiag, sorry.' \n" +
				"else \n" +
				"    exec "+dbname+"..sp__optdiag '"+owner+"."+objname+"' \n" +
				"print 'xxxxxxxxxxxxxxx' \n" +
				"raiserror 99999 'test raise error...' \n" +
				"print 'yyyyyyyyyyyyyyy' \n" +
				"go\n" +
				"raiserror 99999 'test raise error...' \n" +
				"print 'adflkashfdlkajshf' \n" +
				"raiserror 99999 'test raise error...' \n" +
				"\n";

			AseSqlScript ss = new AseSqlScript(conn, 10);
			try	{ 
				System.out.println("NORMAL:\n" + ss.executeSqlStr(sql, true) ); 
			} catch (SQLException e) { 
				System.out.println("EXCEPTION:\n" + e.toString() ); 
				e.printStackTrace();
			} finally {
				ss.close();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
