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
package com.asetune.central.lmetrics;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asetune.Version;
import com.asetune.central.pcs.CentralPersistWriterBase;
import com.asetune.central.pcs.CentralPersistWriterBase.Table;
import com.asetune.cm.CountersModel;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.pcs.PersistContainer;
import com.asetune.pcs.PersistWriterBase;
import com.asetune.pcs.PersistWriterJdbc;
import com.asetune.sql.conn.DbxConnection;

public class LocalMetricsPersistWriterJdbc
extends PersistWriterJdbc
{
	private final Logger _logger = LoggerFactory.getLogger(this.getClass().getName());

	private Timestamp _localMetricsSessionStartTime;
	private boolean   _initialized = false;

	/** used in checkSaveGraphProperties() to indicate if we already had inserted a record or not */
	private Set<String> _isGraphPropertiesSaved = new HashSet<>();
	
	
	@Override
	protected boolean isShutdownWithNoWait()
	{
		return false;
	}

	
	/**
	 * Initialize some common tables, that may/will be used from some other modules. For example the CentralPcsReader
	 */
	public void checkAndCreateCommonTables(DbxConnection conn)
	throws Exception
	{
		if (conn == null)
		{
			_logger.error("No database connection to Persistent Storage DB, in checkAndCreateCommonTables().'");
			return;
		}

		_logger.info("Checking/creating common tables for Local Metrics in DbxCentral. ");

		String schemaName = getSchemaName();

		// Check if the Schema Exists, if not create it
		conn.createSchemaIfNotExists(schemaName);
		
		// Check/Create
		checkAndCreateTable(conn, schemaName, PersistWriterJdbc.ALARM_ACTIVE);
		checkAndCreateTable(conn, schemaName, PersistWriterJdbc.ALARM_HISTORY);

		checkAndCreateTable(conn, schemaName, CentralPersistWriterBase.Table.GRAPH_PROPERTIES);
		checkAndCreateTable(conn, null      , CentralPersistWriterBase.Table.CENTRAL_GRAPH_PROFILES); // Just used to check add profile 'DbxCentral'
	}

	public static final String LOCAL_METRICS_SCHEMA_NAME = "DbxcLocalMetrics";

	@Override
	public String getSchemaName()
	{
		return LOCAL_METRICS_SCHEMA_NAME;
	}

	@Override
	public GraphStorageType getGraphStorageType()
	{
		return GraphStorageType.COLUMN_NAME_IS_LABEL;
	}


	//-------------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------------
	//-- Save Local Metrics methods
	//-------------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------------

	public void saveLocalMetricsSample(DbxConnection conn, PersistContainer cont)
	{
//System.out.println("saveLocalMetricsSample(): cont.getCounterObjects().size()="+cont.getCounterObjects().size());
//		DbxConnection conn = _mainConn;

		if (conn == null)
		{
			_logger.error("No database connection to Persistent Storage DB.'");
			return;
		}
		if (isShutdownWithNoWait())
		{
			_logger.info("Save Sample: Discard entry due to 'ShutdownWithNoWait'.");
			return;
		}

		if ( ! _initialized)
		{
			try
			{
				checkAndCreateCommonTables(conn);
				_initialized = true;
			}
			catch (Exception ex)
			{
				_logger.error("Problems initializing module with: checkAndCreateCommonTables(). Can't Continue saving Local Metrics.");
				return;
			}
		}

		
		String schemaName = getSchemaName();

		Timestamp sessionStartTime  = cont.getSessionStartTime();
		Timestamp sessionSampleTime = cont.getMainSampleTime();

		
		// If the 'sessionStartTime' hasn't yet been assigned (or NOT STARTED) 
		if (sessionStartTime == null)
		{
			if (_localMetricsSessionStartTime == null)
				_localMetricsSessionStartTime = sessionSampleTime;

			sessionStartTime = _localMetricsSessionStartTime;
			cont.setSessionStartTime(sessionStartTime);
		}

		try
		{
			// Lock the Connection so that "Other" thread doesn't insert at the same time.
//			_persistLock.lock();

			// CREATE-DDL
			for (CountersModel cm : cont.getCounterObjects())
			{
				// only call saveDdl() the first time...
				if ( ! isDdlCreated(cm) )
				{
					if (saveDdl(conn, schemaName, cm))
					{
						markDdlAsCreated(cm);
					}
				}
			}

			
			// START a transaction
			// This will lower number of IO's to the transaction log
			if (conn.getAutoCommit() == true)
				conn.setAutoCommit(false);

			// Save any alarms
			saveAlarms(conn, schemaName, cont);

			// Save counters
			if ( ! cont.getCounterObjects().isEmpty() )
			{
				//--------------------------------------
				// COUNTERS
				//--------------------------------------
				for (CountersModel cm : cont.getCounterObjects())
				{
//					saveCounterData(conn, schemaName, cm, sessionStartTime, sessionSampleTime);
					saveLocalMetricsCounterData(conn, schemaName, cm, sessionStartTime, sessionSampleTime);
				}
			}
				
			// CLOSE the transaction
			conn.commit();
		}
		catch (SQLException e)
		{
			try 
			{
				if (conn.getAutoCommit() == true)
					conn.rollback();
			}
			catch (SQLException e2) {}

			isSevereProblem(conn, e);
			_logger.warn("Error writing to Persistent Counter Store. getErrorCode()="+e.getErrorCode(), e);
		}
		finally
		{
			try { conn.setAutoCommit(true); }
			catch (SQLException e2) { _logger.error("Problems when setting AutoCommit to true.", e2); }

//			_persistLock.unlock();
		}
	}
	

	/**
	 * Save the counters in the database
	 * 
	 * @param cm
	 * @param sessionStartTime
	 * @param sessionSampleTime
	 */
	private void saveLocalMetricsCounterData(DbxConnection conn, String schemaName, CountersModel cm, Timestamp sessionStartTime, Timestamp sessionSampleTime)
	{
		if (cm == null)
		{
			_logger.debug("saveCounterData: cm == null.");
			return;
		}
//System.out.println("saveLocalMetricsCounterData(): cm='" + cm.getName() + "'.");

//		if (cm instanceof CountersModelAppend) 
//			return;

//		if ( ! cm.hasDiffData() && ( cm.isPersistCountersDiffEnabled() || cm.isPersistCountersRateEnabled() ) )
//		{
//			_logger.info("No diffData is available, skipping writing Counters for name='"+cm.getName()+"'.");
//			return;
//		}

		if (isShutdownWithNoWait())
		{
			_logger.info("SaveCounterData: Discard entry due to 'ShutdownWithNoWait'.");
			return;
		}

		_logger.debug("Persisting Counters for CounterModel='"+cm.getName()+"'.");

		int counterType = 0;
		int absRows     = 0;
		int diffRows    = 0;
		int rateRows    = 0;
		if (cm.hasValidSampleData())
		{
    		if (cm.hasAbsData()  && cm.isPersistCountersAbsEnabled())  {counterType += 1; absRows  = save(conn, schemaName, cm, PersistWriterBase.ABS,  sessionStartTime, sessionSampleTime);}
    		if (cm.hasDiffData() && cm.isPersistCountersDiffEnabled()) {counterType += 2; diffRows = save(conn, schemaName, cm, PersistWriterBase.DIFF, sessionStartTime, sessionSampleTime);}
    		if (cm.hasRateData() && cm.isPersistCountersRateEnabled()) {counterType += 4; rateRows = save(conn, schemaName, cm, PersistWriterBase.RATE, sessionStartTime, sessionSampleTime);}
		}
		
		if (isShutdownWithNoWait())
		{
			_logger.info("SaveCounterData:1: Discard entry due to 'ShutdownWithNoWait'.");
			return;
		}

		int graphCount = 0;
//		if (cm.hasValidSampleData())
		if (cm.hasTrendGraphData()) // do not use hasValidSampleData()... if we do not have any DATA records for a Sample, we might have TrendGraph records 
		{
			Map<String,TrendGraphDataPoint> tgdMap = cm.getTrendGraphData();
			if (tgdMap != null)
			{
				for (Map.Entry<String,TrendGraphDataPoint> entry : tgdMap.entrySet()) 
				{
				//	String              key  = entry.getKey();
					TrendGraphDataPoint tgdp = entry.getValue();

//					saveLocalMetricsGraphData(conn, cm, tgdp, sessionStartTime, sessionSampleTime);
					saveGraphData(conn, getGraphStorageType(), schemaName, cm, tgdp, sessionStartTime, sessionSampleTime);
					
					// Save Graph Properties(label etc), so DbxCentral can read the values
					checkSaveGraphProperties(conn, cm, tgdp, schemaName, sessionStartTime, sessionSampleTime);

					graphCount++;
				}
			}
		}
		
		if (_logger.isDebugEnabled())
			_logger.debug("saveLocalMetricsCounterData(): counterType=" + counterType + ", absRows=" + absRows + ", diffRows=" + diffRows + ", rateRows=" + rateRows + ", graphCount=" + graphCount + ".");
	}

	private void checkSaveGraphProperties(DbxConnection conn, CountersModel cm, TrendGraphDataPoint tgdp, String schemaName, Timestamp sessionStartTime, Timestamp sessionSampleTime)
//	throws SQLException
	{
		String cmName          = cm.getName();
		String graphName       = tgdp.getName();
		String graphLabel      = tgdp.getGraphLabel();
		String graphProps      = tgdp.getGraphProps();
		String graphCategory   = tgdp.getCategory().toString();
		boolean percentGraph   = tgdp.isPercentGraph();
		boolean visibleAtStart = tgdp.isVisibleAtStart();

		if ( ! tgdp.hasData() )
		{
			_logger.debug("The graph '" + tgdp.getName() + "' has NO DATA for this sample time, so write will be skipped. TrendGraphDataPoint=" + tgdp);
			return;
		}

		String sql = "";
		String graphFullName = cmName + "_" + graphName;
		
		String isGraphPropertiesSavedKey = sessionStartTime + "|" + graphFullName;
		if (_isGraphPropertiesSaved.contains(isGraphPropertiesSavedKey))
		{
			if (_logger.isDebugEnabled())
				_logger.debug("Skipping inserting into 'DbxGraphProperties'... the key '" + isGraphPropertiesSavedKey + "' existed in '_isGraphPropertiesSaved'.");
			return;
		}

		int initialOrder = _isGraphPropertiesSaved.size() + 1;
		try
		{
			StringBuilder sb = new StringBuilder();
			sb.append(CentralPersistWriterBase.getTableInsertStr(conn, schemaName, Table.GRAPH_PROPERTIES, null, false));
			sb.append(" values");
			sb.append("( ").append(safeStr(sessionStartTime));
			sb.append(", ").append(safeStr(cmName          ));
			sb.append(", ").append(safeStr(graphName       ));
			sb.append(", ").append(safeStr(graphFullName   )); // cmName_graphName
			sb.append(", ").append(safeStr(graphLabel      ));
			sb.append(", ").append(safeStr(graphProps      ));
			sb.append(", ").append(safeStr(graphCategory   ));
			sb.append(", ").append(percentGraph             );
			sb.append(", ").append(visibleAtStart           );
			sb.append(", ").append(initialOrder             );
			sb.append(")");

			sql = sb.toString();
			
			conn.dbExec(sql, false);
			getStatistics().incInserts();
			
			// Mark this entry as "already inserted"
			_isGraphPropertiesSaved.add(isGraphPropertiesSavedKey);
		}
		catch (SQLException ex)
		{
			_logger.warn("Continuing despite: Problems when executing SQL Statement |" + sql + "|, SqlException: ErrorCode=" + ex.getErrorCode() + ", SQLState=" + ex.getSQLState() + ", toString=" + ex.toString());
		}
	}
	
	/** 
	 * Check if table FROM DBX_CENTRAL... has been created, if not create it.
	 * @param table 
	 * @return True if table was created
	 * @throws SQLException
	 */
	private boolean checkAndCreateTable(DbxConnection conn, String schemaName, Table table)
	throws SQLException
	{
		String fullTabName = CentralPersistWriterBase.getTableName(conn, schemaName, table, null, false);
		String onlyTabName = CentralPersistWriterBase.getTableName(conn, null,       table, null, false);

		String sql = "";
		
		if ( ! isDdlCreated(fullTabName) )
		{
			// Obtain a DatabaseMetaData object from our current connection        
			DatabaseMetaData dbmd = conn.getMetaData();
	
			// dbmd.getColumns() Doesn't work with '\' in the schema name... but if we change '\' to '\\' it seems to work (at least in H2)
			String tmpSchemaName = schemaName;
			if (tmpSchemaName != null && tmpSchemaName.indexOf("\\") != -1)
				tmpSchemaName = tmpSchemaName.replace("\\", "\\\\");
				
			ResultSet rs = dbmd.getColumns(null, tmpSchemaName, onlyTabName, "%");
			boolean tabExists = rs.next();
			rs.close();
	
			if( tabExists )
			{
				// FIXME: Check if all desired columns exists
//				xxx: check how I do this in the other PCS

				// Insert some default values into the table
				if (Table.CENTRAL_GRAPH_PROFILES.equals(table))
				{
					int profileCnt = -1;
					sql = conn.quotifySqlString(""
							+ "select count(*) "
							+ "from [" + fullTabName + "] "
							+ "where [ProfileName] = 'DbxCentral'");
					try (Statement stmnt = conn.createStatement(); ResultSet tmpRs = stmnt.executeQuery(sql))
					{
						while(tmpRs.next())
							profileCnt = tmpRs.getInt(1);
					}
					catch (SQLException ex)
					{
						_logger.warn("Problems checking for profile 'DbxCentral'. Continuing anyway. sql='" + sql + "', Caught: " + ex);
					}
					
					if (profileCnt == 0)
					{
						List<String> list = new ArrayList<>();

						// AseTune
						StringBuffer sbSql = new StringBuffer();
						sbSql.append(CentralPersistWriterBase.getTableInsertStr(conn, schemaName, Table.CENTRAL_GRAPH_PROFILES, null, false));
						sbSql.append(" values(");
						sbSql.append("  '").append("").append("'");                                 // ProductString      '' = default
						sbSql.append(", '").append("").append("'");                                 // UserName           '' = default
						sbSql.append(", '").append("DbxCentral").append("'");                       // ProfileName        '' = default
						sbSql.append(", '").append("DbxCentral Local Host Monitoring").append("'"); // ProfileDescription '' = default
						sbSql.append(", '").append("[");
						sbSql.append(                  "{\"srv\":\"DbxcLocalMetrics\",\"graph\":\"CmOsMpstat_MpSum\"}"            );
						sbSql.append(                 ",{\"srv\":\"DbxcLocalMetrics\",\"graph\":\"CmOsMpstat_MpCpu\"}"            );
						sbSql.append(                 ",{\"srv\":\"DbxcLocalMetrics\",\"graph\":\"CmOsUptime_AdjLoadAverage\"}"   );
						sbSql.append(                 ",{\"srv\":\"DbxcLocalMetrics\",\"graph\":\"CmOsUptime_LoadAverage\"}"      );
						sbSql.append(                 ",{\"srv\":\"DbxcLocalMetrics\",\"graph\":\"CmOsVmstat_SwapInOut\"}"        );
						sbSql.append(                 ",{\"srv\":\"DbxcLocalMetrics\",\"graph\":\"CmOsVmstat_MemUsage\"}"         );
						sbSql.append(                 ",{\"srv\":\"DbxcLocalMetrics\",\"graph\":\"CmOsMeminfo_MemAvailable\"}"    );
						sbSql.append(                 ",{\"srv\":\"DbxcLocalMetrics\",\"graph\":\"CmOsMeminfo_MemUsed\"}"         );
						sbSql.append(                 ",{\"srv\":\"DbxcLocalMetrics\",\"graph\":\"CmOsDiskSpace_FsUsedPct\"}"     );
						sbSql.append(                 ",{\"srv\":\"DbxcLocalMetrics\",\"graph\":\"CmOsDiskSpace_FsAvailableMb\"}" );
						sbSql.append(                 ",{\"srv\":\"DbxcLocalMetrics\",\"graph\":\"CmOsDiskSpace_FsUsedMb\"}"      );
						sbSql.append(                 ",{\"srv\":\"DbxcLocalMetrics\",\"graph\":\"CmOsIostat_IoRWOp\"}"           );
						sbSql.append(                 ",{\"srv\":\"DbxcLocalMetrics\",\"graph\":\"CmOsIostat_IoReadOp\"}"         );
						sbSql.append(                 ",{\"srv\":\"DbxcLocalMetrics\",\"graph\":\"CmOsIostat_IoWriteOp\"}"        );
						sbSql.append(                 ",{\"srv\":\"DbxcLocalMetrics\",\"graph\":\"CmOsNwInfo_AllMbit\"}"          );
						sbSql.append(                 ",{\"srv\":\"DbxcLocalMetrics\",\"graph\":\"CmOsNwInfo_RecvMbit\"}"         );
						sbSql.append(                 ",{\"srv\":\"DbxcLocalMetrics\",\"graph\":\"CmOsNwInfo_TransMbit\"}"        );
						sbSql.append(              "]").append("'");
						sbSql.append(", '").append("gcols=2").append("'");                          // ProfileUrlOptions  '' = default
						sbSql.append(" )");
						sql = sbSql.toString();
						list.add(sbSql.toString());

						// Insert the records
						for (String insertStmnt : list)
						{
							sql = insertStmnt;
							try
							{
								_logger.info("Inserting base data into '" + CentralPersistWriterBase.getTableName(conn, schemaName, Table.CENTRAL_GRAPH_PROFILES, null, false) + "'. Using SQL: " + sql);
								conn.dbExec(sql);
								getStatistics().incInserts();
							}
							catch(SQLException ex)
							{
								_logger.warn("Problems inserting values to '" + CentralPersistWriterBase.getTableName(conn, schemaName, Table.CENTRAL_GRAPH_PROFILES, null, false) + "'. Continuing anyway. sql='" + sql + "', Caught: " + ex);
							}
						}
					} // end: profileCnt == 0
				} // end: CENTRAL_GRAPH_PROFILES
			} // end: tabExists
			else
			{
				_logger.info("Creating table '" + fullTabName + "'.");
				getStatistics().incCreateTables();
				
				// Create the table
				sql = CentralPersistWriterBase.getTableDdlString(conn, schemaName, table, null);
				dbDdlExec(conn, sql);

				// Create indexes
				sql = CentralPersistWriterBase.getIndexDdlString(conn, schemaName, table, null);
				if (sql != null)
				{
					dbDdlExec(conn, sql);
				}
			}
			
			markDdlAsCreated(fullTabName);

			return true;
		}
		return false;
	}
}
