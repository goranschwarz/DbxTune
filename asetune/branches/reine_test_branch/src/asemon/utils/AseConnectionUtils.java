/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.utils;

import java.awt.Component;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import asemon.gui.AseMonitoringConfigDialog;

public class AseConnectionUtils
{
	private static Logger _logger = Logger.getLogger(AseConnectionUtils.class);
	private static String SQL_VERSION     = "select @@version";
	private static String SQL_VERSION_NUM = "select @@version_number";
//	private static String SQL_SP_VERSION  = "sp_version 'installmontables'";

	public static String getListeners(Connection conn, Component guiOwner)
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
				listenersStr += rs.getString("net_type").trim() + ":";
				listenersStr += rs.getString("address_info").trim();
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
	public static int aseVersionStringToNumber(String versionStr)
	{
		int aseVersionNumber = 0;

		String[] aseVersionParts = versionStr.split("/");
		if (aseVersionParts.length > 0)
		{
			String aseVersionNumberStr = null;
			String aseEsdStr = null;
			// Scan the string to see if there are any part that looks like a version str (##.#.#)
			for (int i=0; i<aseVersionParts.length; i++)
			{
				if ( aseVersionParts[i].matches("^[0-9][0-9][.][0-9][.][0-9]") )
				{
					// stop on first encounter
					aseVersionNumberStr = aseVersionParts[i];
					//break;
				}

				if ( aseVersionParts[i].indexOf("ESD#") > 0)
				{
					aseEsdStr = aseVersionParts[i];
				}
			}

			if (aseVersionNumberStr == null)
			{
				_logger.warn("There ASE version string seems to be faulty, cant find any '##.#.#' in the version number string '" + versionStr + "'.");
			}

			String[] aseVersionNumberParts = aseVersionNumberStr.split("\\.");
			if (aseVersionNumberParts.length > 1)
			{
				// Version parts can contain characters...
				// hmm version could be: 12.5.3a
				try
				{
					String versionPart = null;
					// MAJOR version: ( 12.5.2.1 - major.minor.maint.rollup )
					if (aseVersionNumberParts.length >= 1)
					{
						versionPart = aseVersionNumberParts[0].trim();
						aseVersionNumber += 1000 * Integer.parseInt(versionPart);
					}

					// MINOR version: ( 12.5.2.1 - major.minor.maint.rollup )
					if (aseVersionNumberParts.length >= 2)
					{
						versionPart = aseVersionNumberParts[1].trim().substring(0, 1);
						aseVersionNumber += 100 * Integer.parseInt(versionPart);
					}

					// MAINTENANCE version: ( 12.5.2.1 -
					// major.minor.maint.rollup )
					if (aseVersionNumberParts.length >= 3)
					{
						versionPart = aseVersionNumberParts[2].trim().substring(0, 1);
						aseVersionNumber += 10 * Integer.parseInt(versionPart);
					}

					// ROLLUP version: ( 12.5.2.1 - major.minor.maint.rollup )
					if (aseVersionNumberParts.length >= 4)
					{
						versionPart = aseVersionNumberParts[3].trim().substring(0, 1);
						aseVersionNumber += 1 * Integer.parseInt(versionPart);
					}
					else // go and check for ESD string 
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
									aseVersionNumber += 1 * Integer.parseInt(versionPart);
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
			String errorMesage = "Sorry the ASE server is not properly configured for monitoring.\n";

			rs = stmt.executeQuery("sp_configure 'enable monitoring'");
			while (rs.next())
			{
				if (rs.getInt(5) == 0)
				{
					configEnableMonitoring = false;
					_logger.error("ASE Configuration option 'enable monitoring' is NOT enabled.");
					errorMesage += "\n - ASE Configuration option 'enable monitoring' is NOT enabled.";
				}
			}

			// Check if 'wait event timing' is activated
			rs = stmt.executeQuery("sp_configure 'wait event timing'");
			while (rs.next())
			{
				if (rs.getInt(5) == 0)
				{
					configWaitEventTiming = false;
					_logger.error("ASE Configuration option 'wait event timing' is NOT enabled.");
					errorMesage += "\n - ASE Configuration option 'wait event timing' is NOT enabled.";
				}
			}

			// Check if 'per object statistics active' is activated
			rs = stmt.executeQuery("sp_configure 'per object statistics active'");
			while (rs.next())
			{
				if (rs.getInt(5) == 0)
				{
					configPerObjectStatisticsActive = false;
					_logger.error("ASE Configuration option 'per object statistics active' is NOT enabled.");
					errorMesage += "\n - ASE Configuration option 'per object statistics active' is NOT enabled.";
				}
			}

			if ( !configEnableMonitoring || !configWaitEventTiming || !configPerObjectStatisticsActive)
			{
				errorMesage += "\n\nI will open the configuration panel for you.";
				errorMesage += "\nThen try to connect again.";

				if (gui)
				{
					SwingUtils.showErrorMessage(parent, "asemon - connect check",	errorMesage, null);
	
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

}
