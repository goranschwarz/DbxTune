package com.asetune.ssh;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.util.HashMap;

import org.apache.log4j.Logger;

import ch.ethz.ssh2.LocalPortForwarder;

import com.asetune.utils.StringUtil;

/**
 * Handle SSH Tunnels
 * 
 * @author gorans
 */
public class SshTunnelManager
{
    /** Log4j logging. */
	private static Logger _logger = Logger.getLogger(SshTunnelManager.class);

	/** Instance variable */
	private static SshTunnelManager _instance = null;

	/** SSH Connection Cache */
	private HashMap<String, SshConnectionWrapper> _connectionCache = new HashMap<String, SshConnectionWrapper>();

	/** Tunnel Cache */
	private HashMap<String, LocalPortForwarderWrapper> _tunnelCache = new HashMap<String, LocalPortForwarderWrapper>();

	public static final int GENERATE_PORT_NUMBER_START = 2955;

	/** local class */
	private static class LocalPortForwarderWrapper
	{
		public LocalPortForwarderWrapper(int port, LocalPortForwarder localPortForwarder, String sshConnKey, SshConnection sshConnection)
		{
			_numberOfUsers      = 0;
			_port               = port;
			_localPortForwarder = localPortForwarder;
			_sshConnKey         = sshConnKey;
			_sshConnection      = sshConnection;
		}
		public void incrementUsage() 
		{
			_numberOfUsers++;
		}
		public void decrementUsage() 
		{
			_numberOfUsers--;
		}
		public int                _numberOfUsers;
		public int                _port;
		public LocalPortForwarder _localPortForwarder;
		public String             _sshConnKey;
		public SshConnection      _sshConnection;
	}
	private static class SshConnectionWrapper
	{
		public SshConnectionWrapper(SshConnection sshConnection)
		{
			_numberOfUsers = 0;
			_sshConnection = sshConnection;
		}
		public void incrementUsage() 
		{
			_numberOfUsers++;
		}
		public void decrementUsage() 
		{
			_numberOfUsers--;
		}
		public int           _numberOfUsers;
		public SshConnection _sshConnection;
	}

	public static boolean hasInstance()
	{
		return (_instance != null);
	}

	public static SshTunnelManager getInstance()
	{
		if (_instance == null)
			_instance = new SshTunnelManager();
		return _instance;
	}


	/** 
	 * guess what port number we will get (this also sets the localport in sshTunnelInfo)
	 */
	public int guessPort(String hostPortStr, SshTunnelInfo sshTunnelInfo)
	{
		if (StringUtil.isNullOrBlank(hostPortStr)) 
			throw new IllegalArgumentException("hostPortStr can't be null");
		if (sshTunnelInfo == null) 
			throw new IllegalArgumentException("sshTunnelInfo can't be null");

		// Get SSH Tunnel if we already has a valid tunnel
		LocalPortForwarderWrapper lpfw =  _tunnelCache.get(hostPortStr);
		if ( lpfw != null )
		{
			// Set the local port with the one in the cache
			sshTunnelInfo.setLocalPort(lpfw._port);
		}
		
		if (sshTunnelInfo.getLocalPort() <= 0 && sshTunnelInfo.isLocalPortGenerated())
		{
			int firstFreeLocalPortNumber = getFirstFreeLocalPortNumber();
			sshTunnelInfo.setLocalPort(firstFreeLocalPortNumber);
		}

		return sshTunnelInfo.getLocalPort();
	}

	/**
	 * 
	 * @param hostPortStr
	 * @param sshTunnelInfo
	 * @throws IOException
	 */
	public void setupTunnel(String hostPortStr, SshTunnelInfo sshTunnelInfo) 
	throws IOException
	{
		if (StringUtil.isNullOrBlank(hostPortStr)) 
			throw new IllegalArgumentException("hostPortStr can't be null");
		if (sshTunnelInfo == null) 
			throw new IllegalArgumentException("sshTunnelInfo can't be null");

		String connKey  = sshTunnelInfo.getSshHost() + ":" + sshTunnelInfo.getSshPort() + ":" + sshTunnelInfo.getSshUsername();
		String connInfo = "Host " + sshTunnelInfo.getSshHost() + ":" + sshTunnelInfo.getSshPort() + ", User " + sshTunnelInfo.getSshUsername();

		// Get SSH Tunnel if we already has a valid tunnel
		LocalPortForwarderWrapper lpfw =  _tunnelCache.get(hostPortStr);
		if ( lpfw != null )
		{
			if (lpfw._sshConnection.isConnected())
			{
				lpfw.incrementUsage();
				// FIXME: should we test if the connection is up and running / valid
				_logger.info("Reusing an Previously setup Tunnel for '"+hostPortStr+"' that uses Local Port '"+lpfw._port+"', which sshConnKey '"+connKey+"'.");
				sshTunnelInfo.setLocalPort(lpfw._port);
				return;
			}
			else
			{
				// remove the cached entry
				lpfw = null;
				_tunnelCache.remove(hostPortStr);
			}
		}
		
		// Get SSH Connection if we already has a connection
		boolean makeNewShhConnection = true;
		SshConnectionWrapper sshConnWrap = _connectionCache.get(connKey);
		if (sshConnWrap != null)
		{
			if (sshConnWrap._sshConnection.isConnected())
			{
				sshConnWrap.incrementUsage();
				_logger.info("Reusing an already connected SSH Connection to "+connInfo);
				makeNewShhConnection = false;
			}
			else
			{
				// remove the cached entry
				sshConnWrap = null;
				_connectionCache.remove(connKey);
			}
		}

		if (makeNewShhConnection)
		{
			SshConnection sshConn = new SshConnection(
				sshTunnelInfo.getSshHost(), 
				sshTunnelInfo.getSshPort(), 
				sshTunnelInfo.getSshUsername(), 
				sshTunnelInfo.getSshPassword());

			sshConn.connect();
			
			// execute OS Init String if there is one
			String initCmd = sshTunnelInfo.getSshInitOsCmd();
			if (StringUtil.hasValue(initCmd))
			{
				try
				{
					String output = sshConn.execCommandOutputAsStr(initCmd);
					if (StringUtil.hasValue(output))
						_logger.info("SSH Init OS Command '"+initCmd+"' produced the following output: " + output);
				}
				catch (IOException e)
				{
					_logger.warn("SSH Init OS Command '"+initCmd+"' probably failed: " + e.toString());
				}
			}

			sshConnWrap = new SshConnectionWrapper(sshConn);
			sshConnWrap.incrementUsage();

			// Add it to the "cache"
			_connectionCache.put(connKey, sshConnWrap);
		}

		// Create a local port forwarder
		LocalPortForwarder lpf = sshConnWrap._sshConnection.createLocalPortForwarder(sshTunnelInfo);
		lpfw = new LocalPortForwarderWrapper(sshTunnelInfo.getLocalPort(), lpf, connKey, sshConnWrap._sshConnection);
		lpfw.incrementUsage();
		
		// Add it to the "cache"
		_tunnelCache.put(hostPortStr, lpfw);
	}

	public void releaseTunnel(String hostPortStr)
	{
		// Get SSH Tunnel if we already has a valid tunnel
		LocalPortForwarderWrapper lpfw =  _tunnelCache.get(hostPortStr);
		if ( lpfw != null )
		{
			// decrement usage count
			lpfw.decrementUsage();
			
			// if not used anymore, close the listener
			if (lpfw._numberOfUsers <= 0)
			{
				try
				{
					_logger.info("Closing the LocalPortForwarder on port '"+lpfw._port+"'.");
					lpfw._localPortForwarder.close();
				}
				catch (IOException e)
				{
					_logger.warn("Problem closing the LocalPortForwarder on port '"+lpfw._port+"'.", e);
				}
				_tunnelCache.remove(hostPortStr);
			}
			
			// FIXME: do something similar with the SSH Connection
			SshConnectionWrapper cw =  _connectionCache.get(lpfw._sshConnKey);
			if ( cw != null )
			{
				// decrement usage count
				cw.decrementUsage();
				
				// if not used anymore, close the listener
				if (cw._numberOfUsers <= 0)
				{
					_logger.warn("Closing the SshConnection to '"+cw._sshConnection.getHost()+":"+cw._sshConnection.getPort()+"', User '"+cw._sshConnection.getUsername()+"'.");
					cw._sshConnection.close();
					_connectionCache.remove(lpfw._sshConnKey);
				}
			}
		}
	}

	/** 
	 * generate first available local port number that is not in use 
	 * 
	 * @return first available local port number that is not in use
	 */
	public static int generateLocalPort()
	throws IOException
	{
		for (int port=GENERATE_PORT_NUMBER_START; port<(32768*2); port++)
		{
			try
			{
				ServerSocket serverSocket = new ServerSocket(port);
				serverSocket.close();
				
				return port;
			}
			catch (BindException ex)
			{
				continue;
			}
			catch (Throwable ex)
			{
				if (ex instanceof IOException)
					throw (IOException)ex;

				throw new IOException("Unhandled exception when trying to find new local port. Caught: "+ex, ex);
			}
		}
		// we should never get here
		return -1;
	}

	/**
	 * Get a port number which is free, start to loop at 2955 (GENERATE_PORT_NUMBER_START)<br>
	 * @return -1 if unhandled exceptions occurs, otherwise the first free port number.
	 */
	public static int getFirstFreeLocalPortNumber()
	{
		for (int port=GENERATE_PORT_NUMBER_START; port<(32768*2); port++)
		{
			try
			{
				ServerSocket serverSocket = new ServerSocket(port);
				serverSocket.close();
				
				return port;
			}
			catch (BindException ex)
			{
				continue;
			}
			catch (Throwable ex)
			{
				return -1;
			}
		}
		return -1;
	}

}
