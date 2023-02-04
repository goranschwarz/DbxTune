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
package com.asetune.hostmon;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.asetune.Version;
import com.asetune.ssh.SshConnection2.LinuxUtilType;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.VersionShort;

public class HostMonitorConnectionLocalOsCmdWrapper 
extends HostMonitorConnectionLocalOsCmd
{
	private static Logger _logger = Logger.getLogger(HostMonitorConnectionLocalOsCmdWrapper.class);

	/** This can be used to execute a "local" ssh command, which connects to the remote system and returns results */
	private String              _wrapperCmd = null;
//	private Map<String, String> _envMap     = null;

	
	/** hostname */
	private String _hostname = null;

	/** output from 'uname -a', which was made while the connection was created. */
	private String _uname = null;

	/** output from the 'nproc' command. Which tells us how many scheduling/processing units are available on this os */
	private int _nproc = -1;
	
	private String _osCharset = null;
	
//	public HostMonitorConnectionLocalOs()
//	{
//		this(false);
//	}
	public HostMonitorConnectionLocalOsCmdWrapper(boolean isConnected, String wrapperCmd, Map<String, String> envMap)
	{
		super(isConnected, envMap);

		_wrapperCmd  = wrapperCmd;
		
		if (StringUtil.isNullOrBlank(_wrapperCmd))
			throw new RuntimeException("HostMonitorConnectionLocalOsCmdWrapper(), wrapperCmd='" + _wrapperCmd + "' is not allowed. This is a mandatory parameter.");
	}

	@Override
	public String getHostname()
	{
		// Return CACHED value
		if (_hostname != null)
			return _hostname;

		try
		{
			String output = execCommandOutputAsStr("hostname");
			if (StringUtil.hasValue(output))
				_hostname = output;
			return output;
		}
		catch (Exception ex)
		{
			_logger.error("Problems getting Hostname, using 'hostname', returing '-unknown-' instead.", ex);
			return "-unknown-";
		}
		
		
//		try { return InetAddress.getLocalHost().getHostName(); } // Or possibly call OS 'hostname'
//		catch (Exception ignore) { return "-unknown-"; }
	}

	@Override
	public int getOsCoreCount()
	{
		// Return CACHED value
		if (_nproc != -1)
			return _nproc;

		String cmd = "nproc";
		
		if (_uname != null)
		{
			if (_uname.equals("Windows-CMD")) // DOS Prompt
				cmd = "echo %NUMBER_OF_PROCESSORS%";

			if (_uname.startsWith("Windows-Powershell-")) // Powershell (any kind)
				cmd = "echo $env:NUMBER_OF_PROCESSORS";
		}
		
		try
		{
			String output = execCommandOutputAsStr(cmd);
			_nproc = StringUtil.parseInt(output, 0);
		}
		catch (Exception ex)
		{
			_logger.error("Problems getting getOsCoreCount, using '" + cmd + "'.", ex);
		}

		return _nproc;
		
//		return Runtime.getRuntime().availableProcessors();
	}

	@Override
	public String getOsCharset()
	{
		// Return CACHED value
		if (_osCharset != null)
			return _osCharset;

//		return "UTF-8";
//	//	return System.getProperty("file.encoding", "UTF-8");
//		OutputStreamWriter writer = new OutputStreamWriter(new ByteArrayOutputStream());
//		String enc = writer.getEncoding();
//		return enc;

		String osName = getOsName();
		if (StringUtil.hasValue(osName))
		{
			// also try to figure out a dummy default character set for the OS
//			if      (osName.equals    ("Linux"   )) _osCharset = "UTF-8";
			if      (osName.equals    ("Linux"   )) _osCharset = linuxToJavaCharset();
			else if (osName.equals    ("SunOS"   )) _osCharset = "ISO-8859-1";
			else if (osName.equals    ("AIX"     )) _osCharset = "ISO-8859-1"; // TODO: CHECK
			else if (osName.equals    ("HP-UX"   )) _osCharset = "ISO-8859-1"; // TODO: CHECK
			else if (osName.startsWith("Windows-")) _osCharset = windowsToJavaCharset();
			else
			{
				_osCharset = "UTF-8";
				_logger.info("Unhandled osName='" + osName + "' setting charset to '" + _osCharset + "'");
			}

			//Charset.forName("ISO-8859-1");
		}
		_logger.debug("getOsInfo: osName='" + osName + "', chartset='" + _osCharset + "'.");
		//System.out.println("getOsInfo: osName='" + osName + "', chartset='" + _osCharset + "'.");

		return _osCharset;
	}
	
	/**
	 * INTERNAL: called from: getOsCharset()
	 */
	private String linuxToJavaCharset()
	{
		String output = null;
		String cmd    = "echo ${LC_ALL:-${LC_CTYPE:-${LANG}}}";
		try
		{
			output = execCommandOutputAsStr(cmd);
		}
		catch (Exception ex)
		{
			_logger.error("Problems executing '" + cmd + "', continuing anyway... caught: " + ex);
		}

		if (StringUtil.isNullOrBlank(output))
		{
			output = "UTF-8";
			_logger.info("Could not retrive Linux charset, setting it to '" + output + "'");
		}
		else
		{
			// Active code page: ###
			String[] sa = output.split("\\.");
			if (sa.length == 2)
			{
				output = sa[1]; // echo ${LC_ALL:-${LC_CTYPE:-${LANG}}} -->>> 'en_US.UTF-8'
			}
			_logger.info("Detected Linux charset='" + output + "'.");
		}

		if (StringUtil.isNullOrBlank(output))
		{
			output = "UTF-8";
			_logger.info("After charset lookup translation, the charset is still not known. setting it to '" + output + "' as a last resort.");
		}

		return output;
	}

	/**
	 * INTERNAL: called from: getOsCharset()
	 */
	private String windowsToJavaCharset()
	{
		// Check what OS we ended up in
		String output = "";
		String cmd    = "chcp";
		try
		{
			output = execCommandOutputAsStr(cmd);
		}
		catch (Exception ex)
		{
			_logger.error("Problems executing '" + cmd + "', continuing anyway... caught: " + ex);
		}

		if (StringUtil.isNullOrBlank(output))
		{
			output = "IBM850";
			_logger.info("Could not retrive Windows codepage, setting it to '" + output + "'");
		}
		else
		{
			// Active code page: ###
			String[] sa = output.split(" ");
			for (int i=0; i<sa.length; i++)
			{
				int num = StringUtil.parseInt(sa[i], -99);
				if (num != -99)
				{
					if      (num > 10  && num <100)  output = "IBM0" + num;
					else if (num > 100 && num <1000) output = "IBM" + num;
					else if (num == 65001)           output = "UTF-8";
					else
					{
						output = "IBM850";
						_logger.warn("Windows codepage '" + num + "' is unknown in translation table, setting it to '" + output + "'");
					}
				}
			}
			_logger.info("Detected Windows codepage='" + output + "'.");
		}

		if (StringUtil.isNullOrBlank(output))
		{
			output = "IBM850";
			_logger.info("After codepage lookup translation, the code page is still not known. setting it to '" + output + "' as a last resort.");
		}

		return output;
	}
	
	
	@Override
	public String getOsName()
	{
		// Return CACHED value
		if (_uname != null)
			return _uname;

		try
		{
			String output = execCommandOutputAsStr("uname");
			_uname = output;
			return _uname;
		}
		catch (Exception ex)
		{
			_logger.error("Problems getting OS Name, using 'uname'.", ex);
		}
		
		try
		{
			String output = execCommandOutputAsStr("(dir 2>&1 *`|echo CMD);&<# rem #>echo ($PSVersionTable).PSEdition");
			_uname = output;
			return _uname;
		}
		catch (Exception ex)
		{
			_logger.error("Problems getting OS Name, using '(dir 2>&1 *`|echo CMD);&<# rem #>echo ($PSVersionTable).PSEdition'.", ex);
		}
		
		return _uname;
	}

//	@Override
//	public String getOsName()
//	{
//		_uname = "Linux";
//		return _uname;
//	}
//	@Override
//	public String getOsName()
//	{
//		String javaOsName = System.getProperty("os.name", "");
//		String output = "";
//System.out.println("HostMonitorConnectionLocalOs(): javaOsName=|" + javaOsName + "|.");
//
//		// ---- Windows like systems
//		if (javaOsName.startsWith("Windows"))
//		{
////			try
////			{
////				output = execCommandOutputAsStr("(dir 2>&1 *`|echo CMD);&<# rem #>echo ($PSVersionTable).PSEdition");
////
////				if (StringUtil.hasValue(output))
////				{
////					if (output.equals("Core")   ) output = "Powershell-Core";
////					if (output.equals("Desktop")) output = "Powershell-Desktop";
////
////					output = "Windows-" + output;
////				}
////			}
////			catch (Exception ex)
////			{
////				_logger.error("Problems getting OS Name specifics for 'Windows' like systems, lets continue with default of 'Windows-CMD'. Caught: " + ex);
////				output = "Windows-CMD";
////			}
//			output = "Windows-CMD";
//		}
//		// ---- Otherwise presume the OS has 'uname'
//		else
//		{
//			try
//			{
//				// Simple execute 'uname'
//				output = execCommandOutputAsStr("uname");
//			}
//			catch (Exception ex)
//			{
//				_logger.error("Problems getting OS Name, using 'uname'.", ex);
//			}
//		}
//
//System.out.println("HostMonitorConnectionLocalOs(): <<<--- |" + output + "|.");
//		return output;
//	}

	/**
	 * On Linux, utilities is upgraded some times... which means there could be new columns etc...
	 * Get version of some unix utilities
	 * 
	 * @param utilityType
	 * @return version number as an integer version... holding (major, minor, maintenance) 2 "positions" each in the integer<br>
	 *         version "10.2.0" is returned as 100200 <br>
	 *         version "3.3.9"  is returned as  30309 <br>
	 * @throws Exception
	 */
	@Override
	public int getLinuxUtilVersion(LinuxUtilType utilType) throws Exception
	{
//		gorans@gorans-ub:~$ iostat -V
//		sysstat version 10.2.0
//		(C) Sebastien Godard (sysstat <at> orange.fr)
		
//		gorans@gorans-ub:~$ vmstat -V
//		vmstat from procps-ng 3.3.9

//		gorans@gorans-ub:~$ uptime -V
//		uptime from procps-ng 3.3.9

//		gorans@gorans-ub:~$ mpstat -V
//		sysstat version 10.2.0
//		(C) Sebastien Godard (sysstat <at> orange.fr)

//		[23:23:38][sybase@mig2-sybase:~/dbxtune]$ ps -V
//		procps-ng version 3.3.10
		
		String cmd = "";
		if      (LinuxUtilType.IOSTAT.equals(utilType)) cmd = "iostat -V";
		else if (LinuxUtilType.VMSTAT.equals(utilType)) cmd = "vmstat -V";
		else if (LinuxUtilType.MPSTAT.equals(utilType)) cmd = "mpstat -V";
		else if (LinuxUtilType.UPTIME.equals(utilType)) cmd = "uptime -V";
		else if (LinuxUtilType.PS    .equals(utilType)) cmd = "ps -V";
		else
			throw new Exception("Unsupported utility of '"+utilType+"'.");

		
		String output = execCommandOutputAsStr(cmd);

		int i = VersionShort.parse(output);
		_logger.debug("getLinuxUtilVersion() stdout: '" + output + "'. VersionShort.parse() returned: " + i);

		int intVersion = -1;
		String usedVersionString = null;
		if (i >= 0)
		{
			intVersion = i;
			usedVersionString = output;
		}
		
		_logger.info("When issuing command '" + cmd + "' the version " + intVersion + " was parsed from the version string '" + StringUtil.removeLastNewLine(usedVersionString) + "'.");


		_logger.debug("getLinuxUtilVersion(): returned " + intVersion );
//System.out.println("HostMonitorConnectionLocalOsCmdWrapper.getLinuxUtilVersion(): returned " + intVersion );
		return intVersion;
	}

	private static String substituteVars(String cmd, Map<String, String> envMap)
	{
		String hostmonCmd     = envMap.getOrDefault("HOSTMON_CMD"     , "");
		String hostmonCmdFile = envMap.getOrDefault("HOSTMON_CMD_FILE", "");
		String sshUsername    = envMap.getOrDefault("SSH_USERNAME"    , "");
		String sshPassword    = envMap.getOrDefault("SSH_PASSWORD"    , "");
		String sshKeyFile     = envMap.getOrDefault("SSH_KEYFILE"     , "");
		String sshHostname    = envMap.getOrDefault("SSH_HOSTNAME"    , "");
		String sshPort        = envMap.getOrDefault("SSH_PORT"        , "22");
		
		String returnCmd = cmd;
		if (returnCmd.contains("${hostMonCmd}"    )) { returnCmd = returnCmd.replace("${hostMonCmd}"     , hostmonCmd    ); }
		if (returnCmd.contains("${hostMonCmdFile}")) { returnCmd = returnCmd.replace("${hostMonCmdFile}" , hostmonCmdFile); }
		if (returnCmd.contains("${sshUsername}"   )) { returnCmd = returnCmd.replace("${sshUsername}"    , sshUsername   ); }
		if (returnCmd.contains("${sshPassword}"   )) { returnCmd = returnCmd.replace("${sshPassword}"    , sshPassword   ); }
		if (returnCmd.contains("${sshKeyFile}"    )) { returnCmd = returnCmd.replace("${sshKeyFile}"     , sshKeyFile    ); }
		if (returnCmd.contains("${sshHostname}"   )) { returnCmd = returnCmd.replace("${sshHostname}"    , sshHostname   ); }
		if (returnCmd.contains("${sshPort}"       )) { returnCmd = returnCmd.replace("${sshPort}"        , sshPort       ); }

		return returnCmd;
	}
	
	private static File createHostMonCmdFile(String cmd)
	{
		File cmdFile = null;
		
		boolean writeCommandToFile = Configuration.getCombinedConfiguration().getBooleanProperty("HostMonitorConnectionLocalOsCmdWrapper.writeCommandToFile", true);
		if (writeCommandToFile)
		{
			try 
			{ 
				cmdFile = File.createTempFile(Version.getAppName() + ".hostmon.", ".tmp"); 

				FileUtils.writeStringToFile(cmdFile, cmd, Charset.defaultCharset()); 
			}
			catch (Exception ex) 
			{
				_logger.info("Problems writing '${hostMonCmdFile}' with name ='" + cmdFile + "', Continuing anyway. Caught: " + ex); 
			}
		}
		
		return cmdFile;
	}
	
	private static void writeCommandToStdin(Process process, String wrapperCmd, String sendCmd)
	{
		boolean writeCommandToStdin = Configuration.getCombinedConfiguration().getBooleanProperty("HostMonitorConnectionLocalOsCmdWrapper.writeCommandToStdin", true);
		if (writeCommandToStdin)
		{
			System.out.println("Send Command As STDIN to wrapperCmd=|"+wrapperCmd+"|, sendCmd=|"+sendCmd+"|");

			// Get a handle to the STDIN to the process
			OutputStream stdin = process.getOutputStream(); 
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
			
			try 
			{
    	        writer.write(sendCmd);
    	        writer.flush();
    			writer.close();
			}
			catch (Exception ex) 
			{
				_logger.info("Problems writing cmd=|" + sendCmd + "| to process, Continuing anyway. Caught: " + ex); 
			}
		}
	}

	@Override
	public String execCommandOutputAsStr(String sendCmd) throws Exception
	{
		String javaOsName = System.getProperty("os.name", "");

		String execShell = "/bin/bash";
		String execSw1   = "-c";
		// ---- Windows like systems
		if (javaOsName.startsWith("Windows"))
		{
			execShell = "cmd.exe";
			execSw1   = "/C";
		}

		// Write the Command to a file, that can be used
		File cmdFile = createHostMonCmdFile(sendCmd); 

		// set some env variable to be used by the execution
		_envMap.put("HOSTMON_CMD", sendCmd);
		_envMap.put("HOSTMON_CMD_FILE", cmdFile == null ? "" : cmdFile.getAbsolutePath());

		// Substitute some ${xxx} variables to a "real" string  
		String wrapperCmd = substituteVars(_wrapperCmd, _envMap);
		
		if (_logger.isDebugEnabled())
			_logger.debug("About to execute 'LocalOsCmd': wrapperCmd='" + wrapperCmd + "', sendCmd='" + sendCmd + "', cmdFile='" + cmdFile + "'.");

		// Create ProcessBuilder
		ProcessBuilder pb = new ProcessBuilder(execShell, execSw1, wrapperCmd);
//		pb.inheritIO();
		pb.redirectErrorStream(true);

		// Change environment, this could be usable if the 'dbxtune.sql.pretty.print.cmd' is a shell script or a bat file
		Map<String, String> env = pb.environment();
		if (_envMap != null)
			env.putAll(_envMap);
		
		// Set current working directory to DBXTUNE_HOME
		//_pb.directory(new File(progDir));
		
		// Start
		Process process = pb.start();

		// Write command to STDIN of the command 
		writeCommandToStdin(process, wrapperCmd, sendCmd);

		// Get output, into the output variable
		InputStream stdout = process.getInputStream();
		String output = IOUtils.toString(stdout, getOsCharset());

		if (StringUtil.hasValue(output))
			output = output.trim();

		// Wait for the process to end
		process.waitFor();
		
		if (cmdFile != null)
			cmdFile.delete();

//System.out.println("HostMonitorConnectionLocalOsCmdWrapper.WRAPPER-1.execCommandOutputAsStr(wrapperCmd=|" + wrapperCmd + "|, sendCmd=|" + sendCmd + "|): <<<<<<<< returned=|" + output + "|");
		return output;
	}
		
	@Override
	public ExecutionWrapper executeCommand(String cmd) throws Exception
	{
		if (_envMap == null)
			_envMap = new HashMap<>();

		// Write the Command to a file, that can be used
		File cmdFile = createHostMonCmdFile(cmd); 
		
		_envMap.put("HOSTMON_CMD", cmd);
		_envMap.put("HOSTMON_CMD_FILE", cmdFile == null ? "" : cmdFile.getAbsolutePath());

		ExecutionWrapperLocalCmdWrapper execWrapper = new ExecutionWrapperLocalCmdWrapper(_envMap, cmdFile);

		String wrapperCmd = substituteVars( _wrapperCmd, _envMap);
		execWrapper.executeCommand(wrapperCmd);

//System.out.println("HostMonitorConnectionLocalOsCmdWrapper.WRAPPER-2.executeCommand(cmd='"+cmd+"'): wrapperCmd=|"+wrapperCmd+"| <<<<<<<< cmdFile="+cmdFile+", returned.execWrapper=|" + execWrapper + "|");
		return execWrapper;
	}



	private static class ExecutionWrapperLocalCmdWrapper
	implements ExecutionWrapper
	{
		private ProcessBuilder _pb;
		private Process        _proc;
		private InputStream    _stdout;
		private InputStream    _stderr;
		private int            _exitStatus = -1;
		
		// Below is used in waitForData(): algorithm: _sleepCount++; _sleepCount*_sleepTimeMultiplier; but maxSleepTime is respected
		protected int            _sleepCount = 0;
		protected int            _sleepTimeMultiplier = 3; 
		protected int            _sleepTimeMax        = 250;

		private Map<String, String> _envMap = null;
		private File _cmdFile; 
		
		// Used in debug prints
		private String _name;

		public ExecutionWrapperLocalCmdWrapper(Map<String, String> envMap, File cmdFile)
		{
			_envMap = envMap;
			_cmdFile = cmdFile;
		}

		@Override
		public void executeCommand(String wrapperCmd) throws Exception
		{
			// Reset sleepCount on every execution
			_sleepCount = 0;
			
			String javaOsName = System.getProperty("os.name", "");

			String execShell = "/bin/bash";
			String execSw1   = "-c";
			// ---- Windows like systems
			if (javaOsName.startsWith("Windows"))
			{
				execShell = "cmd.exe";
				execSw1   = "/C";
			}

			// Get the command to send to the wrapper 
			String sendCmd = _envMap.get("HOSTMON_CMD");

			// Write the Command to a file, that can be used
			_cmdFile = createHostMonCmdFile(sendCmd); 

			if (_logger.isDebugEnabled())
				_logger.debug("About to execute 'LocalOsCmd': wrapperCmd='" + wrapperCmd + "', sendCmd='" + sendCmd + "', cmdFile='" + _cmdFile + "'.");
			
			// Create the ProcessBuilder
			_pb = new ProcessBuilder(execShell, execSw1, wrapperCmd);

			// Change environment, this could be usable if the 'dbxtune.sql.pretty.print.cmd' is a shell script or a bat file
			Map<String, String> env = _pb.environment();
			if (_envMap != null)
				env.putAll(_envMap);
			
			// Set current working directory to DBXTUNE_HOME
			//_pb.directory(new File(progDir));
			
			// Start the process
			_proc = _pb.start();

			// for debug
			_name = "wrapperCmd=|" + wrapperCmd + "|, sendCmd=|" + sendCmd + "|";;

			// Send Command to STDIN of the Process
			writeCommandToStdin(_proc, wrapperCmd, sendCmd);

			// Get the STDIN and STDOUT 
			_stdout = _proc.getInputStream();
			_stderr = _proc.getErrorStream();
		}

		@Override
		public int waitForData() throws InterruptedException
		{
			_sleepCount++;
			int sleepMs = Math.min(_sleepCount * _sleepTimeMultiplier, _sleepTimeMax);;

			if (_logger.isDebugEnabled())
				_logger.debug("waitForData(), sleep(" + sleepMs + "). _name=" + _name);

			Thread.sleep(sleepMs);
			return sleepMs;
		}

		@Override
		public InputStream getStdout()
		{
			return _stdout;
		}

		@Override
		public InputStream getStderr()
		{
			return _stderr;
		}

		@Override
		public Integer getExitStatus()
		{
			_exitStatus = _proc.exitValue();
			return _exitStatus;
		}

		@Override
		public boolean isClosed()
		{
			return ! _proc.isAlive();
		}

		@Override
		public void close()
		{
			if (_cmdFile != null)
				_cmdFile.delete();

			try
			{
				_stdout.close();
				_stderr.close();
			}
			catch (IOException ignore)
			{
			}

			// Kill the process if it's still there
			if (_proc.isAlive())
				_proc.destroy();

//			_pb.close();
		}
	}

}
