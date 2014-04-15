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
import java.util.ArrayList;
import java.util.Calendar;
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

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;

import com.asetune.MonTablesDictionary;
import com.asetune.Version;
import com.asetune.gui.AseConfigMonitoringDialog;
import com.sybase.jdbc4.jdbc.SybSQLWarning;
import com.sybase.jdbcx.EedInfo;
import com.sybase.jdbcx.SybConnection;
import com.sybase.jdbcx.SybMessageHandler;

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
					String msg = "It looks like the database '"+dbname+"' is in 'load database', 'recovery mode' or 'offline', still in the database '"+dbNameBeforeChange+"', please try again later. AseMsgNumber="+aseError+", AseMsg="+aseMsg;
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
		             "from "+dbname+"..sysobjects " +
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
		String exMsg    = getMessageFromSQLException(sqlex, true);
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
					"In the Connection Dialog, in the field 'URL Option' specify:<br>" +
					"<font size=\"4\">" +
					"  <code>CHARSET=iso_1</code><br>" +
					"</font>" +
					"<i>This will hopefully get you going, but characters might not converted correctly</i><br>" +
					"<br>" +
					"Below is all Exceptions that happened during the login attempt.<br>" +
					"<HR NOSHADE>";
		}

		String htmlStr = 
			"<html>" +
			  msg + "<br>" +
			  extraInfo + 
			  "<br>" + 
//			  exMsg.replace("\n", "<br>") + 
			  exMsg + 
			"</html>"; 

		SwingUtils.showErrorMessage(owner, title, htmlStr, sqlex);
		
		return exMsgRet;
	}

	public static String getMessageFromSQLException(SQLException sqlex, boolean htmlTags) 
	{
		StringBuffer sb = new StringBuffer("");
		if (htmlTags)
			sb.append("<UL>");

		boolean first = true;
		while (sqlex != null)
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

			sqlex = sqlex.getNextException();
		}

		if (htmlTags)
			sb.append("</UL>");

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
	 *   <li>12503 for (12.5.0 ESD#3)</li>
	 *   <li>12549 for (12.5.4 ESD#9)</li>
	 *   <li>15031 for (15.0.3 ESD#1)</li>
	 *   <li>15500 for (15.5)</li>
	 *   <li>15502 for (15.5 ESD#2)</li>
	 * </ul>
	 * If the ESD level is above 9 it will still return 9 (otherwise it would wrap...)
	 */
	public static int getAseVersionNumber(Connection conn)
	{
		int aseVersionNum = 0;

//		// @@version_number
//		try
//		{
//			Statement stmt = conn.createStatement();
//			ResultSet rs = stmt.executeQuery(SQL_VERSION_NUM);
//			while ( rs.next() )
//			{
//				aseVersionNum = rs.getInt(1);
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
			String aseVersionStr = "";

			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(SQL_VERSION);
			while ( rs.next() )
			{
				aseVersionStr = rs.getString(1);
			}
			rs.close();
	
			if (aseVersionNum == 0)
			{
				aseVersionNum = Ver.sybVersionStringToNumber(aseVersionStr);
			}
		}
		catch (SQLException ex)
		{
			_logger.error("MonTablesDictionary:getAseVersionNumber(), @@version", ex);
		}
		
		return aseVersionNum;
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
	public static int getRsVersionNumber(Connection conn)
	{
		int srvVersionNum = 0;

		// version
		try
		{
			String aseVersionStr = "";

			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("admin version");
			while ( rs.next() )
			{
				aseVersionStr = rs.getString(1);
			}
			rs.close();
	
			if (srvVersionNum == 0)
			{
				srvVersionNum = Ver.sybVersionStringToNumber(aseVersionStr);
			}
		}
		catch (SQLException ex)
		{
			_logger.error("AseConnectionUtils:getRsVersionNumber(), @@version", ex);
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
	public static boolean checkForMonitorOptions(Connection conn, String user, boolean gui, Component parent, String... needsConfig)
	{
		int    aseVersionNum  = 0;
		String aseVersionStr  = "";
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
					aseVersionNum = rs.getInt(1);
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
				aseVersionStr = rs.getString(1);
			}
			rs.close();

			if ( ! aseVersionStr.startsWith(DbUtils.DB_PROD_NAME_SYBASE_ASE) )
			{
				String msg = "This doesn't look like an ASE server. @@version='"+aseVersionStr+"'.";
				_logger.error(msg);
				if (gui)
				{
					String msgHtml = 
						"<html>" +
						"This doesn't look like an Sybase ASE server.<br>" +
						"<br>" +
						"The Version String is '<code>"+aseVersionStr+"</code>'. <br>" +
						"In my book this ain't a ASE Server, so I can't continue.<br>" +
						"</html>";

					SwingUtils.showErrorMessage(parent, Version.getAppName()+" - connect check", msgHtml, null);
				}
				return false;
			}

			int aseVersionNumFromVerStr = Ver.sybVersionStringToNumber(aseVersionStr);
			aseVersionNum = Math.max(aseVersionNum, aseVersionNumFromVerStr);

			// MINIMUM ASE Version is 12.5.0.3
			if (aseVersionNum < Ver.ver(12,5,0,3,0))
			{
				// FIXME: 
				String msg = "The minimum ASE Version supported by "+Version.getAppName()+" is 12.5.0.3 in earlier releases MDA tables doesn't exists. Connected to @@version='"+aseVersionStr+"'.";
				_logger.error(msg);
				if (gui)
				{
					String msgHtml = 
						"<html>" +
						"The minimum ASE Version supported by "+Version.getAppName()+" is 12.5.0.3<br>" +
						"in earlier ASE releases MDA tables doesn't exists<br>" +
						"<br>" +
						"The Version String is '<code>"+aseVersionStr+"</code>'. <br>" +
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

			_logger.info("Just connected to an ASE Server named '"+atAtServername+"' with Version Number "+aseVersionNum+", and the Version String '"+aseVersionStr+"', using language '"+aseLanguage+"'.");

			
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
//						if (aseVersionNum >= 15000)
//							scriptName = "$SYBASE/$SYBASE_ASE/scripts/installmaster";
//						if (aseVersionNum >= 1500000)
//							scriptName = "$SYBASE/$SYBASE_ASE/scripts/installmaster";
						if (aseVersionNum >= Ver.ver(15,0))
							scriptName = "$SYBASE/$SYBASE_ASE/scripts/installmaster";
	
						String msg = "Monitoring tables must be installed ( please apply '"+scriptName+"' )";
						_logger.error(msg);
						if (gui)
						{
							String msgHtml = 
								"<html>" +
								"ASE Monitoring tables hasn't been installed. <br>" +
								"<br>" +
								"ASE Version is '"+Ver.versionIntToStr(aseVersionNum)+"'. <br>" +
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
								AseConfigMonitoringDialog.showDialog(parent, conn, aseVersionNum);
		
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
	public static List<String> getActiveRoles(Connection conn)
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
		String htmlNewLine = "";
		if ( addHtmlTags )
		{
			htmlBegin   = "<html>"+htmlStartStr+"<pre>";
			htmlEnd     = "</pre></html>";
//			htmlNewLine = "<br>";
		}
		StringBuilder sb = null;
		String sql = "exec sp_showplan "+spid+", null, null, null";

		// Set an empty Message handler
		SybMessageHandler curMsgHandler = null;
		if (conn instanceof SybConnection)
		{
			curMsgHandler = ((SybConnection)conn).getSybMessageHandler();
			((SybConnection)conn).setSybMessageHandler(null);
		}

		try
		{
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(sql);

			for (SQLWarning sqlw = stmt.getWarnings(); sqlw != null; sqlw = sqlw.getNextWarning())
			{
				// Ignore "10233 01ZZZ The specified statement number..." message
				if (sqlw.getErrorCode() == 10233)
					continue;
				// Ignore "010P4: An output parameter was received and ignored." message
				if (sqlw.getSQLState() == "010P4")
					continue;

				if (sb == null)
					sb = new StringBuilder(htmlBegin);

				sb = sb.append(sqlw.getMessage()).append(htmlNewLine);
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

		int aseVersion = 0;
		String LineNumber      = "LineNumber='', ";
		String StatementNumber = "StatementNumber='', ";
		if (MonTablesDictionary.hasInstance())
		{
			 aseVersion = MonTablesDictionary.getInstance().getMdaVersion();
			
//			if (aseVersion >= 12530) LineNumber      = "LineNumber      = convert(varchar(10),LineNumber), ";
//			if (aseVersion >= 15025) StatementNumber = "StatementNumber = convert(varchar(10),StatementNumber), ";
//			if (aseVersion >= 1253000) LineNumber      = "LineNumber      = convert(varchar(10),LineNumber), ";
//			if (aseVersion >= 1502050) StatementNumber = "StatementNumber = convert(varchar(10),StatementNumber), ";
			if (aseVersion >= Ver.ver(12,5,3))   LineNumber      = "LineNumber      = convert(varchar(10),LineNumber), ";
			if (aseVersion >= Ver.ver(15,0,2,2)) StatementNumber = "StatementNumber = convert(varchar(10),StatementNumber), ";
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
			if (MonTablesDictionary.hasInstance())
			{
//				if (MonTablesDictionary.getInstance().getMdaVersion() >= 15700)
//				if (MonTablesDictionary.getInstance().getMdaVersion() >= 1570000)
				if (MonTablesDictionary.getInstance().getMdaVersion() >= Ver.ver(15,7))
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
		}

		if (sb == null)
			return null;
		while(sb.charAt(sb.length()-1) == '\n')
			sb.deleteCharAt(sb.length()-1);
		return sb.append(htmlEnd).toString();
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
	public static boolean checkCreateStoredProc(Connection conn, int needsVersion, String dbname, String procName, Date procDateThreshold, Class<?> scriptLocation, String scriptName, String needsRoleToRecreate)
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
				int aseVersion = AseConnectionUtils.getAseVersionNumber(conn);
				
				if (aseVersion < needsVersion)
				{
					String msg = "The procedure '"+procName+"' in '"+dbname+"', needs at least version '"+needsVersion+"', while we are connected to ASE Version '"+aseVersion+"'.";
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
					location = "'$ASETUNE_HOME/classes' under the class '"+scriptLocation.getClass().getName()+"' you will find the script '"+scriptName+"'";

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
	 * @param dbname     Name of the database
	 * @param objectName Name of the procedure/view/trigger...
	 * 
	 * @return owner of the object name
	 */
	public static String getObjectOwner(Connection conn, String dbname, String objectName)
	{
		if (StringUtil.isNullOrBlank(dbname))     throw new RuntimeException("getObjectOwner(): dbname='"     + dbname     + "', which is blank or null. This is mandatory.");
		if (StringUtil.isNullOrBlank(objectName)) throw new RuntimeException("getObjectOwner(): objectName='" + objectName + "', which is blank or null. This is mandatory.");

		dbname     = dbname    .trim();
		objectName = objectName.trim();

		String sql = 
			"select owner = u.name \n" +
			"from "+dbname+"..sysobjects o, "+dbname+"..sysusers u \n" +
			"where o.name = '"+objectName+"' \n" +
			"  and o.uid  = u.uid";

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
	 * @param aseVersion Version of the ASE, if 0, the version will be fetched from ASE
	 * @return Text of the procedure/view/trigger...
	 */
	public static String getObjectText(Connection conn, String dbname, String objectName, String owner, int aseVersion)
	{
		if (StringUtil.isNullOrBlank(owner))
			owner = "dbo";

		if (aseVersion <= 0)
		{
			aseVersion = getAseVersionNumber(conn);
		}

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
//			if (aseVersion >= 15700)
//			if (aseVersion >= 1570000)
			if (aseVersion >= Ver.ver(15,7))
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

				String sql = "select show_cached_plan_in_xml("+ssqlid+", 0, 0)";

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
					}
					rs.close();
					stmnt.close();

					returnText = sb.toString().trim();
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
//				if (aseVersion >= 15020)
//				if (aseVersion >= 1502000)
				if (aseVersion >= Ver.ver(15,0,2))
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
				dbnameStr = dbname + ".dbo.";
				
			//--------------------------------------------
			// GET OBJECT TEXT
			String sql;
			sql = " select c.text "
				+ " from "+dbnameStr+"sysobjects o, "+dbnameStr+"syscomments c, "+dbnameStr+"sysusers u \n"
				+ " where o.name = '"+objectName+"' \n"
				+ "   and u.name = '"+owner+"' \n" 
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
				_logger.warn("Problems getting text for object '"+objectName+"', with owner '"+owner+"', in db '"+dbname+"'. Caught: "+e); 
			}
		}

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



	/**
	 * Get various state about a ASE Connection
	 */
	public static ConnectionStateInfo getAseConnectionStateInfo(Connection conn, boolean getTranState)
	{
		String sql = "select dbname=db_name(), spid=@@spid, username = user_name(), susername =suser_name(), trancount=@@trancount";
		if (getTranState)
			sql += ", transtate=@@transtate";

		ConnectionStateInfo csi = new ConnectionStateInfo();

		// Do the work
		try
		{
			Statement stmnt = conn.createStatement();
			ResultSet rs = stmnt.executeQuery(sql);

			while(rs.next())
			{
				csi._dbname    = rs.getString(1);
				csi._spid      = rs.getInt   (2);
				csi._username  = rs.getString(3);
				csi._susername = rs.getString(4);
				csi._tranCount = rs.getInt   (5);
				csi._tranState = getTranState ? rs.getInt(6) : ConnectionStateInfo.TSQL_TRANSTATE_NOT_AVAILABLE;
			}
			rs.close();
			stmnt.close();
		}
		catch (SQLException sqle)
		{
			_logger.error("Error in getAseConnectionStateInfo() problems executing sql='"+sql+"'.", sqle);
		}

		_logger.debug("getAseConnectionStateInfo(): db_name()='"+csi._dbname+"', @@spid='"+csi._spid+"', user_name()='"+csi._username+"', suser_name()='"+csi._susername+"', @@transtate="+csi._tranState+", '"+csi.getTranStateStr()+"', @@trancount="+csi._tranCount+".");
		return csi;
	}
	/**
	 * Class that reflects a call to getAseConnectionStateInfo()
	 * @author gorans
	 */
	public static class ConnectionStateInfo
	{
		/** _current* below is only maintained if we are connected to ASE */
		public String _dbname    = "";
		public int    _spid      = -1;
		public String _username  = "";
		public String _susername = "";
		public int    _tranState = -1;
		public int    _tranCount = -1;

		// Transaction SQL states for DONE flavors
		//
		// 0 Transaction in progress: an explicit or implicit transaction is in effect;
		//   the previous statement executed successfully.
		// 1 Transaction succeeded: the transaction completed and committed its changes.
		// 2 Statement aborted: the previous statement was aborted; no effect on the transaction.
		// 3 Transaction aborted: the transaction aborted and rolled back any changes.
		public static final int TSQL_TRAN_IN_PROGRESS = 0;
		public static final int TSQL_TRAN_SUCCEED = 1;
		public static final int TSQL_STMT_ABORT = 2;
		public static final int TSQL_TRAN_ABORT = 3;
		public static final int TSQL_TRANSTATE_NOT_AVAILABLE = 4; // Possible a MSSQL system

		public static final String[] TSQL_TRANSTATE_NAMES =
		{
//			"TSQL_TRAN_IN_PROGRESS",
//			"TSQL_TRAN_SUCCEED",
//			"TSQL_STMT_ABORT",
//			"TSQL_TRAN_ABORT"
			"TRAN_IN_PROGRESS",
			"TRAN_SUCCEED",
			"STMT_ABORT",
			"TRAN_ABORT",
			"NOT_AVAILABLE"
		};

		public static final String[] TSQL_TRANSTATE_DESCRIPTIONS =
		{
//			"TRAN_IN_PROGRESS = Transaction in progress. A transaction is in effect; \nThe previous statement executed successfully.",
			"TRAN_IN_PROGRESS = Transaction in progress. \nThe previous statement executed successfully.",
			"TRAN_SUCCEED = Last Transaction succeeded. \nThe transaction completed and committed its changes.",
			"STMT_ABORT = Last Statement aborted. \nThe previous statement was aborted; \nNo effect on the transaction.",
			"TRAN_ABORT = Last Transaction aborted. \nThe transaction aborted and rolled back any changes.",
			"NOT_AVAILABLE = Not available in this system."
		};
		

		public boolean isTranStateUsed()
		{
			return _tranState != TSQL_TRANSTATE_NOT_AVAILABLE;
		}

		public boolean isNonNormalTranState()
		{
			return ! isNormalTranState();
		}
		public boolean isNormalTranState()
		{
			if (_tranState == TSQL_TRAN_SUCCEED)            return true;
			if (_tranState == TSQL_TRANSTATE_NOT_AVAILABLE) return true;
			return false;
		}

		public String getTranStateStr()
		{
			return tsqlTranStateToString(_tranState);
		}

		public String getTranStateDescription()
		{
			return tsqlTranStateToDescription(_tranState);
		}

		/**
		 * Get the String name of the transactionState
		 *
		 * @param state
		 * @return
		 */
		protected String tsqlTranStateToString(int state)
		{
			switch (state)
			{
				case TSQL_TRAN_IN_PROGRESS:
					return TSQL_TRANSTATE_NAMES[state];

				case TSQL_TRAN_SUCCEED:
					return TSQL_TRANSTATE_NAMES[state];

				case TSQL_STMT_ABORT:
					return TSQL_TRANSTATE_NAMES[state];

				case TSQL_TRAN_ABORT:
					return TSQL_TRANSTATE_NAMES[state];

				case TSQL_TRANSTATE_NOT_AVAILABLE:
					return TSQL_TRANSTATE_NAMES[state];

				default:
					return "TSQL_UNKNOWN_STATE("+state+")";
			}
		}
		protected String tsqlTranStateToDescription(int state)
		{
			switch (state)
			{
				case TSQL_TRAN_IN_PROGRESS:
					return TSQL_TRANSTATE_DESCRIPTIONS[state];

				case TSQL_TRAN_SUCCEED:
					return TSQL_TRANSTATE_DESCRIPTIONS[state];

				case TSQL_STMT_ABORT:
					return TSQL_TRANSTATE_DESCRIPTIONS[state];

				case TSQL_TRAN_ABORT:
					return TSQL_TRANSTATE_DESCRIPTIONS[state];

				case TSQL_TRANSTATE_NOT_AVAILABLE:
					return TSQL_TRANSTATE_DESCRIPTIONS[state];

				default:
					return "TSQL_UNKNOWN_STATE("+state+")";
			}
		}
	}

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
			_logger.warn("Problems doing simple SQL '"+sql+"' SQLException Error="+ex.getErrorCode()+", Msg='"+StringUtil.stripNewLine(ex.getMessage())+"'.");
			return false;
		}
	}
	
}

