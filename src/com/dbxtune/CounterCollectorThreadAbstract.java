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
package com.dbxtune;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventSrvDown;
import com.dbxtune.alarm.writers.AlarmWriterToPcsJdbc;
import com.dbxtune.alarm.writers.AlarmWriterToPcsJdbc.AlarmEventWrapper;
import com.dbxtune.cm.CmSummaryAbstract;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.pcs.PersistContainer;
import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.pcs.PersistContainer.HeaderInfo;
import com.dbxtune.sql.JdbcUrlParser;
import com.dbxtune.sql.conn.ConnectionProp;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

public abstract class CounterCollectorThreadAbstract
extends Thread
{
	private static Logger _logger = Logger.getLogger(CounterCollectorThreadAbstract.class);

	public static final String PROPERTY_MEMORY_LOW_ON_MEMORY_THRESHOLD_IN_MB = "dbxtune.memory.monitor.threshold.low_on_memory.mb"; 
	public static final int    DEFAULT_MEMORY_LOW_ON_MEMORY_THRESHOLD_IN_MB  = 130; 

	public static final String PROPERTY_MEMORY_OUT_OF_MEMORY_THRESHOLD_IN_MB = "dbxtune.memory.monitor.threshold.out_of_memory.mb";
	public static final int    DEFAULT_MEMORY_OUT_OF_MEMORY_THRESHOLD_IN_MB  = 30; 

	public static final String  PROPKEY_PRINT_CM_REFRESH_TIME                = "dbxtune.print.cm.refresh.time";
//	public static final boolean DEFAULT_PRINT_CM_REFRESH_TIME                = false; 
	public static final boolean DEFAULT_PRINT_CM_REFRESH_TIME                = true; 

	static
	{
		Configuration.registerDefaultValue(PROPERTY_MEMORY_LOW_ON_MEMORY_THRESHOLD_IN_MB, DEFAULT_MEMORY_LOW_ON_MEMORY_THRESHOLD_IN_MB);
		Configuration.registerDefaultValue(PROPERTY_MEMORY_OUT_OF_MEMORY_THRESHOLD_IN_MB, DEFAULT_MEMORY_OUT_OF_MEMORY_THRESHOLD_IN_MB);
		Configuration.registerDefaultValue(PROPKEY_PRINT_CM_REFRESH_TIME                , DEFAULT_PRINT_CM_REFRESH_TIME);
	}

	public static final int MEMORY_LOW_ON_MEMORY_THRESHOLD_IN_MB = Configuration.getCombinedConfiguration().getIntProperty(PROPERTY_MEMORY_LOW_ON_MEMORY_THRESHOLD_IN_MB, DEFAULT_MEMORY_LOW_ON_MEMORY_THRESHOLD_IN_MB);
	public static final int MEMORY_OUT_OF_MEMORY_THRESHOLD_IN_MB = Configuration.getCombinedConfiguration().getIntProperty(PROPERTY_MEMORY_OUT_OF_MEMORY_THRESHOLD_IN_MB, DEFAULT_MEMORY_OUT_OF_MEMORY_THRESHOLD_IN_MB);

	
	
	protected Thread   _thread  = null;
	protected boolean  _running = false;

	protected ICounterController _counterController;

	// Save the last know header info, so it can be "reused" in case we have connect issues and want to send AlarmEvents to DbxCentral
	protected HeaderInfo _lastKnownHeaderInfo;

	public CounterCollectorThreadAbstract(ICounterController counterController)
	{
		_counterController = counterController;
//		_thread = this;
	}
	
	public ICounterController getCounterController()
	{
		return _counterController;
	}

//	public boolean isRunning()
//	{
//		return _running;
//	}
//	public void setRunning(boolean toValue)
//	{
//		_running = toValue;
//	}

	public void shutdown()
	{
		_logger.info("Stopping the collector thread.");
		_running = false;
		if (_thread != null)
			_thread.interrupt();
	}

	public abstract void init(boolean hasGui) throws Exception;

	@Override
	public abstract void run();

	/** NOTE: this SHOULD be called when GetCounters.closeMonConnection() */
	public void cleanupMonConnection()
	{
	}

	public void doInterrupt()
	{
		if (_thread != null)
		{
			_logger.debug("Sending 'interrupt' to the thread '"+_thread.getName()+"', this was done by thread '"+Thread.currentThread().getName()+"'.");
			_thread.interrupt();
		}
	}


	protected AlarmEventSrvDown sendAlarmServerIsDown(String fallbackSrvName, Exception connectException, String connectInfoMsg)
	{
		if (StringUtil.isNullOrBlank(fallbackSrvName))
			fallbackSrvName = "-UNKNOWN-";

		if ( ! AlarmHandler.hasInstance() )
			_logger.warn("No alarm handler installed, so NO alarms will be created for this. sendAlarmServerIsDown(fallbackSrvName='"+fallbackSrvName+"')");

	
		AlarmEventSrvDown alarmEventSrvDown = null;
		String serverName = "";
		String jdbcUrl    = "";
		

		// Send alarm: Server is down
		//  hasDefaultConnProp indicate that we previously had a good connection
		DbxConnection conn = getCounterController().getMonConnection();
		if ( ( conn != null && conn.getConnProp() != null) || DbxConnection.hasDefaultConnProp() )
		{
			ConnectionProp connProp;
			if (conn != null && conn.getConnProp() != null)
				connProp = conn.getConnProp();
			else
				connProp = DbxConnection.getDefaultConnProp();

			if ( AlarmHandler.hasInstance() )
			{
				if (conn != null)
				{
					try
					{
						serverName = conn.getDbmsServerName();							
						jdbcUrl    = conn.getMetaData().getURL();
					}
					catch (Throwable ignore) {}
				}

				if (StringUtil.isNullOrBlank(serverName)) serverName = connProp.getServer();
				if (StringUtil.isNullOrBlank(serverName)) serverName = fallbackSrvName;
				if (StringUtil.isNullOrBlank(jdbcUrl   )) jdbcUrl    = connProp.getUrl();
				
				_logger.info("Sending AlarmEventSrvDown(serverName='"+serverName+"', jdbcUrl='"+jdbcUrl+"') to the AlarmHandler.");

				alarmEventSrvDown = new AlarmEventSrvDown(serverName, jdbcUrl, connectException, connectInfoMsg);
//				AlarmHandler.getInstance().addAlarmToQueue(alarmEventSrvDown);
				AlarmHandler.getInstance().addAlarm(alarmEventSrvDown);

				// Make the AlarmHandler act NOW
				AlarmHandler.getInstance().endOfScan(); // This is synchronous operation
			}
		}
		else
		{
			if ( AlarmHandler.hasInstance() )
			{
				_logger.warn("Can't send detailed AlarmEventSrvDown, instead sending a simplified one sending: new AlarmEventSrvDown(serverName='"+fallbackSrvName+"', jdbcUrl='unknown-url'). Reason: No Monitor Connection or no defaultConnProp. fallbackSrvName='"+fallbackSrvName+"', conn='"+conn+"', conn.getConnProp()='"+(conn==null?"":conn.getConnProp())+"', DbxConnection.hasDefaultConnProp()='"+DbxConnection.hasDefaultConnProp()+"'.");

				serverName = fallbackSrvName;							
				jdbcUrl    = "unknown-url";

				alarmEventSrvDown = new AlarmEventSrvDown(serverName, jdbcUrl, connectException, connectInfoMsg);
//				AlarmHandler.getInstance().addAlarmToQueue(alarmEventSrvDown);
				AlarmHandler.getInstance().addAlarm(alarmEventSrvDown);

				// Make the AlarmHandler act NOW
				AlarmHandler.getInstance().endOfScan(); // This is synchronous operation
			}
		}
		
		// should we send an event to DbxCentral (as a HTTP REST / JSON value)
		// This means a new Persistent Container
		if (alarmEventSrvDown != null && AlarmHandler.hasInstance() && PersistentCounterHandler.hasInstance())
		{
			AlarmHandler ah = AlarmHandler.getInstance();
			List<AlarmEvent> alarmList = ah.getAlarmList();

			Timestamp sessionSampleTime = new Timestamp(System.currentTimeMillis());
			
			HeaderInfo headerInfo = _lastKnownHeaderInfo; // _lastKnownHeaderInfo will be NULL if we havnt made initial connection
			if (headerInfo == null)
			{
				String onHost = jdbcUrl;
				try
				{
					JdbcUrlParser urlParser = JdbcUrlParser.parse(jdbcUrl);
					onHost = urlParser.getHost();

					_logger.info("TRACE-INFO: in sendAlarmServerIsDown(), no old HeaderInfo so we need to create a new HeaderInfo(). Parsing JDBC URL '"+jdbcUrl+"', to get onHostname='" + onHost + "'.");
				}
				catch (Throwable ex)
				{
					_logger.info("Problems parsing JDBC URL '"+jdbcUrl+"', this will be ignored.");
				}

				headerInfo = new HeaderInfo(sessionSampleTime, serverName, onHost, null);
			}
			headerInfo.setMainSampleTime(sessionSampleTime);
			
			// Create a Container
			PersistContainer pc = new PersistContainer(headerInfo);

			// Add the ACTIVE Alarms
			pc.addActiveAlarms(alarmList);

			// Add Alarm events that has happened in this sample. (RASIE/RE-RAISE/CANCEL)
			// Not 100% sure we need this in here, just "keeping" the code "in synch" with how it look at the "normal" send point...
			if ( AlarmWriterToPcsJdbc.hasInstance() )
			{
				List<AlarmEventWrapper> alarmEvents = AlarmWriterToPcsJdbc.getInstance().getList();
				pc.addAlarmEvents(alarmEvents);
				// Note: AlarmWriterToPcsJdbc.getInstance().clear(); is done in PersistContainerHandler after each container entry is handled
			}
			
			PersistentCounterHandler.getInstance().add(pc);
		}

		
		return alarmEventSrvDown;
	}

	/**
	 * Update some graphs that needs to be done AFTER ALL CM's are refreshed<br>
	 * Typically CmSummary -- CmRefreshTime
	 * @param refreshedCms 
	 */
	protected void postRefreshUpdateGraphData(LinkedHashMap<String, CountersModel> refreshedCms)
	{
		CmSummaryAbstract cmSummary = getCounterController().getSummaryCm();
		if (cmSummary != null)
		{
			cmSummary.postAllRefreshUpdateGraphData(refreshedCms);
		}
	}

	/**
	 * Print out how long it took to do refresh of each CM took
	 */
	protected void printCmRefreshTimes(LinkedHashMap<String, CountersModel> refreshedCms)
	{
		boolean printRefreshTime = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_PRINT_CM_REFRESH_TIME, DEFAULT_PRINT_CM_REFRESH_TIME);
		if ( ! printRefreshTime )
			return;

		Collection<CountersModel> cmList = refreshedCms != null ? refreshedCms.values() : getCounterController().getCmList();
		
		long timeLimitInMs = 0;
		
		Map<String, Long> refreshTimeMap = new LinkedHashMap<>();
		String maxCmName = "";
		long   maxCmMs   = 0;
		long   totalMs   = 0;
//		long   cmCount   = 0;

		for (CountersModel cm : cmList)
		{
			if ( cm == null )
				continue;

			if ( ! cm.isActive() )
				continue;
			
			long refreshTimeMs = cm.getSqlRefreshTime() + cm.getLcRefreshTime();
			refreshTimeMap.put(cm.getName(), refreshTimeMs);

			totalMs += refreshTimeMs;
//			cmCount++;

			// Save MAX value
			if (refreshTimeMs > maxCmMs)
			{
				maxCmName = cm.getName();
				maxCmMs   = refreshTimeMs;
			}
		}
		
		// Sort by refreshTime
		Map<String,Long> refreshTimeMapSorted = refreshTimeMap.entrySet().stream()
				.sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
//				.limit(10) // Only the top 10
				.filter(entry -> entry.getValue() > timeLimitInMs)  // Only refreshTime above # ms
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

		// Map to CSV
		String sortedByRefreshTimeStr = StringUtil.toCommaStr(refreshTimeMapSorted);
		
//		_logger.info("RefreshTime: Total=" + totalMs + " ms, CmCount=" + cmCount + ", Max=[" + maxCmName + "=" + maxCmMs + "], sortedByRefreshTime=[" + sortedByRefreshTimeStr + "].   Note: to-disable-this: " + PROPKEY_PRINT_CM_REFRESH_TIME + "=false");
		_logger.info("RefreshTime: Total=" + totalMs + " ms, Max=[" + maxCmName + "=" + maxCmMs + "], sortedByRefreshTime=[" + sortedByRefreshTimeStr + "].   Note: to-disable-this: " + PROPKEY_PRINT_CM_REFRESH_TIME + "=false");
	}

}
