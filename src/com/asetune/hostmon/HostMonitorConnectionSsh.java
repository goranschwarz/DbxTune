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

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

import com.asetune.ssh.SshConnection2;
import com.asetune.ssh.SshConnection2.LinuxUtilType;
import com.jcraft.jsch.ChannelExec;

public class HostMonitorConnectionSsh 
extends HostMonitorConnection
{
	private static Logger _logger = Logger.getLogger(HostMonitorConnectionSsh.class);

	private SshConnection2 _sshConn;
	
	public HostMonitorConnectionSsh(SshConnection2 sshConn)
	{
		super(ConnectionType.SSH);
//System.out.println(this.getClass().getSimpleName()+".CONSTRUCTOR(): sshConn="+sshConn);
//new Exception("DUMMY_EXCEPTION").printStackTrace();
		_sshConn = sshConn;
	}

	public SshConnection2 getSshConnection()
	{
		return _sshConn;
	}

	@Override
	public String getHostname()
	{
		return _sshConn.getHost();
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
		_sshConn.close();
	}

	@Override
	public void handleException(Exception ex)
	{
		// In some cases we might want to close the SSH Connection and start "all over"
		if (ex.getMessage().contains("SSH_OPEN_CONNECT_FAILED"))
		{
			try 
			{
				_sshConn.reconnect(); 
			}
			catch(Exception reConnectEx)
			{ 
				_logger.error("Problems SSH reconnect. Caught: " + reConnectEx); 
			}
		}
	}

	@Override
	public boolean isConnected()
	{
//System.out.println(this.getClass().getSimpleName()+".isConnected()");
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
		ExecutionWrapperShh execWrapper = new ExecutionWrapperShh(_sshConn);
		execWrapper.executeCommand(cmd);
		return execWrapper;
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
		private SshConnection2 _sshConn;
		
		private ChannelExec _sshChannel;

		// Below is used in waitForData(): algorithm: _sleepCount++; _sleepCount*_sleepTimeMultiplier; but maxSleepTime is respected
		protected int            _sleepCount = 0;
		protected int            _sleepTimeMultiplier = 3; 
		protected int            _sleepTimeMax        = 250;

		// Used in debug prints
		private String _name;

		public ExecutionWrapperShh(SshConnection2 sshConn)
		{
			_sshConn = sshConn;
		}

		@Override
		public void executeCommand(String cmd) throws Exception
		{
			// Reset sleepCount on every execution
			_sleepCount = 0;
			_name = cmd;
			
			_sshChannel = _sshConn.execCommand(cmd);
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
			try {
				return _sshChannel.getInputStream();
			} catch (IOException ex) {
				throw new RuntimeException("Problems getting STDOUT Stream from SSH Command");
			}
		}

		@Override
		public InputStream getStderr()
		{
			try {
				return _sshChannel.getErrStream();
			} catch (IOException ex) {
				throw new RuntimeException("Problems getting STDERR Stream from SSH Command");
			}
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
			_sshChannel.disconnect();
		}
	}
}
