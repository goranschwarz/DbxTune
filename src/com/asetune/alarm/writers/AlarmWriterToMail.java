package com.asetune.alarm.writers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.HtmlEmail;
import org.apache.log4j.Logger;

import com.asetune.alarm.events.AlarmEvent;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSettingsHelper.Type;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public class AlarmWriterToMail
extends AlarmWriterAbstract
{
	private static Logger _logger = Logger.getLogger(AlarmWriterToSyslog.class);

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
		String msgSubject = WriterUtils.createMessageFromTemplate(action, alarmEvent, _subjectTemplate, true);
		String msgBody    = WriterUtils.createMessageFromTemplate(action, alarmEvent, _msgBodyTemplate, true);

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
			if (_useSsl && _sslPort >= 0)
				email.setSslSmtpPort(_sslPort+""); // Hmm why is this a String parameter?

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

			_logger.info("Sent mail message: host='"+_smtpHostname+"', to='"+_to+"', cc='"+_cc+"', subject='"+msgSubject+"'.");
		}
		catch (Exception ex)
		{
			_logger.error("Problems sending mail (host='"+_smtpHostname+"', to='"+_to+"', cc='"+_cc+"', subject='"+msgSubject+"').", ex);
		}
	}

	@Override
	public List<CmSettingsHelper> getAvailableSettings()
	{
		ArrayList<CmSettingsHelper> list = new ArrayList<>();
		
		Configuration conf = Configuration.getCombinedConfiguration();

		list.add( new CmSettingsHelper("hostname",         Type.MANDATORY, PROPKEY_smtpHostname,      String .class, conf.getProperty       (PROPKEY_smtpHostname     , DEFAULT_smtpHostname     ), DEFAULT_smtpHostname     , "Name of the host that holds the smtp server"));
		list.add( new CmSettingsHelper("to",               Type.MANDATORY, PROPKEY_to,                String .class, conf.getProperty       (PROPKEY_to               , DEFAULT_to               ), DEFAULT_to               , "To what mail adresses should we send the mail (if several ones, just comma separate them)"));
		list.add( new CmSettingsHelper("from",             Type.MANDATORY, PROPKEY_from,              String .class, conf.getProperty       (PROPKEY_from             , DEFAULT_from             ), DEFAULT_from             , "What should be the senders email address"));
		list.add( new CmSettingsHelper("Subject-Template", Type.MANDATORY, PROPKEY_subjectTemplate,   String .class, conf.getProperty       (PROPKEY_subjectTemplate  , DEFAULT_subjectTemplate  ), DEFAULT_subjectTemplate  , "What should be the subject (Note: this is a template)"));
		list.add( new CmSettingsHelper("Msg-Template",     Type.MANDATORY, PROPKEY_msgBodyTemplate,   String .class, conf.getProperty       (PROPKEY_msgBodyTemplate  , DEFAULT_msgBodyTemplate  ), DEFAULT_msgBodyTemplate  , "What content should we send (Note: this is a template, if the content starts with <html> then it will try to send the mail as a HTML mail.)"));
                                                                                                                                                                                                                             
		list.add( new CmSettingsHelper("username",                         PROPKEY_username,          String .class, conf.getProperty       (PROPKEY_username         , DEFAULT_username         ), DEFAULT_username         , "If the SMTP server reuires you to login (default: is not to logon)"));
		list.add( new CmSettingsHelper("password",                         PROPKEY_password,          String .class, conf.getProperty       (PROPKEY_password         , DEFAULT_password         ), DEFAULT_password         , "If the SMTP server reuires you to login (default: is not to logon)"));
		list.add( new CmSettingsHelper("cc",                               PROPKEY_cc,                String .class, conf.getProperty       (PROPKEY_cc               , DEFAULT_cc               ), DEFAULT_cc               , "To what CC mail adresses should we send the mail (if several ones, just comma separate them)"));
		list.add( new CmSettingsHelper("smtp-port",                        PROPKEY_smtpPort,          Integer.class, conf.getIntProperty    (PROPKEY_smtpPort         , DEFAULT_smtpPort         ), DEFAULT_smtpPort         , "What port number is the SMTP server on (-1 = use the default)"));
		list.add( new CmSettingsHelper("ssl-port",                         PROPKEY_sslPort,           Integer.class, conf.getIntProperty    (PROPKEY_sslPort          , DEFAULT_sslPort          ), DEFAULT_sslPort          , "What port number is the SSL-SMTP server on (-1 = use the default)"));
		list.add( new CmSettingsHelper("use-ssl",                          PROPKEY_useSsl,            Boolean.class, conf.getBooleanProperty(PROPKEY_useSsl           , DEFAULT_useSsl           ), DEFAULT_useSsl           , "Sets whether SSL/TLS encryption should be enabled for the SMTP transport upon connection (SMTPS/POPS)"));
		list.add( new CmSettingsHelper("connection-timeout",               PROPKEY_connectionTimeout, Integer.class, conf.getIntProperty    (PROPKEY_connectionTimeout, DEFAULT_connectionTimeout), DEFAULT_connectionTimeout, "Set the socket connection timeout value in milliseconds. (-1 = use the default)"));

		return list;
	}

	//-------------------------------------------------------
	// class members
	//-------------------------------------------------------
	private String  _smtpHostname        = "";
	private String  _to                  = "";
	private String  _from                = "";
	private String  _subjectTemplate     = "";
	private String  _msgBodyTemplate     = "";
                                         
	private String  _username            = "";
	private String  _password            = "";
	private String  _cc                  = "";
	private int     _smtpPort            = -1;
	private int     _sslPort             = -1;
	private boolean _useSsl              = false;
	private int     _smtpConnectTimeout  = -1;

	private List<String> _toList     = new ArrayList<>();
	private List<String> _ccList     = new ArrayList<>();
	//-------------------------------------------------------

	@Override
	public void init(Configuration conf) throws Exception
	{
		super.init(conf);

		_logger.info("Initializing the AlarmWriter component named '"+getName()+"'.");

		_smtpHostname       = conf.getProperty       (PROPKEY_smtpHostname,      DEFAULT_smtpHostname);
		_to                 = conf.getProperty       (PROPKEY_to,                DEFAULT_to);
		_from               = conf.getProperty       (PROPKEY_from,              DEFAULT_from);
		_subjectTemplate    = conf.getProperty       (PROPKEY_subjectTemplate,   DEFAULT_subjectTemplate);
		_msgBodyTemplate    = conf.getProperty       (PROPKEY_msgBodyTemplate,   DEFAULT_msgBodyTemplate);
                            
		_username           = conf.getProperty       (PROPKEY_username,          DEFAULT_username);
		_password           = conf.getProperty       (PROPKEY_password,          DEFAULT_password);
		_smtpPort           = conf.getIntProperty    (PROPKEY_smtpPort,          DEFAULT_smtpPort);
		_sslPort            = conf.getIntProperty    (PROPKEY_sslPort,           DEFAULT_sslPort);
		_useSsl             = conf.getBooleanProperty(PROPKEY_useSsl,            DEFAULT_useSsl);
		_smtpConnectTimeout = conf.getIntProperty    (PROPKEY_connectionTimeout, DEFAULT_connectionTimeout);

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
		_logger.info("    " + StringUtil.left(PROPKEY_smtpHostname      , spaces) + ": " + _smtpHostname);
		_logger.info("    " + StringUtil.left(PROPKEY_to                , spaces) + ": " + _to);
		_logger.info("    " + StringUtil.left(PROPKEY_from              , spaces) + ": " + _from);
		_logger.info("    " + StringUtil.left(PROPKEY_subjectTemplate   , spaces) + ": " + _subjectTemplate);
		_logger.info("    " + StringUtil.left(PROPKEY_msgBodyTemplate   , spaces) + ": " + _msgBodyTemplate);

		_logger.info("    " + StringUtil.left(PROPKEY_username          , spaces) + ": " + _username);
		_logger.info("    " + StringUtil.left(PROPKEY_password          , spaces) + ": " + (_logger.isDebugEnabled() ? _password : "*secret*") );
		_logger.info("    " + StringUtil.left(PROPKEY_cc                , spaces) + ": " + _cc);
		_logger.info("    " + StringUtil.left(PROPKEY_smtpPort          , spaces) + ": " + _smtpPort);
		_logger.info("    " + StringUtil.left(PROPKEY_sslPort           , spaces) + ": " + _sslPort);
		_logger.info("    " + StringUtil.left(PROPKEY_useSsl            , spaces) + ": " + _useSsl);
		_logger.info("    " + StringUtil.left(PROPKEY_connectionTimeout , spaces) + ": " + _smtpConnectTimeout);
	}


	public static final String  PROPKEY_smtpHostname       = "AlarmWriterToMail.smtp.hostname";
	public static final String  DEFAULT_smtpHostname       = "";

	public static final String  PROPKEY_to                 = "AlarmWriterToMail.to";
	public static final String  DEFAULT_to                 = "";
	
	public static final String  PROPKEY_from               = "AlarmWriterToMail.from";
	public static final String  DEFAULT_from               = "";
	
	public static final String  PROPKEY_subjectTemplate    = "AlarmWriterToMail.msg.subject.template";
	public static final String  DEFAULT_subjectTemplate    = "${type}: ${alarmClassAbriviated} - ${serviceInfo} - ${extraInfo}";
	
	public static final String  PROPKEY_msgBodyTemplate    = "AlarmWriterToMail.msg.body.template";
	public static final String  DEFAULT_msgBodyTemplate    = createMsgBodyTemplate();

	
	public static final String  PROPKEY_username           = "AlarmWriterToMail.smtp.username";
	public static final String  DEFAULT_username           = "";

	public static final String  PROPKEY_password           = "AlarmWriterToMail.smpt.password";
	public static final String  DEFAULT_password           = "";

	public static final String  PROPKEY_cc                 = "AlarmWriterToMail.cc";
	public static final String  DEFAULT_cc                 = "";

	public static final String  PROPKEY_smtpPort           = "AlarmWriterToMail.smpt.port";
	public static final int     DEFAULT_smtpPort           = -1;

	public static final String  PROPKEY_sslPort            = "AlarmWriterToMail.ssl.port";
	public static final int     DEFAULT_sslPort            = -1;

	public static final String  PROPKEY_useSsl             = "AlarmWriterToMail.ssl.use";
	public static final boolean DEFAULT_useSsl             = false;

	public static final String  PROPKEY_connectionTimeout  = "AlarmWriterToMail.smtp.connect.timeout";
	public static final int     DEFAULT_connectionTimeout  = -1;
	
	public static String createMsgBodyTemplate()
	{
		return ""
			+ "Type:     ${type}  (${duration})\n"
			+ "\n"
			+ "Server:   ${serviceName}\n"
			+ "Alarm:    ${alarmClassAbriviated}\n"
			+ "\n"
			+ "Category: ${category}\n"
			+ "Severity: ${severity}\n"
			+ "State:    ${state}\n"
			+ "\n"
			+ "Info:     ${extraInfo}\n"
			+ "Time:     ${crTimeStr}\n"
			+ "\n"
			+ "${description}\n"
			+ "\n"
			+ "${extendedDescription}\n"
			;
	}
}