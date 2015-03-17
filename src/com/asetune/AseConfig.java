/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

import org.apache.log4j.Logger;

import com.asetune.pcs.PersistWriterJdbc;
import com.asetune.utils.SwingUtils;


public class AseConfig
extends AbstractTableModel
{
	private static final long serialVersionUID = 1L;

	/** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(AseConfig.class);

	/** Instance variable */
	private static AseConfig _instance = null;
	
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

	private static final int COL_NAME    = 0;
	private static final int COL_CLASS   = 1;
	private static final int COL_SQLTYPE = 2;
	private static final int COL_TOOLTIP = 3;
	private static Object[][] COLUMN_HEADERS = 
	{
	//   ColumnName,           JTable type,   SQL Datatype,   Tooltip
	//   --------------------- -------------- --------------- --------------------------------------------
		{NON_DEFAULT,          Boolean.class, "bit",          "True if the value is not configured to the default value. (same as for sp_configure 'nondefault')"},
		{SECTION_NAME,         String .class, "varchar(60)",  "Configuration Group"},
		{CONFIG_NAME,          String .class, "varchar(60)",  "Name of the configuration, same as the name in sp_configure."},
		{CONFIG_VALUE,         Integer.class, "int",          "Value of the configuration."},
		{PENDING,              Boolean.class, "bit",          "The Configuration has not yet taken effect, probably needs restart to take effect."},
		{PENDING_VALUE,        Integer.class, "int",          "The value which will be configured on next restart, if PENDING is true."},
		{DEFAULT_VALUE,        Integer.class, "varchar(100)", "The default configuration value."},            // it's a String but, RIGHT align it with Integer
		{MEMORY_USED,          Integer.class, "varchar(30)",  "Memory Used by this configuration parameter"}, // it's a String but, RIGHT align it with Integer
		{CONFIG_UNIT,          String .class, "varchar(30)",  "In what unit is this configuration"},
		{RESTART_IS_REQ,       Boolean.class, "bit",          "ASE needs to be rebooted for the configuration to take effect."},
		{CFG_VAL_STR,          String .class, "varchar(100)", "The char value of the configuration."},
		{CFG_VAL_STR_PENDNING, String .class, "varchar(100)", "The pending char configuration."},
		{READ_ONLY,            Boolean.class, "bit",          "This config is a read only value"},
		{TYPE,                 String .class, "varchar(30)",  "dynamic or static"},
		{MIN_VALUE,            Integer.class, "int",          "integer minimum value of the configuration"},
		{MAX_VALUE,            Integer.class, "int",          "integer maximum value of the configuration"},
		{DESCRIPTION,          String .class, "varchar(255)", "Description of the configuration."},
		{CONFIG_ID,            Integer.class, "int",          "Internal ID number for this configuration."},
		{SECTION_ID,           Integer.class, "int",          "Configuration Group ID"},
		{DISPLAY_LEVEL,        Integer.class, "int",          ""},
		{DATA_TYPE,            Integer.class, "int",          ""}
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
		"from " + PersistWriterJdbc.getTableName(PersistWriterJdbc.SESSION_ASE_CONFIG, null, true) + " \n" +
		"where \"SessionStartTime\" = SESSION_START_TIME \n";

	private static String GET_CONFIG_OFFLINE_MAX_SESSION_SQL = 
		" (select max(\"SessionStartTime\") " +
		"  from "+PersistWriterJdbc.getTableName(PersistWriterJdbc.SESSION_ASE_CONFIG, null, true) + 
		" ) ";
	
	/** hashtable with MonTableEntry */
	private HashMap<String,AseConfigEntry> _aseConfigMap  = null;
	private ArrayList<AseConfigEntry>      _aseConfigList = null;

	private ArrayList<String>              _aseConfigSectionList = null;

	public class AseConfigEntry
	{
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

	/** check if we got an instance or not */
	public static boolean hasInstance()
	{
		return (_instance != null);
	}

	/** Get a instance of the class, if we didn't have an instance, one will be created, but not initialized */
	public static AseConfig getInstance()
	{
		if (_instance == null)
			_instance = new AseConfig();
		return _instance;
	}

	/**
	 * Reset ALL configurations, this so we can get new ones later<br>
	 * Most possible called from disconnect() or similar
	 */
	public static void reset()
	{
		_instance = null;
	}

	/** get the Map */
	public Map<String,AseConfigEntry> getAseConfigMap()
	{
		return _aseConfigMap;
	}

	/** check if the AseConfig is initialized or not */
	public boolean isInitialized()
	{
		return _aseConfigMap != null;
	}

	/**
	 * Initialize 
	 * @param conn
	 */
	public void initialize(Connection conn, boolean hasGui, boolean offline, Timestamp ts)
	{
		_hasGui  = hasGui;
		_offline = offline;
		refresh(conn, ts);
	}

	/**
	 * refresh 
	 * @param conn
	 */
	public void refresh(Connection conn, Timestamp ts)
	{
		if (conn == null)
			return;

		_aseConfigMap         = new HashMap<String,AseConfigEntry>();
		_aseConfigList        = new ArrayList<AseConfigEntry>();
		_aseConfigSectionList = new ArrayList<String>();
		
		// version
//		try
//		{
//			String aseVersionStr = null;
//
//			Statement stmt = conn.createStatement();
//			ResultSet rs = stmt.executeQuery("select @@version");
//			while ( rs.next() )
//			{
//				aseVersionStr = rs.getString(1);
//			}
//			rs.close();
//		}
//		catch (SQLException ex)
//		{
//			_logger.error("AseConfig:initialize, @@version", ex);
//			return;
//		}


		String sql = GET_CONFIG_ONLINE_SQL;
		if (_offline)
		{
			sql = GET_CONFIG_OFFLINE_SQL;

			String tsStr = "";
			if (ts == null)
			{
				tsStr = GET_CONFIG_OFFLINE_MAX_SESSION_SQL;
			}
			else
			{
				tsStr = "'" + ts + "'";
			}
			sql = sql.replace("SESSION_START_TIME", tsStr);

		}

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
				
				_aseConfigMap .put(entry.configName, entry);
				_aseConfigList.add(entry);

				// Add it to the section list, if not already there
				if ( ! _aseConfigSectionList.contains(entry.sectionName) )
					_aseConfigSectionList.add(entry.sectionName);
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
			_aseConfigMap = null;
			_aseConfigList = null;
			_aseConfigSectionList = null;
			return;
		}

		// notify change
		fireTableDataChanged();
	}

	/**
	 * Get description for the configuration name
	 * @param colfigName
	 * @return the description
	 */
	public String getDescription(String configName)
	{
		if (_aseConfigMap == null)
			return null;

		AseConfigEntry e = _aseConfigMap.get(configName);
		
		return e == null ? null : e.description;
	}

	/**
	 * Get run value
	 * @param colfigName
	 * @return the description
	 */
	public int getRunValue(String configName)
	{
		if (_aseConfigMap == null)
			return -1;

		AseConfigEntry e = _aseConfigMap.get(configName);
		
		return e == null ? -1 : e.configValue;
	}

	/**
	 * Get Section list, which is a list of all config sections
	 * @return List<String> of all sections
	 */
	public List<String> getSectionList()
	{
		return _aseConfigSectionList;
	}


	/**
	 * Get Section list, which is a list of all config sections
	 * @return List<String> of all sections
	 */
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
		return _aseConfigList == null ? 0 : _aseConfigList.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		AseConfigEntry e = _aseConfigList.get(rowIndex);
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
		switch (columnIndex)
		{
		case 1:  return true;
		case 2:  return true;
		case 16: return true;
		}
		return false;
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
	// BEGIN implement: AbstractTableModel
	//--------------------------------------------------------------------------------

	public String getColumnToolTip(String colName)
	{
		for (int i=0; i<COLUMN_HEADERS.length; i++)
		{
			if (COLUMN_HEADERS[i][COL_NAME].equals(colName))
				return (String)COLUMN_HEADERS[i][COL_TOOLTIP];
		}
		return "";
	}

	public String getSqlDataType(String colName)
	{
		for (int i=0; i<COLUMN_HEADERS.length; i++)
		{
			if (COLUMN_HEADERS[i][COL_NAME].equals(colName))
				return (String)COLUMN_HEADERS[i][COL_SQLTYPE];
		}
		return "";
	}
	public String getSqlDataType(int colIndex)
	{
		return (String)COLUMN_HEADERS[colIndex][COL_SQLTYPE];
	}
}
