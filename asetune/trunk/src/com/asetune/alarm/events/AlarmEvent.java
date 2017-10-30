package com.asetune.alarm.events;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.asetune.cm.CountersModel;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;


public class AlarmEvent
extends Throwable
{
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
		INFO, 
		WARNING, 
		ERROR
	};
//	public static final int SEVERITY_UNKNOWN  = -1;
//	public static final int SEVERITY_INFO     = 0;
//	public static final int SEVERITY_WARNING  = 1;
//	public static final int SEVERITY_ERROR    = 2;
	
	public enum ServiceState
	{
		UNKNOWN, 
		UP, 
		AFFECTED, 
		DOWN
	};
//	public static final int STATE_SERVICE_IS_UNKNOWN  = -1;
//	public static final int STATE_SERVICE_IS_UP       = 0;
//	public static final int STATE_SERVICE_IS_AFFECTED = 1;
//	public static final int STATE_SERVICE_IS_DOWN     = 2;

	private static SimpleDateFormat _dateFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	protected String       _serviceType         = ""; // Typically the JDBC Connection.metadata.getProductName
	protected String       _serviceName         = ""; // Typically the servername
	protected String       _serviceInfo         = ""; 
	protected Object       _extraInfo           = "";
	protected Severity     _severity            = Severity.INFO;
	protected ServiceState _state               = ServiceState.UP;

	protected String       _description         = "";
	protected String       _reRaiseDesc         = "";
	protected String       _extendedDesc        = "";
	protected String       _reRaiseExtendedDesc = "";
	protected Object       _data                = null;
	protected Object       _reRaiseData         = null;

//	protected long         _initialCrTime       = -1;
	protected int          _reRaiseCount        = 0;
	protected long         _crTime              = -1;
	protected String       _crTimeStr           = "";
	protected long         _cancelTime          = -1;
	protected String       _cancelTimeStr       = "";
	protected int          _timeToLive          = -1; // Time To Live

	public boolean hasTimeToLive()        { return _timeToLive > 0; }
	public boolean hasTimeToLiveExpired()
	{
		if (_timeToLive <= 0)
			return true;

		return (System.currentTimeMillis() - getCrTime()) > _timeToLive;
	}
	public long   getCrTime()                    { return _crTime; }
	public String getCrTimeStr()                 { return _crTimeStr; }
	public long   getCrAgeInMs()                 
	{
		if (_cancelTime == -1)
			return System.currentTimeMillis() -_crTime;
		else
			return _cancelTime - _crTime;
	}
	/** For how long has this Alarm been active. it will be displayed as "[HH:]MM:SS" where hours only will be displayed if the alarm has been active for over an hour */ 
	public String getDuration()                  { return TimeUtils.msToTimeStr("%?HH[:]%MM:%SS", getCrAgeInMs()); }
	public long   getCancelTime()                { return _cancelTime; }
	public String getCancelTimeStr()             { return _cancelTimeStr; }
	public boolean isActive()                    { return _cancelTime == -1; }
                                                 
	public int  getTimeToLive()                  { return _timeToLive; }
	public void setTimeToLive(int ttl)           { _timeToLive = ttl; }
	public void setTimeToLive(long ttl)          { _timeToLive = (int)ttl; }

	public void setTimeToLive(CountersModel cm)
	{
		int ttl = -1;
		
		if (cm.getPostponeTime() > 0 && cm.isPostponeEnabled() )
			ttl = cm.getPostponeTime() + (int)cm.getLastSampleInterval();

		// If the CM is in the DEMAND list... then it's going to be refreshed on "next" try
		if (cm.getCounterController().isCmInDemandRefreshList(cm.getName()))
			ttl = -1;

		setTimeToLive( ttl );
	}
	
	public String       getServiceType()                { return _serviceType; }
	public String       getServiceName()                { return _serviceName; }
	public String       getServiceInfo()                { return _serviceInfo; }
	public Object       getExtraInfo()                  { return _extraInfo; }
	public Severity     getSeverity()                   { return _severity; }
	public ServiceState getState()                      { return _state; }

	public String       getDescription()                { return _description; }
	public String       getReRaiseDescription()         { return _reRaiseDesc; }
	public String       getExtendedDescription()        { return _extendedDesc; }
	public String       getReRaiseExtendedDescription() { return _reRaiseExtendedDesc; }
	public Object       getData()                       { return _data; }
	public Object       getReRaiseData()                { return _reRaiseData; }

	public void         setDescription(String desc)                { _description         = desc; }
	public void         setReRaiseDescription(String desc)         { _reRaiseDesc         = desc; }
	public void         setExtendedDescription(String desc)        { _extendedDesc        = desc; }
	public void         setReRaiseExtendedDescription(String desc) { _reRaiseExtendedDesc = desc; }
	public void         setData(Object data)                       { _data                = data; }
	public void         setReRaiseData(Object data)                { _reRaiseData         = data; }
	

	public void markCancel()
	{
		_cancelTime    = System.currentTimeMillis();
		_cancelTimeStr = _dateFormater.format(new Date(_crTime));
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
	/** SAme as getAlarmClassNameShort(), but it removes "AlarmEvent" from the name */
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
	public AlarmEvent(String serviceType, String serviceName, String serviceInfo, Object extraInfo, Severity severity, ServiceState state, String desc) 
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
		_severity     = severity;
		_state        = state;
		_description  = (desc        == null) ? "" : desc;;
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
			+ " severity="          + StringUtil.left("'" + _severity         + "',", 10)
			+ " state="             + StringUtil.left("'" + _state            + "',", 25)
			+ " repeatCnt="         + StringUtil.left("'" + _reRaiseCount     + "',", 3+3)
			+ " duration="          + StringUtil.left("'" + duration          + "',", 9+3)
//			+ " initialCrTime="     + StringUtil.left("'" + initialCrTime     + "',", 12+3)
			+ " crTime="            + StringUtil.left("'" + crTime            + "',", 12+3)
			+ " timeToLive="        + StringUtil.left("'" + _timeToLive       + "',", 5+3)
			+ " description: "      + _description;
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
}
