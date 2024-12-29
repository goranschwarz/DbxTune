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
package com.dbxtune.test;

import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventDummy;
import com.dbxtune.alarm.events.AlarmEvent.ServiceState;
import com.dbxtune.alarm.events.AlarmEvent.Severity;
import com.dbxtune.alarm.writers.AlarmWriterToMail;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

public class MailTest2
{
	//---------------------------------------------------------------------------------------
	// Example: C:\projects\DbxTune\bin>dbxtune.bat mailtest2 C:\tmp\mailtest2.props
	//---------------------------------------------------------------------------------------
	public static void main(String[] args)
	{
		if (args.length != 1)
		{
			System.out.println("");
			System.out.println("Usage: mailtest2 properties_file");
			System.out.println("");
			System.out.println("Example of a Properties file:");
			int spaces = 45;
			System.out.println( StringUtil.left(AlarmWriterToMail.PROPKEY_smtpHostname          , spaces) + "= hostname.com");
			System.out.println( StringUtil.left(AlarmWriterToMail.PROPKEY_to                    , spaces) + "= username@acme.com");
			System.out.println( StringUtil.left(AlarmWriterToMail.PROPKEY_from                  , spaces) + "= username@acme.com");
			System.out.println( "#" + StringUtil.left(AlarmWriterToMail.PROPKEY_subjectTemplate       , spaces) + "= text       #default: " + AlarmWriterToMail.DEFAULT_subjectTemplate);
//			System.out.println( "#" + StringUtil.left(AlarmWriterToMail.PROPKEY_msgBodyTemplate       , spaces) + "= text       #default: " + AlarmWriterToMail.DEFAULT_msgBodyTemplate);
			System.out.println( "#" + StringUtil.left(AlarmWriterToMail.PROPKEY_msgBodyTemplate       , spaces) + "= text       #default: ...The default is to long to write...");
			System.out.println( "#" + StringUtil.left(AlarmWriterToMail.PROPKEY_msgBodyTemplateUseHtml, spaces) + "= true|false #default: " + AlarmWriterToMail.DEFAULT_msgBodyTemplateUseHtml);

			System.out.println( "#" + StringUtil.left(AlarmWriterToMail.PROPKEY_username              , spaces) + "= username");
			System.out.println( "#" + StringUtil.left(AlarmWriterToMail.PROPKEY_password              , spaces) + "= *secret*");
			System.out.println( "#" + StringUtil.left(AlarmWriterToMail.PROPKEY_smtpPort              , spaces) + "= port        #default: " + AlarmWriterToMail.DEFAULT_smtpPort);
			System.out.println( "#" + StringUtil.left(AlarmWriterToMail.PROPKEY_sslPort               , spaces) + "= port        #default: " + AlarmWriterToMail.DEFAULT_sslPort);
			System.out.println( "#" + StringUtil.left(AlarmWriterToMail.PROPKEY_useSsl                , spaces) + "= true|false  #default: " + AlarmWriterToMail.DEFAULT_useSsl);
			System.out.println( "#" + StringUtil.left(AlarmWriterToMail.PROPKEY_startTls              , spaces) + "= true|false  #default: " + AlarmWriterToMail.DEFAULT_startTls);
			System.out.println( "#" + StringUtil.left(AlarmWriterToMail.PROPKEY_connectionTimeout     , spaces) + "= ##          #default: " + AlarmWriterToMail.DEFAULT_connectionTimeout);
			System.out.println("");
		}
		else
		{
			String filename = args[0];

			Configuration conf = new Configuration(filename);

			// Set Log4J Properties
			Properties log4jProps = new Properties();
			log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
			log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
			log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
			log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");

			if ( (System.getenv("MAILTEST2_DEBUG")+"").trim().equalsIgnoreCase("true") )
				log4jProps.setProperty("log4j.rootLogger", "DEBUG, A1");
			
			PropertyConfigurator.configure(log4jProps);
			
			// Set JUL (Java Util Logging) format
//			System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT,%1$tL - %4$-7s - [%2$s] - %5$s %n");

			System.out.println("");
			System.out.println(" * Creating 'AlarmWriterToMail' object");
			AlarmWriterToMail aw = new AlarmWriterToMail();
			
			try
			{
				System.out.println("");
				System.out.println(" * Initializing 'AlarmWriterToMail' object");
				aw.init(conf);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}

			System.out.println("");
			System.out.println(" * Printing Configuration for: 'AlarmWriterToMail' object");
			aw.printConfig();			
			
			AlarmEvent ae = new AlarmEventDummy("SomeDBMSInstance", "SomeCmName", "SomeExtraInfo", AlarmEvent.Category.OTHER, Severity.WARNING, ServiceState.AFFECTED, -1, 999, "This is an Alarm Example with the data value of '999'", "Extended Description goes here", 0);
			System.out.println("");
			System.out.println(" * Creating dummy Alarm: " + ae);

			System.out.println("");
			System.out.println(" * Raise Alarm (which should send mail)");
			aw.raise(ae);

			System.out.println("");
			System.out.println("-- END -- Exiting");
		}
	}
}
