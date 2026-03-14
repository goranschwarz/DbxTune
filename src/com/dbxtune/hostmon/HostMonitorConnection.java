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
package com.dbxtune.hostmon;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ssh.SshConnection.LinuxUtilType;
import com.dbxtune.utils.StringUtil;

/**
 * This should hold a Connection to the HostMonitoring system. (this could be: a "SSH connection" or a simple "local OS/host")
 * <ul>
 *    <li>If this is a "local OS/host", then Host Monitoring will be done by local command execution</li>
 *    <li>If this is a "remote host", then Host Monitoring will be done over a SSH Connection</li>
 *    <li>In the future: potentially some other connection</li>
 * </ul>
 * 
 */
public abstract class HostMonitorConnection
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private ConnectionType _connType;

	public enum ConnectionType
	{
		LOCAL_OS,
		SSH
	};

	public HostMonitorConnection(ConnectionType connType)
	{
		_connType = connType;
	}

	
	public ConnectionType getConnectionType()
	{
		return
		_connType;
	}

	

	/**
	 * Get what OS we are connected to, typical values:
	 * <ul>
	 *    <li>Linux</li>
	 *    <li>SunOS</li>
	 *    <li>AIX</li>
	 *    <li>HP-UX</li>
	 *    <li>Windows-CMD</li>
	 *    <li>Windows-Powershell-Core</li>
	 *    <li>Windows-Powershell-Desktop</li>
	 * </ul>
	 * @return
	 */
	public abstract String getOsName();
	
	/**
	 * Is Vertias installed on this system
	 * @return
	 * @throws Exception
	 */
	public abstract boolean hasVeritas() throws Exception;
	
	/**
	 * Get version for various Linux Commands (like iostat, mpstat, etc...)
	 * 
	 * @param utilType
	 * @return
	 * @throws Exception
	 */
	public abstract int getLinuxUtilVersion(LinuxUtilType utilType) throws Exception;

	/**
	 * Get host name from the underlying OS
	 * @return host name
	 */
	public abstract String getHostname();

	/**
	 * Get user name from the underlying OS
	 * @return user name
	 */
	public abstract String getUsername();

	/**
	 * Get Number of cores from the underlying OS
	 * @return core count
	 */
	public abstract int getOsCoreCount();

	/**
	 * Get Guessed Charset that the underlying OS is using
	 * @return core count
	 */
	public abstract String getOsCharset();

	
	/**
	 * Is the underlying connection connected?
	 * @return
	 */
	public abstract boolean isConnected();

	/**
	 * Make a connection to the ...
	 * @return
	 */
	public abstract void connect() throws Exception;

	/**
	 * Is the underlying connection closed? 
	 * @return 
	 */
	public abstract boolean isConnectionClosed();

	/**
	 * Close the underlying connection 
	 * @return 
	 */
	public abstract void closeConnection();


	/**
	 * Called from The HostMonitor in case of Exceptions when executing the command
	 * <p>
	 * And here we can decide what to do with the underlying connection (reconnect or similar) 
	 * @return true if we should log the Exception or if it's already handled.
	 */
	public abstract boolean handleException(Exception ex);


	/**
	 * Execute a command
	 * @return a ExecutionWrapper that is implemented by any of the "sub systems" SSH or LocalOs
	 * @throws Exception 
	 */
	public abstract ExecutionWrapper executeCommand(String cmd) throws Exception;
	public abstract ExecutionWrapper executeCommand(String cmd, boolean isStreamingCommand) throws Exception;

	
	/**
	 * Execute a command and get the results as a String (both STDOUT and STDERR)
	 * @param cmd
	 * @return
	 * @throws Exception
	 */
	public abstract String execCommandOutputAsStr(String cmd) throws Exception;

	/**
	 * Interface to be implemented by {@link HostMonitorConnectionSsh}, {@link HostMonitorConnectionLocalOsCmd} or any other implementation
	 * <p>
	 * This should handle execution, taking care of STDOUT/STDERR streams, exit code etc
	 * <p>
	 * Note we can NOT do this in the Connection class, since we will be running multiple commands/requests in parallel
	 */
	public static interface ExecutionWrapper
	{
		/**
		 * Execute the specified command
		 * @param cmd
		 * @throws Exception
		 */
		public void executeCommand(String cmd) throws Exception;
		public void executeCommand(String cmd, boolean isStreamingCommand) throws Exception;

		/**
		 * Returns the STDOUT Stream
		 * @return
		 */
		public InputStream getStdout();

		/**
		 * Returns the STDERR Stream
		 * @return
		 */
		public InputStream getStderr();

		/**
		 * Get the exit code from the Command
		 * @return
		 */
		public Integer getExitStatus();

		/**
		 * If nothing is available on STDOUT/STDERR, then wait for data to arrive on the Streams
		 * <p>
		 * This can be a simple {@link Thread#sleep(long)} or a more advanced "thing" waiting for the Stream to have data and return "at once"
		 * @return
		 * @throws InterruptedException
		 */
		public int waitForData() throws InterruptedException;

		/**
		 * Check if the command is still running and can deliver results on the streams
		 * @return
		 */
		public boolean isClosed(); // Check if the underlying Stream is closed or not

		/**
		 * Close the underlying Command and it's streams
		 */
		public void close();
	}

	/**
	 * Execute an OS Command and return the result as a Object containing STDOUT, STDERR Output and OsReturnCode
	 * @param cmd
	 * @return
	 */
	public ExecOutput execCommand(String cmd)
	{
		// Create a new sample
		ExecOutput execOut = new ExecOutput(cmd);

		ExecutionWrapper execWrapper = null;
		try
		{
			execWrapper = executeCommand(cmd);
		}
		catch (Exception ex)
		{
			execOut.addException(ex);
			return execOut;
		}

		
		InputStream stdout = execWrapper.getStdout();
		InputStream stderr = execWrapper.getStderr();
		
		Charset osCharset = Charset.forName(getOsCharset());
//		Charset osCharset = StandardCharsets.UTF_8;

		BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(stdout, osCharset));
		BufferedReader stderrReader = new BufferedReader(new InputStreamReader(stderr, osCharset));

		boolean running = true;
		while(running)
		{
			try
			{
				if ((stdout.available() == 0) && (stderr.available() == 0))
				{
					try
					{
						execWrapper.waitForData();
					}
					catch (InterruptedException e)
					{
						running = false;
					}
					
					if ( execWrapper.isClosed() )
					{
						if (stdout.available() > 0 || stderr.available() > 0)
							continue;

						break;
					}
				}

				/* If you below replace "while" with "if", then the way the output appears on the local
				 * stdout and stder streams is more "balanced". Additionally reducing the buffer size
				 * will also improve the interleaving, but performance will slightly suffer.
				 * OKOK, that all matters only if you get HUGE amounts of stdout and stderr data =)
				 */
				while (stdout.available() > 0)
				{
					if (_logger.isDebugEnabled())
					{
						_logger.debug("STDOUT[execCommand][available=" + stdout.available() + "]: -start-");
					}
					
					// NOW READ input
					while (stdoutReader.ready())
					{
						// Read row
						String row = stdoutReader.readLine();

						// discard empty rows
						if (StringUtil.isNullOrBlank(row))
							continue;

						if (_logger.isDebugEnabled())
							_logger.debug("Received on STDOUT: " + row);

						execOut.addStdOut(row);
					}
				}

				while (stderr.available() > 0)
				{
					if (_logger.isDebugEnabled())
					{
						_logger.debug("STDERR[execCommand][available=" + stdout.available() + "]: -start-");
					}
					
					// NOW READ input
					while (stderrReader.ready())
					{
						// Read row
						String row = stderrReader.readLine();

//						// discard empty rows
//						if (StringUtil.isNullOrBlank(row))
//							continue;
						
						if (_logger.isDebugEnabled())
							_logger.debug("Received on STDERR: " + row);
						
						execOut.addStdErr(row);
					}
				}
			}
			catch (Exception ex)
			{
				execOut.addException(ex);
			}
		}

		// Sometimes I have seen exception here... so map that away
		try 
		{
			int osRetCode = execWrapper.getExitStatus();
			execOut.setExitCode(osRetCode);
		}
		catch (Exception ignore) { /* ignore */ }

		execWrapper.close();

		// Now return the object, which the OS Commands output was put into
		return execOut;
	}
	
	/**
	 * Output object to separate output for STDOUT, STDERR and ExitCode from the OS Command
	 */
	public static class ExecOutput
	{
		private String _cmd = null;

		private List<String> _stdoutList = new ArrayList<>();
		private List<String> _stderrList = new ArrayList<>();
		private int    _exitCode = -1;

		private List<Exception> _exceptionList = new ArrayList<>();

		public ExecOutput()
		{
		}

		public void addException(Exception ex)
		{
			_exceptionList.add(ex);
		}
		public boolean hasException()
		{
			if (_exceptionList == null) 
				return false;
			return !_exceptionList.isEmpty();
		}
		public List<Exception> getExceptions()
		{
			if (_exceptionList == null) 
				return Collections.emptyList();

			return _exceptionList;
		}
		public Exception getFirstExceptions()
		{
			if (_exceptionList == null) 
				return null;

			return _exceptionList.get(0);
		}

		public void addStdOut(String row)
		{
			_stdoutList.add(row);
		}

		public void addStdErr(String row)
		{
			_stderrList.add(row);
		}

		public ExecOutput(String command)
		{
			_cmd = command;
		}

		@Override
		public String toString()
		{
			String cmd = (_cmd == null) ? "" : _cmd;
			
			// If cmd is "multi-line" just get first row
			int nlPos = cmd.indexOf('\n');
			if (nlPos >= 0)
			{
				cmd = cmd.substring(0, nlPos).trim() + " ## multi-line-input-truncated...";
			}

			return "command=|" + cmd + "|, exitCode=" + _exitCode + ", stdout[" + _stdoutList.size() + "]=|" + getStdOutStr() + "|, stderr[" + _stderrList.size() + "]=|" + getStdErrStr() + "|.";
		}

//		public boolean hasValueStdOut() { return StringUtil.hasValue(_stdout); }
//		public boolean hasValueStdErr() { return StringUtil.hasValue(_stderr); }
//
//		public String  getStdOut() { return _stdout == null ? "" : _stdout; }
//		public String  getStdErr() { return _stderr == null ? "" : _stderr; }

		public boolean hasValueStdOut() { return !_stdoutList.isEmpty(); }
		public boolean hasValueStdErr() { return !_stderrList.isEmpty(); }

		public List<String>  getStdOut() { return _stdoutList; }
		public List<String>  getStdErr() { return _stderrList; }

		public String getStdOutStr()
		{
			StringBuilder sb = new StringBuilder();
			for (String row : _stdoutList)
				sb.append(row).append("\n");
			return sb.toString().trim();
		}
		public String getStdErrStr()
		{
			StringBuilder sb = new StringBuilder();
			for (String row : _stderrList)
				sb.append(row).append("\n");
			return sb.toString().trim();
		}
		
		public int     getExitCode()    { return _exitCode; }
		
//		public void    setStdOut(String str) { _stdout   = str; }
//		public void    setStdErr(String str) { _stderr   = str; }
		public void    setExitCode(int rc)   { _exitCode = rc; }

		public boolean containsStdOut(String value)
		{
			for (String row : _stdoutList)
			{
				if (row.contains(value))
				{
					return true;
				}
			}
			return false;
		}

		public boolean containsStdErr(String value)
		{
			for (String row : _stderrList)
			{
				if (row.contains(value))
				{
					return true;
				}
			}
			return false;
		}
	}

	
}
