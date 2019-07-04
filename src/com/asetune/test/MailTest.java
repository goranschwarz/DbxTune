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
package com.asetune.test;

import java.sql.Timestamp;

import org.apache.commons.mail.EmailConstants;
import org.apache.commons.mail.HtmlEmail;

import com.asetune.utils.StringUtil;

public class MailTest
{

	public static void main(String[] args)
	{
		// java -cp ./lib/asetune.jar:./lib/commons-email-1.4.jar:./lib/javax.mail.jar:./lib/log4j-1.2.17.jar com.asetune.test.MailTest xxx@yyy.com

		System.out.println("Usage: to [smtpHost] [smtpPort] [from]");

		if (args.length == 0)
		{
			System.out.println(" - To few arguments - You need atleast to pass 'to' (first param)");
			System.exit(1);
		}
		
		String to       = args[0];

		String smtpHost = "localhost";
		String smtpPort = "25";
		String from     = System.getProperty("user.name") + "@" + StringUtil.getHostnameWithDomain();

		if (args.length >= 2)  smtpHost = args[1];
		if (args.length >= 3)  smtpPort = args[2];
		if (args.length >= 4)  from     = args[3];
		
		sendTestMail(smtpHost, StringUtil.parseInt(smtpPort, 25), from, to, "");
	}

	private static void sendTestMail(String smtpHostname, int smtpPort, String from, String to, String cc)
	{
		Timestamp ts = new Timestamp(System.currentTimeMillis());
		
		String msgSubject  = "Dummy test mail at: " + ts;
		String msgBodyText = "Dummy test mail at: " + ts;
		String msgBodyHtml = "Dummy test mail at: " + ts;
		
		int msgBodyTextSizeKb = msgBodyText == null ? 0 : msgBodyText.length() / 1024;
		int msgBodyHtmlSizeKb = msgBodyHtml == null ? 0 : msgBodyHtml.length() / 1024;

//		String msgBody = msgBodyText;
//
//		boolean msgBodyUseHtml = true;
//			msgBody = msgBodyHtml;
			
		try
		{
			HtmlEmail email = new HtmlEmail();

			email.setHostName(smtpHostname);

			// Charset
			email.setCharset(EmailConstants.UTF_8);
			
			// Connection timeout
//			if (_smtpConnectTimeout >= 0)
//				email.setSocketConnectionTimeout(_smtpConnectTimeout);

			// SMTP PORT
			if (smtpPort >= 0)
				email.setSmtpPort(smtpPort);

			// USE SSL
//			if (useSsl)
//				email.setSSLOnConnect(useSsl);

			// SSL PORT
//			if (_useSsl && _sslPort >= 0)
//				email.setSslSmtpPort(_sslPort+""); // Hmm why is this a String parameter?

			// AUTHENTICATION
//			if (StringUtil.hasValue(_username))
//				email.setAuthentication(_username, _password);
			
////			 add TO and CC
//			for (String to : _toList)
//				email.addTo(to);
//
//			for (String cc : _ccList)
//				email.addCc(cc);

			email.addTo(to);
			
			if (StringUtil.hasValue(cc))
				email.addCc(cc);

			// FROM & SUBJECT
			email.setFrom(from);
			email.setSubject(msgSubject);

			// CONTENT HTML or PLAIN
//			if (StringUtils.startsWithIgnoreCase(msgBody.trim(), "<html>"))
//				email.setHtmlMsg(msgBody);
//			else
//				email.setTextMsg(msgBody);
			email.setHtmlMsg(msgBodyHtml);
//			email.setTextMsg(msgBodyText);
			
//			System.out.println("About to send the following message: \n"+msgBody);
//			if (_logger.isDebugEnabled())
//			{
//				_logger.debug("About to send the following message: \n"+msgBody);
//			}

			// SEND
			email.send();

			System.out.println("Sent mail message: plainSizeKb="+msgBodyTextSizeKb+", htmlSizeKb="+msgBodyHtmlSizeKb+", host='"+smtpHostname+"', to='"+to+"', cc='"+cc+"', subject='"+msgSubject+"'.");
		}
		catch (Exception ex)
		{
			System.out.println("Problems sending mail (plainSizeKb="+msgBodyTextSizeKb+", htmlSizeKb="+msgBodyHtmlSizeKb+", host='"+smtpHostname+"', to='"+to+"', cc='"+cc+"', subject='"+msgSubject+"').");
			ex.printStackTrace();
		}
		
	}
}
