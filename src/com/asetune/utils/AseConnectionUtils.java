/******************************************************************************
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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.utils;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

import com.asetune.Version;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.config.ui.AseConfigMonitoringDialog;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.TdsConnection;
import com.asetune.sql.conn.info.DbxConnectionStateInfoAse;
import com.asetune.sql.conn.info.DbxConnectionStateInfoAse.LockRecord;
import com.sybase.jdbc42.jdbc.SybSQLWarning;
//import com.sybase.jdbc4.jdbc.SybSQLWarning;
import com.sybase.jdbcx.EedInfo;
import com.sybase.jdbcx.SybConnection;
import com.sybase.jdbcx.SybMessageHandler;

import net.miginfocom.swing.MigLayout;

public class AseConnectionUtils
{
	// Some stuff for ASE Configuration
	public static final int CONFIG_TYPE_IF_CHANGED = 1; 
	public static final int CONFIG_TYPE_IF_CFG_LT  = 2;
	public static final int CONFIG_TYPE_IF_CFG_GT  = 3;

	// Cluster Edition System View (do we see all CE instances or just the local one)
	public static final int CE_SYSTEM_VIEW_UNKNOWN  = 0;
	public static final int CE_SYSTEM_VIEW_CLUSTER  = 1;
	public static final int CE_SYSTEM_VIEW_INSTANCE = 2;

	private static Logger _logger = Logger.getLogger(AseConnectionUtils.class);
	private static String SQL_VERSION     = "select @@version";
//	private static String SQL_VERSION_NUM = "select @@version_number";
//	private static String SQL_SP_VERSION  = "sp_version 'installmontables'";

	
	public static final String  PROPKEY_getShowplan_useLongOption = "AseConnectionUtils.getShowplan.useLongOption";
	public static final boolean DEFAULT_getShowplan_useLongOption = true;
	
	/**
	 * What is current working database
	 * @return database name, null on failure
	 */
	public static String getDbname(Connection conn)
	{
		return getCurrentDbname(conn);
	}
	/**
	 * What is current working database
	 * @return database name, null on failure
	 */
	public static String getCurrentDbname(Connection conn)
	{
		if ( ! isConnectionOk(conn, false, null) )
			return null;

		try
		{
			Statement stmnt   = conn.createStatement();
			ResultSet rs      = stmnt.executeQuery("select db_name()");
			String cwdb = "";
			while (rs.next())
			{
				cwdb = rs.getString(1);
			}
			rs.close();
			return cwdb;
		}
		catch(SQLException e)
		{
			_logger.warn("Problems getting current Working Database. Error Number: "+e.getErrorCode()+", Message: " + e.getMessage());
//			JOptionPane.showMessageDialog(
//					QueryWindow.this, 
//					"Problems getting current Working Database:\n" +
//					"Error Number: "+e.getErrorCode()+"\n" + e.getMessage(),
//					"Error", JOptionPane.ERROR_MESSAGE);
			return null;
		}
	}

	
	public static boolean useDbname(Connection conn, String dbname)
	{
		if ( ! isConnectionOk(conn, false, null) )
			return false;


		// First check if we have a valid dbname
		// And that we already are not within that database.
		if (dbname == null || dbname.equals("") )
			return false;

		String dbNameBeforeChange = getCurrentDbname(conn);
		if ( dbNameBeforeChange.equalsIgnoreCase(dbname) )
		{
			// No need to change database.
			_logger.debug("No need to change database to '"+dbNameBeforeChange+"', you are already in it.");
			return true;
		}

		String cmd = "use "+dbname;

//		if (_extensiveLogging)
//			_logger.info("Start to use the new database '"+dbname+"' in server '"+getServerName()+"' for the '"+getManagedType()+"'. Current database is '"+dbNameBeforeChange+"'.");

		// Do the work
		Statement	stmnt = null;
		try
		{
			stmnt = conn.createStatement();
//			if ( getQueryTimeout() > 0 )
//				stmnt.setQueryTimeout( getQueryTimeout() );
			stmnt.executeUpdate(cmd);

			if ( hasSqlWarnings(stmnt.getWarnings()) )
			{
				_logger.info("Received following warnings when changing database to '"+dbname+"', continuing... " + sqlWarningToString(stmnt.getWarnings()) );
			}

			stmnt.close();
		}
		catch (SQLException sqle)
		{
			// Check for timeouts etc.
			//checkForProblems(sqle, cmd);

			for(SQLException e=sqle; e!=null; e=e.getNextException())
			{
				int aseError = e.getErrorCode();
				switch ( aseError )
				{
				case 918: // 918, 14, 2, Database '%.*s' has not yet been recovered - please wait before accessing this database.
				case 919: // 919, 21, 2, Database '%.*s' was marked 'suspect' by an earlier attempt at recovery. Check the ASE errorlog for information as to the cause.
				case 921: // 921, 14, 2, Database '%.*s' has not been recovered yet - please wait and try again.
				case 922: // 922, 14, 2, Database '%.*s' is being recovered - will wait until recovery is finished.
				case 924: // 924, 14, 2, Database '%.*s' is already open and can only have one user at a time.
				case 926: // 926, 14, 2, Database '%.*s' cannot be opened. An earlier attempt at recovery marked it 'suspect'. Check the ASE errorlog for information as to the cause.
				case 927: // 927, 14, 2, Database <%d> cannot be opened - it is in the middle of a load.
				case 928: // 928, 14, 2, Database '%.*s' cannot be opened - it is currently being created.  Wait and try query again.
				case 930: // 930, 14, 2, Database '%.*s' cannot be opened because either an earlier system termination left LOAD DATABASE incomplete or the database is created with 'for load' option. Load the database or contact a user with System Administrator (SA) role.
				case 931: // 931, 21, 2, Database '%.*s' cannot be opened because of a failure to initialize the global timestamp.  This indicates that a problem exists in the log for the current database.  Please contact Technical support for assistance.
				case 932: // 932, 22, 2, Database '%.*s' cannot be opened because the log for the current database is corrupt.  Page %ld of the log is linked to a page that belongs to a database object with id %ld. Please contact Technical support for assistance.
				case 937: // 937, 14, 2, Database '%.*s' is unavailable. It is undergoing LOAD DATABASE.
				case 938: // 938, 14, 2, Database '%.*s' is unavailable.  It is undergoing LOAD TRANSACTION.
				case 942: // 942, 20, 2, Database cannot be opened because a system descriptor cannot be installed.
				case 943: // 943, 14, 2, Database '%.*s' cannot be opened since an attempt to upgrade it was unsuccessful.
				case 947: // 947, 14, 2, Database '%.*s' has been marked as having corrupt security labels. Please contact a user with the System Administrator, System Security Officer, or Oper role or the Database Owner.
				case 948: // 948, 14, 2, Database '%.*s' is unavailable. It is undergoing a security label consistency fix.
				case 949: // 949, 14, 2, Database '%.*s' is unavailable.  It is being bound to a named cache.
				case 950: // 950, 14, 2, Database '%.*s' is currently offline. Please wait and try your command again later.
				case 951: // 951, 14, 2, Database identity for server user id %d changed after permission checking in database '%.*s'.  Please try again.
				case 952: // 952, 20, 2, Database '%.*s' cannot be opened because a system index descriptor cannot be installed.
				case 959: // 959, 14, 2, Database '%.*s' cannot be opened because an earlier system termination left DROP DATABASE incomplete. Drop the database or run 'DBCC CHECKALLOC' to correct allocation information.
				case 961: // 961, 14, 2, Database '%.*s' is temporarily unavailable.
				case 962: // 962, 14, 2, Database with ID '%d' is not available. Please try again later.
				case 963: // 963, 14, 2, Database '%.*s' is unavailable. It is undergoing ONLINE DATABASE.
				case 968: // 968, 16, 2, Database '%S_DBINFO' is not upgraded and hence is not available for access. Please retry your query after database has been upgraded.
				case 3471://3471, 10, 2, Database '%.*s' cannot be brought online because it has replicated tables that may not be completely transferred. After making sure that your replication is in sync, use dbcc dbrepair to remove the secondary truncpt.
					String aseMsg = (e.getMessage() == null) ? "-unknown-" : e.getMessage().replaceAll("\n", "");
					String msg = "It looks like the database '"+dbname+"' is in 'load database', 'recovery mode' or 'offline', still in the database '"+dbNameBeforeChange+"', please try again later. DbmsMsgNumber="+aseError+", DbmsMsg="+aseMsg;
					_logger.warn(msg);
					//throw new DbNotRecoveredException(_servername, dbname, getManagedType(), msg, sqle);
					return false;
				}
			}
			String msg = "Problems when executing '"+cmd+"' in '"+dbNameBeforeChange+"'.";
			_logger.error(msg + sqlExceptionToString(sqle));
			//throw new ManageException(msg, sqle);
			return false;
		}

		// maybe set up a messageHandler and check for message 5701,
		// then parse in what database we currentley is in.
		// The below check is expensive, if it hapens alot.
		//
		// It doesn't look like the 5701 is available in the messagehandler code
		// 5701, 5703, 5704, 7326 is discarded in Jconnect 5.5 code. (Tds.java:2777-2783)
		//
		//getAseInfo(); // refreshes _currentDbname
		String dbNameAfterChange = getCurrentDbname(conn);


		if ( ! dbname.equalsIgnoreCase(dbNameAfterChange) )
		{
			return false;
		}

		// Write some debug info
		_logger.debug("Changed database from '"+dbNameBeforeChange+"' to '"+dbNameAfterChange+"'.");
		return true;
	}

	
	/**
	 * Get current time at the ASE server
	 */
	public static Timestamp getAseGetdate(Connection conn)
	{
		Timestamp   aseGetdate = null;
		Statement	stmnt = null;
		ResultSet	rs = null;

		String sql = "select timenow=getdate()";

		if ( ! isConnectionOk(conn, false, null) )
			return null;

		// Do the work
		try
		{
			stmnt = conn.createStatement();
//			if ( getQueryTimeout() > 0 )
//				stmnt.setQueryTimeout( getQueryTimeout() );
			rs = stmnt.executeQuery(sql);

			while(rs.next())
			{
				aseGetdate    = rs.getTimestamp(1);
			}
			rs.close();
			stmnt.close();
		}
		catch (SQLException sqle)
		{
			aseGetdate = null;

			String msg = "Problems when executing '"+sql+"' in ASE Server.";
			_logger.error(msg + sqlExceptionToString(sqle));
		}

		_logger.debug("getAseGetdate(): getdate()='"+aseGetdate+"'.");

		return aseGetdate;
	}


	/**
	 * Get creation time of a database object
	 */
	public static Calendar getObjectCreationCalendar(Connection conn, String dbname, String objectName)
	{
		return getObjectCreationCalendar(conn, dbname, objectName, null);
	}
	public static Calendar getObjectCreationCalendar(Connection conn, String dbname, String objectName, String type)
	{
		Timestamp ts = getObjectCreationTimestamp(conn, dbname, objectName, type);
		if (ts == null)
			return null;

//		Calendar  crDate = Calendar.getInstance();
		Calendar  crDate = new GregorianCalendar();
		crDate.setTimeInMillis(ts.getTime());
		
		return crDate;
	}
	public static Date getObjectCreationDate(Connection conn, String dbname, String objectName)
	{
		return getObjectCreationDate(conn, dbname, objectName, null);
	}
	public static Date getObjectCreationDate(Connection conn, String dbname, String objectName, String type)
	{
		Timestamp ts = getObjectCreationTimestamp(conn, dbname, objectName, type);
		if (ts == null)
			return null;

		return new Date(ts.getTime());
	}
	public static Timestamp getObjectCreationTimestamp(Connection conn, String dbname, String objectName)
	{
		return getObjectCreationTimestamp(conn, dbname, objectName, null);
	}
	public static Timestamp getObjectCreationTimestamp(Connection conn, String dbname, String objectName, String type)
	{
		Timestamp   crDate = null;
		Statement	stmnt  = null;
		ResultSet	rs     = null;

		String sql = "select crdate " +
		             "from ["+dbname+"]..sysobjects " +
		             "where name = '"+objectName+"'";

		if (type != null)
			sql += "  and type = '"+type+"'";

		if ( ! isConnectionOk(conn, false, null) )
			return null;

		// Do the work
		try
		{
			stmnt = conn.createStatement();
//			if ( getQueryTimeout() > 0 )
//				stmnt.setQueryTimeout( getQueryTimeout() );
			rs = stmnt.executeQuery(sql);

			while(rs.next())
			{
				crDate    = rs.getTimestamp(1);
			}
			rs.close();
			stmnt.close();
		}
		catch (SQLException sqle)
		{
			crDate = null;

			String msg = "Problems when executing '"+sql+"' in ASE Server.";
			_logger.error(msg + sqlExceptionToString(sqle));
		}

		_logger.debug("getObjectCreationDate(): objectName='"+objectName+"', created='"+crDate+"'.");

		return crDate;
	}


	/**
	 * Turn a SQLException into a warning, if it's a Sybase msg, keep all EedInfo properties
	 */
	public static SQLWarning sqlExceptionToWarning(SQLException sqle)
	{
		if (sqle instanceof EedInfo)
		{
			EedInfo eed = (EedInfo) sqle;
			return new SybSQLWarning(sqle.getMessage(), sqle.getSQLState(), sqle.getErrorCode(), 
					eed.getState(), eed.getSeverity(), eed.getServerName(), eed.getProcedureName(), eed.getLineNumber(), eed.getEedParams(), eed.getTranState(), eed.getStatus());
		}
		else
			return new SQLWarning(sqle.getMessage(), sqle.getSQLState(), sqle.getErrorCode());
		
	}

	/**
	 * Check if the SQLException is part of some Message like "in load database" or "database is offline"
	 * @param sqle
	 * @return
	 */
	public static boolean isInLoadDbException(SQLException sqle)
	{
		int code = sqle.getErrorCode();

		// error - severity - description
		switch (code)
		{
		case 930:    //   930 - 14 - Database '%.*s' cannot be opened because either an earlier system termination left LOAD DATABASE incomplete or the database is created with 'for load' option. Load the database or contact a user with System Administrator (SA) role.
		case 937:    //   937 - 14 - Database '%.*s' is unavailable. It is undergoing LOAD DATABASE.
		case 938:    //   938 - 14 - Database '%.*s' is unavailable.  It is undergoing LOAD TRANSACTION.
		case 3118:   //  3118 - 16 - LOAD DATABASE has been interrupted by a USER ATTENTION signal.  A LOAD DATABASE must be completed in this database before it will be accessible.
		case 12537:  // 12537 - 10 - Database '%.*s' is in QUIESCE DATABASE state. It will recovered as for LOAD DATABASE and left off line.

		case 950:    //   950 - 14 - Database '%.*s' is currently offline. Please wait and try your command again later.
		case 18174:  // 18174 -  0 - The database '%1!' is offline. To obtain cache-bindings for objects in this database, please online the database and rerun sp_helpcache.

		case 918:    //   918 - 14 - Database '%.*s' has not yet been recovered - please wait before accessing this database.
		case 921:    //   921 - 14 - Database '%.*s' has not been recovered yet - please wait and try again.
		case 922:    //   922 - 14 - Database '%.*s' is being recovered - will wait until recovery is finished.

		case 2206:   //  2206 - 10 - Database %.*s with dbid %d is already shut down.

		case 7408:   //  7408 - 20 - Could not find a dbtable for database %d.

			return true;

		default:
			return false;
		}
	}
	
	public static String sqlExceptionToString(SQLException sqle)
	{
		StringBuffer sb = new StringBuffer();
		while (sqle != null)
		{
			if (sqle instanceof EedInfo) 
			{
				EedInfo sybsqle = (EedInfo) sqle;
				
				sb.append( " (Srv=" );
				sb.append( sybsqle.getServerName() );
				sb.append( ", Error=" );
				sb.append( sqle.getErrorCode() );
				sb.append( ", Severity=" );
				sb.append( sybsqle.getSeverity() );
				sb.append( ", Proc=" );
				sb.append( sybsqle.getProcedureName() );
				sb.append( ", Line=" );
				sb.append( sybsqle.getLineNumber() );
				sb.append( ", Text=" );
				sb.append( sqle.getMessage() );
				sb.append( ")" );
			}
			else
			{
				sb.append( " (Error=" );
				sb.append( sqle.getErrorCode() );
				sb.append( ", Text=" );
				sb.append( sqle.getMessage() );
				sb.append( ")" );
			}
			sqle = sqle.getNextException();
		}
		return sb.toString();
	}
	public static boolean hasSqlWarnings(SQLWarning sqlw)
	{
		while (sqlw != null)
		{
			return true;
		}
		return false;
	}
	public static String sqlWarningToString(SQLWarning sqlw)
	{
		StringBuffer sb = new StringBuffer();
		while (sqlw != null)
		{
			if (sqlw instanceof EedInfo) 
			{
				EedInfo sybsqlw = (EedInfo) sqlw;
				
				sb.append( " (Srv=" );
				sb.append( sybsqlw.getServerName() );
				sb.append( ", Error=" );
				sb.append( sqlw.getErrorCode() );
				sb.append( ", Severity=" );
				sb.append( sybsqlw.getSeverity() );
				sb.append( ", Proc=" );
				sb.append( sybsqlw.getProcedureName() );
				sb.append( ", Line=" );
				sb.append( sybsqlw.getLineNumber() );
				sb.append( ", Text=" );
				sb.append( sqlw.getMessage() );
				sb.append( ")" );
			}
			else
			{
				sb.append( " (Error=" );
				sb.append( sqlw.getErrorCode() );
				sb.append( ", Text=" );
				sb.append( sqlw.getMessage() );
				sb.append( ")" );
			}
			sqlw = sqlw.getNextWarning();
		}
		return sb.toString();
	}

	/**
	 * If SQLExceptions has been down graded to SQLWarnings in the jConnect message handler, but
	 * we still want to throw an SQLException if we find any "warnings" that is above severity 10 
	 *
	 * @param sqlw
	 * @return
	 * @throws SQLException
	 */
	public static void checkSqlWarningsAndThrowSqlExceptionIfSeverityIsAbove10(SQLWarning sqlw)
	throws SQLException
	{
		while (sqlw != null)
		{
			if (sqlw instanceof EedInfo)
			{
				EedInfo eed = (EedInfo) sqlw;
				if (eed.getSeverity() > 10)
					throw sqlw;
				
//				throw new SybSQLException(sqlw.getMessage(), sqlw.getSQLState(), sqlw.getErrorCode(), 
//						eed.getState(), eed.getSeverity(), eed.getServerName(), eed.getProcedureName(), eed.getLineNumber(), eed.getEedParams(), eed.getTranState(), eed.getStatus());
			}
			
			sqlw = sqlw.getNextWarning();
		}
	}

	/**
	 * Get a message string that looks like the one got from Sybase 'isql' utility
	 * @param sqe a SQLException object
	 * @return A String, even if no SQLException is present, return a empty string.
	 */
	public static String getSqlWarningMsgs(SQLException sqe)
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
		
	/**
	 * Get the @@servername from the ASE server
	 * 
	 * @param conn
	 * @return @@servername from the ASE, or empty string "" on problems
	 */
	public static String getAseServername(Connection conn)
	{
		final String UNKNOWN = "";

		if ( ! isConnectionOk(conn, true, null) )
			return UNKNOWN;

		try
		{
			String name = UNKNOWN;

			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select @@servername");
			while (rs.next())
			{
				name = rs.getString(1);
				name = (name == null) ? "" : name.trim();
			}
			rs.close();
			stmt.close();

			return name;
		}
		catch (SQLException e)
		{
			_logger.debug("When getting @@servername, Caught exception.", e);

			return UNKNOWN;
		}
	}

	/**
	 * Get the hostname where the ase is running on
	 * <p>
	 * NOTE: this requires ASE version 15.0.2 or higher
	 * @param conn
	 * @param stripDomainName if true, only the hostname will be returned, for axample "hostname1" if name is "hostname1.domain.com"
	 * @return hostname where the ASE is running on. "UNKNOWN" on problems
	 */
	public static String getAseHostname(Connection conn, boolean stripDomainName)
	{
		final String UNKNOWN = "UNKNOWN";

		if ( ! isConnectionOk(conn, true, null) )
			return UNKNOWN;

		try
		{
			String hostname = UNKNOWN;

			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select asehostname()");
			while (rs.next())
			{
				String host = rs.getString(1).trim();
				if (stripDomainName && host.indexOf(".") > 0)
					hostname = host.substring(0, host.indexOf("."));
				else
					hostname = host;
			}
			rs.close();
			stmt.close();

			return hostname;
		}
		catch (SQLException e)
		{
			_logger.debug("When getting asehostname(), Caught exception.", e);

			return UNKNOWN;
		}
	}

	/**
	 * Get the @@maxpagesize from the ASE server
	 * 
	 * @param conn
	 * @return @@maxpagesize from the ASE, or -1 on problems, it's delivered in bytes 2048, 4096, 8192 or 16384
	 */
	public static int getAsePageSize(Connection conn)
	{
		final int UNKNOWN = -1;

		if ( ! isConnectionOk(conn, true, null) )
			return UNKNOWN;

		try
		{
			int pgsize = UNKNOWN;

			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select @@maxpagesize");
			while (rs.next())
			{
				pgsize = rs.getInt(1);
			}
			rs.close();
			stmt.close();

			return pgsize;
		}
		catch (SQLException e)
		{
			_logger.debug("When getting @@maxpagesize, Caught exception.", e);

			return UNKNOWN;
		}
	}

	/**
	 * Get ASE SPID
	 * @param conn
	 * @return
	 */
	public static int getAseSpid(Connection conn)
	{
		final int UNKNOWN = -1;

		if ( ! isConnectionOk(conn, true, null) )
			return UNKNOWN;

		try
		{
			int spid = UNKNOWN;

			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select @@spid");
			while (rs.next())
			{
				spid = rs.getInt(1);
			}
			rs.close();
			stmt.close();

			return spid;
		}
		catch (SQLException e)
		{
			_logger.debug("When getting @@spid, Caught exception.", e);

			return UNKNOWN;
		}
	}

	/**
	 * Get the currently used errolog on the server 
	 * 
	 * @param conn
	 * @return the errolog 
	 * @throws SQLException
	 */
	public static String getServerLogFileName(Connection conn)
	throws SQLException
	{
		String cmd = "select @@errorlog";
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

	/**
	 * Get ASE SPID
	 * @param conn
	 * @return
	 */
	public static String getAseVersionStr(Connection conn)
	{
		final String UNKNOWN = "";

		if ( ! isConnectionOk(conn, true, null) )
			return UNKNOWN;

		try
		{
			String verStr = UNKNOWN;

			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select @@version");
			while (rs.next())
			{
				verStr = rs.getString(1);
			}
			rs.close();
			stmt.close();

			return verStr;
		}
		catch (SQLException e)
		{
			_logger.debug("When getting @@version, Caught exception.", e);

			return UNKNOWN;
		}
	}

	/**
	 * Get a comma separated list of values from ASE syslisteners table.
	 * @param conn
	 * @param addType
	 * @param stripDomainName
	 * @param guiOwner
	 * @return
	 */
	public static String getListeners(Connection conn, boolean addType, boolean stripDomainName, Component guiOwner)
	{
		if ( ! isConnectionOk(conn, true, guiOwner) )
			return null;

		try
		{
			// LIST WHAT hostnames port(s) the ASE server is listening on.
			String listenersStr = "";
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select distinct * from master..syslisteners");
			while (rs.next())
			{
				if (addType)
					listenersStr += rs.getString("net_type").trim() + ":";

				String host = rs.getString("address_info").trim();
				if (stripDomainName && host != null && host.indexOf(".") > 0)
				{
					String hostname = host.substring(0, host.indexOf("."));
					String portnum  = host.substring(host.lastIndexOf(" "));
					
					// if hostname is a number, then it's probably an IP adress
					// Then use the full IP
					try 
					{ 
						hostname = host.substring(0, host.indexOf(" ")).trim();
						Integer.parseInt(hostname);
					}
					catch(NumberFormatException ignore) {}
					listenersStr += hostname + portnum;
				}
				else
					listenersStr += host;

				listenersStr += ", ";
			}
			rs.close();
			// Take away last ", "
			listenersStr = StringUtil.removeLastComma(listenersStr);
			
			return listenersStr;
		}
		catch (SQLException e)
		{
			_logger.debug("When getting listeners, Caught exception.", e);

			if (guiOwner != null)
				showSqlExceptionMessage(guiOwner, "Getting Listeners", "When getting listeners, we got an SQLException", e);
			
			return null;
		}
	}

	public static String getAseCharset(Connection conn)
	{
		final String UNKNOWN = "UNKNOWN";

		if ( ! isConnectionOk(conn, true, null) )
			return UNKNOWN;

		try
		{
			String retStr = UNKNOWN;

			String sql = 
				"declare @charid tinyint \n" +
				"select @charid = value from master..syscurconfigs where config = 131 \n" +
				"" +
				"select charset_name = name  from master..syscharsets   where id = @charid \n";
			
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
			_logger.debug("When getting ASE charset, Caught exception.", e);

			return UNKNOWN;
		}
	}

	public static String getAseCharsetId(Connection conn)
	{
		final String UNKNOWN = "UNKNOWN";

		if ( ! isConnectionOk(conn, true, null) )
			return UNKNOWN;

		try
		{
			String retStr = UNKNOWN;

			String sql = "select value from master..syscurconfigs where config = 131";
			
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
			_logger.debug("When getting ASE charset id, Caught exception.", e);

			return UNKNOWN;
		}
	}

	public static String getAseSortorder(Connection conn)
	{
		final String UNKNOWN = "UNKNOWN";

		if ( ! isConnectionOk(conn, true, null) )
			return UNKNOWN;

		try
		{
			String retStr = UNKNOWN;

			String sql = 
				"declare @sortid tinyint, @charid tinyint \n" +
				"select @sortid = value from master..syscurconfigs where config = 123 \n" +
				"select @charid = value from master..syscurconfigs where config = 131 \n" +
				"" +
				"select sortorder_name= name  from master..syscharsets   where id = @sortid and csid = @charid \n";
			
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
			_logger.debug("When getting ASE sortorder, Caught exception.", e);

			return UNKNOWN;
		}
	}

	public static String getAseSortorderId(Connection conn)
	{
		final String UNKNOWN = "UNKNOWN";

		if ( ! isConnectionOk(conn, true, null) )
			return UNKNOWN;

		try
		{
			String retStr = UNKNOWN;

			String sql = "select value from master..syscurconfigs where config = 123";
			
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
			_logger.debug("When getting ASE sortorder ID, Caught exception.", e);

			return UNKNOWN;
		}
	}

	public static String getClientCharsetId(Connection conn)
	{
		final String UNKNOWN = "";

		if ( ! isConnectionOk(conn, true, null) )
			return UNKNOWN;

		try
		{
			String retStr = UNKNOWN;

			String sql = "select @@client_csid";
			
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
			_logger.debug("When getting Client charset id, Caught exception.", e);

			return UNKNOWN;
		}
	}

	public static String getClientCharsetName(Connection conn)
	{
		final String UNKNOWN = "";

		if ( ! isConnectionOk(conn, true, null) )
			return UNKNOWN;

		try
		{
			String retStr = UNKNOWN;

			String sql = "select name from master.dbo.syscharsets where id = @@client_csid";

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
			_logger.debug("When getting Client charset id, Caught exception.", e);

			return UNKNOWN;
		}
	}

	public static String getClientCharsetDesc(Connection conn)
	{
		final String UNKNOWN = "";

		if ( ! isConnectionOk(conn, true, null) )
			return UNKNOWN;

		try
		{
			String retStr = UNKNOWN;
 
			String sql = "select description from master.dbo.syscharsets where id = @@client_csid";

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
			_logger.debug("When getting Client charset id, Caught exception.", e);

			return UNKNOWN;
		}
	}

	public static String getAsaCharset(Connection conn)
	{
		final String UNKNOWN = "UNKNOWN";

		if ( ! isConnectionOk(conn, true, null) )
			return UNKNOWN;

		try
		{
			String retStr = UNKNOWN;

			String sql = "select * from syscollation";
			
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				retStr = rs.getString(2).trim();
			}
			rs.close();
			stmt.close();

			return retStr;
		}
		catch (SQLException e)
		{
			_logger.debug("When getting ASE charset, Caught exception.", e);

			return UNKNOWN;
		}
	}

	public static String getAsaSortorder(Connection conn)
	{
		final String UNKNOWN = "UNKNOWN";

		if ( ! isConnectionOk(conn, true, null) )
			return UNKNOWN;

		try
		{
			String retStr = UNKNOWN;

			String sql = "select * from syscollation";
			
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				retStr = rs.getString(3).trim();
			}
			rs.close();
			stmt.close();

			return retStr;
		}
		catch (SQLException e)
		{
			_logger.debug("When getting ASE charset, Caught exception.", e);

			return UNKNOWN;
		}
	}

	/**
	 * When was ASE last started, as a String using pattern "yyyyMMdd.HHmmss"<br>
	 * if problems accessing ASE, it will return "epoch" (new Timestamp(0))
	 * 
	 * @param conn
	 * @param pattern
	 * @return
	 */
	public static String getAseStartDateAsString(Connection conn)
	{
		return getAseStartDateAsString(conn, "yyyyMMdd.HHmmss");
	}
	
	/**
	 * When was ASE last started,as a String using pattern<br>
	 * if problems accessing ASE, it will return "epoch" (new Timestamp(0))
	 * 
	 * @param conn
	 * @param pattern  pattern using SimpleDateFormat
	 * @return
	 */
	public static String getAseStartDateAsString(Connection conn, String pattern)
	{
		Timestamp startTs = getAseStartDate(conn);
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		
		return sdf.format(startTs);
	}
	/**
	 * When was ASE last started
	 * @param conn
	 * @return Timestamp when server started. if problems return <code>new Timestamp(0)</code>
	 */
	public static Timestamp getAseStartDate(Connection conn)
	{
		final Timestamp UNKNOWN = new Timestamp(0);

		if ( ! isConnectionOk(conn, true, null) )
			return UNKNOWN;

		try
		{
			Timestamp retTs = UNKNOWN;

			String sql = "select StartDate=max(StartDate) from master.dbo.monState";
			
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				retTs = rs.getTimestamp(1);
			}
			rs.close();
			stmt.close();

			if (retTs == null)
				retTs = new Timestamp(0);
			
			return retTs;
		}
		catch (SQLException e)
		{
			_logger.debug("When getting ASE StartTime, Caught exception.", e);

			return UNKNOWN;
		}
	}

	/** Check if a connection is ok or not, no GUI error, just return true or false */
	public static boolean isConnectionOk(Connection conn)
	{
		return isConnectionOk(conn, false, null);
	}
	public static boolean isConnectionOk(Connection conn, boolean guiMsgOnError, Component guiOwner)
	{
		String msg   = "";
		String title = "Checking DB Connection";

		if ( conn == null ) 
		{	
			msg = "The passed Connection object is null.";
			_logger.debug(msg);

			if (guiOwner != null)
				SwingUtils.showWarnMessage(guiOwner, title, msg, new Exception(msg));

			return false;
		}
		
		try
		{
			if ( conn.isClosed() )
			{
				msg = "The passed Connection object is NOT connected.";
				_logger.debug(msg);

				if (guiOwner != null)
					SwingUtils.showWarnMessage(guiOwner, title, msg, new Exception(msg));

				return false;
			}
		}
		catch (SQLException e)
		{
			_logger.debug("When checking the DB Connection, Caught exception.", e);

			if (guiOwner != null)
				showSqlExceptionMessage(guiOwner, "Checking DB Connection", "When checking the DB Connection, we got an SQLException", e);
			
			return false;
		}
		return true;
	}

//	public static String showSqlExceptionMessage(Component owner, String title, String msg, SQLException sqlex) 
//	{
//		String exMsg = getMessageFromSQLException(sqlex);
//
//		SwingUtils.showErrorMessage(owner, title, 
//			msg + "\n\n" + exMsg, sqlex);
//		
//		return exMsg;
//	}
	public static String showSqlExceptionMessage(Component owner, String title, String msg, SQLException sqlex) 
	{
		String exMsg    = getMessageFromSQLException(sqlex, true, "010HA"); // 010HA: The server denied your request to use the high-availability feature. Please reconfigure your database, or do not request a high-availability session.
		String exMsgRet = getMessageFromSQLException(sqlex, false);

		String extraInfo = ""; 

		// JZ00L: Login failed
		if (AseConnectionUtils.containsSqlState(sqlex, "JZ00L"))
//		if ( "JZ00L".equals(sqlex.getSQLState()) )
		{
			extraInfo  = "<br>" +
					"<b>Are you sure about the password, SQL State 'JZ00L'</b><br>" +
					"The first Exception 'JZ00L' in <b>most</b> cases means: wrong user or password<br>" +
					"Before you begin searching for strange reasons, please try to login again and make sure you use the correct password.<br>" +
					"<br>" +
					"Below is all Exceptions that happened during the login attempt.<br>" +
					"<HR NOSHADE>";
		}

		// JZ0IB: The server's default charset of roman8 does not map to an encoding that is available in the client Java environment. Because jConnect will not be able to do client-side conversion, the connection is unusable and is being closed. Try using a later Java version, or try including your Java installation's i18n.jar or charsets.jar file in the classpath.
		if (AseConnectionUtils.containsSqlState(sqlex, "JZ0IB"))
//		if ( "JZ0IB".equals(sqlex.getSQLState()) )
		{
			extraInfo  = "<br>" +
					"<b>Try a Workaround for the problem, SQL State 'JZ0IB'</b><br>" +
					"<ul>" + 
					"  <li>In the Connection Dialog, in the field 'URL Option' specify: <font size=\"4\"><code>CHARSET=iso_1</code></font></li>" +
					"  <li>Or simply choose '<font size=\"4\"><code>iso_1</code></font>' in the 'Client Charset' DropBox.</li>" +
					"</ul>" + 
					"<i>This will hopefully get you going, but characters might not converted correctly</i><br>" +
					"<br>" +
					"Below is all Exceptions that happened during the login attempt.<br>" +
					"<HR NOSHADE>";
		}

		String htmlStr = 
			"<html>" +
			  msg + "<br>" +
			  extraInfo + 
//			  "<br>" + 
//			  exMsg.replace("\n", "<br>") + 
			  exMsg + 
			"</html>"; 

		SwingUtils.showErrorMessage(owner, title, htmlStr, sqlex);
		
		return exMsgRet;
	}

	public static String getMessageFromSQLException(SQLException sqlex, boolean htmlTags, String... discardSqlState) 
	{
		StringBuffer sb = new StringBuffer("");
		if (htmlTags)
			sb.append("<UL>");

		ArrayList<String> discardedMessages = null;

		boolean first = true;
		while (sqlex != null)
		{
			boolean discardThis = false;
			for (String sqlState : discardSqlState)
			{
				if (sqlState.equals(sqlex.getSQLState()))
					discardThis = true;
			}

			if (discardThis)
			{
				if (discardedMessages == null)
					discardedMessages = new ArrayList<String>();
				discardedMessages.add( sqlex.getMessage() );
			}
			else
			{
    			if (first)
    				first = false;
    			else
    				sb.append( "\n" );
    
    			if (htmlTags)
    				sb.append("<LI>");
    			sb.append( sqlex.getMessage() );
    			if (htmlTags)
    				sb.append("</LI>");
			}

			sqlex = sqlex.getNextException();
		}

		if (htmlTags)
			sb.append("</UL>");

		if (discardedMessages != null)
		{
			sb.append(htmlTags ? "<BR><BR><B>Note</B>:" : "\nNote: ");
			sb.append("The following messages was also found but, they are not as important as above messages.").append(htmlTags ? "<br>" : "\n");
			if (htmlTags)
				sb.append("<HR NOSHADE>").append("<UL>");
			for (String msg : discardedMessages)
			{
    			if (htmlTags)
    				sb.append("<LI>");
    			sb.append( msg );
    			if (htmlTags)
    				sb.append("</LI>");
			}
			if (htmlTags)
				sb.append("</UL>");
		}
		
		return sb.toString();
	}

	/**
	 * Check if the SQLSate is part of the SQLException chain.
	 * 
	 * @param sqlex
	 * @param sqlstate
	 * @return
	 */
	public static boolean containsSqlState(SQLException sqlex, String sqlstate) 
	{
		while (sqlex != null)
		{
			if ( sqlstate.equals(sqlex.getSQLState()) )
				return true;

				sqlex = sqlex.getNextException();
		}

		return false;
	}

	/**
	 * Executes select @@version in the ASE Server and parses the output into a integer
	 * @param conn
	 * @return a int with the version number in the form:
	 * <ul>
	 *   <li>125000000 for (12.5.0 ESD#3)</li>
	 *   <li>125400900 for (12.5.4 ESD#9)</li>
	 *   <li>150300100 for (15.0.3 ESD#1)</li>
	 *   <li>155000000 for (15.5)</li>
	 *   <li>155000200 for (15.5 ESD#2)</li>
	 *   <li>157000402 for (15.7 ESD#4.2)</li>
	 *   <li>157010000 for (15.7 SP100)</li>
	 *   <li>160000101 for (16.0 SP01 PL01)</li>
	 * </ul>
	 * It's a 9 digit number where 112344455<br>
	 * 11 = Major Release: a 2 digit number<br>
	 * 2 = Minor Release: a 1 digit number<br>
	 * 3 = Maint Release: a 1 digit number<br>
	 * 444 = Service Pack or ESD: a 3 digit number<br>
	 * 55 = Patch Level or SUB ESD: a 2 digit number<br>
	 */
	public static long getAseVersionNumber(Connection conn)
	{
		long srvVersionNum = 0;

//		// @@version_number
//		try
//		{
//			Statement stmt = conn.createStatement();
//			ResultSet rs = stmt.executeQuery(SQL_VERSION_NUM);
//			while ( rs.next() )
//			{
//				srvVersionNum = rs.getInt(1);
//			}
//			rs.close();
//		}
//		catch (SQLException ex)
//		{
//			_logger.debug("MonTablesDictionary:getAseVersionNumber(), @@version_number, probably an early ASE version", ex);
//		}
	
		// version
		try
		{
			String srvVersionStr = "";

			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(SQL_VERSION);
			while ( rs.next() )
			{
				srvVersionStr = rs.getString(1);
			}
			rs.close();
	
			if (srvVersionNum == 0)
			{
				srvVersionNum = Ver.sybVersionStringToNumber(srvVersionStr);
			}
		}
		catch (SQLException ex)
		{
			_logger.error("MonTablesDictionary:getAseVersionNumber(), @@version", ex);
		}
		
		return srvVersionNum;
	}

	/**
	 * Executes 'admin version' in the Replication Server and parses the output into a integer
	 * @param conn
	 * @return a int with the version number in the form:
	 * <ul>
	 *   <li>12503 for (12.5.0 ESD#3)</li>
	 *   <li>12549 for (12.5.4 ESD#9)</li>
	 *   <li>15031 for (15.0.3 ESD#1)</li>
	 *   <li>15500 for (15.5)</li>
	 *   <li>15502 for (15.5 ESD#2)</li>
	 * </ul>
	 * If the ESD level is above 9 it will still return 9 (otherwise it would wrap...)
	 */
	public static long getRsVersionNumber(Connection conn)
	{
		long srvVersionNum = 0;

		// version
		try
		{
			String srvVersionStr = "";

			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("admin version");
			while ( rs.next() )
			{
				srvVersionStr = rs.getString(1);
			}
			rs.close();
	
			if (srvVersionNum == 0)
			{
				srvVersionNum = Ver.sybVersionStringToNumber(srvVersionStr);
			}
		}
		catch (SQLException ex)
		{
			_logger.error("AseConnectionUtils:getRsVersionNumber(), 'admin version'", ex);
		}
		
		return srvVersionNum;
	}

	/**
	 * Executes 'ra_version' in the Replication Agent and parses the output into a integer
	 * @param conn
	 * @return a int with the version number in the form:
	 * <ul>
	 *   <li>12503 for (12.5.0 ESD#3)</li>
	 *   <li>12549 for (12.5.4 ESD#9)</li>
	 *   <li>15031 for (15.0.3 ESD#1)</li>
	 *   <li>15500 for (15.5)</li>
	 *   <li>15502 for (15.5 ESD#2)</li>
	 * </ul>
	 * If the ESD level is above 9 it will still return 9 (otherwise it would wrap...)
	 */
	public static long getRaxVersionNumber(Connection conn)
	{
		long srvVersionNum = 0;

		// version
		try
		{
			String srvVersionStr = "";

			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("ra_version");
			while ( rs.next() )
			{
				srvVersionStr = rs.getString(1);
			}
			rs.close();
	
			if (srvVersionNum == 0)
			{
				srvVersionNum = Ver.sybVersionStringToNumber(srvVersionStr);
			}
		}
		catch (SQLException ex)
		{
			_logger.error("AseConnectionUtils:getRaVersionNumber(), 'ra_version'", ex);
		}
		
		return srvVersionNum;
	}

	public static long getAsaVersionNumber(Connection conn)
	{
		long srvVersionNum = 0;

		// version
		try
		{
			String srvVersionStr = "";

			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select @@version");
			while ( rs.next() )
			{
				srvVersionStr = rs.getString(1);
			}
			rs.close();
	
			if (srvVersionNum == 0)
			{
				srvVersionNum = Ver.asaVersionStringToNumber(srvVersionStr);
			}
		}
		catch (SQLException ex)
		{
			_logger.error("MonTablesDictionary:getAsaVersionNumber(), @@version", ex);
		}
		
		return srvVersionNum;
	}

	public static long getIqVersionNumber(Connection conn)
	{
		long srvVersionNum = 0;

		// version
		try
		{
			String srvVersionStr = "";

			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select @@version");
			while ( rs.next() )
			{
				srvVersionStr = rs.getString(1);
			}
			rs.close();
	
			if (srvVersionNum == 0)
			{
				srvVersionNum = Ver.iqVersionStringToNumber(srvVersionStr);
			}
		}
		catch (SQLException ex)
		{
			_logger.error("MonTablesDictionary:getAsaVersionNumber(), @@version", ex);
		}
		
		return srvVersionNum;
	}

	public static boolean isClusterEnabled(Connection conn)
	{
		String clusterMode = null;

		// @@clustermode
		try
		{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select @@clustermode");
			while ( rs.next() )
			{
				clusterMode = rs.getString(1).trim();
			}
			rs.close();
		}
		catch (SQLException ex)
		{
			_logger.debug("MonTablesDictionary:isClusterEnabled(), @@clustermode, probably an early ASE version", ex);
		}

		_logger.debug("Ase @@clustermode = '"+clusterMode+"'.");

		if (clusterMode == null)
			return false;
		
		if (clusterMode.equalsIgnoreCase("shared disk cluster"))
			return true;

		return false;
	}

	/**
	 * Set the ASE Cluster Editions system view, meaning should all instances be visible
	 * when querying the MDA tables or just the instance we are connected to.
	 * 
	 * @param conn Connection
	 * @param type <code>CE_SYSTEM_VIEW_CLUSTER or CE_SYSTEM_VIEW_INSTANCE</code>
	 */
	public static void setClusterEditionSystemView(Connection conn, int type)
	{
//		set system_view cluster -- current default at login time
//		set system_view instance

		String sql = null;
		if (type == CE_SYSTEM_VIEW_CLUSTER)  sql = "set system_view cluster";
		if (type == CE_SYSTEM_VIEW_INSTANCE) sql = "set system_view instance";

		if (sql == null)
		{
			String err = "The passed System View type='"+type+"', is unknown.";
			_logger.error(err);
			//throw new InvalidXXXXXXXException(err);
			return;
		}

		// Debug to get from where this method was called
//		Exception xxx = new Exception("setClusterEditionSystemView: "+sql);
//		xxx.printStackTrace();
		
		try
		{
			Statement stmt = conn.createStatement();
			stmt.execute(sql);
			stmt.close();
		}
		catch (SQLException ex)
		{
			String msg = AseConnectionUtils.showSqlExceptionMessage(null, Version.getAppName(), "Problems when setting 'system view' in ASE Server.", ex); 
			_logger.error("Problems when setting 'system view' in ASE Server. "+msg);
		}
	}

	/**
	 * Get the ASE Cluster Editions system view, meaning should all instances be visible
	 * when querying the MDA tables or just the instance we are connected to.
	 * 
	 * @param conn Connection
	 * @return <code>CE_SYSTEM_VIEW_CLUSTER or CE_SYSTEM_VIEW_INSTANCE or CE_SYSTEM_VIEW_UNKNOWN</code>
	 */
	public static int getClusterEditionSystemView(Connection conn)
	{
//		@@system_view -- current setting for this session
		String ceSystemView = null;
		try
		{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select @@system_view");
			while ( rs.next() )
			{
				ceSystemView = rs.getString(1);
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException ex)
		{
			String msg = AseConnectionUtils.showSqlExceptionMessage(null, Version.getAppName(), "Problems when getting 'system view' in ASE Server.", ex); 
			_logger.error("Problems when getting 'system view' in ASE Server. "+msg);
		}

		if (ceSystemView == null) 
			return CE_SYSTEM_VIEW_UNKNOWN;

		ceSystemView = ceSystemView.trim();
		if (ceSystemView.equalsIgnoreCase("cluster"))  return CE_SYSTEM_VIEW_CLUSTER;
		if (ceSystemView.equalsIgnoreCase("instance")) return CE_SYSTEM_VIEW_INSTANCE;

		return CE_SYSTEM_VIEW_UNKNOWN;
	}
	
	/**
	 * Check if various stuff has been enabled
	 * 
	 * @param conn Connection to use when checking stuff
	 * @param user User name to check for correct roles, if null the current connected user name will be used. 
	 * @param gui  Show error logs etc as GUI messages
	 * @param parent GUI parent component
	 * @param needsConfig an array of ASE Configuration that we can't do without. If null, nothing is required. 
	 * @return
	 */
//	public static boolean checkForMonitorOptions(Connection conn, String user, boolean gui, Component parent, String... needsConfig)
	public static boolean checkForMonitorOptions(DbxConnection conn, String user, boolean gui, Component parent, String... needsConfig)
	{
		long   srvVersionNum  = 0;
		String srvVersionStr  = "";
		String atAtServername = "";
		String aseLanguage    = "";
		String sql = "";
		try
		{
			// Set LANGUAGE to ENGLISH
			// if default langauge isn't 'english', then ASE 15.7 and above will throw error, when accessing monTables, monTableColumns, monWaitClassInfo, monWaitEventInfo
			//      Msg 12061, Level 16, State 1:
			//      Server 'goran', Line 1:
			//      Usen est une langue non prise en charge pour la localisation MDA !
			try
			{
				sql = "select @@language";
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(sql);
				while ( rs.next() )
				{
					aseLanguage = rs.getString(1);
				}
				rs.close();
				
				if ( ! "us_english".equals(aseLanguage))
				{
					_logger.info("Changing the connected ASE users default Language from '"+aseLanguage+"' to 'us_english'.");
					sql = "set language us_english";
					stmt = conn.createStatement();
					stmt.executeUpdate(sql);
	
					sql = "select @@language";
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					while ( rs.next() )
					{
						aseLanguage = rs.getString(1);
					}
					rs.close();
				}
			}
			catch (SQLException ex)
			{
				_logger.warn("checkForMonitorOptions, set or get @@language failed.", ex);
			}

			// Get the version of the ASE server
			// select @@version_number (new since 15 I think, this means the local try block)
			try
			{
				// ------------------------------
				sql = "select @@version_number";
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(sql);
				while ( rs.next() )
				{
					srvVersionNum = rs.getInt(1);
				}
				rs.close();
			}
			catch (SQLException ex)
			{
				_logger.debug("checkForMonitorOptions, @@version_number failed, probably an early ASE version", ex);
			}

			// ------------------------------
			sql = "select @@version";
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while ( rs.next() )
			{
				srvVersionStr = rs.getString(1);
			}
			rs.close();

			if ( ! srvVersionStr.startsWith(DbUtils.DB_PROD_NAME_SYBASE_ASE) )
			{
				String msg = "This doesn't look like an ASE server. @@version='"+srvVersionStr+"'.";
				_logger.error(msg);
				if (gui)
				{
					String msgHtml = 
						"<html>" +
						"This doesn't look like an Sybase ASE server.<br>" +
						"<br>" +
						"The Version String is '<code>"+srvVersionStr+"</code>'. <br>" +
						"In my book this ain't a ASE Server, so I can't continue.<br>" +
						"</html>";

					SwingUtils.showErrorMessage(parent, Version.getAppName()+" - connect check", msgHtml, null);
				}
				return false;
			}

			long srvVersionNumFromVerStr = Ver.sybVersionStringToNumber(srvVersionStr);
			srvVersionNum = Math.max(srvVersionNum, srvVersionNumFromVerStr);

			// MINIMUM ASE Version is 12.5.0.3
			if (srvVersionNum < Ver.ver(12,5,0,3,0))
			{
				// FIXME: 
				String msg = "The minimum ASE Version supported by "+Version.getAppName()+" is 12.5.0.3 in earlier releases MDA tables doesn't exists. Connected to @@version='"+srvVersionStr+"'.";
				_logger.error(msg);
				if (gui)
				{
					String msgHtml = 
						"<html>" +
						"The minimum ASE Version supported by "+Version.getAppName()+" is 12.5.0.3<br>" +
						"in earlier ASE releases MDA tables doesn't exists<br>" +
						"<br>" +
						"The Version String is '<code>"+srvVersionStr+"</code>'. <br>" +
						"</html>";

					SwingUtils.showErrorMessage(parent, Version.getAppName()+" - connect check", msgHtml, null);
				}
				return false;
			}

			// ------------------------------
			sql = "select @@servername";
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while ( rs.next() )
			{
				atAtServername = rs.getString(1);
			}
			rs.close();

			_logger.info("Just connected to an ASE Server named '"+atAtServername+"' with Version Number "+srvVersionNum+", and the Version String '"+srvVersionStr+"', using language '"+aseLanguage+"'.");

			
			// if user name is null or empty, then get current user
			if (StringUtil.isNullOrBlank(user))
			{
				sql = "select suser_name()";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while (rs.next())
				{
					user = rs.getString(1);
				}
				stmt.close();
				rs.close();
			}
			
			// Get various roles
			_logger.debug("Verify mon_role");
			stmt = conn.createStatement();
			rs = stmt.executeQuery("sp_activeroles 'expand_down'");
			boolean has_sa_role        = false;
			boolean has_sso_role       = false;
			boolean has_mon_role       = false;
			boolean has_sybase_ts_role = false;
			while (rs.next())
			{
				String roleName = rs.getString(1);
				if (roleName.equals("sa_role"))        has_sa_role        = true;
				if (roleName.equals("sso_role"))       has_sso_role       = true;
				if (roleName.equals("mon_role"))       has_mon_role       = true;
				if (roleName.equals("sybase_ts_role")) has_sybase_ts_role = true;
			}

			//---------------------------------
			// check for MON_ROLE
			//---------------------------------
			final String PROPKEY_showDialogOnNoRole_mon_role = "AseConnectionUtils.showDialogOnNoRole.mon_role";

			if ( ! has_mon_role )
			{
				// Try to grant access to current user
//				if (has_sa_role && has_sso_role)
				if (has_sso_role)
				{
					_logger.info("User '"+user+"' has NOT got role 'mon_role', but since this users do have 'sso_role', I will automatically try to grant 'mon_role' to the user '"+user+"'.");

					sql = "sp_role 'grant', 'mon_role', '"+user+"'";
					stmt.execute(sql);
					_logger.info("Executed: "+sql);

					sql = "set role 'mon_role' on";
					stmt.execute(sql);
					_logger.info("Executed: "+sql);

					// re-check if grant of mon_role succeeded
					sql = "sp_activeroles 'expand_down'";
					rs = stmt.executeQuery(sql);
					has_mon_role = false;
					while (rs.next())
					{
						if (rs.getString(1).equals("mon_role"))
							has_mon_role = true;
					}
				}
				else
				{
					_logger.info("Automatic grant of 'mon_role' to user '"+user+"' can't be done. This since the user '"+user+"' doesn't have 'sso_role'.");
				}

				// If mon_role was still unsuccessfull
				if ( ! has_mon_role )
				{
					String msg = "You need 'mon_role' to access monitoring tables, the login attempt will be continued, but with limited access.";
					_logger.warn(msg);

					boolean showInfo = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_showDialogOnNoRole_mon_role, true);
					if (gui && showInfo)
					{
						String msgHtml = 
							"<html>" +
							"<b>You need 'mon_role' to access monitoring tables</b><br>" +
							"<br>" +
							"Have your system administrator grant 'mon_role' to the login '"+user+"'.<br>" +
							"<i>Note: if current user '"+user+"' had 'sso_role', then the grant is automatically done by "+Version.getAppName()+".</i><br>" +
							"<br>" +
							"The grant can be done with the following command:<br>" +
//							"<font size=\"4\">" +
							"  <code>isql -Usa -Psecret -S"+atAtServername+" -w999 </code><br>" +
							"  <code>1> sp_role 'grant', 'mon_role', '"+user+"'</code><br>" +
							"  <code>2> go</code><br>" +
//							"</font>" +
							"<br>" +
							"<HR NOSHADE>" +
							"<b>This is only a warning message, You will still be allowed to login.</b><br>" +
							"<br>" +
							"But a <b>very restricted</b> functionality will be available<br>" +
							"Most <i>if not all</i> Performance Counters is disabled.<br>" +
							"A basic set of graphs will be enabled. <br>" +
							"<ul>" +
							"  <li>CPU Summary, Global Variables</li>" +
							"  <li>Connections/Users in ASE</li>" +
							"  <li>Disk read/write, Global Variables</li>" +
							"  <li>Network Packets received/sent, Global Variables</li>" +
							"</ul>" +
							"So make sure you get 'mon_role' granted so you can start looking at all Performance Counters.<br>" +
							"</html>";

//						SwingUtils.showErrorMessage(parent, "Problems when checking 'Monitor Role'", 
//						msgHtml, null);

						// Create a check box that will be passed to the message
						JCheckBox chk = new JCheckBox("Show this information on next connect attempt.", showInfo);
						chk.addActionListener(new ActionListener()
						{
							@Override
							public void actionPerformed(ActionEvent e)
							{
								Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
								if (conf == null)
									return;
								conf.setProperty(PROPKEY_showDialogOnNoRole_mon_role, ((JCheckBox)e.getSource()).isSelected());
								conf.save();
							}
						});

						SwingUtils.showWarnMessageExt(parent, "Problems when checking 'Monitor Role'",
								msgHtml, chk, (JPanel)null);
					}
//					return false;
				}
			} // end: ! has_mon_role
			else
			{
				// Remove the 'Show this information on next connect attempt.' if we have access
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf != null)
				{
					conf.remove(PROPKEY_showDialogOnNoRole_mon_role);
					conf.save();
				}
			}
			
			//---------------------------------
			// check for SYBASE_TS_ROLE
			//---------------------------------
			final String PROPKEY_showDialogOnNoRole_sybase_ts_role = "AseConnectionUtils.showDialogOnNoRole.sybase_ts_role";

			if ( ! has_sybase_ts_role )
			{
				// Try to grant access to current user
				if ( has_sso_role )
				{
					_logger.info("User '"+user+"' has NOT got role 'sybase_ts_role', but since this users do have 'sso_role', I will automatically try to grant 'sybase_ts_role' to the user '"+user+"'.");

					sql = "sp_role 'grant', 'sybase_ts_role', '"+user+"'";
					stmt.execute(sql);
					_logger.info("Executed: "+sql);

					sql = "set role 'sybase_ts_role' on";
					stmt.execute(sql);
					_logger.info("Executed: "+sql);

					// re-check if grant of mon_role succeeded
					sql = "sp_activeroles 'expand_down'";
					rs = stmt.executeQuery(sql);
					has_sybase_ts_role = false;
					while (rs.next())
					{
						if (rs.getString(1).equals("sybase_ts_role"))
							has_sybase_ts_role = true;
					}
				}
				else
				{
					_logger.info("Automatic grant of 'sybase_ts_role' to user '"+user+"' can't be done. This since the user '"+user+"' doesn't have 'sso_role'.");
				}

				// If mon_role was still unsuccessfull
				// but show the message only if you have mon_role, otherwise it will be to many messages
				if ( ! has_sybase_ts_role && has_mon_role )
				{
					String msg = "You may need 'sybase_ts_role' to access some DBCC functionality or other commands used while monitoring.";
					_logger.warn(msg);

					boolean showInfo = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_showDialogOnNoRole_sybase_ts_role, true);
					if (gui && showInfo)
					{
						String msgHtml = 
							"<html>" +
							"You need 'sybase_ts_role' to access some DBCC functionality or other commands used while monitoring.<br>" +
							"This is especially true for Performance Counter 'Active Statements', if you enable 'Get DBCC SQL Text' or 'Get ASE Stacktrace'.<br>" +
							"<br>" +
							"<b>This is only a warning message, You will still be allowed to login.</b><br>" +
							"<br>" +
							"Have your system administrator grant 'sybase_ts_role' to the login '"+user+"'.<br>" +
							"Note: if user '"+user+"' has 'sso_role', then "+Version.getAppName()+" would have done this automatically.<br>" +
							"<br>" +
							"This can be done with the following command:<br>" +
//							"<font size=\"4\">" +
							"  <code>isql -Usa -Psecret -S"+atAtServername+" -w999 </code><br>" +
							"  <code>1> sp_role 'grant', 'sybase_ts_role', '"+user+"'</code><br>" +
							"  <code>2> go</code><br>" +
//							"</font>" +
							"</html>";

						// Create a check box that will be passed to the message
						JCheckBox chk = new JCheckBox("Show this information on next connect attempt.", showInfo);
						chk.addActionListener(new ActionListener()
						{
							@Override
							public void actionPerformed(ActionEvent e)
							{
								Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
								if (conf == null)
									return;
								conf.setProperty(PROPKEY_showDialogOnNoRole_sybase_ts_role, ((JCheckBox)e.getSource()).isSelected());
								conf.save();
							}
						});
						
//						SwingUtils.showWarnMessage(parent, "Problems when checking 'Sybase TS Role'", 
//								msgHtml, null);
						
						SwingUtils.showWarnMessageExt(parent, "Problems when checking 'Sybase TS Role'",
								msgHtml, chk, (JPanel)null);
					}
				}
			} // end: ! has_sybase_ts_role
			else
			{
				// Remove the 'Show this information on next connect attempt.' if we have access
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf != null)
				{
					conf.remove(PROPKEY_showDialogOnNoRole_sybase_ts_role);
					conf.save();
				}
			}

			//---------------------------------
			// force master
			sql = "use master";
			stmt.executeUpdate(sql);

			_logger.debug("Verify monTables existance");

			//---------------------------------
			// Check if montables are configured
			if (has_mon_role)
			{
				sql = "select count(*) from master..sysobjects where name ='monTables'";
				rs = stmt.executeQuery(sql);
				while (rs.next())
				{
					if (rs.getInt(1) == 0)
					{
						String scriptName = "$SYBASE/$SYBASE_ASE/scripts/installmontables";
//						if (srvVersionNum >= 15000)
//							scriptName = "$SYBASE/$SYBASE_ASE/scripts/installmaster";
//						if (srvVersionNum >= 1500000)
//							scriptName = "$SYBASE/$SYBASE_ASE/scripts/installmaster";
						if (srvVersionNum >= Ver.ver(15,0))
							scriptName = "$SYBASE/$SYBASE_ASE/scripts/installmaster";
	
						String msg = "Monitoring tables must be installed ( please apply '"+scriptName+"' )";
						_logger.error(msg);
						if (gui)
						{
							String msgHtml = 
								"<html>" +
								"ASE Monitoring tables hasn't been installed. <br>" +
								"<br>" +
								"ASE Version is '"+Ver.versionNumToStr(srvVersionNum)+"'. <br>" +
								"Please apply '"+scriptName+"'.<br>" +
								"" +
								"<br>" +
								"Do the following on the machine that hosts the ASE:<br>" +
								"<font size=\"4\">" +
								"  <code>isql -Usa -Psecret -S"+atAtServername+" -w999 </code><br>" +
								"  <code>1> sp_addserver loopback, null, @@servername</code><br>" +
								"  <code>2> go</code><br>" +
								"  <code>isql -Usa -Psecret -S"+atAtServername+" -w999 -i"+scriptName+"</code><br>" +
								"</font>" +
								"</html>";
	
							SwingUtils.showErrorMessage(parent, Version.getAppName()+" - connect check", msgHtml, null);
						}
						return false;
					}
				}
			}

			//---------------------------------
			// Check if configuration 'xxx' is activated
			// if not maybe open the Configuration Dialog, but only if 'sa_role'
			//---------------------------------
			if (has_mon_role)
			{
				// Check if configuration 'xxx' is activated
				if (needsConfig != null)
				{
					sql = "java:method:checkAseConfig()";
					_logger.debug("Verify monitor configuration: "+StringUtil.toCommaStr(needsConfig));

					String errorMesage = checkAseConfig(conn, needsConfig, true, has_sa_role);
					if ( errorMesage != null )
					{
						if (gui)
						{
							// First build up an extra dialog that will be used if config is not changed
							String msgHtml = 
								"<html>" +
								"<br>" +
								"<HR NOSHADE>" +
								"<b>You may proceed with the login, if you check the box below.</b><br>" +
								"<br>" +
								"But a <b>very restricted</b> functionality will be available<br>" +
								"Most <i>if not all</i> Performance Counters is disabled.<br>" +
								"A basic set of graphs will be enabled. <br>" +
								"<ul>" +
								"  <li>CPU Summary, Global Variables</li>" +
								"  <li>Connections/Users in ASE</li>" +
								"  <li>Disk read/write, Global Variables</li>" +
								"  <li>Network Packets received/sent, Global Variables</li>" +
								"  <li>Transaction per second       <b>(only in 15.0.3 esd#3 and later)</b></li>" +
								"  <li>Run Queue Length, Per Engine <b>(only in 15.5 and later)</b></li>" +
								"  <li>Procedure Cache Module Usage <b>(only in 15.0.1 and later)</li>" +
								"</ul>" +
								"</html>";
							JPanel tmpPanel = new JPanel(new MigLayout());
							JLabel tmpLbl = new JLabel(msgHtml);
							JCheckBox tmpChk = new JCheckBox("Yes I still want to connect...", false);
							tmpPanel.add(tmpLbl, "push, grow, wrap");
							tmpPanel.add(tmpChk, "push, grow, wrap");
//							SwingUtils.showErrorMessage(parent, Version.getAppName()+" - connect check", errorMesage, null);

							if (has_sa_role)
							{
								// Show message, not with the extra dialog
								SwingUtils.showErrorMessageExt(parent, Version.getAppName()+" - connect check", errorMesage, null, (JPanel)null);

								// open config dialog
								AseConfigMonitoringDialog.showDialog(parent, conn, srvVersionNum, true);
		
								// After reconfig, go and check again
								errorMesage = checkAseConfig(conn, needsConfig, false, has_sa_role);
								
								// If we still have errors, get out of here with false=failure
								if ( errorMesage != null )
								{
									// Show message, now WITH the extra dialog
//									SwingUtils.showErrorMessage(parent, Version.getAppName()+" - connect check", errorMesage, null);
									SwingUtils.showErrorMessageExt(parent, Version.getAppName()+" - connect check", errorMesage, null, tmpPanel);

									if (tmpChk.isSelected())
									{
										_logger.warn("Continuing with a minimal environment. Config options '"+StringUtil.toCommaStr(needsConfig)+"' is still not enabled");
										return true;
									}
									_logger.warn("Login will be aborted due to: Config options '"+StringUtil.toCommaStr(needsConfig)+"' is still not enabled");
									return false;
								}
							}
							else
							{
								SwingUtils.showErrorMessageExt(parent, Version.getAppName()+" - connect check", errorMesage, null, tmpPanel);
								if (tmpChk.isSelected())
								{
									_logger.warn("Continuing with a minimal environment. Config options '"+StringUtil.toCommaStr(needsConfig)+"' is still not enabled");
									return true;
								}
								_logger.warn("Login will be aborted due to: Config options '"+StringUtil.toCommaStr(needsConfig)+"' is still not enabled");
								return false;
							}
						}
						else
						{
							_logger.warn("Login will be aborted due to: Config options '"+StringUtil.toCommaStr(needsConfig)+"' is not enabled");
							return false;
						}
					}
				}
			}

			_logger.debug("Connection passed 'Check Monitoring'.");
			return true;
		}
		catch (SQLException ex)
		{
			String msg = AseConnectionUtils.showSqlExceptionMessage(parent, 
					Version.getAppName()+" - connect", 
					"Problems when connecting to a ASE Server.<br>" +
					"Last Executed SQL: "+sql, 
					ex); 
			_logger.error("Problems when connecting to a ASE Server. Last Executed SQL '"+sql+"'. "+msg);
			return false;
		}
		catch (Exception ex)
		{
			_logger.error("Problems when connecting to a ASE Server. "+ex.toString());
			if (gui)
			{
				SwingUtils.showErrorMessage(parent, Version.getAppName()+" - connect", 
					"Problems when connecting to a ASE Server" +
					"\n\n"+ex.getMessage(), ex);
			}
			return false;
		}
	}

	/**
	 * @returns null if OK, a HTML message if problems
	 */
	private static String checkAseConfig(Connection conn, String[] needsConfig, boolean firstTimeCheck, boolean hasSaRole)
	throws SQLException
	{
		boolean notConfigured = false;
		String errorMesage = "<HTML><h1>Sorry the ASE server is "+(firstTimeCheck?"":"STILL ")+"not properly configured for monitoring.</h1>";
	       errorMesage += "<UL>";
		for (String cfgOption : needsConfig)
		{
			if (getAseConfigRunValue(conn, cfgOption) <= 0)
			{
				_logger.warn("ASE Configuration option '"+cfgOption+"' is NOT enabled.");
				errorMesage += "<LI> ASE option '"+cfgOption+"' is NOT enabled.";
				notConfigured = true;
			}
		}
		errorMesage += "</UL>";

		if ( firstTimeCheck )
		{
			if (hasSaRole)
			{
				errorMesage += "<b>I will now open the configuration panel for you.</b><BR>";
				errorMesage += "<b>A new check will be done after that.</b><BR>";
				errorMesage += "<BR>";
				errorMesage += "<b>Recomendation:</b><BR>";
				errorMesage += "Use the 'pre defined' configuration 'drop down' at the bottom.<BR>";
			}
			else
			{
				errorMesage += "<b>You don't have 'sa_role'...</b><BR>";
				errorMesage += "<b>So there is no need to open the configuration panel.</b><BR>";
			}
		}
		else
		{
			errorMesage += "<b>It's STILL a problem, please FIX the configuration manually.</b><BR>";
			errorMesage += "<b>Then you can try to connect again.</b>";
		}
		errorMesage += "</HTML>";

		return notConfigured ? errorMesage : null;
	}

	public static void setBasicAseConfigForMonitoring(Connection conn)
	throws SQLException
	{
		checkAndSetAseConfig(conn, "enable monitoring",           1); // Needs to be on: collects the monitoring table data. Data is not collected if enable monitoring is set to 0. enable monitoring acts as a master switch that determines whether any of the following configuration parameters are enabled.
	//-	checkAndSetAseConfig(conn, "per object statistics active",1); // collects statistics for each object.
//		checkAndSetAseConfig(conn, "statement statistics active", 1); // collects the monitoring tables statement-level statistics. You can use monProcessStatement to get statement statistics for a specific task.
//		checkAndSetAseConfig(conn, "enable stmt cache monitoring",1); // MDA tables monStatementCache and monCachedStatement display valid data
	//-	checkAndSetAseConfig(conn, "object lockwait timing",      1); // collects timing statistics for requests of locks on objects.
	//-	checkAndSetAseConfig(conn, "process wait events",         1); // collects statistics for each wait event for every task. You can get wait information for a specific task using monProcessWaits.
//		checkAndSetAseConfig(conn, "SQL batch capture",           1); // collects SQL text. If both SQL batch capture and max SQL text monitored are enabled, Adaptive Server collects the SQL text for each batch for each user task.
	//-	checkAndSetAseConfig(conn, "wait event timing",           1); // collects statistics for individual wait events. A task may have to wait for a variety of reasons (for example, waiting for a buffer read to complete). The monSysWaits table contains the statistics for each wait event. The monWaitEventInfo table contains a complete list of wait events.
//		checkAndSetAseConfig(conn, "deadlock pipe active",        1);
//		checkAndSetAseConfig(conn, "errorlog pipe active",        1);
//		checkAndSetAseConfig(conn, "sql text pipe active",        1);
//		checkAndSetAseConfig(conn, "statement pipe active",       1);
//		checkAndSetAseConfig(conn, "plan text pipe active",       0,    CONFIG_TYPE_IF_CFG_LT);
//		checkAndSetAseConfig(conn, "deadlock pipe max messages",  500,  CONFIG_TYPE_IF_CFG_LT);
//		checkAndSetAseConfig(conn, "errorlog pipe max messages",  200,  CONFIG_TYPE_IF_CFG_LT);
//		checkAndSetAseConfig(conn, "sql text pipe max messages",  1000, CONFIG_TYPE_IF_CFG_LT);
//		checkAndSetAseConfig(conn, "statement pipe max messages", 5000, CONFIG_TYPE_IF_CFG_LT);
//		checkAndSetAseConfig(conn, "plan text pipe max messages", 0,    CONFIG_TYPE_IF_CFG_LT);
		checkAndSetAseConfig(conn, "max SQL text monitored",      2048, CONFIG_TYPE_IF_CFG_LT);

	}

	public static boolean checkAndSetAseConfig(Connection conn, String config, int val)
	{
		return checkAndSetAseConfig(conn, config, val, CONFIG_TYPE_IF_CHANGED);
	}
	public static boolean checkAndSetAseConfig(Connection conn, String config, int val, int type)
	{
		try
		{
			int aseCfg = AseConnectionUtils.getAseConfigConfigValue(conn, config);
			boolean doConfig = false;

			if (type == CONFIG_TYPE_IF_CHANGED)
			{
				if ( aseCfg != val )
					doConfig = true;
			}
			else if (type == CONFIG_TYPE_IF_CFG_LT)
			{
				if ( aseCfg < val )
					doConfig = true;
			}
			else if (type == CONFIG_TYPE_IF_CFG_GT)
			{
				if ( aseCfg > val )
					doConfig = true;
			}
			else
			{
				throw new RuntimeException("checkAndSetAseConfig, unknown type="+type);
			}
			if (doConfig)
			{
				AseConnectionUtils.setAseConfigValue(conn, config, val);
				return true;
			}
			return false;
		}
		catch (SQLException sqle)
		{
			String errStr = "";
			while (sqle != null)
			{
				errStr += sqle.getMessage() + " ";
				sqle = sqle.getNextException();
			}
		}
		return false;
	}

	public static void setAseConfigValue(Connection conn, String config, boolean val)
	throws SQLException
	{
		setAseConfigValue(conn, config, (val == true) ? 1 : 0);
	}

	public static void setAseConfigValue(Connection conn, String config, int val)
	throws SQLException
	{
		setAseConfigValue( conn, config, Integer.toString(val) );
	}

	public static void setAseConfigValue(Connection conn, String config, String val)
	throws SQLException
	{
		if (val == null)
			return;

		boolean isNumber = false;
		try 
		{ 
			Integer.parseInt(val); 
			isNumber = true;
		}
		catch (NumberFormatException ignore) 
		{/*ignore*/}

		if ( ! isNumber )
			val = "'" + val.trim() + "'";

		_logger.info("Setting ASE Configuration '"+config+"' to value '"+val+"'.");

		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("sp_configure '"+config+"', "+val);
		while (rs.next())
		{
		}
		rs.close();
		stmt.close();
	}

	public static int getAseConfigRunValue(Connection conn, String config)
	throws SQLException
	{
		int    val = -1;

		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("sp_configure '"+config+"'");
		while (rs.next())
		{
			val = rs.getInt(5);
		}
		rs.close();
		stmt.close();
		
		return val;
	}

	public static String getAseConfigRunValueStr(Connection conn, String config)
	throws SQLException
	{
		String val = null;

		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("sp_configure '"+config+"'");
		while (rs.next())
		{
			val = rs.getString(5);
		}
		rs.close();
		stmt.close();
		
		if ( val != null )
			val = val.trim();

		return val;
	}

	public static int getAseConfigRunValueNoEx(Connection conn, String config)
	{
		int val = -1;
		try
		{
			val = getAseConfigRunValue(conn, config);
		}
		catch (SQLException ex)
		{
			// ignore
		}
		return val;
	}

	public static boolean getAseConfigRunValueBooleanNoEx(Connection conn, String config)
	{
		int val = -1;
		try
		{
			val = getAseConfigRunValue(conn, config);
		}
		catch (SQLException ex)
		{
			// ignore
		}
		return (val > 0);
	}

	public static String getAseConfigRunValueStrNoEx(Connection conn, String config)
	{
		String val = null;
		try
		{
			val = getAseConfigRunValueStr(conn, config);
		}
		catch (SQLException ex)
		{
			// ignore
		}
		return val;
	}

	public static int getAseConfigConfigValue(Connection conn, String config)
	throws SQLException
	{
		int    val = -1;

		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("sp_configure '"+config+"'");
		while (rs.next())
		{
			val = rs.getInt(4);
		}
		rs.close();
		stmt.close();
		
		return val;
	}

	public static boolean isAseConfigStatic(Connection conn, String config)
	throws SQLException
	{
		String val = "";

		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("sp_configure '"+config+"'");
		while (rs.next())
		{
			val = rs.getString(7);
		}
		rs.close();
		stmt.close();
		
		return val.equals("static");
	}


	/** 
	 * compatibility_mode was introduced in 15.0.3.1, 
	 * and it will enforce the ASE server to use 12.5.4 Optimizer & Execution Engine.
	 * Note: showplan will be "without" the '|' vertical bars.
	 * 
	 * @param conn
	 * @param on
	 * @return
	 */
	public static boolean setCompatibilityMode(Connection conn, boolean on)
	{
		String option = "compatibility_mode";
		return setAseOption(conn, option, on);
	}

	public static boolean getCompatibilityMode(Connection conn)
	{
		String option = "compatibility_mode";
		return getAseOption(conn, option);
	}

	/**
	 * do: set xxxxxxxxxxx on|off
	 * 
	 * @param conn
	 * @param option
	 * @param on
	 * @return
	 */
	public static boolean setAseOption(Connection conn, String option, boolean on)
	{
		String sql = "set " + option + (on ? " on" : " off");
		try
		{
			Statement stmnt = conn.createStatement();
			stmnt.executeUpdate(sql);
			stmnt.close();

			return true;
		}
		catch (SQLException e)
		{
			_logger.warn("Problems execute SQL '"+sql+"', Caught: " + e.toString() );
			return false;
		}
	}

	public static boolean getAseOption(Connection conn, String option)
	{
		String sql = 
			"-- check if we are in 'compatibility_mode', introduced in 15.0.3 esd#1 \n" +
			"declare @option int \n" +
			"select @option = 0  \n" +
			"    \n" +
			"select @option = sign(convert(tinyint,substring(@@options, c.low, 1)) & c.high) \n" +
			"  from master.dbo.spt_values a, master.dbo.spt_values c \n" +
			"  where a.number = c.number \n" +
			"    and c.type = 'P' \n" +
			"    and a.type = 'N' \n" +
			"    and c.low <= datalength(@@options) \n" +
			"    and a.name = '"+option+"' \n" +
			"    \n" +
			"    select @option\n" +
			"\n";

		try
		{
			int resultOption = 0;
			Statement stmnt = conn.createStatement();
			ResultSet rs = stmnt.executeQuery(sql);
			while (rs.next())
			{
				resultOption = rs.getInt(1);
			}
			rs.close();
			stmnt.close();

			return resultOption > 0;
		}
		catch (SQLException e)
		{
			_logger.warn("Problems execute SQL '"+sql+"', Caught: " + e.toString() );
			return false;
		}
	}

	public static final String SA_ROLE           = "sa_role";
	public static final String SSO_ROLE          = "sso_role";
	public static final String OPER_ROLE         = "oper_role";
	public static final String SYBASE_TS_ROLE    = "sybase_ts_role";
	public static final String NAVIGATOR_ROLE    = "navigator_role";
	public static final String REPLICATION_ROLE  = "replication_role";
	public static final String DTM_TM_ROLE       = "dtm_tm_role";
	public static final String HA_ROLE           = "ha_role";
	public static final String MON_ROLE          = "mon_role";
	public static final String JS_ADMIN_ROLE     = "js_admin_role";
	public static final String MESSAGING_ROLE    = "messaging_role";
	public static final String JS_CLIENT_ROLE    = "js_client_role";
	public static final String JS_USER_ROLE      = "js_user_role";
	public static final String WEBSERVICES_ROLE  = "webservices_role";
	public static final String KEYCUSTODIAN_ROLE = "keycustodian_role";

	/**
	 * Check if a role is activated for a specific connection
	 * @param conn The connection to get active roles from
	 * @param role what role name do we check for
	 * @return true if the role is active
	 */
	public static boolean hasRole(Connection conn, String role)
	{
		int val = -1;
		String sql = "select proc_role('"+role+"')";
		try
		{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				val = rs.getInt(1);
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems when executing sql: "+sql, ex);
		}
		_logger.debug("hasRole(role='"+role+"'): SQL 'select proc_role(rolename)' returned="+val+", so rasRole() will return="+(val > 0));
		return (val > 0);
	}

	/**
	 * Get all active roles this connection/login has activated
	 * @param conn The connection to get active roles from
	 * @return A List containing Strings of active role names
	 */
	public static List<String> getActiveSystemRoles(Connection conn)
	{
		String sql = "select show_role()";
		try
		{
//			List<String> roleList = null;
			List<String> roleList = new LinkedList<String>();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				if (roleList == null)
					roleList = new LinkedList<String>();
				String val = rs.getString(1);
				if (val != null)
				{
					String[] sa = val.split(" ");
					for (int i=0; i<sa.length; i++)
					{
						String role = sa[i].trim();
						if ( ! roleList.contains(role) )
							roleList.add(role);
					}
				}
			}
			rs.close();
			stmt.close();

			_logger.debug("getRoles(roleList='"+roleList+"'.");
			return roleList;
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems when executing sql: "+sql, ex);
			return null;
		}
	}



	/**
	 * Get all active roles this connection/login has activated
	 * @param conn The connection to get active roles from
	 * @return A List containing Strings of active role names
	 */
	public static List<String> getActiveRoles(Connection conn)
	{
		String sql = "exec sp_activeroles 'expand_down'";
		try
		{
			List<String> roleList = new LinkedList<String>();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				String role = rs.getString(1);
				if ( ! roleList.contains(role) )
					roleList.add(role);
			}
			rs.close();
			stmt.close();

			_logger.debug("getActiveRoles(roleList='"+roleList+"'.");
			return roleList;
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems when executing sql: "+sql, ex);
			return null;
		}
	}



	/**
	 * Do sp_configure "Monitoring"
	 * @param conn The connection to get configuration from
	 * @return A Map containing <Strings,Integer> of configuration
	 */
	public static Map<String,Integer> getMonitorConfigs(Connection conn)
	{
		String sql = "exec sp_configure 'Monitoring'";
		try
		{
			Map<String,Integer> configMap = null;
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				if (configMap == null)
					configMap = new LinkedHashMap<String,Integer>();
				String name     = rs.getString(1);
				int    runValue = rs.getInt(5);

				configMap.put(name, runValue);
			}
			rs.close();
			stmt.close();

			_logger.debug("getMonitorConfigs(configMap='"+configMap+"'.");
			return configMap;
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems when executing sql: "+sql, ex);
			return null;
		}
	}


	/**
	 * execute 'sp_showplan SPID, null, null, null'
	 * <br>
	 * Note: sp_showplan in some ASE 16 releases stacktraces...<br>
	 * Fixed: in ASE 16.0 SP2 PL5, or SP3<br>
	 * CR: 796763, note: 2321932<br>
	 * 
	 * @param conn 
	 * @param spid The SPID to do showplan on
	 * @param htmlStartStr
	 * @param addHtmlTags  Adds <html><code> the Showplan Text </code></html> and <br> after every line
	 * @return The showplan text
	 */
	public static String getShowplan(Connection conn, int spid, String htmlStartStr, boolean addHtmlTags)
	{
		if (htmlStartStr == null)
			htmlStartStr = "";

		String htmlBegin   = "";
		String htmlEnd     = "";
		String htmlNewLine = "\n";
		if ( addHtmlTags )
		{
			htmlBegin   = "<html>"+htmlStartStr+"<pre>";
			htmlEnd     = "</pre></html>";
//			htmlNewLine = "<br>";
		}
		String showplanExtraParamInAse16 = "";
		if (conn instanceof DbxConnection)
		{
			if (((DbxConnection) conn).getDbmsVersionNumber() >= Ver.ver(16, 0))
			{
				boolean useLongOption = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_getShowplan_useLongOption, DEFAULT_getShowplan_useLongOption);
				if (useLongOption)
					showplanExtraParamInAse16 = ", 'long'";
			}
		}
		StringBuilder sb = null;
		String sql = "exec sp_showplan "+spid+", null, null, null" + showplanExtraParamInAse16;

		// Set an empty Message handler
		SybMessageHandler curMsgHandler = null;
		if (conn instanceof SybConnection)
		{
			curMsgHandler = ((SybConnection)conn).getSybMessageHandler();
			((SybConnection)conn).setSybMessageHandler(null);
		}
		// Set a TDS Message Handler
		if (conn instanceof TdsConnection)
			((TdsConnection)conn).setSybMessageHandler(null);

		try
		{
			Statement stmnt = conn.createStatement();
			boolean hasRs = stmnt.execute(sql);
			ResultSet rs;
			int rowsAffected = 0;
			do
			{
				// In ASE 16, there is a ResultSet, that contains the SQL Statement
				if(hasRs)
				{
					if (sb == null)
						sb = new StringBuilder(htmlBegin).append("---- BEGIN: SQL Statement Executed ------------------------------------").append(htmlNewLine);

					rs = stmnt.getResultSet();
					//int colCount = rs.getMetaData().getColumnCount();
					while (rs.next())
					{
						sb = sb.append(rs.getString(1));
					}
					rs.close();
					sb.append(htmlNewLine).append("---- END: SQL Statement Executed --------------------------------------").append(htmlNewLine);
				}
				else // Treat update/row count(s) for NON RESULTSETS
				{
					// Java DOC: getUpdateCount() Retrieves the current result as an update count; if the result is a ResultSet object 
					//           or there are no more results, -1 is returned. This method should be called only once per result.
					// Without this else statement, some drivers might fail... (MS-SQL actually did)

					rowsAffected = stmnt.getUpdateCount();
					if (rowsAffected >= 0)
						_logger.debug("---- DDL or DML (statement with no-resultset) Rowcount: "+rowsAffected);
					else
						_logger.debug("---- No more results to process.");
				} // end: no-resultset

				hasRs = stmnt.getMoreResults();
			}
			while (hasRs || rowsAffected != -1);

			// Read messages: this is where The ShowPlan is
			for (SQLWarning sqlw = stmnt.getWarnings(); sqlw != null; sqlw = sqlw.getNextWarning())
			{
				// Ignore "10233 01ZZZ The specified statement number..." message
				if (sqlw.getErrorCode() == 10233)
					continue;
				// Ignore "010P4: An output parameter was received and ignored." message
				if (sqlw.getSQLState() == "010P4")
					continue;

				if (sb == null)
					sb = new StringBuilder(htmlBegin);

				String msg = sqlw.getMessage();
				sb.append(msg);
				if ( ! msg.endsWith("\n"))
					sb.append(htmlNewLine);
			}

			stmnt.close();
		}
		catch (SQLException e)
		{
			_logger.warn("Problems when executing sql: "+sql, e);
		}
		finally
		{
			// restore old message handler
			if (curMsgHandler != null)
			{
				((SybConnection)conn).setSybMessageHandler(curMsgHandler);
			}
			// Restore old message handler
			if (conn instanceof TdsConnection)
				((TdsConnection)conn).restoreSybMessageHandler();
		}

		if (sb == null)
			return null;
		while(sb.charAt(sb.length()-1) == '\n')
			sb.deleteCharAt(sb.length()-1);
		return sb.append(htmlEnd).toString();
	}



//	public static xxx getActiveTraceSwitches()
//	{
//		1> show switch
//		2> go
//		Local switches set :   302 (print_plan_index_selection),  3604 (print_output_to_client).
//		Serverwide switches set :  3604 (print_output_to_client).
//		1>
//	}


	/** NOTE this is a quick fix, need to implement this in a MonConnection object instead */
	private static Map<String, List<Integer>> _connHasTraceFlagEnabled = new HashMap<String, List<Integer>>();
	/** NOTE this is a quick fix, need to implement this in a MonConnection object instead */
	private static boolean __isDbccTraceOnInMap(Connection conn, int trace)
	{
		if (conn == null) 
			return false;
		List<Integer> traceList = _connHasTraceFlagEnabled.get(conn.toString());
		if (traceList == null)
			return false;
		return traceList.contains(trace);
	}
	/** NOTE this is a quick fix, need to implement this in a MonConnection object instead */
	private static void __setDbccTraceOnInMap(Connection conn, int trace)
	{
		if (conn == null) 
			return;
		List<Integer> traceList = _connHasTraceFlagEnabled.get(conn.toString());
		if (traceList == null)
			traceList = new ArrayList<Integer>();
		traceList.add(trace);
	}

	public static void dbccTraceOn(Connection conn, int trace)
	{
		// If the trace is already ON, get out of here.
		if (__isDbccTraceOnInMap(conn, trace))
			return;

		// Works with above ASE 12.5.4 and 15.0.2
		String setSwitch   = "set switch on "+trace+" with no_info";

		// Used as fallback if above 'set switch...' is failing
		String dbccTraceon = "DBCC traceon("+trace+")";

		// TRY with set switch
		// This should also be changed to check if the MonConnection, is of version... MonConnection needs to be implemented
		try
		{
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(setSwitch);
			stmt.close();
			__setDbccTraceOnInMap(conn, trace);
		}
		catch (SQLException e)
		{
			_logger.debug("Problems when executing sql '"+setSwitch+"', I will fallback and use '"+dbccTraceon+"' instead.");

			// Fallback and use DBCC TRACEON
			try
			{
				Statement stmt = conn.createStatement();
				stmt.executeUpdate(dbccTraceon);
				stmt.close();
				__setDbccTraceOnInMap(conn, trace);
			}
			catch (SQLException e2)
			{
				_logger.warn("Problems when executing sql: "+dbccTraceon, e2);
			}
		}

	}

//	/**
//	 * Check if trace is enabled at the server.
//	 * <br>
//	 * First try: <code>show switch</code><br>
//	 * If it fails try: <code>dbcc traceon(3604) dbcc traceflags dbcc traceoff(3604)</code><br>
//	 * 
//	 * @param conn
//	 * @param trace
//	 */
//	public static boolean isTraceEnabled(Connection conn, int trace)
//	throws SQLException
//	{
//		// Works with above ASE 12.5.4 and 15.0.2
//		String showSwitch   = "show switch";
//
//		// Used as fallback if above 'set switch...' is failing
//		String dbccTraceFlags = "dbcc traceon(3604) dbcc traceflags dbcc traceoff(3604)";
//
//		SQLException sqlEx = null;
//		// TRY with show switch
//		// This should also be changed to check if the MonConnection, is of version... MonConnection needs to be implemented
//		try
//		{
//			Statement stmt = conn.createStatement();
//			stmt.executeUpdate(showSwitch);
//
//			SQLWarning sqlwarn = stmt.getWarnings();
//			while (sqlwarn != null)
//			{
//				// output looks like this: Serverwide switches set :  3650,  3651.
//				if (sqlwarn.getMessage().indexOf(" "+trace+",") >= 0 || sqlwarn.getMessage().indexOf(" "+trace+".") >= 0)
//					return true;
//				sqlwarn = sqlwarn.getNextWarning();
//			}
//			stmt.close();
//			return false;
//		}
//		catch (SQLException e)
//		{
//			_logger.debug("Problems when executing sql '"+showSwitch+"', I will fallback and use '"+dbccTraceFlags+"' instead.");
//
//			// Fallback and use DBCC traceflags
//			try
//			{
//				Statement stmt = conn.createStatement();
//				stmt.executeUpdate(dbccTraceFlags);
//
//				SQLWarning sqlwarn = stmt.getWarnings();
//				while (sqlwarn != null)
//				{
//					// output looks like this: Active traceflags: 3604, 3650, 3651
//					if (sqlwarn.getMessage().indexOf(" "+trace+",") >= 0 || sqlwarn.getMessage().indexOf(" "+trace+"") >= 0) // NOTE: this might be true for "any" *3650*...
//						return true;
//					sqlwarn = sqlwarn.getNextWarning();
//				}
//				stmt.close();
//				return false;
//			}
//			catch (SQLException e2)
//			{
//				sqlEx = e2;
//				_logger.warn("Problems when executing sql: "+dbccTraceFlags, e2);
//				throw e2; // HERE WE DO THROW IF WE ECAUSED ALL OUR OPTIONS
//			}
//		}
//	}
	/**
	 * Check if trace is enabled at the server.
	 * <br>
	 * Uses: <code>dbcc istraceon(####)</code><br>
	 * 
	 * @param conn
	 * @param trace
	 */
	public static boolean isTraceEnabled(Connection conn, int trace)
	throws SQLException
	{
		// Tested that this works on 12.5.3 and above
		String dbccTraceFlags = 
			"dbcc istraceon("+trace+") \n" +
			"select istraceon = case when @@error = 0 then 1 else 0 end";

		try
		{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(dbccTraceFlags);

			int isTraceEnabled = 0;
			while (rs.next())
			{
				isTraceEnabled = rs.getInt(1);
			}
			rs.close();
			stmt.close();

			return isTraceEnabled > 0;
		}
		catch (SQLException e)
		{
			_logger.warn("Problems when executing sql: "+dbccTraceFlags, e);
			throw e;
		}
	}

	/** 
	 * Execute 'select * from monProcessSQLText where SPID = spid)' on the passed spid 
	 * @param conn The database connection to use
	 * @param spid The spid to get SQL Text for
	 * @param addHtmlTags add html tags around the sql output
	 * @return the sql text
	 */
	public static String monSqlText(Connection conn, int spid, boolean addHtmlTags)
	{
		String htmlBegin   = "";
		String htmlEnd     = "";
//		String htmlNewLine = "";
		if ( addHtmlTags )
		{
			htmlBegin   = "<html><pre>";
			htmlEnd     = "</pre></html>";
//			htmlNewLine = "<br>";
		}

		StringBuilder sb = null;
		String sql = "select BatchID, LineNumber, SequenceInLine, SQLText from master..monProcessSQLText where SPID = "+spid;
		try
		{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			int saveLineNumber = 1;
			while (rs.next())
			{
				if (sb == null)
					sb = new StringBuilder(htmlBegin);

				// Add a newline, before we "enter" a new LineNumber
				int atLineNum =  rs.getInt(2);
				String sqltext = rs.getString(4);
				//sqltext = StringUtil.makeApproxLineBreak(sqltext, 100, 5, "\n");
				
				// Add newlines for "" empty SQL lines...
				// For example if the First row from monProcessSQLText
				// has LineNumber 5, then there should be 4 newlines added.
				while (saveLineNumber < atLineNum)
				{
					sb.append("\n");
					saveLineNumber++;

					// do not end up in a endless loop...
					if (saveLineNumber > 10240) 
						break;
				}

				if (atLineNum == saveLineNumber)
					sb.append(sqltext);
				else
					sb.append(sqltext).append("\n");

				saveLineNumber = atLineNum;
			}

			rs.close();
			stmt.close();
		}
		catch (SQLException e)
		{
			_logger.warn("Problems when executing sql: "+sql, e);
		}

		if (sb == null)
			return null;
		while(sb.charAt(sb.length()-1) == '\n')
			sb.deleteCharAt(sb.length()-1);
		return sb.append(htmlEnd).toString();
	}

	/** 
	 * Execute 'DBCC sqltext(spid)' on the passed spid 
	 * @param conn The database connection to use
	 * @param spid The spid to db dbcc sqltext on
	 * @param addHtmlTags add html tags around the sql output
	 * @return the sting dbcc sqltext generated
	 */
	public static String dbccSqlText(Connection conn, int spid, boolean addHtmlTags)
	{
		String htmlBegin   = "";
		String htmlEnd     = "";
		String htmlNewLine = "";
		if ( addHtmlTags )
		{
			htmlBegin   = "<html><pre>";
			htmlEnd     = "</pre></html>";
//			htmlNewLine = "<br>";
		}

		// do: dbcc traceon
		dbccTraceOn(conn, 3604);

		StringBuilder sb = null;
		String sql = "DBCC sqltext("+spid+")";

		// Set an empty Message handler
		SybMessageHandler curMsgHandler = null;
		if (conn instanceof SybConnection)
		{
			curMsgHandler = ((SybConnection)conn).getSybMessageHandler();
			((SybConnection)conn).setSybMessageHandler(null);
		}
		// Set a TDS Message Handler
		if (conn instanceof TdsConnection)
			((TdsConnection)conn).setSybMessageHandler(null);

		try
		{
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(sql);

			for (SQLWarning sqlw = stmt.getWarnings(); sqlw != null; sqlw = sqlw.getNextWarning())
			{
				// IGNORE: DBCC execution completed. If DBCC printed error messages, contact a user with System Administrator (SA) role.
				if (sqlw.getMessage().startsWith("DBCC execution completed. If DBCC"))
					continue;

				if (sb == null)
					sb = new StringBuilder(htmlBegin);

				String text = sqlw.getMessage();
				//text = StringUtil.makeApproxLineBreak(text, 100, 5, "\n");

				sb = sb.append(text).append(htmlNewLine);
			}

			stmt.close();
		}
		catch (SQLException e)
		{
			_logger.warn("Problems when executing sql: "+sql, e);
		}
		finally
		{
			// restore old message handler
			if (curMsgHandler != null)
			{
				((SybConnection)conn).setSybMessageHandler(curMsgHandler);
			}
			// Restore old message handler
			if (conn instanceof TdsConnection)
				((TdsConnection)conn).restoreSybMessageHandler();
		}

		if (sb == null)
			return null;
		while(sb.charAt(sb.length()-1) == '\n')
			sb.deleteCharAt(sb.length()-1);
		return sb.append(htmlEnd).toString();
	}

	/** 
	 * Get Stored Procedure call stack for the currect executing SPID.
	 * <p>
	 * Execute 'select ...someCols... from mmonProcessProcedures where SPID = spid)' on the passed spid 
	 * @param conn The database connection to use
	 * @param spid The spid to get SQL Text for
	 * @param addHtmlTags add html tags around the sql output
	 * @return the sql text
	 */
	public static String monProcCallStack(Connection conn, int spid, boolean addHtmlTags)
	{
		String htmlBegin   = "";
		String htmlEnd     = "";
		String htmlNewLine = "\n";
		if ( addHtmlTags )
		{
			htmlBegin   = "<html><pre>";
			htmlEnd     = "</pre></html>";
			htmlNewLine = "<br>\n";
		}

		long srvVersion = 0;
		String LineNumber      = "LineNumber='', ";
		String StatementNumber = "StatementNumber='', ";
		if (MonTablesDictionaryManager.hasInstance())
		{
			 srvVersion = MonTablesDictionaryManager.getInstance().getDbmsMonTableVersion();
			
//			if (srvVersion >= 12530) LineNumber      = "LineNumber      = convert(varchar(10),LineNumber), ";
//			if (srvVersion >= 15025) StatementNumber = "StatementNumber = convert(varchar(10),StatementNumber), ";
//			if (srvVersion >= 1253000) LineNumber      = "LineNumber      = convert(varchar(10),LineNumber), ";
//			if (srvVersion >= 1502050) StatementNumber = "StatementNumber = convert(varchar(10),StatementNumber), ";
			if (srvVersion >= Ver.ver(12,5,3))   LineNumber      = "LineNumber      = convert(varchar(10),LineNumber), ";
			if (srvVersion >= Ver.ver(15,0,2,2)) StatementNumber = "StatementNumber = convert(varchar(10),StatementNumber), ";
		}
		
		StringBuilder sb = null;
		String sql = 
			"select ContextID, DBName, OwnerName, ObjectName, " + LineNumber + StatementNumber + "ObjectType \n" +
			"from master..monProcessProcedures \n" +
			"where SPID = " + spid + " \n" +
			"order by ContextID desc";
		try
		{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			boolean rows = false;
			while (rs.next())
			{
				if (sb == null)
					sb = new StringBuilder(htmlBegin);

				rows = true;
				String vContextID       = rs.getString(1);
				String vDBName          = rs.getString(2);
				String vOwnerName       = rs.getString(3);
				String vObjectName      = rs.getString(4);
				String vLineNumber      = rs.getString(5).trim();
				String vStatementNumber = rs.getString(6).trim();
				String vObjectType      = rs.getString(7);

				if (vOwnerName == null)
					vOwnerName = "";
				
				int len = 60 - (vDBName.length() + vOwnerName.length() + vObjectName.length());
				sb.append(vContextID).append(": ");
				sb.append(vDBName).append(".");
				sb.append(vOwnerName).append(".");
				sb.append(vObjectName).append(StringUtil.replicate(" ", len));
				
				sb.append("LineNumber=").append(vLineNumber).append(", ");
				sb.append("StmtNumber=").append(vStatementNumber).append(", ");
				sb.append("ObjectType=").append(vObjectType).append(htmlNewLine);
			}

			if ( ! rows )
			{
				return null;
//				if (sb == null)
//					sb = new StringBuilder(htmlBegin);
//				sb.append("NO Procedure is currentley executing.");
			}

			rs.close();
			stmt.close();
		}
		catch (SQLException e)
		{
			_logger.warn("Problems when executing sql: "+sql, e);
		}

		if (sb == null)
			return null;
//		while(sb.charAt(sb.length()-1) == '\n')
//			sb.deleteCharAt(sb.length()-1);
		return sb.append(htmlEnd).toString();
	}

	/** 
	 * Execute 'DBCC stacktrace(spid)' on the passed spid 
	 * @param conn The database connection to use
	 * @param spid The spid to db dbcc stacktrace on
	 * @param addHtmlTags add html tags around the output
	 * @param waitEventID If positive number (>=0), first check if we are still in the same WaitEventID in the monProcess table.
	 *                    If the WaitEventID has changed, do not do the stacktrace...
	 * @return the sting dbcc stacktrace generated
	 */
	public static String dbccStacktrace(Connection conn, int spid, boolean addHtmlTags, int waitEventID)
	{
		String htmlBegin   = "";
		String htmlEnd     = "";
		String htmlNewLine = "";
		if ( addHtmlTags )
		{
			htmlBegin   = "<html><pre>";
			htmlEnd     = "</pre></html>";
//			htmlNewLine = "<br>";
		}

		// do: dbcc traceon
		dbccTraceOn(conn, 3604);

		StringBuilder sb = null;
		StringBuilder sqlSb = new StringBuilder();
		String sql = null;
		if (waitEventID < 0)
		{
			sqlSb.append("DBCC stacktrace(").append(spid).append(")");
		}
		else
		{
			String monWaitClassInfoWhere = "";
			String monWaitEventInfoWhere = "";
			if (MonTablesDictionaryManager.hasInstance())
			{
//				if (MonTablesDictionary.getInstance().getMdaVersion() >= 15700)
//				if (MonTablesDictionary.getInstance().getMdaVersion() >= 1570000)
				if (MonTablesDictionaryManager.getInstance().getDbmsMonTableVersion() >= Ver.ver(15,7))
				{
					monWaitClassInfoWhere = " and CI.Language = 'en_US'";
					monWaitEventInfoWhere = "      and WI.Language = 'en_US' \n";
				}
			}

			sqlSb.append("declare @prevWaitEventId int, @nowWaitEventId int, @spid int \n");
			sqlSb.append("select @spid = ").append(spid).append(" \n");
			sqlSb.append("select @prevWaitEventId = ").append(waitEventID).append(" \n");
			sqlSb.append("     \n");
			sqlSb.append("select @nowWaitEventId = WaitEventID \n");
			sqlSb.append("from master..monProcess \n");
			sqlSb.append("where SPID = @spid \n");
			sqlSb.append("     \n");
			sqlSb.append("if (@@rowcount = 0) \n");
			sqlSb.append("    return \n"); // if the SPID is not around anymore, get out of there
			sqlSb.append("     \n");
			sqlSb.append("if (@nowWaitEventId is NULL) \n");
			sqlSb.append("    select @nowWaitEventId = 0\n");
			sqlSb.append("     \n");
			sqlSb.append("if (@prevWaitEventId = @nowWaitEventId) \n");
			sqlSb.append("begin \n");
			sqlSb.append("    DBCC stacktrace(@spid) \n");
			sqlSb.append("end \n");
			sqlSb.append("else \n");
			sqlSb.append("begin \n");
			sqlSb.append("    declare @prevWaitDescription varchar(60), @prevClassDescription varchar(60) \n");
			sqlSb.append("    declare @nowWaitDescription  varchar(60), @nowClassDescription  varchar(60) \n");
			sqlSb.append("    \n");
			sqlSb.append("    select @prevWaitDescription  = WI.Description,  \n");
			sqlSb.append("           @prevClassDescription = (select CI.Description from master..monWaitClassInfo CI where WI.WaitClassID = CI.WaitClassID").append(monWaitClassInfoWhere).append(") \n");
			sqlSb.append("    from master..monWaitEventInfo WI \n");
			sqlSb.append("    where WI.WaitEventID = @prevWaitEventId \n");
			sqlSb.append(monWaitEventInfoWhere);
			sqlSb.append("    \n");
			sqlSb.append("    select @nowWaitDescription  = WI.Description,  \n");
			sqlSb.append("           @nowClassDescription = (select CI.Description from master..monWaitClassInfo CI where WI.WaitClassID = CI.WaitClassID").append(monWaitClassInfoWhere).append(") \n");
			sqlSb.append("    from master..monWaitEventInfo WI \n");
			sqlSb.append("    where WI.WaitEventID = @nowWaitEventId \n");
			sqlSb.append(monWaitEventInfoWhere);
			sqlSb.append("    \n");
			sqlSb.append("    print 'The WaitEventID was changed from %1! to %2!, so there is no reason to do DBCC stacktrace anymore.', @prevWaitEventId, @nowWaitEventId \n");
			sqlSb.append("    print '-------------------------------------------------------------------------------------------------' \n");
			sqlSb.append("    print 'From WaitEventID=%1!, class=''%2!'', description=''%3!''.', @prevWaitEventId, @prevClassDescription, @prevWaitDescription \n");
			sqlSb.append("    print 'To   WaitEventID=%1!, class=''%2!'', description=''%3!''.', @nowWaitEventId,  @nowClassDescription,  @nowWaitDescription \n");
			sqlSb.append("end \n");
		}
		sql = sqlSb.toString();

		// Set an empty Message handler
		SybMessageHandler curMsgHandler = null;
		if (conn instanceof SybConnection)
		{
			curMsgHandler = ((SybConnection)conn).getSybMessageHandler();
			((SybConnection)conn).setSybMessageHandler(null);
		}
		// Set a TDS Message Handler
		if (conn instanceof TdsConnection)
			((TdsConnection)conn).setSybMessageHandler(null);

		try
		{
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(sql);

			for (SQLWarning sqlw = stmt.getWarnings(); sqlw != null; sqlw = sqlw.getNextWarning())
			{
				// IGNORE: DBCC execution completed. If DBCC printed error messages, contact a user with System Administrator (SA) role.
				if (sqlw.getMessage().startsWith("DBCC execution completed. If DBCC"))
					continue;

				if (sb == null)
					sb = new StringBuilder(htmlBegin);

				String msg = sqlw.getMessage();
				if (msg.endsWith("\n"))
					sb = sb.append(msg).append(htmlNewLine);
				else
					sb = sb.append(msg).append("\n").append(htmlNewLine);
			}

			stmt.close();
		}
		catch (SQLException e)
		{
			_logger.warn("Problems when executing sql: "+sql, e);
		}
		finally
		{
			// restore old message handler
			if (curMsgHandler != null)
			{
				((SybConnection)conn).setSybMessageHandler(curMsgHandler);
			}
			// Restore old message handler
			if (conn instanceof TdsConnection)
				((TdsConnection)conn).restoreSybMessageHandler();
		}

		if (sb == null)
			return null;
		while(sb.charAt(sb.length()-1) == '\n')
			sb.deleteCharAt(sb.length()-1);
		return sb.append(htmlEnd).toString();
	}

	

	/** 
	 * Get XML ShowPlan a specific plan
	 * <p>
	 * Execute 'select show_cached_plan_in_xml(1966128182, 0, 0)' on the passed planid 
	 * @param conn The database connection to use
	 * @param objectName The "planid" to get Text for, but it's in the form '*ss##########_##########ss*' or '*sq##########_##########sq*
	 * @param addHtmlTags add html tags around the sql output
	 * @return the XML text (null if it can't be found, or if the name doesn't start with *ss or *sq)
	 */
	public static String cachedPlanInXml(Connection conn, String objectName, boolean addHtmlTags)
	{
		if (objectName.startsWith("*ss") || objectName.startsWith("*sq") ) // *sq in ASE 15.7 esd#2, DynamicSQL can/will end up in statement cache
		{
			int    sep   = objectName.indexOf('_');
			String ssqlidStr = objectName.substring(3, sep);
			//String haskey    = objectName.substring(sep+1, objectName.length()-3);
			
			int ssqlid = StringUtil.parseInt(ssqlidStr, -1);
			if (ssqlid != -1)
				return cachedPlanInXml(conn, ssqlid, addHtmlTags);
		}
		return null;
	}
	/** 
	 * Get XML ShowPlan a specific plan
	 * <p>
	 * Execute 'select show_cached_plan_in_xml(1966128182, 0, 0)' on the passed planid 
	 * @param conn The database connection to use
	 * @param ssqlid The planid to get Text for
	 * @param addHtmlTags add html tags around the sql output
	 * @return the XML text (null if it can't be found)
	 */
	public static String cachedPlanInXml(Connection conn, int ssqlid, boolean addHtmlTags)
	{
		String htmlBegin   = "";
		String htmlEnd     = "";
//		String htmlNewLine = "\n";
		if ( addHtmlTags )
		{
			htmlBegin   = "<html><pre>";
			htmlEnd     = "</pre></html>";
//			htmlNewLine = "<br>\n";
		}

		String query_plan = null;
		
		String sql = "select show_cached_plan_in_xml("+ssqlid+", 0, 0)";
		try
		{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while(rs.next())
			{
				query_plan = rs.getString(1);
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException e)
		{
			_logger.warn("Problems when executing sql: "+sql, e);
		}

		if (query_plan == null)
			return null;

		if (! addHtmlTags)
			return query_plan;

		StringBuilder sb = new StringBuilder();
		sb.append(htmlBegin).append(query_plan).append(htmlEnd);
		return sb.toString();
	}

	/**
	 * Get a lock summary for a SPID
	 * 
	 * @param conn           The connection to use 
	 * @param spid           The SPID we want to get locks for
	 * @return List&lt;LockRecord&gt; never null
	 */
	public static List<LockRecord> getLockSummaryForSpid(DbxConnection conn, int spid)
	{
		String sql = "select dbname=db_name(dbid), table_name=object_name(id, dbid), lock_type=type, lock_count=count(*) "
				+ " from master.dbo.syslocks "
				+ " where spid = " + spid
				+ " group by dbid, id, type ";

		List<LockRecord> lockList = new ArrayList<>();

		// possibly add a SQLException -> SQLWarning using a message handler for:
		// Error=8233, Msg='ALTER TABLE operation is in progress on the object 'xxxxx' in database 'yyyyyy'. Retry your query later.'.
		// The above is done in the SybMessageHandler at CmSybMessageHandler, which is installed by the CounterModel
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
			{
				String dbname    = rs.getString(1);
				String tableName = rs.getString(2);
				int    lockType  = rs.getInt   (3);
				int    lockCount = rs.getInt   (4);

				lockList.add( new LockRecord(dbname, tableName, lockType, lockCount) );
			}
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems when executing sql: " + sql + ". SQLException Error=" + ex.getErrorCode() + ", Msg='" + StringUtil.stripNewLine(ex.getMessage()) + "'.", ex);
		}
		
		return lockList;
	}

	/**
	 * Get a lock summary for a SPID
	 * 
	 * @param conn           The connection to use 
	 * @param lockList       The lockList produced by: getLockSummaryForSpid(DbxConnection conn, int spid)
	 * @param asHtml         Produce a HTML table (if false a ASCII table will be produced)
	 * @param htmlBeginEnd   (if asHtml=true) should we wrap the HTML with begin/end tags
	 * @return
	 */
	public static String getLockSummaryForSpid(List<LockRecord> lockList, boolean asHtml, boolean htmlBeginEnd)
	{
		if (lockList.isEmpty())
			return null;
	
		if (asHtml)
		{
			String htmlTable = DbxConnectionStateInfoAse.getLockListTableAsHtmlTable(lockList);
			if (htmlBeginEnd)
				return "<html>" + htmlTable + "</html>";
			else
				return htmlTable;
		}
		else
			return DbxConnectionStateInfoAse.getLockListTableAsAsciiTable(lockList);
	}

	/**
	 * Get a lock summary for a SPID
	 * 
	 * @param conn           The connection to use 
	 * @param spid           The SPID we want to get locks for
	 * @param asHtml         Produce a HTML table (if false a ASCII table will be produced)
	 * @param htmlBeginEnd   (if asHtml=true) should we wrap the HTML with begin/end tags
	 * @return
	 */
	public static String getLockSummaryForSpid(DbxConnection conn, int spid, boolean asHtml, boolean htmlBeginEnd)
	{
		List<LockRecord> lockList = getLockSummaryForSpid(conn, spid);

		return getLockSummaryForSpid(lockList, asHtml, htmlBeginEnd);
	}

	/**
	 * Check if a stored procedure exists or not
	 * <p>
	 * If the stored procedure did not exist, or it was to old, try to recreate it.
	 * 
	 * @param conn                 Connectio to the ASE
	 * @param needsVersion         Needs atleast this ASE version to install (for example 12540 = 12.5.4 ESD#0), 0 = any version
	 * @param dbname               If what database should the procedure exist
	 * @param procName             Name of the procedure
	 * @param procDateThreshold    If the proc is "older" than this date, recreate it
	 * @param scriptLocation       Location of the script
	 * @param scriptName           Name of the script.
	 * @param needsRoleToRecreate  What role is needed to create the proc
	 * 
	 * @return
	 */
	public static boolean checkCreateStoredProc(Connection conn, long needsVersion, String dbname, String procName, Date procDateThreshold, Class<?> scriptLocation, String scriptName, String needsRoleToRecreate)
	throws Exception
	{
		if (dbname            == null) throw new IllegalArgumentException("checkCreateStoredProc(): 'dbname' cant be null");
		if (procName          == null) throw new IllegalArgumentException("checkCreateStoredProc(): 'procName' cant be null");
		if (procDateThreshold == null) throw new IllegalArgumentException("checkCreateStoredProc(): 'procDateThreshold' cant be null");
//		if (scriptLocation    == null) throw new IllegalArgumentException("checkCreateStoredProc(): 'scriptLocation' cant be null");
		if (scriptName        == null) throw new IllegalArgumentException("checkCreateStoredProc(): 'scriptName' cant be null");

		// If procName does not exists
		// or is of an earlier version than procDateThreshold
		// GO AND CREATE IT.
		Date crDate = AseConnectionUtils.getObjectCreationDate(conn, dbname, procName);
		if (crDate == null || ( crDate != null && crDate.getTime() < procDateThreshold.getTime()) )
		{
			if (crDate == null)
				_logger.info("Checking for stored procedure '"+procName+"' in '"+dbname+"', which was NOT found.");
			else
				_logger.info("Checking for stored procedure '"+procName+"' in '"+dbname+"', which was to old, crdate was '"+crDate+"', re-creation threshold date is '"+procDateThreshold+"'.");

			boolean hasProc = false;

			// CHECK ASE VERSION
			if (needsVersion > 0)
			{
				long srvVersion = AseConnectionUtils.getAseVersionNumber(conn);
				
				if (srvVersion < needsVersion)
				{
					String msg = "The procedure '"+procName+"' in '"+dbname+"', needs at least version '"+needsVersion+"', while we are connected to ASE Version '"+srvVersion+"'.";
					_logger.warn(msg);
					throw new Exception(msg);
				}
			}

			// CHECK IF WE HAVE "some ROLE", so we can create the proc
			boolean hasRole = true;
			if (needsRoleToRecreate != null && !needsRoleToRecreate.equals(""))
			{
				hasRole = AseConnectionUtils.hasRole(conn, needsRoleToRecreate);
			}

			if ( ! hasRole )
			{
				String msg = "Can't (re)create procedure '"+procName+"' in '"+dbname+"', for doing that the connected user needs to have '"+needsRoleToRecreate+"'.";
				_logger.warn(msg);
				throw new Exception(msg);
			}
			else
			{
				AseSqlScript script = null;
				try
				{
					script = new AseSqlScript(conn, 30); // 30 seconds timeout

					_logger.info("Creating procedure '"+procName+"' in '"+dbname+"'.");
					script.setMsgPrefix(scriptName+": ");
					if (scriptLocation == null)
						script.execute(scriptName);
					else
						script.execute(scriptLocation, scriptName);
					hasProc = true;
				}
				catch (SQLException e) 
				{
					String msg = "Problem loading the script '"+scriptName+"'.";
					_logger.error(msg, e);
					throw new Exception(msg, e);
				}
				finally
				{
					if (script != null)
					script.close();
				}
			}

			if ( ! hasProc )
			{
				String location;
				if (scriptLocation == null)
					location = "'" + scriptName + "'";
				else
//					location = "'$"+DbxTune.getInstance().getAppHomeEnvName()+"/classes' under the class '"+scriptLocation.getClass().getName()+"' you will find the script '"+scriptName+"'";
					location = "'$DBXTUNE_HOME/classes' under the class '"+scriptLocation.getClass().getName()+"' you will find the script '"+scriptName+"'";

				String msg = "Missing stored proc '"+procName+"' in database '"+dbname+"' please create it. (connect with a user that has '"+needsRoleToRecreate+"' or load the proc from "+location+").";
				_logger.warn(msg);
				throw new Exception(msg);
				//return false;
			}
		}
		else
		{
			_logger.info("No Need to re-create procedure '"+procName+"' in '"+dbname+"', creation date was '"+crDate+"', re-creation threshold date is '"+procDateThreshold+"'.");
		}
		return true;
	}
	
	/**
	 * Get object owner
	 * 
	 * @param conn       Connection to the database
	 * @param dbname     Name of the database (not the dbid)
	 * @param objectName Name or ID of the procedure/view/trigger...
	 * 
	 * @return owner of the object name
	 */
	public static String getObjectOwner(Connection conn, String dbname, String objectName)
	{
		if (StringUtil.isNullOrBlank(dbname))     throw new RuntimeException("getObjectOwner(): dbname='"     + dbname     + "', which is blank or null. This is mandatory.");
		if (StringUtil.isNullOrBlank(objectName)) throw new RuntimeException("getObjectOwner(): objectName='" + objectName + "', which is blank or null. This is mandatory.");

		dbname     = dbname    .trim();
		objectName = objectName.trim();

//		String sql = 
//			"select owner = u.name \n" +
//			"from ["+dbname+"]..sysobjects o, ["+dbname+"]..sysusers u \n" +
//			"where o.name = '"+objectName+"' \n" +
//			"  and o.uid  = u.uid";
		String sql = "";

		// Create SQL
		// - first part if objectName is a number
		// - second part if objectName is a normal string
		try
		{
			int objId = Integer.parseInt(objectName);

			sql = "select owner = u.name \n" +
				      "from ["+dbname+"]..sysobjects o, ["+dbname+"]..sysusers u \n" +
				      "where o.id  = "+objId+" \n" +
				      "  and o.uid = u.uid";
		}
		catch(NumberFormatException nfe)
		{
			sql = "select owner = u.name \n" +
			      "from ["+dbname+"]..sysobjects o, ["+dbname+"]..sysusers u \n" +
			      "where o.name = '"+objectName+"' \n" +
			      "  and o.uid  = u.uid";
		}
		
		String owner = "dbo";
		try
		{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				owner = rs.getString(1);
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems when executing sql: "+sql, ex);
		}

		if (StringUtil.isNullOrBlank(owner))
			owner = "dbo";

		return owner;
	}

	/**
	 * Get (procedure) text about an object
	 * 
	 * @param conn       Connection to the database
	 * @param dbname     Name of the database (if null, current db will be used)
	 * @param objectName Name of the procedure/view/trigger...
	 * @param owner      Name of the owner, if null is passed, it will be set to 'dbo'
	 * @param srvVersion Version of the ASE, if 0, the version will be fetched from ASE
	 * @return Text of the procedure/view/trigger...
	 */
//	public static String getObjectText(Connection conn, String dbname, String objectName, String owner, int planId, long srvVersion)
	public static String getObjectText(DbxConnection conn, String dbname, String objectName, String owner, int planId, long srvVersion)
	{
		if (StringUtil.isNullOrBlank(owner) || (owner != null && (owner.equals("-1") || owner.equals("0") || owner.equals("1"))) )
			owner = "dbo";

//		if (srvVersion <= 0)
//			srvVersion = getAseVersionNumber(conn);
		if (srvVersion <= 0)
			srvVersion = conn.getDbmsVersionNumber();

		if (planId < 0)
			planId = 0;

		String returnText = null;
		
		// Statement Cache object
		boolean isStatementCache = false;
		String  ssqlid = null;
		if (objectName.startsWith("*ss") || objectName.startsWith("*sq") ) // *sq in ASE 15.7 esd#2, DynamicSQL can/will end up in statement cache
		{
			isStatementCache = true;
			int sep = objectName.indexOf('_');
			ssqlid  = objectName.substring(3, sep);
			//haskey = objectName.substring(sep+1, objectName.length()-3);
		}

		// Statement Cache objects
		if (isStatementCache)
		{
			if (srvVersion >= Ver.ver(15,7))
			{
				//-----------------------------------------------------------
				// From Documentation on: show_cached_plan_in_xml(statement_id, plan_id, level_of_detail)
				//-----------------------------------------------------------
				// statement_id
				//			     is the object ID of the lightweight procedure (A procedure that can be created and invoked 
				//			     internally by Adaptive Server). This is the SSQLID from monCachedStatement.
				// 
				// plan_id
				//			     is the unique identifier for the plan. This is the PlanID from monCachedProcedures. 
				//			     A value of zero for plan_id displays the showplan output for all cached plans for the indicated SSQLID.
				// 
				// level_of_detail
				//			     is a value from 0 - 6 indicating the amount of detail show_cached_plan_in_xml returns (see Table 2-6). 
				//			     level_of_detail determines which sections of showplan are returned by show_cached_plan_in_xml. 
				//			     The default value is 0.
				// 
				//			     The output of show_cached_plan_in_xml includes the plan_id and these sections:
				// 
				//			         parameter - contains the parameter values used to compile the query and the parameter values 
				//			                     that caused the slowest performance. The compile parameters are indicated with the 
				//			                     <compileParameters> and </compileParameters> tags. The slowest parameter values are 
				//			                     indicated with the <execParameters> and </execParameters> tags. 
				//			                     For each parameter, show_cached_plan_in_xml displays the:
				//			                        Number
				//			                        Datatype
				//			                        Value:    values that are larger than 500 bytes and values for insert-value statements 
				//			                                  do not appear. The total memory used to store the values for all parameters 
				//			                                  is 2KB for each of the two parameter sets.
				// 
				//			         opTree    - contains the query plan and the optimizer estimates. 
				//			                     The opTree section is delineated by the <opTree> and </opTree> tags.
				// 
				//			         execTree  - contains the query plan with the lava operator details. 
				//			                     The execTree section is identified by the tags <execTree> and </execTree>.
				//
				// level_of_detail parameter opTree execTree
				// --------------- --------- ------ --------
				// 0 (the default)       YES    YES         
				// 1                     YES                
				// 2                            YES         
				// 3                                     YES
				// 4                            YES      YES
				// 5                     YES             YES
				// 6                     YES    YES      YES
				//-----------------------------------------------------------
				int levelOfDetail = Configuration.getCombinedConfiguration().getIntProperty("AseConnectionUtil.getObjectText.show_cached_plan_in_xml.level_of_detail", 0);
				
				String sql = "select show_cached_plan_in_xml("+ssqlid+", "+planId+", "+levelOfDetail+")";

				boolean foundXmlPlan = false;
				try
				{
					Statement stmnt = conn.createStatement();
					stmnt.setQueryTimeout(10);
					
					ResultSet rs = stmnt.executeQuery(sql);

					StringBuilder sb = new StringBuilder();
					sb.append(sql).append("\n");
					sb.append("------------------------------------------------------------------\n");
					while (rs.next())
					{
						sb.append(rs.getString(1));
						foundXmlPlan = true;
					}
					rs.close();
					stmnt.close();

					if (foundXmlPlan)
						returnText = sb.toString().trim();
					else
						returnText = "";   // FIXME: should this be null or ""... also make sure we return the best thing later on (xmlplan or prsqlcache)
				}
				catch(SQLException e)
				{
					_logger.warn("Problems getting text from Statement Cache about '"+objectName+"'. Msg="+e.getErrorCode()+", Text='" + e.getMessage() + "'. Caught: "+e); 
					returnText = null;
				}

//				AseSqlScript ss = new AseSqlScript(conn, 10);
//				try	
//				{
//					returnText = ss.executeSqlStr(sql, true);
//				} 
//				catch (SQLException e) 
//				{
//					returnText = null;
//					_logger.warn("Problems getting text from Statement Cache about '"+objectName+"'. Caught: "+e); 
//				} 
//				finally 
//				{
//					ss.close();
//				}
			}
			else
			{
				String sql;
//				if (srvVersion >= 15020)
//				if (srvVersion >= 1502000)
				if (srvVersion >= Ver.ver(15,0,2))
				{
					sql =
						"set switch on 3604 with no_info \n" +
						"dbcc prsqlcache("+ssqlid+", 1) "; // 1 = also prints showplan"
				}
				else
				{
					sql=
						"dbcc traceon(3604) \n" +
						"dbcc prsqlcache("+ssqlid+", 1) "; // 1 = also prints showplan"
				}
				
				AseSqlScript ss = new AseSqlScript(conn, 10);
				try	
				{ 
					returnText = ss.executeSqlStr(sql, true); 
				} 
				catch (SQLException e) 
				{ 
					returnText = null;
					_logger.warn("Problems getting text from Statement Cache about '"+objectName+"'. Caught: "+e); 
				} 
				finally 
				{
					ss.close();
				}
			}
		}
		else
		{
			String dbnameStr = dbname;
			if (dbnameStr == null)
				dbnameStr = "";
			else
				dbnameStr = "[" + dbname + "].dbo.";

			// Check if the "owner" is a number
			boolean ownerIsNumber = false;
			try { Integer.parseInt(owner); ownerIsNumber = true; } catch(NumberFormatException ignore) {}

			//--------------------------------------------
			// GET OBJECT TEXT
			String sql;
			sql = " select c.text, c.status, c.id \n"
				+ " from "+dbnameStr+"sysobjects o, "+dbnameStr+"syscomments c, "+dbnameStr+"sysusers u \n"
				+ " where o.name = '"+objectName+"' \n" +
				(ownerIsNumber 
				? "   and u.uid  = "  + owner + "  \n" // if owner is a *number* we will use this 
				: "   and u.name = '" + owner + "' \n" // if owner is a *string* we will use this
				) 
				+ "   and o.id   = c.id \n"
				+ "   and o.uid  = u.uid \n"
				+ " order by c.number, c.colid2, c.colid ";

			try
			{
				StringBuilder sb = new StringBuilder();

				Statement statement = conn.createStatement();
				ResultSet rs = statement.executeQuery(sql);
				while(rs.next())
				{
					String textPart = rs.getString(1);
					int    status   = rs.getInt(2);
					int    id       = rs.getInt(3);

					// if status is ASE: SYSCOM_TEXT_HIDDEN
					if ((status & 1) == 1)
					{
						sb.append("ASE StoredProcedure Source text for compiled object '"+dbname+"."+owner+"."+objectName+"' (id = "+id+") is hidden.");
						break;
					}

					sb.append(textPart);
				}
				rs.close();
				statement.close();

				if (sb.length() > 0)
					returnText = sb.toString();
				else
				{
					// Maybe the MetaData is wrong... so if it's "master" and a proc starting with "sp_", lets try to look it up in sybsystemprocs 
					if (objectName.startsWith("sp_") && !"sybsystemprocs".equals(dbname)) // !sybsystemprocs will stop us from possible infinate loop
						returnText = getObjectText(conn, "sybsystemprocs", objectName, owner, planId, srvVersion);
				}
			}
			catch (SQLException e)
			{
				returnText = null;
				_logger.warn("Problems getting text for object '"+objectName+"', with owner '"+owner+"', in db '"+dbname+"'. Caught: "+e); 
			}
		}
		if (_logger.isDebugEnabled())
			_logger.debug("Fetched text for object '"+objectName+"', with owner '"+owner+"', in db '"+dbname+"'. textLength=" + (returnText==null ? "-null-" : returnText.length()) );

		return returnText;
	}
	
	/**
	 * find a column name in a ResultSetMetaData
	 * @param rsmd
	 * @param colLabel
	 * @return -1 if not found, otherwise the column id, starting at 1
	 */
	public static int findColumn(ResultSetMetaData rsmd, String colLabel)
	{
		if (rsmd     == null) throw new IllegalArgumentException("findColumn(ResultSetMetaData rsmd, String colLabel): rsmd can't be null");
		if (colLabel == null) throw new IllegalArgumentException("findColumn(ResultSetMetaData rsmd, String colLabel): colLabel can't be null");

		int col_pos = -1;
		try
		{
			for (int i=1; i<=rsmd.getColumnCount(); i++)
			{
				if (colLabel.equals(rsmd.getColumnLabel(i)))
				{
					col_pos = i;
					break;
				}
			}
		}
		catch(SQLException e)
		{
			_logger.error("Problems accessing ResultSetMetaData, caught: "+e, e);
		}
		return col_pos;
	}



//	/**
//	 * Get various state about a ASE Connection
//	 * 
//	 * @param conn
//	 * @param isAse true for ASE, false if it's a MS-SQL Server (no we should probably not share this method... but at start it looked like a good idea)
//	 * @return
//	 */
//	public static ConnectionStateInfo getAseConnectionStateInfo(Connection conn, boolean isAse)
//	{
//		String sql = "select dbname=db_name(), spid=@@spid, username = user_name(), susername =suser_name(), trancount=@@trancount";
//		if (isAse)
//			sql += ", tranchained=@@tranchained, transtate=@@transtate";
//		else
//			sql += ", tranchained=sign((@@options & 2))"; // MSSQL retired @@transtate in SqlServer2008, SqlServer never implemented @@transtate 
//
//		ConnectionStateInfo csi = new ConnectionStateInfo();
//
//		// Do the work
//		try
//		{
//			Statement stmnt = conn.createStatement();
//			ResultSet rs = stmnt.executeQuery(sql);
//
//			while(rs.next())
//			{
//				csi._dbname      = rs.getString(1);
//				csi._spid        = rs.getInt   (2);
//				csi._username    = rs.getString(3);
//				csi._susername   = rs.getString(4);
//				csi._tranCount   = rs.getInt   (5);
//				csi._tranChained = rs.getInt   (6);
//				csi._tranState   = isAse ? rs.getInt(7) : ConnectionStateInfo.TSQL_TRANSTATE_NOT_AVAILABLE;
//			}
//
////			sql = "select count(*) from master.dbo.syslocks where spid = @@spid";
////			rs = stmnt.executeQuery(sql);
////			while(rs.next())
////			{
////				csi._lockCount = rs.getInt(1);
////			}
//			if (isAse)
//			{
//				sql = "select dbname=db_name(dbid), table_name=object_name(id, dbid), lock_type=type, lock_count=count(*) "
//					+ " from master.dbo.syslocks "
//					+ " where spid = @@spid	"
//					+ " group by dbid, id, type ";
//
//				csi._lockCount = 0;
//				csi._lockList.clear();
//
//				rs = stmnt.executeQuery(sql);
//				while(rs.next())
//				{
//					String dbname    = rs.getString(1);
//					String tableName = rs.getString(2);
//					int    lockType  = rs.getInt   (3);
//					int    lockCount = rs.getInt   (4);
//
//					csi._lockCount += lockCount;
//					csi._lockList.add( new LockRecord(dbname, tableName, lockType, lockCount) );
//				}
//
//				rs.close();
//				stmnt.close();
//			}
//			else // MS SQL do not have syslocks anymore, so use: sys.dm_tran_locks, and simulate some kind of equal question...
//			{
//				sql = "select dbname=db_name(resource_database_id),	table_name=object_name(resource_associated_entity_id, resource_database_id), lock_type=request_mode, lock_count=request_reference_count "
//				    + " from sys.dm_tran_locks "
//				    + " where request_session_id = @@spid "
//				    + "  and resource_type = 'OBJECT' ";
//
//				csi._lockCount = 0;
//				csi._lockList.clear();
//
//				rs = stmnt.executeQuery(sql);
//				while(rs.next())
//				{
//					String dbname    = rs.getString(1);
//					String tableName = rs.getString(2);
//					String lockType  = rs.getString(3);
//					int    lockCount = rs.getInt   (4);
//
//					csi._lockCount += lockCount;
//					csi._lockList.add( new LockRecord(dbname, tableName, lockType, lockCount) );
//				}
//
//				rs.close();
//				stmnt.close();
//			}
//
//		}
//		catch (SQLException sqle)
//		{
//			_logger.error("Error in getAseConnectionStateInfo() problems executing sql='"+sql+"'.", sqle);
//		}
//
////		select count(*) from master.dbo.syslogshold where spid = @@spid
//
//		_logger.debug("getAseConnectionStateInfo(): db_name()='"+csi._dbname+"', @@spid='"+csi._spid+"', user_name()='"+csi._username+"', suser_name()='"+csi._susername+"', @@transtate="+csi._tranState+", '"+csi.getTranStateStr()+"', @@trancount="+csi._tranCount+".");
//		return csi;
//	}
//	/**
//	 * Class that reflects a call to getAseConnectionStateInfo()
//	 * @author gorans
//	 */
//	public static class ConnectionStateInfo
//	{
//		/** _current* below is only maintained if we are connected to ASE */
//		public String _dbname      = "";
//		public int    _spid        = -1;
//		public String _username    = "";
//		public String _susername   = "";
//		public int    _tranState   = -1;
//		public int    _tranCount   = -1;
//		public int    _tranChained = -1;
//		public int    _lockCount   = -1;
//		public List<LockRecord> _lockList = new ArrayList<LockRecord>();
//
//		// Transaction SQL states for DONE flavors
//		//
//		// 0 Transaction in progress: an explicit or implicit transaction is in effect;
//		//   the previous statement executed successfully.
//		// 1 Transaction succeeded: the transaction completed and committed its changes.
//		// 2 Statement aborted: the previous statement was aborted; no effect on the transaction.
//		// 3 Transaction aborted: the transaction aborted and rolled back any changes.
//		public static final int TSQL_TRAN_IN_PROGRESS = 0;
//		public static final int TSQL_TRAN_SUCCEED = 1;
//		public static final int TSQL_STMT_ABORT = 2;
//		public static final int TSQL_TRAN_ABORT = 3;
//		public static final int TSQL_TRANSTATE_NOT_AVAILABLE = 4; // Possible a MSSQL system
//
//		public static final String[] TSQL_TRANSTATE_NAMES =
//		{
////			"TSQL_TRAN_IN_PROGRESS",
////			"TSQL_TRAN_SUCCEED",
////			"TSQL_STMT_ABORT",
////			"TSQL_TRAN_ABORT"
//			"TRAN_IN_PROGRESS",
//			"TRAN_SUCCEED",
//			"STMT_ABORT",
//			"TRAN_ABORT",
//			"NOT_AVAILABLE"
//		};
//
//		public static final String[] TSQL_TRANSTATE_DESCRIPTIONS =
//		{
////			"TRAN_IN_PROGRESS = Transaction in progress. A transaction is in effect; \nThe previous statement executed successfully.",
//			"TRAN_IN_PROGRESS = Transaction in progress. \nThe previous statement executed successfully.",
//			"TRAN_SUCCEED = Last Transaction succeeded. \nThe transaction completed and committed its changes.",
//			"STMT_ABORT = Last Statement aborted. \nThe previous statement was aborted; \nNo effect on the transaction.",
//			"TRAN_ABORT = Last Transaction aborted. \nThe transaction aborted and rolled back any changes.",
//			"NOT_AVAILABLE = Not available in this system."
//		};
//		
//
//		public boolean isTranStateUsed()
//		{
//			return _tranState != TSQL_TRANSTATE_NOT_AVAILABLE;
//		}
//
//		public boolean isNonNormalTranState()
//		{
//			return ! isNormalTranState();
//		}
//		public boolean isNormalTranState()
//		{
//			if (_tranState == TSQL_TRAN_SUCCEED)            return true;
//			if (_tranState == TSQL_TRANSTATE_NOT_AVAILABLE) return true;
//			return false;
//		}
//
//		public String getTranStateStr()
//		{
//			return tsqlTranStateToString(_tranState);
//		}
//
//		public String getTranStateDescription()
//		{
//			return tsqlTranStateToDescription(_tranState);
//		}
//
//		/**
//		 * Get the String name of the transactionState
//		 *
//		 * @param state
//		 * @return
//		 */
//		protected String tsqlTranStateToString(int state)
//		{
//			switch (state)
//			{
//				case TSQL_TRAN_IN_PROGRESS:
//					return TSQL_TRANSTATE_NAMES[state];
//
//				case TSQL_TRAN_SUCCEED:
//					return TSQL_TRANSTATE_NAMES[state];
//
//				case TSQL_STMT_ABORT:
//					return TSQL_TRANSTATE_NAMES[state];
//
//				case TSQL_TRAN_ABORT:
//					return TSQL_TRANSTATE_NAMES[state];
//
//				case TSQL_TRANSTATE_NOT_AVAILABLE:
//					return TSQL_TRANSTATE_NAMES[state];
//
//				default:
//					return "TSQL_UNKNOWN_STATE("+state+")";
//			}
//		}
//		protected String tsqlTranStateToDescription(int state)
//		{
//			switch (state)
//			{
//				case TSQL_TRAN_IN_PROGRESS:
//					return TSQL_TRANSTATE_DESCRIPTIONS[state];
//
//				case TSQL_TRAN_SUCCEED:
//					return TSQL_TRANSTATE_DESCRIPTIONS[state];
//
//				case TSQL_STMT_ABORT:
//					return TSQL_TRANSTATE_DESCRIPTIONS[state];
//
//				case TSQL_TRAN_ABORT:
//					return TSQL_TRANSTATE_DESCRIPTIONS[state];
//
//				case TSQL_TRANSTATE_NOT_AVAILABLE:
//					return TSQL_TRANSTATE_DESCRIPTIONS[state];
//
//				default:
//					return "TSQL_UNKNOWN_STATE("+state+")";
//			}
//		}
//		
//		/** 
//		 * @return "" if no locks, otherwise a HTML TABLE, with the headers: DB, Table, Type, Count
//		 */
//		public String getLockListTableAsHtmlTable()
//		{
//			if (_lockList.size() == 0)
//				return "";
//
//			StringBuilder sb = new StringBuilder("<TABLE BORDER=1>");
//			sb.append("<TR> <TH>DB</TH> <TH>Table</TH> <TH>Type</TH> <TH>Count</TH> </TR>");
//			for (LockRecord lr : _lockList)
//			{
//				sb.append("<TR>");
//				sb.append("<TD>").append(lr._dbname   ).append("</TD>");
//				sb.append("<TD>").append(lr._tableName).append("</TD>");
//				sb.append("<TD>").append(lr._lockType ).append("</TD>");
//				sb.append("<TD>").append(lr._lockCount).append("</TD>");
//				sb.append("</TR>");
//			}
//			sb.append("</TABLE>");
//			return sb.toString();
//		}
//	}
//	public static class LockRecord
//	{
//		public String _dbname    = "";
//		public String _tableName = "";
//		public String _lockType  = "";
//		public int    _lockCount = 0;
//
////		public LockRecord(String dbname, String tableName, String lockType, int lockCount)
////		{
////			_dbname    = dbname;
////			_tableName = tableName;
////			_lockType  = lockType;
////			_lockCount = lockCount;
////		}
//
//		public LockRecord(String dbname, String tableName, int lockType, int lockCount)
//		{
//			_dbname    = dbname;
//			_tableName = tableName;
//			_lockType  = getAseLockType(lockType);
//			_lockCount = lockCount;
//		}
//		public LockRecord(String dbname, String tableName, String lockType, int lockCount)
//		{
//			_dbname    = dbname;
//			_tableName = tableName;
//			_lockType  = lockType;
//			_lockCount = lockCount;
//		}
//	}

//	public static String getAseLockType(int type)
//	{
//		// below values grabbed from ASE 15.7 SP102: 
//		//            select 'case '+convert(char(5),number)+': return "'+name+'";' from master..spt_values where type in ('L') and number != -1
//		switch (type)
//		{
//		case 1   : return "Ex_table";
//		case 2   : return "Sh_table";
//		case 3   : return "Ex_intent";
//		case 4   : return "Sh_intent";
//		case 5   : return "Ex_page";
//		case 6   : return "Sh_page";
//		case 7   : return "Update_page";
//		case 8   : return "Ex_row";
//		case 9   : return "Sh_row";
//		case 10  : return "Update_row";
//		case 11  : return "Sh_nextkey";
//		case 257 : return "Ex_table-blk";
//		case 258 : return "Sh_table-blk";
//		case 259 : return "Ex_intent-blk";
//		case 260 : return "Sh_intent-blk";
//		case 261 : return "Ex_page-blk";
//		case 262 : return "Sh_page-blk";
//		case 263 : return "Update_page-blk";
//		case 264 : return "Ex_row-blk";
//		case 265 : return "Sh_row-blk";
//		case 266 : return "Update_row-blk";
//		case 267 : return "Sh_nextkey-blk";
//		case 513 : return "Ex_table-demand";
//		case 514 : return "Sh_table-demand";
//		case 515 : return "Ex_intent-demand";
//		case 516 : return "Sh_intent-demand";
//		case 517 : return "Ex_page-demand";
//		case 518 : return "Sh_page-demand";
//		case 519 : return "Update_page-demand";
//		case 520 : return "Ex_row-demand";
//		case 521 : return "Sh_row-demand";
//		case 522 : return "Update_row-demand";
//		case 523 : return "Sh_nextkey-demand";
//		case 769 : return "Ex_table-demand-blk";
//		case 770 : return "Sh_table-demand-blk";
//		case 771 : return "Ex_intent-demand-blk";
//		case 772 : return "Sh_intent-demand-blk";
//		case 773 : return "Ex_page-demand-blk";
//		case 774 : return "Sh_page-demand-blk";
//		case 775 : return "Update_page-demand-blk";
//		case 776 : return "Ex_row-demand-blk";
//		case 777 : return "Sh_row-demand-blk";
//		case 778 : return "Update_row-demand-blk";
//		case 779 : return "Sh_nextkey-demand-blk";
//		case 1025: return "Ex_table-request";
//		case 1026: return "Sh_table-request";
//		case 1027: return "Ex_intent-request";
//		case 1028: return "Sh_intent-request";
//		case 1029: return "Ex_page-request";
//		case 1030: return "Sh_page-request";
//		case 1031: return "Update_page-request";
//		case 1032: return "Ex_row-request";
//		case 1033: return "Sh_row-request";
//		case 1034: return "Update_row-request";
//		case 1035: return "Sh_nextkey-request";
//		case 1537: return "Ex_table-demand-request";
//		case 1538: return "Sh_table-demand-request";
//		case 1539: return "Ex_intent-demand-request";
//		case 1540: return "Sh_intent-demand-request";
//		case 1541: return "Ex_page-demand-request";
//		case 1542: return "Sh_page-demand-request";
//		case 1543: return "Update_page-demand-request";
//		case 1544: return "Ex_row-demand-request";
//		case 1545: return "Sh_row-demand-request";
//		case 1546: return "Update_row-demand-request";
//		case 1547: return "Sh_nextkey-demand-request";
//		}
//		return "unknown("+type+")";
//	}

	
	/**
	 * Check if we can do a simple select on the provided table.
	 * <p>
	 * The SQL issued would be "select * from "+tableName+" where 1=2"
	 * @param tableName the table name passed into the query. In here you can add the databasename and owner if you like. Example "master.dbo.syslogshold"
	 * @return true if no problems, false if connection is not OK or SQLException where thrown in the select statement.
	 */
	public static boolean canDoSelectOnTable(Connection conn, String tableName)
	{
		String sql = "select * from "+tableName+" where 1=2";

		// Check if the connection is OK
		if ( ! isConnectionOk(conn) )
		{
			return false;
		}

		// Check if we can do select on syslogshold
		try 
		{
			// do dummy select, which will return 0 rows
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {}
			rs.close();
			stmt.close();
			
			return true;
		}
		catch (SQLException ex)
		{
			_logger.warn("Authoritization problems when checking simple select on table '"+tableName+"'. SQL issued '"+sql+"' SQLException Error="+ex.getErrorCode()+", Msg='"+StringUtil.stripNewLine(ex.getMessage())+"'.");
			return false;
		}
	}

//	/**
//	 * Check if the ASE is is grace period
//	 * @param conn 
//	 * @return null if OK, otherwise a String with the warning message
//	 */
//	public static String getAseGracePeriodWarning(DbxConnection conn)
//	{
//		if (conn == null)
//			return null;
//
//		// master.dbo.monLicense does only exists if ASE is above 15.0
//		if (conn.getDbmsVersionNumber() < Ver.ver(15,0))
//			return null;
//
//
//		String sql = "select Status, GraceExpiry, Name, Edition, Type, srvName=@@servername from master.dbo.monLicense";
//
//		try 
//		{
//			String warningStr = null;
//			
//			// do dummy select, which will return 0 rows
//			Statement stmt = conn.createStatement();
//			ResultSet rs = stmt.executeQuery(sql);
//			while (rs.next()) 
//			{
//				String    licStatus      = rs.getString   (1);
//				Timestamp licGraceExpiry = rs.getTimestamp(2);
//				String    licName        = rs.getString   (3);
//				String    licEdition     = rs.getString   (4);
//				String    licType        = rs.getString   (5);
//				String    aseSrvName     = rs.getString   (6);
//
//				if ("graced".equalsIgnoreCase(licStatus))
//				{
//					// add newline if we have several rows
//					warningStr = warningStr == null ? "" : warningStr + "\n";
//
//					warningStr += "Server '"+aseSrvName+"' is in grace period and will stop working at '"+licGraceExpiry+"'. (licName='"+licName+"', licEdition='"+licEdition+"', licType='"+licType+"').";
//				}
//			}
//			rs.close();
//			stmt.close();
//			
//			return warningStr;
//		}
//		catch (SQLException ex)
//		{
//			_logger.warn("Problems when checking grace period. SQL issued '"+sql+"' SQLException Error="+ex.getErrorCode()+", Msg='"+StringUtil.stripNewLine(ex.getMessage())+"'.");
//			return "Problems when checking grace period. ("+StringUtil.stripNewLine(ex.getMessage()+").");
//		}
//	}

	/**
	 * Check if the RS Replication Server is is grace period
	 * @param conn 
	 * @return null if OK, otherwise a String with the warning message
	 */
	public static String getRsGracePeriodWarning(DbxConnection conn)
	{
		if (conn == null)
			return null;

		// Only in 15.7 and above
		if (conn.getDbmsVersionNumber() < Ver.ver(15,7))
			return null;


		String sql = "sysadmin lmconfig";

		try 
		{
			String warningStr = null;
			
			// do dummy select, which will return 0 rows
			Statement stmt = conn.createStatement();
			ResultSet rs;
			//int rsNum = 0;
			boolean hasRs = stmt.execute(sql);
			while (hasRs)
			{
				if(hasRs) 
				{
					rs = stmt.getResultSet();
					int colCount = rs.getMetaData().getColumnCount();
					boolean hasExpiryDate = false;
					for (int c=0; c<colCount; c++)
					{
						if ("Expiry Date".equalsIgnoreCase(rs.getMetaData().getColumnLabel(c+1)))
							hasExpiryDate = true;
					}
					while(rs.next()) 
					{
						if (hasExpiryDate)
						{
							String licStatus      = rs.getString("Status");
							String licGraceExpiry = rs.getString("Expiry Date");
							String licName        = rs.getString("License Name");
							String srvName        = rs.getString("Server Name");

							if ("graced".equalsIgnoreCase(licStatus))
							{
								// add newline if we have several rows
								warningStr = warningStr == null ? "" : warningStr + "\n";
								
								if (srvName != null && "null".equalsIgnoreCase(srvName))
									srvName = conn.getDbmsServerName();

								warningStr += "Server '"+srvName+"' is in grace period and will stop working at '"+licGraceExpiry+"'. (licName='"+licName+"').";
							}
						}
					}

					rs.close();
				}
				else
				{
					if(stmt.getUpdateCount() == -1)
						break;
				}

				//rsNum++;
				hasRs = stmt.getMoreResults();
			}
			stmt.close();
			
			return warningStr;
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems when checking grace period. SQL issued '"+sql+"' SQLException Error="+ex.getErrorCode()+", Msg='"+StringUtil.stripNewLine(ex.getMessage())+"'.");
			return "Problems when checking grace period. ("+StringUtil.stripNewLine(ex.getMessage()+").");
		}
	}
	
	
	/**
	 * Get a list of all "user" databases, which is is a <b>usable/accessable</b> state
	 * @param conn
	 * @return
	 * @throws SQLException
	 */
	public static List<String> getDatabaseList(Connection conn)
	throws SQLException
	{
		String sql = "" 
			+ "SELECT name \n"
			+ "FROM master.dbo.sysdatabases \n"
			+ "WHERE name not like 'tempdb%' \n"
			+ "  AND name not in ('master', 'sybsecurity', 'sybsystemdb', 'sybsystemprocs','model', 'tempdb') \n"

			+ "  AND (status  & 32)      != 32      -- ignore: Database created with for load option, or crashed while loading database, instructs recovery not to proceed \n"
			+ "  AND (status  & 64)      != 64      -- ignore: Recovery started for all databases to be recovered (Database suspect, Not recovered, Cannot be opened or used, Can be dropped only with dbcc dbrepair) \n"
			+ "  AND (status  & 1024)    != 1024    -- ignore: read only; can be set by user \n"
			+ "  AND (status  & 2048)    != 2048    -- ignore: dbo use only; can be set by user \n"
			+ "  AND (status  & 4096)    != 4096    -- ignore: single user; can be set by user \n"

			+ "  AND (status2 & 16)      != 16      -- ignore: Database is offline. \n"
			+ "  AND (status2 & 32)      != 32      -- ignore: Database is offline until recovery completes. \n"
			+ "  AND (status2 & 256)     != 256     -- ignore: Table structure written to disk. If this bit appears after recovery completes, server may be under-configured for open databases. Use sp_configure to increase this parameter. \n"
			+ "  AND (status2 & 512)     != 512     -- ignore: Database is in the process of being upgraded. \n"

			+ "  AND (status3 & 2)       != 2       -- ignore: Database is a proxy database created by high availability. \n"
			+ "  AND (status3 & 4)       != 4       -- ignore: Database has a proxy database created by high availability \n"
			+ "  AND (status3 & 8)       != 8       -- ignore: Disallow access to the database, since database is being shut down \n"
			+ "  AND (status3 & 256)     != 256     -- ignore: User-created tempdb. \n"
			+ "  AND (status3 & 4096)    != 4096    -- ignore: Database has been shut down successfully. \n"
			+ "  AND (status3 & 8192)    != 8192    -- ignore: A drop database is in progress. \n"
			+ "  AND (status3 & 4194304) != 4194304 -- ignore: archive databases \n"

			+ "ORDER BY dbid \n"
			+ "";

		ArrayList<String> list = new ArrayList<String>();

		try ( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql); )
		{
			while(rs.next())
			{
				String dbname = rs.getString(1);
				list.add(dbname);
			}
		}
		
		return list;
	}
	// Status Control Bits in the sysdatabases Table
	// +----------+---------+--------------------------------------------------------------------------------
	// | Decimal  |Hex      | Status
	// +----------+---------+--------------------------------------------------------------------------------
	// | 1        | 0x01    | Upgrade started on this database
	// | 2        | 0x02    | Upgrade has been successful
	// | 4        | 0x04    | select into/bulkcopy; can be set by user
	// | 8        | 0x08    | trunc log on chkpt; can be set by user
	// | 16       | 0x10    | no chkpt on recovery; can be set by user
	// | 32       | 0x20    | Database created with for load option, or crashed while loading database, instructs recovery not to proceed
	// | 64       | 0x04    | Recovery started for all databases to be recovered
	// | 256      | 0x100   | •   Database suspect
	// |          |         | •   Not recovered
	// |          |         | •   Cannot be opened or used
	// |          |         | •   Can be dropped only with dbcc dbrepair
	// | 512      | 0x200   | ddl in tran; can be set by user
	// | 1024     | 0x400   | read only; can be set by user
	// | 2048     | 0x800   | dbo use only; can be set by user
	// | 4096     | 0x1000  | single user; can be set by user
	// | 8192     | 0x2000  | allow nulls by default; can be set by user
	// +----------+---------+--------------------------------------------------------------------------------
	// 
	// This table lists the bit representations for the status2 column.
	// status2 Control Bits in the sysdatabases Table
	// +----------+------------+--------------------------------------------------------------------------------
	// | Decimal  |Hex         | Status
	// +----------+------------+--------------------------------------------------------------------------------
	// | 1        | 0x0001     | abort tran on log full; can be set by user
	// | 2        | 0x0002     | no free space acctg; can be set by user
	// | 4        | 0x0004     | auto identity; can be set by user
	// | 8        | 0x0008     | identity in nonunique index; can be set by user
	// | 16       | 0x0010     | Database is offline
	// | 32       | 0x0020     | Database is offline until recovery completes
	// | 64       | 0x0040     | The table has an auto identity feature, and a unique constraint on the identity column
	// | 128      | 0x0080     | Database has suspect pages
	// | 256      | 0x0100     | Table structure written to disk. If this bit appears after recovery completes, server may be under-configured for open databases. Use sp_configure to increase this parameter.
	// | 512      | 0x0200     | Database is in the process of being upgraded
	// | 1024     | 0x0400     | Database brought online for standby access
	// | 2048     | 0x0800     | When set by the user, prevents cross-database access via an alias mechanism
	// | -32768   | 0xFFFF8000 | Database has some portion of the log which is not on a log-only device
	// +----------+------------+--------------------------------------------------------------------------------
	// 
	// This table lists the bit representations for the status3 column.
	// status3 Control Bits in the sysdatabases Table
	// +----------+---------+--------------------------------------------------------------------------------
	// | Decimal  |Hex      | Status
	// +----------+---------+--------------------------------------------------------------------------------
	// | 0        | 0x0000  | A normal or standard database, or a database without a proxy update in the create statement.
	// | 1        | 0x0001  | You specified the proxy_update option, and the database is a user-created proxy database.
	// | 2        | 0x0002  | Database is a proxy database created by high availability.
	// | 4        | 0x0004  | Database has a proxy database created by high availability.
	// | 8        | 0x0008  | Disallow access to the database, since database is being shut down.
	// | 16       | 0x0010  | Database is a failed-over database.
	// | 32       | 0x0020  | Database is a mounted database of the type master.
	// | 64       | 0x0040  | Database is a mounted database.
	// | 128      | 0x0080  | Writes to the database are blocked by the quiesce database command.
	// | 256      | 0x0100  | User-created tempdb.
	// | 512      | 0x0200  | Disallow external access to database in the server in failed-over state.
	// | 1024     | 0x0400  | User-provided option to enable or disable asynchronous logging service threads. Enable through sp_dboption enbale async logging service option set to true on a particular database.
	// | 4096     | 0x1000  | Database has been shut down successfully.
	// | 8192     | 0x2000  | A drop database is in progress.
	// +----------+---------+--------------------------------------------------------------------------------
	// 
	// This table lists the bit representations for the status4 column.
	// status4 Control Bits in the sysdatabases Table
	// +----------+------------+--------------------------------------------------------------------------------
	// | Decimal  |Hex         | Status
	// +----------+------------+--------------------------------------------------------------------------------
	// | 512      | 0x0200     | The in-memory database has a template database with it.
	// | 4096     | 0x1000     | Database is an in-memory databases.
	// | 16384    | 0x4000     | 64-bit atomic operations have been enabled on this database.
	// | 16777216 | 0x01000000 | All tables in the database are created as page compressed.
	// | 33554432 | 0x02000000 | All tables in the database are created as row compressed.
	// +----------+------------+--------------------------------------------------------------------------------
	// 
	// The sysdatabases system table supports the full database encryption feature in the status5, which indicates the encryption status of a database. The values are:
	// Hex Description
	// +------------+----------------------------------------------------------------------------------------
	// | Hex        | Description
	// +------------+----------------------------------------------------------------------------------------
	// | 0x00000001 | Indicates whether the database is encrypted or not.
	// | 0x00000002 | The database is being encrypted, and the encryption is still in progress.
	// | 0x00000004 | The database is being decrypted, and the decryption is still in progress.
	// | 0x00000008 | The database is only partially encrypted, either due to an error or because the process was suspended by the user.
	// | 0x00000010 | The database is only partially decrypted, either due to an error or because the process was suspended by the user.
	// +------------+----------------------------------------------------------------------------------------
	//
	
	/**
	 * Get database statuses
	 * 
	 * @param dbname		Name of the database (used only for 'full logging mode'. if "master", the 'full logging for XXX' will be skipped)
	 * @param status1		sysdatabases.status
	 * @param status2		sysdatabases.status2
	 * @param status3		sysdatabases.status3
	 * @param status4		sysdatabases.status4
	 * @param status5		sysdatabases.status5 (added in ASE 16.x)
	 * @param full_logging_mode	    Full logging mode (if "master", the 'full logging for XXX' will be skipped)... This is value is can be fetched with SQL: <code>select object_info1 from master.dbo.sysattributes where class = 38 and attribute = 0 and object_type = 'D' and object = ${dbid} </code>
	 * @return
	 */
	public static List<String> decodeSysDatabasesStatus(String dbname, int status1, int status2, int status3, int status4, int status5, int full_logging_mode)
	{
		if (status1 == 0 && status2 == 0 && status3 == 0 && status4 == 0 && status5 == 0)
			return Collections.emptyList();
			
		List<String> list = new ArrayList<>();
		
//		+------------------------------------------------+--------------+----+
//		|name                                            |number        |type|
//		+------------------------------------------------+--------------+----+
//		|DATABASE STATUS                                 |            -1|D   |
//		|text back linked                                |             1|D   |
//		|allow page signing                              |             2|D   |
//		|select into/bulkcopy/pllsort                    |             4|D   |
//		|trunc log on chkpt                              |             8|D   |
//		|trunc. log on chkpt.                            |             8|D   |
//		|no chkpt on recovery                            |            16|D   |
//		|don't recover                                   |            32|D   |
//		|not recovered                                   |           256|D   |
//		|ddl in tran                                     |           512|D   |
//		|read only                                       |         1,024|D   |
//		|dbo use only                                    |         2,048|D   |
//		|single user                                     |         4,096|D   |
//		|allow nulls by default                          |         8,192|D   |
//		|erase residual data                             |        16,384|D   |
//		|ALL SETTABLE OPTIONS                            |        32,286|D   |
//		+------------------------------------------------+--------------+----+
//		if (status1 != 0)
		if ( (status1 & 32_286) != 0 ) // ALL SETTABLE OPTIONS
		{
			if ((status1 &      1) != 0) list.add("text back linked"); 
			if ((status1 &      2) != 0) list.add("allow page signing");
			if ((status1 &      4) != 0) list.add("select into/bulkcopy/pllsort");
			if ((status1 &      8) != 0) list.add("trunc log on chkpt");
			if ((status1 &     16) != 0) list.add("no chkpt on recovery");
			if ((status1 &     32) != 0) list.add("don't recover");
//			if ((status1 &     64) != 0) list.add("");
//			if ((status1 &    128) != 0) list.add("");
			if ((status1 &    256) != 0) list.add("not recovered");
			if ((status1 &    512) != 0) list.add("ddl in tran");
			if ((status1 &   1024) != 0) list.add("read only");
			if ((status1 &   2048) != 0) list.add("dbo use only");
			if ((status1 &   4096) != 0) list.add("single user");
			if ((status1 &   8192) != 0) list.add("allow nulls by default");
			if ((status1 & 16_384) != 0) list.add("erase residual data");
//			if ((status1 & 32_768) != 0) list.add("");
		}

//		+------------------------------------------------+--------------+----+
//		|name                                            |number        |type|
//		+------------------------------------------------+--------------+----+
//		|abort tran on log full                          |             1|D2  |
//		|no free space acctg                             |             2|D2  |
//		|auto identity                                   |             4|D2  |
//		|identity in nonunique index                     |             8|D2  |
//		|offline                                         |            16|D2  |
//		|unique auto_identity index                      |            64|D2  |
//		|ALL SETTABLE OPTIONS                            |            79|D2  |
//		|has suspect pages/objects                       |           128|D2  |
//		|online for standby access                       |         1,024|D2  |
//		|mixed log and data                              |        32,768|D2  |
//		+------------------------------------------------+--------------+----+
//		if (status2 != 0)
		if ( (status2 & 79) != 0 || (status2 & 32768) != 0) // ALL SETTABLE OPTIONS  or ismixedlog
		{
			if ((status2 &      1) != 0) list.add("abort tran on log full"); 
			if ((status2 &      2) != 0) list.add("no free space acctg");
			if ((status2 &      4) != 0) list.add("auto identity");
			if ((status2 &      8) != 0) list.add("identity in nonunique index");
			if ((status2 &     16) != 0) list.add("offline");
//			if ((status2 &     32) != 0) list.add("");
			if ((status2 &     64) != 0) list.add("unique auto_identity index");
			if ((status2 &    128) != 0) list.add("has suspect pages/objects");
//			if ((status2 &    256) != 0) list.add("");
//			if ((status2 &    512) != 0) list.add("");
			if ((status2 &   1024) != 0) list.add("online for standby access");
//			if ((status2 &   2048) != 0) list.add("");
//			if ((status2 &   4096) != 0) list.add("");
//			if ((status2 &   8192) != 0) list.add("");
//			if ((status2 & 16_384) != 0) list.add("");
			if ((status2 & 32_768) != 0) list.add("mixed log and data");
		}
		
//		+------------------------------------------------+--------------+----+
//		|name                                            |number        |type|
//		+------------------------------------------------+--------------+----+
//		|quiesce database                                |           128|D3  |
//		|TEMPDB STATUS MASK                              |           256|D3  |
//		|user created temp db                            |           256|D3  |
//		|async log service                               |         1,024|D3  |
//		|delayed commit                                  |         2,048|D3  |
//		|archive database                                |     4,194,304|D3  |
//		|compressed data                                 |     8,388,608|D3  |
//		|scratch database                                |    16,777,216|D3  |
//		|ALL SETTABLE OPTIONS                            |    16,780,288|D3  |
//		|compressed log                                  |   268,435,456|D3  |
//		+------------------------------------------------+--------------+----+
//		if (status3 != 0)
		if ( (status3 & 16_780_288) != 0 || (status3 & 256) != 0) // ALL SETTABLE OPTIONS  or "user created temp db"
		{
//			if ((status3 &           1) != 0) list.add(""); 
//			if ((status3 &           2) != 0) list.add("");
//			if ((status3 &           4) != 0) list.add("");
//			if ((status3 &           8) != 0) list.add("");
//			if ((status3 &          16) != 0) list.add("");
//			if ((status3 &          32) != 0) list.add("");
//			if ((status3 &          64) != 0) list.add("");
			if ((status3 &         128) != 0) list.add("quiesce database");
			if ((status3 &         256) != 0) list.add("user created temp db");
//			if ((status3 &         512) != 0) list.add("");
			if ((status3 &        1024) != 0) list.add("async log service");
			if ((status3 &        2048) != 0) list.add("delayed commit");
//			if ((status3 &        4096) != 0) list.add("");
//			if ((status3 &        8192) != 0) list.add("");
//			if ((status3 &      16_384) != 0) list.add("");
//			if ((status3 &      32_768) != 0) list.add("");

			if ((status3 &   4_194_304) != 0) list.add("archive database");
			if ((status3 &   8_388_608) != 0) list.add("compressed data");
			if ((status3 &  16_777_216) != 0) list.add("scratch database");
			if ((status3 & 268_435_456) != 0) list.add("compressed log");
		}
		
//		+------------------------------------------------+--------------+----+
//		|name                                            |number        |type|
//		+------------------------------------------------+--------------+----+
//		|deallocate first text page                      |-2,147,483,648|D4  |
//		|ALL SETTABLE OPTIONS                            |-1,605,861,374|D4  |
//		|allow db suspect on rollback error              |             2|D4  |
//		|minimal dml logging                             |           256|D4  |
//		|template database                               |         1,024|D4  |
//		|in-memory database                              |         4,096|D4  |
//		|user-created                                    |         8,192|D4  |
//		|enhanced performance temp db                    |         8,192|D4  |
//		|enforce dump tran sequence                      |        32,768|D4  |
//		|defer_index_recovery auto                       |        65,536|D4  |
//		|defer_index_recovery manual                     |       131,072|D4  |
//		|defer_index_recovery none                       |       262,144|D4  |
//		|allow wide dol rows                             |       524,288|D4  |
//		|defer_index_recovery parallel                   |     2,097,152|D4  |
//		|deferred table allocation                       |     4,194,304|D4  |
//		|page compressed                                 |    16,777,216|D4  |
//		|row compressed                                  |    33,554,432|D4  |
//		|allow incremental dumps                         |   536,870,912|D4  |
//		+------------------------------------------------+--------------+----+
//		if (status4 != 0)
		if ( (status4 & -1_605_861_374) != 0 || (status4 & 4096) != 0) // ALL SETTABLE OPTIONS  or isinmemdb
		{
//			if ((status4 &           1) != 0) list.add(""); 
			if ((status4 &           2) != 0) list.add("allow db suspect on rollback error");
//			if ((status4 &           4) != 0) list.add("");
//			if ((status4 &           8) != 0) list.add("");
//			if ((status4 &          16) != 0) list.add("");
//			if ((status4 &          32) != 0) list.add("");
//			if ((status4 &          64) != 0) list.add("");
//			if ((status4 &         128) != 0) list.add("");
			if ((status4 &         256) != 0) list.add("minimal dml logging");
//			if ((status4 &         512) != 0) list.add("");
			if ((status4 &        1024) != 0) list.add("template database");
//			if ((status4 &        2048) != 0) list.add("");
			if ((status4 &        4096) != 0) list.add("in-memory database");
			if ((status4 &        8192) != 0) list.add("enhanced performance temp db");
//			if ((status4 &      16_384) != 0) list.add("64-bit atomic operations");
			if ((status4 &      32_768) != 0) list.add("enforce dump tran sequence");
			if ((status4 &      65_536) != 0) list.add("defer_index_recovery auto");
			if ((status4 &     131_072) != 0) list.add("defer_index_recovery manual");
			if ((status4 &     262_144) != 0) list.add("defer_index_recovery none");
			if ((status4 &     524_288) != 0) list.add("allow wide dol rows");
			if ((status4 &   2_097_152) != 0) list.add("defer_index_recovery parallel");
			if ((status4 &   4_194_304) != 0) list.add("deferred table allocation");
			if ((status4 &  16_777_216) != 0) list.add("page compressed");
			if ((status4 &  33_554_432) != 0) list.add("row compressed");
			if ((status4 & 536_870_912) != 0) list.add("allow incremental dumps");
		}
		
//		+------------------------------------------------+--------------+----+
//		|name                                            |number        |type|
//		+------------------------------------------------+--------------+----+
//		|encrypted                                       |             1|D5  |
//		|encryption in progress                          |             2|D5  |
//		|decryption in progress                          |             4|D5  |
//		|encrypted partly                                |             8|D5  |
//		|decrypted partly                                |            16|D5  |
//		|index compression                               |            64|D5  |
//		|in-memory row storage                           |           256|D5  |
//		|snapshot isolation                              |           512|D5  |
//		|data row caching                                |         1,024|D5  |
//		|snapshot isolation using on-disk version storage|         4,096|D5  |
//		|sqlscript                                       |         8,192|D5  |
//		|on-disk version storage                         |        16,384|D5  |
//		|auto imrs partition tuning                      |        65,536|D5  |
//		|ALL SETTABLE OPTIONS                            |        98,304|D5  |
//		|latch free index                                |     2,097,152|D5  |
//		+------------------------------------------------+--------------+----+
//		if (status5 != 0)
		if ( (status5 & 98_304) != 0 ) // ALL SETTABLE OPTIONS
		{
			if ((status5 &         1) != 0) list.add("encrypted"); 
			if ((status5 &         2) != 0) list.add("encryption in progress");
			if ((status5 &         4) != 0) list.add("decryption in progress");
			if ((status5 &         8) != 0) list.add("encrypted partly");
			if ((status5 &        16) != 0) list.add("decrypted partly");
//			if ((status5 &        32) != 0) list.add("");
			if ((status5 &        64) != 0) list.add("index compression");
//			if ((status5 &       128) != 0) list.add("");
			if ((status5 &       256) != 0) list.add("in-memory row storage");
			if ((status5 &       512) != 0) list.add("snapshot isolation");
			if ((status5 &      1024) != 0) list.add("data row caching");
//			if ((status5 &      2048) != 0) list.add("");
			if ((status5 &      4096) != 0) list.add("snapshot isolation using on-disk version storage");
			if ((status5 &      8192) != 0) list.add("sqlscript");
			if ((status5 &    16_384) != 0) list.add("on-disk version storage");
//			if ((status5 &    32_768) != 0) list.add("");

			if ((status5 &    65_536) != 0) list.add("auto imrs partition tuning");
			if ((status5 & 2_097_152) != 0) list.add("latch free index");
		}

		//-------------------------------------------------
		// FULL LOGGING MODE
		//-------------------------------------------------
		/*
		** The full logging options are stored in master..sysattributes as:
		**
		**	- class:	38
		**	- type:		'D'
		**
		**      attribute  char_value    object  object_info1
		**      ---------- ------------- ------- ----------------
		**      0          NULL          <dbid>  <bitmap>
		**      0          all                1  0x0000000f
		**      1          select into        1  0x00000001
		**      3          alter table        1  0x00000004
		**      4          reorg rebuild      1  0x00000008
		**
		** The database master stores only the descriptions, so, the
		** attribute 0 that in other databases stores the database bitmap,
		** in the case of master it's used just to store the description
		** 'all' for the bitmap 0xd. This is ok because the full 
		** logging options cannot be changed in master.
		** 
		** If there is an attribute configured, we will update it, otherwise
		** we will insert a new row.
		**
		*/
		if (full_logging_mode != 0) // SQL: select object_info1 from master.dbo.sysattributes where class = 38 and attribute = 0 and object_type = 'D' and object = <dbid>
		{
			// Full logging mode is NOT applicable for the "master" database
			if ( ! "master".equalsIgnoreCase(dbname))
			{
				if (full_logging_mode == 13) // 13 = (1 + 4 + 8) // select object_info1 from master..sysattributes where class = 38 and object = 1 and attribute = 0
				{
					list.add("full logging for all");
				}
				else
				{
					if ((full_logging_mode &  1) != 0) list.add("full logging for select into");
//					if ((full_logging_mode &  2) != 0) list.add("full logging for XXXXXXX");
					if ((full_logging_mode &  4) != 0) list.add("full logging for alter table");
					if ((full_logging_mode &  8) != 0) list.add("full logging for reorg rebuild");
//					if ((full_logging_mode & 16) != 0) list.add("full logging for XXXXXXX");
				}
			}
		}

		return list;
	}
//	1> select v.*
//	2> from master.dbo.spt_values v
//	3> where 1=1
//	4>   and v.type in("D", "D1", "D2", "D3", "D4", "D5")
//	5> order by v.type, number
//	+------------------------------------------------+--------------+----+
//	|name                                            |number        |type|
//	+------------------------------------------------+--------------+----+
//	|DATABASE STATUS                                 |            -1|D   |
//	|text back linked                                |             1|D   |
//	|allow page signing                              |             2|D   |
//	|select into/bulkcopy/pllsort                    |             4|D   |
//	|trunc log on chkpt                              |             8|D   |
//	|trunc. log on chkpt.                            |             8|D   |
//	|no chkpt on recovery                            |            16|D   |
//	|don't recover                                   |            32|D   |
//	|not recovered                                   |           256|D   |
//	|ddl in tran                                     |           512|D   |
//	|read only                                       |         1,024|D   |
//	|dbo use only                                    |         2,048|D   |
//	|single user                                     |         4,096|D   |
//	|allow nulls by default                          |         8,192|D   |
//	|erase residual data                             |        16,384|D   |
//	|ALL SETTABLE OPTIONS                            |        32,286|D   |
//	+------------------------------------------------+--------------+----+
//	|abort tran on log full                          |             1|D2  |
//	|no free space acctg                             |             2|D2  |
//	|auto identity                                   |             4|D2  |
//	|identity in nonunique index                     |             8|D2  |
//	|offline                                         |            16|D2  |
//	|unique auto_identity index                      |            64|D2  |
//	|ALL SETTABLE OPTIONS                            |            79|D2  |
//	|has suspect pages/objects                       |           128|D2  |
//	|online for standby access                       |         1,024|D2  |
//	|mixed log and data                              |        32,768|D2  |
//	+------------------------------------------------+--------------+----+
//	|quiesce database                                |           128|D3  |
//	|TEMPDB STATUS MASK                              |           256|D3  |
//	|user created temp db                            |           256|D3  |
//	|async log service                               |         1,024|D3  |
//	|delayed commit                                  |         2,048|D3  |
//	|archive database                                |     4,194,304|D3  |
//	|compressed data                                 |     8,388,608|D3  |
//	|scratch database                                |    16,777,216|D3  |
//	|ALL SETTABLE OPTIONS                            |    16,780,288|D3  |
//	|compressed log                                  |   268,435,456|D3  |
//	+------------------------------------------------+--------------+----+
//	|deallocate first text page                      |-2,147,483,648|D4  |
//	|ALL SETTABLE OPTIONS                            |-1,605,861,374|D4  |
//	|allow db suspect on rollback error              |             2|D4  |
//	|minimal dml logging                             |           256|D4  |
//	|template database                               |         1,024|D4  |
//	|in-memory database                              |         4,096|D4  |
//	|user-created                                    |         8,192|D4  |
//	|enhanced performance temp db                    |         8,192|D4  |
//	|enforce dump tran sequence                      |        32,768|D4  |
//	|defer_index_recovery auto                       |        65,536|D4  |
//	|defer_index_recovery manual                     |       131,072|D4  |
//	|defer_index_recovery none                       |       262,144|D4  |
//	|allow wide dol rows                             |       524,288|D4  |
//	|defer_index_recovery parallel                   |     2,097,152|D4  |
//	|deferred table allocation                       |     4,194,304|D4  |
//	|page compressed                                 |    16,777,216|D4  |
//	|row compressed                                  |    33,554,432|D4  |
//	|allow incremental dumps                         |   536,870,912|D4  |
//	+------------------------------------------------+--------------+----+
//	|encrypted                                       |             1|D5  |
//	|encryption in progress                          |             2|D5  |
//	|decryption in progress                          |             4|D5  |
//	|encrypted partly                                |             8|D5  |
//	|decrypted partly                                |            16|D5  |
//	|index compression                               |            64|D5  |
//	|in-memory row storage                           |           256|D5  |
//	|snapshot isolation                              |           512|D5  |
//	|data row caching                                |         1,024|D5  |
//	|snapshot isolation using on-disk version storage|         4,096|D5  |
//	|sqlscript                                       |         8,192|D5  |
//	|on-disk version storage                         |        16,384|D5  |
//	|auto imrs partition tuning                      |        65,536|D5  |
//	|ALL SETTABLE OPTIONS                            |        98,304|D5  |
//	|latch free index                                |     2,097,152|D5  |
//	+------------------------------------------------+--------------+----+
}

