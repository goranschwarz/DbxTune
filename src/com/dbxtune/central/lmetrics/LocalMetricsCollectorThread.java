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
package com.dbxtune.central.lmetrics;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterCollectorThreadAbstract;
import com.dbxtune.ICounterController;
import com.dbxtune.Version;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmEvent.Category;
import com.dbxtune.alarm.events.AlarmEvent.ServiceState;
import com.dbxtune.alarm.events.AlarmEvent.Severity;
import com.dbxtune.alarm.events.AlarmEventDummy;
import com.dbxtune.alarm.writers.AlarmWriterToPcsJdbc;
import com.dbxtune.alarm.writers.AlarmWriterToPcsJdbc.AlarmEventWrapper;
import com.dbxtune.central.pcs.CentralPcsWriterHandler;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.LostConnectionException;
import com.dbxtune.hostmon.HostMonitorConnection;
import com.dbxtune.hostmon.HostMonitorConnectionLocalOsCmd;
import com.dbxtune.pcs.PersistContainer;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.Memory;
import com.dbxtune.utils.TimeUtils;

public class LocalMetricsCollectorThread
extends CounterCollectorThreadAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String THREAD_NAME = "DbxCentralLocalMetrics";
	
	public static final String PROPKEY_sampleTime = "localMetrics.sampleTime";
	public static final int    DEFAULT_sampleTime = 60;

	/** sleep time between samples */
	private int _sleepTime = DEFAULT_sampleTime;
//	private int _sleepTime = 10;

	private boolean _running = true;
	
	
	public LocalMetricsCollectorThread(ICounterController counterController)
	{
		super(counterController);
	}

	@Override
	public void init(boolean hasGui) 
	throws Exception
	{
		// PROPERTY: sampleTime
		_sleepTime = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_sampleTime, DEFAULT_sampleTime);

		// Print Configuration.
		_logger.info("Local Metrics will be sampled every " + _sleepTime + " seconds.       " + PROPKEY_sampleTime + " = " + _sleepTime + ").");
	}

	@Override
	public void shutdown()
	{
		_logger.info("Stopping the DbxCentral Local Metrics collector thread.");
		_running = false;
		if (_thread != null)
			_thread.interrupt();
	}

	@Override
	public void run()
	{
		// Set the Thread name
		_thread = Thread.currentThread();
		_thread.setName(THREAD_NAME);
		
		_running = true;
		
		// If you want to start a new session in the Persistent Storage, just set this to true...
		// This could for instance be used when you connect to a new DBMS Server
		boolean startNewPcsSession = false;

		//---------------------------
		// NOW LOOP
		//---------------------------
		while (_running)
		{
			// notify the heartbeat that we are still running...
			// This is also done right before we go to sleep (waiting for next data collection)
//			HeartbeatMonitor.doHeartbeat();
			
			// HOST Monitoring connection
			if ( ! getCounterController().isHostMonConnected() )
			{
				boolean localHostMon = true;
				if (localHostMon)
				{
					_logger.info( "Host Monitoring: Using Local OS Commands.");

					HostMonitorConnection hostMonConn = new HostMonitorConnectionLocalOsCmd(true); // Create it with status as: connected
					getCounterController().setHostMonConnection(hostMonConn);
				}
			}
				
			// When 10 MB of memory or less, write some info about that.
			Memory.checkMemoryUsage(10);

			// Set the CM's to be in normal refresh state
			for (CountersModel cm : getCounterController().getCmList())
			{
				if (cm == null)
					continue;

				cm.setState(CountersModel.State.NORMAL);
			}

		
			// ????????????????????
			// Check if we are connected or not
			// if not connected, go to sleep and retry
			
			
			// Get some Header information that will be used by the PersistContainer sub system
			PersistContainer.HeaderInfo headerInfo = getCounterController().createPcsHeaderInfo();
			if (headerInfo == null)
			{
				_logger.warn("No 'header information' object could be created... starting at top of the while loop.");
				continue;
			}

		
			// PCS
			PersistContainer pc = new PersistContainer(headerInfo);
			if (startNewPcsSession)
				pc.setStartNewSample(true);
			startNewPcsSession = false;

			// add some statistics on the "main" sample level
			getCounterController().setStatisticsTime(headerInfo.getMainSampleTime());

		
			try
			{
				getCounterController().setInRefresh(true);

				// Initialize the counters, now when we know what 
				// release we are connected to
				if ( ! getCounterController().isInitialized() )
				{
//					DbxConnection xconn = getCounterController().getMonConnection();
					
					getCounterController().initCounters( 
							getCounterController().getMonConnection(), 
							false, 
							-1,    //mtd.getDbmsExecutableVersionNum(), 
							false, //mtd.isClusterEnabled(), 
							-1);   //mtd.getDbmsMonTableVersion());
				}

				// Keep a list of all the CM's that are refreshed during this loop
				// This one will be passed to doPostRefresh()
				LinkedHashMap<String, CountersModel> refreshedCms = new LinkedHashMap<String, CountersModel>();

				// Clear the demand refresh list
				getCounterController().clearCmDemandRefreshList();

				//-----------------
				// LOOP all CounterModels, and get new data, 
				//   if it should be done
				//-----------------
				for (CountersModel cm : getCounterController().getCmList())
				{
//System.out.println("---- before-refresh: cm="+cm.getName()+", cm.isRefreshable()="+cm.isRefreshable()+", isActive="+cm.isActive()+", isPersistCountersEnabled="+cm.isPersistCountersEnabled()+", isCmInDemandRefreshList="+getCounterController().isCmInDemandRefreshList(cm.getName())+", getTimeToNextPostponedRefresh()="+cm.getTimeToNextPostponedRefresh());

					if (cm != null && cm.isRefreshable())
					{
						cm.setServerName(      headerInfo.getServerNameOrAlias()); // or should we just use getServerName()
						cm.setSampleTimeHead(  headerInfo.getMainSampleTime());
						cm.setCounterClearTime(headerInfo.getCounterClearTime());

						try
						{
							cm.setSampleException(null);
							cm.refresh();
//							cm.refresh(getMonConnection());
	
							// Add it to the list of refreshed cm's
							refreshedCms.put(cm.getName(), cm);

							// move this into cm.refresh()
//							cm.setValidSampleData( (cm.getRowCount() > 0) ); 

							// Add the CM to the container, which will 
							// be posted to persister thread later.
							pc.add(cm);
//System.out.println("---- after-refresh: cm="+cm.getName()+", absRows="+cm.getAbsRowCount()+", absCols="+cm.getAbsColumnCount());
						}
						catch (LostConnectionException ex)
						{
							cm.setSampleException(ex);

							// Try to re-connect, otherwise we might "cancel" some ongoing alarms (due to the fact that we do 'end-of-scan' at the end of the loop)
							_logger.info("Try reconnect. When refreshing the data for cm '"+cm.getName()+"', we got 'LostConnectionException'.");
							DbxConnection conn = getCounterController().getMonConnection();
							if (conn != null)
							{
								try
								{
									conn.close();
									conn.reConnect(null);
									_logger.info("Succeeded: reconnect. continuing to refresh data for next CM.");

									// After reconnect, call: onMonConnect() again. 
									getCounterController().onMonConnect(conn);
								}
								catch(Exception reconnectEx)
								{
									_logger.error("Problem when reconnecting. Caught: "+reconnectEx);
								}
							}
							// If we got an exception, go and check if we are still connected
							if ( ! getCounterController().isMonConnected(true, true) ) // forceConnectionCheck=true, closeConnOnFailure=true
							{
								_logger.warn("Breaking check loop, due to 'not-connected' (after trying to re-connect). Next check loop will do new connection. When refreshing the data for cm '"+getName()+"', we Caught an Exception and we are no longer connected to the monitored server.");
								break; // break: LOOP CM's
							}
						}
						catch (Exception ex)
						{
							// log the stack trace for all others than the SQLException
							if (ex instanceof SQLException)
								_logger.warn("Problem when refreshing cm '"+cm.getName()+"'. Caught: " + ex);
							else
								_logger.warn("Problem when refreshing cm '"+cm.getName()+"'. Caught: " + ex, ex);

							cm.setSampleException(ex);

							// move this into cm.refresh()
//							cm.setValidSampleData(false); 
						}
						
						cm.endOfRefresh();

					} // END: isRefreshable

				} // END: LOOP CM's


				//-----------------
				// POST Refresh handling
				//-----------------
				_logger.debug("---- Do POST Refreshing...");
				for (CountersModel cm : getCounterController().getCmList())
				{
					if ( cm == null )
						continue;

					cm.doPostRefresh(refreshedCms);
				}

				// Send any Dummy/debug alarm if a specific file exists
				sendDummyAlarmIfFileExists();

				//-----------------
				// AlarmHandler: end-of-scan: Cancel any alarms that has not been repeated
				//-----------------
				if (AlarmHandler.hasInstance())
				{
					// Get the Alarm Handler
					AlarmHandler ah = AlarmHandler.getInstance();
					
					// Generate a DummyAlarm if the file '/tmp/${SRVNAME}.dummyAlarm.deleteme exists' exists.
					ah.checkSendDummyAlarm(pc.getServerNameOrAlias());

					ah.endOfScan();           // This is synchronous operation (if we want to stuff Alarms in the PersistContainer before it's called/sent)
//					ah.addEndOfScanToQueue(); // This is async operation

					// Add any alarms to the Persist Container.
					pc.addActiveAlarms(ah.getAlarmList());

					// Add Alarm events that has happened in this sample. (RASIE/RE-RAISE/CANCEL)
					if ( AlarmWriterToPcsJdbc.hasInstance() )
					{
						List<AlarmEventWrapper> alarmEvents = AlarmWriterToPcsJdbc.getInstance().getList();
						pc.addAlarmEvents(alarmEvents);
						// Note: AlarmWriterToPcsJdbc.getInstance().clear(); is done in PersistContainerHandler after each container entry is handled
					}
				}

				//-----------------
				// POST the container to the Persistent Counter Handler
				// That thread will store the information in any Storage.
				//-----------------
//System.out.println(">>>>>>>>>>>>>>>>>>> addLocalMetrics(): pc=" + pc);

				CentralPcsWriterHandler.getInstance().addLocalMetrics(pc);

			}
			catch (Throwable t)
			{
				_logger.error(Version.getAppName()+": error in GetCounters loop.", t);

				if (t instanceof OutOfMemoryError)
				{
					_logger.error(Version.getAppName()+": in GetCounters loop, caught 'OutOfMemoryError'. Calling: Memory.fireOutOfMemory(), which hopefully will release some memory.");
					Memory.fireOutOfMemory();
				}
			}
			finally
			{
				getCounterController().setInRefresh(false);
			}

			//-----------------------------
			// Do Java Garbage Collection?
			//-----------------------------
//			boolean noGuiDoJavaGcAfterRefresh = Configuration.getCombinedConfiguration().getBooleanProperty(          PROPKEY_noGuiDoJavaGcAfterRefresh,           DEFAULT_noGuiDoJavaGcAfterRefresh);
//			boolean doJavaGcAfterRefresh      = Configuration.getCombinedConfiguration().getBooleanProperty(MainFrame.PROPKEY_doJavaGcAfterRefresh     , MainFrame.DEFAULT_doJavaGcAfterRefresh);
//			if (noGuiDoJavaGcAfterRefresh || doJavaGcAfterRefresh)
//			{
//				getCounterController().setWaitEvent("Doing Java Garbage Collection.");
//				System.gc();
//			}

			//-----------------------------
			// Sleep
			//-----------------------------
			// If previous CHECK has DEMAND checks, lets sleep for a shorter while
			// This so we can catch data if the CM's are not yet initialized and has 2 samples (has diff data)
			int sleepTime = getCounterController().getCmDemandRefreshSleepTime(_sleepTime, getCounterController().getLastRefreshTimeInMs());

			if (_logger.isDebugEnabled())
			{
				getCounterController().setWaitEvent("next sample period...");
				_logger.debug("Sleeping for " + sleepTime + " seconds. Waiting for " + getCounterController().getWaitEvent() );
			}

			// notify the heartbeat that we are still running...
//			HeartbeatMonitor.doHeartbeat();

			// Sleep / wait for next sample
			waitForNextSample(sleepTime * 1000);

		} // END: while(_running)

		// closing the CM's
		for (CountersModel cm : getCounterController().getCmList())
		{
			_logger.info("DbxCentral Local Metrics - about to stop, closing CM '" + cm.getName() + "'.");
			cm.close();
		}

		_logger.info("Stopped DbxCentral Local Metrics collector thread.");
	}

	private void sendDummyAlarmIfFileExists()
	{
		File testFile = new File( System.getProperty("java.io.tmpdir") + "/LocalMetricsCollectorThread.send.dummy.alarm");
		
		AlarmHandler alarmHandler = AlarmHandler.getInstance();
		if (alarmHandler == null)
			return;

		String serviceInfo = testFile.getName();
		alarmHandler.addUndergoneAlarmDetection(serviceInfo); // Otherwise it wont ever be CANCELLED
		
		if (testFile.exists())
		{
			String       serviceName    = "LocalMetricsCollectorThread";
		//	String       serviceInfo    = testFile.getName();
			Object       extraInfo      = null;
			Category     category       = Category.OTHER;
			Severity     severity       = Severity.INFO;
			ServiceState state          = ServiceState.UP;
			int          timeToLive     = 0;
			Object       data           = "";
			String       description    = "Dummy/Test Alarm";
			String       extendedDesc   = "";
			int          thresholdInSec = 0;
			
			AlarmEventDummy ae = new AlarmEventDummy(serviceName, serviceInfo, extraInfo, category, severity, state, timeToLive, data, description, extendedDesc, thresholdInSec);
		//	AlarmEvent ae = new AlarmEventDbxCollectorNoData(testFile.getName(), 99_999, 999);
			
			alarmHandler.addAlarm(ae);

			_logger.info("SENDING DUMMY ALARM. File='" + testFile + "' existed, which now will be deleted.");
			testFile.delete();
		}
	}

	/**
	 * Sleep for some seconds. <br>
	 * If we find a
	 *  
	 * @param sleepTimeMs 
	 */
	private void waitForNextSample(int sleepTimeMs)
	{
//System.out.println("waitForNextSample(sleepTimeMs=" + sleepTimeMs + ")");
//		String serverName = _lastKnownHeaderInfo.getServerNameOrAlias();
		
		long startTime = System.currentTimeMillis();
//		String interuptSleepFileName = CollectorRefreshController.REFRESH_REQUEST_FILE_NAME_TEMPLATE.replace(CollectorRefreshController.REFRESH_REQUEST_SERVERNAME_TEMPLATE, serverName);
//		File interuptSleepFile = new File(interuptSleepFileName);

		try
		{
			// indicate that we will be sleeping for a while
			getCounterController().setSleeping(true);
			
			do
			{
				sleep(1000);

//				// Check if file exists; then skip sleep
//				if (_logger.isDebugEnabled())
//					_logger.debug("Checking if 'refresh request' file exists. Filename='" + interuptSleepFile + "'.");
//
//				if (interuptSleepFile.exists())
//				{
//					interuptSleepFile.delete();
//					_logger.info("Found a refresh request. Filename='" + interuptSleepFile + "'.");
//					return;
//				}
			}
			while (TimeUtils.msDiffNow(startTime) < sleepTimeMs);
		}
		catch (InterruptedException ignore)
		{
			// ignore
		}
		finally
		{
			// No longer in sleep
			getCounterController().setSleeping(false);
		}
	}
}
