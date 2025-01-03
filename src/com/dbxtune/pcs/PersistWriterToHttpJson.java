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
package com.dbxtune.pcs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.NameFileComparator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.dbxc.AlarmEventHttpDestinationDown;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSettingsHelper.Type;
import com.dbxtune.cm.CmSettingsHelper.UrlInputValidator;
import com.dbxtune.pcs.sqlcapture.SqlCaptureDetails;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.HttpUtils;
import com.dbxtune.utils.JsonUtils;
import com.dbxtune.utils.Memory;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

public class PersistWriterToHttpJson 
extends PersistWriterBase
//implements IPersistWriter
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

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
			_confSlot0.sendErrorQueue(cont);

			// Construct and send JSON
			jsonStr = toJson(cont, _confSlot0);
			sendMessage(jsonStr, _confSlot0);
		}
		catch (ConnectException ex) 
		{
			_logger.error("Problems connecting/sending JSON-REST call to '" + _confSlot0._url + "'. The entry will be saved in the 'error-queue' and sent later. Caught: " + ex);
			_confSlot0.addToErrorQueue(cont, jsonStr);
			
			// if destination has been down for some time: Send Alarm ?
			checkSendAlarm(_confSlot0);
		}
		catch (Exception ex)
		{
			_logger.error("Problems creating JSON or sending REST call to '" + _confSlot0._url + "'. Caught: " + ex , ex);
		}

		// Loop over the "extra" configurations and send to *each* destination
		for (HttpConfigSlot slot : _confSlots)
		{
			try
			{
				// First send old entries in the error queue (no exception is thrown)
				slot.sendErrorQueue(cont);
				
				// Construct and send JSON
				jsonStr = toJson(cont, slot);
				sendMessage(jsonStr, slot);
			}
			catch (ConnectException ex) 
			{
				_logger.error("Problems connecting/sending JSON-REST call to '" + slot._url + "'. The entry will be saved in the 'error-queue' and sent later. Caught: " + ex);
				slot.addToErrorQueue(cont, jsonStr);

				// if destination has been down for some time: Send Alarm ?
				checkSendAlarm(slot);
			}
			catch (Exception ex)
			{
				_logger.error("Problems creating JSON or sending REST call to '" + slot._url + "'. Caught: " + ex , ex);
			}
		}
	}

	/** Check if we should send alarm, and if so: send the alarm */
	protected void checkSendAlarm(HttpConfigSlot slot)
	{
		AlarmHandler alarmHandler = AlarmHandler.getInstance();

		// NO Alarm Handler == NO WORK
		if ( alarmHandler == null )
			return;

		long lastSendSuccessInSec = (System.currentTimeMillis() - slot._lastSendSuccess) / 1000;

		// Is it time to send Alarm
		if (lastSendSuccessInSec > slot._errorSendAlarmThresholdInSec)
		{
			String sourceSrvName = Configuration.getCombinedConfiguration().getProperty("SERVERNAME", "srv-" + slot._cfgName);

			AlarmEvent alarmEvent = new AlarmEventHttpDestinationDown(sourceSrvName, slot._cfgName, slot._url, lastSendSuccessInSec, slot._errorSendAlarmThresholdInSec);

			// Set the time to live
			// But why ????
			// IF:   the "other" PCS Writers takes a long time (longer than the "sample interval" in the Collector thread
			// THEN: It will not see any Alarms from here... so it will see that as a "CANCEL" (since no re-raise has happened)
			// SO:   Set the TimeToLive to "maxSaveTime" for "any" of the PCS Writers in the PCS Handler
			long timeToLive = 120*1000; // 2 minutes by default
			if (PersistentCounterHandler.hasInstance())
				timeToLive = Math.max(PersistentCounterHandler.getInstance().getMaxConsumeTime(), timeToLive);

			// lets use 10 minutes as the MAX MAX time to live
			timeToLive = Math.min(timeToLive, 600*1000);

			alarmEvent.setTimeToLive(timeToLive);
			// NOTE: This will probably just decrease number of "FALSE" Alarms, so wee probably need to think of a better solution here...

			// Finally add the Alarm
			alarmHandler.addAlarm(alarmEvent);
		}
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
	protected String toJson(PersistContainer cont, HttpConfigSlot cfgSlot)
	throws IOException
	{
		try
		{
			long startTime = System.currentTimeMillis();

//			String json = toJsonMessage(cont, cfgSlot);
			String json = cont.toJsonMessage(cfgSlot._sendCounters, cfgSlot._sendGraphs, cfgSlot._sendAlarmExtDescAsHtml);

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
//			return toJsonMessage(cont, cfgSlot);
			return cont.toJsonMessage(cfgSlot._sendCounters, cfgSlot._sendGraphs, cfgSlot._sendAlarmExtDescAsHtml);
		}
	}
	

	/**
	 * Here is where the send happens
	 * @param action
	 * @param alarmEvent
	 * @return how many entries are there in the server side queue. -1 if it's unknown.
	 * @throws Exception 
	 */
	protected int sendMessage(String text, HttpConfigSlot slot) throws Exception
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

		// Check response
		int responceCode = conn.getResponseCode();
		if ( responceCode >= 203) // see 'https://httpstatuses.com/' for http codes... or at the bottom of this source code
		{
			throw new Exception("Failed : HTTP error code : " + responceCode + " (" + HttpUtils.httpResponceCodeToText(responceCode) + "). From URL '" + slot._url + "'. Sent JSON content.length: " + text.length());
		}
		else
		{
			int threshold = 1000;
			if (sendTime > threshold)
				_logger.warn("HTTP REST Call took longer than expected. sendTimeInMs = " + sendTime + ", which is above threshold=" + threshold + ", Responce code " + responceCode + " (" + HttpUtils.httpResponceCodeToText(responceCode) + "). From URL '" + slot._url + "'. Sent JSON content.length: " + text.length() + ", " + (text.length()/1024) + " KB");

			_logger.debug("Responce code " + responceCode + " (" + HttpUtils.httpResponceCodeToText(responceCode) + "). From URL '" + slot._url + "'. Sent JSON content.length: " + text.length() + ", " + (text.length()/1024) + " KB");
		}

		// Mark last success time
		slot._lastSendSuccess = System.currentTimeMillis();

		// Read response and print the output...
		BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

		String outputRow;
		StringBuilder sb = new StringBuilder();
		while ((outputRow = br.readLine()) != null)
		{
			sb.append(outputRow);
		}
		String output = sb.toString().trim();
		_logger.debug("Responce from server: " + output);
		
		int serverSideQueueSize = -1;
		// Check if output looks like a JSON message
		if (JsonUtils.isPossibleJson(output))
		{
			// TODO: read the JSON
			// { "queueSize": ### }
			// Quick fix: UGGLY IMPLEMENTATION
			String str = "\"queueSize\": ";
			int startPos = output.indexOf(str);
			int endPos   = output.indexOf(" ", startPos + str.length());
			if ( startPos >= 0 && endPos > startPos)
			{
				startPos = startPos + str.length();
				
				String queueSizeStr = output.substring(startPos, endPos).trim();
				serverSideQueueSize = StringUtil.parseInt(queueSizeStr, -1);
			}
		}
		else // If NOT JSON, print the output... so we can trace what it was
		{
			_logger.info("Responce from server: " + output);
		}

		conn.disconnect();
		
		return serverSideQueueSize;
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
			throw new Exception("Problem parsing the value '" + keyVal + "', can't find any ':' in it.");
		
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

	protected void printConfig(HttpConfigSlot slot)
	{
		int spaces = 55;

		String cfgName = slot._cfgName;
		if (StringUtil.containsAny(cfgName, "DbxCentral", "slot0"))
			cfgName = "";

		if (StringUtil.hasValue(slot._url     )) _logger.info("    " + StringUtil.left(key(PROPKEY_url                              , cfgName), spaces) + ": " + slot._url);

		if (StringUtil.hasValue(slot._url     )) _logger.info("    " + StringUtil.left(key(PROPKEY_errorSendAlarm                   , cfgName), spaces) + ": " + slot._errorSendAlarm);
		if (StringUtil.hasValue(slot._url     )) _logger.info("    " + StringUtil.left(key(PROPKEY_errorSendAlarmThresholdInSec     , cfgName), spaces) + ": " + slot._errorSendAlarmThresholdInSec);

		if (StringUtil.hasValue(slot._url     )) _logger.info("    " + StringUtil.left(key(PROPKEY_errorSaveToDisk                  , cfgName), spaces) + ": " + slot._errorSaveToDisk);
		if (StringUtil.hasValue(slot._url     )) _logger.info("    " + StringUtil.left(key(PROPKEY_errorSaveToDiskPath              , cfgName), spaces) + ": " + slot._errorSaveToDiskPath);
		if (StringUtil.hasValue(slot._url     )) _logger.info("    " + StringUtil.left(key(PROPKEY_errorSaveToDiskDiscardAfterXDays , cfgName), spaces) + ": " + slot._errorSaveToDiskDiscardAfterXDays);
		if (StringUtil.hasValue(slot._url     )) _logger.info("    " + StringUtil.left(key(PROPKEY_errorSaveToDiskSuccessSleepTimeMs, cfgName), spaces) + ": " + slot._errorSaveToDiskSuccessSleepTimeMs);

		if (StringUtil.hasValue(slot._url     )) _logger.info("    " + StringUtil.left(key(PROPKEY_errorMemQueueSize                , cfgName), spaces) + ": " + slot._errorMemQueueSize);

		if (StringUtil.hasValue(slot._url     )) _logger.info("    " + StringUtil.left(key(PROPKEY_sendAlarmExtDescAsHtml           , cfgName), spaces) + ": " + slot._sendAlarmExtDescAsHtml);
		if (StringUtil.hasValue(slot._url     )) _logger.info("    " + StringUtil.left(key(PROPKEY_sendCounters                     , cfgName), spaces) + ": " + slot._sendCounters);
		if (StringUtil.hasValue(slot._url     )) _logger.info("    " + StringUtil.left(key(PROPKEY_sendGraphs                       , cfgName), spaces) + ": " + slot._sendGraphs);

		if (StringUtil.hasValue(slot._header_1)) _logger.info("    " + StringUtil.left(key(PROPKEY_header1                          , cfgName), spaces) + ": " + slot._header_1);
		if (StringUtil.hasValue(slot._header_2)) _logger.info("    " + StringUtil.left(key(PROPKEY_header2                          , cfgName), spaces) + ": " + slot._header_2);
		if (StringUtil.hasValue(slot._header_3)) _logger.info("    " + StringUtil.left(key(PROPKEY_header3                          , cfgName), spaces) + ": " + slot._header_3);
		if (StringUtil.hasValue(slot._header_4)) _logger.info("    " + StringUtil.left(key(PROPKEY_header4                          , cfgName), spaces) + ": " + slot._header_4);
		if (StringUtil.hasValue(slot._header_5)) _logger.info("    " + StringUtil.left(key(PROPKEY_header5                          , cfgName), spaces) + ": " + slot._header_5);
		if (StringUtil.hasValue(slot._header_6)) _logger.info("    " + StringUtil.left(key(PROPKEY_header6                          , cfgName), spaces) + ": " + slot._header_6);
		if (StringUtil.hasValue(slot._header_7)) _logger.info("    " + StringUtil.left(key(PROPKEY_header7                          , cfgName), spaces) + ": " + slot._header_7);
		if (StringUtil.hasValue(slot._header_8)) _logger.info("    " + StringUtil.left(key(PROPKEY_header8                          , cfgName), spaces) + ": " + slot._header_8);
		if (StringUtil.hasValue(slot._header_9)) _logger.info("    " + StringUtil.left(key(PROPKEY_header9                          , cfgName), spaces) + ": " + slot._header_9);
	}
	
	public void printConfig()
	{
		_logger.info("Configuration for Persist Writer Module: " + getName());
		
		printConfig(_confSlot0);
		
		for (HttpConfigSlot slot : _confSlots)
		{
			printConfig(slot);
		}
	}

	private void checkOrCreateRecoveryDir(HttpConfigSlot httpConfigSlot)
	{
		if (httpConfigSlot == null)
			return;
		
		if ( ! httpConfigSlot._errorSaveToDisk )
			return;

		// If dir do NOT exist, create the dir
		File dir = new File(httpConfigSlot._errorSaveToDiskPath);
		if ( ! dir.exists() )
		{
			if ( dir.mkdirs() )
			{
				_logger.info("Created the recovery directory '" + httpConfigSlot._errorSaveToDiskPath + "' for configuration '" + httpConfigSlot._cfgName + "'.");
			}
		}
	}

	protected HttpConfigSlot initConfigSlot(Configuration conf, String cfgKey, HttpConfigSlot preAllocatedSlot)
	throws Exception
	{
		// Special for "DbxCentral" & "slot0"... Then there is NO "{KEY}" in the PROPERTY Names
//		if (StringUtil.containsAny(cfgKey, "DbxCentral", "slot0"))
		if (StringUtil.equalsAny(cfgKey, "DbxCentral", "slot0"))
			cfgKey = "";
		
		String defaultUrl = null;
		if (preAllocatedSlot != null)
			defaultUrl = preAllocatedSlot._url;
		
		String url                                = conf.getProperty            (key(PROPKEY_url                              , cfgKey), defaultUrl);
        
		boolean errorSendAlarm                    = conf.getBooleanProperty     (key(PROPKEY_errorSendAlarm                   , cfgKey), DEFAULT_errorSendAlarm);
		int     errorSendAlarmThresholdInSec      = conf.getIntProperty         (key(PROPKEY_errorSendAlarmThresholdInSec     , cfgKey), DEFAULT_errorSendAlarmThresholdInSec);
                                                                                
		boolean errorSaveToDisk                   = conf.getBooleanProperty     (key(PROPKEY_errorSaveToDisk                  , cfgKey), DEFAULT_errorSaveToDisk);
//		String  errorSaveToDiskPath               = conf.getProperty            (key(PROPKEY_errorSaveToDiskPath              , cfgKey), DEFAULT_errorSaveToDiskPath);
		String  errorSaveToDiskPath               = conf.getProperty            (key(PROPKEY_errorSaveToDiskPath              , cfgKey), get_DEFAULT_errorSaveToDiskPath());
		int     errorSaveToDiskDiscardAfterXDays  = conf.getIntProperty         (key(PROPKEY_errorSaveToDiskDiscardAfterXDays , cfgKey), DEFAULT_errorSaveToDiskDiscardAfterXDays);
		int     errorSaveToDiskSuccessSleepTimeMs = conf.getIntProperty         (key(PROPKEY_errorSaveToDiskSuccessSleepTimeMs, cfgKey), DEFAULT_errorSaveToDiskSuccessSleepTimeMs);
                                                                                
		int    errorMemQueueSize                  = conf.getIntProperty         (key(PROPKEY_errorMemQueueSize                , cfgKey), DEFAULT_errorMemQueueSize);
                                                                                                                              
		boolean sendAlarmExtDescAsHtml            = conf.getBooleanProperty     (key(PROPKEY_sendAlarmExtDescAsHtml           , cfgKey), DEFAULT_sendAlarmExtDescAsHtml);
//		boolean sendCounters                      = conf.getBooleanProperty     (key(PROPKEY_sendCounters                     , cfgKey), DEFAULT_sendCounters);
//		boolean sendGraphs                        = conf.getBooleanProperty     (key(PROPKEY_sendGraphs                       , cfgKey), DEFAULT_sendGraphs  );
		SendCountersConfig sendCounters           = new SendCountersConfig(conf, key(PROPKEY_sendCounters                     , cfgKey), DEFAULT_sendCounters);
		SendCountersConfig sendGraphs             = new SendCountersConfig(conf, key(PROPKEY_sendGraphs                       , cfgKey), DEFAULT_sendGraphs);
		                                                                                                                 
		String header_1                           = conf.getProperty            (key(PROPKEY_header1                          , cfgKey), null);
		String header_2                           = conf.getProperty            (key(PROPKEY_header2                          , cfgKey), null);
		String header_3                           = conf.getProperty            (key(PROPKEY_header3                          , cfgKey), null);
		String header_4                           = conf.getProperty            (key(PROPKEY_header4                          , cfgKey), null);
		String header_5                           = conf.getProperty            (key(PROPKEY_header5                          , cfgKey), null);
		String header_6                           = conf.getProperty            (key(PROPKEY_header6                          , cfgKey), null);
		String header_7                           = conf.getProperty            (key(PROPKEY_header7                          , cfgKey), null);
		String header_8                           = conf.getProperty            (key(PROPKEY_header8                          , cfgKey), null);
		String header_9                           = conf.getProperty            (key(PROPKEY_header9                          , cfgKey), null);
		
		if (StringUtil.isNullOrBlank(url))
		{
			_logger.warn("When getting configuration for config '" + cfgKey + "' using property '" + key(PROPKEY_url, cfgKey) + "' no value for URL was found. Skipping this section.");
			return null;
		}
		else
		{
			// Add a SLOT to the list
			HttpConfigSlot slot = preAllocatedSlot;
			if (slot == null)
				slot = new HttpConfigSlot();

			// do NOT change NAME/URL if we have a preAllocatedSlot
			// Only set NAME/URL for NEW slots, if a preAllocatedSlot was passed, trust NAME/URL
			if (preAllocatedSlot != null)
			{
				slot._cfgName = cfgKey;
				slot._url     = url;
			}
			
			slot._errorSendAlarm                    = errorSendAlarm;
			slot._errorSendAlarmThresholdInSec      = errorSendAlarmThresholdInSec;
			
			slot._errorSaveToDisk                   = errorSaveToDisk;
			slot._errorSaveToDiskPath               = errorSaveToDiskPath;
			slot._errorSaveToDiskDiscardAfterXDays  = errorSaveToDiskDiscardAfterXDays;
			slot._errorSaveToDiskSuccessSleepTimeMs = errorSaveToDiskSuccessSleepTimeMs;

			slot._errorMemQueueSize      = errorMemQueueSize;

			slot._sendAlarmExtDescAsHtml = sendAlarmExtDescAsHtml;
			slot._sendCounters           = sendCounters;
			slot._sendGraphs             = sendGraphs;
			
			slot._header_1       = header_1;
			slot._header_2       = header_2;
			slot._header_3       = header_3;
			slot._header_4       = header_4;
			slot._header_5       = header_5;
			slot._header_6       = header_6;
			slot._header_7       = header_7;
			slot._header_8       = header_8;
			slot._header_9       = header_9;

			checkOrCreateRecoveryDir(slot);

			// Check if the URL seems to be OK...
			try 
			{ 
				new URL(slot._url); 
			}
			catch(MalformedURLException ex) 
			{ 
				throw new Exception("The URL '" + slot._url + "' for config '" + slot._cfgName + "' seems to be malformed. Caught: " + ex, ex); 
			}

			//_confSlots.add(slot);
			return slot;
		}
		
	}

	@Override
	public void init(Configuration conf) throws Exception
	{
//System.out.println(getName() + ": INIT.....................................");

		_conf = conf;

		_logger.info("Initializing the PersistWriter component named '" + getName() + "'.");

		//--------------------------------------------
		// Read "slot0/DbxCentral" configurations
		//--------------------------------------------
		_confSlot0 = new HttpConfigSlot();
		_confSlot0._url            = conf.getProperty(key(PROPKEY_url), DEFAULT_url);
		_confSlot0._cfgName        = "slot0";
		if (_confSlot0._url.endsWith("/api/pcs/receiver"))
			_confSlot0._cfgName    = "DbxCentral";

		_confSlot0 = initConfigSlot(conf, "", _confSlot0);
		

		//--------------------------------------------
		// Read "extra" configurations "slots"
		//--------------------------------------------
		String keyStr = conf.getProperty(PROPKEY_configKeys, DEFAULT_configKeys);
		List<String> keyList = StringUtil.commaStrToList(keyStr);
		
		for (String cfgKey : keyList)
		{
			HttpConfigSlot httpConfigSlot = initConfigSlot(conf, cfgKey, null);
			if (httpConfigSlot != null)
				_confSlots.add(httpConfigSlot);
		}


		//------------------------------------------
		// Check for mandatory parameters
		//------------------------------------------
		if ( StringUtil.isNullOrBlank(_confSlot0._url) )          throw new Exception("The property '" + PROPKEY_url          + "' is mandatory for the PersistWriter named '" + getName() + "'.");


		//------------------------------------------
		// Check for valid configuration
		//------------------------------------------
		// Check if the URL seems to be OK... This is already done in method 'initConfigSlot(...)'
		
		//------------------------------------------
		// Print how we are configured
		//------------------------------------------
		printConfig();
	}

	public static final String  PROPKEY_configKeys        = "{CLASSNAME}.config.keys";
	public static final String  DEFAULT_configKeys        = null;

	public static final String  PROPKEY_url               = "{CLASSNAME}.{KEY}.url";
//	public static final String  DEFAULT_url               = null;
	public static final String  DEFAULT_url               = "http://localhost:8080/api/pcs/receiver";


	public static final String  PROPKEY_errorSendAlarm                    = "{CLASSNAME}.{KEY}.error.sendAlarm";
	public static final boolean DEFAULT_errorSendAlarm                    = true;

	public static final String  PROPKEY_errorSendAlarmThresholdInSec      = "{CLASSNAME}.{KEY}.error.sendAlarm.thresholdInSec";
//	public static final int     DEFAULT_errorSendAlarmThresholdInSec      = 60 * 30;  // 30  minutes
//	public static final int     DEFAULT_errorSendAlarmThresholdInSec      = 60 * 60;  // 1   Hour
//	public static final int     DEFAULT_errorSendAlarmThresholdInSec      = 60 * 90;  // 1.5 Hour
//	public static final int     DEFAULT_errorSendAlarmThresholdInSec      = 60 * 120; // 2   Hour
//	public static final int     DEFAULT_errorSendAlarmThresholdInSec      = 60 * 150; // 2.5 Hour
	public static final int     DEFAULT_errorSendAlarmThresholdInSec      = 60 * 180; // 3   Hour

	
	public static final String  PROPKEY_errorSaveToDisk                   = "{CLASSNAME}.{KEY}.error.saveToDisk";
	public static final boolean DEFAULT_errorSaveToDisk                   = true;

	public static final String  PROPKEY_errorSaveToDiskPath               = "{CLASSNAME}.{KEY}.error.saveToDisk.path";
//	public static final String  DEFAULT_errorSaveToDiskPath               = System.getProperty("java.io.tmpdir") + File.separatorChar + "DbxTune" + File.separatorChar + "PersistWriterToHttpJson";
//	public static final String  DEFAULT_errorSaveToDiskPath               = System.getProperty("java.io.tmpdir") + File.separatorChar + "DbxTune" + File.separatorChar + MethodHandles.lookup().lookupClass().getSimpleName();
	public String  get_DEFAULT_errorSaveToDiskPath()
	{
//		return System.getProperty("java.io.tmpdir") + File.separatorChar + "DbxTune" + File.separatorChar + "PersistWriterToHttpJson";
		return System.getProperty("java.io.tmpdir") + File.separatorChar + "DbxTune" + File.separatorChar + this.getClass().getSimpleName();
	}

	public static final String  PROPKEY_errorSaveToDiskDiscardAfterXDays  = "{CLASSNAME}.{KEY}.error.saveToDisk.discard.after.days";
	public static final int     DEFAULT_errorSaveToDiskDiscardAfterXDays  = 1;

	public static final String  PROPKEY_errorSaveToDiskSuccessSleepTimeMs = "{CLASSNAME}.{KEY}.error.saveToDisk.success.sleep.time.ms";
	public static final int     DEFAULT_errorSaveToDiskSuccessSleepTimeMs = 2000;

	
	public static final String  PROPKEY_errorMemQueueSize                 = "{CLASSNAME}.{KEY}.error.in-memory.queue.size";
	public static final int     DEFAULT_errorMemQueueSize                 = 10;


	public static final String  PROPKEY_sendAlarmExtDescAsHtml            = "{CLASSNAME}.{KEY}.send.alarm.extendedDescAsHtml";
	public static final boolean DEFAULT_sendAlarmExtDescAsHtml            = true;

	public static final String  PROPKEY_sendCounters      = "{CLASSNAME}.{KEY}.send.counters";
//	public static final boolean DEFAULT_sendCounters      = false;
//	public static final String  DEFAULT_sendCounters      = "none";
	public static final String  DEFAULT_sendCounters      = "CmActiveStatements=adrc";

	public static final String  PROPKEY_sendGraphs        = "{CLASSNAME}.{KEY}.send.graphs";
//	public static final boolean DEFAULT_sendGraphs        = true;
	public static final String  DEFAULT_sendGraphs        = "all";

	public static final String  PROPKEY_header1           = "{CLASSNAME}.{KEY}.header.1";
//	public static final String  DEFAULT_header1           = null;
//	public static final String  DEFAULT_header1           = "Content-Type: application/json";
	public static final String  DEFAULT_header1           = "Content-Type: text/plain";

	public static final String  PROPKEY_header2           = "{CLASSNAME}.{KEY}.header.2";
	public static final String  DEFAULT_header2           = null;

	public static final String  PROPKEY_header3           = "{CLASSNAME}.{KEY}.header.3";
	public static final String  DEFAULT_header3           = null;

	public static final String  PROPKEY_header4           = "{CLASSNAME}.{KEY}.header.4";
	public static final String  DEFAULT_header4           = null;

	public static final String  PROPKEY_header5           = "{CLASSNAME}.{KEY}.header.5";
	public static final String  DEFAULT_header5           = null;

	public static final String  PROPKEY_header6           = "{CLASSNAME}.{KEY}.header.6";
	public static final String  DEFAULT_header6           = null;

	public static final String  PROPKEY_header7           = "{CLASSNAME}.{KEY}.header.7";
	public static final String  DEFAULT_header7           = null;

	public static final String  PROPKEY_header8           = "{CLASSNAME}.{KEY}.header.8";
	public static final String  DEFAULT_header8           = null;

	public static final String  PROPKEY_header9           = "{CLASSNAME}.{KEY}.header.9";
	public static final String  DEFAULT_header9           = null;

	/** All files should START with this */
//	public static final String RECOVERY_FILE_PREFIX = "DbxTune.PersistWriterToHttpJson.retry.";
//	public static final String RECOVERY_FILE_SUFFIX = "json";
	public String get_RECOVERY_FILE_PREFIX() { return "DbxTune." + this.getClass().getSimpleName() + ".retry."; }
	public String get_RECOVERY_FILE_SUFFIX() { return "json"; }

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

	private      HttpConfigSlot  _confSlot0 = new HttpConfigSlot();
	private List<HttpConfigSlot> _confSlots = new ArrayList<>();

	protected class HttpConfigSlot
	{
		String  _cfgName  = "";
		String  _url      = "";

		boolean _errorSendAlarm                    = DEFAULT_errorSendAlarm;
		int     _errorSendAlarmThresholdInSec      = DEFAULT_errorSendAlarmThresholdInSec;
		long    _lastSendSuccess                   = System.currentTimeMillis(); // used to determine if we should send alarm

		boolean _errorSaveToDisk                   = DEFAULT_errorSaveToDisk;
//		String  _errorSaveToDiskPath               = DEFAULT_errorSaveToDiskPath;
		String  _errorSaveToDiskPath               = get_DEFAULT_errorSaveToDiskPath();
		int     _errorSaveToDiskDiscardAfterXDays  = DEFAULT_errorSaveToDiskDiscardAfterXDays;
		int     _errorSaveToDiskSuccessSleepTimeMs = DEFAULT_errorSaveToDiskSuccessSleepTimeMs;
		
		int     _errorMemQueueSize = DEFAULT_errorMemQueueSize;
		LinkedList<String> _errorQueue = new LinkedList<>(); 

		boolean _sendAlarmExtDescAsHtml   = DEFAULT_sendAlarmExtDescAsHtml;
//		boolean _sendCounters             = DEFAULT_sendCounters;
//		boolean _sendGraphs               = DEFAULT_sendGraphs;
		SendCountersConfig _sendCounters = new SendCountersConfig(false);
		SendCountersConfig _sendGraphs   = new SendCountersConfig(true);
		
		String  _header_1 = "";
		String  _header_2 = "";
		String  _header_3 = "";
		String  _header_4 = "";
		String  _header_5 = "";
		String  _header_6 = "";
		String  _header_7 = "";
		String  _header_8 = "";
		String  _header_9 = "";

//		/** All files should START with this */
//		public static final String RECOVERY_FILE_PREFIX = "DbxTune.PersistWriterToHttpJson.retry.";
//		public static final String RECOVERY_FILE_SUFFIX = "json";
		
		public HttpConfigSlot()
		{
		}

		/** 
		 * Add message to error queue, to be sent later 
		 */
		public void addToErrorQueue(PersistContainer cont, String jsonStr)
		{
			if (StringUtil.isNullOrBlank(jsonStr))
				return;

			try
			{
				// SAVE the JSON to a file and retry later
				if (_errorSaveToDisk)
				{
					checkOrCreateRecoveryDir(this);
					
					// Remove older error files
					removeOldErrorFiles(cont);
					
					// Maybe check if we have enough SPACE on: _errorSaveToDiskPath

					// init some helper variables
					String ts  = new SimpleDateFormat("yyyy-MM-dd.HH_mm_ss_SSS").format( new Date(System.currentTimeMillis()) );
					String srv = cont.getServerNameOrAlias(); 
					File   f   = new File(_errorSaveToDiskPath + File.separatorChar + get_RECOVERY_FILE_PREFIX() + srv + "." + _cfgName + "." + ts + "." + get_RECOVERY_FILE_SUFFIX());

					_logger.info("addToErrorQueue(SAVE-TO-DISK): cfgName='" + _cfgName + "'. Saving to file: " + f);
					try
					{
						FileUtils.write(f, jsonStr, StandardCharsets.UTF_8);
					}
					catch (IOException ex)
					{
						_logger.error("addToErrorQueue(SAVE-TO-DISK): cfgName='" + _cfgName + "'. Error when saving to file '" + f + "'. Caught: " + ex, ex);
					}
				}
				// IN-MEMORY error queue
				else
				{
					_errorQueue.addLast(jsonStr);
					
					while (_errorQueue.size() > _errorMemQueueSize)
					{
						_errorQueue.removeFirst();
						_logger.info("addToErrorQueue(IN-MEMORY): Removing 'oldest' entry in the ErrorQueue for config name '" + _cfgName + "'. _errorQueue.size()=" + _errorQueue.size() + ", maxEntries=" + _errorMemQueueSize);
					}
				}
			}
			catch (RuntimeException ex)
			{
				_logger.error("Runtime Problems in: addToErrorQueue(), cfgName='" + _cfgName + "', continuing anyway... Caught: " + ex, ex);
			}
		}

		/** 
		 * send old entries in the errorQueue, if the queue is empty, simply do nothing.
		 * @return number of entries sent
		 */
		public int sendErrorQueue(PersistContainer cont)
		{
			try
			{
				// recover saved FILES
				if (_errorSaveToDisk)
				{
					int sendCount = 0;
					
					// Remove older error files
					//removeOldErrorFiles();

					String srv = cont.getServerNameOrAlias(); 

					// grab error files, and send them
					//Collection<File> filesColl = FileUtils.listFiles(new File(_errorSaveToDiskPath), new WildcardFileFilter(RECOVERY_FILE_PREFIX + srv + "." + _cfgName + ".*"), TrueFileFilter.TRUE); // maybe... but not tested.
					Collection<File> filesColl = FileUtils.listFiles(new File(_errorSaveToDiskPath), new String[] {get_RECOVERY_FILE_SUFFIX()}, false);
					if ( ! filesColl.isEmpty() )
					{
						// Make an array, and sort it...
						File[] filesArr = FileUtils.convertFileCollectionToFileArray(filesColl);
						Arrays.sort(filesArr, NameFileComparator.NAME_COMPARATOR);
						
						// loop all files (skip files that "someone else" is responsible for)
						for (File file : filesArr )
						{
							String filename = file.getName();
							if (filename.startsWith(get_RECOVERY_FILE_PREFIX() + srv + "." + _cfgName + "."))
							{
								_logger.info("sendErrorQueue(SAVE-TO-DISK): cfgName='" + _cfgName + "', srv='" + srv + "'. trying to recover and send file: " + file);
								try
								{
									String jsonStr = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

									// If the file is empty... delete the file and go to next
									if (StringUtil.isNullOrBlank(jsonStr))
									{
										_logger.info("sendErrorQueue(SAVE-TO-DISK): cfgName='" + _cfgName + "', srv='" + srv + "'. Found empty file, just deleting it and continuing with next... deleted file: " + file);
										file.delete();
										continue;
									}
									
									// send message (if we have problems an exception will be thrown)
									int serverSideQueueSize = sendMessage(jsonStr, this);
									
									// on success: remove the file
									file.delete();
									
									sendCount++;
									
									// Sleep for a while (do not overload the central server)
									// NOTE: if we do NOT want the sleep time to be static, then the server needs to send feedback (in sendMessage()) about how long we should sleep
									//       meaning if the server has a short list to save, then short sleep time, or if the persist list is long a longer sleep time.
									int sleepThreshold = 5;
									if (serverSideQueueSize > sleepThreshold)
									{
										_logger.info("sendErrorQueue(SAVE-TO-DISK): serverSideQueueSize=" + serverSideQueueSize + ", Sleeping " + _errorSaveToDiskSuccessSleepTimeMs + " ms for config name '" + _cfgName + "', srv='" + srv + "', sendCount=" + sendCount + ", after sending file '" + file + "'. This not to overload the Central Server.");
										Thread.sleep(_errorSaveToDiskSuccessSleepTimeMs);
									}
								}
								catch (InterruptedException ex)
								{
									_logger.info("sendErrorQueue(SAVE-TO-DISK): Interupted when doing disk entry recovery at file '" + file + "' in the ErrorQueue for config name '" + _cfgName + "'. to '" + _url + "'.");
									break;
								}
								catch (ConnectException ex) 
								{
									// log WITHOUT stacktrace
									_logger.info("sendErrorQueue(SAVE-TO-DISK): Resending PROBLEMS for disk entry '" + file + "' in the ErrorQueue for config name '" + _cfgName + "'. to '" + _url + "'. It will be kept on disk... Caught: " + ex);
									break;
								}
								catch (Exception ex) 
								{
									// log with STACKTRACE
									_logger.info("sendErrorQueue(SAVE-TO-DISK): Resending PROBLEMS for disk entry '" + file + "' in the ErrorQueue for config name '" + _cfgName + "'. to '" + _url + "'. It will be kept on disk... Caught: " + ex, ex);
									break;
								}
							} // end: correct file
						} // end: loop
					} // end: ! files.isEmpty()
					
					return sendCount;
				}
				// IN-MEMORY recovery
				else
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

							_logger.info("sendErrorQueue(IN-MEMORY): Resending 'oldest' entry in the ErrorQueue for config name '" + _cfgName + "'. _errorQueue.size()=" + _errorQueue.size());
							
							// send message (if we have problems an exception will be thrown)
							sendMessage(jsonStr, this);
							
							// on success: remove the queue entry
							_errorQueue.removeFirst();
							sendCount++;
						}
						catch (ConnectException ex) 
						{
							// log WITHOUT stacktrace
							_logger.info("sendErrorQueue(IN-MEMORY): Resending PROBLEMS for 'oldest' entry in the ErrorQueue for config name '" + _cfgName + "'. _errorQueue.size()=" + _errorQueue.size() + ". to '" + _url + "'. It will be kept in the queue... Caught: " + ex);
							break;
						}
						catch (Exception ex) 
						{
							// log with STACKTRACE
							_logger.info("sendErrorQueue(IN-MEMORY): Resending PROBLEMS for 'oldest' entry in the ErrorQueue for config name '" + _cfgName + "'. _errorQueue.size()=" + _errorQueue.size() + ". to '" + _url + "'. It will be kept in the queue... Caught: " + ex, ex);
							break;
						}
					}
					return sendCount;
				}
			}
			catch (RuntimeException ex)
			{
				_logger.error("Runtime Problems in: sendErrorQueue(), cfgName='" + _cfgName + "', continuing anyway... Caught: " + ex, ex);
				return 0;
			}
		}
		
		/**
		 * Remove older error files than the configuration allows...
		 */
		private void removeOldErrorFiles(PersistContainer cont)
		{
			try
			{
				String srv = cont.getServerNameOrAlias(); 

				// Threshold for what to delete
				long timeMillis = System.currentTimeMillis() - (_errorSaveToDiskDiscardAfterXDays * 3600 * 24 * 1000); 
				
				// grab error files, and send them
				for (File file : FileUtils.listFiles(new File(_errorSaveToDiskPath), new String[] {get_RECOVERY_FILE_SUFFIX()}, false) )
				{
					String filename = file.getName();
					if (filename.startsWith(get_RECOVERY_FILE_PREFIX() + srv + "." + _cfgName + "."))
					{
						if (FileUtils.isFileOlder(file, timeMillis))
						{
							if (file.delete())
								_logger.info("removeOldErrorFiles(): cfgName='" + _cfgName + "', srv='" + srv + "'. SUCCESS: removing file: " + file);
							else
								_logger.info("removeOldErrorFiles(): cfgName='" + _cfgName + "', srv='" + srv + "'. FAILED: removing file: " + file);
						}
					}
				}
			}
			catch (RuntimeException ex)
			{
				_logger.error("Runtime Problems in: removeOldErrorFiles(), cfgName='" + _cfgName + "', continuing anyway... Caught: " + ex, ex);
			}
		}
	}

//	public static String replaceKey(String str)                 { return replaceKey(str, ""); }
//	public static String replaceKey(String str, String replace) { return str.replace("{KEY}.", replace); }
//
//	private String key(String str)                              { return key(str, ""); }
//	private String key(String str, String replace)              { return str.replace("{KEY}.", replace); }

//	public static String replaceKey(String str)                 { return replaceKey(str, ""); }
	public static String replaceKey(String str, String className, String key)  { return str.replace("{CLASSNAME}", className).replace("{KEY}.", key); }

	protected String key(String str)                              { return key(str, ""); }
	protected String key(String str, String replace)              { return str.replace("{CLASSNAME}", getName()).replace("{KEY}.", replace); } // Note: getName() does: return this.getClass().getSimpleName()
	
	public List<CmSettingsHelper> getAvailableSettings()
	{
		ArrayList<CmSettingsHelper> list = new ArrayList<>();
		
		Configuration conf = Configuration.getCombinedConfiguration();

		list.add( new CmSettingsHelper("URL",           Type.MANDATORY,     key(PROPKEY_url                              ), String .class, conf.getProperty       (key(PROPKEY_url                              ), DEFAULT_url                              ), DEFAULT_url                              , "URL to use when issuing the HTTP POST request", new UrlInputValidator()));

		list.add( new CmSettingsHelper("errorSendAlarm",                    key(PROPKEY_errorSendAlarm                   ), Boolean.class, conf.getBooleanProperty(key(PROPKEY_errorSendAlarm                   ), DEFAULT_errorSendAlarm                   ), DEFAULT_errorSendAlarm                   , "If send errors, Send Alarm.)"));
		list.add( new CmSettingsHelper("errorSendAlarmThresholdInSec",      key(PROPKEY_errorSendAlarmThresholdInSec     ), Integer.class, conf.getIntProperty    (key(PROPKEY_errorSendAlarmThresholdInSec     ), DEFAULT_errorSendAlarmThresholdInSec     ), DEFAULT_errorSendAlarmThresholdInSec     , "If send errors, Send Alarm, but only after X Seconds.)"));

		list.add( new CmSettingsHelper("errorSaveToDisk",                   key(PROPKEY_errorSaveToDisk                  ), Boolean.class, conf.getBooleanProperty(key(PROPKEY_errorSaveToDisk                  ), DEFAULT_errorSaveToDisk                  ), DEFAULT_errorSaveToDisk                  , "If send errors, save the JSON message to DISK and retry later.)"));
//		list.add( new CmSettingsHelper("errorSaveToDiskPath",               key(PROPKEY_errorSaveToDiskPath              ), String .class, conf.getProperty       (key(PROPKEY_errorSaveToDiskPath              ), DEFAULT_errorSaveToDiskPath              ), DEFAULT_errorSaveToDiskPath              , "Path where to save JSON messages on send errors"));
		list.add( new CmSettingsHelper("errorSaveToDiskPath",               key(PROPKEY_errorSaveToDiskPath              ), String .class, conf.getProperty       (key(PROPKEY_errorSaveToDiskPath              ), get_DEFAULT_errorSaveToDiskPath()        ), get_DEFAULT_errorSaveToDiskPath()        , "Path where to save JSON messages on send errors"));
		list.add( new CmSettingsHelper("errorSaveToDiskDiscardAfterXDays",  key(PROPKEY_errorSaveToDiskDiscardAfterXDays ), Integer.class, conf.getIntProperty    (key(PROPKEY_errorSaveToDiskDiscardAfterXDays ), DEFAULT_errorSaveToDiskDiscardAfterXDays ), DEFAULT_errorSaveToDiskDiscardAfterXDays , "How many days should we save the files."));
		list.add( new CmSettingsHelper("errorSaveToDiskSuccessSleepTimeMs", key(PROPKEY_errorSaveToDiskSuccessSleepTimeMs), Integer.class, conf.getIntProperty    (key(PROPKEY_errorSaveToDiskSuccessSleepTimeMs), DEFAULT_errorSaveToDiskSuccessSleepTimeMs), DEFAULT_errorSaveToDiskSuccessSleepTimeMs, "After a recovered file has been sent, sleep for a while so we do not overflow the central server with messages."));

		list.add( new CmSettingsHelper("errorMemQueueSize",                 key(PROPKEY_errorMemQueueSize                ), Integer.class, conf.getIntProperty    (key(PROPKEY_errorMemQueueSize                ), DEFAULT_errorMemQueueSize                ), DEFAULT_errorMemQueueSize               , "If send errors, in memory queue size for resend (Only valid if 'errorSaveToDisk' is false)"));

		list.add( new CmSettingsHelper("sendAlarmExtDescAsHtml",            key(PROPKEY_sendAlarmExtDescAsHtml           ), Boolean.class, conf.getBooleanProperty(key(PROPKEY_sendAlarmExtDescAsHtml           ), DEFAULT_sendAlarmExtDescAsHtml           ), DEFAULT_sendAlarmExtDescAsHtml          , "If we have alarms in the Counter Collector, Send the 'Alarm Extended Descriptions' in HTML.)"));
//		list.add( new CmSettingsHelper("sendCounters",                      key(PROPKEY_sendCounters                     ), Boolean.class, conf.getBooleanProperty(key(PROPKEY_sendCounters                     ), DEFAULT_sendCounters                     ), DEFAULT_sendCounters                    , "Send Performance Counters data"));
//		list.add( new CmSettingsHelper("sendGraphs",                        key(PROPKEY_sendGraphs                       ), Boolean.class, conf.getBooleanProperty(key(PROPKEY_sendGraphs                       ), DEFAULT_sendGraphs                       ), DEFAULT_sendGraphs                      , "Send Graph/Chart data"));
		list.add( new CmSettingsHelper("sendCounters",                      key(PROPKEY_sendCounters                     ), String .class, conf.getProperty       (key(PROPKEY_sendCounters                     ), DEFAULT_sendCounters                     ), DEFAULT_sendCounters                    , "Send Performance Counters data. CmName of counters to send. '*' or 'all' means all CM's. '' or 'none' means do not send any. Example: 'CmName1, CmName2=adr, CmName3=r'   Note: 'adrjc' can be specified where a=AbsoluteCounters, d=DiffCounters, r=RateCounters, j=JdbcMetaData, c=CounterModelMetaData. Default is 'adrc'."));
		list.add( new CmSettingsHelper("sendGraphs",                        key(PROPKEY_sendGraphs                       ), String .class, conf.getProperty       (key(PROPKEY_sendGraphs                       ), DEFAULT_sendGraphs                       ), DEFAULT_sendGraphs                      , "Send Graph/Chart data.          CmName of counters to send. '*' or 'all' means all CM's. '' or 'none' means do not send any.  Example: 'CmName1, CmName2'"));

		list.add( new CmSettingsHelper("http-header-1",                     key(PROPKEY_header1                          ), String .class, conf.getProperty       (key(PROPKEY_header1                          ), DEFAULT_header1                          ), DEFAULT_header1                         , "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-2",                     key(PROPKEY_header2                          ), String .class, conf.getProperty       (key(PROPKEY_header2                          ), DEFAULT_header2                          ), DEFAULT_header2                         , "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-3",                     key(PROPKEY_header3                          ), String .class, conf.getProperty       (key(PROPKEY_header3                          ), DEFAULT_header3                          ), DEFAULT_header3                         , "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-4",                     key(PROPKEY_header4                          ), String .class, conf.getProperty       (key(PROPKEY_header4                          ), DEFAULT_header4                          ), DEFAULT_header4                         , "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-5",                     key(PROPKEY_header5                          ), String .class, conf.getProperty       (key(PROPKEY_header5                          ), DEFAULT_header5                          ), DEFAULT_header5                         , "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-6",                     key(PROPKEY_header6                          ), String .class, conf.getProperty       (key(PROPKEY_header6                          ), DEFAULT_header6                          ), DEFAULT_header6                         , "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-7",                     key(PROPKEY_header7                          ), String .class, conf.getProperty       (key(PROPKEY_header7                          ), DEFAULT_header7                          ), DEFAULT_header7                         , "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-8",                     key(PROPKEY_header8                          ), String .class, conf.getProperty       (key(PROPKEY_header8                          ), DEFAULT_header8                          ), DEFAULT_header8                         , "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-9",                     key(PROPKEY_header9                          ), String .class, conf.getProperty       (key(PROPKEY_header9                          ), DEFAULT_header9                          ), DEFAULT_header9                         , "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));

		return list;
	}
}
