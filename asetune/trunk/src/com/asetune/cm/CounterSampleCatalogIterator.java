package com.asetune.cm;

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

import com.asetune.IGuiController;
import com.asetune.gui.MainFrame;
import com.asetune.utils.AseSqlScript;
import com.asetune.utils.StringUtil;

public class CounterSampleCatalogIterator 
extends CounterSample
{
	private static final long serialVersionUID = 1L;
	private static Logger     _logger          = Logger.getLogger(CounterSampleCatalogIterator.class);

	public CounterSampleCatalogIterator(String name, boolean negativeDiffCountersToZero, String[] diffColNames, CounterSample prevSample)
	{
		super(name, negativeDiffCountersToZero, diffColNames, prevSample);
	}
	
	/**
	 * Get all databases/catalogs we want to interrogate
	 * @param cm
	 * @param conn
	 * @return
	 * @throws SQLException
	 */
	protected List<String> getCatalogList(CountersModel cm, Connection conn)
	throws SQLException
	{
//		String sql = "select name from master.dbo.sysdatabases";

		ArrayList<String> list = new ArrayList<String>();

		ResultSet rs = conn.getMetaData().getCatalogs();
		while(rs.next())
		{
			String dbname = rs.getString(1);
			list.add(dbname);
		}
		return list;
	}

	/**
	 * This is a special getSample(), below is what it does<br>
	 * <ul>
	 *    <li>Get sample time</li>
	 *    <li>Get databases that we should interrogate</li>
	 *    <li>Loop over all the databases</li>
	 *    <ul>
	 *       <li>set context to correct database: use dbname</li>
	 *       <li>Execute the SQL constructed by the CM</li>
	 *    </ul>
	 * </ul>
	 * After everything is executed
	 * <ul>
	 *    <li>Restore the original database context</li>
	 * </ul>
	 */
	@Override
	public boolean getSample(CountersModel cm, Connection conn, String sql, List<String> pkList)
	throws SQLException, NoValidRowsInSample
	{
		int queryTimeout = cm.getQueryTimeout();
		if (_logger.isDebugEnabled())
			_logger.debug(getName()+": queryTimeout="+queryTimeout);

		String originCatalog = "";
		try
		{
			originCatalog = conn.getCatalog();

			String sendSql = sql;

			// update/set the current refresh time and interval
			updateSampleTime(conn, cm);

			// Get the list of databases
			List<String> dblist = getCatalogList(cm, conn);

			// create a "bucket" where all the rows will end up in ( add will be done in method: readResultset() )
			_rows = new ArrayList<List<Object>>();

			// Loop over all the databases
			for (String catname : dblist)
			{
				// set context to the correct database
				conn.setCatalog(catname);
				if (_logger.isDebugEnabled())
					_logger.debug("Setting database context to '"+catname+"'.");

				if (cm.getGuiController() != null)
						cm.getGuiController().setStatus(MainFrame.ST_STATUS2_FIELD, "for db '"+catname+"'");

				Statement stmnt = conn.createStatement();
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

							ResultSetMetaData rsmd = rs.getMetaData();
							if ( ! cm.hasResultSetMetaData() )
								cm.setResultSetMetaData(rsmd);
	
							if (readResultset(cm, rs, rsmd, pkList, rsNum))
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
		finally
		{
			// Restore database context
			if ( StringUtil.hasValue(originCatalog) )
			{
				if (cm.getGuiController() != null)
					cm.getGuiController().setStatus(MainFrame.ST_STATUS2_FIELD, "restoring DB Context to '"+originCatalog+"'");

				try { conn.setCatalog(originCatalog); }
				catch (SQLException ex) { _logger.warn("Problems restoring the current catalog/dbname to '"+originCatalog+"'. Caught: "+ex); }

				if (cm.getGuiController() != null)
					cm.getGuiController().setStatus(MainFrame.ST_STATUS2_FIELD, "");
			}
		}
	}
}
