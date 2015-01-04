package com.asetune.utils;

import java.awt.Component;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import com.asetune.gui.ConnectionDialog;
import com.asetune.gui.ResultSetTableModel;

public class DbUtils
{
	private static Logger _logger = Logger.getLogger(DbUtils.class);
	
	public static final String DB_PROD_NAME_SYBASE_ASE = "Adaptive Server Enterprise";
	public static final String DB_PROD_NAME_SYBASE_ASA = "SQL Anywhere";
	public static final String DB_PROD_NAME_SYBASE_IQ  = "Sybase IQ";
	public static final String DB_PROD_NAME_SYBASE_RS  = "Replication Server";
	public static final String DB_PROD_NAME_H2         = "H2";
	public static final String DB_PROD_NAME_HANA       = "HDB";
	public static final String DB_PROD_NAME_MAXDB      = "SAP DB";

	public static final String DB_PROD_NAME_HSQL       = "HSQL Database Engine"; // got this from web, so might not be correct
	public static final String DB_PROD_NAME_MSSQL      = "Microsoft SQL Server"; // got this from web, so might not be correct
	public static final String DB_PROD_NAME_ORACLE     = "Oracle";               // got this from web, so might not be correct
	public static final String DB_PROD_NAME_DB2_UX     = "DB2/Linux";            // got this from web, so might not be correct
	public static final String DB_PROD_NAME_DB2_ZOS    = "DB2";                  // got this from web, so might not be correct
	public static final String DB_PROD_NAME_MYSQL      = "MySQL";                // got this from web, so might not be correct
	public static final String DB_PROD_NAME_DERBY      = "Apache Derby";         // got this from web, so might not be correct


	/**
	 * Check if the specified product name is in any of the passed ones
	 * @param checkProductName product name to check
	 * @param prodNameList     Product names to be checked...
	 * @return true if part of the specified "list"
	 */
	public static boolean isProductName(String checkProductName, String... prodNameList)
	{
		if (checkProductName == null)
			return false;
		
		for (int i=0; i<prodNameList.length; i++)
		{
			String name = prodNameList[i];
			if (name == null)
				continue;

			if (checkProductName.equals(name))
				return true;

			// Special stuff for DB2... since it's not a static value
			// for DB2 Linux it looks like: 'DB2/LINUXX8664'
			if (DB_PROD_NAME_DB2_UX.equals(name))
			{
				if (checkProductName.toLowerCase().startsWith(name.toLowerCase()))
					return true;
			}
		}
		return false;
	}

	/**
	 * Simply calls conn.getMetaData().getDatabaseProductName() to get the ProductName<br>
	 * If SQLException a "" blank string will be returned.
	 * @param conn
	 * @return The product name (or "" on SQLExceptions)
	 */
	public static String getProductName(Connection conn)
	{
		if (conn == null)
			throw new RuntimeException("DbUtils.setAutoCommit() conn=null, So I can't continue");

		try
		{
			DatabaseMetaData md = conn.getMetaData();
			return md.getDatabaseProductName();
		}
		catch (SQLException ex)
		{
			return "";
		}
	}

	/**
	 * Calls getAutoCommit(), but catches errors<br>
	 * If errors true will be returned, since that is the normal default for JDBC
	 * @return
	 */
	public static boolean getAutoCommitNoThrow(Connection conn, String dbVendorName)
	{
		try
		{
			return getAutoCommit(conn, dbVendorName);
		}
		catch (SQLException e)
		{
			_logger.warn("Problems when calling getAutoCommit(). Caught: "+e);
			return true;
		}
	}

	/**
	 * Local method to get AutoCommit, in jConnect the value seems to be cached... <br>
	 * so if you change it at the server with 'set chained on|off' the return value might be changed.
	 * @return true=inAutoCommit(un-chained-mode), false=notInAutoCommit(chained-mode)
	 * @throws SQLException
	 */
	public static boolean getAutoCommit(Connection conn, String dbVendorName)
	throws SQLException
	{
		boolean isAutoCommit = true; // well this is the default...
		
		// Go and get the vendor if not specified
		if (StringUtil.isNullOrBlank(dbVendorName))
			dbVendorName = getProductName(conn);

		if (DbUtils.isProductName(dbVendorName, DbUtils.DB_PROD_NAME_SYBASE_ASE))
		{
			int atatTranChained = -1;				
			
			Statement stmnt = conn.createStatement();
			ResultSet rs    = stmnt.executeQuery("select @@tranchained");
			while (rs.next())
				atatTranChained = rs.getInt(1);

			rs.close();
			stmnt.close();

			if (atatTranChained != -1)
				isAutoCommit = (atatTranChained == 0 ? true : false);
		}
		else
		{
			isAutoCommit = conn.getAutoCommit();
		}
		
		return isAutoCommit;
	}

	/**
	 * Sets the AutoCommit value<br>
	 * But before, check the current mode (based on extended logic, for ASE get @@tranchained instead of getAutoCommit())<br>
	 * If there is problems, ask a question how to proceed (Commit/Rollback/Cancel)<br>
	 * <br>
	 * NOTE: The GUI handler will simply be null...
	 *  
	 * @param conn          The connection to do it on
	 * @param dbVendorName  Vendor name, so we can do this differently based on what type we are connected to
	 * @param toValue       set AutoCommit to the value
	 * @param calledFrom    If this fail, what extra message do you want to have in the dialog (html tags can be used)
	 * 
	 * @return              The value which was set. (fetched with extended getAutoCommit())
	 */
	public static boolean setAutoCommit(Connection conn, String dbVendorName, boolean toValue, String calledFrom)
	{
		return setAutoCommit(conn, dbVendorName, null, toValue, calledFrom);
	}

	/**
	 * Sets the AutoCommit value<br>
	 * But before, check the current mode (based on extended logic, for ASE get @@tranchained instead of getAutoCommit())<br>
	 * If there is problems, ask a question how to proceed (Commit/Rollback/Cancel)
	 *  
	 * @param conn          The connection to do it on
	 * @param dbVendorName  Vendor name, so we can do this differently based on what type we are connected to
	 * @param owner         GUI handle, if we got oen (null can be passed)
	 * @param toValue       set AutoCommit to the value
	 * @param calledFrom    If this fail, what extra message do you want to have in the dialog (html tags can be used)
	 * 
	 * @return              The value which was set. (fetched with extended getAutoCommit())
	 */
	public static boolean setAutoCommit(Connection conn, String dbVendorName, Component owner, boolean toValue, String calledFrom)
	{
		if (conn == null)
			throw new RuntimeException("DbUtils.setAutoCommit() conn=null, So I can't continue");

		if (calledFrom == null)
			calledFrom = "";
		
		// Go and get the vendor if not specified
		if (StringUtil.isNullOrBlank(dbVendorName))
			dbVendorName = getProductName(conn);

		try 
		{
			// The jConnect driver seems to "cache" AutoCommit value, so if it's changed at the server side, the getAutoCommit() seems to show wrong value
			boolean isAutoCommit = getAutoCommit(conn, dbVendorName);
			if (isAutoCommit != toValue)
			{
				_logger.info("Setting JDBC AutoCommit to: " + toValue + (toValue ? " (unchained/normal mode)" : " (chained mode)") );

				if (DbUtils.isProductName(dbVendorName, DbUtils.DB_PROD_NAME_SYBASE_ASE))
				{
					if (_logger.isDebugEnabled())
						_logger.debug("setAutoCommit("+dbVendorName+"): Special logic for Sybase ASE.");

					Statement stmnt = conn.createStatement();
					stmnt.executeUpdate(toValue ? "set chained off" : "set chained on"); // set chained on|off: true=OFF, false=ON

					// If jConnect has MessageHandler, the exception might be down graded to warnings... so check for this... and Throw that as an Exception
					AseConnectionUtils.checkSqlWarningsAndThrowSqlExceptionIfSeverityIsAbove10(stmnt.getWarnings());
					AseConnectionUtils.checkSqlWarningsAndThrowSqlExceptionIfSeverityIsAbove10(conn.getWarnings());

					stmnt.close();
				}
				else
				{
					conn.setAutoCommit(toValue);
				}
			}
		}
		catch (SQLException ex)
		{
			// If it failed, for Sybase ASE: get lockCount, if it holds locks: prompt COMMIT/ROLLBACK+setAutoCommit() else COMMIT+setAutoCommit()
			String sybLockHtmlTable = null;
			int sybLockCount = -1;
			if (DbUtils.isProductName(dbVendorName, DbUtils.DB_PROD_NAME_SYBASE_ASE))
			{
				String sql = "select dbname=db_name(dbid), table_name=object_name(id, dbid), lock_type=type, lock_count=count(*) "
						+ " from master.dbo.syslocks "
						+ " where spid = @@spid	"
						+ " group by dbid, id, type ";
				
//				String sql = "select count(*) from master.dbo.syslocks where spid = @@spid";
				try 
				{
					sybLockCount = 0;

//					StringBuilder sb = new StringBuilder("<TABLE BORDER=1 style=\"background-color:#FFFFE0;color:black;border:1px solid #BDB76B;\"");
//					sb.append("<TR style=\"background-color:#BDB76B;color:white;\"> <TH>DB</TH> <TH>Table</TH> <TH>Type</TH> <TH>Count</TH> </TR>");
					StringBuilder sb = new StringBuilder("<TABLE BORDER=1>");
					sb.append("<TR> <TH>DB</TH> <TH>Table</TH> <TH>Type</TH> <TH>Count</TH> </TR>");

					Statement stmnt = conn.createStatement();
					ResultSet rs = stmnt.executeQuery(sql); // set chained on|off: true=OFF, false=ON
					while (rs.next())
					{
						sybLockCount++;
//						sybLockCount = rs.getInt(1);
						sb.append("<TR>");
						sb.append("<TD>").append(                                  rs.getString(1)) .append("</TD>");
						sb.append("<TD>").append(                                  rs.getString(2)) .append("</TD>");
						sb.append("<TD>").append(AseConnectionUtils.getAseLockType(rs.getInt   (3))).append("</TD>");
						sb.append("<TD>").append(                                  rs.getString(4)) .append("</TD>");
						sb.append("</TR>");
					}
					rs.close();
					sb.append("</TABLE>");
					sb.append("<BR>");
					sb.insert(0, "User <b>holds "+sybLockCount+" locks</b> in the server. Below is a summary table with current locks.<br>");
					sybLockHtmlTable = sb.toString();

					// If jConnect has MessageHandler, the exception might be down graded to warnings... so check for this... and Throw that as an Exception
					AseConnectionUtils.checkSqlWarningsAndThrowSqlExceptionIfSeverityIsAbove10(stmnt.getWarnings());
					AseConnectionUtils.checkSqlWarningsAndThrowSqlExceptionIfSeverityIsAbove10(conn.getWarnings());

					stmnt.close();
				}
				catch (SQLException ex2) { _logger.warn("Problem checking if we are holding locks in the server. SQL='"+sql+"'. Caught: "+ex2); }
			}

			// IF ASE and Lock Count is 0: simply do COMMIT and try to setAutoCommit() again
			if (sybLockCount == 0)
			{
				_logger.info("setAutoCommit(): problems, but when examin the syslocks, the current SPID holds NO locks. So I will COMMIT and try setAutoCommit("+toValue+") again. BTW Exception Caught when trying setAutoCommit("+toValue+") the first time: "+ex.getMessage().replace('\n', ' '));
				try
				{
					conn.commit();
					conn.setAutoCommit(toValue);
				}
				catch(SQLException ex2)
				{
					SwingUtils.showErrorMessage(owner, "setAutoCommit problem", "Sorry, Problems issuing JDBC setAutoCommit("+toValue+") on the connection.", ex);
				}
			}
			else // NOT SYBASE ASE or spid is holding locks...
			{
    			String htmlMsg = "<html>"
    					+ "Problems issuing JDBC setAutoCommit(<b>"+toValue+"</b>) on the connection<br>"
    					+ "<br>"
    					+ calledFrom + (StringUtil.isNullOrBlank(calledFrom) ? "" : "<br><br>") // Add newlines if a text was provided
    					+ (sybLockHtmlTable == null ? "" : sybLockHtmlTable)
    					+ "<b>SQLException</b>: "
    					+ "<table BORDER=0 CELLSPACING=1 CELLPADDING=0>"
    					+ "  <tr> <td><b>Error:   </b></td> <td>&nbsp;<code>" + ex.getErrorCode()                     + "</code></td> </tr>"
    					+ "  <tr> <td><b>SQLState:</b></td> <td>&nbsp;<code>" + ex.getSQLState()                      + "</code></td> </tr>"
    					+ "  <tr> <td><b>Message: </b></td> <td>&nbsp;<code>" + ex.getMessage().replace("\n", "<br>") + "</code></td> </tr>"
    					+ "</table>"
    					+ "<br>"
    					+ "My guess is that some <i>explicit</i> or <i>implicit</i> change was done on the system<br>"
    					+ "Please try one of the below methods to solve the issue."
    					+ "<ul>"
    					+ "  <li>Commit - Will commit current transaction and then retry the setAutoCommit(<b>"+toValue+"</b>)</li>"
    					+ "  <li>Rollback - Will rollback current transaction and then retry the setAutoCommit(<b>"+toValue+"</b>)</li>"
    					+ "  <li>Cancel - Will simply do nothing and just continues.</li>"
    					+ "</ul>"
    					+ "</html>";
    			Object[] buttons = {"Commit and Retry", "Rollback and Retry", "Cancel"};
    			int answer = JOptionPane.showOptionDialog(owner, 
    					htmlMsg,
    					"Problems issuing JDBC setAutoCommit", 
    					JOptionPane.DEFAULT_OPTION,
    					JOptionPane.QUESTION_MESSAGE,
    					null,
    					buttons,
    					buttons[0]);
    
    			// 0=COMMIT / 1=ROLLBACK 
    			if (answer == 0 || answer == 1) 
    			{
    				try
    				{
    					if (answer == 0)
    						conn.commit();
    					else
    						conn.rollback();
    						
    					conn.setAutoCommit(toValue);
    				}
    				catch(SQLException ex2)
    				{
    					SwingUtils.showErrorMessage(owner, "setAutoCommit problem", "Sorry, there were still Problems issuing JDBC setAutoCommit("+toValue+") on the connection.", ex);
    				}
    			}
    			// CANCEL
    			else 
    			{
    			}
			}
		} // end: catch

		// Get the current value of AutoCommit
		return getAutoCommitNoThrow(conn, dbVendorName);
	}

	/**
	 * Get server name for various vendors
	 * @param conn
	 * @param dbProductName
	 * @return
	 */
	public static String getDatabaseServerName(Connection conn, String dbProductName)
	{
		if (conn == null)
			return "";

		String srvName = null;
		try
		{
			if (StringUtil.isNullOrBlank(dbProductName))
				dbProductName = ConnectionDialog.getDatabaseProductName(conn);
			
			srvName = ConnectionDialog.getDatabaseServerName(conn);
		}
		catch (SQLException ignore) {}

		if (StringUtil.isNullOrBlank(srvName))
		{
			try { srvName = conn.getMetaData().getURL(); }
			catch (Throwable ignore) {}

//			if (StringUtil.isNullOrBlank(srvName))
//				srvName = conn.toString();
		}

		return srvName;
	}

	/**
	 * Execute a SQL Statement, if any ResultSets are produced, they will be returned as a List
	 * 
	 * @param conn a valid connection
	 * @param sql SQL Statement to execute in the server
	 * @return a list of ResultSets if any
	 * @throws SQLException if we had problems
	 */
	public static List<ResultSetTableModel> exec(Connection conn, String sql)
	throws SQLException
	{
		ArrayList<ResultSetTableModel> rsList = new ArrayList<ResultSetTableModel>();

		if (conn == null)
			return rsList;

		Statement stmnt = conn.createStatement();

		boolean hasRs = stmnt.execute(sql);
		int rowsAffected = 0;

		// iterate through each result set
		do
		{
			if(hasRs)
			{
				// Get next ResultSet to work with
				ResultSet rs = stmnt.getResultSet();
				
				ResultSetTableModel tm = new ResultSetTableModel(rs, sql);
				rsList.add(tm);

				// Close it
				rs.close();
			}
			else
			{
				rowsAffected = stmnt.getUpdateCount();
				if (rowsAffected >= 0)
				{
				}
			}

			// Check if we have more ResultSets
			hasRs = stmnt.getMoreResults();
		}
		while (hasRs || rowsAffected != -1);

		// Close the statement
		stmnt.close();
		
		return rsList;
	}

	/**
	 * Parse SQL trying to find out first line where a SQL Statement is
	 * <p>
	 * Basically disregards comments, empty lines etc...
	 * 
	 * @param sql text to "parse"
	 * @return minimum 1
	 */
	public static int getLineForFirstStatement(String sql)
	{
		if (sql == null)
			return 0;

		if ("".equals(sql))
			return 1;

		int rowNumber = 0;
		boolean inMultiLineComment = false;

		Scanner scanner = new Scanner(sql);
		while (scanner.hasNextLine()) 
		{
			rowNumber++;
			String lineStr = scanner.nextLine().trim();

			// Multi line comments, this takes a bit processing
			//------------------------------------------------

			// simple comments: /* some text */
			if (lineStr.startsWith("/*") && lineStr.endsWith("*/")) 
				continue; // get next line

			// hmm simple comments but something after comment: /* some text */ XXXX<-This is a Statement
			// Just remove this part from the str, and continue parsing
			if (lineStr.startsWith("/*") && lineStr.indexOf("*/") > 0)
				lineStr = lineStr.substring(lineStr.indexOf("*/")+2).trim();

			// Now try multiLine comment, START
			if (lineStr.startsWith("/*"))
			{
				inMultiLineComment = true;
				continue; // next row
			}

			// LAST of the row comment END: */
			if (lineStr.endsWith("*/"))
			{
				inMultiLineComment = false;
				continue; // next line
			}

			// Comment ENDS, but not at the end of the row: end of comment */ XXXX<-This is a Statement 
			if (lineStr.indexOf("*/") >= 0)
			{
				inMultiLineComment = false;
				lineStr = lineStr.substring(lineStr.indexOf("*/")+2).trim();
			}
				
			// If we are in Multi Line Comment, just read next row
			if (inMultiLineComment)
				continue;

			// Nothing left in the string, get next line
			if (lineStr.equals(""))
				continue;

			// The rest is comment, get next line
			if (lineStr.startsWith("--"))  
				continue;

			// Finally something that looks like a statement... 
			return rowNumber;
		}
		return rowNumber;
	}
	
	//------------------------------------------------------------------------------
	//------------------------------------------------------------------------------
	// BEGIN HANA helper methods
	//------------------------------------------------------------------------------
	//------------------------------------------------------------------------------
	
	public static String getHanaServername(Connection conn)
	{
		final String UNKNOWN = "UNKNOWN";

		// FIXME: move this to DbUtils
		if ( ! AseConnectionUtils.isConnectionOk(conn) )
			return UNKNOWN;

		String retStr = UNKNOWN;
		String sql    = "select DATABASE_NAME from m_database";

		try
		{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				retStr = rs.getString(1).trim();
			}
			rs.close();
			stmt.close();

			return retStr;
		}
		catch (SQLException e)
		{
			_logger.debug("When getting HANA Server Instance Name. sql='"+sql+"', Caught exception.", e);

			return UNKNOWN;
		}
	}

	/**
	 * Try to extract he procedure or function name from a HANA error message
	 * <p>
	 * Example of HANA messages
	 * <pre>
	 * 1> select * FROM OBJECTS WHERE xxx = xxx
	 * HANA: ErrorCode 260, SQLState HY000, ExceptionClass: com.sap.db.jdbc.exceptions.JDBCDriverException
	 * SAP DBTech JDBC: [260] (at 28): invalid column name: XXX: line 1 col 29 (at pos 28)
	 * 
	 * 1> call sp_dbmtk_proc_install_final('gs_p1', 'procedure')
	 * HANA: ErrorCode 19999, SQLState HY000, ExceptionClass: com.sap.db.jdbc.exceptions.JDBCDriverException
	 * SAP DBTech JDBC: [19999]: user-defined error:  [19999] "SYSTEM"."SP_DBMTK_PROC_INSTALL_FINAL": line 58 col 3 (at pos 1705): [19999] (range 3) user-defined error exception: user-defined error:  [19999] "SYSTEM"."SP_DBMTK_ABORT_SESSION": line 56 col 3 (at pos 1929): [19999] (range 3) user-defined error exception: *** Error while installing PROCEDURE SYSTEM.GS_P1  Object was not found.
	 * 
	 * 1> call gs_p1
	 * HANA: ErrorCode 304, SQLState 22012, ExceptionClass: com.sap.db.jdbc.exceptions.jdbc40.SQLDataException
	 * [304]: division by zero undefined:  [304] "SYSTEM"."GS_P1": line 5 col 2 (at pos 54): [304] (range 3) division by zero undefined exception: division by zero undefined:  at function /()
	 * </pre>
	 * @param msg the message...
	 * @return null if not found, otherwise SCHEMA.OBJECTNAME
	 */
	public static String parseHanaMessageForProcName(String msg)
	{
		if (msg == null)
			return null;

		int linePos = msg.indexOf(" line ");
		if (linePos > 0)
		{
			String objName = null;

			// Find first ']' to the LEFT of " line "
			int bracketPos = 0;
			for (int p=linePos; p>0; p--)
			{
				if (msg.charAt(p) == ']')
				{
					bracketPos = p;
					break;
				}
			}
			if (bracketPos > 0)
			{
				int firstQuote = msg.indexOf('"', bracketPos);
				if (firstQuote > 0 && firstQuote < linePos)
				{
					objName = msg.substring(bracketPos+1, linePos-1); // linePos-1 = get rid of the ':' before " line "
					objName = objName.replace("\"", "").trim(); // remove all " and spaces
				}
			}
			return objName;
		}
		
		return null;
	}


	/**
	 * Get a stored procedure or function text
	 * 
	 * @param conn the Connection to the database
	 * @param objectName in the form SCHEMA.OBJECTNAME
	 * @return null if it can't be found in database, otherwise the text definition
	 */
	public static String getHanaObjectText(Connection conn, String objectName)
	{
		if (conn == null)                return null;
		if (objectName == null)          return null;
		if (objectName.indexOf(".") < 0) return null;

		// remove any " chars
		int firstDot = objectName.indexOf(".");
		String schema = objectName.substring(0, firstDot).trim();
		String name   = objectName.substring(firstDot+1).trim();

		String sqlGetObjType  = "select OBJECT_TYPE from PUBLIC.OBJECTS    where SCHEMA_NAME = '"+schema+"' and OBJECT_NAME    = '"+name+"'";
		String sqlGetProcText = "select DEFINITION  from PUBLIC.PROCEDURES where SCHEMA_NAME = '"+schema+"' and PROCEDURE_NAME = '"+name+"'";
		String sqlGetFuncText = "select DEFINITION  from PUBLIC.FUNCTIONS  where SCHEMA_NAME = '"+schema+"' and FUNCTION_NAME  = '"+name+"'";

		try
		{
			Statement stmnt   = conn.createStatement();
			ResultSet rs      = stmnt.executeQuery(sqlGetObjType);
			String objType = "";
			while (rs.next())
				objType = rs.getString(1);

			String sql = null;
			if      (objType.equalsIgnoreCase("PROCEDURE")) sql = sqlGetProcText;
			else if (objType.equalsIgnoreCase("FUNCTION"))  sql = sqlGetFuncText;
			else
			{
				_logger.warn("getHanaObjectText(objectName='"+objectName+"'), was of type '"+objType+"', which isn't implemented yet, returning null");
				return null;
			}

			StringBuilder sb = new StringBuilder();

			rs = stmnt.executeQuery(sql);
			while (rs.next())
				sb.append(rs.getString(1));

			return sb.toString();
		}
		catch(SQLException e)
		{
			_logger.warn("Problems getting procedure/function text for '"+objectName+"' from HANA. Error Number: "+e.getErrorCode()+", Message: " + e.getMessage());
		}
		return null;
	}
	
	//------------------------------------------------------------------------------
	//------------------------------------------------------------------------------
	// END HANA helper methods
	//------------------------------------------------------------------------------
	//------------------------------------------------------------------------------

	
	//------------------------------------------------------------------------------
	//------------------------------------------------------------------------------
	// BEGIN Oracle helper methods
	//------------------------------------------------------------------------------
	//------------------------------------------------------------------------------
	
	public static String getOracleServername(Connection conn)
	{
		final String UNKNOWN = "UNKNOWN";

		// FIXME: move this to DbUtils
		if ( ! AseConnectionUtils.isConnectionOk(conn) )
			return UNKNOWN;

		String retStr = UNKNOWN;
		String sql    = "select sys_context('USERENV','DB_NAME') as Instance from dual";

		try
		{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				retStr = rs.getString(1).trim();
			}
			rs.close();
			stmt.close();

			return retStr;
		}
		catch (SQLException e)
		{
			_logger.debug("When getting Oracle Server Instance Name. sql='"+sql+"', Caught exception.", e);

			return UNKNOWN;
		}
	}

	public static String getOracleCharset(Connection conn)
	{
		final String UNKNOWN = "UNKNOWN";

		// FIXME: move this to DbUtils
		if ( ! AseConnectionUtils.isConnectionOk(conn) )
			return UNKNOWN;

		String retStr = UNKNOWN;
		String sql    = "select VALUE from NLS_DATABASE_PARAMETERS where PARAMETER = 'NLS_CHARACTERSET'";

		try
		{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				retStr = rs.getString(1).trim();
			}
			rs.close();
			stmt.close();

			return retStr;
		}
		catch (SQLException e)
		{
			_logger.debug("When getting Oracle Server CharacterSet. sql='"+sql+"', Caught exception.", e);

			return UNKNOWN;
		}
	}

	public static String getOracleSortorder(Connection conn)
	{
		final String UNKNOWN = "UNKNOWN";

		// FIXME: move this to DbUtils
		if ( ! AseConnectionUtils.isConnectionOk(conn) )
			return UNKNOWN;

		String retStr = UNKNOWN;
		String sql    = "select VALUE from NLS_DATABASE_PARAMETERS where PARAMETER = 'NLS_SORT'"; 

		try
		{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				retStr = rs.getString(1).trim();
			}
			rs.close();
			stmt.close();

			return retStr;
		}
		catch (SQLException e)
		{
			_logger.debug("When getting Oracle Server Sort Order. sql='"+sql+"', Caught exception.", e);

			return UNKNOWN;
		}
	}
	/**
	 * NOTE: <b>not yet tested</b><br>
	 * Get a stored procedure or function text
	 * 
	 * @param conn       Connection to the database
	 * @param objectName Name of the procedure/view/trigger... in the form SCHEMA.OBJNAME
	 * @return Text of the procedure/view/trigger...
	 */
	public static String getOracleObjectText(Connection conn,String objectName)
	{
		String returnText = null;
		
		if (conn == null)                return null;
		if (objectName == null)          return null;
		if (objectName.indexOf(".") < 0) return null;

		// split schema and object name
		int firstDot = objectName.indexOf(".");
		String schema = objectName.substring(0, firstDot).trim().toUpperCase();
		String name   = objectName.substring(firstDot+1).trim().toUpperCase();


		//--------------------------------------------
		// GET OBJECT TEXT
		String sql =
			"SELECT text \n" +
			"FROM all_source \n" +
			"WHERE owner = '"+schema+"' \n" +
			"  AND name  = '"+name+"' \n" +
			"ORDER BY line \n";
		try
		{
			StringBuilder sb = new StringBuilder();

			Statement statement = conn.createStatement();
			ResultSet rs = statement.executeQuery(sql);
			while(rs.next())
			{
				String textPart = rs.getString(1);
				sb.append(textPart);
			}
			rs.close();
			statement.close();

			if (sb.length() > 0)
				returnText = sb.toString();
		}
		catch (SQLException e)
		{
			returnText = null;
			_logger.warn("Problems getting text for Oracle object '"+objectName+"'. Caught: "+e); 
		}

		return returnText;
	}
	//------------------------------------------------------------------------------
	//------------------------------------------------------------------------------
	// END Oracle helper methods
	//------------------------------------------------------------------------------
	//------------------------------------------------------------------------------

	
	//------------------------------------------------------------------------------
	//------------------------------------------------------------------------------
	// BEGIN DB2 helper methods
	//------------------------------------------------------------------------------
	//------------------------------------------------------------------------------
	
	public static String getDb2Servername(Connection conn)
	{
		final String UNKNOWN = "UNKNOWN";

		// FIXME: move this to DbUtils
		if ( ! AseConnectionUtils.isConnectionOk(conn) )
			return UNKNOWN;

		String retStr = UNKNOWN;
//		String sql    = "SELECT CURRENT SERVER as DBNAME, ei.INST_NAME FROM SYSIBMADM.ENV_INST_INFO ei";
		String sql    = "SELECT CURRENT SERVER as DBNAME, ei.INST_NAME, (SELECT HOST_NAME FROM TABLE(SYSPROC.ENV_GET_SYS_INFO())) FROM SYSIBMADM.ENV_INST_INFO ei";

		try
		{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				retStr = rs.getString(1).trim() + "/" + rs.getString(2).trim() + "@" + rs.getString(3).trim();
			}
			rs.close();
			stmt.close();

			return retStr;
		}
		catch (SQLException e)
		{
			_logger.debug("When getting DB2 Server Instance Name. sql='"+sql+"', Caught exception.", e);

			return UNKNOWN;
		}
	}

	public static String getDb2Charset(Connection conn)
	{
		final String UNKNOWN = "UNKNOWN";

		// FIXME: move this to DbUtils
		if ( ! AseConnectionUtils.isConnectionOk(conn) )
			return UNKNOWN;

		String retStr = UNKNOWN;
		String sql    = "select VALUE from SYSIBMADM.DBCFG where NAME = 'codeset'";

		try
		{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				retStr = rs.getString(1).trim();
			}
			rs.close();
			stmt.close();

			return retStr;
		}
		catch (SQLException e)
		{
			_logger.debug("When getting DB2 Server CharacterSet. sql='"+sql+"', Caught exception.", e);

			return UNKNOWN;
		}
	}

	public static String getDb2Sortorder(Connection conn)
	{
		final String UNKNOWN = "UNKNOWN";
		
		return UNKNOWN;

//		// FIXME: move this to DbUtils
//		if ( ! AseConnectionUtils.isConnectionOk(conn) )
//			return UNKNOWN;
//
//		String retStr = UNKNOWN;
//		String sql    = "select VALUE from NLS_DATABASE_PARAMETERS where PARAMETER = 'NLS_SORT'"; 
//
//		try
//		{
//			Statement stmt = conn.createStatement();
//			ResultSet rs = stmt.executeQuery(sql);
//			while (rs.next())
//			{
//				retStr = rs.getString(1).trim();
//			}
//			rs.close();
//			stmt.close();
//
//			return retStr;
//		}
//		catch (SQLException e)
//		{
//			_logger.debug("When getting DB2 Server Sort Order. sql='"+sql+"', Caught exception.", e);
//
//			return UNKNOWN;
//		}
	}
	//------------------------------------------------------------------------------
	//------------------------------------------------------------------------------
	// END DB2 helper methods
	//------------------------------------------------------------------------------
	//------------------------------------------------------------------------------


	//------------------------------------------------------------------------------
	//------------------------------------------------------------------------------
	// BEGIN MaxDB helper methods
	//------------------------------------------------------------------------------
	//------------------------------------------------------------------------------
	
	public static String getMaxDbServername(Connection conn)
	{
//		final String UNKNOWN = "UNKNOWN";
		return "";

//		// FIXME: move this to DbUtils
//		if ( ! AseConnectionUtils.isConnectionOk(conn) )
//			return UNKNOWN;
//
//		String retStr = UNKNOWN;
////		String sql    = "SELECT CURRENT SERVER as DBNAME, ei.INST_NAME FROM SYSIBMADM.ENV_INST_INFO ei";
//		String sql    = "SELECT CURRENT SERVER as DBNAME, ei.INST_NAME, (SELECT HOST_NAME FROM TABLE(SYSPROC.ENV_GET_SYS_INFO())) FROM SYSIBMADM.ENV_INST_INFO ei";
//
//		try
//		{
//			Statement stmt = conn.createStatement();
//			ResultSet rs = stmt.executeQuery(sql);
//			while (rs.next())
//			{
//				retStr = rs.getString(1).trim() + "/" + rs.getString(2).trim() + "@" + rs.getString(3).trim();
//			}
//			rs.close();
//			stmt.close();
//
//			return retStr;
//		}
//		catch (SQLException e)
//		{
//			_logger.debug("When getting DB2 Server Instance Name. sql='"+sql+"', Caught exception.", e);
//
//			return UNKNOWN;
//		}
	}

	public static String getMaxDbCharset(Connection conn)
	{
		final String UNKNOWN = "UNKNOWN";
		return UNKNOWN;

//		// FIXME: move this to DbUtils
//		if ( ! AseConnectionUtils.isConnectionOk(conn) )
//			return UNKNOWN;
//
//		String retStr = UNKNOWN;
//		String sql    = "select VALUE from SYSIBMADM.DBCFG where NAME = 'codeset'";
//
//		try
//		{
//			Statement stmt = conn.createStatement();
//			ResultSet rs = stmt.executeQuery(sql);
//			while (rs.next())
//			{
//				retStr = rs.getString(1).trim();
//			}
//			rs.close();
//			stmt.close();
//
//			return retStr;
//		}
//		catch (SQLException e)
//		{
//			_logger.debug("When getting DB2 Server CharacterSet. sql='"+sql+"', Caught exception.", e);
//
//			return UNKNOWN;
//		}
	}

	public static String getMaxDbSortorder(Connection conn)
	{
		final String UNKNOWN = "UNKNOWN";
		return UNKNOWN;

//		// FIXME: move this to DbUtils
//		if ( ! AseConnectionUtils.isConnectionOk(conn) )
//			return UNKNOWN;
//
//		String retStr = UNKNOWN;
//		String sql    = "select VALUE from NLS_DATABASE_PARAMETERS where PARAMETER = 'NLS_SORT'"; 
//
//		try
//		{
//			Statement stmt = conn.createStatement();
//			ResultSet rs = stmt.executeQuery(sql);
//			while (rs.next())
//			{
//				retStr = rs.getString(1).trim();
//			}
//			rs.close();
//			stmt.close();
//
//			return retStr;
//		}
//		catch (SQLException e)
//		{
//			_logger.debug("When getting DB2 Server Sort Order. sql='"+sql+"', Caught exception.", e);
//
//			return UNKNOWN;
//		}
	}
	//------------------------------------------------------------------------------
	//------------------------------------------------------------------------------
	// END MaxDB helper methods
	//------------------------------------------------------------------------------
	//------------------------------------------------------------------------------

	
	/**
	 * Get various state about a ASE Connection
	 */
	public static JdbcConnectionStateInfo getJdbcConnectionStateInfo(Connection conn)
	{
		JdbcConnectionStateInfo csi = new JdbcConnectionStateInfo();

		// Do the work
		try
		{
			csi._catalog        = conn.getCatalog();
			csi._autocommit     = conn.getAutoCommit();
			csi._isolationLevel = conn.getTransactionIsolation();
		}
		catch (SQLException sqle)
		{
			_logger.error("Error in getJdbcConnectionStateInfo()", sqle);
		}

		return csi;
	}
	/**
	 * Class that reflects a call to getJdbcConnectionStateInfo()
	 * @author gorans
	 */
	public static class JdbcConnectionStateInfo
	{
		public String  _catalog        = "";
		public boolean _autocommit     = true;
		public int     _isolationLevel = -1;

		protected String isolationLevelToString(int isolation)
		{
			switch (isolation)
			{
				case Connection.TRANSACTION_READ_UNCOMMITTED: return "0=READ_UNCOMMITTED";
				case Connection.TRANSACTION_READ_COMMITTED:   return "1=READ_COMMITTED";
				case Connection.TRANSACTION_REPEATABLE_READ:  return "2=REPEATABLE_READ";
				case Connection.TRANSACTION_SERIALIZABLE:     return "3=SERIALIZABLE";
				case Connection.TRANSACTION_NONE:             return "NONE";

				default:
					return "TRANSACTION_ISOLATION_UNKNOWN_STATE("+isolation+")";
			}
		}

		public String getCatalog()
		{
			return _catalog;
		}

		public boolean getAutoCommit()
		{
			return _autocommit;
		}

		public String getIsolationLevelStr()
		{
			return isolationLevelToString(_isolationLevel);
		}
	}
	
	private static void test(int testCase, int expected, String str)
	{
		System.out.println();
		System.out.println(">>>>> BEGIN, test case("+testCase+"): expected="+expected);
		int r = getLineForFirstStatement(str);
		if (r != expected)
		{
			System.out.println("###### FAILED, test case("+testCase+"): ret="+r+", expected="+expected+" ===== THE STR:");
			Scanner sc = new Scanner(str);
			int lineNumber = 0;
			while (sc.hasNextLine())
				System.out.printf("%04d: %s%n", ++lineNumber, sc.nextLine());
			System.out.println("###### END ########################################");
			System.out.println();
		}
		else
		{
			System.out.println("<<<<< -END-, test case("+testCase+"): -OK-OK-OK-");
		}
	}
	public static void main(String[] args)
	{
		test(0,  0, null);
		test(1,  1, "");
		test(2,  1, " ");
		test(3,  1, "--askjdhgads"); 
		test(4,  2, "--askjdhgads\n XXX"); 
		test(5,  3, "\n\n\n");
		test(6,  2, "\nXXX"); 
		test(7,  2, "   /*xxxxx*/   \nXXXX"); 
		test(8,  1, "   /*xxxxx*/ XXXX"); 
		test(9,  3, "/*\nXXX\n*/\n"); 
		test(10, 4, "/*\nXXX\n*/\n  XXXXX");
		
		System.out.println("1: " + parseHanaMessageForProcName("SAP DBTech JDBC: [260] (at 28): invalid column name: XXX: line 1 col 29 (at pos 28)"));
		System.out.println("2: " + parseHanaMessageForProcName("SAP DBTech JDBC: [19999]: user-defined error:  [19999] \"SYSTEM\".\"SP_DBMTK_PROC_INSTALL_FINAL\": line 58 col 3 (at pos 1705): [19999] (range 3) user-defined error exception: user-defined error:  [19999] \"SYSTEM\".\"SP_DBMTK_ABORT_SESSION\": line 56 col 3 (at pos 1929): [19999] (range 3) user-defined error exception: *** Error while installing PROCEDURE SYSTEM.GS_P1  Object was not found."));
		System.out.println("3: " + parseHanaMessageForProcName("[304]: division by zero undefined:  [304] \"SYSTEM\".\"GS_P1\": line 5 col 2 (at pos 54): [304] (range 3) division by zero undefined exception: division by zero undefined:  at function /()"));
	}
}
