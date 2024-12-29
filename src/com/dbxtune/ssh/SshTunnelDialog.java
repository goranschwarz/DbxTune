/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.ssh;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.net.BindException;
import java.net.ServerSocket;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.apache.log4j.Logger;

import com.dbxtune.Version;
import com.dbxtune.gui.swing.MultiLineLabel;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class SshTunnelDialog
extends JDialog
implements ActionListener, KeyListener, FocusListener
{
	private static final long serialVersionUID = 1L;
	private static Logger _logger = Logger.getLogger(SshTunnelDialog.class);
	
	private static final String PROP_PREFIX = "conn.ssh.tunnel.dialog.";

	private SshTunnelInfo      _sshTunelInfo         = null;
	private String             _hostPortStr          = "";

	private static final int   PANEL_LEFT_PREF_WIDTH  = 390; 
	private static final int   PANEL_RIGHT_PREF_WIDTH = 480; 

	private ImageIcon          _sshLocalDestImageIcon = SwingUtils.readImageIcon(Version.class, "images/login_key.gif");
	private JLabel             _sshLocalDestIcon     = new JLabel(_sshLocalDestImageIcon);
	private MultiLineLabel     _sshLocalDestHelp     = new MultiLineLabel("Local and Destination Host, Port numbers.");
	private JLabel             _sshLocalHost_lbl     = new JLabel("Local Hostname");
	private JTextField         _sshLocalHost_txt     = new JTextField("localhost");
	private JLabel             _sshLocalPort_lbl     = new JLabel("Local Port");
	private JTextField         _sshLocalPort_txt     = new JTextField();
	private JCheckBox          _sshLocalPortRand_chk = new JCheckBox("Generate a Port Number", true);
	private JButton            _sshLocalPort_but     = new JButton("Check");
	private JLabel             _sshDestHost_lbl      = new JLabel("Destination Hostname");
	private JTextField         _sshDestHost_txt      = new JTextField();
	private JLabel             _sshDestPort_lbl      = new JLabel("Destination Port");
	private JTextField         _sshDestPort_txt      = new JTextField();

	private ImageIcon          _sshServerImageIcon = SwingUtils.readImageIcon(Version.class, "images/server_32.png");
	private JLabel             _sshServerIcon      = new JLabel(_sshServerImageIcon);
	private MultiLineLabel     _sshServerHelp      = new MultiLineLabel("Specify host name to the machine where you want to use as an intermediate server. The connection will be using SSH (Secure Shell), which normally is listening on port 22. ");
	private JLabel             _sshServerName_lbl  = new JLabel();

	private JLabel             _sshUser_lbl        = new JLabel("User name");
	private JTextField         _sshUser_txt        = new JTextField();
	private JLabel             _sshPasswd_lbl      = new JLabel("Password");
	private JTextField         _sshPasswd_txt      = null; // set to JPasswordField or JTextField depending on debug level
	private JLabel             _sshHost_lbl        = new JLabel("Host Name");
	private JTextField         _sshHost_txt        = new JTextField();
	private JLabel             _sshPort_lbl        = new JLabel("Port Number");
	private JTextField         _sshPort_txt        = new JTextField("22");

	private JCheckBox          _sshOptionSavePwd_chk = new JCheckBox("Save password", true);
	
	private JLabel             _sshKeyFile_lbl     = new JLabel("Key File");
	private JTextField         _sshKeyFile_txt     = new JTextField("");
	private JButton            _sshKeyFile_but     = new JButton("...");

	private JLabel             _sshInitOsCmd_lbl   = new JLabel("Init OS Cmd");
	private JTextField         _sshInitOsCmd_txt   = new JTextField("");

	private ImageIcon          _sshTunnelHelpImageIcon  = SwingUtils.readImageIcon(Version.class, "images/sshSessionForwarding.png");
	private JLabel             _sshTunnelHelpIcon       = new JLabel(_sshTunnelHelpImageIcon);

	//---- Buttons at the bottom
	private JLabel             _ok_lbl         = new JLabel(""); // Problem description if _ok is disabled
	private JButton            _ok             = new JButton("OK");
	private JButton            _cancel         = new JButton("Cancel");

	
	public SshTunnelDialog(JDialog owner, String hostPortStr)
	{
		super(owner, "SSH Tunnel Settings", true);
		setHostPortStr(hostPortStr);
		init(owner);
	}

	public SshTunnelInfo getSshTunnelInfo()
	{
		return _sshTunelInfo;
	}

	public void setHostPortStr(String hostPortStr)
	{
		_hostPortStr = hostPortStr;

		List<String> hostPostList = StringUtil.parseCommaStrToList(hostPortStr);
		for (String str : hostPostList)
		{
			String host = "";
			String port = "";
			String[] sa = str.split(":");
			if (sa.length > 0) host = sa[0];
			if (sa.length > 1) port = sa[1];

			// skip localhost entries
			if (host.equals("localhost") || host.equals("127.0.0.1"))
				continue;

			_sshDestHost_txt.setText(host);
			_sshDestPort_txt.setText(port);

			if ( ! _sshLocalPortRand_chk.isSelected() )
				_sshLocalPort_txt.setText(port);

			break;
		}
	}

	private void init(JDialog owner)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("insets 0 0 0 0, wrap 1", "", ""));   // insets Top Left Bottom Right

		panel.add(createSsh(),           "height 100%, grow, push, wrap");
		panel.add(createOkCancelPanel(), "bottom, right");

		loadProps();

		setContentPane(panel);
		pack();
		
		SwingUtils.centerWindow(this);
		
		// If Generate port number, get first free port
		if (_sshLocalPortRand_chk.isSelected())
		{
			int freePort = SshTunnelManager.getFirstFreeLocalPortNumber();
			if (freePort >= 0)
			{
				_sshLocalPort_txt.setText( Integer.toString(freePort) ); 
				_sshLocalPort_txt.setEditable(false); 
			}
		}
		
		getSavedWindowProps();
		
		validateContents();
	}

	private JPanel createSsh()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("flowy", "grow"));

		panel.add(createSshLocalDestPanel(), "split, grow");
		panel.add(createSshServerPanel(),    "grow, wrap");
		panel.add(createSshInfoPanel(),      "grow");

		return panel;
	}

	private JPanel createOkCancelPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("","",""));   // insets Top Left Bottom Right

		_ok_lbl.setForeground(Color.RED);
		_ok_lbl.setFont( _ok_lbl.getFont().deriveFont(Font.BOLD) );
		_ok_lbl.setText("");

		// ADD the OK, Cancel, Apply buttons
		panel.add(_ok_lbl, "left");
		panel.add(_ok,     "tag ok, right");
		panel.add(_cancel, "tag cancel");

		// ADD ACTIONS TO COMPONENTS
		_ok    .addActionListener(this);
		_cancel.addActionListener(this);

		return panel;
	}

	private JPanel createSshLocalDestPanel()
	{
		JPanel panel = SwingUtils.createPanel("Local and Destination Host and Port information", true);
		panel.setLayout(new MigLayout("wrap 2","",""));   // insets Top Left Bottom Right

		_sshLocalHost_lbl.setToolTipText("Hostname where to but up a listener interface, This would NORMALLY be 'localhost' or '127.0.0.1'.");
		_sshLocalHost_txt.setToolTipText(_sshLocalHost_lbl.getToolTipText());
		_sshLocalPort_lbl.setToolTipText("Port number on the local machine we should start a listener on");
		_sshLocalPort_txt.setToolTipText(_sshLocalPort_lbl.getToolTipText());
		_sshLocalPortRand_chk.setToolTipText(
			"<html>" +
			   "Generate a new random number every time the listener is started.<br>" +
			   "<br>" +
			   "This is done by starting too check for free port number at '"+SshTunnelManager.GENERATE_PORT_NUMBER_START+"' then looping until it finds a free port number." +
			"</html>");
		_sshLocalPort_but.setToolTipText("Check if the current 'Local Port' is availiable for start a listener service on.");

		_sshDestHost_lbl.setToolTipText("This is the IP Adress or host name where the ASE Server is running on");
		_sshDestHost_txt.setToolTipText(_sshDestHost_lbl.getToolTipText());
		_sshDestPort_lbl.setToolTipText("This is the Port number where the ASE Server is running on on the destination host.");
		_sshDestPort_txt.setToolTipText(_sshDestPort_lbl.getToolTipText());

		panel.add(_sshLocalDestIcon,     "");
		panel.add(_sshLocalDestHelp,     "wmin 100, pushx, growx");

		panel.add(_sshLocalHost_lbl,     "");
		panel.add(_sshLocalHost_txt,     "pushx, growx");

		panel.add(_sshLocalPort_lbl,     "");
		panel.add(_sshLocalPort_txt,     "split, pushx, growx");
		panel.add(_sshLocalPortRand_chk, "");
		panel.add(_sshLocalPort_but,     "wrap");

		panel.add(_sshDestHost_lbl,      "");
		panel.add(_sshDestHost_txt,      "pushx, growx");

		panel.add(_sshDestPort_lbl,      "");
		panel.add(_sshDestPort_txt,      "pushx, growx");

		// set non editable fields
		_sshLocalHost_txt.setEditable(false);

		// just for validation
		_sshLocalHost_txt.addKeyListener(this);
		_sshLocalPort_txt.addKeyListener(this);
		_sshDestHost_txt .addKeyListener(this);
		_sshDestPort_txt .addKeyListener(this);
		
		// KEY ACTION LISTENERS
		_sshLocalPort_txt.addKeyListener(this);
		_sshDestPort_txt .addKeyListener(this);

		// ADD ACTION LISTENERS
		_sshLocalPort_but    .addActionListener(this);
		_sshLocalPortRand_chk.addActionListener(this);

		// ADD FOCUS LISTENERS
		_sshLocalHost_txt.addFocusListener(this);
		_sshLocalPort_txt.addFocusListener(this);
		_sshDestHost_txt .addFocusListener(this);
		_sshDestPort_txt .addFocusListener(this);

		// Set minimum/preferred size
		Dimension preSize = panel.getPreferredSize();
		preSize.width = PANEL_LEFT_PREF_WIDTH;
		panel.setMinimumSize(preSize);
		panel.setPreferredSize(preSize);

		return panel;
	}

	private JPanel createSshServerPanel()
	{
		JPanel panel = SwingUtils.createPanel("Specify the server to connect to", true);
		panel.setLayout(new MigLayout("wrap 2","",""));   // insets Top Left Bottom Right

		// Hide password or not...
		if (_logger.isDebugEnabled())
			_sshPasswd_txt = new JTextField();
		else
			_sshPasswd_txt = new JPasswordField();

		_sshUser_lbl  .setToolTipText("User name to use when logging in to the below Operating System Host.");
		_sshUser_txt  .setToolTipText("User name to use when logging in to the below Operating System Host.");
		_sshPasswd_lbl.setToolTipText("<html>"
		                               + "Password to use when logging in to the below Operating System Host<br>"
		                               + "<br>"
		                               + "If SSH Authentication model is 'publickey' and you have a password for the <i>private key file</i>, then type this password here.<br>"
		                               + "If the <i>private key file</i> does <b>not</b> have a password, just type <i>anything here</i> so the button is enabled.<br>"
		                               + "<br>"
		                               + "Note 1: To use 'publickey' authentication the file '"+SshConnection.getRsaKeyFilename()+"' is used.<br>"
		                               + "Note 2: PUTTY generated keys should also work.<br>"
//		                               + "Note 2: The above file needs to contain a key in the OpenSSH format.<br>"
//		                               + "(If you have a PUTTY generated it needs to be converted using <i>puttygen</i>, load the file, then: Menu -&gt; Convertion -&gt; Export OpenSSH Key)<br>"
//		                               + "<br>"
//		                               + "If 'publickey' authentication does <b>not</b> work, it can be disabled, by inserting:.<br>"
//		                               + "<code>"+SshConnection.PROPKEY_sshAuthenticateEnableRSA+" = false</code>.<br>"
//		                               + "<code>"+SshConnection.PROPKEY_sshAuthenticateEnableDSA+" = false</code>.<br>"
//		                               + "In the file: <code>"+Configuration.getInstance(Configuration.USER_TEMP).getFilename()+"</code><br>"
		                               + "</html>");
		_sshPasswd_txt.setToolTipText(_sshPasswd_lbl.getToolTipText());
		_sshOptionSavePwd_chk.setToolTipText("Save the password in the configuration file, and yes it's encrypted");
		
		_sshHost_lbl      .setToolTipText("<html>Hostname or IP address of the intermediate Host you are connecting to</html>");
		_sshHost_txt      .setToolTipText("<html>Hostname or IP address of the intermediate Host you are connecting to</html>");
		_sshPort_lbl      .setToolTipText("<html>Port number of the SSH server you are connecting to</html>");
		_sshPort_txt      .setToolTipText("<html>Port number of the SSH server you are connecting to</html>");
		_sshKeyFile_lbl   .setToolTipText("<html>Private Key File, if you want to use that to authenticate.</html>");
		_sshKeyFile_txt   .setToolTipText(_sshKeyFile_lbl.getToolTipText());
		_sshInitOsCmd_lbl .setToolTipText("<html>Execute OS command(s) after we have connected.<br>This is if you need to do setup some <i>stuff</i> or create a new <i>sub SSH tunnel</i> or do...</html>");
		_sshInitOsCmd_txt .setToolTipText(_sshInitOsCmd_lbl.getToolTipText());

		
		panel.add(_sshServerIcon,        "");
		panel.add(_sshServerHelp,        "wmin 100, pushx, growx");

		panel.add(_sshHost_lbl,          "");
		panel.add(_sshHost_txt,          "pushx, growx");

		panel.add(_sshPort_lbl,          "");
		panel.add(_sshPort_txt,          "pushx, growx");

		panel.add(_sshUser_lbl,          "");
		panel.add(_sshUser_txt,          "pushx, growx");

		panel.add(_sshPasswd_lbl,        "");
		panel.add(_sshPasswd_txt,        "pushx, growx");

		panel.add(_sshOptionSavePwd_chk, "skip");

		panel.add(_sshKeyFile_lbl,       "");
		panel.add(_sshKeyFile_txt,       "split, pushx, growx");
		panel.add(_sshKeyFile_but,       "wrap");

		panel.add(_sshInitOsCmd_lbl,     "");
		panel.add(_sshInitOsCmd_txt,     "pushx, growx");

		_sshServerName_lbl.setText(":");
		if (_logger.isDebugEnabled())
		{
			panel.add(_sshServerName_lbl, "skip, pushx, growx");
		}

		// Set some default values
		_sshUser_txt.setText(System.getProperty("user.name"));


		// KEY ACTION LISTENERS
		_sshPort_txt   .addKeyListener(this);
		// just for validation
		_sshUser_txt   .addKeyListener(this);
		_sshPasswd_txt .addKeyListener(this);
		_sshHost_txt   .addKeyListener(this);
		_sshPort_txt   .addKeyListener(this);
		_sshKeyFile_txt.addKeyListener(this);

		// ADD ACTION LISTENERS
		_sshHost_txt         .addActionListener(this);
		_sshPort_txt         .addActionListener(this);
		_sshUser_txt         .addActionListener(this);
		_sshPasswd_txt       .addActionListener(this);
		_sshOptionSavePwd_chk.addActionListener(this);
		_sshKeyFile_txt      .addActionListener(this);
		_sshKeyFile_but      .addActionListener(this);

		// ADD FOCUS LISTENERS
		_sshUser_txt   .addFocusListener(this);
		_sshPasswd_txt .addFocusListener(this);
		_sshHost_txt   .addFocusListener(this);
		_sshPort_txt   .addFocusListener(this);
		_sshKeyFile_txt.addFocusListener(this);
		
		// Set minimum/preferred size
		Dimension preSize = panel.getPreferredSize();
		preSize.width = PANEL_LEFT_PREF_WIDTH;
		panel.setMinimumSize(preSize);
		panel.setPreferredSize(preSize);

		return panel;
	}

	private JPanel createSshInfoPanel()
	{
		JPanel panel = SwingUtils.createPanel("SSH Tunnel Information", true);
		panel.setLayout(new MigLayout("wrap 1, gap 0","",""));   // insets Top Left Bottom Right

		String helpText = 
			"<html>" +
			"Creating a SSH Tunnel, is done by:" +
			"<ul>" +
			"  <li>Setting up a local listener port (in the example below 2955). </li>" +
			"  <li>The ASE Client connect to localhost:2955. </li>" +
			"  <li>The SSH connection tunnels/forwards the packets sent on localhost:2955 to the \"intermediate\" SSH Host (via port 22). </li>" +
			"  <li>The SSH Daemon then opens a connection to the Destination host:port and forwards the data traffic. </li>" +
			"</ul>" +
			"This means: as long you can connect to the \"intermediate\" SSH Host you can connect to a ASE located on <i>the other side</i>.<br>" +
			"</html>";
		JLabel helpText_lbl = new JLabel(helpText);
		
		panel.add(helpText_lbl,        "wrap");
		panel.add(_sshTunnelHelpIcon,  "");
//		panel.add(_sshLoginHelp,  "wmin 100, push, grow");

		// Set minimum/preferred size
		Dimension preSize = panel.getPreferredSize();
		preSize.width = PANEL_RIGHT_PREF_WIDTH;
		preSize.height = preSize.height + (14 * 3); // add 3 rows (HTML wraps on 3 lines, with a smaller preferred size)
		panel.setMinimumSize(preSize);
		panel.setPreferredSize(preSize);

		return panel;
	}

	@Override
	public void focusGained(FocusEvent e)
	{
	}

	@Override
	public void focusLost(FocusEvent e)
	{
		validateContents();
	}

	@Override
	public void keyTyped(KeyEvent keyevent)
	{
		validateContents();

		Object source = keyevent.getSource();
		
		// some fields should just be DIGITs
		if (source.equals(_sshDestPort_txt) || source.equals(_sshLocalPort_txt) || source.equals(_sshPort_txt) )
		{
			char ch = keyevent.getKeyChar();
			if ( ! Character.isDigit(ch) )
			{
				keyevent.consume();
				return;
			}
		}
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();
//		String action = e.getActionCommand();

		// --- BUTTON: OK ---
		if (_ok.equals(source))
		{
			SshTunnelInfo sshInfo = new SshTunnelInfo();
			sshInfo.setLocalPortGenerated(                 _sshLocalPortRand_chk.isSelected());
			sshInfo.setLocalHost         (                 _sshLocalHost_txt    .getText());
			sshInfo.setLocalPort         (Integer.parseInt(_sshLocalPort_txt    .getText()));
			sshInfo.setDestHost          (                 _sshDestHost_txt     .getText());
			sshInfo.setDestPort          (Integer.parseInt(_sshDestPort_txt     .getText()));

			sshInfo.setSshHost    (                 _sshHost_txt  .getText());
			sshInfo.setSshPort    (Integer.parseInt(_sshPort_txt  .getText()));
			sshInfo.setSshUsername(                 _sshUser_txt  .getText());
			sshInfo.setSshPassword(                 _sshPasswd_txt.getText());

			sshInfo.setSshKeyFile(                  _sshKeyFile_txt  .getText());
			sshInfo.setSshInitOsCmd(                _sshInitOsCmd_txt.getText());

			_sshTunelInfo = sshInfo;

			saveProps();
			
			setVisible(false);
		}
		
		// --- BUTTON: CANCEL ---
		if (_cancel.equals(source))
		{
			_sshTunelInfo = null;
			setVisible(false);
		}
		

		// --- CHECKBOX: Generate new port number ---
		if (_sshLocalPortRand_chk.equals(source))
		{
			if (_sshLocalPortRand_chk.isSelected())
			{
				int freePort = SshTunnelManager.getFirstFreeLocalPortNumber();
				if (freePort >= 0)
				{
					_sshLocalPort_txt.setText( Integer.toString(freePort) ); 
					_sshLocalPort_txt.setEditable(false); 
				}
			}
			else
			{
				//_sshLocalPort_txt.setText( _sshDestPort_txt.getText() );
				_sshLocalPort_txt.setEditable(true); 
			}
		}

		// --- BUTTON: CHECK LOCAL PORT  ---
		if (_sshLocalPort_but.equals(source) )
		{
			String localPortStr = _sshLocalPort_txt.getText();
			try
			{
				int localPort = Integer.parseInt(localPortStr);

				ServerSocket serverSocket = new ServerSocket(localPort);
				serverSocket.close();

				SwingUtils.showInfoMessage(this, "OK", "Port number '"+localPort+"' is free.");
			}
			catch (BindException ex)
			{
				SwingUtils.showErrorMessage(this, "FAILURE", "Port number '"+localPortStr+"' is bussy, choose another number.", ex);
			}
			catch (Throwable ex)
			{
				SwingUtils.showErrorMessage(this, "FAILURE", "Problems starting a listener on Port number '"+localPortStr+"', check the Exception below.", ex);
			}
		}
		
		// --- BUTTON: KEY FILE ...  ---
		if (_sshKeyFile_but.equals(source) )
		{
			String dir = System.getProperty("user.home") + File.separatorChar + ".ssh";

			JFileChooser fc = new JFileChooser(dir);
			int returnVal = fc.showOpenDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION) 
			{
				String filename = fc.getSelectedFile().getAbsolutePath();
				_sshKeyFile_txt.setText(filename);
			}
		}

		validateContents();
	}

	private void validateContents()
	{
		String problem = "";

		if ( ! _sshLocalPortRand_chk.isSelected())
		{
			if (_sshDestPort_txt.getText().trim().equals("")) 
				problem = "Destination Port must be specified";
		}

		if (_sshUser_txt.getText().trim().equals(""))
			problem = "SSH username must be specified";

		if (_sshPasswd_txt.getText().trim().equals(""))
			problem = "SSH password must be specified";

		if (_sshHost_txt.getText().trim().equals(""))
			problem = "SSH hostname must be specified";

		try { Integer.parseInt(_sshPort_txt.getText()); } 
		catch (NumberFormatException e) 
		{
			problem = "SSH Port number must be an integer";
		}

		
		_ok_lbl.setText(problem);

		if (problem.equals(""))
		{
			_ok.setEnabled(true);
		}
		else
		{
			_ok.setEnabled(false);
		}
	}


	/*---------------------------------------------------
	** BEGIN: implementing saveProps & loadProps
	**---------------------------------------------------
	*/	
	private void saveProps()
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null)
		{
			_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
			return;
		}
		
		// LOCAL *
		conf.setProperty(PROP_PREFIX +"local.port.generate."+_hostPortStr, _sshLocalPortRand_chk.isSelected());
		conf.setProperty(PROP_PREFIX +"local.port."+_hostPortStr,          _sshLocalPort_txt.getText());
		conf.getProperty(PROP_PREFIX +"local.host."+_hostPortStr,          _sshLocalHost_txt.getText());

		// DESTINATION *
		conf.setProperty(PROP_PREFIX +"destination.host."+_hostPortStr,    _sshDestHost_txt.getText());
		conf.setProperty(PROP_PREFIX +"destination.port."+_hostPortStr,    _sshDestPort_txt.getText());

		// Below, set basename as a fallback
		// HOSTNAME
		conf.setProperty(PROP_PREFIX +"ssh.conn.host."+_hostPortStr,       _sshHost_txt.getText());
		conf.setProperty(PROP_PREFIX +"ssh.conn.host",                     _sshHost_txt.getText());

		// PORT
		conf.setProperty(PROP_PREFIX +"ssh.conn.port."+_hostPortStr,       _sshPort_txt.getText());
		conf.setProperty(PROP_PREFIX +"ssh.conn.port",                     _sshPort_txt.getText());

		// USERNAME
		conf.setProperty(PROP_PREFIX +"ssh.conn.username."+_hostPortStr,   _sshUser_txt.getText());
		conf.setProperty(PROP_PREFIX +"ssh.conn.username",                 _sshUser_txt.getText());

		// PASSWORD
		conf.setProperty(PROP_PREFIX +"ssh.conn.password."+_hostPortStr,   _sshPasswd_txt.getText(), true);
		conf.setProperty(PROP_PREFIX +"ssh.conn.password",                 _sshPasswd_txt.getText(), true);

		// INIT KEY FILE
		conf.setProperty(PROP_PREFIX +"ssh.conn.keyFile."+_hostPortStr,    _sshKeyFile_txt.getText());
		conf.setProperty(PROP_PREFIX +"ssh.conn.keyFile",                  _sshKeyFile_txt.getText());

		// INIT OS CMD
		conf.setProperty(PROP_PREFIX +"ssh.conn.initOsCmd."+_hostPortStr,  _sshInitOsCmd_txt.getText());
		conf.setProperty(PROP_PREFIX +"ssh.conn.initOsCmd",                _sshInitOsCmd_txt.getText());


		//------------------
		// WINDOW
		//------------------
		conf.setLayoutProperty(PROP_PREFIX + "window.width",  this.getSize().width);
		conf.setLayoutProperty(PROP_PREFIX + "window.height", this.getSize().height);
		conf.setLayoutProperty(PROP_PREFIX + "window.pos.x",  this.getLocationOnScreen().x);
		conf.setLayoutProperty(PROP_PREFIX + "window.pos.y",  this.getLocationOnScreen().y);

		conf.save();
	}

	private void loadProps()
	{
		SshTunnelInfo ti = getSshTunnelInfo(_hostPortStr);

		_sshLocalPortRand_chk.setSelected(ti.isLocalPortGenerated());
		_sshLocalPort_txt.setText(ti.getLocalPort() +"");
		_sshLocalHost_txt.setText(ti.getLocalHost());

		// DESTINATION *
		_sshDestHost_txt.setText(ti.getDestHost());
		_sshDestPort_txt.setText(ti.getDestPort() +"");
		
		// HOSTNAME
		_sshHost_txt.setText(ti.getSshHost());

		// PORT
		_sshPort_txt.setText(ti.getSshPort() +"");

		// USERNAME
		_sshUser_txt.setText(ti.getSshUsername());

		// PASSWORD
		_sshPasswd_txt.setText(ti.getSshPassword());

		// INIT KEY FILE
		_sshKeyFile_txt.setText(ti.getSshKeyFile());

		// INIT OS CMD
		_sshInitOsCmd_txt.setText(ti.getSshInitOsCmd());
	}

	private void getSavedWindowProps()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		
		int width  = conf.getLayoutProperty(PROP_PREFIX + "window.width",  -1);
		int height = conf.getLayoutProperty(PROP_PREFIX + "window.height", -1);
		int x      = conf.getLayoutProperty(PROP_PREFIX + "window.pos.x",  -1);
		int y      = conf.getLayoutProperty(PROP_PREFIX + "window.pos.y",  -1);
		if (width != -1 && height != -1)
			this.setSize(width, height);

		if (x != -1 && y != -1)
			if ( ! SwingUtils.isOutOfScreen(x, y, width, height) )
				this.setLocation(x, y);

		// Window size can not be "smaller" than the minimum size
		// If so "OK" button etc will be hidden.
		SwingUtils.setWindowMinSize(this);
	}

	
	/*---------------------------------------------------
	** END: implementing saveProps & loadProps
	**---------------------------------------------------
	*/
	
	public static SshTunnelInfo getSshTunnelInfo(String hostPortStr)
	{
		SshTunnelInfo sshInfo = new SshTunnelInfo();

		// Guess Destination host by: first NON localhost entry in list will be destination server 
		String guessDestHostStr = "";
		String guessDestPortStr = "-1";
		List<String> hostPostList = StringUtil.parseCommaStrToList(hostPortStr);
		for (String str : hostPostList)
		{
			String host = "";
			String port = "";
			String[] sa = str.split(":");
			if (sa.length > 0) host = sa[0];
			if (sa.length > 1) port = sa[1];

			// skip localhost entries
			if (host.equals("localhost") || host.equals("127.0.0.1"))
				continue;

			guessDestHostStr = host;
			guessDestPortStr = port;
			break;
		}
		// MAYBE: set default port (not to -1) but to default port based on URL (which isn't passed here) 
		int guessDestPortInt = StringUtil.parseInt(guessDestPortStr, -1);
		

		// Get config
		Configuration conf = Configuration.getCombinedConfiguration();
		String  strVal;
		int     intVal;
		boolean bolVal;

		// LOCAL *
		bolVal = conf.getBooleanProperty(PROP_PREFIX +"local.port.generate."+hostPortStr, true);
		sshInfo.setLocalPortGenerated(bolVal);

		intVal = conf.getIntProperty(PROP_PREFIX +"local.port."+hostPortStr, -1);
		sshInfo.setLocalPort(intVal);

		strVal = conf.getProperty(PROP_PREFIX +"local.host."+hostPortStr, "localhost");
		sshInfo.setLocalHost(strVal);

		// If the option is used... either set the LocalPort back to -1 or try to check if this port is busy or not...
		// but if the local cached "PortForwarderManager" should really pick this up...
		if (sshInfo.isLocalPortGenerated())
			sshInfo.setLocalPort(-1);


		// DESTINATION *
		strVal = conf.getProperty(PROP_PREFIX +"destination.host."+hostPortStr, guessDestHostStr);
		sshInfo.setDestHost(strVal);

		intVal = conf.getIntProperty(PROP_PREFIX +"destination.port."+hostPortStr, guessDestPortInt);
		sshInfo.setDestPort(intVal);
		
		// Below, get hostPortStr first, then get basename as a fallback
		// HOSTNAME
		strVal     = conf.getProperty(PROP_PREFIX +"ssh.conn.host."+hostPortStr);
		if (strVal == null)
			strVal = conf.getProperty(PROP_PREFIX +"ssh.conn.host", "");
		sshInfo.setSshHost(strVal);

		// PORT
		intVal     = conf.getIntProperty(PROP_PREFIX +"ssh.conn.port."+hostPortStr, -1);
		if (intVal == -1)
			intVal = conf.getIntProperty(PROP_PREFIX +"ssh.conn.port", 22);
		sshInfo.setSshPort(intVal);

		// USERNAME
		strVal     = conf.getProperty(PROP_PREFIX +"ssh.conn.username."+hostPortStr);
		if (strVal == null)
			strVal = conf.getProperty(PROP_PREFIX +"ssh.conn.username", System.getProperty("user.name"));
		sshInfo.setSshUsername(strVal);

		// PASSWORD
		strVal = conf.getProperty(PROP_PREFIX +"ssh.conn.password."+hostPortStr);
		if (strVal == null)
			strVal = conf.getProperty(PROP_PREFIX +"ssh.conn.password", "");
		sshInfo.setSshPassword(strVal);

		// KEY FILE
		strVal     = conf.getProperty(PROP_PREFIX +"ssh.conn.keyFile."+hostPortStr);
		if (strVal == null)
			strVal = conf.getProperty(PROP_PREFIX +"ssh.conn.keyFile", "");
		sshInfo.setSshKeyFile(strVal);

		// INIT OS CMD
		strVal     = conf.getProperty(PROP_PREFIX +"ssh.conn.initOsCmd."+hostPortStr);
		if (strVal == null)
			strVal = conf.getProperty(PROP_PREFIX +"ssh.conn.initOsCmd", "");
		sshInfo.setSshInitOsCmd(strVal);

		return sshInfo;
	}
}
