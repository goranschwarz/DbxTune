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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.MySqlTune;
import com.dbxtune.config.dbms.DbmsConfigIssue.Severity;
import com.dbxtune.pcs.MonRecordingInfo;
import com.dbxtune.pcs.PersistWriterJdbc;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfoMySql;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;
import com.dbxtune.utils.Ver;


/**
 * The MySQL system variables: the GLOBAL values the server runs with (performance_schema.global_variables),
 * where each value comes from (variables_info), and what SET PERSIST has saved for the next restart (persisted_variables).
 * <p>
 * MySQL has no "default value" of a variable to compare with, so 'NonDefault' is from where the value came from (variables_info.VARIABLE_SOURCE).
 */
public class MySqlConfig
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
	public static final String CONFIG_NAME     = "VariableName";
	public static final String CONFIG_VALUE    = "Value";
	public static final String PENDING         = "Pending";
	public static final String PERSISTED_VALUE = "PersistedValue";
	public static final String SOURCE          = "Source";
	public static final String SOURCE_FILE     = "SourceFile";
	public static final String SET_TIME        = "SetTime";
	public static final String SET_USER        = "SetUser";
	public static final String SET_HOST        = "SetHost";
	public static final String SCOPE           = "Scope";
	public static final String DATA_TYPE       = "DataType";
	public static final String MIN_VALUE       = "MinValue";
	public static final String MAX_VALUE       = "MaxValue";
	public static final String DESCRIPTION     = "Description";

	/** Variables that the installation must set (paths and files): they are never NonDefault (a comma separated list of variable names) */
	public static final String PROPKEY_installationVariables = "MySqlConfig.nonDefault.always";
	public static final String DEFAULT_installationVariables = ""
		+ "basedir, "
		+ "datadir, "
		+ "socket, "
		+ "mysqlx_socket, "
		+ "pid_file, "
		+ "secure_file_priv, "
		+ "log_error, "
		+ "plugin_dir, "
		+ "tmpdir, "
		+ "lc_messages_dir, "
		+ "character_sets_dir, "
		+ "general_log_file, "
		+ "slow_query_log_file";

	/**
	 * Variables that clients and JDBC/ODBC drivers set for their OWN session: when one of them is 'set at runtime' (DYNAMIC), it's probably a SET SESSION, so it's not NonDefault.<br>
	 * (MySQL's variables_info shows the last SET of a variable, also of the session value)
	 */
	public static final String PROPKEY_sessionVariables = "MySqlConfig.nonDefault.skipWhenSetAtRuntime";
	public static final String DEFAULT_sessionVariables = ""
		+ "autocommit, "
		+ "character_set_client, "
		+ "character_set_connection, "
		+ "character_set_results, "
		+ "collation_connection, "
		+ "foreign_key_checks, "
		+ "unique_checks, "
		+ "sql_log_bin, "
		+ "sql_select_limit, "
		+ "net_read_timeout, "
		+ "net_write_timeout, "
		+ "transaction_isolation, "
		+ "transaction_read_only, "
		+ "time_zone";

	public static final String SECTION_INNODB       = "InnoDB";
	public static final String SECTION_REPLICATION  = "Replication and Binary Log";
	public static final String SECTION_PFS          = "Performance Schema";
	public static final String SECTION_SECURITY     = "Security";
	public static final String SECTION_CHARSET      = "Character Sets";
	public static final String SECTION_LOGGING      = "Logging";
	public static final String SECTION_CONNECTIONS  = "Connections and Threads";
	public static final String SECTION_OPTIMIZER    = "Optimizer";
	public static final String SECTION_MEMORY       = "Memory and Caches";
	public static final String SECTION_COMPONENTS   = "Plugins and Components";
	public static final String SECTION_OTHER        = "Other";

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
		{NON_DEFAULT,      false,    Boolean.class, Types.BOOLEAN, -1,         -1,         "<html>The variable has been set to something else than MySQL's default: in a configuration file (my.cnf), on the command line, with SET PERSIST, or with SET GLOBAL after the server started.<br>MySQL can not tell the default value, so this is from where the value came from (column 'Source').<br>Not NonDefault:<ul><li>the paths and files that the installation must set, from the property <code>" + PROPKEY_installationVariables + "</code></li><li>a SET at runtime of a variable that clients normally set for their own session (autocommit, character_set_results...), from the property <code>" + PROPKEY_sessionVariables + "</code></li><li>a SET at runtime by the server itself (no SetUser), or by the login of MySqlTune</li></ul></html>"},
		{SECTION_NAME,     true,     String .class, Types.VARCHAR, 30,         -1,         "<html>MySQL has no groups for the variables, so this is from the name: InnoDB, Replication and Binary Log, Performance Schema, Security, Logging, Memory and Caches...</html>"},
		{CONFIG_NAME,      true,     String .class, Types.VARCHAR, 80,         -1,         "Name of the system variable (as in SHOW GLOBAL VARIABLES)."},
		{CONFIG_VALUE,     true,     String .class, Types.VARCHAR, 4096,       -1,         "<html>The GLOBAL value the server is running with.<br>(A connection can have another value for itself, for the variables that has a session value)</html>"},
		{PENDING,          false,    Boolean.class, Types.BOOLEAN, -1,         -1,         "<html>SET PERSIST (or SET PERSIST_ONLY) has saved another value than the server is running with: it takes effect at the next restart.<br>This is what you get with <code>SET PERSIST_ONLY</code> for a variable that can't be changed while the server runs.<br><b>Source</b>: performance_schema.persisted_variables (the file mysqld-auto.cnf in the data directory)</html>"},
		{PERSISTED_VALUE,  true,     String .class, Types.VARCHAR, 4096,       -1,         "<html>The value that SET PERSIST has saved (it's used at every restart). Empty if it is not persisted.<br><b>Source</b>: performance_schema.persisted_variables</html>"},
		{SOURCE,           true,     String .class, Types.VARCHAR, 20,         -1,         "<html>Where the value came from:<ul><li><b>COMPILED</b> - MySQL's default (or a value MySQL has calculated by itself).</li><li><b>GLOBAL, SERVER, EXPLICIT, EXTRA, USER</b> - a configuration file (see 'SourceFile').</li><li><b>COMMAND_LINE</b> - the command line of mysqld.</li><li><b>PERSISTED</b> - SET PERSIST (mysqld-auto.cnf).</li><li><b>DYNAMIC</b> - SET GLOBAL (or SET SESSION) after the server started. Lost at the next restart, unless it's also persisted or in the configuration file.</li><li><b>LOGIN</b> - set by the client at login (session).</li></ul><b>Source</b>: performance_schema.variables_info</html>"},
		{SOURCE_FILE,      true,     String .class, Types.VARCHAR, 1024,       -1,         "The configuration file the value came from, when 'Source' is a file."},
		{SET_TIME,         true,     String .class, Types.VARCHAR, 30,         -1,         "When it was set at runtime (SET GLOBAL / SET PERSIST), in the time of the server."},
		{SET_USER,         true,     String .class, Types.VARCHAR, 64,         -1,         "The user that set it at runtime. Empty if the server set it by itself."},
		{SET_HOST,         true,     String .class, Types.VARCHAR, 255,        -1,         "The host that user came from."},
		{SCOPE,            true,     String .class, Types.VARCHAR, 20,         -1,         "<html>GLOBAL: only a global value. SESSION: a global value, and every connection can have its own value. MySQL 9.0 and above (empty before that).</html>"},
		{DATA_TYPE,        true,     String .class, Types.VARCHAR, 20,         -1,         "Integer, Boolean, String, Enumeration, Set, Numeric... MySQL 9.0 and above (empty before that)."},
		{MIN_VALUE,        true,     String .class, Types.VARCHAR, 64,         -1,         "The lowest value it can be set to (numbers only)."},
		{MAX_VALUE,        true,     String .class, Types.VARCHAR, 64,         -1,         "The highest value it can be set to (numbers only)."},
		{DESCRIPTION,      true,     String .class, Types.VARCHAR, 4096,       -1,         "<html>What the variable does. MySQL 9.0 and above has it for all variables (variables_metadata.DOCUMENTATION).<br>Before that only for the most important ones (written by MySqlTune).</html>"},
	};

	private static String GET_CONFIG_OFFLINE_SQL =
		"select * " +
		"from [" + PersistWriterJdbc.getTableName(null, null, PersistWriterJdbc.SESSION_DBMS_CONFIG, null, false) + "] \n" +
		"where [SessionStartTime] = SESSION_START_TIME \n";

	private static String GET_CONFIG_OFFLINE_MAX_SESSION_SQL =
		" (select max([SessionStartTime]) " +
		"  from [" + PersistWriterJdbc.getTableName(null, null, PersistWriterJdbc.SESSION_DBMS_CONFIG, null, false) + "]" +
		" ) ";

	/** hashtable with MySqlConfigEntry */
	private HashMap<String,MySqlConfigEntry> _configMap  = null;
	private ArrayList<MySqlConfigEntry>      _configList = null;

	private ArrayList<String>                _configSectionList = null;

	public class MySqlConfigEntry
	implements IDbmsConfigEntry
	{
		@Override public String  getConfigKey()   { return configName; }
		@Override public String  getConfigValue() { return configValue; }
		@Override public String  getSectionName() { return sectionName; }
		@Override public boolean isPending()      { return isPending; }
		@Override public boolean isNonDefault()   { return isNonDefault; }

		public boolean isNonDefault;
		public String  sectionName;
		public String  configName;
		public String  configValue;
		public boolean isPending;
		public String  persistedValue;
		public String  source;
		public String  sourceFile;
		public String  setTime;
		public String  setUser;
		public String  setHost;
		public String  scope;
		public String  dataType;
		public String  minValue;
		public String  maxValue;
		public String  description;
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

		_configMap         = new HashMap<String,MySqlConfigEntry>();
		_configList        = new ArrayList<MySqlConfigEntry>();
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
				// ONLINE: 1 - The time, and our own login (a SET at runtime by us is not NonDefault)
				//-------------------------------------------------------
				String ownLogin = "";
				sql = "select CURRENT_TIMESTAMP, SUBSTRING_INDEX(USER(), '@', 1)";
				try (ResultSet rs = stmt.executeQuery(sql))
				{
					while (rs.next())
					{
						_timestamp = rs.getTimestamp(1);
						ownLogin   = rs.getString(2);
					}
				}

				//-------------------------------------------------------
				// ONLINE: 2 - The variables
				//   The newest SQL first: the first one that this MySQL version accepts is used
				//   - 9.0 and above: variables_metadata has the scope, the data type and a description
				//   - 8.0: variables_info (where the value came from) and persisted_variables (SET PERSIST)
				//   - anything else (MariaDB, performance_schema not readable): SHOW GLOBAL VARIABLES, only the name and the value
				//-------------------------------------------------------
				String sqlColumns = ""
					+ "select g.VARIABLE_NAME \n"
					+ "      ,g.VARIABLE_VALUE \n"
					+ "      ,i.VARIABLE_SOURCE \n"
					+ "      ,i.VARIABLE_PATH \n"
					+ "      ,i.MIN_VALUE \n"
					+ "      ,i.MAX_VALUE \n"
					+ "      ,DATE_FORMAT(i.SET_TIME, '%Y-%m-%d %H:%i:%s') as SET_TIME \n"
					+ "      ,i.SET_USER \n"
					+ "      ,i.SET_HOST \n"
					+ "      ,p.VARIABLE_VALUE as PERSISTED_VALUE \n";
				String sqlFrom = ""
					+ "from performance_schema.global_variables g \n"
					+ "left join performance_schema.variables_info      i on i.VARIABLE_NAME = g.VARIABLE_NAME \n"
					+ "left join performance_schema.persisted_variables p on p.VARIABLE_NAME = g.VARIABLE_NAME \n";

				String[] sqlCandidates = {
					// 9.0 and above
					sqlColumns
						+ "      ,m.VARIABLE_SCOPE \n"
						+ "      ,m.DATA_TYPE \n"
						+ "      ,m.DOCUMENTATION \n"
						+ sqlFrom
						+ "left join performance_schema.variables_metadata  m on m.VARIABLE_NAME = g.VARIABLE_NAME \n"
						+ "order by g.VARIABLE_NAME",
					// 8.0
					sqlColumns
						+ "      ,CAST(NULL as CHAR) as VARIABLE_SCOPE \n"
						+ "      ,CAST(NULL as CHAR) as DATA_TYPE \n"
						+ "      ,CAST(NULL as CHAR) as DOCUMENTATION \n"
						+ sqlFrom
						+ "order by g.VARIABLE_NAME",
					// The last way out
					"SHOW GLOBAL VARIABLES"
				};

				ResultSet rs = null;
				for (int i=0; i<sqlCandidates.length && rs == null; i++)
				{
					sql = sqlCandidates[i];
					try
					{
						rs = stmt.executeQuery(sql);
					}
					catch (SQLException ex)
					{
						// The last one must work
						if (i == sqlCandidates.length - 1)
							throw ex;
					}
				}
				boolean hasSourceInfo = rs.getMetaData().getColumnCount() >= 13;
				if ( ! hasSourceInfo )
					_logger.info("MySqlConfig: could not read performance_schema.global_variables / variables_info, so it's not known where the values came from (NonDefault is false for all). The variables are from: " + sql);

				Set<String> installationVariables = new HashSet<>();
				for (String name : StringUtil.commaStrToSet(Configuration.getCombinedConfiguration().getProperty(PROPKEY_installationVariables, DEFAULT_installationVariables), true))
					installationVariables.add(name.trim().toLowerCase());

				Set<String> sessionVariables = new HashSet<>();
				for (String name : StringUtil.commaStrToSet(Configuration.getCombinedConfiguration().getProperty(PROPKEY_sessionVariables, DEFAULT_sessionVariables), true))
					sessionVariables.add(name.trim().toLowerCase());

				while ( rs.next() )
				{
					MySqlConfigEntry entry = new MySqlConfigEntry();

					entry.configName  = rs.getString(1);
					entry.configValue = rs.getString(2);

					if (hasSourceInfo)
					{
						entry.source         = rs.getString(3);
						entry.sourceFile     = rs.getString(4);
						entry.minValue       = rs.getString(5);
						entry.maxValue       = rs.getString(6);
						entry.setTime        = rs.getString(7);
						entry.setUser        = rs.getString(8);
						entry.setHost        = rs.getString(9);
						entry.persistedValue = rs.getString(10);
						entry.scope          = rs.getString(11);
						entry.dataType       = rs.getString(12);
						entry.description    = rs.getString(13);
					}

					// Should not happen, but the name is our key
					if (entry.configName == null || _configMap.containsKey(entry.configName))
						continue;

					// Before 9.0 there is no description: use ours (only for the most important variables)
					if (StringUtil.isNullOrBlank(entry.description))
						entry.description = DESCRIPTIONS.get(entry.configName);

					// Empty strings from the view: show them as nothing
					if ("".equals(entry.sourceFile)) entry.sourceFile = null;
					if ("".equals(entry.setUser))    entry.setUser    = null;
					if ("".equals(entry.setHost))    entry.setHost    = null;

					// NonDefault: where the value came from is not COMPILED (MySQL's default)
					String name = entry.configName.toLowerCase();
					entry.isNonDefault = entry.source != null && ! "COMPILED".equals(entry.source);

					// The paths and files that the installation must set
					if (installationVariables.contains(name))
						entry.isNonDefault = false;

					// A SET at runtime by the server itself, by our own login, or of a variable that clients set for their own session (probably a SET SESSION)
					if ("DYNAMIC".equals(entry.source) && (entry.setUser == null || entry.setUser.equalsIgnoreCase(ownLogin) || sessionVariables.contains(name)))
						entry.isNonDefault = false;

					// Pending: SET PERSIST(_ONLY) has saved another value than the server runs with
					entry.isPending = entry.persistedValue != null && ! isSameValue(entry.persistedValue, entry.configValue);

					entry.sectionName = getSection(name);

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
				// For OFFLINE mode: override some the values previously fetched the connection object, with values from the Recorded Database
				MonRecordingInfo recordingInfo = new MonRecordingInfo(conn, null);
				setOfflineRecordingInfo(recordingInfo);
				setDbmsServerName(recordingInfo.getDbmsServerName());
				setDbmsVersionStr(recordingInfo.getDbmsVersionStr());

				// Check if this is correct TYPE
				String expectedType = MySqlTune.APP_NAME;
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

				// All columns may not be in the recording (an older version had other columns), so only read the ones that are there
				// for DBMS portability use "lower-case" names
				List<String> columns = new ArrayList<>();
				for (int c=1; c<=rsmd.getColumnCount(); c++)
					columns.add(rsmd.getColumnLabel(c).toLowerCase());

				// Recordings made before the variable table was redone (it was a copy of the SQL Server one) has: Category, Name, RunValue, IsPending
				String colSection = columns.contains(SECTION_NAME.toLowerCase()) ? SECTION_NAME : "Category";
				String colName    = columns.contains(CONFIG_NAME .toLowerCase()) ? CONFIG_NAME  : "Name";
				String colValue   = columns.contains(CONFIG_VALUE.toLowerCase()) ? CONFIG_VALUE : "RunValue";
				String colPending = columns.contains(PENDING     .toLowerCase()) ? PENDING      : "IsPending";

				// In those old recordings NonDefault was TRUE for every variable (the value was compared with a default of -999), so it's not known
				boolean isOldLayout = ! columns.contains(SOURCE.toLowerCase());

				while ( rs.next() )
				{
					MySqlConfigEntry entry = new MySqlConfigEntry();

					_timestamp = rs.getTimestamp(1);

					if (columns.contains(NON_DEFAULT    .toLowerCase())) entry.isNonDefault   = rs.getBoolean( 1 + columns.indexOf(NON_DEFAULT    .toLowerCase()) );
					if (columns.contains(colSection     .toLowerCase())) entry.sectionName    = rs.getString ( 1 + columns.indexOf(colSection     .toLowerCase()) );
					if (columns.contains(colName        .toLowerCase())) entry.configName     = rs.getString ( 1 + columns.indexOf(colName        .toLowerCase()) );
					if (columns.contains(colValue       .toLowerCase())) entry.configValue    = rs.getString ( 1 + columns.indexOf(colValue       .toLowerCase()) );
					if (columns.contains(colPending     .toLowerCase())) entry.isPending      = rs.getBoolean( 1 + columns.indexOf(colPending     .toLowerCase()) );
					if (columns.contains(PERSISTED_VALUE.toLowerCase())) entry.persistedValue = rs.getString ( 1 + columns.indexOf(PERSISTED_VALUE.toLowerCase()) );
					if (columns.contains(SOURCE         .toLowerCase())) entry.source         = rs.getString ( 1 + columns.indexOf(SOURCE         .toLowerCase()) );
					if (columns.contains(SOURCE_FILE    .toLowerCase())) entry.sourceFile     = rs.getString ( 1 + columns.indexOf(SOURCE_FILE    .toLowerCase()) );
					if (columns.contains(SET_TIME       .toLowerCase())) entry.setTime        = rs.getString ( 1 + columns.indexOf(SET_TIME       .toLowerCase()) );
					if (columns.contains(SET_USER       .toLowerCase())) entry.setUser        = rs.getString ( 1 + columns.indexOf(SET_USER       .toLowerCase()) );
					if (columns.contains(SET_HOST       .toLowerCase())) entry.setHost        = rs.getString ( 1 + columns.indexOf(SET_HOST       .toLowerCase()) );
					if (columns.contains(SCOPE          .toLowerCase())) entry.scope          = rs.getString ( 1 + columns.indexOf(SCOPE          .toLowerCase()) );
					if (columns.contains(DATA_TYPE      .toLowerCase())) entry.dataType       = rs.getString ( 1 + columns.indexOf(DATA_TYPE      .toLowerCase()) );
					if (columns.contains(MIN_VALUE      .toLowerCase())) entry.minValue       = rs.getString ( 1 + columns.indexOf(MIN_VALUE      .toLowerCase()) );
					if (columns.contains(MAX_VALUE      .toLowerCase())) entry.maxValue       = rs.getString ( 1 + columns.indexOf(MAX_VALUE      .toLowerCase()) );
					if (columns.contains(DESCRIPTION    .toLowerCase())) entry.description    = rs.getString ( 1 + columns.indexOf(DESCRIPTION    .toLowerCase()) );

					if (entry.configName == null)
						continue;

					if (isOldLayout)
						entry.isNonDefault = false;
					if ("".equals(entry.description))
						entry.description = null;

					// An old recording has the section 'DELETEME?...' or none: use ours
					if (entry.sectionName == null || ! columns.contains(SECTION_NAME.toLowerCase()))
						entry.sectionName = getSection(entry.configName.toLowerCase());

					_configMap .put(entry.configName, entry);
					_configList.add(entry);

					// Add it to the section list, if not already there
					if ( ! _configSectionList.contains(entry.sectionName) )
						_configSectionList.add(entry.sectionName);
				}
				rs.close();
			}

			stmt.close();
		}
		catch (SQLException ex)
		{
			_logger.error("MySqlConfig:initialize:sql='" + sql + "'", ex);
			if (_hasGui)
				SwingUtils.showErrorMessage("MySqlConfig - Initialize", "SQL Exception: " + ex.getMessage() + "\n\nThis was found when executing SQL statement:\n\n" + sql, ex);
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

	/**
	 * Is the persisted value the same as the value the server runs with?<br>
	 * 'ON' = '1' and 'OFF' = '0', and numbers are compared as numbers (a persisted double is saved as '10.000000')
	 */
	private static boolean isSameValue(String persisted, String value)
	{
		if (persisted == null || value == null)
			return persisted == value;

		String a = persisted.trim();
		String b = value.trim();

		if (a.equalsIgnoreCase(b))
			return true;

		if (("ON" .equalsIgnoreCase(a) && "1".equals(b)) || ("1".equals(a) && "ON" .equalsIgnoreCase(b))) return true;
		if (("OFF".equalsIgnoreCase(a) && "0".equals(b)) || ("0".equals(a) && "OFF".equalsIgnoreCase(b))) return true;

		try { return Double.parseDouble(a) == Double.parseDouble(b); }
		catch (NumberFormatException ex) { return false; }
	}

	/** MySQL has no groups for the variables, so make some from the name */
	private static String getSection(String name)
	{
		if (name.contains("."))                                                       return SECTION_COMPONENTS; // component variables: validate_password.length...
		if (name.startsWith("performance_schema"))                                   return SECTION_PFS;
		if (name.startsWith("innodb_"))                                              return SECTION_INNODB;

		if (name.startsWith("binlog_")      || name.startsWith("log_bin")            || name.equals("sync_binlog")          || name.startsWith("max_binlog")
		 || name.startsWith("gtid_")        || name.equals("enforce_gtid_consistency")
		 || name.startsWith("replica_")     || name.startsWith("slave_")             || name.startsWith("source_")          || name.startsWith("master_")
		 || name.startsWith("relay_log")    || name.startsWith("rpl_")               || name.startsWith("sync_relay_log")   || name.startsWith("sync_master_info") || name.startsWith("sync_source_info")
		 || name.startsWith("log_replica")  || name.startsWith("log_slave")          || name.startsWith("skip_replica")     || name.startsWith("skip_slave")
		 || name.startsWith("group_replication") || name.equals("server_id")         || name.equals("server_uuid")          || name.equals("expire_logs_days"))
			return SECTION_REPLICATION;

		if (name.startsWith("ssl")          || name.startsWith("tls")                || name.startsWith("admin_ssl")        || name.startsWith("admin_tls")
		 || name.contains("password")       || name.contains("authentication")       || name.startsWith("caching_sha2")     || name.startsWith("sha256")
		 || name.equals("require_secure_transport") || name.equals("local_infile")   || name.equals("secure_file_priv")     || name.equals("auto_generate_certs")
		 || name.startsWith("mysql_native_password") || name.equals("activate_all_roles_on_login") || name.equals("mandatory_roles") || name.equals("partial_revokes"))
			return SECTION_SECURITY;

		if (name.startsWith("character_set") || name.startsWith("collation"))          return SECTION_CHARSET;

		if (name.startsWith("log_")         || name.startsWith("general_log")        || name.startsWith("slow_")            || name.equals("long_query_time")
		 || name.equals("min_examined_row_limit"))
			return SECTION_LOGGING;

		if (name.startsWith("max_connect")  || name.equals("max_user_connections")   || name.endsWith("_timeout")           || name.startsWith("thread_")
		 || name.equals("back_log")         || name.equals("skip_name_resolve")      || name.equals("host_cache_size")      || name.equals("port")
		 || name.equals("bind_address")     || name.startsWith("admin_")             || name.startsWith("mysqlx")           || name.startsWith("net_")
		 || name.equals("max_allowed_packet"))
			return SECTION_CONNECTIONS;

		if (name.startsWith("optimizer_")   || name.contains("join_")                || name.startsWith("eq_range")         || name.startsWith("range_")
		 || name.startsWith("max_seeks")    || name.startsWith("histogram")          || name.equals("cte_max_recursion_depth") || name.startsWith("max_execution_time"))
			return SECTION_OPTIMIZER;

		if (name.endsWith("_buffer_size")   || name.endsWith("_cache_size")          || name.startsWith("table_open_cache") || name.equals("table_definition_cache")
		 || name.equals("tmp_table_size")   || name.equals("max_heap_table_size")    || name.startsWith("temptable_")       || name.startsWith("key_")
		 || name.equals("open_files_limit") || name.startsWith("schema_definition")  || name.startsWith("stored_program"))
			return SECTION_MEMORY;

		return SECTION_OTHER;
	}

	/**
	 * Descriptions of the most important variables: MySQL 8.0 and 8.4 has no description in the server (9.0 has it in variables_metadata).
	 */
	private static final Map<String, String> DESCRIPTIONS = new HashMap<>();
	static
	{
		DESCRIPTIONS.put("innodb_buffer_pool_size",         "The memory InnoDB caches table and index pages in. The most important memory setting: on a server that only runs MySQL, normally 50-75% of the RAM. The default (128 MB) is for a small test.");
		DESCRIPTIONS.put("innodb_buffer_pool_instances",    "How many parts the buffer pool is split in (less waiting on the buffer pool mutexes).");
		DESCRIPTIONS.put("innodb_redo_log_capacity",        "The size of the redo log (MySQL 8.0.30 and above). Too small: InnoDB has to write dirty pages to disk often (checkpoints), which slows down writes.");
		DESCRIPTIONS.put("innodb_log_file_size",            "The size of one redo log file (before 8.0.30; innodb_redo_log_capacity replaces it).");
		DESCRIPTIONS.put("innodb_log_files_in_group",       "The number of redo log files (before 8.0.30; innodb_redo_log_capacity replaces it).");
		DESCRIPTIONS.put("innodb_log_buffer_size",          "Memory for redo records before they are written to the redo log. Large transactions write to disk early if it is too small.");
		DESCRIPTIONS.put("innodb_flush_log_at_trx_commit",  "1 (default): the redo log is written AND flushed to disk at every commit (no committed transaction is lost). 2: written at commit, flushed once a second. 0: both once a second. With 0 or 2, a crash can lose about 1 second of committed transactions.");
		DESCRIPTIONS.put("innodb_flush_method",             "How InnoDB writes and flushes its files (O_DIRECT is normal on Linux: no double caching in the file system cache).");
		DESCRIPTIONS.put("innodb_io_capacity",              "How many IO operations per second InnoDB's background work (flushing dirty pages) may use. Should match what the storage can do.");
		DESCRIPTIONS.put("innodb_io_capacity_max",          "How many IO operations per second the background flushing may use when it is behind.");
		DESCRIPTIONS.put("innodb_doublewrite",              "Pages are first written to the doublewrite buffer, then to their place: a page that is only half written at a crash can be repaired. Turn off only if the storage guarantees atomic page writes.");
		DESCRIPTIONS.put("innodb_file_per_table",           "Every table gets its own file (.ibd). OFF: new tables go into the system tablespace (ibdata1), which never shrinks.");
		DESCRIPTIONS.put("innodb_print_all_deadlocks",      "Write every deadlock to the error log (it is written as a note, so log_error_verbosity must be 3). Otherwise only the last one is seen, in SHOW ENGINE INNODB STATUS.");
		DESCRIPTIONS.put("innodb_lock_wait_timeout",        "Seconds a transaction waits for a row lock before it gets an error (1205). Only the statement is rolled back, not the transaction (unless innodb_rollback_on_timeout).");
		DESCRIPTIONS.put("innodb_dedicated_server",         "MySQL sets the buffer pool size and the redo log size from the RAM of the server by itself (for a server that only runs MySQL).");
		DESCRIPTIONS.put("innodb_stats_persistent",         "The optimizer statistics are saved (in mysql.innodb_table_stats and innodb_index_stats), so they survive a restart and do not change by themselves.");
		DESCRIPTIONS.put("sync_binlog",                     "1 (default): the binary log is flushed to disk at every commit. 0: the operating system decides. N: every N commits. Not 1: a crash can lose transactions from the binary log (a replica or point-in-time recovery misses them).");
		DESCRIPTIONS.put("log_bin",                         "The binary log is ON: it is needed for replication and for point-in-time recovery.");
		DESCRIPTIONS.put("binlog_format",                   "ROW (default): the changed rows are logged. STATEMENT / MIXED: the statements (can give other results on a replica). Deprecated since 8.0.34.");
		DESCRIPTIONS.put("binlog_expire_logs_seconds",      "Binary logs older than this are removed. 0 = never removed (the disk fills up). Default 2592000 (30 days).");
		DESCRIPTIONS.put("binlog_expire_logs_auto_purge",   "OFF: binary logs are never removed by themselves (MySQL 8.0.29 and above).");
		DESCRIPTIONS.put("expire_logs_days",                "Days before binary logs are removed (removed in MySQL 8.4: use binlog_expire_logs_seconds).");
		DESCRIPTIONS.put("gtid_mode",                       "Global Transaction Identifiers: every transaction gets a unique id, so a replica knows what it has applied (easier failover).");
		DESCRIPTIONS.put("server_id",                       "Must be unique for every server in a replication setup.");
		DESCRIPTIONS.put("read_only",                       "ON: only users with CONNECTION_ADMIN (or SUPER) can change data. Should be ON on a replica.");
		DESCRIPTIONS.put("super_read_only",                 "ON: not even users with CONNECTION_ADMIN (or SUPER) can change data.");
		DESCRIPTIONS.put("relay_log_recovery",              "At startup on a replica: throw away the relay logs and fetch them again from the source (makes a replica crash safe).");
		DESCRIPTIONS.put("replica_parallel_workers",        "How many threads a replica applies transactions with. 0 or 1: one at a time.");
		DESCRIPTIONS.put("slave_parallel_workers",          "How many threads a replica applies transactions with (the old name of replica_parallel_workers).");
		DESCRIPTIONS.put("max_connections",                 "The most connections at the same time. One more is allowed for a user with CONNECTION_ADMIN.");
		DESCRIPTIONS.put("table_open_cache",                "How many open tables the server can keep (for all connections). Too small: tables are closed and opened again all the time.");
		DESCRIPTIONS.put("table_definition_cache",          "How many table definitions the server can keep in memory.");
		DESCRIPTIONS.put("tmp_table_size",                  "The largest internal temporary table in memory (together with max_heap_table_size, for the MEMORY engine). Larger ones are written to disk.");
		DESCRIPTIONS.put("max_heap_table_size",             "The largest MEMORY table (also limits internal temporary tables that use the MEMORY engine).");
		DESCRIPTIONS.put("temptable_max_ram",               "The most memory all internal temporary tables (TempTable engine) can use together, before they go to disk.");
		DESCRIPTIONS.put("sort_buffer_size",                "Memory per sort (per connection, allocated when needed). Large values are seldom better.");
		DESCRIPTIONS.put("join_buffer_size",                "Memory per join that can't use an index (per connection, allocated when needed).");
		DESCRIPTIONS.put("thread_cache_size",               "How many threads are kept for new connections, so a thread does not have to be created for every new connection.");
		DESCRIPTIONS.put("wait_timeout",                    "Seconds before an idle connection is closed by the server.");
		DESCRIPTIONS.put("skip_name_resolve",               "ON: the host name of a client is not looked up in DNS (faster connects, no problems when DNS is slow). Users must then be defined with IP addresses, not host names.");
		DESCRIPTIONS.put("local_infile",                    "ON: LOAD DATA LOCAL is allowed. A server (or anyone who controls the connection) can then ask the client to send any file it can read. OFF by default since 8.0.");
		DESCRIPTIONS.put("require_secure_transport",        "ON: only encrypted (TLS) connections, or a local socket, are allowed.");
		DESCRIPTIONS.put("default_authentication_plugin",   "The password method for new users (8.0; removed in 8.4). mysql_native_password is deprecated (removed in 9.0).");
		DESCRIPTIONS.put("sql_mode",                        "How strict MySQL is. Without STRICT_TRANS_TABLES (or STRICT_ALL_TABLES) a too long or wrong value is silently changed (truncated) with only a warning, instead of an error.");
		DESCRIPTIONS.put("character_set_server",            "The character set for new databases (schemas) that do not say. utf8mb4 is the default since 8.0 (utf8mb3 and latin1 can't store all characters).");
		DESCRIPTIONS.put("collation_server",                "The collation (sort and compare rules) for new databases (schemas) that do not say.");
		DESCRIPTIONS.put("default_storage_engine",          "The storage engine of a new table that does not say. InnoDB (the default) is the only one with transactions and crash recovery.");
		DESCRIPTIONS.put("slow_query_log",                  "Statements that run longer than long_query_time are written to the slow query log.");
		DESCRIPTIONS.put("long_query_time",                 "Seconds a statement must run to be written to the slow query log.");
		DESCRIPTIONS.put("log_error_verbosity",             "1: errors, 2: errors and warnings (default), 3: also notes (deadlocks are written as notes).");
		DESCRIPTIONS.put("performance_schema",              "The performance_schema (monitoring tables). Almost everything MySqlTune shows comes from it. Can only be changed with a restart.");
		DESCRIPTIONS.put("performance_schema_digests_size", "How many different statements (digests) the statement summary can have. When it is full, new statements are counted in one row without a digest.");
		DESCRIPTIONS.put("transaction_isolation",           "The default isolation level: REPEATABLE-READ (MySQL's default), READ-COMMITTED, READ-UNCOMMITTED or SERIALIZABLE.");
	}


	/**
	 * Look for things in the configuration that a DBA normally wants to know about.
	 * <p>
	 * Every check is on it's own: if one of them can't get it's data (a missing privilege...) the others are still done.
	 */
	@Override
	public void checkConfig(DbxConnection conn)
	{
		if (_configMap == null)
			return;

		String srvName = "-UNKNOWN-";
		try { srvName = conn.getDbmsServerName(); } catch (SQLException ex) { _logger.info("Problems getting MySQL server name. ex=" + ex);}

		String keyPrefix = "DbmsConfigIssue." + srvName + ".mysql.";

		DbmsVersionInfoMySql versionInfo = (conn.getDbmsVersionInfo() instanceof DbmsVersionInfoMySql) ? (DbmsVersionInfoMySql) conn.getDbmsVersionInfo() : null;
		long srvVersion = conn.getDbmsVersionNumber(); // 0 if we don't know

		// MariaDB has other variables, and other versions: MySqlTune only detects it
		if (versionInfo != null && versionInfo.isMariaDb())
		{
			_logger.info("MySqlConfig.checkConfig(): This is MariaDB, the configuration checks are made for MySQL, so they are skipped.");
			return;
		}

		//-------------------------------------------------------
		// The status variables: one read, used by several checks
		//-------------------------------------------------------
		Map<String, String> status = new HashMap<>();
		String sql = "select VARIABLE_NAME, VARIABLE_VALUE from performance_schema.global_status";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while (rs.next())
				status.put(rs.getString(1), rs.getString(2));
		}
		catch (SQLException ex)
		{
			_logger.info("MySqlConfig.checkConfig(): Skipping the checks that need the status variables. Caught: " + ex.getMessage().trim());
		}

		// When the server was started: now - Uptime. To the MINUTE: the two values are read at different times (+- 1 second), and the time is a part of the key to discard an issue
		Timestamp srvRestart = null;
		long uptime = StringUtil.parseLong(status.get("Uptime"), -1);
		if (_timestamp != null && uptime >= 0)
		{
			LocalDateTime startTime = _timestamp.toLocalDateTime().minusSeconds(uptime).truncatedTo(ChronoUnit.MINUTES);
			srvRestart = Timestamp.valueOf(startTime);
		}

		//-------------------------------------------------------
		// performance_schema OFF
		//-------------------------------------------------------
		if ("OFF".equalsIgnoreCase(value("performance_schema")))
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "performance_schema.off", "performance_schema", Severity.WARNING,
					"performance_schema is OFF. Almost everything MySqlTune shows comes from it (statements, waits, locks, memory...), so most of the Performance Counters are disabled.",
					"In the configuration file (my.cnf): performance_schema = ON, and restart the server. (It costs a few percent of CPU, and some memory: see the tab 'Memory')"));
		}
		else
		{
			//-------------------------------------------------------
			// performance_schema consumers that are OFF, but MySqlTune needs
			//-------------------------------------------------------
			sql = "select NAME from performance_schema.setup_consumers where ENABLED = 'NO' and NAME in ('global_instrumentation', 'thread_instrumentation', 'statements_digest', 'events_statements_current', 'events_transactions_current') order by NAME";
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				List<String> off = new ArrayList<>();
				while (rs.next())
					off.add(rs.getString(1));

				if ( ! off.isEmpty() )
				{
					DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "performance_schema.consumers_off", "setup_consumers", Severity.INFO,
							"performance_schema consumers that MySqlTune needs are OFF: " + StringUtil.toCommaStr(off) + ". Some Performance Counters show less (or nothing): statements_digest is the tab 'Statements', events_statements_current is 'Active Statements'.",
							"UPDATE performance_schema.setup_consumers SET ENABLED = 'YES' WHERE NAME IN (...); and, so it survives a restart, in my.cnf: performance-schema-consumer-<name with - instead of _> = ON"));
				}
			}
			catch (SQLException ex)
			{
				_logger.info("MySqlConfig.checkConfig(): Skipping the check for performance_schema consumers. Caught: " + ex.getMessage().trim());
			}

			//-------------------------------------------------------
			// performance_schema has lost data: one of its fixed size tables was too small
			//-------------------------------------------------------
			List<String> lost = new ArrayList<>();
			for (Entry<String, String> e : status.entrySet())
			{
				if (e.getKey().startsWith("Performance_schema_") && e.getKey().endsWith("_lost") && StringUtil.parseLong(e.getValue(), 0) > 0)
					lost.add(e.getKey() + "=" + e.getValue());
			}
			if ( ! lost.isEmpty() )
			{
				lost.sort(null);
				DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "performance_schema.lost", "Performance_schema_*_lost", Severity.INFO,
						"performance_schema could not keep everything since the server started, one of its tables was full: " + StringUtil.toCommaStr(lost) + ". What MySqlTune shows for that part is not complete.",
						"Increase the matching performance_schema_* size variable in my.cnf (for example performance_schema_digests_size for Performance_schema_digest_lost), and restart. They are auto-sized by default (-1): then increase it explicitly."));
			}
		}

		//-------------------------------------------------------
		// MySQL 8.0: end of life
		//-------------------------------------------------------
		if (srvVersion > 0 && srvVersion < Ver.ver(8,1))
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "version.eol_8_0", "version", Severity.WARNING,
					"This is MySQL " + Ver.versionNumToStr(srvVersion) + ". MySQL 8.0 reached its end of life in April 2026: there are no more bug fixes or security fixes.",
					"Plan an upgrade to the long term support version 8.4 (in place, or with a replica). Read 'Changes in MySQL 8.4' first: some variables are removed (for example default_authentication_plugin and expire_logs_days), and mysql_native_password is off by default."));
		}

		//-------------------------------------------------------
		// mysql_native_password: deprecated in 8.0, off by default in 8.4, removed in 9.0
		//-------------------------------------------------------
		if ("mysql_native_password".equalsIgnoreCase(value("default_authentication_plugin")) || "ON".equalsIgnoreCase(value("mysql_native_password")))
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "auth.mysql_native_password", "mysql_native_password", Severity.INFO,
					"The old password method mysql_native_password is " + ("ON".equalsIgnoreCase(value("mysql_native_password")) ? "turned ON" : "the default for new users (default_authentication_plugin)") + ". It is deprecated, and it is removed in MySQL 9.0: users that still have it can't log in after that upgrade.",
					"Move the users to caching_sha2_password (ALTER USER ... IDENTIFIED WITH caching_sha2_password BY '...'), check that all clients and drivers support it, then remove the setting from my.cnf."));
		}

		//-------------------------------------------------------
		// expire_logs_days: removed in 8.4
		//-------------------------------------------------------
		if (StringUtil.parseLong(value("expire_logs_days"), 0) > 0)
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "binlog.expire_logs_days", "expire_logs_days", Severity.INFO,
					"expire_logs_days is set (" + value("expire_logs_days") + "). It is deprecated, and it is removed in MySQL 8.4: the server does not start with it in my.cnf after an upgrade.",
					"Use binlog_expire_logs_seconds instead (days * 86400), and remove expire_logs_days from my.cnf."));
		}

		//-------------------------------------------------------
		// Durability: innodb_flush_log_at_trx_commit
		//-------------------------------------------------------
		if (value("innodb_flush_log_at_trx_commit") != null && ! "1".equals(value("innodb_flush_log_at_trx_commit")))
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "innodb_flush_log_at_trx_commit.not_1", "innodb_flush_log_at_trx_commit", Severity.WARNING,
					"innodb_flush_log_at_trx_commit is " + value("innodb_flush_log_at_trx_commit") + ", not 1. The redo log is not flushed to disk at every commit: a crash of the server (or of the operating system) can lose about the last second of COMMITTED transactions.",
					"If no committed transaction may be lost: SET PERSIST innodb_flush_log_at_trx_commit = 1; (it costs one flush per commit, which is normally fine on SSD storage)"));
		}

		//-------------------------------------------------------
		// Binary log: OFF, sync_binlog, never purged, format
		//-------------------------------------------------------
		boolean isLogBin = "ON".equalsIgnoreCase(value("log_bin")) || "1".equals(value("log_bin"));
		if ( ! isLogBin && value("log_bin") != null )
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "log_bin.off", "log_bin", Severity.INFO,
					"The binary log is OFF. The server can't be a replication source, and a restore can only go back to the last backup (no point-in-time recovery).",
					"If that is not what you want: remove 'skip-log-bin' (or 'disable-log-bin') from my.cnf, and restart. The binary log is ON by default since 8.0."));
		}

		if (isLogBin && value("sync_binlog") != null && ! "1".equals(value("sync_binlog")))
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "sync_binlog.not_1", "sync_binlog", Severity.WARNING,
					"sync_binlog is " + value("sync_binlog") + ", not 1. The binary log is not flushed to disk at every commit: after a crash, transactions can be missing in the binary log (a replica, or a point-in-time recovery, does not get them), or the binary log and InnoDB can disagree.",
					"SET PERSIST sync_binlog = 1;"));
		}

		if (isLogBin)
		{
			long expireSeconds = StringUtil.parseLong(value("binlog_expire_logs_seconds"), -1);
			long expireDays    = StringUtil.parseLong(value("expire_logs_days"), 0);
			boolean autoPurgeOff = "OFF".equalsIgnoreCase(value("binlog_expire_logs_auto_purge"));
			if ((expireSeconds == 0 && expireDays == 0) || autoPurgeOff)
			{
				DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "binlog.never_purged", "binlog_expire_logs_seconds", Severity.WARNING,
						"The binary logs are never removed by the server (" + (autoPurgeOff ? "binlog_expire_logs_auto_purge = OFF" : "binlog_expire_logs_seconds = 0") + "): unless something else removes them (PURGE BINARY LOGS), they fill the disk. See the tab 'Binary Logs' for how much they use now.",
						"SET PERSIST binlog_expire_logs_seconds = 604800; (7 days, or how long you need them: longer than a replica can be stopped, and back to the last full backup)" + (autoPurgeOff ? " and SET PERSIST binlog_expire_logs_auto_purge = ON;" : "")));
			}
		}

		if (isLogBin && value("binlog_format") != null && ! "ROW".equalsIgnoreCase(value("binlog_format")))
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "binlog_format.not_row", "binlog_format", Severity.INFO,
					"binlog_format is " + value("binlog_format") + ". The statements are logged instead of the changed rows: a statement that is not deterministic (LIMIT without ORDER BY, NOW(), UUID()...) can give another result on a replica. binlog_format is deprecated since 8.0.34 (ROW will be the only one).",
					"SET PERSIST binlog_format = 'ROW'; (check first that nothing depends on statement based logging)"));
		}

		//-------------------------------------------------------
		// Deadlocks in the error log
		//-------------------------------------------------------
		boolean printAllDeadlocks = "ON".equalsIgnoreCase(value("innodb_print_all_deadlocks"));
		long    errorVerbosity    = StringUtil.parseLong(value("log_error_verbosity"), -1);
		if (value("innodb_print_all_deadlocks") != null && ( ! printAllDeadlocks || (errorVerbosity >= 0 && errorVerbosity < 3)))
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "deadlocks.not_logged", "innodb_print_all_deadlocks", Severity.INFO,
					"Deadlocks are not written to the error log (innodb_print_all_deadlocks = " + value("innodb_print_all_deadlocks") + ", log_error_verbosity = " + value("log_error_verbosity") + "). Only the last deadlock can be seen (SHOW ENGINE INNODB STATUS), and the tab 'Error Log' can't raise an alarm for them.",
					"SET PERSIST innodb_print_all_deadlocks = ON; SET PERSIST log_error_verbosity = 3; (InnoDB writes deadlocks as notes, so both are needed. Verbosity 3 also writes other notes, for example aborted connections)"));
		}

		//-------------------------------------------------------
		// Memory: the buffer pool at the default size
		//-------------------------------------------------------
		long bufferPoolSize = StringUtil.parseLong(value("innodb_buffer_pool_size"), -1);
		if (bufferPoolSize == 134217728L)
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "innodb_buffer_pool_size.default", "innodb_buffer_pool_size", Severity.WARNING,
					"innodb_buffer_pool_size is the default, 128 MB. That is made for a small test server: InnoDB can only cache 128 MB of data and indexes, the rest is read from disk.",
					"On a server that only runs MySQL: about 50-75% of the RAM, for example SET PERSIST innodb_buffer_pool_size = <bytes>; (it can be changed without a restart). Or innodb_dedicated_server = ON in my.cnf, and MySQL sizes it by itself."));
		}

		//-------------------------------------------------------
		// The redo log at the default size (8.0.30+: innodb_redo_log_capacity, before: file size * files)
		//-------------------------------------------------------
		long redoCapacity = StringUtil.parseLong(value("innodb_redo_log_capacity"), -1);
		if (redoCapacity < 0)
			redoCapacity = StringUtil.parseLong(value("innodb_log_file_size"), -1) * StringUtil.parseLong(value("innodb_log_files_in_group"), 1);
		if (redoCapacity > 0 && redoCapacity <= 104857600L && ! "ON".equalsIgnoreCase(value("innodb_dedicated_server")))
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "redo.small", "innodb_redo_log_capacity", Severity.INFO,
					"The redo log is " + (redoCapacity / 1048576) + " MB (the default). With much writing, InnoDB has to write dirty pages to disk early to make room in the redo log, and the writes can stall.",
					"From 8.0.30: SET PERSIST innodb_redo_log_capacity = <bytes>; (for example 1-4 GB, or about one hour of writing; see the graph of the redo log in the Summary). Before 8.0.30: innodb_log_file_size in my.cnf, and a restart."));
		}

		//-------------------------------------------------------
		// max_connections: nearly reached, or reached
		//-------------------------------------------------------
		long maxConnections = StringUtil.parseLong(value("max_connections"), -1);
		long maxUsed        = StringUtil.parseLong(status.get("Max_used_connections"), -1);
		long refused        = StringUtil.parseLong(status.get("Connection_errors_max_connections"), 0);
		if (maxConnections > 0 && maxUsed >= 0 && (refused > 0 || maxUsed * 100 / maxConnections >= 85))
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "max_connections.reached", "max_connections", Severity.WARNING,
					"Since the server started, the most connections at the same time was " + maxUsed + " of max_connections " + maxConnections + (refused > 0 ? ", and " + refused + " connections were REFUSED (Too many connections)" : "") + ".",
					"Find out if the connections are needed (a connection pool that is too large, connections that are not closed: see the tab 'Sessions'). If they are: SET PERSIST max_connections = <more>; (every connection uses some memory)"));
		}

		//-------------------------------------------------------
		// The table caches are too small
		//-------------------------------------------------------
		// NOTE: Open_tables can't be compared with table_open_cache: the cache is split in table_open_cache_instances parts (a connection uses one of them), so a part can be full long before
		//       So: how often a table that was not in the cache (a miss) had to throw out another one (an overflow)
		long misses    = StringUtil.parseLong(status.get("Table_open_cache_misses"), 0);
		long overflows = StringUtil.parseLong(status.get("Table_open_cache_overflows"), 0);
		if (overflows >= 100 && misses > 0 && overflows * 100 / misses >= 10)
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "table_open_cache.full", "table_open_cache", Severity.INFO,
					"The table cache is too small: of the " + misses + " times a table was not in the cache since the server started, another table had to be closed to make room " + overflows + " times (Table_open_cache_overflows). "
					+ "table_open_cache is " + value("table_open_cache") + ", split in " + value("table_open_cache_instances") + " parts (table_open_cache_instances), and each connection uses one part.",
					"SET PERSIST table_open_cache = <more>; (about the number of tables that are used, times the number of parts it is split in, if memory allows. Check that open_files_limit allows it)"));
		}

		long tableDefCache = StringUtil.parseLong(value("table_definition_cache"), -1);
		long openTableDefs = StringUtil.parseLong(status.get("Open_table_definitions"), -1);
		if (tableDefCache > 0 && openTableDefs >= tableDefCache)
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "table_definition_cache.full", "table_definition_cache", Severity.INFO,
					"The table definition cache is full (Open_table_definitions " + openTableDefs + " = table_definition_cache " + tableDefCache + "): table definitions are read from the data dictionary again and again.",
					"SET PERSIST table_definition_cache = <more than the number of tables that are used>;"));
		}

		//-------------------------------------------------------
		// Internal temporary tables on disk
		//-------------------------------------------------------
		long tmpTables     = StringUtil.parseLong(status.get("Created_tmp_tables"), 0);
		long tmpDiskTables = StringUtil.parseLong(status.get("Created_tmp_disk_tables"), 0);
		if (tmpTables > 1000 && tmpDiskTables * 100 / tmpTables >= 25)
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "tmp_tables.on_disk", "tmp_table_size", Severity.INFO,
					"Of the internal temporary tables since the server started, " + (tmpDiskTables * 100 / tmpTables) + "% (" + tmpDiskTables + " of " + tmpTables + ") had to be written to disk. That is slow.",
					"Find the statements (tab 'Statements', column TmpDiskTables): often an index or a smaller GROUP BY / ORDER BY helps more. Otherwise more memory: tmp_table_size and temptable_max_ram (and max_heap_table_size)."));
		}

		//-------------------------------------------------------
		// Security
		//-------------------------------------------------------
		if ("ON".equalsIgnoreCase(value("local_infile")))
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "local_infile.on", "local_infile", Severity.WARNING,
					"local_infile is ON: LOAD DATA LOCAL is allowed. With it, a server (or someone who has taken over the connection) can ask a client to send any file the client can read.",
					"If no application needs LOAD DATA LOCAL: SET PERSIST local_infile = OFF; (the default since 8.0)"));
		}

		if ("OFF".equalsIgnoreCase(value("require_secure_transport")))
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "require_secure_transport.off", "require_secure_transport", Severity.INFO,
					"require_secure_transport is OFF: clients may connect without encryption (TLS), and then passwords for some login methods, and all data, go over the network as plain text.",
					"When all clients use TLS: SET PERSIST require_secure_transport = ON; (a local socket connection is still allowed)"));
		}

		if ("OFF".equalsIgnoreCase(value("skip_name_resolve")))
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "skip_name_resolve.off", "skip_name_resolve", Severity.INFO,
					"skip_name_resolve is OFF: the server looks up the host name of every new client in DNS. A slow or broken DNS makes new connections slow, or makes them fail.",
					"If the users are defined with IP addresses (or '%'), not host names: skip_name_resolve = ON in my.cnf, and restart."));
		}

		//-------------------------------------------------------
		// Data correctness: sql_mode, character set, storage engine, InnoDB files
		//-------------------------------------------------------
		String sqlMode = value("sql_mode");
		if (sqlMode != null && ! sqlMode.toUpperCase().contains("STRICT_TRANS_TABLES") && ! sqlMode.toUpperCase().contains("STRICT_ALL_TABLES"))
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "sql_mode.not_strict", "sql_mode", Severity.WARNING,
					"sql_mode has no strict mode ('" + sqlMode + "'): a value that is too long, or wrong, is silently changed (cut, or set to 0 or an empty date) with only a warning, instead of giving an error. Data is changed without anyone knowing.",
					"Add STRICT_TRANS_TABLES (the default since 5.7): SET PERSIST sql_mode = '<the current modes>,STRICT_TRANS_TABLES'; Test the applications first: statements that worked can now give errors. See the tab 'SQL Mode'."));
		}

		String charsetServer = value("character_set_server");
		if (charsetServer != null && ! charsetServer.toLowerCase().startsWith("utf8mb4"))
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "character_set_server.not_utf8mb4", "character_set_server", Severity.INFO,
					"character_set_server is " + charsetServer + ", not utf8mb4 (the default since 8.0). New schemas get it, and it can't store all characters (utf8mb3 is also deprecated).",
					"character_set_server = utf8mb4 in my.cnf (or SET PERSIST). Existing schemas and tables keep their character set: they must be converted (ALTER TABLE ... CONVERT TO CHARACTER SET utf8mb4), which rebuilds them."));
		}

		if (value("default_storage_engine") != null && ! "InnoDB".equalsIgnoreCase(value("default_storage_engine")))
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "default_storage_engine.not_innodb", "default_storage_engine", Severity.WARNING,
					"default_storage_engine is " + value("default_storage_engine") + ": a new table that does not say ENGINE= gets it. Only InnoDB has transactions, row locks and crash recovery.",
					"SET PERSIST default_storage_engine = InnoDB;"));
		}

		if ("OFF".equalsIgnoreCase(value("innodb_doublewrite")))
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "innodb_doublewrite.off", "innodb_doublewrite", Severity.WARNING,
					"innodb_doublewrite is OFF. A page that is only half written when the server (or the power) goes down can't be repaired: the table is corrupt.",
					"Unless the storage guarantees atomic writes of 16 KB pages (some file systems and SSDs do): innodb_doublewrite = ON in my.cnf, and restart."));
		}

		if ("OFF".equalsIgnoreCase(value("innodb_file_per_table")))
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "innodb_file_per_table.off", "innodb_file_per_table", Severity.INFO,
					"innodb_file_per_table is OFF: new tables go into the system tablespace (ibdata1), which never gets smaller, also when tables are dropped or truncated.",
					"SET PERSIST innodb_file_per_table = ON; (existing tables get their own file when they are rebuilt: ALTER TABLE ... ENGINE=InnoDB)"));
		}

		//-------------------------------------------------------
		// The slow query log
		//-------------------------------------------------------
		if ("OFF".equalsIgnoreCase(value("slow_query_log")))
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "slow_query_log.off", "slow_query_log", Severity.INFO,
					"The slow query log is OFF: there is no record of single slow statements, with their values (the tab 'Statements' has them summed per normalized statement).",
					"SET PERSIST slow_query_log = ON; SET PERSIST long_query_time = 2; (seconds)"));
		}

		//-------------------------------------------------------
		// On a replica
		//-------------------------------------------------------
		if (versionInfo != null && versionInfo.isReplica())
		{
			if ("OFF".equalsIgnoreCase(value("read_only")))
			{
				DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "replica.read_only.off", "read_only", Severity.WARNING,
						"This is a replica, but read_only is OFF: an application that writes to it by mistake makes the replica different from the source, and the replication can stop (a duplicate key, a row that is not found).",
						"SET PERSIST read_only = ON; (and super_read_only = ON, so not even administrators write to it by mistake). The replication itself still works."));
			}

			if ("OFF".equalsIgnoreCase(value("relay_log_recovery")))
			{
				DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "replica.relay_log_recovery.off", "relay_log_recovery", Severity.INFO,
						"This is a replica, but relay_log_recovery is OFF: after a crash of the replica, the relay logs can be damaged, and the replication can stop or apply something twice.",
						"relay_log_recovery = ON in my.cnf (it is read at startup)."));
			}

			long parallelWorkers = StringUtil.parseLong(value("replica_parallel_workers") != null ? value("replica_parallel_workers") : value("slave_parallel_workers"), -1);
			if (parallelWorkers >= 0 && parallelWorkers <= 1)
			{
				DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "replica.parallel_workers", "replica_parallel_workers", Severity.INFO,
						"This is a replica that applies the transactions one at a time (replica_parallel_workers = " + parallelWorkers + "). A source with many connections that write can then make the replica fall behind.",
						"SET PERSIST replica_parallel_workers = 4; (the default since 8.0.27) and replica_preserve_commit_order = ON. Takes effect after STOP REPLICA SQL_THREAD; START REPLICA SQL_THREAD;"));
			}

			if (value("gtid_mode") != null && ! "ON".equalsIgnoreCase(value("gtid_mode")))
			{
				DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "replica.gtid_mode.off", "gtid_mode", Severity.INFO,
						"This is a replica without GTIDs (gtid_mode = " + value("gtid_mode") + "): it keeps track of where it is with file names and positions. Changing to another source (a failover) is harder, and easy to get wrong.",
						"Turn on GTIDs on all servers of the replication, step by step, without a stop: see 'Enabling GTID Transactions Online' in the MySQL manual."));
			}
		}

		//-------------------------------------------------------
		// Variables: pending (saved for the next restart), and changed at runtime but NOT saved (lost at the next restart)
		//-------------------------------------------------------
		List<String> pending      = new ArrayList<>();
		List<String> notPersisted = new ArrayList<>();
		for (MySqlConfigEntry entry : _configList)
		{
			if (entry.isPending)
				pending.add(entry.configName + " (now: " + entry.configValue + ", persisted: " + entry.persistedValue + ")");

			if (entry.isNonDefault && "DYNAMIC".equals(entry.source) && entry.persistedValue == null)
				notPersisted.add(entry.configName + " = " + entry.configValue + " (by " + entry.setUser + ", " + entry.setTime + ")");
		}

		if ( ! pending.isEmpty() )
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "variables.pending", "persisted variables", Severity.INFO,
					"SET PERSIST (or PERSIST_ONLY) has saved other values than the server is running with for: " + StringUtil.toCommaStr(pending) + ". The server gets the saved values at the next restart.",
					"Nothing to fix if the restart is planned. Otherwise decide which value is the right one: SET PERSIST <name> = <value>; or remove the saved value: RESET PERSIST <name>;"));
		}

		if ( ! notPersisted.isEmpty() )
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "variables.not_persisted", "SET GLOBAL", Severity.INFO,
					"Variables were changed after the server started, but not saved: " + StringUtil.toCommaStr(notPersisted) + ". If it was a SET GLOBAL, they get their old value at the next restart (unless the same value is also in my.cnf). "
					+ "NOTE: MySQL also shows a SET SESSION of one connection like this (it does not say which), so if the user is an application, it was probably that, and nothing needs to be done.",
					"If the new value should stay: SET PERSIST <name> = <value>; (or put it in my.cnf). If not: SET GLOBAL <name> = <old value>; A variable that an application sets for its own session can be added to the property " + PROPKEY_sessionVariables + " (then it is not NonDefault)."));
		}
	}

	/** The value of a variable (null if this version does not have it) */
	private String value(String name)
	{
		MySqlConfigEntry e = _configMap == null ? null : _configMap.get(name);
		return e == null ? null : e.configValue;
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
		MySqlConfigEntry e = _configList.get(rowIndex);
		if (e == null)
			return null;

		// NOTE: same order as in COLUMN_HEADERS
		switch (columnIndex)
		{
		case 0:  return e.isNonDefault;
		case 1:  return e.sectionName;
		case 2:  return e.configName;
		case 3:  return e.configValue;
		case 4:  return e.isPending;
		case 5:  return e.persistedValue;
		case 6:  return e.source;
		case 7:  return e.sourceFile;
		case 8:  return e.setTime;
		case 9:  return e.setUser;
		case 10: return e.setHost;
		case 11: return e.scope;
		case 12: return e.dataType;
		case 13: return e.minValue;
		case 14: return e.maxValue;
		case 15: return e.description;
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
		return "MySQL Config";
	}

	@Override
	public String getCellToolTip(int mrow, int mcol)
	{
		MySqlConfigEntry e = _configList.get(mrow);
		if (e == null)
			return null;

		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		if (StringUtil.hasValue(e.description))
			sb.append("<b>Description:     </b>").append(e.description).append("<br>");
		sb.append("<b>Variable:        </b>").append(e.configName) .append("<br>");
		sb.append("<b>Value:           </b>").append(e.configValue).append("<br>");
		sb.append("<b>Persisted Value: </b>").append(e.persistedValue == null ? "<i>not persisted</i>" : e.persistedValue).append("<br>");
		sb.append("<b>Came from:       </b>").append(e.source == null ? "" : e.source).append(e.sourceFile == null ? "" : " (" + e.sourceFile + ")").append("<br>");
		if (e.setUser != null)
			sb.append("<b>Set at runtime:  </b>").append("by ").append(e.setUser).append(e.setHost == null ? "" : "@" + e.setHost).append(", ").append(e.setTime).append("<br>");
		if (e.isPending)
		{
			sb.append("<br>");
			sb.append("<b>NOTE: SET PERSIST has saved another value than the server runs with. It takes effect at the next restart.</b>").append("<br>");
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

	/**
	 * One <code>SET PERSIST</code> statement, with some comment lines above it.<br>
	 * The statement ends with <code>go</code>: that is the terminator in the SQL Window. For the mysql client: replace it with <code>;</code>
	 */
	private String toSetPersist(MySqlConfigEntry e, String value, String prevValue)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("---------------------------------------------------------------------------\n");
		sb.append("-- Variable:        ").append(e.configName).append("\n");
		if (StringUtil.hasValue(e.description))
			sb.append("-- Description:     ").append(e.description).append("\n");
		sb.append("-- Came from:       ").append(e.source == null ? "" : e.source).append(e.sourceFile == null ? "" : " (" + e.sourceFile + ")").append("\n");
		if (prevValue != null)
			sb.append("-- Prev Value:      ").append(prevValue).append("\n");
		sb.append("-- NOTE: If it says that the variable is read only: use SET PERSIST_ONLY, and restart the server.\n");

		// Numbers as they are, everything else in quotes
		String valStr = value == null ? "" : value;
		if ( ! valStr.matches("-?[0-9]+(\\.[0-9]+)?") )
			valStr = "'" + valStr.replace("'", "''") + "'";

		sb.append("SET PERSIST ").append(e.configName).append(" = ").append(valStr).append("\n");
		sb.append("go\n");
		sb.append("\n");

		return sb.toString();
	}

	@Override
	public String reverseEngineer(int[] modelRows)
	{
		if (modelRows == null)     return null;
		if (modelRows.length == 0) return null;

		StringBuilder sb = new StringBuilder();

		// Build a header...
		sb.append("/* \n");
		sb.append("** Reverse engineering ").append(modelRows.length).append(" entries. \n");
		sb.append("** At Date:           ").append(new Timestamp(System.currentTimeMillis())).append("\n");
		sb.append("** MySQL Server name: ").append(getDbmsServerName()).append("\n");
		sb.append("** MySQL Version:     ").append((getDbmsVersionStr() + "").replace('\n', ' ')).append("\n");
		sb.append("** Note:              'go' ends a statement in the SQL Window. For the mysql client: replace it with ';'\n");
		sb.append("*/ \n");
		sb.append("\n");

		for (int r=0; r<modelRows.length; r++)
		{
			MySqlConfigEntry e = _configList.get(modelRows[r]);

			sb.append(toSetPersist(e, e.configValue, null));
		}

		return sb.toString();
	}

	@Override
	public String reverseEngineer(Map<String, String> keyValMap, String comment)
	{
		if (keyValMap == null)     return null;
		if (keyValMap.size() == 0) return null;

		StringBuilder sb = new StringBuilder();

		sb.append("/* \n");
		sb.append("** Reverse engineering ").append(keyValMap.size()).append(" entries. \n");
		sb.append("** At Date:           ").append(new Timestamp(System.currentTimeMillis())).append("\n");
		sb.append("** MySQL Server name: ").append(getDbmsServerName()).append("\n");
		sb.append("** MySQL Version:     ").append((getDbmsVersionStr() + "").replace('\n', ' ')).append("\n");
		if (StringUtil.hasValue(comment))
			sb.append("** Comment:           ").append(comment).append("\n");
		sb.append("** Note:              'go' ends a statement in the SQL Window. For the mysql client: replace it with ';'\n");
		sb.append("*/ \n");
		sb.append("\n");

		for (Entry<String, String> kv : keyValMap.entrySet())
		{
			MySqlConfigEntry e = _configMap.get(kv.getKey());
			if (e == null)
			{
				sb.append("-- The variable '").append(kv.getKey()).append("' does not exist in this server, so nothing is done for it.\n\n");
				continue;
			}

			sb.append(toSetPersist(e, kv.getValue(), e.configValue));
		}

		return sb.toString();
	}
}
