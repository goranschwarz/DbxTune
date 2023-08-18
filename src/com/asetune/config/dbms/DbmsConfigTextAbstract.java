/*******************************************************************************
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
package com.asetune.config.dbms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import com.asetune.Version;
import com.asetune.hostmon.HostMonitorConnection;
import com.asetune.pcs.PersistWriterJdbc;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.AseSqlScript;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;
import com.asetune.utils.Ver;


public abstract class DbmsConfigTextAbstract
implements IDbmsConfigText
{
	/** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(DbmsConfigTextAbstract.class);

	private boolean _offline = false;
	private boolean _hasGui  = false;
//	private int     _srvVersion = 0;

	/** The configuration is kept in a String */
	private String _configStr = null;
	
	public static final String OS_COMMAND_OUTPUT_SEPARATOR = "---------- Below is output from Operating System Command ----------\n";

	// ------------------------------------------
	// ---- LOCAL METHODS -----------------------
	// ---- probably TO BE overridden by implementors
	// ------------------------------------------

	public boolean isOffline()
	{
		return _offline;
	}

	/**
	 * When executing the statements, should we remember the various states like: AutoCommit, CurrentCatalog
	 * @return true to remember set back the states, false if we dont care
	 */
	@Override
	public boolean getKeepDbmsState()
	{
		return true;
	}

	@Override
	public int getSqlTimeout()
	{
		return 10;
	}

	@Override
	public List<Integer> getDiscardDbmsErrorList()
	{
		return null;
	}

	/** What type is this specific Configuration of */
	abstract public String getConfigType();
	

//	/** get SQL statement to be executed to GET current configuration string 
//	 * @param srvVersion */
//	abstract protected String getSqlCurrentConfig(long srvVersion);
//
//	/** get SQL statement to be executed to GET current configuration string 
//	 * @param srvVersion */
//	public String getSqlCurrentConfig(DbmsVersionInfo versionInfo)
//	{
//		return getSqlCurrentConfig(versionInfo.getLongVersion());
//	}
	/** get SQL statement to be executed to GET current configuration string 
	 * @param versionInfo */
	abstract protected String getSqlCurrentConfig(DbmsVersionInfo versionInfo);

	/** get Operating System Command to be executed to GET current configuration string */
	protected String getOsCurrentConfig(DbmsVersionInfo versionInfo, String osName)
	{
		return null;
	}
	
	/**
	 * get SQL Statement used to get information from the offline storage
	 * @param ts What Session timestamp are we looking for, null = last Session timestamp
	 * @return
	 */
	@Override
	public String getSqlOffline(DbxConnection conn, Timestamp ts)
	{
		boolean oldNameExists = DbUtils.checkIfTableExistsNoThrow(conn, null, null, "MonSessionAseConfigText");

		String schemaName = null;
		String tabName = oldNameExists ? "[MonSessionAseConfigText]" : PersistWriterJdbc.getTableName(conn, schemaName, PersistWriterJdbc.SESSION_DBMS_CONFIG_TEXT, null, true);

		String sql = 
			"select [configText] \n" +
			"from " + tabName + " \n" +
			"where [configName]       = '"+getConfigType().toString()+"' \n" +
			"  and [SessionStartTime] = ";

		if (ts == null)
		{
			// Do a sub-select to get last timestamp
			sql += 
				" (select max([SessionStartTime]) " +
				"  from " + tabName + 
				" ) ";
		}
		else
		{
			// use the passed timestamp value
			sql += "'" + ts + "'";
		}

		// replace all '[' and ']' into DBMS Vendor Specific Chars
		sql = conn.quotifySqlString(sql);

		return sql;
	}


	// ------------------------------------------
	// ---- LOCAL METHODS, probably NOT to be overridden
	// ---- probably NOT to be overridden by implementors
	// ------------------------------------------

	@Override
	public void reset()
	{
		setConfig(null);
	}

	/** get the Config String */
	@Override
	public String getConfig()
	{
		return _configStr;
	}
	/** set the Config String */
	protected void setConfig(String str)
	{
		_configStr = str;
		
		String comment = getComment();
		if (StringUtil.hasValue(comment))
		{
			_configStr += comment;
		}
	}
	
	@Override
	public String getComment()
	{
		return null;
	}

	/** check if the AseConfig is initialized or not */
	@Override
	public boolean isInitialized()
	{
		return (getConfig() != null ? true : false);
	}

	/**
	 * Initialize 
	 * @param conn
	 */
	@Override
	public void initialize(DbxConnection conn, HostMonitorConnection hostMonConn, boolean hasGui, boolean offline, Timestamp ts)
	throws SQLException
	{
		_hasGui  = hasGui;
		_offline = offline;
		refresh(conn, hostMonConn, ts);
	}

	/**
	 * What server version do we need to get this information.
	 * @return an integer version in the form 12549 for version 12.5.4.9, 0 = all version
	 */
	@Override
	public long needVersion()
	{
		return 0;
	}

	/**
	 * If server needs to be Cluster Edition to get this information.
	 * @return true or false
	 */
	@Override
	public boolean needCluster()
	{
		return false; 
	}

	/**
	 * We need any of the roles to access information.
	 * @return List<String> of role(s) we must be apart of to get config. null = do not need any role.
	 */
	@Override
	public List<String> needRole()
	{
		return null;
	}

	/**
	 * 
	 * @return List<String> of configurations(s) that must be true.
	 */
	@Override
	public List<String> needConfig()
	{ 
		return null;
	}

	/**
	 * @return true if it's enabled or false if not
	 */
	@Override
	public boolean isEnabled()
	{
		String propName = "dbms.config.text."+getName()+".enabled";
		boolean isEnabled = Configuration.getCombinedConfiguration().getBooleanProperty(propName, true);
		
		if (_logger.isDebugEnabled())
			_logger.debug(propName + "=" + isEnabled);

		return isEnabled;
	}

//	/**
//	 * Like 'isEnabled()' but in here we can check for various things on the DbxConnection and return a message why this was skipped if ww can't do the check!
//	 * @param conn
//	 * @return null or empty string if we should proceed, otherwise a message why this config check cant be done.
//	 */
//	@Override
//	public String getSkipReason(DbxConnection conn)
//	{
//		return null;
//	}

	/**
	 * Checks if we meet all the requirements for this configuration check
	 * <p>
	 * This typically does
	 * <ul>
	 *   <li>Check if we can get the configuration, due to compatible version</li>
	 *   <li>Check if we can get the configuration, due to cluster</li>
	 *   <li>Check if we can get the configuration, due to enough rights/role based.</li>
	 *   <li>etc</li>
	 * </ul>
	 * 
	 * Override this if you have special needs...
	 * 
	 * @param conn
	 * @param hostMonConn 
	 * @return null or empty string if we should proceed, otherwise a message why this configuration check can't be checked.
	 */
	@Override
	public String checkRequirements(DbxConnection conn, HostMonitorConnection hostMonConn)
	{
//		DbmsVersionInfo dbmsVersionInfo = conn.getDbmsVersionInfo();

		long         srvVersion = conn.getDbmsVersionNumber();
		boolean      isCluster  = conn.isDbmsClusterEnabled();

		long         needVersion = needVersion();
		boolean      needCluster = needCluster();
		List<String> needRole    = needRole();
		List<String> needConfig  = needConfig();

		if (_logger.isDebugEnabled())
			_logger.debug(getName() + ":checkRequirements(): srvVersion=" + srvVersion + ", isCluster=" + isCluster + ", needVersion=" + needVersion + ", needCluster=" + needCluster + ", needRole=" + needRole + ", needConfig=" + needConfig);

		// Check if we can get the configuration, due to compatible version.
		if (needVersion > 0 && srvVersion < needVersion)
		{
			return "This info is only available if the Server Version is above " + Ver.versionNumToStr(needVersion);
		}

		// Check if we can get the configuration, due to cluster.
		if (needCluster && ! isCluster)
		{
			return "This info is only available if the Server is Cluster Enabled.";
		}

		// Check if we can get the configuration, due to enough rights/role based.
		if (needRole != null)
		{
//			List<String> hasRoles = AseConnectionUtils.getActiveSystemRoles(conn);
			List<String> hasRoles = conn.getActiveServerRolesOrPermissions();

			boolean haveRole = false;
			for (String role : needRole)
			{
				if (hasRoles.contains(role))
					haveRole = true;
			}

			if (_logger.isDebugEnabled())
				_logger.debug(getName() + ":      --- haveRole=" + haveRole + ", hasRoles=" + hasRoles);

			if ( ! haveRole )
			{
				return "This info is only available if you have been granted any of the following role(s) '"+needRole+"'.";
			}
		}

		// Check if we can get the configuration, due to enough rights/role based.
		if (needConfig != null)
		{
			List<String> missingConfigs = new ArrayList<String>();
			
			for (String configName : needConfig)
			{
				boolean isConfigured = AseConnectionUtils.getAseConfigRunValueBooleanNoEx(conn, configName);
				if ( ! isConfigured )
					missingConfigs.add(configName);
			}
			
			if (missingConfigs.size() > 0)
			{
				String configStr;
				configStr  = "This info is only available if the following configuration(s) has been enabled '"+needConfig+"'.\n";
				configStr += "\n";
				configStr += "The following configuration(s) is missing:\n";
				for (String str : missingConfigs)
					configStr += "     exec sp_configure '" + str + "', 1\n";

				return configStr;
			}
		}
		
		// All requirements are met
		return null;
	}

	@Override
	public String getSyntaxEditingStyle()
	{
		return SyntaxConstants.SYNTAX_STYLE_SQL;
//		return AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_RCL;
//		return AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL;
	}

	/**
	 * refresh 
	 * @param conn
	 * @param hostMonConn 
	 */
	@Override
	public void refresh(DbxConnection conn, HostMonitorConnection hostMonConn, Timestamp ts)
	throws SQLException
	{
		if (conn == null)
			return;

		setConfig(null);

		if ( ! _offline )
		{
//			DbmsVersionInfo dbmsVersionInfo = conn.getDbmsVersionInfo();
			
			// Check if it's enabled, or should we go ahead and try to get the configuration
			if ( ! isEnabled() )
			{
				setConfig("This configuration check is disabled. \n"
						+ "To enable it: change the property 'dbms.config.text."+getName()+".enabled=false', to true. Or simply remove it.\n"
						+ "\n"
						+ "The different properties files you can change it in is:\n"
						+ "  - USER_TEMP:   " + Configuration.getInstance(Configuration.USER_TEMP).getFilename() + "\n"
						+ "  - USER_CONF:   " + Configuration.getInstance(Configuration.USER_CONF).getFilename() + "\n"
						+ "  - SYSTEM_CONF: " + Configuration.getInstance(Configuration.SYSTEM_CONF).getFilename() + "\n"
						+ Version.getAppName() + " reads the config files in the above order. So USER_TEMP overrides the other files, etc...\n"
						+ "Preferable change it in *USER_CONF* or USER_TEMP. SYSTEM_CONF is overwritten when a new version of "+Version.getAppName()+" is installed.\n"
						+ "If USER_CONF, do not exist: simply create it.");
				return;
			}

			// Check if we meet all the requirements
			String requirements  = checkRequirements(conn, hostMonConn); // return null/empty-string on OK, otherwise a message why we didn't meet the requirements 
			if (StringUtil.hasValue(requirements))
			{
				setConfig(requirements);
				return;
			}

			// Get the SQL to execute.
//			String sql = getSqlCurrentConfig(srvVersion);
//			String sql   = getSqlCurrentConfig(dbmsVersionInfo);
//			String osCmd = getOsCurrentConfig(dbmsVersionInfo);
			
			// Fetch/Execute the SQL Statement in the online DBMS
//			String result = doOnlineRefresh(conn, sql, hostMonConn, osCmd);
			String result = doOnlineRefresh(conn, hostMonConn);
			setConfig(result);
			
			// Check if we got any strange in the configuration
			// in case it does: report that...
			checkConfig(conn, hostMonConn);
		}
		else 
		{
			String sql = getSqlOffline(conn, ts);
			setConfig("The saved value for '"+getConfigType().toString()+"' wasn't available in the offline database, sorry.");
			
			try
			{
				Statement stmt = conn.createStatement();

				ResultSet rs = stmt.executeQuery(sql);
				while ( rs.next() )
				{
					setConfig(rs.getString(1));
				}
				rs.close();
				stmt.close();
			}
			catch (SQLException ex)
			{
				if (_offline && ex.getMessage().contains("not found"))
				{
					_logger.warn("The saved value for '"+getConfigType().toString()+"' wasn't available in the offline database, sorry.");
					return;
				}
				_logger.error("DbmsConfigText:initialize:sql='"+sql+"'", ex);
				if (_hasGui)
					SwingUtils.showErrorMessage("DbmsConfigText - Initialize", "SQL Exception: "+ex.getMessage()+"\n\nThis was found when executing SQL statement:\n\n"+sql, ex);
				setConfig(null);

				// JZ0C0: Connection is already closed.
				if ("JZ0C0".equals(ex.getSQLState()))
					throw ex;

				return;
			}
		}
	}
	
	/**
	 * Responsible for executing the SQL Statement and return a proper "configuration" string set as the value to show 
	 * @param conn
	 * @param sql
	 * @param hostMonConn 
	 * @param osCmd
	 * @return
	 */
//	protected String doOnlineRefresh(DbxConnection conn, String sql, HostMonitorConnection hostMonConn, String osCmd)
//	{
//		AseSqlScript script = null;
//		try
//		{
//			 // 10 seconds timeout, it shouldn't take more than 10 seconds to get Cache Config or similar.
//			script = new AseSqlScript(conn, getSqlTimeout(), getKeepDbmsState(), getDiscardDbmsErrorList()); 
//			script.setRsAsAsciiTable(true);
//
//			return script.executeSqlStr(sql, true);
//		}
//		catch (SQLException ex)
//		{
//			_logger.error("DbmsConfigText:initialize:sql='"+sql+"'", ex);
//			if (_hasGui)
//				SwingUtils.showErrorMessage("DbmsConfigText - Initialize", "SQL Exception: "+ex.getMessage()+"\n\nThis was found when executing SQL statement:\n\n"+sql, ex);
////			setConfig(null);
//			String errorMsg = "SQL Exception: "+ex.getMessage();
//
//			// JZ0C0: Connection is already closed.
//			// JZ006: Caught IOException: com.sybase.jdbc4.jdbc.SybConnectionDeadException: JZ0C0: Connection is already closed.
//			if ( "JZ0C0".equals(ex.getSQLState()) || "JZ006".equals(ex.getSQLState()) )
//			{
//				try
//				{
//					_logger.info("DbmsConfigText:initialize(): lost connection... try to reconnect...");
//					conn.reConnect(null);
//					_logger.info("DbmsConfigText:initialize(): Reconnect succeeded, but the configuration will not be visible");
//				}
//				catch(Exception reconnectEx)
//				{
//					_logger.warn("DbmsConfigText:initialize(): reconnect failed due to: "+reconnectEx);
////					throw ex; // Note throw the original exception and not reconnectEx
//					return ex + "";
//				}
//			}
//
//			return errorMsg;
//		}
//		finally
//		{
//			if (script != null)
//				script.close();
//		}
//	}

	/**
	 * Responsible for executing the SQL Statement and or OS Command, then return a proper "configuration" string set as the value to show 
	 * @param conn
	 * @param hostMonConn 
	 * @return
	 */
	protected String doOnlineRefresh(DbxConnection conn, HostMonitorConnection hostMonConn)
	{
		String result = ""; // This will be the OUTPUT

		DbmsVersionInfo dbmsVersionInfo = conn.getDbmsVersionInfo();

		String sql   = getSqlCurrentConfig(dbmsVersionInfo);
		String osCmd = "";

		// Get OS Command, depending of what OS Name we are connected to.
		if (hostMonConn != null && hostMonConn.isConnected())
		{
			String osName = hostMonConn.getOsName();
			if (StringUtil.hasValue(osName))
			{
				osCmd = getOsCurrentConfig(dbmsVersionInfo, osName);
			}
		}

		if (StringUtil.isNullOrBlank(sql) && StringUtil.isNullOrBlank(osCmd))
			return "No SQL or OS Command was specified. sql='" + sql + "', osCmd='" + osCmd + "'"
					+ ", hostMonConn.isConnected=" + (hostMonConn != null ? hostMonConn.isConnected() : "'No HostMonConn is available'");
		
		// Execute SQL Command
		if (StringUtil.hasValue(sql))
		{
			String output = doOnlineRefresh(conn, sql);
			if (StringUtil.hasValue(output))
			{
				result += output;
			}
		}

		// Execute OS Command
		if (StringUtil.hasValue(osCmd))
		{
			String output = doOnlineRefresh(hostMonConn, osCmd);
			if (StringUtil.hasValue(output))
			{
				// Add some newlines if we already have SQL Output
				if (StringUtil.hasValue(result))
				{
					result += "\n\n";
					result += OS_COMMAND_OUTPUT_SEPARATOR;
				}

				result += output;
			}
		}

		return result;
	}

//	protected String doOnlineRefresh(DbxConnection conn, String sql, HostMonitorConnection hostMonConn, String osCmd)
//	{
//		String result = "";
//
//		if (StringUtil.isNullOrBlank(sql) && StringUtil.isNullOrBlank(osCmd))
//			return "No SQL or OS Command was passed. sql='" + sql + "', osCmd='" + osCmd + "'.";
//		
//		// Execute SQL Command
//		if (StringUtil.hasValue(sql))
//		{
//			String tmp = doOnlineRefresh(conn, sql);
//			if (StringUtil.hasValue(tmp))
//			{
//				result += tmp;
//			}
//		}
//
//		// Execute OS Command
//		if (StringUtil.hasValue(osCmd))
//		{
//			String tmp = doOnlineRefresh(hostMonConn, osCmd);
//			if (StringUtil.hasValue(tmp))
//			{
//				// Add some newlines if we already have SQL Output
//				if (StringUtil.hasValue(result))
//					result += "\n\n";
//
//				result += tmp;
//			}
//		}
//
//		return result;
//	}

	protected List<String> getDiscardDbmsErrorText()
	{
		return null;
	}

	private String doOnlineRefresh(DbxConnection conn, String sql)
	{
		AseSqlScript script = null;
		try
		{
			 // 10 seconds timeout, it shouldn't take more than 10 seconds to get Cache Config or similar.
			script = new AseSqlScript(conn, getSqlTimeout(), getKeepDbmsState(), getDiscardDbmsErrorList()); 
			script.setRsAsAsciiTable(true);
			script.setDiscardDbmsErrorText( getDiscardDbmsErrorText() );

			return script.executeSqlStr(sql, true);
		}
		catch (SQLException ex)
		{
			_logger.error("DbmsConfigText:initialize:sql='"+sql+"'", ex);
			if (_hasGui)
				SwingUtils.showErrorMessage("DbmsConfigText - Initialize", "SQL Exception: "+ex.getMessage()+"\n\nThis was found when executing SQL statement:\n\n"+sql, ex);
//			setConfig(null);
			String errorMsg = "SQL Exception: "+ex.getMessage();

			// JZ0C0: Connection is already closed.
			// JZ006: Caught IOException: com.sybase.jdbc4.jdbc.SybConnectionDeadException: JZ0C0: Connection is already closed.
			if ( "JZ0C0".equals(ex.getSQLState()) || "JZ006".equals(ex.getSQLState()) )
			{
				try
				{
					_logger.info("DbmsConfigText:initialize(): lost connection... try to reconnect...");
					conn.reConnect(null);
					_logger.info("DbmsConfigText:initialize(): Reconnect succeeded, but the configuration will not be visible");
				}
				catch(Exception reconnectEx)
				{
					_logger.warn("DbmsConfigText:initialize(): reconnect failed due to: "+reconnectEx);
//					throw ex; // Note throw the original exception and not reconnectEx
					return ex + "";
				}
			}

			return errorMsg;
		}
		finally
		{
			if (script != null)
				script.close();
		}
	}

	private String doOnlineRefresh(HostMonitorConnection hostMonConn, String osCmd)
	{
		if (StringUtil.isNullOrBlank(osCmd))
			return "WARNING: No 'OS Command' was provided.";

		if (hostMonConn == null)
			return "WARNING: No 'hostMonConn' was provided.";

		if ( ! hostMonConn.isConnected() )
			return "WARNING: The provided 'hostMonConn' was NOT Connected, cant execute OS Command '" + osCmd + "'.";

		String osName = hostMonConn.getOsName();
//System.out.println("-------- hostMonConn='" + hostMonConn + "'.");
//System.out.println("-------- getOsName        ='" + hostMonConn.getOsName() + "'.");
//System.out.println("-------- getHostname      ='" + hostMonConn.getHostname() + "'.");
//System.out.println("-------- getOsCharset     ='" + hostMonConn.getOsCharset() + "'.");
//System.out.println("-------- getConnectionType='" + hostMonConn.getConnectionType() + "'.");

		try
		{
			String tmp = hostMonConn.execCommandOutputAsStr(osCmd);
//			return tmp;
//System.out.println("-------- execCommandOutputAsStr=|" + tmp + "|.");
			return "" 
				+ "DEBUG: osName=|" + osName + "|\n" 
				+ "DEBUG: osCmd=|"  + osCmd  + "|\n" 
				+ tmp;
		}
		catch (Exception ex)
		{
			if (_hasGui)
				SwingUtils.showErrorMessage("DbmsConfigText - Initialize - OSCommand", "Exception: " + ex.getMessage() + "\n\nThis was found when executing Operating System Command:\n\n" + osCmd, ex);

			return "ERROR: " + ex;
		}
	}


	@Override
	public void checkConfig(DbxConnection conn, HostMonitorConnection hostMonConn)
	{
	}
}
