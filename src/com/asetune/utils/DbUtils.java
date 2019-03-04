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
import com.asetune.sql.conn.info.DbxConnectionStateInfoAse;

public class DbUtils
{
	private static Logger _logger = Logger.getLogger(DbUtils.class);
	
	public static final String DB_PROD_NAME_SYBASE_ASE   = "Adaptive Server Enterprise";
	public static final String DB_PROD_NAME_SYBASE_ASA   = "SQL Anywhere";
	public static final String DB_PROD_NAME_SYBASE_IQ    = "Sybase IQ";
	public static final String DB_PROD_NAME_SYBASE_RS    = "Replication Server";
	public static final String DB_PROD_NAME_SYBASE_RAX   = "Sybase Replication Agent for Unix & Windows";
	public static final String DB_PROD_NAME_SYBASE_RSDRA = "DR Agent";
	public static final String DB_PROD_NAME_SYBASE_RSDA  = "SAP Replication Server Data Assurance";

	public static final String DB_PROD_NAME_H2           = "H2";
	public static final String DB_PROD_NAME_HANA         = "HDB";
	public static final String DB_PROD_NAME_MAXDB        = "SAP DB";

	public static final String DB_PROD_NAME_HSQL         = "HSQL Database Engine"; // got this from web, so might not be correct
	public static final String DB_PROD_NAME_MSSQL        = "Microsoft SQL Server"; // got this from web, so might not be correct
	public static final String DB_PROD_NAME_ORACLE       = "Oracle";               // got this from web, so might not be correct
//	public static final String DB_PROD_NAME_DB2_UX       = "DB2/Linux";            // got this from web, so might not be correct
	public static final String DB_PROD_NAME_DB2_LUW      = "DB2/"; 
	public static final String DB_PROD_NAME_DB2_ZOS      = "DB2";                  // got this from web, so might not be correct
	public static final String DB_PROD_NAME_MYSQL        = "MySQL";                // got this from web, so might not be correct
	public static final String DB_PROD_NAME_DERBY        = "Apache Derby";         // got this from web, so might not be correct
	public static final String DB_PROD_NAME_POSTGRES     = "PostgreSQL";
	public static final String DB_PROD_NAME_APACHE_HIVE  = "Apache Hive";


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
			if (DB_PROD_NAME_DB2_LUW.equals(name))
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
	 * Try figure out if the DBMS support schema
	 * 
	 * @param conn   Connection (can not be null)
	 */
	public static boolean isSchemaSupported(Connection conn)
	throws SQLException
	{
		DatabaseMetaData dbmd = conn.getMetaData();

		String schemaTerm       = "";
		int    maxSchemaNameLen = 0;
		
		try { schemaTerm       = dbmd.getSchemaTerm();          } catch(SQLException ex) { _logger.debug("Problems executing: conn.getMetaData().getSchemaTerm(): Caught: "+ex); }
		try { maxSchemaNameLen = dbmd.getMaxSchemaNameLength(); } catch(SQLException ex) { _logger.debug("Problems executing: conn.getMetaData().getMaxSchemaNameLength() Caught: "+ex); }

		if (maxSchemaNameLen <= 0 && StringUtil.isNullOrBlank(schemaTerm))
			return false;

		return true;
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
						sb.append("<TD>").append(                                         rs.getString(1)) .append("</TD>");
						sb.append("<TD>").append(                                         rs.getString(2)) .append("</TD>");
						sb.append("<TD>").append(DbxConnectionStateInfoAse.getAseLockType(rs.getInt   (3))).append("</TD>");
						sb.append("<TD>").append(                                         rs.getString(4)) .append("</TD>");
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
	 * Get server error log for various vendors
	 * @param dbProductName name of the product, if null: get the product name using JDBC DatabaseMetaData
	 * @param conn
	 * @return
	 */
	public static String getServerLogFileName(String dbProduct, Connection conn)
	throws SQLException
	{
		String sql = "not-yet-specified";
		int colPos = 1;
		
		if (StringUtil.isNullOrBlank(dbProduct))
			dbProduct = getDatabaseProductName(conn);

		if (DbUtils.isProductName(dbProduct, DbUtils.DB_PROD_NAME_SYBASE_ASE))  // does MsSQL support @@errorlog??? //
		{
			sql = "select @@errorlog";
			colPos = 1;
		}
		else if (DbUtils.isProductName(dbProduct, DbUtils.DB_PROD_NAME_SYBASE_RS))
		{
			sql = "admin log_name";
			colPos = 1;
		}
		else if (DbUtils.isProductName(dbProduct, DbUtils.DB_PROD_NAME_SYBASE_RAX))
		{
			sql = "log_system_name";
			colPos = 1;
		}
		else if (DbUtils.isProductName(dbProduct, DbUtils.DB_PROD_NAME_SYBASE_IQ, DbUtils.DB_PROD_NAME_SYBASE_ASA))
		{
			sql = "select property('ConsoleLogFile')";
			colPos = 1;
		}
		else if (DbUtils.isProductName(dbProduct, DbUtils.DB_PROD_NAME_MSSQL))
		{
			sql = "select convert(varchar(1024),serverproperty('ErrorLogFileName'))";
			colPos = 1;
		}
		else if (DbUtils.isProductName(dbProduct, DbUtils.DB_PROD_NAME_MYSQL))
		{
			sql = "SHOW VARIABLES LIKE 'log_error'";
			colPos = 2;
		}
//		else if (DbUtils.isProductName(dbProduct, DbUtils.DB_PROD_NAME_POSTGRES))
//		{
//			sql = "show log_filename"; // this just takes the filename not the full path
//		}
//		else if (DbUtils.isProductName(dbProduct, DbUtils.DB_PROD_NAME_APACHE_HIVE))
//		{
//		}
		else if (DbUtils.isProductName(dbProduct, DbUtils.DB_PROD_NAME_HANA))
		{
			// into: /usr/sap/<SID>/HDB<system number>/<hostname>/trace
			// NOTE: The below needs to be tested verified
			//       Also the 'somelogname.log' needs to be determined
			//       I wasn't able to find any M_xxx table with the LOG names, so I had to do the below...
			sql = "select '/usr/sap/' \n"
				+ "   || (select VALUE from M_SYSTEM_OVERVIEW where NAME = 'Instance ID') \n" 
				+ "   || '/HDB' \n"
				+ "   || (select VALUE from M_SYSTEM_OVERVIEW where NAME = 'Instance Number') \n" 
				+ "   || '/' \n"
				+ "   || (select HOST from M_SERVICES where SERVICE_NAME = 'indexserver' and COORDINATOR_TYPE = 'MASTER') \n" 
				+ "   || '/trace/somelogname.log' \n"
				+ "   AS logName \n"
				+ "from DUMMY \n";
		}
		else
		{
			return "unsupported product name '"+dbProduct+"'.";
		}

		String retStr = "";

		Statement stmt = conn.createStatement();
		
		ResultSet rs = stmt.executeQuery(sql);
		while (rs.next())
		{
			retStr = rs.getString(colPos);
		}
		rs.close();
		stmt.close();

		// Postfix on the filename
		if (DbUtils.isProductName(dbProduct, DbUtils.DB_PROD_NAME_MSSQL))
		{
			if (StringUtil.hasValue(retStr))
			{
				// MsSql for Linux seesm to return a "windows type" path... at least for CTP Drop
				// @@version: 'Microsoft SQL Server vNext (CTP1.3) - 14.0.304.138 (X64) Feb 13 2017 16:49:12 Copyright (C) 2016 Microsoft Corporation. All rights reserved. on Linux (Ubuntu 16.04.1 LTS)' 
				// serverproperty('ErrorLogFileName') returns: 'C:\var\opt\mssql\log\errorlog'
				// but I dont think "anyone" would be allowed to look at the file alyway, since the directory is only accessable by user 'mssql' and group 'mssql'... ls -Fal /var/opt/ 'drwxrwx---  7 mssql mssql 4096 mar 11 23:30 mssql/'
				if (retStr.startsWith("C:\\var\\opt\\mssql"))
				{
					// Remove 'C:' and turn \ into /
					retStr = retStr.substring(2).replace('\\', '/');
				}
			}
		}

		return retStr;
	}

	/**
	 * Get product name for the connection, this is also works for ReplicationServer, which normally throws an Exception
	 * @param conn if this is null, a "" is returned.
	 * @return
	 */
	public static String getDatabaseProductName(Connection conn)
	throws SQLException
	{
		if (conn == null)
			return "";

		// yes this was already implemented, so lets reuse it...
		return ConnectionDialog.getDatabaseProductName(conn);
	}

	/**
	 * Check if the connection is in a transaction
	 * 
	 * Oracle uses: dbms_transaction.local_transaction_id
	 * Others:      always return false
	 * 
	 * @param conn
	 * @param dbProduct if null or blank, we will call getDatabaseProductName(conn) to get the name
	 * @return
	 * @throws SQLException
	 */
	public static boolean isInTransaction(Connection conn, String dbProduct)
	throws SQLException
	{
		if (StringUtil.isNullOrBlank(dbProduct))
			dbProduct = getDatabaseProductName(conn);

		if      (isProductName(dbProduct, DB_PROD_NAME_ORACLE )) return isInTransactionOracle(conn);
		else if (isProductName(dbProduct, DB_PROD_NAME_DB2_LUW)) return isInTransactionDb2(conn);

		return false;
	}

	private static boolean isInTransactionOracle(Connection conn)
	throws SQLException
	{
		String sql = 
			  "select "
			+ "CASE "
			+ "  WHEN dbms_transaction.local_transaction_id IS NULL THEN 0 "
			+ "  ELSE 1 "
			+ "END FROM DUAL";

		boolean retVal = false;
		
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		while (rs.next())
		{
			retVal = rs.getInt(1) == 1;
		}
		rs.close();
		stmt.close();

		return retVal;
	}
	private static boolean isInTransactionDb2(Connection conn)
	throws SQLException
	{
		// Hopefully this works, but NOT certain
		String sql = 
			  "select UOW_LOG_SPACE_USED "
			+ "FROM TABLE(MON_GET_UNIT_OF_WORK((select agent_id from sysibmadm.applications where appl_id = application_id()), -1))";

		boolean retVal = false;
		
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		while (rs.next())
		{
			retVal = rs.getInt(1) > 0;
		}
		rs.close();
		stmt.close();

		return retVal;
	}

	/**
	 * Execute a SQL Statement, if any ResultSets are produced, they will be returned as a List
	 * 
	 * @param conn a valid connection
	 * @param sql SQL Statement to execute in the server
	 * @return a list of ResultSets if any
	 * @throws SQLException if we had problems
	 */
	public static List<ResultSetTableModel> exec(Connection conn, String sql, int timeout)
	throws SQLException
	{
		ArrayList<ResultSetTableModel> rsList = new ArrayList<ResultSetTableModel>();

		if (conn == null)
			return rsList;

		Statement stmnt = conn.createStatement();
		if (timeout > 0)
		{
			stmnt.setQueryTimeout(timeout);
		}

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
	 * Simply calls DatabaseMetaData.getMetaData().getTables(cat, schema, tableName) to check if the table exists.
	 * 
	 * @param conn
	 * @param cat       a catalog name; must match the catalog name as it is stored in the database; "" retrieves those without a catalog; null means that the catalog name should not be used to narrow the search
	 * @param schema    a schema name pattern; must match the schema name as it is stored in the database; "" retrieves those without a schema; null means that the schema name should not be used to narrow the search
	 * @param tableName a table name pattern; must match the table name as it is stored in the database 
	 * @return true if table exists
	 * @throws SQLException
	 */
	public static boolean checkIfTableExists(Connection conn, String cat, String schema, String tableName)
	throws SQLException
	{
		if (conn == null)
			throw new SQLException("Connection is NULL.");
		
		DatabaseMetaData dbmd = conn.getMetaData();
		ResultSet rs = dbmd.getTables(cat, schema, tableName, new String[] {"TABLE"});
		boolean tabExists = rs.next();
		rs.close();

		return tabExists;
	}
	/**
	 * Simply calls DatabaseMetaData.getMetaData().getTables(cat, schema, tableName) to check if the table exists.
	 * 
	 * @param conn
	 * @param cat       a catalog name; must match the catalog name as it is stored in the database; "" retrieves those without a catalog; null means that the catalog name should not be used to narrow the search
	 * @param schema    a schema name pattern; must match the schema name as it is stored in the database; "" retrieves those without a schema; null means that the schema name should not be used to narrow the search
	 * @param tableName a table name pattern; must match the table name as it is stored in the database 
	 * @return true if table exists else false (false if any exception)
	 */
	public static boolean checkIfTableExistsNoThrow(Connection conn, String cat, String schema, String tableName)
	{
		try 
		{ 
			return checkIfTableExists(conn, cat, schema, tableName); 
		}
		catch (SQLException ex) 
		{ 
			return false; 
		}
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
	// BEGIN IQ helper methods
	//------------------------------------------------------------------------------
	//------------------------------------------------------------------------------
	public static String getIqServerLogFileName(Connection conn)
	throws SQLException
	{
		String cmd = "select property('ConsoleLogFile')";
		String retStr = "";

		Statement stmt = conn.createStatement();
		
		ResultSet rs = stmt.executeQuery(cmd);
		while (rs.next())
		{
			retStr = rs.getString(1);
		}
		rs.close();
		stmt.close();

		return retStr;
	}
	//------------------------------------------------------------------------------
	//------------------------------------------------------------------------------
	// BEGIN IQ helper methods
	//------------------------------------------------------------------------------
	//------------------------------------------------------------------------------

	
	
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

	public static long getHanaVersionNumber(Connection conn)
	{
		final int UNKNOWN = -1;

		// FIXME: move this to DbUtils
		if ( ! AseConnectionUtils.isConnectionOk(conn) )
			return UNKNOWN;

		String versionStr = "";
		String sql = "select VERSION from m_database";

		try
		{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				versionStr = rs.getString(1).trim();
			}
			rs.close();
			stmt.close();

			long versionNum = Ver.hanaVersionStringToNumber(versionStr);
//System.out.println("getHanaVersionNumber() VersionNum = "+versionNum);
//System.out.println("getHanaVersionNumber() VersionStr = '"+versionStr+"'.");
			return versionNum;
		}
		catch (SQLException e)
		{
			_logger.debug("When getting HANA Version Number. sql='"+sql+"', Caught exception.", e);

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
			rs.close();

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
			rs.close();

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
//		String sql    = "select sys_context('USERENV','DB_NAME') as Instance from dual";
		String sql    = "select sys_context('USERENV','INSTANCE_NAME') as Instance from dual";

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

	public static long getOracleVersionNumber(Connection conn)
	{
		final int UNKNOWN = -1;

		// FIXME: move this to DbUtils
		if ( ! AseConnectionUtils.isConnectionOk(conn) )
			return UNKNOWN;

		String versionStr = "";
		String sql = "SELECT version FROM V$INSTANCE";

		try
		{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				versionStr = rs.getString(1).trim();
			}
			rs.close();
			stmt.close();

			long versionNum = Ver.oracleVersionStringToNumber(versionStr);
			return versionNum;
		}
		catch (SQLException e)
		{
			_logger.debug("When getting Oracle Version Number. sql='"+sql+"', Caught exception.", e);

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
	 * Try to extract the procedure or function name from a ORACLE error message
	 * <p>
	 * Example of Oracle messages
	 * <pre>
	 * 1> \call gs_deleteme_proc(100001, ?) from dual :(str=null out)
	 * Oracle: ErrorCode 6502, SQLState 65000, ExceptionClass: java.sql.SQLException
	 * ORA-06502: PL/SQL: numeric or value error: character string buffer too small
	 * ORA-06512: at "APPDEMO.GS_DELETEME", line 40
	 * ORA-06512: at "APPDEMO.GS_DELETEME_PROC", line 11
	 * ORA-06512: at line 1
	 * </pre>
	 * @param msg the message...
	 * @return null if not found, otherwise SCHEMA.OBJECTNAME:lineNum
	 */
	public static String parseOracleMessageForProcName(SQLException ex)
	{
		if (ex == null)
			return null;
		
		String msg = ex.getMessage();
		if (msg == null)
			return null;

		// The message seems to be split in several lines so read the lines
		// when hitting first "ORA-06512:" then process and exit
		Scanner scanner = new Scanner(msg);
		while (scanner.hasNextLine()) 
		{
			String lineStr = scanner.nextLine();

			// Split the line into various parts, and return (hopefully all language translations will use the same positions)
			// ORA-06512: at "APPDEMO.GS_DELETEME", line 40
			if (lineStr.startsWith("ORA-06512: "))
			{
				List<String> parts = StringUtil.splitOnCharAllowQuotes(lineStr, ' ', true);
				
				String objName = parts.size() >= 3 ? parts.get(2) : "";
				String lineNum = parts.size() >= 5 ? parts.get(4) : "";

				// Remove leading quote char
				if (objName.startsWith("\""))
					objName = objName.substring(1);

				// Remove tailing quote char & comma char
				if (objName.endsWith("\","))
					objName = objName.substring(0, objName.length()-2);

				return objName + ":" + lineNum;
			}
		}
		return null;
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
//		String sql =
//			"SELECT text \n" +
//			"FROM all_source \n" +
//			"WHERE owner = '"+schema+"' \n" +
//			"  AND name  = '"+name+"' \n" +
//			"ORDER BY line \n";

//		String sql = 
//			"select dbms_metadata.get_ddl(OBJECT_TYPE, OBJECT_NAME, OWNER) \n"
//			+ "from DBA_OBJECTS \n"  // or possible ALL_OBJECTS
//			+ "where OWNER       = '"+schema+"' \n"
//			+ "  and OBJECT_NAME = '"+name+"'";

		String sql = 
//			"SELECT SUBSTR(TEXT, 1, LENGTH(TEXT) - 1) \n" +
			"SELECT TEXT \n" +
//			"FROM DBA_SOURCE \n" +
			"FROM ALL_SOURCE \n" +                   // ALL_SOURCE will hopefully need less authority/grants than DBA_SOURCE
			"WHERE OWNER = '"+schema+"' \n" +
			"  AND NAME = '"+name+"' \n" +
//			"  AND TYPE = '????' \n" +
			"ORDER BY LINE \n";

		try
		{
			StringBuilder sb = new StringBuilder();

			Statement statement = conn.createStatement();
			ResultSet rs = statement.executeQuery(sql);
			while(rs.next())
			{
				String textPart = rs.getString(1);
 
				// only needed if: dbms_metadata.get_ddl is used
				//if (textPart.startsWith("\n"))
				//	textPart = textPart.substring(1);

				sb.append(textPart);
//				sb.append(textPart).append("\n");
			}
			rs.close();
			statement.close();

			if (sb.length() > 0)
				returnText = sb.toString();
		}
		catch (SQLException e)
		{
			returnText = null;
			_logger.warn("Problems getting text for ORACLE object '"+objectName+"'. Caught: "+e); 
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
	// BEGIN - SQL-Server helper methods
	//------------------------------------------------------------------------------
	//------------------------------------------------------------------------------
//	public static String getSqlServerVersionStr(Connection conn)
//	{
//		final String UNKNOWN = "UNKNOWN";
//
//		// FIXME: move this to DbUtils
//		if ( ! AseConnectionUtils.isConnectionOk(conn) )
//			return UNKNOWN;
//
//		String versionStr = "";
//		String sql        = "SELECT @@version";
//
//		try
//		{
//			Statement stmt = conn.createStatement();
//			ResultSet rs = stmt.executeQuery(sql);
//			while (rs.next())
//			{
//				versionStr = rs.getString(1).trim();
//			}
//			rs.close();
//			stmt.close();
//
//			return versionStr;
//		}
//		catch (SQLException e)
//		{
//			_logger.debug("When getting SQL-Server Version String. sql='"+sql+"', Caught exception.", e);
//
//			return UNKNOWN;
//		}
//	}
//
//	public static long getSqlServerVersionNumber(Connection conn)
//	{
//		final int UNKNOWN = -1;
//
//		// FIXME: move this to DbUtils
//		if ( ! AseConnectionUtils.isConnectionOk(conn) )
//			return UNKNOWN;
//
//		String versionStr = getSqlServerVersionStr(conn);
//
//		long versionNum = Ver.sqlServerVersionStringToNumber(versionStr);
//		return versionNum;
//	}

	//------------------------------------------------------------------------------
	//------------------------------------------------------------------------------
	// END - SQL-Server helper methods
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

	public static String getDb2VersionStr(Connection conn)
	{
		final String UNKNOWN = "UNKNOWN";

		// FIXME: move this to DbUtils
		if ( ! AseConnectionUtils.isConnectionOk(conn) )
			return UNKNOWN;

		String versionStr = "";
		String sql        = "SELECT SERVICE_LEVEL FROM SYSIBMADM.ENV_INST_INFO";

		try
		{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				versionStr = rs.getString(1).trim();
			}
			rs.close();
			stmt.close();

			return versionStr;
		}
		catch (SQLException e)
		{
			_logger.debug("When getting DB2 Version String. sql='"+sql+"', Caught exception.", e);

			return UNKNOWN;
		}
	}

	public static long getDb2VersionNumber(Connection conn)
	{
		final int UNKNOWN = -1;

		// FIXME: move this to DbUtils
		if ( ! AseConnectionUtils.isConnectionOk(conn) )
			return UNKNOWN;

		String versionStr = getDb2VersionStr(conn);

		long versionNum = Ver.db2VersionStringToNumber(versionStr);
		return versionNum;
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

	
//	/**
//	 * Get various state about a ASE Connection
//	 */
//	public static JdbcConnectionStateInfo getJdbcConnectionStateInfo(Connection conn, String dbProduct)
//	{
//		JdbcConnectionStateInfo csi = new JdbcConnectionStateInfo();
//		
//		// Do the work
//		try
//		{
//			csi._catalog        = conn.getCatalog();
//			csi._autocommit     = conn.getAutoCommit();
//			csi._isolationLevel = conn.getTransactionIsolation();
//			csi._inTransaction  = DbUtils.isInTransaction(conn, dbProduct);
//		}
//		catch (SQLException sqle)
//		{
//			_logger.error("Error in getJdbcConnectionStateInfo()", sqle);
//		}
//
//		return csi;
//	}
//	/**
//	 * Class that reflects a call to getJdbcConnectionStateInfo()
//	 * @author gorans
//	 */
//	public static class JdbcConnectionStateInfo
//	{
//		public String  _catalog        = "";
//		public boolean _autocommit     = true;
//		public int     _isolationLevel = -1;
//		public boolean _inTransaction  = false;
//
//		protected String isolationLevelToString(int isolation)
//		{
//			switch (isolation)
//			{
//				case Connection.TRANSACTION_READ_UNCOMMITTED: return "0=READ_UNCOMMITTED";
//				case Connection.TRANSACTION_READ_COMMITTED:   return "1=READ_COMMITTED";
//				case Connection.TRANSACTION_REPEATABLE_READ:  return "2=REPEATABLE_READ";
//				case Connection.TRANSACTION_SERIALIZABLE:     return "3=SERIALIZABLE";
//				case Connection.TRANSACTION_NONE:             return "NONE";
//
//				default:
//					return "TRANSACTION_ISOLATION_UNKNOWN_STATE("+isolation+")";
//			}
//		}
//
//		public String getCatalog()
//		{
//			return _catalog;
//		}
//
//		public boolean getAutoCommit()
//		{
//			return _autocommit;
//		}
//
//		public String getIsolationLevelStr()
//		{
//			return isolationLevelToString(_isolationLevel);
//		}
//	}


	/**
	 * Make a string a bit more <i>safe</i>
	 * <ul>
	 *    <li>a null object will return the String NULL</li>
	 *    <li>If it's a Number, Simply call toString() on the Number.</li>
	 *    <li>For all others objects, make toString, and Quote the it using single ', if the value contains any ' they will be transformed into ''.</li>
	 * </ul>
	 * @param obj
	 * @return
	 */
	public static String safeStr(Object obj)
	{
		return safeStr(obj, -1);
	}
	public static String safeStr(Object obj, int maxStrLen)
	{
		if (obj == null)
			return "NULL";

		if (obj instanceof Number)
		{
			return obj.toString();
		}
		else
		{
			String str = obj.toString();

			if ( maxStrLen > 0 && str.length() > maxStrLen )
			{
				// Put '...' at the end to mark the string as "truncated"
				// Unless it's a "short" string, then just truncate to the maxLen
				String truncStr;
				if (maxStrLen >= 4)
					truncStr = str.substring(0, maxStrLen-3) + "...";
				else
					truncStr = str.substring(0, maxStrLen);
					
				_logger.debug("DbUtils.safeStr(): MaxLen="+maxStrLen+". Truncating value |"+str+"|, into |"+truncStr+"|.");
				str = truncStr;
			}

			StringBuilder sb = new StringBuilder();

			// add ' around the string...
			// and replace all ' into ''
			sb.append("'");
			sb.append(str.replace("'", "''"));
			sb.append("'");
			return sb.toString();
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
