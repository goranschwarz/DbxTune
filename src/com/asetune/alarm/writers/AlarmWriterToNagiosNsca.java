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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.alarm.events.AlarmEvent;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSettingsHelper.Type;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.googlecode.jsendnsca.Level;
import com.googlecode.jsendnsca.MessagePayload;
import com.googlecode.jsendnsca.NagiosException;
import com.googlecode.jsendnsca.NagiosPassiveCheckSender;
import com.googlecode.jsendnsca.NagiosSettings;
import com.googlecode.jsendnsca.builders.MessagePayloadBuilder;
import com.googlecode.jsendnsca.builders.NagiosSettingsBuilder;

public class AlarmWriterToNagiosNsca
extends AlarmWriterAbstract
{
	private static Logger _logger = Logger.getLogger(AlarmWriterToNagiosNsca.class);

	@Override
	public String getDescription()
	{
		return "Send Alarm Events to Nagios (in passive mode).";
	}

	@Override
	public boolean isCallReRaiseEnabled()
	{
		return true;
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
	 * Here is where the send happens
	 * @param action
	 * @param alarmEvent
	 */
	private void sendMessage(String action, AlarmEvent alarmEvent)
	{
		// replace variables in the template with runtime variables
		String msgBody    = WriterUtils.createMessageFromTemplate(action, alarmEvent, _msgTemplate, true, null, getDbxCentralUrl());

		// below is found at: https://stackoverflow.com/questions/16777560/sending-passive-checks-to-the-nagios-nsca-add-on-from-java-application
		//                or: https://github.com/jsendnsca/jsendnsca
		// Download jsendnsca-2.1.0.jar: https://github.com/jsendnsca/jsendnsca/releases
		
		NagiosSettings nagiosSettings = new NagiosSettingsBuilder()
				.withNagiosHost(_hostname)
				.withPort(_port)
				.create();
		
		if (StringUtil.hasValue(_password))
			nagiosSettings.setPassword(_password);

		if (_connectTimeout > 0)
			nagiosSettings.setConnectTimeout(_connectTimeout);
		
		if (_responceTimeout > 0)
			nagiosSettings.setTimeout(_responceTimeout);
		
		NagiosPassiveCheckSender sender = new NagiosPassiveCheckSender(nagiosSettings);
		
		MessagePayload payload = new MessagePayloadBuilder()
				.withHostname   ( alarmEvent.getServiceName() )
				.withLevel      ( alarmEventToNagiosSeverity(action, alarmEvent) )
				.withServiceName( alarmEvent.getAlarmClassAbriviated() )
				.withMessage    ( msgBody)
				.create();
		
		try
		{
			sender.send(payload);
			_logger.info("Sent message message: hostname='"+_hostname+".");
		}
		catch (NagiosException e)
		{
			_logger.error("Problems sending Nagios message. hostname='"+_hostname+"', port="+_port, e);
		}
		catch (IOException e)
		{
			_logger.error("Problems sending Nagios message. hostname='"+_hostname+"', port="+_port, e);
		}
	}

	/**
	 * Map a AlarmEvent service-state and severity to any syslog Severity
	 * 
	 * @param action        RAISE or CANCEL
	 * @param alarmEvent    the alarm event we have received 
	 * @return A syslog severity
	 */
	private Level alarmEventToNagiosSeverity(String action, AlarmEvent alarmEvent)
	{
		if (ACTION_CANCEL.equals(action))
			return Level.OK;

		/* ---------------------------------------------------------------
		 * Nagios levels
		 * 
		 * Level.OK       =        
		 * Level.WARNING  =   
		 * Level.CRITICAL =  
		 * Level.UNKNOWN  =   
		 * ---------------------------------------------------------------
		 */
		if (ACTION_RAISE.equals(action) || ACTION_RE_RAISE.equals(action))
		{
			// AlarmEvent-service-state: DOWN
			if (AlarmEvent.ServiceState.DOWN.equals(alarmEvent.getState()))
				return Level.CRITICAL;
				
			// AlarmEvent-service-state: AFFECTED
			if (AlarmEvent.ServiceState.AFFECTED.equals(alarmEvent.getState()))
				return Level.CRITICAL;

			
			// AlarmEvent-severity ERROR
			if (AlarmEvent.Severity.ERROR.equals(alarmEvent.getSeverity()))
				return Level.CRITICAL;

			// AlarmEvent-severity WARNING
			if (AlarmEvent.Severity.WARNING.equals(alarmEvent.getSeverity()))
				return Level.WARNING;

			// AlarmEvent-severity INFO
			if (AlarmEvent.Severity.INFO.equals(alarmEvent.getSeverity()))
				return Level.OK;
		}

		return Level.OK;
	}

	@Override
	public List<CmSettingsHelper> getAvailableSettings()
	{
		ArrayList<CmSettingsHelper> list = new ArrayList<>();
		
		Configuration conf = Configuration.getCombinedConfiguration();

		list.add( new CmSettingsHelper("hostname",         Type.MANDATORY, PROPKEY_hostname,          String .class, conf.getProperty   (PROPKEY_hostname       , DEFAULT_hostname       ), DEFAULT_hostname        , "Name of the nagios host"));
		list.add( new CmSettingsHelper("port",             Type.MANDATORY, PROPKEY_port,              Integer.class, conf.getIntProperty(PROPKEY_port           , DEFAULT_port           ), DEFAULT_port            , "port number"));
		list.add( new CmSettingsHelper("Msg-Template",     Type.MANDATORY, PROPKEY_msgTemplate,       String .class, conf.getProperty   (PROPKEY_msgTemplate    , DEFAULT_msgTemplate    ), DEFAULT_msgTemplate     , "What content should we send (Note: this is a template)"));
                                                                                                                                                                                                                   
//		list.add( new CmSettingsHelper("username",                         PROPKEY_username,          String .class, conf.getProperty   (PROPKEY_username       , DEFAULT_username       ), DEFAULT_username        , "Username if authentication is required"));
		list.add( new CmSettingsHelper("password",                         PROPKEY_password,          String .class, conf.getProperty   (PROPKEY_password       , DEFAULT_password       ), DEFAULT_password        , "Username if authentication is required"));
		list.add( new CmSettingsHelper("connect-timeout",                  PROPKEY_connectTimeout,    Integer.class, conf.getIntProperty(PROPKEY_connectTimeout , DEFAULT_connectTimeout ), DEFAULT_connectTimeout  , "Connection Timeout (-1 = use the default)"));
		list.add( new CmSettingsHelper("responce-timeout",                 PROPKEY_responceTimeout,   Integer.class, conf.getIntProperty(PROPKEY_responceTimeout, DEFAULT_responceTimeout), DEFAULT_responceTimeout , "Responce Timeout (-1 = use the default)"));

		return list;
	}

	//-------------------------------------------------------
	// class members
	//-------------------------------------------------------
	private String       _hostname        = "";
	private int          _port            = -1;
	private String       _msgTemplate     = "";

//	private String       _username        = "";
	private String       _password        = "";
	private int          _connectTimeout  = -1;
	private int          _responceTimeout = -1;

	//-------------------------------------------------------

	@Override
	public void init(Configuration conf) throws Exception
	{
		super.init(conf);

		_logger.info("Initializing the AlarmWriter component named '"+getName()+"'.");

		_hostname        = conf.getProperty       (PROPKEY_hostname,        DEFAULT_hostname);
		_port            = conf.getIntProperty    (PROPKEY_port,            DEFAULT_port);
		_msgTemplate     = conf.getProperty       (PROPKEY_msgTemplate,     DEFAULT_msgTemplate);
                            
//		_username        = conf.getProperty       (PROPKEY_username,        DEFAULT_username);
		_password        = conf.getProperty       (PROPKEY_password,        DEFAULT_password);
		_connectTimeout  = conf.getIntProperty    (PROPKEY_connectTimeout,  DEFAULT_connectTimeout);
		_responceTimeout = conf.getIntProperty    (PROPKEY_responceTimeout, DEFAULT_responceTimeout);

		//------------------------------------------
		// Check for mandatory parameters
		//------------------------------------------
		if ( StringUtil.isNullOrBlank(_hostname   ) ) throw new Exception("The property '" + PROPKEY_hostname    + "' is mandatory for the AlarmWriter named '"+getName()+"'.");
		if ( _port == -1                            ) throw new Exception("The property '" + PROPKEY_port        + "' is mandatory for the AlarmWriter named '"+getName()+"'.");
		if ( StringUtil.isNullOrBlank(_msgTemplate) ) throw new Exception("The property '" + PROPKEY_msgTemplate + "' is mandatory for the AlarmWriter named '"+getName()+"'.");
//		if ( StringUtil.isNullOrBlank(_username   ) ) throw new Exception("The property '" + PROPKEY_username    + "' is mandatory for the AlarmWriter named '"+getName()+"'.");
//		if ( StringUtil.isNullOrBlank(_password   ) ) throw new Exception("The property '" + PROPKEY_password    + "' is mandatory for the AlarmWriter named '"+getName()+"'.");

		//------------------------------------------
		// Check for valid configuration
		//------------------------------------------
	}

	@Override
	public void printConfig()
	{
		int spaces = 45;
		_logger.info("Configuration for Alarm Writer Module: "+getName());
		_logger.info("    " + StringUtil.left(PROPKEY_hostname          , spaces) + ": " + _hostname);
		_logger.info("    " + StringUtil.left(PROPKEY_port              , spaces) + ": " + _port);
		_logger.info("    " + StringUtil.left(PROPKEY_msgTemplate       , spaces) + ": " + _msgTemplate);

//		_logger.info("    " + StringUtil.left(PROPKEY_username          , spaces) + ": " + _username);
		_logger.info("    " + StringUtil.left(PROPKEY_password          , spaces) + ": " + (_logger.isDebugEnabled() ? _password : "*secret*") );
		_logger.info("    " + StringUtil.left(PROPKEY_connectTimeout    , spaces) + ": " + _connectTimeout);
		_logger.info("    " + StringUtil.left(PROPKEY_responceTimeout   , spaces) + ": " + _responceTimeout);
	}

	public static final String  PROPKEY_hostname        = "AlarmWriterToNagiosNsca.hostname";
	public static final String  DEFAULT_hostname        = "";
	
	public static final String  PROPKEY_port            = "AlarmWriterToNagiosNsca.port";
	public static final int     DEFAULT_port            = 5667;
	
	public static final String  PROPKEY_msgTemplate     = "AlarmWriterToNagiosNsca.msg.template";
	public static final String  DEFAULT_msgTemplate     = "${description} (${extraInfo})";
	

//	public static final String  PROPKEY_username        = "AlarmWriterToNagiosNsca.username";
//	public static final String  DEFAULT_username        = "";

	public static final String  PROPKEY_password        = "AlarmWriterToNagiosNsca.password";
	public static final String  DEFAULT_password        = "";

	public static final String  PROPKEY_connectTimeout  = "AlarmWriterToNagiosNsca.timeout.connect";
	public static final int     DEFAULT_connectTimeout  = -1;

	public static final String  PROPKEY_responceTimeout = "AlarmWriterToNagiosNsca.timeout.responce";
	public static final int     DEFAULT_responceTimeout = -1;
}
