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
package com.asetune.pcs.report.senders;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.mail.EmailConstants;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.log4j.Logger;

import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSettingsHelper.Type;
import com.asetune.pcs.report.content.DailySummaryReportContent;
import com.asetune.utils.Configuration;
import com.asetune.utils.FileUtils;
import com.asetune.utils.JsonUtils;
import com.asetune.utils.StringUtil;

public class ReportSenderToMail 
extends ReportSenderAbstract
{
	private static Logger _logger = Logger.getLogger(ReportSenderToMail.class);

	/**
	 * Since we send mail from 2 places... use this to "create and setup" a basic email message
	 * 
	 * @param toList
	 * @return
	 * @throws EmailException
	 */
	private HtmlEmail createAndSetupMailObject(List<String> toList)
	throws EmailException
	{
		HtmlEmail email = new HtmlEmail();

		email.setHostName(_smtpHostname);

		// CharSet
		email.setCharset(EmailConstants.UTF_8);
		
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
		
		// add TO
//		for (String to : _toList)
		for (String to : toList)
			email.addTo(to);

		// add CC
//		for (String cc : _ccList)
//			email.addCc(cc);

		// FROM
//		email.setFrom(_from);
		String fromMailAddress = StringUtil.parseMailFromAddress_getEmailAddress(_from);
		String fromDisplayName = StringUtil.parseMailFromAddress_getDisplayName (_from);
		email.setFrom(fromMailAddress, fromDisplayName);
		
		return email;
	}
	

	/**
	 * SEND the DSR - Daily Summary Report
	 */
	@Override
	public void send(DailySummaryReportContent reportContent)
	{
//		String msgSubject = WriterUtils.createMessageFromTemplate(action, alarmEvent, _subjectTemplate, true);
//		String msgBody    = WriterUtils.createMessageFromTemplate(action, alarmEvent, _msgBodyTemplate, true);

//		String serverName = reportContent.getServerName();
		String serverName = reportContent.getDisplayOrServerName();
		
		if (reportContent.hasNothingToReport())
		{
			if (_sendNtr == false)
			{
				_logger.info("Property '"+PROPKEY_sendNtr+"' is enabled. The report for server '"+serverName+"' will NOT be sent.");
				return;
			}
		}

		// Getting mail addresses to send the report to
		List<String> toList = MailHelper.getMailToAddressForServerNameAsList(serverName, null, PROPKEY_to, _to);
		
		if (toList.isEmpty())
		{
			String toPropVal = ", property: " + PROPKEY_to + "=" + Configuration.getCombinedConfiguration().getProperty(PROPKEY_to, _to);

			_logger.info("Canceling sending Daily Report for server '" + serverName + "' due to NO mail recipiants. toList=" + toList + toPropVal);
			return;
		}

		String msgSubject = _subjectTemplate.replace("${srvName}", serverName);

//		String msgBodyText    = reportContent.getReportAsText();
//		String msgBodyHtml    = reportContent.getReportAsHtml();

//		int msgBodyTextSizeKb = msgBodyText == null ? 0 : msgBodyText.length() / 1024;
//		int msgBodyHtmlSizeKb = msgBodyHtml == null ? 0 : msgBodyHtml.length() / 1024;

//		String  msgBodyHtml   = null;
		String  msgBody       = null;
		File    msgFile       = null;
		long    msgSizeKb     = -1;
		long    msgSizeMb     = -1;
		long    attachSizeKb  = -1;
		long    attachSizeMb  = -1;
		long    fullSizeKb    = -1;
		long    fullSizeMb    = -1;
		boolean hasAttachment = false;
		boolean zipAttachment = false;
		long    zipSaveMb     = -1;
		File    zipFile       = null;

		if (reportContent.hasReportFile())
		{
			msgFile      = reportContent.getReportFile();
			attachSizeKb = reportContent.getReportFile().length() / 1024;
			attachSizeMb = reportContent.getReportFile().length() / 1024 / 1024;
		}

		try 
		{
//			msgBodyHtml = reportContent.getReportAsHtml();
			msgBody     = reportContent.getShortMessage();
			msgSizeKb   = msgBody.length() / 1024;
			msgSizeMb   = msgSizeKb / 1024;

			fullSizeKb  = attachSizeKb == -1 ? msgSizeKb : msgSizeKb + attachSizeKb;
			fullSizeMb  = fullSizeKb / 1024;
		} 
		catch (IOException ex) 
		{
			_logger.error("Problems reading Report Content.", ex);
		}

		
		if (reportContent.hasNothingToReport())
			msgSubject = _subjectNtrTemplate.replace("${srvName}", serverName);


		try
		{
			// Create basic mail object (setup "basic info")
			HtmlEmail email = createAndSetupMailObject(toList);
			
			// SUBJECT
			email.setSubject(msgSubject);

			// CONTENT HTML or PLAIN
			if (reportContent.isShortMessageOfHtml())
			{
				email.setHtmlMsg(msgBody);

				// Client do not have HTML support
				email.setTextMsg("Your email client does not support HTML messages, see DbxCentral to read the report!");
			}
			else
			{
				email.setTextMsg(msgBody);
			}

			// If we also should attach the FULL Report as an ATTACHMENT
			if (_attachHtmlContent && msgFile != null)
			{
				// Should we try to ZIP the file!
				if ((fullSizeMb) >= _attachHtmlZipContentGtMb)
				{
					String zipFilename = msgFile.getAbsolutePath() + ".zip";
					try 
					{
						FileUtils.zipSingleFile(msgFile.toPath(), zipFilename);
						
						zipFile = new File(zipFilename);
						// Delete the file later on...
						//zipFile.deleteOnExit();
						
						msgFile = zipFile;
						zipAttachment = true;
						
						zipSaveMb = (attachSizeKb - (zipFile.length() / 1024)) / 1024;

						// Adjust Sizes etc...
						attachSizeKb = zipFile.length() / 1024;
						attachSizeMb = zipFile.length() / 1024 / 1024;
						
						fullSizeKb  = attachSizeKb == -1 ? msgSizeKb : msgSizeKb + attachSizeKb;
						fullSizeMb  = fullSizeKb / 1024;
					}
					catch (Exception ex)
					{
						_logger.error("Problems ZIP the mail attachement file from '" + msgFile + "' to '" + zipFilename + "'. Sending the plain file instead.", ex);
					}
				}

//				if ((attachSizeMb) < _attachHtmlContentMaxSizeMb)
				if ((fullSizeMb) < _attachHtmlContentMaxSizeMb)
				{
					hasAttachment = true;
					email.attach(msgFile);
				}
				else
				{
					_logger.warn("Skipping attaching HTML Content file to Daily Summary Report. Due to: attached-file-size-to-big. fullSizeMb=" + fullSizeMb + ", msgSizeMb=" + msgSizeMb + ", attachSizeMb=" + attachSizeMb + ", limit=" + _attachHtmlContentMaxSizeMb + ". (the limit can be changed with property '" + PROPKEY_attachHtmlContentMaxSizeMb + "=###'.");
				}
			}

			if (_logger.isDebugEnabled())
			{
				_logger.debug("About to send the following message: \n" + (msgFile == null ? msgBody : msgFile.getAbsolutePath()) );
			}

			// SEND
			email.send();

			_logger.info("Sent mail message: fullSizeKb=" + fullSizeKb + ", fullSizeMb=" + fullSizeMb + ", msgSizeKb=" + msgSizeKb + ", msgSizeMb=" + msgSizeMb + ", hasAttachment=" + hasAttachment + ", zipAttachment=" + zipAttachment + ", zipSaveMb=" + zipSaveMb + ", attachSizeKb=" + attachSizeKb + ", attachSizeMb=" + attachSizeMb + ", attachMaxSizeMb=" + _attachHtmlContentMaxSizeMb + ", host='" + _smtpHostname + "', toList=" + toList + ", subject='" + msgSubject + "', for server name '" + serverName + "'.");
		}
		catch (Exception ex)
		{
			String msg = "Problems sending mail (fullSizeKb=" + fullSizeKb + ", fullSizeMb=" + fullSizeMb + ", msgSizeKb=" + msgSizeKb + ", msgSizeMb=" + msgSizeMb + ", hasAttachment=" + hasAttachment + ", zipAttachment=" + zipAttachment + ", zipSaveMb=" + zipSaveMb + ", attachSizeKb=" + attachSizeKb + ", attachSizeMb=" + attachSizeMb + ", attachMaxSizeMb=" + _attachHtmlContentMaxSizeMb + ", host='" + _smtpHostname + "', toList=" + toList + ", subject='" + msgSubject + "', for server name '" + serverName + "').";
			_logger.error(msg, ex);
			
			// Fallback: Send mail (if possible) with the error information!
			// This will of course only work if the mail is setup correctly... 
			// Typically this will be send for:
			//   * message file too big (when the mail server says the content is to *big*
			sendExceptionMail(toList, serverName, msg, ex);
			
			//-------------------------------------------------------------------------
			// Below is a typical Exception, where the mail is TO BIG
			//-------------------------------------------------------------------------
			// For Server: DBMS_SERVER_NAME 
			// Problem Description: 
			//  
			// Problems sending mail (fullSizeKb=30678, fullSizeMb=29, msgSizeKb=11265, msgSizeMb=11, hasAttachment=true, zipAttachment=true, zipSaveMb=56, attachSizeKb=19413, attachSizeMb=18, attachMaxSizeMb=30, host='relay.maxm.se', toList=[goran.schwarz@maxm.se, sybase-dba-alarms@b3.se], subject='Daily Report for: MM-OP-DW', for server name 'MM-OP-DW').
            // 
			// Exception: 
			//  
			// org.apache.commons.mail.EmailException: Sending the email to the following server failed : relay.maxm.se:25
			//           at org.apache.commons.mail.Email.sendMimeMessage(Email.java:1421)
			//           at org.apache.commons.mail.Email.send(Email.java:1448)
			//           at com.asetune.pcs.report.senders.ReportSenderToMail.send(ReportSenderToMail.java:254)
			//           at com.asetune.pcs.report.DailySummaryReportAbstract.send(DailySummaryReportAbstract.java:209)
			//           at com.asetune.pcs.PersistWriterJdbc.createDailySummaryReport(PersistWriterJdbc.java:5074)
			//           at com.asetune.pcs.PersistWriterJdbc.access$200(PersistWriterJdbc.java:103)
			//           at com.asetune.pcs.PersistWriterJdbc$2.run(PersistWriterJdbc.java:790)
			//           at java.lang.Thread.run(Thread.java:750)
			// Caused by: com.sun.mail.smtp.SMTPSendFailedException: 552 5.3.4 Error: message file too big
    		// 	         at com.sun.mail.smtp.SMTPTransport.issueSendCommand(SMTPTransport.java:2267)
			// 	         at com.sun.mail.smtp.SMTPTransport.finishData(SMTPTransport.java:2045)
			// 	         at com.sun.mail.smtp.SMTPTransport.sendMessage(SMTPTransport.java:1260)
			// 	         at javax.mail.Transport.send0(Transport.java:255)
			// 	         at javax.mail.Transport.send(Transport.java:124)
			// 	         at org.apache.commons.mail.Email.sendMimeMessage(Email.java:1411)
			// 	         ... 7 more
			//-------------------------------------------------------------------------
			
			// Should we try to send this once again, WITHOUT any Attachment?
			// Note: do not trust 'message file too big' the message may be localized
			String exStr = StringUtil.exceptionToString(ex); 
			if (exStr.contains("Exception: 552 5.3.4 Error: ") || exStr.contains("message file too big")) 
			{
				// Possibly send mail again without any attachment
				sendNoAttachmentMail(toList, msgSubject, msgBody, reportContent.isShortMessageOfHtml());
			}
			
		}
		finally
		{
			// Delete the ZIP file if we have one.
			if (zipFile != null)
				zipFile.delete();
		}
	}

	private void sendNoAttachmentMail(List<String> toList, String subject, String msgBody, boolean isMsgHtml)
	{
		String msgSubject  = subject + " (fallback: no-attachment)";

		try
		{
			// Create basic mail object (setup "basic info")
			HtmlEmail email = createAndSetupMailObject(toList);
			
			// SUBJECT
			email.setSubject(msgSubject);

			// CONTENT HTML or PLAIN
			if (isMsgHtml)
			{
				email.setHtmlMsg(msgBody);

				// Client do not have HTML support
				email.setTextMsg("Your email client does not support HTML messages, see DbxCentral to read the report!");
			}
			else
			{
				email.setTextMsg(msgBody);
			}

			// SEND
			email.send();
		}
		catch (Exception ex)
		{
			String msg = "Problems sending DSR FALLBACK Error mail. Subject: " + msgSubject;
			_logger.error(msg, ex);
		}
		
	}

	/**
	 * If we had problems sending the Daily Summary Mail... use this to <b>try</b> to send a mail informing about the error...
	 * 
	 * @param toList             To the following addresses
	 * @param serverName         Name of the DBMS server name we tried to send message for
	 * @param problemDesc        Short info about sizes etc
	 * @param problemException   The Exception originally grabbed when sending mail
	 */
	private void sendExceptionMail(List<String> toList, String serverName, String problemDesc, Exception problemException)
	{
		String msgSubject      = "Problems sending DSR email for '" + serverName + "'.";

		String msgPlainContent = ""
				+ "ProblemDesc: " + problemDesc      + "\n"
				+ "Exception: "   + problemException + "\n"
				+ "";

		String msgHtmlContent  = "" 
				+ "<html> \n"
				+ "<head> \n"
				+ "    <style> \n" 
				+ "        body  { font-family: Arial, Helvetica, sans-serif; } \n" 
				+ "        pre   { font-size: 10px; word-wrap: none; white-space: no-wrap; space: nowrap; } \n"
				+ "    </style> \n"
				+ "</head> \n"
				+ "<body> \n"
				+ ""
				+ "<b>For Server:</b> " + serverName + "\n"
				+ "<br>"
				+ ""
				+ "<b>Problem Description:</b> \n"
				+ "<pre> \n"
				+ problemDesc + "\n"
				+ "</pre> \n"
				+ "<br>"
				+ ""
				+ "<b>Exception:</b> \n"
				+ "<pre> \n"
				+ StringUtil.exceptionToString(problemException) + "\n"
				+ "</pre> \n"
				+ "<br>"
				+ ""
				+ "</body> \n"
				+ "</html> \n"
				+ "";

		try
		{
			// Create basic mail object (setup "basic info")
			HtmlEmail email = createAndSetupMailObject(toList);
			
			// SUBJECT
			email.setSubject(msgSubject);

			// CONTENT HTML or PLAIN
			email.setHtmlMsg(msgHtmlContent);

			// Client do not have HTML support
			email.setTextMsg(msgPlainContent);

			// SEND
			email.send();
		}
		catch (Exception ex)
		{
			String msg = "Problems sending DSR FALLBACK Error mail. Subject: " + msgSubject;
			_logger.error(msg, ex);
		}
	}
	

//	@Override
	public List<CmSettingsHelper> getAvailableSettings()
	{
		ArrayList<CmSettingsHelper> list = new ArrayList<>();
		
		Configuration conf = Configuration.getCombinedConfiguration();

		list.add( new CmSettingsHelper("from",             Type.MANDATORY, PROPKEY_from,                       String .class, conf.getProperty       (PROPKEY_from                      , DEFAULT_from                      ), DEFAULT_from                      , "What should be the senders email address"));
		list.add( new CmSettingsHelper("hostname",         Type.MANDATORY, PROPKEY_smtpHostname,               String .class, conf.getProperty       (PROPKEY_smtpHostname              , DEFAULT_smtpHostname              ), DEFAULT_smtpHostname              , "Name of the host that holds the smtp server"));
		list.add( new CmSettingsHelper("to",               Type.MANDATORY, PROPKEY_to,                         String .class, conf.getProperty       (PROPKEY_to                        , DEFAULT_to                        ), DEFAULT_to                        , "To what mail adresses should we send the mail (if several ones, just comma separate them). Note: this can be configured to filter on <i>serverName</i>, in the property <code>ReportSenderToMail.to=[ {#serverName#:#xxx#, #to#:#user1@acme.com, user2@acme.com#}, {#serverName#:#yyy#, #to#:#user1@acme.com#}, {#serverName#:#zzz#, #to#:#user3@acme.com#}, {#fallback#:#true#, #to#:#user4@acme.com#} ]</code>".replace('#', '"') ));
//		list.add( new CmSettingsHelper("cc",                               PROPKEY_cc,                         String .class, conf.getProperty       (PROPKEY_cc                        , DEFAULT_cc                        ), DEFAULT_cc                        , "To what CC mail adresses should we send the mail (if several ones, just comma separate them)"));
		list.add( new CmSettingsHelper("Subject-Template",                 PROPKEY_subjectTemplate,            String .class, conf.getProperty       (PROPKEY_subjectTemplate           , DEFAULT_subjectTemplate           ), DEFAULT_subjectTemplate           , "What should be the subject (Note: this is a template)"));
		list.add( new CmSettingsHelper("Subject-NTR-Template",             PROPKEY_subjectNtrTemplate,         String .class, conf.getProperty       (PROPKEY_subjectNtrTemplate        , DEFAULT_subjectNtrTemplate        ), DEFAULT_subjectNtrTemplate        , "What should be the subject, when NTR=Nothing To Report (Note: this is a template)"));
		list.add( new CmSettingsHelper("Send-NothingToReport",             PROPKEY_sendNtr,                    Boolean.class, conf.getBooleanProperty(PROPKEY_sendNtr                   , DEFAULT_sendNtr                   ), DEFAULT_sendNtr                   , "Send email even when there is Nothing To Report (NTR)."));
//		list.add( new CmSettingsHelper("Msg-Template",                     PROPKEY_msgBodyTemplate,            String .class, conf.getProperty       (PROPKEY_msgBodyTemplate           , DEFAULT_msgBodyTemplate           ), DEFAULT_msgBodyTemplate           , "What content should we send (Note: this is a template, if the content starts with <html> then it will try to send the mail as a HTML mail.)"));
//		list.add( new CmSettingsHelper("Msg-Template-Use-HTML",            PROPKEY_msgBodyTemplateUseHtml,     Boolean.class, conf.getBooleanProperty(PROPKEY_msgBodyTemplateUseHtml    , DEFAULT_msgBodyTemplateUseHtml    ), DEFAULT_msgBodyTemplateUseHtml    , "If '"+PROPKEY_msgBodyTemplate+"' is not specified, then use a HTML Template as the default."));
//		list.add( new CmSettingsHelper("Msg-Use-HTML",                     PROPKEY_msgBodyUseHtml,             Boolean.class, conf.getBooleanProperty(PROPKEY_msgBodyUseHtml            , DEFAULT_msgBodyUseHtml            ), DEFAULT_msgBodyUseHtml            , "Send HTML message."));
		list.add( new CmSettingsHelper("Attach-HTML-content",              PROPKEY_attachHtmlContent,          Boolean.class, conf.getBooleanProperty(PROPKEY_attachHtmlContent         , DEFAULT_attachHtmlContent         ), DEFAULT_attachHtmlContent         , "Attach the content as a HTML file (might be easier to open/read)"));
		list.add( new CmSettingsHelper("Attach-HTML-zip-content-gt-mb",    PROPKEY_attachHtmlZipContentGtMb,   Integer.class, conf.getIntProperty    (PROPKEY_attachHtmlZipContentGtMb  , DEFAULT_attachHtmlZipContentGtMb  ), DEFAULT_attachHtmlZipContentGtMb  , "If the Attached HTML content is greater than XX MB, then ZIP it before it's sent."));
		list.add( new CmSettingsHelper("Attach-HTML-content-maxSizeMb",    PROPKEY_attachHtmlContentMaxSizeMb, Integer.class, conf.getIntProperty    (PROPKEY_attachHtmlContentMaxSizeMb, DEFAULT_attachHtmlContentMaxSizeMb), DEFAULT_attachHtmlContentMaxSizeMb, "Only attach HTML file if the file size is below this value in MB (some receivers do not allow large messages)"));

		list.add( new CmSettingsHelper("username",                         PROPKEY_username,                   String .class, conf.getProperty       (PROPKEY_username                  , DEFAULT_username                  ), DEFAULT_username                  , "If the SMTP server reuires you to login (default: is not to logon)"));
		list.add( new CmSettingsHelper("password",                         PROPKEY_password,                   String .class, conf.getProperty       (PROPKEY_password                  , DEFAULT_password                  ), DEFAULT_password                  , "If the SMTP server reuires you to login (default: is not to logon)"));
		list.add( new CmSettingsHelper("smtp-port",                        PROPKEY_smtpPort,                   Integer.class, conf.getIntProperty    (PROPKEY_smtpPort                  , DEFAULT_smtpPort                  ), DEFAULT_smtpPort                  , "What port number is the SMTP server on (-1 = use the default)"));
		list.add( new CmSettingsHelper("ssl-port",                         PROPKEY_sslPort,                    Integer.class, conf.getIntProperty    (PROPKEY_sslPort                   , DEFAULT_sslPort                   ), DEFAULT_sslPort                   , "What port number is the SSL-SMTP server on (-1 = use the default)"));
		list.add( new CmSettingsHelper("use-ssl",                          PROPKEY_useSsl,                     Boolean.class, conf.getBooleanProperty(PROPKEY_useSsl                    , DEFAULT_useSsl                    ), DEFAULT_useSsl                    , "Sets whether SSL/TLS encryption should be enabled for the SMTP transport upon connection (SMTPS/POPS)"));
		list.add( new CmSettingsHelper("start-tls",                        PROPKEY_startTls,                   Boolean.class, conf.getBooleanProperty(PROPKEY_startTls                  , DEFAULT_startTls                  ), DEFAULT_startTls                  , "Set required STARTTLS encryption. "));
		list.add( new CmSettingsHelper("connection-timeout",               PROPKEY_connectionTimeout,          Integer.class, conf.getIntProperty    (PROPKEY_connectionTimeout         , DEFAULT_connectionTimeout         ), DEFAULT_connectionTimeout         , "Set the socket connection timeout value in milliseconds. (-1 = use the default)"));

		return list;
	}

	//-------------------------------------------------------
	// class members
	//-------------------------------------------------------
	private String  _smtpHostname               = "";
	private String  _to                         = "";
	private String  _from                       = "";
	private String  _subjectTemplate            = "";
	private String  _subjectNtrTemplate         = ""; // NothingToReport
	private boolean _sendNtr                    = DEFAULT_sendNtr;
//	private String  _msgBodyTemplate            = "";
//	private boolean _msgBodyTemplateUseHtml     = DEFAULT_msgBodyTemplateUseHtml;
//	private boolean _msgBodyUseHtml             = DEFAULT_msgBodyUseHtml;
	private boolean _attachHtmlContent          = DEFAULT_attachHtmlContent;
	private int     _attachHtmlZipContentGtMb   = DEFAULT_attachHtmlZipContentGtMb;
	private int     _attachHtmlContentMaxSizeMb = DEFAULT_attachHtmlContentMaxSizeMb;
                                         
	private String  _username                   = "";
	private String  _password                   = "";
//	private String  _cc                         = "";
	private int     _smtpPort                   = -1;
	private int     _sslPort                    = -1;
	private boolean _useSsl                     = DEFAULT_useSsl;
	private boolean _startTls                   = DEFAULT_startTls;
	private int     _smtpConnectTimeout         = -1;

//	private List<String> _toList     = new ArrayList<>();
//	private List<String> _ccList     = new ArrayList<>();
	//-------------------------------------------------------


	/**
	 * This method is called <b>early</b> possibly in <code>DailySummaryReportFactory.isCreateReportEnabledForServer(serverName)</code><br>
	 * This to check if a <i>potentially costly</i> report should be created or not!<br>
	 * which means that the senders etc hasn't been fully created/initialized<br>
	 * <p> 
	 * ToMail can be configured to filter on <i>serverName</i>, in the property <code>ReportSenderToMail.to=[ {"serverName":"xxx", "to":"user1@acme.com, user2@acme.com"}, {"serverName":"yyy", "to":"user1@acme.com"}, {"serverName":"zzz", "to":"user3@acme.com"} ]</code>
	 */
	@Override
	public boolean isEnabledForServer(String serverName)
	{
		return StringUtil.hasValue( MailHelper.getMailToAddressForServerName(serverName, null, PROPKEY_to, _to) );

		// TODO: not yet implemented
		//return true;
	}
	
//	private static List<String> getMailToAddressForServerNameAsList(String serverName, String propKeyTo, String defaultTo)
//	{
//		String toStr = getMailToAddressForServerName(serverName, propKeyTo, defaultTo);
//		return StringUtil.parseCommaStrToList(toStr);
//	}
//	
//	private static String getMailToAddressForServerName(String serverName, String propKeyTo, String defaultTo)
//	{
//		Configuration conf = Configuration.getCombinedConfiguration();
//		
//		String retStr = "";
//
//		// If the "to" sender is of JSON Content, then we need to parse the JSON and see if any of the entries is enabled for this serverName  
////		String toStr = conf.getProperty(PROPKEY_to, DEFAULT_to);
//		String toStr = conf.getProperty(propKeyTo, defaultTo);
//		
//		if (_logger.isDebugEnabled())
//			_logger.debug("getMailToAddressForServerName('"+serverName+"'): "+propKeyTo+"=|"+toStr+"|");
//
//		// Exit early if not properly configured.
//		if (StringUtil.isNullOrBlank(toStr))
//		{
//			_logger.warn("getMailToAddressForServerName('"+serverName+"'): "+propKeyTo+"=|"+toStr+"| is EMPTY... This is a Mandatory property value.");
//			return null;
//		}
//
//		
//		retStr = toStr;
//		if (JsonUtils.isPossibleJson(toStr))
//		{
//			try
//			{
//				JsonArray jsonArr = new JsonParser().parse(toStr).getAsJsonArray();
//				for (JsonElement jsonElement : jsonArr)
//				{
//					JsonObject jsonObj = jsonElement.getAsJsonObject();
//					
//					if (_logger.isDebugEnabled())
//						_logger.debug("Checking serverName='" + serverName + "' against the JSON entry: " + jsonObj );
//
//					if (jsonObj.has("serverName") && jsonObj.has("to"))
//					{
//						String entryServerName = jsonObj.get("serverName").getAsString();
//						String entryTo         = jsonObj.get("to"        ).getAsString();
//						
//						if (_logger.isDebugEnabled())
//							_logger.debug("Checking serverName='" + serverName + "' against the JSON entry with: serverNameRegExp='" + entryServerName + "', toStr='" + entryTo + "'.");
//
//						if (StringUtil.hasValue(entryServerName) && StringUtil.hasValue(entryTo))
//						{
//							// USE REGEXP to check if it matches
//							if (serverName.matches(entryServerName))
//							{
//								if (_logger.isDebugEnabled())
//									_logger.debug("MATCH: using mail address to='" + entryTo + "' for serverName='" + serverName + "'.");
//
//								return entryTo;
//							}
//						}
//					}
//					else
//					{
//						_logger.info("Skipping JSON entry '" + jsonObj + "', it dosn't contain members: 'serverName' and 'to'.");
//					}
//				}
//			}
//			catch(Exception ex)
//			{
//				_logger.error("getMailToAddressForServerName('"+serverName+"'): Trying to parse the JSON Array String '" + toStr + "', Caught: " + ex, ex);
//			}
//		}
//		else
//		{
//			_logger.debug("getMailToAddressForServerName('"+serverName+"'): NOT a JSON Array, using as 'plain-email-address': "+propKeyTo+"=|"+toStr+"|");
//		}
//		
//		return retStr;
//	}

	@Override
	public void init() throws Exception
	{
//		super.init(conf);
		Configuration conf = Configuration.getCombinedConfiguration();

		_logger.info("Initializing the ReportSender component named '"+getName()+"'.");

		_smtpHostname               = conf.getProperty       (PROPKEY_smtpHostname,               DEFAULT_smtpHostname);
		_to                         = conf.getProperty       (PROPKEY_to,                         DEFAULT_to);
//		_cc                         = conf.getProperty       (PROPKEY_cc,                         DEFAULT_cc);
		_from                       = conf.getProperty       (PROPKEY_from,                       DEFAULT_from);
		_subjectTemplate            = conf.getProperty       (PROPKEY_subjectTemplate,            DEFAULT_subjectTemplate);
		_subjectNtrTemplate         = conf.getProperty       (PROPKEY_subjectNtrTemplate,         DEFAULT_subjectNtrTemplate);
		_sendNtr                    = conf.getBooleanProperty(PROPKEY_sendNtr,                    DEFAULT_sendNtr);
//		_msgBodyTemplate            = conf.getProperty       (PROPKEY_msgBodyTemplate,            DEFAULT_msgBodyTemplate);
//		_msgBodyTemplateUseHtml     = conf.getBooleanProperty(PROPKEY_msgBodyTemplateUseHtml,     DEFAULT_msgBodyTemplateUseHtml);
//		_msgBodyUseHtml             = conf.getBooleanProperty(PROPKEY_msgBodyUseHtml,             DEFAULT_msgBodyUseHtml);
		_attachHtmlContent          = conf.getBooleanProperty(PROPKEY_attachHtmlContent,          DEFAULT_attachHtmlContent);
		_attachHtmlZipContentGtMb   = conf.getIntProperty    (PROPKEY_attachHtmlZipContentGtMb,   DEFAULT_attachHtmlZipContentGtMb);
		_attachHtmlContentMaxSizeMb = conf.getIntProperty    (PROPKEY_attachHtmlContentMaxSizeMb, DEFAULT_attachHtmlContentMaxSizeMb);
                                                                                                  
		_username                   = conf.getProperty       (PROPKEY_username,                   DEFAULT_username);
		_password                   = conf.getProperty       (PROPKEY_password,                   DEFAULT_password);
		_smtpPort                   = conf.getIntProperty    (PROPKEY_smtpPort,                   DEFAULT_smtpPort);
		_sslPort                    = conf.getIntProperty    (PROPKEY_sslPort,                    DEFAULT_sslPort);
		_useSsl                     = conf.getBooleanProperty(PROPKEY_useSsl,                     DEFAULT_useSsl);
		_startTls                   = conf.getBooleanProperty(PROPKEY_startTls,                   DEFAULT_startTls);
		_smtpConnectTimeout         = conf.getIntProperty    (PROPKEY_connectionTimeout,          DEFAULT_connectionTimeout);

		// Fix for backward Compatibility: both properties below was named as 'smpt' instead of 'smtp' (so if not found in the 'smtp' go back and check for the *faulty* property)
		if (StringUtil.isNullOrBlank(_password)) _password = conf.getProperty   ("ReportSenderToMail.smpt.password", _password);
		if (_smtpPort == DEFAULT_smtpPort)       _smtpPort = conf.getIntProperty("ReportSenderToMail.smpt.port"    , _smtpPort);

		
		//------------------------------------------
		// Check for mandatory parameters
		//------------------------------------------
		if ( StringUtil.isNullOrBlank(_smtpHostname      ) ) throw new Exception("The property '" + PROPKEY_smtpHostname       + "' is mandatory for the ReportSender named '"+getName()+"'.");
		if ( StringUtil.isNullOrBlank(_to                ) ) throw new Exception("The property '" + PROPKEY_to                 + "' is mandatory for the ReportSender named '"+getName()+"'.");
		if ( StringUtil.isNullOrBlank(_from              ) ) throw new Exception("The property '" + PROPKEY_from               + "' is mandatory for the ReportSender named '"+getName()+"'.");
		if ( StringUtil.isNullOrBlank(_subjectTemplate   ) ) throw new Exception("The property '" + PROPKEY_subjectTemplate    + "' is mandatory for the ReportSender named '"+getName()+"'.");
		if ( StringUtil.isNullOrBlank(_subjectNtrTemplate) ) throw new Exception("The property '" + PROPKEY_subjectNtrTemplate + "' is mandatory for the ReportSender named '"+getName()+"'.");
//		if ( StringUtil.isNullOrBlank(_msgBodyTemplate   ) ) throw new Exception("The property '" + PROPKEY_msgBodyTemplate    + "' is mandatory for the ReportSender named '"+getName()+"'.");

		// Parse the 'to string' into a list
//		_toList = StringUtil.parseCommaStrToList(_to);
		
//		if (StringUtil.hasValue(_cc))
//			_ccList = StringUtil.parseCommaStrToList(_cc);

		//------------------------------------------
		// Check for valid configuration
		//------------------------------------------
	}

	@Override
	public void printConfig()
	{
		int spaces = 50;
		_logger.info("Configuration for Report Sender Module: "+getName());
		_logger.info("    " + StringUtil.left(PROPKEY_smtpHostname              , spaces) + ": " + _smtpHostname);
		_logger.info("    " + StringUtil.left(PROPKEY_to                        , spaces) + ": " + _to);
		if (JsonUtils.isPossibleJson(_to))
			_logger.info("    NOTE: the Above 'to' string look like JSON Content and will be evaluated at runtime, when the ServerName is known.");
		_logger.info("    " + StringUtil.left(PROPKEY_from                      , spaces) + ": " + _from);
		_logger.info("    " + StringUtil.left(PROPKEY_subjectTemplate           , spaces) + ": " + _subjectTemplate);
		_logger.info("    " + StringUtil.left(PROPKEY_subjectNtrTemplate        , spaces) + ": " + _subjectNtrTemplate);
		_logger.info("    " + StringUtil.left(PROPKEY_sendNtr                   , spaces) + ": " + _sendNtr);
//		_logger.info("    " + StringUtil.left(PROPKEY_msgBodyTemplate           , spaces) + ": " + _msgBodyTemplate);
//		_logger.info("    " + StringUtil.left(PROPKEY_msgBodyTemplateUseHtml    , spaces) + ": " + _msgBodyTemplateUseHtml);
//		_logger.info("    " + StringUtil.left(PROPKEY_msgBodyUseHtml            , spaces) + ": " + _msgBodyUseHtml);
		_logger.info("    " + StringUtil.left(PROPKEY_attachHtmlContent         , spaces) + ": " + _attachHtmlContent);
		_logger.info("    " + StringUtil.left(PROPKEY_attachHtmlZipContentGtMb  , spaces) + ": " + _attachHtmlZipContentGtMb);
		_logger.info("    " + StringUtil.left(PROPKEY_attachHtmlContentMaxSizeMb, spaces) + ": " + _attachHtmlContentMaxSizeMb);

		_logger.info("    " + StringUtil.left(PROPKEY_username                  , spaces) + ": " + _username);
		_logger.info("    " + StringUtil.left(PROPKEY_password                  , spaces) + ": " + (_logger.isDebugEnabled() ? _password : "*secret*") );
//		_logger.info("    " + StringUtil.left(PROPKEY_cc                        , spaces) + ": " + _cc);
		_logger.info("    " + StringUtil.left(PROPKEY_smtpPort                  , spaces) + ": " + _smtpPort);
		_logger.info("    " + StringUtil.left(PROPKEY_sslPort                   , spaces) + ": " + _sslPort);
		_logger.info("    " + StringUtil.left(PROPKEY_useSsl                    , spaces) + ": " + _useSsl);
		_logger.info("    " + StringUtil.left(PROPKEY_startTls                  , spaces) + ": " + _startTls);
		_logger.info("    " + StringUtil.left(PROPKEY_connectionTimeout         , spaces) + ": " + _smtpConnectTimeout);
	}


	public static final String  PROPKEY_smtpHostname               = "ReportSenderToMail.smtp.hostname";
	public static final String  DEFAULT_smtpHostname               = "";
                                                                   
	public static final String  PROPKEY_to                         = "ReportSenderToMail.to";
	public static final String  DEFAULT_to                         = "";
	                                                               
	public static final String  PROPKEY_from                       = "ReportSenderToMail.from";
	public static final String  DEFAULT_from                       = "";
	                                                               
	public static final String  PROPKEY_subjectTemplate            = "ReportSenderToMail.msg.subject.template";
	public static final String  DEFAULT_subjectTemplate            = "Daily Report for: ${srvName}";
	                                                               
	public static final String  PROPKEY_subjectNtrTemplate         = "ReportSenderToMail.msg.subject.nothingToReport.template";
	public static final String  DEFAULT_subjectNtrTemplate         = "Daily Report -NTR- for: ${srvName}";
	                                                               
	public static final String  PROPKEY_sendNtr                    = "ReportSenderToMail.send.nothingToReport";
	public static final boolean DEFAULT_sendNtr                    = true;
                                                                   
//	public static final String  PROPKEY_msgBodyTemplate            = "ReportSenderToMail.msg.body.template";
//	public static final String  DEFAULT_msgBodyTemplate            = createMsgBodyTemplate();
                                                                   
//	public static final String  PROPKEY_msgBodyUseHtml             = "ReportSenderToMail.msg.body.html";
//	public static final boolean DEFAULT_msgBodyUseHtml             = true;
                                                                   
	public static final String  PROPKEY_attachHtmlContent          = "ReportSenderToMail.attachHtmlContent";
	public static final boolean DEFAULT_attachHtmlContent          = true;

	public static final String  PROPKEY_attachHtmlZipContentGtMb   = "ReportSenderToMail.attachHtmlContent.zip.gt.mb";
	public static final int     DEFAULT_attachHtmlZipContentGtMb   = 10;
	
	public static final String  PROPKEY_attachHtmlContentMaxSizeMb = "ReportSenderToMail.attachHtmlContent.maxSizeMb";
	public static final int     DEFAULT_attachHtmlContentMaxSizeMb = 30;

	public static final String  PROPKEY_username                   = "ReportSenderToMail.smtp.username";
	public static final String  DEFAULT_username                   = "";
                                                                   
	public static final String  PROPKEY_password                   = "ReportSenderToMail.smtp.password";
	public static final String  DEFAULT_password                   = "";
                                                                   
//	public static final String  PROPKEY_cc                         = "ReportSenderToMail.cc";
//	public static final String  DEFAULT_cc                         = "";
                                                                   
	public static final String  PROPKEY_smtpPort                   = "ReportSenderToMail.smtp.port";
	public static final int     DEFAULT_smtpPort                   = -1;
                                                                   
	public static final String  PROPKEY_sslPort                    = "ReportSenderToMail.ssl.port";
	public static final int     DEFAULT_sslPort                    = -1;
                                                                   
	public static final String  PROPKEY_useSsl                     = "ReportSenderToMail.ssl.use";
	public static final boolean DEFAULT_useSsl                     = false;
                                                                   
	public static final String  PROPKEY_startTls                   = "ReportSenderToMail.start.tls";
	public static final boolean DEFAULT_startTls                   = false;
                                                               
	public static final String  PROPKEY_connectionTimeout          = "ReportSenderToMail.smtp.connect.timeout";
	public static final int     DEFAULT_connectionTimeout          = -1;
}
