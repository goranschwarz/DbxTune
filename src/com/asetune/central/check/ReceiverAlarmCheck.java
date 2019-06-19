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
package com.asetune.central.check;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.dbxc.AlarmEventDbxCollectorNoData;
import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.central.pcs.DbxTuneSample;
import com.asetune.central.pcs.objects.DbxCentralSessions;
import com.asetune.utils.Configuration;

public class ReceiverAlarmCheck
implements Runnable
{
	private static Logger _logger = Logger.getLogger(ReceiverAlarmCheck.class);

	public static final String  PROPKEY_checkSleepTimeInSec  = "ReceiverAlarmCheck.check.sleep.time.inSeconds";
	public static final int     DEFAULT_checkSleepTimeInSec  = 30;

	public static final String PROPKEY_ALARM_INTERVAL_MULTIPLIER = "ReceiverAlarmCheck.alarm.interval.multiplier";
	public static final int    DEFAULT_ALARM_INTERVAL_MULTIPLIER = 40; // multiplier


	//////////////////////////////////////////////
	//// Instance
	//////////////////////////////////////////////
	public static ReceiverAlarmCheck getInstance()
	{
		return _instance;
	}

	public static boolean hasInstance()
	{
		return (_instance != null);
	}

	public static void setInstance(ReceiverAlarmCheck inst)
	{
		_instance = inst;
	}

	/*---------------------------------------------------
	** class members
	**---------------------------------------------------
	*/

	// implements singleton pattern
	private static ReceiverAlarmCheck _instance = null;

	private boolean  _initialized = false;
	private Thread   _thread      = null;
	private boolean  _running     = false;

	private int      _checkSleepTimeInSec = DEFAULT_checkSleepTimeInSec;
	
	/** Configuration we were initialized with */
	private Configuration _conf;

	/** hold an entry for each expected server */
	private Map<String, ReceiverEntry> _receiverInfo;

	
	
	/** just Create NO initialize() */
	public ReceiverAlarmCheck()
	throws Exception
	{
	}

	/** Create AND initialize() */
	public ReceiverAlarmCheck(Configuration props)
	throws Exception
	{
		init(props);
	}

	/** 
	 * Initialize various member of the class
	 * 
	 * @param conf                    Configuration than can be used by the various writers (may be null, then use: Configuration.getCombinedConfiguration() )
	 * 
	 * @throws Exception When there is a problem with the initialization...
	 */
	public void init(Configuration conf)
	throws Exception
	{
		if (conf == null)
			_conf = Configuration.getCombinedConfiguration();
		else
			_conf = conf;
		
		_logger.info("Initializing the ReceiverAlarmCheck functionality.");

		_checkSleepTimeInSec = _conf.getIntProperty(PROPKEY_checkSleepTimeInSec, DEFAULT_checkSleepTimeInSec);
		_logger.info("Monitoring receive data from collectors every " + _checkSleepTimeInSec + " seconds.");
		
		// Get all Servers that SHOULD be active (not disabled)
		refresh();
		
		_initialized = true;
	}

	public void refresh()
	{
		boolean onlyLast  = true;
		CentralPersistReader reader = CentralPersistReader.getInstance();

		_receiverInfo = new HashMap<>();

		try
		{
			for (DbxCentralSessions s : reader.getSessions( onlyLast, -1 ))
			{
				String srvName        = s.getServerName();
				int    sampleInterval = s.getCollectorSampleInterval();
				
				// skip disabled servers (we should not expect disabled servers to send data)
				if (s.getStatus() == DbxCentralSessions.ST_DISABLED)
					continue;

				ReceiverEntry entry = new ReceiverEntry(srvName, sampleInterval);
				_receiverInfo.put(s.getServerName(), entry);
				
				_logger.info("Monitoring that we will receive data from collector '"+srvName+"', AlarmThreshold is " + entry.getAlarmThreshold() + " seconds.");
			}
		}
		catch (SQLException ex)
		{
			_receiverInfo = null; // retry the refresh at a later time
			_logger.warn("Problems getting sessions from DBX Central Database.", ex);
		}
	}

	public Configuration getConfig()
	{
		return _conf;
	}
	
	
	/**
	 * Check and sends Alarm.<br>
	 * This is normally called from "this" class in the run() method, where we do a "end-of-scan" to the AlarmHandler after every check loop<br>
	 * BUT: If we have another check loop, we might want to call it from somewhere else (hence the "end-of-scan" is done else where), then we can call this method
	 */
	public void checkAlarms()
	{
		if (_receiverInfo == null)
		{
			refresh();
		}

		if (_receiverInfo != null)
		{
			AlarmHandler alarmHandler = AlarmHandler.getInstance();

			// Loop all entries and check...
			for (ReceiverEntry entry : _receiverInfo.values())
			{
				if (_logger.isDebugEnabled())
					_logger.debug("CHECKING: srvName='" + entry.getSessionName() + "', AlarmThreshold=" + entry.getAlarmThreshold() + ", ReceiveTimeInSec="+entry.getReceiveTimeInSec());

				if (entry.hasReceiveTimeExpired())
				{
					String srvName        = entry.getSessionName();
					long threshold        = entry.getAlarmThreshold();
					long secSinceLastRecv = entry.getReceiveTimeInSec();
				
					// Post alarm
					if (alarmHandler != null)
					{
						AlarmEvent ae = new AlarmEventDbxCollectorNoData(srvName, secSinceLastRecv, threshold);
						alarmHandler.addAlarm(ae);
					}
					else
					{
						_logger.warn("No Alarm Handler Installed: AlarmEvent-DbxCollectorNoData: srvName='" + srvName + "', SecSinceLastRecv=" + secSinceLastRecv + ", AlarmThreshold=" + threshold);
					}
				}
			}
		}
	}
	
	@Override
	public void run()
	{
		String threadName = _thread.getName();
		_logger.info("Starting a thread for the module '"+threadName+"'.");

		isInitialized();

		_running = true;

		while(isRunning())
		{
			// CHECK
			checkAlarms();
			
			// Send End Of Scan to AlarmHandler
			// NOTE: we might want to move this IF: we send Alarms from more places, like internal checking of OS etc... (which we do not do for the moment)
			if (AlarmHandler.hasInstance())
			{
				AlarmHandler.getInstance().endOfScan();
			}

			try
			{
				// Wait for next check...
				//_logger.info("Thread '"+_thread.getName()+"', SLEEPS...");
				Thread.sleep(_checkSleepTimeInSec * 1000);
			} 
			catch (InterruptedException ex) 
			{
				// simply start all over by checking...
			}
		}

		_logger.info("Thread '"+threadName+"' was stopped.");
	}



	/**
	 * check/maintain alarm functionality<br>
	 * Thats is if we havn't received data from a specific server/instance in a "while", the alarm
	 * 
	 * @param sample
	 */
	public void receivedData(DbxTuneSample sample)
	{
		if (_receiverInfo == null)
			_receiverInfo = new HashMap<>();

		String srvName = sample.getServerName();

		ReceiverEntry entry = _receiverInfo.get(srvName);
		if (entry == null)
			entry = new ReceiverEntry(sample);

		entry.setReceiveTime();
		
		_receiverInfo.put(srvName, entry);

		// Kick off a new check
		//_thread.interrupt();
	}

	
	////////////////////////////////////////////////////////////////////////////////////////////////
	// BEGIN: LOCAL CLASSES
	////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Local class
	 */
	private class ReceiverEntry
	{
		private String _srvName;
		private long   _lastReceiveTime;
		private long   _alarmThreshold;
		
		public ReceiverEntry(String srvName, int sampleInterval)
		{
//			int multiplier = ReceiverAlarmCheck.getInstance().getConfig().getIntProperty(PROPKEY_ALARM_INTERVAL_MULTIPLIER, DEFAULT_ALARM_INTERVAL_MULTIPLIER);
			int multiplier = _conf.getIntProperty(PROPKEY_ALARM_INTERVAL_MULTIPLIER, DEFAULT_ALARM_INTERVAL_MULTIPLIER);
			
			_srvName         = srvName;
			_alarmThreshold  = sampleInterval * multiplier;
			setReceiveTime();
		}
		
		public ReceiverEntry(DbxTuneSample sample)
		{
//			int multiplier = ReceiverAlarmCheck.getInstance().getConfig().getIntProperty(PROPKEY_ALARM_INTERVAL_MULTIPLIER, DEFAULT_ALARM_INTERVAL_MULTIPLIER);
			int multiplier = _conf.getIntProperty(PROPKEY_ALARM_INTERVAL_MULTIPLIER, DEFAULT_ALARM_INTERVAL_MULTIPLIER);
			
			_srvName         = sample.getServerName();
			_alarmThreshold  = sample.getCollectorSampleInterval() * multiplier;
			setReceiveTime();
		}
		
		public String getSessionName()
		{
			return _srvName;
		}

		public long getAlarmThreshold()
		{
			return _alarmThreshold;
		}

		public void setReceiveTime()
		{
			_lastReceiveTime = System.currentTimeMillis();
		}

		public long getReceiveTimeInSec()
		{
			return (System.currentTimeMillis() - _lastReceiveTime) / 1000;
		}

		public boolean hasReceiveTimeExpired()
		{
			long timeDiffSec = getReceiveTimeInSec();
			if ( timeDiffSec > _alarmThreshold)
				return true;
			return false;
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// END: LOCAL CLASSES
	////////////////////////////////////////////////////////////////////////////////////////////////
	







	private void isInitialized()
	{
		if ( ! _initialized )
		{
			throw new RuntimeException("The AlarmHandler module has NOT yet been initialized.");
		}
	}

	/** 
	 * If we want a service thread that does the checking for us 
	 */
	public void start()
	{
//		if (_writerClasses.size() == 0)
//		{
//			_logger.warn("No Alarm Writers has been installed, The service thread will NOT be started and NO alarms will be propagated.");
//			return;
//		}

		isInitialized();

		// Start the Container Persist Thread
		_thread = new Thread(this);
		_thread.setName(this.getClass().getSimpleName());
		_thread.setDaemon(true);
		_thread.start();
	}

	/** 
	 * Stop the service thread
	 */
	public void shutdown()
	{
		_logger.info("Recieved 'stop' request in ReceiverAlarmCheck.");
		_running = false;
		_thread.interrupt();
	}
	
	/** 
	 * Are we running or not 
	 */
	public boolean isRunning()
	{
		return _running;
	}


}
