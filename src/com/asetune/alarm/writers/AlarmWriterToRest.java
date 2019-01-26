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
package com.asetune.alarm.writers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.alarm.events.AlarmEvent;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSettingsHelper.JsonInputValidator;
import com.asetune.cm.CmSettingsHelper.Type;
import com.asetune.cm.CmSettingsHelper.UrlInputValidator;
import com.asetune.utils.Configuration;
import com.asetune.utils.HttpUtils;
import com.asetune.utils.StringUtil;
import com.google.gson.stream.JsonWriter;

public class AlarmWriterToRest
extends AlarmWriterAbstract
{
	private static Logger _logger = Logger.getLogger(AlarmWriterToRest.class);

	@Override
	public boolean isCallReRaiseEnabled()
	{
		return _isReRaiseEnabled;
	}

	@Override
	public void raise(AlarmEvent alarmEvent)
	{
		sendMessage(ACTION_RAISE, alarmEvent);
	}

	@Override
	public void reRaise(AlarmEvent alarmEvent)
	{
		sendMessage(ACTION_RE_RAISE, alarmEvent);
	}

	@Override
	public void cancel(AlarmEvent alarmEvent)
	{
		sendMessage(ACTION_CANCEL, alarmEvent);
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

	/**
	 * Here is where the send happens
	 * @param action
	 * @param alarmEvent
	 */
	private void sendMessage(String action, AlarmEvent alarmEvent)
	{
		// replace variables in the template with runtime variables
		String jsonMessage = WriterUtils.createMessageFromTemplate(action, alarmEvent, _jsonTemplate, true, null, getDbxCentralUrl());

		try
		{
			URL url = new URL(_url);

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");

			// add headers
			addHeader(conn, _header_1);
			addHeader(conn, _header_2);
			addHeader(conn, _header_3);
			addHeader(conn, _header_4);
			addHeader(conn, _header_5);
			addHeader(conn, _header_6);
			addHeader(conn, _header_7);
			addHeader(conn, _header_8);
			addHeader(conn, _header_9);

			// Write the output
			OutputStream os = conn.getOutputStream();
			os.write(jsonMessage.getBytes());
			os.flush();

			// Check responce
			int responceCode = conn.getResponseCode();
			if ( responceCode >= 203) // see 'https://httpstatuses.com/' for http codes... or at the bottom of this source code
			{
				throw new Exception("Failed : HTTP error code : " + responceCode);
			}
			else
			{
				_logger.info("Responce code "+responceCode+" ("+HttpUtils.httpResponceCodeToText(responceCode)+"). From URL '"+_url+"'. Sent JSON content: "+jsonMessage);
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
		catch (Exception  ex)
		{
			_logger.error("Problems sending REST call to '"+_url+"'. Caught: "+ex , ex);
		}
	}
	
	@Override
	public String getDescription()
	{
		return "Write Alarms to a HTTP URL as a REST call with JSON content.";
	}

	@Override
	public List<CmSettingsHelper> getAvailableSettings()
	{
		ArrayList<CmSettingsHelper> list = new ArrayList<>();
		
		Configuration conf = Configuration.getCombinedConfiguration();

		list.add( new CmSettingsHelper("URL",           Type.MANDATORY, PROPKEY_url,              String .class, conf.getProperty       (PROPKEY_url,              DEFAULT_url),              DEFAULT_url,              "URL to use when issuing the HTTP POST request", new UrlInputValidator()));
		list.add( new CmSettingsHelper("json-template", Type.MANDATORY, PROPKEY_jsonTemplate,     String .class, conf.getProperty       (PROPKEY_jsonTemplate,     DEFAULT_jsonTemplate),     DEFAULT_jsonTemplate,     "JSON Template to POST. Note: all ${somValue} will be replaced with runtime values.", new JsonInputValidator()));
		list.add( new CmSettingsHelper("isReRaiseEnabled",              PROPKEY_isReRaiseEnabled, Boolean.class, conf.getBooleanProperty(PROPKEY_isReRaiseEnabled, DEFAULT_isReRaiseEnabled), DEFAULT_isReRaiseEnabled, "If the Alarm Hander should send an event every time it receives an event. or if it should just be called on RAISE and CANCEL"));
		list.add( new CmSettingsHelper("http-header-1",                 PROPKEY_header1,          String .class, conf.getProperty       (PROPKEY_header1,          DEFAULT_header1),          DEFAULT_header1,          "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-2",                 PROPKEY_header2,          String .class, conf.getProperty       (PROPKEY_header2,          DEFAULT_header2),          DEFAULT_header2,          "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-3",                 PROPKEY_header3,          String .class, conf.getProperty       (PROPKEY_header3,          DEFAULT_header3),          DEFAULT_header3,          "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-4",                 PROPKEY_header4,          String .class, conf.getProperty       (PROPKEY_header4,          DEFAULT_header4),          DEFAULT_header4,          "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-5",                 PROPKEY_header5,          String .class, conf.getProperty       (PROPKEY_header5,          DEFAULT_header5),          DEFAULT_header5,          "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-6",                 PROPKEY_header6,          String .class, conf.getProperty       (PROPKEY_header6,          DEFAULT_header6),          DEFAULT_header6,          "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-7",                 PROPKEY_header7,          String .class, conf.getProperty       (PROPKEY_header7,          DEFAULT_header7),          DEFAULT_header7,          "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-8",                 PROPKEY_header8,          String .class, conf.getProperty       (PROPKEY_header8,          DEFAULT_header8),          DEFAULT_header8,          "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));
		list.add( new CmSettingsHelper("http-header-9",                 PROPKEY_header9,          String .class, conf.getProperty       (PROPKEY_header9,          DEFAULT_header9),          DEFAULT_header9,          "Extra header values that you want to add the the HTTP Header. Like: Authorization: ..."));

		list.add( new CmSettingsHelper("DbxCentralUrl",                 PROPKEY_dbxCentralUrl,    String .class, conf.getProperty       (PROPKEY_dbxCentralUrl   , DEFAULT_dbxCentralUrl   ), DEFAULT_dbxCentralUrl   , "Where is the DbxCentral located, if you want your template/messages to include it using ${dbxCentralUrl}", new UrlInputValidator()));

		return list;
	}

	//-------------------------------------------------------
	// class members
	//-------------------------------------------------------
	private String  _url          = "";
	private String  _jsonTemplate = "";
	private String  _header_1     = "";
	private String  _header_2     = "";
	private String  _header_3     = "";
	private String  _header_4     = "";
	private String  _header_5     = "";
	private String  _header_6     = "";
	private String  _header_7     = "";
	private String  _header_8     = "";
	private String  _header_9     = "";
	
	private boolean _isReRaiseEnabled = false;
	
	//-------------------------------------------------------

	@Override
	public void init(Configuration conf) throws Exception
	{
		super.init(conf);

		_logger.info("Initializing the AlarmWriter component named '"+getName()+"'.");

		_url              = conf.getProperty       (PROPKEY_url,              DEFAULT_url);
		_jsonTemplate     = conf.getProperty       (PROPKEY_jsonTemplate,     DEFAULT_jsonTemplate);
		_isReRaiseEnabled = conf.getBooleanProperty(PROPKEY_isReRaiseEnabled, DEFAULT_isReRaiseEnabled);

		_header_1         = conf.getProperty       (PROPKEY_header1,          DEFAULT_header1);
		_header_2         = conf.getProperty       (PROPKEY_header2,          DEFAULT_header2);
		_header_3         = conf.getProperty       (PROPKEY_header3,          DEFAULT_header3);
		_header_4         = conf.getProperty       (PROPKEY_header4,          DEFAULT_header4);
		_header_5         = conf.getProperty       (PROPKEY_header5,          DEFAULT_header5);
		_header_6         = conf.getProperty       (PROPKEY_header6,          DEFAULT_header6);
		_header_7         = conf.getProperty       (PROPKEY_header7,          DEFAULT_header7);
		_header_8         = conf.getProperty       (PROPKEY_header8,          DEFAULT_header8);
		_header_9         = conf.getProperty       (PROPKEY_header9,          DEFAULT_header9);
		
		//------------------------------------------
		// Check for mandatory parameters
		//------------------------------------------
		if ( StringUtil.isNullOrBlank(_url) )          throw new Exception("The property '" + PROPKEY_url          + "' is mandatory for the AlarmWriter named '"+getName()+"'.");
		if ( StringUtil.isNullOrBlank(_jsonTemplate) ) throw new Exception("The property '" + PROPKEY_jsonTemplate + "' is mandatory for the AlarmWriter named '"+getName()+"'.");


		//------------------------------------------
		// Check for valid configuration
		//------------------------------------------
		// Check if the URL seems to be OK...
		try 
		{ 
			new URL(_url); 
		}
		catch(MalformedURLException ex) 
		{ 
			throw new Exception("The URL '"+_url+"' seems to be malformed. Caught: "+ex, ex); 
		}
	}

	@Override
	public void printConfig()
	{
		int spaces = 35;
		_logger.info("Configuration for Alarm Writer Module: "+getName());
		_logger.info("    " + StringUtil.left(PROPKEY_url,              spaces) + ": " + _url);
		_logger.info("    " + StringUtil.left(PROPKEY_jsonTemplate,     spaces) + ": " + _jsonTemplate);
		_logger.info("    " + StringUtil.left(PROPKEY_isReRaiseEnabled, spaces) + ": " + _isReRaiseEnabled);

		_logger.info("    " + StringUtil.left(PROPKEY_header1,          spaces) + ": " + _header_1);
		_logger.info("    " + StringUtil.left(PROPKEY_header2,          spaces) + ": " + _header_2);
		_logger.info("    " + StringUtil.left(PROPKEY_header3,          spaces) + ": " + _header_3);
		_logger.info("    " + StringUtil.left(PROPKEY_header4,          spaces) + ": " + _header_4);
		_logger.info("    " + StringUtil.left(PROPKEY_header5,          spaces) + ": " + _header_5);
		_logger.info("    " + StringUtil.left(PROPKEY_header6,          spaces) + ": " + _header_6);
		_logger.info("    " + StringUtil.left(PROPKEY_header7,          spaces) + ": " + _header_7);
		_logger.info("    " + StringUtil.left(PROPKEY_header8,          spaces) + ": " + _header_8);
		_logger.info("    " + StringUtil.left(PROPKEY_header9,          spaces) + ": " + _header_9);
	}

	public static final String  PROPKEY_url              = "AlarmWriterToRest.url";
	public static final String  DEFAULT_url              = null;
                                                     
	public static final String  PROPKEY_jsonTemplate     = "AlarmWriterToRest.json.template";
	public static final String  DEFAULT_jsonTemplate     = getDefaultJsonBody();

	public static final String  PROPKEY_isReRaiseEnabled = "AlarmWriterToRest.isReRaiseEnabled";
	public static final boolean DEFAULT_isReRaiseEnabled = false;

	public static final String  PROPKEY_header1          = "AlarmWriterToRest.header.1";
	public static final String  DEFAULT_header1          = null;
                                                         
	public static final String  PROPKEY_header2          = "AlarmWriterToRest.header.2";
	public static final String  DEFAULT_header2          = null;
                                                         
	public static final String  PROPKEY_header3          = "AlarmWriterToRest.header.3";
	public static final String  DEFAULT_header3          = null;
                                                         
	public static final String  PROPKEY_header4          = "AlarmWriterToRest.header.4";
	public static final String  DEFAULT_header4          = null;
                                                         
	public static final String  PROPKEY_header5          = "AlarmWriterToRest.header.5";
	public static final String  DEFAULT_header5          = null;
                                                         
	public static final String  PROPKEY_header6          = "AlarmWriterToRest.header.6";
	public static final String  DEFAULT_header6          = null;
                                                         
	public static final String  PROPKEY_header7          = "AlarmWriterToRest.header.7";
	public static final String  DEFAULT_header7          = null;
                                                         
	public static final String  PROPKEY_header8          = "AlarmWriterToRest.header.8";
	public static final String  DEFAULT_header8          = null;
                                                         
	public static final String  PROPKEY_header9          = "AlarmWriterToRest.header.9";
	public static final String  DEFAULT_header9          = null;

	public static String getDefaultJsonBody()
	{
		/*
		{
			"type" : "${alarmType}",
			"alarmEvent"
			{
				"alarmClass" : "${alarmClass}",
				"serviceType" : "${serviceType}",
				"serviceName" : "${serviceName}",
				...more values...,
				"description" : "${description}"
			}
		}
		 */
		try
		{
			StringWriter sw = new StringWriter();
			JsonWriter w = new JsonWriter(sw);

			w.beginObject();

			w.name("type").value("${type}");
			w.name("alarmEvent");
			w.beginObject();
				w.name("alarmClass")  .value("${alarmClass}");
				w.name("serviceType") .value("${serviceType}");
				w.name("serviceName") .value("${serviceName}");
				w.name("serviceInfo") .value("${serviceInfo}");
				w.name("extraInfo")   .value("${extraInfo}");
				w.name("category")    .value("${category}");
				w.name("severity")    .value("${severity}");
				w.name("state")       .value("${state}");
				w.name("data")        .value("${data}");
				w.name("description") .value("${description}");
			w.endObject();

			w.endObject();

			w.close();
			return sw.toString();
		}
		catch(IOException ex)
		{
			ex.toString();
			return ""+ex;
		}
	}
}
