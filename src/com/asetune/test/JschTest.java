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

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

public class JschTest
{
	private static Logger _logger = Logger.getLogger(JschTest.class);


	public static void main(String[] args)
	{
		// Set Log4J Properties
		Properties log4jProps = new Properties();
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		try
		{
			JSch.setLogger(new JschLog4jBridge());
			JSch jsch = new JSch();

			String user = "gorans";
			String host = "gorans.org";

			
			Session session=jsch.getSession(user, host, 22);
			session.setPassword("1niss2e_xxx");

			String sskKnownHostsFile = System.getProperty("user.home") + "/.ssh/known_hosts";
			if ( new File(sskKnownHostsFile).exists() )
			{
				System.out.println("sskKnownHostsFile='" + sskKnownHostsFile + "'.");
		        jsch.setKnownHosts(sskKnownHostsFile);
			}
			else
			{
				System.out.println("settingConfig: StrictHostKeyChecking=no");
				session.setConfig("StrictHostKeyChecking", "no");
			}
//TODO; if in GUI, ask question on the below
// 2023-01-22 00:45:08,743 - INFO  - JschTest                       - expecting SSH_MSG_KEX_ECDH_REPLY
// 2023-01-22 00:45:08,756 - INFO  - JschTest                       - ssh_ecdsa_verify: ecdsa-sha2-nistp256 signature true
// MyUserInfo: promptYesNo() message='The authenticity of host 'gorans.org' can't be established.
// ECDSA key fingerprint is SHA256:+bBtLT+Ln4ef+w2y8ZwDIUFGYnYqPDKDO7bceMuwmjw.
// Are you sure you want to continue connecting?'.
// 2023-01-22 00:45:08,757 - INFO  - JschTest                       - Disconnecting from gorans.org port 22
// com.jcraft.jsch.JSchException: reject HostKey: gorans.org
//     at com.jcraft.jsch.Session.checkHost(Session.java:932)
//     at com.jcraft.jsch.Session.connect(Session.java:359)
//     at com.jcraft.jsch.Session.connect(Session.java:194)
//     at com.asetune.test.JschTest.main(JschTest.java:88)

			
			String sshKeyFile_rsa = System.getProperty("user.home") + "/.ssh/id_rsa";
			String sshKeyFile_dsa = System.getProperty("user.home") + "/.ssh/id_dsa";
			
			if ( new File(sshKeyFile_rsa).exists() )
			{
				jsch.addIdentity(sshKeyFile_rsa, "passwordToTheIdFile");
				jsch.addIdentity(sshKeyFile_rsa);
			}
			
			UserInfo ui = new MyUserInfo();
			session.setUserInfo(ui);
			session.connect();

//			String cmd = "uname -a";
//			String cmd = "iostat -xdck 3";
//			String cmd = "/home/sybase/xxx.sh";
			String cmd = "/home/sybase/xxx.sh 3";
			
			Channel channel=session.openChannel("exec");
			((ChannelExec)channel).setCommand(cmd);

			channel.setInputStream(null);
//			((ChannelExec)channel).setErrStream(System.err);
			
			InputStream in=channel.getInputStream();
			InputStream err=channel.getExtInputStream();
			
			channel.connect();
			
			byte[] tmp = new byte[1024];
			while (true)
			{
				if (in.available() > 0 || err.available() > 0)
				{
					while (in.available() > 0)
					{
						int i = in.read(tmp, 0, 1024);
						if ( i < 0 )
							break;
						System.out.print("STDOUT-COMMAND: " + new String(tmp, 0, i));
					}

					while (err.available() > 0)
					{
						int i = err.read(tmp, 0, 1024);
						if ( i < 0 )
							break;
						System.err.print("STDERR-COMMAND: " + new String(tmp, 0, i));
					}
					
				}
				if ( channel.isClosed() )
				{
					if ( in.available() > 0 )
						continue;
					System.out.println("EXIT-STATUS: " + channel.getExitStatus());
					break;
				}
				try 
				{
					System.out.println("------ Sleeping 500 ms for input...");
					Thread.sleep(500); 
				} catch (Exception ignore) {}
			}
			channel.disconnect();
			session.disconnect();
			
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	private static class MyUserInfo implements UserInfo
	{

		@Override
		public String getPassphrase()
		{
			System.out.println("MyUserInfo: getPassphrase()");
			return null;
		}

		@Override
		public String getPassword()
		{
			System.out.println("MyUserInfo: getPassword()");
			return null;
		}

		@Override
		public boolean promptPassword(String message)
		{
			System.out.println("MyUserInfo: promptPassword() message='"+message+"'.");
			return false;
		}

		@Override
		public boolean promptPassphrase(String message)
		{
			System.out.println("MyUserInfo: promptPassphrase() message='"+message+"'.");
			return false;
		}

		@Override
		public boolean promptYesNo(String message)
		{
			System.out.println("MyUserInfo: promptYesNo() message='"+message+"'.");
			return false;
		}

		@Override
		public void showMessage(String message)
		{
			System.out.println("MyUserInfo: showMessage() message='"+message+"'.");
		}
	}

	/**
	 * Bridges all JSch logging to the SLF4J API.
	 */
	private static class JschLog4jBridge implements com.jcraft.jsch.Logger
	{
//		private static Logger logger = LoggerFactory.getLogger(Slf4jBridge.class);

		@Override
		public boolean isEnabled(int level)
		{
			return true;
//			switch (level)
//			{
//			case com.jcraft.jsch.Logger.DEBUG:  return _logger.isDebugEnabled();
//			case com.jcraft.jsch.Logger.INFO:   return _logger.isInfoEnabled();
//			case com.jcraft.jsch.Logger.WARN:   return true; //logger.isWarnEnabled();
//			case com.jcraft.jsch.Logger.ERROR:  return true; // logger.isErrorEnabled();
//			case com.jcraft.jsch.Logger.FATAL:  return true;
//			default:                            return _logger.isTraceEnabled();
//			}
		}

		@Override
		public void log(int level, String message)
		{
			switch (level)
			{
			case com.jcraft.jsch.Logger.DEBUG: _logger.debug(message); break;
			case com.jcraft.jsch.Logger.INFO:  _logger.info(message);  break;
			case com.jcraft.jsch.Logger.WARN:  _logger.warn(message);  break;
			case com.jcraft.jsch.Logger.ERROR: _logger.error(message); break;
			case com.jcraft.jsch.Logger.FATAL: _logger.error(message); break;
			default:                           _logger.trace(message); break;
			}
		}

	}
}
