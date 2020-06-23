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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.apache.log4j.Logger;

import com.asetune.config.dbms.DbmsConfigIssue.Severity;
import com.asetune.config.dict.AseTraceFlagsDictionary;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;


public abstract class AseConfigText
{
	/** What sub types exists */
	public enum ConfigType
	{
		AseCacheConfig
		,AseConfigHistory
		,AseThreadPool
		,AseHelpDb
		,AseTempdb
		,AseHelpDevice
		,AseDeviceFsSpaceUsage
		,AseHelpServer
		,AseTraceflags
		,AseSpVersion
		,AseShmDumpConfig
		,AseMonitorConfig
		,AseHelpSort
		,AseResourceGovernor
		,AseLicenseInfo
		,AseClusterInfo
		,AseConfigFile
		//,AseConfigMonitoring
		};

	/** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(AseConfigText.class);

//	/** Instance variable */
//	private static Map<ConfigType, AseConfigText> _instances = new HashMap<ConfigType, AseConfigText>();
//
//	private boolean _offline = false;
//	private boolean _hasGui  = false;
////	private int     _srvVersion = 0;
//
//	/** The configuration is kept in a String */
//	private String _configStr = null;
//	
//	
//	// ------------------------------------------
//	// ---- INSTANCE METHODS --------------------
//	// ------------------------------------------
//
//	/** check if we got an instance or not */
//	public static boolean hasInstance(ConfigType type)
//	{
//		return _instances.containsKey(type);
//	}
//
//	/** Get a instance of the class, if we didn't have an instance, one will be created, but not initialized */
//	public static AseConfigText getInstance(ConfigType type)
//	{
//		if ( ! hasInstance(type))
//		{
//			createInstance(type);
//		}
//		return _instances.get(type);
//	}

	public static void createAndRegisterAllInstances()
	{
		DbmsConfigTextManager.addInstance(new AseConfigText.Cache());              // ConfigType.AseCacheConfig);
		DbmsConfigTextManager.addInstance(new AseConfigText.ConfigHistory());
		DbmsConfigTextManager.addInstance(new AseConfigText.ThreadPool());         // ConfigType.AseThreadPool);
		DbmsConfigTextManager.addInstance(new AseConfigText.HelpDb());             // ConfigType.AseHelpDb);
		DbmsConfigTextManager.addInstance(new AseConfigText.Tempdb());             // ConfigType.AseTempdb);
		DbmsConfigTextManager.addInstance(new AseConfigText.HelpDevice());         // ConfigType.AseHelpDevice);
		DbmsConfigTextManager.addInstance(new AseConfigText.DeviceFsSpaceUsage()); // ConfigType.AseDeviceFsSpaceUsage);
		DbmsConfigTextManager.addInstance(new AseConfigText.HelpServer());         // ConfigType.AseHelpServer);
		DbmsConfigTextManager.addInstance(new AseConfigText.Traceflags());         // ConfigType.AseTraceflags);
		DbmsConfigTextManager.addInstance(new AseConfigText.SpVersion());          // ConfigType.AseSpVersion);
		DbmsConfigTextManager.addInstance(new AseConfigText.ShmDumpConfig());      // ConfigType.AseShmDumpConfig);
		DbmsConfigTextManager.addInstance(new AseConfigText.MonitorConfig());      // ConfigType.AseMonitorConfig);
		DbmsConfigTextManager.addInstance(new AseConfigText.HelpSort());           // ConfigType.AseHelpSort);
		DbmsConfigTextManager.addInstance(new AseConfigText.ResourceGovernor());   // ConfigType.AseResourceGovernor);
		DbmsConfigTextManager.addInstance(new AseConfigText.LicenceInfo());        // ConfigType.AseLicenseInfo);
		DbmsConfigTextManager.addInstance(new AseConfigText.ClusterInfo());        // ConfigType.AseClusterInfo);
		DbmsConfigTextManager.addInstance(new AseConfigText.ConfigFile());         // ConfigType.AseConfigFile);
//		DbmsConfigTextManager.addInstance(new AseConfigText.ConfigMonitoring());   // ConfigType.AseConfigMonitoring);
	}

//	private static void createInstance(ConfigType type)
//	{
////		AseConfigText aseConfigText = null;
//		IDbmsConfigText aseConfigText = null;
//		switch (type)
//		{
//		case AseCacheConfig:
//			aseConfigText = new AseConfigText.Cache();
//			break;
//
//		case AseThreadPool:
//			aseConfigText = new AseConfigText.ThreadPool();
//			break;
//
//		case AseHelpDb:
//			aseConfigText = new AseConfigText.HelpDb();
//			break;
//
//		case AseTempdb:
//			aseConfigText = new AseConfigText.Tempdb();
//			break;
//
//		case AseHelpDevice:
//			aseConfigText = new AseConfigText.HelpDevice();
//			break;
//
//		case AseDeviceFsSpaceUsage:
//			aseConfigText = new AseConfigText.DeviceFsSpaceUsage();
//			break;
//
//		case AseHelpServer:
//			aseConfigText = new AseConfigText.HelpServer();
//			break;
//
//		case AseTraceflags:
//			aseConfigText = new AseConfigText.Traceflags();
//			break;
//
//		case AseSpVersion:
//			aseConfigText = new AseConfigText.SpVersion();
//			break;
//
//		case AseShmDumpConfig:
//			aseConfigText = new AseConfigText.ShmDumpConfig();
//			break;
//
//		case AseMonitorConfig:
//			aseConfigText = new AseConfigText.MonitorConfig();
//			break;
//
//		case AseHelpSort:
//			aseConfigText = new AseConfigText.HelpSort();
//			break;
//
//		case AseLicenseInfo:
//			aseConfigText = new AseConfigText.LicenceInfo();
//			break;
//
//		case AseClusterInfo:
//			aseConfigText = new AseConfigText.ClusterInfo();
//			break;
//
//		case AseConfigFile:
//			aseConfigText = new AseConfigText.ConfigFile();
//			break;
//
//		default:
//			throw new RuntimeException("Unknown type was passed when create instance. "+type);
//		}
//		
////		_instances.put(type, aseConfigText);
//		DbmsConfigTextManager.addInstance(aseConfigText);
//	}

	
	// ------------------------------------------
	// ---- LOCAL METHODS -----------------------
	// ---- probably TO BE overridden by implementors
	// ------------------------------------------

//	/** What type is this specific Configuration of */
//	abstract public ConfigType getConfigType();
//	
//	/** get SQL statement to be executed to GET current configuration string 
//	 * @param srvVersion */
//	abstract protected String getSqlCurrentConfig(long srvVersion);
//
//	/**
//	 * get SQL Statement used to get information from the offline storage
//	 * @param ts What Session timestamp are we looking for, null = last Session timestamp
//	 * @return
//	 */
//	protected String getSqlOffline(Connection conn, Timestamp ts)
//	{
//		boolean oldNameExists = DbUtils.checkIfTableExistsNoThrow(conn, null, null, "MonSessionAseConfigText");
//
//		String tabName = oldNameExists ? "\"MonSessionAseConfigText\"" : PersistWriterJdbc.getTableName(PersistWriterJdbc.SESSION_DBMS_CONFIG_TEXT, null, true);
//
//		String sql = 
//			"select \"configText\" \n" +
//			"from " + tabName + " \n" +
//			"where \"configName\"       = '"+getConfigType().toString()+"' \n" +
//			"  and \"SessionStartTime\" = ";
//
//		if (ts == null)
//		{
//			// Do a sub-select to get last timestamp
//			sql += 
//				" (select max(\"SessionStartTime\") " +
//				"  from " + tabName + 
//				" ) ";
//		}
//		else
//		{
//			// use the passed timestamp value
//			sql += "'" + ts + "'";
//		}
//
//		return sql;
//	}
//
//
//	// ------------------------------------------
//	// ---- LOCAL METHODS, probably NOT to be overridden
//	// ---- probably NOT to be overridden by implementors
//	// ------------------------------------------
//
//	/** get the Config String */
//	public String getConfig()
//	{
//		return _configStr;
//	}
//	/** set the Config String */
//	protected void setConfig(String str)
//	{
//		_configStr = str;
//	}
//
//	/** check if the AseConfig is initialized or not */
//	public boolean isInitialized()
//	{
//		return (_configStr != null ? true : false);
//	}
//
//	/**
//	 * Initialize 
//	 * @param conn
//	 */
//	public void initialize(Connection conn, boolean hasGui, boolean offline, Timestamp ts)
//	{
//		_hasGui  = hasGui;
//		_offline = offline;
//		refresh(conn, ts);
//	}
//
//	/**
//	 * Initialize 
//	 * @param conn
//	 */
//	public static void initializeAll(Connection conn, boolean hasGui, boolean offline, Timestamp ts)
//	{
//		for (ConfigType t : ConfigType.values())
//		{
//			AseConfigText aseConfigText = AseConfigText.getInstance(t);
//			if ( ! aseConfigText.isInitialized() )
//			{
//				aseConfigText.initialize(conn, hasGui, offline, ts);
//			}
//		}
//	}
//
//	/**
//	 * Reset ALL configurations, this so we can get new ones later<br>
//	 * Most possible called from disconnect() or similar
//	 */
//	public static void reset()
//	{
//		_instances.clear();
//	}
//
//
//	/**
//	 * What server version do we need to get this information.
//	 * @return an integer version in the form 12549 for version 12.5.4.9, 0 = all version
//	 */
//	public int needVersion()
//	{
//		return 0;
//	}
//
//	/**
//	 * If server needs to be Cluster Edition to get this information.
//	 * @return true or false
//	 */
//	public boolean needCluster()
//	{
//		return false; 
//	}
//
//	/**
//	 * We need any of the roles to access information.
//	 * @return List<String> of role(s) we must be apart of to get config. null = do not need any role.
//	 */
//	public List<String> needRole()
//	{
//		return null;
//	}
//
//	/**
//	 * 
//	 * @return List<String> of configurations(s) that must be true.
//	 */
//	public List<String> needConfig()
//	{ 
//		return null;
//	}
//
//	/**
//	 * refresh 
//	 * @param conn
//	 */
//	public void refresh(Connection conn, Timestamp ts)
//	{
//		if (conn == null)
//			return;
//
//		_configStr = null;
//
//		if ( ! _offline )
//		{
//			int          srvVersion = AseConnectionUtils.getAseVersionNumber(conn);
//			boolean      isCluster  = AseConnectionUtils.isClusterEnabled(conn);
//
//			int          needVersion = needVersion();
//			boolean      needCluster = needCluster();
//			List<String> needRole    = needRole();
//			List<String> needConfig  = needConfig();
//
//			// Check if we can get the configuration, due to compatible version.
//			if (needVersion > 0 && srvVersion < needVersion)
//			{
//				_configStr = "This info is only available if the Server Version is above " + Ver.versionNumToStr(needVersion);
//				return;
//			}
//
//			// Check if we can get the configuration, due to cluster.
//			if (needCluster && ! isCluster)
//			{
//				_configStr = "This info is only available if the Server is Cluster Enabled.";
//				return;
//			}
//
//			// Check if we can get the configuration, due to enough rights/role based.
//			if (needRole != null)
//			{
//				List<String> hasRoles = AseConnectionUtils.getActiveSystemRoles(conn);
//
//				boolean haveRole = false;
//				for (String role : needRole)
//				{
//					if (hasRoles.contains(role))
//						haveRole = true;
//				}
//				if ( ! haveRole )
//				{
//					_configStr = "This info is only available if you have been granted any of the following role(s) '"+needRole+"'.";
//					return;
//				}
//			}
//
//			// Check if we can get the configuration, due to enough rights/role based.
//			if (needConfig != null)
//			{
//				List<String> missingConfigs = new ArrayList<String>();
//				
//				for (String configName : needConfig)
//				{
//					boolean isConfigured = AseConnectionUtils.getAseConfigRunValueBooleanNoEx(conn, configName);
//					if ( ! isConfigured )
//						missingConfigs.add(configName);
//				}
//				
//				if (missingConfigs.size() > 0)
//				{
//					_configStr  = "This info is only available if the following configuration(s) has been enabled '"+needConfig+"'.\n";
//					_configStr += "\n";
//					_configStr += "The following configuration(s) is missing:\n";
//					for (String str : missingConfigs)
//						_configStr += "     exec sp_configure '" + str + "', 1\n";
//					return;
//				}
//			}
//
//			// Get the SQL to execute.
//			String sql = getSqlCurrentConfig(srvVersion);
//			
//			AseSqlScript script = null;
//			try
//			{
//				 // 10 seconds timeout, it shouldn't take more than 10 seconds to get Cache Config or similar.
//				script = new AseSqlScript(conn, 10); 
//				_configStr = script.executeSqlStr(sql, true);
//			}
//			catch (SQLException ex)
//			{
//				_logger.error("AseConfigText:initialize:sql='"+sql+"'", ex);
//				if (_hasGui)
//					SwingUtils.showErrorMessage("AseConfigText - Initialize", "SQL Exception: "+ex.getMessage()+"\n\nThis was found when executing SQL statement:\n\n"+sql, ex);
//				_configStr = null;
//				return;
//			}
//			finally
//			{
//				if (script != null)
//					script.close();
//			}
//		}
//		else 
//		{
//			String sql = getSqlOffline(conn, ts);
//			_configStr = "The saved value for '"+getConfigType().toString()+"' wasn't available in the offline database, sorry.";
//			
//			try
//			{
//				Statement stmt = conn.createStatement();
//
//				ResultSet rs = stmt.executeQuery(sql);
//				while ( rs.next() )
//				{
//					_configStr = rs.getString(1);
//				}
//				rs.close();
//				stmt.close();
//			}
//			catch (SQLException ex)
//			{
//				if (_offline && ex.getMessage().contains("not found"))
//				{
//					_logger.warn("The saved value for '"+getConfigType().toString()+"' wasn't available in the offline database, sorry.");
//					return;
//				}
//				_logger.error("AseConfigText:initialize:sql='"+sql+"'", ex);
//				if (_hasGui)
//					SwingUtils.showErrorMessage("AseConfigText - Initialize", "SQL Exception: "+ex.getMessage()+"\n\nThis was found when executing SQL statement:\n\n"+sql, ex);
//				_configStr = null;
//				return;
//			}
//		}
//	}

	/*-----------------------------------------------------------
	**-----------------------------------------------------------
	**-----------------------------------------------------------
	**----- SUB CLASSES ----- SUB CLASSES ----- SUB CLASSES -----
	**-----------------------------------------------------------
	**-----------------------------------------------------------
	**-----------------------------------------------------------
	*/
//	public static class Cache extends AseConfigText
	public static class Cache extends DbmsConfigTextAbstract
	{
		private static final String MB_AVAIL_FOR_RECONF_STR = "MB available for reconfiguration";

		// xxx.x MB available for reconfiguration
		private String _freeMemoryStr = null;
		private Double _freeMemory    = null;
		/** How much memory is available for reconfiguration */
		public String getFreeMemoryStr()
		{
			return _freeMemoryStr;
		}

		/** How much memory is available for reconfiguration */
		public Double getFreeMemory()
		{
			return _freeMemory == null ? 0 :_freeMemory;
		}

		@Override
		public void refresh(DbxConnection conn, Timestamp ts)
		throws SQLException
		{
			super.refresh(conn, ts);

			String configStr = getConfig();
			if (configStr != null)
			{
				// find where to copy the info
				int stop = configStr.indexOf(MB_AVAIL_FOR_RECONF_STR) - 1;
				int start = stop;
				for (; start>0; start--)
				{
					if (configStr.charAt(start) == '\n')
					{
						start++;
						break;
					}
				}

				// Then copy and parse the information
				_freeMemory = null;
				if (stop >= 0)
				{
					String mb = configStr.substring(start, stop);
					_logger.debug("parse Available Memory for reconfiguration: start="+start+", stop="+stop+", MB='"+mb+"'.");
					try 
					{
						_freeMemory = Double.parseDouble(mb);
					}
					catch(NumberFormatException e)
					{
						_logger.warn("Can't parse the Free MB for reuse, MB='"+mb+"'. Caught="+e);
						_freeMemory = null; 
					}
				}
				_freeMemoryStr =  (_freeMemory == null ? "UNKNOWN" : _freeMemory) + " " + MB_AVAIL_FOR_RECONF_STR;
			}
		}

//		@Override
//		public ConfigType getConfigType()
//		{
//			return ConfigType.AseCacheConfig;
//		}

		@Override
		public String getConfigType()
		{
			return getName();
		}

		@Override
		public String getName()
		{
			return ConfigType.AseCacheConfig.toString();
		}

		@Override
		public String getTabLabel()
		{
			return "Cache Config";
		}

		@Override
		protected String getSqlCurrentConfig(long srvVersion)
		{
			String sql = 
				"select ConfigSnapshotAtDateTime = convert(varchar(30),getdate(),109) \n" +

				"print '' \n" +
				"print '######################################################################################' \n" +
				"print '## ASE Memory available for reconfiguration' \n" +
				"print '######################################################################################' \n" +
				"declare @freeMb varchar(30) \n" +
				" \n" +
				"select @freeMb = ltrim(str( ((max(b.value) - min(b.value)) / 512.0), 15,1 )) \n" +
				"from master.dbo.sysconfigures a, master.dbo.syscurconfigs b \n" +
				"where a.name in ('max memory', 'total logical memory') \n" +
				"  and a.config = b.config \n" +
				" \n" +
				"print '%1! "+MB_AVAIL_FOR_RECONF_STR+".', @freeMb  \n" +

				"print '' \n" +
				"print '######################################################################################' \n" +
				"print '## sp_helpcache' \n" +
				"print '######################################################################################' \n" +
				"exec sp_helpcache \n" +

				"print '' \n" +
				"print '######################################################################################' \n" +
				"print '## sp_cacheconfig' \n" +
				"print '######################################################################################' \n" +
				"exec sp_cacheconfig \n" +
				"\n"; 
			
//			if (srvVersion >= 15700)
//			if (srvVersion >= 1570000)
			if (srvVersion >= Ver.ver(15,7))
			{
				sql += 
					"\n" +
					"print '' \n" +
					"print '######################################################################################' \n" +
					"print '## select * from master..syscacheinfo' \n" +
					"print '######################################################################################' \n" +
					"select * from master..syscacheinfo \n" +

					"print '' \n" +
					"print '######################################################################################' \n" +
					"print '## select * from master..syspoolinfo' \n" +
					"print '######################################################################################' \n" +
					"select * from master..syspoolinfo \n" +

					"print '' \n" +
					"print '######################################################################################' \n" +
					"print '## select * from master..syscachepoolinfo' \n" +
					"print '######################################################################################' \n" +
					"select * from master..syscachepoolinfo \n" +
					"\n";
			}

			return sql;
		}
		
		@Override
		public void checkConfig(DbxConnection conn)
		{
			// no nothing, if we havnt got an instance
			if ( ! DbmsConfigManager.hasInstance() )
				return;
			
			int    defaultDataCacheSizeInMb = -1;
			String sql        = "";
			String srvName    = "UNKNOWN";
			long   srvVersion = 0;
			Timestamp srvRestart = AseConnectionUtils.getAseStartDate(conn);
			try
			{
				srvName    = conn.getDbmsServerName();
				srvVersion = conn.getDbmsVersionNumber();
			}
			catch(SQLException ex)
			{
				_logger.error("Problems getting ASE 'default data cache' size, when getting DBMS ServerName or VersionNumber. Caught: "+ex, ex);
			}
				
			
			if (srvVersion >= Ver.ver(16,0))
				sql = "select run_size from master.dbo.syscacheinfo where cache_name = 'default data cache'";
			else
				sql = "select run_size = value/1024 from master.dbo.syscurconfigs where config=19 and comment = 'default data cache'";
			
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
					defaultDataCacheSizeInMb = rs.getInt(1);
			}
			catch(SQLException ex)
			{
				_logger.error("Problems getting ASE 'default data cache' size, using sql '"+sql+"'. Caught: "+ex, ex);
			}

			// If 'default data cache' is at "factory" setting...
			if (defaultDataCacheSizeInMb > 0 && defaultDataCacheSizeInMb < 10)
			{
				String key = "DbmsConfigIssue."+srvName+".defaultDataCache.atFactorySetting";
				
				DbmsConfigIssue issue = new DbmsConfigIssue(srvRestart, key, "default data cache", Severity.WARNING, 
						"The 'default data cache' is configured at the factory setting... 8 MB or similar... This is WAY TO LOW.", 
						"Fix this using: exec sp_cacheconfig 'default data cache', '#G'");

				DbmsConfigManager.getInstance().addConfigIssue(issue);
			}
		}
	}

	public static class ConfigHistory extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                       { return "Config History"; }
		@Override public    String     getName()                           { return ConfigType.AseConfigHistory.toString(); }
		@Override public    String     getConfigType()                     { return getName(); }
//		@Override public    ConfigType getConfigType()                     { return ConfigType.AseConfigHistory; }
		@Override public    long       needVersion()                       { return Ver.ver(16,0); }
		@Override protected String     getSqlCurrentConfig(long srvVersion) 
		{
			return ""
				+ "-- Check if the database exists... \n"
				+ "if exists (select * from master.dbo.sysdatabases where name = 'sybsecurity') \n"
				+ "begin \n"
				+ "    --execute('exec sybsecurity.dbo.sp_confighistory') \n"
//				+ "    execute('select top 1000 * from sybsecurity.dbo.ch_events order by timestamp desc') \n" // to get *last* 1000 changes...
				+ "    execute('select * from sybsecurity.dbo.ch_events where isnull(type,'''') != ''set switch'' and isnull(target,'''') != ''3604'' ') \n"
				+ "end \n"
				+ "else \n"
				+ "begin \n"
				+ "    print '--###########################################################################' \n"
				+ "    print '--ERROR: the database ''sybsecurity'' do not exist.' \n"
				+ "    print '-- NOTE: Auditing must be enabled to get this feature...' \n"
				+ "    print '--###########################################################################' \n"
				+ "    print '' \n"
				+ "    print '----------------------------------------------------------' \n"
				+ "    print '-- Below is some steps in how to create and enable this.'   \n"
				+ "    print '----------------------------------------------------------' \n"
				+ "    print '' \n"
				+ "    print '-- Create data and log device to hold the database' \n"
				+ "    print 'disk init name = ''sybsecurity_data_1'',   physname = ''/somewhere/devices/data/%1!.sybsecurity_data_1.dev'', size = ''300m'', skip_alloc=false, directio=true, dsync=false', @@servername \n"
				+ "    print 'disk init name = ''sybsecurity_log_1'',    physname = ''/somewhere/devices/log/%1!.sybsecurity_log_1.dev'',   size = ''50m'',  skip_alloc=false, directio=true, dsync=false', @@servername \n"
				+ "    print 'go' \n"
				+ "    print '' \n"
				+ "    print '-- Create the database' \n"
				+ "    print 'create database sybsecurity on sybsecurity_data_1=300 log on sybsecurity_log_1=50' \n"
				+ "    print 'go' \n"
				+ "    print 'sp_dboption sybsecurity, ''trunc log on chkpt'', true' \n"
				+ "    print 'go' \n"
				+ "    print 'sp_helpdb sybsecurity' \n"
				+ "    print 'go' \n"
				+ "    print '' \n"
				+ "    print '-- apply the install script from the OS' \n"
				+ "    print 'OS: isql -Usa -Psecret -S%1! -w999 -i ${SYBASE}/${SYBASE_ASE}/scripts/installsecurity', @@servername \n"
				+ "    print 'OS: RESTART ASE' \n"
				+ "    print '' \n"
				+ "    print '-- Configure auditing' \n"
				+ "    print 'exec sp_configure ''auditing'', 1 -- NOTE: if you get errors here, you will probably have to reboot ASE' \n"
				+ "    print 'go' \n"
				+ "    print '' \n"
				+ "    print '-- Configure auditing to save config changes' \n"
				+ "    print 'exec sp_audit ''config_history'', ''all'', ''all'', ''on'' ' \n"
				+ "    print 'go' \n"
				+ "    print '' \n"
				+ "    print '-- Create a view so we can read the config changes' \n"
				+ "    print 'exec sybsecurity.dbo.sp_confighistory ''create_view'' ' \n"
				+ "    print 'go' \n"
				+ "    print '' \n"
				+ "    print '----------------------------------------------------------' \n"
				+ "    print '-- DONE'   \n"
				+ "    print '----------------------------------------------------------' \n"
				+ "    print '' \n"
				+ "    print '-- So how do we get the configuration changes...' \n"
				+ "    print 'exec sybsecurity.dbo.sp_confighistory ' \n"
				+ "    print '' \n"
				+ "    print '-- or:' \n"
				+ "    print 'select * from sybsecurity.dbo.ch_events' \n"
				+ "    print 'go' \n"
				+ "    print '' \n"
				+ "end \n"
				+ "";
//			return "exec sybsecurity.dbo.sp_confighistory"; 
		}
	}

	public static class ThreadPool extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                       { return "Thread Pools"; }
		@Override public    String     getName()                           { return ConfigType.AseThreadPool.toString(); }
		@Override public    String     getConfigType()                     { return getName(); }
//		@Override public    ConfigType getConfigType()                     { return ConfigType.AseThreadPool; }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "select * from master.dbo.monThreadPool"; }
		@Override public    long       needVersion()                       { return Ver.ver(15,7); }
	}

	public static class HelpDb extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                       { return "sp_helpdb"; }
		@Override public    String     getName()                           { return ConfigType.AseHelpDb.toString(); }
		@Override public    String     getConfigType()                     { return getName(); }
//		@Override public    ConfigType getConfigType()                     { return ConfigType.AseHelpDb; }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "exec sp_helpdb"; }
	}

	public static class Tempdb extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                       { return "tempdb"; }
		@Override public    String     getName()                           { return ConfigType.AseTempdb.toString(); }
		@Override public    String     getConfigType()                     { return getName(); }
//		@Override public    ConfigType getConfigType()                     { return ConfigType.AseTempdb; }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "exec sp_tempdb 'show'"; }
	}

	public static class HelpDevice extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                       { return "sp_helpdevice"; }
		@Override public    String     getName()                           { return ConfigType.AseHelpDevice.toString(); }
		@Override public    String     getConfigType()                     { return getName(); }
//		@Override public    ConfigType getConfigType()                     { return ConfigType.AseHelpDevice; }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "exec sp_helpdevice"; }
		
		@Override
		public void checkConfig(DbxConnection conn)
		{
			// no nothing, if we havnt got an instance
			if ( ! DbmsConfigManager.hasInstance() )
				return;
			
			String    sql        = "exec sp_helpdevice";
			String    srvName    = "UNKNOWN";
			Timestamp srvRestart = AseConnectionUtils.getAseStartDate(conn);
			try
			{
				srvName    = conn.getDbmsServerName();
			}
			catch(SQLException ex)
			{
				_logger.error("Problems getting ASE 'sp_helpdevice' for dsync/directio, when getting DBMS ServerName or VersionNumber. Caught: "+ex, ex);
			}
				
			
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
				{
					String deviceName  = rs.getString(1);
					String physName    = rs.getString(2);
					String description = rs.getString(3);
					//int  status      = rs.getInt(4);
					int    cntrltype   = rs.getInt(5);

					// cntrltype == 0, seems to be "normal" physical devices 
					if (cntrltype != 0)
						continue;

					// skip any device names that contains "temp" or "tmp"
					if (deviceName.indexOf("temp") >= 0 || deviceName.indexOf("tmp") >= 0)
					{
						_logger.info("Checking Configuration for 'HelpDevice': Skipping device name '"+deviceName+"', which looks like a 'temp' device. (reason: name contains 'temp' or 'tmp')");
						continue;
					}
					
					boolean hasDsync    = description.indexOf("dsync on")    >= 0;
					boolean hasDirectIo = description.indexOf("directio on") >= 0;
					
					if ( hasDsync == false && hasDirectIo == false)
					{
						String key = "DbmsConfigIssue."+srvName+".device."+deviceName+".noDsyncOrDirectIo";
						
						DbmsConfigIssue issue = new DbmsConfigIssue(srvRestart, key, "device '"+deviceName+"' no dsync or directio", Severity.WARNING, 
								"The device name '"+deviceName+"' with physical name '"+physName+"' is not correctly configured for durability, directio=false and dsync=false.", 
								"Fix this using: exec sp_deviceattr '"+deviceName+"', 'directio', 'true' \n"
										+ "\n" 
										+ "Note: you also need to restart ASE for 'sp_deviceattr' to take effect.");

						DbmsConfigManager.getInstance().addConfigIssue(issue);
					}
				}
			}
			catch(SQLException ex)
			{
				_logger.error("Problems getting ASE 'sp_helpdevice' for dsync/directio, using sql '"+sql+"'. Caught: "+ex, ex);
			}
		}
	}

	public static class DeviceFsSpaceUsage extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                       { return "Device FS Usage"; }
		@Override public    String     getName()                           { return ConfigType.AseDeviceFsSpaceUsage.toString(); }
		@Override public    String     getConfigType()                     { return getName(); }
//		@Override public    ConfigType getConfigType()                     { return ConfigType.AseDeviceFsSpaceUsage; }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "select * from master.dbo.monDeviceSpaceUsage"; }
		@Override public    long       needVersion()                       { return Ver.ver(15,7); }
	}

	public static class HelpServer extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                       { return "sp_helpserver"; }
		@Override public    String     getName()                           { return ConfigType.AseHelpServer.toString(); }
		@Override public    String     getConfigType()                     { return getName(); }
//		@Override public    ConfigType getConfigType()                     { return ConfigType.AseHelpServer; }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "exec sp_helpserver"; }
	}

	public static class Traceflags extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                       { return "traceflags"; }
		@Override public    String     getName()                           { return ConfigType.AseTraceflags.toString(); }
		@Override public    String     getConfigType()                     { return getName(); }
//		@Override public ConfigType getConfigType() { return ConfigType.AseTraceflags; }

		@Override 
		public List<String> needRole()
		{
			ArrayList<String> needAnyRole = new ArrayList<String>();

			// show switch: needs any of the following roles
			needAnyRole.add("sa_role");
			needAnyRole.add("sso_role");

			return needAnyRole;
		}
		@Override 
		protected String getSqlCurrentConfig(long srvVersion) 
		{
			// 12.5.4 esd#2 and 15.0.2 supports "show switch", which makes less output in the ASE Errorlog
//			if (srvVersion >= 15020 || (srvVersion >= 12542 && srvVersion < 15000) )
//			if (srvVersion >= 1502000 || (srvVersion >= 1254020 && srvVersion < 1500000) )
			if (srvVersion >= Ver.ver(15,0,2) || (srvVersion >= Ver.ver(12,5,4,2) && srvVersion < Ver.ver(15,0)) )
				return "show switch"; 
			else
				return "dbcc traceon(3604) dbcc traceflags dbcc traceoff(3604)"; 
		}
		
		@Override
		public void refresh(DbxConnection conn, Timestamp ts)
		throws SQLException
		{
			super.refresh(conn, ts);

			String configStr = getConfig();
			
			// Check if we need to do CHANGE of the string...
			if (configStr == null)
				return;

			// "show switch" was used
			if (configStr.startsWith("Local switches"))
			{
			}
			// "dbcc traceflags" was used
			else
			{
				// Modify the output string
				// Take away: DBCC execution completed.			
				StringBuilder sb = new StringBuilder();
				boolean foundSomeRows = false;
				BufferedReader reader = new BufferedReader(new StringReader(configStr));
				try 
				{
					String inStr = null;
					while ((inStr = reader.readLine()) != null) 
					{
						if ( ! inStr.startsWith("DBCC execution completed.") )
						{
							sb.append(inStr).append("\n");
							if (inStr.length() > 0)
								foundSomeRows = true;
						}
					}
	
				}
				catch(IOException e) 
				{
					sb = new StringBuilder(configStr);
				}
	
				if ( !foundSomeRows && sb.length() < 9 )
					sb = new StringBuilder("No traceflags was found.");
	
				// Now set the config
				setConfig(sb.toString());
			}

			
			// add trace flags explanations.
			//-----------------------------------------

			// Add all integers in the config to a Set
			Set<Integer> flags = new LinkedHashSet<Integer>();
			configStr = getConfig();
			String[] sa = configStr.split("\\s"); // \s = A whitespace character: [ \t\n\x0B\f\r] 
			for (String str : sa)
			{
				try
				{
					str = str.replace(",", ""); // remove ',' - commas can be in str
					str = str.replace(".", ""); // remove '.' - dot can be in str, especially at the end
					str = str.replace(";", ""); // remove ';' - don't know if this can exists
					str = str.replace(":", ""); // remove ':' - don't know if this can exists

					int intTrace = Integer.parseInt(str);
					flags.add(intTrace);
					//System.out.println("Found trace flag '"+intTrace+"'.");
				}
				catch (NumberFormatException ignore)
				{
				}
			}
			if (flags.size() > 0)
			{
				StringBuilder sb = new StringBuilder(configStr);
				sb.append("\n\n");
				sb.append("===========================================================================\n");
				sb.append("Explanation of the trace flags\n");
				sb.append("---------------------------------------------------------------------------\n");

				AseTraceFlagsDictionary tfd = AseTraceFlagsDictionary.getInstance();
				for (Integer trace : flags)
				{
					sb.append(tfd.getDescription(trace)).append("\n");
				}
				sb.append("---------------------------------------------------------------------------\n");
				
				// Now set the config
				setConfig(sb.toString());
			}
		}
	}

	public static class SpVersion extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                       { return "sp_version"; }
		@Override public    String     getName()                           { return ConfigType.AseSpVersion.toString(); }
		@Override public    String     getConfigType()                     { return getName(); }
//		@Override public    ConfigType getConfigType()                     { return ConfigType.AseSpVersion; }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "exec sp_version"; }
		@Override public    long       needVersion()                       { return Ver.ver(12,5,4); }
	}

	public static class ShmDumpConfig extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                       { return "sp_shmdumpconfig"; }
		@Override public    String     getName()                           { return ConfigType.AseShmDumpConfig.toString(); }
		@Override public    String     getConfigType()                     { return getName(); }
//		@Override public    ConfigType getConfigType()                     { return ConfigType.AseShmDumpConfig; }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "exec sp_shmdumpconfig"; }

		@Override
		public void checkConfig(DbxConnection conn)
		{
			// TODO: Check that at least 1 shmem dump without data is configured for signal 11 or timeslice
			// http://infocenter.sybase.com/help/index.jsp?topic=/com.sybase.39996_1250/html/svrtsg/svrtsg178.htm
			// https://help.sap.com/viewer/3bdda6b0ffad441aab4fe51e4e876a19/16.0.3.5/en-US/a8b25b91bc2b1014b1e19f32c163a329.html
		}
		
		@Override
		public String getComment()
		{
			StringBuilder sb = new StringBuilder();
			
			sb.append("\n");
			sb.append("\n");
			sb.append("\n");
			sb.append("\n");
			sb.append("--##############################################################################\n");
			sb.append("--## BEGIN -- Some comments about how to add/drop/manage Shared Memory Dumps.\n");
			sb.append("--##############################################################################\n");
			sb.append("\n");
			
			sb.append("/*    \n");
			sb.append("** Below information was grabbed from:   \n");
			sb.append("**   \n");
			sb.append("** https://launchpad.support.sap.com/#/notes/0001986667   \n");
			sb.append("** https://help.sap.com/viewer/29a04b8081884fb5b715fe4aa1ab4ad2/16.0.3.4/en-US/ab7d4c75bc2b1014a897eaa73a1ca419.html   \n");
			sb.append("**   \n");
			sb.append("** Basic Syntax:   \n");
			sb.append("** sp_shmdumpconfig action, type, value, max_dumps, dump_dir, dump_file, option1, option2, option3, option4, option5   \n");
			sb.append("*/   \n");
			sb.append("   \n");
			sb.append("   \n");
			sb.append("-- SETUP: Allow Shared Memory Dumps   \n");
			sb.append("sp_configure 'dump on conditions', 1   \n");
			sb.append("go   \n");
			sb.append("   \n");
			sb.append("-- SETUP, optional: Compress the memory dump   \n");
			sb.append("sp_configure 'memory dump compression level', 2   \n");
			sb.append("   \n");
			sb.append("   \n");
			sb.append("-- Check current configuration   \n");
			sb.append("sp_shmdumpconfig   \n");
			sb.append("   \n");
			sb.append("   \n");
			sb.append("   \n");
			sb.append("/*----------------------------------------------------------------------------------------------------   \n");
			sb.append("** ADD, signal 11 (SIGSEGV), maximum of 2 dumps, to '/sybdev/shmdumps', let ASE decide the filename   \n");
			sb.append("** Note: if you do NOT want ASE to halt when dumping the memory, append:  ,'no_halt'  \n");
			sb.append("*/   \n");
			sb.append("sp_shmdumpconfig 'add', 'signal', 11, 2, '/sybdev/shmdumps', null   \n");
			sb.append("   \n");
			sb.append("   \n");
			sb.append("/*----------------------------------------------------------------------------------------------------   \n");
			sb.append("** Add so we can do \"manual\" dump using: dbcc memdump   \n");
			sb.append("** This is a good TEST to see how long a memory dump takes (which depends on the disk speed in the dump directory)   \n");
			sb.append("** Note: if you do NOT want ASE to halt when dumping the memory, append:  ,'no_halt'  \n");
			sb.append("*/   \n");
			sb.append("sp_shmdumpconfig 'add', 'dbcc', null, null, '/sybdev/shmdumps', null   \n");
			sb.append("go   \n");
			sb.append("   \n");
			sb.append("-- To test how large the dump will be and how long it takes on this host to do the memory dump   \n");
			sb.append("-- execute the following: dbcc memdump('DUMMY_DUMP: to check how long it takes on this server...'))   \n");
			sb.append("go   \n");
			sb.append("   \n");
			sb.append("   \n");
			sb.append("-- Check current configuration   \n");
			sb.append("sp_shmdumpconfig   \n");
			sb.append("   \n");
			sb.append("   \n");
			sb.append("   \n");
			sb.append("-- resets the dump count for a shared memory dump condition.   \n");
			sb.append("sp_shmdumpconfig 'reset', 'signal', 11   \n");
			sb.append("   \n");
			sb.append("   \n");
			sb.append("-- drop/disable   \n");
			sb.append("sp_shmdumpconfig 'drop', 'signal', 11   \n");
			sb.append("sp_shmdumpconfig 'drop', 'dbcc'   \n");
			sb.append("sp_configure 'dump on conditions', 0   \n");
			sb.append("   \n");
			sb.append("-- Check current configuration   \n");
			sb.append("sp_shmdumpconfig   \n");
			sb.append("   \n");
			sb.append("/*   \n");
			sb.append("** Below you find descriptions of the various parameters   \n");
			sb.append("**   \n");
			sb.append("** sp_shmdumpconfig action, type, value, max_dumps, dump_dir, dump_file, option1, option2, option3, option4, option5   \n");
			sb.append("**       \n");
			sb.append("**    --------------   \n");
			sb.append("**    -- action (string)   \n");
			sb.append("**    --------------   \n");
			sb.append("**    * add     – adds the specified shared memory dump conditions.   \n");
			sb.append("**    * drop    – drops the specified shared memory dump conditions.   \n");
			sb.append("**    * update  – changes the settings for an existing memory dump condition.   \n");
			sb.append("**    * reset   – resets the dump count for a shared memory dump condition.   \n");
			sb.append("**    * display – dispalys the current shared memory dump conditions. (this is the DEFAULT if nothing is specified)  \n");
			sb.append("**    * config  – then, parameter 'type' should have one of (below), and 'value' should be 1 or 0:   \n");
			sb.append("**    	'include errorlog' – determines if the errorlog file is included in the dump file:   \n");
			sb.append("**    		0 – do not inlude the error log in the dump file.   \n");
			sb.append("**    		1 – include the errorlog in the dump file.   \n");
			sb.append("**    	'merge files' – determines if the dump files are merged after a parallel dump:   \n");
			sb.append("**    		0 – do not merge dump files.   \n");
			sb.append("**    		1 – merge the dump files.   \n");
			sb.append("**       \n");
			sb.append("**    --------------   \n");
			sb.append("**    -- type (string)  \n");
			sb.append("**    --------------   \n");
			sb.append("**    * error     – generates a dump file for the specified server error number (for example, error numbers 1105 or 813).   \n");
			sb.append("**    * signal    – generates a dump file when the specified operating system signal occurs (for example, signals 11 or 10).   \n");
			sb.append("**    * severity  – generates a dump file when an error occurs with a severity equal to or greater than the specified severity   \n");
			sb.append("**    * module    – generates a dump file for a range of server error numbers. The range is delimited by multiples of 100, for example 800 or 1200.   \n");
			sb.append("**    * defaults   \n");
			sb.append("**    * timeslice – generates a dump file when a process receives a timeslice error.   \n");
			sb.append("**    * panic     – generates a dump file when a server panic occurs. A server panic terminates Adaptive Server after perfoming the shared memory dump.   \n");
			sb.append("**    * message   – generates a dump file when a specified error log message occurs. Contact Sybase Technical Support to optain specific error message numbers.   \n");
			sb.append("**    * dbcc      – Sets up a configuration with defaults and omissions as requested. Upon the next occurrence of the problem, issue dbcc memdump at the isql prompt to create a memory dump.  \n");
			sb.append("**       \n");
			sb.append("**    --------------   \n");
			sb.append("**    -- value (int)  \n");
			sb.append("**    --------------   \n");
			sb.append("**    * integer value   \n");
			sb.append("**       \n");
			sb.append("**    --------------   \n");
			sb.append("**    -- max_dumps (int)  \n");
			sb.append("**    --------------   \n");
			sb.append("**    * maximum number of dumps generated for a dump condition. The dump count is reset each time you restart the server. You can also reset the dump count with the reset action parameter.   \n");
			sb.append("**       \n");
			sb.append("**    --------------   \n");
			sb.append("**    -- dump_dir (string)  \n");
			sb.append("**    --------------   \n");
			sb.append("**    * is the directory in which Adaptive Server creates the dump file. The \"sybase\" user must have read and write permission in this directory.   \n");
			sb.append("**      Sybase recommends that you set the dump_dir to a known, consistent location. Make sure there is sufficient space in this directory to hold the required number of dump files.    \n");
			sb.append("**      Remove a dump_dir setting by performing an update action with an empty string ('') as the dump_dir value:   \n");
			sb.append("**    	sp_shmdumpconfig 'update', signal, 11, null, null, ''   \n");
			sb.append("**       \n");
			sb.append("**    --------------   \n");
			sb.append("**    -- dump_file (string)  \n");
			sb.append("**    --------------   \n");
			sb.append("**    * File name for the dump.    \n");
			sb.append("**      If you do not supply a file name, Adaptive Server creates a name that is guaranteed to be unique.    \n");
			sb.append("**      If you provide a file name, all files for the affected conditions use this name, and existing files are overwritten.   \n");
			sb.append("**       \n");
			sb.append("**    --------------   \n");
			sb.append("**    -- option1, ... , option5 (strings)  \n");
			sb.append("**    --------------   \n");
			sb.append("**    Determine whether areas of SAP ASE memory are included in the dump file (by default, the procedure cache is included). One of:   \n");
			sb.append("**    * include_page    – include all pages from data caches.   \n");
			sb.append("**    * omit_page       – omit all pages from data caches.   \n");
			sb.append("**    * default_page    – use the default value when including data cache pages.   \n");
			sb.append("**    * include_proc    – include all pages from the procedure cache.   \n");
			sb.append("**    * omit_proc       – omit all pages from the procedure cache.   \n");
			sb.append("**    * default_proc    – use the default values for the procedure cache.   \n");
			sb.append("**    * include_unused  – include unused pages.   \n");
			sb.append("**    * omit_unused     – omit unused pages.   \n");
			sb.append("**    * default_unused  – use the default value for unused pages.   \n");
			sb.append("**    * core            – produce a core dump if this event is triggered.   \n");
			sb.append("**    * nocore          – do not produce a core dump.   \n");
			sb.append("**    * csmd            – produce a csmd if this event is triggered.   \n");
			sb.append("**    * nocsmd          – do not produce a csmd.   \n");
			sb.append("**    * no_halt         – no engines halted during the dump.    \n");
			sb.append("**                        Use this option if you do not want to use shared memory dumps (for example, because the downtime is unacceptable or because the event triggering the shared memory dump is based on a synchronization problem, and you need to see what other engines are doing).   \n");
			sb.append("**    				     Memory dumps made with the no_halt option may contain a \"fuzzy\" image and the dump file will likely contain corrupted lock tables, run queues, and so on.   \n");
			sb.append("**    * default_halt    – \n");
			sb.append("**    * halt            – \n");
			sb.append("**    * cluster_all     – \n");
			sb.append("**    * cluster_local   – \n");
			sb.append("**    * default_cluster – \n");
			sb.append("**       \n");
			sb.append("**    Note: For the core dump options (core, nocore, csmd, nocsmd), the default behavior is to produce a csmd (configured shared memory dump) but not a core, therefore the nocore option is only used to switch off a previously requested core dump using the update function.   \n");
			sb.append("**    Values for these options override the system-wide default settings. Specify default_cache, default_proc, or default_unused to inherit the appropriate value from the system-wide default settings.   \n");
			sb.append("**    Note: Unless you are instructed otherwise by Sybase Technical Support, Sybase recommends that you include the procedure cache in all shared memory dumps.   \n");
			sb.append("*/   \n");

			sb.append("\n");
			sb.append("--##############################################################################\n");
			sb.append("--## END -- Some comments about how to add/drop/manage Shared Memory Dumps.\n");
			sb.append("--##############################################################################\n");
			
			return sb.toString();
		}
	}

	public static class MonitorConfig extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                       { return "sp_monitorconfig"; }
		@Override public    String     getName()                           { return ConfigType.AseMonitorConfig.toString(); }
		@Override public    String     getConfigType()                     { return getName(); }
//		@Override public    ConfigType getConfigType()                     { return ConfigType.AseMonitorConfig; }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "exec sp_monitorconfig 'all'"; }
		@Override public    List<String> needRole()
		{ 
			List<String> list = new ArrayList<String>();
			list.add(AseConnectionUtils.SA_ROLE);
			return list;
		}
		@Override
		public void checkConfig(DbxConnection conn)
		{
			// no nothing, if we havnt got an instance
			if ( ! DbmsConfigManager.hasInstance() )
				return;
			
			String    sql        = "exec sp_monitorconfig 'all'";
			String    srvName    = "UNKNOWN";
			Timestamp srvRestart = AseConnectionUtils.getAseStartDate(conn);
			try
			{
				srvName = conn.getDbmsServerName();
			}
			catch(SQLException ex)
			{
				_logger.error("Problems getting ASE 'sp_monitorconfig', when getting DBMS ServerName or VersionNumber. Caught: "+ex, ex);
			}
				
			
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
				{
					String name      = StringUtil.trim(rs.getString(1));
					int    numFree   =                 rs.getInt   (2);
					int    numActive =                 rs.getInt   (3);
					String pctActive = StringUtil.trim(rs.getString(4));
					int    maxUsed   =                 rs.getInt   (5);
					int    numReuse  =                 rs.getInt   (6);

					// System.out.println("MonitorConfig: row: Name='"+name+"', Num_free="+numFree+", Num_active="+numActive+", Pct_Act="+pctActive+", Max_Used="+maxUsed+", Num_Reuse="+numReuse);
					if (_logger.isDebugEnabled())
						_logger.debug("MonitorConfig: row: Name='"+name+"', Num_free="+numFree+", Num_active="+numActive+", Pct_Act="+pctActive+", Max_Used="+maxUsed+", Num_Reuse="+numReuse);

					if ( numReuse > 0 )
					{
						String key = "DbmsConfigIssue."+srvName+".sp_monitorconfig."+name+".numReuse";
						
						DbmsConfigIssue issue = new DbmsConfigIssue(srvRestart, key, name, Severity.WARNING, 
								"Configuration '"+name+"' has Num_Reuse="+numReuse+" (in sp_monitorconfig 'all'). \n"
										+ "The server will re-use older entries, which will degrade performance. \n"
										+ "\n" 
										+ "Note: This might be older 'Num_Reuse' counters, if the server hasn't rebooted.", 
								"Fix this using: exec sp_configure '"+name+"', ##### \n"
										+ "\n" 
										+ "Note: You can also check 'Num_Reuse', by: exec sp_monitorconfig 'all' \n"
										+ "Details: Name='"+name+"', Num_free="+numFree+", Num_active="+numActive+", Pct_Act="+pctActive+", Max_Used="+maxUsed+", Num_Reuse="+numReuse);

						DbmsConfigManager.getInstance().addConfigIssue(issue);
					}
				}
			}
			catch(SQLException ex)
			{
				_logger.error("Problems getting ASE 'sp_monitorconfig', using sql '"+sql+"'. Caught: "+ex, ex);
			}
		}
	}
	
	public static class HelpSort extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                       { return "sp_helpsort"; }
		@Override public    String     getName()                           { return ConfigType.AseHelpSort.toString(); }
		@Override public    String     getConfigType()                     { return getName(); }
//		@Override public    ConfigType getConfigType()                     { return ConfigType.AseHelpSort; }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "exec sp_helpsort"; }
	}

	public static class ResourceGovernor extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                       { return "Resource Governor"; }
		@Override public    String     getName()                           { return ConfigType.AseResourceGovernor.toString(); }
		@Override public    String     getConfigType()                     { return getName(); }
//		@Override public    ConfigType getConfigType()                     { return ConfigType.AseHelpResourceLimit; }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "exec sp_help_resource_limit"; }
	}

	public static class LicenceInfo extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                       { return "ASE License Info"; }
		@Override public    String     getName()                           { return ConfigType.AseLicenseInfo.toString(); }
		@Override public    String     getConfigType()                     { return getName(); }
//		@Override public    ConfigType getConfigType()                     { return ConfigType.AseLicenseInfo; }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "select * from master.dbo.monLicense"; }
		@Override public    long       needVersion()                       { return Ver.ver(15,0); }
	}
	
	public static class ClusterInfo extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                       { return "Cluster Info"; }
		@Override public    String     getName()                           { return ConfigType.AseClusterInfo.toString(); }
		@Override public    String     getConfigType()                     { return getName(); }
//		@Override public    ConfigType getConfigType()                     { return ConfigType.AseClusterInfo; }
		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "exec sp_cluster 'logical', 'show', NULL"; }
		@Override public    long       needVersion()                       { return Ver.ver(15,0,2); }
		@Override public    boolean    needCluster()                       { return true; }
	}
	
	public static class ConfigFile extends DbmsConfigTextAbstract
	{
		@Override public    String     getTabLabel()                       { return "Config File"; }
		@Override public    String     getName()                           { return ConfigType.AseConfigFile.toString(); }
		@Override public    String     getConfigType()                     { return getName(); }
//		@Override public    ConfigType getConfigType()                     { return ConfigType.AseConfigFile; }
		@Override public    long       needVersion()                       { return Ver.ver(15,0); }
		@Override public    List<String> needRole()
		{ 
			List<String> list = new ArrayList<String>();
			list.add(AseConnectionUtils.SA_ROLE); // Or SSO_ROLE
			return list;
		}
		@Override public    List<String> needConfig()
		{ 
			List<String> list = new ArrayList<String>();
			list.add("enable file access");
			return list;
		}
		@Override protected String     getSqlCurrentConfig(long srvVersion) 
		{ 
			return 
			"declare @cmd      varchar(1024) \n" +
			"declare @filename varchar(255) \n" +
			"                               \n" +
			"-- Get config file name \n" +
			"select @filename = value2  \n" +
			"  from master.dbo.syscurconfigs \n" +
			" where config = (select config from master.dbo.sysconfigures where name = 'configuration file') \n" +
			"                                        \n" +
			"-- Drop the object if it already exists \n" +
			"select @cmd = 'if (object_id(''tempdb.guest.localConfigFile_'+convert(varchar(10),@@spid)+''') is not null) ' + \n" +
			"              '    drop table tempdb.guest.localConfigFile_'+convert(varchar(10),@@spid) \n" +
			"execute(@cmd) \n" +
			"                          \n" +
			"-- Create the dummy table \n" +
			"select @cmd = 'create existing table tempdb.guest.localConfigFile_'+convert(varchar(10),@@spid)+'(record varchar(256) null) ' + \n" +
			"              'external file at ''' + @filename + '''' \n" +
			"execute(@cmd) \n" +
			"go                 \n" +
			"-- get the records \n" +
			"declare @cmd varchar(1024) \n" +
			"select @cmd = 'select isnull(record,'''') from tempdb.guest.localConfigFile_'+convert(varchar(10),@@spid) \n" +
			"execute(@cmd) \n" +
			"go                \n" +
			"-- Drop the table \n" +
			"declare @cmd varchar(1024) \n" +
			"select @cmd = 'drop table tempdb.guest.localConfigFile_'+convert(varchar(10),@@spid) \n" +
			"execute(@cmd) \n" +
			"go \n" +
			""; 
		}
		@Override
		protected void setConfig(String configStr)
		{
			// Trim all lines to get rid of all spaces at the end (since it's only 1 column)
			if (configStr != null)
			{
				StringBuilder sb = new StringBuilder();

				Scanner scanner = new Scanner(configStr);
				while (scanner.hasNextLine()) 
					sb.append( scanner.nextLine().trim() ).append("\n");
				scanner.close();
				
				configStr = sb.toString();
			}
			super.setConfig(configStr);
		}
	}
	
//	public static class ConfigMonitoring extends DbmsConfigTextAbstract
//	{
//		@Override public    String     getTabLabel()                       { return "Monitoring"; }
//		@Override public    String     getName()                           { return ConfigType.AseConfigMonitoring.toString(); }
//		@Override public    String     getConfigType()                     { return getName(); }
//		@Override protected String     getSqlCurrentConfig(long srvVersion) { return "exec sp_configure 'Monitoring'"; }
//	}

	
	/*---------------------------------------------------
	**---------------------------------------------------
	**---------------------------------------------------
	**----- TEST-CODE ---- TEST-CODE ---- TEST-CODE -----
	**---------------------------------------------------
	**---------------------------------------------------
	**---------------------------------------------------
	*/
//	public static void main(String[] args)
//	{
//		Properties log4jProps = new Properties();
//		//log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
//		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
//		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
//		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
//		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
//		PropertyConfigurator.configure(log4jProps);
//
//		Configuration conf2 = new Configuration("c:\\projects\\asetune\\dbxtune.properties");
//		Configuration.setInstance(Configuration.SYSTEM_CONF, conf2);
//
//
//		// DO THE THING
//		try
//		{
//			System.out.println("Open the Dialog with a VALID connection.");
//			Connection conn = AseConnectionFactory.getConnection("localhost", 5000, null, "sa", "", "test-AseConfigText", null);
//
//			AseConfigText c = AseConfigText.getInstance(ConfigType.AseCacheConfig);
//			c.initialize(conn, true, false, null);
//			System.out.println("AseConfigText(AseCacheConfig).getConfig()=\n"+c.getConfig());
//
//			c = AseConfigText.getInstance(ConfigType.AseThreadPool);
//			c.initialize(conn, true, false, null);
//			System.out.println("AseConfigText(AseThreadPool).getConfig()=\n"+c.getConfig());
//
//			c = AseConfigText.getInstance(ConfigType.AseHelpDb);
//			c.initialize(conn, true, false, null);
//			System.out.println("AseConfigText(AseHelpdb).getConfig()=\n"+c.getConfig());
//
//			c = AseConfigText.getInstance(ConfigType.AseTempdb);
//			c.initialize(conn, true, false, null);
//			System.out.println("AseConfigText(AseTempdb).getConfig()=\n"+c.getConfig());
//
//			c = AseConfigText.getInstance(ConfigType.AseHelpDevice);
//			c.initialize(conn, true, false, null);
//			System.out.println("AseConfigText(AseHelpdevice).getConfig()=\n"+c.getConfig());
//
//			c = AseConfigText.getInstance(ConfigType.AseDeviceFsSpaceUsage);
//			c.initialize(conn, true, false, null);
//			System.out.println("AseConfigText(AseDeviceFsSpaceUsage).getConfig()=\n"+c.getConfig());
//
//			c = AseConfigText.getInstance(ConfigType.AseHelpServer);
//			c.initialize(conn, true, false, null);
//			System.out.println("AseConfigText(AseHelpServer).getConfig()=\n"+c.getConfig());
//
//			c = AseConfigText.getInstance(ConfigType.AseTraceflags);
//			c.initialize(conn, true, false, null);
//			System.out.println("AseConfigText(AseTraceflags).getConfig()=\n"+c.getConfig());
//
//			c = AseConfigText.getInstance(ConfigType.AseSpVersion);
//			c.initialize(conn, true, false, null);
//			System.out.println("AseConfigText(AseSpVersion).getConfig()=\n"+c.getConfig());
//
//			c = AseConfigText.getInstance(ConfigType.AseShmDumpConfig);
//			c.initialize(conn, true, false, null);
//			System.out.println("AseConfigText(AseShmDumpConfig).getConfig()=\n"+c.getConfig());
//
//			c = AseConfigText.getInstance(ConfigType.AseMonitorConfig);
//			c.initialize(conn, true, false, null);
//			System.out.println("AseConfigText(AseMonitorConfig).getConfig()=\n"+c.getConfig());
//
//			c = AseConfigText.getInstance(ConfigType.AseHelpSort);
//			c.initialize(conn, true, false, null);
//			System.out.println("AseConfigText(AseHelpSort).getConfig()=\n"+c.getConfig());
//
//			c = AseConfigText.getInstance(ConfigType.AseLicenseInfo);
//			c.initialize(conn, true, false, null);
//			System.out.println("AseConfigText(AseLicenseInfo).getConfig()=\n"+c.getConfig());
//
//			c = AseConfigText.getInstance(ConfigType.AseClusterInfo);
//			c.initialize(conn, true, false, null);
//			System.out.println("AseConfigText(AseClusterInfo).getConfig()=\n"+c.getConfig());
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//		}
//	}
}
