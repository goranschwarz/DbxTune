package com.asetune.alarm.writers;

import java.util.List;

import com.asetune.alarm.events.AlarmEvent;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.utils.Configuration;


/**
 * Implement you'r own forwarding of Alarms to various subsystems.
 * 
 * @author qgoschw
 */
public interface IAlarmWriter 
{
	/**
	 * When the AlarmHandler created this writer, the init() method will be called
	 * so it can configure itself. Meaning reading the "props" and initialize itself.
	 * 
	 * @param props The Configuration (basically a Properties object)
	 * @throws Exception when the initialization failes, you will 
	 *         throw an Exception to tell what was wrong
	 */
	public void init(Configuration conf)	
	throws Exception;

	/**
	 * If you want to print some configuration into the log, this can be done in init() or in this method.
	 */
	public void printConfig();

	/**
	 * If the alarm writer wants AlarmEvents to be raised <b>every</b> time an event is raised or only on <b>new</b> events
	 * If the AlarmHandler should call raise(AlarmEvent) everytime it receives it<br>
	 * Or if already raised alarms should be filtered out (because it has already been raised)
	 * 
	 * @return true  = Always call the method raise(AlarmEvent) when the AlarmHandler receives an event<br>
	 *         false = Only call raise(AlarmEvent) when a <b>new</b> event has been identified
	 */
	public boolean isCallReRaiseEnabled();
	
	/**
	 * A alarm has been added to the AlarmHandler
	 * 
	 * @param alarmEvent the actual alarm object
	 */
	public void raise(AlarmEvent alarmEvent);

	/**
	 * A alarm has been added to the AlarmHandler, which is already in <b>active</b> state
	 * 
	 * @param alarmEvent the actual alarm object
	 */
	void reRaise(AlarmEvent alarmEvent);

	/**
	 * The AlarmHandler think that this alarm can be canceled
	 * 
	 * @param alarmEvent the actual alram object
	 */
	public void cancel(AlarmEvent alarmEvent);

	/**
	 * At the end of a scan for suspected components, this method is called 
	 * and it can be used for "sending of a batch" of events or flushing a file
	 * or wahtever you want to do.
	 * <p>
	 * Even if no alarms was raised or canceled during the "scan", this method
	 * will always be called after all components has been checked.</p>
	 * 
	 * @param activeAlarms a list of alarms that are currently active in the 
	 *                     alarm handler. Note: do NOT remove entries from the 
	 *                     list, it should only be read. If you want to manipulate it.
	 *                     Create your own copy of the list.
	 */
	public void endOfScan(List<AlarmEvent> activeAlarms);
	
	/**
	 * When the AlarmHandler initiates, it restores old alarms from privious sessions
	 * this method is called from the AlarmHandler.init() after the old alarms has been
	 * restored. 
	 * <p>
	 * @param activeAlarms a list of alarms that was restored 
	 *                     Note: do NOT remove entriws from the list, 
	 *                     it should only be read. If you want to manipulate it.
	 *                     Create your own copy of the list.
	 */
	public void restoredAlarms(List<AlarmEvent> restoredAlarms);
	
	/**
	 * The writer has to have some kind of name...
	 * 
	 * @return name of the Writer
	 */
	public String getName();
	
	/**
	 * @return A Short description of the Writer
	 */
	public String getDescription();
	
	/** 
	 * Used by the: Create 'Offline Session' Wizard to get/set properties for this specific writer
	 * @return A List of settings. Should <b>NOT</b> return null (always return an empty list for no settings) 
	 */
	public List<CmSettingsHelper> getAvailableSettings();

	/**
	 * Start any internal threads etc that this service needs
	 */
	public void startService();

	/**
	 * Stop any internal threads etc that this service needs
	 */
	public void stopService();

	/**
	 * Should we continue with processing of this alarms or is it filtered out by some configuration
	 * 
	 * @param AlarmEvent
	 * @return true=continue, false=skip
	 */
	boolean doAlarm(AlarmEvent ae);

	/**
	 * Used by any GUI Configuration Helper/Setter that will configure the AlarmWriter
	 * @return
	 */
	public List<CmSettingsHelper> getAvailableFilters(); //getLocalAlarmWriterFilterSettings();

	/**
	 * If you want to print some configuration into the log, this can be done in init() or in this method.
	 */
	public void printFilterConfig();

}
