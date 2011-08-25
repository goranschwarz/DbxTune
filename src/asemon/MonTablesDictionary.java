/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import asemon.utils.AseConnectionUtils;

public class MonTablesDictionary
{
    /** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(MonTablesDictionary.class);

	/** Instance variable */
	private static MonTablesDictionary _instance = null;

	/** hashtable with MonTableEntry */
	private Hashtable _monTables = null;

	/** ASE @@version string */
	public String aseVersionStr = "";
	/** Calculate the @@version into a number. example: 12.5.4 -> 12540 */
	public int    aseVersionNum = 0;

	/** ASE sp_version 'installmontables' string */
	public String aseMonTablesInstallVersionStr = "";
	/** Calculate the sp_version 'installmontables' into a number. example: 12.5.4 -> 12540 */
	public int    aseMonTablesInstallVersionNum = 0;
	/** sp_version 'installmontables' Status string */
	public String aseMonTablesInstallStatus = "";

	private class MonTableEntry
	{
		int       _tableID         = 0;    // Unique identifier for the table
		int       _columns         = 0;    // Total number of columns in the table
		int       _parameters      = 0;    // Total number of optional parameters that can be specified
		int       _indicators      = 0;    // Indicators for specific table properties, e.g. if the table retains session context
		int       _size            = 0;    // Maximum row size (in bytes)
		String    _tableName       = null; // Name of the table
		String    _description     = null; // Description of the table

		/** hashtable with MonTableColumnsEntry */
		Hashtable _monTableColumns = null;
	}

	private class MonTableColumnsEntry
	{
		int    _tableID     = 0;    // Unique identifier for the table
		int    _columnID    = 0;    // Position of the column
		int    _typeID      = 0;    // Identifier for the data type of the column
		int    _precision   = 0;    // Precision of the column, if numeric
		int    _scale       = 0;    // Scale of the column, if numeric
		int    _length      = 0;    // Maximum length of the column (in bytes)
		int    _indicators  = 0;    // Indicators for specific column properties, e.g. if the column is prone to wrapping and should be sampled 
		String _tableName   = null; // Name of the table
		String _columnName  = null; // Name of the column
		String _typeName    = null; // Name of the data type of the column
		String _description = null; // Description of the column
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

	private Map                     _sysMonitorsInfo  = new HashMap();
	private MonWaitClassInfoEntry[] _monWaitClassInfo = null;
	private MonWaitEventInfoEntry[] _monWaitEventInfo = null;

	private static String SQL_TABLES                = "select TableID, Columns, Parameters, Indicators, Size, TableName, Description from master..monTables";
	private static String SQL_COLUMNS               = "select TableID, ColumnID, TypeID, Precision, Scale, Length, Indicators, TableName, ColumnName, TypeName, Description from master..monTableColumns where TableName = ";
	private static String SQL_MON_WAIT_CLASS_INFO_1 = "select max(WaitClassID) from monWaitClassInfo";
	private static String SQL_MON_WAIT_CLASS_INFO   = "select WaitClassID, Description from monWaitClassInfo";
	private static String SQL_MON_WAIT_EVENT_INFO_1 = "select max(WaitEventID) from monWaitEventInfo";
	private static String SQL_MON_WAIT_EVENT_INFO   = "select WaitEventID, WaitClassID, Description from monWaitEventInfo";
	private static String SQL_VERSION               = "select @@version";
	private static String SQL_VERSION_NUM           = "select @@version_number";
	private static String SQL_SP_VERSION            = "sp_version 'installmontables'";

	
	public static MonTablesDictionary getInstance()
	{
		if (_instance == null)
			_instance = new MonTablesDictionary();
		return _instance;
	}
	
	
	public boolean isInitialized()
	{
		return (_monTables != null ? true : false);
	}

	public void initialize(Connection conn)
	{
		if (conn == null)
			return;

		_monTables = new Hashtable();

		try
		{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(SQL_TABLES);
			while ( rs.next() )
			{
				MonTableEntry entry = new MonTableEntry();
				int pos = 1;

				entry._tableID      = rs.getInt(pos++);
				entry._columns      = rs.getInt(pos++);
				entry._parameters   = rs.getInt(pos++);
				entry._indicators   = rs.getInt(pos++);
				entry._size         = rs.getInt(pos++);
				entry._tableName    = rs.getString(pos++);
				entry._description  = rs.getString(pos++);
				
				// Create substructure with the columns
				// This is filled in BELOW (next SQL query)
				entry._monTableColumns = new Hashtable();

				_monTables.put(entry._tableName, entry);
			}
			rs.close();
		}
		catch (SQLException ex)
		{
			_logger.error("MonTablesDictionary:initialize", ex);
			_monTables = null;
			return;
		}

		Enumeration e = _monTables.elements();
		while (e.hasMoreElements())
		{
			MonTableEntry monTableEntry = (MonTableEntry) e.nextElement();
			
			if (monTableEntry._monTableColumns == null)
			{
				monTableEntry._monTableColumns = new Hashtable();
			}
			else
			{
				monTableEntry._monTableColumns.clear();
			}

			try
			{
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(SQL_COLUMNS + "'" + monTableEntry._tableName + "'");
				while ( rs.next() )
				{
					MonTableColumnsEntry entry = new MonTableColumnsEntry();
					int pos = 1;
	
					entry._tableID      = rs.getInt(pos++);
					entry._columnID     = rs.getInt(pos++);
					entry._typeID       = rs.getInt(pos++);
					entry._precision    = rs.getInt(pos++);
					entry._scale        = rs.getInt(pos++);
					entry._length       = rs.getInt(pos++);
					entry._indicators   = rs.getInt(pos++);
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
				_logger.error("MonTablesDictionary:initialize", ex);
				_monTables = null;
				return;
			}
		}

		// monWaitClassInfo
		try
		{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(SQL_MON_WAIT_CLASS_INFO_1);
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
			_monWaitClassInfo = null;
			return;
		}

		// _monWaitEventInfo
		try
		{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(SQL_MON_WAIT_EVENT_INFO_1);
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
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(SQL_VERSION);
			while ( rs.next() )
			{
				aseVersionStr = rs.getString(1);
			}
			rs.close();

			int aseVersionNumFromVerStr = AseConnectionUtils.aseVersionStringToNumber(aseVersionStr);
			aseVersionNum = Math.max(aseVersionNum, aseVersionNumFromVerStr);
		}
		catch (SQLException ex)
		{
			_logger.error("MonTablesDictionary:initialize, @@version", ex);
			return;
		}

		// sp_version
		try
		{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(SQL_SP_VERSION);
			while ( rs.next() )
			{
				//String scriptName      = rs.getString(1);
				aseMonTablesInstallVersionStr = rs.getString(2);
				aseMonTablesInstallStatus     = rs.getString(3);
				
				aseMonTablesInstallVersionNum = AseConnectionUtils.aseVersionStringToNumber(aseMonTablesInstallVersionStr);

				if ( ! aseMonTablesInstallStatus.equalsIgnoreCase("Complete") )
				{
					aseMonTablesInstallStatus = "incomplete";
				}
			}
			rs.close();
		}
		catch (SQLException ex)
		{
			_logger.warn("MonTablesDictionary:initialize, problems executing: "+SQL_SP_VERSION+ ". Exception: "+ex.getMessage());
			return;
		}

		// is installed monitor tables fully installed.
		if (aseMonTablesInstallStatus.equals("incomplete"))
		{
			_logger.warn("ASE Monitoring tables has not been completely installed. Please check it's status with: sp_version 'installmontables'");
		}

		// is installed monitor tables version different than ASE version
		if (aseMonTablesInstallVersionNum > 0)
		{
			if (aseVersionNum != aseMonTablesInstallVersionNum)
			{
				_logger.warn("ASE Monitoring tables may be of a faulty version. ASE Version is '"+aseVersionNum+"' while MonTables version is '"+aseMonTablesInstallVersionNum+"'. Please check it's status with: sp_version 'installmontables'");
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
		entry._monTableColumns = new Hashtable();

		_monTables.put(entry._tableName, entry);
	}

	/**
	 * Add a column to a table that is NOT part of the MDA tables
	 * @param tabName
	 * @param colName
	 * @param desc
	 * @throws NameNotFoundException
	 */
	public void addColumn(String tabName, String colName, String desc)
	throws NameNotFoundException
	{
		MonTableEntry monTableEntry = (MonTableEntry) _monTables.get(tabName);
		
		if (monTableEntry == null)
		{
			throw new NameNotFoundException("The table '"+tabName+"' was not found in the MonTables dictionary.");
		}

		if (monTableEntry._monTableColumns == null)
		{
			monTableEntry._monTableColumns = new Hashtable();
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

		Enumeration e = _monTables.keys();
		while (e.hasMoreElements())
		{
			String monTable = (String) e.nextElement();

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

		MonTableEntry mte = (MonTableEntry) _monTables.get(tabName);
		if (mte == null)
			return null;
		if (mte._monTableColumns == null)
			return null;

		MonTableColumnsEntry mtce = (MonTableColumnsEntry) mte._monTableColumns.get(colName);
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
		String desc = (String) _sysMonitorsInfo.get(spinName);
		return (desc == null) ? "" : desc;
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
}
