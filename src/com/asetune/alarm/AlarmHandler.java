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
package com.asetune.alarm;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.asetune.AppDir;
import com.asetune.Version;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.AlarmEvent.Category;
import com.asetune.alarm.events.AlarmEvent.ServiceState;
import com.asetune.alarm.events.AlarmEvent.Severity;
import com.asetune.alarm.events.AlarmEventOsLoadAverageAdjusted;
import com.asetune.alarm.events.AlarmEventSrvDown;
import com.asetune.alarm.events.dbxc.AlarmEventHttpDestinationDown;
import com.asetune.alarm.events.internal.AlarmEvent_EndOfScan;
import com.asetune.alarm.events.internal.AlarmEvent_Stop;
import com.asetune.alarm.writers.AlarmWriterAbstract;
import com.asetune.alarm.writers.AlarmWriterToApplicationLog;
import com.asetune.alarm.writers.AlarmWriterToPcsJdbc;
import com.asetune.alarm.writers.AlarmWriterToTableModel;
import com.asetune.alarm.writers.IAlarmWriter;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.utils.Configuration;
import com.asetune.utils.Logging;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;


/**
 * TODO: make it a service thread with a Queue so that it wont "block" 
 *       the caller while any of the AlarmWriter does it's job
 *       
 * @author qgoschw
 */
public class AlarmHandler 
implements Runnable
{
	private static Logger _logger          = Logger.getLogger(AlarmHandler.class);

	public static final String  PROPKEY_enable                = "AlarmHandler.enable";
	public static final boolean DEFAULT_enable                = true;

	public static final String  PROPKEY_WriterClass           = "AlarmHandler.WriterClass";
	public static final String  DEFAULT_WriterClass           = null; // no default
//	public static final String  DEFAULT_WriterClass           = "com.asetune.alarm.writers.AlarmWriterToStdout";
//	public static final String  DEFAULT_WriterClass           = "com.asetune.alarm.writers.AlarmWriterToStdout, com.asetune.alarm.writers.AlarmWriterToFile";
//	public static final String  DEFAULT_WriterClass           = "com.asetune.alarm_ToBeRemoved.writers.AlarmWriterDummy"; // no default

	public static final String  PROPKEY_persistAlarmsEnabled  = "AlarmHandler.persist.alarms.enabled";
	public static final boolean DEFAULT_persistAlarmsEnabled  = false; // no default

	public static final String  PROPKEY_persistAlarmsFilename = "AlarmHandler.persist.alarms.filename";
	public static final String  DEFAULT_persistAlarmsFilename = AppDir.getAppStoreDir() + File.separator + Version.getAppName() + ".AlarmHandler.jso";

	/*---------------------------------------------------
	** Constants
	**---------------------------------------------------
	*/
//	private static final String STATUS_NORMAL  = "NORMAL";
//	private static final String STATUS_SUSPECT = "SUSPECT";
//	private static final String STATUS_DOWN    = "DOWN";

	/*---------------------------------------------------
	** class members
	**---------------------------------------------------
	*/

	// implements singleton pattern
	private static AlarmHandler _instance = null;

	private boolean  _initialized = false;
	private Thread   _thread      = null;
	private boolean  _running     = false;

	private Object _waitForQueueEndOfScan = new Object();

	private BlockingQueue<AlarmEvent> _alarmQueue = new LinkedBlockingQueue<>();

	/** Configuration we were initialized with */
	private Configuration _conf;

	/** save Active alarms in this file, when the AlarmHandler restarts it will read up old active alarms from here, so they can be canceled later */
	private String _serializedFileName = null;
	private boolean _persistActiveAlarms = true; 
	
	/** a list of AlarmHandlerEntry */
	private AlarmContainer _alarmContActive   = null; // gets created in the init();
	private AlarmContainer _alarmContThisScan = new AlarmContainer();

//	private AlarmContainer _delayedAlarms     = new AlarmContainer();
	private AlarmContainer _delayedAlarmsActive   = new AlarmContainer();
	private AlarmContainer _delayedAlarmsThisScan = new AlarmContainer();
			
	/** a list of installed AlarmWriters */
	private List<IAlarmWriter> _writerClasses = new LinkedList<>();

	/** */
	//private boolean _sendAlarms = true;
	
	/*---------------------------------------------------
	** Constructors
	**---------------------------------------------------
	*/
	public AlarmHandler()
	throws Exception
	{
	}

	public AlarmHandler(Configuration props, boolean createTableModelWriter, boolean createPcsWriter, boolean createToApplicationLog)
	throws Exception
	{
		init(props, createTableModelWriter, createPcsWriter, createToApplicationLog);
	}



	/** a list of AlarmWriters that will be added */
	public void addAlarmWriters(List<String> writerClassNameList)
	throws Exception
	{
		for (String writerClassName : writerClassNameList)
			addAlarmWriter(writerClassName);
	}

	/** a list of AlarmWriters that will be removed */
	public void removeAlarmWriters(List<String> writerClassNameList)
	{
		for (String writerClassName : writerClassNameList)
			removeAlarmWriter(writerClassName);
	}

	/** an AlarmWriters that will be added 
	 * @throws Exception */
	public void addAlarmWriter(String writerClassName) 
	throws Exception
	{
		IAlarmWriter alarmWriterClass;

		// First check if it already has been added or not
		for (IAlarmWriter writer : _writerClasses)
		{
			if (writer.getClass().getName().equals(writerClassName))
			{
				_logger.info("The AlarmWriter class '"+writerClassName+"' is already lodeded. Lets try to remove it, then add it...");
				removeAlarmWriter(writerClassName);
			}
		}
		
		_logger.debug("Instantiating and Initializing AlarmWriterClass='"+writerClassName+"'.");
		try
		{
			Class<?> c = Class.forName( writerClassName );
			alarmWriterClass = (IAlarmWriter) c.newInstance();
			_writerClasses.add( alarmWriterClass );
		}
		catch (ClassCastException e)
		{
			throw new ClassCastException("When trying to load alarmWriter class '"+writerClassName+"'. The alarmWriter do not seem to follow the interface '"+IAlarmWriter.class.getName()+"'");
		}
		catch (ClassNotFoundException e)
		{
			throw new ClassNotFoundException("Tried to load alarmWriter class '"+writerClassName+"'.", e);
		}

		// Now initialize the User Defined AlarmWriter
		alarmWriterClass.init(_conf);
		alarmWriterClass.printFilterConfig();
		alarmWriterClass.printConfig();
		alarmWriterClass.startService();
	}

	/** an AlarmWriters that will be removed */
	public void removeAlarmWriter(String writerClassName)
	{
		for (IAlarmWriter writer : _writerClasses)
		{
			if (writer.getClass().getName().equals(writerClassName))
			{
				_logger.info("Stopping/removing the AlarmWriter class '"+writerClassName+"'.");
				writer.stopService();
				_writerClasses.remove(writer);
			}
		}
	}
		
	/** 
	 * Initialize various member of the class
	 * 
	 * @param conf                    Configuration than can be used by the various writers
	 * @param createTableModelWriter  Create A GUI model, which DbxTune can look at current/historical alarms
	 * @param createPcsWriter         Create Writer which sends, current/historical alarms to the Percistent  Conter Storage
	 * @param createToApplicationLog  Create Writer which sends, current/historical alarms the applications errorlog
	 * 
	 * @throws Exception When there is a problem with the initialization...
	 */
	public void init(Configuration conf, boolean createTableModelWriter, boolean createPcsWriter, boolean createToApplicationLog)
	throws Exception
	{
		_conf = conf; 
		
		_logger.info("Initializing the AlarmHandler functionality.");

		if (createTableModelWriter)
		{
			AlarmWriterToTableModel alarmClass = new AlarmWriterToTableModel();
			alarmClass.init(_conf);
			alarmClass.printFilterConfig();
			alarmClass.printConfig();

			AlarmWriterToTableModel.setInstance(alarmClass);
			_writerClasses.add( alarmClass );
		}
		
		if (createPcsWriter)
		{
			AlarmWriterToPcsJdbc alarmClass = new AlarmWriterToPcsJdbc();
			alarmClass.init(_conf);
			alarmClass.printFilterConfig();
			alarmClass.printConfig();

			AlarmWriterToPcsJdbc.setInstance(alarmClass);
			_writerClasses.add( alarmClass );
		}
		
		//boolean createToApplicationLog = true;
		if (createToApplicationLog)
		{
			AlarmWriterToApplicationLog alarmClass = new AlarmWriterToApplicationLog();
			alarmClass.init(_conf);
			alarmClass.printFilterConfig();
			alarmClass.printConfig();

			_writerClasses.add( alarmClass );
		}
		
		// property: PROPKEY_persistActiveAlarms
		_persistActiveAlarms = _conf.getBooleanProperty(PROPKEY_persistAlarmsEnabled, DEFAULT_persistAlarmsEnabled);

		// property: AlarmHandler.storage
		if (_persistActiveAlarms)
		{
			_serializedFileName = _conf.getProperty(PROPKEY_persistAlarmsFilename, DEFAULT_persistAlarmsFilename);
			if (_serializedFileName == null)
			{
				throw new Exception("The property '"+PROPKEY_persistAlarmsFilename+"' is mandatory for the AlarmHandler module. It should specify a filename where generated AlarmEvents are stored between sessions.");
			}
			_logger.info("The AlarmHandler module is using the file '"+_serializedFileName+"' for storing active alarms between application restart.");

			// LOAD old alarms that was saved...
			try
			{
				_alarmContActive = AlarmContainer.load(_serializedFileName);
			}
			catch (Exception e)
			{
				_logger.warn("Problems loading any saved alarms. The initialization continues anyway.  This means that cancel request cant be sent if the problem doesnt exist anymore, and alarms that has privioulsy been sent will be sent a second time if they still exists. Caught: "+e);			
			}
		}
		else
			_logger.info("The AlarmHandler module does NOT save active alarms. Alarm cancelation between application restarts wont be possible. This can be enabled with the property '"+PROPKEY_persistAlarmsEnabled+"=true' and set the filename with '"+PROPKEY_persistAlarmsFilename+"=/xxx/filename.jso'.");
		
		if (_alarmContActive == null)
		{
			_alarmContActive = new AlarmContainer();
		}

		// NOTE: this could be a comma ',' separated list
		String alarmClasses = _conf.getProperty(PROPKEY_WriterClass, DEFAULT_WriterClass);
		if (alarmClasses == null && _writerClasses.size() == 0)
		{
			throw new Exception("The property '"+PROPKEY_WriterClass+"' is mandatory for the AlarmHandler module. It should contain one or several classes that implemets the IAlarmWriter interface. If you have more than one writer, specify them as a comma separated list.");
		}
		if (alarmClasses != null)
		{
			String[] alarmClassArray =  alarmClasses.split(",");
			for (int i=0; i<alarmClassArray.length; i++)
			{
				alarmClassArray[i] = alarmClassArray[i].trim();
				String alarmClassName = alarmClassArray[i];

				// ADD the WRITER
				addAlarmWriter(alarmClassName);
			}
		}
		if (_writerClasses.size() == 0)
		{
			_logger.warn("No alarm Writers has been installed, alarms will not be raised to any subsystem.");
		}

		_initialized = true;

		// Call all Writers with the: restoredAlarms(List) method
		restoredAlarms();
	}

	public void reloadConfig()
	throws Exception
	{
		reloadConfig(Configuration.getCombinedConfiguration());
	}

	public void reloadConfig(Configuration conf)
	throws Exception
	{
		if (conf == null)
			return;

		// Get property "AlarmHandler.WriterClass" and turn it into a List
		String newConfigWritersStr = conf.getProperty(AlarmHandler.PROPKEY_WriterClass);
		List<String> newConfigWriters = StringUtil.commaStrToList(newConfigWritersStr);

		// Get current alarm classes
		List<String> currentAlarmWritesStrList = getAlarmWritersClassNames();

		// Figgure out what to add/change/remove internally
		List<String> configWritersToAdd    = new ArrayList<>(newConfigWriters);
		List<String> configWritersToChange = new ArrayList<>(newConfigWriters);
		List<String> configWritersToRemove = new ArrayList<>(currentAlarmWritesStrList);

		configWritersToAdd   .removeAll(currentAlarmWritesStrList);
		configWritersToChange.retainAll(currentAlarmWritesStrList);
		configWritersToRemove.removeAll(newConfigWriters);

		_logger.debug("To be added  : " + configWritersToAdd);
		_logger.debug("To be changed: " + configWritersToChange);
		_logger.debug("To be removed: " + configWritersToRemove);
		
		// "changes" will simply be added to the add list and then let the AlarmHander "restart" them
		configWritersToAdd.addAll(configWritersToChange);

		// Remove 
		removeAlarmWriters(configWritersToRemove);

		// Add
		addAlarmWriters(configWritersToAdd);

		// lest just say that it's initialized...
		// And start the thread if that hasn't already been done.
		_initialized = true;
		if ( ! isRunning() )
			start();
	}

	/*---------------------------------------------------
	** Methods
	**---------------------------------------------------
	*/
	
	//////////////////////////////////////////////
	//// Instance
	//////////////////////////////////////////////
	public static AlarmHandler getInstance()
	{
		return _instance;
	}

	public static boolean hasInstance()
	{
		return (_instance != null);
	}

	public static void setInstance(AlarmHandler inst)
	{
		_instance = inst;
	}

	//////////////////////////////////////////////
	//// enable/disable alarm handling...
	//////////////////////////////////////////////
//	public boolean getSendAlarms()                   { return _sendAlarms; }
//	public void    setSendAlarms(boolean sendAlarms) { _sendAlarms = sendAlarms; }
	public boolean getSendAlarms()                   { return _conf.getBooleanProperty(AlarmHandler.PROPKEY_enable, AlarmHandler.DEFAULT_enable); }
	public void    setSendAlarms(boolean sendAlarms) { _conf.setProperty(AlarmHandler.PROPKEY_enable, sendAlarms); }

	public void    enableSendAlarms()    { setSendAlarms(true);      }
	public void    disableSendAlarms()   { setSendAlarms(false);     }
	public boolean isSendAlarmsEnabled() { return getSendAlarms();   }
	public boolean isSendAlarmsDisabled(){ return ! getSendAlarms(); }

	public List<IAlarmWriter> getAlarmWriters() 
	{ 
		List<IAlarmWriter> list = new ArrayList<>();
		for (IAlarmWriter aw : _writerClasses)
		{
			// Skip internal AlarmWriters
			if (aw instanceof AlarmWriterToTableModel || aw instanceof AlarmWriterToPcsJdbc || aw instanceof AlarmWriterToApplicationLog)
				continue;
			
			list.add(aw);
		}
		return list; 
	}

	public List<String> getAlarmWritersClassNames() 
	{
		List<String> list = new ArrayList<>();
		for (IAlarmWriter aw : _writerClasses)
		{
			// Skip internal AlarmWriters
			if (aw instanceof AlarmWriterToTableModel || aw instanceof AlarmWriterToPcsJdbc || aw instanceof AlarmWriterToApplicationLog)
				continue;
			
			list.add(aw.getClass().getName());
		}
		return list; 
	}

	
	//////////////////////////////////////////////
	//// Change listener
	//////////////////////////////////////////////
	private List<ChangeListener> _changeListeners = new ArrayList<>();
	
	public interface ChangeListener
	{
		public void alarmChanges(AlarmHandler alarmHandler, int activeAlarms, List<AlarmEvent> list);
	}

	public void addChangeListener(ChangeListener listener)
	{
		_changeListeners.add(listener);
	}

	public void removeChangeListener(ChangeListener listener)
	{
		_changeListeners.remove(listener);
	}

	public void fireChangeListener()
	{
		for (ChangeListener l : _changeListeners)
		{
			l.alarmChanges(this, _alarmContActive.size(), _alarmContActive.getAlarmList());
		}
	}

	
	//////////////////////////////////////////////
	//// xxx
	//////////////////////////////////////////////
	/**
	 * Add an Alarm, the Alarm will be written to all AlarmWriters before method ends.<br>
	 * If you want it to by async, use addAlarmToQueue() instead
	 * @param alarmEvent
	 */
	public void addAlarm(AlarmEvent alarmEvent)
	{
		raise(alarmEvent);
	}

	private void raise(AlarmEvent alarmEvent)
	{
		addUndergoneAlarmDetection(alarmEvent.getServiceInfo()); // in _serviceInfo the CM.getName() is stored 
		
		isInitialized();
		if (isSendAlarmsDisabled())
		{
			_logger.debug("AlarmHandler.raise() is DISABLED. isSendAlarmsDisabled()=true.");
			return;
		}

		// normal alarm, just pass it on 
		if (alarmEvent.getRaiseDelayInSec() <= 0)
		{
			raiseInternal(alarmEvent);
		}
		// If "delay" is enabled... put it in a special queue (if not already there)
		else
		{
			// if the alarms is already in active state, send it again (for potential re-raise)
			if (_alarmContActive.contains(alarmEvent))
			{
//				System.out.println("raise(): DELAY-but-already-in-active-do-raise(for RE-RAISE-PURPOSE): ae='"+alarmEvent.getAlarmClassAbriviated()+"', xxx="+alarmEvent.getRaiseDelayInSec());
				raiseInternal(alarmEvent);
			}
			else
			{
				// Always add alarms to "this scan"
				// This so we can cancel alarms later on... in the endOfScan()... 
				// in endOfScan() we can intersect alarms that 
				//     - overlaps   = action: increment repeat count
				//     - no-overlap = action: cancel-the-alarm if TimeToLive has expired
				_delayedAlarmsThisScan.add(alarmEvent);

				if ( ! _delayedAlarmsActive.contains(alarmEvent) )
				{
//					System.out.println("raise(): DELAY: ae='"+alarmEvent.getAlarmClassAbriviated()+"', xxx="+alarmEvent.getRaiseDelayInSec());
					_delayedAlarmsActive.add(alarmEvent);
					
					_logger.info("Received an AlarmEvent with deferred/delayed activation. RaiseDelayInSec="+alarmEvent.getRaiseDelayInSec()+" for: "+alarmEvent.getMessage());
				}
				else
				{
//					System.out.println("raise(): DELAY-ALREADY-ADDED: ae='"+alarmEvent.getAlarmClassAbriviated()+"', xxx="+alarmEvent.getRaiseDelayInSec());
				}
			}
		}
		
		// now iterate the queue and check for entries that has passed the "delay" time
		checkAndPostDelayedAlarms();
	}

//	/**
//	 * if the "raise delay" has expired, raise the event (and delete it from the "deleayed queue"
//	 */
//	// NOTE: This will/may throw ConcurrentModificationException due to:  _delayedAlarmsActive.remove(alarmEvent);
//	private void checkAndPostDelayedAlarms()
//	{
//		// now iterate the queue and check for entries that has passed the "delay" time
//		for (AlarmEvent alarmEvent : _delayedAlarmsActive.getAlarmList())
//		{
//			//long timeToExpireMs = (alarmEvent.getRaiseDelayInSec()*1000) - alarmEvent.getCrAgeInMs();
//			//System.out.println("checkAndPostDelayedAlarms: hasRaiseDelayExpired="+alarmEvent.hasRaiseDelayExpired() + ", timeToExpireMs="+timeToExpireMs);
//			
//			if (alarmEvent.hasRaiseDelayExpired())
//			{
//				_delayedAlarmsActive.remove(alarmEvent);
//				raiseInternal(alarmEvent);
//			}
//		}
//	}

//	/**
//	 * if the "raise delay" has expired, raise the event (and delete it from the "deleayed queue"
//	 */
//	// NOTE: This may work... if there are no duplicates in the list... 
//	private void checkAndPostDelayedAlarms()
//	{
//		// now iterate the queue and check for entries that has passed the "delay" time
//		Iterator<AlarmEvent> iter = _delayedAlarmsActive.getAlarmList().iterator();
//		while (iter.hasNext())
//		{
//			AlarmEvent alarmEvent = iter.next();
//
//			if (alarmEvent.hasRaiseDelayExpired())
//			{
//				iter.remove();
//				raiseInternal(alarmEvent);
//			}
//		}
//	}

	/**
	 * if the "raise delay" has expired, raise the event (and delete it from the "deleayed queue"
	 */
	// NOTE: This works... if there ARE duplicates in the list... 
	private void checkAndPostDelayedAlarms()
	{
		List<AlarmEvent> alarmsToBeRemoved = null;

		// now iterate the queue and check for entries that has passed the "delay" time
		for (AlarmEvent alarmEvent : _delayedAlarmsActive.getAlarmList())
		{
			//long timeToExpireMs = (alarmEvent.getRaiseDelayInSec()*1000) - alarmEvent.getCrAgeInMs();
			//System.out.println("checkAndPostDelayedAlarms: hasRaiseDelayExpired="+alarmEvent.hasRaiseDelayExpired() + ", timeToExpireMs="+timeToExpireMs);
			
			if (alarmEvent.hasRaiseDelayExpired())
			{
//				_delayedAlarmsActive.remove(alarmEvent); // <<-- This will/may cause ConcurrentModificationException
				// Add the removed Alarm into another list, and delete them later (otherwise we will/may cause ConcurrentModificationException)
				if (alarmsToBeRemoved == null)
					alarmsToBeRemoved = new ArrayList<AlarmEvent>();
				alarmsToBeRemoved.add(alarmEvent);

				raiseInternal(alarmEvent);
			}
		}
		
		if (alarmsToBeRemoved != null)
		{
			for (AlarmEvent alarmEvent : alarmsToBeRemoved)
				_delayedAlarmsActive.remove(alarmEvent);
		}
	}

	private void raiseInternal(AlarmEvent alarmEvent)
	{
//		isInitialized();
//		if (isSendAlarmsDisabled())
//		{
//			_logger.debug("AlarmHandler.raise() is DISABLED. isSendAlarmsDisabled()=true.");
//			return;
//		}
		
		// Always add alarms to "this scan"
		// This so we can cancel alarms later on... in the endOfScan()... 
		// in endOfScan() we can intersect alarms that 
		//     - overlaps   = action: increment repeat count
		//     - no-overlap = action: cancel-the-alarm if TimeToLive has expired
		_alarmContThisScan.add(alarmEvent); 

		// Check if the alarm already has been raised
		// Then we do NOT need to signal a new alarm... just increase the repeat counter
		if ( _alarmContActive.contains(alarmEvent) )
		{
			_logger.debug("The AlarmEvent has already been raised: " + alarmEvent);

			// Increment the repeat counter in the ACTIVE container
			_alarmContActive.handleReRaise(alarmEvent);
			
			// set the Active Alarm Count
			alarmEvent.setActiveAlarmCount(_alarmContActive.size());

			// if any writers still wants to have the alarm raised... raise the alarms in THAT specific alarm writers
			for (IAlarmWriter aw : _writerClasses)
			{
				if (aw.isCallReRaiseEnabled())
				{
					// CALL THE installed AlarmWriter
					// AND catch all runtime errors that might come
					try 
					{
						if (aw.doAlarm(alarmEvent))
							aw.reRaise(alarmEvent);
					}
					catch (Throwable t)
					{
						_logger.error("The AlarmHandler got runtime error when calling the method raise() in AlarmWriter named '"+aw.getName()+"'. Continuing with next AlarmWriter...", t);
					}
				}
			}

			// Notify that we got changes
			fireChangeListener();

			return;
		}

		// put it in the "Active alarms" Container
		_alarmContActive.add(alarmEvent);

		// set the Active Alarm Count
		alarmEvent.setActiveAlarmCount(_alarmContActive.size());

		// Persist/Save the alarms
		saveAlarms(_alarmContActive, _serializedFileName);

		// raise the alarms in all the alarm writers
		for (IAlarmWriter aw : _writerClasses)
		{
			// CALL THE installed AlarmWriter
			// AND catch all runtime errors that might come
			try 
			{
				if (aw.doAlarm(alarmEvent))
					aw.raise(alarmEvent);
			}
			catch (Throwable t)
			{
				_logger.error("The AlarmHandler got runtime error when calling the method raise() in AlarmWriter named '"+aw.getName()+"'. Continuing with next AlarmWriter...", t);
			}
		}
		
		// Notify that we got changes
		fireChangeListener();
	}

	/**
	 * Called on init/startup
	 */
	public void restoredAlarms()
	{
		// Call restoredAlarms() for all writers
		for (IAlarmWriter aw : _writerClasses)
		{
			_logger.debug("Calling restoredAlarms() event in AlarmWriter named='"+aw.getName()+"'.");

			// CALL THE installed AlarmWriter
			// AND catch all runtime errors that might come
			try 
			{
				aw.restoredAlarms( getAlarmList() );
			}
			catch (Throwable t)
			{
				_logger.error("The AlarmHandler got runtime error when calling the method restoredAlarms() in AlarmWriter named '"+aw.getName()+"'. Continuing with next AlarmWriter...", t);
			}
		}
	}
	
	/**
	 * Add a "message" to the AlarmHandler saying that THIS CM have undergone Alarm detection
	 * So when endOfScan() is called, we can see WHAT CM's we can CANCEL alarms for
	 * endOfScan() should NOT cancel alarms for CM's that we have NOT made any alarms checks for
	 * For example if we would have (several) "timeout" on CM's the TimeToLive wont be trustworthy and then we will CANCEL alarms even if we shouldn't
	 * 
	 * @param name
	 */
	public void addUndergoneAlarmDetection(String name)
	{
		_hasUndergoneAlarmDetection.add(name);
	}
	private Set<String> _hasUndergoneAlarmDetection = new HashSet<>();

	/**
	 * This method should only be executed at the end of a check loop.
	 */
	public void endOfScan()
	{
		// Remove any alarms in the "delayed alarms" when TimeToLive has expired and no "repeat" has been sent
		checkForCancelationsDelayedAlarms();
		
		// Check if there are any delayed alarms that we need to handle/raise
		checkAndPostDelayedAlarms();

		// check for cancels
		checkForCancelations();
		
		// Call endOfScan() for all writers
		for (IAlarmWriter aw : _writerClasses)
		{
			_logger.debug("Sending end-of-scan event to AlarmWriter named='"+aw.getName()+"'.");

			// CALL THE installed AlarmWriter
			// AND catch all runtime errors that might come
			try 
			{
				aw.endOfScan( getAlarmList() );
			}
			catch (Throwable t)
			{
				_logger.error("The AlarmHandler got runtime error when calling the method endOfScan() in AlarmWriter named '"+aw.getName()+"'. Continuing with next AlarmWriter...", t);
			}
		}

		// Clear this for next "end-of-scan"
		_hasUndergoneAlarmDetection.clear();
		
		// Notify that we got changes
		fireChangeListener();
	}
	
	/**
	 * Add a EndOfScan object to the Alarm Handler QUEUE...<br>
	 * This methos should only be executed at the end of a check loop.
	 */
	public void addEndOfScanToQueue()
	{
		addAlarmToQueue(new AlarmEvent_EndOfScan());
	}
	
	public List<AlarmEvent> getAlarmList()
	{
		if (_alarmContActive == null)
			return null;

		return _alarmContActive.getAlarmList();
	}

	/**
	 * 
	 */
	private void checkForCancelationsDelayedAlarms()
	{
		isInitialized();
		if (isSendAlarmsDisabled())
		{
			_logger.debug("AlarmHandler.checkForCancelationsDelayedAlarms() is DISABLED. isSendAlarmsDisabled()=true.");
			return;
		}

		// get all alarms from the "saved/history" which is NOT part of ThisScan  
		List<AlarmEvent> cancelList = _delayedAlarmsActive.getCancelList(_delayedAlarmsThisScan);

		// remove cancellations from the _delayedAlarmsActive
		for (AlarmEvent alarmEvent : cancelList)
		{
			long timeToExpireMs = (alarmEvent.getRaiseDelayInSec()*1000) - alarmEvent.getCrAgeInMs();
			_logger.info("Remove/Cancel AlarmEvent with deferred/delayed activation due to no overlap/repeat at end-of-scan. " + 
					"RaiseDelayInSec="  + alarmEvent.getRaiseDelayInSec() + 
					", crAgeInMs="      + TimeUtils.msToTimeStr( alarmEvent.getCrAgeInMs() ) + 
					", timeToExpireMs=" + TimeUtils.msToTimeStr( timeToExpireMs ) + 
					". AlarmEvent: "    + alarmEvent.getMessage());

			_delayedAlarmsActive.remove(alarmEvent);
		}

		// empty "this scan"
		_delayedAlarmsThisScan = new AlarmContainer();
	}
	
	/**
	 * 
	 */
	private void checkForCancelations()
	{
		isInitialized();
		if (isSendAlarmsDisabled())
		{
			_logger.debug("AlarmHandler.checkForCancelations() is DISABLED. isSendAlarmsDisabled()=true.");
			return;
		}
		_logger.debug("AlarmHandler is Checking for canceled events.");

		// get all alarms from the "saved/history" which is NOT part of ThisScan  
		List<AlarmEvent> cancelList = _alarmContActive.getCancelList(_alarmContThisScan);

		// If the Cancelled Alarm has NOT been marked as "undergone Alarm detection"...
		// then remove it from the cancelList
		ListIterator<AlarmEvent> li = cancelList.listIterator();
		while(li.hasNext())
		{
			AlarmEvent cancelledAlarm = li.next();

			//if (cancelledAlarm.isXxx) // NOTE: Possibly implement a method on the Alarm to check if the Alarm *must* have a "have undergone alarm detection" message
			//                                   But for now, just check the alarm class for AlarmEventSrvDown
			if (cancelledAlarm instanceof AlarmEventSrvDown)
			{
				_logger.info("Keeping Alarm 'SRV-DOWN' in checkForCancelations(), when checking 'hasUndergoneAlarmDetection'. cancelledAlarm: " + cancelledAlarm + ", _hasUndergoneAlarmDetection=" + _hasUndergoneAlarmDetection);
				continue;
			}
			if (cancelledAlarm instanceof AlarmEventHttpDestinationDown)
			{
				_logger.info("Keeping Alarm 'HTTP-DESTINATION-DOWN' in checkForCancelations(), when checking 'hasUndergoneAlarmDetection'. cancelledAlarm: " + cancelledAlarm + ", _hasUndergoneAlarmDetection=" + _hasUndergoneAlarmDetection);
				continue;
			}
			if (cancelledAlarm instanceof AlarmEventOsLoadAverageAdjusted)
			{
				if ("H2WriterStat".equals(cancelledAlarm.getServiceInfo()))
				{
					_logger.info("Keeping Alarm 'OS-LOAD-AVERAGE-ADJUSTED' with serviceInfo='H2WriterStat' in checkForCancelations(), when checking 'hasUndergoneAlarmDetection'. cancelledAlarm: " + cancelledAlarm + ", _hasUndergoneAlarmDetection=" + _hasUndergoneAlarmDetection);
					continue;
				}
			}

			if ( ! _hasUndergoneAlarmDetection.contains( cancelledAlarm.getServiceInfo() ) )
			{
				_logger.info("Removing Alarm '" + cancelledAlarm.getAlarmClassAbriviated() + "' in checkForCancelations(), when checking 'hasUndergoneAlarmDetection'. Possibly a timeout or some other error when the CM sampled data. cancelledAlarm=" + cancelledAlarm + ", _hasUndergoneAlarmDetection=" + _hasUndergoneAlarmDetection);
				li.remove();
			}
		}
		
		// Mark the AlarmEvent as "canceled"
		for (AlarmEvent alarmEvent : cancelList)
		{
			alarmEvent.markCancel();
			
			// set the Active Alarm Count
			alarmEvent.setActiveAlarmCount(_alarmContActive.size() - cancelList.size());
		}

		// cancel the alarms in all the alarm writers
		for (IAlarmWriter aw : _writerClasses)
		{
			for (AlarmEvent alarmEvent : cancelList)
			{
				_logger.debug("Sending cancel event to AlarmWriter named='"+aw.getName()+"', AlarmEvent='"+alarmEvent+"'.");

				// CALL THE installed AlarmWriter
				// AND catch all runtime errors that might come
				try 
				{
					if (aw.doAlarm(alarmEvent))
						aw.cancel(alarmEvent);
				}
				catch (Throwable t)
				{
					_logger.error("The AlarmHandler got runtime error when calling the method cancel() in AlarmWriter named '"+aw.getName()+"'. Continuing with next AlarmWriter...", t);
				}
			}
		}

		// Remove the canceled alarms from the Saved/Active alarms
		for (AlarmEvent ae : cancelList)
		{
			_alarmContActive.remove(ae);
		}

		// Move the last entries pointer to save
//		_alarmContActive = _alarmContThisScan;
//		_logger.debug("AlarmHandler removing expired alarms from the saved/history.");
//		_alarmContActive.removeExpiredAlarms();

		// Save the alarms
		saveAlarms(_alarmContActive, _serializedFileName);
		
		// empty the "last" alarms
		//_alarmContThisScan.clear();
		_alarmContThisScan = new AlarmContainer();
	}
	
	private void saveAlarms(AlarmContainer ac, String filename)
	{
		if ( ! _persistActiveAlarms)
		{
			_logger.debug("AlarmHandler.saveAlarms() is DISABLED. The active alarms wont be saved/persisted between application restarts.");
			return;
		}

		// Save the alarms
		try
		{
			ac.save(filename);
		}
		catch (Exception e)
		{
			_logger.warn("Problems saving alarms. The alarm will still be propageted to all the Alarm Writers. This means that cancel request cant be sent if the problem doesnt exist anymore, and alarms that has privioulsy been sent will be sent a second time if they still exists. Caught: "+e);
		}
	}
	
	private void isInitialized()
	{
		if ( ! _initialized )
		{
			throw new RuntimeException("The AlarmHandler module has NOT yet been initialized.");
		}
	}

	
	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
	//// Threaded Queue imlpementation
	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////

	/** 
	 * wait for the end-of-queue marker has been processed by all writers 
	 * @return -1 if interuptde, otherwise the time it sleept
	 */
	public long waitForQueueEndOfScan(int timeout)
	{
		long startWaitTime = System.currentTimeMillis();
		
		synchronized (_waitForQueueEndOfScan)
		{
			try
			{
				_waitForQueueEndOfScan.wait(timeout);
				return System.currentTimeMillis() - startWaitTime;
			}
			catch (InterruptedException e)
			{
				return -1;
			}
		}
	}

	public void start()
	{
		if (_writerClasses.size() == 0)
		{
			_logger.warn("No Alarm Writers has been installed, The service thread will NOT be started and NO alarms will be propagated.");
			return;
		}

		isInitialized();

		// Start the Container Persist Thread
		_thread = new Thread(this);
		_thread.setName(this.getClass().getSimpleName());
		_thread.setDaemon(true);
		_thread.start();
	}
	
	public void shutdown()
	{
		_logger.info("Recieved 'stop' request in AlarmHandler.");
		
		_running = false;
		_thread.interrupt();
	}
	
	/** Are we running or not */
	public boolean isRunning()
	{
		return _running;
	}

	/**
	 * Add an Alarm to the alarm Queue, which "as soon as possible" will call raise() on all writers from the thread named 'AlamHandler'
	 * @param alarmEvent
	 */
	public void addAlarmToQueue(AlarmEvent alarmEvent)
	{
		if (_writerClasses.size() == 0)
			return;

		if ( ! isRunning() )
		{
			_logger.warn("The Alarm Handler is not running, discarding entry.");
			return;
		}

//		int qsize = _alarmQueue.size();
//		if (qsize > _warnQueueSizeThresh)
//		{
//			long currentConsumeTimeMs    = System.currentTimeMillis() -_currentConsumeStartTime;
//			String currentConsumeTimeStr = "The consumer is currently not active.";
//			if (_currentConsumeStartTime > 0)
//				currentConsumeTimeStr = "The current consumer has been active for " + TimeUtils.msToTimeStr(currentConsumeTimeMs);
//
//			_logger.warn("The persistent queue has "+qsize+" entries. The persistent writer might not keep in pace. "+currentConsumeTimeStr);
//
//			// call each writes to let them know about this.
//			for (IPersistWriter pw : _writerClasses)
//			{
//				pw.storageQueueSizeWarning(qsize, _warnQueueSizeThresh);
//			}
//		}

		_alarmQueue.add(alarmEvent);
		fireQueueSizeChange();
	}

	/** Kicked off when new entries are added */
	protected void fireQueueSizeChange()
	{
//		int pcsQueueSize         = _containerQueue.size();
//		int ddlLookupQueueSize   = _ddlInputQueue .size();
//		int ddlStoreQueueSize    = _ddlStoreQueue .size();
//		int sqlCapStoreQueueSize = _sqlCaptureStoreQueue.size();
//		int sqlCapStoreEntries   = getSqlCaptureStorageEntries();
//
//		for (PcsQueueChange l : _queueChangeListeners)
//			l.pcsStorageQueueChange(pcsQueueSize, ddlLookupQueueSize, ddlStoreQueueSize, sqlCapStoreQueueSize, sqlCapStoreEntries);
	}

	/**
	 * Read from the Container "in" queue, and use all Writers to save DATA 
	 */
	@Override
	public void run()
	{
		String threadName = _thread.getName();
		_logger.info("Starting a thread for the module '"+threadName+"'.");

		isInitialized();

		_running = true;
		long prevConsumeTimeMs = 0;

		while(isRunning())
		{
			//_logger.info("Thread '"+_thread.getName()+"', SLEEPS...");
			//try { Thread.sleep(5 * 1000); }
			//catch (InterruptedException ignore) {}
			
			if (_logger.isDebugEnabled())
				_logger.debug("Thread '"+threadName+"', waiting on queue...");

			try 
			{
				AlarmEvent alarm = _alarmQueue.take();
				fireQueueSizeChange();

				// Make sure the container isn't empty.
				if (alarm == null)                     continue;
//				if (alarm._counterObjects == null)	  continue;
//				if (alarm._counterObjects.size() <= 0) continue;

				// if we are about to STOP the service
				if ( ! isRunning() )
				{
					_logger.info("The service is about to stop, discarding all alarm queue entries.");
					continue;
				}

				// Go and store or consume the in-data/container
				long startTime = System.currentTimeMillis();
				consume(alarm);
				long stopTime = System.currentTimeMillis();

				prevConsumeTimeMs = stopTime-startTime;
				_logger.debug("It took "+prevConsumeTimeMs+" ms to consume the above information (using all writers).");
				
			} 
			catch (InterruptedException ex) 
			{
				_running = false;
			}
		}

		_logger.info("Emptying the queue for module '"+threadName+"', which had "+_alarmQueue.size()+" entries.");
		_alarmQueue.clear();
		fireQueueSizeChange();

		_logger.info("Thread '"+threadName+"' was stopped.");
	}

	/**
	 * consume
	 */
	private void consume(AlarmEvent alarmEvent)
	{
		if (alarmEvent instanceof AlarmEvent_EndOfScan)
		{
			_logger.debug("AlarmHandler.queue.consume: calling: -end-of-scan-");
			endOfScan();
			synchronized (_waitForQueueEndOfScan)
			{
				_waitForQueueEndOfScan.notifyAll();
			}
		}
		else if (alarmEvent instanceof AlarmEvent_Stop)
		{
			_logger.debug("AlarmHandler.queue.consume: calling: -shutdown-");
			shutdown();
		}
		else
		{
			_logger.debug("AlarmHandler.queue.consume: calling: raise() alarmEvent="+alarmEvent);
			raise(alarmEvent);
		}
	}
	

	public static String getDymmyAlarmFileName(String srvName)
	{
		String tmpDir = System.getProperty("java.io.tmpdir", "/tmp/");
		if ( ! (tmpDir.endsWith("/") || tmpDir.endsWith("\\")) )
			tmpDir += File.separatorChar;
		
//		if (StringUtil.isNullOrBlank(srvName))
//			srvName = "SERVER_NAME";
		if (StringUtil.isNullOrBlank(srvName))
			srvName = "${srvName}";
		
		return tmpDir + "DbxTune.dummyAlarm." + srvName + ".deleteme";
	}

	/**
	 * Send dummy alarm if file '/tmp/DbxTune.dummyAlarm.SRVNAME.deleteme' exists
	 */
	public void checkSendDummyAlarm(String srvName)
	{
		if ( ! AlarmHandler.hasInstance() )
			return;

		//---------------------------------------------------------------
		// For ALARM testing: generate a dummy alarm...
		// create the below file...
		//---------------------------------------------------------------
		String tmpFilename = getDymmyAlarmFileName(srvName);
		File probeFile1 = new File(tmpFilename);
		if (probeFile1.exists())
		{
			_logger.info("DUMMY-FORCE-DUMMY-ALARM: found-file('"+probeFile1+"'), Sending alarm 'AlarmEventDummy'...");

			AlarmEvent dummyAlarm = new com.asetune.alarm.events.AlarmEventDummy(srvName, "SomeCmName", "SomeExtraInfo", Category.OTHER, Severity.INFO, ServiceState.UP, -1, 999, "Dummy alarm, just to test if the alarm handler is working", "Extended Description goes here");
			AlarmHandler.getInstance().addAlarm( dummyAlarm );

			_logger.info("DUMMY-FORCE-DUMMY-ALARM: removing file('"+probeFile1+"').");
			try { probeFile1.delete(); }
			catch(Exception ex) { _logger.error("DUMMY-FORCE-DUMMY-ALARM: Problems removing file '"+probeFile1+"'. Caught: "+ex);}
		}
	}



	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
	//// ---- TEST CODE ---- TEST CODE ---- TEST CODE ---- TEST CODE ----
	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
	private static class AlarmEventDummy
	extends AlarmEvent 
	{
		private static final long serialVersionUID = 1L;
		public AlarmEventDummy(String info, int ttl)
		{
			super("dummy-dbmsVendor", "dummy-serviceName", "dummy-serviceInfo-"+info, "dummy-config", AlarmEvent.Category.OTHER, AlarmEvent.Severity.INFO, AlarmEvent.ServiceState.UP, "Dummy description", null); 
			setTimeToLive(ttl);
		}
	}
	public static class TestAlarmWriter 
	extends AlarmWriterAbstract
	implements IAlarmWriter 
	{
		public int _raiseCount = 0;
		public int _reRaiseCount = 0;
		public int _cancelCount = 0;
		public int _activeEventCount = 0;
		public void reset()
		{
			_raiseCount = 0;
			_cancelCount = 0;
			_activeEventCount = 0;
		}
		@Override public String getName()                                     { return "TestAlarmWriter"; }
		@Override public void init(Configuration props) throws Exception      {}
		@Override public void printConfig()                                   {}
		@Override public boolean isCallReRaiseEnabled()                          { return false; }
		@Override public void restoredAlarms(List<AlarmEvent> restoredAlarms) {}
		@Override public String getDescription() { return "Internally used for testing"; }

		@Override
		public void raise(AlarmEvent alarmEvent)
		{
			_raiseCount++;
			_activeEventCount++;
			System.out.println(">>> "+getName()+": -----RAISE-----(a="+_activeEventCount+", r="+_raiseCount+", c="+_cancelCount+"): "+alarmEvent);
		}

		@Override
		public void reRaise(AlarmEvent alarmEvent)
		{
			_reRaiseCount++;
			System.out.println(">>> "+getName()+": -----RE-RAISE-----(a="+_activeEventCount+", r="+_raiseCount+", c="+_cancelCount+"): "+alarmEvent);
		}

		@Override
		public void cancel(AlarmEvent alarmEvent)
		{
			_cancelCount++;
			_activeEventCount--;
			System.out.println(">>> "+getName()+": -----CANCEL-----(a="+_activeEventCount+", r="+_raiseCount+", c="+_cancelCount+"): "+alarmEvent);
		}

		@Override
		public void endOfScan(List<AlarmEvent> activeAlarms)
		{
			System.out.println(">>> "+getName()+": -----END-OF-SCAN-----(a="+_activeEventCount+", r="+_raiseCount+", c="+_cancelCount+").");
		}

		@Override
		public List<CmSettingsHelper> getAvailableSettings()
		{
			return new ArrayList<>();
		}
	}

	private static void sleep(int sleepTime)
	{
		try
		{
			System.out.println("Sleeping "+sleepTime+" ms");
			Thread.sleep(sleepTime);
		}
		catch (InterruptedException ignore)
		{
		}
	}
	private static void addTestAlarm(boolean useQueueImpl, AlarmEvent ae)
	{
		if (useQueueImpl)
		{
			AlarmHandler.getInstance().addAlarmToQueue(ae);
			// sleep a *short* wh�le to let the Queue Thread consume the entry
			try { Thread.sleep(20); }
			catch (InterruptedException ignore) { ignore.printStackTrace(); }
		}
		else
		{
			AlarmHandler.getInstance().addAlarm(ae);
		}
	}
	private static void doEndOfScan(boolean useQueueImpl)
	{
		if (useQueueImpl)
		{
			AlarmHandler.getInstance().addEndOfScanToQueue();
			// sleep a *short* wh�le to let the Queue Thread consume the entry
			try { Thread.sleep(20); }
			catch (InterruptedException ignore) { ignore.printStackTrace(); }
		}
		else
		{
			AlarmHandler.getInstance().endOfScan();
		}
	}
	public static void main(String[] args) 
	{
		Properties log4jProps = new Properties();
//		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		
//		log4jProps.setProperty("log4j.logger.com.asetune.alarm.AlarmContainer", "debug");
		log4jProps.setProperty("log4j.logger.com.asetune.alarm.AlarmHandler",   "debug");

		PropertyConfigurator.configure(log4jProps);

		System.out.println("DUMMY ALARM FILE: " + AlarmHandler.getDymmyAlarmFileName(null));
		
		try 
		{
			Logging.init();

			Configuration config = new Configuration();
			Configuration.setInstance("dummy", config);

			String jsoFile = "c:\\tmp\\dummy.AlarmHandler.jso";
			File jso = new File(jsoFile);
			if (jso.exists())
			{
				System.out.println("#### Removing JSO file: " + jso.getAbsolutePath());
				jso.delete();
			}

			config.setProperty(PROPKEY_persistAlarmsEnabled,  false);
			config.setProperty(PROPKEY_persistAlarmsFilename, jsoFile);
//			config.setProperty(PROPKEY_WriterClass, "com.asetune.alarm.writers.AlarmWriterToStdout");
			config.setProperty(PROPKEY_WriterClass, "com.asetune.alarm.AlarmHandler$TestAlarmWriter");

			boolean useQueueImpl = true;
			System.out.println("#### Test program: Initializing the alarm handler.");
			System.out.println("#### Test program: useQueueImpl="+useQueueImpl);
			AlarmHandler alarmHandler = new AlarmHandler();
			alarmHandler.init(config, false, false, true);

			if (useQueueImpl) 
				alarmHandler.start();
			AlarmHandler.setInstance(alarmHandler);

			TestAlarmWriter testWriter = null;
			for (IAlarmWriter aw : alarmHandler.getAlarmWriters())
			{
				if ("TestAlarmWriter".equals(aw.getName()))
					testWriter = (TestAlarmWriter) aw;
			}

			//-----------------------------
			// Normal duplicate detection
			//-----------------------------
			System.out.println("#### Normal duplicate detection: add FIRST");
			addTestAlarm(useQueueImpl, new AlarmEventDummy("test1", 0));
			System.out.println("#### Normal duplicate detection: add SECOND");
			addTestAlarm(useQueueImpl, new AlarmEventDummy("test1", 0));
			System.out.println("#### Normal duplicate detection: calling -EOS- FIRST time :::: should NOT cancel the event");
			doEndOfScan(useQueueImpl);
			if (testWriter._activeEventCount == 1)
				System.out.println("OK: _activeEventCount is 1");
			else
				throw new RuntimeException("ERROR: _activeEventCount should be 1, it's now: "+testWriter._activeEventCount);

			sleep(1000); // sleep between 2 samples

			System.out.println("#### Normal duplicate detection: calling -EOS- SECOND time :::: should CANCEL the event");
			doEndOfScan(useQueueImpl); // Should CANCEL the above event
			if (testWriter._activeEventCount == 0)
				System.out.println("OK: _activeEventCount is 0");
			else
				throw new RuntimeException("ERROR: _activeEventCount should be 0, it's now: "+testWriter._activeEventCount);

			testWriter.reset();
			System.out.println("###########################################################################");
			System.out.println("###########################################################################");

			//-----------------------------
			// Test with TimeToLive
			//-----------------------------
			System.out.println("#### TimeToLive: add ttl=1500");
			addTestAlarm(useQueueImpl, new AlarmEventDummy("test2", 1500));
			System.out.println("#### TimeToLive: calling -EOS- FIRST time :::: should NOT cancel the event");
			doEndOfScan(useQueueImpl);
			if (testWriter._activeEventCount == 1)
				System.out.println("OK: _activeEventCount is 1");
			else
				throw new RuntimeException("ERROR: _activeEventCount should be 1, it's now: "+testWriter._activeEventCount);

			sleep(1000); // sleep between 2 samples

			System.out.println("#### TimeToLive: calling -EOS- SECOND time :::: should NOT cancel the event, due to TTL");
			doEndOfScan(useQueueImpl); // Should NOT CANCEL the above event
			if (testWriter._activeEventCount == 1)
				System.out.println("OK: _activeEventCount is 1");
			else
				throw new RuntimeException("ERROR: _activeEventCount should be 1, it's now: "+testWriter._activeEventCount);

			sleep(1000); // sleep between 2 samples

			System.out.println("#### TimeToLive: calling -EOS- THIRD time :::: should CANCEL the event");
			doEndOfScan(useQueueImpl); // Should CANCEL the above event
			if (testWriter._activeEventCount == 0)
				System.out.println("OK: _activeEventCount is 0");
			else
				throw new RuntimeException("ERROR: _activeEventCount should be 0, it's now: "+testWriter._activeEventCount);

			if (useQueueImpl)
				addTestAlarm(useQueueImpl, new AlarmEvent_Stop());

			System.out.println("---end---");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

//	public static void main(String[] args) 
//	{
//		//for (int i=0; i<args.length; i++)
//		//{
//		//	System.out.println("args["+i+"] == '"+args[i]+"'.");
//		//}
//
//		if (args.length == 0)
//		{
//			String appname = "alarm_tester"; 
//			System.out.println("");
//			System.out.println("Usage: "+appname+" ConfigFile");
//			System.out.println("");
//			System.exit(1);
//		}
//		String configFile = args[0];
//		
//		
//		
//		try 
//		{
//			Logging.init("alarmTester.", configFile);
//			
//			System.out.println("");
//			System.out.println("");
//			System.out.println("");
//			System.out.println("Setting up the configuration using config file '"+configFile+"'.");
//			Configuration config = new Configuration(configFile);
//			Configuration.setInstance(config);
//
//			System.out.println("Initializing the alarm handler.");
//			AlarmHandler alarmHandler = new AlarmHandler();
//			alarmHandler.init(config);
//			AlarmHandler.setInstance(alarmHandler);
//	
//			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//			boolean loop = true;
//			while(loop)
//			{
//				System.out.print("Type a command [help]: ");
//				String cmd;
//	
//				cmd = br.readLine().trim();
//				
//				if ( cmd.equals("exit") || cmd.equals("quit") || cmd.equals("q") )
//				{
//					loop = false;
//				}
//				else if (cmd.equals("1"))
//				{
//					AlarmHandler.getInstance().raise( new AlarmEventAseDown("ASE1", "ACTIVE", "Cant connect to server 'ASE1'.") );
//				}
//				else if (cmd.equals("2"))
//				{
//					AlarmHandler.getInstance().raise( new AlarmEventRepserverThreadDown("RS1", "REPSERVER", "DSI:ASE1.db1", "Then checking threads in the...") );
//				}
//				else if (cmd.equals("3"))
//				{
//					String eventName   = null;
//					String serviceName = null;
//					String serviceInfo = null;
//					String extraInfo   = null;
//					String desc        = null;
//
//					System.out.print("Type event in the form AlarmEventXXX(serviceName, serviceInfo, [extraInfo,] desc): ");
//					String line = br.readLine().trim();
//					if (line.indexOf("(") < 0)
//					{
//						System.out.println("Cant find any '(' character in there... try again...");
//						continue;
//					}
//					eventName = line.substring( 0, line.indexOf("(") ).trim();
//
//					line = line.substring( line.indexOf("(") + 1 );
//					String[] strA = line.split(",");
//					
//					if (strA.length == 3)
//					{
//						serviceName = strA[0].trim();
//						serviceInfo = strA[1].trim();
//						desc        = strA[2].trim();
//					}
//					else if (strA.length == 4)
//					{
//						serviceName = strA[0].trim();
//						serviceInfo = strA[1].trim();
//						extraInfo   = strA[2].trim();
//						desc        = strA[3].trim();
//					}
//					else
//					{
//						System.out.println("Looks like it's the wrong format, try again...");
//						System.out.println("Found: AlarmEventXXX='"+eventName+"', serviceName='"+serviceName+"', serviceInfo='"+serviceInfo+"', extraInfo='"+extraInfo+"', desc='"+desc+"'.");
//						continue;
//					}
//					if (desc.endsWith(")"))
//						desc = desc.substring(0, desc.length()-1);
//
//					if (false) {}
//					else if ( eventName.equals("AlarmEventAseConfiguration")            ) AlarmHandler.getInstance().raise( new AlarmEventAseConfiguration            (serviceName, serviceInfo, extraInfo, desc) );
//					else if ( eventName.equals("AlarmEventAseDbNotMarkedForRep")        ) AlarmHandler.getInstance().raise( new AlarmEventAseDbNotMarkedForRep        (serviceName, serviceInfo, desc) );
//					else if ( eventName.equals("AlarmEventAseDbNotRecovered")           ) AlarmHandler.getInstance().raise( new AlarmEventAseDbNotRecovered           (serviceName, serviceInfo, desc) );
//					else if ( eventName.equals("AlarmEventAseDown")                     ) AlarmHandler.getInstance().raise( new AlarmEventAseDown                     (serviceName, serviceInfo, desc) );
//					else if ( eventName.equals("AlarmEventAseMaintUserProblem")         ) AlarmHandler.getInstance().raise( new AlarmEventAseMaintUserProblem         (serviceName, serviceInfo, extraInfo, desc) );
//					else if ( eventName.equals("AlarmEventAseRepAgentDown")             ) AlarmHandler.getInstance().raise( new AlarmEventAseRepAgentDown             (serviceName, serviceInfo, desc) );
//					else if ( eventName.equals("AlarmEventAseRepAgentNotConfigured")    ) AlarmHandler.getInstance().raise( new AlarmEventAseRepAgentNotConfigured    (serviceName, serviceInfo, desc) );
//					else if ( eventName.equals("AlarmEventAseRepAgentProblem")          ) AlarmHandler.getInstance().raise( new AlarmEventAseRepAgentProblem          (serviceName, serviceInfo, extraInfo, desc) );
//					else if ( eventName.equals("AlarmEventAseRepAgentStandbyTruncPoint")) AlarmHandler.getInstance().raise( new AlarmEventAseRepAgentStandbyTruncPoint(serviceName, serviceInfo, desc) );
//					else if ( eventName.equals("AlarmEventAseRepAgentTruncPoint")       ) AlarmHandler.getInstance().raise( new AlarmEventAseRepAgentTruncPoint       (serviceName, serviceInfo, desc) );
//					else if ( eventName.equals("AlarmEventAseRepAgentUp")               ) AlarmHandler.getInstance().raise( new AlarmEventAseRepAgentUp               (serviceName, serviceInfo, desc) );
//					else if ( eventName.equals("AlarmEventAseTranLogFull")              ) AlarmHandler.getInstance().raise( new AlarmEventAseTranLogFull              (serviceName, serviceInfo, desc) );
//					else if ( eventName.equals("AlarmEventAseUsersInStandbyDb")         ) AlarmHandler.getInstance().raise( new AlarmEventAseUsersInStandbyDb         (serviceName, serviceInfo, desc) );
//					else if ( eventName.equals("AlarmEventCommunicationTimeout")        ) AlarmHandler.getInstance().raise( new AlarmEventCommunicationTimeout        (serviceName, serviceInfo, desc) );
//					else if ( eventName.equals("AlarmEventDataDestinationAge")          ) AlarmHandler.getInstance().raise( new AlarmEventDataDestinationAge          (serviceName, serviceInfo, desc) );
//					else if ( eventName.equals("AlarmEventDataLatency")                 ) AlarmHandler.getInstance().raise( new AlarmEventDataLatency                 (serviceName, serviceInfo, extraInfo, desc) );
//					else if ( eventName.equals("AlarmEventRepserverDown")               ) AlarmHandler.getInstance().raise( new AlarmEventRepserverDown               (serviceName, serviceInfo, desc) );
//					else if ( eventName.equals("AlarmEventRepserverQueueSizeForDb")     ) AlarmHandler.getInstance().raise( new AlarmEventRepserverQueueSizeForDb     (serviceName, serviceInfo, Integer.parseInt(extraInfo), desc) );
//					else if ( eventName.equals("AlarmEventRepserverSdFull")             ) AlarmHandler.getInstance().raise( new AlarmEventRepserverSdFull             (serviceName, serviceInfo, desc) );
//					else if ( eventName.equals("AlarmEventRepserverSdThreshold")        ) AlarmHandler.getInstance().raise( new AlarmEventRepserverSdThreshold        (serviceName, serviceInfo, extraInfo, desc) );
//					else if ( eventName.equals("AlarmEventRepserverThreadDown")         ) AlarmHandler.getInstance().raise( new AlarmEventRepserverThreadDown         (serviceName, serviceInfo, extraInfo, desc) );
//					else if ( eventName.equals("AlarmEventWrongAppStatus")              ) AlarmHandler.getInstance().raise( new AlarmEventWrongAppStatus              (serviceName, serviceInfo, extraInfo, desc) );
//					else if ( eventName.equals("AlarmEventWsActiveConnNotDefined")      ) AlarmHandler.getInstance().raise( new AlarmEventWsActiveConnNotDefined      (serviceName, serviceInfo, desc) );
//					else if ( eventName.equals("AlarmEventWsActiveServiceOnNodeB")      ) AlarmHandler.getInstance().raise( new AlarmEventWsActiveServiceOnNodeB      (serviceName, serviceInfo, desc) );
//					else if ( eventName.equals("AlarmEventWsGroupOutOfSync")            ) AlarmHandler.getInstance().raise( new AlarmEventWsGroupOutOfSync            (serviceName, serviceInfo, extraInfo, desc) );
//					else if ( eventName.equals("AlarmEventWsInOperationSwitch")         ) AlarmHandler.getInstance().raise( new AlarmEventWsInOperationSwitch         (serviceName, serviceInfo, desc) );
//					else if ( eventName.equals("AlarmEventWsInOperationSynchDb")        ) AlarmHandler.getInstance().raise( new AlarmEventWsInOperationSynchDb        (serviceName, serviceInfo, desc) );
//					else if ( eventName.equals("AlarmEventWsLogicalConnNotDefined")     ) AlarmHandler.getInstance().raise( new AlarmEventWsLogicalConnNotDefined     (serviceName, serviceInfo, desc) );
//					else if ( eventName.equals("AlarmEventWsStandbyConnNotDefined")     ) AlarmHandler.getInstance().raise( new AlarmEventWsStandbyConnNotDefined     (serviceName, serviceInfo, desc) );
//					else if ( eventName.equals("AlarmEventWsSwitchPostOperation")       ) AlarmHandler.getInstance().raise( new AlarmEventWsSwitchPostOperation       (serviceName, serviceInfo, desc) );
//					else if ( eventName.equals("AlarmEventWsSyncDbPostOperation")       ) AlarmHandler.getInstance().raise( new AlarmEventWsSyncDbPostOperation       (serviceName, serviceInfo, desc) );
//					else
//					{
//						System.out.print("Unknown event '"+eventName+"'. try agin...");
//						continue;
//					}
//				}
//				else if (cmd.equals("eos"))
//				{
//					AlarmHandler.getInstance().endOfScan();
//				}
//				else
//				{
//					System.out.println("Available commands:");
//					System.out.println("    '1'    - Generate a dummy alarm: AlarmEventAseDown(\"ASE1\", \"ACTIVE\", \"Cant connect to server 'ASE1'.\")");
//					System.out.println("    '2'    - Generate AlarmEventRepserverThreadDown(\"RS1\", \"REPSERVER\", \"DSI:ASE1.db1\", \"Then checking threads in the...\")");
//					System.out.println("    '3'    - Generate an event for one of the following types");
//					System.out.println("              AlarmEventAseConfiguration            (serviceName, serviceInfo, configName, description)");
//					System.out.println("              AlarmEventAseDbNotMarkedForRep        (serviceName, serviceInfo, description)");
//					System.out.println("              AlarmEventAseDbNotRecovered           (serviceName, serviceInfo, description)");
//					System.out.println("              AlarmEventAseDown                     (serviceName, serviceInfo, description)");
//					System.out.println("              AlarmEventAseMaintUserProblem         (serviceName, serviceInfo, problemId, description)");
//					System.out.println("              AlarmEventAseRepAgentDown             (serviceName, serviceInfo, description)");
//					System.out.println("              AlarmEventAseRepAgentNotConfigured    (serviceName, serviceInfo, description)");
//					System.out.println("              AlarmEventAseRepAgentProblem          (serviceName, serviceInfo, problemId, description)");
//					System.out.println("              AlarmEventAseRepAgentStandbyTruncPoint(serviceName, serviceInfo, description)");
//					System.out.println("              AlarmEventAseRepAgentTruncPoint       (serviceName, serviceInfo, description)");
//					System.out.println("              AlarmEventAseRepAgentUp               (serviceName, serviceInfo, description)");
//					System.out.println("              AlarmEventAseTranLogFull              (serviceName, serviceInfo, description)");
//					System.out.println("              AlarmEventAseUsersInStandbyDb         (serviceName, serviceInfo, description)");
//					System.out.println("              AlarmEventCommunicationTimeout        (serviceName, serviceInfo, description)");
//					System.out.println("              AlarmEventDataDestinationAge          (serviceName, serviceInfo, description)");
//					System.out.println("              AlarmEventDataLatency                 (serviceName, serviceInfo, originSrvDb, description)");
//					System.out.println("              AlarmEventRepserverDown               (serviceName, serviceInfo, description)");
//					System.out.println("              AlarmEventRepserverQueueSizeForDb     (serviceName, serviceInfo, queueSizeThresholdInMb, description)");
//					System.out.println("              AlarmEventRepserverSdFull             (serviceName, serviceInfo, description)");
//					System.out.println("              AlarmEventRepserverSdThreshold        (serviceName, serviceInfo, thresholdId, description)");
//					System.out.println("              AlarmEventRepserverThreadDown         (serviceName, serviceInfo, threadName, description)");
//					System.out.println("              AlarmEventWrongAppStatus              (serviceName, serviceInfo, inAppStatus, description)");
//					System.out.println("              AlarmEventWsActiveConnNotDefined      (serviceName, serviceInfo, description)");
//					System.out.println("              AlarmEventWsActiveServiceOnNodeB      (serviceName, serviceInfo, description)");
//					System.out.println("              AlarmEventWsGroupOutOfSync            (serviceName, serviceInfo, status, description)");
//					System.out.println("              AlarmEventWsInOperationSwitch         (serviceName, serviceInfo, description)");
//					System.out.println("              AlarmEventWsInOperationSynchDb        (serviceName, serviceInfo, description)");
//					System.out.println("              AlarmEventWsLogicalConnNotDefined     (serviceName, serviceInfo, description)");
//					System.out.println("              AlarmEventWsStandbyConnNotDefined     (serviceName, serviceInfo, description)");
//					System.out.println("              AlarmEventWsSwitchPostOperation       (serviceName, serviceInfo, description)");
//					System.out.println("              AlarmEventWsSyncDbPostOperation       (serviceName, serviceInfo, description)");
//					System.out.println("    'eos'  - \"end-of-scan\" Simulate a \"I have now checked all components for problem\".");
//					System.out.println("    'help' - This text.");
//					System.out.println("    'exit' - get out of here.");
//					System.out.println("    'quit' - get out of here.");
//					System.out.println("    'q'    - get out of here.");
//				}
//			}
//		}
//		catch (Exception ioe) 
//		{
//			ioe.printStackTrace();
//			System.exit(1);
//		}
//	}
}
