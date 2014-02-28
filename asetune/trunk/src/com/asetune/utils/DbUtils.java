package com.asetune.utils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.log4j.Logger;

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
			if (checkProductName.equals(prodNameList[i]))
				return true;
		}
		return false;
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

			rowsAffected = stmnt.getUpdateCount();
			if (rowsAffected >= 0)
			{
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
	//------------------------------------------------------------------------------
	//------------------------------------------------------------------------------
	// END Oracle helper methods
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
