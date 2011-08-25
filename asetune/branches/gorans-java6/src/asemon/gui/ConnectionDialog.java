/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.gui;

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
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

import asemon.Asemon;
import asemon.GetCounters;
import asemon.MonTablesDictionary;
import asemon.Version;
import asemon.cm.CountersModel;
import asemon.gui.swing.MultiLineLabel;
import asemon.pcs.PersistentCounterHandler;
import asemon.utils.AseConnectionFactory;
import asemon.utils.AseConnectionUtils;
import asemon.utils.AseUrlHelper;
import asemon.utils.Configuration;
import asemon.utils.PlatformUtils;
import asemon.utils.StringUtil;
import asemon.utils.SwingUtils;

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
//	private Connection               _pcsConn         = null;
	private Connection               _offlineConn     = null;
//	private PersistentCounterHandler _pcsWriter       = null;


	private static final int   TAB_POS_ASE      = 0;
	private static final int   TAB_POS_PCS      = 1;
	private static final int   TAB_POS_OFFLINE  = 2;

	//-------------------------------------------------
	// Actions
	public static final String ACTION_OK        = "OK";
	public static final String ACTION_CANCEL    = "CANCEL";

	@SuppressWarnings("unused")
	private Frame              _owner           = null;
	
	private JTabbedPane        _tab;
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

	private JLabel             _options_lbl     = new JLabel("URL Options");
	private JTextField         _options_txt     = new JTextField();
	private JButton            _options_but     = new JButton("...");

	private JLabel             _ifile_lbl       = new JLabel("Name service");
	private JTextField         _ifile_txt       = new JTextField(AseConnectionFactory.getIFileName());
	private String             _ifile_save      = AseConnectionFactory.getIFileName();
	private JButton            _ifile_but       = new JButton("...");

	private JCheckBox          _monUrl_chk      = new JCheckBox("Use URL", false);
	private JTextField         _monUrl_txt      = new JTextField();


	private JCheckBox          _optionSavePwd_chk         = new JCheckBox("Save password", true);
	private JCheckBox          _optionConnOnStart_chk     = new JCheckBox("Connect to this server on startup", false);
	private JCheckBox          _optionReConnOnFailure_chk = new JCheckBox("Reconnect to server if connection is lost", true);
//	private JCheckBox          _optionUsedForNoGui_chk    = new JCheckBox("Use connection info above for no-gui mode", false);
	private JCheckBox          _optionStore_chk           = new JCheckBox("Save counter data in a Persistent Counter Storage...", false);

	@SuppressWarnings("unused")
	private JPanel             _pcsPanel            = null;
	private ImageIcon          _pcsImageIcon        = SwingUtils.readImageIcon(Version.class, "./images/pcs_write_32.png");
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

	@SuppressWarnings("unused")
	private JPanel             _offlinePanel            = null;
	private ImageIcon          _offlineImageIcon        = SwingUtils.readImageIcon(Version.class, "./images/pcs_read_32.png");
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
	private JCheckBox          _offlineCheckForNewSessions_chk = new JCheckBox("Check PCS database for new sample sessions", false);
	// Specific options if we are using H2 as PCS
	private JCheckBox          _offlineH2Option_startH2NwSrv_chk = new JCheckBox("Start H2 Database as a Network Server", false);

	private JLabel             _ok_lbl         = new JLabel(""); // Problem description if _ok is disabled
	private JButton            _ok             = new JButton("OK");
	private JButton            _cancel         = new JButton("Cancel");

	private boolean            _checkAseCfg    = true;
	private boolean            _showAseTab     = true;
	private boolean            _showPcsTab     = true;
	private boolean            _showOfflineTab = true;

//	private static ConnectionDialog   _instance       = null;
//	public static ConnectionDialog getInstance()
//	{
//		return _instance;
//	}

	public ConnectionDialog(Frame owner)
	{
		this(owner, true, true, true, true);
	}
	public ConnectionDialog(Frame owner, boolean checkAseCfg, boolean showAseTab, boolean showPcsTab, boolean showOfflineTab)
	{
		super(owner, "Connect", true);
//		_instance = this;
		_owner = owner;

		if (owner == null)
			_useCmForPcsTable = false;

		_checkAseCfg    = checkAseCfg;
		_showAseTab     = showAseTab;
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

		setLocationRelativeTo(owner);

		getSavedWindowProps();

		setFocus();
	}

	public int                      getConnectionType() { return _connectionType; }
	public Connection               getAseConn()        { return _aseConn; }
//	public Connection               getPcsConn()        { return _pcsConn; }
//	public PersistentCounterHandler getPcsWriter()      { return _pcsWriter; }
	public Connection               getOfflineConn()    { return _offlineConn; }

	public void setAseUsername(String username) { _user_txt.setText(username); }
	public void setAsePassword(String passwd)   { _passwd_txt.setText(passwd); }
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

			_host_txt.setText(host);
			_port_txt.setText(port);
		}
		else
		{
			String str = AseConnectionFactory.resolvInterfaceEntry(server);
			if (str == null)
				throw new Exception("ASE Server name '"+server+"' was not part of the known ASE server's.");

			_server_cbx.setSelectedItem(server);
		}
	}
	
	public String getAseUsername() { return _user_txt.getText(); }
	public String getAsePassword() { return _passwd_txt.getText(); }
	public String getAseServer  () { return (String) _server_cbx.getSelectedItem(); }
	
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
		String pcsTabTip     = "In GUI mode save Data Counter to a Storage, which can be view later by the 'offline' mode.";
		String offlineTabTip = "<html>" +
		                       "Connect to a 'offline' Counter Storage where AseMon has stored counter data.<br>" +
		                       "AseMon will switch to 'offline' mode and just reads data from the Counter Storage.<br>" +
		                       "</html>";

		_tab = new JTabbedPane();
		_tab.addTab("ASE",             null, createTabAse(),     aseTabTip);
		_tab.addTab("Counter Storage", null, createTabPcs(),     pcsTabTip);
		_tab.addTab("Offline Connect", null, createTabOffline(), offlineTabTip);
		
		if (! _showAseTab)     _tab.setEnabledAt(TAB_POS_ASE,     false);
		if (! _showPcsTab)     _tab.setEnabledAt(TAB_POS_PCS,     false);
		if (! _showOfflineTab) _tab.setEnabledAt(TAB_POS_OFFLINE, false);


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
		
		panel.add(createUserPasswdPanel(),  "grow");
		panel.add(createServerPanel(),      "grow");
		if (Configuration.hasInstance(Configuration.TEMP))
			panel.add(createOptionsPanel(),     "grow");
//		panel.add(createPcsPanel(),         "grow, hidemode 3");
		
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
	
	private JPanel createUserPasswdPanel()
	{
		JPanel panel = SwingUtils.createPanel("User information", true);
		panel.setLayout(new MigLayout("wrap 2","",""));   // insets Top Left Bottom Right

		// Hide password or not...
		if (_logger.isDebugEnabled())
			_passwd_txt = new JTextField();
		else
			_passwd_txt = new JPasswordField();

		_user_lbl  .setToolTipText("User name to use when logging in to the below ASE Server.");
		_user_txt  .setToolTipText("User name to use when logging in to the below ASE Server.");
		_passwd_lbl.setToolTipText("Password to use when logging in to the below ASE Server");
		_passwd_txt.setToolTipText("Password to use when logging in to the below ASE Server");
		
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

		String urlExample="URL Example: jdbc:sybase:Tds:host1:port1[,server2:port2,...,serverN:portN][/mydb][?&OPT1=1024&OPT2=true&OPT3=some str]";

		_ifile_lbl  .setToolTipText("Directory Service file (sql.ini or interfaces) to use for resolving ASE name into hostname and port number");
		_ifile_txt  .setToolTipText("Directory Service file (sql.ini or interfaces) to use for resolving ASE name into hostname and port number");
		_ifile_but  .setToolTipText("Open a File Dialog to locate a Directory Service file.");
		_server_lbl .setToolTipText("Name of the ASE you are connecting to");
		_server_cbx .setToolTipText("Name of the ASE you are connecting to");
		_host_lbl   .setToolTipText("<html>Hostname or IP address of the ASE you are connecting to<br>Syntax: host1[,host2,...]</html>");
		_host_txt   .setToolTipText("<html>Hostname or IP address of the ASE you are connecting to<br>Syntax: host1[,host2,...]</html>");
		_port_lbl   .setToolTipText("<html>Port number of the ASE you are connecting to<br>Syntax: port1[,port2,...]</html>");
		_port_txt   .setToolTipText("<html>Port number of the ASE you are connecting to<br>Syntax: port1[,port2,...]</html>");
		_options_lbl.setToolTipText("<html>JConnect Options that can be set.<br>Syntax: OPT1=value[, OPT2=value]<br>Or press the '...' button to add enties.</html>");
		_options_txt.setToolTipText("<html>JConnect Options that can be set.<br>Syntax: OPT1=value[, OPT2=value]<br>Or press the '...' button to add enties.</html>");
		_options_but.setToolTipText("Open a Dialog where available options are presented.");
		_monUrl_chk .setToolTipText("<html>Actual URL used to connect the monitored server.<br>"+urlExample+"</html>");
		_monUrl_txt .setToolTipText("<html>Actual URL used to connect the monitored server.<br>"+urlExample+"</html>");

		panel.add(_serverIcon,  "");
		panel.add(_serverHelp,  "wmin 100, push, grow");

//		_ifile_txt.setEditable(false);
		panel.add(_ifile_lbl,     "");
		panel.add(_ifile_txt,     "push, grow, split");
		panel.add(_ifile_but,     "wrap");

		panel.add(_server_lbl,   "");
		panel.add(_server_cbx,   "push, grow");

		panel.add(_host_lbl,     "");
		panel.add(_host_txt,     "push, grow");

		panel.add(_port_lbl,     "");
		panel.add(_port_txt,     "push, grow");

		panel.add(_options_lbl,  "");
		panel.add(_options_txt,  "push, grow, split");
		panel.add(_options_but,  "wrap");

		panel.add(_monUrl_chk,     "");
		panel.add(_monUrl_txt,     "push, grow");
		_monUrl_txt.setEnabled(_monUrl_chk.isEnabled());

		_serverName_lbl.setText(":");
		if (_logger.isDebugEnabled())
		{
			panel.add(_serverName_lbl, "skip, push, grow");
		}

		// set auto completion
		AutoCompleteDecorator.decorate(_server_cbx);

		// ADD ACTION LISTENERS
		_server_cbx .addActionListener(this);
		_options_txt.addActionListener(this);
		_options_but.addActionListener(this);
		_ifile_but  .addActionListener(this);
		_ifile_txt  .addActionListener(this);
		_monUrl_chk .addActionListener(this);
		_monUrl_txt .addActionListener(this);

		// If write in host/port, create the combined host:port and show that...
		_host_txt.addKeyListener(this);
		_port_txt.addKeyListener(this);

		return panel;
	}

	private JPanel createOptionsPanel()
	{
		JPanel panel = SwingUtils.createPanel("Options", true);
		panel.setLayout(new MigLayout("wrap 1, gap 0","",""));   // insets Top Left Bottom Right

		_optionSavePwd_chk        .setToolTipText("Save the password in the configuration file, and yes it's encrypted");
		_optionConnOnStart_chk    .setToolTipText("When AseMon starts use the above ASE and connect automatically (if the below 'Persisten Counter Storage' is enabled, it will also be used at startup)");
		_optionReConnOnFailure_chk.setToolTipText("If connection to the monitored server is lost in some way, try to reconnect to the server again automatically.");
		_optionStore_chk          .setToolTipText("Store GUI Counter Data in a database (Persistent Counter Storage), which can be viewed later, connect to it from the 'offline' tab");

		panel.add(_optionSavePwd_chk,         "");
		panel.add(_optionConnOnStart_chk,     "");
		panel.add(_optionReConnOnFailure_chk, "");
//		panel.add(_optionUsedForNoGui_chk,    "");
		if (_showPcsTab)
			panel.add(_optionStore_chk,       "");

		_optionStore_chk.addActionListener(this);

		return panel;
	}

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
		_pcsJdbcUrl_lbl     .setToolTipText("URL for the above JDBC drivername to connect to a datastore, a couple of template URL for H2 and Sybase JDBC driver");
		_pcsJdbcUrl_cbx     .setToolTipText("URL for the above JDBC drivername to connect to a datastore, a couple of template URL for H2 and Sybase JDBC driver");
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
		
		_pcsWriter_cbx    .addItem("asemon.pcs.PersistWriterJdbc");

		_pcsJdbcDriver_cbx.addItem("org.h2.Driver");
		_pcsJdbcDriver_cbx.addItem(AseConnectionFactory.getDriver());

		// http://www.h2database.com/html/features.html#database_url
		_pcsJdbcUrl_cbx   .addItem("jdbc:h2:file:[<path>]<dbname>");
		_pcsJdbcUrl_cbx   .addItem("jdbc:h2:tcp://<host>[:<port>]/<dbname>");
		_pcsJdbcUrl_cbx   .addItem("jdbc:h2:ssl://<host>[:<port>]/<dbname>");

		// http://infocenter.sybase.com/help/topic/com.sybase.dc39001_0605/html/prjdbc/X39384.htm
		_pcsJdbcUrl_cbx   .addItem("jdbc:sybase:Tds:<host>:<port>");
		_pcsJdbcUrl_cbx   .addItem("jdbc:sybase:Tds:<host>:<port>[/<dbname>]");
//		_pcsJdbcUrl_cbx   .addItem("jdbc:sybase:Tds:<host>:<port>[/<dbname>][?OPT1=val,&OPT2=val]");

		
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
		_offlineCheckForNewSessions_chk.setToolTipText("<html>If asemon still collects data into the offline database, go and check for new samples.<br>Using this option we can run asemon-nogui in offline mode while the GUI can read from the shared database.</html>");
		_offlineH2Option_startH2NwSrv_chk.setToolTipText("<html>Start the H2 database engine in 'server' mode.<br>This enables other asemon client to be connected to the same database.<br>Or you can use any third party product database viewer to read the content at the same time.</html>");

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
		_offlineJdbcDriver_cbx.addActionListener(this);
		_offlineTestConn_but.addActionListener(this);
		_offlineJdbcUrl_cbx.getEditor().getEditorComponent().addKeyListener(this);
		_offlineJdbcUrl_cbx.addActionListener(this);
		_offlineJdbcUrl_but.addActionListener(this);

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
			row.set(PCS_TAB_POS_ICON,       SwingUtils.readImageIcon(Version.class, "./images/cm_summary_activity.png"));
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
			row.set(PCS_TAB_POS_ICON,       SwingUtils.readImageIcon(Version.class, "./images/cm_engine_activity.png"));
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
			row.set(PCS_TAB_POS_ICON,       SwingUtils.readImageIcon(Version.class, "./images/cm_device_activity.png"));
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
				row.set(PCS_TAB_POS_ICON,       SwingUtils.readImageIcon(Version.class, "./images/ud_counter_activity.png"));
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
			if (_optionStore_chk.isSelected())
			{
				_tab.setEnabledAt(TAB_POS_PCS, true);
			}
			else
			{
				_tab.setEnabledAt(TAB_POS_PCS, false);
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

		else if (_optionStore_chk.isSelected())
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
		else
		{
			// host/port fields has to match
			String[] hosts = StringUtil.commaStrToArray(_host_txt.getText());
			String[] ports = StringUtil.commaStrToArray(_port_txt.getText());
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
		_server_cbx .refresh();
		_host_txt   .setText("");
		_port_txt   .setText("");
		_options_txt.setText("");
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

	public static boolean checkReconnectVersion(Connection conn)
	{
		// If reconnected, check if it's the same version.
		// NOTE: MonTablesDictionary is initialized at a later stage first time we connect
		//       so if we dont have a instance, this would be the first time we connect.
		//       THIS MIGHT CHANGE IN FUTURE.
		if (MonTablesDictionary.hasInstance())
		{
			MonTablesDictionary mtd = MonTablesDictionary.getInstance();
			int currentVersion = mtd.aseVersionNum;
			int newVersion     = AseConnectionUtils.getAseVersionNumber(conn);
	
			if (currentVersion != newVersion)
			{
				String msg = "<html>" +
						"Connecting to a different ASE Version, This is NOT supported now... (previousVersion='"+AseConnectionUtils.versionIntToStr(currentVersion)+"', connectToVersion='"+AseConnectionUtils.versionIntToStr(newVersion)+"'). <br>" +
						"To connect to another ASE Version, you need to restart the application." +
						"</html>";
				if (Asemon.hasGUI())
					JOptionPane.showMessageDialog(MainFrame.getInstance(), msg, "asemon - connect check", JOptionPane.WARNING_MESSAGE);
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
		if (_monUrl_chk.isSelected())
		{
//			AseConnectionFactory.setAppName ( Version.getAppName() );
//			AseConnectionFactory.setUser    ( _user_txt.getText() );
//			AseConnectionFactory.setPassword( _passwd_txt.getText() );

			String username = _user_txt.getText();
			String password = _passwd_txt.getText();
			String rawUrl   = _monUrl_txt.getText();

			Properties props = new Properties();
			props.put("user",     username);
			props.put("password", password);
			try
			{
				_logger.info("Connecting to ASE using RAW-URL username='"+username+"', URL='"+rawUrl+"'.");
				_aseConn = ConnectionProgressDialog.connectWithProgressDialog(this, AseConnectionFactory.getDriver(), rawUrl, props, _checkAseCfg);
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
		String portStr = _port_txt.getText();
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
				+"', user='"+_user_txt.getText()+"', password='"+_passwd_txt.getText()
				+"', host='"+_host_txt.getText()+"', port='"+_port_txt.getText()+"'.");
		
		AseConnectionFactory.setAppName ( Version.getAppName() );
		AseConnectionFactory.setHostName( Version.getVersionStr() );
		AseConnectionFactory.setUser    ( _user_txt.getText() );
		AseConnectionFactory.setPassword( _passwd_txt.getText() );
//		AseConnectionFactory.setHost    ( _host_txt.getText() );
//		AseConnectionFactory.setPort    ( port );
		AseConnectionFactory.setHostPort(_host_txt.getText(), _port_txt.getText());

		// set options... if there are any
		if ( _options_txt.getText().trim().equals("") )
		{
			Map<String,String> options = null;
			AseConnectionFactory.setProperties(options);
		}
		else
		{
			Map<String,String> options = StringUtil.parseCommaStrToMap(_options_txt.getText());
			AseConnectionFactory.setProperties(options);
		}
		
		try
		{
			_logger.info("Connecting to ASE '"+AseConnectionFactory.getServer()+"'.  hostPortStr='"+AseConnectionFactory.getHostPortStr()+"', user='"+AseConnectionFactory.getUser()+"'.");

			String urlStr = "jdbc:sybase:Tds:" + AseConnectionFactory.getHostPortStr();
			_aseConn = ConnectionProgressDialog.connectWithProgressDialog(this, urlStr, _checkAseCfg);
//			_aseConn = AseConnectionFactory.getConnection();

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
	
	@SuppressWarnings("unused")
	private boolean checkForMonitorOptions()
	{
//System.out.println(">>>>> checkForMonitorOptions");
		MainFrame.setStatus(MainFrame.ST_STATUS_FIELD, "Checking for Monitor Options...");

		boolean b = AseConnectionUtils.checkForMonitorOptions(_aseConn, _user_txt.getText(), true, this);

		MainFrame.setStatus(MainFrame.ST_CONNECT);
//System.out.println("<<<<< checkForMonitorOptions");
		return b;
	}

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
			Configuration pcsProps = new Configuration();

			String pcsAll = _pcsWriter_cbx.getEditor().getItem().toString();
			pcsProps.put("PersistentCounterHandler.WriterClass", pcsAll);

			// pcsAll "could" be a ',' separated string
			// But I dont know how to set the properties for those Writers
			String[] pcsa = pcsAll.split(",");
			for (int i=0; i<pcsa.length; i++)
			{
				String pcs = pcsa[i].trim();
				
				if (pcs.equals("asemon.pcs.PersistWriterJdbc"))
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
			JOptionPane.showMessageDialog(this, "Connection FAILED.\n\n"+sb.toString(), "asemon - jdbc connect", JOptionPane.ERROR_MESSAGE);
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this, "Connection FAILED.\n\n"+e.toString(),  "asemon - jdbc connect", JOptionPane.ERROR_MESSAGE);
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
	
			JOptionPane.showMessageDialog(this, "Connection succeeded.", "asemon - connect check", JOptionPane.INFORMATION_MESSAGE);
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
			JOptionPane.showMessageDialog(this, "Connection FAILED.\n\n"+sb.toString(), "asemon - connect check", JOptionPane.ERROR_MESSAGE);
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this, "Connection FAILED.\n\n"+e.toString(),  "asemon - connect check", JOptionPane.ERROR_MESSAGE);
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
		if (_server_cbx.equals(source))
		{
			_logger.debug("_server_cbx.actionPerformed(): getSelectedIndex()='"+_server_cbx.getSelectedIndex()+"', getSelectedItem()='"+_server_cbx.getSelectedItem()+"'.");
	
			// NOTE: index 0 is "host:port" or SERVER_FIRST_ENTRY("-CHOOSE A SERVER-")
			//       so we wont touch host_txt and port_txt if we are on index 0
			if ( _server_cbx.getSelectedIndex() > 0 )
			{
				String server = (String) _server_cbx.getSelectedItem();
				
				String hosts       = AseConnectionFactory.getIHosts(server, ", ");
				String ports       = AseConnectionFactory.getIPorts(server, ", ");
				String hostPortStr = AseConnectionFactory.getIHostPortStr(server);
				_host_txt.setText(hosts);
				_port_txt.setText(ports);

				// Try to load user name & password for this server
				loadPropsForServer(hostPortStr);
			}
		}

		// --- ASE: BUTTON: "..." Open file to get interfaces/sql.ini file ---
		if (_ifile_but.equals(source))
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
				_ifile_txt.setText( fc.getSelectedFile().getAbsolutePath() );
				loadNewInterfaces( _ifile_txt.getText() );
			}
		}
		
		// --- ASE: TEXTFIELD: INTERFACES FILE ---
		if (_ifile_txt.equals(source))
		{
			loadNewInterfaces( _ifile_txt.getText() );
		}		

		// --- ASE: jConnect Options BUTTON: "..."  (open dialig to choose available options)
		if (_options_but.equals(source))
		{
			Map<String,String> sendOpt = StringUtil.parseCommaStrToMap(_options_txt.getText());
//			sendOpt.put("OPT1", "val 1");
//			sendOpt.put("OPT2", "val 2");
//			sendOpt.put("CHARSET", "wow... look at this");
			
			Map<String,String> outOpt = JdbcOptionsDialog.showDialog(this, AseConnectionFactory.getDriver(), AseConnectionFactory.getUrlTemplate(), sendOpt);
			// null == CANCEL
			if (outOpt != null)
				_options_txt.setText(StringUtil.toCommaStr(outOpt));
		}

		// --- ASE: TEXTFIELD: PASSWORD ---
		if (_passwd_txt.equals(source))
		{
			//saveProps();
			//setVisible(false);
			if (    _user_txt  .getText().trim().equals("")
			     || _host_txt  .getText().trim().equals("")
			     || _port_txt  .getText().trim().equals("")
			   )
			{
				setFocus();
			}
			else
			{
				_ok.doClick();
			}
		}

		// --- ASE: CHECKBOX: STORE DATA ---
		if (_optionStore_chk.equals(source))
		{
			toggleCounterStorageTab();
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

		// --- PCS: BUTTON: "..." 
		if (_pcsJdbcUrl_but.equals(source))
		{
			JFileChooser fc = new JFileChooser();
			if (System.getProperty("ASEMON_SAVE_DIR") != null)
				fc.setCurrentDirectory(new File(System.getProperty("ASEMON_SAVE_DIR")));
			int returnVal = fc.showOpenDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION) 
			{
				String url  = _pcsJdbcUrl_cbx.getEditor().getItem().toString();
				String path = fc.getSelectedFile().getAbsolutePath().replace('\\', '/');

				// Take away db suffix. ".h2.db"
				if (path.matches(".*\\.h2\\.db.*"))
					path = path.replaceAll("\\.h2\\.db", "");

				// Take away db suffix. ".data.db"
				if (path.matches(".*\\.data\\.db.*"))
					path = path.replaceAll("\\.data\\.db", "");

				// Take away index suffix. ".index.db"
				if (path.matches(".*\\.index\\.db.*"))
					path = path.replaceAll("\\.index\\.db", "");

				// Take away log suffix. ".99.log.db"
				if (path.matches(".*\\.[0-9]*\\.log\\.db.*"))
					path = path.replaceAll("\\.[0-9]*\\.log\\.db", "");

				// fill in the template
				if ( url.matches(".*\\[<path>\\]<dbname>.*") )
					url = url.replaceFirst("\\[<path>\\]<dbname>", path);
				else
					url += path;

				_pcsJdbcUrl_cbx.getEditor().setItem(url);
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
				JFileChooser fc = new JFileChooser();
				if (System.getProperty("ASEMON_SAVE_DIR") != null)
					fc.setCurrentDirectory(new File(System.getProperty("ASEMON_SAVE_DIR")));
//				if (url.startsWith("jdbc:h2:file:")) fc.setFileFilter();
//				if (url.startsWith("jdbc:h2:zip:"))  fc.setFileFilter();

				int returnVal = fc.showOpenDialog(this);
				if(returnVal == JFileChooser.APPROVE_OPTION) 
				{
					String path = fc.getSelectedFile().getAbsolutePath().replace('\\', '/');

					if (    url.startsWith("jdbc:h2:file:")
					     || url.startsWith("jdbc:h2:tcp:")
					   )
					{
						// Take away db suffix. ".h2.db"
						if (path.matches(".*\\.h2\\.db.*"))
							path = path.replaceAll("\\.h2\\.db", "");

						// Take away db suffix. ".data.db"
						if (path.matches(".*\\.data\\.db.*"))
							path = path.replaceAll("\\.data\\.db", "");

						// Take away index suffix. ".index.db"
						if (path.matches(".*\\.index\\.db.*"))
							path = path.replaceAll("\\.index\\.db", "");

						// Take away log suffix. ".99.log.db"
						if (path.matches(".*\\.[0-9]*\\.log\\.db.*"))
							path = path.replaceAll("\\.[0-9]*\\.log\\.db", "");

						// fill in the template
						if ( url.matches(".*\\[<path>\\]<dbname>.*") )
							url = url.replaceFirst("\\[<path>\\]<dbname>", path);
						else
							url += path;
					}

					if (url.startsWith("jdbc:h2:zip:"))
					{
						File f = new File(path);
						String dbname = f.getName();
						if (dbname.toLowerCase().endsWith(".zip"))
							dbname = dbname.substring(0, dbname.length()-".zip".length());
						else
							dbname = "offlineDb";

						// fill in the template
						if ( url.matches(".*<zipFileName>!/<dbname>.*") )
						{
							url = url.replaceFirst("<zipFileName>", path);
							url = url.replaceFirst("<dbname>", dbname);
						}
						else
							url += path;
					}
	
					_offlineJdbcUrl_cbx.getEditor().setItem(url);
				}
			}
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
				if (_optionStore_chk.isSelected())
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
//							Iterator iter = GetCounters.getCmList().iterator();
//							while (iter.hasNext())
//							{
//								CountersModel cm = (CountersModel) iter.next();
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
									cm.setPersistCounters    (pcsStore);
									cm.setPersistCountersAbs (pcsStoreAbs);
									cm.setPersistCountersDiff(pcsStoreDiff);
									cm.setPersistCountersRate(pcsStoreRate);
									cm.setPostponeTime       (pcsPostpone.intValue());
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
				} // end: _optionStore_chk.isSelected()

				if ( _aseConn == null )
				{
					// CONNECT to the ASE, if it fails, we stay in the dialog
					if ( ! aseConnect() )
					{
						return;
					}
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
//						if (Asemon.hasGUI())
//							JOptionPane.showMessageDialog(MainFrame.getInstance(), msg, "asemon - connect check", JOptionPane.WARNING_MESSAGE);
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
		if (_monUrl_txt.equals(source))
		{
			try 
			{
				AseUrlHelper aseUrl = AseUrlHelper.parseUrl(_monUrl_txt.getText());

				_host_txt   .setText(aseUrl.getHosts());
				_port_txt   .setText(aseUrl.getPorts());
				_options_txt.setText(aseUrl.getOptions());

				String serverName = aseUrl.getServerName();
				if (serverName == null)
				{
					_server_cbx.setSelectedIndex(0);

					String host        = _host_txt.getText();
					String portStr     = _port_txt.getText();
					
					// Update the first entry in the combo box to be "host:port"
					// the host:port, will be what we have typed so far...
					// If the host_port can be found in the interfaces file, then
					// the combo box will display the server.
					_server_cbx.updateFirstEntry(host, portStr);
				}
				else
					_server_cbx.setSelectedItem(serverName);

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
		if (keyevent.getSource().equals(_port_txt))
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
		if (    keyevent.getSource().equals(_host_txt) 
		     || keyevent.getSource().equals(_port_txt) )
		{
			String hosts = _host_txt.getText();
			String ports = _port_txt.getText();
			
			// Update the first entry in the combo box to be "host:port"
			// the host:port, will be what we have typed so far...
			// If the host_port can be found in the interfaces file, then
			// the combo box will display the server.
			_server_cbx.updateFirstEntry(hosts, ports);
	
//			if (_logger.isDebugEnabled())
//			{
//				_serverName_lbl.setText(host + ":" + portStr);
//			}
		}
		validateContents();
	}

	// TAB change
	public void stateChanged(ChangeEvent e)
	{
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
		if (_monUrl_chk.isSelected())
		{
			_monUrl_txt  .setEnabled(true);

			_server_cbx  .setEnabled(false);
			_host_txt    .setEnabled(false);
			_port_txt    .setEnabled(false);
			_options_txt .setEnabled(false);
			_options_but .setEnabled(false);
			_ifile_txt   .setEnabled(false);
			_ifile_but   .setEnabled(false);
			return;
		}
		_monUrl_txt  .setEnabled(false);

		_server_cbx  .setEnabled(true);
		_host_txt    .setEnabled(true);
		_port_txt    .setEnabled(true);
		_options_txt .setEnabled(true);
		_options_but .setEnabled(true);
		_ifile_txt   .setEnabled(true);
		_ifile_but   .setEnabled(true);

		String[] hosts = StringUtil.commaStrToArray(_host_txt.getText());
		String[] ports = StringUtil.commaStrToArray(_port_txt.getText());
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
		Map<String,String>       optionsMap  = StringUtil.parseCommaStrToMap(_options_txt.getText());

		String url = AseUrlHelper.buildUrlString(hostPortMap, null, optionsMap);
		_monUrl_txt.setText(url);		
	}


	
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

		String hostPort = AseConnectionFactory.toHostPortStr(_host_txt.getText(), _port_txt.getText());

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

		conf.setProperty("conn.url.raw",                   _monUrl_txt.getText() );
		conf.setProperty("conn.url.raw.checkbox",          _monUrl_chk.isSelected() );
		conf.setProperty("conn.url.options",               _options_txt.getText() );

		conf.setProperty("conn.savePassword",              _optionSavePwd_chk.isSelected() );
		conf.setProperty(CONF_OPTION_CONNECT_ON_STARTUP,   _optionConnOnStart_chk.isSelected() );
		conf.setProperty(CONF_OPTION_RECONNECT_ON_FAILURE, _optionReConnOnFailure_chk.isSelected());
//		conf.setProperty("conn.usedForNoGui",              _optionUsedForNoGui_chk.isSelected() );
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

		conf.setProperty("conn.persistCounterStorage", _optionStore_chk.isSelected() );

		//----------------------------------
		// TAB: Counter Storage
		//----------------------------------
		if ( _optionStore_chk.isSelected() )
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



		str = conf.getProperty("conn.url.options");
		if (str != null)
			_options_txt.setText(str);
		
		str = conf.getProperty("conn.url.raw");
		if (str != null)
			_monUrl_txt.setText(str);

		bol = conf.getBooleanProperty("conn.url.raw.checkbox", false);
		_monUrl_chk.setSelected(bol);

		if (_monUrl_chk.isSelected())
		{
			try
			{
				AseUrlHelper aseUrl = AseUrlHelper.parseUrl(_monUrl_txt.getText());

				_host_txt   .setText(aseUrl.getHosts());
				_port_txt   .setText(aseUrl.getPorts());
				_options_txt.setText(aseUrl.getOptions());

				String serverName = aseUrl.getServerName();
				if (serverName == null)
				{
					_server_cbx.setSelectedIndex(0);

					String host        = _host_txt.getText();
					String portStr     = _port_txt.getText();
					
					// Update the first entry in the combo box to be "host:port"
					// the host:port, will be what we have typed so far...
					// If the host_port can be found in the interfaces file, then
					// the combo box will display the server.
					_server_cbx.updateFirstEntry(host, portStr);
				}
				else
					_server_cbx.setSelectedItem(serverName);
			}
			catch (ParseException pe)
			{
			}			
		}

		String hostPort = AseConnectionFactory.toHostPortStr(_host_txt.getText(), _port_txt.getText());
		loadPropsForServer(hostPort);


		
		bol = conf.getBooleanProperty("conn.savePassword", false);
		_optionSavePwd_chk.setSelected(bol);
		
		bol = conf.getBooleanProperty(CONF_OPTION_CONNECT_ON_STARTUP, false);
		_optionConnOnStart_chk.setSelected(bol); 

		bol = conf.getBooleanProperty(CONF_OPTION_RECONNECT_ON_FAILURE, true);
		_optionReConnOnFailure_chk.setSelected(bol); 

//		bol = conf.getBooleanProperty("conn.usedForNoGui", false);
//		_optionUsedForNoGui_chk.setSelected(bol);

		bol = conf.getBooleanProperty("conn.persistCounterStorage", false);
		_optionStore_chk.setSelected(bol);


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

		str = conf.getProperty("pcs.write.jdbcUrl");
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

		bol = conf.getBooleanProperty("pcs.read.checkForNewSessions", false);
		_offlineCheckForNewSessions_chk.setSelected(bol);
		
		bol = conf.getBooleanProperty("pcs.read.h2.startH2NetworkServer", false);
		_offlineH2Option_startH2NwSrv_chk.setSelected(bol);
	}
	private void getSavedWindowProps()
	{
		Configuration conf = Configuration.getInstance(Configuration.TEMP);
		if (conf == null)
		{
			_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
			return;
		}
		//----------------------------------
		// TAB: Offline
		//----------------------------------
		int width  = conf.getIntProperty("conn.dialog.window.width",  480);
		int height = conf.getIntProperty("conn.dialog.window.height", 620);
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
		Configuration conf = Configuration.getInstance(Configuration.TEMP);
		if (conf == null)
		{
			_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
			return;
		}

		String str = null;

		// First do "conn.username.hostName.portNum", if not found, go to "conn.username"
		str = conf.getProperty("conn.username."+hostPortStr);
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
		str = conf.getProperty("conn.password."+hostPortStr);
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

	public Configuration getAseOptions()
	{
		Configuration conf = new Configuration();

		conf.setProperty(CONF_OPTION_CONNECT_ON_STARTUP,   _optionConnOnStart_chk.isSelected());
		conf.setProperty(CONF_OPTION_RECONNECT_ON_FAILURE, _optionReConnOnFailure_chk.isSelected());
		
		return conf;
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
		final String CONFIG_FILE_NAME     = System.getProperty("CONFIG_FILE_NAME",     "asemon.properties");
		final String TMP_CONFIG_FILE_NAME = System.getProperty("TMP_CONFIG_FILE_NAME", "asemon.save.properties");
		final String ASEMON_HOME          = System.getProperty("ASEMON_HOME");
		
		String defaultPropsFile    = (ASEMON_HOME           != null) ? ASEMON_HOME           + "/" + CONFIG_FILE_NAME     : CONFIG_FILE_NAME;
		String defaultTmpPropsFile = (Version.APP_STORE_DIR != null) ? Version.APP_STORE_DIR + "/" + TMP_CONFIG_FILE_NAME : TMP_CONFIG_FILE_NAME;
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
		Configuration.setInstance(Configuration.TEMP, conf1);

		Configuration conf2 = new Configuration(defaultPropsFile);
		Configuration.setInstance(Configuration.CONF, conf2);

		
		// DO THE THING
		ConnectionDialog connDialog = new ConnectionDialog(null, false, true, false, false);
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

