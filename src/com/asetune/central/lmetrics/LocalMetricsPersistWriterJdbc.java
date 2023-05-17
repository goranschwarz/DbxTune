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

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
					graphCount++;
				}
			}
		}
		
		if (_logger.isDebugEnabled())
			_logger.debug("saveLocalMetricsCounterData(): counterType=" + counterType + ", absRows=" + absRows + ", diffRows=" + diffRows + ", rateRows=" + rateRows + ", graphCount=" + graphCount + ".");
	}
}
