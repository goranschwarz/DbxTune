/**
 */

package com.asetune.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
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
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.apache.log4j.lf5.LogLevel;

import com.asetune.AseConfig;
import com.asetune.AseConfigText;
import com.asetune.AseTune;
import com.asetune.GetCounters;
import com.asetune.GetCountersGui;
import com.asetune.MonTablesDictionary;
import com.asetune.ProcessDetailFrame;
import com.asetune.Version;
import com.asetune.check.CheckForUpdates;
import com.asetune.cm.CountersModel;
import com.asetune.cm.sql.VersionInfo;
import com.asetune.gui.swing.AbstractComponentDecorator;
import com.asetune.gui.swing.GTabbedPane;
import com.asetune.gui.swing.GTabbedPane.TabOrderAndVisibilityListener;
import com.asetune.gui.swing.GTabbedPaneViewDialog;
import com.asetune.gui.swing.Screenshot;
import com.asetune.gui.wizard.WizardOffline;
import com.asetune.gui.wizard.WizardUserDefinedCm;
import com.asetune.pcs.InMemoryCounterHandler;
import com.asetune.pcs.PersistContainer;
import com.asetune.pcs.PersistReader;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.Memory;
import com.asetune.utils.PropPropEntry;
import com.asetune.utils.SwingUtils;


public class MainFrame
    extends JFrame
    implements ActionListener, ChangeListener, TableModelListener, TabOrderAndVisibilityListener
{
	private static final long    serialVersionUID = 8984251025337127843L;
	private static Logger        _logger          = Logger.getLogger(MainFrame.class);

	//-------------------------------------------------
	// STATUS fields
	public static final int     ST_CONNECT                 = 1;
	public static final int     ST_OFFLINE_CONNECT         = 2;
	public static final int     ST_DISCONNECT              = 3;
	public static final int     ST_STATUS_FIELD            = 4;
	public static final int     ST_STATUS2_FIELD           = 5;
	public static final int     ST_MEMORY                  = 6;

	private static final String ST_DEFAULT_STATUS_FIELD    = "Not Connected";
	private static final String ST_DEFAULT_STATUS2_FIELD   = "";
	private static final String ST_DEFAULT_SERVER_NAME     = "ASENAME (host:port)";
	private static final String ST_DEFAULT_SERVER_LISTENERS= "ASE Server listens on address";

	//-------------------------------------------------
	// Actions
	public static final String ACTION_CONNECT                   = "CONNECT";
	public static final String ACTION_DISCONNECT                = "DISCONNECT";
	public static final String ACTION_EXIT                      = "EXIT";

	public static final String ACTION_OPEN_LOG_VIEW             = "OPEN_LOG_VIEW";
	public static final String ACTION_OPEN_OFFLINE_SESSION_VIEW = "OPEN_OFFLINE_SESSION_VIEW";
	public static final String ACTION_TOGGLE_AUTO_RESIZE_PC_TABLES = "TOGGLE_AUTO_RESIZE_PC_TABLES";
	public static final String ACTION_OPEN_REFRESH_RATE         = "OPEN_REFRESH_RATE";
	public static final String ACTION_OPEN_COUNTER_TAB_VIEW     = "OPEN_COUNTER_TAB_VIEW";
	public static final String ACTION_OPEN_GRAPH_GRAPH_VIEW     = "OPEN_GRAPH_GRAPH_VIEW";
	public static final String ACTION_OPEN_ASE_CONFIG_VIEW      = "ACTION_OPEN_ASE_CONFIG_VIEW";
	public static final String ACTION_OPEN_TCP_PANEL_CONFIG     = "OPEN_TCP_PANEL_CONFIG";

	public static final String ACTION_OPEN_ASE_CONFIG_MON       = "OPEN_ASE_CONFIG_MON";
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
	public static final String ACTION_GOTO_DATABASE_TAB         = "GOTO_DATABASE_TAB";

	public static final String ACTION_OUT_OF_MEMORY             = "OUT_OF_MEMORY";
	
	public static final String ACTION_SCREENSHOT                = "SCREENSHOT";
	public static final String ACTION_SAMPLING_PAUSE            = "SAMPLING_PAUSE";
	public static final String ACTION_SAMPLING_PLAY             = "SAMPLING_PLAY";
	
	public static final String ACTION_TAB_SELECTOR              = "TAB_SELECTOR";


	private PersistContainer _currentPc      = null;

	//-------------------------------------------------
	// Menus / toolbar

	private JToolBar            _toolbar                = new JToolBar();
//	private JToolBar            _pcsNavigation          = new JToolBar();
//	private JCheckBox           _viewStorage_chk        = new JCheckBox("View Stored Data", false);
	private JCheckBox           _viewStorage_chk        = new JCheckBox();
	private JSlider             _readSlider             = new JSlider();
	private ReadTsWatermark     _readTsWatermark        = null;
	private Timer               _readSelectionTimer     = new Timer(100, new ReadSelectionTimerAction(this));
	private JSlider             _offlineSlider          = new JSlider();
	private ReadTsWatermark     _offlineTsWatermark     = null;
//	private OfflineReadWatermark _offlineReadWatermark  = null;
	private Timer               _offlineSelectionTimer  = new Timer(100, new OfflineSelectionTimerAction(this));

	private JComboBox           _workspace_cbx          = new JComboBox();
	
	private JMenuBar            _main_mb                = new JMenuBar();

	// File
	private JMenu               _file_m                 = new JMenu("File");
	private JMenuItem           _connect_mi             = new JMenuItem("Connect...");
	private JMenuItem           _disconnect_mi          = new JMenuItem("Disconnect");
	private JMenuItem           _exit_mi                = new JMenuItem("Exit");

	// View
	private JMenu               _view_m                 = new JMenu("View");
	private JMenuItem           _logView_mi             = new JMenuItem("Open Log Window...");
	private JMenuItem           _offlineSessionsView_mi = new JMenuItem("Offline Sessions Window...");
	private JMenu               _preferences_m          = new JMenu("Preferences");
	private JMenuItem           _refreshRate_mi         = new JMenuItem("Refresh Rate...");
	private JCheckBoxMenuItem   _autoResizePcTable_mi   = new JCheckBoxMenuItem("Auto Resize Column Width in Performance Counter Tables", false);
	private JMenuItem           _aseConfigView_mi       = new JMenuItem("View ASE Configuration...");
	private JMenuItem           _tcpSettingsConf_mi     = new JMenuItem("Change 'Counter Table' Parameters...");
	private JMenuItem           _counterTabView_mi      = new JMenuItem("Change 'Tab Titles' Order and Visibility...");
	private JMenuItem           _graphView_mi           = new JMenuItem("Change 'Graph' Order and Visibility...");
	private static JMenu        _graphs_m               = new JMenu("Active Graphs");
	
	// Tools
	private JMenu               _tools_m                = new JMenu("Tools");
	private JMenuItem           _aseConfMon_mi          = new JMenuItem("Configure ASE for Monitoring...");
	private JMenuItem           _captureSql_mi          = new JMenuItem("Capture SQL...");
	private JMenu               _preDefinedSql_m        = null;
	private JMenuItem           _sqlQuery_mi            = new JMenuItem("SQL Query Window...");
//	private JMenuItem           _lockTool_mi            = new JMenuItem("Lock Tool (NOT YET IMPLEMENTED)");
	private JMenuItem           _createOffline_mi       = new JMenuItem("Create 'Offline Session' Wizard...");
	private JMenuItem           _wizardCrUdCm_mi        = new JMenuItem("Create 'User Defined Counter' Wizard...");
	private JMenuItem           _doGc_mi                = new JMenuItem("Java Garbage Collection");

	// Help
	private JMenu               _help_m                 = new JMenu("Help");
	private JMenuItem           _about_mi               = new JMenuItem("About");

	private static GTabbedPane  _mainTabbedPane         = new GTabbedPane();

	//-------------------------------------------------
	// STATUS Panel
	private JPanel                    _statusPanel               = new JPanel();
	private JButton                   _blockAlert_but            = new JButton();
	private JButton                   _fullTranlogAlert_but      = new JButton();
	private JButton                   _refreshNow_but            = new JButton();
	private JButton                   _samplePause_but           = new JButton();
	private JButton                   _samplePauseTb_but         = new JButton();
	private JButton                   _samplePlay_but            = new JButton();
	private JButton                   _samplePlayTb_but          = new JButton();
	private JButton                   _gcNow_but                 = new JButton();
	private static JLabel             _statusStatus              = new JLabel(ST_DEFAULT_STATUS_FIELD);
	private static JLabel             _statusStatus2             = new JLabel(ST_DEFAULT_STATUS2_FIELD);
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
	private static Map<String,TabularCntrPanel> _TcpMap           = new HashMap<String,TabularCntrPanel>();
	private static SummaryPanel       _summaryPanel               = new SummaryPanel();
	private static TabularCntrPanel   _currentPanel               = null;

	/** Keep a list of user defined SQL statements that will be used for tooltip on a JTable cell level */
	private static HashMap<String,String> _udTooltipMap = new HashMap<String,String>();
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

		// Install shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			@Override
			public void run()
			{
				_logger.debug("----Start Shutdown Hook");
				CheckForUpdates.sendCounterUsageInfoNoBlock();
				_logger.debug("----End Shutdown Hook");
			}
		});
	}

//	static
//	{
//		_logger.setLevel(Level.TRACE);
//	}
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
		setTitle(Version.getAppName());
		ImageIcon icon = SwingUtils.readImageIcon(Version.class, "images/asetune_icon.gif");
		if (icon != null)
			setIconImage(icon.getImage());

		
		//-------------------------------------------------------------------------
		// HARDCODE a STOP date when this "DEVELOPMENT VERSION" will STOP working
		//-------------------------------------------------------------------------
		if (Version.IS_DEVELOPMENT_VERSION)
		{
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			//setTitle(Version.getAppName()+" - This TEMPORARY DEVELOPMENT VERSION will NOT work after '"+df.format(Version.DEV_VERSION_EXPIRE_DATE)+"'.");
			setTitle(Version.getAppName()+" - This DEVELOPMENT VERSION will NOT work after '"+df.format(Version.DEV_VERSION_EXPIRE_DATE)+"', then you will have to download a later version.");
			//setTitle(Version.getAppName()+" - DEVELOPMENT VERSION, after '"+df.format(Version.DEV_VERSION_EXPIRE_DATE)+"' you will have to download a later version.");
		}
		if (AseTune.hasDevVersionExpired())
			setTitle(Version.getAppName()+" - In Development Version, \"time to live date\" has EXPIRED, only 'Persistent Counter Storage - READ' is enabled.");

		
		JPanel contentPane = (JPanel) this.getContentPane();
		contentPane.setLayout(new BorderLayout());

		//--------------------------
		// MENU - Icons
		_connect_mi            .setIcon(SwingUtils.readImageIcon(Version.class, "images/connect16.gif"));
		_disconnect_mi         .setIcon(SwingUtils.readImageIcon(Version.class, "images/disconnect16.gif"));
		_exit_mi               .setIcon(SwingUtils.readImageIcon(Version.class, "images/close.gif"));

		_logView_mi            .setIcon(SwingUtils.readImageIcon(Version.class, "images/log_viewer.gif"));
		_offlineSessionsView_mi.setIcon(SwingUtils.readImageIcon(Version.class, "images/offline_sessions_view.png"));
		_autoResizePcTable_mi  .setIcon(SwingUtils.readImageIcon(Version.class, "images/auto_resize_table_columns.png"));
		_refreshRate_mi        .setIcon(SwingUtils.readImageIcon(Version.class, "images/refresh_rate.png"));
		_aseConfigView_mi      .setIcon(SwingUtils.readImageIcon(Version.class, "images/config_ase_view.png"));
		_tcpSettingsConf_mi    .setIcon(SwingUtils.readImageIcon(Version.class, "images/tcp_settings_conf.png"));
		_counterTabView_mi     .setIcon(SwingUtils.readImageIcon(Version.class, "images/counter_tab_view.gif"));
		_graphView_mi          .setIcon(SwingUtils.readImageIcon(Version.class, "images/graph.png"));
		_graphs_m              .setIcon(SwingUtils.readImageIcon(Version.class, "images/summary_tab.png"));
		
		_aseConfMon_mi         .setIcon(SwingUtils.readImageIcon(Version.class, "images/config_ase_mon.png"));
		_captureSql_mi         .setIcon(SwingUtils.readImageIcon(Version.class, "images/capture_sql_tool.gif"));
		_sqlQuery_mi           .setIcon(SwingUtils.readImageIcon(Version.class, "images/sql_query_window.png"));
//		_lockTool_mi           .setIcon(SwingUtils.readImageIcon(Version.class, "images/locktool16.gif"));
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
		_view_m.add(_preferences_m);
			_preferences_m.add(_autoResizePcTable_mi);
			_preferences_m.add(_refreshRate_mi);
		_view_m.add(_aseConfigView_mi);
		_view_m.add(_tcpSettingsConf_mi);
		_view_m.add(_counterTabView_mi);
		_view_m.add(_graphView_mi);
		_view_m.add(_graphs_m);
		
		_tools_m.add(_aseConfMon_mi);
		_tools_m.add(_captureSql_mi);
		_preDefinedSql_m = createPredefinedSqlMenu();
		if (_preDefinedSql_m != null) _tools_m.add(_preDefinedSql_m);
		_tools_m.add(_sqlQuery_mi);
//		_tools_m.add(_lockTool_mi);
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
		_autoResizePcTable_mi  .setActionCommand(ACTION_TOGGLE_AUTO_RESIZE_PC_TABLES);
		_refreshRate_mi        .setActionCommand(ACTION_OPEN_REFRESH_RATE);
		_aseConfigView_mi      .setActionCommand(ACTION_OPEN_ASE_CONFIG_VIEW);
		_tcpSettingsConf_mi    .setActionCommand(ACTION_OPEN_TCP_PANEL_CONFIG);
		_counterTabView_mi     .setActionCommand(ACTION_OPEN_COUNTER_TAB_VIEW);
		_graphView_mi          .setActionCommand(ACTION_OPEN_GRAPH_GRAPH_VIEW);

		_aseConfMon_mi         .setActionCommand(ACTION_OPEN_ASE_CONFIG_MON);
		_captureSql_mi         .setActionCommand(ACTION_OPEN_CAPTURE_SQL);
		_sqlQuery_mi           .setActionCommand(ACTION_OPEN_SQL_QUERY_WIN);
//		_lockTool_mi           .setActionCommand(ACTION_OPEN_LOCK_TOOL);
		_createOffline_mi      .setActionCommand(ACTION_OPEN_WIZARD_OFFLINE);
		_wizardCrUdCm_mi       .setActionCommand(ACTION_OPEN_WIZARD_UDCM);
		_doGc_mi               .setActionCommand(ACTION_GARBAGE_COLLECT);
		
		_about_mi              .setActionCommand(ACTION_OPEN_ABOUT);

		//--------------------------
		// And the action listener
		_connect_mi            .addActionListener(this);
		_disconnect_mi         .addActionListener(this);
		_exit_mi               .addActionListener(this);

		_logView_mi            .addActionListener(this);
		_offlineSessionsView_mi.addActionListener(this);
		_autoResizePcTable_mi  .addActionListener(this);
		_refreshRate_mi        .addActionListener(this);
		_aseConfigView_mi      .addActionListener(this);
		_tcpSettingsConf_mi    .addActionListener(this);
		_counterTabView_mi     .addActionListener(this);
		_graphView_mi          .addActionListener(this);

		_aseConfMon_mi         .addActionListener(this);
		_captureSql_mi         .addActionListener(this);
		_sqlQuery_mi           .addActionListener(this);
//		_lockTool_mi           .addActionListener(this);
		_createOffline_mi      .addActionListener(this);
		_wizardCrUdCm_mi       .addActionListener(this);
		_doGc_mi               .addActionListener(this);
		
		_about_mi              .addActionListener(this);

		//--------------------------
		// Keyboard shortcuts
		_file_m .setMnemonic(KeyEvent.VK_F);
		_view_m .setMnemonic(KeyEvent.VK_V);
		_tools_m.setMnemonic(KeyEvent.VK_T);
		_help_m .setMnemonic(KeyEvent.VK_H);

		_connect_mi        .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK));
		_disconnect_mi     .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.ALT_MASK));
		_sqlQuery_mi       .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));
		_logView_mi        .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.ALT_MASK));
		_tcpSettingsConf_mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.ALT_MASK));
		_aseConfMon_mi     .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, ActionEvent.ALT_MASK));

		
		//--------------------------
		// TOOLBAR
		JButton connect    = SwingUtils.makeToolbarButton(Version.class, "connect16.gif",    ACTION_CONNECT,    this, "Connect to a ASE",         "Connect");
		JButton disConnect = SwingUtils.makeToolbarButton(Version.class, "disconnect16.gif", ACTION_DISCONNECT, this, "Close the ASE Connection", "Disconnect");

		JButton screenshot  = SwingUtils.makeToolbarButton(Version.class, "screenshot.png",   ACTION_SCREENSHOT,     this, "Take a screenshot of the application", "Screenshot");
		_samplePauseTb_but  = SwingUtils.makeToolbarButton(Version.class, "sample_pause.png", ACTION_SAMPLING_PAUSE, this, "Pause ALL sampling activity",          "Pause");
		_samplePlayTb_but   = SwingUtils.makeToolbarButton(Version.class, "sample_play.png",  ACTION_SAMPLING_PLAY,  this, "Continue to sample...",          "Pause");
		_samplePlayTb_but.setVisible(false);

		JButton tabSelectorNoSort = SwingUtils.makeToolbarButton(Version.class, "tab_selector_no_sort.gif", ACTION_TAB_SELECTOR, this, "Activate a specific tab", "Activate Tab");
		final JPopupMenu tabSelectorNoSortPopupMenu = new JPopupMenu();
		tabSelectorNoSortPopupMenu.add(new JMenuItem("No Tab's is Visible"));
		tabSelectorNoSort.setComponentPopupMenu(tabSelectorNoSortPopupMenu);
		tabSelectorNoSortPopupMenu.addPopupMenuListener(new PopupMenuListener()
		{
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e)
			{
				// remove all old items (if any)
				tabSelectorNoSortPopupMenu.removeAll();

				// Get all titles into a list
				ArrayList<String> tabStrList = new ArrayList<String>();
				for (int t=0; t<_mainTabbedPane.getTabCount(); t++)
					tabStrList.add(_mainTabbedPane.getTitleAt(t));

				// Sort the list
				//Collections.sort(tabStrList);

				// Now create menu items in the correct order
				for (String name : tabStrList)
				{
					int tabIndex = _mainTabbedPane.indexOfTab(name);

					JMenuItem mi = new JMenuItem();
					mi.setText(_mainTabbedPane.getTitleAt(tabIndex));
					mi.setIcon(_mainTabbedPane.getIconAt(tabIndex));
					mi.addActionListener(new ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent e)
						{
							Object o = e.getSource();
							if (o instanceof JMenuItem)
							{
								JMenuItem mi = (JMenuItem) o;
								String tabName = mi.getText();
								int tabIndex = _mainTabbedPane.indexOfTab(tabName);
								_mainTabbedPane.setSelectedIndex(tabIndex);
							}
						}
					});

					tabSelectorNoSortPopupMenu.add(mi);
				}
			}
			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {/*empty*/}
			@Override
			public void popupMenuCanceled(PopupMenuEvent e)	{/*empty*/}
		});

		JButton tabSelectorSorted = SwingUtils.makeToolbarButton(Version.class, "tab_selector_sorted.gif", ACTION_TAB_SELECTOR, this, "Activate a specific tab (sorted by name)", "Activate Tab");
		final JPopupMenu tabSelectorSortedPopupMenu = new JPopupMenu();
		tabSelectorSortedPopupMenu.add(new JMenuItem("No Tab's is Visible"));
		tabSelectorSorted.setComponentPopupMenu(tabSelectorSortedPopupMenu);
		tabSelectorSortedPopupMenu.addPopupMenuListener(new PopupMenuListener()
		{
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e)
			{
				// remove all old items (if any)
				tabSelectorSortedPopupMenu.removeAll();

				// Get all titles into a list
				ArrayList<String> tabStrList = new ArrayList<String>();
				for (int t=0; t<_mainTabbedPane.getTabCount(); t++)
					tabStrList.add(_mainTabbedPane.getTitleAt(t));

				// Sort the list
				Collections.sort(tabStrList);

				// Now create menu items in the correct order
				for (String name : tabStrList)
				{
					int tabIndex = _mainTabbedPane.indexOfTab(name);

					JMenuItem mi = new JMenuItem();
					mi.setText(_mainTabbedPane.getTitleAt(tabIndex));
					mi.setIcon(_mainTabbedPane.getIconAt(tabIndex));
					mi.addActionListener(new ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent e)
						{
							Object o = e.getSource();
							if (o instanceof JMenuItem)
							{
								JMenuItem mi = (JMenuItem) o;
								String tabName = mi.getText();
								int tabIndex = _mainTabbedPane.indexOfTab(tabName);
								_mainTabbedPane.setSelectedIndex(tabIndex);
							}
						}
					});

					tabSelectorSortedPopupMenu.add(mi);
				}
			}
			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {/*empty*/}
			@Override
			public void popupMenuCanceled(PopupMenuEvent e)	{/*empty*/}
		});

		_viewStorage_chk .setIcon(        SwingUtils.readImageIcon(Version.class, "images/read_storage.png"));
		_viewStorage_chk .setSelectedIcon(SwingUtils.readImageIcon(Version.class, "images/read_storage_minus.png"));
		_viewStorage_chk .setToolTipText("View Counters that are stored in memory for a while...");

		_toolbar.setLayout(new MigLayout("insets 0 0 0 3", "", "")); // insets Top Left Bottom Right
		_toolbar.add(connect);
		_toolbar.add(disConnect);
		_toolbar.add(screenshot);
		_toolbar.add(_samplePauseTb_but, "hidemode 3");
		_toolbar.add(_samplePlayTb_but,  "hidemode 3");
		_toolbar.add(tabSelectorNoSort);
		_toolbar.add(tabSelectorSorted);

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
//		_offlineReadWatermark = new OfflineReadWatermark(_offlineSlider, "");
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
		_fullTranlogAlert_but .setToolTipText("<html>The Transaction log in one/several databases are <b>full</b> other SPID(s) are probably <b>blocked</b> by this. Click here to the 'Database' tab to find out more.</html>");
		_refreshNow_but       .setToolTipText("Abort the \"sleep for next refresh\" and make a new refresh of data NOW.");
		_samplePause_but      .setToolTipText("Pause ALL sampling activity.");
		_samplePlay_but       .setToolTipText("Continue to sample counters again.");
		_gcNow_but            .setToolTipText("Do Java Garbage Collection.");
		_statusStatus         .setToolTipText("What are we doing or waiting for.");
		_statusStatus2        .setToolTipText("What are we doing or waiting for.");
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

		// Full transaction log alert butt
		_fullTranlogAlert_but.setIcon(SwingUtils.readImageIcon(Version.class, "images/full_translog_alert.png"));
		_fullTranlogAlert_but.setText("");
		_fullTranlogAlert_but.setContentAreaFilled(false);
//		_fullTranlogAlert_but.setMargin( new Insets(3,3,3,3) );
		_fullTranlogAlert_but.setMargin( new Insets(0,0,0,0) );
		_fullTranlogAlert_but.addActionListener(this);
		_fullTranlogAlert_but.setActionCommand(ACTION_GOTO_DATABASE_TAB);
		_fullTranlogAlert_but.setVisible(false);

		
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
		_statusPanel.add(_fullTranlogAlert_but,                   "hidemode 3");
		_statusPanel.add(_statusStatus,                           "gaptop 3, gapbottom 5, grow, push");
		_statusPanel.add(_statusStatus2,                          "");
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
		_mainTabbedPane.setTabOrderAndVisibilityListener(this);


		
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
				if (AseTune.getCounterCollector().isMonConnected())
				{
					int answer = AseConfigMonitoringDialog.onExit(_instance, AseTune.getCounterCollector().getMonConnection());

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
		_autoResizePcTable_mi.setEnabled(true);
		_refreshRate_mi      .setEnabled(true);
		_aseConfigView_mi    .setEnabled(true);
		_tcpSettingsConf_mi  .setEnabled(true);
		_counterTabView_mi   .setEnabled(true);
		_graphView_mi        .setEnabled(true);
		_graphs_m            .setEnabled(true);

		_aseConfMon_mi       .setEnabled(true);
		_captureSql_mi       .setEnabled(true);
		if (_preDefinedSql_m != null) _preDefinedSql_m.setEnabled(true);
		_sqlQuery_mi         .setEnabled(true);
//		_lockTool_mi         .setEnabled(true);
		_createOffline_mi    .setEnabled(true);
		_wizardCrUdCm_mi     .setEnabled(true);

		// Offline components
		_offlineSessionsView_mi.setEnabled(false);
	}

	private void setOfflineMenuMode()
	{
		// Monitor Server components
		_autoResizePcTable_mi.setEnabled(true);
		_refreshRate_mi      .setEnabled(false);
		_aseConfigView_mi    .setEnabled(true);
		_tcpSettingsConf_mi  .setEnabled(true);
		_counterTabView_mi   .setEnabled(true);
		_graphView_mi        .setEnabled(true);
		_graphs_m            .setEnabled(true);

		_aseConfMon_mi       .setEnabled(false);
		_captureSql_mi       .setEnabled(false);
		if (_preDefinedSql_m != null) _preDefinedSql_m.setEnabled(false);
		_sqlQuery_mi         .setEnabled(false);
//		_lockTool_mi         .setEnabled(false);
		_createOffline_mi    .setEnabled(false);
		_wizardCrUdCm_mi     .setEnabled(false);

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
		if ( _guiInitTime == 0 ) // setVisible has not been called
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
		try
		{
			LogLevel logLevel = _logView.getLogLevelForRow(e.getLastRow());
			
			// if loglevel is more severe than WARN, then show the logView window 
			// even if it's not visible for the moment
			if (logLevel != null && ! logLevel.encompasses(LogLevel.WARN))
			{
				if (_logView.doOpenOnErrors())
				{
					if ( ! _logView.isVisible() )
						_logView.setVisible(true);
				}
			}
		}
		catch (RuntimeException rex) { /*Skip runtime exceptions*/ }
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

		if (ACTION_TOGGLE_AUTO_RESIZE_PC_TABLES.equals(actionCmd))
			action_toggleAutoResizePcTables(e);

		if (ACTION_OPEN_REFRESH_RATE.equals(actionCmd))
			action_refreshRate(e);

		if (ACTION_OPEN_COUNTER_TAB_VIEW.equals(actionCmd))
			action_counterTabView(e);

		if (ACTION_OPEN_GRAPH_GRAPH_VIEW.equals(actionCmd))
			action_openGraphViewDialog(e);

		if (ACTION_OPEN_ASE_CONFIG_MON.equals(actionCmd))
			AseConfigMonitoringDialog.showDialog(this, AseTune.getCounterCollector().getMonConnection(), -1);

		if (ACTION_OPEN_ASE_CONFIG_VIEW.equals(actionCmd))
			AseConfigViewDialog.showDialog(this);

		if (ACTION_OPEN_TCP_PANEL_CONFIG.equals(actionCmd))
			TcpConfigDialog.showDialog(_instance);			

		if (ACTION_OPEN_CAPTURE_SQL.equals(actionCmd))
			new ProcessDetailFrame(-1);


		if (ACTION_OPEN_SQL_QUERY_WIN.equals(actionCmd))
		{
			try 
			{
				// Check that we are connected
				if ( ! AseTune.getCounterCollector().isMonConnected(true, true) )
				{
					SwingUtils.showInfoMessage(MainFrame.getInstance(), "Not connected", "Not yet connected to a server.");
					return;
				}

				Connection conn = AseConnectionFactory.getConnection(null, Version.getAppName()+"-QueryWindow", null);
				QueryWindow qf = new QueryWindow(conn, true, QueryWindow.WindowType.JFRAME);
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

			for (TabularCntrPanel tcp : _TcpMap.values())
			{
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
			}

			_readSlider.setVisible(_viewStorage_chk.isSelected());
		}

		if (ACTION_REFRESH_NOW.equals(actionCmd))
		{
			_logger.debug("called: ACTION_REFRESH_NOW");

			GetCounters getCnt = AseTune.getCounterCollector();
			if (getCnt != null)
				getCnt.doInterrupt();
		}

		if (ACTION_GOTO_BLOCKED_TAB.equals(actionCmd))
		{
			_logger.debug("called: ACTION_GOTO_BLOCKED_TAB");

			String toTabName = GetCounters.CM_DESC__BLOCKING; // "Blocking"
			JTabbedPane tabPane = getTabbedPane();
			if (tabPane != null)
				tabPane.setSelectedIndex(tabPane.indexOfTab(toTabName));
		}

		if (ACTION_GOTO_DATABASE_TAB.equals(actionCmd))
		{
			_logger.debug("called: ACTION_GOTO_DATABASE_TAB");

			String toTabName = GetCounters.CM_DESC__OPEN_DATABASES; // "Databases"
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
			JDialog dialog = optionPane.createDialog(this, "out-of-memory");
			dialog.setModal(false);
			dialog.setVisible(true);

			setStatus(MainFrame.ST_MEMORY);
		}

		if (ACTION_SCREENSHOT.equals(actionCmd))
		{
			String fileList = "";
			String extraInfo = Version.getAppName() + ", Version: "+ Version.getVersionStr();
			String main = Screenshot.windowScreenshot(this, Version.APP_STORE_DIR, "asetune.main", true, extraInfo);
			fileList += main;

			// LOOP all CounterModels, and check if they got any windows open, then screenshot that also
			for (CountersModel cm : GetCounters.getCmList())
			{
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
							String fn = Screenshot.windowScreenshot(frame, Version.APP_STORE_DIR, "asetune."+cm.getName(), true, extraInfo);
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

		if (ACTION_TAB_SELECTOR.equals(actionCmd))
		{
			if (source instanceof JButton)
			{
				JButton but = (JButton)source;
				JPopupMenu pm = but.getComponentPopupMenu();
				pm.show(but, 14, 14);
				pm.setLocation( MouseInfo.getPointerInfo().getLocation() );
//				pm.setVisible(true);
			}
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
			int selectedTab = _mainTabbedPane.getSelectedIndex();
			if (selectedTab < 0)
				return;

			String currentTab = _mainTabbedPane.getTitleAt(selectedTab);
			if (_logger.isDebugEnabled())
			{
				_logger.debug("state changed for pannel named '" + currentTab + "'.");
			}
	
	
			// LOOP all TabularCntrPanel to check which is the current one...
			// if it should be done
			for (TabularCntrPanel tcp : _TcpMap.values())
			{
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
	
			if ((_currentPanel != null) && (AseTune.getCounterCollector().getMonConnection() != null) && (_currentPanel.getCm() != null) && (!_currentPanel.getCm().isDataInitialized()))
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
				Dictionary<Integer, JLabel> dict = new Hashtable<Integer, JLabel>();
				String leftStr  = SimpleDateFormat.getTimeInstance().format(pcLeft .getMainSampleTime());
				String rightStr = SimpleDateFormat.getTimeInstance().format(pcRight.getMainSampleTime());
				
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
					}
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
			Timestamp ts = _offlineTsList.get(listPos);
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
				// First load the Summary panel...
				CountersModel summaryCm = _currentPc.getCm(SummaryPanel.CM_NAME);
				if (summaryCm != null)
					setSummaryData(summaryCm);

				for (TabularCntrPanel tcp : _TcpMap.values())
				{
					boolean directAccess = false;
					if (directAccess)
					{
						CountersModel cm = _currentPc.getCm(tcp.getName());
						tcp.setDisplayCm(cm, false);
					}
					else
					{
						CountersModel inMemCm = _currentPc.getCm(tcp.getName());
						tcp.setInMemHistSampleTime(inMemCm, ts);
						
						if (_mainTabbedPane.isTabUnDocked(tcp.getPanelName()))
							tcp.tabSelected();
	
						// notify that the current tab to "re-read" it's data...
						if (_currentPanel != null)
							_currentPanel.tabSelected();
	
						// Position the time line marker in graphs
						CountersModel cm = tcp.getCm();
						if (cm != null && cm.hasTrendGraph())
						{
							for (TrendGraph tg : cm.getTrendGraphs().values())
								tg.setTimeLineMarker(ts);
						}
					}
				}

				// if Summary has attached, graphs, go and set the time line marker
//				CountersModel summaryCm = GetCounters.getCmByName(SummaryPanel.CM_NAME);
				if (summaryCm != null && summaryCm.hasTrendGraph() )
					for (TrendGraph tg : summaryCm.getTrendGraphs().values())
						tg.setTimeLineMarker(ts);
			}
		}
		_currentPc = pc;
	}

	/** Differed action on the offline slider when it has not been received input for X ms */
	private void offlineSliderMoveToCurrentTs()
	{
		int listPos = _offlineSlider.getValue(); 
		Timestamp ts = _offlineTsList.get(listPos);
		_offlineTsWatermark.setWatermarkText(ts==null ? "-" : ts.toString());

		if (ts == null)
			return;

		PersistReader reader = PersistReader.getInstance();
		if (reader == null)
			return;

//		_offlineReadWatermark.setWatermarkText("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
		reader.setCurrentSampleTime(ts);
		reader.loadSummaryCm(ts);

		// set current location in the view...
		if (_offlineView != null)
			_offlineView.setCurrentSampleTimeView(ts);

		for (TabularCntrPanel tcp : _TcpMap.values())
		{
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
				for (TrendGraph tg : cm.getTrendGraphs().values())
					tg.setTimeLineMarker(ts);
			}
		}

		// if Summary has attached, graphs, go and set the time line marker
		CountersModel summaryCm = GetCounters.getCmByName(SummaryPanel.CM_NAME);
		if (summaryCm != null && summaryCm.hasTrendGraph() )
			for (TrendGraph tg : summaryCm.getTrendGraphs().values())
				tg.setTimeLineMarker(ts);

//		_offlineReadWatermark.setWatermarkText("");
	}

	
	
	/*---------------------------------------------------
	** BEGIN: Helper methods for actions
	**---------------------------------------------------
	*/
	private boolean isNull(String str)
	{
		if (str == null)        return true;
		if (str.equals("null")) return true;
		return false;
	}
	public void action_connect(ActionEvent e)
	{
		Object source = e.getSource();
		String action = e.getActionCommand();

		if (AseTune.getCounterCollector().isMonConnected())
		{
			SwingUtils.showInfoMessage(this, "ASE - connect", "Connection already opened, Please close the connection first...");
			return;
		}

		// Create a new dialog Window
		boolean checkAseCfg    = true;
		boolean showAseTab     = true;
		boolean showAseOptions = true;
		boolean showHostmonTab = true;
		boolean showPcsTab     = true;
		boolean showOfflineTab = true;
		if (AseTune.hasDevVersionExpired())
		{
			checkAseCfg    = true;
			showAseTab     = false;
			showAseOptions = true;
			showHostmonTab = false;
			showPcsTab     = false;
			showOfflineTab = true;
		}

		ConnectionDialog connDialog = new ConnectionDialog(this, checkAseCfg, showAseTab, showAseOptions, showHostmonTab, showPcsTab, showOfflineTab);
		if (source instanceof GetCountersGui && showAseTab)
		{
			if ( action != null && action.startsWith(ConnectionDialog.CONF_OPTION_CONNECT_ON_STARTUP) )
			{
				try
				{
					// user, passwd commes as the String:
					// conn.onStartup={aseUsername=user, asePassword=pass, ...} see below for props
					// PropPropEntry parses the entries and then we can query the PPE object
					_logger.debug(action);
					PropPropEntry ppe = new PropPropEntry(action);
					String key = ConnectionDialog.CONF_OPTION_CONNECT_ON_STARTUP;

					if (!isNull(ppe.getProperty(key, "aseUsername"))) connDialog.setAseUsername(ppe.getProperty(key, "aseUsername"));
					if (!isNull(ppe.getProperty(key, "asePassword"))) connDialog.setAsePassword(ppe.getProperty(key, "asePassword"));
					if (!isNull(ppe.getProperty(key, "aseServer"))  ) connDialog.setAseServer  (ppe.getProperty(key, "aseServer"));

					if (!isNull(ppe.getProperty(key, "sshUsername"))) connDialog.setSshUsername(ppe.getProperty(key, "sshUsername"));
					if (!isNull(ppe.getProperty(key, "sshPassword"))) connDialog.setSshPassword(ppe.getProperty(key, "sshPassword"));
					if (!isNull(ppe.getProperty(key, "sshHostname"))) connDialog.setSshHostname(ppe.getProperty(key, "sshHostname"));
					if (!isNull(ppe.getProperty(key, "sshPort"))    ) connDialog.setSshPort    (ppe.getProperty(key, "sshPort"));

//					String[] sa = action.split(":");
//					if (sa.length >= 4) connDialog.setAseServer  (sa[3]); // THIS MUST BE FIRST
//					if (sa.length >= 3) connDialog.setAsePassword(sa[2]);
//					if (sa.length >= 2) connDialog.setAseUsername(sa[1]);
//					_logger.debug("CONF_OPTION_CONNECT_ON_STARTUP: sa.length="+sa.length+"; "+StringUtil.toCommaStr(sa));
//					_logger.debug("CONF_OPTION_CONNECT_ON_STARTUP: aseUser='"+connDialog.getAseUsername()+"', asePasswd='"+connDialog.getAsePassword()+"', aseServer='"+connDialog.getAseServer()+"'.");
	
					connDialog.actionPerformed(new ActionEvent(this, 0, ConnectionDialog.ACTION_OK));
				}
				catch(Exception ex)
				{
					_logger.warn(ex.getMessage());
				}
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
			if (AseTune.hasDevVersionExpired())
				throw new RuntimeException(Version.getAppName()+" DEV Version has expired, can't connect to a ASE. only 'PCS - Read mode' is available.");

			AseTune.getCounterCollector().setMonConnection( connDialog.getAseConn() );
			AseTune.getCounterCollector().setHostMonConnection( connDialog.getSshConn() );

//			if (_conn != null)
			if (AseTune.getCounterCollector().isMonConnected())
			{
//				_summaryPanel.setLocalServerName(AseConnectionFactory.getServer());
				setStatus(ST_CONNECT);

				// Initilize the MonTablesDictionary
				// This will serv as a dictionary for ToolTip
				if ( ! MonTablesDictionary.getInstance().isInitialized() )
				{
//System.out.println(">>>>> MonTablesDictionary.init");
					MonTablesDictionary.getInstance().initialize(AseTune.getCounterCollector().getMonConnection(), true);
					GetCounters.initExtraMonTablesDictionary();
//System.out.println("<<<<< MonTablesDictionary.init");
				}

				// initialize ASE Config Dictionary
				AseConfig aseCfg = AseConfig.getInstance();
				if ( ! aseCfg.isInitialized() )
				{
					aseCfg.initialize(AseTune.getCounterCollector().getMonConnection(), true, false, null);
				}

//				// initialize ASE Cache Config Dictionary
//				AseCacheConfig aseCacheCfg = AseCacheConfig.getInstance();
//				if ( ! aseCacheCfg.isInitialized() )
//				{
//					aseCacheCfg.initialize(AseTune.getCounterCollector().getMonConnection(), true, false, null);
//				}
				// initialize ASE Config Text Dictionary
				AseConfigText.initializeAll(AseTune.getCounterCollector().getMonConnection(), true, false, null);

				GetCounters getCnt = AseTune.getCounterCollector();
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

				// Read in the MonTablesDictionary from the offline store
				// This will serve as a dictionary for ToolTip
				MonTablesDictionary.getInstance().initializeMonTabColHelper(getOfflineConnection(), true);
				GetCounters.initExtraMonTablesDictionary();

				// initialize ASE Config Dictionary
				AseConfig.getInstance().initialize(getOfflineConnection(), true, true, null);

//				// initialize ASE Cache Config Dictionary
//				AseCacheConfig.getInstance().initialize(getOfflineConnection(), true, true, null);

				// initialize ASE Config Text Dictionary
				AseConfigText.initializeAll(getOfflineConnection(), true, true, null);

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

		// If we have a PersistentCounterHandler, stop it...
		if ( PersistentCounterHandler.hasInstance() )
		{
			PersistentCounterHandler.getInstance().stop();
		}

		// If we have a Reader, stop it...
		if ( PersistReader.hasInstance() )
		{
			PersistReader.getInstance().shutdown();
			PersistReader.setInstance(null);
		}
		
		// Possible
		// Clear all tabs, online/offline-slider, (graphs) 

//FIXME: need much more work
//		// Call all Dictionaries to empty them
//		// this enables us to reconnect to any version...
		AseConfig.reset();
		AseConfigText.reset();
//		MonTablesDictionary.reset();      // Most probably need to work more on this one...
//	//	MonWaitEventIdDictionary.reset(); // Do not need to be reset, it's not getting anything from DB
//		GetCounters.reset();              // Which does reset on all CM objects
	}


	private void action_exit(ActionEvent e)
	{
		// notify SWING that it's time to close...
		dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
//		setVisible(false);
//		System.exit(0);
	}

	private void action_toggleAutoResizePcTables(ActionEvent e)
	{
		saveProps();
	}

	private void action_refreshRate(ActionEvent e)
	{
		String key1 = "Refresh Rate";
//		String key2 = "Refresh Rate (no-gui)";

		LinkedHashMap<String, String> in = new LinkedHashMap<String, String>();
		in.put(key1, Integer.toString(_refreshInterval));
//		in.put(key2, Integer.toString(_refreshNoGuiInterval));

		Map<String,String> results = ParameterDialog.showParameterDialog(this, "Refresh Interval", in, false);

		if (results != null)
		{
			_refreshInterval      = Integer.parseInt(results.get(key1));
//			_refreshNoGuiInterval = Integer.parseInt(results.get(key2));

			saveProps();
		}
	}

	private void action_counterTabView(ActionEvent e)
	{
		int ret = GTabbedPaneViewDialog.showDialog(this, _mainTabbedPane);
		if (ret == JOptionPane.OK_OPTION)
		{
		}
	}

	private void action_openGraphViewDialog(ActionEvent e)
	{
		int ret = TrendGraphPanelReorderDialog.showDialog(this, SummaryPanel.getInstance().getGraphPanel());
		if (ret == JOptionPane.OK_OPTION)
		{
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
	private List<Timestamp> _offlineTsList = new ArrayList<Timestamp>();

	/**
	 * Get current Timestamp for the slider position
	 * @return Timestamp where the slider is positioned at or null if (_offlineSlider is not visible or _readSlider is not visible)
	 */
	public Timestamp getCurrentSliderTs()
	{
		if (_offlineSlider.isVisible())
		{
			int listPos = _offlineSlider.getValue(); 
			return _offlineTsList.get(listPos);
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
		_offlineTsList = new ArrayList<Timestamp>();
	}
	/**
	 * Add a Timestamp to the offline slider
	 */
	public void addOfflineSliderEntry(Timestamp ts)
	{
		_offlineTsList.add(ts);
		
		int size = _offlineTsList.size()-1;
		_offlineSlider.setMaximum(size);
		
		Timestamp left  = _offlineTsList.get(0);
		Timestamp right = _offlineTsList.get(size);

		Dictionary<Integer, JLabel> dict = new Hashtable<Integer, JLabel>();
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
	public void addOfflineSliderEntryList(List<Timestamp> list)
	{
		if (list == null)
			_offlineTsList = new ArrayList<Timestamp>(1);
		else
			_offlineTsList = list;
		
		int size = _offlineTsList.size()-1;
		_offlineSlider.setMaximum(size);
		
		Timestamp left  = (Timestamp)_offlineTsList.get(0);
		Timestamp right = (Timestamp)_offlineTsList.get(size);

		Dictionary<Integer, JLabel> dict = new Hashtable<Integer, JLabel>();
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
				GetCounters getCnt = AseTune.getCounterCollector();
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
				for (CountersModel cm : GetCounters.getCmList())
					cm.close();
				
				// Close the database connection
				AseTune.getCounterCollector().closeMonConnection();
		
				// Close Host Monitor connection
				AseTune.getCounterCollector().closeHostMonConnection();
		
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
		if ( ! AseTune.hasGUI() )
			return;

		_mainTabbedPane.addTab(tcp.getPanelName(), tcp.getIcon(), tcp, tcp.getCm().getDescription());
		_TcpMap.put(tcp.getPanelName(), tcp);
	}

	/**
	 */
	public static JTabbedPane getTabbedPane()
	{
		// We are probably in NO-GUI mode
		if ( ! AseTune.hasGUI() )
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


	/**
	 * Set the FULL TRANSACTION LOG to be visible or not.
	 * @param visible
	 */
	public void setFullTransactionLog(boolean visible, int fullCount)
	{
		if (fullCount > 0)
			_fullTranlogAlert_but.setText(fullCount+" DB Log(s) are full, ");
		else
			_fullTranlogAlert_but.setText("");

		_fullTranlogAlert_but.setVisible(visible);
	}


	/*---------------------------------------------------
	 ** END: implementing ConnectionFactory
	 **---------------------------------------------------
	 */
	public JMenu createPredefinedSqlMenu()
	{
		_logger.debug("createPredefinedSqlMenu(): called.");

		JMenu menu = new JMenu("Predefined SQL Statements");
		menu.setIcon(SwingUtils.readImageIcon(Version.class, "images/pre_defined_sql_statement.png"));

		Configuration systmp = new Configuration();

		//----- sp_list_unused_indexes.sql -----
		systmp.setProperty("system.predefined.sql.01.name",                        "List unused indexes in all databases.");
		systmp.setProperty("system.predefined.sql.01.execute",                     "exec sp_list_unused_indexes");
		systmp.setProperty("system.predefined.sql.01.install.needsVersion",        "0");
		systmp.setProperty("system.predefined.sql.01.install.dbname",              "sybsystemprocs");
		systmp.setProperty("system.predefined.sql.01.install.procName",            "sp_list_unused_indexes");
		systmp.setProperty("system.predefined.sql.01.install.procDateThreshold",   VersionInfo.SP_LIST_UNUSED_INDEXES_CR_STR);
		systmp.setProperty("system.predefined.sql.01.install.scriptLocation",      com.asetune.cm.sql.VersionInfo.class.getName());
		systmp.setProperty("system.predefined.sql.01.install.scriptName",          "sp_list_unused_indexes.sql");
		systmp.setProperty("system.predefined.sql.01.install.needsRole",           "sa_role");

		//----- sp_whoisw.sql -----
		systmp.setProperty("system.predefined.sql.02.name",                        "sp_whoisw - Who Is Working (list SPID's that are doing stuff).");
		systmp.setProperty("system.predefined.sql.02.execute",                     "exec sp_whoisw");
		systmp.setProperty("system.predefined.sql.02.install.needsVersion",        "0");
		systmp.setProperty("system.predefined.sql.02.install.dbname",              "sybsystemprocs");
		systmp.setProperty("system.predefined.sql.02.install.procName",            "sp_whoisw");
		systmp.setProperty("system.predefined.sql.02.install.procDateThreshold",   VersionInfo.SP_WHOISW_CR_STR);
		systmp.setProperty("system.predefined.sql.02.install.scriptLocation",      com.asetune.cm.sql.VersionInfo.class.getName());
		systmp.setProperty("system.predefined.sql.02.install.scriptName",          "sp_whoisw.sql");
		systmp.setProperty("system.predefined.sql.02.install.needsRole",           "sa_role");

		//----- sp_whoisb.sql -----
		systmp.setProperty("system.predefined.sql.03.name",                        "sp_whoisb - Who Is Blocking (list info about SPID's taht are blocking other SPID's from running).");
		systmp.setProperty("system.predefined.sql.03.execute",                     "exec sp_whoisb");
		systmp.setProperty("system.predefined.sql.03.install.needsVersion",        "0");
		systmp.setProperty("system.predefined.sql.03.install.dbname",              "sybsystemprocs");
		systmp.setProperty("system.predefined.sql.03.install.procName",            "sp_whoisb");
		systmp.setProperty("system.predefined.sql.03.install.procDateThreshold",   VersionInfo.SP_WHOISB_CR_STR);
		systmp.setProperty("system.predefined.sql.03.install.scriptLocation",      com.asetune.cm.sql.VersionInfo.class.getName());
		systmp.setProperty("system.predefined.sql.03.install.scriptName",          "sp_whoisb.sql");
		systmp.setProperty("system.predefined.sql.03.install.needsRole",           "sa_role");

		//----- sp_opentran.sql -----
		systmp.setProperty("system.predefined.sql.04.name",                        "sp_opentran - List information about the SPID that is holding the oldest open transaction (using syslogshold).");
		systmp.setProperty("system.predefined.sql.04.execute",                     "exec sp_opentran");
		systmp.setProperty("system.predefined.sql.04.install.needsVersion",        "0");
		systmp.setProperty("system.predefined.sql.04.install.dbname",              "sybsystemprocs");
		systmp.setProperty("system.predefined.sql.04.install.procName",            "sp_opentran");
		systmp.setProperty("system.predefined.sql.04.install.procDateThreshold",   VersionInfo.SP_OPENTRAN_CR_STR);
		systmp.setProperty("system.predefined.sql.04.install.scriptLocation",      com.asetune.cm.sql.VersionInfo.class.getName());
		systmp.setProperty("system.predefined.sql.04.install.scriptName",          "sp_opentran.sql");
		systmp.setProperty("system.predefined.sql.04.install.needsRole",           "sa_role");

		//----- sp_lock2.sql -----
		systmp.setProperty("system.predefined.sql.05.name",                        "sp_lock2 - More or less the same as sp_lock, but uses 'table name' instead of 'table id'.");
		systmp.setProperty("system.predefined.sql.05.execute",                     "exec sp_lock2");
		systmp.setProperty("system.predefined.sql.05.install.needsVersion",        "0");
		systmp.setProperty("system.predefined.sql.05.install.dbname",              "sybsystemprocs");
		systmp.setProperty("system.predefined.sql.05.install.procName",            "sp_lock2");
		systmp.setProperty("system.predefined.sql.05.install.procDateThreshold",   VersionInfo.SP_LOCK2_CR_STR);
		systmp.setProperty("system.predefined.sql.05.install.scriptLocation",      com.asetune.cm.sql.VersionInfo.class.getName());
		systmp.setProperty("system.predefined.sql.05.install.scriptName",          "sp_lock2.sql");
		systmp.setProperty("system.predefined.sql.05.install.needsRole",           "sa_role");

		//----- sp_locksum.sql -----
		systmp.setProperty("system.predefined.sql.06.name",                        "sp_locksum - Prints number of locks each SPID has.");
		systmp.setProperty("system.predefined.sql.06.execute",                     "exec sp_locksum");
		systmp.setProperty("system.predefined.sql.06.install.needsVersion",        "0");
		systmp.setProperty("system.predefined.sql.06.install.dbname",              "sybsystemprocs");
		systmp.setProperty("system.predefined.sql.06.install.procName",            "sp_locksum");
		systmp.setProperty("system.predefined.sql.06.install.procDateThreshold",   VersionInfo.SP_LOCKSUM_CR_STR);
		systmp.setProperty("system.predefined.sql.06.install.scriptLocation",      com.asetune.cm.sql.VersionInfo.class.getName());
		systmp.setProperty("system.predefined.sql.06.install.scriptName",          "ssp_locksum.sqlp_.sql");
		systmp.setProperty("system.predefined.sql.06.install.needsRole",           "sa_role");

		//----- sp_spaceused2.sql -----
		systmp.setProperty("system.predefined.sql.07.name",                        "sp_spaceused2 - List space and row used by each table in the current database");
		systmp.setProperty("system.predefined.sql.07.execute",                     "exec sp_spaceused2");
		systmp.setProperty("system.predefined.sql.07.install.needsVersion",        "15000");
		systmp.setProperty("system.predefined.sql.07.install.dbname",              "sybsystemprocs");
		systmp.setProperty("system.predefined.sql.07.install.procName",            "sp_spaceused2");
		systmp.setProperty("system.predefined.sql.07.install.procDateThreshold",   VersionInfo.SP_SPACEUSED2_CR_STR);
		systmp.setProperty("system.predefined.sql.07.install.scriptLocation",      com.asetune.cm.sql.VersionInfo.class.getName());
		systmp.setProperty("system.predefined.sql.07.install.scriptName",          "sp_spaceused2.sql");
		systmp.setProperty("system.predefined.sql.07.install.needsRole",           "sa_role");

		createPredefinedSqlMenu(menu, "system.predefined.sql.", systmp);
		createPredefinedSqlMenu(menu, "user.predefined.sql.",   null);

		if ( menu.getMenuComponentCount() == 0 )
		{
			_logger.warn("No Menuitems has been assigned for the '"+menu.getText()+"'.");
			return null;
		}
		else
			return menu;
	}

	/**
	 * 
	 * @param menu   if null a new JMenuPopup will be created otherwise it will be appended to it
	 * @param prefix prefix of the property string. Should contain a '.' at the end
	 * @param conf
	 * @return
	 */
	private static JMenu createPredefinedSqlMenu(JMenu menu, String prefix, Configuration conf)
	{
		if (prefix == null)           throw new IllegalArgumentException("prefix cant be null.");
		if (prefix.trim().equals("")) throw new IllegalArgumentException("prefix cant be empty.");
		prefix = prefix.trim();
		if ( ! prefix.endsWith(".") )
			prefix = prefix + ".";

		if (conf == null)
			conf = Configuration.getCombinedConfiguration();

		_logger.debug("createMenu(): prefix='"+prefix+"'.");		

		//Create the menu, if it didnt exists. 
		if (menu == null)
			menu = new JMenu();

		boolean firstAdd = true;
		for (String prefixStr : conf.getUniqueSubKeys(prefix, true))
		{
			_logger.debug("createPredefinedSqlMenu(): found prefix '"+prefixStr+"'.");

			// Read properties
			final String menuItemName      = conf.getProperty(prefixStr   +".name");
			final String sqlStr            = conf.getProperty(prefixStr   +".execute");
			final int    needsVersion      = conf.getIntProperty(prefixStr+".install.needsVersion", 0); 
			final String dbname            = conf.getProperty(prefixStr   +".install.dbname"); 
			final String procName          = conf.getProperty(prefixStr   +".install.procName"); 
			final String procDateThreshStr = conf.getProperty(prefixStr   +".install.procDateThreshold"); 
			final String scriptLocationStr = conf.getProperty(prefixStr   +".install.scriptLocation"); 
			final String scriptName        = conf.getProperty(prefixStr   +".install.scriptName"); 
			final String needsRole         = conf.getProperty(prefixStr   +".install.needsRole"); 

			//---------------------------------------
			// Check that we got everything we needed
			//---------------------------------------
			if (menuItemName == null)
			{
				_logger.warn("Missing property '"+prefixStr+".name'");
				continue;
			}
			if (sqlStr == null)
			{
				_logger.warn("Missing property '"+prefixStr+".execute'");
				continue;
			}

			//---------------------------------------
			// is any "install" there?
			//---------------------------------------
			boolean tmpDoCheckCreate = false;
			if (dbname != null)
			{
				tmpDoCheckCreate = true;

				String missing = "";
				// Check rest of the mandatory parameters
				if (procName          == null) missing += prefixStr   +".install.procName, ";
				if (procDateThreshStr == null) missing += prefixStr   +".install.procDateThreshold, ";
//				if (scriptLocationStr == null) missing += prefixStr   +".install.scriptLocation, ";
				if (scriptName        == null) missing += prefixStr   +".install.scriptName, ";
				
				if ( ! missing.equals("") )
				{
					_logger.warn("Missing property '"+missing+"'.");
					continue;
				}
			}
			final boolean doCheckCreate = tmpDoCheckCreate;

			
			//---------------------------------------
			// if 'install.scriptLocation' is used...
			//---------------------------------------
			Class<?> tmpScriptLocation = null;
			if (scriptLocationStr != null)
			{
				try { tmpScriptLocation = Class.forName(scriptLocationStr); }
				catch (ClassNotFoundException e)
				{
					_logger.warn("Property "+prefixStr+".install.scriptLocation, contained '"+scriptLocationStr+"', the class can't be loaded. This should be a classname in asetune.jar, If it's a path name you want to specify, please use the property '"+prefixStr+".install.scriptName' instead.");
					continue;
				}
			}
			final Class<?> scriptLocation = tmpScriptLocation;

			
			//---------------------------------------
			// if 'install.procDateThreshold' is used...
			//---------------------------------------
			java.util.Date tmpProcDateThreshold = null;
			if (procDateThreshStr != null)
			{
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				try { tmpProcDateThreshold = sdf.parse(procDateThreshStr); }
				catch (ParseException e)
				{
					_logger.warn("Property "+prefixStr+".install.procDateThreshold, contained '"+procDateThreshStr+"', Problems parsing the string, it should look like 'yyyy-MM-dd'.");
					continue;
				}
			}
			final java.util.Date procDateThreshold = tmpProcDateThreshold;

			
			//---------------------------------------
			// Create the executor class
			//---------------------------------------
			ActionListener action = new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					Connection conn = null;
					try
					{
						// Check that we are connected
						if ( ! AseTune.getCounterCollector().isMonConnected(true, true) )
						{
							SwingUtils.showInfoMessage(MainFrame.getInstance(), "Not connected", "Not yet connected to a server.");
							return;
						}
						// Get a new connection
						conn = AseConnectionFactory.getConnection(null, Version.getAppName()+"-PreDefinedSql", null);

						// Check if the procedure exists (and create it if it dosn't)
						if (doCheckCreate)
							AseConnectionUtils.checkCreateStoredProc(conn, needsVersion, dbname, procName, procDateThreshold, scriptLocation, scriptName, needsRole);

						// Open the SQL Window
						QueryWindow qf = new QueryWindow(conn, sqlStr, true, QueryWindow.WindowType.JFRAME);
						qf.openTheWindow();
					}
					catch (Throwable t)
					{
						String msg = "Problems when checking/creating the procedure";
						if (conn == null) 
							msg = "Problems when getting a database connection";

						SwingUtils.showErrorMessage("Problems executing Predefined SQL statement", 
								msg + "\n\n" + t.getMessage(), t);
					}
				}
			};

			JMenuItem menuItem = new JMenuItem(menuItemName);
			menuItem.addActionListener(action);

			if ( firstAdd )
			{
				firstAdd = false;
				if (menu.getMenuComponentCount() > 0)
					menu.addSeparator();
			}
			menu.add(menuItem);
		}

//		menu.addPopupMenuListener( createPopupMenuListener() );

		return menu;
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
		if (type == ST_STATUS2_FIELD)   return _statusStatus2.getText();
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
				_statusStatus2   .setText(ST_DEFAULT_STATUS2_FIELD);
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
			if (AseTune.getCounterCollector().isMonConnected())
			{
				_statusStatus    .setText("Just Connected...");
				_statusStatus2   .setText(ST_DEFAULT_STATUS2_FIELD);

//				_statusServerName.setText(
//						AseConnectionFactory.getServer() + " (" +
//						AseConnectionFactory.getHost()   + ":" +
//						AseConnectionFactory.getPort()   + ")"
//						);
				_statusServerName.setText(
						AseConnectionFactory.getServer() +
						" (" + AseConnectionFactory.getHostPortStr() + ")" );

				Connection conn = AseTune.getCounterCollector().getMonConnection();
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
			_statusStatus2        .setText(ST_DEFAULT_STATUS2_FIELD);

			_summaryPanel.setLocalServerName("");

			// SET WATERMARK
			SummaryPanel.getInstance().setWatermark();

			for (TabularCntrPanel tcp : _TcpMap.values())
			{
				tcp.setWatermark();
			}
		}

		// STATUS
		if (type == ST_STATUS_FIELD)
		{
			_statusStatus.setText(param);
		}
		
		// STATUS2
		if (type == ST_STATUS2_FIELD)
		{
			// For example if we are in CounterModel.refreshGetData(conn)... "updateGui": fireTable*
			// the status update isn't visible until later anyway...
			// so this will probably never work... 
			// I have tried _statusStatus2.paintImmediately(_statusStatus2.getBounds());
			// but id did not paint stuff...
			_statusStatus2.setText(param);
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
				reader.addNotificationListener(_offlineView);

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
		String sql = _udTooltipMap.get(cmName + "." + colName);

		if (sql == null)
			sql = _udTooltipMap.get(colName);

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

	public static boolean isInMemoryViewOn()
	{
		return _instance._viewStorage_chk.isSelected();
	}

	/**
	 * Load the JTabbedPane tab order and which of the tabs should be visible 
	 */
	public void loadTabOrderAndVisibility()
	{
//		Configuration tmpConf = Configuration.getInstance(Configuration.TEMP);
		Configuration tmpConf = Configuration.getCombinedConfiguration();

		// JTabbedPane tab order and visibility
		String orderAndVisibility = tmpConf.getProperty("main.tab.orderAndVisibility");
		if (orderAndVisibility != null)
		{
			_logger.info("Loading TabbedPane 'Tab Titles' order and visibility using property 'main.tab.orderAndVisibility', which looks like '"+orderAndVisibility+"'.");
			_logger.info("To change/remove the sorting use the menu 'Counter Tab View'. OR to get rid of this sorting simply delete the entry 'main.tab.orderAndVisibility' in the file '"+tmpConf.getFilename()+"'.");
			_mainTabbedPane.setTabOrderAndVisibility(orderAndVisibility);
		}
	}
	//----------------------------------------------------------------------------
	// BEGIN - implementing interface: GTabbedPane.TabOrderAndVisibilityListener
	@Override
	public void saveTabOrderAndVisibility(String tabOptions)
	{
		Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
		if (tmpConf != null)
		{
			_logger.info("Saving TabbedPane 'Tab Titles' order and visibility using property 'main.tab.orderAndVisibility', which looks like '"+tabOptions+"'.");
			tmpConf.setProperty("main.tab.orderAndVisibility", tabOptions);
			tmpConf.save();
		}
	}
	@Override
	public void removeTabOrderAndVisibility()
	{
		Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
		_logger.info("Removing the ordering and visibility entry 'main.tab.orderAndVisibility' in the file '"+tmpConf.getFilename()+"'.");
		tmpConf.remove("main.tab.orderAndVisibility");
		tmpConf.save();
	}
	// END   - implementing interface: GTabbedPane.TabOrderAndVisibilityListener
	//----------------------------------------------------------------------------

//	/**
//	 * Set Busy/wait cusor or the default cursor.
//	 */
//	public void setBusyCursor(boolean to)
//	{
//System.out.println("MainFrame: Setting busy cursor to: "+to);
//		Cursor cursor = to ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor();
//		Frame.getFrames()[0].setCursor(cursor);
//	}

	/*---------------------------------------------------
	** END: public methods
	**---------------------------------------------------
	*/
	static
	{
//		Configuration conf = Configuration.getInstance(Configuration.CONF);
		Configuration conf = Configuration.getCombinedConfiguration();
		if (conf != null)
		{
		}

		String prefix = "table.tooltip.";
		for (String key : conf.getKeys(prefix))
		{
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
		for (TabularCntrPanel tcp : _TcpMap.values())
		{
			tcp.saveProps();
		}
	}

	private void saveProps()
	{
		//_logger.debug("xxxxx: " + e.getWindow().getSize());
		Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);

		tmpConf.setProperty("main.refresh.interval", _refreshInterval);
		tmpConf.setProperty("nogui.sleepTime",       _refreshNoGuiInterval);

		tmpConf.setProperty("TabularCntrPanel.autoAdjustTableColumnWidth", _autoResizePcTable_mi.isSelected());

		tmpConf.setProperty("window.width",  getSize().width);
		tmpConf.setProperty("window.height", getSize().height);
		if (isVisible())
		{
			tmpConf.setProperty("window.pos.x",  getLocationOnScreen().x);
			tmpConf.setProperty("window.pos.y",  getLocationOnScreen().y);
		}
		
		getSummaryPanel().saveLayoutProps();

		tmpConf.save();
	}

	private void loadProps()
	{
//		Configuration tmpConf = Configuration.getInstance(Configuration.TEMP);
		Configuration tmpConf = Configuration.getCombinedConfiguration();

		Dimension crtSize = Toolkit.getDefaultToolkit().getScreenSize();

		int width   = tmpConf.getIntProperty("window.width",  crtSize.width  - 200);
		int height  = tmpConf.getIntProperty("window.height", crtSize.height - 200);
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

		boolean bool = tmpConf.getBooleanProperty("TabularCntrPanel.autoAdjustTableColumnWidth", _autoResizePcTable_mi.isSelected());
		_autoResizePcTable_mi.setSelected(bool);

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

//	private class OfflineReadWatermark
//    extends AbstractComponentDecorator
//    {
//		public OfflineReadWatermark(JComponent target, String text)
//		{
//			super(target);
//			if (text != null)
//				_text = text;
//		}
//		private String		_text	= "";
//		private Graphics2D	g		= null;
//		private Rectangle	r		= null;
//		
//		private JLabel      lbl     = new JLabel();
//	
//		public void paint(Graphics graphics)
//		{
//			if (_text == null || _text != null && _text.equals(""))
//				return;
//	
//			r = getDecorationBounds();
//			g = (Graphics2D) graphics;
//			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//			g.setFont(lbl.getFont());
//	
//			FontMetrics fm = g.getFontMetrics();
//			int strWidth = fm.stringWidth(_text);
//			int xPos = (r.width - strWidth) / 2;
//			int yPos = (int) r.height - 5;
//			xPos=10;
//			yPos=10;
//			g.drawString(_text, xPos, yPos);
//		}
//	
//		public void setWatermarkText(String text)
//		{
//System.out.println("OfflineReadWatermark.setWatermarkText()="+text);
//			_text = text;
//			_logger.debug("setWatermarkText: to '" + _text + "'.");
//			repaint();
//		}
//    }


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
