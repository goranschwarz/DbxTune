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

import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ssh.SshConnection;
import com.dbxtune.ssh.SshConnection.ExecChannel;
import com.dbxtune.ssh.SshConnection.LinuxUtilType;
import com.dbxtune.utils.Configuration;
import com.jcraft.jsch.ChannelExec;

public class HostMonitorConnectionSsh 
extends HostMonitorConnection
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private SshConnection _sshConn;

	/** Windows only: A long lived PowerShell process on the remote host, see executeInPowershellSession() */
	private PowershellSession _psSession;
	private int               _psSessionFailCount = 0;

	public HostMonitorConnectionSsh(SshConnection sshConn)
	{
		super(ConnectionType.SSH);
//System.out.println(this.getClass().getSimpleName()+".CONSTRUCTOR(): sshConn="+sshConn);
//new Exception("DUMMY_EXCEPTION").printStackTrace();
		_sshConn = sshConn;
	}

	public SshConnection getSshConnection()
	{
		return _sshConn;
	}

	@Override
	public String getHostname()
	{
		return _sshConn.getHost();
	}

	@Override
	public String getUsername()
	{
		return _sshConn.getUsername();
	}

	@Override
	public int getOsCoreCount()
	{
		return _sshConn.getNproc();
	}

	@Override
	public String getOsCharset()
	{
		return _sshConn.getOsCharset();
	}


	@Override
	public boolean isConnectionClosed()
	{
		return _sshConn.isClosed();
	}

//	@Override
//	public void closeCommand()
//	{
//		// Hmmm... should we also close the Connection...
//		_sshSession.close();
//	}
	
	@Override
	public void closeConnection()
	{
		// EOF on STDIN, so the remote 'powershell' exits by itself
		PowershellSession psSession = _psSession;
		if (psSession != null)
			psSession.close();

		_sshConn.close();
	}

	@Override
	public boolean handleException(Exception ex)
	{
		// In some cases we might want to close the SSH Connection and start "all over"
		if (ex.getMessage().contains("session is down"))          // For JSCH               this is the message for failed connection
//		if (ex.getMessage().contains("SSH_OPEN_CONNECT_FAILED"))  // For ganymed-ssh2-*.jar this was the message 
		{
			try 
			{
				_sshConn.reconnect(); 
			}
			catch(Exception reConnectEx)
			{ 
				_logger.error("Problems SSH reconnect. Caught: " + reConnectEx);
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isConnected()
	{
//System.out.println(this.getClass().getSimpleName()+".isConnected()");
		if (_sshConn == null)
			return false;

		return _sshConn.isConnected();
	}

	@Override
	public void connect() throws Exception
	{
//System.out.println(this.getClass().getSimpleName()+".connect()");
		_sshConn.connect();
	}

	@Override
	public String getOsName()
	{
		return _sshConn.getOsName();
	}

	@Override
	public boolean hasVeritas() throws Exception
	{
		return _sshConn.hasVeritas();
	}

	@Override
	public int getLinuxUtilVersion(LinuxUtilType utilType) throws Exception
	{
		return _sshConn.getLinuxUtilVersion(utilType);
	}

	@Override
	public String execCommandOutputAsStr(String cmd) throws Exception
	{
		return _sshConn.execCommandOutputAsStr(cmd);
	}

	@Override
	public ExecutionWrapper executeCommand(String cmd) throws Exception
	{
//		ExecutionWrapperShh execWrapper = new ExecutionWrapperShh(_sshConn);
//		execWrapper.executeCommand(cmd);
//		return execWrapper;
		return executeCommand(cmd, false);
	}

	@Override
	public ExecutionWrapper executeCommand(String cmd, boolean isStreamingCommand) throws Exception
	{
		ExecutionWrapperShh execWrapper = new ExecutionWrapperShh(_sshConn);
		execWrapper.executeCommand(cmd, isStreamingCommand);
		return execWrapper;
	}

	@Override
	public synchronized ExecutionWrapper executeInPowershellSession(String psScript) throws Exception
	{
		if ( ! _sshConn.isWindows() || _psSessionFailCount >= 3 )
			return null;

		Configuration conf = Configuration.getCombinedConfiguration();
		if ( ! conf.getBooleanProperty(HostMonitor.PROPKEY_windows_powershell_session_enabled, HostMonitor.DEFAULT_windows_powershell_session_enabled) )
		{
			if (_psSession != null)
			{
				_psSession.close();
				_psSession = null;
			}
			return null;
		}

		// Start a new session: First time, or after a SSH reconnect (the channel of the old session is then closed)
		boolean isNewSession = (_psSession == null || _psSession.isClosed());
		if (isNewSession)
		{
			if (_psSession != null)
				_psSession.close();

			// NOTE: SSH problems here are thrown to the caller (and handled like for any other command)
			final ExecChannel execChannel = _sshConn.execCommand(PowershellSession.START_COMMAND, false); // false = NO PTY
			_psSession = new PowershellSession(execChannel.getChannel().getOutputStream(), execChannel.getStdout(), execChannel.getStderr(), Charset.forName(getOsCharset()))
			{
				@Override public boolean isClosed() { return execChannel.getChannel().isClosed(); }
				@Override public void    close()    { super.close(); execChannel.getChannel().disconnect(); } // First EOF on STDIN (powershell exits by itself), then disconnect
			};
		}

		try
		{
			if (isNewSession)
			{
				// First command: This is where we pay for loading PowerShell/.NET, and it proves that STDIN/STDOUT works via the login shell
				long startTime = System.currentTimeMillis();
				_psSession.execute("$ProgressPreference = 'SilentlyContinue'", conf.getIntProperty(HostMonitor.PROPKEY_windows_powershell_session_startTimeoutSec, HostMonitor.DEFAULT_windows_powershell_session_startTimeoutSec));

				_logger.info("Started a PowerShell session at '" + getHostname() + "' in " + (System.currentTimeMillis() - startTime) + " ms, using '" + PowershellSession.START_COMMAND + "'. "
						+ "Non-streaming Host Monitor commands will be executed in that session (instead of starting a new 'powershell' for every execution). "
						+ "This can be disabled with: " + HostMonitor.PROPKEY_windows_powershell_session_enabled + " = false");
			}

			ExecutionWrapper execWrapper = _psSession.execute(psScript, conf.getIntProperty(HostMonitor.PROPKEY_executeAndParse_timeoutSec, HostMonitor.DEFAULT_executeAndParse_timeoutSec));
			_psSessionFailCount = 0;
			return execWrapper;
		}
		catch (Exception ex)
		{
			// The session is in a unknown state (output from the failed script may arrive later), so throw it away
			_psSession.close();
			_psSession = null;
			_psSessionFailCount++;

			_logger.warn("Problems using the PowerShell session at '" + getHostname() + "', the script will instead be executed as a normal OS Command (which starts a new 'powershell'). "
					+ (_psSessionFailCount >= 3
							? "This was failure number " + _psSessionFailCount + " in a row, so the PowerShell session will NOT be used anymore for this connection. "
							: "A new session will be started on next execution. ")
					+ "The PowerShell session can be disabled with: " + HostMonitor.PROPKEY_windows_powershell_session_enabled + " = false. Caught: " + ex);

			// On timeout: The sample is abandoned (like for any other command that times out)
			if (ex instanceof TimeoutException)
				throw ex;

			return null;
		}
	}






//	private static class ExecutionWrapperShh
//	implements ExecutionWrapper
//	{
//		private SshConnection _sshConn;
//		
//		private Session _sshSession;
//
//		// Below is used in waitForData(): algorithm: _sleepCount++; _sleepCount*_sleepTimeMultiplier; but maxSleepTime is respected
//		protected int            _sleepCount = 0;
//		protected int            _sleepTimeMultiplier = 3; 
//		protected int            _sleepTimeMax        = 250;
//
//		// Used in debug prints
//		private String _name;
//
//		public ExecutionWrapperShh(SshConnection sshConn)
//		{
//			_sshConn = sshConn;
//		}
//
//		@Override
//		public void executeCommand(String cmd) throws Exception
//		{
//			// Reset sleepCount on every execution
//			_sleepCount = 0;
//			_name = cmd;
//			
//			_sshSession = _sshConn.execCommand(cmd);
//		}
//
////		@Override
////		public String getCharset()
////		{
////			return _sshConn.getOsCharset();
////		}
//
//		@Override
//		public int waitForData() throws InterruptedException
//		{
//			_sleepCount++;
//			int sleepMs = Math.min(_sleepCount * _sleepTimeMultiplier, _sleepTimeMax);;
//
//			if (_logger.isDebugEnabled())
//				_logger.debug("waitForData(), sleep(" + sleepMs + "). _name=" + _name);
//
//			Thread.sleep(sleepMs);
//			return sleepMs;
//		}
//
//		@Override
//		public InputStream getStdout()
//		{
//			return _sshSession.getStdout();
//		}
//
//		@Override
//		public InputStream getStderr()
//		{
//			return _sshSession.getStderr();
//		}
//
//		@Override
//		public Integer getExitStatus()
//		{
//			return _sshSession.getExitStatus();
//		}
//
//		@Override
//		public boolean isClosed()
//		{
//			return _sshSession.getState() == 4; // STATE_CLOSED = 4;
//		}
//
//		@Override
//		public void close()
//		{
//			_sshSession.close();
//		}
//	}

	private static class ExecutionWrapperShh
	implements ExecutionWrapper
	{
		private SshConnection _sshConn;
		
		private ExecChannel _execChannel;
		private ChannelExec _sshChannel;

		// Below is used in waitForData(): algorithm: _sleepCount++; _sleepCount*_sleepTimeMultiplier; but maxSleepTime is respected
		protected int            _sleepCount = 0;
		protected int            _sleepTimeMultiplier = 3; 
		protected int            _sleepTimeMax        = 250;

		// Used in debug prints
		private String _name;

		public ExecutionWrapperShh(SshConnection sshConn)
		{
			_sshConn = sshConn;
		}

		@Override
		public void executeCommand(String cmd) throws Exception
		{
//			// Reset sleepCount on every execution
//			_sleepCount = 0;
//			_name = cmd;
//
//			_sshChannel = _sshConn.execCommand(cmd);
			executeCommand(cmd, false);
		}

		@Override
		public void executeCommand(String cmd, boolean isStreamingCommand) throws Exception
		{
			// Reset sleepCount on every execution
			_sleepCount = 0;
			_name = cmd;

			// On Windows: Request a PTY for streaming commands (like 'typeperf'), otherwise the Win32-OpenSSH SSHD
			// leaves the command running (orphaned) when the channel/session is closed.
			// see: https://github.com/PowerShell/Win32-OpenSSH/issues/1751
			// On Linux/Unix: no PTY (SSHD kills the remote processes when the session ends)
			//
			// BUT: Only where Win32-OpenSSH uses a real ConPTY (Windows build >= 17763, Server 2019), which honors the requested width.
			//      On older Windows (like Server 2016, build 14393) it uses 'ssh-shellhost.exe', a console "screen scraper", which
			//      wraps at 128 columns and repaints the screen with cursor positioning (overlapping text), so the typeperf CSV is garbled.
			//      There we skip the PTY and live with orphans (they end by themselves due to typeperf '-sc', see HostMonitor.PROPKEY_windows_typeperf_stopAfterXHours)
			//      This can be overridden with: HostMonitor.windows.ssh.requestPty = auto | true | false
			//
			// ALTERNATIVE (if the PTY approach does NOT work): A PowerShell wrapper that kills the command when STDIN is closed (EOF)
			//   - When the SSH channel is closed, SSHD closes the child's STDIN, so the wrapper gets EOF and kills 'typeperf' (from within the same logon session)
			//   - The command would be something like:
			//       powershell -NoProfile -Command "$p = Start-Process typeperf -ArgumentList '-si 10 \"\PhysicalDisk(*)\*\"' -NoNewWindow -PassThru; [void][Console]::In.ReadToEnd(); Stop-Process -Id $p.Id -Force"
			//   - And in close(): call '_sshChannel.getOutputStream().close()' (sends EOF) before '_sshChannel.disconnect()'
			//   - Note: A cleanup from a NEW SSH session (like 'taskkill' on connect) fails with 'Access denied', since the orphans belong to another logon session
			boolean requestPty = isStreamingCommand && _sshConn.isWindows() && isWindowsPtyEnabled(cmd);

			_execChannel = _sshConn.execCommand(cmd, requestPty);
			_sshChannel  = _execChannel.getChannel();
		}

		/**
		 * Decide if a PTY should be requested for a Windows streaming command (see HostMonitor.PROPKEY_windows_ssh_requestPty)
		 */
		private boolean isWindowsPtyEnabled(String cmd)
		{
			String cfg   = Configuration.getCombinedConfiguration().getProperty(HostMonitor.PROPKEY_windows_ssh_requestPty, HostMonitor.DEFAULT_windows_ssh_requestPty);
			int    build = _sshConn.getWindowsBuild();

			boolean requestPty;
			String  reason;
			if ("true".equalsIgnoreCase(cfg))
			{
				requestPty = true;
				reason     = "forced by config";
			}
			else if ("false".equalsIgnoreCase(cfg))
			{
				requestPty = false;
				reason     = "disabled by config";
			}
			else // auto (or unknown value)
			{
				// Unknown build (-1) = no PTY: a garbled CSV breaks all modules, orphans ends by themselves
				requestPty = build >= HostMonitor.WINDOWS_BUILD_FIRST_WITH_CONPTY;
				reason     = requestPty
						? "build >= " + HostMonitor.WINDOWS_BUILD_FIRST_WITH_CONPTY + ", ConPTY is available"
						: (build == -1 ? "unknown build" : "build < " + HostMonitor.WINDOWS_BUILD_FIRST_WITH_CONPTY + ", no ConPTY");
			}

			_logger.info("Windows build " + build + " (" + reason + "): " + (requestPty ? "Requesting" : "NOT requesting") + " a PTY for command '" + cmd + "'. (" + HostMonitor.PROPKEY_windows_ssh_requestPty + "=" + cfg + ")");
			return requestPty;
		}

//		@Override
//		public String getCharset()
//		{
//			return _sshConn.getOsCharset();
//		}

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
			// NOTE: Do NOT use _sshChannel.getInputStream() here, it creates a new pipe, and the output received before that is lost
			return _execChannel.getStdout();
		}

		@Override
		public InputStream getStderr()
		{
			return _execChannel.getStderr();
		}

		@Override
		public Integer getExitStatus()
		{
			return _sshChannel.getExitStatus();
		}

		@Override
		public boolean isClosed()
		{
			return _sshChannel.isClosed();
		}

		@Override
		public void close()
		{
			try 
			{
				if (_sshChannel != null && !_sshChannel.isClosed()) 
				{
					 // JSch signal
					_sshChannel.sendSignal("KILL");
				}
			} 
			catch (Exception ignore) { /* ignore */ } 
			finally 
			{
				if (_sshChannel != null) 
					_sshChannel.disconnect();
			}
		}
	}
}
