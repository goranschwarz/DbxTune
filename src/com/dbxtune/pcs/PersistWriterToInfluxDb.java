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
import java.util.Base64;
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
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.CountersModelAppend;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.pcs.sqlcapture.SqlCaptureDetails;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.HtmlQueryString;
import com.dbxtune.utils.HttpUtils;
import com.dbxtune.utils.Memory;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

/**
 * Write a sample to InfluxDB using the REST api
 * 
 * @author gorans
 *
 */
public class PersistWriterToInfluxDb 
extends PersistWriterBase
//implements IPersistWriter
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private String _srvName;
	
	/** Configuration, if we should convert the Local Time into UTC */
//	private boolean _convertSampleTimeToUtc = true;
	
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
		String msgStr = "";
		
		// Send to the mandatory config
		try
		{
			// First send old entries in the error queue (no exception is thrown)
			_confSlot0.sendErrorQueue(cont);

			// Construct and send Msg
//			msgStr = toMsg(cont, _confSlot0._sendCounters, _confSlot0._sendGraphs);
			msgStr = toMsg(cont, _confSlot0);
			sendMessage(msgStr, _confSlot0);
		}
		catch (ConnectException ex) 
		{
			_logger.error("Problems connecting/sending REST call to '"+_confSlot0._urlLastUsed+"'. The entry will be saved in the 'error-queue' and sent later. Caught: "+ex);
			_confSlot0.addToErrorQueue(cont, msgStr);
			
			// if destination has been down for some time: Send Alarm ?
			checkSendAlarm(_confSlot0);
		}
		catch (Exception ex)
		{
			_logger.error("Problems creating MSG or sending REST call to '"+_confSlot0._urlLastUsed+"'. Caught: "+ex , ex);
		}

		// Loop over the "extra" configurations and send to *each* destination
		for (ConfigSlot slot : _confSlots)
		{
			try
			{
				// First send old entries in the error queue (no exception is thrown)
				slot.sendErrorQueue(cont);
				
				// Construct and send Msg
//				msgStr = toMsg(cont, slot._sendCounters, slot._sendGraphs);
				msgStr = toMsg(cont, slot);
				sendMessage(msgStr, slot);
			}
			catch (ConnectException ex) 
			{
				_logger.error("Problems connecting/sending REST call to '"+slot._urlLastUsed+"'. The entry will be saved in the 'error-queue' and sent later. Caught: "+ex);
				slot.addToErrorQueue(cont, msgStr);

				// if destination has been down for some time: Send Alarm ?
				checkSendAlarm(slot);
			}
			catch (Exception ex)
			{
				_logger.error("Problems creating MSG or sending REST call to '"+slot._urlLastUsed+"'. Caught: "+ex , ex);
			}
		}
	}

	/** Check if we should send alarm, and if so: send the alarm */
	private void checkSendAlarm(ConfigSlot slot)
	{
		AlarmHandler alarmHandler = AlarmHandler.getInstance();

		// NO Alarm Handler == NO WORK
		if ( alarmHandler == null )
			return;

		long lastSendSuccessInSec = (System.currentTimeMillis() - slot._lastSendSuccess) / 1000;

		// Is it time to send Alarm
		if (lastSendSuccessInSec > slot._errorSendAlarmThresholdInSec)
		{
			String sourceSrvName = Configuration.getCombinedConfiguration().getProperty("SERVERNAME", "srv-"+slot._cfgName);

			AlarmEvent alarmEvent = new AlarmEventHttpDestinationDown(sourceSrvName, slot._cfgName, slot._urlLastUsed, lastSendSuccessInSec, slot._errorSendAlarmThresholdInSec);

			// Set the time to live
			// But why ????
			// IF:   the "other" PCS Writers takes a long time (longer than the "sample interval" in the Collector thread
			// THEN: It will not see any Alarms from here... so it will see that as a "CANCEL" (since no re-rease has happened)
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
	
//	/**
//	 * Null safe way to do obj.toString()
//	 *  
//	 * @param obj The object
//	 * @return obj.toString() or if (null if the inout is null
//	 */
//	private String toString(Object obj)
//	{
//		return obj == null ? null : obj.toString(); 
//	}
//
//	/**
//	 * Null safe way to do obj.toString()
//	 *  
//	 * @param obj The object
//	 * @return obj.toString() or if (null if the inout is null
//	 */
//	private BigDecimal toBigDec(Number num)
//	{
//		return num == null ? null : new BigDecimal(num.toString()); 
//	}


	/**
	 * Wrapper around toJsonWork...<br>
	 * In here we catch OutOfMemory errors, tries to do cleanup, and try again...
	 * 
	 * @param cont
	 * @param slot 
	 * @param writeCounters
	 * @param writeGraphs
	 * @return
	 * @throws IOException
	 */
//	private String toMsg(PersistContainer cont, boolean writeCounters, boolean writeGraphs)
	private String toMsg(PersistContainer cont, ConfigSlot slot)
	throws IOException
	{
		try
		{
			long startTime = System.currentTimeMillis();

			_srvName = cont.getServerNameOrAlias();
			
//			String msg = createMessage(cont, writeCounters, writeGraphs);
			String msg = createMessage(cont, slot);

			long createTime = TimeUtils.msDiffNow(startTime);
			getStatistics().setLastCreateJsonTimeInMs(createTime);
			
			// For debugging - set the last InfluxDB Message TimeStamp, then it will be visible in the Statistics written.
			getStatistics().setLastSentTimestamp(slot._lastSentTimestamp);

			return msg;
		}
		catch (IOException ex)
		{
			throw ex;
		}
		catch (OutOfMemoryError oom)
		{
			_logger.warn("Caught OutOfMemoryError when generating Msg text. Call the Memory.evaluate(), which should do cleanup of registered components. Then we try to generate the Msg Text again.");

			// Do memory cleanup
			Memory.evaluateAndWait(10*1000); // wait for max 10 sec
			
			// Try again
//			return createMessage(cont, writeCounters, writeGraphs);
			return createMessage(cont, slot);
		}
	}
	
	/**
	 * Write Msg for the container
	 * 
	 * @param cont           The container
	 * @param slot 
	 * @param writeCounters  if we should write COUNTER information
	 * @param writeGraphs    if we should write GRAPH/CHART information
	 * @return A Msg String
	 * @throws IOException 
	 */
//	private String createMessage(PersistContainer cont, boolean writeCounters, boolean writeGraphs)
	private String createMessage(PersistContainer cont, ConfigSlot slot)
	throws IOException
	{
		StringBuilder sb = new StringBuilder();
		
		//--------------------------------------
		// COUNTERS
		//--------------------------------------
		for (CountersModel cm : cont.getCounterObjects())
		{
			// For the moment do not send Append Models
			if (cm instanceof CountersModelAppend) 
				continue;
			
			if ( ! cm.hasValidSampleData() )
				continue;

			if ( ! cm.hasData() && ! cm.hasTrendGraphData() )
				continue;

			// if we ONLY write graph data, but there is no graphs with data
//			if ( writeCounters == false  &&  writeGraphs  &&  cm.getTrendGraphCountWithData() == 0)
			if (cm.getTrendGraphCountWithData() == 0)
				continue;
			
			//cm.toJson(w, writeCounters, writeGraphs);
			addTimestampMsg(sb, cm, slot);
		}

////			System.out.println(sw.toString());
//		File toFileName = new File("c:\\tmp\\PersistWriterToInfluxDb.tmp.influxdb");
//System.out.println("Writing Msg to file: "+toFileName.getAbsolutePath());
//
		String msgStr = sb.toString();
//System.out.println(msgStr);
//		FileUtils.writeStringToFile(toFileName, msgStr);
		
//		System.out.println("#### END Msg #######################################################################");

		return msgStr;
	}

	/**
	 * Escapse some special chars in the InfluxDb tag's and field's
	 * @param name
	 * @return
	 */
	private String escapeChars(String val)
	{
		if (val == null)
			return "";
		
		// If nothing to escape... get out of here
		if ( ! (val.indexOf(' ') != -1  || val.indexOf(',') != -1 || val.indexOf('=') != -1) )	
			return val;

		StringBuilder sb = new StringBuilder(val.length() + 10);

		int len = val.length();
		for (int i=0; i<len; i++)
		{
			char ch = val.charAt(i);

			if      (ch == ' ') { sb.append("\\ "); }
			else if (ch == ',') { sb.append("\\,"); }
			else if (ch == '=') { sb.append("\\="); }
			else                { sb.append(ch   ); }
		}

		return sb.toString();
	}

//	/**
//	 * Convert a local timezone <i>long</i> timestamp "long" into the "long" UTC value
//	 * @param timestamp
//	 * @return
//	 */
//	public static long toUTC(long timestamp)
//	{
//		Calendar cal    = Calendar.getInstance();
//		int      offset = cal.getTimeZone().getOffset(timestamp);
//		return timestamp - offset;
//	}

	/**
	 * Add one InfluxDB record for every Chart entry
	 * <pre>
	 *    graphName field_key=field_value[,field_key2=field_value2] timestamp
	 * </pre>
	 * @param sb
	 * @param cm
	 * @param slot 
	 * @throws IOException
	 */
	private int addTimestampMsg(StringBuilder sb, CountersModel cm, ConfigSlot slot)
	{
		int rowsAdded = 0;
		
		// note use 'hasTrendGraphData()' and NOT 'hasTrendGraph()' which is only true in GUI mode
		if ( ! cm.hasTrendGraphData() )
			return rowsAdded;
			
		// https://docs.influxdata.com/influxdb/v0.9/write_protocols/write_syntax/
		
		// measurement[,tag_key1=tag_value1...] field_key=field_value[,field_key2=field_value2] [timestamp]
		
		// Escaping Characters
		//    If a tag key, tag value, or field key contains a space , comma ,, or an equals sign = it must be escaped 
		//    using the backslash character \. Backslash characters do not need to be escaped. Commas , and spaces will 
		//    also need to be escaped for measurements, though equals signs = do not.
		String ROW_TERM = "\n";
		String SEP      = " ";
		
		for (String graphName : cm.getTrendGraphData().keySet())
		{
			TrendGraphDataPoint tgdp = cm.getTrendGraphData(graphName);
			
			String longGraphName = cm.getName() + "_" + graphName;
			
			// Do not write empty graphs
			if ( ! tgdp.hasData() )
				continue;

			String keyValues = "";
			
			Double[] dataArr  = tgdp.getData();
			String[] labelArr = tgdp.getLabel();
			if (dataArr != null && labelArr != null)
			{
				for (int d=0; d<dataArr.length; d++)
				{
					Double data  = dataArr[d];
					String label = null;
					if (d < labelArr.length)
						label = labelArr[d];

					if (label == null)
					{
						if (_logger.isDebugEnabled())
							_logger.debug("Writing InfluxDB Graph, LABEL was null, setting it to 'lbl-"+d+"'. For cm='"+getName()+"', longGraphName='"+longGraphName+"', label='"+label+"', data="+data);
						label = "lbl-"+d;
					}

					if (data == null)
					{
						if (_logger.isDebugEnabled())
							_logger.debug("Writing InfluxDB Graph, DATA was null, setting it to 0. For cm='"+getName()+"', longGraphName='"+longGraphName+"', label='"+label+"', data="+data);
						data = 0d;
					}

					keyValues += (d > 0 ? "," : "") + escapeChars(label) + "=" + data;
				}
			}
			
			if (StringUtil.hasValue(keyValues))
			{
				String tagValues = "";

				if (slot._addMetaDataAsTags)
				{
					StringBuilder tagSb = new StringBuilder(100);

					tagSb.append( ",label="          ).append( escapeChars(tgdp.getGraphLabel()) );
					tagSb.append( ",category="       ).append( escapeChars(tgdp.getCategory().toString()) );
					tagSb.append( ",percentGraph="   ).append( tgdp.isPercentGraph() );
					tagSb.append( ",visibleAtStart=" ).append( tgdp.isVisibleAtStart() );
					tagSb.append( ",props="          ).append( escapeChars(tgdp.getGraphProps()) );
					
					tagValues = tagSb.toString();
					
					// Below is from: CountersModel.toJson()
//					w.writeStringField ("graphName" ,     tgdp.getName());
//					w.writeStringField ("graphLabel",     tgdp.getGraphLabel());
//					w.writeStringField ("graphProps",     tgdp.getGraphProps());
//					w.writeStringField ("graphCategory",  tgdp.getCategory().toString());
//					w.writeBooleanField("percentGraph",   tgdp.isPercentGraph());
//					w.writeBooleanField("visibleAtStart", tgdp.isVisibleAtStart());
				}
				
				long ts = tgdp.getDate().getTime();
//				if (slot._convertSampleTimeToUtc)
//				{
//					ts = toUTC(ts);
//				}

				// WRITE FORMAT: measurement[,tag_key1=tag_value1...] field_key=field_value[,field_key2=field_value2] [timestamp]
				// note the spaces (at the ^)                        ^                                               ^   
				sb.append(longGraphName).append(tagValues).append(SEP); // or tgdp.getName()
				sb.append(keyValues).append(SEP);
				sb.append(ts);   // NOTE: This is in milliseconds... do we need to add 6 zero or not???
				sb.append(ROW_TERM);
				
				slot._lastSentTimestamp = ts;
				rowsAdded++;
			}
		}
		return rowsAdded;
	}

	private boolean createDatabase(String dbname, ConfigSlot slot)
	throws Exception
	{
		if (StringUtil.isNullOrBlank(dbname))
		{
			_logger.error("Create InfluxDB dbname cant be null or blank.");
			return false;
		}

//		Create a database
//		$ curl -XPOST 'http://localhost:8086/query' --data-urlencode 'q=CREATE DATABASE "mydb"'
//
//		{"results":[{"statement_id":0}]}

		HtmlQueryString qs = new HtmlQueryString(slot._url + "/query");
		qs.add("q", "CREATE DATABASE \"" + dbname + "\"");
		
		String urlStr = qs.toString();
		URL url = new URL(urlStr);

		// Set for debugging purposes
//		slot._urlLastUsed = url.toString();
		slot._urlLastUsed = urlStr;

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");

		// Basic Authentication
		if (StringUtil.hasValue(slot._username))
		{
			String userpass = slot._username + ":" + slot._password;
			String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
			conn.setRequestProperty("Authorization", basicAuth);
		}

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

		// Check response
		int responceCode = conn.getResponseCode();
		if ( responceCode >= 203) // see 'https://httpstatuses.com/' for http codes... or at the bottom of this source code
		{
			throw new Exception("Create InfluxDB database '" + dbname + "': Failed : HTTP error code : " + responceCode + " ("+HttpUtils.httpResponceCodeToText(responceCode)+"). From URL '"+slot._urlLastUsed+"'.");
		}
		else
		{
			_logger.debug("Responce code "+responceCode+" ("+HttpUtils.httpResponceCodeToText(responceCode)+"). From URL '"+slot._urlLastUsed+"'.");
		}

		// Read response and print the output...
		BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

		String outputRow;
		StringBuilder sb = new StringBuilder();
		while ((outputRow = br.readLine()) != null)
		{
			sb.append(outputRow);
		}
		String output = sb.toString().trim();
		_logger.info("Create InfluxDB database '" + dbname + "': Responce from server: " + output);
		_logger.debug("Responce from server: " + output);
		
		conn.disconnect();
		
		return true;		
	}
	/**
	 * Here is where the send happens
	 * @param action
	 * @param alarmEvent
	 * @return how many entries are there in the server side queue. -1 if it's unknown.
	 * @throws Exception 
	 */
	private int sendMessage(String text, ConfigSlot slot) 
	throws Exception
	{
		return sendMessage_internal(text, slot, false);
	}
	private int sendMessage_internal(String text, ConfigSlot slot, boolean recursiveCall) 
	throws Exception
	{
		if (StringUtil.isNullOrBlank(_srvName))
		{
			_logger.error("sendMessage(): InfluxDB dbname cant be null or blank.");
			return 0;
		}

		HtmlQueryString qs = new HtmlQueryString(slot._url + "/write");
//		qs.add("db",        "\"" + _srvName + "\"");
		qs.add("db",        _srvName);  // No quotes should be in the URL, strange chars will be decoded
		qs.add("precision", "ms");
		
		String urlStr = qs.toString();
		URL url = new URL(urlStr);

		// Set for debugging purposes
//		slot._urlLastUsed = url.toString();
		slot._urlLastUsed = urlStr;

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");

		// Binary send
		conn.setRequestProperty("Content-Type", "binary/octet-stream");

		// Basic Authentication
		if (StringUtil.hasValue(slot._username) && StringUtil.hasValue(slot._password))
		{
			String userpass = slot._username + ":" + slot._password;
			String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
			conn.setRequestProperty("Authorization", basicAuth);
		}

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
		if (responceCode == 204)
		{
			// SUCCESS
		}
		else if (responceCode == 404 && !recursiveCall)
		{
			// HTTP/1.1 404 Not Found
			// {"error":"database not found: \"mydb1\""}
			
			_logger.warn("Database '"+_srvName+"' NOT FOUND in InfluxDB... Lets try to create it.  INFO: Responce code "+responceCode+" ("+HttpUtils.httpResponceCodeToText(responceCode)+"). From URL '"+slot._urlLastUsed+"'.");

			// If Create database SUCCEEDS, call this method agin... to retry sending the message...
			if ( createDatabase(_srvName, slot) )
			{
				// Disconnect CURRENT connection
				conn.disconnect();

				// Resends the message
				sendMessage_internal(text, slot, true);

				// Get out of here
				return 0;
			}
		}
		else if ( responceCode >= 203) // see 'https://httpstatuses.com/' for http codes... or at the bottom of this source code
		{
			throw new Exception("Failed : HTTP error code : " + responceCode + " ("+HttpUtils.httpResponceCodeToText(responceCode)+"). From URL '"+slot._urlLastUsed+"'. Sent Msg content.length: "+text.length());
		}
		else
		{
			int threshold = 1000;
			if (sendTime > threshold)
				_logger.warn("HTTP REST Call took longer than expected. sendTimeInMs = "+sendTime+", which is above threshold="+threshold+", Responce code "+responceCode+" ("+HttpUtils.httpResponceCodeToText(responceCode)+"). From URL '"+slot._urlLastUsed+"'. Sent Msg content.length: "+text.length()+", "+(text.length()/1024)+" KB");

			_logger.debug("Responce code "+responceCode+" ("+HttpUtils.httpResponceCodeToText(responceCode)+"). From URL '"+slot._urlLastUsed+"'. Sent Msg content.length: "+text.length()+", "+(text.length()/1024)+" KB");
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
		if (StringUtil.hasValue(output))
			_logger.info("Send to InfluxDB database '" + _srvName + "': responceCode=" + responceCode + ", Responce from server: " + output);
		
		conn.disconnect();
		
		return 0;
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
		int spaces = 55;
		_logger.info("Configuration for Persist Writer Module: "+getName());
		_logger.info("    " + StringUtil.left(key(PROPKEY_url                              ), spaces) + ": " + _confSlot0._url);

		_logger.info("    " + StringUtil.left(key(PROPKEY_username                         ), spaces) + ": " + _confSlot0._username);
		_logger.info("    " + StringUtil.left(key(PROPKEY_password                         ), spaces) + ": " + _confSlot0._password);
//		_logger.info("    " + StringUtil.left(key(PROPKEY_asUtcTime                        ), spaces) + ": " + _confSlot0._convertSampleTimeToUtc);
		_logger.info("    " + StringUtil.left(key(PROPKEY_addMetaTags                      ), spaces) + ": " + _confSlot0._addMetaDataAsTags);

		_logger.info("    " + StringUtil.left(key(PROPKEY_errorSendAlarm                   ), spaces) + ": " + _confSlot0._errorSendAlarm);
		_logger.info("    " + StringUtil.left(key(PROPKEY_errorSendAlarmThresholdInSec     ), spaces) + ": " + _confSlot0._errorSendAlarmThresholdInSec);

		_logger.info("    " + StringUtil.left(key(PROPKEY_errorSaveToDisk                  ), spaces) + ": " + _confSlot0._errorSaveToDisk);
		_logger.info("    " + StringUtil.left(key(PROPKEY_errorSaveToDiskPath              ), spaces) + ": " + _confSlot0._errorSaveToDiskPath);
		_logger.info("    " + StringUtil.left(key(PROPKEY_errorSaveToDiskDiscardAfterXDays ), spaces) + ": " + _confSlot0._errorSaveToDiskDiscardAfterXDays);
		_logger.info("    " + StringUtil.left(key(PROPKEY_errorSaveToDiskSuccessSleepTimeMs), spaces) + ": " + _confSlot0._errorSaveToDiskSuccessSleepTimeMs);

		_logger.info("    " + StringUtil.left(key(PROPKEY_errorMemQueueSize                ), spaces) + ": " + _confSlot0._errorMemQueueSize);

//		_logger.info("    " + StringUtil.left(key(PROPKEY_sendCounters                     ), spaces) + ": " + _confSlot0._sendCounters);
//		_logger.info("    " + StringUtil.left(key(PROPKEY_sendGraphs                       ), spaces) + ": " + _confSlot0._sendGraphs);
		
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
			if (StringUtil.hasValue(slot._url     )) _logger.info("    " + StringUtil.left(key(PROPKEY_url                              , slot._cfgName), spaces) + ": " + slot._url);

			if (StringUtil.hasValue(slot._url     )) _logger.info("    " + StringUtil.left(key(PROPKEY_username                         , slot._cfgName), spaces) + ": " + slot._username);
			if (StringUtil.hasValue(slot._url     )) _logger.info("    " + StringUtil.left(key(PROPKEY_password                         , slot._cfgName), spaces) + ": " + slot._password);
//			if (StringUtil.hasValue(slot._url     )) _logger.info("    " + StringUtil.left(key(PROPKEY_asUtcTime                        , slot._cfgName), spaces) + ": " + slot._convertSampleTimeToUtc);
			if (StringUtil.hasValue(slot._url     )) _logger.info("    " + StringUtil.left(key(PROPKEY_addMetaTags                      , slot._cfgName), spaces) + ": " + slot._addMetaDataAsTags);

			if (StringUtil.hasValue(slot._url     )) _logger.info("    " + StringUtil.left(key(PROPKEY_errorSendAlarm                   , slot._cfgName), spaces) + ": " + slot._errorSendAlarm);
			if (StringUtil.hasValue(slot._url     )) _logger.info("    " + StringUtil.left(key(PROPKEY_errorSendAlarmThresholdInSec     , slot._cfgName), spaces) + ": " + slot._errorSendAlarmThresholdInSec);

			if (StringUtil.hasValue(slot._url     )) _logger.info("    " + StringUtil.left(key(PROPKEY_errorSaveToDisk                  , slot._cfgName), spaces) + ": " + slot._errorSaveToDisk);
			if (StringUtil.hasValue(slot._url     )) _logger.info("    " + StringUtil.left(key(PROPKEY_errorSaveToDiskPath              , slot._cfgName), spaces) + ": " + slot._errorSaveToDiskPath);
			if (StringUtil.hasValue(slot._url     )) _logger.info("    " + StringUtil.left(key(PROPKEY_errorSaveToDiskDiscardAfterXDays , slot._cfgName), spaces) + ": " + slot._errorSaveToDiskDiscardAfterXDays);
			if (StringUtil.hasValue(slot._url     )) _logger.info("    " + StringUtil.left(key(PROPKEY_errorSaveToDiskSuccessSleepTimeMs, slot._cfgName), spaces) + ": " + slot._errorSaveToDiskSuccessSleepTimeMs);

			if (StringUtil.hasValue(slot._url     )) _logger.info("    " + StringUtil.left(key(PROPKEY_errorMemQueueSize                , slot._cfgName), spaces) + ": " + slot._errorMemQueueSize);

//			if (StringUtil.hasValue(slot._url     )) _logger.info("    " + StringUtil.left(key(PROPKEY_sendCounters                     , slot._cfgName), spaces) + ": " + slot._sendCounters);
//			if (StringUtil.hasValue(slot._url     )) _logger.info("    " + StringUtil.left(key(PROPKEY_sendGraphs                       , slot._cfgName), spaces) + ": " + slot._sendGraphs);
                                                                                                                                                              
			if (StringUtil.hasValue(slot._header_1)) _logger.info("    " + StringUtil.left(key(PROPKEY_header1                          , slot._cfgName), spaces) + ": " + slot._header_1);
			if (StringUtil.hasValue(slot._header_2)) _logger.info("    " + StringUtil.left(key(PROPKEY_header2                          , slot._cfgName), spaces) + ": " + slot._header_2);
			if (StringUtil.hasValue(slot._header_3)) _logger.info("    " + StringUtil.left(key(PROPKEY_header3                          , slot._cfgName), spaces) + ": " + slot._header_3);
			if (StringUtil.hasValue(slot._header_4)) _logger.info("    " + StringUtil.left(key(PROPKEY_header4                          , slot._cfgName), spaces) + ": " + slot._header_4);
			if (StringUtil.hasValue(slot._header_5)) _logger.info("    " + StringUtil.left(key(PROPKEY_header5                          , slot._cfgName), spaces) + ": " + slot._header_5);
			if (StringUtil.hasValue(slot._header_6)) _logger.info("    " + StringUtil.left(key(PROPKEY_header6                          , slot._cfgName), spaces) + ": " + slot._header_6);
			if (StringUtil.hasValue(slot._header_7)) _logger.info("    " + StringUtil.left(key(PROPKEY_header7                          , slot._cfgName), spaces) + ": " + slot._header_7);
			if (StringUtil.hasValue(slot._header_8)) _logger.info("    " + StringUtil.left(key(PROPKEY_header8                          , slot._cfgName), spaces) + ": " + slot._header_8);
			if (StringUtil.hasValue(slot._header_9)) _logger.info("    " + StringUtil.left(key(PROPKEY_header9                          , slot._cfgName), spaces) + ": " + slot._header_9);
		}
	}

	private void checkOrCreateRecoveryDir(ConfigSlot configSlot)
	{
		if (configSlot == null)
			return;
		
		if ( ! configSlot._errorSaveToDisk )
			return;

		// If dir do NOT exist, create the dir
		File dir = new File(configSlot._errorSaveToDiskPath);
		if ( ! dir.exists() )
		{
			if ( dir.mkdirs() )
			{
				_logger.info("Created the recovery directory '"+configSlot._errorSaveToDiskPath+"' for configuration '"+configSlot._cfgName+"'.");
			}
		}
	}

	@Override
	public void init(Configuration conf) throws Exception
	{
		System.out.println(getName()+": INIT.....................................");

		_conf = conf;

		_logger.info("Initializing the PersistWriter component named '"+getName()+"'.");

		_confSlot0._url            = conf.getProperty       (key(PROPKEY_url              ), DEFAULT_url);
		_confSlot0._cfgName        = "slot0";
		if (_confSlot0._url.endsWith(":8086"))
			_confSlot0._cfgName    = "InfluxDB";

		_confSlot0._username                          = conf.getProperty       (key(PROPKEY_username                         ), DEFAULT_username);
		_confSlot0._password                          = conf.getProperty       (key(PROPKEY_password                         ), DEFAULT_password);
//		_confSlot0._convertSampleTimeToUtc            = conf.getBooleanProperty(key(PROPKEY_asUtcTime                        ), DEFAULT_asUtcTime);
		_confSlot0._addMetaDataAsTags                 = conf.getBooleanProperty(key(PROPKEY_addMetaTags                      ), DEFAULT_addMetaTags);
		
		_confSlot0._errorSendAlarm                    = conf.getBooleanProperty(key(PROPKEY_errorSendAlarm                   ), DEFAULT_errorSendAlarm);
		_confSlot0._errorSendAlarmThresholdInSec      = conf.getIntProperty    (key(PROPKEY_errorSendAlarmThresholdInSec     ), DEFAULT_errorSendAlarmThresholdInSec);

		_confSlot0._errorSaveToDisk                   = conf.getBooleanProperty(key(PROPKEY_errorSaveToDisk                  ), DEFAULT_errorSaveToDisk);
		_confSlot0._errorSaveToDiskPath               = conf.getProperty       (key(PROPKEY_errorSaveToDiskPath              ), DEFAULT_errorSaveToDiskPath);
		_confSlot0._errorSaveToDiskDiscardAfterXDays  = conf.getIntProperty    (key(PROPKEY_errorSaveToDiskDiscardAfterXDays ), DEFAULT_errorSaveToDiskDiscardAfterXDays);
		_confSlot0._errorSaveToDiskSuccessSleepTimeMs = conf.getIntProperty    (key(PROPKEY_errorSaveToDiskSuccessSleepTimeMs), DEFAULT_errorSaveToDiskSuccessSleepTimeMs);

		_confSlot0._errorMemQueueSize                 = conf.getIntProperty    (key(PROPKEY_errorMemQueueSize                ), DEFAULT_errorMemQueueSize);
                                                                                                                             
//		_confSlot0._sendCounters                      = conf.getBooleanProperty(key(PROPKEY_sendCounters                     ), DEFAULT_sendCounters);
//		_confSlot0._sendGraphs                        = conf.getBooleanProperty(key(PROPKEY_sendGraphs                       ), DEFAULT_sendGraphs);
                                                                                                                             
		_confSlot0._header_1                          = conf.getProperty       (key(PROPKEY_header1                          ), DEFAULT_header1);
		_confSlot0._header_2                          = conf.getProperty       (key(PROPKEY_header2                          ), DEFAULT_header2);
		_confSlot0._header_3                          = conf.getProperty       (key(PROPKEY_header3                          ), DEFAULT_header3);
		_confSlot0._header_4                          = conf.getProperty       (key(PROPKEY_header4                          ), DEFAULT_header4);
		_confSlot0._header_5                          = conf.getProperty       (key(PROPKEY_header5                          ), DEFAULT_header5);
		_confSlot0._header_6                          = conf.getProperty       (key(PROPKEY_header6                          ), DEFAULT_header6);
		_confSlot0._header_7                          = conf.getProperty       (key(PROPKEY_header7                          ), DEFAULT_header7);
		_confSlot0._header_8                          = conf.getProperty       (key(PROPKEY_header8                          ), DEFAULT_header8);
		_confSlot0._header_9                          = conf.getProperty       (key(PROPKEY_header9                          ), DEFAULT_header9);

		checkOrCreateRecoveryDir(_confSlot0);
		
		// Read "extra" configurations
		String keyStr = conf.getProperty(PROPKEY_configKeys, DEFAULT_configKeys);
		List<String> keyList = StringUtil.commaStrToList(keyStr);
		
		for (String cfgKey : keyList)
		{
			String url                                = conf.getProperty       (key(PROPKEY_url                              , cfgKey), null);
                                   
			String  username                          = conf.getProperty       (key(PROPKEY_username                         , cfgKey), DEFAULT_username);
			String  password                          = conf.getProperty       (key(PROPKEY_password                         , cfgKey), DEFAULT_password);
//			boolean asUtcTime                         = conf.getBooleanProperty(key(PROPKEY_asUtcTime                        , cfgKey), DEFAULT_asUtcTime);
			boolean addMetaTags                       = conf.getBooleanProperty(key(PROPKEY_addMetaTags                      , cfgKey), DEFAULT_addMetaTags);

			boolean errorSendAlarm                    = conf.getBooleanProperty(key(PROPKEY_errorSendAlarm                   , cfgKey), DEFAULT_errorSendAlarm);
			int     errorSendAlarmThresholdInSec      = conf.getIntProperty    (key(PROPKEY_errorSendAlarmThresholdInSec     , cfgKey), DEFAULT_errorSendAlarmThresholdInSec);

			boolean errorSaveToDisk                   = conf.getBooleanProperty(key(PROPKEY_errorSaveToDisk                  , cfgKey), DEFAULT_errorSaveToDisk);
			String  errorSaveToDiskPath               = conf.getProperty       (key(PROPKEY_errorSaveToDiskPath              , cfgKey), DEFAULT_errorSaveToDiskPath);
			int     errorSaveToDiskDiscardAfterXDays  = conf.getIntProperty    (key(PROPKEY_errorSaveToDiskDiscardAfterXDays , cfgKey), DEFAULT_errorSaveToDiskDiscardAfterXDays);
			int     errorSaveToDiskSuccessSleepTimeMs = conf.getIntProperty    (key(PROPKEY_errorSaveToDiskSuccessSleepTimeMs, cfgKey), DEFAULT_errorSaveToDiskSuccessSleepTimeMs);

			int    errorMemQueueSize                  = conf.getIntProperty    (key(PROPKEY_errorMemQueueSize                , cfgKey), DEFAULT_errorMemQueueSize);
                                                                                                                             
//			boolean sendCounters                      = conf.getBooleanProperty(key(PROPKEY_sendCounters                     , cfgKey), DEFAULT_sendCounters);
//			boolean sendGraphs                        = conf.getBooleanProperty(key(PROPKEY_sendGraphs                       , cfgKey), DEFAULT_sendGraphs  );
			                                                                                                                 
			String header_1                           = conf.getProperty       (key(PROPKEY_header1                          , cfgKey), null);
			String header_2                           = conf.getProperty       (key(PROPKEY_header2                          , cfgKey), null);
			String header_3                           = conf.getProperty       (key(PROPKEY_header3                          , cfgKey), null);
			String header_4                           = conf.getProperty       (key(PROPKEY_header4                          , cfgKey), null);
			String header_5                           = conf.getProperty       (key(PROPKEY_header5                          , cfgKey), null);
			String header_6                           = conf.getProperty       (key(PROPKEY_header6                          , cfgKey), null);
			String header_7                           = conf.getProperty       (key(PROPKEY_header7                          , cfgKey), null);
			String header_8                           = conf.getProperty       (key(PROPKEY_header8                          , cfgKey), null);
			String header_9                           = conf.getProperty       (key(PROPKEY_header9                          , cfgKey), null);
			
			if (StringUtil.isNullOrBlank(url))
			{
				_logger.warn("When getting configuration for config '"+cfgKey+"' using property '"+key(PROPKEY_url, cfgKey)+"' no value for URL was found. Skipping this section.");
			}
			else
			{
				// Add a SLOT to the list
				ConfigSlot slot = new ConfigSlot();

				slot._cfgName                           = cfgKey;
				slot._url                               = url;

				slot._username                          = username;
				slot._password                          = password;
//				slot._convertSampleTimeToUtc            = asUtcTime;

				slot._errorSendAlarm                    = errorSendAlarm;
				slot._errorSendAlarmThresholdInSec      = errorSendAlarmThresholdInSec;
				
				slot._errorSaveToDisk                   = errorSaveToDisk;
				slot._errorSaveToDiskPath               = errorSaveToDiskPath;
				slot._errorSaveToDiskDiscardAfterXDays  = errorSaveToDiskDiscardAfterXDays;
				slot._errorSaveToDiskSuccessSleepTimeMs = errorSaveToDiskSuccessSleepTimeMs;

				slot._errorMemQueueSize = errorMemQueueSize;

//				slot._sendCounters   = sendCounters;
//				slot._sendGraphs     = sendGraphs;
				
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

	public static final String  PROPKEY_configKeys                        = "PersistWriterToInfluxDb.config.keys";
	public static final String  DEFAULT_configKeys                        = null;

	public static final String  PROPKEY_url                               = "PersistWriterToInfluxDb.{KEY}.url";
//	public static final String  DEFAULT_url                               = null;
//	public static final String  DEFAULT_url                               = "http://localhost:8086";
	public static final String  DEFAULT_url                               = "http://192.168.0.110:8086";

	public static final String  PROPKEY_username                          = "PersistWriterToInfluxDb.{KEY}.username";
	public static final String  DEFAULT_username                          = "";

	public static final String  PROPKEY_password                          = "PersistWriterToInfluxDb.{KEY}.password";
	public static final String  DEFAULT_password                          = "";

//	public static final String  PROPKEY_asUtcTime                         = "PersistWriterToInfluxDb.{KEY}.convertSampleTimeToUtc";
////	public static final boolean DEFAULT_asUtcTime                         = true;  // true  = As to InfuxDB, all time stamps should be written as UTC, but that comes out wrong in my tests (I don't know what's wrong)
//	public static final boolean DEFAULT_asUtcTime                         = false; // false = Is a workaround... It seems that my InfluxDB converts the LocalTime to UTC... is it my InfluxDB or Grafana that is not properly configured...

	public static final String  PROPKEY_addMetaTags                       = "PersistWriterToInfluxDb.{KEY}.addGraphMetaDataAsTags";
	public static final boolean DEFAULT_addMetaTags                       = false;


	public static final String  PROPKEY_errorSendAlarm                    = "PersistWriterToInfluxDb.{KEY}.error.sendAlarm";
	public static final boolean DEFAULT_errorSendAlarm                    = true;

	public static final String  PROPKEY_errorSendAlarmThresholdInSec      = "PersistWriterToInfluxDb.{KEY}.error.sendAlarm.thresholdInSec";
//	public static final int     DEFAULT_errorSendAlarmThresholdInSec      = 60 * 30; // 30 minutes
	public static final int     DEFAULT_errorSendAlarmThresholdInSec      = 60 * 60; // 1 Hour


	public static final String  PROPKEY_errorSaveToDisk                   = "PersistWriterToInfluxDb.{KEY}.error.saveToDisk";
	public static final boolean DEFAULT_errorSaveToDisk                   = true;

	public static final String  PROPKEY_errorSaveToDiskPath               = "PersistWriterToInfluxDb.{KEY}.error.saveToDisk.path";
	public static final String  DEFAULT_errorSaveToDiskPath               = System.getProperty("java.io.tmpdir") + File.separatorChar + "DbxTune" + File.separatorChar + "PersistWriterToInfluxDb";

	public static final String  PROPKEY_errorSaveToDiskDiscardAfterXDays  = "PersistWriterToInfluxDb.{KEY}.error.saveToDisk.discard.after.days";
	public static final int     DEFAULT_errorSaveToDiskDiscardAfterXDays  = 1;

	public static final String  PROPKEY_errorSaveToDiskSuccessSleepTimeMs = "PersistWriterToInfluxDb.{KEY}.error.saveToDisk.success.sleep.time.ms";
	public static final int     DEFAULT_errorSaveToDiskSuccessSleepTimeMs = 2000;


	public static final String  PROPKEY_errorMemQueueSize                 = "PersistWriterToInfluxDb.{KEY}.error.in-memory.queue.size";
	public static final int     DEFAULT_errorMemQueueSize                 = 10;


//	public static final String  PROPKEY_sendCounters      = "PersistWriterToInfluxDb.{KEY}.send.counters";
//	public static final boolean DEFAULT_sendCounters      = false;
//
//	public static final String  PROPKEY_sendGraphs        = "PersistWriterToInfluxDb.{KEY}.send.graphs";
//	public static final boolean DEFAULT_sendGraphs        = true;

	public static final String  PROPKEY_header1           = "PersistWriterToInfluxDb.{KEY}.header.1";
//	public static final String  DEFAULT_header1           = null;
//	public static final String  DEFAULT_header1           = "Content-Type: application/json";
	public static final String  DEFAULT_header1           = "Content-Type: text/plain";

	public static final String  PROPKEY_header2           = "PersistWriterToInfluxDb.{KEY}.header.2";
	public static final String  DEFAULT_header2           = null;

	public static final String  PROPKEY_header3           = "PersistWriterToInfluxDb.{KEY}.header.3";
	public static final String  DEFAULT_header3           = null;

	public static final String  PROPKEY_header4           = "PersistWriterToInfluxDb.{KEY}.header.4";
	public static final String  DEFAULT_header4           = null;

	public static final String  PROPKEY_header5           = "PersistWriterToInfluxDb.{KEY}.header.5";
	public static final String  DEFAULT_header5           = null;

	public static final String  PROPKEY_header6           = "PersistWriterToInfluxDb.{KEY}.header.6";
	public static final String  DEFAULT_header6           = null;

	public static final String  PROPKEY_header7           = "PersistWriterToInfluxDb.{KEY}.header.7";
	public static final String  DEFAULT_header7           = null;

	public static final String  PROPKEY_header8           = "PersistWriterToInfluxDb.{KEY}.header.8";
	public static final String  DEFAULT_header8           = null;

	public static final String  PROPKEY_header9           = "PersistWriterToInfluxDb.{KEY}.header.9";
	public static final String  DEFAULT_header9           = null;

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
		String  _urlLastUsed       = ""; // set in sendMessage(), so we know what we called when printing error messages etc.
		long    _lastSentTimestamp = -1; // set in sendMessage(), Used to print debug message if we have problems with InfuxDB, Grafana - UTC and LocalTime translations 

		String  _username = DEFAULT_username;
		String  _password = DEFAULT_password;
//		boolean _convertSampleTimeToUtc = DEFAULT_asUtcTime;
		boolean _addMetaDataAsTags      = DEFAULT_addMetaTags;

		boolean _errorSendAlarm                    = DEFAULT_errorSendAlarm;
		int     _errorSendAlarmThresholdInSec      = DEFAULT_errorSendAlarmThresholdInSec;
		long    _lastSendSuccess                   = System.currentTimeMillis(); // used to determine if we should send alarm

		boolean _errorSaveToDisk                   = DEFAULT_errorSaveToDisk;
		String  _errorSaveToDiskPath               = DEFAULT_errorSaveToDiskPath;
		int     _errorSaveToDiskDiscardAfterXDays  = DEFAULT_errorSaveToDiskDiscardAfterXDays;
		int     _errorSaveToDiskSuccessSleepTimeMs = DEFAULT_errorSaveToDiskSuccessSleepTimeMs;
		
		int     _errorMemQueueSize = DEFAULT_errorMemQueueSize;
		LinkedList<String> _errorQueue = new LinkedList<>(); 

//		boolean _sendCounters   = DEFAULT_sendCounters;
//		boolean _sendGraphs     = DEFAULT_sendGraphs;
		
		String  _header_1 = "";
		String  _header_2 = "";
		String  _header_3 = "";
		String  _header_4 = "";
		String  _header_5 = "";
		String  _header_6 = "";
		String  _header_7 = "";
		String  _header_8 = "";
		String  _header_9 = "";

		/** All files should START with this */
		public static final String RECOVERY_FILE_PREFIX = "DbxTune.PersistWriterToInfluxDb.retry.";
		public static final String RECOVERY_FILE_SUFFIX = "influxdb";

		
		public ConfigSlot()
		{
		}

		/** 
		 * Add message to error queue, to be sent later 
		 */
		public void addToErrorQueue(PersistContainer cont, String msgStr)
		{
			if (StringUtil.isNullOrBlank(msgStr))
				return;

			try
			{
				// SAVE the Msg to a file and retry later
				if (_errorSaveToDisk)
				{
					checkOrCreateRecoveryDir(this);
					
					// Remove older error files
					removeOldErrorFiles(cont);
					
					// Maybe check if we have enough SPACE on: _errorSaveToDiskPath

					// init some helper variables
					String ts  = new SimpleDateFormat("yyyy-MM-dd.HH_mm_ss_SSS").format( new Date(System.currentTimeMillis()) );
					String srv = cont.getServerNameOrAlias(); 
					File   f   = new File(_errorSaveToDiskPath + File.separatorChar + RECOVERY_FILE_PREFIX + srv + "." + _cfgName + "." + ts + "." + RECOVERY_FILE_SUFFIX);

					_logger.info("addToErrorQueue(SAVE-TO-DISK): cfgName='"+_cfgName+"'. Saving to file: "+f);
					try
					{
						FileUtils.write(f, msgStr, StandardCharsets.UTF_8);
					}
					catch (IOException ex)
					{
						_logger.error("addToErrorQueue(SAVE-TO-DISK): cfgName='"+_cfgName+"'. Error when saving to file '"+f+"'. Caught: "+ex, ex);
					}
				}
				// IN-MEMORY error queue
				else
				{
					_errorQueue.addLast(msgStr);
					
					while (_errorQueue.size() > _errorMemQueueSize)
					{
						_errorQueue.removeFirst();
						_logger.info("addToErrorQueue(IN-MEMORY): Removing 'oldest' entry in the ErrorQueue for config name '"+_cfgName+"'. _errorQueue.size()="+_errorQueue.size()+", maxEntries="+_errorMemQueueSize);
					}
				}
			}
			catch (RuntimeException ex)
			{
				_logger.error("Runtime Problems in: addToErrorQueue(), cfgName='"+_cfgName+"', continuing anyway... Caught: " + ex, ex);
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
					//Collection<File> filesColl = FileUtils.listFiles(new File(_errorSaveToDiskPath), new WildcardFileFilter(RECOVER_FILE_PREFIX + srv + "." + _cfgName + ".*"), TrueFileFilter.TRUE); // maybe... but not tested.
					Collection<File> filesColl = FileUtils.listFiles(new File(_errorSaveToDiskPath), new String[] {RECOVERY_FILE_SUFFIX}, false);
					if ( ! filesColl.isEmpty() )
					{
						// Make an array, and sort it...
						File[] filesArr = FileUtils.convertFileCollectionToFileArray(filesColl);
						Arrays.sort(filesArr, NameFileComparator.NAME_COMPARATOR);
						
						// loop all files (skip files that "someone else" is responsible for)
						for (File file : filesArr )
						{
							String filename = file.getName();
							if (filename.startsWith(RECOVERY_FILE_PREFIX + srv + "." + _cfgName + "."))
							{
								_logger.info("sendErrorQueue(SAVE-TO-DISK): cfgName='"+_cfgName+"', srv='"+srv+"'. trying to recover and send file: "+file);
								try
								{
									String msgStr = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

									// If the file is empty... delete the file and go to next
									if (StringUtil.isNullOrBlank(msgStr))
									{
										_logger.info("sendErrorQueue(SAVE-TO-DISK): cfgName='"+_cfgName+"', srv='"+srv+"'. Found empty file, just deleting it and continuing with next... deleted file: "+file);
										file.delete();
										continue;
									}
									
									// maybe fix this to be passed into: sendMessage() instead
									_srvName = srv;
									
									// send message (if we have problems an exception will be thrown)
									int serverSideQueueSize = sendMessage(msgStr, this);
									
									// on success: remove the file
									file.delete();
									
									sendCount++;
									
									// Sleep for a while (do not overload the central server)
									// NOTE: if we do NOT want the sleep time to be static, then the server needs to send feedback (in sendMessage()) about how long we should sleep
									//       meaning if the server has a short list to save, then short sleep time, or if the persist list is long a longer sleep time.
									int sleepThreshold = 5;
									if (serverSideQueueSize > sleepThreshold)
									{
										_logger.info("sendErrorQueue(SAVE-TO-DISK): serverSideQueueSize="+serverSideQueueSize+", Sleeping "+_errorSaveToDiskSuccessSleepTimeMs+" ms for config name '"+_cfgName+"', srv='"+srv+"', sendCount="+sendCount+", after sending file '"+file+"'. This not to overload the Central Server.");
										Thread.sleep(_errorSaveToDiskSuccessSleepTimeMs);
									}
								}
								catch (InterruptedException ex)
								{
									_logger.info("sendErrorQueue(SAVE-TO-DISK): Interupted when doing disk entry recovery at file '"+file+"' in the ErrorQueue for config name '"+_cfgName+"'. to '"+_urlLastUsed+"'.");
									break;
								}
								catch (ConnectException ex) 
								{
									// log WITHOUT stacktrace
									_logger.info("sendErrorQueue(SAVE-TO-DISK): Resending PROBLEMS for disk entry '"+file+"' in the ErrorQueue for config name '"+_cfgName+"'. to '"+_urlLastUsed+"'. It will be kept on disk... Caught: "+ex);
									break;
								}
								catch (Exception ex) 
								{
									// log with STACKTRACE
									_logger.info("sendErrorQueue(SAVE-TO-DISK): Resending PROBLEMS for disk entry '"+file+"' in the ErrorQueue for config name '"+_cfgName+"'. to '"+_urlLastUsed+"'. It will be kept on disk... Caught: "+ex, ex);
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
							String msgStr = _errorQueue.getFirst();

							_logger.info("sendErrorQueue(IN-MEMORY): Resending 'oldest' entry in the ErrorQueue for config name '"+_cfgName+"'. _errorQueue.size()="+_errorQueue.size());
							
							// send message (if we have problems an exception will be thrown)
							sendMessage(msgStr, this);
							
							// on success: remove the queue entry
							_errorQueue.removeFirst();
							sendCount++;
						}
						catch (ConnectException ex) 
						{
							// log WITHOUT stacktrace
							_logger.info("sendErrorQueue(IN-MEMORY): Resending PROBLEMS for 'oldest' entry in the ErrorQueue for config name '"+_cfgName+"'. _errorQueue.size()="+_errorQueue.size()+". to '"+_urlLastUsed+"'. It will be kept in the queue... Caught: "+ex);
							break;
						}
						catch (Exception ex) 
						{
							// log with STACKTRACE
							_logger.info("sendErrorQueue(IN-MEMORY): Resending PROBLEMS for 'oldest' entry in the ErrorQueue for config name '"+_cfgName+"'. _errorQueue.size()="+_errorQueue.size()+". to '"+_urlLastUsed+"'. It will be kept in the queue... Caught: "+ex, ex);
							break;
						}
					}
					return sendCount;
				}
			}
			catch (RuntimeException ex)
			{
				_logger.error("Runtime Problems in: sendErrorQueue(), cfgName='"+_cfgName+"', continuing anyway... Caught: " + ex, ex);
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
				for (File file : FileUtils.listFiles(new File(_errorSaveToDiskPath), new String[] {RECOVERY_FILE_SUFFIX}, false) )
				{
					String filename = file.getName();
					if (filename.startsWith(RECOVERY_FILE_PREFIX + srv + "." + _cfgName + "."))
					{
						if (FileUtils.isFileOlder(file, timeMillis))
						{
							if (file.delete())
								_logger.info("removeOldErrorFiles(): cfgName='"+_cfgName+"', srv='"+srv+"'. SUCCESS: removing file: "+file);
							else
								_logger.info("removeOldErrorFiles(): cfgName='"+_cfgName+"', srv='"+srv+"'. FAILED: removing file: "+file);
						}
					}
				}
			}
			catch (RuntimeException ex)
			{
				_logger.error("Runtime Problems in: removeOldErrorFiles(), cfgName='"+_cfgName+"', continuing anyway... Caught: " + ex, ex);
			}
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

		list.add( new CmSettingsHelper("URL",           Type.MANDATORY,     key(PROPKEY_url                              ), String .class, conf.getProperty       (key(PROPKEY_url                              ), DEFAULT_url                              ), DEFAULT_url                              , "URL to use when issuing the HTTP POST request", new UrlInputValidator()));

		list.add( new CmSettingsHelper("Username",                          key(PROPKEY_username                         ), String .class, conf.getProperty       (key(PROPKEY_username                         ), DEFAULT_username                         ), DEFAULT_username                         , "Username when connecting"));
		list.add( new CmSettingsHelper("Password",                          key(PROPKEY_password                         ), String .class, conf.getProperty       (key(PROPKEY_password                         ), DEFAULT_password                         ), DEFAULT_password                         , "Password when connecting"));
//		list.add( new CmSettingsHelper("ConvertSampleTimeToUtc",            key(PROPKEY_asUtcTime                        ), Boolean.class, conf.getBooleanProperty(key(PROPKEY_asUtcTime                        ), DEFAULT_asUtcTime                        ), DEFAULT_asUtcTime                        , "Convert Sample Time to UTC Time. According to InfluxDB Documentation UTC should be used when writing data (this didn't work for me). NOTE: Check if your InfluxDB automatically converts the Sent timestamp to UTC (based on the TimeZone where InfluxDB is running)."));
		list.add( new CmSettingsHelper("AddGraphMetaDataAsTags",            key(PROPKEY_addMetaTags                      ), Boolean.class, conf.getBooleanProperty(key(PROPKEY_addMetaTags                      ), DEFAULT_addMetaTags                      ), DEFAULT_addMetaTags                      , "Add Graph/Chart MetaData (like GraphLabel, etc) as InfluxDB Tags"));

		list.add( new CmSettingsHelper("errorSendAlarm",                    key(PROPKEY_errorSendAlarm                   ), Boolean.class, conf.getBooleanProperty(key(PROPKEY_errorSendAlarm                   ), DEFAULT_errorSendAlarm                   ), DEFAULT_errorSendAlarm                   , "If send errors, Send Alarm.)"));
		list.add( new CmSettingsHelper("errorSendAlarmThresholdInSec",      key(PROPKEY_errorSendAlarmThresholdInSec     ), Integer.class, conf.getIntProperty    (key(PROPKEY_errorSendAlarmThresholdInSec     ), DEFAULT_errorSendAlarmThresholdInSec     ), DEFAULT_errorSendAlarmThresholdInSec     , "If send errors, Send Alarm, but only after X Seconds.)"));

		list.add( new CmSettingsHelper("errorSaveToDisk",                   key(PROPKEY_errorSaveToDisk                  ), Boolean.class, conf.getBooleanProperty(key(PROPKEY_errorSaveToDisk                  ), DEFAULT_errorSaveToDisk                  ), DEFAULT_errorSaveToDisk                  , "If send errors, save the Msg message to DISK and retry later.)"));
		list.add( new CmSettingsHelper("errorSaveToDiskPath",               key(PROPKEY_errorSaveToDiskPath              ), String .class, conf.getProperty       (key(PROPKEY_errorSaveToDiskPath              ), DEFAULT_errorSaveToDiskPath              ), DEFAULT_errorSaveToDiskPath              , "Path where to save Msg messages on send errors"));
		list.add( new CmSettingsHelper("errorSaveToDiskDiscardAfterXDays",  key(PROPKEY_errorSaveToDiskDiscardAfterXDays ), Integer.class, conf.getIntProperty    (key(PROPKEY_errorSaveToDiskDiscardAfterXDays ), DEFAULT_errorSaveToDiskDiscardAfterXDays ), DEFAULT_errorSaveToDiskDiscardAfterXDays , "How many days should we save the files."));
		list.add( new CmSettingsHelper("errorSaveToDiskSuccessSleepTimeMs", key(PROPKEY_errorSaveToDiskSuccessSleepTimeMs), Integer.class, conf.getIntProperty    (key(PROPKEY_errorSaveToDiskSuccessSleepTimeMs), DEFAULT_errorSaveToDiskSuccessSleepTimeMs), DEFAULT_errorSaveToDiskSuccessSleepTimeMs, "After a recovered file has been sent, sleep for a while so we do not overflow the central server with messages."));

		list.add( new CmSettingsHelper("errorMemQueueSize",                 key(PROPKEY_errorMemQueueSize                ), Integer.class, conf.getIntProperty    (key(PROPKEY_errorMemQueueSize                ), DEFAULT_errorMemQueueSize                 ), DEFAULT_errorMemQueueSize               , "If send errors, in memory queue size for resend (Only valid if 'errorSaveToDisk' is false)"));
                                                                                                                                                                                                                                                                                                        
//		list.add( new CmSettingsHelper("sendCounters",                      key(PROPKEY_sendCounters                     ), Boolean.class, conf.getBooleanProperty(key(PROPKEY_sendCounters                     ), DEFAULT_sendCounters                      ), DEFAULT_sendCounters                    , "Send Performance Counters data"));
//		list.add( new CmSettingsHelper("sendGraphs",                        key(PROPKEY_sendGraphs                       ), Boolean.class, conf.getBooleanProperty(key(PROPKEY_sendGraphs                       ), DEFAULT_sendGraphs                        ), DEFAULT_sendGraphs                      , "Send Graph/Chart data"));
                                                                                                                                                                                                                                                                                                        
		list.add( new CmSettingsHelper("http-header-1",                     key(PROPKEY_header1                          ), String .class, conf.getProperty       (key(PROPKEY_header1                          ), DEFAULT_header1                           ), DEFAULT_header1                         , "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-2",                     key(PROPKEY_header2                          ), String .class, conf.getProperty       (key(PROPKEY_header2                          ), DEFAULT_header2                           ), DEFAULT_header2                         , "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-3",                     key(PROPKEY_header3                          ), String .class, conf.getProperty       (key(PROPKEY_header3                          ), DEFAULT_header3                           ), DEFAULT_header3                         , "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-4",                     key(PROPKEY_header4                          ), String .class, conf.getProperty       (key(PROPKEY_header4                          ), DEFAULT_header4                           ), DEFAULT_header4                         , "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-5",                     key(PROPKEY_header5                          ), String .class, conf.getProperty       (key(PROPKEY_header5                          ), DEFAULT_header5                           ), DEFAULT_header5                         , "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-6",                     key(PROPKEY_header6                          ), String .class, conf.getProperty       (key(PROPKEY_header6                          ), DEFAULT_header6                           ), DEFAULT_header6                         , "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-7",                     key(PROPKEY_header7                          ), String .class, conf.getProperty       (key(PROPKEY_header7                          ), DEFAULT_header7                           ), DEFAULT_header7                         , "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-8",                     key(PROPKEY_header8                          ), String .class, conf.getProperty       (key(PROPKEY_header8                          ), DEFAULT_header8                           ), DEFAULT_header8                         , "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-9",                     key(PROPKEY_header9                          ), String .class, conf.getProperty       (key(PROPKEY_header9                          ), DEFAULT_header9                           ), DEFAULT_header9                         , "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));

		return list;
	}
}

// Maybe add: user/password   with Basic Authentication  ... 
//   look at: https://www.baeldung.com/java-http-url-connection
//            https://github.com/eugenp/tutorials/tree/master/core-java-modules/core-java-networking-2
//headers.put("Authorization", "Basic " + Util.base64Encode(options.get(++i).getBytes()));
