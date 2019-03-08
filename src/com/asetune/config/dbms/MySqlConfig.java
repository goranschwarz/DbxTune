/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.config.dbms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.asetune.pcs.PersistWriterJdbc;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.SwingUtils;


public class MySqlConfig
extends DbmsConfigAbstract 
{
	private static final long serialVersionUID = 1L;

	/** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(MySqlConfig.class);

	private boolean   _offline   = false;
	private boolean   _hasGui    = false;
	private Timestamp _timestamp = null;

	
	public static final String NON_DEFAULT          = "NonDefault";
	public static final String CONFIG_NAME          = "Name";
	public static final String RUN_VALUE            = "RunValue";
	public static final String PENDING              = "IsPending";
	public static final String CONFIG_VALUE         = "ConfigValue";
	public static final String DEFAULT_VALUE        = "DefaultValue";
	public static final String LEGAL_VALUES         = "LegalValues";
	public static final String SECTION_NAME         = "Category";
	public static final String RESTART_IS_REQ       = "Restart";
	public static final String IS_ADVANCED          = "Advanced";
	public static final String DESCRIPTION          = "Description";
	public static final String COMMENT              = "Comment";

	private static final int COL_NAME        = 0;
	private static final int COL_EDITABLE    = 1;
	private static final int COL_CLASS       = 2;
	private static final int COL_SQLTYPE     = 3;
	private static final int COL_TOOLTIP     = 4;
	private static Object[][] COLUMN_HEADERS = 
	{
	//   ColumnName,           Editable, JTable type,   SQL Datatype,   Tooltip
	//   --------------------- --------- -------------- --------------- --------------------------------------------
		{NON_DEFAULT,          false,    Boolean.class, "bit",           "True if the value is not configured to the default value."},
		{SECTION_NAME,         true,     String .class, "varchar(60)",   "DELETEME?: Configuration Group"},
		{CONFIG_NAME,          true,     String .class, "varchar(64)",   "Name of the configuration, same as the name in 'show global variables'."},
		{RUN_VALUE,            true,     String .class, "varchar(1024)", "Value of the configuration 'Value' ."},
		{PENDING,              false,    Boolean.class, "bit",           "DELETEME?: The Configuration has not yet taken effect, probably needs restart to take effect."},
		{CONFIG_VALUE,         true,     String .class, "varchar(1024)", "DELETEME?: The value which will be configured on next restart, if PENDING is true."},
		{DEFAULT_VALUE,        true,     String .class, "varchar(1024)", "DELETEME?: The default configuration value."},
		{RESTART_IS_REQ,       false,    Boolean.class, "bit",           "DELETEME?: Server needs to be rebooted for the configuration to take effect."},
		{IS_ADVANCED,          false,    Boolean.class, "bit",           "DELETEME?: Considered as an \"advanced\" option."},
		{LEGAL_VALUES,         true,     String .class, "varchar(60)",   "DELETEME?: What legal values can this configuration hold"},
		{DESCRIPTION,          true,     String .class, "varchar(255)",  "Description of the configuration."},
		{COMMENT,              true,     String .class, "varchar(1024)", "Some comment(s) about this configuration."}
	};

	// RS> Col# Label         JDBC Type Name         Guessed DBMS type Source Table    
	// RS> ---- ------------- ---------------------- ----------------- ----------------
	// RS> 1    Variable_name java.sql.Types.VARCHAR VARCHAR(64)       global_variables
	// RS> 2    Value         java.sql.Types.VARCHAR VARCHAR(1024)     global_variables
	
	private static String GET_CONFIG_ONLINE_SQL = 
		"show global variables \n" +
		"";

	private static String GET_CONFIG_OFFLINE_SQL = 
		"select * " +
		"from [" + PersistWriterJdbc.getTableName(null, PersistWriterJdbc.SESSION_DBMS_CONFIG, null, false) + "] \n" +
		"where [SessionStartTime] = SESSION_START_TIME \n";

	private static String GET_CONFIG_OFFLINE_MAX_SESSION_SQL = 
		" (select max([SessionStartTime]) " +
		"  from ["+PersistWriterJdbc.getTableName(null, PersistWriterJdbc.SESSION_DBMS_CONFIG, null, false) + "]" + 
		" ) ";


	/** hash table */
	private HashMap<String,MySqlConfigEntry> _configMap  = null;
	private ArrayList<MySqlConfigEntry>      _configList = null;

	private ArrayList<String>              _configSectionList = null;

	public class MySqlConfigEntry
	{
		/** configuration has been changed by any user */
		public boolean isNonDefault;

		/** Different sections in the configuration */
		public String  sectionName;

		/** Name of the configuration */
		public String  configName;

		/** integer representation of the configuration */
//		public String  configValue;
		public String  runValue;

		/** true if the configuration value is set, but we need to restart the server to enable the configuration */
		public boolean isPending;

		/** integer representation of the pending configuration */
//		public String  pendingValue;
		public String  configValue;

		/** String representation of the default configuration */
		public String  defaultValue;

		/** true if the server needs to be restarted to enable the configuration */
		public boolean requiresRestart;

		/** true if the server consider this as an advanced option */
		public boolean isAdvanced;

		/** What legal values can this configuration hold */
		public String  legalValues;

		/** description of the configuration option */
		public String  description;

		/** Some comment(s) about this configuration */
		public String  comment;
	}

	public MySqlConfig()
	{
		DbmsConfigTextManager.clear();
		MySqlConfigText.createAndRegisterAllInstances();
	}

	/**
	 * Reset ALL configurations, this so we can get new ones later<br>
	 * Most possible called from disconnect() or similar
	 */
	@Override
	public void reset()
	{
		_configMap = null;
		super.reset();
	}

//	/** get the Map */
//	public Map<String,MySqlConfigEntry> getMySqlConfigMap()
//	{
//		return _configMap;
//	}

	/** check if the MySqlConfig is initialized or not */
	@Override
	public boolean isInitialized()
	{
		return _configMap != null;
	}

	/**
	 * Initialize 
	 * @param conn
	 */
	@Override
	public void initialize(DbxConnection conn, boolean hasGui, boolean offline, Timestamp ts)
	{
		_hasGui  = hasGui;
		_offline = offline;
		refresh(conn, ts);
	}

	/**
	 * refresh 
	 * @param conn
	 */
	@Override
	public void refresh(DbxConnection conn, Timestamp ts)
	{
		if (conn == null)
			return;

		reset();
		
		_configMap         = new HashMap<String,MySqlConfigEntry>();
		_configList        = new ArrayList<MySqlConfigEntry>();
		_configSectionList = new ArrayList<String>();
		
		String sql = "";
		try
		{
			// ONLINE GET CONFIG
			Statement stmt = conn.createStatement();
			stmt.setQueryTimeout(10);

			if ( ! _offline )
			{
				sql = GET_CONFIG_ONLINE_SQL;

				// First simple get a date
				ResultSet rs = stmt.executeQuery("SELECT CURRENT_TIMESTAMP");
				while ( rs.next() )
					_timestamp = rs.getTimestamp(1);
				rs.close();

				// Then execute the Real query
				rs = stmt.executeQuery(sql);
				while ( rs.next() )
				{
					MySqlConfigEntry entry = new MySqlConfigEntry();

					// RS> Col# Label         JDBC Type Name         Guessed DBMS type Source Table    
					// RS> ---- ------------- ---------------------- ----------------- ----------------
					// RS> 1    Variable_name java.sql.Types.VARCHAR VARCHAR(64)       global_variables
					// RS> 2    Value         java.sql.Types.VARCHAR VARCHAR(1024)     global_variables

					entry.configName         = rs.getString (1);
					entry.runValue           = rs.getString (2);
					entry.legalValues        = ""; // "Min: " + rs.getString(3) + ", Max: " + rs.getString(4); 
					entry.configValue        = ""; // rs.getInt    (5);
					entry.description        = ""; // rs.getString (6);
					entry.requiresRestart    = false; // rs.getInt    (7) > 0; 
					entry.isAdvanced         = false; // rs.getInt    (8) > 0; 
					entry.defaultValue       = getDefaultConfigValue(entry.configName);
					entry.sectionName        = getSectionName       (entry.configName);
					entry.comment            = getConfigComment     (entry.configName);
					entry.isPending          = false; // entry.runValue != entry.configValue;
					entry.isNonDefault       = entry.runValue != entry.defaultValue;

					// Add
					_configMap .put(entry.configName, entry);
					_configList.add(entry);

					// Add it to the section list, if not already there
					if ( ! _configSectionList.contains(entry.sectionName) )
						_configSectionList.add(entry.sectionName);
				}
				rs.close();
			}
			else // OFFLINE GET CONFIG
			{
				sql = GET_CONFIG_OFFLINE_SQL;

				String tsStr = "";
				if (ts == null)
					tsStr = GET_CONFIG_OFFLINE_MAX_SESSION_SQL;
				else
					tsStr = "'" + ts + "'";

				sql = sql.replace("SESSION_START_TIME", tsStr);

				// replace all '[' and ']' into DBMS Vendor Specific Chars
				sql = conn.quotifySqlString(sql);
				
				// Then execute the Real query
				ResultSet rs = stmt.executeQuery(sql);
				while ( rs.next() )
				{
					MySqlConfigEntry entry = new MySqlConfigEntry();

					// NOTE: same order as in COLUMN_HEADERS
					_timestamp               = rs.getTimestamp(1);
					entry.isNonDefault       = rs.getBoolean(2);
					entry.sectionName        = rs.getString (3);
					entry.configName         = rs.getString (4);
					entry.runValue           = rs.getString (5);
					entry.isPending          = rs.getBoolean(6);
					entry.configValue        = rs.getString (7);
					entry.defaultValue       = rs.getString (8);
					entry.requiresRestart    = rs.getBoolean(9); 
					entry.isAdvanced         = rs.getBoolean(10); 
					entry.legalValues        = rs.getString (11); 
					entry.description        = rs.getString (12);
					entry.comment            = rs.getString (13);



					_configMap .put(entry.configName, entry);
					_configList.add(entry);

					// Add it to the section list, if not already there
					if ( ! _configSectionList.contains(entry.sectionName) )
						_configSectionList.add(entry.sectionName);
				}
				rs.close();
				
			}
		}
		catch (SQLException ex)
		{
			_logger.error("MySqlConfig:initialize:sql='"+sql+"'", ex);
			if (_hasGui)
				SwingUtils.showErrorMessage("MySqlConfig - Initialize", "SQL Exception: "+ex.getMessage()+"\n\nThis was found when executing SQL statement:\n\n"+sql, ex);
			_configMap = null;
			_configList = null;
			_configSectionList = null;
			return;
		}

		// Check if we got any strange in the configuration
		// in cese it does: report that...
		checkConfig(conn);
		
		// notify change
		fireTableDataChanged();
	}

	/**
	 * Get description for the configuration name
	 * @param colfigName
	 * @return the description
	 */
	@Override
	public String getDescription(String configName)
	{
		if (_configMap == null)
			return null;

		MySqlConfigEntry e = _configMap.get(configName);
		
		return e == null ? null : e.description;
	}

//	/**
//	 * Get run value
//	 * @param colfigName
//	 * @return the description
//	 */
//	@Override
//	public int getRunValue(String configName)
//	{
//		if (_configMap == null)
//			return -1;
//
//		MySqlConfigEntry e = _configMap.get(configName);
//		
//		return e == null ? -1 : e.configValue;
//		return -99;
//	}

	/**
	 * Get Section list, which is a list of all config sections
	 * @return List<String> of all sections
	 */
	@Override
	public List<String> getSectionList()
	{
		return _configSectionList;
	}


	/**
	 * Get Section list, which is a list of all config sections
	 * @return List<String> of all sections
	 */
	@Override
	public Timestamp getTimestamp()
	{
		return _timestamp;
	}


	//--------------------------------------------------------------------------------
	// BEGIN: Emulate default and ConfigSection
	//--------------------------------------------------------------------------------
	private static final Map<String, Integer> DEFAULT_VALUES_MAP;
	private static final Map<String, String>  CONFIG_SECTION_MAP;
	private static final Map<String, String>  CONFIG_COMMENT_MAP;
	static
	{
		DEFAULT_VALUES_MAP = createDefaultConfigValuesMap();
		CONFIG_SECTION_MAP = createConfigSectionMap();
		CONFIG_COMMENT_MAP = createConfigCommentMap();
	}
	
	private String getDefaultConfigValue(String configName)
	{
		return getDefaultConfigValue(configName, -999);
	}
	private String getDefaultConfigValue(String configName, int defaultVal)
	{
		Integer intVal = DEFAULT_VALUES_MAP.get(configName);
		if (intVal == null)
		{
//			System.out.println("getDefaultConfigValue(): configName='"+configName+"', not found, return default value '"+defaultVal+"'.");
			intVal = defaultVal;
		}
		
		return intVal + "";
	}

	private static Map<String, Integer> createDefaultConfigValuesMap()
	{
		HashMap<String, Integer> map = new HashMap<String, Integer>();

		map.put("access check cache bucket count",    0);
		map.put("access check cache quota",           0);
		map.put("Ad Hoc Distributed Queries",         0);
		map.put("affinity I/O mask",                  0);
		map.put("affinity mask",                      0);
		map.put("affinity64 I/O mask",                0);
		map.put("affinity64 mask",                    0);
		map.put("Agent XPs",                          0);
		map.put("allow updates",                      0);
		map.put("awe enabled",                        0);
		map.put("backup compression default",         0);
		map.put("blocked process threshold (s)",      0);
		map.put("c2 audit mode",                      0);
		map.put("clr enabled",                        0);
		map.put("common criteria compliance enabled", 0);
		map.put("cost threshold for parallelism",     5);
		map.put("cross db ownership chaining",        0);
		map.put("cursor threshold",                   -1);
		map.put("Database Mail XPs",                  0);
		map.put("default full-text language",         1033);
		map.put("default language",                   0);
		map.put("default trace enabled",              1);
		map.put("disallow results from triggers",     0);
		map.put("EKM provider enabled",               0);
		map.put("filestream access level",            0);
		map.put("fill factor (%)",                    0);
		map.put("ft crawl bandwidth (max)",           100);
		map.put("ft crawl bandwidth (min)",           0);
		map.put("ft notify bandwidth (max)",          100);
		map.put("ft notify bandwidth (min)",          0);
		map.put("index create memory (KB)",           0);
		map.put("in-doubt xact resolution",           0);
		map.put("lightweight pooling",                0);
		map.put("locks",                              0);
		map.put("max degree of parallelism",          0);
		map.put("max full-text crawl range",          4);
		map.put("max server memory (MB)",             2147483647);
		map.put("max text repl size (B)",             65536);
		map.put("max worker threads",                 0);
		map.put("media retention",                    0);
		map.put("min memory per query (KB)",          1024);
		map.put("min server memory (MB)",             0);
		map.put("nested triggers",                    1);
		map.put("network packet size (B)",            4096);
		map.put("Ole Automation Procedures",          0);
		map.put("open objects",                       0);
		map.put("optimize for ad hoc workloads",      0);
		map.put("PH timeout (s)",                     60);
		map.put("precompute rank",                    0);
		map.put("priority boost",                     0);
		map.put("query governor cost limit",          0);
		map.put("query wait (s)",                     -1);
		map.put("recovery interval (min)",            0);
		map.put("remote access",                      1);
		map.put("remote admin connections",           0);
		map.put("remote login timeout (s)",           20);
		map.put("remote proc trans",                  0);
		map.put("remote query timeout (s)",           600);
		map.put("Replication XPs",                    0);
		map.put("scan for startup procs",             0);
		map.put("server trigger recursion",           1);
		map.put("set working set size",               0);
		map.put("show advanced options",              0);
		map.put("SMO and DMO XPs",                    1);
		map.put("SQL Mail XPs",                       0);
		map.put("transform noise words",              0);
		map.put("two digit year cutoff",              2049);
		map.put("user connections",                   0);
		map.put("user instance timeout",              60);
		map.put("user instances enabled",             0);
		map.put("user options",                       0);
		map.put("xp_cmdshell",                        0);
		
		// SQL-Server 2017 (probably earlier as well)
		map.put("backup checksum default",            0);
		map.put("automatic soft-NUMA disabled",       0);
		map.put("external scripts enabled",           0);
		map.put("clr strict security",                1);
		map.put("contained database authentication",  0);
		map.put("hadoop connectivity",                0);
		map.put("polybase network encryption",        1);
		map.put("remote data archive",                0);
		map.put("allow polybase export",              0);

		return map;
	}

	private String getSectionName(String configName)
	{
		return getSectionName(configName, SECTION_UNSPECIFIED);
	}
	private String getSectionName(String configName, String defaultVal)
	{
		String strVal = CONFIG_SECTION_MAP.get(configName);
		if (strVal == null)
		{
//			System.out.println("getSectionName(): configName='"+configName+"', not found, return default value '"+defaultVal+"'.");
			strVal = defaultVal;
		}
		
		return strVal;
	}

	private static Map<String, String> createConfigSectionMap()
	{
		HashMap<String, String> map = new HashMap<String, String>();
		
		map.put("access check cache bucket count",    SECTION_UNSPECIFIED);
		map.put("access check cache quota",           SECTION_UNSPECIFIED);
		map.put("Ad Hoc Distributed Queries",         SECTION_UNSPECIFIED);
		map.put("affinity I/O mask",                  SECTION_UNSPECIFIED);
		map.put("affinity mask",                      SECTION_UNSPECIFIED);
		map.put("affinity64 I/O mask",                SECTION_UNSPECIFIED);
		map.put("affinity64 mask",                    SECTION_UNSPECIFIED);
		map.put("Agent XPs",                          SECTION_UNSPECIFIED);
		map.put("allow updates",                      SECTION_UNSPECIFIED);
		map.put("awe enabled",                        SECTION_UNSPECIFIED);
		map.put("backup compression default",         SECTION_GOOD_TO_LOOK_AT);
		map.put("blocked process threshold (s)",      SECTION_UNSPECIFIED);
		map.put("c2 audit mode",                      SECTION_UNSPECIFIED);
		map.put("clr enabled",                        SECTION_UNSPECIFIED);
		map.put("common criteria compliance enabled", SECTION_UNSPECIFIED);
		map.put("cost threshold for parallelism",     SECTION_GOOD_TO_LOOK_AT);
		map.put("cross db ownership chaining",        SECTION_UNSPECIFIED);
		map.put("cursor threshold",                   SECTION_UNSPECIFIED);
		map.put("Database Mail XPs",                  SECTION_UNSPECIFIED);
		map.put("default full-text language",         SECTION_UNSPECIFIED);
		map.put("default language",                   SECTION_UNSPECIFIED);
		map.put("default trace enabled",              SECTION_UNSPECIFIED);
		map.put("disallow results from triggers",     SECTION_UNSPECIFIED);
		map.put("EKM provider enabled",               SECTION_UNSPECIFIED);
		map.put("filestream access level",            SECTION_UNSPECIFIED);
		map.put("fill factor (%)",                    SECTION_UNSPECIFIED);
		map.put("ft crawl bandwidth (max)",           SECTION_UNSPECIFIED);
		map.put("ft crawl bandwidth (min)",           SECTION_UNSPECIFIED);
		map.put("ft notify bandwidth (max)",          SECTION_UNSPECIFIED);
		map.put("ft notify bandwidth (min)",          SECTION_UNSPECIFIED);
		map.put("index create memory (KB)",           SECTION_UNSPECIFIED);
		map.put("in-doubt xact resolution",           SECTION_UNSPECIFIED);
		map.put("lightweight pooling",                SECTION_UNSPECIFIED);
		map.put("locks",                              SECTION_UNSPECIFIED);
		map.put("max degree of parallelism",          SECTION_GOOD_TO_LOOK_AT);
		map.put("max full-text crawl range",          SECTION_UNSPECIFIED);
		map.put("max server memory (MB)",             SECTION_GOOD_TO_LOOK_AT);
		map.put("max text repl size (B)",             SECTION_UNSPECIFIED);
		map.put("max worker threads",                 SECTION_UNSPECIFIED);
		map.put("media retention",                    SECTION_UNSPECIFIED);
		map.put("min memory per query (KB)",          SECTION_UNSPECIFIED);
		map.put("min server memory (MB)",             SECTION_GOOD_TO_LOOK_AT);
		map.put("nested triggers",                    SECTION_UNSPECIFIED);
		map.put("network packet size (B)",            SECTION_UNSPECIFIED);
		map.put("Ole Automation Procedures",          SECTION_UNSPECIFIED);
		map.put("open objects",                       SECTION_UNSPECIFIED);
		map.put("optimize for ad hoc workloads",      SECTION_GOOD_TO_LOOK_AT);
		map.put("PH timeout (s)",                     SECTION_UNSPECIFIED);
		map.put("precompute rank",                    SECTION_UNSPECIFIED);
		map.put("priority boost",                     SECTION_UNSPECIFIED);
		map.put("query governor cost limit",          SECTION_UNSPECIFIED);
		map.put("query wait (s)",                     SECTION_UNSPECIFIED);
		map.put("recovery interval (min)",            SECTION_UNSPECIFIED);
		map.put("remote access",                      SECTION_UNSPECIFIED);
		map.put("remote admin connections",           SECTION_GOOD_TO_LOOK_AT);
		map.put("remote login timeout (s)",           SECTION_UNSPECIFIED);
		map.put("remote proc trans",                  SECTION_UNSPECIFIED);
		map.put("remote query timeout (s)",           SECTION_UNSPECIFIED);
		map.put("Replication XPs",                    SECTION_UNSPECIFIED);
		map.put("scan for startup procs",             SECTION_UNSPECIFIED);
		map.put("server trigger recursion",           SECTION_UNSPECIFIED);
		map.put("set working set size",               SECTION_UNSPECIFIED);
		map.put("show advanced options",              SECTION_UNSPECIFIED);
		map.put("SMO and DMO XPs",                    SECTION_UNSPECIFIED);
		map.put("SQL Mail XPs",                       SECTION_UNSPECIFIED);
		map.put("transform noise words",              SECTION_UNSPECIFIED);
		map.put("two digit year cutoff",              SECTION_UNSPECIFIED);
		map.put("user connections",                   SECTION_UNSPECIFIED);
		map.put("user instance timeout",              SECTION_UNSPECIFIED);
		map.put("user instances enabled",             SECTION_UNSPECIFIED);
		map.put("user options",                       SECTION_UNSPECIFIED);
		map.put("xp_cmdshell",                        SECTION_UNSPECIFIED);

		// SQL-Server 2017 (probably earlier as well)
		map.put("backup checksum default",            SECTION_UNSPECIFIED);
		map.put("automatic soft-NUMA disabled",       SECTION_UNSPECIFIED);
		map.put("external scripts enabled",           SECTION_UNSPECIFIED);
		map.put("clr strict security",                SECTION_UNSPECIFIED);
		map.put("contained database authentication",  SECTION_UNSPECIFIED);
		map.put("hadoop connectivity",                SECTION_UNSPECIFIED);
		map.put("polybase network encryption",        SECTION_UNSPECIFIED);
		map.put("remote data archive",                SECTION_UNSPECIFIED);
		map.put("allow polybase export",              SECTION_UNSPECIFIED);

		return map;
	}
	public static final String  SECTION_UNSPECIFIED          = "Unspecified";
	public static final String  SECTION_GOOD_TO_LOOK_AT  = "Good to look at";
	

	
	
	
	private String getConfigComment(String configName)
	{
		return getConfigComment(configName, SECTION_UNSPECIFIED);
	}
	private String getConfigComment(String configName, String defaultVal)
	{
		String strVal = CONFIG_COMMENT_MAP.get(configName);
		if (strVal == null)
		{
//			System.out.println("getConfigComment(): configName='"+configName+"', not found, return default value '"+defaultVal+"'.");
			strVal = defaultVal;
		}
		
		return strVal;
	}

	private static Map<String, String> createConfigCommentMap()
	{
		HashMap<String, String> map = new HashMap<String, String>();
		
		map.put("access check cache bucket count",    NO_COMMENT);
		map.put("access check cache quota",           NO_COMMENT);
		map.put("Ad Hoc Distributed Queries",         NO_COMMENT);
		map.put("affinity I/O mask",                  NO_COMMENT);
		map.put("affinity mask",                      NO_COMMENT);
		map.put("affinity64 I/O mask",                NO_COMMENT);
		map.put("affinity64 mask",                    NO_COMMENT);
		map.put("Agent XPs",                          NO_COMMENT);
		map.put("allow updates",                      NO_COMMENT);
		map.put("awe enabled",                        NO_COMMENT);
		map.put("backup compression default",         "We like to typically see this set to 1");
		map.put("blocked process threshold (s)",      NO_COMMENT);
		map.put("c2 audit mode",                      NO_COMMENT);
		map.put("clr enabled",                        NO_COMMENT);
		map.put("common criteria compliance enabled", NO_COMMENT);
		map.put("cost threshold for parallelism",     "5 worked awhile back when this first came out. The right number takes some work to get to... but we like to look for the default and normally bump to a higher value, why not 30 or 50. We don't want parallel execution plans on OLTP systems... Then it's time to add some indexes or rewrite the queries!");
		map.put("cross db ownership chaining",        NO_COMMENT);
		map.put("cursor threshold",                   NO_COMMENT);
		map.put("Database Mail XPs",                  NO_COMMENT);
		map.put("default full-text language",         NO_COMMENT);
		map.put("default language",                   NO_COMMENT);
		map.put("default trace enabled",              NO_COMMENT);
		map.put("disallow results from triggers",     NO_COMMENT);
		map.put("EKM provider enabled",               NO_COMMENT);
		map.put("filestream access level",            NO_COMMENT);
		map.put("fill factor (%)",                    NO_COMMENT);
		map.put("ft crawl bandwidth (max)",           NO_COMMENT);
		map.put("ft crawl bandwidth (min)",           NO_COMMENT);
		map.put("ft notify bandwidth (max)",          NO_COMMENT);
		map.put("ft notify bandwidth (min)",          NO_COMMENT);
		map.put("index create memory (KB)",           NO_COMMENT);
		map.put("in-doubt xact resolution",           NO_COMMENT);
		map.put("lightweight pooling",                NO_COMMENT);
		map.put("locks",                              NO_COMMENT);
		map.put("max degree of parallelism",          "There is no one size fits all guidance, but we like to know when it is default (and when it was changed, but the second check picks that one up.) Microsoft has some guidance that is mostly a good guide here.");
		map.put("max full-text crawl range",          NO_COMMENT);
		map.put("max server memory (MB)",             "We like to see memory purposefully left for the OS. How much? It depends on what else is running on the server, but somewhere around 20% is where we typically start the \"bidding\".");
		map.put("max text repl size (B)",             NO_COMMENT);
		map.put("max worker threads",                 NO_COMMENT);
		map.put("media retention",                    NO_COMMENT);
		map.put("min memory per query (KB)",          NO_COMMENT);
		map.put("min server memory (MB)",             "Perhaps not as important as Max Server Memory, but we like to know if it is default and will sometimes suggest changes.");
		map.put("nested triggers",                    NO_COMMENT);
		map.put("network packet size (B)",            NO_COMMENT);
		map.put("Ole Automation Procedures",          NO_COMMENT);
		map.put("open objects",                       NO_COMMENT);
		map.put("optimize for ad hoc workloads",      "Used to improve the efficiency of the plan cache for workloads that contain many single use ad hoc batches. When this option is set to 1, the Database Engine stores a small compiled plan stub in the plan cache when a batch is compiled for the first time, instead of the full compiled plan. This helps to relieve memory pressure by not allowing the plan cache to become filled with compiled plans that are not reused.");
		map.put("PH timeout (s)",                     NO_COMMENT);
		map.put("precompute rank",                    NO_COMMENT);
		map.put("priority boost",                     NO_COMMENT);
		map.put("query governor cost limit",          NO_COMMENT);
		map.put("query wait (s)",                     NO_COMMENT);
		map.put("recovery interval (min)",            NO_COMMENT);
		map.put("remote access",                      NO_COMMENT);
		map.put("remote admin connections",           "The default is Off (0) here. I like it on (1) so I can connect to a client’s instance with a remote administrator connection without having to restart. Helps in the rarest of rare situations but it is a nice tool to have.");
		map.put("remote login timeout (s)",           NO_COMMENT);
		map.put("remote proc trans",                  NO_COMMENT);
		map.put("remote query timeout (s)",           NO_COMMENT);
		map.put("Replication XPs",                    NO_COMMENT);
		map.put("scan for startup procs",             NO_COMMENT);
		map.put("server trigger recursion",           NO_COMMENT);
		map.put("set working set size",               NO_COMMENT);
		map.put("show advanced options",              NO_COMMENT);
		map.put("SMO and DMO XPs",                    NO_COMMENT);
		map.put("SQL Mail XPs",                       NO_COMMENT);
		map.put("transform noise words",              NO_COMMENT);
		map.put("two digit year cutoff",              NO_COMMENT);
		map.put("user connections",                   NO_COMMENT);
		map.put("user instance timeout",              NO_COMMENT);
		map.put("user instances enabled",             NO_COMMENT);
		map.put("user options",                       NO_COMMENT);
		map.put("xp_cmdshell",                        NO_COMMENT);
		
		// SQL-Server 2017 (probably earlier as well)
		map.put("backup checksum default",            NO_COMMENT);
		map.put("automatic soft-NUMA disabled",       NO_COMMENT);
		map.put("external scripts enabled",           NO_COMMENT);
		map.put("clr strict security",                NO_COMMENT);
		map.put("contained database authentication",  NO_COMMENT);
		map.put("hadoop connectivity",                NO_COMMENT);
		map.put("polybase network encryption",        NO_COMMENT);
		map.put("remote data archive",                NO_COMMENT);
		map.put("allow polybase export",              NO_COMMENT);

		return map;
	}
	public static final String  NO_COMMENT = "";
	
	//--------------------------------------------------------------------------------
	// END: Emulate default and ConfigSection
	//--------------------------------------------------------------------------------

	
	
	
	//--------------------------------------------------------------------------------
	// BEGIN implement: AbstractTableModel
	//--------------------------------------------------------------------------------
	@Override
	public Class<?> getColumnClass(int columnIndex)
	{
		return (Class<?>)COLUMN_HEADERS[columnIndex][COL_CLASS];
	}

	@Override
	public int getColumnCount()
	{
		return COLUMN_HEADERS.length;
	}

	@Override
	public String getColumnName(int columnIndex)
	{
		return (String)COLUMN_HEADERS[columnIndex][COL_NAME];
	}

	@Override
	public int getRowCount()
	{
		return _configList == null ? 0 : _configList.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		MySqlConfigEntry e = _configList.get(rowIndex);
		if (e == null)
			return null;

		switch (columnIndex)
		{
		case 0:  return e.isNonDefault;
		case 1:  return e.sectionName;
		case 2:  return e.configName;
		case 3:  return e.runValue;
		case 4:  return e.isPending;
		case 5:  return e.configValue;
		case 6:  return e.defaultValue;
		case 7:  return e.requiresRestart; 
		case 8:  return e.isAdvanced; 
		case 9:  return e.legalValues; 
		case 10: return e.description;
		case 11: return e.comment;
		}
		return null;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
	{
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return (Boolean) COLUMN_HEADERS[columnIndex][COL_EDITABLE];
	}

	
	@Override
	public int findColumn(String columnName) 
	{
		for (int i=0; i<COLUMN_HEADERS.length; i++) 
		{
			if (COLUMN_HEADERS[i][COL_NAME].equals(columnName)) 
				return i;
		}
		return -1;
	}
	//--------------------------------------------------------------------------------
	// END implement: AbstractTableModel
	//--------------------------------------------------------------------------------

	@Override
	public String getColumnToolTip(String colName)
	{
		for (int i=0; i<COLUMN_HEADERS.length; i++)
		{
			if (COLUMN_HEADERS[i][COL_NAME].equals(colName))
				return (String)COLUMN_HEADERS[i][COL_TOOLTIP];
		}
		return "";
	}

	@Override
	public String getSqlDataType(String colName)
	{
		for (int i=0; i<COLUMN_HEADERS.length; i++)
		{
			if (COLUMN_HEADERS[i][COL_NAME].equals(colName))
				return (String)COLUMN_HEADERS[i][COL_SQLTYPE];
		}
		return "";
	}
	@Override
	public String getSqlDataType(int colIndex)
	{
		return (String)COLUMN_HEADERS[colIndex][COL_SQLTYPE];
	}

	@Override
	public String getTabLabel()
	{
		return "SQL-Server Config";
	}

	@Override
	public String getCellToolTip(int mrow, int mcol)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("<b>Description:  </b>").append(getValueAt(mrow, findColumn(MySqlConfig.DESCRIPTION))) .append("<br>");
		sb.append("<b>Comment:      </b>").append(getValueAt(mrow, findColumn(MySqlConfig.COMMENT)))     .append("<br>");
		sb.append("<b>Section Name: </b>").append(getValueAt(mrow, findColumn(MySqlConfig.SECTION_NAME))).append("<br>");
		sb.append("<b>Config Name:  </b>").append(getValueAt(mrow, findColumn(MySqlConfig.CONFIG_NAME))) .append("<br>");
		sb.append("<b>Run Value:    </b>").append(getValueAt(mrow, findColumn(MySqlConfig.RUN_VALUE)))   .append("<br>");
		if (Boolean.TRUE.equals(getValueAt(mrow, findColumn(MySqlConfig.PENDING))))
		{
			sb.append("<br>");
			sb.append("<b>NOTE: SQL-Server Needs to be rebooted for this option to take effect.</b>").append("<br>");
		}
		sb.append("</html>");
		return sb.toString();
	}

	@Override
	public String getColName_pending()
	{
		return PENDING;
	}

	@Override
	public String getColName_sectionName()
	{
		return SECTION_NAME;
	}

	@Override
	public String getColName_configName()
	{
		return CONFIG_NAME;
	}

	@Override
	public String getColName_nonDefault()
	{
		return NON_DEFAULT;
	}
}
