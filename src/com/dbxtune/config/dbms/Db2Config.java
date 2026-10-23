/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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
package com.dbxtune.config.dbms;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.Db2Tune;
import com.dbxtune.config.dbms.DbmsConfigIssue.Severity;
import com.dbxtune.pcs.MonRecordingInfo;
import com.dbxtune.pcs.PersistWriterJdbc;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfoDb2;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;
import com.dbxtune.utils.Ver;


/**
 * The Db2 configuration: the database configuration (DB CFG: SYSIBMADM.DBCFG), the instance configuration (DBM CFG: SYSIBMADM.DBMCFG)
 * and the registry variables (db2set: SYSIBMADM.REG_VARIABLES). All three are readable by PUBLIC (the monitoring login needs no grant).
 * <p>
 * Db2 has no "default value" of a parameter, so 'NonDefault' compares with a database and an instance that was just created
 * (Db2 11.5.8, the ibmcom/db2 image), see {@link #DEFAULTS}. AUTOMATIC values (that Db2 sizes by itself) are never NonDefault.
 * <p>
 * A row is a parameter in a scope: <code>db.locktimeout</code>, <code>dbm.instance_memory</code>, <code>reg.DB2COMM</code> (the key of the map).
 */
public class Db2Config
extends DbmsConfigAbstract
{
	private static final long serialVersionUID = 1L;

	/** Log4j logging. */
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/** Instance variable */
	private boolean   _offline   = false;
	private boolean   _hasGui    = false;
	private Timestamp _timestamp = null;

	public static final String NON_DEFAULT     = "NonDefault";
	public static final String SECTION_NAME    = "Section";
	public static final String SCOPE           = "Scope";
	public static final String CONFIG_NAME     = "ParameterName";
	public static final String CONFIG_VALUE    = "Value";
	public static final String PENDING         = "Pending";
	public static final String DEFERRED_VALUE  = "DeferredValue";
	public static final String VALUE_FLAGS     = "ValueFlags";
	public static final String DEFAULT_VALUE   = "DefaultValue";
	public static final String DATA_TYPE       = "DataType";
	public static final String DESCRIPTION     = "Description";

	public static final String SCOPE_DB        = "DB";
	public static final String SCOPE_DBM       = "DBM";
	public static final String SCOPE_REGISTRY  = "REGISTRY";

	/** Registry variables that the installation sets: they are never NonDefault (a comma separated list of names) */
	public static final String PROPKEY_installationRegistry = "Db2Config.nonDefault.registry.installation";
	public static final String DEFAULT_installationRegistry = "DB2COMM, DB2SYSTEM, DB2INSTPROF, DB2INSTDEF, DB2ADMINSERVER, DB2PATH, DB2_FED_LIBPATH, DB2AUTOSTART";

	/** A check: the total transaction log (primary + secondary) is smaller than this */
	public static final String PROPKEY_check_minTotalLogMb      = "Db2Config.check.minTotalLogMb";
	public static final int    DEFAULT_check_minTotalLogMb      = 1024;

	/** A check: no good database backup in this many days */
	public static final String PROPKEY_check_maxDaysSinceBackup = "Db2Config.check.maxDaysSinceBackup";
	public static final int    DEFAULT_check_maxDaysSinceBackup = 7;

	/** Parameters that can have a password in them: the value is never stored */
	private static final Set<String> SECRET_PARAMETERS = new HashSet<>();
	static
	{
		SECRET_PARAMETERS.add("db.tsm_password");
		SECRET_PARAMETERS.add("db.encropts");
	}
	private static final String SECRET_VALUE = "*secret*";

	public static final String SECTION_DB_LOGGING   = "DB: Logging and Archiving";
	public static final String SECTION_DB_RECOVERY  = "DB: Backup and Recovery";
	public static final String SECTION_DB_HADR      = "DB: HADR";
	public static final String SECTION_DB_MEMORY    = "DB: Memory";
	public static final String SECTION_DB_LOCKING   = "DB: Locking";
	public static final String SECTION_DB_MONITOR   = "DB: Monitoring";
	public static final String SECTION_DB_AUTOMAINT = "DB: Automatic Maintenance";
	public static final String SECTION_DB_OPTIMIZER = "DB: Optimizer";
	public static final String SECTION_DB_IO        = "DB: I/O";
	public static final String SECTION_DB_COMPAT    = "DB: Compatibility and Defaults";
	public static final String SECTION_DB_WLM       = "DB: Workload Management";
	public static final String SECTION_DB_INFO      = "DB: Information (read only)";
	public static final String SECTION_DB_PURESCALE = "DB: pureScale";
	public static final String SECTION_DB_OTHER     = "DB: Other";
	public static final String SECTION_DBM_MEMORY   = "Instance: Memory";
	public static final String SECTION_DBM_SECURITY = "Instance: Security";
	public static final String SECTION_DBM_DIAG     = "Instance: Diagnostics";
	public static final String SECTION_DBM_AGENTS   = "Instance: Agents and Connections";
	public static final String SECTION_DBM_MONITOR  = "Instance: Monitoring";
	public static final String SECTION_DBM_FCM      = "Instance: FCM and pureScale";
	public static final String SECTION_DBM_OTHER    = "Instance: Other";
	public static final String SECTION_REGISTRY     = "Registry (db2set)";

	private static final int COL_NAME        = 0;
	private static final int COL_EDITABLE    = 1;
	private static final int COL_CLASS       = 2;
	private static final int COL_JDBC_TYPE   = 3;
	private static final int COL_JDBC_LENGTH = 4;
	private static final int COL_JDBC_SCALE  = 5;
	private static final int COL_TOOLTIP     = 6;
	private static Object[][] COLUMN_HEADERS =
	{
	//   ColumnName,       Editable, JTable type,   JDBC Type      JDBC Length JDBC Scale  Tooltip
	//   ----------------- --------- -------------- -------------- ----------- ----------  --------------------------------------------
		{NON_DEFAULT,      false,    Boolean.class, Types.BOOLEAN, -1,         -1,         "<html>The parameter has another value than in a database (or instance) that was just created.<br>Db2 has no default value to compare with, so the defaults are from a new Db2 11.5.8 database and instance (see 'DefaultValue').<br>Not NonDefault:<ul><li>an AUTOMATIC value (Db2 sizes it by itself)</li><li>a parameter that is information (read only), a path, or computed from the hardware</li><li>the registry variables that the installation sets, from the property <code>" + PROPKEY_installationRegistry + "</code></li></ul>A registry variable that is set is otherwise always NonDefault.</html>"},
		{SECTION_NAME,     true,     String .class, Types.VARCHAR, 40,         -1,         "<html>Db2 has no groups for the parameters, so this is from the name: Logging, Memory, Locking, Monitoring, HADR...<br>DB = the database configuration (DB CFG), Instance = the instance configuration (DBM CFG).</html>"},
		{SCOPE,            true,     String .class, Types.VARCHAR, 10,         -1,         "<html>DB = the database configuration (<code>db2 get db cfg for &lt;db&gt;</code>), DBM = the instance configuration (<code>db2 get dbm cfg</code>), REGISTRY = a registry variable (<code>db2set -all</code>).</html>"},
		{CONFIG_NAME,      true,     String .class, Types.VARCHAR, 80,         -1,         "Name of the parameter (as in UPDATE DB CFG / UPDATE DBM CFG), or of the registry variable."},
		{CONFIG_VALUE,     true,     String .class, Types.VARCHAR, 1024,       -1,         "<html>The value the database (or instance) runs with.<br>Sizes are normally in 4 KB pages.</html>"},
		{PENDING,          false,    Boolean.class, Types.BOOLEAN, -1,         -1,         "<html>'DeferredValue' is another value: it takes effect when the database is activated again (or the instance is restarted).<br>Not for an AUTOMATIC value that Db2 is tuning (the deferred value of a self tuning parameter is often a bit different).</html>"},
		{DEFERRED_VALUE,   true,     String .class, Types.VARCHAR, 1024,       -1,         "<html>The value the database (or instance) gets at the next activation (restart). Empty for a registry variable.</html>"},
		{VALUE_FLAGS,      true,     String .class, Types.VARCHAR, 20,         -1,         "<html>AUTOMATIC = Db2 sizes it by itself (self tuning memory, or computed at start). COMPUTED = computed by Db2. NONE = the value is what was set.<br>For a registry variable: where it is set: I = instance, G = global, E = environment, N = database partition.</html>"},
		{DEFAULT_VALUE,    true,     String .class, Types.VARCHAR, 1024,       -1,         "<html>The value in a database (or instance) that was just created (Db2 11.5.8). Empty when there is no default to compare with.</html>"},
		{DATA_TYPE,        true,     String .class, Types.VARCHAR, 30,         -1,         "The data type of the parameter."},
		{DESCRIPTION,      true,     String .class, Types.VARCHAR, 1024,       -1,         "What the parameter does (written by Db2Tune for the most important ones)."},
	};

	private static String GET_CONFIG_OFFLINE_SQL =
		"select * " +
		"from [" + PersistWriterJdbc.getTableName(null, null, PersistWriterJdbc.SESSION_DBMS_CONFIG, null, false) + "] \n" +
		"where [SessionStartTime] = SESSION_START_TIME \n";

	private static String GET_CONFIG_OFFLINE_MAX_SESSION_SQL =
		" (select max([SessionStartTime]) " +
		"  from [" + PersistWriterJdbc.getTableName(null, null, PersistWriterJdbc.SESSION_DBMS_CONFIG, null, false) + "]" +
		" ) ";

	/** key is scope.name (db.locktimeout, dbm.instance_memory, reg.DB2COMM) */
	private HashMap<String,Db2ConfigEntry> _configMap  = null;
	private ArrayList<Db2ConfigEntry>      _configList = null;

	private ArrayList<String>              _configSectionList = null;

	public class Db2ConfigEntry
	implements IDbmsConfigEntry
	{
		@Override public String  getConfigKey()   { return configKey; }
		@Override public String  getConfigValue() { return configValue; }
		@Override public String  getSectionName() { return sectionName; }
		@Override public boolean isPending()      { return isPending; }
		@Override public boolean isNonDefault()   { return isNonDefault; }

		public String  configKey;
		public boolean isNonDefault;
		public String  sectionName;
		public String  scope;
		public String  configName;
		public String  configValue;
		public boolean isPending;
		public String  deferredValue;
		public String  valueFlags;
		public String  defaultValue;
		public String  dataType;
		public String  description;
	}

	public Db2Config()
	{
		DbmsConfigTextManager.clear();
		Db2ConfigText.createAndRegisterAllInstances();
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

	/** get the Map */
	@Override
	public Map<String, ? extends IDbmsConfigEntry> getDbmsConfigMap()
	{
		return _configMap;
	}

	/** check if the Config is initialized or not */
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
	throws SQLException
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
	throws SQLException
	{
		if (conn == null)
			return;

		reset();

		_configMap         = new HashMap<String,Db2ConfigEntry>();
		_configList        = new ArrayList<Db2ConfigEntry>();
		_configSectionList = new ArrayList<String>();

		try { setDbmsServerName(conn.getDbmsServerName()); } catch (SQLException ex) { setDbmsServerName(ex.getMessage()); };
		try { setDbmsVersionStr(conn.getDbmsVersionStr()); } catch (SQLException ex) { setDbmsVersionStr(ex.getMessage()); };

		try { setLastUsedUrl( conn.getMetaData().getURL() ); } catch(SQLException ignore) { }
		setLastUsedConnProp(conn.getConnPropOrDefault());

		String sql = "";
		try
		{
			Statement stmt = conn.createStatement();
			stmt.setQueryTimeout(10);

			if ( ! _offline )
			{
				//-------------------------------------------------------
				// ONLINE: 1 - The time
				//-------------------------------------------------------
				sql = "select CURRENT TIMESTAMP from SYSIBM.SYSDUMMY1";
				try (ResultSet rs = stmt.executeQuery(sql))
				{
					while (rs.next())
						_timestamp = rs.getTimestamp(1);
				}

				//-------------------------------------------------------
				// ONLINE: 2 - The database and the instance configuration
				//   DBCFG has one row per member (pureScale): ours. VALUE is the value now, DEFERRED_VALUE at the next activation (restart)
				//-------------------------------------------------------
				sql = ""
					+ "select 'DB' as SCOPE, NAME, VALUE, VALUE_FLAGS, DEFERRED_VALUE, DEFERRED_VALUE_FLAGS, DATATYPE \n"
					+ "from SYSIBMADM.DBCFG \n"
					+ "where MEMBER = CURRENT MEMBER \n"
					+ "union all \n"
					+ "select 'DBM', NAME, VALUE, VALUE_FLAGS, DEFERRED_VALUE, DEFERRED_VALUE_FLAGS, DATATYPE \n"
					+ "from SYSIBMADM.DBMCFG \n"
					+ "order by 1, 2";
				try (ResultSet rs = stmt.executeQuery(sql))
				{
					while (rs.next())
					{
						Db2ConfigEntry entry = new Db2ConfigEntry();
						entry.scope         = rs.getString(1);
						entry.configName    = rs.getString(2);
						entry.configValue   = rs.getString(3);
						entry.valueFlags    = rs.getString(4);
						entry.deferredValue = rs.getString(5);
						String deferredFlags= rs.getString(6);
						entry.dataType      = rs.getString(7);

						if (entry.configName == null)
							continue;

						String prefix = SCOPE_DB.equals(entry.scope) ? "db." : "dbm.";
						entry.configKey = prefix + entry.configName.toLowerCase();

						// Never store a password
						if (SECRET_PARAMETERS.contains(entry.configKey))
						{
							if (StringUtil.hasValue(entry.configValue))   entry.configValue   = SECRET_VALUE;
							if (StringUtil.hasValue(entry.deferredValue)) entry.deferredValue = SECRET_VALUE;
						}

						boolean isAuto = "AUTOMATIC".equals(entry.valueFlags) || "COMPUTED".equals(entry.valueFlags);

						// Pending: the value at the next activation is another one (a self tuning value moves all the time, so only when the "kind" changes there)
						boolean isDeferredAuto = "AUTOMATIC".equals(deferredFlags) || "COMPUTED".equals(deferredFlags);
						if (isAuto || isDeferredAuto)
							entry.isPending = isAuto != isDeferredAuto;
						else
							entry.isPending = ! isSameValue(entry.configValue, entry.deferredValue);

						// NonDefault: another value than in a new database / instance
						entry.defaultValue = DEFAULTS.get(entry.configKey);
						entry.isNonDefault = ! isAuto && entry.defaultValue != null && ! isSameValue(entry.configValue, entry.defaultValue) && ! SECRET_VALUE.equals(entry.configValue);

						entry.sectionName = SCOPE_DB.equals(entry.scope) ? getDbSection(entry.configName.toLowerCase()) : getDbmSection(entry.configName.toLowerCase());
						entry.description = DESCRIPTIONS.get(entry.configKey);

						addEntry(entry);
					}
				}

				//-------------------------------------------------------
				// ONLINE: 3 - The registry variables (only the ones that are set). Can fail without the config: then there are no registry rows
				//-------------------------------------------------------
				Set<String> installationRegistry = new HashSet<>();
				for (String name : StringUtil.commaStrToSet(Configuration.getCombinedConfiguration().getProperty(PROPKEY_installationRegistry, DEFAULT_installationRegistry), true))
					installationRegistry.add(name.trim().toUpperCase());

				sql = "select REG_VAR_NAME, REG_VAR_VALUE, LEVEL from SYSIBMADM.REG_VARIABLES where DBPARTITIONNUM = (select min(DBPARTITIONNUM) from SYSIBMADM.REG_VARIABLES) order by REG_VAR_NAME";
				try (ResultSet rs = stmt.executeQuery(sql))
				{
					while (rs.next())
					{
						Db2ConfigEntry entry = new Db2ConfigEntry();
						entry.scope        = SCOPE_REGISTRY;
						entry.configName   = rs.getString(1);
						entry.configValue  = rs.getString(2);
						entry.valueFlags   = rs.getString(3);
						entry.dataType     = "VARCHAR";

						if (entry.configName == null)
							continue;

						entry.configKey    = "reg." + entry.configName.trim();
						entry.isNonDefault = ! installationRegistry.contains(entry.configName.trim().toUpperCase());
						entry.sectionName  = SECTION_REGISTRY;
						entry.description  = DESCRIPTIONS.get("reg." + entry.configName.trim().toUpperCase());

						addEntry(entry);
					}
				}
				catch (SQLException ex)
				{
					_logger.info("Db2Config: could not read the registry variables (SYSIBMADM.REG_VARIABLES), continuing without them. Caught: " + ex.getMessage().trim());
				}
			}
			else // OFFLINE GET CONFIG
			{
				// For OFFLINE mode: override some the values previously fetched the connection object, with values from the Recorded Database
				MonRecordingInfo recordingInfo = new MonRecordingInfo(conn, null);
				setOfflineRecordingInfo(recordingInfo);
				setDbmsServerName(recordingInfo.getDbmsServerName());
				setDbmsVersionStr(recordingInfo.getDbmsVersionStr());

				// Check if this is correct TYPE
				String expectedType = Db2Tune.APP_NAME;
				String readType     = recordingInfo.getRecDbxAppName();
				if ( ! expectedType.equals(readType) )
				{
					throw new WrongRecordingVendorContent(expectedType, readType);
				}

				sql = GET_CONFIG_OFFLINE_SQL;
				sql = sql.replace("SESSION_START_TIME", ts == null ? GET_CONFIG_OFFLINE_MAX_SESSION_SQL : "'" + ts + "'");

				// replace all '[' and ']' into DBMS Vendor Specific Chars
				sql = conn.quotifySqlString(sql);

				ResultSet rs = stmt.executeQuery(sql);
				ResultSetMetaData rsmd = rs.getMetaData();

				// All columns may not be in the recording (a later version can have more columns), so only read the ones that are there
				// for DBMS portability use "lower-case" names
				List<String> columns = new ArrayList<>();
				for (int c=1; c<=rsmd.getColumnCount(); c++)
					columns.add(rsmd.getColumnLabel(c).toLowerCase());

				while ( rs.next() )
				{
					Db2ConfigEntry entry = new Db2ConfigEntry();

					_timestamp = rs.getTimestamp(1);

					if (columns.contains(NON_DEFAULT   .toLowerCase())) entry.isNonDefault  = rs.getBoolean( 1 + columns.indexOf(NON_DEFAULT   .toLowerCase()) );
					if (columns.contains(SECTION_NAME  .toLowerCase())) entry.sectionName   = rs.getString ( 1 + columns.indexOf(SECTION_NAME  .toLowerCase()) );
					if (columns.contains(SCOPE         .toLowerCase())) entry.scope         = rs.getString ( 1 + columns.indexOf(SCOPE         .toLowerCase()) );
					if (columns.contains(CONFIG_NAME   .toLowerCase())) entry.configName    = rs.getString ( 1 + columns.indexOf(CONFIG_NAME   .toLowerCase()) );
					if (columns.contains(CONFIG_VALUE  .toLowerCase())) entry.configValue   = rs.getString ( 1 + columns.indexOf(CONFIG_VALUE  .toLowerCase()) );
					if (columns.contains(PENDING       .toLowerCase())) entry.isPending     = rs.getBoolean( 1 + columns.indexOf(PENDING       .toLowerCase()) );
					if (columns.contains(DEFERRED_VALUE.toLowerCase())) entry.deferredValue = rs.getString ( 1 + columns.indexOf(DEFERRED_VALUE.toLowerCase()) );
					if (columns.contains(VALUE_FLAGS   .toLowerCase())) entry.valueFlags    = rs.getString ( 1 + columns.indexOf(VALUE_FLAGS   .toLowerCase()) );
					if (columns.contains(DEFAULT_VALUE .toLowerCase())) entry.defaultValue  = rs.getString ( 1 + columns.indexOf(DEFAULT_VALUE .toLowerCase()) );
					if (columns.contains(DATA_TYPE     .toLowerCase())) entry.dataType      = rs.getString ( 1 + columns.indexOf(DATA_TYPE     .toLowerCase()) );
					if (columns.contains(DESCRIPTION   .toLowerCase())) entry.description   = rs.getString ( 1 + columns.indexOf(DESCRIPTION   .toLowerCase()) );

					if (entry.configName == null)
						continue;

					if (entry.scope == null)
						entry.scope = SCOPE_DB;
					entry.configKey = (SCOPE_DB.equals(entry.scope) ? "db." : SCOPE_DBM.equals(entry.scope) ? "dbm." : "reg.") + (SCOPE_REGISTRY.equals(entry.scope) ? entry.configName.trim() : entry.configName.toLowerCase());

					if ("".equals(entry.description))
						entry.description = null;

					addEntry(entry);
				}
				rs.close();
			}

			stmt.close();
		}
		catch (SQLException ex)
		{
			_logger.error("Db2Config:initialize:sql='" + sql + "'", ex);
			if (_hasGui)
				SwingUtils.showErrorMessage("Db2Config - Initialize", "SQL Exception: " + ex.getMessage() + "\n\nThis was found when executing SQL statement:\n\n" + sql, ex);
			_configMap = null;
			_configList = null;
			_configSectionList = null;

			return;
		}

		if ( _offline )
		{
			// Load saved Configuration Issues from the offline database
			getOfflineConfigIssues(conn);
		}
		else
		{
			// Check if we got any strange in the configuration
			// in case it does: report that...
			checkConfig(conn);
		}

		// notify change
		fireTableDataChanged();
	}

	private void addEntry(Db2ConfigEntry entry)
	{
		// Should not happen, but the key is our key
		if (_configMap.containsKey(entry.configKey))
			return;

		_configMap .put(entry.configKey, entry);
		_configList.add(entry);

		// Add it to the section list, if not already there
		if ( ! _configSectionList.contains(entry.sectionName) )
			_configSectionList.add(entry.sectionName);
	}

	/**
	 * The same value? Trimmed, case insensitive, and numbers as numbers ('1.000000e+02' = '100'). NULL and '' are the same.<br>
	 * A state in parentheses at the end is not a part of the value: SELF_TUNING_MEM is 'ON (Active)' or 'ON (Inactive)'
	 */
	private static boolean isSameValue(String a, String b)
	{
		a = a == null ? "" : a.trim().replaceAll("\\s*\\([A-Za-z ]*\\)$", "");
		b = b == null ? "" : b.trim().replaceAll("\\s*\\([A-Za-z ]*\\)$", "");

		if (a.equalsIgnoreCase(b))
			return true;

		try { return Double.parseDouble(a) == Double.parseDouble(b); }
		catch (NumberFormatException ex) { return false; }
	}

	/** Db2 has no groups for the database parameters, so make some from the name */
	private static String getDbSection(String name)
	{
		if (name.startsWith("hadr_") || name.equals("logindexbuild") || name.equals("blocknonlogged"))       return SECTION_DB_HADR;
		if (name.startsWith("cf_"))                                                                          return SECTION_DB_PURESCALE;
		if (name.startsWith("auto_"))                                                                        return SECTION_DB_AUTOMAINT;
		if (name.startsWith("mon_") || name.equals("section_actuals") || name.equals("pl_stack_trace"))      return SECTION_DB_MONITOR;
		if (name.startsWith("wlm_"))                                                                         return SECTION_DB_WLM;

		if (name.startsWith("log") || name.startsWith("num_log") || name.equals("max_log") || name.equals("blk_log_dsk_ful")
		 || name.endsWith("logpath") || name.equals("failarchpath") || name.equals("numarchretry") || name.equals("archretrydelay")
		 || name.equals("softmax") || name.equals("log_appl_info") || name.equals("log_ddl_stmts") || name.equals("log_disk_cap"))
			return SECTION_DB_LOGGING;

		if (name.equals("trackmod") || name.startsWith("tsm_") || name.equals("vendoropt") || name.equals("num_db_backups") || name.equals("rec_his_retentn")
		 || name.equals("autorestart") || name.equals("indexrec") || name.equals("dft_loadrec_ses") || name.equals("ctrl_file_recov_path") || name.startsWith("encr"))
			return SECTION_DB_RECOVERY;

		if (name.equals("database_memory") || name.equals("db_mem_thresh") || name.equals("self_tuning_mem") || name.endsWith("heap") || name.endsWith("heapsz")
		 || name.endsWith("_heap_sz") || name.equals("sortheap") || name.equals("sheapthres_shr") || name.equals("pckcachesz") || name.equals("catalogcache_sz")
		 || name.equals("appl_memory") || name.equals("appgroup_mem_sz") || name.equals("groupheap_ratio") || name.equals("act_sortmem_limit") || name.equals("buffpage"))
			return SECTION_DB_MEMORY;

		if (name.equals("locklist") || name.equals("maxlocks") || name.equals("locktimeout") || name.equals("dlchktime") || name.equals("cur_commit"))
			return SECTION_DB_LOCKING;

		if (name.startsWith("dft_queryopt") || name.equals("dft_degree") || name.startsWith("opt_") || name.equals("num_freqvalues") || name.equals("num_quantiles")
		 || name.equals("dft_refresh_age") || name.equals("dft_mttb_types") || name.equals("avg_appls") || name.equals("stmt_conc") || name.equals("auto_reval")
		 || name.equals("large_aggregation"))
			return SECTION_DB_OPTIMIZER;

		if (name.equals("num_iocleaners") || name.equals("num_ioservers") || name.equals("chngpgs_thresh") || name.equals("dft_prefetch_sz") || name.equals("dft_extent_sz")
		 || name.equals("seqdetect") || name.equals("maxfilop") || name.startsWith("page_age_trgt"))
			return SECTION_DB_IO;

		if (name.endsWith("_compat") || name.equals("decflt_rounding") || name.equals("dec_to_char_fmt") || name.equals("dec_arithmetic") || name.equals("string_units")
		 || name.equals("nchar_mapping") || name.equals("extended_row_sz") || name.equals("dft_table_org") || name.equals("min_dec_div_3") || name.equals("dft_sqlmathwarn")
		 || name.equals("ddl_compression_def") || name.equals("ddl_constraint_def") || name.equals("enable_xmlchar") || name.equals("dft_schemas_dcc") || name.equals("systime_period_adj"))
			return SECTION_DB_COMPAT;

		if (name.equals("codepage") || name.equals("codeset") || name.equals("country") || name.equals("territory") || name.equals("collate_info") || name.equals("alt_collate")
		 || name.equals("db_collname") || name.equals("database_level") || name.equals("release") || name.equals("database_consistent") || name.endsWith("_pending")
		 || name.equals("log_retain_status") || name.equals("user_exit_status") || name.equals("db_seed") || name.equals("pagesize") || name.equals("numsegs")
		 || name.equals("multipage_alloc") || name.equals("restrict_access") || name.equals("suspend_io") || name.equals("db_update_lvl_state") || name.equals("encrypted_database"))
			return SECTION_DB_INFO;

		return SECTION_DB_OTHER;
	}

	/** Db2 has no groups for the instance parameters, so make some from the name */
	private static String getDbmSection(String name)
	{
		if (name.equals("instance_memory") || name.equals("sheapthres") || name.equals("java_heap_sz") || name.equals("aslheapsz") || name.equals("mon_heap_sz")
		 || name.equals("agent_stack_sz") || name.equals("audit_buf_sz") || name.equals("rstrt_light_mem") || name.equals("member_inst_mem"))
			return SECTION_DBM_MEMORY;

		if (name.equals("authentication") || name.startsWith("srvcon_") || name.startsWith("ssl_") || name.endsWith("_group") || name.startsWith("trust_")
		 || name.equals("catalog_noauth") || name.equals("fed_noauth") || name.startsWith("keystore") || name.endsWith("plugin") || name.endsWith("plugin_list")
		 || name.equals("alternate_auth_enc") || name.equals("srv_plugin_mode") || name.equals("local_gssplugin"))
			return SECTION_DBM_SECURITY;

		if (name.startsWith("diag") || name.startsWith("alt_diag") || name.startsWith("cf_diag") || name.equals("notifylevel") || name.equals("health_mon"))
			return SECTION_DBM_DIAG;

		if (name.startsWith("dft_mon_"))
			return SECTION_DBM_MONITOR;

		if (name.equals("max_connections") || name.equals("max_coordagents") || name.equals("num_poolagents") || name.equals("num_initagents") || name.equals("agentpri")
		 || name.equals("intra_parallel") || name.equals("max_querydegree") || name.equals("num_initfenced") || name.equals("fenced_pool") || name.equals("keepfenced")
		 || name.equals("conn_elapse") || name.equals("max_connretries") || name.equals("svcename") || name.equals("rqrioblk") || name.startsWith("wlm_disp"))
			return SECTION_DBM_AGENTS;

		if (name.startsWith("fcm_") || name.startsWith("cf_"))
			return SECTION_DBM_FCM;

		return SECTION_DBM_OTHER;
	}

	/**
	 * The values in a new database (DB CFG) and a new instance (DBM CFG): Db2 11.5.8 from the ibmcom/db2 image (CREATE DATABASE, AUTOCONFIGURE as default).<br>
	 * Without the parameters that are AUTOMATIC there, information (read only), paths, or computed from the hardware.
	 */
	private static final Map<String, String> DEFAULTS = new HashMap<>();
	static
	{
		DEFAULTS.put("db.act_sortmem_limit",      "100");
		DEFAULTS.put("db.app_ctl_heap_sz",        "256");
		DEFAULTS.put("db.appgroup_mem_sz",        "20000");
		DEFAULTS.put("db.archretrydelay",         "20");
		DEFAULTS.put("db.authn_cache_duration",   "3");
		DEFAULTS.put("db.authn_cache_users",      "0");
		DEFAULTS.put("db.auto_cg_stats",          "OFF");
		DEFAULTS.put("db.auto_db_backup",         "OFF");
		DEFAULTS.put("db.auto_del_rec_obj",       "OFF");
		DEFAULTS.put("db.auto_maint",             "ON");
		DEFAULTS.put("db.auto_reorg",             "OFF");
		DEFAULTS.put("db.auto_reval",             "DEFERRED");
		DEFAULTS.put("db.auto_runstats",          "ON");
		DEFAULTS.put("db.auto_sampling",          "ON");
		DEFAULTS.put("db.auto_stats_views",       "OFF");
		DEFAULTS.put("db.auto_stmt_stats",        "ON");
		DEFAULTS.put("db.auto_tbl_maint",         "ON");
		DEFAULTS.put("db.autorestart",            "ON");
		DEFAULTS.put("db.blk_log_dsk_ful",        "NO");
		DEFAULTS.put("db.blocknonlogged",         "NO");
		DEFAULTS.put("db.catalogcache_sz",        "300");
		DEFAULTS.put("db.cf_self_tuning_mem",     "OFF");
		DEFAULTS.put("db.chngpgs_thresh",         "80");
		DEFAULTS.put("db.connect_proc",           "");
		DEFAULTS.put("db.ctrl_file_recov_path",   "");
		DEFAULTS.put("db.cur_commit",             "ON");
		DEFAULTS.put("db.db_mem_thresh",          "100");
		DEFAULTS.put("db.dec_arithmetic",         "");
		DEFAULTS.put("db.dec_to_char_fmt",        "NEW");
		DEFAULTS.put("db.dft_degree",             "1");
		DEFAULTS.put("db.dft_loadrec_ses",        "1");
		DEFAULTS.put("db.dft_mttb_types",         "SYSTEM");
		DEFAULTS.put("db.dft_queryopt",           "5");
		DEFAULTS.put("db.dft_refresh_age",        "0");
		DEFAULTS.put("db.dft_schemas_dcc",        "NO");
		DEFAULTS.put("db.dft_sqlmathwarn",        "NO");
		DEFAULTS.put("db.discover_db",            "ENABLE");
		DEFAULTS.put("db.dlchktime",              "10000");
		DEFAULTS.put("db.enable_xmlchar",         "YES");
		DEFAULTS.put("db.encrlib",                "");
		DEFAULTS.put("db.encropts",               "");
		DEFAULTS.put("db.extbl_strict_io",        "NO");
		DEFAULTS.put("db.failarchpath",           "");
		DEFAULTS.put("db.groupheap_ratio",        "70");
		DEFAULTS.put("db.hadr_local_host",        "");
		DEFAULTS.put("db.hadr_local_svc",         "");
		DEFAULTS.put("db.hadr_peer_window",       "0");
		DEFAULTS.put("db.hadr_remote_host",       "");
		DEFAULTS.put("db.hadr_remote_inst",       "");
		DEFAULTS.put("db.hadr_remote_svc",        "");
		DEFAULTS.put("db.hadr_replay_delay",      "0");
		DEFAULTS.put("db.hadr_ssl_host_val",      "NO");
		DEFAULTS.put("db.hadr_ssl_label",         "");
		DEFAULTS.put("db.hadr_syncmode",          "NEARSYNC");
		DEFAULTS.put("db.hadr_target_list",       "");
		DEFAULTS.put("db.hadr_timeout",           "120");
		DEFAULTS.put("db.indexrec",               "SYSTEM");
		DEFAULTS.put("db.large_aggregation",      "NO");
		DEFAULTS.put("db.locktimeout",            "-1");
		DEFAULTS.put("db.log_appl_info",          "NO");
		DEFAULTS.put("db.log_ddl_stmts",          "NO");
		DEFAULTS.put("db.log_disk_cap",           "0");
		DEFAULTS.put("db.logarchcompr1",          "OFF");
		DEFAULTS.put("db.logarchcompr2",          "OFF");
		DEFAULTS.put("db.logarchmeth1",           "OFF");
		DEFAULTS.put("db.logarchmeth2",           "OFF");
		DEFAULTS.put("db.logarchopt1",            "");
		DEFAULTS.put("db.logarchopt2",            "");
		DEFAULTS.put("db.logbufsz",               "2150");
		DEFAULTS.put("db.logfilsiz",              "1024");
		DEFAULTS.put("db.logindexbuild",          "OFF");
		DEFAULTS.put("db.logprimary",             "13");
		DEFAULTS.put("db.logsecond",              "12");
		DEFAULTS.put("db.max_log",                "0");
		DEFAULTS.put("db.maxfilop",               "61440");
		DEFAULTS.put("db.min_dec_div_3",          "NO");
		DEFAULTS.put("db.mirrorlogpath",          "");
		DEFAULTS.put("db.mon_act_metrics",        "BASE");
		DEFAULTS.put("db.mon_deadlock",           "WITHOUT_HIST");
		DEFAULTS.put("db.mon_lck_msg_lvl",        "1");
		DEFAULTS.put("db.mon_locktimeout",        "NONE");
		DEFAULTS.put("db.mon_lockwait",           "NONE");
		DEFAULTS.put("db.mon_lw_thresh",          "5000000");
		DEFAULTS.put("db.mon_obj_metrics",        "EXTENDED");
		DEFAULTS.put("db.mon_pkglist_sz",         "32");
		DEFAULTS.put("db.mon_req_metrics",        "BASE");
		DEFAULTS.put("db.mon_rtn_data",           "NONE");
		DEFAULTS.put("db.mon_rtn_execlist",       "OFF");
		DEFAULTS.put("db.mon_uow_data",           "NONE");
		DEFAULTS.put("db.mon_uow_execlist",       "OFF");
		DEFAULTS.put("db.mon_uow_pkglist",        "OFF");
		DEFAULTS.put("db.newlogpath",             "");
		DEFAULTS.put("db.num_db_backups",         "12");
		DEFAULTS.put("db.num_freqvalues",         "10");
		DEFAULTS.put("db.num_log_span",           "0");
		DEFAULTS.put("db.num_quantiles",          "20");
		DEFAULTS.put("db.numarchretry",           "5");
		DEFAULTS.put("db.opt_buffpage",           "0");
		DEFAULTS.put("db.opt_direct_wrkld",       "NO");
		DEFAULTS.put("db.opt_locklist",           "0");
		DEFAULTS.put("db.opt_maxlocks",           "0");
		DEFAULTS.put("db.opt_sortheap",           "0");
		DEFAULTS.put("db.overflowlogpath",        "");
		DEFAULTS.put("db.pl_stack_trace",         "NONE");
		DEFAULTS.put("db.rec_his_retentn",        "90");
		DEFAULTS.put("db.repl_site_id",           "0");
		DEFAULTS.put("db.section_actuals",        "NONE");
		DEFAULTS.put("db.self_tuning_mem",        "ON");
		DEFAULTS.put("db.seqdetect",              "YES");
		DEFAULTS.put("db.softmax",                "0");
		DEFAULTS.put("db.stmt_conc",              "OFF");
		DEFAULTS.put("db.systime_period_adj",     "NO");
		DEFAULTS.put("db.trackmod",               "OFF");
		DEFAULTS.put("db.tsm_mgmtclass",          "");
		DEFAULTS.put("db.tsm_nodename",           "");
		DEFAULTS.put("db.tsm_owner",              "");
		DEFAULTS.put("db.tsm_password",           "");
		DEFAULTS.put("db.vendoropt",              "");
		DEFAULTS.put("db.wlm_admission_ctrl",     "NO");
		DEFAULTS.put("db.wlm_collect_int",        "0");
		DEFAULTS.put("db.wlm_cpu_limit",          "0");
		DEFAULTS.put("db.wlm_cpu_share_mode",     "HARD");
		DEFAULTS.put("db.wlm_cpu_shares",         "1000");
		DEFAULTS.put("dbm.agent_stack_sz",        "1024");
		DEFAULTS.put("dbm.agentpri",              "-1");    // (GET DBM CFG says SYSTEM)
		DEFAULTS.put("dbm.alt_diagpath",          "");
		DEFAULTS.put("dbm.alternate_auth_enc",    "NOT_SPECIFIED");
		DEFAULTS.put("dbm.aslheapsz",             "15");
		DEFAULTS.put("dbm.audit_buf_sz",          "0");
		DEFAULTS.put("dbm.authentication",        "SERVER");
		DEFAULTS.put("dbm.catalog_noauth",        "NO");
		DEFAULTS.put("dbm.clnt_krb_plugin",       "");
		DEFAULTS.put("dbm.clnt_pw_plugin",        "");
		DEFAULTS.put("dbm.comm_exit_list",        "");
		DEFAULTS.put("dbm.conn_elapse",           "10");
		DEFAULTS.put("dbm.dft_account_str",       "");
		DEFAULTS.put("dbm.dft_mon_bufpool",       "OFF");
		DEFAULTS.put("dbm.dft_mon_lock",          "OFF");
		DEFAULTS.put("dbm.dft_mon_sort",          "OFF");
		DEFAULTS.put("dbm.dft_mon_stmt",          "OFF");
		DEFAULTS.put("dbm.dft_mon_table",         "OFF");
		DEFAULTS.put("dbm.dft_mon_timestamp",     "ON");
		DEFAULTS.put("dbm.dft_mon_uow",           "OFF");
		DEFAULTS.put("dbm.diaglevel",             "3");
		DEFAULTS.put("dbm.diagsize",              "0");
		DEFAULTS.put("dbm.dir_cache",             "YES");
		DEFAULTS.put("dbm.discover",              "SEARCH");
		DEFAULTS.put("dbm.discover_inst",         "ENABLE");
		DEFAULTS.put("dbm.fcm_buffer_size",       "32768");
		DEFAULTS.put("dbm.fed_noauth",            "NO");
		DEFAULTS.put("dbm.federated",             "YES");   // (the ibmcom/db2 image sets it, a plain db2icrt has NO)
		DEFAULTS.put("dbm.federated_async",       "0");
		DEFAULTS.put("dbm.group_plugin",          "");
		DEFAULTS.put("dbm.health_mon",            "OFF");
		DEFAULTS.put("dbm.indexrec",              "RESTART");
		DEFAULTS.put("dbm.intra_parallel",        "NO");
		DEFAULTS.put("dbm.java_heap_sz",          "65536");
		DEFAULTS.put("dbm.keepfenced",            "YES");
		DEFAULTS.put("dbm.keystore_location",     "");
		DEFAULTS.put("dbm.keystore_type",         "NONE");
		DEFAULTS.put("dbm.local_gssplugin",       "");
		DEFAULTS.put("dbm.max_connretries",       "5");
		DEFAULTS.put("dbm.max_querydegree",       "-1");    // (GET DBM CFG says ANY)
		DEFAULTS.put("dbm.max_time_diff",         "60");
		DEFAULTS.put("dbm.multipartsizemb",       "100");
		DEFAULTS.put("dbm.notifylevel",           "3");
		DEFAULTS.put("dbm.num_initagents",        "0");
		DEFAULTS.put("dbm.num_initfenced",        "0");
		DEFAULTS.put("dbm.numdb",                 "32");
		DEFAULTS.put("dbm.python_path",           "");
		DEFAULTS.put("dbm.r_path",                "");
		DEFAULTS.put("dbm.resync_interval",       "180");
		DEFAULTS.put("dbm.rqrioblk",              "65535");
		DEFAULTS.put("dbm.sheapthres",            "0");
		DEFAULTS.put("dbm.spm_log_file_sz",       "256");
		DEFAULTS.put("dbm.spm_log_path",          "");
		DEFAULTS.put("dbm.spm_max_resync",        "20");
		DEFAULTS.put("dbm.srv_plugin_mode",       "UNFENCED");
		DEFAULTS.put("dbm.srvcon_auth",           "NOT_SPECIFIED");
		DEFAULTS.put("dbm.srvcon_gssplugin_list", "");
		DEFAULTS.put("dbm.srvcon_pw_plugin",      "");
		DEFAULTS.put("dbm.ssl_cipherspecs",       "");
		DEFAULTS.put("dbm.ssl_clnt_keydb",        "");
		DEFAULTS.put("dbm.ssl_clnt_stash",        "");
		DEFAULTS.put("dbm.ssl_svr_keydb",         "");
		DEFAULTS.put("dbm.ssl_svr_label",         "");
		DEFAULTS.put("dbm.ssl_svr_stash",         "");
		DEFAULTS.put("dbm.ssl_versions",          "");
		DEFAULTS.put("dbm.start_stop_time",       "10");
		DEFAULTS.put("dbm.sysctrl_group",         "");
		DEFAULTS.put("dbm.sysmaint_group",        "");
		DEFAULTS.put("dbm.sysmon_group",          "");
		DEFAULTS.put("dbm.tm_database",           "1ST_CONN");
		DEFAULTS.put("dbm.tp_mon_name",           "");
		DEFAULTS.put("dbm.trust_allclnts",        "YES");
		DEFAULTS.put("dbm.trust_clntauth",        "CLIENT");
		DEFAULTS.put("dbm.util_impact_lim",       "10");
		DEFAULTS.put("dbm.wlm_disp_cpu_shares",   "NO");
		DEFAULTS.put("dbm.wlm_disp_min_util",     "5");
		DEFAULTS.put("dbm.wlm_dispatcher",        "NO");
	}

	/**
	 * Descriptions of the most important parameters (Db2 has none in the catalog). Key is scope.name
	 */
	private static final Map<String, String> DESCRIPTIONS = new HashMap<>();
	static
	{
		// Logging
		DESCRIPTIONS.put("db.logarchmeth1",     "Where filled log files are archived: OFF = circular logging (no online backup, no roll forward: a restore can only go back to the last backup), LOGRETAIN (kept, never archived or removed), DISK:<path>, TSM, VENDOR:<library>.");
		DESCRIPTIONS.put("db.logarchmeth2",     "A second place to archive the log files to (the same files go to both).");
		DESCRIPTIONS.put("db.failarchpath",     "Where log files are put when the archive (LOGARCHMETH1/2) can not be reached. Without it, the files stay in the log path until the archive works again (the log path can fill up).");
		DESCRIPTIONS.put("db.logfilsiz",        "The size of one log file, in 4 KB pages. The total log is (LOGPRIMARY + LOGSECOND) * LOGFILSIZ * 4 KB.");
		DESCRIPTIONS.put("db.logprimary",       "Number of log files that are created when the database is activated (always there).");
		DESCRIPTIONS.put("db.logsecond",        "Number of log files that can be created when the primary ones are full. -1 = no limit (infinite logging: a long transaction reads its log back from the archive).");
		DESCRIPTIONS.put("db.logbufsz",         "The log buffer, in 4 KB pages. Log records are collected here before they are written to the log files.");
		DESCRIPTIONS.put("db.num_log_span",     "How many log files one transaction may span. 0 = no limit: one open transaction can hold the whole log (SQL0964C for everyone).");
		DESCRIPTIONS.put("db.max_log",          "How much of the log (in percent) one transaction may use. 0 = no limit.");
		DESCRIPTIONS.put("db.blk_log_dsk_ful",  "YES: when the log disk is full, Db2 waits (and retries every 5 minutes) instead of failing the transactions.");
		DESCRIPTIONS.put("db.mirrorlogpath",    "A second copy of the active log files, on another disk (if the disk of the log path is lost, the log is not).");
		DESCRIPTIONS.put("db.newlogpath",       "A new path for the log files: it is used after the next activation (the database is then using 'logpath' until then).");
		DESCRIPTIONS.put("db.softmax",          "(Deprecated) How much log can be written before a soft checkpoint, in percent of a log file. 0 = Db2 decides (PAGE_AGE_TRGT_MCR).");
		DESCRIPTIONS.put("db.log_disk_cap",     "Max size of the active log path, in MB (Db2 11.5.4 and above). 0 = no limit.");
		// Backup and recovery
		DESCRIPTIONS.put("db.trackmod",         "YES: Db2 tracks which pages are changed, so incremental (and delta) backups can be taken. Needed for BACKUP ... INCREMENTAL.");
		DESCRIPTIONS.put("db.num_db_backups",   "How many full database backups are kept in the recovery history (with AUTO_DEL_REC_OBJ ON also the backup images and the old log files are removed).");
		DESCRIPTIONS.put("db.rec_his_retentn",  "Days the entries of the recovery history file are kept. -1 = for ever (the file grows, and reading it gets slower).");
		DESCRIPTIONS.put("db.auto_del_rec_obj", "ON: when old recovery history entries are pruned, the backup images, load copies and archived log files of them are also removed.");
		DESCRIPTIONS.put("db.autorestart",      "ON: a crash recovery is done automatically at the first connect after a crash.");
		DESCRIPTIONS.put("db.tsm_password",     "The password for TSM (the value is never stored by Db2Tune).");
		// HADR
		DESCRIPTIONS.put("db.hadr_syncmode",    "When a commit on the primary is complete: SYNC (the log is on disk on the standby), NEARSYNC (in the memory of the standby), ASYNC (sent), SUPERASYNC (not waited for at all). ASYNC and SUPERASYNC can lose committed transactions at a failover.");
		DESCRIPTIONS.put("db.hadr_peer_window", "Seconds the primary keeps behaving as in PEER (commits wait) after the connection to the standby is lost. 0 = the primary goes on alone at once (a failover in that time can lose transactions).");
		DESCRIPTIONS.put("db.hadr_timeout",     "Seconds without contact before the HADR connection is seen as broken.");
		DESCRIPTIONS.put("db.hadr_target_list", "The standbys (host:port), the first is the principal standby. Needed for more than one standby.");
		DESCRIPTIONS.put("db.hadr_replay_delay","Seconds the standby waits before it replays a transaction (a delayed standby protects against mistakes). Only with SUPERASYNC.");
		DESCRIPTIONS.put("db.hadr_spool_limit", "How much log the standby can spool to disk when its replay is behind (in 4 KB pages). 0 = none (the primary waits), -1 = no limit.");
		DESCRIPTIONS.put("db.logindexbuild",    "ON: index creates and rebuilds are logged (needed under HADR, otherwise the indexes are invalid on the standby after a takeover).");
		DESCRIPTIONS.put("db.blocknonlogged",   "YES: operations that are not logged (NOT LOGGED INITIALLY, LOAD without COPY YES) are refused. Should be YES under HADR (they are not sent to the standby).");
		// Memory
		DESCRIPTIONS.put("db.self_tuning_mem",  "ON: the self tuning memory manager (STMM) moves memory between the buffer pools, the package cache, the lock list and the sort heap (for the ones that are AUTOMATIC).");
		DESCRIPTIONS.put("db.database_memory",  "The shared memory of the database (4 KB pages). AUTOMATIC: STMM sizes it.");
		DESCRIPTIONS.put("db.locklist",         "The memory for locks, in 4 KB pages. Too small: lock escalations (row locks replaced by table locks, more waiting).");
		DESCRIPTIONS.put("db.maxlocks",         "How much of the lock list (in percent) one application may use before its locks are escalated.");
		DESCRIPTIONS.put("db.pckcachesz",       "The package cache (the compiled statements), in 4 KB pages. Too small: statements are compiled again and again (see PKG_CACHE_NUM_OVERFLOWS in the Summary).");
		DESCRIPTIONS.put("db.catalogcache_sz",  "The catalog cache, in 4 KB pages.");
		DESCRIPTIONS.put("db.sortheap",         "Memory for one sort (or hash join), in 4 KB pages. Too small: sorts spill to the temporary table space (sort overflows).");
		DESCRIPTIONS.put("db.sheapthres_shr",   "The total memory for sorts in the database, in 4 KB pages.");
		DESCRIPTIONS.put("db.dbheap",           "The database heap (control blocks, the log buffer...), in 4 KB pages.");
		DESCRIPTIONS.put("db.stmtheap",         "Memory to compile one statement, in 4 KB pages. Too small: SQL0101N or SQL0437W (a worse plan).");
		DESCRIPTIONS.put("db.util_heap_sz",     "Memory for BACKUP, RESTORE, LOAD..., in 4 KB pages.");
		DESCRIPTIONS.put("db.appl_memory",      "The memory for all applications of the database, in 4 KB pages.");
		// Locking
		DESCRIPTIONS.put("db.locktimeout",      "Seconds a lock request waits before it fails with SQL0911N reason 68. -1 = waits for ever (a forgotten open transaction can hang the others until it is gone).");
		DESCRIPTIONS.put("db.dlchktime",        "How often the deadlock detector runs, in milliseconds.");
		DESCRIPTIONS.put("db.cur_commit",       "ON: a reader under Cursor Stability gets the last committed version of a row that is being changed, instead of waiting for the lock. DISABLED: readers wait for writers.");
		// Monitoring
		DESCRIPTIONS.put("db.mon_req_metrics",  "Request metrics (the time spent, rows read... per connection, unit of work and database). NONE: most of what Db2Tune shows is 0.");
		DESCRIPTIONS.put("db.mon_act_metrics",  "Activity metrics (per statement: the package cache and running activities). NONE: the Statements tab is mostly 0.");
		DESCRIPTIONS.put("db.mon_obj_metrics",  "Object metrics (per table space, buffer pool, table, index). NONE: the I/O per table space and buffer pool is 0.");
		DESCRIPTIONS.put("db.mon_deadlock",     "What the deadlock event monitor (DB2DETAILDEADLOCK or a locking event monitor) captures for a deadlock.");
		DESCRIPTIONS.put("db.mon_lockwait",     "Capture lock waits that are longer than MON_LW_THRESH (needs a locking event monitor).");
		DESCRIPTIONS.put("db.mon_locktimeout",  "Capture lock timeouts (needs a locking event monitor).");
		// Automatic maintenance
		DESCRIPTIONS.put("db.auto_maint",       "The parent switch of all automatic maintenance (OFF turns off the ones below too).");
		DESCRIPTIONS.put("db.auto_tbl_maint",   "The parent switch of automatic table maintenance (RUNSTATS, REORG).");
		DESCRIPTIONS.put("db.auto_runstats",    "ON: statistics are collected automatically when a table has changed enough. OFF: something else must run RUNSTATS, or the optimizer gets old statistics.");
		DESCRIPTIONS.put("db.auto_stmt_stats",  "ON: real-time statistics (collected or fabricated at compile time when a table has no or old statistics).");
		DESCRIPTIONS.put("db.auto_reorg",       "ON: tables and indexes are reorganized automatically (in the maintenance window).");
		DESCRIPTIONS.put("db.auto_db_backup",   "ON: Db2 takes backups automatically (in the maintenance window).");
		// Optimizer / IO / other
		DESCRIPTIONS.put("db.dft_queryopt",     "The default optimization class (0-9). 5 is the default.");
		DESCRIPTIONS.put("db.dft_degree",       "The default degree of intra-partition parallelism (1 = none, ANY = Db2 decides; needs INTRA_PARALLEL).");
		DESCRIPTIONS.put("db.num_iocleaners",   "Page cleaners: write changed pages from the buffer pools to disk.");
		DESCRIPTIONS.put("db.num_ioservers",    "Prefetchers: read pages into the buffer pools ahead of a scan.");
		DESCRIPTIONS.put("db.maxappls",         "The most applications that can be connected to the database at the same time.");
		DESCRIPTIONS.put("db.dft_table_org",    "ROW or COLUMN: the organization of a new table that does not say (COLUMN = BLU).");
		// Instance
		DESCRIPTIONS.put("dbm.instance_memory", "The most memory the instance (all its databases) may use, in 4 KB pages. AUTOMATIC: Db2 computes it from the RAM.");
		DESCRIPTIONS.put("dbm.sheapthres",      "Instance wide limit for private sort memory (0 = sorts use the shared SHEAPTHRES_SHR of each database: the normal way).");
		DESCRIPTIONS.put("dbm.authentication",  "Where users are authenticated: SERVER (the Db2 server checks the password), SERVER_ENCRYPT, CLIENT (the client is trusted: anyone with a client can say who they are), KERBEROS...");
		DESCRIPTIONS.put("dbm.trust_allclnts",  "With AUTHENTICATION CLIENT: YES = all clients are trusted to authenticate users.");
		DESCRIPTIONS.put("dbm.sysadm_group",    "The OS group whose members have SYSADM (all authority on the instance).");
		DESCRIPTIONS.put("dbm.sysmon_group",    "The OS group whose members have SYSMON (can use the snapshot monitor and db2pd, no access to data).");
		DESCRIPTIONS.put("dbm.diaglevel",       "How much is written to db2diag.log: 0 none, 1 severe, 2 errors, 3 warnings (default), 4 informational (a lot: for problem determination only).");
		DESCRIPTIONS.put("dbm.diagsize",        "Max size of db2diag.log and the notification log (MB). 0 = one file that grows for ever.");
		DESCRIPTIONS.put("dbm.intra_parallel",  "YES: one statement can use more than one agent (CPU) in a database partition.");
		DESCRIPTIONS.put("dbm.max_connections", "The most connections to the instance. AUTOMATIC: no limit.");
		DESCRIPTIONS.put("dbm.max_coordagents", "The most coordinating agents (about the most connections that can be active at the same time).");
		DESCRIPTIONS.put("dbm.util_impact_lim", "How much (in percent) throttled utilities (BACKUP, RUNSTATS, REBALANCE...) may slow down the workload.");
		DESCRIPTIONS.put("dbm.health_mon",      "The health monitor (deprecated).");
		DESCRIPTIONS.put("dbm.federated",       "YES: the instance can access remote data sources (federation, nicknames).");
		// Registry
		DESCRIPTIONS.put("reg.DB2COMM",                    "The communication protocols the instance listens on (TCPIP, SSL).");
		DESCRIPTIONS.put("reg.DB2_COMPATIBILITY_VECTOR",   "Compatibility with other databases (ORA = Oracle: PL/SQL, ROWNUM, '' is NULL..., MYS, SYB). Is only used for databases created after it was set.");
		DESCRIPTIONS.put("reg.DB2_WORKLOAD",               "A set of registry settings for a workload (ANALYTICS = BLU / column-organized, SAP, ...).");
		DESCRIPTIONS.put("reg.DB2_SKIPDELETED",            "ON: rows that are deleted but not committed are skipped by scans (no lock wait). Changes the results that readers see.");
		DESCRIPTIONS.put("reg.DB2_SKIPINSERTED",           "ON: rows that are inserted but not committed are skipped by scans (no lock wait).");
		DESCRIPTIONS.put("reg.DB2_EVALUNCOMMITTED",        "ON: predicates are evaluated on uncommitted data, before a lock is requested (fewer lock waits).");
		DESCRIPTIONS.put("reg.DB2_USE_ALTERNATE_PAGE_CLEANING", "ON: page cleaning is driven by the age of the pages and the log (the default from Db2 10.5 with... set by DB2_WORKLOAD).");
	}


	/**
	 * Look for things in the configuration that a DBA normally wants to know about.
	 * <p>
	 * Every check is on its own: if one of them can't get its data (a missing privilege...) the others are still done.
	 */
	@Override
	public void checkConfig(DbxConnection conn)
	{
		if (_configMap == null)
			return;

		String srvName = "-UNKNOWN-";
		try { srvName = conn.getDbmsServerName(); } catch (SQLException ex) { _logger.info("Problems getting Db2 server name. ex=" + ex);}

		String keyPrefix = "DbmsConfigIssue." + srvName + ".db2.";

		DbmsVersionInfoDb2 versionInfo = (conn.getDbmsVersionInfo() instanceof DbmsVersionInfoDb2) ? (DbmsVersionInfoDb2) conn.getDbmsVersionInfo() : null;
		long srvVersion = conn.getDbmsVersionNumber(); // 0 if we don't know

		//-------------------------------------------------------
		// Facts about the database: one row (when it was activated, the last backup, the package cache)
		//-------------------------------------------------------
		Timestamp srvRestart      = null;
		Timestamp lastBackup      = null;
		long      pkgCacheOverflows = 0;
		String sql = "select DB_CONN_TIME, LAST_BACKUP, PKG_CACHE_NUM_OVERFLOWS from table(MON_GET_DATABASE(-1)) t";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while (rs.next())
			{
				srvRestart        = rs.getTimestamp(1);
				lastBackup        = rs.getTimestamp(2);
				pkgCacheOverflows = rs.getLong(3);
			}
		}
		catch (SQLException ex)
		{
			_logger.info("Db2Config.checkConfig(): Skipping the checks for the last backup and the package cache. Caught: " + ex.getMessage().trim());
		}

		String dbName = versionInfo == null ? "" : versionInfo.getDbName();

		//-------------------------------------------------------
		// Logging: circular, LOGRETAIN, no failover archive path
		//-------------------------------------------------------
		String logarchmeth1 = value("db.logarchmeth1");
		String logarchmeth2 = value("db.logarchmeth2");
		boolean isCircular  = "OFF".equalsIgnoreCase(logarchmeth1) && (logarchmeth2 == null || "OFF".equalsIgnoreCase(logarchmeth2));
		boolean isArchiving = ! isCircular && ! "LOGRETAIN".equalsIgnoreCase(logarchmeth1);

		if (isCircular)
		{
			addIssue(srvRestart, keyPrefix + "logarchmeth1.circular", "logarchmeth1", Severity.WARNING,
					"The database '" + dbName + "' uses circular logging (LOGARCHMETH1 = OFF). It can only be restored to the time of the last backup (everything after that is lost), only OFFLINE backups can be taken, and HADR is not possible.",
					"If that is not what you want (for a test database it can be): db2 update db cfg for " + dbName + " using LOGARCHMETH1 DISK:<archive path>; then take a full backup (the database is in BACKUP PENDING until then).");
		}

		if ("LOGRETAIN".equalsIgnoreCase(logarchmeth1))
		{
			addIssue(srvRestart, keyPrefix + "logarchmeth1.logretain", "logarchmeth1", Severity.WARNING,
					"LOGARCHMETH1 is LOGRETAIN: the filled log files are kept in the log path, but never archived and never removed by Db2. The log path fills up unless something else moves the files.",
					"Archive them with Db2: db2 update db cfg for " + dbName + " using LOGARCHMETH1 DISK:<archive path> (or TSM / VENDOR:<library>).");
		}

		if (isArchiving && StringUtil.isNullOrBlank(value("db.failarchpath")))
		{
			addIssue(srvRestart, keyPrefix + "failarchpath.not_set", "failarchpath", Severity.INFO,
					"Log archiving is on, but FAILARCHPATH is not set. If the archive (" + logarchmeth1 + ") can not be reached, the filled log files stay in the active log path until it works again, and the log path can fill up (then SQL0964C or the database stops).",
					"db2 update db cfg for " + dbName + " using FAILARCHPATH <a path on another file system>");
		}

		//-------------------------------------------------------
		// Log archiving fails NOW (also an alarm in 'Transaction Log')
		//-------------------------------------------------------
		if (isArchiving)
		{
			sql = "select ARCHIVE_METHOD1_STATUS, METHOD1_FIRST_FAILURE, ARCHIVE_METHOD2_STATUS, METHOD2_FIRST_FAILURE from table(MON_GET_TRANSACTION_LOG(-1)) t";
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
				{
					boolean fail1 = ! "OFF".equalsIgnoreCase(logarchmeth1) && ! "LOGRETAIN".equalsIgnoreCase(logarchmeth1) && rs.getInt(1) == 0;
					boolean fail2 = logarchmeth2 != null && ! "OFF".equalsIgnoreCase(logarchmeth2) && rs.getInt(3) == 0;
					if (fail1 || fail2)
					{
						addIssue(srvRestart, keyPrefix + "log_archive.failing", "logarchmeth" + (fail1 ? "1" : "2"), Severity.WARNING,
								"Log archiving FAILS (LOGARCHMETH" + (fail1 ? "1" : "2") + " = " + (fail1 ? logarchmeth1 : logarchmeth2) + ", first failure: " + (fail1 ? rs.getString(2) : rs.getString(4)) + "). The filled log files are not archived: the log path fills up, and a roll forward can't get past the gap.",
								"See db2diag.log for why (permissions, a full file system, TSM/vendor errors). When it works again, Db2 archives the files by itself.");
					}
				}
			}
			catch (SQLException ex)
			{
				_logger.info("Db2Config.checkConfig(): Skipping the check for failing log archiving. Caught: " + ex.getMessage().trim());
			}
		}

		//-------------------------------------------------------
		// The size of the log: small, or infinite
		//-------------------------------------------------------
		long logprimary = StringUtil.parseLong(value("db.logprimary"), -1);
		long logsecond  = StringUtil.parseLong(value("db.logsecond") , -2);
		long logfilsiz  = StringUtil.parseLong(value("db.logfilsiz") , -1);
		int  minLogMb   = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_check_minTotalLogMb, DEFAULT_check_minTotalLogMb);
		if (logprimary > 0 && logsecond >= 0 && logfilsiz > 0)
		{
			long totalLogMb = (logprimary + logsecond) * logfilsiz * 4 / 1024;
			if (totalLogMb < minLogMb)
			{
				addIssue(srvRestart, keyPrefix + "log.total_small", "logfilsiz, logprimary, logsecond", Severity.INFO,
						"The transaction log is only " + totalLogMb + " MB ((LOGPRIMARY " + logprimary + " + LOGSECOND " + logsecond + ") * LOGFILSIZ " + logfilsiz + " * 4 KB). One large transaction, or one that is left open, fills it: then all changes fail with SQL0964C. (this check is at " + minLogMb + " MB)",
						"Make the log files bigger (takes effect at the next activation): db2 update db cfg for " + dbName + " using LOGFILSIZ <pages> (for example 16384 = 64 MB). To change the limit of this check: " + PROPKEY_check_minTotalLogMb + "=<MB>");
			}
		}
		if (logsecond == -1)
		{
			addIssue(srvRestart, keyPrefix + "log.infinite", "logsecond", Severity.INFO,
					"Infinite logging is on (LOGSECOND = -1). A transaction can use more log than is in the active log path: a rollback (or crash recovery) of it then reads the log back from the archive, which can take a very long time.",
					"Set a limit, for example with NUM_LOG_SPAN or MAX_LOG, or a LOGSECOND >= 0.");
		}

		//-------------------------------------------------------
		// One transaction can hold the whole log
		//-------------------------------------------------------
		if ("0".equals(value("db.num_log_span")) && "0".equals(value("db.max_log")))
		{
			addIssue(srvRestart, keyPrefix + "log.no_transaction_limit", "num_log_span, max_log", Severity.INFO,
					"NUM_LOG_SPAN and MAX_LOG are 0: there is no limit for one transaction. One open transaction (an application that never commits) can hold the whole log, and then all changes in the database fail with SQL0964C.",
					"Limit one transaction, for example: db2 update db cfg for " + dbName + " using NUM_LOG_SPAN <a bit less than LOGPRIMARY + LOGSECOND>. (Such a transaction is then rolled back instead, with SQL1224N.) See also the alarm 'oldestOpenTranInSec' in the Summary.");
		}

		//-------------------------------------------------------
		if ("NO".equalsIgnoreCase(value("db.blk_log_dsk_ful")))
		{
			addIssue(srvRestart, keyPrefix + "blk_log_dsk_ful.no", "blk_log_dsk_ful", Severity.INFO,
					"BLK_LOG_DSK_FUL is NO: when the file system of the log path is full, the transactions fail (SQL0968C, SQL0964C) instead of waiting until there is room again.",
					"db2 update db cfg for " + dbName + " using BLK_LOG_DSK_FUL YES (Db2 then retries every 5 minutes; the applications wait).");
		}

		//-------------------------------------------------------
		// Locking
		//-------------------------------------------------------
		if ("-1".equals(value("db.locktimeout")))
		{
			addIssue(srvRestart, keyPrefix + "locktimeout.infinite", "locktimeout", Severity.INFO,
					"LOCKTIMEOUT is -1: a lock request waits for ever. One connection that holds a lock and does nothing (an open transaction in an idle application) can hang all the others that need that row or table, until it is gone.",
					"Give it a limit that the applications can handle, for example: db2 update db cfg for " + dbName + " using LOCKTIMEOUT 60 (the waiter then gets SQL0911N reason 68, and can retry). See also the tab 'Active Statements' for who blocks who.");
		}

		if ("DISABLED".equalsIgnoreCase(value("db.cur_commit")))
		{
			addIssue(srvRestart, keyPrefix + "cur_commit.disabled", "cur_commit", Severity.INFO,
					"CUR_COMMIT is DISABLED: readers (Cursor Stability) wait for the locks of writers, instead of reading the last committed version of the row. More lock waits, and more deadlocks.",
					"db2 update db cfg for " + dbName + " using CUR_COMMIT ON (takes effect at the next activation)");
		}

		//-------------------------------------------------------
		// Monitoring metrics NONE: Db2Tune shows zeros
		//-------------------------------------------------------
		List<String> noMetrics = new ArrayList<>();
		for (String name : new String[] {"mon_req_metrics", "mon_act_metrics", "mon_obj_metrics"})
			if ("NONE".equalsIgnoreCase(value("db." + name)))
				noMetrics.add(name.toUpperCase());
		if ( ! noMetrics.isEmpty() )
		{
			addIssue(srvRestart, keyPrefix + "mon_metrics.none", "mon_req_metrics, mon_act_metrics, mon_obj_metrics", Severity.WARNING,
					"The monitoring metrics are NONE for: " + StringUtil.toCommaStr(noMetrics) + ". Db2 does not collect them, so the counters Db2Tune shows from them are 0 (time spent, rows read, statements, I/O per table space...).",
					"db2 update db cfg for " + dbName + " using MON_REQ_METRICS BASE MON_ACT_METRICS BASE MON_OBJ_METRICS EXTENDED (the defaults)");
		}

		//-------------------------------------------------------
		// Automatic statistics
		//-------------------------------------------------------
		List<String> maintOff = new ArrayList<>();
		for (String name : new String[] {"auto_maint", "auto_tbl_maint", "auto_runstats"})
			if ("OFF".equalsIgnoreCase(value("db." + name)))
				maintOff.add(name.toUpperCase());
		if ( ! maintOff.isEmpty() )
		{
			addIssue(srvRestart, keyPrefix + "auto_runstats.off", "auto_maint, auto_tbl_maint, auto_runstats", Severity.WARNING,
					"Automatic statistics are OFF (" + StringUtil.toCommaStr(maintOff) + " = OFF). Unless a job runs RUNSTATS, the statistics get old, and the optimizer starts to choose bad access plans.",
					"If no other job does it: db2 update db cfg for " + dbName + " using AUTO_MAINT ON AUTO_TBL_MAINT ON AUTO_RUNSTATS ON");
		}

		//-------------------------------------------------------
		// Self tuning memory, and fixed (small) buffer pools
		//-------------------------------------------------------
		String stmm = value("db.self_tuning_mem");
		if (stmm != null && stmm.toUpperCase().startsWith("OFF"))
		{
			addIssue(srvRestart, keyPrefix + "self_tuning_mem.off", "self_tuning_mem", Severity.INFO,
					"SELF_TUNING_MEM is OFF: the buffer pools, the package cache, the lock list and the sort memory keep the size they were given, also when the workload needs something else.",
					"db2 update db cfg for " + dbName + " using SELF_TUNING_MEM ON (and set the memory areas that STMM should tune to AUTOMATIC)");
		}

		sql = "select RTRIM(BPNAME), NPAGES, PAGESIZE from SYSCAT.BUFFERPOOLS where NPAGES > 0 and NPAGES * PAGESIZE < 64 * 1048576 order by BPNAME";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			List<String> smallPools = new ArrayList<>();
			while (rs.next())
				smallPools.add(rs.getString(1) + " (" + (rs.getLong(2) * rs.getLong(3) / 1048576) + " MB)");

			if ( ! smallPools.isEmpty() )
			{
				addIssue(srvRestart, keyPrefix + "bufferpool.fixed_small", "buffer pools", Severity.INFO,
						"Buffer pools with a small FIXED size: " + StringUtil.toCommaStr(smallPools) + ". The self tuning memory manager does not change a fixed size, and a small buffer pool means many physical reads (see the hit ratio in the tab 'BufferPool').",
						"Let STMM size it: ALTER BUFFERPOOL <name> SIZE AUTOMATIC; (or give it a bigger fixed size)");
			}
		}
		catch (SQLException ex)
		{
			_logger.info("Db2Config.checkConfig(): Skipping the check for small buffer pools. Caught: " + ex.getMessage().trim());
		}

		//-------------------------------------------------------
		// Pending values
		//-------------------------------------------------------
		List<String> pending = new ArrayList<>();
		for (Db2ConfigEntry entry : _configList)
			if (entry.isPending)
				pending.add(entry.configName + " (now: " + entry.configValue + ", next: " + entry.deferredValue + ")");
		if ( ! pending.isEmpty() )
		{
			addIssue(srvRestart, keyPrefix + "parameters.pending", "pending parameters", Severity.INFO,
					"Parameters have another value at the next activation of the database (or restart of the instance): " + StringUtil.toCommaStr(pending) + ".",
					"Nothing to fix if a restart is planned. Otherwise: db2 deactivate db " + dbName + "; db2 activate db " + dbName + " (with no connections), or db2stop / db2start for the instance parameters.");
		}

		//-------------------------------------------------------
		// Not explicitly activated: the package cache (and the buffer pools) are emptied when the last connection leaves
		//-------------------------------------------------------
		if (versionInfo != null && ! versionInfo.isExplicitlyActivated() && ! "STANDBY".equalsIgnoreCase(versionInfo.getHadrRole()))
		{
			addIssue(srvRestart, keyPrefix + "database.not_activated", "ACTIVATE DATABASE", Severity.INFO,
					"The database '" + dbName + "' is not explicitly activated (DB_ACTIVATION_STATE " + versionInfo.getActivationState() + "): when the last connection leaves, the database is deactivated, and the buffer pools, the package cache and the monitoring counters are emptied. The next connection has to wait for the activation.",
					"db2 activate db " + dbName + " (as the instance owner, with no connection to the database). And in the start script of the instance.");
		}

		//-------------------------------------------------------
		// No (good) backup for N days
		//-------------------------------------------------------
		int maxDays = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_check_maxDaysSinceBackup, DEFAULT_check_maxDaysSinceBackup);
		if (_timestamp != null && (lastBackup == null || (_timestamp.getTime() - lastBackup.getTime()) > maxDays * 24L * 3600 * 1000))
		{
			addIssue(srvRestart, keyPrefix + "backup.old", "LAST_BACKUP", Severity.WARNING,
					lastBackup == null
						? "The database '" + dbName + "' has NO backup (MON_GET_DATABASE.LAST_BACKUP is empty)."
						: "The last backup of the database '" + dbName + "' is from " + lastBackup + ", more than " + maxDays + " days ago.",
					"Take a backup: db2 backup db " + dbName + (isCircular ? "" : " online") + " to <path> compress. And schedule it. See also the tab 'Backups'. To change the limit of this check: " + PROPKEY_check_maxDaysSinceBackup + "=<days>");
		}

		//-------------------------------------------------------
		// Incremental backups without TRACKMOD
		//-------------------------------------------------------
		if ("OFF".equalsIgnoreCase(value("db.trackmod")) || "NO".equalsIgnoreCase(value("db.trackmod")))
		{
			sql = "select count(*) from SYSIBMADM.DB_HISTORY where OPERATION = 'B' and OPERATIONTYPE in ('I', 'O', 'D', 'E') and START_TIME >= VARCHAR_FORMAT(CURRENT TIMESTAMP - 30 DAYS, 'YYYYMMDDHH24MISS')";
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
				{
					if (rs.getInt(1) > 0)
					{
						addIssue(srvRestart, keyPrefix + "trackmod.off_with_incremental", "trackmod", Severity.WARNING,
								"Incremental or delta backups were tried in the last 30 days (" + rs.getInt(1) + "), but TRACKMOD is OFF: they fail (SQL2426N) without it.",
								"db2 update db cfg for " + dbName + " using TRACKMOD YES; then take a FULL backup (the incremental ones are based on it).");
					}
				}
			}
			catch (SQLException ex)
			{
				_logger.info("Db2Config.checkConfig(): Skipping the check for incremental backups without TRACKMOD. Caught: " + ex.getMessage().trim());
			}
		}

		//-------------------------------------------------------
		if ("-1".equals(value("db.rec_his_retentn")))
		{
			addIssue(srvRestart, keyPrefix + "rec_his_retentn.forever", "rec_his_retentn", Severity.INFO,
					"REC_HIS_RETENTN is -1: the recovery history file is never pruned. It grows for ever (every archived log file is an entry), and everything that reads it (backups, SYSIBMADM.DB_HISTORY, the tab 'Backups') gets slower.",
					"db2 update db cfg for " + dbName + " using REC_HIS_RETENTN <days> (for example 90), or prune it with: PRUNE HISTORY <timestamp>");
		}

		//-------------------------------------------------------
		// Version
		//-------------------------------------------------------
		if (srvVersion > 0 && srvVersion < Ver.ver(11,5))
		{
			addIssue(srvRestart, keyPrefix + "version.old", "version", Severity.INFO,
					"The Db2 version is " + Ver.versionNumToStr(srvVersion) + ". Versions before 11.5 are out of IBM's normal support.",
					"Plan an upgrade to Db2 11.5 (the latest mod pack) or 12.1.");
		}

		//-------------------------------------------------------
		// HADR
		//-------------------------------------------------------
		String hadrRole = versionInfo == null ? null : versionInfo.getHadrRole();
		if (hadrRole != null && ! "STANDARD".equalsIgnoreCase(hadrRole))
		{
			if ("NO".equalsIgnoreCase(value("db.blocknonlogged")))
			{
				addIssue(srvRestart, keyPrefix + "hadr.blocknonlogged", "blocknonlogged", Severity.WARNING,
						"The database is in HADR, but BLOCKNONLOGGED is NO: operations that are not logged (CREATE TABLE ... NOT LOGGED INITIALLY, LOAD without COPY YES) are allowed. They are not sent to the standby, so those tables are not usable there after a takeover.",
						"db2 update db cfg for " + dbName + " using BLOCKNONLOGGED YES");
			}
			Db2ConfigEntry lib = _configMap.get("db.logindexbuild");
			if (lib != null && ("OFF".equalsIgnoreCase(lib.configValue) || "OFF".equalsIgnoreCase(lib.deferredValue)))
			{
				addIssue(srvRestart, keyPrefix + "hadr.logindexbuild", "logindexbuild", Severity.WARNING,
						"The database is in HADR, but LOGINDEXBUILD is OFF" + ("OFF".equalsIgnoreCase(lib.configValue) && ! "OFF".equalsIgnoreCase(lib.deferredValue) ? " (it is ON at the next activation)" : "") + ": index builds are not logged, so the indexes are marked invalid on the standby, and they are rebuilt (slowly) after a takeover.",
						"db2 update db cfg for " + dbName + " using LOGINDEXBUILD ON (and activate the database again)");
			}
			String syncmode = value("db.hadr_syncmode");
			if (("SYNC".equalsIgnoreCase(syncmode) || "NEARSYNC".equalsIgnoreCase(syncmode)) && "0".equals(value("db.hadr_peer_window")))
			{
				addIssue(srvRestart, keyPrefix + "hadr.peer_window_0", "hadr_peer_window", Severity.INFO,
						"HADR_SYNCMODE is " + syncmode + " but HADR_PEER_WINDOW is 0: when the standby is lost, the primary goes on alone at once. A failover (TAKEOVER BY FORCE) to the standby after that loses the transactions the primary committed alone.",
						"If no committed transaction may be lost: db2 update db cfg for " + dbName + " using HADR_PEER_WINDOW <seconds, for example 120> (the primary then waits that long for a lost standby).");
			}
			if ("ASYNC".equalsIgnoreCase(syncmode) || "SUPERASYNC".equalsIgnoreCase(syncmode))
			{
				addIssue(srvRestart, keyPrefix + "hadr.syncmode_async", "hadr_syncmode", Severity.INFO,
						"HADR_SYNCMODE is " + syncmode + ": a commit on the primary does not wait for the standby. A failover can lose the transactions that were committed but not yet sent (or replayed).",
						"If that is not acceptable (and the network is fast enough): db2 update db cfg for " + dbName + " using HADR_SYNCMODE NEARSYNC (on both, and start HADR again).");
			}
		}

		//-------------------------------------------------------
		// Security: PUBLIC has database authorities (a database that is not RESTRICTIVE), AUTHENTICATION CLIENT
		//-------------------------------------------------------
		sql = "select CONNECTAUTH, CREATETABAUTH, BINDADDAUTH, IMPLSCHEMAAUTH from SYSCAT.DBAUTH where GRANTEETYPE = 'G' and GRANTEE = 'PUBLIC'";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			List<String> pub = new ArrayList<>();
			while (rs.next())
			{
				if ("Y".equals(rs.getString(1))) pub.add("CONNECT");
				if ("Y".equals(rs.getString(2))) pub.add("CREATETAB");
				if ("Y".equals(rs.getString(3))) pub.add("BINDADD");
				if ("Y".equals(rs.getString(4))) pub.add("IMPLICIT_SCHEMA");
			}
			if ( ! pub.isEmpty() )
			{
				addIssue(srvRestart, keyPrefix + "security.public_dbauth", "PUBLIC database authorities", Severity.INFO,
						"PUBLIC (every user that can log in to the server) has these database authorities: " + StringUtil.toCommaStr(pub) + ". That is the default for a database that is not created RESTRICTIVE: any OS user can connect, and create tables and schemas.",
						"If only known users should use the database: REVOKE CONNECT, CREATETAB, BINDADD, IMPLICIT_SCHEMA ON DATABASE FROM PUBLIC; and GRANT CONNECT to the users or roles that need it. (Test first: applications that connect with other users stop working)");
			}
		}
		catch (SQLException ex)
		{
			_logger.info("Db2Config.checkConfig(): Skipping the check for PUBLIC database authorities. Caught: " + ex.getMessage().trim());
		}

		if ("CLIENT".equalsIgnoreCase(value("dbm.authentication")))
		{
			addIssue(srvRestart, keyPrefix + "security.authentication_client", "authentication", Severity.WARNING,
					"AUTHENTICATION is CLIENT: the server trusts the client to authenticate the user" + ("YES".equalsIgnoreCase(value("dbm.trust_allclnts")) ? " (and TRUST_ALLCLNTS is YES: all clients are trusted)" : "") + ". Anyone who controls a client can connect as any user.",
					"db2 update dbm cfg using AUTHENTICATION SERVER (or SERVER_ENCRYPT), and restart the instance.");
		}

		//-------------------------------------------------------
		if ("4".equals(value("dbm.diaglevel")))
		{
			addIssue(srvRestart, keyPrefix + "diaglevel.4", "diaglevel", Severity.INFO,
					"DIAGLEVEL is 4 (informational): db2diag.log gets very much text, which costs I/O and can fill the file system. It is meant for problem determination, for a short time.",
					"db2 update dbm cfg using DIAGLEVEL 3");
		}

		//-------------------------------------------------------
		// The instance uses almost all of INSTANCE_MEMORY
		//-------------------------------------------------------
		sql = "select sum(MEMORY_SET_COMMITTED), (select max(VALUE) from SYSIBMADM.DBMCFG where NAME = 'instance_memory') from table(MON_GET_MEMORY_SET(NULL, NULL, -1)) t";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while (rs.next())
			{
				long committedKb = rs.getLong(1);
				long limitKb     = StringUtil.parseLong(rs.getString(2), -1) * 4;
				if (limitKb > 0 && committedKb * 100 / limitKb >= 90)
				{
					addIssue(srvRestart, keyPrefix + "instance_memory.almost_full", "instance_memory", Severity.WARNING,
							"The instance uses " + (committedKb / 1024) + " MB of the " + (limitKb / 1024) + " MB it may use (INSTANCE_MEMORY): " + (committedKb * 100 / limitKb) + "%. When it is full, memory requests fail (SQL1084C, SQL0973N) or STMM can not grow anything.",
							"See the tab 'Memory' for where it goes. Raise INSTANCE_MEMORY (if the server has the RAM), or lower DATABASE_MEMORY / the buffer pools.");
				}
			}
		}
		catch (SQLException ex)
		{
			_logger.info("Db2Config.checkConfig(): Skipping the check for INSTANCE_MEMORY. Caught: " + ex.getMessage().trim());
		}

		//-------------------------------------------------------
		// The package cache is too small
		//-------------------------------------------------------
		if (pkgCacheOverflows > 0)
		{
			addIssue(srvRestart, keyPrefix + "pckcachesz.overflows", "pckcachesz", Severity.INFO,
					"The package cache has overflowed " + pkgCacheOverflows + " times since the database was activated (PKG_CACHE_NUM_OVERFLOWS): it was too small, so statements were removed and had to be compiled again (and the 'Statements' tab lost them).",
					"Make PCKCACHESZ AUTOMATIC (with SELF_TUNING_MEM ON), or bigger: db2 update db cfg for " + dbName + " using PCKCACHESZ AUTOMATIC");
		}

		//-------------------------------------------------------
		// No explain tables: Db2Tune can't capture the plans of running statements
		//-------------------------------------------------------
		if (versionInfo != null && ! versionInfo.hasExplainTables())
		{
			addIssue(srvRestart, keyPrefix + "explain_tables.missing", "explain tables", Severity.INFO,
					"There are no explain tables in the schema of the monitoring login (or SYSTOOLS), so Db2Tune can not capture the execution plans of long running statements (the tab 'Active Statements').",
					"Optional: see DbmsSetup/db2.txt, section 3 (one CALL SYSINSTALLOBJECTS, and EXECUTE on SYSPROC.EXPLAIN_FROM_SECTION).");
		}

		//-------------------------------------------------------
		// User tables without statistics
		//-------------------------------------------------------
		sql = "select count(*), max(RTRIM(TABSCHEMA) || '.' || TABNAME) from SYSCAT.TABLES where TYPE = 'T' and STATS_TIME is NULL and TABSCHEMA not like 'SYS%' and TABSCHEMA not in ('NULLID', 'SQLJ')";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while (rs.next())
			{
				int count = rs.getInt(1);
				if (count > 0)
				{
					addIssue(srvRestart, keyPrefix + "statistics.missing", "RUNSTATS", Severity.INFO,
							count + " user table(s) have NO statistics (SYSCAT.TABLES.STATS_TIME is empty), for example " + rs.getString(2) + ". The optimizer then guesses the sizes (or fabricates statistics at compile time), which can give bad plans.",
							"RUNSTATS ON TABLE <schema>.<table> WITH DISTRIBUTION AND INDEXES ALL; (automatic RUNSTATS does it in time, if it is ON; new or empty tables are often skipped)");
				}
			}
		}
		catch (SQLException ex)
		{
			_logger.info("Db2Config.checkConfig(): Skipping the check for tables without statistics. Caught: " + ex.getMessage().trim());
		}
	}

	/** Add an issue to the config manager */
	private static void addIssue(Timestamp srvRestart, String key, String configName, Severity severity, String description, String resolution)
	{
		DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, key, configName, severity, description, resolution));
	}

	/** The value of a parameter: key is scope.name (null if there is no such parameter) */
	private String value(String key)
	{
		Db2ConfigEntry e = _configMap == null ? null : _configMap.get(key);
		return e == null ? null : e.configValue == null ? "" : e.configValue.trim();
	}

	/**
	 * Get description for the configuration name
	 * @param configName  the key (db.locktimeout), or only the name (the database parameter first)
	 * @return the description
	 */
	@Override
	public String getDescription(String configName)
	{
		if (_configMap == null || configName == null)
			return null;

		Db2ConfigEntry e = _configMap.get(configName);
		if (e == null) e = _configMap.get("db."  + configName.toLowerCase());
		if (e == null) e = _configMap.get("dbm." + configName.toLowerCase());

		return e == null ? null : e.description;
	}

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
	 * When was the configuration read
	 */
	@Override
	public Timestamp getTimestamp()
	{
		return _timestamp;
	}


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
		Db2ConfigEntry e = _configList.get(rowIndex);
		if (e == null)
			return null;

		// NOTE: same order as in COLUMN_HEADERS
		switch (columnIndex)
		{
		case 0:  return e.isNonDefault;
		case 1:  return e.sectionName;
		case 2:  return e.scope;
		case 3:  return e.configName;
		case 4:  return e.configValue;
		case 5:  return e.isPending;
		case 6:  return e.deferredValue;
		case 7:  return e.valueFlags;
		case 8:  return e.defaultValue;
		case 9:  return e.dataType;
		case 10: return e.description;
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
			{
				return i;
			}
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
	public String getSqlDataType(DbxConnection conn, int colIndex)
	{
		int jdbcType = (int)COLUMN_HEADERS[colIndex][COL_JDBC_TYPE];
		int length   = (int)COLUMN_HEADERS[colIndex][COL_JDBC_LENGTH];
		int scale    = (int)COLUMN_HEADERS[colIndex][COL_JDBC_SCALE];

		return conn.getDbmsDataTypeResolver().dataTypeResolverToTarget(jdbcType, length, scale);
	}

	@Override
	public String getTabLabel()
	{
		return "Db2 Config";
	}

	@Override
	public String getCellToolTip(int mrow, int mcol)
	{
		Db2ConfigEntry e = _configList.get(mrow);
		if (e == null)
			return null;

		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		if (StringUtil.hasValue(e.description))
			sb.append("<b>Description:    </b>").append(e.description).append("<br>");
		sb.append("<b>Parameter:      </b>").append(e.configName).append(" (").append(e.scope).append(")").append("<br>");
		sb.append("<b>Value:          </b>").append(e.configValue).append(StringUtil.hasValue(e.valueFlags) && ! "NONE".equals(e.valueFlags) ? " (" + e.valueFlags + ")" : "").append("<br>");
		if ( ! SCOPE_REGISTRY.equals(e.scope) )
		{
			sb.append("<b>Deferred Value: </b>").append(e.deferredValue == null ? "" : e.deferredValue).append("<br>");
			sb.append("<b>Default Value:  </b>").append(e.defaultValue == null ? "<i>not known</i>" : e.defaultValue).append("<br>");
		}
		if (e.isPending)
		{
			sb.append("<br>");
			sb.append("<b>NOTE: The value changes at the next activation of the database (or restart of the instance).</b>").append("<br>");
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

	@Override
	public boolean isReverseEngineeringPossible()
	{
		return true;
	}

	/** One UPDATE DB CFG / UPDATE DBM CFG / db2set command, with some comment lines above it (they are CLP commands, not SQL) */
	private String toUpdateCommand(Db2ConfigEntry e, String value, String prevValue)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("---------------------------------------------------------------------------\n");
		sb.append("-- Parameter:       ").append(e.configName).append(" (").append(e.scope).append(")\n");
		if (StringUtil.hasValue(e.description))
			sb.append("-- Description:     ").append(e.description).append("\n");
		if (prevValue != null)
			sb.append("-- Prev Value:      ").append(prevValue).append("\n");

		String valStr = value == null ? "" : value;
		if ("AUTOMATIC".equals(e.valueFlags) && ! valStr.toUpperCase().contains("AUTOMATIC"))
			valStr = valStr + " AUTOMATIC";

		if      (SCOPE_DB .equals(e.scope)) sb.append("db2 update db cfg for ").append(getDbNameForCommand()).append(" using ").append(e.configName.toUpperCase()).append(" ").append(valStr).append("\n");
		else if (SCOPE_DBM.equals(e.scope)) sb.append("db2 update dbm cfg using ").append(e.configName.toUpperCase()).append(" ").append(valStr).append("\n");
		else                                sb.append("db2set ").append(e.configName).append("=").append(valStr).append("\n");
		sb.append("\n");

		return sb.toString();
	}

	/** The database name for UPDATE DB CFG (from the server name inst/db@host if it is there) */
	private String getDbNameForCommand()
	{
		String srv = getDbmsServerName();
		if (srv != null && srv.contains("/") && srv.contains("@"))
			return srv.substring(srv.indexOf('/') + 1, srv.indexOf('@'));
		return "<DBNAME>";
	}

	@Override
	public String reverseEngineer(int[] modelRows)
	{
		if (modelRows == null)     return null;
		if (modelRows.length == 0) return null;

		StringBuilder sb = new StringBuilder();

		// Build a header...
		sb.append("# \n");
		sb.append("# Reverse engineering ").append(modelRows.length).append(" entries. \n");
		sb.append("# At Date:          ").append(new Timestamp(System.currentTimeMillis())).append("\n");
		sb.append("# Db2 Server name:  ").append(getDbmsServerName()).append("\n");
		sb.append("# Db2 Version:      ").append((getDbmsVersionStr() + "").replace('\n', ' ')).append("\n");
		sb.append("# Note:             These are CLP / shell commands (run them as the instance owner), not SQL statements.\n");
		sb.append("# \n");
		sb.append("\n");

		for (int r=0; r<modelRows.length; r++)
		{
			Db2ConfigEntry e = _configList.get(modelRows[r]);

			sb.append(toUpdateCommand(e, e.configValue, null));
		}

		return sb.toString();
	}

	@Override
	public String reverseEngineer(Map<String, String> keyValMap, String comment)
	{
		if (keyValMap == null)     return null;
		if (keyValMap.size() == 0) return null;

		StringBuilder sb = new StringBuilder();

		sb.append("# \n");
		sb.append("# Reverse engineering ").append(keyValMap.size()).append(" entries. \n");
		sb.append("# At Date:          ").append(new Timestamp(System.currentTimeMillis())).append("\n");
		sb.append("# Db2 Server name:  ").append(getDbmsServerName()).append("\n");
		sb.append("# Db2 Version:      ").append((getDbmsVersionStr() + "").replace('\n', ' ')).append("\n");
		if (StringUtil.hasValue(comment))
			sb.append("# Comment:          ").append(comment).append("\n");
		sb.append("# Note:             These are CLP / shell commands (run them as the instance owner), not SQL statements.\n");
		sb.append("# \n");
		sb.append("\n");

		for (Entry<String, String> kv : keyValMap.entrySet())
		{
			Db2ConfigEntry e = _configMap.get(kv.getKey());
			if (e == null)
			{
				sb.append("# The parameter '").append(kv.getKey()).append("' does not exist in this server, so nothing is done for it.\n\n");
				continue;
			}

			sb.append(toUpdateCommand(e, kv.getValue(), e.configValue));
		}

		return sb.toString();
	}
}
