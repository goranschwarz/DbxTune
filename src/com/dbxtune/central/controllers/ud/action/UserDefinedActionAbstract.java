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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.central.controllers.ud.action;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.DbxTuneCentral;
import com.dbxtune.central.controllers.HtmlStatic;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

public abstract class UserDefinedActionAbstract
implements IUserDefinedAction
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String  PROPKEY_name                   = "name";
	public static final String  PROPKEY_actionType             = "actionType";
	public static final String  PROPKEY_command                = "command";
	public static final String  PROPKEY_description            = "description";
	public static final String  PROPKEY_refresh                = "refresh";
	public static final String  PROPKEY_authorizedRoles        = "authorized.roles";
	public static final String  PROPKEY_authorizedUsers        = "authorized.users";
	public static final String  PROPKEY_logFilename            = "logFilename";

	public static final String  PROPKEY_requiresReasonMessage  = "requiresReasonMessage";
	public static final boolean DEFAULT_requiresReasonMessage  = false;

	public static final String  PROPKEY_mail_smtpHostname           = "mail.smtp.hostname";
	public static final String  DEFAULT_mail_smtpHostname           = "";

	public static final String  PROPKEY_mail_smtpUsername           = "mail.smtp.username";
	public static final String  DEFAULT_mail_smtpUsername           = "";

	public static final String  PROPKEY_mail_smtpPassword           = "mail.smtp.password";
	public static final String  DEFAULT_mail_smtpPassword           = "";

	public static final String  PROPKEY_mail_smtpPort               = "mail.smtp.port";
	public static final int     DEFAULT_mail_smtpPort               = -1;

	public static final String  PROPKEY_mail_sslPort                = "mail.ssl.port";
	public static final int     DEFAULT_mail_sslPort                = -1;

	public static final String  PROPKEY_mail_useSsl                 = "mail.ssl.use";
	public static final boolean DEFAULT_mail_useSsl                 = false;

	public static final String  PROPKEY_mail_startTls               = "mail.start.tls";
	public static final boolean DEFAULT_mail_startTls               = false;

	public static final String  PROPKEY_mail_connectionTimeout      = "mail.smtp.connect.timeout";
	public static final int     DEFAULT_mail_connectionTimeout      = -1;

	public static final String  PROPKEY_mail_to                     = "mail.to";
	public static final String  DEFAULT_mail_to                     = "";

	public static final String  PROPKEY_mail_from                   = "mail.from";
	public static final String  DEFAULT_mail_from                   = "";

	public static final String  PROPKEY_mail_cc                     = "mail.cc";
	public static final String  DEFAULT_mail_cc                     = "";
                                                               

	
	private String       _name           ;
	private ActionType   _actionType     ;
	private String       _command        ;
	private String       _description    ;
	private int          _refresh        ;
	private List<String> _authorizedRoles;
	private List<String> _authorizedUsers;
	private String       _logFilename    ;
	private String       _executedByUser ;         // DbxTune login name of the user who triggered this action
	private boolean      _requiresReasonMessage = DEFAULT_requiresReasonMessage;
	private String       _executionReason       = null; // free-text reason entered before execution

	private Configuration _conf;
	private boolean       _isValid;
	private Map<String, String> _urlParameterMap;


	//-------------------------------------------------------
	// Mail variables
	//-------------------------------------------------------
	private String  _mailSmtpHostname           = "";
	private String  _mailTo                     = "";
	private String  _mailFrom                   = "";
                                         
	private String  _mailSmtpUsername           = "";
	private String  _mailSmtpPassword           = "";
	private String  _mailCc                     = "";
	private int     _mailSmtpPort               = -1;
	private int     _mailSslPort                = -1;
	private boolean _mailUseSsl                 = DEFAULT_mail_useSsl;
	private boolean _mailStartTls               = DEFAULT_mail_startTls;
	private int     _mailSmtpConnectTimeout     = -1;
	//-------------------------------------------------------
	
	@Override
	public    boolean isValid()                 { return _isValid; }
	protected void    setValid(boolean isValid) { _isValid = isValid; }

	public Configuration getConfig()
	{
		return _conf;
	}

	@Override public String       getName()            { return _name; }
	@Override public ActionType   getActionType()      { return _actionType; }
	@Override public String       getCommand()         { return _command; }
	@Override public String       getDescription()     { return _description; }
	@Override public List<String> getAuthorizedRoles() { return _authorizedRoles == null ? Collections.emptyList() : _authorizedRoles; }
	@Override public List<String> getAuthorizedUsers() { return _authorizedUsers == null ? Collections.emptyList() : _authorizedUsers; }
	@Override public String       getLogFilename()     { return _logFilename; }
	@Override public String       getConfigFilename()  { return _conf.getFilename(); }
	@Override public int          getPageRefreshTime() { return _refresh; }
	@Override public String       getExecutedByUser()  { return StringUtil.nullToValue(_executedByUser, "-unknown-"); }
	@Override public String       getExecutionReason() { return _executionReason; }

	@Override public boolean      isReasonMessageRequired() { return _requiresReasonMessage; }

	          public void setName           (String name       ) { _name            = name; }
	          public void setCommand        (String command    ) { _command         = command; }
	          public void setDescription    (String description) { _description     = description; }
	          public void setLogFilename    (String logFilename) { _logFilename     = logFilename; }
	@Override public void setPageRefreshTime(int    refresh    ) { _refresh         = refresh; }
	@Override public void setExecutedByUser (String username   ) { _executedByUser  = username; }
	@Override public void setExecutionReason(String reason     ) { _executionReason = (reason == null ? null : reason.trim()); }

	

	/**
	 * Constructor
	 */
	public UserDefinedActionAbstract(Configuration conf)
	throws Exception
	{
		_conf = conf;
		init(conf);
	}
		
	public void logDebug(String message) { log("DEBUG"  , message); }
	public void logInfo (String message) { log("INFO"   , message); }
	public void logWarn (String message) { log("WARNING", message); }
	public void logError(String message) { log("ERROR"  , message); }
	public void log(String type, String message) 
	{
		String logfile = getLogFilename();
		if (StringUtil.isNullOrBlank(logfile))
		{
			if      ("DEBUG"  .equals(type)) _logger.debug(message);
			else if ("INFO"   .equals(type)) _logger.info (message);
			else if ("WARNING".equals(type)) _logger.warn (message);
			else if ("ERROR"  .equals(type)) _logger.error(message);
		}
		else
		{
			Path path = Paths.get(logfile);
			
			String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
			
			String logEntry = timestamp + " - " + StringUtil.left(type, 7) + " - " + message + System.lineSeparator();

			// Append to file (creates file if it doesn't exist)
			try
			{
				Files.writeString(path, 
						logEntry,
						StandardCharsets.UTF_8,
						StandardOpenOption.CREATE, 
						StandardOpenOption.APPEND);
			}
			catch (IOException ex)
			{
				_logger.error("Problems writing " + type + " message=|" + message + "| to file '" + path + "'.", ex);
			}
		}
	}

	public void printConfig()
	{
		int spaces = 45;
		_logger.info("Mail Configuration for User Defined Action: " + getName());
		_logger.info("    " + StringUtil.left(PROPKEY_mail_smtpHostname          , spaces) + ": " + _mailSmtpHostname);
		_logger.info("    " + StringUtil.left(PROPKEY_mail_to                    , spaces) + ": " + _mailTo);
		_logger.info("    " + StringUtil.left(PROPKEY_mail_from                  , spaces) + ": " + _mailFrom);

		_logger.info("    " + StringUtil.left(PROPKEY_mail_smtpUsername          , spaces) + ": " + _mailSmtpUsername);
		_logger.info("    " + StringUtil.left(PROPKEY_mail_smtpPassword          , spaces) + ": " + (_logger.isDebugEnabled() ? _mailSmtpPassword : "*secret*") );
		_logger.info("    " + StringUtil.left(PROPKEY_mail_cc                    , spaces) + ": " + _mailCc);
		_logger.info("    " + StringUtil.left(PROPKEY_mail_smtpPort              , spaces) + ": " + _mailSmtpPort);
		_logger.info("    " + StringUtil.left(PROPKEY_mail_sslPort               , spaces) + ": " + _mailSslPort);
		_logger.info("    " + StringUtil.left(PROPKEY_mail_useSsl                , spaces) + ": " + _mailUseSsl);
		_logger.info("    " + StringUtil.left(PROPKEY_mail_startTls              , spaces) + ": " + _mailStartTls);
		_logger.info("    " + StringUtil.left(PROPKEY_mail_connectionTimeout     , spaces) + ": " + _mailSmtpConnectTimeout);
	}
	
	public void init(Configuration conf)
	throws Exception
	{
		_name                    = conf.getMandatoryProperty(PROPKEY_name);
		_actionType              = ActionType.fromString(conf.getMandatoryProperty(PROPKEY_actionType));
		_command                 = conf.getMandatoryProperty(PROPKEY_command);
		_description             = conf.getProperty         (PROPKEY_description           , null);
//		_authorization           = conf.getProperty         (PROPKEY_authorization         , null);
		_authorizedRoles         = StringUtil.parseCommaStrToList(conf.getProperty(PROPKEY_authorizedRoles, null));
		_authorizedUsers         = StringUtil.parseCommaStrToList(conf.getProperty(PROPKEY_authorizedUsers, null));
		_logFilename             = conf.getProperty         (PROPKEY_logFilename           , null);
		_refresh                 = conf.getIntProperty      (PROPKEY_refresh               , -1);
		_requiresReasonMessage   = conf.getBooleanProperty  (PROPKEY_requiresReasonMessage , DEFAULT_requiresReasonMessage);

		// Mail stuff
		_mailSmtpHostname           = conf.getProperty       (PROPKEY_mail_smtpHostname,           DEFAULT_mail_smtpHostname);
		_mailTo                     = conf.getProperty       (PROPKEY_mail_to,                     DEFAULT_mail_to);
		_mailCc                     = conf.getProperty       (PROPKEY_mail_cc,                     DEFAULT_mail_cc);
		_mailFrom                   = conf.getProperty       (PROPKEY_mail_from,                   DEFAULT_mail_from);
                            
		_mailSmtpUsername           = conf.getProperty       (PROPKEY_mail_smtpUsername,           DEFAULT_mail_smtpUsername);
		_mailSmtpPassword           = conf.getProperty       (PROPKEY_mail_smtpPassword,           DEFAULT_mail_smtpPassword);
		_mailSmtpPort               = conf.getIntProperty    (PROPKEY_mail_smtpPort,               DEFAULT_mail_smtpPort);
		_mailSslPort                = conf.getIntProperty    (PROPKEY_mail_sslPort,                DEFAULT_mail_sslPort);
		_mailUseSsl                 = conf.getBooleanProperty(PROPKEY_mail_useSsl,                 DEFAULT_mail_useSsl);
		_mailStartTls               = conf.getBooleanProperty(PROPKEY_mail_startTls,               DEFAULT_mail_startTls);
		_mailSmtpConnectTimeout     = conf.getIntProperty    (PROPKEY_mail_connectionTimeout,      DEFAULT_mail_connectionTimeout);

		
		//------------------------------------------
		// Check for mandatory parameters
		//------------------------------------------
		if (StringUtil.hasValue(_mailTo))
		{
			if ( StringUtil.isNullOrBlank(_mailSmtpHostname) ) throw new Exception("The property '" + PROPKEY_mail_smtpHostname    + "' is mandatory for the UserDefinedAction named '"+getName()+"'.");
//			if ( StringUtil.isNullOrBlank(_mailFrom        ) ) throw new Exception("The property '" + PROPKEY_mail_from            + "' is mandatory for the UserDefinedAction named '"+getName()+"'.");
		}

		
		setValid(true);

		if (_name != null && _name.equalsIgnoreCase("FROM_FILENAME"))
		{
			String name    = getFromFileName(0, conf.getFilename());  // 0 == indexPos, logicalPos is 1 
			String srvName = getFromFileName(1, conf.getFilename());  // 1 == indexPos, logicalPos is 2 
//			_name = name + "@" + srvName; 
			_name = name; 
		}

		// -----------------------------------------------
		// BEGIN: Check for mandatory fields
		// -----------------------------------------------
		// already done with: getMandatoryProperty("prop.key")
		

		// Check if we have any authorized ROLES or USERS
		if (    (_authorizedRoles != null && _authorizedRoles.isEmpty()) 
		     && (_authorizedUsers != null && _authorizedUsers.isEmpty())
		   )
		{
			String msg = "One of the properties '" + PROPKEY_authorizedRoles + "' or '" + PROPKEY_authorizedUsers + "' is Mandatory.";
			_logger.error(msg);
			_description = msg;
			setValid(false);
			throw new Exception(msg);
		}

		// Check if the log file has any path/prefix... 
		// If NOT inject the DbxCentral log directory
		if (StringUtil.hasValue(_logFilename))
		{
			Path path = Paths.get(_logFilename);

			// Check if path has a parent directory, if NOT add DbxCentrals log directory
			if (path.getParent() == null)
			{
				path = Paths.get(DbxTuneCentral.getAppLogDir(), _logFilename);
				_logFilename = path.toString();

				_logger.info("The logfile for the User Defined Action '" + getName() + "' will be '" + _logFilename + "'.");
			}

			// Create parent directories if they don't exist
			Path parentDir = path.getParent();
			if (parentDir != null && !Files.exists(parentDir)) 
			{
				_logger.info("The parent directories for logfile '" + _logFilename + "' does not exists. Trying to create them. This is for the User Defined Action '" + getName() + "'.");
				Files.createDirectories(parentDir);
			}
		}
	}

//	@Override
//	public String getUrl()
//	{
//		return "/api/udaction"
//				+ "?name="    + getName()
////				+ "&srvName=" + getDbmsServerName()
//				+ ( getRefresh() <= 0 ? "" : "&refresh=" + getRefresh() )
////				+ "&showKeys=false"
////				+ "&onlyLevelZero=false"
////				+ "&startTime="+_defaultStartTime
//				;
//	}
	@Override
	public String getUrl()
	{
		return "/api/udaction"
				+ "?name="    + getName()
				+ ( getPageRefreshTime() <= 0 ? "" : "&refresh=" + getPageRefreshTime() )
				;
	}

	
	
	/**
	 * Here is where the send mail happens
	 * @param action
	 * @param alarmEvent
	 */
	private void sendMailMessage(String msg)
	{
		// If we don't have a any 'to' address we can't send...
		if (StringUtil.isNullOrBlank(_mailTo))
			return;

		// Get server name from the Alarm
//		String serverName = getOnServerName();

		// Getting mail addresses to send the report to
//		List<String> toList = MailHelper.getMailToAddressForServerNameAsList(serverName, alarmEvent, PROPKEY_to, _to);
		List<String> toList = StringUtil.commaStrToList(_mailTo, true);
		List<String> ccList = StringUtil.commaStrToList(_mailCc, true);

		// replace variables in the template with runtime variables
//		String msgSubject = WriterUtils.createMessageFromTemplate(action, alarmEvent, _subjectTemplate, true, null, getDbxCentralUrl());
//		String msgBody    = WriterUtils.createMessageFromTemplate(action, alarmEvent, _msgBodyTemplate, true, null, getDbxCentralUrl());

		String reasonSuffix = StringUtil.hasValue(getExecutionReason()) ? " [reason: " + getExecutionReason() + "]" : "";
		String msgSubject = "Dbx: User Action '" + getName() + "' on '" + getOnServerName() + "' was executed by '" + getExecutedByUser() + "'" + reasonSuffix;
		String msgBody    = msg;
		
		int msgBodySizeKb = msgBody == null ? 0 : msgBody.length() / 1024;

		try
		{
			HtmlEmail email = new HtmlEmail();

			email.setHostName(_mailSmtpHostname);

			// Charset
			email.setCharset(StandardCharsets.UTF_8.name());
			
			// Connection timeout
			if (_mailSmtpConnectTimeout >= 0)
				email.setSocketConnectionTimeout( Duration.ofSeconds(_mailSmtpConnectTimeout) );

			// SMTP PORT
			if (_mailSmtpPort >= 0)
				email.setSmtpPort(_mailSmtpPort);

			// USE SSL
			if (_mailUseSsl)
				email.setSSLOnConnect(_mailUseSsl);

			// SSL PORT
			if (_mailSslPort >= 0)
				email.setSslSmtpPort(_mailSslPort+""); // Hmm why is this a String parameter?

			// START TLS
			if (_mailStartTls)
				email.setStartTLSEnabled(_mailStartTls);
			
			// AUTHENTICATION
			if (StringUtil.hasValue(_mailSmtpUsername))
				email.setAuthentication(_mailSmtpUsername, _mailSmtpPassword);
			
			// add TO
			for (String to : toList)
				email.addTo(to);

			// add CC
			for (String cc : ccList)
				email.addCc(cc);

			// FROM
//			email.setFrom(_from);
			String fromMailAddress = StringUtil.parseMailFromAddress_getEmailAddress(_mailFrom);
			String fromDisplayName = StringUtil.parseMailFromAddress_getDisplayName (_mailFrom);
			email.setFrom(fromMailAddress, fromDisplayName);

			// SUBJECT
			email.setSubject(msgSubject);

			// CONTENT HTMP or PLAIN
			if (StringUtils.startsWithIgnoreCase(msgBody.trim(), "<html>") || StringUtils.startsWithIgnoreCase(msgBody.trim(), "<!DOCTYPE html>"))
				email.setHtmlMsg(msgBody);
			else
				email.setTextMsg(msgBody);
			
			// SEND
			email.send();

			_logger.info("Sent mail message: msgBodySizeKb=" + msgBodySizeKb + ", host='" + _mailSmtpHostname + "', to='" + toList + "', subject='" + msgSubject + "', for UDA name '" + getName() + "', on server '" + getOnServerName() + "'.");
		}
		catch (Exception ex)
		{
			_logger.error("Problems sending mail (msgBodySizeKb=" + msgBodySizeKb + ", host='" + _mailSmtpHostname + "', to='" + toList + "', subject='" + msgSubject + "', for UDA name '" + getName() + "'), on server '" + getOnServerName() + "'.", ex);
		}
	}
	

//	@Override
//	public void onConfigFileChange(Path fullPath)
//	{
//		_logger.info("Configuration was changed for '" + getName() + "' with filename '" + fullPath + "'.");
//
//		Configuration newConfig = new Configuration(fullPath.toString());
//		Configuration curConfig = getConfig();
//		
//		// No changes has been made... (the file was saved with same values or touched) no need to continue
//		if (curConfig.equals(newConfig))
//		{
//			_logger.info("NO Config changes was found, someone probably just 'touched' the file... nothing will be done.");
//			return;
//		}
//		
//		try
//		{
//			// Properties that can NOT be changed
//			String new_name       = newConfig.getProperty(PROPKEY_name);
//			String new_actionType = newConfig.getProperty(PROPKEY_actionType);
//
//			if ( ! _name                 .equals(new_name)      ) { throw new Exception("Propery '" + PROPKEY_name       + "' can NOT be changed."); }
//			if ( ! _actionType.toString().equals(new_actionType)) { throw new Exception("Propery '" + PROPKEY_actionType + "' can NOT be changed."); }
//
//
//			// Let the "init" do it's stuff
//			init(newConfig);
//
//			// Print information about what was changed!
//			if ( ! curConfig.getProperty(PROPKEY_description    , "").equals(newConfig.getProperty(PROPKEY_description    , "")) ) { _logger.info("Propery '" + PROPKEY_description     + "' was changed. The new value is '" + newConfig.getProperty(PROPKEY_description    , "") + "'."); }
////			if ( ! curConfig.getProperty(PROPKEY_authorization  , "").equals(newConfig.getProperty(PROPKEY_authorization  , "")) ) { _logger.info("Propery '" + PROPKEY_authorization   + "' was changed. The new value is '" + newConfig.getProperty(PROPKEY_authorization  , "") + "'."); }
//			if ( ! curConfig.getProperty(PROPKEY_authorizedRoles, "").equals(newConfig.getProperty(PROPKEY_authorizedRoles, "")) ) { _logger.info("Propery '" + PROPKEY_authorizedRoles + "' was changed. The new value is '" + newConfig.getProperty(PROPKEY_authorizedRoles, "") + "'."); }
//			if ( ! curConfig.getProperty(PROPKEY_authorizedUsers, "").equals(newConfig.getProperty(PROPKEY_authorizedUsers, "")) ) { _logger.info("Propery '" + PROPKEY_authorizedUsers + "' was changed. The new value is '" + newConfig.getProperty(PROPKEY_authorizedUsers, "") + "'."); }
//			if ( ! curConfig.getProperty(PROPKEY_logFilename    , "").equals(newConfig.getProperty(PROPKEY_logFilename    , "")) ) { _logger.info("Propery '" + PROPKEY_logFilename     + "' was changed. The new value is '" + newConfig.getProperty(PROPKEY_logFilename    , "") + "'."); }
//			if ( ! curConfig.getProperty(PROPKEY_refresh        , "").equals(newConfig.getProperty(PROPKEY_refresh        , "")) ) { _logger.info("Propery '" + PROPKEY_refresh         + "' was changed. The new value is '" + newConfig.getProperty(PROPKEY_refresh        , "") + "'."); }
//			if ( ! curConfig.getProperty(PROPKEY_dbms_sql       , "").equals(newConfig.getProperty(PROPKEY_dbms_sql       , "")) ) { _logger.info("Propery '" + PROPKEY_dbms_sql        + "' was changed. The new value is '" + newConfig.getProperty(PROPKEY_dbms_sql       , "") + "'."); }
//			if ( ! curConfig.getProperty(PROPKEY_dbms_username  , "").equals(newConfig.getProperty(PROPKEY_dbms_username  , "")) ) { _logger.info("Propery '" + PROPKEY_dbms_username   + "' was changed. The new value is '" + newConfig.getProperty(PROPKEY_dbms_username  , "") + "'."); }
//			if ( ! curConfig.getProperty(PROPKEY_dbms_password  , "").equals(newConfig.getProperty(PROPKEY_dbms_password  , "")) ) { _logger.info("Propery '" + PROPKEY_dbms_password   + "' was changed. The new value is '" + newConfig.getProperty(PROPKEY_dbms_password  , "") + "'."); }
//			if ( ! curConfig.getProperty(PROPKEY_dbms_servername, "").equals(newConfig.getProperty(PROPKEY_dbms_servername, "")) ) { _logger.info("Propery '" + PROPKEY_dbms_servername + "' was changed. The new value is '" + newConfig.getProperty(PROPKEY_dbms_servername, "") + "'."); }
//			if ( ! curConfig.getProperty(PROPKEY_dbms_dbname    , "").equals(newConfig.getProperty(PROPKEY_dbms_dbname    , "")) ) { _logger.info("Propery '" + PROPKEY_dbms_dbname     + "' was changed. The new value is '" + newConfig.getProperty(PROPKEY_dbms_dbname    , "") + "'."); }
//			if ( ! curConfig.getProperty(PROPKEY_dbms_url       , "").equals(newConfig.getProperty(PROPKEY_dbms_url       , "")) ) { _logger.info("Propery '" + PROPKEY_dbms_url        + "' was changed. The new value is '" + newConfig.getProperty(PROPKEY_dbms_url       , "") + "'."); }
//			
//			// Switch to the new Configuration Object
//			_conf = newConfig;
//		}
//		catch (Exception ex)
//		{
//			_logger.error("Configuration changes for User Defined Action '" + getName() + "' and file '" + fullPath + "' was REJECTED. reason: " + ex.getMessage(), ex);
//			return;
//		}
//	}

	
	/**
	 * Split the file name on '.' and get the string from the index position 'pos'  
	 * @param pos       index position (starting at 0)
	 * @param filename
	 * @return 
	 * @throws IndexOutOfBoundsException if we can't find the pos.
	 */
	protected String getFromFileName(int pos, final String filename)
	{
		File f = new File(filename);
		String fn = f.getName();
		String[] sa = fn.split("\\.");
		
		if (sa.length < pos)
			throw new IndexOutOfBoundsException("Trying to find array pos " + pos + " on split by '.' from the string '" + filename + "'. The array.length=" + sa.length + " is < pos=" + pos);
		
		return sa[pos];
	}


	@Override
	public List<String> getJavaScriptList()
	{
		return Collections.emptyList();
	}

	@Override
	public List<String> getCssList()
	{
		return Collections.emptyList();
	}
	
	@Override
	public String getOnServerName()
	{
		return "";
	}
	

//	/**
//	 * Replaces any single-quote strings (') with \x27
//	 * @param str
//	 * @return
//	 */
//	protected String escapeJsQuote(String str)
//	{
//		if (str == null)
//			return str;
//		return str.replace("'", "\\x27");
//	}

//	@Override
//	public String getInfoContent()
//	{
//		return _infoContent;
//	}
//
//	@Override
//	public String getContent()
//	{
//		return _content;
//	}

//	@Override
//	public void createInfoContent(PrintWriter out) 
//	throws IOException
//	{
//	}
	@Override
	public void createInfoContent(PrintWriter out)
	throws IOException
	{
		out.println("<div id='servlet-params'> ");
		out.println("Known Parameters: <code>" + StringUtil.toCommaStr(getKnownParameters()) + "</code><br> ");
		out.println("Used Variables: <code>" + getUrlParameters() + "</code><br> ");
//		out.println("Scroll to: ");
//		out.println("<a href='#' onClick=\"scrollToTop('timeline')\">Top</a>  ");
//		out.println(" or ");
//		out.println("<a href='#' onClick=\"scrollToBottom('timeline')\">Bottom</a> ");
//		out.println(" of the Timeline. &emsp; <input type='checkbox' id='autoscroll-to-bottom' onclick='changeAutoscroll()'> On load AutoScroll to bottom. ");
//		out.println(" <a href='#' data-tooltip=\"" + StringEscapeUtils.escapeHtml4(getDbmsSql()) + "\" onClick='copyExecutedSql()'>Copy Executed SQL</a>");
////		out.println("<br> ");
//		out.println("</div> ");
		out.println("");

		out.println("<div id='parameter-descriptions'> ");
		out.println("Parameter Description:<br> ");
		out.println(getParameterDescriptionHtmlTable());
		out.println("<br> ");
		out.println("</div> ");
		out.println("");

//		out.println("<details open>");
		out.println("<details>");
		out.println("<summary>");
		out.println("  <b>Executed Command:</b><br> ");
		out.println("</summary>");
		out.println("  <div id='executed_command'> ");
		out.println("  <pre> ");
		out.println(StringEscapeUtils.escapeHtml4(getCommand()));
		out.println("  </pre> ");
		out.println("  </div> ");
		out.println("</details>");
		out.println("<br> ");
		out.println("");
	}


	
//	@Override
//	public void createContent(PrintWriter out) 
//	throws IOException
//	{
//	}

//	/**
//	 * Set content
//	 * 
//	 * @param content
//	 */
//	public void setInfoContent(String infoContent)
//	{
//		_infoContent = infoContent;
//	}

//	/**
//	 * Set content
//	 * 
//	 * @param content
//	 */
//	public void setContent(String content)
//	{
//		_content = content;
//	}

	
	@Override
	public void setUrlParameters(Map<String, String> parameterMap)
	{
		_urlParameterMap = parameterMap;
	}

	@Override
	public Map<String, String> getUrlParameters()
	{
		return _urlParameterMap != null ? _urlParameterMap : Collections.emptyMap();
	}
	
	public String getParameterDescriptionHtmlTable()
	{
		Map<String, String> map = getParameterDescription();
		if (map == null)
			return "";

		StringBuilder sb = new StringBuilder();

		// START tag
		sb.append("<table class='parameter-description'> \n");

		// TBODY
		sb.append("<tbody> \n");
		for (Entry<String, String> entry : map.entrySet())
		{
			sb.append("  <tr> \n");
			sb.append("    <td nowrap><b>").append( entry.getKey()   ).append("</b></td> \n");
			sb.append("    <td nowrap>")   .append( entry.getValue() ).append("</td> \n");
			sb.append("  </tr> \n");
		}
		sb.append("</tbody> \n");

		// END tag
		sb.append("</table> \n");
		
		return sb.toString();
	}


	/**
	 * Utility method to escape various JavaScript Characters
	 * @param s
	 * @return
	 */
	protected String escapeJavaScript(String s)
	{
//		private String escapeJavaScript(String s)
//		{
//			return s.replace("\\", "\\\\")
//					.replace("'", "\\'")
//					.replace("\n", "\\n")
//					.replace("\r", "\\r");
//		}
		if (s == null) return "";
		return s.replace("\\", "\\\\")
				.replace("'", "\\'")
				.replace("\"", "\\\"")
				.replace("\n", "\\n")
				.replace("\r", "\\r")
				.replace("\t", "\\t")
				.replace("<", "\\x3C")
				.replace(">", "\\x3E")
				.replace("&", "\\x26");
	}
	
	/**
	 * Utility method to escape various HTML Characters
	 * @param s
	 * @return
	 */
	protected String escapeHtml(String s)
	{
		if (s == null) return "";
		return s.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;");
	}
	
//	@Override
//	public void produce(PrintWriter servletOutput)
//	throws Exception
//	{
//		// If we want to send mail with the output
//		// Capture the output
//		if (StringUtil.hasValue(_mailTo))
//		{
//			// Prepare to capture output for the email
//			StringWriter mailCapture = new StringWriter();
//
//			// Use TeeWriter to write to both the response and our StringWriter
//			TeeWriter teeWriter = new TeeWriter(servletOutput, mailCapture);
//			PrintWriter combinedWriter = new PrintWriter(teeWriter);
//			
//	        produceContent(combinedWriter);
//	        combinedWriter.flush();
//	        
//			// Send the email with the captured content
//			String contentForEmail = mailCapture.toString();
//			sendMailMessage(contentForEmail);
//		}
//		else
//		{
//			produceContent(servletOutput);
//		}
//	}

	@Override
	public void produce(PrintWriter servletOutput)
	throws Exception
	{
		// Prepare to capture output for the email
		PrintWriter  mailOutput = null;
		StringWriter mailOutputStrWriter = null;

		// If we want to send mail with the output, create a print writer for the mail portion
		if (StringUtil.hasValue(_mailTo))
		{
			mailOutputStrWriter = new StringWriter();
			mailOutput          = new PrintWriter(mailOutputStrWriter);
		}

		// Now produce content
		produceContent(servletOutput, mailOutput);

		// Send the email with the captured content
		if (mailOutputStrWriter != null)
		{
			mailOutput.flush();
			String contentForEmail = mailOutputStrWriter.toString();
			mailOutput.close();

			sendMailMessage(contentForEmail);
		}
	}
	
	protected void println(PrintWriter pageOut, PrintWriter mailOut, String msg)
	{
		if (pageOut != null) pageOut.println(msg);
		if (mailOut != null) mailOut.println(msg);
	}

	public void produceContent(PrintWriter pageOut, PrintWriter mailOut)
	throws Exception
	{
		println(pageOut, mailOut, "<!DOCTYPE html>");
		println(pageOut, mailOut, "<html>");
		println(pageOut, mailOut, "<head>");

		pageOut.println("<title>" + getOnServerName() + " - " + getName() + "</title> ");

		if (getPageRefreshTime() > 0)
			pageOut.println("<meta http-equiv='refresh' content='" + getPageRefreshTime() + "' />");

		pageOut.println(HtmlStatic.getUserDefinedContentHead());
		if (mailOut != null)
			mailOut.println("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
		
//		println(pageOut, mailOut, "<style type='text/css'>");
//		println(pageOut, mailOut, "  table {border-collapse: collapse;}");
//		println(pageOut, mailOut, "  th {border: 1px solid black; text-align: left; padding: 2px; white-space: nowrap; background-color:gray; color:white;}");
//		println(pageOut, mailOut, "  td {border: 1px solid black; text-align: left; padding: 2px; white-space: nowrap; }");
//		println(pageOut, mailOut, "  tr:nth-child(even) {background-color: #f2f2f2;}");
////		println(pageOut, mailOut, "  .topright { position: absolute; top: 8px; right: 16px; font-size: 14px; }"); // topright did not work with bootstrap (and navigation bar) 
//		println(pageOut, mailOut, "</style>");

		println(pageOut, mailOut, "<style type='text/css'>");
		println(pageOut, mailOut, "    /* The below data-tooltip is used to show Actual exected SQL Text, as a tooltip where a normalized text is in a table cell */ ");
		println(pageOut, mailOut, "    [data-tooltip] { ");
		println(pageOut, mailOut, "        position: relative; ");
		println(pageOut, mailOut, "    } ");
		println(pageOut, mailOut, "");
		println(pageOut, mailOut, "    /* 'tooltip' CSS settings for SQL Text... */ ");
		println(pageOut, mailOut, "    [data-tooltip]:hover::before { ");
		println(pageOut, mailOut, "        content: attr(data-tooltip);		 ");
//		println(pageOut, mailOut, "        content: 'Click to Open Text Dialog...'; ");
		println(pageOut, mailOut, "        position: absolute; ");
		println(pageOut, mailOut, "        z-index: 103; ");
		println(pageOut, mailOut, "        top: 20px; ");
		println(pageOut, mailOut, "        left: 30px; ");
		println(pageOut, mailOut, "        width: 1000px; ");
		println(pageOut, mailOut, "        height: 900px; ");
//		println(pageOut, mailOut, "        width: 220px; ");
		println(pageOut, mailOut, "        padding: 10px; ");
		println(pageOut, mailOut, "        background: #454545; ");
		println(pageOut, mailOut, "        color: #fff; ");
		println(pageOut, mailOut, "        font-size: 11px; ");
		println(pageOut, mailOut, "        font-family: Courier; ");
		println(pageOut, mailOut, "        white-space: pre-wrap; ");
		println(pageOut, mailOut, "    } ");
		println(pageOut, mailOut, "    [data-title]:hover::after { ");
		println(pageOut, mailOut, "        content: ''; ");
		println(pageOut, mailOut, "        position: absolute; ");
		println(pageOut, mailOut, "        bottom: -12px; ");
		println(pageOut, mailOut, "        left: 8px; ");
		println(pageOut, mailOut, "        border: 8px solid transparent; ");
		println(pageOut, mailOut, "        border-bottom: 8px solid #000; ");
		println(pageOut, mailOut, "    } ");
		println(pageOut, mailOut, "");
//		println(pageOut, mailOut, "    table, th, td { ");
//		println(pageOut, mailOut, "    parameter-description, parameter-description th, parameter-description td { ");
		println(pageOut, mailOut, "    table.parameter-description th, ");
		println(pageOut, mailOut, "    table.parameter-description td { ");
		println(pageOut, mailOut, "        border: 1px solid black; ");
		println(pageOut, mailOut, "        border-collapse: collapse; ");
		println(pageOut, mailOut, "        padding: 5px; ");
		println(pageOut, mailOut, "    } ");
		println(pageOut, mailOut, "</style>");

		println(pageOut, mailOut, "</head>");
		
		println(pageOut, mailOut, "<body onload='updateLastUpdatedClock()'>");

		pageOut.println(HtmlStatic.getUserDefinedContentNavbar());

		pageOut.println("<script>");
		pageOut.println("function updateLastUpdatedClock() {                   ");
		pageOut.println("    var ageInSec = Math.floor((new Date() - lastUpdateTime) / 1000);");
		pageOut.println("    document.getElementById('last-update-ts').innerHTML = ageInSec + ' seconds ago'; ");
//		pageOut.println("    console.log('updateLastUpdatedClock(): ' + document.getElementById('last-update-ts'));");
//		pageOut.println("    console.log('updateLastUpdatedClock(): ' + ageInSec + ' seconds ago');");
		pageOut.println("    setTimeout(updateLastUpdatedClock, 1000); ");
		pageOut.println("}                                                     ");
		pageOut.println("var lastUpdateTime = new Date();");
		pageOut.println("</script>");
		pageOut.println("");
		
		for (String cssLocation : getCssList())
		{
			pageOut.println("<link rel='stylesheet' href='" + cssLocation + "'>");
		}

		for (String scriptLocation : getJavaScriptList())
		{
			pageOut.println("<script type='text/javascript' src='" + scriptLocation + "'></script>");
		}

//		println(pageOut, mailOut, "<div class='container-fluid'>");
//		println(pageOut, mailOut, "<div class='container mt-5'>");
		pageOut.println("<div class='container-fluid'>");
		if (mailOut != null)
			mailOut.println("<div style='width:100%;max-width:100%;'>"); // with for mail


//		String ver = "Version: " + Version.getVersionStr() + ", Build: " + Version.getBuildStr();
//		println(pageOut, mailOut, "<h1>DbxTune - Central - " + username + "@" + hostname + "</h1>");
//		println(pageOut, mailOut, "<div class='topright'>"+ver+"</div>");

		// Create a "drop down section" where we will have
		// - When was the page loaded
		// - And various User Defined Information content
		pageOut.println("<details>");
		pageOut.println("<summary>");
		pageOut.println("Show parameters, Page loaded: <span id='last-update-ts'>" + (new Timestamp(System.currentTimeMillis())) + "</span>, ");
		if (getPageRefreshTime() > 0)
			pageOut.println("This page will 'auto-refresh' every " + getPageRefreshTime() + " second. This can be changed with URL parameter 'refresh=##' (where ## is seconds)<br>" );
		else
			pageOut.println("To 'auto-refresh' this page every ## second. This can be changed with URL parameter 'refresh=##' (where ## is seconds)<br>" );
		pageOut.println("</summary>");
		createInfoContent(pageOut);
		pageOut.println("</details>");

		println(pageOut, mailOut, "<div id='ud-content'>");

		println(pageOut, mailOut, "<h3 class='mb-4'>" + getActionType() + " Action: " + getName() + "</h2>");
		// Show execution metadata on both the page and in the mail report
		String metaHtml = "<p style='color:#666; font-size:0.9em; margin-top:-8px;'>"
				+ "Executed by: <strong>" + escapeHtml(getExecutedByUser()) + "</strong>";
		if (StringUtil.hasValue(getExecutionReason()))
			metaHtml += "<br>Reason: <strong>" + escapeHtml(getExecutionReason()) + "</strong>";
		metaHtml += "</p>";
		println(pageOut, mailOut, metaHtml);
		println(pageOut, mailOut, "<div id='dbx-uda-output'></div>"); // We will ADD stuff in here via JavaScript

		//-------------------------------------------------------
		// BEGIN: Produce the User Defined Content
		//-------------------------------------------------------
		createContent(pageOut, mailOut);
		//-------------------------------------------------------
		// END: Produce the User Defined Content
		//-------------------------------------------------------
		
		println(pageOut, mailOut, "</div>"); // ud-content
		
		pageOut.println("</div>"); // container-fluid
		if (mailOut != null)
			mailOut.println("</div>"); // with for mail

		// Write some JavaScript code
		pageOut.println(HtmlStatic.getJavaScriptAtEnd(true));

		println(pageOut, mailOut, "</body>");
		println(pageOut, mailOut, "</html>");
	}
}
