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
package com.asetune.alarm.events;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import com.asetune.cm.CountersModel;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;


public class AlarmEvent
extends Throwable
{
	private static Logger _logger = Logger.getLogger(AlarmEvent.class);

	private static final long serialVersionUID = 1L;

	public static String SERVICE_TYPE_GENERIC   = "GENERIC"; // probably only used in: AlarmEventCommunicationTimeout
	public static String SERVICE_TYPE_ASE       = "ASE";
	public static String SERVICE_TYPE_RS        = "REPSERVER";
	public static String SERVICE_TYPE_WSPEER    = "WS-PEER";
//	public static String SERVICE_TYPE_ORACLE    = "ORACLE";
//	public static String SERVICE_TYPE_SQLSERVER = "SQLSERVER";
//	public static String SERVICE_TYPE_POSTGRES  = "POSTGRES";

	public enum Severity
	{
		UNKNOWN, 
		
		/**
		 * Informational message <br>
		 * This is typically something that we do not care about
		 */
		INFO, 

		/**
		 * Warning message <br>
		 */
		WARNING, 

		/**
		 * Error message <br>
		 */
		ERROR
	};
//	public static final int SEVERITY_UNKNOWN  = -1;
//	public static final int SEVERITY_INFO     = 0;
//	public static final int SEVERITY_WARNING  = 1;
//	public static final int SEVERITY_ERROR    = 2;
	
	public enum ServiceState
	{
		UNKNOWN, 
		
		/**
		 * UP <br>
		 * System is up and running, typically for a WARNING message
		 */
		UP, 
		
		/**
		 * AFFECTED <br>
		 * System is up and running, but some users <b>might</b> be affected, like BlockingLocks etc... 
		 */
		AFFECTED, 
		
		/**
		 * DOWN <br>
		 * System is down, all users are probably affected... 
		 */
		DOWN
	};
//	public static final int STATE_SERVICE_IS_UNKNOWN  = -1;
//	public static final int STATE_SERVICE_IS_UP       = 0;
//	public static final int STATE_SERVICE_IS_AFFECTED = 1;
//	public static final int STATE_SERVICE_IS_DOWN     = 2;

	public enum Category
	{
		/** Usnspecified */
		OTHER, 
		
		/** Only used for internal alarms etc */
		INTERNAL, 
		
		/** CPU Resources */
		CPU, 

		/** Some part is down */
		DOWN, 

		/** SPACE Utilization */
		SPACE, 

		/** Server Configuration */
		SRV_CONFIG, 

		/** LOCK */
		LOCK,

		/** HADR */
		HADR,

		/** RPO - Recovery Point Objective */
		RPO,
	};
	
	public static final int DEFAULT_raiseDelay = 0;

	private static SimpleDateFormat _dateFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	protected String       _serviceType         = ""; // Typically the JDBC Connection.metadata.getProductName
	protected String       _serviceName         = ""; // Typically the servername
	protected String       _serviceInfo         = ""; 
	protected Object       _extraInfo           = "";
	protected Category     _category            = Category.OTHER;
//	protected Category     _categoryDefault     = Category.OTHER;
	protected Severity     _severity            = Severity.INFO;
	protected ServiceState _state               = ServiceState.UP;

	//think more about how to implement this... and how it should be configurable (property AlarmEventName.raiseDelay=### or parameter to the AlarmEvent - then configurable for every creator of the AlarmEvent)
//	protected int          _raiseDelayInSec     = 0; // Delay or Postpone a initial raise with X seconds... meaning that a cancel can within the timeframe = That the Alarm will be irrelevant/canceled/not-raised
	protected int          _raiseDelayInSec     = DEFAULT_raiseDelay; // wait with the raise event until X number of seconds has passed (usabe for CPU events or other "peak" things that happends, which we might want to filter out)  
//	protected int          _raiseDelayInSecDefault = 0;

	protected String       _description             = "";
	protected String       _reRaiseDesc             = "";
	protected String       _extendedDesc            = "";
	protected String       _extendedDescHtml        = "";
	protected String       _reRaiseExtendedDesc     = "";
	protected String       _reRaiseExtendedDescHtml = "";
	protected Object       _data                    = null;
	protected Object       _reRaiseData             = null;

//	protected long         _initialCrTime       = -1;
	protected int          _reRaiseCount        = 0;
	protected long         _reRaiseTime         = -1;
	protected long         _crTime              = -1;
	protected String       _crTimeStr           = "";
	protected long         _cancelTime          = -1;
	protected String       _cancelTimeStr       = "";
	protected int          _timeToLive          = -1; // Time To Live in Milliseconds

	protected Number       _crossedThreshold    = null;

	protected int          _activeAlarmCount    = -1;

	/** How many Active Alarms to we have in the AlarmHandler */
	public int  getActiveAlarmCount()          { return _activeAlarmCount; }
	public void setActiveAlarmCount(int count) { _activeAlarmCount = count; }
	
	/**
	 * If Writer/Sender should <b>always</b> send this alarm<br>
	 * The the "filter" functionality will be discarded/short-circuited 
	 * 
	 * @return true if we should always send, false if we should look at the "filter" settings.
	 */
	public boolean alwaysSend()
	{
		return false;
	}
	
	public boolean hasTimeToLive()        { return _timeToLive > 0; }
	public boolean hasTimeToLiveExpired()
	{
		if (_timeToLive <= 0)
			return true;

		long time = Math.max(getCrTime(), getReRaiseTime());
		return (System.currentTimeMillis() - time) > _timeToLive;
	}
	public long   getCrTime()                    { return _crTime; }
	public String getCrTimeStr()                 { return _crTimeStr; }
	public String getCrTimeIso8601()             { return TimeUtils.toStringIso8601(_crTime); }
	public long   getCrAgeInMs()                 
	{
		if (_cancelTime == -1)
			return System.currentTimeMillis() -_crTime;
		else
			return _cancelTime - _crTime;
	}

	public int  getRaiseDelayInSec()        { return _raiseDelayInSec; }  
	public void setRaiseDelayInSec(int sec) { _raiseDelayInSec = sec;  }  
	public boolean hasRaiseDelay()          { return _raiseDelayInSec > 0; }
	public boolean hasRaiseDelayExpired()
	{
		if (_raiseDelayInSec <= 0)
			return true;
		
		long msSinceCreation = (System.currentTimeMillis() - _crTime);
		return msSinceCreation > (_raiseDelayInSec * 1000);
	}
	
	public long   getReRaiseTime()        { return _reRaiseTime; }
	public String getReRaiseTimeStr()     { return _reRaiseTime <= 0 ? null : TimeUtils.toString(_reRaiseTime); }
	public String getReRaiseTimeIso8601() { return _reRaiseTime <= 0 ? null : TimeUtils.toStringIso8601(_reRaiseTime); }
	public void   setReRaiseTime()        { _reRaiseTime = System.currentTimeMillis(); }


	/** For how long has this Alarm been active. it will be displayed as "[HH:]MM:SS" where hours only will be displayed if the alarm has been active for over an hour */ 
	public String getDuration()                  { return TimeUtils.msToTimeStr("%?HH[:]%MM:%SS", getCrAgeInMs()); }
	public long   getCancelTime()                { return _cancelTime; }
	public String getCancelTimeStr()             { return _cancelTimeStr; }
	public String getCancelTimeIso8601()         { return _cancelTime == -1 ? null : TimeUtils.toStringIso8601(_cancelTime); }
	public boolean isActive()                    { return _cancelTime == -1; }

	/** Get Time To Live in Milliseconds */
	public int  getTimeToLive()                  { return _timeToLive; }
	/** Set Time To Live in Milliseconds */
	public void setTimeToLive(int ttl)           { _timeToLive = ttl; }
	/** Set Time To Live in Milliseconds */
	public void setTimeToLive(long ttl)          { _timeToLive = (int)ttl; }

	public void setTimeToLive(CountersModel cm)
	{
		int ttl = -1;
		
		if (cm.getPostponeTime() > 0 && cm.isPostponeEnabled() )
		{
			int postponeTime = cm.getPostponeTime(); 
			if (postponeTime < 1000)
				postponeTime = postponeTime * 1000;

			// Get last sample intervall, if not found: get the default sleep time... if the numbers are negative turn them into positive...
			int sampleInterval = (int)cm.getLastSampleInterval();
			if (sampleInterval <= 0)
				sampleInterval = cm.getCounterController().getDefaultSleepTimeInSec();
			if (sampleInterval < 1000)
				sampleInterval = sampleInterval * 1000;
			sampleInterval = Math.abs( sampleInterval ); 
			
			ttl = postponeTime + sampleInterval;
		}

		// If the CM is in the DEMAND list... then it's going to be refreshed on "next" try
		if (cm.getCounterController().isCmInDemandRefreshList(cm.getName()))
			ttl = -1;

		setTimeToLive( ttl );
	}
	
	public String       getServiceType()                    { return _serviceType; }
	public String       getServiceName()                    { return _serviceName; }
	public String       getServiceInfo()                    { return _serviceInfo; }
	public Object       getExtraInfo()                      { return _extraInfo; }
	public Category     getCategory()                       { return _category; }
	public Severity     getSeverity()                       { return _severity; }
	public ServiceState getState()                          { return _state; }
                                                            
	public String       getDescription()                    { return !hasRaiseDelay() ? _description : _description + " [raiseDelay="+getRaiseDelayInSec()+"]"; }
	public String       getReRaiseDescription()             { return _reRaiseDesc; }
	public String       getExtendedDescription()            { return _extendedDesc; }
	public String       getExtendedDescriptionHtml()        { return StringUtil.hasValue(_extendedDescHtml) ? _extendedDescHtml : StringUtil.toHtmlString(_extendedDesc); }
	public String       getReRaiseExtendedDescription()     { return _reRaiseExtendedDesc; }
	public String       getReRaiseExtendedDescriptionHtml() { return StringUtil.hasValue(_reRaiseExtendedDescHtml) ? _reRaiseExtendedDescHtml : StringUtil.toHtmlString(_reRaiseExtendedDesc); }
	public Object       getData()                           { return _data; }
	public Object       getReRaiseData()                    { return _reRaiseData; }

//	public Category     getCategoryDefault()                { return _categoryDefault; }
//	public int          getRaiseDelayInSecDefault()         { return _raiseDelayInSecDefault; }

	public void         setDescription(String desc)                             { _description         = desc; }
	public void         setReRaiseDescription(String desc)                      { _reRaiseDesc         = desc; }
	public void         setExtendedDescription(String desc, String html)        { _extendedDesc        = desc; _extendedDescHtml        = html; }
	public void         setReRaiseExtendedDescription(String desc, String html) { _reRaiseExtendedDesc = desc; _reRaiseExtendedDescHtml = html; }
	public void         setData(Object data)                                    { _data                = data; }
	public void         setReRaiseData(Object data)                             { _reRaiseData         = data; }

	

	public Number getCrossedThreshold()
	{
		return _crossedThreshold;
	}
	public void setCrossedThreshold(Number threshold)
	{
		_crossedThreshold = threshold;
	}

	public void markCancel()
	{
		_cancelTime    = System.currentTimeMillis();
		_cancelTimeStr = _dateFormater.format(new Date(_cancelTime));
	}

//	public String getAlarmClassNameShort()
//	{
//		String alarmClass = this.getClass().getName();
//		return alarmClass.substring( alarmClass.lastIndexOf('.')+1 );
//	}
	public String getAlarmClass()
	{
		return this.getClass().getSimpleName();
	}
	/** SAme as getAlarmClass(), but it removes "AlarmEvent" from the name */
	public String getAlarmClassAbriviated()
	{
		return this.getClass().getSimpleName().replace("AlarmEvent", "");
	}


//	public AlarmEvent(String serviceType, String serviceName, String serviceInfo, Object extraInfo, int severity, int state, String desc) 
//	{
//		// This will provide us with a stacktrace if we want to look at that later...
//		super();
//
//		_serviceType  = (serviceType == null) ? "" : serviceType;
//		_serviceName  = (serviceName == null) ? "" : serviceName;
//		_serviceInfo  = (serviceInfo == null) ? "" : serviceInfo;
//		_extraInfo    = (extraInfo   == null) ? "" : extraInfo;
//		_severity     = severity;
//		_state        = state;
//		_description  = (desc        == null) ? "" : desc;;
//	}
	public AlarmEvent(String serviceType, String serviceName, String serviceInfo, Object extraInfo, Category category, Severity severity, ServiceState state, String desc, Number crossedThreshold) 
	{
		// This will provide us with a stacktrace if we want to look at that later...
		super();

		_crTime       = System.currentTimeMillis();
		_crTimeStr    = _dateFormater.format(new Date(_crTime));
		_timeToLive   = -1;

		_serviceType  = (serviceType == null) ? "" : serviceType;
		_serviceName  = (serviceName == null) ? "" : serviceName;
		_serviceInfo  = (serviceInfo == null) ? "" : serviceInfo;
		_extraInfo    = (extraInfo   == null) ? "" : extraInfo;
		_category     = category;
		_severity     = severity;
		_state        = state;
		_description  = (desc        == null) ? "" : desc;;
		
		_crossedThreshold = crossedThreshold;

		_category     = category;
//		_categoryDefault         = category; // Set default values so we can check for changes/overrides later on
//		_raiseDelayInSecDefault  = 0;        // Set default values so we can check for changes/overrides later on
		
		// Get UserDefined: 'category' properties
		_category = getDefaultProperty_category();

		// Get UserDefined: 'raiseDelay' properties
		_raiseDelayInSec = getDefaultProperty_raiseDelay();
	}

	/** get default property for 'category', override this to set any new default that is class specific, or use property 'className.category=CategoryEnumStr' */
	protected Category getDefaultProperty_category()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		
		String udCategoryProp = this.getClass().getSimpleName()+".category";
		String udCategory     = conf.getProperty(udCategoryProp, null);
		if (StringUtil.hasValue(udCategory))
		{
			if (_logger.isDebugEnabled())
				_logger.debug(getAlarmClassAbriviated() + ": Overriding/Reading default value of '"+_category+"' for 'category' using property '"+udCategoryProp+"' with value '"+udCategory+"'. for: "+this.getMessage());

			try {
				return Category.valueOf(udCategory);
			} catch (IllegalArgumentException e) {
				_logger.error("Problems parsing Category Value '"+udCategory+"' for the property '"+udCategoryProp+"'. known values: "+StringUtil.toCommaStr(Category.values())+". Caught: "+e);
			}
		}
		return _category;
	}

	/** get default property for 'raiseDelay', override this to set any new default that is class specific, or use property 'className.raise.delay=seconds' */
	protected int getDefaultProperty_raiseDelay()
	{
		Configuration conf = Configuration.getCombinedConfiguration();

		String udRaiseDelayInSecProp = this.getClass().getSimpleName()+".raise.delay";
		int    udRaiseDelayInSec     = conf.getIntProperty(udRaiseDelayInSecProp, -1);
		if (udRaiseDelayInSec != -1)
		{
			if (_logger.isDebugEnabled())
				_logger.debug(getAlarmClassAbriviated() + ": Overriding/Reading default value of '"+_raiseDelayInSec+"' for 'raiseDelayInSec' using property '"+udRaiseDelayInSecProp+"' with value '"+udRaiseDelayInSec+"'. for: "+this.getMessage());

			return udRaiseDelayInSec;
		}
		return _raiseDelayInSec;
	}
	
	/**
	 * This could be good for Short TEXT Messages etc...<br>
	 * <pre>
	 * yyyy-MM-dd HH:mm:ss.SSS - AseTune - SERVERNAME - module - INFO - UP - AlarmType - message
	 * </pre>
	 * @return
	 */
	public String getShortMessage()
	{
		return getCrTimeStr() 
				+ " - " + getServiceType()  // AseTune
				+ " - " + getServiceName()  // servername
				+ " - " + getExtraInfo()    // ...
				+ " - " + getCategory()     // ...
				+ " - " + getSeverity()     // INFO/WARNING/ERROR
				+ " - " + getState()        // UP/AFFECTED/DOWN
				+ " - " + getAlarmClassAbriviated()  // SrvDown/RunQueueLength/OsLoadAverage/RunningTransaction/HighCpuUtilization/FullTranLog/BlockingLock/...
				+ " - " + getDescription()
				;
	}

	@Override
	public String toString()
	{
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
		String alarmClass    = this.getClass().getSimpleName();
		String crTime        = sdf.format(new Date(_crTime));
//		String initialCrTime = "-none-";
//		if (_initialCrTime > 0)
//			initialCrTime = sdf.format(new Date(_initialCrTime));

		String duration = TimeUtils.msToTimeStr("%MM:%SS.%ms", getCrAgeInMs() );

		return "AlarmEvent: class=" + StringUtil.left("'" + alarmClass        + "',", 40)
			+ " serviceType="       + StringUtil.left("'" + _serviceType      + "',", 15)
			+ " serviceName="       + StringUtil.left("'" + _serviceName      + "',", 40)
			+ " serviceInfo="       + StringUtil.left("'" + _serviceInfo      + "',", 20)
			+ " extraInfo="         + StringUtil.left("'" + _extraInfo        + "',", 30)
			+ " category="          + StringUtil.left("'" + _category         + "',", 10)
			+ " severity="          + StringUtil.left("'" + _severity         + "',", 10)
			+ " state="             + StringUtil.left("'" + _state            + "',", 25)
			+ " repeatCnt="         + StringUtil.left("'" + _reRaiseCount     + "',", 3+3)
			+ " duration="          + StringUtil.left("'" + duration          + "',", 9+3)
//			+ " initialCrTime="     + StringUtil.left("'" + initialCrTime     + "',", 12+3)
			+ " crTime="            + StringUtil.left("'" + crTime            + "',", 12+3)
			+ " timeToLive="        + StringUtil.left("'" + _timeToLive       + "',", 5+3)
			+ " description: "      + getDescription();
	}
	
	@Override
	public String getMessage()
	{
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
		String alarmClass    = this.getClass().getSimpleName();
		String crTime        = sdf.format(new Date(_crTime));

		String duration = TimeUtils.msToTimeStr("%MM:%SS.%ms", getCrAgeInMs() );

		return "AlarmEvent: class=" + "'" + alarmClass        + "',"
			+ " serviceType="       + "'" + _serviceType      + "',"
			+ " serviceName="       + "'" + _serviceName      + "',"
			+ " serviceInfo="       + "'" + _serviceInfo      + "',"
			+ " extraInfo="         + "'" + _extraInfo        + "',"
			+ " category="          + "'" + _category         + "',"
			+ " severity="          + "'" + _severity         + "',"
			+ " state="             + "'" + _state            + "',"
			+ " repeatCnt="         + "'" + _reRaiseCount     + "',"
			+ " duration="          + "'" + duration          + "',"
			+ " crTime="            + "'" + crTime            + "',"
			+ " timeToLive="        + "'" + _timeToLive       + "',"
			+ " description: "      + getDescription();
	}
	
//	public static String severityToString(int severity)
//	{
//		switch (severity)
//		{
//		case SEVERITY_UNKNOWN:   return "UNKNOWN";
//		case SEVERITY_INFO:      return "INFO";
//		case SEVERITY_WARNING:   return "WARNING";
//		case SEVERITY_ERROR:     return "ERROR";
//		}
//		return "UNKNOWN-severity-id-"+severity;
//	}
//	
//	public static String stateToString(int state)
//	{
//		switch (state)
//		{
//		case STATE_SERVICE_IS_UNKNOWN:  return "SERVICE_IS_UNKNOWN";
//		case STATE_SERVICE_IS_UP:       return "SERVICE_IS_UP";
//		case STATE_SERVICE_IS_DOWN:     return "SERVICE_IS_DOWN";
//		case STATE_SERVICE_IS_AFFECTED: return "SERVICE_IS_AFFECTED";
//		}
//		return "UNKNOWN-state-id-"+state;
//	}
	
//	public void setRaiseRepeatValues(AlarmEvent existing)
//	{
//		if (existing == null)
//			return;
//
//		// increment counter
//		_raiseRepeatCount = existing._raiseRepeatCount + 1;
//
//		// set the initial CreationTime
//		_initialCrTime = (existing._initialCrTime > 0 ? existing._initialCrTime : existing._crTime);
//	}

	public void incrementReRaiseCount()
	{
		// increment counter
		_reRaiseCount++;
	}
	public int getReRaiseCount()
	{
		return _reRaiseCount;
	}
	public void setReRaiseCount(int count)
	{
		// increment counter
		_reRaiseCount = count;
	}

	public String stackTraceToString(Throwable t) 
	{
		StringWriter writer = new StringWriter();
		t.printStackTrace(new PrintWriter(writer));
		return writer.toString();
	}

	// hashCode() is generated with Eclipse
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_serviceType == null) ? 0 : _serviceType.hashCode());
		result = prime * result + ((_serviceName == null) ? 0 : _serviceName.hashCode());
		result = prime * result + ((_serviceInfo == null) ? 0 : _serviceInfo.hashCode());
		result = prime * result + ((_extraInfo   == null) ? 0 : _extraInfo  .hashCode());
		result = prime * result + ((_category    == null) ? 0 : _category   .hashCode());
		result = prime * result + ((_severity    == null) ? 0 : _severity   .hashCode());
		result = prime * result + ((_state       == null) ? 0 : _state      .hashCode());
		return result;
	}

	// equals() is generated with Eclipse
	@Override
	public boolean equals(Object obj)
	{
		if ( this == obj )
			return true;

		if ( obj == null )
			return false;

		if ( getClass() != obj.getClass() )
			return false;

		AlarmEvent other = (AlarmEvent) obj;

		// _serviceType
		if ( _serviceType == null )
		{
			if ( other._serviceType != null )
				return false;
		}
		else if ( !_serviceType.equals(other._serviceType) )
			return false;

		// _serviceName
		if ( _serviceName == null )
		{
			if ( other._serviceName != null )
				return false;
		}
		else if ( !_serviceName.equals(other._serviceName) )
			return false;

		// _serviceInfo
		if ( _serviceInfo == null )
		{
			if ( other._serviceInfo != null )
				return false;
		}
		else if ( !_serviceInfo.equals(other._serviceInfo) )
			return false;

		// _extraInfo
		if ( _extraInfo == null )
		{
			if ( other._extraInfo != null )
				return false;
		}
		else if ( !_extraInfo.equals(other._extraInfo) )
			return false;

		// _category
		if ( _category != other._category )
			return false;

		// _severity
		if ( _severity != other._severity )
			return false;

		// _state
		if ( _state != other._state )
			return false;

		return true;
	}

//	@Override
//	public boolean equals(Object obj) 
//	{
//		if (this == obj)
//			return true;
//
//		if (obj instanceof AlarmEvent) 
//		{
//			AlarmEvent ae = (AlarmEvent) obj;
//
//			if (   this._serviceType.equals(ae._serviceType) 
//			    && this._serviceName.equals(ae._serviceName)
//			    && this._serviceInfo.equals(ae._serviceInfo)
//			    && this._extraInfo  .equals(ae._extraInfo)
//			    && this._severity   .equals(ae._severity)
//			    && this._state      .equals(ae._state)
//			   )
//			{
//				return true;
//			}
//			else
//			{
//				return false;
//			}
//		}
//		return false;
//	}
	
//	/**
//	 * get id of AlarmEvent by calculating hash for individual elements, hash that and return a String<br>
//	 * This could for example be used externally to check for ID's of alarms (but for the moment it do NOT include TIME... so lets think more)
//	 * <p>
//	 * NOTE: NOT YET TESTED, so use with caution
//	 */
//	public String getHashId()
//	{
//		ByteBuffer byteBuffer = ByteBuffer.allocate(32); // 4 bytes per field, so 32 bytes covers 8 fields
//		byteBuffer.putInt(Objects.hashCode(_serviceType));
//		byteBuffer.putInt(Objects.hashCode(_serviceName));
//		byteBuffer.putInt(Objects.hashCode(_serviceInfo));
//		byteBuffer.putInt(Objects.hashCode(_extraInfo));
//		byteBuffer.putInt(Objects.hashCode(_category));
//		byteBuffer.putInt(Objects.hashCode(_severity));
//		byteBuffer.putInt(Objects.hashCode(_state));
//		byteBuffer.putInt(Objects.hashCode(null)); // reserved for future field typicall _crDate or similar to make it unique
//		
////		super.setId(DigestUtils.sha512Hex(byteBuffer.array())); 
//		String hashId = DigestUtils.sha512Hex(byteBuffer.array()); 
//		byteBuffer.clear();
//		
//		return hashId;
//	}
	
}
