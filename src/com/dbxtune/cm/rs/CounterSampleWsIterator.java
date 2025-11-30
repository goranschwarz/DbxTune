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
package com.dbxtune.cm.rs;

import java.awt.Window;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.NoValidRowsInSample;
import com.dbxtune.cm.rs.helper.AdminLogicalStatusEntry;
import com.dbxtune.cm.rs.helper.AdminLogicalStatusList;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.ResultSetMetaDataCached;
import com.dbxtune.sql.conn.ConnectionProp;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.DbxConnectionPool;
import com.dbxtune.sql.conn.DbxConnectionPoolMap;
import com.dbxtune.utils.AseSqlScript;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

public class CounterSampleWsIterator 
extends CounterSample
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long serialVersionUID = 1L;

	private static DbxConnectionPoolMap _cpm;
	private static long _lastUpdateOfActive = 0;   // Note: CounterSampleWsIterator is created every time... so this needs to be static

	private static Map<Integer, LastKnownInfo> _lastKnownInfo;   // Note: CounterSampleWsIterator is created every time... so this needs to be static
	
	/** Small class to hold various data for Connections that has WS COnnection is status "down" */
	private static class LastKnownInfo
	{
		private int       _logicalId;
		private Timestamp _lastKnownOriginCommitTime = null;
		private Timestamp _lastKnownDestCommitTime   = null;
		
		public LastKnownInfo(int logicalId, Timestamp originCommitTime, Timestamp destCommitTime)
		{
			_logicalId = logicalId;
			_lastKnownOriginCommitTime = originCommitTime;
			_lastKnownDestCommitTime   = destCommitTime;
		}

//		public void setLastKnownOriginCommitTime(Timestamp originCommitTime) { _lastKnownOriginCommitTime = originCommitTime; }
//		public void setLastKnownDestCommitTime  (Timestamp destCommitTime)   { _lastKnownDestCommitTime   = destCommitTime; }

		public Timestamp getLastKnownOriginCommitTime() { return _lastKnownOriginCommitTime; }
		public Timestamp getLastKnownDestCommitTime  () { return _lastKnownDestCommitTime; }
	}

	public static void closeConnPool()
	{
		if (_cpm != null)
			_cpm.close();
		_cpm = null;
	}

	/**
	 * @param name
	 * @param negativeDiffCountersToZero
	 * @param diffColNames
	 * @param prevSample
	 * @param fallbackList a list of database(s) that will be used in case of "no valid" databases can be found, typically usage is "tempdb" to at least get one database.
	 */
	public CounterSampleWsIterator(String name, boolean negativeDiffCountersToZero, String[] diffColNames, CounterSample prevSample)
	{
		super(name, negativeDiffCountersToZero, diffColNames, prevSample);
		
		if (_cpm == null)
			_cpm = new DbxConnectionPoolMap();

		if (_lastKnownInfo == null)
			_lastKnownInfo = new HashMap<>();
	}
	
	
	private List<AdminLogicalStatusEntry> getWsList(CountersModel cm, DbxConnection conn)
	throws SQLException
	{
		return AdminLogicalStatusList.getList(conn);
	}

//	private DbxConnection getConnection(CountersModel cm, DbxConnection srvConn, String name)
//	throws SQLException
//	{
//		if (srvConn == null)
//			throw new RuntimeException("The 'template' Connection can not be null.");
//		
//		if (_cpm == null)
//			throw new RuntimeException("Connection pool Map is not initialized");
//
//		// If not a valid name, return null
//		if (StringUtil.isNullOrBlank(name))
//			throw new RuntimeException("Not a valid name");
//		
//		// Are we in GUI mode or not (connections can then use)
//		Window guiOwner = null;
////		guiOwner = cm.getGuiController().getGuiHandle();
//		if (MainFrame.hasInstance())
//			guiOwner = MainFrame.getInstance();
//
//		// reuse a connction if one exists
//		if (_cpm.hasMapping(name))
//		{
//			// Set status
//			if (cm != null && cm.getGuiController() != null)
//				cm.getGuiController().setStatus(MainFrame.ST_STATUS2_FIELD, "get conn to WS '"+name+"'");
//			
////			return _cpm.getPool(name).getConnection(guiOwner);
//			// FIXME: we can NOT return here, we first need to check in some way if we are in a Gateway connection... otherwise we need to create a GW connection.
//			DbxConnection connPoolConn = _cpm.getPool(name).getConnection(guiOwner);
//			return connPoolConn;
//		}
//
//		// Grab the ConnectionProperty from the template Connection
//		ConnectionProp connProp = srvConn.getConnProp();
//		if (connProp == null)
//			throw new SQLException("No ConnectionProperty object could be found at the template connection.");
//		
//		// Clone the ConnectionProp
//		connProp = new ConnectionProp(connProp);
//
////		// Set the new database name
////		String url = connProp.getUrl();
////		if (url.indexOf("/postgres") == -1)
////			throw new SQLException("Initial connection 'template' has to be made to the 'postgres' database");
////			
////		url = url.replace("/postgres", "/"+name);
////		connProp.setUrl(url);
//		
//		// Create a new connection pool for this DB
//		DbxConnectionPool cp = new DbxConnectionPool(this.getClass().getSimpleName(), connProp, 5); // Max size = 5
//
//		// Set status in GUI if available
//		if (cm != null && cm.getGuiController() != null)
//			cm.getGuiController().setStatus(MainFrame.ST_STATUS2_FIELD, "Connecting to WS '"+name+"'");
//
//		// grab a new connection.
//		DbxConnection dbConn = cp.getConnection(guiOwner);
//
//		_logger.info("Created a new Connection for WS '"+name+"', which will be cached in a connection pool. with maxSize=5, connProp="+connProp);
//
//		String sql = "connect to "+name;
//		_logger.info("Issuing a Gateway connection for WS '"+name+"', rcl cmd: "+sql);
//
//		try (Statement stmnt = dbConn.createStatement())
//		{
//			stmnt.executeUpdate(sql);
//		}
//		catch (SQLException ex)
//		{
//			int error = ex.getErrorCode(); 
//			if (error == 15539 || error == 15540)
//			{
//				// Skipping messages
//				// 15539 - Gateway connection to 'SRV.db' is created.
//				// 15540 - Gateway connection to 'SRV.db' is dropped.
//				_logger.info(ex.getMessage());
//			}
//			else
//			{
//				throw ex;
//			}
//		}
//		
//		// when first connection is successfull, add the connection pool to the MAP
//		_cpm.setPool(name, cp);
//		
//		return dbConn;
//	}

	/**
	 * Get a connection 
	 * 
	 * @param cm
	 * @param srvConn
	 * @param name
	 * @return
	 * @throws SQLException
	 */
	private DbxConnection getConnection(CountersModel cm, DbxConnection srvConn, String name)
	throws SQLException
	{
		if (srvConn == null)
			throw new RuntimeException("The 'template' Connection can not be null.");
		
		if (_cpm == null)
			throw new RuntimeException("Connection pool Map is not initialized");

		// If not a valid name, return null
		if (StringUtil.isNullOrBlank(name))
			throw new RuntimeException("Not a valid name");
		
		// Are we in GUI mode or not (connections can then use)
		Window guiOwner = null;
//		guiOwner = cm.getGuiController().getGuiHandle();
		if (MainFrame.hasInstance())
			guiOwner = MainFrame.getInstance();

		// Get the Connection pool, if it do not exists: create one
		DbxConnectionPool cp = _cpm.getPool(name);
		if (cp == null)
		{
			// Grab the ConnectionProperty from the template Connection
			ConnectionProp connProp = srvConn.getConnProp();
			if (connProp == null)
				throw new SQLException("No ConnectionProperty object could be found at the template connection.");
			
			// Clone the ConnectionProp
			connProp = new ConnectionProp(connProp);

			// Create a new connection pool for this DB
			cp = new DbxConnectionPool(this.getClass().getSimpleName(), connProp, 5); // Max size = 5

			// set the new connection pool in the map
			_cpm.setPool(name, cp);

			_logger.info("Created a new Connection Pool for WS '"+name+"', with maxSize=5, connProp="+connProp);
		}

		// Set status in GUI if available
		if (cm != null && cm.getGuiController() != null)
			cm.getGuiController().setStatus(MainFrame.ST_STATUS2_FIELD, "get conn to WS '"+name+"'");
			
		// Get a connection from the connection pool (it could be new; or reused, so we need to check if it's in a RS-GateWay-Connection or not)
		DbxConnection dbConn = cp.getConnection(guiOwner);

		// Should we check the connection if it's ok...
		// and if it's in a Gateway Connection
		//      RCL 'show server'
		//      if in a GW Connection it returns something:
		//                         PROD_REP_RSSD.PROD_REP_RSSD
		//      if it's NOT GW Connected yet, then we get "error": 
		//                         Msg 2056, Level 12, State 0:
		//                         Server 'PROD_REP', Line 0 (script row 4322), Status 0, TranState 0:
		//                         Line 1, character 1: Incorrect syntax with 'show'.
		
		// If we already has a RepServer Gateway Connection to the destination database, THEN no more needs to be done.
		if (dbConn.hasProperty("GW-CONNECTION"))
		{
			return dbConn;
		}

		// If we havent created a Gateway Connection to the Destination SRV... issue 'connect to SRV.db'
		String sql = "connect to "+name;

		_logger.info("Issuing a Gateway connection for WS '"+name+"', rcl cmd: "+sql);

		try (Statement stmnt = dbConn.createStatement())
		{
			stmnt.executeUpdate(sql);
		}
		catch (SQLException ex)
		{
			int error = ex.getErrorCode(); 
			if (error == 15539 || error == 15540)
			{
				// Skipping messages
				// 15539 - Gateway connection to 'SRV.db' is created.
				// 15540 - Gateway connection to 'SRV.db' is dropped.
				_logger.info(ex.getMessage());
			}
			else
			{
				// Release the connection from the pool if we had errors
				// if NOT: the pool will get full soon
				cp.releaseConnection(dbConn);
				
				// Now throw the error
				throw ex;
			}
		}

		// Mark this connection as
		dbConn.setProperty("GW-CONNECTION", name);
		
		return dbConn;
	}

	/**
	 * Relase a connection
	 * 
	 * @param cm
	 * @param dbConn
	 * @param dbname
	 */
	private void releaseConnection(CountersModel cm, DbxConnection dbConn, String name)
	{
		if (dbConn == null)
			return;

		if (_cpm == null)
			throw new RuntimeException("Connection pool Map is not initialized");
		
		if (_cpm.hasMapping(name))
		{
			_cpm.getPool(name).releaseConnection(dbConn);
		}
		else
		{
			// The connection pool did not exists, close this connection.
			_logger.info("When trying to 'give back' a connection to the connection pool with key '"+name+"'. The key could not be found, so CLOSING the connection instead.");
			
			// Close the connection...
			dbConn.closeNoThrow();
		}
	}

	/**
	 * Create a record from a AdminLogicalStatusEntry, but with all latency information as NULL's
	 * @param e
	 * @return
	 */
	private List<Object> createSkipRow(AdminLogicalStatusEntry e, String standbyMsg)
	{
		List<Object> row = new ArrayList<>();
		
		row.add(e.getLogicalConnId());           // "LogicalId",           java.sql.Types.INTEGER,   "int",         
		row.add(e.getLogicalConnName());         // "LogicalName",         java.sql.Types.VARCHAR,   "varchar(80)", 
                                                 
		row.add(e.getActiveConnId());            // "ActiveId",            java.sql.Types.INTEGER,   "int",         
		row.add(e.getActiveConnName());          // "ActiveName",          java.sql.Types.VARCHAR,   "varchar(80)", 
		row.add(e.getActiveConnState());         // "ActiveState",         java.sql.Types.VARCHAR,   "varchar(80)", 

		row.add(e.getStandbyConnId());           // "StandbyId",           java.sql.Types.INTEGER,   "int",         
		row.add(e.getStandbyConnName());         // "StandbyName",         java.sql.Types.VARCHAR,   "varchar(80)", 
		row.add(e.getStandbyConnState());        // "StandbyState",        java.sql.Types.VARCHAR,   "varchar(80)", 

		LastKnownInfo lastKnownInfo = _lastKnownInfo.get(e.getLogicalConnId());
		Timestamp oct = null;
		Timestamp dct = null;
		if (lastKnownInfo != null)
		{
			oct = lastKnownInfo.getLastKnownOriginCommitTime();
			dct = lastKnownInfo.getLastKnownDestCommitTime();
		}
		if (oct != null && dct != null)
		{
			// TRY to mimic the below logic without talking to the DB, since the Connection is DOWN
			// "    LatencyInSec        = datediff(ss,         x.origin_time, x.dest_commit_time), \n" +
			// "    ApplyAgeInSec       = datediff(ss,         x.dest_commit_time, getdate()),     \n" +
			// "    ApplyAgeInMinutes   = datediff(minute,     x.dest_commit_time, getdate()),     \n" +
			// "    DataAgeInSec        = datediff(ss,         x.origin_time, getdate()),          \n" +
			// "    DataAgeInMinutes    = datediff(minute,     x.origin_time, getdate()),          \n" +
			// "    OriginCommitTime    = x.origin_time,       \n" +
			// "    DestCommitTime      = x.dest_commit_time,  \n" +
			// "    ActiveLocalTime     = convert(datetime, "+DbUtils.safeStr(wsEntry.getActiveTimestamp())+"),  \n" +
			// "    StandbyLocalTime    = getdate(),  \n" +
			// "    StandbyMsg          = convert(varchar(1024), ''),  \n" +

			long now = System.currentTimeMillis();
			int LatencyInSec      = (int) ( dct.getTime() - oct.getTime() ) / 1000;
			int ApplyAgeInSec     = (int) ( now - dct.getTime() ) / 1000;
			int ApplyAgeInMinutes = ApplyAgeInSec / 60;
			int DataAgeInSec      = (int) ( now - oct.getTime() ) / 1000;
			int DataAgeInMinutes  = DataAgeInSec / 60;

			row.add(LatencyInSec);               // "LatencyInSec",        java.sql.Types.INTEGER,   "int",         
			row.add(ApplyAgeInSec);              // "ApplyAgeInSec",       java.sql.Types.INTEGER,   "int",         
			row.add(ApplyAgeInMinutes);          // "ApplyAgeInMinutes",   java.sql.Types.INTEGER,   "int",         
			row.add(DataAgeInSec);               // "DataAgeInSec",        java.sql.Types.INTEGER,   "int",         
			row.add(DataAgeInMinutes);           // "DataAgeInMinutes",    java.sql.Types.INTEGER,   "int",         
			row.add(oct);                        // "OriginCommitTime",    java.sql.Types.TIMESTAMP, "datetime",    
			row.add(dct);                        // "DestCommitTime",      java.sql.Types.TIMESTAMP, "datetime",    
			row.add(e.getActiveTimestamp());     // "ActiveLocalTime",     java.sql.Types.TIMESTAMP, "datetime",    
			row.add(new Timestamp(now));         // "StandbyLocalTime",    java.sql.Types.TIMESTAMP, "datetime",    
			row.add(standbyMsg);                 // "StandbyMsg",          java.sql.Types.VARCHAR,   "varchar(1024)",
		}
		else
		{
			row.add(null);                       // "LatencyInSec",        java.sql.Types.INTEGER,   "int",         
			row.add(null);                       // "ApplyAgeInSec",       java.sql.Types.INTEGER,   "int",         
			row.add(null);                       // "ApplyAgeInMinutes",   java.sql.Types.INTEGER,   "int",         
			row.add(null);                       // "DataAgeInSec",        java.sql.Types.INTEGER,   "int",         
			row.add(null);                       // "DataAgeInMinutes",    java.sql.Types.INTEGER,   "int",         
			row.add(null);                       // "OriginCommitTime",    java.sql.Types.TIMESTAMP, "datetime",    
			row.add(null);                       // "DestCommitTime",      java.sql.Types.TIMESTAMP, "datetime",    
			row.add(null);                       // "ActiveLocalTime",     java.sql.Types.TIMESTAMP, "datetime",    
			row.add(null);                       // "StandbyLocalTime",    java.sql.Types.TIMESTAMP, "datetime",    
			row.add(standbyMsg);                 // "StandbyMsg",          java.sql.Types.VARCHAR,   "varchar(1024)",
		}

		row.add(e.getRsId());                    // "RsId",                java.sql.Types.INTEGER,   "int",         
		row.add(e.getRsSrvName());               // "RsName",              java.sql.Types.VARCHAR,   "varchar(80)", 

		row.add(e.getOpInProgress());            // "OpInProgress",        java.sql.Types.VARCHAR,   "varchar(128)",
		row.add(e.getStateOfOpInProgress());     // "StateOfOpInProgress", java.sql.Types.VARCHAR,   "varchar(128)",
		row.add(e.getSpid());                    // "Spid",                java.sql.Types.VARCHAR,   "varchar(10)", 

		return row;
	}

//private int _deleteme_callCount_getSample = 0;
	/**
	 * This is a special getSample(), below is what it does<br>
	 * <ul>
	 *    <li>Get sample time</li>
	 *    <li>Get databases (logical connections) that we should interrogate</li>
	 *    <li>Loop over all the databases (logical connections)</li>
	 *    <ul>
	 *       <li>Active: Optionally: Update the Active side every ## minute</li>
	 *       <li>Standby: Make a connection (using a connection pool), using RepServer Gateway Connection (so we don't have to hassle with user/password)</li>
	 *       <li>Standby: Execute the SQL to get values</li>
	 *       <li>Standby: release the connection (to the connection pool)</li>
	 *       <li>Standby: On errors a row will still be created, column 'standbyMsg' will hold any message details.</li>
	 *    </ul>
	 * </ul>
	 */
	@Override
	public boolean getSample(CountersModel cm, DbxConnection srvConn, String sql, List<String> pkList)
	throws SQLException, NoValidRowsInSample
	{
		int queryTimeout = cm.getQueryTimeout();
		if (_logger.isDebugEnabled())
			_logger.debug(getName()+": queryTimeout="+queryTimeout);

		
		// Create a RSMD that will be used
		ResultSetMetaDataCached rsmd = new ResultSetMetaDataCached();
		
		//             columnName,            columnType,               columnTypeName, nullable, columnClassName             columnDisplaySize, precision, scale, 
		rsmd.addColumn("LogicalId",           java.sql.Types.INTEGER,   "int",          false,    Integer  .class.getName(),  12,                10,        0);
		rsmd.addColumn("LogicalName",         java.sql.Types.VARCHAR,   "varchar",      false,    String   .class.getName(),  80,                80,        0);
                                                                                                                                                      
		rsmd.addColumn("ActiveId",            java.sql.Types.INTEGER,   "int",          true,     Integer  .class.getName(),  12,                10,        0);
		rsmd.addColumn("ActiveName",          java.sql.Types.VARCHAR,   "varchar",      true,     String   .class.getName(),  80,                80,        0);
		rsmd.addColumn("ActiveState",         java.sql.Types.VARCHAR,   "varchar",      true,     String   .class.getName(),  80,                80,        0);
                                                                                                                                                      
		rsmd.addColumn("StandbyId",           java.sql.Types.INTEGER,   "int",          true,     Integer  .class.getName(),  12,                10,        0);
		rsmd.addColumn("StandbyName",         java.sql.Types.VARCHAR,   "varchar",      true,     String   .class.getName(),  80,                80,        0);
		rsmd.addColumn("StandbyState",        java.sql.Types.VARCHAR,   "varchar",      true,     String   .class.getName(),  80,                80,        0);
                                                                                                                                                      
		rsmd.addColumn("LatencyInSec",        java.sql.Types.INTEGER,   "int",          true,     Integer  .class.getName(),  12,                10,        0);
		rsmd.addColumn("ApplyAgeInSec",       java.sql.Types.INTEGER,   "int",          true,     Integer  .class.getName(),  12,                10,        0);
		rsmd.addColumn("ApplyAgeInMinutes",   java.sql.Types.INTEGER,   "int",          true,     Integer  .class.getName(),  12,                10,        0);
		rsmd.addColumn("DataAgeInSec",        java.sql.Types.INTEGER,   "int",          true,     Integer  .class.getName(),  12,                10,        0);
		rsmd.addColumn("DataAgeInMinutes",    java.sql.Types.INTEGER,   "int",          true,     Integer  .class.getName(),  12,                10,        0);
		rsmd.addColumn("OriginCommitTime",    java.sql.Types.TIMESTAMP, "datetime",     true,     Timestamp.class.getName(),  26,                26,        0);
		rsmd.addColumn("DestCommitTime",      java.sql.Types.TIMESTAMP, "datetime",     true,     Timestamp.class.getName(),  26,                26,        0);
		rsmd.addColumn("ActiveLocalTime",     java.sql.Types.TIMESTAMP, "datetime",     true,     Timestamp.class.getName(),  26,                26,        0);
		rsmd.addColumn("StandbyLocalTime",    java.sql.Types.TIMESTAMP, "datetime",     true,     Timestamp.class.getName(),  26,                26,        0);
		rsmd.addColumn("StandbyMsg",          java.sql.Types.VARCHAR,   "varchar",      true,     String   .class.getName(),  1024,              1024,      0);
                                                                                                                                                      
		rsmd.addColumn("RsId",                java.sql.Types.INTEGER,   "int",          true,     Integer  .class.getName(),  12,                10,        0);
		rsmd.addColumn("RsName",              java.sql.Types.VARCHAR,   "varchar",      true,     String   .class.getName(),  80,                80,        0);
                                                                                                                                                      
		rsmd.addColumn("OpInProgress",        java.sql.Types.VARCHAR,   "varchar",      true,     String   .class.getName(),  128,               128,       0);
		rsmd.addColumn("StateOfOpInProgress", java.sql.Types.VARCHAR,   "varchar",      true,     String   .class.getName(),  128,               128,       0);
		rsmd.addColumn("Spid",                java.sql.Types.VARCHAR,   "varchar",      true,     String   .class.getName(),  10,                10,        0);

		if ( ! cm.hasResultSetMetaData() )
			cm.setResultSetMetaData( rsmd );

		// Initialize the column structure (this normally done in: readResultset(...) but if we get exceptions and we don't read data, Then we add 
		// rows manually to "_rows", hence the column structure etc, are NOT initialized...
		// I think it's safe to do "up here", but we might have to move it into the "exceptions section"  
		if (getColNames() == null)
			initColumnInfo(rsmd, pkList, 1);
		
		// To remember exception in the entire loop
		List<SQLException> sqlExList = new ArrayList<>();
		
//		String originCatalog = "";
		try
		{
//			originCatalog = conn.getCatalog();

			String sendSql = sql;

			// update/set the current refresh time and interval
			updateSampleTime(srvConn, cm);

			// Get the list of databases (logical connections)
			List<AdminLogicalStatusEntry> wslist = getWsList(cm, srvConn);

			// create a "bucket" where all the rows will end up in ( add will be done in method: readResultset() )
			_rows = new ArrayList<List<Object>>();

			if (wslist.isEmpty())
			{
				_logger.info("Skipping collecting WS Latency statistics. 'admin logical_status' returned 0 rows.");
				return true;
			}

			long startTime = System.currentTimeMillis();

			boolean updateActive           = Configuration.getCombinedConfiguration().getBooleanProperty(CmWsRepLatency.PROPKEY_update_active,              CmWsRepLatency.DEFAULT_update_active);
			long updateActiveIntervalInSec = Configuration.getCombinedConfiguration().getLongProperty   (CmWsRepLatency.PROPKEY_update_activeIntervalInSec, CmWsRepLatency.DEFAULT_update_activeIntervalInSec);
			long maxWaitTimeMs             = Configuration.getCombinedConfiguration().getLongProperty   (CmWsRepLatency.PROPKEY_update_maxWaitTimeMs,       CmWsRepLatency.DEFAULT_update_maxWaitTimeMs);

			// How many seconds since last update...
			long secondsSinceLastActiveUpdate = TimeUtils.msDiffNow(_lastUpdateOfActive) / 1000;
//System.out.println("----- secondsSinceLastActiveUpdate="+secondsSinceLastActiveUpdate+", updateActiveIntervalInSec="+updateActiveIntervalInSec+", doUpdate="+(secondsSinceLastActiveUpdate > updateActiveIntervalInSec));
			//---------------------------------------------------------
			// ACTIVE SIDE: Loop over all the databases
			//---------------------------------------------------------
			if (updateActive && secondsSinceLastActiveUpdate > updateActiveIntervalInSec)
			{
				_lastUpdateOfActive = System.currentTimeMillis();
				
				for (AdminLogicalStatusEntry wsEntry : wslist)
				{
					DbxConnection dbConn = null;
					String        name   = wsEntry.getActiveConnName();

					String state = wsEntry.getActiveConnState();
					if ( ! "Active/".equalsIgnoreCase(state) )
					{
						_logger.info("Skipping ACTIVE Connection '"+name+"', State should be in 'Active/', and current status is in '"+state+"'.");
						continue;
					}
					
					try
					{
						// Grab a connection (from the connection pool)
						dbConn = getConnection(cm, srvConn, name);
						
						if (cm.getGuiController() != null)
							cm.getGuiController().setStatus(MainFrame.ST_STATUS2_FIELD, "update active '"+name+"'");

						String updateTable = 
								"-- Create the dummy table if it do not exist \n" +
								"if not exists (select 1 from sysobjects where name = 'rsTune_ws_dummy_update' and type = 'U') \n" +
								"   exec('CREATE TABLE rsTune_ws_dummy_update(id varchar(30), activeServerName varchar(80), ts datetime, primary key(id))') \n" +
								"-- remove all old rows \n" +
								"exec('DELETE FROM rsTune_ws_dummy_update') \n" +
								"-- insert a new dummy row \n" +
								"exec('INSERT INTO rsTune_ws_dummy_update(id, activeServerName, ts) SELECT newid(), @@servername+''/''+suser_name(), getdate()') \n" +
								"";
						try ( Statement stmnt = dbConn.createStatement() )
						{
							stmnt.executeUpdate(updateTable);
						}
						catch (SQLException ex)
						{
							_logger.warn("Problems updating dummy table 'rsTune_ws_dummy_update' at '"+name+"'. But continuing with next step. Caught: Error="+ex.getErrorCode()+", Msg='"+ex.getMessage().trim()+"', SQL="+updateTable);
						}

						try ( Statement stmnt = dbConn.createStatement(); ResultSet rs = stmnt.executeQuery("select getdate()") )
						{
							while(rs.next())
								wsEntry.setActiveTimestamp(rs.getTimestamp(1));
						}
						catch (SQLException ex)
						{
							_logger.warn("Problems executing 'select getdate()' at '"+name+"'. But continuing with next step. Caught: Error="+ex.getErrorCode()+", Msg='"+ex.getMessage().trim()+"'. ACTION: Closing the connection");
							// Closing the connection... for example if the Gateway connection has failed and we are still in RepServer...
							dbConn.closeNoThrow();
						}
					}
					catch (SQLException sqlEx)
					{
						_logger.warn("CounterSample("+getName()+").getCnt : ACTIVE='"+name+"', ErrorCode=" + sqlEx.getErrorCode() + ", Message=|" + sqlEx.getMessage() + "|. Inner-ACTION: Closing the connection.");
						if (dbConn != null)
							dbConn.closeNoThrow();

						sqlExList.add(sqlEx);
					}
					finally 
					{
						releaseConnection(cm, dbConn,  name);
					}
				}
				
				// wait "a while" for the records to be replicated...
				while (TimeUtils.msDiffNow(startTime) < maxWaitTimeMs)
				{
					if (cm != null && cm.getGuiController() != null)
						cm.getGuiController().setStatus(MainFrame.ST_STATUS2_FIELD, "Wait a short while for data to be replicated.");

					try { Thread.sleep(50); }
					catch (InterruptedException ignore) {}
				}
			}
			
			//---------------------------------------------------------
			// STANDBY SIDE: Loop over all the databases
			//---------------------------------------------------------
			for (AdminLogicalStatusEntry wsEntry : wslist)
			{
				DbxConnection dbConn = null;
				String        name   = wsEntry.getStandbyConnName();

				String state = wsEntry.getStandbyConnState();
				if ( ! "Active/".equalsIgnoreCase(state) )
				{
					//_logger.info("Skipping STANDBY Connection '"+name+"', State should be in 'Active/', and current status is in '"+state+"'.");
					
					_rows.add(createSkipRow(wsEntry, "StandbyState != 'Active/'"));
//					addRow(cm, createSkipRow(wsEntry, "StandbyState != 'Active/'")); // this wont work here
					continue;
				}

//				1> admin logical_status
//				RS> Col# Label                          JDBC Type Name         Guessed DBMS type Source Table
//				RS> ---- ------------------------------ ---------------------- ----------------- ------------
//				RS> 1    Logical Connection Name        java.sql.Types.VARCHAR varchar(75)       -none-      
//				RS> 2    Active Connection Name         java.sql.Types.VARCHAR varchar(75)       -none-      
//				RS> 3    Active Conn State              java.sql.Types.VARCHAR varchar(61)       -none-      
//				RS> 4    Standby Connection Name        java.sql.Types.VARCHAR varchar(75)       -none-      
//				RS> 5    Standby Conn State             java.sql.Types.VARCHAR varchar(61)       -none-      
//				RS> 6    Controller RS                  java.sql.Types.VARCHAR varchar(75)       -none-      
//				RS> 7    Operation in Progress          java.sql.Types.VARCHAR varchar(122)      -none-      
//				RS> 8    State of Operation in Progress java.sql.Types.VARCHAR varchar(122)      -none-      
//				RS> 9    Spid                           java.sql.Types.VARCHAR varchar(5)        -none-      
//				+-----------------------+------------------------+-----------------+------------------------+------------------+-------------------+---------------------+------------------------------+----+
//				|Logical Connection Name|Active Connection Name  |Active Conn State|Standby Connection Name |Standby Conn State|Controller RS      |Operation in Progress|State of Operation in Progress|Spid|
//				+-----------------------+------------------------+-----------------+------------------------+------------------+-------------------+---------------------+------------------------------+----+
//				|[180] LDS1.b2b         |[186] PROD_A1_ASE.b2b   |Active/          |[197] PROD_B1_ASE.b2b   |Active/           |[16777317] PROD_REP|None                 |None                          |    |
//				|[179] LDS1.gorans      |[184] PROD_A1_ASE.gorans|Active/          |[194] PROD_B1_ASE.gorans|Active/           |[16777317] PROD_REP|None                 |None                          |    |
//				|[182] LDS1.Linda       |[190] PROD_A1_ASE.Linda |Active/          |[199] PROD_B1_ASE.Linda |Active/           |[16777317] PROD_REP|None                 |None                          |    |
//				|[181] LDS1.mts         |[188] PROD_A1_ASE.mts   |Active/          |[198] PROD_B1_ASE.mts   |Active/           |[16777317] PROD_REP|None                 |None                          |    |
//				|[183] LDS1.PML         |[192] PROD_A1_ASE.PML   |Active/          |[204] PROD_B1_ASE.PML   |Active/           |[16777317] PROD_REP|None                 |None                          |    |
//				+-----------------------+------------------------+-----------------+------------------------+------------------+-------------------+---------------------+------------------------------+----+
//				(5 rows affected)

				// Hardcode the SQL for now...
				sendSql = "select \n" + 
						"    LogicalId           = convert(int,         " +wsEntry.getLogicalConnId()+"), \n" +
						"    LogicalName         = convert(varchar(80), '"+wsEntry.getLogicalConnName()+"'), \n" +
						" \n" +                  
						"    ActiveId            = convert(int,         " +wsEntry.getActiveConnId()+"), \n" +
						"    ActiveName          = convert(varchar(80), '"+wsEntry.getActiveConnName()+"'), \n" +
						"    ActiveState         = convert(varchar(80), '"+wsEntry.getActiveConnState()+"'), \n" +
						" \n" +                  
						"    StandbyId           = convert(int,         " +wsEntry.getStandbyConnId()+"), \n" +
						"    StandbyName         = convert(varchar(80), '"+wsEntry.getStandbyConnName()+"'), \n" +
						"    StandbyState        = convert(varchar(80), '"+wsEntry.getStandbyConnState()+"'), \n" +
						" \n" +
						"    LatencyInSec        = datediff(ss,         x.origin_time, x.dest_commit_time), \n" +
						"    ApplyAgeInSec       = datediff(ss,         x.dest_commit_time, getdate()),     \n" +
						"    ApplyAgeInMinutes   = datediff(minute,     x.dest_commit_time, getdate()),     \n" +
						"    DataAgeInSec        = datediff(ss,         x.origin_time, getdate()),          \n" +
						"    DataAgeInMinutes    = datediff(minute,     x.origin_time, getdate()),          \n" +
						"    OriginCommitTime    = x.origin_time,       \n" +
						"    DestCommitTime      = x.dest_commit_time,  \n" +
						"    ActiveLocalTime     = convert(datetime, "+DbUtils.safeStr(wsEntry.getActiveTimestamp())+"),  \n" +
						"    StandbyLocalTime    = getdate(),  \n" +
						"    StandbyMsg          = convert(varchar(1024), ''),  \n" +
						" \n" +
						"    RsId                = convert(int,         " +wsEntry.getRsId()+"), \n" +
						"    RsName              = convert(varchar(80), '"+wsEntry.getRsSrvName()+"'), \n" +
						" \n" +
						"    OpInProgress        = convert(varchar(128), '"+wsEntry.getOpInProgress()+"'), \n" +
						"    StateOfOpInProgress = convert(varchar(128), '"+wsEntry.getStateOfOpInProgress()+"'), \n" +
						"    Spid                = convert(varchar(10),  '"+wsEntry.getSpid()+"') \n" +
						" \n" +
						" from "+wsEntry.getStandbyConnDbName()+".dbo.rs_lastcommit x \n" +
						" where origin = "+wsEntry.getActiveConnId()+" \n" +
						"";
				
				// To make it visible in the "tab", and also if we got exception... The Correct SQL Will be printed.
				cm.setSql(sendSql);
				
				try
				{
//_deleteme_callCount_getSample++;
//boolean doDummyThrow = true;
//if (doDummyThrow && _deleteme_callCount_getSample > 3)
//	throw new SQLException("Dummy Connection FAIL.");

					// Grab a connection (from the connection pool)
					dbConn = getConnection(cm, srvConn, name);

					// NOTE: If above connection fails (via the RepServer Gateway Connection)... we will throw a connection error...
					//       which causes the CM's data to be "invalid"... So no Alarms etc can be checked/fired
					//       Is this desired, or should we just set NULL values for the columns "standby columns" (LatenceInSec, ApplyAgeInSec, ApplyAgeInMinutes, DataAge..., OriginCommitTime, DestCommitTime, StandbyLocalTime)
					// Think about the above... 
					//       Also possible to add a column "StandbyConnectionMessage" -- If we have connection errors etc, we can put the message in here...
					//       Maybe update a "simulated" ApplyAge etc... based on last "date" + sampleTime or similar "stuff"

					if (cm.getGuiController() != null)
							cm.getGuiController().setStatus(MainFrame.ST_STATUS2_FIELD, "check standby '"+name+"'");

					Statement stmnt = dbConn.createStatement();
					ResultSet rs;

					stmnt.setQueryTimeout(queryTimeout); // XX seconds query timeout
					if (_logger.isDebugEnabled())
						_logger.debug("QUERY_TIMEOUT="+queryTimeout+", for SampleCnt='"+getName()+"'.");


					// Allow 'go' in the string, then we should send multiple batches
					// this will take care about dropping tempdb tables prior to executing a batch that depends on it.
					// is a query batch we can't do:
					//     if ((select object_id('#cacheInfo')) is not null) drop table #cacheInfo 
					//     select CacheName, CacheID into #cacheInfo from master..monCachePool 
					// The second row will fail...
					//     Msg 12822, Level 16, State 1:
					//     Server 'GORAN_1_DS', Line 5, Status 0, TranState 0:
					//     Cannot create temporary table '#cacheInfo'. Prefix name '#cacheInfo' is already in use by another temporary table '#cacheInfo'.
					// So we need to send the statemenmts in two separate batches
					// so instead do:
					//     if ((select object_id('#cacheInfo')) is not null) drop table #cacheInfo 
					//     go
					//     select CacheName, CacheID into #cacheInfo from master..monCachePool 
					// Then it works...


					// treat each 'go' rows as a individual execution
					// readCommand(), does the job
					//int batchCount = AseSqlScript.countSqlGoBatches(sendSql);
					int batchCounter = 0;
					BufferedReader br = new BufferedReader( new StringReader(sendSql) );
					for(String sqlBatch=AseSqlScript.readCommand(br); sqlBatch!=null; sqlBatch=AseSqlScript.readCommand(br))
					{
						sendSql = sqlBatch;

						if (_logger.isDebugEnabled())
						{
							_logger.debug("##### BEGIN (send sql), batchCounter="+batchCounter+" ############################### "+ getName());
							_logger.debug(sendSql);
							_logger.debug("##### END   (send sql), batchCounter="+batchCounter+" ############################### "+ getName());
							_logger.debug("");
						}

						int rsNum = 0;
						int rowsAffected = 0;
						boolean hasRs = stmnt.execute(sendSql);
						checkWarnings(cm, stmnt);
						do
						{
							if (hasRs)
							{
								// Get next result set to work with
								rs = stmnt.getResultSet();
								checkWarnings(cm, stmnt);

								ResultSetMetaData originRsmd = rs.getMetaData();
								ResultSetMetaData cmRsmd     = cm.getResultSetMetaData();

								if (readResultset(cm, rs, cmRsmd, originRsmd, pkList, rsNum))
									rs.close();

								checkWarnings(cm, stmnt);
			
								rsNum++;
							}
							else
							{
								// Treat update/row count(s)
								rowsAffected = stmnt.getUpdateCount();

								if (rowsAffected >= 0)
								{
									_logger.debug("DDL or DML rowcount = "+rowsAffected);
								}
								else
								{
									_logger.debug("No more results to process.");
								}
							}
			
							// Check if we have more result sets
							hasRs = stmnt.getMoreResults();
			
							_logger.trace( "--hasRs="+hasRs+", rsNum="+rsNum+", rowsAffected="+rowsAffected );
						}
						while (hasRs || rowsAffected != -1);
			
						checkWarnings(cm, stmnt);
						batchCounter++;
					}
					br.close();

					// Close the statement
					stmnt.close();
					
					
					// update LAST KNOW: OriginCommitTime & DestCommitTime in the AdminLogicalStatusEntry
					// This so that we can still present some values if the State is NOT 'Active'
					if (getRowCount() > 0)
					{
						int LogicalId_col = findColumn("LogicalId");
						int row = -1;
						if (LogicalId_col != -1)
						{
							for (int r=0; r<getRowCount(); r++)
							{
								Integer LogicalId = (Integer) getValueAt(r, LogicalId_col);
								if (LogicalId != null && LogicalId == wsEntry.getLogicalConnId())
								{
									row = r;
									break;
								}
							}
						}
						
						if (row != -1)
						{
							int OriginCommitTime_col = findColumn("OriginCommitTime");
							int DestCommitTime_col   = findColumn("DestCommitTime");

							if (OriginCommitTime_col != -1 && DestCommitTime_col != -1)
							{
								Timestamp OriginCommitTime = (Timestamp) getValueAt(row, OriginCommitTime_col);
								Timestamp DestCommitTime   = (Timestamp) getValueAt(row, DestCommitTime_col);
								
								LastKnownInfo lki = new LastKnownInfo(wsEntry.getLogicalConnId(), OriginCommitTime, DestCommitTime);
								_lastKnownInfo.put(lki._logicalId, lki);
//								wsEntry.setLastKnownOriginCommitTime(OriginCommitTime);
//								wsEntry.setLastKnownDestCommitTime  (DestCommitTime);
							}
						}
					}
				}
				catch (SQLException sqlEx)
				{
					String standbyMsg = "STANDBY='"+name+"', ErrorCode=" + sqlEx.getErrorCode() + ", Message=" + sqlEx.getMessage();

					// Add a row (with most "Standby" fields empty/null), but the "StandbyMsg" filled in with the ERROR message 
					_rows.add(createSkipRow(wsEntry, standbyMsg));
//					addRow(cm, createSkipRow(wsEntry, standbyMsg)); // this wont work here

					// Closing the connection... for example if the Gateway connection has failed and we are still in RepServer...
					_logger.warn("CounterSample("+getName()+").getCnt : STANDBY='"+name+"', ErrorCode=" + sqlEx.getErrorCode() + ", Message=|" + sqlEx.getMessage() + "|. Inner-ACTION: Closing the connection.");
					if (dbConn != null)
						dbConn.closeNoThrow();
					
					// If we throw the exception here, it will abort this "refresh"
					// It's better to continue to loop until we have "all" done ALL dbnames in the list.
					// and THROW it AFTER the loop...
					//throw sqlEx;

					sqlExList.add(sqlEx);
				}
				finally 
				{
					releaseConnection(cm, dbConn,  name);
				}
			} // end: loop dbnames

			// --------------
			// ---- NOTE ----
			// --------------
			// -- If we throw any (SQL)Exception here, POST handling like "Alarm Handling" will NOT be kicked off
			// -- SO: If we think we have a "object" that can be "interrogate", then DO NOT THROW here (possibly just set set the exception)
			// --------------
			if (sqlExList.size() > 0)
			{
				// Only throw if we have NOT added any rows
				boolean doThrow = _rows.isEmpty();

//				SQLException crEx = new SQLException("In the Active/Standby check we had "+sqlExList.size()+" SQLException(only first Exception): "+sqlExList.get(0));
				if (doThrow)
				{
					// NOTE: if we THROW here: POST handling like "Alarm Handling" will NOT be kicked off
//					throw crEx;
				}
				else
				{
					// Set the exception so that the GUI well SEE that there is "some" problems
					// HOPEFULLY the NO-GUI will not examen the "setSampleException()" and bail out... (at least from what I can see in the code)
					// ... otherwise we can do: if (cm.getGuiController() != null && cm.getGuiController().hasGUI()) cm.setSampleException(crEx); 
//					cm.setSampleException(crEx);
					// it looks like it was a BAD idea calling: cm.setSampleException(crEx); 
					// Things did NOT work as expected... investigate a bit more here...
					// However since we have the "StandbyMsg" column now, maybe the "GUI WaterMark message" is not needed...
					
					_logger.warn("In the Active/Standby check we had "+sqlExList.size()+" Below are this list: ");
					for (int ec= 0; ec<sqlExList.size(); ec++)
						_logger.warn("  -- Active/Standby check Exception["+ec+"]: " + sqlExList.get(ec));

					for (int r=0; r<_rows.size(); r++)
						_logger.info("Just for intermidiate DEBUG: Row[" + r + "] still contains the following values: "+ _rows.get(r));
				}
			}

			return true;
		}
		catch (SQLException sqlEx)
		{
			if (sqlExList.size() == 0)
			{
				_logger.warn("CounterSample("+getName()+").getCnt : ErrorCode=" + sqlEx.getErrorCode() + ", Message=|" + sqlEx.getMessage() + "|. SQL: "+sql, sqlEx);
				if (sqlEx.toString().indexOf("SocketTimeoutException") > 0)
				{
					_logger.info("QueryTimeout in '"+getName()+"', with query timeout '"+queryTimeout+"'. This can be changed with the config option '"+getName()+".queryTimeout=seconds' in the config file.");
				}
			}
			else
			{
				for (SQLException x : sqlExList)
				{
					if (x.toString().indexOf("SocketTimeoutException") > 0)
					{
						_logger.info("QueryTimeout in '"+getName()+"', with query timeout '"+queryTimeout+"'. This can be changed with the config option '"+getName()+".queryTimeout=seconds' in the config file.");
					}
				}
			}

			//return false;
			throw sqlEx;
		}
		catch (IOException ex)
		{
			_logger.error("While reading the input SQL 'go' String, caught: "+ex, ex);
			throw new SQLException("While reading the input SQL 'go' String, caught: "+ex, ex);
		}
		catch (Exception ex)
		{
			_logger.error("Problems when connecting to DBMS via connection pool, caught: "+ex, ex);
			throw new SQLException("Problems when connecting to DBMS via connection pool, caught: "+ex, ex);
		}
		finally
		{
//			// Restore database context
//			if ( StringUtil.hasValue(originCatalog) )
//			{
//				if (cm.getGuiController() != null)
//					cm.getGuiController().setStatus(MainFrame.ST_STATUS2_FIELD, "restoring DB Context to '"+originCatalog+"'");
//
//				try { conn.setCatalog(originCatalog); }
//				catch (SQLException ex) { _logger.warn("Problems restoring the current catalog/dbname to '"+originCatalog+"'. Caught: "+ex); }
//			}
			
			if (cm.getGuiController() != null)
				cm.getGuiController().setStatus(MainFrame.ST_STATUS2_FIELD, "");
		}
	}
}
