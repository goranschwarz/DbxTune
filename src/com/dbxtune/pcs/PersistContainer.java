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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.dbxtune.pcs;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.DbxTune;
import com.dbxtune.Version;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.writers.AlarmWriterToPcsJdbc.AlarmEventWrapper;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.JsonCmWriterOptions;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;


public class PersistContainer
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	
	/*---------------------------------------------------
	** Constants
	**---------------------------------------------------
	*/

	/*---------------------------------------------------
	** class members
	**---------------------------------------------------
	*/
	/** This one is maintained from the PersistWriter, which is responsible for starting/ending storage sessions. 
	 * If _sessionStartTime is set by a user, it will be over written by the PersistCoiunterHandler or the PersistWriter */
	protected Timestamp           _sessionStartTime = null;
	protected Timestamp           _mainSampleTime   = null;
	protected long                _sampleInterval   = 0;
	protected String              _serverName       = null;
	protected String              _onHostname       = null;
	protected String              _serverNameAlias  = null;
	protected String              _serverDisplayName= null;
	private   List<CountersModel> _counterObjects   = null;

	protected boolean             _startNewSample   = false;

	protected List<AlarmEvent>        _activeAlarmList = null;
	protected List<AlarmEventWrapper> _alarmEventsList = null; // This is "history" events (that has happened during last sample)


	/**
	 * Small class to hold Header information
	 */
	public static class HeaderInfo
	{
		private Timestamp _mainSampleTime;
		private String    _serverName;        // Normally the DBMS instance name
		private String    _serverNameAlias;   // If we want an alternate name for DbxCentral (eg schema name storage in DbxCentral)
		private String    _serverDisplayName; // If we want an alternate name for DbxCentral (just for buttons etc)
		private String    _onHostname;        // on which host does this server run
		private Timestamp _counterClearTime;  // if the server wise counters are cleared at some date/time, then this is it.
		
		public HeaderInfo()
		{
		}
		
		public Timestamp getMainSampleTime()             { return _mainSampleTime; }
		public void      setMainSampleTime(Timestamp ts) { _mainSampleTime = ts; }

		public String    getServerName()       { return _serverName; }
		public String    getOnHostname()       { return _onHostname; }
		public Timestamp getCounterClearTime() { return _counterClearTime; }

		public void      setServerNameAlias(String alias) { _serverNameAlias = alias; }
		public String    getServerNameAlias()
		{
			String alias = _serverNameAlias; 
			if (StringUtil.hasValue(alias))
			{
				// replace template with the server name
				if (alias.contains("<SRVNAME>"))
					alias = alias.replace("<SRVNAME>", getServerName());
			}
			return alias; 
		}

		/**
		 * Get Server Name
		 * <ul>
		 *   <li>First try the Alternate/Alis server name</li>
		 *   <li>Fallback on the real DBMS Instance Name</li>
		 * </ul>
		 * @return
		 */
		public String getServerNameOrAlias()
		{
			String srvName = getServerNameAlias();
			if (StringUtil.isNullOrBlank(srvName))
				srvName = getServerName();
			return srvName;
		}

		public void setServerDisplayName(String displayName)
		{
			_serverDisplayName = displayName;
		}
		public String getServerDisplayName()
		{
			String name = _serverDisplayName; 
			if (StringUtil.hasValue(name))
			{
				// replace template with the server name
				if (name.contains("<SRVNAME>"))
					name = name.replace("<SRVNAME>", getServerName());
			}
			return name; 
		}
		
		public HeaderInfo(Timestamp mainSampleTime, String serverName, String onHostname, Timestamp counterClearTime)
		{
			_mainSampleTime   = mainSampleTime;
			_serverName       = serverName;
			_onHostname       = onHostname;
			_counterClearTime = counterClearTime;

			_serverNameAlias  = null;
			
			if (true)
			{
				if (_onHostname == null)
				{
					Exception traceEx = new Exception("TRACE-INFO: Just created a HeaderInfo that had a NULL value for 'onHostname'. This may cause problems later...");
					_logger.info(traceEx.getMessage(), traceEx);
				}
			}
		}
	}
	
	/*---------------------------------------------------
	** Constructors
	**---------------------------------------------------
	*/
//	public PersistContainer(Timestamp sessionStartTime, Timestamp mainSampleTime, String serverName, String onHostname)
//	{
//		_sessionStartTime = sessionStartTime;
//		_mainSampleTime   = mainSampleTime;
//		_serverName       = serverName;
//		_onHostname       = onHostname;
//	}
	private PersistContainer(Timestamp mainSampleTime, String serverName, String onHostname, String serverNameAlias, String serverDisplayName)
	{
		_sessionStartTime  = null;
		_mainSampleTime    = mainSampleTime;
		_serverName        = serverName;
		_onHostname        = onHostname;
		_serverNameAlias   = serverNameAlias;
		_serverDisplayName = serverDisplayName;
	}
	
	public PersistContainer(HeaderInfo headerInfo)
	{
		this(headerInfo.getMainSampleTime(), headerInfo.getServerName(), headerInfo.getOnHostname(), headerInfo.getServerNameAlias(), headerInfo.getServerDisplayName());
	}


	/*---------------------------------------------------
	** Methods
	**---------------------------------------------------
	*/
	/** Set the time when X number of "main samples" should consist of, this is basically when we connect to a ASE and start to sample performance counters */
	public void setSessionStartTime (Timestamp startTime)      { _sessionStartTime  = startTime; }
	/** Set the "main" Timestamp, this is the time when a "loop" to collect all various ConterModdel's, which we get data for */ 
	public void setMainSampleTime   (Timestamp mainSampleTime) { _mainSampleTime    = mainSampleTime; }
	public void setServerName       (String serverName)        { _serverName        = serverName; }
	public void setOnHostname       (String onHostname)        { _onHostname        = onHostname; }
	public void setServerNameAlias  (String alias)             { _serverNameAlias   = alias; }
	public void setServerDisplayName(String displayName)       { _serverDisplayName = displayName; }
	
	/** This can be used to "force" a new sample, for example if you want to 
	 * start a new session on reconnects to the monitored ASE server */
	public void setStartNewSample(boolean startNew) { _startNewSample = startNew; }

	/** Get the time when X number of "main samples" should consist of, this is basically when we connect to a ASE and start to sample performance counters */
	public Timestamp getSessionStartTime()  { return _sessionStartTime; }
	/** Get the "main" Timestamp, this is the time when a "loop" to collect all various ConterModdel's, which we get data for */ 
	public Timestamp getMainSampleTime()    { return _mainSampleTime; }
	public long      getSampleInterval()    { return _sampleInterval; }
	public String    getServerName()        { return _serverName; }
	public String    getOnHostname()        { return _onHostname; }
	public String    getServerNameAlias()   { return _serverNameAlias; }
	public String    getServerDisplayName() { return _serverDisplayName; }
	public boolean   getStartNewSample()    { return _startNewSample; }

	/**
	 * Get Server Name
	 * <ul>
	 *   <li>First try the Alternate/Alis server name</li>
	 *   <li>Fallback on the real DBMS Instance Name</li>
	 * </ul>
	 * @return
	 */
	public String getServerNameOrAlias()
	{
		String srvName = getServerNameAlias();
		if (StringUtil.isNullOrBlank(srvName))
			srvName = getServerName();
		return srvName;
	}

	public CountersModel getCm(String name)
	{
		if (_counterObjects == null)
			return null;

		for (Iterator<CountersModel> it = _counterObjects.iterator(); it.hasNext();)
        {
	        CountersModel cm = it.next();
	        if (cm.getName().equals(name))
	        	return cm;
        }
		return null;
	}

	public void add(CountersModel cm)
	{
		if (_counterObjects == null)
			_counterObjects = new ArrayList<CountersModel>(20);

		if (_logger.isDebugEnabled())
			_logger.debug("PersistContainer.add: name="+StringUtil.left(cm.getName(),20)+", timeHead="+cm.getSampleTimeHead()+", sampleTime="+cm.getSampleTime()+", interval="+cm.getSampleInterval());

		// Hmmm, this can probably be done better
		// The _sampleInterval might be to low or to high
		// It should span ThisSampleTime -> untillNextSampleTime
		long cmInterval = cm.getSampleInterval();
		if (_sampleInterval < cmInterval)
			_sampleInterval = cmInterval;
		
		_counterObjects.add(cm.copyForStorage());
	}

	/**
	 * Add the active alarms to the container.
	 * @param alarmList
	 */
	public void addActiveAlarms(List<AlarmEvent> alarmList)
	{
		_activeAlarmList = new ArrayList<>(alarmList);
	}

	/**
	 * Get the alarm list
	 * @return Will never return null
	 */
	public List<AlarmEvent> getAlarmList()
	{
		if (_activeAlarmList == null)
			return Collections.emptyList();
		return _activeAlarmList;
	}

	/**
	 * Add Alarm Events that has happened in last sample (RAISE/RE-RAISE/CANCEL)
	 * @param alarmEvents
	 */
	public void addAlarmEvents(List<AlarmEventWrapper> alarmEvents)
	{
		_alarmEventsList = new ArrayList<>(alarmEvents);
	}

	/**
	 * Get the alarm list
	 * @return Will never return null
	 */
	public List<AlarmEventWrapper> getAlarmEvents()
	{
		if (_alarmEventsList == null)
			return Collections.emptyList();
		return _alarmEventsList;
	}


	/**
	 * Get counter objects
	 * @return Will never return null
	 */
	public List<CountersModel> getCounterObjects()
	{
		if (_counterObjects == null)
			return Collections.emptyList();
		return _counterObjects;		
	}
	
	/**
	 * check if the containe contains anything...
	 * @return
	 */
	public boolean isEmpty()
	{
		return getCounterObjects().isEmpty() && getAlarmList().isEmpty() && getAlarmEvents().isEmpty();
//		boolean isEmpty = true;
//		if (_counterObjects != null)
//			isEmpty = _counterObjects.isEmpty();
//
//		if (_activeAlarmList != null)
//			isEmpty = _activeAlarmList.isEmpty();
//			
//		if (_alarmEventsList != null)
//			isEmpty = _alarmEventsList.isEmpty();
//		
//		return isEmpty;
	}
	
	public boolean equals(PersistContainer pc)
	{
		if (pc == null)	         return false;
		if (_mainSampleTime == null) return false;
		
		return _mainSampleTime.equals(pc.getMainSampleTime());
	}
	
	/**
	 * The HEAD sample time might not be the one we campare to<br>
	 * So lets try to see if the Timestamp is within the "head sample time" + sampleInterval
	 * 
	 * @param ts Typically a sampleTime from a CounterModel and not the "head" sample time
	 * @return true if within the interval
	 */
	public boolean equalsApprox(Timestamp ts)
	{
		if (ts == null)	         return false;
		if (_mainSampleTime == null) return false;
		
		if (    _mainSampleTime.equals(ts) 
		     || ( ts.after(_mainSampleTime) && ts.getTime() <= (_mainSampleTime.getTime() + _sampleInterval) ) 
		   )
			return true;
		return false;
	}



	//--------------------------------------------------------------------
	//---- to JSON ----
	//--------------------------------------------------------------------

	/** Every time the message structure is changed this should be increments so that any receiver knows what fields to expect */
	public static final int JSON_MESSAGE_VERSION = 1;

	/**
	 * Null save save way to do obj.toString()
	 *  
	 * @param obj The object
	 * @return obj.toString() or if (null if the inout is null
	 */
	private static String toString(Object obj)
	{
		return obj == null ? null : obj.toString(); 
	}

	/**
	 * Null save save way to do obj.toString()
	 *  
	 * @param obj The object
	 * @return obj.toString() or if (null if the inout is null
	 */
	private static BigDecimal toBigDec(Number num)
	{
		return num == null ? null : new BigDecimal(num.toString()); 
	}



	/**
	 * Write JSON for the container
	 * 
	 * @param sendCounters                   What COUNTER(s) should we send/write information about
	 * @param sendGraphs                     What GRAPH/CHART(s) should we send/write information about
	 * @param sendExtendedAlarmDescAsHtml    Should Extended Alarm text be in HTML = true, or should we produce PLAIN TEXT = false
	 * 
	 * @return A JSON String
	 * @throws IOException 
	 */
	public String toJsonMessage(SendCountersConfig sendCounters, SendCountersConfig sendGraphs, boolean sendExtendedAlarmDescAsHtml)
	throws IOException
	{
		return toJsonMessage(this, sendCounters, sendGraphs, sendExtendedAlarmDescAsHtml);
	}

	/**
	 * Write JSON for the container
	 * 
	 * @param cont                           The PersistContainer we should produce JSON text for 
	 * @param sendCountersConfig             What COUNTER(s) should we send/write information about
	 * @param sendGraphsConfig               What GRAPH/CHART(s) should we send/write information about
	 * @param sendExtendedAlarmDescAsHtml    Should Extended Alarm text be in HTML = true, or should we produce PLAIN TEXT = false
	 * 
	 * @return A JSON String
	 * @throws IOException 
	 */
	public static String toJsonMessage(PersistContainer cont, SendCountersConfig sendCountersConfig, SendCountersConfig sendGraphsConfig, boolean sendExtendedAlarmDescAsHtml)
	throws IOException
	{
		StringWriter sw = new StringWriter();

		JsonFactory jfactory = new JsonFactory();
		JsonGenerator gen = jfactory.createGenerator(sw);
		gen.setPrettyPrinter(new DefaultPrettyPrinter());
		gen.setCodec(new ObjectMapper(jfactory));

		
		String collectorHostname = "-unknown-";
		try
		{
			InetAddress addr = InetAddress.getLocalHost();
			collectorHostname = addr.getCanonicalHostName();
		}
		catch (UnknownHostException ignore) {}

		int collectorSampleInterval = -1;
		if (CounterController.hasInstance())
		{
			collectorSampleInterval = CounterController.getInstance().getDefaultSleepTimeInSec();
		}

		// Management host/port
//		String collectorMgtHostname = null;
//		int    collectorMgtPort = -1;
//		String collectorMgtInfo = null;
		
		// Get the current recording database from the INFO Configuration object ( which is written in PersistWriterJdbc.startServices() )
		String collectorCurrentUrl = null;
		String collectorInfoFile = null;
		Configuration conf = Configuration.getInstance(DbxTune.DBXTUNE_NOGUI_INFO_CONFIG);
		if (conf != null)
		{
			collectorCurrentUrl = conf.getProperty("pcs.h2.jdbc.url");
			collectorInfoFile   = conf.getFilename();
			
//			collectorMgtHostname = conf.getProperty   ("dbxtune.management.host", null);
//			collectorMgtPort     = conf.getIntProperty("dbxtune.management.port", -1);
//			collectorMgtInfo     = conf.getProperty   ("dbxtune.management.info", null);
		}

		// Get List of Cm's with enabled Graphs / Counters
//		List<String> cmListEnabled         = new ArrayList<>();
//		List<String> cmListEnabledCounters = new ArrayList<>();
//		List<String> cmListEnabledGraphs   = new ArrayList<>();
////		for (CountersModel cm : cont.getCounterObjects())
//		for (CountersModel cm : CounterController.getInstance().getCmList())
//		{
//			// For the moment do not send Append Models
//			if (cm instanceof CountersModelAppend) 
//				continue;
//
////			if ( ! cm.hasValidSampleData() )
////				continue;
//			
////			if ( ! cm.isPersistCountersEnabled() )
////				continue;
//			
//			if ( ! cm.isActive() )
//				continue;
//
//			String shortCmName = cm.getName();
//
//			cmListEnabled.add(shortCmName);
//
//			if (sendCountersConfig.isEnabled(shortCmName)) 
//				cmListEnabledCounters.add(shortCmName);
//
//			if (sendGraphsConfig.isEnabled(shortCmName) && cm.hasTrendGraphData())
//				cmListEnabledGraphs.add(shortCmName);
//		}
		
//		System.out.println();
//		System.out.println("#### BEGIN JSON #######################################################################");
//		System.out.println("getSessionStartTime = " + cont.getSessionStartTime());
//		System.out.println("getMainSampleTime   = " + cont.getMainSampleTime());
//		System.out.println("getServerName       = " + cont.getServerName());
//		System.out.println("getOnHostname       = " + cont.getOnHostname());

		gen.writeStartObject();
		
		gen.writeFieldName("head");
		gen.writeStartObject();
			gen.writeNumberField("messageVersion"         , JSON_MESSAGE_VERSION);
			gen.writeStringField("appName"                , Version.getAppName());
			gen.writeStringField("appVersion"             , Version.getVersionStr());
			gen.writeStringField("appBuildString"         , Version.getBuildStr());
			gen.writeStringField("collectorHostname"      , collectorHostname);
			gen.writeNumberField("collectorSampleInterval", collectorSampleInterval);
			gen.writeStringField("collectorCurrentUrl"    , collectorCurrentUrl);
			gen.writeStringField("collectorInfoFile"      , collectorInfoFile);
//			gen.writeStringField("collectorMgtHostname"   , collectorMgtHostname);
//			gen.writeNumberField("collectorMgtPort"       , collectorMgtPort);
//			gen.writeStringField("collectorMgtInfo"       , collectorMgtInfo);

			gen.writeStringField("sessionStartTime"       , TimeUtils.toStringIso8601(cont.getSessionStartTime()));
			gen.writeStringField("sessionSampleTime"      , TimeUtils.toStringIso8601(cont.getMainSampleTime()));
			gen.writeStringField("serverName"             , cont.getServerName());
			gen.writeStringField("onHostname"             , cont.getOnHostname());
			gen.writeStringField("serverNameAlias"        , cont.getServerNameAlias());
			gen.writeStringField("serverDisplayName"      , cont.getServerDisplayName());

// Make better names...: enabledCmList, enabledCmSendCountersList, enabledCmSendGraphsList
//			gen.writeObjectField("cmListEnabled"          , cmListEnabled);
//			gen.writeObjectField("cmListEnabledGraphs"    , cmListEnabledGraphs);
//			gen.writeObjectField("cmListEnabledCounters"  , cmListEnabledCounters);
		gen.writeEndObject();

		// Write ACTIVE the alarms
		List<AlarmEvent> alarmList = cont.getAlarmList();
		if (alarmList != null && !alarmList.isEmpty())
		{
			gen.writeFieldName("activeAlarms");
			gen.writeStartArray();
			for (AlarmEvent ae : alarmList)
			{
				gen.writeStartObject();
					gen.writeStringField("alarmClass"                 , toString( ae.getAlarmClass()                 ));
					gen.writeStringField("alarmClassAbriviated"       , toString( ae.getAlarmClassAbriviated()       ));
					gen.writeStringField("serviceType"                , toString( ae.getServiceType()                ));
					gen.writeStringField("serviceName"                , toString( ae.getServiceName()                ));
					gen.writeStringField("serviceInfo"                , toString( ae.getServiceInfo()                ));
					gen.writeStringField("extraInfo"                  , toString( ae.getExtraInfo()                  ));
					gen.writeStringField("category"                   , toString( ae.getCategory()                   ));
					gen.writeStringField("severity"                   , toString( ae.getSeverity()                   ));
					gen.writeStringField("state"                      , toString( ae.getState()                      ));
					gen.writeStringField("alarmId"                    , toString( ae.getAlarmId()                    ));
					gen.writeNumberField("repeatCnt"                  ,           ae.getReRaiseCount()                );
//gen.writeBooleanField("hasExtendedDescription"        , StringUtil.hasValue( toString( sendExtendedAlarmDescAsHtml ? ae.getExtendedDescriptionHtml() : ae.getExtendedDescription() )) );
//gen.writeBooleanField("hasReRaiseExtendedDescription" , StringUtil.hasValue( toString( sendExtendedAlarmDescAsHtml ? ae.getReRaiseExtendedDescriptionHtml() : ae.getReRaiseExtendedDescription() )) );
					gen.writeStringField("duration"                   , toString( ae.getFullDuration(true)           ));
					gen.writeStringField("alarmDuration"              , toString( ae.getAlarmDuration()              ));
					gen.writeStringField("fullDuration"               , toString( ae.getFullDuration()               ));
					gen.writeNumberField("fullDurationAdjustmentInSec",           ae.getFullDurationAdjustmentInSec() );
					gen.writeNumberField("creationAgeInMs"            ,           ae.getCrAgeInMs()                   );
					gen.writeNumberField("creationTime"               ,           ae.getCrTime()                      );
					gen.writeStringField("creationTimeIso8601"        , toString( ae.getCrTimeIso8601()              )); 
					gen.writeNumberField("reRaiseTime"                ,           ae.getReRaiseTime()                 );
					gen.writeStringField("reRaiseTimeIso8601"         , toString( ae.getReRaiseTimeIso8601()         ));
					gen.writeNumberField("cancelTime"                 ,           ae.getCancelTime()                  );
					gen.writeStringField("cancelTimeIso8601"          , toString( ae.getCancelTimeIso8601()          ));
					gen.writeNumberField("TimeToLive"                 ,           ae.getTimeToLive()                  );
					gen.writeNumberField("threshold"                  , toBigDec( ae.getCrossedThreshold()           ));
					gen.writeStringField("data"                       , toString( ae.getData()                       ));
					gen.writeStringField("description"                , toString( ae.getDescription()                ));
					gen.writeStringField("extendedDescription"        , toString( sendExtendedAlarmDescAsHtml ? ae.getExtendedDescriptionHtml() : ae.getExtendedDescription() ));
					gen.writeStringField("reRaiseData"                , toString( ae.getReRaiseData()                ));
					gen.writeStringField("reRaiseDescription"         , toString( ae.getReRaiseDescription()         ));
					gen.writeStringField("reRaiseExtendedDescription" , toString( sendExtendedAlarmDescAsHtml ? ae.getReRaiseExtendedDescriptionHtml() : ae.getReRaiseExtendedDescription() ));
				gen.writeEndObject();
			}
			gen.writeEndArray();
		}

		// Write HISTORICAL the alarms (things that has happened during last sample: RAISE/RE-RAISE/CANCEL)
		List<AlarmEventWrapper> alarmEvents = cont.getAlarmEvents();
		if (alarmEvents != null && !alarmEvents.isEmpty())
		{
			gen.writeFieldName("alarmEvents");
			gen.writeStartArray();
			for (AlarmEventWrapper aew : alarmEvents)
			{
				gen.writeStartObject();
					gen.writeStringField("eventTime"                  , TimeUtils.toStringIso8601(aew.getAddDate()   ));
					gen.writeStringField("action"                     , toString( aew.getAction()                    ));
					
					AlarmEvent ae = aew.getAlarmEvent();
					
					gen.writeStringField("alarmClass"                 , toString( ae.getAlarmClass()                 ));
					gen.writeStringField("alarmClassAbriviated"       , toString( ae.getAlarmClassAbriviated()       ));
					gen.writeStringField("serviceType"                , toString( ae.getServiceType()                ));
					gen.writeStringField("serviceName"                , toString( ae.getServiceName()                ));
					gen.writeStringField("serviceInfo"                , toString( ae.getServiceInfo()                ));
					gen.writeStringField("extraInfo"                  , toString( ae.getExtraInfo()                  ));
					gen.writeStringField("category"                   , toString( ae.getCategory()                   ));
					gen.writeStringField("severity"                   , toString( ae.getSeverity()                   ));
					gen.writeStringField("state"                      , toString( ae.getState()                      ));
					gen.writeStringField("alarmId"                    , toString( ae.getAlarmId()                    ));
					gen.writeNumberField("repeatCnt"                  ,           ae.getReRaiseCount()                );
//gen.writeBooleanField("hasExtendedDescription"        , StringUtil.hasValue( toString( sendExtendedAlarmDescAsHtml ? ae.getExtendedDescriptionHtml() : ae.getExtendedDescription() )) );
//gen.writeBooleanField("hasReRaiseExtendedDescription" , StringUtil.hasValue( toString( sendExtendedAlarmDescAsHtml ? ae.getReRaiseExtendedDescriptionHtml() : ae.getReRaiseExtendedDescription() )) );
					gen.writeStringField("duration"                   , toString( ae.getFullDuration(true)           ));
					gen.writeStringField("alarmDuration"              , toString( ae.getAlarmDuration()              ));
					gen.writeStringField("fullDuration"               , toString( ae.getFullDuration()               ));
					gen.writeNumberField("fullDurationAdjustmentInSec",           ae.getFullDurationAdjustmentInSec() );
					gen.writeNumberField("creationAgeInMs"            ,           ae.getCrAgeInMs()                   );
					gen.writeNumberField("creationTime"               ,           ae.getCrTime()                      );
					gen.writeStringField("creationTimeIso8601"        , toString( ae.getCrTimeIso8601()              )); 
					gen.writeNumberField("reRaiseTime"                ,           ae.getReRaiseTime()                 );
					gen.writeStringField("reRaiseTimeIso8601"         , toString( ae.getReRaiseTimeIso8601()         ));
					gen.writeNumberField("cancelTime"                 ,           ae.getCancelTime()                  );
					gen.writeStringField("cancelTimeIso8601"          , toString( ae.getCancelTimeIso8601()          ));
					gen.writeNumberField("TimeToLive"                 ,           ae.getTimeToLive()                  );
					gen.writeNumberField("threshold"                  , toBigDec( ae.getCrossedThreshold()           ));
					gen.writeStringField("data"                       , toString( ae.getData()                       ));
					gen.writeStringField("description"                , toString( ae.getDescription()                ));
					gen.writeStringField("extendedDescription"        , toString( sendExtendedAlarmDescAsHtml ? ae.getExtendedDescriptionHtml() : ae.getExtendedDescription() ));
					gen.writeStringField("reRaiseData"                , toString( ae.getReRaiseData()                ));
					gen.writeStringField("reRaiseDescription"         , toString( ae.getReRaiseDescription()         ));
					gen.writeStringField("reRaiseExtendedDescription" , toString( sendExtendedAlarmDescAsHtml ? ae.getReRaiseExtendedDescriptionHtml() : ae.getReRaiseExtendedDescription() ));
				gen.writeEndObject();
			}
			gen.writeEndArray();
		}
		
		gen.writeFieldName("collectors");
		gen.writeStartArray();


		//--------------------------------------
		// COUNTERS
		//--------------------------------------
//-------------------------------------------
// Moved the below logic to: cm.toJson(...)
//-------------------------------------------
//		for (CountersModel cm : cont.getCounterObjects())
//		{
//			// For the moment do not send Append Models
//			if (cm instanceof CountersModelAppend) 
//				continue;
//
////System.out.println("HttpJson.toJsonMessage[srvName='"+cont.getServerNameAlias()+"', CmName="+StringUtil.left(cm.getName(),30)+"]: hasValidSampleData()="+cm.hasValidSampleData()+", hasData()="+cm.hasData()+", hasTrendGraphData()="+cm.hasTrendGraphData()+", writeGraphs="+writeGraphs+", getTrendGraphCountWithData()="+cm.getTrendGraphCountWithData());
//			if ( ! cm.hasValidSampleData() )
//				continue;
//
//			if ( ! cm.hasData() && ! cm.hasTrendGraphData() )
//				continue;
//
//			String shortCmName = cm.getName();
//			
//			// Create Options object that specifies *what* JSON objects to write/produce 
//			JsonCmWriterOptions writeOptions = new JsonCmWriterOptions();
//			writeOptions.writeCounters      = sendCounters.isEnabled(shortCmName);
//			if (writeOptions.writeCounters)
//			{
//				writeOptions.writeMetaData      = sendCounters.isMetaDataEnabled    (shortCmName);
//				writeOptions.writeMetaData_jdbc = sendCounters.isMetaDataJdbcEnabled(shortCmName);
//				writeOptions.writeMetaData_cm   = sendCounters.isMetaDataCmEnabled  (shortCmName);
//				writeOptions.writeCounters_abs  = sendCounters.isAbsEnabled         (shortCmName);
//				writeOptions.writeCounters_diff = sendCounters.isDiffEnabled        (shortCmName);
//				writeOptions.writeCounters_rate = sendCounters.isRateEnabled        (shortCmName);
//			}
//
//			writeOptions.writeGraphs = sendGraphs.isEnabled(shortCmName);
//			
//			// if we ONLY write graph data, but there is no graphs with data
//			// or possibly move this check INTO the cm method: cm.toJson(w, writeOptions);
//			if ( writeOptions.writeCounters == false && (writeOptions.writeGraphs && cm.getTrendGraphCountWithData() == 0))
//				continue;
//
//			// Use the CM method to write JSON
//			cm.toJson(gen, writeOptions);
//		}
		for (CountersModel cm : cont.getCounterObjects())
		{
			String shortCmName = cm.getName();
			
			// Create Options object that specifies *what* JSON objects to write/produce 
			JsonCmWriterOptions writeOptions = new JsonCmWriterOptions();
			
			// The "sender", decides what we should send...
			writeOptions.writeCounters = sendCountersConfig.isEnabled(shortCmName);
			if (writeOptions.writeCounters)
			{
				writeOptions.writeMetaData      = sendCountersConfig.isMetaDataEnabled    (shortCmName);
				writeOptions.writeMetaData_jdbc = sendCountersConfig.isMetaDataJdbcEnabled(shortCmName);
				writeOptions.writeMetaData_cm   = sendCountersConfig.isMetaDataCmEnabled  (shortCmName);
				writeOptions.writeCounters_abs  = sendCountersConfig.isAbsEnabled         (shortCmName);
				writeOptions.writeCounters_diff = sendCountersConfig.isDiffEnabled        (shortCmName);
				writeOptions.writeCounters_rate = sendCountersConfig.isRateEnabled        (shortCmName);
			}

			writeOptions.writeGraphs = sendGraphsConfig.isEnabled(shortCmName);
			
			// Use the CM method to write JSON
			cm.toJson(gen, writeOptions);
		}

		gen.writeEndArray();
		gen.writeEndObject();
		gen.close();

		String jsonStr = sw.toString();
		
		// Debug write to a file, if property is set
		if (Configuration.getCombinedConfiguration().hasProperty("PersistContainer.debug.toJson.writeToFile"))
		{
			//-------------------------------------------------------------------------------------------------------------------
			// Property example: PersistWriterToHttpJson.debug.writeToFile = c:\tmp\PersistWriterToHttpJson.tmp.json
			//-------------------------------------------------------------------------------------------------------------------
			String debugToFileNameStr = Configuration.getCombinedConfiguration().getProperty("PersistContainer.debug.toJson.writeToFile", "");
			if (StringUtil.hasValue(debugToFileNameStr))
			{
				if ("STDOUT".equalsIgnoreCase(debugToFileNameStr))
				{
					System.out.println("#### BEGIN JSON #####################################################################");
					System.out.println(jsonStr);
					System.out.println("#### END JSON #######################################################################");
				}
				else
				{
					File debugToFileName = new File(debugToFileNameStr);
					
					try
					{
						_logger.info("Writing JSON message to DEBUG file '" + debugToFileName.getAbsolutePath() + "'.");
						FileUtils.writeStringToFile(debugToFileName, jsonStr, StandardCharsets.UTF_8);
					}
					catch(Exception ex)
					{
						_logger.error("PROBLEMS Writing JSON message to DEBUG file '" + debugToFileName.getAbsolutePath() + "', skipping and continuing.", ex);
					}
				}
			}
		}

		return jsonStr;
	}

	
	
	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
	//// ---- TEST CODE ---- TEST CODE ---- TEST CODE ---- TEST CODE ----
	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
//	public static void main(String[] args) 
//	{
//	}

}
