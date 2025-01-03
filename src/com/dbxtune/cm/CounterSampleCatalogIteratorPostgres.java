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
package com.dbxtune.cm;

import java.awt.Window;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.JdbcUrlParser;
import com.dbxtune.sql.conn.ConnectionProp;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.DbxConnectionPool;
import com.dbxtune.sql.conn.DbxConnectionPoolMap;
import com.dbxtune.utils.AseSqlScript;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;
import com.dbxtune.utils.TimeUtils;

public class CounterSampleCatalogIteratorPostgres 
extends CounterSampleCatalogIterator
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long serialVersionUID = 1L;

	// This is used if no databases are in a "valid" state.
	private List<String> _fallbackList = null;

//	private static DbxConnectionPoolMap _cpm;

	private static Set<String> _dbSkipSet = new HashSet<>();


	public static void closeConnPool()
	{
//		if (_cpm != null)
//			_cpm.close();
//		_cpm = null;
		if (DbxConnectionPoolMap.hasInstance())
		{
			DbxConnectionPoolMap.getInstance().close();
			DbxConnectionPoolMap.setInstance(null);
		}
		
		// Reset some other locals
		_dbSkipSet = new HashSet<>();
	}

	/**
	 * @param name
	 * @param negativeDiffCountersToZero
	 * @param diffColNames
	 * @param prevSample
	 * @param fallbackList a list of database(s) that will be used in case of "no valid" databases can be found, typically usage is "tempdb" to at least get one database.
	 */
	public CounterSampleCatalogIteratorPostgres(String name, boolean negativeDiffCountersToZero, String[] diffColNames, CounterSample prevSample, List<String> fallbackList)
	{
		super(name, negativeDiffCountersToZero, diffColNames, prevSample);
		_fallbackList = fallbackList;
		
//		if (_cpm == null)
//			_cpm = new DbxConnectionPoolMap();
		if ( ! DbxConnectionPoolMap.hasInstance() )
			DbxConnectionPoolMap.setInstance(new DbxConnectionPoolMap());
	}
	
	@Override
	protected List<String> getCatalogList(CountersModel cm, Connection conn)
	throws SQLException
	{
		String sql 
				= "select datname \n"
				+ "from pg_database \n"
//				+ "where datname not like 'template%' \n"
				+ "where NOT datistemplate \n"
				+ "  and has_database_privilege(datname, 'CONNECT') \n" // Possibly add this to only lookup databases that we have access to
				+ "order by 1 \n";

		ArrayList<String> list = new ArrayList<String>();

		Statement stmnt = conn.createStatement();
		ResultSet rs = stmnt.executeQuery(sql);
		while(rs.next())
		{
			String dbname = rs.getString(1);
			list.add(dbname);
		}
		
		// If the above get **no** databases that are in correct state add the "passed" database(s)
		if (list.isEmpty())
		{
			if (_fallbackList != null)
				list.addAll(_fallbackList);
		}
		
		if ( ! _dbSkipSet.isEmpty() )
		{
			list.removeAll(_dbSkipSet);
		}

		return list;
	}

	public static DbxConnection getConnection(CountersModel cm, DbxConnection srvConn, String dbname)
	throws SQLException
	{
		if (srvConn == null)
			throw new RuntimeException("The 'template' Connection can not be null.");
		
//		if (_cpm == null)
//			throw new RuntimeException("Connection pool Map is not initialized");
		if ( ! DbxConnectionPoolMap.hasInstance() )
			throw new RuntimeException("Connection pool Map is not initialized");

		// Are we in GUI mode or not (connections can then use)
		Window guiOwner = null;
//		guiOwner = cm.getGuiController().getGuiHandle();
		if (MainFrame.hasInstance())
			guiOwner = MainFrame.getInstance();

		DbxConnectionPoolMap cpm = DbxConnectionPoolMap.getInstance();
		
		// reuse a connection if one exists
//		if (_cpm.hasMapping(dbname))
		if (cpm.hasMapping(dbname))
		{
			// Set status
			if (cm != null && cm.getGuiController() != null)
				cm.getGuiController().setStatus(MainFrame.ST_STATUS2_FIELD, "get conn to db '"+dbname+"'");
			
//			return _cpm.getPool(dbname).getConnection(guiOwner);
			return cpm.getPool(dbname).getConnection(guiOwner);
		}

		// Grab the ConnectionProperty from the template Connection
//		ConnectionProp connProp = srvConn.getConnProp();
		ConnectionProp connProp = srvConn.getConnPropOrDefault();
		if (connProp == null)
			throw new SQLException("No ConnectionProperty object could be found at the template connection.");
		
		// Clone the ConnectionProp
		connProp = new ConnectionProp(connProp);

//		// Set the new database name
//		String url = connProp.getUrl();
//		if (url.indexOf("/postgres") == -1)
//			throw new SQLException("Initial connection 'template' has to be made to the 'postgres' database");
//			
//		url = url.replace("/postgres", "/"+dbname);
//		connProp.setUrl(url);

		// Set the new database name
		String url = connProp.getUrl();
		JdbcUrlParser p = JdbcUrlParser.parse(url); 
		p.setPath("/"+dbname); // set the new database name

		url = p.toUrl();
		connProp.setUrl(url);
		
		// Create a new connection pool for this DB
		DbxConnectionPool cp = new DbxConnectionPool(CounterSampleCatalogIteratorPostgres.class.getSimpleName(), connProp, 5); // Max size = 5

		// Set status in GUI if available
		if (cm != null && cm.getGuiController() != null)
			cm.getGuiController().setStatus(MainFrame.ST_STATUS2_FIELD, "Connecting to db '"+dbname+"'");

		try
		{
			// grab a new connection.
			DbxConnection dbConn = cp.getConnection(guiOwner);

			_logger.info("Created a new Connection for db '"+dbname+"', which will be cached in a connection pool. with maxSize=5, url='"+url+"', connProp="+connProp);

			// Make the same settings as for a new Monitor Connection
			if (CounterController.hasInstance())
				CounterController.getInstance().onMonConnect(dbConn);

			// when first connection is successful, add the connection pool to the MAP
//			_cpm.setPool(dbname, cp);
			cpm.setPool(dbname, cp);
			
			return dbConn;
		}
		catch (SQLException ex)
		{
			String msg = ex.getMessage();
			if (StringUtil.isNullOrBlank(msg))
				throw ex;
				
			// Check the problem... In some cases we might want to add the database to the "skip list"
			
			// For example in GCP - Google Cloud Platform, we MIGHT not have access to the database 'cloudsqladmin', which seems like it's protected by 'pg_hba.conf'
			if (msg.contains("FATAL: pg_hba.conf rejects connection"))
			{
				_logger.error("When trying to connect to database '" + dbname + "' we got rejected from HBA Conf subsystem. Adding this database to the 'skip list'...");
				_dbSkipSet.add(dbname);

				if (guiOwner != null)
				{
					SwingUtils.showErrorMessage(guiOwner, 
							"Problems Connecting to '" + dbname + "'.", 
							"<html>"
							+ "Problems connecting to database '" + dbname + "'.<br>"
							+ "Adding this database to a <b>skip list</b>, so we wont try do connect at next sample...<br>"
							+ "<br>"
							+ "Current Skip list: " + _dbSkipSet + "<br>"
							+ "</html>"
							, ex);
				}
			}
			
			throw ex;
			
		}
	}

	/**
	 * Relase a connection
	 * 
	 * @param cm
	 * @param dbConn
	 * @param dbname
	 */
	public static void releaseConnection(CountersModel cm, DbxConnection dbConn, String dbname)
	{
		if (dbConn == null)
			return;

//		if (_cpm == null)
//			throw new RuntimeException("Connection pool Map is not initialized");
		if ( ! DbxConnectionPoolMap.hasInstance() )
			throw new RuntimeException("Connection pool Map is not initialized");
		
		DbxConnectionPoolMap cpm = DbxConnectionPoolMap.getInstance();
		
//		if (_cpm.hasMapping(dbname))
//		{
//			_cpm.getPool(dbname).releaseConnection(dbConn);
//		}
		if (cpm.hasMapping(dbname))
		{
			cpm.getPool(dbname).releaseConnection(dbConn);
		}
		else
		{
			// The connection pool did not exists, close this connection.
			_logger.info("When trying to 'give back' a connection to the connection pool with key '"+dbname+"'. The key could not be found, so CLOSING the connection instead.");
			
			// Close the connection...
			dbConn.closeNoThrow();
		}
	}

	/**
	 * This is a special getSample(), below is what it does<br>
	 * <ul>
	 *    <li>Get sample time</li>
	 *    <li>Get databases that we should interrogate</li>
	 *    <li>Loop over all the databases</li>
	 *    <ul>
	 *       <li>Make a connection (using a connection pool)</li>
	 *       <li>Execute the SQL constructed by the CM</li>
	 *       <li>release the connection (to the connection pool)</li>
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

		long execStartTime = System.currentTimeMillis();

//		String originCatalog = "";
		try
		{
//			originCatalog = conn.getCatalog();

			String sendSql = sql;

			// update/set the current refresh time and interval
			updateSampleTime(srvConn, cm);

			// Get the list of databases
			List<String> dblist = getCatalogList(cm, srvConn);

			// create a "bucket" where all the rows will end up in ( add will be done in method: readResultset() )
			_rows = new ArrayList<List<Object>>();

			// Loop over all the databases
			for (String catname : dblist)
			{
				execStartTime = System.currentTimeMillis();

				DbxConnection dbConn = null;
				try
				{
					// Grab a connection (from the connection pool)
					dbConn = getConnection(cm, srvConn, catname);

					// set context to the correct database
//					conn.setCatalog(catname);
					if (_logger.isDebugEnabled())
						_logger.debug("Setting database context to '"+catname+"'.");

					if (cm.getGuiController() != null)
							cm.getGuiController().setStatus(MainFrame.ST_STATUS2_FIELD, "for db '"+catname+"'");

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

//								ResultSetMetaData rsmd = rs.getMetaData();
//								if ( ! cm.hasResultSetMetaData() )
//									cm.setResultSetMetaData(rsmd);

								ResultSetMetaData originRsmd = rs.getMetaData();
								if ( ! cm.hasResultSetMetaData() )
									cm.setResultSetMetaData( cm.createResultSetMetaData(originRsmd) );

								// The above "remapps" some things...
								//  - Like in Oracle 'NUMBER(0,-127)' is mapped to INTEGER
								// So we should use this when calling readResultset()...
								ResultSetMetaData translatedRsmd = cm.getResultSetMetaData();

								if (readResultset(cm, rs, translatedRsmd, originRsmd, pkList, rsNum))
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
				}
				finally 
				{
					releaseConnection(cm, dbConn, catname);
				}
			} // end: loop dbnames

			return true;
		}
		catch (SQLException sqlEx)
		{
			long execTime = TimeUtils.msDiffNow(execStartTime);

			_logger.warn("CounterSample("+getName()+").getCnt : ErrorCode=" + sqlEx.getErrorCode() + ", SqlState=" + sqlEx.getSQLState() + ", Message=|" + sqlEx.getMessage() + "|. execTimeInMs=" + execTime + ", SQL: "+sql, sqlEx);
			if (sqlEx.toString().indexOf("SocketTimeoutException") > 0)
			{
				_logger.info("QueryTimeout in '"+getName()+"', with query timeout '"+queryTimeout+"'. This can be changed with the config option '"+getName()+".queryTimeout=seconds' in the config file.");
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
			_logger.error("Problems when connecting to Postgres via connection pool, caught: "+ex, ex);
			throw new SQLException("Problems when connecting to Postgres via connection pool, caught: "+ex, ex);
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
