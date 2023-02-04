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
package com.asetune.ssh;

import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public class SshConnectionTester
{
	public static void main(String[] args)
	{
		System.out.println("");
		System.out.println(">>> SSH Connection test -- below is what will be done.");
		System.out.println("  - Create a SSH connection, to 'hostname'");
		System.out.println("  - Execute 'cmd'");
		System.out.println("  - Close the connection");
		System.out.println("");
		if (args.length != 1)
		{
			System.out.println("");
			System.out.println("Usage: conntest properties_file");
			System.out.println("");
			System.out.println("Example of a Properties file: (NOTE: remove the comments)");
			System.out.println("hostname = value            ## Mandatory");
			System.out.println("port     = value            ## Default: 22");
			System.out.println("username = value            ## Default: the-one-you-are-using");
			System.out.println("password = value            ## Default: null, which would be blank ''");
			System.out.println("keyfile  = value            ## Default: null, not used");
			System.out.println("cmd      = value            ## Default: id, just to prompt what user we logged in as");
			System.out.println("debugLvl = value            ## Default: 0  (between 0 and 3)");
			System.out.println("");
		}
		else
		{
			String filename = args[0];

			Configuration conf = new Configuration(filename);
			
			String hostname = conf.getProperty("hostname", "");
			int    port     = conf.getIntProperty("port", 22);
			String username = conf.getProperty("username", System.getProperty("user.name"));
			String password = conf.getProperty("password");
			String keyfile  = conf.getProperty("keyfile");
			String cmd      = conf.getProperty("cmd", "id");
			int    debugLvl = conf.getIntProperty("debugLvl", 0);

			if (StringUtil.isNullOrBlank(hostname))
			{
				System.out.println("");
				System.out.println(">>> ERROR: 'hostname' is mandatory field in the properties file.");
				System.exit(1);
			}

			System.out.println("");
			System.out.println("Below variables was in the properties file.");
			System.out.println("   hostname = |" + hostname + "|");
			System.out.println("   port     = |" + port     + "|");
			System.out.println("   username = |" + username + "|");
			System.out.println("   password = |" + password + "|");
			System.out.println("   keyfile  = |" + keyfile  + "|");
			System.out.println("   cmd      = |" + cmd      + "|");
			System.out.println("   debugLvl = |" + debugLvl + "|");

			
			// Enable more logging.

			// Set Log4J Properties
			Properties log4jProps = new Properties();
			log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
			log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
			log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
			log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");

			// Set JUL (Java Util Logging) format
//			System.setProperty("java.util.logging.SimpleFormatter.format", "%1$s [%4$s] [%2$s] %5$s%6$s%n");
//			System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT - %4$-7s - [%2$s] - %5$s %n");
			System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT,%1$tL - %4$-7s - [%2$s] - %5$s %n");

			if (debugLvl > 0)
			{
//				ch.ethz.ssh2.log.Logger.enabled = true;

				// JUL needs full NameSpace, since we also use Log4J in here
//				final java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
				final java.util.logging.Logger app = java.util.logging.Logger.getLogger("ch.ethz");

//				java.util.logging.LogManager.getLogManager().reset();
//				java.util.logging.ConsoleHandler ch = new java.util.logging.ConsoleHandler();
//				ch.setLevel(java.util.logging.Level.ALL);
//				app.addHandler(ch);
				
				if (debugLvl >= 3)
				{
					System.out.println(" - LogLvl 3: Setting JUL=ALL, Log4J=TRACE");
					java.util.logging.Level logLevel = java.util.logging.Level.ALL; 
//					java.util.logging.Level logLevel = java.util.logging.Level.FINEST; 
					app.setLevel(logLevel);
//					System.setProperty("java.util.logging.ConsoleHandler.level", "ALL");
//					System.setProperty("java.util.logging.ConsoleHandler.level", "FINEST");

					// Set Log4J Properties
					log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
				}
				else if (debugLvl >= 2)
				{
					System.out.println(" - LogLvl 2: Setting JUL=FINER, Log4J=DEBUG");
					java.util.logging.Level logLevel = java.util.logging.Level.FINER; 
					app.setLevel(logLevel);

					// Set Log4J Properties
					log4jProps.setProperty("log4j.rootLogger", "DEBUG, A1");
				}
				else if (debugLvl >= 1)
				{
					System.out.println(" - LogLvl 2: Setting JUL=FINE, Log4J=DEBUG");
					java.util.logging.Level logLevel = java.util.logging.Level.FINE; 
					app.setLevel(logLevel);

					// Set Log4J Properties
					log4jProps.setProperty("log4j.rootLogger", "DEBUG, A1");
				}
			}
			PropertyConfigurator.configure(log4jProps);
			
			
			
//			SshConnection conn = new SshConnection(hostname, port, username, password, keyfile);
			SshConnection2 conn = new SshConnection2(hostname, port, username, password, keyfile);

			try
			{
				System.out.println("");
				System.out.println(">>> Connecting...");
				boolean success = conn.connect();
				System.out.println("    - " + (success ? "Success" : "FAILED"));
				System.out.println("");
				
				System.out.println(">>> Executing command...");
				String result = conn.execCommandOutputAsStr(cmd);
				System.out.println("--- Command Output ------------------------------------------------");
				System.out.println(result);
				System.out.println("-------------------------------------------------------------------");
				System.out.println("");
				
				System.out.println(">>> Closing connection...");
				conn.close();

				System.out.println("DONE");
				System.out.println("");
				
			}
			catch (Exception ex)
			{
				System.out.println("");
				System.out.println("--- ERROR --------------------------------------------------");
				ex.printStackTrace();
				System.out.println("------------------------------------------------------------");
				System.out.println("");

				System.exit(1);
			}
		}
	}
}
