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
import com.asetune.utils.SwingUtils;
import com.asetune.utils.Ver;


public abstract class DbmsConfigTextAbstract
implements IDbmsConfigText
{
	/** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(DbmsConfigTextAbstract.class);

//	/** Instance variable */
//	private static Map<ConfigType, DbmsConfigTextAbstract> _instances = new HashMap<ConfigType, DbmsConfigTextAbstract>();

	private boolean _offline = false;
	private boolean _hasGui  = false;
//	private int     _aseVersion = 0;

	/** The configuration is kept in a String */
	private String _configStr = null;
	
	
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
//	public static DbmsConfigTextAbstract getInstance(ConfigType type)
//	{
//		if ( ! hasInstance(type))
//		{
//			createInstance(type);
//		}
//		return _instances.get(type);
//	}
//
//	private static void createInstance(ConfigType type)
//	{
//		DbmsConfigTextAbstract aseConfigText = null;
//		switch (type)
//		{
//		case AseCacheConfig:
//			aseConfigText = new DbmsConfigTextAbstract.Cache();
//			break;
//
//		case AseThreadPool:
//			aseConfigText = new DbmsConfigTextAbstract.ThreadPool();
//			break;
//
//		case AseHelpDb:
//			aseConfigText = new DbmsConfigTextAbstract.HelpDb();
//			break;
//
//		case AseTempdb:
//			aseConfigText = new DbmsConfigTextAbstract.Tempdb();
//			break;
//
//		case AseHelpDevice:
//			aseConfigText = new DbmsConfigTextAbstract.HelpDevice();
//			break;
//
//		case AseDeviceFsSpaceUsage:
//			aseConfigText = new DbmsConfigTextAbstract.DeviceFsSpaceUsage();
//			break;
//
//		case AseHelpServer:
//			aseConfigText = new DbmsConfigTextAbstract.HelpServer();
//			break;
//
//		case AseTraceflags:
//			aseConfigText = new DbmsConfigTextAbstract.Traceflags();
//			break;
//
//		case AseSpVersion:
//			aseConfigText = new DbmsConfigTextAbstract.SpVersion();
//			break;
//
//		case AseShmDumpConfig:
//			aseConfigText = new DbmsConfigTextAbstract.ShmDumpConfig();
//			break;
//
//		case AseMonitorConfig:
//			aseConfigText = new DbmsConfigTextAbstract.MonitorConfig();
//			break;
//
//		case AseHelpSort:
//			aseConfigText = new DbmsConfigTextAbstract.HelpSort();
//			break;
//
//		case AseLicenseInfo:
//			aseConfigText = new DbmsConfigTextAbstract.LicenceInfo();
//			break;
//
//		case AseClusterInfo:
//			aseConfigText = new DbmsConfigTextAbstract.ClusterInfo();
//			break;
//
//		case AseConfigFile:
//			aseConfigText = new DbmsConfigTextAbstract.ConfigFile();
//			break;
//
//		default:
//			throw new RuntimeException("Unknown type was passed when create instance. "+type);
//		}
//		
//		_instances.put(type, aseConfigText);
//	}

	
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
	 * @param aseVersion */
	abstract protected String getSqlCurrentConfig(int aseVersion);

	/**
	 * get SQL Statement used to get information from the offline storage
	 * @param ts What Session timestamp are we looking for, null = last Session timestamp
	 * @return
	 */
	@Override
	public String getSqlOffline(DbxConnection conn, Timestamp ts)
	{
		boolean oldNameExists = DbUtils.checkIfTableExistsNoThrow(conn, null, null, "MonSessionAseConfigText");

		String tabName = oldNameExists ? "\"MonSessionAseConfigText\"" : PersistWriterJdbc.getTableName(PersistWriterJdbc.SESSION_DBMS_CONFIG_TEXT, null, true);

		String sql = 
			"select \"configText\" \n" +
			"from " + tabName + " \n" +
			"where \"configName\"       = '"+getConfigType().toString()+"' \n" +
			"  and \"SessionStartTime\" = ";

		if (ts == null)
		{
			// Do a sub-select to get last timestamp
			sql += 
				" (select max(\"SessionStartTime\") " +
				"  from " + tabName + 
				" ) ";
		}
		else
		{
			// use the passed timestamp value
			sql += "'" + ts + "'";
		}

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

//	/**
//	 * Initialize 
//	 * @param conn
//	 */
//	public static void initializeAll(DbxConnection conn, boolean hasGui, boolean offline, Timestamp ts)
//	{
//		for (ConfigType t : ConfigType.values())
//		{
//			DbmsConfigTextAbstract aseConfigText = DbmsConfigTextAbstract.getInstance(t);
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


	/**
	 * What server version do we need to get this information.
	 * @return an integer version in the form 12549 for version 12.5.4.9, 0 = all version
	 */
	@Override
	public int needVersion()
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
			int          srvVersion = conn.getDbmsVersionNumber();
			boolean      isCluster  = conn.isDbmsClusterEnabled();

			int          needVersion = needVersion();
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
				setConfig("This info is only available if the Server Version is above " + Ver.versionIntToStr(needVersion));
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

//	/*-----------------------------------------------------------
//	**-----------------------------------------------------------
//	**-----------------------------------------------------------
//	**----- SUB CLASSES ----- SUB CLASSES ----- SUB CLASSES -----
//	**-----------------------------------------------------------
//	**-----------------------------------------------------------
//	**-----------------------------------------------------------
//	*/
//	public static class Cache extends DbmsConfigTextAbstract
//	{
//		private static final String MB_AVAIL_FOR_RECONF_STR = "MB available for reconfiguration";
//
//		// xxx.x MB available for reconfiguration
//		private String _freeMemoryStr = null;
//		private Double _freeMemory    = null;
//		/** How much memory is available for reconfiguration */
//		public String getFreeMemoryStr()
//		{
//			return _freeMemoryStr;
//		}
//
//		/** How much memory is available for reconfiguration */
//		public Double getFreeMemory()
//		{
//			return _freeMemory == null ? 0 :_freeMemory;
//		}
//
//		@Override
//		public void refresh(Connection conn, Timestamp ts)
//		{
//			super.refresh(conn, ts);
//
//			String configStr = getConfig();
//			if (configStr != null)
//			{
//				// find where to copy the info
//				int stop = configStr.indexOf(MB_AVAIL_FOR_RECONF_STR) - 1;
//				int start = stop;
//				for (; start>0; start--)
//				{
//					if (configStr.charAt(start) == '\n')
//					{
//						start++;
//						break;
//					}
//				}
//
//				// Then copy and parse the information
//				_freeMemory = null;
//				if (stop > 0)
//				{
//					String mb = configStr.substring(start, stop);
//					_logger.debug("parse Available Memory for reconfiguration: start="+start+", stop="+stop+", MB='"+mb+"'.");
//					try 
//					{
//						_freeMemory = Double.parseDouble(mb);
//					}
//					catch(NumberFormatException e)
//					{
//						_logger.warn("Can't parse the Free MB for reuse, MB='"+mb+"'. Caught="+e);
//						_freeMemory = null; 
//					}
//				}
//				_freeMemoryStr =  (_freeMemory == null ? "UNKNOWN" : _freeMemory) + " " + MB_AVAIL_FOR_RECONF_STR;
//			}
//		}
//
//		@Override
//		public ConfigType getConfigType()
//		{
//			return ConfigType.AseCacheConfig;
//		}
//
//		@Override
//		protected String getSqlCurrentConfig(int aseVersion)
//		{
//			String sql = 
//				"select ConfigSnapshotAtDateTime = convert(varchar(30),getdate(),109) \n" +
//
//				"print '' \n" +
//				"print '######################################################################################' \n" +
//				"print '## ASE Memory available for reconfiguration' \n" +
//				"print '######################################################################################' \n" +
//				"declare @freeMb varchar(30) \n" +
//				" \n" +
//				"select @freeMb = ltrim(str( ((max(b.value) - min(b.value)) / 512.0), 15,1 )) \n" +
//				"from master.dbo.sysconfigures a, master.dbo.syscurconfigs b \n" +
//				"where a.name in ('max memory', 'total logical memory') \n" +
//				"  and a.config = b.config \n" +
//				" \n" +
//				"print '%1! "+MB_AVAIL_FOR_RECONF_STR+".', @freeMb  \n" +
//
//				"print '' \n" +
//				"print '######################################################################################' \n" +
//				"print '## sp_helpcache' \n" +
//				"print '######################################################################################' \n" +
//				"exec sp_helpcache \n" +
//
//				"print '' \n" +
//				"print '######################################################################################' \n" +
//				"print '## sp_cacheconfig' \n" +
//				"print '######################################################################################' \n" +
//				"exec sp_cacheconfig \n" +
//				"\n"; 
//			
////			if (aseVersion >= 15700)
////			if (aseVersion >= 1570000)
//			if (aseVersion >= Ver.ver(15,7))
//			{
//				sql += 
//					"\n" +
//					"print '' \n" +
//					"print '######################################################################################' \n" +
//					"print '## select * from master..syscacheinfo' \n" +
//					"print '######################################################################################' \n" +
//					"select * from master..syscacheinfo \n" +
//
//					"print '' \n" +
//					"print '######################################################################################' \n" +
//					"print '## select * from master..syspoolinfo' \n" +
//					"print '######################################################################################' \n" +
//					"select * from master..syspoolinfo \n" +
//
//					"print '' \n" +
//					"print '######################################################################################' \n" +
//					"print '## select * from master..syscachepoolinfo' \n" +
//					"print '######################################################################################' \n" +
//					"select * from master..syscachepoolinfo \n" +
//					"\n";
//			}
//
//			return sql;
//		}
//	}
//
//	public static class ThreadPool extends DbmsConfigTextAbstract
//	{
//		@Override public    ConfigType getConfigType()                     { return ConfigType.AseThreadPool; }
//		@Override protected String     getSqlCurrentConfig(int aseVersion) { return "select * from master.dbo.monThreadPool"; }
////		@Override public    int        needVersion()                       { return 15700; }
////		@Override public    int        needVersion()                       { return 1570000; }
//		@Override public    int        needVersion()                       { return Ver.ver(15,7); }
//	}
//
//	public static class HelpDb extends DbmsConfigTextAbstract
//	{
//		@Override public    ConfigType getConfigType()                     { return ConfigType.AseHelpDb; }
//		@Override protected String     getSqlCurrentConfig(int aseVersion) { return "exec sp_helpdb"; }
//	}
//
//	public static class Tempdb extends DbmsConfigTextAbstract
//	{
//		@Override public    ConfigType getConfigType()                     { return ConfigType.AseTempdb; }
//		@Override protected String     getSqlCurrentConfig(int aseVersion) { return "exec sp_tempdb 'show'"; }
//	}
//
//	public static class HelpDevice extends DbmsConfigTextAbstract
//	{
//		@Override public    ConfigType getConfigType()                     { return ConfigType.AseHelpDevice; }
//		@Override protected String     getSqlCurrentConfig(int aseVersion) { return "exec sp_helpdevice"; }
//	}
//
//	public static class DeviceFsSpaceUsage extends DbmsConfigTextAbstract
//	{
//		@Override public    ConfigType getConfigType()                     { return ConfigType.AseDeviceFsSpaceUsage; }
//		@Override protected String     getSqlCurrentConfig(int aseVersion) { return "select * from master.dbo.monDeviceSpaceUsage"; }
////		@Override public    int        needVersion()                       { return 15700; }
////		@Override public    int        needVersion()                       { return 1570000; }
//		@Override public    int        needVersion()                       { return Ver.ver(15,7); }
//	}
//
//	public static class HelpServer extends DbmsConfigTextAbstract
//	{
//		@Override public    ConfigType getConfigType()                     { return ConfigType.AseHelpServer; }
//		@Override protected String     getSqlCurrentConfig(int aseVersion) { return "exec sp_helpserver"; }
//	}
//
//	public static class Traceflags extends DbmsConfigTextAbstract
//	{
//		@Override public ConfigType getConfigType() { return ConfigType.AseTraceflags; }
//
//		@Override 
//		public List<String> needRole()
//		{
//			ArrayList<String> needAnyRole = new ArrayList<String>();
//
//			// show switch: needs any of the following roles
//			needAnyRole.add("sa_role");
//			needAnyRole.add("sso_role");
//
//			return needAnyRole;
//		}
//		@Override 
//		protected String getSqlCurrentConfig(int aseVersion) 
//		{
//			// 12.5.4 esd#2 and 15.0.2 supports "show switch", which makes less output in the ASE Errorlog
////			if (aseVersion >= 15020 || (aseVersion >= 12542 && aseVersion < 15000) )
////			if (aseVersion >= 1502000 || (aseVersion >= 1254020 && aseVersion < 1500000) )
//			if (aseVersion >= Ver.ver(15,0,2) || (aseVersion >= Ver.ver(12,5,4,2) && aseVersion < Ver.ver(15,0)) )
//				return "show switch"; 
//			else
//				return "dbcc traceon(3604) dbcc traceflags dbcc traceoff(3604)"; 
//		}
//		
//		@Override
//		public void refresh(Connection conn, Timestamp ts)
//		{
//			super.refresh(conn, ts);
//
//			String configStr = getConfig();
//			
//			// Check if we need to do CHANGE of the string...
//			if (configStr == null)
//				return;
//
//			// "show switch" was used
//			if (configStr.startsWith("Local switches"))
//			{
//			}
//			// "dbcc traceflags" was used
//			else
//			{
//				// Modify the output string
//				// Take away: DBCC execution completed.			
//				StringBuilder sb = new StringBuilder();
//				boolean foundSomeRows = false;
//				BufferedReader reader = new BufferedReader(new StringReader(configStr));
//				try 
//				{
//					String inStr = null;
//					while ((inStr = reader.readLine()) != null) 
//					{
//						if ( ! inStr.startsWith("DBCC execution completed.") )
//						{
//							sb.append(inStr).append("\n");
//							if (inStr.length() > 0)
//								foundSomeRows = true;
//						}
//					}
//	
//				}
//				catch(IOException e) 
//				{
//					sb = new StringBuilder(configStr);
//				}
//	
//				if ( !foundSomeRows && sb.length() < 9 )
//					sb = new StringBuilder("No traceflags was found.");
//	
//				// Now set the config
//				setConfig(sb.toString());
//			}
//
//			
//			// add trace flags explanations.
//			//-----------------------------------------
//
//			// Add all integers in the config to a Set
//			Set<Integer> flags = new LinkedHashSet<Integer>();
//			configStr = getConfig();
//			String[] sa = configStr.split("\\s"); // \s = A whitespace character: [ \t\n\x0B\f\r] 
//			for (String str : sa)
//			{
//				try
//				{
//					str = str.replace(",", ""); // remove ',' - commas can be in str
//					str = str.replace(".", ""); // remove '.' - dot can be in str, especially at the end
//					str = str.replace(";", ""); // remove ';' - don't know if this can exists
//					str = str.replace(":", ""); // remove ':' - don't know if this can exists
//
//					int intTrace = Integer.parseInt(str);
//					flags.add(intTrace);
//					//System.out.println("Found trace flag '"+intTrace+"'.");
//				}
//				catch (NumberFormatException ignore)
//				{
//				}
//			}
//			if (flags.size() > 0)
//			{
//				StringBuilder sb = new StringBuilder(configStr);
//				sb.append("\n\n");
//				sb.append("===========================================================================\n");
//				sb.append("Explanation of the trace flags\n");
//				sb.append("---------------------------------------------------------------------------\n");
//
//				AseTraceFlagsDictionary tfd = AseTraceFlagsDictionary.getInstance();
//				for (Integer trace : flags)
//				{
//					sb.append(tfd.getDescription(trace)).append("\n");
//				}
//				sb.append("---------------------------------------------------------------------------\n");
//				
//				// Now set the config
//				setConfig(sb.toString());
//			}
//		}
//	}
//
//	public static class SpVersion extends DbmsConfigTextAbstract
//	{
//		@Override public    ConfigType getConfigType()                     { return ConfigType.AseSpVersion; }
//		@Override protected String     getSqlCurrentConfig(int aseVersion) { return "exec sp_version"; }
//		@Override public    int        needVersion()                       { return Ver.ver(12,5,4); }
//	}
//
//	public static class ShmDumpConfig extends DbmsConfigTextAbstract
//	{
//		@Override public    ConfigType getConfigType()                     { return ConfigType.AseShmDumpConfig; }
//		@Override protected String     getSqlCurrentConfig(int aseVersion) { return "exec sp_shmdumpconfig"; }
//	}
//
//	public static class MonitorConfig extends DbmsConfigTextAbstract
//	{
//		@Override public    ConfigType getConfigType()                     { return ConfigType.AseMonitorConfig; }
//		@Override protected String     getSqlCurrentConfig(int aseVersion) { return "exec sp_monitorconfig 'all'"; }
//		@Override public    List<String> needRole()
//		{ 
//			List<String> list = new ArrayList<String>();
//			list.add(AseConnectionUtils.SA_ROLE);
//			return list;
//		}
//	}
//	
//	public static class HelpSort extends DbmsConfigTextAbstract
//	{
//		@Override public    ConfigType getConfigType()                     { return ConfigType.AseHelpSort; }
//		@Override protected String     getSqlCurrentConfig(int aseVersion) { return "exec sp_helpsort"; }
//	}
//
//	public static class LicenceInfo extends DbmsConfigTextAbstract
//	{
//		@Override public    ConfigType getConfigType()                     { return ConfigType.AseLicenseInfo; }
//		@Override protected String     getSqlCurrentConfig(int aseVersion) { return "select * from master.dbo.monLicense"; }
//		@Override public    int        needVersion()                       { return Ver.ver(15,0); }
//	}
//	
//	public static class ClusterInfo extends DbmsConfigTextAbstract
//	{
//		@Override public    ConfigType getConfigType()                     { return ConfigType.AseClusterInfo; }
//		@Override protected String     getSqlCurrentConfig(int aseVersion) { return "exec sp_cluster 'logical', 'show', NULL"; }
//		@Override public    int        needVersion()                       { return Ver.ver(15,0,2); }
//		@Override public    boolean    needCluster()                       { return true; }
//	}
//	
//	public static class ConfigFile extends DbmsConfigTextAbstract
//	{
//		@Override public    ConfigType getConfigType()                     { return ConfigType.AseConfigFile; }
//		@Override public    int        needVersion()                       { return Ver.ver(15,0); }
//		@Override public    List<String> needRole()
//		{ 
//			List<String> list = new ArrayList<String>();
//			list.add(AseConnectionUtils.SA_ROLE); // Or SSO_ROLE
//			return list;
//		}
//		@Override public    List<String> needConfig()
//		{ 
//			List<String> list = new ArrayList<String>();
//			list.add("enable file access");
//			return list;
//		}
//		@Override protected String     getSqlCurrentConfig(int aseVersion) 
//		{ 
//			return 
//			"declare @cmd      varchar(1024) \n" +
//			"declare @filename varchar(255) \n" +
//			"                               \n" +
//			"-- Get config file name \n" +
//			"select @filename = value2  \n" +
//			"  from master.dbo.syscurconfigs \n" +
//			" where config = (select config from master.dbo.sysconfigures where name = 'configuration file') \n" +
//			"                                        \n" +
//			"-- Drop the object if it already exists \n" +
//			"select @cmd = 'if (object_id(''tempdb.guest.localConfigFile_'+convert(varchar(10),@@spid)+''') is not null) ' + \n" +
//			"              '    drop table tempdb.guest.localConfigFile_'+convert(varchar(10),@@spid) \n" +
//			"execute(@cmd) \n" +
//			"                          \n" +
//			"-- Create the dummy table \n" +
//			"select @cmd = 'create existing table tempdb.guest.localConfigFile_'+convert(varchar(10),@@spid)+'(record varchar(256) null) ' + \n" +
//			"              'external file at ''' + @filename + '''' \n" +
//			"execute(@cmd) \n" +
//			"go                 \n" +
//			"-- get the records \n" +
//			"declare @cmd varchar(1024) \n" +
//			"select @cmd = 'select isnull(record,'''') from tempdb.guest.localConfigFile_'+convert(varchar(10),@@spid) \n" +
//			"execute(@cmd) \n" +
//			"go                \n" +
//			"-- Drop the table \n" +
//			"declare @cmd varchar(1024) \n" +
//			"select @cmd = 'drop table tempdb.guest.localConfigFile_'+convert(varchar(10),@@spid) \n" +
//			"execute(@cmd) \n" +
//			"go \n" +
//			""; 
//		}
//	}
//	
//	
//	/*---------------------------------------------------
//	**---------------------------------------------------
//	**---------------------------------------------------
//	**----- TEST-CODE ---- TEST-CODE ---- TEST-CODE -----
//	**---------------------------------------------------
//	**---------------------------------------------------
//	**---------------------------------------------------
//	*/
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
//		Configuration conf2 = new Configuration("c:\\projects\\asetune\\asetune.properties");
//		Configuration.setInstance(Configuration.SYSTEM_CONF, conf2);
//
//
//		// DO THE THING
//		try
//		{
//			System.out.println("Open the Dialog with a VALID connection.");
//			Connection conn = AseConnectionFactory.getConnection("localhost", 5000, null, "sa", "", "test-AseConfigText", null);
//
//			DbmsConfigTextAbstract c = DbmsConfigTextAbstract.getInstance(ConfigType.AseCacheConfig);
//			c.initialize(conn, true, false, null);
//			System.out.println("AseConfigText(AseCacheConfig).getConfig()=\n"+c.getConfig());
//
//			c = DbmsConfigTextAbstract.getInstance(ConfigType.AseThreadPool);
//			c.initialize(conn, true, false, null);
//			System.out.println("AseConfigText(AseThreadPool).getConfig()=\n"+c.getConfig());
//
//			c = DbmsConfigTextAbstract.getInstance(ConfigType.AseHelpDb);
//			c.initialize(conn, true, false, null);
//			System.out.println("AseConfigText(AseHelpdb).getConfig()=\n"+c.getConfig());
//
//			c = DbmsConfigTextAbstract.getInstance(ConfigType.AseTempdb);
//			c.initialize(conn, true, false, null);
//			System.out.println("AseConfigText(AseTempdb).getConfig()=\n"+c.getConfig());
//
//			c = DbmsConfigTextAbstract.getInstance(ConfigType.AseHelpDevice);
//			c.initialize(conn, true, false, null);
//			System.out.println("AseConfigText(AseHelpdevice).getConfig()=\n"+c.getConfig());
//
//			c = DbmsConfigTextAbstract.getInstance(ConfigType.AseDeviceFsSpaceUsage);
//			c.initialize(conn, true, false, null);
//			System.out.println("AseConfigText(AseDeviceFsSpaceUsage).getConfig()=\n"+c.getConfig());
//
//			c = DbmsConfigTextAbstract.getInstance(ConfigType.AseHelpServer);
//			c.initialize(conn, true, false, null);
//			System.out.println("AseConfigText(AseHelpServer).getConfig()=\n"+c.getConfig());
//
//			c = DbmsConfigTextAbstract.getInstance(ConfigType.AseTraceflags);
//			c.initialize(conn, true, false, null);
//			System.out.println("AseConfigText(AseTraceflags).getConfig()=\n"+c.getConfig());
//
//			c = DbmsConfigTextAbstract.getInstance(ConfigType.AseSpVersion);
//			c.initialize(conn, true, false, null);
//			System.out.println("AseConfigText(AseSpVersion).getConfig()=\n"+c.getConfig());
//
//			c = DbmsConfigTextAbstract.getInstance(ConfigType.AseShmDumpConfig);
//			c.initialize(conn, true, false, null);
//			System.out.println("AseConfigText(AseShmDumpConfig).getConfig()=\n"+c.getConfig());
//
//			c = DbmsConfigTextAbstract.getInstance(ConfigType.AseMonitorConfig);
//			c.initialize(conn, true, false, null);
//			System.out.println("AseConfigText(AseMonitorConfig).getConfig()=\n"+c.getConfig());
//
//			c = DbmsConfigTextAbstract.getInstance(ConfigType.AseHelpSort);
//			c.initialize(conn, true, false, null);
//			System.out.println("AseConfigText(AseHelpSort).getConfig()=\n"+c.getConfig());
//
//			c = DbmsConfigTextAbstract.getInstance(ConfigType.AseLicenseInfo);
//			c.initialize(conn, true, false, null);
//			System.out.println("AseConfigText(AseLicenseInfo).getConfig()=\n"+c.getConfig());
//
//			c = DbmsConfigTextAbstract.getInstance(ConfigType.AseClusterInfo);
//			c.initialize(conn, true, false, null);
//			System.out.println("AseConfigText(AseClusterInfo).getConfig()=\n"+c.getConfig());
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//		}
//	}
}
