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
package com.asetune.alarm.writers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.HtmlEmail;
import org.apache.log4j.Logger;

import com.asetune.alarm.events.AlarmEvent;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSettingsHelper.Type;
import com.asetune.cm.CmSettingsHelper.UrlInputValidator;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public class AlarmWriterToMail
extends AlarmWriterAbstract
{
	private static Logger _logger = Logger.getLogger(AlarmWriterToMail.class);

	@Override
	public String getDescription()
	{
		return "Send all Raise and Cancel Alarm Events to any user(s) via SMTP mail.";
	}

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
		// replace variables in the template with runtime variables
		String msgSubject = WriterUtils.createMessageFromTemplate(action, alarmEvent, _subjectTemplate, true, null, getDbxCentralUrl());
		String msgBody    = WriterUtils.createMessageFromTemplate(action, alarmEvent, _msgBodyTemplate, true, null, getDbxCentralUrl());

		int msgBodySizeKb = msgBody == null ? 0 : msgBody.length() / 1024;

		try
		{
			HtmlEmail email = new HtmlEmail();

			email.setHostName(_smtpHostname);

			// Charset
			//email.setCharset(StandardCharsets.UTF_8.name());
			
			// Connection timeout
			if (_smtpConnectTimeout >= 0)
				email.setSocketConnectionTimeout(_smtpConnectTimeout);

			// SMTP PORT
			if (_smtpPort >= 0)
				email.setSmtpPort(_smtpPort);

			// USE SSL
			if (_useSsl)
				email.setSSLOnConnect(_useSsl);

			// SSL PORT
			if (_sslPort >= 0)
				email.setSslSmtpPort(_sslPort+""); // Hmm why is this a String parameter?

			// START TLS
			if (_startTls)
				email.setStartTLSEnabled(_startTls);
			
			// AUTHENTICATION
			if (StringUtil.hasValue(_username))
				email.setAuthentication(_username, _password);
			
			// add TO and CC
			for (String to : _toList)
				email.addTo(to);

			for (String cc : _ccList)
				email.addCc(cc);

			// FROM & SUBJECT
			email.setFrom(_from);
			email.setSubject(msgSubject);

			// CONTENT HTMP or PLAIN
			if (StringUtils.startsWithIgnoreCase(msgBody.trim(), "<html>"))
				email.setHtmlMsg(msgBody);
			else
				email.setTextMsg(msgBody);
			
			// SEND
			email.send();

			_logger.info("Sent mail message: msgBodySizeKb="+msgBodySizeKb+", host='"+_smtpHostname+"', to='"+_to+"', cc='"+_cc+"', subject='"+msgSubject+"'.");
		}
		catch (Exception ex)
		{
			_logger.error("Problems sending mail (msgBodySizeKb="+msgBodySizeKb+", host='"+_smtpHostname+"', to='"+_to+"', cc='"+_cc+"', subject='"+msgSubject+"').", ex);
		}
	}

	@Override
	public List<CmSettingsHelper> getAvailableSettings()
	{
		ArrayList<CmSettingsHelper> list = new ArrayList<>();
		
		Configuration conf = Configuration.getCombinedConfiguration();

		list.add( new CmSettingsHelper("hostname",         Type.MANDATORY, PROPKEY_smtpHostname,           String .class, conf.getProperty       (PROPKEY_smtpHostname          , DEFAULT_smtpHostname          ), DEFAULT_smtpHostname          , "Name of the host that holds the smtp server"));
		list.add( new CmSettingsHelper("to",               Type.MANDATORY, PROPKEY_to,                     String .class, conf.getProperty       (PROPKEY_to                    , DEFAULT_to                    ), DEFAULT_to                    , "To what mail adresses should we send the mail (if several ones, just comma separate them)"));
		list.add( new CmSettingsHelper("from",             Type.MANDATORY, PROPKEY_from,                   String .class, conf.getProperty       (PROPKEY_from                  , DEFAULT_from                  ), DEFAULT_from                  , "What should be the senders email address"));
		list.add( new CmSettingsHelper("Subject-Template",                 PROPKEY_subjectTemplate,        String .class, conf.getProperty       (PROPKEY_subjectTemplate       , DEFAULT_subjectTemplate       ), DEFAULT_subjectTemplate       , "What should be the subject (Note: this is a template)"));
		list.add( new CmSettingsHelper("Msg-Template",                     PROPKEY_msgBodyTemplate,        String .class, conf.getProperty       (PROPKEY_msgBodyTemplate       , DEFAULT_msgBodyTemplate       ), DEFAULT_msgBodyTemplate       , "What content should we send (Note: this is a template, if the content starts with <html> then it will try to send the mail as a HTML mail.)"));
		list.add( new CmSettingsHelper("Msg-Template-Use-HTML",            PROPKEY_msgBodyTemplateUseHtml, Boolean.class, conf.getBooleanProperty(PROPKEY_msgBodyTemplateUseHtml, DEFAULT_msgBodyTemplateUseHtml), DEFAULT_msgBodyTemplateUseHtml, "If '"+PROPKEY_msgBodyTemplate+"' is not specified, then use a HTML Template as the default."));

		list.add( new CmSettingsHelper("username",                         PROPKEY_username,               String .class, conf.getProperty       (PROPKEY_username              , DEFAULT_username              ), DEFAULT_username              , "If the SMTP server reuires you to login (default: is not to logon)"));
		list.add( new CmSettingsHelper("password",                         PROPKEY_password,               String .class, conf.getProperty       (PROPKEY_password              , DEFAULT_password              ), DEFAULT_password              , "If the SMTP server reuires you to login (default: is not to logon)"));
		list.add( new CmSettingsHelper("cc",                               PROPKEY_cc,                     String .class, conf.getProperty       (PROPKEY_cc                    , DEFAULT_cc                    ), DEFAULT_cc                    , "To what CC mail adresses should we send the mail (if several ones, just comma separate them)"));
		list.add( new CmSettingsHelper("smtp-port",                        PROPKEY_smtpPort,               Integer.class, conf.getIntProperty    (PROPKEY_smtpPort              , DEFAULT_smtpPort              ), DEFAULT_smtpPort              , "What port number is the SMTP server on (-1 = use the default)"));
		list.add( new CmSettingsHelper("ssl-port",                         PROPKEY_sslPort,                Integer.class, conf.getIntProperty    (PROPKEY_sslPort               , DEFAULT_sslPort               ), DEFAULT_sslPort               , "What port number is the SSL-SMTP server on (-1 = use the default)"));
		list.add( new CmSettingsHelper("use-ssl",                          PROPKEY_useSsl,                 Boolean.class, conf.getBooleanProperty(PROPKEY_useSsl                , DEFAULT_useSsl                ), DEFAULT_useSsl                , "Sets whether SSL/TLS encryption should be enabled for the SMTP transport upon connection (SMTPS/POPS)"));
		list.add( new CmSettingsHelper("start-tls",                        PROPKEY_startTls,               Boolean.class, conf.getBooleanProperty(PROPKEY_startTls              , DEFAULT_startTls              ), DEFAULT_startTls              , "Set required STARTTLS encryption. "));
		list.add( new CmSettingsHelper("connection-timeout",               PROPKEY_connectionTimeout,      Integer.class, conf.getIntProperty    (PROPKEY_connectionTimeout     , DEFAULT_connectionTimeout     ), DEFAULT_connectionTimeout     , "Set the socket connection timeout value in milliseconds. (-1 = use the default)"));

		list.add( new CmSettingsHelper("DbxCentralUrl",                    PROPKEY_dbxCentralUrl,          String .class, conf.getProperty       (PROPKEY_dbxCentralUrl         , DEFAULT_dbxCentralUrl         ), DEFAULT_dbxCentralUrl         , "Where is the DbxCentral located, if you want your template/messages to include it using ${dbxCentralUrl}", new UrlInputValidator()));

		return list;
	}

	//-------------------------------------------------------
	// class members
	//-------------------------------------------------------
	private String  _smtpHostname           = "";
	private String  _to                     = "";
	private String  _from                   = "";
	private String  _subjectTemplate        = "";
	private String  _msgBodyTemplate        = "";
	private boolean _msgBodyTemplateUseHtml = DEFAULT_msgBodyTemplateUseHtml;
                                         
	private String  _username               = "";
	private String  _password               = "";
	private String  _cc                     = "";
	private int     _smtpPort               = -1;
	private int     _sslPort                = -1;
	private boolean _useSsl                 = DEFAULT_useSsl;
	private boolean _startTls               = DEFAULT_startTls;
	private int     _smtpConnectTimeout     = -1;

	private List<String> _toList     = new ArrayList<>();
	private List<String> _ccList     = new ArrayList<>();
	//-------------------------------------------------------

	@Override
	public void init(Configuration conf) throws Exception
	{
		super.init(conf);

		_logger.info("Initializing the AlarmWriter component named '"+getName()+"'.");

		_smtpHostname           = conf.getProperty       (PROPKEY_smtpHostname,           DEFAULT_smtpHostname);
		_to                     = conf.getProperty       (PROPKEY_to,                     DEFAULT_to);
		_cc                     = conf.getProperty       (PROPKEY_cc,                     DEFAULT_cc);
		_from                   = conf.getProperty       (PROPKEY_from,                   DEFAULT_from);
		_subjectTemplate        = conf.getProperty       (PROPKEY_subjectTemplate,        DEFAULT_subjectTemplate);
		_msgBodyTemplate        = conf.getProperty       (PROPKEY_msgBodyTemplate,        DEFAULT_msgBodyTemplate);
		_msgBodyTemplateUseHtml = conf.getBooleanProperty(PROPKEY_msgBodyTemplateUseHtml, DEFAULT_msgBodyTemplateUseHtml);
                            
		_username               = conf.getProperty       (PROPKEY_username,               DEFAULT_username);
		_password               = conf.getProperty       (PROPKEY_password,               DEFAULT_password);
		_smtpPort               = conf.getIntProperty    (PROPKEY_smtpPort,               DEFAULT_smtpPort);
		_sslPort                = conf.getIntProperty    (PROPKEY_sslPort,                DEFAULT_sslPort);
		_useSsl                 = conf.getBooleanProperty(PROPKEY_useSsl,                 DEFAULT_useSsl);
		_startTls               = conf.getBooleanProperty(PROPKEY_startTls,               DEFAULT_startTls);
		_smtpConnectTimeout     = conf.getIntProperty    (PROPKEY_connectionTimeout,      DEFAULT_connectionTimeout);

		//------------------------------------------
		// Check for mandatory parameters
		//------------------------------------------
		if ( StringUtil.isNullOrBlank(_smtpHostname   ) ) throw new Exception("The property '" + PROPKEY_smtpHostname    + "' is mandatory for the AlarmWriter named '"+getName()+"'.");
		if ( StringUtil.isNullOrBlank(_to             ) ) throw new Exception("The property '" + PROPKEY_to              + "' is mandatory for the AlarmWriter named '"+getName()+"'.");
		if ( StringUtil.isNullOrBlank(_from           ) ) throw new Exception("The property '" + PROPKEY_from            + "' is mandatory for the AlarmWriter named '"+getName()+"'.");
		if ( StringUtil.isNullOrBlank(_subjectTemplate) ) throw new Exception("The property '" + PROPKEY_subjectTemplate + "' is mandatory for the AlarmWriter named '"+getName()+"'.");
		if ( StringUtil.isNullOrBlank(_msgBodyTemplate) ) throw new Exception("The property '" + PROPKEY_msgBodyTemplate + "' is mandatory for the AlarmWriter named '"+getName()+"'.");

		// Parse the 'to string' into a list
		_toList = StringUtil.parseCommaStrToList(_to);
		
		if (StringUtil.hasValue(_cc))
			_ccList = StringUtil.parseCommaStrToList(_cc);

		//------------------------------------------
		// Check for valid configuration
		//------------------------------------------
	}

	@Override
	public void printConfig()
	{
		int spaces = 45;
		_logger.info("Configuration for Alarm Writer Module: "+getName());
		_logger.info("    " + StringUtil.left(PROPKEY_smtpHostname          , spaces) + ": " + _smtpHostname);
		_logger.info("    " + StringUtil.left(PROPKEY_to                    , spaces) + ": " + _to);
		_logger.info("    " + StringUtil.left(PROPKEY_from                  , spaces) + ": " + _from);
		_logger.info("    " + StringUtil.left(PROPKEY_subjectTemplate       , spaces) + ": " + _subjectTemplate);
		_logger.info("    " + StringUtil.left(PROPKEY_msgBodyTemplate       , spaces) + ": " + _msgBodyTemplate);
		_logger.info("    " + StringUtil.left(PROPKEY_msgBodyTemplateUseHtml, spaces) + ": " + _msgBodyTemplateUseHtml);

		_logger.info("    " + StringUtil.left(PROPKEY_username              , spaces) + ": " + _username);
		_logger.info("    " + StringUtil.left(PROPKEY_password              , spaces) + ": " + (_logger.isDebugEnabled() ? _password : "*secret*") );
		_logger.info("    " + StringUtil.left(PROPKEY_cc                    , spaces) + ": " + _cc);
		_logger.info("    " + StringUtil.left(PROPKEY_smtpPort              , spaces) + ": " + _smtpPort);
		_logger.info("    " + StringUtil.left(PROPKEY_sslPort               , spaces) + ": " + _sslPort);
		_logger.info("    " + StringUtil.left(PROPKEY_useSsl                , spaces) + ": " + _useSsl);
		_logger.info("    " + StringUtil.left(PROPKEY_startTls              , spaces) + ": " + _startTls);
		_logger.info("    " + StringUtil.left(PROPKEY_connectionTimeout     , spaces) + ": " + _smtpConnectTimeout);
	}


	public static final String  PROPKEY_smtpHostname           = "AlarmWriterToMail.smtp.hostname";
	public static final String  DEFAULT_smtpHostname           = "";
                                                               
	public static final String  PROPKEY_to                     = "AlarmWriterToMail.to";
	public static final String  DEFAULT_to                     = "";
	                                                           
	public static final String  PROPKEY_from                   = "AlarmWriterToMail.from";
	public static final String  DEFAULT_from                   = "";
	                                                           
	public static final String  PROPKEY_subjectTemplate        = "AlarmWriterToMail.msg.subject.template";
	public static final String  DEFAULT_subjectTemplate        = "${type}: ${serviceName} - ${alarmClassAbriviated} - ${extraInfo}";
	                                                           
	public static final String  PROPKEY_msgBodyTemplate        = "AlarmWriterToMail.msg.body.template";
	public static final String  DEFAULT_msgBodyTemplate        = createMsgBodyTemplate();

	public static final String  PROPKEY_msgBodyTemplateUseHtml = "AlarmWriterToMail.msg.body.template.default.html";
	public static final boolean DEFAULT_msgBodyTemplateUseHtml = true;

	
	public static final String  PROPKEY_username               = "AlarmWriterToMail.smtp.username";
	public static final String  DEFAULT_username               = "";
                                                               
	public static final String  PROPKEY_password               = "AlarmWriterToMail.smpt.password";
	public static final String  DEFAULT_password               = "";
                                                               
	public static final String  PROPKEY_cc                     = "AlarmWriterToMail.cc";
	public static final String  DEFAULT_cc                     = "";
                                                               
	public static final String  PROPKEY_smtpPort               = "AlarmWriterToMail.smpt.port";
	public static final int     DEFAULT_smtpPort               = -1;
                                                               
	public static final String  PROPKEY_sslPort                = "AlarmWriterToMail.ssl.port";
	public static final int     DEFAULT_sslPort                = -1;
                                                               
	public static final String  PROPKEY_useSsl                 = "AlarmWriterToMail.ssl.use";
	public static final boolean DEFAULT_useSsl                 = false;
                                                               
	public static final String  PROPKEY_startTls               = "AlarmWriterToMail.start.tls";
	public static final boolean DEFAULT_startTls               = false;
                                                               
	public static final String  PROPKEY_connectionTimeout      = "AlarmWriterToMail.smtp.connect.timeout";
	public static final int     DEFAULT_connectionTimeout      = -1;
	
	public static String createMsgBodyTemplate()
	{
		boolean useHtmlTemplate = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_msgBodyTemplateUseHtml, DEFAULT_msgBodyTemplateUseHtml);
		if (useHtmlTemplate)
			return createHtmlMsgBodyTemplate();
		else
			return createTextMsgBodyTemplate();
	}
	
//	public static String createTextMsgBodyTemplate()
//	{
//		return ""
//			+ "Type:     ${type}  (${duration})\n"
//			+ "\n"
//			+ "Server:   ${serviceName}\n"
//			+ "Alarm:    ${alarmClassAbriviated}\n"
//			+ "\n"
//			+ "Category: ${category}\n"
//			+ "Severity: ${severity}\n"
//			+ "State:    ${state}\n"
//			+ "\n"
//			+ "Info:     ${extraInfo}\n"
//			+ "Time:     ${crTimeStr}\n"
//			+ "\n"
//			+ "${description}\n"
//			+ "\n"
//			+ "${extendedDescription}\n"
//			;
//	}
	public static String createTextMsgBodyTemplate()
	{
		return ""
			+ "Type:      ${type}  (${duration})\n"
			+ "\n"
			+ "Server:    ${serviceName}\n"
			+ "Alarm:     ${alarmClassAbriviated}\n"
			+ "Collector: ${serviceInfo}\n"
			+ "\n"
			+ "Category:  ${category}\n"
			+ "Severity:  ${severity}\n"
			+ "State:     ${state}\n"
			+ "\n"
			+ "Info:      ${extraInfo}\n"
			+ "Time:      ${crTimeStr}\n"
			+ "\n"
			+ "\n"
			+ "Alarm description:\n"
			+ "==================================================================\n"
			+ "${description}\n"
			+ "------------------------------------------------------------------\n"
			+ "#if ( ${extendedDescription} != '' )\n"
			+ "\n"
			+ "\n"
			+ "Extended description:\n"
			+ "==================================================================\n"
			+ "${extendedDescription}\n"
			+ "------------------------------------------------------------------\n"
			+ "#end\n"
			;
	}
	public static String createHtmlMsgBodyTemplate() // Note: this is not tested (at least not reading the text in Outlook or similar)
	{
		return ""
			+ "<html> \n"
				
			+ "<head> \n"
			+ "    <style> \n"
			+ "        body  { font-family: Arial, Helvetica, sans-serif; } \n"
			+ "        pre   { font-size: 10px; word-wrap: none; white-space: no-wrap; space: nowrap; } \n"
//			+ "        table { border-collapse: collapse;} \n"
//			+ "        th    { border: 1px solid black; text-align: left; padding: 2px; white-space: nowrap; background-color:gray; color:white; } \n"
//			+ "        td    { border: 1px solid black; text-align: left; padding: 2px; white-space: nowrap; } \n"
//			+ "        tr:nth-child(even) { background-color: #f2f2f2; } \n"
			+ "    </style> \n"
			+ "</head> \n"
			
			+ "<table>\n"
			+ "  <tr> <td><b>Type:      </b></td> <td>${type}                 </td> </tr>\n"
			+ "#if (${type} == 'CANCEL' || ${type} == 'RE-RAISE')\n"
			+ "  <tr> <td><b>Duration:  </b></td> <td>${duration} (MM:SS)     </td> </tr>\n"
			+ "  <tr> <td><b>ReRaiseCnt:</b></td> <td>${reRaiseCount}         </td> </tr>\n"
			+ "#end\n"
			+ "  <tr> <td><b>Active Cnt:</b></td> <td>${activeAlarmCount}     </td> </tr>\n"
			+ "\n"
			+ "  <td colspan='2'>&nbsp; </td>\n"
			+ "  <tr> <td><b>Server:    </b></td> <td>${serviceName}          </td> </tr>\n"
			+ "  <tr> <td><b>Alarm:     </b></td> <td>${alarmClassAbriviated} </td> </tr>\n"
			+ "  <tr> <td><b>Collector: </b></td> <td>${serviceInfo}          </td> </tr>\n"
			+ "\n"
			+ "  <td colspan='2'>&nbsp; </td>\n"
			+ "  <tr> <td><b>Category:  </b></td> <td>${category}             </td> </tr>\n"
			+ "  <tr> <td><b>Severity:  </b></td> <td>${severity}             </td> </tr>\n"
			+ "  <tr> <td><b>State:     </b></td> <td>${state}                </td> </tr>\n"
			+ "\n"
			+ "  <td colspan='2'>&nbsp; </td>\n"
			+ "  <tr> <td><b>Info:      </b></td> <td>${extraInfo}            </td> </tr>\n"
			+ "  <tr> <td><b>Raise Time:</b></td> <td>${crTimeStr}            </td> </tr>\n"
			
			+ "#if (${type} == 'RE-RAISE')\n"
			+ "  <tr> <td><b>ReRaise Time:</b></td> <td>${reRaiseTimeStr}     </td> </tr>\n"
			+ "#end\n"
			
			+ "#if (${type} == 'CANCEL')\n"
			+ "  <tr> <td><b>Cancel Time:</b></td> <td>${cancelTimeStr}       </td> </tr>\n"
			+ "#end\n"

			+ "</table>\n"
			
			+ "<br>\n"
			+ "<b>Alarm description:</b>\n"
			+ "<hr>\n"
			+ "${description}\n"
			
			// Only if we have a extended description
			+ "#if ( ${extendedDescription} != '' )\n"
			+ "<br>\n"
			+ "<br>\n"
			+ "<br>\n"
			+ "<b>Extended Description:</b>\n"
			+ "<hr>\n"
			+ "${extendedDescription}\n"
			+ "#end\n"
			
			// Only on CANCEL -- write last known Description
			+ "#if ( ${type} == 'CANCEL' && ${reRaiseDescription} != '' )\n"
			+ "<br>\n"
			+ "<br>\n"
			+ "<br>\n"
			+ "<b>Last Known Description:</b>\n"
			+ "<hr>\n"
			+ "${reRaiseDescription}\n"
			+ "#end\n"

			// Only on CANCEL -- write last known EXTENDED Description
			+ "#if ( ${type} == 'CANCEL' && ${reRaiseExtendedDescription} != '' )\n"
			+ "<br>\n"
			+ "<br>\n"
			+ "<br>\n"
			+ "<b>Last Known Extended Description:</b>\n"
			+ "<hr>\n"
			+ "${reRaiseExtendedDescription}\n"
			+ "#end\n"
			
			+ "</html>\n"
			;
		}
}
