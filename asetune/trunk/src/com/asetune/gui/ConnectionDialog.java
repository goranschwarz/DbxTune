/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DropMode;
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
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
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
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import org.jdesktop.swingx.table.TableColumnModelExt;

import com.asetune.AseTune;
import com.asetune.GetCounters;
import com.asetune.MonTablesDictionary;
import com.asetune.Version;
import com.asetune.cm.CountersModel;
import com.asetune.gui.focusabletip.FocusableTipExtention;
import com.asetune.gui.swing.GLabel;
import com.asetune.gui.swing.GTabbedPane;
import com.asetune.gui.swing.MultiLineLabel;
import com.asetune.gui.swing.TreeTransferHandler;
import com.asetune.gui.swing.VerticalScrollPane;
import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.gui.swing.WaitForExecDialog.BgExecutor;
import com.asetune.pcs.PersistReader;
import com.asetune.pcs.PersistWriterBase;
import com.asetune.pcs.PersistWriterJdbc;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.ssh.SshConnection;
import com.asetune.ssh.SshTunnelDialog;
import com.asetune.ssh.SshTunnelInfo;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.AseUrlHelper;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;
import com.asetune.utils.H2UrlHelper;
import com.asetune.utils.JdbcDriverHelper;
import com.asetune.utils.PlatformUtils;
import com.asetune.utils.RepServerUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;


public class ConnectionDialog
	extends JDialog
	implements ActionListener, KeyListener, TableModelListener, ChangeListener, FocusListener
{
	private static Logger _logger = Logger.getLogger(ConnectionDialog.class);
	private static final long serialVersionUID = -7782953767666701933L;

	public static final String CONF_OPTION_RECONNECT_ON_FAILURE = "conn.reconnectOnFailure";
	public static final String CONF_OPTION_CONNECT_ON_STARTUP   = "conn.onStartup";

	public  static final String  PROPKEY_CONN_SSH_TUNNEL        = "conn.ssh.tunnel";
	public  static final boolean DEFAULT_CONN_SSH_TUNNEL        = false;

	public static final String   PROPKEY_showDialogOnNoLocalPcsDrive = "ConnectionDialog.showDialog.pcs.noLocalDrive";
	public static final boolean  DEFAULT_showDialogOnNoLocalPcsDrive = true;
	
	private static final boolean DEFAULT_CONN_PROFILE_PANEL_VISIBLE  = true;
	private static final boolean DEFAULT_CONN_TABED_PANEL_VISIBLE    = true;

	static
	{
//		Configuration.registerDefaultValue(CONF_OPTION_RECONNECT_ON_FAILURE, DEFAULT_xxx); // FIXME
//		Configuration.registerDefaultValue(CONF_OPTION_CONNECT_ON_STARTUP,   DEFAULT_xxx); // FIXME
		Configuration.registerDefaultValue(PROPKEY_CONN_SSH_TUNNEL,          DEFAULT_CONN_SSH_TUNNEL);
	}
	
	public  static final int   CANCEL           = 0;
	public  static final int   TDS_CONN         = 1;
	public  static final int   OFFLINE_CONN     = 2;
//	public  static final int   TDS_CONN         = 3;
	public  static final int   JDBC_CONN        = 4;
	private int                _connectionType  = CANCEL;

//	private Map                      _inputMap        = null;
	private Connection               _aseConn         = null;
	private SshConnection            _sshConn         = null;
//	private Connection               _pcsConn         = null;
	private Connection               _offlineConn     = null;
	private Connection               _jdbcConn        = null;
//	private PersistentCounterHandler _pcsWriter       = null;

	/** If the connected Product Name must be a certain string, this is it */
	private String                   _desiredProductName = null;

	private Date                     _disConnectTime  = null;

	private static final int    TAB_POS_ASE        = 0;
	private static final int    TAB_POS_HOSTMON    = 1;
	private static final int    TAB_POS_PCS        = 2;
	private static final int    TAB_POS_OFFLINE    = 3;
	private static final int    TAB_POS_JDBC       = 4;

	private static final String TAB_TITLE_ASE      = "ASE";
	private static final String TAB_TITLE_HOSTMON  = "Host Monitor";
//	private static final String TAB_TITLE_PCS      = "Counter Storage";
//	private static final String TAB_TITLE_OFFLINE  = "Offline Connect";
	private static final String TAB_TITLE_PCS      = "Record this Session";
	private static final String TAB_TITLE_OFFLINE  = "Load Recorded Sessions";
	private static final String TAB_TITLE_JDBC     = "JDBC";

	private static final int    DEFAULT_CONN_SPLITPANE_DIVIDER_LOCATION = 150;
	
	//-------------------------------------------------
	// Actions
	public static final String ACTION_OK        = "OK";
	public static final String ACTION_CANCEL    = "CANCEL";

	@SuppressWarnings("unused")
	private Frame              _owner           = null;

	private int                _lastKnownConnSplitPaneDividerLocation = DEFAULT_CONN_SPLITPANE_DIVIDER_LOCATION;
	private JSplitPane         _connSplitPane;
	private JPanel             _connProfilePanel;
	private JPanel             _connTabbedPanel;
	private GTabbedPane        _tab;
	private JPanel             _okCancelPanel;
	private JCheckBox          _connProfileVisible_chk = new JCheckBox("", DEFAULT_CONN_PROFILE_PANEL_VISIBLE);
	private JCheckBox          _connTabbedVisible_chk  = new JCheckBox("", DEFAULT_CONN_TABED_PANEL_VISIBLE);

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

	private JLabel             _aseLoginTimeout_lbl= new JLabel("Login Timeout");
	private JTextField         _aseLoginTimeout_txt= new JTextField();

	private JLabel             _aseOptions_lbl     = new JLabel("URL Options");
	private JTextField         _aseOptions_txt     = new JTextField();
	private JButton            _aseOptions_but     = new JButton("...");

	private JLabel             _aseIfile_lbl       = new JLabel("Name service");
	private JTextField         _aseIfile_txt       = new JTextField();
	private String             _aseIfile_save      = "";
	private JButton            _aseIfile_but       = new JButton("...");
	private JButton            _aseEditIfile_but   = new JButton("Edit");

	private JCheckBox          _aseConnUrl_chk     = new JCheckBox("Use URL", false);
	private JTextField         _aseConnUrl_txt     = new JTextField();

	private SshTunnelInfo      _aseSshTunnelInfo       = null;
//	private JLabel             _aseSshTunnel_lbl       = new JLabel("SSH Tunnel");
	private JCheckBox          _aseSshTunnel_chk       = new JCheckBox("Use SSH (Secure Shell) Tunnel to connect to ASE", DEFAULT_CONN_SSH_TUNNEL);
	private JLabel             _aseSshTunnelDesc_lbl   = new JLabel();
	private JButton            _aseSshTunnel_but       = new JButton("SSH Settings...");

	private JLabel             _aseSqlInit_lbl         = new JLabel("SQL Init");
	private JTextField         _aseSqlInit_txt         = new JTextField("");

	private JCheckBox          _aseOptionSavePwd_chk            = new JCheckBox("Save password", true);
	private JCheckBox          _aseOptionConnOnStart_chk        = new JCheckBox("Connect to this server on startup", false);
	private JCheckBox          _aseOptionReConnOnFailure_chk    = new JCheckBox("Reconnect to server if connection is lost", true);
//	private JCheckBox          _aseOptionUsedForNoGui_chk       = new JCheckBox("Use connection info above for no-gui mode", false);
	private JCheckBox          _aseHostMonitor_chk              = new JCheckBox("<html>Monitor the OS Host for IO and CPU... <i>Set parameters in tab '<b>Host Monitor</b>'</i></html>", false);
//	private JCheckBox          _aseOptionStore_chk              = new JCheckBox("Save counter data in a Persistent Counter Storage...", false);
	private JCheckBox          _aseOptionStore_chk              = new JCheckBox("<html>Record this Performance Session in a DB... <i>Set parameters in tab '<b>Record this Session</b>'</i></html>", false);
	private JCheckBox          _aseDeferredConnect_chk          = new JCheckBox("Make the connection later", false);
	private JLabel             _aseDeferredConnectHour_lbl      = new JLabel("Start Hour");
	private SpinnerNumberModel _aseDeferredConnectHour_spm      = new SpinnerNumberModel(0, 0, 23, 1); // value, min, max, step
	private JSpinner           _aseDeferredConnectHour_sp       = new JSpinner(_aseDeferredConnectHour_spm);
	private JLabel             _aseDeferredConnectMinute_lbl    = new JLabel(", Minute");
	private SpinnerNumberModel _aseDeferredConnectMinute_spm    = new SpinnerNumberModel(0, 0, 59, 1); // value, min, max, step
	private JSpinner           _aseDeferredConnectMinute_sp     = new JSpinner(_aseDeferredConnectMinute_spm);
	private JLabel             _aseDeferredConnectTime_lbl      = new JLabel();
	private JCheckBox          _aseDeferredDisConnect_chk       = new JCheckBox("Disconnect After Elapsed Time", false);
	private JLabel             _aseDeferredDisConnectHour_lbl   = new JLabel("Hours");
	private SpinnerNumberModel _aseDeferredDisConnectHour_spm   = new SpinnerNumberModel(0, 0, 999, 1); // value, min, max, step
	private JSpinner           _aseDeferredDisConnectHour_sp    = new JSpinner(_aseDeferredDisConnectHour_spm);
	private JLabel             _aseDeferredDisConnectMinute_lbl = new JLabel(", Minutes");
	private SpinnerNumberModel _aseDeferredDisConnectMinute_spm = new SpinnerNumberModel(0, 0, 59, 1); // value, min, max, step
	private JSpinner           _aseDeferredDisConnectMinute_sp  = new JSpinner(_aseDeferredDisConnectMinute_spm);
	private JLabel             _aseDeferredDisConnectTime_lbl   = new JLabel();

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
	private JPanel             _pcsPanel                   = null;
	private ImageIcon          _pcsImageIcon               = SwingUtils.readImageIcon(Version.class, "images/pcs_write_32.png");
	private JLabel             _pcsIcon                    = new JLabel(_pcsImageIcon);
	private MultiLineLabel     _pcsHelp                    = new MultiLineLabel();
	private JLabel             _pcsWriter_lbl              = new JLabel("PCS Writer");
	private JComboBox          _pcsWriter_cbx              = new JComboBox();
	private JLabel             _pcsJdbcDriver_lbl          = new JLabel("JDBC Driver");
	private JComboBox          _pcsJdbcDriver_cbx          = new JComboBox();
	private JLabel             _pcsJdbcUrl_lbl             = new GLabel("JDBC Url"); 
	private JComboBox          _pcsJdbcUrl_cbx             = new JComboBox();
	private JButton            _pcsJdbcUrl_but             = new JButton("...");
	private JLabel             _pcsJdbcUsername_lbl        = new JLabel("Username");
	private JTextField         _pcsJdbcUsername_txt        = new JTextField("sa");
	private JLabel             _pcsJdbcPassword_lbl        = new JLabel("Password");
	private JTextField         _pcsJdbcPassword_txt        = new JPasswordField();
	private JLabel             _pcsTestConn_lbl            = new JLabel();
	private JButton            _pcsTestConn_but            = new JButton("Test Connection");
	private PcsTable           _pcsSessionTable            = null;
	private JButton            _pcsTabSelectAll_but        = new JButton("Select All");
	private JButton            _pcsTabDeSelectAll_but      = new JButton("Deselect All");
	private JButton            _pcsTabTemplate_but         = new JButton("Reset"); // "Set to Template"
	private JButton            _pcsOpenTcpConfigDialog_but = new JButton("Dialog...");
	private boolean            _useCmForPcsTable           = true;
	// Specific options if we are using H2 as PCS
	private JCheckBox          _pcsH2Option_startH2NetworkServer_chk = new JCheckBox("Start H2 Database as a Network Server", false);
	//---- PCS:DDL Lookup & Store
	private JCheckBox          _pcsDdl_doDdlLookupAndStore_chk             = new JCheckBox("Do DDL lookup and Store", PersistentCounterHandler.DEFAULT_ddl_doDdlLookupAndStore);
	private JLabel             _pcsDdl_afterDdlLookupSleepTimeInMs_lbl     = new JLabel("Sleep Time");
	private JTextField         _pcsDdl_afterDdlLookupSleepTimeInMs_txt     = new JTextField(""+PersistentCounterHandler.DEFAULT_ddl_afterDdlLookupSleepTimeInMs);
	private JCheckBox          _pcsDdl_addDependantObjectsToDdlInQueue_chk = new JCheckBox("Store Dependent Objects", PersistentCounterHandler.DEFAULT_ddl_addDependantObjectsToDdlInQueue);

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

	//---- SEND OFFLINE panel
	@SuppressWarnings("unused")
	private JPanel             _sendOfflinePanel            = null;
	private ImageIcon          _sendOfflineImageIcon        = SwingUtils.readImageIcon(Version.class, "images/pcs_send_32.png");
	private JLabel             _sendOfflineIcon             = new JLabel(_sendOfflineImageIcon);
	private MultiLineLabel     _sendOfflineHelp             = new MultiLineLabel();
//	private JLabel             _sendOfflineNotYetImpl1_lbl  = new JLabel("<html><i>NOT YET IMPLEMETED</i></html>", JLabel.CENTER);
	private JLabel             _sendOfflineNotYetImpl1_lbl  = new JLabel("<html><i></i></html>", JLabel.CENTER);
	private JLabel             _sendOfflineNotYetImpl2_lbl  = new JLabel();
	private JButton            _sendOfflineSend_but         = new JButton("Send DB for analyze...");
	private JButton            _sendOfflineTestConn_but     = new JButton("Test Connectivity");

	//---- JDBC panel
	@SuppressWarnings("unused")
	private JPanel             _jdbcPanel            = null;
	private ImageIcon          _jdbcImageIcon        = SwingUtils.readImageIcon(Version.class, "images/pcs_read_32.png"); // FIXME: get a icon for this
	private JLabel             _jdbcIcon             = new JLabel(_jdbcImageIcon);
	private MultiLineLabel     _jdbcHelp             = new MultiLineLabel();
	private JLabel             _jdbcDriver_lbl       = new JLabel("JDBC Driver");
	private JComboBox          _jdbcDriver_cbx       = new JComboBox();
	private JLabel             _jdbcUrl_lbl          = new JLabel("JDBC Url"); 
	private JComboBox          _jdbcUrl_cbx          = new JComboBox();
	private JButton            _jdbcUrl_but          = new JButton("...");
	private JLabel             _jdbcUsername_lbl     = new JLabel("Username");
	private JTextField         _jdbcUsername_txt     = new JTextField("sa");
	private JLabel             _jdbcPassword_lbl     = new JLabel("Password");
	private JTextField         _jdbcPassword_txt     = new JPasswordField();
//	private JLabel             _jdbcTestConn_lbl     = new JLabel();
	private JButton            _jdbcTestConn_but     = new JButton("Test Connection");
	private JLabel             _jdbcSqlInit_lbl      = new JLabel("SQL Init");
	private JTextField         _jdbcSqlInit_txt      = new JTextField("");
	private JLabel             _jdbcUrlOptions_lbl   = new JLabel("URL Options");
	private JTextField         _jdbcUrlOptions_txt   = new JTextField("");
	private JButton            _jdbcUrlOptions_but   = new JButton("...");
	
	//---- JDBC Driver Info panel
	@SuppressWarnings("unused")
	private JPanel             _jdbcDriverInfoPanel      = null;
	private ImageIcon          _jdbcDriverInfoImageIcon  = SwingUtils.readImageIcon(Version.class, "images/pcs_read_32.png"); // FIXME: get a icon for this
	private JLabel             _jdbcDriverInfoIcon       = new JLabel(_jdbcDriverInfoImageIcon);
	private MultiLineLabel     _jdbcDriverInfoHelp       = new MultiLineLabel();
//	private GTable             _jdbcDiTable              = new GTable();
//	private DefaultTableModel  _jdbcDiTableModel         = new DefaultTableModel();
//	private JButton            _jdbcDiAddDriver_but      = new JButton("Add Driver");

	//---- Buttons at the bottom
	private JLabel             _ok_lbl         = new JLabel(""); // Problem description if _ok is disabled
	private JButton            _ok             = new JButton("OK");
	private JButton            _cancel         = new JButton("Cancel");

	private boolean            _checkAseCfg    = true;
	private boolean            _showAseTab     = true;
	private boolean            _showHostmonTab = true;
	private boolean            _showPcsTab     = true;
	private boolean            _showOfflineTab = true;
	private boolean            _showJdbcTab    = false;

	private boolean            _showAseOptions = true;

//	private static ConnectionDialog   _instance       = null;
//	public static ConnectionDialog getInstance()
//	{
//		return _instance;
//	}

	public static Connection showTdsOnlyConnectionDialog(Frame owner)
	{
		ConnectionDialog connDialog = new ConnectionDialog(null, false, true, false, false, false, false, false);
		connDialog.setVisible(true);
		connDialog.dispose();

		int connType = connDialog.getConnectionType();
		
		if ( connType == ConnectionDialog.CANCEL)
			return null;

		if ( connType == ConnectionDialog.TDS_CONN)
			return connDialog.getAseConn();

		return null;
	}

	public ConnectionDialog(Frame owner)
	{
		this(owner, true, true, true, true, true, true, false);
	}
	public ConnectionDialog(Frame owner, boolean checkAseCfg, boolean showAseTab, boolean showAseOptions, boolean showHostmonTab, boolean showPcsTab, boolean showOfflineTab, boolean showJdbcTab)
	{
		super(owner, "Connect", ModalityType.DOCUMENT_MODAL);
//		super(owner, "Connect", true);
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
		_showJdbcTab    = showJdbcTab;

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

// TEMP CHANGED: THIS SHOULD BE DELETED... when the ConnectionProfile Works
if (_connProfileVisible_chk.isSelected())
	_connProfileVisible_chk.doClick();

		setFocus();
	}

	/**
	 * Set what tab that should be active
	 * @param jdbcConn
	 */
	public void setSelectedTab(int tabId)
	{
		if      (tabId == TDS_CONN    ) _tab.setSelectedTitle(TAB_TITLE_ASE);
		else if (tabId == OFFLINE_CONN) _tab.setSelectedTitle(TAB_TITLE_OFFLINE);
		else if (tabId == JDBC_CONN   ) _tab.setSelectedTitle(TAB_TITLE_JDBC);
		else _logger.warn("setSelectedTab(tabId="+tabId+"): invalid tabId="+tabId);
	}

	/**
	 * If the Product Name of the server we connects to needs to be of a specific name
	 * @param productName
	 */
	public void setDesiredProductName(String productName)
	{
		_desiredProductName = productName;
	}

	/**
	 * Get the connected database product name, simply call jdbc.getMetaData().getDatabaseProductName();
	 * @return null if not connected else: Retrieves the name of this database product.
	 * @see java.sql.DatabaseMetaData.getDatabaseProductName
	 */
	public String getDatabaseProductName() 
	throws SQLException
	{
		Connection conn = _aseConn;
		if (conn == null)
			conn = _offlineConn;
		if (conn == null)
			conn = _jdbcConn;
		return getDatabaseProductName(conn);
	}
	public static String getDatabaseProductName(Connection conn) 
	throws SQLException
	{
		if (conn == null)
			return null;
		try
		{
			String str = conn.getMetaData().getDatabaseProductName();
			_logger.debug("getDatabaseProductName() returns: '"+str+"'.");
			return str; 
		}
		catch (SQLException e)
		{
			// If NO metadata installed, check if it's a Sybase Replication Server.
			// JZ0SJ: Metadata accessor information was not found on this database. Please install the required tables as mentioned in the jConnect documentation.
			if ( "JZ0SJ".equals(e.getSQLState()) )
			{
				try
				{
					String str1 = "";
					String str2 = "";
					Statement stmt = conn.createStatement();
					ResultSet rs = stmt.executeQuery("admin rssd_name");
					while ( rs.next() )
					{
						str1 = rs.getString(1);
						str2 = rs.getString(2);
					}
					rs.close();
					stmt.close();

					_logger.info("Replication Server with RSSD at '"+str1+"."+str2+"'.");

					// If the above statement succeeds, then it must be a RepServer without metadata installed.
					return DbUtils.DB_PROD_NAME_SYBASE_RS;
				}
				catch(SQLException ignoreRsExceptions) {}
			}
			_logger.debug("getDatabaseProductName() Caught: "+e, e);
			throw e;
		}
	}
	/**
	 * Get the connected database version string, simply call jdbc.getMetaData().getDatabaseProductName();
	 * @return null if not connected else: Retrieves the version number of this database product.
	 * @see java.sql.DatabaseMetaData.getDatabaseProductName
	 */
	public String getDatabaseProductVersion() 
	throws SQLException
	{
		Connection conn = _aseConn;
		if (conn == null)
			conn = _offlineConn;
		if (conn == null)
			conn = _jdbcConn;
		return getDatabaseProductVersion(conn);
	}
	public static String getDatabaseProductVersion(Connection conn) 
	throws SQLException
	{
		if (conn == null)
			return null;
		try
		{
			String str = conn.getMetaData().getDatabaseProductVersion();
			_logger.debug("getDatabaseProductVersion() returns: '"+str+"'.");
			return str; 
		}
		catch (SQLException e)
		{
			// If NO metadata installed, check if it's a Sybase Replication Server.
			// JZ0SJ: Metadata accessor information was not found on this database. Please install the required tables as mentioned in the jConnect documentation.
			if ( "JZ0SJ".equals(e.getSQLState()) )
			{
				try
				{
					String str = "";
					Statement stmt = conn.createStatement();
					ResultSet rs = stmt.executeQuery("admin version");
					while ( rs.next() )
					{
						str = rs.getString(1);
					}
					rs.close();
					stmt.close();

					_logger.info("Replication Server with Version string '"+str+"'.");

					// If the above statement succeeds, then it must be a RepServer without metadata installed.
					return str;
				}
				catch(SQLException ignoreRsExceptions) {}
			}
			_logger.debug("getDatabaseProductVersion() Caught: "+e, e);
			throw e;
		}
	}

	/**
	 * Check if current connected product name is equal to the input parameter
	 * @param str Name of the product to test for
	 * @return true if equal
	 */
	public boolean isDatabaseProduct(String str)
	{
		if (str == null)
			return false;

		try
		{
			String currentDbProductName = getDatabaseProductName();
			return str.equals(currentDbProductName);
		}
		catch (SQLException e)
		{
			_logger.debug("isDatabaseProduct() Caught: "+e, e);
			return false;
		}
	}

	/**
	 * Get the connected database product name, simply call jdbc.getMetaData().getDatabaseProductName();
	 * @return null if not connected else: Retrieves the name of this database product.
	 * @see java.sql.DatabaseMetaData.getDatabaseProductName
	 */
	public String getDatabaseServerName() 
	throws SQLException
	{
		Connection conn = _aseConn;
		if (conn == null)
			conn = _offlineConn;
		if (conn == null)
			conn = _jdbcConn;
		return getDatabaseServerName(conn);
	}
	public static String getDatabaseServerName(Connection conn) 
	throws SQLException
	{
		if (conn == null)
			return null;
		
		String serverName = "";
		String currentDbProductName = getDatabaseProductName(conn);

		// ASE
		if      (DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(currentDbProductName))
		{
			serverName = AseConnectionUtils.getAseServername(conn);
		}
		// ASA SQL Anywhere
		else if (DbUtils.DB_PROD_NAME_SYBASE_ASA.equals(currentDbProductName))
		{
			serverName = AseConnectionUtils.getAseServername(conn);
		}
		// Sybase IQ
		else if (DbUtils.DB_PROD_NAME_SYBASE_IQ.equals(currentDbProductName))
		{
			serverName = AseConnectionUtils.getAseServername(conn);
		}
		// Replication Server
		else if (DbUtils.DB_PROD_NAME_SYBASE_RS.equals(currentDbProductName))
		{
			serverName = RepServerUtils.getServerName(conn);
		}
		// H2
		else if (DbUtils.DB_PROD_NAME_H2.equals(currentDbProductName))
		{
		}
		// HANA
		else if (DbUtils.DB_PROD_NAME_HANA.equals(currentDbProductName))
		{
			serverName = DbUtils.getHanaServername(conn);
		}
		// ORACLE
		else if (DbUtils.DB_PROD_NAME_ORACLE.equals(currentDbProductName))
		{
			serverName = DbUtils.getOracleServername(conn);
		}
		// Microsoft
		else if (DbUtils.DB_PROD_NAME_MSSQL.equals(currentDbProductName))
		{
			serverName = AseConnectionUtils.getAseServername(conn);
		}
		// UNKNOWN
		else
		{
		}
		return serverName;
	}

	/**
	 * Get the connected DJBC Driver Name, simply call jdbc.getMetaData().getDriverName();
	 * @return null if not connected else: Retrieves the name of this JDBC Driver.
	 * @see java.sql.DatabaseMetaData.getDriverName
	 */
	public String getDriverName() 
	throws SQLException
	{
		Connection conn = _aseConn;
		if (conn == null)
			conn = _offlineConn;
		if (conn == null)
			conn = _jdbcConn;
		return getDriverName(conn);
	}
	public static String getDriverName(Connection conn) 
	throws SQLException
	{
		if (conn == null)
			return null;

		String str = conn.getMetaData().getDriverName();
		_logger.debug("getDriverName() returns: '"+str+"'.");
		return str; 
	}

	/**
	 * Get the connected DJBC Driver Name, simply call jdbc.getMetaData().getDriverName();
	 * @return null if not connected else: Retrieves the name of this JDBC Driver.
	 * @see java.sql.DatabaseMetaData.getDriverName
	 */
	public String getDriverVersion() 
	throws SQLException
	{
		Connection conn = _aseConn;
		if (conn == null)
			conn = _offlineConn;
		if (conn == null)
			conn = _jdbcConn;
		return getDriverVersion(conn);
	}
	public static String getDriverVersion(Connection conn) 
	throws SQLException
	{
		if (conn == null)
			return null;

		String str = conn.getMetaData().getDriverVersion();
		_logger.debug("getDriverVersion() returns: '"+str+"'.");
		return str; 
	}

	public int                      getConnectionType() { return _connectionType; }
	public Connection               getAseConn()        { return _aseConn; }
	public SshConnection            getSshConn()        { return _sshConn; }
//	public Connection               getPcsConn()        { return _pcsConn; }
//	public PersistentCounterHandler getPcsWriter()      { return _pcsWriter; }
	public Connection               getOfflineConn()    { return _offlineConn; }
	public Connection               getJdbcConn()       { return _jdbcConn; }

	public SshTunnelInfo            getAseSshTunnelInfo() { return _aseSshTunnelInfo; }

	public Date getDisConnectTime()
	{
		return _disConnectTime;
	}

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

	/** depending on what we have connected to give the user name we connected as 
	 * @return null if not connected, else user name*/
	public String getUsername() 
	{ 
		if (getConnectionType() == TDS_CONN)
			return _aseUser_txt.getText(); 

		if (getConnectionType() == OFFLINE_CONN)
			return _offlineJdbcUsername_txt.getText(); 

		if (getConnectionType() == JDBC_CONN)
			return _jdbcUsername_txt.getText(); 

		return null;
	}

	/** depending on what we have connected to give the user name we connected as 
	 * @return null if not connected, else URL used when connecting */
	public String getUrl() 
	{ 
		if (getConnectionType() == TDS_CONN)
			return _aseConnUrl_txt.getText();

		if (getConnectionType() == OFFLINE_CONN)
			return _offlineJdbcUrl_cbx.getSelectedItem()+"";

		if (getConnectionType() == JDBC_CONN)
			return _jdbcUrl_cbx.getSelectedItem()+"";

		return null;
	}

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

	public String getJdbcDriver() { return _jdbcDriver_cbx  .getEditor().getItem().toString(); }
	public String getJdbcUrl()    { return _jdbcUrl_cbx     .getEditor().getItem().toString(); }
	public String getJdbcUser()   { return _jdbcUsername_txt.getText(); }
	public String getJdbcPasswd() { return _jdbcPassword_txt.getText(); }

	public void setJdbcDriver  (String driver)   { addAndSelectItem(_jdbcDriver_cbx, driver); }
	public void setJdbcUrl     (String url)      { addAndSelectItem(_jdbcUrl_cbx,    url); }
	public void setJdbcUsername(String username) { _jdbcUsername_txt.setText(username); }
	public void setJdbcPassword(String password) { _jdbcPassword_txt.setText(password); }

	private void addAndSelectItem(JComboBox cbx, String item)
	{
		boolean exists = false;
		for (int i=0; i<cbx.getItemCount(); i++)
		{
			Object obj = cbx.getItemAt(i);
			if (obj.equals(item))
			{
				exists = true;
				break;
			}
		}
		if ( ! exists )
			cbx.addItem(item);
		
		cbx.setSelectedItem(item);
	}

	/**
	 * Set a Connection profile to use when connecting
	 * @param name Name of the profile
	 */
	public void setConnProfileName(String name)
	{
		_logger.warn("setConnProfileName(name): NOT YET IMPLEMENTED");
		throw new RuntimeException("setConnProfileName(name): NOT YET IMPLEMENTED");
	}
	
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

		_connProfilePanel = createConnProfilePanel();
		_connTabbedPanel  = createConnTabbedPanel();
		_okCancelPanel    = createOkCancelPanel();

		_connSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		_connSplitPane.setLeftComponent (_connProfilePanel);
		_connSplitPane.setRightComponent(_connTabbedPanel);
//		_connSplitPane.setDividerLocation(150);
		
		VerticalScrollPane scroll = new VerticalScrollPane(_connSplitPane);
//		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
//		scroll.setVerticalScrollBarPolicy  (JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

		panel.add(scroll,           "grow, push, wrap");
		panel.add(_okCancelPanel,   "pushx, growx, bottom, right");

		loadProps();

		// enable/disable DEFERRED fields...
		aseDeferredConnectChkAction();
		aseDeferredDisConnectChkAction();

		setContentPane(panel);
	}

	private static class ConnProfileSrvType
	{
		public String _name;
		public String _vendor;
		public ConnProfileSrvType(String name, String vendor)
		{
			_name = name;
			_vendor = vendor;
		}
	}
	private JPanel createConnProfilePanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("insets 0 0 0 0, wrap 1", "", ""));   // insets Top Left Bottom Right

//		panel.add( new JButton("Create a Profile"));
//		panel.add( new JLabel("Connection Profiles"));
		
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("All", true);
//		DefaultMutableTreeNode root = new DefaultMutableTreeNode("Connection Profiles");

		DefaultMutableTreeNode n1   = new DefaultMutableTreeNode("Production", true);
		DefaultMutableTreeNode n1c1 = new DefaultMutableTreeNode(new ConnProfileSrvType("GORAN_1_DS", DbUtils.DB_PROD_NAME_SYBASE_ASE), false);
		DefaultMutableTreeNode n1c2 = new DefaultMutableTreeNode(new ConnProfileSrvType("XXX_DS",     DbUtils.DB_PROD_NAME_SYBASE_ASE), false);
		DefaultMutableTreeNode n1c3 = new DefaultMutableTreeNode(new ConnProfileSrvType("YYY_DS",     DbUtils.DB_PROD_NAME_SYBASE_ASE), false);
		n1.add(n1c1);
		n1.add(n1c2);
		n1.add(n1c3);

		DefaultMutableTreeNode n2   = new DefaultMutableTreeNode("Development", true);
		DefaultMutableTreeNode n2c1 = new DefaultMutableTreeNode(new ConnProfileSrvType("DEV_SERVER_1", DbUtils.DB_PROD_NAME_SYBASE_IQ), false);
		DefaultMutableTreeNode n2c2 = new DefaultMutableTreeNode(new ConnProfileSrvType("DEV_SERVER_2", DbUtils.DB_PROD_NAME_SYBASE_ASA), false);
		DefaultMutableTreeNode n2c3 = new DefaultMutableTreeNode(new ConnProfileSrvType("DEV_SERVER_3", DbUtils.DB_PROD_NAME_SYBASE_ASE), false);
		n2.add(n2c1);
		n2.add(n2c2);
		n2.add(n2c3);

		DefaultMutableTreeNode n3   = new DefaultMutableTreeNode("Test", true);
		DefaultMutableTreeNode n3c1 = new DefaultMutableTreeNode(new ConnProfileSrvType("TEST_1_RS",  DbUtils.DB_PROD_NAME_SYBASE_RS), false);
		DefaultMutableTreeNode n3c2 = new DefaultMutableTreeNode(new ConnProfileSrvType("TEST_2_XX",  DbUtils.DB_PROD_NAME_HANA), false);
		DefaultMutableTreeNode n3c3 = new DefaultMutableTreeNode(new ConnProfileSrvType("TEST_3_ORA", DbUtils.DB_PROD_NAME_ORACLE), false);
		n3.add(n3c1);
		n3.add(n3c2);
		n3.add(n3c3);

		DefaultMutableTreeNode n4   = new DefaultMutableTreeNode(new ConnProfileSrvType("Dummy", DbUtils.DB_PROD_NAME_H2), false);

        root.add(n1);
        root.add(n2);
        root.add(n3);
        root.add(n4);
         
		JTree connProfileTree = new JTree(root);
		connProfileTree.setRootVisible(false);
//		connProfileTree.expandRow(0);
//		connProfileTree.expandRow(1);
//		connProfileTree.expandRow(2);
		connProfileTree.setDragEnabled(true);  
		connProfileTree.setDropMode(DropMode.ON_OR_INSERT);
		connProfileTree.setTransferHandler(new TreeTransferHandler());
		connProfileTree.getSelectionModel().setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);  
		expandTree(connProfileTree);

		connProfileTree.setCellRenderer(new DefaultTreeCellRenderer()
		{
			private static final long serialVersionUID = 1L;
			@Override
			public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus)
			{
				Component comp = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

				if (value instanceof DefaultMutableTreeNode)
				{
					Object o = ((DefaultMutableTreeNode) value).getUserObject();
					if (o instanceof ConnProfileSrvType) 
					{
						if (comp instanceof JLabel)
						{
							JLabel l = (JLabel) comp;
							ConnProfileSrvType t = (ConnProfileSrvType) o;
							l.setIcon(ConnectionProfileManager.getIcon(t._vendor));
							l.setText(t._name);

							return l;
						}
					}
				} 
				return comp;
			}
		});
		JLabel heading = new JLabel(" Connection Profiles ");
		heading.setFont(new java.awt.Font("Dialog", Font.BOLD, 14));

		panel.add( heading );
		panel.add( connProfileTree, "push, grow" );
		panel.add( new JCheckBox("Add on Connect", true));
		
		return panel;
	}

	@SuppressWarnings("rawtypes")
	private void expandTree(JTree tree)
	{
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
		Enumeration e = root.breadthFirstEnumeration();
		while (e.hasMoreElements())
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
			if ( node.isLeaf() )
				continue;
			int row = tree.getRowForPath(new TreePath(node.getPath()));
			tree.expandRow(row);
		}
	}

	private JPanel createConnTabbedPanel()
	{
		JPanel panel = new JPanel();
//		panel.setLayout(new MigLayout("insets 0 0 0 0, wrap 1", "", ""));   // insets Top Left Bottom Right
		panel.setLayout(new BorderLayout());

		String aseTabTip     = "Connect to an ASE to monitor performance.";
		String hostmonTabTip = "Connect to Operating System host machine where ASE is hosted to monitor IO and or CPU performance.";
		String pcsTabTip     = "In GUI mode save Data Counter to a Storage, which can be view later by the 'offline' mode.";
		String offlineTabTip = "<html>" +
		                       "Connect to a 'offline' Counter Storage where "+Version.getAppName()+" has stored counter data.<br>" +
		                       Version.getAppName()+" will switch to 'offline' mode and just reads data from the Counter Storage.<br>" +
		                       "</html>";
		String jdbcTabTip    = "<html>Connect to any JDBC data source</html>";

//		_tab = new JTabbedPane();
		_tab = new GTabbedPane();
		_tab.addTab(TAB_TITLE_ASE,     null, createTabAse(),     aseTabTip);
		_tab.addTab(TAB_TITLE_HOSTMON, null, createTabHostmon(), hostmonTabTip);
		_tab.addTab(TAB_TITLE_PCS,     null, createTabPcs(),     pcsTabTip);
		_tab.addTab(TAB_TITLE_OFFLINE, null, createTabOffline(), offlineTabTip);
		_tab.addTab(TAB_TITLE_JDBC,    null, createTabJdbc(),    jdbcTabTip);
		
		if (! _showAseTab)     _tab.setVisibleAtModel(TAB_POS_ASE,     false);
		if (! _showHostmonTab) _tab.setVisibleAtModel(TAB_POS_HOSTMON, false);
		if (! _showPcsTab)     _tab.setVisibleAtModel(TAB_POS_PCS,     false);
		if (! _showOfflineTab) _tab.setVisibleAtModel(TAB_POS_OFFLINE, false);
		if (! _showJdbcTab)    _tab.setVisibleAtModel(TAB_POS_JDBC,    false);

		// Set active tab to first tab that is enabled
		for (int t=0; t<_tab.getTabCount(); t++)
		{
			if (_tab.isEnabledAt(t))
			{
				_tab.setSelectedIndex(t);
				break;
			}
		}

		_tab.addChangeListener(this);

//		panel.add(_tab);
		panel.add(_tab, BorderLayout.CENTER);

		return panel;
	}

	private JPanel createTabAse()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("wrap 1", "", ""));   // insets Top Left Bottom Right
		
		panel.add(createAseUserPasswdPanel(),  "growx, pushx");
		panel.add(createAseServerPanel(),      "growx, pushx");
		if (_showAseOptions)
			panel.add(createAseOptionsPanel(), "growx, pushx");
//		panel.add(createPcsPanel(),         "grow, hidemode 3");
		
		return panel;
	}
	private JPanel createTabHostmon()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("wrap 1", "", ""));   // insets Top Left Bottom Right
		
		panel.add(createHostmonUserPasswdPanel(), "growx, pushx");
		panel.add(createHostmonServerPanel(),     "growx, pushx");
		panel.add(createHostmonInfoPanel(),       "growx, pushx");
		
		return panel;
	}
	private JPanel createTabPcs()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("wrap 1", "", ""));   // insets Top Left Bottom Right
		
		panel.add(createPcsJdbcPanel(),              "growx, hidemode 3");
		panel.add(createPcsDdlLookupAndStorePanel(), "growx");
		panel.add(createPcsTablePanel(),             "grow, push");
		
		return panel;
	}
	private JPanel createTabOffline()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("wrap 1", "", ""));   // insets Top Left Bottom Right

		panel.add(createOfflineJdbcPanel(), "growx, pushx");
		panel.add(createSendOfflinePanel(), "growx, pushx");

		return panel;
	}
	private JPanel createTabJdbc()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("wrap 1", "", ""));   // insets Top Left Bottom Right

		panel.add(new JLabel("NOT YET FULLY IMPLEMENTED AND TESTED"), "grow");
		panel.add(createJdbcPanel(),           "growx, pushx");
		panel.add(createJdbcDriverInfoPanel(), "grow, push");
		
		return panel;
	}
	private JPanel createAseUserPasswdPanel()
	{
		JPanel panel = SwingUtils.createPanel("User information", true);
		panel.setLayout(new MigLayout("wrap 2", "", ""));   // insets Top Left Bottom Right

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

		// ADD FOCUS LISTENERS
		_aseUser_txt  .addFocusListener(this);
		_asePasswd_txt.addFocusListener(this);
		
		return panel;
	}

	private JPanel createAseServerPanel()
	{
		JPanel panel = SwingUtils.createPanel("Specify the server to connect to", true);
		panel.setLayout(new MigLayout("wrap 2", "", ""));   // insets Top Left Bottom Right

		// Initialize Interfaces fields
		String iFile = AseConnectionFactory.getIFileName();
		_aseIfile_txt.setText(iFile);
		_aseIfile_save = iFile;

		refreshServers();

		String urlExample="URL Example: jdbc:sybase:Tds:host1:port1[,server2:port2,...,serverN:portN][/mydb][?&OPT1=1024&OPT2=true&OPT3=some str]";

		_aseIfile_lbl       .setToolTipText("Directory Service file (sql.ini or interfaces) to use for resolving ASE name into hostname and port number");
		_aseIfile_txt       .setToolTipText("Directory Service file (sql.ini or interfaces) to use for resolving ASE name into hostname and port number");
		_aseIfile_but       .setToolTipText("Open a File Dialog to locate a Directory Service file.");
		_aseEditIfile_but   .setToolTipText("Edit the Name/Directory Service file. Just opens a text editor.");
		_aseServer_lbl      .setToolTipText("Name of the ASE you are connecting to");
		_aseServer_cbx      .setToolTipText("Name of the ASE you are connecting to");
		_aseHost_lbl        .setToolTipText("<html>Hostname or IP address of the ASE you are connecting to<br>Syntax: host1[,host2,...]</html>");
		_aseHost_txt        .setToolTipText("<html>Hostname or IP address of the ASE you are connecting to<br>Syntax: host1[,host2,...]</html>");
		_asePort_lbl        .setToolTipText("<html>Port number of the ASE you are connecting to<br>Syntax: port1[,port2,...]</html>");
		_asePort_txt        .setToolTipText("<html>Port number of the ASE you are connecting to<br>Syntax: port1[,port2,...]</html>");
		_aseLoginTimeout_lbl.setToolTipText("<html>Login timeout in seconds</html>");
		_aseLoginTimeout_txt.setToolTipText("<html>Login timeout in seconds</html>");
		_aseOptions_lbl     .setToolTipText("<html>JConnect Options that can be set.<br>Syntax: OPT1=value[, OPT2=value]<br>Or press the '...' button to add enties.</html>");
		_aseOptions_txt     .setToolTipText("<html>JConnect Options that can be set.<br>Syntax: OPT1=value[, OPT2=value]<br>Or press the '...' button to add enties.</html>");
		_aseOptions_but     .setToolTipText("Open a Dialog where available options are presented.");
		_aseConnUrl_chk     .setToolTipText("<html>Actual URL used to connect the monitored server.<br>"+urlExample+"</html>");
		_aseConnUrl_txt     .setToolTipText("<html>Actual URL used to connect the monitored server.<br>"+urlExample+"</html>");
		_aseSshTunnel_chk   .setToolTipText(
			"<html>" +
			    "Use a SSH (Secure Shell) connection as a tunnel or intermediate hop, when you can't connect " +
			    "directly to the destination machine where the ASE is hosted. This due to firewall restrictions or port blocking.<br>" +
			    "When you use a SSH Tunnel all data traffic will be encrypted.<br>" +
			    "<br> " +
			    "The intermediate unix/linux host needs to have a SSH Server, which normally runs on port 22. " +
			    "It should also allow port forwarding, which is enabled by default.<br>" +
			    "You will need an account on the machine where the SSH Server is hosted.<br>" +
			    "<br> " +
			    "Read more about SSH Tunnels on: <br>" +
			    "<ul>" +
			    "   <li>http://en.wikipedia.org/wiki/Port_forwarding </li>" +
			    "   <li>http://chamibuddhika.wordpress.com/2012/03/21/ssh-tunnelling-explained/ </li>" +
			    "</ul>" +
			    "Or just Google: 'ssh tunnel' or 'ssh port forwarding'.<br>" +
			"</html>");
		_aseSqlInit_lbl .setToolTipText("<html>Send this SQL Statement after the connection has been established.<br>If you want to send several statements, use ';' as a teminator for each statement.</html>");
		_aseSqlInit_txt .setToolTipText("<html>Send this SQL Statement after the connection has been established.<br>If you want to send several statements, use ';' as a teminator for each statement.</html>");

		panel.add(_aseServerIcon,       "");
		panel.add(_aseServerHelp,       "wmin 100, push, grow");

//		_ifile_txt.setEditable(false);
		panel.add(_aseIfile_lbl,         "");
		panel.add(_aseIfile_txt,         "push, grow, split");
//		panel.add(_aseIfile_but,         "wrap");
		panel.add(_aseIfile_but,         "");
		panel.add(_aseEditIfile_but,     "wrap");

		panel.add(_aseServer_lbl,        "");
		panel.add(_aseServer_cbx,        "push, grow");

		panel.add(_aseHost_lbl,          "");
		panel.add(_aseHost_txt,          "push, grow");

		panel.add(_asePort_lbl,          "");
		panel.add(_asePort_txt,          "split, push, grow");
		panel.add(_aseLoginTimeout_lbl,  "");
		panel.add(_aseLoginTimeout_txt,  "width 30:30:30, wrap");

//		panel.add(_aseSshTunnel_lbl,     "");
		panel.add(_aseSshTunnel_chk,     "skip, push, grow, split");
		panel.add(_aseSshTunnel_but,     "wrap");
		
		panel.add(_aseSshTunnelDesc_lbl, "skip, wrap, hidemode 3");

		panel.add(_aseSqlInit_lbl,       "");
		panel.add(_aseSqlInit_txt,       "push, grow");

		panel.add(_aseOptions_lbl,       "");
		panel.add(_aseOptions_txt,       "push, grow, split");
		panel.add(_aseOptions_but,       "wrap");

		panel.add(_aseConnUrl_chk,       "");
		panel.add(_aseConnUrl_txt,       "push, grow");
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
		_aseSshTunnel_chk.addActionListener(this);
		_aseSshTunnel_but.addActionListener(this);

		// If write in host/port, create the combined host:port and show that...
		_aseHost_txt        .addKeyListener(this);
		_asePort_txt        .addKeyListener(this);
		_aseLoginTimeout_txt.addKeyListener(this);

		// ADD FOCUS LISTENERS
		_aseIfile_txt  .addFocusListener(this);
		_aseServer_cbx .addFocusListener(this);
		_aseHost_txt   .addFocusListener(this);
		_asePort_txt   .addFocusListener(this);
		_aseOptions_txt.addFocusListener(this);
		
		return panel;
	}

	private JPanel createAseOptionsPanel()
	{
		JPanel panel = SwingUtils.createPanel("Options", true);
		panel.setLayout(new MigLayout("wrap 1, gap 0", "", ""));   // insets Top Left Bottom Right

//		_aseOptionSavePwd_chk        .setToolTipText("Save the password in the configuration file, and yes it's encrypted");
		_aseOptionConnOnStart_chk    .setToolTipText("When "+Version.getAppName()+" starts use the above ASE and connect automatically (if the below 'Persisten Counter Storage' is enabled, it will also be used at startup)");
		_aseOptionReConnOnFailure_chk.setToolTipText("If connection to the monitored server is lost in some way, try to reconnect to the server again automatically.");
		_aseOptionStore_chk          .setToolTipText("Store GUI Counter Data in a database (Persistent Counter Storage), which can be viewed later, connect to it from the 'offline' tab");
		_aseHostMonitor_chk          .setToolTipText("Connect to the Operating System host via SSH, to monitor IO statistics and/or CPU usage.");

		_aseDeferredConnect_chk        .setToolTipText("If you want to connect at a specific time, and start to collect data");
		_aseDeferredConnectHour_sp     .setToolTipText("What Hour do you want to connect");
		_aseDeferredConnectMinute_sp   .setToolTipText("What Minute do you want to connect");
		_aseDeferredDisConnect_chk     .setToolTipText("If you want to disconnect after a certain elapsed time frame");
		_aseDeferredDisConnectHour_sp  .setToolTipText("After how many hours of sampling do you want to disconnect");
		_aseDeferredDisConnectMinute_sp.setToolTipText("After how many minutes of sampling do you want to disconnect");

		if (_showPcsTab)
			panel.add(_aseOptionStore_chk,       "");

		if (_showHostmonTab)
			panel.add(_aseHostMonitor_chk,       "");

		if (_showHostmonTab || _showPcsTab)
			panel.add(new JSeparator(),          "gap 5 5 5 5, pushx, growx"); // gap left [right] [top] [bottom]

//		panel.add(_aseOptionSavePwd_chk,            "");
		panel.add(_aseOptionConnOnStart_chk,        "");
		panel.add(_aseOptionReConnOnFailure_chk,    "");
		panel.add(_aseDeferredConnect_chk,          "split");
		panel.add(_aseDeferredConnectHour_lbl,      "");
		panel.add(_aseDeferredConnectHour_sp,       "width 45:45");
		panel.add(_aseDeferredConnectMinute_lbl,    "width 45:45");
		panel.add(_aseDeferredConnectMinute_sp,     "");
		panel.add(_aseDeferredConnectTime_lbl,      "wrap");
		panel.add(_aseDeferredDisConnect_chk,       "split");
		panel.add(_aseDeferredDisConnectHour_lbl,   "");
		panel.add(_aseDeferredDisConnectHour_sp,    "width 45:45");
		panel.add(_aseDeferredDisConnectMinute_lbl, "width 45:45");
		panel.add(_aseDeferredDisConnectMinute_sp,  "");
		panel.add(_aseDeferredDisConnectTime_lbl,   "wrap");
//		panel.add(_aseOptionUsedForNoGui_chk,       "");

		_aseOptionConnOnStart_chk.addActionListener(this);
		_aseOptionStore_chk      .addActionListener(this);
		_aseHostMonitor_chk      .addActionListener(this);

		_aseDeferredConnect_chk        .addActionListener(this);
		_aseDeferredConnectHour_sp     .addChangeListener(this);
		_aseDeferredConnectMinute_sp   .addChangeListener(this);
		_aseDeferredDisConnect_chk     .addActionListener(this);
		_aseDeferredDisConnectHour_sp  .addChangeListener(this);
		_aseDeferredDisConnectMinute_sp.addChangeListener(this);

		return panel;
	}

	private JPanel createHostmonUserPasswdPanel()
	{
		JPanel panel = SwingUtils.createPanel("User information", true);
		panel.setLayout(new MigLayout("wrap 2", "", ""));   // insets Top Left Bottom Right

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

		// ADD FOCUS LISTENERS
//		_hostmonUser_txt  .addFocusListener(this);
//		_hostmonPasswd_txt.addFocusListener(this);
		
		return panel;
	}

	private JPanel createHostmonServerPanel()
	{
		JPanel panel = SwingUtils.createPanel("Specify the server to connect to", true);
		panel.setLayout(new MigLayout("wrap 2", "", ""));   // insets Top Left Bottom Right

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

		// ADD FOCUS LISTENERS
//		_hostmonHost_txt.addFocusListener(this);
//		_hostmonPort_txt.addFocusListener(this);
		
		return panel;
	}

	private JPanel createHostmonInfoPanel()
	{
		JPanel panel = SwingUtils.createPanel("Basic Host Monitoring Information", true);
		panel.setLayout(new MigLayout("wrap 1, gap 0", "", ""));   // insets Top Left Bottom Right

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
				"  <li> <code> uptime </code></li>" +
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
		"    Content of the <code></code> is based on Java <A HREF=\"http://docs.oracle.com/javase/6/docs/api/java/text/SimpleDateFormat.html\">SimpleDateFormat</A>, http://docs.oracle.com/javase/6/docs/api/java/text/SimpleDateFormat.html <br>" +
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
		panel.setLayout(new MigLayout("", "", ""));   // insets Top Left Bottom Right
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
		_pcsJdbcUsername_lbl.setToolTipText("<html>User name to be used by the Persistent Counter Storage to save Counter Data<br><br><b>Note</b>: this is <b>not</b> the ASE username</html>");
		_pcsJdbcUsername_txt.setToolTipText("<html>User name to be used by the Persistent Counter Storage to save Counter Data<br><br><b>Note</b>: this is <b>not</b> the ASE username</html>");
		_pcsJdbcPassword_lbl.setToolTipText("<html>Password to be used by the Persistent Counter Storage to save Counter Data<br><br><b>Note</b>: this is <b>not</b> the password to the ASE server, you can most likely leave this to blank.</html>");
		_pcsJdbcPassword_txt.setToolTipText("<html>Password to be used by the Persistent Counter Storage to save Counter Data<br><br><b>Note</b>: this is <b>not</b> the password to the ASE server, you can most likely leave this to blank.</html>");
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
		
		panel.add(_pcsH2Option_startH2NetworkServer_chk, "skip, span, split, push, grow");
		
		panel.add(_pcsTestConn_but, "wrap");
//		panel.add(_pcsWhatCm_but, "skip 1, right, wrap");
		
		// ADD ACTION LISTENERS
//		_pcsJdbcDriver_but.addActionListener(this);
//		_pcsJdbcUrl_but   .addActionListener(this);

		
//		GDefaultListCellRenderer pcsJdbcUrl_tooltipRenderer = new GDefaultListCellRenderer()
//		{
//			private static final long serialVersionUID = 1L;
//
//			
//			@Override
//			public String getToolTipText(MouseEvent e)
//			{
//				System.out.println("-------------getListCellRendererComponent........... getToolTipText(MouseEvent) e="+e);
//				return super.getToolTipText(e);
//			}
//			@Override
//			public String getToolTipText()
//			{
//				System.out.println("-------------getListCellRendererComponent........... getToolTipText()");
//				return super.getToolTipText();
//			}
//			@Override
//		    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) 
//		    {
//System.out.println("getListCellRendererComponent........... list="+list.getClass().getName());
//
//		        JComponent comp = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
//		        comp.setToolTipText(JDBC_URL_TOOLTIP);
//		        return comp;
//		    }
//		};
//		// Set This will make the Renderer to use GLable instead of JLabel
//		// GLable has the focusable tooltip implemented
////		_pcsJdbcUrl_cbx.setRenderer(new GDefaultListCellRenderer());
//		_pcsJdbcUrl_cbx.setRenderer(pcsJdbcUrl_tooltipRenderer);

		
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
//		_pcsJdbcUrl_cbx   .addItem("jdbc:h2:file:[<path>]<dbname>;AUTO_SERVER=TRUE");
//		_pcsJdbcUrl_cbx   .addItem("jdbc:h2:file:${ASETUNE_SAVE_DIR}/${SERVERNAME}_${DATE};AUTO_SERVER=TRUE");
//		_pcsJdbcUrl_cbx   .addItem("jdbc:h2:file:${ASETUNE_SAVE_DIR}/${ASEHOSTNAME}_${DATE};AUTO_SERVER=TRUE");
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

		// ADD FOCUS LISTENERS
//		xxx_txt.addFocusListener(this);
//		xxx_txt.addFocusListener(this);

		FocusableTipExtention.install(_pcsJdbcUrl_cbx);

		return panel;
	}

	private JPanel createPcsDdlLookupAndStorePanel()
	{
		JPanel panel = SwingUtils.createPanel("DDL Lookup and Store", true);
		panel.setLayout(new MigLayout("", "", ""));   // insets Top Left Bottom Right

		_pcsDdl_doDdlLookupAndStore_chk            .setToolTipText("<html>If you want the most accessed objects, Stored procedures, views etc and active statements to be DDL information to be stored in the PCS.<br>You can view them with the tool 'DDL Viewer' when connected to a offline database.<html>");
		_pcsDdl_addDependantObjectsToDdlInQueue_chk.setToolTipText("Also do DDL Lookup and Storage of dependant objects. Simply does 'exec sp_depends tabname' and add dependant objects for lookup...");
		_pcsDdl_afterDdlLookupSleepTimeInMs_lbl    .setToolTipText("How many milliseconds should we wait between DDL Lookups, this so we do not saturate the ASE Server.");
		_pcsDdl_afterDdlLookupSleepTimeInMs_txt    .setToolTipText("How many milliseconds should we wait between DDL Lookups, this so we do not saturate the ASE Server.");

		// LAYOUT
		panel.add(_pcsDdl_doDdlLookupAndStore_chk,             "");
		panel.add(_pcsDdl_addDependantObjectsToDdlInQueue_chk, "");

		panel.add(_pcsDdl_afterDdlLookupSleepTimeInMs_lbl,     "gap 50");
		panel.add(_pcsDdl_afterDdlLookupSleepTimeInMs_txt,     "push, grow, wrap");
		

		// ACTIONS
		_pcsDdl_doDdlLookupAndStore_chk            .addActionListener(this);
		_pcsDdl_addDependantObjectsToDdlInQueue_chk.addActionListener(this);
		_pcsDdl_afterDdlLookupSleepTimeInMs_txt    .addActionListener(this);

		// ADD FOCUS LISTENERS
//		xxx_txt.addFocusListener(this);
//		xxx_txt.addFocusListener(this);

		return panel;
	}

	private JPanel createPcsTablePanel()
	{
		JPanel panel = SwingUtils.createPanel("What Counter Data should be Persisted", true);
		panel.setLayout(new MigLayout("", "", ""));   // insets Top Left Bottom Right

		_pcsSessionTable = new PcsTable();

		PcsTableModel tm = (PcsTableModel) _pcsSessionTable.getModel();
		tm.addTableModelListener(this);

		JScrollPane jScrollPane = new JScrollPane(_pcsSessionTable);
//		jScrollPane.setViewportView(_pcsSessionTable);
//		jScrollPane.setMaximumSize(new Dimension(10000, 10000));
//		panel.add(jScrollPane, "push, grow, height 100%, wrap");
		panel.add(jScrollPane, "push, grow, wrap");

		panel.add(_pcsTabSelectAll_but,        "split");
		panel.add(_pcsTabDeSelectAll_but,      "split");
		panel.add(_pcsTabTemplate_but,         "");
		panel.add(new JLabel(),                "pushx, growx");
		panel.add(_pcsOpenTcpConfigDialog_but, "");

		// ADD ACTION LISTENERS
		_pcsTabSelectAll_but       .addActionListener(this);
		_pcsTabDeSelectAll_but     .addActionListener(this);
		_pcsTabTemplate_but        .addActionListener(this);
		_pcsOpenTcpConfigDialog_but.addActionListener(this);

		// TOOLTIP
		_pcsTabTemplate_but        .setToolTipText("Use current setting of the Performance Counter tabs as a template.");
		_pcsOpenTcpConfigDialog_but.setToolTipText("Open the 'All Performance Counter' options dialog. From here you can use other templates.");

		return panel;
	}
	
	private JPanel createOfflineJdbcPanel()
	{
		JPanel panel = SwingUtils.createPanel("Counter Storage Read", true);
		panel.setLayout(new MigLayout("", "", ""));   // insets Top Left Bottom Right
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
		_offlineJdbcUrl_cbx   .addItem("jdbc:h2:file:[<path>]<dbname>;IFEXISTS=TRUE;AUTO_SERVER=TRUE");
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

		// ADD FOCUS LISTENERS
//		xxx_txt.addFocusListener(this);
//		xxx_txt.addFocusListener(this);

		return panel;
	}

	private JPanel createSendOfflinePanel()
	{
		JPanel panel = SwingUtils.createPanel("Send a Recorded Session for Analyze", true);
		panel.setLayout(new MigLayout("", "", ""));   // insets Top Left Bottom Right
		_sendOfflinePanel = panel;
		
		_sendOfflineHelp.setText("If you want a recorded session to be analyzed by a skilled person...\n" +
			"This would be available as an extra service, which you will have to pay for! \n" +
			"To get this extra service you need to contact goran_schwarz@hotmail.com\n");

		_sendOfflineNotYetImpl2_lbl.setText("<html>" +
				"This functionality is <i>NOT YET IMPLEMETED</i>, sorry...<br>" +
				"<br>" +
				"But if you are <b>interested</b> in this functionality<br>" +
				"Please let me know, so I can implemented it ASAP<br>" +
				"<br>" +
				"If you <b>already</b> have the need for this functionality<br>" +
				"Please email me at goran_schwarz@hotmail.com, and we will work something out.<br>" +
				"<br>" +
				"</html>");
		
		panel.add(_sendOfflineIcon,            "");
		panel.add(_sendOfflineHelp,            "wmin 100, push, grow, wrap 15");

		panel.add(_sendOfflineNotYetImpl2_lbl, "skip 1, wrap");

		panel.add(_sendOfflineTestConn_but,    "skip 1, split 3");
		panel.add(_sendOfflineNotYetImpl1_lbl, "pushx, growx");
		panel.add(_sendOfflineSend_but,        "wrap");

		_sendOfflineSend_but    .setEnabled(false);
		_sendOfflineTestConn_but.setEnabled(false);
		
		// ACTIONS
		_sendOfflineSend_but    .addActionListener(this);
		_sendOfflineTestConn_but.addActionListener(this);

		// ADD FOCUS LISTENERS
//		xxx_txt.addFocusListener(this);
//		xxx_txt.addFocusListener(this);

//		SwingUtils.setEnabled(panel, false);
		return panel;
	}

	private JPanel createJdbcPanel()
	{
		JPanel panel = SwingUtils.createPanel("JDBC Connection Information", true);
		panel.setLayout(new MigLayout("", "", ""));   // insets Top Left Bottom Right
		_jdbcPanel = panel;
		
		_jdbcHelp.setText("Connect to any JDBC datasource\n\nNote: The JDBC Driver needs to be in the classpath\n");

		_jdbcDriver_lbl    .setToolTipText("JDBC drivername to be used when creating the connection");
		_jdbcDriver_cbx    .setToolTipText("JDBC drivername to be used when creating the connection");
		_jdbcUrl_lbl       .setToolTipText("URL for the above JDBC drivername to connect to a datastore, a couple of template URL for H2 and Sybase JDBC driver");
		_jdbcUrl_cbx       .setToolTipText("URL for the above JDBC drivername to connect to a datastore, a couple of template URL for H2 and Sybase JDBC driver");
		_jdbcUrl_but       .setToolTipText("Open a File chooser dialog to get a filename, for some templates values are replaced");
		_jdbcUsername_lbl  .setToolTipText("User name to be used when creating the connection");
		_jdbcUsername_txt  .setToolTipText("User name to be used when creating the connection");
		_jdbcPassword_lbl  .setToolTipText("Password to be used when creating the connection");
		_jdbcPassword_txt  .setToolTipText("Password to be used when creating the connection");
		_jdbcSqlInit_lbl   .setToolTipText("<html>Send this SQL Statement after the connection has been established.<br>If you want to send several statements, use ';' as a teminator for each statement.</html>");
		_jdbcSqlInit_txt   .setToolTipText("<html>Send this SQL Statement after the connection has been established.<br>If you want to send several statements, use ';' as a teminator for each statement.</html>");
		_jdbcUrlOptions_lbl.setToolTipText("<html>If the current Driver supports <code>driver.getPropertyInfo()</code>, show available Options.<br><b>NOTE</b>: You still have to copy the Option into the URL field yourself...</html>");
		_jdbcUrlOptions_txt.setToolTipText("<html>If the current Driver supports <code>driver.getPropertyInfo()</code>, show available Options.<br><b>NOTE</b>: You still have to copy the Option into the URL field yourself...</html>");
		_jdbcUrlOptions_but.setToolTipText("<html>If the current Driver supports <code>driver.getPropertyInfo()</code>, show available Options.<br><b>NOTE</b>: You still have to copy the Option into the URL field yourself...</html>");
		_jdbcTestConn_but  .setToolTipText("Make a test connection to the above JDBC datastore");

		panel.add(_jdbcIcon,  "");
		panel.add(_jdbcHelp,  "wmin 100, push, grow, wrap 15");

		panel.add(_jdbcDriver_lbl,   "");
		panel.add(_jdbcDriver_cbx,   "push, grow, wrap");

		panel.add(_jdbcUrl_lbl,      "");
		panel.add(_jdbcUrl_cbx,      "push, grow, split");
		panel.add(_jdbcUrl_but,      "wrap");

		panel.add(_jdbcUsername_lbl, "");
		panel.add(_jdbcUsername_txt, "push, grow, wrap");

		panel.add(_jdbcPassword_lbl, "");
		panel.add(_jdbcPassword_txt, "push, grow, wrap");
		
		panel.add(_jdbcSqlInit_lbl, "");
		panel.add(_jdbcSqlInit_txt, "push, grow, wrap");
		
		panel.add(_jdbcUrlOptions_lbl, "");
		panel.add(_jdbcUrlOptions_txt, "push, grow, split");
		panel.add(_jdbcUrlOptions_but, "wrap");

//		panel.add(_jdbcTestConn_lbl, "skip, split, left");
		panel.add(_jdbcTestConn_but, "skip, right, wrap");
		
		_jdbcDriver_cbx.setEditable(true);
		_jdbcUrl_cbx   .setEditable(true);

		List<String> driversList = JdbcDriverHelper.getAvailableDriverList();
		if (driversList.size() > 0)
		{
			for (String str : driversList)
				_jdbcDriver_cbx.addItem(str);
		}
		else
		{
			_jdbcDriver_cbx.addItem("org.h2.Driver");
//			_jdbcDriver_cbx.addItem(AseConnectionFactory.getDriver());
			_jdbcDriver_cbx.addItem("com.sybase.jdbc3.jdbc.SybDriver");
			_jdbcDriver_cbx.addItem("com.sybase.jdbc4.jdbc.SybDriver");
			_jdbcDriver_cbx.addItem("com.sap.db.jdbc.Driver");
		}

		// http://www.h2database.com/html/features.html#database_url
		_jdbcUrl_cbx   .addItem("jdbc:h2:file:[<path>]<dbname>;IFEXISTS=TRUE;AUTO_SERVER=TRUE");
		_jdbcUrl_cbx   .addItem("jdbc:h2:zip:<zipFileName>!/<dbname>");
		_jdbcUrl_cbx   .addItem("jdbc:h2:tcp://<host>[:<port>]/<dbname>");
		_jdbcUrl_cbx   .addItem("jdbc:h2:ssl://<host>[:<port>]/<dbname>");
 
		// http://infocenter.sybase.com/help/topic/com.sybase.dc39001_0605/html/prjdbc/X39384.htm
		_jdbcUrl_cbx   .addItem("jdbc:sybase:Tds:<host>:<port>");
		_jdbcUrl_cbx   .addItem("jdbc:sybase:Tds:<host>:<port>[/<dbname>]");
//		_jdbcUrl_cbx   .addItem("jdbc:sybase:Tds:<host>:<port>[/<dbname>][?OPT1=val,&OPT2=val]");

		
		// ACTIONS
		_jdbcDriver_cbx    .addActionListener(this);
		_jdbcTestConn_but  .addActionListener(this);
		_jdbcUrl_cbx       .getEditor().getEditorComponent().addKeyListener(this);
		_jdbcUrl_cbx       .addActionListener(this);
		_jdbcPassword_txt  .addActionListener(this);
		_jdbcUrl_but       .addActionListener(this);
		_jdbcUrlOptions_but.addActionListener(this);

		// ADD FOCUS LISTENERS
//		xxx_txt.addFocusListener(this);
//		xxx_txt.addFocusListener(this);

		return panel;
	}

	private JPanel createJdbcDriverInfoPanel()
	{
		JPanel panel = SwingUtils.createPanel("JDBC Driver Information", true);
		panel.setLayout(new MigLayout("", "", ""));   // insets Top Left Bottom Right
		_jdbcDriverInfoPanel = panel;
		
		_jdbcDriverInfoHelp.setText(
				"What JDBC Drivers are known to the system\n" +
				"Or what drivers has been registered in the Java DriverManager.\n" +
				"This is not 100% functional yet (some bugs in the dialog)");

//		_jdbcDriver_lbl  .setToolTipText("JDBC drivername to be used when creating the connection");
//		_jdbcDriver_cbx  .setToolTipText("JDBC drivername to be used when creating the connection");
//		_jdbcUrl_lbl     .setToolTipText("URL for the above JDBC drivername to connect to a datastore, a couple of template URL for H2 and Sybase JDBC driver");
//		_jdbcUrl_cbx     .setToolTipText("URL for the above JDBC drivername to connect to a datastore, a couple of template URL for H2 and Sybase JDBC driver");
//		_jdbcUrl_but     .setToolTipText("Open a File chooser dialog to get a filename, for some templates values are replaced");
//		_jdbcUsername_lbl.setToolTipText("User name to be used when creating the connection");
//		_jdbcUsername_txt.setToolTipText("User name to be used when creating the connection");
//		_jdbcPassword_lbl.setToolTipText("Password to be used when creating the connection");
//		_jdbcPassword_txt.setToolTipText("Password to be used when creating the connection");
//		_jdbcTestConn_but    .setToolTipText("Make a test connection to the above JDBC datastore");

		panel.add(_jdbcDriverInfoIcon,  "");
		panel.add(_jdbcDriverInfoHelp,  "wmin 100, pushx, growx, wrap 15");

//		panel.add(new JLabel("NOT YET IMPLEMENTED"),   "wrap");
		
		panel.add(new JdbcDriverHelper.JdbcDriverInfoPanel(), "span, push, grow");

//		_jdbcDiTableModel.addColumn("Class");
//		_jdbcDiTableModel.addColumn("Description");
//		_jdbcDiTableModel.addColumn("Home Page");
//		_jdbcDiTableModel.addColumn("Version");
//		_jdbcDiTableModel.addColumn("Jar File");
//		_jdbcDiTableModel.addColumn("toString");
//
//		for (Enumeration<Driver> drivers = DriverManager.getDrivers(); drivers.hasMoreElements();)
//		{
//			Driver driver    = drivers.nextElement();
//			String className = driver.getClass().getName();
//			String desc      = "";
//			String homePage  = "";
//			String version   = "";
//			if      ("sun.jdbc.odbc.JdbcOdbcDriver"    .equals(className)) { desc = "JDBC - ODBC Bridge";     homePage = "en.wikipedia.org/wiki/JDBC_driver"; }
//			else if ("com.sybase.jdbc3.jdbc.SybDriver" .equals(className)) { desc = "Sybase JDBC 3.0 Driver"; homePage = "www.sybase.com/jconnect"; }
//			else if ("com.sybase.jdbc4.jdbc.SybDriver" .equals(className)) { desc = "Sybase JDBC 4.0 Driver"; homePage = "www.sybase.com/jconnect"; }
//			else if ("net.sourceforge.jtds.jdbc.Driver".equals(className)) { desc = "jTDS Driver";            homePage = "jtds.sourceforge.net"; }
//			else if ("org.h2.Driver"                   .equals(className)) { desc = "H2 Driver";              homePage = "www.h2database.com"; }
//			else if ("com.sap.db.jdbc.Driver"          .equals(className)) { desc = "SAP HANA Driver";        homePage = "www.sap.com/HANA"; }
//
//			try
//			{
//				DriverPropertyInfo[] dpi = driver.getPropertyInfo("", null);
//				if (dpi != null)
//				{
//					for (int i=0; i<dpi.length; i++)
//					{
//						String xName  = dpi[i].name;
//						String xValue = dpi[i].value;
//					//	String xDesc  = dpi[i].description;
//					//	System.out.println("dpi["+i+"]: name='"+dpi[i].name+"', value='"+dpi[i].value+"', desc='"+dpi[i].description+"'.");
//						
//						if (xName != null && xName.toLowerCase().startsWith("version"))
//						{
//							version = xValue;
//							if (version != null && version.indexOf('\n') >= 0)
//								version = version.substring(0, version.indexOf('\n'));
//						}
//					}
//				}
//			}
//			catch (Throwable ignore) {}
//
//			Vector<String> row = new Vector<String>();
//			row.add(className);
//			row.add(desc);
//			row.add(homePage);
//			row.add(version);
//			row.add("<in system classpath>");
//			row.add(driver.toString());
//			_jdbcDiTableModel.addRow(row);
////			try
////			{
////				System.out.println("DRIVER: class='"+driver.getClass().getName()+"', props="+driver.getPropertyInfo("", null));
////			}
////			catch (SQLException e)
////			{
////				// TODO Auto-generated catch block
////				e.printStackTrace();
////			}
//		}
//		JScrollPane jScrollPane = new JScrollPane();
//		jScrollPane.setViewportView(_jdbcDiTable);
//
//		_jdbcDiTable.setModel(_jdbcDiTableModel);
//		_jdbcDiTable.setSortable(true);
//		_jdbcDiTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
//		_jdbcDiTable.setColumnControlVisible(true);
//		_jdbcDiTable.packAll();
//
//		panel.add(jScrollPane,           "span, push, grow, wrap");
//		panel.add(_jdbcDiAddDriver_but,  "span, right, pushx, wrap");
//
//		_jdbcDiAddDriver_but.setEnabled(false);
//		_jdbcDiAddDriver_but.setToolTipText("Sorry not yet implemeted...");

		/*
		http://stackoverflow.com/questions/288828/how-to-use-a-jdbc-driver-from-an-arbitrary-location
		http://www.kfu.com/~nsayer/Java/dyn-jdbc.html
		http://www.jroller.com/tackline/entry/dynamically_loading_jdbc_drivers
		*/
		
//		panel.add(_jdbcDriver_lbl,   "");
//		panel.add(_jdbcDriver_cbx,   "push, grow, wrap");
//
//		panel.add(_jdbcUrl_lbl,      "");
//		panel.add(_jdbcUrl_cbx,      "push, grow, split");
//		panel.add(_jdbcUrl_but,      "wrap");
//
//		panel.add(_jdbcUsername_lbl, "");
//		panel.add(_jdbcUsername_txt, "push, grow, wrap");
//
//		panel.add(_jdbcPassword_lbl, "");
//		panel.add(_jdbcPassword_txt, "push, grow, wrap");
//		
////		panel.add(_jdbcTestConn_lbl, "skip, split, left");
//		panel.add(_jdbcTestConn_but, "skip, right, wrap");
//		
//		_jdbcDriver_cbx.setEditable(true);
//		_jdbcUrl_cbx   .setEditable(true);
//		
//		_jdbcDriver_cbx.addItem("org.h2.Driver");
////		_jdbcDriver_cbx.addItem(AseConnectionFactory.getDriver());
//		_jdbcDriver_cbx.addItem("com.sybase.jdbc3.jdbc.SybDriver");
//		_jdbcDriver_cbx.addItem("com.sybase.jdbc4.jdbc.SybDriver");
//
//		// http://www.h2database.com/html/features.html#database_url
//		_jdbcUrl_cbx   .addItem("jdbc:h2:file:[<path>]<dbname>;IFEXISTS=TRUE;AUTO_SERVER=TRUE");
//		_jdbcUrl_cbx   .addItem("jdbc:h2:zip:<zipFileName>!/<dbname>");
//		_jdbcUrl_cbx   .addItem("jdbc:h2:tcp://<host>[:<port>]/<dbname>");
//		_jdbcUrl_cbx   .addItem("jdbc:h2:ssl://<host>[:<port>]/<dbname>");
// 
//		// http://infocenter.sybase.com/help/topic/com.sybase.dc39001_0605/html/prjdbc/X39384.htm
//		_jdbcUrl_cbx   .addItem("jdbc:sybase:Tds:<host>:<port>");
//		_jdbcUrl_cbx   .addItem("jdbc:sybase:Tds:<host>:<port>[/<dbname>]");
////		_jdbcUrl_cbx   .addItem("jdbc:sybase:Tds:<host>:<port>[/<dbname>][?OPT1=val,&OPT2=val]");
//
//		
//		// ACTIONS
//		_jdbcDriver_cbx  .addActionListener(this);
//		_jdbcTestConn_but    .addActionListener(this);
//		_jdbcUrl_cbx     .getEditor().getEditorComponent().addKeyListener(this);
//		_jdbcUrl_cbx     .addActionListener(this);
//		_jdbcPassword_txt.addActionListener(this);
//		_jdbcUrl_but     .addActionListener(this);

		// ADD FOCUS LISTENERS
//		xxx_txt.addFocusListener(this);
//		xxx_txt.addFocusListener(this);

		return panel;
	}

	private JPanel createOkCancelPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("", "", ""));   // insets Top Left Bottom Right

		_connProfileVisible_chk.setToolTipText("Show/Hide the Connection Profile Panel");
		_connProfileVisible_chk.setIcon(        SwingUtils.readImageIcon(Version.class, "images/layouts_unselect_sidebar.png"));
		_connProfileVisible_chk.setSelectedIcon(SwingUtils.readImageIcon(Version.class, "images/layouts_select_sidebar.png"));
		_connProfileVisible_chk.setText("");
		_connProfileVisible_chk.setContentAreaFilled(false);
		_connProfileVisible_chk.setMargin( new Insets(0,0,0,0) );
//		_connProfileVisible_chk.addActionListener(this);
//		_connProfileVisible_chk.setActionCommand(ACTION_XXX);
		_connProfileVisible_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				actionPerformedProfileOrTabbedPane(e);
			}
		});

		_connTabbedVisible_chk.setToolTipText("Show/Hide the Tabbed Connection Properties Panel");
		_connTabbedVisible_chk.setIcon(        SwingUtils.readImageIcon(Version.class, "images/layouts_unselect_content.png"));
		_connTabbedVisible_chk.setSelectedIcon(SwingUtils.readImageIcon(Version.class, "images/layouts_select_content.png"));
		_connTabbedVisible_chk.setText("");
		_connTabbedVisible_chk.setContentAreaFilled(false);
		_connTabbedVisible_chk.setMargin( new Insets(0,0,0,0) );
//		_connTabbedVisible_chk.addActionListener(this);
//		_connTabbedVisible_chk.setActionCommand(ACTION_XXX);
		_connTabbedVisible_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				actionPerformedProfileOrTabbedPane(e);
			}
		});

		_ok_lbl.setForeground(Color.RED);
		_ok_lbl.setFont( _ok_lbl.getFont().deriveFont(Font.BOLD) );
		_ok_lbl.setText("");

		// ADD the OK, Cancel, Apply buttons
		panel.add(_connProfileVisible_chk, "");
		panel.add(_connTabbedVisible_chk,  "");
		panel.add(new JLabel(), "pushx, growx"); // make a dummy label to "grow" the lefthand components with the righthand components
		panel.add(_ok_lbl, "");
		panel.add(_ok,     "tag ok, right");
		panel.add(_cancel, "tag cancel");

		// ADD ACTIONS TO COMPONENTS
		_ok           .addActionListener(this);
		_cancel       .addActionListener(this);

		return panel;
	}
	private void actionPerformedProfileOrTabbedPane(ActionEvent e)
	{
		if (_connProfileVisible_chk.isSelected() && _connTabbedVisible_chk.isSelected())
		{
			_connSplitPane.setDividerSize( 5 );
			_connSplitPane.setDividerLocation( _lastKnownConnSplitPaneDividerLocation );
		}
		else
		{
			if (_connSplitPane.getDividerSize() > 0)
				_lastKnownConnSplitPaneDividerLocation = _connSplitPane.getDividerLocation();
			_connSplitPane.setDividerSize( 0 );
		}

		_connProfilePanel.setVisible( _connProfileVisible_chk.isSelected() );
		_connTabbedPanel.setVisible(  _connTabbedVisible_chk .isSelected() );
	}
	/*---------------------------------------------------
	** END: component initialization
	**---------------------------------------------------
	*/

	/*---------------------------------------------------
	** BEGIN: subclasses for PCS Table
	**---------------------------------------------------
	*/
	private static final String[] PCS_TAB_HEADER = {"Icon", "Short Desc", "Group", "Timeout", "Postpone", "Store", "Abs", "Diff", "Rate", "Long Description", "CM Name"};
	private static final int PCS_TAB_POS_ICON       = 0;
	private static final int PCS_TAB_POS_TAB_NAME   = 1; //fix PCS_TAB_POS_GROUP_NAME or use TcpConfigDialog
	private static final int PCS_TAB_POS_GROUP_NAME = 2; 
	private static final int PCS_TAB_POS_TIMEOUT    = 3;
	private static final int PCS_TAB_POS_POSTPONE   = 4;
	private static final int PCS_TAB_POS_STORE_PCS  = 5;
	private static final int PCS_TAB_POS_STORE_ABS  = 6;
	private static final int PCS_TAB_POS_STORE_DIFF = 7;
	private static final int PCS_TAB_POS_STORE_RATE = 8;
	private static final int PCS_TAB_POS_LONG_DESC  = 9;
	private static final int PCS_TAB_POS_CM_NAME    = 10;

	private static final Color PCS_TAB_PCS_COL_BG = new Color(240, 240, 240);

	/** PcsTable */
	private static class PcsTable
	extends JXTable
	{
		private static final long	serialVersionUID	= 1L;

		PcsTable()
		{
			super();
			setModel( new PcsTableModel() );

			setShowGrid(false);
			setSortable(true);
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			packAll(); // set size so that all content in all cells are visible
			setColumnControlVisible(true);

			// Populate the table
			refreshTable();
			
			// make this low, otherwise it will grow to much because of any outer JScrollPane
			setPreferredScrollableViewportSize(new Dimension(400, 100));

			// hide 'Group Name' if no child's are found
			if ( MainFrame.hasInstance() && ! MainFrame.getInstance().getTabbedPane().hasChildPanels() )
			{
				TableColumnModelExt tcmx = (TableColumnModelExt)this.getColumnModel();
				tcmx.getColumnExt(PCS_TAB_HEADER[PCS_TAB_POS_GROUP_NAME]).setVisible(false);
			}
		}

		/** Enable/Disable + add some color to pcsStore, Abs, Diff, Rate */
		@Override
		public Component prepareRenderer(TableCellRenderer renderer, int row, int column)
		{
			Component c = super.prepareRenderer(renderer, row, column);

			int view_PCS_TAB_POS_STORE_PCS  = convertColumnIndexToView(PCS_TAB_POS_STORE_PCS);
			int view_PCS_TAB_POS_STORE_ABS  = convertColumnIndexToView(PCS_TAB_POS_STORE_ABS);
			int view_PCS_TAB_POS_STORE_RATE = convertColumnIndexToView(PCS_TAB_POS_STORE_RATE);
			
			if (column >= view_PCS_TAB_POS_STORE_PCS && column <= view_PCS_TAB_POS_STORE_RATE)
			{
				c.setBackground(PCS_TAB_PCS_COL_BG);
				if ((column >= view_PCS_TAB_POS_STORE_ABS && column <= view_PCS_TAB_POS_STORE_RATE) || row == 0)
				{
					// if not editable, lets disable it
					// calling isCellEditable instead of getModel().isCellEditable(row, column)
					// does the viewRow->modelRow translation for us.
					c.setEnabled( isCellEditable(row, column) );
				}
			}
			return c;
		}

		/** Populate information in the table */
		protected void refreshTable()
		{
			DefaultTableModel tm = (DefaultTableModel)getModel();

			JTabbedPane tabPane = MainFrame.hasInstance() ? MainFrame.getInstance().getTabbedPane() : null;
			if (tabPane == null)
				return;

			while (tm.getRowCount() > 0)
				tm.removeRow(0);

			refreshTable(tabPane, tm, null);

			packAll(); // set size so that all content in all cells are visible
		}
		private void refreshTable(JTabbedPane tabPane, DefaultTableModel tm, String groupName)
		{
			for (int t=0; t<tabPane.getTabCount(); t++)
			{
				Component comp    = tabPane.getComponentAt(t);
				String    tabName = tabPane.getTitleAt(t);

				if (comp instanceof JTabbedPane)
					refreshTable((JTabbedPane)comp, tm, tabName);
				else if (comp instanceof TabularCntrPanel)
				{
					TabularCntrPanel tcp = (TabularCntrPanel) comp;
					CountersModel    cm  = tcp.getCm();
					
					if (StringUtil.isNullOrBlank(groupName))
						groupName = tcp.getGroupName();

					if (cm != null)
					{
						Vector<Object> row = new Vector<Object>();
						row.setSize(PCS_TAB_HEADER.length);
						
						row.set(PCS_TAB_POS_TIMEOUT,    new Integer(cm.getQueryTimeout()));
						row.set(PCS_TAB_POS_POSTPONE,   new Integer(cm.getPostponeTime()));
						row.set(PCS_TAB_POS_STORE_PCS,  new Boolean(cm.isPersistCountersEnabled()));
						row.set(PCS_TAB_POS_STORE_ABS,  new Boolean(cm.isPersistCountersAbsEnabled()));
						row.set(PCS_TAB_POS_STORE_DIFF, new Boolean(cm.isPersistCountersDiffEnabled()));
						row.set(PCS_TAB_POS_STORE_RATE, new Boolean(cm.isPersistCountersRateEnabled()));
	
						row.set(PCS_TAB_POS_TAB_NAME,   cm.getDisplayName());
						row.set(PCS_TAB_POS_GROUP_NAME, groupName);
						row.set(PCS_TAB_POS_CM_NAME,    cm.getName());
						row.set(PCS_TAB_POS_ICON,       cm.getTabPanel() == null ? null : cm.getTabPanel().getIcon());
						row.set(PCS_TAB_POS_LONG_DESC,  cm.getDescription().replaceAll("\\<.*?\\>", ""));

						tm.addRow(row);
					}
				}
			}
		} // end: refreshTable
	}

	/** PcsTableModel */
	private static class PcsTableModel 
	extends DefaultTableModel
	{
        private static final long serialVersionUID = 1L;

        PcsTableModel()
		{
			super();
			setColumnIdentifiers(PCS_TAB_HEADER);
		}

		@Override
		public Class<?> getColumnClass(int column) 
		{
			if (column == PCS_TAB_POS_TIMEOUT)    return Integer.class;
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
			if (col == PCS_TAB_POS_TIMEOUT)
				return true;

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
				CountersModel cm  = GetCounters.getInstance().getCmByDisplayName(tabName);
				if (cm != null)
				{
					if (col == PCS_TAB_POS_STORE_ABS)  return storePcs && cm.isPersistCountersAbsEditable();
					if (col == PCS_TAB_POS_STORE_DIFF) return storePcs && cm.isPersistCountersDiffEditable();
					if (col == PCS_TAB_POS_STORE_RATE) return storePcs && cm.isPersistCountersRateEditable();
				}
			}
			return false;
		}
	}
	/*---------------------------------------------------
	** END: subclasses for PCS Table
	**---------------------------------------------------
	*/

	
	
	/*---------------------------------------------------
	** BEGIN: helper methods
	**---------------------------------------------------
	*/

//	public void setCounterStorageTabVisible(boolean b)
//	{
//		_showPcsTab = b;
//		_tab.setVisibleAtModel(TAB_POS_PCS, b);
//	}
//	public void setHostmonTabVisible(boolean b)
//	{
//		_showHostmonTab = b;
//		_tab.setVisibleAtModel(TAB_POS_HOSTMON, b);
//	}

	private void toggleCounterStorageTab()
	{
		if (_showPcsTab)
		{
			if (_aseOptionStore_chk.isSelected())
			{
//				_tab.setVisibleAtModel(TAB_POS_PCS, true);
				_tab.setEnabledAt(TAB_POS_PCS, true);
			}
			else
			{
//				_tab.setVisibleAtModel(TAB_POS_PCS, false);
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
//				_tab.setVisibleAtModel(TAB_POS_HOSTMON, true);
				_tab.setEnabledAt(TAB_POS_HOSTMON, true);
			}
			else
			{
//				_tab.setVisibleAtModel(TAB_POS_HOSTMON, false);
				_tab.setEnabledAt(TAB_POS_HOSTMON, false);
			}
		}
	}
	private void validateContents()
	{
		String aseProblem     = "";
		String offlineProblem = "";
		String jdbcProblem    = "";
		String pcsProblem     = "";
		String hostmonProblem = "";
		String otherProblem   = "";

		boolean checkMoreStuff = true;

		if (TAB_TITLE_ASE.equals(_tab.getSelectedTitle(false)))
		{
			checkMoreStuff = true;

			if (_aseUser_txt.getText().trim().equals("")) 
				aseProblem = "ASE User name must be specified";
			
			if (_aseSshTunnel_chk.isSelected() && _aseSshTunnelInfo != null && ! _aseSshTunnelInfo.isValid() )
			{
				String problem = _aseSshTunnelInfo.getInvalidReason();
				aseProblem = "SSH Tunnel is incomplete: " + problem;
			}
		}

		else if (TAB_TITLE_OFFLINE.equals(_tab.getSelectedTitle(false)))
		{
			checkMoreStuff = false;

			String url = _offlineJdbcUrl_cbx.getEditor().getItem().toString();
			// URL
			if ( url.matches(".*<.*>.*") )
				offlineProblem = "Replace the <template> with something.";

			if ( url.matches(".*\\[.*\\].*"))
				offlineProblem = "Replace the [template] with something or delete it.";
		}

		else if (TAB_TITLE_JDBC.equals(_tab.getSelectedTitle(false)))
		{
			checkMoreStuff = false;

//			String url = _jdbcUrl_cbx.getEditor().getItem().toString();
//			// URL
//			if ( url.matches(".*<.*>.*") )
//				jdbcProblem = "Replace the <template> with something.";
//
//			if ( url.matches(".*\\[.*\\].*"))
//				jdbcProblem = "Replace the [template] with something or delete it.";
		}

		if (checkMoreStuff)
		{
			if (_aseOptionStore_chk.isSelected())
			{
				String url = _pcsJdbcUrl_cbx.getEditor().getItem().toString();
				// URL
				if ( url.matches(".*<.*>.*") )
					pcsProblem = "Replace the <template> with something.";
	
				if ( url.matches(".*\\[.*\\].*"))
					pcsProblem = "Replace the [template] with something or delete it.";
	
				// SESSIONS TABLE
				int rows = 0;
				TableModel tm = _pcsSessionTable.getModel();
				for (int r=0; r<tm.getRowCount(); r++)
				{
					if ( ((Boolean)tm.getValueAt(r, PCS_TAB_POS_STORE_PCS)).booleanValue() )
						rows++;
				}
				if (rows == 0 && pcsProblem.equals(""))
					pcsProblem = "Atleast one Performance Counter needs to be checked.";
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
					hostmonProblem = "SSH Port number must be an integer";
				}
	
				if (username.trim().equals(""))
					hostmonProblem = "SSH username must be specified";
	
				if (password.trim().equals(""))
					hostmonProblem = "SSH password must be specified";
	
				if (hostname.trim().equals(""))
					hostmonProblem = "SSH hostname must be specified";
			}
			else
			{
				// host/port fields has to match
				String[] hosts = StringUtil.commaStrToArray(_aseHost_txt.getText());
				String[] ports = StringUtil.commaStrToArray(_asePort_txt.getText());
				if (hosts.length != ports.length)
				{
					otherProblem = "Host has "+hosts.length+" entries, Port has "+ports.length+" entries. They must match";
				}
			}
		}
		
		String sumProblem = otherProblem + aseProblem + offlineProblem + jdbcProblem + pcsProblem + hostmonProblem;
		_ok_lbl.setText(sumProblem);
		_pcsTestConn_lbl.setText(sumProblem);
		_tab.setForegroundAtModel(TAB_POS_HOSTMON, _tab.getForegroundAtModel(TAB_POS_ASE));
		_tab.setForegroundAtModel(TAB_POS_PCS,     _tab.getForegroundAtModel(TAB_POS_ASE));
		_tab.setForegroundAtModel(TAB_POS_OFFLINE, _tab.getForegroundAtModel(TAB_POS_ASE));

		if (sumProblem.equals(""))
		{
			_ok                 .setEnabled(true);
			_offlineTestConn_but.setEnabled(true);
			_jdbcTestConn_but   .setEnabled(true);
			_pcsTestConn_but    .setEnabled(true);
		}
		else
		{
			_ok                 .setEnabled(false);
			_offlineTestConn_but.setEnabled(false);
			_jdbcTestConn_but   .setEnabled(false);
			_pcsTestConn_but    .setEnabled(false);

//			if (_tab.getSelectedIndex() == TAB_POS_OFFLINE)
//			if (TAB_TITLE_OFFLINE.equals(_tab.getSelectedTitle(false)))
//				_tab.setForegroundAtModel(TAB_POS_OFFLINE, Color.RED);
//			else
//				_tab.setForegroundAtModel(TAB_POS_PCS, Color.RED);

			if ( ! hostmonProblem.equals("")) _tab.setForegroundAtModel(TAB_POS_HOSTMON, Color.RED);
			if ( ! pcsProblem    .equals("")) _tab.setForegroundAtModel(TAB_POS_PCS,     Color.RED);
			if ( ! offlineProblem.equals("")) _tab.setForegroundAtModel(TAB_POS_OFFLINE, Color.RED);
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
			if (AseConnectionFactory.setInterfaces(file))
			{
				// On success, reload the servers
				refreshServers();
				_aseIfile_save = file;
				_aseIfile_txt.setText(file);
			}
		}
		catch (Exception e)
		{
			SwingUtils.showErrorMessage(this, "Problems setting new Name Service file", 
				"Problems setting the Name Service file '"+file+"'." +
				"\n\n" + e.getMessage(), e);
			
			// Maybe try to edit the file
			if (e.getMessage().indexOf("unknown format") >= 0)//unknown format
			{
				String str = 
					"<html>" +
					"Do you want to edit the file "+file+"<br>" +
					"<br>" +
					"It looks like it was 'unknown format', which could be fixed if you edit the file.<br>" +
					"</html>";
				int answer = JOptionPane.showConfirmDialog(this, str, "Edit file", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				if ( answer == JOptionPane.YES_OPTION )
				{
					String currentSrvName = _aseServer_cbx.getSelectedItem().toString(); 
	//				String ifile = _aseIfile_txt.getText(); 
					InterfaceFileEditor ife = new InterfaceFileEditor(this, file, currentSrvName);
					int rc = ife.open();
					if (rc == InterfaceFileEditor.OK)
					{
						loadNewInterfaces( file );
						_aseServer_cbx.setSelectedItem(currentSrvName);
					}
					return;
				}
			}
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
			@Override
			public void run()
			{
				if (TAB_TITLE_ASE.equals(_tab.getSelectedTitle(false)))
				{
					if (_aseUser_txt  .getText().trim().equals("")) {_aseUser_txt  .requestFocus(); return; }
					if (_asePasswd_txt.getText().trim().equals("")) {_asePasswd_txt.requestFocus(); return; }
					if (_aseHost_txt  .getText().trim().equals("")) {_aseHost_txt  .requestFocus(); return; }
					if (_asePort_txt  .getText().trim().equals("")) {_asePort_txt  .requestFocus(); return; }
				}
				else if (TAB_TITLE_HOSTMON.equals(_tab.getSelectedTitle(false)))
				{
					if (_hostmonUser_txt  .getText().trim().equals("")) {_hostmonUser_txt  .requestFocus(); return; }
					if (_hostmonPasswd_txt.getText().trim().equals("")) {_hostmonPasswd_txt.requestFocus(); return; }
					if (_hostmonHost_txt  .getText().trim().equals("")) {_hostmonHost_txt  .requestFocus(); return; }
					if (_hostmonPort_txt  .getText().trim().equals("")) {_hostmonPort_txt  .requestFocus(); return; }
				}
				else if (TAB_TITLE_OFFLINE.equals(_tab.getSelectedTitle(false)))
				{
					if (_offlineJdbcUsername_txt.getText().trim().equals("")) {_offlineJdbcUsername_txt.requestFocus(); return; }
					if (_offlineJdbcPassword_txt.getText().trim().equals("")) {_offlineJdbcPassword_txt.requestFocus(); return; }
				}
				else if (TAB_TITLE_JDBC.equals(_tab.getSelectedTitle(false)))
				{
					if (_jdbcUsername_txt.getText().trim().equals("")) {_jdbcUsername_txt.requestFocus(); return; }
					if (_jdbcPassword_txt.getText().trim().equals("")) {_jdbcPassword_txt.requestFocus(); return; }
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
			int currentVersion = mtd.getAseExecutableVersionNum();
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
		String sqlInit = _aseSqlInit_txt.getText();

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
				_aseConn = ConnectionProgressDialog.connectWithProgressDialog(this, AseConnectionFactory.getDriver(), rawUrl, props, _checkAseCfg, _sshConn, _aseSshTunnelInfo, _desiredProductName, sqlInit);
//				_aseConn = AseConnectionFactory.getConnection(AseConnectionFactory.getDriver(), rawUrl, props, null);
				return true;
			}
			catch (SQLException e)
			{
				String msg = AseConnectionUtils.showSqlExceptionMessage(this, "Problems Connecting", "Problems when connecting to the data server.", e); 
				_logger.warn("Problems when connecting to a ASE Server. "+msg);
				return false;
			}
			catch (Exception e)
			{
				SwingUtils.showErrorMessage(this, "Problems Connecting", "Problems when connecting to the data server.\n\n" + e.getMessage(), e);
				_logger.warn("Problems when connecting to a ASE Server. Caught: "+e);
				return false;
			}
			//<<<<<----- RETURN after this
		}

		// set login timeout property
		String loginTimeoutStr = _aseLoginTimeout_txt.getText().trim();
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if ( conf != null && ! StringUtil.isNullOrBlank(loginTimeoutStr) )
		{
			conf.setProperty(AseConnectionFactory.PROPERTY_LOGINTIMEOUT, loginTimeoutStr);
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

		// reset the sshTunnel info if not selected
		if ( ! _aseSshTunnel_chk.isSelected() )
			_aseSshTunnelInfo = null;

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

			String urlStr = AseConnectionFactory.getUrlTemplateBase() + AseConnectionFactory.getHostPortStr();
//			String urlStr = "jdbc:sybase:Tds:" + AseConnectionFactory.getHostPortStr();
			_aseConn = ConnectionProgressDialog.connectWithProgressDialog(this, urlStr, _checkAseCfg, _sshConn, _aseSshTunnelInfo, _desiredProductName, sqlInit);
//			_aseConn = ConnectionProgressDialog.connectWithProgressDialog(this, urlStr, _checkAseCfg, _sshConn, _desiredProductName);
//			_aseConn = AseConnectionFactory.getConnection();
//String TDS_SSH_TUNNEL_CONNECTION  = _aseConn.getClientInfo("TDS_SSH_TUNNEL_CONNECTION");
//String TDS_SSH_TUNNEL_INFORMATION = _aseConn.getClientInfo("TDS_SSH_TUNNEL_INFORMATION");
//System.out.println("TDS_SSH_TUNNEL_CONNECTION="+TDS_SSH_TUNNEL_CONNECTION);
//System.out.println("TDS_SSH_TUNNEL_INFORMATION="+TDS_SSH_TUNNEL_INFORMATION);
			return true;
		}
		catch (SQLException e)
		{
			// The below shows a showErrorMessage
			String msg = AseConnectionUtils.showSqlExceptionMessage(this, "Problems Connecting", "Problems when connecting to the data server.", e); 
			_logger.warn("Problems when connecting to a ASE Server. "+msg);
			return false;
		}
		catch (Exception e)
		{
			_logger.warn("Problems when connecting to a ASE Server. Caught: "+e);
			SwingUtils.showErrorMessage(this, "Problems Connecting", 
					"Problems when connecting to the data server." +
					"\n\n" + e.getMessage(), e);
			return false;
		}
	}
	
//	@SuppressWarnings("unused")
//	private boolean checkForMonitorOptions()
//	{
////System.out.println(">>>>> checkForMonitorOptions");
//		MainFrame.setStatus(MainFrame.ST_STATUS_FIELD, "Checking for Monitor Options...");
//
//		boolean b = AseConnectionUtils.checkForMonitorOptions(_aseConn, _aseUser_txt.getText(), true, this, "enable monitoring");
//
//		MainFrame.setStatus(MainFrame.ST_CONNECT);
////System.out.println("<<<<< checkForMonitorOptions");
//		return b;
//	}

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
			pcsProps.put(PersistentCounterHandler.PROPKEY_WriterClass, pcsAll);

			// pcsAll "could" be a ',' separated string
			// But I dont know how to set the properties for those Writers
			String[] pcsa = pcsAll.split(",");
			for (int i=0; i<pcsa.length; i++)
			{
				String pcs = pcsa[i].trim();
				
				if (pcs.equals("com.asetune.pcs.PersistWriterJdbc"))
				{
					pcsProps.put(PersistWriterJdbc.PROP_jdbcDriver,   _pcsJdbcDriver_cbx  .getEditor().getItem().toString());
					pcsProps.put(PersistWriterJdbc.PROP_jdbcUrl,      _pcsJdbcUrl_cbx     .getEditor().getItem().toString());
					pcsProps.put(PersistWriterJdbc.PROP_jdbcUsername, _pcsJdbcUsername_txt.getText());
					pcsProps.put(PersistWriterJdbc.PROP_jdbcPassword, _pcsJdbcPassword_txt.getText());

					pcsProps.put(PersistWriterJdbc.PROP_startH2NetworkServer, _pcsH2Option_startH2NetworkServer_chk.isSelected() + "");

					// DDL
					pcsProps.put(PersistentCounterHandler.PROPKEY_ddl_doDdlLookupAndStore,             _pcsDdl_doDdlLookupAndStore_chk            .isSelected() + "");
					pcsProps.put(PersistentCounterHandler.PROPKEY_ddl_addDependantObjectsToDdlInQueue, _pcsDdl_addDependantObjectsToDdlInQueue_chk.isSelected() + "");
					pcsProps.put(PersistentCounterHandler.PROPKEY_ddl_afterDdlLookupSleepTimeInMs,     _pcsDdl_afterDdlLookupSleepTimeInMs_txt    .getText());
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

		//-----------------------------------------------------
		// IF Jdbc driver: H2
		//-----------------------------------------------------
		if ( jdbcDriver.equals("org.h2.Driver") )
		{
			// start the H2 TCP Server
			if (startH2NetworkServer)
			{
				try
				{
					_logger.info("Starting a H2 TCP server.");
					org.h2.tools.Server h2ServerTcp = org.h2.tools.Server.createTcpServer("-tcpAllowOthers");
					h2ServerTcp.start();
		
		//			_logger.info("H2 TCP server, listening on port='"+h2Server.getPort()+"', url='"+h2Server.getURL()+"', service='"+h2Server.getService()+"'.");
					_logger.info("H2 TCP server, url='"+h2ServerTcp.getURL()+"', service='"+h2ServerTcp.getService()+"'.");
		
					if (true)
					{
						try
						{
							_logger.info("Starting a H2 WEB server.");
							//String[] argsWeb = new String[] { "-trace" };
							org.h2.tools.Server h2ServerWeb = org.h2.tools.Server.createWebServer();
							h2ServerWeb.start();

							_logger.info("H2 WEB server, url='"+h2ServerWeb.getURL()+"', service='"+h2ServerWeb.getService()+"'.");
						}
						catch (Exception e)
						{
							_logger.info("H2 WEB server, failed to start, but I will continue anyway... Caught: "+e);
						}
					}

					if (true)
					{
						try
						{
							_logger.info("Starting a H2 Postgres server.");
							org.h2.tools.Server h2ServerPostgres = org.h2.tools.Server.createPgServer("-pgAllowOthers");
							h2ServerPostgres.start();
			
							_logger.info("H2 Postgres server, url='"+h2ServerPostgres.getURL()+"', service='"+h2ServerPostgres.getService()+"'.");
						}
						catch (Exception e)
						{
							_logger.info("H2 Postgres server, failed to start, but I will continue anyway... Caught: "+e);
						}
					}
				}
				catch (SQLException e) 
				{
					_logger.warn("Problem starting H2 network service", e);
				}
			}

			//-----------------------------------------------------
			// IF H2, add hard coded stuff to URL
			//-----------------------------------------------------
			H2UrlHelper urlHelper = new H2UrlHelper(jdbcUrl);
			Map<String, String> urlMap = urlHelper.getUrlOptionsMap();
			if (urlMap == null)
				urlMap = new LinkedHashMap<String, String>();

			boolean change = false;

			// Database short names are converted to uppercase for the DATABASE() function, 
			// and in the CATALOG column of all database meta data methods. 
			// Setting this to "false" is experimental. 
			// When set to false, all identifier names (table names, column names) are case 
			// sensitive (except aggregate, built-in functions, data types, and keywords).
			if ( ! urlMap.containsKey("DATABASE_TO_UPPER") )
			{
				change = true;
				_logger.info("H2 URL add option: DATABASE_TO_UPPER=false");
				urlMap.put("DATABASE_TO_UPPER", "false");
			}

//			// The maximum time in milliseconds used to compact a database when closing.
//			if ( ! urlMap.containsKey("MAX_COMPACT_TIME") )
//			{
//				change = true;
//				_logger.info("H2 URL add option: MAX_COMPACT_TIME=2000");
//				urlMap.put("MAX_COMPACT_TIME",  "2000");
//			}

			// AutoServer mode
			if ( ! urlMap.containsKey("AUTO_SERVER") )
			{
				change = true;
				_logger.info("H2 URL add option: AUTO_SERVER=TRUE");
				urlMap.put("AUTO_SERVER",  "TRUE");
			}

//			// DATABASE_EVENT_LISTENER
//			if ( ! urlMap.containsKey("DATABASE_EVENT_LISTENER") )
//			{
//				change = true;
//				_logger.info("H2 URL add option: DATABASE_EVENT_LISTENER="+H2DatabaseEventListener.class.getName());
//				urlMap.put("DATABASE_EVENT_LISTENER",  H2DatabaseEventListener.class.getName());
//			}

			if (change)
			{
				urlHelper.setUrlOptionsMap(urlMap);
				jdbcUrl = urlHelper.getUrl();
				
				_logger.info("Added some options to the H2 URL. New URL is '"+jdbcUrl+"'.");
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
		
		// Get some info about what we connected to.
    	DatabaseMetaData dbmd = null;
    	try	{ dbmd = _offlineConn.getMetaData(); } catch (Throwable ignore) {}
		if (dbmd != null)
		{
			String getDriverName             = "-";
			String getDriverVersion          = "-";
			int    getDriverMajorVersion     = -1;
			int    getDriverMinorVersion     = -1;
			int    getJDBCMajorVersion       = -1;
			int    getJDBCMinorVersion       = -1;

			String getDatabaseProductName    = "-";
			String getDatabaseProductVersion = "-";
			int    getDatabaseMajorVersion   = -1;
			int    getDatabaseMinorVersion   = -1;

			try	{ getDriverName             = dbmd.getDriverName();             } catch (Throwable ignore) {}
			try	{ getDriverVersion          = dbmd.getDriverVersion();          } catch (Throwable ignore) {}
			try	{ getDriverMajorVersion     = dbmd.getDriverMajorVersion();     } catch (Throwable ignore) {}
			try	{ getDriverMinorVersion     = dbmd.getDriverMinorVersion();     } catch (Throwable ignore) {}
			try	{ getJDBCMajorVersion       = dbmd.getJDBCMajorVersion();       } catch (Throwable ignore) {}
			try	{ getJDBCMinorVersion       = dbmd.getJDBCMinorVersion();       } catch (Throwable ignore) {}

			try	{ getDatabaseProductName    = dbmd.getDatabaseProductName();    } catch (Throwable ignore) {}
			try	{ getDatabaseProductVersion = dbmd.getDatabaseProductVersion(); } catch (Throwable ignore) {}
			try	{ getDatabaseMajorVersion   = dbmd.getDatabaseMajorVersion();   } catch (Throwable ignore) {}
			try	{ getDatabaseMinorVersion   = dbmd.getDatabaseMinorVersion();   } catch (Throwable ignore) {}

			_logger.info("Connected using JDBC driver Name='"+getDriverName
					+"', Version='"         +getDriverVersion
					+"', MajorVersion='"    +getDriverMajorVersion
					+"', MinorVersion='"    +getDriverMinorVersion
					+"', JdbcMajorVersion='"+getJDBCMajorVersion
					+"', JdbcMinorVersion='"+getJDBCMinorVersion
					+"'.");
			_logger.info("Connected to Database Product Name='"+getDatabaseProductName
					+"', Version='"     +getDatabaseProductVersion
					+"', MajorVersion='"+getDatabaseMajorVersion
					+"', MinorVersion='"+getDatabaseMinorVersion
					+"'.");

			// if H2
			// Set some specific stuff
			if ( DbUtils.DB_PROD_NAME_H2.equals(getDatabaseProductName) )
			{
				String dbProdName = getDatabaseProductName;

				_logger.info("Do H2 Specific settings for the database.");

				// Sets the size of the cache in KB (each KB being 1024 bytes) for the current database. 
				// The default value is 16384 (16 MB). The value is rounded to the next higher power 
				// of two. Depending on the virtual machine, the actual memory required may be higher.
				//dbExecSetting(_offlineConn, dbProdName, "SET CACHE_SIZE int");

				// Sets the compression algorithm for BLOB and CLOB data. Compression is usually slower, 
				// but needs less disk space. LZF is faster but uses more space.
				// Admin rights are required to execute this command, as it affects all connections. 
				// This command commits an open transaction. This setting is persistent.
				// SET COMPRESS_LOB { NO | LZF | DEFLATE }
				//dbExecSetting(_offlineConn, dbProdName, "SET COMPRESS_LOB DEFLATE");

				// Sets the default lock timeout (in milliseconds) in this database that is used for 
				// the new sessions. The default value for this setting is 1000 (one second).
				// Admin rights are required to execute this command, as it affects all connections. 
				// This command commits an open transaction. This setting is persistent.
				// SET DEFAULT LOCK_TIMEOUT int
				dbExecSetting(_offlineConn, dbProdName, "SET DEFAULT_LOCK_TIMEOUT 30000");

				// If IGNORECASE is enabled, text columns in newly created tables will be 
				// case-insensitive. Already existing tables are not affected. 
				// The effect of case-insensitive columns is similar to using a collation with 
				// strength PRIMARY. Case-insensitive columns are compared faster than when 
				// using a collation. String literals and parameters are however still considered 
				// case sensitive even if this option is set.
				// Admin rights are required to execute this command, as it affects all connections. 
				// This command commits an open transaction. This setting is persistent. 
				// This setting can be appended to the database URL: jdbc:h2:test;IGNORECASE=TRUE
				// SET IGNORECASE { TRUE | FALSE }
				//dbExecSetting(_offlineConn, dbProdName, "SET IGNORECASE TRUE");

				// Sets the transaction log mode. The values 0, 1, and 2 are supported, 
				// the default is 2. This setting affects all connections.
				// LOG 0 means the transaction log is disabled completely. It is the fastest mode, 
				//       but also the most dangerous: if the process is killed while the database is open
				//       in this mode, the data might be lost. It must only be used if this is not a 
				//       problem, for example when initially loading a database, or when running tests.
				// LOG 1 means the transaction log is enabled, but FileDescriptor.sync is disabled. 
				//       This setting is about half as fast as with LOG 0. This setting is useful if 
				//       no protection against power failure is required, but the data must be protected 
				//       against killing the process.
				// LOG 2 (the default) means the transaction log is enabled, and FileDescriptor.sync 
				//       is called for each checkpoint. This setting is about half as fast as LOG 1. 
				//       Depending on the file system, this will also protect against power failure 
				//       in the majority if cases.
				// Admin rights are required to execute this command, as it affects all connections. 
				// This command commits an open transaction. This setting is not persistent. 
				// This setting can be appended to the database URL: jdbc:h2:test;LOG=0
				// SET LOG int
				//dbExecSetting(_offlineConn, dbProdName, "SET LOG 1");

				// Sets the maximum size of an in-place LOB object. LOB objects larger that this size 
				// are stored in a separate file, otherwise stored directly in the database (in-place). 
				// The default max size is 1024. This setting has no effect for in-memory databases.
				// Admin rights are required to execute this command, as it affects all connections. 
				// This command commits an open transaction. This setting is persistent.
				// SET MAX_LENGTH_INPLACE_LOB int
				//dbExecSetting(_offlineConn, dbProdName, "SET MAX_LENGTH_INPLACE_LOB 4096");

				// Set the query timeout of the current session to the given value. The timeout is 
				// in milliseconds. All kinds of statements will throw an exception if they take 
				// longer than the given value. The default timeout is 0, meaning no timeout.
				// This command does not commit a transaction, and rollback does not affect it.
				// SET QUERY_TIMEOUT int
				dbExecSetting(_offlineConn, dbProdName, "SET QUERY_TIMEOUT 30000");

				// Sets the trace level for file the file or system out stream. Levels are: 0=off, 
				// 1=error, 2=info, 3=debug. The default level is 1 for file and 0 for system out. 
				// To use SLF4J, append ;TRACE_LEVEL_FILE=4 to the database URL when opening the database.
				// This setting is not persistent. Admin rights are required to execute this command, 
				// as it affects all connections. This command does not commit a transaction, 
				// and rollback does not affect it. This setting can be appended to the 
				// database URL: jdbc:h2:test;TRACE_LEVEL_SYSTEM_OUT=3
				// SET { TRACE_LEVEL_FILE | TRACE_LEVEL_SYSTEM_OUT } int
				//dbExecSetting("");

				// 
				//dbExec("");
			}
		}
		
		//
		// FIXME: open a dialog where we show what information is in the offline database store
//		new OfflineSessionVeiwer(_owner);
//		new OfflineSessionVeiwer(null, _offlineConn, true);

		return _offlineConn != null;
	}
	private boolean dbExecSetting(Connection conn, String dbProductName, String sql)
	{
		_logger.info(dbProductName+": "+sql);
		try
		{
			return dbExec(conn, sql, true);
		}
		catch (SQLException ignore)
		{
			return false;
		}
	}

	private boolean dbExec(Connection conn, String sql, boolean printErrors)
	throws SQLException
	{
		if (_logger.isDebugEnabled())
		{
			_logger.debug("SEND SQL: " + sql);
		}

		try
		{
			Statement s = conn.createStatement();
			s.execute(sql);
			s.close();
		}
		catch(SQLException e)
		{
			_logger.warn("Problems when executing sql statement: "+sql);
			throw e;
		}

		return true;
	}


	/**
	 * Test to connect
	 */
	private boolean jdbcConnect()
	{
		String jdbcDriver = _jdbcDriver_cbx.getEditor().getItem().toString();
		String jdbcUrl    = _jdbcUrl_cbx.getEditor().getItem().toString();
		String jdbcUser   = _jdbcUsername_txt.getText();
		String jdbcPasswd = _jdbcPassword_txt.getText();

		_jdbcConn = jdbcConnect(Version.getAppName(), 
				jdbcDriver, 
				jdbcUrl,
				jdbcUser, 
				jdbcPasswd);
		
		return _jdbcConn != null;
	}

//	/**
//	 * Test to connect
//	 */
//	private Connection jdbcConnect(String appname, String driver, String url, String user, String passwd)
//	{
//		try
//		{
//			Class.forName(driver).newInstance();
//			Properties props = new Properties();
//			props.put("user", user);
//			props.put("password", passwd);
//	
//			_logger.debug("getConnection to driver='"+driver+"', url='"+url+"', user='"+user+"'.");
//			Connection conn = DriverManager.getConnection(url, props);
//	
//			return conn;
//		}
//		catch (SQLException e)
//		{
//			StringBuffer sb = new StringBuffer();
//			while (e != null)
//			{
//				sb.append( "\n" );
//				sb.append( e.getMessage() );
//				e = e.getNextException();
//			}
////			JOptionPane.showMessageDialog(this, "Connection FAILED.\n\n"+sb.toString(), Version.getAppName()+" - jdbc connect", JOptionPane.ERROR_MESSAGE);
//			SwingUtils.showErrorMessage(Version.getAppName()+" - jdbc connect", "Connection FAILED.\n\n"+sb.toString(), e);
//		}
//		catch (Exception e)
//		{
////			JOptionPane.showMessageDialog(this, "Connection FAILED.\n\n"+e.toString(),  Version.getAppName()+" - jdbc connect", JOptionPane.ERROR_MESSAGE);
//			SwingUtils.showErrorMessage(Version.getAppName()+" - jdbc connect", "Connection FAILED.\n\n"+e.toString(), e);
//		}
//		return null;
//	}
	/**
	 * Test to connect
	 */
	private Connection jdbcConnect(final String appname, final String driver, final String url, final String user, final String passwd)
	{
		WaitForExecDialog wait = new WaitForExecDialog(this, "JDBC Connect...");
		BgExecutor doWork = new BgExecutor(wait)
		{
			@Override
			public Object doWork()
			{
				try
				{
					// If no suitable driver can be found for the URL, to to load it "the old fashion way" (hopefully it's in the classpath)
					try
					{
						Driver jdbcDriver = DriverManager.getDriver(url);
						if (jdbcDriver == null)
							Class.forName(driver).newInstance();
					}
					catch (Exception ex)
					{
						_logger.warn("Can't load JDBC driver for URL='"+url+"' using 'old way od doing it' using: DriverManager.getDriver(url); Lets continue and try just to use DriverManager.getConnection(url, props); which is the 'new' way of doing it. Caught="+ex);
						_logger.debug("Can't load JDBC driver for URL='"+url+"' using 'old way od doing it' using: DriverManager.getDriver(url); Lets continue and try just to use DriverManager.getConnection(url, props); which is the 'new' way of doing it. Caught="+ex, ex);
					}

//					Class.forName(driver).newInstance();
//					JdbcDriverHelper.newDriverInstance(driver);

					Properties props = new Properties();
					props.put("user", user);
					props.put("password", passwd);

					_logger.debug("getConnection to driver='"+driver+"', url='"+url+"', user='"+user+"'.");
					getWaitDialog().setState(
							"<html>" +
							"Driver: "+ driver + "<br>" +
							"URL: "   + url    + "<br>" +
							"User: "  + user   + "<br>" +
							"</html>");
					SwingUtils.setWindowMinSize(getWaitDialog());

					Connection conn = DriverManager.getConnection(url, props);
					
					// Execute any SQL Init 
					String sqlInit = _jdbcSqlInit_txt.getText();
					if (StringUtil.hasValue(sqlInit))
					{
						try
						{
							String[] sa =  sqlInit.split(";");
							for (String sql : sa)
							{
								sql = sql.trim();
								if ("".equals(sql))
									continue;
								getWaitDialog().setState(
										"<html>" +
										"SQL Init: "+ sql + "<br>" +
										"</html>");
								DbUtils.exec(conn, sql);
							}
						}
						catch (SQLException ex)
						{
							SwingUtils.showErrorMessage(ConnectionDialog.this, "SQL Initialization Failed", 
									"<html>" +
									"<h2>SQL Initialization Failed</h2>" +
									"Full SQL Init String '"+ sqlInit + "'<br>" +
									"<br>" +
									"<b>SQL State:     </b>" + ex.getSQLState()  + "<br>" +
									"<b>Error number:  </b>" + ex.getErrorCode() + "<br>" +
									"<b>Error Message: </b>" + ex.getMessage()   + "<br>" +
									"</html>",
									ex);
							throw ex;
						}
					}

					return conn;
				}
				catch (SQLException ex)
				{
					SQLException eTmp = ex;
					StringBuffer sb = new StringBuffer();
					while (eTmp != null)
					{
						sb.append( "\n" );
						sb.append( "ex.toString='").append( ex.toString()       ).append("', ");
						sb.append( "Driver='"     ).append( driver              ).append("', ");
						sb.append( "URL='"        ).append( url                 ).append("', ");
						sb.append( "User='"       ).append( user                ).append("', ");
						sb.append( "SQLState='"   ).append( eTmp.getSQLState()  ).append("', ");
						sb.append( "ErrorCode="   ).append( eTmp.getErrorCode() ).append(", ");
						sb.append( "Message='"    ).append( eTmp.getMessage()   ).append("', ");
						sb.append( "classpath='"  ).append( System.getProperty("java.class.path") ).append("'.");
						eTmp = eTmp.getNextException();
					}
					_logger.info(Version.getAppName()+" - JDBC connect FAILED (catch SQLException) Caught: "+sb.toString());
					setException(ex);
				}
				catch (Exception ex)
				{
					_logger.info(Version.getAppName()+" - JDBC connect FAILED (catch Exception) Caught: "+ex);
					setException(ex);
				}
				return null;
			}
		};
		Connection conn = (Connection) wait.execAndWait(doWork, 100);

		if (doWork.hasException())
		{
			Throwable t = doWork.getException();
			if (t instanceof SQLException)
			{
				SQLException e = (SQLException) t;
				StringBuffer sb = new StringBuffer();
				sb.append("<html>");
				sb.append("<h2>Problems During Connect (SQLException)</h2>");
				boolean loadDriverProblem = false;
				while (e != null)
				{
					if (e.getMessage().indexOf("No suitable driver") >= 0)
						loadDriverProblem = true;

					sb.append( "<table border=0 cellspacing=1 cellpadding=1>" );
					sb.append( "<tr> <td><b>Message    </b></td> <td nowrap>").append( e.getMessage()   ).append("</td> </tr>");
					sb.append( "<tr> <td><b>SQLState   </b></td> <td nowrap>").append( e.getSQLState()  ).append("</td> </tr>");
					sb.append( "<tr> <td><b>ErrorCode  </b></td> <td nowrap>").append( e.getErrorCode() ).append("</td> </tr>");
					sb.append( "<tr> <td><b>Driver     </b></td> <td nowrap>").append( driver           ).append("</td> </tr>");
					sb.append( "<tr> <td><b>URL        </b></td> <td nowrap>").append( url              ).append("</td> </tr>");
					sb.append( "<tr> <td><b>User       </b></td> <td nowrap>").append( user             ).append("</td> </tr>");
					sb.append( "<tr> <td><b>classpath  </b></td> <td nowrap>").append( System.getProperty("java.class.path") ).append("</td> </tr>");
					sb.append( "</table>" );
					e = e.getNextException();
				}
				if (loadDriverProblem)
				{
						sb.append("<h2>An error occurred while establishing the connection: </h2>");
						sb.append("The selected Driver cannot handle the specified Database URL. <br>");
						sb.append("The most common reason for this error is that the database <b>URL contains a syntax error</b> preventing the driver from accepting it. <br>");
						sb.append("The error also occurs when trying to connect to a database with the wrong driver. Correct this and try again.");
				}
				sb.append("</html>");
//				SwingUtils.showErrorMessage(Version.getAppName()+" - jdbc connect", "Connection (SQLException) FAILED.\n\n"+sb.toString(), e);
				SwingUtils.showErrorMessage(Version.getAppName()+" - jdbc connect", sb.toString(), e);
			}
			else if (t instanceof Exception)
			{
				SwingUtils.showErrorMessage(Version.getAppName()+" - jdbc connect", "Connection (Exception) FAILED.\n\n"+t.toString(), t);
			}
			else
			{
				SwingUtils.showErrorMessage(Version.getAppName()+" - jdbc connect", "Connection (other) FAILED.\n\n"+t.toString(), t);
			}
		}

		return conn;
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
//			JOptionPane.showMessageDialog(this, "Connection FAILED.\n\n"+sb.toString(), Version.getAppName()+" - connect check", JOptionPane.ERROR_MESSAGE);
			SwingUtils.showErrorMessage(Version.getAppName()+" - connect check", "Connection FAILED.\n\n"+sb.toString(), e);
		}
		catch (Exception e)
		{
//			JOptionPane.showMessageDialog(this, "Connection FAILED.\n\n"+e.toString(),  Version.getAppName()+" - connect check", JOptionPane.ERROR_MESSAGE);
			SwingUtils.showErrorMessage(Version.getAppName()+" - connect check", "Connection FAILED.\n\n"+e.toString(), e);
		}
		return false;
	}
	/*---------------------------------------------------
	** END: helper methods
	**---------------------------------------------------
	*/

	
	
	
	
	/*---------------------------------------------------
	** BEGIN: implementing TableModelListener, ActionListener, KeyListeners, FocusListener
	**---------------------------------------------------
	*/
	@Override
	public void tableChanged(TableModelEvent e)
	{
		// This wasnt kicked off for a table change...
		validateContents();
	}

	@Override
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
			// if sybase cant be found, set starting directory to AseTune Save Directory
			if (dir == null)
			{
				if (System.getProperty("ASETUNE_SAVE_DIR") != null)
					dir = System.getProperty("ASETUNE_SAVE_DIR");
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
			InterfaceFileEditor ife = new InterfaceFileEditor(this, ifile, currentSrvName);
			int rc = ife.open();
//			if (rc == InterfaceFileEditor.OK)
			if (rc != 999) // always do this
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

		// --- ASE: CHECKBOX: USE SSH TUNNEL
		if (_aseSshTunnel_chk.equals(source))
		{
			String hostPortStr = AseConnectionFactory.toHostPortStr(_aseHost_txt.getText(), _asePort_txt.getText());
			_aseSshTunnelInfo = SshTunnelDialog.getSshTunnelInfo(hostPortStr);
			updateSshTunnelDescription();
			
			validateContents(); // the ok_lable, seems to be fuckedup if not do this
			SwingUtils.setWindowMinSize(this);
		}
		
		// --- ASE: BUTTON: "SSH Tunnel"
		if (_aseSshTunnel_but.equals(source))
		{
			String hostPortStr = AseConnectionFactory.toHostPortStr(_aseHost_txt.getText(), _asePort_txt.getText());

			SshTunnelDialog dialog = new SshTunnelDialog(this, hostPortStr);

			dialog.setVisible(true);
			
//			_aseSshTunnelInfo = dialog.getSshTunnelInfo();
			_aseSshTunnelInfo = SshTunnelDialog.getSshTunnelInfo(hostPortStr);
			updateSshTunnelDescription();
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

		// --- ASE: CHECKBOX: Deferred Connect ---
		if (_aseDeferredConnect_chk.equals(source))
		{
			aseDeferredConnectChkAction();
		}

		// --- ASE: CHECKBOX: Deferred DisConnect ---
		if (_aseDeferredDisConnect_chk.equals(source))
		{
			aseDeferredDisConnectChkAction();
		}

		// --- PCS: COMBOBOX: JDBC DRIVER ---
		if (_pcsJdbcDriver_cbx.equals(source))
		{
//			String jdbcDriver = _pcsJdbcDriver_cbx.getEditor().getItem().toString();
			String jdbcDriver = _pcsJdbcDriver_cbx.getSelectedItem().toString();

			if ("org.h2.Driver".equals(jdbcDriver))
				_pcsH2Option_startH2NetworkServer_chk.setVisible(true);
			else
				_pcsH2Option_startH2NetworkServer_chk.setVisible(false);
			
			// add templates
			List<String> urlTemplates = JdbcDriverHelper.getUrlTemplateList(jdbcDriver);
			if (urlTemplates != null && urlTemplates.size() > 0)
			{
				_pcsJdbcUrl_cbx.removeAllItems();
				for (String template : urlTemplates)
					_pcsJdbcUrl_cbx.addItem(template);
			}
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

		// --- PCS: BUTTON: "Open " 
		if (_pcsOpenTcpConfigDialog_but.equals(source))
		{
			boolean madeChanges = TcpConfigDialog.showDialog(this);
			if (madeChanges)
				_pcsSessionTable.refreshTable();
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

		// --- PCS DDL Lookup: Sleep Time 
		if (_pcsDdl_afterDdlLookupSleepTimeInMs_txt.equals(source))
		{
			String str = _pcsDdl_afterDdlLookupSleepTimeInMs_txt.getText();
			try { Integer.parseInt(str); }
			catch (Exception nfe)
			{
				SwingUtils.showErrorMessage("Not a number", 
					"<html>" +
						"Sleep Time must be a number, currently it is '"+str+"'.<br>" +
						"Resetting to default value: " + PersistentCounterHandler.DEFAULT_ddl_afterDdlLookupSleepTimeInMs +
					"</html>", nfe);
				_pcsDdl_afterDdlLookupSleepTimeInMs_txt.setText(""+PersistentCounterHandler.DEFAULT_ddl_afterDdlLookupSleepTimeInMs);
			}
		}
		
		// --- OFFLINE: COMBOBOX: JDBC DRIVER ---
		if (_offlineJdbcDriver_cbx.equals(source))
		{
//			String jdbcDriver = _offlineJdbcDriver_cbx.getEditor().getItem().toString();
			String jdbcDriver = _offlineJdbcDriver_cbx.getSelectedItem().toString();
			
			if ("org.h2.Driver".equals(jdbcDriver))
				_offlineH2Option_startH2NwSrv_chk.setVisible(true);
			else
				_offlineH2Option_startH2NwSrv_chk.setVisible(false);

			// Get templates
			List<String> urlTemplates = JdbcDriverHelper.getUrlTemplateList(jdbcDriver);
			if (urlTemplates != null && urlTemplates.size() > 0)
			{
				_offlineJdbcUrl_cbx.removeAllItems();
				for (String template : urlTemplates)
					_offlineJdbcUrl_cbx.addItem(template);
			}
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

		// --- JDBC COMBOBOX: JDBC DRIVER ---
		if (_jdbcDriver_cbx.equals(source))
		{
//			String jdbcDriver = _jdbcDriver_cbx.getEditor().getItem().toString();
			String jdbcDriver = _jdbcDriver_cbx.getSelectedItem().toString();

			// Get templates
			List<String> urlTemplates = JdbcDriverHelper.getUrlTemplateList(jdbcDriver);
			if (urlTemplates != null && urlTemplates.size() > 0)
			{
				_jdbcUrl_cbx.removeAllItems();
				for (String template : urlTemplates)
					_jdbcUrl_cbx.addItem(template);
			}
		}
		
		// --- JDBC: BUTTON: "..." 
		if (_jdbcUrl_but.equals(source))
		{
		//	"jdbc:h2:file:[<path>]<dbname>;IFEXISTS=TRUE";
		//	"jdbc:h2:zip:<zipFileName>!/<dbname>";
		
			String url  = _jdbcUrl_cbx.getEditor().getItem().toString();
		
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
		
					_jdbcUrl_cbx.getEditor().setItem(newUrl);
				}
			}
			else
			{
				JFileChooser fc = new JFileChooser();
		
				int returnVal = fc.showOpenDialog(this);
				if(returnVal == JFileChooser.APPROVE_OPTION) 
				{
					String newFile = fc.getSelectedFile().getAbsolutePath().replace('\\', '/');
					String newUrl = _jdbcUrl_cbx.getEditor().getItem() + newFile;

					_jdbcUrl_cbx.getEditor().setItem(newUrl);
				}
			}
		}
		
		// --- JDBC: PASSWORD ---
		if (_jdbcPassword_txt.equals(source))
		{
			if (_jdbcUsername_txt.getText().trim().equals("") )
				setFocus();
			else
				_ok.doClick();
		}
		
		// --- JDBC: BUTTON: JDBC TEST CONNECTION ---
		if (_jdbcTestConn_but.equals(source))
		{
			testJdbcConnection("testConnect", 
					_jdbcDriver_cbx.getEditor().getItem().toString(), 
					_jdbcUrl_cbx.getEditor().getItem().toString(),
					_jdbcUsername_txt.getText(), 
					_jdbcPassword_txt.getText());
		}

		// --- ASE: jConnect Options BUTTON: "..."  (open dialig to choose available options)
		if (_jdbcUrlOptions_but.equals(source))
		{
			Map<String,String> sendOpt = StringUtil.parseCommaStrToMap(_jdbcUrlOptions_txt.getText());
			
			Map<String,String> outOpt = JdbcOptionsDialog.showDialog(this, (String)_jdbcDriver_cbx.getSelectedItem(), (String)_jdbcUrl_cbx.getSelectedItem(), sendOpt);
			// null == CANCEL
			if (outOpt != null)
				_jdbcUrlOptions_txt.setText(StringUtil.toCommaStr(outOpt));
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
				 PersistentCounterHandler.getInstance().stop(true, 0);
				 PersistentCounterHandler.setInstance(null);
			}
			if ( _offlineConn != null )
			{
				try { _offlineConn.close(); }
				catch (SQLException ignore) {}
			}
			if ( _jdbcConn != null )
			{
				try { _jdbcConn.close(); }
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
			if (TAB_TITLE_OFFLINE.equals(_tab.getSelectedTitle(false)))
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
			// JDBC CONNECT
			else if (TAB_TITLE_JDBC.equals(_tab.getSelectedTitle(false)))
			{
				// CONNECT to the JDBC, if it fails, we stay in the dialog
				if ( _jdbcConn == null )
				{
					if ( ! jdbcConnect() )
						return;
				}

				// SET CONNECTION TYP and "CLOSE" the dialog
				_connectionType = JDBC_CONN;
				setVisible(false);
			}
			// ASE & PCS CONNECT
			else
			{
				boolean recordThisSession = _aseOptionStore_chk.isSelected();
				
				// Double check if we want to RECORD the session
				// We might have pressed OK and RECORD has been checked, but it was the previous session.
				if (recordThisSession)
				{
					String msgHtml = 
						"<html>" +
						   "<h2>Do you want to record this session?</h2>" +
						   "Your intentions might be: to just view Performance Counters without Recording them in a database (the Persistent Counter Storage)<br>" +
						   "Meaning you have just forgot to <b>disable</b> the Recording for this specific session...<br>" +
						   "<ul>" +
						   "  <li><b>Continue</b> the connection and <b>Record</b> the Performance Counters.</li>" +
						   "  <li><b>Continue</b> the connection <b>without recoding</b> the data, just view the Performance Counters.</li>" +
						   "  <li><b>Cancel</b> and <b>return</b> to the Connection Dialog.</li>" +
						   "</ul>" +
						"</html>";

					Object[] options = {
							"Record Session",
							"Do NOT Record",
							"Cancel"
							};
					int answer = JOptionPane.showOptionDialog(this, 
						msgHtml,
						"Record this Performance Session?", // title
						JOptionPane.YES_NO_CANCEL_OPTION,
						JOptionPane.QUESTION_MESSAGE,
						null,     //do not use a custom Icon
						options,  //the titles of buttons
						options[0]); //default button title

					if      (answer == 0) recordThisSession = true;
					else if (answer == 1) recordThisSession = false;
					else                  return;
				}

				// Should we WAIT to make the connection. DEFERRED or CONNECT LATER
				if (_aseDeferredConnect_chk.isSelected())
				{
					final String hhmm = aseDeferredConnectChkAction();
					if (hhmm != null)
					{
						Date startTime = null;
						try { startTime = PersistWriterBase.getRecordingStartTime(hhmm); }
						catch(Exception ignore) { }

						if (startTime != null)
						{
							// Create a Waitfor Dialog
							WaitForExecDialog wait = new WaitForExecDialog(this, "Waiting for a Deferred Connect, at "+startTime);

							// Create the Executor object
							WaitForExecDialog.BgExecutor doWork = new WaitForExecDialog.BgExecutor(wait)
							{
								@Override
								public Object doWork()
								{
									try { PersistWriterBase.waitForRecordingStartTime(hhmm, getWaitDialog()); }
									catch (InterruptedException ignore) {}

									return null;
								}

								/** Should the cancel button be visible or not. */
								@Override
								public boolean canDoCancel()
								{
									return true;
								}
							};
							  
							// or if you didn't return anything from the doWork() method
							wait.execAndWait(doWork);
							
							// Stay in the Connection Dialog if cancel was pressed
							if (wait.wasCanceled())
							{
								return;
							}
						}
					}
				} // end: wait for connect

				// PCS CONNECT
				if (recordThisSession)
				{
					// CONNECT to the PCS, if it fails, we stay in the dialog

					// A new instance will be created each time we connect/hit_ok_but, 
					// thats ok because its done inside pcsConnect().
					if ( ! pcsConnect() )
						return;

					// setPersistCounters for all CM:s
					if (_useCmForPcsTable)
					{
						TableModel tm = _pcsSessionTable.getModel();
						for(int r=0; r<tm.getRowCount(); r++)
						{
							for (CountersModel cm : GetCounters.getCmList())
							{
								String  rowName      = (String)   tm.getValueAt(r, PCS_TAB_POS_CM_NAME);
								Integer pcsTimeout   = (Integer)  tm.getValueAt(r, PCS_TAB_POS_TIMEOUT);
								Integer pcsPostpone  = (Integer)  tm.getValueAt(r, PCS_TAB_POS_POSTPONE);
								boolean pcsStore     = ((Boolean) tm.getValueAt(r, PCS_TAB_POS_STORE_PCS)) .booleanValue();
								boolean pcsStoreAbs  = ((Boolean) tm.getValueAt(r, PCS_TAB_POS_STORE_ABS)) .booleanValue();
								boolean pcsStoreDiff = ((Boolean) tm.getValueAt(r, PCS_TAB_POS_STORE_DIFF)).booleanValue();
								boolean pcsStoreRate = ((Boolean) tm.getValueAt(r, PCS_TAB_POS_STORE_RATE)).booleanValue();
								String  cmName   = cm.getName();
								if (cmName.equals(rowName))
								{
									cm.setPersistCounters    (pcsStore,               true);
									cm.setPersistCountersAbs (pcsStoreAbs,            true);
									cm.setPersistCountersDiff(pcsStoreDiff,           true);
									cm.setPersistCountersRate(pcsStoreRate,           true);
									cm.setPostponeTime       (pcsPostpone.intValue(), true);
									cm.setQueryTimeout       (pcsTimeout.intValue(),  true);
									continue;
								}
							}
						}
					}
					else
					{
//						TableModel tm = _pcsSessionTable.getModel();
//						for(int r=0; r<tm.getRowCount(); r++)
//						{
//							String  rowName      = (String)   tm.getValueAt(r, PCS_TAB_POS_CM_NAME);
//							Integer pcsTimeout   = (Integer)  tm.getValueAt(r, PCS_TAB_POS_TIMEOUT);
//							Integer pcsPostpone  = (Integer)  tm.getValueAt(r, PCS_TAB_POS_POSTPONE);
//							boolean pcsStore     = ((Boolean) tm.getValueAt(r, PCS_TAB_POS_STORE_PCS)) .booleanValue();
//							boolean pcsStoreAbs  = ((Boolean) tm.getValueAt(r, PCS_TAB_POS_STORE_ABS)) .booleanValue();
//							boolean pcsStoreDiff = ((Boolean) tm.getValueAt(r, PCS_TAB_POS_STORE_DIFF)).booleanValue();
//							boolean pcsStoreRate = ((Boolean) tm.getValueAt(r, PCS_TAB_POS_STORE_RATE)).booleanValue();
//							
//							//System.out.println("OK: name="+StringUtil.left(rowName,25)+", timeout="+pcsTimeout+", postpone="+pcsPostpone+", store="+pcsStore+", abs="+pcsStoreAbs+", diff="+pcsStoreDiff+", rate="+pcsStoreRate+".");
//						}
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

				// Set the desired disconnect time.
				_disConnectTime = null;
				try 
				{ 
					Date stopTime  = PersistWriterBase.getRecordingStopTime(null, aseDeferredDisConnectChkAction()); 
					_disConnectTime = stopTime;
				}
				catch(Exception ex) 
				{
					_logger.warn("Could not determen stop/disconnect time, stop time will NOT be set.");
				}

				// SET CONNECTION TYP and "CLOSE" the dialog
				_connectionType = TDS_CONN;
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

		// ALWAYS: update tunnel description
		updateSshTunnelDescription();
		
		validateContents();
	}

	private void updateSshTunnelDescription()
	{
		// set the ASE SSH desription to visible or not + resize dialog if it's the checkbox
		_aseSshTunnelDesc_lbl.setVisible(_aseSshTunnel_chk.isSelected());
		if (_aseSshTunnel_chk.isSelected())
		{
			boolean generateLocalPort = true;
			int    localPort = 7487;
			String destHost  = "asehostname";
			int    destPort  = 5000;
			String sshHost   = "sshHost";
			int    sshPort   = 22;
			String sshUser   = "sshUser";
			String sshPass   = "*secret*";
			if (_aseSshTunnelInfo != null)
			{
				generateLocalPort = _aseSshTunnelInfo.isLocalPortGenerated();
				localPort = _aseSshTunnelInfo.getLocalPort();
				destHost  = _aseSshTunnelInfo.getDestHost();
				destPort  = _aseSshTunnelInfo.getDestPort();
				sshHost   = _aseSshTunnelInfo.getSshHost();
				sshPort   = _aseSshTunnelInfo.getSshPort();
				sshUser   = _aseSshTunnelInfo.getSshUsername();
				sshPass   = _aseSshTunnelInfo.getSshPassword();
			}
			_aseSshTunnelDesc_lbl.setText(
				"<html>" +
					"Local Port '<b>" + (generateLocalPort ? "*generated*" : localPort) + "</b>', " +
					"Dest Host  '<b>" + destHost + ":" + destPort  + "</b>', <br>" +
					"SSH Host   '<b>" + sshHost  + ":" + sshPort   + "</b>', " +
					"SSH User   '<b>" + sshUser   + "</b>'. " +
					(_logger.isDebugEnabled() ? "SSH Passwd '<b>" + sshPass + "</b>' " : "") +
				"</html>");
		}
	}

	private String aseDeferredConnectChkAction()
	{
		boolean enable = _aseDeferredConnect_chk.isSelected();
		_aseDeferredConnectHour_lbl  .setEnabled(enable);
		_aseDeferredConnectHour_sp   .setEnabled(enable);
		_aseDeferredConnectMinute_lbl.setEnabled(enable);
		_aseDeferredConnectMinute_sp .setEnabled(enable);
		_aseDeferredConnectTime_lbl  .setEnabled(enable);

		String hhmm = null;
		if ( enable )
		{
			String hh = "" + _aseDeferredConnectHour_sp  .getValue();
			String mm = "" + _aseDeferredConnectMinute_sp.getValue();
			
			hhmm = hh + ":" + mm;
			Date startTime;
			try 
			{ 
				startTime = PersistWriterBase.getRecordingStartTime(hhmm); 
			}
			catch(Exception ignore) 
			{
				startTime = new Date(); 
			}
			String startTimeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(startTime);

			_aseDeferredConnectTime_lbl.setText("  Start Time: " + startTimeStr);
		}
		else
		{
			_aseDeferredConnectTime_lbl.setText("");
		}
		return hhmm;
	}
	private String aseDeferredDisConnectChkAction()
	{
		boolean enable = _aseDeferredDisConnect_chk.isSelected();
		_aseDeferredDisConnectHour_lbl  .setEnabled(enable);
		_aseDeferredDisConnectHour_sp   .setEnabled(enable);
		_aseDeferredDisConnectMinute_lbl.setEnabled(enable);
		_aseDeferredDisConnectMinute_sp .setEnabled(enable);
		_aseDeferredDisConnectTime_lbl  .setEnabled(enable);

		String hhmm = null;
		if ( enable )
		{
			String hh = "" + _aseDeferredDisConnectHour_sp  .getValue();
			String mm = "" + _aseDeferredDisConnectMinute_sp.getValue();
			
			hhmm = hh + ":" + mm;
			Date stopTime;
			try 
			{ 
				Date startTime = PersistWriterBase.getRecordingStartTime(aseDeferredConnectChkAction());
				stopTime = PersistWriterBase.getRecordingStopTime(startTime, hhmm); 
			}
			catch(Exception ignore) 
			{
				stopTime = new Date(); 
			}
			String startTimeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(stopTime);

			_aseDeferredDisConnectTime_lbl.setText("   Stop Time: " + startTimeStr);
		}
		else
		{
			_aseDeferredDisConnectTime_lbl.setText("");
		}
		return hhmm;
	}

	// Typed characters in the fields are visible first when the key has been released: keyReleased()
	@Override
	public void keyPressed (KeyEvent keyevent)
	{
	}

	// Discard all but digits for the _port_txt field
	@Override
	public void keyTyped   (KeyEvent keyevent) 
	{
		Object source = keyevent.getSource();
		
		if (_asePort_txt.equals(source) || _aseLoginTimeout_txt.equals(source))
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
	@Override
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

	// TAB change or Spinner changes
	@Override
	public void stateChanged(ChangeEvent e)
	{
		if (   _aseDeferredConnectHour_sp     .equals(e.getSource())    
		    || _aseDeferredConnectMinute_sp   .equals(e.getSource())
		    || _aseDeferredDisConnectHour_sp  .equals(e.getSource()) 
		    || _aseDeferredDisConnectMinute_sp.equals(e.getSource())
		   )
		{
			aseDeferredConnectChkAction();
			aseDeferredDisConnectChkAction();
			return;
		}

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

	
	@Override
	public void focusGained(FocusEvent e)
	{
	}

	@Override
	public void focusLost(FocusEvent e)
	{
		validateContents();
	}
	/*---------------------------------------------------
	** END: implementing ActionListener, KeyListeners, FocusListener
	**---------------------------------------------------
	*/

	private void setToTemplate()
	{
		TableModel tm = _pcsSessionTable.getModel();
		for (int r=0; r<tm.getRowCount(); r++)
		{
			String        cmName = (String)  tm.getValueAt(r, PCS_TAB_POS_CM_NAME);
			CountersModel cm     = GetCounters.getInstance().getCmByName(cmName);
			if (cm == null)
				continue;

			tm.setValueAt(new Integer(cm.getQueryTimeout()),              r, PCS_TAB_POS_TIMEOUT);
			tm.setValueAt(new Integer(cm.getPostponeTime()),              r, PCS_TAB_POS_POSTPONE);
//			tm.setValueAt(new Boolean(cm.isPersistCountersEnabled() || cm.isBackgroundDataPollingEnabled()), r, PCS_TAB_POS_STORE_PCS);
			tm.setValueAt(new Boolean(cm.isPersistCountersEnabled()),     r, PCS_TAB_POS_STORE_PCS);
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

		if ( ! hostPortMap.isEmpty() )
		{
			String url = AseUrlHelper.buildUrlString(hostPortMap, null, optionsMap);
			_aseConnUrl_txt.setText(url);
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

		if (_connProfileVisible_chk.isSelected() && _connTabbedVisible_chk .isSelected())
			conf.setProperty("conn.splitpane.dividerLocation",  _connSplitPane.getDividerLocation());
		conf.setProperty("conn.panel.profile.visible",          _connProfileVisible_chk.isSelected());
		conf.setProperty("conn.panel.tabed.visible",            _connTabbedVisible_chk .isSelected());


		String hostPort = AseConnectionFactory.toHostPortStr(_aseHost_txt.getText(), _asePort_txt.getText());

		conf.setProperty("conn.interfaces",                     _aseIfile_txt.getText());
		conf.setProperty("conn.serverName",                     _aseServer_cbx.getSelectedItem().toString());

		conf.setProperty("conn.hostname",                       _aseHost_txt.getText());
		conf.setProperty("conn.port",                           _asePort_txt.getText());

		conf.setProperty("conn.username",                       _aseUser_txt.getText());
		conf.setProperty("conn.username."+hostPort,             _aseUser_txt.getText());
		if (_aseOptionSavePwd_chk.isSelected())
		{
			conf.setProperty("conn.password",           _asePasswd_txt.getText(), true);
			conf.setProperty("conn.password."+hostPort, _asePasswd_txt.getText(), true);
		}
		else
		{
			conf.remove("conn.password");
			conf.remove("conn.password."+hostPort);
		}

		conf.setProperty("conn.login.timeout",                  _aseLoginTimeout_txt.getText() );

		conf.setProperty(PROPKEY_CONN_SSH_TUNNEL,               _aseSshTunnel_chk.isSelected() );
		conf.setProperty(PROPKEY_CONN_SSH_TUNNEL+"."+hostPort,  _aseSshTunnel_chk.isSelected() );

		conf.setProperty("conn.login.sql.init",                 _aseSqlInit_txt.getText() );
		conf.setProperty("conn.login.sql.init."+hostPort,       _aseSqlInit_txt.getText() );

		conf.setProperty("conn.url.raw",                        _aseConnUrl_txt.getText() );
		conf.setProperty("conn.url.raw.checkbox",               _aseConnUrl_chk.isSelected() );
		conf.setProperty("conn.url.options",                    _aseOptions_txt.getText() );

		conf.setProperty("conn.savePassword",                   _aseOptionSavePwd_chk.isSelected() );
		conf.setProperty(CONF_OPTION_CONNECT_ON_STARTUP,        _aseOptionConnOnStart_chk.isSelected() );
		conf.setProperty(CONF_OPTION_RECONNECT_ON_FAILURE,      _aseOptionReConnOnFailure_chk.isSelected());

		conf.setProperty("conn.persistCounterStorage",           _aseOptionStore_chk.isSelected() );
		conf.setProperty("conn.hostMonitoring",                  _aseHostMonitor_chk.isSelected() );
		conf.setProperty("conn.deferred.connect",                _aseDeferredConnect_chk.isSelected() );
		conf.setProperty("conn.deferred.connect.hour",           _aseDeferredConnectHour_sp  .getValue().toString() );
		conf.setProperty("conn.deferred.connect.minute",         _aseDeferredConnectMinute_sp.getValue().toString() );
		conf.setProperty("conn.deferred.disconnect",             _aseDeferredDisConnect_chk.isSelected() );
		conf.setProperty("conn.deferred.disconnect.hour",        _aseDeferredDisConnectHour_sp  .getValue().toString() );
		conf.setProperty("conn.deferred.disconnect.minute",      _aseDeferredDisConnectMinute_sp.getValue().toString() );

		//----------------------------------
		// TAB: OS Host
		//----------------------------------
		if ( _aseHostMonitor_chk.isSelected() )
		{
			conf.setProperty("ssh.conn.hostname."+hostPort,   _hostmonHost_txt.getText() );
			conf.setProperty("ssh.conn.port."+hostPort,       _hostmonPort_txt.getText() );
			conf.setProperty("ssh.conn.username."+hostPort,   _hostmonUser_txt.getText() );

			if (_hostmonOptionSavePwd_chk.isSelected())
				conf.setProperty("ssh.conn.password."+hostPort, _hostmonPasswd_txt.getText(), true);
			else
				conf.remove("ssh.conn.password."+hostPort);

			conf.setProperty("ssh.conn.savePassword", _hostmonOptionSavePwd_chk.isSelected() );
		}

		//----------------------------------
		// TAB: Counter Storage
		//----------------------------------
		if ( _aseOptionStore_chk.isSelected() )
		{
			conf.setProperty("pcs.write.writerClass", _pcsWriter_cbx    .getEditor().getItem().toString() );
			conf.setProperty("pcs.write.jdbcDriver",  _pcsJdbcDriver_cbx.getEditor().getItem().toString() );
			conf.setProperty("pcs.write.jdbcUrl",     _pcsJdbcUrl_cbx   .getEditor().getItem().toString() );
			conf.setProperty("pcs.write.jdbcUser",    _pcsJdbcUsername_txt.getText() );
			conf.setProperty("pcs.write.jdbcPasswd",  _pcsJdbcPassword_txt.getText(), true );

			if (_pcsH2Option_startH2NetworkServer_chk.isVisible())
				conf.setProperty    ("pcs.write.h2.startH2NetworkServer", _pcsH2Option_startH2NetworkServer_chk.isSelected() );
			else
				conf.setProperty    ("pcs.write.h2.startH2NetworkServer", false );

			// DDL Lookup & Store
			conf.setProperty        ("pcs.write.ddl.doDdlLookup",                     _pcsDdl_doDdlLookupAndStore_chk            .isSelected() );
			conf.setProperty        ("pcs.write.ddl.addDependantObjectsToDdlInQueue", _pcsDdl_addDependantObjectsToDdlInQueue_chk.isSelected() );
			conf.setProperty        ("pcs.write.ddl.afterDdlLookupSleepTimeInMs",     _pcsDdl_afterDdlLookupSleepTimeInMs_txt    .getText() );

			// The info in JTable is stored by the CM itself...
		}
		
		//----------------------------------
		// TAB: offline
		//----------------------------------
		if ( true )
		{
			conf.setProperty("pcs.read.jdbcDriver",          _offlineJdbcDriver_cbx.getEditor().getItem().toString() );
			conf.setProperty("pcs.read.jdbcUrl",             _offlineJdbcUrl_cbx   .getEditor().getItem().toString() );
			conf.setProperty("pcs.read.jdbcUser",            _offlineJdbcUsername_txt.getText() );
			conf.setProperty("pcs.read.jdbcPasswd",          _offlineJdbcPassword_txt.getText(), true );
			conf.setProperty("pcs.read.checkForNewSessions", _offlineCheckForNewSessions_chk.isSelected());

			if (_offlineH2Option_startH2NwSrv_chk.isVisible())
				conf.setProperty    ("pcs.read.h2.startH2NetworkServer", _offlineH2Option_startH2NwSrv_chk.isSelected() );
			else
				conf.setProperty    ("pcs.read.h2.startH2NetworkServer", false );
			
		}

		//----------------------------------
		// TAB: JDBC
		//----------------------------------
		if ( true )
		{
			String urlStr = _jdbcUrl_cbx   .getEditor().getItem().toString();
			
			conf.setProperty("jdbc.jdbcDriver",          _jdbcDriver_cbx.getEditor().getItem().toString() );
			conf.setProperty("jdbc.jdbcUrl",             _jdbcUrl_cbx   .getEditor().getItem().toString() );
			conf.setProperty("jdbc.jdbcUser",            _jdbcUsername_txt.getText() );
			conf.setProperty("jdbc.jdbcPasswd",          _jdbcPassword_txt.getText(), true );

			conf.setProperty("jdbc.login.sql.init",         _jdbcSqlInit_txt.getText() );
			conf.setProperty("jdbc.login.sql.init."+urlStr, _jdbcSqlInit_txt.getText() );
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

		_lastKnownConnSplitPaneDividerLocation = conf.getIntProperty("conn.splitpane.dividerLocation", DEFAULT_CONN_SPLITPANE_DIVIDER_LOCATION);
		if (_lastKnownConnSplitPaneDividerLocation < 10)
			_lastKnownConnSplitPaneDividerLocation = DEFAULT_CONN_SPLITPANE_DIVIDER_LOCATION;
		_connSplitPane.setDividerLocation(_lastKnownConnSplitPaneDividerLocation);

		if ( ! conf.getBooleanProperty("conn.panel.profile.visible", DEFAULT_CONN_PROFILE_PANEL_VISIBLE) )
			_connProfileVisible_chk.doClick();

		if ( ! conf.getBooleanProperty("conn.panel.tabed.visible", DEFAULT_CONN_TABED_PANEL_VISIBLE) )
			_connTabbedVisible_chk.doClick();

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

		str = conf.getProperty("conn.login.timeout");
		if (str == null)
			str = conf.getProperty(AseConnectionFactory.PROPERTY_LOGINTIMEOUT, "10");
		_aseLoginTimeout_txt.setText(str);

		bol = conf.getBooleanProperty(PROPKEY_CONN_SSH_TUNNEL, DEFAULT_CONN_SSH_TUNNEL);
		_aseSshTunnel_chk.setSelected(bol);

		str = conf.getProperty("conn.login.sql.init");
		if (str != null)
			_aseSqlInit_txt.setText(str);

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

// Do not restore Deferred Connect, lets always start at FALSE / NOT CHECKED 
//		_aseDeferredConnect_chk     .setSelected( conf.getBooleanProperty("conn.deferred.connect",        false ));
		_aseDeferredConnectHour_sp  .setValue(    conf.getIntProperty(    "conn.deferred.connect.hour",   0 ));
		_aseDeferredConnectMinute_sp.setValue(    conf.getIntProperty(    "conn.deferred.connect.minute", 0 ));
// Do not restore Deferred DisConnect, lets always start at FALSE / NOT CHECKED 
//		_aseDeferredDisConnect_chk     .setSelected( conf.getBooleanProperty("conn.deferred.disconnect",        false ));
		_aseDeferredDisConnectHour_sp  .setValue(    conf.getIntProperty(    "conn.deferred.disconnect.hour",   0 ));
		_aseDeferredDisConnectMinute_sp.setValue(    conf.getIntProperty(    "conn.deferred.disconnect.minute", 0 ));

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

		// DDL Lookup & Store
		bol = conf.getBooleanProperty("pcs.write.ddl.doDdlLookup", PersistentCounterHandler.DEFAULT_ddl_doDdlLookupAndStore );
		_pcsDdl_doDdlLookupAndStore_chk.setSelected(bol);

		bol = conf.getBooleanProperty("pcs.write.ddl.addDependantObjectsToDdlInQueue", PersistentCounterHandler.DEFAULT_ddl_addDependantObjectsToDdlInQueue);
		_pcsDdl_addDependantObjectsToDdlInQueue_chk.setSelected(bol);

		str = conf.getProperty("pcs.write.ddl.afterDdlLookupSleepTimeInMs", PersistentCounterHandler.DEFAULT_ddl_afterDdlLookupSleepTimeInMs + "");
		_pcsDdl_afterDdlLookupSleepTimeInMs_txt.setText(str);
		
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

		//----------------------------------
		// TAB: JDBC
		//----------------------------------
		str = conf.getProperty("jdbc.jdbcDriver");
		if (str != null)
			_jdbcDriver_cbx.setSelectedItem(str);

		str = conf.getProperty("jdbc.jdbcUrl");
		if (str != null)
			_jdbcUrl_cbx.setSelectedItem(str);

		str = conf.getProperty("jdbc.jdbcUser");
		if (str != null)
			_jdbcUsername_txt.setText(str);

		str = conf.getProperty("jdbc.jdbcPasswd");
		if (str != null)
			_jdbcPassword_txt.setText(str);

//		String urlStr = conf.getProperty("jdbc.jdbcUrl");
		_jdbcSqlInit_txt.setText(conf.getProperty("jdbc.login.sql.init", "" ));
//		conf.getProperty("jdbc.login.sql.init."+urlStr, "" );

	}
	private void getSavedWindowProps()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		if (conf == null)
		{
			_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
			return;
		}
		
		//----------------------------------
		// TAB: Offline
		//----------------------------------
		int width  = conf.getIntProperty("conn.dialog.window.width",  -1);
		int height = conf.getIntProperty("conn.dialog.window.height", -1);
		int x      = conf.getIntProperty("conn.dialog.window.pos.x",  -1);
		int y      = conf.getIntProperty("conn.dialog.window.pos.y",  -1);
		if (width != -1 && height != -1)
			this.setSize(width, height);

		if (x != -1 && y != -1)
			if ( ! SwingUtils.isOutOfScreen(x, y, width, height) )
				this.setLocation(x, y);

		// Window size can not be "smaller" than the minimum size
		// If so "OK" button etc will be hidden.
		SwingUtils.setWindowMinSize(this);
	}

	
//	private void loadPropsForServer(String host, int port)
//	{
//		String hostPortStr = host + ":" + port;
//		loadPropsForServer(hostPortStr);
//	}

	public static String getPasswordForServer(String hostPortStr)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String str = null;

		// First do "conn.password.hostName.portNum", if not found, go to "conn.password"
		str = conf.getProperty("conn.password."+hostPortStr);
		if (str != null)
		{
			return str;
		}
		else
		{
			str = conf.getProperty("conn.password");
			if (str != null)
				return str;
		}
		return str;
	}
	private void loadPropsForServer(String hostPortStr)
	{
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
		str = getPasswordForServer(hostPortStr);
		if (str != null)
			_asePasswd_txt.setText(str);

//		str = conf.getProperty("conn.password."+hostPortStr);
//		if (str != null)
//		{
//			_asePasswd_txt.setText(str);
//		}
//		else
//		{
//			str = conf.getProperty("conn.password");
//			if (str != null)
//				_asePasswd_txt.setText(str);
//		}
		
		// SSH Tunnel stuff: first for host:port, then use fallback
//		str = conf.getProperty(PROPERTY_CONN_SSH_TUNNEL+"."+hostPortStr);
//		if (str != null)
//			_aseSshTunnel_chk.setSelected( Boolean.parseBoolean(str) );
//		else
//			_aseSshTunnel_chk.setSelected( false );
//		else
//		{
//			str = conf.getProperty(PROPERTY_CONN_SSH_TUNNEL);
//			if (str != null)
//				_aseSshTunnel_chk.setSelected( Boolean.parseBoolean(str) );
//		}
		_aseSshTunnel_chk.setSelected( conf.getBooleanProperty(PROPKEY_CONN_SSH_TUNNEL+"."+hostPortStr, DEFAULT_CONN_SSH_TUNNEL) );
		if (_aseSshTunnel_chk.isSelected())
			_aseSshTunnelInfo = SshTunnelDialog.getSshTunnelInfo(hostPortStr);
		updateSshTunnelDescription();

		_aseSqlInit_txt.setText( conf.getProperty("conn.login.sql.init."+hostPortStr, ""));


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
								if ( systemType.toLowerCase().indexOf("local") > 0 )
									networkDrive = false;

								// Finish Localization: 
								if ( systemType.toLowerCase().indexOf("paikallinen levy") > 0 )
									networkDrive = false;
							}
						}
						catch (Throwable t) {/*ignore*/}

						if (networkDrive)
						{
							boolean showDialog = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_showDialogOnNoLocalPcsDrive, DEFAULT_showDialogOnNoLocalPcsDrive);
							if (showDialog)
							{
								// Create a check box that will be passed to the message
								JCheckBox chk = new JCheckBox("Show this information next time.", showDialog);
								chk.addActionListener(new ActionListener()
								{
									@Override
									public void actionPerformed(ActionEvent e)
									{
										Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
										if (conf == null)
											return;
										conf.setProperty(PROPKEY_showDialogOnNoLocalPcsDrive, ((JCheckBox)e.getSource()).isSelected());
										conf.save();
									}
								});

								String htmlStr = 
									"<html>" +
									"<h2>Warning - Use a Local drive for recordings</h2>" +
									"Using a network drive for recordings will be <b>much</b> slower.<br>" +
									"The recomendation is to use a local drive as the database storage!<br>" +
									"<br>" +
									"The selected storage file is '"+filename+"'.<br>" +
									"Is this on a local drive?<br>" +
									"<br>" +
									"The localized System Type Description from the Operating System, is '<b>"+systemType+"</b>'.<br>" +
									"If this string doesn't contain 'local', then it's considdered as a network drive.<br>" +
									"<br>" +
									"Note: If the localized type '<b>"+systemType+"</b>' containes 'local' in you'r localization, please email me at goran_schwarz@hotmail.com the localized string.<br>" +
									"<br>" +
									"This is just a warning message, but be aware that the storage thread might not keep up...<br>" +
									"</html>";

//								SwingUtils.showWarnMessage(this, "Local drive?", htmlStr, null);
								SwingUtils.showWarnMessageExt(this, "Local drive?", htmlStr, chk, (JPanel)null);
							} // end: showDialog 
						} // end: networkDrive
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

		System.out.println("showTdsOnlyConnectionDialog ...");
		Connection conn = ConnectionDialog.showTdsOnlyConnectionDialog(null);
		System.out.println("showTdsOnlyConnectionDialog, returned: conn="+conn);

		// DO THE THING
		ConnectionDialog connDialog = new ConnectionDialog(null, false, true, true, false, false, false, true);
		connDialog.setVisible(true);
		connDialog.dispose();

		int connType = connDialog.getConnectionType();
		
		if ( connType == ConnectionDialog.CANCEL)
		{
			System.out.println("---CANCEL...");
		}

		if ( connType == ConnectionDialog.TDS_CONN)
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

