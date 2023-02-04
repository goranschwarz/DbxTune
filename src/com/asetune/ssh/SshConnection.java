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
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.asetune.gui.swing.PromptForPassword;
import com.asetune.gui.swing.PromptForPassword.SaveType;
import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.VersionShort;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.InteractiveCallback;
import ch.ethz.ssh2.KnownHosts;
import ch.ethz.ssh2.LocalPortForwarder;
import ch.ethz.ssh2.ServerHostKeyVerifier;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

public class SshConnection
{
	private static Logger _logger = Logger.getLogger(SshConnection.class);

	private String _username = null;
	private String _password = null;
	private String _hostname = null;
	private int    _port     = 22;
	private String _keyFile  = null;
	
	private Connection _conn = null;
	private boolean    _isConnected = false;

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

	/** There is stdout data available that is ready to be consumed. */
	public static final int STDOUT_DATA = ChannelCondition.STDOUT_DATA;

	/** There is stderr data available that is ready to be consumed. */
	public static final int STDERR_DATA = ChannelCondition.STDERR_DATA;

	private KnownHosts _knownHostsDb = new KnownHosts();

	private static final String _homeDir = System.getProperty("user.home", "");

	private static final String _knownHostPath = _homeDir + File.separator + ".ssh" + File.separator + "known_hosts";
	private static final String _idDSAPath     = _homeDir + File.separator + ".ssh" + File.separator + "id_dsa";
	private static final String _idRSAPath     = _homeDir + File.separator + ".ssh" + File.separator + "id_rsa";

	private WaitForExecDialog _waitforDialog = null;
//	private static Component         _guiOwner = null;
	private Component         _guiOwner = null;

	public static String getRsaKeyFilename() { return _idRSAPath; }

	public static final String  PROPKEY_sshAuthenticateEnableKeyboardInteractive = "ssh.authenticate.enable.KeyboardInteractive";
	public static final boolean DEFAULT_sshAuthenticateEnableKeyboardInteractive = true;

	public static final String  PROPKEY_sshAuthenticateEnableDSA                 = "ssh.authenticate.enable.DSA";
	public static final boolean DEFAULT_sshAuthenticateEnableDSA                 = true;

	public static final String  PROPKEY_sshAuthenticateEnableRSA                 = "ssh.authenticate.enable.RSA";
	public static final boolean DEFAULT_sshAuthenticateEnableRSA                 = true;

	public static final String PROMPT_FOR_PASSWORD = "<PROMPT_FOR_PASSWORD>";

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

		// enable logging?
		boolean enableLogging = Configuration.getCombinedConfiguration().getBooleanProperty("SshConnection.logging.enable", false);
		if (enableLogging)
		{
			ch.ethz.ssh2.log.Logger.enabled = true;
		}
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
	
	protected Connection getConnection()
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
		// Check that user, password and hostname is set 
		if (StringUtil.isNullOrBlank(_username)) throw new IllegalArgumentException("Trying to connect to a SSH host, but 'username' fields is net yet given.");
		if (StringUtil.isNullOrBlank(_hostname)) throw new IllegalArgumentException("Trying to connect to a SSH host, but 'hostname' fields is net yet given.");

		if (StringUtil.isNullOrBlank(_password) && StringUtil.isNullOrBlank(_keyFile)) throw new IllegalArgumentException("Trying to connect to a SSH host, but 'password' or 'sshKeyFile' fields is net yet given.");

		// Create a connection instance if none exists.
		if (_conn == null)
			_conn = new Connection(_hostname, _port);

		// And connect to the host
		File hostKeyFile = new File(_knownHostPath);
		if (hostKeyFile.exists())
		{
			try 
			{ 
				_knownHostsDb.addHostkeys(hostKeyFile); 

				String[] hostkeyAlgos = _knownHostsDb.getPreferredServerHostkeyAlgorithmOrder(_hostname);
				if (hostkeyAlgos != null)
					_conn.setServerHostKeyAlgorithms(hostkeyAlgos);
			}
			catch (IOException ex) 
			{ 
				logInfoMsg("SSH Problems reading the 'host-key-database' from file '"+hostKeyFile+"'. Caught: "+ex);
				_knownHostsDb = null;
			}
		}

		if (StringUtil.hasValue(_keyFile))
		{
			File f = new File(_keyFile);
			if ( ! f.exists() )
			{
				throw new FileNotFoundException("The SSH Key File '"+f+"' did NOT exists.");
			}
		}

		
		if (_waitforDialog != null)
			_waitforDialog.setState("SSH Connecting to host '"+_hostname+"' on port "+_port+" with username '"+_username+"'.");

		if (_knownHostsDb != null)
			_conn.connect(new AdvancedVerifier());
		else
			_conn.connect();

		// Authenticate
		if (_waitforDialog != null)
			_waitforDialog.setState("SSH Authenticating the Connection, in order: 'publickey', 'keyboard-interactive' and 'password'");
//		_isAuthenticated = _conn.authenticateWithPassword(_username, _password);
		_isAuthenticated = authenticate();

		// Get out of here, if not successful authentication
		if (_isAuthenticated == false)
			throw new IOException("Authentication failed to host='"+_hostname+"', on port='"+_port+"', with username='"+_username+"'.");

		_logger.info("Just Connected to SSH host '"+_hostname+"' on port '"+_port+"' with user '"+_username+"'.");


		// Try to get what OS we connected to
		getOsInfo();

		// Try to get number of procs (scheduble units on this os)
		getNproc();

		_logger.info("The host SSH host '"+_hostname+"' has '"+getOsName()+"' as it's Operating System (nproc="+_nproc+"). My guess is that it's using character set '"+getOsCharset()+"'.");

		_isConnected = true;
		return true;
	}
	
	private boolean authenticate()
//	throws IOException
	throws Exception
	{
		boolean enableKeyboardInteractive = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sshAuthenticateEnableKeyboardInteractive, DEFAULT_sshAuthenticateEnableKeyboardInteractive);;
		boolean enableDSA                 = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sshAuthenticateEnableDSA,                 DEFAULT_sshAuthenticateEnableDSA);
		boolean enableRSA                 = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sshAuthenticateEnableRSA,                 DEFAULT_sshAuthenticateEnableRSA);

		String lastError = null;
		
		boolean useSshKeyFile = false;
		if (StringUtil.hasValue(_keyFile))
		{
			useSshKeyFile = new File(_keyFile).exists();
		}

		while (true)
		{
			if (_conn.isAuthMethodAvailable(_username, "publickey") && (useSshKeyFile || enableDSA || enableRSA))
				logInfoMsg("SSH Authentication method 'publickey': is available, and will be tested first.");


			//--------------------------------------------------
			// User Supplied SSH Key File
			//--------------------------------------------------
			if (useSshKeyFile && _conn.isAuthMethodAvailable(_username, "publickey"))
			{
				File key = new File(_keyFile);
				
				logInfoMsg("SSH Authentication method 'publickey': Trying User Supplied SSH Key File '"+key+"'.");
				
				try
				{
					boolean res = _conn.authenticateWithPublicKey(_username, key, _password);

					if (res == true)
					{
						logInfoMsg("SSH Authentication method 'publickey' with User Supplied SSH Key File '"+key+"': SUCCEEDED");
						break;
					}

					lastError = "User Supplied SSH Key File '"+key+"' authentication FAILED.";

					logInfoMsg("SSH Authentication method 'publickey': "+lastError);

					useSshKeyFile = false; // do not try again
					
				}
				catch (Exception ex)
				{
					logInfoMsg("SSH Authentication method 'publickey': User Supplied SSH Key File '"+key+"' authentication FAILED. Caught Exception: " + ex);

					useSshKeyFile = false; // do not try again
				}
			}
			
			
			//--------------------------------------------------
			// Default RSA or DSA Key File
			//--------------------------------------------------
			if ((enableDSA || enableRSA) && _conn.isAuthMethodAvailable(_username, "publickey"))
			{
				if (enableRSA)
				{
					File privKey = new File(_idRSAPath);
					File pubKey  = new File(_idRSAPath + ".pub");

					if (privKey.exists())
					{
						logInfoMsg("SSH Authentication method 'publickey': Trying Default RSA using key file '"+privKey+"'.");

						try
						{
//							EnterSomethingDialog esd = new EnterSomethingDialog(null, "RSA Authentication",
//							new String[] { lastError, "Enter RSA private key password:" }, true);
//							esd.setVisible(true);

//							boolean res = _conn.authenticateWithPublicKey(_username, key, esd.answer);
							boolean res = _conn.authenticateWithPublicKey(_username, privKey, _password);

							if (res == true)
							{
								logInfoMsg("SSH Authentication method 'publickey': Default RSA: SUCCEEDED");
								break;
							}

							lastError = "Default RSA authentication FAILED.";
							logInfoMsg("SSH Authentication method 'publickey': "+lastError);

							// Print help message how to implement PUBLIC KEY Authentication
							try {
								if (pubKey.exists())
								{
									String keyType    = "RSA";
	    							String keyContent = FileUtils.readFileToString(pubKey, Charset.defaultCharset()).trim();
	    							String addKeyMethodHelpText = "To use the " + keyType + " Public Key, add the following '" + keyContent + "' as a new row in file '~" + _username + "/.ssh/authorized_keys' at server '" + _hostname + "'. Note: the 'authorized_keys' file needs ta have '-rw-------' (chmod 600 ~/.ssh/authorized_keys) authorization.";
	    
	    							logInfoMsg(addKeyMethodHelpText);
								}
							}
							catch (IOException ignore) {}
						}
						catch (Exception ex)
						{
							logInfoMsg("SSH Authentication method 'publickey': Default RSA authentication FAILED. Caught Exception: " + ex);
						}						
					}
					else
					{
						logInfoMsg("Skipping: SSH Authentication method 'publickey': Default RSA Key File '"+privKey+"' not found.");
					}
					enableRSA = false; // do not try again
				}

				if (enableDSA)
				{
					File privKey = new File(_idDSAPath);
					File pubKey  = new File(_idDSAPath + ".pub");

					if (privKey.exists())
					{
						logInfoMsg("SSH Authentication method 'publickey': Trying Default DSA using key file '"+privKey+"'.");
						
						try
						{
//							EnterSomethingDialog esd = new EnterSomethingDialog(null, "DSA Authentication",
//							new String[] { lastError, "Enter DSA private key password:" }, true);
//							esd.setVisible(true);

//							boolean res = _conn.authenticateWithPublicKey(_username, key, esd.answer);

							boolean res = _conn.authenticateWithPublicKey(_username, privKey, _password);

							if (res == true)
							{
								logInfoMsg("SSH Authentication method 'publickey': Default DSA: SUCCEEDED");
								break;
							}

							lastError = "Default DSA authentication FAILED.";
							logInfoMsg("SSH Authentication method 'publickey': "+lastError);

							// Print help message how to implement PUBLIC KEY Authentication
							try {
								if (pubKey.exists())
								{
									String keyType    = "DSA";
	    							String keyContent = FileUtils.readFileToString(pubKey, Charset.defaultCharset()).trim();
	    							String addKeyMethodHelpText = "To use the " + keyType + " Public Key, add the following '" + keyContent + "' as a new row in file '~" + _username + "/.ssh/authorized_keys' at server '" + _hostname + "'. Note: the 'authorized_keys' file needs ta have '-rw-------' (chmod 600 ~/.ssh/authorized_keys) authorization.";
	    
	    							logInfoMsg(addKeyMethodHelpText);
								}
							}
							catch (IOException ignore) {}
						}
						catch (Exception ex)
						{
							logInfoMsg("SSH Authentication method 'publickey': Default DSA authentication FAILED. Caught Exception: " + ex);
						}
					}
					else
					{
						logInfoMsg("Skipping: SSH Authentication method 'publickey': Default DSA Key File '"+privKey+"' not found.");
					}
					enableDSA = false; // do not try again
				}

				continue;
			}

			//--------------------------------------------------
			// keyboard-interactive
			//--------------------------------------------------
			if (enableKeyboardInteractive && _conn.isAuthMethodAvailable(_username, "keyboard-interactive"))
			{
				logInfoMsg("SSH Authentication method 'keyboard-interactive': Trying...");

				InteractiveLogic il = new InteractiveLogic(lastError);

				boolean res = _conn.authenticateWithKeyboardInteractive(_username, il);

				if (res == true)
				{
					logInfoMsg("SSH Authentication method 'keyboard-interactive': SUCCEEDED");
					break;
				}

				if (il.getPromptCount() == 0)
				{
					// aha. the server announced that it supports "keyboard-interactive", but when
					// we asked for it, it just denied the request without sending us any prompt.
					// That happens with some server versions/configurations.
					// We just disable the "keyboard-interactive" method and notify the user.

					lastError = "Keyboard-interactive does not work.";
					
					logInfoMsg("SSH Authentication method 'keyboard-interactive': "+lastError);

					enableKeyboardInteractive = false; // do not try this again
				}
				else
				{
					lastError = "Keyboard-interactive auth failed."; // try again, if possible

					logInfoMsg("SSH Authentication method 'keyboard-interactive': "+lastError);
				}

				continue;
			}

			//--------------------------------------------------
			// Password
			//--------------------------------------------------
			if (_conn.isAuthMethodAvailable(_username, "password"))
			{
				logInfoMsg("SSH Authentication method 'password': Trying...");

				boolean res = false;
				if (PROMPT_FOR_PASSWORD.equals(_password))
				{
					try
					{
						// Prompt for password
						String promptPasswd = PromptForPassword.show(null, "Please specify the Password for SSH connection to '"+_hostname+"'.", _hostname, _username, SaveType.TO_CONFIG_USER_TEMP, "unused");

						// Authenticate
						res = _conn.authenticateWithPassword(_username, promptPasswd);
						
						if (res)
							_password = promptPasswd;
					}
					catch (HeadlessException ex) 
					{
						throw new IOException("No password was supplied or '<PROMPT_FOR_PASSWORD>' was specified for user '" + _username + "', also check that the user exists at the remote host '" + _hostname + "'. And we are in NO-GUI mode... So I can not prompt for password.", ex);
					}
				}
				else
				{
					// Use the already specified passord
					res = _conn.authenticateWithPassword(_username, _password);

					if ( ! res )
						_password = PROMPT_FOR_PASSWORD;
				}

				if (res == true)
				{
					logInfoMsg("SSH Authentication method 'password': SUCCEEDED");
					break;
				}

				lastError = "Password authentication failed."; // try again, if possible

				logInfoMsg("SSH Authentication method 'password': "+lastError);

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
	 * This ServerHostKeyVerifier asks the user on how to proceed if a key cannot be found
	 * in the in-memory database.
	 */
	class AdvancedVerifier implements ServerHostKeyVerifier
	{
		@Override
		public boolean verifyServerHostKey(String hostname, int port, String serverHostKeyAlgorithm, byte[] serverHostKey) throws Exception
		{
			// If we cant provide a GUI simply say YES
			if (GraphicsEnvironment.isHeadless()) 
			{
				return true;
			}
			
			final String host = hostname;
			final String algo = serverHostKeyAlgorithm;

			String message;

			/* Check database */
			int result = _knownHostsDb.verifyHostkey(hostname, serverHostKeyAlgorithm, serverHostKey);

			switch (result)
			{
			case KnownHosts.HOSTKEY_IS_OK:
				return true;

			case KnownHosts.HOSTKEY_IS_NEW:
				message = "Do you want to accept the hostkey (type " + algo + ") from " + host + " ?\n";
				break;

			case KnownHosts.HOSTKEY_HAS_CHANGED:
				message = "WARNING! Hostkey for " + host + " has changed!\nAccept anyway?\n";
				break;

			default:
				throw new IllegalStateException();
			}

			/* Include the fingerprint in the message */
			String hexFingerprint = KnownHosts.createHexFingerprint(serverHostKeyAlgorithm, serverHostKey);
			String bubblebabbleFingerprint = KnownHosts.createBubblebabbleFingerprint(serverHostKeyAlgorithm, serverHostKey);

			message += "Hex Fingerprint: " + hexFingerprint + "\nBubblebabble Fingerprint: " + bubblebabbleFingerprint;

			String htmlMsg 
				= "<html>"
				+ "<h2>Question: When establishing a SSH Connection</h2>"
				+ "<b>Host Key database, needed to be updated. </b><br>"
				+ "Host Key database File: <code>" + _knownHostPath + "</code><br>"
				+ "<br>"
				+ message.replace("\n", "<br>")
				+ "</html>";

			/* if we have a "parent" GUI --- Now ask the user for input */
			Component parentComponent = hasWaitForDialog() ? getWaitForDialog() : getGuiOwner();
			int choice;
//			if (parentComponent == null)
//				parentComponent = FocusManager.getCurrentManager().getActiveWindow();//KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();

			if (parentComponent != null)
				choice = JOptionPane.showConfirmDialog( parentComponent, htmlMsg);
			else
			{
				choice = JOptionPane.YES_OPTION;
				_logger.warn("SSH Host Key database was updated without user interaction, since 'parentComponent' was null. adding entry for host '"+hostname+"' to file '"+_knownHostPath+"'.");
			}
			
			if (choice == JOptionPane.YES_OPTION)
			{
				/* Be really paranoid. We use a hashed hostname entry */
				String hashedHostname = KnownHosts.createHashedHostname(hostname);

				/* Add the hostkey to the in-memory database */
				_knownHostsDb.addHostkey(new String[] { hashedHostname }, serverHostKeyAlgorithm, serverHostKey);

				/* Also try to add the key to a known_host file */
				try
				{
					File hostKeyFile = new File(_knownHostPath);
					
					// Create directory/directories to hold the 'known_hosts' if the file dosn't exists
					File sshDir = hostKeyFile.getParentFile();
					if ( sshDir != null && !sshDir.exists() )
						sshDir.mkdirs();

					// Add the key to the file.
					KnownHosts.addHostkeyToFile(hostKeyFile, new String[] { hashedHostname }, serverHostKeyAlgorithm, serverHostKey);

					logInfoMsg("SSH Added '"+hostname+"' to the HostKey file '"+hostKeyFile+"'.");
				}
				catch (IOException ex)
				{
					logInfoMsg("SSH Problems Adding '"+hostname+"' to the HostKey file '"+_knownHostPath+"'. Exception: "+ex);
				}

				return true;
			}

			if (choice == JOptionPane.CANCEL_OPTION)
			{
				logInfoMsg("The user aborted the server hostkey verification.");
				throw new Exception("The user aborted the server hostkey verification.");
			}

			return false;
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
//	throws IOException
	throws Exception
	{
		if (_isAuthenticated == false)
			throw new IOException("Can't do reconnect yet, you need to have a valid connection first. This means that you need to connect with a successful authentication first.");

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
		{
			_isConnected = false;
		}
		else
		{
			try 
			{
				_logger.debug("isConnected(): SEND IGNORE PACKET to SSH Server.");
				_conn.sendIgnorePacket();
			}
			catch (IOException e) 
			{
//				_logger.info("isConnected() has problems when sending a 'ignore packet' to the SSH Server. The connection will be closed. sendIgnorePacket Caught: "+e);
//				close();
				// Or poosibly: do reconnect() here instead...

				_logger.warn("isConnected() has problems when sending a 'ignore packet' to the SSH Server. Lets try to re-connect to the server. sendIgnorePacket Caught: "+e);
				try 
				{
					reconnect();
				}
				catch(Exception ex)
				{
					_logger.warn("isConnected() has problems when trying to re-connect. Caught: "+ex);
					// Not sure what to do here... leave the connection or close the connection
					// reconnect() does a close... before it tries to do connect(). hopefully thats good enough...
				}
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
//	throws IOException
	throws Exception
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
//	throws IOException
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
	synchronized public Session execCommand(String command, boolean requestPty) 
//	throws IOException
	throws Exception
	{
		if (_conn == null)
		{
			throw new IOException("The SSH connection to the host '"+_hostname+"' was null. The connection has not been initialized OR someone has closed the connection.");
		}

		try
		{
			Session sess = _conn.openSession();
			_logger.debug("Executing command '"+command+"' on connection: "+toString());
			
			// SSHD On Windows do not close "long running" commands on the server side on disconnect
			// see: https://github.com/PowerShell/Win32-OpenSSH/issues/1751
			// The workaround seems to be "ssh -t user@ip" where the "-t" is to request a Terminal
			if (requestPty)
				sess.requestDumbPTY();
			
			// Now execute the command
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
				throw new IOException("The SSH connection to the host '"+_hostname+"' was null. The connection has not been initialized OR someone has closed the connection.");
			}

			Session sess = _conn.openSession();
			sess.execCommand(cmd);

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

			br.close();
			sess.close();

			str = output;
			_nproc = StringUtil.parseInt(str, 0);
		}
		catch (Exception e)
		{
			_nproc = 0;
			_logger.info("Problems executing command '"+cmd+"'. retStr='"+str+"', Caught: "+e);
		}
		return _nproc;
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

		// Check what OS we ended up in
		String output = "";
		{
			Session sess = _conn.openSession();
//			sess.execCommand("uname -a");
			sess.execCommand("uname");
			
			// TODO: Possibly also use 'ver' to check if it's a DOS/Windows system

			InputStream stdout = new StreamGobbler(sess.getStdout());
			BufferedReader br = new BufferedReader(new InputStreamReader(stdout));

			while (true)
			{
				String line = br.readLine();
				if (line == null)
					break;

				output += line;
			}

			br.close();
			sess.close();
		}
		
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
			Session sess = _conn.openSession();
			sess.execCommand("(dir 2>&1 *`|echo CMD);&<# rem #>echo ($PSVersionTable).PSEdition"); 

			InputStream stdout = new StreamGobbler(sess.getStdout());
			BufferedReader br = new BufferedReader(new InputStreamReader(stdout));

			while (true)
			{
				String line = br.readLine();
				if (line == null)
					break;

				output += line;
			}

			br.close();
			sess.close();
			
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
				_osName = sa[0];

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
		//System.out.println("getOsInfo: osName='"+_uname+"', chartset='"+_osCharset+"'.");

		return output;
	}

	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	private String linuxToJavaCharset()
	throws IOException
	{
		if (_conn == null)
		{
			throw new IOException("The SSH connection to the host '"+_hostname+"' was null. The connection has not been initialized OR someone has closed the connection.");
		}

		// Check what OS we ended up in
		String output = "";
		{
			Session sess = _conn.openSession();
			sess.execCommand("echo ${LC_ALL:-${LC_CTYPE:-${LANG}}}");

			InputStream stdout = new StreamGobbler(sess.getStdout());
			BufferedReader br = new BufferedReader(new InputStreamReader(stdout));

			while (true)
			{
				String line = br.readLine();
				if (line == null)
					break;

				output += line;
			}

			br.close();
			sess.close();
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
	 * 
	 * @return
	 * @throws IOException
	 */
	private String windowsToJavaCharset()
	throws IOException
	{
		if (_conn == null)
		{
			throw new IOException("The SSH connection to the host '"+_hostname+"' was null. The connection has not been initialized OR someone has closed the connection.");
		}

		// Check what OS we ended up in
		String output = "";
		{
			Session sess = _conn.openSession();
			sess.execCommand("chcp");

			InputStream stdout = new StreamGobbler(sess.getStdout());
			BufferedReader br = new BufferedReader(new InputStreamReader(stdout));

			while (true)
			{
				String line = br.readLine();
				if (line == null)
					break;

				output += line;
			}

			br.close();
			sess.close();
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

			br.close();
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

		br.close();
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

		br.close();
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

		BufferedReader stdout_br = new BufferedReader(new InputStreamReader(new StreamGobbler(sess.getStdout())));
		BufferedReader stderr_br = new BufferedReader(new InputStreamReader(new StreamGobbler(sess.getStderr())));

		boolean hasVeritasMsg = false;

		// Read output (probably on stderr)
		// expected string if vxstat is available
		// VxVM vxstat ERROR V-5-1-15324 Specify a disk group with -g <diskgroup> or configure a default disk group. Refer to the vxdctl(1m) man page for more details on configuring a default disk group
		while (true)
		{
			String line = stdout_br.readLine();
			if (line == null)
				break;

			if (line.indexOf("VxVM vxstat ERROR") >= 0)
				hasVeritasMsg = true;
			
			_logger.debug("hasVeritas() stdout: "+line);
		}

		while (true)
		{
			String line = stderr_br.readLine();
			if (line == null)
				break;

			if (line.indexOf("VxVM vxstat ERROR") >= 0)
				hasVeritasMsg = true;
			
			_logger.debug("hasVeritas() stderr: "+line);
		}

		boolean retStatus = true;
		Integer exitCode = sess.getExitStatus();
		if ( exitCode == null || exitCode != null && exitCode != 0 )
			retStatus = false;

		stdout_br.close();
		stderr_br.close();
		sess.close();

		_logger.debug("hasVeritas(): returned " + (retStatus || hasVeritasMsg) );
		return retStatus || hasVeritasMsg;
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
			throw new Exception("Unsupported utility of '"+utilType+"'.");
	
		if (isClosed())
			throw new IOException("SSH is not connected. (host='"+_hostname+"', port="+_port+", user='"+_username+"', osName='"+_osName+"', osCharset='"+_osCharset+"'.)");

		Session sess = _conn.openSession();
		sess.execCommand(cmd);

		BufferedReader stdout_br = new BufferedReader(new InputStreamReader(new StreamGobbler(sess.getStdout())));
		BufferedReader stderr_br = new BufferedReader(new InputStreamReader(new StreamGobbler(sess.getStderr())));

		int intVersion = -1;
		String usedVersionString = null;
		// Read output (probably on stdout)
		while (true)
		{
			String line = stdout_br.readLine();
			if (line == null)
				break;

			int i = VersionShort.parse(line);
			_logger.debug("getLinuxUtilVersion() stdout: '"+line+"'. VersionShort.parse() returned: "+i);
//System.out.println("getLinuxUtilVersion() stdout: '"+line+"'. VersionShort.parse() returned: "+i);

			if (i >= 0)
			{
				intVersion = i;
				usedVersionString = line;
			}
		}

		while (true)
		{
			String line = stderr_br.readLine();
			if (line == null)
				break;

			int i = VersionShort.parse(line);
			_logger.debug("getLinuxUtilVersion() stderr: '"+line+"'. VersionShort.parse() returned: "+i);
//System.out.println("getLinuxUtilVersion() stderr: '"+line+"'. VersionShort.parse() returned: "+i);

			if (i >= 0)
			{
				intVersion = i;
				usedVersionString = line;
			}
		}

		_logger.info("When issuing command '"+cmd+"' the version "+intVersion+" was parsed from the version string '"+StringUtil.removeLastNewLine(usedVersionString)+"'.");
//		Integer exitCode = sess.getExitStatus();

		stdout_br.close();
		stderr_br.close();
		sess.close();

//		if ( exitCode == null || exitCode != null && exitCode != 0 )
//			return -1;

		_logger.debug("getLinuxUtilVersion(): returned " + intVersion );
//System.out.println("getLinuxUtilVersion(): returned " + intVersion );
		return intVersion;
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

