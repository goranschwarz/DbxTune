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

import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ConfigRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.OpenSSHConfig;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

public class JschTest2
{
	private static Logger _logger = Logger.getLogger(JschTest2.class);


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
//			String cmd      = conf.getProperty("cmd", "hostname; id; date");
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

//			if (debugLvl > 0)
//			{
//				if (debugLvl >= 3)
//				{
//					System.out.println(" - LogLvl 3: Setting JUL=ALL, Log4J=TRACE");
//					java.util.logging.Level logLevel = java.util.logging.Level.ALL; 
//
//					app.setLevel(logLevel);
//
//					// Set Log4J Properties
//					log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
//				}
//				else if (debugLvl >= 2)
//				{
//					System.out.println(" - LogLvl 2: Setting JUL=FINER, Log4J=DEBUG");
//					java.util.logging.Level logLevel = java.util.logging.Level.FINER; 
//					app.setLevel(logLevel);
//
//					// Set Log4J Properties
//					log4jProps.setProperty("log4j.rootLogger", "DEBUG, A1");
//				}
//				else if (debugLvl >= 1)
//				{
//					System.out.println(" - LogLvl 2: Setting JUL=FINE, Log4J=DEBUG");
//					java.util.logging.Level logLevel = java.util.logging.Level.FINE; 
//					app.setLevel(logLevel);
//
//					// Set Log4J Properties
//					log4jProps.setProperty("log4j.rootLogger", "DEBUG, A1");
//				}
//			}
			PropertyConfigurator.configure(log4jProps);
			

			JSch.setLogger(new JschLog4jBridge());
			JSch jsch = new JSch();

			try
			{
				Session session=jsch.getSession(username, hostname, 22);
				session.setPassword(password);

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
				
				//session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
				
				
				
				String sshKeyFile_rsa = System.getProperty("user.home") + "/.ssh/id_rsa";
				String sshKeyFile_dsa = System.getProperty("user.home") + "/.ssh/id_dsa";
				
				if ( new File(sshKeyFile_rsa).exists() )
				{
					jsch.addIdentity(sshKeyFile_rsa, password);
					jsch.addIdentity(sshKeyFile_rsa);
				}

				if (StringUtil.hasValue(keyfile))
				{
					if ( new File(keyfile).exists() )
					{
						jsch.addIdentity(keyfile);
					}
					else
					{
						System.out.println("The specified SHH KeyFile '" + keyfile + "' did NOT exists.");
					}
				}

				
				String sshConfigFile = System.getProperty("user.home") + "/.ssh/config";
				if ( new File(sshConfigFile).exists() )
				{
//					ConfigRepository configRepository = OpenSSHConfig.parse(configStr);
					ConfigRepository configRepository = OpenSSHConfig.parseFile(sshConfigFile);
					jsch.setConfigRepository(configRepository);
				}


				UserInfo ui = new MyUserInfo();
				session.setUserInfo(ui);

				
				System.out.println("");
				System.out.println(">>> Connecting...");
				session.connect();

				
				
				System.out.println("");				
				System.out.println(">>> Executing command: " + cmd);

				boolean useNative = true;
				if (useNative)
				{
					Channel channel=session.openChannel("exec");
					((ChannelExec)channel).setCommand(cmd);

					channel.setInputStream(null);
//					((ChannelExec)channel).setErrStream(System.err);
					
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
								System.out.print(">>> STDOUT-COMMAND: " + new String(tmp, 0, i));
							}

							while (err.available() > 0)
							{
								int i = err.read(tmp, 0, 1024);
								if ( i < 0 )
									break;
								System.err.print(">>> STDERR-COMMAND: " + new String(tmp, 0, i));
							}
							
						}
						if ( channel.isClosed() )
						{
							if ( in.available() > 0 )
								continue;
							System.out.println(">>> EXIT-STATUS: " + channel.getExitStatus());
							break;
						}
						try 
						{
							//System.out.println("------ Sleeping 500 ms for input...");
							Thread.sleep(500); 
						} catch (Exception ignore) {}
					}
					channel.disconnect();
				}
				else
				{
					throw new Exception("Not yet implemented. Ony 'useNative' is implemented.");
				}

				// 
				session.disconnect();
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

			
			
			
			
			
			
//			SshConnection conn = new SshConnection(hostname, port, username, password, keyfile);
//
//			try
//			{
//				System.out.println("");
//				System.out.println(">>> Connecting...");
//				boolean success = conn.connect();
//				System.out.println("    - " + (success ? "Success" : "FAILED"));
//				System.out.println("");
//				
//				System.out.println(">>> Executing command...");
//				String result = conn.execCommandOutputAsStr(cmd);
//				System.out.println("--- Command Output ------------------------------------------------");
//				System.out.println(result);
//				System.out.println("-------------------------------------------------------------------");
//				System.out.println("");
//				
//				System.out.println(">>> Closing connection...");
//				conn.close();
//
//				System.out.println("DONE");
//				System.out.println("");
//				
//			}
//			catch (Exception ex)
//			{
//				System.out.println("");
//				System.out.println("--- ERROR --------------------------------------------------");
//				ex.printStackTrace();
//				System.out.println("------------------------------------------------------------");
//				System.out.println("");
//
//				System.exit(1);
//			}
		}
	}

	/**
	 * Allows user interaction. The application can provide an implementation of this interface to the Session to allow 
	 * for feedback to the user and retrieving information (e.g. passwords, passphrases or a confirmation) from the user.
	 * <p>
	 * If an object of this interface also implements UIKeyboardInteractive, it can also be used for 
	 * keyboard-interactive authentication as described in RFC 4256.
	 * <p>
	 * Most of the examples include an implementation of this interface based on Swings JOptionPane.
	 * 
	 */
	private static class MyUserInfo implements UserInfo, UIKeyboardInteractive
	{

		/**
		 * Prompts the user for a password used for authentication for the remote server.
		 * 
		 * @param message - the prompt string to be shown to the user.    
		 * 
		 * @return true if the user entered a password. This password then can be retrieved by getPassword().
		 */
		@Override
		public boolean promptPassword(String message)
		{
			System.out.println("---------- MyUserInfo: promptPassword() message='"+message+"'.");
			return false;
		}

		/**
		 * Returns the password entered by the user. This should be only called after a successful promptPassword(java.lang.String).
		 */
		@Override
		public String getPassword()
		{
			System.out.println("---------- MyUserInfo: getPassword()");
			return null;
		}


		
		/**
		 * Prompts the user for a passphrase for a public key.
		 * 
		 * @param message - the prompt message to be shown to the user.
		 * 
		 * @return true if the user entered a passphrase. The passphrase then can be retrieved by getPassphrase().
		 */
		@Override
		public boolean promptPassphrase(String message)
		{
			System.out.println("---------- MyUserInfo: promptPassphrase() message='"+message+"'.");
			return false;
		}

		/**
		 * Returns the passphrase entered by the user. This should be only called after a successful promptPassphrase(java.lang.String).
		 */
		@Override
		public String getPassphrase()
		{
			System.out.println("---------- MyUserInfo: getPassphrase()");
			return null;
		}

	
		/**
		 * Prompts the user to answer a yes-no-question.
		 * <p>
		 * Note: These are currently used to decide whether to create nonexisting files or directories, whether to replace an existing host key, and whether to connect despite a non-matching key.
		 * 
		 * @param message - the prompt message to be shown to the user.
		 * 
		 * @return true if the user answered with "Yes", else false.
		 */
		@Override
		public boolean promptYesNo(String message)
		{
			System.out.println("---------- MyUserInfo: promptYesNo() message='"+message+"'.");
			return false;
		}

		/**
		 * Shows an informational message to the user.
		 * 
		 * @param message - the message to show to the user.
		 */
		@Override
		public void showMessage(String message)
		{
			System.out.println("---------- MyUserInfo: showMessage() message='"+message+"'.");
		}

		/**
		 * Retrieves answers from the user to a number of questions.
		 * 
		 * @param destination - identifies the user/host pair where we want to login. (This was not sent by the remote side).
		 * @param name - the name of the request (could be shown in the window title). This may be empty.
		 * @param instruction - an instruction string to be shown to the user. This may be empty, and may contain new-lines.
		 * @param prompt - a list of prompt strings.
		 * @param echo - for each prompt string, whether to show the texts typed in (true) or to mask them (false). This array will have the same length as prompt.
		 * 
		 * @return the answers as given by the user. This must be an array of same length as prompt, if the user confirmed. If the user cancels the input, the return value should be null.
		 * 
		 */
		@Override
		public String[] promptKeyboardInteractive(String destination, String name, String instruction, String[] prompt, boolean[] echo)
		{
			System.out.println("MyUserInfo: promptKeyboardInteractive() destination='" + destination + "', name='" + name + "', instruction='" + instruction + "', prompt=[" + StringUtil.toCommaStr(prompt) + "], echo=[" + StringUtil.toCommaStr(echo) + "].");
			return null;
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
//			case com.jcraft.jsch.Logger.DEBUG: _logger.debug(message); break;
//			case com.jcraft.jsch.Logger.INFO:  _logger.info(message);  break;
//			case com.jcraft.jsch.Logger.WARN:  _logger.warn(message);  break;
//			case com.jcraft.jsch.Logger.ERROR: _logger.error(message); break;
//			case com.jcraft.jsch.Logger.FATAL: _logger.error(message); break;
//			default:                           _logger.trace(message); break;
			case com.jcraft.jsch.Logger.DEBUG: System.out.println("--- DEBUG:   - " + message); break;
			case com.jcraft.jsch.Logger.INFO:  System.out.println("--- INFO:    - " + message); break;
			case com.jcraft.jsch.Logger.WARN:  System.out.println("--- WARNING: - " + message); break;
			case com.jcraft.jsch.Logger.ERROR: System.out.println("--- ERROR:   - " + message); break;
			case com.jcraft.jsch.Logger.FATAL: System.out.println("--- FATAL:   - " + message); break;
			default:                           System.out.println("--- TRACE?:  - " + message); break;
			}
		}
	}
}
