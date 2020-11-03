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
package com.asetune.pcs;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.writers.AlarmWriterToPcsJdbc.AlarmEventWrapper;
import com.asetune.cm.CountersModel;
import com.asetune.utils.StringUtil;


public class PersistContainer
{
	private static Logger _logger          = Logger.getLogger(PersistContainer.class);

	
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
				if (alias.indexOf("<SRVNAME>") != -1)
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
	private PersistContainer(Timestamp mainSampleTime, String serverName, String onHostname, String serverNameAlias)
	{
		_sessionStartTime = null;
		_mainSampleTime   = mainSampleTime;
		_serverName       = serverName;
		_onHostname       = onHostname;
		_serverNameAlias  = serverNameAlias;
	}
	
	public PersistContainer(HeaderInfo headerInfo)
	{
		this(headerInfo.getMainSampleTime(), headerInfo.getServerName(), headerInfo.getOnHostname(), headerInfo.getServerNameAlias());
	}


	/*---------------------------------------------------
	** Methods
	**---------------------------------------------------
	*/
	/** Set the time when X number of "main samples" should consist of, this is basically when we connect to a ASE and start to sample performance counters */
	public void setSessionStartTime(Timestamp startTime)    { _sessionStartTime = startTime; }
	/** Set the "main" Timestamp, this is the time when a "loop" to collect all various ConterModdel's, which we get data for */ 
	public void setMainSampleTime(Timestamp mainSampleTime) { _mainSampleTime   = mainSampleTime; }
	public void setServerName     (String serverName)       { _serverName       = serverName; }
	public void setOnHostname     (String onHostname)       { _onHostname       = onHostname; }
	public void setServerNameAlias(String alias)            { _serverNameAlias  = alias; }
	
	/** This can be used to "force" a new sample, for example if you want to 
	 * start a new session on reconnects to the monitored ASE server */
	public void setStartNewSample(boolean startNew) { _startNewSample = startNew; }

	/** Get the time when X number of "main samples" should consist of, this is basically when we connect to a ASE and start to sample performance counters */
	public Timestamp getSessionStartTime() { return _sessionStartTime; }
	/** Get the "main" Timestamp, this is the time when a "loop" to collect all various ConterModdel's, which we get data for */ 
	public Timestamp getMainSampleTime()   { return _mainSampleTime; }
	public long      getSampleInterval()   { return _sampleInterval; }
	public String    getServerName()       { return _serverName; }
	public String    getOnHostname()       { return _onHostname; }
	public String    getServerNameAlias()  { return _serverNameAlias; }
	public boolean   getStartNewSample()   { return _startNewSample; }

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

	
	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
	//// ---- TEST CODE ---- TEST CODE ---- TEST CODE ---- TEST CODE ----
	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
//	public static void main(String[] args) 
//	{
//	}

}
