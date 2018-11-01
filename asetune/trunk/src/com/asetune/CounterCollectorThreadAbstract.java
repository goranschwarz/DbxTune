package com.asetune;

import java.sql.Timestamp;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.AlarmEventSrvDown;
import com.asetune.alarm.writers.AlarmWriterToPcsJdbc;
import com.asetune.alarm.writers.AlarmWriterToPcsJdbc.AlarmEventWrapper;
import com.asetune.pcs.PersistContainer;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.pcs.PersistContainer.HeaderInfo;
import com.asetune.sql.JdbcUrlParser;
import com.asetune.sql.conn.ConnectionProp;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public abstract class CounterCollectorThreadAbstract
extends Thread
{
	private static Logger _logger = Logger.getLogger(CounterCollectorThreadAbstract.class);

	public static final String PROPERTY_MEMORY_LOW_ON_MEMORY_THRESHOLD_IN_MB = "asetune.memory.monitor.threshold.low_on_memory.mb"; 
	public static final int    DEFAULT_MEMORY_LOW_ON_MEMORY_THRESHOLD_IN_MB = 130; 

	public static final String PROPERTY_MEMORY_OUT_OF_MEMORY_THRESHOLD_IN_MB = "asetune.memory.monitor.threshold.out_of_memory.mb";
	public static final int    DEFAULT_MEMORY_OUT_OF_MEMORY_THRESHOLD_IN_MB = 30; 

	static
	{
		Configuration.registerDefaultValue(PROPERTY_MEMORY_LOW_ON_MEMORY_THRESHOLD_IN_MB, DEFAULT_MEMORY_LOW_ON_MEMORY_THRESHOLD_IN_MB);
		Configuration.registerDefaultValue(PROPERTY_MEMORY_OUT_OF_MEMORY_THRESHOLD_IN_MB, DEFAULT_MEMORY_OUT_OF_MEMORY_THRESHOLD_IN_MB);
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


	protected AlarmEventSrvDown sendAlarmServerIsDown(String fallbackSrvName)
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

				alarmEventSrvDown = new AlarmEventSrvDown(serverName, jdbcUrl);
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

				alarmEventSrvDown = new AlarmEventSrvDown(serverName, jdbcUrl);
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

}
