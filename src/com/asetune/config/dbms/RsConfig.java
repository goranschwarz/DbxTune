/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.config.dbms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.asetune.RsTune;
import com.asetune.pcs.MonRecordingInfo;
import com.asetune.pcs.PersistWriterJdbc;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.RepServerUtils;
import com.asetune.utils.RepServerUtils.ConfigEntry;
import com.asetune.utils.SwingUtils;


public class RsConfig
extends DbmsConfigAbstract 
{
	private static final long serialVersionUID = 1L;

	/** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(RsConfig.class);

	private boolean   _offline   = false;
	private boolean   _hasGui    = false;
	private Timestamp _timestamp = null;

// 1> admin config, 'table', GS1, hana
// RS> Col# Label         JDBC Type Name      Guessed DBMS type
// RS> ---- ------------- ------------------- -----------------
// RS> 1    Configuration java.sql.Types.CHAR char(31)         
// RS> 2    Config Value  java.sql.Types.CHAR char(255)        
// RS> 3    Run Value     java.sql.Types.CHAR char(255)        
// RS> 4    Default Value java.sql.Types.CHAR char(255)        
// RS> 5    Legal Values  java.sql.Types.CHAR char(255)        
// RS> 6    Datatype      java.sql.Types.CHAR char(255)        
// RS> 7    Status        java.sql.Types.CHAR char(255)        
// RS> 8    Table         java.sql.Types.CHAR char(255)        -- Only available if: admin config, 'table'...
	
	public static final String NON_DEFAULT          = "NonDefault";
	public static final String SECTION_NAME         = "Category";  // GLOBAL, CONNECTION, TABLE, LOGICAL CONNECTION, ROUTE
	public static final String INSTANCE_NAME        = "Name";
//	public static final String CONFIG_TYPE          = "ParameterType";
	public static final String PENDING              = "Pending";
	public static final String RESTART_IS_REQ       = "Restart";
	public static final String DESCRIPTION          = "Description";

	public static final String CONFIG_NAME          = "Configuration";
	public static final String CONFIG_VALUE         = "ConfigValue";
	public static final String RUN_VALUE            = "RunValue";
	public static final String DEFAULT_VALUE        = "DefaultValue";
	public static final String LEGAL_VALUES         = "LegalValues";
	public static final String DATATYPE             = "ConfigDatatype";
	public static final String RESTART_STRING       = "RestartDesc";

	private static final int COL_NAME        = 0;
	private static final int COL_EDITABLE    = 1;
	private static final int COL_CLASS       = 2;
//	private static final int COL_SQLTYPE     = 3;
	private static final int COL_JDBC_TYPE   = 3;
	private static final int COL_JDBC_LENGTH = 4;
	private static final int COL_JDBC_SCALE  = 5;
	private static final int COL_TOOLTIP     = 6;
	private static Object[][] COLUMN_HEADERS = 
	{
	//   ColumnName,           Editable, JTable type,   JDBC Type      JDBC Length JDBC Scale /* SQL Datatype,    */ Tooltip
	//   --------------------- --------- -------------- -------------- ----------- ---------- /* ---------------- */ --------------------------------------------
		{NON_DEFAULT,          false,    Boolean.class, Types.BOOLEAN, -1,         -1,        /* "bit",           */ "True if the value is not configured to the default value."},
		{SECTION_NAME,         true,     String .class, Types.VARCHAR, 60,         -1,        /* "varchar(60)",   */ "Configuration Group"},
		{INSTANCE_NAME,        true,     String .class, Types.VARCHAR, 60,         -1,        /* "varchar(60)",   */ "Instance name, for example a Connection name"},
		{CONFIG_NAME,          true,     String .class, Types.VARCHAR, 60,         -1,        /* "varchar(60)",   */ "Name of the configuration, same as the name in ra_config."},
		{CONFIG_VALUE,         true,     String .class, Types.VARCHAR, 255,        -1,        /* "varchar(255)",  */ "Value of the configuration."},
		{RUN_VALUE,            true,     String .class, Types.VARCHAR, 255,        -1,        /* "varchar(255)",  */ "RUN Value of the configuration."},
		{DEFAULT_VALUE,        true,     String .class, Types.VARCHAR, 255,        -1,        /* "varchar(255)",  */ "The default configuration value."},
		{PENDING,              false,    Boolean.class, Types.BOOLEAN, -1,         -1,        /* "bit",           */ "The Configuration has not yet taken effect, probably needs restart to take effect."},
		{RESTART_IS_REQ,       false,    Boolean.class, Types.BOOLEAN, -1,         -1,        /* "bit",           */ "Needs to be \"rebooted\" for the configuration to take effect."},
		{RESTART_STRING,       false,    String .class, Types.VARCHAR, 255,        -1,        /* "varchar(255)",  */ "Needs to be \"rebooted\" for the configuration to take effect."},
		{LEGAL_VALUES,         true,     String .class, Types.VARCHAR, 255,        -1,        /* "varchar(255)",  */ "What legal values can this configuration hold"},
		{DATATYPE,             true,     String .class, Types.VARCHAR, 255,        -1,        /* "varchar(255)",  */ "What type of parameter is this."},
		{DESCRIPTION,          true,     String .class, Types.VARCHAR, 1024,       -1,        /* "varchar(1024)", */ "Description of the configuration."}
	};

//	private static String GET_CONFIG_ONLINE_SQL = "admin config..."; // This is done directly in refresh

	private static String GET_CONFIG_OFFLINE_SQL = 
		"select * " +
		"from [" + PersistWriterJdbc.getTableName(null, PersistWriterJdbc.SESSION_DBMS_CONFIG, null, false) + " \n" +
		"where [SessionStartTime] = SESSION_START_TIME \n";

	private static String GET_CONFIG_OFFLINE_MAX_SESSION_SQL = 
		" (select max([SessionStartTime]) " +
		"  from ["+PersistWriterJdbc.getTableName(null, PersistWriterJdbc.SESSION_DBMS_CONFIG, null, false) + "]" +
		" ) ";


//	@Override
//	public String getSqlForDiff(boolean isOffline)
//	{
//		return ""; // NOT YET SUPPORTED
//	}
		
	/** hash table */
	private HashMap<String,RsConfigEntry> _configMap  = null;
	private ArrayList<RsConfigEntry>      _configList = null;

	private ArrayList<String>              _configSectionList = null;

	public class RsConfigEntry
	implements IDbmsConfigEntry
	{
		@Override public String  getConfigKey()   { return configName; }
		@Override public String  getConfigValue() { return configValue; }
		@Override public String  getSectionName() { return sectionName; }
		@Override public boolean isPending()      { return isPending; }
		@Override public boolean isNonDefault()   { return isNonDefault; }
		
		/** configuration has been changed by any user */
		public boolean isNonDefault;

		/** Different sections in the configuration */
		public String  sectionName;

		/** Instance name, for example a Connection name */
		public String  instanceName;

		/** Name of the configuration */
		public String  configName;

		/** Value of the configuration */
		public String  configValue;

		/** RUN Value of the configuration. */
		public String  runValue;

		/** String representation of the default configuration */
		public String  defaultValue;

		/** true if the configuration value is set, but we need to restart the ASE server to enable the configuration */
		public boolean isPending;

		/** true if the server needs to be restarted to enable the configuration */
		public boolean requiresRestart;

		/** String representation of the default configuration */
		public String  restartString;

		/** What legal values can this configuration hold */
		public String  legalValues;

		/** What type of parameter is this */
		public String  datatype;

		/** description of the configuration option */
		public String  description;
	}

	public RsConfig()
	{
		DbmsConfigTextManager.clear();
		RsConfigText.createAndRegisterAllInstances();
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
		
		_configMap         = new HashMap<String,RsConfigEntry>();
		_configList        = new ArrayList<RsConfigEntry>();
		_configSectionList = new ArrayList<String>();
		
		try { setDbmsServerName(conn.getDbmsServerName()); } catch (SQLException ex) { setDbmsServerName(ex.getMessage()); };
		try { setDbmsVersionStr(conn.getDbmsVersionStr()); } catch (SQLException ex) { setDbmsVersionStr(ex.getMessage()); };

		try { setLastUsedUrl( conn.getMetaData().getURL() ); } catch(SQLException ignore) { }
		setLastUsedConnProp(conn.getConnPropOrDefault());


		String sql = "";
		try
		{
			// ONLINE GET CONFIG
			Statement stmt = conn.createStatement();
			stmt.setQueryTimeout(10);

			if ( ! _offline )
			{
//				sql = GET_CONFIG_ONLINE_SQL;

				// First simple get a date
				ResultSet rs = stmt.executeQuery("admin time");
				while ( rs.next() )
					_timestamp = rs.getTimestamp(1);
				rs.close();


				Map<String, String> configDescription = RepServerUtils.getConfigDescriptions(conn);

				// REP SERVER Configurations
//				if (waitDialog != null) 
//					waitDialog.setState("Getting Global Configuration");
				List<ConfigEntry> rsConfig = RepServerUtils.getRsConfig(conn);
				for (ConfigEntry ce : rsConfig)
				{
					RsConfigEntry entry = new RsConfigEntry();

					entry.isNonDefault    = ce.isConfigOptionChanged();
					entry.sectionName     = "GLOBAL";
					entry.instanceName    = "GLOBAL";
					entry.configName      = ce._configName;
					entry.configValue     = ce._configValue;
					entry.runValue        = ce._runValue;
					entry.defaultValue    = ce._defaultValue;
					entry.isPending       = ce.isPending();
					entry.requiresRestart = ce.isRestartRequired();
					entry.restartString   = ce._status;
					entry.legalValues     = ce._legalValues;
					entry.datatype        = ce._datatype;
					entry.description     = configDescription.get(ce.getConfigName());

					// Add
					_configMap .put(entry.configName, entry);
					_configList.add(entry);

					// Add it to the section list, if not already there
					if ( ! _configSectionList.contains(entry.sectionName) )
						_configSectionList.add(entry.sectionName);
				}
				

				// CONNECTIONS Configurations
//				if (waitDialog != null) 
//					waitDialog.setState("Getting Physical Connection Configuration");
				List<String> rsDbs = RepServerUtils.getConnections(conn);
				for (String dsdb : rsDbs)
				{
					String[] sa = dsdb.split("\\.");
					String ds = sa[0];
					String db = sa[1];

//					if (waitDialog != null) 
//						waitDialog.setState("Getting Physical Connection Configuration, for '"+ds+"."+db+"'.");

					List<ConfigEntry> config = RepServerUtils.getConnectionConfig(conn, ds, db);
					for (ConfigEntry ce : config)
					{
						RsConfigEntry entry = new RsConfigEntry();

						entry.isNonDefault    = ce.isConfigOptionChanged();
						entry.sectionName     = "CONNECTION";
						entry.instanceName    = dsdb;
						entry.configName      = ce._configName;
						entry.configValue     = ce._configValue;
						entry.runValue        = ce._runValue;
						entry.defaultValue    = ce._defaultValue;
						entry.isPending       = ce.isPending();
						entry.requiresRestart = ce.isRestartRequired();
						entry.restartString   = ce._status;
						entry.legalValues     = ce._legalValues;
						entry.datatype        = ce._datatype;
						entry.description     = configDescription.get(ce.getConfigName());

						// Add
						_configMap .put(entry.configName, entry);
						_configList.add(entry);

						// Add it to the section list, if not already there
						if ( ! _configSectionList.contains(entry.sectionName) )
							_configSectionList.add(entry.sectionName);
					}

					config = RepServerUtils.getTableConnectionConfig(conn, ds, db);
					for (ConfigEntry ce : config)
					{
						RsConfigEntry entry = new RsConfigEntry();

						entry.isNonDefault    = ce.isConfigOptionChanged();
						entry.sectionName     = "TABLE";
						entry.instanceName    = dsdb + " - " + ce.getTableName();
						entry.configName      = ce._configName;
						entry.configValue     = ce._configValue;
						entry.runValue        = ce._runValue;
						entry.defaultValue    = ce._defaultValue;
						entry.isPending       = ce.isPending();
						entry.requiresRestart = ce.isRestartRequired();
						entry.restartString   = ce._status;
						entry.legalValues     = ce._legalValues;
						entry.datatype        = ce._datatype;
						entry.description     = configDescription.get(ce.getConfigName());

						// Add
						_configMap .put(entry.configName, entry);
						_configList.add(entry);

						// Add it to the section list, if not already there
						if ( ! _configSectionList.contains(entry.sectionName) )
							_configSectionList.add(entry.sectionName);
					}
				}


				// LOGICAL CONNECTION Configurations
//				if (waitDialog != null) 
//					waitDialog.setState("Getting Logical Connection Configuration");
				List<String> rsLDbs = RepServerUtils.getLogicalConnections(conn);
				for (String dsdb : rsLDbs)
				{
					String[] sa = dsdb.split("\\.");
					String lds = sa[0];
					String ldb = sa[1];

//					if (waitDialog != null) 
//						waitDialog.setState("Getting Logical Connection Configuration, for '"+lds+"."+ldb+"'.");

					List<ConfigEntry> config = RepServerUtils.getLogicalConnectionConfig(conn, lds, ldb);
					for (ConfigEntry ce : config)
					{
						RsConfigEntry entry = new RsConfigEntry();

						entry.isNonDefault    = ce.isConfigOptionChanged();
						entry.sectionName     = "LOGICAL CONNECTION";
						entry.instanceName    = dsdb;
						entry.configName      = ce._configName;
						entry.configValue     = ce._configValue;
						entry.runValue        = ce._runValue;
						entry.defaultValue    = ce._defaultValue;
						entry.isPending       = ce.isPending();
						entry.requiresRestart = ce.isRestartRequired();
						entry.restartString   = ce._status;
						entry.legalValues     = ce._legalValues;
						entry.datatype        = ce._datatype;
						entry.description     = configDescription.get(ce.getConfigName());

						// Add
						_configMap .put(entry.configName, entry);
						_configList.add(entry);

						// Add it to the section list, if not already there
						if ( ! _configSectionList.contains(entry.sectionName) )
							_configSectionList.add(entry.sectionName);
					}
				}

				// ROUTE Configurations
//				if (waitDialog != null) 
//					waitDialog.setState("Getting Route Configuration");
				List<String> rsRoutes = RepServerUtils.getRoutes(conn);
				for (String routeTo : rsRoutes)
				{
//					if (waitDialog != null) 
//						waitDialog.setState("Getting Route Configuration, for '"+routeTo+"'.");

					List<ConfigEntry> config = RepServerUtils.getRouteConfig(conn, routeTo);
					for (ConfigEntry ce : config)
					{
						RsConfigEntry entry = new RsConfigEntry();

						entry.isNonDefault    = ce.isConfigOptionChanged();
						entry.sectionName     = "ROUTE";
						entry.instanceName    = routeTo;
						entry.configName      = ce._configName;
						entry.configValue     = ce._configValue;
						entry.runValue        = ce._runValue;
						entry.defaultValue    = ce._defaultValue;
						entry.isPending       = ce.isPending();
						entry.requiresRestart = ce.isRestartRequired();
						entry.restartString   = ce._status;
						entry.legalValues     = ce._legalValues;
						entry.datatype        = ce._datatype;
						entry.description     = configDescription.get(ce.getConfigName());

						// Add
						_configMap .put(entry.configName, entry);
						_configList.add(entry);

						// Add it to the section list, if not already there
						if ( ! _configSectionList.contains(entry.sectionName) )
							_configSectionList.add(entry.sectionName);
					}
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
				String expectedType = RsTune.APP_NAME;
				String readType     = recordingInfo.getRecDbxAppName();
				if ( ! expectedType.equals(readType) )
				{
					throw new WrongRecordingVendorContent(expectedType, readType);
				}

				
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
					RsConfigEntry entry = new RsConfigEntry();

					// NOTE: same order as in COLUMN_HEADERS
					_timestamp            = rs.getTimestamp(1);
					entry.isNonDefault    = rs.getBoolean(2);
					entry.sectionName     = rs.getString (3);
					entry.instanceName    = rs.getString (4);
					entry.configName      = rs.getString (5);
					entry.configValue     = rs.getString (6);
					entry.runValue        = rs.getString (7);
					entry.defaultValue    = rs.getString (8);
					entry.isPending       = rs.getBoolean(9);
					entry.requiresRestart = rs.getBoolean(10);
					entry.restartString   = rs.getString (11);
					entry.legalValues     = rs.getString (12);
					entry.datatype        = rs.getString (13);
					entry.description     = rs.getString (14);

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
			_logger.error("RsConfig:initialize:sql='"+sql+"'", ex);
			if (_hasGui)
				SwingUtils.showErrorMessage("RsConfig - Initialize", "SQL Exception: "+ex.getMessage()+"\n\nThis was found when executing SQL statement:\n\n"+sql, ex);
			_configMap = null;
			_configList = null;
			_configSectionList = null;
			return;
		}

		// Check if we got any strange in the configuration
		// in case it does: report that...
		if ( ! _offline )
		{
			checkConfig(conn);
		}

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

		RsConfigEntry e = _configMap.get(configName);
		
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
//		RsConfigEntry e = _configMap.get(configName);
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
		RsConfigEntry e = _configList.get(rowIndex);
		if (e == null)
			return null;

		switch (columnIndex)
		{
		case 0:  return e.isNonDefault;
		case 1:  return e.sectionName;
		case 2:  return e.instanceName;
		case 3:  return e.configName;
		case 4:  return e.configValue;
		case 5:  return e.runValue;
		case 6:  return e.defaultValue;
		case 7:  return e.isPending;
		case 8:  return e.requiresRestart;
		case 9:  return e.restartString;
		case 10: return e.legalValues;
		case 11: return e.datatype;
		case 12: return e.description;
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

//	@Override
//	public String getSqlDataType(String colName)
//	{
//		for (int i=0; i<COLUMN_HEADERS.length; i++)
//		{
//			if (COLUMN_HEADERS[i][COL_NAME].equals(colName))
//				return (String)COLUMN_HEADERS[i][COL_SQLTYPE];
//		}
//		return "";
//	}
//	@Override
//	public String getSqlDataType(int colIndex)
//	{
//		return (String)COLUMN_HEADERS[colIndex][COL_SQLTYPE];
//	}
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
		return "RS Config";
	}

	@Override
	public String getCellToolTip(int mrow, int mcol)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("<b>Description:  </b>").append(getValueAt(mrow, findColumn(RsConfig.DESCRIPTION))) .append("<br>");
		sb.append("<b>Section Name: </b>").append(getValueAt(mrow, findColumn(RsConfig.SECTION_NAME))).append("<br>");
		sb.append("<b>Config Name:  </b>").append(getValueAt(mrow, findColumn(RsConfig.CONFIG_NAME))) .append("<br>");
		sb.append("<b>Run Value:    </b>").append(getValueAt(mrow, findColumn(RsConfig.RUN_VALUE)))   .append("<br>");
		if (Boolean.TRUE.equals(getValueAt(mrow, findColumn(RsConfig.PENDING))))
		{
			sb.append("<br>");
			sb.append("<b>NOTE: RS/Connection/route Needs to be rebooted for this option to take effect.</b>").append("<br>");
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
