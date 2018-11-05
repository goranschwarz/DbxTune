package com.asetune.alarm.writers;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.asetune.alarm.events.AlarmEvent;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSettingsHelper.Type;
import com.asetune.utils.Configuration;


/**
 * This AlarmWriter will write information to a file.<br>
 * That file will be monitored by SunMC (Sun Management Center), which will 
 * pass on the information to any other Management subsystem.<br>
 * A good candidate for SunMC to pass on to would probably be 
 * Ericsson OSS - FaultManagement subsystem
 * <p>
 * This AlarmWriter is NOT event driven, it will only do actions
 * when the AlarmHandler calls the method endOfScan().<br>
 * The methods raise() and cancel() are empty.
 * <p>  
 * 
 * <h2>How to Configure the AlarmHandler to use this AlarmWriter</h2>
 * The AlarmHandler has been initialized with a Configuration.<br>
 * In that configuration there one property specifying what writer(s)
 * the AlarmHandler should call when something happens.<br>
 * The below propert contains a comma ',' separated list of writer classes
 * <pre>
 * AlarmHandler.AlarmWriterClass = gorans.mred.alarm.AlarmWriterToSunMcFile
 * </pre>
 * 
 * <h2>How to Configure this AlarmWriter</h2>
 * The AlarmHandler which initializes this AlarmWriter will pass on 
 * the Configuration object, which containes properties.<br>
 * The following properties are used by this writer and they must 
 * be specified, if they are not specified the AlarmWriter will throw 
 * an Exception, with the meaning that it has noot been initialized,
 * and therefore the parent component should NOT start. 
 * 
 * <h4>AlarmWriterToSunMcFile.filename=FILENAME</h4>
 * Name of the file where to write information to<br>
 * You can have operating system Environment varaiables in here, 
 * those are spefified with ${NAME_OF_ENV_VAR}
 * 
 * <h4>AlarmWriterToSunMcFile.map.class.EventAlarmClassToMap=VALUE_STRING</h4>
 * <i>EventAlarmClassToMap</i> should be the name of the classname which you want to 
 * map to something. That something is a "value" string that will be written to the text
 * file previously specified "AlarmWriterToSunMcFile.filename".
 * <p>
 * The VALUE_STRING is of the format:<br>
 * [EVENT_NOT_FOUND_STATIC_STRING:]EVENT_FOUND_TEMPLATE_STRING<br>
 * <p>
 * The character ':' is the separator between "not found" and "found"<br>
 * If the "not found" is left out, nothing will be written for that 
 * alarm in the case when the alarm doesnt exist in the current alarm list<br>
 * <p>
 * <ul>
 * <li>EVENT_NOT_FOUND_STATIC_STRING is optinal and is the value written to the file if the
 * AlarmEvent can not be found in the current alarms.
 * <li>EVENT_FOUND_TEMPLATE_STRING is a string that will be treated as a "template", where
 * specific marker words could be replaced by runtime values from the AlarmEvent object.<br>
 * </ul>
 * For more information about the template string see "Template format" below.
 * <p>
 * 
 * <h4>AlarmWriterToSunMcFile.map.default=TEMPLATE_STRING</h4>
 * If an incoming AlarmEvent has not been mapped to any action, and this property is
 * specified. Then we will use this TEMPLATE_STRING to write info to the file
 * priviously specified.
 * 
 * <h2>Example of the Properties</h2>
 * <pre>
 * AlarmWriterToSunMcFile.filename = ${MRED_HOME}/mred_status.out
 * 
 * AlarmWriterToSunMcFile.map.class.AlarmEventAseDown              = ASESERVER=0 : ASESERVER=2
 * AlarmWriterToSunMcFile.map.class.AlarmEventAseRepAgentDown      = REPAGENT=0  : REPAGENT=1
 * AlarmWriterToSunMcFile.map.class.AlarmEventAseRepAgentProblem   = REPAGENT=0  : REPAGENT=1
 * AlarmWriterToSunMcFile.map.class.AlarmEventAseRepAgentUp        = REPAGENT=0  : REPAGENT=0
 * AlarmWriterToSunMcFile.map.class.AlarmEventAseTranLogFull       = TRANSLOG=0  : TRANSLOG=2
 * AlarmWriterToSunMcFile.map.class.AlarmEventRepserverDown        = REPSERVER=0 : REPSERVER=1
 * AlarmWriterToSunMcFile.map.class.AlarmEventRepserverSdFull      = STABLEQ=0   : STABLEQ=2
 * AlarmWriterToSunMcFile.map.class.AlarmEventRepserverSdThreshold = STABLEQ=0   : STABLEQ=1
 * AlarmWriterToSunMcFile.map.class.AlarmEventRepserverThreadDown  = REPTHREAD=0 : REPTHREAD=1
 * AlarmWriterToSunMcFile.map.class.AlarmEventWrongAppStatus       = APPSTATUS=0 : APPSTATUS=1
 * AlarmWriterToSunMcFile.map.class.AlarmEventWsGroupOutOfSync     = WSGROUP=0   : WSGROUP=1
 * 
 * AlarmWriterToSunMcFile.map.default = UNHANDLED '?className?'. '?serviceName?', ?severity?, ?state?, '?stateStr?', '?severityStr?'
 * </pre>
 * 
 * <h2>Template format</h2>
 * The TEMPLATE_STRING discussed above is "just a string", but it has some marker words
 * that will be translated during runtime into "actual" values.
 * <p>
 * Avialable template markers/variables
 * <p>
 * <TABLE>
 * <TR ALIGN="left"> <TH NOWRAP> variable      </TH> <TH>Explenation</TH>                                      <TH> Typical values</TH> </TR>
 * <TR ALIGN="left"> <TD NOWRAP> ?className?   </TD> <TD> Name of the AlarmEvent </TD>                         <TD> AlarmEventAseDown </TD> </TR>
 * <TR ALIGN="left"> <TD NOWRAP> ?serviceType? </TD> <TD> Type of the service </TD>                            <TD> "ASE", "REPSERVER" </TD> </TR>
 * <TR ALIGN="left"> <TD NOWRAP> ?serviceName? </TD> <TD> Name of the service </TD>                            <TD> "SERVERNAME[.dbname]" </TD> </TR>
 * <TR ALIGN="left"> <TD NOWRAP> ?serviceInfo? </TD> <TD> Information about the service </TD>                  <TD> "ACTIVE", "STANDBY", "RSSD" </TD> </TR>
 * <TR ALIGN="left"> <TD NOWRAP> ?extraInfo?   </TD> <TD> Additional information about the event... </TD>      <TD> For example in RsThreadDown it will be a threadname </TD> </TR>
 * <TR ALIGN="left"> <TD NOWRAP> ?severity?    </TD> <TD> A integer that states how severe the event is </TD>  <TD> -1=UNKNOWN, 0=INFO, 1=WARNING, 2=ERROR </TD> </TR>
 * <TR ALIGN="left"> <TD NOWRAP> ?severityStr? </TD> <TD> The severity as a string value </TD>                 <TD> "UNKNOWN", "INFO", "WARNING", "ERROR" </TD> </TR>
 * <TR ALIGN="left"> <TD NOWRAP> ?state?       </TD> <TD> A integer with the state </TD>                       <TD> -1=UNKNOWN, 0=OK, 1=AFFECTED, 2=DOWN </TD> </TR>
 * <TR ALIGN="left"> <TD NOWRAP> ?stateStr?    </TD> <TD> State as a string </TD>                              <TD> "SERVICE_IS_UNKNOWN", "SERVICE_IS_UP", "SERVICE_IS_AFFECTED", "SERVICE_IS_DOWN" </TD> </TR>
 * <TR ALIGN="left"> <TD NOWRAP> ?description? </TD> <TD> A textual description of the event </TD>             <TD> Whatever msg that was generated in the code. </TD> </TR>
 * </TABLE>
 * <p>
 * The above strings etc might change in the future
 * <p>
 * 
 * <h2>Some more examples of configuration and file output</h2>
 * Config:
 * <pre>
 * AlarmWriterToSunMcFile.map.class.AlarmEventRepserverSdFull      = STABLEQ=0   : STABLEQ=2
 * AlarmWriterToSunMcFile.map.class.AlarmEventRepserverSdThreshold = STABLEQ=0   : STABLEQ=1
 * </pre>
 * The RepServers StableDevice is full, so we will have 4 active alarms for this<br>
 * <pre>
 * AlarmEventRepserverSdThreshold(...extraInfo=SD_THRESHOLD_1...) -> mapped to: STABLEQ=1
 * AlarmEventRepserverSdThreshold(...extraInfo=SD_THRESHOLD_2...) -> mapped to: STABLEQ=1
 * AlarmEventRepserverSdThreshold(...extraInfo=SD_THRESHOLD_3...) -> mapped to: STABLEQ=1
 * AlarmEventRepserverSdFull(...with some info...)                -> mapped to: STABLEQ=2
 * </pre>
 * Output:
 * <pre>
 * STABLEQ=2
 * </pre>
 * <p>
 * This doesnt look that hard, but really...<br>
 * When a StableDevice is full, the Alarms for Threshold 1, 2 and 3 is also "active".
 * This means that we internally would write <b>three</b> "STABLE=1" message and <b>one</b> "STABLEQ=2"
 * messages, but that's probably not a good idea, we need to sort that out and only write
 * one message. Probably the one with the highest number/priority... but how do I know that?
 * <p>
 * So at the end of all internal mapping I will sort all the "alarm event" strings 
 * that was generated and try to discard all "duplicates".<br>
 * Duplicated are discarded by finding out what the "key" part of the string is. 
 * I will then skip all "keys" that has the same value, and keeping only the last one found
 * in the sorted alarm list.<br>
 * This means that all "alarm strings" in the alarm list are treated as key=value strings.<br>
 * <ul>
 * <li>The separator between KEY and VALUE is a equal sign '='
 * <li>If NO equal sig exists in the string, the whole string i considdered as a KEY
 * </ul>
 * <p>
 * See below example:
 * <pre>
 *   KEY1=0
 *   KEY1=1
 *   KEY1=2
 *   KEY2=0
 *   KEY3=0
 *   KEY4=AAAA
 *   KEY4=BBBBB
 *   STRING 1 with NO equal sign
 *   STRING 2 with NO equal sign
 * </pre>
 *   
 * Will result in:
 * <pre>
 *   KEY1=2
 *   KEY2=0
 *   KEY3=0
 *   KEY4=BBBBB
 *   STRING 1 with NO equal sign
 *   STRING 2 with NO equal sign
 * </pre>
 * 
 * 
 * <h2>Another example</h2>
 * Config:
 * <pre>
 * AlarmWriterToSunMcFile.map.class.AlarmEventAseDown = ASESERVER=0 : ASESERVER=2
 * </pre>
 * Three ASE servers goes down...(PPLE_SYB_MAIN_NodeB, PPLE_SYB_STAT_NodeB, PPLE_SYB_STAT_NodeA), 
 * while the most important server PPLE_SYB_MAIN_NodeA is still up.<br>
 * so the following events would internally be generated.<br>
 * <pre>
 * AlarmEventAseDown(...serviceName=PPLE_SYB_MAIN_NodeB, serviceInfo=STANDBY...) -> mapped to: ASESERVER=2
 * AlarmEventAseDown(...serviceName=PPLE_SYB_STAT_NodeB, serviceInfo=STANDBY...) -> mapped to: ASESERVER=2
 * AlarmEventAseDown(...serviceName=PPLE_SYB_STAT_NodeA, serviceInfo=ACTIVE...)  -> mapped to: ASESERVER=2
 * </pre>
 * Output:
 * <pre>
 * ASESERVER=2
 * </pre>
 * <p>
 * In the above scenario we might wanted to send a little more information 
 * to the ManagementCenter application.<br>
 * Yes lets do that, with the following configuration.
 * <pre>
 * AlarmWriterToSunMcFile.map.class.AlarmEventAseDown = ASESERVER=0 : ASESERVER.?serviceInfo?.?serviceName?=2
 * </pre>
 * Output:
 * <pre>
 * ASESERVER.ACTIVE.PPLE_SYB_STAT_NodeA=2
 * ASESERVER.STANDBY.PPLE_SYB_MAIN_NodeB=2
 * ASESERVER.STANDBY.PPLE_SYB_STAT_NodeB=2
 * </pre>
 * 
 * @author qgoschw
 *
 */
public class AlarmWriterToSunMcFile 
extends AlarmWriterAbstract
{
	private static Logger _logger             = Logger.getLogger(AlarmWriterToSunMcFile.class);

	/** Give the writer a name */
	private String        _name               = "AlarmWriterToSunMcFile";

	/** What filename should we write events to */
	private String        _writeToFileName    = null;

	/** A list of classes that are mapped to a action string */
	private Map<String, String> _mappedAlarmClasses = new HashMap<>();

	/** If the AlarmEvent that comes in is NOT mapped to anything */
	private String        _unMapped           = null;


	@Override
	public boolean isCallReRaiseEnabled()
	{
		return false;
	}

	@Override
	public String getDescription()
	{
		return "Write all Active alarms to a file, which Sun Management Center is monitoring.";
	}
	
	/**
	 * Initialize the component
	 */
	@Override
	public void init(Configuration conf) 
	throws Exception 
	{
		super.init(conf);

		String propPrefix = "AlarmWriterToSunMcFile.";
		String propname = null;

		// property: name
		propname = propPrefix+"name";
		_writeToFileName = conf.getProperty(propname, _name);

		// WRITE init message, jupp a little late, but I wanted to grab the _name
		_logger.info("Initializing the AlarmHandler.AlarmWriter component named '"+_name+"'.");

		// property: filename
		// and then open the file.
		propname = propPrefix+"filename";
		_writeToFileName = conf.getProperty(propname);
		if (_writeToFileName == null)
		{
			throw new Exception("The property '"+propname+"' is mandatory for the AlarmWriter named '"+getName()+"'.");
		}
		try
		{
			// TEST to write to file...
			PrintStream writeToFile = new PrintStream( new FileOutputStream(_writeToFileName) );
			writeToFile.close();
		}
		catch (FileNotFoundException e)
		{
			String msg = "The AlarmWriter named '"+getName()+"' cant open the writer file '"+_writeToFileName+"'.";
			_logger.error(msg);
			throw new Exception(msg, e);
		}
		
		// list all: 'AlarmWriterToSunMcFile.map.class.' properties
		String classPrefix = propPrefix + "map.class.";
//		Enumeration keys = props.getKeys(classPrefix);
//		while (keys.hasMoreElements())
//		{
//			String key = (String) keys.nextElement();
//			String val = props.getProperty(key);
//
//			//_logger.debug("readProps: key='"+key+"', val='"+val+"'.");
//
//			// Take away the prefix
//			String className = key.replaceFirst(classPrefix, "");
//
//			// Add it to the mapper
//			_logger.debug("AddToMapper: key='"+className+"', val='"+val+"'.");
//			_mappedAlarmClasses.put(className, val);
//		}
		List<String> keys = conf.getKeys(classPrefix);
		for (String key : keys)
		{
			String val = conf.getProperty(key);

			//_logger.debug("readProps: key='"+key+"', val='"+val+"'.");

			// Take away the prefix
			String className = key.replaceFirst(classPrefix, "");

			// Add it to the mapper
			_logger.debug("AddToMapper: key='"+className+"', val='"+val+"'.");
			_mappedAlarmClasses.put(className, val);
		}
		
		if (_mappedAlarmClasses.size() == 0)
		{
			throw new Exception("There are NO properties named '"+classPrefix+"', you need atleast one. AlarmWriter named '"+getName()+"'.");
		}

		
		// property: map.default
		propname = propPrefix+"map.default";
		_unMapped = conf.getProperty(propname);
	}

	@Override
	public void printConfig()
	{
	}

	@Override
	public List<CmSettingsHelper> getAvailableSettings()
	{
		ArrayList<CmSettingsHelper> list = new ArrayList<>();
		
		list.add( new CmSettingsHelper("Filename",   Type.MANDATORY, "AlarmWriterToSunMcFile.filename",      String.class,  null, null, "The file where active alarms are written to."));
		list.add( new CmSettingsHelper("MapClassTo", Type.PROBABLY,  "AlarmWriterToSunMcFile.map.class.Xxx", String.class,  null, null, "FIXME"));
		
		return list;
	}

	/**
	 * A alarm has been raised by the AlarmHandler
	 */
	@Override
	public void raise(AlarmEvent alarmEvent) 
	{
		_logger.debug(getName()+": -----RAISE-----: "+alarmEvent);
	}

	@Override
	public void reRaise(AlarmEvent alarmEvent)
	{
		_logger.debug(getName()+": -----RE-RAISE-----: "+alarmEvent);
	}

	/**
	 * A alarm has been canceled by the AlarmHandler
	 */
	@Override
	public void cancel(AlarmEvent alarmEvent) 
	{
		_logger.debug(getName()+": -----CANCEL-----: "+alarmEvent);
	}

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
	@Override
	public void restoredAlarms(List<AlarmEvent> restoredAlarms)
	{
		_logger.debug(getName()+": -----RESTORED-ALARMS-----.");
		writeAlarmFile(restoredAlarms);
	}

	/**
	 * At the end of a scan for suspected components, this method is called 
	 * and it can be used for "sending of a batch" of events or flushing a file
	 * or wahtever you want to do.
	 * <p>
	 * Even if no alarms was raised or canceled during the "scan", this method
	 * will always be called after all components has been checked.</p>
	 * 
	 * @param activeAlarms a list of alarms that are currently active in the 
	 *                     alarm handler. Note: do NOT remove entriws from the 
	 *                     list, it should only be read. If you want to manipulate it.
	 *                     Create your own copy of the list.
	 */
	@Override
	public void endOfScan(List<AlarmEvent> activeAlarms)
	{
		_logger.debug(getName()+": -----END-OF-SCAN-----.");
		writeAlarmFile(activeAlarms);
	}

	/**
	 * What is this AlarmWriter named to...
	 */
	@Override
	public String getName() 
	{
		return _name;
	}

	/**
	 * Write the mapped set of alarms to the output file.
	 * 
	 * @param activeAlarms list of the alarms.
	 */
	private void writeAlarmFile(List<AlarmEvent> activeAlarms)
	{
		// Put "write alarm" records to this list
		// At the end we will sort the list, and only write the "last" entry in every "alarm group"
		List<String> writeThis = new LinkedList<>();
		
		// Print active alarms
		// Print mapped alarm classes
		if ( _logger.isDebugEnabled() )
		{
			for (AlarmEvent ae : activeAlarms)
				_logger.debug("writeAlarmFile.ACTIVE_ALARMS: " + ae );

			for (String str : _mappedAlarmClasses.values())
				_logger.debug("writeAlarmFile.MAPPED_ALARM: " + str );
				
//			iter = _mappedAlarmClasses.entrySet().iterator();
//			while (iter.hasNext())
//			{
//				_logger.debug("writeAlarmFile.MAPPED_ALARM: " + iter.next() );
//			}
		}
		
		// Loop all mapped alarms, then check if the alarm is within the activeList
		// Then write info to the "write" list
		Iterator<String> mapIter = _mappedAlarmClasses.keySet().iterator();
		while (mapIter.hasNext())
		{
			String mappedClassName = (String) mapIter.next();
			String mappedToValue   = (String) _mappedAlarmClasses.get(mappedClassName);
			
			String valNotFound     = "";
			String valFound        = mappedToValue;

			// the mappedToValue can have a "not found" and a "found" value.
			// the mappedToValue should look like className = [NOT_FOUND_ALARM_VAL:]FOUND_ALARM_VAL
			if (mappedToValue.indexOf(":") > 0)
			{
				String p1 = mappedToValue.substring(0, mappedToValue.indexOf(":")).trim();
				String p2 = mappedToValue.substring(mappedToValue.indexOf(":")+1).trim();
				valNotFound = p1;
				valFound    = p2;
			}

			// Check in the active alarm list 
			// if we got any of "that kind" then translate some values in the template
			boolean alarmIsMapped = false;

			Iterator<AlarmEvent> eventIter = activeAlarms.iterator();
			while (eventIter.hasNext())
			{
				AlarmEvent ae = (AlarmEvent) eventIter.next();

				// Make translations of "variables" in the template
				if ( mappedClassName.equals(ae.getAlarmClass()) )
				{
					alarmIsMapped = true;
					
					// Translate values...
					String mappedTo = translate(ae, valFound);

					// Write the info to the "write" list
					// that list will later on be "stripped" for duplicates
					writeThis.add(mappedTo);
				}

			} // end: while(activeAlarms)

			// The alarm was not found in the active alarm list
			if ( ! alarmIsMapped )
			{
				// Write the info to the "write" list
				// that list will later on be "stripped" for duplicates
				writeThis.add(valNotFound);
			}
			
		} // end: while(_mappedAlarmClasses)

		// Now go and check for active alarms that is not mapped to anything
		// then add it as "unmapped"
		if ( _unMapped != null )
		{
			Iterator<AlarmEvent> eventIter = activeAlarms.iterator();
			while (eventIter.hasNext())
			{
				AlarmEvent ae = (AlarmEvent) eventIter.next();

				if ( ! _mappedAlarmClasses.containsKey(ae.getAlarmClass()) )
				{
					String unmappedTemplate = _unMapped;

					// Translate values...
					String mappedTo = translate(ae, unmappedTemplate);

					// Write the info to the "write" list
					// that list will later on be "stripped" for duplicates
					writeThis.add(mappedTo);
				}

			} // end: while(activeAlarms)
		}
		
		
		// Now sort the "write" list
		Collections.sort( writeThis );
		
		if ( _logger.isDebugEnabled() )
		{
			Iterator<String> iter = writeThis.iterator();
			while (iter.hasNext())
			{
				_logger.debug("writeAlarmFile.SORTED_WRITE_LIST: '" + iter.next() + "'." );
			}
		}


		// Reset the file...
		PrintStream writeToFile = null;
		try
		{
			// TEST to write to file...
			writeToFile = new PrintStream( new FileOutputStream(_writeToFileName) );
		}
		catch (FileNotFoundException e)
		{
			String msg = "The AlarmWriter named '"+getName()+"' cant open the writer file '"+_writeToFileName+"'.";
			_logger.error(msg);
			return;
		}
		

		
		//
		// Take away duplicates from the list
		// The duplication detection works on a "KEY" basis
		//
		// This means that the string is cosiddered as a KEY=value
		// The separator between KEY and VALUE is a equal sign '='
		// If NO equal sig exists in the string, the whole string i considdered as a KEY
		//
		// I will KEEP the "last" KEY value
		//
		// See below example:
		//   KEY1=0
		//   KEY1=1
		//   KEY1=2
		//   KEY2=0
		//   KEY3=0
		//   KEY4=AAAA
		//   KEY4=BBBBB
		//   STRING 1 with NO equal sign
		//   STRING 2 with NO equal sign
		// Will result in:
		//   KEY1=2
		//   KEY2=0
		//   KEY3=0
		//   KEY4=BBBBB
		//   STRING 1 with NO equal sign
		//   STRING 2 with NO equal sign
		//
		String[] eventArray = (String[]) writeThis.toArray(new String[writeThis.size()]);

		for (int i=0; i<eventArray.length; i++)
		{
			String thisRowEvent = eventArray[i];
			String thisKey      = thisRowEvent;
			//String thisVal      = null;
			if (thisRowEvent.indexOf("=") > 0)
			{
				thisKey = thisRowEvent.substring(0, thisRowEvent.indexOf("=")).trim();
				//thisVal = thisRowEvent.substring(thisRowEvent.indexOf("=")+1).trim();
			}

			String nextRowEvent = null;
			String nextKey      = null;
			//String nextVal      = null;
			if ( (i + 1) < eventArray.length )
				nextRowEvent = eventArray[ i + 1 ];

			nextKey      = nextRowEvent;

			if (nextRowEvent != null && nextRowEvent.indexOf("=") > 0)
			{
				nextKey = nextRowEvent.substring(0, nextRowEvent.indexOf("=")).trim();
				//nextVal = nextRowEvent.substring(nextRowEvent.indexOf("=")+1).trim();
			}

			// Next row is not the same as this (at a key level)
			if ( ! thisKey.equals(nextKey) )
			{
				// Do not print empty rows.
				if ( ! thisRowEvent.equals("") )
				{
					_logger.debug("writeAlarmFile.ACTUAL_WRITE_TO_FILE: '"+thisRowEvent+"'.");
					
					//----------------------------------
					// FINALY WRITE TO THE FILE
					//----------------------------------
					if (writeToFile != null)
					{
						writeToFile.println(thisRowEvent);
					}
				}
			}
			
		} // end: for(eventArray)

		// Cleanup...
		if (writeToFile != null)
		{
			writeToFile.close();
		}
		
	} // end: method
	
	/**
	 * Translate values inside a template to real values from a AlarmEvent
	 * 
	 * @param ae       The AlarmEvent to get actual values from.
	 * @param template The template string to be translated
	 * @return         The translated template string.
	 */
	private String translate(AlarmEvent ae, String template)
	{
		String mappedTo = template;
		mappedTo = mappedTo.replaceAll( "\\?className\\?",   ae.getAlarmClass() );
		mappedTo = mappedTo.replaceAll( "\\?serviceType\\?", ae.getServiceType() );
		mappedTo = mappedTo.replaceAll( "\\?serviceName\\?", ae.getServiceName() );
		mappedTo = mappedTo.replaceAll( "\\?serviceInfo\\?", ae.getServiceInfo() );
		mappedTo = mappedTo.replaceAll( "\\?extraInfo\\?",   ae.getExtraInfo()+"" );
		mappedTo = mappedTo.replaceAll( "\\?category\\?",    ae.getCategory().toString() );
		mappedTo = mappedTo.replaceAll( "\\?severity\\?",    ae.getSeverity().toString() );
		mappedTo = mappedTo.replaceAll( "\\?state\\?",       ae.getState().toString() );
		mappedTo = mappedTo.replaceAll( "\\?description\\?", ae.getDescription() );

		return mappedTo;
	}
}
