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

import org.apache.log4j.Logger;

import com.asetune.pcs.PersistWriterJdbc;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.SwingUtils;


public class RaxConfig
extends DbmsConfigAbstract 
{
	private static final long serialVersionUID = 1L;

	/** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(RaxConfig.class);

	private boolean   _offline   = false;
	private boolean   _hasGui    = false;
	private Timestamp _timestamp = null;

//	1> ra_config
//	RS> Col# Label          JDBC Type Name         Guessed DBMS type
//	RS> ---- -------------- ---------------------- -----------------
//	RS> 1    Parameter Name java.sql.Types.CHAR    char(31)         
//	RS> 2    Parameter Type java.sql.Types.CHAR    char(9)          
//	RS> 3    Current Value  java.sql.Types.CHAR    char(78)         
//	RS> 4    Pending Value  java.sql.Types.CHAR    char(1)          
//	RS> 5    Default Value  java.sql.Types.CHAR    char(74)         
//	RS> 6    Legal Values   java.sql.Types.CHAR    char(38)         
//	RS> 7    Category       java.sql.Types.CHAR    char(25)         
//	RS> 8    Restart        java.sql.Types.INTEGER int              
//	RS> 9    Description    java.sql.Types.CHAR    char(252)        
//	+---------------------+--------------+-------------+-------------+----------------+---------------+--------+-------+---------------------------------------------------------------+
//	|Parameter Name       |Parameter Type|Current Value|Pending Value|Default Value   |Legal Values   |Category|Restart|Description                                                    |
//	+---------------------+--------------+-------------+-------------+----------------+---------------+--------+-------+---------------------------------------------------------------+
//	|admin_port           |int           |20500        |(NULL)       |10000           |range: 1,65535 |Admin   |1      |The client socket port number.                                 |
//	|admin_port_chunk_size|int           |1024         |(NULL)       |1024            |range: 1, 65535|Admin   |0      |The client socket port stream chunk size.                      |
//	|asa_password         |password      |******       |(NULL)       |N/A             |N/A            |ASA     |0      |ASA database user password.                                    |
//	|asm_password         |password      |******       |(NULL)       |<not_configured>|N/A            |PDS User|0      |Specifies the user password used when connecting to Oracle ASM.|
//	+---------------------+--------------+-------------+-------------+----------------+---------------+--------+-------+---------------------------------------------------------------+
//	(139 rows affected)
	
	public static final String NON_DEFAULT          = "NonDefault";
	public static final String CONFIG_NAME          = "ParameterName";
	public static final String CONFIG_TYPE          = "ParameterType";
	public static final String CURENT_VALUE         = "CurrentValue";
	public static final String PENDING              = "Pending";
	public static final String PENDING_VALUE        = "PendingValue";
	public static final String DEFAULT_VALUE        = "DefaultValue";
	public static final String LEGAL_VALUES         = "LegalValues";
	public static final String SECTION_NAME         = "Category";
	public static final String RESTART_IS_REQ       = "Restart";
	public static final String DESCRIPTION          = "Description";

	private static final int COL_NAME        = 0;
	private static final int COL_EDITABLE    = 1;
	private static final int COL_CLASS       = 2;
	private static final int COL_SQLTYPE     = 3;
	private static final int COL_TOOLTIP     = 4;
	private static Object[][] COLUMN_HEADERS = 
	{
	//   ColumnName,           Editable, JTable type,   SQL Datatype,   Tooltip
	//   --------------------- --------- -------------- --------------- --------------------------------------------
		{NON_DEFAULT,          false,    Boolean.class, "bit",          "True if the value is not configured to the default value."},
		{SECTION_NAME,         true,     String .class, "varchar(60)",  "Configuration Group"},
		{CONFIG_NAME,          true,     String .class, "varchar(60)",  "Name of the configuration, same as the name in ra_config."},
		{CONFIG_TYPE,          true,     String .class, "varchar(30)",  "What type of parameter is this."},
		{CURENT_VALUE,         true,     String .class, "varchar(255)", "Value of the configuration."},
		{PENDING,              false,    Boolean.class, "bit",          "The Configuration has not yet taken effect, probably needs restart to take effect."},
		{PENDING_VALUE,        true,     String .class, "varchar(255)", "The value which will be configured on next restart, if PENDING is true."},
		{DEFAULT_VALUE,        true,     String .class, "varchar(100)", "The default configuration value."},
		{RESTART_IS_REQ,       false,    Boolean.class, "bit",          "ASE needs to be rebooted for the configuration to take effect."},
		{LEGAL_VALUES,         true,     String .class, "varchar(60)",  "What legal values can this configuration hold"},
		{DESCRIPTION,          true,     String .class, "varchar(255)", "Description of the configuration."}
	};

	private static String GET_CONFIG_ONLINE_SQL = "ra_config";

	private static String GET_CONFIG_OFFLINE_SQL = 
		"select * " +
		"from [" + PersistWriterJdbc.getTableName(null, PersistWriterJdbc.SESSION_DBMS_CONFIG, null, false) + "] \n" +
		"where [SessionStartTime] = SESSION_START_TIME \n";

	private static String GET_CONFIG_OFFLINE_MAX_SESSION_SQL = 
		" (select max([SessionStartTime]) " +
		"  from ["+PersistWriterJdbc.getTableName(null, PersistWriterJdbc.SESSION_DBMS_CONFIG, null, false) + "]" +
		" ) ";


	/** hash table */
	private HashMap<String,RaxConfigEntry> _configMap  = null;
	private ArrayList<RaxConfigEntry>      _configList = null;

	private ArrayList<String>              _configSectionList = null;

	public class RaxConfigEntry
	{
		/** configuration has been changed by any user */
		public boolean isNonDefault;

		/** Different sections in the configuration */
		public String  sectionName;

		/** Name of the configuration */
		public String  configName;

		/** What type of parameter is this. int, password, etc... */
		public String  configType;

		/** integer representation of the configuration */
		public String  configValue;

		/** true if the configuration value is set, but we need to restart the ASE server to enable the configuration */
		public boolean isPending;

		/** integer representation of the pending configuration */
		public String  pendingValue;

		/** String representation of the default configuration */
		public String  defaultValue;

		/** true if the server needs to be restarted to enable the configuration */
		public boolean requiresRestart;

		/** What legal values can this configuration hold */
		public String  legalValues;

		/** description of the configuration option */
		public String  description;
	}

	public RaxConfig()
	{
//		DbmsConfigTextManager.clear();
//		RaxConfigText.createAndRegisterAllInstances();
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
//	public Map<String,RaxConfigEntry> getRaxConfigMap()
//	{
//		return _configMap;
//	}

	/** check if the RaxConfig is initialized or not */
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
		
		_configMap         = new HashMap<String,RaxConfigEntry>();
		_configList        = new ArrayList<RaxConfigEntry>();
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
				ResultSet rs = stmt.executeQuery("ra_date");
				while ( rs.next() )
					_timestamp = rs.getTimestamp(1);
				rs.close();

				// Then execute the Real query
				rs = stmt.executeQuery(sql);
				while ( rs.next() )
				{
//System.out.println("RaxConfig.refresh(): read row...");
					RaxConfigEntry entry = new RaxConfigEntry();

					// RS> 1    Parameter Name java.sql.Types.CHAR    char(31)         
					// RS> 2    Parameter Type java.sql.Types.CHAR    char(9)          
					// RS> 3    Current Value  java.sql.Types.CHAR    char(78)         
					// RS> 4    Pending Value  java.sql.Types.CHAR    char(1)          
					// RS> 5    Default Value  java.sql.Types.CHAR    char(74)         
					// RS> 6    Legal Values   java.sql.Types.CHAR    char(38)         
					// RS> 7    Category       java.sql.Types.CHAR    char(25)         
					// RS> 8    Restart        java.sql.Types.INTEGER int              
					// RS> 9    Description    java.sql.Types.CHAR    char(252)        

					//entry.isNonDefault     = Set this below: configValue != defaultValue
					entry.configName         = rs.getString (1);
					entry.configType         = rs.getString (2);
					entry.configValue        = rs.getString (3);
					entry.pendingValue       = rs.getString (4);
					entry.defaultValue       = rs.getString (5);
					entry.legalValues        = rs.getString (6); 
					entry.sectionName        = rs.getString (7);
					entry.isPending          = entry.pendingValue != null;
					entry.requiresRestart    = rs.getInt    (8) > 0; 
					entry.description        = rs.getString (9);

					// fix 'used memory' column
					if (entry.configValue == null)
						entry.isNonDefault = entry.configValue != entry.defaultValue;
					else
						entry.isNonDefault = ! entry.configValue.equals(entry.defaultValue);

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
					RaxConfigEntry entry = new RaxConfigEntry();

					// NOTE: same order as in COLUMN_HEADERS
					_timestamp               = rs.getTimestamp(1);
					entry.isNonDefault       = rs.getBoolean(2);
					entry.sectionName        = rs.getString (3);
					entry.configName         = rs.getString (4);
					entry.configType         = rs.getString (5);
					entry.configValue        = rs.getString (6);
					entry.isPending          = rs.getBoolean(7);
					entry.pendingValue       = rs.getString (8);
					entry.defaultValue       = rs.getString (9);
					entry.requiresRestart    = rs.getBoolean(10); 
					entry.legalValues        = rs.getString (11); 
					entry.description        = rs.getString (12);

					// fix 'used memory' column
					if ( ! _offline )
					{
						if (entry.configValue == null)
							entry.isNonDefault = entry.configValue != entry.defaultValue;
						else
							entry.isNonDefault = entry.configValue.equals(entry.defaultValue);
					}
					
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
			_logger.error("RaxConfig:initialize:sql='"+sql+"'", ex);
			if (_hasGui)
				SwingUtils.showErrorMessage("RaxConfig - Initialize", "SQL Exception: "+ex.getMessage()+"\n\nThis was found when executing SQL statement:\n\n"+sql, ex);
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

		RaxConfigEntry e = _configMap.get(configName);
		
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
//		RaxConfigEntry e = _configMap.get(configName);
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
		RaxConfigEntry e = _configList.get(rowIndex);
		if (e == null)
			return null;

		switch (columnIndex)
		{
		case 0:  return e.isNonDefault;
		case 1:  return e.sectionName;
		case 2:  return e.configName;
		case 3:  return e.configType;
		case 4:  return e.configValue;
		case 5:  return e.isPending;
		case 6:  return e.pendingValue;
		case 7:  return e.defaultValue;
		case 8:  return e.requiresRestart; 
		case 9:  return e.legalValues; 
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
		return "RAX Config";
	}

	@Override
	public String getCellToolTip(int mrow, int mcol)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("<b>Description:  </b>").append(getValueAt(mrow, findColumn(RaxConfig.DESCRIPTION))) .append("<br>");
		sb.append("<b>Section Name: </b>").append(getValueAt(mrow, findColumn(RaxConfig.SECTION_NAME))).append("<br>");
		sb.append("<b>Config Name:  </b>").append(getValueAt(mrow, findColumn(RaxConfig.CONFIG_NAME))) .append("<br>");
		sb.append("<b>Run Value:    </b>").append(getValueAt(mrow, findColumn(RaxConfig.CURENT_VALUE))).append("<br>");
		if (Boolean.TRUE.equals(getValueAt(mrow, findColumn(RaxConfig.PENDING))))
		{
			sb.append("<br>");
			sb.append("<b>NOTE: RAX Needs to be rebooted for this option to take effect.</b>").append("<br>");
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
