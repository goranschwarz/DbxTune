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
package com.asetune.central.pcs;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asetune.Version;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DbxTuneSample
{
	private final static Logger _logger = LoggerFactory.getLogger(DbxTuneSample.class);

	String _appName;
	String _appVersion;
	String _appBuildString;
	String _collectorHostname;
	int    _collectorSampleInterval;
	String _collectorCurrentUrl;
	String _collectorInfoFile;
	
//	String _sessionStartTime;
//	String _sessionSampleTime;
	Timestamp _sessionStartTime;
	Timestamp _sessionSampleTime;
	String _serverName;
	String _onHostname;
	
	List<CmEntry>           _collectors   = new ArrayList<>();
	List<AlarmEntry>        _activeAlarms = new ArrayList<>();
	List<AlarmEntryWrapper> _alarmEvents  = new ArrayList<>();
	
	public List<CmEntry>           getCollectors()   { return _collectors; }
	public List<AlarmEntry>        getActiveAlarms() { return _activeAlarms; }
	public List<AlarmEntryWrapper> getAlarmEntries() { return _alarmEvents; }

	public String    getAppName()                 { return _appName;                 }
	public String    getAppVersion()              { return _appVersion;              }
	public String    getAppBuildStr()             { return _appBuildString;          }
	public String    getCollectorHostname()       { return _collectorHostname;       }
	public int       getCollectorSampleInterval() { return _collectorSampleInterval; }
	public String    getCollectorCurrentUrl()     { return _collectorCurrentUrl;     }
	public String    getCollectorInfoFile()       { return _collectorInfoFile;       }
	
	public Timestamp getSessionStartTime()  { return _sessionStartTime;  }
	public Timestamp getSessionSampleTime() { return _sessionSampleTime; }
	public String    getServerName()        { return _serverName;        }
	public String    getOnHostname()        { return _onHostname;        }

	public void setAppName                (String appName)        { _appName                 = appName;     }
	public void setAppVersion             (String appVersion)     { _appVersion              = appVersion;  }
	public void setAppBuildStr            (String appBuildStr)    { _appBuildString          = appBuildStr; }
	public void setCollectorHostname      (String hostname)       { _collectorHostname       = hostname;    }
	public void setCollectorSampleInterval(int    interval)       { _collectorSampleInterval = interval;    }
	public void setCollectorCurrentUrl    (String url)            { _collectorCurrentUrl     = url;         }
	public void setCollectorInfoFile      (String filename)       { _collectorInfoFile       = filename;    }
	
	public void setSessionStartTime (Timestamp sessionStartTime)  { _sessionStartTime  = sessionStartTime;  }
	public void setSessionSampleTime(Timestamp sessionSampleTime) { _sessionSampleTime = sessionSampleTime; }
	public void setServerName       (String    serverName)        { _serverName        = serverName;        }
	public void setOnHostname       (String    onHostname)        { _onHostname        = onHostname;        }

	public DbxTuneSample(String appName, String appVersion, String appBuildString, String collectorHostname, int collectorSampleInterval, String collectorCurrentUrl, String collectorInfoFile, Timestamp sessionStartTime, Timestamp sessionSampleTime, String serverName, String onHostname)
	{
		_appName                 = appName;
		_appVersion              = appVersion;
		_appBuildString          = appBuildString;
		_collectorHostname       = collectorHostname;
		_collectorSampleInterval = collectorSampleInterval;
		_collectorCurrentUrl     = collectorCurrentUrl;
		_collectorInfoFile       = collectorInfoFile;

		_sessionStartTime        = sessionStartTime;
		_sessionSampleTime       = sessionSampleTime;
		_serverName              = serverName;
		_onHostname              = onHostname;
	}
	public void add(CmEntry cmEntry)
	{
		_collectors.add(cmEntry);
	}

	public void addActiveAlarm(AlarmEntry ae)
	{
		_activeAlarms.add(ae);
	}

	public void addAlarmEntry(AlarmEntryWrapper aew)
	{
		_alarmEvents.add(aew);
	}


	
//	public static class Sample
//	{
//		String _sessionStartTime;
//		String _sessionSampleTime;
//		String _serverName;
//		String _onHostname;
//		
//		List<CmEntry> _collectors = new ArrayList<>();
//		
//		public Sample(String sessionStartTime, String sessionSampleTime, String serverName, String onHostname)
//		{
//			_sessionStartTime  = sessionStartTime;
//			_sessionSampleTime = sessionSampleTime;
//			_serverName        = serverName;
//			_onHostname        = onHostname;
//		}
//		public void add(CmEntry cmEntry)
//		{
//			_collectors.add(cmEntry);
//		}
//	}
	
	public static class MetaDataEntry
	{
		String  _columnName;
		String  _jdbcTypeName;
		int     _jdbcType;
		String  _javaClassName;
		String  _guessedDbmsType;
		boolean _isDiffColumn;
		boolean _isPctColumn;

		public MetaDataEntry(String columnName, String jdbcTypeName, String javaClassName, String guessedDbmsType, boolean isDiffColumn, boolean isPctColumn)
		{
			_columnName      = columnName; 
			_jdbcTypeName    = jdbcTypeName;
			_jdbcType        = ResultSetTableModel.getColumnJavaSqlTypeNameToInt(jdbcTypeName);
			_javaClassName   = javaClassName;
			_guessedDbmsType = guessedDbmsType; 
			_isDiffColumn    = isDiffColumn; 
			_isPctColumn     = isPctColumn; 
		}
		
	}
	
	public static class CmEntry
	{
		String                  _name; 
		String                  _desc; 
		Timestamp               _sessionSampleTime;
		Timestamp               _cmSampleTime;
		int                     _cmSampleMs;
		String                  _type;

		List<MetaDataEntry>     _metaData     = new ArrayList<>();
		CounterEntry            _absCounters  = new CounterEntry(1);
		CounterEntry            _diffCounters = new CounterEntry(2);
		CounterEntry            _rateCounters = new CounterEntry(3);
		Map<String, GraphEntry> _graphMap     = new LinkedHashMap<>();

		private int     _statGraphCount;
		private int     _statAbsRowCount;
		private int     _statDiffRowCount;
		private int     _statRateRowCount;
		private int     _sqlRefreshTime;
		private int     _guiRefreshTime;
		private int     _lcRefreshTime;
		private boolean _hasNonConfiguredMonitoringHappened;
		private String  _nonConfiguredMonitoringMissingParams;
		private String  _nonConfiguredMonitoringMessage;
		private boolean _isCountersCleared;
		private boolean _hasValidSampleData;
		private String  _sampleExceptionMsg;
		private String  _sampleExceptionFullText;
		
		public String    getName()              { return _name;}
		public String    getDesc()              { return _desc; }
		public Timestamp getSessionSampleTime() { return _sessionSampleTime; }
		public Timestamp getCmSampleTime()      { return _cmSampleTime; }
		public int       getCmSampleMs()        { return _cmSampleMs; }
		public String    getType()              { return _type; }

		public void setName             (String name)                 { _name = name; }
		public void setDesc             (String desc)                 { _desc = desc; }
		public void setSessionSampleTime(Timestamp sessionSampleTime) { _sessionSampleTime = sessionSampleTime; }
		public void setCmSampleTime     (Timestamp cmSampleTime)      { _cmSampleTime = cmSampleTime; }
		public void setCmSampleMs       (int cmSampleMs)              { _cmSampleMs = cmSampleMs; }
		public void setType             (String type)                 { _type = type; }

		public List<MetaDataEntry>     getMetaData()     { return _metaData; }
		public CounterEntry            getAbsCounters()  { return _absCounters; }
		public CounterEntry            getDiffCounters() { return _diffCounters; }
		public CounterEntry            getRateCounters() { return _rateCounters; }
		public Map<String, GraphEntry> getGraphMap()     { return _graphMap; }

		public CmEntry(String cmName, Timestamp sessionSampleTime, Timestamp cmSampleTime, int cmSampleMs, String type)
		{
			_name              = cmName;
			_sessionSampleTime = sessionSampleTime;
			_cmSampleTime      = cmSampleTime;
			_cmSampleMs        = cmSampleMs;
			_type              = type;
		}
		public void addMetaData(MetaDataEntry md) { _metaData.add(md); }
		public void addAbsRow () { _absCounters .addRow(); }
		public void addDiffRow() { _diffCounters.addRow(); }
		public void addRateRow() { _rateCounters.addRow(); }
		public void addAbsRecord (String field, Object value) { _absCounters .add(field, value); }
		public void addDiffRecord(String field, Object value) { _diffCounters.add(field, value); }
		public void addRateRecord(String field, Object value) { _rateCounters.add(field, value); }
		public void addGraph(GraphEntry graphEntry) { _graphMap.put(graphEntry._name, graphEntry); }

		public void setStatGraphCount(int val)                          { _statGraphCount                       = val; }
		public void setStatAbsRowCount(int val)                         { _statAbsRowCount                      = val; }
		public void setStatDiffRowCount(int val)                        { _statDiffRowCount                     = val; }
		public void setStatRateRowCount(int val)                        { _statRateRowCount                     = val; }
		public void setSqlRefreshTime(int val)                          { _sqlRefreshTime                       = val; }
		public void setGuiRefreshTime(int val)                          { _guiRefreshTime                       = val; }
		public void setLcRefreshTime(int val)                           { _lcRefreshTime                        = val; }
		public void setHasNonConfiguredMonitoringHappened(boolean val)  { _hasNonConfiguredMonitoringHappened   = val; }
		public void setNonConfiguredMonitoringMissingParams(String val) { _nonConfiguredMonitoringMissingParams = val; }
		public void setNonConfiguredMonitoringMessage(String val)       { _nonConfiguredMonitoringMessage       = val; }
		public void setIsCountersCleared(boolean val)                   { _isCountersCleared                    = val; }
		public void setHasValidSampleData(boolean val)                  { _hasValidSampleData                   = val; }
		public void setSampleExceptionMsg(String val)                   { _sampleExceptionMsg                   = val; }
		public void setSampleExceptionFullText(String val)              { _sampleExceptionFullText              = val; }
		
		public int     getStatGraphCount()                       { return _statGraphCount                      ; }
		public int     getStatAbsRowCount()                      { return _statAbsRowCount                     ; }
		public int     getStatDiffRowCount()                     { return _statDiffRowCount                    ; }
		public int     getStatRateRowCount()                     { return _statRateRowCount                    ; }
		public int     getSqlRefreshTime()                       { return _sqlRefreshTime                      ; }
		public int     getGuiRefreshTime()                       { return _guiRefreshTime                      ; }
		public int     getLcRefreshTime()                        { return _lcRefreshTime                       ; }
		public boolean hasNonConfiguredMonitoringHappened()      { return _hasNonConfiguredMonitoringHappened  ; }
		public String  getNonConfiguredMonitoringMissingParams() { return _nonConfiguredMonitoringMissingParams; }
		public String  getNonConfiguredMonitoringMessage()       { return _nonConfiguredMonitoringMessage      ; }
		public boolean isCountersCleared()                       { return _isCountersCleared                   ; }
		public boolean hasValidSampleData()                      { return _hasValidSampleData                  ; }
		public String  getSampleExceptionMsg()                   { return _sampleExceptionMsg                  ; }
		public String  getSampleExceptionFullText()              { return _sampleExceptionFullText             ; }

		public boolean hasAbsData()     { return _absCounters  != null && _absCounters .getRowCount() > 0; }
		public boolean hasDiffData()    { return _diffCounters != null && _diffCounters.getRowCount() > 0; }
		public boolean hasRateData()    { return _rateCounters != null && _rateCounters.getRowCount() > 0; }
		public boolean hasTrendGraphs() { return _graphMap     != null && _graphMap    .size()        > 0; }

		public void toJsonForGraph(JsonGenerator w, List<String> graphNameList) 
		throws IOException
		{
			if (! hasTrendGraphs())
				return;
			
			// TODO: do not write graphs that isn't in the passed graphList  
			
			w.writeStartObject();

			w.writeStringField("cmName",            getName());
			w.writeStringField("sessionSampleTime", TimeUtils.toStringIso8601(getSessionSampleTime()));
			w.writeStringField("cmSampleTime",      TimeUtils.toStringIso8601(_cmSampleTime));
			w.writeNumberField("cmSampleMs",        _cmSampleMs);
			w.writeStringField("type",              _type);

			w.writeFieldName("graphs");
			w.writeStartArray(); 
			for (String graphName : _graphMap.keySet())
			{
				GraphEntry ge = _graphMap.get(graphName);

				w.writeStartObject();  // BEGIN: Graph
				w.writeStringField ("cmName",            getName());
				w.writeStringField ("sessionSampleTime", TimeUtils.toStringIso8601(getSessionSampleTime()));
				w.writeStringField ("graphName" ,        ge.getName());
				w.writeStringField ("graphLabel",        ge.getGraphLabel());
				w.writeStringField ("graphProps",        ge.getGraphProps());
				w.writeStringField ("graphCategory",     ge.getGraphCategory());
				w.writeBooleanField("percentGraph",      ge.isPercentGraph());
				w.writeBooleanField("visibleAtStart",    ge.isVisibleAtStart());

				w.writeFieldName("data");
//				w.writeStartArray(); 
				w.writeStartObject(); // BEGIN: data

				// loop all data
				for (String label : ge._labelValue.keySet())
				{
					Double data = ge._labelValue.get(label);
					
//					w.writeStartObject();
//					w.writeStringField("label",     label);
//					w.writeNumberField("dataPoint", data);
//					w.writeEndObject();
					w.writeNumberField(label, data);
				}
				
//				w.writeEndArray(); 
				w.writeEndObject(); // END: data
				w.writeEndObject(); // END: Graph
			}
			w.writeEndArray(); 

			w.writeEndObject(); // END: this CM
		}
	}
	
	public static class CounterEntry
	{
		int _type;
//		List<Map<String, Object>> _records;

		List<String>       _colnames = new ArrayList<>();
		List<List<Object>> _rows     = new ArrayList<>();
		List<Object>       _lastRow = null;
		
		public CounterEntry(int type)
		{
			_type = type;
		}
		public void addRow()
		{
			_lastRow = new ArrayList<Object>();
			_rows.add( _lastRow );
		}
		public void add(String field, Object value)
		{
			int colPos = _colnames.indexOf(field);
			if (colPos == -1)
			{
				colPos = _colnames.size();
				_colnames.add(field);
			}
			// If not big enough... make it bigger
			while (_lastRow.size() <= colPos)
				_lastRow.add(null);

			_lastRow.set(colPos, value);
		}
		public int getRowCount()
		{
			return _rows.size();
		}
	}

	public static class GraphEntry
	{
		String       _name; 
		String       _graphLabel; 
		String       _graphProps; 
		String       _graphCategory; 
		boolean      _isPercentGraph; 
		boolean      _visibleAtStart; 
		String       _originJsonStr;
		Map<String, Double> _labelValue = new LinkedHashMap<>();

		public String  getName()          { return _name; }
		public String  getGraphLabel()    { return _graphLabel; }
		public String  getGraphProps()    { return _graphProps; }
		public String  getGraphCategory() { return _graphCategory; }
		public boolean isPercentGraph()   { return _isPercentGraph; }
		public boolean isVisibleAtStart() { return _visibleAtStart; }

		public String getOriginJsonStr()                     { return _originJsonStr; }
		public void   setOriginJsonStr(String originJsonStr) { _originJsonStr = originJsonStr; }
		
		public GraphEntry(String name, String graphLabel, String graphProps, String graphCategory, boolean isPercentGraph, boolean visibleAtStart)
		{
			_name           = name;
			_graphLabel     = graphLabel;
			_graphProps     = graphProps;
			_graphCategory  = graphCategory;
			_isPercentGraph = isPercentGraph;
			_visibleAtStart = visibleAtStart;
		}

		public void add(String label, double value)
		{
			if (StringUtil.hasValue(label))
				_labelValue.put(label, value);
			else
				System.out.println("Graph '"+_name+"', Adding a label that seems to be empty. Skipping this. DEBUG: Origin JSON Text: "+_originJsonStr);
		}

		public boolean hasData()
		{
			return _labelValue.size() > 0;
		}
	}

	public static class AlarmEntry
	{
		String    _alarmClass                 ;
		String    _alarmClassAbriviated       ;
		String    _serviceType                ;
		String    _serviceName                ;
		String    _serviceInfo                ;
		String    _extraInfo                  ;
		String    _category                   ;
		String    _severity                   ;
		String    _state                      ;
		int       _repeatCnt                  ;
		String    _duration                   ;
		int       _creationAgeInMs            ;
		Timestamp _creationTime               ;
		Timestamp _reRaiseTime                ;
		Timestamp _cancelTime                 ;
		int       _TimeToLive                 ;
		Number    _threshold                  ;
		String    _data                       ;
		String    _description                ;
		String    _extendedDescription        ;
		String    _reRaiseData                ;
		String    _reRaiseDescription         ;
		String    _reRaiseExtendedDescription ;

		public String    getAlarmClass()                 { return _alarmClass; }
		public String    getAlarmClassAbriviated()       { return _alarmClassAbriviated; }
		public String    getServiceType()                { return _serviceType; }
		public String    getServiceName()                { return _serviceName; }
		public String    getServiceInfo()                { return _serviceInfo; }
		public String    getExtraInfo()                  { return _extraInfo; }
		public String    getCategory()                   { return _category; }
		public String    getSeverity()                   { return _severity; }
		public String    getState()                      { return _state; }
		public int       getRepeatCnt()                  { return _repeatCnt; }
		public String    getDuration()                   { return _duration; }
		public int       getCreationAgeInMs()            { return _creationAgeInMs; }
		public Timestamp getCreationTime()               { return _creationTime; }
		public Timestamp getReRaiseTime()                { return _reRaiseTime; }
		public Timestamp getCancelTime()                 { return _cancelTime; }
		public int       getTimeToLive()                 { return _TimeToLive; }
		public Number    getThreshold()                  { return _threshold; }
		public String    getData()                       { return _data; }
		public String    getDescription()                { return _description; }
		public String    getExtendedDescription()        { return _extendedDescription; }
		public String    getReRaiseData()                { return _reRaiseData; }
		public String    getReRaiseDescription()         { return _reRaiseDescription; }
		public String    getReRaiseExtendedDescription() { return _reRaiseExtendedDescription; }

		public void setAlarmClass                (String    alarmClass)                { _alarmClass                 = alarmClass; }
		public void setAlarmClassAbriviated      (String    alarmClassAbriviated)      { _alarmClassAbriviated       = alarmClassAbriviated; }
		public void setServiceType               (String    serviceType)               { _serviceType                = serviceType; }
		public void setServiceName               (String    serviceName)               { _serviceName                = serviceName; }
		public void setServiceInfo               (String    serviceInfo)               { _serviceInfo                = serviceInfo; }
		public void setExtraInfo                 (String    extraInfo)                 { _extraInfo                  = extraInfo; }
		public void setCategory                  (String    category)                  { _category                   = category; }
		public void setSeverity                  (String    severity)                  { _severity                   = severity; }
		public void setState                     (String    state)                     { _state                      = state; }
		public void setRepeatCnt                 (int       repeatCnt)                 { _repeatCnt                  = repeatCnt; }
		public void setDuration                  (String    duration)                  { _duration                   = duration; }
		public void setCreationAgeInMs           (int       creationAgeInMs)           { _creationAgeInMs            = creationAgeInMs; }
		public void setCreationTime              (Timestamp creationTime)              { _creationTime               = creationTime; }
		public void setReRaiseTime               (Timestamp reRaiseTime)               { _reRaiseTime                = reRaiseTime; }
		public void setCancelTime                (Timestamp cancelTime)                { _cancelTime                 = cancelTime; }
		public void setTimeToLive                (int       timeToLive)                { _TimeToLive                 = timeToLive; }
		public void setThreshold                 (Number    threshold)                 { _threshold                  = threshold; }
		public void setData                      (String    data)                      { _data                       = data; }
		public void setDescription               (String    description)               { _description                = description; }
		public void setExtendedDescription       (String    extendedDescription)       { _extendedDescription        = extendedDescription; }
		public void setReRaiseData               (String    reRaiseData)               { _reRaiseData                = reRaiseData; }
		public void setReRaiseDescription        (String    reRaiseDescription)        { _reRaiseDescription         = reRaiseDescription; }
		public void setReRaiseExtendedDescription(String    reRaiseExtendedDescription){ _reRaiseExtendedDescription = reRaiseExtendedDescription; }
		
		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();

			sb.append(super.toString());
			sb.append("[");

			sb.append("alarmClass                ".trim()).append("='").append(_alarmClass                ).append("'").append(","); // String    _alarmClass                 ;
			sb.append("alarmClassAbriviated      ".trim()).append("='").append(_alarmClassAbriviated      ).append("'").append(","); // String    _alarmClassAbriviated       ;
			sb.append("serviceType               ".trim()).append("='").append(_serviceType               ).append("'").append(","); // String    _serviceType                ;
			sb.append("serviceName               ".trim()).append("='").append(_serviceName               ).append("'").append(","); // String    _serviceName                ;
			sb.append("serviceInfo               ".trim()).append("='").append(_serviceInfo               ).append("'").append(","); // String    _serviceInfo                ;
			sb.append("extraInfo                 ".trim()).append("='").append(_extraInfo                 ).append("'").append(","); // String    _extraInfo                  ;
			sb.append("category                  ".trim()).append("='").append(_category                  ).append("'").append(","); // String    _category                   ;
			sb.append("severity                  ".trim()).append("='").append(_severity                  ).append("'").append(","); // String    _severity                   ;
			sb.append("state                     ".trim()).append("='").append(_state                     ).append("'").append(","); // String    _state                      ;
			sb.append("repeatCnt                 ".trim()).append("=") .append(_repeatCnt                 ).append("") .append(","); // int       _repeatCnt                  ;
			sb.append("duration                  ".trim()).append("='").append(_duration                  ).append("'").append(","); // String    _duration                   ;
			sb.append("creationAgeInMs           ".trim()).append("=") .append(_creationAgeInMs           ).append("") .append(","); // int       _creationAgeInMs            ;
			sb.append("creationTime              ".trim()).append("='").append(_creationTime              ).append("'").append(","); // Timestamp _creationTime               ;
			sb.append("reRaiseTime               ".trim()).append("='").append(_reRaiseTime               ).append("'").append(","); // Timestamp _reRaiseTime                ;
			sb.append("cancelTime                ".trim()).append("='").append(_cancelTime                ).append("'").append(","); // Timestamp _cancelTime                 ;
			sb.append("TimeToLive                ".trim()).append("=") .append(_TimeToLive                ).append("") .append(","); // int       _TimeToLive                 ;
			sb.append("threshold                 ".trim()).append("=") .append(_threshold                 ).append("") .append(","); // Number    _threshold                  ;
			sb.append("data                      ".trim()).append("='").append(_data                      ).append("'").append(","); // String    _data                       ;
			sb.append("description               ".trim()).append("='").append(_description               ).append("'").append(","); // String    _description                ;
			sb.append("extendedDescription       ".trim()).append("='").append(_extendedDescription       ).append("'").append(","); // String    _extendedDescription        ;
			sb.append("reRaiseData               ".trim()).append("='").append(_reRaiseData               ).append("'").append(","); // String    _reRaiseData                ;
			sb.append("reRaiseDescription        ".trim()).append("='").append(_reRaiseDescription        ).append("'").append(","); // String    _reRaiseDescription         ;
			sb.append("reRaiseExtendedDescription".trim()).append("='").append(_reRaiseExtendedDescription).append("'").append("");  // String    _reRaiseExtendedDescription ;

			sb.append("]");
			
			return sb.toString();
		}
		
	}

	public static class AlarmEntryWrapper
	{
		Timestamp  _eventTime ;
		String     _action    ;
		AlarmEntry _alarmEntry;

		public Timestamp  getEventTime()   { return _eventTime;  }
		public String     getAction()      { return _action;     }
		public AlarmEntry getAlarmEntry()  { return _alarmEntry; }

		public void setEventTime (Timestamp  eventTime)  { _eventTime  = eventTime;  }
		public void setAction    (String     action)     { _action     = action;     }
		public void setAlarmEntry(AlarmEntry alarmEntry) { _alarmEntry = alarmEntry; }
	}
	
	
	
	
	/**
	 * Get Node from a node 
	 * @param node               The node
	 * @param fieldName          The fields name
	 * @return                   A String
	 * @throws ServletException  When the field name is not found
	 */
	private static JsonNode getNode(JsonNode node, String fieldName)
	throws ServletException
	{
		JsonNode n = node.get(fieldName);
		if (n == null)
			throw new ServletException("Expecting field '"+fieldName+"' which was not found, this can't be a valid DbxTune PCS Content.");
		return n;
	}

	/**
	 * Get String from a node 
	 * @param node           The node
	 * @param fieldName      The fields name
	 * @param defaultValue   Default value if field is not found
	 * @return               A String
	 */
	private static String getString(JsonNode node, String fieldName, String defaultValue)
	{
		JsonNode n = node.get(fieldName);
		if (n == null)
			return defaultValue;
		if (n.isNull())
			return null;
		return n.asText();
	}

	/**
	 * Get String from a node 
	 * @param node               The node
	 * @param fieldName          The fields name
	 * @return                   A String
	 * @throws ServletException  When the field name is not found
	 */
	private static String getString(JsonNode node, String fieldName)
	throws ServletException
	{
		JsonNode n = node.get(fieldName);
		if (n == null)
			throw new ServletException("Expecting field '"+fieldName+"' which was not found, this can't be a valid DbxTune PCS Content.");
		if (n.isNull())
			return null;
		return n.asText();
	}

	/**
	 * Get int value from a node
	 * @param node               The node
	 * @param fieldName          The fields name
	 * @param defaultValue       Default value if field is not found
	 * @return                   an int
	 */
	private static int getInt(JsonNode node, String fieldName, int defaultValue)
	throws ServletException
	{
		JsonNode n = node.get(fieldName);
		if (n == null)
			return defaultValue;
		return n.asInt();
	}

	/**
	 * Get int value from a node
	 * @param node               The node
	 * @param fieldName          The fields name
	 * @return                   an int
	 * @throws ServletException  When the field name is not found
	 */
	private static int getInt(JsonNode node, String fieldName)
	throws ServletException
	{
		JsonNode n = node.get(fieldName);
		if (n == null)
			throw new ServletException("Expecting field '"+fieldName+"' which was not found, this can't be a valid DbxTune PCS Content.");
		return n.asInt();
	}

	/**
	 * Get boolean value from a node
	 * @param node               The node
	 * @param fieldName          The fields name
	 * @param defaultValue       Default value if field is not found
	 * @return                   an boolean value
	 */
	private static boolean getBoolean(JsonNode node, String fieldName, boolean defaultValue)
	throws ServletException
	{
		JsonNode n = node.get(fieldName);
		if (n == null)
			return defaultValue;
		return n.asBoolean();
	}

	/**
	 * Get boolean value from a node
	 * @param node               The node
	 * @param fieldName          The fields name
	 * @throws ServletException  When the field name is not found
	 * @return                   an boolean value
	 */
	private static boolean getBoolean(JsonNode node, String fieldName)
	throws ServletException
	{
		JsonNode n = node.get(fieldName);
		if (n == null)
			throw new ServletException("Expecting field '"+fieldName+"' which was not found, this can't be a valid DbxTune PCS Content.");
		return n.asBoolean();
	}

	/**
	 * Get double value from a node
	 * @param node               The node
	 * @param fieldName          The fields name
	 * @param defaultValue       Default value if field is not found
	 * @return                   an double value
	 */
	private static double getDouble(JsonNode node, String fieldName, double defaultValue)
	throws ServletException
	{
		JsonNode n = node.get(fieldName);
		if (n == null)
			return defaultValue;
		return n.asDouble();
	}

	/**
	 * Get double value from a node
	 * @param node               The node
	 * @param fieldName          The fields name
	 * @throws ServletException  When the field name is not found
	 * @return                   an double value
	 */
	private static double getDouble(JsonNode node, String fieldName)
	throws ServletException
	{
		JsonNode n = node.get(fieldName);
		if (n == null)
			throw new ServletException("Expecting field '"+fieldName+"' which was not found, this can't be a valid DbxTune PCS Content.");
		return n.asDouble();
	}

	/**
	 * Get Timestamp value from a node
	 * @param node               The node
	 * @param fieldName          The fields name
	 * @param defaultValue       Default value if field is not found
	 * @return                   an Timestamp value
	 */
	private static Timestamp getTimestamp(JsonNode node, String fieldName, Timestamp defaultValue)
	throws ServletException
	{
		try
		{
			return getTimestamp(node, fieldName);
		}
		catch (Exception e) 
		{
			return defaultValue;
		}
	}

	/**
	 * Get Timestamp value from a node
	 * @param node               The node
	 * @param fieldName          The fields name
	 * @throws ServletException  When the field name is not found
	 * @return                   an Timestamp value
	 */
	private static Timestamp getTimestamp(JsonNode node, String fieldName)
	throws ServletException
	{
		JsonNode n = node.get(fieldName);
		if (n == null)
			throw new ServletException("Expecting field '"+fieldName+"' which was not found, this can't be a valid DbxTune PCS Content.");
		
		if (n.isNull())
			return null;
		
		String str = n.asText();
		Timestamp ts = null;
		Exception ex = null;

		// if String(iso-8601), then convert it into a Timestamp 
		try 
		{ 
			ts = TimeUtils.parseToTimestampIso8601(str); 
			return ts;
		}
		catch (ParseException pe) 
		{ 
			ex = pe;
		}

		// if String(yyyy-MM-dd hh:mm:ss.SSS), then convert it into a Timestamp 
		try 
		{ 
			ts = TimeUtils.parseToTimestamp(str); 
			return ts;
		}
		catch (ParseException pe) 
		{ 
			ex = pe;
		}

		// if Long value, then convert it into a Timestamp
		try 
		{ 
			long time = Long.parseLong(str);
			return new Timestamp(time);
		}
		catch (NumberFormatException nfe) 
		{ 
			ex = nfe;
		}
		

		throw new ServletException("Problems parsing the Timestamp for field '"+fieldName+"' using value '"+str+"'. Caught: "+ex, ex); 
	}

	/**
	 * create an Object based ofn what type the desiered field is of
	 * @param node               The node
	 */
	private static Object createObjectFromNodeType(JsonNode node)
	{
		if (node == null)
			return null;

		if (node.isTextual()) return node.asText();
		if (node.isInt())     return node.asInt();
		if (node.isBoolean()) return node.asBoolean();

		if (node.isLong())       return node.asLong();
		if (node.isDouble())     return node.asDouble();
		if (node.isShort())      return new Short(node.asText());
		if (node.isFloat())      return new Float(node.asText());
		if (node.isBigDecimal()) return new BigDecimal(node.asText());
		if (node.isBigInteger()) return new BigInteger(node.asText());
//		if (node.isBinary())     return node.as);

		return null;
	}
	
	/**
	 * Parse a JSON string into this object
	 * @param json
	 * @return
	 * @throws IOException 
	 * @throws JsonProcessingException 
	 */
	public static DbxTuneSample parseJson(String json) 
	throws JsonProcessingException, IOException, ServletException
	{
		if (_logger.isDebugEnabled())
		{
			_logger.debug("");
			_logger.debug("#######################################################################################################");
		}

		ObjectMapper mapper = new ObjectMapper();
//		try
//		{
			JsonNode root = mapper.readTree(json);
			
			JsonNode headNode = getNode(root, "head");
			int    messageVersion          = getInt   (headNode, "messageVersion");
			String appName                 = getString(headNode, "appName");
			String appVersion              = getString(headNode, "appVersion");
			String appBuildString          = getString(headNode, "appBuildString");
			String collectorHostname       = getString(headNode, "collectorHostname");
			int    collectorSampleInterval = getInt   (headNode, "collectorSampleInterval", -1);
			String collectorCurrentUrl     = getString(headNode, "collectorCurrentUrl", null);
			String collectorInfoFile       = getString(headNode, "collectorInfoFile",   null);
			
			Timestamp sessionStartTime     = getTimestamp(headNode, "sessionStartTime");
			Timestamp sessionSampleTime    = getTimestamp(headNode, "sessionSampleTime");
			String    serverName           = getString   (headNode, "serverName");
			String    onHostname           = getString   (headNode, "onHostname");
			String    serverNameAlias      = getString   (headNode, "serverNameAlias", null);

			String serverNameOrAlias = serverName;
			// Override the SERVERNAME if "serverNameAlias" is set
			if (StringUtil.hasValue(serverNameAlias))
				serverNameOrAlias = serverNameAlias;
			
			// skip serverName "unknown" 
			if ("unknown".equals(serverName))
			{
				String headerInfo = "appName='"+appName+"', appVersion='"+appVersion+"', appBuildString='"+appBuildString+"', collectorHostname='"+collectorHostname+"', collectorSampleInterval="+collectorSampleInterval+", collectorCurrentUrl='"+collectorCurrentUrl+"', collectorInfoFile='"+collectorInfoFile+"', sessionStartTime='"+sessionStartTime+"', sessionSampleTime='"+sessionSampleTime+"', serverName='"+serverName+"', onHostname='"+onHostname+"', serverNameAlias='"+serverNameAlias+"'.";
				_logger.warn("Recieved a JSON massage with serverName='unknown'. Skipping this entry. headerInfo: "+headerInfo);
				return null;
			}

			DbxTuneSample sample = new DbxTuneSample(appName, appVersion, appBuildString, collectorHostname, collectorSampleInterval, collectorCurrentUrl, collectorInfoFile, sessionStartTime, sessionSampleTime, serverNameOrAlias, onHostname);

			if (_logger.isDebugEnabled())
				_logger.debug("sessionStartTime='"+sessionStartTime+"', sessionSampleTime='"+sessionSampleTime+"', serverName='"+serverName+"', serverNameAlias='"+serverNameAlias+"', onHostname='"+onHostname+"'.");

			// ACTIVE ALARMS
			JsonNode activeAlarmsNode = root.get("activeAlarms");
			if (activeAlarmsNode != null)
			{
				for (JsonNode alarm : activeAlarmsNode)
				{
					AlarmEntry ae = new AlarmEntry();

					ae.setAlarmClass                ( getString   (alarm, "alarmClass"));
					ae.setAlarmClassAbriviated      ( getString   (alarm, "alarmClassAbriviated"));
					ae.setServiceType               ( getString   (alarm, "serviceType"));
					ae.setServiceName               ( getString   (alarm, "serviceName"));
					ae.setServiceInfo               ( getString   (alarm, "serviceInfo"));
					ae.setExtraInfo                 ( getString   (alarm, "extraInfo"));
					ae.setCategory                  ( getString   (alarm, "category"));
					ae.setSeverity                  ( getString   (alarm, "severity"));
					ae.setState                     ( getString   (alarm, "state"));
					ae.setRepeatCnt                 ( getInt      (alarm, "repeatCnt"));
					ae.setDuration                  ( getString   (alarm, "duration"));
					ae.setCreationAgeInMs           ( getInt      (alarm, "creationAgeInMs"));
					ae.setCreationTime              ( getTimestamp(alarm, "creationTimeIso8601")); 
					ae.setReRaiseTime               ( getTimestamp(alarm, "reRaiseTimeIso8601")); 
					ae.setCancelTime                ( getTimestamp(alarm, "cancelTimeIso8601"));
					ae.setTimeToLive                ( getInt      (alarm, "TimeToLive"));
					ae.setThreshold                 ( (Number) createObjectFromNodeType(alarm.get("threshold")));
					ae.setData                      ( getString   (alarm, "data"));
					ae.setDescription               ( getString   (alarm, "description"));
					ae.setExtendedDescription       ( getString   (alarm, "extendedDescription"));
					ae.setReRaiseData               ( getString   (alarm, "reRaiseData"));
					ae.setReRaiseDescription        ( getString   (alarm, "reRaiseDescription"));
					ae.setReRaiseExtendedDescription( getString   (alarm, "reRaiseExtendedDescription"));
					
					sample.addActiveAlarm(ae);
				}
			}
			if (_logger.isDebugEnabled())
				_logger.debug(" - activeAlarms.size='"+sample._activeAlarms.size()+"'.");

			// ALARMS EVENTS
			JsonNode alarmEventsNode = root.get("alarmEvents");
			if (alarmEventsNode != null)
			{
				for (JsonNode alarm : alarmEventsNode)
				{
					AlarmEntry ae = new AlarmEntry();
					AlarmEntryWrapper aew = new AlarmEntryWrapper();

					ae.setAlarmClass                ( getString   (alarm, "alarmClass"));
					ae.setAlarmClassAbriviated      ( getString   (alarm, "alarmClassAbriviated"));
					ae.setServiceType               ( getString   (alarm, "serviceType"));
					ae.setServiceName               ( getString   (alarm, "serviceName"));
					ae.setServiceInfo               ( getString   (alarm, "serviceInfo"));
					ae.setExtraInfo                 ( getString   (alarm, "extraInfo"));
					ae.setCategory                  ( getString   (alarm, "category"));
					ae.setSeverity                  ( getString   (alarm, "severity"));
					ae.setState                     ( getString   (alarm, "state"));
					ae.setRepeatCnt                 ( getInt      (alarm, "repeatCnt"));
					ae.setDuration                  ( getString   (alarm, "duration"));
					ae.setCreationAgeInMs           ( getInt      (alarm, "creationAgeInMs"));
					ae.setCreationTime              ( getTimestamp(alarm, "creationTimeIso8601")); 
					ae.setReRaiseTime               ( getTimestamp(alarm, "reRaiseTimeIso8601")); 
					ae.setCancelTime                ( getTimestamp(alarm, "cancelTimeIso8601"));
					ae.setTimeToLive                ( getInt      (alarm, "TimeToLive"));
					ae.setThreshold                 ( (Number) createObjectFromNodeType(alarm.get("threshold")));
					ae.setData                      ( getString   (alarm, "data"));
					ae.setDescription               ( getString   (alarm, "description"));
					ae.setExtendedDescription       ( getString   (alarm, "extendedDescription"));
					ae.setReRaiseData               ( getString   (alarm, "reRaiseData"));
					ae.setReRaiseDescription        ( getString   (alarm, "reRaiseDescription"));
					ae.setReRaiseExtendedDescription( getString   (alarm, "reRaiseExtendedDescription"));

					aew.setEventTime(getTimestamp(alarm, "eventTime"));
					aew.setAction   (getString   (alarm, "action"));
					aew.setAlarmEntry(ae);

					sample.addAlarmEntry(aew);
				}
			}
			if (_logger.isDebugEnabled())
				_logger.debug(" - alarmEvents.size='"+sample._alarmEvents.size()+"'.");

			// COLLECTORS
			JsonNode collectorsNode = getNode(root, "collectors");
			if (_logger.isDebugEnabled())
				_logger.debug(" - collectorsNode.count='"+collectorsNode.size()+"'.");
			for (JsonNode collector : collectorsNode)
			{
				String    cmName       = getString   (collector, "cmName");
				Timestamp cmSampleTime = getTimestamp(collector, "cmSampleTime");
				int       cmSampleMs   = getInt      (collector, "cmSampleMs");
				String    type         = getString   (collector, "type");
				
				CmEntry cmEntry = new CmEntry(cmName, sessionSampleTime, cmSampleTime, cmSampleMs, type);
				sample.add(cmEntry);

				// sampleDetails
				JsonNode sampleDetailsNode = collector.get("sampleDetails"); // NOTE: sampleDetails is an Object
				if (sampleDetailsNode != null)
				{
					cmEntry.setStatGraphCount(                       getInt    (sampleDetailsNode, "graphCount", 0));
					cmEntry.setStatAbsRowCount(                      getInt    (sampleDetailsNode, "absRows",    0));
					cmEntry.setStatDiffRowCount(                     getInt    (sampleDetailsNode, "diffRows",   0));
					cmEntry.setStatRateRowCount(                     getInt    (sampleDetailsNode, "rateRows",   0));
					
					cmEntry.setSqlRefreshTime(                       getInt    (sampleDetailsNode, "sqlRefreshTime",                       0));
					cmEntry.setGuiRefreshTime(                       getInt    (sampleDetailsNode, "guiRefreshTime",                       0));
					cmEntry.setLcRefreshTime (                       getInt    (sampleDetailsNode, "lcRefreshTime",                        0));
					cmEntry.setHasNonConfiguredMonitoringHappened(   getBoolean(sampleDetailsNode, "hasNonConfiguredMonitoringHappened",   false));
					cmEntry.setNonConfiguredMonitoringMissingParams( getString (sampleDetailsNode, "nonConfiguredMonitoringMissingParams", ""));
					cmEntry.setNonConfiguredMonitoringMessage(       getString (sampleDetailsNode, "nonConfiguredMonitoringMessage",       ""));
					cmEntry.setIsCountersCleared(                    getBoolean(sampleDetailsNode, "isCountersCleared",                    false));
					cmEntry.setHasValidSampleData(                   getBoolean(sampleDetailsNode, "hasValidSampleData",                   false));
					cmEntry.setSampleExceptionMsg(                   getString (sampleDetailsNode, "exceptionMsg",                         ""));
					cmEntry.setSampleExceptionFullText(              getString (sampleDetailsNode, "exceptionFullText",                    ""));
				}

//System.out.println("-------------------------------------------------------------------------------------------------------");
//System.out.println(" - CmName='"+CmName+"', CmSampleTime='"+CmSampleTime+"', CmSampleMs='"+CmSampleMs+"', Type='"+Type+"'.");

				JsonNode countersNode = collector.get("counters"); // NOTE: Counters is an Object
				if (countersNode != null)
				{
//System.out.println(" - COUNTERS: Counters.size='"+Counters.size()+"', Counters.getNodeType()="+Counters.getNodeType()+".");
					JsonNode metaDataNode = countersNode.get("metaData");
					if (metaDataNode != null)
					{
//System.out.println(" - - COUNTERS: CmName='"+CmName+"', MetaData.count='"+MetaData.size()+"'.");
						for (JsonNode metaData : metaDataNode)
						{
							String  columnName       = getString (metaData, "columnName");
							String  jdbcTypeName     = getString (metaData, "jdbcTypeName");
							String  javaClassName    = getString (metaData, "javaClassName");
							String  guessedDbmsType  = getString (metaData, "guessedDbmsType");
							boolean isDiffColumn     = getBoolean(metaData, "isDiffColumn");
							boolean isPctColumn      = getBoolean(metaData, "isPctColumn");
							
							MetaDataEntry mde = new MetaDataEntry(columnName, jdbcTypeName, javaClassName, guessedDbmsType, isDiffColumn, isPctColumn);
							cmEntry.addMetaData(mde);
						}
					}
					
					JsonNode absCountersNode = countersNode.get("absCounters");
					if (absCountersNode != null)
					{
//System.out.println(" - - COUNTERS: CmName='"+CmName+"', AbsCounters.count='"+AbsCounters.size()+"'.");
						for (JsonNode absCounters : absCountersNode)
						{
							cmEntry.addAbsRow();
							
							Iterator<Map.Entry<String, JsonNode>> fields = absCounters.fields();
							while (fields.hasNext()) 
							{
								Map.Entry<String, JsonNode> entry = fields.next();
								cmEntry.addAbsRecord(
										entry.getKey(), 
										createObjectFromNodeType( entry.getValue() )
										);
							}
						}
					}

					JsonNode diffCountersNode = countersNode.get("diffCounters");
					if (diffCountersNode != null)
					{
//System.out.println(" - - COUNTERS: CmName='"+CmName+"', DiffCounters.count='"+DiffCounters.size()+"'.");
						for (JsonNode diffCounters : diffCountersNode)
						{
							cmEntry.addDiffRow();
							
							Iterator<Map.Entry<String, JsonNode>> fields = diffCounters.fields();
							while (fields.hasNext()) 
							{
								Map.Entry<String, JsonNode> entry = fields.next();
								cmEntry.addDiffRecord(
										entry.getKey(), 
										createObjectFromNodeType( entry.getValue() )
										);
							}
						}
					}

					JsonNode rateCountersNode = countersNode.get("rateCounters");
					if (rateCountersNode != null)
					{
//System.out.println(" - - COUNTERS: CmName='"+CmName+"', RateCounters.count='"+RateCounters.size()+"'.");
						for (JsonNode rateCounters : rateCountersNode)
						{
							cmEntry.addRateRow();
							
							Iterator<Map.Entry<String, JsonNode>> fields = rateCounters.fields();
							while (fields.hasNext()) 
							{
								Map.Entry<String, JsonNode> entry = fields.next();
								cmEntry.addRateRecord(
										entry.getKey(), 
										createObjectFromNodeType( entry.getValue() )
										);
							}
						}
					}
				} // end: Counters

				JsonNode graphsNode = collector.get("graphs");
				if (graphsNode != null)
				{
//					System.out.println(" - Graphs.count='"+Graphs.size()+"'.");
//fireGraphData("graph: "+ new Timestamp(System.currentTimeMillis()) + " - - CmName='"+CmName+"', Graphs.count='"+Graphs.size()+"'.");
//fireGraphData("{\"graph\": \""+ new Timestamp(System.currentTimeMillis()) + " - - CmName='"+CmName+"', Graphs.count='"+Graphs.size()+"'.\"}");
					for (JsonNode graphs : graphsNode)
					{
						String  graphName      = getString (graphs, "graphName");
						String  graphLabel     = getString (graphs, "graphLabel", "unknown graph label");
						String  graphProps     = getString (graphs, "graphProps");
						String  graphCategory  = getString (graphs, "graphCategory", "unknown graph category");
						boolean isPercentGraph = getBoolean(graphs, "percentGraph",   false);
						boolean visibleAtStart = getBoolean(graphs, "visibleAtStart", false);

						GraphEntry graphEntry = new GraphEntry(graphName, graphLabel, graphProps, graphCategory, isPercentGraph, visibleAtStart);
						cmEntry.addGraph(graphEntry);

						// Add the full JSON just for debugging
						graphEntry.setOriginJsonStr(graphs.toString());

//						System.out.println("XXX: GraphsName='"+GraphName+"'. JSON="+graphs);
						
						JsonNode dataEntry = graphs.get("data");
//						System.out.println(" - - CmName='"+CmName+"', GraphName='"+GraphName+"', Data='"+Data+"'.");
						if (dataEntry != null)
						{
//							for (JsonNode data : dataEntry)
//							{
//								String  label     = getString(data, "label");
//								double  dataPoint = getDouble(data, "dataPoint");
//								
//								graphEntry.add(label, dataPoint);
//								
////								System.out.println(" - - CmName='"+CmName+"', GraphName='"+GraphName+"', Label='"+Label+"', DataPoint='"+DataPoint+"'.");
//							}
							Iterator<Map.Entry<String, JsonNode>> fields = dataEntry.fields();
							while (fields.hasNext()) 
							{
								Map.Entry<String, JsonNode> entry = fields.next();

								String  label     = entry.getKey();
								double  dataPoint = entry.getValue().asDouble();
								
								graphEntry.add(label, dataPoint);
							}
						}
					}
				}
			}
			
			
//			System.out.println("### The Sample has: "+sample.getCollectors().size()+" collector entries. sessionStartTime="+sample.getSessionStartTime()+", sessionSampleTime="+sample.getSessionSampleTime()+", serverName="+sample.getServerName()+", onHostname="+sample.getOnHostname()+".");
//			for (CmEntry cmEntry : sample.getCollectors())
//			{
//				System.out.println("    -- The CmEntry '"+cmEntry.getName()+"' has: "
//						+ cmEntry.getGraphMap()    .size()      +" Graph entries, "
//						+ cmEntry.getAbsCounters() .getRowCount()+" Abs rows, "
//						+ cmEntry.getDiffCounters().getRowCount()+" Diff rows, "
//						+ cmEntry.getRateCounters().getRowCount()+" Rate rows, "
//						);
//				for (GraphEntry graphEntry : cmEntry.getGraphMap().values())
//				{
//					if ("aaCpuGraph".equals(graphEntry.getName()))
//						fireGraphData(graphEntry.getOriginJsonStr());
//				}
//			}
//			
//			
//			// Finally add the data to the Writer, which will store the data "somewhere" probably in a DB
//			CentralPcsWriterHandler writer = CentralPcsWriterHandler.getInstance();
//			writer.add(sample);

//		}
//		catch(JsonProcessingException ex) { ex.printStackTrace(); }
//		catch(IOException ex) { ex.printStackTrace(); }
			
		return sample;
	}

	/**
	 * Null save save way to do obj.toString()
	 *  
	 * @param obj The object
	 * @return obj.toString() or if (null if the inout is null
	 */
	private String toString(Object obj)
	{
		if (obj == null)
			return null;

		if (obj instanceof Timestamp)
		{
			return TimeUtils.toStringIso8601((Timestamp)obj);
		}
		return obj.toString(); 
	}

	/**
	 * Null save save way to do obj.toString()
	 *  
	 * @param obj The object
	 * @return obj.toString() or if (null if the inout is null
	 */
	private BigDecimal toBigDec(Number num)
	{
		return num == null ? null : new BigDecimal(num.toString()); 
	}

	public String getJsonForGraphs(List<String> graphNameList)
	throws IOException
	{
		StringWriter sw = new StringWriter();

		JsonFactory jfactory = new JsonFactory();
		JsonGenerator w = jfactory.createGenerator(sw);
		w.setPrettyPrinter(new DefaultPrettyPrinter());
		

		w.writeStartObject();
		
		w.writeFieldName("head");
		w.writeStartObject();
			w.writeStringField("appName"          , Version.getAppName());
			w.writeStringField("appVersion"       , Version.getVersionStr());
			w.writeStringField("appBuildString"   , Version.getBuildStr());

			w.writeStringField("sessionStartTime" , getSessionStartTime()  +"");
			w.writeStringField("sessionSampleTime", getSessionSampleTime() +"");
			w.writeStringField("serverName"       , getServerName());
			w.writeStringField("onHostname"       , getOnHostname());
//			w.writeStringField("serverNameAlias"  , getServerNameAlias());
		w.writeEndObject();

		
		//--------------------------------------
		// Active Alarms
		//--------------------------------------
		if ( ! _activeAlarms.isEmpty() )
		{
			w.writeFieldName("activeAlarms");

			w.writeStartArray();
			for (AlarmEntry ae : _activeAlarms)
			{
				w.writeStartObject();

				w.writeStringField("alarmClass"                 , toString( ae.getAlarmClass()                 ));
				w.writeStringField("alarmClassAbriviated"       , toString( ae.getAlarmClassAbriviated()       ));
				w.writeStringField("serviceType"                , toString( ae.getServiceType()                ));
				w.writeStringField("serviceName"                , toString( ae.getServiceName()                ));
				w.writeStringField("serviceInfo"                , toString( ae.getServiceInfo()                ));
				w.writeStringField("extraInfo"                  , toString( ae.getExtraInfo()                  ));
				w.writeStringField("category"                   , toString( ae.getCategory()                   ));
				w.writeStringField("severity"                   , toString( ae.getSeverity()                   ));
				w.writeStringField("state"                      , toString( ae.getState()                      ));
				w.writeNumberField("repeatCnt"                  ,           ae.getRepeatCnt()                   );
				w.writeStringField("duration"                   , toString( ae.getDuration()                   ));
				w.writeNumberField("creationAgeInMs"            ,           ae.getCreationAgeInMs()             );
//				w.writeNumberField("creationTime"               ,           ae.getCreationTime()                );
//				w.writeStringField("creationTimeIso8601"        , toString( ae.getCreationTime()               )); 
				w.writeStringField("creationTime"               , toString( ae.getCreationTime()               )); 
				w.writeStringField("reRaiseTime"                , toString( ae.getReRaiseTime()                )); 
//				w.writeNumberField("cancelTime"                 ,           ae.getCancelTime()                  );
//				w.writeStringField("cancelTimeIso8601"          , toString( ae.getCancelTime()                 ));
				w.writeStringField("cancelTime"                 , toString( ae.getCancelTime()                 ));
				w.writeNumberField("TimeToLive"                 ,           ae.getTimeToLive()                  );
				w.writeNumberField("threshold"                  , toBigDec( ae.getThreshold()                  ));
				w.writeStringField("data"                       , toString( ae.getData()                       ));
				w.writeStringField("description"                , toString( ae.getDescription()                ));
				w.writeStringField("extendedDescription"        , toString( ae.getExtendedDescription()        ));
				w.writeStringField("reRaiseData"                , toString( ae.getReRaiseData()                ));
				w.writeStringField("reRaiseDescription"         , toString( ae.getReRaiseDescription()         ));
				w.writeStringField("reRaiseExtendedDescription" , toString( ae.getReRaiseExtendedDescription() ));
				
				w.writeEndObject();
			}

			w.writeEndArray();
		}

		
		
		w.writeFieldName("collectors");
		w.writeStartArray();

		//--------------------------------------
		// COUNTERS
		//--------------------------------------
		for (CmEntry cme : _collectors)
		{
			cme.toJsonForGraph(w, graphNameList);
		}

		w.writeEndArray();
		w.writeEndObject();
		w.close();
		
		String jsonStr = sw.toString();
		return jsonStr;
	}

	/**
	 * check if the containe contains anything...
	 * @return
	 */
	public boolean isEmpty()
	{
		return getCollectors().isEmpty() && getActiveAlarms().isEmpty() && getAlarmEntries().isEmpty();
	}
	
}
