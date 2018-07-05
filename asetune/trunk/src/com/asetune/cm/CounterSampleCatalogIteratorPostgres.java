package com.asetune.cm;

import java.awt.Window;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.gui.MainFrame;
import com.asetune.sql.conn.ConnectionProp;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.DbxConnectionPool;
import com.asetune.sql.conn.DbxConnectionPoolMap;
import com.asetune.utils.AseSqlScript;

public class CounterSampleCatalogIteratorPostgres 
extends CounterSampleCatalogIterator
{
	private static final long serialVersionUID = 1L;
	private static Logger     _logger          = Logger.getLogger(CounterSampleCatalogIteratorPostgres.class);

	// This is used if no databases are in a "valid" state.
	private List<String> _fallbackList = null;

	private static DbxConnectionPoolMap _cpm;
	

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
	public CounterSampleCatalogIteratorPostgres(String name, boolean negativeDiffCountersToZero, String[] diffColNames, CounterSample prevSample, List<String> fallbackList)
	{
		super(name, negativeDiffCountersToZero, diffColNames, prevSample);
		_fallbackList = fallbackList;
		
		if (_cpm == null)
			_cpm = new DbxConnectionPoolMap();
	}
	
	@Override
	protected List<String> getCatalogList(CountersModel cm, Connection conn)
	throws SQLException
	{
		String sql = "select datname from pg_catalog.pg_database where datname not like 'template%' order by 1";

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

		return list;
	}

	private DbxConnection getConnection(CountersModel cm, DbxConnection srvConn, String dbname)
	throws SQLException
	{
		if (srvConn == null)
			throw new RuntimeException("The 'template' Connection can not be null.");
		
		if (_cpm == null)
			throw new RuntimeException("Connection pool Map is not initialized");

		// Are we in GUI mode or not (connections can then use)
		Window guiOwner = null;
//		guiOwner = cm.getGuiController().getGuiHandle();
		if (MainFrame.hasInstance())
			guiOwner = MainFrame.getInstance();

		// reuse a connction if one exists
		if (_cpm.hasMapping(dbname))
		{
			// Set status
			if (cm != null && cm.getGuiController() != null)
				cm.getGuiController().setStatus(MainFrame.ST_STATUS2_FIELD, "get conn to db '"+dbname+"'");
			
			return _cpm.getPool(dbname).getConnection(guiOwner);
		}

		// Grab the ConnectionProperty from the template Connection
		ConnectionProp connProp = srvConn.getConnProp();
		if (connProp == null)
			throw new SQLException("No ConnectionProperty object could be found at the template connection.");
		
		// Clone the ConnectionProp
		connProp = new ConnectionProp(connProp);

		// Set the new database name
		String url = connProp.getUrl();
		if (url.indexOf("/postgres") == -1)
			throw new SQLException("Initial connection 'template' has to be made to the 'postgres' database");
			
		url = url.replace("/postgres", "/"+dbname);
		connProp.setUrl(url);
		
		// Create a new connection pool for this DB
		DbxConnectionPool cp = new DbxConnectionPool(this.getClass().getSimpleName(), connProp, 5); // Max size = 5

		// Set status in GUI if available
		if (cm != null && cm.getGuiController() != null)
			cm.getGuiController().setStatus(MainFrame.ST_STATUS2_FIELD, "Connecting to db '"+dbname+"'");

		// grab a new conntion.
		DbxConnection dbConn = cp.getConnection(guiOwner);

		_logger.info("Created a new Connection for db '"+dbname+"', which will be cached in a connection pool. with maxSize=5, url='"+url+"', connProp="+connProp);
		
		// when first connection is successfull, add the connection pool to the MAP
		_cpm.setPool(dbname, cp);
		
		return dbConn;
	}

	/**
	 * Relase a connection
	 * 
	 * @param cm
	 * @param dbConn
	 * @param dbname
	 */
	private void releaseConnection(CountersModel cm, DbxConnection dbConn, String dbname)
	{
		if (dbConn == null)
			return;

		if (_cpm == null)
			throw new RuntimeException("Connection pool Map is not initialized");
		
		if (_cpm.hasMapping(dbname))
		{
			_cpm.getPool(dbname).releaseConnection(dbConn);
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
						checkWarnings(stmnt);
						do
						{
							if (hasRs)
							{
								// Get next result set to work with
								rs = stmnt.getResultSet();
								checkWarnings(stmnt);

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

								checkWarnings(stmnt);
			
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
			
						checkWarnings(stmnt);
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
			_logger.warn("CounterSample("+getName()+").getCnt : " + sqlEx.getErrorCode() + " " + sqlEx.getMessage() + ". SQL: "+sql, sqlEx);
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
