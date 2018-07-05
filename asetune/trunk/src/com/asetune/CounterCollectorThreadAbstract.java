package com.asetune;

import org.apache.log4j.Logger;

import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.AlarmEventSrvDown;
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


	protected void sendAlarmServerIsDown(String fallbackSrvName)
	{
		if (StringUtil.isNullOrBlank(fallbackSrvName))
			fallbackSrvName = "-UNKNOWN-";

		if ( ! AlarmHandler.hasInstance() )
			_logger.warn("No alarm handler installed, so NO alarms will be created for this. sendAlarmServerIsDown(fallbackSrvName='"+fallbackSrvName+"')");

	
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
				String serverName = "";
				String jdbcUrl    = "";
				
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

				AlarmEvent alarmEvent = new AlarmEventSrvDown(serverName, jdbcUrl);
				AlarmHandler.getInstance().addAlarmToQueue(alarmEvent);

				// Make the AlarmHandler act NOW
				AlarmHandler.getInstance().endOfScan(); // This is synchronous operation
			}
		}
		else
		{
			if ( AlarmHandler.hasInstance() )
			{
				_logger.warn("Can't send detailed AlarmEventSrvDown, instead sending a simplified one sending: new AlarmEventSrvDown(serverName='"+fallbackSrvName+"', jdbcUrl='unknown-url'). Reason: No Monitor Connection or no defaultConnProp. fallbackSrvName='"+fallbackSrvName+"', conn='"+conn+"', conn.getConnProp()='"+(conn==null?"":conn.getConnProp())+"', DbxConnection.hasDefaultConnProp()='"+DbxConnection.hasDefaultConnProp()+"'.");

				AlarmEvent alarmEvent = new AlarmEventSrvDown(fallbackSrvName, "unknown-url");
				AlarmHandler.getInstance().addAlarmToQueue(alarmEvent);

				// Make the AlarmHandler act NOW
				AlarmHandler.getInstance().endOfScan(); // This is synchronous operation
			}
		}
	}

}
