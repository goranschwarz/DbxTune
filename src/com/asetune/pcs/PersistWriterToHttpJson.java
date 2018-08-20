package com.asetune.pcs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.CounterController;
import com.asetune.DbxTune;
import com.asetune.Version;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.writers.AlarmWriterToPcsJdbc.AlarmEventWrapper;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSettingsHelper.Type;
import com.asetune.cm.CmSettingsHelper.UrlInputValidator;
import com.asetune.cm.CountersModel;
import com.asetune.cm.CountersModelAppend;
import com.asetune.pcs.sqlcapture.SqlCaptureDetails;
import com.asetune.utils.Configuration;
import com.asetune.utils.HttpUtils;
import com.asetune.utils.Memory;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

public class PersistWriterToHttpJson 
extends PersistWriterBase
//implements IPersistWriter
{
	private static Logger _logger = Logger.getLogger(PersistWriterToHttpJson.class);

	/** Everytime the message structure is changed this should be increments so that any receiver knows what fields to expect */
	public static final int MESSAGE_VERSION = 1;

	/** Some statistics for this Writer */
	private PersistWriterStatisticsRest _writerStatistics = new PersistWriterStatisticsRest();
	
	@Override 
	public PersistWriterStatisticsRest getStatistics()
	{
		if (_writerStatistics == null)
			_writerStatistics = new PersistWriterStatisticsRest();

		return _writerStatistics;
	}

	@Override
	public void resetCounters()
	{
		if (_writerStatistics != null)
			_writerStatistics.clear();
	}
	

	@Override
	public void close()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public Configuration getConfig()
	{
		return _conf;
	}

	@Override
	public String getConfigStr()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void saveSample(PersistContainer cont)
	{
		String jsonStr = "";
		
		// Send to the mandatory config
		try
		{
			// First send old entries in the error queue (no exception is thrown)
			_confSlot0.sendErrorQueue();

			// Constrct and send JSON
			jsonStr = toJson(cont, _confSlot0._sendCounters, _confSlot0._sendGraphs);
			sendMessage(jsonStr, _confSlot0);
		}
		catch (ConnectException ex) 
		{
			_logger.error("Problems connecting/sending JSON-REST call to '"+_confSlot0._url+"'. The entry will be saved in the 'error-queue' and sent later. Caught: "+ex);
			_confSlot0.addToErrorQueue(jsonStr);
		}
		catch (Exception ex)
		{
			_logger.error("Problems creating JSON or sending REST call to '"+_confSlot0._url+"'. Caught: "+ex , ex);
		}

		// Loop over the "extra" configurations and send to *each* destination
		for (ConfigSlot slot : _confSlots)
		{
			try
			{
				// First send old entries in the error queue (no exception is thrown)
				slot.sendErrorQueue();
				
				// Constrct and send JSON
				jsonStr = toJson(cont, slot._sendCounters, slot._sendGraphs);
				sendMessage(jsonStr, slot);
			}
			catch (ConnectException ex) 
			{
				_logger.error("Problems connecting/sending JSON-REST call to '"+slot._url+"'. The entry will be saved in the 'error-queue' and sent later. Caught: "+ex);
				slot.addToErrorQueue(jsonStr);
			}
			catch (Exception ex)
			{
				_logger.error("Problems creating JSON or sending REST call to '"+slot._url+"'. Caught: "+ex , ex);
			}
		}
	}

//	private String toJsonGSON(PersistContainer cont, boolean writeCounters, boolean writeGraphs)
//	throws IOException
//	{
//		StringWriter sw = new StringWriter();
//		JsonWriter w = new JsonWriter(sw);
//		w.setIndent("    ");
//
//		System.out.println();
//		System.out.println("#### BEGIN JSON #######################################################################");
//		System.out.println("getSessionStartTime = " + cont.getSessionStartTime());
//		System.out.println("getMainSampleTime   = " + cont.getMainSampleTime());
//		System.out.println("getServerName       = " + cont.getServerName());
//		System.out.println("getOnHostname       = " + cont.getOnHostname());
//
//		w.beginObject();
//		
//		w.name("sessionStartTime") .value(cont.getSessionStartTime() +"");
//		w.name("sessionSampleTime").value(cont.getMainSampleTime()   +"");
//		w.name("serverName")       .value(cont.getServerName());
//		w.name("onHostname")       .value(cont.getOnHostname());
//
//		w.name("appName")          .value(Version.getAppName());
//		w.name("appVersion")       .value(Version.getVersionStr());
//		w.name("appBuildString")   .value(Version.getBuildStr());
//
//		w.name("collectors");
//		w.beginArray();
//
//		//--------------------------------------
//		// COUNTERS
//		//--------------------------------------
//		for (CountersModel cm : cont._counterObjects)
//		{
//			cm.toJson(w, writeCounters, writeGraphs);
//		}
//
//		w.endArray();
//		w.endObject();
//		w.close();
//		
////			System.out.println(sw.toString());
//		File toFileName = new File("c:\\tmp\\PersistWriterToHttpJson.tmp.json");
//		System.out.println("Writing JSON to file: "+toFileName.getAbsolutePath());
//
//		String jsonStr = sw.toString();
//		FileUtils.writeStringToFile(toFileName, jsonStr);
//		
//		System.out.println("#### END JSON #######################################################################");
//
//		return jsonStr;
//	}

	/**
	 * Null save save way to do obj.toString()
	 *  
	 * @param obj The object
	 * @return obj.toString() or if (null if the inout is null
	 */
	private String toString(Object obj)
	{
		return obj == null ? null : obj.toString(); 
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


	/**
	 * Wrapper around toJsonWork...<br>
	 * In here we catch OutOfMemory errors, tries to do cleanup, and try again...
	 * 
	 * @param cont
	 * @param writeCounters
	 * @param writeGraphs
	 * @return
	 * @throws IOException
	 */
	private String toJson(PersistContainer cont, boolean writeCounters, boolean writeGraphs)
	throws IOException
	{
		try
		{
			long startTime = System.currentTimeMillis();

			String json = toJsonMessage(cont, writeCounters, writeGraphs);

			long createTime = TimeUtils.msDiffNow(startTime);
			getStatistics().setLastCreateJsonTimeInMs(createTime);

			return json;
		}
		catch (IOException ex)
		{
			throw ex;
		}
		catch (OutOfMemoryError oom)
		{
			_logger.warn("Caught OutOfMemoryError when generating JSON text. Call the Memory.evaluate(), which should do cleanup of registered components. Then we try to generate the JSON Text again.");

			// Do memory cleanup
			Memory.evaluateAndWait(10*1000); // wait for max 10 sec
			
			// Try again
			return toJsonMessage(cont, writeCounters, writeGraphs);
		}
	}
	
	/**
	 * Write JSON for the container
	 * 
	 * @param cont           The container
	 * @param writeCounters  if we should write COUNTER information
	 * @param writeGraphs    if we should write GRAPH/CHART information
	 * @return A JSON String
	 * @throws IOException 
	 */
	private String toJsonMessage(PersistContainer cont, boolean writeCounters, boolean writeGraphs)
	throws IOException
	{
		StringWriter sw = new StringWriter();

		JsonFactory jfactory = new JsonFactory();
		JsonGenerator w = jfactory.createGenerator(sw);
		w.setPrettyPrinter(new DefaultPrettyPrinter());
		
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

		// Get the current recording database from the INFO Configuration object ( which is written in PersistWriterJdbc.startServices() )
		String collectorCurrentUrl = null;
		String collectorInfoFile = null;
		Configuration conf = Configuration.getInstance(DbxTune.DBXTUNE_NOGUI_INFO_CONFIG);
		if (conf != null)
		{
			collectorCurrentUrl = conf.getProperty("pcs.h2.jdbc.url");
			collectorInfoFile   = conf.getFilename();
		}

//		System.out.println();
//		System.out.println("#### BEGIN JSON #######################################################################");
//		System.out.println("getSessionStartTime = " + cont.getSessionStartTime());
//		System.out.println("getMainSampleTime   = " + cont.getMainSampleTime());
//		System.out.println("getServerName       = " + cont.getServerName());
//		System.out.println("getOnHostname       = " + cont.getOnHostname());

		w.writeStartObject();
		
		w.writeFieldName("head");
		w.writeStartObject();
			w.writeNumberField("messageVersion"         , MESSAGE_VERSION);
			w.writeStringField("appName"                , Version.getAppName());
			w.writeStringField("appVersion"             , Version.getVersionStr());
			w.writeStringField("appBuildString"         , Version.getBuildStr());
			w.writeStringField("collectorHostname"      , collectorHostname);
			w.writeNumberField("collectorSampleInterval", collectorSampleInterval);
			w.writeStringField("collectorCurrentUrl"    , collectorCurrentUrl);
			w.writeStringField("collectorInfoFile"      , collectorInfoFile);

			w.writeStringField("sessionStartTime"       , TimeUtils.toStringIso8601(cont.getSessionStartTime()));
			w.writeStringField("sessionSampleTime"      , TimeUtils.toStringIso8601(cont.getMainSampleTime()));
			w.writeStringField("serverName"             , cont.getServerName());
			w.writeStringField("onHostname"             , cont.getOnHostname());
			w.writeStringField("serverNameAlias"        , cont.getServerNameAlias());
		w.writeEndObject();

		// Write ACTIVE the alarms
		List<AlarmEvent> alarmList = cont.getAlarmList();
		if (alarmList != null && !alarmList.isEmpty())
		{
			w.writeFieldName("activeAlarms");
			w.writeStartArray();
			for (AlarmEvent ae : alarmList)
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
					w.writeNumberField("repeatCnt"                  ,           ae.getReRaiseCount()                );
					w.writeStringField("duration"                   , toString( ae.getDuration()                   ));
					w.writeNumberField("creationAgeInMs"            ,           ae.getCrAgeInMs()                   );
					w.writeNumberField("creationTime"               ,           ae.getCrTime()                      );
					w.writeStringField("creationTimeIso8601"        , toString( ae.getCrTimeIso8601()              )); 
					w.writeNumberField("reRaiseTime"                ,           ae.getReRaiseTime()                 );
					w.writeStringField("reRaiseTimeIso8601"         , toString( ae.getReRaiseTimeIso8601()         ));
					w.writeNumberField("cancelTime"                 ,           ae.getCancelTime()                  );
					w.writeStringField("cancelTimeIso8601"          , toString( ae.getCancelTimeIso8601()          ));
					w.writeNumberField("TimeToLive"                 ,           ae.getTimeToLive()                  );
					w.writeNumberField("threshold"                  , toBigDec( ae.getCrossedThreshold()           ));
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

		// Write HISTORICAL the alarms (things that has happened during last sample: RAISE/RE-RAISE/CANCEL)
		List<AlarmEventWrapper> alarmEvents = cont.getAlarmEvents();
		if (alarmEvents != null && !alarmEvents.isEmpty())
		{
			w.writeFieldName("alarmEvents");
			w.writeStartArray();
			for (AlarmEventWrapper aew : alarmEvents)
			{
				w.writeStartObject();
					w.writeStringField("eventTime"                  , TimeUtils.toStringIso8601(aew.getAddDate()   ));
					w.writeStringField("action"                     , toString( aew.getAction()                    ));
					
					AlarmEvent ae = aew.getAlarmEvent();
					
					w.writeStringField("alarmClass"                 , toString( ae.getAlarmClass()                 ));
					w.writeStringField("alarmClassAbriviated"       , toString( ae.getAlarmClassAbriviated()       ));
					w.writeStringField("serviceType"                , toString( ae.getServiceType()                ));
					w.writeStringField("serviceName"                , toString( ae.getServiceName()                ));
					w.writeStringField("serviceInfo"                , toString( ae.getServiceInfo()                ));
					w.writeStringField("extraInfo"                  , toString( ae.getExtraInfo()                  ));
					w.writeStringField("category"                   , toString( ae.getCategory()                   ));
					w.writeStringField("severity"                   , toString( ae.getSeverity()                   ));
					w.writeStringField("state"                      , toString( ae.getState()                      ));
					w.writeNumberField("repeatCnt"                  ,           ae.getReRaiseCount()                );
					w.writeStringField("duration"                   , toString( ae.getDuration()                   ));
					w.writeNumberField("creationAgeInMs"            ,           ae.getCrAgeInMs()                   );
					w.writeNumberField("creationTime"               ,           ae.getCrTime()                      );
					w.writeStringField("creationTimeIso8601"        , toString( ae.getCrTimeIso8601()              )); 
					w.writeNumberField("reRaiseTime"                ,           ae.getReRaiseTime()                 );
					w.writeStringField("reRaiseTimeIso8601"         , toString( ae.getReRaiseTimeIso8601()         ));
					w.writeNumberField("cancelTime"                 ,           ae.getCancelTime()                  );
					w.writeStringField("cancelTimeIso8601"          , toString( ae.getCancelTimeIso8601()          ));
					w.writeNumberField("TimeToLive"                 ,           ae.getTimeToLive()                  );
					w.writeNumberField("threshold"                  , toBigDec( ae.getCrossedThreshold()           ));
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
		for (CountersModel cm : cont._counterObjects)
		{
			// For the moment do not send Append Models
			if (cm instanceof CountersModelAppend) 
				continue;
			
			if ( ! cm.hasValidSampleData() )
				continue;

			if ( ! cm.hasData() )
				continue;

			cm.toJson(w, writeCounters, writeGraphs);
		}

		w.writeEndArray();
		w.writeEndObject();
		w.close();
		
////			System.out.println(sw.toString());
//		File toFileName = new File("c:\\tmp\\PersistWriterToHttpJson.tmp.json");
//System.out.println("Writing JSON to file: "+toFileName.getAbsolutePath());
//
		String jsonStr = sw.toString();
//		FileUtils.writeStringToFile(toFileName, jsonStr);
		
//		System.out.println("#### END JSON #######################################################################");

		return jsonStr;
	}

	/**
	 * Here is where the send happens
	 * @param action
	 * @param alarmEvent
	 * @throws Exception 
	 */
	private void sendMessage(String text, ConfigSlot slot) throws Exception
	{
		URL url = new URL(slot._url);

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
//		conn.setRequestProperty("Content-Type", "application/json");
// Moved this to: _header_1

		// add headers
		addHeader(conn, slot._header_1);
		addHeader(conn, slot._header_2);
		addHeader(conn, slot._header_3);
		addHeader(conn, slot._header_4);
		addHeader(conn, slot._header_5);
		addHeader(conn, slot._header_6);
		addHeader(conn, slot._header_7);
		addHeader(conn, slot._header_8);
		addHeader(conn, slot._header_9);

		// Write the output
		long startTime = System.currentTimeMillis();
		OutputStream os = conn.getOutputStream();
		os.write(text.getBytes());
		os.flush();
		long sendTime = TimeUtils.msDiffNow(startTime);
		
		getStatistics().setLastSendTimeInMs(sendTime);

		// Check responce
		int responceCode = conn.getResponseCode();
		if ( responceCode >= 203) // see 'https://httpstatuses.com/' for http codes... or at the bottom of this source code
		{
			throw new Exception("Failed : HTTP error code : " + responceCode + " ("+HttpUtils.httpResponceCodeToText(responceCode)+"). From URL '"+slot._url+"'. Sent JSON content.length: "+text.length());
		}
		else
		{
			int threshold = 1000;
			if (sendTime > threshold)
				_logger.warn("HTTP REST Call took longer than expected. sendTimeInMs = "+sendTime+", which is above threshold="+threshold+", Responce code "+responceCode+" ("+HttpUtils.httpResponceCodeToText(responceCode)+"). From URL '"+slot._url+"'. Sent JSON content.length: "+text.length()+", "+(text.length()/1024)+" KB");

			_logger.debug("Responce code "+responceCode+" ("+HttpUtils.httpResponceCodeToText(responceCode)+"). From URL '"+slot._url+"'. Sent JSON content.length: "+text.length()+", "+(text.length()/1024)+" KB");
		}

		// Read responce and print the output...
		BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

		String output;
		while ((output = br.readLine()) != null)
		{
			_logger.info("Responce from server: " + output);
			_logger.debug("Responce from server: " + output);
		}

		conn.disconnect();
	}
	/** 
	 * take the keyVal <code>"Accept: application/json"</code><br> and parse into <code>key="Accept", val="application/json"</code> 
	 * @throws exception if it can't find any ':' char in the keyVal string 
	 */
	private void addHeader(HttpURLConnection conn, String keyVal)
	throws Exception
	{
		if (StringUtil.isNullOrBlank(keyVal))
			return;

		int firstColonPos = keyVal.indexOf(':');
		if (firstColonPos == -1)
			throw new Exception("Problem parsing the value '"+keyVal+"', can't find any ':' in it.");
		
		String key = keyVal.substring(0, firstColonPos);
		String val = keyVal.substring(firstColonPos+1).trim();

		conn.addRequestProperty(key, val);
	}

	@Override
	public void saveSqlCaptureDetails(SqlCaptureDetails sqlCaptureDetails)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void startServices() throws Exception
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void stopServices(int maxWaitTimeInMs)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public String getName()
	{
		return this.getClass().getSimpleName();
	}

	public void printConfig()
	{
		int spaces = 35;
		_logger.info("Configuration for Persist Writer Module: "+getName());
		_logger.info("    " + StringUtil.left(key(PROPKEY_url           ), spaces) + ": " + _confSlot0._url);

		_logger.info("    " + StringUtil.left(key(PROPKEY_errorQueueSize), spaces) + ": " + _confSlot0._errorQueueSize);

		_logger.info("    " + StringUtil.left(key(PROPKEY_sendCounters  ), spaces) + ": " + _confSlot0._sendCounters);
		_logger.info("    " + StringUtil.left(key(PROPKEY_sendGraphs    ), spaces) + ": " + _confSlot0._sendGraphs);
		
		if (StringUtil.hasValue(_confSlot0._header_1)) _logger.info("    " + StringUtil.left(key(PROPKEY_header1     ), spaces) + ": " + _confSlot0._header_1);
		if (StringUtil.hasValue(_confSlot0._header_2)) _logger.info("    " + StringUtil.left(key(PROPKEY_header2     ), spaces) + ": " + _confSlot0._header_2);
		if (StringUtil.hasValue(_confSlot0._header_3)) _logger.info("    " + StringUtil.left(key(PROPKEY_header3     ), spaces) + ": " + _confSlot0._header_3);
		if (StringUtil.hasValue(_confSlot0._header_4)) _logger.info("    " + StringUtil.left(key(PROPKEY_header4     ), spaces) + ": " + _confSlot0._header_4);
		if (StringUtil.hasValue(_confSlot0._header_5)) _logger.info("    " + StringUtil.left(key(PROPKEY_header5     ), spaces) + ": " + _confSlot0._header_5);
		if (StringUtil.hasValue(_confSlot0._header_6)) _logger.info("    " + StringUtil.left(key(PROPKEY_header6     ), spaces) + ": " + _confSlot0._header_6);
		if (StringUtil.hasValue(_confSlot0._header_7)) _logger.info("    " + StringUtil.left(key(PROPKEY_header7     ), spaces) + ": " + _confSlot0._header_7);
		if (StringUtil.hasValue(_confSlot0._header_8)) _logger.info("    " + StringUtil.left(key(PROPKEY_header8     ), spaces) + ": " + _confSlot0._header_8);
		if (StringUtil.hasValue(_confSlot0._header_9)) _logger.info("    " + StringUtil.left(key(PROPKEY_header9     ), spaces) + ": " + _confSlot0._header_9);
		
		for (ConfigSlot slot : _confSlots)
		{
			if (StringUtil.hasValue(slot._url     )) _logger.info("    " + StringUtil.left(key(PROPKEY_url            , slot._cfgName), spaces) + ": " + slot._url);

			if (StringUtil.hasValue(slot._url     )) _logger.info("    " + StringUtil.left(key(PROPKEY_errorQueueSize , slot._cfgName), spaces) + ": " + slot._errorQueueSize);

			if (StringUtil.hasValue(slot._url     )) _logger.info("    " + StringUtil.left(key(PROPKEY_sendCounters   , slot._cfgName), spaces) + ": " + slot._sendCounters);
			if (StringUtil.hasValue(slot._url     )) _logger.info("    " + StringUtil.left(key(PROPKEY_sendGraphs     , slot._cfgName), spaces) + ": " + slot._sendGraphs);
                                                                                                                                            
			if (StringUtil.hasValue(slot._header_1)) _logger.info("    " + StringUtil.left(key(PROPKEY_header1        , slot._cfgName), spaces) + ": " + slot._header_1);
			if (StringUtil.hasValue(slot._header_2)) _logger.info("    " + StringUtil.left(key(PROPKEY_header2        , slot._cfgName), spaces) + ": " + slot._header_2);
			if (StringUtil.hasValue(slot._header_3)) _logger.info("    " + StringUtil.left(key(PROPKEY_header3        , slot._cfgName), spaces) + ": " + slot._header_3);
			if (StringUtil.hasValue(slot._header_4)) _logger.info("    " + StringUtil.left(key(PROPKEY_header4        , slot._cfgName), spaces) + ": " + slot._header_4);
			if (StringUtil.hasValue(slot._header_5)) _logger.info("    " + StringUtil.left(key(PROPKEY_header5        , slot._cfgName), spaces) + ": " + slot._header_5);
			if (StringUtil.hasValue(slot._header_6)) _logger.info("    " + StringUtil.left(key(PROPKEY_header6        , slot._cfgName), spaces) + ": " + slot._header_6);
			if (StringUtil.hasValue(slot._header_7)) _logger.info("    " + StringUtil.left(key(PROPKEY_header7        , slot._cfgName), spaces) + ": " + slot._header_7);
			if (StringUtil.hasValue(slot._header_8)) _logger.info("    " + StringUtil.left(key(PROPKEY_header8        , slot._cfgName), spaces) + ": " + slot._header_8);
			if (StringUtil.hasValue(slot._header_9)) _logger.info("    " + StringUtil.left(key(PROPKEY_header9        , slot._cfgName), spaces) + ": " + slot._header_9);
		}
	}

	@Override
	public void init(Configuration conf) throws Exception
	{
		System.out.println(getName()+": INIT.....................................");

		_conf = conf;

		_logger.info("Initializing the PersistWriter component named '"+getName()+"'.");

		_confSlot0._url            = conf.getProperty       (key(PROPKEY_url           ), DEFAULT_url);
		_confSlot0._cfgName        = "slot0";
		if (_confSlot0._cfgName.endsWith("/api/pcs/receiver"))
			_confSlot0._cfgName    = "dbxCentral";

		_confSlot0._errorQueueSize = conf.getIntProperty    (key(PROPKEY_errorQueueSize), DEFAULT_errorQueueSize);

		_confSlot0._sendCounters   = conf.getBooleanProperty(key(PROPKEY_sendCounters  ), DEFAULT_sendCounters);
		_confSlot0._sendGraphs     = conf.getBooleanProperty(key(PROPKEY_sendGraphs    ), DEFAULT_sendGraphs);
                                                                                       
		_confSlot0._header_1       = conf.getProperty       (key(PROPKEY_header1       ), DEFAULT_header1);
		_confSlot0._header_2       = conf.getProperty       (key(PROPKEY_header2       ), DEFAULT_header2);
		_confSlot0._header_3       = conf.getProperty       (key(PROPKEY_header3       ), DEFAULT_header3);
		_confSlot0._header_4       = conf.getProperty       (key(PROPKEY_header4       ), DEFAULT_header4);
		_confSlot0._header_5       = conf.getProperty       (key(PROPKEY_header5       ), DEFAULT_header5);
		_confSlot0._header_6       = conf.getProperty       (key(PROPKEY_header6       ), DEFAULT_header6);
		_confSlot0._header_7       = conf.getProperty       (key(PROPKEY_header7       ), DEFAULT_header7);
		_confSlot0._header_8       = conf.getProperty       (key(PROPKEY_header8       ), DEFAULT_header8);
		_confSlot0._header_9       = conf.getProperty       (key(PROPKEY_header9       ), DEFAULT_header9);

		// Read "extra" configurations
		String keyStr = conf.getProperty(PROPKEY_configKeys, DEFAULT_configKeys);
		List<String> keyList = StringUtil.commaStrToList(keyStr);
		
		for (String cfgKey : keyList)
		{
			String url             = conf.getProperty       (key(PROPKEY_url           , cfgKey), null);
                                   
			int    errorQueueSize  = conf.getIntProperty    (key(PROPKEY_errorQueueSize, cfgKey), DEFAULT_errorQueueSize);

			boolean sendCounters   = conf.getBooleanProperty(key(PROPKEY_sendCounters  , cfgKey), DEFAULT_sendCounters);
			boolean sendGraphs     = conf.getBooleanProperty(key(PROPKEY_sendGraphs    , cfgKey), DEFAULT_sendGraphs  );
			                                                                           
			String header_1        = conf.getProperty       (key(PROPKEY_header1       , cfgKey), null);
			String header_2        = conf.getProperty       (key(PROPKEY_header2       , cfgKey), null);
			String header_3        = conf.getProperty       (key(PROPKEY_header3       , cfgKey), null);
			String header_4        = conf.getProperty       (key(PROPKEY_header4       , cfgKey), null);
			String header_5        = conf.getProperty       (key(PROPKEY_header5       , cfgKey), null);
			String header_6        = conf.getProperty       (key(PROPKEY_header6       , cfgKey), null);
			String header_7        = conf.getProperty       (key(PROPKEY_header7       , cfgKey), null);
			String header_8        = conf.getProperty       (key(PROPKEY_header8       , cfgKey), null);
			String header_9        = conf.getProperty       (key(PROPKEY_header9       , cfgKey), null);
			
			if (StringUtil.isNullOrBlank(url))
			{
				_logger.warn("When getting configuration for config '"+cfgKey+"' using property '"+key(PROPKEY_url, cfgKey)+"' no value for URL was found. Skipping this section.");
			}
			else
			{
				// Add a SLOT to the list
				ConfigSlot slot = new ConfigSlot();

				slot._cfgName        = cfgKey;
				slot._url            = url;

				slot._errorQueueSize = errorQueueSize;

				slot._sendCounters   = sendCounters;
				slot._sendGraphs     = sendGraphs;
				
				slot._header_1       = header_1;
				slot._header_2       = header_2;
				slot._header_3       = header_3;
				slot._header_4       = header_4;
				slot._header_5       = header_5;
				slot._header_6       = header_6;
				slot._header_7       = header_7;
				slot._header_8       = header_8;
				slot._header_9       = header_9;

				_confSlots.add(slot);
			}
		}

		
		//------------------------------------------
		// Check for mandatory parameters
		//------------------------------------------
		if ( StringUtil.isNullOrBlank(_confSlot0._url) )          throw new Exception("The property '" + PROPKEY_url          + "' is mandatory for the PersistWriter named '"+getName()+"'.");


		//------------------------------------------
		// Check for valid configuration
		//------------------------------------------
		// Check if the URL seems to be OK...
		String cfgName = "";
		String testUrl = "";
		try 
		{ 
			cfgName = "base";
			testUrl = _confSlot0._url;
			new URL(_confSlot0._url); 

			for (ConfigSlot slot : _confSlots)
			{
				cfgName = slot._cfgName;
				testUrl = slot._url;
				new URL(slot._url); 
			}
		}
		catch(MalformedURLException ex) 
		{ 
			throw new Exception("The URL '"+testUrl+"' for config '"+cfgName+"' seems to be malformed. Caught: "+ex, ex); 
		}
		
		printConfig();
	}

	public static final String  PROPKEY_configKeys       = "PersistWriterToHttpJson.config.keys";
	public static final String  DEFAULT_configKeys       = null;

	public static final String  PROPKEY_url              = "PersistWriterToHttpJson.{KEY}.url";
//	public static final String  DEFAULT_url              = null;
	public static final String  DEFAULT_url              = "http://localhost:8080/api/pcs/receiver";

	public static final String  PROPKEY_errorQueueSize   = "PersistWriterToHttpJson.{KEY}.error.queue.size";
	public static final int     DEFAULT_errorQueueSize   = 10;

	public static final String  PROPKEY_sendCounters     = "PersistWriterToHttpJson.{KEY}.send.counters";
	public static final boolean DEFAULT_sendCounters     = false;

	public static final String  PROPKEY_sendGraphs       = "PersistWriterToHttpJson.{KEY}.send.graphs";
	public static final boolean DEFAULT_sendGraphs       = true;

	public static final String  PROPKEY_header1          = "PersistWriterToHttpJson.{KEY}.header.1";
//	public static final String  DEFAULT_header1          = null;
//	public static final String  DEFAULT_header1          = "Content-Type: application/json";
	public static final String  DEFAULT_header1          = "Content-Type: text/plain";

	public static final String  PROPKEY_header2          = "PersistWriterToHttpJson.{KEY}.header.2";
	public static final String  DEFAULT_header2          = null;

	public static final String  PROPKEY_header3          = "PersistWriterToHttpJson.{KEY}.header.3";
	public static final String  DEFAULT_header3          = null;

	public static final String  PROPKEY_header4          = "PersistWriterToHttpJson.{KEY}.header.4";
	public static final String  DEFAULT_header4          = null;

	public static final String  PROPKEY_header5          = "PersistWriterToHttpJson.{KEY}.header.5";
	public static final String  DEFAULT_header5          = null;

	public static final String  PROPKEY_header6          = "PersistWriterToHttpJson.{KEY}.header.6";
	public static final String  DEFAULT_header6          = null;

	public static final String  PROPKEY_header7          = "PersistWriterToHttpJson.{KEY}.header.7";
	public static final String  DEFAULT_header7          = null;

	public static final String  PROPKEY_header8          = "PersistWriterToHttpJson.{KEY}.header.8";
	public static final String  DEFAULT_header8          = null;

	public static final String  PROPKEY_header9          = "PersistWriterToHttpJson.{KEY}.header.9";
	public static final String  DEFAULT_header9          = null;

	//-------------------------------------------------------
	// class members
	//-------------------------------------------------------
	private Configuration _conf = null;

//	private String  _url      = "";
//	
//	private String  _header_1 = "";
//	private String  _header_2 = "";
//	private String  _header_3 = "";
//	private String  _header_4 = "";
//	private String  _header_5 = "";
//	private String  _header_6 = "";
//	private String  _header_7 = "";
//	private String  _header_8 = "";
//	private String  _header_9 = "";

	private      ConfigSlot  _confSlot0 = new ConfigSlot();
	private List<ConfigSlot> _confSlots = new ArrayList<>();

	private class ConfigSlot
	{
		String  _cfgName  = "";
		String  _url      = "";

		int     _errorQueueSize = DEFAULT_errorQueueSize;
		LinkedList<String> _errorQueue = new LinkedList<>(); 

		boolean _sendCounters   = DEFAULT_sendCounters;
		boolean _sendGraphs     = DEFAULT_sendGraphs;
		
		String  _header_1 = "";
		String  _header_2 = "";
		String  _header_3 = "";
		String  _header_4 = "";
		String  _header_5 = "";
		String  _header_6 = "";
		String  _header_7 = "";
		String  _header_8 = "";
		String  _header_9 = "";

		/** 
		 * Add message to error queue, to be sent later 
		 */
		public void addToErrorQueue(String jsonStr)
		{
			if (StringUtil.isNullOrBlank(jsonStr))
				return;

			_errorQueue.addLast(jsonStr);
			
			while (_errorQueue.size() > _errorQueueSize)
			{
				_errorQueue.removeFirst();
				_logger.info("Removing 'oldest' entry in the ErrorQueue for config name '"+_cfgName+"'. _errorQueue.size()="+_errorQueue.size()+", maxEntries="+_errorQueueSize);
			}
		}

		/** 
		 * send old entries in the errorQueue, if the queue is empty, simply do nothing.
		 * @return number of entries sent
		 */
		public int sendErrorQueue()
		{
			int sendCount = 0;
			if (_errorQueue.isEmpty())
				return sendCount;
			
			while ( ! _errorQueue.isEmpty() )
			{
				try
				{
					// Get message
					String jsonStr = _errorQueue.getFirst();

					_logger.info("Resending 'oldest' entry in the ErrorQueue for config name '"+_cfgName+"'. _errorQueue.size()="+_errorQueue.size());
					
					// send message (if we have problems an exception will be thrown)
					sendMessage(jsonStr, this);
					
					// on success: remove the queue entry
					_errorQueue.removeFirst();
					sendCount++;
				}
				catch (ConnectException ex) 
				{
					// log WITHOUT stacktrace
					_logger.info("Resending PROBLEMS for 'oldest' entry in the ErrorQueue for config name '"+_cfgName+"'. _errorQueue.size()="+_errorQueue.size()+". to '"+_url+"'. It will be kept in the queue... Caught: "+ex);
					break;
				}
				catch (Exception ex) 
				{
					// log with STACKTRACE
					_logger.info("Resending PROBLEMS for 'oldest' entry in the ErrorQueue for config name '"+_cfgName+"'. _errorQueue.size()="+_errorQueue.size()+". to '"+_url+"'. It will be kept in the queue... Caught: "+ex, ex);
					break;
				}
			}
			return sendCount;
		}
	}

	public static String replaceKey(String str)                 { return replaceKey(str, "");}
	public static String replaceKey(String str, String replace) { return str.replace("{KEY}.", replace); }

	private String key(String str)                              { return key(str, "");}
	private String key(String str, String replace)              { return str.replace("{KEY}.", replace); }

	public List<CmSettingsHelper> getAvailableSettings()
	{
		ArrayList<CmSettingsHelper> list = new ArrayList<>();
		
		Configuration conf = Configuration.getCombinedConfiguration();

		list.add( new CmSettingsHelper("URL",           Type.MANDATORY, key(PROPKEY_url),            String .class, conf.getProperty       (key(PROPKEY_url           ), DEFAULT_url           ), DEFAULT_url           , "URL to use when issuing the HTTP POST request", new UrlInputValidator()));

		list.add( new CmSettingsHelper("errorQueueSize",                key(PROPKEY_errorQueueSize), Integer.class, conf.getIntProperty    (key(PROPKEY_errorQueueSize), DEFAULT_errorQueueSize), DEFAULT_errorQueueSize, "If send errors, queue size for resend"));

		list.add( new CmSettingsHelper("sendCounters",                  key(PROPKEY_sendCounters),   Boolean.class, conf.getBooleanProperty(key(PROPKEY_sendCounters  ), DEFAULT_sendCounters  ), DEFAULT_sendCounters  , "Send Performance Counters data"));
		list.add( new CmSettingsHelper("sendGraphs",                    key(PROPKEY_sendGraphs  ),   Boolean.class, conf.getBooleanProperty(key(PROPKEY_sendGraphs    ), DEFAULT_sendGraphs    ), DEFAULT_sendGraphs    , "Send Graph/Chart data"));
                                                                                                                                                                                                                        
		list.add( new CmSettingsHelper("http-header-1",                 key(PROPKEY_header1),        String .class, conf.getProperty       (key(PROPKEY_header1       ), DEFAULT_header1       ), DEFAULT_header1       , "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-2",                 key(PROPKEY_header2),        String .class, conf.getProperty       (key(PROPKEY_header2       ), DEFAULT_header2       ), DEFAULT_header2       , "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-3",                 key(PROPKEY_header3),        String .class, conf.getProperty       (key(PROPKEY_header3       ), DEFAULT_header3       ), DEFAULT_header3       , "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-4",                 key(PROPKEY_header4),        String .class, conf.getProperty       (key(PROPKEY_header4       ), DEFAULT_header4       ), DEFAULT_header4       , "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-5",                 key(PROPKEY_header5),        String .class, conf.getProperty       (key(PROPKEY_header5       ), DEFAULT_header5       ), DEFAULT_header5       , "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-6",                 key(PROPKEY_header6),        String .class, conf.getProperty       (key(PROPKEY_header6       ), DEFAULT_header6       ), DEFAULT_header6       , "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-7",                 key(PROPKEY_header7),        String .class, conf.getProperty       (key(PROPKEY_header7       ), DEFAULT_header7       ), DEFAULT_header7       , "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-8",                 key(PROPKEY_header8),        String .class, conf.getProperty       (key(PROPKEY_header8       ), DEFAULT_header8       ), DEFAULT_header8       , "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-9",                 key(PROPKEY_header9),        String .class, conf.getProperty       (key(PROPKEY_header9       ), DEFAULT_header9       ), DEFAULT_header9       , "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));

		return list;
	}
}
