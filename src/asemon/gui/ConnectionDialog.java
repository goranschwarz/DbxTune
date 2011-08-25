/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import asemon.Version;
import asemon.gui.swing.MultiLineLabel;
import asemon.utils.AseConnectionFactory;
import asemon.utils.AseConnectionUtils;
import asemon.utils.Configuration;
import asemon.utils.StringUtil;
import asemon.utils.SwingUtils;

public class ConnectionDialog
    extends JDialog
    implements ActionListener, KeyListener
{
	private static Logger _logger = Logger.getLogger(ConnectionDialog.class);
    private static final long serialVersionUID = -7782953767666701933L;

//	private Map                _inputMap        = null;
	private Connection         _conn            = null;

	private ImageIcon          _loginImageIcon  = SwingUtils.readImageIcon(Version.class, "./images/login_key.gif");
	private JLabel             _loginIcon       = new JLabel(_loginImageIcon);
	private MultiLineLabel     _loginHelp       = new MultiLineLabel("Identify youself to the server with user name and password");
	private JLabel             _user_lbl        = new JLabel("User name");
	private JTextField         _user_txt        = new JTextField();
	private JLabel             _passwd_lbl      = new JLabel("Password");
	private JTextField         _passwd_txt      = null; // set to JPasswordField or JTextField depending on debug level

	private ImageIcon          _serverImageIcon = SwingUtils.readImageIcon(Version.class, "./images/ase32.gif");
	private JLabel             _serverIcon      = new JLabel(_serverImageIcon);
	private MultiLineLabel     _serverHelp      = new MultiLineLabel("Select a server from the dropdown list, or enter host name and port number separeted by \":\" (For example \""+StringUtil.getHostname()+":5000\")");
	private JLabel             _serverName_lbl  = new JLabel();
	private JLabel             _server_lbl      = new JLabel("Server name");
	private LocalSrvComboBox   _server_cbx      = new LocalSrvComboBox();

	private JLabel             _host_lbl        = new JLabel("Host name");
	private JTextField         _host_txt        = new JTextField();

	private JLabel             _port_lbl        = new JLabel("Port number");
	private JTextField         _port_txt        = new JTextField();

	private JLabel             _ifile_lbl       = new JLabel("Name service");
	private JTextField         _ifile_txt       = new JTextField(AseConnectionFactory.getIFileName());
	private String             _ifile_save      = AseConnectionFactory.getIFileName();
	private JButton            _ifile_but       = new JButton("...");

	private JCheckBox          _optionSavePwd_chk      = new JCheckBox("Save password", false);
	private JCheckBox          _optionConnOnStart_chk  = new JCheckBox("Connect to this server on startup", false);
//	private JCheckBox          _optionUsedForNoGui_chk = new JCheckBox("Use connection info above for no-gui mode", false);

	private JButton            _ok             = new JButton("OK");
	private JButton            _cancel         = new JButton("Cancel");

//	private ConnectionDialog(Frame owner, String title, Map input)
	private ConnectionDialog(Frame owner, String title)
	{
		super(owner, title, true);
//		_inputMap = input;
		initComponents();
		pack();
		
		Dimension size = getPreferredSize();
		size.width += 200;

		setPreferredSize(size);
//		setMinimumSize(size);
		setSize(size);

		setLocationRelativeTo(owner);
	}


//	public static Connection showConnectionDialog(Frame owner, Map input)
	public static Connection showConnectionDialog(Frame owner)
	{
		ConnectionDialog params = new ConnectionDialog(owner, "Connect");
		params.setFocus();
		params.setVisible(true);
		params.dispose();

		return params._conn;
	}

	/*---------------------------------------------------
	** BEGIN: component initialization
	**---------------------------------------------------
	*/
	protected void initComponents() 
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("wrap 1","grow",""));   // insets Top Left Bottom Right

		
		// ADD the OK, Cancel, Apply buttons
		panel.add(createUserPasswdPanel(),  "grow");
		panel.add(createServerPanel(),      "grow");
		panel.add(createOptionsPanel(),     "grow");
		panel.add(createOkCancelPanel(),    "bottom, right, push");

		loadProps();

		setContentPane(panel);
	}
	
	private JPanel createUserPasswdPanel()
	{
		JPanel panel = SwingUtils.createPanel("User information", true);
		panel.setLayout(new MigLayout("wrap 2","",""));   // insets Top Left Bottom Right

		// Hide password or not...
		if (_logger.isDebugEnabled())
			_passwd_txt = new JTextField();
		else
			_passwd_txt = new JPasswordField();

		
		panel.add(_loginIcon,  "");
		panel.add(_loginHelp,  "wmin 100, push, grow");

		panel.add(_user_lbl,   "");
		panel.add(_user_txt,   "push, grow");

		panel.add(_passwd_lbl, "");
		panel.add(_passwd_txt, "push, grow");

		// ADD ACTION LISTENERS
		_passwd_txt.addActionListener(this);

		return panel;
	}

	private JPanel createServerPanel()
	{
		JPanel panel = SwingUtils.createPanel("Specify the server to connect to", true);
		panel.setLayout(new MigLayout("wrap 2","",""));   // insets Top Left Bottom Right

		refreshServers();

		panel.add(_serverIcon,  "");
		panel.add(_serverHelp,  "wmin 100, push, grow");

		panel.add(_server_lbl,   "");
		panel.add(_server_cbx,   "push, grow");

		panel.add(_host_lbl,     "");
		panel.add(_host_txt,     "push, grow");

		panel.add(_port_lbl,     "");
		panel.add(_port_txt,     "push, grow");

//		_ifile_txt.setEditable(false);
		panel.add(_ifile_lbl,     "");
		panel.add(_ifile_txt,     "push, grow, split");
		panel.add(_ifile_but,     "");

		_serverName_lbl.setText(":");
		if (_logger.isDebugEnabled())
		{
			panel.add(_serverName_lbl, "skip, push, grow");
		}

		// ADD ACTION LISTENERS
		_server_cbx.addActionListener(this);
		_ifile_but .addActionListener(this);
		_ifile_txt .addActionListener(this);
		
		// If write in host/port, create the combined host:port and show that...
		_host_txt.addKeyListener(this);
		_port_txt.addKeyListener(this);

		return panel;
	}

	private JPanel createOptionsPanel()
	{
		JPanel panel = SwingUtils.createPanel("Options", true);
		panel.setLayout(new MigLayout("wrap 1","",""));   // insets Top Left Bottom Right

		panel.add(_optionSavePwd_chk,      "");
		panel.add(_optionConnOnStart_chk,  "");
//		panel.add(_optionUsedForNoGui_chk, "");

		return panel;
	}

	private JPanel createOkCancelPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("","",""));   // insets Top Left Bottom Right

		// ADD the OK, Cancel, Apply buttons
		panel.add(_ok,     "tag ok, right");
		panel.add(_cancel, "tag cancel");

		// ADD ACTIONS TO COMPONENTS
		_ok           .addActionListener(this);
		_cancel       .addActionListener(this);

		return panel;
	}
	
	/*---------------------------------------------------
	** END: component initialization
	**---------------------------------------------------
	*/


	
	
	/*---------------------------------------------------
	** BEGIN: helper methods
	**---------------------------------------------------
	*/

	/**
	 * Refresh the Server combo box
	 */
	private void refreshServers()
	{
		_server_cbx.refresh();
		_host_txt.setText("");
		_port_txt.setText("");
	}

	/**
	 * New interfaces or sql.ini file, load it...
	 * @param file
	 */
	private void loadNewInterfaces(String file)
	{
		if (file == null || (file != null && file.trim().equals("")))
		{
			_logger.debug("loadNewInterfaces(): The passed interfaces file was null empty string.");
			return;
		}

		try
		{
			File f = new File(file);
			if ( ! f.exists() )
			{
				SwingUtils.showWarnMessage(this, "File dosn't exists", "The file '"+file+"' dosn't exists.", null);
				return;
			}
			AseConnectionFactory.setInterfaces(file);
			refreshServers();
			_ifile_save = file;
		}
		catch (Exception e)
		{
			SwingUtils.showErrorMessage(this, "Problems setting new Name Service file", 
				"Problems setting the Name Service file '"+file+"'." +
				"\n\n" + e.getMessage(), e);
			
			_ifile_txt.setText(_ifile_save);
		}
	}

	/**
	 * Set focus to a good field or button
	 */
	private void setFocus()
	{
		// The components needs to be visible for the requestFocus()
		// to work, so lets the EventThreda do it for us after the windows is visible.
		Runnable deferredAction = new Runnable()
		{
			public void run()
			{
				if (_user_txt  .getText().trim().equals("")) {_user_txt  .requestFocus(); return; }
				if (_passwd_txt.getText().trim().equals("")) {_passwd_txt.requestFocus(); return; }
				if (_host_txt  .getText().trim().equals("")) {_host_txt  .requestFocus(); return; }
				if (_port_txt  .getText().trim().equals("")) {_port_txt  .requestFocus(); return; }

				_ok.requestFocus();
			}
		};
		SwingUtilities.invokeLater(deferredAction);
	}

	/**
	 * Make a connection to the ASE
	 * @return
	 */
	private boolean connect()
	{
		String portStr = _port_txt.getText();
		int    port = -1;
		try {  port = Integer.parseInt( portStr ); } 
		catch (NumberFormatException e) 
		{
			SwingUtils.showErrorMessage(this, "Problem with port number", 
				"The port number '"+portStr+"' is either missing or is not a number.", e);
			return false;
		}

		_logger.debug("Setting connection info to AseConnectionFactory appname='"+Version.getAppName()
				+"', user='"+_user_txt.getText()+"', password='"+_passwd_txt.getText()
				+"', host='"+_host_txt.getText()+"', port='"+port+"'.");
		
		AseConnectionFactory.setAppName ( Version.getAppName() );
		AseConnectionFactory.setUser    ( _user_txt.getText() );
		AseConnectionFactory.setPassword( _passwd_txt.getText() );
		AseConnectionFactory.setHost    ( _host_txt.getText() );
		AseConnectionFactory.setPort    ( port );

		try
		{
			_conn = AseConnectionFactory.getConnection();
			return true;
		}
		catch (SQLException e)
		{
			String msg = AseConnectionUtils.showSqlExceptionMessage(this, "Problems Connecting", "Problems when connecting to the data server.", e); 
			_logger.error("Problems when connecting to a ASE Server. "+msg);
			return false;
		}
		catch (Exception e)
		{
			SwingUtils.showErrorMessage(this, "Problems Connecting", 
					"Problems when connecting to the data server." +
					"\n\n" + e.getMessage(), e);
			return false;
		}
	}
	
	private boolean checkForMonitorOptions()
	{
		return AseConnectionUtils.checkForMonitorOptions(_conn, _user_txt.getText(), true, this);
	}

	/*---------------------------------------------------
	** END: helper methods
	**---------------------------------------------------
	*/

	
	
	
	
	/*---------------------------------------------------
	** BEGIN: implementing ActionListener, KeyListeners
	**---------------------------------------------------
	*/
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();

		// --- CHECKBOX: SERVERS ---
		if (_server_cbx.equals(source))
		{
			_logger.debug("_server_cbx.actionPerformed(): getSelectedIndex()='"+_server_cbx.getSelectedIndex()+"', getSelectedItem()='"+_server_cbx.getSelectedItem()+"'.");
	
			// NOTE: index 0 is "host:port" or SERVER_FIRST_ENTRY("-CHOOSE A SERVER-")
			//       so we wont touch host_txt and port_txt if we are on index 0
			if ( _server_cbx.getSelectedIndex() > 0 )
			{
				String server = (String) _server_cbx.getSelectedItem();
				
				String host = AseConnectionFactory.getHost(server);
				int    port = AseConnectionFactory.getPort(server);
				_host_txt.setText(host);
				_port_txt.setText(Integer.toString(port));

				// Try to load user name & password for this server
				loadPropsForServer(host, port);
			}
		}

		// --- BUTTON: "..." Open file to get interfaces/sql.ini file ---
		if (_ifile_but.equals(source))
		{
			String dir = System.getProperty("SYBASE");
			if (dir == null)
			{
				dir = System.getenv("SYBASE");
			}
			if (dir != null)
			{
				if ( System.getProperty("os.name").startsWith("Windows"))
					dir += "\\ini";
			}

			JFileChooser fc = new JFileChooser(dir);
			int returnVal = fc.showOpenDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION) 
			{
				_ifile_txt.setText( fc.getSelectedFile().getAbsolutePath() );
				loadNewInterfaces( _ifile_txt.getText() );
			}
		}
		
		// --- TEXTFIELD: INTERFACES FILE ---
		if (_ifile_txt.equals(source))
		{
			loadNewInterfaces( _ifile_txt.getText() );
		}		

		// --- TEXTFIELD: PASSWORD ---
		if (_passwd_txt.equals(source))
		{
			saveProps();
			setVisible(false);
		}

		// --- BUTTON: CANCEL ---
		if (_cancel.equals(source))
		{
			if ( _conn != null )
			{
				try { _conn.close(); }
				catch (SQLException ignore) {}
			}
			setVisible(false);
		}

		// --- BUTTON: OK ---
		if (_ok.equals(source))
		{
			saveProps();
			if ( _conn == null )
			{
				if ( ! connect() )
				{
					return;
				}
			}
			if ( ! checkForMonitorOptions() )
				return;

			setVisible(false);
		}
	}


	
	// Typed characters in the fields are visible first when the key has been released: keyReleased()
	public void keyPressed (KeyEvent keyevent)
	{
	}

	// Discard all but digits for the _port_txt field
	public void keyTyped   (KeyEvent keyevent) 
	{
		if (keyevent.getSource().equals(_port_txt))
		{
			char ch = keyevent.getKeyChar();
			if ( ! Character.isDigit(ch) )
			{
				keyevent.consume();
				return;
			}
		}
	}

	// Update the server combo box
	public void keyReleased(KeyEvent keyevent) 
	{
		String host        = _host_txt.getText();
		String portStr     = _port_txt.getText();
		
		// Update the first entry in the combo box to be "host:port"
		// the host:port, will be what we have typed so far...
		// If the host_port can be found in the interfaces file, then
		// the combo box will display the server.
		_server_cbx.updateFirstEntry(host, portStr);

		if (_logger.isDebugEnabled())
		{
			_serverName_lbl.setText(host + ":" + portStr);
		}
	}
	/*---------------------------------------------------
	** END: implementing ActionListener, KeyListeners
	**---------------------------------------------------
	*/


	
	/*---------------------------------------------------
	** BEGIN: implementing saveProps & loadProps
	**---------------------------------------------------
	*/	
	private void saveProps()
	{
		Configuration conf = Configuration.getInstance(Configuration.TEMP);
		if (conf == null)
		{
			_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
			return;
		}

		String hostPort = _host_txt.getText() + ":" + _port_txt.getText();

		conf.setProperty("conn.interfaces", _ifile_txt.getText());
		conf.setProperty("conn.serverName", _server_cbx.getSelectedItem().toString());

		conf.setProperty("conn.hostname",   _host_txt.getText());
		conf.setProperty("conn.port",       _port_txt.getText());

		conf.setProperty("conn.username",           _user_txt.getText());
		conf.setProperty("conn.username."+hostPort, _user_txt.getText());
		if (_optionSavePwd_chk.isSelected())
		{
			conf.setEncrypedProperty("conn.password",           _passwd_txt.getText());
			conf.setEncrypedProperty("conn.password."+hostPort, _passwd_txt.getText());
		}
		else
		{
			conf.remove("conn.password");
			conf.remove("conn.password."+hostPort);
		}

		conf.setProperty("conn.savePassword", _optionSavePwd_chk.isSelected() );
		conf.setProperty("conn.onStartup",    _optionConnOnStart_chk.isSelected() );
//		conf.setProperty("conn.usedForNoGui", _optionUsedForNoGui_chk.isSelected() );
//
//		if (_optionUsedForNoGui_chk.isSelected())
//		{
//			conf.setProperty("nogui.conn.username", _user_txt.getText() );
//			conf.setProperty("nogui.conn.password", _passwd_txt.getText() );
//			conf.setProperty("nogui.conn.server",   hostPort);
//		}
//		else
//		{
//			conf.removeAll("nogui.conn.");
//		}

		conf.save();
	}

	private void loadProps()
	{
		Configuration conf = Configuration.getInstance(Configuration.TEMP);
		if (conf == null)
		{
			_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
			return;
		}

		String str = null;
		boolean bol = false;

		str = conf.getProperty("conn.interfaces");
		if (str != null)
			loadNewInterfaces(str);

		str = conf.getProperty("conn.serverName");
		if (str != null)
			_server_cbx.setSelectedItem(str);


		str = conf.getProperty("conn.hostname");
		if (str != null)
			_host_txt.setText(str);

		str = conf.getProperty("conn.port");
		if (str != null)
			_port_txt.setText(str);

		String hostPort = _host_txt.getText() + ":" + _port_txt.getText();



		// First do "conn.username.hostName.portNum", if not found, go to "conn.username"
		str = conf.getProperty("conn.username."+hostPort);
		if (str != null)
		{
			_user_txt.setText(str);
		}
		else
		{
			str = conf.getProperty("conn.username");
			if (str != null)
				_user_txt.setText(str);
		}

		// First do "conn.password.hostName.portNum", if not found, go to "conn.password"
		str = conf.getProperty("conn.password."+hostPort);
		if (str != null)
		{
			_passwd_txt.setText(str);
		}
		else
		{
			str = conf.getProperty("conn.password");
			if (str != null)
				_passwd_txt.setText(str);
		}

		bol = conf.getBooleanProperty("conn.savePassword", false);
		_optionSavePwd_chk.setSelected(bol);
		
		bol = conf.getBooleanProperty("conn.onStartup", false);
		_optionConnOnStart_chk.setSelected(bol); 

//		bol = conf.getBooleanProperty("conn.usedForNoGui", false);
//		_optionUsedForNoGui_chk.setSelected(bol);
	}

	private void loadPropsForServer(String host, int port)
	{
		Configuration conf = Configuration.getInstance(Configuration.TEMP);
		if (conf == null)
		{
			_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
			return;
		}

		String str = null;
		String hostPort = host + ":" + port;

		// First do "conn.username.hostName.portNum", if not found, go to "conn.username"
		str = conf.getProperty("conn.username."+hostPort);
		if (str != null)
		{
			_user_txt.setText(str);
		}
		else
		{
			str = conf.getProperty("conn.username");
			if (str != null)
				_user_txt.setText(str);
		}

		// First do "conn.password.hostName.portNum", if not found, go to "conn.password"
		str = conf.getProperty("conn.password."+hostPort);
		if (str != null)
		{
			_passwd_txt.setText(str);
		}
		else
		{
			str = conf.getProperty("conn.password");
			if (str != null)
				_passwd_txt.setText(str);
		}
	}

	/*---------------------------------------------------
	** END: implementing saveProps & loadProps
	**---------------------------------------------------
	*/

	
	
	
	
	/*---------------------------------------------------
	**---------------------------------------------------
	**---------------------------------------------------
	**---- SUBCLASSES ---- SUBCLASES ---- SUBCLASES ----- 
	**---------------------------------------------------
	**---------------------------------------------------
	**---------------------------------------------------
	*/
	protected class LocalSrvComboBox
	extends JComboBox
	{
		private static final long   serialVersionUID   = 7884363654457237606L;
		private static final String SERVER_FIRST_ENTRY = "-CHOOSE A SERVER-";

		private LocalSrvComboBoxModel  _model = null;

		private class LocalSrvComboBoxModel 
		extends DefaultComboBoxModel
		{
            private static final long serialVersionUID = -318689353529705207L;
			private Vector _data;

			LocalSrvComboBoxModel(Vector v)
			{
				super(v);
				_data = v;
			}
			protected void set(int index, Object obj)
			{
				_data.set(index, obj);
				fireContentsChanged(obj, index, index);
			}
		}
		
		protected LocalSrvComboBox()
		{
			super();
			_model = new LocalSrvComboBoxModel(new Vector());
			setModel(_model);
		}

		/**
		 * rebuild the server list from the interfaces file.
		 */
		public void refresh()
		{
			removeAllItems();
			addItem(SERVER_FIRST_ENTRY);
	
			String[] servers = AseConnectionFactory.getServers();
			if (servers != null)
			{
				for (int i=0; i<servers.length; i++)
					addItem(servers[i]);

				setSelectedItem(SERVER_FIRST_ENTRY);
			}
		}

		/** 
		 * Update the first entry in the combo box to be "host:port"
		 * the host:port, will be what we have typed so far...
		 * <p>
		 * If the host_port can be found in the interfaces file, then
		 * the combo box will display the server.
		 */
		public void updateFirstEntry(String host, String portStr) 
		{
			String hostAndPort = host + ":" + portStr;

			if (hostAndPort.equals(":"))
			{
				_model.set(0, SERVER_FIRST_ENTRY);
				setForeground(Color.BLACK);
				setSelectedIndex(0);
				return;
			}

			int    port = -1;
			try {  port = Integer.parseInt( portStr ); } catch (NumberFormatException ignore) {}

			String server = null;
			if (port > 0)
				server = AseConnectionFactory.getServerName(host, port);

			if (server == null || (server != null && server.trim().equals("")) )
			{
				if (_logger.isTraceEnabled())
					_logger.trace("host='"+host+"', port='"+port+"' was NOT FOUND.");
				_model.set(0, hostAndPort);
				setSelectedIndex(0);
				setForeground(Color.BLUE);
			}
			else
			{
				if (_logger.isTraceEnabled())
					_logger.trace("Found='"+server+"' for host='"+host+"', port='"+port+"'.");

				if ( ! server.equals(getSelectedItem()) )
				{
					_model.set(0, SERVER_FIRST_ENTRY);
					setForeground(Color.BLACK);
					setSelectedItem(server);
				}
			}
		}
		
	}
	

	
	
	
	//--------------------------------------------------
	// TEST-CODE
	//--------------------------------------------------
	public static void main(String[] args)
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		Properties log4jProps = new Properties();
		//log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		Configuration conf1 = new Configuration("c:\\projects\\asemon\\asemon.save.properties");
		Configuration.setInstance(Configuration.TEMP, conf1);

		Configuration conf2 = new Configuration("c:\\projects\\asemon\\asemon.properties");
		Configuration.setInstance(Configuration.CONF, conf2);

		
		// DO THE THING
		Connection conn = showConnectionDialog(null);
		
		if (conn == null)
		{
			System.out.println("no connection...");
		}
		else
		{
			System.out.println("GOT connection...");
		}
	}
}

