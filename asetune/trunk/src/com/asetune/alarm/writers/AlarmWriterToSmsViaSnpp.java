package com.asetune.alarm.writers;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.writers.snpp.Message;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSettingsHelper.Type;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public class AlarmWriterToSmsViaSnpp
extends AlarmWriterAbstract
{
	private static Logger _logger = Logger.getLogger(AlarmWriterToSmsViaSnpp.class);

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
//		String msgBody = action + ": " + alarmEvent.getShortMessage();
		String msgBody = WriterUtils.createMessageFromTemplate(action, alarmEvent, _msgBodyTemplate, true);
		
		
		Message m = new Message();

		m.setConnectionInfo(_hostname, _port);

		if (StringUtil.hasValue(_pager))
			m.setPager(_pager);

		if (StringUtil.hasValue(_callerIdentifier))
			m.setCallerIdentifier(_callerIdentifier);

		if (StringUtil.hasValue(_subject))
			m.setCallerIdentifier(_subject);

		if (StringUtil.hasValue(_username))
			m.SetLogin(_username, _password);

		m.setMessage(msgBody);

		try 
		{
			m.send();
		} 
		catch (Exception ex) 
		{
			_logger.error("Problems sending SNPP message to server '"+_hostname+":"+_port+"'. Caught: "+ex , ex);
		}

	}
	
	@Override
	public String getDescription()
	{
		return "Send a SMS message via SNPP (Simple Network Paging Protocol)";
	}


	//-------------------------------------------------------
	// class members
	//-------------------------------------------------------
	private String      _hostname         = "";
	private int         _port             = -1;
	private String      _msgBodyTemplate  = "";
	private String      _username         = "";
	private String      _password         = "";
	
	private String      _pager            = "";
	private String      _callerIdentifier = "";
	private String      _subject          = "";
	//-------------------------------------------------------

	@Override
	public void init(Configuration conf) throws Exception
	{
		super.init(conf);

		_logger.info("Initializing the AlarmWriter component named '"+getName()+"'.");

		_hostname         = conf.getProperty   (PROPKEY_hostname,         DEFAULT_hostname);
		_port             = conf.getIntProperty(PROPKEY_port,             DEFAULT_port);
		_msgBodyTemplate  = conf.getProperty   (PROPKEY_msgBodyTemplate,  DEFAULT_msgBodyTemplate);
		_username         = conf.getProperty   (PROPKEY_username,         DEFAULT_username);
		_password         = conf.getProperty   (PROPKEY_password,         DEFAULT_password);

		_pager            = conf.getProperty   (PROPKEY_pager,            DEFAULT_pager);
		_callerIdentifier = conf.getProperty   (PROPKEY_callerIdentifier, DEFAULT_callerIdentifier);
		_subject          = conf.getProperty   (PROPKEY_subject,          DEFAULT_subject);
		
		// Check for mandatory parameters
		if ( StringUtil.isNullOrBlank(_hostname       ) ) throw new Exception("The property '" + PROPKEY_hostname        + "' is mandatory for the AlarmWriter named '"+getName()+"'.");
		if ( _port == -1                                ) throw new Exception("The property '" + PROPKEY_port            + "' is mandatory for the AlarmWriter named '"+getName()+"'.");
		if ( StringUtil.isNullOrBlank(_msgBodyTemplate) ) throw new Exception("The property '" + PROPKEY_msgBodyTemplate + "' is mandatory for the AlarmWriter named '"+getName()+"'.");
	}

	@Override
	public void printConfig()
	{
		int spaces = 45;
		_logger.info("Configuration for Alarm Writer Module: "+getName());
		_logger.info("    " + StringUtil.left(PROPKEY_hostname        , spaces) + ": " + _hostname);
		_logger.info("    " + StringUtil.left(PROPKEY_port            , spaces) + ": " + _port);
		_logger.info("    " + StringUtil.left(PROPKEY_msgBodyTemplate , spaces) + ": " + _msgBodyTemplate);
		_logger.info("    " + StringUtil.left(PROPKEY_username        , spaces) + ": " + _username);
		_logger.info("    " + StringUtil.left(PROPKEY_password        , spaces) + ": " + (_logger.isDebugEnabled() ? _password : "*secret*"));
		_logger.info("    " + StringUtil.left(PROPKEY_pager           , spaces) + ": " + _pager);
		_logger.info("    " + StringUtil.left(PROPKEY_callerIdentifier, spaces) + ": " + _callerIdentifier);
		_logger.info("    " + StringUtil.left(PROPKEY_subject         , spaces) + ": " + _subject);
	}

	@Override
	public List<CmSettingsHelper> getAvailableSettings()
	{
		ArrayList<CmSettingsHelper> list = new ArrayList<>();
		
		Configuration conf = Configuration.getCombinedConfiguration();

		list.add( new CmSettingsHelper("hostname", Type.MANDATORY, PROPKEY_hostname,         String .class, conf.getProperty   (PROPKEY_hostname,         DEFAULT_hostname        ), DEFAULT_hostname        , "Hostname where the SNPP Service is located"));
		list.add( new CmSettingsHelper("port",     Type.MANDATORY, PROPKEY_port,             Integer.class, conf.getIntProperty(PROPKEY_port,             DEFAULT_port            ), DEFAULT_port            , "Port number where the SNPP Service is located"));
		list.add( new CmSettingsHelper("message",  Type.MANDATORY, PROPKEY_msgBodyTemplate,  String .class, conf.getProperty   (PROPKEY_msgBodyTemplate,  DEFAULT_msgBodyTemplate ), DEFAULT_msgBodyTemplate , "Content to send... Note: this is a template where variables will be filled in at runtime."));
		list.add( new CmSettingsHelper("username",                 PROPKEY_username,         String .class, conf.getProperty   (PROPKEY_username,         DEFAULT_username        ), DEFAULT_username        , "If you need to login with a user/password to the SNPP Service"));
		list.add( new CmSettingsHelper("password",                 PROPKEY_password,         String .class, conf.getPropertyRaw(PROPKEY_password,         DEFAULT_password        ), DEFAULT_password        , "If you need to login with a user/password to the SNPP Service"));

		list.add( new CmSettingsHelper("pager",                    PROPKEY_pager,            String .class, conf.getProperty   (PROPKEY_pager,            DEFAULT_pager           ), DEFAULT_pager           , "If you want a specififc 'pager' to be used by the SNPP Service"));
		list.add( new CmSettingsHelper("callerIdentifier",         PROPKEY_callerIdentifier, String .class, conf.getProperty   (PROPKEY_callerIdentifier, DEFAULT_callerIdentifier), DEFAULT_callerIdentifier, "If you want a specififc 'caller id' to be used by the SNPP Service"));
		list.add( new CmSettingsHelper("subject",                  PROPKEY_subject,          String .class, conf.getProperty   (PROPKEY_subject,          DEFAULT_subject         ), DEFAULT_subject         , "If you want to set a specific 'subject' to be used by the SNPP Service"));

		return list;
	}

	public static final String  PROPKEY_hostname         = "AlarmWriterToSmsViaSnpp.hostname";
	public static final String  DEFAULT_hostname         = null;

	public static final String  PROPKEY_port             = "AlarmWriterToSmsViaSnpp.port";
	public static final int     DEFAULT_port             = -1;
	
	public static final String  PROPKEY_msgBodyTemplate  = "AlarmWriterToSmsViaSnpp.msg.template";
	public static final String  DEFAULT_msgBodyTemplate  = "${type}: ${alarmClassAbriviated} - ${serviceName} - ${extraInfo} - ${state} - ${severity} - ${description}";

	public static final String  PROPKEY_username         = "AlarmWriterToSmsViaSnpp.username";
	public static final String  DEFAULT_username         = null;
	
	public static final String  PROPKEY_password         = "AlarmWriterToSmsViaSnpp.password";
	public static final String  DEFAULT_password         = null;
	

	public static final String  PROPKEY_pager            = "AlarmWriterToSmsViaSnpp.pager";
	public static final String  DEFAULT_pager            = null;

	public static final String  PROPKEY_callerIdentifier = "AlarmWriterToSmsViaSnpp.callerIdentifier";
	public static final String  DEFAULT_callerIdentifier = null;

	public static final String  PROPKEY_subject          = "AlarmWriterToSmsViaSnpp.subject";
	public static final String  DEFAULT_subject          = null;
}
