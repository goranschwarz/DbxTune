/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import com.asetune.Version;
import com.asetune.gui.swing.PromptForPassword;
import com.asetune.gui.swing.PromptForPassword.SaveType;
import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;
import com.asetune.utils.VersionShort;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ConfigRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.OpenSSHConfig;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;


public class SshConnection
{
	private static Logger _logger = Logger.getLogger(SshConnection.class);

	private String _username = null;
	private String _password = null;
	private String _hostname = null;
	private int    _port     = 22;
	private String _keyFile  = null;
	
	private JSch _jsch;
	private Session _conn;

	/** output from 'uname -a', which was made while the connection was created. */
	private String _uname = null;

	/** First String in the output from 'uname -a', which was made while the connection was created. */
	private String _osName = null;

	/** outpu from the 'nproc' command. Which tells us how many scheduling/processing units are available on this os */
	private int _nproc = -1;

	/** Used to create Strings from the remote host, so that client character set convention can be done. 
	 * NOTE: For the moment this is hard coded based on the OsName 
	 * (Linux=UTF-8, SunOs=ISO-8859-1, AIX=ISO-8859-1, HP-UX=ISO-8859-1, else=null)*/
	private String _osCharset = null;

	/** If we have been authenticated once, it means that we can do reconnect, because login has been successful at least once */
	private boolean _isAuthenticated = false;

	private boolean _defaultOpenSshKkownHostsFileExists = false;
	
	/** There is stdout data available that is ready to be consumed. */
	public static final int STDOUT_DATA = 1;

	/** There is stderr data available that is ready to be consumed. */
	public static final int STDERR_DATA = 2;

	private static final String _homeDir = System.getProperty("user.home", "");

//	private static final String _knownHostPath = _homeDir + File.separator + ".ssh" + File.separator + "known_hosts";
//	private static final String _idDSAPath     = _homeDir + File.separator + ".ssh" + File.separator + "id_dsa";
	private static final String _idRSAPath     = _homeDir + File.separator + ".ssh" + File.separator + "id_rsa";

	private WaitForExecDialog _waitforDialog = null;
//	private static Component         _guiOwner = null;
	private Component         _guiOwner = null;

//	public static String getRsaKeyFilename() { return _idRSAPath; }

//	public static final String  PROPKEY_sshAuthenticateEnableKeyboardInteractive = "ssh.authenticate.enable.KeyboardInteractive";
//	public static final boolean DEFAULT_sshAuthenticateEnableKeyboardInteractive = true;
//
//	public static final String  PROPKEY_sshAuthenticateEnableDSA                 = "ssh.authenticate.enable.DSA";
//	public static final boolean DEFAULT_sshAuthenticateEnableDSA                 = true;
//
//	public static final String  PROPKEY_sshAuthenticateEnableRSA                 = "ssh.authenticate.enable.RSA";
//	public static final boolean DEFAULT_sshAuthenticateEnableRSA                 = true;

	public static final String PROMPT_FOR_PASSWORD = "<PROMPT_FOR_PASSWORD>";


	public static String getRsaKeyFilename()
	{
		return _idRSAPath;
	}

	/**
	 * Create an empty SshConnection, but you need to setUser,password,host
	 */
	public SshConnection()
	{
	}

	/**
	 * Create a SshConnection object (to port 22), you still need to connect() after the object is created
	 * @param hostname
	 * @param username
	 * @param password
	 */
	public SshConnection(String hostname, String username, String password)
	{
		this(hostname, 22, username, password, null);
	}

	/**
	 * Create a SshConnection object, you still need to connect() after the object is created
	 * @param hostname
	 * @param port
	 * @param username
	 * @param password
	 * @param keyFile
	 */
//	public SshConnection(String hostname, int port, String username, String password)
//	{
//		this(hostname, port, username, password, null);
//	}

	/**
	 * Create a SshConnection object, you still need to connect() after the object is created
	 * @param hostname
	 * @param port
	 * @param username
	 * @param password
	 * @param keyFile
	 */
	public SshConnection(String hostname, int port, String username, String password, String keyFile)
	{
		setHost    (hostname);
		setPort    (port);
		setUsername(username);
		setPassword(password);
		setKeyFile (keyFile);

		init();
	}
	
	/**
	 * Setup some basics
	 */
	private void init()
	{
		if (_jsch == null)
		{
			JSch.setLogger(new JschLog4jBridge());
			_jsch = new JSch();

			String sskKnownHostsFile = System.getProperty("user.home") + File.separator + ".ssh" + File.separator + "known_hosts";
			if ( new File(sskKnownHostsFile).exists() )
			{
				try
				{
					_jsch.setKnownHosts(sskKnownHostsFile);
					_logger.error("Setting SSH Known Hosts file '" + sskKnownHostsFile + "'.");
					
					_defaultOpenSshKkownHostsFileExists = true;
				}
				catch (JSchException ex)
				{
					_logger.error("Problems setting SSH Known Hosts file '" + sskKnownHostsFile + "'. Continuing without this...", ex);
				}
			}

			String sshConfigFile = System.getProperty("user.home") + File.separator + ".ssh" + File.separator + "config";
			if ( new File(sshConfigFile).exists() )
			{
				try
				{
					ConfigRepository configRepository = OpenSSHConfig.parseFile(sshConfigFile);
					_jsch.setConfigRepository(configRepository);
					_logger.error("Using Open SSH Config file '" + sshConfigFile + "'.");
				}
				catch (Exception ex) 
				{
					_logger.error("Problems setting SSH Open SSH Config file '" + sshConfigFile + "'. Continuing without this...", ex);
				}
			}

			// Add User Defined Key File
			addKeyFile(_keyFile, _password, true);
			addKeyFile(System.getProperty("user.home") + File.separator + ".ssh" + File.separator + "id_rsa", _password, false);
			addKeyFile(System.getProperty("user.home") + File.separator + ".ssh" + File.separator + "id_dsa", _password, false);
			

		} // end: _jsch == null
	}

	public void setUsername(String username) { _username = username; }
	public void setPassword(String password) { _password = password; }
//	public void setHost(String host)         { _hostname = host; }
	public void setPort(int port)            { _port     = port; }
	public void setKeyFile(String keyFile)   { _keyFile  = keyFile; }
	public void setHost(String host)
	{
		// If this looks like a SQL-Server instance name on a Windows machine (it contains a backslash 'hostname\instanceName' then strip of '\instanceName')
		if (host != null) 
		{
			int pos = host.indexOf("\\");
			if (pos >= 1) // Not starting with '\', but has '\'  somewhere in the middle
			{
				// Strip off 'hostname\instanceName' instance name part
				String newHostname = host.substring(0, pos);
				
				_logger.info("This looks like a Windows (SQL-Server hostname/instance) name. The instance part has been stripped out. originHostName='" + host + "', strippedHostName='" + newHostname + "'.");
				host = newHostname;
			}
		}

		_hostname = host; 
	}

	public String getUsername() { return _username; }
	public String getPassword() { return _password; }
	public String getHost()     { return _hostname; }
	public int    getPort()     { return _port; }
	public String getKeyFile()  { return _keyFile; }

	public WaitForExecDialog getWaitForDialog()                       { return _waitforDialog; }
	public void              setWaitForDialog(WaitForExecDialog wait) { _waitforDialog = wait; }
	public boolean           hasWaitForDialog()                       { return _waitforDialog != null; }

//	public static Component         getGuiOwner()                       { return _guiOwner; }
//	public static void              setGuiOwner(Component guiHandle)    { _guiOwner = guiHandle; }
//	public static boolean           hasGuiOwner()                       { return _guiOwner != null; }
	public Component         getGuiOwner()                       { return _guiOwner; }
	public void              setGuiOwner(Component guiHandle)    { _guiOwner = guiHandle; }
	public boolean           hasGuiOwner()                       { return _guiOwner != null; }

	private void logInfoMsg(String logMsg)
	{
		_logger.info(logMsg);
		if (_waitforDialog != null)
			_waitforDialog.setState(logMsg);
	}
	
	protected Session getConnection()
	{
		return _conn;
	}

	/**
	 * Connect to a remote host
	 * @return true if we succeeded to connect.
	 * @throws IOException if we failed to authenticate
	 */
	public boolean connect()
//	throws IOException
	throws Exception
	{
		// Exit early if we do not have enough information 
		if (StringUtil.isNullOrBlank(_username)) 
			throw new IllegalArgumentException("Trying to connect to a SSH host, but 'username' fields is net yet given.");

		if (StringUtil.isNullOrBlank(_hostname)) 
			throw new IllegalArgumentException("Trying to connect to a SSH host, but 'hostname' fields is net yet given.");

		if (StringUtil.isNullOrBlank(_password) && StringUtil.isNullOrBlank(_keyFile)) 
			throw new IllegalArgumentException("Trying to connect to a SSH host, but 'password' or 'sshKeyFile' fields is net yet given.");

		// Create a connection instance if none exists.
		if (_conn == null)
		{
//System.out.println(">>> SSH _jsch.getSession");
			_conn = _jsch.getSession(_username, _hostname, _port);
			_conn.setUserInfo( new DbxUserInfo() );

			if (PROMPT_FOR_PASSWORD.equals(_password))
				_password = "";

			_conn.setPassword(_password);
		}


		// If no GUI, allow "unknown" hosts...
		if (GraphicsEnvironment.isHeadless() || !_defaultOpenSshKkownHostsFileExists) 
		{
			_logger.info("Can't find GUI, setting SSH Option 'StrictHostKeyChecking=no'.");
			_conn.setConfig("StrictHostKeyChecking", "no");
		}
		
		
		
		if (_waitforDialog != null)
			_waitforDialog.setState("SSH Connectiong to host '" + _hostname + "' on port " + _port + " with username '" + _username + "' and keyFile '" + _keyFile + "'.");

		try
		{
System.out.println(">>> SSH Connect");
			_conn.connect();

			_isAuthenticated = true;
			
			// Try to get what OS we connected to
			getOsInfo();

			// Try to get number of procs (scheduble units on this os)
			getNproc();

			_logger.info("Just Connected to SSH host '" + _hostname + "' which has '" + getOsName() + "' as it's Operating System (nproc=" + _nproc + "). My guess is that it's using character set '" + getOsCharset() + "'.");

			return true;
		}
		catch (JSchException ex)
		{
			_logger.error("Problems Connection to host '" + _hostname + "' on port " + _port + " with username '" + _username + "' and keyFile '" + _keyFile + "'. Caught: " + ex);
			return false;
		}
	}

	
	/**
	 * Add a key file 
	 * 
	 * @param filename                  Name of the file
	 * @param passphrase                Passphrase for the file
	 * @param throwIfFileDoNotExist     If file do NOT exist, throw RuntimeException
	 * 
	 * @return true if file was added, false otherwise.
	 * @throws RuntimeException if throwIfFileDoNotExist and the file do not exist
	 */
	private boolean addKeyFile(String filename, String passphrase, boolean throwIfFileDoNotExist)
	{
		if (StringUtil.hasValue(filename))
		{
			File f = new File(filename);
			if ( ! f.exists() )
			{
				if (throwIfFileDoNotExist)
					throw new RuntimeException("The SSH Key File '" + f + "' did NOT exists.");
				else
					_logger.info("The SSH Key File '" + f + "' did NOT exists.");
				
				return false;
			}
			else
			{
				try
				{
					_jsch.addIdentity(f.getAbsolutePath(), passphrase);
					_logger.info("Adding SSH Key File '" + f + "'.");

					return true;
				}
				catch (JSchException ex)
				{
					_logger.error("Problems adding SSH Key File '" + f + "'.", ex);
				}
			}
		}

		return false;
	}
	
	

//	/**
//	 * Creates a new LocalPortForwarder. <br>
//	 * A LocalPortForwarder forwards TCP/IP connections that arrive at a local port via the 
//	 * secure tunnel to another host (which may or may not be identical to the remote SSH-2 server). 
//	 * <p>
//	 * This method must only be called after one has passed successfully the authentication step. 
//	 * There is no limit on the number of concurrent forwarding.
//	 * 
//	 * @param sshTunnelInfo
//	 * @return LocalPortForwarder
//	 * @throws IOException 
//	 */
//	public LocalPortForwarder createLocalPortForwarder(SshTunnelInfo sshTunnelInfo)
//	throws IOException
//	{
//		int    localPort = sshTunnelInfo.getLocalPort();
//		String destHost = sshTunnelInfo.getDestHost();
//		int    destPort = sshTunnelInfo.getDestPort();
//
//		if (localPort < 0 && sshTunnelInfo.isLocalPortGenerated() )
//		{
////			localPort = sshTunnelInfo.generateLocalPort();
//			localPort = SshTunnelManager.generateLocalPort();
//			
//			sshTunnelInfo.setLocalPort(localPort);
//		}
//
//		try
//		{
//			_logger.info("Creating a Local Port Forwarder/Tunnel from Local port '" + localPort + "' to Destination host '" + destHost + "', port '" + destPort + "'.");
//			return _conn.createLocalPortForwarder(localPort, destHost, destPort);
//		}
//		catch (IOException e)
//		{
//			_logger.info("Problems ,creating a Local Port Forwarder/Tunnel from Local port '" + localPort + "' to Destination host '" + destHost + "', port '" + destPort + "'. Caught: " + e, e);
//			throw e;
//		}
//	}
//	/**
//	 * Method to forward a local port to a remote host and port number.
//	 * 
//	 * <p>
//	 * This method is a convenience method that is meaningful only for remote server
//	 * sessions. This method uses SSH port forwarding support in JSch to forward
//	 * connections from a local port to a given remote host and port.
//	 * </p>
//	 * 
//	 * @param localPort  The local port on the local machine to be forwarded to a
//	 *                   given remote host. If this value is -1, then a free port is
//	 *                   detected by this method and used.
//	 * 
//	 * @param remoteHost The host or IP address of the remote machine to which a
//	 *                   connection is to be forwarded.
//	 * 
//	 * @param remotePort The remote port number to which the connection is to be
//	 *                   forwarded.
//	 * 
//	 * @return This method returns the local port that has been forwarded.
//	 * @throws IOException
//	 */
//	@Override
//	public int forwardPort(int localPort, String remoteHost, int remotePort) throws IOException 
//	{
//		try 
//		{
//			if (localPort == -1) 
//			{
//				// Find out a local socket that is free.
//				ServerSocket tempSocket = new ServerSocket(0);
//				localPort = tempSocket.getLocalPort();
//				tempSocket.close();
//			}
//			// Get JSch to forward the port for us.
//			return session.setPortForwardingL(localPort, remoteHost, remotePort);
//		} 
//		catch (Exception e) 
//		{
//			ProgrammerLog.log(e);
//			UserLog.log(LogLevel.WARNING, "RemoteServerSession", e.getMessage());
//			throw new IOException(e);
//		}
//	}	
	/**
	 * Creates a new LocalPortForwarder. <br>
	 * A LocalPortForwarder forwards TCP/IP connections that arrive at a local port via the 
	 * secure tunnel to another host (which may or may not be identical to the remote SSH-2 server). 
	 * <p>
	 * This method must only be called after one has passed successfully the authentication step. 
	 * There is no limit on the number of concurrent forwarding.
	 * 
	 * @param sshTunnelInfo
	 * @return int
	 * @throws IOException 
	 */
	public int createLocalPortForwarder(SshTunnelInfo sshTunnelInfo)
	throws Exception
	{
		int    localPort = sshTunnelInfo.getLocalPort();
		String destHost = sshTunnelInfo.getDestHost();
		int    destPort = sshTunnelInfo.getDestPort();

		if (localPort < 0 && sshTunnelInfo.isLocalPortGenerated() )
		{
			localPort = SshTunnelManager2.generateLocalPort();
			
			sshTunnelInfo.setLocalPort(localPort);
		}

		try
		{
			_logger.info("Creating a Local Port Forwarder/Tunnel from Local port '" + localPort + "' to Destination host '" + destHost + "', port '" + destPort + "'.");

			//return _conn.createLocalPortForwarder(localPort, destHost, destPort);

			// Get JSch to forward the port for us.
			return _conn.setPortForwardingL(localPort, destHost, destPort);
		}
		catch (JSchException e)
		{
			_logger.info("Problems ,creating a Local Port Forwarder/Tunnel from Local port '" + localPort + "' to Destination host '" + destHost + "', port '" + destPort + "'. Caught: " + e, e);
			throw e;
		}
	}

	/**
	 * Close the local port forwarder
	 * 
	 * @param localPort
	 * @throws Exception
	 */
	public void closeLocalPortForwarder(int localPort)
	throws Exception
	{
		_conn.delPortForwardingL(localPort);
	}


	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	public boolean reconnect()
	throws Exception
	{
		if (_conn == null)
			throw new IOException("Can't do reconnect yet, you need to have a valid connection object first.");

		if (_isAuthenticated == false)
			throw new IOException("Can't do reconnect yet, you need to have a valid connection first. This means that you need to connect with a successful authentication first.");

		if (_logger.isDebugEnabled())
			_logger.debug("Closing the connection to SSH host '" + _hostname + "' on port '" + _port + "' with user '" + _username + "'.");

		_conn.disconnect();
			
		// If we reuse the _conn (Session object) we get: "SSH FATAL: Bad packet length 1044"
		// So lets null it and create a new one
		_conn = null; 

		_logger.info("Trying to reconnect to SSH host '" + _hostname + "' on port '" + _port + "' with user '" + _username + "'.");
		return connect();
	}

	/**
	 * Close the connection to the remote host
	 */
	public void close()
	{
		if (_conn != null)
		{
			_logger.debug("Closing the connection to SSH host '" + _hostname + "' on port '" + _port + "' with user '" + _username + "'.");
//			_conn.close();
			_conn.disconnect();
		}
		_conn = null;
//		_isConnected = false;
	}

	/**
	 * Check if you are connected to a remote host 
	 * @return true if connected, false if not connected
	 */
	public boolean isConnected()
	{
//		return isConnected(false);
		
		if (_conn == null)
		{
			return false;
		}

		return _conn.isConnected();
	}


//	/**
//	 * Check if you are connected to a remote host
//	 *  
//	 * @param reConnectOnProblems    if the connection has been authenticated (previously logged in) and the connection has been lost, then try to reconnect.
//	 *  
//	 * @return true if connected, false if not connected
//	 */
//	public boolean isConnected(boolean reConnectOnProblems)
//	{
//		if (_conn == null)
//		{
//			return false;
//		}
//
//		if ( ! _conn.isConnected() )
//		{
//			if (_isAuthenticated && reConnectOnProblems)
//			{
//				try
//				{
//					reconnect();
//				}
//				catch(Exception ex)
//				{
//					_logger.warn("isConnected() has problems when trying to re-connect. Caught: " + ex);
//					// Not sure what to do here... leave the connection or close the connection
//					// reconnect() does a close... before it tries to do connect(). hopefully thats good enough...
//				}
//			}
//			else
//			{
//				return false;
//			//	throw new IOException("SSH is not connected. (host='" + _hostname + "', port=" + _port + ", user='" + _username + "', osName='" + _osName + "', osCharset='" + _osCharset + "'.)");
//			}
//		}
//
//		try 
//		{
//			_logger.debug("isConnected(): SEND IGNORE/KEEPALIVE Message to SSH Server.");
//		//	_conn.sendIgnore();
//			_conn.sendKeepAliveMsg();
//		}
//		catch (Exception e) 
//		{
//			if (_isAuthenticated && reConnectOnProblems)
//			{
//				_logger.warn("isConnected() has problems when sending a 'ignore packet' to the SSH Server. Lets try to re-connect to the server. sendIgnorePacket Caught: " + e);
//				try 
//				{
//					reconnect();
//				}
//				catch(Exception ex)
//				{
//					_logger.warn("isConnected() has problems when trying to re-connect. Caught: " + ex);
//					// Not sure what to do here... leave the connection or close the connection
//					// reconnect() does a close... before it tries to do connect(). hopefully thats good enough...
//				}
//			}
//		}
//
//		if (_conn != null)
//			return _conn.isConnected();
//
//		return false;
//	}

	/**
	 * INTERAL: Check if the connections is OK, or try to reconnect if we once have done authenticated
	 * @return true on OK
	 * @throws Exception if we had problems re-connecting or that we are not authenticated 
	 */
	private boolean checkConnectionAndPossiblyReconnect()
	throws Exception
	{
		if (_conn == null)
		{
			throw new IOException("SSH is not connected. (host='" + _hostname + "', port=" + _port + ", user='" + _username + "', osName='" + _osName + "', osCharset='" + _osCharset + "', isAuthenticated=" + _isAuthenticated + ".)");
			//return false;
		}

System.out.println(">>>>>>>>>>>>>>> _conn.isConnected() == " + _conn.isConnected());
		if ( ! _conn.isConnected() )
		{
			if (_isAuthenticated)
			{
				_logger.info("Lost the connection to SSH host '" + _hostname + "' on port '" + _port + "' with user '" + _username + "'.");
				try 
				{
					reconnect();
				}
				catch(Exception ex)
				{
					_logger.warn("isConnected() has problems when trying to re-connect. Caught: " + ex);
					// Not sure what to do here... leave the connection or close the connection
					// reconnect() does a close... before it tries to do connect(). hopefully thats good enough...
				}
			}
			else
			{
				throw new IOException("SSH is not connected. (host='" + _hostname + "', port=" + _port + ", user='" + _username + "', osName='" + _osName + "', osCharset='" + _osCharset + "'.)");
			}
		}

		try 
		{
			_logger.debug("isConnected(): SEND IGNORE/KEEPALIVE Message to SSH Server.");
//			_conn.sendIgnore();
			_conn.sendKeepAliveMsg();
		}
		catch (Exception e) 
		{
			if (_isAuthenticated)
			{
				_logger.warn("checkConnectionAndPossiblyReconnect() has problems when sending a 'keepalive/ignore packet' to the SSH Server. Lets try to re-connect to the server. sendKeepAliveMsg Caught: " + e);
				reconnect();
			}
		}

		if (_conn != null)
			return _conn.isConnected();

		return false;
	}
	
	
	/**
	 * Check if the SshConnection is closed to the remote host 
	 * @return true if closed, false if connected
	 */
	public boolean isClosed()
	{
		return ! isConnected();
	}

//	/** 
//	 * Open a session where a command can be executed. {@link ch.ethz.ssh2.Session}
//	 * @see Session 
//	 */ 
//	public Session openSession() 
//	throws IOException
//	{
//		return _conn.openSession();
//	}


	
	/**
	 * Execute a Operating System Command on the remote host
	 * <p>
	 * If the connection has been closed, a new one will be attempted.
	 * <p>
	 * Note: This is synchronized because if several execute it simultaneously and we have 
	 * lost the connection and make a reconnect attempt, it's likely to fail with 'is already in connected state!' or 'IllegalStateException: Cannot open session, you need to establish a connection first.'  or similar errors.
	 * 
	 * @param command The OS Command to be executed
	 * @return a String, which the command produced
	 * @throws IOException if return code from the command != 0
	 */
	synchronized public ExecOutput execCommandOutput(String command) 
	throws Exception
	{
		// Check if the connection has been closed (not connected anymore)
		if ( ! isConnected() )
		{
			if (_isAuthenticated)
				reconnect();
			else
				throw new IOException("SSH is not connected. (host='" + _hostname + "', port=" + _port + ", user='" + _username + "', osName='" + _osName + "', osCharset='" + _osCharset + "'.)");
		}

		ChannelExec channel = (ChannelExec) _conn.openChannel("exec");
		
		// Setup the command for execution on remote machine.
		channel.setCommand(command);

		// Process the output streams. The following output stream buffers the data from
		// standard error (on a different thread) while we read standard output in this thread.
		ByteArrayOutputStream stderr = new ByteArrayOutputStream(8192);
		channel.setErrStream(stderr);

		// Read stdout one line at a time and add it to the output
		BufferedReader stdin = new BufferedReader(new InputStreamReader(channel.getInputStream()));

		// Now run the command on the remote server
//		channel.connect(60000);
		channel.connect();

		ExecOutput output = new ExecOutput(command);
		
		StringBuilder sb = new StringBuilder();
		
		String line = null;
		while ((line = stdin.readLine()) != null) 
		{
			// Got another line of standard output. Display it.
//			output.insertString(output.getLength(), line + "\n", output.getStyle("stdout"));
			sb.append(line).append("\n");
		}

		// Flush out any pending data on the standard error stream
		String stdErrData = stderr.toString();
//		output.insertString(output.getLength(), stdErrData, output.getStyle("stderr"));

		// Save exit status.
		output.setExitCode(channel.getExitStatus());

		output.setStdOut(StringUtil.trim(sb.toString()));
		output.setStdErr(StringUtil.trim(stdErrData));

		channel.disconnect();
		
		return output;
	}
	
	public static class ExecOutput
	{
		private String _cmd = null;

		private String _stdout = null;
		private String _stderr = null;
		private int    _exitCode = -1;


		public ExecOutput()
		{
		}

		public ExecOutput(String command)
		{
			_cmd = command;
		}

		@Override
		public String toString()
		{
			return "command=|" + _cmd + "|, exitCode=" + _exitCode + ", stdout=|" + _stdout + "|, stderr=|" + _stderr + "|.";
		}

		public boolean hasValueStdOut() { return StringUtil.hasValue(_stdout); }
		public boolean hasValueStdErr() { return StringUtil.hasValue(_stderr); }

		public String  getStdOut() { return _stdout == null ? "" : _stdout; }
		public String  getStdErr() { return _stderr == null ? "" : _stderr; }

		public int     getExitCode()    { return _exitCode; }
		
		public void    setStdOut(String str) { _stdout   = str; }
		public void    setStdErr(String str) { _stderr   = str; }
		public void    setExitCode(int rc)   { _exitCode = rc; }

		public boolean containsStdOut(String value)
		{
			if (_stdout == null)
				return false;
			
			return _stdout.contains(value);
		}

		public boolean containsStdErr(String value)
		{
			if (_stderr == null)
				return false;
			
			return _stderr.contains(value);
		}
	}
	
	/**
	 * Execute a Operating System Command on the remote host
	 * <p>
	 * If the connection has been closed, a new one will be attempted.
	 * <p>
	 * Note: This is synchronized because if several execute it simultaneously and we have 
	 * lost the connection and make a reconnect attempt, it's likely to fail with 'is already in connected state!' or 'IllegalStateException: Cannot open session, you need to establish a connection first.'  or similar errors.
	 * 
	 * @param command The OS Command to be executed
	 * @return a String, which the command produced
	 * @throws IOException if return code from the command != 0
	 */
	synchronized public String execCommandOutputAsStr(String command) 
	throws Exception
	{
		// Check if connection is OK.. if NOT yet connected an Exception will be thrown
		checkConnectionAndPossiblyReconnect();
		
		// EXECUTE
		ExecOutput output = execCommandOutput(command);
//System.out.println("execCommandOutputAsStr(): " + output);

		if (output.hasValueStdOut())
			return output.getStdOut();

		if (output.hasValueStdErr())
		{
			_logger.info("Executing command '" + command + "', had issues. EXIT-CODE=" + output.getExitCode() + ", STDERR=|" + output.getStdErr() + "|.");
			return "";
		}

		if (output.getExitCode() != 0)
		{
			_logger.info("Executing command '" + command + "', had issues. EXIT-CODE=" + output.getExitCode() + ", no output on STDOUT or STDERR.");
			return "";
		}

		return "";
	}

	/**
	 * Execute a Operating System Command on the remote host
	 * <p>
	 * If the connection has been closed, a new one will be attempted.
	 * <p>
	 * Note: This is synchronized because if several execute it simultaneously and we have 
	 * lost the connection and make a reconnect attempt, it's likely to fail with 'is already in connected state!' or 'IllegalStateException: Cannot open session, you need to establish a connection first.'  or similar errors.
	 * 
	 * @param command The OS Command to be executed
	 * @return a Session object, which you can read stdout and stderr on
	 * @throws IOException 
	 * @see Session
	 */
	synchronized public ChannelExec execCommand(String command) 
	throws Exception
	{
		return execCommand(command, false);
	}

	/**
	 * Execute a Operating System Command on the remote host
	 * <p>
	 * If the connection has been closed, a new one will be attempted.
	 * <p>
	 * Note: This is synchronized because if several execute it simultaneously and we have 
	 * lost the connection and make a reconnect attempt, it's likely to fail with 'is already in connected state!' or 'IllegalStateException: Cannot open session, you need to establish a connection first.'  or similar errors.
	 * 
	 * @param command       The OS Command to be executed
	 * @param requestPty    Request a "teminal" from where the command is executed on
	 * @return a Session object, which you can read stdout and stderr on
	 * @throws IOException 
	 * @see Session
	 */
	synchronized public ChannelExec execCommand(String command, boolean requestPty) 
	throws Exception
	{
		// Check if connection is OK.. if NOT yet connected an Exception will be thrown
		checkConnectionAndPossiblyReconnect();

		try
		{
			ChannelExec channel = (ChannelExec) _conn.openChannel("exec");
			
			// Setup the command for execution on remote machine.
			channel.setCommand(command);
			
			// Now run the command on the remote server
//			channel.connect(60000);
			channel.connect();

			//TODO: Handle Exceptions for reconnect (as below code does)
			return channel;
		}
		catch (Exception ex)
		{
ex.printStackTrace();
			throw ex;
		}
		
//		try
//		{
//			Session sess = _conn.openSession();
//			_logger.debug("Executing command '" + command + "' on connection: " + toString());
//			
//			// SSHD On Windows do not close "long running" commands on the server side on disconnect
//			// see: https://github.com/PowerShell/Win32-OpenSSH/issues/1751
//			// The workaround seems to be "ssh -t user@ip" where the "-t" is to request a Terminal
//			if (requestPty)
//				sess.requestDumbPTY();
//			
//			// Now execute the command
//			sess.execCommand(command);
//
//			return sess;
//		}
//		catch (IllegalStateException e)
//		{
//			// This is thrown in openSession, and it's a RuntimeException
//			// IllegalStateException("Cannot open session, you need to establish a connection first.");
//			// IllegalStateException("Cannot open session, connection is not authenticated.");
//			// SO should we re-connect or just leave it?
//			// lets try use same logic as below
//			if (_isAuthenticated)
//			{
//				_logger.info("The Connection to SSH host '" + _hostname + "' on port '" + _port + "' seems to be lost/closed. I will try reconnect and, execute the command '" + command + "' again. Caught: " + e);
//
//				reconnect();
//				Session sess = _conn.openSession();
//				_logger.info("Re-Executing command '" + command + "' after the reconnect.");
//				sess.execCommand(command);
//				return sess;
//			}
//			// if we can't handle the Exception, throw it
//			throw e;
//		}
//		catch (IOException e)
//		{
//			// If this is a "lost" connection try to "reconnect"
//			if (e.getMessage().indexOf("connection is closed") >= 0)
//			{
//				// if we already has been authenticated once, then try to reconnect again
//				if (_isAuthenticated)
//				{
//					_logger.info("The Connection to SSH host '" + _hostname + "' on port '" + _port + "' seems to be lost/closed. I will try reconnect and, execute the command '" + command + "' again.");
//
//					reconnect();
//					Session sess = _conn.openSession();
//					_logger.info("Re-Executing command '" + command + "' after the reconnect.");
//					sess.execCommand(command);
//					return sess;
//				}
//			}
//			// if we can't handle the Exception, throw it
//			throw e;
//		}
	}

	/**
	 * Class used by {@link #execCommand(String command, ExecutionFeedback execFeedback)} to simplify streaming or long running command to handle output
	 */
	public static interface IExecutionFeedback
	{
		/** 
		 * Called before waiting for data, so caller can choose to abort
		 * @return true = Continue to receive data, false = Abort receiving data 
		 */
		default public boolean doContinue()
		{
			return true;
		}

		/**
		 * Called when a row is received from the command executed
		 * 
		 * @param type    1=STDOUT, 2=STDERR
		 * @param row     The row received
		 */
		void onData(int type, String row);

		/**
		 * Called at the end to let you know the return code of the command
		 * @param exitCode
		 */
		void onExitCode(int exitCode);
	}

	/**
	 * Class used by {@link #execCommand(String command, ExecutionFeedback execFeedback)} to simplify streaming or long running command to handle output
	 */
	public static abstract class ExecutionFeedback
	implements IExecutionFeedback
	{
		private int _exitCode = -1;

		@Override
		public abstract void onData(int type, String row);

		@Override
		public void onExitCode(int exitCode)
		{
			_exitCode = exitCode;
		}

		/**
		 * Get the exit code, set by any callers of {@link #onExitCode(int)}
		 * @return exitCode
		 */
		public int getExitCode()
		{
			return _exitCode;
		}
	}

	/**
	 * Execute a Operating System Command on the remote host
	 * <p>
	 * If the connection has been closed, a new one will be attempted.
	 * <p>
	 * Note: This is synchronized because if several execute it simultaneously and we have 
	 * lost the connection and make a reconnect attempt, it's likely to fail with 'is already in connected state!' or 'IllegalStateException: Cannot open session, you need to establish a connection first.'  or similar errors.
	 * 
	 * @param command         The OS Command to be executed
	 * @param execFeedback    An ExecutionFeedback to handle received data
	 * 
	 * @return ExitCode of the passed command
	 * @throws Exception    On problems
	 */
	synchronized public int execCommand(String command, IExecutionFeedback execFeedback) 
	throws Exception
	{
		// Check if connection is OK.. if NOT yet connected an Exception will be thrown
		checkConnectionAndPossiblyReconnect();


		ChannelExec channel = (ChannelExec) _conn.openChannel("exec");
		
		// Setup the command for execution on remote machine.
		channel.setCommand(command);

		// Now run the command on the remote server
		channel.connect();

		// Get the CharSet of the OS
		Charset osCharset = Charset.forName(getOsCharset());

		// Get the input streams from the SSH channel
		InputStream stdout = channel.getInputStream();
		InputStream stderr = channel.getErrStream();
		
		// Read the streams row-by-row 
		BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(stdout, osCharset));
		BufferedReader stderrReader = new BufferedReader(new InputStreamReader(stderr, osCharset));

		// Setup how to sleep
		int sleepCount          = 0;
		int sleepTimeMultiplier = 3; 
		int sleepTimeMax        = 250;

		boolean running = true;

		// Receive data, until end-of-data or doContinue() is false
		while(running)
		{
//System.out.println("--TOP: running=|"+running+"|");
			try
			{
				// Check if we want to abort or continue to receive data
				running = execFeedback.doContinue();
				
				// If we have NOT got any data... Sleep for a while
				if ((stdout.available() == 0) && (stderr.available() == 0))
				{
					if (running)
					{
						try
						{
    						sleepCount++;
    						int sleepMs = Math.min(sleepCount * sleepTimeMultiplier, sleepTimeMax);;
    
    						if (_logger.isDebugEnabled())
    							_logger.debug("waitForData(), sleep(" + sleepMs + "). command=" + command);
    
    						Thread.sleep(sleepMs);
						}
						catch (InterruptedException e)
						{
							running = false;
						}
					}
					
					if ( channel.isClosed() || channel.isEOF())
					{
						if (stdout.available() > 0 || stderr.available() > 0)
							continue;
						break;
					}
				}

				// Read STDOUT, on data do callback
				while (stdout.available() > 0)
				{
					if (_logger.isDebugEnabled())
					{
//						startTs = System.currentTimeMillis();
						_logger.debug("SSH-STDOUT[" + command + "][available=" + stdout.available() + "]: -start-");
					}
					
					// NOW READ input
					while (stdoutReader.ready())
					{
//System.out.println("--STD-OUT: before readLine();");
						String row = stdoutReader.readLine();
//System.out.println("--STD-OUT:  after readLine();row=|"+row+"|");

						// discard empty rows
						if (StringUtil.isNullOrBlank(row))
							continue;

						if (_logger.isDebugEnabled())
							_logger.debug("Received on STDOUT: "+row);

						// do callback
						execFeedback.onData(STDOUT_DATA, row);
					}
				}

				// Read STDERR, on data do callback
				while (stderr.available() > 0)
				{
					if (_logger.isDebugEnabled())
					{
						_logger.debug("SSH-STDERR[" + command + "][available=" + stdout.available() + "]: -start-");
					}
					
					// NOW READ input
					while (stderrReader.ready())
					{
//System.out.println("--STD-ERR: before readLine();");
						String row = stderrReader.readLine();
//System.out.println("--STD-ERR:  after readLine();row=|"+row+"|");

						// discard empty rows
						if (StringUtil.isNullOrBlank(row))
							continue;
						
						if (_logger.isDebugEnabled())
							_logger.debug("Received on STDERR: "+row);

						// do callback
						execFeedback.onData(STDERR_DATA, row);
					}
				}
			}
			catch(Exception ex)
			{
				_logger.error("Problems when reading output from the OS Command '" + command + "', Caught: " + ex.getMessage(), ex);
//				fixme;
			}
		}
		
		int exitCode = channel.getExitStatus();
		execFeedback.onExitCode(exitCode);

		// if we did return false on 'doContinue()', is the command terminated at the backend?
		
		channel.disconnect();
		
		return exitCode;
	}
	
	/**
	 * Get the Character Set this operating system is using.
	 * <p>
	 * For the moment this is hardcoded based on the Operating System
	 * <ul>
	 * <li>Linux: UTF-8
	 * <li>SunOS: ISO-8859-1
	 * <li>AIX:   ISO-8859-1
	 * <li>HP-UX: ISO-8859-1
	 * 
	 */
	public String getOsCharset()
	{
		return _osCharset;
	}

	/**
	 * Get the name of the Operating System, this is first work of 'uname -a'
	 */
	public String getOsName()
	{
		return _osName;
	}

	/**
	 * execute 'nproc' on the OS and return the result
	 * 
	 * @return The value of 'nproc'.  0 = failure to execute nproc
	 */
	public int getNproc()
	{
		if (_nproc != -1)
			return _nproc;

		String cmd = "nproc";
		String str = "-empty-";
		
		if (_uname != null)
		{
			if (_uname.equals("Windows-CMD")) // DOS Prompt
				cmd = "echo %NUMBER_OF_PROCESSORS%";

			if (_uname.startsWith("Windows-Powershell-")) // Powershell (any kind)
				cmd = "echo $env:NUMBER_OF_PROCESSORS";
		}

		try
		{
			if (_conn == null)
			{
				throw new IOException("The SSH connection to the host '" + _hostname + "' was null. The connection has not been initialized OR someone has closed the connection.");
			}

			// EXECUTE
			String output = execCommandOutputAsStr(cmd);

			str = output;
			_nproc = StringUtil.parseInt(str, 0);
		}
		catch (Exception e)
		{
			_nproc = 0;
			_logger.info("Problems executing command '" + cmd + "'. retStr='" + str + "', Caught: " + e);
		}
		return _nproc;
	}

	/**
	 * simply does 'uname -a' and return the string.
	 */
	public String getOsInfo()
	throws Exception
	{
		if (_uname != null)
			return _uname;

		if (_conn == null)
		{
			throw new IOException("The SSH connection to the host '" + _hostname + "' was null. The connection has not been initialized OR someone has closed the connection.");
		}

		// EXECUTE
		String output = execCommandOutputAsStr("uname");
		
		// well we might end up in a Windows SSH ... so lets check if it's CMD or POWERSHELL we ended up in, when loggin on
		if (StringUtil.isNullOrBlank(output))
		{
			// https://stackoverflow.com/questions/34471956/how-to-determine-if-im-in-powershell-or-cmd
			// (dir 2>&1 *`|echo CMD);&<# rem #>echo PowerShell
			// (dir 2>&1 *`|echo CMD);&<# rem #>echo ($PSVersionTable).PSEdition
			
			// the below returns: 
			// 'CMD'     - For DOS Command promt environment
			// 'Core'    - For Powershell 'core' implementation
			// 'Desktop' - For Powershell 'Desktop' implementation, which is the "full blown Windows version"

			// EXECUTE
			output = execCommandOutputAsStr("(dir 2>&1 *`|echo CMD);&<# rem #>echo ($PSVersionTable).PSEdition");

			if (StringUtil.hasValue(output))
			{
				if (output.equals("Core")   ) output = "Powershell-Core";
				if (output.equals("Desktop")) output = "Powershell-Desktop";

				output = "Windows-" + output;
			}
			
		}
		
		_uname = output;
		if (_uname != null)
		{
			String[] sa = _uname.split(" ");
			if (sa.length > 0)
				_osName = StringUtil.trim(sa[0]);

			// TODO:
			// on Linux you it might be available using: 'locale charmap' -- returned 'UTF-8'
			
			// also try to figure out a dummy default character set for the OS
//			if      (_osName.equals    ("Linux"   )) _osCharset = "UTF-8";
			if      (_osName.equals    ("Linux"   )) _osCharset = linuxToJavaCharset();
			else if (_osName.equals    ("SunOS"   )) _osCharset = "ISO-8859-1";
			else if (_osName.equals    ("AIX"     )) _osCharset = "ISO-8859-1"; // TODO: CHECK
			else if (_osName.equals    ("HP-UX"   )) _osCharset = "ISO-8859-1"; // TODO: CHECK
			else if (_osName.startsWith("Windows-")) _osCharset = windowsToJavaCharset();
			else _osCharset = null;

			//Charset.forName("ISO-8859-1");
		}
		_logger.debug("getOsInfo: osName='" + _uname + "', chartset='" + _osCharset + "'.");
		//System.out.println("getOsInfo: osName='" + _uname + "', chartset='" + _osCharset + "'.");

		return output;
	}

	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	private String linuxToJavaCharset()
	throws Exception
	{
		if (_conn == null)
		{
			throw new IOException("The SSH connection to the host '" + _hostname + "' was null. The connection has not been initialized OR someone has closed the connection.");
		}

		// EXECUTE
		String output = execCommandOutputAsStr("echo ${LC_ALL:-${LC_CTYPE:-${LANG}}}");

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
				output = StringUtil.trim(sa[1]); // echo ${LC_ALL:-${LC_CTYPE:-${LANG}}} -->>> 'en_US.UTF-8'
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
	 * 
	 * @return
	 * @throws IOException
	 */
	private String windowsToJavaCharset()
	throws Exception
	{
		if (_conn == null)
		{
			throw new IOException("The SSH connection to the host '" + _hostname + "' was null. The connection has not been initialized OR someone has closed the connection.");
		}

		// EXECUTE
		String output = execCommandOutputAsStr("chcp");

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

	/**
	 * Simply check if a file name exists in the remote server
	 * @param filename to check if it exists
	 */
	public boolean doFileExist(String filename)
	{
		try
		{
			// Check if connection is OK.. if NOT yet connected an Exception will be thrown
			checkConnectionAndPossiblyReconnect();

			// EXECUTE
			String output = execCommandOutputAsStr("ls -Fal '" + filename + "'");
			
			_logger.debug("doFileExist: '" + filename + "' produced '" + output + "'.");

			if ( output.startsWith("-") )
				return true;
			return false;
		}
		catch (Exception e)
		{
			if ( ! isConnected() )
				throw new RuntimeException("SSH is not connected. (host='" + _hostname + "', port=" + _port + ", user='" + _username + "', osName='" + _osName + "', osCharset='" + _osCharset + "'.)");

			_logger.error("doFileExist() caught: " + e, e);
			return false;
		}
	}

	/**
	 * Create a file
	 * @param filename to create
	 */
	public boolean createNewFile(String filename)
	throws Exception
	{
		// Check if connection is OK.. if NOT yet connected an Exception will be thrown
		checkConnectionAndPossiblyReconnect();

		// EXECUTE
		String osCmd = "touch '" + filename + "'";
		ExecOutput output = execCommandOutput(osCmd);

		_logger.debug("createNewFile: '" + filename + "' produced '" + output + "'.");

		if ( ! output.hasValueStdOut() )
			throw new IOException("createNewFile('" + filename + "') produced output, which wasn't expected. Output: " + output);

		if (output.getExitCode() != 0)
			throw new IOException("createNewFile('" + filename + "') return code not zero. Output: " + output);

		return true;
	}

	/**
	 * Remove a file
	 * @param filename to remove
	 */
	public boolean removeFile(String filename)
	throws Exception
	{
		// Check if connection is OK.. if NOT yet connected an Exception will be thrown
		checkConnectionAndPossiblyReconnect();

		// EXECUTE
		String osCmd = "rm -f '" + filename + "'";
		ExecOutput output = execCommandOutput(osCmd);

		_logger.debug("removeFile: '" + filename + "' produced '" + output + "'.");

		if ( ! output.hasValueStdOut() )
			throw new IOException("removeFile('" + filename + "') produced output, which wasn't expected. Output: " + output);

		if (output.getExitCode() != 0)
			throw new IOException("removeFile('" + filename + "') return code not zero. Output: " + output);

		return true;
	}

	/**
	 * Check if the Veritas 'vxstat' is executable and in the current path
	 * 
	 * @return true if Veritas commands are available (in the path)
	 * @throws IOException
	 */
	public boolean hasVeritas()
	throws Exception
	{
		// Check if connection is OK.. if NOT yet connected an Exception will be thrown
		checkConnectionAndPossiblyReconnect();

		// EXECUTE
		String osCmd = "vxstat";
		ExecOutput output = execCommandOutput(osCmd);

		_logger.debug("hasVeritas(): produced '" + output + "'.");

		boolean hasVeritasMsg = false;
		
		if (output.containsStdOut("VxVM vxstat ERROR"))
			hasVeritasMsg = true;

		if (output.containsStdErr("VxVM vxstat ERROR"))
			hasVeritasMsg = true;
		
		
		if ( ! output.hasValueStdOut() )
			throw new IOException("hasVeritas() produced output, which wasn't expected. Output: " + output);

		if (output.getExitCode() != 0)
			throw new IOException("hasVeritas() return code not zero. Output: " + output);

		return hasVeritasMsg;
	}

	/**
	 * Different Linux Utilities that we want to check version information for
	 */
	public enum LinuxUtilType
	{
		IOSTAT, VMSTAT, MPSTAT, UPTIME, PS
	};

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
	public int getLinuxUtilVersion(LinuxUtilType utilType)
	throws Exception
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
			throw new Exception("Unsupported utility of '" + utilType + "'.");
	
		// Check if connection is OK.. if NOT yet connected an Exception will be thrown
		checkConnectionAndPossiblyReconnect();

		// EXECUTE
		ExecOutput output = execCommandOutput(cmd);

		int intVersion = -1;
		String usedVersionString = "";
		
		int tmp = VersionShort.parse(output.getStdOut());
		if (tmp >= 0)
		{
			intVersion = tmp;
			usedVersionString = output.getStdOut();
		}
		else
		{
			tmp = VersionShort.parse(output.getStdErr());
			if (tmp >= 0)
			{
				intVersion = tmp;
				usedVersionString = output.getStdErr();
			}
		}
		
		_logger.info("When issuing command '" + cmd + "' the version " + intVersion + " was parsed from the version string '" + StringUtil.removeLastNewLine(usedVersionString) + "'.");

		if ( output.getExitCode() != 0 )
			return -1;

		_logger.debug("getLinuxUtilVersion(): returned " + intVersion );
//System.out.println("getLinuxUtilVersion(): returned " + intVersion );

		return intVersion;
	}

	@Override
	public String toString()
	{
		return 
			"host='"        + _hostname    + ":" + _port + "', " +
			"user='"        + _username    + "', " +
			"osName='"      + _osName      + "', " +
			"osCharset='"   + _osCharset   + "', " +
//			"isConnected='" + _isConnected + "'." +
			"";
	}








	/**
	 * The OS-specific path where the list of known hosts (that is the servers to
	 * which we have connected before) is stored. This list is applicable only for
	 * remote hosts.
	 */
//	private static final String KNOWN_HOST_PATH = Utilities.getDefaultDirectory() + File.separator + ".KnownHosts";
	private static final String KNOWN_HOST_PATH = System.getProperty("user.home") + File.separator + ".ssh/known_hosts";

	/**
	 * A generic informational message that is displayed to the user when any error
	 * occurs when trying to set the known hosts file to be used by JSch.
	 */
	private static final String KNOWN_HOSTS_ERROR = ""
			+ "<html>"
			+ "Error occured when attempting to use the known hosts<br>"
			+ "file: '" + KNOWN_HOST_PATH + "'.<br>"
			+ "Please rectify this issue appropriately. You can still continute<br>"
			+ "to use " + Version.getAppName() + ". But caching of known hosts for secure shell connection<br>will be disabled.<br>"
			+ "</html>";

	/**
	 * A first-time connection message that is formatted and displayed to the user.
	 * 
	 * This string contains an HTML message that is suitably formatted (to fill-in
	 * additional information) and displayed to the user. This message is used when
	 * the user connects to an Server for the first time and the server entry is not
	 * in the Known hosts file.
	 */
	private static final String FIRST_SSH_CONNECTION_MSG = ""
			+ "<html>"
			+ "The authenticity of host %s<br/>"
			+ "cannot be verified as it is not a \"known host\".<br/>The RSA fingerprint key is: %s<br/><br/>"
			+ "This is most likely because this is the first time you<br/>"
			+ "connect to this host via " + Version.getAppName() + " and this message is<br/>"
			+ "normal when connecting via secure shell (SSH) protocol.<br/><br/>"
			+ "<b>Would you like to add this server to the \"known hosts\"<br/>"
			+ "and proceed with the connection?</b>"
			+ "</html>";

	/**
	 * Message that is formatted and displayed to the user to warn about change in
	 * RSA finger print.
	 * 
	 * This string contains an HTML message that is suitably formatted (to fill-in
	 * additional information) and displayed to the user. This message is used when
	 * the user connects to an Server but the server's RSA finger print key has
	 * changed.
	 */
	private static final String SSH_HOST_CHANGE_MSG = ""
			+ "<html>"
			+ "<b>The server's identification has changed!</b><br/>"
			+ "<b>It is possible that someone is doing something nasty</b><br/>"
			+ "(someone could be eavesdropping via man-in-the-middle type attack).<br/>"
			+ "It is aslo possible that the RSA host key for the server has changed.<br/>"
			+ "<ul>"
			+ "  <li>If this is a server maintained by your company or department<br/>"
			+ "      it is normally safe to proceed with using the server.</li>"
			+ "  <li>If not please contact your server administrators to verify that<br/>"
			+ "      the change in finger print is expected prior to using the server.</li>"
			+ "</ul>"
			+ "<b>Would you like to update the server's entry in \"known hosts\"<br/>"
			+ "and proceed with the connection?</b></html>";

	/**
	 * A static message that is included as a part of the RuntimeException generated
	 * by some of the methods in this class.
	 */
	private static final String USER_INTERRUPTED_EXP_MSG = "User has interrupted SSH connection to server";


	
	
	/**
	 * Allows user interaction. The application can provide an implementation of this interface to the Session to allow 
	 * for feedback to the user and retrieving information (e.g. passwords, passphrases or a confirmation) from the user.
	 * <p>
	 * If an object of this interface also implements UIKeyboardInteractive, it can also be used for 
	 * keyboard-interactive authentication as described in RFC 4256.
	 */
	private class DbxUserInfo implements UserInfo, UIKeyboardInteractive
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
			System.out.println("---------- DbxUserInfo: promptPassword() message='" + message + "'.");

			// If we can't provide a GUI simply say NO
			if (GraphicsEnvironment.isHeadless()) 
			{
				return false;
			}
						
			// Prompt for password
			String promptPasswd = PromptForPassword.show(null, "Please specify the Password for SSH connection to '" + _hostname + "'.", _hostname, _username, SaveType.TO_CONFIG_USER_TEMP, "unused");

			if (StringUtil.hasValue(promptPasswd))
			{
				_password = promptPasswd;
				return true;
			}
			return false;
		}

		/**
		 * Returns the password entered by the user. This should be only called after a successful promptPassword(java.lang.String).
		 */
		@Override
		public String getPassword()
		{
			System.out.println("---------- DbxUserInfo: getPassword()");
			return _password;
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
			System.out.println("---------- DbxUserInfo: promptPassphrase() message='" + message + "'.");

			// If we can't provide a GUI simply say NO
			if (GraphicsEnvironment.isHeadless()) 
			{
				return false;
			}

			// Prompt for password
			String promptPasswd = PromptForPassword.show(null, "Please specify the Password for KeyFile for SSH connection to '" + _hostname + "'.", _hostname, _username, SaveType.TO_CONFIG_USER_TEMP, "unused");

			if (StringUtil.hasValue(promptPasswd))
			{
				_password = promptPasswd;
				return true;
			}
			return false;
		}

		/**
		 * Returns the passphrase entered by the user. This should be only called after a successful promptPassphrase(java.lang.String).
		 */
		@Override
		public String getPassphrase()
		{
			System.out.println("---------- DbxUserInfo: getPassphrase()");
			return _password;
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
			System.out.println("---------- DbxUserInfo: promptYesNo() message='" + message + "'.");

			// If we can't provide a GUI simply say NO
			if (GraphicsEnvironment.isHeadless()) 
			{
				return true; // return YES to any questions
			}
			
			// The message object to be filled in further below.
			JComponent msgDisplay = null;
			int msgType = JOptionPane.INFORMATION_MESSAGE;
			boolean expOnNo = false;

			// JSch messages are OK for folks who are familiar with SSH and such. However,
			// for a common user, here try and create a more meaningful message in
			// situations where the user is connecting to the server for the first time.
			if (message.startsWith("The authenticity of host") && message.contains("can't be established")) 
			{
				// This is a situation where the user is typically connecting for the first
				// time. Display a custom message.
				String rsaTag = "RSA key fingerprint is ";
				int rsaKeyPos = message.indexOf(rsaTag) + rsaTag.length() + 1;
				String rsaKey = message.substring(rsaKeyPos, message.indexOf('.', rsaKeyPos));

				// Format and display the message to the user.
				String custMsg = String.format(FIRST_SSH_CONNECTION_MSG, _hostname, rsaKey);
				msgDisplay = collapsedMessage(custMsg, message, true);
				expOnNo = true;
			} 
			else if (message.startsWith("WARNING: ")) 
			{
				// This is a situation when the RSA key for the server has changed. Here we
				// create a custom and more informative message than the one displayed by JSch.
				msgDisplay = collapsedMessage(SSH_HOST_CHANGE_MSG, message, true);
				msgType = JOptionPane.WARNING_MESSAGE;
				expOnNo = true;
			} 
			else 
			{
				// Create a text area to display the message from JSch in other cases
				JTextArea jta = new JTextArea(message);
				msgDisplay = new JScrollPane(jta);
			}

			// Display message to user and get user's Yes/No choice
			int choice = JOptionPane.showConfirmDialog(_guiOwner, msgDisplay, "Secure Shell (SSH) Protocol Interaction", JOptionPane.YES_NO_OPTION, msgType);

			// If the user clicked "No" for a "Warning" message, we throw an exception here
			// to communicate a serious issue.
			if (choice == JOptionPane.NO_OPTION && expOnNo) 
			{
				// A serious issue from which we should not retry connections etc.
				throw new RuntimeException(USER_INTERRUPTED_EXP_MSG);
			}

			// Return true if the user clicked on the "Yes" button.
			return (choice == JOptionPane.YES_OPTION);
		}

		/**
		 * Shows an informational message to the user.
		 * 
		 * @param message - the message to show to the user.
		 */
		@Override
		public void showMessage(String message)
		{
			System.out.println("---------- DbxUserInfo: showMessage() message='" + message + "'.");

			// Create a text area to display the message and place it inside a scroll pane
			// to permit display of large messages using a decent sized GUI window.
			JTextArea msgDisplay = new JTextArea(message);
			JScrollPane jsp = new JScrollPane(msgDisplay);

			// Show the message to the user.
			JOptionPane.showMessageDialog(_guiOwner, jsp, "Secure Shell (SSH) Message", JOptionPane.INFORMATION_MESSAGE);
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
		 */
		@Override
		public String[] promptKeyboardInteractive(String destination, String name, String instruction, String[] prompt, boolean[] echo)
		{
System.out.println("DbxUserInfo: promptKeyboardInteractive() destination='" + destination + "', name='" + name + "', instruction='" + instruction + "', prompt=[" + StringUtil.toCommaStr(prompt) + "], echo=[" + StringUtil.toCommaStr(echo) + "].");

			if ( prompt.length != echo.length )
			{
				// if jcsh is buggy... and passes different sizes
				throw new IllegalArgumentException( "prompt and echo size arrays are different!" );
			}

			int numPrompts = prompt.length;
//			int promptCount = 0;

			_logger.debug("SSH-authenticate-promptKeyboardInteractive(name='" + name + "', instruction='" + instruction + "', numPrompts=" + numPrompts + ", prompt='" + StringUtil.toCommaStr(prompt) + "', echo=" + StringUtil.toCommaStr(echo) + ")");

			// If it *only* asks for "Password:", do not prompt for that... just return it...
			if (numPrompts == 1 && prompt != null && prompt.length >= 1 && prompt[0].toLowerCase().startsWith("password:"))
			{
				return new String[] { _password };
			}

			String[] result = new String[numPrompts];
			String lastError = null;

			for (int i = 0; i < numPrompts; i++)
			{
				/* Often, servers just send empty strings for "name" and "instruction" */

				String[] content = new String[] { lastError, name, instruction, prompt[i] };

				if (lastError != null)
				{
					/* show lastError only once */
					lastError = null;
				}

				EnterSomethingDialog esd = new EnterSomethingDialog(null, "Keyboard Interactive Authentication", content, !echo[i]);

				esd.setVisible(true);

//				if (esd.answer == null)
//					throw new IOException("Login aborted by user");
				if (esd.answer == null)
					return null;

				result[i] = esd.answer;
//				promptCount++;
			}

			return result;
	    } // end: promptKeyboardInteractive

	} // end: class DbxUserInfo
	
	
	
	
	
	
	public static final String PROPKEY_LOG_TO_STDOUT = "SshConnection.log.jsch.to.stdout"; public static final boolean DEFAULT_LOG_TO_STDOUT = false;
	public static final String PROPKEY_LOG_TRACE     = "SshConnection.log.jsch.trace"    ; public static final boolean DEFAULT_LOG_TRACE     = false;
	public static final String PROPKEY_LOG_DEBUG     = "SshConnection.log.jsch.debug"    ; public static final boolean DEFAULT_LOG_DEBUG     = false;
	public static final String PROPKEY_LOG_INFO      = "SshConnection.log.jsch.info"     ; public static final boolean DEFAULT_LOG_INFO      = false;
	public static final String PROPKEY_LOG_WARNING   = "SshConnection.log.jsch.warning"  ; public static final boolean DEFAULT_LOG_WARNING   = true;
	public static final String PROPKEY_LOG_ERROR     = "SshConnection.log.jsch.error"    ; public static final boolean DEFAULT_LOG_ERROR     = true;
	public static final String PROPKEY_LOG_FATAL     = "SshConnection.log.jsch.fatal"    ; public static final boolean DEFAULT_LOG_FATAL     = true;

	/**
	 * JSch logging to the Log4J or STDOUT.
	 */
	private static class JschLog4jBridge implements com.jcraft.jsch.Logger
	{
		private static Logger _logger = Logger.getLogger(JschLog4jBridge.class);

		@Override
		public boolean isEnabled(int level)
		{
			Configuration conf = Configuration.getCombinedConfiguration();
			
			boolean enableAllLogging = conf.getBooleanProperty("SshConnection.logging.enable", false);
			if (enableAllLogging)
			{
				return true;
			}

			switch (level)
			{
				case com.jcraft.jsch.Logger.DEBUG:  return conf.getBooleanProperty(PROPKEY_LOG_TRACE  , DEFAULT_LOG_TRACE  );
				case com.jcraft.jsch.Logger.INFO:   return conf.getBooleanProperty(PROPKEY_LOG_DEBUG  , DEFAULT_LOG_DEBUG  );
				case com.jcraft.jsch.Logger.WARN:   return conf.getBooleanProperty(PROPKEY_LOG_INFO   , DEFAULT_LOG_INFO   );
				case com.jcraft.jsch.Logger.ERROR:  return conf.getBooleanProperty(PROPKEY_LOG_WARNING, DEFAULT_LOG_WARNING);
				case com.jcraft.jsch.Logger.FATAL:  return conf.getBooleanProperty(PROPKEY_LOG_ERROR  , DEFAULT_LOG_ERROR  );
				default:                            return conf.getBooleanProperty(PROPKEY_LOG_FATAL  , DEFAULT_LOG_FATAL  );
			}
		}

		@Override
		public void log(int level, String message)
		{
			// Write to the LOG subsystem
			switch (level)
			{
				case com.jcraft.jsch.Logger.DEBUG: _logger.info ("SSH DEBUG: "   + message); break; // Write using INFO method (debug is probably disabled)  
				case com.jcraft.jsch.Logger.INFO:  _logger.info ("SSH INFO: "    + message); break;
				case com.jcraft.jsch.Logger.WARN:  _logger.warn ("SSH WARNING: " + message); break;
				case com.jcraft.jsch.Logger.ERROR: _logger.error("SSH ERROR: "   + message); break;
				case com.jcraft.jsch.Logger.FATAL: _logger.fatal("SSH FATAL: "   + message); break;
				default:                           _logger.info ("SSH TRACE: "   + message); break; // Write using INFO method (trace is probably disabled)
			}
			
			// Write to the STDOUT
			boolean enableStdoutLogging = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_LOG_TO_STDOUT, DEFAULT_LOG_TO_STDOUT);
			if (enableStdoutLogging)
			{
				switch (level)
				{
					case com.jcraft.jsch.Logger.DEBUG: System.out.println("--- SSH DEBUG:   - " + message); break;
					case com.jcraft.jsch.Logger.INFO:  System.out.println("--- SSH INFO:    - " + message); break;
					case com.jcraft.jsch.Logger.WARN:  System.out.println("--- SSH WARNING: - " + message); break;
					case com.jcraft.jsch.Logger.ERROR: System.out.println("--- SSH ERROR:   - " + message); break;
					case com.jcraft.jsch.Logger.FATAL: System.out.println("--- SSH FATAL:   - " + message); break;
					default:                           System.out.println("--- SSH TRACE:   - " + message); break;
				}
			}
		}
	}





	/**
	 * Helper method to create a collapsed pane with message.
	 * 
	 * <p>This is a convenience utility method that creates a panel that
	 * contains two pieces of information. The first one is a "message"
	 * that is placed within a JLabel to be displayed to the user.
	 * This information is constantly visible. The second parameter
	 * "details", is placed within a JTextArea (inside a scroll pane)
	 * that is initially not visible. The text area is made visible
	 * only when the user clicks on a "Details" button that is created
	 * by this method. The complete JPanel can then be placed within
	 * other dialogs (such as JOptionPane.showMessageDialog()) to
	 * provide additional information to the user in a form that does
	 * not overwhelm the user with information.</p> 
	 * 
	 * <p><b>Note:</b>Use this method sparingly and only in circumstances
	 * in which you are absolutely sure that details are to be shown or
	 * hidden. When in doubt, prefer to use the overloaded
	 * {{@link #collapsedMessage(String, String)} method instead.</p>
	 * 
	 * @param message The message that is to be constantly displayed
	 * to the user via a JLabel.
	 * 
	 * @param details The extra information that will be placed within
	 * a JTextArea that is hidden (or shown) depending on the user's
	 * choice (indicated by clicking on the details button)
	 * 
	 * @param showDetails If this flag is true then the details are
	 * visible by default. If this flag is false, then the details are
	 * not visible by default.
	 * 
	 * @return This method returns the JPanel containing a collapsed
	 * details box with the details.
	 */
	public static JPanel collapsedMessage(String message, String details, boolean showDetails) {
		JPanel container = new JPanel(new BorderLayout(5, 5));
		JLabel info      = new JLabel(message);
		Dimension maxSize= info.getPreferredSize();
		maxSize.width     = Math.max(550, maxSize.width);
		info.setMinimumSize(maxSize);
		info.setPreferredSize(maxSize);
		container.add(info, BorderLayout.CENTER);
		// Use the preferred message dimension to setup the
		// dimensions of the collapsible panel.
		final JTextArea msg   = new JTextArea(details);
		final JScrollPane jsp = new JScrollPane(msg);
		jsp.setVisible(showDetails);
		// Setup the maximum scroll pane size so that it looks good.
		maxSize        = info.getPreferredSize();
		maxSize.height = 100;
		maxSize.width  = Math.max(550, maxSize.width);
		jsp.setPreferredSize(maxSize);
		jsp.setMaximumSize(maxSize);
		jsp.setMinimumSize(maxSize);
		// The simple details button with an icon.
//		final JToggleButton detailBtn = new JToggleButton("Details  ", Utilities.getIcon("images/16x16/more_details_16.png"));
//		detailBtn.setSelectedIcon(Utilities.getIcon("images/less_details_16.png"));
		final JToggleButton detailBtn = new JToggleButton("Details  ", SwingUtils.readImageIcon(Version.class, "images/more_details_16.png"));
		detailBtn.setSelectedIcon(SwingUtils.readImageIcon(Version.class, "images/less_details_16.png"));
		
		detailBtn.setBorder(null);
		detailBtn.setContentAreaFilled(false);
		detailBtn.setSelected(true);
		detailBtn.setFocusable(false);
		detailBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				// Details button was clicked. Show or hide the message depending on status.
				jsp.setVisible(detailBtn.isSelected());
				// Get top-level parent to validate its layout again.
				SwingUtilities.getWindowAncestor(jsp).pack();
			}
		});
		// Put button and a horizontal line in a suitable sizer.
		Box btnBox = Box.createHorizontalBox();
		btnBox.add(detailBtn);
		btnBox.add(Box.createHorizontalGlue());
		// Add a JSeparator adjacent to button to make it pretty.
		JPanel subBox = new JPanel(new BorderLayout(0, 0));
		subBox.add(btnBox, BorderLayout.NORTH);
		subBox.add(new JSeparator(), BorderLayout.CENTER);
		subBox.add(jsp, BorderLayout.SOUTH);
		// Add the subBox to the main container.
		container.add(subBox, BorderLayout.SOUTH);
		return container;
	}

	/**
	 * This dialog displays a number of text lines and a text field.
	 * The text field can either be plain text or a password field.
	 */
	class EnterSomethingDialog extends JDialog
	{
		private static final long serialVersionUID = 1L;

		JTextField answerField;
		JPasswordField passwordField;

		final boolean isPassword;

		String answer;

		public EnterSomethingDialog(JFrame parent, String title, String content, boolean isPassword)
		{
			this(parent, title, new String[] { content }, isPassword);
		}

		public EnterSomethingDialog(JFrame parent, String title, String[] content, boolean isPassword)
		{
			super(parent, title, true);

			this.isPassword = isPassword;

			JPanel pan = new JPanel();
			pan.setLayout(new BoxLayout(pan, BoxLayout.Y_AXIS));

			for (int i = 0; i < content.length; i++)
			{
				if ((content[i] == null) || (content[i] == ""))
					continue;
				JLabel contentLabel = new JLabel(content[i]);
				pan.add(contentLabel);

			}

			answerField = new JTextField(20);
			passwordField = new JPasswordField(20);

			if (isPassword)
				pan.add(passwordField);
			else
				pan.add(answerField);

			KeyAdapter kl = new KeyAdapter()
			{
				@Override
				public void keyTyped(KeyEvent e)
				{
					if (e.getKeyChar() == '\n')
						finish();
				}
			};

			answerField.addKeyListener(kl);
			passwordField.addKeyListener(kl);

			getContentPane().add(BorderLayout.CENTER, pan);

			setResizable(false);
			pack();
			setLocationRelativeTo(null);
		}

		private void finish()
		{
			if (isPassword)
				answer = new String(passwordField.getPassword());
			else
				answer = answerField.getText();

			dispose();
		}
	}
}

