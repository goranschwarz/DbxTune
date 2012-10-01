/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import com.asetune.gui.MainFrame;
import com.asetune.pcs.PersistWriterBase;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
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
//	private HashMap<String,MonTableEntry> _monTables = null;
	private HashMap<String,MonTableEntry> _monTables = new HashMap<String,MonTableEntry>();

	/** has been initalized or not */
	private boolean _initialized = false;

	/** ASE @@servername string */
	public String aseServerName = "";

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

	/** ASE Sort Order ID */
	public int     aseSortId   = -1;
	/** ASE Sort Order Name */
	public String  aseSortName = "";

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
		return _initialized;
//		return (_monTables != null ? true : false);
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
			{
				if (aseVersionNum >= 15700)
					sql += " where Language = 'en_US' ";

				sql = sql.replace("\"", "");
			}

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
				{
					if (aseVersionNum >= 15700)
						sql += " and Language = 'en_US' ";

					sql = sql.replace("\"", "");
				}

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

	
	public void initializeVersionInfo(Connection conn, boolean hasGui)
	{
		if (conn == null)
			return;
		String sql = null;

		// @@servername
		aseServerName = AseConnectionUtils.getAseServername(conn);

		// aseVersionStr = @@version
		// aseVersionNum = @@version -> to an integer
		// isClusterEnabled...
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
			stmt.close();

			aseVersionNum = AseConnectionUtils.aseVersionStringToNumber(aseVersionStr);

			if (AseConnectionUtils.isClusterEnabled(conn))
				isClusterEnabled = true;
		}
		catch (SQLException ex)
		{
			_logger.error("initializeVersionInfo, @@version", ex);
			if (_hasGui)
				SwingUtils.showErrorMessage("MonTablesDictionary - initializeVersionInfo", "SQL Exception: "+ex.getMessage()+"\n\nThis was found when executing SQL statement:\n\n"+sql, ex);
			return;
		}

		// SORT order ID and NAME
		try
		{
			sql="declare @sortid tinyint, @charid tinyint \n" +
				"select @sortid = value from master..syscurconfigs where config = 123 \n" +
				"select @charid = value from master..syscurconfigs where config = 131  \n" +
				"\n" +
				"select id, name \n" +
				"from master.dbo.syscharsets \n" + 
				"where id = @sortid and csid = @charid \n";
			
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while ( rs.next() )
			{
				aseSortId   = rs.getInt   (1);
				aseSortName = rs.getString(2);
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException ex)
		{
			_logger.error("initializeVersionInfo, Sort order information", ex);
		}
	}

	public void initialize(Connection conn, boolean hasGui)
	{
		if (conn == null)
			return;
		_hasGui = hasGui;

		// Do more things if user has MON_ROLE
		boolean hasMonRole = AseConnectionUtils.hasRole(conn, AseConnectionUtils.MON_ROLE);

		String sql = null;

		// get values from monTables & monTableColumns
		if (hasMonRole)
			initializeMonTabColHelper(conn, false);

		// monWaitClassInfo
		if (hasMonRole)
		{
			try
			{
				sql = SQL_MON_WAIT_CLASS_INFO_1;
				if (aseVersionNum >= 15700)
					sql += " where Language = 'en_US'";
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(sql);
				int max_waitClassId = 0; 
				while ( rs.next() )
				{
					max_waitClassId = rs.getInt(1); 
				}
				rs.close();
	
				_monWaitClassInfo = new MonWaitClassInfoEntry[max_waitClassId+1];

				sql = SQL_MON_WAIT_CLASS_INFO;
				if (aseVersionNum >= 15700)
					sql += " where Language = 'en_US'";
				rs = stmt.executeQuery(sql);
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
				if (aseVersionNum >= 15700)
					sql += " where Language = 'en_US'";
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(sql);
				int max_waitEventId = 0; 
				while ( rs.next() )
				{
					max_waitEventId = rs.getInt(1); 
				}
				rs.close();
	
				_monWaitEventInfo = new MonWaitEventInfoEntry[max_waitEventId+1];
	
				sql = SQL_MON_WAIT_EVENT_INFO;
				if (aseVersionNum >= 15700)
					sql += " where Language = 'en_US'";
				rs = stmt.executeQuery(sql);
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
		}

		// @@servername
		aseServerName = AseConnectionUtils.getAseServername(conn);
		
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

		// SORT order ID and NAME
		try
		{
			sql="declare @sortid tinyint, @charid tinyint \n" +
				"select @sortid = value from master..syscurconfigs where config = 123 \n" +
				"select @charid = value from master..syscurconfigs where config = 131  \n" +
				"\n" +
				"select id, name \n" +
				"from master.dbo.syscharsets \n" + 
				"where id = @sortid and csid = @charid \n";
			
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while ( rs.next() )
			{
				aseSortId   = rs.getInt   (1);
				aseSortName = rs.getString(2);
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException ex)
		{
			_logger.error("initializeVersionInfo, Sort order information", ex);
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
				// Msg 2812, Level 16, State 5:
				// Server 'GORAN_12503_DS', Line 1:
				// Stored procedure 'sp_version' not found. Specify owner.objectname or use sp_help to check whether the object exists (sp_help may produce lots of output).
				if (ex.getErrorCode() == 2818)
				{
					String msg = "ASE 'installmaster' script may be of a faulty version. ASE Version is '"+aseVersionNum+"'. " +
							"The stored procedure 'sp_version' was introduced in ASE 12.5.3, which I can't find in the connected ASE, this implies that 'installmaster' has not been applied after upgrade. " +
							"Please apply '$SYBASE/$SYBASE_ASE/scripts/installmaster' and check it's status with: sp_version.";
					_logger.error(msg);
	
					String msgHtml = 
						"<html>" +
						"ASE 'installmaster' script may be of a faulty version. <br>" +
						"<br>" +
						"ASE Version is '"+AseConnectionUtils.versionIntToStr(aseVersionNum)+"'.<br>" +
						"The stored procedure 'sp_version' was introduced in ASE 12.5.3, which I can't find in the connected ASE, <br>" +
						"this implies that 'installmaster' has not been applied after upgrade.<br>" +
						"Please apply '$SYBASE/$SYBASE_ASE/scripts/installmaster' and check it's status with: sp_version. <br>" +
						"<br>" +
						"Do the following on the machine that hosts the ASE:<br>" +
						"<font size=\"4\">" +
						"  <code>isql -Usa -Psecret -SSRVNAME -w999 -i$SYBASE/$SYBASE_ASE/scripts/installmaster</code><br>" +
						"</font>" +
						"<br>" +
						"If this is <b>not</b> done, SQL Statements issued by "+Version.getAppName()+" may fail due to version inconsistency (wrong column names etc).<br>" +
						"<br>" +
						"Also the MDA tables(mon*) may deliver faulty or corrupt information, because the MDA proxy table definitions are not in sync with it's underlying data structures.<br>" +
						"</html>";
					if (_hasGui)
						SwingUtils.showErrorMessage(MainFrame.getInstance(), Version.getAppName()+" - connect check", msgHtml, null);
				}
				else
				{
					_logger.warn("MonTablesDictionary:initialize, problems executing: "+SQL_SP_VERSION+ ". Exception: "+ex.getMessage());
					if (_hasGui)
						SwingUtils.showErrorMessage("MonTablesDictionary - Initialize", "SQL Exception: "+ex.getMessage()+"\n\nThis was found when executing SQL statement:\n\n"+sql, ex);
					return;
				}
			}
		} // end: if (aseVersionNum >= 12530)

		_logger.info("ASE 'montables'     for sp_version shows: Status='"+montablesStatus    +"', VersionNum='"+montablesVersionNum    +"', VersionStr='"+montablesVersionStr+"'.");
		_logger.info("ASE 'installmaster' for sp_version shows: Status='"+installmasterStatus+"', VersionNum='"+installmasterVersionNum+"', VersionStr='"+installmasterVersionStr+"'.");

		//-------- montables ------
		// is installed monitor tables fully installed.
		if (hasMonRole)
		{
			if (montablesStatus.equals("incomplete"))
			{
				String msg = "ASE Monitoring tables has not been completely installed. Please check it's status with: sp_version";
				if (AseTune.hasGUI())
					JOptionPane.showMessageDialog(MainFrame.getInstance(), msg, Version.getAppName()+" - connect check", JOptionPane.WARNING_MESSAGE);
				_logger.warn(msg);
			}
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
		if (hasMonRole)
		{
			if (installmasterStatus.equals("incomplete"))
			{
				String msg = "ASE 'installmaster' script has not been completely installed. Please check it's status with: sp_version";
				if (_hasGui)
					JOptionPane.showMessageDialog(MainFrame.getInstance(), msg, Version.getAppName()+" - connect check", JOptionPane.ERROR_MESSAGE);
				_logger.error(msg);
			}
		}

		// is 'installmaster' version different than ASE version
		if (hasMonRole)
		{
			if (installmasterVersionNum > 0)
			{
				if (aseVersionNum != installmasterVersionNum)
				{
					String msg = "ASE 'installmaster' script may be of a faulty version. ASE Version is '"+aseVersionNum+"' while 'installmaster' version is '"+installmasterVersionNum+"'. Please apply '$SYBASE/$SYBASE_ASE/scripts/installmaster' and check it's status with: sp_version.";
					_logger.warn(msg);
	
					if (_hasGui)
					{
						String msgHtml = 
							"<html>" +
							"ASE 'installmaster' script may be of a faulty version. <br>" +
							"<br>" +
							"ASE Version is '"+AseConnectionUtils.versionIntToStr(aseVersionNum)+"' while 'installmaster' version is '"+AseConnectionUtils.versionIntToStr(installmasterVersionNum)+"'. <br>" +
							"Please apply '$SYBASE/$SYBASE_ASE/scripts/installmaster' and check it's status with: sp_version. <br>" +
							"<br>" +
							"Do the following on the machine that hosts the ASE:<br>" +
							"<code>isql -Usa -Psecret -SSRVNAME -w999 -i$SYBASE/$SYBASE_ASE/scripts/installmaster</code><br>" +
							"<br>" +
							"If this is <b>not</b> done, SQL Statements issued by "+Version.getAppName()+" may fail due to version inconsistency (wrong column names etc).<br>" +
							"<br>" +
							"Also the MDA tables(mon*) may deliver faulty or corrupt information, because the MDA proxy table definitions are not in sync with it's underlying data structures.<br>" +
							"<br>" +
							"<hr>" + // horizontal ruler
							"<br>" +
							"<center><b>Choose what Version you want to initialize the Performance Counters with</b></center><br>" +
							"</html>";

//						SwingUtils.showErrorMessage(MainFrame.getInstance(), Version.getAppName()+" - connect check", msgHtml, null);
						//JOptionPane.showMessageDialog(MainFrame.getInstance(), msgHtml, Version.getAppName()+" - connect check", JOptionPane.ERROR_MESSAGE);

						Configuration config = Configuration.getInstance(Configuration.USER_TEMP);
						if (config != null)
						{
							Object[] options = {
									"ASE binary Version "                  + AseConnectionUtils.versionIntToStr(aseVersionNum), 
									"ASE montables/installmaster Version " + AseConnectionUtils.versionIntToStr(installmasterVersionNum)
									};
							int answer = JOptionPane.showOptionDialog(MainFrame.getInstance(), 
//								"ASE Binary and 'installmaster' is out of sync...\n" +
//									"What Version of ASE would you like to initialize the Performance Counters with?", // message
								msgHtml,
								"Initialize Performance Counters Using ASE Version", // title
								JOptionPane.YES_NO_OPTION,
								JOptionPane.WARNING_MESSAGE,
								null,     //do not use a custom Icon
								options,  //the titles of buttons
								options[0]); //default button title

							boolean useMonTablesVersion = answer > 0;
							_logger.warn("Setting option '"+GetCounters.PROPKEY_USE_MON_TABLES_VERSION+"' to '"+useMonTablesVersion+"'.");
							config.setProperty(GetCounters.PROPKEY_USE_MON_TABLES_VERSION, useMonTablesVersion);
							config.save();
						}
					}
				}
			}
		}
		
		// finally MARK it as initialized
		_initialized = true;
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
