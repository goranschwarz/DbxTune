/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

import com.asetune.AseTune;
import com.asetune.GetCounters;
import com.asetune.MonTablesDictionary;
import com.asetune.Version;
import com.asetune.cm.CountersModel;
import com.asetune.gui.swing.MultiLineLabel;
import com.asetune.hostmon.SshConnection;
import com.asetune.pcs.PersistReader;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.AseUrlHelper;
import com.asetune.utils.Configuration;
import com.asetune.utils.H2UrlHelper;
import com.asetune.utils.PlatformUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;


public class ConnectionDialog
	extends JDialog
	implements ActionListener, KeyListener, TableModelListener, ChangeListener
{
	private static Logger _logger = Logger.getLogger(ConnectionDialog.class);
	private static final long serialVersionUID = -7782953767666701933L;

	public static final String CONF_OPTION_RECONNECT_ON_FAILURE = "conn.reconnectOnFailure";
	public static final String CONF_OPTION_CONNECT_ON_STARTUP   = "conn.onStartup";

	
	public  static final int   CANCEL           = 0;
	public  static final int   ASE_CONN         = 1;
	public  static final int   OFFLINE_CONN     = 2;
	private int                _connectionType  = CANCEL;

//	private Map                      _inputMap        = null;
	private Connection               _aseConn         = null;
	private SshConnection            _sshConn         = null;
//	private Connection               _pcsConn         = null;
	private Connection               _offlineConn     = null;
//	private PersistentCounterHandler _pcsWriter       = null;


	private static final int   TAB_POS_ASE      = 0;
	private static final int   TAB_POS_HOSTMON  = 1;
	private static final int   TAB_POS_PCS      = 2;
	private static final int   TAB_POS_OFFLINE  = 3;

	//-------------------------------------------------
	// Actions
	public static final String ACTION_OK        = "OK";
	public static final String ACTION_CANCEL    = "CANCEL";

	@SuppressWarnings("unused")
	private Frame              _owner           = null;
	
	private JTabbedPane        _tab;

	//---- ASE panel
	private ImageIcon          _aseLoginImageIcon  = SwingUtils.readImageIcon(Version.class, "images/login_key.gif");
	private JLabel             _aseLoginIcon       = new JLabel(_aseLoginImageIcon);
	private MultiLineLabel     _aseLoginHelp       = new MultiLineLabel("Identify yourself to the server with user name and password");
	private JLabel             _aseUser_lbl        = new JLabel("User name");
	private JTextField         _aseUser_txt        = new JTextField();
	private JLabel             _asePasswd_lbl      = new JLabel("Password");
	private JTextField         _asePasswd_txt      = null; // set to JPasswordField or JTextField depending on debug level

	private ImageIcon          _aseServerImageIcon = SwingUtils.readImageIcon(Version.class, "images/ase32.gif");
	private JLabel             _aseServerIcon      = new JLabel(_aseServerImageIcon);
	private MultiLineLabel     _aseServerHelp      = new MultiLineLabel("Select a server from the dropdown list, or enter host name and port number separeted by \":\" (For example \""+StringUtil.getHostname()+":5000\")");
	private JLabel             _aseServerName_lbl  = new JLabel();
	private JLabel             _aseServer_lbl      = new JLabel("Server name");
	private LocalSrvComboBox   _aseServer_cbx      = new LocalSrvComboBox();

	private JLabel             _aseHost_lbl        = new JLabel("Host name");
	private JTextField         _aseHost_txt        = new JTextField();

	private JLabel             _asePort_lbl        = new JLabel("Port number");
	private JTextField         _asePort_txt        = new JTextField();

	private JLabel             _aseOptions_lbl     = new JLabel("URL Options");
	private JTextField         _aseOptions_txt     = new JTextField();
	private JButton            _aseOptions_but     = new JButton("...");

	private JLabel             _aseIfile_lbl       = new JLabel("Name service");
	private JTextField         _aseIfile_txt       = new JTextField(AseConnectionFactory.getIFileName());
	private String             _aseIfile_save      = AseConnectionFactory.getIFileName();
	private JButton            _aseIfile_but       = new JButton("...");
	private JButton            _aseEditIfile_but   = new JButton("Edit");

	private JCheckBox          _aseConnUrl_chk     = new JCheckBox("Use URL", false);
	private JTextField         _aseConnUrl_txt     = new JTextField();


	private JCheckBox          _aseOptionSavePwd_chk         = new JCheckBox("Save password", true);
	private JCheckBox          _aseOptionConnOnStart_chk     = new JCheckBox("Connect to this server on startup", false);
	private JCheckBox          _aseOptionReConnOnFailure_chk = new JCheckBox("Reconnect to server if connection is lost", true);
//	private JCheckBox          _aseOptionUsedForNoGui_chk    = new JCheckBox("Use connection info above for no-gui mode", false);
	private JCheckBox          _aseHostMonitor_chk           = new JCheckBox("Monitor the OS Host for IO and CPU...", false);
	private JCheckBox          _aseOptionStore_chk           = new JCheckBox("Save counter data in a Persistent Counter Storage...", false);

	//---- OS HOST panel
	private ImageIcon          _hostmonLoginImageIcon  = SwingUtils.readImageIcon(Version.class, "images/login_key.gif");
	private JLabel             _hostmonLoginIcon       = new JLabel(_hostmonLoginImageIcon);
	private MultiLineLabel     _hostmonLoginHelp       = new MultiLineLabel("Identify yourself to the host Operating System with user name and password. A SSH (Secure Shell) connection will be used, so password and traffic will be encrypted over the network.");
	private JLabel             _hostmonUser_lbl        = new JLabel("User name");
	private JTextField         _hostmonUser_txt        = new JTextField();
	private JLabel             _hostmonPasswd_lbl      = new JLabel("Password");
	private JTextField         _hostmonPasswd_txt      = null; // set to JPasswordField or JTextField depending on debug level

	private ImageIcon          _hostmonServerImageIcon = SwingUtils.readImageIcon(Version.class, "images/server_32.png");
	private JLabel             _hostmonServerIcon      = new JLabel(_hostmonServerImageIcon);
	private MultiLineLabel     _hostmonServerHelp      = new MultiLineLabel("Specify host name to the machine where you want to do Operating System Monitoring. The connection will be using SSH (Secure Shell), which normally is listening on port 22. ");
	private JLabel             _hostmonServerName_lbl  = new JLabel();

	private JLabel             _hostmonHost_lbl        = new JLabel("Host Name");
	private JTextField         _hostmonHost_txt        = new JTextField();
	private JLabel             _hostmonPort_lbl        = new JLabel("Port Number");
	private JTextField         _hostmonPort_txt        = new JTextField("22");

	private JCheckBox          _hostmonOptionSavePwd_chk = new JCheckBox("Save password", true);
	
	//---- PCS panel
	@SuppressWarnings("unused")
	private JPanel             _pcsPanel            = null;
	private ImageIcon          _pcsImageIcon        = SwingUtils.readImageIcon(Version.class, "images/pcs_write_32.png");
	private JLabel             _pcsIcon             = new JLabel(_pcsImageIcon);
	private MultiLineLabel     _pcsHelp             = new MultiLineLabel();
	private JLabel             _pcsWriter_lbl       = new JLabel("PCS Writer");
	private JComboBox          _pcsWriter_cbx       = new JComboBox();
	private JLabel             _pcsJdbcDriver_lbl   = new JLabel("JDBC Driver");
	private JComboBox          _pcsJdbcDriver_cbx   = new JComboBox();
	private JLabel             _pcsJdbcUrl_lbl      = new JLabel("JDBC Url"); 
	private JComboBox          _pcsJdbcUrl_cbx      = new JComboBox();
	private JButton            _pcsJdbcUrl_but      = new JButton("...");
	private JLabel             _pcsJdbcUsername_lbl = new JLabel("Username");
	private JTextField         _pcsJdbcUsername_txt = new JTextField("sa");
	private JLabel             _pcsJdbcPassword_lbl = new JLabel("Password");
	private JTextField         _pcsJdbcPassword_txt = new JPasswordField();
	private JLabel             _pcsTestConn_lbl     = new JLabel();
	private JButton            _pcsTestConn_but     = new JButton("Test Connection");
	private JXTable            _pcsSessionTable     = null;
	private JButton            _pcsTabSelectAll_but = new JButton("Select All");
	private JButton            _pcsTabDeSelectAll_but = new JButton("Deselect All");
	private JButton            _pcsTabTemplate_but  = new JButton("Set to Template");
	private boolean            _useCmForPcsTable    = true;
	// Specific options if we are using H2 as PCS
	private JCheckBox          _pcsH2Option_startH2NetworkServer_chk = new JCheckBox("Start H2 Database as a Network Server", false);

	//---- OFFLINE panel
	@SuppressWarnings("unused")
	private JPanel             _offlinePanel            = null;
	private ImageIcon          _offlineImageIcon        = SwingUtils.readImageIcon(Version.class, "images/pcs_read_32.png");
	private JLabel             _offlineIcon             = new JLabel(_offlineImageIcon);
	private MultiLineLabel     _offlineHelp             = new MultiLineLabel();
	private JLabel             _offlineJdbcDriver_lbl   = new JLabel("JDBC Driver");
	private JComboBox          _offlineJdbcDriver_cbx   = new JComboBox();
	private JLabel             _offlineJdbcUrl_lbl      = new JLabel("JDBC Url"); 
	private JComboBox          _offlineJdbcUrl_cbx      = new JComboBox();
	private JButton            _offlineJdbcUrl_but      = new JButton("...");
	private JLabel             _offlineJdbcUsername_lbl = new JLabel("Username");
	private JTextField         _offlineJdbcUsername_txt = new JTextField("sa");
	private JLabel             _offlineJdbcPassword_lbl = new JLabel("Password");
	private JTextField         _offlineJdbcPassword_txt = new JPasswordField();
//	private JLabel             _offlineTestConn_lbl     = new JLabel();
	private JButton            _offlineTestConn_but     = new JButton("Test Connection");
//	private JXTable            _offlineSessionTable     = new JXTable();
//	private JButton            _offlineTabSelectAll_but = new JButton("Select All");
//	private JButton            _offlineTabDeSelectAll_but = new JButton("Deselect All");
	private JCheckBox          _offlineCheckForNewSessions_chk = new JCheckBox("Check PCS database for new sample sessions", true);
	// Specific options if we are using H2 as PCS
	private JCheckBox          _offlineH2Option_startH2NwSrv_chk = new JCheckBox("Start H2 Database as a Network Server", false);

	//---- Buttons at the bottom
	private JLabel             _ok_lbl         = new JLabel(""); // Problem description if _ok is disabled
	private JButton            _ok             = new JButton("OK");
	private JButton            _cancel         = new JButton("Cancel");

	private boolean            _checkAseCfg    = true;
	private boolean            _showAseTab     = true;
	private boolean            _showHostmonTab = true;
	private boolean            _showPcsTab     = true;
	private boolean            _showOfflineTab = true;

	private boolean            _showAseOptions = true;

//	private static ConnectionDialog   _instance       = null;
//	public static ConnectionDialog getInstance()
//	{
//		return _instance;
//	}

	public static Connection showAseOnlyConnectionDialog(Frame owner)
	{
		ConnectionDialog connDialog = new ConnectionDialog(null, false, true, false, false, false, false);
		connDialog.setVisible(true);
		connDialog.dispose();

		int connType = connDialog.getConnectionType();
		
		if ( connType == ConnectionDialog.CANCEL)
			return null;

		if ( connType == ConnectionDialog.ASE_CONN)
			return connDialog.getAseConn();

		return null;
	}

	public ConnectionDialog(Frame owner)
	{
		this(owner, true, true, true, true, true, true);
	}
	public ConnectionDialog(Frame owner, boolean checkAseCfg, boolean showAseTab, boolean showAseOptions, boolean showHostmonTab, boolean showPcsTab, boolean showOfflineTab)
	{
		super(owner, "Connect", true);
//		_instance = this;
		_owner = owner;

		if (owner == null)
			_useCmForPcsTable = false;

		_checkAseCfg    = checkAseCfg;
		_showAseTab     = showAseTab;
		_showAseOptions = showAseOptions;
		_showHostmonTab = showHostmonTab;
		_showPcsTab     = showPcsTab;
		_showOfflineTab = showOfflineTab;

//		_inputMap = input;
		initComponents();
		pack();
		
		Dimension size = getPreferredSize();
//		size.width += 100;
		size.width = 480;

//		setPreferredSize(size);
//		setMinimumSize(size);
		setSize(size);

		toggleCounterStorageTab();
		toggleHostmonTab();

		setLocationRelativeTo(owner);

		getSavedWindowProps();

		setFocus();
	}

	public int                      getConnectionType() { return _connectionType; }
	public Connection               getAseConn()        { return _aseConn; }
	public SshConnection            getSshConn()        { return _sshConn; }
//	public Connection               getPcsConn()        { return _pcsConn; }
//	public PersistentCounterHandler getPcsWriter()      { return _pcsWriter; }
	public Connection               getOfflineConn()    { return _offlineConn; }

	public void setAseUsername(String username) { _aseUser_txt.setText(username); }
	public void setAsePassword(String passwd)   { _asePasswd_txt.setText(passwd); }
	public void setAseServer  (String server)
	throws Exception
	{
		if (server == null)
			return;

		if (server.indexOf(":") >= 0)
		{
			String[] sa = server.split(":");
			String host = sa[0];
			String port = sa[1];

			if ( ! AseConnectionFactory.isHostPortStrValid(server) )
				throw new Exception("Problems the server name '"+server+"'. "+AseConnectionFactory.isHostPortStrValidReason(server));

			_aseHost_txt.setText(host);
			_asePort_txt.setText(port);
		}
		else
		{
			String str = AseConnectionFactory.resolvInterfaceEntry(server);
			if (str == null)
				throw new Exception("ASE Server name '"+server+"' was not part of the known ASE server's.");

			_aseServer_cbx.setSelectedItem(server);
		}
	}
	
	public String getAseUsername() { return _aseUser_txt.getText(); }
	public String getAsePassword() { return _asePasswd_txt.getText(); }
	public String getAseServer  () { return (String) _aseServer_cbx.getSelectedItem(); }


	public void setSshUsername(String username) { _hostmonUser_txt  .setText(username); }
	public void setSshPassword(String password) { _hostmonPasswd_txt.setText(password); }
	public void setSshHostname(String hostname) { _hostmonHost_txt  .setText(hostname); }
	public void setSshPort(String portStr)      { _hostmonPort_txt  .setText(portStr); }

	public String getSshUsername() { return _hostmonUser_txt  .getText(); }
	public String getSshPassword() { return _hostmonPasswd_txt.getText(); }
	public String getSshHostname() { return _hostmonHost_txt  .getText(); }
	public String getSshPortStr()  { return _hostmonPort_txt  .getText(); }

	public String getOfflineJdbcDriver() { return _offlineJdbcDriver_cbx  .getEditor().getItem().toString(); }
	public String getOfflineJdbcUrl()    { return _offlineJdbcUrl_cbx     .getEditor().getItem().toString(); }
	public String getOfflineJdbcUser()   { return _offlineJdbcUsername_txt.getText(); }
	public String getOfflineJdbcPasswd() { return _offlineJdbcPassword_txt.getText(); }

//	public static Connection showConnectionDialog(Frame owner, Map input)
//	public static Connection showConnectionDialog(Frame owner)
//	{
//		ConnectionDialog params = new ConnectionDialog(owner);
//		params.setFocus();
//		params.setVisible(true);
//		params.dispose();
//
//		return params._aseConn;
//	}

	/*---------------------------------------------------
	** BEGIN: component initialization
	**---------------------------------------------------
	*/
	protected void initComponents() 
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("insets 0 0 0 0, wrap 1", "", ""));   // insets Top Left Bottom Right

		String aseTabTip     = "Connect to an ASE to monitor performance.";
		String hostmonTabTip = "Connect to Operating System host machine where ASE is hosted to monitor IO and or CPU performance.";
		String pcsTabTip     = "In GUI mode save Data Counter to a Storage, which can be view later by the 'offline' mode.";
		String offlineTabTip = "<html>" +
		                       "Connect to a 'offline' Counter Storage where "+Version.getAppName()+" has stored counter data.<br>" +
		                       Version.getAppName()+" will switch to 'offline' mode and just reads data from the Counter Storage.<br>" +
		                       "</html>";

		_tab = new JTabbedPane();
		_tab.addTab("ASE",             null, createTabAse(),     aseTabTip);
		_tab.addTab("Host Monitor",    null, createTabHostmon(), hostmonTabTip);
		_tab.addTab("Counter Storage", null, createTabPcs(),     pcsTabTip);
		_tab.addTab("Offline Connect", null, createTabOffline(), offlineTabTip);
		
		if (! _showAseTab)     _tab.setEnabledAt(TAB_POS_ASE,     false);
		if (! _showHostmonTab) _tab.setEnabledAt(TAB_POS_HOSTMON, false);
		if (! _showPcsTab)     _tab.setEnabledAt(TAB_POS_PCS,     false);
		if (! _showOfflineTab) _tab.setEnabledAt(TAB_POS_OFFLINE, false);

		// Set active tab to first tab that is enabled
		for (int t=0; t<_tab.getTabCount(); t++)
		{
			if (_tab.isEnabledAt(t))
			{
				_tab.setSelectedIndex(t);
				break;
			}
		}


		panel.add(_tab,                  "height 100%, grow, push, wrap");
		panel.add(createOkCancelPanel(), "bottom, right");

//		panel.add(createUserPasswdPanel(),  "grow");
//		panel.add(createServerPanel(),      "grow");
//		panel.add(createOptionsPanel(),     "grow");
//		panel.add(createPcsPanel(),         "grow, hidemode 3");
//		panel.add(createOkCancelPanel(),    "bottom, right, push");

		_tab.addChangeListener(this);

		loadProps();

		setContentPane(panel);
	}

	private JPanel createTabAse()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("wrap 1","grow",""));   // insets Top Left Bottom Right
		
		panel.add(createAseUserPasswdPanel(),  "grow");
		panel.add(createAseServerPanel(),      "grow");
		if (_showAseOptions)
			panel.add(createAseOptionsPanel(), "grow");
//		panel.add(createPcsPanel(),         "grow, hidemode 3");
		
		return panel;
	}
	private JPanel createTabHostmon()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("wrap 1","grow",""));   // insets Top Left Bottom Right
		
		panel.add(createHostmonUserPasswdPanel(), "grow");
		panel.add(createHostmonServerPanel(),     "grow");
		panel.add(createHostmonInfoPanel(),       "grow");
		
		return panel;
	}
	private JPanel createTabPcs()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("wrap 1","grow",""));   // insets Top Left Bottom Right
		
		panel.add(createPcsJdbcPanel(),         "grow, hidemode 3");
		panel.add(createPcsTablePanel(),        "grow");
		
		return panel;
	}
	private JPanel createTabOffline()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("wrap 1","grow",""));   // insets Top Left Bottom Right

		panel.add(createOfflineJdbcPanel(), "grow");

		return panel;
	}
	
	private JPanel createAseUserPasswdPanel()
	{
		JPanel panel = SwingUtils.createPanel("User information", true);
		panel.setLayout(new MigLayout("wrap 2","",""));   // insets Top Left Bottom Right

		// Hide password or not...
		if (_logger.isDebugEnabled())
			_asePasswd_txt = new JTextField();
		else
			_asePasswd_txt = new JPasswordField();

		_aseUser_lbl  .setToolTipText("User name to use when logging in to the below ASE Server.");
		_aseUser_txt  .setToolTipText("User name to use when logging in to the below ASE Server.");
		_asePasswd_lbl.setToolTipText("Password to use when logging in to the below ASE Server");
		_asePasswd_txt.setToolTipText("Password to use when logging in to the below ASE Server");
		_aseOptionSavePwd_chk.setToolTipText("Save the password in the configuration file, and yes it's encrypted");

		
		panel.add(_aseLoginIcon,  "");
		panel.add(_aseLoginHelp,  "wmin 100, push, grow");

		panel.add(_aseUser_lbl,   "");
		panel.add(_aseUser_txt,   "push, grow");

		panel.add(_asePasswd_lbl, "");
		panel.add(_asePasswd_txt, "push, grow");

		panel.add(_aseOptionSavePwd_chk, "skip");

		// ADD ACTION LISTENERS
		_asePasswd_txt.addActionListener(this);

		return panel;
	}

	private JPanel createAseServerPanel()
	{
		JPanel panel = SwingUtils.createPanel("Specify the server to connect to", true);
		panel.setLayout(new MigLayout("wrap 2","",""));   // insets Top Left Bottom Right

		refreshServers();

		String urlExample="URL Example: jdbc:sybase:Tds:host1:port1[,server2:port2,...,serverN:portN][/mydb][?&OPT1=1024&OPT2=true&OPT3=some str]";

		_aseIfile_lbl  .setToolTipText("Directory Service file (sql.ini or interfaces) to use for resolving ASE name into hostname and port number");
		_aseIfile_txt  .setToolTipText("Directory Service file (sql.ini or interfaces) to use for resolving ASE name into hostname and port number");
		_aseIfile_but  .setToolTipText("Open a File Dialog to locate a Directory Service file.");
		_aseEditIfile_but.setToolTipText("Edit the Name/Directory Service file. Just opens a text editor.");
		_aseServer_lbl .setToolTipText("Name of the ASE you are connecting to");
		_aseServer_cbx .setToolTipText("Name of the ASE you are connecting to");
		_aseHost_lbl   .setToolTipText("<html>Hostname or IP address of the ASE you are connecting to<br>Syntax: host1[,host2,...]</html>");
		_aseHost_txt   .setToolTipText("<html>Hostname or IP address of the ASE you are connecting to<br>Syntax: host1[,host2,...]</html>");
		_asePort_lbl   .setToolTipText("<html>Port number of the ASE you are connecting to<br>Syntax: port1[,port2,...]</html>");
		_asePort_txt   .setToolTipText("<html>Port number of the ASE you are connecting to<br>Syntax: port1[,port2,...]</html>");
		_aseOptions_lbl.setToolTipText("<html>JConnect Options that can be set.<br>Syntax: OPT1=value[, OPT2=value]<br>Or press the '...' button to add enties.</html>");
		_aseOptions_txt.setToolTipText("<html>JConnect Options that can be set.<br>Syntax: OPT1=value[, OPT2=value]<br>Or press the '...' button to add enties.</html>");
		_aseOptions_but.setToolTipText("Open a Dialog where available options are presented.");
		_aseConnUrl_chk.setToolTipText("<html>Actual URL used to connect the monitored server.<br>"+urlExample+"</html>");
		_aseConnUrl_txt.setToolTipText("<html>Actual URL used to connect the monitored server.<br>"+urlExample+"</html>");

		panel.add(_aseServerIcon,  "");
		panel.add(_aseServerHelp,  "wmin 100, push, grow");

//		_ifile_txt.setEditable(false);
		panel.add(_aseIfile_lbl,     "");
		panel.add(_aseIfile_txt,     "push, grow, split");
//		panel.add(_aseIfile_but,     "wrap");
		panel.add(_aseIfile_but,     "");
		panel.add(_aseEditIfile_but, "wrap");

		panel.add(_aseServer_lbl,   "");
		panel.add(_aseServer_cbx,   "push, grow");

		panel.add(_aseHost_lbl,     "");
		panel.add(_aseHost_txt,     "push, grow");

		panel.add(_asePort_lbl,     "");
		panel.add(_asePort_txt,     "push, grow");

		panel.add(_aseOptions_lbl,  "");
		panel.add(_aseOptions_txt,  "push, grow, split");
		panel.add(_aseOptions_but,  "wrap");

		panel.add(_aseConnUrl_chk,     "");
		panel.add(_aseConnUrl_txt,     "push, grow");
		_aseConnUrl_txt.setEnabled(_aseConnUrl_chk.isEnabled());

		_aseServerName_lbl.setText(":");
		if (_logger.isDebugEnabled())
		{
			panel.add(_aseServerName_lbl, "skip, push, grow");
		}

		// set auto completion
		AutoCompleteDecorator.decorate(_aseServer_cbx);

		// ADD ACTION LISTENERS
		_aseServer_cbx   .addActionListener(this);
		_aseOptions_txt  .addActionListener(this);
		_aseOptions_but  .addActionListener(this);
		_aseIfile_but    .addActionListener(this);
		_aseEditIfile_but.addActionListener(this);
		_aseIfile_txt    .addActionListener(this);
		_aseConnUrl_chk  .addActionListener(this);
		_aseConnUrl_txt  .addActionListener(this);

		// If write in host/port, create the combined host:port and show that...
		_aseHost_txt.addKeyListener(this);
		_asePort_txt.addKeyListener(this);

		return panel;
	}

	private JPanel createAseOptionsPanel()
	{
		JPanel panel = SwingUtils.createPanel("Options", true);
		panel.setLayout(new MigLayout("wrap 1, gap 0","",""));   // insets Top Left Bottom Right

//		_aseOptionSavePwd_chk        .setToolTipText("Save the password in the configuration file, and yes it's encrypted");
		_aseOptionConnOnStart_chk    .setToolTipText("When "+Version.getAppName()+" starts use the above ASE and connect automatically (if the below 'Persisten Counter Storage' is enabled, it will also be used at startup)");
		_aseOptionReConnOnFailure_chk.setToolTipText("If connection to the monitored server is lost in some way, try to reconnect to the server again automatically.");
		_aseOptionStore_chk          .setToolTipText("Store GUI Counter Data in a database (Persistent Counter Storage), which can be viewed later, connect to it from the 'offline' tab");
		_aseHostMonitor_chk          .setToolTipText("Connect to the Operating System host via SSH, to monitor IO statistics and/or CPU usage.");

//		panel.add(_aseOptionSavePwd_chk,         "");
		panel.add(_aseOptionConnOnStart_chk,     "");
		panel.add(_aseOptionReConnOnFailure_chk, "");
//		panel.add(_aseOptionUsedForNoGui_chk,    "");
		panel.add(_aseHostMonitor_chk,         "");

		if (_showPcsTab)
			panel.add(_aseOptionStore_chk,       "");

		if (_showHostmonTab)
			panel.add(_aseHostMonitor_chk,     "");

		_aseOptionConnOnStart_chk.addActionListener(this);
		_aseOptionStore_chk      .addActionListener(this);
		_aseHostMonitor_chk      .addActionListener(this);

		return panel;
	}

	private JPanel createHostmonUserPasswdPanel()
	{
		JPanel panel = SwingUtils.createPanel("User information", true);
		panel.setLayout(new MigLayout("wrap 2","",""));   // insets Top Left Bottom Right

		// Hide password or not...
		if (_logger.isDebugEnabled())
			_hostmonPasswd_txt = new JTextField();
		else
			_hostmonPasswd_txt = new JPasswordField();

		_hostmonUser_lbl  .setToolTipText("User name to use when logging in to the below Operating System Host.");
		_hostmonUser_txt  .setToolTipText("User name to use when logging in to the below Operating System Host.");
		_hostmonPasswd_lbl.setToolTipText("Password to use when logging in to the below Operating System Host");
		_hostmonPasswd_txt.setToolTipText("Password to use when logging in to the below Operating System Host");
		_hostmonOptionSavePwd_chk.setToolTipText("Save the password in the configuration file, and yes it's encrypted");
		
		panel.add(_hostmonLoginIcon,  "");
		panel.add(_hostmonLoginHelp,  "wmin 100, push, grow");

		panel.add(_hostmonUser_lbl,   "");
		panel.add(_hostmonUser_txt,   "push, grow");

		panel.add(_hostmonPasswd_lbl, "");
		panel.add(_hostmonPasswd_txt, "push, grow");

		panel.add(_hostmonOptionSavePwd_chk,      "skip");

		// ADD ACTION LISTENERS
//		_hostmonUser_txt  .addActionListener(this);
//		_hostmonPasswd_txt.addActionListener(this);
		_hostmonUser_txt  .addKeyListener(this);
		_hostmonPasswd_txt.addKeyListener(this);
		_hostmonOptionSavePwd_chk.addActionListener(this);

		return panel;
	}

	private JPanel createHostmonServerPanel()
	{
		JPanel panel = SwingUtils.createPanel("Specify the server to connect to", true);
		panel.setLayout(new MigLayout("wrap 2","",""));   // insets Top Left Bottom Right

		_hostmonHost_lbl   .setToolTipText("<html>Hostname or IP address of the OS Host you are connecting to</html>");
		_hostmonHost_txt   .setToolTipText("<html>Hostname or IP address of the OS Host you are connecting to</html>");
		_hostmonPort_lbl   .setToolTipText("<html>Port number of the SSH server you are connecting to</html>");
		_hostmonPort_txt   .setToolTipText("<html>Port number of the SSH server you are connecting to</html>");

		panel.add(_hostmonServerIcon,  "");
		panel.add(_hostmonServerHelp,  "wmin 100, push, grow");

		panel.add(_hostmonHost_lbl,     "");
		panel.add(_hostmonHost_txt,     "push, grow");

		panel.add(_hostmonPort_lbl,     "");
		panel.add(_hostmonPort_txt,     "push, grow");

		_hostmonServerName_lbl.setText(":");
		if (_logger.isDebugEnabled())
		{
			panel.add(_hostmonServerName_lbl, "skip, push, grow");
		}

		// ADD ACTION LISTENERS

		_hostmonHost_txt.addKeyListener(this);
		_hostmonPort_txt.addKeyListener(this);
//		_hostmonHost_txt.addActionListener(this);
//		_hostmonPort_txt.addActionListener(this);

		return panel;
	}

	private JPanel createHostmonInfoPanel()
	{
		JPanel panel = SwingUtils.createPanel("Basic Host Monitoring Information", true);
		panel.setLayout(new MigLayout("wrap 1, gap 0","",""));   // insets Top Left Bottom Right

		MultiLineLabel txt;
		String s, t;

		s = "Hover over the different subsection and the ToolTip manager will display more information about the various topics.";
		t = s;
		txt = new MultiLineLabel("<html>"+s+"</html>");
		txt.setToolTipText("<html>"+t+"</html>");
		panel.add(txt, "wmin 100, push, grow, wrap 10");

		s = "<b>- What is this Basic Operating System Monitoring.</b>";
		t = "If you want to monitor the Operating System with some basic command, this is one way you can do it.<br>" +
		    "And also if you are storing the Performance Counters in a Persistent storage, the OS Monitoring is in sync with <br>" +
		    "the Performance Counters from the monitored ASE Server, which makes it easier to correlate the various metrics.";
		txt = new MultiLineLabel("<html>"+s+"</html>");
		txt.setToolTipText("<html>"+t+"</html>");
		panel.add(txt, "wmin 100, push, grow, wrap 10");

		s = "<b>- What OS Monitoring Commands are supported for the moment.</b>";
		t = "The basic command which for the moment is supported is:" +
				"<ul>" +
				"  <li> <code> iostat </code></li>" +
				"  <li> <code> vmstat </code></li>" +
				"  <li> <code> mpstat </code></li>" +
				"</ul>" +
		    "If you are using <b>Veritas</b> as the Disk IO subsystem and want to sample disk statistics with <b>vxstat</b> and not iostat.<br>" +
		    "Make sure <b>vxstat</b> is executable and in the <b>$PATH</b> off the user that you're connecting to the Operating System with.<br>" +
		    "Then it will execute <b>vxstat</b> instead of iostat and use that information.";
		txt = new MultiLineLabel("<html>"+s+"</html>");
		txt.setToolTipText("<html>"+t+"</html>");
		panel.add(txt, "wmin 100, push, grow, wrap 10");

		s = "<b>- But every Operating System work slightly different.</b>";
		t = "But all Operating Systems has different layouts for the various commands. <br>" +
		    "For Example 'iostat' does not produce the same output on Solaris as it does on Linux. <br>" +
		    "<br>" +
		    "When a connection to the OS made, It will try to identify what OS were connecting to (by using uname -a) and then use OS specific parsers. <br>" +
		    "This implies that a \"parser\" is implement for every OS.<br>" +
		    "So if you are getting errors, faulty output or that your OS is not supported, please notify me and I will try to fix it.<br>" +
		    "Also if you want to change the parameters/flags to the OS Commands, to get better Performance Counters, kick me and I'll fix it!<br>" +
		    "<br>" +
		    "The only thing that can change by ourself is the OS Commands sleep interval<br>" +
		    "This is done by adding the below information to the configuration file. (asetune.properties)" +
			"<ul>" +
			"  <li> for iostat: <code>MonitorIoSolaris.sleep=5</code></li>" +
			"  <li> for mpstat: <code>MonitorVmstatAix.sleep=5</code></li>" +
			"  <li> for vmstat: <code>MonitorMpstatLinux.sleep=5</code></li>" +
			"</ul>" +
		    "You can probably fill in the blanks... <code>[Monitor][Subsystem][Osname].sleep=[samplePeriodToTheCommand]</code><br>" +
		    "<br>" +
		    "If you need to debug or check what the OS Command is sending, you can add the following to the Configuration file:<br>" +
		    "<code>log4j.logger.com.asetune.hostmon.HostMonitor=DEBUG</code><br>" +
		    "<br>" +
		    "Or you can use the 'log viewer' to change the 'log level' while "+Version.getAppName()+" is still running: <br>" +
		    "MainMenu->View->Open Log Window..., then Press 'Set log level'... locate com.asetune.hostmon.HostMonitor and change it.";
		txt = new MultiLineLabel("<html>"+s+"</html>");
		txt.setToolTipText("<html>"+t+"</html>");
		panel.add(txt, "wmin 100, push, grow, wrap 10");

		s = "<b>- The big picture of how it's implememted.</b>";
		t = "The way it's implemented:<br>" +
		    "<ul>" +
		    "  <li> Start a background task that executes for instance 'iostat' every 2 seconds (iostat -xdz 2) or whatever sample period (see section above).</li>" +
		    "  <li> Parse the output from the OS Command and store it in a intermediate in-memory storage.</li>" +
		    "  <li> Then when "+Version.getAppName()+" think it's time to sample some Performance Counters (depends on "+Version.getAppName()+" sample time)<br>" +
		    "       The subsystem does an <b>average</b> calculation on the intermediate stored values and then resets the intermidiate storage so that new samples from the OS Command can be sampled. <br>" +
		    "  <li> Now the Performance Counters or values are available to view in "+Version.getAppName()+".</li>" +
		    "</ul>" +
		    "For 'vmstat' I choose a slightly different approach, this since 'vmstat' is only delivering one row per vmstats-sample period. <br>" +
		    "So in this case I do <b>not</b> do a summary or average calculation.<br>" +
		    "<br>" +
		    "Instead I simply displays <b>all</b> the sampled records I have received from the command with a 'timestamp' column to indicate when it was received.<br>" +
		    "If you instead want to have some kind of average or summary, please notify me and explain...";
		txt = new MultiLineLabel("<html>"+s+"</html>");
		txt.setToolTipText("<html>"+t+"</html>");
		panel.add(txt, "wmin 100, push, grow, wrap 10");

		s = "<b>- What to expect in upcomming releases of "+Version.getAppName()+".</b>";
		t = "In current release there are limited support for User Defined OS Monitoring, which you can add to the Configuration file<br>" +
			"In future release, the plan is to add a <b>Wizard</b> where you can add User Defined Host Monitor Counters and maybe support for diff/rate counters as well.<br>" +
			"<br>" +
		    "With a Used Defined OS Monitoring you could for example Monitor Outputs from <b>your</b> Application Server or in whatever way you think is's usable.<br>" +
			"<br>" +
		    "Below is an example of how a Used Defined Counter could look like, but you might find examples in the Configuration file...<br>" +
		    "<pre>" +
		    "hostmon.udc.TestGoransLs.displayName                   = List of files in $HOME<BR>" +
		    "hostmon.udc.TestGoransLs.description                   = This would be the tooltip help on the Tab for: List of files in $HOME<BR>" +
		    "hostmon.udc.TestGoransLs.osCommand                     = ls -Fl | egrep -v '^d'<BR>" +
		    "hostmon.udc.TestGoransLs.osCommand.isStreaming         = false<BR>" +
		    "hostmon.udc.TestGoransLs.addStrColumn.umode            = {length=10, sqlColumnNumber=1,  parseColumnNumber=1,  isNullable=false, description=xxx}<BR>" +
		    "hostmon.udc.TestGoransLs.addIntColumn.files            = {           sqlColumnNumber=2,  parseColumnNumber=2,  isNullable=true,  description=xxx}<BR>" +
		    "hostmon.udc.TestGoransLs.addStrColumn.owner            = {length=10, sqlColumnNumber=3,  parseColumnNumber=3,  isNullable=true,  description=xxx}<BR>" +
		    "hostmon.udc.TestGoransLs.addStrColumn.group            = {length=10, sqlColumnNumber=4,  parseColumnNumber=4,  isNullable=true,  description=xxx}<BR>" +
		    "hostmon.udc.TestGoransLs.addIntColumn.sizeInBytes      = {           sqlColumnNumber=5,  parseColumnNumber=5,  isNullable=true,  description=xxx}<BR>" +
		    "hostmon.udc.TestGoransLs.addStrColumn.date             = {length=10, sqlColumnNumber=6,  parseColumnNumber=6,  isNullable=true,  description=xxx}<BR>" +
		    "hostmon.udc.TestGoransLs.addStrColumn.time             = {length=5,  sqlColumnNumber=7,  parseColumnNumber=7,  isNullable=true,  description=xxx}<BR>" +
		    "hostmon.udc.TestGoransLs.addStrColumn.filename         = {length=99, sqlColumnNumber=8,  parseColumnNumber=8,  isNullable=true,  description=xxx}<BR>" +
		    "</pre>" +
		    "Some Graph properties can also be added, they are using the same notation as for 'SQL Used Defined Counters'.";
		txt = new MultiLineLabel("<html>"+s+"</html>");
		txt.setToolTipText("<html>"+t+"</html>");
		panel.add(txt, "wmin 100, push, grow, wrap 10");

		return panel;
	}

	public final static String JDBC_URL_TOOLTIP = 
		"<html>" +
		"URL for the above JDBC drivername to connect to a datastore, a couple of template URL for H2 and Sybase JDBC driver<br>" +
		"<br>" +
		"For H2 you can use the following variables: <code>${DATE}, ${SERVERNAME}, ${ASEHOSTNAME}, ${ASETUNE_HOME}, ${ASETUNE_SAVE_DIR}</code><br>" +
		"Explanation of the above variables<br>" +
		"<ul>" +
		"  <li><code>${DATE[:format=fmt[;roll=true|false]]}</code><br>" +
		"    The DATE will be substituted with a timestamp, the format of the timestamp is specified by modifier 'format'.<br>" +
		"    <br>" +
		"    The DATE variable has modifiers, which means you can change the behaviour of the variable.<br>" +
		"    <ul>" +
		"      <li>modifier: <code>format=formatSpecification</code><br>" +
		"          Is how the DATE string should be built.<br>" +
		"          The specification is according to <code>java.text.SimpleDateFormat</code>.<br>" +
		"          The default value is <code>yyyy-MM-dd</code>" +
		"      </li>" +
		"      <li>modifier: <code>roll=true|false</code><br>" +
		"          This means: start/create a new database if we roll over into a new DATE string.<br>" +
		"          With the format=<code>yyyy-MM-dd</code> a new DB will be created every midnight.<br>" +
		"          With the format=<code>yyyy-MM-dd_HH</code> a new DB will be created every hour.<br>" +
		"          The default value for <code>roll</code> is <code>false</code>. " +
		"      </li>" +
		"    </ul>" +
		"    The default value for <code>format</code> can be changed with in the Configuration file with property <code>PersistWriterJdbc.h2DateParseFormat=formatString</code> <br>" +
		"    The default value for <code>roll</code>   can be changed with in the Configuration file with property <code>PersistWriterJdbc.h2NewDbOnDateChange=true|false</code> <br>" +
		"  </li>" +
		"  <li><code>${SERVERNAME} </code> <br>" +
		"    The SERVERNAME will be substituted with the content of the ASE global variable <code>@@servername</code> of which ASE server we are monitoring.<br>" +
		"  </li>" +
		"  <li><code>${ASEHOSTNAME}</code> <br>" +
		"    The ASEHOSTNAME will be substituted with the output from ASE function <code>asehostname()</code> of which ASE server we are monitoring.<br>" +
		"  </li>" +
		"  <li><code>${ASETUNE_HOME}</code> <br>" +
		"    The ASETUNE_HOME will be substituted with the installation path of "+Version.getAppName()+".<br>" +
		"  </li>" +
		"  <li><code>${ASETUNE_SAVE_DIR}</code> <br>" +
		"    The ASETUNE_SAVE_DIR will be substituted with ${ASETUNE_HOME}/data or whatever the environment variable is set to.<br>" +
		"  </li>" +
		"</ul>" +
		"" +
		"Example:<br>" +
		"<code>${ASETUNE_SAVE_DIR}/xxx.${ASEHOSTNAME}.${DATE:format=yyyy-MM-dd.HH;roll=true}</code><br>" +
		"<br>" +
		"The above example, does:" +
		"<ul>" +
		"  <li>H2 dbname would be '<i>asetuneInstallDir</i>/data/xxx.host1.2011-05-31.21'. <br>" +
		"      Every hour a new database will be created." +
		"  </li>" +
		"</ul>" +
		"</html>";

	private JPanel createPcsJdbcPanel()
	{
		JPanel panel = SwingUtils.createPanel("Counter Storage", true);
		panel.setLayout(new MigLayout("","",""));   // insets Top Left Bottom Right
		_pcsPanel = panel;

		_pcsHelp.setText("What Counter Data should be stored in the Persistent Counter Storage\n" +
			"If this is enabled counters that are sampled via the GUI would be stored in a database, this so we can view them later... One good thing about this is that we can view 'baseline' for different configurations or sample sessions\n" +
			"You can also create an offline session and start in no-gui mode instead.");
		
		_pcsWriter_lbl      .setToolTipText("Persistent Counter Storage Implementation(s) that is responsible for Storing the Counter Data.");
		_pcsWriter_cbx      .setToolTipText("Persistent Counter Storage Implementation(s) that is responsible for Storing the Counter Data.");
		_pcsJdbcDriver_lbl  .setToolTipText("JDBC drivername to be used by the Persistent Counter Storage to save Counter Data");
		_pcsJdbcDriver_cbx  .setToolTipText("JDBC drivername to be used by the Persistent Counter Storage to save Counter Data");
		_pcsJdbcUrl_lbl     .setToolTipText(JDBC_URL_TOOLTIP);
		_pcsJdbcUrl_cbx     .setToolTipText(JDBC_URL_TOOLTIP);
		_pcsJdbcUrl_but     .setToolTipText("Open a File chooser dialog to get a filename, for some templates values are replaced");
		_pcsJdbcUsername_lbl.setToolTipText("User name to be used by the Persistent Counter Storage to save Counter Data");
		_pcsJdbcUsername_txt.setToolTipText("User name to be used by the Persistent Counter Storage to save Counter Data");
		_pcsJdbcPassword_lbl.setToolTipText("Password to be used by the Persistent Counter Storage to save Counter Data");
		_pcsJdbcPassword_txt.setToolTipText("Password to be used by the Persistent Counter Storage to save Counter Data");
		_pcsTestConn_but    .setToolTipText("Make a test connection to the above JDBC datastore");
		_pcsH2Option_startH2NetworkServer_chk.setToolTipText("Start the H2 database engine in 'server' mode, so we can connect to the server while the PCS is storing information...");

//		_pcsTestConn_lbl.setForeground(Color.BLUE);
//		_pcsTestConn_lbl.setFont( _ok_lbl.getFont().deriveFont(Font.BOLD) );
//		_pcsTestConn_lbl.setText("xxxx");

		panel.add(_pcsIcon,  "");
		panel.add(_pcsHelp,  "wmin 100, push, grow, wrap 10");

		panel.add(_pcsWriter_lbl,       "");
		panel.add(_pcsWriter_cbx,       "push, grow, wrap");

		panel.add(_pcsJdbcDriver_lbl,   "");
//		panel.add(_pcsJdbcDriver_txt,   "push, grow, split");
		panel.add(_pcsJdbcDriver_cbx,   "push, grow, wrap");
//		panel.add(_pcsJdbcDriver_but,   "wrap");

		panel.add(_pcsJdbcUrl_lbl,      "");
//		panel.add(_pcsJdbcUrl_txt,      "push, grow, split");
		panel.add(_pcsJdbcUrl_cbx,      "push, grow, split");
		panel.add(_pcsJdbcUrl_but,      "wrap");

		panel.add(_pcsJdbcUsername_lbl, "");
		panel.add(_pcsJdbcUsername_txt, "push, grow, wrap");

		panel.add(_pcsJdbcPassword_lbl, "");
		panel.add(_pcsJdbcPassword_txt, "push, grow, wrap");
		
		panel.add(_pcsH2Option_startH2NetworkServer_chk, "skip, hidemode 3, wrap");
		
//		panel.add(_pcsTestConn_lbl, "skip, split, left");
		panel.add(_pcsTestConn_but, "skip, right, wrap");
//		panel.add(_pcsWhatCm_but, "skip 1, right, wrap");
		
		// ADD ACTION LISTENERS
//		_pcsJdbcDriver_but.addActionListener(this);
//		_pcsJdbcUrl_but   .addActionListener(this);

		
		
		_pcsWriter_cbx    .setEditable(true);
		_pcsJdbcDriver_cbx.setEditable(true);
		_pcsJdbcUrl_cbx   .setEditable(true);
		
		_pcsWriter_cbx    .addItem("com.asetune.pcs.PersistWriterJdbc");

		_pcsJdbcDriver_cbx.addItem("org.h2.Driver");
		_pcsJdbcDriver_cbx.addItem(AseConnectionFactory.getDriver());

		// http://www.h2database.com/html/features.html#database_url
		_pcsJdbcUrl_cbx   .addItem("jdbc:h2:file:[<path>]<dbname>");
		_pcsJdbcUrl_cbx   .addItem("jdbc:h2:file:${ASETUNE_SAVE_DIR}/${SERVERNAME}_${DATE}");
		_pcsJdbcUrl_cbx   .addItem("jdbc:h2:file:${ASETUNE_SAVE_DIR}/${ASEHOSTNAME}_${DATE}");
		_pcsJdbcUrl_cbx   .addItem("jdbc:h2:tcp://<host>[:<port>]/<dbname>");
		_pcsJdbcUrl_cbx   .addItem("jdbc:h2:ssl://<host>[:<port>]/<dbname>");

		// http://infocenter.sybase.com/help/topic/com.sybase.dc39001_0605/html/prjdbc/X39384.htm
		_pcsJdbcUrl_cbx   .addItem("jdbc:sybase:Tds:<host>:<port>");
		_pcsJdbcUrl_cbx   .addItem("jdbc:sybase:Tds:<host>:<port>[/<dbname>]");
//		_pcsJdbcUrl_cbx   .addItem("jdbc:sybase:Tds:<host>:<port>[/<dbname>][?OPT1=val&OPT2=val]");

		
		// ACTIONS
		_pcsTestConn_but  .addActionListener(this);
		_pcsJdbcUrl_cbx   .getEditor().getEditorComponent().addKeyListener(this);
		_pcsJdbcUrl_cbx   .addActionListener(this);
		_pcsJdbcUrl_but   .addActionListener(this);
		_pcsJdbcDriver_cbx.addActionListener(this);

		return panel;
	}

	private static final int PCS_TAB_POS_ICON       = 0;
	private static final int PCS_TAB_POS_TAB_NAME   = 1;
	private static final int PCS_TAB_POS_POSTPONE   = 2;
	private static final int PCS_TAB_POS_STORE_PCS  = 3;
	private static final int PCS_TAB_POS_STORE_ABS  = 4;
	private static final int PCS_TAB_POS_STORE_DIFF = 5;
	private static final int PCS_TAB_POS_STORE_RATE = 6;
	private static final int PCS_TAB_POS_LONG_DESC  = 7;
	private static final int PCS_TAB_POS_CM_NAME    = 8;
	private static final int PCS_TAB_POS_MAX        = 9; // NOT a column just the MAX value

	private static final Color PCS_TAB_PCS_COL_BG = new Color(240, 240, 240);

	private JPanel createPcsTablePanel()
	{
		JPanel panel = SwingUtils.createPanel("What Counter Data should be Persisted", true);
		panel.setLayout(new MigLayout("","",""));   // insets Top Left Bottom Right
		// Add a helptext
//		panel.add( new MultiLineLabel(WIZ_HELP), WizardOffline.MigLayoutHelpConstraints );

		_pcsSessionTable = new JXTable()
		{
			private static final long	serialVersionUID	= 1L;

			/** Enable/Disable + add some color to pcsStore, Abs, Diff, Rate */
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column)
			{
				Component c = super.prepareRenderer(renderer, row, column);

				if (column >= PCS_TAB_POS_STORE_PCS && column <= PCS_TAB_POS_STORE_RATE)
				{
					c.setBackground(PCS_TAB_PCS_COL_BG);
					if ((column >= PCS_TAB_POS_STORE_ABS && column <= PCS_TAB_POS_STORE_RATE) || row == 0)
					{
						// if not editable, lets disable it
						// calling isCellEditable instead of getModel().isCellEditable(row, column)
						// does the viewRow->modelRow translation for us.
						c.setEnabled( isCellEditable(row, column) );
					}
				}
				return c;
			}
		};

		// Create a TABLE
		Vector<String> tabHead = new Vector<String>();
		tabHead.setSize(PCS_TAB_POS_MAX);
		tabHead.set(PCS_TAB_POS_POSTPONE,   "Postpone");
		tabHead.set(PCS_TAB_POS_STORE_PCS,  "Sample");
		tabHead.set(PCS_TAB_POS_STORE_ABS,  "Abs");
		tabHead.set(PCS_TAB_POS_STORE_DIFF, "Diff");
		tabHead.set(PCS_TAB_POS_STORE_RATE, "Rate");
		tabHead.set(PCS_TAB_POS_ICON,       "Icon");
		tabHead.set(PCS_TAB_POS_TAB_NAME,   "Short Desc");
		tabHead.set(PCS_TAB_POS_CM_NAME,    "CM Name");
		tabHead.set(PCS_TAB_POS_LONG_DESC,  "Long Description");
		
		Vector<Vector<Object>> tabData = populatePcsTable();

		DefaultTableModel defaultTabModel = new DefaultTableModel(tabData, tabHead)
		{
            private static final long serialVersionUID = 1L;

			@Override
			public Class<?> getColumnClass(int column) 
			{
				if (column == PCS_TAB_POS_POSTPONE)   return Integer.class;
				if (column == PCS_TAB_POS_STORE_PCS)  return Boolean.class;
				if (column == PCS_TAB_POS_STORE_ABS)  return Boolean.class;
				if (column == PCS_TAB_POS_STORE_DIFF) return Boolean.class;
				if (column == PCS_TAB_POS_STORE_RATE) return Boolean.class;
				if (column == PCS_TAB_POS_ICON)       return Icon.class;
				return Object.class;
			}
			@Override
			public boolean isCellEditable(int row, int col)
			{
				if (row == 0) // CMSummary
					return false;

				if (col == PCS_TAB_POS_POSTPONE)
					return true;

				if (col == PCS_TAB_POS_STORE_PCS)
					return true;

				if (col <= PCS_TAB_POS_STORE_RATE)
				{
					// get some values from the MODEL viewRow->modelRow translation should be done before calling isCellEditable
					boolean storePcs    = ((Boolean) getValueAt(row, PCS_TAB_POS_STORE_PCS)).booleanValue();
					String tabName      = (String)   getValueAt(row, PCS_TAB_POS_TAB_NAME);

					//System.out.println("isCellEditable: row="+row+", col="+col+", storePcs="+storePcs+", tabName='"+tabName+"'.");

					// Get CountersModel and check if that model supports editing for Abs, Diff & Rate
					CountersModel cm  = GetCounters.getCmByDisplayName(tabName);
					if (cm != null)
					{
						if (col == PCS_TAB_POS_STORE_ABS)  return storePcs && cm.isPersistCountersAbsEditable();
						if (col == PCS_TAB_POS_STORE_DIFF) return storePcs && cm.isPersistCountersDiffEditable();
						if (col == PCS_TAB_POS_STORE_RATE) return storePcs && cm.isPersistCountersRateEditable();
					}
				}
				return false;
			}
		};
		defaultTabModel.addTableModelListener(this);

		_pcsSessionTable.setModel( defaultTabModel );
//		_pcsSessionTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
//		_pcsSessionTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
//		_pcsSessionTable.setAutoscrolls(true);
//		_pcsSessionTable.doLayout();
		_pcsSessionTable.setShowGrid(false);
//		_pcsSessionTable.setShowHorizontalLines(false);
//		_pcsSessionTable.setShowVerticalLines(false);
//		_pcsSessionTable.setMaximumSize(new Dimension(10000, 10000));

		_pcsSessionTable.setSortable(true);
		_pcsSessionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_pcsSessionTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		_pcsSessionTable.packAll(); // set size so that all content in all cells are visible
		_pcsSessionTable.setSortable(true);
		_pcsSessionTable.setColumnControlVisible(true);

//		SwingUtils.calcColumnWidths(_pcsSessionTable);

		JScrollPane jScrollPane = new JScrollPane();
		jScrollPane.setViewportView(_pcsSessionTable);
//		jScrollPane.setMaximumSize(new Dimension(10000, 10000));
		panel.add(jScrollPane, "push, grow, height 100%, wrap");

		panel.add(_pcsTabSelectAll_but,   "split");
		panel.add(_pcsTabDeSelectAll_but, "split");
		panel.add(_pcsTabTemplate_but,    "");

		// ADD ACTION LISTENERS
		_pcsTabSelectAll_but  .addActionListener(this);
		_pcsTabDeSelectAll_but.addActionListener(this);
		_pcsTabTemplate_but   .addActionListener(this);

		// TOOLTIP
		_pcsTabTemplate_but.setToolTipText("Use current setting of the tabs as a template.");

		return panel;
	}
	
	private JPanel createOfflineJdbcPanel()
	{
		JPanel panel = SwingUtils.createPanel("Counter Storage Read", true);
		panel.setLayout(new MigLayout("","",""));   // insets Top Left Bottom Right
		_offlinePanel = panel;
		
		_offlineHelp.setText("Read Stored Counter Data from an offline storage\n" +
			"The Saved Counter Data can be stored in a database eather by the GUI or NO-GUI mode.\n" +
			"One tip is to store Counter Data for different configurations or test suites, this so we can view the 'baseline' for different configurations or sample sessions\n");

		_offlineJdbcDriver_lbl  .setToolTipText("JDBC drivername to be used by the Persistent Counter Storage to READ Counter Data");
		_offlineJdbcDriver_cbx  .setToolTipText("JDBC drivername to be used by the Persistent Counter Storage to READ Counter Data");
		_offlineJdbcUrl_lbl     .setToolTipText("URL for the above JDBC drivername to connect to a datastore, a couple of template URL for H2 and Sybase JDBC driver");
		_offlineJdbcUrl_cbx     .setToolTipText("URL for the above JDBC drivername to connect to a datastore, a couple of template URL for H2 and Sybase JDBC driver");
		_offlineJdbcUrl_but     .setToolTipText("Open a File chooser dialog to get a filename, for some templates values are replaced");
		_offlineJdbcUsername_lbl.setToolTipText("User name to be used by the Persistent Counter Storage to READ Counter Data");
		_offlineJdbcUsername_txt.setToolTipText("User name to be used by the Persistent Counter Storage to READ Counter Data");
		_offlineJdbcPassword_lbl.setToolTipText("Password to be used by the Persistent Counter Storage to READ Counter Data");
		_offlineJdbcPassword_txt.setToolTipText("Password to be used by the Persistent Counter Storage to READ Counter Data");
		_offlineTestConn_but    .setToolTipText("Make a test connection to the above JDBC datastore");
		_offlineCheckForNewSessions_chk.setToolTipText("<html>If "+Version.getAppName()+" still collects data into the offline database, go and check for new samples.<br>Using this option we can run "+Version.getAppName()+" in offline mode while the GUI can read from the shared database.</html>");
		_offlineH2Option_startH2NwSrv_chk.setToolTipText("<html>Start the H2 database engine in 'server' mode.<br>This enables other "+Version.getAppName()+" client to be connected to the same database.<br>Or you can use any third party product database viewer to read the content at the same time.</html>");

//		_offlineTestConn_lbl.setForeground(Color.BLUE);
//		_offlineTestConn_lbl.setFont( _ok_lbl.getFont().deriveFont(Font.BOLD) );
//		_offlineTestConn_lbl.setText("xxxx");

		panel.add(_offlineIcon,  "");
		panel.add(_offlineHelp,  "wmin 100, push, grow, wrap 15");

		panel.add(_offlineJdbcDriver_lbl,   "");
		panel.add(_offlineJdbcDriver_cbx,   "push, grow, wrap");

		panel.add(_offlineJdbcUrl_lbl,      "");
		panel.add(_offlineJdbcUrl_cbx,      "push, grow, split");
		panel.add(_offlineJdbcUrl_but,      "wrap");

		panel.add(_offlineJdbcUsername_lbl, "");
		panel.add(_offlineJdbcUsername_txt, "push, grow, wrap");

		panel.add(_offlineJdbcPassword_lbl, "");
		panel.add(_offlineJdbcPassword_txt, "push, grow, wrap");
		
		panel.add(_offlineCheckForNewSessions_chk, "skip, wrap");
		panel.add(_offlineH2Option_startH2NwSrv_chk, "skip, hidemode 3, wrap");
		
//		panel.add(_offlineTestConn_lbl, "skip, split, left");
		panel.add(_offlineTestConn_but, "skip, right, wrap");
//		panel.add(_offlineWhatCm_but, "skip 1, right, wrap");
		
		_offlineJdbcDriver_cbx.setEditable(true);
		_offlineJdbcUrl_cbx   .setEditable(true);
		
		_offlineJdbcDriver_cbx.addItem("org.h2.Driver");
		_offlineJdbcDriver_cbx.addItem(AseConnectionFactory.getDriver());

		// http://www.h2database.com/html/features.html#database_url
		_offlineJdbcUrl_cbx   .addItem("jdbc:h2:file:[<path>]<dbname>;IFEXISTS=TRUE");
		_offlineJdbcUrl_cbx   .addItem("jdbc:h2:zip:<zipFileName>!/<dbname>");
		_offlineJdbcUrl_cbx   .addItem("jdbc:h2:tcp://<host>[:<port>]/<dbname>");
		_offlineJdbcUrl_cbx   .addItem("jdbc:h2:ssl://<host>[:<port>]/<dbname>");
 
		// http://infocenter.sybase.com/help/topic/com.sybase.dc39001_0605/html/prjdbc/X39384.htm
		_offlineJdbcUrl_cbx   .addItem("jdbc:sybase:Tds:<host>:<port>");
		_offlineJdbcUrl_cbx   .addItem("jdbc:sybase:Tds:<host>:<port>[/<dbname>]");
//		_offlineJdbcUrl_cbx   .addItem("jdbc:sybase:Tds:<host>:<port>[/<dbname>][?OPT1=val,&OPT2=val]");

		
		// ACTIONS
		_offlineJdbcDriver_cbx  .addActionListener(this);
		_offlineTestConn_but    .addActionListener(this);
		_offlineJdbcUrl_cbx     .getEditor().getEditorComponent().addKeyListener(this);
		_offlineJdbcUrl_cbx     .addActionListener(this);
		_offlineJdbcPassword_txt.addActionListener(this);
		_offlineJdbcUrl_but     .addActionListener(this);

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
		_ok           .addActionListener(this);
		_cancel       .addActionListener(this);

		return panel;
	}
	
	private Vector<Vector<Object>> populatePcsTable()
	{
		Vector<Vector<Object>> tab = new Vector<Vector<Object>>();
		Vector<Object>         row = new Vector<Object>();

		if (_useCmForPcsTable)
		{
			Iterator<CountersModel> iter = GetCounters.getCmList().iterator();
			while (iter.hasNext())
			{
				CountersModel cm = iter.next();
				
				if (cm != null)
				{
					row = new Vector<Object>();
					row.setSize(PCS_TAB_POS_MAX);
					row.set(PCS_TAB_POS_POSTPONE,   new Integer(cm.getPostponeTime()));
					row.set(PCS_TAB_POS_STORE_PCS,  new Boolean(cm.isPersistCountersEnabled() || cm.isBackgroundDataPollingEnabled()));
					row.set(PCS_TAB_POS_STORE_ABS,  new Boolean(cm.isPersistCountersAbsEnabled()));
					row.set(PCS_TAB_POS_STORE_DIFF, new Boolean(cm.isPersistCountersDiffEnabled()));
					row.set(PCS_TAB_POS_STORE_RATE, new Boolean(cm.isPersistCountersRateEnabled()));

					row.set(PCS_TAB_POS_TAB_NAME,   cm.getDisplayName());
					row.set(PCS_TAB_POS_CM_NAME,    cm.getName());
					row.set(PCS_TAB_POS_ICON,       cm.getTabPanel() == null ? null : cm.getTabPanel().getIcon());
					row.set(PCS_TAB_POS_LONG_DESC,  cm.getDescription().replaceAll("\\<.*?\\>", ""));
//					row.add(new Boolean( cm.isPersistCountersEnabled() ));
//					row.add(cm.getTabPanel() == null ? null : cm.getTabPanel().getIcon());
//					row.add(cm.getName());
//					row.add(cm.getDisplayName());
//					row.add(cm.getDescription().replaceAll("\\<.*?\\>", "")); // STRIP HTML Tags from the description.
					tab.add(row);
					
					if (cm.getName().equals(SummaryPanel.CM_NAME))
					{
						row.set(PCS_TAB_POS_ICON, SwingUtils.readImageIcon(Version.class, "images/summary_tab.png"));
					}
				}
			}
		}
		else
		{
			row = new Vector<Object>();
			row.setSize(PCS_TAB_POS_MAX);
			row.set(PCS_TAB_POS_POSTPONE,   new Integer(0));
			row.set(PCS_TAB_POS_STORE_PCS,  new Boolean(true));
			row.set(PCS_TAB_POS_STORE_ABS,  new Boolean(true));
			row.set(PCS_TAB_POS_STORE_DIFF, new Boolean(true));
			row.set(PCS_TAB_POS_STORE_RATE, new Boolean(true));
			row.set(PCS_TAB_POS_TAB_NAME,   "Summary");
			row.set(PCS_TAB_POS_CM_NAME,    "cmSummary");
			row.set(PCS_TAB_POS_ICON,       SwingUtils.readImageIcon(Version.class, "images/cm_summary_activity.png"));
			row.set(PCS_TAB_POS_LONG_DESC,  "All the fields on the left hand side of the graphs.");
			tab.add(row);
	
			row = new Vector<Object>();
			row.setSize(PCS_TAB_POS_MAX);
			row.set(PCS_TAB_POS_POSTPONE,   new Integer(0));
			row.set(PCS_TAB_POS_STORE_PCS,  new Boolean(true));
			row.set(PCS_TAB_POS_STORE_ABS,  new Boolean(true));
			row.set(PCS_TAB_POS_STORE_DIFF, new Boolean(true));
			row.set(PCS_TAB_POS_STORE_RATE, new Boolean(true));
			row.set(PCS_TAB_POS_TAB_NAME,   "CPU Usage");
			row.set(PCS_TAB_POS_CM_NAME,    "cmCpu");
			row.set(PCS_TAB_POS_ICON,       SwingUtils.readImageIcon(Version.class, "images/cm_engine_activity.png"));
			row.set(PCS_TAB_POS_LONG_DESC,  "bla bla bla... asfdha dkjfg askj gfakj gfkajgshd fagsakgdfakdfhs kjfhgoiqay edatfshjghv kfdsjhgaks dfajhdfskjdf glkash df.");
			tab.add(row);
	
			row = new Vector<Object>();
			row.setSize(PCS_TAB_POS_MAX);
			row.set(PCS_TAB_POS_POSTPONE,   new Integer(0));
			row.set(PCS_TAB_POS_STORE_PCS,  new Boolean(true));
			row.set(PCS_TAB_POS_STORE_ABS,  new Boolean(true));
			row.set(PCS_TAB_POS_STORE_DIFF, new Boolean(true));
			row.set(PCS_TAB_POS_STORE_RATE, new Boolean(true));
			row.set(PCS_TAB_POS_TAB_NAME,   "Device Usage");
			row.set(PCS_TAB_POS_CM_NAME,    "cmDevice");
			row.set(PCS_TAB_POS_ICON,       SwingUtils.readImageIcon(Version.class, "images/cm_device_activity.png"));
			row.set(PCS_TAB_POS_LONG_DESC,  "wwwwwwwwwwwwwwww wwww ttttt uuuuuu bla bla bla... hhhhhhhhhhhhh  kkkkkkkkkkkk yyyyyyy ssssssssssssssssss ggggggggggggg w wwww aaaaa.");
			tab.add(row);
	
			for (int i=0; i<40; i++)
			{
				row = new Vector<Object>();
				row.setSize(PCS_TAB_POS_MAX);
				row.set(PCS_TAB_POS_POSTPONE,   new Integer(0));
				row.set(PCS_TAB_POS_STORE_PCS,  new Boolean(true));
				row.set(PCS_TAB_POS_STORE_ABS,  new Boolean(true));
				row.set(PCS_TAB_POS_STORE_DIFF, new Boolean(true));
				row.set(PCS_TAB_POS_STORE_RATE, new Boolean(true));
				row.set(PCS_TAB_POS_TAB_NAME,   "Dummy Tab "+i);
				row.set(PCS_TAB_POS_CM_NAME,    "cmDummy"+i);
				row.set(PCS_TAB_POS_ICON,       SwingUtils.readImageIcon(Version.class, "images/ud_counter_activity.png"));
				row.set(PCS_TAB_POS_LONG_DESC,  UUID.randomUUID().toString() + " : " + UUID.randomUUID().toString());
				tab.add(row);
			}			
		}

		return tab;
	}
	/*---------------------------------------------------
	** END: component initialization
	**---------------------------------------------------
	*/


	
	
	/*---------------------------------------------------
	** BEGIN: helper methods
	**---------------------------------------------------
	*/

	private void toggleCounterStorageTab()
	{
		if (_showPcsTab)
		{
			if (_aseOptionStore_chk.isSelected())
			{
				_tab.setEnabledAt(TAB_POS_PCS, true);
			}
			else
			{
				_tab.setEnabledAt(TAB_POS_PCS, false);
			}
		}
	}
	private void toggleHostmonTab()
	{
		if (_showHostmonTab)
		{
			if (_aseHostMonitor_chk.isSelected())
			{
				_tab.setEnabledAt(TAB_POS_HOSTMON, true);
			}
			else
			{
				_tab.setEnabledAt(TAB_POS_HOSTMON, false);
			}
		}
	}
	private void validateContents()
	{
		String problem = "";

		if (_tab.getSelectedIndex() == TAB_POS_OFFLINE)
		{
			String url = _offlineJdbcUrl_cbx.getEditor().getItem().toString();
			// URL
			if ( url.matches(".*<.*>.*") )
				problem = "Replace the <template> with something.";

			if ( url.matches(".*\\[.*\\].*"))
				problem = "Replace the [template] with something or delete it.";
		}

		else if (_aseOptionStore_chk.isSelected())
		{
			String url = _pcsJdbcUrl_cbx.getEditor().getItem().toString();
			// URL
			if ( url.matches(".*<.*>.*") )
				problem = "Replace the <template> with something.";

			if ( url.matches(".*\\[.*\\].*"))
				problem = "Replace the [template] with something or delete it.";

			// SESSIONS TABLE
			int rows = 0;
			TableModel tm = _pcsSessionTable.getModel();
			for (int r=0; r<tm.getRowCount(); r++)
			{
				if ( ((Boolean)tm.getValueAt(r, PCS_TAB_POS_STORE_PCS)).booleanValue() )
					rows++;
			}
			if (rows == 0 && problem.equals(""))
				problem = "Atleast one session needs to be checked.";
		}

		else if (_aseHostMonitor_chk.isSelected())
		{
			String username = _hostmonUser_txt  .getText();
			String password = _hostmonPasswd_txt.getText();
			String hostname = _hostmonHost_txt  .getText();
			String portStr  = _hostmonPort_txt  .getText();

			try { Integer.parseInt(portStr); } 
			catch (NumberFormatException e) 
			{
				problem = "SSH Port number must be an integer";
			}

			if (username.trim().equals(""))
				problem = "SSH username must be specified";

			if (password.trim().equals(""))
				problem = "SSH password must be specified";

			if (hostname.trim().equals(""))
				problem = "SSH hostname must be specified";
		}
		else
		{
			// host/port fields has to match
			String[] hosts = StringUtil.commaStrToArray(_aseHost_txt.getText());
			String[] ports = StringUtil.commaStrToArray(_asePort_txt.getText());
			if (hosts.length != ports.length)
			{
				problem = "Host has "+hosts.length+" entries, Port has "+ports.length+" entries. They must match";
			}
		}
		
		_ok_lbl.setText(problem);
		_pcsTestConn_lbl.setText(problem);
		_tab.setForegroundAt(TAB_POS_PCS,     _tab.getForegroundAt(TAB_POS_ASE));
		_tab.setForegroundAt(TAB_POS_OFFLINE, _tab.getForegroundAt(TAB_POS_ASE));

		if (problem.equals(""))
		{
			_ok                 .setEnabled(true);
			_offlineTestConn_but.setEnabled(true);
			_pcsTestConn_but    .setEnabled(true);
		}
		else
		{
			_ok                 .setEnabled(false);
			_offlineTestConn_but.setEnabled(false);
			_pcsTestConn_but    .setEnabled(false);

			if (_tab.getSelectedIndex() == TAB_POS_OFFLINE)
				_tab.setForegroundAt(TAB_POS_OFFLINE, Color.RED);
			else
				_tab.setForegroundAt(TAB_POS_PCS, Color.RED);
		}
	}

	/**
	 * Refresh the Server combo box
	 */
	private void refreshServers()
	{
		_aseServer_cbx .refresh();
		_aseHost_txt   .setText("");
		_asePort_txt   .setText("");
		_aseOptions_txt.setText("");
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
				String interfacesFileName = "%SYBASE%\\ini\\sql.ini";
				if (PlatformUtils.getCurrentPlattform() != PlatformUtils.Platform_WIN)
					interfacesFileName = "$SYBASE/interfaces";

				SwingUtils.showWarnMessage(this, 
						"Name Service file dosn't exists", 
						"The Name Service file '"+file+"' doesn't exists.\n" +
						    "\n" +
						    "Name Service file is used to lookup a Sybase Server into a hostname and port number.\n" +
						    "The Name Service file is normally named '"+interfacesFileName+"'.", 
						null);
				return;
			}
			AseConnectionFactory.setInterfaces(file);
			refreshServers();
			_aseIfile_save = file;
		}
		catch (Exception e)
		{
			SwingUtils.showErrorMessage(this, "Problems setting new Name Service file", 
				"Problems setting the Name Service file '"+file+"'." +
				"\n\n" + e.getMessage(), e);
			
			_aseIfile_txt.setText(_aseIfile_save);
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
				if (_tab.getSelectedIndex() == TAB_POS_ASE)
				{
					if (_aseUser_txt  .getText().trim().equals("")) {_aseUser_txt  .requestFocus(); return; }
					if (_asePasswd_txt.getText().trim().equals("")) {_asePasswd_txt.requestFocus(); return; }
					if (_aseHost_txt  .getText().trim().equals("")) {_aseHost_txt  .requestFocus(); return; }
					if (_asePort_txt  .getText().trim().equals("")) {_asePort_txt  .requestFocus(); return; }
				}
				else if (_tab.getSelectedIndex() == TAB_POS_HOSTMON)
				{
					if (_hostmonUser_txt  .getText().trim().equals("")) {_hostmonUser_txt  .requestFocus(); return; }
					if (_hostmonPasswd_txt.getText().trim().equals("")) {_hostmonPasswd_txt.requestFocus(); return; }
					if (_hostmonHost_txt  .getText().trim().equals("")) {_hostmonHost_txt  .requestFocus(); return; }
					if (_hostmonPort_txt  .getText().trim().equals("")) {_hostmonPort_txt  .requestFocus(); return; }
				}
				else if (_tab.getSelectedIndex() == TAB_POS_OFFLINE)
				{
					if (_offlineJdbcUsername_txt.getText().trim().equals("")) {_offlineJdbcUsername_txt.requestFocus(); return; }
					if (_offlineJdbcPassword_txt.getText().trim().equals("")) {_offlineJdbcPassword_txt.requestFocus(); return; }
				}

				_ok.requestFocus();
			}
		};
		SwingUtilities.invokeLater(deferredAction);
	}

	public static boolean checkReconnectVersion(Connection conn)
	{
		// If reconnected, check if it's the same version.
		// NOTE: MonTablesDictionary is initialized at a later stage first time we connect
		//       so if we dont have a instance, this would be the first time we connect.
		//       THIS MIGHT CHANGE IN FUTURE.
		if (MonTablesDictionary.hasInstance())
		{
			MonTablesDictionary mtd = MonTablesDictionary.getInstance();
			if ( ! mtd.isInitialized() )
			{
				_logger.debug("checkReconnectVersion(): MonTablesDictionary.isInitialized()=false. I'll just return true here...");
				return true;
			}
			int currentVersion = mtd.aseVersionNum;
			int newVersion     = AseConnectionUtils.getAseVersionNumber(conn);
	
			if (currentVersion <= 0)
			{
				_logger.debug("checkReconnectVersion(): MonTablesDictionary.hasInstance()=true, Are we checking this a bit early... currentVersion='"+currentVersion+"', newVersion='"+newVersion+"'. since currentVersion is <= 0, I'll just return true here...", new Exception("Dummy Exception to get call stack..."));
				return true;
			}

			if (currentVersion != newVersion)
			{
				String msg = "<html>" +
						"Connecting to a different ASE Version, This is NOT supported now... (previousVersion='"+AseConnectionUtils.versionIntToStr(currentVersion)+"', connectToVersion='"+AseConnectionUtils.versionIntToStr(newVersion)+"'). <br>" +
						"To connect to another ASE Version, you need to restart the application." +
						"</html>";
				if (AseTune.hasGUI())
					JOptionPane.showMessageDialog(MainFrame.getInstance(), msg, Version.getAppName()+" - connect check", JOptionPane.WARNING_MESSAGE);
				_logger.warn(msg);
				return false;
			}
			else
			{
				_logger.info("Re-connected to the same ASE Version as priviously. Version is '"+currentVersion+"'.");
				return true;
			}
		}
		return true;
	}

	/**
	 * Make a connection to the ASE
	 * @return
	 */
	private boolean aseConnect()
	{
		// -------------------------------------------
		// if RAW URL is used
		// -------------------------------------------
		// FIXME: this we need to redo, I need to remember stuff for reconnects etc
		if (_aseConnUrl_chk.isSelected())
		{
//			AseConnectionFactory.setAppName ( Version.getAppName() );
//			AseConnectionFactory.setUser    ( _aseUser_txt.getText() );
//			AseConnectionFactory.setPassword( _asePasswd_txt.getText() );

			String username = _aseUser_txt.getText();
			String password = _asePasswd_txt.getText();
			String rawUrl   = _aseConnUrl_txt.getText();

			Properties props = new Properties();
			props.put("user",     username);
			props.put("password", password);
			try
			{
				_logger.info("Connecting to ASE using RAW-URL username='"+username+"', URL='"+rawUrl+"'.");
				_aseConn = ConnectionProgressDialog.connectWithProgressDialog(this, AseConnectionFactory.getDriver(), rawUrl, props, _checkAseCfg, _sshConn);
//				_aseConn = AseConnectionFactory.getConnection(AseConnectionFactory.getDriver(), rawUrl, props, null);
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
				SwingUtils.showErrorMessage(this, "Problems Connecting", "Problems when connecting to the data server.\n\n" + e.getMessage(), e);
				return false;
			}
			//<<<<<----- RETURN after this
		}

		// -------------------------------------------
		// NORMAL connect starts here
		// -------------------------------------------
		String portStr = _asePort_txt.getText();
		String[] sa = StringUtil.commaStrToArray(portStr);
		for (int i=0; i<sa.length; i++)
		{
			try { Integer.parseInt(sa[i]); } 
			catch (NumberFormatException e) 
			{
				SwingUtils.showErrorMessage(this, "Problem with port number", 
					"The port number '"+sa[i]+"' is not a number.", e);
				return false;
			}
		}
		if (sa == null || (sa != null && sa.length == 0) )
		{
			SwingUtils.showErrorMessage(this, "Problem with port number", 
					"The port number '"+portStr+"' is either missing or is not a number.", null);
				return false;
		}

		_logger.debug("Setting connection info to AseConnectionFactory appname='"+Version.getAppName()
				+"', user='"+_aseUser_txt.getText()+"', password='"+_asePasswd_txt.getText()
				+"', host='"+_aseHost_txt.getText()+"', port='"+_asePort_txt.getText()+"'.");
		
		AseConnectionFactory.setAppName ( Version.getAppName() );
		AseConnectionFactory.setHostName( Version.getVersionStr() );
		AseConnectionFactory.setUser    ( _aseUser_txt.getText() );
		AseConnectionFactory.setPassword( _asePasswd_txt.getText() );
//		AseConnectionFactory.setHost    ( _aseHost_txt.getText() );
//		AseConnectionFactory.setPort    ( port );
		AseConnectionFactory.setHostPort(_aseHost_txt.getText(), _asePort_txt.getText());

		// set options... if there are any
		if ( _aseOptions_txt.getText().trim().equals("") )
		{
			Map<String,String> options = null;
			AseConnectionFactory.setProperties(options);
		}
		else
		{
			Map<String,String> options = StringUtil.parseCommaStrToMap(_aseOptions_txt.getText());
			AseConnectionFactory.setProperties(options);
		}
		
		try
		{
			_logger.info("Connecting to ASE '"+AseConnectionFactory.getServer()+"'.  hostPortStr='"+AseConnectionFactory.getHostPortStr()+"', user='"+AseConnectionFactory.getUser()+"'.");

			String urlStr = "jdbc:sybase:Tds:" + AseConnectionFactory.getHostPortStr();
			_aseConn = ConnectionProgressDialog.connectWithProgressDialog(this, urlStr, _checkAseCfg, _sshConn);
//			_aseConn = AseConnectionFactory.getConnection();

			return true;
		}
		catch (SQLException e)
		{
			// The below shows a showErrorMessage
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
	
	@SuppressWarnings("unused")
	private boolean checkForMonitorOptions()
	{
//System.out.println(">>>>> checkForMonitorOptions");
		MainFrame.setStatus(MainFrame.ST_STATUS_FIELD, "Checking for Monitor Options...");

		boolean b = AseConnectionUtils.checkForMonitorOptions(_aseConn, _aseUser_txt.getText(), true, this, "enable monitoring");

		MainFrame.setStatus(MainFrame.ST_CONNECT);
//System.out.println("<<<<< checkForMonitorOptions");
		return b;
	}

	/**
	 * Make a SSH connection object, but do NOT connect
	 * @return
	 */
	private SshConnection hostmonCreateConnectionObject()
	{
		String username = _hostmonUser_txt  .getText();
		String password = _hostmonPasswd_txt.getText();
		String hostname = _hostmonHost_txt  .getText();
		String portStr  = _hostmonPort_txt  .getText();
		int port = 22;

		try { port = Integer.parseInt(portStr); } 
		catch (NumberFormatException e) 
		{
			SwingUtils.showErrorMessage(this, "Problem with port number", 
				"The port number '"+portStr+"' is not a number.", e);
			return null;
		}


		_logger.info("Creating SSH Connection-Object to hostname='"+hostname+"'.  port='"+port+"', username='"+username+"'.");
		return new SshConnection(hostname, port, username, password);
	}
	
	/**
	 * Make a SSH connection to the remote host
	 * @return
	 */
//	private boolean hostmonConnect()
//	{
//		try
//		{
//			if (_sshConn == null)
//				_sshConn = hostmonCreateConnectionObject();
//
//			_logger.info("Connecting to SSH hostname='"+_sshConn.getHost()+"'.  port='"+_sshConn.getPort()+"', username='"+_sshConn.getUsername()+"'.");
//			_sshConn.connect();
//
//			return true;
//		}
//		catch (Exception e)
//		{
//			SwingUtils.showErrorMessage(this, "Problems Connecting", 
//					"Problems when connecting to the SSH server." +
//					"\n\n" + e.getMessage(), e);
//			return false;
//		}
//	}

	/**
	 * Make a connection to the PCS storage
	 * @return
	 */
	private boolean pcsConnect()
	{
		//---------------------------
		// START the Persistent Storage thread
		//---------------------------
		PersistentCounterHandler pch = null;
		try
		{
			checkForH2LocalDrive(null);

			Configuration pcsProps = new Configuration();

			String pcsAll = _pcsWriter_cbx.getEditor().getItem().toString();
			pcsProps.put("PersistentCounterHandler.WriterClass", pcsAll);

			// pcsAll "could" be a ',' separated string
			// But I dont know how to set the properties for those Writers
			String[] pcsa = pcsAll.split(",");
			for (int i=0; i<pcsa.length; i++)
			{
				String pcs = pcsa[i].trim();
				
				if (pcs.equals("com.asetune.pcs.PersistWriterJdbc"))
				{
					pcsProps.put("PersistWriterJdbc.jdbcDriver", _pcsJdbcDriver_cbx  .getEditor().getItem().toString());
					pcsProps.put("PersistWriterJdbc.jdbcUrl",    _pcsJdbcUrl_cbx     .getEditor().getItem().toString());
					pcsProps.put("PersistWriterJdbc.jdbcUser",   _pcsJdbcUsername_txt.getText());
					pcsProps.put("PersistWriterJdbc.jdbcPasswd", _pcsJdbcPassword_txt.getText());

					pcsProps.put("PersistWriterJdbc.startH2NetworkServer", _pcsH2Option_startH2NetworkServer_chk.isSelected() + "");
				}
			}
			
			pch = new PersistentCounterHandler();
			pch.init( pcsProps );
			
			if (pch.hasWriters())
			{
				PersistentCounterHandler.setInstance(pch);
				pch.start();

				return true;
			}
			_logger.error("No writers installed to the PersistentCounterHandler.");
			return false;
		}
		catch (Exception e)
		{
			_logger.error("Problems initializing PersistentCounterHandler", e);
			return false;
		}
//		_pcsConn = jdbcConnect(Version.getAppName(), 
//				_pcsJdbcDriver_cbx.getEditor().getItem().toString(), 
//				_pcsJdbcUrl_cbx.getEditor().getItem().toString(),
//				_pcsJdbcUsername_txt.getText(), 
//				_pcsJdbcPassword_txt.getText());
//		return _pcsConn != null;
	}
	
	/**
	 * Make a connection to the OFFLINE read
	 * @return
	 */
	private boolean offlineConnect()
	{
		String jdbcDriver = _offlineJdbcDriver_cbx.getEditor().getItem().toString();
		String jdbcUrl    = _offlineJdbcUrl_cbx.getEditor().getItem().toString();
		String jdbcUser   = _offlineJdbcUsername_txt.getText();
		String jdbcPasswd = _offlineJdbcPassword_txt.getText();

		boolean startH2NetworkServer = _offlineH2Option_startH2NwSrv_chk.isSelected();

		String configStr = "jdbcDriver='"+jdbcDriver+"', jdbcUrl='"+jdbcUrl+"', jdbcUser='"+jdbcUser+"', jdbcPasswd='*hidden*'.";
		_logger.info("Configuration for PersistReader component named 'PersistReader': "+configStr);

		// Everything could NOT be done with the jdbcUrl... so here goes some special
		// start the H2 TCP Server
		if ( jdbcDriver.equals("org.h2.Driver") && startH2NetworkServer )
		{
			try
			{
				_logger.info("Starting a H2 TCP server.");
				String[] args = new String[] { "-tcpAllowOthers" };
				org.h2.tools.Server h2ServerTcp = org.h2.tools.Server.createTcpServer(args);
				h2ServerTcp.start();
	
	//			_logger.info("H2 TCP server, listening on port='"+h2Server.getPort()+"', url='"+h2Server.getURL()+"', service='"+h2Server.getService()+"'.");
				_logger.info("H2 TCP server, url='"+h2ServerTcp.getURL()+"', service='"+h2ServerTcp.getService()+"'.");
	
				if (true)
				{
					_logger.info("Starting a H2 WEB server.");
					String[] argsWeb = new String[] { "-trace" };
					org.h2.tools.Server h2ServerWeb = org.h2.tools.Server.createWebServer(argsWeb);
					h2ServerWeb.start();
	
					_logger.info("H2 WEB server, url='"+h2ServerWeb.getURL()+"', service='"+h2ServerWeb.getService()+"'.");
				}
			}
			catch (SQLException e) 
			{
				_logger.warn("Problem starting H2 network service", e);
			}
		}

		_offlineConn = jdbcConnect(Version.getAppName(), 
				jdbcDriver, 
				jdbcUrl,
				jdbcUser, 
				jdbcPasswd);

		if (_offlineConn == null)
			return false;

		// Check if the connected database looks like a OFFLINE database
		// Just check for a 'set' of known tables.
		boolean isOfflineDb = PersistReader.isOfflineDb(_offlineConn);
		if ( ! isOfflineDb )
		{
			try { _offlineConn.close(); }
			catch (SQLException e) {}
			_offlineConn = null;

			String msg = "This is NOT a valid offline database. No "+Version.getAppName()+" system table exists.";
			if (jdbcDriver.equals("org.h2.Driver"))
			{
				if (jdbcUrl.indexOf("IFEXISTS=TRUE") == -1)
					msg += " Please append ';IFEXISTS=TRUE' to the URL, this stops H2 to create an empty database if it didn't exist.";
			}

			_logger.error(msg);
			JOptionPane.showMessageDialog(this, 
					msg, 
					Version.getAppName()+" - offline db check", 
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		//
		// FIXME: open a dialog where we show what information is in the offline database store
//		new OfflineSessionVeiwer(_owner);
//		new OfflineSessionVeiwer(null, _offlineConn, true);

		return _offlineConn != null;
	}

	/**
	 * Test to connect
	 */
	private Connection jdbcConnect(String appname, String driver, String url, String user, String passwd)
	{
		try
		{
			Class.forName(driver).newInstance();
			Properties props = new Properties();
			props.put("user", user);
			props.put("password", passwd);
	
			_logger.debug("getConnection to driver='"+driver+"', url='"+url+"', user='"+user+"'.");
			Connection conn = DriverManager.getConnection(url, props);
	
			return conn;
		}
		catch (SQLException e)
		{
			StringBuffer sb = new StringBuffer();
			while (e != null)
			{
				sb.append( "\n" );
				sb.append( e.getMessage() );
				e = e.getNextException();
			}
			JOptionPane.showMessageDialog(this, "Connection FAILED.\n\n"+sb.toString(), Version.getAppName()+" - jdbc connect", JOptionPane.ERROR_MESSAGE);
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this, "Connection FAILED.\n\n"+e.toString(),  Version.getAppName()+" - jdbc connect", JOptionPane.ERROR_MESSAGE);
		}
		return null;
	}

	/**
	 * Test to connect
	 */
	private boolean testJdbcConnection(String appname, String driver, String url, String user, String passwd)
	{
		try
		{
			Class.forName(driver).newInstance();
			Properties props = new Properties();
			props.put("user", user);
			props.put("password", passwd);
	
			_logger.debug("Try getConnection to driver='"+driver+"', url='"+url+"', user='"+user+"'.");
			Connection conn = DriverManager.getConnection(url, props);
			conn.close();
	
			JOptionPane.showMessageDialog(this, "Connection succeeded.", Version.getAppName()+" - connect check", JOptionPane.INFORMATION_MESSAGE);
			return true;
		}
		catch (SQLException e)
		{
			StringBuffer sb = new StringBuffer();
			while (e != null)
			{
				sb.append( "\n" );
				sb.append( e.getMessage() );
				e = e.getNextException();
			}
			JOptionPane.showMessageDialog(this, "Connection FAILED.\n\n"+sb.toString(), Version.getAppName()+" - connect check", JOptionPane.ERROR_MESSAGE);
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this, "Connection FAILED.\n\n"+e.toString(),  Version.getAppName()+" - connect check", JOptionPane.ERROR_MESSAGE);
		}
		return false;
	}
	/*---------------------------------------------------
	** END: helper methods
	**---------------------------------------------------
	*/

	
	
	
	
	/*---------------------------------------------------
	** BEGIN: implementing TableModelListener, ActionListener, KeyListeners
	**---------------------------------------------------
	*/
	public void tableChanged(TableModelEvent e)
	{
		// This wasnt kicked off for a table change...
		validateContents();
	}

	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();
		String action = e.getActionCommand();

		// --- ASE: CHECKBOX: SERVERS ---
		if (_aseServer_cbx.equals(source))
		{
			_logger.debug("_server_cbx.actionPerformed(): getSelectedIndex()='"+_aseServer_cbx.getSelectedIndex()+"', getSelectedItem()='"+_aseServer_cbx.getSelectedItem()+"'.");
	
			// NOTE: index 0 is "host:port" or SERVER_FIRST_ENTRY("-CHOOSE A SERVER-")
			//       so we wont touch host_txt and port_txt if we are on index 0
			if ( _aseServer_cbx.getSelectedIndex() > 0 )
			{
				String server = (String) _aseServer_cbx.getSelectedItem();
				
				String hosts       = AseConnectionFactory.getIHosts(server, ", ");
				String ports       = AseConnectionFactory.getIPorts(server, ", ");
				String hostPortStr = AseConnectionFactory.getIHostPortStr(server);
				_aseHost_txt.setText(hosts);
				_asePort_txt.setText(ports);

				// Try to load user name & password for this server
				loadPropsForServer(hostPortStr);
			}
		}

		// --- ASE: BUTTON: "..." Open file to get interfaces/sql.ini file ---
		if (_aseIfile_but.equals(source))
		{
			String dir = System.getProperty("SYBASE");
			if (dir == null)
			{
				dir = System.getenv("SYBASE");
			}
			if (dir != null)
			{
//				if ( System.getProperty("os.name").startsWith("Windows"))
				if ( PlatformUtils.getCurrentPlattform() == PlatformUtils.Platform_WIN )
					dir += "\\ini";
			}

			JFileChooser fc = new JFileChooser(dir);
			int returnVal = fc.showOpenDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION) 
			{
				_aseIfile_txt.setText( fc.getSelectedFile().getAbsolutePath() );
				loadNewInterfaces( _aseIfile_txt.getText() );
			}
		}
		
		// --- ASE: BUTTON: "Edit" edit interfaces/sql.ini file ---
		if (_aseEditIfile_but.equals(source))
		{
			String currentSrvName = _aseServer_cbx.getSelectedItem().toString(); 
			String ifile = _aseIfile_txt.getText(); 
			InterfaceFileEditor ife = new InterfaceFileEditor(this, ifile);
			int rc = ife.open();
			if (rc == InterfaceFileEditor.OK)
			{
				loadNewInterfaces( ifile );
				_aseServer_cbx.setSelectedItem(currentSrvName);
			}
		}
		
		// --- ASE: TEXTFIELD: INTERFACES FILE ---
		if (_aseIfile_txt.equals(source))
		{
			loadNewInterfaces( _aseIfile_txt.getText() );
		}		

		// --- ASE: jConnect Options BUTTON: "..."  (open dialig to choose available options)
		if (_aseOptions_but.equals(source))
		{
			Map<String,String> sendOpt = StringUtil.parseCommaStrToMap(_aseOptions_txt.getText());
//			sendOpt.put("OPT1", "val 1");
//			sendOpt.put("OPT2", "val 2");
//			sendOpt.put("CHARSET", "wow... look at this");
			
			Map<String,String> outOpt = JdbcOptionsDialog.showDialog(this, AseConnectionFactory.getDriver(), AseConnectionFactory.getUrlTemplate(), sendOpt);
			// null == CANCEL
			if (outOpt != null)
				_aseOptions_txt.setText(StringUtil.toCommaStr(outOpt));
		}

		// --- ASE: TEXTFIELD: PASSWORD ---
		if (_asePasswd_txt.equals(source))
		{
			//saveProps();
			//setVisible(false);
			if (    _aseUser_txt  .getText().trim().equals("")
			     || _aseHost_txt  .getText().trim().equals("")
			     || _asePort_txt  .getText().trim().equals("")
			   )
			{
				setFocus();
			}
			else
			{
				_ok.doClick();
			}
		}

		// --- ASE: CHECKBOX: Connect On Startup ---
		if (_aseOptionConnOnStart_chk.equals(source))
		{
			// Save this option at once...
			// Then we can press "cancel"... and it will still be stored
			Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
			if (conf != null)
			{
				conf.setProperty(CONF_OPTION_CONNECT_ON_STARTUP, _aseOptionConnOnStart_chk.isSelected());
				conf.save();
			}
		}

		// --- ASE: CHECKBOX: STORE DATA ---
		if (_aseOptionStore_chk.equals(source))
		{
			toggleCounterStorageTab();
		}

		// --- ASE: CHECKBOX: HOSTMON monitoring ---
		if (_aseHostMonitor_chk.equals(source))
		{
			toggleHostmonTab();
		}

		// --- PCS: COMBOBOX: JDBC DRIVER ---
		if (_pcsJdbcDriver_cbx.equals(source))
		{
			String jdbcDriver = _pcsJdbcDriver_cbx.getEditor().getItem().toString();
			if ("org.h2.Driver".equals(jdbcDriver))
				_pcsH2Option_startH2NetworkServer_chk.setVisible(true);
			else
				_pcsH2Option_startH2NetworkServer_chk.setVisible(false);
		}

		// --- PCS: BUTTON: JDBC TEST CONNECTION ---
		if (_pcsTestConn_but.equals(source))
		{
			testJdbcConnection("testConnect", 
					_pcsJdbcDriver_cbx.getEditor().getItem().toString(), 
					_pcsJdbcUrl_cbx.getEditor().getItem().toString(),
					_pcsJdbcUsername_txt.getText(), 
					_pcsJdbcPassword_txt.getText());
		}

		// --- PCS: BUTTON: "Select All" 
		if (_pcsTabSelectAll_but.equals(source))
		{
			TableModel tm = _pcsSessionTable.getModel();
			for (int r=0; r<tm.getRowCount(); r++)
			{
				tm.setValueAt(new Boolean(true), r, PCS_TAB_POS_STORE_PCS);
			}
		}

		// --- PCS: BUTTON: "DeSelect All" 
		if (_pcsTabDeSelectAll_but.equals(source))
		{
			TableModel tm = _pcsSessionTable.getModel();
			for (int r=0; r<tm.getRowCount(); r++)
			{
				tm.setValueAt(new Boolean(false), r, PCS_TAB_POS_STORE_PCS);
				tm.setValueAt(new Integer(0),     r, PCS_TAB_POS_POSTPONE);
			}
		}

		// --- PCS: BUTTON: "Use Template" 
		if (_pcsTabTemplate_but.equals(source))
		{
			setToTemplate();
		}

		// --- PCS: COMBOBOX: JDBC URL ---
		if (_pcsJdbcUrl_cbx.equals(source))
		{
			checkForH2LocalDrive(null);
		}

		// --- PCS: BUTTON: "..." 
		if (_pcsJdbcUrl_but.equals(source))
		{
			String currentUrl = _pcsJdbcUrl_cbx.getEditor().getItem().toString();
			H2UrlHelper h2help = new H2UrlHelper(currentUrl);

			File baseDir = h2help.getDir(System.getProperty("ASETUNE_SAVE_DIR"));
			JFileChooser fc = new JFileChooser(baseDir);

			int returnVal = fc.showOpenDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION) 
			{
				String newFile = fc.getSelectedFile().getAbsolutePath().replace('\\', '/');
				String newUrl  = h2help.getNewUrl(newFile);

				_pcsJdbcUrl_cbx.getEditor().setItem(newUrl);
				checkForH2LocalDrive(null);
			}
		}
		
		// --- OFFLINE: COMBOBOX: JDBC DRIVER ---
		if (_offlineJdbcDriver_cbx.equals(source))
		{
			String jdbcDriver = _offlineJdbcDriver_cbx.getEditor().getItem().toString();
			if ("org.h2.Driver".equals(jdbcDriver))
				_offlineH2Option_startH2NwSrv_chk.setVisible(true);
			else
				_offlineH2Option_startH2NwSrv_chk.setVisible(false);
		}

		// --- OFFLINE: BUTTON: "..." 
		if (_offlineJdbcUrl_but.equals(source))
		{
//			"jdbc:h2:file:[<path>]<dbname>;IFEXISTS=TRUE";
//			"jdbc:h2:zip:<zipFileName>!/<dbname>";

			String url  = _offlineJdbcUrl_cbx.getEditor().getItem().toString();

			// Handle: jdbc:h2:file:  &&  "jdbc:h2:zip:"
			if (    url.startsWith("jdbc:h2:file:") 
			     || url.startsWith("jdbc:h2:tcp:") 
			     || url.startsWith("jdbc:h2:zip:")
			   )
			{
				H2UrlHelper h2help = new H2UrlHelper(url);

				File baseDir = h2help.getDir(System.getProperty("ASETUNE_SAVE_DIR"));
				JFileChooser fc = new JFileChooser(baseDir);

				int returnVal = fc.showOpenDialog(this);
				if(returnVal == JFileChooser.APPROVE_OPTION) 
				{
					String newFile = fc.getSelectedFile().getAbsolutePath().replace('\\', '/');
					String newUrl  = h2help.getNewUrl(newFile);

					_offlineJdbcUrl_cbx.getEditor().setItem(newUrl);
				}
			}
		}
		
		// --- OFFLINE: BUTTON: JDBC TEST CONNECTION ---
		if (_offlineJdbcPassword_txt.equals(source))
		{
			if (_offlineJdbcUsername_txt  .getText().trim().equals("") )
				setFocus();
			else
				_ok.doClick();
		}

		// --- OFFLINE: BUTTON: JDBC TEST CONNECTION ---
		if (_offlineTestConn_but.equals(source))
		{
			testJdbcConnection("testConnect", 
					_offlineJdbcDriver_cbx.getEditor().getItem().toString(), 
					_offlineJdbcUrl_cbx.getEditor().getItem().toString(),
					_offlineJdbcUsername_txt.getText(), 
					_offlineJdbcPassword_txt.getText());
		}

		// --- BUTTON: CANCEL ---
		if (_cancel.equals(source) || ACTION_CANCEL.equals(action) )
		{
			if ( _aseConn != null )
			{
				try { _aseConn.close(); }
				catch (SQLException ignore) {}
			}
//			if ( _pcsConn != null )
//			{
//				try { _pcsConn.close(); }
//				catch (SQLException ignore) {}
//			}
			if ( PersistentCounterHandler.hasInstance() )
			{
				 PersistentCounterHandler.getInstance().stop();
				 PersistentCounterHandler.setInstance(null);
			}
			if ( _offlineConn != null )
			{
				try { _offlineConn.close(); }
				catch (SQLException ignore) {}
			}

			// SET CONNECTION TYP and "CLOSE" the dialog
			_connectionType = CANCEL;
			setVisible(false);
		}

		// --- BUTTON: OK ---
		if (_ok.equals(source) || ACTION_OK.equals(action) )
		{
			// If we call the actionPerformed() from outside, we do not want to save stuff
			if (isVisible())
				saveProps();

			// OFFLINE CONNECT
			if (_tab.getSelectedIndex() == TAB_POS_OFFLINE)
			{
				// CONNECT to the PCS, if it fails, we stay in the dialog
				if ( _offlineConn == null )
				{
					if ( ! offlineConnect() )
						return;
				}

				// SET CONNECTION TYP and "CLOSE" the dialog
				_connectionType = OFFLINE_CONN;
				setVisible(false);
			}
			// ASE & PCS CONNECT
			else
			{
				// PCS CONNECT
				if (_aseOptionStore_chk.isSelected())
				{
					// CONNECT to the PCS, if it fails, we stay in the dialog

					// A new instance will be created each time we connect/hit_ok_but, 
					// thats ok because its done inside pcsConnect().
					if ( ! pcsConnect() )
						return;

					// setPersistCounters for all CM:s
					if (_useCmForPcsTable)
					{
						for(int r=0; r<_pcsSessionTable.getRowCount(); r++)
						{
							for (CountersModel cm : GetCounters.getCmList())
							{
								String  rowName      = (String)   _pcsSessionTable.getValueAt(r, PCS_TAB_POS_CM_NAME);
								Integer pcsPostpone  = (Integer)  _pcsSessionTable.getValueAt(r, PCS_TAB_POS_POSTPONE);
								boolean pcsStore     = ((Boolean) _pcsSessionTable.getValueAt(r, PCS_TAB_POS_STORE_PCS)) .booleanValue();
								boolean pcsStoreAbs  = ((Boolean) _pcsSessionTable.getValueAt(r, PCS_TAB_POS_STORE_ABS)) .booleanValue();
								boolean pcsStoreDiff = ((Boolean) _pcsSessionTable.getValueAt(r, PCS_TAB_POS_STORE_DIFF)).booleanValue();
								boolean pcsStoreRate = ((Boolean) _pcsSessionTable.getValueAt(r, PCS_TAB_POS_STORE_RATE)).booleanValue();
								String  cmName   = cm.getName();
								if (cmName.equals(rowName))
								{
									cm.setPersistCounters    (pcsStore,               true);
									cm.setPersistCountersAbs (pcsStoreAbs,            true);
									cm.setPersistCountersDiff(pcsStoreDiff,           true);
									cm.setPersistCountersRate(pcsStoreRate,           true);
									cm.setPostponeTime       (pcsPostpone.intValue(), true);
									continue;
								}
							}
						}
					}
					else
					{
						for(int r=0; r<_pcsSessionTable.getRowCount(); r++)
						{
							String  rowName      = (String)   _pcsSessionTable.getValueAt(r, PCS_TAB_POS_CM_NAME);
							Integer pcsPostpone  = (Integer)  _pcsSessionTable.getValueAt(r, PCS_TAB_POS_POSTPONE);
							boolean pcsStore     = ((Boolean) _pcsSessionTable.getValueAt(r, PCS_TAB_POS_STORE_PCS)) .booleanValue();
							boolean pcsStoreAbs  = ((Boolean) _pcsSessionTable.getValueAt(r, PCS_TAB_POS_STORE_ABS)) .booleanValue();
							boolean pcsStoreDiff = ((Boolean) _pcsSessionTable.getValueAt(r, PCS_TAB_POS_STORE_DIFF)).booleanValue();
							boolean pcsStoreRate = ((Boolean) _pcsSessionTable.getValueAt(r, PCS_TAB_POS_STORE_RATE)).booleanValue();
							
							System.out.println("OK: name="+StringUtil.left(rowName,25)+", store="+pcsStore+", abs="+pcsStoreAbs+", diff="+pcsStoreDiff+", rate="+pcsStoreRate+", postpone="+pcsPostpone+".");
						}
					}
				} // end: PCS CONNECT

				// OS HOST CONNECT
				if (_aseHostMonitor_chk.isSelected())
				{
					// Simply set the Connection Object used to connect
					// the ACTUAL SSH Connection will be done as a tak in aseConnect() + ConnectionProgressDialog... 
					_sshConn = hostmonCreateConnectionObject();

				} // end: OS HOST CONNECT

				if ( _aseConn == null )
				{
					// CONNECT to the ASE
					boolean ok = aseConnect();

					// HOST MONITOR: post fix
					if (_sshConn != null && ! _sshConn.isConnected() )
					{
						_sshConn.close();
						_sshConn = null;
					}
					
					// if it failed: stay in the dialog
					if ( ! ok  )
						return;
				}
//				if ( ! checkForMonitorOptions() )
//					return;

//				// If reconnected, check if it's the same version.
//				// NOTE: MonTablesDictionary is initialized at a later stage first time we connect
//				//       so if we dont have a instance, this would be the first time we connect.
//				//       THIS MIGHT CHANGE IN FUTURE.
//				if (MonTablesDictionary.hasInstance())
//				{
//					MonTablesDictionary mtd = MonTablesDictionary.getInstance();
//					int currentVersion = mtd.aseVersionNum;
//					int newVersion     = AseConnectionUtils.getAseVersionNumber(_aseConn);
//
//					if (currentVersion != newVersion)
//					{
//						String msg = "Connecting to a different ASE Version, This is NOT supported now... (previousVersion='"+AseConnectionUtils.versionIntToStr(currentVersion)+"', connectToVersion='"+AseConnectionUtils.versionIntToStr(newVersion)+"'). Please restart the application.";
//						if (AseTune.hasGUI())
//							JOptionPane.showMessageDialog(MainFrame.getInstance(), msg, Version.getAppName()+" - connect check", JOptionPane.WARNING_MESSAGE);
//						_logger.warn(msg);
//					}
//					else
//						_logger.info("Re-connected to the same ASE Version as priviously. Version is '"+currentVersion+"'.");
//				}

				// SET CONNECTION TYP and "CLOSE" the dialog
				_connectionType = ASE_CONN;
				setVisible(false);

			} // END: ASE & PCS CONNECT

		} // END: BUTTON: OK

		
		// --- ASE: URL Field ---
		if (_aseConnUrl_txt.equals(source))
		{
			try 
			{
				AseUrlHelper aseUrl = AseUrlHelper.parseUrl(_aseConnUrl_txt.getText());

				_aseHost_txt   .setText(aseUrl.getHosts());
				_asePort_txt   .setText(aseUrl.getPorts());
				_aseOptions_txt.setText(aseUrl.getOptions());

				String serverName = aseUrl.getServerName();
				if (serverName == null)
				{
					_aseServer_cbx.setSelectedIndex(0);

					String host        = _aseHost_txt.getText();
					String portStr     = _asePort_txt.getText();
					
					// Update the first entry in the combo box to be "host:port"
					// the host:port, will be what we have typed so far...
					// If the host_port can be found in the interfaces file, then
					// the combo box will display the server.
					_aseServer_cbx.updateFirstEntry(host, portStr);
				}
				else
					_aseServer_cbx.setSelectedItem(serverName);

			}
			catch (ParseException pe)
			{
			}
		}

		// ALWAYS: do stuff for URL
		setUrlText();

		validateContents();
	}
	
	// Typed characters in the fields are visible first when the key has been released: keyReleased()
	public void keyPressed (KeyEvent keyevent)
	{
	}

	// Discard all but digits for the _port_txt field
	public void keyTyped   (KeyEvent keyevent) 
	{
		if (keyevent.getSource().equals(_asePort_txt))
		{
			char ch = keyevent.getKeyChar();
			if ( ! (Character.isDigit(ch) || ch == ',' || ch == ' ') )
			{
				keyevent.consume();
				return;
			}
		}
	}

	// Update the server combo box
	public void keyReleased(KeyEvent keyevent) 
	{
		if (    keyevent.getSource().equals(_aseHost_txt) 
		     || keyevent.getSource().equals(_asePort_txt) )
		{
			String hosts = _aseHost_txt.getText();
			String ports = _asePort_txt.getText();
			
			// Update the first entry in the combo box to be "host:port"
			// the host:port, will be what we have typed so far...
			// If the host_port can be found in the interfaces file, then
			// the combo box will display the server.
			_aseServer_cbx.updateFirstEntry(hosts, ports);
	
//			if (_logger.isDebugEnabled())
//			{
//				_aseServerName_lbl.setText(host + ":" + portStr);
//			}
		}
		validateContents();
	}

	// TAB change
	public void stateChanged(ChangeEvent e)
	{
		// Set the oshostname if it's blank
		// use the same hostname os the ASE
		if (_aseHostMonitor_chk.isSelected())
		{
			String oshost = _hostmonHost_txt.getText().trim();
			if (oshost.equals(""))
			{
				String oshostname = "";
				String[] hosts = StringUtil.commaStrToArray(_aseHost_txt.getText());
				for (String host : hosts)
				{
					if (host.equals("localhost")) continue;
					if (host.equals("127.0.0.1")) continue;
					oshostname = host;
					break;
				}
				_hostmonHost_txt.setText(oshostname);
			}
		}
		setFocus();

		validateContents();		
	}
	/*---------------------------------------------------
	** END: implementing ActionListener, KeyListeners
	**---------------------------------------------------
	*/

	private void setToTemplate()
	{
		TableModel tm = _pcsSessionTable.getModel();
		for (int r=0; r<tm.getRowCount(); r++)
		{
			String        cmName = (String)  tm.getValueAt(r, PCS_TAB_POS_CM_NAME);
			CountersModel cm     = GetCounters.getCmByName(cmName);
			if (cm == null)
				continue;

			tm.setValueAt(new Integer(cm.getPostponeTime()),              r, PCS_TAB_POS_POSTPONE);
			tm.setValueAt(new Boolean(cm.isPersistCountersEnabled() || cm.isBackgroundDataPollingEnabled()), r, PCS_TAB_POS_STORE_PCS);
			tm.setValueAt(new Boolean(cm.isPersistCountersAbsEnabled()),  r, PCS_TAB_POS_STORE_ABS);
			tm.setValueAt(new Boolean(cm.isPersistCountersDiffEnabled()), r, PCS_TAB_POS_STORE_DIFF);
			tm.setValueAt(new Boolean(cm.isPersistCountersRateEnabled()), r, PCS_TAB_POS_STORE_RATE);
		}
	}

	private void setUrlText()
	{
		if (_aseConnUrl_chk.isSelected())
		{
			_aseConnUrl_txt .setEnabled(true);

			_aseServer_cbx  .setEnabled(false);
			_aseHost_txt    .setEnabled(false);
			_asePort_txt    .setEnabled(false);
			_aseOptions_txt .setEnabled(false);
			_aseOptions_but .setEnabled(false);
			_aseIfile_txt   .setEnabled(false);
			_aseIfile_but   .setEnabled(false);
			return;
		}
		_aseConnUrl_txt .setEnabled(false);

		_aseServer_cbx  .setEnabled(true);
		_aseHost_txt    .setEnabled(true);
		_asePort_txt    .setEnabled(true);
		_aseOptions_txt .setEnabled(true);
		_aseOptions_but .setEnabled(true);
		_aseIfile_txt   .setEnabled(true);
		_aseIfile_but   .setEnabled(true);

		String[] hosts = StringUtil.commaStrToArray(_aseHost_txt.getText());
		String[] ports = StringUtil.commaStrToArray(_asePort_txt.getText());
		if (hosts.length != ports.length)
		{
			return;
		}
		String hostPortStr = "";
		for (int i=0; i<hosts.length; i++)
			hostPortStr += hosts[i] + ":" + ports[i] + ",";
		if (hostPortStr.endsWith(","))  // remove last comma ','
			hostPortStr = hostPortStr.substring(0, hostPortStr.length()-1);
			

		Map<String,List<String>> hostPortMap = StringUtil.parseCommaStrToMultiMap(hostPortStr, ":", ",");
		Map<String,String>       optionsMap  = StringUtil.parseCommaStrToMap(_aseOptions_txt.getText());

		String url = AseUrlHelper.buildUrlString(hostPortMap, null, optionsMap);
		_aseConnUrl_txt.setText(url);		
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

		String hostPort = AseConnectionFactory.toHostPortStr(_aseHost_txt.getText(), _asePort_txt.getText());

		conf.setProperty("conn.interfaces", _aseIfile_txt.getText());
		conf.setProperty("conn.serverName", _aseServer_cbx.getSelectedItem().toString());

		conf.setProperty("conn.hostname",   _aseHost_txt.getText());
		conf.setProperty("conn.port",       _asePort_txt.getText());

		conf.setProperty("conn.username",           _aseUser_txt.getText());
		conf.setProperty("conn.username."+hostPort, _aseUser_txt.getText());
		if (_aseOptionSavePwd_chk.isSelected())
		{
			conf.setEncrypedProperty("conn.password",           _asePasswd_txt.getText());
			conf.setEncrypedProperty("conn.password."+hostPort, _asePasswd_txt.getText());
		}
		else
		{
			conf.remove("conn.password");
			conf.remove("conn.password."+hostPort);
		}

		conf.setProperty("conn.url.raw",                   _aseConnUrl_txt.getText() );
		conf.setProperty("conn.url.raw.checkbox",          _aseConnUrl_chk.isSelected() );
		conf.setProperty("conn.url.options",               _aseOptions_txt.getText() );

		conf.setProperty("conn.savePassword",              _aseOptionSavePwd_chk.isSelected() );
		conf.setProperty(CONF_OPTION_CONNECT_ON_STARTUP,   _aseOptionConnOnStart_chk.isSelected() );
		conf.setProperty(CONF_OPTION_RECONNECT_ON_FAILURE, _aseOptionReConnOnFailure_chk.isSelected());

		conf.setProperty("conn.persistCounterStorage", _aseOptionStore_chk.isSelected() );
		conf.setProperty("conn.hostMonitoring",        _aseHostMonitor_chk.isSelected() );

		//----------------------------------
		// TAB: OS Host
		//----------------------------------
		if ( _aseHostMonitor_chk.isSelected() )
		{
			conf.setProperty("ssh.conn.hostname."+hostPort,   _hostmonHost_txt.getText() );
			conf.setProperty("ssh.conn.port."+hostPort,       _hostmonPort_txt.getText() );
			conf.setProperty("ssh.conn.username."+hostPort,   _hostmonUser_txt.getText() );

			if (_hostmonOptionSavePwd_chk.isSelected())
				conf.setEncrypedProperty("ssh.conn.password."+hostPort, _hostmonPasswd_txt.getText());
			else
				conf.remove("ssh.conn.password."+hostPort);

			conf.setProperty("ssh.conn.savePassword", _hostmonOptionSavePwd_chk.isSelected() );
		}

		//----------------------------------
		// TAB: Counter Storage
		//----------------------------------
		if ( _aseOptionStore_chk.isSelected() )
		{
			conf.setProperty        ("pcs.write.writerClass", _pcsWriter_cbx    .getEditor().getItem().toString() );
			conf.setProperty        ("pcs.write.jdbcDriver",  _pcsJdbcDriver_cbx.getEditor().getItem().toString() );
			conf.setProperty        ("pcs.write.jdbcUrl",     _pcsJdbcUrl_cbx   .getEditor().getItem().toString() );
			conf.setProperty        ("pcs.write.jdbcUser",    _pcsJdbcUsername_txt.getText() );
			conf.setEncrypedProperty("pcs.write.jdbcPasswd",  _pcsJdbcPassword_txt.getText() );

			if (_pcsH2Option_startH2NetworkServer_chk.isVisible())
				conf.setProperty    ("pcs.write.h2.startH2NetworkServer", _pcsH2Option_startH2NetworkServer_chk.isSelected() );
			else
				conf.setProperty    ("pcs.write.h2.startH2NetworkServer", false );
			
			// The info in JTable is stored by the CM itself...
		}
		
		//----------------------------------
		// TAB: offline
		//----------------------------------
		if ( true )
		{
			conf.setProperty        ("pcs.read.jdbcDriver",          _offlineJdbcDriver_cbx.getEditor().getItem().toString() );
			conf.setProperty        ("pcs.read.jdbcUrl",             _offlineJdbcUrl_cbx   .getEditor().getItem().toString() );
			conf.setProperty        ("pcs.read.jdbcUser",            _offlineJdbcUsername_txt.getText() );
			conf.setEncrypedProperty("pcs.read.jdbcPasswd",          _offlineJdbcPassword_txt.getText() );
			conf.setProperty        ("pcs.read.checkForNewSessions", _offlineCheckForNewSessions_chk.isSelected());

			if (_offlineH2Option_startH2NwSrv_chk.isVisible())
				conf.setProperty    ("pcs.read.h2.startH2NetworkServer", _offlineH2Option_startH2NwSrv_chk.isSelected() );
			else
				conf.setProperty    ("pcs.read.h2.startH2NetworkServer", false );
			
		}

		//------------------
		// WINDOW
		//------------------
		conf.setProperty("conn.dialog.window.width",  this.getSize().width);
		conf.setProperty("conn.dialog.window.height", this.getSize().height);
		conf.setProperty("conn.dialog.window.pos.x",  this.getLocationOnScreen().x);
		conf.setProperty("conn.dialog.window.pos.y",  this.getLocationOnScreen().y);

		conf.save();
	}

	private void loadProps()
	{
//		Configuration conf = Configuration.getInstance(Configuration.TEMP);
		Configuration conf = Configuration.getCombinedConfiguration();
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
			_aseServer_cbx.setSelectedItem(str);


		str = conf.getProperty("conn.hostname");
		if (str != null)
			_aseHost_txt.setText(str);

		str = conf.getProperty("conn.port");
		if (str != null)
			_asePort_txt.setText(str);



		str = conf.getProperty("conn.url.options");
		if (str != null)
			_aseOptions_txt.setText(str);
		
		str = conf.getProperty("conn.url.raw");
		if (str != null)
			_aseConnUrl_txt.setText(str);

		bol = conf.getBooleanProperty("conn.url.raw.checkbox", false);
		_aseConnUrl_chk.setSelected(bol);

		if (_aseConnUrl_chk.isSelected())
		{
			try
			{
				AseUrlHelper aseUrl = AseUrlHelper.parseUrl(_aseConnUrl_txt.getText());

				_aseHost_txt   .setText(aseUrl.getHosts());
				_asePort_txt   .setText(aseUrl.getPorts());
				_aseOptions_txt.setText(aseUrl.getOptions());

				String serverName = aseUrl.getServerName();
				if (serverName == null)
				{
					_aseServer_cbx.setSelectedIndex(0);

					String host        = _aseHost_txt.getText();
					String portStr     = _asePort_txt.getText();
					
					// Update the first entry in the combo box to be "host:port"
					// the host:port, will be what we have typed so far...
					// If the host_port can be found in the interfaces file, then
					// the combo box will display the server.
					_aseServer_cbx.updateFirstEntry(host, portStr);
				}
				else
					_aseServer_cbx.setSelectedItem(serverName);
			}
			catch (ParseException pe)
			{
			}			
		}

		String hostPort = AseConnectionFactory.toHostPortStr(_aseHost_txt.getText(), _asePort_txt.getText());
		loadPropsForServer(hostPort);


		
		bol = conf.getBooleanProperty("conn.savePassword", true);
		_aseOptionSavePwd_chk.setSelected(bol);
		
		bol = conf.getBooleanProperty(CONF_OPTION_CONNECT_ON_STARTUP, false);
		_aseOptionConnOnStart_chk.setSelected(bol); 

		bol = conf.getBooleanProperty(CONF_OPTION_RECONNECT_ON_FAILURE, true);
		_aseOptionReConnOnFailure_chk.setSelected(bol); 

		bol = conf.getBooleanProperty("conn.persistCounterStorage", false);
		_aseOptionStore_chk.setSelected(bol);

		bol = conf.getBooleanProperty("conn.hostMonitoring", false);
		_aseHostMonitor_chk.setSelected(bol);

		//----------------------------------
		// TAB: OS Host
		//----------------------------------

		// NOTE: hostname, port, username, password is LOADED in loadPropsForServer();

		bol = conf.getBooleanProperty("ssh.conn.savePassword", true);
		_hostmonOptionSavePwd_chk.setSelected(bol);

		//----------------------------------
		// TAB: Counter Storage
		//----------------------------------
		str = conf.getProperty("pcs.write.writerClass");
		if (str != null)
			_pcsWriter_cbx.setSelectedItem(str);

		str = conf.getProperty("pcs.write.jdbcDriver");
		if (str != null)
			_pcsJdbcDriver_cbx.setSelectedItem(str);
		if ("org.h2.Driver".equals(str))
			_pcsH2Option_startH2NetworkServer_chk.setVisible(true);

		str = conf.getPropertyRaw("pcs.write.jdbcUrl");
		if (str != null)
			_pcsJdbcUrl_cbx.setSelectedItem(str);

		str = conf.getProperty("pcs.write.jdbcUser");
		if (str != null)
			_pcsJdbcUsername_txt.setText(str);

		str = conf.getProperty("pcs.write.jdbcPasswd");
		if (str != null)
			_pcsJdbcPassword_txt.setText(str);

		bol = conf.getBooleanProperty("pcs.write.h2.startH2NetworkServer", false);
		_pcsH2Option_startH2NetworkServer_chk.setSelected(bol);

		
		//----------------------------------
		// TAB: Offline
		//----------------------------------
		str = conf.getProperty("pcs.read.jdbcDriver");
		if (str != null)
			_offlineJdbcDriver_cbx.setSelectedItem(str);
		if ("org.h2.Driver".equals(str))
			_offlineH2Option_startH2NwSrv_chk.setVisible(true);

		str = conf.getProperty("pcs.read.jdbcUrl");
		if (str != null)
			_offlineJdbcUrl_cbx.setSelectedItem(str);

		str = conf.getProperty("pcs.read.jdbcUser");
		if (str != null)
			_offlineJdbcUsername_txt.setText(str);

		str = conf.getProperty("pcs.read.jdbcPasswd");
		if (str != null)
			_offlineJdbcPassword_txt.setText(str);

		bol = conf.getBooleanProperty("pcs.read.checkForNewSessions", true);
		_offlineCheckForNewSessions_chk.setSelected(bol);
		
		bol = conf.getBooleanProperty("pcs.read.h2.startH2NetworkServer", false);
		_offlineH2Option_startH2NwSrv_chk.setSelected(bol);
	}
	private void getSavedWindowProps()
	{
//		Configuration conf = Configuration.getInstance(Configuration.TEMP);
		Configuration conf = Configuration.getCombinedConfiguration();
		if (conf == null)
		{
			_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
			return;
		}
		//----------------------------------
		// TAB: Offline
		//----------------------------------
		int width  = conf.getIntProperty("conn.dialog.window.width",  510);
		int height = conf.getIntProperty("conn.dialog.window.height", 645);
		int x      = conf.getIntProperty("conn.dialog.window.pos.x",  -1);
		int y      = conf.getIntProperty("conn.dialog.window.pos.y",  -1);
		if (width != -1 && height != -1)
		{
			this.setSize(width, height);
		}
		if (x != -1 && y != -1)
		{
			this.setLocation(x, y);
		}
	}

	
//	private void loadPropsForServer(String host, int port)
//	{
//		String hostPortStr = host + ":" + port;
//		loadPropsForServer(hostPortStr);
//	}

	private void loadPropsForServer(String hostPortStr)
	{
//		Configuration conf = Configuration.getInstance(Configuration.TEMP);
		Configuration conf = Configuration.getCombinedConfiguration();
		if (conf == null)
		{
			_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
			return;
		}

		String str = null;

		//----------------------------------------
		// ASE stuff
		//----------------------------------------

		// First do "conn.username.hostName.portNum", if not found, go to "conn.username"
		str = conf.getProperty("conn.username."+hostPortStr);
		if (str != null)
		{
			_aseUser_txt.setText(str);
		}
		else
		{
			str = conf.getProperty("conn.username");
			if (str != null)
				_aseUser_txt.setText(str);
		}

		// First do "conn.password.hostName.portNum", if not found, go to "conn.password"
		str = conf.getProperty("conn.password."+hostPortStr);
		if (str != null)
		{
			_asePasswd_txt.setText(str);
		}
		else
		{
			str = conf.getProperty("conn.password");
			if (str != null)
				_asePasswd_txt.setText(str);
		}

		//----------------------------------------
		// OS HOST stuff
		//----------------------------------------

		// USERNAME
		str = conf.getProperty("ssh.conn.username."+hostPortStr, "");
		_hostmonUser_txt.setText(str);

		// PASSWORD
		str = conf.getProperty("ssh.conn.password."+hostPortStr, "");
		_hostmonPasswd_txt.setText(str);

		// HOSTNAME
		str = conf.getProperty("ssh.conn.hostname."+hostPortStr, "");
		_hostmonHost_txt.setText(str);

		// PORT
		str = conf.getProperty("ssh.conn.port."+hostPortStr, "22");
		_hostmonPort_txt.setText(str);
	}

	/*---------------------------------------------------
	** END: implementing saveProps & loadProps
	**---------------------------------------------------
	*/

	public Configuration getAseOptions()
	{
		Configuration conf = new Configuration();

		conf.setProperty(CONF_OPTION_CONNECT_ON_STARTUP,   _aseOptionConnOnStart_chk.isSelected());
		conf.setProperty(CONF_OPTION_RECONNECT_ON_FAILURE, _aseOptionReConnOnFailure_chk.isSelected());
		
		return conf;
	}
	
	/**
	 * Invoke a message if the storage driver is NOT local<br>
	 * With local I only check if it's 'C:' or not, I dont know how to do it in another way...
	 */
	private void checkForH2LocalDrive(String jdbcUrl)
	{
		// Do some extra checking if H2
		if (jdbcUrl == null)
			jdbcUrl = _pcsJdbcUrl_cbx.getEditor().getItem().toString();

		if ( jdbcUrl.startsWith("jdbc:h2:file:") )
		{
			if (PlatformUtils.getCurrentPlattform() == PlatformUtils.Platform_WIN)
			{
				H2UrlHelper urlHelper = new H2UrlHelper(jdbcUrl);
				String filename = urlHelper.getFilename();
				if ( ! StringUtil.isNullOrBlank(filename) )
				{
					filename = filename.toLowerCase();
					if (   filename.matches("[d-z]:.*") 
						|| filename.startsWith("\\\\") 
						|| filename.startsWith("//") 
						|| filename.startsWith("\\")
					   )
					{
						boolean networkDrive = true;
						String systemType    = "unknown";
						// try even harder to figgure out if it's a local drive or not
						try
						{
							String fnStart = filename.substring(0,3); // get first 3 chars
							File f = new File(fnStart);

							// Type description for a file, directory, or folder as it would be displayed 
							// in a system file browser. Example from Windows: the "Desktop" folder is desribed 
							// s "Desktop". Override for platforms with native ShellFolder implementations
							//
							// returns: the file type description as it would be displayed by a native 
							//          file chooser or null if no native information is available.
							systemType = FileSystemView.getFileSystemView().getSystemTypeDescription(f);

							if (systemType != null)
							{
								if ( systemType.toLowerCase().indexOf("local") > 1 )
									networkDrive = false;
							}
						}
						catch (Throwable t) {/*ignore*/}

						if (networkDrive)
						{
							String htmlStr = 
								"<html>" +
								"The selected storage file is '"+filename+"'.<br>" +
								"Is this <b>really</b> a local drive?<br>" +
								"<br>" +
								"Using a network drive will be <b>much</b> slower.<br>" +
								"The recomendation is to use a local drive as the database storage!<br>" +
								"<br>" +
								"The localized System Type Description from the Operating System, is '<b>"+systemType+"</b>'.<br>" +
								"If this string doesn't contain 'local', then it's considdered as a netork drive.<br>" +
								"<br>" +
								"Note: If the localized type '<b>"+systemType+"</b>' containes 'local' in you'r localization, please email me at goran_schwarz@hotmail.com the localized string.<br>" +
								"<br>" +
								"This is just a warning message, but be aware that the storage thread might not keep up...<br>" +
								"</html>";
							SwingUtils.showWarnMessage(this, "Local drive?", htmlStr, null);
						}
					} // end: not 'c:'
				} // end: filename containes something
			} // end: PlatformUtils.Platform_WIN
		} // end: jdbcUrl.startsWith("jdbc:h2:file:")
	}

	
	
	
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
			private Vector<String> _data;

			LocalSrvComboBoxModel(Vector<String> v)
			{
				super(v);
				_data = v;
			}
//			protected void set(int index, Object obj)
//			{
//				_data.set(index, obj);
//				fireContentsChanged(obj, index, index);
//			}
			protected void set(int index, String str)
			{
				_data.set(index, str);
				fireContentsChanged(str, index, index);
			}
		}
		
		protected LocalSrvComboBox()
		{
			super();
			_model = new LocalSrvComboBoxModel(new Vector<String>());
			setModel(_model);
		}

		/**
		 * rebuild the server list from the interfaces file.
		 */
		public void refresh()
		{
			removeAllItems();
			addItem(SERVER_FIRST_ENTRY);
	
			String[] servers = AseConnectionFactory.getIServerNames();
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
		public void updateFirstEntry(String hostStr, String portStr) 
		{
			String hostAndPort = AseConnectionFactory.toHostPortStr(hostStr, portStr);

			if (hostAndPort.equals(":"))
			{
				_model.set(0, SERVER_FIRST_ENTRY);
				setForeground(Color.BLACK);
				setSelectedIndex(0);
				return;
			}

//			int    port = -1;
//			try {  port = Integer.parseInt( portStr ); } catch (NumberFormatException ignore) {}
//
//			String server = null;
//			if (port > 0)
//				server = AseConnectionFactory.getServerName(hostAndPort);

			String server = AseConnectionFactory.getIServerName(hostAndPort);

			if (server == null || (server != null && server.trim().equals("")) )
			{
				if (_logger.isTraceEnabled())
					_logger.trace("hostPort='"+hostAndPort+"' was NOT FOUND.");
				_model.set(0, hostAndPort);
				setSelectedIndex(0);
				setForeground(Color.BLUE);
			}
			else
			{
				if (_logger.isTraceEnabled())
					_logger.trace("Found='"+server+"' for hostPort='"+hostAndPort+"'.");

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
		final String CONFIG_FILE_NAME     = System.getProperty("CONFIG_FILE_NAME",     "asetune.properties");
		final String TMP_CONFIG_FILE_NAME = System.getProperty("TMP_CONFIG_FILE_NAME", "asetune.save.properties");
		final String ASETUNE_HOME         = System.getProperty("ASETUNE_HOME");
		
		String defaultPropsFile    = (ASETUNE_HOME          != null) ? ASETUNE_HOME          + File.separator + CONFIG_FILE_NAME     : CONFIG_FILE_NAME;
		String defaultTmpPropsFile = (Version.APP_STORE_DIR != null) ? Version.APP_STORE_DIR + File.separator + TMP_CONFIG_FILE_NAME : TMP_CONFIG_FILE_NAME;
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

		Configuration conf1 = new Configuration(defaultTmpPropsFile);
		Configuration.setInstance(Configuration.USER_TEMP, conf1);

		Configuration conf2 = new Configuration(defaultPropsFile);
		Configuration.setInstance(Configuration.SYSTEM_CONF, conf2);

		System.out.println("showAseOnlyConnectionDialog ...");
		Connection conn = ConnectionDialog.showAseOnlyConnectionDialog(null);
		System.out.println("showAseOnlyConnectionDialog, returned: conn="+conn);

		// DO THE THING
		ConnectionDialog connDialog = new ConnectionDialog(null, false, true, true, false, false, false);
		connDialog.setVisible(true);
		connDialog.dispose();

		int connType = connDialog.getConnectionType();
		
		if ( connType == ConnectionDialog.CANCEL)
		{
			System.out.println("---CANCEL...");
		}

		if ( connType == ConnectionDialog.ASE_CONN)
		{
			System.out.println("---ASE connection...");
			Connection               aseConn   = connDialog.getAseConn();
			PersistentCounterHandler pcsWriter = PersistentCounterHandler.getInstance();
			
			if (aseConn != null)
				System.out.println("- has ASE connection");
			else
				System.out.println("- no  ASE connection");

			if (pcsWriter != null)
				System.out.println("- has PCS Handler");
			else
				System.out.println("- no  PCS Handler");

			Configuration options = connDialog.getAseOptions();
			System.out.println("- OPTIONS: "+options);
			
		}

		if ( connType == ConnectionDialog.OFFLINE_CONN)
		{
			System.out.println("---OFFLINE connection...");
			Connection offlineConn = connDialog.getOfflineConn();

			if (offlineConn != null)
				System.out.println("- has OFFLINE connection");
			else
				System.out.println("- no  OFFLINE connection");
		}
	}
}

