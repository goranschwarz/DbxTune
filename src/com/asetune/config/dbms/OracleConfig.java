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
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.asetune.OracleTune;
import com.asetune.pcs.MonRecordingInfo;
import com.asetune.pcs.PersistWriterJdbc;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.NumberUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;


public class OracleConfig
extends DbmsConfigAbstract 
{
	private static final long serialVersionUID = 1L;

	/** Log4j logging. */
	private static Logger _logger = Logger.getLogger(OracleConfig.class);

	/** Instance variable */
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
	//   ColumnName,           Editable, JTable type,   JDBC Type      JDBC Length JDBC Scale /* SQL Datatype,    */ Tooltip
	//   --------------------- --------- -------------- -------------- ----------- ---------- /* ---------------- */ --------------------------------------------
		{NON_DEFAULT,          false,    Boolean.class, Types.BOOLEAN, -1,          -1,       /* "bit",           */ "True if the value is not configured to the default value. (same as for sp_configure 'nondefault')"},
		{SECTION_NAME,         true,     String .class, Types.VARCHAR, 60,          -1,       /* "varchar(60)",   */ "Configuration Group"},
		{CONFIG_NAME,          true,     String .class, Types.VARCHAR, 60,          -1,       /* "varchar(60)",   */ "Name of the configuration, same as the name in sp_configure."},
		{CONFIG_VALUE,         true,     Integer.class, Types.BIGINT,  -1,          -1,       /* "bigint",        */ "Value of the configuration."},
		{PENDING,              false,    Boolean.class, Types.BOOLEAN  -1,          -1,       /* "bit",           */ "The Configuration has not yet taken effect, probably needs restart to take effect."},
		{PENDING_VALUE,        true,     Integer.class, Types.BIGINT,  -1,          -1,       /* "bigint",        */ "The value which will be configured on next restart, if PENDING is true."},
		{DEFAULT_VALUE,        true,     Integer.class, Types.VARCHAR, 300,         -1,       /* "varchar(100)",  */ "The default configuration value."},            // it's a String but, RIGHT align it with Integer
		{MEMORY_USED,          true,     Integer.class, Types.VARCHAR, 30,          -1,       /* "varchar(30)",   */ "Memory Used by this configuration parameter"}, // it's a String but, RIGHT align it with Integer
		{CONFIG_UNIT,          true,     String .class, Types.VARCHAR, 30,          -1,       /* "varchar(30)",   */ "In what unit is this configuration"},
		{RESTART_IS_REQ,       false,    Boolean.class, Types.BOOLEAN, -1,          -1,       /* "bit",           */ "ASE needs to be rebooted for the configuration to take effect."},
		{CFG_VAL_STR,          true,     String .class, Types.VARCHAR, 100,         -1,       /* "varchar(100)",  */ "The char value of the configuration."},
		{CFG_VAL_STR_PENDNING, true,     String .class, Types.VARCHAR, 100,         -1,       /* "varchar(100)",  */ "The pending char configuration."},
		{READ_ONLY,            false,    Boolean.class, Types.BOOLEAN, -1,          -1,       /* "bit",           */ "This config is a read only value"},
		{TYPE,                 true,     String .class, Types.VARCHAR, 30,          -1,       /* "varchar(30)",   */ "dynamic or static"},
		{MIN_VALUE,            true,     Integer.class, Types.BIGINT,  -1,          -1,       /* "bigint",        */ "integer minimum value of the configuration"},
		{MAX_VALUE,            true,     Integer.class, Types.BIGINT,  -1,          -1,       /* "bigint",        */ "integer maximum value of the configuration"},
		{DESCRIPTION,          true,     String .class, Types.VARCHAR, 1024,        -1,       /* "varchar(1024)", */ "Description of the configuration."},
		{CONFIG_ID,            true,     Integer.class, Types.INTEGER, -1,          -1,       /* "bigint",        */ "Internal ID number for this configuration."},
		{SECTION_ID,           true,     Integer.class, Types.INTEGER, -1,          -1,       /* "bigint",        */ "Configuration Group ID"},
		{DISPLAY_LEVEL,        true,     Integer.class, Types.INTEGER, -1,          -1,       /* "bigint",        */ ""},
		{DATA_TYPE,            true,     Integer.class, Types.INTEGER, -1,          -1,       /* "bigint",        */ ""}
	};

//
//  select * from v$parameter
//
//	RS> Col# Label                 JDBC Type Name         Guessed DBMS type Source Table
//	RS> ---- --------------------- ---------------------- ----------------- ------------
//	RS> 1    NUM                   java.sql.Types.NUMERIC NUMBER(0,0)       -none-      
//	RS> 2    NAME                  java.sql.Types.VARCHAR VARCHAR2(80)      -none-      
//	RS> 3    TYPE                  java.sql.Types.NUMERIC NUMBER(0,-127)    -none-      
//	RS> 4    VALUE                 java.sql.Types.VARCHAR VARCHAR2(4000)    -none-      
//	RS> 5    DISPLAY_VALUE         java.sql.Types.VARCHAR VARCHAR2(4000)    -none-      
//	RS> 6    ISDEFAULT             java.sql.Types.VARCHAR VARCHAR2(9)       -none-      
//	RS> 7    ISSES_MODIFIABLE      java.sql.Types.VARCHAR VARCHAR2(5)       -none-      
//	RS> 8    ISSYS_MODIFIABLE      java.sql.Types.VARCHAR VARCHAR2(9)       -none-      
//	RS> 9    ISINSTANCE_MODIFIABLE java.sql.Types.VARCHAR VARCHAR2(5)       -none-      
//	RS> 10   ISMODIFIED            java.sql.Types.VARCHAR VARCHAR2(10)      -none-      
//	RS> 11   ISADJUSTED            java.sql.Types.VARCHAR VARCHAR2(5)       -none-      
//	RS> 12   ISDEPRECATED          java.sql.Types.VARCHAR VARCHAR2(5)       -none-      
//	RS> 13   ISBASIC               java.sql.Types.VARCHAR VARCHAR2(5)       -none-      
//	RS> 14   DESCRIPTION           java.sql.Types.VARCHAR VARCHAR2(255)     -none-      
//	RS> 15   UPDATE_COMMENT        java.sql.Types.VARCHAR VARCHAR2(255)     -none-      
//	RS> 16   HASH                  java.sql.Types.NUMERIC NUMBER(0,-127)    -none-      

	private static String GET_CONFIG_ONLINE_SQL =
		"select current_timestamp as CONFIG_DATE \n"
//		+ "     ,ISDEFAULT        as NON_DEFAULT \n"
		+ "     ,CASE WHEN ISDEFAULT = 'TRUE' THEN 'FALSE' ELSE 'TRUE' END as NON_DEFAULT \n"
		+ "     ,'Unknown'        as SECTION \n"
		+ "     ,NAME             as NAME \n"
//		+ "     ,VALUE            as VALUE \n"
		+ "     ,CASE WHEN regexp_like(VALUE,'^[0-9]+$') THEN TO_NUMBER(VALUE) ELSE -1 END as VALUE \n"
		+ "     ,'FALSE'          as PENDING \n"
		+ "     ,0                as PENDING_VALUE \n"
		+ "     ,''               as DEFAULT_VALUE \n"
		+ "     ,''               as USED_MEMORY \n"
		+ "     ,''               as CONFIG_UNIT \n"
		+ "     ,'FALSE'          as RESTART_REQ \n"
		+ "     ,VALUE            as CHAR_VALUE \n"
		+ "     ,''               as CHAR_PENDING \n"
		+ "     ,'FALSE'          as READONLY \n"
		+ "     ,'xxx'            as TYPE \n"
		+ "     ,0                as MIN_VALUE \n"
		+ "     ,0                as MAX_VALUE \n"
		+ "     ,DESCRIPTION      as DESCRIPTION \n"
		+ "     ,NUM              as CONFIG_ID \n"
		+ "     ,0                as PARENT_ID \n"
		+ "     ,0                as DISP_LEVEL \n"
		+ "     ,TYPE             as DATA_TYPE \n"
		+ "from v$parameter";

	private static String GET_CONFIG_OFFLINE_SQL = 
		"select * " +
		"from [" + PersistWriterJdbc.getTableName(null, null, PersistWriterJdbc.SESSION_DBMS_CONFIG, null, false) + "] \n" +
		"where [SessionStartTime] = SESSION_START_TIME \n";

	private static String GET_CONFIG_OFFLINE_MAX_SESSION_SQL = 
		" (select max([SessionStartTime]) " +
		"  from ["+PersistWriterJdbc.getTableName(null, null, PersistWriterJdbc.SESSION_DBMS_CONFIG, null, false) + "]" +
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
//					"     [NAME] \n" +
//					"    ,[VALUE] \n" +
//					"from [" + tabName + "] \n" +
//					"where [SessionStartTime] = (select max([SessionStartTime]) from [" + tabName + "]) \n" +
//					"order by 1 \n" +
//					"";
//		}
//		else
//		{
//			return
//					"select \n"
//					+ "      NAME             as NAME \n"
//					+ "     ,CASE WHEN regexp_like(VALUE,'^[0-9]+$') THEN TO_NUMBER(VALUE) ELSE -1 END as VALUE \n"
//					+ "from v$parameter \n" +
//					"order by 1 \n" +
//					"";
//		}
//	}
		
	/** hashtable with MonTableEntry */
	private HashMap<String,OracleConfigEntry> _configMap  = null;
	private ArrayList<OracleConfigEntry>      _configList = null;

	private ArrayList<String>              _configSectionList = null;

	public class OracleConfigEntry
	implements IDbmsConfigEntry
	{
		@Override public String getConfigKey()    { return configName; }
		@Override public String getConfigValue()  { return configValueString; }
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
		public long    configValue;

		/** true if the configuration value is set, but we need to restart the ASE server to enable the configuration */
		public boolean isPending;

		/** integer representation of the pending configuration */
		public long    pendingValue;

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
		public long    minValue;

		/** maximum value of a integer value */
		public long    maxValue;

		/** description of the configuration option */
		public String  description;

		/** ID of the configuration option */
		public long    configId;

		/** ID of the sections in the configuration */
		public long    parentId;

		/** display level */
		public long    displayLevel;

		/** datatype ?, dont know if this is the ASE, JDBC or ODBC ID of the datatype */
		public long    dataType;
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
		}

		// replace all '[' and ']' into DBMS Vendor Specific Chars
		sql = conn.quotifySqlString(sql);

		try
		{
			Statement stmt = conn.createStatement();
			stmt.setQueryTimeout(10);

			ResultSet rs = stmt.executeQuery(sql);
			while ( rs.next() )
			{
				OracleConfigEntry entry = new OracleConfigEntry();

				int pos = 2; 
				// Skip first column
				//   1: online  = CONFIG_DATE
				//   2: offline = SessionStartTime
				_timestamp               = rs.getTimestamp(1);
				entry.isNonDefault       = rs.getBoolean(pos++);
				entry.sectionName        = rs.getString (pos++);
				entry.configName         = rs.getString (pos++);
				entry.configValue        = rs.getLong   (pos++);
				entry.isPending          = rs.getBoolean(pos++);
				entry.pendingValue       = rs.getLong   (pos++);
				entry.defaultValue       = rs.getString (pos++);
				entry.usedMemoryStr      = rs.getString (pos++);
				entry.configUnit         = rs.getString (pos++);
				entry.requiresRestart    = rs.getBoolean(pos++);
				entry.configValueString  = rs.getString (pos++);
				entry.pendingValueString = rs.getString (pos++);
				entry.isReadOnly         = rs.getBoolean(pos++);
				entry.configType         = rs.getString (pos++);
				entry.minValue           = rs.getLong   (pos++);
				entry.maxValue           = rs.getLong   (pos++);
				entry.description        = rs.getString (pos++);
				entry.configId           = rs.getLong   (pos++);
				entry.parentId           = rs.getLong   (pos++);
				entry.displayLevel       = rs.getLong   (pos++);
				entry.dataType           = rs.getLong   (pos++);

				// fix 'used memory' column
				if ( ! _offline )
				{
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
			_logger.error("OracleConfig:initialize:sql='"+sql+"'", ex);
			if (_hasGui)
				SwingUtils.showErrorMessage("OracleConfig - Initialize", "SQL Exception: "+ex.getMessage()+"\n\nThis was found when executing SQL statement:\n\n"+sql, ex);
			_configMap = null;
			_configList = null;
			_configSectionList = null;

			// JZ0C0: Connection is already closed.
			// JZ006: Caught IOException: com.sybase.jdbc4.jdbc.SybConnectionDeadException: JZ0C0: Connection is already closed.
			if ( "JZ0C0".equals(ex.getSQLState()) || "JZ006".equals(ex.getSQLState()) )
			{
				try
				{
					_logger.info("OracleConfig:initialize(): lost connection... try to reconnect...");
					conn.reConnect(null);
					_logger.info("OracleConfig:initialize(): Reconnect succeeded, but the configuration will not be visible");
				}
				catch(Exception reconnectEx)
				{
					_logger.warn("OracleConfig:initialize(): reconnect failed due to: "+reconnectEx);
					throw ex; // Note throw the original exception and not reconnectEx
				}
			}

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
		OracleConfigEntry e = _configList.get(rowIndex);
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
		return "Oracle Config";
	}

	@Override
	public String getCellToolTip(int mrow, int mcol)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("<b>Description:  </b>").append(getValueAt(mrow, findColumn(OracleConfig.DESCRIPTION))) .append("<br>");
		sb.append("<b>Section Name: </b>").append(getValueAt(mrow, findColumn(OracleConfig.SECTION_NAME))).append("<br>");
		sb.append("<b>Config Name:  </b>").append(getValueAt(mrow, findColumn(OracleConfig.CONFIG_NAME))) .append("<br>");
		sb.append("<b>Run Value:    </b>").append(getValueAt(mrow, findColumn(OracleConfig.CONFIG_VALUE))).append("<br>");
		sb.append("<b>Max Value:    </b>").append(getValueAt(mrow, findColumn(OracleConfig.MAX_VALUE)))   .append("<br>");
		sb.append("<b>Min Value:    </b>").append(getValueAt(mrow, findColumn(OracleConfig.MIN_VALUE)))   .append("<br>");
		if (Boolean.TRUE.equals(getValueAt(mrow, findColumn(OracleConfig.PENDING))))
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
		sb.append("** At Date:            ").append(new Timestamp(System.currentTimeMillis())).append("\n");
		sb.append("** Oracle Server name: ").append(getDbmsServerName()).append("\n");
		sb.append("** Oracle Version:     ").append(getDbmsVersionStr()).append("\n");
		sb.append("*/ \n");
		sb.append("\n");
		for (int r=0; r<modelRows.length; r++)
		{
			int mrow = modelRows[r];

			// SKIP: If it's a read only configuration 
			if ("read-only".equals(getValueAt(mrow, findColumn(OracleConfig.TYPE))))
				continue;

			sb.append("---------------------------------------------------------------------------\n");
			sb.append("-- Config Name:   ").append(getValueAt(mrow, findColumn(OracleConfig.CONFIG_NAME))) .append("\n");
			sb.append("-- Description:   ").append(getValueAt(mrow, findColumn(OracleConfig.DESCRIPTION))) .append("\n");
			sb.append("-- Section Name:  ").append(getValueAt(mrow, findColumn(OracleConfig.SECTION_NAME))).append("\n");
			sb.append("-- Type:          ").append(getValueAt(mrow, findColumn(OracleConfig.TYPE))).append("\n");

			String cfgValStr = getValueAt(mrow, findColumn(OracleConfig.CFG_VAL_STR)) + "";
			if (NumberUtils.isNumber(cfgValStr))
			{
				sb.append("-- Default Value: ").append(getValueAt(mrow, findColumn(OracleConfig.DEFAULT_VALUE))).append("\n");
				sb.append("-- Min Value:     ").append(getValueAt(mrow, findColumn(OracleConfig.MIN_VALUE))).append("\n");
				sb.append("-- Max Value:     ").append(getValueAt(mrow, findColumn(OracleConfig.MAX_VALUE))).append("\n");
				
    			sb.append("ALTER SYSTEM SET ").append(getValueAt(mrow, findColumn(OracleConfig.CONFIG_NAME))).append(" = ");
    			sb.append(getValueAt(mrow, findColumn(OracleConfig.CONFIG_VALUE))).append("");
    			sb.append("\n");
    			sb.append("go\n");
			}
			else
			{
    			sb.append("ALTER SYSTEM SET ").append(getValueAt(mrow, findColumn(OracleConfig.CONFIG_NAME))).append(" = '").append(cfgValStr).append("'");
    			sb.append("\n");
    			sb.append("go\n");
			}
			sb.append("\n");
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
		sb.append("** Oracle Version:     ").append(getDbmsVersionStr()).append("\n");
		if (StringUtil.hasValue(comment))
			sb.append("** Comment:            ").append(comment).append("\n");
		sb.append("*/ \n");
		sb.append("\n");

		for (Entry<String, String> e : keyValMap.entrySet())
		{
			String key = e.getKey();
			String val = e.getValue();

			OracleConfigEntry cfgEntry = _configMap.get(e.getKey());

			sb.append("---------------------------------------------------------------------------\n");
			sb.append("-- Config Name:   ").append(key)                  .append("\n");
			sb.append("-- Description:   ").append(cfgEntry.description) .append("\n");
			sb.append("-- Section Name:  ").append(cfgEntry.sectionName) .append("\n");
			sb.append("-- Type:          ").append(cfgEntry.configType)  .append("\n");
			sb.append("-- Default Value: ").append(cfgEntry.defaultValue).append("\n");

			sb.append("-- Prev Value:    ").append(cfgEntry.getConfigValue()).append("\n");
			sb.append("-- New Value:     ").append(val)                      .append("\n");

			if (cfgEntry.isReadOnly)
			{
				sb.append("-- NOTE -- This is a 'read-only' configuration... so nothing will be done here.\n");
				sb.append("\n");
				sb.append("go\n");
				continue;
			}
			if (NumberUtils.isNumber(val))
			{
				sb.append("-- Min Value:     ").append(cfgEntry.minValue).append("\n");
				sb.append("-- Max Value:     ").append(cfgEntry.maxValue).append("\n");

				sb.append("ALTER SYSTEM SET ").append(key).append(" = ").append(e.getValue()).append("");
    			sb.append("\n");
    			sb.append("go\n");
			}
			else
			{
    			sb.append("ALTER SYSTEM SET ").append(key).append(" = '").append(val).append("'");
    			sb.append("\n");
    			sb.append("go\n");
			}
		}
		
		return sb.toString();
	}
}
