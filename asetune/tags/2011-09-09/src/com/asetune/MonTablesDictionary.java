/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.naming.NameNotFoundException;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import com.asetune.gui.MainFrame;
import com.asetune.pcs.PersistWriterBase;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.SwingUtils;


public class MonTablesDictionary
{
    /** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(MonTablesDictionary.class);

	/** Instance variable */
	private static MonTablesDictionary _instance = null;

	/** Was initialized using a GUI */
	private boolean _hasGui = false;

	/** hashtable with MonTableEntry */
	private HashMap<String,MonTableEntry> _monTables = null;

	/** ASE @@version string */
	public String aseVersionStr = "";
	/** Calculate the @@version into a number. example: 12.5.4 -> 12540 */
	public int    aseVersionNum = 0;
	/** If the version string contained the string 'Cluster Edition', this member will be true. */
	public boolean isClusterEnabled = false;

	/** ASE sp_version 'installmontables' string */
	public String montablesVersionStr = "";
	/** Calculate the sp_version 'installmontables' into a number. example: 12.5.4 -> 12540 */
	public int    montablesVersionNum = 0;
	/** sp_version 'installmontables' Status string */
	public String montablesStatus = "";

	/** ASE sp_version 'installmaster' string */
	public String installmasterVersionStr = "";
	/** Calculate the sp_version 'installmontables' into a number. example: 12.5.4 -> 12540 */
	public int    installmasterVersionNum = 0;
	/** sp_version 'installmontables' Status string */
	public String installmasterStatus = "";

	public class MonTableEntry
	{
		public int       _tableID         = 0;    // Unique identifier for the table
		public int       _columns         = 0;    // Total number of columns in the table
		public int       _parameters      = 0;    // Total number of optional parameters that can be specified
		public int       _indicators      = 0;    // Indicators for specific table properties, e.g. if the table retains session context
		public int       _size            = 0;    // Maximum row size (in bytes)
		public String    _tableName       = null; // Name of the table
		public String    _description     = null; // Description of the table

		/** hashtable with MonTableColumnsEntry */
		public HashMap<String,MonTableColumnsEntry> _monTableColumns = null;
	}

	public class MonTableColumnsEntry
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
	}

	private class MonWaitClassInfoEntry
	{
		int		_waitClassId    = 0;    // select * from monWaitClassInfo  WaitClassID Description
		String  _description    = null;
		
		public String toString()
		{
			return "MonWaitClassInfoEntry _waitClassId="+_waitClassId+", _description='"+_description+"'.";
		}
	}
	private class MonWaitEventInfoEntry
	{
		int		_waitEventId    = 0;    // select * from monWaitEventInfo  WaitEventID WaitClassID Description
		int		_waitClassId    = 0;
		String  _description    = null;

		public String toString()
		{
			return "MonWaitEventInfoEntry _waitEventId="+_waitEventId+", _waitClassId="+_waitClassId+", _description='"+_description+"'.";
		}
	}

	private Map<String,String>      _sysMonitorsInfo  = new HashMap<String,String>();
	private MonWaitClassInfoEntry[] _monWaitClassInfo = null;
	private MonWaitEventInfoEntry[] _monWaitEventInfo = null;

//	/** Character used for quoted identifier */
//	public static String  qic = "\"";

	private static String FROM_TAB_NAME             = "?FROM_TAB_NAME?";
	private static String TAB_NAME                  = "?TAB_NAME?";
//	private static String SQL_TABLES                = "select TableID, Columns, Parameters, Indicators, Size, TableName, Description from master..monTables";
//	private static String SQL_COLUMNS               = "select TableID, ColumnID, TypeID, Precision, Scale, Length, Indicators, TableName, ColumnName, TypeName, Description from master..monTableColumns where TableName = '?TAB_NAME?'";
	private static String SQL_TABLES                = "select \"TableID\", \"Columns\", \"Parameters\", \"Indicators\", \"Size\", \"TableName\", \"Description\" from "+FROM_TAB_NAME;
	private static String SQL_COLUMNS               = "select \"TableID\", \"ColumnID\", \"TypeID\", \"Precision\", \"Scale\", \"Length\", \"Indicators\", \"TableName\", \"ColumnName\", \"TypeName\", \"Description\" from "+FROM_TAB_NAME+" where \"TableName\" = '"+TAB_NAME+"'";
//	private static String SQL_TABLES                = "select TableID, Columns, Parameters, Indicators, Size, TableName, Description from "+FROM_TAB_NAME;
//	private static String SQL_COLUMNS               = "select TableID, ColumnID, TypeID, Precision, Scale, Length, Indicators, TableName, ColumnName, TypeName, Description from "+FROM_TAB_NAME+" where TableName = '"+TAB_NAME+"'";
//	private static String SQL_TABLES                = "select * from "+FROM_TAB_NAME;
//	private static String SQL_COLUMNS               = "select * from "+FROM_TAB_NAME+" where TableName = '"+TAB_NAME+"'";
	private static String SQL_MON_WAIT_CLASS_INFO_1 = "select max(WaitClassID) from monWaitClassInfo";
	private static String SQL_MON_WAIT_CLASS_INFO   = "select WaitClassID, Description from monWaitClassInfo";
	private static String SQL_MON_WAIT_EVENT_INFO_1 = "select max(WaitEventID) from monWaitEventInfo";
	private static String SQL_MON_WAIT_EVENT_INFO   = "select WaitEventID, WaitClassID, Description from monWaitEventInfo";
	private static String SQL_VERSION               = "select @@version";
	private static String SQL_VERSION_NUM           = "select @@version_number";
//	private static String SQL_SP_VERSION            = "sp_version 'installmontables'";
	private static String SQL_SP_VERSION            = "sp_version";

	
	public static boolean hasInstance()
	{
		return (_instance != null);
	}

	public static MonTablesDictionary getInstance()
	{
		if (_instance == null)
			_instance = new MonTablesDictionary();
		return _instance;
	}

	/**
	 * Reset the dictionary, this so we can get new ones later<br>
	 * Most possible called from disconnect() or similar
	 */
	public static void reset()
	{
		_instance = null;
	}


	public Map<String,MonTableEntry> getMonTablesDictionaryMap()
	{
		return _monTables;
	}
	
	public boolean isInitialized()
	{
		return (_monTables != null ? true : false);
	}

	/**
	 * Initialize 
	 * @param conn
	 */
	public void initializeMonTabColHelper(Connection conn, boolean offline)
	{
		if (conn == null)
			return;

		_monTables = new HashMap<String,MonTableEntry>();

		String monTables       = "monTables";
		String monTableColumns = "monTableColumns";
		if (offline)
		{
			monTables       = PersistWriterBase.getTableName(PersistWriterBase.SESSION_MON_TAB_DICT,     null, true);
			monTableColumns = PersistWriterBase.getTableName(PersistWriterBase.SESSION_MON_TAB_COL_DICT, null, true);
		}
		
		String sql = null;
		try
		{
			Statement stmt = conn.createStatement();
			sql = SQL_TABLES.replace(FROM_TAB_NAME, monTables);
			if ( ! offline )
				sql = sql.replace("\"", "");

			ResultSet rs = stmt.executeQuery(sql);
			while ( rs.next() )
			{
				MonTableEntry entry = new MonTableEntry();

				int pos = 1;
				entry._tableID      = rs.getInt   (pos++);
				entry._columns      = rs.getInt   (pos++);
				entry._parameters   = rs.getInt   (pos++);
				entry._indicators   = rs.getInt   (pos++);
				entry._size         = rs.getInt   (pos++);
				entry._tableName    = rs.getString(pos++);
				entry._description  = rs.getString(pos++);
				
				// Create substructure with the columns
				// This is filled in BELOW (next SQL query)
				entry._monTableColumns = new HashMap<String,MonTableColumnsEntry>();

				_monTables.put(entry._tableName, entry);
			}
			rs.close();
		}
		catch (SQLException ex)
		{
			if (offline && ex.getMessage().contains("not found"))
			{
				_logger.warn("Tooltip on column headers wasn't available in the offline database. This simply means that tooltip wont be showed in various places.");
				return;
			}
			_logger.error("MonTablesDictionary:initialize:sql='"+sql+"'", ex);
			_monTables = null;
			return;
		}

		for (Map.Entry<String,MonTableEntry> mapEntry : _monTables.entrySet()) 
		{
		//	String        key           = mapEntry.getKey();
			MonTableEntry monTableEntry = mapEntry.getValue();
			
			if (monTableEntry._monTableColumns == null)
			{
				monTableEntry._monTableColumns = new HashMap<String,MonTableColumnsEntry>();
			}
			else
			{
				monTableEntry._monTableColumns.clear();
			}

			try
			{
				Statement stmt = conn.createStatement();
				sql = SQL_COLUMNS.replace(FROM_TAB_NAME, monTableColumns);
				sql = sql.replace(TAB_NAME, monTableEntry._tableName);
				if ( ! offline )
					sql = sql.replace("\"", "");

				ResultSet rs = stmt.executeQuery(sql);
				while ( rs.next() )
				{
					MonTableColumnsEntry entry = new MonTableColumnsEntry();

					int pos = 1;
					entry._tableID      = rs.getInt   (pos++);
					entry._columnID     = rs.getInt   (pos++);
					entry._typeID       = rs.getInt   (pos++);
					entry._precision    = rs.getInt   (pos++);
					entry._scale        = rs.getInt   (pos++);
					entry._length       = rs.getInt   (pos++);
					entry._indicators   = rs.getInt   (pos++);
					entry._tableName    = rs.getString(pos++);
					entry._columnName   = rs.getString(pos++);
					entry._typeName     = rs.getString(pos++);
					entry._description  = rs.getString(pos++);
					
					monTableEntry._monTableColumns.put(entry._columnName, entry);
				}
				rs.close();
			}
			catch (SQLException ex)
			{
				if (offline && ex.getMessage().contains("not found"))
				{
					_logger.warn("Tooltip on column headers wasn't available in the offline database. This simply means that tooltip wont be showed in various places.");
					return;
				}
				_logger.error("MonTablesDictionary:initialize:sql='"+sql+"'", ex);
				_monTables = null;
				return;
			}
		}
	}

	public void initialize(Connection conn, boolean hasGui)
	{
		if (conn == null)
			return;
		_hasGui = hasGui;

		String sql = null;

		// get values from monTables & monTableColumns
		initializeMonTabColHelper(conn, false);

		// monWaitClassInfo
		try
		{
			sql = SQL_MON_WAIT_CLASS_INFO_1;
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			int max_waitClassId = 0; 
			while ( rs.next() )
			{
				max_waitClassId = rs.getInt(1); 
			}
			rs.close();

			_monWaitClassInfo = new MonWaitClassInfoEntry[max_waitClassId+1];

			rs = stmt.executeQuery(SQL_MON_WAIT_CLASS_INFO);
			while ( rs.next() )
			{
				MonWaitClassInfoEntry entry = new MonWaitClassInfoEntry();
				int pos = 1;

				entry._waitClassId  = rs.getInt(pos++);
				entry._description  = rs.getString(pos++);

				_logger.debug("Adding WaitClassInfo: " + entry);

				_monWaitClassInfo[entry._waitClassId] = entry;
			}
			rs.close();
		}
		catch (SQLException ex)
		{
			_logger.error("MonTablesDictionary:initialize, _monWaitClassInfo", ex);
			if (_hasGui)
				SwingUtils.showErrorMessage("MonTablesDictionary - Initialize", "SQL Exception: "+ex.getMessage()+"\n\nThis was found when executing SQL statement:\n\n"+sql, ex);
			_monWaitClassInfo = null;
			return;
		}

		// _monWaitEventInfo
		try
		{
			sql = SQL_MON_WAIT_EVENT_INFO_1;
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			int max_waitEventId = 0; 
			while ( rs.next() )
			{
				max_waitEventId = rs.getInt(1); 
			}
			rs.close();

			_monWaitEventInfo = new MonWaitEventInfoEntry[max_waitEventId+1];

			rs = stmt.executeQuery(SQL_MON_WAIT_EVENT_INFO);
			while ( rs.next() )
			{
				MonWaitEventInfoEntry entry = new MonWaitEventInfoEntry();
				int pos = 1;

				entry._waitEventId  = rs.getInt(pos++);
				entry._waitClassId  = rs.getInt(pos++);
				entry._description  = rs.getString(pos++);
				
				_logger.debug("Adding WaitEventInfo: " + entry);

				_monWaitEventInfo[entry._waitEventId] = entry;
			}
			rs.close();
		}
		catch (SQLException ex)
		{
			_logger.error("MonTablesDictionary:initialize, _monWaitEventInfo", ex);
			if (_hasGui)
				SwingUtils.showErrorMessage("MonTablesDictionary - Initialize", "SQL Exception: "+ex.getMessage()+"\n\nThis was found when executing SQL statement:\n\n"+sql, ex);
			_monWaitEventInfo = null;
			return;
		}

		// @@version_number
		try
		{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(SQL_VERSION_NUM);
			while ( rs.next() )
			{
				aseVersionNum = rs.getInt(1);
			}
			rs.close();
		}
		catch (SQLException ex)
		{
			_logger.debug("MonTablesDictionary:initialize, @@version_number, probably an early ASE version", ex);
		}

		// version
		try
		{
			sql = SQL_VERSION;
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while ( rs.next() )
			{
				aseVersionStr = rs.getString(1);
			}
			rs.close();

			int aseVersionNumFromVerStr = AseConnectionUtils.aseVersionStringToNumber(aseVersionStr);
			aseVersionNum = Math.max(aseVersionNum, aseVersionNumFromVerStr);

			// Check if the ASE binary is Cluster Edition Enabled
//			if (aseVersionStr.indexOf("Cluster Edition") >= 0)
//				isClusterEdition = true;
			if (AseConnectionUtils.isClusterEnabled(conn))
				isClusterEnabled = true;
		}
		catch (SQLException ex)
		{
			_logger.error("MonTablesDictionary:initialize, @@version", ex);
			if (_hasGui)
				SwingUtils.showErrorMessage("MonTablesDictionary - Initialize", "SQL Exception: "+ex.getMessage()+"\n\nThis was found when executing SQL statement:\n\n"+sql, ex);
			return;
		}

		// sp_version
		if (aseVersionNum >= 12530)
		{
			try
			{
				sql = SQL_SP_VERSION;
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(sql);
				while ( rs.next() )
				{
					String spVersion_scriptName = rs.getString(1);
					String spVersion_versionStr = rs.getString(2);
					String spVersion_status     = rs.getString(3);
	
					if (spVersion_scriptName.endsWith("montables")) // could be: installmontables or montables
					{
						montablesVersionStr = spVersion_versionStr;
						montablesStatus     = spVersion_status;
	
						montablesVersionNum = AseConnectionUtils.aseVersionStringToNumber(montablesVersionStr);
		
						if ( ! montablesStatus.equalsIgnoreCase("Complete") )
						{
							montablesStatus = "incomplete";
						}
					}
	
					if (spVersion_scriptName.equals("installmaster"))
					{
						installmasterVersionStr = spVersion_versionStr;
						installmasterStatus     = spVersion_status;
	
						installmasterVersionNum = AseConnectionUtils.aseVersionStringToNumber(installmasterVersionStr);
		
						if ( ! installmasterStatus.equalsIgnoreCase("Complete") )
						{
							installmasterStatus = "incomplete";
						}
					}
				}
				rs.close();
			}
			catch (SQLException ex)
			{
				_logger.warn("MonTablesDictionary:initialize, problems executing: "+SQL_SP_VERSION+ ". Exception: "+ex.getMessage());
				if (_hasGui)
					SwingUtils.showErrorMessage("MonTablesDictionary - Initialize", "SQL Exception: "+ex.getMessage()+"\n\nThis was found when executing SQL statement:\n\n"+sql, ex);
				return;
			}
		}

		_logger.info("ASE 'montables'     for sp_version shows: Status='"+montablesStatus    +"', VersionNum='"+montablesVersionNum    +"', VersionStr='"+montablesVersionStr+"'.");
		_logger.info("ASE 'installmaster' for sp_version shows: Status='"+installmasterStatus+"', VersionNum='"+installmasterVersionNum+"', VersionStr='"+installmasterVersionStr+"'.");

		//-------- montables ------
		// is installed monitor tables fully installed.
		if (montablesStatus.equals("incomplete"))
		{
			String msg = "ASE Monitoring tables has not been completely installed. Please check it's status with: sp_version";
			if (AseTune.hasGUI())
				JOptionPane.showMessageDialog(MainFrame.getInstance(), msg, Version.getAppName()+" - connect check", JOptionPane.WARNING_MESSAGE);
			_logger.warn(msg);
		}

		// is installed monitor tables version different than ASE version
		if (montablesVersionNum > 0)
		{
			// strip off the ROLLUP VERSION  (divide by 10 takes away last digit)
			if (aseVersionNum/10 != montablesVersionNum/10)
			{
				String msg = "ASE Monitoring tables may be of a faulty version. ASE Version is '"+aseVersionNum+"' while MonTables version is '"+montablesVersionNum+"'. Please check it's status with: sp_version";
				if (_hasGui)
					JOptionPane.showMessageDialog(MainFrame.getInstance(), msg, Version.getAppName()+" - connect check", JOptionPane.WARNING_MESSAGE);
				_logger.warn(msg);
			}
		}

		//-------- installmaster ------
		// is installmaster fully installed.
		if (installmasterStatus.equals("incomplete"))
		{
			String msg = "ASE 'installmaster' script has not been completely installed. Please check it's status with: sp_version";			
			if (_hasGui)
				JOptionPane.showMessageDialog(MainFrame.getInstance(), msg, Version.getAppName()+" - connect check", JOptionPane.ERROR_MESSAGE);
			_logger.error(msg);
		}

		// is 'installmaster' version different than ASE version
		if (installmasterVersionNum > 0)
		{
			if (aseVersionNum != installmasterVersionNum)
			{
				String msg = "ASE 'installmaster' script may be of a faulty version. ASE Version is '"+aseVersionNum+"' while 'installmaster' version is '"+installmasterVersionNum+"'. Please apply '$SYBASE/$SYBASE_ASE/scripts/installmaster' and check it's status with: sp_version.";
				if (_hasGui)
					JOptionPane.showMessageDialog(MainFrame.getInstance(), msg, Version.getAppName()+" - connect check", JOptionPane.ERROR_MESSAGE);
				_logger.error(msg);
			}
		}
	}

	/**
	 * Add a table that is NOT part of the MDA tables
	 * 
	 * @param tabName
	 * @param desc
	 */
	public void addTable(String tabName, String desc)
	{
		MonTableEntry entry = new MonTableEntry();

		entry._tableName    = tabName;
		entry._description  = desc;
		
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

			entry._tableName    = tabName;
			entry._columnName   = colName;
			entry._description  = desc;

			monTableEntry._monTableColumns.put(entry._columnName, entry);
		}
		else
		{
			String currentDesc = entry._description;
			if (! currentDesc.trim().endsWith("."))
				currentDesc = currentDesc.trim() + ".";

			entry._description  = currentDesc + " " + desc;
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

		entry._tableName    = tabName;
		entry._columnName   = colName;
		entry._description  = desc;
		
		monTableEntry._monTableColumns.put(entry._columnName, entry);
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

	/**
	 * 
	 * @param conn
	 */
	public void loadOfflineMonTablesDictionary(Connection conn)
	{
	}

}
