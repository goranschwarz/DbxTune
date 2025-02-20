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
package com.dbxtune.central.pcs;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.Version;
import com.dbxtune.central.check.ReceiverAlarmCheck;
import com.dbxtune.central.lmetrics.LocalMetricsPersistWriterJdbc;
import com.dbxtune.central.pcs.CentralPersistWriterBase.Table;
import com.dbxtune.central.pcs.objects.DbxAlarmActive;
import com.dbxtune.central.pcs.objects.DbxAlarmHistory;
import com.dbxtune.central.pcs.objects.DbxCentralProfile;
import com.dbxtune.central.pcs.objects.DbxCentralServerDescription;
import com.dbxtune.central.pcs.objects.DbxCentralSessions;
import com.dbxtune.central.pcs.objects.DbxCentralUser;
import com.dbxtune.central.pcs.objects.DbxGraphData;
import com.dbxtune.central.pcs.objects.DbxGraphDescription;
import com.dbxtune.central.pcs.objects.DbxGraphProperties;
import com.dbxtune.central.pcs.objects.DsrSkipEntry;
import com.dbxtune.sql.conn.ConnectionProp;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.DbxConnectionPool;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class CentralPersistReader
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String  PROPKEY_SERVER_LIST_SORT = "CentralPersistReader.server.list.sort";
	public static final boolean DEFAULT_SERVER_LIST_SORT = true;
	
	
	/** implements singleton pattern */
	private static CentralPersistReader _instance = null;

	/** User information, just used to get what we are connected to
	    The connection is made outside */
	private String _jdbcDriver   = null;
	private String _jdbcUrl      = null;
	private String _jdbcUsername = null;
	private String _jdbcPassword = null;

	private DbxConnectionPool _connectionPool = null;

	// Cache all session/server names
	private Set<String> _sessionNames = null;
	
	// Unlimited query time
	private int _defaultQueryTimeout = 0; 

	/** 360 records is 2 hours, with a 20 seconds sample interval:  60 * 3 * 2 */
	private static final int SAMPLE_TYPE_AUTO__OVERFLOW_THRESHOLD = 360;
	
	/** Different ways we can use to get data from the storage tables */
	public enum SampleType
	{
		/** Sample everything */
		ALL,
		
		/** Auto switch to MAX_OVER_SAMPLES when there are to many samples */
		AUTO,
		
		/** Only take the MAX value from X samples, this can be used if there are to many samples */
		MAX_OVER_SAMPLES,

		/** Only take the MIN value from X samples, this can be used if there are to many samples */
		MIN_OVER_SAMPLES,

		/** Only take the MAX value from a sample over X minutes, this can be used if there are to many samples */
		MAX_OVER_MINUTES, 

		/** Only take the MIN value from a sample over X minutes, this can be used if there are to many samples */
		MIN_OVER_MINUTES, 

		/** Only take the Average value from a sample over X minutes, this can be used if there are to many samples */
		AVG_OVER_MINUTES,
		
		/** Sum all value from a sample over X minutes, this can be used if there are to many samples */
		SUM_OVER_MINUTES;

		/** parse the value */
		public static SampleType fromString(String text)
		{
			for (SampleType type : SampleType.values()) 
			{
				// check for upper/lower: 'MAX_OVER_MINUTES', 'max_over_minutes'
				if (type.name().equalsIgnoreCase(text))
					return type;

				// check for camelCase: 'maxOverMinutes', 'maxoverminutes'
				if (type.name().replace("_", "").equalsIgnoreCase(text))
					return type;
			}
			throw new IllegalArgumentException("No constant with text " + text + " found");
		}
	};

//	/** Hold information of last loaded session list, this can be used to send statistics */
//	private List<SessionInfo> _lastLoadedSessionList = null;
//
//	/** Used to get what version of AseTune that was used to store the Performance Counters, we might have to do backward name compatibility */
//	private MonVersionInfo _monVersionInfo = null;
//
//	/** hold information about if current selected sample */
//	private HashMap<String,CmIndicator> _currentIndicatorMap = new HashMap<String,CmIndicator>();
//
//	/** hold information about if current session */
//	private HashMap<String,SessionIndicator> _sessionIndicatorMap = new HashMap<String,SessionIndicator>();
//	
//	/** Execution mode, this means if we do the fetch in a background thread or directly in current thread */
//	private             int _execMode        = EXEC_MODE_BG;
////	private             int _execMode        = EXEC_MODE_DIRECT;
//	public static final int EXEC_MODE_BG     = 0;
//	public static final int EXEC_MODE_DIRECT = 1;
//
//	private int _numOfSamplesOnLastRefresh = -1;
//	private int _numOfSamplesNow           = -1;
//	
//	private static String GET_ALL_SESSIONS = 
//		"select \"SessionStartTime\", \"ServerName\", \"NumOfSamples\", \"LastSampleTime\" " +
//		"from " + PersistWriterBase.getTableName(PersistWriterBase.SESSIONS, null, true) + " "+
//		"order by \"SessionStartTime\"";
//
//	private static String GET_SESSION = 
//		"select \"SessionStartTime\", \"SessionSampleTime\" " +
//		"from " + PersistWriterBase.getTableName(PersistWriterBase.SESSION_SAMPLES, null, true) + " "+
//		"where \"SessionStartTime\" = ? " +
//		"order by \"SessionSampleTime\"";

	//////////////////////////////////////////////
	//// Constructors
	//////////////////////////////////////////////
//	static
//	{
//		_logger.setLevel(Level.DEBUG);
//	}
//	public PersistReader(Connection conn)
//	public CentralPersistReader(DbxConnection conn)
//	{
//		_conn = conn;
//
//		// Start the COMMAND READER THREAD
//		start();
//	}

	public void init()
	throws Exception
	{
		if (_jdbcDriver == null || _jdbcUrl == null || _jdbcUsername == null || _jdbcPassword == null)
			throw new Exception("JDBC Properties has not yet been set to the reader.");
		
		// Create a connection pool (probably move this to CentralPcs(Reader|Writer)Jdbc or some other place)
		ConnectionProp cp = new ConnectionProp();
		cp.setDriverClass(_jdbcDriver);
		cp.setUrl        (_jdbcUrl);
		cp.setUsername   (_jdbcUsername);
		cp.setPassword   (_jdbcPassword);
		cp.setAppName    (Version.getAppName()+"-pcsReader");
//		DbxConnectionPool connPool = new DbxConnectionPool(cp, 30);
//		DbxConnectionPool.setInstance(connPool);
		
		_connectionPool = new DbxConnectionPool(this.getClass().getSimpleName(), cp, 30);
		
		_logger.info("PCS Reader initialized using: "+getConnectionInfo());
	}



	
	/*---------------------------------------------------
	** BEGIN: Instance stuff
	**---------------------------------------------------
	*/
	public static CentralPersistReader getInstance()
	{
		return _instance;
	}

	public static boolean hasInstance()
	{
		return (_instance != null);
	}

	public static void setInstance(CentralPersistReader inst)
	{
		_instance = inst;
	}
	/*---------------------------------------------------
	** END: Instance stuff
	**---------------------------------------------------
	*/

	/*---------------------------------------------------
	** BEGIN: Connection methods
	**---------------------------------------------------
	*/
//	/**
//	 * Set the <code>Connection</code> to use for getting offline sessions.
//	 */
//	public void setConnection(DbxConnection conn)
//	{
//		_conn = conn;
//	}

	/*---------------------------------------------------
	 ** BEGIN: implementing ConnectionProvider
	 **---------------------------------------------------
	 */
	/**
	 * Gets a <code>Connection</code> to the Central Database storage (using a Connection Cache, so release the connection after usage with releaseConnection).
	 */
//	@Override
//	public Connection getConnection()
	public DbxConnection getConnection()
	throws SQLException
	{
		if (_connectionPool != null)
		{
			return _connectionPool.getConnection();
//			try
//			{
//				return _connectionPool.getConnection();
//			}
//			catch(SQLException ex)
//			{
//				_logger.warn("getConnection() returning null. caught: "+ex);
//				return null;
//			}
		}
		throw new SQLException("Connection pool for the Reader has not yet been initialized");
//		return _conn;
	}
	
	public void releaseConnection(DbxConnection conn)
	{
		if (_connectionPool != null)
		{
			_connectionPool.releaseConnection(conn);
		}
	}

//	@Override
////	public Connection getNewConnection(String connName)
//	public DbxConnection getNewConnection(String connName)
//	{
//		throw new RuntimeException("PersistentReader has not implemented the method 'getNewConnection(String)'");
//	}
	/*---------------------------------------------------
	 ** END: implementing ConnectionProvider
	 **---------------------------------------------------
	 */
	
//	/** Close the offline connection */
//	public void closeConnection()
//	{
//		// Close the Offline database connection
//		try
//		{
//			if (isConnected())
//			{
//				_conn.close();
//				if (_logger.isDebugEnabled())
//				{
//					_logger.debug("Offline connection closed");
//				}
//			}
//		}
//		catch (SQLException ev)
//		{
//			_logger.error("Closing Offline connection", ev);
//		}
//		_conn = null;
//	}

//	/**
//	 * Are we connected to a offline storage
//	 * @return true or false
//	 */
//	public boolean isConnected()
//	{
//		if (_conn == null) 
//			return false;
//
//		// check the connection itself
//		try
//		{
//			if (_conn.isClosed())
//				return false;
//		}
//		catch (SQLException e)
//		{
//			return false;
//		}
//
//		return true;
//	}
	/*---------------------------------------------------
	** END: Connection methods
	**---------------------------------------------------
	*/

	public void setJdbcDriver(String jdbcDriver) { _jdbcDriver   = jdbcDriver; }
	public void setJdbcUrl(String jdbcUrl)       { _jdbcUrl      = jdbcUrl; }
	public void setJdbcUser(String jdbcUser)     { _jdbcUsername = jdbcUser; }
	public void setJdbcPasswd(String jdbcPasswd) { _jdbcPassword = jdbcPasswd; }

	public String getJdbcDriver() { return _jdbcDriver; }
	public String getJdbcUrl()    { return _jdbcUrl; }
	public String getJdbcUser()   { return _jdbcUsername; }
	public String getJdbcPasswd() { return _jdbcPassword; }

	/** What is current JDBC settings */
	public String getConnectionInfo()
	{
		return "jdbcDriver='"   + _jdbcDriver     + "', " +
		       "jdbcUrl='"      + _jdbcUrl        + "', " +
		       "jdbcUsername='" + _jdbcUsername   + "', " +
		       "jdbcPassword='" + "*secret*"      + "'.";
	}
	
	public void setDefaultQueryTimeout(int val) { _defaultQueryTimeout = val;  }
	public int  getDefaultQueryTimeout()        { return _defaultQueryTimeout; }
	
//	/** 
//	 * Last loaded session(s) information <br>
//	 * This will be used to grab statistics on disconnect.
//	 */
//	public List<SessionInfo> getLastLoadedSessionList()
//	{
//		return _lastLoadedSessionList;
//	}

	/**
	 * Checks if this looks like a offline database
	 * <p>
	 * Just go and check for a 'set' of known tables that should be there
	 * 
	 * @return true if this looks like a offline db, otherwise false
	 */
	public static boolean isDbxCentralDb(DbxConnection conn)
	{
		// First check if we are connected
		if (conn == null) 
			return false;
		try
		{
			if (conn.isClosed())
				return false;
		}
		catch (SQLException e)
		{
			return false;
		}

		// Go and check for Tables
		try
		{
			String tabName = CentralPersistWriterBase.getTableName(conn, null, Table.CENTRAL_VERSION_INFO, null, false);

			// Obtain a DatabaseMetaData object from our current connection        
			DatabaseMetaData dbmd = conn.getMetaData();
	
			ResultSet rs = dbmd.getColumns(null, null, tabName, "%");
			boolean tabExists = rs.next();
			rs.close();

			return tabExists;
		}
		catch (SQLException e)
		{
			_logger.error("Problems checking if this is a valid 'central dbxtune' database.", e);
			return false;
		}
	}


	/**
	 * Get a list of active Alarms
	 * @param servername name of server (if null all schemas will be fetched)
	 * @return
	 */
	public List<DbxAlarmActive> getAlarmActive(String servername)
	throws SQLException
	{
		DbxConnection conn = getConnection(); // Get connection from a ConnectionPool
		try // block with: finally at end to return the connection to the ConnectionPool
		{
			List<DbxAlarmActive> list = new ArrayList<>();

			if (StringUtil.hasValue(servername))
			{
				getAlarmActive(conn, list, servername);
			}
			else // for ALL SCHEMAS
			{
				String onlyTabName = CentralPersistWriterBase.getTableName(conn, null, Table.ALARM_ACTIVE, null, false);

				// Obtain a DatabaseMetaData object from our current connection
				DatabaseMetaData dbmd = conn.getMetaData();
		
				// Get schemas
				Set<String> schemaSet = new LinkedHashSet<>();
				ResultSet schemaRs = dbmd.getSchemas();
				while (schemaRs.next())
					schemaSet.add(schemaRs.getString(1));
				schemaRs.close();

				// Loop schemas: if table exists, get data
				for (String schemaName : schemaSet)
				{
					ResultSet rs = dbmd.getColumns(null, schemaName, onlyTabName, "%");
					boolean tabExists = rs.next();
					rs.close();
			
					if( tabExists )
					{
						getAlarmActive(conn, list, schemaName);
					}
				}
			}
			
			return list;
		}
		finally
		{
			releaseConnection(conn);
		}
	}
	
	private void getAlarmActive(DbxConnection conn, List<DbxAlarmActive> list, String schema)
	throws SQLException
	{
		String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
		String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection

		String tabName = CentralPersistWriterBase.getTableName(conn, schema, Table.ALARM_ACTIVE, null, true);

		String sql = "select "
					+ "  '"+schema+"' as " + lq + "srvName"     + rq
					+ " ," + lq + "alarmClass"                  + rq
					+ " ," + lq + "serviceType"                 + rq
					+ " ," + lq + "serviceName"                 + rq
					+ " ," + lq + "serviceInfo"                 + rq
					+ " ," + lq + "extraInfo"                   + rq
					+ " ," + lq + "category"                    + rq
					+ " ," + lq + "severity"                    + rq
					+ " ," + lq + "state"                       + rq
					+ " ," + lq + "alarmId"                     + rq
					+ " ," + lq + "repeatCnt"                   + rq
					+ " ," + lq + "duration"                    + rq
					+ " ," + lq + "alarmDuration"               + rq
					+ " ," + lq + "fullDuration"                + rq
					+ " ," + lq + "fullDurationAdjustmentInSec" + rq
					+ " ," + lq + "createTime"                  + rq
					+ " ," + lq + "cancelTime"                  + rq
					+ " ," + lq + "timeToLive"                  + rq
					+ " ," + lq + "threshold"                   + rq
					+ " ," + lq + "data"                        + rq
					+ " ," + lq + "lastData"                    + rq
					+ " ," + lq + "description"                 + rq
					+ " ," + lq + "lastDescription"             + rq
					+ " ," + lq + "extendedDescription"         + rq
					+ " ," + lq + "lastExtendedDescription"     + rq
				+" from " + tabName
				+" order by " + lq+"createTime"+rq + ", " +lq+"cancelTime"+rq;

		// Change '[' and ']' into DBMS Vendor Specific Identity Quote Chars
		sql = conn.quotifySqlString(sql);
		
		// autoclose: stmnt, rs
		try (Statement stmnt = conn.createStatement())
		{
			// set TIMEOUT
			stmnt.setQueryTimeout(_defaultQueryTimeout);

			// Execute and read result
			try (ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
				{
					DbxAlarmActive a = new DbxAlarmActive(
						rs.getString   (1),  // "srvName"             
						rs.getString   (2),  // "alarmClass"             
						rs.getString   (3),  // "serviceType"            
						rs.getString   (4),  // "serviceName"            
						rs.getString   (5),  // "serviceInfo"            
						rs.getString   (6),  // "extraInfo"              
						rs.getString   (7),  // "category"               
						rs.getString   (8),  // "severity"               
						rs.getString   (9),  // "state"                  
						rs.getString   (10), // "alarmId"                  
						rs.getInt      (11), // "repeatCnt"              
						rs.getString   (12), // "duration"               
						rs.getString   (13), // "alarmDuration"               
						rs.getString   (14), // "fullDuration"               
						rs.getInt      (15), // "fullDurationAdjustmentInSec"               
						rs.getTimestamp(16), // "createTime"             
						rs.getTimestamp(17), // "cancelTime"             
						rs.getInt      (18), // "timeToLive"             
						rs.getString   (19), // "threshold"              
						rs.getString   (10), // "data"                   
						rs.getString   (21), // "lastData"               
						rs.getString   (22), // "description"            
						rs.getString   (23), // "lastDescription"        
						rs.getString   (24), // "extendedDescription"    
						rs.getString   (25)  // "lastExtendedDescription"
						);
					list.add(a);
				}
			}
		}
	}

	/**
	 * Clear All Active Alarms
	 * @param servername name of server
	 * @return
	 */
	public int clearAlarmsAllActive(String servername)
	throws SQLException
	{
		if (StringUtil.isNullOrBlank(servername))
			throw new RuntimeException("clearAlarmsAllActive(): mandatory parameter 'servername' was null or blank.");

		DbxConnection conn = getConnection(); // Get connection from a ConnectionPool
		try // block with: finally at end to return the connection to the ConnectionPool
		{
			String fullTabName = CentralPersistWriterBase.getTableName(conn, servername, Table.ALARM_ACTIVE, null, true);

			String sql = "delete from " + fullTabName;
			
			try (Statement stmnt = conn.createStatement())
			{
				return stmnt.executeUpdate(sql);
			}
		}
		finally
		{
			releaseConnection(conn);
		}
	}
	
	/**
	 * Get a list of historical Alarms
	 * @param servername Name of the server (or if '' null, then all servers, with the below restrictions) 
	 * @param category  DISK, CPU, etc...
	 * @param type      RAISE, RE-RAISE, CANCEL
	 * @param age       YYYY-MM-DD hh:mm:ss or -10, 10m, 10h, 10d, 10l   (-10=last-10-rows, 10m=last-10-minutes, 10h=last-10-hours, 10d=last-10-days, 10l=last-10-rows)
	 * @return
	 */
	public List<DbxAlarmHistory> getAlarmHistory(String servername, String age, String type, String category)
	throws SQLException
	{
		DbxConnection conn = getConnection(); // Get connection from a ConnectionPool
		try // block with: finally at end to return the connection to the ConnectionPool
		{
			List<DbxAlarmHistory> list = new ArrayList<>();

			if (StringUtil.hasValue(servername))
			{
				getAlarmHistory(conn, list, servername, age, type, category);
			}
			else // for ALL SCHEMAS
			{
				String onlyTabName = CentralPersistWriterBase.getTableName(conn, null, Table.ALARM_HISTORY, null, false);

				// Obtain a DatabaseMetaData object from our current connection
				DatabaseMetaData dbmd = conn.getMetaData();
		
				// Get schemas
				Set<String> schemaSet = new LinkedHashSet<>();
				ResultSet schemaRs = dbmd.getSchemas();
				while (schemaRs.next())
					schemaSet.add(schemaRs.getString(1));
				schemaRs.close();

				// Loop schemas: if table exists, get data
				for (String schemaName : schemaSet)
				{
					ResultSet rs = dbmd.getColumns(null, schemaName, onlyTabName, "%");
					boolean tabExists = rs.next();
					rs.close();
			
					if( tabExists )
					{
						getAlarmHistory(conn, list, schemaName, age, type, category);
					}
				}
			}
			
			return list;
		}
		finally
		{
			releaseConnection(conn);
		}
	}
	private void getAlarmHistory(DbxConnection conn, List<DbxAlarmHistory> list, String schema, String age, String type, String category)
	throws SQLException
	{
//System.out.println("----- getAlarmHistory(conn, list='" + list + "', schema='" + schema + "', age='" + age + "', type='" + type + "', category='" + category + "')");
		String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
		String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection

		String tabName = CentralPersistWriterBase.getTableName(conn, schema, Table.ALARM_HISTORY, null, true);

		// Build a where string
		String whereStr = " where 1 = 1 \n";

		// action = ${type}
		if (StringUtil.hasValue(type))
			whereStr += "   and " + lq + "action" + rq + " = '" + type.trim() + "' \n";
		
		// category = ${category}
		if (StringUtil.hasValue(category))
			whereStr += "   and " + lq + "category" + rq + " = '" + category.trim() + "' \n";
		
		// If we only want the last X number of records
		int onlyLastXNumOfRecords = 0;
		
		// eventTime >= ${age}
		if (StringUtil.hasValue(age))
		{
			try
			{
				age = age.trim();
				
				if (age.startsWith("-"))
				{
					age = age.substring(1);
					onlyLastXNumOfRecords = StringUtil.parseInt(age, 0);
				}
				
				if (onlyLastXNumOfRecords == 0)
				{
					// If value starts with a number and ends with m, h, d or l  (or M, H, D or L), then set 
					int multiPlayer = 60;
					if (age.matches("^[0-9]+[mhdlMHDL]$"))
					{
						String lastChar = age.substring(age.length()-1).toLowerCase();
						if ("m".equals(lastChar)) multiPlayer = 60;           // Minutes
						if ("h".equals(lastChar)) multiPlayer = 60 * 60;      // Hours
						if ("d".equals(lastChar)) multiPlayer = 24 * 60 * 60; // Days
						
						age = age.substring(0, age.length()-1);

						if ("l".equals(lastChar)) // keep only last X rows from the resultset
						{
							onlyLastXNumOfRecords = StringUtil.parseInt(age, 0);
							age = "all"; // below parseInt() will fail and "catch" will do the rest
						}
					}

					int intVal = Integer.parseInt(age);
					Timestamp ts = new Timestamp( System.currentTimeMillis() - (intVal * multiPlayer * 1000L)); // NOTE: the long/L after 1000 otherwise we will have int overflow 
					
					whereStr += "   and " + lq + "eventTime" + rq + " >= '" + ts.toString() + "' \n";
				}
			}
			catch (NumberFormatException nfe)
			{
				if ( ! "all".equals(age))
				{
					// lets hope the "age" is a valid date/timestamp
					whereStr += "   and " + lq + "eventTime" + rq + " >= '" + age + "' \n";
				}
			}
		}
		
		String sql = "select "
					+ "  '"+schema+"' as " + lq + "srvName"     + rq
					+ " ," + lq + "SessionStartTime"            + rq
					+ " ," + lq + "SessionSampleTime"           + rq
					+ " ," + lq + "eventTime"                   + rq
					+ " ," + lq + "action"                      + rq
					+ " ," + lq + "alarmClass"                  + rq
					+ " ," + lq + "serviceType"                 + rq
					+ " ," + lq + "serviceName"                 + rq
					+ " ," + lq + "serviceInfo"                 + rq
					+ " ," + lq + "extraInfo"                   + rq
					+ " ," + lq + "category"                    + rq
					+ " ," + lq + "severity"                    + rq
					+ " ," + lq + "state"                       + rq
					+ " ," + lq + "alarmId"                     + rq
					+ " ," + lq + "repeatCnt"                   + rq
					+ " ," + lq + "duration"                    + rq
					+ " ," + lq + "alarmDuration"               + rq
					+ " ," + lq + "fullDuration"                + rq
					+ " ," + lq + "fullDurationAdjustmentInSec" + rq
					+ " ," + lq + "createTime"                  + rq
					+ " ," + lq + "cancelTime"                  + rq
					+ " ," + lq + "timeToLive"                  + rq
					+ " ," + lq + "threshold"                   + rq
					+ " ," + lq + "data"                        + rq
					+ " ," + lq + "lastData"                    + rq
					+ " ," + lq + "description"                 + rq
					+ " ," + lq + "lastDescription"             + rq
					+ " ," + lq + "extendedDescription"         + rq
					+ " ," + lq + "lastExtendedDescription"     + rq
				+" from " + tabName
				+whereStr
			//	+" order by " + lq+"createTime"+rq + ", " +lq+"cancelTime"+rq;
				+" order by " + lq+"SessionStartTime"+rq + ", " +lq+"SessionSampleTime"+rq + ", " +lq+"eventTime"+rq;

//System.out.println("----- getAlarmHistory(): SQL=|" + sql + "|");
		// autoclose: stmnt, rs
		// autoclose: stmnt, rs
		try (Statement stmnt = conn.createStatement())
		{
			// set TIMEOUT
			stmnt.setQueryTimeout(_defaultQueryTimeout);

			// Execute and read result
			try (ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
				{
					DbxAlarmHistory a = new DbxAlarmHistory(
						rs.getString   (1),  // srvName                              
						rs.getTimestamp(2),  // SessionStartTime                    
						rs.getTimestamp(3),  // SessionSampleTime                   
						rs.getTimestamp(4),  // eventTime                           
						rs.getString   (5),  // action                              
						rs.getString   (6),  // alarmClass                          
						rs.getString   (7),  // serviceType                         
						rs.getString   (8),  // serviceName                         
						rs.getString   (9),  // serviceInfo                         
						rs.getString   (10),  // extraInfo                           
						rs.getString   (11), // category                            
						rs.getString   (12), // severity                            
						rs.getString   (13), // state                               
						rs.getString   (14), // alarmId                               
						rs.getInt      (15), // repeatCnt                           
						rs.getString   (16), // duration                            
						rs.getString   (17), // alarmDuration                       
						rs.getString   (18), // fullDuration                        
						rs.getInt      (19), // fullDurationAdjustmentInSec         
						rs.getTimestamp(20), // createTime                          
						rs.getTimestamp(21), // cancelTime                          
						rs.getInt      (22), // timeToLive                          
						rs.getString   (23), // threshold                           
						rs.getString   (24), // data                                
						rs.getString   (25), // lastData                            
						rs.getString   (26), // description                         
						rs.getString   (27), // lastDescription                     
						rs.getString   (28), // extendedDescription                 
						rs.getString   (29)  // lastExtendedDescription             
						);
					list.add(a);
				}
			}
		}

		// Only send last X number of records
		if (onlyLastXNumOfRecords > 0)
		{
			while(onlyLastXNumOfRecords < list.size())
			{
				list.remove(0);
			}
		}
//System.out.println("----- getAlarmHistory(): end: list.size=" + list.size());
	}


	
	
//	/**
//	 * Get a Map(srvName;cmName, jsonText) Last sent/stored JSON text for some CounterModels (not all are saved)
//	 * @param servername   name of server (if null all schemas will be fetched)
//	 * @param cmName       name of CounterModel (if null XXXXX)
//	 * @return
//	 */
//	public Map<String, String> getLastSampleForCm(String servername, String cmName)
//	throws SQLException
//	{
//		DbxConnection conn = getConnection(); // Get connection from a ConnectionPool
//		try // block with: finally at end to return the connection to the ConnectionPool
//		{
//			Map<String, String> map = new LinkedHashMap<>();
//
//			if (StringUtil.hasValue(servername))
//			{
//				getLastSampleForCm(conn, map, servername, cmName);
//			}
//			else // for ALL SCHEMAS
//			{
//				String onlyTabName = CentralPersistWriterBase.getTableName(conn, null, Table.CM_LAST_SAMPLE_JSON, null, false);
//
//				// Obtain a DatabaseMetaData object from our current connection
//				DatabaseMetaData dbmd = conn.getMetaData();
//		
//				// Get schemas
//				Set<String> schemaSet = new LinkedHashSet<>();
//				ResultSet schemaRs = dbmd.getSchemas();
//				while (schemaRs.next())
//					schemaSet.add(schemaRs.getString(1));
//				schemaRs.close();
//
//				// Loop schemas: if table exists, get data
//				for (String schemaName : schemaSet)
//				{
//					ResultSet rs = dbmd.getColumns(null, schemaName, onlyTabName, "%");
//					boolean tabExists = rs.next();
//					rs.close();
//			
//					if( tabExists )
//					{
//						getLastSampleForCm(conn, map, schemaName, cmName);
//					}
//				}
//			}
//			
//			return map;
//		}
//		finally
//		{
//			releaseConnection(conn);
//		}
//	}
//	
//	private void getLastSampleForCm(DbxConnection conn, Map<String, String> map, String schema, String cmName)
//	throws SQLException
//	{
//		String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
//		String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection
//
//		String tabName = CentralPersistWriterBase.getTableName(conn, schema, Table.CM_LAST_SAMPLE_JSON, null, true);
//
//		String sql = "select "
//					+ "  '"+schema+"' as " + lq + "srvName" + rq
//					+ " ," + lq + "SessionSampleTime"       + rq
//					+ " ," + lq + "CmName"                  + rq
//					+ " ," + lq + "JsonText"                + rq
//				+" from " + tabName
//				+" where " + lq+"CmName"+rq + " = " + DbUtils.safeStr(cmName);
//
//		// Change '[' and ']' into DBMS Vendor Specific Identity Quote Chars
//		sql = conn.quotifySqlString(sql);
//		
//		// autoclose: stmnt, rs
//		try (Statement stmnt = conn.createStatement())
//		{
//			// set TIMEOUT
//			stmnt.setQueryTimeout(_defaultQueryTimeout);
//
//			// Execute and read result
//			try (ResultSet rs = stmnt.executeQuery(sql))
//			{
//				while (rs.next())
//				{
//					String    srvName           = rs.getString   (1);  // "srvName"
//				//	Timestamp SessionSampleTime = rs.getTimestamp(2);  // "SessionSampleTime"
//					String    CmName            = rs.getString   (3);  // "CmName"
//					String    JsonText          = rs.getString   (4);  // "JsonText"
//
//					String key = srvName + ";" + CmName;
//
//					map.put(key, JsonText);
//				}
//			}
//		}
//	}

	/**
	 * Get a Map(srvName;cmName, jsonText) Last sent/stored JSON text for some CounterModels (not all are saved)
	 * @param servername   name of server (if null all schemas will be fetched)
	 * @param cmName       name of CounterModel (if null XXXXX)
	 * @return
	 */
//	public Map<String, Map<String, String>> getLastSampleForCm(String servername, String cmName)
//	throws SQLException
//	{
//		DbxConnection conn = getConnection(); // Get connection from a ConnectionPool
//		try // block with: finally at end to return the connection to the ConnectionPool
//		{
//			Map<String, Map<String, String>> map = new LinkedHashMap<>();
//
//			if (StringUtil.hasValue(servername))
//			{
//				for (String srvName : StringUtil.parseCommaStrToList(servername))
//				{
//					getLastSampleForCm(conn, map, srvName, cmName);
//				}
//			}
//			else // for ALL SCHEMAS
//			{
//				String onlyTabName = CentralPersistWriterBase.getTableName(conn, null, Table.CM_LAST_SAMPLE_JSON, null, false);
//
//				// Obtain a DatabaseMetaData object from our current connection
//				DatabaseMetaData dbmd = conn.getMetaData();
//		
//				// Get schemas
//				Set<String> schemaSet = new LinkedHashSet<>();
//				ResultSet schemaRs = dbmd.getSchemas();
//				while (schemaRs.next())
//					schemaSet.add(schemaRs.getString(1));
//				schemaRs.close();
//
//				// Loop schemas: if table exists, get data
//				for (String schemaName : schemaSet)
//				{
//					ResultSet rs = dbmd.getColumns(null, schemaName, onlyTabName, "%");
//					boolean tabExists = rs.next();
//					rs.close();
//			
//					if( tabExists )
//					{
//						getLastSampleForCm(conn, map, schemaName, cmName);
//					}
//				}
//			}
//			
//			return map;
//		}
//		finally
//		{
//			releaseConnection(conn);
//		}
//	}
	public Map<String, Map<String, String>> getLastSampleForCm(String servername, String cmName)
	throws SQLException
	{
		DbxConnection conn = getConnection(); // Get connection from a ConnectionPool
		try // block with: finally at end to return the connection to the ConnectionPool
		{
			// Store results here
			Map<String, Map<String, String>> map = new LinkedHashMap<>();

			// Obtain a DatabaseMetaData object from our current connection
			DatabaseMetaData dbmd = conn.getMetaData();
	
			// Get schemas
			Set<String> schemaSet = new LinkedHashSet<>();
			ResultSet schemaRs = dbmd.getSchemas();
			while (schemaRs.next())
			{
				String schemaName = schemaRs.getString(1);
				if ("PUBLIC".equalsIgnoreCase(schemaName) || "INFORMATION_SCHEMA".equalsIgnoreCase(schemaName))
					continue;
				schemaSet.add(schemaName);
			}
			schemaRs.close();


			// What server names do we want to check
			List<String> srvNameList = new ArrayList<>();

			if (StringUtil.hasValue(servername))
			{
				// Check if schema exists
				for (String srvName : StringUtil.parseCommaStrToList(servername))
				{
					if (schemaSet.contains(srvName))
						srvNameList.add(srvName);
					else
						_logger.debug("getLastSampleForCm(): Skipping checking srvName '" + srvName +  "', because it didnt exist. Existing schemas is: " + schemaSet);
					//	_logger.warn("getLastSampleForCm(): Skipping checking srvName '" + srvName +  "', because it didnt exist. Existing schemas is: " + schemaSet);
				}
			}
			else // for ALL SCHEMAS
			{
				srvNameList.addAll(schemaSet);
			}

			String onlyTabName = CentralPersistWriterBase.getTableName(conn, null, Table.CM_LAST_SAMPLE_JSON, null, false);

			// Now Get from the LIST
			for (String srvName : srvNameList)
			{
				// Loop: if table exists, get data
				ResultSet rs = dbmd.getColumns(null, srvName, onlyTabName, "%");
				boolean tabExists = rs.next();
				rs.close();
		
				if( tabExists )
				{
					getLastSampleForCm(conn, map, srvName, cmName);
				}
				else
				{
					_logger.warn("getLastSampleForCm(): Skipping checking srvName '" + srvName +  "', because table '" + onlyTabName + "' didnt exist.");
				}
			}
			
			return map;
		}
		finally
		{
			releaseConnection(conn);
		}
	}
	
	private void getLastSampleForCm(DbxConnection conn, Map<String, Map<String, String>> map, String schema, String cmName)
	throws SQLException
	{
		String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
		String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection

		String tabName = CentralPersistWriterBase.getTableName(conn, schema, Table.CM_LAST_SAMPLE_JSON, null, true);

		String sql = "select "
					+ "  '"+schema+"' as " + lq + "srvName" + rq
					+ " ," + lq + "SessionSampleTime"       + rq
					+ " ," + lq + "CmName"                  + rq
					+ " ," + lq + "JsonText"                + rq
				+" from " + tabName
				+" where " + lq+"CmName"+rq + " = " + DbUtils.safeStr(cmName); // Possibly convert to IN (...javaList...)

		// Change '[' and ']' into DBMS Vendor Specific Identity Quote Chars
		sql = conn.quotifySqlString(sql);
		
		// autoclose: stmnt, rs
		try (Statement stmnt = conn.createStatement())
		{
			// set TIMEOUT
			stmnt.setQueryTimeout(_defaultQueryTimeout);

			// Execute and read result
			try (ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
				{
					String    srvName           = rs.getString   (1);  // "srvName"
				//	Timestamp SessionSampleTime = rs.getTimestamp(2);  // "SessionSampleTime"
					String    CmName            = rs.getString   (3);  // "CmName"
					String    JsonText          = rs.getString   (4);  // "JsonText"

//					String key = srvName + ";" + CmName;

					Map<String, String> cmValMap = map.get(srvName);
					if (cmValMap == null)
						cmValMap = new LinkedHashMap<>();

					cmValMap.put(CmName, JsonText);
					map.put(srvName, cmValMap);
				}
			}
		}
	}

//	// DEBUG: used by CentralPersistWriterJdbc.saveCmJsonCounters() to get data (check if stored data is the same as received data)
//	public static String xxxxx_getLastSampleForCm(DbxConnection conn, String schema, String cmName)
//	throws SQLException
//	{
//		String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
//		String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection
//
//		String tabName = CentralPersistWriterBase.getTableName(conn, schema, Table.CM_LAST_SAMPLE_JSON, null, true);
//
//		String sql = "select "
//					+ "  '"+schema+"' as " + lq + "srvName" + rq
//					+ " ," + lq + "SessionSampleTime"       + rq
//					+ " ," + lq + "CmName"                  + rq
//					+ " ," + lq + "JsonText"                + rq
//				+" from " + tabName
//				+" where " + lq+"CmName"+rq + " = " + DbUtils.safeStr(cmName);
//
//		// Change '[' and ']' into DBMS Vendor Specific Identity Quote Chars
//		sql = conn.quotifySqlString(sql);
//		
//		// autoclose: stmnt, rs
//		try (Statement stmnt = conn.createStatement())
//		{
//			// set TIMEOUT
////			stmnt.setQueryTimeout(_defaultQueryTimeout);
//
//			// Execute and read result
//			try (ResultSet rs = stmnt.executeQuery(sql))
//			{
//				while (rs.next())
//				{
//					String    srvName           = rs.getString   (1);  // "srvName"
//				//	Timestamp SessionSampleTime = rs.getTimestamp(2);  // "SessionSampleTime"
//					String    CmName            = rs.getString   (3);  // "CmName"
//					String    JsonText          = rs.getString   (4);  // "JsonText"
//
//					return JsonText;
//				}
//			}
//		}
//		return "";
//	}
	
	/**
	 * Clear All Active Alarms
	 * @param servername name of server
	 * @return
	 */
	public int clearLastSampleForCm(String servername, String cmName)
	throws SQLException
	{
		if (StringUtil.isNullOrBlank(servername))
			throw new RuntimeException("clearLastSampleForCm(): mandatory parameter 'servername' was null or blank.");

		if (StringUtil.isNullOrBlank(cmName))
			throw new RuntimeException("clearLastSampleForCm(): mandatory parameter 'cmName' was null or blank.");

		DbxConnection conn = getConnection(); // Get connection from a ConnectionPool
		try // block with: finally at end to return the connection to the ConnectionPool
		{
			String fullTabName = CentralPersistWriterBase.getTableName(conn, servername, Table.CM_LAST_SAMPLE_JSON, null, true);

			String sql = "delete from " + fullTabName + " where [CmName] = " + DbUtils.safeStr(cmName);
			sql = conn.quotifySqlString(sql);
			
			try (Statement stmnt = conn.createStatement())
			{
				return stmnt.executeUpdate(sql);
			}
		}
		finally
		{
			releaseConnection(conn);
		}
	}




	/**
	 * Get a Map(srvName;cmName, jsonText) for a specific "sampleTime" sent/stored JSON text for some CounterModels (not all are saved)
	 * @param servername   name of server (if null all schemas will be fetched)
	 * @param cmName       name of CounterModel (if null XXXXX)
	 * @param sampleTime   Sample time we are interested in
	 * @return
	 */
//	public Map<String, Map<String, String>> getHistorySampleForCm(String servername, String cmName, Timestamp sampleTime)
//	throws SQLException
//	{
//		// TODO: Same thing as getLastSampleForCm() but for a specific "sampleTime"
//		//       This so we can show the info in the WEB UI
//		//       IDEA: Here is how it will work in the future: 
//		//         - when we click on a graph
//		//         - a Time Range Slider will show at the top (when we select a time)
//		//         - we will get information about a CM
//		//         - then we can show details from that CM in the WEB UI (like we do with "CmActiveStatements")
//		
//		throw new RuntimeException("-NOT-YET-IMPLEMENTED-");
//	}
	public Map<String, Map<String, String>> getHistorySampleForCm(String servername, String cmName, Timestamp sampleTime, Timestamp startTime, Timestamp endTime, boolean addSampleTimeToJson, boolean sampleTimeShort)
	throws SQLException
	{
		DbxConnection conn = getConnection(); // Get connection from a ConnectionPool
		try // block with: finally at end to return the connection to the ConnectionPool
		{
			// Store results here
			Map<String, Map<String, String>> map = new LinkedHashMap<>();

			// Obtain a DatabaseMetaData object from our current connection
			DatabaseMetaData dbmd = conn.getMetaData();
	
			// Get schemas
			Set<String> schemaSet = new LinkedHashSet<>();
			ResultSet schemaRs = dbmd.getSchemas();
			while (schemaRs.next())
			{
				String schemaName = schemaRs.getString(1);
				if ("PUBLIC".equalsIgnoreCase(schemaName) || "INFORMATION_SCHEMA".equalsIgnoreCase(schemaName))
					continue;
				schemaSet.add(schemaName);
			}
			schemaRs.close();


			// What server names do we want to check
			List<String> srvNameList = new ArrayList<>();

			if (StringUtil.hasValue(servername))
			{
				// Check if schema exists
				for (String srvName : StringUtil.parseCommaStrToList(servername))
				{
					if (schemaSet.contains(srvName))
						srvNameList.add(srvName);
					else
						_logger.debug("getHistorySampleForCm(): Skipping checking srvName '" + srvName +  "', because it didnt exist. Existing schemas is: " + schemaSet);
					//	_logger.warn("getHistorySampleForCm(): Skipping checking srvName '" + srvName +  "', because it didnt exist. Existing schemas is: " + schemaSet);
				}
			}
			else // for ALL SCHEMAS
			{
				srvNameList.addAll(schemaSet);
			}

			String onlyTabName = CentralPersistWriterBase.getTableName(conn, null, Table.CM_HISTORY_SAMPLE_JSON, null, false);

			// Now Get from the LIST
			for (String srvName : srvNameList)
			{
				// Loop: if table exists, get data
				ResultSet rs = dbmd.getColumns(null, srvName, onlyTabName, "%");
				boolean tabExists = rs.next();
				rs.close();
		
				if( tabExists )
				{
					getHistorySampleForCm(conn, map, srvName, cmName, sampleTime, startTime, endTime, addSampleTimeToJson, sampleTimeShort);
				}
				else
				{
					_logger.warn("getHistorySampleForCm(): Skipping checking srvName '" + srvName +  "', because table '" + onlyTabName + "' didnt exist.");
				}
			}
			
			return map;
		}
		finally
		{
			releaseConnection(conn);
		}
	}
	
	private void getHistorySampleForCm(DbxConnection conn, Map<String, Map<String, String>> map, String schema, String cmName, Timestamp sampleTime, Timestamp startTime, Timestamp endTime, boolean addSampleTimeToJson, boolean sampleTimeShort)
	throws SQLException
	{
		String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
		String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection

		String ymdNow = TimeUtils.toStringYmd( new Timestamp(System.currentTimeMillis()));
		
		Timestamp startTs = null;
		Timestamp endTs   = null;

		if (sampleTime != null)
		{
			startTs = new Timestamp(sampleTime.getTime() - 1000); // set start time 1 send previous "sampleTime" this to do a "like" search
			endTs   = new Timestamp(sampleTime.getTime() + 1000); // set start time 1 send after    "sampleTime" this to do a "like" search
		}
		else
		{
			startTs = startTime;
			endTs   = endTime;
			
			if (endTs == null)
				endTs = new Timestamp(System.currentTimeMillis());
		}
		
		String tabName = CentralPersistWriterBase.getTableName(conn, schema, Table.CM_HISTORY_SAMPLE_JSON, null, true);

		String sql = "select "
					+ "  '"+schema+"' as " + lq + "srvName" + rq
					+ " ," + lq + "SessionSampleTime"       + rq
					+ " ," + lq + "CmName"                  + rq
					+ " ," + lq + "JsonText"                + rq
				+" from " + tabName
				+" where " + lq+"CmName"+rq + " = " + DbUtils.safeStr(cmName)
				+"   and " + lq+"SessionSampleTime"+rq + " >= " + DbUtils.safeStr(startTs) // We could have used BETWEEN '' AND ''
				+"   and " + lq+"SessionSampleTime"+rq + "  < " + DbUtils.safeStr(endTs)
				;
//System.out.println(">>>>>>>>>>>>>>>>>>>> getHistorySampleForCm(); SQL=|" + sql + "|");

		// Change '[' and ']' into DBMS Vendor Specific Identity Quote Chars
		sql = conn.quotifySqlString(sql);
		
		// autoclose: stmnt, rs
		try (Statement stmnt = conn.createStatement())
		{
			// set TIMEOUT
			stmnt.setQueryTimeout(_defaultQueryTimeout);

			// Execute and read result
			try (ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
				{
					String    srvName           = rs.getString   (1);  // "srvName"
					Timestamp SessionSampleTime = rs.getTimestamp(2);  // "SessionSampleTime"
					String    CmName            = rs.getString   (3);  // "CmName"
					String    JsonText          = rs.getString   (4);  // "JsonText"

//System.out.println(">>>>>>>>>>>>>>>>>>>> getHistorySampleForCm(); ROW... srvName=|" + srvName + "|, CmName=|" + CmName + "|, SessionSampleTime|" + SessionSampleTime + "|, JsonText=|" + JsonText + "|");

					if (addSampleTimeToJson)
					{
						String sampleTimeStr = SessionSampleTime.toString();
						if (sampleTimeShort && sampleTimeStr.startsWith(ymdNow))
							sampleTimeStr = sampleTimeStr.substring("yyyy-MM-dd ".length(), "yyyy-MM-dd HH:mm:ss".length());
						
						JsonText = addSampleTimeToHistoryStatementJson(JsonText, sampleTimeStr, true);
//System.out.println(">>>>>>>>>>>>>>>>>>>> getHistorySampleForCm(); ADDED 'sampleTime' ROW... srvName=|" + srvName + "|, CmName=|" + CmName + "|, SessionSampleTime|" + SessionSampleTime + "|, JsonText=|" + JsonText + "|");
					}

					// TODO: Possibly -- Inject 'SessionSampleTime' as 'sampleTime' (YYYY-MM-DD hh:mm:ss) into the: JsonText

//					String key = srvName + ";" + CmName;

					Map<String, String> cmValMap = map.get(srvName);
					if (cmValMap == null)
						cmValMap = new LinkedHashMap<>();

					cmValMap.put(CmName, JsonText);
					map.put(srvName, cmValMap);
				}
			}
		}
	}

	private static String addSampleTimeToHistoryStatementJson(String jsonText, String sampleTimeStr, boolean prettyPrint)
	{
		try
		{
			// Create an JSON Object Mapper
			ObjectMapper om = new ObjectMapper();

			// 
			StringWriter sw = new StringWriter();

			JsonFactory jfactory = new JsonFactory();
			JsonGenerator gen = jfactory.createGenerator(sw);
			if (prettyPrint)
				gen.setPrettyPrinter(new DefaultPrettyPrinter());
			gen.setCodec(om); // key to get 'gen.writeTree(cmLastSampleJsonTree)' to work

			JsonNode node = om.readTree(jsonText);
//			private_addSampleTime(node, sampleTime.toString(), false); // false is internally used as a switch WHEN to start writing 'sampleTime'

			// Add JSON Field: "sampleTime" : "-time-description-" to 'absCounters', 'diffCounters' and 'rateCounters'
			List<JsonNode> xxxCounters = new ArrayList<>();
			xxxCounters.add( node.path("counters").path("absCounters") );
			xxxCounters.add( node.path("counters").path("diffCounters") );
			xxxCounters.add( node.path("counters").path("rateCounters") );
			for (JsonNode locatedNode : xxxCounters)
			{
				if (locatedNode.isArray())
				{
					ArrayNode arrayNode = (ArrayNode) locatedNode;
					for (int i = 0; i < arrayNode.size(); i++) 
					{
						JsonNode jsonNode = arrayNode.get(i);
						if (jsonNode.isObject())
						{
							ObjectNode objectNode = (ObjectNode) jsonNode;
							
							// Take a copy of all objects, then remove everything
							JsonNode deepCopy = objectNode.deepCopy();
							objectNode.removeAll();

							// Add the 'sampleTime'
							objectNode.put("sampleTime", sampleTimeStr);

							// Add all entries from the "deepCopy"
							Iterator<Entry<String, JsonNode>> fields = deepCopy.fields();
							while (fields.hasNext()) 
							{
								Entry<String, JsonNode> jsonField = fields.next();
								objectNode.set(jsonField.getKey(), jsonField.getValue());
							}
						}
					}
				}
			}

			// If we in above xxxCounters added "sampleTime", we also needs to add META DATA for that column 
			// Add JSON Object: "columnName" : "sampleTime" as FIRST entry in 'metaData'
			JsonNode metaData = node.path("counters").path("metaData");
			if (metaData != null && metaData.isArray())
			{
				ArrayNode arrayNode = (ArrayNode) metaData; 

				// Create a new "metaData" object entry at START
				ObjectNode newObjectName = arrayNode.insertObject(0); // or possibly -1
				newObjectName.put("columnName",   "sampleTime");
				newObjectName.put("isDiffColumn", false);
				newObjectName.put("isPctColumn",  false);
			}

			gen.writeTree(node);
			
			String newJsonStr = sw.toString();
//System.out.println("XXXXXXXXXXXXXXX: newJsonStr=|" + newJsonStr + "|.");
			return newJsonStr;
//			return jsonText;
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			_logger.error("Problems injecting 'sampleTime' into JSON str.", ex);
			return jsonText;
		}
	}

	public static void main(String[] args)
	{
		String jsonText = ""
				+ "{ \n"
				+ "  \"counters\": { \n"
				+ "    \"absCounters\": [ \n"
				+ "      { \n"
				+ "        \"monSource\": \"ACTIVE\", \n"
				+ "        \"SPID\": 111, \n"
				+ "        \"KPID\": 111, \n"
				+ "        \"AAA\": 1 \n"
				+ "      } \n"
				+ "    ], \n"
				+ "    \"diffCounters\": [ \n"
				+ "      { \n"
				+ "        \"monSource\": \"ACTIVE\", \n"
				+ "        \"SPID\": 222, \n"
				+ "        \"KPID\": 222, \n"
				+ "        \"BBB\": 2 \n"
				+ "      } \n"
				+ "    ], \n"
				+ "    \"rateCounters\": [ \n"
				+ "      { \n"
				+ "        \"monSource\": \"ACTIVE\", \n"
				+ "        \"SPID\": 333, \n"
				+ "        \"KPID\": 333, \n"
				+ "        \"CCC\": 3 \n"
				+ "      } \n"
				+ "    ] \n"
				+ "  } \n"
				+ "} \n"
				+ "";		

		String ymdNow = TimeUtils.toStringYmd( new Timestamp(System.currentTimeMillis()) );

		String sampleTimeStr = new Timestamp(System.currentTimeMillis()).toString();
		if (true && sampleTimeStr.startsWith(ymdNow))
			sampleTimeStr = sampleTimeStr.substring("yyyy-MM-dd ".length(), "yyyy-MM-dd HH:mm:ss".length());
		
		System.out.println("sampleTimeStr=|" + sampleTimeStr + "|");

		String xxx = addSampleTimeToHistoryStatementJson(jsonText, sampleTimeStr, true);
		System.out.println(xxx);
	}


	/**
	 * Get a Map(sampleTime;srvName, list:cmName) for start/end time get entries stored in "DbxCmHistorySampleJson" for each serverName/schema
	 * 
	 * @param servername
	 * @param cmName
	 * @param startTime
	 * @param endTime
	 * @return a TreeMap which is sorted on key:"sampleTime", with a Map of key:"ServerName", which holds a Set of "cmNames"
	 * @throws SQLException
	 */
	public Map<Timestamp, Map<String, Set<String>>> getHistoryActiveSamplesForCm(String servername, String cmName, Timestamp startTime, Timestamp endTime)
	throws SQLException
	{
		DbxConnection conn = getConnection(); // Get connection from a ConnectionPool
		try // block with: finally at end to return the connection to the ConnectionPool
		{
			// Store results here
//			Map<String, Map<String, String>> map = new LinkedHashMap<>();
			Map<Timestamp, Map<String, Set<String>>> map = new TreeMap<>();

			// Obtain a DatabaseMetaData object from our current connection
			DatabaseMetaData dbmd = conn.getMetaData();
	
			// Get schemas
			Set<String> schemaSet = new LinkedHashSet<>();
			ResultSet schemaRs = dbmd.getSchemas();
			while (schemaRs.next())
			{
				String schemaName = schemaRs.getString(1);
				if ("PUBLIC".equalsIgnoreCase(schemaName) || "INFORMATION_SCHEMA".equalsIgnoreCase(schemaName))
					continue;
				schemaSet.add(schemaName);
			}
			schemaRs.close();


			// What server names do we want to check
			List<String> srvNameList = new ArrayList<>();

			if (StringUtil.hasValue(servername))
			{
				// Check if schema exists
				for (String srvName : StringUtil.parseCommaStrToList(servername))
				{
					if (schemaSet.contains(srvName))
						srvNameList.add(srvName);
					else
						_logger.debug("getHistoryActiveSamplesForCm(): Skipping checking srvName '" + srvName +  "', because it didnt exist. Existing schemas is: " + schemaSet);
					//	_logger.warn("getLastSampleForCm(): Skipping checking srvName '" + srvName +  "', because it didnt exist. Existing schemas is: " + schemaSet);
				}
			}
			else // for ALL SCHEMAS
			{
				srvNameList.addAll(schemaSet);
			}

			String onlyTabName = CentralPersistWriterBase.getTableName(conn, null, Table.CM_HISTORY_SAMPLE_JSON, null, false);

			// Now Get from the LIST
			for (String srvName : srvNameList)
			{
				// Loop: if table exists, get data
				ResultSet rs = dbmd.getColumns(null, srvName, onlyTabName, "%");
				boolean tabExists = rs.next();
				rs.close();
		
				if( tabExists )
				{
					getHistoryActiveSamplesForCm(conn, map, srvName, cmName, startTime, endTime);
				}
				else
				{
					_logger.warn("getHistoryActiveSamplesForCm(): Skipping checking srvName '" + srvName +  "', because table '" + onlyTabName + "' didnt exist.");
				}
			}
			
			return map;
		}
		finally
		{
			releaseConnection(conn);
		}
	}
	private void getHistoryActiveSamplesForCm(DbxConnection conn, Map<Timestamp, Map<String, Set<String>>> map, String schema, String cmName, Timestamp startTime, Timestamp endTime)
	throws SQLException
	{
		String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
		String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection

		Timestamp startTs = new Timestamp(startTime.getTime() - 1000); // set start time 1 send previous "sampleTime" this to do a "like" search
		Timestamp endTs   = new Timestamp(endTime.getTime() + 1000); // set start time 1 send after    "sampleTime" this to do a "like" search
		
		String tabName = CentralPersistWriterBase.getTableName(conn, schema, Table.CM_HISTORY_SAMPLE_JSON, null, true);

		String whereCmName = "";
		if (StringUtil.hasValue(cmName))
			whereCmName = "   and " + lq+"CmName"+rq + " = " + DbUtils.safeStr(cmName);
		
		String sql = "select "
					+ "  '"+schema+"' as " + lq + "srvName" + rq
					+ " ," + lq + "SessionSampleTime"       + rq
					+ " ," + lq + "CmName"                  + rq
				+ " from " + tabName
				+ " where 1 = 1 "
				+ whereCmName
				+ "   and " + lq+"SessionSampleTime"+rq + " >= " + DbUtils.safeStr(startTs) // We could have used BETWEEN '' AND ''
				+ "   and " + lq+"SessionSampleTime"+rq + "  < " + DbUtils.safeStr(endTs)
				;
//System.out.println(">>>>>>>>>>>>>>>>>>>> getHistoryActiveSamplesForCm(); SQL=|" + sql + "|");

		// Change '[' and ']' into DBMS Vendor Specific Identity Quote Chars
		sql = conn.quotifySqlString(sql);
		
		// autoclose: stmnt, rs
		try (Statement stmnt = conn.createStatement())
		{
			// set TIMEOUT
			stmnt.setQueryTimeout(_defaultQueryTimeout);

			// Execute and read result
			try (ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
				{
					String    srvName           = rs.getString   (1);  // "srvName"
					Timestamp SessionSampleTime = rs.getTimestamp(2);  // "SessionSampleTime"
					String    CmName            = rs.getString   (3);  // "CmName"

//System.out.println(">>>>>>>>>>>>>>>>>>>> getHistoryActiveSamplesForCm(); ROW... , SessionSampleTime|" + SessionSampleTime + "|, srvName=|" + srvName + "|, CmName=|" + CmName + "|");

//					String key = srvName + ";" + CmName;

//					Map<String, String> cmValMap = map.get(srvName);
//					if (cmValMap == null)
//						cmValMap = new LinkedHashMap<>();
//
//					cmValMap.put(CmName, JsonText);
//					map.put(srvName, cmValMap);

					Map<String, Set<String>> srvCmMap = map.get(SessionSampleTime);
					if (srvCmMap == null)
						srvCmMap = new LinkedHashMap<>();

					Set<String> cmSet = srvCmMap.get(srvName);
					if (cmSet == null)
						cmSet = new LinkedHashSet<>();
					
					cmSet.add(CmName);
					srvCmMap.put(srvName, cmSet);
					map.put(SessionSampleTime, srvCmMap);
				}
			}
		}
	}
	



	/**
	 * Get LAST Session
	 * @param conn
	 * @param serverName
	 * @return
	 * @throws SQLException
	 */
	public DbxCentralSessions getLastSession(DbxConnection conn, String serverName)
	throws SQLException
	{
		if (conn == null)
			throw new SQLException("Connection must be valid, conn="+conn);

		String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
		String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection

		String tabName = CentralPersistWriterBase.getTableName(conn, null, Table.CENTRAL_SESSIONS, null, true);

		String sql = "select "
					+ "  " + lq + "SessionStartTime"        + rq
					+ " ," + lq + "Status"                  + rq
					+ " ," + lq + "ServerName"              + rq
					+ " ," + lq + "ServerDisplayName"       + rq
					+ " ," + lq + "OnHostname"              + rq
					+ " ," + lq + "ProductString"           + rq
					+ " ," + lq + "VersionString"           + rq
					+ " ," + lq + "BuildString"             + rq
					+ " ," + lq + "CollectorHostname"       + rq
					+ " ," + lq + "CollectorSampleInterval" + rq
					+ " ," + lq + "CollectorCurrentUrl"     + rq
					+ " ," + lq + "CollectorInfoFile"       + rq
//					+ " ," + lq + "CollectorMgtHostname"    + rq
//					+ " ," + lq + "CollectorMgtPort"        + rq
//					+ " ," + lq + "CollectorMgtInfo"        + rq
					+ " ," + lq + "NumOfSamples"            + rq
					+ " ," + lq + "LastSampleTime"          + rq
				+" from " + tabName
				+" where " + lq + "ServerName"       + rq + " = '" + serverName + "'"
				+"   and " + lq + "SessionStartTime" + rq + " = (select max("+lq+"SessionStartTime"+rq+") from "+tabName+" where "+lq+"ServerName"+rq+" = '"+serverName+"') "
				+"";

		// Get server order/description file
		Map<String, DbxCentralServerDescription> sdMap = new HashMap<>();
		try 
		{
			sdMap = DbxCentralServerDescription.getFromFile();
		}
		catch (IOException ex)
		{
			_logger.warn("Problems reading file '"+DbxCentralServerDescription.getDefaultFile()+"'. This is used to sort the 'sessions list'. Skipping this... Caught: "+ex);
		}
		
		// autoclose: stmnt, rs
		try (Statement stmnt = conn.createStatement())
		{
			// set TIMEOUT
			stmnt.setQueryTimeout(_defaultQueryTimeout);

			// Execute and read result
			try (ResultSet rs = stmnt.executeQuery(sql))
			{
				DbxCentralSessions s = null;
				while (rs.next())
				{
					// Get: serverDescrption & serverExtraInfo
					String srvName          = rs.getString(3);
					String serverDescrption = "";
					String serverExtraInfo  = "";
					DbxCentralServerDescription sd = sdMap.get(srvName);
					if (sd != null)
					{
						serverDescrption = sd.getDescription();
						serverExtraInfo  = sd.getExtraInfo();
					}
	
					int c = 1;
					s = new DbxCentralSessions(
						rs.getTimestamp(c++), // "SessionStartTime"       
						rs.getInt      (c++), // "Status"
						rs.getString   (c++), // "ServerName"             
						rs.getString   (c++), // "ServerDisplayName"      
						rs.getString   (c++), // "OnHostname"             
						rs.getString   (c++), // "ProductString"          
						rs.getString   (c++), // "VersionString"          
						rs.getString   (c++), // "BuildString"            
						rs.getString   (c++), // "CollectorHostname"      
						rs.getInt      (c++), // "CollectorSampleInterval"
						rs.getString   (c++), // "CollectorCurrentUrl"
						rs.getString   (c++), // "CollectorInfoFile"
//						rs.getString   (c++), // "CollectorMgtHostname"
//						rs.getInt      (c++), // "CollectorMgtPort"           
//						rs.getString   (c++), // "CollectorMgtInfo"
						rs.getInt      (c++), // "NumOfSamples"           
						rs.getTimestamp(c++), // "LastSampleTime"         
						serverDescrption,    // serverDescrption,
						serverExtraInfo,     // serverExtraInfo,
						null);               // graphProperties
				}
				return s;
			}
		}
	}
	
	/**
	 * Get LAST Session (using a connection from the connection pool)
	 * @param serverName
	 * @return
	 * @throws SQLException
	 */
	public DbxCentralSessions getLastSession(String serverName)
	throws SQLException
	{
		DbxConnection conn = getConnection(); // Get connection from a ConnectionPool
		try // block with: finally at end to return the connection to the ConnectionPool
		{
			return getLastSession(conn, serverName);
		}
		finally
		{
			releaseConnection(conn);
		}
	}
	
	/**
	 * Get a list of session names (a session is stored in a schema with the same name)
	 * @param onlyLast only the last session (servername will be used as a unique)
	 * @param status 
	 * @return
	 */
	public List<DbxCentralSessions> getSessions(boolean onlyLast, int status, String... orderByList)
	throws SQLException
	{
		DbxConnection conn = getConnection(); // Get connection from a ConnectionPool
		try // block with: finally at end to return the connection to the ConnectionPool
		{
			String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
			String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection

			String tabName = CentralPersistWriterBase.getTableName(conn, null, Table.CENTRAL_SESSIONS, null, true);

			String orderByStr = " order by " + lq + "ServerName" + rq + ", " + lq + "SessionStartTime" + rq;
			if (orderByList != null && orderByList.length > 0)
			{
				StringUtil.toCommaStrQuoted(lq, rq, orderByList);
				orderByStr = " order by " + StringUtil.toCommaStrQuoted(lq, rq, orderByList);
			}
			
			String whereStr = "";
			if (status >= 0)
				whereStr = " where " + lq + "Status" + rq + " = " + status;
			
			String sql = "select "
						+ "  " + lq + "SessionStartTime"        + rq
						+ " ," + lq + "Status"                  + rq
						+ " ," + lq + "ServerName"              + rq
						+ " ," + lq + "ServerDisplayName"       + rq
						+ " ," + lq + "OnHostname"              + rq
						+ " ," + lq + "ProductString"           + rq
						+ " ," + lq + "VersionString"           + rq
						+ " ," + lq + "BuildString"             + rq
						+ " ," + lq + "CollectorHostname"       + rq
						+ " ," + lq + "CollectorSampleInterval" + rq
						+ " ," + lq + "CollectorCurrentUrl"     + rq
						+ " ," + lq + "CollectorInfoFile"       + rq
//						+ " ," + lq + "CollectorMgtHostname"    + rq
//						+ " ," + lq + "CollectorMgtPort"        + rq
//						+ " ," + lq + "CollectorMgtInfo"        + rq
						+ " ," + lq + "NumOfSamples"            + rq
						+ " ," + lq + "LastSampleTime"          + rq
					+" from " + tabName
					+ whereStr
					+ orderByStr;
			List<DbxCentralSessions> list = new ArrayList<>();

			Map<String, DbxCentralServerDescription> sdMap = new HashMap<>();
			try 
			{
				sdMap = DbxCentralServerDescription.getFromFile();
			}
			catch (IOException ex)
			{
				_logger.warn("Problems reading file '"+DbxCentralServerDescription.getDefaultFile()+"'. This is used to sort the 'sessions list'. Skipping this... Caught: "+ex);
			}
			
			// autoclose: stmnt, rs
			try (Statement stmnt = conn.createStatement())
			{
				// set TIMEOUT
				stmnt.setQueryTimeout(_defaultQueryTimeout);

				// Execute and read result
				try (ResultSet rs = stmnt.executeQuery(sql))
				{
					while (rs.next())
					{
						// Get: serverDescrption & serverExtraInfo
						String serverName       = rs.getString(3);
						String serverDescrption = "";
						String serverExtraInfo  = "";
						DbxCentralServerDescription sd = sdMap.get(serverName);
						if (sd != null)
						{
							serverDescrption = sd.getDescription();
							serverExtraInfo  = sd.getExtraInfo();
						}
						
						int c = 1;
						DbxCentralSessions s = new DbxCentralSessions(
							rs.getTimestamp(c++), // "SessionStartTime"       
							rs.getInt      (c++), // "Status"
							rs.getString   (c++), // "ServerName"             
							rs.getString   (c++), // "ServerDisplayName"      
							rs.getString   (c++), // "OnHostname"             
							rs.getString   (c++), // "ProductString"          
							rs.getString   (c++), // "VersionString"          
							rs.getString   (c++), // "BuildString"            
							rs.getString   (c++), // "CollectorHostname"      
							rs.getInt      (c++), // "CollectorSampleInterval"
							rs.getString   (c++), // "CollectorCurrentUrl"
							rs.getString   (c++), // "CollectorInfoFile"
//							rs.getString   (c++), // "CollectorMgtHostname"
//							rs.getInt      (c++), // "CollectorMgtPort"           
//							rs.getString   (c++), // "CollectorMgtInfo"
							rs.getInt      (c++), // "NumOfSamples"           
							rs.getTimestamp(c++), // "LastSampleTime"         
							serverDescrption,     // serverDescrption
							serverExtraInfo,      // serverExtraInfo
							null);                // graphProperties
						list.add(s);
					}
					if (onlyLast)
					{
						// The list is ordered by: ServerName, SessionStartTime   (using the SQL)
						// So just att stuff to the map and use the ones that are left at the end
						LinkedHashMap<String, DbxCentralSessions> map = new LinkedHashMap<>();
						for (DbxCentralSessions s : list)
							map.put(s.getServerName(), s);
						
						list.clear();
						list.addAll(map.values());
					}
				}
			}
			
			return list;
		}
		finally
		{
			releaseConnection(conn);
		}
	}

	/**
	 * Get a list of unique session names (a session is stored in a schema with the same name)
	 * @return
	 */
	public List<String> getLastSessionGraphs(String sessionName)
	throws SQLException
	{
		DbxConnection conn = getConnection(); // Get connection from a ConnectionPool
		try // block with: finally at end to return the connection to the ConnectionPool
		{
			String sql = "";
			String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
			String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection
			String tabName = CentralPersistWriterBase.getTableName(conn, null, Table.CENTRAL_SESSIONS, null, true);

			String sessionStartTime = null;
			sql = "select max("+lq+"SessionStartTime"+rq+") from "+tabName+" where "+lq+"ServerName"+rq+" = '"+sessionName+"'";

			// autoclose: stmnt, rs
			try (Statement stmnt = conn.createStatement())
			{
				// set TIMEOUT
				stmnt.setQueryTimeout(_defaultQueryTimeout);

				// Execute and read result
				try (ResultSet rs = stmnt.executeQuery(sql))
				{
					while (rs.next())
						sessionStartTime = rs.getString(1);
				}
			}
			
			tabName = CentralPersistWriterBase.getTableName(conn, sessionName, Table.GRAPH_PROPERTIES, null, true);
			sql = "select "+lq+"TableName"+rq+" from "+tabName+" where "+lq+"SessionStartTime"+rq+" = '"+sessionStartTime+"'";
			List<String> list = new ArrayList<>();

			// autoclose: stmnt, rs
			try (Statement stmnt = conn.createStatement())
			{
				// set TIMEOUT
				stmnt.setQueryTimeout(_defaultQueryTimeout);

				// Execute and read result
				try (ResultSet rs = stmnt.executeQuery(sql))
				{
					while (rs.next())
						list.add(rs.getString(1));
				}
			}
			
			return list;
		}
		finally
		{
			releaseConnection(conn);
		}
	}
	

	/**
	 * Update Graph Profile<br>
	 * @return
	 */
//	public List<DbxCentralProfile> setGraphProfile(DbxCentralProfile profile)
	public int setGraphProfile(DbxCentralProfile profile)
	throws SQLException
	{
		DbxConnection conn = getConnection(); // Get connection from a ConnectionPool
		try // block with: finally at end to return the connection to the ConnectionPool
		{
			String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
			String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection
			
			String tabName = CentralPersistWriterBase.getTableName(conn, null, Table.CENTRAL_GRAPH_PROFILES, null, true);

			// Table: DbxCentralGraphProfiles
			//    PK: ProductString, UserName, ProfileName

			String dbxProduct     = profile.getProductString();
			String dbxUser        = profile.getUserName();
			String dbxProfileName = profile.getProfileName();
			
			String sqlExists = 
				 " select 1"
				+" from " + tabName
				+" where " + lq + "ProductString" + rq + " = '" + dbxProduct     + "'"
				+"   and " + lq + "UserName"      + rq + " = '" + dbxUser        + "'"
				+"   and " + lq + "ProfileName"   + rq + " = '" + dbxProfileName + "'"
				;

			String sqlDelete = 
				 " delete from " + tabName
				+" where " + lq + "ProductString" + rq + " = '" + dbxProduct     + "'"
				+"   and " + lq + "UserName"      + rq + " = '" + dbxUser        + "'"
				+"   and " + lq + "ProfileName"   + rq + " = '" + dbxProfileName + "'"
				;

			String sqlUpdate = 
				 " update " + tabName
				+" set " + lq + "ProfileDescription" + rq + " = ?"
				+"    ," + lq + "ProfileValue"       + rq + " = ?"
				+"    ," + lq + "ProfileUrlOptions"  + rq + " = ?"
				+" where " + lq + "ProductString"    + rq + " = '" + dbxProduct     + "'"
				+"   and " + lq + "UserName"         + rq + " = '" + dbxUser        + "'"
				+"   and " + lq + "ProfileName"      + rq + " = '" + dbxProfileName + "'"
				;

			String[] insCols = new String[]{"ProductString", "UserName", "ProfileName", "ProfileDescription", "ProfileValue", "ProfileUrlOptions"};
//			String[] insVals = new String[]{profile.getProductString(), profile.getUserName(), profile.getProfileName(), profile.getProfileDescription(), profile.getProfileValue(), profile.getProfileUrlOptions()};
			String sqlInsert = 
				 " insert into " + tabName + " (" + StringUtil.toCommaStrQuoted(lq, rq, insCols) + ")"
//				+" values(" + StringUtil.toCommaStrQuoted(q, insVals) + ")"
				+" values(?, ?, ?, ?, ?, ?)"
				;

			boolean rowExists = false;
			// autoclose: stmnt, rs
			try (Statement stmnt = conn.createStatement())
			{
				// set TIMEOUT
				stmnt.setQueryTimeout(_defaultQueryTimeout);

				// Execute and read result
				try (ResultSet rs = stmnt.executeQuery(sqlExists))
				{
					while (rs.next())
						rowExists = true;
				}
			}

			_logger.debug("setGraphProfile(): EXISTS="+rowExists+": "+sqlExists);
			int rowCount = 0;
			if (rowExists)
			{
				// If profileValue is empty... remove the profile.
				String profileValue = profile.getProfileValue();
				_logger.debug("setGraphProfile(): profileValue: |"+profileValue+"|.");
				
				if (StringUtil.isNullOrBlank(profileValue) || (profileValue != null && "[]".equals(profileValue.trim())) )
				{
					try (Statement stmnt = conn.createStatement())
					{
						_logger.debug("setGraphProfile(): DELETE: "+sqlDelete);
						
						stmnt.executeUpdate(sqlDelete);
						rowCount = stmnt.getUpdateCount();
						if (rowCount != 1)
						{
							throw new SQLException("Problems deleting profile '"+dbxProfileName+"' for DbxProduct '"+dbxProduct+"'. rowcount="+rowCount+", expected rowcount was 1. "+profile);
						}
					}
				}
				else
				{
					_logger.debug("setGraphProfile(): UPDATE: "+sqlUpdate);
					
					try (PreparedStatement pstmnt = conn.prepareStatement(sqlUpdate))
					{
						pstmnt.setString(1, profile.getProfileDescription());
						pstmnt.setString(2, profile.getProfileValue());
						pstmnt.setString(3, profile.getProfileUrlOptions());
						
						pstmnt.executeUpdate();
						rowCount = pstmnt.getUpdateCount();
						if (rowCount != 1)
						{
							throw new SQLException("Problems updating profile '"+dbxProfileName+"' for DbxProduct '"+dbxProduct+"'. rowcount="+rowCount+", expected rowcount was 1. "+profile);
						}
					}
				}
			}
			else
			{
				_logger.debug("setGraphProfile(): INSERT: "+sqlInsert);
				
				// If profileValue is empty... Throw exception
				String profileValue = profile.getProfileValue();
				if (StringUtil.isNullOrBlank(profileValue) || (profileValue != null && "[]".equals(profileValue.trim())) )
				{
					throw new SQLException("The passed value for 'profileValue' is empty, this is NOT allowed. "+profile);
				}

				try (PreparedStatement pstmnt = conn.prepareStatement(sqlInsert))
				{
					pstmnt.setString(1, profile.getProductString());
					pstmnt.setString(2, profile.getUserName());
					pstmnt.setString(3, profile.getProfileName());
					pstmnt.setString(4, profile.getProfileDescription());
					pstmnt.setString(5, profile.getProfileValue());
					pstmnt.setString(6, profile.getProfileUrlOptions());
					
					pstmnt.executeUpdate();
					rowCount = pstmnt.getUpdateCount();
					if (rowCount != 1)
					{
						throw new SQLException("Problems inserting profile '"+dbxProfileName+"' for DbxProduct '"+dbxProduct+"'. rowcount="+rowCount+", expected rowcount was 1. "+profile);
					}
				}
			}
			return rowCount;
		}
		finally
		{
			releaseConnection(conn);
		}
	}
	
	/**
	 * Get Graph Profiles<br>
	 * @return
	 */
	public List<DbxCentralProfile> getGraphProfiles(String dbxTypeName, String user)
	throws SQLException
	{
		DbxConnection conn = getConnection(); // Get connection from a ConnectionPool
		try // block with: finally at end to return the connection to the ConnectionPool
		{
			String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
			String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection

			String tabName = CentralPersistWriterBase.getTableName(conn, null, Table.CENTRAL_GRAPH_PROFILES, null, true);

			String sql = "select "
					+ "  " + lq + "ProductString"      + rq
					+ " ," + lq + "UserName"           + rq
					+ " ," + lq + "ProfileName"        + rq
					+ " ," + lq + "ProfileDescription" + rq
					+ " ," + lq + "ProfileValue"       + rq
					+ " ," + lq + "ProfileUrlOptions"  + rq
				+" from " + tabName
				+" where 1 = 1"
				;

			if (StringUtil.hasValue(dbxTypeName))
				sql += "   and " + lq + "ProductString" + rq + " = '" + dbxTypeName + "'";

			if (StringUtil.hasValue(user))
				sql += "   and " + lq + "UserName" + rq + " = '" + user + "'";
			
			sql += " order by " + lq + "ProfileName" + rq;
			
			List<DbxCentralProfile> list = new ArrayList<>();

//System.out.println("getGraphProfiles: SQL=|"+sql+"|");
			// autoclose: stmnt, rs
			try (Statement stmnt = conn.createStatement())
			{
				// set TIMEOUT
				stmnt.setQueryTimeout(_defaultQueryTimeout);

				// Execute and read result
				try (ResultSet rs = stmnt.executeQuery(sql))
				{
					while (rs.next())
					{
						DbxCentralProfile s = new DbxCentralProfile(
							rs.getString   (1),
							rs.getString   (2),
							"", // profileType is decided by the constructor ("SYSTEM_SELECTED" or "USER_SELECTED") 
							rs.getString   (3),
							rs.getString   (4),
							rs.getString   (5),
							rs.getString   (6) );
						list.add(s);
					}
				}
			}
//System.out.println("getGraphProfiles: returns.list=|"+list+"|");

			
			// get ALL available graphs names avalilable for the different types of DbxTune collectors
			// 1 - System Selected Graphs for this DbxProduct
			// 2 - ALL Graphs available for this DbxProduct
//			addSystemProfiles(conn, list, true);  // onlySystemSelected = true
//			addSystemProfiles(conn, list, false); // onlySystemSelected = false
			
			return list;
		}
		finally
		{
			releaseConnection(conn);
		}
	}
	
	private void addSystemProfiles(DbxConnection conn, List<DbxCentralProfile> list, boolean onlySystemSelected)
	throws SQLException
	{
		String tabName;
		String sql;
//		String q = conn.getQuotedIdentifierChar();
		
		// get ALL available graphs names avalilable for the different types of DbxTune collectors
		// 1 - Get all DbxTune Collectors
		// 2 - for each server and collector, get GraphNames... save only the server/dbxTune with the most graphs
		tabName = CentralPersistWriterBase.getTableName(conn, null, Table.CENTRAL_SESSIONS, null, true);
		sql = "select distinct [ServerName], [ProductString] from " + tabName + " order by 2, 1";
		sql = conn.quotifySqlString(sql);
		Map<String, String> dbxSrvProductMap = new LinkedHashMap<>();

		// autoclose: stmnt, rs
		try (Statement stmnt = conn.createStatement())
		{
			// set TIMEOUT
			stmnt.setQueryTimeout(_defaultQueryTimeout);

			// Execute and read result
			try (ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
					dbxSrvProductMap.put(rs.getString(1), rs.getString(2));
			}
		}

		// Save a list of graph names for each DbxProduct (only the largest list for each DbxProduct is saved)
		Map<String, List<String>> dbxProductMapAll = new LinkedHashMap<>();
		for (String srvName : dbxSrvProductMap.keySet())
		{
			String dbxProduct = dbxSrvProductMap.get(srvName);
			tabName = CentralPersistWriterBase.getTableName(conn, srvName, Table.GRAPH_PROPERTIES, null, true);
			String whereVisibleAtStart = "";
			if (onlySystemSelected)
				whereVisibleAtStart = " and [visibleAtStart] = 1 ";
			sql = "select [TableName] from " + tabName + " where [SessionStartTime] = (select max([SessionStartTime]) from " + tabName + ") " + whereVisibleAtStart + " order by [initialOrder]";
			sql = conn.quotifySqlString(sql);
					
			List<String> graphNameList = new ArrayList<>();

			// autoclose: stmnt, rs
			try (Statement stmnt = conn.createStatement())
			{
				// set TIMEOUT
				stmnt.setQueryTimeout(_defaultQueryTimeout);

				// Execute and read result
				try (ResultSet rs = stmnt.executeQuery(sql))
				{
					while (rs.next())
						graphNameList.add(rs.getString(1));
				}
			}

			List<String> existingList = dbxProductMapAll.get(dbxProduct);
			if (existingList != null)
			{
				if (graphNameList.size() > existingList.size())
					dbxProductMapAll.put(dbxProduct, graphNameList);
			}
			else
				dbxProductMapAll.put(dbxProduct, graphNameList);
		}

		// Save a list of graph names for each DbxProduct (only the largest list for each DbxProduct is saved)
		for (String dbxProduct : dbxProductMapAll.keySet())
		{
			// If the record "TYPE_SYSTEM_SELECTED" is already pressent in the list... DO Not add it...
			boolean addRecord = true;
			if (onlySystemSelected)
			{
				for (DbxCentralProfile entry : list)
				{
					if ( entry.getProductString().equals(dbxProduct) && entry.getProfileType().equals(DbxCentralProfile.TYPE_SYSTEM_SELECTED))
						addRecord = false;
				}
			}

			if (addRecord)
			{
				List<String> graphList = dbxProductMapAll.get(dbxProduct);
				StringBuilder sb = new StringBuilder();
				
				sb.append("[");
				for (String entry : graphList)
				{
					// append a comma (but not at first iteration)
					if (sb.length() > 1)
						sb.append(",");

					// {"graph":"value"}
					sb.append("{\"graph\":\"").append(entry).append("\"}");
				}
				sb.append("]");
				
				DbxCentralProfile s = new DbxCentralProfile(
						dbxProduct, 
						"", // userName
						onlySystemSelected ? DbxCentralProfile.TYPE_SYSTEM_SELECTED : DbxCentralProfile.TYPE_SYSTEM_ALL, 
						"", // profileName
						"", // profileDescription
						sb.toString(), 
						""); // profileUrlOptions
				list.add(s);
			}
		}
	}
	
	/**
	 * Get Graph Profile<br>
	 * <ul>
	 *   <li>First try to get the profile name (with current user, and then 'default' user)</li>
	 *   <li>Fallback: is to get use the 'serverName' from sessions and get the default profile for the DbxTuneType (AseTune|SqlServerTune|oraTune|...)</li>
	 *   <li>Last Fallback: get profile with default user, and profile</li>
	 * </ul>
	 * @param name  This could be a serverName or a profileName
	 * @param user  a specific user (can be blank)
	 * @return
	 */
	public String getGraphProfile(String name, String user)
	throws SQLException
	{
		String result = "";
		
		if (user == null)
			user = "";

		// Get with: profileName and currentUser
		result = privateGetGraphProfile("", name, user);
		if (StringUtil.hasValue(result))
			return result;

		// If no-result and 'user' was specified, try with the "default" user
		result = privateGetGraphProfile("", name, ""); // "" = default
		if (StringUtil.hasValue(result))
			return result;

		// Still no-reult get the DbxTune type from servers...
		String dbxType = getDbxTypeForServerName(name);
		
		result = privateGetGraphProfile(dbxType, name, user);
		if (StringUtil.hasValue(result))
			return result;

		// If no-result and 'user' was specified, try with the "default" user
		result = privateGetGraphProfile(dbxType, name, ""); // "" = default
		if (StringUtil.hasValue(result))
			return result;

		// If no-result and 'user' was specified, try with the "default" user and profile
		result = privateGetGraphProfile(dbxType, "", ""); // "" = default
		if (StringUtil.hasValue(result))
			return result;

		return result;
	}

	private String privateGetGraphProfile(String dbxTypeName, String name, String user)
	throws SQLException
	{
		DbxConnection conn = getConnection(); // Get connection from a ConnectionPool
		try // block with: finally at end to return the connection to the ConnectionPool
		{
			String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
			String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection

			String tabName = CentralPersistWriterBase.getTableName(conn, null, Table.CENTRAL_GRAPH_PROFILES, null, true);

			String sql = "select " + lq + "ProfileValue" + rq
					+" from " + tabName
					+" where " + lq + "ProfileName" + rq + " = '" + name + "'"
					+"   and " + lq + "UserName"    + rq + " = '" + user + "'"
					;
			if (StringUtil.hasValue(dbxTypeName))
				sql += "   and " + lq + "ProductString" + rq + " = '" + dbxTypeName + "'";
			
			String result = "";

			// autoclose: stmnt, rs
			try (Statement stmnt = conn.createStatement())
			{
				// set TIMEOUT
				stmnt.setQueryTimeout(_defaultQueryTimeout);

				// Execute and read result
				try (ResultSet rs = stmnt.executeQuery(sql))
				{
					while (rs.next())
					{
						result = rs.getString(1);
					}
				}
			}
//			System.out.println("privateGetGraphProfile(dbxTypeName=|"+dbxTypeName+"|, name=|"+name+"|, user=|"+user+"|): sql=|"+sql+"|      <<<<<<<< result=|"+result+"|.");
			
			return result;
		}
		finally
		{
			releaseConnection(conn);
		}
	}

	private String getDbxTypeForServerName(String name)
	throws SQLException
	{
		DbxConnection conn = getConnection(); // Get connection from a ConnectionPool
		try // block with: finally at end to return the connection to the ConnectionPool
		{
			String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
			String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection

			String tabName = CentralPersistWriterBase.getTableName(conn, null, Table.CENTRAL_SESSIONS, null, true);

			String sql = "select " + lq + "ProductString" + rq
					+" from " + tabName
					+" where " + lq + "ServerName"       + rq + " = '" + name + "'"
					+"   and " + lq + "SessionStartTime" + rq + " = (select max("+lq+"SessionStartTime"+rq+") from "+tabName+" where "+lq+"ServerName"+rq+" = '"+name+"')"
					;

			String result = "";

			// autoclose: stmnt, rs
			try (Statement stmnt = conn.createStatement())
			{
				// set TIMEOUT
				stmnt.setQueryTimeout(_defaultQueryTimeout);

				// Execute and read result
				try (ResultSet rs = stmnt.executeQuery(sql))
				{
					while (rs.next())
					{
						result = rs.getString(1);
					}
				}
			}
			
			return result;
		}
		finally
		{
			releaseConnection(conn);
		}
	}
	


	/**
	 * Get Graph Properties
	 * @param sessionName       Name of the session (the schema name)
	 * @param sessionStartTime  StartTime (if null = last session)
	 * @return
	 */
	public List<DbxGraphProperties> getGraphProperties(String sessionName, String sessionStartTime)
	throws SQLException
	{
		DbxConnection conn = getConnection(); // Get connection from a ConnectionPool
		try // block with: finally at end to return the connection to the ConnectionPool
		{
			String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
			String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection

			String tabName = CentralPersistWriterBase.getTableName(conn, sessionName, Table.GRAPH_PROPERTIES, null, true);

			// Build a where clause
			String whereSessionStartTime = " (select max(" + lq + "SessionStartTime" + rq + ") from " + tabName + ")";
			if (StringUtil.hasValue(sessionStartTime))
				whereSessionStartTime = "'" + sessionStartTime + "'";
			
			String sql = "select "
						+ "  " + lq + "SessionStartTime" + rq
						+ " ," + lq + "CmName"           + rq
						+ " ," + lq + "GraphName"        + rq
						+ " ," + lq + "TableName"        + rq
						+ " ," + lq + "GraphLabel"       + rq
						+ " ," + lq + "GraphProps"       + rq
						+ " ," + lq + "GraphCategory"    + rq
						+ " ," + lq + "isPercentGraph"   + rq
						+ " ," + lq + "visibleAtStart"   + rq
						+ " ," + lq + "initialOrder"     + rq
					+" from " + tabName
					+" where " + lq + "SessionStartTime" + rq + " = " + whereSessionStartTime
			//		+"   and " + lq + "visibleAtStart"   + rq + " = 1"
			//		+"   and " + lq + "CmName"    + rq + " = 'CmSummary' " 
			//		+"   and " + lq + "GraphName" + rq + " like 'aa%' " 
					+" order by " + lq + "initialOrder" + rq
					;

			List<DbxGraphProperties> list = new ArrayList<>();

			// autoclose: stmnt, rs
			try (Statement stmnt = conn.createStatement())
			{
				// set TIMEOUT
				stmnt.setQueryTimeout(_defaultQueryTimeout);

				// Execute and read result
				try (ResultSet rs = stmnt.executeQuery(sql))
				{
					while (rs.next())
					{
						DbxGraphProperties e = new DbxGraphProperties(
							rs.getTimestamp(1),  //SessionStartTime
							sessionName,
							rs.getString   (2),   // CmName
							rs.getString   (3),   // GraphName
							rs.getString   (4),   // TableName
							rs.getString   (5),   // GraphLabel
							rs.getString   (6),   // GraphProps
							rs.getString   (7),   // GraphCategory
							rs.getBoolean  (8),   // isPercent
							rs.getBoolean  (9),   // visiableAtStart
							rs.getInt      (10)); // initialOrder
						list.add(e);
					}
				}
			}
			
			return list;
		}
		finally
		{
			releaseConnection(conn);
		}
	}

	
//	/**
//	 * Get Graph Descriptions
//	 * 
//	 * @return
//	 */
//	public List<DbxGraphDescription> getGraphDescriptions()
//	throws SQLException
//	{
//		// Get sessions
//		List<DbxCentralSessions> sessions = getSessions(true, -1);
//		
//		DbxConnection conn = getConnection(); // Get connection from a ConnectionPool
//		try // block with: finally at end to return the connection to the ConnectionPool
//		{
//			String q = conn.getQuotedIdentifierChar();
//			List<DbxGraphDescription> list = new ArrayList<>();
//			Map<String, DbxGraphDescription> map = new LinkedHashMap<>();
//
//			for (DbxCentralSessions session : sessions)
//			{
//				String tabName = CentralPersistWriterBase.getTableName(session.getServerName(), Table.GRAPH_PROPERTIES, null, true);
//				
//				// Build a where clause
//				Timestamp sessionStartTime = session.getSessionStartTime();
//				String whereSessionStartTime = " (select max(" + q + "SessionStartTime" + q + ") from " + tabName + ")";
//				if (sessionStartTime != null)
//					whereSessionStartTime = "'" + sessionStartTime + "'";
//				
//				String sql = "select "
//							+ "  " + q + "CmName"           + q
//							+ " ," + q + "GraphName"        + q
//							+ " ," + q + "TableName"        + q
//							+ " ," + q + "GraphLabel"       + q
//							+ " ," + q + "GraphCategory"    + q
//							+ " ," + q + "isPercentGraph"   + q
//							+ " ," + q + "visibleAtStart"   + q
//							+ " ," + q + "initialOrder"     + q
//						+" from " + tabName
//						+" where " + q + "SessionStartTime" + q + " = " + whereSessionStartTime
//						+" order by " + q + "initialOrder" + q
//						;
//
//				// autoclose: stmnt, rs
//				try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
//				{
//					String dbxProduct = session.getProductString();
//					
//					while (rs.next())
//					{
//						String       cmName           = rs.getString   (1);        
//						String       graphName        = rs.getString   (2);         
//						String       tableName        = rs.getString   (3);         
//						String       graphLabel       = rs.getString   (4);         
//						String       graphCategory    = rs.getString   (5);         
//						boolean      isPercentGraph   = rs.getBoolean  (6);         
//						boolean      visibleAtStartup = rs.getBoolean  (7);         
//						int          initialOrder     = rs.getInt      (8);        
//
//						String key = tableName + "|" + dbxProduct;
//						
//						DbxGraphDescription ce = map.get(key);
//						if (ce != null)
//						{
//							List<String> srvList = ce.getServerNameList();
//							if (srvList == null)
//								srvList = new ArrayList<String>();
//
//							if ( ! srvList.contains(session.getServerName()) ) // I think this is unnececarry, it should already be unique entries...
//								srvList.add(session.getServerName());
//						}
//						else
//						{
//							List<String> srvList = new ArrayList<String>();
//							srvList.add(session.getServerName());
//							DbxGraphDescription e = new DbxGraphDescription(srvList, dbxProduct, cmName, graphName, tableName, graphLabel, graphCategory, isPercentGraph, visibleAtStartup, initialOrder);
//							map.put(key, e);
//						}
//					}
//				}
//			}
//
//			// Add the Map values to the return list
//			list.addAll(map.values());
//
//			// Should we traverse the map and make new "initialOrder" depending on the current order???
//			// or should we keep "initialOrder" even that it might be wrong...
//			if (true && ! list.isEmpty() )
//			{
//				int initialOrder = 0;
//				String curDbxProduct = "dummyEntry";
//				for (DbxGraphDescription entry : list)
//				{
//					if ( ! curDbxProduct.equals(entry.getDbxProduct()) )
//					{
//						curDbxProduct = entry.getDbxProduct();
//						initialOrder = 0;
//					}
//					
//					entry.setInitialOrder(initialOrder);
//					initialOrder++;
//				}
//			}
//			
//			return list;
//		}
//		finally
//		{
//			releaseConnection(conn);
//		}
//	}
//	/**
//	 * Get Graph Descriptions
//	 * 
//	 * @return
//	 */
//	public Map<String, List<DbxGraphDescription>> getGraphDescriptions()
//	throws SQLException
//	{
//		// Get sessions
//		List<DbxCentralSessions> sessions = getSessions(true, -1);
//		
//		DbxConnection conn = getConnection(); // Get connection from a ConnectionPool
//		try // block with: finally at end to return the connection to the ConnectionPool
//		{
//			String q = conn.getQuotedIdentifierChar();
//			Map<String, List<DbxGraphDescription>> returnMap = new LinkedHashMap<>();
//
////			List<DbxGraphDescription> list = new ArrayList<>();
//			Map<String, List<DbxGraphDescription>> srvDescMap = new LinkedHashMap<>();
//
//			// For each session: get all records
//			// put them in a Map<srvName, List<GraphDescriptions>>
//			// Next step: "merge them together, starting with the server with most entries"
//			for (DbxCentralSessions session : sessions)
//			{
//				String dbxProduct = session.getProductString();
//				String srvName    = session.getServerName();
//				String tabName    = CentralPersistWriterBase.getTableName(srvName, Table.GRAPH_PROPERTIES, null, true);
//				
//				// Build a where clause
//				Timestamp sessionStartTime = session.getSessionStartTime();
//				String whereSessionStartTime = " (select max(" + q + "SessionStartTime" + q + ") from " + tabName + ")";
//				if (sessionStartTime != null)
//					whereSessionStartTime = "'" + sessionStartTime + "'";
//				
//				String sql = "select "
//							+ "  " + q + "CmName"           + q
//							+ " ," + q + "GraphName"        + q
//							+ " ," + q + "TableName"        + q
//							+ " ," + q + "GraphLabel"       + q
//							+ " ," + q + "GraphCategory"    + q
//							+ " ," + q + "isPercentGraph"   + q
//							+ " ," + q + "visibleAtStart"   + q
//							+ " ," + q + "initialOrder"     + q
//						+" from " + tabName
//						+" where " + q + "SessionStartTime" + q + " = " + whereSessionStartTime
//						+" order by " + q + "initialOrder" + q
//						;
//
//				// autoclose: stmnt, rs
//				try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
//				{
//					while (rs.next())
//					{
//						String       cmName           = rs.getString   (1);        
//						String       graphName        = rs.getString   (2);         
//						String       tableName        = rs.getString   (3);         
//						String       graphLabel       = rs.getString   (4);         
//						String       graphCategory    = rs.getString   (5);         
//						boolean      isPercentGraph   = rs.getBoolean  (6);         
//						boolean      visibleAtStartup = rs.getBoolean  (7);         
//						int          initialOrder     = rs.getInt      (8);        
//
//						List<DbxGraphDescription> srvDescList = srvDescMap.get(srvName);
//						if (srvDescList == null)
//							srvDescList = new ArrayList<>(); 
//
//						DbxGraphDescription e = new DbxGraphDescription(null, dbxProduct, cmName, graphName, tableName, graphLabel, graphCategory, isPercentGraph, visibleAtStartup, initialOrder);
//						srvDescList.add(e);
//					}
//				}
//			} // end: foreach session
//			
//			// now, on the serverLevel: merge the entries together (starting with the server with most entries, in each DbxProduct)
//			Map<String, String> dbxSrvMaxMap = new LinkedHashMap<>(); // key=DbxProduct, val=SrvName
//			for (Entry<String, List<DbxGraphDescription>> srvEntry : srvDescMap.entrySet())
//			{
//				String                    srvName     = srvEntry.getKey();
//				List<DbxGraphDescription> srvDescList = srvEntry.getValue();
//
//				String dbxProduct = srvDescList.get(0).getDbxProduct();
//				
//				List<DbxGraphDescription> list = dbxSrvMaxMap.
//				List<DbxGraphDescription> srvDescList = srvDescMap.get(srvName);
//				
//			}
//
//			// Add the Map values to the return list
//			list.addAll(map.values());
//
//			// Should we traverse the map and make new "initialOrder" depending on the current order???
//			// or should we keep "initialOrder" even that it might be wrong...
//			if (true && ! list.isEmpty() )
//			{
//				int initialOrder = 0;
//				String curDbxProduct = "dummyEntry";
//				for (DbxGraphDescription entry : list)
//				{
//					if ( ! curDbxProduct.equals(entry.getDbxProduct()) )
//					{
//						curDbxProduct = entry.getDbxProduct();
//						initialOrder = 0;
//					}
//					
//					entry.setInitialOrder(initialOrder);
//					initialOrder++;
//				}
//			}
//			
//			return list;
//		}
//		finally
//		{
//			releaseConnection(conn);
//		}
//	}
	/**
	 * Get Graph Descriptions
	 * 
	 * @return map<key=DbxProduct, val=map< key=srvName, val=description >>
	 */
	public Map<String, Map<String, List<DbxGraphDescription>>> getGraphDescriptions()
	throws SQLException
	{
		// Get sessions
		List<DbxCentralSessions> sessions = getSessions(true, -1);

		// Get System Supplied Graph Profiles
		List<DbxCentralProfile> cProfileList = getGraphProfiles(null, null);
		
		DbxConnection conn = getConnection(); // Get connection from a ConnectionPool
		try // block with: finally at end to return the connection to the ConnectionPool
		{
			String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
			String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection

			Map<String, Map<String, List<DbxGraphDescription>>> prodSrvDescMap = new LinkedHashMap<>();

//			List<DbxGraphDescription> list = new ArrayList<>();
//			Map<String, List<DbxGraphDescription>> srvDescMap = new LinkedHashMap<>();

			// For each session: get all records
			for (DbxCentralSessions session : sessions)
			{
				String dbxProduct = session.getProductString();
				String srvName    = session.getServerName();
				String tabName    = CentralPersistWriterBase.getTableName(conn, srvName, Table.GRAPH_PROPERTIES, null, true);

				// add DbxProduct to outer map (if not already there)
				Map<String, List<DbxGraphDescription>> srvDescMap = prodSrvDescMap.get(dbxProduct);
				if (srvDescMap == null)
				{
					srvDescMap = new LinkedHashMap<>();
					prodSrvDescMap.put(dbxProduct, srvDescMap);
				}
				
				// Build a where clause
				Timestamp sessionStartTime = session.getSessionStartTime();
				String whereSessionStartTime = " (select max(" + lq + "SessionStartTime" + rq + ") from " + tabName + ")";
				if (sessionStartTime != null)
					whereSessionStartTime = "'" + sessionStartTime + "'";
				
				String sql = "select "
							+ "  " + lq + "CmName"           + rq
							+ " ," + lq + "GraphName"        + rq
							+ " ," + lq + "TableName"        + rq
							+ " ," + lq + "GraphLabel"       + rq
							+ " ," + lq + "GraphProps"       + rq
							+ " ," + lq + "GraphCategory"    + rq
							+ " ," + lq + "isPercentGraph"   + rq
							+ " ," + lq + "visibleAtStart"   + rq
							+ " ," + lq + "initialOrder"     + rq
						+" from " + tabName
						+" where "    + lq + "SessionStartTime" + rq + " = " + whereSessionStartTime
						+" order by " + lq + "initialOrder" + rq
						;

				// autoclose: stmnt, rs
				try (Statement stmnt = conn.createStatement())
				{
					// set TIMEOUT
					stmnt.setQueryTimeout(_defaultQueryTimeout);

					// Execute and read result
					try (ResultSet rs = stmnt.executeQuery(sql))
					{
						while (rs.next())
						{
							String       cmName           = rs.getString   (1);        
							String       graphName        = rs.getString   (2);         
							String       tableName        = rs.getString   (3);         
							String       graphLabel       = rs.getString   (4);         
							String       graphProps       = rs.getString   (5);         
							String       graphCategory    = rs.getString   (6);         
							boolean      isPercentGraph   = rs.getBoolean  (7);         
							boolean      visibleAtStartup = rs.getBoolean  (8);         
							int          initialOrder     = rs.getInt      (9);        
	
							List<DbxGraphDescription> srvDescList = srvDescMap.get(srvName);
							if (srvDescList == null)
							{
								srvDescList = new ArrayList<>(); 
								srvDescMap.put(srvName, srvDescList);
							}
	
							DbxGraphDescription e = new DbxGraphDescription(null, dbxProduct, cmName, graphName, tableName, graphLabel, graphProps, graphCategory, isPercentGraph, visibleAtStartup, initialOrder);
							srvDescList.add(e);
							//System.out.println("getGraphDescriptions(): srvName='"+srvName+"': "+e);
						}
					}
				}
			} // end: foreach session
			
			// Add an extra srvName "ALL" for each DbxProduct
			// That entry will contian all graphs for all servers
			for (Entry<String, Map<String, List<DbxGraphDescription>>> dbxProds : prodSrvDescMap.entrySet())
			{
				String dbxProd = dbxProds.getKey();
				Map<String, List<DbxGraphDescription>> srvMap  = dbxProds.getValue();

				String maxSrvName    = "";
				int    maxSrvNameCnt = 0;
				for (Entry<String, List<DbxGraphDescription>> srvEntry : srvMap.entrySet())
				{
					String srvName = srvEntry.getKey();
					List<DbxGraphDescription> descList = srvEntry.getValue();
					
					if (descList.size() >= maxSrvNameCnt)
					{
						maxSrvName    = srvName;
						maxSrvNameCnt = descList.size();
					}
				}
				
//				// In the srvMap add entry "ALL", with a DEEP COPY of the SrvName with the most graph description entries
				List<DbxGraphDescription> AllDescList = new ArrayList<>();
				for (DbxGraphDescription gd : srvMap.get(maxSrvName))
					AllDescList.add( new DbxGraphDescription(gd) );

				// ssp = System Selected Profile
				List<DbxGraphDescription> sspDescList = new ArrayList<>();

				List<String> sspGraphNames = new ArrayList<>();
				for (DbxCentralProfile cp : cProfileList)
				{
					if (dbxProd.equals(cp.getProductString()) && DbxCentralProfile.TYPE_SYSTEM_SELECTED.equals(cp.getProfileType()))
					{
						// parse json "profileValue": [ {"graph": "CmSummary_aaCpuGraph"}, {"graph": "CmSummary_aaReadWriteGraph"}...]'
						// into List: CmSummary_aaCpuGraph, CmSummary_aaReadWriteGraph...
						String json = cp.getProfileValue();
						ObjectMapper mapper = new ObjectMapper();
						try
						{
							JsonNode root = mapper.readTree(json);
							//System.out.println("JSON: dbxProd=|"+dbxProd+"|, str=|"+json+"|, isArray="+root.isArray());
							if (root.isArray())
							{
								for (JsonNode node : root)
								{
									JsonNode graph = node.get("graph");
									String fullName = graph.textValue();

									//System.out.println("     JSON: fullName=|"+fullName+"|");
									sspGraphNames.add(fullName);
								}
							}
						}
						catch (IOException e)
						{
							_logger.warn("Problems parsing System Profile for '"+dbxProd+"', Caught: "+e, e);
						}
					}
				}
				//System.out.println("JSON: dbxProd=|"+dbxProd+"|, sspGraphNames.size()="+sspGraphNames.size()+", sspGraphNames=|"+sspGraphNames+"|.");

				if ( sspGraphNames.isEmpty() )
				{
					for (DbxGraphDescription gd : srvMap.get(maxSrvName))
					{
						if (gd.isVisibleAtStartup())
							sspDescList.add( new DbxGraphDescription(gd) );
					}
				}
				else
				{
					for (String graphFullName : sspGraphNames)
					{
						for (DbxGraphDescription gd : srvMap.get(maxSrvName))
						{
							if (gd.getTableName().equals(graphFullName))
							{
								sspDescList.add( new DbxGraphDescription(gd) );
								break; // continue with next: graphFullName
							}
						}
					}
				}


				// insert all Graphs (that is not already part of ALL) from all servers
				for (Entry<String, List<DbxGraphDescription>> srvEntry : srvMap.entrySet())
				{
					String srvName = srvEntry.getKey();
					List<DbxGraphDescription> descList = srvEntry.getValue();

					for (DbxGraphDescription gd : descList)
					{
						boolean exists = false;
						String fullName = gd.getTableName();  
						for (DbxGraphDescription allEntry : AllDescList)
						{
							if (fullName.equals(allEntry.getTableName()))
							{
								allEntry.addServerName(srvName);
								exists = true;
								break;
							}
						}
						if ( ! exists )
						{
							DbxGraphDescription newEntry = new DbxGraphDescription(gd);
							newEntry.addServerName(srvName);
							AllDescList.add( newEntry );
						}
					}
				}
				
				// Make new "initialOrder" depending on the current order???
				if ( ! AllDescList.isEmpty() )
				{
					int initialOrder = 0;
					for (DbxGraphDescription entry : AllDescList)
					{
						entry.setInitialOrder(initialOrder);
						initialOrder++;
					}
				}

				// Make new "initialOrder" depending on the current order???
				if ( ! sspDescList.isEmpty() )
				{
					int initialOrder = 0;
					for (DbxGraphDescription entry : sspDescList)
					{
						entry.setInitialOrder(initialOrder);
						initialOrder++;
					}
				}

				// Finally add "ALL" to the srvMap
//				srvMap.put("-ALL-", AllDescList);
//				srvMap.put("-SSP-", sspDescList); 
				srvMap.put("_ALL_", AllDescList);
				srvMap.put("_SSP_", sspDescList); 
			}
			
			return prodSrvDescMap;
		}
		finally
		{
			releaseConnection(conn);
		}
	}



	public DbxCentralUser getDbxCentralUser(String username)
	throws SQLException
	{
		DbxConnection conn = getConnection(); // Get connection from a ConnectionPool
		try // block with: finally at end to return the connection to the ConnectionPool
		{
//			DbxCentralUser xxx = new DbxCentralUser(username, username, "", "admin");
//			return xxx;
			
			String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
			String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection

			String tabName = CentralPersistWriterBase.getTableName(conn, null, Table.CENTRAL_USERS, null, true);

			// Build SQL
			String sql = "select "
						+ "  " + lq + "UserName" + rq
						+ " ," + lq + "Password" + rq
						+ " ," + lq + "Email"    + rq
						+ " ," + lq + "Roles"    + rq
					+" from " + tabName
					+" where " + lq + "UserName" + rq + " = '" + username + "'"
					;

			DbxCentralUser user = null;
			
			// autoclose: stmnt, rs
			try (Statement stmnt = conn.createStatement())
			{
				// set TIMEOUT
				stmnt.setQueryTimeout(_defaultQueryTimeout);

				// Execute and read result
				try (ResultSet rs = stmnt.executeQuery(sql))
				{
					while (rs.next())
					{
						user = new DbxCentralUser(
								rs.getString   (1), // UserName
								rs.getString   (2), // Password
								rs.getString   (3), // Email
								rs.getString   (4)  // Roles
								);
					}
				}
			}
			
			return user;
		}
		finally
		{
			releaseConnection(conn);
		}
	}





	public List<DsrSkipEntry> getDsrSkipEntries(String srvName, String className, String entryType)
	throws SQLException
	{
		DbxConnection conn = getConnection(); // Get connection from a ConnectionPool
		try // block with: finally at end to return the connection to the ConnectionPool
		{
			String tabName = CentralPersistWriterBase.getTableName(conn, null, Table.DSR_SKIP_ENTRIES, null, false);

			String andSrvName   = StringUtil.isNullOrBlank(srvName)   ? "" : "   and [SrvName]   = " + DbUtils.safeStr(srvName  ) + " \n";
			String andClassName = StringUtil.isNullOrBlank(className) ? "" : "   and [ClassName] = " + DbUtils.safeStr(className) + " \n";
			String andEntryType = StringUtil.isNullOrBlank(entryType) ? "" : "   and [EntryType] = " + DbUtils.safeStr(entryType) + " \n";

			// Build SQL
			String sql = 
					  "select \n"
					+ "  [SrvName] \n"       
					+ " ,[ClassName] \n"     
					+ " ,[EntryType] \n"     
					+ " ,[StringVal] \n"     
					+ " ,[Description] \n"     
					+ " ,[SqlTextExample] \n"
					+" from [" + tabName + "] \n"
					+" where 1 = 1 \n"
					+andSrvName
					+andClassName
					+andEntryType
					+ "order by [SrvName], [ClassName], [EntryType], [StringVal]"
					;
			
			sql = conn.quotifySqlString(sql);
			
			if (_logger.isDebugEnabled())
				_logger.debug("getDsrSkipEntries(srvName='" + srvName + "', className='" + className + "', entryType='" + entryType + "'): SQL=" + sql);

			List<DsrSkipEntry> list = new ArrayList<>();
			
			// autoclose: stmnt, rs
			try (Statement stmnt = conn.createStatement())
			{
				// set TIMEOUT
				stmnt.setQueryTimeout(_defaultQueryTimeout);

				// Execute and read result
				try (ResultSet rs = stmnt.executeQuery(sql))
				{
					while (rs.next())
					{
						DsrSkipEntry entry = new DsrSkipEntry(
								rs.getString   (1), // SrvName
								rs.getString   (2), // ClassName
								rs.getString   (3), // EntryType
								rs.getString   (4), // StringVal
								rs.getString   (5), // Description
								rs.getString   (6)  // SqlTextExample
								);
						
						list.add(entry);
					}
				}
			}
			
			return list;
		}
		finally
		{
			releaseConnection(conn);
		}
	}

	public void addDsrSkipEntry(DsrSkipEntry entry) 
	throws SQLException
	{
		DbxConnection conn = getConnection(); // Get connection from a ConnectionPool
		try // block with: finally at end to return the connection to the ConnectionPool
		{
			// Get a writer (from which we get the INSERT SQL Statement)
			CentralPersistWriterBase writer = null;
			for (ICentralPersistWriter pw : CentralPcsWriterHandler.getInstance().getWriters())
			{
				if (pw instanceof CentralPersistWriterBase)
				{
					writer = (CentralPersistWriterBase) pw;
				}
			}
			if (writer == null)
				throw new SQLException("No CentralPersistWriter was found.");

			// Get the INSERT Statement
			String sql = writer.getTableInsertStr(conn, null, Table.DSR_SKIP_ENTRIES, null, true);
			
			// autoclose: pstmnt
			try (PreparedStatement pstmnt = conn.prepareStatement(sql))
			{
				// set TIMEOUT
				pstmnt.setQueryTimeout(_defaultQueryTimeout);

				pstmnt.setString(1, entry.getSrvName());
				pstmnt.setString(2, entry.getClassName());
				pstmnt.setString(3, entry.getEntryType());
				pstmnt.setString(4, entry.getStringVal());
				pstmnt.setString(5, entry.getDescription());
				pstmnt.setString(6, entry.getSqlTextExample());

				pstmnt.execute();
			}
		}
		finally
		{
			releaseConnection(conn);
		}
	}

	public int removeDsrSkipEntry(String srvName, String className, String entryType, String stringVal)
	throws SQLException
	{
		DbxConnection conn = getConnection(); // Get connection from a ConnectionPool
		try // block with: finally at end to return the connection to the ConnectionPool
		{
			String tabName = CentralPersistWriterBase.getTableName(conn, null, Table.DSR_SKIP_ENTRIES, null, false);

			// Build SQL
			String sql = "delete \n"
					+" from [" + tabName + "] \n"
					+" where 1 = 1 \n"
					+"   and [SrvName]   = " + DbUtils.safeStr( srvName   ) + " \n"
					+"   and [ClassName] = " + DbUtils.safeStr( className ) + " \n"
					+"   and [EntryType] = " + DbUtils.safeStr( entryType ) + " \n"
					+"   and [StringVal] = " + DbUtils.safeStr( stringVal ) + " \n"
					;
			
			sql = conn.quotifySqlString(sql);

			// autoclose: stmnt, rs
			try (Statement stmnt = conn.createStatement())
			{
				// set TIMEOUT
				stmnt.setQueryTimeout(_defaultQueryTimeout);

				stmnt.executeUpdate(sql);
				
				return stmnt.getUpdateCount();
			}
		}
		finally
		{
			releaseConnection(conn);
		}
	}


	public int removeDsrSkipEntry(DsrSkipEntry entry) 
	throws SQLException
	{
		return removeDsrSkipEntry(entry.getSrvName(), entry.getClassName(), entry.getEntryType(), entry.getStringVal());
	}






	/**
	 * Get graph data
	 * 
	 * @param sessionName
	 * @param cmName        Name of the CM to get data for
	 * @param graphName     Name of the Graph to get data for
	 * @param startTime     Can be a 'iso8601' or '#m|#h|#d' (Minutes, Hours or Day)
	 * @param endTime       Can be a 'iso8601' or '#m|#h|#d' (Minutes, Hours or Day) if startTime is 'date' then #mhd is added to startTime
	 * @param sampleType    Used specify what method we want to use for reading data
	 * @param sampleValue   The associated value for the above sampleType
	 * @param autoOverflow  If we use sampleType.AUTO then if we have to many "data entries", lets fallback to this sampleType
	 * 
	 * @return A List of entries
	 */
	public List<DbxGraphData> getGraphData(String sessionName, String cmName, String graphName, String startTime, String endTime, SampleType sampleType, int sampleValue, SampleType autoOverflow)
	throws SQLException
	{
		DbxConnection conn = getConnection(); // Get connection from a ConnectionPool
		try // block with: finally at end to return the connection to the ConnectionPool
		{
			String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
			String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection

			String sql;
			String tabName;
			
			// Check input parameters and give some defaults
			if (StringUtil.isNullOrBlank(endTime))
				endTime = "now";


			// Get graph properties, (Label, and isPercentGraph) for this graph
			tabName = CentralPersistWriterBase.getTableName(conn, sessionName, Table.GRAPH_PROPERTIES, null, true);
			
			String  graphLabel     = "";
			String  graphProps     = "";
			String  graphCategory  = "";
			boolean isPercentGraph = false;

			sql = "select "
				+ "  " + lq + "GraphLabel"       + rq
				+ " ," + lq + "GraphProps"       + rq
				+ " ," + lq + "GraphCategory"    + rq
				+ " ," + lq + "isPercentGraph"   + rq
				+" from " + tabName
				+" where " + lq + "CmName"           + rq + " = '" + cmName + "'"
				+"   and " + lq + "GraphName"        + rq + " = '" + graphName + "'"
				+"   and " + lq + "SessionStartTime" + rq + " = (select max(" + lq + "SessionStartTime" + rq + ") from " + tabName + ")"
				;

			// autoclose: stmnt, rs
			try (Statement stmnt = conn.createStatement())
			{
				// set TIMEOUT
				stmnt.setQueryTimeout(_defaultQueryTimeout);

				// Execute and read result
				try (ResultSet rs = stmnt.executeQuery(sql))
				{
					while (rs.next())
					{
						graphLabel     = rs.getString (1);
						graphProps     = rs.getString (2);
						graphCategory  = rs.getString (3);
						isPercentGraph = rs.getBoolean(4);
						
						//System.out.println("sessionName='"+sessionName+"', cmName='"+cmName+"', graphName='"+graphName+"', startTime='"+startTime+"':::::::: graphLabel='"+graphLabel+"', isPercentGraph='"+isPercentGraph+"'.");
					}
				}
			}

			// TODO: We could probably use 'graphProps' or guess from 'graphLabel'
			//       what 'sampleType' we should use MIN or MAX (if the input is AUTO)
			SampleType sampleType_AutoOverflowFallback = autoOverflow;
			if (sampleType_AutoOverflowFallback == null)
			{
				sampleType_AutoOverflowFallback = SampleType.MAX_OVER_SAMPLES;
				if (StringUtil.hasValue(graphProps) && graphProps.contains("autoOverflow"))
				{
					// Parse the JSON in 'graphProps'
					try
					{
						ObjectMapper mapper = new ObjectMapper();
						JsonNode jsonRoot = mapper.readTree(graphProps);
						JsonNode autoOverflowNode = jsonRoot.get("autoOverflow");
						if (autoOverflowNode != null)
						{
							String autoOverflowStr = autoOverflowNode.asText();
							if      ("MAX_OVER_SAMPLES".equalsIgnoreCase(autoOverflowStr)) sampleType_AutoOverflowFallback = SampleType.MAX_OVER_SAMPLES;
							else if ("MIN_OVER_SAMPLES".equalsIgnoreCase(autoOverflowStr)) sampleType_AutoOverflowFallback = SampleType.MIN_OVER_SAMPLES;
							else {
								_logger.info("Unknown value '" + autoOverflowStr + "' for JSON field 'autoOverflow'. The default '" + sampleType_AutoOverflowFallback + "' will be used.");
							}
						}
					}
					catch(IOException ex)
					{
						_logger.info("Problems parsing JSON 'graphProps' String '" + graphProps + "'. Setting autoOverflow to '" + sampleType_AutoOverflowFallback + "'.");
					}
				}
			}
			
			// sessionName.cmName_graphName
			tabName = lq + sessionName + rq + "." + lq + cmName + "_" + graphName + rq;

			// If we only should "keep" last x records
			// Note: This can't be done in SQL, so just keep last records
			int onlyLastXNumOfRecords = 0;

			// Build a where string
			// - no startTime: grab last hour
			// - has startTime
			//     - if INT value, use that as last x minutes
			//     - if STR value, pass that as a valid "timestamp str"
			String whereSessionSampleTime = "";
			if (StringUtil.hasValue(startTime))
			{
				startTime = startTime.trim();
				try
				{
					// Remove any '-' signs at the start
					if (startTime.startsWith("-"))
						startTime = startTime.substring(1);
					
					// If value starts with a number and ends with m, h, d or l  (or M, H, D or L), then set 
					int multiPlayer = 60;
					if (startTime.matches("^[0-9]+[mhdlMHDL]$"))
					{
						String lastChar = startTime.substring(startTime.length()-1).toLowerCase();
						if ("m".equals(lastChar)) multiPlayer = 60;           // Minutes
						if ("h".equals(lastChar)) multiPlayer = 60 * 60;      // Hours
						if ("d".equals(lastChar)) multiPlayer = 24 * 60 * 60; // Days
						
						startTime = startTime.substring(0, startTime.length()-1);

						if ("l".equals(lastChar)) // keep only last X rows from the resultset
						{
							onlyLastXNumOfRecords = StringUtil.parseInt(startTime, 0);
							startTime = "all"; // below parseInt() will fail and "catch" will do the rest
						}
					}

					int intVal = Integer.parseInt(startTime);
					Timestamp ts = new Timestamp( System.currentTimeMillis() - (intVal * multiPlayer * 1000L)); // NOTE: the long/L after 1000 otherwise we will have int overflow 
					whereSessionSampleTime = "'" + ts.toString() + "'";
				}
				catch(NumberFormatException nfe)
				{
					if ("all".equalsIgnoreCase(startTime) || "ls".equalsIgnoreCase(startTime)) // ls = Last Session
					{
						whereSessionSampleTime = " (select max(" + lq + "SessionStartTime" + rq + ") from " + tabName + ")";
					}
					// Parse startTime and also add endTime
					else
					{
						Timestamp startTimeTs = null;
						Timestamp endTimeTs = null;

						try { startTimeTs = TimeUtils.parseToTimestampX(startTime); }
						catch (ParseException pe) 
						{ 
							throw new SQLException("Problems parsing startTime.", pe);
						}
						
						// endTime: can be 'a full date', or '#m|#h|#d'
						if (StringUtil.hasValue(endTime))
						{
							if (endTime.equalsIgnoreCase("now"))
							{
								endTimeTs = new Timestamp( System.currentTimeMillis() );
							}
							else if (endTime.matches("^[0-9]+[mhdMHD]$")) // '#m|#h|#d'
							{
								int multiPlayer = 60;
								String endTimeTmp = endTime;
								String lastChar = endTimeTmp.substring(endTimeTmp.length()-1).toLowerCase();
								if ("m".equals(lastChar)) multiPlayer = 60;           // Minutes
								if ("h".equals(lastChar)) multiPlayer = 60 * 60;      // Hours
								if ("d".equals(lastChar)) multiPlayer = 24 * 60 * 60; // Days
								
								endTimeTmp = endTimeTmp.substring(0, endTimeTmp.length()-1);
								
								int intVal = Math.abs(Integer.parseInt(endTimeTmp)); // Parse + Turn minus values into positive
								endTimeTs = new Timestamp( startTimeTs.getTime() + intVal * multiPlayer * 1000);
							}
							else if (endTime.matches("^[0-9]+$")) // only a number, means minutes
							{
								int multiPlayer = 60;
								int intVal = Math.abs(Integer.parseInt(endTime)); // Parse + Turn minus values into positive
								endTimeTs = new Timestamp( startTimeTs.getTime() + intVal * multiPlayer * 1000);
							}
							else
							{
								try { endTimeTs = TimeUtils.parseToTimestampX(endTime); }
								catch (ParseException pe) 
								{ 
									throw new SQLException("Problems parsing endTime.", pe);
								}
							}
						}

						if (endTimeTs == null)
							endTimeTs = new Timestamp( startTimeTs.getTime() + 3600 * 1000);

						whereSessionSampleTime = "'" + startTimeTs + "' and " + lq + "SessionSampleTime" + rq + " <= '" + endTimeTs + "' ";
					}
				}
			}
			else
			{
				Timestamp minusOneHour = new Timestamp( System.currentTimeMillis() - 3600 * 1000);
				whereSessionSampleTime = "'" + minusOneHour.toString() + "'";
			}


			// Get num records that we expect (this so we automatically can apply SampleType.MAX_OVER_SAMPLES if it's to many rows)
			if (SampleType.AUTO.equals(sampleType))
			{
				int threshold = SAMPLE_TYPE_AUTO__OVERFLOW_THRESHOLD;        // 360 records is 2 hours, with a 20 seconds sample interval:  60 * 3 * 2
				int dataRowCount = -1;

				if (sampleValue > 0)
					threshold = sampleValue;
				
				sql = "select count(*) "
						+" from " + tabName
						+" where " + lq + "SessionSampleTime" + rq + " >= " + whereSessionSampleTime
						;
				// autoclose: stmnt, rs
				try (Statement stmnt = conn.createStatement())
				{
					// set TIMEOUT
					stmnt.setQueryTimeout(_defaultQueryTimeout);

					// Execute and read result
					try (ResultSet rs = stmnt.executeQuery(sql))
					{
						while (rs.next())
							dataRowCount = rs.getInt(1);
					}
				}

				// Default is to get ALL records
				sampleType  = SampleType.ALL;

				// If it's to many rows, then switch to MAX_OVER_SAMPLES
				if (dataRowCount > threshold)
				{
					sampleValue = dataRowCount / threshold;
					if (sampleValue > 1)
					{
					//	sampleType = SampleType.MAX_OVER_SAMPLES;
						sampleType = sampleType_AutoOverflowFallback;
					}
				}
				//System.out.println("GraphData AUTO... dataRowCount="+dataRowCount+", sampleType="+sampleType+", sampleValue="+sampleValue+" :::: for sessionName='"+sessionName+"', cmName='"+cmName+"', graphName='"+graphName+"'.");
				_logger.debug("GraphData AUTO... dataRowCount="+dataRowCount+", sampleType="+sampleType+", sampleValue="+sampleValue+" :::: for sessionName='"+sessionName+"', cmName='"+cmName+"', graphName='"+graphName+"'.");
			}


			sql = "select * "
					+" from " + tabName
					+" where "    + lq + "SessionSampleTime" + rq + " >= " + whereSessionSampleTime
					+" order by " + lq + "SessionSampleTime" + rq 
					;

			// A Map that will indicate that ONLY NULL values was fetched from the database
			LinkedHashMap<String, Boolean> labelAndAllNullMap = null;
			if (Configuration.getCombinedConfiguration().getBooleanProperty("CentralPersistReader.getGraphData.removeSeriesWhenAllRowsAreNull", true));
				labelAndAllNullMap = new LinkedHashMap<>();

			List<DbxGraphData> list = new ArrayList<>();
			int readCount = 0;

			// autoclose: stmnt, rs
			try (Statement stmnt = conn.createStatement())
			{
				// set TIMEOUT
				stmnt.setQueryTimeout(_defaultQueryTimeout);

				// Execute and read result
				try (ResultSet rs = stmnt.executeQuery(sql))
				{
					ResultSetMetaData md = rs.getMetaData();
					int colCount = md.getColumnCount();
					
					int colDataStart = 4; // "SessionStartTime", "SessionSampleTime", "CmSampleTime", "System+User CPU (@@cpu_busy + @@cpu_io)"
					
					List<String> labelNames = new LinkedList<>();
					for (int c=colDataStart; c<colCount+1; c++)
						labelNames.add( md.getColumnLabel(c) );
	
					// Return ALL rows
	//				if (avgOverMinutes <= 0 && maxOverMinutes <= 0)
					if (SampleType.ALL.equals(sampleType))
					{
						while (rs.next())
						{
							readCount++;
							LinkedHashMap<String, Double> labelAndDataMap = new LinkedHashMap<>();
	
						//	Timestamp sessionStartTime  = rs.getTimestamp(1);
							Timestamp sessionSampleTime = rs.getTimestamp(2);
						//	Timestamp cmSampleTime      = rs.getTimestamp(3);
							
							for (int c=colDataStart, l=0; c<colCount+1; c++, l++)
							{
								labelAndDataMap.put( labelNames.get(l), rs.getDouble(c) );
								labelAndDataNullHandler(labelAndAllNullMap, labelNames.get(l), rs.wasNull());

								//-------------------------------------------------
								// IF we want to handle NULL values...
								//-------------------------------------------------
								//String label = labelNames.get(l);
								//Double value = rs.getDouble(c);
                                //
								//labelAndDataMap.put( label, value );
                                //
								//labelAndDataNullHandler(labelAndAllNullMap, label, rs.wasNull());
							}
	
							DbxGraphData e = new DbxGraphData(cmName, graphName, sessionSampleTime, graphLabel, graphProps, graphCategory, isPercentGraph, labelAndDataMap);
							list.add(e);
							
							// Remove already added records (keep only last X number of rows)
							if (onlyLastXNumOfRecords > 0)
							{
								if (list.size() > onlyLastXNumOfRecords)
									list.remove(0);
							}
						}
					}
					else if (SampleType.AVG_OVER_MINUTES.equals(sampleType)) // calculate average over X minutes
					{
						// This section calculates avaerage values over X number of minutes (goal is to return LESS records to client)
						// Algorithm:
						//   - Add records to a temporary list
						//   - When 'sessionSampleTime' has reached a "time span" (or there are no-more-rows)
						//     - calculate average over all "saved" records.
						//     - add the average calculated record to the "return list"
						
						//
						Timestamp spanStartTime = null;
						List<Map<String, Double>> tmpList = new ArrayList<>();
						
						long avgOverMs = sampleValue * 1000 * 60; // convert the input MINUTE into MS
	
						while (rs.next())
						{
							readCount++;
							LinkedHashMap<String, Double> labelAndDataMap = new LinkedHashMap<>();
	
						//	Timestamp sessionStartTime  = rs.getTimestamp(1);
							Timestamp sessionSampleTime = rs.getTimestamp(2);
						//	Timestamp cmSampleTime      = rs.getTimestamp(3);
							
							if (spanStartTime == null)
								spanStartTime = sessionSampleTime;
	
							for (int c=colDataStart, l=0; c<colCount+1; c++, l++)
							{
								labelAndDataMap.put( labelNames.get(l), rs.getDouble(c) );
								labelAndDataNullHandler(labelAndAllNullMap, labelNames.get(l), rs.wasNull());
							}
	
							tmpList.add(labelAndDataMap);
							
							// Is it time to do calculation yet? 
							long spanDiffMs = sessionSampleTime.getTime() - spanStartTime.getTime();
							if (spanDiffMs >= avgOverMs)
							{
								LinkedHashMap<String, Double> avgMap = calcAvgData(tmpList);
								tmpList.clear();
								
								DbxGraphData e = new DbxGraphData(cmName, graphName, spanStartTime, graphLabel, graphProps, graphCategory, isPercentGraph, avgMap);
								list.add(e);
								
								// Start a new spanTime
								spanStartTime = sessionSampleTime;
							}
						}
						// Calculate and Add results from "last" records 
						if ( ! tmpList.isEmpty() )
						{
							LinkedHashMap<String, Double> avgMap = calcAvgData(tmpList);
							tmpList.clear();
							
							DbxGraphData e = new DbxGraphData(cmName, graphName, spanStartTime, graphLabel, graphProps, graphCategory, isPercentGraph, avgMap);
							list.add(e);
						}
					}
					else if (SampleType.MAX_OVER_MINUTES.equals(sampleType)) // calculate MAX over X minutes
					{
						// This section calculates MAX values over X number of minutes (goal is to return LESS records to client)
						// Algorithm:
						//   - Add records to a temporary list
						//   - When 'sessionSampleTime' has reached a "time span" (or there are no-more-rows)
						//     - calculate MAX over all "saved" records.
						//     - add the MAX calculated record to the "return list"
						
						//
						Timestamp spanStartTime = null;
						List<Map<String, Double>> tmpList = new ArrayList<>();
						
						long maxOverMs = sampleValue * 1000 * 60; // convert the input MINUTE into MS
	
						while (rs.next())
						{
							readCount++;
							LinkedHashMap<String, Double> labelAndDataMap = new LinkedHashMap<>();
	
						//	Timestamp sessionStartTime  = rs.getTimestamp(1);
							Timestamp sessionSampleTime = rs.getTimestamp(2);
						//	Timestamp cmSampleTime      = rs.getTimestamp(3);
							
							if (spanStartTime == null)
								spanStartTime = sessionSampleTime;
	
							for (int c=colDataStart, l=0; c<colCount+1; c++, l++)
							{
								labelAndDataMap.put( labelNames.get(l), rs.getDouble(c) );
								labelAndDataNullHandler(labelAndAllNullMap, labelNames.get(l), rs.wasNull());
							}
	
							tmpList.add(labelAndDataMap);
							
							// Is it time to do calculation yet? 
							long spanDiffMs = sessionSampleTime.getTime() - spanStartTime.getTime();
							if (spanDiffMs >= maxOverMs)
							{
								LinkedHashMap<String, Double> maxMap = calcMaxData(tmpList);
								tmpList.clear();
								
								DbxGraphData e = new DbxGraphData(cmName, graphName, spanStartTime, graphLabel, graphProps, graphCategory, isPercentGraph, maxMap);
								list.add(e);
								
								// Start a new spanTime
								spanStartTime = sessionSampleTime;
							}
						}
						// Calculate and Add results from "last" records 
						if ( ! tmpList.isEmpty() )
						{
							LinkedHashMap<String, Double> maxMap = calcMaxData(tmpList);
							tmpList.clear();
							
							DbxGraphData e = new DbxGraphData(cmName, graphName, spanStartTime, graphLabel, graphProps, graphCategory, isPercentGraph, maxMap);
							list.add(e);
						}
					}
					else if (SampleType.MIN_OVER_MINUTES.equals(sampleType)) // calculate MAX over X minutes
					{
						// This section calculates MIN values over X number of minutes (goal is to return LESS records to client)
						// Algorithm:
						//   - Add records to a temporary list
						//   - When 'sessionSampleTime' has reached a "time span" (or there are no-more-rows)
						//     - calculate MIN over all "saved" records.
						//     - add the MIN calculated record to the "return list"
						
						//
						Timestamp spanStartTime = null;
						List<Map<String, Double>> tmpList = new ArrayList<>();
						
						long maxOverMs = sampleValue * 1000 * 60; // convert the input MINUTE into MS
	
						while (rs.next())
						{
							readCount++;
							LinkedHashMap<String, Double> labelAndDataMap = new LinkedHashMap<>();
	
						//	Timestamp sessionStartTime  = rs.getTimestamp(1);
							Timestamp sessionSampleTime = rs.getTimestamp(2);
						//	Timestamp cmSampleTime      = rs.getTimestamp(3);
							
							if (spanStartTime == null)
								spanStartTime = sessionSampleTime;
	
							for (int c=colDataStart, l=0; c<colCount+1; c++, l++)
							{
								labelAndDataMap.put( labelNames.get(l), rs.getDouble(c) );
								labelAndDataNullHandler(labelAndAllNullMap, labelNames.get(l), rs.wasNull());
							}
	
							tmpList.add(labelAndDataMap);
							
							// Is it time to do calculation yet? 
							long spanDiffMs = sessionSampleTime.getTime() - spanStartTime.getTime();
							if (spanDiffMs >= maxOverMs)
							{
								LinkedHashMap<String, Double> maxMap = calcMinData(tmpList);
								tmpList.clear();
								
								DbxGraphData e = new DbxGraphData(cmName, graphName, spanStartTime, graphLabel, graphProps, graphCategory, isPercentGraph, maxMap);
								list.add(e);
								
								// Start a new spanTime
								spanStartTime = sessionSampleTime;
							}
						}
						// Calculate and Add results from "last" records 
						if ( ! tmpList.isEmpty() )
						{
							LinkedHashMap<String, Double> maxMap = calcMinData(tmpList);
							tmpList.clear();
							
							DbxGraphData e = new DbxGraphData(cmName, graphName, spanStartTime, graphLabel, graphProps, graphCategory, isPercentGraph, maxMap);
							list.add(e);
						}
					}
					else if (SampleType.SUM_OVER_MINUTES.equals(sampleType)) // calculate SUM over X minutes
					{
						// This section calculates SUM values over X number of minutes (goal is to return LESS records to client)
						// Algorithm:
						//   - Add records to a temporary list
						//   - When 'sessionSampleTime' has reached a "time span" (or there are no-more-rows)
						//     - calculate SUM over all "saved" records.
						//     - add the SUM calculated record to the "return list"
						
						//
						Timestamp spanStartTime = null;
						List<Map<String, Double>> tmpList = new ArrayList<>();
						
						long sumOverMs = sampleValue * 1000 * 60; // convert the input MINUTE into MS
	
						while (rs.next())
						{
							readCount++;
							LinkedHashMap<String, Double> labelAndDataMap = new LinkedHashMap<>();
	
						//	Timestamp sessionStartTime  = rs.getTimestamp(1);
							Timestamp sessionSampleTime = rs.getTimestamp(2);
						//	Timestamp cmSampleTime      = rs.getTimestamp(3);
							
							if (spanStartTime == null)
								spanStartTime = sessionSampleTime;
	
							for (int c=colDataStart, l=0; c<colCount+1; c++, l++)
							{
								labelAndDataMap.put( labelNames.get(l), rs.getDouble(c) );
								labelAndDataNullHandler(labelAndAllNullMap, labelNames.get(l), rs.wasNull());
							}
	
							tmpList.add(labelAndDataMap);
							
							// Is it time to do calculation yet? 
							long spanDiffMs = sessionSampleTime.getTime() - spanStartTime.getTime();
							if (spanDiffMs >= sumOverMs)
							{
								LinkedHashMap<String, Double> sumMap = calcSumData(tmpList);
								tmpList.clear();
								
								DbxGraphData e = new DbxGraphData(cmName, graphName, spanStartTime, graphLabel, graphProps, graphCategory, isPercentGraph, sumMap);
								list.add(e);
								
								// Start a new spanTime
								spanStartTime = sessionSampleTime;
							}
						}
						// Calculate and Add results from "last" records 
						if ( ! tmpList.isEmpty() )
						{
							LinkedHashMap<String, Double> sumMap = calcSumData(tmpList);
							tmpList.clear();
							
							DbxGraphData e = new DbxGraphData(cmName, graphName, spanStartTime, graphLabel, graphProps, graphCategory, isPercentGraph, sumMap);
							list.add(e);
						}
					}
					else if (SampleType.MAX_OVER_SAMPLES.equals(sampleType)) // calculate MAX over X samples
					{
						// This section calculates MAX values over X number of samples (goal is to return LESS records to client)
						// Algorithm:
						//   - Add records to a temporary list
						//   - When 'count' has reached a "span" (or there are no-more-rows)
						//     - calculate MAX over all "saved" records.
						//     - add the MAX calculated record to the "return list"
						
						//
						int       spanStartRow  = 0;
						Timestamp spanStartTime = null;
						List<Map<String, Double>> tmpList = new ArrayList<>();
						
						long maxOverRows = sampleValue; // convert the input MINUTE into MS
	
						while (rs.next())
						{
							spanStartRow++;
							readCount++;
							LinkedHashMap<String, Double> labelAndDataMap = new LinkedHashMap<>();
	
						//	Timestamp sessionStartTime  = rs.getTimestamp(1);
							Timestamp sessionSampleTime = rs.getTimestamp(2);
						//	Timestamp cmSampleTime      = rs.getTimestamp(3);
							
							if (spanStartTime == null)
								spanStartTime = sessionSampleTime;
	
							for (int c=colDataStart, l=0; c<colCount+1; c++, l++)
							{
								labelAndDataMap.put( labelNames.get(l), rs.getDouble(c) );
								labelAndDataNullHandler(labelAndAllNullMap, labelNames.get(l), rs.wasNull());
							}
	
							tmpList.add(labelAndDataMap);
							
							// Is it time to do calculation yet? 
							if (spanStartRow >= maxOverRows)
							{
								LinkedHashMap<String, Double> maxMap = calcMaxData(tmpList);
								tmpList.clear();
								
								DbxGraphData e = new DbxGraphData(cmName, graphName, spanStartTime, graphLabel, graphProps, graphCategory, isPercentGraph, maxMap);
								list.add(e);
								
								// Start a new spanTime
								spanStartRow  = 0;
								spanStartTime = sessionSampleTime;
							}
						}
						// Calculate and Add results from "last" records 
						if ( ! tmpList.isEmpty() )
						{
							LinkedHashMap<String, Double> maxMap = calcMaxData(tmpList);
							tmpList.clear();
							
							DbxGraphData e = new DbxGraphData(cmName, graphName, spanStartTime, graphLabel, graphProps, graphCategory, isPercentGraph, maxMap);
							list.add(e);
						}
					}
					else if (SampleType.MIN_OVER_SAMPLES.equals(sampleType)) // calculate MIN over X samples
					{
						// This section calculates MIN values over X number of samples (goal is to return LESS records to client)
						// Algorithm:
						//   - Add records to a temporary list
						//   - When 'count' has reached a "span" (or there are no-more-rows)
						//     - calculate MIN over all "saved" records.
						//     - add the MIN calculated record to the "return list"
						
						//
						int       spanStartRow  = 0;
						Timestamp spanStartTime = null;
						List<Map<String, Double>> tmpList = new ArrayList<>();
						
						long maxOverRows = sampleValue; // convert the input MINUTE into MS
	
						while (rs.next())
						{
							spanStartRow++;
							readCount++;
							LinkedHashMap<String, Double> labelAndDataMap = new LinkedHashMap<>();
	
						//	Timestamp sessionStartTime  = rs.getTimestamp(1);
							Timestamp sessionSampleTime = rs.getTimestamp(2);
						//	Timestamp cmSampleTime      = rs.getTimestamp(3);
							
							if (spanStartTime == null)
								spanStartTime = sessionSampleTime;
	
							for (int c=colDataStart, l=0; c<colCount+1; c++, l++)
							{
								labelAndDataMap.put( labelNames.get(l), rs.getDouble(c) );
								labelAndDataNullHandler(labelAndAllNullMap, labelNames.get(l), rs.wasNull());
							}
	
							tmpList.add(labelAndDataMap);
							
							// Is it time to do calculation yet? 
							if (spanStartRow >= maxOverRows)
							{
								LinkedHashMap<String, Double> maxMap = calcMinData(tmpList);
								tmpList.clear();
								
								DbxGraphData e = new DbxGraphData(cmName, graphName, spanStartTime, graphLabel, graphProps, graphCategory, isPercentGraph, maxMap);
								list.add(e);
								
								// Start a new spanTime
								spanStartRow  = 0;
								spanStartTime = sessionSampleTime;
							}
						}
						// Calculate and Add results from "last" records 
						if ( ! tmpList.isEmpty() )
						{
							LinkedHashMap<String, Double> maxMap = calcMinData(tmpList);
							tmpList.clear();
							
							DbxGraphData e = new DbxGraphData(cmName, graphName, spanStartTime, graphLabel, graphProps, graphCategory, isPercentGraph, maxMap);
							list.add(e);
						}
					}
					else
					{
						throw new RuntimeException("Reached un-determened code... The sampleType="+sampleType+" has not yet been implemeted, sampleValue="+sampleValue);
					}
				}
			}

			// For debugging purposes
			if (readCount == 0)
			{
				_logger.info("NO Records was found: getGraphData(sessionName='"+sessionName+"', cmName='"+cmName+"', graphName='"+graphName+"', startTime='"+startTime+"', endTime='"+endTime+"', sampleType="+sampleType+", sampleValue="+sampleValue+") SQL=|"+sql+"|, readCount="+readCount+", retListSize="+list.size());
			}
			
//			System.out.println("getGraphData(sessionName='"+sessionName+"', cmName='"+cmName+"', graphName='"+graphName+"', startTime='"+startTime+"', endTime='"+endTime+"', sampleType="+sampleType+", sampleValue="+sampleValue+") SQL=|"+sql+"|, readCount="+readCount+", retListSize="+list.size());
			if (_logger.isDebugEnabled())
				_logger.debug("getGraphData(sessionName='"+sessionName+"', cmName='"+cmName+"', graphName='"+graphName+"', startTime='"+startTime+"', endTime='"+endTime+"', sampleType="+sampleType+", sampleValue="+sampleValue+") SQL=|"+sql+"|, readCount="+readCount+", retListSize="+list.size());


			// Removing series/labels that ONLY hold NULL values in the database
			if (labelAndAllNullMap != null)
			{
				for (Entry<String, Boolean> entry : labelAndAllNullMap.entrySet())
				{
					if (entry.getValue())
					{
						String label = entry.getKey();
						
						for (DbxGraphData dbxGraphEntry : list)
							dbxGraphEntry.removeSerieName(label);

						_logger.info("Removing label/serie '" + label + "' from CM='" + cmName + "', graphName='" + graphName + "' since ALL rows had NULL values in the table.");
					}
				}
			}

			// Return
			return list;
		}
		finally
		{
			releaseConnection(conn);
		}
	}

	/** maintain a Map with &lt;Label, allRowsWasNull:Boolean&gt;  */
	private static void labelAndDataNullHandler(LinkedHashMap<String, Boolean> map, String label, boolean wasNull)
	{
		if (map == null)
			return;

		Boolean prev = map.get(label);
		if (prev == null)
		{
			// did not exists, just add it with current value of wasNull
			map.put(label, wasNull);
		}
		else
		{
			// Previous fetch was a NULL value
			// But this fetch was a REAL value ... then set to FALSE
			if (prev.equals(Boolean.TRUE) && !wasNull)
				map.put(label, Boolean.FALSE);
		}
	}
	
//	public static void main(String[] args)
//	{
//		LinkedHashMap<String, Boolean> labelAndAllNullMap = new LinkedHashMap<>();
//		
//		labelAndDataNullHandler(labelAndAllNullMap, "c1", true);
//		labelAndDataNullHandler(labelAndAllNullMap, "c1", true);
//		labelAndDataNullHandler(labelAndAllNullMap, "c1", true);
//		labelAndDataNullHandler(labelAndAllNullMap, "c1", true);
//		labelAndDataNullHandler(labelAndAllNullMap, "c1", true);
//		labelAndDataNullHandler(labelAndAllNullMap, "c1", true);
//
//		labelAndDataNullHandler(labelAndAllNullMap, "c2", false);
//		labelAndDataNullHandler(labelAndAllNullMap, "c2", true);
//
//		labelAndDataNullHandler(labelAndAllNullMap, "c3", true);
//		labelAndDataNullHandler(labelAndAllNullMap, "c3", false);
//
//		System.out.println(labelAndAllNullMap);
//		
//		for (Entry<String, Boolean> entry : labelAndAllNullMap.entrySet())
//		{
//			if (entry.getValue())
//				System.out.println("ALL NULL column '" + entry.getKey() + "'."); // This should return 'c1' as the only column
//		}
//	}

	/**
	 * Calculate average values from all values in the Map
	 * @param tmpList
	 * @return
	 */
	private LinkedHashMap<String, Double> calcAvgData(List<Map<String, Double>> tmpList)
	{
		// Algorithm
		// - add all List entries to a SUM map
		// - SUM Map Values / numOfNotNullValuesInMap
		
		// Create the output Map
		LinkedHashMap<String, Double> toMap    = new LinkedHashMap<>();
		
		// This will hold counts for each key (null values wont be counted)
		LinkedHashMap<String, Integer> countMap = new LinkedHashMap<>();

		// Add/Sum all entries from the List of Maps into a single Map with same keys 
		for (Map<String, Double> fromMap : tmpList)
		{
			for (Entry<String, Double> from : fromMap.entrySet())
			{
				String fromKey = from.getKey();
				Double fromVal = from.getValue();

				if (fromVal == null)
					fromVal = 0d;
				else
				{
					// Increment "count" for this key.
					Integer count = countMap.get(fromKey);
					count = (count == null ? 1 : count + 1);
					countMap.put(fromKey, count);
				}
				
				Double currentVal = toMap.get(fromKey);
				if (currentVal == null)
					toMap.put(fromKey, fromVal);
				else
					toMap.put(fromKey, currentVal + fromVal);
			}
		}

		// Do average calulation on the SUM Map
//		double count = tmpList.size();
		for (Entry<String, Double> sum : toMap.entrySet())
		{
			String sumKey = sum.getKey();
			Double sumVal = sum.getValue();

			// toMap.put(sumKey, Double.valueOf(sumVal / count) );
			
			Integer count = countMap.get(sumKey);
			
			// If we didn't find any count, then all values in the from List/Map only contained null values
			// - So set a null value for the key
			// - if count: simply divide sum values with count
			if (count == null)
			{
				toMap.put(sumKey, null);
			}
			else
			{
				BigDecimal bd = new BigDecimal(sumVal / count).setScale(1, BigDecimal.ROUND_HALF_EVEN /*RoundingMode.HALF_UP*/);  // in the CENTRAL DB the datatype is decimal(16, 1)
				Double avgVal = Double.valueOf(bd.doubleValue());
				toMap.put(sumKey, avgVal);
			}
		}

		return toMap;
	}

	/** Check if all values are negative, returns early (as soon as any positive numbers are found */
	private boolean containsOnlyNegativeNumbers(List<Map<String, Double>> tmpList)
	{
		if (tmpList.isEmpty())
			return false;

		for (Map<String, Double> fromMap : tmpList)
		{
			for (Double fromVal : fromMap.values())
			{
				if (fromVal != null && fromVal > 0)
					return false;
			}
		}
		return true;
	}
	
	/**
	 * Calculate max values from all values in the Map
	 * @param tmpList
	 * @return
	 */
	private LinkedHashMap<String, Double> calcMaxData(List<Map<String, Double>> tmpList)
	{
		// Algorithm
		// - add all List entries to a MAX map

		// Check if there is only NEGATIVE numbers... then we need to change strategy from MAX to MIN
		boolean containsOnlyNegativeNumbers = containsOnlyNegativeNumbers(tmpList);
		
		// Create the output Map
		LinkedHashMap<String, Double> toMap = new LinkedHashMap<>();
		
		// Add MAX entries from the List of Maps into a single Map with same keys 
		for (Map<String, Double> fromMap : tmpList)
		{
			for (Entry<String, Double> from : fromMap.entrySet())
			{
				String fromKey = from.getKey();
				Double fromVal = from.getValue();

				Double currentMaxVal = toMap.get(fromKey);
				if (currentMaxVal == null)
					toMap.put(fromKey, fromVal);
				else
				{
					if (fromVal != null)
					{
						if (containsOnlyNegativeNumbers)
							toMap.put(fromKey, Math.min(currentMaxVal, fromVal));
						else
							toMap.put(fromKey, Math.max(currentMaxVal, fromVal));
					}
				}
			}
		}
		
		return toMap;
	}

	/**
	 * Calculate min values from all values in the Map
	 * @param tmpList
	 * @return
	 */
	private LinkedHashMap<String, Double> calcMinData(List<Map<String, Double>> tmpList)
	{
		// Algorithm
		// - add all List entries to a MAX map

		// Create the output Map
		LinkedHashMap<String, Double> toMap = new LinkedHashMap<>();
		
		// Add MAX entries from the List of Maps into a single Map with same keys 
		for (Map<String, Double> fromMap : tmpList)
		{
			for (Entry<String, Double> from : fromMap.entrySet())
			{
				String fromKey = from.getKey();
				Double fromVal = from.getValue();

				Double currentMaxVal = toMap.get(fromKey);
				if (currentMaxVal == null)
					toMap.put(fromKey, fromVal);
				else
				{
					if (fromVal != null)
					{
						toMap.put(fromKey, Math.min(currentMaxVal, fromVal));
					}
				}
			}
		}
		
		return toMap;
	}

	/**
	 * Calculate sum values from all values in the Map
	 * @param tmpList
	 * @return
	 */
	private LinkedHashMap<String, Double> calcSumData(List<Map<String, Double>> tmpList)
	{
		// Algorithm
		// - add all List entries to a SUM map

		// Create the output Map
		LinkedHashMap<String, Double> toMap = new LinkedHashMap<>();
		
		// Add SUM entries from the List of Maps into a single Map with same keys 
		for (Map<String, Double> fromMap : tmpList)
		{
			for (Entry<String, Double> from : fromMap.entrySet())
			{
				String fromKey = from.getKey();
				Double fromVal = from.getValue();

				Double currentSumVal = toMap.get(fromKey);
				if (currentSumVal == null)
					toMap.put(fromKey, fromVal);
				else
				{
					if (fromVal != null)
					{
						toMap.put(fromKey, currentSumVal + fromVal);
					}
				}
			}
		}
		
		// Round the numbers in the toMap
		for (Entry<String, Double> sum : toMap.entrySet())
		{
			String sumKey = sum.getKey();
			Double sumVal = sum.getValue();

			if (sumVal != null)
			{
				BigDecimal bd = new BigDecimal(sumVal).setScale(1, BigDecimal.ROUND_HALF_EVEN /*RoundingMode.HALF_UP*/);  // in the CENTRAL DB the datatype is decimal(16, 1)
				Double avgVal = Double.valueOf(bd.doubleValue());
				toMap.put(sumKey, avgVal);
			}
		}

		return toMap;
	}

//	private static void testCalcAvgData()
//	{
//		List<Map<String, Double>> list = new ArrayList<>();
//		
//		Map<String, Double> map1 = new LinkedHashMap<>();
//		map1.put("k1", Double.valueOf(1.1) );
//		map1.put("k2", Double.valueOf(2.2) );
//		map1.put("k3", Double.valueOf(3.3) );
//		map1.put("k4", Double.valueOf(3.0) );
//		map1.put("k5", Double.valueOf(50) );
//		map1.put("k6", null );
//		map1.put("k7", null );
//		
//		Map<String, Double> map2 = new LinkedHashMap<>();
//		map2.put("k1", Double.valueOf(1.1) );
//		map2.put("k2", Double.valueOf(2.2) );
//		map2.put("k3", Double.valueOf(3.3) );
//		map2.put("k4", Double.valueOf(3.0) );
//		map2.put("k5", null );             // Note: null value (should not be included in divideByCount)
//		map2.put("k6", null );
//		map2.put("k7", null );
//		
//		Map<String, Double> map3 = new LinkedHashMap<>();
//		map3.put("k1", Double.valueOf(1.1) );
//		map3.put("k2", Double.valueOf(2.2) );
//		map3.put("k3", Double.valueOf(3.3) );
//		map3.put("k4", Double.valueOf(4.0) );
//		map3.put("k5", Double.valueOf(150) );
//		map3.put("k6", null );
//		map3.put("k7", Double.valueOf(99) );
//		
//		// Add entries to list
//		list.add(map1);
//		list.add(map2);
//		list.add(map3);
//
//		
//		// TEST: AVG
//		Map<String, Double> avgMap = new CentralPersistReader().calcAvgData(list);
//		Double avg1 = avgMap.get("k1");
//		Double avg2 = avgMap.get("k2");
//		Double avg3 = avgMap.get("k3");
//		Double avg4 = avgMap.get("k4");
//		Double avg5 = avgMap.get("k5");
//		Double avg6 = avgMap.get("k6");
//		Double avg7 = avgMap.get("k7");
//
//		System.out.println("--- TEST: AVG");
//		System.out.println("testCalcAvgData(): avgMap.size()="+avgMap.size());
//		System.out.println("testCalcAvgData(): k1="+avg1);
//		System.out.println("testCalcAvgData(): k2="+avg2);
//		System.out.println("testCalcAvgData(): k3="+avg3);
//		System.out.println("testCalcAvgData(): k4="+avg4);
//		System.out.println("testCalcAvgData(): k5="+avg5);
//		System.out.println("testCalcAvgData(): k6="+avg6);
//		System.out.println("testCalcAvgData(): k7="+avg7);
//
//		if ( avgMap.size() != 7) System.err.println("FAIL: avgMap.size() expected value '7', result value '"+avgMap.size()+"'");
//		if ( avg1 != 1.1d ) System.err.println("FAIL: avg1 expected value '1.1', result value '" +avg1+"'");
//		if ( avg2 != 2.2d ) System.err.println("FAIL: avg2 expected value '2.2', result value '" +avg2+"'");
//		if ( avg3 != 3.3d ) System.err.println("FAIL: avg3 expected value '3.3', result value '" +avg3+"'");
//		if ( avg4 != 3.3d ) System.err.println("FAIL: avg4 expected value '3.3', result value '" +avg4+"'");
//		if ( avg5 != 100d ) System.err.println("FAIL: avg5 expected value '100', result value '" +avg5+"'");
//		if ( avg6 != null ) System.err.println("FAIL: avg6 expected value 'null', result value '"+avg6+"'");
//		if ( avg7 != 99d  ) System.err.println("FAIL: avg7 expected value '99.0', result value '"+avg7+"'");
//
//	
//		// TEST: MAX
//		Map<String, Double> maxMap = new CentralPersistReader().calcMaxData(list);
//		Double max1 = maxMap.get("k1");
//		Double max2 = maxMap.get("k2");
//		Double max3 = maxMap.get("k3");
//		Double max4 = maxMap.get("k4");
//		Double max5 = maxMap.get("k5");
//		Double max6 = maxMap.get("k6");
//		Double max7 = maxMap.get("k7");
//		
//		System.out.println("--- TEST: MAX");
//		System.out.println("testCalcMaxData(): maxMap.size()="+maxMap.size());
//		System.out.println("testCalcMaxData(): k1="+max1);
//		System.out.println("testCalcMaxData(): k2="+max2);
//		System.out.println("testCalcMaxData(): k3="+max3);
//		System.out.println("testCalcMaxData(): k4="+max4);
//		System.out.println("testCalcMaxData(): k5="+max5);
//		System.out.println("testCalcMaxData(): k6="+max6);
//		System.out.println("testCalcMaxData(): k7="+max7);
//
//		if ( maxMap.size() != 7) System.err.println("FAIL: maxMap.size() expected value '7', result value '"+maxMap.size()+"'");
//		if ( max1 != 1.1d ) System.err.println("FAIL: max1 expected value '1.1', result value '" +max1+"'");
//		if ( max2 != 2.2d ) System.err.println("FAIL: max2 expected value '2.2', result value '" +max2+"'");
//		if ( max3 != 3.3d ) System.err.println("FAIL: max3 expected value '3.3', result value '" +max3+"'");
//		if ( max4 != 4d   ) System.err.println("FAIL: max4 expected value '4.0', result value '" +max4+"'");
//		if ( max5 != 150d ) System.err.println("FAIL: max5 expected value '150', result value '" +max5+"'");
//		if ( max6 != null ) System.err.println("FAIL: max6 expected value 'null', result value '"+max6+"'");
//		if ( max7 != 99d  ) System.err.println("FAIL: msg7 expected value '99.0', result value '"+max7+"'");
//	}
//
//	private static void testCalcMaxNegativeData()
//	{
//		List<Map<String, Double>> list = new ArrayList<>();
//		
//		Map<String, Double> map1 = new LinkedHashMap<>();
//		map1.put("k1", Double.valueOf(-1.1) );
//		map1.put("k2", Double.valueOf(-2.2) );
//		map1.put("k3", Double.valueOf(-3.3) );
//		map1.put("k4", Double.valueOf(-3.0) );
//		map1.put("k5", Double.valueOf(-50) );
//		map1.put("k6", null );
//		map1.put("k7", null );
//		
//		Map<String, Double> map2 = new LinkedHashMap<>();
//		map2.put("k1", Double.valueOf(-1.1) );
//		map2.put("k2", Double.valueOf(-2.2) );
//		map2.put("k3", Double.valueOf(-3.3) );
//		map2.put("k4", Double.valueOf(-3.0) );
//		map2.put("k5", null );             // Note: null value (should not be included in divideByCount)
//		map2.put("k6", null );
//		map2.put("k7", null );
//		
//		Map<String, Double> map3 = new LinkedHashMap<>();
//		map3.put("k1", Double.valueOf(-1.1) );
//		map3.put("k2", Double.valueOf(-2.2) );
//		map3.put("k3", Double.valueOf(-3.3) );
//		map3.put("k4", Double.valueOf(-4.0) );
//		map3.put("k5", Double.valueOf(-150) );
//		map3.put("k6", null );
//		map3.put("k7", Double.valueOf(-99) );
//		
//		// Add entries to list
//		list.add(map1);
//		list.add(map2);
//		list.add(map3);
//
//		
//		// TEST: AVG
//		Map<String, Double> avgMap = new CentralPersistReader().calcAvgData(list);
//		Double avg1 = avgMap.get("k1");
//		Double avg2 = avgMap.get("k2");
//		Double avg3 = avgMap.get("k3");
//		Double avg4 = avgMap.get("k4");
//		Double avg5 = avgMap.get("k5");
//		Double avg6 = avgMap.get("k6");
//		Double avg7 = avgMap.get("k7");
//
//		System.out.println("--- TEST: NEGATIVE-AVG");
//		System.out.println("testCalcAvgData(): avgMap.size()="+avgMap.size());
//		System.out.println("testCalcAvgData(): k1="+avg1);
//		System.out.println("testCalcAvgData(): k2="+avg2);
//		System.out.println("testCalcAvgData(): k3="+avg3);
//		System.out.println("testCalcAvgData(): k4="+avg4);
//		System.out.println("testCalcAvgData(): k5="+avg5);
//		System.out.println("testCalcAvgData(): k6="+avg6);
//		System.out.println("testCalcAvgData(): k7="+avg7);
//
//		if ( avgMap.size() != 7) System.err.println("FAIL: avgMap.size() expected value '7', result value '"+avgMap.size()+"'");
//		if ( avg1 != -1.1d ) System.err.println("FAIL: avg1 expected value '-1.1', result value '" +avg1+"'");
//		if ( avg2 != -2.2d ) System.err.println("FAIL: avg2 expected value '-2.2', result value '" +avg2+"'");
//		if ( avg3 != -3.3d ) System.err.println("FAIL: avg3 expected value '-3.3', result value '" +avg3+"'");
//		if ( avg4 != -3.3d ) System.err.println("FAIL: avg4 expected value '-3.3', result value '" +avg4+"'");
//		if ( avg5 != -100d ) System.err.println("FAIL: avg5 expected value '-100', result value '" +avg5+"'");
//		if ( avg6 != null  ) System.err.println("FAIL: avg6 expected value 'null', result value '"+avg6+"'");
//		if ( avg7 != -99d  ) System.err.println("FAIL: avg7 expected value '-99.0', result value '"+avg7+"'");
//
//	
//		// TEST: MAX
//		Map<String, Double> maxMap = new CentralPersistReader().calcMaxData(list);
//		Double max1 = maxMap.get("k1");
//		Double max2 = maxMap.get("k2");
//		Double max3 = maxMap.get("k3");
//		Double max4 = maxMap.get("k4");
//		Double max5 = maxMap.get("k5");
//		Double max6 = maxMap.get("k6");
//		Double max7 = maxMap.get("k7");
//		
//		System.out.println("--- TEST: NEGATIVE-MAX");
//		System.out.println("testCalcMaxData(): maxMap.size()="+maxMap.size());
//		System.out.println("testCalcMaxData(): k1="+max1);
//		System.out.println("testCalcMaxData(): k2="+max2);
//		System.out.println("testCalcMaxData(): k3="+max3);
//		System.out.println("testCalcMaxData(): k4="+max4);
//		System.out.println("testCalcMaxData(): k5="+max5);
//		System.out.println("testCalcMaxData(): k6="+max6);
//		System.out.println("testCalcMaxData(): k7="+max7);
//
//		if ( maxMap.size() != 7) System.err.println("FAIL: maxMap.size() expected value '7', result value '"+maxMap.size()+"'");
//		if ( max1 != -1.1d ) System.err.println("FAIL: max1 expected value '-1.1', result value '" +max1+"'");
//		if ( max2 != -2.2d ) System.err.println("FAIL: max2 expected value '-2.2', result value '" +max2+"'");
//		if ( max3 != -3.3d ) System.err.println("FAIL: max3 expected value '-3.3', result value '" +max3+"'");
//		if ( max4 != -4d   ) System.err.println("FAIL: max4 expected value '-4.0', result value '" +max4+"'");
//		if ( max5 != -150d ) System.err.println("FAIL: max5 expected value '-150', result value '" +max5+"'");
//		if ( max6 != null  ) System.err.println("FAIL: max6 expected value 'null', result value '"+max6+"'");
//		if ( max7 != -99d  ) System.err.println("FAIL: msg7 expected value '-99.0', result value '"+max7+"'");
//	}
//	
//	private static void testCalcSumData()
//	{
//		List<Map<String, Double>> list = new ArrayList<>();
//		
//		Map<String, Double> map1 = new LinkedHashMap<>();
//		map1.put("k1", Double.valueOf(1.1) );
//		map1.put("k2", Double.valueOf(2.2) );
//		map1.put("k3", Double.valueOf(3.3) );
//		map1.put("k4", Double.valueOf(3.0) );
//		map1.put("k5", Double.valueOf(50) );
//		map1.put("k6", null );
//		map1.put("k7", null );
//		
//		Map<String, Double> map2 = new LinkedHashMap<>();
//		map2.put("k1", Double.valueOf(1.1) );
//		map2.put("k2", Double.valueOf(2.2) );
//		map2.put("k3", Double.valueOf(3.3) );
//		map2.put("k4", Double.valueOf(3.0) );
//		map2.put("k5", null );             // Note: null value (should not be included in divideByCount)
//		map2.put("k6", null );
//		map2.put("k7", null );
//		
//		Map<String, Double> map3 = new LinkedHashMap<>();
//		map3.put("k1", Double.valueOf(1.1) );
//		map3.put("k2", Double.valueOf(2.2) );
//		map3.put("k3", Double.valueOf(3.3) );
//		map3.put("k4", Double.valueOf(4.0) );
//		map3.put("k5", Double.valueOf(150) );
//		map3.put("k6", null );
//		map3.put("k7", Double.valueOf(99) );
//		
//		// Add entries to list
//		list.add(map1);
//		list.add(map2);
//		list.add(map3);
//
//		
//		// TEST: SUM
//		Map<String, Double> sumMap = new CentralPersistReader().calcSumData(list);
//		Double sum1 = sumMap.get("k1");
//		Double sum2 = sumMap.get("k2");
//		Double sum3 = sumMap.get("k3");
//		Double sum4 = sumMap.get("k4");
//		Double sum5 = sumMap.get("k5");
//		Double sum6 = sumMap.get("k6");
//		Double sum7 = sumMap.get("k7");
//
//		System.out.println("--- TEST: SUM");
//		System.out.println("testCalcSumData(): avgMap.size()="+sumMap.size());
//		System.out.println("testCalcSumData(): k1="+sum1);
//		System.out.println("testCalcSumData(): k2="+sum2);
//		System.out.println("testCalcSumData(): k3="+sum3);
//		System.out.println("testCalcSumData(): k4="+sum4);
//		System.out.println("testCalcSumData(): k5="+sum5);
//		System.out.println("testCalcSumData(): k6="+sum6);
//		System.out.println("testCalcSumData(): k7="+sum7);
//
//		if ( sumMap.size() != 7) System.err.println("FAIL: sumMap.size() expected value '7', result value '"+sumMap.size()+"'");
//		if ( sum1 != 3.3d ) System.err.println("FAIL: sum1 expected value '3.3', result value '" +sum1+"'");
//		if ( sum2 != 6.6d ) System.err.println("FAIL: sum2 expected value '6.6', result value '" +sum2+"'");
//		if ( sum3 != 9.9d ) System.err.println("FAIL: sum3 expected value '9.9', result value '" +sum3+"'");
//		if ( sum4 != 10d  ) System.err.println("FAIL: sum4 expected value '10',  result value '" +sum4+"'");
//		if ( sum5 != 200d ) System.err.println("FAIL: sum5 expected value '200', result value '" +sum5+"'");
//		if ( sum6 != null ) System.err.println("FAIL: sum6 expected value 'null', result value '"+sum6+"'");
//		if ( sum7 != 99d  ) System.err.println("FAIL: sum7 expected value '99.0', result value '"+sum7+"'");
//
//	}

//	public static void main(String[] args)
//	{
//		testCalcAvgData();
//		testCalcMaxNegativeData();
//		testCalcSumData();
//	}



//	/**
//	 * Check if the last fetch from Counter Storage database has any counters
//	 * for the CounterModel named
//	 * 
//	 * @param name Name of the CM to check for
//	 */
//	public boolean hasCountersForCm(String name)
//	{
//		CmIndicator cmInd = getIndicatorForCm(name);
//		if (cmInd == null)
//			return false;
//
//		int rows = cmInd._absRows + cmInd._diffRows + cmInd._rateRows;
//		return (rows > 0);
//	}
//
//	public CmIndicator getIndicatorForCm(String name)
//	{
//		CmIndicator cmInd = _currentIndicatorMap.get(name);
//		return cmInd;
//	}
//	
//
//	/**
//	 */
//	public boolean hasSessionCountersForCm(String name)
//	{
//		SessionIndicator ind = getSessionIndicatorForCm(name);
//		if (ind == null)
//			return false;
//
//		int rows = ind._absSamples + ind._diffSamples + ind._rateSamples;
//		return (rows > 0);
//	}
//
//	public SessionIndicator getSessionIndicatorForCm(String name)
//	{
//		SessionIndicator ind = _sessionIndicatorMap.get(name);
//		return ind;
//	}
//
//	public SessionIndicator[] getSessionIndicators()
//	{
//		SessionIndicator[] si = new SessionIndicator[_sessionIndicatorMap.size()];
//		int i=0;
//		for (Map.Entry<String,SessionIndicator> entry : _sessionIndicatorMap.entrySet()) 
//		{
//			//String           key = entry.getKey();
//			SessionIndicator val = entry.getValue();
//			si[i++] = val;
//		}
//		return si;
//	}
	

	/*---------------------------------------------------
	** BEGIN: Listener stuff
	**---------------------------------------------------
	*/
//	EventListenerList   _listenerList  = new EventListenerList();
//
//	/** Add any listeners that want to see changes */
//	public void addChangeListener(ChangeListener l)
//	{
//		_listenerList.add(ChangeListener.class, l);
//	}
//
//	/** Remove the listener */
//	public void removeChangeListener(ChangeListener l)
//	{
//		_listenerList.remove(ChangeListener.class, l);
//	}
//
//	/** Kicked off when new entries are added */
////	protected void fireStateChanged()
//	protected void fireNewSessionsIsAvalilable()
//	{
//		Object aobj[] = _listenerList.getListenerList();
//		for (int i = aobj.length - 2; i >= 0; i -= 2)
//		{
//			if (aobj[i] == ChangeListener.class)
//			{
//				ChangeEvent changeEvent = new ChangeEvent(this);
//				((ChangeListener) aobj[i + 1]).stateChanged(changeEvent);
//			}
//		}
//	}
	/*---------------------------------------------------
	** END: Listener stuff
	**---------------------------------------------------
	*/



	/*---------------------------------------------------
	** BEGIN: Remove/cleanup functionality (should this be in the reader???)
	**---------------------------------------------------
	*/
	public void removeServerSchema(String name)
	throws SQLException
	{
		DbxConnection conn = getConnection(); // Get connection from a ConnectionPool
		try // block with: finally at end to return the connection to the ConnectionPool
		{
			conn.dropSchema(name);
		}
		finally
		{
			releaseConnection(conn);
		}
		
//		try // block with: finally at end to return the connection to the ConnectionPool
//		{
//			String q = conn.getQuotedIdentifierChar();
//
//			String sql = "drop schema " + q + name + q;
//
//			// autoclose: stmnt, rs
//			try (Statement stmnt = conn.createStatement())
//			{
//				stmnt.executeUpdate(sql);
//			}
//		}
//		finally
//		{
//			releaseConnection(conn);
//		}
	}

	public int removeServerMetaData(String name)
	throws SQLException
	{
		DbxConnection conn = getConnection(); // Get connection from a ConnectionPool
		try // block with: finally at end to return the connection to the ConnectionPool
		{
			String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
			String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection

			// CENTRAL_SESSIONS
			String tabName = CentralPersistWriterBase.getTableName(conn, null, Table.CENTRAL_SESSIONS, null, true);

			String sql = "delete from " + tabName + " where " + lq + "ServerName" + rq + " = '" + name + "'";

			// autoclose: stmnt, rs
			try (Statement stmnt = conn.createStatement())
			{
				int rowc = stmnt.executeUpdate(sql);
				return rowc;
			}
		}
		finally
		{
			releaseConnection(conn);

			// Notify any listeners that Sessions has been changed
			fireSessionChanges();
		}
	}




	/**
	 * SET the 'Status' field in the table 'DbxCentralSessions' (Table.CENTRAL_SESSIONS) to the passed status, which is a bitmap field.
	 * @param name    Name of the server
	 * @param status  Status (which is a bitmap), found in DbxCentralSessions.ST_*
	 * @return number of records the SQL Statement changed
	 * @throws SQLException
	 */
	public int sessionStatusSet(String name, int status)
	throws SQLException
	{
		return sessionStatus(true, name, status);
	}

	/**
	 * UNSET the 'Status' field in the table 'DbxCentralSessions' (Table.CENTRAL_SESSIONS) to the passed status, which is a bitmap field.
	 * @param name    Name of the server
	 * @param status  Status (which is a bitmap), found in DbxCentralSessions.ST_*
	 * @return number of records the SQL Statement changed
	 * @throws SQLException
	 */
	public int sessionStatusUnSet(String name, int status)
	throws SQLException
	{
		return sessionStatus(false, name, status);
	}

	/** internal method called by sessionStatusSet/sessionStatusUnSet */
	private int sessionStatus(boolean doSet, String name, int status)
	throws SQLException
	{
		DbxConnection conn = getConnection(); // Get connection from a ConnectionPool
		try // block with: finally at end to return the connection to the ConnectionPool
		{
			String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
			String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection

			String tabName = CentralPersistWriterBase.getTableName(conn, null, Table.CENTRAL_SESSIONS, null, true);

			String sqlGet = 
					" select max(" + lq + "Status" + rq + ")" + 
					" from " + tabName + 
					" where " + lq + "ServerName" + rq + " = '" + name + "'";

			int rowc = 0;
			int currentStatus = 0;
			// autoclose: stmnt, rs
			try (Statement stmnt = conn.createStatement())
			{
				// set TIMEOUT
				stmnt.setQueryTimeout(_defaultQueryTimeout);

				// Execute and read result
				try (ResultSet rs = stmnt.executeQuery(sqlGet))
				{
					while (rs.next())
					{
						rowc++;
						currentStatus = rs.getInt(1);
					}
				}
			}
			if (rowc != 1)
			{
				throw new SQLException("Expected to find 1 row, but "+rowc+" was returned using SQL: "+sqlGet);
			}
			
			int newStatus = 0;
			if (doSet)
				newStatus = currentStatus | status; // set this bit(s)
			else
				newStatus = currentStatus & ~status; // erase this bit(s)

			String sqlApply = 
					" update " + tabName + 
					" set "   + lq + "Status"     + rq + " = " + newStatus + 
					" where " + lq + "ServerName" + rq + " = '" + name + "'";

			// autoclose: stmnt, rs
			try (Statement stmnt = conn.createStatement())
			{
				int count = stmnt.executeUpdate(sqlApply);
				_logger.info("Set Session status to "+newStatus+" for ServerName '"+name+"'. This affected "+count+" session rows. SQL Executed: "+sqlApply);
				return count;
			}
		}
		finally
		{
			releaseConnection(conn);

			// Notify any listeners that Sessions has been changed
			fireSessionChanges();
		}
	}



	/**
	 * Check if DBX Central database has a server named<br>
	 * The server names are cached, so this should be a cheep call
	 * 
	 * @param sessionName name of the server
	 * @return true | false
	 */
	public boolean hasServerSession(String sessionName)
	{
		refreshServerSessions(false);
		
		return _sessionNames.contains(sessionName);
	}
	
	public Set<String> getServerSessions()
	{
		refreshServerSessions(false);
		
		return _sessionNames;
	}

	private synchronized void refreshServerSessions(boolean force)
	{
		if (force)
			_sessionNames = null;
		
		if (_sessionNames == null)
		{
			_sessionNames = new LinkedHashSet<>();

			try
			{
				boolean onlyLast  = true;
				for (DbxCentralSessions s : getSessions( onlyLast, -1 ))
				{
					_sessionNames.add(s.getServerName());
				}
				
				// If LocalMetrics is enabled in DbxCentral... just add the name
				if (true)
					_sessionNames.add(LocalMetricsPersistWriterJdbc.LOCAL_METRICS_SCHEMA_NAME);

				// if we want to sort according to 'conf/SERVER_LIST'
				boolean sort = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_SERVER_LIST_SORT, DEFAULT_SERVER_LIST_SORT);
				if (sort)
				{
					File srvDescFile = new File(DbxCentralServerDescription.getDefaultFile());
					if (srvDescFile.exists())
					{
						// GET Entries
						Map<String, DbxCentralServerDescription> srvDescMap = new HashMap<>();
						try 
						{
							srvDescMap = DbxCentralServerDescription.getFromFile();
						}
						catch (IOException ex)
						{
							_logger.warn("Problems reading file '"+srvDescFile+"'. This is used to sort the 'sessions list'. Skipping this... Caught: "+ex);
						}
						
						// Sort if we have any entries
						if ( ! srvDescMap.isEmpty() )
						{
							Set<String> tmp = new LinkedHashSet<>();

							// Loop ServerDescMap
							//   - move entries from "current structure" into a "new/tmp structure"
							//   - entries in the "current structure" but NOT in the ServerDescMap, will be appended to "new/tmp structure"
							//   - at the end "swap" new/tmp->current
							for (String srvName : srvDescMap.keySet())
							{
								Iterator<String> i = _sessionNames.iterator();
								while (i.hasNext())
								{
									String entry = i.next();
									if (srvName.equals(entry))
									{
										tmp.add(entry);
										i.remove();
									}
								}
							}

							// copy all the entries that was NOT part of the SERVER_LIST file
							tmp.addAll(_sessionNames);
							
							// swap the structures
							_sessionNames = tmp;
						}
					}
				} // end: sort
			}
			catch (SQLException ex)
			{
				_logger.error("Problems getting sessions from DBX Central Database.", ex);
				_sessionNames = null;
			}
		}
	}
	/*---------------------------------------------------
	** END: Remove/cleanup functionality (should this be in the reader???)
	**---------------------------------------------------
	*/



	public void fireSessionChanges()
	{
		// Empty the sessions set, it will be populated on next call to: hasServerSession()
		_sessionNames = null;

		// TODO: create a add/remove listeners, so we can notify registered listeners about changes

		// Notify any listeners
//		for (SessionListenerInterface l : _sessionListeners)
//		{
//			l.sessionChanges(possiblyListOfCurrentSessions);
//		}
		if (ReceiverAlarmCheck.hasInstance())
		{
			ReceiverAlarmCheck.getInstance().refresh();
		}
	}










//	/*---------------------------------------------------
//	** BEGIN: Command Execution
//	**---------------------------------------------------
//	*/
//	////.... hmm... this looks a bit dodgy....
//	/// mayby rethink this
//
//	private boolean _asynchExec = false;
//	public void doXXX()
//	{
//		doXXX(_asynchExec);
//	}
//	public void doXXX(/*params*/ boolean asynchExec)
//	{
//		// Execute later
//		if ( asynchExec )
//		{
//			// prepare a reflection object and post on queue.
//			return;
//		}
//
//		// Execute at once
//		//-do the stuff
//	}
//	/*---------------------------------------------------
//	** END: Command Execution
//	**---------------------------------------------------
//	*/




//	/*---------------------------------------------------
//	** BEGIN: background thread
//	**---------------------------------------------------
//	*/
//	// start()
//	// shutdown()
//	// run()
//	//   - command executor (a method execution (using reflection) will be posted on the queue)
//	//       - readFromQueue (with timeout of X ms)
//	//   - look for new sessions
//	//       - if (foundNewOnes) fireNewSessionsIsAvalilable();
//	/*---------------------------------------------------
//	** END: background thread
//	**---------------------------------------------------
//	*/
//	/*---------------------------------------------------
//	** BEGIN: Command thread stuff
//	**---------------------------------------------------
//	*/
//	/** Queue that holds commands for the _readThread */
//	private BlockingQueue<QueueCommand> _cmdQueue   = new LinkedBlockingQueue<QueueCommand>();
//	private Thread         _readThread = null;
//	private boolean        _running    = false;
//	private int            _warnQueueSizeThresh = 10;
//
//	/** Place holder object for commands to be executed be the readThread */
//	private static class QueueCommand
//	{
//		private String    _cmd  = null;
//		private Timestamp _ts1  = null;
//		private Timestamp _ts2  = null;
//		private Timestamp _ts3  = null;
//		private int       _int1 = -1;
//		
//		QueueCommand(String cmd, Timestamp ts1, Timestamp ts2, Timestamp ts3, int int1) 
//		{
//			_cmd  = cmd;
//			_ts1  = ts1;
//			_ts2  = ts2;
//			_ts3  = ts3;
//			_int1 = int1;
//		}
//		QueueCommand(String cmd, Timestamp ts1, Timestamp ts2, Timestamp ts3) 
//		{
//			this(cmd, ts1, ts2, ts3, -1);
//		}
////		QueueCommand(String cmd, Timestamp ts1, Timestamp ts2) 
////		{
////			this(cmd, ts1, ts2, null, -1);
////		}
//		QueueCommand(String cmd, Timestamp ts1) 
//		{
//			this(cmd, ts1, null, null, -1);
//		}
//		QueueCommand(String cmd) 
//		{
//			this(cmd, null, null, null, -1);
//		}
//	}
//
//	private void addCommand(QueueCommand qcmd)
//	{
//		int qsize = _cmdQueue.size();
//		if (qsize > _warnQueueSizeThresh)
//		{
//			_logger.warn("The Command queue has "+qsize+" entries. The CommandExecutor might not keep in pace.");
//		}
//
//		_cmdQueue.add(qcmd);
//	}
//
//	public void shutdown()
//	{
//		_running = false;
//		if (_readThread != null)
//			_readThread.interrupt();
//		_readThread = null;
//	}
//	public void start()
//	{
//		if (_readThread != null)
//		{
//			_logger.info("The thread '"+_readThread.getName()+"' is already running. Skipping the start.");
//			return;
//		}
//
//		_readThread = new Thread(this);
//		_readThread.setName("OfflineSessionReader");
//		_readThread.setDaemon(true);
//		_readThread.start();
////		SwingUtilities.invokeLater(this);
//	}
//	public boolean isRunning()
//	{
//		return _running;
//	}
//
//	@Override
//	public void run()
//	{
//		String threadName = _readThread.getName();
//		_logger.info("Starting a thread for the module '"+threadName+"'.");
//
//		_running = true;
//
//		Configuration conf = Configuration.getCombinedConfiguration();
//		boolean checkforNewSamples = conf.getBooleanProperty("pcs.read.checkForNewSessions", false);
//
//		while(_running)
//		{
//			if (_logger.isDebugEnabled())
//				_logger.debug("Thread '"+threadName+"', waiting on queue...");
//
//			try 
//			{
////				QueueCommand qo = _cmdQueue.take();
//				QueueCommand qo = _cmdQueue.poll(2000, TimeUnit.MILLISECONDS);
//
//				// Make sure the container isn't empty.
//				// If poll is used, then NULL means poll-timeout
//				// lets go and check for new samples (if data is inserted to the PCS at the same time we are reading from it)
//				if (qo == null)
//				{
//					if (checkforNewSamples && isConnected() && _running)
//					{
//						_numOfSamplesNow = getNumberOfSamplesInLastSession(false);
//						if (_numOfSamplesNow > _numOfSamplesOnLastRefresh)
//						{
//							if (_logger.isDebugEnabled())
//								_logger.debug("New samples is available: '"+(_numOfSamplesNow-_numOfSamplesOnLastRefresh)+"'. _numOfSamplesNow='"+_numOfSamplesNow+"', _numOfSamplesOnLastRefresh='"+_numOfSamplesOnLastRefresh+"'.");
//							setWatermark( (_numOfSamplesNow-_numOfSamplesOnLastRefresh) + " New samples is available.");
//						}
//					}
//					continue;
//				}
//				long xStartTime = System.currentTimeMillis();
//
//				// Known commands
//				// addCommand(new QueueCommand("loadTimelineSlider", sl.getSampleId(), sl.getPeriodStartTime(), sl.getPeriodEndTime()));
//				// addCommand(new QueueCommand("loadSessionGraphs",  sl.getSampleId(), sl.getPeriodStartTime(), sl.getPeriodEndTime()));
//				// addCommand(new QueueCommand("loadSessionCms", ts, null, null));
//
//				// DO WORK
//				String cmdStr = qo._cmd + "(ts1="+qo._ts1+", ts2="+qo._ts2+", ts3="+qo._ts3+")";
//				if      (CMD_loadTimelineSlider     .equals(qo._cmd)) execLoadTimelineSlider     (qo._ts1, qo._ts2, qo._ts3);
//				else if (CMD_loadSessionGraphs      .equals(qo._cmd)) execLoadSessionGraphs      (qo._ts1, qo._ts2, qo._ts3, qo._int1);
//				else if (CMD_loadSessionCms         .equals(qo._cmd)) execLoadSessionCms         (qo._ts1);
//				else if (CMD_loadSessionCmIndicators.equals(qo._cmd)) execLoadSessionCmIndicators(qo._ts1);
//				else if (CMD_loadSummaryCm          .equals(qo._cmd)) execLoadSummaryCm          (qo._ts1);
//				else if (CMD_loadSessions           .equals(qo._cmd)) execLoadSessions();
//				else _logger.error("Unknown command '"+qo._cmd+"' was taken from the queue.");
//
//				long xStopTime = System.currentTimeMillis();
//				long consumeTimeMs = xStopTime-xStartTime;
//				_logger.debug("It took "+TimeUtils.msToTimeStr(consumeTimeMs)+" to execute: "+cmdStr);
//				
//			} 
//			catch (InterruptedException ex) 
//			{
//				_running = false;
//			}
//			catch (Throwable t)
//			{
//				_logger.error("The thread '"+threadName+"' ran into unexpected problems, but continues, Caught: "+t, t);
//			}
//		}
//
//		_logger.info("Emptying the queue for module '"+threadName+"', which had "+_cmdQueue.size()+" entries.");
//		_cmdQueue.clear();
//
//		_logger.info("Thread '"+threadName+"' was stopped.");
//	}
//
////	public void run() 
////	{
////		_running = true;
////		while(true)
////		{
////			try
////			{
////			}
////			catch (Throwable t)
////			{
////			}
////		}
////	}
//
//	/**
//	 * Set execution mode for this module
//	 * @param execMode EXEC_MODE_DIRECT to execute in current thread, EXEC_MODE_BG to execute on the commands event thread
//	 */
//	public void setExecMode(int execMode)
//	{
//		if (execMode != EXEC_MODE_BG && execMode != EXEC_MODE_DIRECT)
//			throw new IllegalArgumentException("Setting exec mode to a unknow value of '"+execMode+"'.");
//		_execMode = execMode;
//	}
//
//	/**
//	 * What execution mode are we in.
//	 * @return EXEC_MODE_BG     if we the executor thread is running and execMode is in EXEC_MODE_BG
//	 *         EXEC_MODE_DIRECT if we are in direct mode.
//	 */
//	public int getExecMode()
//	{
//		if (_running && _execMode == EXEC_MODE_BG)
//			return EXEC_MODE_BG;
//		else
//			return EXEC_MODE_DIRECT;
//	}
//
//
//	/*---------------------------------------------------
//	** END: Command thread stuff
//	**---------------------------------------------------
//	*/





//	/*---------------------------------------------------
//	** BEGIN: public execution/access methods
//	**---------------------------------------------------
//	*/
//	public void loadTimelineSlider(Timestamp sampleId, Timestamp startTime, Timestamp endTime)
//	{
//		if (getExecMode() == EXEC_MODE_DIRECT)
//		{
//			execLoadTimelineSlider(sampleId, startTime, endTime);
//			return;
//		}
//		
//		addCommand(new QueueCommand(CMD_loadTimelineSlider,  sampleId, startTime, endTime));
//	}
//
//	public void loadSessionGraphs(Timestamp sampleId, Timestamp startTime, Timestamp endTime, int expectedRows)
//	{
//		if (getExecMode() == EXEC_MODE_DIRECT)
//		{
//			execLoadSessionGraphs(sampleId, startTime, endTime, expectedRows);
//			return;
//		}
//		
//		addCommand(new QueueCommand(CMD_loadSessionGraphs,  sampleId, startTime, endTime, expectedRows));
//	}
//
//	public void loadSessionCms(Timestamp sampleId)
//	{
//		if (getExecMode() == EXEC_MODE_DIRECT)
//		{
//			execLoadSessionCms(sampleId);
//			return;
//		}
//
//		addCommand(new QueueCommand(CMD_loadSessionCms, sampleId));
//	}
//	
//	/** 
//	 * Load indicators for this sample<br>
//	 * NOTE: This will not use the background thread to get data, since various timing issues.
//	 * If we are doing it with the background thread, the data might not show up "in time"
//	 * so the GUI will simply say "no data for this sample". 
//	 * Another thing is if you issue a lot of request if would end up in a loop on the slider 
//	 * so it looks like it's just "hopping around" in a indefinite loop due to various 
//	 * component actions get's initiated and triggers new action events which then is
//	 * handled in a differed way/mode.
//	 */
//	public void loadSessionCmIndicators(Timestamp sampleId)
//	{
//		execLoadSessionCmIndicators(sampleId);
////		if (getExecMode() == EXEC_MODE_DIRECT)
////		{
////			execLoadSessionCmIndicators(sampleId);
////			return;
////		}
////
////		addCommand(new QueueCommand(CMD_loadSessionCmIndicators, sampleId));
//	}
//
//	public void loadSummaryCm(Timestamp sampleId)
//	{
//		if (getExecMode() == EXEC_MODE_DIRECT)
//		{
//			execLoadSummaryCm(sampleId);
//			return;
//		}
//
//		addCommand(new QueueCommand(CMD_loadSummaryCm, sampleId));
//	}
//
//	public void loadSessions()
//	{
//		if (getExecMode() == EXEC_MODE_DIRECT)
//		{
//			execLoadSessions();
//			return;
//		}
//
//		addCommand(new QueueCommand(CMD_loadSessions));
//	}
//
//	public void loadMonTablesDictionary(Timestamp sampleId)
//	{
////		throw new RuntimeException("NOT IMPLEMENTED: loadMonTablesDictionary()");
//// OK NOT YET TESTED...
//		if (sampleId == null)
//			throw new IllegalArgumentException("sampleId can't be null");
//		
//		HashMap<String,MonTableEntry> monTablesMap = new HashMap<String,MonTableEntry>();
//
//		String monTables       = PersistWriterBase.getTableName(PersistWriterBase.SESSION_MON_TAB_DICT,     null, true);
//		String monTableColumns = PersistWriterBase.getTableName(PersistWriterBase.SESSION_MON_TAB_COL_DICT, null, true);
//
//		final String SQL_TABLES                = "select \"TableID\", \"Columns\", \"Parameters\", \"Indicators\", \"Size\", \"TableName\", \"Description\" from "+monTables;
//		final String SQL_COLUMNS               = "select \"TableID\", \"ColumnID\", \"TypeID\", \"Precision\", \"Scale\", \"Length\", \"Indicators\", \"TableName\", \"ColumnName\", \"TypeName\", \"Description\" from "+monTableColumns;
//
//		String sessionStartTime = sampleId.toString();
//		String sql = null;
//		try
//		{
//			Statement stmt = _conn.createStatement();
//			sql = SQL_TABLES + " where \"SessionStartTime\" = '"+sessionStartTime+"'";
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
//				monTablesMap.put(entry._tableName, entry);
//			}
//			rs.close();
//		}
//		catch (SQLException ex)
//		{
//			if (ex.getMessage().contains("not found"))
//			{
//				_logger.warn("Tooltip on column headers wasn't available in the offline database. This simply means that tooltip wont be showed in various places.");
//				return;
//			}
//			_logger.error("MonTablesDictionary:initialize:sql='"+sql+"'", ex);
//			return;
//		}
//
//		for (Map.Entry<String,MonTableEntry> mapEntry : monTablesMap.entrySet()) 
//		{
//		//	String        key           = mapEntry.getKey();
//			MonTableEntry monTableEntry = mapEntry.getValue();
//			
//			if (monTableEntry._monTableColumns == null)
//				monTableEntry._monTableColumns = new HashMap<String,MonTableColumnsEntry>();
//			else
//				monTableEntry._monTableColumns.clear();
//
//			try
//			{
//				Statement stmt = _conn.createStatement();
//				sql = SQL_COLUMNS + " where \"SessionStartTime\" = '"+sessionStartTime+"' and \"TableName\" = '"+monTableEntry._tableName+"'";
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
//				if (ex.getMessage().contains("not found"))
//				{
//					_logger.warn("Tooltip on column headers wasn't available in the offline database. This simply means that tooltip wont be showed in various places.");
//					return;
//				}
//				_logger.error("MonTablesDictionary:initialize:sql='"+sql+"'", ex);
//				return;
//			}
//		}
//
//		MonTablesDictionaryManager.getInstance().setMonTablesDictionaryMap(monTablesMap);
//	}
//
//	/**
//	 * Get properties about UDC, User Defined Counters stored in the offline database
//	 * @param sampleId session start time
//	 */
//	public Configuration getUdcProperties(Timestamp sampleId)
//	{
//		String sql = 
//			"select \"ParamName\", \"ParamValue\" \n" +
//			"from \"MonSessionParams\" \n" +
//			"where 1=1 \n" +
//			"  and \"SessionStartTime\" = '"+sampleId+"' \n" +
//			"  and \"Type\"            in ('system.config', 'user.config', 'temp.config') \n" +
//			"  and \"ParamName\"     like 'udc.%' \n";
//
//		try
//		{
//			Statement stmnt = _conn.createStatement();
//			ResultSet rs = stmnt.executeQuery(sql);
//
//			Configuration conf = new Configuration();
//			while (rs.next())
//			{
//				String key = rs.getString(1);
//				String val = rs.getString(2);
//				
//				conf.setProperty(key, val);
//			}
//
//			rs.close();
//			stmnt.close();
//			
//			return conf;
//		}
//		catch (SQLException e)
//		{
//			_logger.error("Problems getting UDC parameters for sample '"+sampleId+"'.", e);
//			return null;
//		}
//	}
//	
//	
//
//	/**
//	 * Get timestamp for PREVIOUS (rewind) cmName that has any data 
//	 * @param sampleId
//	 * @param currentSampleTime
//	 * @param cmName
//	 * @return
//	 */
//	public Timestamp getPrevSample(Timestamp sampleId, Timestamp currentSampleTime, String cmName)
//	{
//		cmName = getNameTranslateCmToDb(cmName);
//		// Rewind to previous cmName that has data
////		String sql = 
////			"select top 1 \"SessionSampleTime\" \n" +
////			"from \"MonSessionSampleDetailes\" \n" +
//////			"where \"SessionStartTime\"  = ? \n" +
////			"where 1=1 \n" +
////			"  and \"SessionSampleTime\" < ? \n" +
////			"  and \"CmName\"            = ? \n" +
////			"  and (\"absRows\" > 0 or \"diffRows\" > 0 or \"rateRows\" > 0) \n" +
////			"order by \"SessionSampleTime\" desc";
//		String sql = 
//			"select top 1 \"SessionSampleTime\" \n" +
//			"from \"MonSessionSampleDetailes\" \n" +
////			"where \"SessionStartTime\"  = '"+sampleId+"' \n" +
//			"where 1=1 \n" +
//			"  and \"SessionSampleTime\" < '"+currentSampleTime+"' \n" +
//			"  and \"CmName\"            = '"+cmName+"' \n" +
//			"  and (\"absRows\" > 0 or \"diffRows\" > 0 or \"rateRows\" > 0) \n" +
//			"order by \"SessionSampleTime\" desc";
////System.out.println("getPrevSample.sql=\n"+sql);
//		try
//		{
////			PreparedStatement pstmnt = _conn.prepareStatement(sql);
//////			pstmnt.setString(1, sampleId.toString());
//////			pstmnt.setString(2, currentSampleTime.toString());
//////			pstmnt.setString(3, cmName);
////			pstmnt.setString(1, currentSampleTime.toString());
////			pstmnt.setString(2, cmName);
////
////			ResultSet rs = pstmnt.executeQuery();
//			Statement stmnt = _conn.createStatement();
//			ResultSet rs = stmnt.executeQuery(sql);
//
//			// Only get first row
//			Timestamp ts = null;
//			if (rs.next())
//				ts = rs.getTimestamp(1);
//
//			rs.close();
//			stmnt.close();
//			
//			return ts;
//		}
//		catch (SQLException e)
//		{
//			_logger.error("Problems getting previous sample for '"+cmName+"'.", e);
//			return null;
//		}
//	}
////	-- REWIND
////	select top 1 'rew' as "xxx","SessionSampleTime" from "MonSessionSampleDetailes"
////	where "SessionStartTime" = '2010-05-03 11:51:29.513'
////	  and "SessionSampleTime" < '2010-05-03 13:00:06.166'
////	  and "CmName"='CMActiveStatements'
////	  and ("absRows" > 0 or "diffRows" > 0 or "rateRows" > 0)
////	order by "SessionSampleTime" desc
////
////	-- FAST-FORWARD
////	select top 1 'ff' as "xxx","SessionSampleTime" from "MonSessionSampleDetailes"
////	where "SessionStartTime" = '2010-05-03 11:51:29.513'
////	  and "SessionSampleTime" > '2010-05-03 13:00:06.166'
////	  and "CmName"='CMActiveStatements'
////	  and ("absRows" > 0 or "diffRows" > 0 or "rateRows" > 0)
////	order by "SessionSampleTime" asc
//
//	/**
//	 * Get timestamp for NEXT (fast forward) cmName that has any data 
//	 * @param sampleId
//	 * @param currentSampleTime
//	 * @param cmName
//	 * @return
//	 */
//	public Timestamp getNextSample(Timestamp sampleId, Timestamp currentSampleTime, String cmName)
//	{
//		cmName = getNameTranslateCmToDb(cmName);
//		// Fast Forward to next cmName that has data
////		String sql = 
////			"select top 1 \"SessionSampleTime\" \n" +
////			"from \"MonSessionSampleDetailes\" \n" +
//////			"where \"SessionStartTime\"  = ? \n" +
////			"where 1=1 \n" +
////			"  and \"SessionSampleTime\" > ? \n" +
////			"  and \"CmName\"            = ? \n" +
////			"  and (\"absRows\" > 0 or \"diffRows\" > 0 or \"rateRows\" > 0) \n" +
////			"order by \"SessionSampleTime\" asc";
//		String sql = 
//			"select top 1 \"SessionSampleTime\" \n" +
//			"from \"MonSessionSampleDetailes\" \n" +
////			"where \"SessionStartTime\"  = '"+sampleId+"' \n" +
//			"where 1=1 \n" +
//			"  and \"SessionSampleTime\" > '"+currentSampleTime+"' \n" +
//			"  and \"CmName\"            = '"+cmName+"' \n" +
//			"  and (\"absRows\" > 0 or \"diffRows\" > 0 or \"rateRows\" > 0) \n" +
//			"order by \"SessionSampleTime\" asc";
////System.out.println("getNextSample.sql=\n"+sql);
//
//		try
//		{
////			PreparedStatement pstmnt = _conn.prepareStatement(sql);
//////			pstmnt.setString(1, sampleId.toString());
//////			pstmnt.setString(2, currentSampleTime.toString());
//////			pstmnt.setString(3, cmName);
////			pstmnt.setString(1, currentSampleTime.toString());
////			pstmnt.setString(2, cmName);
////
////			ResultSet rs = pstmnt.executeQuery();
//			Statement stmnt = _conn.createStatement();
//			ResultSet rs = stmnt.executeQuery(sql);
//
//			// Only get first row
//			Timestamp ts = null;
//			if (rs.next())
//				ts = rs.getTimestamp(1);
//
//			rs.close();
//			stmnt.close();
//			
//			return ts;
//		}
//		catch (SQLException e)
//		{
//			_logger.error("Problems getting next sample for '"+cmName+"'.", e);
//			return null;
//		}
//	}
//
//	@SuppressWarnings("unused")
//	public int getNumberOfSamplesInLastSession(boolean printErrors)
//	{
////		 SessionStartTime              ServerName                     NumOfSamples LastSampleTime
////		 ----------------------------- ------------------------------ ------------ -----------------------------
////		 Apr 11 2011  4:53:09.006000PM GORAN_1_DS                              410 Apr 11 2011  5:28:40.066000PM
////		 Apr 11 2011  5:29:09.256000PM GORAN_1_DS                               13 Apr 11 2011  5:30:17.286000PM
//
//		String sql = 
//			"select * \n" +
//			"from \"MonSessions\" \n" +
//			"order by \"LastSampleTime\" \n";
//		try
//		{
//			Statement stmnt = _conn.createStatement();
//			ResultSet rs = stmnt.executeQuery(sql);
//
//			// Get all rows, but we only save the last rows...
//			// I could have done: select top 1 * from MonSessions  order by LastSampleTime desc
//			// but that isn't that portal as getting all records... 
//			// Note: It should only be a few rows in the table anyway... 
//			Timestamp sessionStartTime = null;
//			String    serverName       = null;
//			int       numOfSamples     = -1;
//			Timestamp lastSampleTime = null;
//
//			while (rs.next())
//			{
//				sessionStartTime = rs.getTimestamp(1);
//				serverName       = rs.getString   (2);
//				numOfSamples     = rs.getInt      (3);
//				lastSampleTime   = rs.getTimestamp(4);
//			}
//
//			rs.close();
//			stmnt.close();
//			
//			return numOfSamples;
//		}
//		catch (SQLException e)
//		{
//			String msg = e.getMessage();
//			if (msg != null && msg.toLowerCase().indexOf("timeout") >= 0)
//			{
//				_logger.info ("Got 'timeout' when reading the MonSessions table, retrying this later.");
//				_logger.debug("Got 'timeout' when reading the MonSessions table, retrying this later. Message: "+msg);
//				return -1;
//			}
//
//			if (printErrors)
//				_logger.error("Problems getting number of samples from last session sample.", e);
//			return -1;
//		}
//	}
//	/*---------------------------------------------------
//	** END: public execution/access methods
//	**---------------------------------------------------
//	*/
//
//	
//	
//	/*---------------------------------------------------
//	** BEGIN: 
//	**---------------------------------------------------
//	*/
//	/** containes OfflineCm */
//	private LinkedHashMap<String,OfflineCm> _offlineCmMap = null;
//
//	private class OfflineCm
//	{
//		public String             name      = "";
//		public boolean            hasAbs    = false;
//		public boolean            hasDiff   = false;
//		public boolean            hasRate   = false;
//		public LinkedList<String> graphList = null;
//
//		public OfflineCm(String name) 
//		{
//			this.name = name;
//		}
//		public void add(String name, String type)
//		{
//			if (type == null)
//				return;
//			if      (type.equals("abs"))  hasAbs  = true;
//			else if (type.equals("diff")) hasDiff = true;
//			else if (type.equals("rate")) hasRate = true;
//			else
//			{
//				if (graphList == null)
//					graphList = new LinkedList<String>();
//				graphList.add(type);
//			}
//		}
//		@Override
//		public String toString()
//		{
//			return "OfflineCm(name='"+name+"', hasAbs="+hasAbs+", hasDiff="+hasDiff+", hasRate="+hasRate+", graphList="+graphList+")";
//		}
//	}
//	private void addOfflineCm(String name, String type)
//	{
//		OfflineCm ocm = _offlineCmMap.get(name);
//		if (ocm == null)
//		{
//			ocm = new OfflineCm(name);
//			_offlineCmMap.put(ocm.name, ocm);
//		}
//		ocm.add(name, type);
//	}
//
//	private void getStoredCms(boolean refresh)
//	{
//		// No need to refresh this list
//		if (_offlineCmMap != null && !refresh)
//			return;
//
//		_offlineCmMap = new LinkedHashMap<String,OfflineCm>();
//		ResultSet rs = null;
//
//		try 
//		{
//			// Obtain a DatabaseMetaData object from our current connection
//			DatabaseMetaData dbmd = _conn.getMetaData();
//	
//	//		rs = dbmd.getTables(null, null, "%", null);
//			rs = dbmd.getTables(null, null, "%", new String[] { "TABLE" });
//
//			while(rs.next())
//			{
//				String tableName = rs.getString("TABLE_NAME");
//				String tableType = rs.getString("TABLE_TYPE");
//	
//				int sepPos = tableName.indexOf("_");
//				if (sepPos < 0)
//					continue;
//				String name = tableName.substring(0, sepPos);
//				String type = tableName.substring(sepPos+1);
//				if (_logger.isDebugEnabled())
//					_logger.debug("getStoredCms()-rs-row- TYPE='"+tableType+"', NAME='"+tableName+"'. name='"+name+"', type='"+type+"'");
//				
//				name = getNameTranslateDbToCm(name);
//				type = getNameTranslateDbToCm(type, true); // type can be a graph name...
//
//				addOfflineCm(name, type);
//			}
////			ResultSetTableModel tab = new ResultSetTableModel(rs);
////			System.out.println("getStoredCms()-3\n" + tab.toTableString());
//			rs.close();
//
//			// Sort the information according to the Counter Tab's in MainFarme
//			sortOfflineCm();
//
//			if (_logger.isDebugEnabled())
//				_logger.debug("_offlineCmMap="+_offlineCmMap);
//		}
//		catch (SQLException e)
//		{
//			_logger.error("Problems getting Offlined table names.", e);
//		}
//	}
//
//	/**
//	 * Sort all available tables in the same order as the "tab" placement in the Main GUI
//	 * TODO: sort it in the order they appear in the Summary Graph list (since we can order the graphs now)
//	 */
//	private void sortOfflineCm()
//	{
//		GTabbedPane tabPane = MainFrame.hasInstance() ? MainFrame.getInstance().getTabbedPane() : null;
//		if (tabPane == null)
//			return;
//
//		// New Map that we will add the sorted rows into
////		LinkedHashMap<String,OfflineCm> originOfflineCmMap = _offlineCmMap.clone();
//		LinkedHashMap<String,OfflineCm> originOfflineCmMap = new LinkedHashMap<String,OfflineCm>(_offlineCmMap);
//		LinkedHashMap<String,OfflineCm> sortedOfflineCmMap = new LinkedHashMap<String,OfflineCm>();
//
//		// Holds 'names' that wasn't found in the TAB, add those at the end
//		LinkedList<String> misses = new LinkedList<String>();
//
//		for (String tabName : tabPane.getAllTitles())
//		{			
//			Component comp = tabPane.getComponentAtTitle(tabName);
//			String name = comp.getName();
//
//			_logger.debug("sortOfflineCm() Working on tab named '"+tabName+", component name '"+name+"'");
//
//			// Get the OfflineCm for this TAB and store it in the new Map
//			OfflineCm ocm = originOfflineCmMap.get(name);
//			if (ocm == null)
//			{
//				// If the OCM can't be found in the list, it just means that
//				// The CM has no tables in the Counter Storage
//				// So we don't really need the code for "misses", but lets keep it for now.
//				//misses.add(name);
//			}
//			else
//			{
//				// move it over to the new sorted Map
//				sortedOfflineCmMap.put(name, ocm);
//				originOfflineCmMap.remove(name);
//
//				// If we have more than 1 graph, go and sort those things as well
//				if (ocm.graphList != null && ocm.graphList.size() > 1)
//				{
//					if (comp instanceof TabularCntrPanel)
//					{
//						TabularCntrPanel tcp = (TabularCntrPanel) comp;
//						CountersModel    cm  = tcp.getCm();
//						if (cm != null)
//						{
//							Map<String,TrendGraph> graphs = cm.getTrendGraphs();
//							if (graphs != null && graphs.size() > 1)
//							{
////								LinkedList<String> originGraphList = (LinkedList) ocm.graphList.clone();
//								LinkedList<String> originGraphList = new LinkedList<String>(ocm.graphList);
//								LinkedList<String> sortedGraphList = new LinkedList<String>();
//
//								// Loop how the Graphs was initially added to the CM
//								for (Map.Entry<String,TrendGraph> entry : graphs.entrySet()) 
//								{
//									String     key = entry.getKey();
//									//TrendGraph val = entry.getValue();
//
//									// move it over to the new sorted List
//									// But only if it exists in the originGraphList, otherwise we will add a graph that do not exist in the database
//									if (originGraphList.indexOf(key) > -1)
//									{
//										sortedGraphList.add(key);
//										originGraphList.remove(key);
//									}
//								}
//								// Check for errors, all entries should have been removed from the "work" map
//								if (originGraphList.size() > 0)
//								{
//									_logger.warn("The sorting of 'ocm.graphList' for ocm '"+name+"' failed. Continuing with old/unsorted List");
//									_logger.debug("sortOfflineCm() originGraphList("+originGraphList.size()+"): "+StringUtil.toCommaStr(originGraphList));
//									_logger.debug("sortOfflineCm() sortedGraphList("+sortedGraphList.size()+"): "+StringUtil.toCommaStr(sortedGraphList));
//									_logger.debug("sortOfflineCm() ocm.graphList  ("+ocm.graphList.size()+")  : "+StringUtil.toCommaStr(ocm.graphList));
//								}
//								else
//								{
//									ocm.graphList = sortedGraphList;
//								}
//							}
//						}
//					}
//				}
//			}
//		}
//		// Now move the ones left over to the 
//		if (misses.size() > 0)
//		{
//			for (Iterator<String> it = misses.iterator(); it.hasNext();)
//			{
//				String name = it.next();
//				OfflineCm ocm = originOfflineCmMap.get(name);
//
//				sortedOfflineCmMap.put(name, ocm);
//				originOfflineCmMap.remove(name);
//				it.remove();
//			}
//		}
//		// Check for errors, all entries should have been removed from the "work" map
//		if (originOfflineCmMap.size() > 0)
//		{
//			_logger.warn("The sorting of '_offlineCmMap' failed. Continuing with old/unsorted Map");
//			_logger.debug("sortOfflineCm() originOfflineCmMap("+originOfflineCmMap.size()+"): "+StringUtil.toCommaStr(originOfflineCmMap));
//			_logger.debug("sortOfflineCm() sortedOfflineCmMap("+sortedOfflineCmMap.size()+"): "+StringUtil.toCommaStr(sortedOfflineCmMap));
//			_logger.debug("sortOfflineCm() misses            ("+misses.size()            +"): "+StringUtil.toCommaStr(misses));
//			_logger.debug("sortOfflineCm() _offlineCmMap     ("+_offlineCmMap.size()     +"): "+StringUtil.toCommaStr(_offlineCmMap));
//		}
//		else
//		{
//			_offlineCmMap = sortedOfflineCmMap;
//		}
//		return;
//	}
//
//
//	/** Do name translation if Performance Counters has been saved with AseTune with SourceControlRevision */
//	private static final int NEED_NAME_TRANSLATION_BEFORE_SRC_VERSION = 280;
//
//	/**
//	 * Used to translate a DBName table into a CMName table
//	 * <p>
//	 * This is only done if we are reading a older database.
//	 */
//	private String getNameTranslateDbToCm(String name, boolean isGraph)
//	{
//		if (_monVersionInfo == null)
//			return name;
//		if (_monVersionInfo._sourceRev >= NEED_NAME_TRANSLATION_BEFORE_SRC_VERSION)
//			return name;
//		if ("abs".equals(name) || "diff".equals(name) || "rate".equals(name))
//			return name;
//
//		String translatedName = null;
//		if (isGraph)
//			translatedName = BackwardNameCompatibility.getOldToNewGraph(name, name);
//		else
//			translatedName = BackwardNameCompatibility.getOldToNew(name, name);
//
//		if (_logger.isDebugEnabled())
//			_logger.debug(" --> Translating name db->CM: '"+name+"' to '"+translatedName+"'.");
////System.out.println(" --> Translating name db->CM: '"+name+"' to '"+translatedName+"'.");
//
//		return translatedName;
//	}
//	private String getNameTranslateDbToCm(String name)
//	{
//		return getNameTranslateDbToCm(name, false);
//	}
//	/**
//	 * Used to translate a CMName table into a DBName table
//	 * <p>
//	 * This is only done if we are reading a older database.
//	 */
//	private String getNameTranslateCmToDb(String name, boolean isGraph)
//	{
//		if (_monVersionInfo == null)
//			return name;
//		if (_monVersionInfo._sourceRev >= NEED_NAME_TRANSLATION_BEFORE_SRC_VERSION)
//			return name;
//		if ("abs".equals(name) || "diff".equals(name) || "rate".equals(name))
//			return name;
//
//		String translatedName = null;
//		if (isGraph)
//			translatedName = BackwardNameCompatibility.getNewToOldGraph(name, name);
//		else
//			translatedName = BackwardNameCompatibility.getNewToOld(name, name);
//
//		if (_logger.isDebugEnabled())
//			_logger.debug(" <-- Translating name CM->db: '"+name+"' to '"+translatedName+"'.");
////System.out.println(" <-- Translating name CM->db: '"+name+"' to '"+translatedName+"'.");
//
//		return translatedName;
//	}
//	private String getNameTranslateCmToDb(String name)
//	{
//		return getNameTranslateCmToDb(name, false);
//	}
//	
//	private int loadSessionGraph(String cmName, String graphName, Timestamp sampleId, Timestamp startTime, Timestamp endTime, int expectedRows)
//	{
////		CountersModel cm = GetCounters.getInstance().getCmByName(cmName);
//		CountersModel cm = CounterController.getInstance().getCmByName(cmName);
//		if (cm == null)
//		{
//			_logger.warn("Can't find any CM named '"+cmName+"'.");
//			return 0;
//		}
//		TrendGraph tg = cm.getTrendGraph(graphName);
//		if (tg == null)
//		{
//			_logger.warn("Can't find any TrendGraph named '"+graphName+"', for the CM '"+cmName+"'.");
//			return 0;
//		}
//		tg.clearGraph();
//
//		// When doing dbAccess we need the database names
//		cmName    = getNameTranslateCmToDb(cmName);
//		graphName = getNameTranslateCmToDb(graphName, true);
//
//		//----------------------------------------
//		// TYPICAL look of a graph table
//		//----------------------------------------
//		// CREATE TABLE "CMengineActivity_cpuSum"
//		//   "SessionStartTime"   DATETIME        NOT NULL,
//		//   "SessionSampleTime"  DATETIME        NOT NULL,
//		//   "SampleTime"         DATETIME        NOT NULL,
//		//   "label_0"            VARCHAR(30)         NULL,
//		//   "data_0"             NUMERIC(10, 1)      NULL,
//		//   "label_1"            VARCHAR(30)         NULL,
//		//   "data_1"             NUMERIC(10, 1)      NULL,
//		//   "label_2"            VARCHAR(30)         NULL,
//		//   "data_2"             NUMERIC(10, 1)      NULL
//
//		String sql = "select * from \""+cmName+"_"+graphName+"\" " +
//		             "where \"SessionStartTime\" = ? " +
//		             "  AND \"SessionSampleTime\" >= ? " +
//		             "  AND \"SessionSampleTime\" <= ? " +
//		             "order by \"SessionSampleTime\"";
//
//		// If we expect a big graph, load only every X row
//		// if we add to many to the graph, the JVM takes 100% CPU, I'm guessing it 
//		// has to do too many repaints, we could do an "average" of X rows during the load
//		// but I took the easy way out... (or figure out why it's taking all the CPU)
//		int loadEveryXRow = expectedRows / 1000 + 1; // 1 = load every row
//
//		try
//		{
//			long fetchStartTime = System.currentTimeMillis();
//			setStatusText("Loading graph '"+graphName+"' for '"+cmName+"'.");
//
//			PreparedStatement pstmnt = _conn.prepareStatement(sql);
////			pstmnt.setTimestamp(1, sampleId);
////			pstmnt.setTimestamp(2, startTime);
////			pstmnt.setTimestamp(3, endTime);
//			pstmnt.setString(1, sampleId.toString());
//			pstmnt.setString(2, startTime.toString());
//			pstmnt.setString(3, endTime.toString());
//
//			_logger.debug("loadSessionGraph(cmName='"+cmName+"', graphName='"+graphName+"') loadEveryXRow="+loadEveryXRow+": "+pstmnt);
//
//			ResultSet rs = pstmnt.executeQuery();
//			ResultSetMetaData rsmd = rs.getMetaData();
//
//			int cols = rsmd.getColumnCount();
//			String[] labels = new String[(cols-3)/2];
//			Double[] datapt = new Double[(cols-3)/2];
//			boolean firstRow = true;
//			int row = 0;
//
////			Timestamp sessionStartTime  = null;
//			Timestamp sessionSampleTime = null;
////			Timestamp sampleTime        = null;
//			// do not render while we addPoints
//			//tg.setVisible(false);
//
//			while (rs.next())
//			{
////				sessionStartTime  = rs.getTimestamp(1);
//				sessionSampleTime = rs.getTimestamp(2);
////				sampleTime        = rs.getTimestamp(3);
//
////System.out.println("loadSessionGraph(): READ(row="+row+"): graphName='"+graphName+"', sampleId="+sampleId+", sessionSampleTime="+sessionSampleTime);
//				// Start to read column 4
//				// move c (colIntex) 2 cols at a time, move ca (ArrayIndex) by one
//				for (int c=4, ca=0; c<=cols; c+=2, ca++)
//				{
//					labels[ca] = rs.getString(c);
//					datapt[ca] = Double.valueOf(rs.getDouble(c+1));					
//				}
//				// Add a extra record at the BEGINING of the traces... using 0 data values
//				if (firstRow)
//				{
//					firstRow = false;
//					Double[] firstDatapt = new Double[datapt.length];
//					for (int d=0; d<firstDatapt.length; d++)
//						firstDatapt[d] = Double.valueOf(0);
//					tg.addPoint(new Timestamp(sessionSampleTime.getTime()-10),  // - 10 millisec
//							firstDatapt, 
//							labels, null, startTime, endTime);
//				}
//
//				// If we expect a big graph, load only every X row
//				// if we add to many to the graph, the JVM takes 100% CPU, I'm guessing it 
//				// has to do too many repaints, we could do an "average" of X rows during the load
//				// but I took the easy way out... (or figure out why it's taking all the CPU)
//				if ( row % loadEveryXRow == 0 )
//				{
////System.out.println("loadSessionGraph(): ADD (row="+row+"): graphName='"+graphName+"', sampleId="+sampleId+", sessionSampleTime="+sessionSampleTime);
//					tg.addPoint(sessionSampleTime, datapt, labels, null, startTime, endTime);
//				}
//
//				row++;
//			}
//			rs.close();
//			pstmnt.close();
//
//			// Add a extra record at the end of the traces... using 0 data values
//			if (sessionSampleTime != null)
//			{
//				Double[] lastDatapt = new Double[datapt.length];
//				for (int d=0; d<lastDatapt.length; d++)
//					lastDatapt[d] = Double.valueOf(0);
//				tg.addPoint(new Timestamp(sessionSampleTime.getTime()+10), // + 10 millisec
//						lastDatapt, 
//						labels, null, startTime, endTime);
//			}
////System.out.println("Loaded "+row+" rows into TrendGraph named '"+graphName+"', for the CM '"+cmName+"', which took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.");
//			_logger.debug("Loaded "+row+" rows into TrendGraph named '"+graphName+"', for the CM '"+cmName+"', which took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.");
//			setStatusText("");
//			
//			tg.setMinimumChartArea();
//			
//			return loadEveryXRow;
//		}
//		catch (SQLException e)
//		{
//			_logger.error("Problems loading graph for cm='"+cmName+"', graph='"+graphName+"'.", e);
//		}
//		finally 
//		{
//			// restore rendering
//			//tg.setVisible(true);
//		}
//		return 0;
//	}
//
//	private void setAllChartRendering(boolean toValue)
//	{
//		for (Map.Entry<String,OfflineCm> entry : _offlineCmMap.entrySet())
//		{
//			String cmName = entry.getKey();
//			OfflineCm ocm = entry.getValue();
//
//			//System.out.println("loadSessionGraphs(): LOOP, cmName='"+cmName+"', ocm='"+ocm+"'.");
//			if (ocm == null)           continue; // why should this happen
//			if (ocm.graphList == null) continue;
//
//			for (String graphName : ocm.graphList)
//			{
////				loadEveryXRow = loadSessionGraph(cmName, graphName, sampleId, startTime, endTime, expectedRows);
//				CountersModel cm = CounterController.getInstance().getCmByName(cmName);
//				if (cm == null)
//				{
//					_logger.warn("Can't find any CM named '"+cmName+"'.");
//					continue;
//				}
//				TrendGraph tg = cm.getTrendGraph(graphName);
//				if (tg == null)
//				{
//					_logger.warn("Can't find any TrendGraph named '"+graphName+"', for the CM '"+cmName+"'.");
//					continue;
//				}
//				tg.setVisible(toValue);
//			}
//		}
//		
//	}
//
//	private void execLoadSessionGraphs(Timestamp sampleId, Timestamp startTime, Timestamp endTime, int expectedRows)
//	{
//		setWatermark("Loading graphs...");
////		System.out.println("loadSession(sampleId='"+sampleId+"', startTime='"+startTime+"', endTime='"+endTime+"')");
//		long xStartTime = System.currentTimeMillis();
//
//		// Get what version of the tool that stored the information
//		_monVersionInfo = getMonVersionInfo(sampleId);
//		
//		// Populate _offlineCmMap
//		getStoredCms(false);
//
//		int loadEveryXRow = 0;
//
//		try
//		{
//			// Disable rendering of charts
//			setAllChartRendering(false);
//			
//			// Write "HH:mm - HH:mm" of what we are watching in the MainFrame's watermark
//			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
//			String graphWatermark = sdf.format(startTime) + " - " + sdf.format(endTime);
//			MainFrame.setOfflineSamplePeriodText(graphWatermark);
//
//			// Now loop the _offlineCmMap
//			for (Map.Entry<String,OfflineCm> entry : _offlineCmMap.entrySet())
//			{
//				String cmName = entry.getKey();
//				OfflineCm ocm = entry.getValue();
//
//				//System.out.println("loadSessionGraphs(): LOOP, cmName='"+cmName+"', ocm='"+ocm+"'.");
//				if (ocm == null)           continue; // why should this happen
//				if (ocm.graphList == null) continue;
//
//				for (String graphName : ocm.graphList)
//				{
////System.out.println("execLoadSessionGraphs(): cmName="+cmName+", graphName="+graphName+", sampleId="+sampleId+", startTime="+startTime+", endTime="+endTime+", expectedRows="+expectedRows+"");
//					loadEveryXRow = loadSessionGraph(cmName, graphName, sampleId, startTime, endTime, expectedRows);
//				}
//			}
//			String str = "Loading all TrendGraphs took '"+TimeUtils.msToTimeStr("%SS.%ms", System.currentTimeMillis()-xStartTime)+"' seconds.";
//			if (loadEveryXRow > 1)
//				str += " Loaded every "+(loadEveryXRow-1)+" row, graphs was to big.";
//			setStatusText(str);
//			setWatermark();
//		}
//		finally 
//		{
//			// Enable rendering of charts
//			setAllChartRendering(true);
//		}
//	}
//
//	@SuppressWarnings("unused")
//	private void execLoadTimelineSlider(Timestamp sampleId, Timestamp startTime, Timestamp endTime)
//	{
//		long xStartTime = System.currentTimeMillis();
//		setWatermark("Refreshing Timeline slider...");
//
//		MainFrame mf = MainFrame.getInstance();
//		if ( mf == null )
//			return;
//
//		mf.resetOfflineSlider();
//		
//		String sql = "select \"SessionSampleTime\" " +
//		             "from " + PersistWriterBase.getTableName(PersistWriterBase.SESSION_SAMPLES, null, true) + " "+
//		             "where \"SessionStartTime\" = ? " +
//		             "  AND \"SessionSampleTime\" >= ? " +
//		             "  AND \"SessionSampleTime\" <= ? " +
//		             "order by \"SessionSampleTime\"";
//		try
//		{
//			PreparedStatement pstmnt = _conn.prepareStatement(sql);
////			pstmnt.setTimestamp(1, sampleId);
////			pstmnt.setTimestamp(2, startTime);
////			pstmnt.setTimestamp(3, endTime);
//			pstmnt.setString(1, sampleId.toString());
//			pstmnt.setString(2, startTime.toString());
//			pstmnt.setString(3, endTime.toString());
//
//			ResultSet rs = pstmnt.executeQuery();
//
//			ArrayList<Timestamp> tsList = new ArrayList<Timestamp>();
//			while (rs.next())
//			{
//				Timestamp sessionSampleTime = rs.getTimestamp(1);
//				tsList.add(sessionSampleTime);
////				mf.addOfflineSliderEntry(sessionSampleTime);
//			}
//
//			rs.close();
//			pstmnt.close();
//
//			mf.addOfflineSliderEntryList(tsList);
//		}
//		catch (SQLException e)
//		{
//			_logger.error("Problems loading Timeline slider.", e);
//		}
////System.out.println("loadTimelineSlider(sampleId='"+sampleId+"', startTime='"+startTime+"', endTime='"+endTime+"'), which took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-xStartTime)+"'.");
//		setWatermark();
//	}
//
//	private void execLoadSessionCms(Timestamp sampleId)
//	{
//		setWatermark("Loading Counters...");
////		System.out.println("loadSessionCms(sampleId='"+sampleId+"')");
//
//		long fetchStartTime = System.currentTimeMillis();
//		setStatusText("Loading all counters for time '"+sampleId+"'.");
//
//		// Populate _offlineCmMap
//		getStoredCms(false);
//
//		// Now loop the _offlineCmMap
//		for (Map.Entry<String,OfflineCm> entry : _offlineCmMap.entrySet()) 
//		{
//			//String cmName = entry.getKey();
//			OfflineCm ocm = entry.getValue();
//
//			loadSessionCm(ocm, sampleId);
//		}
//
//		String str = "Loading took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.";
//		_logger.debug(str);
//		setStatusText(str);
//		setWatermark();
//	}
//	private void loadSessionCm(OfflineCm ocm, Timestamp sampleTs)
//	{
//		if (ocm == null || sampleTs == null)
//			throw new IllegalArgumentException("OfflineCm or sampleTs cant be null");
//
//		String cmName = ocm.name;
////		CountersModel cm = GetCounters.getInstance().getCmByName(cmName);
//		CountersModel cm = CounterController.getInstance().getCmByName(cmName);
//		if (cm == null)
//		{
//			_logger.warn("Can't find any CM named '"+cmName+"'.");
//			return;
//		}
//		
//		// Remove all the rows in the CM, so that new can be added
//		// if this is not done, all the old rows will still be visible when displaying it in the JTable
//		cm.clearForRead();
////System.out.println("loadSessionCm()|abs="+ocm.hasAbs+",diff="+ocm.hasDiff+",rate="+ocm.hasRate+"| cm.getName()='"+cm.getName()+"', cmName='"+cmName+"'.");
//
//		if (ocm.hasAbs)  loadSessionCm(cm, CountersModel.DATA_ABS,  sampleTs);
//		if (ocm.hasDiff) loadSessionCm(cm, CountersModel.DATA_DIFF, sampleTs);
//		if (ocm.hasRate) loadSessionCm(cm, CountersModel.DATA_RATE, sampleTs);
//
//		cm.setDataInitialized(true);
//		cm.fireTableStructureChanged();
//		if (cm.getTabPanel() != null)
//			cm.getTabPanel().adjustTableColumnWidth();
//	}
//	@SuppressWarnings("unused")
//	private void loadSessionCm(CountersModel cm, int type, Timestamp sampleTs)
//	{
//		if (cm       == null) throw new IllegalArgumentException("CountersModel can't be null");
//		if (sampleTs == null) throw new IllegalArgumentException("sampleTs can't be null");
//		
//		String cmName = getNameTranslateCmToDb(cm.getName());
//
//		String typeStr = null;
//		if      (type == CountersModel.DATA_ABS)  typeStr = "abs";
//		else if (type == CountersModel.DATA_DIFF) typeStr = "diff";
//		else if (type == CountersModel.DATA_RATE) typeStr = "rate";
//		else throw new IllegalArgumentException("Unknown type of "+type+".");
//
//		long fetchStartTime = System.currentTimeMillis();
//		setStatusText("Loading '"+typeStr+"' counters for '"+cmName+"'.");
//
//		//----------------------------------------
//		// TYPICAL look of a graph table
//		//----------------------------------------
//		// CREATE TABLE "CMengineActivity_abs"  << abs|diff|rate
//		//     "SessionStartTime"  DATETIME NOT NULL,
//		//     "SessionSampleTime" DATETIME NOT NULL,
//		//     "SampleTime"        DATETIME NOT NULL,
//		//     "SampleMs"          INT      NOT NULL,
//		//     "col1"              datatype     null,
//		//     "col2"              datatype     null,
//		//     "...."              datatype     null,
//		//
//		String sql = "select * from \""+cmName+"_"+typeStr+"\" " +
//		             "where \"SessionSampleTime\" = ? ";
//		String sql2 = "select * from \""+cmName+"_"+typeStr+"\" " +
//		             "where \"SessionSampleTime\" = '"+sampleTs+"' ";
//
//		try
//		{
////			PreparedStatement pstmnt = _conn.prepareStatement(sql);
////			pstmnt.setTimestamp(1, sampleTs);
////			pstmnt.setObject(1, sampleTs);
//			Statement pstmnt = _conn.createStatement();
//			
//
//			_logger.debug("loadSessionCm(cmName='"+cmName+"', type='"+typeStr+"', sampleTs='"+sampleTs+"'): "+pstmnt);
//
////			ResultSet rs = pstmnt.executeQuery();
//			ResultSet rs = pstmnt.executeQuery(sql2);
//			ResultSetMetaData rsmd = rs.getMetaData();
//			int cols = rsmd.getColumnCount();
//			int row  = 0;
//
////			Object oa[] = new Object[cols-4]; // Object Array
////			Object ha[] = new String[cols-4]; // Header Array  (Column Names)
//			int colSqlDataType[] = new int[cols];
//			List<String> colHead = new ArrayList<String>(cols);
//
//			int     startColPos = 5;
//			boolean hasNewDiffRateRowCol = false;
//
//			// Backward compatibility... for older recordings...
//			// A new column 'CmNewDiffRateRow' was added to all ABS/RATE/DIFF tables
//			// If this column exists, inrement the start position
//			String checkCol = rsmd.getColumnLabel(startColPos);
//			if ("CmNewDiffRateRow".equalsIgnoreCase(checkCol))
//			{
//				startColPos++;
//				hasNewDiffRateRowCol = true;
//			}
//			
//			// Get headers / colNames
//			for (int c=startColPos; c<=cols; c++)
//			{
//				colHead.add(rsmd.getColumnLabel(c));
//				colSqlDataType[c-1] = rsmd.getColumnType(c);
//			}
////System.out.println("RRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRR PersistReader: cm["+cm+"].setOfflineColumnNames(type="+type+", colHead="+colHead+")");
//			cm.setOfflineColumnNames(type, colHead);
//
////System.out.println("RRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRR PersistReader: sql2="+sql2);
////int r=0;
//			// Get Rows
//			while (rs.next())
//			{
////r++;
////System.out.println("RRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRR PersistReader: read row="+r+", cols="+cols+", cm="+cm);
//				Timestamp sessionStartTime  = rs.getTimestamp(1);
//				Timestamp sessionSampleTime = rs.getTimestamp(2);
//				Timestamp sampleTime        = rs.getTimestamp(3);
//				int       sampleMs          = rs.getInt(4);
//				int       newDiffRateRow    = hasNewDiffRateRowCol ? rs.getInt(5) : 0;
//
////				cm.setSampleTimeHead(sessionStartTime);
//				cm.setSampleTimeHead(sessionSampleTime);
//				cm.setSampleTime(sampleTime);
//				cm.setSampleInterval(sampleMs);
//				cm.setNewDeltaOrRateRow(row, newDiffRateRow>0);
//				
//				for (int c=startColPos,col=0; c<=cols; c++,col++)
//				{
////					oa[col] = rs.getObject(c);
//					Object colVal = rs.getObject(c);
//					
//					// Some datatypes we need to take extra care of
//					if (colSqlDataType[c-1] == Types.CLOB)
//						colVal = rs.getString(c);
//
//					cm.setOfflineValueAt(type, colVal, row, col);
//				}
//				
//				row++;
//			}
//			rs.close();
//			pstmnt.close();
//
//			// now rows was found, do something?
//			if (row == 0)
//			{
//				if (cm.getRowCount() > 0)
//					_logger.info("loadSessionCm(cmName='"+cmName+"', type='"+typeStr+"', sampleTs='"+sampleTs+"'): NO ROW WAS FOUND IN THE STORAGE, but cm.getRowCount()="+cm.getRowCount());
//			}
//
////System.out.println(Thread.currentThread().getName()+": Loaded "+row+" rows into for the CM '"+cmName+"', type='"+typeStr+"', which took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"' for sampleTs '"+sampleTs+"'.");
//			_logger.debug("Loaded "+row+" rows into for the CM '"+cmName+"', type='"+typeStr+"', which took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"' for sampleTs '"+sampleTs+"'.");
//			setStatusText("");
//		}
//		catch (SQLException e)
//		{
//			_logger.error("Problems loading cm='"+cmName+"', type='"+typeStr+"'.", e);
//		}
//	}
//
//	
//	private void execLoadSummaryCm(Timestamp sampleTs)
//	{
//		if (sampleTs == null)
//			throw new IllegalArgumentException("sampleTs can't be null");
//
////		String cmName = SummaryPanel.CM_NAME;
//		String cmName = CounterController.getSummaryCmName();
////		CountersModel cm = GetCounters.getInstance().getCmByName(cmName);
//		CountersModel cm = CounterController.getInstance().getCmByName(cmName);
//		if (cm == null)
//		{
//			_logger.warn("Can't find any CM named '"+cmName+"'.");
//			return;
//		}
//		if (true) loadSessionCm(cm, CountersModel.DATA_ABS,  sampleTs);
//		if (true) loadSessionCm(cm, CountersModel.DATA_DIFF, sampleTs);
//		if (true) loadSessionCm(cm, CountersModel.DATA_RATE, sampleTs);
//
//		CmIndicator cmInd = getIndicatorForCm(cmName);
//		if (cmInd != null)
//			cm.setIsCountersCleared(cmInd._isCountersCleared);
//
//		cm.setDataInitialized(true);
//		cm.fireTableStructureChanged();
//		if (cm.getTabPanel() != null)
//			cm.getTabPanel().adjustTableColumnWidth();
//
//		// Load/mark everything else...
//		MainFrame.getInstance().setTimeLinePoint(sampleTs);
//	}
//	
////	private void execLoadSessionIndicators(Timestamp sessionStartTs)
////	{
////		setWatermark("Loading Session Indicators...");
//////		System.out.println("execLoadSessionIndicators(sessionStartTime='"+sessionStartTs+"')");
////
////		long fetchStartTime = System.currentTimeMillis();
////		setStatusText("Loading Session indicators for sessionStartTime '"+sessionStartTs+"'.");
////
////		// Reset the Map...
////		_sessionIndicatorMap.clear();
////
////		//----------------------------------------
////		// TYPICAL look of a graph table
////		//----------------------------------------
////		//create table MonSessionSampleDetailes
////		//(
////		//   SessionStartTime  datetime  not null,
////		//   CmName            varchar   not null,
////		//   absSamples        int           null,
////		//   diffSamples       int           null,
////		//   rateSamples       int           null
////		//)
////		String sql = "select * from " + PersistWriterBase.getTableName(PersistWriterBase.SESSION_SAMPLE_SUM, null, true) + " " +
////		             "where \"SessionStartTime\" = ? ";
////
////		try
////		{
////			PreparedStatement pstmnt = _conn.prepareStatement(sql);
//////			pstmnt.setTimestamp(1, sampleTs);
////			pstmnt.setString(1, sessionStartTs.toString());
////			
////
////			_logger.debug("loadSessionIndicators(sampleTs='"+sessionStartTs+"'): "+pstmnt);
////
////			ResultSet rs = pstmnt.executeQuery();
////			ResultSetMetaData rsmd = rs.getMetaData();
////			int cols = rsmd.getColumnCount();
////			int row  = 0;
////
////			// Get Rows
////			while (rs.next())
////			{
////				Timestamp sessionStartTime  = rs.getTimestamp(1);
////				String    cmName            = rs.getString(2);
////				int       absSamples        = rs.getInt(3);
////				int       diffSamples       = rs.getInt(4);
////				int       rateSamples       = rs.getInt(5);
////
////				SessionIndicator sessInd = new SessionIndicator(sessionStartTime, cmName, absSamples, diffSamples, rateSamples);
////
////				// Add it to the indicators map
////				_sessionIndicatorMap.put(cmName, sessInd);
////				
////				row++;
////			}
////			rs.close();
////			pstmnt.close();
////
//////System.out.println("Loaded "+row+" indicators for ts '"+sampleTs+"' , which took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.");
////			_logger.debug("Loaded "+row+" session indicators for ts '"+sessionStartTs+"' , which took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.");
////			setStatusText("");
////		}
////		catch (SQLException e)
////		{
////			_logger.error("Problems loading session indicators for ts '"+sessionStartTs+"'.", e);
////		}
////
////		String str = "Loading session indicators took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.";
////		_logger.debug(str);
////		setStatusText(str);
////		setWatermark();
////	}
//
//	private void execLoadSessionCmIndicators(Timestamp sampleTs)
//	{
//		setWatermark("Loading Counter Indicators...");
////		System.out.println("loadSessionCmIndicators(sampleId='"+sampleTs+"')");
//
//		long fetchStartTime = System.currentTimeMillis();
//		setStatusText("Loading all counter indicators for time '"+sampleTs+"'.");
//
//		// Reset the Map...
//		_currentIndicatorMap.clear();
//
//		//----------------------------------------
//		// TYPICAL look of a graph table
//		//----------------------------------------
//		//create table MonSessionSampleDetailes
//		//(
//		//   SessionStartTime  datetime  not null,
//		//   SessionSampleTime datetime  not null,
//		//   CmName            varchar   not null,
//		//   type              int           null,
//		//   graphCount        int           null,
//		//   absRows           int           null,
//		//   diffRows          int           null,
//		//   rateRows          int           null
//		//)
//		String sql = "select * from " + PersistWriterBase.getTableName(PersistWriterBase.SESSION_SAMPLE_DETAILES, null, true) + " " +
//		             "where \"SessionSampleTime\" = ? ";
//
//		try
//		{
//			PreparedStatement pstmnt = _conn.prepareStatement(sql);
////			pstmnt.setTimestamp(1, sampleTs);
//			pstmnt.setString(1, sampleTs.toString());
//			
//
//			_logger.debug("loadSessionCmIndicators(sampleTs='"+sampleTs+"'): "+pstmnt);
//
//			ResultSet rs = pstmnt.executeQuery();
//			ResultSetMetaData rsmd = rs.getMetaData();
//			int cols = rsmd.getColumnCount();
//			int row  = 0;
//			boolean hasSqlGuiRefreshTime    = cols >= 9;
//			boolean hasNonConfiguredFields  = cols >= 12;
//			boolean hasCounterClearedFields = cols >= 15;
//			boolean hasExceptionFields      = cols >= 16;
////FIXME: nonConfigCapture....cols both in the reader and writer
////boolean nonConfigCapture
////String  missingConfigParams
////String  srvMessage
//
//			// Get Rows
//			while (rs.next())
//			{
//				Timestamp sessionStartTime    = rs.getTimestamp(1);
//				Timestamp sessionSampleTime   = rs.getTimestamp(2);
//				String    cmName              = rs.getString(3);
//				int       type                = rs.getInt(4);
//				int       graphCount          = rs.getInt(5);
//				int       absRows             = rs.getInt(6);
//				int       diffRows            = rs.getInt(7);
//				int       rateRows            = rs.getInt(8);
//				int       sqlRefreshTime      = hasSqlGuiRefreshTime ? rs.getInt(9)  : -1;
//				int       guiRefreshTime      = hasSqlGuiRefreshTime ? rs.getInt(10) : -1;
//				int       lcRefreshTime       = hasSqlGuiRefreshTime ? rs.getInt(11) : -1;
//				boolean   nonConfiguredMonitoringHappened     = (hasNonConfiguredFields  ? rs.getInt   (12) : 0) > 0;
//				String    nonConfiguedMonitoringMissingParams =  hasNonConfiguredFields  ? rs.getString(13) : null;
//				String    nonConfiguedMonitoringMessages      =  hasNonConfiguredFields  ? rs.getString(14) : null;
//				boolean   isCountersCleared                   = (hasCounterClearedFields ? rs.getInt   (15) : 0) > 0;
//				boolean   hasValidCounterData                 = (hasExceptionFields      ? rs.getInt   (16) : 0) > 0;
//				String    exceptionMsg                        =  hasExceptionFields      ? rs.getString(17) : null;
//				String    exceptionFullText                   =  hasExceptionFields      ? rs.getString(18) : null;
//
//				
//				// Map cmName from DB->Internal name if needed.
//				cmName = getNameTranslateDbToCm(cmName);
//				
//				CmIndicator cmInd = new CmIndicator(sessionStartTime, sessionSampleTime, cmName, type, graphCount, absRows, diffRows, rateRows, 
//						sqlRefreshTime, guiRefreshTime, lcRefreshTime,
//						nonConfiguredMonitoringHappened, nonConfiguedMonitoringMissingParams, nonConfiguedMonitoringMessages,
//						isCountersCleared, hasValidCounterData, exceptionMsg, exceptionFullText);
//
//				// Add it to the indicators map
//				_currentIndicatorMap.put(cmName, cmInd);
//				
//				row++;
//			}
//			rs.close();
//			pstmnt.close();
//
////System.out.println("Loaded "+row+" indicators for ts '"+sampleTs+"' , which took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.");
//			_logger.debug("Loaded "+row+" indicators for ts '"+sampleTs+"' , which took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.");
//			setStatusText("");
//		}
//		catch (SQLException e)
//		{
//			_logger.error("Problems loading cm indicators for ts '"+sampleTs+"'.", e);
//		}
//
//		String str = "Loading indicators took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.";
//		_logger.debug(str);
//		setStatusText(str);
//		setWatermark();
//	}
//
//	public void setCurrentSampleTime(Timestamp ts)
//	{
//		loadSessionCmIndicators(ts);
//	}
//
//	public CountersModel getCmForSample(String name, Timestamp sampleTs)
//	{
//		CmIndicator cmInd = getIndicatorForCm(name);
////System.out.println("getCmForSample(name='"+name+"', sampleTs='"+sampleTs+"': cmInd="+cmInd);
//		if (cmInd == null)
//		{
//			if (_logger.isDebugEnabled())
//			{
//				_logger.debug("No CmIndicator was found in 'map' for cm named '"+name+"' with ts '"+sampleTs+"'.");
//				// Print the content for this sample
//				for (Map.Entry<String,CmIndicator> entry : _currentIndicatorMap.entrySet()) 
//				{
//					String      key = entry.getKey();
//					CmIndicator val = entry.getValue();
//
//					_logger.debug("IndicatorMap: key="+StringUtil.left(key, 20)+": CmIndicator="+val);
//				}
//			}
//			return null;
//		//	throw new RuntimeException("No CmIndicator was found in 'map' for cm named '"+name+"'.");
//		}
//
//		return loadSessionCm(cmInd, sampleTs);
//	}
//
//	public CountersModel loadSessionCm(CmIndicator cmInd, Timestamp sampleTs)
//	{
//		if (cmInd    == null) throw new IllegalArgumentException("CmIndicator can't be null");
//		if (sampleTs == null) throw new IllegalArgumentException("sampleTs can't be null");
//
//		String cmName = cmInd._cmName;
////		CountersModel cm = GetCounters.getInstance().getCmByName(cmName);
//		CountersModel cm = CounterController.getInstance().getCmByName(cmName);
//		if (cm == null)
//		{
//			_logger.warn("Can't find any CM named '"+cmName+"'.");
//			return null;
//		}
//
//		// Make a new object, which the data will be attached to
//		// current CM is reused, then the fireXXX will be done and TableModel.get* will fail.
////System.out.println("PersistReaderloadSessionCm(): BEFORE copy cm="+cm);
//		cm = cm.copyForOfflineRead(); // NOTE: This causes A LOT OF TROUBLE (since a new instance/object is created every time... PLEASE: make a better solution...
////		cm.clearForRead(); // NOTE: When we use a "single" CM (or single offline-cm) then it should be enough to clear/reset some fields in the offline-cm
////System.out.println("PersistReaderloadSessionCm(): AFTER  copy cm="+cm);
////System.out.println("loadSessionCm()|absRows="+cmInd._absRows+",diffRows="+cmInd._absRows+",rateRows="+cmInd._absRows+"| cm.getName()='"+cm.getName()+"', cmName='"+cmName+"'.");
//
//		if (cmInd._absRows  > 0) loadSessionCm(cm, CountersModel.DATA_ABS,  sampleTs);
//		if (cmInd._diffRows > 0) loadSessionCm(cm, CountersModel.DATA_DIFF, sampleTs);
//		if (cmInd._rateRows > 0) loadSessionCm(cm, CountersModel.DATA_RATE, sampleTs);
//
//		cm.setSqlRefreshTime(cmInd._sqlRefreshTime);
//		cm.setGuiRefreshTime(cmInd._guiRefreshTime);
//		cm.setLcRefreshTime (cmInd._lcRefreshTime);
//		
//		cm.setNonConfiguredMonitoringHappened     (cmInd._nonConfiguredMonitoringHappened);
//		cm.addNonConfiguedMonitoringMessage       (cmInd._nonConfiguedMonitoringMessages);
//		cm.setNonConfiguredMonitoringMissingParams(cmInd._nonConfiguedMonitoringMissingParams);
//
//		cm.setIsCountersCleared(cmInd._isCountersCleared);
//
//		cm.setValidSampleData        (cmInd._hasValidSampleData);
//		cm.setSampleException        (cmInd._exceptionMsg == null ? null : new PcsSavedException(cmInd._exceptionMsg));
////		cm.setSampleExceptionFullText(cmInd._exceptionFullText);
//
//		cm.setDataInitialized(true);
////		cm.fireTableStructureChanged();
////		if (cm.getTabPanel() != null)
////			cm.getTabPanel().adjustTableColumnWidth();
//		
//		return cm;
//	}
//
//	
//	private void execLoadSessions()
//	{
//		setWatermark("Loading Sessions...");
//
//		long fetchStartTime = System.currentTimeMillis();
//		setStatusText("Loading Sessions....");
//
//		// get a LIST of sessions
//		List<SessionInfo> sessionList = getSessionList();
//	
//		// Loop the sessions and load all samples 
//		for (SessionInfo sessionInfo : sessionList)
//		{
//			// Load all samples for this sampleId
//			sessionInfo._sampleList         = getSessionSamplesList       (sessionInfo._sessionId);
//			sessionInfo._sampleCmNameSumMap = getSessionSampleCmNameSumMap(sessionInfo._sessionId);
//		}
//
//		// Save last SessionInfo, this can be used to send statistics
//		_lastLoadedSessionList = sessionList;
//		
//		// Get number of samples in LAST session
//		_numOfSamplesOnLastRefresh = getNumberOfSamplesInLastSession(true);
//
//		_logger.debug("loadSessions(COMPLETED) calling listeners");
//		for (INotificationListener nl : _notificationListeners)
//		{
//			nl.setSessionList(sessionList);
//		}
//
//		String str = "Loading Sessions took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.";
//		_logger.debug(str);
//		setStatusText(str);
//		setWatermark();
//	}
//
//	public List<SessionInfo> getSessionList()
//	{
//		setWatermark("Loading Sessions..."); // if the window wasn't visible, set the watermark now
//		long fetchStartTime = System.currentTimeMillis();
//		setStatusText("Loading Session List....");
//
//		List<SessionInfo> sessions = new LinkedList<SessionInfo>();
//
//		try
//		{
//			Statement stmnt   = _conn.createStatement();
//			ResultSet rs      = stmnt.executeQuery(GET_ALL_SESSIONS);
//			while (rs.next())
//			{
//				Timestamp sessionId      = rs.getTimestamp("SessionStartTime");
//				Timestamp lastSampleTime = rs.getTimestamp("LastSampleTime");
//				int       numOfSamples   = rs.getInt("NumOfSamples");
//
//				sessions.add( new SessionInfo(sessionId, lastSampleTime, numOfSamples) );
//			}
//			rs.close();
//			stmnt.close();
//		}
//		catch (SQLException e)
//		{
//			_logger.error("Problems inititialize...", e);
//		}
//
//		String str = "Loading Session List took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.";
//		_logger.debug(str);
//		setStatusText(str);
//		
//		return sessions;
//	}
//
//	public List<Timestamp> getSessionSamplesList(Timestamp sessionId)
//	{
//		setWatermark("Loading Sessions...");  // if the window wasn't visible, set the watermark now
//		long fetchStartTime = System.currentTimeMillis();
//		setStatusText("Loading Session Samples for sessionId '"+sessionId+"'.");
//
//		List<Timestamp> list = new LinkedList<Timestamp>();
//
//		try
//		{
//			PreparedStatement pstmnt = _conn.prepareStatement(GET_SESSION);
////			pstmnt.setTimestamp(1, sessionId);
//			pstmnt.setString(1, sessionId.toString());
//
//			ResultSet rs = pstmnt.executeQuery();
//			while (rs.next())
//			{
//				list.add(rs.getTimestamp("SessionSampleTime"));
//			}
//			rs.close();
//			pstmnt.close();
//		}
//		catch (SQLException e)
//		{
//			_logger.error("Problems inititialize...", e);
//		}
//		
//		String str = "Loading Session Samples for sessionId '"+sessionId+"' took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.";
//		_logger.debug(str);
//		setStatusText(str);
//		
//		return list;
//	}
//
//	public Map<String, CmNameSum> getSessionSampleCmNameSumMap(Timestamp sessionStartTime)
//	{
//		setWatermark("Loading Sessions...");  // if the window wasn't visible, set the watermark now
//		long fetchStartTime = System.currentTimeMillis();
//		setStatusText("Loading Session Samples CM Summy for sessionId '"+sessionStartTime+"'.");
//
//		// create table MonSessionSampleSum
//		// (
//		//    SessionStartTime datetime     not null,
//		//    CmName           varchar(30)  not null,
//		//    absSamples       int              null,
//		//    diffSamples      int              null,
//		//    rateSamples      int              null,
//		// 
//		//    PRIMARY KEY (SessionStartTime, CmName)
//		// )
//
//		Map<String, CmNameSum> map = new LinkedHashMap<String, CmNameSum>();
//
//		String sql = 
//			"select * " +
//			"from " + PersistWriterBase.getTableName(PersistWriterBase.SESSION_SAMPLE_SUM, null, true) + " " +
//			"where \"SessionStartTime\" = ? ";
//
//		try
//		{
//			PreparedStatement pstmnt = _conn.prepareStatement(sql);
////			pstmnt.setTimestamp(1, sessionStartTime);
//			pstmnt.setString(1, sessionStartTime.toString());
//
//			ResultSet rs = pstmnt.executeQuery();
//			while (rs.next())
//			{
//				Timestamp ssTime      = rs.getTimestamp(1);
//				String    cmName      = rs.getString   (2);
//				int       absSamples  = rs.getInt      (3);
//				int       diffSamples = rs.getInt      (4);
//				int       rateSamples = rs.getInt      (5);
//
//				// Map cmName from DB->Internal name if needed.
//				cmName = getNameTranslateDbToCm(cmName);
//				
//				map.put(cmName, new CmNameSum(ssTime, cmName, absSamples, diffSamples, rateSamples));
//			}
//			rs.close();
//			pstmnt.close();
//		}
//		catch (SQLException e)
//		{
//			_logger.error("Problems loading 'session sample cm summary' for ts '"+sessionStartTime+"'. sql="+sql, e);
//		}
//		
//		String str = "Loading Session Samples for sessionStartTime '"+sessionStartTime+"' took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.";
//		_logger.debug(str);
//		setStatusText(str);
//		
//		return map;
//	}
//	
//	public Map<Timestamp, SampleCmCounterInfo> getSessionSampleCmCounterInfoMap(Timestamp inSessionStartTime)
//	{
//		setWatermark("Loading Sessions...");  // if the window wasn't visible, set the watermark now
//		long fetchStartTime = System.currentTimeMillis();
//		setStatusText("Loading 'Session Samples CM Counter Info' for sessionId '"+inSessionStartTime+"'.");
//
//		// CREATE TABLE "MonSessionSampleDetailes"
//		// (
//		//     "SessionStartTime"  DATETIME    NOT NULL,
//		//     "SessionSampleTime" DATETIME    NOT NULL,
//		//     "CmName"            VARCHAR(30) NOT NULL,
//		//     "type"              INT,
//		//     "graphCount"        INT,
//		//     "absRows"           INT,
//		//     "diffRows"          INT,
//		//     "rateRows"          INT
//		// )
//
//		Map<Timestamp, SampleCmCounterInfo> map = new LinkedHashMap<Timestamp, SampleCmCounterInfo>();
//
//		String sql = 
//			"select * " +
//			"from " + PersistWriterBase.getTableName(PersistWriterBase.SESSION_SAMPLE_DETAILES, null, true) + " " +
//			"where \"SessionStartTime\" = ? " +
//			"order by \"SessionSampleTime\" ";
//
//		SampleCmCounterInfo scmci = null;
//		try
//		{
//			PreparedStatement pstmnt = _conn.prepareStatement(sql);
////			pstmnt.setTimestamp(1, sessionStartTime);
//			pstmnt.setString(1, inSessionStartTime.toString());
//
//			long lastSampleTime = 0;
//			ResultSet rs = pstmnt.executeQuery();
//			ResultSetMetaData rsmd = rs.getMetaData();
//			int cols = rsmd.getColumnCount();
//			boolean hasSqlGuiRefreshTime = cols >= 9;
//
//			while (rs.next())
//			{
//				CmCounterInfo cmci = new CmCounterInfo();
//				cmci._sessionStartTime  = rs.getTimestamp(1);
//				cmci._sessionSampleTime = rs.getTimestamp(2);
//				cmci._cmName            = rs.getString   (3);
//				cmci._type              = rs.getInt      (4);
//				cmci._graphCount        = rs.getInt      (5);
//				cmci._absRows           = rs.getInt      (6);
//				cmci._diffRows          = rs.getInt      (7);
//				cmci._rateRows          = rs.getInt      (8);
//				cmci._sqlRefreshTime    = hasSqlGuiRefreshTime ? rs.getInt(9)  : -1;
//				cmci._guiRefreshTime    = hasSqlGuiRefreshTime ? rs.getInt(10) : -1;
//				cmci._lcRefreshTime     = hasSqlGuiRefreshTime ? rs.getInt(11) : -1;
//
//				// Map cmName from DB->Internal name if needed.
//				cmci._cmName = getNameTranslateDbToCm(cmci._cmName);
//				
//				if (lastSampleTime != cmci._sessionSampleTime.getTime())
//				{
//					lastSampleTime = cmci._sessionSampleTime.getTime();
////if (scmci != null)
////	System.out.println("XXXX: scmci._ciMap.size()="+scmci._ciMap.size()+" entries in: ts='"+scmci._sessionSampleTime+"'.");
//
//					scmci = new SampleCmCounterInfo(cmci._sessionStartTime, cmci._sessionSampleTime);
//					//scmci._ciMap = new..done in obj
//
//					// Add this new entry to the OUTER map
//					map.put(scmci._sessionSampleTime, scmci);
////System.out.println("getSessionSampleCmCounterInfoMap: NEW: ts='"+scmci._sessionSampleTime+"'.");
//				}
//				// Now add the row(CmCounterInfo) to the sample/agregate object (SampleCmCounterInfo)
//				scmci._ciMap.put(cmci._cmName, cmci);
//			}
//			rs.close();
//			pstmnt.close();
//		}
//		catch (SQLException e)
//		{
//			_logger.error("Problems loading 'session sample cm counter info' for ts '"+inSessionStartTime+"'. sql="+sql, e);
//		}
//		
//		String str = "Loading 'Session Samples CM Counter Info' for sessionId '"+inSessionStartTime+"' took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.";
////System.out.println(str);
//		_logger.debug(str);
//		setStatusText(str);
//
//		return map;
//	}
//	
//	public MonVersionInfo getMonVersionInfo(Timestamp sessionStartTime)
//	{
//		// CREATE TABLE "MonVersionInfo"
//		// (
//		//    "SessionStartTime" DATETIME    NOT NULL,
//		//    "ProductString"    VARCHAR(30) NOT NULL,
//		//    "VersionString"    VARCHAR(30) NOT NULL,
//		//    "BuildString"      VARCHAR(30) NOT NULL,
//		//    "SourceDate"       VARCHAR(30) NOT NULL,
//		//    "SourceRev"        INT         NOT NULL,
////		//    "DbProductName"    VARCHAR(30) NOT NULL
//		// )
//
//		String sql = 
//			"select * " +
//			"from " + PersistWriterBase.getTableName(PersistWriterBase.VERSION_INFO, null, true) + " " +
//			"where \"SessionStartTime\" = ? ";
//
//		try
//		{
//			PreparedStatement pstmnt = _conn.prepareStatement(sql);
////			pstmnt.setTimestamp(1, sessionStartTime);
//			pstmnt.setString(1, sessionStartTime.toString());
//
//			MonVersionInfo monVersionInfo = new MonVersionInfo();
//			ResultSet rs = pstmnt.executeQuery();
////			int colCount = rs.getMetaData().getColumnCount();
//			boolean foundRows = false;
//			while (rs.next())
//			{
//				foundRows = true;
//				monVersionInfo._sessionStartTime  = rs.getTimestamp(1);
//				monVersionInfo._productString     = rs.getString   (2);
//				monVersionInfo._versionString     = rs.getString   (3);
//				monVersionInfo._buildString       = rs.getString   (4);
//				monVersionInfo._sourceDate        = rs.getString   (5);
//				monVersionInfo._sourceRev         = rs.getInt      (6);
////				if (colCount >= 7)
////					monVersionInfo._dbProductName = rs.getString   (7);
//			}
//			rs.close();
//			pstmnt.close();
//
//			if ( ! foundRows )
//			{
//				_logger.warn("No row was found when loading 'MonVersionInfo' for ts '"+sessionStartTime+"'. sql="+sql);
//				return null;
//			}
//				
//			return monVersionInfo;
//		}
//		catch (SQLException e)
//		{
//			_logger.error("Problems loading 'MonVersionInfo' for ts '"+sessionStartTime+"'. sql="+sql, e);
//			return null;
//		}
//	}
//
//
//	/*---------------------------------------------------
//	** END: 
//	**---------------------------------------------------
//	*/
//	/** a reflection of one row in: MonVersionInfo */
//	public static class MonVersionInfo
//	{
//		public Timestamp _sessionStartTime = null;
//		public String    _productString    = null;
//		public String    _versionString    = null;
//		public String    _buildString      = null;
//		public String    _sourceDate       = null;
//		public int       _sourceRev        = 0;
////		public String    _dbProductName    = null;
//		
//		@Override
//		public String toString()
//		{
////			return "MonVersionInfo(sessionStartTime='"+_sessionStartTime+"', productString='"+_productString+"', versionString='"+_versionString+"', buildString='"+_buildString+"', sourceDate='"+_sourceDate+"', sourceRev='"+_sourceRev+"', _dbProductName='"+_dbProductName+"')";
//			return "MonVersionInfo(sessionStartTime='"+_sessionStartTime+"', productString='"+_productString+"', versionString='"+_versionString+"', buildString='"+_buildString+"', sourceDate='"+_sourceDate+"', sourceRev='"+_sourceRev+"')";
//		}
//	}
//
//	/** a reflection of one row in: MonSessionSampleDetailes */
//	public static class CmCounterInfo
//	{
//		public Timestamp _sessionStartTime  = null;
//		public Timestamp _sessionSampleTime = null;
//		public String    _cmName            = null;
//		public int       _type              = -1;
//		public int       _graphCount        = -1;
//		public int       _absRows           = -1;
//		public int       _diffRows          = -1;
//		public int       _rateRows          = -1;
//		public int       _sqlRefreshTime    = -1;
//		public int       _guiRefreshTime    = -1;
//		public int       _lcRefreshTime     = -1;
//
//		public CmCounterInfo()
//		{
//		}
//		public CmCounterInfo(CmCounterInfo copyMe, boolean asSamples)
//		{
//			_sessionStartTime  = copyMe._sessionStartTime;
//			_sessionSampleTime = copyMe._sessionSampleTime;
//			_cmName            = copyMe._cmName;
//			_type              = copyMe._type;
//			if (asSamples)
//			{
//				_graphCount += copyMe._graphCount == 0 ? 0 : 1;
//				_absRows    += copyMe._absRows    == 0 ? 0 : 1;
//				_diffRows   += copyMe._diffRows   == 0 ? 0 : 1;
//				_rateRows   += copyMe._rateRows   == 0 ? 0 : 1;
//			}
//			else
//			{
//				_graphCount += copyMe._graphCount;
//				_absRows    += copyMe._absRows;
//				_diffRows   += copyMe._diffRows;
//				_rateRows   += copyMe._rateRows;
//			}
////			_graphCount        = copyMe._graphCount;
////			_absRows           = copyMe._absRows;
////			_diffRows          = copyMe._diffRows;
////			_rateRows          = copyMe._rateRows;
//			_sqlRefreshTime    = copyMe._sqlRefreshTime;
//			_guiRefreshTime    = copyMe._guiRefreshTime;
//			_lcRefreshTime     = copyMe._lcRefreshTime;
//		}
//		@Override
//		public String toString()
//		{
//			StringBuilder sb = new StringBuilder();
//			sb.append("sessionStartTime='")   .append(_sessionStartTime) .append("'");
//			sb.append(", sessionSampleTime='").append(_sessionSampleTime).append("'");
//			sb.append(", cmName='")           .append(_cmName)           .append("'");
//			sb.append(", type=")              .append(_type);
//			sb.append(", graphCount=")        .append(_graphCount);
//			sb.append(", absRows=")           .append(_absRows);
//			sb.append(", diffRows=")          .append(_diffRows);
//			sb.append(", rateRows=")          .append(_rateRows);
//			sb.append(", sqlRefreshTime=")    .append(_sqlRefreshTime);
//			sb.append(", guiRefreshTime=")    .append(_guiRefreshTime);
//			sb.append(", lcRefreshTime=")     .append(_lcRefreshTime);
//			return sb.toString();
//		}
//	}
//	public static class SampleCmCounterInfo
//	{
//		public Timestamp _sessionStartTime  = null;
//		public Timestamp _sessionSampleTime = null;
//		public Map<String, CmCounterInfo> _ciMap = new LinkedHashMap<String, CentralPersistReader.CmCounterInfo>();
//
//		public SampleCmCounterInfo(Timestamp sessionStartTime, Timestamp sessionSampleTime)
//		{
//			_sessionStartTime  = sessionStartTime;
//			_sessionSampleTime = sessionSampleTime;
//		}
//
//		public void merge(SampleCmCounterInfo in, boolean asSamples)
//		{
//			if (in == null)
//				return;
//			
//			// loop input 
//			for (Map.Entry<String,CmCounterInfo> entry : in._ciMap.entrySet()) 
//			{
//				String        cmName = entry.getKey();
//				CmCounterInfo cmci   = entry.getValue();
//
//				if ( ! this._ciMap.containsKey(cmName) )
//				{
//					this._ciMap.put(cmName, new CmCounterInfo(cmci, asSamples));
////System.out.println("----1: cm='"+cmName+"', cmci="+cmci);
//				}
//				else
//				{
//					CmCounterInfo localCmci = this._ciMap.get(cmName);
////System.out.println("2: cm='"+cmName+"', localCmci="+localCmci);
//					if (asSamples)
//					{
//						localCmci._graphCount += cmci._graphCount == 0 ? 0 : 1;
//						localCmci._absRows    += cmci._absRows    == 0 ? 0 : 1;
//						localCmci._diffRows   += cmci._diffRows   == 0 ? 0 : 1;
//						localCmci._rateRows   += cmci._rateRows   == 0 ? 0 : 1;
//					}
//					else
//					{
//						localCmci._graphCount += cmci._graphCount;
//						localCmci._absRows    += cmci._absRows;
//						localCmci._diffRows   += cmci._diffRows;
//						localCmci._rateRows   += cmci._rateRows;
//					}
//
//					localCmci._sqlRefreshTime += cmci._sqlRefreshTime;
//					localCmci._guiRefreshTime += cmci._guiRefreshTime;
//					localCmci._lcRefreshTime  += cmci._lcRefreshTime;
//				}
//			}
//		}
//	}
//
//	/** a reflection of one row in: MonSessionSampleSum */
//	public static class CmNameSum
//	{
//		public Timestamp _sessionStartTime = null;
//		public String    _cmName           = null;
//		public int       _absSamples       = 0;
//		public int       _diffSamples      = 0;
//		public int       _rateSamples      = 0;
//
//		public CmNameSum(Timestamp sessionStartTime, String cmName, int absSamples, int diffSamples, int rateSamples)
//		{
//			_sessionStartTime = sessionStartTime;
//			_cmName           = cmName;
//			_absSamples       = absSamples;
//			_diffSamples      = diffSamples;
//			_rateSamples      = rateSamples;
//		}
//	}
//
//	public static class SessionInfo
//	{
//		public Timestamp              _sessionId          = null;
//		public Timestamp              _lastSampleTime     = null;
//		public int                    _numOfSamples       = -1;
//		public List<Timestamp>        _sampleList         = null;
//		public Map<String, CmNameSum> _sampleCmNameSumMap = null;
//		public Map<Timestamp, SampleCmCounterInfo> _sampleCmCounterInfoMap = null;
//
//		public SessionInfo(Timestamp sessionId, Timestamp lastSampleTime, int numOfSamples)
//		{
//			_sessionId      = sessionId;
//			_lastSampleTime = lastSampleTime;
//			_numOfSamples   = numOfSamples;
//		}
//	}
//
//	public static class SessionIndicator
//	{
//		public Timestamp _sessionStartTime  = null;
//		public String    _cmName            = null;
//		public int       _absSamples        = -1;
//		public int       _diffSamples       = -1;
//		public int       _rateSamples       = -1;
//
//		public SessionIndicator(Timestamp sessionStartTime, String cmName, 
//				int absSamples, int diffSamples, int rateSamples)
//		{
//			_sessionStartTime  = sessionStartTime;
//			_cmName            = cmName;
//			_absSamples        = absSamples;
//			_diffSamples       = diffSamples;
//			_rateSamples       = rateSamples;
//		}
//		
//		@Override
//		public String toString()
//		{
//			StringBuilder sb = new StringBuilder();
//			sb.append("sessionStartTime='").append(_sessionStartTime) .append("'");
//			sb.append(", cmName='")        .append(_cmName)           .append("'");
//			sb.append(", absSamples=")     .append(_absSamples);
//			sb.append(", diffSamples=")    .append(_diffSamples);
//			sb.append(", rateSamples=")    .append(_rateSamples);
//			return sb.toString();
//		}
//	}
//
//	public static class CmIndicator
//	{
//		public Timestamp _sessionStartTime                    = null;
//		public Timestamp _sessionSampleTime                   = null;
//		public String    _cmName                              = null;
//		public int       _type                                = -1;
//		public int       _graphCount                          = -1;
//		public int       _absRows                             = -1;
//		public int       _diffRows                            = -1;
//		public int       _rateRows                            = -1;
//		public int       _sqlRefreshTime                      = -1;
//		public int       _guiRefreshTime                      = -1;
//		public int       _lcRefreshTime                       = -1;
//		public boolean   _nonConfiguredMonitoringHappened     = false;
//		public String    _nonConfiguedMonitoringMissingParams = null;
//		public String    _nonConfiguedMonitoringMessages      = null;
//		public boolean   _isCountersCleared                   = false;
//		public boolean   _hasValidSampleData                  = false;
//		public String    _exceptionMsg                        = null;
//		public String    _exceptionFullText                   = null;
//
//		public CmIndicator(Timestamp sessionStartTime, Timestamp sessionSampleTime, 
//		                   String cmName, int type, int graphCount, int absRows, int diffRows, int rateRows,
//		                   int sqlRefreshTime, int guiRefreshTime, int lcRefreshTime, 
//		                   boolean nonConfiguredMonitoringHappened, String nonConfiguedMonitoringMissingParams, String nonConfiguedMonitoringMessages,
//		                   boolean isCountersCleared, boolean hasValidSampleData, String exceptionMsg, String exceptionFullText)
//		{
//			_sessionStartTime                    = sessionStartTime;
//			_sessionSampleTime                   = sessionSampleTime;
//			_cmName                              = cmName;
//			_type                                = type;
//			_graphCount                          = graphCount;
//			_absRows                             = absRows;
//			_diffRows                            = diffRows;
//			_rateRows                            = rateRows;
//			_sqlRefreshTime                      = sqlRefreshTime;
//			_guiRefreshTime                      = guiRefreshTime;
//			_lcRefreshTime                       = lcRefreshTime;
//			_nonConfiguredMonitoringHappened     = nonConfiguredMonitoringHappened;
//			_nonConfiguedMonitoringMissingParams = nonConfiguedMonitoringMissingParams;
//			_nonConfiguedMonitoringMessages      = nonConfiguedMonitoringMessages;
//			_isCountersCleared                   = isCountersCleared;
//			_hasValidSampleData                  = hasValidSampleData;
//			_exceptionMsg                        = exceptionMsg;
//			_exceptionFullText                   = exceptionFullText;
//		}
//		
//		@Override
//		public String toString()
//		{
//			StringBuilder sb = new StringBuilder();
//			sb.append("sessionStartTime='")                     .append(_sessionStartTime) .append("'");
//			sb.append(", sessionSampleTime='")                  .append(_sessionSampleTime).append("'");
//			sb.append(", cmName='")                             .append(_cmName)           .append("'");
//			sb.append(", type=")                                .append(_type);
//			sb.append(", graphCount=")                          .append(_graphCount);
//			sb.append(", absRows=")                             .append(_absRows);
//			sb.append(", diffRows=")                            .append(_diffRows);
//			sb.append(", rateRows=")                            .append(_rateRows);
//			sb.append(", sqlRefreshTime=")                      .append(_sqlRefreshTime);
//			sb.append(", guiRefreshTime=")                      .append(_guiRefreshTime);
//			sb.append(", lcRefreshTime=")                       .append(_lcRefreshTime);
//			sb.append(", nonConfiguredMonitoringHappened=")     .append(_nonConfiguredMonitoringHappened);
//			sb.append(", nonConfiguedMonitoringMissingParams='").append(_nonConfiguedMonitoringMissingParams).append("'");
//			sb.append(", nonConfiguedMonitoringMessages='")     .append(_nonConfiguedMonitoringMessages)     .append("'");
//			sb.append(", isCountersCleared=")                   .append(_isCountersCleared);
//			sb.append(", hasValidSampleData=")                  .append(_hasValidSampleData);
//			sb.append(", exceptionMsg='")                       .append(_exceptionMsg)     .append("'");
//			sb.append(", exceptionFullText='")                  .append(_exceptionFullText).append("'");
//			return sb.toString();
//		}
//	}
//	
//	public static class PcsSavedException
//	extends Exception
//	{
//		public PcsSavedException(String message)
//		{
//			super(message);
//		}
//	}
//
//	/*---------------------------------------------------
//	** BEGIN: notification listener stuff
//	**---------------------------------------------------
//	*/
//	/** List of any subscribers for notifications <code>NotificationListener</code>*/
//	private LinkedList<INotificationListener> _notificationListeners = new LinkedList<INotificationListener>();
//
//	/**
//	 * Interface that the listener has to implement
//	 */
//	public interface INotificationListener
//	{
//		public void setWatermark(String text);
//		public void setStatusText(String status);
//		public void setSessionList(List<SessionInfo> sessionList);
//	}
//
//	/** Add a listener component */
//	public void addNotificationListener(INotificationListener comp)
//	{
//		_notificationListeners.add(comp);
//	}
//
//	/** Remove a listener component */
//	public void removeNotificationListener(INotificationListener comp)
//	{
//		_notificationListeners.remove(comp);
//	}
//
//	/** calls all listeners using the method <code>setWatermark(String text)</code> */
//	private void setWatermark(String text)
//	{
//		_logger.debug("PersistentReader.setWatermark(text='"+text+"')");
//		for (INotificationListener nl : _notificationListeners)
//		{
//			nl.setWatermark(text);
//		}
//	}
//	/** Sets a watermark to null */
//	private void setWatermark()
//	{
//		setWatermark(null);
//	}
//
//	/** calls all listeners using the method <code>setStatusText(String status)</code> */
//	private void setStatusText(String status)
//	{
//		_logger.debug("setStatusText(status='"+status+"')");
//		for (INotificationListener nl : _notificationListeners)
//		{
//			nl.setStatusText(status);
//		}
//	}
//	/*---------------------------------------------------
//	** END: notification listener stuff
//	**---------------------------------------------------
//	*/
//
//
//	
//	/*---------------------------------------------------
//	** BEGIN: DDL reader
//	**---------------------------------------------------
//	*/
////	sbSql.append("create table " + tabName + "\n");
////	sbSql.append("( \n");
////	sbSql.append("    "+fill(qic+"dbname"           +qic,40)+" "+fill(getDatatype("varchar",   30,-1,-1),20)+" "+getNullable(false)+"\n");
////	sbSql.append("   ,"+fill(qic+"owner"            +qic,40)+" "+fill(getDatatype("varchar",   30,-1,-1),20)+" "+getNullable(false)+"\n");
////	sbSql.append("   ,"+fill(qic+"objectName"       +qic,40)+" "+fill(getDatatype("varchar",  255,-1,-1),20)+" "+getNullable(false)+"\n");
////	sbSql.append("   ,"+fill(qic+"type"             +qic,40)+" "+fill(getDatatype("varchar",   20,-1,-1),20)+" "+getNullable(false)+"\n");
////	sbSql.append("   ,"+fill(qic+"crdate"           +qic,40)+" "+fill(getDatatype("datetime",  -1,-1,-1),20)+" "+getNullable(false)+"\n");
////	sbSql.append("   ,"+fill(qic+"source"           +qic,40)+" "+fill(getDatatype("varchar",  255,-1,-1),20)+" "+getNullable(true) +"\n");
////	sbSql.append("   ,"+fill(qic+"dependParent"     +qic,40)+" "+fill(getDatatype("varchar",  255,-1,-1),20)+" "+getNullable(true) +"\n");
////	sbSql.append("   ,"+fill(qic+"dependLevel"      +qic,40)+" "+fill(getDatatype("int",       -1,-1,-1),20)+" "+getNullable(false)+"\n");
////	sbSql.append("   ,"+fill(qic+"dependList"       +qic,40)+" "+fill(getDatatype("varchar", 1500,-1,-1),20)+" "+getNullable(true) +"\n");
////	sbSql.append("   ,"+fill(qic+"objectText"       +qic,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(true) +"\n");
////	sbSql.append("   ,"+fill(qic+"dependsText"      +qic,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(true) +"\n");
////	sbSql.append("   ,"+fill(qic+"optdiagText"      +qic,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(true) +"\n");
////	sbSql.append("   ,"+fill(qic+"extraInfoText"    +qic,40)+" "+fill(getDatatype("text",      -1,-1,-1),20)+" "+getNullable(true) +"\n");
////	sbSql.append("\n");
////	sbSql.append("   ,PRIMARY KEY ("+qic+"dbname"+qic+", "+qic+"owner"+qic+", "+qic+"objectName"+qic+")\n");
////	sbSql.append(") \n");
//
////	public static class DdlDetails
////	{
////		public String    _dbname;
////		public String    _owner;
////		public String    _objectName;
////		public String    _type;
////		public Timestamp _crdate;
////		public String    _source;
////		public String    _dependParent;
////		public int       _dependLevel;
////		public String    _dependList;
////		public String    _objectText;
////		public String    _dependsText;
////		public String    _optdiagText;
////		public String    _extraInfoText;
////	}
//
//	public DdlDetails getDdlDetailes(String dbname, String type, String objectName, String owner)
//	{
//		long fetchStartTime = System.currentTimeMillis();
//
//		String sql = 
//			" select * " +
//			" from " + PersistWriterBase.getTableName(PersistWriterBase.DDL_STORAGE, null, true) + " " +
//			" where \"dbname\"     = ? " +
//			"   and \"type\"       = ? " +
//			"   and \"objectName\" = ? " +
//			"   and \"owner\"      = ? ";
//
//		DdlDetails ddlDetails = null;
//		try
//		{
//			PreparedStatement pstmnt = _conn.prepareStatement(sql);
//			pstmnt.setString(1, dbname);
//			pstmnt.setString(2, type);
//			pstmnt.setString(3, objectName);
//			pstmnt.setString(4, owner);
//
//			ResultSet rs = pstmnt.executeQuery();
//
//			while (rs.next())
//			{
//				ddlDetails = new DdlDetails();
//
//				ddlDetails.setDbname(      rs.getString   ("dbname"));
//				ddlDetails.setOwner(       rs.getString   ("owner"));
//				ddlDetails.setObjectName(  rs.getString   ("objectName"));
//				ddlDetails.setType(        rs.getString   ("type"));
//				ddlDetails.setCrdate(      rs.getTimestamp("crdate"));
//				ddlDetails.setSampleTime(  rs.getTimestamp("sampleTime"));
//				ddlDetails.setSource(      rs.getString   ("source"));
//				ddlDetails.setDependParent(rs.getString   ("dependParent"));
//				ddlDetails.setDependLevel( rs.getInt      ("dependLevel"));
//				ddlDetails.setDependList(  StringUtil.parseCommaStrToList( rs.getString("dependList") ) );
//
//				ddlDetails.setObjectText(   rs.getString("objectText"));
//				ddlDetails.setDependsText(  rs.getString("dependsText"));
//				ddlDetails.setOptdiagText(  rs.getString("optdiagText"));
//				ddlDetails.setExtraInfoText(rs.getString("extraInfoText"));
//			}
//			rs.close();
//			pstmnt.close();
//		}
//		catch (SQLException e)
//		{
//			_logger.error("Problems loading 'DDL Detailes'. sql="+sql, e);
//		}
//		
//		String str = "Loading 'DDL Detailes' took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.";
////System.out.println(str);
//		_logger.debug(str);
//		setStatusText(str);
//
//		return ddlDetails;
//	}
//
//	public List<DdlDetails> getDdlObjects(boolean addBlobs)
//	{
////		setWatermark("Loading Sessions...");  // if the window wasn't visible, set the watermark now
//		long fetchStartTime = System.currentTimeMillis();
////		setStatusText("Loading 'Session Samples CM Counter Info' for sessionId '"+inSessionStartTime+"'.");
//
//		List<DdlDetails> list = new ArrayList<DdlDetails>();
//
//		String blobCols = "";
//		if (addBlobs)
//			blobCols = ", \"objectText\", \"dependsText\", \"optdiagText\", \"extraInfoText\" ";
//
//		String sql = 
////			"select * " +
//			"select \"dbname\", \"owner\", \"objectName\", \"type\", \"crdate\", \"sampleTime\", \"source\", \"dependParent\", \"dependLevel\", \"dependList\" " + blobCols +
////			"select \"dbname\", \"owner\", \"objectName\", \"type\", \"crdate\", \"sampleTime\", \"source\", \"dependLevel\", \"dependList\" " + blobCols +
//			"from " + PersistWriterBase.getTableName(PersistWriterBase.DDL_STORAGE, null, true) + " " +
//			"order by \"dbname\", \"type\", \"objectName\", \"owner\" ";
//
//		try
//		{
//			PreparedStatement pstmnt = _conn.prepareStatement(sql);
////			pstmnt.setString(1, inSessionStartTime.toString());
//
//			// No timeout
//			pstmnt.setQueryTimeout(0);
//			
//			ResultSet rs = pstmnt.executeQuery();
////			ResultSetMetaData rsmd = rs.getMetaData();
////			int cols = rsmd.getColumnCount();
//
//			while (rs.next())
//			{
//				DdlDetails ddlDetails = new DdlDetails();
//
//				ddlDetails.setDbname(      rs.getString   ("dbname"));
//				ddlDetails.setOwner(       rs.getString   ("owner"));
//				ddlDetails.setObjectName(  rs.getString   ("objectName"));
//				ddlDetails.setType(        rs.getString   ("type"));
//				ddlDetails.setCrdate(      rs.getTimestamp("crdate"));
//				ddlDetails.setSampleTime(  rs.getTimestamp("sampleTime"));
//				ddlDetails.setSource(      rs.getString   ("source"));
//				ddlDetails.setDependParent(rs.getString   ("dependParent"));
//				ddlDetails.setDependLevel( rs.getInt      ("dependLevel"));
//				ddlDetails.setDependList(  StringUtil.parseCommaStrToList( rs.getString("dependList") ) );
//
//				if (addBlobs)
//				{
//					ddlDetails.setObjectText(   rs.getString("objectText"));
//					ddlDetails.setDependsText(  rs.getString("dependsText"));
//					ddlDetails.setOptdiagText(  rs.getString("optdiagText"));
//					ddlDetails.setExtraInfoText(rs.getString("extraInfoText"));
//				}
//
//				list.add(ddlDetails);
//			}
//			rs.close();
//			pstmnt.close();
//		}
//		catch (SQLException e)
//		{
//			_logger.error("Problems loading 'DDL TreeView'. sql="+sql, e);
//		}
//		
//		String str = "Loading 'DDL TreeView' took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.";
////System.out.println(str);
//		_logger.debug(str);
//		setStatusText(str);
//
//		return list;
//	}
//
//
//	/*---------------------------------------------------
//	** END: DDL reader
//	**---------------------------------------------------
//	*/
}
