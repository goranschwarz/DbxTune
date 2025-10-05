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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.dbxtune.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import javax.naming.NameNotFoundException;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DropMode;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
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
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import org.jdesktop.swingx.table.TableColumnModelExt;

import com.dbxtune.AppDir;
import com.dbxtune.CounterController;
import com.dbxtune.DbxTune;
import com.dbxtune.Version;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.ConnectionProfile.DbxTuneParams;
import com.dbxtune.gui.ConnectionProfileManager.ProfileType;
import com.dbxtune.gui.focusabletip.FocusableTipExtention;
import com.dbxtune.gui.swing.ClickListener;
import com.dbxtune.gui.swing.GLabel;
import com.dbxtune.gui.swing.GTabbedPane;
import com.dbxtune.gui.swing.GTextField;
import com.dbxtune.gui.swing.GTree;
import com.dbxtune.gui.swing.MultiLineLabel;
import com.dbxtune.gui.swing.TreeTransferHandler;
import com.dbxtune.gui.swing.VerticalScrollPane;
import com.dbxtune.gui.swing.WaitForExecDialog;
import com.dbxtune.hostmon.HostMonitorConnection;
import com.dbxtune.hostmon.HostMonitorConnectionLocalOsCmd;
import com.dbxtune.hostmon.HostMonitorConnectionLocalOsCmdWrapper;
import com.dbxtune.hostmon.HostMonitorConnectionSsh;
import com.dbxtune.pcs.PersistReader;
import com.dbxtune.pcs.PersistWriterBase;
import com.dbxtune.pcs.PersistWriterJdbc;
import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.pcs.inspection.IObjectLookupInspector;
import com.dbxtune.pcs.sqlcapture.ISqlCaptureBroker;
import com.dbxtune.sql.JdbcUrlParser;
import com.dbxtune.sql.conn.ConnectionProp;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.ssh.SshConnection;
import com.dbxtune.ssh.SshTunnelDialog;
import com.dbxtune.ssh.SshTunnelInfo;
import com.dbxtune.utils.AseConnectionFactory;
import com.dbxtune.utils.AseConnectionUtils;
import com.dbxtune.utils.AseUrlHelper;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.FileUtils;
import com.dbxtune.utils.H2UrlHelper;
import com.dbxtune.utils.JdbcDriverHelper;
import com.dbxtune.utils.PlatformUtils;
import com.dbxtune.utils.RepServerUtils;
import com.dbxtune.utils.SqlUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;


public class ConnectionDialog
	extends JDialog
	implements ActionListener, KeyListener, TableModelListener, ChangeListener, FocusListener
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long serialVersionUID = -7782953767666701933L;
	
	public static final String   PROPKEY_RECONNECT_ON_FAILURE        = "conn.reconnectOnFailure";
	public static final boolean  DEFAULT_RECONNECT_ON_FAILURE        = false;

	public static final int      ACTION_EVENT_ID__CONNECT_ON_STARTUP = 99;
	public static final String   PROPKEY_CONNECT_ON_STARTUP          = "conn.onStartup";
	public static final boolean  DEFAULT_CONNECT_ON_STARTUP          = false;

	public  static final String  PROPKEY_CONN_SSH_TUNNEL             = "conn.ssh.tunnel";
	public  static final boolean DEFAULT_CONN_SSH_TUNNEL             = false;

	public  static final String  PROPKEY_CONN_JDBC_SSH_TUNNEL        = "conn.jdbc.ssh.tunnel";
	public  static final boolean DEFAULT_CONN_JDBC_SSH_TUNNEL        = false;

	public static final String   PROPKEY_showDialogOnNoLocalPcsDrive = "ConnectionDialog.showDialog.pcs.noLocalDrive";
	public static final boolean  DEFAULT_showDialogOnNoLocalPcsDrive = true;
	
	private static final String  PROPKEY_CONN_PROFILE_PANEL_VISIBLE  = "conn.panel.profile.isVisible";
	private static final boolean DEFAULT_CONN_PROFILE_PANEL_VISIBLE  = true;
	private static final String  PROPKEY_CONN_TABED_PANEL_VISIBLE    = "conn.panel.tabed.isVisible";
	private static final boolean DEFAULT_CONN_TABED_PANEL_VISIBLE    = true;

	private static final String  PROPKEY_CONN_SPLITPANE_DIVIDER_LOCATION = "conn.splitpane.dividerLocation";
	private static final int     DEFAULT_CONN_SPLITPANE_DIVIDER_LOCATION = SwingUtils.hiDpiScale(150);
	
	public  static final String  PROPKEY_CONN_SQLSERVER_WIN_AUTH     = "conn.jdbc.sqlserver.windows.authentication";
	public  static final boolean DEFAULT_CONN_SQLSERVER_WIN_AUTH     = false;
	
	public  static final String  PROPKEY_CONN_SQLSERVER_ENCRYPT      = "conn.jdbc.sqlserver.encrypt";
	public  static final boolean DEFAULT_CONN_SQLSERVER_ENCRYPT      = true;

	public  static final String  PROPKEY_CONN_SQLSERVER_TRUST_CERT   = "conn.jdbc.sqlserver.trustServerCertificate";
	public  static final boolean DEFAULT_CONN_SQLSERVER_TRUST_CERT   = true;
	
	static
	{
		Configuration.registerDefaultValue(PROPKEY_RECONNECT_ON_FAILURE,       DEFAULT_RECONNECT_ON_FAILURE);
		Configuration.registerDefaultValue(PROPKEY_CONNECT_ON_STARTUP,         DEFAULT_CONNECT_ON_STARTUP);
		Configuration.registerDefaultValue(PROPKEY_CONN_SSH_TUNNEL,            DEFAULT_CONN_SSH_TUNNEL);
		Configuration.registerDefaultValue(PROPKEY_CONN_JDBC_SSH_TUNNEL,       DEFAULT_CONN_JDBC_SSH_TUNNEL);

		Configuration.registerDefaultValue(PROPKEY_CONN_PROFILE_PANEL_VISIBLE, DEFAULT_CONN_PROFILE_PANEL_VISIBLE);
		Configuration.registerDefaultValue(PROPKEY_CONN_TABED_PANEL_VISIBLE,   DEFAULT_CONN_TABED_PANEL_VISIBLE);
	}
	
	public  static final int   CANCEL           = 0;
	public  static final int   TDS_CONN         = 1;
	public  static final int   OFFLINE_CONN     = 2;
//	public  static final int   TDS_CONN         = 3;
	public  static final int   JDBC_CONN        = 4;
	private int                _connectionType  = CANCEL;

//	private Map                      _inputMap        = null;
	private DbxConnection            _aseConn         = null;
	private SshConnection            _sshConn         = null; // FIXME: Still used internally, but we should really be using _hostMonConn...
	private HostMonitorConnection    _hostMonConn     = null;
//	private Connection               _pcsConn         = null;
	private DbxConnection            _offlineConn     = null;
	private DbxConnection            _jdbcConn        = null;
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

	private static final String      NO_TEMPLATE_IS_SELECTED       = "<choose a template>";
	private static final String      NO_PROFILE_IS_SELECTED        = "<choose a profile>";
	private static final ProfileType NO_PROFILE_TYPE_IS_SELECTED   = new ProfileType("&lt;choose type&gt;");
	private static final ProfileType EDIT_PROFILE_TYPE_IS_SELECTED = new ProfileType("<b>Edit Types...</b>");

	//-------------------------------------------------
	// Actions
	public static final String ACTION_OK        = "OK";
	public static final String ACTION_CANCEL    = "CANCEL";
	

	@SuppressWarnings("unused")
//	private Frame                _owner           = null;
	private Window               _owner           = null;

	private int                  _lastKnownConnSplitPaneDividerLocation = DEFAULT_CONN_SPLITPANE_DIVIDER_LOCATION;
	private JSplitPane           _connSplitPane;
	private JPanel               _connProfilePanel;
	private GTree                _connProfileTree;
	private boolean              _treeExpansionListenerEnabled = true;
	private JPanel               _connTabbedPanel;
	private GTabbedPane          _tab;
	private JPanel               _okCancelPanel;
	private JCheckBox            _connProfileVisible_chk = new JCheckBox("", DEFAULT_CONN_PROFILE_PANEL_VISIBLE);
	private JCheckBox            _connTabbedVisible_chk  = new JCheckBox("", DEFAULT_CONN_TABED_PANEL_VISIBLE);

	//---- ASE panel
	private JLabel               _aseProfile_lbl     = new JLabel("Use Profile");
	private ProfileComboBoxModel _aseProfile_mod     = new ProfileComboBoxModel(ConnectionProfile.Type.TDS);
	private JComboBox<String>    _aseProfile_cbx     = new JComboBox<String>(_aseProfile_mod);
	private ProfileTypeComboBoxModel _aseProfileType_mod = new ProfileTypeComboBoxModel();
	private JComboBox<ProfileType>   _aseProfileType_cbx = new JComboBox<ProfileType>(_aseProfileType_mod);
	private JButton              _aseProfileSave_but = new JButton("Save Profile...");
	private JButton              _aseProfileNew_but  = new JButton("New");
	private ImageIcon            _aseLoginImageIcon  = SwingUtils.readImageIcon(Version.class, "images/login_key.gif");
	private JLabel               _aseLoginIcon       = new JLabel(_aseLoginImageIcon);
	private MultiLineLabel       _aseLoginHelp       = new MultiLineLabel("Identify yourself to the server with user name and password");
	private JLabel               _aseUsername_lbl    = new JLabel("User name");
	private JTextField           _aseUsername_txt    = new JTextField();
	private JLabel               _asePassword_lbl    = new JLabel("Password");
	private JTextField           _asePassword_txt    = null; // set to JPasswordField or JTextField depending on debug level

	private ImageIcon            _aseServerImageIcon = SwingUtils.readImageIcon(Version.class, "images/ase32.gif");
	private JLabel               _aseServerIcon      = new JLabel(_aseServerImageIcon);
//	private MultiLineLabel       _aseServerHelp      = new MultiLineLabel("Select a server from the dropdown list, or enter host name and port number separeted by \":\" (For example \""+StringUtil.getHostname()+":5000\")");
	private MultiLineLabel       _aseServerHelp      = new MultiLineLabel("Select a server from the dropdown list, or enter host name and port number");
	private JLabel               _aseServerName_lbl  = new JLabel();
	private JLabel               _aseServer_lbl      = new JLabel("Server name");
	private LocalSrvComboBox     _aseServer_cbx      = new LocalSrvComboBox();

	private JCheckBox            _aseHostPortResolve_chk = new JCheckBox("Auto Lookup Profiles", true);

	private JLabel               _aseHost_lbl        = new JLabel("Host name");
	private JTextField           _aseHost_txt        = new JTextField();

	private JLabel               _asePort_lbl        = new JLabel("Port number");
	private JTextField           _asePort_txt        = new JTextField();

	private JLabel               _aseLoginTimeout_lbl= new JLabel("Login Timeout");
	private JTextField           _aseLoginTimeout_txt= new JTextField();

	private JLabel               _aseOptions_lbl     = new JLabel("URL Options");
	private JTextField           _aseOptions_txt     = new JTextField();
	private JButton              _aseOptions_but     = new JButton("...");

	private JLabel               _aseIfile_lbl       = new JLabel("Name service");
	private JTextField           _aseIfile_txt       = new JTextField();
	private String               _aseIfile_save      = "";
	private JButton              _aseIfile_but       = new JButton("...");
	private JButton              _aseEditIfile_but   = new JButton("Edit");

	private JCheckBox            _aseConnUrl_chk     = new JCheckBox("Use URL", false);
	private JTextField           _aseConnUrl_txt     = new JTextField();

	private SshTunnelInfo        _aseSshTunnelInfo       = null;
//	private JLabel               _aseSshTunnel_lbl       = new JLabel("SSH Tunnel");
	private JCheckBox            _aseSshTunnel_chk       = new JCheckBox("Use SSH (Secure Shell) Tunnel to connect to ASE", DEFAULT_CONN_SSH_TUNNEL);
	private JLabel               _aseSshTunnelDesc_lbl   = new JLabel();
	private JButton              _aseSshTunnel_but       = new JButton("SSH Settings...");

	private JLabel               _aseClientCharset_lbl   = new JLabel("Client Charset");
	private JComboBox<String>    _aseClientCharset_cbx   = new JComboBox<String>();

	private JLabel               _aseSqlInit_lbl         = new JLabel("SQL Init");
	private JTextField           _aseSqlInit_txt         = new JTextField("");

	private JPanel               _dbxTuneOptionsPanel             = null;
	private TemplateComboBoxModel _aseOptionUseTemplate_mod       = new TemplateComboBoxModel();
	private JCheckBox            _aseOptionUseTemplate_chk        = new JCheckBox("Use Counter Template", false);
	private JComboBox<String>    _aseOptionUseTemplate_cbx        = new JComboBox<String>(_aseOptionUseTemplate_mod);
	private JButton              _aseOptionUseTemplate_but        = new JButton("Template Dialog...");
	private JCheckBox            _aseOptionSavePwd_chk            = new JCheckBox("Save password", true);
	private JCheckBox            _aseOptionPwdEncryption_chk      = new JCheckBox("Encrypt password over the Network", true);
	private JCheckBox            _aseOptionConnOnStart_chk        = new JCheckBox("Connect to this server on startup",         DEFAULT_CONNECT_ON_STARTUP);
	private JCheckBox            _aseOptionReConnOnFailure_chk    = new JCheckBox("Reconnect to server if connection is lost", DEFAULT_RECONNECT_ON_FAILURE);
//	private JCheckBox            _aseOptionUsedForNoGui_chk       = new JCheckBox("Use connection info above for no-gui mode", false);
	private JCheckBox            _aseHostMonitor_chk              = new JCheckBox("<html>Monitor the OS Host for IO and CPU... <i>Set parameters in tab '<b>Host Monitor</b>'</i></html>", false);
//	private JCheckBox            _aseOptionStore_chk              = new JCheckBox("Save counter data in a Persistent Counter Storage...", false);
	private JCheckBox            _aseOptionStore_chk              = new JCheckBox("<html>Record this Performance Session in a DB... <i>Set parameters in tab '<b>Record this Session</b>'</i></html>", false);
	private JCheckBox            _aseDeferredConnect_chk          = new JCheckBox("Make the connection later", false);
	private JLabel               _aseDeferredConnectHour_lbl      = new JLabel("Start Hour");
	private SpinnerNumberModel   _aseDeferredConnectHour_spm      = new SpinnerNumberModel(0, 0, 23, 1); // value, min, max, step
	private JSpinner             _aseDeferredConnectHour_sp       = new JSpinner(_aseDeferredConnectHour_spm);
	private JLabel               _aseDeferredConnectMinute_lbl    = new JLabel(", Minute");
	private SpinnerNumberModel   _aseDeferredConnectMinute_spm    = new SpinnerNumberModel(0, 0, 59, 1); // value, min, max, step
	private JSpinner             _aseDeferredConnectMinute_sp     = new JSpinner(_aseDeferredConnectMinute_spm);
	private JLabel               _aseDeferredConnectTime_lbl      = new JLabel();
	private JCheckBox            _aseDeferredDisConnect_chk       = new JCheckBox("Disconnect After Elapsed Time", false);
	private JLabel               _aseDeferredDisConnectHour_lbl   = new JLabel("Hours");
	private SpinnerNumberModel   _aseDeferredDisConnectHour_spm   = new SpinnerNumberModel(0, 0, 999, 1); // value, min, max, step
	private JSpinner             _aseDeferredDisConnectHour_sp    = new JSpinner(_aseDeferredDisConnectHour_spm);
	private JLabel               _aseDeferredDisConnectMinute_lbl = new JLabel(", Minutes");
	private SpinnerNumberModel   _aseDeferredDisConnectMinute_spm = new SpinnerNumberModel(0, 0, 59, 1); // value, min, max, step
	private JSpinner             _aseDeferredDisConnectMinute_sp  = new JSpinner(_aseDeferredDisConnectMinute_spm);
	private JLabel               _aseDeferredDisConnectTime_lbl   = new JLabel();

	//---- OS HOST panel
	private JPanel               _hostmonUserPasswd_pan  = null;
	private ImageIcon            _hostmonLoginImageIcon  = SwingUtils.readImageIcon(Version.class, "images/login_key.gif");
	private JLabel               _hostmonLoginIcon       = new JLabel(_hostmonLoginImageIcon);
	private MultiLineLabel       _hostmonLoginHelp       = new MultiLineLabel("Identify yourself to the host Operating System with user name and password. A SSH (Secure Shell) connection will be used, so password and traffic will be encrypted over the network.");
	private JLabel               _hostmonUsername_lbl    = new JLabel("User name");
	private JTextField           _hostmonUsername_txt    = new JTextField();
	private JLabel               _hostmonPassword_lbl    = new JLabel("Password");
	private JTextField           _hostmonPassword_txt    = null; // set to JPasswordField or JTextField depending on debug level
	private JCheckBox            _hostmonOptionSavePwd_chk = new JCheckBox("Save password", true);
	private JLabel               _hostmonKeyFile_lbl     = new JLabel("Private Key File");
	private JTextField           _hostmonKeyFile_txt     = new JTextField();
	private JButton              _hostmonKeyFile_but     = new JButton("...");

	private JPanel               _hostmonServer_pan      = null;
	private ImageIcon            _hostmonServerImageIcon = SwingUtils.readImageIcon(Version.class, "images/server_32.png");
	private JLabel               _hostmonServerIcon      = new JLabel(_hostmonServerImageIcon);
//	private MultiLineLabel       _hostmonServerHelp      = new MultiLineLabel("Specify host name to the machine where you want to do Operating System Monitoring. The connection will be using SSH (Secure Shell), which normally is listening on port 22. ");
	private MultiLineLabel       _hostmonServerHelp      = new MultiLineLabel("Specify host name to the machine where you want to do Operating System Monitoring. The connection will be using SSH (Secure Shell), which normally is listening on port 22.  Note: If the OS is Windows, you need to install OpenSSH Server on the host you want to monitor.");
	private JLabel               _hostmonServerName_lbl  = new JLabel();

	private JLabel               _hostmonHost_lbl        = new JLabel("Host Name");
	private JTextField           _hostmonHost_txt        = new JTextField();
	private JLabel               _hostmonPort_lbl        = new JLabel("Port Number");
	private JTextField           _hostmonPort_txt        = new JTextField("22");

	private JPanel               _hostmonLocalOsCmd_pan        = null;
	private ImageIcon            _hostmonLocalOsCmdImageIcon   = SwingUtils.readImageIcon(Version.class, "images/hostmon_local_cmd.png");
	private JLabel               _hostmonLocalOsCmdIcon        = new JLabel(_hostmonLocalOsCmdImageIcon);
	private MultiLineLabel       _hostmonLocalOsCmdHelp        = new MultiLineLabel();
	private JCheckBox            _hostMonLocalOsCmd_chk        = new JCheckBox("Execute the Host Monitoring Command on the Local Computer (no SSH)");
	private GLabel               _hostmonLocalOsCmdWrapper_lbl = new GLabel("Wrapper Cmd");
	private GTextField           _hostmonLocalOsCmdWrapper_txt = new GTextField();

	//---- PCS panel
	@SuppressWarnings("unused")
	private JPanel               _pcsPanel                   = null;
	private ImageIcon            _pcsImageIcon               = SwingUtils.readImageIcon(Version.class, "images/pcs_write_32.png");
	private JLabel               _pcsIcon                    = new JLabel(_pcsImageIcon);
	private MultiLineLabel       _pcsHelp                    = new MultiLineLabel();
	private JLabel               _pcsWriter_lbl              = new JLabel("PCS Writer");
	private JComboBox<String>    _pcsWriter_cbx              = new JComboBox<String>();
	private JLabel               _pcsJdbcDriver_lbl          = new JLabel("JDBC Driver");
	private JComboBox<String>    _pcsJdbcDriver_cbx          = new JComboBox<String>();
	private JLabel               _pcsJdbcUrl_lbl             = new GLabel("JDBC Url"); 
	private JComboBox<String>    _pcsJdbcUrl_cbx             = new JComboBox<String>();
	private JButton              _pcsJdbcUrl_but             = new JButton("...");
	private JLabel               _pcsDbxTuneSaveDir_lbl      = new JLabel("<html><i>${DBXTUNE_SAVE_DIR}</i></html>");
	private JTextField           _pcsDbxTuneSaveDir_txt      = new JTextField();
	private JButton              _pcsDbxTuneSaveDir_but      = new JButton("...");
	private JLabel               _pcsJdbcUsername_lbl        = new JLabel("Username");
	private JTextField           _pcsJdbcUsername_txt        = new JTextField("sa");
	private JLabel               _pcsJdbcPassword_lbl        = new JLabel("Password");
	private JTextField           _pcsJdbcPassword_txt        = null; // set to JPasswordField or JTextField depending on debug level
	private JCheckBox            _pcsJdbcSavePassword_chk    = new JCheckBox("Save password", true);
	private JLabel               _pcsTestConn_lbl            = new JLabel();
	private JButton              _pcsTestConn_but            = new JButton("Test Connection");
	private PcsTable             _pcsSessionTable            = null;
	private JButton              _pcsTabSelectAll_but        = new JButton("Select All");
	private JButton              _pcsTabDeSelectAll_but      = new JButton("Deselect All");
	private JButton              _pcsTabTemplate_but         = new JButton("Reset"); // "Set to Template"
	private JButton              _pcsOpenTcpConfigDialog_but = new JButton("Dialog...");
	private boolean              _useCmForPcsTable           = true;
	// Specific options if we are using H2 as PCS
	private JCheckBox            _pcsH2Option_startH2NetworkServer_chk = new JCheckBox("Start H2 Database as a Network Server", false);
	//---- PCS:DDL Lookup & Store
	private JCheckBox            _pcsDdl_doDdlLookupAndStore_chk             = new JCheckBox("Do DDL lookup and Store", PersistentCounterHandler.DEFAULT_ddl_doDdlLookupAndStore);
	private JCheckBox            _pcsDdl_enabledForDatabaseObjects_chk       = new JCheckBox("DB Objects",              PersistentCounterHandler.DEFAULT_ddl_enabledForDatabaseObjects);
	private JCheckBox            _pcsDdl_enabledForStatementCache_chk        = new JCheckBox("Statement Cache",         PersistentCounterHandler.DEFAULT_ddl_enabledForStatementCache);
	private JLabel               _pcsDdl_afterDdlLookupSleepTimeInMs_lbl     = new JLabel("Sleep Time");
	private JTextField           _pcsDdl_afterDdlLookupSleepTimeInMs_txt     = new JTextField(""+PersistentCounterHandler.DEFAULT_ddl_afterDdlLookupSleepTimeInMs);
	private JCheckBox            _pcsDdl_addDependantObjectsToDdlInQueue_chk = new JCheckBox("Store Dependent Objects", PersistentCounterHandler.DEFAULT_ddl_addDependantObjectsToDdlInQueue);

	//---- PCS: Capture SQL Statements
	private JCheckBox            _pcsCapSql_doSqlCaptureAndStore_chk          = new JCheckBox("Do SQL Capture and Store", PersistentCounterHandler.DEFAULT_sqlCap_doSqlCaptureAndStore);
	private JLabel               _pcsCapSql_sleepTimeInMs_lbl                 = new JLabel("Sleep Time");
	private JTextField           _pcsCapSql_sleepTimeInMs_txt                 = new JTextField(""+PersistentCounterHandler.DEFAULT_sqlCap_sleepTimeInMs);
	private JCheckBox            _pcsCapSql_doSqlText_chk                     = new JCheckBox("SQL Text",       PersistentCounterHandler.DEFAULT_sqlCap_doSqlText);
	private JCheckBox            _pcsCapSql_doStatementInfo_chk               = new JCheckBox("Statement Info", PersistentCounterHandler.DEFAULT_sqlCap_doStatementInfo);
	private JCheckBox            _pcsCapSql_doPlanText_chk                    = new JCheckBox("Plan Text",      PersistentCounterHandler.DEFAULT_sqlCap_doPlanText);
	
	private JLabel               _pcsCapSql_saveStatement_lbl                 = new JLabel("                   But only save Statements if: "); // apces is for "aligning" with the field _pcsCapSql_sendDdlForLookup_chk
	private JLabel               _pcsCapSql_saveStatement_execTime_lbl        = new JLabel("Exec Time is above (ms)");
	private JTextField           _pcsCapSql_saveStatement_execTime_txt        = new JTextField(""+PersistentCounterHandler.DEFAULT_sqlCap_saveStatement_gt_execTime, 3);
	private JLabel               _pcsCapSql_saveStatement_logicalRead_lbl     = new JLabel(", and Logical Reads >");
	private JTextField           _pcsCapSql_saveStatement_logicalRead_txt     = new JTextField(""+PersistentCounterHandler.DEFAULT_sqlCap_saveStatement_gt_logicalReads, 3);
	private JLabel               _pcsCapSql_saveStatement_physicalRead_lbl    = new JLabel(", and Physical Reads >");
	private JTextField           _pcsCapSql_saveStatement_physicalRead_txt    = new JTextField(""+PersistentCounterHandler.DEFAULT_sqlCap_saveStatement_gt_physicalReads, 3);

	private JCheckBox            _pcsCapSql_sendDdlForLookup_chk              = new JCheckBox("Send Statements for DDL Lookup if:", PersistentCounterHandler.DEFAULT_sqlCap_sendDdlForLookup);
	private JLabel               _pcsCapSql_sendDdlForLookup_execTime_lbl     = new JLabel("Exec Time is above (ms)");
	private JTextField           _pcsCapSql_sendDdlForLookup_execTime_txt     = new JTextField(""+PersistentCounterHandler.DEFAULT_sqlCap_sendDdlForLookup_gt_execTime, 3);
	private JLabel               _pcsCapSql_sendDdlForLookup_logicalRead_lbl  = new JLabel(", and Logical Reads >");
	private JTextField           _pcsCapSql_sendDdlForLookup_logicalRead_txt  = new JTextField(""+PersistentCounterHandler.DEFAULT_sqlCap_sendDdlForLookup_gt_logicalReads, 3);
	private JLabel               _pcsCapSql_sendDdlForLookup_physicalRead_lbl = new JLabel(", and Physical Reads >");
	private JTextField           _pcsCapSql_sendDdlForLookup_physicalRead_txt = new JTextField(""+PersistentCounterHandler.DEFAULT_sqlCap_sendDdlForLookup_gt_physicalReads, 3);

	//---- OFFLINE panel
	@SuppressWarnings("unused")
	private JPanel               _offlinePanel                     = null;
	private ImageIcon            _offlineImageIcon                 = SwingUtils.readImageIcon(Version.class, "images/pcs_read_32.png");
	private JLabel               _offlineIcon                      = new JLabel(_offlineImageIcon);
	private MultiLineLabel       _offlineHelp                      = new MultiLineLabel();
	private JLabel               _offlineProfile_lbl               = new JLabel("Use Profile");
	private ProfileComboBoxModel _offlineProfile_mod               = new ProfileComboBoxModel(ConnectionProfile.Type.OFFLINE);
	private JComboBox<String>    _offlineProfile_cbx               = new JComboBox<String>(_offlineProfile_mod);
	private ProfileTypeComboBoxModel _offlineProfileType_mod       = new ProfileTypeComboBoxModel();
	private JComboBox<ProfileType>   _offlineProfileType_cbx       = new JComboBox<ProfileType>(_offlineProfileType_mod);
	private JButton              _offlineProfileSave_but           = new JButton("Save Profile...");
	private JButton              _offlineProfileNew_but            = new JButton("New");
	private JLabel               _offlineJdbcDriver_lbl            = new JLabel("JDBC Driver");
	private JComboBox<String>    _offlineJdbcDriver_cbx            = new JComboBox<String>();
	private JLabel               _offlineJdbcUrl_lbl               = new JLabel("JDBC Url"); 
	private JComboBox<String>    _offlineJdbcUrl_cbx               = new JComboBox<String>();
	private JButton              _offlineJdbcUrl_but               = new JButton("...");
	private JLabel               _offlineJdbcUsername_lbl          = new JLabel("Username");
	private JTextField           _offlineJdbcUsername_txt          = new JTextField("sa");
	private JLabel               _offlineJdbcPassword_lbl          = new JLabel("Password");
	private JTextField           _offlineJdbcPassword_txt          = null; // set to JPasswordField or JTextField depending on debug level
	private JCheckBox            _offlineJdbcSavePassword_chk      = new JCheckBox("Save password", true);
//	private JLabel               _offlineTestConn_lbl              = new JLabel();
	private JButton              _offlineTestConn_but              = new JButton("Test Connection");
//	private JXTable              _offlineSessionTable              = new JXTable();
//	private JButton              _offlineTabSelectAll_but          = new JButton("Select All");
//	private JButton              _offlineTabDeSelectAll_but        = new JButton("Deselect All");
	private JCheckBox            _offlineCheckForNewSessions_chk   = new JCheckBox("Check PCS database for new sample sessions", true);
	// Specific options if we are using H2 as PCS
	private JCheckBox            _offlineH2Option_startH2NwSrv_chk = new JCheckBox("Start H2 Database as a Network Server", false);

	//---- SEND OFFLINE panel
	@SuppressWarnings("unused")
	private JPanel               _sendOfflinePanel            = null;
	private ImageIcon            _sendOfflineImageIcon        = SwingUtils.readImageIcon(Version.class, "images/pcs_send_32.png");
	private JLabel               _sendOfflineIcon             = new JLabel(_sendOfflineImageIcon);
	private MultiLineLabel       _sendOfflineHelp             = new MultiLineLabel();
//	private JLabel               _sendOfflineNotYetImpl1_lbl  = new JLabel("<html><i>NOT YET IMPLEMETED</i></html>", JLabel.CENTER);
	private JLabel               _sendOfflineNotYetImpl1_lbl  = new JLabel("<html><i></i></html>", JLabel.CENTER);
	private JLabel               _sendOfflineNotYetImpl2_lbl  = new JLabel();
	private JButton              _sendOfflineSend_but         = new JButton("Send DB for analyze...");
	private JButton              _sendOfflineTestConn_but     = new JButton("Test Connectivity");

	//---- JDBC panel
	private JPanel               _jdbcPanel            = null;
	private ImageIcon            _jdbcImageIcon        = SwingUtils.readImageIcon(Version.class, "images/jdbc_connect_32.png");
	private JLabel               _jdbcIcon             = new JLabel(_jdbcImageIcon);
	private MultiLineLabel       _jdbcHelp             = new MultiLineLabel();
	private JLabel               _jdbcProfile_lbl      = new JLabel("Use Profile");
	private ProfileComboBoxModel _jdbcProfile_mod      = new ProfileComboBoxModel(ConnectionProfile.Type.JDBC);
	private JComboBox<String>    _jdbcProfile_cbx      = new JComboBox<String>(_jdbcProfile_mod);
	private ProfileTypeComboBoxModel _jdbcProfileType_mod = new ProfileTypeComboBoxModel();
	private JComboBox<ProfileType>   _jdbcProfileType_cbx = new JComboBox<ProfileType>(_jdbcProfileType_mod);
	private JButton              _jdbcProfileSave_but  = new JButton("Save Profile...");
	private JButton              _jdbcProfileNew_but   = new JButton("New");
	private JLabel               _jdbcDriver_lbl       = new JLabel("JDBC Driver");
	private JComboBox<String>    _jdbcDriver_cbx       = new JComboBox<String>();
	private JLabel               _jdbcUrl_lbl          = new JLabel("JDBC Url"); 
	private JComboBox<String>    _jdbcUrl_cbx          = new JComboBox<String>();
	private JButton              _jdbcUrl_but          = new JButton("...");
	private JLabel               _jdbcUsername_lbl     = new JLabel("Username");
	private JTextField           _jdbcUsername_txt     = new JTextField("sa");
	private JLabel               _jdbcPassword_lbl     = new JLabel("Password");
	private JTextField           _jdbcPassword_txt     = null; // set to JPasswordField or JTextField depending on debug level
	private JCheckBox            _jdbcSavePassword_chk = new JCheckBox("Save password", true);
//	private JLabel               _jdbcTestConn_lbl     = new JLabel();
	private JButton              _jdbcDriverInfo_but   = new JButton("JDBC Driver Info...");
	private JButton              _jdbcTestConn_but     = new JButton("Test Connection");
	private JLabel               _jdbcSqlInit_lbl      = new JLabel("SQL Init");
	private JTextField           _jdbcSqlInit_txt      = new JTextField("");
	private JLabel               _jdbcUrlOptions_lbl   = new JLabel("URL Options");
	private JTextField           _jdbcUrlOptions_txt   = new JTextField("");
	private JButton              _jdbcUrlOptions_but   = new JButton("...");	
	private SshTunnelInfo        _jdbcSshTunnelInfo       = null; // always NULL for the moment, since this isn't supported for the moment
	private JCheckBox            _jdbcSshTunnel_chk       = new JCheckBox("Use SSH (Secure Shell) Tunnel to connect to DBMS", DEFAULT_CONN_SSH_TUNNEL);
	private JLabel               _jdbcSshTunnelDesc_lbl   = new JLabel();
	private JButton              _jdbcSshTunnel_but       = new JButton("SSH Settings...");

	//---- JDBC proprietary fields
	private JCheckBox            _jdbcSqlServerUseWindowsAuthentication_chk  = new JCheckBox("Use Windows Authentication", DEFAULT_CONN_SQLSERVER_WIN_AUTH);
	private JCheckBox            _jdbcSqlServerUseEncrypt_chk                = new JCheckBox("Encrypt"                   , DEFAULT_CONN_SQLSERVER_ENCRYPT);
	private JCheckBox            _jdbcSqlServerUseTrustServerCertificate_chk = new JCheckBox("Trust Certificate"         , DEFAULT_CONN_SQLSERVER_TRUST_CERT);

	//---- JDBC Driver Info panel
	private JPanel               _jdbcDriverInfoPanel      = null;
	private ImageIcon            _jdbcDriverInfoImageIcon  = SwingUtils.readImageIcon(Version.class, "images/jdbc_driver_32.png");
	private JLabel               _jdbcDriverInfoIcon       = new JLabel(_jdbcDriverInfoImageIcon);
	private MultiLineLabel       _jdbcDriverInfoHelp       = new MultiLineLabel();
//	private GTable               _jdbcDiTable              = new GTable();
//	private DefaultTableModel    _jdbcDiTableModel         = new DefaultTableModel();
//	private JButton              _jdbcDiAddDriver_but      = new JButton("Add Driver");

	//---- Buttons at the bottom
	private JLabel               _ok_lbl             = new JLabel(""); // Problem description if _ok is disabled
	private JButton              _ok                 = new JButton("OK");
	private JButton              _cancel             = new JButton("Cancel");

////	private boolean              _checkAseCfg        = true;
//	private ConnectionProgressExtraActions _srvExtraChecks = null;
//	private boolean              _showAseTab         = true;
//	private boolean              _showHostmonTab     = true;
//	private boolean              _showPcsTab         = true;
//	private boolean              _showOfflineTab     = true;
//	private boolean              _showJdbcTab        = false;
//
//	private boolean              _showAseTuneOptions = true;
//	private boolean              _showDbxTuneOptionsInJdbc = false;
	private Options              _options = null;

//	private static ConnectionDialog   _instance       = null;
//	public static ConnectionDialog getInstance()
//	{
//		return _instance;
//	}

	// When we actually did a connection, this will hold the Connection Profile Type NAME, if none was used, then it will hold null
	private String _usedConnectionProfileTypeName = null;
	// When we actually did a connection, this will hold the Connection Profile      NAME, if none was used, then it will hold null
	private String _usedConnectionProfileName = null;

	// Only do this ONCE
	// If we have saved 'DBXTUNE_SAVE_DIR' in this dialog, then set it to the System Property
	static
	{
		String dbxTuneSaveDir = Configuration.getCombinedConfiguration().getProperty("DBXTUNE_SAVE_DIR");
		if (StringUtil.hasValue(dbxTuneSaveDir))
		{
			_logger.info("Setting 'DBXTUNE_SAVE_DIR' in the System Properties to value '"+dbxTuneSaveDir+"'.");
			System.setProperty("DBXTUNE_SAVE_DIR", dbxTuneSaveDir);
		}
	}


	/**
	 * Options that can be passed to the Connection Dialog, what should be visible etc...
	 */
	public static class Options
	{
		public String               _dialogTitlePostfix = null;
		public ConnectionProgressExtraActions _srvExtraChecks = null;
		public boolean              _showAseTab         = true;
		public boolean              _showHostmonTab     = true;
		public boolean              _showPcsTab         = true;
		public boolean              _showOfflineTab     = true;
		public boolean              _showJdbcTab        = false;

		public boolean              _showDbxTuneOptionsInTds  = true;
		public boolean              _showDbxTuneOptionsInJdbc = false;
		
		public Options()
		{
		}
		public Options(ConnectionProgressExtraActions srvExtraChecks, boolean showAseTab, boolean showAseOptions, boolean showHostmonTab, boolean showPcsTab, boolean showOfflineTab, boolean showJdbcTab, boolean showJdbcOptions)
		{
			_srvExtraChecks           = srvExtraChecks;
			_showAseTab               = showAseTab;
			_showDbxTuneOptionsInTds  = showAseOptions;
			_showHostmonTab           = showHostmonTab;
			_showPcsTab               = showPcsTab;
			_showOfflineTab           = showOfflineTab;
			_showJdbcTab              = showJdbcTab;
			_showDbxTuneOptionsInJdbc = showJdbcOptions;
		}
	}

	public static DbxConnection showTdsOnlyConnectionDialog(Frame owner)
	{
//		ConnectionDialog connDialog = new ConnectionDialog(null, false, true, false, false, false, false, false);
//		ConnectionDialog connDialog = new ConnectionDialog(null, null, true, false, false, false, false, false, false);
		ConnectionDialog connDialog = new ConnectionDialog(owner, new Options(null, true, false, false, false, false, false, false));
		connDialog.setVisible(true);
		connDialog.dispose();

		int connType = connDialog.getConnectionType();
		
		if ( connType == ConnectionDialog.CANCEL)
			return null;

		if ( connType == ConnectionDialog.TDS_CONN)
			return connDialog.getAseConn();

		return null;
	}

	private static String createDialogTitle(Options options)
	{
		if (options != null && StringUtil.hasValue(options._dialogTitlePostfix))
			return "Connect - " + options._dialogTitlePostfix;

		return "Connect";
	}
	public ConnectionDialog(Window owner)
	{
		this(owner, null);
	}
	public ConnectionDialog(Window owner, Options options)
	{
		super(owner, createDialogTitle(options), ModalityType.DOCUMENT_MODAL);
//		super(owner, "Connect", true);
//		_instance = this;
		_owner = owner;

		if (owner == null)
			_useCmForPcsTable = false;

		_options = options;
		if (_options == null)
			_options = new Options();

		// FIXME: add all of the below to a class, which is passed as the constructor...
////		_checkAseCfg        = checkAseCfg;
//		_srvExtraChecks     = srvExtraChecks;
//		_showAseTab         = showAseTab;
//		_showAseTuneOptions = showAseOptions;
//		_showHostmonTab     = showHostmonTab;
//		_showPcsTab         = showPcsTab;
//		_showOfflineTab     = showOfflineTab;
//		_showJdbcTab        = showJdbcTab;
//		_showDbxTuneOptionsInJdbc = showJdbcOptions; // NOT YET IMPLEMENTED

//		_inputMap = input;
		initComponents();
		pack();
		
		Dimension size = getPreferredSize();
//		size.width += 100;
//		size.width = 600; // if not set the window will grow to wide...
		size.width = SwingUtils.hiDpiScale(950); // if not set the window will grow to wide...

//		setPreferredSize(size);
//		setMinimumSize(size);
		setSize(size);

		toggleCounterStorageTab();
		toggleHostmonTab();

		setLocationRelativeTo(owner);

		getSavedWindowProps();
		// Check size, if below minimum after "upgrade" using ConnectionProfile, make it approximate 150px bigger
		size = getSize();
		if (_connProfileVisible_chk.isSelected() && size.width < SwingUtils.hiDpiScale(675))
		{
			size.width += SwingUtils.hiDpiScale(150);
			setSize(size);
		}

// TEMP CHANGED: THIS SHOULD BE DELETED... when the ConnectionProfile Works
//if (_connProfileVisible_chk.isSelected())
//	_connProfileVisible_chk.doClick();

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
		
		_treeExpansionListenerEnabled = false;
		ConnectionProfileManager.getInstance().setTreeModelFilterOnProductName(productName);
		restoreToSavedTreeExpansionState(_connProfileTree);
		_treeExpansionListenerEnabled = true;
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

		String productName = null;

		try
		{
			productName = conn.getMetaData().getDatabaseProductName();
			_logger.debug("getDatabaseProductName() returns: '"+productName+"'.");
			return productName; 
		}
		catch (SQLException e)
		{
			// If NO metadata installed, check if it's a Sybase Replication Server.
			// JZ0SJ: Metadata accessor information was not found on this database. Please install the required tables as mentioned in the jConnect documentation.
			if ( "JZ0SJ".equals(e.getSQLState()) )
			{
//				try
//				{
//					String str1 = "";
//					String str2 = "";
//					Statement stmt = conn.createStatement();
//					ResultSet rs = stmt.executeQuery("admin rssd_name");
//					while ( rs.next() )
//					{
//						str1 = rs.getString(1);
//						str2 = rs.getString(2);
//					}
//					rs.close();
//					stmt.close();
//
//					_logger.info("Replication Server with RSSD at '"+str1+"."+str2+"'.");
//
//					// If the above statement succeeds, then it must be a RepServer without metadata installed.
//					return DbUtils.DB_PROD_NAME_SYBASE_RS;
//				}
//				catch(SQLException ignoreRsExceptions) {}

				// Check for Replication Server
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
					productName = DbUtils.DB_PROD_NAME_SYBASE_RS;
				}
				catch(SQLException ignoreRsExceptions) {_logger.debug("getDatabaseProductName(): at RS", ignoreRsExceptions);}
//				catch(SQLException ignoreRsExceptions) {System.out.println("getDatabaseProductName(): at RS, caught: "+ignoreRsExceptions);}

				// Check for Replication Agent
				if (StringUtil.isNullOrBlank(productName))
				{
					try
					{
						String str1 = "";
						Statement stmt = conn.createStatement();
						ResultSet rs = stmt.executeQuery("ra_version");
						while ( rs.next() )
						{
							str1 = rs.getString(1);
						}
						rs.close();
						stmt.close();
						
						_logger.info("Replication Agent Version '"+str1+"'.");
						
						// If the above statement succeeds, then it must be a RepServer without metadata installed.
						productName = DbUtils.DB_PROD_NAME_SYBASE_RAX;
					}
					catch(SQLException ignoreRsExceptions) {_logger.debug("getDatabaseProductName(): at RepAgent", ignoreRsExceptions);}
//					catch(SQLException ignoreRsExceptions) {System.out.println("getDatabaseProductName(): at RepAgent, caught: "+ignoreRsExceptions);}
				}

				// Check for DR Agent (Disaster Recovery AGent)
				if (StringUtil.isNullOrBlank(productName))
				{
					try
					{
						String str1 = "";
						String str2 = "";
						Statement stmt = conn.createStatement();
						ResultSet rs = stmt.executeQuery("sap_version");
						while ( rs.next() )
						{
							str1 = rs.getString(1);
							str2 = rs.getString(2);
							
							_logger.info("DR Agent Version info type='"+str1+"', version='"+str2+"'.");

							if ("DR Agent".equals(str1))
							{
								// If the above statement succeeds, then it must be a RepServer without metadata installed.
								productName = DbUtils.DB_PROD_NAME_SYBASE_RSDRA;
							}
						}
						rs.close();
						stmt.close();
					}
					catch(SQLException ignoreRsExceptions) {_logger.debug("getDatabaseProductName(): at DR Agent", ignoreRsExceptions);}
//					catch(SQLException ignoreRsExceptions) {System.out.println("getDatabaseProductName(): at DR Agent, caught: "+ignoreRsExceptions);}
				}

				// Check for ??? ... @@version (possibly IQ or any other Sybase TDS service) 
				if (StringUtil.isNullOrBlank(productName))
				{
    				try
    				{
    					String str1 = "";
    					Statement stmt = conn.createStatement();
    					ResultSet rs = stmt.executeQuery("select @@version");
    					while ( rs.next() )
    					{
    						str1 = rs.getString(1);
    						
        					_logger.info("unknown-srv-type: @@version='"+str1+"'.");

    						if (StringUtil.hasValue(str1))
    						{
    							if (str1.startsWith("Sybase IQ/"))
    								productName = DbUtils.DB_PROD_NAME_SYBASE_IQ;

    							if (str1.startsWith("SAP IQ/"))
    								productName = DbUtils.DB_PROD_NAME_SYBASE_IQ;
    						}
    					}
    					rs.close();
    					stmt.close();
    				}
    				catch(SQLException ignoreRsExceptions) {}
				}

				if (StringUtil.hasValue(productName))
					return productName;
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
				// Check for Replication Server
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

				// Check for Replication Agent
				try
				{
					String str1 = "";
					Statement stmt = conn.createStatement();
					ResultSet rs = stmt.executeQuery("ra_version");
					while ( rs.next() )
					{
						str1 = rs.getString(1);
					}
					rs.close();
					stmt.close();

					_logger.info("Replication Agent Version '"+str1+"'.");

					// If the above statement succeeds, then it must be a RepServer without metadata installed.
					return str1;
				}
   				catch(SQLException ignoreRsExceptions) {_logger.debug("getDatabaseProductVersion(): at RepAgent", ignoreRsExceptions);}

				// Check for DR Agent (Disaster Recovery AGent)
				try
				{
					String str1 = "";
					String str2 = "";
					Statement stmt = conn.createStatement();
					ResultSet rs = stmt.executeQuery("sap_version");
					while ( rs.next() )
					{
						str1 = rs.getString(1);
						str2 = rs.getString(2);
						
						_logger.info("DR Agent Version info type='"+str1+"', version='"+str2+"'.");

						if ("DR Agent".equals(str1))
						{
	    					// If the above statement succeeds, then it must be a RepServer without metadata installed.
	    					return str2;
						}
					}
					rs.close();
					stmt.close();
				}
				catch(SQLException ignoreRsExceptions) {_logger.debug("getDatabaseProductVersion(): at DR Agent", ignoreRsExceptions);}
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
		// MaxDB
		else if (DbUtils.DB_PROD_NAME_MAXDB.equals(currentDbProductName))
		{
			serverName = DbUtils.getMaxDbServername(conn);
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
		// DB2
		else if (DbUtils.isProductName(currentDbProductName, DbUtils.DB_PROD_NAME_DB2_LUW))
		{
			serverName = DbUtils.getDb2Servername(conn);
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

		try
		{
			String str = conn.getMetaData().getDriverName();
			_logger.debug("getDriverName() returns: '"+str+"'.");
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
						_logger.debug("getDriverName(): RepServer check, using version "+str);
					}
					rs.close();
					stmt.close();

					// If the above statement succeeds, then it must be a RepServer without metadata installed.
					//return "getDriverName(): CONSTANT-ReplicationServer";
					return "jConnect (TM) for JDBC (TM)";
				}
				catch(SQLException ignoreRsExceptions) {}
			}
			_logger.debug("getDriverName() Caught: "+e, e);
			throw e;
		}
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

		try
		{
    		String str = conn.getMetaData().getDriverVersion();
    		_logger.debug("getDriverVersion() returns: '"+str+"'.");
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
						_logger.debug("getDriverVersion(): RepServer check, using version "+str);
					}
					rs.close();
					stmt.close();

					// If the above statement succeeds, then it must be a RepServer without metadata installed.
//					return "getDriverVersion(): CONSTANT-ReplicationServer";
					return "jConnect (TM) for JDBC(TM)/unknown/getDriverVersion()/getSQLState()=JZ0SJ: Metadata accessor information was not found on this database";
				}
				catch(SQLException ignoreRsExceptions) {}
			}
			_logger.debug("getDriverVersion() Caught: "+e, e);
			throw e;
		}
	}

	/**
	 * Get Connection, first TDS, then OFFLINE, then JDBC
	 * @return
	 */
	public DbxConnection getConnection()
	{
		DbxConnection conn = getAseConn();
		if (conn == null) conn = getOfflineConn();
		if (conn == null) conn = getJdbcConn();
		return conn;
	}

	public int                      getConnectionType() { return _connectionType; }
	public DbxConnection            getAseConn()        { return _aseConn; }
//	public SshConnection            getSshConn()        { return _sshConn; }
	public HostMonitorConnection    getHostMonConn()    { return _hostMonConn; }
//	public Connection               getPcsConn()        { return _pcsConn; }
//	public PersistentCounterHandler getPcsWriter()      { return _pcsWriter; }
	public DbxConnection            getOfflineConn()    { return _offlineConn; }
	public DbxConnection            getJdbcConn()       { return _jdbcConn; }

	public boolean                  isAseSshTunnelSelected()  { return _aseSshTunnel_chk.isSelected(); }
	public SshTunnelInfo            getAseSshTunnelInfo()     { return _aseSshTunnelInfo; }
	public boolean                  isJdbcSshTunnelSelected() { return _jdbcSshTunnel_chk.isSelected(); }
	public SshTunnelInfo            getJdbcSshTunnelInfo()    { return _jdbcSshTunnelInfo; }

	public Date getDisConnectTime()
	{
		return _disConnectTime;
	}

	public void setAseUsername(String username) { _aseUsername_txt.setText(username); }
	public void setAsePassword(String passwd)   { _asePassword_txt.setText(passwd); }
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
	
	public String getAseUsername() { return _aseUsername_txt.getText(); }
	public String getAsePassword() { return _asePassword_txt.getText(); }
	public String getAseServer  () { return StringUtil.getSelectedItemString(_aseServer_cbx); }

	/** depending on what we have connected to give the user name we connected as 
	 * @return null if not connected, else user name*/
	public String getUsername() 
	{ 
		if (getConnectionType() == TDS_CONN)
			return _aseUsername_txt.getText(); 

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
			return StringUtil.getSelectedItemString(_offlineJdbcUrl_cbx);

		if (getConnectionType() == JDBC_CONN)
			return StringUtil.getSelectedItemString(_jdbcUrl_cbx);

		return null;
	}

	public void setSshUsername(String username) { _hostmonUsername_txt.setText(username); }
	public void setSshPassword(String password) { _hostmonPassword_txt.setText(password); }
	public void setSshHostname(String hostname) { _hostmonHost_txt    .setText(hostname); }
	public void setSshPort(String portStr)      { _hostmonPort_txt    .setText(portStr); }
	public void setSshKeyFile(String keyFile)   { _hostmonKeyFile_txt .setText(keyFile); }

	public String getSshUsername() { return _hostmonUsername_txt.getText(); }
	public String getSshPassword() { return _hostmonPassword_txt.getText(); }
	public String getSshHostname() { return _hostmonHost_txt    .getText(); }
	public String getSshPortStr()  { return _hostmonPort_txt    .getText(); }
	public String getSshKeyFile()  { return _hostmonKeyFile_txt .getText(); }

	public boolean  isHostMonEnabled()           { return _aseHostMonitor_chk          .isSelected(); }
	public boolean  isHostMonLocalOsCmd()        { return _hostMonLocalOsCmd_chk       .isSelected(); }
	public String  getHostMonLocalOsCmdWrapper() { return _hostmonLocalOsCmdWrapper_txt.getText(); }
	
	public String getOfflineJdbcDriver() { return _offlineJdbcDriver_cbx  .getEditor().getItem().toString(); }
	public String getOfflineJdbcUrl()    { return _offlineJdbcUrl_cbx     .getEditor().getItem().toString(); }
	public String getOfflineJdbcUser()   { return _offlineJdbcUsername_txt.getText(); }
	public String getOfflineJdbcPasswd() { return _offlineJdbcPassword_txt.getText(); }

	public void setOffflineJdbcDriver(String driver)   { addAndSelectItem(_offlineJdbcDriver_cbx, driver); }
	public void setOffflineJdbcUser  (String username) { _offlineJdbcUsername_txt.setText(username); }
	public void setOffflineJdbcPasswd(String password) { _offlineJdbcPassword_txt.setText(password); }
	public void setOffflineJdbcUrl   (String url)      
	{ 
		// Set the driver name if it's not set.
		if (StringUtil.isNullOrBlank(getOfflineJdbcDriver()))
		{
			String driver = JdbcDriverHelper.guessDriverForUrl(url);
			setOffflineJdbcDriver(driver);
		}
		// AFTER the driver, set the URL (other way around: when setting driver, a default template will be choosen)
		addAndSelectItem(_offlineJdbcUrl_cbx, url); 
	}

	public String getJdbcDriver() { return _jdbcDriver_cbx  .getEditor().getItem().toString(); }
	public String getJdbcUrl()    { return _jdbcUrl_cbx     .getEditor().getItem().toString(); }
	public String getJdbcUser()   { return _jdbcUsername_txt.getText(); }
	public String getJdbcPasswd() { return _jdbcPassword_txt.getText(); }

	public void setJdbcDriver  (String driver)   { addAndSelectItem(_jdbcDriver_cbx, driver); }
	public void setJdbcUrl     (String url)      { addAndSelectItem(_jdbcUrl_cbx,    url); }
	public void setJdbcUsername(String username) { _jdbcUsername_txt.setText(username); }
	public void setJdbcPassword(String password) { _jdbcPassword_txt.setText(password); }

	private void addAndSelectItem(JComboBox<String> cbx, String item)
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
		{
			cbx.addItem(item);
		}
		
		cbx.setSelectedItem(item);
	}

	/**
	 * Set a Connection profile to use when connecting
	 * @param name Name of the profile
	 */
	public void setConnProfileName(String name)
	{
		ConnectionProfileManager cpm = ConnectionProfileManager.getInstance();
		ConnectionProfile cp = cpm.getProfile(name);
		
		if (cp == null)
		{
			SwingUtils.showErrorMessage(this, "Connect", "Connection Profile '"+name+"' was not found.", null);;
			//throw new SQLException("Connection Profile '"+profileName+"' was not found.");
			return;
		}

		// Load the connection Profile, and switch to the correct tab
		load(cp);
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
		aseDeferredConnectChkAction(null);
		aseDeferredDisConnectChkAction(null);

		// Save window & divider location etc... 
		_connTabbedPanel.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				saveWindowProps();
			}
		});
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

		// Get tree from the Connection Profile Manager
//		DefaultMutableTreeNode cpRoot = ConnectionProfileManager.getInstance().getConnectionProfileTreeNode();
//		if (cpRoot != null && cpRoot.getChildCount() > 0)
//			root = cpRoot;

		boolean showRootNode = Configuration.getCombinedConfiguration().getBooleanProperty(ConnectionProfileManager.PROPKEY_connProfile_show_rootNode, ConnectionProfileManager.DEFAULT_connProfile_show_rootNode);
		
//		_connProfileTree = new JTree(root);
		_connProfileTree = new GTree(ConnectionProfileManager.getInstance().getConnectionProfileTreeModel(_desiredProductName));
		_connProfileTree.setRootVisible(false);
//		_connProfileTree.setRootVisible(showRootNode);
		_connProfileTree.setShowsRootHandles(showRootNode);
		_connProfileTree.setDragEnabled(true);  
		_connProfileTree.setDropMode(DropMode.ON_OR_INSERT);
		_connProfileTree.setTransferHandler(new TreeTransferHandler());
		_connProfileTree.getSelectionModel().setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);  
		restoreToSavedTreeExpansionState(_connProfileTree);

		// Right click Pop-up Menu and there Actions
		JPopupMenu popup = new JPopupMenu();
		final AbstractAction connProfileLoadAction             = new ConnProfileLoadAction();
		final AbstractAction connProfileLoadOnNodeSelectAction = new ConnProfileLoadOnNodeSelectAction();
		final AbstractAction connProfileConnectAction          = new ConnProfileConnectAction();
		final AbstractAction connProfileCopyNameAction         = new ConnProfileCopyNameAction();
		final AbstractAction connProfileAddProfileAction       = new ConnProfileAddProfileAction();
		final AbstractAction connProfileAddCatalogAction       = new ConnProfileAddCatalogAction();
		final AbstractAction connProfileDeleteAction           = new ConnProfileDeleteAction();
		final AbstractAction connProfileRenameAction           = new ConnProfileRenameAction();
		final AbstractAction connProfileDuplicateAction        = new ConnProfileDuplicateAction();
		final AbstractAction connProfileMoveAction             = new ConnProfileMoveAction();
		final AbstractAction connProfileReLoadAction           = new ConnProfileReloadAction();
		final AbstractAction connProfileChangeFile             = new ConnProfileChangeFileAction();
		final AbstractAction connProfileShowAddProfile         = new ConnProfileShowAddProfileAction();
		final AbstractAction connProfileShowAddIFile           = new ConnProfileShowAddIFileAction();
		final AbstractAction connProfileShowSaveChanges        = new ConnProfileShowSaveChangesAction();
		final AbstractAction connProfileDefaultSaveChanges     = new ConnProfileDefaultSaveChangesAction();
		final AbstractAction connProfileShowTreeRoot           = new ConnProfileShowTreeRootAction();
		
		final JCheckBoxMenuItem connProfileLoadOnNodeSelect_mi   = new JCheckBoxMenuItem(connProfileLoadOnNodeSelectAction);
		final JCheckBoxMenuItem connProfileShowAddProfile_mi     = new JCheckBoxMenuItem(connProfileShowAddProfile);
		final JCheckBoxMenuItem connProfileShowAddIFile_mi       = new JCheckBoxMenuItem(connProfileShowAddIFile);
		final JCheckBoxMenuItem connProfileShowSaveChanges_mi    = new JCheckBoxMenuItem(connProfileShowSaveChanges);
		final JCheckBoxMenuItem connProfileDefaultSaveChanges_mi = new JCheckBoxMenuItem(connProfileDefaultSaveChanges);
		final JCheckBoxMenuItem connProfileShowTreeRoot_mi       = new JCheckBoxMenuItem(connProfileShowTreeRoot);

		popup.add(new JMenuItem(connProfileLoadAction));
		popup.add(connProfileLoadOnNodeSelect_mi);
		popup.add(new JMenuItem(connProfileConnectAction));
		popup.add(new JMenuItem(connProfileCopyNameAction));
		popup.add(new JSeparator());
		popup.add(new JMenuItem(connProfileAddProfileAction));
		popup.add(new JMenuItem(connProfileAddCatalogAction));
		popup.add(new JMenuItem(connProfileDeleteAction));
		popup.add(new JMenuItem(connProfileRenameAction));
		popup.add(new JMenuItem(connProfileDuplicateAction));
		popup.add(new JMenuItem(connProfileMoveAction));
		popup.add(new JSeparator());
		popup.add(new JMenuItem(connProfileReLoadAction));
		popup.add(new JMenuItem(connProfileChangeFile));
		popup.add(connProfileShowAddProfile_mi);
		popup.add(connProfileShowAddIFile_mi);
		popup.add(connProfileShowSaveChanges_mi);
		popup.add(connProfileDefaultSaveChanges_mi);
		popup.add(connProfileShowTreeRoot_mi);
		
		popup.addPopupMenuListener(new PopupMenuListener()
		{
			@Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
			@Override public void popupMenuCanceled(PopupMenuEvent e) {}

			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e)
			{
				connProfileLoadAction         .setEnabled(false);
				connProfileConnectAction      .setEnabled(false);
				connProfileAddProfileAction   .setEnabled(true);  // always to true
				connProfileAddCatalogAction   .setEnabled(true);  // always to true
				connProfileDeleteAction       .setEnabled(false);
				connProfileRenameAction       .setEnabled(false);
				connProfileDuplicateAction    .setEnabled(false);
				connProfileMoveAction         .setEnabled(false);
				connProfileChangeFile         .setEnabled(true);  // always to true

				// Disable components if nothing is selected
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) _connProfileTree.getLastSelectedPathComponent();
				if (node != null)
				{
					Object o = node.getUserObject();

					connProfileDeleteAction       .setEnabled(true);
					connProfileRenameAction       .setEnabled(true);
					connProfileDuplicateAction    .setEnabled(true);
					connProfileMoveAction         .setEnabled(true);

					if (o instanceof ConnectionProfile)
					{
						connProfileLoadAction   .setEnabled(true);
						connProfileConnectAction.setEnabled(true);
					}
					else if (o instanceof ConnectionProfileCatalog)
					{
					}
					else
					{
					}
				}
				
				// Set current settings for: AutoAddProfile, AutoAddIFile
				Configuration conf = Configuration.getCombinedConfiguration();
				boolean loadOnNodeSelect   = conf.getBooleanProperty(ConnectionProfileManager.PROPKEY_connProfile_load_onNodeSelection, ConnectionProfileManager.DEFAULT_connProfile_load_onNodeSelection);
				boolean showAddProfile     = conf.getBooleanProperty(ConnectionProfileManager.PROPKEY_connProfile_serverAdd_showDialog, ConnectionProfileManager.DEFAULT_connProfile_serverAdd_showDialog);
				boolean showAddIFile       = conf.getBooleanProperty(ConnectionProfileManager.PROPKEY_ifile_serverAdd_showDialog,       ConnectionProfileManager.DEFAULT_ifile_serverAdd_showDialog);
				boolean showSaveChanges    = conf.getBooleanProperty(ConnectionProfileManager.PROPKEY_connProfile_changed_showDialog,   ConnectionProfileManager.DEFAULT_connProfile_changed_showDialog);
				boolean defaultSaveChanges = conf.getBooleanProperty(ConnectionProfileManager.PROPKEY_connProfile_changed_alwaysSave,   ConnectionProfileManager.DEFAULT_connProfile_changed_alwaysSave);
				boolean showRootNode       = conf.getBooleanProperty(ConnectionProfileManager.PROPKEY_connProfile_show_rootNode,        ConnectionProfileManager.DEFAULT_connProfile_show_rootNode);
				
				connProfileLoadOnNodeSelect_mi  .setSelected(loadOnNodeSelect);
				connProfileShowAddProfile_mi    .setSelected(showAddProfile);
				connProfileShowAddIFile_mi      .setSelected(showAddIFile);
				connProfileShowSaveChanges_mi   .setSelected(showSaveChanges);
				connProfileDefaultSaveChanges_mi.setSelected(defaultSaveChanges);
				connProfileShowTreeRoot_mi      .setSelected(showRootNode);
			}
		});
		_connProfileTree.setComponentPopupMenu(popup);

		// Register the Tree at the tooltip manager, otherwise subcomponents wont fire tooltip...
		ToolTipManager.sharedInstance().registerComponent(_connProfileTree);
		
		// If the underlying model changes
		_connProfileTree.getModel().addTreeModelListener(new TreeModelListener()
		{
			@Override
			public void treeStructureChanged(TreeModelEvent e)
			{
//				System.out.println("treeStructureChanged() TreeModelEvent="+e);
				ConnectionProfileManager.getInstance().save();

				// Refresh the Profile drop downs
				_aseProfile_mod    .refresh();
				_jdbcProfile_mod   .refresh();
				_offlineProfile_mod.refresh();
			}
			
			@Override
			public void treeNodesRemoved(TreeModelEvent e)
			{
//				System.out.println("treeNodesRemoved() TreeModelEvent="+e);
				ConnectionProfileManager.getInstance().save();

				// Refresh the Profile drop downs
				_aseProfile_mod    .refresh();
				_jdbcProfile_mod   .refresh();
				_offlineProfile_mod.refresh();
			}
			
			@Override
			public void treeNodesInserted(TreeModelEvent e)
			{
//				System.out.println("treeNodesInserted() TreeModelEvent="+e);
				ConnectionProfileManager.getInstance().save();

				// Refresh the Profile drop downs
				_aseProfile_mod    .refresh();
				_jdbcProfile_mod   .refresh();
				_offlineProfile_mod.refresh();
			}
			
			@Override
			public void treeNodesChanged(TreeModelEvent e)
			{
//				System.out.println("treeNodesChanged() TreeModelEvent="+e);
				ConnectionProfileManager.getInstance().save();

				// Refresh the Profile drop downs
				_aseProfile_mod    .refresh();
				_jdbcProfile_mod   .refresh();
				_offlineProfile_mod.refresh();
			}
		});
		
		// Render object in the Tree
		_connProfileTree.setRowHeight(0);
		_connProfileTree.setCellRenderer(new DefaultTreeCellRenderer()
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
							l.setIcon(ConnectionProfileManager.getIcon16(t._vendor));
							l.setText(t._name);
							l.setToolTipText(null);

							return l;
						}
					}
					if (o instanceof ConnectionProfile) 
					{
						if (comp instanceof JLabel)
						{
							JLabel l = (JLabel) comp;
							ConnectionProfile t = (ConnectionProfile) o;
//							ImageIcon icon = ConnectionProfileManager.getIcon16(t.getSrvType());
							ProfileType connProfileType = ConnectionProfileManager.getInstance().getProfileTypeByName(t.getProfileTypeName());
							Color indicatorColor = (connProfileType == null ? null : connProfileType._color);
							l.setIcon(SwingUtils.paintIcon(ConnectionProfileManager.getIcon16(t.getSrvType()), indicatorColor, 3, false));
							l.setText(t.getName());
							l.setToolTipText(t.getToolTipText());

							return l;
						}
					}
					if (o instanceof ConnectionProfileCatalog) 
					{
						if (comp instanceof JLabel)
						{
							JLabel l = (JLabel) comp;
							ConnectionProfileCatalog t = (ConnectionProfileCatalog) o;
							l.setIcon(t.getIcon());
							l.setText(t.getName());
							l.setToolTipText(null);

							return l;
						}
					}
				} 
				return comp;
			}
		});

		// Add mouse listener: set selection on right clicks
		_connProfileTree.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e) 
			{
				TreePath selPath = _connProfileTree.getPathForLocation(e.getX(), e.getY());
				if(selPath != null) 
				{
					if (SwingUtilities.isLeftMouseButton(e)) 
					{
					}
					else
					{
						_connProfileTree.setSelectionPath(selPath);
					}
				}
			}
		});

		// Add keyboard listener: ENTER-KEY = connect 
		_connProfileTree.addKeyListener(new KeyListener()
		{
			@Override public void keyTyped(KeyEvent e)   {}
			@Override public void keyPressed(KeyEvent e) {}
			@Override public void keyReleased(KeyEvent e) 
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) _connProfileTree.getLastSelectedPathComponent();
					Object o = node.getUserObject();
					if (o instanceof ConnectionProfile)
						connect( (ConnectionProfile) o );
				}
			}
		});

		// Normally 500ms
//		int nodeSelectionTimerInterval = (Integer) Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval");
		int nodeSelectionTimerInterval = 350;
		final Timer nodeSelectionTimer = new Timer(nodeSelectionTimerInterval, new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				_logger.debug("nodeSelectionTimer() FIRED");

				Configuration conf = Configuration.getCombinedConfiguration();
				boolean loadOnNodeSelection  = conf.getBooleanProperty(ConnectionProfileManager.PROPKEY_connProfile_load_onNodeSelection, ConnectionProfileManager.DEFAULT_connProfile_load_onNodeSelection);

				if (loadOnNodeSelection)
				{
					// Remember current component in focus
					final Component hasFocus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();

					DefaultMutableTreeNode node = (DefaultMutableTreeNode) _connProfileTree.getLastSelectedPathComponent();
					if (node == null)
						return;

					Object o = node.getUserObject();
					if (o != null && o instanceof ConnectionProfile)
						load((ConnectionProfile)o);
					
					// RESTORE focused component before the profile was loaded
					// this so we can continue to key-navigate in the tree
					if (hasFocus != null)
					{
						SwingUtilities.invokeLater(new Runnable()
						{
							@Override
							public void run()
							{
								hasFocus.requestFocus();
							}
						});
					}
				}
			}
		});
		nodeSelectionTimer.setRepeats(false);
		_logger.debug("nodeSelectionTimerInterval="+nodeSelectionTimerInterval);

		// Tree Expand Listener... save the state on Dialog OK/CLOSE
		_connProfileTree.addTreeExpansionListener(new TreeExpansionListener()
		{
			@Override
			public void treeExpanded(TreeExpansionEvent event)
			{
				if ( ! _treeExpansionListenerEnabled )
					return;
				
				Object o = event.getPath().getLastPathComponent();
				if (o instanceof DefaultMutableTreeNode)
				{
					Object userObj = ((DefaultMutableTreeNode)o).getUserObject();
					//System.out.println("ConnectionDialogTree.treeExpanded(): userObj="+userObj+" - "+userObj.getClass().getName());
					//new Exception("DUMMY").printStackTrace();
					
					if (userObj instanceof ConnectionProfileCatalog)
					{
						((ConnectionProfileCatalog)userObj).setExpanded(true);
					}
				}
			}
			
			@Override
			public void treeCollapsed(TreeExpansionEvent event)
			{
				if ( ! _treeExpansionListenerEnabled )
					return;
				
				Object o = event.getPath().getLastPathComponent();
				if (o instanceof DefaultMutableTreeNode)
				{
					Object userObj = ((DefaultMutableTreeNode)o).getUserObject();
					//System.out.println("ConnectionDialogTree.treeCollapsed(): userObj="+userObj+" - "+userObj.getClass().getName());
					//new Exception("DUMMY").printStackTrace();
					
					if (userObj instanceof ConnectionProfileCatalog)
					{
						((ConnectionProfileCatalog)userObj).setExpanded(false);
					}
				}
			}
		});

		_connProfileTree.addTreeSelectionListener(new TreeSelectionListener()
		{			
			@Override
			public void valueChanged(TreeSelectionEvent e)
			{
				_logger.debug("selectedNode(): timer: " + (nodeSelectionTimer.isRunning() ? "RESTART": "START"));
				if (nodeSelectionTimer.isRunning())
					nodeSelectionTimer.restart();
				else
					nodeSelectionTimer.start();
			}
		});
		_connProfileTree.addMouseListener(new ClickListener()
		{
			@Override
			public void doubleClick(MouseEvent e)
			{
				TreePath selPath = _connProfileTree.getPathForLocation(e.getX(), e.getY());

				if (selPath != null)
				{
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath.getLastPathComponent();
					Object o = node.getUserObject();
					if (o instanceof ConnectionProfile)
						connect( (ConnectionProfile) o );
				}
			}
			
			@Override
			public void singleClick(MouseEvent e)
			{
//				TreePath selPath = _connProfileTree.getPathForLocation(e.getX(), e.getY());
//
//				Configuration conf = Configuration.getCombinedConfiguration();
//				boolean loadOnNodeSelection  = conf.getBooleanProperty(ConnectionProfileManager.PROPKEY_connProfile_load_onNodeSelection, ConnectionProfileManager.DEFAULT_connProfile_load_onNodeSelection);
//
//				if (loadOnNodeSelection && selPath != null)
//				{
//    				DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath.getLastPathComponent();
//					Object o = node.getUserObject();
//					if (o instanceof ConnectionProfile)
//						load( (ConnectionProfile) o );
//				}
			}
		});

		JLabel heading = new JLabel(" Connection Profiles ");
//		heading.setFont(new java.awt.Font("Dialog", Font.BOLD, 14));
		heading.setFont(new java.awt.Font("Dialog", Font.BOLD, SwingUtils.hiDpiScale(14)));

		final JLabel     filter_lbl = new JLabel(" Filter");
		final JTextField filter_txt = new JTextField();

		filter_lbl.setToolTipText("<html>"
				+ "Show only profile names that matches the filter. <br>"
				+ "<b>Tip 1</b>: Regular Expresions can be used<br>"
				+ "<b>Tip 2</b>: To see <b>all</b> entries... Closed catalogs will temporarily be expanded: type '.', which is a regex for any character."
				+ "</html>");
		filter_txt.setToolTipText(filter_lbl.getToolTipText());

		if ( StringUtil.hasValue(ConnectionProfileManager.getInstance().getTreeModelFilterOnProfileName()) )
			filter_txt.setText( ConnectionProfileManager.getInstance().getTreeModelFilterOnProfileName() );
		
		filter_txt.addKeyListener(new KeyListener()
		{
			@Override public void keyTyped(KeyEvent e) {}
			@Override public void keyPressed(KeyEvent e) {}
			@Override public void keyReleased(KeyEvent e) 
			{
				String filterStr = filter_txt.getText();

				// Set the profile name to look for in the model
				_treeExpansionListenerEnabled = false;
				ConnectionProfileManager.getInstance().setTreeModelFilterOnProfileName(filterStr);
				if (StringUtil.hasValue(filterStr))
					treeExpandAll(_connProfileTree);
				else
					restoreToSavedTreeExpansionState(_connProfileTree);
				_treeExpansionListenerEnabled = true;

				// set focus back to the filter
				SwingUtilities.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						filter_txt.requestFocus();
					}
				});
			}
		});
		
//		JScrollPane treeScroll = new JScrollPane(_connProfileTree, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		JScrollPane treeScroll = new JScrollPane(_connProfileTree);
		treeScroll.setViewportView(_connProfileTree);

		// Ctrl-C == Copy Profile Name
		ActionMap actionMap = _connProfileTree.getActionMap();
		actionMap.put("copy", connProfileCopyNameAction);
		
		panel.add( heading );
		panel.add( filter_lbl, "split" );
		panel.add( filter_txt, "pushx, growx, wrap" );

		//		panel.add( _connProfileTree, "push, grow" );
		panel.add( treeScroll, "push, grow" );
//		panel.add( new JCheckBox("Add on Connect", true));
		
		return panel;
	}

	private void restoreToSavedTreeExpansionState(JTree tree)
	{
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();

		// First Expand all nodes
		// And as a second step, collapse the ones that we want to close
		// If we do it the other way: top-down, then catalogs at top level will be open (even if they should be closed)
		treeExpandAll(tree);
		
		collapseChildNodes(tree, root, 0);
	}

	private void treeExpandAll(JTree tree)
	{
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();

		// Expand all nodes
		Enumeration<?> e = root.breadthFirstEnumeration();
		while (e.hasMoreElements())
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
			if ( node.isLeaf() )
				continue;

			int row = tree.getRowForPath(new TreePath(node.getPath()));
			tree.expandRow(row);
		}
	}
	
	private void collapseChildNodes(JTree tree, DefaultMutableTreeNode node, int nestLevel)
	{
		String indentStr = StringUtil.replicate(" ", nestLevel*2);

		@SuppressWarnings("rawtypes")
		Enumeration e = node.children();
		while (e.hasMoreElements())
		{
			DefaultMutableTreeNode thisNode = (DefaultMutableTreeNode) e.nextElement();
			Object userObj = thisNode.getUserObject();

			if (userObj instanceof ConnectionProfileCatalog)
			{
				String  name     = ((ConnectionProfileCatalog)userObj).getName();
				boolean expanded = ((ConnectionProfileCatalog)userObj).isExpanded();
				//System.out.println(indentStr+(expanded?"++":"--")+"catalog: name='"+name+"', expanded="+expanded);

				// recursive call needs to be done BEFORE: collapsePath()
				collapseChildNodes(tree, thisNode, nestLevel+1);

				// Collapse the Path
				if ( ! expanded )
				{
					TreePath treePath = new TreePath(thisNode.getPath());
//					//System.out.println(indentStr+"treePath="+treePath+", catalog: name='"+name+"', expanded="+expanded);
					tree.collapsePath(treePath);
					
//					int row = tree.getRowForPath(treePath);
//					System.out.println(indentStr+"row="+row+", catalog: name='"+name+"', expanded="+expanded);
//					tree.collapseRow(row);
				}
				
//				// Expand the Path
//				if (expanded)
//				{
//					TreePath treePath = new TreePath(thisNode.getPath());
//					System.out.println(indentStr+"treePath="+treePath+", catalog: name='"+name+"', expanded="+expanded);
//					tree.expandPath(treePath);
//				}
//				// recursive call needs to be done AFTER: expandPath()
//				expandChildNodes(tree, thisNode, nestLevel+1);

			}
//			else if (userObj instanceof ConnectionProfile)
//			{
//				String name = ((ConnectionProfile)userObj).getName();
//				System.out.println(indentStr+">> profile: name='"+name+"'.");
//			}
//			else
//			{
//				String name = userObj.toString();
//				System.out.println(indentStr+"## profile: name='"+name+"'.");
//			}
		}
	}

	// NOTE: This did NOT work (any expanded 'dirs' below a closed 'dir' ias maked as "collapsed" in this way... so go with TreeExpansionListener instead)
//	private void saveTreeExpansionState(JTree tree)
//	{
//		DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
//
//		// First Expand all nodes
//		// And as a second step, collapse the ones that we want to close
//		// If we do it the other way: top-down, then catalogs at top level will be open (even if they should be closed)
////		Enumeration<?> e = root.breadthFirstEnumeration();
//		Enumeration<?> e = root.preorderEnumeration();
//		while (e.hasMoreElements())
//		{
//			DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
//			if ( node.isLeaf() )
//				continue;
//
//			TreePath treePath = new TreePath(node.getPath());
//			boolean isExpanded = tree.isExpanded(treePath);
//			
//			Object userObj = node.getUserObject();
//			if (userObj instanceof ConnectionProfileCatalog)
//			{
//				ConnectionProfileCatalog cat = (ConnectionProfileCatalog)userObj;
//				cat.setExpanded(isExpanded);
//				
//System.out.println("saveTreeExpansionState(): catalog: "+(isExpanded?"++":"--")+"expanded="+isExpanded+", name='"+cat.getName()+"', treePath="+treePath);
//			}
//		}
//	}
	
	private JPanel createConnTabbedPanel()
	{
		JPanel panel = new JPanel();
//		panel.setLayout(new MigLayout("insets 0 0 0 0, wrap 1", "", ""));   // insets Top Left Bottom Right
		panel.setLayout(new BorderLayout());

		String aseTabTip     = "Connect to an ASE to monitor performance.";
		String hostmonTabTip = "Connect to Operating System host machine where DB Server is hosted to monitor IO and or CPU performance.";
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
		
		if (! _options._showAseTab)     _tab.setVisibleAtModel(TAB_POS_ASE,     false);
		if (! _options._showHostmonTab) _tab.setVisibleAtModel(TAB_POS_HOSTMON, false);
		if (! _options._showPcsTab)     _tab.setVisibleAtModel(TAB_POS_PCS,     false);
		if (! _options._showOfflineTab) _tab.setVisibleAtModel(TAB_POS_OFFLINE, false);
		if (! _options._showJdbcTab)    _tab.setVisibleAtModel(TAB_POS_JDBC,    false);

		// If ASE isn't visible, Set the JDBC tab first
		if (! _options._showAseTab)
		{
			String[] tabOrder = {TAB_TITLE_JDBC, TAB_TITLE_HOSTMON, TAB_TITLE_PCS, TAB_TITLE_OFFLINE};
			_tab.setTabOrder(tabOrder);
		}

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
		
		panel.add(createAseUserPasswdPanel(),      "growx, pushx");
		panel.add(createAseServerPanel(),          "growx, pushx");
		if (_options._showDbxTuneOptionsInTds)
			panel.add(createDbxTuneOptionsPanel(), "growx, pushx");
//		panel.add(createPcsPanel(),                "grow, hidemode 3");
		
		return panel;
	}
	private JPanel createTabHostmon()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("wrap 1", "", ""));   // insets Top Left Bottom Right
		
		panel.add(createHostmonUserPasswdPanel(), "growx, pushx");
		panel.add(createHostmonServerPanel(),     "growx, pushx");
		panel.add(createHostmonLocalOsPanel(),    "growx, pushx");
		panel.add(createHostmonInfoPanel(),       "growx, pushx");
		
		return panel;
	}
	private JPanel createTabPcs()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("wrap 1", "", ""));   // insets Top Left Bottom Right
		
		panel.add(createPcsJdbcPanel(),               "growx, hidemode 3");
		panel.add(createPcsDdlLookupAndStorePanel(),  "growx");
		panel.add(createPcsSqlCaptureAndStorePanel(), "growx");
		panel.add(createPcsTablePanel(),              "grow, push");
		
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

		createJdbcPanel();
		createJdbcDriverInfoPanel();

		panel.add(_jdbcPanel,           "growx, pushx");
		
		if (_options._showDbxTuneOptionsInJdbc)
		{
			panel.add(createDbxTuneOptionsPanel(), "growx, pushx");

			// Normally do not show this button, only if the DbxTuneOptions panel is visible (meaning we might record a session)
			_jdbcDriverInfo_but.setVisible(true);
		}
		else
		{
			panel.add(_jdbcDriverInfoPanel, "grow, push");

			// Normally do not show this button, only if the DbxTuneOptions panel is visible (meaning we might record a session)
			_jdbcDriverInfo_but.setVisible(false);
		}

		return panel;
	}
	private JPanel createAseUserPasswdPanel()
	{
		JPanel panel = SwingUtils.createPanel("User information", true);
		panel.setLayout(new MigLayout("wrap 2", "", ""));   // insets Top Left Bottom Right

		// Hide password or not...
		if (_logger.isDebugEnabled())
			_asePassword_txt = new JTextField();
		else
			_asePassword_txt = new JPasswordField();

		_aseUsername_lbl.setToolTipText("User name to use when logging in to the below DB Server.");
		_aseUsername_txt.setToolTipText("User name to use when logging in to the below DB Server.");
		_asePassword_lbl.setToolTipText("Password to use when logging in to the below DB Server");
		_asePassword_txt.setToolTipText("Password to use when logging in to the below DB Server");
		_aseOptionSavePwd_chk      .setToolTipText("Save the password in the configuration file, and yes it's encrypted");
		_aseOptionPwdEncryption_chk.setToolTipText("<html>Encrypt the password when sending it over the network<br>This will set jConnect option ENCRYPT_PASSWORD=true</html>");


		
		panel.add(_aseLoginIcon,    "");
		panel.add(_aseLoginHelp,    "wmin 100, push, grow");

		panel.add(_aseUsername_lbl, "");
		panel.add(_aseUsername_txt, "push, grow");

		panel.add(_asePassword_lbl, "");
		panel.add(_asePassword_txt, "push, grow");

		panel.add(_aseOptionSavePwd_chk, "skip, split");
		panel.add(_aseOptionPwdEncryption_chk, "");

		// ADD ACTION LISTENERS
		_asePassword_txt             .addActionListener(this);
		_aseOptionPwdEncryption_chk.addActionListener(this);

		// ADD FOCUS LISTENERS
		_aseUsername_txt.addFocusListener(this);
		_asePassword_txt.addFocusListener(this);
		
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

		_aseProfile_lbl     .setToolTipText("Choose an earlier sessions that was saved");
		_aseProfile_cbx     .setToolTipText("Choose an earlier sessions that was saved");
		_aseProfileType_cbx .setToolTipText("Set a Connection Profile Type, which you can set border colors etc...");
		_aseProfileSave_but .setToolTipText("<html>Save the profile<br><ul>"
				+ "<li>If <b>no</b> profile name has been choosen: A dialog will ask you what name to choose</li>"
				+ "<li>If <b>a profile name <b>has</b> been choosen</b>: A dialog will ask you if you want to save it as a new name or just save it.</li>"
				+ "</ul></html>");
		_aseProfileNew_but  .setToolTipText("<html>Clears all fields to create a new Profile</html>");
		_aseIfile_lbl       .setToolTipText("Directory Service file (sql.ini or interfaces) to use for resolving DB Server name into hostname and port number");
		_aseIfile_txt       .setToolTipText("Directory Service file (sql.ini or interfaces) to use for resolving DB Server name into hostname and port number");
		_aseIfile_but       .setToolTipText("Open a File Dialog to locate a Directory Service file.");
		_aseEditIfile_but   .setToolTipText("Edit the Name/Directory Service file. Just opens a text editor.");
		_aseServer_lbl      .setToolTipText("Name of the DB Server you are connecting to");
		_aseServer_cbx      .setToolTipText("Name of the DB Server you are connecting to");
		_aseHostPortResolve_chk.setToolTipText("<html>"
		                                           + "If you want to <b>change</b> a profiles host/port, then <b>disable</b> this lookup.<br>"
		                                           + "<br>"
		                                           + "Check if we have this hostname/port in any other profile.<br>"
		                                           + "If we <b>have</b> a profile with that those entries... Then set that profile.<br>"
		                                           + "</html>");
		_aseHost_lbl        .setToolTipText("<html>Hostname or IP address of the DB Server you are connecting to<br>Syntax: host1[,host2,...]</html>");
		_aseHost_txt        .setToolTipText("<html>Hostname or IP address of the DB Server you are connecting to<br>Syntax: host1[,host2,...]</html>");
		_asePort_lbl        .setToolTipText("<html>Port number of the DB Server you are connecting to<br>Syntax: port1[,port2,...]</html>");
		_asePort_txt        .setToolTipText("<html>Port number of the DB Server you are connecting to<br>Syntax: port1[,port2,...]</html>");
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
			    "directly to the destination machine where the DB Server is hosted. This due to firewall restrictions or port blocking.<br>" +
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
		_aseClientCharset_lbl .setToolTipText("<html>Character set which is used by the client.<br><b>Note:</b> This will just set field 'URL Options' to use CHARSET=valueInComboBox</html>");
		_aseClientCharset_cbx .setToolTipText(_aseClientCharset_lbl.getToolTipText());
		_aseSqlInit_lbl       .setToolTipText("<html>Send this SQL Statement after the connection has been established.<br>If you want to send several statements, use ';' as a teminator for each statement.</html>");
		_aseSqlInit_txt       .setToolTipText("<html>Send this SQL Statement after the connection has been established.<br>If you want to send several statements, use ';' as a teminator for each statement.</html>");

		populateClientCharset();

		panel.add(_aseServerIcon,        "");
		panel.add(_aseServerHelp,        "wmin 100, push, grow");

		panel.add(_aseProfile_lbl,       "");
		panel.add(_aseProfile_cbx,       "push, grow, split");
		panel.add(_aseProfileType_cbx,   "");
		panel.add(_aseProfileSave_but,   "");
		panel.add(_aseProfileNew_but,    "wrap");
		_aseProfile_cbx.setEditable(false);
		_aseProfileType_cbx.setEditable(false);

//		_ifile_txt.setEditable(false);
		panel.add(_aseIfile_lbl,         "");
		panel.add(_aseIfile_txt,         "push, grow, split");
//		panel.add(_aseIfile_but,         "wrap");
		panel.add(_aseIfile_but,         "");
		panel.add(_aseEditIfile_but,     "wrap");

		panel.add(_aseServer_lbl,        "");
		panel.add(_aseServer_cbx,        "push, grow");

		panel.add(_aseHost_lbl,          "");
		panel.add(_aseHost_txt,          "split, push, grow");
		panel.add(_aseHostPortResolve_chk,"wrap");

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

		panel.add(_aseClientCharset_lbl, "");
		panel.add(_aseClientCharset_cbx, "push, grow");

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
		_aseProfile_cbx      .addActionListener(this);
		_aseProfileType_cbx  .addActionListener(this);
		_aseProfileSave_but  .addActionListener(this);
		_aseProfileNew_but   .addActionListener(this);
		_aseServer_cbx       .addActionListener(this);
		_aseOptions_txt      .addActionListener(this);
		_aseOptions_but      .addActionListener(this);
		_aseIfile_but        .addActionListener(this);
		_aseEditIfile_but    .addActionListener(this);
		_aseIfile_txt        .addActionListener(this);
		_aseConnUrl_chk      .addActionListener(this);
		_aseConnUrl_txt      .addActionListener(this);
		_aseSshTunnel_chk    .addActionListener(this);
		_aseSshTunnel_but    .addActionListener(this);
		_aseClientCharset_cbx.addActionListener(this);

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
	private void populateClientCharset()
	{
		_aseClientCharset_cbx.addItem("");
		_aseClientCharset_cbx.addItem("ascii_8");
		_aseClientCharset_cbx.addItem("big5");
		_aseClientCharset_cbx.addItem("big5hk");
		_aseClientCharset_cbx.addItem("cp1250");
		_aseClientCharset_cbx.addItem("cp1251");
		_aseClientCharset_cbx.addItem("cp1252");
		_aseClientCharset_cbx.addItem("cp1253");
		_aseClientCharset_cbx.addItem("cp1254");
		_aseClientCharset_cbx.addItem("cp1255");
		_aseClientCharset_cbx.addItem("cp1256");
		_aseClientCharset_cbx.addItem("cp1257");
		_aseClientCharset_cbx.addItem("cp1258");
		_aseClientCharset_cbx.addItem("cp437");
		_aseClientCharset_cbx.addItem("cp850");
		_aseClientCharset_cbx.addItem("cp852");
		_aseClientCharset_cbx.addItem("cp855");
		_aseClientCharset_cbx.addItem("cp857");
		_aseClientCharset_cbx.addItem("cp858");
		_aseClientCharset_cbx.addItem("cp860");
		_aseClientCharset_cbx.addItem("cp864");
		_aseClientCharset_cbx.addItem("cp866");
		_aseClientCharset_cbx.addItem("cp869");
		_aseClientCharset_cbx.addItem("cp874");
		_aseClientCharset_cbx.addItem("cp932");
		_aseClientCharset_cbx.addItem("cp936");
		_aseClientCharset_cbx.addItem("cp949");
		_aseClientCharset_cbx.addItem("cp950");
		_aseClientCharset_cbx.addItem("deckanji");
		_aseClientCharset_cbx.addItem("euccns");
		_aseClientCharset_cbx.addItem("eucgb");
		_aseClientCharset_cbx.addItem("eucjis");
		_aseClientCharset_cbx.addItem("eucksc");
		_aseClientCharset_cbx.addItem("gb18030");
		_aseClientCharset_cbx.addItem("greek8");
		_aseClientCharset_cbx.addItem("iso15");
		_aseClientCharset_cbx.addItem("iso88592");
		_aseClientCharset_cbx.addItem("iso88595");
		_aseClientCharset_cbx.addItem("iso88596");
		_aseClientCharset_cbx.addItem("iso88597");
		_aseClientCharset_cbx.addItem("iso88598");
		_aseClientCharset_cbx.addItem("iso88599");
		_aseClientCharset_cbx.addItem("iso_1");
		_aseClientCharset_cbx.addItem("koi8");
		_aseClientCharset_cbx.addItem("kz1048");
		_aseClientCharset_cbx.addItem("mac");
		_aseClientCharset_cbx.addItem("macgrk2");
		_aseClientCharset_cbx.addItem("macturk");
		_aseClientCharset_cbx.addItem("mac_cyr");
		_aseClientCharset_cbx.addItem("mac_ee");
		_aseClientCharset_cbx.addItem("mac_euro");
		_aseClientCharset_cbx.addItem("roman8");
		_aseClientCharset_cbx.addItem("roman9");
		_aseClientCharset_cbx.addItem("sjis");
		_aseClientCharset_cbx.addItem("tis620");
		_aseClientCharset_cbx.addItem("turkish8");
//		_aseClientCharset_cbx.addItem("unicode");
		_aseClientCharset_cbx.addItem("utf8");	}
	
	private JPanel createDbxTuneOptionsPanel()
	{
		if (_dbxTuneOptionsPanel != null)
			return _dbxTuneOptionsPanel;

		JPanel panel = SwingUtils.createPanel("Options", true);
		panel.setLayout(new MigLayout("wrap 1, gap 0", "", ""));   // insets Top Left Bottom Right
		
		_dbxTuneOptionsPanel = panel;

//		_aseOptionSavePwd_chk          .setToolTipText("Save the password in the configuration file, and yes it's encrypted");
		_aseOptionConnOnStart_chk      .setToolTipText("When "+Version.getAppName()+" starts use the Server and connect automatically (if the below 'Persisten Counter Storage' is enabled, it will also be used at startup)");
		_aseOptionReConnOnFailure_chk  .setToolTipText("If connection to the monitored server is lost in some way, try to reconnect to the server again automatically.");
		_aseOptionStore_chk            .setToolTipText("Store GUI Counter Data in a database (Persistent Counter Storage), which can be viewed later, connect to it from the 'offline' tab");
		_aseHostMonitor_chk            .setToolTipText("Connect to the Operating System host via SSH, to monitor IO statistics and/or CPU usage.");
                                       
		_aseOptionUseTemplate_chk      .setToolTipText("<html>Use a specific 'Performance Counter' template, which will be enabled/set when connecting to the above server.<br>So if you have specific monitoring needs for this specific server, you can easely choose a specific preconfigured monitoring template.</html>");
		_aseOptionUseTemplate_cbx      .setToolTipText("Choose a specific 'Performance Counter' template, which will be enabled/set when connecting to the above server.");
		_aseOptionUseTemplate_but      .setToolTipText("Open the Template Dialog (same as menu->View->Change 'Performance Counter' Options...");
		
		_aseDeferredConnect_chk        .setToolTipText("<html>If you want to connect at a specific time, and start to collect data.<br><br><b>Note:</b> 00 is midnight. If you want to start at 23:15 later today, simply say 23:15 </html>");
		_aseDeferredConnectHour_sp     .setToolTipText("What Hour do you want to connect, Note that this is an exact hour in time. 00 means midnight.");
		_aseDeferredConnectMinute_sp   .setToolTipText("What Minute do you want to connect");
		_aseDeferredDisConnect_chk     .setToolTipText("If you want to disconnect after a certain elapsed time frame");
		_aseDeferredDisConnectHour_sp  .setToolTipText("After how many hours of sampling do you want to disconnect");
		_aseDeferredDisConnectMinute_sp.setToolTipText("After how many minutes of sampling do you want to disconnect");

		if (_options._showPcsTab)
			panel.add(_aseOptionStore_chk,       "");

		if (_options._showHostmonTab)
			panel.add(_aseHostMonitor_chk,       "");

		if (_options._showPcsTab)
		{
			panel.add(_aseOptionUseTemplate_chk, "split");
			panel.add(_aseOptionUseTemplate_cbx, "pushx, growx");
			panel.add(_aseOptionUseTemplate_but, "wrap");
		}

		if (_options._showHostmonTab || _options._showPcsTab)
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

//		_aseOptionUseTemplate_chk      .addActionListener(this);
//		_aseOptionUseTemplate_cbx      .addActionListener(this);
		_aseOptionUseTemplate_but      .addActionListener(this);

		_aseOptionStore_chk            .addActionListener(this);
		_aseHostMonitor_chk            .addActionListener(this);

		_aseDeferredConnect_chk        .addActionListener(this);
		_aseDeferredConnectHour_sp     .addChangeListener(this);
		_aseDeferredConnectMinute_sp   .addChangeListener(this);
		_aseDeferredDisConnect_chk     .addActionListener(this);
		_aseDeferredDisConnectHour_sp  .addChangeListener(this);
		_aseDeferredDisConnectMinute_sp.addChangeListener(this);

		return panel;
	}

	private JPanel createHostmonLocalOsPanel()
	{
		JPanel panel = SwingUtils.createPanel("Local OS Command", true);
		panel.setLayout(new MigLayout("wrap 2", "", ""));   // insets Top Left Bottom Right
		_hostmonLocalOsCmd_pan = panel;

		_hostmonLocalOsCmdHelp.setText("" 
				+ "Normally Host Monitoring is done via a SSH Connection. (info in above panels) \n"
                + "But if the Monitored DBMS Host is <i>the same</i> as where " + Version.getAppName() + " is running we can use <b>local execution</b>.\n"
                + "Or if we want to <i>wrap</i> the OS Command used to execute the monitoring commands, meaning <i>sudo</i> or simular...\n"
                + "Or if the Java SSH Library simply isn't working for some reasons...\n"
                + "Or <i>whatever</i> reasons you have, you can execute a Local Command that forwards the Host Monitoring Command to the DBMS host."
                + "");
		
		_hostMonLocalOsCmd_chk       .setToolTipText("Execute the Host Monitoring Command on the Local Computer (no SSH)");
		_hostmonLocalOsCmdWrapper_lbl.setToolTipText("<html>Execute this command as a wrapper for the <b>real</b> Host Monitor Commands.<br>"
		                                                 + "So this can be: "
		                                                 + "<ul>"
		                                                 + "  <li>A Shell Script that does <i>sudo</i> or some other operation</li>"
		                                                 + "  <li>Or a local 'ssh' binary responsible for the comunication (if the above Java Library SSH doesn't work for any reason) </li>"
		                                                 + "</ul>"
		                                                 + "There will be some environment variables set in the shell that can be used when executing."
		                                                 + "<ul>"
		                                                 + "  <li><b>HOSTMON_CMD </b>     -- The actual OS command that will be executed for that specififc Host Monitor Module.</li>"
		                                                 + "  <li><b>HOSTMON_CMD_FILE</b> -- A temp file with same content as HOSTMON_CMD, If there is a problem with quoting, this can be used to read the commands from</li>"
		                                                 + "  <li><b>SSH_HOSTNAME</b>     -- Hostname         specified in the above panel</li>"
		                                                 + "  <li><b>SSH_PORT    </b>     -- Port number      specified in the above panel</li>"
		                                                 + "  <li><b>SSH_USERNAME</b>     -- Username         specified in the above panel</li>"
		                                                 + "  <li><b>SSH_PASSWORD</b>     -- Password         specified in the above panel</li>"
		                                                 + "  <li><b>SSH_KEYFILE </b>     -- Private Key File specified in the above panel</li>"
		                                                 + "</ul>"
		                                                 + "You can also use ${hostMonCmd} in the command, which will be replaced with the command we want to send to the remote host.<br>"
		                                                 + "<b>Example 1</b>: <code>ssh username@acme.com \"${hostMonCmd}\"</code><br>"
		                                                 + "<b>Example 2</b>: <code>ssh -i /path/to/id_rsa.xxx -o StrictHostKeyChecking=no username@acme.com \"${hostMonCmd}\"</code><br>"
		                                                 + "<br>"
		                                                 + "The hostMonCmd is also sent to the command as STDIN, so you can do the following:<br>"
		                                                 + "<b>Example 3</b>: <code>ssh username@acme.com \"bash -s\"</code><br>"
		                                                 + "<br>"
		                                                 + "<i><b>Note</b>: 'writeCommandToStdin' can be disabled by property: <code>HostMonitorConnectionLocalOsCmdWrapper.writeCommandToStdin=false</code> if it causes any problems.</i><br>"
		                                                 + "<i><b>Note</b>: 'writeCommandToFile' can be disabled by property: <code>HostMonitorConnectionLocalOsCmdWrapper.writeCommandToFile=false</code> if it causes any problems.</i><br>"
		                                                 + "</html");
		_hostmonLocalOsCmdWrapper_txt.setToolTipText(_hostmonLocalOsCmdWrapper_lbl.getToolTipText());
		_hostmonLocalOsCmdWrapper_lbl.setUseFocusableTips(true);
		_hostmonLocalOsCmdWrapper_txt.setUseFocusableTips(true);

		String note = "<br><br><b>Note</b>: all ${xxx} will be replaced with the above specified user/password/keyfile/hostname/port before the command is executed.<br>The environment variables (SSH_XXX described Above) will also be present, but as environment variables in the local OS Shell ('CMD.exe' on Windows or 'bash' on Linux/Unix)";
		final JButton template1 = new JButton("ssh stdin");      template1.setToolTipText("<html>Execute 'ssh', which will execute <code>bash -s</code> at the rmote host.<br>-s switch to bash means that it will accept stdin for commands. ${NOTE}</html>".replace("${NOTE}", note));
		final JButton template2 = new JButton("ssh parameter");  template2.setToolTipText("<html>Execute 'ssh', which will execute <code><i>hostMonCmd</i></code> at the rmote host.<br>This may have problems with quoutation (The hostMonCmd will/may contain singel or double quotes). ${NOTE}</html>".replace("${NOTE}", note));
//		final JButton template3 = new JButton("Template 3");     template3.setToolTipText("<html>Execute 'xxx', ........... ${NOTE}</html>".replace("${NOTE}", note));
		final JButton template4 = new JButton("call Script");    template4.setToolTipText("<html>Execute <code>yourSpecialScript.sh</code> on the locla machine, which will have to connect to the remote system in <i>some</i> way and execute the HOSTMON_CMD. ${NOTE}</html>".replace("${NOTE}", note));
		
		template1.addActionListener(button -> {_hostmonLocalOsCmdWrapper_txt.setText("ssh ${sshUsername}@${sshHostname} \"bash -s\""); validateContents();});
		template2.addActionListener(button -> {_hostmonLocalOsCmdWrapper_txt.setText("ssh ${sshUsername}@${sshHostname} \"${hostMonCmd}\""); validateContents();});
//		template3.addActionListener(button -> {_hostmonLocalOsCmdWrapper_txt.setText("ssh ${sshUsername}@${sshHostname} 'bash -s'"); validateContents();});
		template4.addActionListener(button -> {_hostmonLocalOsCmdWrapper_txt.setText("yourSpecialScript.sh"); validateContents();});

		panel.add(_hostmonLocalOsCmdIcon,  "");
		panel.add(_hostmonLocalOsCmdHelp,  "wmin 100, push, grow");

		panel.add(_hostMonLocalOsCmd_chk,  "skip");

		panel.add(_hostmonLocalOsCmdWrapper_lbl, "");
		panel.add(_hostmonLocalOsCmdWrapper_txt, "push, grow");

		panel.add(new JLabel("Templates"), "");
		panel.add(template1,               "split");
		panel.add(template2,               "");
//		panel.add(template3,               "");
		panel.add(template4,               "");

		// ADD ACTION LISTENERS
		_hostMonLocalOsCmd_chk.addActionListener(this);
		
		_hostmonLocalOsCmdWrapper_txt.addActionListener(this);
		_hostmonLocalOsCmdWrapper_txt.addKeyListener(this);
		_hostmonLocalOsCmdWrapper_txt.addFocusListener(this);
		
		// ADD FOCUS LISTENERS
		_hostmonLocalOsCmdWrapper_txt.addFocusListener(this);
		
		return panel;
	}

	private JPanel createHostmonUserPasswdPanel()
	{
		JPanel panel = SwingUtils.createPanel("User information", true);
		panel.setLayout(new MigLayout("wrap 2", "", ""));   // insets Top Left Bottom Right
		_hostmonUserPasswd_pan = panel;

		// Hide password or not...
		if (_logger.isDebugEnabled())
			_hostmonPassword_txt = new JTextField();
		else
			_hostmonPassword_txt = new JPasswordField();

		_hostmonUsername_lbl.setToolTipText("User name to use when logging in to the below Operating System Host.");
		_hostmonUsername_txt.setToolTipText("User name to use when logging in to the below Operating System Host.");
		_hostmonPassword_lbl.setToolTipText("Password to use when logging in to the below Operating System Host");
		_hostmonPassword_txt.setToolTipText("Password to use when logging in to the below Operating System Host");
		_hostmonOptionSavePwd_chk.setToolTipText("Save the password in the configuration file, and yes it's encrypted");
		_hostmonKeyFile_lbl.setToolTipText("SSH Private Key File, if you want to use 'Open SSH private key' to authenticate.");
		_hostmonKeyFile_txt.setToolTipText("SSH Private Key File, if you want to use 'Open SSH private key' to authenticate.");
		
		panel.add(_hostmonLoginIcon,  "");
		panel.add(_hostmonLoginHelp,  "wmin 100, push, grow");

		panel.add(_hostmonUsername_lbl, "");
		panel.add(_hostmonUsername_txt, "push, grow");

		panel.add(_hostmonPassword_lbl, "");
		panel.add(_hostmonPassword_txt, "push, grow");

		panel.add(_hostmonOptionSavePwd_chk,      "skip");

		panel.add(_hostmonKeyFile_lbl, "");
		panel.add(_hostmonKeyFile_txt, "split, push, grow");
		panel.add(_hostmonKeyFile_but, "wrap");

		// ADD ACTION LISTENERS
		_hostmonKeyFile_but.addActionListener(this);

//		_hostmonUsername_txt.addActionListener(this);
//		_hostmonPassword_txt.addActionListener(this);
		_hostmonUsername_txt.addKeyListener(this);
		_hostmonPassword_txt.addKeyListener(this);
		_hostmonOptionSavePwd_chk.addActionListener(this);
		_hostmonKeyFile_txt.addKeyListener(this);

		// ADD FOCUS LISTENERS
//		_hostmonUsername_txt.addFocusListener(this);
//		_hostmonPassword_txt.addFocusListener(this);
		
		return panel;
	}

	private JPanel createHostmonServerPanel()
	{
		JPanel panel = SwingUtils.createPanel("Specify the server to connect to", true);
		panel.setLayout(new MigLayout("wrap 2", "", ""));   // insets Top Left Bottom Right
		_hostmonServer_pan = panel;

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

//		MultiLineLabel txt;
		GLabel txt;
		String s, t;

		s = "Hover over the different subsection and the ToolTip manager will display more information about the various topics.";
		t = s;
		txt = new GLabel("<html>"+s+"</html>");
		txt.setToolTipText("<html>"+t+"</html>");
		panel.add(txt, "wmin 100, push, grow, wrap 10");

		s = "<b>- What is this Basic Operating System Monitoring.</b>";
		t = "If you want to monitor the Operating System with some basic command, this is one way you can do it.<br>" +
		    "And also if you are storing the Performance Counters in a Persistent storage, the OS Monitoring is in sync with <br>" +
		    "the Performance Counters from the monitored  Server, which makes it easier to correlate the various metrics.";
		txt = new GLabel("<html>"+s+"</html>");
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
		    "Then it will execute <b>vxstat</b> instead of iostat and use that information.<br>" +
		    "<br>" +
		    "If the OS is Windows, then we will use:" +
				"<ul>" +
				"  <li>For <code>iostat</code> - Simulation using: <code> typeperf -si ${sleepTime} \"\\PhysicalDisk(*)\\*\" </code></li>" +
				"  <li>For <code>vmstat</code> - not yet implemented</li>" +
				"  <li>For <code>mpstat</code> - Simulation using: <code> typeperf -si ${sleepTime} \"\\Processor(*)\\*\" </code></li>" +
				"  <li>For <code>uptime</code> - Simulation using: <code> typeperf -si ${sleepTime} \"\\System\\*\" </code></li>" +
				"</ul>" +
		    "";
		txt = new GLabel("<html>"+s+"</html>");
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
		    "This is done by adding the below information to the configuration file. (conf/dbxtune.properties)" +
			"<ul>" +
			"  <li> for iostat: <code>MonitorIoSolaris.sleep=5</code></li>" +
			"  <li> for mpstat: <code>MonitorVmstatAix.sleep=5</code></li>" +
			"  <li> for vmstat: <code>MonitorMpstatLinux.sleep=5</code></li>" +
			"</ul>" +
		    "You can probably fill in the blanks... <code>[Monitor][Subsystem][Osname].sleep=[samplePeriodToTheCommand]</code><br>" +
		    "<br>" +
		    "If you need to debug or check what the OS Command is sending, you can add the following to the Configuration file:<br>" +
		    "<code>log4j.logger.com.dbxtune.hostmon.HostMonitor=DEBUG</code><br>" +
		    "<br>" +
		    "Or you can use the 'log viewer' to change the 'log level' while "+Version.getAppName()+" is still running: <br>" +
		    "MainMenu->View->Open Log Window..., then Press 'Set log level'... locate com.dbxtune.hostmon.HostMonitor and change it.";
		txt = new GLabel("<html>"+s+"</html>");
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
		txt = new GLabel("<html>"+s+"</html>");
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
		txt = new GLabel("<html>"+s+"</html>");
		txt.setToolTipText("<html>"+t+"</html>");
		panel.add(txt, "wmin 100, push, grow, wrap 10");

		return panel;
	}

	public final static String JDBC_URL_TOOLTIP = 
		"<html>" +
		"URL for the above JDBC drivername to connect to a datastore, a couple of template URL for H2 and Sybase JDBC driver<br>" +
		"<br>" +
		"For H2 you can use the following variables: <code>${DATE}, ${SERVERNAME}, ${HOSTNAME}, ${DBXTUNE_HOME}, ${DBXTUNE_SAVE_DIR}</code><br>" +
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
		"  <li><code>${HOSTNAME}</code> <br>" +
		"    The HOSTNAME will be substituted with the output from ASE function <code>asehostname()</code> of which ASE server we are monitoring.<br>" +
		"  </li>" +
		"  <li><code>${DBXTUNE_HOME}</code> <br>" +
		"    The DBXTUNE_HOME will be substituted with the installation path of "+Version.getAppName()+".<br>" +
		"  </li>" +
		"  <li><code>${DBXTUNE_SAVE_DIR}</code> <br>" +
		"    The DBXTUNE_SAVE_DIR will be substituted with ${DBXTUNE_HOME}/data or whatever the environment variable is set to.<br>" +
		"    DBXTUNE_SAVE_DIR is currently set to '<RUNTIME_REPLACE_DBXTUNE_SAVE_DIR>'<br>" +
		"  </li>" +
		"</ul>" +
		"" +
		"Example:<br>" +
		"<code>${DBXTUNE_SAVE_DIR}/xxx.${HOSTNAME}.${DATE:format=yyyy-MM-dd.HH;roll=true}</code><br>" +
		"<br>" +
		"The above example, does:" +
		"<ul>" +
		"  <li>H2 dbname would be '<i>dbxtuneInstallDir</i>/data/xxx.host1.2011-05-31.21'. <br>" +
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
		
		String dbxTuneSaveDir = StringUtil.getEnvVariableValue("DBXTUNE_SAVE_DIR");
		if (dbxTuneSaveDir == null)
			dbxTuneSaveDir = "Not yet specified";

		// Hide password or not...
		if (_logger.isDebugEnabled())
			_pcsJdbcPassword_txt = new JTextField();
		else
			_pcsJdbcPassword_txt = new JPasswordField();
		
		_pcsWriter_lbl      .setToolTipText("Persistent Counter Storage Implementation(s) that is responsible for Storing the Counter Data.");
		_pcsWriter_cbx      .setToolTipText("Persistent Counter Storage Implementation(s) that is responsible for Storing the Counter Data.");
		_pcsJdbcDriver_lbl  .setToolTipText("JDBC drivername to be used by the Persistent Counter Storage to save Counter Data");
		_pcsJdbcDriver_cbx  .setToolTipText("JDBC drivername to be used by the Persistent Counter Storage to save Counter Data");
		_pcsJdbcUrl_lbl     .setToolTipText(JDBC_URL_TOOLTIP.replace("<RUNTIME_REPLACE_DBXTUNE_SAVE_DIR>", dbxTuneSaveDir));
		_pcsJdbcUrl_cbx     .setToolTipText(JDBC_URL_TOOLTIP.replace("<RUNTIME_REPLACE_DBXTUNE_SAVE_DIR>", dbxTuneSaveDir));
		_pcsJdbcUrl_but     .setToolTipText("Open a File chooser dialog to get a filename, for some templates values are replaced");
		_pcsDbxTuneSaveDir_lbl.setToolTipText("<html>Where is the variable 'DBXTUNE_SAVE_DIR' pointing to. This is the directory where the H2 database will be stored if using the ${DBXTUNE_SAVE_DIR} variable in the URL.</html>");
		_pcsDbxTuneSaveDir_txt.setToolTipText(_pcsDbxTuneSaveDir_lbl.getToolTipText());
		_pcsDbxTuneSaveDir_but.setToolTipText("Open a File chooser dialog to get a directory");
		_pcsJdbcUsername_lbl.setToolTipText("<html>User name to be used by the Persistent Counter Storage to save Counter Data<br><br><b>Note</b>: this is <b>not</b> the monitored DB Server username</html>");
		_pcsJdbcUsername_txt.setToolTipText("<html>User name to be used by the Persistent Counter Storage to save Counter Data<br><br><b>Note</b>: this is <b>not</b> the monitored DB Server username</html>");
		_pcsJdbcPassword_lbl.setToolTipText("<html>Password to be used by the Persistent Counter Storage to save Counter Data<br><br><b>Note</b>: this is <b>not</b> the password to the monitored DB Server, you can most likely leave this to blank.</html>");
		_pcsJdbcPassword_txt.setToolTipText("<html>Password to be used by the Persistent Counter Storage to save Counter Data<br><br><b>Note</b>: this is <b>not</b> the password to the monitored DB Server, you can most likely leave this to blank.</html>");
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

		panel.add(_pcsDbxTuneSaveDir_lbl, "skip, span, split, hidemode 3");
		panel.add(_pcsDbxTuneSaveDir_txt, "push, grow, hidemode 3");
		panel.add(_pcsDbxTuneSaveDir_but, "hidemode 3, wrap");

		panel.add(_pcsJdbcUsername_lbl, "");
		panel.add(_pcsJdbcUsername_txt, "push, grow, wrap");

		panel.add(_pcsJdbcPassword_lbl, "");
		panel.add(_pcsJdbcPassword_txt, "push, grow, wrap");
		
		panel.add(_pcsJdbcSavePassword_chk, "skip, span, split");
		panel.add(_pcsH2Option_startH2NetworkServer_chk, "push, grow");
//		panel.add(_pcsH2Option_startH2NetworkServer_chk, "skip, span, split, push, grow");
		
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

//		String envNameSaveDir = DbxTune.getInstance().getAppSaveDirEnvName();  // DBXTUNE_SAVE_DIR
		String envNameSaveDir = "DBXTUNE_SAVE_DIR";

		_pcsDbxTuneSaveDir_txt.setText( StringUtil.getEnvVariableValue("DBXTUNE_SAVE_DIR"));

		_pcsWriter_cbx    .setEditable(true);
		_pcsJdbcDriver_cbx.setEditable(true);
		_pcsJdbcUrl_cbx   .setEditable(true);
		
		// Set how many items the "dropdown" can have before a JScrollBar is visible
		_pcsWriter_cbx    .setMaximumRowCount(30);
		_pcsJdbcDriver_cbx.setMaximumRowCount(30);
		_pcsJdbcUrl_cbx   .setMaximumRowCount(30);
		
		_pcsJdbcDriver_cbx.setRenderer(new JdbcDriverHelper.JdbcDriverComboBoxRender());

		_pcsWriter_cbx    .addItem("com.dbxtune.pcs.PersistWriterJdbc");
		_pcsWriter_cbx    .addItem("com.dbxtune.pcs.PersistWriterToHttpJson");
		_pcsWriter_cbx    .addItem("com.dbxtune.pcs.PersistWriterToInfluxDb");

//		_pcsJdbcDriver_cbx.addItem("org.h2.Driver");
//		_pcsJdbcDriver_cbx.addItem(AseConnectionFactory.getDriver());
		List<String> driversList = JdbcDriverHelper.getAvailableDriverList();
		if (driversList.size() > 0)
		{
			for (String str : driversList)
				_pcsJdbcDriver_cbx.addItem(str);
		}
		_pcsJdbcDriver_cbx.addPopupMenuListener(new PopupMenuListener()
		{
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent paramPopupMenuEvent)
			{
				if (_pcsJdbcDriver_cbx.getItemCount() == 0)
				{
					List<String> driversList = JdbcDriverHelper.getAvailableDriverList();
					if (driversList.size() > 0)
					{
						for (String str : driversList)
							_pcsJdbcDriver_cbx.addItem(str);
					}
				}
			}
			
			@Override public void popupMenuWillBecomeInvisible(PopupMenuEvent paramPopupMenuEvent) {}
			@Override public void popupMenuCanceled(PopupMenuEvent paramPopupMenuEvent) {}
		});

		// http://www.h2database.com/html/features.html#database_url
		_pcsJdbcUrl_cbx   .addItem("jdbc:h2:file:[<path>]<dbname>");
		_pcsJdbcUrl_cbx   .addItem("jdbc:h2:file:${"+envNameSaveDir+"}/${SERVERNAME}_${DATE}");
		_pcsJdbcUrl_cbx   .addItem("jdbc:h2:file:${"+envNameSaveDir+"}/${HOSTNAME}_${DATE}");
//		_pcsJdbcUrl_cbx   .addItem("jdbc:h2:file:[<path>]<dbname>;AUTO_SERVER=TRUE");
//		_pcsJdbcUrl_cbx   .addItem("jdbc:h2:file:${"+envNameSaveDir+"}/${SERVERNAME}_${DATE};AUTO_SERVER=TRUE");
//		_pcsJdbcUrl_cbx   .addItem("jdbc:h2:file:${"+envNameSaveDir+"}/${HOSTNAME}_${DATE};AUTO_SERVER=TRUE");
		_pcsJdbcUrl_cbx   .addItem("jdbc:h2:tcp://<host>[:<port>]/<dbname>");
		_pcsJdbcUrl_cbx   .addItem("jdbc:h2:ssl://<host>[:<port>]/<dbname>");

		// http://infocenter.sybase.com/help/topic/com.sybase.dc39001_0605/html/prjdbc/X39384.htm
		_pcsJdbcUrl_cbx   .addItem("jdbc:sybase:Tds:<host>:<port>");
		_pcsJdbcUrl_cbx   .addItem("jdbc:sybase:Tds:<host>:<port>[/<dbname>]");
//		_pcsJdbcUrl_cbx   .addItem("jdbc:sybase:Tds:<host>:<port>[/<dbname>][?OPT1=val&OPT2=val]");

		
		// ACTIONS
		_pcsTestConn_but      .addActionListener(this);
		_pcsJdbcUrl_cbx       .getEditor().getEditorComponent().addKeyListener(this);
		_pcsJdbcUrl_cbx       .addActionListener(this);
		_pcsJdbcUrl_but       .addActionListener(this);
		_pcsJdbcDriver_cbx    .addActionListener(this);
		_pcsDbxTuneSaveDir_txt.addActionListener(this);
		_pcsDbxTuneSaveDir_but.addActionListener(this);

		_pcsDbxTuneSaveDir_txt.addFocusListener( new FocusListener()
		{
			@Override public void focusGained(FocusEvent e) {}
			@Override public void focusLost(FocusEvent e) { _pcsDbxTuneSaveDir_txt.postActionEvent(); }
		});

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
		_pcsDdl_enabledForDatabaseObjects_chk      .setToolTipText("<html>Store Database Objects (Stored procedures, views, tables, triggers, etc).<html>");
		_pcsDdl_enabledForStatementCache_chk       .setToolTipText("<html>Store Statement Cache (XML Plans).<html>");
		_pcsDdl_addDependantObjectsToDdlInQueue_chk.setToolTipText("Also do DDL Lookup and Storage of dependant objects. Simply does 'exec sp_depends tabname' and add dependant objects for lookup...");
		_pcsDdl_afterDdlLookupSleepTimeInMs_lbl    .setToolTipText("How many milliseconds should we wait between DDL Lookups, this so we do not saturate the source DB Server.");
		_pcsDdl_afterDdlLookupSleepTimeInMs_txt    .setToolTipText("How many milliseconds should we wait between DDL Lookups, this so we do not saturate the source DB Server.");

		// LAYOUT
		panel.add(_pcsDdl_doDdlLookupAndStore_chk,             "");
		panel.add(_pcsDdl_enabledForDatabaseObjects_chk,       "");
		panel.add(_pcsDdl_enabledForStatementCache_chk,        "");
		panel.add(_pcsDdl_addDependantObjectsToDdlInQueue_chk, "");

		panel.add(_pcsDdl_afterDdlLookupSleepTimeInMs_lbl,     "gap 50");
		panel.add(_pcsDdl_afterDdlLookupSleepTimeInMs_txt,     "pushx, growx, wrap");
		

		// ACTIONS
		_pcsDdl_doDdlLookupAndStore_chk            .addActionListener(this);
		_pcsDdl_enabledForDatabaseObjects_chk      .addActionListener(this);
		_pcsDdl_enabledForStatementCache_chk       .addActionListener(this);
		_pcsDdl_addDependantObjectsToDdlInQueue_chk.addActionListener(this);
		_pcsDdl_afterDdlLookupSleepTimeInMs_txt    .addActionListener(this);

		// ADD FOCUS LISTENERS
//		xxx_txt.addFocusListener(this);
//		xxx_txt.addFocusListener(this);

		return panel;
	}

	/** Helper class to toggle if fields are "enabled" or not */ 
	private void updateEnabledForPcsDdlLookupAndSqlCapture()
	{
		boolean enableDdlLookupFields  = _pcsDdl_doDdlLookupAndStore_chk    .isSelected();
		boolean enableSqlCaptureFields = _pcsCapSql_doSqlCaptureAndStore_chk.isSelected();
		
		_pcsDdl_enabledForDatabaseObjects_chk      .setEnabled(enableDdlLookupFields);
		_pcsDdl_enabledForStatementCache_chk       .setEnabled(enableDdlLookupFields);
		_pcsDdl_addDependantObjectsToDdlInQueue_chk.setEnabled(enableDdlLookupFields);
		_pcsDdl_afterDdlLookupSleepTimeInMs_lbl    .setEnabled(enableDdlLookupFields);
		_pcsDdl_afterDdlLookupSleepTimeInMs_txt    .setEnabled(enableDdlLookupFields);
		
		_pcsCapSql_doSqlText_chk           .setEnabled(enableSqlCaptureFields);
		_pcsCapSql_doStatementInfo_chk     .setEnabled(enableSqlCaptureFields);
		_pcsCapSql_doPlanText_chk          .setEnabled(enableSqlCaptureFields);
		_pcsCapSql_sleepTimeInMs_lbl       .setEnabled(enableSqlCaptureFields);
		_pcsCapSql_sleepTimeInMs_txt       .setEnabled(enableSqlCaptureFields);

		_pcsCapSql_saveStatement_lbl             .setEnabled(enableSqlCaptureFields);
		_pcsCapSql_saveStatement_execTime_lbl    .setEnabled(enableSqlCaptureFields);
		_pcsCapSql_saveStatement_execTime_txt    .setEnabled(enableSqlCaptureFields);
		_pcsCapSql_saveStatement_logicalRead_lbl .setEnabled(enableSqlCaptureFields);
		_pcsCapSql_saveStatement_logicalRead_txt .setEnabled(enableSqlCaptureFields);
		_pcsCapSql_saveStatement_physicalRead_lbl.setEnabled(enableSqlCaptureFields);
		_pcsCapSql_saveStatement_physicalRead_txt.setEnabled(enableSqlCaptureFields);
		
		_pcsCapSql_sendDdlForLookup_chk             .setEnabled(enableDdlLookupFields && enableSqlCaptureFields);
		_pcsCapSql_sendDdlForLookup_execTime_lbl    .setEnabled(enableDdlLookupFields && enableSqlCaptureFields);
		_pcsCapSql_sendDdlForLookup_execTime_txt    .setEnabled(enableDdlLookupFields && enableSqlCaptureFields);
		_pcsCapSql_sendDdlForLookup_logicalRead_lbl .setEnabled(enableDdlLookupFields && enableSqlCaptureFields);
		_pcsCapSql_sendDdlForLookup_logicalRead_txt .setEnabled(enableDdlLookupFields && enableSqlCaptureFields);
		_pcsCapSql_sendDdlForLookup_physicalRead_lbl.setEnabled(enableDdlLookupFields && enableSqlCaptureFields);
		_pcsCapSql_sendDdlForLookup_physicalRead_txt.setEnabled(enableDdlLookupFields && enableSqlCaptureFields);
	}
	
	private JPanel createPcsSqlCaptureAndStorePanel()
	{
		JPanel panel = SwingUtils.createPanel("Capture SQL and Store", true);
		panel.setLayout(new MigLayout("", "", ""));   // insets Top Left Bottom Right

		_pcsCapSql_doSqlCaptureAndStore_chk.setToolTipText("<html>Store executed SQL Statements and it's SQL Text when recording a session.<html>");
//		_pcsCapSql_xxx_chk                 .setToolTipText("<html>xxx</html>");
		_pcsCapSql_doSqlText_chk           .setToolTipText("<html>Collect SQL Text                   <br>NOTE requires: sp_configure 'sql text pipe active'  and 'sql text pipe max messages'.</html>");
		_pcsCapSql_doStatementInfo_chk     .setToolTipText("<html>Collect SQL Statements information <br>NOTE requires: sp_configure 'statement pipe active' and 'statement pipe max messages'.</html>");
		_pcsCapSql_doPlanText_chk          .setToolTipText("<html>Collect SQL Plans                  <br>NOTE requires: sp_configure 'plan text pipe active' and 'plan text pipe max messages'.</html>");
		_pcsCapSql_sleepTimeInMs_lbl       .setToolTipText("<html>How many milliseconds should we wait between SQL Capture Lookups.</html>");
		_pcsCapSql_sleepTimeInMs_txt       .setToolTipText("<html>How many milliseconds should we wait between SQL Capture Lookups.</html>");

		_pcsCapSql_saveStatement_lbl             .setToolTipText("<html>You can choose to save Statements that are more expensive... set some limits on what to save...</html>");
		_pcsCapSql_saveStatement_execTime_lbl    .setToolTipText("<html>Only save Statements if the execution time for this statement is above this value in milliseconds. <br>Note: -1 means save all statements </html>"); // <br>Note: all 3 fields need to be <b>greater</b> than -1 if you do <b>not</b> want to save it.</html>");
		_pcsCapSql_saveStatement_execTime_txt    .setToolTipText("<html>Only save Statements if the execution time for this statement is above this value in milliseconds. <br>Note: -1 means save all statements </html>"); // <br>Note: all 3 fields need to be <b>greater</b> than -1 if you do <b>not</b> want to save it.</html>");
		_pcsCapSql_saveStatement_logicalRead_lbl .setToolTipText("<html>Only save Statements if the number of LogicalReads for this statement is above this value.         <br>Note: -1 means save all statements </html>"); // <br>Note: all 3 fields need to be <b>greater</b> than -1 if you do <b>not</b> want to save it.</html>");
		_pcsCapSql_saveStatement_logicalRead_txt .setToolTipText("<html>Only save Statements if the number of LogicalReads for this statement is above this value.         <br>Note: -1 means save all statements </html>"); // <br>Note: all 3 fields need to be <b>greater</b> than -1 if you do <b>not</b> want to save it.</html>");
		_pcsCapSql_saveStatement_physicalRead_lbl.setToolTipText("<html>Only save Statements if the number of PhysicalReads for this statement is above this value.        <br>Note: -1 means save all statements </html>"); // <br>Note: all 3 fields need to be <b>greater</b> than -1 if you do <b>not</b> want to save it.</html>");
		_pcsCapSql_saveStatement_physicalRead_txt.setToolTipText("<html>Only save Statements if the number of PhysicalReads for this statement is above this value.        <br>Note: -1 means save all statements </html>"); // <br>Note: all 3 fields need to be <b>greater</b> than -1 if you do <b>not</b> want to save it.</html>");

		_pcsCapSql_sendDdlForLookup_chk             .setToolTipText("<html>When a procedure name is found in monSysStatement send it of for DDL Lookup</html>");
		_pcsCapSql_sendDdlForLookup_execTime_lbl    .setToolTipText("<html>Send DDL only if the execution time for this statement is above this value in milliseconds. </html>"); // <br>Note: -1 means send all statements <br>Note: all 3 fields need to be <b>greater</b> than -1 if you do <b>not</b> want to send it for DDL lookup.</html>");
		_pcsCapSql_sendDdlForLookup_execTime_txt    .setToolTipText("<html>Send DDL only if the execution time for this statement is above this value in milliseconds. </html>"); // <br>Note: -1 means send all statements <br>Note: all 3 fields need to be <b>greater</b> than -1 if you do <b>not</b> want to send it for DDL lookup.</html>");
		_pcsCapSql_sendDdlForLookup_logicalRead_lbl .setToolTipText("<html>Send DDL only if the number of LogicalReads for this statement is above this value.         </html>"); // <br>Note: -1 means send all statements <br>Note: all 3 fields need to be <b>greater</b> than -1 if you do <b>not</b> want to send it for DDL lookup.</html>");
		_pcsCapSql_sendDdlForLookup_logicalRead_txt .setToolTipText("<html>Send DDL only if the number of LogicalReads for this statement is above this value.         </html>"); // <br>Note: -1 means send all statements <br>Note: all 3 fields need to be <b>greater</b> than -1 if you do <b>not</b> want to send it for DDL lookup.</html>");
		_pcsCapSql_sendDdlForLookup_physicalRead_lbl.setToolTipText("<html>Send DDL only if the number of PhysicalReads for this statement is above this value.        </html>"); // <br>Note: -1 means send all statements <br>Note: all 3 fields need to be <b>greater</b> than -1 if you do <b>not</b> want to send it for DDL lookup.</html>");
		_pcsCapSql_sendDdlForLookup_physicalRead_txt.setToolTipText("<html>Send DDL only if the number of PhysicalReads for this statement is above this value.        </html>"); // <br>Note: -1 means send all statements <br>Note: all 3 fields need to be <b>greater</b> than -1 if you do <b>not</b> want to send it for DDL lookup.</html>");

		// LAYOUT
		panel.add(_pcsCapSql_doSqlCaptureAndStore_chk, "split");
		panel.add(_pcsCapSql_doSqlText_chk,            "");
		panel.add(_pcsCapSql_doStatementInfo_chk,      "");
		panel.add(_pcsCapSql_doPlanText_chk,           "");
//		panel.add(_pcsCapSql_xxx_chk,                  "");
		panel.add(_pcsCapSql_sleepTimeInMs_lbl,        "gap 50");
		panel.add(_pcsCapSql_sleepTimeInMs_txt,        "pushx, growx, wrap");
		
		panel.add(_pcsCapSql_saveStatement_lbl,                 "split");
		panel.add(_pcsCapSql_saveStatement_execTime_lbl,        "");
		panel.add(_pcsCapSql_saveStatement_execTime_txt,        "wmin 10lp, pushx, growx");
		panel.add(_pcsCapSql_saveStatement_logicalRead_lbl,     "");
		panel.add(_pcsCapSql_saveStatement_logicalRead_txt,     "wmin 10lp, pushx, growx");
		panel.add(_pcsCapSql_saveStatement_physicalRead_lbl,    "");
		panel.add(_pcsCapSql_saveStatement_physicalRead_txt,    "wmin 10lp, pushx, growx, wrap");

		panel.add(_pcsCapSql_sendDdlForLookup_chk,              "split");
		panel.add(_pcsCapSql_sendDdlForLookup_execTime_lbl,     "");
		panel.add(_pcsCapSql_sendDdlForLookup_execTime_txt,     "wmin 10lp, pushx, growx");
		panel.add(_pcsCapSql_sendDdlForLookup_logicalRead_lbl,  "");
		panel.add(_pcsCapSql_sendDdlForLookup_logicalRead_txt,  "wmin 10lp, pushx, growx");
		panel.add(_pcsCapSql_sendDdlForLookup_physicalRead_lbl, "");
		panel.add(_pcsCapSql_sendDdlForLookup_physicalRead_txt, "wmin 10lp, pushx, growx, wrap");

		// ACTIONS
		_pcsCapSql_doSqlCaptureAndStore_chk.addActionListener(this);
		_pcsCapSql_doSqlText_chk           .addActionListener(this);
		_pcsCapSql_doStatementInfo_chk     .addActionListener(this);
		_pcsCapSql_doPlanText_chk          .addActionListener(this);
//		_pcsCapSql_xxx_chk                 .addActionListener(this);
		_pcsCapSql_sleepTimeInMs_txt       .addActionListener(this);

		_pcsCapSql_saveStatement_execTime_txt       .addActionListener(this);
		_pcsCapSql_saveStatement_logicalRead_txt    .addActionListener(this);
		_pcsCapSql_saveStatement_physicalRead_txt   .addActionListener(this);

		_pcsCapSql_sendDdlForLookup_chk             .addActionListener(this);
		_pcsCapSql_sendDdlForLookup_execTime_txt    .addActionListener(this);
		_pcsCapSql_sendDdlForLookup_logicalRead_txt .addActionListener(this);
		_pcsCapSql_sendDdlForLookup_physicalRead_txt.addActionListener(this);
		
		// ADD FOCUS LISTENERS
//		xxx_txt.addFocusListener(this);
		_pcsCapSql_sleepTimeInMs_txt                .addFocusListener(this);
		_pcsCapSql_sendDdlForLookup_execTime_txt    .addFocusListener(this);
		_pcsCapSql_sendDdlForLookup_logicalRead_txt .addFocusListener(this);
		_pcsCapSql_sendDdlForLookup_physicalRead_txt.addFocusListener(this);

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

		// Hide password or not...
		if (_logger.isDebugEnabled())
			_offlineJdbcPassword_txt = new JTextField();
		else
			_offlineJdbcPassword_txt = new JPasswordField();
		
		_offlineHelp.setText("Read Stored Counter Data from an offline storage\n" +
			"The Saved Counter Data can be stored in a database eather by the GUI or NO-GUI mode.\n" +
			"One tip is to store Counter Data for different configurations or test suites, this so we can view the 'baseline' for different configurations or sample sessions\n");

		_offlineProfile_lbl     .setToolTipText("Choose an earlier sessions that was saved");
		_offlineProfile_cbx     .setToolTipText("Choose an earlier sessions that was saved");
		_offlineProfileType_cbx .setToolTipText("Set a Connection Profile Type, which you can set border colors etc...");
		_offlineProfileSave_but .setToolTipText("<html>Save the profile<br><ul>"
				+ "<li>If <b>no</b> profile name has been choosen: A dialog will ask you what name to choose</li>"
				+ "<li>If <b>a profile name <b>has</b> been choosen</b>: A dialog will ask you if you want to save it as a new name or just save it.</li>"
				+ "</ul></html>");
		_offlineProfileNew_but  .setToolTipText("<html>Clears all fields to create a new Profile</html>");
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

		panel.add(_offlineProfile_lbl,      "");
		panel.add(_offlineProfile_cbx,      "push, grow, split");
		panel.add(_offlineProfileType_cbx,  "");
		panel.add(_offlineProfileSave_but,  "");
		panel.add(_offlineProfileNew_but,   "wrap");

		panel.add(_offlineJdbcDriver_lbl,   "");
		panel.add(_offlineJdbcDriver_cbx,   "push, grow, wrap");

		panel.add(_offlineJdbcUrl_lbl,      "");
		panel.add(_offlineJdbcUrl_cbx,      "push, grow, split");
		panel.add(_offlineJdbcUrl_but,      "wrap");

		panel.add(_offlineJdbcUsername_lbl, "");
		panel.add(_offlineJdbcUsername_txt, "push, grow, wrap");

		panel.add(_offlineJdbcPassword_lbl, "");
		panel.add(_offlineJdbcPassword_txt, "push, grow, wrap");
		
		panel.add(_offlineJdbcSavePassword_chk,      "skip, wrap");
		panel.add(_offlineCheckForNewSessions_chk,   "skip, wrap");
		panel.add(_offlineH2Option_startH2NwSrv_chk, "skip, hidemode 3, wrap");
		
//		panel.add(_offlineTestConn_lbl, "skip, split, left");
		panel.add(_offlineTestConn_but, "skip, right, wrap");
//		panel.add(_offlineWhatCm_but, "skip 1, right, wrap");

		_offlineProfile_cbx    .setEditable(false);
		_offlineProfileType_cbx.setEditable(false);
		_offlineJdbcDriver_cbx .setEditable(true);
		_offlineJdbcUrl_cbx    .setEditable(true);
		
		// Set how many items the "dropdown" can have before a JScrollBar is visible
		_offlineProfile_cbx    .setMaximumRowCount(30);
		_offlineProfileType_cbx.setMaximumRowCount(30);
		_offlineJdbcDriver_cbx .setMaximumRowCount(30);
		_offlineJdbcUrl_cbx    .setMaximumRowCount(30);
		
//		_offlineJdbcDriver_cbx.addItem("org.h2.Driver");
//		_offlineJdbcDriver_cbx.addItem(AseConnectionFactory.getDriver());
		List<String> driversList = JdbcDriverHelper.getAvailableDriverList();
		if (driversList.size() > 0)
		{
			for (String str : driversList)
				_offlineJdbcDriver_cbx.addItem(str);
		}
		_offlineJdbcDriver_cbx.addPopupMenuListener(new PopupMenuListener()
		{
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent paramPopupMenuEvent)
			{
				if (_offlineJdbcDriver_cbx.getItemCount() == 0)
				{
					List<String> driversList = JdbcDriverHelper.getAvailableDriverList();
					if (driversList.size() > 0)
					{
						for (String str : driversList)
							_offlineJdbcDriver_cbx.addItem(str);
					}
				}
			}
			
			@Override public void popupMenuWillBecomeInvisible(PopupMenuEvent paramPopupMenuEvent) {}
			@Override public void popupMenuCanceled(PopupMenuEvent paramPopupMenuEvent) {}
		});

		_offlineJdbcDriver_cbx.setRenderer(new JdbcDriverHelper.JdbcDriverComboBoxRender());

		// http://www.h2database.com/html/features.html#database_url
		_offlineJdbcUrl_cbx   .addItem("jdbc:h2:file:[<path>]<dbname>;IFEXISTS=TRUE");
		_offlineJdbcUrl_cbx   .addItem("jdbc:h2:file:[<path>]<dbname>;IFEXISTS=TRUE;AUTO_SERVER=TRUE");
		_offlineJdbcUrl_cbx   .addItem("jdbc:h2:zip:<zipFileName>!/<dbname>");
		_offlineJdbcUrl_cbx   .addItem("jdbc:h2:tcp://<host>[:<port>]/<dbname>");
		_offlineJdbcUrl_cbx   .addItem("jdbc:h2:ssl://<host>[:<port>]/<dbname>");
 
		// http://infocenter.sybase.com/help/topic/com.sybase.dc39001_0605/html/prjdbc/X39384.htm
		_offlineJdbcUrl_cbx   .addItem("jdbc:sybase:Tds:<host>:<port>");
		_offlineJdbcUrl_cbx   .addItem("jdbc:sybase:Tds:<host>:<port>[/<dbname>]");
//		_offlineJdbcUrl_cbx   .addItem("jdbc:sybase:Tds:<host>:<port>[/<dbname>][?OPT1=val,&OPT2=val]");

		
		// ACTIONS
		_offlineProfile_cbx     .addActionListener(this);
		_offlineProfileType_cbx .addActionListener(this);
		_offlineProfileSave_but .addActionListener(this);
		_offlineProfileNew_but  .addActionListener(this);
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
			"\n" +
			"This would be available as an extra service, which you will have to pay for! \n" +
			"To get this extra service you need to contact: goran_schwarz@hotmail.com\n");

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

//		panel.add(_sendOfflineNotYetImpl2_lbl, "skip 1, wrap");

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

		// Hide password or not...
		if (_logger.isDebugEnabled())
			_jdbcPassword_txt = new JTextField();
		else
			_jdbcPassword_txt = new JPasswordField();

		_jdbcProfile_lbl    .setToolTipText("Choose an earlier sessions that was saved");
		_jdbcProfile_cbx    .setToolTipText("Choose an earlier sessions that was saved");
		_jdbcProfileType_cbx.setToolTipText("Set a Connection Profile Type, which you can set border colors etc...");
		_jdbcProfileSave_but.setToolTipText("<html>Save the profile<br><ul>"
				+ "<li>If <b>no</b> profile name has been choosen: A dialog will ask you what name to choose</li>"
				+ "<li>If <b>a profile name <b>has</b> been choosen</b>: A dialog will ask you if you want to save it as a new name or just save it.</li>"
				+ "</ul></html>");
		_jdbcProfileNew_but.setToolTipText("<html>Clears all fields to create a new Profile</html>");
		_jdbcDriver_lbl    .setToolTipText("JDBC drivername to be used when creating the connection");
		_jdbcDriver_cbx    .setToolTipText("JDBC drivername to be used when creating the connection");
		_jdbcUrl_lbl       .setToolTipText("URL for the above JDBC drivername to connect to a datastore, a couple of template URL for H2 and Sybase JDBC driver");
		_jdbcUrl_cbx       .setToolTipText("URL for the above JDBC drivername to connect to a datastore, a couple of template URL for H2 and Sybase JDBC driver");
		_jdbcUrl_but       .setToolTipText("Open a File chooser dialog to get a filename, for some templates values are replaced");
		_jdbcUsername_lbl  .setToolTipText("User name to be used when creating the connection");
		_jdbcUsername_txt  .setToolTipText("User name to be used when creating the connection");
		_jdbcPassword_lbl  .setToolTipText("Password to be used when creating the connection");
		_jdbcPassword_txt  .setToolTipText("Password to be used when creating the connection");
		_jdbcSqlServerUseWindowsAuthentication_chk .setToolTipText("Use Windows Authentication subsystem when connecting.");
		_jdbcSqlServerUseEncrypt_chk               .setToolTipText("<html>Encrypt all data over the network (between client and server) via SSL/TLS.<br>JDBC Option 'encrypt=true'<br>Note: This is the default in MS SQL Server JDBC Version 10.1 and above. </html>");
		_jdbcSqlServerUseTrustServerCertificate_chk.setToolTipText("<html>If 'encrypt=true' via SSL/TLS is enabled, then: trust 'self-signed' certificates, which is common in test environments.<br>In MS SQL Server JDBC Version 10.1 and above 'encrypt=true' is the default.</html>");
		_jdbcSqlInit_lbl   .setToolTipText("<html>Send this SQL Statement after the connection has been established.<br>If you want to send several statements, use ';' as a teminator for each statement.</html>");
		_jdbcSqlInit_txt   .setToolTipText("<html>Send this SQL Statement after the connection has been established.<br>If you want to send several statements, use ';' as a teminator for each statement.</html>");
		_jdbcUrlOptions_lbl.setToolTipText("<html>If the current Driver supports <code>driver.getPropertyInfo()</code>, show available Options.<br><b>NOTE</b>: You still have to copy the Option into the URL field yourself...</html>");
		_jdbcUrlOptions_txt.setToolTipText("<html>If the current Driver supports <code>driver.getPropertyInfo()</code>, show available Options.<br><b>NOTE</b>: You still have to copy the Option into the URL field yourself...</html>");
		_jdbcUrlOptions_but.setToolTipText("<html>If the current Driver supports <code>driver.getPropertyInfo()</code>, show available Options.<br><b>NOTE</b>: You still have to copy the Option into the URL field yourself...</html>");
		_jdbcTestConn_but  .setToolTipText("Make a test connection to the above JDBC datastore");
		_jdbcDriverInfo_but.setToolTipText("Show a dialog with all available JDBC Drivers that are in the java classpath");
		_jdbcSshTunnel_chk .setToolTipText(
				"<html>" +
				    "Use a SSH (Secure Shell) connection as a tunnel or intermediate hop, when you can't connect " +
				    "directly to the destination machine where the DB Server is hosted. This due to firewall restrictions or port blocking.<br>" +
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

		panel.add(_jdbcIcon,  "");
		panel.add(_jdbcHelp,  "wmin 100, push, grow, wrap 15");

		panel.add(_jdbcProfile_lbl,      "");
		panel.add(_jdbcProfile_cbx,      "push, grow, split");
		panel.add(_jdbcProfileType_cbx,  "");
		panel.add(_jdbcProfileSave_but,  "");
		panel.add(_jdbcProfileNew_but,   "wrap");

		panel.add(_jdbcDriver_lbl,       "");
		panel.add(_jdbcDriver_cbx,       "push, grow, wrap");

		panel.add(_jdbcUrl_lbl,          "");
		panel.add(_jdbcUrl_cbx,          "push, grow, split");
		panel.add(_jdbcUrl_but,          "wrap");

		panel.add(_jdbcUsername_lbl,     "");
		panel.add(_jdbcUsername_txt,     "push, grow, wrap");

		panel.add(_jdbcPassword_lbl,     "");
		panel.add(_jdbcPassword_txt,     "push, grow, split");
		panel.add(_jdbcSqlServerUseWindowsAuthentication_chk,     "hidemode 3");
		panel.add(_jdbcSavePassword_chk, "wrap");

		panel.add(_jdbcSshTunnel_chk,    "skip, push, grow, split");
		panel.add(_jdbcSshTunnel_but,    "wrap");
		
		panel.add(_jdbcSshTunnelDesc_lbl,"skip, wrap, hidemode 3");

		panel.add(_jdbcSqlInit_lbl,      "");
		panel.add(_jdbcSqlInit_txt,      "push, grow, wrap");
		
		panel.add(_jdbcUrlOptions_lbl,   "");
		panel.add(_jdbcUrlOptions_txt,   "push, grow, split");
		panel.add(_jdbcUrlOptions_but,   "wrap");

//		panel.add(_jdbcSavePassword_chk, "skip, split, growx, pushx");
//		panel.add(_jdbcTestConn_lbl,     "");
		panel.add(_jdbcSqlServerUseEncrypt_chk               , "skip, split, hidemode 2");
		panel.add(_jdbcSqlServerUseTrustServerCertificate_chk, "hidemode 2");
		panel.add(new JLabel(),          "growx, pushx"); // dummy to push next to the right
		panel.add(_jdbcDriverInfo_but,   "hidemode 2");
		panel.add(_jdbcTestConn_but,     "wrap");

		_jdbcProfile_cbx    .setEditable(false);
		_jdbcProfileType_cbx.setEditable(false);
		_jdbcDriver_cbx     .setEditable(true);
		_jdbcUrl_cbx        .setEditable(true);

		// Set how many items the "dropdown" can have before a JScrollBar is visible
		_jdbcProfile_cbx    .setMaximumRowCount(30);
		_jdbcProfileType_cbx.setMaximumRowCount(30);
		_jdbcDriver_cbx     .setMaximumRowCount(30);
		_jdbcUrl_cbx        .setMaximumRowCount(30);
		
//		_jdbcSqlServerUseWindowsAuthentication_chk.setVisible(false);
		setSqlServerUseWindowsAuthenticationVisible(false);
		setSqlServerUseEncryptVisible(false);
		setSqlServerUseTrustServerCertificateVisible(false);

		// NOTE: initialization of the getAvailableDriverList() is now asynchronous (since it takes a long time)
		//       So therefore: I added the _jdbcDriver_cbx.addPopupMenuListener() which will add the JDBC Drivers at a later time.
		List<String> driversList = JdbcDriverHelper.getAvailableDriverList();
		if (driversList.size() > 0)
		{
			for (String str : driversList)
				_jdbcDriver_cbx.addItem(str);
		}
		_jdbcDriver_cbx.addPopupMenuListener(new PopupMenuListener()
		{
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent paramPopupMenuEvent)
			{
				if (_jdbcDriver_cbx.getItemCount() == 0)
				{
					List<String> driversList = JdbcDriverHelper.getAvailableDriverList();
					if (driversList.size() > 0)
					{
						for (String str : driversList)
							_jdbcDriver_cbx.addItem(str);
					}
				}
			}
			
			@Override public void popupMenuWillBecomeInvisible(PopupMenuEvent paramPopupMenuEvent) {}
			@Override public void popupMenuCanceled(PopupMenuEvent paramPopupMenuEvent) {}
		});
		_jdbcDriver_cbx.setRenderer(new JdbcDriverHelper.JdbcDriverComboBoxRender());

		// http://www.h2database.com/html/features.html#database_url
		_jdbcUrl_cbx   .addItem("jdbc:h2:file:[<path>]<dbname>;IFEXISTS=TRUE");
		_jdbcUrl_cbx   .addItem("jdbc:h2:file:[<path>]<dbname>;IFEXISTS=TRUE;AUTO_SERVER=TRUE");
		_jdbcUrl_cbx   .addItem("jdbc:h2:zip:<zipFileName>!/<dbname>");
		_jdbcUrl_cbx   .addItem("jdbc:h2:tcp://<host>[:<port>]/<dbname>");
		_jdbcUrl_cbx   .addItem("jdbc:h2:ssl://<host>[:<port>]/<dbname>");
 
		// http://infocenter.sybase.com/help/topic/com.sybase.dc39001_0605/html/prjdbc/X39384.htm
		_jdbcUrl_cbx   .addItem("jdbc:sybase:Tds:<host>:<port>");
		_jdbcUrl_cbx   .addItem("jdbc:sybase:Tds:<host>:<port>[/<dbname>]");
//		_jdbcUrl_cbx   .addItem("jdbc:sybase:Tds:<host>:<port>[/<dbname>][?OPT1=val,&OPT2=val]");

		
		// ACTIONS
		_jdbcProfile_cbx    .addActionListener(this);
		_jdbcProfileType_cbx.addActionListener(this);
		_jdbcProfileSave_but.addActionListener(this);
		_jdbcProfileNew_but .addActionListener(this);
		_jdbcDriver_cbx     .addActionListener(this);
		_jdbcTestConn_but   .addActionListener(this);
		_jdbcUrl_cbx        .getEditor().getEditorComponent().addKeyListener(this);
		_jdbcUrl_cbx        .addActionListener(this);
		_jdbcPassword_txt   .addActionListener(this);
		_jdbcSqlServerUseWindowsAuthentication_chk .addActionListener(this);
		_jdbcSqlServerUseEncrypt_chk               .addActionListener(this);
		_jdbcSqlServerUseTrustServerCertificate_chk.addActionListener(this);
		_jdbcUrl_but        .addActionListener(this);
		_jdbcUrlOptions_txt .addActionListener(this);
		_jdbcUrlOptions_but .addActionListener(this);
		_jdbcSshTunnel_chk  .addActionListener(this);
		_jdbcSshTunnel_but  .addActionListener(this);

		_jdbcDriverInfo_but .addActionListener(this);

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
				"Or what drivers has been registered in the Java DriverManager.");

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
//			else if ("com.sybase.jdbc42.jdbc.SybDriver".equals(className)) { desc = "Sybase JDBC 4.2 Driver"; homePage = "www.sybase.com/jconnect"; }
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
						
						row.set(PCS_TAB_POS_TIMEOUT,    Integer.valueOf(cm.getQueryTimeout()));
						row.set(PCS_TAB_POS_POSTPONE,   Integer.valueOf(cm.getPostponeTime()));
						row.set(PCS_TAB_POS_STORE_PCS,  Boolean.valueOf(cm.isPersistCountersEnabled()));
						row.set(PCS_TAB_POS_STORE_ABS,  Boolean.valueOf(cm.isPersistCountersAbsEnabled()));
						row.set(PCS_TAB_POS_STORE_DIFF, Boolean.valueOf(cm.isPersistCountersDiffEnabled()));
						row.set(PCS_TAB_POS_STORE_RATE, Boolean.valueOf(cm.isPersistCountersRateEnabled()));
	
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
		static final long serialVersionUID = 1L;

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
//				CountersModel cm  = GetCounters.getInstance().getCmByDisplayName(tabName);
				CountersModel cm  = CounterController.getInstance().getCmByDisplayName(tabName);
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
		if (_options._showPcsTab)
		{
			if (_aseOptionStore_chk.isSelected())
			{
//				_tab.setVisibleAtModel(TAB_POS_PCS, true);
//				_tab.setEnabledAtModel(TAB_POS_PCS, true);
				_tab.setEnabledAtTitle(TAB_TITLE_PCS, true);
			}
			else
			{
//				_tab.setVisibleAtModel(TAB_POS_PCS, false);
//				_tab.setEnabledAtModel(TAB_POS_PCS, false);
				_tab.setEnabledAtTitle(TAB_TITLE_PCS, false);
			}
		}
	}
	private void toggleHostmonTab()
	{
		if (_options._showHostmonTab)
		{
			if (_aseHostMonitor_chk.isSelected())
			{
//				_tab.setVisibleAtModel(TAB_POS_HOSTMON, true);
//				_tab.setEnabledAtModel(TAB_POS_HOSTMON, true);
				_tab.setEnabledAtTitle(TAB_TITLE_HOSTMON, true);
			}
			else
			{
//				_tab.setVisibleAtModel(TAB_POS_HOSTMON, false);
//				_tab.setEnabledAtModel(TAB_POS_HOSTMON, false);
				_tab.setEnabledAtTitle(TAB_TITLE_HOSTMON, false);
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
		boolean checkHostPort  = true;

		String curTabName = _tab.getSelectedTitle(false);

		if (TAB_TITLE_ASE.equals(curTabName))
		{
			checkMoreStuff = true;
			checkHostPort  = true;

			if (_aseUsername_txt.getText().trim().equals("")) 
				aseProblem = "ASE User name must be specified";
			
			if (_aseSshTunnel_chk.isSelected() && _aseSshTunnelInfo != null && ! _aseSshTunnelInfo.isValid() )
			{
				String problem = _aseSshTunnelInfo.getInvalidReason();
				aseProblem = "SSH Tunnel is incomplete (TDS): " + problem;
			}
		}

		else if (TAB_TITLE_OFFLINE.equals(curTabName))
		{
			checkMoreStuff = false;
			checkHostPort  = false;

			String url = _offlineJdbcUrl_cbx.getEditor().getItem().toString();
			// URL
			if ( url.matches(".*<.*>.*") )
				offlineProblem = "Replace the <template> with something.";

			if ( url.matches(".*\\[.*\\].*"))
				offlineProblem = "Replace the [template] with something or delete it.";
		}

		else if (TAB_TITLE_JDBC.equals(curTabName))
		{
			checkMoreStuff = true;
			checkHostPort  = false;

//			String url = _jdbcUrl_cbx.getEditor().getItem().toString();
//			// URL
//			if ( url.matches(".*<.*>.*") )
//				jdbcProblem = "Replace the <template> with something.";
//
//			if ( url.matches(".*\\[.*\\].*"))
//				jdbcProblem = "Replace the [template] with something or delete it.";

			if (_jdbcSshTunnel_chk.isSelected() && _jdbcSshTunnelInfo != null && ! _jdbcSshTunnelInfo.isValid() )
			{
				String problem = _jdbcSshTunnelInfo.getInvalidReason();
				jdbcProblem = "SSH Tunnel is incomplete (JDBC): " + problem;
			}
		}

		if (checkMoreStuff)
		{
			if (_options._showDbxTuneOptionsInTds || _options._showDbxTuneOptionsInJdbc) // then the createAseTuneOptionsPanel() has been done, and we want to check this
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
	
				if (_aseHostMonitor_chk.isSelected())
				{
					String username = _hostmonUsername_txt.getText();
					String password = _hostmonPassword_txt.getText();
					String hostname = _hostmonHost_txt    .getText();
					String portStr  = _hostmonPort_txt    .getText();
					String keyFile  = _hostmonKeyFile_txt .getText();
		
					if (_hostMonLocalOsCmd_chk.isSelected() && StringUtil.isNullOrBlank(_hostmonLocalOsCmdWrapper_txt.getText()))
					{
						_hostmonLocalOsCmdWrapper_txt.setEnabled(true);

						SwingUtils.setEnabled(_hostmonUserPasswd_pan, false);
						SwingUtils.setEnabled(_hostmonServer_pan    , false);
					}
					else
					{
						_hostmonLocalOsCmdWrapper_txt.setEnabled(true);

						SwingUtils.setEnabled(_hostmonUserPasswd_pan, true);
						SwingUtils.setEnabled(_hostmonServer_pan    , true);

						if (_hostMonLocalOsCmd_chk.isSelected() == false)
						{
							_hostmonLocalOsCmdWrapper_txt.setEnabled(false);

							try { Integer.parseInt(portStr); } 
							catch (NumberFormatException e) 
							{
								hostmonProblem = "SSH Port number must be an integer";
							}
				
							if (StringUtil.isNullOrBlank(username))
								hostmonProblem = "SSH username must be specified";
				
							if (StringUtil.isNullOrBlank(password) && StringUtil.isNullOrBlank(keyFile) )
								hostmonProblem = "SSH password OR keyFile must be specified";
				
							if (StringUtil.isNullOrBlank(hostname))
								hostmonProblem = "SSH hostname must be specified";
						}
					}
				}
			}

			if (checkHostPort)
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

//		Color fgColor = _tab.getForegroundAtModel( _tab.getSelectedIndexModel() );
		Color fgColor = Color.BLACK;
		_tab.setForegroundAtModel(TAB_POS_HOSTMON, fgColor);
		_tab.setForegroundAtModel(TAB_POS_PCS,     fgColor);
		_tab.setForegroundAtModel(TAB_POS_OFFLINE, fgColor);

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

			if ( StringUtil.hasValue(hostmonProblem)) _tab.setForegroundAtModel(TAB_POS_HOSTMON, Color.RED);
			if ( StringUtil.hasValue(pcsProblem))     _tab.setForegroundAtModel(TAB_POS_PCS,     Color.RED);
			if ( StringUtil.hasValue(offlineProblem)) _tab.setForegroundAtModel(TAB_POS_OFFLINE, Color.RED);
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
		if (StringUtil.isNullOrBlank(file))
		{
			_logger.debug("loadNewInterfaces(): The passed interfaces file was null empty string.");
			return;
		}

		if (file.equals(_aseIfile_txt.getText()) && file.equals(AseConnectionFactory.getIFileName()))
		{
			_logger.debug("loadNewInterfaces(): Same file as currently loaded, return...");
			return;
		}
		
		try
		{
			File f = new File(file);
			
			// File DOESN'T exists
			if ( ! f.exists() )
			{
				String interfacesFileName = "%SYBASE%\\ini\\sql.ini";
				if (PlatformUtils.getCurrentPlattform() != PlatformUtils.Platform_WIN)
					interfacesFileName = "$SYBASE/interfaces";

				String privateInterfacesFile = AseConnectionFactory.getPrivateInterfacesFile(true);
				_logger.info("Trying to open the local "+Version.getAppName()+" Name/Directory Service file '"+privateInterfacesFile+"'.");
				AseConnectionFactory.createPrivateInterfacesFile(privateInterfacesFile); // if it exists, this does nothing...
				System.setProperty("interfaces.file", privateInterfacesFile);

				String htmlMsg = "<html>" + 
						"The Name Service file '"+file+"' doesn't exists.<br>" +
						"<br>" +
						"Instead, lets try to use the default <i>private</i> Name Service file: " + privateInterfacesFile + "<br>" +
						"<br>" +
						"<b>Note</b>: If the faulty file was restored from a Connection Profile, <b>you need save the profile</b> to get rid of this message in the future</b><br>" +
						"<br>" +
						"Name Service file is used to lookup a Sybase Server into a hostname and port number.<br>" +
						"The default Sybase Name Service file is normally named '"+interfacesFileName+"'.<br>" + 
						"</html>";
				SwingUtils.showWarnMessage(this, "Name Service file dosn't exists", htmlMsg, null);
				file = privateInterfacesFile;
				_aseIfile_txt.setText(file);
//				return;
			}

			// Can we READ the file
			if ( ! FileUtils.canRead(file) )
			{
				_logger.warn("The file '"+file+"' is NOT Readable...");
			}

			// Can we WRITE to the file
			if ( ! FileUtils.canWrite(file) )
			{
				_logger.warn("The file '"+file+"' is NOT Writable...");

				// The copy interfaces file is instead done:
				// - When you try to edit the interfaces file
				// - or when you try to add "unknown" entries after a successfull connect
				// Having it in here was, bad... incase a shared-read-only interfaces file was used
				// AND that users do not want to add/copy entries to the "private" file...
				// So deffering the copy was a better option
				
				//file = ConnectionProfileManager.getInstance().copyInterfacesFileToPrivateFile(file);
			}

			// Load the file
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
					String currentSrvName = StringUtil.getSelectedItemString(_aseServer_cbx); 
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
		refreshServers();
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
					if (_aseUsername_txt.getText().trim().equals("")) {_aseUsername_txt.requestFocus(); return; }
					if (_asePassword_txt.getText().trim().equals("")) {_asePassword_txt.requestFocus(); return; }
					if (_aseHost_txt    .getText().trim().equals("")) {_aseHost_txt    .requestFocus(); return; }
					if (_asePort_txt    .getText().trim().equals("")) {_asePort_txt    .requestFocus(); return; }
				}
				else if (TAB_TITLE_HOSTMON.equals(_tab.getSelectedTitle(false)))
				{
					if (_hostmonUsername_txt.getText().trim().equals("")) {_hostmonUsername_txt.requestFocus(); return; }
					if (_hostmonPassword_txt.getText().trim().equals("")) {_hostmonPassword_txt.requestFocus(); return; }
					if (_hostmonHost_txt    .getText().trim().equals("")) {_hostmonHost_txt    .requestFocus(); return; }
					if (_hostmonPort_txt    .getText().trim().equals("")) {_hostmonPort_txt    .requestFocus(); return; }
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

//	public static boolean checkReconnectVersion(Connection conn)
//	{
//		// If reconnected, check if it's the same version.
//		// NOTE: MonTablesDictionary is initialized at a later stage first time we connect
//		//       so if we dont have a instance, this would be the first time we connect.
//		//       THIS MIGHT CHANGE IN FUTURE.
//		if (MonTablesDictionary.hasInstance())
//		{
//			MonTablesDictionary mtd = MonTablesDictionary.getInstance();
//			if ( ! mtd.isInitialized() )
//			{
//				_logger.debug("checkReconnectVersion(): MonTablesDictionary.isInitialized()=false. I'll just return true here...");
//				return true;
//			}
//			int currentVersion = mtd.getAseExecutableVersionNum();
//			int newVersion     = AseConnectionUtils.getAseVersionNumber(conn);
//	
//			if (currentVersion <= 0)
//			{
//				_logger.debug("checkReconnectVersion(): MonTablesDictionary.hasInstance()=true, Are we checking this a bit early... currentVersion='"+currentVersion+"', newVersion='"+newVersion+"'. since currentVersion is <= 0, I'll just return true here...", new Exception("Dummy Exception to get call stack..."));
//				return true;
//			}
//
//			if (currentVersion != newVersion)
//			{
//				String msg = "<html>" +
//						"Connecting to a different DB Version, This is NOT supported now... (previousVersion='"+Ver.versionNumToStr(currentVersion)+"', connectToVersion='"+Ver.versionNumToStr(newVersion)+"'). <br>" +
//						"To connect to another DB Version, you need to restart the application." +
//						"</html>";
//				if (DbxTune.hasGui())
//					JOptionPane.showMessageDialog(MainFrame.getInstance(), msg, Version.getAppName()+" - connect check", JOptionPane.WARNING_MESSAGE);
//				_logger.warn(msg);
//				return false;
//			}
//			else
//			{
//				_logger.info("Re-connected to the same DB Version as priviously. Version is '"+currentVersion+"'.");
//				return true;
//			}
//		}
//		return true;
//	}

	/**
	 * Make a connection to the ASE
	 * @param connProfile 
	 * @return
	 */
	private boolean aseConnect(ConnectionProfile connProfile)
	{
		String  sqlInit               = _aseSqlInit_txt.getText();
		boolean tdsUseUrl             = _aseConnUrl_chk.isSelected();
		String  tdsUseUrlStr          = _aseConnUrl_txt.getText();
		String  tdsUrlOptions         = _aseOptions_txt.getText();

		String username               = _aseUsername_txt.getText();
		String password               = _asePassword_txt.getText();
		String hosts                  = _aseHost_txt.getText();
		String ports                  = _asePort_txt.getText();

		boolean       aseSshTunnel    = _aseSshTunnel_chk.isSelected();
		SshTunnelInfo sshTunnelInfo   = _aseSshTunnelInfo;

		String        loginTimeoutStr = _aseLoginTimeout_txt.getText().trim();
		
		ImageIcon     srvIcon         = ConnectionProfileManager.getIcon32(ConnectionProfile.SrvType.TDS_ASE);

		if (connProfile != null)
		{
			ConnectionProfile.TdsEntry entry = connProfile.getTdsEntry();

			sqlInit        = entry._tdsSqlInit;
			tdsUseUrl      = entry._tdsUseUrl;
			tdsUseUrlStr   = entry._tdsUseUrlStr;
			tdsUrlOptions  = entry._tdsUrlOptions;

			username       = entry._tdsUsername;
			password       = entry._tdsPassword;
			hosts          = entry._tdsHosts;
			ports          = entry._tdsPorts;

			sshTunnelInfo  = entry._tdsShhTunnelInfo;
			aseSshTunnel   = entry._tdsShhTunnelUse;
			
			loginTimeoutStr= entry._tdsLoginTimout + "";
			
			srvIcon         = ConnectionProfileManager.getIcon32(connProfile.getSrvType());
		}

		// -------------------------------------------
		// if RAW URL is used
		// -------------------------------------------
		// FIXME: this we need to redo, I need to remember stuff for reconnects etc
		if (tdsUseUrl)
		{
			Properties props = new Properties();
			props.put("user",     username);
			props.put("password", password);
			try
			{
				_logger.info("Connecting to DB Server using RAW-URL username='"+username+"', URL='"+tdsUseUrlStr+"'.");
//				_aseConn = ConnectionProgressDialog.connectWithProgressDialog(this, AseConnectionFactory.getDriver(), tdsUseUrlStr, props, _checkAseCfg, _sshConn, sshTunnelInfo, _desiredProductName, sqlInit);
//				_aseConn = ConnectionProgressDialog.connectWithProgressDialog(this, AseConnectionFactory.getDriver(), tdsUseUrlStr, props, _options._srvExtraChecks, _sshConn, sshTunnelInfo, _desiredProductName, sqlInit, srvIcon);
//				_aseConn = AseConnectionFactory.getConnection(AseConnectionFactory.getDriver(), rawUrl, props, null);

				// Set DBX Connection Defaults
				ConnectionProp cp = new ConnectionProp();
				cp.setLoginTimeout ( loginTimeoutStr );
				cp.setDriverClass  ( AseConnectionFactory.getDriver() );
				cp.setUrl          ( tdsUseUrlStr );
				cp.setUrlOptions   ( tdsUrlOptions );
				cp.setUsername     ( username );
				cp.setPassword     ( password );
				cp.setAppName      ( Version.getAppName() );
				cp.setAppVersion   ( Version.getVersionStr() );
				cp.setSshTunnelInfo( sshTunnelInfo );

				Connection conn = ConnectionProgressDialog.connectWithProgressDialog(this, AseConnectionFactory.getDriver(), tdsUseUrlStr, props, cp, _options._srvExtraChecks, _sshConn, sshTunnelInfo, _desiredProductName, sqlInit, srvIcon);
				_aseConn = DbxConnection.createDbxConnection(conn);

				DbxConnection.setDefaultConnProp(cp);
				
				if (_aseConn != null)
					_aseConn.setConnProp(cp);

				return true;
			}
			catch (SQLException e)
			{
				String msg = AseConnectionUtils.showSqlExceptionMessage(this, "Problems Connecting", "Problems when connecting to the data server.", e); 
				_logger.warn("Problems when connecting to a DB Server. "+msg);
				return false;
			}
			catch (Exception e)
			{
				SwingUtils.showErrorMessage(this, "Problems Connecting", "Problems when connecting to the data server.\n\n" + e.getMessage(), e);
				_logger.warn("Problems when connecting to a DB Server. Caught: "+e);
				return false;
			}
			//<<<<<----- RETURN after this
		}

		// set login timeout property
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if ( conf != null && StringUtil.hasValue(loginTimeoutStr) )
		{
			conf.setProperty(AseConnectionFactory.PROPKEY_LOGINTIMEOUT, loginTimeoutStr);
		}

		// -------------------------------------------
		// NORMAL connect starts here
		// -------------------------------------------
		String[] sa = StringUtil.commaStrToArray(ports);
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
					"The port number '"+ports+"' is either missing or is not a number.", null);
				return false;
		}

		_logger.debug("Setting connection info to AseConnectionFactory appname='"+Version.getAppName()
				+"', user='"+username+"', password='"+password
				+"', host='"+hosts+"', port='"+ports+"'.");

		// reset the sshTunnel info if not selected
		if ( ! aseSshTunnel )
			sshTunnelInfo = null;

		// The AseConnectionFactory will be phased out and replaced with DbxConnection
		AseConnectionFactory.setAppName ( Version.getAppName() );
//		AseConnectionFactory.setHostName( Version.getVersionStr() );
		AseConnectionFactory.setAppVersion( Version.getVersionStr() );
		AseConnectionFactory.setUser    ( username );
		AseConnectionFactory.setPassword( password );
//		AseConnectionFactory.setHost    ( hosts );
//		AseConnectionFactory.setPort    ( port );
		AseConnectionFactory.setHostPort(hosts, ports);

		// set options... if there are any
		if ( StringUtil.hasValue(tdsUrlOptions) )
		{
			Map<String,String> options = StringUtil.parseCommaStrToMap(tdsUrlOptions);
			AseConnectionFactory.setProperties(options);
		}
		else
		{
			Map<String,String> options = null;
			AseConnectionFactory.setProperties(options);
		}

		
		// Set DBX Connection Defaults
		ConnectionProp cp = new ConnectionProp();
		cp.setLoginTimeout ( loginTimeoutStr );
		cp.setDriverClass  ( AseConnectionFactory.getDriver() );
		cp.setUrl          ( AseConnectionFactory.getUrlTemplateBase() + AseConnectionFactory.getHostPortStr() );
		cp.setUrlOptions   ( tdsUrlOptions );
		cp.setUsername     ( username );
		cp.setPassword     ( password );
		cp.setAppName      ( Version.getAppName() );
		cp.setAppVersion   ( Version.getVersionStr() );
//		cp.setHostPort     ( hosts, ports );
		cp.setSshTunnelInfo( sshTunnelInfo );

		DbxConnection.setDefaultConnProp(cp);
		
		
		try
		{
			_logger.info("Connecting to DB Server '"+AseConnectionFactory.getServer()+"'.  hostPortStr='"+AseConnectionFactory.getHostPortStr()+"', user='"+AseConnectionFactory.getUser()+"'.");

			String urlStr = AseConnectionFactory.getUrlTemplateBase() + AseConnectionFactory.getHostPortStr();
//			_aseConn = ConnectionProgressDialog.connectWithProgressDialog(this, urlStr, _options._srvExtraChecks, _sshConn, sshTunnelInfo, _desiredProductName, sqlInit, srvIcon);
			_aseConn = ConnectionProgressDialog.connectWithProgressDialog(this, urlStr, cp, _options._srvExtraChecks, _sshConn, sshTunnelInfo, _desiredProductName, sqlInit, srvIcon);
//			_aseConn = DbxConnection.createDbxConnection(conn);
//String TDS_SSH_TUNNEL_CONNECTION  = _aseConn.getClientInfo("TDS_SSH_TUNNEL_CONNECTION");
//String TDS_SSH_TUNNEL_INFORMATION = _aseConn.getClientInfo("TDS_SSH_TUNNEL_INFORMATION");
//System.out.println("TDS_SSH_TUNNEL_CONNECTION="+TDS_SSH_TUNNEL_CONNECTION);
//System.out.println("TDS_SSH_TUNNEL_INFORMATION="+TDS_SSH_TUNNEL_INFORMATION);

			if (_aseConn != null)
				_aseConn.setConnProp(cp);

			return true;
		}
		catch (SQLException e)
		{
			// The below shows a showErrorMessage
			String msg = AseConnectionUtils.showSqlExceptionMessage(this, "Problems Connecting", "Problems when connecting to the data server.", e); 
			_logger.warn("Problems when connecting to a  Server. "+msg);
			return false;
		}
		catch (Exception e)
		{
			_logger.warn("Problems when connecting to a DB Server. Caught: "+e);
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
	 * @param connProfile 
	 * @return
	 */
//	private SshConnection hostmonCreateConnectionObject(ConnectionProfile connProfile)
//	{
//		String username = _hostmonUsername_txt.getText();
//		String password = _hostmonPassword_txt.getText();
//		String hostname = _hostmonHost_txt    .getText();
//		String portStr  = _hostmonPort_txt    .getText();
//		int port = 22;
//		String keyFile  = _hostmonKeyFile_txt.getText();
//
//		if (connProfile != null)
//		{
////			ConnectionProfile.TdsEntry entry = connProfile.getTdsEntry();
//			ConnectionProfile.DbxTuneParams entry = connProfile.getDbxTuneParams(); 
//
//			username = entry._osMonUsername;
//			password = entry._osMonPassword;
//			hostname = entry._osMonHost;
//			portStr  = entry._osMonPort + "";
//			keyFile  = entry._osMonKeyFile;
//		}
//
//		try { port = Integer.parseInt(portStr); } 
//		catch (NumberFormatException e) 
//		{
//			SwingUtils.showErrorMessage(this, "Problem with port number", 
//				"The port number '"+portStr+"' is not a number.", e);
//			return null;
//		}
//
//
//		_logger.info("Creating SSH Connection-Object to hostname='"+hostname+"'.  port='"+port+"', username='"+username+"', keyFile='"+keyFile+"'.");
//		return new SshConnection(hostname, port, username, password, keyFile);
//	}
	/**
	 * Make a HostMonitor connection object, but do NOT connect
	 * @param connProfile 
	 * @return
	 */
	private HostMonitorConnection hostmonCreateConnectionObject(ConnectionProfile connProfile)
	{
		String username = _hostmonUsername_txt.getText();
		String password = _hostmonPassword_txt.getText();
		String hostname = _hostmonHost_txt    .getText();
		String portStr  = _hostmonPort_txt    .getText();
		int port = 22;
		String keyFile  = _hostmonKeyFile_txt.getText();

		boolean localOsCmd        = _hostMonLocalOsCmd_chk.isSelected();
		String  localOsCmdWrapper = _hostmonLocalOsCmdWrapper_txt.getText();
		
		if (connProfile != null)
		{
//			ConnectionProfile.TdsEntry entry = connProfile.getTdsEntry();
			ConnectionProfile.DbxTuneParams entry = connProfile.getDbxTuneParams(); 

			username = entry._osMonUsername;
			password = entry._osMonPassword;
			hostname = entry._osMonHost;
			portStr  = entry._osMonPort + "";
			keyFile  = entry._osMonKeyFile;

			localOsCmd        = entry._osMonLocalOsCmd;
			localOsCmdWrapper = entry._osMonLocalOsCmdWrapper;
		}

		try { port = Integer.parseInt(portStr); } 
		catch (NumberFormatException e) 
		{
			SwingUtils.showErrorMessage(this, "Problem with port number", 
				"The port number '"+portStr+"' is not a number.", e);
			return null;
		}


		_logger.info("Creating HostMon Connection-Object to hostname='"+hostname+"'.  port='"+port+"', username='"+username+"', keyFile='"+keyFile+"', localOsCmd='"+localOsCmd+"', localOsCmdWrapper='"+localOsCmdWrapper+"'.");
		
		HostMonitorConnection hostMonConn;
		if (localOsCmd)
		{
			if (StringUtil.isNullOrBlank(localOsCmdWrapper))
			{
				hostMonConn = new HostMonitorConnectionLocalOsCmd(true); // true means: Create it with "isConnected() == true"
			}
			else
			{
				Map<String, String> envMap = new HashMap<>();
				if (StringUtil.hasValue(hostname)) envMap.put("SSH_HOSTNAME", hostname);
				if (StringUtil.hasValue(portStr )) envMap.put("SSH_PORT"    , portStr);
				if (StringUtil.hasValue(username)) envMap.put("SSH_USERNAME", username);
				if (StringUtil.hasValue(password)) envMap.put("SSH_PASSWORD", password);
				if (StringUtil.hasValue(keyFile )) envMap.put("SSH_KEYFILE" , keyFile);

				hostMonConn = new HostMonitorConnectionLocalOsCmdWrapper(true, localOsCmdWrapper, envMap);
			}
		}
		else
		{
			SshConnection sshConn = new SshConnection(hostname, port, username, password, keyFile);
			hostMonConn = new HostMonitorConnectionSsh(sshConn);
		}
		return hostMonConn;
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
	 * @param connProfile 
	 * @return
	 */
	private boolean pcsConnect(ConnectionProfile connProfile)
	{
//System.out.println("pcsConnect(): connProfile="+connProfile);
		//---------------------------
		// START the Persistent Storage thread
		//---------------------------
		try
		{
			checkForH2LocalDrive(null);

			Configuration pcsProps = new Configuration();

			String pcsAll = _pcsWriter_cbx.getEditor().getItem().toString();
			if (connProfile != null)
			{
				pcsAll = connProfile.getDbxTuneParams()._pcsWriterClass;
			}
			pcsProps.setProperty(PersistentCounterHandler.PROPKEY_WriterClass, pcsAll);
//System.out.println("pcsConnect(): pcsAll='"+pcsAll+"'.");

			// pcsAll "could" be a ',' separated string
			// But I don't know how to set the properties for those Writers
			for (String pcs : pcsAll.split(","))
			{
//System.out.println("pcsConnect(): pcs='"+pcs+"'.");
				if (pcs.startsWith("com.asetune."))
				{
					String tmpSave = pcs;
					pcs = pcs.replace("com.asetune.", "com.dbxtune.");

					Exception tmpEx = new Exception("MAKE THE LOG MESSAGE STICK OUT: Found old configuration '" + tmpSave+ "' which was replaced with '" + pcs + "'."); 
					_logger.warn("Found old configuration '" + tmpSave+ "' which was replaced with '" + pcs + "'.", tmpEx);
				}

				if (pcs.equals("com.dbxtune.pcs.PersistWriterJdbc"))
				{
//System.out.println("pcsConnect(): XXXXXX: com.dbxtune.pcs.PersistWriterJdbc");
					if (connProfile != null)
					{
//						ConnectionProfile.TdsEntry tdsEntry = connProfile.getTdsEntry();
						ConnectionProfile.DbxTuneParams entry = connProfile.getDbxTuneParams(); 

//System.out.println(">>>>PROFILE>>>>>>> XML = "+entry.toXml());
//System.out.println(">>>>PROFILE>>>>>>> entry._pcsWriterDdlLookup = "+entry._pcsWriterDdlLookup);
//System.out.println(">>>>PROFILE>>>>>>> entry._pcsWriterCapSql_doSqlCaptureAndStore = "+entry._pcsWriterCapSql_doSqlCaptureAndStore);
//System.out.println(">>>>PROFILE>>>>>>> entry._pcsWriterPassword = "+entry._pcsWriterPassword);

						pcsProps.setProperty(PersistWriterJdbc.PROPKEY_jdbcDriver,   entry._pcsWriterDriver);
						pcsProps.setProperty(PersistWriterJdbc.PROPKEY_jdbcUrl,      entry._pcsWriterUrl);
						pcsProps.setProperty(PersistWriterJdbc.PROPKEY_jdbcUsername, entry._pcsWriterUsername);
						pcsProps.setProperty(PersistWriterJdbc.PROPKEY_jdbcPassword, entry._pcsWriterPassword == null ? "" : entry._pcsWriterPassword); // do not write NULL objects to the HashTable, then it's NullPointerException 
	
						pcsProps.setProperty(PersistWriterJdbc.PROPKEY_startH2NetworkServer, entry._pcsWriterStartH2asNwServer);
	
						// DDL
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_ddl_doDdlLookupAndStore,             entry._pcsWriterDdlLookup);
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_ddl_enabledForDatabaseObjects,       entry._pcsWriterDdlLookupEnabledForDatabaseObjects);
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_ddl_enabledForStatementCache,        entry._pcsWriterDdlLookupEnabledForStatementCache);
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_ddl_addDependantObjectsToDdlInQueue, entry._pcsWriterDdlStoreDependantObjects);
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_ddl_afterDdlLookupSleepTimeInMs,     entry._pcsWriterDdlLookupSleepTime);

						// SQL Capture
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_sqlCap_doSqlCaptureAndStore             , entry._pcsWriterCapSql_doSqlCaptureAndStore             );
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_sqlCap_doSqlText                        , entry._pcsWriterCapSql_doSqlText                        );
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_sqlCap_doStatementInfo                  , entry._pcsWriterCapSql_doStatementInfo                  );
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_sqlCap_doPlanText                       , entry._pcsWriterCapSql_doPlanText                       );
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_sqlCap_sleepTimeInMs                    , entry._pcsWriterCapSql_sleepTimeInMs                    );
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_execTime        , entry._pcsWriterCapSql_saveStatement_gt_execTime        );
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_logicalReads    , entry._pcsWriterCapSql_saveStatement_gt_logicalReads    );
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_physicalReads   , entry._pcsWriterCapSql_saveStatement_gt_physicalReads   );
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_sqlCap_sendDdlForLookup                 , entry._pcsWriterCapSql_sendDdlForLookup                 );
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_sqlCap_sendDdlForLookup_gt_execTime     , entry._pcsWriterCapSql_sendDdlForLookup_gt_execTime     );
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_sqlCap_sendDdlForLookup_gt_logicalReads , entry._pcsWriterCapSql_sendDdlForLookup_gt_logicalReads );
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_sqlCap_sendDdlForLookup_gt_physicalReads, entry._pcsWriterCapSql_sendDdlForLookup_gt_physicalReads);
					}
					else
					{
						pcsProps.setProperty(PersistWriterJdbc.PROPKEY_jdbcDriver,   _pcsJdbcDriver_cbx  .getEditor().getItem().toString());
						pcsProps.setProperty(PersistWriterJdbc.PROPKEY_jdbcUrl,      _pcsJdbcUrl_cbx     .getEditor().getItem().toString());
						pcsProps.setProperty(PersistWriterJdbc.PROPKEY_jdbcUsername, _pcsJdbcUsername_txt.getText());
						pcsProps.setProperty(PersistWriterJdbc.PROPKEY_jdbcPassword, _pcsJdbcPassword_txt.getText());
	
						pcsProps.setProperty(PersistWriterJdbc.PROPKEY_startH2NetworkServer, _pcsH2Option_startH2NetworkServer_chk.isSelected() + "");
	
						// DDL
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_ddl_doDdlLookupAndStore,             _pcsDdl_doDdlLookupAndStore_chk            .isSelected() + "");
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_ddl_enabledForDatabaseObjects,       _pcsDdl_enabledForDatabaseObjects_chk      .isSelected() + "");
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_ddl_enabledForStatementCache,        _pcsDdl_enabledForStatementCache_chk       .isSelected() + "");
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_ddl_addDependantObjectsToDdlInQueue, _pcsDdl_addDependantObjectsToDdlInQueue_chk.isSelected() + "");
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_ddl_afterDdlLookupSleepTimeInMs,     _pcsDdl_afterDdlLookupSleepTimeInMs_txt    .getText());

						// SQL Capture
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_sqlCap_doSqlCaptureAndStore             , _pcsCapSql_doSqlCaptureAndStore_chk         .isSelected());
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_sqlCap_doSqlText                        , _pcsCapSql_doSqlText_chk                    .isSelected());
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_sqlCap_doStatementInfo                  , _pcsCapSql_doStatementInfo_chk              .isSelected());
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_sqlCap_doPlanText                       , _pcsCapSql_doPlanText_chk                   .isSelected());
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_sqlCap_sleepTimeInMs                    , _pcsCapSql_sleepTimeInMs_txt                .getText());
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_execTime        , _pcsCapSql_saveStatement_execTime_txt       .getText());
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_logicalReads    , _pcsCapSql_saveStatement_logicalRead_txt    .getText());
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_physicalReads   , _pcsCapSql_saveStatement_physicalRead_txt   .getText());
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_sqlCap_sendDdlForLookup                 , _pcsCapSql_sendDdlForLookup_chk             .isSelected());
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_sqlCap_sendDdlForLookup_gt_execTime     , _pcsCapSql_sendDdlForLookup_execTime_txt    .getText());
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_sqlCap_sendDdlForLookup_gt_logicalReads , _pcsCapSql_sendDdlForLookup_logicalRead_txt .getText());
						pcsProps.setProperty(PersistentCounterHandler.PROPKEY_sqlCap_sendDdlForLookup_gt_physicalReads, _pcsCapSql_sendDdlForLookup_physicalRead_txt.getText());
//System.out.println(">>>>GUI>>>>>>> _pcsDdl_doDdlLookupAndStore_chk.isSelected() = "+_pcsDdl_doDdlLookupAndStore_chk.isSelected());
//System.out.println(">>>>GUI>>>>>>> _pcsCapSql_doSqlCaptureAndStore_chk.isSelected() = "+_pcsCapSql_doSqlCaptureAndStore_chk.isSelected());
//System.out.println(">>>>GUI>>>>>>> _pcsJdbcPassword_txt.getText() = "+_pcsJdbcPassword_txt.getText());
					}
				}
			}

//System.out.println(">>000>>> PROPKEY_ddl_doDdlLookupAndStore:     '"+PersistentCounterHandler.PROPKEY_ddl_doDdlLookupAndStore+"'    : "+pcsProps.getProperty(PersistentCounterHandler.PROPKEY_ddl_doDdlLookupAndStore));
//System.out.println(">>000>>> PROPKEY_sqlCap_doSqlCaptureAndStore: '"+PersistentCounterHandler.PROPKEY_sqlCap_doSqlCaptureAndStore+"': "+pcsProps.getProperty(PersistentCounterHandler.PROPKEY_sqlCap_doSqlCaptureAndStore));
			// Craete the Vendor Specific Object Inspector and SqlCaptureBroker
			IObjectLookupInspector oli = DbxTune.getInstance().createPcsObjectLookupInspector();
			ISqlCaptureBroker      scb = DbxTune.getInstance().createPcsSqlCaptureBroker();

//System.out.println("pcsConnect(): IObjectLookupInspector='"+oli+"'.");
//System.out.println("pcsConnect(): ISqlCaptureBroker='"+scb+"'.");
//System.out.println("pcsConnect(): pcsProps="+StringUtil.toCommaStr(pcsProps));

//System.out.println(">>111>>> PROPKEY_ddl_doDdlLookupAndStore:     '"+PersistentCounterHandler.PROPKEY_ddl_doDdlLookupAndStore+"'    : "+pcsProps.getProperty(PersistentCounterHandler.PROPKEY_ddl_doDdlLookupAndStore));
//System.out.println(">>111>>> PROPKEY_sqlCap_doSqlCaptureAndStore: '"+PersistentCounterHandler.PROPKEY_sqlCap_doSqlCaptureAndStore+"': "+pcsProps.getProperty(PersistentCounterHandler.PROPKEY_sqlCap_doSqlCaptureAndStore));
			
			// Create the Persistance Counter Handler and initialize it
			PersistentCounterHandler pch = new PersistentCounterHandler(oli, scb);
			pch.init( pcsProps );
//System.out.println(">>222>>> PROPKEY_ddl_doDdlLookupAndStore:     '"+PersistentCounterHandler.PROPKEY_ddl_doDdlLookupAndStore+"'    : "+pcsProps.getProperty(PersistentCounterHandler.PROPKEY_ddl_doDdlLookupAndStore));
//System.out.println(">>222>>> PROPKEY_sqlCap_doSqlCaptureAndStore: '"+PersistentCounterHandler.PROPKEY_sqlCap_doSqlCaptureAndStore+"': "+pcsProps.getProperty(PersistentCounterHandler.PROPKEY_sqlCap_doSqlCaptureAndStore));

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
			
			SwingUtils.showErrorMessage(this, "Init PCS Error", "Problems initializing PersistentCounterHandler", e);
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
	 * @param connProfile 
	 * @return
	 */
	private boolean offlineConnect(ConnectionProfile connProfile)
	{
		String jdbcDriver = _offlineJdbcDriver_cbx.getEditor().getItem().toString();
		String jdbcUrl    = _offlineJdbcUrl_cbx.getEditor().getItem().toString();
		String jdbcUser   = _offlineJdbcUsername_txt.getText();
		String jdbcPasswd = _offlineJdbcPassword_txt.getText();

		boolean startH2NetworkServer = _offlineH2Option_startH2NwSrv_chk.isSelected();

		if (connProfile != null)
		{
			ConnectionProfile.OfflineEntry entry = connProfile.getOfflineEntry();
			
			jdbcDriver = entry._jdbcDriver;
			jdbcUrl    = entry._jdbcUrl;
			jdbcUser   = entry._jdbcUsername;
			jdbcPasswd = entry._jdbcPassword;

			startH2NetworkServer = entry._H2Option_startH2NwSrv;
			
			// FIXME: hmm why doesn't this one have a local variable...
			//        Lets set the GUI property for the moment
			_offlineCheckForNewSessions_chk.setSelected(entry._checkForNewSessions);
		}
		
		String configStr = "jdbcDriver='"+jdbcDriver+"', jdbcUrl='"+jdbcUrl+"', jdbcUser='"+jdbcUser+"', jdbcPasswd='*hidden*'.";
		_logger.info("Configuration for PersistReader component named 'PersistReader': "+configStr);

		//-----------------------------------------------------
		// IF Jdbc driver: H2
		//-----------------------------------------------------
		if ( jdbcDriver.equals("org.h2.Driver") )
		{
//			// start the H2 TCP Server
//			if (startH2NetworkServer)
//			{
//				try
//				{
//					_logger.info("Starting a H2 TCP server.");
//					org.h2.tools.Server h2ServerTcp = org.h2.tools.Server.createTcpServer("-tcpAllowOthers");
//					h2ServerTcp.start();
//		
//		//			_logger.info("H2 TCP server, listening on port='"+h2Server.getPort()+"', url='"+h2Server.getURL()+"', service='"+h2Server.getService()+"'.");
//					_logger.info("H2 TCP server, url='"+h2ServerTcp.getURL()+"', service='"+h2ServerTcp.getService()+"'.");
//		
//					if (true)
//					{
//						try
//						{
//							_logger.info("Starting a H2 WEB server.");
//							//String[] argsWeb = new String[] { "-trace" };
//							org.h2.tools.Server h2ServerWeb = org.h2.tools.Server.createWebServer();
//							h2ServerWeb.start();
//
//							_logger.info("H2 WEB server, url='"+h2ServerWeb.getURL()+"', service='"+h2ServerWeb.getService()+"'.");
//						}
//						catch (Exception e)
//						{
//							_logger.info("H2 WEB server, failed to start, but I will continue anyway... Caught: "+e);
//						}
//					}
//
//					if (true)
//					{
//						try
//						{
//							_logger.info("Starting a H2 Postgres server.");
//							org.h2.tools.Server h2ServerPostgres = org.h2.tools.Server.createPgServer("-pgAllowOthers");
//							h2ServerPostgres.start();
//			
//							_logger.info("H2 Postgres server, url='"+h2ServerPostgres.getURL()+"', service='"+h2ServerPostgres.getService()+"'.");
//						}
//						catch (Exception e)
//						{
//							_logger.info("H2 Postgres server, failed to start, but I will continue anyway... Caught: "+e);
//						}
//					}
//				}
//				catch (SQLException e) 
//				{
//					_logger.warn("Problem starting H2 network service", e);
//				}
//			}
//			// start the H2 TCP Server
//			if (startH2NetworkServer)
//			{
//				try
//				{
//					boolean writeDbxTuneServiceFile = false;
//					String baseDir = StringUtil.getEnvVariableValue("DBXTUNE_SAVE_DIR");
//
//					List<String> tcpSwitches = new ArrayList<>();
//					List<String> webSwitches = new ArrayList<>();
//					List<String> pgSwitches  = new ArrayList<>();
//
//					boolean startTcpServer = Configuration.getCombinedConfiguration().getBooleanProperty("h2.tcp.startServer", true);
//					boolean startWebServer = Configuration.getCombinedConfiguration().getBooleanProperty("h2.web.startServer", true);
//					boolean startPgServer  = Configuration.getCombinedConfiguration().getBooleanProperty("h2.pg.startServer",  true);
//
//					int     tcpBasePortNumber  = Configuration.getCombinedConfiguration().getIntProperty("h2.tcp.basePort", 19092);
//					int     webBasePortNumber  = Configuration.getCombinedConfiguration().getIntProperty("h2.web.basePort", 18082);
//					int     pgBasePortNumber   = Configuration.getCombinedConfiguration().getIntProperty("h2.pg.basePort",  15435);
//					
//					//-------------------------------------------
//					// Switches to TCP server
//					tcpSwitches.add("-tcpAllowOthers");    // Allow other that the localhost to connect
//					tcpSwitches.add("-tcpPort");           // Try this port as a base, if it's bussy, H2 will grab "next" available
//					tcpSwitches.add(""+tcpBasePortNumber); // Try this port as a base, if it's bussy, H2 will grab "next" available
//					tcpSwitches.add("-ifExists");          // If the database file DO NOT exists, DO NOT CREATE one
//					if (StringUtil.hasValue(baseDir))
//					{
//						tcpSwitches.add("-baseDir");
//						tcpSwitches.add(baseDir);
//						
//						writeDbxTuneServiceFile = true;
//					}
//					
//					//-------------------------------------------
//					// Switches to WEB server
//					webSwitches.add("-webAllowOthers"); // Allow other that the localhost to connect
//					webSwitches.add("-webPort");           // Try this port as a base, if it's bussy, H2 will grab "next" available
//					webSwitches.add(""+webBasePortNumber); // Try this port as a base, if it's bussy, H2 will grab "next" available
//					webSwitches.add("-ifExists");          // If the database file DO NOT exists, DO NOT CREATE one
//					if (StringUtil.hasValue(baseDir))
//					{
//						webSwitches.add("-baseDir");
//						webSwitches.add(baseDir);
//					}
//
//					//-------------------------------------------
//					// Switches to POSTGRES server
//					pgSwitches.add("-pgAllowOthers"); // Allow other that the localhost to connect
//					pgSwitches.add("-pgPort");           // Try this port as a base, if it's bussy, H2 will grab "next" available
//					pgSwitches.add(""+pgBasePortNumber); // Try this port as a base, if it's bussy, H2 will grab "next" available
//					pgSwitches.add("-ifExists");          // If the database file DO NOT exists, DO NOT CREATE one
//					if (StringUtil.hasValue(baseDir))
//					{
//						pgSwitches.add("-baseDir");
//						pgSwitches.add(baseDir);
//					}
//					
//					
//					//java -cp ${H2_JAR} org.h2.tools.Server -tcp -tcpAllowOthers -tcpPort ${portStart} -ifExists -baseDir ${baseDir} &
//
//					org.h2.tools.Server h2TcpServer = null;
//					org.h2.tools.Server h2WebServer = null;
//					org.h2.tools.Server h2PgServer  = null;
//					
//					if (startTcpServer)
//					{
//						_logger.info("Starting a H2 TCP server. Switches: "+tcpSwitches);
//						h2TcpServer = org.h2.tools.Server.createTcpServer(tcpSwitches.toArray(new String[0]));
//						h2TcpServer.start();
//			
//			//			_logger.info("H2 TCP server, listening on port='"+h2TcpServer.getPort()+"', url='"+h2TcpServer.getURL()+"', service='"+h2TcpServer.getService()+"'.");
//						_logger.info("H2 TCP server, url='"+h2TcpServer.getURL()+"', Status='"+h2TcpServer.getStatus()+"'.");
//					}
//		
//					if (startWebServer)
//					{
//						try
//						{
//							_logger.info("Starting a H2 WEB server. Switches: "+webSwitches);
//							h2WebServer = org.h2.tools.Server.createWebServer(webSwitches.toArray(new String[0]));
//							h2WebServer.start();
//
//							_logger.info("H2 WEB server, url='"+h2WebServer.getURL()+"', Status='"+h2WebServer.getStatus()+"'.");
//						}
//						catch (Exception e)
//						{
//							_logger.info("H2 WEB server, failed to start, but I will continue anyway... Caught: "+e);
//						}
//					}
//
//					if (startPgServer)
//					{
//						try
//						{
//							_logger.info("Starting a H2 Postgres server. Switches: "+pgSwitches);
//							h2PgServer = org.h2.tools.Server.createPgServer(pgSwitches.toArray(new String[0]));
//							h2PgServer.start();
//			
//							_logger.info("H2 Postgres server, url='"+h2PgServer.getURL()+"', Status='"+h2PgServer.getStatus()+"'.");
//						}
//						catch (Exception e)
//						{
//							_logger.info("H2 Postgres server, failed to start, but I will continue anyway... Caught: "+e);
//						}
//					}
//					
//					if (writeDbxTuneServiceFile)
//					{
//						try
//						{
//							H2UrlHelper urlHelper = new H2UrlHelper(jdbcUrl);
//
//							// Create a file like: $DBXTUNE_SAVE_DIR/H2DBNAME.dbxtune
//							File f = new File(baseDir + File.separatorChar + urlHelper.getFile().getName() + ".dbxtune");
//							_logger.info("Creating DbxTune - H2 Service information file '" + f.getAbsolutePath() + "'.");
//System.out.println("Creating DbxTune - H2 Service information file '" + f.getAbsolutePath() + "'.");
//							f.createNewFile();
//							f.deleteOnExit();
//
//							PrintStream w = new PrintStream( new FileOutputStream(f) );
//							w.println("dbxtune.pid = " + JavaVersion.getProcessId("-1"));
//							if (h2TcpServer != null)
//							{
//								w.println("h2.tcp.port = " + h2TcpServer.getPort());
//								w.println("h2.tcp.url  = " + h2TcpServer.getURL());
//							}
//							if (h2WebServer != null)
//							{
//								w.println("h2.web.port = " + h2WebServer.getPort());
//								w.println("h2.web.url  = " + h2WebServer.getURL());
//							}
//							if (h2PgServer != null)
//							{
//								w.println("h2.pg.port  = " + h2PgServer.getPort());
//								w.println("h2.pg.url   = " + h2PgServer.getURL());
//							}
//							w.close();
//						}
//						catch (Exception ex)
//						{
//							_logger.warn("Problems creating DbxTune H2 internal service file, continuing anyway. Caught: "+ex);
//						}
//					}
//				}
//				catch (SQLException e) 
//				{
//					_logger.warn("Problem starting H2 network service", e);
//				}
//			}

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
			// Also consider: IGNORECASE and CASE_INSENSITIVE_IDENTIFIERS

//			// The maximum time in milliseconds used to compact a database when closing.
//			if ( ! urlMap.containsKey("MAX_COMPACT_TIME") )
//			{
//				change = true;
//				_logger.info("H2 URL add option: MAX_COMPACT_TIME=2000");
//				urlMap.put("MAX_COMPACT_TIME",  "2000");
//			}

			// AutoServer mode
//			if ( ! urlMap.containsKey("AUTO_SERVER") )
//			{
//				change = true;
//				_logger.info("H2 URL add option: AUTO_SERVER=TRUE");
//				urlMap.put("AUTO_SERVER",  "TRUE");
//			}

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

//		_offlineConn = jdbcConnect(Version.getAppName(), 
//		Connection conn = jdbcConnect(Version.getAppName(), 
//				jdbcDriver, 
//				jdbcUrl,
//				jdbcUser, 
//				jdbcPasswd,
//				null,
//				null,
//				null);
//		_offlineConn = DbxConnection.createDbxConnection(conn);
		_offlineConn = jdbcConnect2( 
				true, // offline connection
				Version.getAppName(), 
				jdbcDriver, 
				jdbcUrl,
				jdbcUser, 
				jdbcPasswd,
				null,
				null,
				null);

		ConnectionProp cp = new ConnectionProp();
		cp.setLoginTimeout ( -1 );
		cp.setDriverClass  ( jdbcDriver );
		cp.setUrl          ( jdbcUrl );
		cp.setUrlOptions   ( (String) null );
		cp.setUsername     ( jdbcUser );
		cp.setPassword     ( jdbcPasswd );
		cp.setAppName      ( Version.getAppName() );
		cp.setAppVersion   ( Version.getVersionStr() );
		cp.setSshTunnelInfo( null );

		DbxConnection.setDefaultConnProp(cp);

		if (_offlineConn != null)
			_offlineConn.setConnProp(cp);
		
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

			String msg = "This is NOT a valid offline database. No DbxTune system table exists.<br>";
			
//			if (jdbcDriver.equals("org.h2.Driver"))
			if (jdbcUrl.startsWith("jdbc:h2:"))
			{
				msg += " If the DB file was transferred from another machine, make sure it was done in <b>binary</b> mode.<br>";

				if (jdbcUrl.indexOf("IFEXISTS=TRUE") == -1)
					msg += " Please append ';IFEXISTS=TRUE' to the URL, this stops H2 to create an empty database if it didn't exist.<br>";
			}

			_logger.error(msg);
			JOptionPane.showMessageDialog(this, 
					"<html>" + msg + "</html>", 
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
	 * @param connProfile 
	 */
	private boolean jdbcConnect(ConnectionProfile connProfile)
	{
		String jdbcDriver           = StringUtil.getSelectedItemString(_jdbcDriver_cbx);
		String jdbcUrl              = StringUtil.getSelectedItemString(_jdbcUrl_cbx);
		String jdbcUser             = _jdbcUsername_txt.getText();
		String jdbcPasswd           = _jdbcPassword_txt.getText();
		String sqlInit              = _jdbcSqlInit_txt.getText();
		String jdbcUrlOptions       = _jdbcUrlOptions_txt.getText();
		boolean jdbcSshTunnelUse    = _jdbcSshTunnel_chk.isSelected();
		SshTunnelInfo sshTunnelInfo = _jdbcSshTunnelInfo;

		if (connProfile != null)
		{
			ConnectionProfile.JdbcEntry entry = connProfile.getJdbcEntry();

			jdbcDriver     = entry._jdbcDriver;
			jdbcUrl        = entry._jdbcUrl;
			jdbcUser       = entry._jdbcUsername;
			jdbcPasswd     = entry._jdbcPassword;
			sqlInit        = entry._jdbcSqlInit;
			jdbcUrlOptions = entry._jdbcUrlOptions;
			sshTunnelInfo  = entry._jdbcShhTunnelInfo;
			jdbcSshTunnelUse = entry._jdbcShhTunnelUse;
		}
//System.out.println("jdbcConnect(): connProfile="+connProfile+", jdbcSshTunnelUse="+jdbcSshTunnelUse+", sshTunnelInfo="+sshTunnelInfo+"("+(sshTunnelInfo == null ? "-NULL-" : sshTunnelInfo.getConfigString(false, true))+")");
if ( ! jdbcSshTunnelUse )
	sshTunnelInfo = null;

//		_jdbcConn = jdbcConnect(Version.getAppName(), 
//				jdbcDriver, 
//				jdbcUrl,
//				jdbcUser, 
//				jdbcPasswd,
//				jdbcUrlOptions,
//				sqlInit,
//				tunnelInfo);


		// Set DBX Connection Defaults
		ConnectionProp cp = new ConnectionProp();
		cp.setLoginTimeout ( -1 );
		cp.setDriverClass  ( jdbcDriver );
		cp.setUrl          ( jdbcUrl );
		cp.setUrlOptions   ( jdbcUrlOptions );
		cp.setUsername     ( jdbcUser );
		cp.setPassword     ( jdbcPasswd );
		cp.setAppName      ( Version.getAppName() );
		cp.setAppVersion   ( Version.getVersionStr() );
		cp.setSshTunnelInfo( sshTunnelInfo );

		DbxConnection.setDefaultConnProp(cp);

		
		_jdbcConn = jdbcConnect2(
				false, // is ofline connection
				Version.getAppName(), 
				jdbcDriver, 
				jdbcUrl,
				jdbcUser, 
				jdbcPasswd,
				jdbcUrlOptions,
				sqlInit,
				sshTunnelInfo);
		
		if (_jdbcConn != null)
			_jdbcConn.setConnProp(cp);

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
	 * @param tunnelInfo 
	 * @throws Exception 
	 */
	private DbxConnection jdbcConnect2(final boolean isOfflineConnection, final String appname, final String driver, final String url, final String user, final String passwd, final String urlOptions, final String sqlInit, final SshTunnelInfo tunnelInfo) 
	{
		Properties props  = new Properties();
	//	Properties props2 = new Properties(); // NOTE declared at the TOP: only used when displaying what properties we connect with
		props.put("user", user);
		props.put("password", passwd);
		
		if (StringUtil.hasValue(urlOptions))
		{
			Map<String, String> urlMap = StringUtil.parseCommaStrToMap(urlOptions);
			for (String key : urlMap.keySet())
			{
				String val = urlMap.get(key);
				
				props .put(key, val);
			}
		}
		
		// Add specific JDBC Properties, for specific URL's, if not already specified
		if (url.startsWith("jdbc:db2:"))
		{
			if ( ! props.containsKey("retrieveMessagesFromServerOnGetMessage") )
			{
				props .put("retrieveMessagesFromServerOnGetMessage", "true");
			}
		}

		try
		{
			String                         desiredProductName = _desiredProductName;
			ConnectionProgressExtraActions connExtraActions   = _options._srvExtraChecks;
			
			if (isOfflineConnection)
			{
				desiredProductName = null;
				connExtraActions   = null;
			}
			
			ConnectionProp connProp = null;

			ImageIcon srvIcon = ConnectionProfileManager.getIcon32byUrl(url);
//System.out.println("jdbcConnect2(): tunnelInfo="+tunnelInfo);
//System.out.println("jdbcConnect2(): sshTunnelInfo="+tunnelInfo+"("+(tunnelInfo == null ? "-NULL-" : tunnelInfo.getConfigString(false, true))+")");

			DbxConnection dbxConn = ConnectionProgressDialog.connectWithProgressDialog(this, driver, url, props, connProp, connExtraActions, _sshConn, tunnelInfo, desiredProductName, sqlInit, srvIcon); 
			return dbxConn;
		}
		catch (Exception ex)
		{
			// Or we can choose to show a DIALOG, but lets skip it for now.
			//ex.printStackTrace();
			return null;
		}
	}

//	private Connection jdbcConnect(final String appname, final String driver, final String url, final String user, final String passwd, final String urlOptions, final String sqlInit, final SshTunnelInfo tunnelInfo)
//	{
//		final Properties props2 = new Properties(); // only used when displaying what properties we connect with
//
//		WaitForExecDialog wait = new WaitForExecDialog(this, "JDBC Connect...");
//		BgExecutor doWork = new BgExecutor(wait)
//		{
//			@Override
//			public Object doWork()
//			{
//				try
//				{
//					// If no suitable driver can be found for the URL, to to load it "the old fashion way" (hopefully it's in the classpath)
//					try
//					{
//						Driver jdbcDriver = DriverManager.getDriver(url);
//						if (jdbcDriver == null)
//							Class.forName(driver).newInstance();
//					}
//					catch (Exception ex)
//					{
//						_logger.warn("Can't load JDBC driver for URL='"+url+"' using 'old way od doing it' using: DriverManager.getDriver(url); Lets continue and try just to use DriverManager.getConnection(url, props); which is the 'new' way of doing it. Caught="+ex);
//						_logger.debug("Can't load JDBC driver for URL='"+url+"' using 'old way od doing it' using: DriverManager.getDriver(url); Lets continue and try just to use DriverManager.getConnection(url, props); which is the 'new' way of doing it. Caught="+ex, ex);
//					}
//
////					Class.forName(driver).newInstance();
////					JdbcDriverHelper.newDriverInstance(driver);
//
//					Properties props  = new Properties();
//				//	Properties props2 = new Properties(); // NOTE declared at the TOP: only used when displaying what properties we connect with
//					props.put("user", user);
//					props.put("password", passwd);
//					
//					if (StringUtil.hasValue(urlOptions))
//					{
//						Map<String, String> urlMap = StringUtil.parseCommaStrToMap(urlOptions);
//						for (String key : urlMap.keySet())
//						{
//							String val = urlMap.get(key);
//							
//							props .put(key, val);
//							props2.put(key, val);
//						}
//					}
//					
//					// Add specific JDBC Properties, for specific URL's, if not already specified
//					if (url.startsWith("jdbc:db2:"))
//					{
//						if ( ! props.containsKey("retrieveMessagesFromServerOnGetMessage") )
//						{
//							props .put("retrieveMessagesFromServerOnGetMessage", "true");
//							props2.put("retrieveMessagesFromServerOnGetMessage", "true");
//						}
//					}
//
//					_logger.debug("getConnection to driver='"+driver+"', url='"+url+"', user='"+user+"'.");
//
//					StringBuilder sb = new StringBuilder();
//					sb.append( "<html>" );
//					sb.append( "<table border=0 cellspacing=1 cellpadding=1>" );
//					sb.append( "<tr> <td nowrap><b>User:  </b></td> <td nowrap>").append( user   ).append("</td> </tr>");
//					sb.append( "<tr> <td nowrap><b>Url:   </b></td> <td nowrap>").append( url    ).append("</td> </tr>");
//					if (props2.size() > 0)
//						sb.append( "<tr> <td nowrap><b>Url Options: </b></td> <td nowrap>").append( StringUtil.toCommaStr(props2) ).append("</td> </tr>");
//					sb.append( "<tr> <td nowrap><b>Driver:</b></td> <td nowrap>").append( driver ).append("</td> </tr>");
//					sb.append( "</table>" );
//					sb.append( "</html>" );
//
//					getWaitDialog().setState(sb.toString());
//					SwingUtils.setWindowMinSize(getWaitDialog());
//
//					Connection conn = DriverManager.getConnection(url, props);
//					
//					// Execute any SQL Init 
//					if (StringUtil.hasValue(sqlInit))
//					{
//						try
//						{
//							String[] sa =  sqlInit.split(";");
//							for (String sql : sa)
//							{
//								sql = sql.trim();
//								if ("".equals(sql))
//									continue;
//								getWaitDialog().setState(
//										"<html>" +
//										"SQL Init: "+ sql + "<br>" +
//										"</html>");
//								DbUtils.exec(conn, sql);
//							}
//						}
//						catch (SQLException ex)
//						{
//							SwingUtils.showErrorMessage(ConnectionDialog.this, "SQL Initialization Failed", 
//									"<html>" +
//									"<h2>SQL Initialization Failed</h2>" +
//									"Full SQL Init String '"+ sqlInit + "'<br>" +
//									"<br>" +
//									"<b>SQL State:     </b>" + ex.getSQLState()  + "<br>" +
//									"<b>Error number:  </b>" + ex.getErrorCode() + "<br>" +
//									"<b>Error Message: </b>" + ex.getMessage()   + "<br>" +
//									"</html>",
//									ex);
//							throw ex;
//						}
//					}
//
//					return conn;
//				}
//				catch (SQLException ex)
//				{
//					SQLException eTmp = ex;
//					StringBuffer sb = new StringBuffer();
//					while (eTmp != null)
//					{
//						sb.append( "\n" );
//						sb.append( "ex.toString='").append( ex.toString()       ).append("', ");
//						sb.append( "Driver='"     ).append( driver              ).append("', ");
//						sb.append( "URL='"        ).append( url                 ).append("', ");
//						sb.append( "User='"       ).append( user                ).append("', ");
//						sb.append( "SQLState='"   ).append( eTmp.getSQLState()  ).append("', ");
//						sb.append( "ErrorCode="   ).append( eTmp.getErrorCode() ).append(", ");
//						sb.append( "Message='"    ).append( eTmp.getMessage()   ).append("', ");
//						sb.append( "classpath='"  ).append( System.getProperty("java.class.path") ).append("'.");
//						eTmp = eTmp.getNextException();
//					}
//					_logger.info(Version.getAppName()+" - JDBC connect FAILED (catch SQLException) Caught: "+sb.toString());
//					setException(ex);
//				}
//				catch (Exception ex)
//				{
//					_logger.info(Version.getAppName()+" - JDBC connect FAILED (catch Exception) Caught: "+ex);
//					setException(ex);
//				}
//				return null;
//			}
//		};
//		Connection conn = (Connection) wait.execAndWait(doWork, 100);
//
//		if (doWork.hasException())
//		{
//			Throwable t = doWork.getException();
//			if (t instanceof SQLException)
//			{
//				SQLException e = (SQLException) t;
//				StringBuffer sb = new StringBuffer();
//				sb.append("<html>");
//				sb.append("<h2>Problems During Connect (SQLException)</h2>");
//				sb.append( "<hr>" );
//				boolean loadDriverProblem = false;
//				while (e != null)
//				{
//					if (e.getMessage().indexOf("No suitable driver") >= 0)
//						loadDriverProblem = true;
//
//					sb.append( "<table border=0 cellspacing=1 cellpadding=1>" );
//					sb.append( "<tr> <td nowrap><b>Message    </b></td> <td nowrap>").append( e.getMessage()   ).append("</td> </tr>");
//					sb.append( "<tr> <td nowrap><b>SQLState   </b></td> <td nowrap>").append( e.getSQLState()  ).append("</td> </tr>");
//					sb.append( "<tr> <td nowrap><b>ErrorCode  </b></td> <td nowrap>").append( e.getErrorCode() ).append("</td> </tr>");
//					sb.append( "<tr> <td nowrap><b>Driver     </b></td> <td nowrap>").append( driver           ).append("</td> </tr>");
//					sb.append( "<tr> <td nowrap><b>URL        </b></td> <td nowrap>").append( url              ).append("</td> </tr>");
//					if (props2.size() > 0)
//						sb.append( "<tr> <td nowrap><b>Url Options: </b></td> <td nowrap>").append( StringUtil.toCommaStr(props2) ).append("</td> </tr>");
//					sb.append( "<tr> <td nowrap><b>User       </b></td> <td nowrap>").append( user             ).append("</td> </tr>");
//					sb.append( "<tr> <td nowrap><b>classpath  </b></td> <td nowrap>").append( System.getProperty("java.class.path") ).append("</td> </tr>");
//					sb.append( "</table>" );
//					sb.append( "<hr>" );
//					e = e.getNextException();
//				}
//				if (loadDriverProblem)
//				{
//						sb.append("<h2>An error occurred while establishing the connection: </h2>");
//						sb.append("The selected Driver cannot handle the specified Database URL. <br>");
//						sb.append("The most common reason for this error is that the database <b>URL contains a syntax error</b> preventing the driver from accepting it. <br>");
//						sb.append("The error also occurs when trying to connect to a database with the wrong driver. Correct this and try again.");
//				}
//				sb.append("</html>");
////				SwingUtils.showErrorMessage(Version.getAppName()+" - jdbc connect", "Connection (SQLException) FAILED.\n\n"+sb.toString(), e);
//				SwingUtils.showErrorMessage(Version.getAppName()+" - jdbc connect", sb.toString(), e);
//			}
//			else if (t instanceof Exception)
//			{
//				SwingUtils.showErrorMessage(Version.getAppName()+" - jdbc connect", "Connection (Exception) FAILED.\n\n"+t.toString(), t);
//			}
//			else
//			{
//				SwingUtils.showErrorMessage(Version.getAppName()+" - jdbc connect", "Connection (other) FAILED.\n\n"+t.toString(), t);
//			}
//		}
//
//		return conn;
//	}

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

	private boolean _inLoadProfile = false;
	@Override
	public void actionPerformed(ActionEvent actionEvent)
	{
		Object source = actionEvent.getSource();
		String action = actionEvent.getActionCommand();

		if (_inLoadProfile)
			return;

		// --- ASE: CHECKBOX: SERVERS ---
		if (_aseServer_cbx.equals(source))
		{
			_logger.debug("_server_cbx.actionPerformed(): getSelectedIndex()='"+_aseServer_cbx.getSelectedIndex()+"', getSelectedItem()='"+StringUtil.getSelectedItemString(_aseServer_cbx)+"'.");
	
			// NOTE: index 0 is "host:port" or SERVER_FIRST_ENTRY("-CHOOSE A SERVER-")
			//       so we wont touch host_txt and port_txt if we are on index 0
			if ( _aseServer_cbx.getSelectedIndex() > 0 )
			{
				String server = StringUtil.getSelectedItemString(_aseServer_cbx);
				
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
//				String envNameSaveDir    = DbxTune.getInstance().getAppSaveDirEnvName();  // DBXTUNE_SAVE_DIR
				String envNameSaveDir    = "DBXTUNE_SAVE_DIR";
				String envNameSaveDirVal = StringUtil.getEnvVariableValue(envNameSaveDir);

				if (envNameSaveDirVal != null)
					dir = envNameSaveDirVal;
			}

			JFileChooser fc = new JFileChooser(dir);
			int returnVal = fc.showOpenDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION) 
			{
				String filename = fc.getSelectedFile().getAbsolutePath();
				loadNewInterfaces( filename );
			}
		}
		
		// --- ASE: BUTTON: "Edit" edit interfaces/sql.ini file ---
		if (_aseEditIfile_but.equals(source))
		{
			String currentSrvName = StringUtil.getSelectedItemString(_aseServer_cbx); 
			String ifile = _aseIfile_txt.getText(); 
			
			// If file isn't writable, do we want to copy the file to a private file?
			if ( ! FileUtils.canWrite(ifile) )
			{
				String newFile = ConnectionProfileManager.getInstance().copyInterfacesFileToPrivateFile(ifile, this);
				if (newFile != null)
					ifile = newFile;
			}

			// Now open the new or old interfaces file
			InterfaceFileEditor ife = new InterfaceFileEditor(this, ifile, currentSrvName);
			int rc = ife.open();
//			if (rc == InterfaceFileEditor.OK)
			if (rc != 999) // always do this
			{
				loadNewInterfaces( ifile );
				refreshServers();
				
				String lastEditSrv = ife.getLastEditServerEntry();
				if (StringUtil.hasValue(lastEditSrv))
					_aseServer_cbx.setSelectedItem(lastEditSrv);
				else
					_aseServer_cbx.setSelectedItem(currentSrvName);
			}
		}
		
		// --- ASE: TEXTFIELD: INTERFACES FILE ---
		if (_aseIfile_txt.equals(source))
		{
			loadNewInterfaces( _aseIfile_txt.getText() );
			// Refresh even if user just pressed "return" to refresh the list
			// since loadNewInterfaces() will do nothing if the interfaces file hasn't changed
			refreshServers();
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
		if (_asePassword_txt.equals(source))
		{
			//saveProps();
			//setVisible(false);
			if (    _aseUsername_txt.getText().trim().equals("")
			     || _aseHost_txt    .getText().trim().equals("")
			     || _asePort_txt    .getText().trim().equals("")
			   )
			{
				setFocus();
			}
			else
			{
				_ok.doClick();
			}
		}

		// --- ASE: NETWORK ENCRYPTION ---
		if (_aseOptionPwdEncryption_chk.equals(source))
		{
			action_nwPasswdEncryption();
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
		
		// --- ASE: COMBOBOX: Charset ---
		if (_aseClientCharset_cbx.equals(source))
		{
			String selectedCharset = StringUtil.getSelectedItemString(_aseClientCharset_cbx);
			
			Map<String,String> optionsMap  = StringUtil.parseCommaStrToMap(_aseOptions_txt.getText());

			if (StringUtil.hasValue(selectedCharset))
				optionsMap.put("CHARSET", selectedCharset);
			else
				optionsMap.remove("CHARSET");

			_aseOptions_txt.setText( StringUtil.toCommaStr(optionsMap, "=", ", ") );
		}
		
		// --- ASE: OPTIONS ---
		if (_aseOptions_txt.equals(source))
		{
			if (StringUtil.hasValue(_aseOptions_txt.getText()))
			{
				Map<String,String> optionsMap  = StringUtil.parseCommaStrToMap(_aseOptions_txt.getText());
				String charset = optionsMap.get("CHARSET");
				
				if (charset != null)
					_aseClientCharset_cbx.setSelectedItem(charset);
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
				conf.setProperty(PROPKEY_CONNECT_ON_STARTUP, _aseOptionConnOnStart_chk.isSelected());
				conf.save();
			}
		}

		// --- ASE: BUTTON: "Template Dialog..." 
		if (_aseOptionUseTemplate_but.equals(source))
		{
			TcpConfigDialog.showDialog(this);
			_aseOptionUseTemplate_mod.refresh();
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
			aseDeferredConnectChkAction(null);
		}

		// --- ASE: CHECKBOX: Deferred DisConnect ---
		if (_aseDeferredDisConnect_chk.equals(source))
		{
			aseDeferredDisConnectChkAction(null);
		}

		// --- HOSTMON: KEY FILE ... ---
		if (_hostmonKeyFile_but.equals(source))
		{
			String dir = System.getProperty("user.home") + File.separatorChar + ".ssh";

			JFileChooser fc = new JFileChooser(dir);
			int returnVal = fc.showOpenDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION) 
			{
				String filename = fc.getSelectedFile().getAbsolutePath();
				_hostmonKeyFile_txt.setText(filename);
			}
		}
		
		// --- PCS: COMBOBOX: JDBC DRIVER ---
		if (_pcsJdbcDriver_cbx.equals(source))
		{
//			String jdbcDriver = _pcsJdbcDriver_cbx.getEditor().getItem().toString();
			String jdbcDriver = StringUtil.getSelectedItemString(_pcsJdbcDriver_cbx);

			if ("org.h2.Driver".equals(jdbcDriver))
			{
				_pcsH2Option_startH2NetworkServer_chk.setVisible(true);
				_pcsDbxTuneSaveDir_lbl.setVisible(true);
				_pcsDbxTuneSaveDir_txt.setVisible(true);
				_pcsDbxTuneSaveDir_but.setVisible(true);
			}
			else
			{
				_pcsH2Option_startH2NetworkServer_chk.setVisible(false);
				_pcsDbxTuneSaveDir_lbl.setVisible(false);
				_pcsDbxTuneSaveDir_txt.setVisible(false);
				_pcsDbxTuneSaveDir_but.setVisible(false);
			}
			
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
				tm.setValueAt(Boolean.valueOf(true), r, PCS_TAB_POS_STORE_PCS);
			}
		}

		// --- PCS: BUTTON: "DeSelect All" 
		if (_pcsTabDeSelectAll_but.equals(source))
		{
			TableModel tm = _pcsSessionTable.getModel();
			for (int r=0; r<tm.getRowCount(); r++)
			{
				tm.setValueAt(Boolean.valueOf(false), r, PCS_TAB_POS_STORE_PCS);
				tm.setValueAt(Integer.valueOf(0),     r, PCS_TAB_POS_POSTPONE);
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
			
			String jdbcUrl = _pcsJdbcUrl_cbx.getEditor().getItem().toString();
			_pcsDbxTuneSaveDir_lbl.setEnabled( jdbcUrl.startsWith("jdbc:h2:") );
			_pcsDbxTuneSaveDir_txt.setEnabled( jdbcUrl.startsWith("jdbc:h2:") );
			_pcsDbxTuneSaveDir_but.setEnabled( jdbcUrl.startsWith("jdbc:h2:") );
		}

		// --- PCS: BUTTON: "..." 
		if (_pcsJdbcUrl_but.equals(source))
		{
			String currentUrl = _pcsJdbcUrl_cbx.getEditor().getItem().toString();
			H2UrlHelper h2help = new H2UrlHelper(currentUrl);

//			String envNameSaveDir    = DbxTune.getInstance().getAppSaveDirEnvName();  // DBXTUNE_SAVE_DIR
			String envNameSaveDir    = "DBXTUNE_SAVE_DIR";
			String envNameSaveDirVal = StringUtil.getEnvVariableValue(envNameSaveDir);

			File baseDir = h2help.getDir(envNameSaveDirVal);
			if (baseDir == null || (baseDir != null && baseDir.toString().equals("${DBXTUNE_SAVE_DIR}")) )
				baseDir = new File(envNameSaveDirVal);

			JFileChooser fc = new JFileChooser(baseDir);

			int returnVal = fc.showOpenDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION) 
			{
				String newFile = fc.getSelectedFile().getAbsolutePath().replace('\\', '/');
				String newUrl  = h2help.getNewUrl(newFile);

				_pcsJdbcUrl_cbx.getEditor().setItem(newUrl);
				checkForH2LocalDrive(null);

				String jdbcUrl = _pcsJdbcUrl_cbx.getEditor().getItem().toString();
				_pcsDbxTuneSaveDir_txt.setEnabled( jdbcUrl.startsWith("jdbc:h2:") );
				_pcsDbxTuneSaveDir_but.setEnabled( jdbcUrl.startsWith("jdbc:h2:") );
			}
		}

		// --- PCS: DBXTUNE_SAVE_DIR - FIELD ---
		if (_pcsDbxTuneSaveDir_txt.equals(source))
		{
			String dbxTuneSaveDir = _pcsDbxTuneSaveDir_txt.getText();
			System.setProperty("DBXTUNE_SAVE_DIR", dbxTuneSaveDir);
			
			Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
			if (conf != null)
			{
				conf.setProperty("DBXTUNE_SAVE_DIR", dbxTuneSaveDir);
				conf.save();
			}
		}

		// --- PCS: DBXTUNE_SAVE_DIR - BUTTON: "..." ---
		if (_pcsDbxTuneSaveDir_but.equals(source))
		{
			String envNameSaveDir    = "DBXTUNE_SAVE_DIR";
			String envNameSaveDirVal = StringUtil.getEnvVariableValue(envNameSaveDir);

			JFileChooser fc = new JFileChooser(envNameSaveDirVal);
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

			int returnVal = fc.showDialog(this, "Select Directory");
//			int returnVal = fc.showOpenDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION) 
			{
				String newFile = fc.getSelectedFile().getAbsolutePath().replace('\\', '/');

				_pcsDbxTuneSaveDir_txt.setText(newFile);
				_pcsDbxTuneSaveDir_txt.postActionEvent(); // Calls this handler but with _pcsDbxTuneSaveDir_txt as source
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
			String jdbcDriver = StringUtil.getSelectedItemString(_offlineJdbcDriver_cbx);
			
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

//				String envNameSaveDir    = DbxTune.getInstance().getAppSaveDirEnvName();  // DBXTUNE_SAVE_DIR
				String envNameSaveDir    = "DBXTUNE_SAVE_DIR";
				String envNameSaveDirVal = StringUtil.getEnvVariableValue(envNameSaveDir);

				File baseDir = h2help.getDir(envNameSaveDirVal);
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
			String jdbcDriver = StringUtil.getSelectedItemString(_jdbcDriver_cbx);

			// Get/Set templates
			setJdbcUrlTemplates(jdbcDriver);
		}
		
		// --- JDBC COMBOBOX: URL ---
		if (_jdbcUrl_cbx.equals(source))
		{
			String url = StringUtil.getSelectedItemString(_jdbcUrl_cbx);

			// Show or hide the MS SQL-Server Windows Authentication CheckBox
			//_jdbcSqlServerUseWindowsAuthentication_chk.setVisible(url.startsWith("jdbc:sqlserver:"));
			setSqlServerUseWindowsAuthenticationVisible (url.startsWith("jdbc:sqlserver:"));
			setSqlServerUseEncryptVisible               (url.startsWith("jdbc:sqlserver:"));
			setSqlServerUseTrustServerCertificateVisible(url.startsWith("jdbc:sqlserver:"));
		}
		
		// --- JDBC: CHECKBOX: "Windows Authentication" 
		if (_jdbcSqlServerUseWindowsAuthentication_chk.equals(source))
		{
			boolean useWinAuth = _jdbcSqlServerUseWindowsAuthentication_chk.isSelected();
			
			Map<String,String> optionsMap  = StringUtil.parseCommaStrToMap(_jdbcUrlOptions_txt.getText());

			if (useWinAuth)
				optionsMap.put("integratedSecurity", "true");
			else
				optionsMap.remove("integratedSecurity");

			setJdbcUrlOptions(optionsMap);
		}


		// --- JDBC: CHECKBOX: "Encryption" 
		if (_jdbcSqlServerUseEncrypt_chk.equals(source))
		{
			boolean useEncryption = _jdbcSqlServerUseEncrypt_chk.isSelected();
			
			Map<String,String> optionsMap  = StringUtil.parseCommaStrToMap(_jdbcUrlOptions_txt.getText());

			if (useEncryption)
				optionsMap.put("encrypt", "true");
			else
			//	optionsMap.remove("encrypt");
				optionsMap.put("encrypt", "false");

			setJdbcUrlOptions(optionsMap);
		}


		// --- JDBC: CHECKBOX: "Trust Server Certificate" 
		if (_jdbcSqlServerUseTrustServerCertificate_chk.equals(source))
		{
			boolean useTrustServerCert = _jdbcSqlServerUseTrustServerCertificate_chk.isSelected();
			
			Map<String,String> optionsMap  = StringUtil.parseCommaStrToMap(_jdbcUrlOptions_txt.getText());

			if (useTrustServerCert)
				optionsMap.put("trustServerCertificate", "true");
			else
			//	optionsMap.remove("trustServerCertificate");
				optionsMap.put("trustServerCertificate", "false");

			setJdbcUrlOptions(optionsMap);
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
		
//				String envNameSaveDir    = DbxTune.getInstance().getAppSaveDirEnvName();  // DBXTUNE_SAVE_DIR
				String envNameSaveDir    = "DBXTUNE_SAVE_DIR";
				String envNameSaveDirVal = StringUtil.getEnvVariableValue(envNameSaveDir);

				File baseDir = h2help.getDir(envNameSaveDirVal);
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
		
		// --- JDBC: CHECKBOX: USE SSH TUNNEL
		if (_jdbcSshTunnel_chk.equals(source))
		{
			String urlHostPortStr = JdbcUrlParser.parse(_jdbcUrl_cbx.getSelectedItem()+"").getHostPortStr();
			_jdbcSshTunnelInfo = SshTunnelDialog.getSshTunnelInfo(urlHostPortStr);
			updateSshTunnelDescription();

			validateContents(); // the ok_lable, seems to be fuckedup if not do this
			SwingUtils.setWindowMinSize(this);
		}
		
		// --- JDBC: BUTTON: "SSH Tunnel"
		if (_jdbcSshTunnel_but.equals(source))
		{
			String hostPortStr = JdbcUrlParser.parse(_jdbcUrl_cbx.getSelectedItem()+"").getHostPortStr();

			SshTunnelDialog dialog = new SshTunnelDialog(this, hostPortStr);
			dialog.setVisible(true);

//			_jdbcSshTunnelInfo = dialog.getSshTunnelInfo();
			_jdbcSshTunnelInfo = SshTunnelDialog.getSshTunnelInfo(hostPortStr);
			updateSshTunnelDescription();
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

		// --- JDBC: SHOW JDBC DRIVER INFO
		if (_jdbcDriverInfo_but.equals(source))
		{
			final JDialog dialog = new JDialog(this, "JDBC Driver Information", true);
			JButton close = new JButton("Close");

			dialog.setLayout(new MigLayout());
			dialog.add(_jdbcDriverInfoPanel, "grow, push, wrap");
			dialog.add(close,                "tag ok, wrap");

			dialog.pack();
			SwingUtils.setLocationCenterParentWindow(this, dialog);

			close.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					dialog.setVisible(false);
				}
			});
			
			dialog.setVisible(true);
		}

		// --- JDBC: Options BUTTON: "..."  (open dialig to choose available options)
		if (_jdbcUrlOptions_but.equals(source))
		{
			Map<String,String> sendOpt = StringUtil.parseCommaStrToMap(_jdbcUrlOptions_txt.getText());
			
			Map<String,String> outOpt = JdbcOptionsDialog.showDialog(this, StringUtil.getSelectedItemString(_jdbcDriver_cbx), StringUtil.getSelectedItemString(_jdbcUrl_cbx), sendOpt);
			// null == CANCEL
			if (outOpt != null)
				setJdbcUrlOptions(outOpt);
		}
		if (_jdbcUrlOptions_txt.equals(source))
		{
			setJdbcUrlOptions(_jdbcUrlOptions_txt.getText());
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

			// Save the connection profile if tree expanded/collapsed has changed
			saveConnectionProfile();
			
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

			int connType = -1;

			// OFFLINE CONNECT
			if (TAB_TITLE_OFFLINE.equals(_tab.getSelectedTitle(false)))
			{
				connType = OFFLINE_CONN;
			}
			// JDBC CONNECT
			else if (TAB_TITLE_JDBC.equals(_tab.getSelectedTitle(false)))
			{
				connType = JDBC_CONN;
			}
			// if current tab is 'PCS' or 'HOSTMON', then choose what type based of what tab's are visible
			else
			{
				if      (_options._showAseTab)  connType = TDS_CONN;
				else if (_options._showJdbcTab) connType = JDBC_CONN;
				else throw new RuntimeException("Sorry I can't figgure out where to connect, please choose tab '"+TAB_TITLE_ASE+"' or '"+TAB_TITLE_JDBC+"'.");
			}

			// Save the connection profile if tree expanded/collapsed has changed
			saveConnectionProfile();
			
			action_connect(connType, null, actionEvent);
		} // end OK

		
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

		// --- ASE: Profile ---
		if (_aseProfile_cbx.equals(source))
		{
			String name = StringUtil.getSelectedItemString(_aseProfile_cbx);
			ConnectionProfile connProfile = ConnectionProfileManager.getInstance().getProfile(name);
			if (connProfile != null)
				load(connProfile);
			else
				_aseServerIcon.setIcon(_aseServerImageIcon);
		}
		if (_aseProfileType_cbx.equals(source))
		{
			action_ConnectionProfileTypeIsSelected(_aseProfileType_cbx, _aseProfile_cbx);
		}
		if (_aseProfileSave_but.equals(source))
		{
			updateConnectionProfile(TDS_CONN, false, StringUtil.getSelectedItemString(_aseProfile_cbx), null, true);
		}
		if (_aseProfileNew_but.equals(source))
		{
			_aseProfile_cbx.setSelectedItem(NO_PROFILE_IS_SELECTED);
			_aseServer_cbx.setSelectedItem(LocalSrvComboBox.SERVER_FIRST_ENTRY);
			_aseUsername_txt.setText("");
			_asePassword_txt.setText("");
			_aseHost_txt    .setText("");
			_asePort_txt    .setText("");
		}
		
		// --- JDBC: Profile ---
		if (_jdbcProfile_cbx.equals(source))
		{
			String name = StringUtil.getSelectedItemString(_jdbcProfile_cbx);
			ConnectionProfile connProfile = ConnectionProfileManager.getInstance().getProfile(name);
			if (connProfile != null)
				load(connProfile);
			else
				_jdbcIcon.setIcon(_jdbcImageIcon);
		}
		if (_jdbcProfileType_cbx.equals(source))
		{
			action_ConnectionProfileTypeIsSelected(_jdbcProfileType_cbx, _jdbcProfile_cbx);
		}
		if (_jdbcProfileSave_but.equals(source))
		{
			updateConnectionProfile(JDBC_CONN, false, StringUtil.getSelectedItemString(_jdbcProfile_cbx), null, true);
		}
		if (_jdbcProfileNew_but.equals(source))
		{
			_jdbcProfile_cbx   .setSelectedItem(NO_PROFILE_IS_SELECTED);
			_jdbcDriver_cbx    .setSelectedItem("");;
			_jdbcUrl_cbx       .setSelectedItem("");;
			_jdbcUsername_txt  .setText("");
			_jdbcPassword_txt  .setText("");
			_jdbcSshTunnel_chk .setSelected(false);
			_jdbcSqlInit_txt   .setText("");
			_jdbcUrlOptions_txt.setText("");
		}
		
		// --- OFFLINE: Profile ---
		if (_offlineProfile_cbx.equals(source))
		{
			String name = StringUtil.getSelectedItemString(_offlineProfile_cbx);
			ConnectionProfile connProfile = ConnectionProfileManager.getInstance().getProfile(name);
			if (connProfile != null)
			{
				load(connProfile);
			}
		}
		if (_offlineProfileType_cbx.equals(source))
		{
			action_ConnectionProfileTypeIsSelected(_offlineProfileType_cbx, _offlineProfile_cbx);
		}
		if (_offlineProfileSave_but.equals(source))
		{
			updateConnectionProfile(OFFLINE_CONN, false, StringUtil.getSelectedItemString(_offlineProfile_cbx), null, true);
		}
		if (_offlineProfileNew_but.equals(source))
		{
			_offlineProfile_cbx.setSelectedItem(NO_PROFILE_IS_SELECTED);
			_offlineJdbcDriver_cbx    .setSelectedItem("");;
			_offlineJdbcUrl_cbx       .setSelectedItem("");;
			_offlineJdbcUsername_txt  .setText("");
			_offlineJdbcPassword_txt  .setText("");
		}
		
		// ALWAYS: do stuff for URL
		setUrlText();

		// ALWAYS: update tunnel description
		updateSshTunnelDescription();
		
		// ALWAYS: update if some fields are enabled or disabled
		updateEnabledForPcsDdlLookupAndSqlCapture();
		
		validateContents();
	}

	private void saveConnectionProfile()
	{
		if ( ! ConnectionProfileManager.hasInstance() )
			return;

		// Loop and SET expanded properties for the tree
		//saveTreeExpansionState(_connProfileTree);
		
		// Save the info to file
		ConnectionProfileManager.getInstance().save();
	}

	private void action_ConnectionProfileTypeIsSelected(JComboBox<ProfileType> profileType_cbx, JComboBox<String> profile_cbx)
	{
//		String profileType = StringUtil.getSelectedItemString(profileType_cbx);
//		if (NO_PROFILE_TYPE_IS_SELECTED.equals(profileType))
//		{
//			// Do nothing
//		}
//		if (EDIT_PROFILE_TYPE_IS_SELECTED.equals(profileType))
//		{
//			System.out.println("OPEN 'PROFILE_TYPE' EDITOR --- NOT-YET-IMPLEMENTED");
//		}
//		else
//		{
//			String profileName = StringUtil.getSelectedItemString(profile_cbx);
//			ConnectionProfile connProfile = ConnectionProfileManager.getInstance().getProfile(profileName);
//			if (connProfile != null)
//				connProfile.setProfileType(profileType);
//			setConnectionProfileType(profileType);
//		}
		
		ProfileType profileType = (ProfileType) profileType_cbx.getSelectedItem();
		if (profileType == null)
			return;

		if (NO_PROFILE_TYPE_IS_SELECTED.equals(profileType))
		{
			// Do nothing
		}
		else if (EDIT_PROFILE_TYPE_IS_SELECTED.equals(profileType))
		{
			ConnectionProfileTypeDialog dialog = new ConnectionProfileTypeDialog(this);
			dialog.setVisible(true);
			
			if (dialog.wasOkPressed())
			{
				// TODO: maybe remember each of the selected items, and restore it after refresh
				_aseProfileType_mod    .refresh();
				_jdbcProfileType_mod   .refresh();
				_offlineProfileType_mod.refresh();
				
				_aseProfileType_mod    .setSelectedItem(NO_PROFILE_TYPE_IS_SELECTED);
				_jdbcProfileType_mod   .setSelectedItem(NO_PROFILE_TYPE_IS_SELECTED);
				_offlineProfileType_mod.setSelectedItem(NO_PROFILE_TYPE_IS_SELECTED);
			}
		}
		else
		{
			// Set the border on the ConnectionDialog
			setBorderForConnectionProfileTypeName(profileType.getName());

// Not 100% sure we should do this here... SInce it's REALLY done in updateConnectionProfile() when a connection is made
//			// Get the selected Connection Profile (if we have selected a profile)
//			String profileName = StringUtil.getSelectedItemString(profile_cbx);
//
//			// Get the PROFILE and set the TYPE
//			ConnectionProfile connProfile = ConnectionProfileManager.getInstance().getProfile(profileName);
//			if (connProfile != null)
//				connProfile.setProfileTypeName(profileType.getName());
		}
	}

	private void setJdbcUrlTemplates(String jdbcDriver)
	{
		List<String> urlTemplates = JdbcDriverHelper.getUrlTemplateList(jdbcDriver);
		if (urlTemplates != null && urlTemplates.size() > 0)
		{
			_jdbcUrl_cbx.removeAllItems();
			for (String template : urlTemplates)
				_jdbcUrl_cbx.addItem(template);
		}

		// Show or hide the MS SQL-Server Windows Authentication CheckBox
		//_jdbcSqlServerUseWindowsAuthentication_chk.setVisible(StringUtil.getSelectedItemString(_jdbcUrl_cbx).startsWith("jdbc:sqlserver:"));
		setSqlServerUseWindowsAuthenticationVisible (StringUtil.getSelectedItemString(_jdbcUrl_cbx).startsWith("jdbc:sqlserver:"));
		setSqlServerUseEncryptVisible               (StringUtil.getSelectedItemString(_jdbcUrl_cbx).startsWith("jdbc:sqlserver:"));
		setSqlServerUseTrustServerCertificateVisible(StringUtil.getSelectedItemString(_jdbcUrl_cbx).startsWith("jdbc:sqlserver:"));
		// Set some default values... for SQL Server
		if (StringUtil.getSelectedItemString(_jdbcUrl_cbx).startsWith("jdbc:sqlserver:"))
		{
			setJdbcUrlOptions("encrypt=true, trustServerCertificate=true");
		}
	}

	private void setJdbcUrlOptions(Map<String, String> optionsMap)
	{
		if (optionsMap == null)
		{
			_jdbcUrlOptions_txt.setText("");
			return;
		}
		_jdbcUrlOptions_txt.setText(StringUtil.toCommaStr(optionsMap, "=", ", "));
		
		// Show or hide the MS SQL-Server Windows Authentication CheckBox
		String url = StringUtil.getSelectedItemString(_jdbcUrl_cbx);
//		_jdbcSqlServerUseWindowsAuthentication_chk.setVisible(url.startsWith("jdbc:sqlserver:"));
		setSqlServerUseWindowsAuthenticationVisible (url.startsWith("jdbc:sqlserver:"));
		setSqlServerUseEncryptVisible               (url.startsWith("jdbc:sqlserver:"));
		setSqlServerUseTrustServerCertificateVisible(url.startsWith("jdbc:sqlserver:"));

		// MS SQL-Server: Windows authentication
		String val = optionsMap.get("integratedSecurity");
//		_jdbcSqlServerUseWindowsAuthentication_chk.setSelected(val != null && val.equalsIgnoreCase("true"));
		setSqlServerUseWindowsAuthenticationSelected (val != null && val.equalsIgnoreCase("true"));
		
		// MS SQL-Server: Encrypt
		val = optionsMap.get("encrypt");
		setSqlServerUseEncryptSelected(val != null && val.equalsIgnoreCase("true"));

		// MS SQL-Server: Trust Server Certificate
		val = optionsMap.get("trustServerCertificate");
		setSqlServerUseTrustServerCertificateSelected(val != null && val.equalsIgnoreCase("true"));
	}
	private void setJdbcUrlOptions(String optionsStr)
	{
		setJdbcUrlOptions(StringUtil.parseCommaStrToMap(optionsStr));
	}

	private void setSqlServerUseWindowsAuthenticationVisible(boolean toValue)
	{
		_jdbcSqlServerUseWindowsAuthentication_chk.setVisible( toValue );

		_jdbcUsername_txt.setEnabled( ! toValue );
		_jdbcPassword_txt.setEnabled( ! toValue );
	}
	private void setSqlServerUseWindowsAuthenticationSelected(boolean toValue)
	{
		_jdbcSqlServerUseWindowsAuthentication_chk.setSelected( toValue );

		_jdbcUsername_txt.setEnabled( ! toValue );
		_jdbcPassword_txt.setEnabled( ! toValue );
	}
	
	private void setSqlServerUseEncryptVisible(boolean toValue)
	{
		_jdbcSqlServerUseEncrypt_chk.setVisible( toValue );
	}
	private void setSqlServerUseEncryptSelected(boolean toValue)
	{
		_jdbcSqlServerUseEncrypt_chk.setSelected( toValue );
	}
	
	private void setSqlServerUseTrustServerCertificateVisible(boolean toValue)
	{
		_jdbcSqlServerUseTrustServerCertificate_chk.setVisible( toValue );

//		_jdbcUsername_txt.setEnabled( ! toValue );
//		_jdbcPassword_txt.setEnabled( ! toValue );
	}
	private void setSqlServerUseTrustServerCertificateSelected(boolean toValue)
	{
		_jdbcSqlServerUseTrustServerCertificate_chk.setSelected( toValue );

//		_jdbcUsername_txt.setEnabled( ! toValue );
//		_jdbcPassword_txt.setEnabled( ! toValue );
	}
	
	private void action_nwPasswdEncryption()
	{
		boolean encrypt = _aseOptionPwdEncryption_chk.isSelected();
		
		Map<String,String> optionsMap  = StringUtil.parseCommaStrToMap(_aseOptions_txt.getText());

		if (encrypt)
		{
			optionsMap.put("ENCRYPT_PASSWORD", "true");
//			optionsMap.put("RETRY_WITH_NO_ENCRYPTION", "true");
		}
		else
		{
			optionsMap.remove("ENCRYPT_PASSWORD");
//			optionsMap.remove("RETRY_WITH_NO_ENCRYPTION");
		}

		_aseOptions_txt.setText( StringUtil.toCommaStr(optionsMap, "=", ", ") );
	}

	private void action_connect(int connType, ConnectionProfile connProfile, ActionEvent actionEvent)
	{
		boolean recordThisSession        = false;
		boolean dbxDeferredConnect       = false;
		boolean dbxHostMonitor           = false;

		// If we call the actionPerformed() from outside, we do not want to save stuff
		if (isVisible())
			saveProps();

		// OFFLINE CONNECT
		if (OFFLINE_CONN == connType)
		{
			// CONNECT to the PCS, if it fails, we stay in the dialog
			if ( _offlineConn == null )
			{
				if ( ! offlineConnect(connProfile) )
					return;
			}

			// Update Connection Profile
			updateConnectionProfile(OFFLINE_CONN, true, StringUtil.getSelectedItemString(_offlineProfile_cbx), connProfile, false);

			_usedConnectionProfileTypeName = (connProfile == null) ? ProfileTypeComboBoxModel.getSelectedProfileTypeName(_offlineProfileType_cbx) : connProfile.getProfileTypeName();
			
			// SET CONNECTION TYP and "CLOSE" the dialog
			_connectionType = OFFLINE_CONN;
			setVisible(false);
		}

		// ASE/JDBC & PCS CONNECT
		else if (TDS_CONN == connType || JDBC_CONN == connType)
		{
			recordThisSession        = _aseOptionStore_chk.isSelected();
			dbxDeferredConnect       = _aseDeferredConnect_chk.isSelected();
			dbxHostMonitor           = _aseHostMonitor_chk.isSelected();

			if (_options._showDbxTuneOptionsInTds || _options._showDbxTuneOptionsInJdbc)
			{
				if (connProfile != null)
				{
//					ConnectionProfile.TdsEntry entry = connProfile.getTdsEntry();
					ConnectionProfile.DbxTuneParams entry = connProfile.getDbxTuneParams(); 
	
					recordThisSession        = entry._dbxtuneOptRecordSession;
					dbxDeferredConnect       = entry._dbxtuneOptConnectLater;
					dbxHostMonitor           = entry._dbxtuneOptOsMonitoring;
				}
	
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
				if (dbxDeferredConnect)
				{
					final String hhmm = aseDeferredConnectChkAction(connProfile);
					if (hhmm != null)
					{
						Date startTime = null;
						try { startTime = PersistWriterBase.getRecordingStartTime(hhmm); }
						catch(Exception ignore) { }
	
						if (startTime != null)
						{
							// Create a Waitfor Dialog
							WaitForExecDialog wait = new WaitForExecDialog(this, "Waiting for a Deferred Connect, at: "+SimpleDateFormat.getDateTimeInstance().format(startTime));
	
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
					if ( ! pcsConnect(connProfile) )
						return;
	
					// setPersistCounters for all CM:s
					if (_useCmForPcsTable && connProfile == null)
					{
						TableModel tm = _pcsSessionTable.getModel();
						for(int r=0; r<tm.getRowCount(); r++)
						{
							for (CountersModel cm : CounterController.getInstance().getCmList())
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
	//					TableModel tm = _pcsSessionTable.getModel();
	//					for(int r=0; r<tm.getRowCount(); r++)
	//					{
	//						String  rowName      = (String)   tm.getValueAt(r, PCS_TAB_POS_CM_NAME);
	//						Integer pcsTimeout   = (Integer)  tm.getValueAt(r, PCS_TAB_POS_TIMEOUT);
	//						Integer pcsPostpone  = (Integer)  tm.getValueAt(r, PCS_TAB_POS_POSTPONE);
	//						boolean pcsStore     = ((Boolean) tm.getValueAt(r, PCS_TAB_POS_STORE_PCS)) .booleanValue();
	//						boolean pcsStoreAbs  = ((Boolean) tm.getValueAt(r, PCS_TAB_POS_STORE_ABS)) .booleanValue();
	//						boolean pcsStoreDiff = ((Boolean) tm.getValueAt(r, PCS_TAB_POS_STORE_DIFF)).booleanValue();
	//						boolean pcsStoreRate = ((Boolean) tm.getValueAt(r, PCS_TAB_POS_STORE_RATE)).booleanValue();
	//						
	//						//System.out.println("OK: name="+StringUtil.left(rowName,25)+", timeout="+pcsTimeout+", postpone="+pcsPostpone+", store="+pcsStore+", abs="+pcsStoreAbs+", diff="+pcsStoreDiff+", rate="+pcsStoreRate+".");
	//					}
					}
				} // end: PCS CONNECT
	
				// OS HOST CONNECT
				if (dbxHostMonitor)
				{
					// Simply set the Connection Object used to connect
					// the ACTUAL SSH Connection will be done as a tak in aseConnect() + ConnectionProgressDialog... 
//					_sshConn = hostmonCreateConnectionObject(connProfile);
					_hostMonConn = hostmonCreateConnectionObject(connProfile);
					if (_hostMonConn instanceof HostMonitorConnectionSsh)
					{
						_sshConn = ((HostMonitorConnectionSsh)_hostMonConn).getSshConnection();
					}
	
				} // end: OS HOST CONNECT
			} // end: _showAseTuneOptions || _showDbxTuneOptionsInJdbc

			if (TDS_CONN == connType)
			{
    			if ( _aseConn == null )
    			{
    				// CONNECT to the ASE
    				boolean ok = aseConnect(connProfile);
    
    				// HOST MONITOR: post fix
//    				if (_sshConn != null && ! _sshConn.isConnected() )
//    				{
//    					_sshConn.close();
//    					_sshConn = null;
//    				}
    				if (_hostMonConn != null && ! _hostMonConn.isConnected() )
    				{
    					_hostMonConn.closeConnection();
    					_hostMonConn = null;
    				}
    				
    				// if it failed: stay in the dialog
    				if ( ! ok  )
    					return;
    			}
			}
			else if (JDBC_CONN == connType)
			{
				// CONNECT to the JDBC, if it fails, we stay in the dialog
				if ( _jdbcConn == null )
				{
    				boolean ok = jdbcConnect(connProfile);

//    				// HOST MONITOR: post fix
//    				if (_sshConn != null && ! _sshConn.isConnected() )
//    				{
//    					_sshConn.close();
//    					_sshConn = null;
//    				}
    				if (_hostMonConn != null && ! _hostMonConn.isConnected() )
    				{
    					_hostMonConn.closeConnection();
    					_hostMonConn = null;
    				}
    				
    				// if it failed: stay in the dialog
    				if ( ! ok  )
    					return;
				}
			}
			else
			{
				// This could not happen but lets put it ion here if we change the code
				throw new RuntimeException("Unknow connection type. connType="+connType);
			}
			
			
			// Set a specific COUNTER TEMPLATE
			if (_options._showDbxTuneOptionsInTds || _options._showDbxTuneOptionsInJdbc)
			{
				boolean aseOptionUseTemplate     = _aseOptionUseTemplate_chk.isSelected();
				String  aseOptionUseTemplateName = StringUtil.getSelectedItemString(_aseOptionUseTemplate_cbx);
	
				if (connProfile != null)
				{
//					ConnectionProfile.TdsEntry entry = connProfile.getTdsEntry();
					ConnectionProfile.DbxTuneParams entry = connProfile.getDbxTuneParams(); 

					aseOptionUseTemplate     = entry._dbxtuneUseTemplate;
					aseOptionUseTemplateName = entry._dbxtuneUseTemplateName;
				}
	
				if (aseOptionUseTemplate)
				{
					if ( NO_TEMPLATE_IS_SELECTED.equals(aseOptionUseTemplateName) )
					{
						// Should we display a message that NO TEMPLATE is selected here???
						// For the moment, just continue...
					}
					else
					{
						try 
						{
							TcpConfigDialog.setTemplate(aseOptionUseTemplateName);
						}
						catch(NameNotFoundException ex)
						{
							SwingUtils.showInfoMessage(this, "Error loading Template", 
									"<html>"
									+ "Can't load the Template Named '"+aseOptionUseTemplateName+"'<br>"
									+ "<br>"
									+ "Continuing the connect process, but without the specified template loaded."
									+ "</html>");
						}
					}
				}

				// Set the desired disconnect time.
				_disConnectTime = null;
				try 
				{ 
					Date stopTime  = PersistWriterBase.getRecordingStopTime(null, aseDeferredDisConnectChkAction(connProfile)); 
					_disConnectTime = stopTime;
				}
				catch(Exception ex) 
				{
					_logger.warn("Could not determen stop/disconnect time, stop time will NOT be set.");
				}
			} // end: _showAseTuneOptions || _showDbxTuneOptionsInJdbc

			// 
			if (dbxDeferredConnect)
			{
				_logger.info("Since 'deferred connect' was enabled, I will skip saving the connection profile. If we did we might 'require a save question/popup' after the connection was made.");
			}
			else
			{
				boolean tryUpdateConnectionProfile = true;
				if (actionEvent != null)
				{
					if (ACTION_EVENT_ID__CONNECT_ON_STARTUP == actionEvent.getID())
					{
						tryUpdateConnectionProfile = false;
					}
				}

				// Update Connection Profile
				if (tryUpdateConnectionProfile)
				{
	    			if (TDS_CONN == connType)
	    			{
	    				updateConnectionProfile(TDS_CONN, true, StringUtil.getSelectedItemString(_aseProfile_cbx), connProfile, false);
	    				
	    				_usedConnectionProfileTypeName = (connProfile == null) ? ProfileTypeComboBoxModel.getSelectedProfileTypeName(_aseProfileType_cbx) : connProfile.getProfileTypeName();
	    			}
	    			else if (JDBC_CONN == connType)
	    			{
	    				updateConnectionProfile(JDBC_CONN, true, StringUtil.getSelectedItemString(_jdbcProfile_cbx), connProfile, false);
	    
	    				_usedConnectionProfileTypeName = (connProfile == null) ? ProfileTypeComboBoxModel.getSelectedProfileTypeName(_jdbcProfileType_cbx) : connProfile.getProfileTypeName();
	    			}
	    			else
	    			{
	    				// This could not happen but lets put it ion here if we change the code
	    				throw new RuntimeException("Unknow connection type. connType="+connType);
	    			}
				}
			}


			// If we call the actionPerformed() from outside, we do not want to save stuff
			if (isVisible())
				saveProps();

			// SET CONNECTION TYP and "CLOSE" the dialog
			_connectionType = connType;
			setVisible(false);

		} // END: ASE & PCS CONNECT
	}

	private void updateConnectionProfile(int connType, boolean afterSuccessfulConnect, String selectedProfileName, ConnectionProfile connProfile, boolean showProfileOverride)
	{
		// Check if it's "<choose a profile>" and if so; return null
		selectedProfileName = ProfileComboBoxModel.notSelectedValueToNull(selectedProfileName);
		
		// Save the last ConnectionProfile... so we can "select it" in the tree view next time we open
		if (StringUtil.hasValue(selectedProfileName))
		{
			_usedConnectionProfileName = (connProfile == null) ? selectedProfileName : connProfile.getName();
		}
		
		// Get what database product we connected to
		String dbProduct = null;
		if (afterSuccessfulConnect)
		{
			try { dbProduct = getDatabaseProductName(); }
			catch (SQLException ignore) {}

			// Not sure if this is a "correct" place to do this, but lets do it for now (when we do disconnect, it "needs" to be cleared)
			SqlUtils.setPrettyPrintDatabaseProductName(dbProduct);
		}

		// Get servername we connected to (if it's available, and implemented for that database product/type)
		String dbServerName = null;
		if (afterSuccessfulConnect)
		{
			try { dbServerName = getDatabaseServerName(); }
			catch (SQLException ignore) {}
		}

		// If a connection profile is passed, then we connected using that connection profile
		// So we don't want to create and send a new entry to the manager
		// JUST update the server type (if current server type is UNKNOWN)
		if (connProfile != null)
		{
			connProfile.setSrvType(dbProduct);
			return;
		}

		if (connType == TDS_CONN)
		{
			// Get host:port combination and use that as a key
			String key = AseConnectionFactory.toHostPortStr(_aseHost_txt.getText(), _asePort_txt.getText());

			ConnectionProfile.TdsEntry tds = new ConnectionProfile.TdsEntry(key);
			tds._profileTypeName        = getSelectedConnectionProfileTypeName(connType);
			tds._tdsIfile               = _aseIfile_txt .getText();
			tds._tdsUsername            = _aseUsername_txt.getText();
			tds._tdsPassword            = _asePassword_txt.getText();
			tds._tdsNwEncryptPasswd     = _aseOptionPwdEncryption_chk.isSelected();
			tds._tdsServer              = StringUtil.getSelectedItemString(_aseServer_cbx);
			tds._tdsHosts               = _aseHost_txt  .getText();
			tds._tdsPorts               = _asePort_txt  .getText();
			tds._tdsDbname              = null;
			tds._tdsLoginTimout         = Integer.parseInt(_aseLoginTimeout_txt.getText());
			tds._tdsShhTunnelUse        = _aseSshTunnel_chk.isSelected();
			tds._tdsShhTunnelInfo       = _aseSshTunnelInfo;
			tds._tdsClientCharset       = StringUtil.getSelectedItemString(_aseClientCharset_cbx);
			tds._tdsSqlInit             = _aseSqlInit_txt.getText();
			tds._tdsUrlOptions          = _aseOptions_txt.getText();
			tds._tdsUseUrl              = _aseConnUrl_chk.isSelected();
			tds._tdsUseUrlStr           = _aseConnUrl_txt.getText(); // actual URL if _aseConnUrl_chk.isSelected is true

			tds._isDbxTuneParamsValid   = _options._showDbxTuneOptionsInTds;
			if (tds._isDbxTuneParamsValid)
			{
				tds._dbxtuneParams = createDbxTuneParams();
//				tds._dbxtuneUseTemplate     = _aseOptionUseTemplate_chk.isSelected();
//				tds._dbxtuneUseTemplateName = _aseOptionUseTemplate_mod.notSelectedValueToNull(StringUtil.getSelectedItemString(_aseOptionUseTemplate_cbx));
//
//				tds._dbxtuneOptRecordSession          = _aseOptionStore_chk.isSelected();
//				tds._dbxtuneOptOsMonitoring           = _aseHostMonitor_chk.isSelected();
//
//				tds._dbxtuneOptConnectAtStartup       = _aseOptionConnOnStart_chk    .isSelected();
//				tds._dbxtuneOptReconnectOnLostConn    = _aseOptionReConnOnFailure_chk.isSelected();
//				tds._dbxtuneOptConnectLater           = _aseDeferredConnect_chk      .isSelected();
//				tds._dbxtuneOptDissConnectLater       = _aseDeferredDisConnect_chk   .isSelected();
//				if (tds._dbxtuneOptConnectLater)
//				{
//					tds._dbxtuneOptConnectLaterHour       = _aseDeferredConnectHour_spm  .getNumber()+"";
//					tds._dbxtuneOptConnectLaterMinute     = _aseDeferredConnectMinute_spm.getNumber()+"";
//				}
//				if (tds._dbxtuneOptConnectLater)
//				{
//					tds._dbxtuneOptDissConnectLaterHour   = _aseDeferredDisConnectHour_spm  .getNumber()+"";
//					tds._dbxtuneOptDissConnectLaterMinute = _aseDeferredDisConnectMinute_spm.getNumber()+"";
//				}
//
//				if (tds._dbxtuneOptOsMonitoring)
//				{
//					tds._osMonUsername      = _hostmonUser_txt         .getText();
//					tds._osMonPassword      = _hostmonPasswd_txt       .getText();
//					tds._osMonSavePassword  = _hostmonOptionSavePwd_chk.isSelected();
//					tds._osMonHost          = _hostmonHost_txt         .getText();
//					tds._osMonPort          = StringUtil.parseInt(_hostmonPort_txt.getText(), 22);
//					tds._osMonKeyFile       = _hostmonKeyFile_txt      .getText();
//				}
//
//				if (tds._dbxtuneOptRecordSession)
//				{
//					tds._pcsWriterClass                    = StringUtil.getSelectedItemString(_pcsWriter_cbx);
//					tds._pcsWriterDriver                   = StringUtil.getSelectedItemString(_pcsJdbcDriver_cbx);
//					tds._pcsWriterUrl                      = StringUtil.getSelectedItemString(_pcsJdbcUrl_cbx);
//					tds._pcsWriterUsername                 = _pcsJdbcUsername_txt    .getText();
//					tds._pcsWriterPassword                 = _pcsJdbcPassword_txt    .getText();
//					tds._pcsWriterSavePassword             = _pcsJdbcSavePassword_chk.isSelected();
//					tds._pcsWriterStartH2asNwServer        = _pcsH2Option_startH2NetworkServer_chk      .isSelected();
//					tds._pcsWriterDdlLookup                = _pcsDdl_doDdlLookupAndStore_chk            .isSelected();
//					tds._pcsWriterDdlStoreDependantObjects = _pcsDdl_addDependantObjectsToDdlInQueue_chk.isSelected();
//					tds._pcsWriterDdlLookupSleepTime       = StringUtil.parseInt(_pcsDdl_afterDdlLookupSleepTimeInMs_txt.getText(), tds._pcsWriterDdlLookupSleepTime);
//				}
			}

			ConnectionProfileManager.getInstance().possiblyAddChange(key, afterSuccessfulConnect, dbProduct, dbServerName, tds, selectedProfileName, ConnectionDialog.this, showProfileOverride);
			//_tdsProfile_mod.refresh();
		}
		else if (connType == OFFLINE_CONN)
		{
			// Get JDBC URL and use that as a key
			String key = StringUtil.getSelectedItemString(_offlineJdbcUrl_cbx);

			ConnectionProfile.OfflineEntry offline = new ConnectionProfile.OfflineEntry(); 
			offline._profileTypeName  = getSelectedConnectionProfileTypeName(connType);
			offline._jdbcDriver       = StringUtil.getSelectedItemString(_offlineJdbcDriver_cbx);
			offline._jdbcUrl          = StringUtil.getSelectedItemString(_offlineJdbcUrl_cbx);
			offline._jdbcUsername     = _offlineJdbcUsername_txt    .getText();
			offline._jdbcPassword     = _offlineJdbcPassword_txt    .getText();
			offline._jdbcSavePassword = _offlineJdbcSavePassword_chk.isSelected();

			offline._checkForNewSessions   = _offlineCheckForNewSessions_chk  .isSelected();
			offline._H2Option_startH2NwSrv = _offlineH2Option_startH2NwSrv_chk.isSelected();

			ConnectionProfileManager.getInstance().possiblyAddChange(key, afterSuccessfulConnect, dbProduct, dbServerName, offline, selectedProfileName, ConnectionDialog.this, showProfileOverride);
			_offlineProfile_mod.refresh();
		}
		else if (connType == JDBC_CONN)
		{
			// Get JDBC URL and use that as a key
			String key = StringUtil.getSelectedItemString(_jdbcUrl_cbx);

			ConnectionProfile.JdbcEntry jdbc = new ConnectionProfile.JdbcEntry(); 
			jdbc._profileTypeName        = getSelectedConnectionProfileTypeName(connType);
			jdbc._jdbcDriver             = StringUtil.getSelectedItemString(_jdbcDriver_cbx);
			jdbc._jdbcUrl                = StringUtil.getSelectedItemString(_jdbcUrl_cbx);
			jdbc._jdbcUsername           = _jdbcUsername_txt.getText();
			jdbc._jdbcPassword           = _jdbcPassword_txt.getText();
			jdbc._jdbcSqlInit            = _jdbcSqlInit_txt.getText();
			jdbc._jdbcUrlOptions         = _jdbcUrlOptions_txt.getText(); // should the be in here???
			jdbc._jdbcSavePassword       = _jdbcSavePassword_chk.isSelected();
			jdbc._jdbcShhTunnelUse       = _jdbcSshTunnel_chk.isSelected();
			jdbc._jdbcShhTunnelInfo      = _jdbcSshTunnelInfo;

			jdbc._isDbxTuneParamsValid   = _options._showDbxTuneOptionsInJdbc;
			if (jdbc._isDbxTuneParamsValid)
			{
				jdbc._dbxtuneParams = createDbxTuneParams();
//System.out.println("xxxxx: jdbc._dbxtuneParams="+jdbc._dbxtuneParams);
			}
			
			ConnectionProfileManager.getInstance().possiblyAddChange(key, afterSuccessfulConnect, dbProduct, dbServerName, jdbc, selectedProfileName, ConnectionDialog.this, showProfileOverride);
			_jdbcProfile_mod.refresh();
		}
		else
		{
			_logger.error("Unknown connection type of '"+connType+"'. This was found in updateConnectionProfile() when trying to update the connection profile.");
			return;
		}
	}

	private DbxTuneParams createDbxTuneParams()
	{
		DbxTuneParams entry = new DbxTuneParams();
		
		entry._dbxtuneUseTemplate     = _aseOptionUseTemplate_chk.isSelected();
		entry._dbxtuneUseTemplateName = _aseOptionUseTemplate_mod.notSelectedValueToNull(StringUtil.getSelectedItemString(_aseOptionUseTemplate_cbx));

		entry._dbxtuneOptRecordSession          = _aseOptionStore_chk.isSelected();
		entry._dbxtuneOptOsMonitoring           = _aseHostMonitor_chk.isSelected();

		entry._dbxtuneOptConnectAtStartup       = _aseOptionConnOnStart_chk    .isSelected();
		entry._dbxtuneOptReconnectOnLostConn    = _aseOptionReConnOnFailure_chk.isSelected();
		entry._dbxtuneOptConnectLater           = _aseDeferredConnect_chk      .isSelected();
		entry._dbxtuneOptDissConnectLater       = _aseDeferredDisConnect_chk   .isSelected();
		if (entry._dbxtuneOptConnectLater)
		{
			entry._dbxtuneOptConnectLaterHour       = _aseDeferredConnectHour_spm  .getNumber()+"";
			entry._dbxtuneOptConnectLaterMinute     = _aseDeferredConnectMinute_spm.getNumber()+"";
		}
		if (entry._dbxtuneOptConnectLater)
		{
			entry._dbxtuneOptDissConnectLaterHour   = _aseDeferredDisConnectHour_spm  .getNumber()+"";
			entry._dbxtuneOptDissConnectLaterMinute = _aseDeferredDisConnectMinute_spm.getNumber()+"";
		}

		if (entry._dbxtuneOptOsMonitoring)
		{
			entry._osMonUsername      = _hostmonUsername_txt     .getText();
			entry._osMonPassword      = _hostmonPassword_txt     .getText();
			entry._osMonSavePassword  = _hostmonOptionSavePwd_chk.isSelected();
			entry._osMonHost          = _hostmonHost_txt         .getText();
			entry._osMonPort          = StringUtil.parseInt(_hostmonPort_txt.getText(), 22);
			entry._osMonKeyFile       = _hostmonKeyFile_txt      .getText();
			
			entry._osMonLocalOsCmd        = _hostMonLocalOsCmd_chk       .isSelected();
			entry._osMonLocalOsCmdWrapper = _hostmonLocalOsCmdWrapper_txt.getText();
		}

		if (entry._dbxtuneOptRecordSession)
		{
			entry._pcsWriterClass                                    = StringUtil.getSelectedItemString(_pcsWriter_cbx);
			entry._pcsWriterDriver                                   = StringUtil.getSelectedItemString(_pcsJdbcDriver_cbx);
			entry._pcsWriterUrl                                      = StringUtil.getSelectedItemString(_pcsJdbcUrl_cbx);
			entry._pcsWriterUsername                                 = _pcsJdbcUsername_txt    .getText().trim();
			entry._pcsWriterPassword                                 = _pcsJdbcPassword_txt    .getText().trim();
			entry._pcsWriterSavePassword                             = _pcsJdbcSavePassword_chk.isSelected();
			entry._pcsWriterStartH2asNwServer                        = _pcsH2Option_startH2NetworkServer_chk      .isSelected();
			entry._pcsWriterDdlLookup                                = _pcsDdl_doDdlLookupAndStore_chk            .isSelected();
			entry._pcsWriterDdlLookupEnabledForDatabaseObjects       = _pcsDdl_enabledForDatabaseObjects_chk      .isSelected();
			entry._pcsWriterDdlLookupEnabledForStatementCache        = _pcsDdl_enabledForStatementCache_chk       .isSelected();
			entry._pcsWriterDdlStoreDependantObjects                 = _pcsDdl_addDependantObjectsToDdlInQueue_chk.isSelected();
			entry._pcsWriterDdlLookupSleepTime                       = StringUtil.parseInt(_pcsDdl_afterDdlLookupSleepTimeInMs_txt.getText(), entry._pcsWriterDdlLookupSleepTime);

			entry._pcsWriterCapSql_doSqlCaptureAndStore              =                     _pcsCapSql_doSqlCaptureAndStore_chk         .isSelected();
			entry._pcsWriterCapSql_doSqlText                         =                     _pcsCapSql_doSqlText_chk                    .isSelected();
			entry._pcsWriterCapSql_doStatementInfo                   =                     _pcsCapSql_doStatementInfo_chk              .isSelected();
			entry._pcsWriterCapSql_doPlanText                        =                     _pcsCapSql_doPlanText_chk                   .isSelected();
			entry._pcsWriterCapSql_sleepTimeInMs                     = StringUtil.parseInt(_pcsCapSql_sleepTimeInMs_txt                .getText(), entry._pcsWriterCapSql_sleepTimeInMs);
			entry._pcsWriterCapSql_saveStatement_gt_execTime         = StringUtil.parseInt(_pcsCapSql_saveStatement_execTime_txt       .getText(), entry._pcsWriterCapSql_saveStatement_gt_execTime     );
			entry._pcsWriterCapSql_saveStatement_gt_logicalReads     = StringUtil.parseInt(_pcsCapSql_saveStatement_logicalRead_txt    .getText(), entry._pcsWriterCapSql_saveStatement_gt_logicalReads );
			entry._pcsWriterCapSql_saveStatement_gt_physicalReads    = StringUtil.parseInt(_pcsCapSql_saveStatement_physicalRead_txt   .getText(), entry._pcsWriterCapSql_saveStatement_gt_physicalReads);
			entry._pcsWriterCapSql_sendDdlForLookup                  =                     _pcsCapSql_sendDdlForLookup_chk             .isSelected();
			entry._pcsWriterCapSql_sendDdlForLookup_gt_execTime      = StringUtil.parseInt(_pcsCapSql_sendDdlForLookup_execTime_txt    .getText(), entry._pcsWriterCapSql_sendDdlForLookup_gt_execTime     );
			entry._pcsWriterCapSql_sendDdlForLookup_gt_logicalReads  = StringUtil.parseInt(_pcsCapSql_sendDdlForLookup_logicalRead_txt .getText(), entry._pcsWriterCapSql_sendDdlForLookup_gt_logicalReads );
			entry._pcsWriterCapSql_sendDdlForLookup_gt_physicalReads = StringUtil.parseInt(_pcsCapSql_sendDdlForLookup_physicalRead_txt.getText(), entry._pcsWriterCapSql_sendDdlForLookup_gt_physicalReads);
		}

		return entry;
	}

	private void loadDbxTuneParams(DbxTuneParams entry)
	{
		_aseOptionUseTemplate_chk.setSelected(    entry._dbxtuneUseTemplate);
		_aseOptionUseTemplate_cbx.setSelectedItem(entry._dbxtuneUseTemplateName); // FIXME default value

		_aseOptionStore_chk.setSelected(entry._dbxtuneOptRecordSession);
		_aseHostMonitor_chk.setSelected(entry._dbxtuneOptOsMonitoring);

		_aseOptionConnOnStart_chk    .setSelected(entry._dbxtuneOptConnectAtStartup);
		_aseOptionReConnOnFailure_chk.setSelected(entry._dbxtuneOptReconnectOnLostConn);
		_aseDeferredConnect_chk      .setSelected(entry._dbxtuneOptConnectLater);
		_aseDeferredDisConnect_chk   .setSelected(entry._dbxtuneOptDissConnectLater);
		if (entry._dbxtuneOptConnectLater)
		{
			_aseDeferredConnectHour_spm  .setValue(Integer.valueOf(entry._dbxtuneOptConnectLaterHour));
			_aseDeferredConnectMinute_spm.setValue(Integer.valueOf(entry._dbxtuneOptConnectLaterMinute));
		}
		if (entry._dbxtuneOptConnectLater)
		{
			_aseDeferredDisConnectHour_spm  .setValue(Integer.valueOf(entry._dbxtuneOptDissConnectLaterHour));
			_aseDeferredDisConnectMinute_spm.setValue(Integer.valueOf(entry._dbxtuneOptDissConnectLaterMinute));
		}

		if (entry._dbxtuneOptOsMonitoring)
		{
			_hostmonUsername_txt     .setText(    entry._osMonUsername);
			_hostmonPassword_txt     .setText(    entry._osMonPassword);
			_hostmonOptionSavePwd_chk.setSelected(entry._osMonSavePassword);
			_hostmonHost_txt         .setText(    entry._osMonHost);
			_hostmonPort_txt         .setText(    entry._osMonPort+"");
			_hostmonKeyFile_txt      .setText(    entry._osMonKeyFile);

			_hostMonLocalOsCmd_chk       .setSelected(entry._osMonLocalOsCmd       );
			_hostmonLocalOsCmdWrapper_txt.setText    (entry._osMonLocalOsCmdWrapper);
		}

		if (entry._dbxtuneOptRecordSession)
		{
			_pcsWriter_cbx                             .setSelectedItem(entry._pcsWriterClass);
			_pcsJdbcDriver_cbx                         .setSelectedItem(entry._pcsWriterDriver);
			_pcsJdbcUrl_cbx                            .setSelectedItem(entry._pcsWriterUrl);
			_pcsJdbcUsername_txt                       .setText(        entry._pcsWriterUsername);
			_pcsJdbcPassword_txt                       .setText(        entry._pcsWriterPassword);
			_pcsJdbcSavePassword_chk                   .setSelected(    entry._pcsWriterSavePassword);
			_pcsH2Option_startH2NetworkServer_chk      .setSelected(    entry._pcsWriterStartH2asNwServer);
			_pcsDdl_doDdlLookupAndStore_chk            .setSelected(    entry._pcsWriterDdlLookup);
			_pcsDdl_enabledForDatabaseObjects_chk      .setSelected(    entry._pcsWriterDdlLookupEnabledForDatabaseObjects);
			_pcsDdl_enabledForStatementCache_chk       .setSelected(    entry._pcsWriterDdlLookupEnabledForStatementCache);
			_pcsDdl_addDependantObjectsToDdlInQueue_chk.setSelected(    entry._pcsWriterDdlStoreDependantObjects);
			_pcsDdl_afterDdlLookupSleepTimeInMs_txt    .setText(        entry._pcsWriterDdlLookupSleepTime+"");
			
			_pcsCapSql_doSqlCaptureAndStore_chk         .setSelected(entry._pcsWriterCapSql_doSqlCaptureAndStore             );
			_pcsCapSql_doSqlText_chk                    .setSelected(entry._pcsWriterCapSql_doSqlText                        );
			_pcsCapSql_doStatementInfo_chk              .setSelected(entry._pcsWriterCapSql_doStatementInfo                  );
			_pcsCapSql_doPlanText_chk                   .setSelected(entry._pcsWriterCapSql_doPlanText                       );
			_pcsCapSql_sleepTimeInMs_txt                .setText(""+ entry._pcsWriterCapSql_sleepTimeInMs                    );
			_pcsCapSql_saveStatement_execTime_txt       .setText(""+ entry._pcsWriterCapSql_saveStatement_gt_execTime        );
			_pcsCapSql_saveStatement_logicalRead_txt    .setText(""+ entry._pcsWriterCapSql_saveStatement_gt_logicalReads    );
			_pcsCapSql_saveStatement_physicalRead_txt   .setText(""+ entry._pcsWriterCapSql_saveStatement_gt_physicalReads   );
			_pcsCapSql_sendDdlForLookup_chk             .setSelected(entry._pcsWriterCapSql_sendDdlForLookup                 );
			_pcsCapSql_sendDdlForLookup_execTime_txt    .setText(""+ entry._pcsWriterCapSql_sendDdlForLookup_gt_execTime     );
			_pcsCapSql_sendDdlForLookup_logicalRead_txt .setText(""+ entry._pcsWriterCapSql_sendDdlForLookup_gt_logicalReads );
			_pcsCapSql_sendDdlForLookup_physicalRead_txt.setText(""+ entry._pcsWriterCapSql_sendDdlForLookup_gt_physicalReads);
		}
	}


	public void load(ConnectionProfile connProfile)
	{
		try
		{
			// indicate to method actionPerformed() that actions should be disabled during load 
			_inLoadProfile = true;
			
			if (connProfile.isType(ConnectionProfile.Type.TDS))
			{
//				SwingUtils.showErrorMessage("NOT IMPLEMETED", "Connection Profile LOAD TDS is not yet implemented", null);
				setSelectedTab(TDS_CONN);
				_aseProfile_cbx.setSelectedItem(connProfile.getName());

				ConnectionProfile.TdsEntry entry = connProfile.getTdsEntry();
				
				setConnectionProfileTypeName(connProfile, entry._profileTypeName);
//				_aseIfile_txt              .setText(        entry._tdsIfile);
				loadNewInterfaces(entry._tdsIfile);
				_aseUsername_txt           .setText(        entry._tdsUsername);
				_asePassword_txt           .setText(        entry._tdsPassword);
				_aseOptionPwdEncryption_chk.setSelected(    entry._tdsNwEncryptPasswd);
				_aseServer_cbx             .setSelectedItem(entry._tdsServer);
				_aseHost_txt               .setText(        entry._tdsHosts);
				_asePort_txt               .setText(        entry._tdsPorts);
//				_aseDbname_txt             .setText(        entry._tdsDbname);
				_aseLoginTimeout_txt       .setText(        entry._tdsLoginTimout+"");
				_aseSshTunnel_chk          .setSelected(    entry._tdsShhTunnelUse);
				_aseSshTunnelInfo          =                entry._tdsShhTunnelInfo;
				_aseClientCharset_cbx      .setSelectedItem(entry._tdsClientCharset);
				_aseSqlInit_txt            .setText(        entry._tdsSqlInit);
				_aseOptions_txt            .setText(        entry._tdsUrlOptions);
				_aseConnUrl_chk            .setSelected(    entry._tdsUseUrl);
				_aseConnUrl_txt            .setText(        entry._tdsUseUrlStr); // actual URL if _aseConnUrl_chk.isSelected is true

				boolean isDbxTuneParamsValid = entry._isDbxTuneParamsValid;
				if (isDbxTuneParamsValid)
				{
					loadDbxTuneParams(entry._dbxtuneParams);

//					_aseOptionUseTemplate_chk.setSelected(    entry._dbxtuneUseTemplate);
//					_aseOptionUseTemplate_cbx.setSelectedItem(entry._dbxtuneUseTemplateName); // FIXME default value
//
//					_aseOptionStore_chk.setSelected(entry._dbxtuneOptRecordSession);
//					_aseHostMonitor_chk.setSelected(entry._dbxtuneOptOsMonitoring);
//
//					_aseOptionConnOnStart_chk    .setSelected(entry._dbxtuneOptConnectAtStartup);
//					_aseOptionReConnOnFailure_chk.setSelected(entry._dbxtuneOptReconnectOnLostConn);
//					_aseDeferredConnect_chk      .setSelected(entry._dbxtuneOptConnectLater);
//					_aseDeferredDisConnect_chk   .setSelected(entry._dbxtuneOptDissConnectLater);
//					if (entry._dbxtuneOptConnectLater)
//					{
//						_aseDeferredConnectHour_spm  .setValue(Integer.valueOf(entry._dbxtuneOptConnectLaterHour));
//						_aseDeferredConnectMinute_spm.setValue(Integer.valueOf(entry._dbxtuneOptConnectLaterMinute));
//					}
//					if (entry._dbxtuneOptConnectLater)
//					{
//						_aseDeferredDisConnectHour_spm  .setValue(Integer.valueOf(entry._dbxtuneOptDissConnectLaterHour));
//						_aseDeferredDisConnectMinute_spm.setValue(Integer.valueOf(entry._dbxtuneOptDissConnectLaterMinute));
//					}
//
//					if (entry._dbxtuneOptOsMonitoring)
//					{
//						_hostmonUser_txt         .setText(    entry._osMonUsername);
//						_hostmonPasswd_txt       .setText(    entry._osMonPassword);
//						_hostmonOptionSavePwd_chk.setSelected(entry._osMonSavePassword);
//						_hostmonHost_txt         .setText(    entry._osMonHost);
//						_hostmonPort_txt         .setText(    entry._osMonPort+"");
//						_hostmonKeyFile_txt      .setText(    entry._osMonKeyFile);
//					}
//
//					if (entry._dbxtuneOptRecordSession)
//					{
//						_pcsWriter_cbx                             .setSelectedItem(entry._pcsWriterClass);
//						_pcsJdbcDriver_cbx                         .setSelectedItem(entry._pcsWriterDriver);
//						_pcsJdbcUrl_cbx                            .setSelectedItem(entry._pcsWriterUrl);
//						_pcsJdbcUsername_txt                       .setText(        entry._pcsWriterUsername);
//						_pcsJdbcPassword_txt                       .setText(        entry._pcsWriterPassword);
//						_pcsJdbcSavePassword_chk                   .setSelected(    entry._pcsWriterSavePassword);
//						_pcsH2Option_startH2NetworkServer_chk      .setSelected(    entry._pcsWriterStartH2asNwServer);
//						_pcsDdl_doDdlLookupAndStore_chk            .setSelected(    entry._pcsWriterDdlLookup);
//						_pcsDdl_addDependantObjectsToDdlInQueue_chk.setSelected(    entry._pcsWriterDdlStoreDependantObjects);
//						_pcsDdl_afterDdlLookupSleepTimeInMs_txt    .setText(        entry._pcsWriterDdlLookupSleepTime+"");
//					}
				}
				
				// Set the ICON
				ImageIcon icon = ConnectionProfileManager.getIcon32(connProfile.getSrvType());
				if (icon != null)
					_aseServerIcon.setIcon(icon);
			}
			else if (connProfile.isType(ConnectionProfile.Type.JDBC))
			{
//				SwingUtils.showErrorMessage("NOT IMPLEMETED", "Connection Profile LOAD JDBC is not yet implemented", null);
				setSelectedTab(JDBC_CONN);
				_jdbcProfile_cbx.setSelectedItem(connProfile.getName());

				ConnectionProfile.JdbcEntry entry = connProfile.getJdbcEntry(); 

				String jdbcUrlOptions = entry._jdbcUrlOptions;
				// Special thing for SQL-Server and with JDBC Driver 10.1 or newer where encrypt=true is the new DEFAULT
				// Then we need to "upgrade" current connection profile with: trustServerCertificate=true
				if (entry._jdbcUrl.startsWith("jdbc:sqlserver:"))
				{
					Map<String, String> optionsMap = StringUtil.parseCommaStrToMap(entry._jdbcUrlOptions);
					
					if ( ! optionsMap.containsKey("encrypt") )
					{
						optionsMap.put("encrypt", "true");
						_logger.info("Adding Connection Property 'encrypt=true' when loading Connection Profile '" + connProfile.getName() + "'.");
					}

					if ( ! optionsMap.containsKey("trustServerCertificate") )
					{
						optionsMap.put("trustServerCertificate", "true");
						_logger.info("Adding Connection Property 'trustServerCertificate=true' when loading Connection Profile '" + connProfile.getName() + "'.");
					}

					jdbcUrlOptions = StringUtil.toCommaStr(optionsMap, "=", ", ");
				}

				
				setConnectionProfileTypeName(connProfile, entry._profileTypeName);
				_jdbcDriver_cbx       .setSelectedItem(entry._jdbcDriver);
				setJdbcUrlTemplates(entry._jdbcDriver);
				_jdbcUrl_cbx          .setSelectedItem(entry._jdbcUrl);
				_jdbcUsername_txt     .setText(        entry._jdbcUsername);
				_jdbcPassword_txt     .setText(        entry._jdbcPassword);
				_jdbcSqlInit_txt      .setText(        entry._jdbcSqlInit);
//				_jdbcUrlOptions_txt   .setText(        entry._jdbcUrlOptions);
//				setJdbcUrlOptions(entry._jdbcUrlOptions);
				setJdbcUrlOptions(jdbcUrlOptions);
				_jdbcSavePassword_chk .setSelected(    entry._jdbcSavePassword);
				_jdbcSshTunnel_chk    .setSelected(    entry._jdbcShhTunnelUse);
				_jdbcSshTunnelInfo    =                entry._jdbcShhTunnelInfo;

				boolean isDbxTuneParamsValid = entry._isDbxTuneParamsValid;
				if (isDbxTuneParamsValid)
				{
					loadDbxTuneParams(entry._dbxtuneParams);
				}

				// Set the ICON
				ImageIcon icon = ConnectionProfileManager.getIcon32(connProfile.getSrvType());
				if (icon != null)
					_jdbcIcon.setIcon(icon);
			}
			else if (connProfile.isType(ConnectionProfile.Type.OFFLINE))
			{
//				SwingUtils.showErrorMessage("NOT IMPLEMETED", "Connection Profile LOAD OFFLINE is not yet implemented", null);
				setSelectedTab(OFFLINE_CONN);
				_offlineProfile_cbx.setSelectedItem(connProfile.getName());

				ConnectionProfile.OfflineEntry entry = connProfile.getOfflineEntry();
				
				setConnectionProfileTypeName(connProfile, entry._profileTypeName);
				_offlineJdbcDriver_cbx      .setSelectedItem(entry._jdbcDriver);
				_offlineJdbcUrl_cbx         .setSelectedItem(entry._jdbcUrl);
				_offlineJdbcUsername_txt    .setText(        entry._jdbcUsername);
				_offlineJdbcPassword_txt    .setText(        entry._jdbcPassword);
				_offlineJdbcSavePassword_chk.setSelected(    entry._jdbcSavePassword);

				_offlineCheckForNewSessions_chk  .setSelected(entry._checkForNewSessions);
				_offlineH2Option_startH2NwSrv_chk.setSelected(entry._H2Option_startH2NwSrv);
			}
			else
			{
				SwingUtils.showErrorMessage("Unknown Connection Profile Type", "Unknown Connection Profile Type of '"+connProfile.getType()+"'.", null);
			}
		}
		finally
		{
			// RESET: indicator to method actionPerformed() that actions should be disabled during load 
			_inLoadProfile = false;
		}

		action_nwPasswdEncryption();
		
		// ALWAYS: do stuff for URL
		setUrlText();

		// ALWAYS: update tunnel description
		updateSshTunnelDescription();
		
		validateContents();
	}

	/**
	 * Connect using a connection profile
	 * 
	 * @param entry the ConnectionProfile entry
	 */
	public void connect(ConnectionProfile entry)
	{
		if      (entry.isType(ConnectionProfile.Type.TDS))     { action_connect(TDS_CONN,     entry, null); }
		else if (entry.isType(ConnectionProfile.Type.JDBC))    { action_connect(JDBC_CONN,    entry, null); }
		else if (entry.isType(ConnectionProfile.Type.OFFLINE)) { action_connect(OFFLINE_CONN, entry, null); }
		else
		{
			SwingUtils.showErrorMessage("Unknown Connection Profile Type", "Unknown Connection Profile Type of '"+entry.getType()+"'.", null);
		}
	}

	
	
	
//	public void loadOfflineConnectionProfile(String name)
//	{
//	}
//
//	public void saveJdbcConnectionProfile(String name)
//	{
//		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
//		if (conf == null)
//			return;
//	}
//	public void loadJdbcConnectionProfile(String name)
//	{
//		// OR use ConnectionProfile.JdbcEntry
//		// depending on how we want to store the ConnectionProfile entries (in ConnectionProfileManager object (as xml) or in the Configuration as properties)
//		Configuration conf = Configuration.getCombinedConfiguration();
//
////		String jdbcDriver             = conf.getProperty("xxx.xxx.xxx");
////		String jdbcUrl                = conf.getProperty("xxx.xxx.xxx");
////		String jdbcUsername           = conf.getProperty("xxx.xxx.xxx");
////		String jdbcPassword           = conf.getProperty("xxx.xxx.xxx");
////		String jdbcSqlInit            = conf.getProperty("xxx.xxx.xxx");
////		String jdbcUrlOptions         = conf.getProperty("xxx.xxx.xxx");
//////		String jdbcShhTunnelInfo      = // NOT YET IMPLEMENTED
////
////		_jdbcDriver_cbx    .setSelectedItem(jdbcDriver);
////		_jdbcUrl_cbx       .setSelectedItem(jdbcUrl);
////		_jdbcUsername_txt  .setText(jdbcUsername);
////		_jdbcPassword_txt  .setText(jdbcPassword);
////		_jdbcSqlInit_txt   .setText(jdbcSqlInit);
////		_jdbcUrlOptions_txt.setText(jdbcUrlOptions);
//	}

	private void updateSshTunnelDescription()
	{
		// set the ASE SSH description to visible or not + resize dialog if it's the checkbox
		_aseSshTunnelDesc_lbl.setVisible(_aseSshTunnel_chk.isSelected());
		if (_aseSshTunnel_chk.isSelected())
		{
			boolean generateLocalPort = true;
			int    localPort    = 7487;
			String destHost     = "asehostname";
			int    destPort     = 5000;
			String sshHost      = "sshHost";
			int    sshPort      = 22;
			String sshUser      = "sshUser";
			String sshPass      = "*secret*";
			String sshKeyFile   = "";
			String sshInitOsCmd = "";
			
			if (_aseSshTunnelInfo != null)
			{
				generateLocalPort = _aseSshTunnelInfo.isLocalPortGenerated();
				localPort    = _aseSshTunnelInfo.getLocalPort();
				destHost     = _aseSshTunnelInfo.getDestHost();
				destPort     = _aseSshTunnelInfo.getDestPort();
				sshHost      = _aseSshTunnelInfo.getSshHost();
				sshPort      = _aseSshTunnelInfo.getSshPort();
				sshUser      = _aseSshTunnelInfo.getSshUsername();
				sshPass      = _aseSshTunnelInfo.getSshPassword();
				sshKeyFile   = _aseSshTunnelInfo.getSshKeyFile();
				sshInitOsCmd = _aseSshTunnelInfo.getSshInitOsCmd();
			}
			
			String sshKeyFileDesc = "";
			if (StringUtil.hasValue(sshKeyFile))
				sshKeyFileDesc = ", <br>SSH Key File '<b>"+sshKeyFile+"</b>'";

			String initOsCmdDesc = "";
			if (StringUtil.hasValue(sshInitOsCmd))
				initOsCmdDesc = ", <br>Init OS Cmd '<b>"+sshInitOsCmd+"</b>'";

			_aseSshTunnelDesc_lbl.setText(
				"<html>" +
					"Local Port  '<b>" + (generateLocalPort ? "*generated*" : localPort) + "</b>', " +
					"Dest Host   '<b>" + destHost + ":" + destPort  + "</b>', <br>" +
					"SSH Host    '<b>" + sshHost  + ":" + sshPort   + "</b>', " +
					"SSH User    '<b>" + sshUser   + "</b>'"+sshKeyFileDesc+initOsCmdDesc+". " +
					(_logger.isDebugEnabled() ? "SSH Passwd '<b>" + sshPass + "</b>' " : "") +
				"</html>");
		}

		// JDBC
		_jdbcSshTunnelDesc_lbl.setVisible(_jdbcSshTunnel_chk.isSelected());
		if (_jdbcSshTunnel_chk.isSelected())
		{
			boolean generateLocalPort = true;
			int    localPort    = 7487;
			String destHost     = "jdbchostname";
			int    destPort     = 5000;
			String sshHost      = "sshHost";
			int    sshPort      = 22;
			String sshUser      = "sshUser";
			String sshPass      = "*secret*";
			String sshKeyFile   = "";
			String sshInitOsCmd = "";
			
			if (_jdbcSshTunnelInfo != null)
			{
				generateLocalPort = _jdbcSshTunnelInfo.isLocalPortGenerated();
				localPort    = _jdbcSshTunnelInfo.getLocalPort();
				destHost     = _jdbcSshTunnelInfo.getDestHost();
				destPort     = _jdbcSshTunnelInfo.getDestPort();
				sshHost      = _jdbcSshTunnelInfo.getSshHost();
				sshPort      = _jdbcSshTunnelInfo.getSshPort();
				sshUser      = _jdbcSshTunnelInfo.getSshUsername();
				sshPass      = _jdbcSshTunnelInfo.getSshPassword();
				sshKeyFile   = _jdbcSshTunnelInfo.getSshKeyFile();
				sshInitOsCmd = _jdbcSshTunnelInfo.getSshInitOsCmd();
			}
			
			String sshKeyFileDesc = "";
			if (StringUtil.hasValue(sshKeyFile))
				sshKeyFileDesc = ", <br>SSH Key File '<b>"+sshKeyFile+"</b>'";

			String initOsCmdDesc = "";
			if (StringUtil.hasValue(sshInitOsCmd))
				initOsCmdDesc = ", <br>Init OS Cmd '<b>"+sshInitOsCmd+"</b>'";

			_jdbcSshTunnelDesc_lbl.setText(
				"<html>" +
					"Local Port  '<b>" + (generateLocalPort ? "*generated*" : localPort) + "</b>', " +
					"Dest Host   '<b>" + destHost + ":" + destPort  + "</b>', <br>" +
					"SSH Host    '<b>" + sshHost  + ":" + sshPort   + "</b>', " +
					"SSH User    '<b>" + sshUser   + "</b>'"+sshKeyFileDesc+initOsCmdDesc+". " +
					(_logger.isDebugEnabled() ? "SSH Passwd '<b>" + sshPass + "</b>' " : "") +
				"</html>");
		}
	}

	private String aseDeferredConnectChkAction(ConnectionProfile connProfile)
	{
		boolean enable = _aseDeferredConnect_chk.isSelected();
		String  hh     = "" + _aseDeferredConnectHour_sp  .getValue();
		String  mm     = "" + _aseDeferredConnectMinute_sp.getValue();
		
		if (connProfile != null)
		{
//			ConnectionProfile.TdsEntry entry = connProfile.getTdsEntry();
			ConnectionProfile.DbxTuneParams entry = connProfile.getDbxTuneParams(); 

			enable = entry._dbxtuneOptConnectLater;
			hh     = entry._dbxtuneOptConnectLaterHour;
			mm     = entry._dbxtuneOptConnectLaterMinute;
		}

		_aseDeferredConnectHour_lbl  .setEnabled(enable);
		_aseDeferredConnectHour_sp   .setEnabled(enable);
		_aseDeferredConnectMinute_lbl.setEnabled(enable);
		_aseDeferredConnectMinute_sp .setEnabled(enable);
		_aseDeferredConnectTime_lbl  .setEnabled(enable);

		String hhmm = null;
		if ( enable )
		{
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

	private String aseDeferredDisConnectChkAction(ConnectionProfile connProfile)
	{
		boolean enable = _aseDeferredDisConnect_chk.isSelected();
		String  hh     = "" + _aseDeferredDisConnectHour_sp  .getValue();
		String  mm     = "" + _aseDeferredDisConnectMinute_sp.getValue();

		if (connProfile != null)
		{
//			ConnectionProfile.TdsEntry entry = connProfile.getTdsEntry();
			ConnectionProfile.DbxTuneParams entry = connProfile.getDbxTuneParams(); 

			enable = entry._dbxtuneOptDissConnectLater;
			hh     = entry._dbxtuneOptDissConnectLaterHour;
			mm     = entry._dbxtuneOptDissConnectLaterMinute;
		}

		_aseDeferredDisConnectHour_lbl  .setEnabled(enable);
		_aseDeferredDisConnectHour_sp   .setEnabled(enable);
		_aseDeferredDisConnectMinute_lbl.setEnabled(enable);
		_aseDeferredDisConnectMinute_sp .setEnabled(enable);
		_aseDeferredDisConnectTime_lbl  .setEnabled(enable);

		String hhmm = null;
		if ( enable )
		{
			hhmm = hh + ":" + mm;
			Date stopTime;
			try 
			{ 
				Date startTime = PersistWriterBase.getRecordingStartTime(aseDeferredConnectChkAction(connProfile));
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

			// If it's not a servername from the sql.ini
			// Check if we got a host:port combination in the properties file, if so lets load it
			String hostPortStr = AseConnectionFactory.toHostPortStr(hosts, ports);

			// If resolve is enabled, then load "known" profile
			if (_aseHostPortResolve_chk.isSelected())
			{
				// if username has been stored, restore "all" other saved properties
				String user = Configuration.getCombinedConfiguration().getProperty("conn.username."+hostPortStr);
				if (StringUtil.hasValue(user)) 
					loadPropsForServer(hostPortStr);

				setProfileBox(hostPortStr);
			}
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
			aseDeferredConnectChkAction(null);
			aseDeferredDisConnectChkAction(null);
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
//			CountersModel cm     = GetCounters.getInstance().getCmByName(cmName);
			CountersModel cm     = CounterController.getInstance().getCmByName(cmName);
			if (cm == null)
				continue;

			tm.setValueAt(Integer.valueOf(cm.getQueryTimeout()),              r, PCS_TAB_POS_TIMEOUT);
			tm.setValueAt(Integer.valueOf(cm.getPostponeTime()),              r, PCS_TAB_POS_POSTPONE);
//			tm.setValueAt(Boolean.valueOf(cm.isPersistCountersEnabled() || cm.isBackgroundDataPollingEnabled()), r, PCS_TAB_POS_STORE_PCS);
			tm.setValueAt(Boolean.valueOf(cm.isPersistCountersEnabled()),     r, PCS_TAB_POS_STORE_PCS);
			tm.setValueAt(Boolean.valueOf(cm.isPersistCountersAbsEnabled()),  r, PCS_TAB_POS_STORE_ABS);
			tm.setValueAt(Boolean.valueOf(cm.isPersistCountersDiffEnabled()), r, PCS_TAB_POS_STORE_DIFF);
			tm.setValueAt(Boolean.valueOf(cm.isPersistCountersRateEnabled()), r, PCS_TAB_POS_STORE_RATE);
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
		{
			conf.setLayoutProperty(PROPKEY_CONN_SPLITPANE_DIVIDER_LOCATION,  _connSplitPane.getDividerLocation());
//			System.out.println("saveProps(): "+PROPKEY_CONN_SPLITPANE_DIVIDER_LOCATION+" = "+_connSplitPane.getDividerLocation());
		}
		conf.setProperty(PROPKEY_CONN_PROFILE_PANEL_VISIBLE,    _connProfileVisible_chk.isSelected());
		conf.setProperty(PROPKEY_CONN_TABED_PANEL_VISIBLE,      _connTabbedVisible_chk .isSelected());
		conf.setProperty("conn.profile.last.used",              _usedConnectionProfileName == null ? "" : _usedConnectionProfileName);


		String hostPort = AseConnectionFactory.toHostPortStr(_aseHost_txt.getText(), _asePort_txt.getText());

		conf.setProperty("conn.interfaces",                     _aseIfile_txt.getText());
		conf.setProperty("conn.serverName",                     StringUtil.getSelectedItemString(_aseServer_cbx) );

		conf.setProperty("conn.hostname",                       _aseHost_txt.getText());
		conf.setProperty("conn.port",                           _asePort_txt.getText());

		conf.setProperty("conn.username",                       _aseUsername_txt.getText());
		conf.setProperty("conn.username."+hostPort,             _aseUsername_txt.getText());
		if (_aseOptionSavePwd_chk.isSelected())
		{
			conf.setProperty("conn.password",           _asePassword_txt.getText(), true);
			conf.setProperty("conn.password."+hostPort, _asePassword_txt.getText(), true);
		}
		else
		{
			conf.remove("conn.password");
			conf.remove("conn.password."+hostPort);
		}

		conf.setProperty("conn.login.timeout",                  _aseLoginTimeout_txt.getText() );
		conf.setProperty("conn.login.timeout."+hostPort,        _aseLoginTimeout_txt.getText() );

		conf.setProperty(PROPKEY_CONN_SSH_TUNNEL,               _aseSshTunnel_chk.isSelected() );
		conf.setProperty(PROPKEY_CONN_SSH_TUNNEL+"."+hostPort,  _aseSshTunnel_chk.isSelected() );

		conf.setProperty("conn.login.client.charset",           StringUtil.getSelectedItemString(_aseClientCharset_cbx) );
		conf.setProperty("conn.login.client.charset."+hostPort, StringUtil.getSelectedItemString(_aseClientCharset_cbx) );

		conf.setProperty("conn.login.sql.init",                 _aseSqlInit_txt.getText() );
		conf.setProperty("conn.login.sql.init."+hostPort,       _aseSqlInit_txt.getText() );

		conf.setProperty("conn.url.raw",                        _aseConnUrl_txt.getText() );
		conf.setProperty("conn.url.raw."+hostPort,              _aseConnUrl_txt.getText() );
		conf.setProperty("conn.url.raw.checkbox",               _aseConnUrl_chk.isSelected() );
		conf.setProperty("conn.url.raw.checkbox."+hostPort,     _aseConnUrl_chk.isSelected() );
		conf.setProperty("conn.url.options",                    _aseOptions_txt.getText() );
		conf.setProperty("conn.url.options."+hostPort,          _aseOptions_txt.getText() );

		conf.setProperty("conn.savePassword",                             _aseOptionSavePwd_chk.isSelected() );
		conf.setProperty("conn.savePassword."+hostPort,                   _aseOptionSavePwd_chk.isSelected() );
		conf.setProperty("conn.passwordEncryptionOverNetwork",            _aseOptionPwdEncryption_chk.isSelected() );
		conf.setProperty("conn.passwordEncryptionOverNetwork."+hostPort,  _aseOptionPwdEncryption_chk.isSelected() );
		conf.setProperty(PROPKEY_CONNECT_ON_STARTUP,                      _aseOptionConnOnStart_chk.isSelected() );
		conf.setProperty(PROPKEY_RECONNECT_ON_FAILURE,                    _aseOptionReConnOnFailure_chk.isSelected());

		conf.setProperty("conn.counter.use.template",                _aseOptionUseTemplate_chk.isSelected() );
		conf.setProperty("conn.counter.use.template."+hostPort,      _aseOptionUseTemplate_chk.isSelected() );
		conf.setProperty("conn.counter.use.template.name",           StringUtil.getSelectedItemString(_aseOptionUseTemplate_cbx) );
		conf.setProperty("conn.counter.use.template.name."+hostPort, StringUtil.getSelectedItemString(_aseOptionUseTemplate_cbx) );
		conf.setProperty("conn.persistCounterStorage",               _aseOptionStore_chk.isSelected() );
		conf.setProperty("conn.persistCounterStorage."+hostPort,     _aseOptionStore_chk.isSelected() );
		conf.setProperty("conn.hostMonitoring",                      _aseHostMonitor_chk.isSelected() );
		conf.setProperty("conn.hostMonitoring."+hostPort,            _aseHostMonitor_chk.isSelected() );
		conf.setProperty("conn.deferred.connect",                    _aseDeferredConnect_chk.isSelected() );
		conf.setProperty("conn.deferred.connect.hour",               _aseDeferredConnectHour_sp  .getValue().toString() );
		conf.setProperty("conn.deferred.connect.minute",             _aseDeferredConnectMinute_sp.getValue().toString() );
		conf.setProperty("conn.deferred.disconnect",                 _aseDeferredDisConnect_chk.isSelected() );
		conf.setProperty("conn.deferred.disconnect.hour",            _aseDeferredDisConnectHour_sp  .getValue().toString() );
		conf.setProperty("conn.deferred.disconnect.minute",          _aseDeferredDisConnectMinute_sp.getValue().toString() );

		//----------------------------------
		// TAB: OS Host
		//----------------------------------
		if ( _aseHostMonitor_chk.isSelected() )
		{
			conf.setProperty("ssh.conn.hostname."+hostPort,   _hostmonHost_txt    .getText() );
			conf.setProperty("ssh.conn.port."+hostPort,       _hostmonPort_txt    .getText() );
			conf.setProperty("ssh.conn.username."+hostPort,   _hostmonUsername_txt.getText() );

			if (_hostmonOptionSavePwd_chk.isSelected())
				conf.setProperty("ssh.conn.password."+hostPort, _hostmonPassword_txt.getText(), true);
			else
				conf.remove("ssh.conn.password."+hostPort);

			conf.setProperty("ssh.conn.savePassword", _hostmonOptionSavePwd_chk.isSelected() );

			conf.setProperty("ssh.conn.hostmon.localOsCmd."+hostPort,        _hostMonLocalOsCmd_chk       .isSelected() );
			conf.setProperty("ssh.conn.hostmon.localOsCmdWrapper."+hostPort, _hostmonLocalOsCmdWrapper_txt.getText() );
			
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
			conf.setProperty        ("pcs.write.ddl.doDdlLookup",                              _pcsDdl_doDdlLookupAndStore_chk            .isSelected() );
			conf.setProperty        ("pcs.write.ddl.enabledForDatabaseObjects",                _pcsDdl_enabledForDatabaseObjects_chk      .isSelected() );
			conf.setProperty        ("pcs.write.ddl.enabledForStatementCache",                 _pcsDdl_enabledForStatementCache_chk       .isSelected() );
			conf.setProperty        ("pcs.write.ddl.addDependantObjectsToDdlInQueue",          _pcsDdl_addDependantObjectsToDdlInQueue_chk.isSelected() );
			conf.setProperty        ("pcs.write.ddl.afterDdlLookupSleepTimeInMs",              _pcsDdl_afterDdlLookupSleepTimeInMs_txt    .getText() );

			// SQL Capture Lookup & Store
			conf.setProperty        ("pcs.write.sqlCapture.doSqlCaptureAndStore",              _pcsCapSql_doSqlCaptureAndStore_chk         .isSelected() );
			conf.setProperty        ("pcs.write.sqlCapture.doSqlText",                         _pcsCapSql_doSqlText_chk                    .isSelected() );
			conf.setProperty        ("pcs.write.sqlCapture.doStatementInfo",                   _pcsCapSql_doStatementInfo_chk              .isSelected() );
			conf.setProperty        ("pcs.write.sqlCapture.doPlanText",                        _pcsCapSql_doPlanText_chk                   .isSelected() );
			conf.setProperty        ("pcs.write.sqlCapture.sleepTimeInMs",                     _pcsCapSql_sleepTimeInMs_txt                .getText() );
			conf.setProperty        ("pcs.write.sqlCapture.saveStatement_gt_execTime",         _pcsCapSql_saveStatement_execTime_txt       .getText() );
			conf.setProperty        ("pcs.write.sqlCapture.saveStatement_gt_logicalReads",     _pcsCapSql_saveStatement_logicalRead_txt    .getText() );
			conf.setProperty        ("pcs.write.sqlCapture.saveStatement_gt_physicalReads",    _pcsCapSql_saveStatement_physicalRead_txt   .getText() );
			conf.setProperty        ("pcs.write.sqlCapture.sendDdlForLookup",                  _pcsCapSql_sendDdlForLookup_chk             .isSelected() );
			conf.setProperty        ("pcs.write.sqlCapture.sendDdlForLookup_gt_execTime",      _pcsCapSql_sendDdlForLookup_execTime_txt    .getText() );
			conf.setProperty        ("pcs.write.sqlCapture.sendDdlForLookup_gt_logicalReads",  _pcsCapSql_sendDdlForLookup_logicalRead_txt .getText() );
			conf.setProperty        ("pcs.write.sqlCapture.sendDdlForLookup_gt_physicalReads", _pcsCapSql_sendDdlForLookup_physicalRead_txt.getText() );

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

//			conf.setProperty(PROPKEY_CONN_JDBC_SSH_TUNNEL,               _jdbcSshTunnel_chk.isSelected() );
//			conf.setProperty(PROPKEY_CONN_JDBC_SSH_TUNNEL+"."+hostPort,  _jdbcSshTunnel_chk.isSelected() );

			conf.setProperty(PROPKEY_CONN_SQLSERVER_WIN_AUTH,             _jdbcSqlServerUseWindowsAuthentication_chk.isSelected() );
			conf.setProperty(PROPKEY_CONN_SQLSERVER_WIN_AUTH+"."+urlStr,  _jdbcSqlServerUseWindowsAuthentication_chk.isSelected() );

			conf.setProperty(PROPKEY_CONN_SQLSERVER_ENCRYPT,             _jdbcSqlServerUseEncrypt_chk.isSelected() );
			conf.setProperty(PROPKEY_CONN_SQLSERVER_ENCRYPT+"."+urlStr,  _jdbcSqlServerUseEncrypt_chk.isSelected() );

			conf.setProperty(PROPKEY_CONN_SQLSERVER_TRUST_CERT,             _jdbcSqlServerUseTrustServerCertificate_chk.isSelected() );
			conf.setProperty(PROPKEY_CONN_SQLSERVER_TRUST_CERT+"."+urlStr,  _jdbcSqlServerUseTrustServerCertificate_chk.isSelected() );
		}

		//------------------
		// WINDOW
		//------------------
		conf.setLayoutProperty("conn.dialog.window.width",  this.getSize().width);
		conf.setLayoutProperty("conn.dialog.window.height", this.getSize().height);
		conf.setLayoutProperty("conn.dialog.window.pos.x",  this.getLocationOnScreen().x);
		conf.setLayoutProperty("conn.dialog.window.pos.y",  this.getLocationOnScreen().y);

		// last TAB and PROFILE
		conf.setProperty("conn.dialog.last.tab.name",              _tab.getSelectedTitle(false));
		conf.setProperty("conn.dialog.last.profile.name.tds",      StringUtil.getSelectedItemString(_aseProfile_cbx));
		conf.setProperty("conn.dialog.last.profile.name.offline",  StringUtil.getSelectedItemString(_offlineProfile_cbx));
		conf.setProperty("conn.dialog.last.profile.name.jdbc",     StringUtil.getSelectedItemString(_jdbcProfile_cbx));

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

		_lastKnownConnSplitPaneDividerLocation = conf.getLayoutProperty(PROPKEY_CONN_SPLITPANE_DIVIDER_LOCATION, DEFAULT_CONN_SPLITPANE_DIVIDER_LOCATION);
		if (_lastKnownConnSplitPaneDividerLocation < 10)
			_lastKnownConnSplitPaneDividerLocation = DEFAULT_CONN_SPLITPANE_DIVIDER_LOCATION;
		_connSplitPane.setDividerLocation(_lastKnownConnSplitPaneDividerLocation);
//System.out.println("loadProps(): "+PROPKEY_CONN_SPLITPANE_DIVIDER_LOCATION+" = "+_lastKnownConnSplitPaneDividerLocation);


		if ( ! conf.getBooleanProperty(PROPKEY_CONN_PROFILE_PANEL_VISIBLE, DEFAULT_CONN_PROFILE_PANEL_VISIBLE) )
			_connProfileVisible_chk.doClick();

		if ( ! conf.getBooleanProperty(PROPKEY_CONN_TABED_PANEL_VISIBLE, DEFAULT_CONN_TABED_PANEL_VISIBLE) )
			_connTabbedVisible_chk.doClick();

		_usedConnectionProfileName = conf.getProperty("conn.profile.last.used", "");
		if (StringUtil.hasValue(_usedConnectionProfileName))
			setConnectionProfileSelectedName(_usedConnectionProfileName);
			
		
		String str = null;
		boolean bol = false;

		str = conf.getProperty("conn.interfaces");
		if (str != null)
			loadNewInterfaces(str);

		str = conf.getProperty("conn.hostname");
		if (str != null)
			_aseHost_txt.setText(str);

		str = conf.getProperty("conn.port");
		if (str != null)
			_asePort_txt.setText(str);

		str = conf.getProperty("conn.serverName");
		if (str != null)
			_aseServer_cbx.setSelectedItem(str);

		// If host:port is part of the interfaces file, then use that name
		str = AseConnectionFactory.toHostPortStr(_aseHost_txt.getText(), _asePort_txt.getText());
		if (str != null)
		{
			str = AseConnectionFactory.getIServerName(str);
			if (str != null)
				_aseServer_cbx.setSelectedItem(str);
		}
		

		str = conf.getProperty("conn.login.timeout");
		if (str == null)
			str = conf.getProperty(AseConnectionFactory.PROPKEY_LOGINTIMEOUT, AseConnectionFactory.DEFAULT_LOGINTIMEOUT+"");
		_aseLoginTimeout_txt.setText(str);

		bol = conf.getBooleanProperty(PROPKEY_CONN_SSH_TUNNEL, DEFAULT_CONN_SSH_TUNNEL);
		_aseSshTunnel_chk.setSelected(bol);

		str = conf.getProperty("conn.login.client.charset");
		if (str != null)
			_aseClientCharset_cbx.setSelectedItem(str);

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

		if (_aseConnUrl_chk.isSelected() && StringUtil.hasValue(_aseConnUrl_txt.getText()))
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
			catch (RuntimeException re)
			{
			}
		}

		String hostPortStr = AseConnectionFactory.toHostPortStr(_aseHost_txt.getText(), _asePort_txt.getText());
		loadPropsForServer(hostPortStr);

		
		bol = conf.getBooleanProperty("conn.savePassword", true);
		_aseOptionSavePwd_chk.setSelected(bol);
		
		bol = conf.getBooleanProperty("conn.passwordEncryptionOverNetwork", true);
		_aseOptionPwdEncryption_chk.setSelected(bol);

		
		bol = conf.getBooleanProperty(PROPKEY_CONNECT_ON_STARTUP, false);
		_aseOptionConnOnStart_chk.setSelected(bol); 

		bol = conf.getBooleanProperty(PROPKEY_RECONNECT_ON_FAILURE, true);
		_aseOptionReConnOnFailure_chk.setSelected(bol); 

		bol = conf.getBooleanProperty("conn.counter.use.template", false);
		_aseOptionUseTemplate_chk.setSelected(bol);

		_aseOptionUseTemplate_cbx.setSelectedItem(conf.getProperty("conn.counter.use.template.name", NO_TEMPLATE_IS_SELECTED));

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
		_pcsDdl_doDdlLookupAndStore_chk             .setSelected(conf.getBooleanProperty("pcs.write.ddl.doDdlLookup",                     PersistentCounterHandler.DEFAULT_ddl_doDdlLookupAndStore));
		_pcsDdl_enabledForDatabaseObjects_chk       .setSelected(conf.getBooleanProperty("pcs.write.ddl.enabledForDatabaseObjects",       PersistentCounterHandler.DEFAULT_ddl_enabledForDatabaseObjects));
		_pcsDdl_enabledForStatementCache_chk        .setSelected(conf.getBooleanProperty("pcs.write.ddl.enabledForStatementCache",        PersistentCounterHandler.DEFAULT_ddl_enabledForStatementCache));
		_pcsDdl_addDependantObjectsToDdlInQueue_chk .setSelected(conf.getBooleanProperty("pcs.write.ddl.addDependantObjectsToDdlInQueue", PersistentCounterHandler.DEFAULT_ddl_addDependantObjectsToDdlInQueue));
		_pcsDdl_afterDdlLookupSleepTimeInMs_txt     .setText    (conf.getProperty       ("pcs.write.ddl.afterDdlLookupSleepTimeInMs",     PersistentCounterHandler.DEFAULT_ddl_afterDdlLookupSleepTimeInMs + ""));
		
		// SQL Capture Lookup & Store
		_pcsCapSql_doSqlCaptureAndStore_chk         .setSelected(conf.getBooleanProperty("pcs.write.sqlCapture.doSqlCaptureAndStore",              PersistentCounterHandler.DEFAULT_sqlCap_doSqlCaptureAndStore));
		_pcsCapSql_doSqlText_chk                    .setSelected(conf.getBooleanProperty("pcs.write.sqlCapture.doSqlText",                         PersistentCounterHandler.DEFAULT_sqlCap_doSqlText                        ));
		_pcsCapSql_doStatementInfo_chk              .setSelected(conf.getBooleanProperty("pcs.write.sqlCapture.doStatementInfo",                   PersistentCounterHandler.DEFAULT_sqlCap_doStatementInfo                  ));
		_pcsCapSql_doPlanText_chk                   .setSelected(conf.getBooleanProperty("pcs.write.sqlCapture.doPlanText",                        PersistentCounterHandler.DEFAULT_sqlCap_doPlanText                       ));
		_pcsCapSql_sleepTimeInMs_txt                .setText    (conf.getProperty       ("pcs.write.sqlCapture.sleepTimeInMs",                     PersistentCounterHandler.DEFAULT_sqlCap_sleepTimeInMs                     + ""));
		_pcsCapSql_saveStatement_execTime_txt       .setText    (conf.getProperty       ("pcs.write.sqlCapture.saveStatement_gt_execTime",         PersistentCounterHandler.DEFAULT_sqlCap_saveStatement_gt_execTime         + ""));
		_pcsCapSql_saveStatement_logicalRead_txt    .setText    (conf.getProperty       ("pcs.write.sqlCapture.saveStatement_gt_logicalReads",     PersistentCounterHandler.DEFAULT_sqlCap_saveStatement_gt_logicalReads     + ""));
		_pcsCapSql_saveStatement_physicalRead_txt   .setText    (conf.getProperty       ("pcs.write.sqlCapture.saveStatement_gt_physicalReads",    PersistentCounterHandler.DEFAULT_sqlCap_saveStatement_gt_physicalReads    + ""));
		_pcsCapSql_sendDdlForLookup_chk             .setSelected(conf.getBooleanProperty("pcs.write.sqlCapture.sendDdlForLookup",                  PersistentCounterHandler.DEFAULT_sqlCap_sendDdlForLookup                 ));
		_pcsCapSql_sendDdlForLookup_execTime_txt    .setText    (conf.getProperty       ("pcs.write.sqlCapture.sendDdlForLookup_gt_execTime",      PersistentCounterHandler.DEFAULT_sqlCap_sendDdlForLookup_gt_execTime      + ""));
		_pcsCapSql_sendDdlForLookup_logicalRead_txt .setText    (conf.getProperty       ("pcs.write.sqlCapture.sendDdlForLookup_gt_logicalReads",  PersistentCounterHandler.DEFAULT_sqlCap_sendDdlForLookup_gt_logicalReads  + ""));
		_pcsCapSql_sendDdlForLookup_physicalRead_txt.setText    (conf.getProperty       ("pcs.write.sqlCapture.sendDdlForLookup_gt_physicalReads", PersistentCounterHandler.DEFAULT_sqlCap_sendDdlForLookup_gt_physicalReads + ""));

		updateEnabledForPcsDdlLookupAndSqlCapture();
		
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

		bol = conf.getBooleanProperty(PROPKEY_CONN_SSH_TUNNEL, DEFAULT_CONN_SSH_TUNNEL);
		_jdbcSshTunnel_chk.setSelected(bol);

//		_jdbcSqlServerUseWindowsAuthentication_chk.setSelected(conf.getBooleanProperty(PROPKEY_CONN_SQLSERVER_WIN_AUTH, DEFAULT_CONN_SQLSERVER_WIN_AUTH)); 
		setSqlServerUseWindowsAuthenticationSelected (conf.getBooleanProperty(PROPKEY_CONN_SQLSERVER_WIN_AUTH  , DEFAULT_CONN_SQLSERVER_WIN_AUTH));
		setSqlServerUseEncryptSelected               (conf.getBooleanProperty(PROPKEY_CONN_SQLSERVER_ENCRYPT   , DEFAULT_CONN_SQLSERVER_ENCRYPT));
		setSqlServerUseTrustServerCertificateSelected(conf.getBooleanProperty(PROPKEY_CONN_SQLSERVER_TRUST_CERT, DEFAULT_CONN_SQLSERVER_TRUST_CERT));


		//----------------------------------
		// last PROFILE
		//----------------------------------
		_aseProfile_mod    .setSelectedItem( conf.getProperty("conn.dialog.last.profile.name.tds",     NO_PROFILE_IS_SELECTED) );
		_offlineProfile_mod.setSelectedItem( conf.getProperty("conn.dialog.last.profile.name.offline", NO_PROFILE_IS_SELECTED) );
		_jdbcProfile_mod   .setSelectedItem( conf.getProperty("conn.dialog.last.profile.name.jdbc",    NO_PROFILE_IS_SELECTED) );

		action_nwPasswdEncryption();
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
		int width  = conf.getLayoutProperty("conn.dialog.window.width",  -1);
		int height = conf.getLayoutProperty("conn.dialog.window.height", -1);
		int x      = conf.getLayoutProperty("conn.dialog.window.pos.x",  -1);
		int y      = conf.getLayoutProperty("conn.dialog.window.pos.y",  -1);
		if (width != -1 && height != -1)
			this.setSize(width, height);

		if (x != -1 && y != -1)
			if ( ! SwingUtils.isOutOfScreen(x, y, width, height) )
				this.setLocation(x, y);

		// Window size can not be "smaller" than the minimum size
		// If so "OK" button etc will be hidden.
		SwingUtils.setWindowMinSize(this);
		
		// Set active tab to "last one used"
		String lastTab = conf.getProperty("conn.dialog.last.tab.name");
		if (lastTab != null)
			_tab.setSelectedTitle(lastTab);
	}

	private void saveWindowProps()
	{
		if ( ! isVisible() )
			return;

		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null)
		{
			_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
			return;
		}
		
		//------------------
		// SplitPane Divider location
		//------------------
		if (_connProfileVisible_chk.isSelected() && _connTabbedVisible_chk .isSelected())
		{
			conf.setLayoutProperty(PROPKEY_CONN_SPLITPANE_DIVIDER_LOCATION,  _connSplitPane.getDividerLocation());
		}

		//------------------
		// WINDOW
		//------------------
		conf.setLayoutProperty("conn.dialog.window.width",  this.getSize().width);
		conf.setLayoutProperty("conn.dialog.window.height", this.getSize().height);
		conf.setLayoutProperty("conn.dialog.window.pos.x",  this.getLocationOnScreen().x);
		conf.setLayoutProperty("conn.dialog.window.pos.y",  this.getLocationOnScreen().y);

		conf.save();
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

	/**
	 * If any profile with the key is selected, keep that one
	 * Else load the first profile found in the key set
	 * If no profile was found with the key, set it to "not selected"
	 * 
	 * @param hostPortStr
	 */
	private void setProfileBox(String hostPortStr)
	{
		String currentEntry = StringUtil.getSelectedItemString(_aseProfile_cbx);

		List<ConnectionProfile> connProfiles = ConnectionProfileManager.getInstance().getProfileByKey(hostPortStr);
		if (connProfiles.size() > 0)
		{
			boolean isSelected = false;
			for (ConnectionProfile cp : connProfiles)
			{
				if (currentEntry.equals(cp.getName()) && cp.isType(ConnectionProfile.Type.TDS))
					isSelected = true;
			}
			if (!isSelected)
				_aseProfile_cbx.setSelectedItem(connProfiles.get(0).getName());
		}
		else
		{
			_aseProfile_cbx.setSelectedItem(NO_PROFILE_IS_SELECTED);
		}
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

		// Update the Profile Combobox
		setProfileBox(hostPortStr);
		
		//----------------------------------------
		// ASE stuff
		//----------------------------------------

		// First do "conn.username.hostName.portNum", if not found, go to "conn.username"
		str = conf.getProperty("conn.username."+hostPortStr);
		if (str != null)
		{
			_aseUsername_txt.setText(str);
		}
		else
		{
			str = conf.getProperty("conn.username");
			if (str != null)
				_aseUsername_txt.setText(str);
		}

		// First do "conn.password.hostName.portNum", if not found, go to "conn.password"
		str = getPasswordForServer(hostPortStr);
		if (str != null)
			_asePassword_txt.setText(str);


		// SavePassword and NetworkEncryptionOfPassword
		_aseOptionSavePwd_chk      .setSelected(conf.getBooleanProperty("conn.savePassword."+hostPortStr,                  conf.getBooleanProperty("conn.savePassword", true)));
		_aseOptionPwdEncryption_chk.setSelected(conf.getBooleanProperty("conn.passwordEncryptionOverNetwork."+hostPortStr, conf.getBooleanProperty("conn.passwordEncryptionOverNetwork", true)));
		action_nwPasswdEncryption();
		
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

		// Client CHARSET
		_aseClientCharset_cbx.setSelectedItem( conf.getProperty("conn.login.client.charset."+hostPortStr, ""));

		// SQL Init
		_aseSqlInit_txt.setText( conf.getProperty("conn.login.sql.init."+hostPortStr, ""));

		// Login timeout
		_aseLoginTimeout_txt.setText(conf.getProperty("conn.login.timeout."+hostPortStr, AseConnectionFactory.DEFAULT_LOGINTIMEOUT+""));

		// Use RAW URL string
		_aseConnUrl_chk.setSelected(conf.getBooleanProperty("conn.url.raw.checkbox."+hostPortStr, false));
		if (_aseConnUrl_chk.isSelected())
			_aseConnUrl_txt.setText(conf.getProperty("conn.url.raw."+hostPortStr, ""));

		// URL Options
		_aseOptions_txt.setText(conf.getProperty("conn.url.options."+hostPortStr, ""));


		// Counter Template
		_aseOptionUseTemplate_chk.setSelected(conf.getBooleanProperty("conn.counter.use.template."+hostPortStr, false));
		_aseOptionUseTemplate_cbx.setSelectedItem(conf.getProperty(   "conn.counter.use.template.name."+hostPortStr, NO_TEMPLATE_IS_SELECTED));

		// PCS 
		_aseOptionStore_chk.setSelected(conf.getBooleanProperty("conn.persistCounterStorage."+hostPortStr, false));
		_aseHostMonitor_chk.setSelected(conf.getBooleanProperty("conn.hostMonitoring."+hostPortStr, false));



		//----------------------------------------
		// OS HOST stuff
		//----------------------------------------

		// USERNAME
		str = conf.getProperty("ssh.conn.username."+hostPortStr, "");
		_hostmonUsername_txt.setText(str);

		// PASSWORD
		str = conf.getProperty("ssh.conn.password."+hostPortStr, "");
		_hostmonPassword_txt.setText(str);

		// HOSTNAME
		str = conf.getProperty("ssh.conn.hostname."+hostPortStr, "");
		_hostmonHost_txt.setText(str);

		// PORT
		str = conf.getProperty("ssh.conn.port."+hostPortStr, "22");
		_hostmonPort_txt.setText(str);

		
		// Hostmon Local OS
		boolean bol = conf.getBooleanProperty("ssh.conn.hostmon.localOsCmd."+hostPortStr, false);
		_hostMonLocalOsCmd_chk.setSelected(bol);

		// Hostmon Local OS Wrapper
		str = conf.getProperty("ssh.conn.hostmon.localOsCmdWrapper."+hostPortStr, "");
		_hostmonLocalOsCmdWrapper_txt.setText(str);
		
		//----------------------------------------
		// JDBC ??? should this be in here ???
		//----------------------------------------
//		_jdbcSshTunnel_chk.setSelected( conf.getBooleanProperty(PROPKEY_CONN_JDBC_SSH_TUNNEL+"."+hostPortStr, DEFAULT_CONN_JDBC_SSH_TUNNEL) );
//		if (_jdbcSshTunnel_chk.isSelected())
//			_jdbcSshTunnelInfo = SshTunnelDialog.getSshTunnelInfo(hostPortStr);
//		updateSshTunnelDescription();
	}

	/*---------------------------------------------------
	** END: implementing saveProps & loadProps
	**---------------------------------------------------
	*/

	public Configuration getAseOptions()
	{
		Configuration conf = new Configuration();

		conf.setProperty(PROPKEY_CONNECT_ON_STARTUP,   _aseOptionConnOnStart_chk.isSelected());
		conf.setProperty(PROPKEY_RECONNECT_ON_FAILURE, _aseOptionReConnOnFailure_chk.isSelected());
		
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
	**---- ACTION CLASSES FOR CONNECTION PROFILE -------- 
	**---------------------------------------------------
	**---------------------------------------------------
	**---------------------------------------------------
	*/
	private class ConnProfileLoadAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "Load";
//		private static final String ICON = "images/conn_profile_tree_load.png";
		private static final String ICON = "";

		public ConnProfileLoadAction()
		{
			super(NAME, SwingUtils.readImageIcon(Version.class, ICON));

			putValue(SHORT_DESCRIPTION,	
					"<html>"
					+ "Load values/properties from the Connection Profile into the Connection Dialog<br>"
					+ "Then you can view/change attributes and press <b>OK</b> to make the connection."
					+ "</html>");
			//putValue(MNEMONIC_KEY, mnemonic);
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) _connProfileTree.getLastSelectedPathComponent();
			Object o = node.getUserObject();

			if (o instanceof ConnectionProfile)
			{
				ConnectionProfile entry = (ConnectionProfile)o;
				load(entry);
			}
			else
			{
				String htmlMsg = 
						"<html>"
						+ "<h3>Unknown node type</h3>"
						+ "<b>Classname: </b>" + o.getClass().getName() + "<br>"
						+ "<b>toString(): </b>" + o + "<br>"
						+ "</html>";
				SwingUtils.showWarnMessage(ConnectionDialog.this, "Load problems", htmlMsg, null);
			}
		}
	}

	private class ConnProfileLoadOnNodeSelectAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "Enable 'Load' when a Profile is Selected";
//		private static final String ICON = "images/conn_profile_tree_load_on_click.png";
		private static final String ICON = "";

		public ConnProfileLoadOnNodeSelectAction()
		{
			super(NAME, SwingUtils.readImageIcon(Version.class, ICON));

			putValue(SHORT_DESCRIPTION,	
					"<html>"
					+ "Load values/properties from the Connection Profile into the Connection Dialog<br>"
					+ "Then you can view/change attributes and press <b>OK</b> to make the connection."
					+ "</html>");
			//putValue(MNEMONIC_KEY, mnemonic);
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
			if (conf == null)
				return;
			
			conf.setProperty(ConnectionProfileManager.PROPKEY_connProfile_load_onNodeSelection, ((JCheckBoxMenuItem)e.getSource()).isSelected());
			conf.save();
		}
	}

	private class ConnProfileConnectAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "Connect";
//		private static final String ICON = "images/conn_profile_tree_delete.png";
		private static final String ICON = "";

		public ConnProfileConnectAction()
		{
			super(NAME, SwingUtils.readImageIcon(Version.class, ICON));

			putValue(SHORT_DESCRIPTION,	"<html>Connect to the server using the Connection Profile</html>");
			//putValue(MNEMONIC_KEY, mnemonic);
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) _connProfileTree.getLastSelectedPathComponent();
			Object o = node.getUserObject();

			if (o instanceof ConnectionProfile)
			{
				ConnectionProfile entry = (ConnectionProfile)o;
				connect(entry);
			}
			else
			{
				String htmlMsg = 
						"<html>"
						+ "<h3>Unknown node type</h3>"
						+ "<b>Classname: </b>" + o.getClass().getName() + "<br>"
						+ "<b>toString(): </b>" + o + "<br>"
						+ "</html>";
				SwingUtils.showWarnMessage(ConnectionDialog.this, "Connect problems", htmlMsg, null);
			}
		}
	}

	private class ConnProfileDeleteAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "Delete";
//		private static final String ICON = "images/conn_profile_tree_delete.png";
		private static final String ICON = "";

		public ConnProfileDeleteAction()
		{
			super(NAME, SwingUtils.readImageIcon(Version.class, ICON));

			putValue(SHORT_DESCRIPTION,	"<html>Delete/Remove a Connection Profile Entry.</html>");
			//putValue(MNEMONIC_KEY, mnemonic);
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) _connProfileTree.getLastSelectedPathComponent();
			Object o = node.getUserObject();

			if (o instanceof ConnectionProfileCatalog)
			{
				ConnectionProfileCatalog entry = (ConnectionProfileCatalog)o;
				ConnectionProfileManager.getInstance().deleteCatalog(entry);
			}
			else if (o instanceof ConnectionProfile)
			{
				ConnectionProfile entry = (ConnectionProfile)o;
				ConnectionProfileManager.getInstance().deleteProfile(entry);
			}
			else
			{
				ConnectionProfileManager.getInstance().deleteCatalog(o);
//				String htmlMsg = 
//						"<html>"
//						+ "<h3>Unknown node type</h3>"
//						+ "<b>Classname: </b>" + o.getClass().getName() + "<br>"
//						+ "<b>toString(): </b>" + o + "<br>"
//						+ "</html>";
//				SwingUtils.showWarnMessage(ConnectionDialog.this, "Delete problems", htmlMsg, null);
			}
		}
	}

	private class ConnProfileAddProfileAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "Add Profile...";
//		private static final String ICON = "images/conn_profile_tree_add_profile.png";
		private static final String ICON = "";

		public ConnProfileAddProfileAction()
		{
			super(NAME, SwingUtils.readImageIcon(Version.class, ICON));

			putValue(SHORT_DESCRIPTION,	"<html>Add Connection Profile based on the selected attributes in the panel on the right hand side.</html>");
			//putValue(MNEMONIC_KEY, mnemonic);
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			String htmlMsg = 
					"<html>"
					+ "<h3>Add a Connection Profile</h3>"
					+ "<br>"
					+ "There are two ways to for adding a Connection Profile.<br>"
					+ "<br>"
					+ "The <i>automatic</i> way, after a successful connection<br>"
					+ "<ul>"
					+ "   <li>Fill in all the desired connection properties</li>"
					+ "   <li>Press <i>OK</i></li>"
					+ "   <li>On <b>successfull</b> connect, a poupup will be presented.<br>"
					+ "       Fill in the name you want this profile to be saved as."
					+ "   </li>"
					+ "   <li>Next time the Connection Dialog opens the new Profile will be visible.</li>"
					+ "</ul>"
					+ "The <i>manual</i> way<br>"
					+ "<ul>"
					+ "   <li>Fill in all the desired connection attributes, in the connection properties panel (to the right)</li>"
					+ "   <li>Press the <i>Save Profile...</i> button to the right hand side of the 'Use Profile' drop down.</li>"
					+ "   <li>A new Dialog will be visible where you can choose a Name for the Profile.</li>"
					+ "   <li>Tip: You can also use this method to Save changes to a existing profile, or Save As a New Profile name</li>"
					+ "</ul>"
					+ "</html>";
			SwingUtils.showInfoMessage(ConnectionDialog.this, "Add Profile", htmlMsg);
		}
	}

	private class ConnProfileAddCatalogAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "Add Catalog...";
//		private static final String ICON = "images/conn_profile_tree_add_catalog.png";
		private static final String ICON = "";

		public ConnProfileAddCatalogAction()
		{
			super(NAME, SwingUtils.readImageIcon(Version.class, ICON));

			putValue(SHORT_DESCRIPTION,	
					"<html>"
					+ "Add a Directory/Catalog entry to the Connection Profile<br>"
					+ "You can then <i>drag and drop</i> Profiles into that catalog<br>"
					+ "<br>"
					+ "This is a good way to organize your Connection Profiles.<br>"
					+ "</html>");
			//putValue(MNEMONIC_KEY, mnemonic);
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
//			// Open a dialog, and get the new name
//			String key1 = "Catalog Name";
//
//			LinkedHashMap<String, String> in = new LinkedHashMap<String, String>();
//			in.put(key1, "");
//
//			Map<String,String> results = ParameterDialog.showParameterDialog(ConnectionDialog.this, "Rename", in, false);
//
//			if (results != null)
//			{
//				String name = results.get(key1).trim();
//				ConnectionProfileManager.getInstance().addCatalog(new ConnectionProfileCatalog(name));
//			}

			RenameOrAddDialog dialog = new RenameOrAddDialog(ConnectionDialog.this, null, "<html><b>Add a Catalog</b></html>");
			dialog.setVisible(true);
			if (dialog.pressedOk())
			{
				String name = dialog.getName();
				ConnectionProfileManager.getInstance().addCatalog(new ConnectionProfileCatalog(name, true));
			}
		}
	}

	private class ConnProfileMoveAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "Move...";
//		private static final String ICON = "images/conn_profile_tree_move.png";
		private static final String ICON = "";

		public ConnProfileMoveAction()
		{
			super(NAME, SwingUtils.readImageIcon(Version.class, ICON));

			putValue(SHORT_DESCRIPTION,	"<html>Show instructions how you move a Profile</html>");
			//putValue(MNEMONIC_KEY, mnemonic);
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			String htmlMsg = 
					"<html>"
					+ "<h3>Move a Connection Profile in the Tree</h3>"
					+ "<br>"
					+ "Use <b>Drag and drop</b> to move entries in the tree.<br>"
					+ "<br>"
					+ "<b>Tip</b>: To move a Catalog with all it's content/children, you also need to select <b>all</b> children/profiles before starting the <i>drag</i> operation."
					+ "</html>";
			SwingUtils.showInfoMessage(ConnectionDialog.this, "Move", htmlMsg);
		}
	}

	private class ConnProfileRenameAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "Rename...";
//		private static final String ICON = "images/conn_profile_tree_rename.png";
		private static final String ICON = "";

		public ConnProfileRenameAction()
		{
			super(NAME, SwingUtils.readImageIcon(Version.class, ICON));

			putValue(SHORT_DESCRIPTION,	"<html>Change name of a Connection Profile Entry.</html>");
			//putValue(MNEMONIC_KEY, mnemonic);
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
//			TreePath selPath = _connProfileTree.getSelectionPath();
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) _connProfileTree.getLastSelectedPathComponent();
			Object o = node.getUserObject();
			String name = "";
			ConnectionProfileCatalog catalogEntry = null;
			ConnectionProfile        profileEntry = null;
			if (o instanceof ConnectionProfileCatalog)
			{
				catalogEntry = (ConnectionProfileCatalog)o;
				name = catalogEntry.getName();
			}
			else if (o instanceof ConnectionProfile)
			{
				profileEntry = (ConnectionProfile)o;
				name = profileEntry.getName();
			}


//			// Open a dialog, and get the new name
//			String key1 = "New Name";
//
//			LinkedHashMap<String, String> in = new LinkedHashMap<String, String>();
//			in.put(key1, name);
//
//			Map<String,String> results = ParameterDialog.showParameterDialog(ConnectionDialog.this, "Rename", in, false);
//
//			if (results != null)
//			{
//				String newName = results.get(key1).trim();
//				if (catalogEntry != null) catalogEntry.setName(newName);
//				if (profileEntry != null) profileEntry.setName(newName);
//				
//				((DefaultTreeModel)_connProfileTree.getModel()).nodeChanged(node);
////				ConnectionProfileManager.getInstance().save();
//			}

			RenameOrAddDialog dialog = new RenameOrAddDialog(ConnectionDialog.this, name, "<html><b>Rename a Connection Profile</b></html>");
			dialog.setVisible(true);
			if (dialog.pressedOk())
			{
				String newName = dialog.getName();

				if (catalogEntry != null) catalogEntry.setName(newName);
				if (profileEntry != null) profileEntry.setName(newName);
				
				((DefaultTreeModel)_connProfileTree.getModel()).nodeChanged(node);
			}


//			SwingUtils.showInfoMessage(FavoriteCommandDialog.this, "Select a entry", "No entry is selected");
		}
	}
	
	private class ConnProfileDuplicateAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "Duplicate...";
//		private static final String ICON = "images/conn_profile_tree_rename.png";
		private static final String ICON = "";

		public ConnProfileDuplicateAction()
		{
			super(NAME, SwingUtils.readImageIcon(Version.class, ICON));

			putValue(SHORT_DESCRIPTION,	"<html>Duplicate Connection Profile Entry.</html>");
			//putValue(MNEMONIC_KEY, mnemonic);
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
//			TreePath selPath = _connProfileTree.getSelectionPath();
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) _connProfileTree.getLastSelectedPathComponent();
			Object o = node.getUserObject();
			String name = "";
			ConnectionProfileCatalog catalogEntry = null;
			ConnectionProfile        profileEntry = null;
			if (o instanceof ConnectionProfileCatalog)
			{
				SwingUtils.showWarnMessage(ConnectionDialog.this, "NOT supported", "Duplication of Connection Profile Catalog is NOT supported!", null);
				return;
				// catalogEntry = (ConnectionProfileCatalog)o;
				// name = catalogEntry.getName();
			}
			else if (o instanceof ConnectionProfile)
			{
				profileEntry = (ConnectionProfile)o;
				name = profileEntry.getName();
			}
			
			name += "_copy";

			RenameOrAddDialog dialog = new RenameOrAddDialog(ConnectionDialog.this, name, "<html><b>Duplicate a Connection Profile</b></html>");
			dialog.setVisible(true);
			if (dialog.pressedOk())
			{
				String newName = dialog.getName();

				//if (catalogEntry != null)
				//{
				//	catalogEntry.copy(newName);
				//	catalogEntry.setName(newName);
				//}
				if (profileEntry != null)
				{
					if (ConnectionProfileManager.hasInstance())
					{
						DefaultMutableTreeNode catalogNode = null;
						DefaultMutableTreeNode parentTreeNode = (DefaultMutableTreeNode) node.getParent();
						Object parentObj = parentTreeNode.getUserObject();
						if (parentObj instanceof ConnectionProfileCatalog)
						{
							catalogNode = parentTreeNode;
							//catalogName = ((ConnectionProfileCatalog)parentObj).getName();							
						}

						// Create a new ConnectionProfile Entry
						ConnectionProfile newConnProfile = profileEntry.copy(profileEntry, newName);

						// Add the profile to the Manager
						ConnectionProfileManager.getInstance().addProfile(newConnProfile, true, catalogNode, node);

						((DefaultTreeModel)_connProfileTree.getModel()).nodeChanged(parentTreeNode);
					}
				}
			}


//			SwingUtils.showInfoMessage(FavoriteCommandDialog.this, "Select a entry", "No entry is selected");
		}
	}
	
	private class ConnProfileCopyNameAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "Copy Profile Name";
		private static final String ICON = "";

		public ConnProfileCopyNameAction()
		{
			super(NAME, SwingUtils.readImageIcon(Version.class, ICON));

			putValue(SHORT_DESCRIPTION,	
					"<html>"
					+ "Copy the Profile Name to Copy/Paste buffer<br>"
					+ "</html>");
			//putValue(MNEMONIC_KEY, mnemonic);
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) _connProfileTree.getLastSelectedPathComponent();
			Object o = node.getUserObject();
			String name = "";
			ConnectionProfileCatalog catalogEntry = null;
			ConnectionProfile        profileEntry = null;
			if (o instanceof ConnectionProfileCatalog)
			{
				catalogEntry = (ConnectionProfileCatalog)o;
				name = catalogEntry.getName();
			}
			else if (o instanceof ConnectionProfile)
			{
				profileEntry = (ConnectionProfile)o;
				name = profileEntry.getName();
			}
			
			SwingUtils.setClipboardContents(name);
		}
	}

	private class ConnProfileReloadAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "Reload 'Connection Profiles' from current file";
		private static final String ICON = "images/refresh_now_1.png";

		public ConnProfileReloadAction()
		{
			super(NAME, SwingUtils.readImageIcon(Version.class, ICON));

			putValue(SHORT_DESCRIPTION,	
					"<html>"
					+ "Reload the connection profile content from the current storage file...<br>"
					+ "this is most usable if you have two windows open, and you change the profile in any way.<br>"
					+ "For example if you add/change/delete profiles or catalogs.<br>"
					+ "</html>");
			//putValue(MNEMONIC_KEY, mnemonic);
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			_treeExpansionListenerEnabled = false;
			ConnectionProfileManager.getInstance().reload();
			restoreToSavedTreeExpansionState(_connProfileTree);
			_treeExpansionListenerEnabled = true;
		}
	}

	private class ConnProfileChangeFileAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "Change 'Connection Profile' Storage File...";
//		private static final String ICON = "images/conn_profile_tree_change_file.png";
		private static final String ICON = "";

		public ConnProfileChangeFileAction()
		{
			super(NAME, SwingUtils.readImageIcon(Version.class, ICON));

			putValue(SHORT_DESCRIPTION,	"<html>Change what storage file that are used for storing Connection Profile Entries.</html>");
			//putValue(MNEMONIC_KEY, mnemonic);
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			// Open a dialog, and get the new name
			String key1 = "Connection Profile XML File";

			LinkedHashMap<String, String> in = new LinkedHashMap<String, String>();
			in.put(key1, ConnectionProfileManager.getInstance().getFilename());

			Map<String,String> results = ParameterDialog.showParameterDialog(ConnectionDialog.this, "Set XML File", in, false);

			if (results != null)
			{
				String newName = results.get(key1).trim();
				
				ConnectionProfileManager.getInstance().setFilename(newName);
			}
		}
	}

	private class ConnProfileShowAddProfileAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "<html>Show 'Add Profile Dialog' on <b>successful connect</b></html>";
//		private static final String ICON = "images/conn_profile_tree_showAddProfile.png";
		private static final String ICON = "";

		public ConnProfileShowAddProfileAction()
		{
			super(NAME, SwingUtils.readImageIcon(Version.class, ICON));

			putValue(SHORT_DESCRIPTION, 
					"<html>"
					+ "Show a Dialog after a successfull connect<br>"
					+ "In this dialog you can choose a <b>Profile</b> name, which will be added to the Prilfe tree.<br>"
					+ "</html>");
			//putValue(MNEMONIC_KEY, mnemonic);
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
			if (conf == null)
				return;
			
			conf.setProperty(ConnectionProfileManager.PROPKEY_connProfile_serverAdd_showDialog, ((JCheckBoxMenuItem)e.getSource()).isSelected());
			conf.save();
		}
	}

	private class ConnProfileShowAddIFileAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME1 = "<html>Show 'Add Server' to <i>sql.ini</i> file on on <b>successful connect</b></html>";
		private static final String NAME2 = "<html>Show 'Add Server' to <i>interfaces</i> file on <b>successful connect</b></html>";
//		private static final String ICON = "images/conn_profile_tree_showAddIFile.png";
		private static final String ICON = "";

		public ConnProfileShowAddIFileAction()
		{
			super(
				PlatformUtils.getCurrentPlattform() == PlatformUtils.Platform_WIN ? NAME1 : NAME2, 
				SwingUtils.readImageIcon(Version.class, ICON));

			putValue(SHORT_DESCRIPTION, 
					"<html>"
					+ "Show a Dialog after a successfull connect<br>"
					+ "In this dialog you can choose a name, which the <i>host,port</i> will be added as to the sql.ini or interfaces file.<br>"
					+ "</html>");
			//putValue(MNEMONIC_KEY, mnemonic);
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
			if (conf == null)
				return;
			
			conf.setProperty(ConnectionProfileManager.PROPKEY_ifile_serverAdd_showDialog, ((JCheckBoxMenuItem)e.getSource()).isSelected());
			conf.save();
		}
	}

	private class ConnProfileShowSaveChangesAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "<html>Show 'Save Attributes Changes' question on <b>successful connect</b></html>";
//		private static final String ICON = "images/conn_profile_tree_saveChanges.png";
		private static final String ICON = "";

		public ConnProfileShowSaveChangesAction()
		{
			super(NAME, SwingUtils.readImageIcon(Version.class, ICON));

			putValue(SHORT_DESCRIPTION, 
					"<html>"
					+ "If Connection Attributes was changed since last time this profile was used...<br>"
					+ "Open a question if you want to save the changes.<br>"
					+ "</html>");
			//putValue(MNEMONIC_KEY, mnemonic);
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
			if (conf == null)
				return;
			
			conf.setProperty(ConnectionProfileManager.PROPKEY_connProfile_changed_showDialog, ((JCheckBoxMenuItem)e.getSource()).isSelected());
			conf.save();
		}
	}

	private class ConnProfileDefaultSaveChangesAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "Automatically 'Save Attribute Changes' if question is disabled";
//		private static final String ICON = "images/conn_profile_tree_saveChanges.png";
		private static final String ICON = "";

		public ConnProfileDefaultSaveChangesAction()
		{
			super(NAME, SwingUtils.readImageIcon(Version.class, ICON));

			putValue(SHORT_DESCRIPTION, 
					"<html>"
					+ "If Connection Attributes was changed since last time this profile was used...<br>"
					+ "AND The above 'save changes' question is disabled.<br>"
					+ "The profile changes will automatically saved or NOT.<br>"
					+ "</html>");
			//putValue(MNEMONIC_KEY, mnemonic);
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
			if (conf == null)
				return;
			
			conf.setProperty(ConnectionProfileManager.PROPKEY_connProfile_changed_alwaysSave, ((JCheckBoxMenuItem)e.getSource()).isSelected());
			conf.save();
		}
	}

	private class ConnProfileShowTreeRootAction
	extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private static final String NAME = "Show 'Root Handlers' (+) in the tree view.";
//		private static final String ICON = "images/conn_profile_tree_saveChanges.png";
		private static final String ICON = "";

		public ConnProfileShowTreeRootAction()
		{
			super(NAME, SwingUtils.readImageIcon(Version.class, ICON));

			putValue(SHORT_DESCRIPTION, 
					"<html>"
					+ "In the Connection Profiles, do you want the 'root handler' (+) signs at the root level to be visible.<br>"
					+ "</html>");
			//putValue(MNEMONIC_KEY, mnemonic);
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
			if (conf == null)
				return;
			
			boolean isSelected = ((JCheckBoxMenuItem)e.getSource()).isSelected();
//			_connProfileTree.setRootVisible(isSelected);
			_connProfileTree.setShowsRootHandles(isSelected);

			conf.setProperty(ConnectionProfileManager.PROPKEY_connProfile_show_rootNode, isSelected);
			conf.save();
		}
	}


	/*---------------------------------------------------
	**---------------------------------------------------
	**---------------------------------------------------
	**---- SUBCLASSES ---- SUBCLASES ---- SUBCLASES ----- 
	**---------------------------------------------------
	**---------------------------------------------------
	**---------------------------------------------------
	*/
	private class RenameOrAddDialog
	extends JDialog
	implements ActionListener
	{
		private static final long serialVersionUID = 1L;

//		private JDialog      _owner       = null;
		
		private String       _oldName     = null;
		private String       _newName     = null;
		private boolean      _pressedOk   = false;
		
		private String       _htmlMsg     = "";

		private JLabel       _name_head   = new JLabel();
		private JLabel       _name_lbl    = new JLabel("Name");
		private JTextField   _name_txt    = new JTextField(30);
		private JLabel       _name_bussy  = new JLabel("The Name already exists, choose another one.");

		private JButton      _ok_but     = new JButton("OK");
		private JButton      _cancel_but = new JButton("Cancel");

		public RenameOrAddDialog(JDialog owner, String oldName, String htmlMsg)
		{
			super(owner, oldName != null ? "Rename" : "Add", true);
			
//			_owner     = owner;
			_oldName   = oldName;
			_newName   = null;
			_pressedOk = false;
			
			_htmlMsg   = htmlMsg;
			if (_htmlMsg == null)
				_htmlMsg = "";
			
			init();
			pack();
			setLocationRelativeTo(owner);

			validateCompenents();

			// Focus to 'OK', escape to 'CANCEL'
			SwingUtils.installEscapeButton(this, _cancel_but);
			SwingUtils.setFocus(_name_txt);
		}
		
		public boolean pressedOk()
		{
			return _pressedOk;
		}

		@Override
		public String getName()
		{
			return _newName;
		}

		private void init()
		{

			_name_bussy.setForeground(Color.RED);
			_name_txt.addKeyListener(new KeyAdapter()
			{
				@Override
				public void keyReleased(KeyEvent e)
				{
					validateCompenents();
				}
			});

			_name_head.setText(_htmlMsg);

			if (_oldName != null)
				_name_txt .setText(_oldName);

			setLayout(new MigLayout());

			JPanel panel = new JPanel(new MigLayout());
			panel.add(_name_head,  "span, wrap 20");
			panel.add(_name_lbl,   "");
			panel.add(_name_txt,   "pushx, growx, wrap");
			panel.add(_name_bussy, "skip, pushx, growx, hidemode 3, wrap");
    
			add(panel,               "wrap");
			add(_ok_but,             "split, tag ok");
			add(_cancel_but,         "tag cancel");
			
			_name_txt  .addActionListener(this); // on enter: press OK

			_ok_but    .addActionListener(this);
			_cancel_but.addActionListener(this);
		}

		private void validateCompenents()
		{
			boolean enableOk = true;

			String currentItem = _name_txt.getText();
			boolean bussy = ConnectionProfileManager.getInstance().exists(currentItem);
			_name_bussy.setVisible(bussy);

			if (bussy)
				enableOk = false;

			_ok_but.setEnabled(enableOk);
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			Object source = e.getSource();
			
			if (_name_txt.equals(source))
			{
				_ok_but.doClick();
			}

			if (_ok_but.equals(source))
			{
				_pressedOk = true;
				_newName = _name_txt.getText();

				setVisible(false);
			} // end OK
			
			if (_cancel_but.equals(source))
			{
				_pressedOk = false;
				_newName   = null;

				setVisible(false);
			}
		}
		
	}

	protected class LocalSrvComboBox
	extends JComboBox<String>
	{
		private static final long   serialVersionUID   = 7884363654457237606L;
		private static final String SERVER_FIRST_ENTRY = "-CHOOSE A SERVER-";

		private LocalSrvComboBoxModel  _model = null;

		private class LocalSrvComboBoxModel 
		extends DefaultComboBoxModel<String>
		{
			static final long serialVersionUID = -318689353529705207L;
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

	protected class TemplateComboBoxModel
	extends DefaultComboBoxModel<String>
	{
		private static final long serialVersionUID = 1L;

		public TemplateComboBoxModel()
		{
			super();
			refresh();
		}
		
		public void refresh()
		{
			removeAllElements();

			addElement(NO_TEMPLATE_IS_SELECTED);;

			List<String> templates = TcpConfigDialog.getSystemAndUserDefinedTemplateNames();
			for (String name : templates)
				addElement(name);
		}

		public String notSelectedValueToNull(String value)
		{
			if (NO_TEMPLATE_IS_SELECTED.equals(value))
				return null;
			return value;
		}
	}

	protected static class ProfileComboBoxModel
	extends DefaultComboBoxModel<String>
	{
		private static final long serialVersionUID = 1L;
		ConnectionProfile.Type _type;

		public ProfileComboBoxModel(ConnectionProfile.Type type)
		{
			super();
			_type = type;
			refresh();
		}

		@SuppressWarnings("unchecked")
		public void refresh()
		{
			// NOTE: the save and restore wont work if there are more than 1 listener
			// NOTE: this is a ugly hack, redo this with our own implementation of JComboBox instead
			String currentSelected = null;
			JComboBox<String> cbxOwner = null;
			ListDataListener[] la = getListDataListeners();
			for (int i=0; i<la.length; i++)
			{
				if (la[i] instanceof JComboBox)
				{
					_logger.debug("refresh("+_type+"): "+ StringUtil.getSelectedItemString( ((JComboBox<String>)la[i]) ) );
					cbxOwner = (JComboBox<String>)la[i];
					currentSelected = notSelectedValueToNull(StringUtil.getSelectedItemString(cbxOwner));
				}
			}
			
			Set<String> names = ConnectionProfileManager.getInstance().getProfileNames(_type);
			removeAllElements();
			
			addElement(NO_PROFILE_IS_SELECTED);
			for (String name : names)
				addElement(name);

			if (cbxOwner != null && currentSelected != null)
				cbxOwner.setSelectedItem(currentSelected);
		}

		public static String notSelectedValueToNull(String value)
		{
			if (NO_PROFILE_IS_SELECTED.equals(value))
				return null;
			return value;
		}
	}

	protected static class ProfileTypeComboBoxModel
	extends DefaultComboBoxModel<ProfileType>
	{
		private static final long serialVersionUID = 1L;

		public ProfileTypeComboBoxModel()
		{
			super();
			refresh();
		}

		public static String getSelectedProfileTypeName(JComboBox<ProfileType> cbx)
		{
			ProfileType profileType = (ProfileType) cbx.getSelectedItem();
			if (profileType == null)
				return null;
			if (NO_PROFILE_TYPE_IS_SELECTED.equals(profileType))
				return null;
			if (EDIT_PROFILE_TYPE_IS_SELECTED.equals(profileType))
				return null;
			return profileType.getName();
		}

		@SuppressWarnings("rawtypes")
		public void refresh()
		{
			// NOTE: the save and restore wont work if there are more than 1 listener
			// NOTE: this is a ugly hack, redo this with our own implementation of JComboBox instead
			ProfileType currentSelected = null;
			JComboBox cbxOwner = null;
			ListDataListener[] la = getListDataListeners();
			for (int i=0; i<la.length; i++)
			{
				if (la[i] instanceof JComboBox)
				{
					cbxOwner = (JComboBox)la[i];
					currentSelected = (ProfileType) cbxOwner.getSelectedItem();
				}
			}
			
			removeAllElements();
			
			addElement(NO_PROFILE_TYPE_IS_SELECTED);
			addElement(EDIT_PROFILE_TYPE_IS_SELECTED);

			for (ProfileType type : ConnectionProfileManager.getInstance().getProfileTypes().values())
			{
				addElement(type);
			}

			if (cbxOwner != null && currentSelected != null)
				cbxOwner.setSelectedItem(currentSelected);
		}
	}

	/**
	 * Set the color and size of the main border around the window
	 * @param profileType
	 */
	public void setBorderForConnectionProfileTypeName(String profileTypeName)
	{
		Container contContentPane = getContentPane();
		ConnectionProfileManager.setBorderForConnectionProfileType(contContentPane, profileTypeName);
	}


	/**
	 * Set the ComboBox to the right value
	 * Set the color and size of the main border around the window
	 * @param profileType
	 */
	private void setConnectionProfileTypeName(ConnectionProfile profile, String profileTypeName)
	{
		// Retrive the ProfileType OBJECT
		ProfileType profileType = ConnectionProfileManager.getInstance().getProfileTypeByName(profileTypeName);
		if (profileType == null)
			profileType = NO_PROFILE_TYPE_IS_SELECTED;

		if      (profile.isType(ConnectionProfile.Type.TDS)    ) _aseProfileType_cbx    .setSelectedItem(profileType);
		else if (profile.isType(ConnectionProfile.Type.JDBC)   ) _jdbcProfileType_cbx   .setSelectedItem(profileType);
		else if (profile.isType(ConnectionProfile.Type.OFFLINE)) _offlineProfileType_cbx.setSelectedItem(profileType);

		// Set border
		setBorderForConnectionProfileTypeName(profileTypeName);
	}

	/** Set selected item in the Tree View */
	private void setConnectionProfileSelectedName(String name)
	{
		TreePath treePath = SwingUtils.findNameInTree(_connProfileTree, name);
		if (treePath != null)
		{
			_connProfileTree.setSelectionPath(treePath);

			// Scroll the selected tree path to "center" of the view
			SwingUtils.scrollToCenter(_connProfileTree, treePath, true);
		}
	}

	private ProfileType getSelectedConnectionProfileType(int connType)
	{
		JComboBox<ProfileType> cbx;
		cbx = _aseProfileType_cbx;
		
//		int connType = getConnectionType();
		if      (connType == TDS_CONN    ) cbx = _aseProfileType_cbx;
		else if (connType == JDBC_CONN   ) cbx = _jdbcProfileType_cbx;
		else if (connType == OFFLINE_CONN) cbx = _offlineProfileType_cbx;
		else return null;

//		String profileType = StringUtil.getSelectedItemString(cbx);
		ProfileType profileType = (ProfileType) cbx.getSelectedItem();
		if (profileType == null)
			return null;
		
		if (profileType.equals(NO_PROFILE_TYPE_IS_SELECTED) || profileType.equals(EDIT_PROFILE_TYPE_IS_SELECTED))
			return null;

		return profileType;
	}
	
	private String getSelectedConnectionProfileTypeName(int connType)
	{
		ProfileType profileType = getSelectedConnectionProfileType(connType);
		if (profileType == null)
			return null;
		
		return profileType.getName();
	}

	/** Get the connection profile type NAME that was used when connecting. If none was selected null will be returned */
	public String getConnectionProfileTypeName()
	{
		return _usedConnectionProfileTypeName;
	}

	//--------------------------------------------------
	// TEST-CODE
	//--------------------------------------------------
	public static void main(String[] args)
	{
		final String CONFIG_FILE_NAME     = System.getProperty("CONFIG_FILE_NAME",     "conf" + File.separatorChar + "dbxtune.properties");
		final String TMP_CONFIG_FILE_NAME = System.getProperty("TMP_CONFIG_FILE_NAME", "asetune.save.properties");
		final String DBXTUNE_HOME         = System.getProperty("DBXTUNE_HOME");
		
		String defaultPropsFile    = (DBXTUNE_HOME               != null) ? DBXTUNE_HOME               + File.separator + CONFIG_FILE_NAME     : CONFIG_FILE_NAME;
		String defaultTmpPropsFile = (AppDir.getDbxUserHomeDir() != null) ? AppDir.getDbxUserHomeDir() + File.separator + TMP_CONFIG_FILE_NAME : TMP_CONFIG_FILE_NAME;
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		// Set Log4j Log Level
//		Configurator.setRootLevel(Level.TRACE);

		Configuration conf1 = new Configuration(defaultTmpPropsFile);
		Configuration.setInstance(Configuration.USER_TEMP, conf1);

		Configuration conf2 = new Configuration(defaultPropsFile);
		Configuration.setInstance(Configuration.SYSTEM_CONF, conf2);

		System.out.println("showTdsOnlyConnectionDialog ...");
		Connection conn = ConnectionDialog.showTdsOnlyConnectionDialog(null);
		System.out.println("showTdsOnlyConnectionDialog, returned: conn="+conn);

		// DO THE THING
//		ConnectionDialog connDialog = new ConnectionDialog(null, null, true, true, false, false, false, true, false);
		ConnectionDialog connDialog = new ConnectionDialog(null, new Options(null, true, true, false, false, false, true, false));
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

