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
import com.asetune.cm.CmSettingsHelper.InputValidator;
import com.asetune.cm.CmSettingsHelper.Type;
import com.asetune.cm.CmSettingsHelper.ValidationException;
import com.asetune.utils.Configuration;
import com.asetune.utils.JavaUtils;
import com.asetune.utils.StringUtil;
import com.cloudbees.syslog.Facility;
import com.cloudbees.syslog.MessageFormat;
import com.cloudbees.syslog.SDElement;
import com.cloudbees.syslog.SDParam;
import com.cloudbees.syslog.Severity;
import com.cloudbees.syslog.SyslogMessage;
import com.cloudbees.syslog.sender.SyslogMessageSender;
import com.cloudbees.syslog.sender.TcpSyslogMessageSender;
import com.cloudbees.syslog.sender.UdpSyslogMessageSender;

/**
 * Send AlarmEntry to any syslog server
 * @author gorans
 *
 */
public class AlarmWriterToSyslog 
extends AlarmWriterAbstract
{
	private static Logger _logger = Logger.getLogger(AlarmWriterToSyslog.class);

	@Override
	public boolean isCallReRaiseEnabled()
	{
		return false;
	}

	@Override
	public void raise(AlarmEvent alarmEvent)
	{
		sendMessage(ACTION_RAISE, alarmEvent);
	}

	@Override
	public void reRaise(AlarmEvent alarmEvent)
	{
		// no implementation
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
		// If it's a CANCEL request, append how many HOURS:MINUTES:SECONDS the alarm was active for...
//		String duration = "";
//		if (ACTION_CANCEL.equals(action))
//			duration = TimeUtils.msToTimeStr(" (%HH:%MM:%SS)", alarmEvent.getCrAgeInMs() );
//	
//		String syslogMsgText = action + duration
//				+ "; " + alarmEvent.getAlarmClassNameShortAbriviated()
//				+ "; " + alarmEvent.getServiceInfo()
//				+ "; " + alarmEvent.getExtraInfo()
//				+ "; " + alarmEvent.getDescription()
//				;

		// Use the template and fill in values
		String syslogMsgText;
		if      (ACTION_RAISE .equals(action)) syslogMsgText = WriterUtils.createMessageFromTemplate(action, alarmEvent, _raiseMsgTemplate,  true, null, getDbxCentralUrl());
		else if (ACTION_CANCEL.equals(action)) syslogMsgText = WriterUtils.createMessageFromTemplate(action, alarmEvent, _cancelMsgTemplate, true, null, getDbxCentralUrl());
		else 
			throw new RuntimeException("Unknown action type: "+action);


		// Create the basic syslog message
		SyslogMessage message = new SyslogMessage()
				.withTimestamp(alarmEvent.getCrTime())
				.withAppName  (alarmEvent.getServiceType())  // The APP-NAME field SHOULD identify the device or application that originated the message.  It is a string without further semantics. It is intended for filtering messages on a relay or collector.
				.withHostname (alarmEvent.getServiceName())  // Hostname from where the event originated
				.withFacility (Facility.DAEMON)
				.withSeverity (alarmEventToSyslogSeverity(action, alarmEvent))
				.withMsg(syslogMsgText);

		// If we can get the JVM's process ID, lets add that...
		String javaPid = JavaUtils.getProcessId(null);
		if (StringUtil.hasValue(javaPid))
			message.withProcId(javaPid);
		
		boolean sendStructuredData = false;
		if (sendStructuredData)
		{
			message.withSDElement( new SDElement("dbxtuneSDID@1000", 
				new SDParam("alarmClass",  alarmEvent.getAlarmClass()  + ""), 
				new SDParam("serviceType", alarmEvent.getServiceType() + ""), 
				new SDParam("serviceName", alarmEvent.getServiceName() + ""), 
				new SDParam("serviceInfo", alarmEvent.getServiceInfo() + ""), 
				new SDParam("serviceInfo", alarmEvent.getServiceInfo() + ""), 
				new SDParam("extraInfo",   alarmEvent.getExtraInfo()   + ""), 
				new SDParam("category",    alarmEvent.getCategory()    + ""), 
				new SDParam("severity",    alarmEvent.getSeverity()    + ""), 
				new SDParam("state",       alarmEvent.getState()       + ""), 
				new SDParam("description", alarmEvent.getDescription() + ""), 
				new SDParam("data",        alarmEvent.getData()        + "") 
				));
		}
		
		
		// Create a sender TCP or UDP
		SyslogMessageSender sender;

		if ("UDP".equalsIgnoreCase(_nwProtocol))
		{
			UdpSyslogMessageSender udpSender = new UdpSyslogMessageSender();
			
			udpSender.setSyslogServerHostname(_hostname);
			udpSender.setSyslogServerPort(_port);
		
			if (_rfc == 3164) udpSender.setMessageFormat(MessageFormat.RFC_3164);
			if (_rfc == 5424) udpSender.setMessageFormat(MessageFormat.RFC_5424);

			sender = udpSender;
		}
		else
		{
			TcpSyslogMessageSender tcpSender = new TcpSyslogMessageSender();

			tcpSender.setSyslogServerHostname(_hostname);
			tcpSender.setSyslogServerPort(_port);

			tcpSender.setSocketConnectTimeoutInMillis(_tcpConnectTimeout);
			tcpSender.setSsl(_tcpUseSsl);
			tcpSender.setMaxRetryCount(_tcpMaxRetry);

			if (StringUtil.hasValue(_tcpPostfix))
				tcpSender.setPostfix(StringUtil.unEscapeControlChars(_tcpPostfix));
			
			if (_rfc == 3164) tcpSender.setMessageFormat(MessageFormat.RFC_3164);
			if (_rfc == 5424) tcpSender.setMessageFormat(MessageFormat.RFC_5424);

			sender = tcpSender;
		}
		
		// SEND
		try
		{
			sender.sendMessage(message);
		}
		catch(IOException ex)
		{
			_logger.error("Problems sending syslog record to server '"+_hostname+":"+_port+"'. Caught: "+ex , ex);
		}
	}
	
	/**
	 * Map a AlarmEvent service-state and severity to any syslog Severity
	 * 
	 * @param action        RAISE or CANCEL
	 * @param alarmEvent    the alarm event we have received 
	 * @return A syslog severity
	 */
	private Severity alarmEventToSyslogSeverity(String action, AlarmEvent alarmEvent)
	{
		if (ACTION_CANCEL.equals(action))
			return Severity.INFORMATIONAL;

		/* ---------------------------------------------------------------
		 * Syslog Severity levels
		 * 
		 * EMERGENCY:     system is unusable, numerical code 0.
		 * ALERT:         action must be taken immediately, numerical code 1.
		 * CRITICAL:      critical conditions, numerical code 2.
		 * ERROR:         error conditions, numerical code 3.
		 * WARNING:       warning conditions, numerical code 4.
		 * NOTICE:        normal but significant condition, numerical code 5.
		 * INFORMATIONAL: informational messages, numerical code 6.
		 * DEBUG:         debug-level messages, numerical code 7.
		 * ---------------------------------------------------------------
		 */
		if (ACTION_RAISE.equals(action))
		{
			// AlarmEvent-service-state: DOWN
			if (AlarmEvent.ServiceState.DOWN.equals(alarmEvent.getState()))
				return Severity.EMERGENCY;  // or possibly ALERT
				
			// AlarmEvent-service-state: AFFECTED
			if (AlarmEvent.ServiceState.AFFECTED.equals(alarmEvent.getState()))
				return Severity.CRITICAL;

			
			// AlarmEvent-severity ERROR
			if (AlarmEvent.Severity.ERROR.equals(alarmEvent.getSeverity()))
				return Severity.ERROR;

			// AlarmEvent-severity WARNING
			if (AlarmEvent.Severity.WARNING.equals(alarmEvent.getSeverity()))
				return Severity.WARNING;

			// AlarmEvent-severity INFO
			if (AlarmEvent.Severity.INFO.equals(alarmEvent.getSeverity()))
				return Severity.NOTICE;  // or possibly INFORMATIONAL
		}

		return Severity.DEBUG;
	}

	@Override
	public String getDescription()
	{
		return "Write Alarms to a syslog server.";
	}

	/** Input validator */
	private static class RfcInputValidator
	implements InputValidator
	{
		@Override
		public boolean isValid(CmSettingsHelper sh, String val) throws ValidationException
		{
			int rfc = 0;
			try { rfc = Integer.parseInt(val); }
			catch(NumberFormatException ex) { throw new ValidationException("The value '"+val+"' is not a valid Integer: "+ex.getMessage()); }

			if ( ! (rfc==3164 || rfc == 5424) )
				throw new ValidationException("Allowed RFC numbers are 3164 or 5424");
			
			return true;
		}
	}
	/** Input validator */
	private static class TcpUdpInputValidator
	implements InputValidator
	{
		@Override
		public boolean isValid(CmSettingsHelper sh, String val) throws ValidationException
		{
			if ( ! ("TCP".equalsIgnoreCase(val) || "UDP".equalsIgnoreCase(val)) )
				throw new ValidationException("Allowed values is 'TCP' or 'UDP'.");
			
			return true;
		}
	}

	@Override
	public List<CmSettingsHelper> getAvailableSettings()
	{
		ArrayList<CmSettingsHelper> list = new ArrayList<>();
		
		Configuration conf = Configuration.getCombinedConfiguration();

		list.add( new CmSettingsHelper("hostname",         Type.MANDATORY, PROPKEY_hostname,         String .class, conf.getProperty       (PROPKEY_hostname,         DEFAULT_hostname         ), DEFAULT_hostname         , "Name of the host that holds the syslog server"));
		list.add( new CmSettingsHelper("port",             Type.MANDATORY, PROPKEY_port,             Integer.class, conf.getIntProperty    (PROPKEY_port,             DEFAULT_port             ), DEFAULT_port             , "port number for the syslog server"));
		list.add( new CmSettingsHelper("RFC",              Type.MANDATORY, PROPKEY_rfc,              Integer.class, conf.getIntProperty    (PROPKEY_rfc,              DEFAULT_rfc              ), DEFAULT_rfc              , "What RFC (syslog message standard) do you want to use. (3164 or 5424)", new RfcInputValidator()));
		list.add( new CmSettingsHelper("UDP-or-TCP",       Type.MANDATORY, PROPKEY_networkProtocol,  String .class, conf.getProperty       (PROPKEY_networkProtocol,  DEFAULT_networkProtocol  ), DEFAULT_networkProtocol  , "What network protocol should be used to send the message to the syslog server (TCP or UDP)", new TcpUdpInputValidator()));
		list.add( new CmSettingsHelper("raiseMsgTemplate", Type.MANDATORY, PROPKEY_raiseMsgTemplate, String .class, conf.getProperty       (PROPKEY_raiseMsgTemplate, DEFAULT_raiseMsgTemplate ), DEFAULT_raiseMsgTemplate , "RAISE Message Template to POST. Note: all ${somValue} will be replaced with runtime values."));
		list.add( new CmSettingsHelper("cancelMsgTemplate",Type.MANDATORY, PROPKEY_cancelMsgTemplate,String .class, conf.getProperty       (PROPKEY_cancelMsgTemplate,DEFAULT_cancelMsgTemplate), DEFAULT_cancelMsgTemplate, "CANCEL Message Template to POST. Note: all ${somValue} will be replaced with runtime values."));

		list.add( new CmSettingsHelper("send-SD",                          PROPKEY_sendSd,           Boolean.class, conf.getBooleanProperty(PROPKEY_sendSd,           DEFAULT_sendSd           ), DEFAULT_sendSd           , "if RFC 5424: Send Structured Data to the syslog server"));
		list.add( new CmSettingsHelper("tcpConnTimeout",                   PROPKEY_tcpConnTimeout,   Integer.class, conf.getIntProperty    (PROPKEY_tcpConnTimeout,   DEFAULT_tcpConnTimeout   ), DEFAULT_tcpConnTimeout   , "if TCP: Timeout value when connectiong to the syslog server"));
		list.add( new CmSettingsHelper("tcpUseSsl",                        PROPKEY_tcpUseSsl,        Boolean.class, conf.getBooleanProperty(PROPKEY_tcpUseSsl,        DEFAULT_tcpUseSsl        ), DEFAULT_tcpUseSsl        , "if TCP: Should we use SSL when talking to the syslog server"));
		list.add( new CmSettingsHelper("tcpMaxRetry",                      PROPKEY_tcpMaxRetry,      Integer.class, conf.getIntProperty    (PROPKEY_tcpMaxRetry,      DEFAULT_tcpMaxRetry      ), DEFAULT_tcpMaxRetry      , "if TCP: Max number of retries..."));
		list.add( new CmSettingsHelper("tcpPostfix",                       PROPKEY_tcpPostfix,       String .class, conf.getProperty       (PROPKEY_tcpPostfix,       DEFAULT_tcpPostfix       ), DEFAULT_tcpPostfix       , "if TCP: Terminate the message string with this characters. Note: you need to specify <CR> as \\r and <NL> as \\n"));

		return list;
	}

	//-------------------------------------------------------
	// class members
	//-------------------------------------------------------
	private String  _hostname           = "";
	private int     _port               = -1;
	private int     _rfc                = -1;
	private String  _nwProtocol         = "";
	private String  _raiseMsgTemplate   = "";
	private String  _cancelMsgTemplate  = "";

	private boolean _rfc5424_sendSd     = false;
	private int     _tcpConnectTimeout  = -1;
	private boolean _tcpUseSsl          = false;
	private int     _tcpMaxRetry        = -1;
	private String  _tcpPostfix         = "";
	//-------------------------------------------------------

	@Override
	public void init(Configuration conf) throws Exception
	{
		super.init(conf);

		_logger.info("Initializing the AlarmWriter component named '"+getName()+"'.");

		_hostname          = conf.getProperty       (PROPKEY_hostname,          DEFAULT_hostname);
		_port              = conf.getIntProperty    (PROPKEY_port,              DEFAULT_port);
		_rfc               = conf.getIntProperty    (PROPKEY_rfc,               DEFAULT_rfc);
		_nwProtocol        = conf.getProperty       (PROPKEY_networkProtocol,   DEFAULT_networkProtocol);
		_raiseMsgTemplate  = conf.getProperty       (PROPKEY_raiseMsgTemplate,  DEFAULT_raiseMsgTemplate);
		_cancelMsgTemplate = conf.getProperty       (PROPKEY_cancelMsgTemplate, DEFAULT_cancelMsgTemplate);

		_rfc5424_sendSd    = conf.getBooleanProperty(PROPKEY_sendSd,            DEFAULT_sendSd);
		_tcpConnectTimeout = conf.getIntProperty    (PROPKEY_tcpConnTimeout,    DEFAULT_tcpConnTimeout);
		_tcpUseSsl         = conf.getBooleanProperty(PROPKEY_tcpUseSsl,         DEFAULT_tcpUseSsl);
		_tcpMaxRetry       = conf.getIntProperty    (PROPKEY_tcpMaxRetry,       DEFAULT_tcpMaxRetry);
		_tcpPostfix        = conf.getProperty       (PROPKEY_tcpPostfix,        DEFAULT_tcpPostfix);
		
		//------------------------------------------
		// Check for mandatory parameters
		//------------------------------------------
		if ( StringUtil.isNullOrBlank(_hostname)          ) throw new Exception("The property '" + PROPKEY_hostname          + "' is mandatory for the AlarmWriter named '"+getName()+"'.");
		if ( _port == -1                                  ) throw new Exception("The property '" + PROPKEY_port              + "' is mandatory for the AlarmWriter named '"+getName()+"'.");
		if ( _rfc  == -1                                  ) throw new Exception("The property '" + PROPKEY_rfc               + "' is mandatory for the AlarmWriter named '"+getName()+"'.");
		if ( StringUtil.isNullOrBlank(_nwProtocol)        ) throw new Exception("The property '" + PROPKEY_networkProtocol   + "' is mandatory for the AlarmWriter named '"+getName()+"'.");
		if ( StringUtil.isNullOrBlank(_raiseMsgTemplate)  ) throw new Exception("The property '" + PROPKEY_raiseMsgTemplate  + "' is mandatory for the AlarmWriter named '"+getName()+"'.");
		if ( StringUtil.isNullOrBlank(_cancelMsgTemplate) ) throw new Exception("The property '" + PROPKEY_cancelMsgTemplate + "' is mandatory for the AlarmWriter named '"+getName()+"'.");


		//------------------------------------------
		// Check for valid configuration
		//------------------------------------------

		// RFC
		if ( ! (_rfc == 3164 || _rfc == 5424) )
			throw new Exception("The property '" + PROPKEY_rfc + "' can only be 3164 or 5424. current config is '"+_rfc+"' for the AlarmWriter named '"+getName()+"'.");

		// NETWORK PROTOCOL
		if ( ! ("TCP".equalsIgnoreCase(_nwProtocol) || "UDP".equalsIgnoreCase(_nwProtocol)) )
			throw new Exception("The property '" + PROPKEY_networkProtocol + "' can only be 'TCP' or 'UDP'. current config is '"+_nwProtocol+"' for the AlarmWriter named '"+getName()+"'.");
	}

	@Override
	public void printConfig()
	{
		int spaces = 50;
		_logger.info("Configuration for Alarm Writer Module: "+getName());
		_logger.info("    " + StringUtil.left(PROPKEY_hostname          , spaces) + ": " + _hostname);
		_logger.info("    " + StringUtil.left(PROPKEY_port              , spaces) + ": " + _port);
		_logger.info("    " + StringUtil.left(PROPKEY_rfc               , spaces) + ": " + _rfc);
		_logger.info("    " + StringUtil.left(PROPKEY_networkProtocol   , spaces) + ": " + _nwProtocol);
		_logger.info("    " + StringUtil.left(PROPKEY_raiseMsgTemplate  , spaces) + ": " + _raiseMsgTemplate);
		_logger.info("    " + StringUtil.left(PROPKEY_cancelMsgTemplate , spaces) + ": " + _cancelMsgTemplate);
		_logger.info("    " + StringUtil.left(PROPKEY_sendSd            , spaces) + ": " + _rfc5424_sendSd);
		_logger.info("    " + StringUtil.left(PROPKEY_tcpConnTimeout    , spaces) + ": " + _tcpConnectTimeout);
		_logger.info("    " + StringUtil.left(PROPKEY_tcpUseSsl         , spaces) + ": " + _tcpUseSsl);
		_logger.info("    " + StringUtil.left(PROPKEY_tcpMaxRetry       , spaces) + ": " + _tcpMaxRetry);
		_logger.info("    " + StringUtil.left(PROPKEY_tcpPostfix        , spaces) + ": " + _tcpPostfix);
	}


	public static final String  PROPKEY_hostname          = "AlarmWriterToSyslog.hostname";
	public static final String  DEFAULT_hostname          = "localhost";

	public static final String  PROPKEY_port              = "AlarmWriterToSyslog.port";
	public static final int     DEFAULT_port              = 514;
	
	public static final String  PROPKEY_rfc               = "AlarmWriterToSyslog.rfc";
	public static final int     DEFAULT_rfc               = 5424; // 3164 or 5424
	
	public static final String  PROPKEY_networkProtocol   = "AlarmWriterToSyslog.network.protocol";
	public static final String  DEFAULT_networkProtocol   = "TCP";
	
	public static final String  PROPKEY_raiseMsgTemplate  = "AlarmWriterToSyslog.raise.msg.template";
	public static final String  DEFAULT_raiseMsgTemplate  = "${type}; ${alarmClass}; ${serviceInfo}; ${extraInfo}; ${description}";
	
	public static final String  PROPKEY_cancelMsgTemplate = "AlarmWriterToSyslog.cancel.msg.template";
	public static final String  DEFAULT_cancelMsgTemplate = "${type} (${duration}); ${alarmClass}; ${serviceInfo}; ${extraInfo}; ${description}";
	

	
	public static final String  PROPKEY_sendSd            = "AlarmWriterToSyslog.rfc.5424.send.structured.data";
	public static final boolean DEFAULT_sendSd            = false;
	
	public static final String  PROPKEY_tcpConnTimeout    = "AlarmWriterToSyslog.tcp.connection.timeout";
	public static final int     DEFAULT_tcpConnTimeout    = 500;
	
	public static final String  PROPKEY_tcpUseSsl         = "AlarmWriterToSyslog.rfc.5424.ssl";
	public static final boolean DEFAULT_tcpUseSsl         = false;

	public static final String  PROPKEY_tcpMaxRetry       = "AlarmWriterToSyslog.tcp.max.retry";
	public static final int     DEFAULT_tcpMaxRetry       = 2;

	public static final String  PROPKEY_tcpPostfix        = "AlarmWriterToSyslog.tcp.postfix";
	public static final String  DEFAULT_tcpPostfix        = "\\n";
}
