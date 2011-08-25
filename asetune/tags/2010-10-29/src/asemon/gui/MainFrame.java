/**
 */

package asemon.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.apache.log4j.lf5.LogLevel;

import asemon.Asemon;
import asemon.GetCounters;
import asemon.GetCountersGui;
import asemon.MonTablesDictionary;
import asemon.ProcessDetailFrame;
import asemon.Version;
import asemon.check.CheckForUpdates;
import asemon.cm.CountersModel;
import asemon.gui.swing.AbstractComponentDecorator;
import asemon.gui.swing.GTabbedPane;
import asemon.gui.swing.Screenshot;
import asemon.gui.wizard.WizardOffline;
import asemon.gui.wizard.WizardUserDefinedCm;
import asemon.pcs.InMemoryCounterHandler;
import asemon.pcs.PersistContainer;
import asemon.pcs.PersistReader;
import asemon.pcs.PersistentCounterHandler;
import asemon.utils.AseConnectionFactory;
import asemon.utils.AseConnectionUtils;
import asemon.utils.Configuration;
import asemon.utils.Memory;
import asemon.utils.SwingUtils;

public class MainFrame
    extends JFrame
    implements ActionListener, ChangeListener, TableModelListener
{
	private static final long    serialVersionUID = 8984251025337127843L;
	private static Logger        _logger          = Logger.getLogger(MainFrame.class);

	//-------------------------------------------------
	// STATUS fields
	public static final int     ST_CONNECT                 = 1;
	public static final int     ST_OFFLINE_CONNECT         = 2;
	public static final int     ST_DISCONNECT              = 3;
	public static final int     ST_STATUS_FIELD            = 4;
	public static final int     ST_MEMORY                  = 5;

	private static final String ST_DEFAULT_STATUS_FIELD    = "Not Connected";
	private static final String ST_DEFAULT_SERVER_NAME     = "ASENAME (host:port)";
	private static final String ST_DEFAULT_SERVER_LISTENERS= "ASE Server listens on address";

	//-------------------------------------------------
	// Actions
	public static final String ACTION_CONNECT                   = "CONNECT";
	public static final String ACTION_DISCONNECT                = "DISCONNECT";
	public static final String ACTION_EXIT                      = "EXIT";

	public static final String ACTION_OPEN_LOG_VIEW             = "OPEN_LOG_VIEW";
	public static final String ACTION_OPEN_OFFLINE_SESSION_VIEW = "OPEN_OFFLINE_SESSION_VIEW";
	public static final String ACTION_OPEN_REFRESH_RATE         = "OPEN_REFRESH_RATE";
	public static final String ACTION_OPEN_ASE_CONFIG_MON       = "OPEN_ASE_CONFIG_MON";
	public static final String ACTION_OPEN_TCP_PANEL_CONFIG     = "OPEN_TCP_PANEL_CONFIG";

	public static final String ACTION_OPEN_CAPTURE_SQL          = "OPEN_CAPTURE_SQL";
	public static final String ACTION_OPEN_SQL_QUERY_WIN        = "OPEN_SQL_QUERY_WIN";
	public static final String ACTION_OPEN_LOCK_TOOL            = "OPEN_LOCK_TOOL";
	public static final String ACTION_OPEN_WIZARD_OFFLINE       = "OPEN_WIZARD_OFFLINE";
	public static final String ACTION_OPEN_WIZARD_UDCM          = "OPEN_WIZARD_UDCM";
	public static final String ACTION_GARBAGE_COLLECT           = "GARBAGE_COLLECT";

	public static final String ACTION_OPEN_ABOUT                = "OPEN_ABOUT";
	
	public static final String ACTION_VIEW_STORAGE              = "ACTION_VIEW_STORAGE";

	public static final String ACTION_REFRESH_NOW               = "REFRESH_NOW";

	public static final String ACTION_GOTO_BLOCKED_TAB          = "GOTO_BLOCKED_TAB";

	public static final String ACTION_OUT_OF_MEMORY             = "OUT_OF_MEMORY";
	
	public static final String ACTION_SCREENSHOT                = "SCREENSHOT";
	public static final String ACTION_SAMPLING_PAUSE            = "SAMPLING_PAUSE";
	public static final String ACTION_SAMPLING_PLAY             = "SAMPLING_PLAY";


	private PersistContainer _currentPc      = null;

	//-------------------------------------------------
	// Menus / toolbar

	private JToolBar            _toolbar                = new JToolBar();
	private JToolBar            _pcsNavigation          = new JToolBar();
//	private JCheckBox           _viewStorage_chk        = new JCheckBox("View Stored Data", false);
	private JCheckBox           _viewStorage_chk        = new JCheckBox();
	private JSlider             _readSlider             = new JSlider();
	private ReadTsWatermark     _readTsWatermark        = null;
	private Timer               _readSelectionTimer     = new Timer(100, new ReadSelectionTimerAction(this));
	private JSlider             _offlineSlider          = new JSlider();
	private ReadTsWatermark     _offlineTsWatermark     = null;
	private Timer               _offlineSelectionTimer  = new Timer(100, new OfflineSelectionTimerAction(this));

	private JComboBox           _workspace_cbx          = new JComboBox();
	
	private JMenuBar            _main_mb                = new JMenuBar();

	// File
	private JMenu               _file_m                 = new JMenu("File");
	private JMenuItem           _connect_mi             = new JMenuItem("Connect");
	private JMenuItem           _disconnect_mi          = new JMenuItem("Disconnect");
	private JMenuItem           _exit_mi                = new JMenuItem("Exit");

	// View
	private JMenu               _view_m                 = new JMenu("View");
	private JMenuItem           _logView_mi             = new JMenuItem("Log Window");
	private JMenuItem           _offlineSessionsView_mi = new JMenuItem("Offline Sessions Window");
	private JMenuItem           _refreshRate_mi         = new JMenuItem("Refresh Rate");
	private JMenuItem           _aseMonConf_mi          = new JMenuItem("Config ASE for Monitoring");
	private JMenuItem           _tcpSettingsConf_mi     = new JMenuItem("Set Counter Set Parameters");
	private static JMenu        _graphs_m               = new JMenu("Active Graphs");
	
	// Tools
	private JMenu               _tools_m                = new JMenu("Tools");
	private JMenuItem           _captureSql_mi          = new JMenuItem("Capture SQL");
	private JMenuItem           _sqlQuery_mi            = new JMenuItem("SQL Query Window");
	private JMenuItem           _lockTool_mi            = new JMenuItem("Lock Tool (NOT YET IMPLEMENTED)");
	private JMenuItem           _createOffline_mi       = new JMenuItem("Create 'Offline Session' Wizard");
	private JMenuItem           _wizardCrUdCm_mi        = new JMenuItem("Create 'User Defined Counter' Wizard");
	private JMenuItem           _doGc_mi                = new JMenuItem("Java Garbage Collection");

	// Help
	private JMenu               _help_m                 = new JMenu("Help");
	private JMenuItem           _about_mi               = new JMenuItem("About");

	private static GTabbedPane  _mainTabbedPane         = new GTabbedPane();

	//-------------------------------------------------
	// STATUS Panel
	private JPanel                    _statusPanel               = new JPanel();
	private JButton                   _blockAlert_but            = new JButton();
	private JButton                   _refreshNow_but            = new JButton();
	private JButton                   _samplePause_but           = new JButton();
	private JButton                   _samplePauseTb_but         = new JButton();
	private JButton                   _samplePlay_but            = new JButton();
	private JButton                   _samplePlayTb_but          = new JButton();
	private JButton                   _gcNow_but                 = new JButton();
	private static JLabel             _statusStatus              = new JLabel(ST_DEFAULT_STATUS_FIELD);
	private static JLabel             _statusServerName          = new JLabel(ST_DEFAULT_SERVER_NAME);
	private static JLabel             _statusServerListeners     = new JLabel(ST_DEFAULT_SERVER_LISTENERS);
	private static JLabel             _statusMemory              = new JLabel("JVM Memory Usage");

	//-------------------------------------------------
	// Other members
	private static MainFrame          _instance                  = null;
	private static Log4jViewer        _logView                   = null;
//	private static Connection         _conn                      = null;
//	private static long               _lastIsClosedCheck         = 0;
//	private static long               _lastIsClosedRefreshTime   = 1200;
	private static Connection         _offlineConn               = null;
	private static long               _lastOfflineIsClosedCheck         = 0;
	private static long               _lastOfflineIsClosedRefreshTime   = 1200;
	private static OfflineSessionVeiwer _offlineView             = null;
	private static int                _refreshInterval           = 10;
	private static int                _refreshNoGuiInterval      = 60;
	private static long               _guiInitTime               = 0;

	private static String             _offlineSamplePeriod       = null;

	private static boolean            _isSamplingPaused          = false;
	
	/** Keep a list of all TabularCntrPanel that you have initialized */
	private static Map                _TcpMap                     = new HashMap();
	private static SummaryPanel       _summaryPanel               = new SummaryPanel();
	private static TabularCntrPanel   _currentPanel               = null;

	/** Keep a list of user defined SQL statements that will be used for tooltip on a JTable cell level */
	private static HashMap _udTooltipMap = new HashMap();
	//-------------------------------------------------

	public static int getRefreshInterval()      { return _refreshInterval; }
	public static int getRefreshIntervalNoGui() { return _refreshNoGuiInterval; }
	
	/*---------------------------------------------------
	** BEGIN: constructors
	**---------------------------------------------------
	*/
	public MainFrame()
	{
		super();

		_instance = this;

		//enableEvents(AWTEvent.WINDOW_EVENT_MASK);

		initComponents();
		loadProps();

		// Calculate initial size
//		pack();
		//setSize(new Dimension(747, 536));
	}
	/*---------------------------------------------------
	** END: constructors
	**---------------------------------------------------
	*/

	public static MainFrame getInstance()
	{
		return _instance;
	}

	public static boolean hasInstance()
	{
		return (_instance != null);
	}

	public static void setInstance(MainFrame inst)
	{
		_instance = inst;
	}
	
	
	/*---------------------------------------------------
	** BEGIN: xxx
	**---------------------------------------------------
	*/
	public void setVisible(boolean visible)
	{
		_guiInitTime = System.currentTimeMillis();
		super.setVisible(visible);
	}
	/*---------------------------------------------------
	** END: xxx
	**---------------------------------------------------
	*/

	/*---------------------------------------------------
	** BEGIN: component initialization
	**---------------------------------------------------
	*/
	protected void initComponents() 
	{
		setTitle("Asemon");
		ImageIcon icon = SwingUtils.readImageIcon(Version.class, "images/asemon_icon.gif");
		if (icon != null)
			setIconImage(icon.getImage());

		
		//-------------------------------------------------------------------------
		// HARDCODE a STOP date when this "DEVELOPMENT VERSION" will STOP working
		//-------------------------------------------------------------------------
		if (Version.IS_DEVELOPMENT_VERSION)
		{
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			setTitle("Asemon - This TEMPORARY DEVELOPMENT VERSION will NOT work after '"+df.format(Version.DEV_VERSION_EXPIRE_DATE)+"'.");
		}

		
		JPanel contentPane = (JPanel) this.getContentPane();
		contentPane.setLayout(new BorderLayout());

		//--------------------------
		// MENU - Icons
		_connect_mi            .setIcon(SwingUtils.readImageIcon(Version.class, "images/connect16.gif"));
		_disconnect_mi         .setIcon(SwingUtils.readImageIcon(Version.class, "images/disconnect16.gif"));
		_exit_mi               .setIcon(SwingUtils.readImageIcon(Version.class, "images/close.gif"));

		_logView_mi            .setIcon(SwingUtils.readImageIcon(Version.class, "images/log_viewer.gif"));
		_offlineSessionsView_mi.setIcon(SwingUtils.readImageIcon(Version.class, "images/offline_sessions_view.png"));
		_refreshRate_mi        .setIcon(SwingUtils.readImageIcon(Version.class, "images/refresh_rate.png"));
		_aseMonConf_mi         .setIcon(SwingUtils.readImageIcon(Version.class, "images/config_ase_mon.png"));
		_tcpSettingsConf_mi    .setIcon(SwingUtils.readImageIcon(Version.class, "images/tcp_settings_conf.png"));
		_graphs_m              .setIcon(SwingUtils.readImageIcon(Version.class, "images/summary_tab.png"));
		
		_captureSql_mi         .setIcon(SwingUtils.readImageIcon(Version.class, "images/capture_sql_tool.gif"));
		_sqlQuery_mi           .setIcon(SwingUtils.readImageIcon(Version.class, "images/sql_query_window.png"));
		_lockTool_mi           .setIcon(SwingUtils.readImageIcon(Version.class, "images/locktool16.gif"));
		_createOffline_mi      .setIcon(SwingUtils.readImageIcon(Version.class, "images/pcs_write_16.png"));
		_wizardCrUdCm_mi       .setIcon(SwingUtils.readImageIcon(Version.class, "images/ud_counter_activity.png"));
//		_doGc_mi               .setIcon(SwingUtils.readImageIcon(Version.class, "images/xxx.gif"));
		
		_about_mi              .setIcon(SwingUtils.readImageIcon(Version.class, "images/about.png"));

		
		//--------------------------
		// MENU - composition
		setJMenuBar(_main_mb);

		_main_mb.add(_file_m);
		_main_mb.add(_view_m);
		_main_mb.add(_tools_m);
		_main_mb.add(_help_m);

		_file_m.add(_connect_mi);
		_file_m.add(_disconnect_mi);
		_file_m.add(_exit_mi);

		_view_m.add(_logView_mi);
		_view_m.add(_offlineSessionsView_mi);
		_view_m.add(_refreshRate_mi);
		_view_m.add(_aseMonConf_mi);
		_view_m.add(_tcpSettingsConf_mi);
		_view_m.add(_graphs_m);
		
		_tools_m.add(_captureSql_mi);
		_tools_m.add(_sqlQuery_mi);
		_tools_m.add(_lockTool_mi);
		_tools_m.add(_createOffline_mi);
		_tools_m.add(_wizardCrUdCm_mi);
		_tools_m.add(_doGc_mi);

		_help_m.add(_about_mi);

		//--------------------------
		// MENU - Actions
		_connect_mi            .setActionCommand(ACTION_CONNECT);
		_disconnect_mi         .setActionCommand(ACTION_DISCONNECT);
		_exit_mi               .setActionCommand(ACTION_EXIT);

		_logView_mi            .setActionCommand(ACTION_OPEN_LOG_VIEW);
		_offlineSessionsView_mi.setActionCommand(ACTION_OPEN_OFFLINE_SESSION_VIEW);
		_refreshRate_mi        .setActionCommand(ACTION_OPEN_REFRESH_RATE);
		_aseMonConf_mi         .setActionCommand(ACTION_OPEN_ASE_CONFIG_MON);
		_tcpSettingsConf_mi    .setActionCommand(ACTION_OPEN_TCP_PANEL_CONFIG);

		_captureSql_mi         .setActionCommand(ACTION_OPEN_CAPTURE_SQL);
		_sqlQuery_mi           .setActionCommand(ACTION_OPEN_SQL_QUERY_WIN);
		_lockTool_mi           .setActionCommand(ACTION_OPEN_LOCK_TOOL);
		_createOffline_mi      .setActionCommand(ACTION_OPEN_WIZARD_OFFLINE);
		_wizardCrUdCm_mi       .setActionCommand(ACTION_OPEN_WIZARD_UDCM);
		_doGc_mi               .setActionCommand(ACTION_GARBAGE_COLLECT);
		
		_about_mi              .setActionCommand(ACTION_OPEN_ABOUT);

		// And the action listener
		_connect_mi            .addActionListener(this);
		_disconnect_mi         .addActionListener(this);
		_exit_mi               .addActionListener(this);

		_logView_mi            .addActionListener(this);
		_offlineSessionsView_mi.addActionListener(this);
		_refreshRate_mi        .addActionListener(this);
		_aseMonConf_mi         .addActionListener(this);
		_tcpSettingsConf_mi    .addActionListener(this);

		_captureSql_mi         .addActionListener(this);
		_sqlQuery_mi           .addActionListener(this);
		_lockTool_mi           .addActionListener(this);
		_createOffline_mi      .addActionListener(this);
		_wizardCrUdCm_mi       .addActionListener(this);
		_doGc_mi               .addActionListener(this);
		
		_about_mi              .addActionListener(this);
		
		//--------------------------
		// TOOLBAR
		JButton connect    = SwingUtils.makeToolbarButton(Version.class, "connect16.gif",    ACTION_CONNECT,    this, "Connect to a ASE",         "Connect");
		JButton disConnect = SwingUtils.makeToolbarButton(Version.class, "disconnect16.gif", ACTION_DISCONNECT, this, "Close the ASE Connection", "Disconnect");

		JButton screenshot  = SwingUtils.makeToolbarButton(Version.class, "screenshot.png",   ACTION_SCREENSHOT,     this, "Take a screenshot of the application", "Screenshot");
		_samplePauseTb_but  = SwingUtils.makeToolbarButton(Version.class, "sample_pause.png", ACTION_SAMPLING_PAUSE, this, "Pause ALL sampling activity",          "Pause");
		_samplePlayTb_but   = SwingUtils.makeToolbarButton(Version.class, "sample_play.png",  ACTION_SAMPLING_PLAY,  this, "Continue to sample...",          "Pause");
		_samplePlayTb_but.setVisible(false);


		_viewStorage_chk .setIcon(        SwingUtils.readImageIcon(Version.class, "images/read_storage.png"));
		_viewStorage_chk .setSelectedIcon(SwingUtils.readImageIcon(Version.class, "images/read_storage_minus.png"));
		_viewStorage_chk .setToolTipText("View Counters that are stored in memory for a while...");

		_toolbar.setLayout(new MigLayout("insets 0 0 0 3", "", "")); // insets Top Left Bottom Right
		_toolbar.add(connect);
		_toolbar.add(disConnect);
		_toolbar.add(screenshot);
		_toolbar.add(_samplePauseTb_but, "hidemode 3");
		_toolbar.add(_samplePlayTb_but,  "hidemode 3");

		_workspace_cbx.addItem("ASE");
//		_workspace_cbx.addItem("ASE-CE");
		_workspace_cbx.addItem("IQ");
		_workspace_cbx.addItem("RS");
		_workspace_cbx.setToolTipText("NOT SUPPORTED NOW: What workspace would you like to use.");
		
		_viewStorage_chk.setActionCommand(ACTION_VIEW_STORAGE);
		_viewStorage_chk.addActionListener(this);

//		_toolbar.addSeparator();
		_toolbar.add(new JSeparator(SwingConstants.VERTICAL), "grow");
		_toolbar.add(_viewStorage_chk);
		_toolbar.add(_readSlider,    "push, grow, hidemode 3");
		_toolbar.add(_offlineSlider, "push, grow, hidemode 3");
//		_toolbar.add(_workspace_cbx, "push, right");
//		_toolbar.add(_workspace_cbx, "split, push, right, growpriox 0, shrink 0");

		// ReadSlider
		_readTsWatermark = new ReadTsWatermark(_readSlider, "empty");
		_readSlider.setVisible(_viewStorage_chk.isSelected());
		
		_readSlider.setMinimum(0);
		_readSlider.setMaximum(0);
		_readSlider.setPaintLabels(true);
		_readSlider.setPaintTicks(true);
		_readSlider.setPaintTrack(true);
		_readSlider.setMajorTickSpacing(10);
		_readSlider.setMinorTickSpacing(1);

//		_readSlider.setLabelTable(_readSlider.createStandardLabels(10));
//		_readSlider.setLabelTable(dict);
		_readSlider.addChangeListener(this);
		
		// OfflineSlider
		_offlineTsWatermark = new ReadTsWatermark(_offlineSlider, "empty");
		_offlineSlider.setVisible(false);
		
		_offlineSlider.setMinimum(0);
		_offlineSlider.setMaximum(0);
		_offlineSlider.setPaintLabels(true);
		_offlineSlider.setPaintTicks(true);
		_offlineSlider.setPaintTrack(true);
		_offlineSlider.setMajorTickSpacing(10);
		_offlineSlider.setMinorTickSpacing(1);

//		_offlineSlider.setLabelTable(_readSlider.createStandardLabels(10));
//		_offlineSlider.setLabelTable(dict);
		_offlineSlider.addChangeListener(this);

		//--------------------------
		// STATUS PANEL
		_blockAlert_but       .setToolTipText("<html>You have SPID(s) that <b>blocks</b> other SPID(s) from working. Click here to the 'Blocking' tab to find out more.</html>");
		_refreshNow_but       .setToolTipText("Abort the \"sleep for next refresh\" and make a new refresh of data NOW.");
		_samplePause_but      .setToolTipText("Pause ALL sampling activity.");
		_samplePlay_but       .setToolTipText("Continue to sample counters again.");
		_gcNow_but            .setToolTipText("Do Java Garbage Collection.");
		_statusStatus         .setToolTipText("What are we doing or waiting for.");
		_statusServerName     .setToolTipText("<html>The local name of the ASE Server, as named in the interfaces or sql.ini file.<BR>Also show the HOST:PORT, which we are connected to.</html>");
		_statusServerListeners.setToolTipText("<html>This is the network listeners the ASE Servers listens to.<BR>This is good to see if we connect via SSH tunnels or other proxy functionality.<br>The format is netlibdriver: HOST PORT, next entry...</html>");
		_statusMemory         .setToolTipText("How much memory does the JVM consume for the moment.");

		// Blocking LOCK alert butt
		_blockAlert_but.setIcon(SwingUtils.readImageIcon(Version.class, "images/block_lock_alert.png"));
		_blockAlert_but.setText("");
		_blockAlert_but.setContentAreaFilled(false);
//		_blockAlert_but.setMargin( new Insets(3,3,3,3) );
		_blockAlert_but.setMargin( new Insets(0,0,0,0) );
		_blockAlert_but.addActionListener(this);
		_blockAlert_but.setActionCommand(ACTION_GOTO_BLOCKED_TAB);
		_blockAlert_but.setVisible(false);

		
		// refresh now butt
		_refreshNow_but.setIcon(SwingUtils.readImageIcon(Version.class, "images/refresh_now.png"));
		_refreshNow_but.setText(null);
		_refreshNow_but.setContentAreaFilled(false);
//		_refreshNow_but.setMargin( new Insets(3,3,3,3) );
		_refreshNow_but.setMargin( new Insets(0,0,0,0) );
		_refreshNow_but.addActionListener(this);
		_refreshNow_but.setActionCommand(ACTION_REFRESH_NOW);

		_samplePause_but.setIcon(SwingUtils.readImageIcon(Version.class, "images/sample_pause.png"));
		_samplePause_but.setText(null);
		_samplePause_but.setContentAreaFilled(false);
//		_samplePause_but.setMargin( new Insets(3,3,3,3) );
		_samplePause_but.setMargin( new Insets(0,0,0,0) );
		_samplePause_but.addActionListener(this);
		_samplePause_but.setActionCommand(ACTION_SAMPLING_PAUSE);
		_samplePause_but.setVisible(true);

		_samplePlay_but.setIcon(SwingUtils.readImageIcon(Version.class, "images/sample_play.png"));
		_samplePlay_but.setText(null);
		_samplePlay_but.setContentAreaFilled(false);
//		_samplePlay_but.setMargin( new Insets(3,3,3,3) );
		_samplePlay_but.setMargin( new Insets(0,0,0,0) );
		_samplePlay_but.addActionListener(this);
		_samplePlay_but.setActionCommand(ACTION_SAMPLING_PLAY);
		_samplePlay_but.setVisible(false);

				
		// GC now butt
		_gcNow_but.setIcon(SwingUtils.readImageIcon(Version.class, "images/gc_now.png"));
		_gcNow_but.setText(null);
		_gcNow_but.setContentAreaFilled(false);
//		_gcNow_but.setMargin( new Insets(3,3,3,3) );
		_gcNow_but.setMargin( new Insets(0,0,0,0) );
		_gcNow_but.addActionListener(this);
		_gcNow_but.setActionCommand(ACTION_GARBAGE_COLLECT);

//		_statusPanel.setLayout(new MigLayout("insets 0 10 0 10")); // T L B R
		_statusPanel.setLayout(new MigLayout("insets 0 0 0 10")); // T L B R
		_statusPanel.add(_refreshNow_but,                         "");
		_statusPanel.add(_samplePause_but,                        "hidemode 3");
		_statusPanel.add(_samplePlay_but,                         "hidemode 3");
		_statusPanel.add(_blockAlert_but,                         "hidemode 3");
		_statusPanel.add(_statusStatus,                           "gaptop 3, gapbottom 5, grow, push");
		_statusPanel.add(new JSeparator(SwingConstants.VERTICAL), "grow");
		_statusPanel.add(_statusServerName,                       "right");
		_statusPanel.add(new JSeparator(SwingConstants.VERTICAL), "grow");
		_statusPanel.add(_statusServerListeners,                  "right");
		_statusPanel.add(new JSeparator(SwingConstants.VERTICAL), "grow");
		_statusPanel.add(_statusMemory,                           "right");
		_statusPanel.add(_gcNow_but,                              "right");

		
		//--------------------------
		// Layout
		contentPane.add(_toolbar,        BorderLayout.NORTH);
//		contentPane.add(_pcsNavigation,  BorderLayout.LINE_END);
		contentPane.add(_mainTabbedPane, BorderLayout.CENTER);
		contentPane.add(_statusPanel,    BorderLayout.SOUTH);

		
		//--------------------------
		// Tab
		_mainTabbedPane.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
		_mainTabbedPane.addChangeListener(this);

		
		//--------------------------
		// Add Summary TAB
		Icon summaryIcon = SwingUtils.readImageIcon(Version.class, "images/summary_tab.png");
		_mainTabbedPane.addTab("Summary", summaryIcon, _summaryPanel, "Trend Graphs");


		//--------------------------
		// add myself as a listener to the GuiLogAppender
		// Calling this would make GuiLogAppender, to register itself in log4j.
		if (GuiLogAppender.getTableModel() != null)
			GuiLogAppender.getTableModel().addTableModelListener(this);

		//--------------------------
		// Hide/show various components and menus 
		// for the default mode
		setDefaultMenuMode();

	
		//--------------------------
		// Setup what happens when the window is closing/exiting 
		// for the default mode
		//setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		//setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener( new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				saveProps();
				saveTcpProps();
				if (Asemon.getCounterCollector().isMonConnected())
				{
					int answer = AseMonitoringConfigDialog.onExit(_instance, Asemon.getCounterCollector().getMonConnection());

					// This means that a "prompt" was raised to ask for "disable monitoring"
					// AND the user pressed CANCEL button
					if (answer > 0)
						return;
				}
				terminateConnection();
				
				CheckForUpdates.sendCounterUsageInfoNoBlock();

//				dispose();
				System.exit(0);
			}
		});
	
	
	}
	/*---------------------------------------------------
	** END: component initialization
	**---------------------------------------------------
	*/

	private void setDefaultMenuMode()
	{
		// Monitor Server components
		_refreshRate_mi    .setEnabled(true);
		_aseMonConf_mi     .setEnabled(true);
		_tcpSettingsConf_mi.setEnabled(true);
		_graphs_m          .setEnabled(true);

		_captureSql_mi     .setEnabled(true);
		_sqlQuery_mi       .setEnabled(true);
		_lockTool_mi       .setEnabled(true);
		_createOffline_mi  .setEnabled(true);
		_wizardCrUdCm_mi   .setEnabled(true);

		// Offline components
		_offlineSessionsView_mi.setEnabled(false);
	}

	private void setOfflineMenuMode()
	{
		// Monitor Server components
		_refreshRate_mi    .setEnabled(false);
		_aseMonConf_mi     .setEnabled(false);
		_tcpSettingsConf_mi.setEnabled(true);
		_graphs_m          .setEnabled(false);

		_captureSql_mi     .setEnabled(false);
		_sqlQuery_mi       .setEnabled(false);
		_lockTool_mi       .setEnabled(false);
		_createOffline_mi  .setEnabled(false);
		_wizardCrUdCm_mi   .setEnabled(false);

		// Offline components
		_offlineSessionsView_mi.setEnabled(true);
	}

		
	/*---------------------------------------------------
	** BEGIN: implementing TableModelListener
	**---------------------------------------------------
	*/
	/** called when the lowView table is appended */
	public void tableChanged(TableModelEvent e) 
	{
		if ( _guiInitTime == 0 ) // setVisible has not ben called
			return;

		// Start to react to changes after X seconds after initialization.
//		if ( System.currentTimeMillis() - _guiInitTime < 5*1000 ) 
//			return;

		if (_logView == null)
		{
			_logView = new Log4jViewer(_instance);
		}

		// If no logView GUI exists create one and show it (for all types of LogLevels)
//		if (_logView == null)
//		{
//			_logView = new Log4jViewer(_instance);
//
//			if ( ! _logView.isVisible() )
//				_logView.setVisible(true);
//		}
//		else // If logView GUI exists, make it visible if LogLevel is "above" WARN
//		{
			LogLevel logLevel = _logView.getLogLevelForRow(e.getLastRow());
			
			// if loglevel is more severe than WARN, then show the logView window 
			// even if it's not visible for the moment
			if (logLevel != null && ! logLevel.encompasses(LogLevel.WARN))
			{
				if ( ! _logView.isVisible() )
					_logView.setVisible(true);
			}
//---- FROM LOG4J -----
//			112   /***
//			113    * Returns true if the level supplied is encompassed by this level.
//			114    * For example, LogLevel.SEVERE encompasses no other LogLevels and
//			115    * LogLevel.FINE encompasses all other LogLevels.  By definition,
//			116    * a LogLevel encompasses itself.
//			117    */
//			118   public boolean encompasses(LogLevel level) {
//			119     if (level.getPrecedence() <= getPrecedence()) {
//			120       return true;
//			121     }
//			122 
//			123     return false;
//			124   }
//---------------------
//		}
	}
	/*---------------------------------------------------
	** END: implementing TableModelListener
	**---------------------------------------------------
	*/

	/*---------------------------------------------------
	** BEGIN: implementing ActionListener
	**---------------------------------------------------
	*/
	public void actionPerformed(ActionEvent e)
	{
		Object source    = e.getSource();
		String actionCmd = e.getActionCommand();

		_logger.debug("ACTION '"+actionCmd+"'.");

		if (ACTION_CONNECT.equals(actionCmd))
			action_connect(e);

		if (ACTION_DISCONNECT.equals(actionCmd))
			action_disconnect(e);

		if (ACTION_EXIT.equals(actionCmd))
			action_exit(e);


		
		if (ACTION_OPEN_LOG_VIEW.equals(actionCmd))
			openLogViewer();

		if (ACTION_OPEN_OFFLINE_SESSION_VIEW.equals(actionCmd))
			openOfflineSessionView(true);

		if (ACTION_OPEN_REFRESH_RATE.equals(actionCmd))
			action_refreshRate(e);

		if (ACTION_OPEN_ASE_CONFIG_MON.equals(actionCmd))
			AseMonitoringConfigDialog.showDialog(this, Asemon.getCounterCollector().getMonConnection(), -1);

		if (ACTION_OPEN_TCP_PANEL_CONFIG.equals(actionCmd))
			TcpConfigDialog.showDialog(_instance);			

		if (ACTION_OPEN_CAPTURE_SQL.equals(actionCmd))
			new ProcessDetailFrame(-1);


		if (ACTION_OPEN_SQL_QUERY_WIN.equals(actionCmd))
		{
			try 
			{
				Connection conn = AseConnectionFactory.getConnection(null, Version.getAppName()+"-QueryWindow");
				QueryWindow qf = new QueryWindow(conn, true);
				qf.openTheWindow();
			}
			catch (Exception ex) 
			{
				JOptionPane.showMessageDialog(
					MainFrame.this, 
					"Problems open SQL Query Window\n" + ex.getMessage(),
					"Error", JOptionPane.ERROR_MESSAGE);
			}
		}

		if (ACTION_OPEN_LOCK_TOOL.equals(actionCmd))
		{
			// TO BE IMPLEMENTED
		}

		if (ACTION_OPEN_WIZARD_OFFLINE.equals(actionCmd))
		{
			new WizardOffline();
		}
		
		if (ACTION_OPEN_WIZARD_UDCM.equals(actionCmd))
		{
			new WizardUserDefinedCm();
		}
		
		if (ACTION_GARBAGE_COLLECT.equals(actionCmd))
		{
			System.gc();
			setStatus(MainFrame.ST_MEMORY);
		}

		if (ACTION_OPEN_ABOUT.equals(actionCmd))
			action_about(e);

		
		if (ACTION_VIEW_STORAGE.equals(actionCmd))
		{
			// Show the view
			if (isOfflineConnected())
			{
				openOfflineSessionView(false); // create it if it doesn't exist, but do not set it to visible
				_viewStorage_chk.setSelected(false); // do not flip images...
				
				return;
			}

			for (Iterator it = _TcpMap.values().iterator(); it.hasNext();)
			{
				TabularCntrPanel tcp = (TabularCntrPanel) it.next();
				_logger.trace("ACTION_VIEW_STORAGE: setTail("+_viewStorage_chk.isSelected()+"), '"+tcp.getPanelName()+"'.");

				if (_viewStorage_chk.isSelected())
				{
					InMemoryCounterHandler imch = InMemoryCounterHandler.getInstance();
					if (_currentPc == null && imch != null)
						_currentPc = imch.getRight(); // this is the last added

					CountersModel cm = null;
					if (imch != null)
						cm = _currentPc.getCm(tcp.getName());

					tcp.setDisplayCm(cm, false);
				}
				else
				{
					tcp.setDisplayCm(null, true);
				}
				_readSlider.setVisible(_viewStorage_chk.isSelected());
			}
		}

		if (ACTION_REFRESH_NOW.equals(actionCmd))
		{
			_logger.debug("called: ACTION_REFRESH_NOW");

			GetCounters getCnt = Asemon.getCounterCollector();
			if (getCnt != null)
				getCnt.doInterrupt();
		}

		if (ACTION_GOTO_BLOCKED_TAB.equals(actionCmd))
		{
			_logger.debug("called: ACTION_GOTO_BLOCKED_TAB");

			String toTabName = "Blocking";
			JTabbedPane tabPane = getTabbedPane();
			if (tabPane != null)
				tabPane.setSelectedIndex(tabPane.indexOfTab(toTabName));
		}


		if (ACTION_OUT_OF_MEMORY.equals(actionCmd))
		{
			_logger.debug("called: ACTION_OUT_OF_MEMORY");

			InMemoryCounterHandler imch = InMemoryCounterHandler.getInstance();
			imch.clear(true);

			System.gc();

			int maxConfigMemInMB = (int) Runtime.getRuntime().maxMemory() / 1024 / 1024;
			int mbLeftAfterGc = Memory.getMemoryLeftInMB();

			// OK, this is non-modal, but the OK button doesnt work, fix this later, and use the X on the window instead
			JDialog dialog = new JDialog(this, "out-of-memory", false); // Sets its owner but makes it non-modal 
			JOptionPane optionPane = new JOptionPane(
					"Sorry, out-of-memory. \n" +
					"\n" +
					"I have cleared the In Memory Counter History! \n" +
					"This will hopefully get us going again. \n" +
					"\n" +
					"Note: you can raise the memory parameter -Xmx###m in the "+Version.getAppName()+" start script.\n" +
					"Current max memory setting seems to be around "+maxConfigMemInMB+" MB.\n" +
					"After Garbage Collection, you now have "+mbLeftAfterGc+" free MB.", 
					JOptionPane.INFORMATION_MESSAGE);
			dialog.getContentPane().add(optionPane);
			dialog.pack();
			dialog.setVisible(true);

			setStatus(MainFrame.ST_MEMORY);

			// use a non-blocking window instead... modal=false (non-modal)
//			JOptionPane.showMessageDialog(this, 
//				"Sorry, out-of-memory. \n" +
//				"\n" +
//				"I have cleared the In Memory Counter History! \n" +
//				"This will hopefully get us going again. \n" +
//				"\n" +
//				"Note: you can raise the memory parameter -Xmx###m in the "+Version.getAppName()+" start script.\n" +
//				"Current max memory setting seems to be around "+maxConfigMemInMB+" MB.\n" +
//				"After Garbage Collection, you now have "+mbLeftAfterGc+" free MB.", 
//
//				"out-of-memory", 
//				JOptionPane.INFORMATION_MESSAGE);
		}

		if (ACTION_SCREENSHOT.equals(actionCmd))
		{
			String fileList = "";
			String extraInfo = Version.getAppName() + ", Version: "+ Version.getVersionStr();
			String main = Screenshot.windowScreenshot(this, null, "asemon.main", true, extraInfo);
			fileList += main;

			// LOOP all CounterModels, and check if they got any windows open, then screenshot that also
			Iterator iter = GetCounters.getCmList().iterator();
			while (iter.hasNext())
			{
				CountersModel cm = (CountersModel) iter.next();
				if (cm == null)
					continue;

				if ( cm.getTabPanel() != null)
				{
					JTabbedPane tp = MainFrame.getTabbedPane();
					if (tp instanceof GTabbedPane)
					{
						GTabbedPane gtp = (GTabbedPane) tp;
						if (gtp.isTabUnDocked(cm.getDisplayName()))
						{
							JFrame frame = gtp.getTabUnDockedFrame(cm.getDisplayName());
							String fn = Screenshot.windowScreenshot(frame, null, "asemon."+cm.getName(), true, extraInfo);
							fileList += "\n"+fn;
						}
					}
				}
			}

			// TODO: grab "all" other windows as well: SQL Windows, Capture SQL...
			
			JOptionPane.showMessageDialog(this, 
					"Screenshot was captured. \n" +
					"\n" +
					"The file(s) was stored as: \n" +
					fileList + "\n" +
					"\n" +
//					"Note: If you want to store screen captures in a different directory.\n" +
//					"bla bla bla.\n" +
					"",

					"Screenshot captured", 
					JOptionPane.INFORMATION_MESSAGE);
		}

		if (ACTION_SAMPLING_PAUSE.equals(actionCmd))
		{
			_isSamplingPaused = true;
			_samplePause_but  .setVisible( ! _isSamplingPaused );
			_samplePauseTb_but.setVisible( ! _isSamplingPaused );
			_samplePlay_but   .setVisible(   _isSamplingPaused );
			_samplePlayTb_but .setVisible(   _isSamplingPaused );

			GetCounters.getInstance().doInterrupt();
		}

		if (ACTION_SAMPLING_PLAY.equals(actionCmd))
		{
			_isSamplingPaused = false;
			_samplePause_but  .setVisible( ! _isSamplingPaused );
			_samplePauseTb_but.setVisible( ! _isSamplingPaused );
			_samplePlay_but   .setVisible(   _isSamplingPaused );
			_samplePlayTb_but .setVisible(   _isSamplingPaused );

			GetCounters.getInstance().doInterrupt();
		}
	}

	/*---------------------------------------------------
	** END: implementing ActionListener
	**---------------------------------------------------
	*/

	
	/*---------------------------------------------------
	** BEGIN: implementing ChangeListener
	**---------------------------------------------------
	*/
	public void stateChanged(ChangeEvent e)
	{
		Object source = e.getSource();
		
		//------------------------------------------------------
		// TabPane changes
		//------------------------------------------------------
		if (source.equals(_mainTabbedPane))
		{
			String currentTab = _mainTabbedPane.getTitleAt(_mainTabbedPane.getSelectedIndex());
			if (_logger.isDebugEnabled())
			{
				_logger.debug("state changed for pannel named '" + currentTab + "'.");
			}
	
	
			// LOOP all TabularCntrPanel to check which is the current one...
			// if it should be done
			Iterator iter = _TcpMap.values().iterator();
			while (iter.hasNext())
			{
				TabularCntrPanel tcp = (TabularCntrPanel) iter.next();
	
				if (currentTab.equals(tcp.getPanelName()))
				{
					_currentPanel = tcp;
					_currentPanel.tabSelected();
					break; // no need to continue
				}
			}
	
			if (_currentPanel != null)
			{
				_currentPanel.setWatermark();
			}
	
			if ((_currentPanel != null) && (Asemon.getCounterCollector().getMonConnection() != null) && (_currentPanel.getCm() != null) && (!_currentPanel.getCm().isDataInitialized()))
			{
				GetCounters.setWaitEvent("data to be initialization in the panel '"+_currentPanel.getPanelName()+"'...");
				//statusFld.setText("Waiting for data to be initialization in the panel '"+currentPanel.getPanelName()+"'...");
			}
		} // end: _mainTabbedPane
		
		//------------------------------------------------------
		// listen for changes on InMemoryCounterHandler
		// So this is called when new information is added to the InMemory Storage
		//------------------------------------------------------
		if (source instanceof InMemoryCounterHandler)
		{
			_logger.trace("MainFrame.stateChanged().InMemoryCounterHandler");
			if (_viewStorage_chk.isSelected())
			{
			}
			InMemoryCounterHandler imch = InMemoryCounterHandler.getInstance();
			if (imch != null)
			{
				PersistContainer pcRight = imch.getRight();
				PersistContainer pcLeft  = imch.getLeft();

				if (pcRight == null) return;
				if (pcLeft  == null) return;

				int size = imch.getSize()-1;
				_readSlider.setMaximum(size);
				Dictionary dict = new Hashtable();
				String leftStr  = SimpleDateFormat.getTimeInstance().format(pcLeft .getSampleTime());
				String rightStr = SimpleDateFormat.getTimeInstance().format(pcRight.getSampleTime());
				
				dict.put(new Integer(0),    new JLabel(leftStr));
				dict.put(new Integer(size), new JLabel(rightStr));
				_readSlider.setLabelTable(dict);

//				_readSlider.repaint();
				_logger.trace("MainFrame.stateChanged().InMemoryCounterHandler: dict = "+dict);
				
				//TODO: set slider to "correct" place, if we are not already at that position.
				int imchIndex = imch.indexOf(_currentPc);
				if (imchIndex >= 0)
				{
					int sliderIndex = _readSlider.getValue();
					if (sliderIndex != imchIndex)
					{
						_logger.trace("MOVING: setting slider to '"+imchIndex+"', from '"+sliderIndex+"'.");
						_readSlider.setValue(imchIndex);
					}
				}
				else // Not found: probably aged out, should we go to oldest entry
				{
					_logger.trace("AGED-OUT: SETTING SLIDER TO MAX "+(imch.getSize()-1) );
					_readSlider.setValue(imch.getSize()-1);
				}
			}
		} // end: instanceof InMemoryCounterHandler
		
		//------------------------------------------------------
		// The Slider is changed (or information to it)
		//------------------------------------------------------
		if (source.equals(_readSlider))
		{
			if (_logger.isTraceEnabled())
			{
				_logger.trace("ReadSlider: getValueIsAdjusting="+_readSlider.getValueIsAdjusting()
					+", value="+_readSlider.getValue()
					+", min="+_readSlider.getMinimum()
					+", max="+_readSlider.getMaximum()
					+", CangeEvent="+e
				);
			}

			if (_viewStorage_chk.isSelected())
			{
				InMemoryCounterHandler imch = InMemoryCounterHandler.getInstance();
				if (imch != null)
				{
//					int listPos = _readSlider.getMaximum() - _readSlider.getValue(); 
					int listPos = _readSlider.getValue(); 
					Timestamp ts = imch.getTs(listPos);
					_readTsWatermark.setWatermarkText(ts==null ? "-" : ts.toString());

					// When we have stopped moving the "slider"
					if ( ! _readSlider.getValueIsAdjusting() )
					{
						// Start/restart the timer, and show the selected value when the timer expires
						// timer expire call: readSliderMoveToCurrentTs()
						if (_readSelectionTimer.isRunning())
							_readSelectionTimer.restart();
						else
							_readSelectionTimer.start();
						
//						PersistContainer pc = imch.get(listPos); 
//
//						if (_currentPc != null && !_currentPc.equals(pc))
//						{
//							_currentPc = pc;
//							if (_currentPc != null)
//							{
//								for (Iterator it = _TcpMap.values().iterator(); it.hasNext();)
//								{
//									TabularCntrPanel tcp = (TabularCntrPanel) it.next();
//									CountersModel cm = _currentPc.getCm(tcp.getName());
//									tcp.setDisplayCm(cm, false);
//								}
//							}
//						}
//						_currentPc = pc;
					}
//					else
//					{
//						PersistContainer pc = imch.get(listPos); 
//						if (pc != null)
//						{
//							for (Iterator it = _TcpMap.values().iterator(); it.hasNext();)
//							{
//								TabularCntrPanel tcp = (TabularCntrPanel) it.next();
//								CountersModel cm = _currentPc.getCm(tcp.getName());
//								tcp.setGraphTimeLineMarker(cm);
//							}
//						}
//					}
				}
			}
		} // end: _readSlider

		//------------------------------------------------------
		// The OFFLINE Slider is changed (or information to it)
		//------------------------------------------------------
		if (source.equals(_offlineSlider))
		{
			if (_logger.isTraceEnabled())
			{
				_logger.trace("OfflineSlider: getValueIsAdjusting="+_offlineSlider.getValueIsAdjusting()
					+", value="+_offlineSlider.getValue()
					+", min="+_offlineSlider.getMinimum()
					+", max="+_offlineSlider.getMaximum()
					+", CangeEvent="+e
				);
			}

			int listPos = _offlineSlider.getValue(); 
			Timestamp ts = (Timestamp)_offlineTsList.get(listPos);
			_offlineTsWatermark.setWatermarkText(ts==null ? "-" : ts.toString());

			// When we have stopped moving the "slider"
			if ( ! _offlineSlider.getValueIsAdjusting() )
			{
				if (ts != null)
				{
					// Start/restart the timer, and show the selected value when the timer expires
					// timer expire call: offlineSliderMoveToCurrentTs()
					if (_offlineSelectionTimer.isRunning())
						_offlineSelectionTimer.restart();
					else
						_offlineSelectionTimer.start();
//					PersistReader reader = PersistReader.getInstance();
//					if (reader == null)
//						return;
//
//					reader.setCurrentSampleTime(ts);
//					reader.loadSummaryCm(ts);
//
//					// set current location in the view...
//					if (_offlineView != null)
//						_offlineView.setCurrentSampleTimeView(ts);
//
//					for (Iterator it = _TcpMap.values().iterator(); it.hasNext();)
//					{
//						TabularCntrPanel tcp = (TabularCntrPanel) it.next();
//
//						// Get the OFFLINE version of the CM
//						tcp.setOfflineSampleTime(ts);
////						CountersModel cm = _currentPc.getCm(tcp.getName());
////						tcp.setDisplayCm(cm, false);
//						
//						if (_mainTabbedPane.isTabUnDocked(tcp.getPanelName()))
//							tcp.tabSelected();
//
//						// notify that the current tab to "re-read" it's data...
//						if (_currentPanel != null)
//							_currentPanel.tabSelected();
//
//						// Position the timeline marker in graphs
//						CountersModel cm = tcp.getCm();
//						if (cm.hasTrendGraph())
//						{
//							Map tgm = cm.getTrendGraphs();
//							for (Iterator it2 = tgm.values().iterator(); it2.hasNext();)
//							{
//								TrendGraph tg = (TrendGraph) it2.next();
//								tg.setTimeLineMarker(ts);
//							}
//						}
//					}
				}
			}
		} // end: _offlineSlider

		//------------------------------------------------------
		// listen for changes on PersistReader
		// So this is called when new sessions has been found in the PersistReader 
		// if the background reader thread has been enabled.
		//------------------------------------------------------
		if (source instanceof PersistReader)
		{
			_logger.trace("MainFrame.stateChanged().PersistReader");
//			if (_viewStorage_chk.isSelected())
//			{
//			}
			PersistReader reader = PersistReader.getInstance();
			if (reader != null)
			{
				// TODO: add info to: _offlineSlider
			}
		} // end: PersistReader
	}
	/*---------------------------------------------------
	** END: implementing ChangeListener
	**---------------------------------------------------
	*/
	/** Differed action on the read inMemory slider when it has not been received input for X ms */
	private void readSliderMoveToCurrentTs()
	{
		InMemoryCounterHandler imch = InMemoryCounterHandler.getInstance();
		if (imch == null)
			return;

		int listPos = _readSlider.getValue(); 
		Timestamp ts = imch.getTs(listPos);
		_readTsWatermark.setWatermarkText(ts==null ? "-" : ts.toString());

		if (ts == null)
			return;

		PersistContainer pc = imch.get(listPos); 

		if (_currentPc != null && !_currentPc.equals(pc))
		{
			_currentPc = pc;
			if (_currentPc != null)
			{
				for (Iterator it = _TcpMap.values().iterator(); it.hasNext();)
				{
					TabularCntrPanel tcp = (TabularCntrPanel) it.next();
					CountersModel cm = _currentPc.getCm(tcp.getName());
					tcp.setDisplayCm(cm, false);
				}
			}
		}
		_currentPc = pc;
	}

	/** Differed action on the offline slider when it has not been received input for X ms */
	private void offlineSliderMoveToCurrentTs()
	{
		int listPos = _offlineSlider.getValue(); 
		Timestamp ts = (Timestamp)_offlineTsList.get(listPos);
		_offlineTsWatermark.setWatermarkText(ts==null ? "-" : ts.toString());

		if (ts == null)
			return;

		PersistReader reader = PersistReader.getInstance();
		if (reader == null)
			return;

		reader.setCurrentSampleTime(ts);
		reader.loadSummaryCm(ts);

		// set current location in the view...
		if (_offlineView != null)
			_offlineView.setCurrentSampleTimeView(ts);

		for (Iterator it = _TcpMap.values().iterator(); it.hasNext();)
		{
			TabularCntrPanel tcp = (TabularCntrPanel) it.next();

			// Get the OFFLINE version of the CM
			tcp.setOfflineSampleTime(ts);
//			CountersModel cm = _currentPc.getCm(tcp.getName());
//			tcp.setDisplayCm(cm, false);
			
			if (_mainTabbedPane.isTabUnDocked(tcp.getPanelName()))
				tcp.tabSelected();

			// notify that the current tab to "re-read" it's data...
			if (_currentPanel != null)
				_currentPanel.tabSelected();

			// Position the timeline marker in graphs
			CountersModel cm = tcp.getCm();
			if (cm.hasTrendGraph())
			{
				Map tgm = cm.getTrendGraphs();
				for (Iterator it2 = tgm.values().iterator(); it2.hasNext();)
				{
					TrendGraph tg = (TrendGraph) it2.next();
					tg.setTimeLineMarker(ts);
				}
			}
		}
	}

	
	
	/*---------------------------------------------------
	** BEGIN: Helper methods for actions
	**---------------------------------------------------
	*/
	public void action_connect(ActionEvent e)
	{
		Object source = e.getSource();
		String action = e.getActionCommand();

		if (Asemon.getCounterCollector().isMonConnected())
		{
			SwingUtils.showInfoMessage(this, "ASE - connect", "Connection already opened, Please close the connection first...");
			return;
		}

		// Create a new dialog Window
		ConnectionDialog connDialog = new ConnectionDialog(this);
		if (source instanceof GetCountersGui)
		{
			if ( ConnectionDialog.CONF_OPTION_CONNECT_ON_STARTUP.equals(action) )
			{
				connDialog.actionPerformed(new ActionEvent(this, 0, ConnectionDialog.ACTION_OK));
//				connDialog.doClickOkButton();
			}
		}
		else // Show the dialog and wait for response
		{
			connDialog.setVisible(true);
			connDialog.dispose();
		}

		// Get what was connected to...
		int connType = connDialog.getConnectionType();

		if ( connType == ConnectionDialog.CANCEL)
			return;
		
		if ( connType == ConnectionDialog.ASE_CONN)
		{
			Asemon.getCounterCollector().setMonConnection( connDialog.getAseConn() );

//			if (_conn != null)
			if (Asemon.getCounterCollector().isMonConnected())
			{
//				_summaryPanel.setLocalServerName(AseConnectionFactory.getServer());
				setStatus(ST_CONNECT);

				// Initilize the MonTablesDictionary
				// This will serv as a dictionary for ToolTip
				if ( ! MonTablesDictionary.getInstance().isInitialized() )
				{
//System.out.println(">>>>> MonTablesDictionary.init");
					MonTablesDictionary.getInstance().initialize(Asemon.getCounterCollector().getMonConnection());
					GetCounters.initExtraMonTablesDictionary();
//System.out.println("<<<<< MonTablesDictionary.init");
				}

				GetCounters getCnt = Asemon.getCounterCollector();
				if (getCnt != null)
				{
					getCnt.enableRefresh();
				}
				
				CheckForUpdates.sendConnectInfoNoBlock();
			}
		}

		if ( connType == ConnectionDialog.OFFLINE_CONN)
		{
			_summaryPanel.setLocalServerName("Offline-read");
			setStatus(ST_OFFLINE_CONNECT);
			setOfflineMenuMode();

			setOfflineConnection( connDialog.getOfflineConn() );
			if (getOfflineConnection() != null)
			{
				if ( ! PersistReader.hasInstance() )
				{
					PersistReader reader = new PersistReader(getOfflineConnection());
					PersistReader.setInstance(reader);
				}
//				_offlineSlider.setVisible(true);

				openOfflineSessionView(true);
				
				// TODO: start the reader thread & register as a listener
//				PersistReader _reader = new PersistReader();
//				_reader.setConnection(getOfflineConnection());
//				setStatus(ST_OFFLINE_CONNECT, "Reading sessions from the offline storage...");
//				_reader.refreshSessions();
//				
//				// Register listeners
//				_reader.addChangeListener(MainFrame.getInstance());
//				_reader.addChangeListener(_offlineView);
//
//				// Start it
//				_reader.start();
			}
		}
	}


	private void action_disconnect(ActionEvent e)
	{
		AseConnectionFactory.reset();
		terminateConnection();

		if (PersistentCounterHandler.hasInstance())
		{
			PersistentCounterHandler.getInstance().stop();
		}
	}


	private void action_exit(ActionEvent e)
	{
		// notify SWING that it's time to close...
		dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
//		setVisible(false);
//		System.exit(0);
	}


	private void action_refreshRate(ActionEvent e)
	{
		String key1 = "Refresh Rate";
		String key2 = "Refresh Rate (no-gui)";

		LinkedHashMap in = new LinkedHashMap();
		in.put(key1, Integer.toString(_refreshInterval));
		in.put(key2, Integer.toString(_refreshNoGuiInterval));

		Map results = ParameterDialog.showParameterDialog(this, "Refresh Interval", in);

		if (results != null)
		{
			_refreshInterval      = Integer.parseInt((String) results.get(key1));
			_refreshNoGuiInterval = Integer.parseInt((String) results.get(key2));

			saveProps();
		}
	}

	private void action_about(ActionEvent e)
	{
		AboutBox dlg = new AboutBox(this);
		Dimension dlgSize = dlg.getPreferredSize();
		Dimension frmSize = getSize();
		Point loc = getLocation();
		dlg.setLocation((frmSize.width - dlgSize.width) / 2 + loc.x, (frmSize.height - dlgSize.height) / 2 + loc.y);
		dlg.setModal(true);
		dlg.pack();
		//dlg.show();
		dlg.setVisible(true);
	}


	// Overridden so we can exit when window is closed
//	protected void processWindowEvent(WindowEvent e)
//	{
//		super.processWindowEvent(e);
//		if (e.getID() == WindowEvent.WINDOW_CLOSING)
//		{
//			action_exit(null);
//		}
//	}

	/*---------------------------------------------------
	** END: Helper methods for actions
	**---------------------------------------------------
	*/




	/*---------------------------------------------------
	** BEGIN: public methods
	**---------------------------------------------------
	*/
	private List _offlineTsList = new ArrayList();

	/**
	 * Get current Timestamp for the slider position
	 * @return Timestamp where the slider is positioned at or null if (_offlineSlider is not visible or _readSlider is not visible)
	 */
	public Timestamp getCurrentSliderTs()
	{
		if (_offlineSlider.isVisible())
		{
			int listPos = _offlineSlider.getValue(); 
			return (Timestamp)_offlineTsList.get(listPos);
		}
		if (_readSlider.isVisible())
		{
			InMemoryCounterHandler imch = InMemoryCounterHandler.getInstance();
			if (imch != null)
			{
				int listPos = _readSlider.getValue(); 
				return imch.getTs(listPos);
			}
		}
		return null;
	}
	/**
	 * reset the offline timeline slider
	 */
	public void resetOfflineSlider()
	{
		_offlineTsList = new ArrayList();
	}
	/**
	 * Add a Timestamp to the offline slider
	 */
	public void addOfflineSliderEntry(Timestamp ts)
	{
		_offlineTsList.add(ts);
		
		int size = _offlineTsList.size()-1;
		_offlineSlider.setMaximum(size);
		
		Timestamp left  = (Timestamp)_offlineTsList.get(0);
		Timestamp right = (Timestamp)_offlineTsList.get(size);

		Dictionary dict = new Hashtable();
		String leftStr  = SimpleDateFormat.getTimeInstance().format(left);
		String rightStr = SimpleDateFormat.getTimeInstance().format(right);
		
		dict.put(new Integer(0),    new JLabel(leftStr));
		dict.put(new Integer(size), new JLabel(rightStr));
		_offlineSlider.setLabelTable(dict);

		_offlineSlider.setVisible(true);
	}
	/**
	 * Add a List of Timestamp entries to the offline slider
	 */
	public void addOfflineSliderEntryList(List list)
	{
		if (list == null)
			_offlineTsList = new ArrayList(1);
		else
			_offlineTsList = list;
		
		int size = _offlineTsList.size()-1;
		_offlineSlider.setMaximum(size);
		
		Timestamp left  = (Timestamp)_offlineTsList.get(0);
		Timestamp right = (Timestamp)_offlineTsList.get(size);

		Dictionary dict = new Hashtable();
		String leftStr  = SimpleDateFormat.getTimeInstance().format(left);
		String rightStr = SimpleDateFormat.getTimeInstance().format(right);
		
		dict.put(new Integer(0),    new JLabel(leftStr));
		dict.put(new Integer(size), new JLabel(rightStr));
		_offlineSlider.setLabelTable(dict);

		_offlineSlider.setVisible(true);
	}
	
	
	/**
	 * Set what Time we should display in graphs and slider
	 * @param Time of a sample period, this could be the "head" period
	 * or any "sub" time samples. 
	 */
	public void setTimeLinePoint(long time)
	{
		setTimeLinePoint(new Timestamp(time));
	}
	/**
	 * Set what Time we should display in graphs and slider
	 * @param Time of a sample period, this could be the "head" period
	 * or any "sub" time samples. 
	 */
	public void setTimeLinePoint(Timestamp ts)
	{
		// If not in "timeline view" mode,  
		// click the buttom and the action will be invoked
		if ( ! _viewStorage_chk.isSelected() )
		{
			_viewStorage_chk.doClick();
			//_viewStorage_chk.setSelected(true);
		}

		//-----------------------------------
		// OFFLINE MODE
		//-----------------------------------
		if (isOfflineConnected())
		{
			if (_offlineTsList == null)
				return;

//			int sliderPos = _offlineTsList.indexOf(ts);
			int sliderPos = -1;

			// Find the "nearest" time in the TS List
			Timestamp smallerTs = null;
			Timestamp largerTs  = null;
			for (int i=0; i<_offlineTsList.size(); i++)
			{
				Timestamp indexTs = (Timestamp) _offlineTsList.get(i);
				if (indexTs == null)
					continue;

				if (_logger.isTraceEnabled()) _logger.trace("CHECK ts='"+ts+"', indexTs='"+indexTs+"'.");

				// IF input timestamp is SMALLER than first entry in the TS List, choose index=0
				if (i == 0 && ts.before(indexTs))
				{
					sliderPos = i;
					if (_logger.isTraceEnabled()) _logger.trace("--->>> TS is SMALLER than first index entry: sliderPos='"+sliderPos+"', ts='"+ts+"', indexTs='"+indexTs+"'.");
					break;
				}
				// IF input timestamp is EQUAL, choose index=currentIndex
				else if (ts.equals(indexTs))
				{
					sliderPos = i;
					if (_logger.isTraceEnabled()) _logger.trace("--->>> FOUND EXACT MATCH: ts='"+ts+"', indexTs='"+indexTs+"'.");
					break;
				}
				// No exact match, go and grab the CLOSEST timestamp in the TS List
				else
				{
					if (indexTs.before(ts))
					{
						sliderPos = i; // safety harness... choose smaller one...
						smallerTs = indexTs;
					}
					else if (largerTs == null)
					{
						largerTs = indexTs;
					}
					// Calculate the closest one
					if (smallerTs != null && largerTs != null)
					{
						long a = ts.getTime() - smallerTs.getTime();
						long b = largerTs.getTime() - ts.getTime();
						
						Timestamp closeEnough = ( a < b ) ? smallerTs : largerTs;
						sliderPos = _offlineTsList.indexOf(closeEnough);
						if (_logger.isTraceEnabled()) _logger.trace("--->>> CLOSE ENOUGH: ts='"+ts+"', indexTs='"+indexTs+"', sliderPos='"+sliderPos+"', closeEnough='"+closeEnough+"', smallerTs='"+smallerTs+"', largerTs='"+largerTs+"', a='"+a+"', b='"+b+"'.");
						break;
					}
				}
			}
				
			if (_logger.isTraceEnabled()) _logger.trace("_offlineSlider.setValue("+sliderPos+"); and NOTIFY LISTENERS");
			if (sliderPos >= 0)
			{
				// the sliders listeners will be notified and do the rest of the work.
				_offlineSlider.setValue(sliderPos);
			}
		}
		else
		//-----------------------------------
		// Connected to MONITOR SERVER
		//-----------------------------------
		{
			InMemoryCounterHandler imch = InMemoryCounterHandler.getInstance();
			if (imch != null)
			{
				_logger.trace("MainFrame.setTimeLinePoint(): Ts="+ts);
				
				int index = imch.indexOf(ts);
				if (index >= 0)
				{
					//int sliderPos = (_readSlider.getMaximum() - index) - 1; 
					int sliderPos = index;
	
					_logger.trace("MainFrame.setTimeLinePoint(): index="+index+", sliderPos="+sliderPos);
					// This will call the stateChanged
					// which does the rest of the work.
					_readSlider.setValue(sliderPos);
				}
			}
		}
	}

	/**
	 * Stop monitoring and disconnect from the database.
	 */
	public static void terminateConnection()
	{
		_logger.info("Starting a thread that will do disconnect after this sample session is finnished.");

		// Kick this of as it's own thread, otherwise the sleep below, might block the Swing Event Dispatcher Thread
		Thread terminateConnectionTask = new Thread()
		{
			public void run()
			{
				// Stop the counter refresh thread
				GetCounters getCnt = Asemon.getCounterCollector();
				if (getCnt != null)
				{
					// say to GetCounters to not continuing doing refresh of counter data
					getCnt.disableRefresh();
					while (true)
					{
						// wait until the current refresh loop has finished
						if (!getCnt.isRefreshingCounters())
						{
							_logger.info("Clearing components...");
							getCnt.clearComponents();
							break;
						}
						_logger.info("Waiting for GetCounters to stop before I can: Clearing components...");
						try { Thread.sleep(500); }
						catch (InterruptedException ignore) {}
					}
				}
		
				// Close all cm's
				Iterator iter = GetCounters.getCmList().iterator();
				while (iter.hasNext())
				{
					CountersModel cm = (CountersModel) iter.next();
					cm.close();
				}
				
				// Close the database connection
				Asemon.getCounterCollector().closeMonConnection();
		
				// Close the Offline database connection
				closeOfflineConnection();
				if (_offlineView != null)
				{
					_offlineView.setVisible(false);
					_offlineView.dispose();
					_offlineView = null;
				}
		
				// Update status fields
				setStatus(ST_DISCONNECT);

				_logger.info("The disconnect thread is ending.");
			}
		};
		terminateConnectionTask.setName("terminateConnectionTask");
		terminateConnectionTask.setDaemon(true);
		terminateConnectionTask.start();
	}

	/**
	 * Add a Component to the Tab
	 * @param tcp the component to add
	 */
	public static void addTcp(TabularCntrPanel tcp)
	{
		// We are probably in NO-GUI mode
		if ( ! Asemon.hasGUI() )
			return;

		_mainTabbedPane.addTab(tcp.getPanelName(), tcp.getIcon(), tcp, tcp.getCm().getDescription());
		_TcpMap.put(tcp.getPanelName(), tcp);
	}

	/**
	 */
	public static JTabbedPane getTabbedPane()
	{
		// We are probably in NO-GUI mode
		if ( ! Asemon.hasGUI() )
			return null;

		return _mainTabbedPane;
	}

	/**
	 * Add a "enable/disable" checkbox in the view menu
	 * @param mi The <code>JMenuItem</code> to add.
	 */
	public static void addGraphViewMenu(JMenuItem mi)
	{
		_graphs_m.add(mi);
	}

	/**
	 * Set the BLOCKING LOCK to be visible or not.
	 * @param visible
	 */
	public void setBlockingLocks(boolean visible, int blockCount)
	{
		if (blockCount > 0)
			_blockAlert_but.setText(blockCount+" Blocked SPID's, ");
		else
			_blockAlert_but.setText("");

		_blockAlert_but.setVisible(visible);
	}

//	/**
//	 * Do we have a connection to the database?
//	 * @return true or false
//	 */
//	public static boolean isMonConnected()
//	{
//		return isMonConnected(false, false);
//	}
//	/**
//	 * Do we have a connection to the database?
//	 * @return true or false
//	 */
//	public static boolean isMonConnected(boolean forceConnectionCheck, boolean closeConnOnFailure)
//	{
//		if (_conn == null) 
//			return false;
//
//		// Cache the last call for X ms (default 1200 ms)
//		if ( ! forceConnectionCheck )
//		{
//			long diff = System.currentTimeMillis() - _lastIsClosedCheck;
//			if ( diff < _lastIsClosedRefreshTime)
//			{
//				return true;
//			}
//		}
//
//		// check the connection itself
//		try
//		{
//			// jConnect issues RPC: sp_mda 0, 7 on isClosed()
//			if (_conn.isClosed())
//			{
//				if (closeConnOnFailure)
//					closeMonConnection();
//				return false;
//			}
//		}
//		catch (SQLException e)
//		{
//			return false;
//		}
//
//		_lastIsClosedCheck = System.currentTimeMillis();
//		return true;
//	}
//
//	/**
//	 * Set the <code>Connection</code> to use for monitoring.
//	 */
//	public static void setMonConnection(Connection conn)
//	{
//		_conn = conn;
//		setStatus(MainFrame.ST_CONNECT);
//	}
//
//	/**
//	 * Gets the <code>Connection</code> to the monitored server.
//	 */
//	public static Connection getMonConnection()
//	{
//		return _conn;
//	}
//
//	/** Gets the <code>Connection</code> to the monitored server. */
//	public static void closeMonConnection()
//	{
//		if (_conn == null) 
//			return;
//
//		try
//		{
//			if ( ! _conn.isClosed() )
//			{
//				_conn.close();
//				if (_logger.isDebugEnabled())
//				{
//					_logger.debug("Connection closed");
//				}
//			}
//		}
//		catch (SQLException ev)
//		{
//			_logger.error("closeMonConnection", ev);
//		}
//		_conn = null;
//	}
	
	/**
	 * Are we connected to a offline storage
	 * @return true or false
	 */
	public static boolean isOfflineConnected()
	{
		if (_offlineConn == null) 
			return false;

		// Cache the last call for X ms (default 1200 ms)
		long diff = System.currentTimeMillis() - _lastOfflineIsClosedCheck;
		if ( diff < _lastOfflineIsClosedRefreshTime)
		{
			return true;
		}

		// check the connection itself
		try
		{
			// jConnect issues RPC: sp_mda 0, 7 on isClosed()
			if (_offlineConn.isClosed())
				return false;
		}
		catch (SQLException e)
		{
			return false;
		}

		_lastOfflineIsClosedCheck = System.currentTimeMillis();
		return true;
	}

	/**
	 * Set the <code>Connection</code> to use for monitoring.
	 */
	public static void setOfflineConnection(Connection conn)
	{
		_offlineConn = conn;
	}

	/**
	 * Gets the <code>Connection</code> to the monitored server.
	 */
	public static Connection getOfflineConnection()
	{
		return _offlineConn;
	}
	
	/** Close the offline connection */
	public static void closeOfflineConnection()
	{
		// Close the Offline database connection
		try
		{
			if (isOfflineConnected())
			{
				_offlineConn.close();
				if (_logger.isDebugEnabled())
				{
					_logger.debug("Offline connection closed");
				}
			}
		}
		catch (SQLException ev)
		{
			_logger.error("Closing Offline connection", ev);
		}
		_offlineConn = null;
	}

	/**
	 * What tab is currently active?
	 * @return the Component within the tab
	 */
	public static Component getActiveTab()
	{
		return _mainTabbedPane.getSelectedComponent();
	}
	
	/**
	 * Clears fields in the SummaryPanel
	 */
	public static void clearSummaryData()
	{
		_summaryPanel.clearSummaryData();
	}

	/**
	 * Updates fields in the SummaryPanel
	 */
	public static void setSummaryData(CountersModel cm)
	{
		_summaryPanel.setSummaryData(cm);
	}

	/**
	 * Clears fields in the SummaryPanel
	 */
	public static SummaryPanel getSummaryPanel()
	{
		return _summaryPanel;
	}


	/** If we are in off-line mode, this text can be set, and it will be displayed */
	public static void setOfflineSamplePeriodText(String str)
	{
		_offlineSamplePeriod = str;
	}
	/** If we are in off-line mode, this text can be displayed */
	public static String getOfflineSamplePeriodText()
	{
		return _offlineSamplePeriod;
	}

	/**
	 * Gets current values from the status panel.
	 * @param type <code>ST_CONNECT, ST_DISCONNECT, ST_STATUS_FIELD, ST_MEMORY</code>
	 * @return a String with the information. If not found "" (space) will be returned
	 */
	public static String getStatus(int type)
	{
		if (type == ST_CONNECT)         return _statusServerName.getText();
		if (type == ST_OFFLINE_CONNECT) return "off-line-connect";
		if (type == ST_DISCONNECT)      return _statusServerName.getText();
		if (type == ST_STATUS_FIELD)    return _statusStatus.getText();
		if (type == ST_MEMORY)          return _statusMemory.getText();
		return "";
	}

	/**
	 * Sets values in the status panel.
	 * @param type <code>ST_CONNECT, ST_DISCONNECT, ST_STATUS_FIELD, ST_MEMORY</code>
	 */
	public static void setStatus(int type)
	{
		setStatus(type, null);
	}
	/**
	 * Sets values in the status panel.
	 * @param type <code>ST_CONNECT, ST_DISCONNECT, ST_STATUS_FIELD, ST_MEMORY</code>
	 * @param param The actual string to set (this is only used for <code>ST_STATUS_FIELD</code>)
	 */
	public static void setStatus(int type, String param)
	{
		// If this is NOT the event queue thread, dispatch it to that thread
//		if ( ! SwingUtils.isEventQueueThread() )
//		{
//			final int    finalType  = type;
//			final String finalParam = param;
//			Runnable execThis = new Runnable()
//			{
//				public void run()
//				{
//					MainFrame.setStatus(finalType, finalParam);
//				}
//			};
//			try { SwingUtilities.invokeAndWait(execThis); }
//			catch (InterruptedException e)      { e.printStackTrace(); }
//			catch (InvocationTargetException e) { e.printStackTrace(); }
//			return;
//		}
//		System.out.println("setStatus(): type='"+type+"', param='"+param+"'.");

		if (type == ST_OFFLINE_CONNECT)
		{
			if (isOfflineConnected())
			{
				_statusStatus    .setText("Offline...");
				_statusServerName.setText("Offline-read");
				_statusServerListeners.setText("Offline-read");

				_summaryPanel.setLocalServerName("Offline-read");

				// SET WATERMARK
				SummaryPanel.getInstance().setWatermark();
			}
			else
			{
				type = ST_DISCONNECT;
			}
		}
		// CONNECT
		else if (type == ST_CONNECT)
		{
//			if (_conn != null)
			if (Asemon.getCounterCollector().isMonConnected())
			{
				_statusStatus    .setText("Just Connected...");
//				_statusServerName.setText(
//						AseConnectionFactory.getServer() + " (" +
//						AseConnectionFactory.getHost()   + ":" +
//						AseConnectionFactory.getPort()   + ")"
//						);
				_statusServerName.setText(
						AseConnectionFactory.getServer() +
						" (" + AseConnectionFactory.getHostPortStr() + ")" );

				Connection conn = Asemon.getCounterCollector().getMonConnection();
				if (AseConnectionUtils.hasRole(conn, AseConnectionUtils.SA_ROLE))
					_statusServerListeners.setText(AseConnectionUtils.getListeners(conn, true, true, _instance));
				else
					_statusServerListeners.setText("Need 'sa_role' to get listeners.");

				_summaryPanel.setLocalServerName(_statusServerName.getText());

				// SET WATERMARK
				SummaryPanel.getInstance().setWatermark();
			}
			else
			{
				type = ST_DISCONNECT;
			}
		}
			
			
		// DISCONNECT
		if (type == ST_DISCONNECT)
		{
			_statusServerName     .setText(ST_DEFAULT_SERVER_NAME);
			_statusServerListeners.setText(ST_DEFAULT_SERVER_LISTENERS);
			_statusStatus         .setText(ST_DEFAULT_STATUS_FIELD);

			_summaryPanel.setLocalServerName("");

			// SET WATERMARK
			SummaryPanel.getInstance().setWatermark();

			Iterator iter = _TcpMap.values().iterator();
			while (iter.hasNext())
			{
				TabularCntrPanel tcp = (TabularCntrPanel) iter.next();
				tcp.setWatermark();
			}
		}

		// STATUS
		if (type == ST_STATUS_FIELD)
		{
			_statusStatus.setText(param);
		}
		
		// MEMORY
		if (type == ST_MEMORY)
		{
			_statusMemory.setText(
				"Memory: Used "+Memory.getUsedMemoryInMB() +
				" MB, Free "+Memory.getMemoryLeftInMB() + " MB");
		}
	}
	
	public static void openLogViewer()
	{
		if (_logView == null)
			_logView = new Log4jViewer(_instance);
		_logView.setVisible(true);
	}

	public static void openOfflineSessionView(boolean doSetVisible)
	{
		if (_offlineView == null)
		{
			_offlineView = new OfflineSessionVeiwer( MainFrame.getInstance() );

			PersistReader reader = PersistReader.getInstance();
			if (reader != null)
				reader.addNofificationListener(_offlineView);

			doSetVisible = true;
		}
		if (doSetVisible)
			_offlineView.setVisible(true);
	}


	/**
	 * Get SQL statement that will be executed for tooltip.
	 * @param colName Name of the column we want tolltip for
	 * @return SQL statement
	 */
	public static String getUserDefinedToolTip(String cmName, String colName)
	{
		String sql = (String) _udTooltipMap.get(cmName + "." + colName);

		if (sql == null)
			sql = (String) _udTooltipMap.get(colName);

		// Make a default for some COLUMNS if they was not found in the USER DEFINED MAP
		if (sql == null && "SPID".equalsIgnoreCase(colName))
		{
			sql = 
				"select " +
				" MP.SPID, " +
				" MP.Login, " +
				" MP.DBName, " +
				" MP.Application, " +
				" MP.Command, " +
				" MP.SecondsWaiting, " +
				" MP.SecondsConnected , " +
				" MP.WaitEventID, " +
				" WaitEventDescription = (select W.Description from monWaitEventInfo W where W.WaitEventID = MP.WaitEventID), " +
				" MP.BlockingSPID, " +
				" procname = (select object_name(sp.id,sp.dbid) from master..sysprocesses sp where sp.spid = MP.SPID), " +
				" MP.BatchID, " +
				" MP.LineNumber, " +
				" MP.BlockingXLOID, " +
				" MP.MasterTransactionID" +
				" from monProcess MP " +
				" where MP.SPID = ? ";
		}
		
		return sql;
	}

	public static boolean isSamplingPaused()
	{
		return _isSamplingPaused;
	}
	/*---------------------------------------------------
	** END: public methods
	**---------------------------------------------------
	*/
	static
	{
		Configuration conf = Configuration.getInstance(Configuration.CONF);
		if (conf != null)
		{
			
		}
		String prefix = "table.tooltip.";
		Enumeration en = conf.getKeys(prefix);
		while(en.hasMoreElements())
		{
			String key = (String) en.nextElement();
			String val = conf.getProperty(key);
			String name = key.substring(prefix.length());

			_logger.debug("Adding UD Cell Tooltip for colName='"+name+"', SQL='"+val+"'.");
			_udTooltipMap.put(name, val);
		}
	}
	
	
	/*---------------------------------------------------
	** BEGIN: private helper methods
	**---------------------------------------------------
	*/
	/** Loop all TabularCntrPanel and save some stuff in there */
	private void saveTcpProps()
	{
		for (Iterator it = _TcpMap.values().iterator(); it.hasNext();)
		{
			TabularCntrPanel tcp = (TabularCntrPanel) it.next();
			tcp.saveProps();
		}
	}

	private void saveProps()
	{
		//_logger.debug("xxxxx: " + e.getWindow().getSize());
		Configuration tmpConf = Configuration.getInstance(Configuration.TEMP);

		tmpConf.setProperty("main.refresh.interval", _refreshInterval);
		tmpConf.setProperty("nogui.sleepTime",       _refreshNoGuiInterval);

		tmpConf.setProperty("window.width",  getSize().width);
		tmpConf.setProperty("window.height", getSize().height);
		if (isVisible())
		{
			tmpConf.setProperty("window.pos.x",  getLocationOnScreen().x);
			tmpConf.setProperty("window.pos.y",  getLocationOnScreen().y);
		}
		
		getSummaryPanel().saveLayoutProps();

		tmpConf.save();

		// Done when the system exits
		// AsemonSaveProps.getInstance().save();
	}

	private void loadProps()
	{
		Configuration tmpConf = Configuration.getInstance(Configuration.TEMP);

		int width   = tmpConf.getIntProperty("window.width", 1000);
		int height  = tmpConf.getIntProperty("window.height", 700);
		int winPosX = tmpConf.getIntProperty("window.pos.x",  -1);
		int winPosY = tmpConf.getIntProperty("window.pos.y",  -1);

		// Set last known size, or Set a LARGE size
		setSize(width, height);

		//Center the window
		if (winPosX == -1  && winPosY == -1)
		{
			_logger.debug("Open main window in center of screen.");

			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			Dimension frameSize = getSize();

			// We cant be larger than the screen
			if (frameSize.height > screenSize.height) frameSize.height = screenSize.height;
			if (frameSize.width  > screenSize.width)  frameSize.width  = screenSize.width;

			setLocation((screenSize.width - frameSize.width) / 2,
			        (screenSize.height - frameSize.height) / 2);
		}
		// Set to last known position
		else
		{
			_logger.debug("Open main window in last known position.");
			setLocation(winPosX, winPosY);
		}

		// 
		_refreshInterval      = tmpConf.getIntProperty("main.refresh.interval", _refreshInterval);
		_refreshNoGuiInterval = tmpConf.getIntProperty("nogui.sleepTime",       _refreshNoGuiInterval);
	}
	/*---------------------------------------------------
	** END: private helper methods
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
	private class ReadTsWatermark
    extends AbstractComponentDecorator
    {
		public ReadTsWatermark(JComponent target, String text)
		{
			super(target);
			if (text != null)
				_text = text;
		}
		private String		_text	= "";
		private Graphics2D	g		= null;
		private Rectangle	r		= null;
		
		private JLabel      lbl     = new JLabel();
	
		public void paint(Graphics graphics)
		{
			if (_text == null || _text != null && _text.equals(""))
				return;
	
			r = getDecorationBounds();
			g = (Graphics2D) graphics;
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setFont(lbl.getFont());
	
			FontMetrics fm = g.getFontMetrics();
			int strWidth = fm.stringWidth(_text);
			int xPos = (r.width - strWidth) / 2;
			int yPos = (int) r.height - 5;
			g.drawString(_text, xPos, yPos);
		}
	
		public void setWatermarkText(String text)
		{
			_text = text;
			_logger.debug("setWatermarkText: to '" + _text + "'.");
			repaint();
		}
    }

	/**
	 * This timer is started when the Read InMemory slider is moved, and when the timer expires
	 * it will load/show the selected time.
	 * This is needed if we move the slider with the keyboard arrows, then we will overload the system.
	 */
	private class ReadSelectionTimerAction implements ActionListener
	{
		private MainFrame _mainframe = null;
		ReadSelectionTimerAction(MainFrame mf)
		{
			_mainframe = mf;
		}
		public void actionPerformed(ActionEvent actionevent)
		{
			_mainframe.readSliderMoveToCurrentTs();
			_mainframe._readSelectionTimer.stop();
		}
	}

	/**
	 * This timer is started when the Offline slider is moved, and when the timer expires
	 * it will load/show the selected time.
	 * This is needed if we move the slider with the keyboard arrows, then we will overload the system.
	 */
	private class OfflineSelectionTimerAction implements ActionListener
	{
		private MainFrame _mainframe = null;
		OfflineSelectionTimerAction(MainFrame mf)
		{
			_mainframe = mf;
		}
		public void actionPerformed(ActionEvent actionevent)
		{
			_mainframe.offlineSliderMoveToCurrentTs();
			_mainframe._offlineSelectionTimer.stop();
		}
	}

}
