/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.utils;

import org.apache.log4j.Logger;

public class HeartbeatMonitor
{
	private static Logger _logger          = Logger.getLogger(HeartbeatMonitor.class);

	//-----------------------------------------------------------------------
	// BEGIN: Memory Handler functionality
	//-----------------------------------------------------------------------
	private static Thread  _checkThread = null;
	private static boolean _running = false;
	private static int     _sleepTimeInSec = 20;

	private static long _lastHeartbeat   = -1;
	private static int  _alarmAfterSec   = 60;
	private static int  _reAlarmAfterSec = 120;
	
	private static boolean _restartIsEnabled = false;
	private static int     _restartAfterSec  = 30*60; // 30 minutes

	//	private static String _lastOkStackDump = ""; // used to save how it looked "a while ago"
//	private static long   _lastOkStackDumpTime = -1;

//	private static String _lastWarnStackDump = "";
	private static long   _lastWarnStackDumpTime = -1;

	public static void doHeartbeat()
	{
		_lastHeartbeat = System.currentTimeMillis();
		_logger.debug("Just received a Heartbeat.");
	}

	/** After X seconds, we should raise an alarm... */ 
	public static void setAlarmTime(int alarm, int reAlarm)
	{
		_alarmAfterSec   = alarm;
		_reAlarmAfterSec = reAlarm;
	}
	/** After X seconds, we should raise an alarm... */ 
	public static void setAlarmTime(int time)
	{
		setAlarmTime(time, time*2);
	}
	/** Get the alarm time in seconds */
	public static int getAlarmTime()
	{
		return _alarmAfterSec;
	}

	/** How often should we check for memory, default is every 3 seconds */ 
	public static void setSleepTime(int seconds)
	{
		_sleepTimeInSec = seconds;
	}
	public static int getSleepTime()
	{
		return _sleepTimeInSec;
	}

	/** After X seconds, we should "signal" restart the system... */ 
	public static void setRestartTime(int time)
	{
		_restartAfterSec = time;
	}

	/** Get the threshold for restart time in seconds */
	public static int getRestartTime()
	{
		return _restartAfterSec;
	}

	/** Check if the Restart is enabled */
	public static boolean isRestartEnabled()
	{
		return _restartIsEnabled;
	}

	/** Set if the Restart is enabled */
	public static void setRestartEnabled(boolean enable)
	{
		_restartIsEnabled = enable;
	}

	/**
	 * If the thread is sleeping, simply interrupt it and let it go to work. 
	 */
	public static void evaluate()
	{
		if (_checkThread != null)
		{
			_logger.info("Received 'evaluate' notification, the Heartbeat Monitor thread will 'now' evaluate the usage.");
			_checkThread.interrupt();
		}
		else
		{
			_logger.warn("Monitor thread, has not been started.");
		}
	}

	/**
	 * isRunning
	 */
	public static boolean isRunning()
	{
		return (_checkThread != null && _running);
	}

	/**
	 * Stop it
	 */
	public static void stop()
	{
		_running = false;
		if (_checkThread != null)
		{
			_checkThread.interrupt();
		}
	}
	/**
	 * Start a memory check thread that will notify any listeners
	 */
	public static void start()
	{
		if ( _checkThread != null )
		{
			_logger.info("There is already a Heartbeat Monitor check thread running, I will NOT start another one.");
			return;
		}
		_checkThread = new Thread()
		{
			@Override
			public void run()
			{
				_logger.info("Starting Heartbeat Monitor checker thread (sleepTimeInSec="+_sleepTimeInSec+", alarmAfterSec="+_alarmAfterSec+", reAlarmAfterSec="+_reAlarmAfterSec+").");

				// Just set a start-beat
				_lastHeartbeat = System.currentTimeMillis();

				while(_running)
				{
					try
					{
						long msSinceLastBeat = System.currentTimeMillis() - _lastHeartbeat;
						if (msSinceLastBeat > _alarmAfterSec*1000)
						{
							long msSinceLastStackDump = System.currentTimeMillis() - _lastWarnStackDumpTime;
							
							if (_lastWarnStackDumpTime < 0 || msSinceLastStackDump > _reAlarmAfterSec*1000)
							{
								String stackDump = JavaUtils.getStackDump(true);
								_logger.warn("No heartbeat has been issued for "+(msSinceLastBeat/1000)+" seconds. Below is a stacktrace of all threads.\n" + stackDump);

							//	_lastWarnStackDump     = stackDump;
								_lastWarnStackDumpTime = System.currentTimeMillis();
							}
						}
						
						// should we restart the system
						if (isRestartEnabled())
						{
    						if (msSinceLastBeat > _restartAfterSec*1000)
    						{
								_logger.warn("No heartbeat has been issued for "+(msSinceLastBeat/1000)+" seconds. Restart After "+_reAlarmAfterSec+" Seconds is enabled and has been reached.");
								_logger.warn("RESTARTING SYSTEM. Exit code will be "+ShutdownHandler.RESTART_EXIT_CODE+". And the outer shellscript that started the system has to start it up again.");
    							
    							System.exit(ShutdownHandler.RESTART_EXIT_CODE);
    						}
						}

						if (_logger.isDebugEnabled())
						{
							_logger.debug("Heartbeat check. SecondsSinceLastBeat="+(msSinceLastBeat/1000)+", _alarmAfterSec="+_alarmAfterSec+", _reAlarmAfterSec="+_reAlarmAfterSec+", _lastWarnStackDumpTime="+_lastWarnStackDumpTime);
						}

						// Sleep
						Thread.sleep(_sleepTimeInSec * 1000);
					}
					catch (InterruptedException ignore)
					{
						_logger.debug("Received 'interrupted', checking if we should continue to run.");
					}
					catch (Throwable t)
					{
						_logger.warn("Heartbeat monitor had issues, but it will continue... Caught: "+t);
					}
				} // end: while(_running)

				_logger.info("Ending Heartbeat Monitor checker thread.");
				_checkThread = null;
			}
		};

		_running = true;
		_checkThread.setName("HeartbeatMonitor");
		_checkThread.setDaemon(true);
		_checkThread.start();
	}

	/**
	 * Stop/shutdown the memory check thread
	 */
	public static void shutdown()
	{
		if ( _checkThread == null )
		{
			_logger.info("The Heartbeat Monitor check thread isn't running, so no need to shut it down.");
			return;
		}

		_logger.info("Sending an 'interrupt' to the Heartbeat Monitor check thread.");
		_checkThread.interrupt();
	}
}
