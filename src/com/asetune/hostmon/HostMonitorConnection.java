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

import java.io.InputStream;

import com.asetune.ssh.SshConnection.LinuxUtilType;

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
	 * @return 
	 */
	public abstract void handleException(Exception ex);


	/**
	 * Execute a command
	 * @return a ExecutionWrapper that is implemented by any of the "sub systems" SSH or LocalOs
	 * @throws Exception 
	 */
	public abstract ExecutionWrapper executeCommand(String cmd) throws Exception;

	
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
	
	
}
