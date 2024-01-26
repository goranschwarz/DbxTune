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
package com.asetune.config.dict;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;

import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.StringUtil;


public abstract class MonTablesDictionary
{
    /** Log4j logging. */
//	private static Logger _logger          = Logger.getLogger(MonTablesDictionary.class);

	/** Was initialized using a GUI */
	private boolean _hasGui = false;

	/** hashtable with MonTableEntry */
//	private HashMap<String,MonTableEntry> _monTables = null;
	private HashMap<String,MonTableEntry> _monTables = new HashMap<String,MonTableEntry>();

	/** has been initalized or not */
	private boolean _initialized = false;
	
	/** has VersionInfo been initalized or not */
	private boolean _versionInfoInitialized = false;

	/** will be true after initializeVersionInfo() has been executed, to help CheckForUpdate.sendLogInfo() */
	private boolean _hasEarlyVersionInfo = false;

	/** DBMS @@servername string */
	private String _dbmsServerName = "";

	/** DBMS @@version string */
	private String _dbmsVersionStr = "";

	/** Calculate the @@version into a number. example: 12.5.4 -> 125400000 (15.7.0 SP100: 157010000), 16.0 SP01 PL01 = 160000101 */
	private long   _dbmsVersionNum = 0;

	/** If the version string contained the string 'Cluster Edition', this member will be true. */
	private boolean _isClusterEnabled = false;

	/** DBMS sp_version 'installmontables' string */
	private String _montablesVersionStr = "";

	/** Calculate the sp_version 'installmontables' into a number. example: 12.5.4 -> 125400000 */
	private long   _montablesVersionNum = 0;

	/** sp_version 'installmontables' Status string */
	private String _montablesStatus = "";

	/** DBMS sp_version 'installmaster' string */
	private String _installmasterVersionStr = "";

	/** Calculate the sp_version 'installmontables' into a number. example: 12.5.4 -> 125400000 */
	private long   _installmasterVersionNum = 0;

	/** sp_version 'installmontables' Status string */
	private String _installmasterStatus = "";

	/** If ASE Binary and monTable/installMaster version is out of sync, what MDA version layout should we use<br>
	 * If this is true use the monTable/installMaster<br>
	 * If this is false use the ASE Binary (which could lead to incorrect syntax etc, due to missing column names introduced in later ASE Binary versions, but installmaster has not been installed after upgrade)  
	 */
	private boolean _trustMonTablesVersion = true;
	public boolean trustMonTablesVersion() { return _trustMonTablesVersion; }
	public void setTrustMonTablesVersion(boolean trustMonTablesVersion) { _trustMonTablesVersion = trustMonTablesVersion; }

	/** DBMS Sort Order ID */
	private String  _dbmsSortId     = "";
	/** DBMS Sort Order Name */
	private String  _dbmsSortName   = "";
	/** DBMS Charset ID */
	private String  _dbmsCharsetId     = "";
	/** DBMS Charset Name */
	private String  _dbmsCharsetName   = "";
	/** just a guess if this is a SAP system, if the user 'sapsa' exists as a login, if db 'saptools' exists */
	private String  _sapSystemInfo = "";

//	private String _dbmsHostPortStr = "";
//	private String _dbmsUser        = "";

	
	/**
	 * Initialize this Dictionary
	 *  
	 * @param conn
	 */
	public abstract void initialize(DbxConnection conn, boolean hasGui);
	
	/**
	 * This should be called from initialize() if you want to populate the ToolTip for JTable column heading
	 * 
	 * @param conn
	 * @param offline
	 */
	public abstract void initializeMonTabColHelper(DbxConnection conn, boolean offline);

	/**
	 * This can be called from ConnectionDailog "extra actions" using the object ConnectionProgressExtraActions and method initializeVersionInfo()<br>
	 * which is created at MainFrameXxx.createConnectionProgressExtraActions()<br>
	 * <br>
	 * The Version Number information could then be used by the error subsystem etc... 
	 *  
	 * @param conn
	 * @param hasGui
	 */
	public abstract void initializeVersionInfo(DbxConnection conn, boolean hasGui);

	
	/**
	 * Reset the dictionary, this so we can get new ones later<br>
	 * Most possible called from disconnect() or similar
	 */
	public void reset()
	{
		_initialized             = false;
		_versionInfoInitialized  = false;
                                 
		_hasGui                  = false;
		_monTables               = new HashMap<String,MonTableEntry>();
		_hasEarlyVersionInfo     = false;
		_dbmsServerName          = "";
		_dbmsVersionStr          = "";
		_dbmsVersionNum          = 0;
		_isClusterEnabled        = false;
		_montablesVersionStr     = "";
		_montablesVersionNum     = 0;
		_montablesStatus         = "";
		_installmasterVersionStr = "";
		_installmasterVersionNum = 0;
		_installmasterStatus     = "";
		_trustMonTablesVersion   = true;
		_dbmsSortId              = "";
		_dbmsSortName            = "";
		_dbmsCharsetId           = "";
		_dbmsCharsetName         = "";
		_sapSystemInfo           = "";
//		_dbmsHostPortStr         = "";
//		_dbmsUser                = "";

		_sysMonitorsInfo         = new HashMap<String,String>();
		_monWaitClassInfo        = null;
		_monWaitEventInfo        = null;
	}



	
	/** 
	 * The version we should use when creating SQL Statements for the MDA Tables
	 * depends on Executable/installMaster/monTable and to some extent the ASE version if below ASE 12.5.4 
	 * and user questions '_trustMonTablesVersion' 
	 */
	public long getDbmsMonTableVersion()
	{
		long ver = 0;

		if (_trustMonTablesVersion)
			ver = getDbmsMonTableVersionNum();

		if (ver <= 0)
			ver = getDbmsExecutableVersionNum();

		return ver;
	}

	public boolean isClusterEnabled()              { return _isClusterEnabled; }
	public String  getDbmsServerName()              { return _dbmsServerName; }
	
	public String  getDbmsExecutableVersionStr()    { return _dbmsVersionStr; }
	public long    getDbmsExecutableVersionNum()    { return _dbmsVersionNum; }
	
	public String  getDbmsInstallMasterStatusStr()  { return _installmasterStatus; }
	public String  getDbmsInstallMasterVersionStr() { return _installmasterVersionStr; }
	public long    getDbmsInstallMasterVersionNum() { return _installmasterVersionNum; }

	public String  getDbmsMonTableStatusStr()       { return _montablesStatus; }
	public String  getDbmsMonTableVersionStr()      { return _montablesVersionStr; }
	public long    getDbmsMonTableVersionNum()      { return _montablesVersionNum; }

	public String  getDbmsSortId()                 { return _dbmsSortId; }
	public String  getDbmsSortName()               { return _dbmsSortName; }
	public String  getDbmsCharsetId()              { return _dbmsCharsetId; }
	public String  getDbmsCharsetName()            { return _dbmsCharsetName; }
	public String  getSapSystemInfo()              { return _sapSystemInfo; }

//	public String  getDbmsHostPortStr()             { return _dbmsHostPortStr; }
//	public String  getDbmsUser()                    { return _dbmsUser; }

	
	public void setClusterEnabled(boolean isEnabled)          { _isClusterEnabled        = isEnabled; }
	public void setDbmsServerName(String serverName)          { _dbmsServerName          = serverName; }
	
	public void setDbmsExecutableVersionStr(String verStr)    { _dbmsVersionStr          = verStr; }
	public void setDbmsExecutableVersionNum(long   verNum)    { _dbmsVersionNum          = verNum; }
	
	public void setDbmsInstallMasterVersionStr(String verStr) { _installmasterVersionStr = verStr; }
	public void setDbmsInstallMasterVersionNum(long   verNum) { _installmasterVersionNum = verNum; }
	public void setDbmsInstallMasterStatusStr(String status)  { _installmasterStatus     = status; }

	public void setDbmsMonTableVersionStr(String verStr)      { _montablesVersionStr     = verStr; }
	public void setDbmsMonTableVersionNum(long   verNum)      { _montablesVersionNum     = verNum; }
	public void setDbmsMonTableStatusStr(String status)       { _montablesStatus         = status; }

	public void setDbmsSortId(String id)                      { _dbmsSortId      = id; }
	public void setDbmsSortName(String name)                  { _dbmsSortName    = name; }
	public void setDbmsCharsetId(String id)                   { _dbmsCharsetId   = id; }
	public void setDbmsCharsetName(String name)               { _dbmsCharsetName = name; }
	public void setSapSystemInfo(String sapInfo)              { _sapSystemInfo   = sapInfo; }

//	public void setDbmsHostPortStr(String hostPortStr)        { _dbmsHostPortStr = hostPortStr; }
//	public void setDbmsUser(String userName)                  { _dbmsUser        = userName; }



	/**
	 * If the dictionary brings counter descriptions from the DBMS, we would want to store them in the PCS database recording.<br>
	 * But if it's <b>only</b> static values that are initiated/created by provider, then we do NOT need to store it in the PCS.
	 * 
	 * @return true if you want to save it in the PCS
	 */
	public abstract boolean isSaveMonTablesDictionaryInPcsEnabled();


	public static class MonTableEntry
	{
		public int       _tableID         = 0;    // Unique identifier for the table
		public int       _columns         = 0;    // Total number of columns in the table
		public int       _parameters      = 0;    // Total number of optional parameters that can be specified
		public int       _indicators      = 0;    // Indicators for specific table properties, e.g. if the table retains session context
		public int       _size            = 0;    // Maximum row size (in bytes)
		public String    _tableName       = null; // Name of the table
		public String    _description     = null; // Description of the table

		public static final int TABLE_NAME_MAXLEN  = 255;
		public static final int DESCRIPTION_MAXLEN = 8000;

		/** hashtable with MonTableColumnsEntry */
		public HashMap<String,MonTableColumnsEntry> _monTableColumns = null;

		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			sb.append(super.toString()).append(" _tableID="+_tableID+", _columns='"+_columns+"', _parameters='"+_parameters+"', _indicators='"+_indicators+"', _size='"+_size+"', _tableName='"+_tableName+"', _description='"+_description+"'.").append("\n");
			for (MonTableColumnsEntry ce : _monTableColumns.values())
				sb.append("    ").append(ce.toString()).append("\n");
			return sb.toString();
		}
	}

	public static class MonTableColumnsEntry
	{
		public int    _tableID     = 0;    // Unique identifier for the table
		public int    _columnID    = 0;    // Position of the column
		public int    _typeID      = 0;    // Identifier for the data type of the column
		public int    _precision   = 0;    // Precision of the column, if numeric
		public int    _scale       = 0;    // Scale of the column, if numeric
		public int    _length      = 0;    // Maximum length of the column (in bytes)
		public int    _indicators  = 0;    // Indicators for specific column properties, e.g. if the column is prone to wrapping and should be sampled 
		public String _tableName   = null; // Name of the table
		public String _columnName  = null; // Name of the column
		public String _typeName    = null; // Name of the data type of the column
		public String _description = null; // Description of the column

		public static final int TABLE_NAME_MAXLEN  = 255;
		public static final int COLUMN_NAME_MAXLEN = 255;
		public static final int TYPE_NAME_MAXLEN   = 255;
		public static final int DESCRIPTION_MAXLEN = 8000;
		
		@Override
		public String toString()
		{
			return super.toString()+" _tableID="+_tableID+", _columnID='"+_columnID+"', _typeID='"+_typeID+"', _precision='"+_precision+"', _scale='"+_scale+"', _length='"+_length+"', _indicators='"+_indicators+"', _tableName='"+_tableName+"', _columnName='"+_columnName+"', _typeName='"+_typeName+"', _description='"+_description+"'.";
		}
	}

	public static class MonWaitClassInfoEntry
	{
		int		_waitClassId    = 0;    // select * from monWaitClassInfo  WaitClassID Description
		String  _description    = null;
		
		@Override
		public String toString()
		{
			return "MonWaitClassInfoEntry _waitClassId="+_waitClassId+", _description='"+_description+"'.";
		}
	}
	public static class MonWaitEventInfoEntry
	{
		int		_waitEventId    = 0;    // select * from monWaitEventInfo  WaitEventID WaitClassID Description
		int		_waitClassId    = 0;
		String  _description    = null;

		@Override
		public String toString()
		{
			return "MonWaitEventInfoEntry _waitEventId="+_waitEventId+", _waitClassId="+_waitClassId+", _description='"+_description+"'.";
		}
	}

	private Map<String,String>      _sysMonitorsInfo  = new HashMap<String,String>();
	private MonWaitClassInfoEntry[] _monWaitClassInfo = null;
	private MonWaitEventInfoEntry[] _monWaitEventInfo = null;

	public void setMonWaitClassInfo(MonWaitClassInfoEntry[] monWaitClassInfo)
	{
		_monWaitClassInfo = monWaitClassInfo;
	}

	public void setMonWaitEventInfo(MonWaitEventInfoEntry[] monWaitEventInfo)
	{
		_monWaitEventInfo = monWaitEventInfo;
	}

//	/** Character used for quoted identifier */
//	public static String  qic = "\"";

	public void setMonTablesDictionaryMap(HashMap<String, MonTableEntry> monTablesMap)
	{
		_monTables = monTablesMap;
	}

	public Map<String,MonTableEntry> getMonTablesDictionaryMap()
	{
		return _monTables;
	}

	public boolean hasGui()
	{
		return _hasGui;
	}

	public void setGui(boolean hasGui)
	{
		_hasGui = hasGui;
	}

	
	public boolean isInitialized()
	{
		return _initialized;
	}

	public void setInitialized(boolean toValue)
	{
		_initialized = toValue;
	}
	
	public boolean isVersionInfoInitialized()
	{
		return _versionInfoInitialized;
	}

	public void setVersionInfoInitialized(boolean toValue)
	{
		_versionInfoInitialized = toValue;
	}

	public boolean hasEarlyVersionInfo()
	{
		return _hasEarlyVersionInfo;
	}
	public void setEarlyVersionInfo(boolean hasEarlyVersionInfo)
	{
		_hasEarlyVersionInfo = hasEarlyVersionInfo;
	}


//	public void initializeMonTabColHelper(Connection conn, boolean offline)
//	{
//		if (conn == null)
//			return;
//
//		_monTables = new HashMap<String,MonTableEntry>();
//
//		String monTables       = "monTables";
//		String monTableColumns = "monTableColumns";
//		if (offline)
//		{
//			monTables       = PersistWriterBase.getTableName(PersistWriterBase.SESSION_MON_TAB_DICT,     null, true);
//			monTableColumns = PersistWriterBase.getTableName(PersistWriterBase.SESSION_MON_TAB_COL_DICT, null, true);
//		}
//		
//		String sql = null;
//		try
//		{
//			Statement stmt = conn.createStatement();
//			sql = SQL_TABLES.replace(FROM_TAB_NAME, monTables);
//			if ( ! offline )
//			{
////				if (_srvVersionNum >= 15700)
////				if (_srvVersionNum >= 1570000)
//				if (_srvVersionNum >= Ver.ver(15,7))
//					sql += " where Language = 'en_US' ";
//
//				sql = sql.replace("\"", "");
//			}
//
//			ResultSet rs = stmt.executeQuery(sql);
//			while ( rs.next() )
//			{
//				MonTableEntry entry = new MonTableEntry();
//
//				int pos = 1;
//				entry._tableID      = rs.getInt   (pos++);
//				entry._columns      = rs.getInt   (pos++);
//				entry._parameters   = rs.getInt   (pos++);
//				entry._indicators   = rs.getInt   (pos++);
//				entry._size         = rs.getInt   (pos++);
//				entry._tableName    = rs.getString(pos++);
//				entry._description  = rs.getString(pos++);
//				
//				// Create substructure with the columns
//				// This is filled in BELOW (next SQL query)
//				entry._monTableColumns = new HashMap<String,MonTableColumnsEntry>();
//
//				_monTables.put(entry._tableName, entry);
//			}
//			rs.close();
//		}
//		catch (SQLException ex)
//		{
//			if (offline && ex.getMessage().contains("not found"))
//			{
//				_logger.warn("Tooltip on column headers wasn't available in the offline database. This simply means that tooltip wont be showed in various places.");
//				return;
//			}
//			_logger.error("MonTablesDictionary:initialize:sql='"+sql+"'", ex);
//			_monTables = null;
//			return;
//		}
//
//		for (Map.Entry<String,MonTableEntry> mapEntry : _monTables.entrySet()) 
//		{
//		//	String        key           = mapEntry.getKey();
//			MonTableEntry monTableEntry = mapEntry.getValue();
//			
//			if (monTableEntry._monTableColumns == null)
//			{
//				monTableEntry._monTableColumns = new HashMap<String,MonTableColumnsEntry>();
//			}
//			else
//			{
//				monTableEntry._monTableColumns.clear();
//			}
//
//			try
//			{
//				Statement stmt = conn.createStatement();
//				sql = SQL_COLUMNS.replace(FROM_TAB_NAME, monTableColumns);
//				sql = sql.replace(TAB_NAME, monTableEntry._tableName);
//				if ( ! offline )
//				{
////					if (_srvVersionNum >= 15700)
////					if (_srvVersionNum >= 1570000)
//					if (_srvVersionNum >= Ver.ver(15,7))
//						sql += " and Language = 'en_US' ";
//
//					sql = sql.replace("\"", "");
//				}
//
//				ResultSet rs = stmt.executeQuery(sql);
//				while ( rs.next() )
//				{
//					MonTableColumnsEntry entry = new MonTableColumnsEntry();
//
//					int pos = 1;
//					entry._tableID      = rs.getInt   (pos++);
//					entry._columnID     = rs.getInt   (pos++);
//					entry._typeID       = rs.getInt   (pos++);
//					entry._precision    = rs.getInt   (pos++);
//					entry._scale        = rs.getInt   (pos++);
//					entry._length       = rs.getInt   (pos++);
//					entry._indicators   = rs.getInt   (pos++);
//					entry._tableName    = rs.getString(pos++);
//					entry._columnName   = rs.getString(pos++);
//					entry._typeName     = rs.getString(pos++);
//					entry._description  = rs.getString(pos++);
//					
//					monTableEntry._monTableColumns.put(entry._columnName, entry);
//				}
//				rs.close();
//			}
//			catch (SQLException ex)
//			{
//				if (offline && ex.getMessage().contains("not found"))
//				{
//					_logger.warn("Tooltip on column headers wasn't available in the offline database. This simply means that tooltip wont be showed in various places.");
//					return;
//				}
//				_logger.error("MonTablesDictionary:initialize:sql='"+sql+"'", ex);
////				_monTables = null;
//				return;
//			}
//		}
//	}
//
//	
//	public void initializeVersionInfo(Connection conn, boolean hasGui)
//	{
//		if (conn == null)
//			return;
//		String sql = null;
//
//		// @@servername
//		_aseServerName = AseConnectionUtils.getAseServername(conn);
//
//		// srvVersionStr = @@version
//		// srvVersionNum = @@version -> to an integer
//		// isClusterEnabled...
//		try
//		{
//			sql = SQL_VERSION;
//			Statement stmt = conn.createStatement();
//			ResultSet rs = stmt.executeQuery(sql);
//			while ( rs.next() )
//			{
//				_srvVersionStr = rs.getString(1);
//			}
//			rs.close();
//			stmt.close();
//
//			_srvVersionNum = Ver.sybVersionStringToNumber(_srvVersionStr);
//
//			if (AseConnectionUtils.isClusterEnabled(conn))
//				_isClusterEnabled = true;
//		}
//		catch (SQLException ex)
//		{
//			_logger.error("initializeVersionInfo, @@version", ex);
//			if (_hasGui)
//				SwingUtils.showErrorMessage("MonTablesDictionary - initializeVersionInfo", "SQL Exception: "+ex.getMessage()+"\n\nThis was found when executing SQL statement:\n\n"+sql, ex);
//			return;
//		}
//
//		// SORT order ID and NAME
//		try
//		{
//			sql="declare @sortid tinyint, @charid tinyint \n" +
//				"select @sortid = value from master..syscurconfigs where config = 123 \n" +
//				"select @charid = value from master..syscurconfigs where config = 131  \n" +
//				"\n" +
//				"select 'sortorder', id, name \n" +
//				"from master.dbo.syscharsets \n" + 
//				"where id = @sortid and csid = @charid \n" +
//				"\n" +
//				"UNION ALL \n" +
//				"\n" +
//				"select 'charset', id, name \n" +
//				"from master.dbo.syscharsets \n" + 
//				"where id = @charid \n";
//			
//			Statement stmt = conn.createStatement();
//			ResultSet rs = stmt.executeQuery(sql);
//			while ( rs.next() )
//			{
//				String type = rs.getString(1);
//				if ("sortorder".equals(type))
//				{
//					_aseSortId   = rs.getInt   (2);
//					_aseSortName = rs.getString(3);
//				}
//				if ("charset".equals(type))
//				{
//					_aseCharsetId   = rs.getInt   (2);
//					_aseCharsetName = rs.getString(3);
//				}
//			}
//			rs.close();
//			stmt.close();
//		}
//		catch (SQLException ex)
//		{
//			_logger.error("initializeVersionInfo, Sort order information", ex);
//		}
//
//		_hasEarlyVersionInfo = true;
//	}
//
//	
//	public void initialize(Connection conn, boolean hasGui)
//	{
//		if (conn == null)
//			return;
//		_hasGui = hasGui;
//
//		// Do more things if user has MON_ROLE
//		boolean hasMonRole = AseConnectionUtils.hasRole(conn, AseConnectionUtils.MON_ROLE);
//
//		String sql = null;
//
//		//------------------------------------
//		// get values from monTables & monTableColumns
//		if (hasMonRole)
//			initializeMonTabColHelper(conn, false);
//
//		//------------------------------------
//		// monWaitClassInfo
//		if (hasMonRole)
//		{
//			try
//			{
//				sql = SQL_MON_WAIT_CLASS_INFO_1;
////				if (_srvVersionNum >= 15700)
////				if (_srvVersionNum >= 1570000)
//				if (_srvVersionNum >= Ver.ver(15,7))
//					sql += " where Language = 'en_US'";
//				Statement stmt = conn.createStatement();
//				ResultSet rs = stmt.executeQuery(sql);
//				int max_waitClassId = 0; 
//				while ( rs.next() )
//				{
//					max_waitClassId = rs.getInt(1); 
//				}
//				rs.close();
//	
//				_monWaitClassInfo = new MonWaitClassInfoEntry[max_waitClassId+1];
//
//				sql = SQL_MON_WAIT_CLASS_INFO;
////				if (_srvVersionNum >= 15700)
////				if (_srvVersionNum >= 1570000)
//				if (_srvVersionNum >= Ver.ver(15,7))
//					sql += " where Language = 'en_US'";
//				rs = stmt.executeQuery(sql);
//				while ( rs.next() )
//				{
//					MonWaitClassInfoEntry entry = new MonWaitClassInfoEntry();
//					int pos = 1;
//	
//					entry._waitClassId  = rs.getInt(pos++);
//					entry._description  = rs.getString(pos++);
//	
//					_logger.debug("Adding WaitClassInfo: " + entry);
//	
//					_monWaitClassInfo[entry._waitClassId] = entry;
//				}
//				rs.close();
//			}
//			catch (SQLException ex)
//			{
//				_logger.error("MonTablesDictionary:initialize, _monWaitClassInfo", ex);
//				if (_hasGui)
//					SwingUtils.showErrorMessage("MonTablesDictionary - Initialize", "SQL Exception: "+ex.getMessage()+"\n\nThis was found when executing SQL statement:\n\n"+sql, ex);
//				_monWaitClassInfo = null;
//				return;
//			}
//	
//			// _monWaitEventInfo
//			try
//			{
//				sql = SQL_MON_WAIT_EVENT_INFO_1;
////				if (_srvVersionNum >= 15700)
////				if (_srvVersionNum >= 1570000)
//				if (_srvVersionNum >= Ver.ver(15,7))
//					sql += " where Language = 'en_US'";
//				Statement stmt = conn.createStatement();
//				ResultSet rs = stmt.executeQuery(sql);
//				int max_waitEventId = 0; 
//				while ( rs.next() )
//				{
//					max_waitEventId = rs.getInt(1); 
//				}
//				rs.close();
//	
//				_monWaitEventInfo = new MonWaitEventInfoEntry[max_waitEventId+1];
//	
//				sql = SQL_MON_WAIT_EVENT_INFO;
////				if (_srvVersionNum >= 15700)
////				if (_srvVersionNum >= 1570000)
//				if (_srvVersionNum >= Ver.ver(15,7))
//					sql += " where Language = 'en_US'";
//				rs = stmt.executeQuery(sql);
//				while ( rs.next() )
//				{
//					MonWaitEventInfoEntry entry = new MonWaitEventInfoEntry();
//					int pos = 1;
//	
//					entry._waitEventId  = rs.getInt(pos++);
//					entry._waitClassId  = rs.getInt(pos++);
//					entry._description  = rs.getString(pos++);
//					
//					_logger.debug("Adding WaitEventInfo: " + entry);
//	
//					_monWaitEventInfo[entry._waitEventId] = entry;
//				}
//				rs.close();
//			}
//			catch (SQLException ex)
//			{
//				_logger.error("MonTablesDictionary:initialize, _monWaitEventInfo", ex);
//				if (_hasGui)
//					SwingUtils.showErrorMessage("MonTablesDictionary - Initialize", "SQL Exception: "+ex.getMessage()+"\n\nThis was found when executing SQL statement:\n\n"+sql, ex);
//				_monWaitEventInfo = null;
//				return;
//			}
//		}
//
//		//------------------------------------
//		// @@servername
//		_aseServerName = AseConnectionUtils.getAseServername(conn);
//		
//		//------------------------------------
//		// @@version_number
//		try
//		{
//			Statement stmt = conn.createStatement();
//			ResultSet rs = stmt.executeQuery(SQL_VERSION_NUM);
//			while ( rs.next() )
//			{
//				_srvVersionNum = rs.getInt(1);
//			}
//			rs.close();
//		}
//		catch (SQLException ex)
//		{
//			_logger.debug("MonTablesDictionary:initialize, @@version_number, probably an early ASE version", ex);
//		}
//
//		//------------------------------------
//		// version
//		try
//		{
//			sql = SQL_VERSION;
//			Statement stmt = conn.createStatement();
//			ResultSet rs = stmt.executeQuery(sql);
//			while ( rs.next() )
//			{
//				_srvVersionStr = rs.getString(1);
//			}
//			rs.close();
//
//			long srvVersionNumFromVerStr = Ver.sybVersionStringToNumber(_srvVersionStr);
//			_srvVersionNum = Math.max(_srvVersionNum, srvVersionNumFromVerStr);
//
//			// Check if the ASE binary is Cluster Edition Enabled
////			if (srvVersionStr.indexOf("Cluster Edition") >= 0)
////				isClusterEdition = true;
//			if (AseConnectionUtils.isClusterEnabled(conn))
//				_isClusterEnabled = true;
//		}
//		catch (SQLException ex)
//		{
//			_logger.error("MonTablesDictionary:initialize, @@version", ex);
//			if (_hasGui)
//				SwingUtils.showErrorMessage("MonTablesDictionary - Initialize", "SQL Exception: "+ex.getMessage()+"\n\nThis was found when executing SQL statement:\n\n"+sql, ex);
//			return;
//		}
//
//		//------------------------------------
//		// SORT order ID and NAME
//		try
//		{
//			sql="declare @sortid tinyint, @charid tinyint \n" +
//				"select @sortid = value from master..syscurconfigs where config = 123 \n" +
//				"select @charid = value from master..syscurconfigs where config = 131  \n" +
//				"\n" +
//				"select 'sortorder', id, name \n" +
//				"from master.dbo.syscharsets \n" + 
//				"where id = @sortid and csid = @charid \n" +
//				"\n" +
//				"UNION ALL \n" +
//				"\n" +
//				"select 'charset', id, name \n" +
//				"from master.dbo.syscharsets \n" + 
//				"where id = @charid \n";
//			
//			Statement stmt = conn.createStatement();
//			ResultSet rs = stmt.executeQuery(sql);
//			while ( rs.next() )
//			{
//				String type = rs.getString(1);
//				if ("sortorder".equals(type))
//				{
//					_aseSortId   = rs.getInt   (2);
//					_aseSortName = rs.getString(3);
//				}
//				if ("charset".equals(type))
//				{
//					_aseCharsetId   = rs.getInt   (2);
//					_aseCharsetName = rs.getString(3);
//				}
//			}
//			rs.close();
//			stmt.close();
//		}
//		catch (SQLException ex)
//		{
//			_logger.error("initializeVersionInfo, Sort order information", ex);
//		}
//
//		//------------------------------------
//		// Can this possible be a SAP System
//		try
//		{
//			_sapSystemInfo = "";
//
//			// CHECK for login 'sapsa'
//			// ----------------------
//			sql="select suid from master..syslogins where name = 'sapsa'";
//			
//			Statement stmt = conn.createStatement();
//			ResultSet rs = stmt.executeQuery(sql);
//			while ( rs.next() )
//			{
//				_sapSystemInfo += "USER:sapsa=true, ";
//
//				@SuppressWarnings("unused")
//				int suid = rs.getInt(1);
//			}
//			rs.close();
//			stmt.close();
//
//			// CHECK for db 'saptools'
//			// ----------------------
//			sql="select dbid from master..sysdatabases where name = 'saptools'";
//			
//			stmt = conn.createStatement();
//			rs = stmt.executeQuery(sql);
//			while ( rs.next() )
//			{
//				_sapSystemInfo += "DB:saptools=true, ";
//
//				@SuppressWarnings("unused")
//				int dbid = rs.getInt(1);
//			}
//			rs.close();
//			stmt.close();
//			
//			_sapSystemInfo = StringUtil.removeLastComma(_sapSystemInfo);
//		}
//		catch (SQLException ex)
//		{
//			_logger.error("initializeVersionInfo, When Probing if this looks like a SAP system, Caught exception.", ex);
//		}
//
//		//------------------------------------
//		// sp_version
////		if (_srvVersionNum >= 12540)
////		if (_srvVersionNum >= 1254000)
//		if (_srvVersionNum >= Ver.ver(12,5,4))
//		{
//			try
//			{
//				sql = SQL_SP_VERSION;
//				Statement stmt = conn.createStatement();
//				ResultSet rs = stmt.executeQuery(sql);
//				while ( rs.next() )
//				{
//					String spVersion_scriptName = rs.getString(1);
//					String spVersion_versionStr = rs.getString(2);
//					String spVersion_status     = rs.getString(3);
//	
//					if (spVersion_scriptName.endsWith("montables")) // could be: installmontables or montables
//					{
//						_montablesVersionStr = spVersion_versionStr;
//						_montablesStatus     = spVersion_status;
//	
//						_montablesVersionNum = Ver.sybVersionStringToNumber(_montablesVersionStr);
//		
//						if ( ! _montablesStatus.equalsIgnoreCase("Complete") )
//						{
//							_montablesStatus = "incomplete";
//						}
//					}
//	
//					if (spVersion_scriptName.equals("installmaster"))
//					{
//						_installmasterVersionStr = spVersion_versionStr;
//						_installmasterStatus     = spVersion_status;
//	
//						_installmasterVersionNum = Ver.sybVersionStringToNumber(_installmasterVersionStr);
//
//						if ( ! _installmasterStatus.equalsIgnoreCase("Complete") )
//						{
//							_installmasterStatus = "incomplete";
//						}
//					}
//				}
//				rs.close();
//			}
//			catch (SQLException ex)
//			{
//				// Msg 2812, Level 16, State 5:
//				// Server 'GORAN_12503_DS', Line 1:
//				// Stored procedure 'sp_version' not found. Specify owner.objectname or use sp_help to check whether the object exists (sp_help may produce lots of output).
//				if (ex.getErrorCode() == 2812)
//				{
//					String msg = "ASE 'installmaster' script may be of a faulty version. ASE Version is '"+Ver.versionNumToStr(_srvVersionNum)+"'. " +
//							"The stored procedure 'sp_version' was introduced in ASE 12.5.4, which I can't find in the connected ASE, this implies that 'installmaster' has not been applied after upgrade. " +
//							"Please apply '$SYBASE/$SYBASE_ASE/scripts/installmaster' and check it's status with: sp_version.";
//					_logger.error(msg);
//	
//					String msgHtml = 
//						"<html>" +
//						"ASE 'installmaster' script may be of a faulty version. <br>" +
//						"<br>" +
//						"ASE Version is '"+Ver.versionNumToStr(_srvVersionNum)+"'.<br>" +
//						"<br>" +
//						"The stored procedure 'sp_version' was introduced in ASE 12.5.4, which I can't find in the connected ASE, <br>" +
//						"this implies that 'installmaster' has <b>not</b> been applied after upgrade.<br>" +
//						"Please apply '$SYBASE/$SYBASE_ASE/scripts/installmaster' and check it's status by executing: <code>sp_version</code>. <br>" +
//						"<br>" +
//						"Do the following on the machine that hosts the ASE:<br>" +
//						"<font size=\"4\">" +
//						"  <code>isql -Usa -Psecret -SSRVNAME -w999 -i$SYBASE/$SYBASE_ASE/scripts/installmaster</code><br>" +
//						"</font>" +
//						"<br>" +
//						"If this is <b>not</b> done, SQL Statements issued by "+Version.getAppName()+" may fail due to version inconsistency (wrong column names etc).<br>" +
//						"<br>" +
//						"Also the MDA tables(mon*) may deliver faulty or corrupt information, because the MDA proxy table definitions are not in sync with it's underlying data structures.<br>" +
//						"</html>";
//					if (_hasGui)
//						SwingUtils.showErrorMessage(MainFrame.getInstance(), Version.getAppName()+" - MonTablesDictionary - Initialize", msgHtml, null);
//				}
//				else
//				{
//					_logger.warn("MonTablesDictionary:initialize, problems executing: "+SQL_SP_VERSION+ ". Exception: "+ex.getMessage());
//
//					String msgHtml = 
//						"<html>" +
//						"Problems when executing sp_version. <br>" +
//						"Msg: <code>"+ex.getErrorCode()+"</code><br>" +
//						"Text: <code>"+ex.getMessage()+"</code><br>" +
//						"<br>" +
//						"ASE 'installmaster' script may be of a faulty version. <br>" +
//						"Or the stored procedure 'sp_version' has been replaced with a customer specific one.<br>" +
//						"<br>" +
//						"ASE Version is '"+Ver.versionNumToStr(_srvVersionNum)+"'.<br>" +
//						"<br>" +
//						"To fix the issue Please apply '$SYBASE/$SYBASE_ASE/scripts/installmaster' again and check it's status by executing: <code>sp_version</code>. <br>" +
//						"<br>" +
//						"Do the following on the machine that hosts the ASE:<br>" +
//						"<font size=\"4\">" +
//						"  <code>isql -Usa -Psecret -SSRVNAME -w999 -i$SYBASE/$SYBASE_ASE/scripts/installmaster</code><br>" +
//						"</font>" +
//						"<br>" +
//						"If this is <b>not</b> done, SQL Statements issued by "+Version.getAppName()+" may fail due to version inconsistency (wrong column names etc).<br>" +
//						"<br>" +
//						"Also the MDA tables(mon*) may deliver faulty or corrupt information, because the MDA proxy table definitions are not in sync with it's underlying data structures.<br>" +
//						"</html>";
//					if (_hasGui)
//						SwingUtils.showErrorMessage(MainFrame.getInstance(), Version.getAppName()+" - MonTablesDictionary - Initialize", msgHtml, null);
//					return;
//				}
//			}
//		} // end: if (srvVersionNum >= 12.5.4)
//
//		_logger.info("ASE 'montables'     for sp_version shows: Status='"+_montablesStatus    +"', VersionNum='"+_montablesVersionNum    +"', VersionStr='"+_montablesVersionStr+"'.");
//		_logger.info("ASE 'installmaster' for sp_version shows: Status='"+_installmasterStatus+"', VersionNum='"+_installmasterVersionNum+"', VersionStr='"+_installmasterVersionStr+"'.");
//
//		//-------- montables ------
//		// is installed monitor tables fully installed.
//		if (hasMonRole)
//		{
//			if (_montablesStatus.equals("incomplete"))
//			{
//				String msg = "ASE Monitoring tables has not been completely installed. Please check it's status with: sp_version";
//				if (DbxTune.hasGui())
//					JOptionPane.showMessageDialog(MainFrame.getInstance(), msg, Version.getAppName()+" - connect check", JOptionPane.WARNING_MESSAGE);
//				_logger.warn(msg);
//			}
//		}
//
//		//------------------------------------
//		// is installed monitor tables version different than ASE version
//		if (_montablesVersionNum > 0)
//		{
//			//System.out.println("_srvVersionNum/1000='"+(_srvVersionNum/1000)+"', _montablesVersionNum/1000='"+(_montablesVersionNum/1000)+"'.");
//			//System.out.println("_srvVersionNum/100000='"+(_srvVersionNum/100000)+"', _montablesVersionNum/10000='"+(_montablesVersionNum/10000)+"'.");
//
//			// strip off the ROLLUP VERSION  (divide by 10 takes away last digit)
////			if (_srvVersionNum/10 != _montablesVersionNum/10)
////			if (_srvVersionNum/1000 != _montablesVersionNum/1000)
//			if (_srvVersionNum/100000 != _montablesVersionNum/100000) // Ver.ver(...) can we use that in some way here... if VER "length" changes the xx/100000 needs to be changed
//			{
//				String msg = "ASE Monitoring tables may be of a faulty version. ASE Version is '"+Ver.versionNumToStr(_srvVersionNum)+"' while MonTables version is '"+Ver.versionNumToStr(_montablesVersionNum)+"'. Please check it's status with: sp_version";
//				if (_hasGui)
//					JOptionPane.showMessageDialog(MainFrame.getInstance(), msg, Version.getAppName()+" - connect check", JOptionPane.WARNING_MESSAGE);
//				_logger.warn(msg);
//			}
//		}
//
//		//-------- installmaster ------
//		// is installmaster fully installed.
//		if (hasMonRole)
//		{
//			if (_installmasterStatus.equals("incomplete"))
//			{
//				String msg = "ASE 'installmaster' script has not been completely installed. Please check it's status with: sp_version";
//				if (_hasGui)
//					JOptionPane.showMessageDialog(MainFrame.getInstance(), msg, Version.getAppName()+" - connect check", JOptionPane.ERROR_MESSAGE);
//				_logger.error(msg);
//			}
//		}
//
//		//------------------------------------
//		// is 'installmaster' version different than ASE version
//		if (hasMonRole)
//		{
//			if (_installmasterVersionNum > 0)
//			{
//				if (_srvVersionNum != _installmasterVersionNum)
//				{
//					String msg = "ASE 'installmaster' script may be of a faulty version. ASE Version is '"+Ver.versionNumToStr(_srvVersionNum)+"' while 'installmaster' version is '"+Ver.versionNumToStr(_installmasterVersionNum)+"'. Please apply '$SYBASE/$SYBASE_ASE/scripts/installmaster' and check it's status with: sp_version.";
//					_logger.warn(msg);
//	
//					if (_hasGui)
//					{
//						String msgHtml = 
//							"<html>" +
//							"ASE 'installmaster' script may be of a faulty version. <br>" +
//							"<br>" +
//							"ASE Version is '"+Ver.versionNumToStr(_srvVersionNum)+"' while 'installmaster' version is '"+Ver.versionNumToStr(_installmasterVersionNum)+"'. <br>" +
//							"Please apply '$SYBASE/$SYBASE_ASE/scripts/installmaster' and check it's status with: sp_version. <br>" +
//							"<br>" +
//							"Do the following on the machine that hosts the ASE:<br>" +
//							"<code>isql -Usa -Psecret -SSRVNAME -w999 -i$SYBASE/$SYBASE_ASE/scripts/installmaster</code><br>" +
//							"<br>" +
//							"If this is <b>not</b> done, SQL Statements issued by "+Version.getAppName()+" may fail due to version inconsistency (wrong column names etc).<br>" +
//							"<br>" +
//							"Also the MDA tables(mon*) may deliver faulty or corrupt information, because the MDA proxy table definitions are not in sync with it's underlying data structures.<br>" +
//							"<br>" +
//							"<hr>" + // horizontal ruler
//							"<br>" +
//							"<center><b>Choose what Version you want to initialize the Performance Counters with</b></center><br>" +
//							"</html>";
//
////						SwingUtils.showErrorMessage(MainFrame.getInstance(), Version.getAppName()+" - connect check", msgHtml, null);
//						//JOptionPane.showMessageDialog(MainFrame.getInstance(), msgHtml, Version.getAppName()+" - connect check", JOptionPane.ERROR_MESSAGE);
//
//						Configuration config = Configuration.getInstance(Configuration.USER_TEMP);
//						if (config != null)
//						{
//							Object[] options = {
//									"ASE montables/installmaster Version " + Ver.versionNumToStr(_installmasterVersionNum),
//									"ASE binary Version "                  + Ver.versionNumToStr(_srvVersionNum)
//									};
//							int answer = JOptionPane.showOptionDialog(MainFrame.getInstance(), 
////								"ASE Binary and 'installmaster' is out of sync...\n" +
////									"What Version of ASE would you like to initialize the Performance Counters with?", // message
//								msgHtml,
//								"Initialize Performance Counters Using ASE Version", // title
//								JOptionPane.YES_NO_OPTION,
//								JOptionPane.WARNING_MESSAGE,
//								null,     //do not use a custom Icon
//								options,  //the titles of buttons
//								options[0]); //default button title
//
//							_trustMonTablesVersion = (answer == 0);
//
//							if (_trustMonTablesVersion)
//								_logger.warn("ASE Binary and 'montables/installmaster' is out of sync, installmaster has not been applied. The user decided to use the 'current installmaster version'. The used MDA table layout will be '"+Ver.versionNumToStr(_installmasterVersionNum)+"'. ASE Binary version was '"+Ver.versionNumToStr(_srvVersionNum)+"'.");
//							else
//								_logger.warn("ASE Binary and 'montables/installmaster' is out of sync, installmaster has not been applied. The user decided to use the 'ASE Binary version'. The used MDA table layout will be '"+Ver.versionNumToStr(_srvVersionNum)+"'. ASE installmaster version was '"+Ver.versionNumToStr(_installmasterVersionNum)+"'.");
//						}
//					}
//				}
//			}
//		}
//		
//		// finally MARK it as initialized
//		_initialized = true;
//	}

	/**
	 * Add a table that is NOT part of the MDA tables
	 * 
	 * @param tabName
	 * @param desc
	 */
	public void addTable(String tabName, String desc)
	{
		if (_monTables == null)
			throw new NullPointerException("Dictionary is not initialized or valid. (_monTables is null)");

		MonTableEntry entry = new MonTableEntry();

		entry._tableName    = StringUtil.truncate(tabName, MonTableEntry.TABLE_NAME_MAXLEN , true, "addTable(): MonTableEntry._tableName");
		entry._description  = StringUtil.truncate(desc   , MonTableEntry.DESCRIPTION_MAXLEN, true, "addTable(): MonTableEntry._description");;
		
		// Create substructure with the columns
		entry._monTableColumns = new HashMap<String,MonTableColumnsEntry>();

		_monTables.put(entry._tableName, entry);
	}

	/**
	 * Add a column to a table that is NOT part of the MDA tables 
	 * or add description to already existing description
	 * @param tabName
	 * @param colName
	 * @param desc
	 * @throws NameNotFoundException
	 */
	public void addColumn(String tabName, String colName, String desc)
	throws NameNotFoundException
	{
		if (_monTables == null)
			throw new NullPointerException("Dictionary is not initialized or valid. (_monTables is null)");

		MonTableEntry monTableEntry = _monTables.get(tabName);
		
		if (monTableEntry == null)
		{
			throw new NameNotFoundException("The table '"+tabName+"' was not found in the MonTables dictionary.");
		}

		if (monTableEntry._monTableColumns == null)
		{
			monTableEntry._monTableColumns = new HashMap<String,MonTableColumnsEntry>();
		}

		MonTableColumnsEntry entry = monTableEntry._monTableColumns.get(colName);
		if (entry == null)
		{
			entry = new MonTableColumnsEntry();

			entry._tableName    = StringUtil.truncate(tabName, MonTableColumnsEntry.TABLE_NAME_MAXLEN , true, "addColumn(): MonTableColumnsEntry._tableName, for table='" + tabName + "', column='" + colName + "'.");
			entry._columnName   = StringUtil.truncate(colName, MonTableColumnsEntry.COLUMN_NAME_MAXLEN, true, "addColumn(): MonTableColumnsEntry._columnName, for table='" + tabName + "', column='" + colName + "'.");
			entry._description  = StringUtil.truncate(desc   , MonTableColumnsEntry.DESCRIPTION_MAXLEN, true, "addColumn(): MonTableColumnsEntry._description, for table='" + tabName + "', column='" + colName + "'.");

			monTableEntry._monTableColumns.put(entry._columnName, entry);
		}
		else
		{
			String currentDesc = entry._description;

			// If Description has NOT changed, no need to continue!
			if (desc != null && desc.equals(currentDesc))
				return;

			if (! currentDesc.trim().endsWith("."))
				currentDesc = currentDesc.trim() + ".";

			entry._description  = StringUtil.truncate(currentDesc + " " + desc, MonTableColumnsEntry.DESCRIPTION_MAXLEN, true, "addColumn(exists): MonTableColumnsEntry._description");
		}

		
	}

	/**
	 * Set a column to a table that is NOT part of the MDA tables 
	 * or set/override current description to already existing description
	 * @param tabName
	 * @param colName
	 * @param desc
	 * @throws NameNotFoundException
	 */
	public void setColumn(String tabName, String colName, String desc)
	throws NameNotFoundException
	{
		if (_monTables == null)
			throw new NullPointerException("Dictionary is not initialized. (_monTables is null)");

		MonTableEntry monTableEntry = _monTables.get(tabName);
		
		if (monTableEntry == null)
		{
			throw new NameNotFoundException("The table '"+tabName+"' was not found in the MonTables dictionary.");
		}

		if (monTableEntry._monTableColumns == null)
		{
			monTableEntry._monTableColumns = new HashMap<String,MonTableColumnsEntry>();
		}

		MonTableColumnsEntry entry = new MonTableColumnsEntry();

		entry._tableName    = StringUtil.truncate(tabName, MonTableColumnsEntry.TABLE_NAME_MAXLEN , true, "setColumn(): MonTableColumnsEntry._tableName");
		entry._columnName   = StringUtil.truncate(colName, MonTableColumnsEntry.COLUMN_NAME_MAXLEN, true, "setColumn(): MonTableColumnsEntry._columnName");
		entry._description  = StringUtil.truncate(desc   , MonTableColumnsEntry.DESCRIPTION_MAXLEN, true, "setColumn(): MonTableColumnsEntry._description");
		
		monTableEntry._monTableColumns.put(entry._columnName, entry);
	}

	/**
	 * Get description for a table name
	 * 
	 * @param tabName
	 * @return null if nothing was found
	 */
	public String getDescriptionForTable(String tabName)
	{
		if (_monTables == null)
			return null;

		MonTableEntry mte = _monTables.get(tabName);
		if (mte != null)
			return mte._description;

		return null;
	}

	/**
	 * Get description for the column name, this one will return a 
	 * description on the first table where the column name matches
	 * 
	 * @param colName
	 * @return
	 */
	public String getDescription(String colName)
	{
		if (_monTables == null)
			return null;

//		Enumeration e = _monTables.keys();
//		while (e.hasMoreElements())
//		{
//			String monTable = (String) e.nextElement();
//
//			String desc = getDescription(monTable, colName);
//			if (desc != null)
//				return desc;
//		}
		for (String monTable : _monTables.keySet())
		{
			String desc = getDescription(monTable, colName);
			if (desc != null)
				return desc;
		}
		return null;
	}

	/**
	 * Get the column description for any of the tables in tabNameArr parameter.<br>
	 * If tabNameArr is null, getDescription(String colName) will be called.
	 * @param tabNameArr
	 * @param colName
	 * @return
	 */
	public String getDescription(String[] tabNameArr, String colName)
	{
		if (tabNameArr == null)
			return getDescription(colName);

		for (int i=0; i<tabNameArr.length; i++)
		{
			String desc = getDescription(tabNameArr[i], colName);
			if (desc != null)
				return desc;
		}
		return null;
	}

	/**
	 * Get the column description for the table
	 * 
	 * @param tabNameArr
	 * @param colName
	 * @return
	 */
	public String getDescription(String tabName, String colName)
	{
		if (_monTables == null)
			return null;

		MonTableEntry mte = _monTables.get(tabName);
		if (mte == null)
			return null;
		if (mte._monTableColumns == null)
			return null;

		MonTableColumnsEntry mtce = mte._monTableColumns.get(colName);
		if (mtce == null)
			return null;

		String indicator = "";
		if (mtce._indicators > 0)
		{
			indicator = ". Indicator=" + mtce._indicators + " (";

			if ( (mtce._indicators & 1) == 1 )
			{
				if (mtce._indicators > 1)
					indicator += "Cumulative counter, ";
				else
					indicator += "Cumulative counter";
			}
			if ( (mtce._indicators & 2) == 2 )
				indicator += "Shared with sp_sysmon";
			indicator += ")";
		}
		return mtce._description + indicator;
	}

	
	/**
	 * Add a description for specific spinlock name
	 * 
	 * @param spinlock name
	 * @param description
	 */
	public void addSpinlockDescription(String spinName, String description)
	{
		_sysMonitorsInfo.put(spinName, description);
	}

	/**
	 * Get description for specific spinlock name
	 * 
	 * @param spinlock name
	 * @return description
	 */
	public String getSpinlockDescription(String spinName)
	{
		String desc = _sysMonitorsInfo.get(spinName);
		return desc;
//		return (desc == null) ? "" : desc;
	}

	public String getSpinlockType(String spinName, List<String> aseCacheNames)
	{
		if (spinName == null)
			return null;

		String desc = null;

		if      (spinName.startsWith("Dbtable->" )) desc = "DBTABLE";
		else if (spinName.startsWith("Dbt->"     )) desc = "DBTABLE";
		else if (spinName.startsWith("Dbtable."  )) desc = "DBTABLE";
		else if (spinName.startsWith("Resource->")) desc = "RESOURCE";
		else if (spinName.startsWith("Kernel->"  )) desc = "KERNEL";
		else if (aseCacheNames != null && aseCacheNames.contains(spinName)) desc = "CACHE";
		else                                        desc = "OTHER";

		return desc;
	}

	public Map<String,String> getSpinlockMap()
	{
		return _sysMonitorsInfo;
	}

	/**
	 * Get description for specific waitEventId
	 * 
	 * @param waitEventId
	 * @return description
	 */
	public String getWaitEventDescription(int waitEventId)
	{
		if (_monWaitEventInfo == null)
			return null;

		String desc = null;
		try
		{
			if (_monWaitEventInfo[waitEventId] != null)
				desc = _monWaitEventInfo[waitEventId]._description;
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
		}

		if (desc == null)
		{
			desc = "-unknown-waitEventId-"+waitEventId;
		}

		return desc;
	}

	/**
	 * Is there a description for specific waitEventId
	 * 
	 * @param waitEventId
	 * @return description
	 */
	public boolean hasWaitEventDescription(int waitEventId)
	{
		if (_monWaitEventInfo == null)
			return false;

		try
		{
			if (_monWaitEventInfo[waitEventId] != null)
				if (_monWaitEventInfo[waitEventId]._description != null)
					return true;
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
		}

		return false;
	}

	/**
	 * Get the class description for specific waitEventId
	 * 
	 * @param waitEventId
	 * @return class description
	 */
	public String getWaitEventClassDescription(int waitEventId)
	{
		if (_monWaitEventInfo == null)
			return null;

		String desc = null;
		try
		{
			if (_monWaitEventInfo[waitEventId] != null)
			{
				int waitClassId = _monWaitEventInfo[waitEventId]._waitClassId;
				if (_monWaitClassInfo[waitClassId] != null)
					desc = _monWaitClassInfo[waitClassId]._description;

				if (desc == null)
					desc = "-unknown-waitClassId-"+waitClassId+"-for-known-waitEventId-"+waitEventId;
			}
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
		}

		if (desc == null)
			desc = "-unknown-waitEventId-"+waitEventId;

		return desc;
	}

	/**
	 * Get description for specific waitClassId
	 * 
	 * @param waitClassId
	 * @return description
	 */
	public String getWaitClassDescription(int waitClassId)
	{
		if (_monWaitClassInfo == null)
			return null;

		String desc = null;
		try
		{
			if (_monWaitClassInfo[waitClassId] != null)
				desc = _monWaitClassInfo[waitClassId]._description;
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
		}

		if (desc == null)
		{
			desc = "-unknown-waitClassId-"+waitClassId;
		}

		return desc;
	}

	/**
	 * Is there a description for specific waitEventId
	 * 
	 * @param waitEventId
	 * @return description
	 */
	public boolean hasWaitClassDescription(int waitClassId)
	{
		if (_monWaitClassInfo == null)
			return false;

		try
		{
			if (_monWaitClassInfo[waitClassId] != null)
				if (_monWaitClassInfo[waitClassId]._description != null)
					return true;
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
		}

		return false;
	}

	/**
	 * Get waitClassId for specific waitEventId
	 * 
	 * @param waitClassId
	 * @return description
	 */
	public int getWaitClassId(int waitEventId)
	{
		if (_monWaitEventInfo == null)
			return -1;

		int waitClassId = -1;
		try
		{
			if (_monWaitEventInfo[waitEventId] != null)
				waitClassId = _monWaitEventInfo[waitEventId]._waitClassId;
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
		}

		return waitClassId;
	}

//	/**
//	 * 
//	 * @param conn
//	 */
//	public void loadOfflineMonTablesDictionary(Connection conn)
//	{
//	}

}
