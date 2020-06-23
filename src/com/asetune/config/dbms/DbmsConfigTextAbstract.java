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
import com.asetune.pcs.PersistWriterJdbc;
import com.asetune.sql.conn.DbxConnection;
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
	
	/** get SQL statement to be executed to GET current configuration string 
	 * @param srvVersion */
	abstract protected String getSqlCurrentConfig(long srvVersion);

	/**
	 * get SQL Statement used to get information from the offline storage
	 * @param ts What Session timestamp are we looking for, null = last Session timestamp
	 * @return
	 */
	@Override
	public String getSqlOffline(DbxConnection conn, Timestamp ts)
	{
		boolean oldNameExists = DbUtils.checkIfTableExistsNoThrow(conn, null, null, "MonSessionAseConfigText");

		String tabName = oldNameExists ? "[MonSessionAseConfigText]" : PersistWriterJdbc.getTableName(conn, PersistWriterJdbc.SESSION_DBMS_CONFIG_TEXT, null, true);

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
	public void initialize(DbxConnection conn, boolean hasGui, boolean offline, Timestamp ts)
	throws SQLException
	{
		_hasGui  = hasGui;
		_offline = offline;
		refresh(conn, ts);
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
	 */
	@Override
	public void refresh(DbxConnection conn, Timestamp ts)
	throws SQLException
	{
		if (conn == null)
			return;

		setConfig(null);

		if ( ! _offline )
		{
//			int          srvVersion = AseConnectionUtils.getAseVersionNumber(conn);
//			boolean      isCluster  = AseConnectionUtils.isClusterEnabled(conn);
			long         srvVersion = conn.getDbmsVersionNumber();
			boolean      isCluster  = conn.isDbmsClusterEnabled();

			long         needVersion = needVersion();
			boolean      needCluster = needCluster();
			List<String> needRole    = needRole();
			List<String> needConfig  = needConfig();
			boolean      isEnabled   = isEnabled();

			// Check if it's enabled, or should we go ahead and try to get the configuration
			if ( ! isEnabled )
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

			// Check if we can get the configuration, due to compatible version.
			if (needVersion > 0 && srvVersion < needVersion)
			{
				setConfig("This info is only available if the Server Version is above " + Ver.versionNumToStr(needVersion));
				return;
			}

			// Check if we can get the configuration, due to cluster.
			if (needCluster && ! isCluster)
			{
				setConfig("This info is only available if the Server is Cluster Enabled.");
				return;
			}

			// Check if we can get the configuration, due to enough rights/role based.
			if (needRole != null)
			{
				List<String> hasRoles = AseConnectionUtils.getActiveSystemRoles(conn);

				boolean haveRole = false;
				for (String role : needRole)
				{
					if (hasRoles.contains(role))
						haveRole = true;
				}
				if ( ! haveRole )
				{
					setConfig("This info is only available if you have been granted any of the following role(s) '"+needRole+"'.");
					return;
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
					setConfig(configStr);
					return;
				}
			}

			// Get the SQL to execute.
			String sql = getSqlCurrentConfig(srvVersion);
			
			AseSqlScript script = null;
			try
			{
				 // 10 seconds timeout, it shouldn't take more than 10 seconds to get Cache Config or similar.
				script = new AseSqlScript(conn, getSqlTimeout(), getKeepDbmsState(), getDiscardDbmsErrorList()); 
				script.setRsAsAsciiTable(true);
				setConfig(script.executeSqlStr(sql, true));
			}
			catch (SQLException ex)
			{
				_logger.error("AseConfigText:initialize:sql='"+sql+"'", ex);
				if (_hasGui)
					SwingUtils.showErrorMessage("AseConfigText - Initialize", "SQL Exception: "+ex.getMessage()+"\n\nThis was found when executing SQL statement:\n\n"+sql, ex);
				setConfig(null);

				// JZ0C0: Connection is already closed.
				// JZ006: Caught IOException: com.sybase.jdbc4.jdbc.SybConnectionDeadException: JZ0C0: Connection is already closed.
				if ( "JZ0C0".equals(ex.getSQLState()) || "JZ006".equals(ex.getSQLState()) )
				{
					try
					{
						_logger.info("AseConfigText:initialize(): lost connection... try to reconnect...");
						conn.reConnect(null);
						_logger.info("AseConfigText:initialize(): Reconnect succeeded, but the configuration will not be visible");
					}
					catch(Exception reconnectEx)
					{
						_logger.warn("AseConfigText:initialize(): reconnect failed due to: "+reconnectEx);
						throw ex; // Note throw the original exception and not reconnectEx
					}
				}

				return;
			}
			finally
			{
				if (script != null)
					script.close();
			}

			// Check if we got any strange in the configuration
			// in cese it does: report that...
			checkConfig(conn);
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
				_logger.error("AseConfigText:initialize:sql='"+sql+"'", ex);
				if (_hasGui)
					SwingUtils.showErrorMessage("AseConfigText - Initialize", "SQL Exception: "+ex.getMessage()+"\n\nThis was found when executing SQL statement:\n\n"+sql, ex);
				setConfig(null);

				// JZ0C0: Connection is already closed.
				if ("JZ0C0".equals(ex.getSQLState()))
					throw ex;

				return;
			}
		}
	}
	
	@Override
	public void checkConfig(DbxConnection conn)
	{
	}
}
