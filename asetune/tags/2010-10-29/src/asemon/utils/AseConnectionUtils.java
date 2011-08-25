/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.utils;

import java.awt.Component;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import asemon.gui.AseMonitoringConfigDialog;

import com.sybase.jdbcx.EedInfo;

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
	private static String SQL_VERSION_NUM = "select @@version_number";
//	private static String SQL_SP_VERSION  = "sp_version 'installmontables'";

	/**
	 * What is current working database
	 * @return database name, null on failure
	 */
	public static String getCurrentDbname(Connection conn)
	{
		if ( ! isConnectionOk(conn, null) )
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
		if ( ! isConnectionOk(conn, null) )
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

		if ( ! isConnectionOk(conn, null) )
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

		if ( ! isConnectionOk(conn, null) )
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

	public static String getListeners(Connection conn, boolean addType, boolean stripDomainName, Component guiOwner)
	{
		if ( ! isConnectionOk(conn, guiOwner) )
			return null;

		try
		{
			// LIST WHAT hostnames port(s) the ASE server is listening on.
			String listenersStr = "";
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select * from syslisteners");
			while (rs.next())
			{
				if (addType)
					listenersStr += rs.getString("net_type").trim() + ":";

				String host = rs.getString("address_info").trim();
				if (stripDomainName && host != null && host.indexOf(".") > 0)
				{
					String hostname = host.substring(0, host.indexOf("."));
					String portnum  = host.substring(host.lastIndexOf(" "));
					listenersStr += hostname + portnum;
				}
				else
					listenersStr += host;

				listenersStr += ", ";
			}
			// Take away last ", "
			if (listenersStr.endsWith(", "))
			listenersStr = listenersStr.substring(0, listenersStr.length()-2);
			
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

	public static boolean isConnectionOk(Connection conn, Component guiOwner)
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

	public static String showSqlExceptionMessage(Component owner, String title, String msg, SQLException sqlex) 
	{
		String exMsg = getMessageFromSQLException(sqlex);

		SwingUtils.showErrorMessage(owner, title, 
			msg + "\n\n" + exMsg, sqlex);
		
		return exMsg;
	}

	public static String getMessageFromSQLException(SQLException sqlex) 
	{
		StringBuffer sb = new StringBuffer("");
		boolean first = true;
		while (sqlex != null)
		{
			if (first)
				first = false;
			else
				sb.append( "\n" );

			sb.append( sqlex.getMessage() );
			sqlex = sqlex.getNextException();
		}

		return sb.toString();
	}

	/** 
	 * Convert a int version to a string version 
	 * <p>
	 * <code>15030 will be "15.0.3"</code>
	 * <code>15031 will be "15.0.3 ESD#1"</code>
	 */
	public static String versionIntToStr(int version)
	{
		int major       = version                                     / 1000;
		int minor       =(version -  (major * 1000))                  / 100;
		int maintenance =(version - ((major * 1000) + (minor * 100))) / 10;
		int rollup      = version - ((major * 1000) + (minor * 100) + (maintenance * 10));

		if (rollup == 0)
			return major + "." + minor + "." + maintenance;
		else
			return major + "." + minor + "." + maintenance + " ESD#" + rollup;
	}

	private final static int VERSION_MAJOR        = 1;
	private final static int VERSION_MINOR        = 2;
	private final static int VERSION_MAINTENANCE  = 3;
	private final static int VERSION_ROLLUP       = 4;
	/**
	 * If a version part is overflowing it's max value, try to fix this in here
	 * <p>
	 * For example this could be that version '15.0.3.350' has a rollup of 350
	 * and when we want to convert this into a number of: 1503x the 'x' will be to big
	 * so we need to convert this into a 3
	 * <p>
	 * That is the cind of things we should do in here
	 * 
	 * @param type
	 * @param version
	 * @return
	 */
	private static int fixVersionOverflow(int type, int version)
	{
		if (version < 0)
			return 0;

		if (type == VERSION_ROLLUP)
		{
			if (version <  10) return version;
			if (version >= 10) return 9;
//			if (version < 10)                       return version;
//			if (version >= 10   && version < 100)   return version / 10;
//			if (version >= 100  && version < 1000)  return version / 100;
//			if (version >= 1000 && version < 10000) return version / 1000;
		}
		return version;
	}
	/**
	 * Parses the ASE version string into a number.<br>
	 * The version string will be splitted on the character '/' into different
	 * version parts. The second part will be used as the version string.<br>
	 * 
	 * The version part will then be splitted into different parts by the
	 * delimiter '.'<br>
	 * Four different version parts will be handled:
	 * Major.Minor.Maintenance.Rollup<br>
	 * Major version part can contain several characters, while the other
	 * version parts can only contain 1 character (only the first character i
	 * used).
	 * 
	 * @param versionStr
	 *            the ASE version string fetchedd from the database with select
	 * @@version
	 * @return The version as a number. <br>
	 *         The ase version 12.5 will be returned as 12500 <br>
	 *         The ase version 12.5.2.0 will be returned as 12520 <br>
	 *         The ase version 12.5.2.1 will be returned as 12521 <br>
	 */
// FIXME: this is a ASE-CE version string
//	 Adaptive Server Enterprise/15.0.3/EBF 16748 Cluster Edition/P/x86_64/Enterprise Linux/asepyxis/2837/64-bit/FBO/Mon Jun  1 08:38:39 2009
	public static int aseVersionStringToNumber(String versionStr)
	{
		int aseVersionNumber = 0;

		String[] aseVersionParts = versionStr.split("/");
		if (aseVersionParts.length > 0)
		{
			String aseVersionNumberStr = null;
			String aseEsdStr = null;
			// Scan the string to see if there are any part that looks like a version str (##.#)
			for (int i=0; i<aseVersionParts.length; i++)
			{
//				if ( aseVersionParts[i].matches("^[0-9][0-9][.][0-9][.][0-9]") && aseVersionNumberStr == null )
//				if ( aseVersionParts[i].matches("^[0-9][0-9][.][0-9]([.][0-9])*") && aseVersionNumberStr == null )
				if ( aseVersionParts[i].matches("^[0-9][0-9][.][0-9].*") && aseVersionNumberStr == null )
				{
					aseVersionNumberStr = aseVersionParts[i];
				}

				if ( aseVersionParts[i].indexOf("ESD#") > 0 && aseEsdStr == null)
				{
					aseEsdStr = aseVersionParts[i];
				}
			}

			if (aseVersionNumberStr == null)
			{
				_logger.warn("There ASE version string seems to be faulty, cant find any '##.#' in the version number string '" + versionStr + "'.");
				return aseVersionNumber; // which probably is 0
			}

			String[] aseVersionNumberParts = aseVersionNumberStr.split("\\.");
			if (aseVersionNumberParts.length > 1)
			{
				// Version parts can contain characters...
				// hmm version could be: 12.5.3a
				try
				{
					String versionPart = null;
					// MAJOR version: ( <12>.5.2.1 - MAJOR.minor.maint.rollup )
					if (aseVersionNumberParts.length >= 1)
					{
						versionPart = aseVersionNumberParts[0].trim();
						int major = fixVersionOverflow(VERSION_MAJOR, Integer.parseInt(versionPart));
						aseVersionNumber += 1000 * major;
					}

					// MINOR version: ( 12.<5>.2.1 - major.MINOR.maint.rollup )
					if (aseVersionNumberParts.length >= 2)
					{
						versionPart = aseVersionNumberParts[1].trim().substring(0, 1);
						int minor = fixVersionOverflow(VERSION_MINOR, Integer.parseInt(versionPart));
						if (minor >= 10)
						{
							
						}
						aseVersionNumber += 100 * minor;
					}

					// MAINTENANCE version: ( 12.5.<2>.1 - major.minor.MAINT.rollup )
					if (aseVersionNumberParts.length >= 3)
					{
						versionPart = aseVersionNumberParts[2].trim().substring(0, 1);
						int maint = fixVersionOverflow(VERSION_MAINTENANCE, Integer.parseInt(versionPart));
						aseVersionNumber += 10 * maint;
					}

					// ROLLUP version: ( 12.5.2.<1> - major.minor.maint.ROLLUP )
					if (aseVersionNumberParts.length >= 4)
					{
						versionPart = aseVersionNumberParts[3].trim().substring(0, 1);
						int rollup = fixVersionOverflow(VERSION_ROLLUP, Integer.parseInt(versionPart));
						aseVersionNumber += 1 * rollup;
					}
					else // go and check for ESD string, which is another way of specifying ROLLUP
					{
						if (aseEsdStr != null)
						{
							int start = aseEsdStr.indexOf("ESD#");
							if (start >= 0)
								start += "ESD#".length();
							int end = aseEsdStr.indexOf(" ", start);
							if (end == -1)
								end = aseEsdStr.length();

							if (start != -1)
							{
								try
								{
									versionPart = aseEsdStr.trim().substring(start, end);
									int rollup = fixVersionOverflow(VERSION_ROLLUP, Integer.parseInt(versionPart));
									aseVersionNumber += 1 * rollup;
								}
								catch (RuntimeException e) // NumberFormatException,
								{
									_logger.warn("Problems converting some part(s) of the ESD# in the version string '" + aseVersionNumberStr + "' into a number. ESD# string was '"+versionPart+"'. The version number will be set to " + aseVersionNumber);
								}
							}
						}
					}
				}
				// catch (NumberFormatException e)
				catch (RuntimeException e) // NumberFormatException,
											// IndexOutOfBoundsException
				{
					_logger.warn("Problems converting some part(s) of the version string '" + aseVersionNumberStr + "' into a number. The version number will be set to " + aseVersionNumber);
				}
			}
			else
			{
				_logger.warn("There ASE version string seems to be faulty, cant find any '.' in the version number subsection '" + aseVersionNumberStr + "'.");
			}
		}
		else
		{
			_logger.warn("There ASE version string seems to be faulty, cant find any / in the string '" + versionStr + "'.");
		}

		return aseVersionNumber;
	}

	public static int getAseVersionNumber(Connection conn)
	{
		int aseVersionNum = 0;

		// @@version_number
		try
		{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(SQL_VERSION_NUM);
			while ( rs.next() )
			{
				aseVersionNum = rs.getInt(1);
			}
			rs.close();
		}
		catch (SQLException ex)
		{
			_logger.debug("MonTablesDictionary:getAseVersionNumber(), @@version_number, probably an early ASE version", ex);
		}
	
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
				aseVersionNum = aseVersionStringToNumber(aseVersionStr);
			}
		}
		catch (SQLException ex)
		{
			_logger.error("MonTablesDictionary:getAseVersionNumber(), @@version", ex);
		}
		
		return aseVersionNum;
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
			String msg = AseConnectionUtils.showSqlExceptionMessage(null, "asemon", "Problems when setting 'system view' in ASE Server.", ex); 
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
			String msg = AseConnectionUtils.showSqlExceptionMessage(null, "asemon", "Problems when getting 'system view' in ASE Server.", ex); 
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
	 * @return
	 */
	public static boolean checkForMonitorOptions(Connection conn, String user, boolean gui, Component parent)
	{
		int    aseVersionNum = 0;
		String aseVersionStr = "";
		String atAtServername = "";
		try
		{
			// Get the version of the ASE server
			// select @@version_number (new since 15 I think, this means the local try block)
			try
			{
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("select @@version_number");
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

			// select @@version
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select @@version");
			while ( rs.next() )
			{
				aseVersionStr = rs.getString(1);
			}
			rs.close();

			int aseVersionNumFromVerStr = aseVersionStringToNumber(aseVersionStr);
			aseVersionNum = Math.max(aseVersionNum, aseVersionNumFromVerStr);

			// select @@servername
			stmt = conn.createStatement();
			rs = stmt.executeQuery("select @@servername");
			while ( rs.next() )
			{
				atAtServername = rs.getString(1);
			}
			rs.close();

			_logger.info("Just connected to an ASE Server named '"+atAtServername+"' with Version Number "+aseVersionNum+", and the Version String '"+aseVersionStr+"'.");

			
			// if user name is null or empty, then get current user
			if (user == null || (user != null && user.trim().equals("")) )
			{
				stmt = conn.createStatement();
				rs = stmt.executeQuery("select suser_name()");
				while (rs.next())
				{
					user = rs.getString(1);
				}
				stmt.close();
				rs.close();
			}
			
			// Check if user has mon_role
			_logger.debug("Verify mon_role");
			stmt = conn.createStatement();
			rs = stmt.executeQuery("sp_activeroles");
			boolean has_sa_role = false;
			boolean has_mon_role = false;
			while (rs.next())
			{
				if (rs.getString(1).equals("sa_role"))
					has_sa_role = true;

				if (rs.getString(1).equals("mon_role"))
					has_mon_role = true;
			}
			if (!has_mon_role)
			{
				// Try to grant access to current user
				if (has_sa_role)
				{
					String sql = "sp_role 'grant', 'mon_role', '"+user+"'";
					stmt.execute(sql);
					_logger.info("Executed: "+sql);

					sql = "set role 'mon_role' on";
					stmt.execute(sql);
					_logger.info("Executed: "+sql);

					// re-check if grant of mon_role succeeded
					rs = stmt.executeQuery("sp_activeroles");
					has_mon_role = false;
					while (rs.next())
					{
						if (rs.getString(1).equals("mon_role"))
							has_mon_role = true;
					}
				}

				// If mon_role was still unsuccessfull
				if (!has_mon_role)
				{
					String msg = "You need 'mon_role' to access monitoring tables";
					_logger.error(msg);
					if (gui)
					{
						SwingUtils.showErrorMessage(parent, "Problems when checking 'Monitor Role'", 
								msg, null);
					}
					return false;
				}
			}

			// force master
			stmt.executeUpdate("use master");

			_logger.debug("Verify monTables existance");

			// Check if montables are configured
			rs = stmt.executeQuery("select count(*) from sysobjects where name ='monTables'");
			while (rs.next())
			{
				if (rs.getInt(1) == 0)
				{
					String msg = "Monitoring tables must be installed ( execute '$SYBASE/scripts/installmontables' )";
					_logger.error(msg);
					if (gui)
					{
						SwingUtils.showErrorMessage(parent, "asemon - connect check",	msg, null);
					}
					return false;
				}
			}

			// Check if 'enable monitoring' is activated
			_logger.debug("Verify enable monitoring");

			boolean configEnableMonitoring          = true;
			boolean configWaitEventTiming           = true;
			boolean configPerObjectStatisticsActive = true;
			String errorMesage = "<h1>Sorry the ASE server is not properly configured for monitoring.</h1>";
			       errorMesage += "<UL>";

			//--- Check if 'enable monitoring' is activated
			configEnableMonitoring = getAseConfigRunValue(conn, "enable monitoring") == 1;
			if ( ! configEnableMonitoring )
			{
				_logger.warn("ASE Configuration option 'enable monitoring' is NOT enabled.");
				errorMesage += "<LI> ASE option 'enable monitoring' is NOT enabled.";
			}

//			//--- Check if 'wait event timing' is activated
//			configWaitEventTiming = getAseConfigRunValue(conn, "wait event timing") == 1;
//			if ( ! configWaitEventTiming )
//			{
//				_logger.warn("ASE Configuration option 'wait event timing' is NOT enabled.");
//				errorMesage += "<LI> ASE option 'wait event timing' is NOT enabled.";
//			}
//
//			//--- Check if 'per object statistics active' is activated
//			configPerObjectStatisticsActive = getAseConfigRunValue(conn, "per object statistics active") == 1;
//			if ( ! configPerObjectStatisticsActive )
//			{
//				_logger.warn("ASE Configuration option 'per object statistics active' is NOT enabled.");
//				errorMesage += "<LI> ASE option 'per object statistics active' is NOT enabled.";
//			}

			if ( !configEnableMonitoring || !configWaitEventTiming || !configPerObjectStatisticsActive)
			{
				errorMesage += "</UL>";
				errorMesage += "<b>I will now open the configuration panel for you.</b><BR>";
				errorMesage += "<b>Then try to connect again.</b>";

				if (gui)
				{
					SwingUtils.showErrorMessage(parent, "asemon - connect check",	
							"<html>"+errorMesage+"</html>", null);
	
					AseMonitoringConfigDialog.showDialog(parent, conn, aseVersionNum);
				}

				return false;
			}

			_logger.debug("Connection passed 'Check Monitoring'.");
			return true;
		}
		catch (SQLException ex)
		{
			String msg = AseConnectionUtils.showSqlExceptionMessage(parent, "asemon - connect", "Problems when connecting to a ASE Server.", ex); 
			_logger.error("Problems when connecting to a ASE Server. "+msg);
			return false;
		}
		catch (Exception ex)
		{
			_logger.error("Problems when connecting to a ASE Server. "+ex.toString());
			if (gui)
			{
				SwingUtils.showErrorMessage(parent, "asemon - connect", 
					"Problems when connecting to a ASE Server" +
					"\n\n"+ex.getMessage(), ex);
			}
			return false;
		}
	}

	public static void setBasicAseMonitoring(Connection conn)
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
	public static List getActiveRoles(Connection conn)
	{
		String sql = "select show_role()";
		try
		{
			List roleList = null;
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				if (roleList == null)
					roleList = new LinkedList();
				String val = rs.getString(1);
				String[] sa = val.split(" ");
				for (int i=0; i<sa.length; i++)
				{
					String role = sa[i].trim();
					if ( ! roleList.contains(role) )
						roleList.add(role);
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



	public static void dbccTraceOn(Connection conn, int trace)
	{
		// Works with above ASE 12.5.4 and 15.0.2
		String setSwitch   = "set switch on "+trace+" with no_info";

		// Used as fallback if above 'set switch...' is failing
		String dbccTraceon = "DBCC traceon("+trace+")";

		// TRY with set switch
		try
		{
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(setSwitch);
			stmt.close();
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
			}
			catch (SQLException e2)
			{
				_logger.warn("Problems when executing sql: "+dbccTraceon, e2);
			}
		}

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

				sb = sb.append(sqlw.getMessage()).append(htmlNewLine);
			}

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
			sqlSb.append("declare @prevWaitEventId int, @nowWaitEventId int, @spid int \n");
			sqlSb.append("select @spid = ").append(spid).append(" \n");
			sqlSb.append("select @prevWaitEventId = ").append(waitEventID).append(" \n");
			sqlSb.append("     \n");
			sqlSb.append("select @nowWaitEventId = WaitEventID \n");
			sqlSb.append("from monProcess \n");
			sqlSb.append("where SPID = @spid \n");
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
			sqlSb.append("           @prevClassDescription = (select CI.Description from monWaitClassInfo CI where WI.WaitClassID = CI.WaitClassID) \n");
			sqlSb.append("    from monWaitEventInfo WI \n");
			sqlSb.append("    where WI.WaitEventID = @prevWaitEventId \n");
			sqlSb.append("    \n");
			sqlSb.append("    select @nowWaitDescription  = WI.Description,  \n");
			sqlSb.append("           @nowClassDescription = (select CI.Description from monWaitClassInfo CI where WI.WaitClassID = CI.WaitClassID) \n");
			sqlSb.append("    from monWaitEventInfo WI \n");
			sqlSb.append("    where WI.WaitEventID = @nowWaitEventId \n");
			sqlSb.append("    \n");
			sqlSb.append("    print 'The WaitEventID was changed from %1! to %2!, so there is no reason to do DBCC stacktrace anymore.', @prevWaitEventId, @nowWaitEventId \n");
			sqlSb.append("    print '-------------------------------------------------------------------------------------------------' \n");
			sqlSb.append("    print 'From WaitEventID=%1!, class=''%2!'', description=''%3!''.', @prevWaitEventId, @prevClassDescription, @prevWaitDescription \n");
			sqlSb.append("    print 'To   WaitEventID=%1!, class=''%2!'', description=''%3!''.', @nowWaitEventId,  @nowClassDescription,  @nowWaitDescription \n");
			sqlSb.append("end \n");
		}
		sql = sqlSb.toString();

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

		if (sb == null)
			return null;
		while(sb.charAt(sb.length()-1) == '\n')
			sb.deleteCharAt(sb.length()-1);
		return sb.append(htmlEnd).toString();
	}
}

