package com.asetune.alarm.writers;

import java.util.List;

import com.asetune.alarm.events.AlarmEvent;
import com.asetune.utils.Configuration;

public abstract class AlarmWriterAbstract
implements IAlarmWriter
{
	public final static String ACTION_RAISE    = "RAISE";
	public final static String ACTION_RE_RAISE = "RE-RAISE";
	public final static String ACTION_CANCEL   = "CANCEL";

	private Configuration _configuration = null;

	@Override
	public void init(Configuration conf) throws Exception
	{
		setConfiguration(conf);
	}
	
	public void setConfiguration(Configuration conf)
	{
		_configuration = conf;
	}

	public Configuration getConfiguration()
	{
		return _configuration;
	}

	@Override
	public void startService()
	{
	}

	@Override
	public void stopService()
	{
	}

	/**
	 * What is this AlarmWriter named to...
	 */
	@Override
	public String getName() 
	{
		return this.getClass().getSimpleName();
	}

	@Override
	public void endOfScan(List<AlarmEvent> activeAlarms)
	{
	}

	@Override
	public void restoredAlarms(List<AlarmEvent> restoredAlarms)
	{
	}
}
