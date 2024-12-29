/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.dbxtune.AseTune;
import com.dbxtune.config.dbms.AseConfigText.Cache;
import com.dbxtune.config.dbms.AseConfigText.ConfigType;
import com.dbxtune.config.dict.AseSpConfigureDictionary;
import com.dbxtune.pcs.MonRecordingInfo;
import com.dbxtune.pcs.PersistWriterJdbc;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.NumberUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;


public class AseConfig
extends DbmsConfigAbstract 
{
	private static final long serialVersionUID = 1L;

	/** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(AseConfig.class);

	private boolean   _offline   = false;
	private boolean   _hasGui    = false;
	private Timestamp _timestamp = null;

	public static final String NON_DEFAULT          = "NonDefault";
	public static final String SECTION_NAME         = "SectionName";
	public static final String CONFIG_NAME          = "ConfigName";
	public static final String CONFIG_VALUE         = "ConfigValue";
	public static final String PENDING              = "Pending";
	public static final String PENDING_VALUE        = "PendingValue";
	public static final String DEFAULT_VALUE        = "DefaultValue";
	public static final String MEMORY_USED          = "UsedMemory";
	public static final String CONFIG_UNIT          = "Unit";
	public static final String RESTART_IS_REQ       = "RestartIsReq";
	public static final String CFG_VAL_STR          = "CfgValStr";
	public static final String CFG_VAL_STR_PENDNING = "CfgValStrPending";
	public static final String READ_ONLY            = "ReadOnly";
	public static final String TYPE                 = "Type";
	public static final String MIN_VALUE            = "MinValue";
	public static final String MAX_VALUE            = "MaxValue";
	public static final String DESCRIPTION          = "Description";
	public static final String CONFIG_ID            = "ConfigId";
	public static final String SECTION_ID           = "SectionId";
	public static final String DISPLAY_LEVEL        = "DisplayLevel";
	public static final String DATA_TYPE            = "DataType";

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
	//   ColumnName,           Editable, JTable type,   JDBC Type      JDBC Length JDBC Scale /* SQL Datatype,   */ Tooltip
	//   --------------------- --------- -------------- -------------- ----------- ---------- /* ----------------*/ --------------------------------------------
		{NON_DEFAULT,          false,    Boolean.class, Types.BOOLEAN, -1,         -1,        /* "bit",          */ "True if the value is not configured to the default value. (same as for sp_configure 'nondefault')"},
		{SECTION_NAME,         true,     String .class, Types.VARCHAR, 60,         -1,        /* "varchar(60)",  */ "Configuration Group"},
		{CONFIG_NAME,          true,     String .class, Types.VARCHAR, 60,         -1,        /* "varchar(60)",  */ "Name of the configuration, same as the name in sp_configure."},
		{CONFIG_VALUE,         true,     Integer.class, Types.INTEGER, -1,         -1,        /* "int",          */ "Value of the configuration."},
		{PENDING,              false,    Boolean.class, Types.BOOLEAN, -1,         -1,        /* "bit",          */ "The Configuration has not yet taken effect, probably needs restart to take effect."},
		{PENDING_VALUE,        true,     Integer.class, Types.INTEGER, -1,         -1,        /* "int",          */ "The value which will be configured on next restart, if PENDING is true."},
		{DEFAULT_VALUE,        true,     Integer.class, Types.VARCHAR, 100,        -1,        /* "varchar(100)", */ "The default configuration value."},            // it's a String but, RIGHT align it with Integer
		{MEMORY_USED,          true,     Integer.class, Types.VARCHAR, 30,         -1,        /* "varchar(30)",  */ "Memory Used by this configuration parameter"}, // it's a String but, RIGHT align it with Integer
		{CONFIG_UNIT,          true,     String .class, Types.VARCHAR, 30,         -1,        /* "varchar(30)",  */ "In what unit is this configuration"},
		{RESTART_IS_REQ,       false,    Boolean.class, Types.BOOLEAN, -1,         -1,        /* "bit",          */ "ASE needs to be rebooted for the configuration to take effect."},
		{CFG_VAL_STR,          true,     String .class, Types.VARCHAR, 100,        -1,        /* "varchar(100)", */ "The char value of the configuration."},
		{CFG_VAL_STR_PENDNING, true,     String .class, Types.VARCHAR, 100,        -1,        /* "varchar(100)", */ "The pending char configuration."},
		{READ_ONLY,            false,    Boolean.class, Types.BOOLEAN, -1,         -1,        /* "bit",          */ "This config is a read only value"},
		{TYPE,                 true,     String .class, Types.VARCHAR, 30,         -1,        /* "varchar(30)",  */ "dynamic or static"},
		{MIN_VALUE,            true,     Integer.class, Types.INTEGER, -1,         -1,        /* "int",          */ "integer minimum value of the configuration"},
		{MAX_VALUE,            true,     Integer.class, Types.INTEGER, -1,         -1,        /* "int",          */ "integer maximum value of the configuration"},
		{DESCRIPTION,          true,     String .class, Types.VARCHAR, 1024,       -1,        /* "varchar(1024)",*/ "Description of the configuration."},
		{CONFIG_ID,            true,     Integer.class, Types.INTEGER, -1,         -1,        /* "int",          */ "Internal ID number for this configuration."},
		{SECTION_ID,           true,     Integer.class, Types.INTEGER, -1,         -1,        /* "int",          */ "Configuration Group ID"},
		{DISPLAY_LEVEL,        true,     Integer.class, Types.INTEGER, -1,         -1,        /* "int",          */ ""},
		{DATA_TYPE,            true,     Integer.class, Types.INTEGER, -1,         -1,        /* "int",          */ ""}
	};

	private static String GET_CONFIG_ONLINE_SQL = 
		"select \n" +
		"    CONFIG_DATE   = getdate(),  \n" + 
		"    NON_DEFAULT   = case when (b.defvalue != isnull(a.value2, convert(char(32), a.value)) or b.defvalue != isnull(b.value2, convert(char(32), b.value))) \n" +
		"                               and b.config != 114 /* Exclude option 'configuration file' */ \n" +
		"                               and b.type != 'read-only' \n" +
		"                               and b.display_level <= 10 \n" +
		"	                      then convert(bit, 1) \n" +
		"	                      else convert(bit, 0) \n" +
		"                    end, \n" +
		"    SECTION       = (select convert(char(35),x.name) from master.dbo.sysconfigures x where a.parent = x.config), \n" + 
		"    NAME          = convert(char(35),a.name),  \n" + 
		"    VALUE         = b.value,  \n" + 
		"    PENDING       = case when    a.value = b.value \n" +
		"                              or b.type = 'read-only' \n" +
		"                              or convert(bit,(b.status&0x10)|(b.status&0x20)) = 1 \n" +
		"                         then convert(bit,0) \n" +
		"                         else convert(bit,1) \n" +
		"                    end, \n" + 
		"    PENDING_VALUE = nullif(a.value, b.value), \n" + 
		"    DEFAULT_VALUE = b.defvalue,  \n" + 
		"    USED_MEMORY   = b.comment, \n" +
		"    CONFIG_UNIT   = b.unit, \n" +
		"    RESTART_REQ   = convert(bit, 1-convert(bit, a.status&8)),  \n" + 
		"    CHAR_VALUE    = b.value2,  \n" + 
		"    CHAR_PENDING  = a.value2,  \n" + 
		"    READONLY      = convert(bit,(b.status&0x10)|(b.status&0x20)),  \n" + 
		"    TYPE          = b.type, \n" + 
		"    MIN_VALUE     = b.minimum_value,  \n" + 
		"    MAX_VALUE     = b.maximum_value,  \n" + 
		"    DESCRIPTION   = (select m.description from master.dbo.sysmessages m where m.error = b.message_num and m.langid = NULL),  \n" + 
		"    CONFIG_ID     = a.config, \n" +
		"    PARENT_ID     = a.parent,  \n" + 
		"    DISP_LEVEL    = b.display_level,  \n" + 
		"    DATA_TYPE     = b.datatype  \n" + 
		"from master.dbo.sysconfigures a,  \n" + 
		"     master.dbo.syscurconfigs b  \n" + 
		"where a.config=b.config  \n" + 
		"  and b.display_level<>null  \n" + 
		"  and a.parent!= 19  \n" + 
		"  and a.parent > 1  \n" + 
//		"			      and (b.defvalue != isnull(a.value2, convert(char(32), a.value))  \n" + 
//		"			        or b.defvalue != isnull(b.value2, convert(char(32), b.value)))  \n" + 
//		"			      and b.config != 114 /* Exclude option 'configuration file' */ \n" + 
//		"			      and b.type != 'read-only' \n" + 
//		"			      and b.display_level <= 10 \n" + 
		"order by a.parent, a.name \n";

	private static String GET_CONFIG_OFFLINE_SQL = 
		"select * " +
		"from [" + PersistWriterJdbc.getTableName(null, null, PersistWriterJdbc.SESSION_DBMS_CONFIG, null, false) + "] \n" +
		"where [SessionStartTime] = SESSION_START_TIME \n";

	private static String GET_CONFIG_OFFLINE_MAX_SESSION_SQL = 
		" (select max([SessionStartTime]) " +
		"  from ["+PersistWriterJdbc.getTableName(null, null, PersistWriterJdbc.SESSION_DBMS_CONFIG, null, false) + "]" +
		" ) ";

	// NOTE THE BELOW will be used if table MonSessionDbSrvConfig isn't found (2015-04-18 I changed the name, from Ase to DbSrv to be more generic)
	private static String GET_CONFIG_OFFLINE_SQL_BACKWARD_COMP = 
		"select * " +
		"from [MonSessionAseConfig] \n" +
		"where [SessionStartTime] = SESSION_START_TIME \n";

	// NOTE THE BELOW will be used if table MonSessionDbSrvConfig isn't found (2015-04-18 I changed the name, from Ase to DbSrv to be more generic)
	private static String GET_CONFIG_OFFLINE_MAX_SESSION_SQL_BACKWARD_COMP = 
		" (select max([SessionStartTime]) " +
		"from [MonSessionAseConfig] \n" +
		" ) ";
	
//	@Override
//	public String getSqlForDiff(boolean isOffline)
//	{
//		if (isOffline)
//		{
//			String tabName = PersistWriterJdbc.getTableName(null, PersistWriterJdbc.SESSION_DBMS_CONFIG, null, false);
//
//			return 
//					"select \n" +
//					"     [" + CONFIG_NAME + "] \n" +
////					"    ,[" + CONFIG_VALUE + "] \n" +
//					"    ,[" + CFG_VAL_STR + "] \n" +
//					"from [" + tabName + "] \n" +
//					"where [SessionStartTime] = (select max([SessionStartTime]) from [" + tabName + "]) \n" +
////					"order by [NAME] \n" +
//					"order by 1 \n" +
//					"";
//		}
//		else
//		{
//			return
//					"select \n" +
//					"     " + CONFIG_NAME  + " = convert(char(35),a.name)  \n" +
////					"    ," + CONFIG_VALUE + " = b.value  \n" + 
//					"    ," + CFG_VAL_STR  + " = b.value2  \n" + 
//					"from master.dbo.sysconfigures a,  \n" + 
//					"     master.dbo.syscurconfigs b  \n" + 
//					"where a.config         = b.config  \n" + 
//					"  and b.display_level <> NULL  \n" + 
//					"  and a.parent        <> 19  \n" + 
//					"  and a.parent         > 1  \n" + 
////					"order by a.name \n" +
//					"order by 1 \n" +
//					"";
//		}
//	}
		
	/** hashtable with MonTableEntry */
	private HashMap<String,AseConfigEntry> _configMap  = null;
	private ArrayList<AseConfigEntry>      _configList = null;

	private ArrayList<String>              _configSectionList = null;

	public class AseConfigEntry
	implements IDbmsConfigEntry
	{
		@Override public String  getConfigKey()   { return configName; }
		@Override public String  getConfigValue() { return configValueString; }
		@Override public String  getSectionName() { return sectionName; }
		@Override public boolean isPending()      { return isPending; }
		@Override public boolean isNonDefault()   { return isNonDefault; }
		
		/** configuration has been changed by any user (same as sp_configure 'nondefault')*/
		public boolean isNonDefault;

		/** Different sections in the configuration */
		public String  sectionName;

		/** Name of the configuration */
		public String  configName;

		/** integer representation of the configuration */
		public int     configValue;

		/** true if the configuration value is set, but we need to restart the ASE server to enable the configuration */
		public boolean isPending;

		/** integer representation of the pending configuration */
		public int     pendingValue;

		/** String representation of the default configuration */
		public String  defaultValue;

		/** how much memory does this config option consume. MB/KB */
		public String  usedMemoryStr;

		/** What is the unit this configuration is specified in */
		public String  configUnit;

		/** true if the server needs to be restarted to enable the configuration */
		public boolean requiresRestart;

		/** String representation of the configuration value */
		public String  configValueString;

		/** String representation of the pending configuration */
		public String  pendingValueString;

		/** The configuration is read only and can't be changed */
		public boolean isReadOnly;

		/** static or static */
		public String  configType;

		/** minimum value of a integer value */
		public int     minValue;

		/** maximum value of a integer value */
		public int     maxValue;

		/** description of the configuration option */
		public String  description;

		/** ID of the configuration option */
		public int     configId;

		/** ID of the sections in the configuration */
		public int     parentId;

		/** display level */
		public int     displayLevel;

		/** datatype ?, dont know if this is the ASE, JDBC or ODBC ID of the datatype */
		public int     dataType;
	}

	public AseConfig()
	{
		DbmsConfigTextManager.clear();
		AseConfigText.createAndRegisterAllInstances();
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
	public Map<String,AseConfigEntry> getAseConfigMap()
	{
		return _configMap;
	}

	@Override
	public Map<String, ? extends IDbmsConfigEntry> getDbmsConfigMap()
	{
		return _configMap;
	}

	/** check if the AseConfig is initialized or not */
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
		
		_configMap         = new HashMap<String,AseConfigEntry>();
		_configList        = new ArrayList<AseConfigEntry>();
		_configSectionList = new ArrayList<String>();
		
		try { setDbmsServerName(conn.getDbmsServerName()); } catch (SQLException ex) { setDbmsServerName(ex.getMessage()); };
		try { setDbmsVersionStr(conn.getDbmsVersionStr()); } catch (SQLException ex) { setDbmsVersionStr(ex.getMessage()); };

		try { setLastUsedUrl( conn.getMetaData().getURL() ); } catch(SQLException ignore) { }
		setLastUsedConnProp(conn.getConnPropOrDefault());


		String sql = GET_CONFIG_ONLINE_SQL;
		if (_offline)
		{
			boolean oldNameExists = DbUtils.checkIfTableExistsNoThrow(conn, null, null, "MonSessionAseConfig");

			sql = oldNameExists ? GET_CONFIG_OFFLINE_SQL_BACKWARD_COMP : GET_CONFIG_OFFLINE_SQL;

			String tsStr = "";
			if (ts == null)
			{
				tsStr = oldNameExists ? GET_CONFIG_OFFLINE_MAX_SESSION_SQL_BACKWARD_COMP : GET_CONFIG_OFFLINE_MAX_SESSION_SQL;
			}
			else
			{
				tsStr = "'" + ts + "'";
			}
			sql = sql.replace("SESSION_START_TIME", tsStr);

			// For OFFLINE mode: override some the values previously fetched the connection object, with values from the Recorded Database
			MonRecordingInfo recordingInfo = new MonRecordingInfo(conn, null);
			setOfflineRecordingInfo(recordingInfo);
			setDbmsServerName(recordingInfo.getDbmsServerName());
			setDbmsVersionStr(recordingInfo.getDbmsVersionStr());

			// Check if this is correct TYPE
			String expectedType = AseTune.APP_NAME;
			String readType     = recordingInfo.getRecDbxAppName();
			if ( ! expectedType.equals(readType) )
			{
				throw new WrongRecordingVendorContent(expectedType, readType);
			}
		}

		// replace all [] into DBMS Vendor Specific Chars
		sql = conn.quotifySqlString(sql);

		try
		{
			Statement stmt = conn.createStatement();
			stmt.setQueryTimeout(10);

			ResultSet rs = stmt.executeQuery(sql);
			while ( rs.next() )
			{
				AseConfigEntry entry = new AseConfigEntry();

				int pos = 2; 
				// Skip first column
				// online  = CONFIG_DATE
				// offline = SessionStartTime
				_timestamp               = rs.getTimestamp(1);
				entry.isNonDefault       = rs.getBoolean(pos++);
				entry.sectionName        = rs.getString (pos++);
				entry.configName         = rs.getString (pos++);
				entry.configValue        = rs.getInt    (pos++);
				entry.isPending          = rs.getBoolean(pos++);
				entry.pendingValue       = rs.getInt    (pos++);
				entry.defaultValue       = rs.getString (pos++);
				entry.usedMemoryStr      = rs.getString (pos++);
				entry.configUnit         = rs.getString (pos++);
				entry.requiresRestart    = rs.getBoolean(pos++);
				entry.configValueString  = rs.getString (pos++);
				entry.pendingValueString = rs.getString (pos++);
				entry.isReadOnly         = rs.getBoolean(pos++);
				entry.configType         = rs.getString (pos++);
				entry.minValue           = rs.getInt    (pos++);
				entry.maxValue           = rs.getInt    (pos++);
				entry.description        = rs.getString (pos++);
				entry.configId           = rs.getInt    (pos++);
				entry.parentId           = rs.getInt    (pos++);
				entry.displayLevel       = rs.getInt    (pos++);
				entry.dataType           = rs.getInt    (pos++);

				// fix description
				if (StringUtil.isNullOrBlank(entry.description))
				{
					String desc = AseSpConfigureDictionary.getInstance().getDescription(entry.configName);
					entry.description = "*** " + desc;
				}
				
				// fix 'used memory' column
				if ( ! _offline )
				{
					try
					{
						int mem = Integer.parseInt(entry.usedMemoryStr);
						if (mem == 0)
							entry.usedMemoryStr = "";
						else
						{
							double memInMb = mem / 1024.0;
							BigDecimal bdMem = new BigDecimal( memInMb ).setScale(2, BigDecimal.ROUND_HALF_EVEN);
							entry.usedMemoryStr = bdMem + " MB";
	
							String unit = entry.configUnit;
							if (unit.equals("memory pages(2k)"))
							{
							}
							else if (unit.equals("kilobytes"))
							{
							}
						}
					}
					catch (NumberFormatException ignore) { /* ignore */ }
				}
				
				_configMap .put(entry.configName, entry);
				_configList.add(entry);

				// Add it to the section list, if not already there
				if ( ! _configSectionList.contains(entry.sectionName) )
					_configSectionList.add(entry.sectionName);
			}
			rs.close();
		}
		catch (SQLException ex)
		{
//			if (offline && ex.getMessage().contains("not found"))
//			{
//				_logger.warn("Tooltip on column headers wasn't available in the offline database. This simply means that tooltip wont be showed in various places.");
//				return;
//			}
			_logger.error("AseConfig:initialize:sql='"+sql+"'", ex);
			if (_hasGui)
				SwingUtils.showErrorMessage("AseConfig - Initialize", "SQL Exception: "+ex.getMessage()+"\n\nThis was found when executing SQL statement:\n\n"+sql, ex);
			_configMap = null;
			_configList = null;
			_configSectionList = null;

			// JZ0C0: Connection is already closed.
			// JZ006: Caught IOException: com.sybase.jdbc4.jdbc.SybConnectionDeadException: JZ0C0: Connection is already closed.
			if ( "JZ0C0".equals(ex.getSQLState()) || "JZ006".equals(ex.getSQLState()) )
			{
				try
				{
					_logger.info("AseConfig:initialize(): lost connection... try to reconnect...");
					conn.reConnect(null);
					_logger.info("AseConfig:initialize(): Reconnect succeeded, but the configuration will not be visible");
				}
				catch(Exception reconnectEx)
				{
					_logger.warn("AseConfig:initialize(): reconnect failed due to: "+reconnectEx);
					throw ex; // Note throw the original exception and not reconnectEx
				}
			}

//			throw ex;
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
	 * Get description for the configuration name
	 * @param colfigName
	 * @return the description
	 */
	@Override
	public String getDescription(String configName)
	{
		if (_configMap == null)
			return null;

		AseConfigEntry e = _configMap.get(configName);
		
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
//		AseConfigEntry e = _configMap.get(configName);
//		
//		return e == null ? -1 : e.configValue;
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
		AseConfigEntry e = _configList.get(rowIndex);
		if (e == null)
			return null;

		switch (columnIndex)
		{
		case 0:  return e.isNonDefault;
		case 1:  return e.sectionName;
		case 2:  return e.configName;
		case 3:  return e.configValue;
		case 4:  return e.isPending;
		case 5:  return e.pendingValue;
		case 6:  return e.defaultValue;
		case 7:  return e.usedMemoryStr; 
		case 8:  return e.configUnit; 
		case 9:  return e.requiresRestart; 
		case 10: return e.configValueString;
		case 11: return e.pendingValueString;
		case 12: return e.isReadOnly;
		case 13: return e.configType;
		case 14: return e.minValue;
		case 15: return e.maxValue;
		case 16: return e.description;
		case 17: return e.configId;
		case 18: return e.parentId;
		case 19: return e.displayLevel;
		case 20: return e.dataType;
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
//		switch (columnIndex)
//		{
//		case 1:  return true;
//		case 2:  return true;
//		case 16: return true;
//		}
//		return false;
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
		return "ASE Config";
	}

	@Override
	public String getCellToolTip(int mrow, int mcol)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("<b>Description:  </b>").append(getValueAt(mrow, findColumn(AseConfig.DESCRIPTION))) .append("<br>");
		sb.append("<b>Section Name: </b>").append(getValueAt(mrow, findColumn(AseConfig.SECTION_NAME))).append("<br>");
		sb.append("<b>Config Name:  </b>").append(getValueAt(mrow, findColumn(AseConfig.CONFIG_NAME))) .append("<br>");
		sb.append("<b>Run Value:    </b>").append(getValueAt(mrow, findColumn(AseConfig.CONFIG_VALUE))).append("<br>");
		sb.append("<b>Max Value:    </b>").append(getValueAt(mrow, findColumn(AseConfig.MAX_VALUE)))   .append("<br>");
		sb.append("<b>Min Value:    </b>").append(getValueAt(mrow, findColumn(AseConfig.MIN_VALUE)))   .append("<br>");
		if (Boolean.TRUE.equals(getValueAt(mrow, findColumn(AseConfig.PENDING))))
		{
			sb.append("<br>");
			sb.append("<b>NOTE: DBMS Needs to be rebooted for this option to take effect.</b>").append("<br>");
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
	public String getFreeMemoryStr()
	{
//		return "";
		return ((Cache) DbmsConfigTextManager.getInstance(ConfigType.AseCacheConfig.toString())).getFreeMemoryStr();
	}

	@Override
	public boolean isReverseEngineeringPossible()
	{
		return true;
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
		sb.append("** At Date:         ").append(new Timestamp(System.currentTimeMillis())).append("\n");
		sb.append("** ASE Server name: ").append(getDbmsServerName()).append("\n");
		sb.append("** ASE Version:     ").append(getDbmsVersionStr()).append("\n");
		sb.append("*/ \n");
		sb.append("\n");
		for (int r=0; r<modelRows.length; r++)
		{
			int mrow = modelRows[r];

			// SKIP: If it's a read only configuration 
			if ("read-only".equals(getValueAt(mrow, findColumn(AseConfig.TYPE))))
				continue;

//			sb.append("---------------------------------------------------------------------------\n");
			sb.append("--#########################################################################\n");
			sb.append("-- Config Name:   ").append(getValueAt(mrow, findColumn(AseConfig.CONFIG_NAME))) .append("\n");
			sb.append("-- Description:   ").append(getValueAt(mrow, findColumn(AseConfig.DESCRIPTION))) .append("\n");
			sb.append("-- Section Name:  ").append(getValueAt(mrow, findColumn(AseConfig.SECTION_NAME))).append("\n");
			sb.append("-- Type:          ").append(getValueAt(mrow, findColumn(AseConfig.TYPE))).append("\n");

			String cfgValStr = getValueAt(mrow, findColumn(AseConfig.CFG_VAL_STR)) + "";
			if (NumberUtils.isNumber(cfgValStr))
			{
				sb.append("-- Default Value: ").append(getValueAt(mrow, findColumn(AseConfig.DEFAULT_VALUE))).append("\n");
				sb.append("-- Min Value:     ").append(getValueAt(mrow, findColumn(AseConfig.MIN_VALUE))).append("\n");
				sb.append("-- Max Value:     ").append(getValueAt(mrow, findColumn(AseConfig.MAX_VALUE))).append("\n");
				
    			sb.append("exec sp_configure '").append(getValueAt(mrow, findColumn(AseConfig.CONFIG_NAME))).append("', ");
    			sb.append(getValueAt(mrow, findColumn(AseConfig.CONFIG_VALUE))).append("");
    			sb.append("\n");
    			sb.append("go\n");
			}
			else
			{
    			sb.append("exec sp_configure '").append(getValueAt(mrow, findColumn(AseConfig.CONFIG_NAME))).append("', 0, '").append(cfgValStr).append("'");
    			sb.append("\n");
    			sb.append("go\n");
			}
			sb.append("\n");
		}
		
		return sb.toString();
	}

	protected Map<String, String> getDiffFixSkip()
	{
		HashMap<String, String> skipConfigNames = new HashMap<>();
		
		skipConfigNames.put("upgrade version"           , "This should NOT normally be changed via sp_configure, use normal upgrade path instead");
		skipConfigNames.put("configuration file"        , "This is per instance specfic and should probably NOT be changed");
		skipConfigNames.put("config file version"       , "This is maintained by the Server and should normally NOT be changed manually");
		skipConfigNames.put("default sortorder id"      , "Best to change this via ASE binary 'sqlloc' or 'sqllocres'");
		skipConfigNames.put("default character set id"  , "Best to change this via ASE binary 'sqlloc' or 'sqllocres'");
		skipConfigNames.put("max memory"                , "Probably different on most of the machines");
		
		return skipConfigNames;
	}
	
	@Override
	public String reverseEngineer(Map<String, String> keyValMap, String comment)
	{
		if (keyValMap == null)     return null;
		if (keyValMap.size() == 0) return null;

		// Get a "list" of config names that would be *bad* to change when in Config Sync
		Map<String, String> skipConfigNames = getDiffFixSkip();
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("/* \n");
		sb.append("** Reverse engineering ").append(keyValMap.size()).append(" entries. \n");
		sb.append("** At Date:         ").append(new Timestamp(System.currentTimeMillis())).append("\n");
		sb.append("** ASE Server name: ").append(getDbmsServerName()).append("\n");
		sb.append("** ASE Version:     ").append(getDbmsVersionStr()).append("\n");
		if (StringUtil.hasValue(comment))
			sb.append("** Comment:         ").append(comment).append("\n");
		sb.append("*/ \n");

		for (Entry<String, String> e : keyValMap.entrySet())
		{
			String key = e.getKey();
			String val = e.getValue();

			AseConfigEntry cfgEntry = _configMap.get(e.getKey());

			sb.append("\n");
//			sb.append("---------------------------------------------------------------------------\n");
			sb.append("--#########################################################################\n");
			sb.append("-- Config Name:   ").append(key)                  .append("\n");
			sb.append("-- Description:   ").append(cfgEntry.description) .append("\n");
			sb.append("-- Section Name:  ").append(cfgEntry.sectionName) .append("\n");
			sb.append("-- Type:          ").append(cfgEntry.configType)  .append("\n");
			sb.append("-- Default Value: ").append(cfgEntry.defaultValue).append("\n");
			sb.append("---------------------------------------------------------------------------\n");
			sb.append("-- Prev Value:    ").append(cfgEntry.getConfigValue()).append("\n");
			sb.append("--  New Value:    ").append(val)                      .append("\n");
			sb.append("---------------------------------------------------------------------------\n");

			if (cfgEntry.isReadOnly)
			{
				sb.append("-- NOTE -- This is a 'read-only' configuration... so nothing will be done here.\n");
				sb.append("---------------------------------------------------------------------------\n");
				sb.append("-- exec sp_configure '").append(key).append("', ").append(e.getValue()).append("");
				sb.append("\n");
				sb.append("go\n");
				continue;
			}

			if (skipConfigNames.containsKey(key))
			{
				sb.append("-- SKIP -- This is part of the 'skip-list' configuration... so nothing will be done here.\n");
				sb.append("--      -- It would NOT be wise to change this parameter.\n");
				sb.append("---------------------------------------------------------------------------\n");
				sb.append("-- WHY: ").append(skipConfigNames.get(key)).append("\n"); // print the DESCRIPTION why we are skipping this value
				sb.append("---------------------------------------------------------------------------\n");
				sb.append("-- exec sp_configure '").append(key).append("', ").append(e.getValue()).append("");
				sb.append("\n");
				sb.append("go\n");
				continue;
			}

			if (NumberUtils.isNumber(val))
			{
				sb.append("-- Min Value:     ").append(cfgEntry.minValue).append("\n");
				sb.append("-- Max Value:     ").append(cfgEntry.maxValue).append("\n");

				sb.append("exec sp_configure '").append(key).append("', ").append(e.getValue()).append("");
    			sb.append("\n");
    			sb.append("go\n");
			}
			else
			{
    			sb.append("exec sp_configure '").append(key).append("', 0, '").append(val).append("'");
    			sb.append("\n");
    			sb.append("go\n");
			}
		}
		
		return sb.toString();
	}

}
