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
package com.asetune.pcs;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.sql.ResultSetMetaDataCached;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.ddl.IDbmsDdlResolver;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;
import com.asetune.utils.Ver;

public class SqlServerQueryStoreExtractor
{
	private static Logger _logger = Logger.getLogger(SqlServerQueryStoreExtractor.class);

	private String        _monDbName;
	private String        _pcsSchemaName;

	private DbxConnection _monConn;
	private DbxConnection _pcsConn;

	/** period: -1 = Everything. above 0 = Number of days to extract */
	private int           _period = -1;
	private Timestamp     _periodStartTimeLocal;
	private Timestamp     _periodStopTimeLocal;
	private Timestamp     _periodStartTimeUtc;
	private Timestamp     _periodStopTimeUtc;

	private Timestamp     _recordingStartTime;

	private int _totalRowsTransferred  = 0;
	private int _totalTableTransferred = 0;

	public enum QsTables
	{
		 database_query_store_options         // settings
		,query_context_settings               // --1:m-->> query_store_query(context_settings_id)
		,query_store_query_text               // --1:m-->> query_store_query(query_text_id)
		,query_store_query                    // --1:m-->> query_store_plan(query_id)
		,query_store_plan                     // --1:m-->> query_store_runtime_stats(plan_id), query_store_wait_stats(plan_id)
		,query_store_wait_stats               // --1:m-->> query_store_runtime_stats_interval(runtime_stats_interval_id)
		,query_store_runtime_stats            // --1:m-->> query_store_runtime_stats_interval(runtime_stats_interval_id)
		,query_store_runtime_stats_interval   // intervals
		
		,database_automatic_tuning_options    // Automatic Tuning (2017 and beyond)
		,dm_db_tuning_recommendations         // Recommendations from the (Query Store, 2017 and beyond)
	};
	//------------------------------------------------------------------------------------------------------------
	// Check for ER diagram: https://docs.microsoft.com/en-us/sql/relational-databases/performance/how-query-store-collects-data?view=sql-server-ver15
	//------------------------------------------------------------------------------------------------------------
	// database_query_store_options         -->> https://docs.microsoft.com/en-us/sql/relational-databases/system-catalog-views/sys-database-query-store-options-transact-sql?view=sql-server-ver15
	// query_context_settings               -->> https://docs.microsoft.com/en-us/sql/relational-databases/system-catalog-views/sys-query-context-settings-transact-sql?view=sql-server-ver15
	// query_store_query_text               -->> https://docs.microsoft.com/en-us/sql/relational-databases/system-catalog-views/sys-query-store-query-text-transact-sql?view=sql-server-ver15
	// query_store_query                    -->> https://docs.microsoft.com/en-us/sql/relational-databases/system-catalog-views/sys-query-store-query-transact-sql?view=sql-server-ver15
	// query_store_plan                     -->> https://docs.microsoft.com/en-us/sql/relational-databases/system-catalog-views/sys-query-store-plan-transact-sql?view=sql-server-ver15
	// query_store_wait_stats               -->> https://docs.microsoft.com/en-us/sql/relational-databases/system-catalog-views/sys-query-store-wait-stats-transact-sql?view=sql-server-ver15
	// query_store_runtime_stats            -->> https://docs.microsoft.com/en-us/sql/relational-databases/system-catalog-views/sys-query-store-runtime-stats-transact-sql?view=sql-server-ver15
	// query_store_runtime_stats_interval   -->> https://docs.microsoft.com/en-us/sql/relational-databases/system-catalog-views/sys-query-store-runtime-stats-interval-transact-sql?view=sql-server-ver15

	// dm_db_tuning_recommendations         -->> https://docs.microsoft.com/en-us/sql/relational-databases/system-dynamic-management-views/sys-dm-db-tuning-recommendations-transact-sql?view=sql-server-ver15

// Possibly also add this one: since 2017
// select * from sys.dm_db_tuning_recommendations

	
	public SqlServerQueryStoreExtractor(String dbname, int period, DbxConnection monConn, DbxConnection pcsConn)
	{
		_monDbName     = dbname;
		_pcsSchemaName = "qs:" + _monDbName;

		_monConn    = monConn;
		_pcsConn    = pcsConn;
		
		_period     = period;

		if (_period == 0)
			_period = -1;
	}

	public String getSqlForTable(QsTables tabName)
	{
		// SAME SQL for both ALL-PERIOD and LAST-X-DAYS
		if (QsTables.database_query_store_options.equals(tabName))
		{
			// Get some extra columns:
			// * stateIsOk                  -- (YES/NO) If desired_state = actual_state
			// * storageUsedPct             -- How many percent have we used up of 'max_storage_size_mb'
			// * daysInStorage              -- How many days of metrics are stored in QS
			// * expectedMaxStorageSizeMb   -- If we continue with same daily usage, we will end up using this amount of MB. (algorithm: currentSize/numberOfDays*daysToSave) 
			// * willExceedMaxStorageSizeMb -- (YES/NO) Do we need to alter 'max_storage_size_mb'? (algorithm: currentSize/numberOfDays*daysToSave < max_storage_size_mb)
			return ""
				    + "select \n"
				    + "     CASE WHEN (desired_state = actual_state) THEN 'YES' ELSE 'NO' END AS stateIsOk \n"
				    + "    ,CAST(((current_storage_size_mb*1.0)/(max_storage_size_mb*1.0))*100.0 as numeric(10,1)) AS storageUsedPct \n"
				    + "    ,(select datediff(day, min(start_time), max(start_time)) from [" + _monDbName + "].sys.query_store_runtime_stats_interval) AS daysInStorage \n"
				    + "    ,current_storage_size_mb / (select datediff(day, min(start_time), max(start_time)) from [" + _monDbName + "].sys.query_store_runtime_stats_interval) * stale_query_threshold_days AS expectedMaxStorageSizeMb \n"
				    + "    ,CASE WHEN current_storage_size_mb / (select datediff(day, min(start_time), max(start_time)) from [" + _monDbName + "].sys.query_store_runtime_stats_interval) * stale_query_threshold_days > max_storage_size_mb \n"
				    + "          THEN 'YES' \n"
				    + "          ELSE 'NO' \n"
				    + "     END AS willExceedMaxStorageSizeMb \n"
				    + "    ,* \n"
				    + "from [" + _monDbName + "].sys." + tabName + " \n"
				    + "where 1=1"
				    + "";
		}

		if (QsTables.database_automatic_tuning_options.equals(tabName))
		{
			return "select * from [" + _monDbName + "].sys." + tabName + " where 1=1";
		}

		if (QsTables.dm_db_tuning_recommendations.equals(tabName))
		{
			return "select * from [" + _monDbName + "].sys." + tabName + " where 1=1";
		}
		
		
		// transfer EVERYTHING
		if (_period <= 0)
		{
			switch (tabName)
			{
//				case database_query_store_options      : return "select * from [" + _monDbName + "].sys." + tabName + " where 1=1";
				case query_context_settings            : return "select * from [" + _monDbName + "].sys." + tabName + " where 1=1";
				case query_store_query_text            : return "select * from [" + _monDbName + "].sys." + tabName + " where 1=1";
				case query_store_query                 : return "select * \n"
				                                                      + "    ,schema_name = CASE WHEN q.object_id = 0 THEN NULL ELSE (select s.name from [" + _monDbName + "].sys.objects o WITH (READUNCOMMITTED) inner join [" + _monDbName + "].sys.schemas s WITH (READUNCOMMITTED) ON o.schema_id = s.schema_id where o.object_id = q.object_id) END \n"
				                                                      + "    ,object_name = CASE WHEN q.object_id = 0 THEN NULL ELSE (select o.name from [" + _monDbName + "].sys.objects o WITH (READUNCOMMITTED) where o.object_id = q.object_id) END \n"
				                                                      + " from [" + _monDbName + "].sys." + tabName + " q where 1=1";
				case query_store_plan                  : return "select * from [" + _monDbName + "].sys." + tabName + " where 1=1";
				case query_store_wait_stats            : return "select * from [" + _monDbName + "].sys." + tabName + " where 1=1";
				case query_store_runtime_stats         : return "select * from [" + _monDbName + "].sys." + tabName + " where 1=1";
				case query_store_runtime_stats_interval: return "select * from [" + _monDbName + "].sys." + tabName + " where 1=1";
				case database_automatic_tuning_options : return "select * from [" + _monDbName + "].sys." + tabName + " where 1=1";
				case dm_db_tuning_recommendations      : return "select * from [" + _monDbName + "].sys." + tabName + " where 1=1";
				default:
					throw new RuntimeException("getSqlForTable(): Unhandled Query Store Table Name: " + tabName);
			}
		}
		else
		{
			// Last X days
			switch (tabName)
			{
//				case database_query_store_options:
//				{
//					// OPTIONS
//					return "select * from [" + _monDbName + "].sys." + tabName + " where 1=1";
//				}

				case query_context_settings:
				{
					// QUERY CONTEXT SETTINGS
					return    "SELECT qcs.* \n"
						    + "FROM [" + _monDbName + "].sys.query_context_settings qcs \n"
						    + "WHERE qcs.context_settings_id IN \n"
						    + "( \n"
						    + "    SELECT q.context_settings_id \n"
						    + "    FROM [" + _monDbName + "].sys.query_store_query q \n"
						    + "    WHERE q.query_id IN \n"
						    + "    ( \n"
						    + "        SELECT p.query_id \n"
						    + "        FROM [" + _monDbName + "].sys.query_store_plan p \n"
						    + "        WHERE p.plan_id IN \n"
						    + "        ( \n"
						    + "            SELECT rs.plan_id \n"
						    + "            FROM [" + _monDbName + "].sys.query_store_runtime_stats rs \n"
						    + "            WHERE rs.runtime_stats_interval_id IN \n"
						    + "            ( \n"
						    + "                SELECT rsi.runtime_stats_interval_id \n"
						    + "                FROM [" + _monDbName + "].sys.query_store_runtime_stats_interval rsi \n"
						    + "                WHERE rsi.start_time >= '" + _periodStartTimeUtc + "' \n"
						    + "                  AND rsi.end_time   <= '" + _periodStopTimeUtc  + "' \n"
						    + "            ) \n"
						    + "        ) \n"
						    + "    ) \n"
						    + ") \n"
						    + "";
				}

				case query_store_query_text:
				{
					// QUERY TEXT
					return    "SELECT qt.* \n"
						    + "FROM [" + _monDbName + "].sys.query_store_query_text qt \n"
						    + "WHERE qt.query_text_id IN \n"
						    + "( \n"
						    + "    SELECT q.query_text_id \n"
						    + "    FROM [" + _monDbName + "].sys.query_store_query q \n"
						    + "    WHERE q.query_id IN \n"
						    + "    ( \n"
						    + "        SELECT p.query_id \n"
						    + "        FROM [" + _monDbName + "].sys.query_store_plan p \n"
						    + "        WHERE p.plan_id IN \n"
						    + "        ( \n"
						    + "            SELECT rs.plan_id \n"
						    + "            FROM [" + _monDbName + "].sys.query_store_runtime_stats rs \n"
						    + "            WHERE rs.runtime_stats_interval_id IN \n"
						    + "            ( \n"
						    + "                SELECT rsi.runtime_stats_interval_id \n"
						    + "                FROM [" + _monDbName + "].sys.query_store_runtime_stats_interval rsi \n"
						    + "                WHERE rsi.start_time >= '" + _periodStartTimeUtc + "' \n"
						    + "                  AND rsi.end_time   <= '" + _periodStopTimeUtc  + "' \n"
						    + "            ) \n"
						    + "        ) \n"
						    + "    ) \n"
						    + ") \n"
						    + "";
				}
			
				case query_store_query: 
				{
					// QUERY
					return    "SELECT q.* \n" 
							+ "      ,schema_name = CASE WHEN q.object_id = 0 THEN NULL ELSE (select s.name from [" + _monDbName + "].sys.objects o WITH (READUNCOMMITTED) inner join [" + _monDbName + "].sys.schemas s WITH (READUNCOMMITTED) ON o.schema_id = s.schema_id where o.object_id = q.object_id) END \n" // get NAME of schema
							+ "      ,object_name = CASE WHEN q.object_id = 0 THEN NULL ELSE (select o.name from [" + _monDbName + "].sys.objects o WITH (READUNCOMMITTED) where o.object_id = q.object_id) END \n" // get NAME of object
						    + "FROM [" + _monDbName + "].sys.query_store_query q \n"
						    + "WHERE q.query_id IN \n"
						    + "( \n"
						    + "    SELECT p.query_id \n"
						    + "    FROM [" + _monDbName + "].sys.query_store_plan p \n"
						    + "    WHERE p.plan_id IN \n"
						    + "    ( \n"
						    + "        SELECT rs.plan_id \n"
						    + "        FROM [" + _monDbName + "].sys.query_store_runtime_stats rs \n"
						    + "        WHERE rs.runtime_stats_interval_id IN \n"
						    + "        ( \n"
						    + "            SELECT rsi.runtime_stats_interval_id \n"
						    + "            FROM [" + _monDbName + "].sys.query_store_runtime_stats_interval rsi \n"
						    + "            WHERE rsi.start_time >= '" + _periodStartTimeUtc + "' \n"
						    + "              AND rsi.end_time   <= '" + _periodStopTimeUtc  + "' \n"
						    + "        ) \n"
						    + "    ) \n"
						    + ") \n"
						    + "";
				}
			
				case query_store_plan: 
				{
					// PLANs
					return    "SELECT p.* \n"
						    + "FROM [" + _monDbName + "].sys.query_store_plan p \n"
						    + "WHERE p.plan_id IN \n"
						    + "( \n"
						    + "    SELECT rs.plan_id \n"
						    + "    FROM [" + _monDbName + "].sys.query_store_runtime_stats rs \n"
						    + "    WHERE rs.runtime_stats_interval_id IN \n"
						    + "    ( \n"
						    + "        SELECT rsi.runtime_stats_interval_id \n"
						    + "        FROM [" + _monDbName + "].sys.query_store_runtime_stats_interval rsi \n"
						    + "        WHERE rsi.start_time >= '" + _periodStartTimeUtc + "' \n"
						    + "          AND rsi.end_time   <= '" + _periodStopTimeUtc  + "' \n"
						    + "    ) \n"
						    + ") \n"
						    + "";
				}
			
				case query_store_wait_stats: 
				{
					// WAIT_STATS
					return    "SELECT ws.* \n"
						    + "FROM [" + _monDbName + "].sys.query_store_wait_stats ws \n"
						    + "WHERE ws.runtime_stats_interval_id IN \n"
						    + "( \n"
						    + "    SELECT rsi.runtime_stats_interval_id \n"
						    + "    FROM [" + _monDbName + "].sys.query_store_runtime_stats_interval rsi \n"
						    + "    WHERE rsi.start_time >= '" + _periodStartTimeUtc + "' \n"
						    + "      AND rsi.end_time   <= '" + _periodStopTimeUtc  + "' \n"
						    + ") \n"
						    + "";
				}
			
				case query_store_runtime_stats: 
				{
					// RUNTIME_STATS
					return    "SELECT rs.* \n"
						    + "FROM [" + _monDbName + "].sys.query_store_runtime_stats rs \n"
						    + "WHERE rs.runtime_stats_interval_id IN \n"
						    + "( \n"
						    + "    SELECT rsi.runtime_stats_interval_id \n"
						    + "    FROM [" + _monDbName + "].sys.query_store_runtime_stats_interval rsi \n"
						    + "    WHERE rsi.start_time >= '" + _periodStartTimeUtc + "' \n"
						    + "      AND rsi.end_time   <= '" + _periodStopTimeUtc  + "' \n"
						    + ") \n"
						    + "";
				}
			
				case query_store_runtime_stats_interval: 
				{
					// INTERVALLS
					return    "SELECT rsi.* \n"
						    + "FROM [" + _monDbName + "].sys.query_store_runtime_stats_interval rsi \n"
						    + "WHERE rsi.start_time >= '" + _periodStartTimeUtc + "' \n"
						    + "  AND rsi.end_time   <= '" + _periodStopTimeUtc  + "' \n"
						    + "";
				}

//				case database_automatic_tuning_options:
//				{
//					// GET ALL
//					return "select * from [" + _monDbName + "].sys." + tabName + " where 1=1";
//				}
//			
//				case dm_db_tuning_recommendations:
//				{
//					// GET ALL Recommendations
//					return "select * from [" + _monDbName + "].sys." + tabName + " where 1=1";
//				}
			
				default:
					throw new RuntimeException("getSqlForTable(): Unhandled Query Store Table Name: " + tabName);
			}
		}
	}

	public void transfer()
	throws SQLException
	{
		String periodStr = "everything";
		if (_period == 1)
			periodStr = "this day";
		else if (_period > 1)
			periodStr = "last " + _period + " days";

		// Get recording start time
		setPeriodStartAndEndTime();
		
		_logger.info("[" + _monDbName + "] Start: Transfer (" + periodStr + ") of Query Store, for database '" + _monDbName + "'. StartTimeUTC='" + _periodStartTimeUtc + "', EndTimeUTC='" + _periodStopTimeUtc + "', StartTimeLocal='" + _periodStartTimeLocal + "', EndTimeLocal='" + _periodStopTimeLocal + "'.");

		long startTime = System.currentTimeMillis();

		// should we flush the in memory counters to disk BEFORE we extract stuff
		// EXEC sp_query_store_flush_db;
		doQueryStoreFlush();

		// Start with -- Extract TOP ## SQL Text by "avg_duration" for today!
		// Then extract Table names from those SQL
		// Lookup those tables and store them in the DDL Storage
//		extractTopTablesForDdlStorage();
		
		// Should we do everything in a transaction??? (to get less transaction log entries)
		
		// Create a SCHEMA on the PCS
		_pcsConn.createSchemaIfNotExists(_pcsSchemaName);

		try
		{
			// Create any temporary tables
			createTempTables();

			// Transfer tables
			transferTable(QsTables.database_query_store_options);

			transferTable(QsTables.query_store_runtime_stats_interval);
			transferTable(QsTables.query_store_runtime_stats);
			transferTable(QsTables.query_store_wait_stats); // This is only available from 2017... which is checked enforced in: transferTable()

			transferTable(QsTables.query_store_plan);
			transferTable(QsTables.query_store_query);
			transferTable(QsTables.query_store_query_text);
			transferTable(QsTables.query_context_settings);

			transferTable(QsTables.database_automatic_tuning_options);
			transferTable(QsTables.dm_db_tuning_recommendations);
		}
		finally
		{
			// Drop any temporary tables
			dropTempTables();
		}

		_logger.info("[" + _monDbName + "] Totally " + _totalRowsTransferred + " rows in " + _totalTableTransferred + " tables was transferred for Query Store, in database '" + _monDbName + "' to the PCS Schema '" + _pcsSchemaName + "'. This took " + TimeUtils.msDiffNowToTimeStr(startTime) + " (HH:MM:SS.ms)");
	}

	
	private void doQueryStoreFlush()
	{
		String sql = "exec " + _monDbName + ".dbo.sp_query_store_flush_db";
		try (Statement stmnt = _monConn.createStatement())
		{
			_logger.info("[" + _monDbName + "] Flushing in-memory counters to persistent storage before we start the transfer. SQL='" + sql + "'.");
			stmnt.executeUpdate(sql);
		}
		catch (SQLException ex)
		{
			_logger.error("[" + _monDbName + "] Problems flushing in-memory counters to persistent storage before we start the transfer. Skipping this and continuing. SQL=|" + sql + "|. MsgNum=" + ex.getErrorCode() + ", SqlState='" + ex.getSQLState() + "', MsgText='" + ex.getMessage() + "'.");
			
			// Msg 12421: User does not have necessary permissions to execute Query Store stored procedure.
			if (ex.getErrorCode() == 12421)
			{
				String monitorUser = _monConn.getConnProp() == null ? "" : _monConn.getConnProp().getUsername();
				_logger.info("If you want the monitor user '" + monitorUser + "' to be able to flush the Query Store do: 'GRANT ALTER ANY DATABASE TO [" + monitorUser + "]'. Or grant EXEC to procedure 'sp_query_store_flush_db' in EACH of the databases.");
			}
		}
	}

//	private void extractTopTablesForDdlStorage()
//	{
//		if ( ! PersistentCounterHandler.hasInstance() )
//		{
//			_logger.info("No Persistent Counter Handler was found. exiting early in extractTopTablesForDdlStorage()");
//			return;
//		}
//
//		String sql = ""
//			    + "SELECT top 20 \n"
//			    + "     DB_NAME() AS dbname \n"
//			    + "    ,q.query_id \n"
//			    + "    ,p.plan_id \n"
//			    + "    ,qt.query_text_id \n"
//			    + "    ,qt.query_sql_text \n"
//			    + "FROM sys.query_store_query_text qt \n"
//			    + "INNER JOIN sys.query_store_query q ON qt.query_text_id = q.query_text_id \n"
//			    + "INNER JOIN sys.query_store_plan  p ON q.query_id       = p.query_id \n"
//			    + "WHERE p.plan_id IN ( \n"
//			    + "    SELECT top 200 rs.plan_id \n"   // note: top 200 since plan_id can exists in many intervals (and we cant do distinct and order on avg_duration)
//			    + "    FROM sys.query_store_runtime_stats rs \n"
////			    + "    WHERE rs.runtime_stats_interval_id = (SELECT max(runtime_stats_interval_id) FROM sys.query_store_runtime_stats_interval) \n"
//			    + "    ORDER BY rs.avg_duration DESC \n"
//			    + ") \n"
//			    + "AND qt.query_sql_text NOT LIKE '%/* SqlServerTune:%' \n"
//			    + "";
//		
//		Set<String> tableSet = new HashSet<>();
//
//		try (Statement stmnt = _pcsConn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
//		{
//			while (rs.next())
//			{
//				String sqlText = rs.getString(5);
//				
//				// Get list of tables in the SQL Text and add them to a SET
//				tableSet.addAll( SqlParserUtils.getTables(sqlText) );
//			}
//		}
//		catch (SQLException ex)
//		{
//			_logger.warn("[" + _monDbName + "] Problems extracting SQL Text for top 'avg_duration' in QueryStore. Simply skipping this.", ex);
//		}
//
////		if ( ! tableSet.isEmpty() )
////		{
////			IObjectLookupInspector objectLookupInspector = new ObjectLookupInspectorSqlServer();
////
////			for (String tableName : tableSet)
////			{
////				ObjectLookupQueueEntry qe = new ObjectLookupQueueEntry(_monDbName, tableName, "QueryStoreExtractor.parsedTables", "", 0);
////
////				// NOTE: This wont work since doObjectInfoLookup(...) will send defendant objects like VIEWS to "itself"
////				//       and since this is done AFTER a YYYY-MM-DD roll-over those entries will end up in the NEW database and not the one we want
////				List<DdlDetails> ddlStoreList = objectLookupInspector.doObjectInfoLookup(_monConn, qe, null); // NOTE: we can't send NULL for PCH
////				if (ddlStoreList != null)
////				{
////					for (DdlDetails ddlDetail : ddlStoreList)
////					{
////						
////					}
////				}
////			}
////		}
//
//		if ( ! tableSet.isEmpty() )
//		{
//			// Send all the tables for DDL Store (it will probably end up in the NEW database since a YYYY-MM-DD roll-over has occurred, but then we will hopefully be able to se them "next day") 
//			for (String tableName : tableSet)
//			{
//				PersistentCounterHandler.getInstance().addDdl(_monDbName, tableName, "QueryStoreExtractor.parsedTables");
//			}
//		}
//	}

	private void createTempTables()
	{
		// Not implemented, temp tables are not used
	}

	private void dropTempTables()
	{
		// Not implemented, temp tables are not used
	}


	private void setPeriodStartAndEndTime()
	throws SQLException
	{
		Timestamp ts = null;

		String schemaName = null;

		// Start/end time for the recording
		String sql = "select min([SessionSampleTime]) \n" + // or if we want both start/end: select min([SessionSampleTime]), max([SessionSampleTime])
		      "from "+PersistWriterBase.getTableName(_monConn, schemaName, PersistWriterBase.SESSION_SAMPLES, null, true) + " \n";

		sql = _pcsConn.quotifySqlString(sql);
		try (Statement stmnt = _pcsConn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
				ts = rs.getTimestamp(1);
		}

		if (ts != null)
		{
			_recordingStartTime = ts;
			
			LocalDateTime recStart = ts.toLocalDateTime();

			LocalDateTime periodStartTime = recStart.truncatedTo( ChronoUnit.DAYS );   // set to: YYYY-MM-DD 00:00:00.000000000
			LocalDateTime periodStopTime  = periodStartTime.plusDays(1).minusNanos(1); // set to: YYYY-MM-DD 23:59:59.999999999

			if (_period > 1)
				periodStartTime = periodStartTime.minusDays( _period - 1 );
			
			_recordingStartTime   = ts;
			_periodStartTimeLocal = Timestamp.valueOf(periodStartTime);
			_periodStopTimeLocal  = Timestamp.valueOf(periodStopTime);
			_periodStartTimeUtc   = Timestamp.valueOf(convertToUtc(periodStartTime));
			_periodStopTimeUtc    = Timestamp.valueOf(convertToUtc(periodStopTime));
			
//System.out.println("------------- setPeriodStartAndEndTime(): _recordingStartTime='" + _recordingStartTime + "', _periodStartTime='" + _periodStartTime + "', _periodStopTime='" + _periodStopTime + "'.");
		}
	}

	public static LocalDateTime convertToUtc(LocalDateTime time) 
	{
	    return time.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
	}

	public int transferTable(QsTables tabName)
	throws SQLException
	{
		// query_store_wait_stats, is only from Version 2017... so get out of here
		if (_monConn.getDbmsVersionNumber() < Ver.ver(2017))
		{
			// Introduced in 2017
			if (QsTables.query_store_wait_stats.equals(tabName))
				return -1;

			// Introduced in 2017
			if (QsTables.database_automatic_tuning_options.equals(tabName))
				return -1;

			// Introduced in 2017
			if (QsTables.dm_db_tuning_recommendations.equals(tabName))
				return -1;
		}

		
		long startTime = System.currentTimeMillis();
		String sql = "";

		// Drop table if it already exists?
		boolean pcsTabExists = DbUtils.checkIfTableExistsNoThrow(_pcsConn, null, _pcsSchemaName, tabName.toString());
		if (pcsTabExists)
		{
			// Should we drop it?
			// Should we truncate it?
			
			// Lets drop the table
			sql = _pcsConn.quotifySqlString("drop table [" + _pcsSchemaName + "].[" + tabName + "]");
			
			try (Statement stmnt = _pcsConn.createStatement())
			{
				stmnt.executeUpdate(sql);
			}
		}

		// Get MetaData for the SOURCE
		ResultSetMetaDataCached sourceRsmdC;
//		sql = _monConn.quotifySqlString("select * from [" + _monDbName + "].sys." + tabName + " where 1=2");
		sql = getSqlForTable(tabName) + " AND 1=2"; // execute ('and 1=2' means more or less "no-exec") get only the ResultSet (this is needed if we append any columns like: schema_name & object_name...
		try (Statement stmnt = _monConn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			sourceRsmdC = ResultSetMetaDataCached.createNormalizedRsmd(rs);
		}
		catch (SQLException ex)
		{
			// Log something about the SQL caused the error.
			_logger.error("[" + _monDbName + "] Problems with SQL=|" + sql + "|. MsgNum=" + ex.getErrorCode() + ", SqlState='" + ex.getSQLState() + "', MsgText='" + ex.getMessage() + "'.");
			
			// Get out of here
			throw ex;
		}
//		Check out SQLW -- bcp if that can be used
		

		String crTabSql = null;
		IDbmsDdlResolver dbmsDdlResolver = _pcsConn.getDbmsDdlResolver();
		ResultSetMetaDataCached targetRsmdC = dbmsDdlResolver.transformToTargetDbms(sourceRsmdC); // note the 'sourceRsmdC' is already normalized (done at the "top")

//		Table sourceTable = Table.create(_monConn, _dbname, "sys", tabName);

		crTabSql = dbmsDdlResolver.ddlTextTable(targetRsmdC, _pcsSchemaName, tabName.toString());
		crTabSql = crTabSql.trim();
		
		// Translate '[' and ']' chars into DBMS Vendor specific Quoted Chars
		crTabSql = _pcsConn.quotifySqlString(crTabSql);
		
		try (Statement stmnt = _pcsConn.createStatement())
		{
			stmnt.executeUpdate(crTabSql);
			
//			_logger.info("[" + _monDbName + "] CREATED Destination[" + _pcsConn + "], schema '" + _schemaName + "', table '" + tabName + "'.");
			_logger.debug("[" + _monDbName + "] " + crTabSql);
//System.out.println("----------------------- Create Table: \n" + crTabSql);
		}
		catch (SQLException ex)
		{
			// Log something about the SQL caused the error.
			_logger.error("[" + _monDbName + "] Problems with Creating table '" + tabName + "', using DDL=|" + crTabSql + "|. MsgNum=" + ex.getErrorCode() + ", SqlState='" + ex.getSQLState() + "', MsgText='" + ex.getMessage() + "'.");
			
			// Get out of here
			throw ex;
		}

		// Build Insert statement
		String columnStr = " (" + StringUtil.toCommaStrQuoted('[', ']', targetRsmdC.getColumnNames()) + ")";
		
		// Build: values(?, ?, ?, ?...)
		String valuesStr = " values(" + StringUtil.removeLastComma(StringUtil.replicate("?, ", targetRsmdC.getColumnNames().size())) + ")";
		
		// Build insert SQL
		String insertSql = "insert into [" + _pcsSchemaName + "].[" + tabName + "]" + columnStr + valuesStr;

		// Translate '[' and ']' chars into DBMS Vendor specific Quoted Chars
		insertSql = _pcsConn.quotifySqlString(insertSql);

		// Create the Prepared Statement
		PreparedStatement pstmt = _pcsConn.prepareStatement(insertSql);

		// Build the SQL Statement that will *fetch* data
		sql = _monConn.quotifySqlString(getSqlForTable(tabName));
		
		if (_logger.isDebugEnabled())
			_logger.debug("[" + _monDbName + "] Issuing SQL for table '" + tabName + "'. \n" + sql);
//System.out.println("Issuing SQL for table '" + tabName + "'. \n" + sql);

		// Execute SQL and Loop the SOURCE ResultSet and: setObject(), addBatch(), executeBatch()
		int totalRowCount = 0;
		try (Statement stmnt = _monConn.createStatement(); ResultSet sourceRs = stmnt.executeQuery(sql))
		{
			// Loop source rows
			while (sourceRs.next())
			{
				totalRowCount++;

				// Loop source columns
				for (int sqlPos=1; sqlPos<sourceRsmdC.getColumnCount()+1; sqlPos++)
				{
					int sourceColJdbcDataType = sourceRsmdC.getColumnType(sqlPos);
					int targetColJdbcDataType = targetRsmdC.getColumnType(sqlPos);

					try
					{
						// GET data
						Object obj = sourceRs.getObject(sqlPos);

						// if source type is "TIMESTAMP WITH TIME ZONE"
						// Then we might have to get it as a string
						if (obj != null && sourceColJdbcDataType == Types.TIMESTAMP_WITH_TIMEZONE)
						{
//if (QsTables.query_store_runtime_stats_interval.equals(tabName))
//	System.out.println("---- query_store_runtime_stats_interval[colPos=" + sqlPos + ", colName='" + sourceRsmdC.getColumnLabel(sqlPos) + "'] val is TIMESTAMP_WITH_TIMEZONE=|" + obj.toString() + "|, objJavaClass='" + obj.getClass().getName() + "'.");

							if (_logger.isDebugEnabled() && QsTables.query_store_runtime_stats_interval.equals(tabName))
								_logger.debug("---- query_store_runtime_stats_interval[colPos=" + sqlPos + ", colName='" + sourceRsmdC.getColumnLabel(sqlPos) + "'] val is TIMESTAMP_WITH_TIMEZONE=|" + obj.toString() + "|, objJavaClass='" + obj.getClass().getName() + "'.");

							obj = obj.toString();
						}
								
						// SET the data or NULL value
						if (obj != null)
							pstmt.setObject(sqlPos, obj, targetColJdbcDataType);
						else
							pstmt.setNull(sqlPos, targetColJdbcDataType);
					}
					catch (SQLException ex)
					{
						String sourceColName = sourceRsmdC.getColumnLabel(sqlPos);
						String destColName   = targetRsmdC.getColumnLabel(sqlPos);

						String msg = "[" + _monDbName + "] ROW: " + totalRowCount + " - Problems setting column c=" + sqlPos + ", sourceName='" + sourceColName + "', destName='" + destColName + "'. Caught: Err=" + ex.getErrorCode() + ", State='" + ex.getSQLState() + "', msg='" + ex.getMessage().trim() + "'.";
						_logger.error(msg);

						// NOTE: Here we THROW (out of method), should we do something "better"
						throw ex;
					}
				} // end: loop source columns

				// Add ROW to batch
				pstmt.addBatch();

			} // end: loop rows from source

		} // end: select from source
		catch (SQLException ex)
		{
			// Log something about the SQL caused the error.
			_logger.error("[" + _monDbName + "] Problems with SQL=|" + sql + "|. MsgNum=" + ex.getErrorCode() + ", SqlState='" + ex.getSQLState() + "', MsgText='" + ex.getMessage() + "'.");
			
			// Get out of here
			throw ex;
		}

		// Send the whole batch to the TARGET
//		int[] batchInsCount = pstmt.executeBatch();
		pstmt.executeBatch();
		pstmt.close();
//System.out.println("batchInsCount.length=" + batchInsCount.length + ": " + StringUtil.toCommaStr(batchInsCount));

		_logger.info("[" + _monDbName + "] --> Transferred " + totalRowCount + " rows from '" + _monDbName + "' into: schema '" + _pcsSchemaName + "', table '" + tabName + "'. This took " + TimeUtils.msDiffNowToTimeStr(startTime) + " (HH:MM:SS.ms)");

		// Possibly create any indexes
		createIndexForTable(tabName);

		// Increment some statistics
		_totalRowsTransferred += totalRowCount;
		_totalTableTransferred++;

		return totalRowCount;
	} // end: method

	private void createIndexForTable(QsTables tabName)
	{
		List<String> ddlList = new ArrayList<>();

		switch (tabName)
		{
		case database_query_store_options:
			break;

		case query_context_settings:
			ddlList.add("create unique index " + tabName + "_pk on [" + _pcsSchemaName + "]." + tabName + "(context_settings_id)"); // PK
			break;

		case query_store_query_text:
			ddlList.add("create unique index " + tabName + "_pk on [" + _pcsSchemaName + "]." + tabName + "(query_text_id)"); // PK
			break;
	
		case query_store_query:
			ddlList.add("create unique index " + tabName + "_pk on [" + _pcsSchemaName + "]." + tabName + "(query_id)"); // PK
			break;
		
		case query_store_plan:
			ddlList.add("create unique index " + tabName + "_pk on [" + _pcsSchemaName + "]." + tabName + "(plan_id)"); // PK
			break;
		
		case query_store_wait_stats:
			// According to ER-Diagram it's PK, but the DOC says it can be 2 rows (persisted & in-memory) so lets make it non-unique
			ddlList.add("create index " + tabName + "_pk on [" + _pcsSchemaName + "]." + tabName + "(wait_stats_id)"); // PK
			break;
		
		case query_store_runtime_stats:
			// According to ER-Diagram it's PK, but the DOC says it can be 2 rows (persisted & in-memory) so lets make it non-unique
			ddlList.add("create index " + tabName + "_pk on [" + _pcsSchemaName + "]." + tabName + "(runtime_stats_id)"); // PK
			break;
		
		case query_store_runtime_stats_interval:
			ddlList.add("create unique index " + tabName + "_pk on [" + _pcsSchemaName + "]." + tabName + "(runtime_stats_interval_id)"); // PK
			break;
		
		case database_automatic_tuning_options:
			// No indexes
			break;
	
		case dm_db_tuning_recommendations:
			// No indexes
			break;

		default:
			throw new RuntimeException("createIndexForTable(): Unhandled Query Store Table Name: " + tabName);
		}

		for (String ddl : ddlList)
		{
			ddl = _pcsConn.quotifySqlString(ddl);
			
			try (Statement stmnt = _pcsConn.createStatement())
			{
				_logger.info("[" + _monDbName + "]     Created index in schema '" + _pcsSchemaName + "' on table '" + tabName + "'. DDL: " + ddl);
				stmnt.executeUpdate(ddl);
			}
			catch (SQLException ex)
			{
				_logger.error("[" + _monDbName + "] Problems creating index on table '" + tabName + "' using DDL '" + ddl + "'. Caught: " + ex);
			}
		}
	}
}


//---------------------------------------------------------------------------------------
//-- Below is SQL for doing the extract using temp tables for the in (...)
//-- part instead of nested sub selects
//-- BUT: When testing it out it produced more logical IO than the nested sub selects
//        Just wanted to save the code so it can be used in the future, or for testing purposes 
//---------------------------------------------------------------------------------------

// \set startDate='2021-01-22 00:00:00'
// \set endDate='2021-01-22 23:59:59.999999999'
// 
// ---------------------------------------------------------------------------------------
// -- Step 1 -- get IDs we want to extract --> into temp tables
// ---------------------------------------------------------------------------------------
// if (object_id('tempdb..#intervals')                            is not null) drop table #intervals 
// if (object_id('tempdb..#plan_ids')                             is not null) drop table #plan_ids 
// if (object_id('tempdb..#query_ids')                            is not null) drop table #query_ids 
// if (object_id('tempdb..#query_text_ids__context_settings_ids') is not null) drop table #query_text_ids__context_settings_ids 
// 
// -- get id's for: RUNTIME_STATS_INTERVAL_ID
// select rsi.runtime_stats_interval_id
// into #intervals
// from sys.query_store_runtime_stats_interval rsi
// where rsi.start_time >= '${startDate}' 
// and rsi.end_time     <= '${endDate}'
// 
// -- get id's for: PLANs
// select distinct rs.plan_id
// into #plan_ids
// from sys.query_store_runtime_stats rs
// where rs.runtime_stats_interval_id in (select t.runtime_stats_interval_id from #intervals t)
// 
// -- get id's for: QUERYs
// select p.query_id
// into #query_ids
// from sys.query_store_plan p
// where p.plan_id in (select t.plan_id from #plan_ids t)
// order by 1
// 
// -- get id's for: QUERY_TEXTs & CONTEXT_SETTINGS
// select q.query_text_id, q.context_settings_id
// into #query_text_ids__context_settings_ids
// from sys.query_store_query q
// where q.query_id in (select t.query_id from #query_ids t)
// go
// ---------------------------------------------------------------------------------------
// -- Step 2 -- get data
// ---------------------------------------------------------------------------------------
// 
// 
// -- get Intervalls
// SELECT rsi.* 
// FROM sys.query_store_runtime_stats_interval rsi
// WHERE rsi.start_time >= '${startDate}' 
//   AND rsi.end_time   <= '${endDate}'
// go
// 
// -- Get RUNTIME_STATS
// SELECT rs.* -- can we add: 'query_id' and 'query_text_id' in here as well
// FROM sys.query_store_runtime_stats rs
// WHERE rs.runtime_stats_interval_id IN (SELECT t.runtime_stats_interval_id FROM #intervals t)
// go
// 
// -- Get WAIT_STATS
// SELECT ws.* -- can we add: 'query_id' and 'query_text_id' in here as well
// FROM sys.query_store_wait_stats ws
// WHERE ws.runtime_stats_interval_id IN (SELECT t.runtime_stats_interval_id FROM #intervals t)
// go
// 
// -- get PLANs
// SELECT p.*
// FROM sys.query_store_plan p
// WHERE p.plan_id IN (SELECT t.plan_id FROM #plan_ids t)
// go
// 
// -- get QUERY
// SELECT q.*
// FROM sys.query_store_query q
// WHERE q.query_id IN (SELECT t.query_id FROM #query_ids t)
// go
// 
// -- get QUERY TEXT
// SELECT qt.* 
// FROM sys.query_store_query_text qt
// WHERE qt.query_text_id IN (SELECT t.query_text_id FROM #query_text_ids__context_settings_ids t)
// go
// 
// -- get QUERY CONTEXT SETTINGS
// SELECT qcs.* 
// FROM sys.query_context_settings qcs
// WHERE qcs.context_settings_id IN (SELECT t.context_settings_id FROM #query_text_ids__context_settings_ids t)
// go
// 
// ---------------------------------------------------------------------------------------
// -- Step 3 -- Drop temp tables
// ---------------------------------------------------------------------------------------
// drop table #intervals
// drop table #plan_ids
// drop table #query_ids
// drop table #query_text_ids__context_settings_ids
// go

