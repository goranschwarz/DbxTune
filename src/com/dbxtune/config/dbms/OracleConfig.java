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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.OracleTune;
import com.dbxtune.config.dbms.DbmsConfigIssue.Severity;
import com.dbxtune.pcs.MonRecordingInfo;
import com.dbxtune.pcs.PersistWriterJdbc;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;
import com.dbxtune.utils.Ver;


/**
 * The Oracle initialization parameters, as the INSTANCE has them (V$SYSTEM_PARAMETER), together with what the spfile says (V$SPPARAMETER).
 * <p>
 * Note: V$PARAMETER is NOT used: it has the values of our own SESSION (and the JDBC driver changes for example nls_territory to the locale of the client).
 */
public class OracleConfig
extends DbmsConfigAbstract
{
	private static final long serialVersionUID = 1L;

	/** Log4j logging. */
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/** Instance variable */
	private boolean   _offline   = false;
	private boolean   _hasGui    = false;
	private Timestamp _timestamp = null;

	public static final String NON_DEFAULT    = "NonDefault";
	public static final String SECTION_NAME   = "Section";
	public static final String CONFIG_NAME    = "ParameterName";
	public static final String CONFIG_VALUE   = "Value";
	public static final String DISPLAY_VALUE  = "DisplayValue";
	public static final String PENDING        = "Pending";
	public static final String SPFILE_VALUE   = "SpfileValue";
	public static final String DEFAULT_VALUE  = "DefaultValue";
	public static final String RESTART_IS_REQ = "RestartIsReq";
	public static final String SYS_MODIFIABLE = "SystemModifiable";
	public static final String SES_MODIFIABLE = "SessionModifiable";
	public static final String PDB_MODIFIABLE = "PdbModifiable";
	public static final String MODIFIED       = "Modified";
	public static final String ADJUSTED       = "Adjusted";
	public static final String DEPRECATED     = "Deprecated";
	public static final String BASIC          = "Basic";
	public static final String DATA_TYPE      = "DataType";
	public static final String DESCRIPTION    = "Description";
	public static final String UPDATE_COMMENT = "UpdateComment";
	public static final String CONFIG_ID      = "ConfigId";

	public static final String SECTION_BASIC      = "Basic";
	public static final String SECTION_ADVANCED   = "Advanced";
	public static final String SECTION_DEPRECATED = "Deprecated";
	public static final String SECTION_HIDDEN     = "Hidden";

	private static final int COL_NAME        = 0;
	private static final int COL_EDITABLE    = 1;
	private static final int COL_CLASS       = 2;
	private static final int COL_JDBC_TYPE   = 3;
	private static final int COL_JDBC_LENGTH = 4;
	private static final int COL_JDBC_SCALE  = 5;
	private static final int COL_TOOLTIP     = 6;
	private static Object[][] COLUMN_HEADERS =
	{
	//   ColumnName,      Editable, JTable type,   JDBC Type      JDBC Length JDBC Scale  Tooltip
	//   ---------------- --------- -------------- -------------- ----------- ----------  --------------------------------------------
		{NON_DEFAULT,     false,    Boolean.class, Types.BOOLEAN, -1,         -1,         "<html>The parameter has been set: it was in the spfile (or pfile) when the instance started, or it has been changed with ALTER SYSTEM after that.<br>Note: a parameter that is set to the same value as the default is also 'set'. <br><b>Source</b>: V$SYSTEM_PARAMETER: ISDEFAULT = 'FALSE' or ISMODIFIED is not 'FALSE'</html>"},
		{SECTION_NAME,    true,     String .class, Types.VARCHAR, 30,         -1,         "<html>Oracle has no groups for the parameters, so this is: <ul><li><b>Basic</b> - the about 30 parameters that Oracle says you normally need to look at.</li><li><b>Advanced</b> - all the others.</li><li><b>Deprecated</b> - still works, but should not be used any more.</li><li><b>Hidden</b> - the name starts with an underscore. Only shown if it has been set. Only set them when Oracle Support tells you to.</li></ul></html>"},
		{CONFIG_NAME,     true,     String .class, Types.VARCHAR, 80,         -1,         "Name of the initialization parameter."},
		{CONFIG_VALUE,    true,     String .class, Types.VARCHAR, 4000,       -1,         "<html>The value the instance is running with.<br>A size is in bytes here, see 'DisplayValue' for the short form.<br>A parameter that is a list (like control_files) has the values separated with a comma.</html>"},
		{DISPLAY_VALUE,   true,     String .class, Types.VARCHAR, 4000,       -1,         "<html>The value in the form Oracle shows it with <code>show parameter</code>, for example <code>1536M</code> instead of 1610612736.</html>"},
		{PENDING,         false,    Boolean.class, Types.BOOLEAN, -1,         -1,         "<html>The spfile has another value than the instance: it takes effect at the next restart.<br>For numbers, only when the spfile value is <b>higher</b>: Oracle raises and rounds numbers by itself (memory sizes, or 'sessions' from 'processes'), so a lower value in the spfile is normally not a pending change.<br>Compare 'Value' and 'SpfileValue' to see for yourself.</html>"},
		{SPFILE_VALUE,    true,     String .class, Types.VARCHAR, 4000,       -1,         "<html>The value in the spfile, empty if the parameter is not in the spfile.<br><b>Source</b>: V$SPPARAMETER where ISSPECIFIED = 'TRUE'<br>Note: when connected to a PDB, this is what is set for that PDB only.</html>"},
		{DEFAULT_VALUE,   true,     String .class, Types.VARCHAR, 255,        -1,         "<html>The default value of the parameter. Empty in Oracle 11.2 and 12.1 (the column does not exist there).</html>"},
		{RESTART_IS_REQ,  false,    Boolean.class, Types.BOOLEAN, -1,         -1,         "<html>A <b>static</b> parameter: it can only be changed in the spfile (<code>ALTER SYSTEM SET ... SCOPE=SPFILE</code>), and the instance must be restarted.<br><b>Source</b>: V$SYSTEM_PARAMETER.ISSYS_MODIFIABLE = 'FALSE'</html>"},
		{SYS_MODIFIABLE,  true,     String .class, Types.VARCHAR, 10,         -1,         "<html>Can it be changed with ALTER SYSTEM while the instance is running?<ul><li><b>IMMEDIATE</b> - yes, and it takes effect at once.</li><li><b>DEFERRED</b> - yes, but only for sessions that connect after the change.</li><li><b>FALSE</b> - no, a restart is needed.</li></ul></html>"},
		{SES_MODIFIABLE,  false,    Boolean.class, Types.BOOLEAN, -1,         -1,         "Can be changed for one session with ALTER SESSION."},
		{PDB_MODIFIABLE,  false,    Boolean.class, Types.BOOLEAN, -1,         -1,         "Can be set to another value in a Pluggable Database (Oracle 12.1 and above)."},
		{MODIFIED,        true,     String .class, Types.VARCHAR, 10,         -1,         "<html>Has it been changed since the instance was started?<ul><li><b>FALSE</b> - no.</li><li><b>MODIFIED</b> or <b>SYSTEM_MOD</b> - yes, with ALTER SYSTEM.</li></ul></html>"},
		{ADJUSTED,        false,    Boolean.class, Types.BOOLEAN, -1,         -1,         "Oracle adjusted the value that was given to something that fits better."},
		{DEPRECATED,      false,    Boolean.class, Types.BOOLEAN, -1,         -1,         "The parameter is deprecated: it still works, but should not be used any more."},
		{BASIC,           false,    Boolean.class, Types.BOOLEAN, -1,         -1,         "One of the about 30 'basic' parameters: the ones Oracle says you normally need to look at."},
		{DATA_TYPE,       true,     String .class, Types.VARCHAR, 20,         -1,         "Boolean, String, Integer, File or Big integer."},
		{DESCRIPTION,     true,     String .class, Types.VARCHAR, 255,        -1,         "Oracle's description of the parameter."},
		{UPDATE_COMMENT,  true,     String .class, Types.VARCHAR, 255,        -1,         "The comment that was given when the parameter was last changed (ALTER SYSTEM ... COMMENT='...')."},
		{CONFIG_ID,       true,     Integer.class, Types.INTEGER, -1,         -1,         "Oracle's number for the parameter (V$SYSTEM_PARAMETER.NUM)."}
	};

	private static String GET_CONFIG_OFFLINE_SQL =
		"select * " +
		"from [" + PersistWriterJdbc.getTableName(null, null, PersistWriterJdbc.SESSION_DBMS_CONFIG, null, false) + "] \n" +
		"where [SessionStartTime] = SESSION_START_TIME \n";

	private static String GET_CONFIG_OFFLINE_MAX_SESSION_SQL =
		" (select max([SessionStartTime]) " +
		"  from [" + PersistWriterJdbc.getTableName(null, null, PersistWriterJdbc.SESSION_DBMS_CONFIG, null, false) + "]" +
		" ) ";

	/** hashtable with MonTableEntry */
	private HashMap<String,OracleConfigEntry> _configMap  = null;
	private ArrayList<OracleConfigEntry>      _configList = null;

	private ArrayList<String>                 _configSectionList = null;

	public class OracleConfigEntry
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
		public String  displayValue;
		public boolean isPending;
		public String  spfileValue;
		public String  defaultValue;
		public boolean requiresRestart;
		public String  sysModifiable;
		public boolean isSessionModifiable;
		public boolean isPdbModifiable;
		public String  modified;
		public boolean isAdjusted;
		public boolean isDeprecated;
		public boolean isBasic;
		public String  dataType;
		public String  description;
		public String  updateComment;
		public int     configId;
	}

	public OracleConfig()
	{
		DbmsConfigTextManager.clear();
		OracleConfigText.createAndRegisterAllInstances();
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

		_configMap         = new HashMap<String,OracleConfigEntry>();
		_configList        = new ArrayList<OracleConfigEntry>();
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
				// ONLINE: 1 - What is in the spfile
				//   - one row per value: a parameter that is a list has several rows, in ORDINAL order
				//   - SID '*' is for all instances, otherwise it's for one instance (RAC): the entry for our instance wins
				//   - if it can't be read we just get no spfile values
				//-------------------------------------------------------
				Map<String, String> spfileForAll      = new HashMap<>();
				Map<String, String> spfileForInstance = new HashMap<>();

				sql = "select SID, NAME, VALUE from V$SPPARAMETER where ISSPECIFIED = 'TRUE' and SID in ('*', sys_context('userenv', 'instance_name')) order by NAME, SID, ORDINAL";
				try (ResultSet rs = stmt.executeQuery(sql))
				{
					while ( rs.next() )
					{
						String sid   = rs.getString(1);
						String name  = rs.getString(2);
						String value = rs.getString(3) == null ? "" : rs.getString(3);

						Map<String, String> map = "*".equals(sid) ? spfileForAll : spfileForInstance;
						map.put(name, map.containsKey(name) ? map.get(name) + ", " + value : value);
					}
				}
				catch (SQLException ex)
				{
					_logger.info("OracleConfig: could not read the spfile values from V$SPPARAMETER, continuing without them. Caught: " + ex.getMessage().trim());
				}

				//-------------------------------------------------------
				// ONLINE: 2 - The parameters of the INSTANCE
				//   The newest SQL first: the first one that this Oracle version accepts is used
				//   In a Container Database: CON_ID 0 is the value for the whole CDB, and a PDB has it's own rows for what can be set in a PDB
				//-------------------------------------------------------
				String sqlColumns = ""
					+ "select SYSDATE as CONFIG_DATE \n"
					+ "      ,p.NUM \n"
					+ "      ,p.NAME \n"
					+ "      ,p.TYPE \n"
					+ "      ,p.VALUE \n"
					+ "      ,p.DISPLAY_VALUE \n"
					+ "      ,p.ISDEFAULT \n"
					+ "      ,p.ISSES_MODIFIABLE \n"
					+ "      ,p.ISSYS_MODIFIABLE \n"
					+ "      ,p.ISMODIFIED \n"
					+ "      ,p.ISADJUSTED \n"
					+ "      ,p.ISDEPRECATED \n"
					+ "      ,p.ISBASIC \n"
					+ "      ,p.DESCRIPTION \n"
					+ "      ,p.UPDATE_COMMENT \n";

				String[] sqlCandidates = {
					// 12.2 and above: DEFAULT_VALUE
					sqlColumns
						+ "      ,p.ISPDB_MODIFIABLE \n"
						+ "      ,p.DEFAULT_VALUE \n"
						+ "from V$SYSTEM_PARAMETER p \n"
						+ "where p.CON_ID in (0, sys_context('userenv', 'con_id')) \n"
						+ "order by p.NAME",
					// 12.1: containers
					sqlColumns
						+ "      ,p.ISPDB_MODIFIABLE \n"
						+ "      ,CAST(NULL as VARCHAR2(255)) as DEFAULT_VALUE \n"
						+ "from V$SYSTEM_PARAMETER p \n"
						+ "where p.CON_ID in (0, sys_context('userenv', 'con_id')) \n"
						+ "order by p.NAME",
					// 11.2
					sqlColumns
						+ "      ,'FALSE' as ISPDB_MODIFIABLE \n"
						+ "      ,CAST(NULL as VARCHAR2(255)) as DEFAULT_VALUE \n"
						+ "from V$SYSTEM_PARAMETER p \n"
						+ "order by p.NAME"
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

				while ( rs.next() )
				{
					OracleConfigEntry entry = new OracleConfigEntry();

					_timestamp                = rs.getTimestamp(1);
					entry.configId            = rs.getInt      (2);
					entry.configName          = rs.getString   (3);
					int    type               = rs.getInt      (4);
					entry.configValue         = rs.getString   (5);
					entry.displayValue        = rs.getString   (6);
					boolean isDefault         = "TRUE" .equals(rs.getString(7));
					entry.isSessionModifiable = "TRUE" .equals(rs.getString(8));
					entry.sysModifiable       = rs.getString   (9);
					entry.modified            = rs.getString   (10);
					entry.isAdjusted          = "TRUE" .equals(rs.getString(11));
					entry.isDeprecated        = "TRUE" .equals(rs.getString(12));
					entry.isBasic             = "TRUE" .equals(rs.getString(13));
					entry.description         = rs.getString   (14);
					entry.updateComment       = rs.getString   (15);
					entry.isPdbModifiable     = "TRUE" .equals(rs.getString(16));
					entry.defaultValue        = rs.getString   (17);

					// Should not happen, but the name is our key
					if (entry.configName == null || _configMap.containsKey(entry.configName))
						continue;

					entry.requiresRestart = "FALSE".equals(entry.sysModifiable);

					// ISDEFAULT only says if it was in the parameter file when the instance STARTED: a change with ALTER SYSTEM after that leaves it at TRUE
					// (seen on 23: 'alter system set plsql_debug = TRUE scope=memory' gave ISDEFAULT=TRUE, ISMODIFIED=MODIFIED)
					entry.isNonDefault = ! isDefault || (entry.modified != null && ! "FALSE".equals(entry.modified));

					if      (type == 1) entry.dataType = "Boolean";
					else if (type == 2) entry.dataType = "String";
					else if (type == 3) entry.dataType = "Integer";
					else if (type == 4) entry.dataType = "File";
					else if (type == 6) entry.dataType = "Big integer";
					else                entry.dataType = type + "";

					// Oracle has no groups for the parameters, so make the 4 that it can tell us about
					if      (entry.isDeprecated)               entry.sectionName = SECTION_DEPRECATED;
					else if (entry.configName.startsWith("_")) entry.sectionName = SECTION_HIDDEN;
					else if (entry.isBasic)                    entry.sectionName = SECTION_BASIC;
					else                                       entry.sectionName = SECTION_ADVANCED;

					// What the spfile says (the entry for our instance wins)
					entry.spfileValue = spfileForInstance.containsKey(entry.configName) ? spfileForInstance.get(entry.configName) : spfileForAll.get(entry.configName);

					// Pending: the spfile has another value than the instance, so it takes effect at the next restart
					//   - text and boolean values: when they differ
					//   - numbers: only when the spfile value is HIGHER. Oracle raises and rounds numbers by itself
					//     (a memory size to the next granule, 'sessions' from 'processes'), so a lower value in the spfile is normally not a pending change
					String value        = entry.configValue  == null ? "" : entry.configValue;
					String displayValue = entry.displayValue == null ? "" : entry.displayValue;
					if (entry.spfileValue != null && ! entry.spfileValue.equalsIgnoreCase(value) && ! entry.spfileValue.equalsIgnoreCase(displayValue))
					{
						Long spfileNumber = toNumberOrNull(entry.spfileValue);
						Long valueNumber  = toNumberOrNull(value);

						if (spfileNumber != null && valueNumber != null)
							entry.isPending = spfileNumber > valueNumber;
						else
							entry.isPending = true;
					}

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
				String expectedType = OracleTune.APP_NAME;
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

				// Recordings made before the parameter table was redone has the name in 'ConfigName' and the value in 'CfgValStr'
				String colName  = columns.contains(CONFIG_NAME .toLowerCase()) ? CONFIG_NAME  : "ConfigName";
				String colValue = columns.contains(CONFIG_VALUE.toLowerCase()) ? CONFIG_VALUE : "CfgValStr";

				while ( rs.next() )
				{
					OracleConfigEntry entry = new OracleConfigEntry();

					_timestamp = rs.getTimestamp(1);

					if (columns.contains(NON_DEFAULT   .toLowerCase())) entry.isNonDefault        = rs.getBoolean( 1 + columns.indexOf(NON_DEFAULT   .toLowerCase()) );
					if (columns.contains(SECTION_NAME  .toLowerCase())) entry.sectionName         = rs.getString ( 1 + columns.indexOf(SECTION_NAME  .toLowerCase()) );
					if (columns.contains(colName       .toLowerCase())) entry.configName          = rs.getString ( 1 + columns.indexOf(colName       .toLowerCase()) );
					if (columns.contains(colValue      .toLowerCase())) entry.configValue         = rs.getString ( 1 + columns.indexOf(colValue      .toLowerCase()) );
					if (columns.contains(DISPLAY_VALUE .toLowerCase())) entry.displayValue        = rs.getString ( 1 + columns.indexOf(DISPLAY_VALUE .toLowerCase()) );
					if (columns.contains(PENDING       .toLowerCase())) entry.isPending           = rs.getBoolean( 1 + columns.indexOf(PENDING       .toLowerCase()) );
					if (columns.contains(SPFILE_VALUE  .toLowerCase())) entry.spfileValue         = rs.getString ( 1 + columns.indexOf(SPFILE_VALUE  .toLowerCase()) );
					if (columns.contains(DEFAULT_VALUE .toLowerCase())) entry.defaultValue        = rs.getString ( 1 + columns.indexOf(DEFAULT_VALUE .toLowerCase()) );
					if (columns.contains(RESTART_IS_REQ.toLowerCase())) entry.requiresRestart     = rs.getBoolean( 1 + columns.indexOf(RESTART_IS_REQ.toLowerCase()) );
					if (columns.contains(SYS_MODIFIABLE.toLowerCase())) entry.sysModifiable       = rs.getString ( 1 + columns.indexOf(SYS_MODIFIABLE.toLowerCase()) );
					if (columns.contains(SES_MODIFIABLE.toLowerCase())) entry.isSessionModifiable = rs.getBoolean( 1 + columns.indexOf(SES_MODIFIABLE.toLowerCase()) );
					if (columns.contains(PDB_MODIFIABLE.toLowerCase())) entry.isPdbModifiable     = rs.getBoolean( 1 + columns.indexOf(PDB_MODIFIABLE.toLowerCase()) );
					if (columns.contains(MODIFIED      .toLowerCase())) entry.modified            = rs.getString ( 1 + columns.indexOf(MODIFIED      .toLowerCase()) );
					if (columns.contains(ADJUSTED      .toLowerCase())) entry.isAdjusted          = rs.getBoolean( 1 + columns.indexOf(ADJUSTED      .toLowerCase()) );
					if (columns.contains(DEPRECATED    .toLowerCase())) entry.isDeprecated        = rs.getBoolean( 1 + columns.indexOf(DEPRECATED    .toLowerCase()) );
					if (columns.contains(BASIC         .toLowerCase())) entry.isBasic             = rs.getBoolean( 1 + columns.indexOf(BASIC         .toLowerCase()) );
					if (columns.contains(DATA_TYPE     .toLowerCase())) entry.dataType            = rs.getString ( 1 + columns.indexOf(DATA_TYPE     .toLowerCase()) );
					if (columns.contains(DESCRIPTION   .toLowerCase())) entry.description         = rs.getString ( 1 + columns.indexOf(DESCRIPTION   .toLowerCase()) );
					if (columns.contains(UPDATE_COMMENT.toLowerCase())) entry.updateComment       = rs.getString ( 1 + columns.indexOf(UPDATE_COMMENT.toLowerCase()) );
					if (columns.contains(CONFIG_ID     .toLowerCase())) entry.configId            = rs.getInt    ( 1 + columns.indexOf(CONFIG_ID     .toLowerCase()) );

					if (entry.configName == null)
						continue;

					// An old recording has no section
					if (entry.sectionName == null)
						entry.sectionName = "";

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
			_logger.error("OracleConfig:initialize:sql='" + sql + "'", ex);
			if (_hasGui)
				SwingUtils.showErrorMessage("OracleConfig - Initialize", "SQL Exception: " + ex.getMessage() + "\n\nThis was found when executing SQL statement:\n\n" + sql, ex);
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
	 * '300' -> 300, '2G' -> 2147483648, anything that is not a number -> null<br>
	 * In the spfile a size can be written with K, M, G or T at the end.
	 */
	private static Long toNumberOrNull(String str)
	{
		if (str == null || ! str.trim().matches("[0-9]+[kKmMgGtT]?"))
			return null;

		str = str.trim().toUpperCase();
		long multiplier = 1;
		if      (str.endsWith("K")) multiplier = 1024L;
		else if (str.endsWith("M")) multiplier = 1024L * 1024;
		else if (str.endsWith("G")) multiplier = 1024L * 1024 * 1024;
		else if (str.endsWith("T")) multiplier = 1024L * 1024 * 1024 * 1024;

		if (multiplier > 1)
			str = str.substring(0, str.length() - 1);

		try { return Long.parseLong(str) * multiplier; }
		catch (NumberFormatException ex) { return null; }
	}


	/**
	 * Look for things in the configuration that a DBA normally wants to know about.
	 * <p>
	 * Every check is on it's own: if one of them can't get it's data (a missing privilege, a standby that is only mounted...) the others are still done.
	 */
	@Override
	public void checkConfig(DbxConnection conn)
	{
		if (_configMap == null)
			return;

		String srvName = "-UNKNOWN-";
		try { srvName = conn.getDbmsServerName(); } catch (SQLException ex) { _logger.info("Problems getting Oracle instance name. ex=" + ex);}

		String keyPrefix = "DbmsConfigIssue." + srvName + ".oracle.";

		//-------------------------------------------------------
		// Facts about the database: one row
		//-------------------------------------------------------
		Timestamp srvRestart     = null;
		String    logMode        = null;
		int       controlFiles   = -1;
		int       redoGroups     = -1; // for the redo thread that has fewest
		int       redoMinMembers = -1;

		String sql = ""
			+ "select (select STARTUP_TIME from V$INSTANCE)                       as STARTUP_TIME \n"
			+ "      ,d.LOG_MODE \n"
			+ "      ,(select count(*) from V$CONTROLFILE)                        as CONTROL_FILES \n"
			+ "      ,(select min(count(*)) from V$LOG group by THREAD#)          as REDO_GROUPS \n"
			+ "      ,(select min(MEMBERS) from V$LOG)                            as REDO_MIN_MEMBERS \n"
			+ "from V$DATABASE d";

		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while (rs.next())
			{
				srvRestart     = rs.getTimestamp(1);
				logMode        = rs.getString   (2);
				controlFiles   = rs.getInt      (3);
				redoGroups     = rs.getInt      (4);
				redoMinMembers = rs.getInt      (5);
			}
		}
		catch (SQLException ex)
		{
			_logger.info("OracleConfig.checkConfig(): Skipping the checks for archive log mode, control files and redo logs. Caught: " + ex.getMessage().trim());
		}

		//-------------------------------------------------------
		if ("NOARCHIVELOG".equals(logMode))
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "log_mode.noarchivelog", "LOG_MODE", Severity.WARNING,
					"The database is in NOARCHIVELOG mode. It can only be restored to the time of the last backup (everything after that is lost), and a backup can only be taken when the database is closed.",
					"If that is not what you want (for a test database it can be): SHUTDOWN IMMEDIATE; STARTUP MOUNT; ALTER DATABASE ARCHIVELOG; ALTER DATABASE OPEN; And then make sure the archived logs are backed up and removed (RMAN)."));
		}

		//-------------------------------------------------------
		if (controlFiles == 1)
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "control_files.only_one", "control_files", Severity.WARNING,
					"The database has only ONE control file. If that file is lost or damaged the database can't be opened, and it has to be restored or re-created.",
					"Add a second control file, on another disk: ALTER SYSTEM SET control_files = '<file1>','<file2>' SCOPE=SPFILE; shut down, copy the control file to <file2>, start."));
		}

		//-------------------------------------------------------
		if (redoGroups > 0 && redoGroups < 3)
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "redo.less_than_3_groups", "redo log groups", Severity.WARNING,
					"The database has only " + redoGroups + " redo log groups. With 2 groups the database stops every time a log switch has to wait: for the checkpoint, or for the archiver.",
					"Add a group (or more): ALTER DATABASE ADD LOGFILE GROUP <n> ('<file>') SIZE <same size as the others>;"));
		}

		//-------------------------------------------------------
		if (redoMinMembers == 1)
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "redo.one_member", "redo log members", Severity.INFO,
					"There are redo log groups with only ONE member. If the file of the CURRENT group is lost, committed transactions are lost.",
					"If the storage does not already mirror the files (ASM redundancy, a SAN): add a member on another disk: ALTER DATABASE ADD LOGFILE MEMBER '<file>' TO GROUP <n>;"));
		}

		//-------------------------------------------------------
		// Parameters: go through them once and collect the names
		//-------------------------------------------------------
		List<String> deprecatedSet = new ArrayList<>();
		List<String> hiddenSet     = new ArrayList<>();
		List<String> pending       = new ArrayList<>();
		for (OracleConfigEntry entry : _configList)
		{
			if (entry.isDeprecated && entry.isNonDefault)                   deprecatedSet.add(entry.configName);
			if (entry.configName.startsWith("_") && entry.isNonDefault)     hiddenSet    .add(entry.configName);
			if (entry.isPending)                                            pending      .add(entry.configName + " (now: " + entry.configValue + ", spfile: " + entry.spfileValue + ")");
		}

		//-------------------------------------------------------
		if ( ! deprecatedSet.isEmpty() )
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "parameters.deprecated", "deprecated parameters", Severity.INFO,
					"Deprecated parameters are set: " + StringUtil.toCommaStr(deprecatedSet) + ". They still work, but Oracle can remove them in a later version.",
					"Look up what replaces each of them, and remove it from the spfile: ALTER SYSTEM RESET <name> SCOPE=SPFILE SID='*';"));
		}

		//-------------------------------------------------------
		if ( ! hiddenSet.isEmpty() )
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "parameters.hidden", "hidden parameters", Severity.INFO,
					"Hidden parameters (the name starts with an underscore) are set: " + StringUtil.toCommaStr(hiddenSet) + ". They are not documented, and should only be set when Oracle Support says so.",
					"Check why each of them was set (see column 'UpdateComment'). If it's no longer needed: ALTER SYSTEM RESET \"<name>\" SCOPE=SPFILE SID='*';"));
		}

		//-------------------------------------------------------
		if ( ! pending.isEmpty() )
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "parameters.pending", "pending parameters", Severity.INFO,
					"The spfile has other values than the running instance for: " + StringUtil.toCommaStr(pending) + ". The instance gets the spfile values at the next restart.",
					"Nothing to fix if the restart is planned. Otherwise decide which value is the right one, and set it with ALTER SYSTEM SET <name> = <value> SCOPE=SPFILE (or BOTH)."));
		}

		//-------------------------------------------------------
		OracleConfigEntry entry = _configMap.get("spfile");
		if (entry != null && StringUtil.isNullOrBlank(entry.configValue))
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "spfile.not_used", "spfile", Severity.INFO,
					"The instance was started with a text parameter file (pfile), not a server parameter file (spfile). A change with ALTER SYSTEM is then lost at the next restart, unless the file is also edited by hand.",
					"CREATE SPFILE FROM PFILE; and restart the instance."));
		}

		//-------------------------------------------------------
		entry = _configMap.get("statistics_level");
		if (entry != null && "BASIC".equalsIgnoreCase(entry.configValue))
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "statistics_level.basic", "statistics_level", Severity.WARNING,
					"The parameter 'statistics_level' is BASIC. That turns off, among other things: the tracking of table changes (so the automatic optimizer statistics job does not know what is stale), the memory advisors and the automatic memory tuning, and timed statistics.",
					"ALTER SYSTEM SET statistics_level = TYPICAL SCOPE=BOTH;"));
		}

		//-------------------------------------------------------
		entry = _configMap.get("db_block_checksum");
		if (entry != null && ("OFF".equalsIgnoreCase(entry.configValue) || "FALSE".equalsIgnoreCase(entry.configValue)))
		{
			DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "db_block_checksum.off", "db_block_checksum", Severity.WARNING,
					"The parameter 'db_block_checksum' is OFF. A block that is damaged by the disk or the storage is then not detected when it's read (and it gets into the backups).",
					"ALTER SYSTEM SET db_block_checksum = TYPICAL SCOPE=BOTH;"));
		}

		//-------------------------------------------------------
		// compatible lower than the software: '11.2.0.0.0' and version '11.2.0.2.0' is fine, '12.1.0' on a 19 is not
		//-------------------------------------------------------
		entry = _configMap.get("compatible");
		long srvVersion = conn.getDbmsVersionNumber(); // 0 if we don't know
		if (entry != null && entry.configValue != null && srvVersion > 0)
		{
			String[] parts = entry.configValue.split("\\.");
			int compatMajor = StringUtil.parseInt(parts[0], -1);
			int compatMinor = parts.length > 1 ? StringUtil.parseInt(parts[1], 0) : 0;

			int srvMajor = Ver.versionNumPart(srvVersion, Ver.VERSION_MAJOR);
			int srvMinor = Ver.versionNumPart(srvVersion, Ver.VERSION_MINOR);

			// From 18 and up the second number is the Release Update, and 'compatible' only has the first
			boolean isLower = compatMajor != -1 && (compatMajor < srvMajor || (srvMajor < 18 && compatMajor == srvMajor && compatMinor < srvMinor));
			if (isLower)
			{
				DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "compatible.lower_than_version", "compatible", Severity.INFO,
						"The parameter 'compatible' is " + entry.configValue + ", which is lower than the Oracle software version (" + srvMajor + "." + srvMinor + "). New features of this version are not available until it is raised.",
						"When you are sure that you will not go back to the old version: ALTER SYSTEM SET compatible = '<version>' SCOPE=SPFILE; and restart. It can NOT be lowered again."));
			}
		}

		//-------------------------------------------------------
		// Components that are INVALID
		//-------------------------------------------------------
		sql = "select COMP_NAME, VERSION from DBA_REGISTRY where STATUS = 'INVALID' order by COMP_NAME";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			List<String> invalid = new ArrayList<>();
			while (rs.next())
				invalid.add(rs.getString(1) + " (" + rs.getString(2) + ")");

			if ( ! invalid.isEmpty() )
			{
				DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "registry.invalid", "DBA_REGISTRY", Severity.WARNING,
						"Database components are INVALID: " + StringUtil.toCommaStr(invalid) + ". This is normally what is left after an upgrade or a patch that did not finish.",
						"As SYS: @?/rdbms/admin/utlrp.sql (recompiles invalid objects), then look at DBA_REGISTRY again. If it is still INVALID: see the log of the upgrade or the patch."));
			}
		}
		catch (SQLException ex)
		{
			_logger.info("OracleConfig.checkConfig(): Skipping the check for invalid components. Caught: " + ex.getMessage().trim());
		}

		//-------------------------------------------------------
		// Automatic optimizer statistics
		//-------------------------------------------------------
		sql = "select STATUS from DBA_AUTOTASK_CLIENT where CLIENT_NAME = 'auto optimizer stats collection'";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while (rs.next())
			{
				String status = rs.getString(1);
				if ( ! "ENABLED".equals(status) )
				{
					DbmsConfigManager.getInstance().addConfigIssue(new DbmsConfigIssue(srvRestart, keyPrefix + "autotask.optimizer_stats.disabled", "auto optimizer stats collection", Severity.WARNING,
							"The automatic job that gathers optimizer statistics is " + status + ". Unless something else gathers the statistics, they get old, and the optimizer starts to choose bad execution plans.",
							"If no other job does it: exec DBMS_AUTO_TASK_ADMIN.ENABLE(client_name => 'auto optimizer stats collection', operation => NULL, window_name => NULL);"));
				}
			}
		}
		catch (SQLException ex)
		{
			_logger.info("OracleConfig.checkConfig(): Skipping the check for automatic optimizer statistics. Caught: " + ex.getMessage().trim());
		}
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

		OracleConfigEntry e = _configMap.get(configName);

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
		OracleConfigEntry e = _configList.get(rowIndex);
		if (e == null)
			return null;

		// NOTE: same order as in COLUMN_HEADERS
		switch (columnIndex)
		{
		case 0:  return e.isNonDefault;
		case 1:  return e.sectionName;
		case 2:  return e.configName;
		case 3:  return e.configValue;
		case 4:  return e.displayValue;
		case 5:  return e.isPending;
		case 6:  return e.spfileValue;
		case 7:  return e.defaultValue;
		case 8:  return e.requiresRestart;
		case 9:  return e.sysModifiable;
		case 10: return e.isSessionModifiable;
		case 11: return e.isPdbModifiable;
		case 12: return e.modified;
		case 13: return e.isAdjusted;
		case 14: return e.isDeprecated;
		case 15: return e.isBasic;
		case 16: return e.dataType;
		case 17: return e.description;
		case 18: return e.updateComment;
		case 19: return e.configId;
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
		return "Parameters";
	}

	@Override
	public String getCellToolTip(int mrow, int mcol)
	{
		OracleConfigEntry e = _configList.get(mrow);
		if (e == null)
			return null;

		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("<b>Description:   </b>").append(e.description) .append("<br>");
		sb.append("<b>Parameter:     </b>").append(e.configName)  .append("<br>");
		sb.append("<b>Value:         </b>").append(e.configValue);
		if (e.displayValue != null && ! e.displayValue.equals(e.configValue))
			sb.append(" &nbsp; (").append(e.displayValue).append(")");
		sb.append("<br>");
		sb.append("<b>Spfile Value:  </b>").append(e.spfileValue  == null ? "<i>not in the spfile</i>" : e.spfileValue) .append("<br>");
		sb.append("<b>Default Value: </b>").append(e.defaultValue == null ? ""                         : e.defaultValue).append("<br>");
		sb.append("<b>Change it:     </b>").append(e.requiresRestart ? "static: in the spfile, and then a restart" : "DEFERRED".equals(e.sysModifiable) ? "dynamic, but only for new sessions (DEFERRED)" : "dynamic (ALTER SYSTEM)").append("<br>");
		if (e.isPending)
		{
			sb.append("<br>");
			sb.append("<b>NOTE: The spfile has another value than the instance. It takes effect at the next restart.</b>").append("<br>");
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
	 * One <code>ALTER SYSTEM SET</code> statement, with some comment lines above it.<br>
	 * The statement ends with <code>go</code>: that is the terminator in the SQL Window. For SQL*Plus: replace it with <code>;</code>
	 */
	private String toAlterSystem(OracleConfigEntry e, String value, String prevValue)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("---------------------------------------------------------------------------\n");
		sb.append("-- Parameter:     ").append(e.configName)  .append("\n");
		sb.append("-- Description:   ").append(e.description) .append("\n");
		sb.append("-- Default Value: ").append(e.defaultValue == null ? "" : e.defaultValue).append("\n");
		if (prevValue != null)
			sb.append("-- Prev Value:    ").append(prevValue).append("\n");

		// Text values in quotes, numbers and booleans as they are
		boolean isText = "String".equals(e.dataType) || "File".equals(e.dataType);
		String  valStr = value == null ? "" : value;
		if (isText)
			valStr = "'" + valStr.replace("'", "''") + "'";

		if (isText && value != null && value.contains(", "))
			sb.append("-- NOTE: If this is a LIST of values (like control_files) write it as: 'value1','value2'\n");

		// static: only in the spfile + restart.   DEFERRED: only for new sessions
		String option = " SCOPE=BOTH";
		if (e.requiresRestart)
		{
			option = " SCOPE=SPFILE";
			sb.append("-- NOTE: This is a static parameter: it takes effect at the next restart of the instance.\n");
		}
		else if ("DEFERRED".equals(e.sysModifiable))
		{
			option = " DEFERRED SCOPE=BOTH";
			sb.append("-- NOTE: This only takes effect for sessions that connects after the change.\n");
		}

		sb.append("ALTER SYSTEM SET ").append(e.configName.startsWith("_") ? "\"" + e.configName + "\"" : e.configName).append(" = ").append(valStr).append(option).append("\n");
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
		sb.append("** At Date:            ").append(new Timestamp(System.currentTimeMillis())).append("\n");
		sb.append("** Oracle Server name: ").append(getDbmsServerName()).append("\n");
		sb.append("** Oracle Version:     ").append((getDbmsVersionStr() + "").replace('\n', ' ')).append("\n");
		sb.append("** Note:               'go' ends a statement in the SQL Window. For SQL*Plus: replace it with ';'\n");
		sb.append("*/ \n");
		sb.append("\n");

		for (int r=0; r<modelRows.length; r++)
		{
			OracleConfigEntry e = _configList.get(modelRows[r]);

			sb.append(toAlterSystem(e, e.configValue, null));
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
		sb.append("** At Date:            ").append(new Timestamp(System.currentTimeMillis())).append("\n");
		sb.append("** Oracle Server name: ").append(getDbmsServerName()).append("\n");
		sb.append("** Oracle Version:     ").append((getDbmsVersionStr() + "").replace('\n', ' ')).append("\n");
		if (StringUtil.hasValue(comment))
			sb.append("** Comment:            ").append(comment).append("\n");
		sb.append("** Note:               'go' ends a statement in the SQL Window. For SQL*Plus: replace it with ';'\n");
		sb.append("*/ \n");
		sb.append("\n");

		for (Entry<String, String> kv : keyValMap.entrySet())
		{
			OracleConfigEntry e = _configMap.get(kv.getKey());
			if (e == null)
			{
				sb.append("-- The parameter '").append(kv.getKey()).append("' does not exist in this instance, so nothing is done for it.\n\n");
				continue;
			}

			sb.append(toAlterSystem(e, kv.getValue(), e.configValue));
		}

		return sb.toString();
	}
}
