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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.asetune.ssh.SshConnection2.LinuxUtilType;
import com.asetune.utils.StringUtil;
import com.asetune.utils.VersionShort;

public class HostMonitorConnectionLocalOsCmd 
extends HostMonitorConnection
{
	private static Logger _logger = Logger.getLogger(HostMonitorConnectionLocalOsCmd.class);

	/** Keep a logical status if we are connected or not, this to "simulate" an SSH or "other" connection */
	private boolean _isConnected = false;
	
	/** This can be used to execute a "local" ssh command, which connects to the remote system and returns results */
	protected Map<String, String> _envMap     = null;

	public HostMonitorConnectionLocalOsCmd(boolean isConnected)
	{
		super(ConnectionType.LOCAL_OS);
		_isConnected = isConnected;
	}

	public HostMonitorConnectionLocalOsCmd(boolean isConnected, Map<String, String> envMap)
	{
		super(ConnectionType.LOCAL_OS);
		_isConnected = isConnected;
		_envMap      = envMap;
	}

	@Override
	public String getHostname()
	{
		try { return InetAddress.getLocalHost().getHostName(); } // Or possibly call OS 'hostname'
		catch (Exception ignore) { return "-unknown-"; }
	}

	@Override
	public int getOsCoreCount()
	{
		return Runtime.getRuntime().availableProcessors();
	}

	@Override
	public String getOsCharset()
	{
	//	return System.getProperty("file.encoding", "UTF-8");
		OutputStreamWriter writer = new OutputStreamWriter(new ByteArrayOutputStream());
		String enc = writer.getEncoding();
		return enc;
	}


	@Override
	public boolean isConnectionClosed()
	{
		return ! isConnected();
	}

	@Override
	public void closeConnection()
	{
		_isConnected = false;
	}

	@Override
	public boolean isConnected()
	{
		return _isConnected;
	}

	@Override
	public void connect() throws Exception
	{
		_isConnected = true;
		// empty implementation, because we don't need to connect anywhere (this is a local session)
	}

	@Override
	public void handleException(Exception ex)
	{
	}


	@Override
	public String getOsName()
	{
		String javaOsName = System.getProperty("os.name", "");
		String output = "";
//System.out.println("HostMonitorConnectionLocalOsCmd.getOsName(): javaOsName=|" + javaOsName + "|.");

		// ---- Windows like systems
		if (javaOsName.startsWith("Windows"))
		{
//			try
//			{
//				output = execCommandOutputAsStr("(dir 2>&1 *`|echo CMD);&<# rem #>echo ($PSVersionTable).PSEdition");
//
//				if (StringUtil.hasValue(output))
//				{
//					if (output.equals("Core")   ) output = "Powershell-Core";
//					if (output.equals("Desktop")) output = "Powershell-Desktop";
//
//					output = "Windows-" + output;
//				}
//			}
//			catch (Exception ex)
//			{
//				_logger.error("Problems getting OS Name specifics for 'Windows' like systems, lets continue with default of 'Windows-CMD'. Caught: " + ex);
//				output = "Windows-CMD";
//			}
			output = "Windows-CMD";
		}
		// ---- Otherwise presume the OS has 'uname'
		else
		{
			try
			{
				// Simple execute 'uname'
				output = execCommandOutputAsStr("uname");
			}
			catch (Exception ex)
			{
				_logger.error("Problems getting OS Name, using 'uname'.", ex);
			}
		}

//System.out.println("HostMonitorConnectionLocalOsCmd.getOsName(): <<<--- |" + output + "|.");
		return output;
	}

	@Override
	public boolean hasVeritas() throws Exception
	{
		return false; // lets assume for now...
	}

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
		_logger.debug("HostMonitorConnectionLocalOsCmd.getLinuxUtilVersion() stdout: '" + output + "'. VersionShort.parse() returned: " + i);

		int intVersion = -1;
		String usedVersionString = null;
		if (i >= 0)
		{
			intVersion = i;
			usedVersionString = output;
		}
		
		_logger.info("When issuing command '" + cmd + "' the version " + intVersion + " was parsed from the version string '" + StringUtil.removeLastNewLine(usedVersionString) + "'.");


		_logger.debug("getLinuxUtilVersion(): returned " + intVersion );
//System.out.println("HostMonitorConnectionLocalOsCmd.getLinuxUtilVersion(): returned " + intVersion );
		return intVersion;
	}

	@Override
	public String execCommandOutputAsStr(String cmd) throws Exception
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
		// Parse the command into a String array, if cmd has parameters it needs to be "split" into and array
		//String[] params = StringUtil.translateCommandline(cmd, false);

		// Create ProcessBuilder
		ProcessBuilder pb = new ProcessBuilder(execShell, execSw1, cmd);
//		pb.inheritIO(); // This sets stdin/stdout/stderr to the java processes (and we don't want that) 
		pb.redirectErrorStream(true);

		// Change environment, this could be usable if the 'dbxtune.sql.pretty.print.cmd' is a shell script or a bat file
		Map<String, String> env = pb.environment();
		env.put("HOSTMON_CMD", cmd);
		
		// Set current working directory to DBXTUNE_HOME
		//_pb.directory(new File(progDir));
		
		// Start
		Process process = pb.start();

		// Get output, into the output variable
		InputStream stdout = process.getInputStream();
		String output = IOUtils.toString(stdout, getOsCharset());
		if (StringUtil.hasValue(output))
			output = output.trim();

//System.out.println("HostMonitorConnectionLocalOsCmd.execCommandOutputAsStr(cmd=|"+cmd+"|): <<<<<<<< returned=|" + output + "|");
		return output;
	}

	@Override
	public ExecutionWrapper executeCommand(String cmd) throws Exception
	{
		if (_envMap == null)
			_envMap = new HashMap<>();
		
		_envMap.put("HOSTMON_CMD", cmd);

		ExecutionWrapperLocalCmd execWrapper = new ExecutionWrapperLocalCmd(_envMap);

		execWrapper.executeCommand(cmd);

//System.out.println("HostMonitorConnectionLocalOsCmd.executeCommand(cmd=|"+cmd+"|): <<<<<<<< returned.execWrapper=|" + execWrapper + "|");
		return execWrapper;
	}




	protected static class ExecutionWrapperLocalCmd
	implements ExecutionWrapper
	{
		protected ProcessBuilder _pb;
		protected Process        _proc;
		protected InputStream    _stdout;
		protected InputStream    _stderr;
		protected int            _exitStatus = -1;
		
		// Below is used in waitForData(): algorithm: _sleepCount++; _sleepCount*_sleepTimeMultiplier; but maxSleepTime is respected
		protected int            _sleepCount = 0;
		protected int            _sleepTimeMultiplier = 3; 
		protected int            _sleepTimeMax        = 250;

		protected Map<String, String> _envMap = null;

		// Used in debug prints
		private String _name;

		public ExecutionWrapperLocalCmd(Map<String, String> envMap)
		{
			_envMap = envMap;
		}
		
		@Override
		public void executeCommand(String cmd) throws Exception
		{
			// Reset sleepCount on every execution
			_sleepCount = 0;
			
//System.out.println("HostMonitorConnectionLocalOsCmd.ExecutionWrapperLocalCmd.executeCommand(cmd=|"+cmd+"|).");
			String javaOsName = System.getProperty("os.name", "");

			String execShell = "/bin/bash";
			String execSw1   = "-c";
			// ---- Windows like systems
			if (javaOsName.startsWith("Windows"))
			{
				execShell = "cmd.exe";
				execSw1   = "/C";
			}

			// Parse the command into a String array, if cmd has parameters it needs to be "split" into and array
//			String[] params = StringUtil.translateCommandline(cmd, false);

			// Create the ProcessBuilder
//			_pb = new ProcessBuilder(params);
			_pb = new ProcessBuilder(execShell, execSw1, cmd);

//System.out.println("HostMonitorConnectionLocalOsCmd.ExecutionWrapperLocalCmd.execCommandOutputAsStr(): _pb.command()=[" + StringUtil.toCommaStrQuoted("|", _pb.command()) + "]");

			// Change environment, this could be usable if the 'dbxtune.sql.pretty.print.cmd' is a shell script or a bat file
			Map<String, String> env = _pb.environment();
			if (_envMap != null)
				env.putAll(_envMap);
//System.out.println("HostMonitorConnectionLocalOsCmd.ExecutionWrapperLocalCmd.ProcessBuilder: _envMap: " + StringUtil.toCommaStr(_envMap));
			
			// Set current working directory to DBXTUNE_HOME
			//_pb.directory(new File(progDir));
			
			// Start the process
			_proc = _pb.start();

			_name = cmd;

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
