package asemon.hostmon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

public class SshConnection
{
	private static Logger _logger = Logger.getLogger(SshConnection.class);

	private String _username = null;
	private String _password = null;
	private String _hostname = null;
	private int    _port     = 22;
	
	private Connection _conn = null;
	private boolean    _isConnected = false;

	/** output from 'uname -a', which was made while the connection was created. */
	private String _uname = null;

	/** First String in the output from 'uname -a', which was made while the connection was created. */
	private String _osName = null;

	/** Used to create Strings from the remote host, so that client character set convention can be done. 
	 * NOTE: For the moment this is hard coded based on the OsName 
	 * (Linux=UTF-8, SunOs=ISO-8859-1, AIX=ISO-8859-1, HP-UX=ISO-8859-1, else=null)*/
	private String _osCharset = null;

	/** If we have been authenticated once, it means that we can do reconnect, because login has been successful at least once */
	private boolean _isAuthenticated = false;

	/** There is stdout data available that is ready to be consumed. */
	public static final int STDOUT_DATA = ChannelCondition.STDOUT_DATA;

	/** There is stderr data available that is ready to be consumed. */
	public static final int STDERR_DATA = ChannelCondition.STDERR_DATA;

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
		this(hostname, 22, username, password);
	}

	/**
	 * Create a SshConnection object, you still need to connect() after the object is created
	 * @param hostname
	 * @param port
	 * @param username
	 * @param password
	 */
	public SshConnection(String hostname, int port, String username, String password)
	{
		_hostname = hostname;
		_port     = port;
		_username = username;
		_password = password;
	}

	public void setUsername(String username) { _username = username; }
	public void setPassword(String password) { _password = password; }
	public void setHost(String host)         { _hostname = host; }
	public void setPort(int port)            { _port     = port; }

	public String getUsername() { return _username; }
	public String getPassword() { return _password; }
	public String getHost()     { return _hostname; }
	public int    getPort()     { return _port; }

	/**
	 * Connect to a remote host
	 * @return true if we succeeded to connect.
	 * @throws IOException if we failed to authenticate
	 */
	public boolean connect()
	throws IOException
	{
		// Check that user, password and hostname is set 
		if (_username == null || (_username != null && _username.trim().equals(""))) throw new IllegalArgumentException("Trying to connect to a SSH host, but 'username' fields is net yet given.");
		if (_password == null || (_password != null && _password.trim().equals(""))) throw new IllegalArgumentException("Trying to connect to a SSH host, but 'password' fields is net yet given.");
		if (_hostname == null || (_hostname != null && _hostname.trim().equals(""))) throw new IllegalArgumentException("Trying to connect to a SSH host, but 'hostname' fields is net yet given.");

		// Create a connection instance if none exists.
		if (_conn == null)
			_conn = new Connection(_hostname, _port);

		// And connect to the host
		_conn.connect();

		// Authenticate
		_isAuthenticated = _conn.authenticateWithPassword(_username, _password);

		// Get out of here, if not successful authentication
		if (_isAuthenticated == false)
			throw new IOException("Authentication failed to host='"+_hostname+"', on port='"+_port+"', with username='"+_username+"'.");

		_logger.info("Just Connected to SSH host '"+_hostname+"' on port '"+_port+"' with user '"+_username+"'.");

		// Try to get what OS we connected to
		getOsInfo();
		_logger.info("The host SSH host '"+_hostname+"' has '"+getOsName()+"' as it's Operating System. My guess is that it's using character set '"+getOsCharset()+"'.");

		_isConnected = true;
		return true;
	}

	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	public boolean reconnect()
	throws IOException
	{
		if (_isAuthenticated == false)
			throw new IOException("Cant't do reconnect yet, you need to have a valid connection first. This means that you need to caoonect with a successful authentication first.");

		_conn.close();

		_logger.info("Trying to reconnect to SSH host '"+_hostname+"' on port '"+_port+"' with user '"+_username+"'.");
		return connect();
	}

	/**
	 * Close the connection to the remote host
	 */
	public void close()
	{
		if (_conn != null)
		{
			_logger.debug("Closing the connection to SSH host '"+_hostname+"' on port '"+_port+"' with user '"+_username+"'.");
			_conn.close();
		}
		_conn = null;
		_isConnected = false;
	}

	/**
	 * Check if you are connected to a remote host 
	 * @return true if connected, false if not connected
	 */
	public boolean isConnected()
	{
		if (_conn == null)
			_isConnected = false;

		return _isConnected;
	}

	/**
	 * Check if the SshConnection is closed to the remote host 
	 * @return true if closed, false if connected
	 */
	public boolean isClosed()
	{
		return ! isConnected();
	}

	/** 
	 * Open a session where a command can be executed. {@link ch.ethz.ssh2.Session}
	 * @see Session 
	 */ 
	public Session openSession() 
	throws IOException
	{
		return _conn.openSession();
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
	synchronized public Session execCommand(String command) 
	throws IOException
	{
		if (_conn == null)
		{
			throw new IOException("The SSH connection to the host '"+_hostname+"' was null. The connection has not been initialized OR someone has closed the connection.");
		}

		try
		{
			Session sess = _conn.openSession();
			_logger.debug("Executing command '"+command+"' on connection: "+toString());
			sess.execCommand(command);
			return sess;
		}
		catch (IllegalStateException e)
		{
			// This is thrown in openSession, and it's a RuntimeException
			// IllegalStateException("Cannot open session, you need to establish a connection first.");
			// IllegalStateException("Cannot open session, connection is not authenticated.");
			// SO should we re-connect or just leave it?
			// lets try use same logic as below
			if (_isAuthenticated)
			{
				_logger.info("The Connection to SSH host '"+_hostname+"' on port '"+_port+"' seems to be lost/closed. I will try reconnect and, execute the command '"+command+"' again. Caught: "+e);

				reconnect();
				Session sess = _conn.openSession();
				_logger.info("Re-Executing command '"+command+"' after the reconnect.");
				sess.execCommand(command);
				return sess;
			}
			// if we can't handle the Exception, throw it
			throw e;
		}
		catch (IOException e)
		{
			// If this is a "lost" connection try to "reconnect"
			if (e.getMessage().indexOf("connection is closed") >= 0)
			{
				// if we already has been authenticated once, then try to reconnect again
				if (_isAuthenticated)
				{
					_logger.info("The Connection to SSH host '"+_hostname+"' on port '"+_port+"' seems to be lost/closed. I will try reconnect and, execute the command '"+command+"' again.");

					reconnect();
					Session sess = _conn.openSession();
					_logger.info("Re-Executing command '"+command+"' after the reconnect.");
					sess.execCommand(command);
					return sess;
				}
			}
			// if we can't handle the Exception, throw it
			throw e;
		}
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
	 * simply does 'uname -a' and return the string.
	 */
	public String getOsInfo()
	throws IOException
	{
		if (_uname != null)
			return _uname;

		Session sess = _conn.openSession();
		sess.execCommand("uname -a");

		InputStream stdout = new StreamGobbler(sess.getStdout());
		BufferedReader br = new BufferedReader(new InputStreamReader(stdout));

		String output = "";
		while (true)
		{
			String line = br.readLine();
			if (line == null)
				break;

			output += line;
		}

		sess.close();
		
		_uname = output;
		if (_uname != null)
		{
			String[] sa = _uname.split(" ");
			_osName = sa[0];

			// also try to figgure out a dummy default character set for the OS
			if      (_osName.equals("Linux")) _osCharset = "UTF-8";
			else if (_osName.equals("SunOS")) _osCharset = "ISO-8859-1";
			else if (_osName.equals("AIX"))   _osCharset = "ISO-8859-1"; // TODO: CHECK
			else if (_osName.equals("HP-UX")) _osCharset = "ISO-8859-1"; // TODO: CHECK
			else _osCharset = null;

			//Charset.forName("ISO-8859-1");
		}
		_logger.debug("OS Info: 'uname -a' produced '"+_uname+"'.");

		return output;
	}

	/**
	 * Check if the Veritas 'vxstat' is executable and in the current path
	 * 
	 * @return true if Veritas commands are available (in the path)
	 * @throws IOException
	 */
	public boolean hasVeritas()
	throws IOException
	{
		Session sess = _conn.openSession();
		sess.execCommand("vxstat");

		InputStream stdout = new StreamGobbler(sess.getStdout());
		BufferedReader br = new BufferedReader(new InputStreamReader(stdout));

		while (true)
		{
			String line = br.readLine();
			if (line == null)
				break;

			_logger.debug("hasVeritas(): "+line);
		}

		boolean retStatus = true;
		Integer exitCode = sess.getExitStatus();
		if ( exitCode == null || exitCode != null && exitCode != 0 )
			retStatus = false;

		sess.close();

		_logger.debug("hasVeritas(): returned "+retStatus);

		return retStatus;
	}

	public String toString()
	{
		return 
			"host='"+_hostname+":"+_port+"', " +
			"user='"+_username+"', " +
			"osName='"+_osName+"', " +
			"osCharset='"+_osCharset+"', " +
			"isConnected='"+_isConnected+"'.";
	}
}

