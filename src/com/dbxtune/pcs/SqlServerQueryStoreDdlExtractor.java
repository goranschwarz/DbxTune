/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.pcs;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.ICounterController;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.sql.SqlParserUtils;
import com.dbxtune.sql.conn.ConnectionProp;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.SqlServerUtils;
import com.dbxtune.utils.TimeUtils;

import it.sauronsoftware.cron4j.Task;
import it.sauronsoftware.cron4j.TaskExecutionContext;

public class SqlServerQueryStoreDdlExtractor
extends Task
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String  PROPKEY_start = "SqlServerQueryStoreDdlExtractor.start";
	public static final boolean DEFAULT_start = true;

	public static final String PROPKEY_cron = "SqlServerQueryStoreDdlExtractor.cron";
	public static final String DEFAULT_cron = "35 23 * * *"; // at 23:35 every day

	public static final String PROPKEY_topCount = "SqlServerQueryStoreDdlExtractor.top.count";
	public static final int    DEFAULT_topCount = 40;


	@Override
	public void execute(TaskExecutionContext context) 
	throws RuntimeException
	{
//		_logger.info("");
//		_logger.info("#############################################################################################");
		_logger.info("Begin task: Extract Table Names from top " + getTopCount() + " SQL Statements from Query Store");

		long startTime = System.currentTimeMillis();

		extractTopTablesForDdlStorage();

		_logger.info("End task: Extract Table Names from top " + getTopCount() + " SQL Statements from Query Store. execTime=" + TimeUtils.msDiffNowToTimeStr("%?HH[:]%MM:%SS.%ms", startTime));
	}

	private void extractTopTablesForDdlStorage()
	{
		if ( ! PersistentCounterHandler.hasInstance() )
		{
			_logger.info("Query Store DDL Extractor: No Persistent Counter Handler was found. exiting early in extractTopTablesForDdlStorage()");
			return;
		}

		ICounterController counterController = CounterController.getInstance();
		
		if ( ! counterController.isMonConnected() )
		{
			_logger.info("Not connected to any DBMS, skipping this.");
			return;
		}

		// Check if Capture Query Store is DISABLED for a specific server 
		String srvName = counterController.getMonConnection().getDbmsServerNameNoThrow();
//		String captureQueryStoreSkipServer = Configuration.getCombinedConfiguration().getProperty(PROPKEY_onPcsDatabaseRollover_captureQueryStore_skipServer, DEFAULT_onPcsDatabaseRollover_captureQueryStore_skipServer);
//		if (StringUtil.hasValue(captureQueryStoreSkipServer) && StringUtil.hasValue(srvName))
//		{
//			try
//			{
//				if ( srvName.matches(captureQueryStoreSkipServer) )
//				{
//					_logger.info("On PCS Database Rollover: Skipping 'Capture Query Store' for Server Name '" + srvName + "', due to Configuration '" + PROPKEY_onPcsDatabaseRollover_captureQueryStore_skipServer + "' is set to '" + captureQueryStoreSkipServer + "'.");
//				}
//			}
//			catch (PatternSyntaxException ex) 
//			{
//				_logger.error("On PCS Database Rollover: Problems with regular expression '" + captureQueryStoreSkipServer + "' in cofiguration '" + PROPKEY_onPcsDatabaseRollover_captureQueryStore_skipServer + "'. Skipping this and Continuing...", ex);
//			}
//		}

		// Open a new connection to the monitored server
		ConnectionProp connProp = counterController.getMonConnection().getConnPropOrDefault();
		DbxConnection conn = null;
		try
		{
			_logger.info("Query Store DDL Extractor: Creating a new connection to server '" + srvName+ "'.");
			conn = DbxConnection.connect(null, connProp);
		}
		catch (Exception ex)
		{
			_logger.error("Query Store DDL Extractor: Failed to establish a new connection to server '" + srvName + "'. SKIPPING this.", ex);
			return;
		}

		// Get databases which has Query Store Enabled
		List<String> enabledDatabases = new ArrayList<>();
		String sql = ""
			    + "declare @dbnames table(dbame nvarchar(128)) \n"
			    + "INSERT INTO @dbnames \n"
			    + "exec sys.sp_MSforeachdb 'select ''?'' from [?].sys.database_query_store_options where desired_state != 0' \n"
			    + "SELECT * FROM @dbnames \n"
			    + "";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while (rs.next())
				enabledDatabases.add(rs.getString(1));
		}
		catch (SQLException ex)
		{
			_logger.error("Query Store DDL Extractor: Problems when getting list of databases that has 'Query Store' enabled on '" + srvName + "'. SKIPPING this. SQL=|" + sql + "|.", ex);
		}

		// if we have "Query Store enabled" in any databases Extract them
		if (enabledDatabases.isEmpty())
		{
			_logger.info("Query Store DDL Extractor: No databases had 'Query Store' enabled on server '" + srvName + "'. Skipping this.");
		}
		else
		{
			_logger.info("Query Store DDL Extractor: Examin 'Query Store' On server '" + srvName+ "' for the following " + enabledDatabases.size() + " database(s): " + enabledDatabases);

			// loop and extract each of the databases
			for (String dbname : enabledDatabases)
			{
				try
				{
					extractTopTablesForDdlStorage(conn, dbname);
//					SqlServerQueryStoreExtractor qse = new SqlServerQueryStoreExtractor(dbname, daysToCopy, conn, pcsConn);
//					qse.transfer();
				}
				catch (Exception ex)
				{
					_logger.error("Query Store DDL Extractor: Problems extracting 'Query Store' from server '" + srvName + "', database '" + dbname + "'.", ex);
				}
			}
		}

		// Close
		if (conn != null)
		{
			_logger.info("Query Store DDL Extractor: Closing connection to server '" + srvName + "'..");
			conn.closeNoThrow();
		}
		
	}

	private int getTopCount()
	{
		return Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_topCount, DEFAULT_topCount);
	}

	private void extractTopTablesForDdlStorage(DbxConnection conn, String dbname)
	{
		int top      = getTopCount();
		int topInner = top * 10;

		// Get known columns from SQL Server Query Store
		List<String> orderByCols = new ArrayList<>();
		String dummySql = "SELECT * FROM [sys].[query_store_runtime_stats] WHERE 1=2";
		ResultSetTableModel dummyRstm = null;
		try
		{
			dummyRstm = ResultSetTableModel.executeQuery(conn, dummySql, "metadata");
		}
		catch (SQLException ex)
		{
			_logger.error("Query Store DDL Extractor: [" + dbname + "] SKIPPING extractTopTablesForDdlStorage. Problems getting column info from 'query_store_runtime_stats' using sql '" + dummySql + "'.", ex);
			return;
		}

		// What columns do we want to do ORDER BY on
		if (dummyRstm.hasColumn("count_executions"         )) orderByCols.add("count_executions"         );
		if (dummyRstm.hasColumn("avg_duration"             )) orderByCols.add("avg_duration"             );
		if (dummyRstm.hasColumn("avg_cpu_time"             )) orderByCols.add("avg_cpu_time"             );
		if (dummyRstm.hasColumn("avg_logical_io_reads"     )) orderByCols.add("avg_logical_io_reads"     );
		if (dummyRstm.hasColumn("avg_logical_io_writes"    )) orderByCols.add("avg_logical_io_writes"    );
		if (dummyRstm.hasColumn("avg_physical_io_reads"    )) orderByCols.add("avg_physical_io_reads"    );
		if (dummyRstm.hasColumn("avg_query_max_used_memory")) orderByCols.add("avg_query_max_used_memory");
		if (dummyRstm.hasColumn("avg_rowcount"             )) orderByCols.add("avg_rowcount"             );
		if (dummyRstm.hasColumn("avg_num_physical_io_reads")) orderByCols.add("avg_num_physical_io_reads");
		if (dummyRstm.hasColumn("avg_log_bytes_used"       )) orderByCols.add("avg_log_bytes_used"       );
		if (dummyRstm.hasColumn("avg_tempdb_space_used"    )) orderByCols.add("avg_tempdb_space_used"    );

		if (orderByCols.isEmpty())
		{
			_logger.error("Query Store DDL Extractor: [" + dbname + "] SKIPPING extractTopTablesForDdlStorage. Problems: NO columns for ORDER BY was found. orderByCols is EMPTY.");
			return;
		}
		
		// Somewhere to store the parsed table names
		Set<String> tableSet = new HashSet<>();

		long startTime = System.currentTimeMillis();

		// Get SQL Statements by: orderByCols
		for (String orderByCol : orderByCols)
		{
			String sql = ""
				    + "SELECT top " + top + " \n"
				    + "     '" + dbname + "' AS dbname \n"
				    + "    ,qt.query_sql_text \n"
//				    + "    ,q.query_id \n" 
//				    + "    ,p.plan_id \n"
//				    + "    ,qt.query_text_id \n"
				    + "FROM       [" + dbname + "].sys.query_store_query_text qt \n"
				    + "INNER JOIN [" + dbname + "].sys.query_store_query q ON qt.query_text_id = q.query_text_id \n"
				    + "INNER JOIN [" + dbname + "].sys.query_store_plan  p ON q.query_id       = p.query_id \n"
				    + "WHERE p.plan_id IN ( \n"
				    + "    SELECT top " + topInner + " rs.plan_id \n" // note: top ### since plan_id can exists in many intervals (and we can't do distinct and order on avg_duration)
				    + "    FROM [" + dbname + "].sys.query_store_runtime_stats rs \n"
				    + "    ORDER BY rs." + orderByCol + " DESC \n"
				    + ") \n"
				    + "AND qt.query_sql_text NOT LIKE '%/* SqlServerTune:%' \n"
				    + "";

			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
				{
					String sqlText = rs.getString(2);
					
					// Get list of tables in the SQL Text and add them to a SET
					Set<String> tmpSet = SqlParserUtils.getTables(sqlText);
					for (String sqlObjName : tmpSet)
					{
						sqlObjName = SqlServerUtils.stripSquareBrackets(sqlObjName);
						tableSet.add(sqlObjName);
					}
				}
			}
			catch (SQLException ex)
			{
				_logger.warn("Query Store DDL Extractor: [" + dbname + "] Problems extracting SQL Text for top '" + orderByCol + "' in QueryStore. Simply skipping this. SQL=|" + sql + "|.", ex);
			}
		}

		// Send them off for DDL Extraction!
		if ( ! tableSet.isEmpty() )
		{
			// Send all the tables for DDL Store (it will probably end up in the NEW database since a YYYY-MM-DD roll-over has occurred, but then we will hopefully be able to se them "next day") 
			for (String tableName : tableSet)
			{
				PersistentCounterHandler.getInstance().addDdl(dbname, tableName, "QueryStoreDdlExtractor.parsedTables");
			}

			_logger.info("Query Store DDL Extractor: [" + dbname + "] Sent " + tableSet.size() + " tables for DDL Lookup/Store. execTime=" + TimeUtils.msDiffNowToTimeStr("%?HH[:]%MM:%SS.%ms", startTime) + ". Tables=" + tableSet);
		}
	}
}
