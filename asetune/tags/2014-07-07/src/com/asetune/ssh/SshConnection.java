package com.asetune.ssh;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.apache.log4j.Logger;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.InteractiveCallback;
import ch.ethz.ssh2.LocalPortForwarder;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

import com.asetune.utils.StringUtil;

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
//		_isAuthenticated = _conn.authenticateWithPassword(_username, _password);
		_isAuthenticated = authenticate();

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
	
	private boolean authenticate()
	throws IOException
	{
//		final String knownHostPath = "~/.ssh/known_hosts";
//		final String idDSAPath     = "~/.ssh/id_dsa";
//		final String idRSAPath     = "~/.ssh/id_rsa";

		boolean enableKeyboardInteractive = true;
//		boolean enableDSA = true;
//		boolean enableRSA = true;

		String lastError = null;

		while (true)
		{
//			if ((enableDSA || enableRSA) && _conn.isAuthMethodAvailable(_username, "publickey"))
//			{
//System.out.println("SSH-authenticate-Query: isAuthMethodAvailable(publickey) == TRUE");
//				_logger.debug("SSH-authenticate-Query: isAuthMethodAvailable(publickey) == TRUE");
//
//				if (enableDSA)
//				{
//					File key = new File(idDSAPath);
//
//					if (key.exists())
//					{
//						EnterSomethingDialog esd = new EnterSomethingDialog(null, "DSA Authentication",
//								new String[] { lastError, "Enter DSA private key password:" }, true);
//						esd.setVisible(true);
//
//System.out.println("SSH-authenticate-DO: authenticateWithPublicKey:DSA");
//						_logger.debug("SSH-authenticate-DO: authenticateWithPublicKey:DSA");
//						boolean res = _conn.authenticateWithPublicKey(_username, key, esd.answer);
//
//						if (res == true)
//							break;
//
//						lastError = "DSA authentication failed.";
//					}
//					enableDSA = false; // do not try again
//				}
//
//				if (enableRSA)
//				{
//					File key = new File(idRSAPath);
//
//					if (key.exists())
//					{
//						EnterSomethingDialog esd = new EnterSomethingDialog(null, "RSA Authentication",
//								new String[] { lastError, "Enter RSA private key password:" }, true);
//						esd.setVisible(true);
//
//System.out.println("SSH-authenticate-DO: authenticateWithPublicKey:RSA");
//						_logger.debug("SSH-authenticate-DO: authenticateWithPublicKey:RSA");
//						boolean res = _conn.authenticateWithPublicKey(_username, key, esd.answer);
//
//						if (res == true)
//							break;
//
//						lastError = "RSA authentication failed.";
//					}
//					enableRSA = false; // do not try again
//				}
//
//				continue;
//			}

			if (enableKeyboardInteractive && _conn.isAuthMethodAvailable(_username, "keyboard-interactive"))
			{
//System.out.println("SSH-authenticate-Query: isAuthMethodAvailable(keyboard-interactive) == TRUE");
				_logger.debug("SSH-authenticate-Query: isAuthMethodAvailable(keyboard-interactive) == TRUE");
				InteractiveLogic il = new InteractiveLogic(lastError);

//System.out.println("SSH-authenticate-DO: authenticateWithKeyboardInteractive");
				_logger.debug("SSH-authenticate-DO: authenticateWithKeyboardInteractive");
				boolean res = _conn.authenticateWithKeyboardInteractive(_username, il);

				if (res == true)
					break;

				if (il.getPromptCount() == 0)
				{
					// aha. the server announced that it supports "keyboard-interactive", but when
					// we asked for it, it just denied the request without sending us any prompt.
					// That happens with some server versions/configurations.
					// We just disable the "keyboard-interactive" method and notify the user.

					lastError = "Keyboard-interactive does not work.";

					enableKeyboardInteractive = false; // do not try this again
				}
				else
				{
					lastError = "Keyboard-interactive auth failed."; // try again, if possible
				}

				continue;
			}

			if (_conn.isAuthMethodAvailable(_username, "password"))
			{
//System.out.println("SSH-authenticate-Query: isAuthMethodAvailable(password) == TRUE");
				_logger.debug("SSH-authenticate-Query: isAuthMethodAvailable(password) == TRUE");
//				final EnterSomethingDialog esd = new EnterSomethingDialog(loginFrame,
//						"Password Authentication",
//						new String[] { lastError, "Enter password for " + _username }, true);
//
//				esd.setVisible(true);
//
//				if (esd.answer == null)
//					throw new IOException("Login aborted by user");
//
//				boolean res = _conn.authenticateWithPassword(_username, esd.answer);

//System.out.println("SSH-authenticate-DO: authenticateWithPassword");
				_logger.debug("SSH-authenticate-DO: authenticateWithPassword");
				boolean res = _conn.authenticateWithPassword(_username, _password);
				if (res == true)
					break;

				lastError = "Password authentication failed."; // try again, if possible

				continue;
			}

			throw new IOException("No supported authentication methods available.");
		}
		return true;
	}
	/**
	 * The logic that one has to implement if "keyboard-interactive" authentication shall be supported.
	 */
	class InteractiveLogic implements InteractiveCallback
	{
		int promptCount = 0;
		String lastError;

		public InteractiveLogic(String lastError)
		{
			this.lastError = lastError;
		}

		/* the callback may be invoked several times, depending on how many questions-sets the server sends */

		@Override
		public String[] replyToChallenge(String name, String instruction, int numPrompts, String[] prompt,
				boolean[] echo) throws IOException
		{
//System.out.println("SSH-authenticate-replyToChallenge(name='"+name+"', instruction='"+instruction+"', numPrompts="+numPrompts+", prompt='"+StringUtil.toCommaStr(prompt)+"', echo="+StringUtil.toCommaStr(echo)+")");
			_logger.debug("SSH-authenticate-replyToChallenge(name='"+name+"', instruction='"+instruction+"', numPrompts="+numPrompts+", prompt='"+StringUtil.toCommaStr(prompt)+"', echo="+StringUtil.toCommaStr(echo)+")");

			// If it *only* asks for "Password:", do not prompt for that... just return it...
			if (numPrompts == 1 && prompt != null && prompt.length >= 1 && prompt[0].toLowerCase().startsWith("password:"))
			{
				return new String[] { _password };
			}

			String[] result = new String[numPrompts];

			for (int i = 0; i < numPrompts; i++)
			{
				/* Often, servers just send empty strings for "name" and "instruction" */

				String[] content = new String[] { lastError, name, instruction, prompt[i] };

				if (lastError != null)
				{
					/* show lastError only once */
					lastError = null;
				}

				EnterSomethingDialog esd = new EnterSomethingDialog(null, "Keyboard Interactive Authentication",
						content, !echo[i]);

				esd.setVisible(true);

				if (esd.answer == null)
					throw new IOException("Login aborted by user");

				result[i] = esd.answer;
				promptCount++;
			}

			return result;
		}

		/* We maintain a prompt counter - this enables the detection of situations where the ssh
		 * server is signaling "authentication failed" even though it did not send a single prompt.
		 */

		public int getPromptCount()
		{
			return promptCount;
		}
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

	/**
	 * Creates a new LocalPortForwarder. <br>
	 * A LocalPortForwarder forwards TCP/IP connections that arrive at a local port via the 
	 * secure tunnel to another host (which may or may not be identical to the remote SSH-2 server). 
	 * <p>
	 * This method must only be called after one has passed successfully the authentication step. 
	 * There is no limit on the number of concurrent forwarding.
	 * 
	 * @param sshTunnelInfo
	 * @return LocalPortForwarder
	 * @throws IOException 
	 */
	public LocalPortForwarder createLocalPortForwarder(SshTunnelInfo sshTunnelInfo)
	throws IOException
	{
		int    localPort = sshTunnelInfo.getLocalPort();
		String destHost = sshTunnelInfo.getDestHost();
		int    destPort = sshTunnelInfo.getDestPort();

		if (localPort < 0 && sshTunnelInfo.isLocalPortGenerated() )
		{
//			localPort = sshTunnelInfo.generateLocalPort();
			localPort = SshTunnelManager.generateLocalPort();
			
			sshTunnelInfo.setLocalPort(localPort);
		}

		try
		{
			_logger.info("Creating a Local Port Forwarder/Tunnel from Local port '"+localPort+"' to Destination host '"+destHost+"', port '"+destPort+"'.");
			return _conn.createLocalPortForwarder(localPort, destHost, destPort);
		}
		catch (IOException e)
		{
			_logger.info("Problems ,creating a Local Port Forwarder/Tunnel from Local port '"+localPort+"' to Destination host '"+destHost+"', port '"+destPort+"'. Caught: "+e, e);
			throw e;
		}
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
			throw new IOException("Can't do reconnect yet, you need to have a valid connection first. This means that you need to caoonect with a successful authentication first.");

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
		if ( ! _isConnected )
			return false;

		if (_conn == null)
			_isConnected = false;
		else
		{
			try 
			{
				_logger.debug("isConnected(): SEND IGNORE PACKET to SSH Server.");
				_conn.sendIgnorePacket();
			}
			catch (IOException e) 
			{
				_logger.info("isConnected() has problems when sending a 'ignore packet' to the SSH Server. The connection will be closed. sendIgnorePacket Caught: "+e);
				close();
			}
		}

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
	 * @return a String, which the command produced
	 * @throws IOException if return code from the command != 0
	 */
	synchronized public String execCommandOutputAsStr(String command) 
	throws IOException
	{
		if (isClosed())
			throw new IOException("SSH is not connected. (host='"+_hostname+"', port="+_port+", user='"+_username+"', osName='"+_osName+"', osCharset='"+_osCharset+"'.)");

		Session sess = _conn.openSession();
		sess.execCommand(command);

		InputStream stdout = new StreamGobbler(sess.getStdout());
		BufferedReader br = new BufferedReader(new InputStreamReader(stdout));

		StringBuilder sb = new StringBuilder();
		while (true)
		{
			String line = br.readLine();
			if (line == null)
				break;

			sb.append(line);
			sb.append("\n");
		}
		String output = sb.toString();

		Integer rc = sess.getExitStatus();

		sess.close();
		br.close();
		
		_logger.debug("execCommandRetAsStr: '"+command+"' produced '"+output+"'.");

		if (rc != null && rc.intValue() != 0)
			throw new IOException("execCommandRetAsStr('"+command+"') return code not zero. rc="+rc+". Output: "+output);

		return output;
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

		if (_conn == null)
		{
			throw new IOException("The SSH connection to the host '"+_hostname+"' was null. The connection has not been initialized OR someone has closed the connection.");
		}

		Session sess = _conn.openSession();
//		sess.execCommand("uname -a");
		sess.execCommand("uname");

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
			if (sa.length > 0)
				_osName = sa[0];

			// also try to figure out a dummy default character set for the OS
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
	 * Simply check if a file name exists in the remote server
	 * @param filename to check if it exists
	 */
	public boolean doFileExist(String filename)
	{
		if (isClosed())
			throw new RuntimeException("SSH is not connected. (host='"+_hostname+"', port="+_port+", user='"+_username+"', osName='"+_osName+"', osCharset='"+_osCharset+"'.)");

		try
		{
			Session sess = _conn.openSession();
			sess.execCommand("ls -Fal '" + filename + "'");

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
			
			_logger.debug("doFileExist: '"+filename+"' produced '"+output+"'.");

			if ( output.startsWith("-") )
				return true;
			return false;
		}
		catch (IOException e)
		{
			_logger.error("doFileExist() caught: "+e, e);
			return false;
		}
	}

	/**
	 * Create a file
	 * @param filename to create
	 */
	public boolean createNewFile(String filename)
	throws IOException
	{
		if (isClosed())
			throw new IOException("SSH is not connected. (host='"+_hostname+"', port="+_port+", user='"+_username+"', osName='"+_osName+"', osCharset='"+_osCharset+"'.)");

		Session sess = _conn.openSession();
		sess.execCommand("touch '" + filename + "'");

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

		Integer rc = sess.getExitStatus();

		sess.close();
		
		_logger.debug("createNewFile: '"+filename+"' produced '"+output+"'.");

		if ( ! StringUtil.isNullOrBlank(output) )
			throw new IOException("createNewFile('"+filename+"') produced output, which wasn't expected. Output: "+output);

		if (rc != null && rc.intValue() != 0)
			throw new IOException("createNewFile('"+filename+"') return code not zero. rc="+rc+". Output: "+output);

		return true;
	}

	/**
	 * Remove a file
	 * @param filename to remove
	 */
	public boolean removeFile(String filename)
	throws IOException
	{
		if (isClosed())
			throw new IOException("SSH is not connected. (host='"+_hostname+"', port="+_port+", user='"+_username+"', osName='"+_osName+"', osCharset='"+_osCharset+"'.)");

		Session sess = _conn.openSession();
		sess.execCommand("rm -f '" + filename + "'");

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

		Integer rc = sess.getExitStatus();

		sess.close();
		
		_logger.debug("removeFile: '"+filename+"' produced '"+output+"'.");

		if ( ! StringUtil.isNullOrBlank(output) )
			throw new IOException("removeFile('"+filename+"') produced output, which wasn't expected. Output: "+output);

		if (rc != null && rc.intValue() != 0)
			throw new IOException("removeFile('"+filename+"') return code not zero. rc="+rc+". Output: "+output);

		return true;
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
		if (isClosed())
			throw new IOException("SSH is not connected. (host='"+_hostname+"', port="+_port+", user='"+_username+"', osName='"+_osName+"', osCharset='"+_osCharset+"'.)");

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

	@Override
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

