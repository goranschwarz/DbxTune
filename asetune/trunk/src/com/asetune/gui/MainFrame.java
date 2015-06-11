/**
 */

package com.asetune.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.apache.log4j.lf5.LogLevel;

import com.asetune.CounterCollectorThreadGui;
import com.asetune.CounterController;
import com.asetune.DbxTune;
import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.Version;
import com.asetune.check.CheckForUpdates;
import com.asetune.check.CheckForUpdatesDbx.DbxConnectInfo;
import com.asetune.check.MailGroupDialog;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.CmBlocking;
import com.asetune.cm.ase.CmOpenDatabases;
import com.asetune.config.dbms.DbmsConfigManager;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.config.ui.AseConfigMonitoringDialog;
import com.asetune.config.ui.DbmsConfigViewDialog;
import com.asetune.gui.ConnectionDialog.Options;
import com.asetune.gui.swing.AbstractComponentDecorator;
import com.asetune.gui.swing.GTabbedPane;
import com.asetune.gui.swing.GTabbedPane.TabOrderAndVisibilityListener;
import com.asetune.gui.swing.GTabbedPaneViewDialog;
import com.asetune.gui.swing.Screenshot;
import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.gui.swing.WaitForExecDialog.BgExecutor;
import com.asetune.gui.wizard.WizardOffline;
import com.asetune.gui.wizard.WizardUserDefinedCm;
import com.asetune.pcs.InMemoryCounterHandler;
import com.asetune.pcs.PersistContainer;
import com.asetune.pcs.PersistReader;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.tools.WindowType;
import com.asetune.tools.sqlw.QueryWindow;
import com.asetune.tools.tailw.LogTailWindow;
import com.asetune.ui.rsyntaxtextarea.AsetuneTokenMaker;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.DbUtils;
import com.asetune.utils.Memory;
import com.asetune.utils.PropPropEntry;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;
import com.asetune.utils.Ver;


//public class MainFrame
public abstract class MainFrame
    extends JFrame
    implements IGuiController, ActionListener, ChangeListener, TableModelListener, TabOrderAndVisibilityListener, PersistentCounterHandler.PcsQueueChange, ConnectionProvider, Memory.MemoryListener
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
	private static int          _lastKnownStatus           = -1;

	private static final String ST_DEFAULT_STATUS_FIELD    = "Not Connected";
	private static final String ST_DEFAULT_STATUS2_FIELD   = "";
	private static final String ST_DEFAULT_SERVER_NAME     = "user - ASENAME (host:port)";
	private static final String ST_DEFAULT_SERVER_LISTENERS= "Server listens on address";

	//-------------------------------------------------
	// GROUPS for JTabbedPane
	public static final String    TCP_GROUP_OBJECT_ACCESS  = "Object/Access";
	public static final String    TCP_GROUP_SERVER         = "Server";
	public static final String    TCP_GROUP_CACHE          = "Cache";
	public static final String    TCP_GROUP_DISK           = "Disk";
	public static final String    TCP_GROUP_REP_AGENT      = "RepAgent";
	public static final String    TCP_GROUP_HOST_MONITOR   = "Host Monitor";
	public static final String    TCP_GROUP_UDC            = "User Defined";
	
	public static final ImageIcon TCP_GROUP_ICON_OBJECT_ACCESS = SwingUtils.readImageIcon(Version.class, "images/tcp_group_icon_object_access.png");
	public static final ImageIcon TCP_GROUP_ICON_SERVER        = SwingUtils.readImageIcon(Version.class, "images/tcp_group_icon_server.png");
	public static final ImageIcon TCP_GROUP_ICON_DISK          = SwingUtils.readImageIcon(Version.class, "images/tcp_group_icon_disk.png");
	public static final ImageIcon TCP_GROUP_ICON_CACHE         = SwingUtils.readImageIcon(Version.class, "images/tcp_group_icon_caches.png");
	public static final ImageIcon TCP_GROUP_ICON_REP_AGENT     = SwingUtils.readImageIcon(Version.class, "images/tcp_group_icon_repagent.png");
	public static final ImageIcon TCP_GROUP_ICON_HOST_MONITOR  = SwingUtils.readImageIcon(Version.class, "images/tcp_group_icon_host_monitor.png");
	public static final ImageIcon TCP_GROUP_ICON_UDC           = SwingUtils.readImageIcon(Version.class, "images/tcp_group_icon_udc.png");

	//-------------------------------------------------
	// PROPERTIES KEYS
	public static final String    PROPKEY_refreshInterval              = "main.refresh.interval";
	public static final int       DEFAULT_refreshInterval              = 10; // NOTE use: getDefaultRefreshIntervall() to get this, which depends on different implementations

	public static final String    PROPKEY_refreshIntervalNoGui         = "nogui.sleepTime";
	public static final int       DEFAULT_refreshIntervalNoGui         = 60;

	public static final String    PROPKEY_useTcpGroups                 = "MainFrame.useTcpGroups";
	public static final boolean   DEFAULT_useTcpGroups                 = true;

	public static final String    PROPKEY_doJavaGcAfterXMinutes        = "do.java.gc.after.x.minutes";	
	public static final boolean   DEFAULT_doJavaGcAfterXMinutes        = true;

	public static final String    PROPKEY_doJavaGcAfterXMinutesValue   = "do.java.gc.after.x.minutes.value";	
	public static final int       DEFAULT_doJavaGcAfterXMinutesValue   = 10;

	public static final String    PROPKEY_doJavaGcAfterRefresh         = "do.java.gc.after.refresh";	
	public static final boolean   DEFAULT_doJavaGcAfterRefresh         = false;

	public static final String    PROPKEY_doJavaGcAfterRefreshShowGui  = "do.java.gc.after.refresh.show.gui";	
	public static final boolean   DEFAULT_doJavaGcAfterRefreshShowGui  = true;

	public static final String    PROPKEY_summaryOperations_showAbs    = "MainFrame.summary.operations.show.abs";
	public static final boolean   DEFAULT_summaryOperations_showAbs    = false;

	public static final String    PROPKEY_summaryOperations_showDiff   = "MainFrame.summary.operations.show.diff";
	public static final boolean   DEFAULT_summaryOperations_showDiff   = true;

	public static final String    PROPKEY_summaryOperations_showRate   = "MainFrame.summary.operations.show.rate";
	public static final boolean   DEFAULT_summaryOperations_showRate   = true;

	public static final String    PROPKEY_setStatus_invokeAndWait      = "MainFrame.notInEdt.setStatus.invokeAndWait";
	public static final boolean   DEFAULT_setStatus_invokeAndWait      = true;

	static
	{
		Configuration.registerDefaultValue(PROPKEY_useTcpGroups,                DEFAULT_useTcpGroups);
		Configuration.registerDefaultValue(PROPKEY_doJavaGcAfterXMinutes,       DEFAULT_doJavaGcAfterXMinutes);
		Configuration.registerDefaultValue(PROPKEY_doJavaGcAfterXMinutesValue,  DEFAULT_doJavaGcAfterXMinutesValue);
		Configuration.registerDefaultValue(PROPKEY_doJavaGcAfterRefresh,        DEFAULT_doJavaGcAfterRefresh);
		Configuration.registerDefaultValue(PROPKEY_doJavaGcAfterRefreshShowGui, DEFAULT_doJavaGcAfterRefreshShowGui);

		Configuration.registerDefaultValue(PROPKEY_summaryOperations_showAbs,   DEFAULT_summaryOperations_showAbs);
		Configuration.registerDefaultValue(PROPKEY_summaryOperations_showDiff,  DEFAULT_summaryOperations_showDiff);
		Configuration.registerDefaultValue(PROPKEY_summaryOperations_showRate,  DEFAULT_summaryOperations_showRate);
	}

	//-------------------------------------------------
	// Actions
	public static final String ACTION_CONNECT                           = "CONNECT";
	public static final String ACTION_DISCONNECT                        = "DISCONNECT";
	public static final String ACTION_EXIT                              = "EXIT";

	public static final String ACTION_OPEN_LOG_VIEW                     = "OPEN_LOG_VIEW";
	public static final String ACTION_OPEN_OFFLINE_SESSION_VIEW         = "OPEN_OFFLINE_SESSION_VIEW";
	public static final String ACTION_TOGGLE_AUTO_RESIZE_PC_TABLES      = "TOGGLE_AUTO_RESIZE_PC_TABLES";
	public static final String ACTION_TOGGLE_AUTO_REFRESH_ON_TAB_CHANGE = "TOGGLE_AUTO_REFRESH_ON_TAB_CHANGE";
	public static final String ACTION_GROUP_TCP_IN_TAB_PANE             = "GROUP_TCP_IN_TAB_PANE";
	public static final String ACTION_DO_JAVA_GC_AFTER_X_MINUTES        = "DO_JAVA_GC_AFTER_X_MINUTES";
	public static final String ACTION_DO_JAVA_GC_AFTER_X_MINUTES_VALUE  = "DO_JAVA_GC_AFTER_X_MINUTES_VALUE";
	public static final String ACTION_DO_JAVA_GC_AFTER_REFRESH          = "DO_JAVA_GC_AFTER_REFRESH";
	public static final String ACTION_DO_JAVA_GC_AFTER_REFRESH_SHOW_GUI = "DO_JAVA_GC_AFTER_REFRESH_SHOW_GUI";
	public static final String ACTION_SUMMARY_OPERATIONS_TOGGLE         = "SUMMARY_OPERATIONS_TOGGLE";
	public static final String ACTION_OPEN_REFRESH_RATE                 = "OPEN_REFRESH_RATE";
	public static final String ACTION_OPEN_COUNTER_TAB_VIEW             = "OPEN_COUNTER_TAB_VIEW";
	public static final String ACTION_OPEN_GRAPH_GRAPH_VIEW             = "OPEN_GRAPH_GRAPH_VIEW";
	public static final String ACTION_OPEN_DBMS_CONFIG_VIEW             = "ACTION_OPEN_DBMS_CONFIG_VIEW";
	public static final String ACTION_OPEN_TCP_PANEL_CONFIG             = "OPEN_TCP_PANEL_CONFIG";

//	public static final String ACTION_OPEN_ASE_CONFIG_MON               = "OPEN_ASE_CONFIG_MON";
//	public static final String ACTION_OPEN_CAPTURE_SQL                  = "OPEN_CAPTURE_SQL";
//	public static final String ACTION_OPEN_ASE_APP_TRACE                = "OPEN_ASE_APP_TRACE";
//	public static final String ACTION_OPEN_ASE_STACKTRACE_TOOL          = "OPEN_ASE_STACKTRACE_TOOL";
	public static final String ACTION_OPEN_DDL_VIEW                     = "OPEN_DDL_VIEW";
	public static final String ACTION_OPEN_SQL_QUERY_WIN                = "OPEN_SQL_QUERY_WIN";
	public static final String ACTION_OPEN_LOCK_TOOL                    = "OPEN_LOCK_TOOL";
	public static final String ACTION_OPEN_WIZARD_OFFLINE               = "OPEN_WIZARD_OFFLINE";
	public static final String ACTION_OPEN_WIZARD_UDCM                  = "OPEN_WIZARD_UDCM";
	public static final String ACTION_GARBAGE_COLLECT                   = "GARBAGE_COLLECT";

	public static final String ACTION_OPEN_ABOUT                        = "OPEN_ABOUT";
	
	public static final String ACTION_VIEW_LOG_TAIL                     = "VIEW_LOG_TAIL";
	public static final String ACTION_VIEW_STORAGE                      = "ACTION_VIEW_STORAGE";

	public static final String ACTION_REFRESH_NOW                       = "REFRESH_NOW";

	public static final String ACTION_GOTO_BLOCKED_TAB                  = "GOTO_BLOCKED_TAB";
	public static final String ACTION_GOTO_DATABASE_TAB                 = "GOTO_DATABASE_TAB";

	public static final String ACTION_OUT_OF_MEMORY                     = "OUT_OF_MEMORY";
	public static final String ACTION_LOW_ON_MEMORY                     = "LOW_ON_MEMORY";
	
	public static final String ACTION_SCREENSHOT                        = "SCREENSHOT";
	public static final String ACTION_SAMPLING_PAUSE                    = "SAMPLING_PAUSE";
	public static final String ACTION_SAMPLING_PLAY                     = "SAMPLING_PLAY";
	public static final String ACTION_CHANGE_SAMPLE_INTERVAL            = "CHANGE_SAMPLE_INTERVAL";
	
	public static final String ACTION_TAB_SELECTOR                      = "TAB_SELECTOR";
	public static final String ACTION_CM_NAVIGATOR_PREV                 = "CM_NAVIGATOR_PREV";
	public static final String ACTION_CM_NAVIGATOR_NEXT                 = "CM_NAVIGATOR_NEXT";

	public static final String ACTION_SLIDER_LEFT                       = "SLIDER_LEFT";
	public static final String ACTION_SLIDER_RIGHT                      = "SLIDER_RIGHT";
	public static final String ACTION_SLIDER_LEFT_LEFT                  = "SLIDER_LEFT_LEFT";
	public static final String ACTION_SLIDER_RIGHT_RIGHT                = "SLIDER_RIGHT_RIGHT";
	public static final String ACTION_SLIDER_LEFT_NEXT                  = "SLIDER_LEFT_NEXT";
	public static final String ACTION_SLIDER_RIGHT_NEXT                 = "SLIDER_RIGHT_NEXT";

	private String _inActionCommand = null;
	private PersistContainer _currentPc      = null;

	private LinkedList<String> _cmNavigatorPrevStack  = new LinkedList<String>(); // last selected tab is first in the list.
	private LinkedList<String> _cmNavigatorNextStack  = new LinkedList<String>(); // add an entry every time you press "prev" so we know where to go if "next" is pushed. Reset every time a "normal" tab selection is done 
	private int                _cmNavigatorStackLevel = 0;
	
	//-------------------------------------------------
	// Menus / toolbar

	private JToolBar            _toolbar                       = new JToolBar();
//	private JToolBar            _pcsNavigation                 = new JToolBar();
//	private JCheckBox           _viewStorage_chk               = new JCheckBox("View Stored Data", false);
	private JCheckBox           _viewStorage_chk               = new JCheckBox();
	private JSlider             _readSlider                    = new JSlider();
	private ReadTsWatermark     _readTsWatermark               = null;
	private Timer               _readSelectionTimer            = new Timer(100, new ReadSelectionTimerAction(this));
	private JSlider             _offlineSlider                 = new JSlider();
	private ReadTsWatermark     _offlineTsWatermark            = null;
//	private OfflineReadWatermark _offlineReadWatermark         = null;
	private Timer               _offlineSelectionTimer         = new Timer(100, new OfflineSelectionTimerAction(this));

	private JComboBox           _workspace_cbx                 = new JComboBox();
	
//	private JMenuBar            _main_mb                       = new JMenuBar();
//
//	// File
//	private JMenu               _file_m                        = new JMenu("File");
//	private JMenuItem           _connect_mi                    = new JMenuItem("Connect...");
//	private JMenuItem           _disconnect_mi                 = new JMenuItem("Disconnect");
//	private JMenuItem           _exit_mi                       = new JMenuItem("Exit");
//
//	// View
//	private JMenu               _view_m                        = new JMenu("View");
//	private JMenuItem           _logView_mi                    = new JMenuItem("Open Log Window...");
//	private JMenuItem           _viewSrvLogFile_mi             = new JMenuItem("View/Tail the ASE Server Log File");
//	private JMenuItem           _offlineSessionsView_mi        = new JMenuItem("Offline Sessions Window...");
//	private JMenu               _preferences_m                 = new JMenu("Preferences");
//	private JMenuItem           _refreshRate_mi                = new JMenuItem("Refresh Rate...");
//	private JCheckBoxMenuItem   _autoResizePcTable_mi          = new JCheckBoxMenuItem("Auto Resize Column Width in Performance Counter Tables", false);
//	private JCheckBoxMenuItem   _autoRefreshOnTabChange_mi     = new JCheckBoxMenuItem("Auto Refresh when you change Performance Counter Tab", false);
//	private JCheckBoxMenuItem   _groupTcpInTabPane_mi          = new JCheckBoxMenuItem("Group Performance Counters in Tabular Panels", useTcpGroups());
//	private JMenu               _optDoGc_m                     = new JMenu("Do Java Garbage Collect");
//	private JCheckBoxMenuItem   _optDoGcAfterXMinutes_mi       = new JCheckBoxMenuItem("Do Java Garbage Collect, every X Minutes", DEFAULT_doJavaGcAfterXMinutes);
//	private JMenuItem           _optDoGcAfterXMinutesValue_mi  = new JMenuItem        ("Do Java Garbage Collect, every X Minute, Change Interval...");
//	private JCheckBoxMenuItem   _optDoGcAfterRefresh_mi        = new JCheckBoxMenuItem("Do Java Garbage Collect, after counters has been refreshed", DEFAULT_doJavaGcAfterRefresh);
//	private JCheckBoxMenuItem   _optDoGcAfterRefreshShowGui_mi = new JCheckBoxMenuItem("Do Java Garbage Collect, after counters has been refreshed, Show GUI Dialog", DEFAULT_doJavaGcAfterRefreshShowGui);
//	private JCheckBoxMenuItem   _optSummaryOperShowAbs_mi      = new JCheckBoxMenuItem("Show Absolute Counters for Summary Operations",   DEFAULT_summaryOperations_showAbs);
//	private JCheckBoxMenuItem   _optSummaryOperShowDiff_mi     = new JCheckBoxMenuItem("Show Difference Counters for Summary Operations", DEFAULT_summaryOperations_showDiff);
//	private JCheckBoxMenuItem   _optSummaryOperShowRate_mi     = new JCheckBoxMenuItem("Show Rate Counters for Summary Operations",       DEFAULT_summaryOperations_showRate);
//	private JMenuItem           _aseConfigView_mi              = new JMenuItem("View ASE Configuration...");
//	private JMenuItem           _tcpSettingsConf_mi            = new JMenuItem("Change 'Performance Counter' Options...");
//	private JMenuItem           _counterTabView_mi             = new JMenuItem("Change 'Tab Titles' Order and Visibility...");
//	private JMenuItem           _graphView_mi                  = new JMenuItem("Change 'Graph' Order and Visibility...");
//	private JMenu               _graphs_m                      = new JMenu("Active Graphs");
//	
//	// Tools
//	private JMenu               _tools_m                       = new JMenu("Tools");
//	private JMenuItem           _aseConfMon_mi                 = new JMenuItem("Configure ASE for Monitoring...");
//	private JMenuItem           _captureSql_mi                 = new JMenuItem("Capture SQL...");
//	private JMenuItem           _aseAppTrace_mi                = new JMenuItem("ASE Application Tracing...");
//	private JMenuItem           _aseStackTraceAnalyzer_mi      = new JMenuItem("ASE StackTrace Analyzer...");
//	private JMenuItem           _ddlView_mi                    = new JMenuItem("DDL Viewer...");
//	private JMenu               _preDefinedSql_m               = null;
//	private JMenuItem           _sqlQuery_mi                   = new JMenuItem("SQL Query Window...");
////	private JMenuItem           _lockTool_mi                   = new JMenuItem("Lock Tool (NOT YET IMPLEMENTED)");
//	private JMenuItem           _createOffline_mi              = new JMenuItem("Create 'Record Session - Template file' Wizard...");
//	private JMenuItem           _wizardCrUdCm_mi               = new JMenuItem("Create 'User Defined Counter' Wizard...");
//	private JMenuItem           _doGc_mi                       = new JMenuItem("Java Garbage Collection");
//
//	// Help
//	private JMenu               _help_m                        = new JMenu("Help");
//	private JMenuItem           _about_mi                      = new JMenuItem("About");

	private JMenuBar            _main_mb;

	// File
	private JMenu               _file_m;
	private JMenuItem           _connect_mi;
	private JMenuItem           _disconnect_mi;
	private JMenuItem           _exit_mi;

	// View
	private JMenu               _view_m;
	private JMenuItem           _logView_mi;
	private JMenuItem           _dbmsConfigView_mi;
	private JMenuItem           _offlineSessionsView_mi;
	private JMenu               _preferences_m;
	private JMenuItem           _refreshRate_mi;
	private JCheckBoxMenuItem   _autoResizePcTable_mi;
	private JCheckBoxMenuItem   _autoRefreshOnTabChange_mi;
	private JCheckBoxMenuItem   _groupTcpInTabPane_mi;
	private JMenu               _optDoGc_m;
	private JCheckBoxMenuItem   _optDoGcAfterXMinutes_mi;
	private JMenuItem           _optDoGcAfterXMinutesValue_mi;
	private JCheckBoxMenuItem   _optDoGcAfterRefresh_mi;
	private JCheckBoxMenuItem   _optDoGcAfterRefreshShowGui_mi;
	private JCheckBoxMenuItem   _optSummaryOperShowAbs_mi;
	private JCheckBoxMenuItem   _optSummaryOperShowDiff_mi;
	private JCheckBoxMenuItem   _optSummaryOperShowRate_mi;
	private JMenuItem           _tcpSettingsConf_mi;
	private JMenuItem           _counterTabView_mi;
	private JMenuItem           _graphView_mi;
	private JMenu               _graphs_m;
	
	// Tools
	private JMenu               _tools_m;
//	private JMenuItem           _aseConfMon_mi;
	private JMenuItem           _viewSrvLogFile_mi;
//	private JMenuItem           _captureSql_mi;
//	private JMenuItem           _aseAppTrace_mi;
//	private JMenuItem           _aseStackTraceAnalyzer_mi;
	private JMenuItem           _ddlView_mi;
//	private JMenu               _preDefinedSql_m;
	private JMenuItem           _sqlQuery_mi;
//	private JMenuItem           _lockTool_mi;
	private JMenuItem           _createOffline_mi;
	private JMenuItem           _wizardCrUdCm_mi;
	private JMenuItem           _doGc_mi;

	// Help
	private JMenu               _help_m;
	private JMenuItem           _about_mi;

	private static GTabbedPane  _mainTabbedPane                = new XGTabbedPane("MainFrame_MainTabbedPane");

	//-------------------------------------------------
	// STATUS Panel
	private JPanel                    _statusPanel               = new JPanel();
	private JButton                   _blockAlert_but            = new JButton();
	private JButton                   _fullTranlogAlert_but      = new JButton();
	private JButton                   _oldestOpenTran_but        = new JButton();
	private JButton                   _refreshNow_but            = new JButton();
	private JButton                   _samplePause_but           = new JButton();
	private JButton                   _samplePlay_but            = new JButton();
	private JComboBox                 _sampleInterval_cbx        = new JComboBox(new Integer[] {5, 10, 15, 20, 30, 45, 60, 90, 60*2, 60*5, 60*10, 60*30, 60*60});
	private JButton                   _gcNow_but                 = new JButton();
	private static JLabel             _statusStatus              = new JLabel(ST_DEFAULT_STATUS_FIELD);
	private static JLabel             _statusStatus2             = new JLabel(ST_DEFAULT_STATUS2_FIELD);
	private static JLabel             _statusServerName          = new JLabel(ST_DEFAULT_SERVER_NAME);
	private static JLabel             _statusServerListeners     = new JLabel(ST_DEFAULT_SERVER_LISTENERS);
	private static JLabel             _statusMemory              = new JLabel("JVM Memory Usage");
	private static JLabel             _statusPcsQueueSize          = new JLabel();
	private static JLabel             _statusPcsPersistInfo        = new JLabel();
	private static JLabel             _statusPcsDdlLookupQueueSize = new JLabel();
	private static JLabel             _statusPcsDdlStoreQueueSize  = new JLabel();

	//-------------------------------------------------
	// Toolbar Panel
	private JButton                   _connectTb_but             = null;
	private JButton                   _disConnectTb_but          = null;
	private JButton                   _screenshotTb_but          = null;
	private JButton                   _samplePauseTb_but         = new JButton();
	private JButton                   _samplePlayTb_but          = new JButton();
	private JButton                   _prevCmNavigator_but       = null;
	private JButton                   _nextCmNavigator_but       = null;

	//-------------------------------------------------
	// Other members
	private static MainFrame          _instance                  = null;
	private static Log4jViewer        _logView                   = null;
//	private static Connection         _conn                      = null;
//	private static long               _lastIsClosedCheck         = 0;
//	private static long               _lastIsClosedRefreshTime   = 1200;
//	private static Connection         _offlineConn               = null;
	private static DbxConnection      _offlineConn               = null;
	private static long               _lastOfflineIsClosedCheck         = 0;
	private static long               _lastOfflineIsClosedRefreshTime   = 1200;
	private static OfflineSessionVeiwer _offlineView             = null;
	private static int                _refreshInterval           = 10;
	private static int                _refreshNoGuiInterval      = 60;
	private static long               _guiInitTime               = 0;

	private static String             _offlineSamplePeriod       = null;

	private static boolean            _isSamplingPaused          = false;
	private static boolean            _isForcedRefresh           = false;

	private static String _connectedToProductName = null;

	/** Keep a list of all TabularCntrPanel that you have initialized */
	private static Map<String,TabularCntrPanel> _TcpMap           = new HashMap<String,TabularCntrPanel>();
//	private static SummaryPanel       _summaryPanel               = new SummaryPanel();
	private static TabularCntrPanel   _currentPanel               = null;

	/** Keep a list of user defined SQL statements that will be used for tooltip on a JTable cell level */
	private static HashMap<String,String> _udTooltipMap = new HashMap<String,String>();

	/** String to append at the end of the setTitle() */
	private String _windowTitleAppend = null;

	/** DDL Viewer GUI */
	private DdlViewer _ddlViewer = null;
	
	/** set to true at the end of initialization */
	private boolean _initialized = false;
	//-------------------------------------------------

	public static boolean useTcpGroups()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		return conf.getBooleanProperty(PROPKEY_useTcpGroups, DEFAULT_useTcpGroups);
	}

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

		// Install some extra Syntax Highlighting for RCL and TSQL
		AsetuneTokenMaker.init();  

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
//				CheckForUpdates.sendCounterUsageInfo(true);
//				sendCounterUsageInfo(true);
				CheckForUpdates.getInstance().sendCounterUsageInfo(true);
				_logger.debug("----End Shutdown Hook");
			}
		});

		// ADD this to the out of memory listener, which is started in AseTune.java
		Memory.addMemoryListener(this);
		
		_initialized = true;
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
	
	@Override
	public boolean hasGUI()
	{
		return DbxTune.hasGui();
	}

	@Override
	public void splashWindowProgress(String msg)
	{
		SplashWindow.drawProgress(msg);
	}

	
	/*---------------------------------------------------
	** BEGIN: xxx
	**---------------------------------------------------
	*/
	@Override
	public void setVisible(boolean visible)
	{
		_guiInitTime = System.currentTimeMillis();

		// Join mailing list, show this after the window is visible
		Runnable doLater = new Runnable()
		{
			@Override
			public void run()
			{
				MailGroupDialog.showDialog(MainFrame.getInstance());
			}
		};
		SwingUtilities.invokeLater(doLater);

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
	public abstract ImageIcon getApplicationIcon16();
	public abstract ImageIcon getApplicationIcon32();

	public ArrayList<Image> getApplicationIcons()
	{
		ArrayList<Image> iconList = new ArrayList<Image>();

		ImageIcon icon16 = getApplicationIcon16();
		ImageIcon icon32 = getApplicationIcon32();

		if (icon16 != null) iconList.add(icon16.getImage());
		if (icon32 != null) iconList.add(icon32.getImage());

		return iconList;
	}

	protected void initComponents() 
	{
		setTitle(Version.getAppName(), null);

		// Set icons
		setIconImages(getApplicationIcons());

		Configuration.registerDefaultValue(PROPKEY_refreshInterval,         getDefaultRefreshInterval());
		Configuration.registerDefaultValue(PROPKEY_refreshIntervalNoGui,    DEFAULT_refreshIntervalNoGui);


		//-------------------------------------------------------------------------
		// HARDCODE a STOP date when this "DEVELOPMENT VERSION" will STOP working
		//-------------------------------------------------------------------------
		if (Version.IS_DEVELOPMENT_VERSION)
		{
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			//setTitle(Version.getAppName()+" - This TEMPORARY DEVELOPMENT VERSION will NOT work after '"+df.format(Version.DEV_VERSION_EXPIRE_DATE)+"'.");
			//setTitle(Version.getAppName()+" - This DEVELOPMENT VERSION will NOT work after '"+df.format(Version.DEV_VERSION_EXPIRE_DATE)+"', then you will have to download a later version.");
			//setTitle(Version.getAppName()+" - DEVELOPMENT VERSION, after '"+df.format(Version.DEV_VERSION_EXPIRE_DATE)+"' you will have to download a later version.");

			_windowTitleAppend = "This DEVELOPMENT VERSION will NOT work after '"+df.format(Version.DEV_VERSION_EXPIRE_DATE)+"', then you will have to download a later version.";
		}
		if (DbxTune.hasDevVersionExpired())
		{
			//setTitle(Version.getAppName()+" - In Development Version, \"time to live date\" has EXPIRED, only 'Persistent Counter Storage - READ' is enabled.");
			_windowTitleAppend = "In Development Version, \"time to live date\" has EXPIRED, only 'Persistent Counter Storage - READ' is enabled.";
		}
		setTitle(Version.getAppName(), null);

		
		JPanel contentPane = (JPanel) this.getContentPane();
		contentPane.setLayout(new BorderLayout());

		//--------------------------
		// MENU - Icons
//		_connect_mi                   .setIcon(SwingUtils.readImageIcon(Version.class, "images/connect_16.png"));
//		_disconnect_mi                .setIcon(SwingUtils.readImageIcon(Version.class, "images/disconnect_16.png"));
//		_exit_mi                      .setIcon(SwingUtils.readImageIcon(Version.class, "images/close.gif"));
//
//		_logView_mi                   .setIcon(SwingUtils.readImageIcon(Version.class, "images/log_viewer.gif"));
//		_viewSrvLogFile_mi            .setIcon(SwingUtils.readImageIcon(Version.class, "images/tail_logfile.png"));
//		_offlineSessionsView_mi       .setIcon(SwingUtils.readImageIcon(Version.class, "images/offline_sessions_view.png"));
//		_autoResizePcTable_mi         .setIcon(SwingUtils.readImageIcon(Version.class, "images/auto_resize_table_columns.png"));
//		_autoRefreshOnTabChange_mi    .setIcon(SwingUtils.readImageIcon(Version.class, "images/auto_resize_on_tab_change.png"));
//		_groupTcpInTabPane_mi         .setIcon(SwingUtils.readImageIcon(Version.class, "images/group_tcp_in_tab_pane.png"));
//		_optDoGc_m                    .setIcon(SwingUtils.readImageIcon(Version.class, "images/do_gc_after_refresh.png"));
////		_optDoGcAfterXMinutes_mi      .setIcon(SwingUtils.readImageIcon(Version.class, "images/do_gc_after_refresh.png"));
////		_optDoGcAfterXMinutesValue_mi .setIcon(SwingUtils.readImageIcon(Version.class, "images/do_gc_after_refresh.png"));
////		_optDoGcAfterRefresh_mi       .setIcon(SwingUtils.readImageIcon(Version.class, "images/do_gc_after_refresh.png"));
////		_optDoGcAfterRefreshShowGui_mi.setIcon(SwingUtils.readImageIcon(Version.class, "images/do_gc_after_refresh.png"));
//		_refreshRate_mi               .setIcon(SwingUtils.readImageIcon(Version.class, "images/refresh_rate.png"));
//		_aseConfigView_mi             .setIcon(SwingUtils.readImageIcon(Version.class, "images/config_ase_view.png"));
//		_tcpSettingsConf_mi           .setIcon(SwingUtils.readImageIcon(Version.class, "images/tcp_settings_conf.png"));
//		_counterTabView_mi            .setIcon(SwingUtils.readImageIcon(Version.class, "images/counter_tab_view.png"));
//		_graphView_mi                 .setIcon(SwingUtils.readImageIcon(Version.class, "images/graph.png"));
//		_graphs_m                     .setIcon(SwingUtils.readImageIcon(Version.class, "images/summary_tab.png"));
//		
//		_aseConfMon_mi                .setIcon(SwingUtils.readImageIcon(Version.class, "images/config_ase_mon.png"));
//		_captureSql_mi                .setIcon(SwingUtils.readImageIcon(Version.class, "images/capture_sql_tool.gif"));
//		_aseAppTrace_mi               .setIcon(SwingUtils.readImageIcon(Version.class, "images/ase_app_trace_tool.png"));
//		_aseStackTraceAnalyzer_mi     .setIcon(SwingUtils.readImageIcon(Version.class, "images/ase_stack_trace_tool.png"));
//		_ddlView_mi                   .setIcon(SwingUtils.readImageIcon(Version.class, "images/ddl_view_tool.png"));
//		_sqlQuery_mi                  .setIcon(SwingUtils.readImageIcon(Version.class, "images/sql_query_window.png"));
////		_lockTool_mi                  .setIcon(SwingUtils.readImageIcon(Version.class, "images/locktool16.gif"));
//		_createOffline_mi             .setIcon(SwingUtils.readImageIcon(Version.class, "images/pcs_write_16.png"));
//		_wizardCrUdCm_mi              .setIcon(SwingUtils.readImageIcon(Version.class, "images/ud_counter_activity.png"));
//		_doGc_mi                      .setIcon(SwingUtils.readImageIcon(Version.class, "images/gc_now.png"));
//		
//		_about_mi                     .setIcon(SwingUtils.readImageIcon(Version.class, "images/about.png"));

		
		//--------------------------
		// MENU - composition
		_main_mb = createMainMenu();
		setJMenuBar(_main_mb);

//		_main_mb.add(_file_m);
//		_main_mb.add(_view_m);
//		_main_mb.add(_tools_m);
//		_main_mb.add(_help_m);
//
//		_file_m.add(_connect_mi);
//		_file_m.add(_disconnect_mi);
//		_file_m.add(_exit_mi);
//
//		_view_m.add(_logView_mi);
//		_view_m.add(_offlineSessionsView_mi);
//		_view_m.add(_preferences_m);
//			_preferences_m.add(_autoResizePcTable_mi);
//			_preferences_m.add(_autoRefreshOnTabChange_mi);
//			_preferences_m.add(_refreshRate_mi);
//			_preferences_m.add(_groupTcpInTabPane_mi);
//			_preferences_m.add(_optDoGc_m);
//				_optDoGc_m.add(_optDoGcAfterXMinutes_mi);
//				_optDoGc_m.add(_optDoGcAfterXMinutesValue_mi);
//				_optDoGc_m.add(_optDoGcAfterRefresh_mi);
//				_optDoGc_m.add(_optDoGcAfterRefreshShowGui_mi);
//			_preferences_m.add(new JSeparator());
//			_preferences_m.add(_optSummaryOperShowAbs_mi);
//			_preferences_m.add(_optSummaryOperShowDiff_mi);
//			_preferences_m.add(_optSummaryOperShowRate_mi);
//		_view_m.add(_aseConfigView_mi);
//		_view_m.add(_tcpSettingsConf_mi);
//		_view_m.add(_counterTabView_mi);
//		_view_m.add(_graphView_mi);
//		_view_m.add(_graphs_m);
//
//		_tools_m.add(_aseConfMon_mi);
//		_tools_m.add(_viewSrvLogFile_mi);
//		_tools_m.add(_captureSql_mi);
//		_tools_m.add(_aseAppTrace_mi);
//		_tools_m.add(_aseStackTraceAnalyzer_mi);
//		_tools_m.add(_ddlView_mi);
//		_preDefinedSql_m = createPredefinedSqlMenu(null, VendorType.ASE);
//		if (_preDefinedSql_m != null) _tools_m.add(_preDefinedSql_m);
//		_tools_m.add(_sqlQuery_mi);
////		_tools_m.add(_lockTool_mi);
//		_tools_m.add(_createOffline_mi);
//		_tools_m.add(_wizardCrUdCm_mi);
//		_tools_m.add(_doGc_mi);
//
//		_help_m.add(_about_mi);

		//--------------------------
		// MENU - Actions
//		_connect_mi                   .setActionCommand(ACTION_CONNECT);
//		_disconnect_mi                .setActionCommand(ACTION_DISCONNECT);
//		_exit_mi                      .setActionCommand(ACTION_EXIT);
//
//		_logView_mi                   .setActionCommand(ACTION_OPEN_LOG_VIEW);
//		_viewSrvLogFile_mi            .setActionCommand(ACTION_VIEW_LOG_TAIL);
//		_offlineSessionsView_mi       .setActionCommand(ACTION_OPEN_OFFLINE_SESSION_VIEW);
//		_autoResizePcTable_mi         .setActionCommand(ACTION_TOGGLE_AUTO_RESIZE_PC_TABLES);
//		_autoRefreshOnTabChange_mi    .setActionCommand(ACTION_TOGGLE_AUTO_REFRESH_ON_TAB_CHANGE);
//		_groupTcpInTabPane_mi         .setActionCommand(ACTION_GROUP_TCP_IN_TAB_PANE);
//		_optDoGcAfterXMinutes_mi      .setActionCommand(ACTION_DO_JAVA_GC_AFTER_X_MINUTES);
//		_optDoGcAfterXMinutesValue_mi .setActionCommand(ACTION_DO_JAVA_GC_AFTER_X_MINUTES_VALUE);
//		_optDoGcAfterRefresh_mi       .setActionCommand(ACTION_DO_JAVA_GC_AFTER_REFRESH);
//		_optDoGcAfterRefreshShowGui_mi.setActionCommand(ACTION_DO_JAVA_GC_AFTER_REFRESH_SHOW_GUI);
//		_optSummaryOperShowAbs_mi     .setActionCommand(ACTION_SUMMARY_OPERATIONS_TOGGLE);
//		_optSummaryOperShowDiff_mi    .setActionCommand(ACTION_SUMMARY_OPERATIONS_TOGGLE);
//		_optSummaryOperShowRate_mi    .setActionCommand(ACTION_SUMMARY_OPERATIONS_TOGGLE);
//		_refreshRate_mi               .setActionCommand(ACTION_OPEN_REFRESH_RATE);
//		_aseConfigView_mi             .setActionCommand(ACTION_OPEN_ASE_CONFIG_VIEW);
//		_tcpSettingsConf_mi           .setActionCommand(ACTION_OPEN_TCP_PANEL_CONFIG);
//		_counterTabView_mi            .setActionCommand(ACTION_OPEN_COUNTER_TAB_VIEW);
//		_graphView_mi                 .setActionCommand(ACTION_OPEN_GRAPH_GRAPH_VIEW);
//
//		_aseConfMon_mi                .setActionCommand(ACTION_OPEN_ASE_CONFIG_MON);
//		_captureSql_mi                .setActionCommand(ACTION_OPEN_CAPTURE_SQL);
//		_aseAppTrace_mi               .setActionCommand(ACTION_OPEN_ASE_APP_TRACE);
//		_aseStackTraceAnalyzer_mi     .setActionCommand(ACTION_OPEN_ASE_STACKTRACE_TOOL);
//		_ddlView_mi                   .setActionCommand(ACTION_OPEN_DDL_VIEW);
//		_sqlQuery_mi                  .setActionCommand(ACTION_OPEN_SQL_QUERY_WIN);
////		_lockTool_mi                  .setActionCommand(ACTION_OPEN_LOCK_TOOL);
//		_createOffline_mi             .setActionCommand(ACTION_OPEN_WIZARD_OFFLINE);
//		_wizardCrUdCm_mi              .setActionCommand(ACTION_OPEN_WIZARD_UDCM);
//		_doGc_mi                      .setActionCommand(ACTION_GARBAGE_COLLECT);
//		
//		_about_mi                     .setActionCommand(ACTION_OPEN_ABOUT);

		//--------------------------
		// And the action listener
//		_connect_mi                   .addActionListener(this);
//		_disconnect_mi                .addActionListener(this);
//		_exit_mi                      .addActionListener(this);
//
//		_logView_mi                   .addActionListener(this);
//		_viewSrvLogFile_mi            .addActionListener(this);
//		_offlineSessionsView_mi       .addActionListener(this);
//		_autoResizePcTable_mi         .addActionListener(this);
//		_autoRefreshOnTabChange_mi    .addActionListener(this);
//		_groupTcpInTabPane_mi         .addActionListener(this);
//		_optDoGcAfterXMinutes_mi      .addActionListener(this);
//		_optDoGcAfterXMinutesValue_mi .addActionListener(this);
//		_optDoGcAfterRefresh_mi       .addActionListener(this);
//		_optDoGcAfterRefreshShowGui_mi.addActionListener(this);
//		_optSummaryOperShowAbs_mi     .addActionListener(this);
//		_optSummaryOperShowDiff_mi    .addActionListener(this);
//		_optSummaryOperShowRate_mi    .addActionListener(this);
//		_refreshRate_mi               .addActionListener(this);
//		_aseConfigView_mi             .addActionListener(this);
//		_tcpSettingsConf_mi           .addActionListener(this);
//		_counterTabView_mi            .addActionListener(this);
//		_graphView_mi                 .addActionListener(this);
//
//		_aseConfMon_mi                .addActionListener(this);
//		_captureSql_mi                .addActionListener(this);
//		_aseAppTrace_mi               .addActionListener(this);
//		_aseStackTraceAnalyzer_mi     .addActionListener(this);
//		_ddlView_mi                   .addActionListener(this);
//		_sqlQuery_mi                  .addActionListener(this);
////		_lockTool_mi                  .addActionListener(this);
//		_createOffline_mi             .addActionListener(this);
//		_wizardCrUdCm_mi              .addActionListener(this);
//		_doGc_mi                      .addActionListener(this);
//		
//		_about_mi                     .addActionListener(this);

		//--------------------------
		// Keyboard shortcuts
//		_file_m .setMnemonic(KeyEvent.VK_F);
//		_view_m .setMnemonic(KeyEvent.VK_V);
//		_tools_m.setMnemonic(KeyEvent.VK_T);
//		_help_m .setMnemonic(KeyEvent.VK_H);

//		_connect_mi        .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));
//		_disconnect_mi     .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));
//		_sqlQuery_mi       .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
//		_logView_mi        .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
//		_tcpSettingsConf_mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
//		_aseConfMon_mi     .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

		// Refresh: alt+r, F5
		_refreshNow_but.setMnemonic(KeyEvent.VK_R);
		KeyStroke refreshNow = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0);
		contentPane.registerKeyboardAction(this, ACTION_REFRESH_NOW, refreshNow, JComponent.WHEN_IN_FOCUSED_WINDOW);

//		FIXME: Add some Ctrl+>, Ctrl+<  and Ctrl+Shift+<>  to navigate the offline/inmem JSlider, this will make navigation easier
//		FIXME: look at setFocus on JSlide after a Offline dataset has been loded....
		KeyStroke leftSample       = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_LEFT,  Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
		KeyStroke rightSample      = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_RIGHT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
		KeyStroke leftLeftSample   = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_LEFT,  ActionEvent.SHIFT_MASK);
		KeyStroke rightRightSample = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_RIGHT, ActionEvent.SHIFT_MASK);
		KeyStroke leftNextSample   = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_LEFT,  Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | ActionEvent.SHIFT_MASK);
		KeyStroke rightNextSample  = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_RIGHT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | ActionEvent.SHIFT_MASK);

		contentPane.registerKeyboardAction(this, ACTION_SLIDER_LEFT,        leftSample,       JComponent.WHEN_IN_FOCUSED_WINDOW);
		contentPane.registerKeyboardAction(this, ACTION_SLIDER_RIGHT,       rightSample,      JComponent.WHEN_IN_FOCUSED_WINDOW);
		contentPane.registerKeyboardAction(this, ACTION_SLIDER_LEFT_LEFT,   leftLeftSample,   JComponent.WHEN_IN_FOCUSED_WINDOW);
		contentPane.registerKeyboardAction(this, ACTION_SLIDER_RIGHT_RIGHT, rightRightSample, JComponent.WHEN_IN_FOCUSED_WINDOW);
		contentPane.registerKeyboardAction(this, ACTION_SLIDER_LEFT_NEXT,   leftNextSample,   JComponent.WHEN_IN_FOCUSED_WINDOW);
		contentPane.registerKeyboardAction(this, ACTION_SLIDER_RIGHT_NEXT,  rightNextSample,  JComponent.WHEN_IN_FOCUSED_WINDOW);

		KeyStroke cmNavigatorPrev  = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_LEFT,  ActionEvent.ALT_MASK);
		KeyStroke cmNavigatorNext  = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_RIGHT, ActionEvent.ALT_MASK);

		contentPane.registerKeyboardAction(this, ACTION_CM_NAVIGATOR_PREV, cmNavigatorPrev,   JComponent.WHEN_IN_FOCUSED_WINDOW);
		contentPane.registerKeyboardAction(this, ACTION_CM_NAVIGATOR_NEXT, cmNavigatorNext,   JComponent.WHEN_IN_FOCUSED_WINDOW);

		//--------------------------
		// TOOLBAR
		_connectTb_but      = SwingUtils.makeToolbarButton(Version.class, "connect_16.png",    ACTION_CONNECT,    this, "Connect to a DB Server",         "Connect");
		_disConnectTb_but   = SwingUtils.makeToolbarButton(Version.class, "disconnect_16.png", ACTION_DISCONNECT, this, "Close the DB Server Connection", "Disconnect");

		_screenshotTb_but   = SwingUtils.makeToolbarButton(Version.class, "screenshot.png",   ACTION_SCREENSHOT,     this, "Take a screenshot of the application", "Screenshot");
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
				List<String> tabStrList = _mainTabbedPane.getAllTitles("${TAB_NAME};${GROUP_NAME}");

				// Sort the list
				//Collections.sort(tabStrList);

				// Now create menu items in the correct order
				String lastGroup = null;
				for (String name : tabStrList)
				{
					final String[] sa = name.split(";");
					final String tabName   = sa[0];
					final String groupName = sa.length > 1 ? "<i>"+sa[1]+"</i> - " : "";
					final String menuText = "<html>"+groupName+"<b>"+tabName+"</b></html>";

					// Add separator on new groups
					if ( lastGroup != null && ! lastGroup.equals(groupName) )
						tabSelectorNoSortPopupMenu.add(new JSeparator() );
					lastGroup = groupName;

					JMenuItem mi = new JMenuItem();
					mi.setText(menuText);
					mi.setIcon(_mainTabbedPane.getIconAtTitle(tabName));
					mi.addActionListener(new ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent e)
						{
							Object o = e.getSource();
							if (o instanceof JMenuItem)
							{
								//JMenuItem mi = (JMenuItem) o;
								//String tabName = mi.getText();
								_mainTabbedPane.setSelectedTitle(tabName);
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
				List<String> tabStrList = _mainTabbedPane.getAllTitles("${TAB_NAME};${GROUP_NAME}");

				// Sort the list
				Collections.sort(tabStrList);

				// Now create menu items in the correct order
				for (String name : tabStrList)
				{
					final String[] sa = name.split(";");
					final String tabName   = sa[0];
					final String groupName = sa.length > 1 ? " - <i>"+sa[1]+"</i>" : "";
					final String menuText = "<html><b>"+tabName+"</b>"+groupName+"</html>";
					JMenuItem mi = new JMenuItem();
					mi.setText(menuText);
					mi.setIcon(_mainTabbedPane.getIconAtTitle(tabName));
					mi.addActionListener(new ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent e)
						{
							Object o = e.getSource();
							if (o instanceof JMenuItem)
							{
								//JMenuItem mi = (JMenuItem) o;
								//String tabName = mi.getText();
								_mainTabbedPane.setSelectedTitle(tabName);
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


		// NAVIGATOR: PREVIOUS
		_prevCmNavigator_but = SwingUtils.makeToolbarButton(Version.class, "prev_cm.png", ACTION_CM_NAVIGATOR_PREV, this, "<html>Goto <i>previously</i> used Performance Counter Tab<br>Press <i>right click</i> to see the usage stack.<br>Shortcut: Alt-left</html>", "Previous Tab");
		final JPopupMenu prevCmNavigatorPopupMenu = new JPopupMenu();
		prevCmNavigatorPopupMenu.add(new JMenuItem("Empty"));
		_prevCmNavigator_but.setComponentPopupMenu(prevCmNavigatorPopupMenu);
		prevCmNavigatorPopupMenu.addPopupMenuListener(new PopupMenuListener()
		{
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e)
			{
				// remove all old items (if any)
				prevCmNavigatorPopupMenu.removeAll();

				// Get all titles into a list
//				List<String> tabStrList = _mainTabbedPane.getAllTitles("${TAB_NAME};${GROUP_NAME}");
				List<String> tabStrList = _cmNavigatorPrevStack;

				// Now create menu items in the correct order
				for (String name : tabStrList)
				{
					final String[] sa = name.split(";");
					final String tabName   = sa[0];
					final String groupName = sa.length > 1 ? "<i>"+sa[1]+"</i> - " : "";
					final String menuText = "<html>"+groupName+"<b>"+tabName+"</b></html>";

					JMenuItem mi = new JMenuItem();
					mi.setText(menuText);
					mi.setIcon(_mainTabbedPane.getIconAtTitle(tabName));
					mi.addActionListener(new ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent e)
						{
							Object o = e.getSource();
							if (o instanceof JMenuItem)
							{
								//JMenuItem mi = (JMenuItem) o;
								//String tabName = mi.getText();
								_mainTabbedPane.setSelectedTitle(tabName);
							}
						}
					});

					prevCmNavigatorPopupMenu.add(mi);
				}
			}
			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {/*empty*/}
			@Override
			public void popupMenuCanceled(PopupMenuEvent e)	{/*empty*/}
		});

		// NAVIGATOR: NEXT
		_nextCmNavigator_but = SwingUtils.makeToolbarButton(Version.class, "next_cm.png", ACTION_CM_NAVIGATOR_NEXT, this, "<html>Goto <i>next</i> used Performance Counter Tab, in the usage stack<br>Press <i>right click</i> to see the usage stack.<br>Shortcut: Alt-right</html>", "Next Tab");
		final JPopupMenu nextCmNavigatorPopupMenu = new JPopupMenu();
		nextCmNavigatorPopupMenu.add(new JMenuItem("Empty"));
		_nextCmNavigator_but.setComponentPopupMenu(nextCmNavigatorPopupMenu);
		nextCmNavigatorPopupMenu.addPopupMenuListener(new PopupMenuListener()
		{
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e)
			{
				// remove all old items (if any)
				nextCmNavigatorPopupMenu.removeAll();

				// Get all titles into a list
//				List<String> tabStrList = _mainTabbedPane.getAllTitles("${TAB_NAME};${GROUP_NAME}");
				List<String> tabStrList = _cmNavigatorNextStack;

				// Now create menu items in the correct order
				for (String name : tabStrList)
				{
					final String[] sa = name.split(";");
					final String tabName   = sa[0];
					final String groupName = sa.length > 1 ? "<i>"+sa[1]+"</i> - " : "";
					final String menuText = "<html>"+groupName+"<b>"+tabName+"</b></html>";

					JMenuItem mi = new JMenuItem();
					mi.setText(menuText);
					mi.setIcon(_mainTabbedPane.getIconAtTitle(tabName));
					mi.addActionListener(new ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent e)
						{
							Object o = e.getSource();
							if (o instanceof JMenuItem)
							{
								//JMenuItem mi = (JMenuItem) o;
								//String tabName = mi.getText();
								_mainTabbedPane.setSelectedTitle(tabName);
							}
						}
					});

					nextCmNavigatorPopupMenu.add(mi);
				}
			}
			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {/*empty*/}
			@Override
			public void popupMenuCanceled(PopupMenuEvent e)	{/*empty*/}
		});
//		_prevCm_but.setIcon(SwingUtils.readImageIcon(Version.class, "images/prev_cm.png"));
//		_nextCm_but.setIcon(SwingUtils.readImageIcon(Version.class, "images/next_cm.png"));


		_viewStorage_chk .setIcon(        SwingUtils.readImageIcon(Version.class, "images/read_storage.png"));
		_viewStorage_chk .setSelectedIcon(SwingUtils.readImageIcon(Version.class, "images/read_storage_minus.png"));
		_viewStorage_chk .setToolTipText("View Counters that are stored in memory for a while...");

		_toolbar.setLayout(new MigLayout("insets 0 0 0 3", "", "")); // insets Top Left Bottom Right
		_toolbar.add(_connectTb_but);
		_toolbar.add(_disConnectTb_but);
		_toolbar.add(_screenshotTb_but);
		_toolbar.add(_samplePauseTb_but, "hidemode 3");
		_toolbar.add(_samplePlayTb_but,  "hidemode 3");
		_toolbar.add(tabSelectorNoSort);
		_toolbar.add(tabSelectorSorted);
		_toolbar.add(_prevCmNavigator_but);
		_toolbar.add(_nextCmNavigator_but);

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
		_readSlider.setToolTipText("" +
			"<html>" +
			"Choose a in-memory sample period to display.<br>" +
			"Keyboard Shortcuts:" +
			"<ul>" +
			"    <li>Ctrl + Left - Move one position Left in the slider.</li>" +
			"    <li>Ctrl + Right - Move one position Right in the slider.</li>" +
			"    <li>Shift + Left - Move 10 positions Left in the slider.</li>" +
			"    <li>Shift + Right - Move 10 positions Right in the slider.</li>" +
//			"    <li>Ctrl + Shift + Left - Move to Previous sample that contains data.</li>" +
//			"    <li>Ctrl + Shift + Right - Move to Next sample that contains data.</li>" +
			"</ul>" +
			"</html>");

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
		_offlineSlider.setToolTipText("" +
			"<html>" +
			"Choose a stored sample period to display.<br>" +
			"Keyboard Shortcuts:" +
			"<ul>" +
			"    <li>Ctrl + Left - Move one position Left in the slider.</li>" +
			"    <li>Ctrl + Right - Move one position Right in the slider.</li>" +
			"    <li>Shift + Left - Move 10 positions Left in the slider.</li>" +
			"    <li>Shift + Right - Move 10 positions Right in the slider.</li>" +
			"    <li>Ctrl + Shift + Left - Move to Previous sample that contains data.</li>" +
			"    <li>Ctrl + Shift + Right - Move to Next sample that contains data.</li>" +
			"</ul>" +
			"</html>");

//		_offlineSlider.setLabelTable(_readSlider.createStandardLabels(10));
//		_offlineSlider.setLabelTable(dict);
		_offlineSlider.addChangeListener(this);

		//--------------------------
		// STATUS PANEL
		_blockAlert_but       .setToolTipText("<html>You have SPID(s) that <b>blocks</b> other SPID(s) from working. Click here to the 'Blocking' tab to find out more.</html>");
		_fullTranlogAlert_but .setToolTipText("<html>The Transaction log in one/several databases are <b>full</b> other SPID(s) are probably <b>blocked</b> by this. Click here to the 'Database' tab to find out more.</html>");
		_oldestOpenTran_but   .setToolTipText("<html>You have a transaction in one database that has been open for a long time. Click here to the 'Database' tab to find out more.</html>");
		_refreshNow_but       .setToolTipText("Abort the \"sleep for next refresh\" and make a new refresh of data NOW (shortcut Alt+r).");
		_samplePause_but      .setToolTipText("Pause ALL sampling activity.");
		_samplePlay_but       .setToolTipText("Continue to sample counters again.");
		_sampleInterval_cbx   .setToolTipText("<html>Sleep time between Data Collection<p><b>Tip:</b> You can change this to <i>any</i> number in: <b>Menu-&gt;View-&gt;Preferences-&gt;Refresh Rate...<b></html>");
		_gcNow_but            .setToolTipText("Do Java Garbage Collection.");
		_statusStatus         .setToolTipText("What are we doing or waiting for.");
		_statusStatus2        .setToolTipText("What are we doing or waiting for.");
		_statusServerName     .setToolTipText("<html>The local name of the ASE Server, as named in the interfaces or sql.ini file.<BR>Also show the HOST:PORT, which we are connected to.</html>");
		_statusServerListeners.setToolTipText("<html>This is the network listeners the ASE Servers listens to.<BR>This is good to see if we connect via SSH tunnels or other proxy functionality.<br>The format is netlibdriver: HOST PORT, next entry...</html>");
		_statusMemory         .setToolTipText("How much memory does the JVM consume for the moment.");
		_statusPcsQueueSize         .setToolTipText("Number of entries in the Performance Counter Storage Queue.");
		_statusPcsPersistInfo       .setToolTipText("PCS Information, For Example: How long did the last persist took");
		_statusPcsDdlLookupQueueSize.setToolTipText("Number of entries in the DDL Lookup Queue.");
		_statusPcsDdlStoreQueueSize .setToolTipText("Number of entries in the DDL Storage Queue.");

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

		// Oldest Open Transaction alert butt
		_oldestOpenTran_but.setIcon(SwingUtils.readImageIcon(Version.class, "images/oldest_open_tran_alert.png"));
		_oldestOpenTran_but.setText("");
		_oldestOpenTran_but.setContentAreaFilled(false);
//		_oldestOpenTran_but.setMargin( new Insets(3,3,3,3) );
		_oldestOpenTran_but.setMargin( new Insets(0,0,0,0) );
		_oldestOpenTran_but.addActionListener(this);
		_oldestOpenTran_but.setActionCommand(ACTION_GOTO_DATABASE_TAB);
		_oldestOpenTran_but.setVisible(false);

		
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

		_sampleInterval_cbx.setBackground(_statusPanel.getBackground());
		_sampleInterval_cbx.addActionListener(this);
		_sampleInterval_cbx.setActionCommand(ACTION_CHANGE_SAMPLE_INTERVAL);
		
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
		_statusPanel.add(_sampleInterval_cbx,                     "");
		_statusPanel.add(_blockAlert_but,                         "hidemode 3");
		_statusPanel.add(_fullTranlogAlert_but,                   "hidemode 3");
		_statusPanel.add(_oldestOpenTran_but,                     "hidemode 3");
		_statusPanel.add(_statusStatus,                           "gaptop 3, gapbottom 5, grow, push");
		_statusPanel.add(_statusStatus2,                          "");
		_statusPanel.add(new JSeparator(SwingConstants.VERTICAL), "grow");
		_statusPanel.add(_statusServerName,                       "right");
		_statusPanel.add(new JSeparator(SwingConstants.VERTICAL), "grow");
		_statusPanel.add(_statusServerListeners,                  "right");
		_statusPanel.add(new JSeparator(SwingConstants.VERTICAL), "grow");
		_statusPanel.add(_statusPcsQueueSize,                     "hidemode 3");
		_statusPanel.add(_statusPcsPersistInfo,                   "hidemode 3");
		_statusPanel.add(_statusPcsDdlLookupQueueSize,            "hidemode 3");
		_statusPanel.add(_statusPcsDdlStoreQueueSize,             "hidemode 3");
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
//		_mainTabbedPane.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
		_mainTabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		_mainTabbedPane.addChangeListener(this);
		_mainTabbedPane.setTabOrderAndVisibilityListener(this);


		
		//--------------------------
		// Add Summary TAB
//		_mainTabbedPane.addTab("Summary", _summaryPanel.getIcon(), _summaryPanel, "Trend Graphs");

		// Add Group panels
		if (useTcpGroups())
		{
			// DIALOG: Change 'Tab Titles' Order and Visibility...
			// does not yet work with "sub tabs", so simply hide this until it works
			_counterTabView_mi.setVisible(false);
			
			_mainTabbedPane = createGroupTabbedPane(_mainTabbedPane);
		}


		//--------------------------
		// add myself as a listener to the GuiLogAppender
		// Calling this would make GuiLogAppender, to register itself in log4j.
		if (GuiLogAppender.getTableModel() != null)
			GuiLogAppender.getTableModel().addTableModelListener(this);

		//--------------------------
		// Hide/show various components and menus 
		// for the default mode
		setStatus(ST_DISCONNECT);
		//setDefaultMenuMode();

	
		//--------------------------
		// Setup what happens when the window is closing/exiting 
		// for the default mode
		//setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		//setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				saveProps();
				saveTcpProps();
//				if (AseTune.getCounterCollector().isMonConnected())
//				{
//					int answer = AseConfigMonitoringDialog.onExit(_instance, AseTune.getCounterCollector().getMonConnection());
//
//					// This means that a "prompt" was raised to ask for "disable monitoring"
//					// AND the user pressed CANCEL button
//					if (answer > 0)
//						return;
//				}

				// stop the collector thread
//				if (GetCounters.hasInstance())
//					GetCounters.getInstance().shutdown();
				if (CounterController.hasInstance())
					CounterController.getInstance().shutdown();

				action_disconnect(null, false);
				
//				dispose();
				System.exit(0);
			}
		});
		
		pack();
	}

	/*---------------------------------------------------
	** END: component initialization
	**---------------------------------------------------
	*/
/*
 */
	protected JMenuBar createMainMenu()
	{
		JMenuBar menuBar = new JMenuBar();

		_file_m  = createFileMenu();
		_view_m  = createViewMenu();
		_tools_m = createToolsMenu();
		_help_m  = createHelpMenu();

		menuBar.add(_file_m);
		menuBar.add(_view_m);
		menuBar.add(_tools_m);
		menuBar.add(_help_m);
		
		return menuBar;
	}

	protected JMenu createFileMenu()
	{
		JMenu menu = new JMenu("File");
		menu.setMnemonic(KeyEvent.VK_F);

		_connect_mi                    = new JMenuItem("Connect...");
		_disconnect_mi                 = new JMenuItem("Disconnect");
		_exit_mi                       = new JMenuItem("Exit");

		_connect_mi                   .setIcon(SwingUtils.readImageIcon(Version.class, "images/connect_16.png"));
		_disconnect_mi                .setIcon(SwingUtils.readImageIcon(Version.class, "images/disconnect_16.png"));
		_exit_mi                      .setIcon(SwingUtils.readImageIcon(Version.class, "images/close.gif"));

		_connect_mi                   .setActionCommand(ACTION_CONNECT);
		_disconnect_mi                .setActionCommand(ACTION_DISCONNECT);
		_exit_mi                      .setActionCommand(ACTION_EXIT);

		_connect_mi                   .addActionListener(this);
		_disconnect_mi                .addActionListener(this);
		_exit_mi                      .addActionListener(this);

		_connect_mi                   .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));
		_disconnect_mi                .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));

		menu.add(_connect_mi);
		menu.add(_disconnect_mi);
		menu.add(_exit_mi);
		
		return menu;
	}

	protected JMenu createViewMenu()
	{
		JMenu menu = new JMenu("View");
		menu.setMnemonic(KeyEvent.VK_V);
		
		_preferences_m = createPreferencesMenu();
		
		_logView_mi                    = new JMenuItem("Open Log Window...");
		_offlineSessionsView_mi        = new JMenuItem("Offline Sessions Window...");
		_dbmsConfigView_mi             = new JMenuItem("View DBMS Configuration...");
		_tcpSettingsConf_mi            = new JMenuItem("Change 'Performance Counter' Options...");
		_counterTabView_mi             = new JMenuItem("Change 'Tab Titles' Order and Visibility...");
		_graphView_mi                  = new JMenuItem("Change 'Graph' Order and Visibility...");
		_graphs_m                      = new JMenu("Active Graphs");

		_logView_mi                   .setIcon(SwingUtils.readImageIcon(Version.class, "images/log_viewer.gif"));
		_offlineSessionsView_mi       .setIcon(SwingUtils.readImageIcon(Version.class, "images/offline_sessions_view.png"));
		_dbmsConfigView_mi            .setIcon(SwingUtils.readImageIcon(Version.class, "images/config_dbms_view_16.png"));
		_tcpSettingsConf_mi           .setIcon(SwingUtils.readImageIcon(Version.class, "images/tcp_settings_conf.png"));
		_counterTabView_mi            .setIcon(SwingUtils.readImageIcon(Version.class, "images/counter_tab_view.png"));
		_graphView_mi                 .setIcon(SwingUtils.readImageIcon(Version.class, "images/graph.png"));
		_graphs_m                     .setIcon(SwingUtils.readImageIcon(Version.class, "images/summary_tab.png"));

		_logView_mi                   .setActionCommand(ACTION_OPEN_LOG_VIEW);
		_offlineSessionsView_mi       .setActionCommand(ACTION_OPEN_OFFLINE_SESSION_VIEW);
		_dbmsConfigView_mi            .setActionCommand(ACTION_OPEN_DBMS_CONFIG_VIEW);
		_tcpSettingsConf_mi           .setActionCommand(ACTION_OPEN_TCP_PANEL_CONFIG);
		_counterTabView_mi            .setActionCommand(ACTION_OPEN_COUNTER_TAB_VIEW);
		_graphView_mi                 .setActionCommand(ACTION_OPEN_GRAPH_GRAPH_VIEW);

		_logView_mi                   .addActionListener(this);
		_offlineSessionsView_mi       .addActionListener(this);
		_dbmsConfigView_mi            .addActionListener(this);
		_tcpSettingsConf_mi           .addActionListener(this);
		_counterTabView_mi            .addActionListener(this);
		_graphView_mi                 .addActionListener(this);

		_logView_mi                   .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		_tcpSettingsConf_mi           .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

		menu.add(_logView_mi);
		menu.add(_offlineSessionsView_mi);
		menu.add(_preferences_m);
		menu.add(_dbmsConfigView_mi);
		menu.add(_tcpSettingsConf_mi);
		menu.add(_counterTabView_mi);
		menu.add(_graphView_mi);
		menu.add(_graphs_m);
		
		return menu;
	}
	
	protected JMenu createPreferencesMenu()
	{
		JMenu menu = new JMenu("Preferences");
		_optDoGc_m = createJavaGcMenu();
		_optDoGc_m                    .setIcon(SwingUtils.readImageIcon(Version.class, "images/do_gc_after_refresh.png"));

		_autoResizePcTable_mi          = new JCheckBoxMenuItem("Auto Resize Column Width in Performance Counter Tables", false);
		_autoRefreshOnTabChange_mi     = new JCheckBoxMenuItem("Auto Refresh when you change Performance Counter Tab", false);
		_refreshRate_mi                = new JMenuItem("Refresh Rate...");
		_groupTcpInTabPane_mi          = new JCheckBoxMenuItem("Group Performance Counters in Tabular Panels", useTcpGroups());
		_optSummaryOperShowAbs_mi      = new JCheckBoxMenuItem("Show Absolute Counters for Summary Operations",   DEFAULT_summaryOperations_showAbs);
		_optSummaryOperShowDiff_mi     = new JCheckBoxMenuItem("Show Difference Counters for Summary Operations", DEFAULT_summaryOperations_showDiff);
		_optSummaryOperShowRate_mi     = new JCheckBoxMenuItem("Show Rate Counters for Summary Operations",       DEFAULT_summaryOperations_showRate);

		_autoResizePcTable_mi         .setIcon(SwingUtils.readImageIcon(Version.class, "images/auto_resize_table_columns.png"));
		_autoRefreshOnTabChange_mi    .setIcon(SwingUtils.readImageIcon(Version.class, "images/auto_resize_on_tab_change.png"));
		_refreshRate_mi               .setIcon(SwingUtils.readImageIcon(Version.class, "images/refresh_rate.png"));
		_groupTcpInTabPane_mi         .setIcon(SwingUtils.readImageIcon(Version.class, "images/group_tcp_in_tab_pane.png"));

		_autoResizePcTable_mi         .setActionCommand(ACTION_TOGGLE_AUTO_RESIZE_PC_TABLES);
		_autoRefreshOnTabChange_mi    .setActionCommand(ACTION_TOGGLE_AUTO_REFRESH_ON_TAB_CHANGE);
		_refreshRate_mi               .setActionCommand(ACTION_OPEN_REFRESH_RATE);
		_groupTcpInTabPane_mi         .setActionCommand(ACTION_GROUP_TCP_IN_TAB_PANE);
		_optSummaryOperShowAbs_mi     .setActionCommand(ACTION_SUMMARY_OPERATIONS_TOGGLE);
		_optSummaryOperShowDiff_mi    .setActionCommand(ACTION_SUMMARY_OPERATIONS_TOGGLE);
		_optSummaryOperShowRate_mi    .setActionCommand(ACTION_SUMMARY_OPERATIONS_TOGGLE);

		_autoResizePcTable_mi         .addActionListener(this);
		_autoRefreshOnTabChange_mi    .addActionListener(this);
		_refreshRate_mi               .addActionListener(this);
		_groupTcpInTabPane_mi         .addActionListener(this);
		_optSummaryOperShowAbs_mi     .addActionListener(this);
		_optSummaryOperShowDiff_mi    .addActionListener(this);
		_optSummaryOperShowRate_mi    .addActionListener(this);

		menu.add(_autoResizePcTable_mi);
		menu.add(_autoRefreshOnTabChange_mi);
		menu.add(_refreshRate_mi);
		menu.add(_groupTcpInTabPane_mi);
		menu.add(_optDoGc_m);

		menu.add(new JSeparator());
		menu.add(_optSummaryOperShowAbs_mi);
		menu.add(_optSummaryOperShowDiff_mi);
		menu.add(_optSummaryOperShowRate_mi);

		return menu;
	}

	protected JMenu createJavaGcMenu()
	{
		JMenu menu = new JMenu("Do Java Garbage Collect");
		
		_optDoGcAfterXMinutes_mi       = new JCheckBoxMenuItem("Do Java Garbage Collect, every X Minutes", DEFAULT_doJavaGcAfterXMinutes);
		_optDoGcAfterXMinutesValue_mi  = new JMenuItem        ("Do Java Garbage Collect, every X Minute, Change Interval...");
		_optDoGcAfterRefresh_mi        = new JCheckBoxMenuItem("Do Java Garbage Collect, after counters has been refreshed", DEFAULT_doJavaGcAfterRefresh);
		_optDoGcAfterRefreshShowGui_mi = new JCheckBoxMenuItem("Do Java Garbage Collect, after counters has been refreshed, Show GUI Dialog", DEFAULT_doJavaGcAfterRefreshShowGui);

//		_optDoGcAfterXMinutes_mi      .setIcon(SwingUtils.readImageIcon(Version.class, "images/do_gc_after_refresh.png"));
//		_optDoGcAfterXMinutesValue_mi .setIcon(SwingUtils.readImageIcon(Version.class, "images/do_gc_after_refresh.png"));
//		_optDoGcAfterRefresh_mi       .setIcon(SwingUtils.readImageIcon(Version.class, "images/do_gc_after_refresh.png"));
//		_optDoGcAfterRefreshShowGui_mi.setIcon(SwingUtils.readImageIcon(Version.class, "images/do_gc_after_refresh.png"));

		_optDoGcAfterXMinutes_mi      .setActionCommand(ACTION_DO_JAVA_GC_AFTER_X_MINUTES);
		_optDoGcAfterXMinutesValue_mi .setActionCommand(ACTION_DO_JAVA_GC_AFTER_X_MINUTES_VALUE);
		_optDoGcAfterRefresh_mi       .setActionCommand(ACTION_DO_JAVA_GC_AFTER_REFRESH);
		_optDoGcAfterRefreshShowGui_mi.setActionCommand(ACTION_DO_JAVA_GC_AFTER_REFRESH_SHOW_GUI);

		_optDoGcAfterXMinutes_mi      .addActionListener(this);
		_optDoGcAfterXMinutesValue_mi .addActionListener(this);
		_optDoGcAfterRefresh_mi       .addActionListener(this);
		_optDoGcAfterRefreshShowGui_mi.addActionListener(this);

		menu.add(_optDoGcAfterXMinutes_mi);
		menu.add(_optDoGcAfterXMinutesValue_mi);
		menu.add(_optDoGcAfterRefresh_mi);
		menu.add(_optDoGcAfterRefreshShowGui_mi);

		return menu;
	}

	protected JMenu createToolsMenu()
	{
		JMenu menu = new JMenu("Tools");
		menu.setMnemonic(KeyEvent.VK_T);
		
		_viewSrvLogFile_mi             = new JMenuItem("View/Tail the DB Server Log File");
//		_aseConfMon_mi                 = new JMenuItem("Configure ASE for Monitoring...");
//		_captureSql_mi                 = new JMenuItem("Capture SQL...");
//		_aseAppTrace_mi                = new JMenuItem("ASE Application Tracing...");
//		_aseStackTraceAnalyzer_mi      = new JMenuItem("ASE StackTrace Analyzer...");
		_ddlView_mi                    = new JMenuItem("DDL Viewer...");
//		_preDefinedSql_m               = createPredefinedSqlMenu(null, VendorType.ASE);
		_sqlQuery_mi                   = new JMenuItem("SQL Query Window...");
//		_lockTool_mi                   = new JMenuItem("Lock Tool (NOT YET IMPLEMENTED)");
		_createOffline_mi              = new JMenuItem("Create 'Record Session - Template file' Wizard...");
		_wizardCrUdCm_mi               = new JMenuItem("Create 'User Defined Counter' Wizard...");
		_doGc_mi                       = new JMenuItem("Java Garbage Collection");

		_viewSrvLogFile_mi            .setIcon(SwingUtils.readImageIcon(Version.class, "images/tail_logfile.png"));
//		_aseConfMon_mi                .setIcon(SwingUtils.readImageIcon(Version.class, "images/config_ase_mon.png"));
//		_captureSql_mi                .setIcon(SwingUtils.readImageIcon(Version.class, "images/capture_sql_tool.gif"));
//		_aseAppTrace_mi               .setIcon(SwingUtils.readImageIcon(Version.class, "images/ase_app_trace_tool.png"));
//		_aseStackTraceAnalyzer_mi     .setIcon(SwingUtils.readImageIcon(Version.class, "images/ase_stack_trace_tool.png"));
		_ddlView_mi                   .setIcon(SwingUtils.readImageIcon(Version.class, "images/ddl_view_tool.png"));
		_sqlQuery_mi                  .setIcon(SwingUtils.readImageIcon(Version.class, "images/sql_query_window.png"));
//		_lockTool_mi                  .setIcon(SwingUtils.readImageIcon(Version.class, "images/locktool16.gif"));
		_createOffline_mi             .setIcon(SwingUtils.readImageIcon(Version.class, "images/pcs_write_16.png"));
		_wizardCrUdCm_mi              .setIcon(SwingUtils.readImageIcon(Version.class, "images/ud_counter_activity.png"));
		_doGc_mi                      .setIcon(SwingUtils.readImageIcon(Version.class, "images/gc_now.png"));

		_viewSrvLogFile_mi            .setActionCommand(ACTION_VIEW_LOG_TAIL);
//		_aseConfMon_mi                .setActionCommand(ACTION_OPEN_ASE_CONFIG_MON);
//		_captureSql_mi                .setActionCommand(ACTION_OPEN_CAPTURE_SQL);
//		_aseAppTrace_mi               .setActionCommand(ACTION_OPEN_ASE_APP_TRACE);
//		_aseStackTraceAnalyzer_mi     .setActionCommand(ACTION_OPEN_ASE_STACKTRACE_TOOL);
		_ddlView_mi                   .setActionCommand(ACTION_OPEN_DDL_VIEW);
		_sqlQuery_mi                  .setActionCommand(ACTION_OPEN_SQL_QUERY_WIN);
//		_lockTool_mi                  .setActionCommand(ACTION_OPEN_LOCK_TOOL);
		_createOffline_mi             .setActionCommand(ACTION_OPEN_WIZARD_OFFLINE);
		_wizardCrUdCm_mi              .setActionCommand(ACTION_OPEN_WIZARD_UDCM);
		_doGc_mi                      .setActionCommand(ACTION_GARBAGE_COLLECT);

		_viewSrvLogFile_mi            .addActionListener(this);
//		_aseConfMon_mi                .addActionListener(this);
//		_captureSql_mi                .addActionListener(this);
//		_aseAppTrace_mi               .addActionListener(this);
//		_aseStackTraceAnalyzer_mi     .addActionListener(this);
		_ddlView_mi                   .addActionListener(this);
		_sqlQuery_mi                  .addActionListener(this);
//		_lockTool_mi                  .addActionListener(this);
		_createOffline_mi             .addActionListener(this);
		_wizardCrUdCm_mi              .addActionListener(this);
		_doGc_mi                      .addActionListener(this);

		_sqlQuery_mi                 .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
//		_aseConfMon_mi               .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

//		menu.add(_aseConfMon_mi);
		menu.add(_viewSrvLogFile_mi);
//		menu.add(_captureSql_mi);
//		menu.add(_aseAppTrace_mi);
//		menu.add(_aseStackTraceAnalyzer_mi);
		menu.add(_ddlView_mi);
//		if (_preDefinedSql_m != null) menu.add(_preDefinedSql_m);
		menu.add(_sqlQuery_mi);
//		menu.add(_lockTool_mi);
		menu.add(_createOffline_mi);
		menu.add(_wizardCrUdCm_mi);
		menu.add(_doGc_mi);
		
		return menu;
	}

	protected JMenu createHelpMenu()
	{
		JMenu menu = new JMenu("Help");
		menu.setMnemonic(KeyEvent.VK_H);
		
		_about_mi = new JMenuItem("About");
		_about_mi.setIcon(SwingUtils.readImageIcon(Version.class, "images/about.png"));
		_about_mi.setActionCommand(ACTION_OPEN_ABOUT);
		_about_mi.addActionListener(this);

		menu.add(_about_mi);
		
		return menu;
	}

	/*---------------------------------------------------
	** BEGIN: implementing TableModelListener
	**---------------------------------------------------
	*/
	/** called when the lowView table is appended */
	@Override
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
					{
						// Set it visible *soon*, otherwise the GUI will "glutter" other components
						// making the GUI look strange... order of painting the windows... etc...
						SwingUtilities.invokeLater(new Runnable()
						{
							@Override
							public void run()
							{
								_logView.setVisible(true);
							}
						});
					}
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
	** BEGIN: implementing Memory.MemoryListener
	**---------------------------------------------------
	*/
	@Override
	public void outOfMemoryHandler()
	{
		ActionEvent doGcEvent = new ActionEvent(this, 0, MainFrame.ACTION_OUT_OF_MEMORY);
		actionPerformed(doGcEvent);
	}

	@Override
	public void memoryConsumption(int memoryLeftInMB)
	{
		// When 150 MB of memory or less, enable Java Garbage Collect after each Sample
		if (memoryLeftInMB <= CounterCollectorThreadGui.MEMORY_LOW_ON_MEMORY_THRESHOLD_IN_MB)
		{
			ActionEvent doGcEvent = new ActionEvent(this, 0, MainFrame.ACTION_LOW_ON_MEMORY);
			actionPerformed(doGcEvent);
		}

		// When 30 MB of memory or less, write some info about that.
		// and call some handler to act on low memory.
		if (memoryLeftInMB <= CounterCollectorThreadGui.MEMORY_OUT_OF_MEMORY_THRESHOLD_IN_MB)
		{
			outOfMemoryHandler();
		}
	}
	/*---------------------------------------------------
	** END: implementing Memory.MemoryListener
	**---------------------------------------------------
	*/	

	/*---------------------------------------------------
	** BEGIN: implementing ActionListener
	**---------------------------------------------------
	*/
	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source    = e.getSource();
		String actionCmd = e.getActionCommand();
		_inActionCommand = actionCmd;

		_logger.debug("ACTION '"+actionCmd+"'.");

		if (ACTION_CONNECT.equals(actionCmd))
			action_connect(e);

		if (ACTION_DISCONNECT.equals(actionCmd))
			action_disconnect(e, true);

		if (ACTION_EXIT.equals(actionCmd))
			action_exit(e);


		
		if (ACTION_OPEN_LOG_VIEW.equals(actionCmd))
			openLogViewer();

		if (ACTION_OPEN_OFFLINE_SESSION_VIEW.equals(actionCmd))
			openOfflineSessionView(true);

		if (ACTION_TOGGLE_AUTO_RESIZE_PC_TABLES.equals(actionCmd))
			action_toggleAutoResizePcTables(e);

		if (ACTION_TOGGLE_AUTO_REFRESH_ON_TAB_CHANGE.equals(actionCmd))
			action_toggleAutoRefreshOnTabChange(e);

		if (ACTION_GROUP_TCP_IN_TAB_PANE.equals(actionCmd))
			action_toggleGroupTcpInTabPane(e);
		
		if (ACTION_DO_JAVA_GC_AFTER_X_MINUTES.equals(actionCmd))
			action_toggleDoJavaGcAfterXMinutes(e);

		if (ACTION_DO_JAVA_GC_AFTER_X_MINUTES_VALUE.equals(actionCmd))
			action_toggleDoJavaGcAfterXMinutesValue(e);

		if (ACTION_DO_JAVA_GC_AFTER_REFRESH.equals(actionCmd))
			action_toggleDoJavaGcAfterRefresh(e);

		if (ACTION_DO_JAVA_GC_AFTER_REFRESH_SHOW_GUI.equals(actionCmd))
			action_toggleDoJavaGcAfterRefreshShowGui(e);

		if (ACTION_SUMMARY_OPERATIONS_TOGGLE.equals(actionCmd))
		{
			saveProps();
			CounterController.getSummaryPanel().setComponentProperties();
		}
		
		if (ACTION_OPEN_REFRESH_RATE.equals(actionCmd))
			action_refreshRate(e);

		if (ACTION_OPEN_COUNTER_TAB_VIEW.equals(actionCmd))
			action_counterTabView(e);

		if (ACTION_OPEN_GRAPH_GRAPH_VIEW.equals(actionCmd))
			action_openGraphViewDialog(e);

//		if (ACTION_OPEN_ASE_CONFIG_MON.equals(actionCmd))
//			action_openAseMonitorConfigDialog(e);

		if (ACTION_OPEN_DBMS_CONFIG_VIEW.equals(actionCmd))
			DbmsConfigViewDialog.showDialog(this, this);

		if (ACTION_OPEN_TCP_PANEL_CONFIG.equals(actionCmd))
			TcpConfigDialog.showDialog(_instance);

//		if (ACTION_OPEN_CAPTURE_SQL.equals(actionCmd))
//			new ProcessDetailFrame(this, -1, -1);

//		if (ACTION_OPEN_ASE_APP_TRACE.equals(actionCmd))
//		{
//			String servername    = MonTablesDictionary.getInstance().getAseServerName();
//			String aseVersionStr = MonTablesDictionary.getInstance().getAseExecutableVersionStr();
//			int    aseVersionNum = MonTablesDictionary.getInstance().getAseExecutableVersionNum();
//			if (aseVersionNum >= Ver.ver(15,0,2))
//			{
//				AseAppTraceDialog apptrace = new AseAppTraceDialog(-1, servername, aseVersionStr);
//				apptrace.setVisible(true);
//			}
//			else
//			{
//				// NOT supported in ASE versions below 15.0.2
//				String htmlMsg = 
//					"<html>" +
//					"  <h2>Sorry this functionality is not available in ASE "+Ver.versionIntToStr(aseVersionNum)+"</h2>" +
//					"  Application Tracing is introduced in ASE 15.0.2" +
//					"</html>";
//				SwingUtils.showInfoMessage(this, "Not supported for this ASE Version", htmlMsg);
//			}
//		}

//		if (ACTION_OPEN_ASE_STACKTRACE_TOOL.equals(actionCmd))
//		{
//			AseStackTraceAnalyzer.AseStackTreeView view = new AseStackTreeView(null);
//			view.setVisible(true);
//		}

		if (ACTION_OPEN_DDL_VIEW.equals(actionCmd))
		{
			action_openDdlViewer(null, null);
		}

		if (ACTION_OPEN_SQL_QUERY_WIN.equals(actionCmd))
		{
			// to JDBC OFFLINE database
			if (isOfflineConnected() && PersistReader.hasInstance())
			{
				PersistReader reader = PersistReader.getInstance();
				if (reader != null)
				{
					String jdbcDriver = reader.getJdbcDriver();
					String jdbcUrl    = reader.getJdbcUrl();
					String jdbcUser   = reader.getJdbcUser();
					String jdbcPasswd = reader.getJdbcPasswd();
//NOTE: probably needs to get ConnProps from PersistReader and use that (or can we trust the "default ConnProps")
					try 
					{
						DbxConnection conn = getNewConnection(Version.getAppName()+"-QueryWindow");
//						Connection conn = JdbcUtils.connect(this, jdbcDriver, jdbcUrl, jdbcUser, jdbcPasswd);
						QueryWindow qf = new QueryWindow(conn, true, WindowType.JFRAME);
						qf.openTheWindow();
					}
					catch (Exception ex) 
					{
//						JOptionPane.showMessageDialog(
//							MainFrame.this, 
//							"Problems open SQL Query Window\n" + ex.getMessage(),
//							"Error", JOptionPane.ERROR_MESSAGE);
						SwingUtils.showErrorMessage(MainFrame.this, "Error", 
							"Problems open 'offline connection' for SQL Query Window\n" + ex, ex);
					}
				}
			}
			else
			{ // to ASE
				try 
				{
					// Check that we are connected
//					if ( ! AseTune.getCounterCollector().isMonConnected(true, true) )
					if ( ! CounterController.getInstance().isMonConnected(true, true) )
					{
						SwingUtils.showInfoMessage(MainFrame.getInstance(), "Not connected", "Not yet connected to a server.");
						return;
					}

					AseConnectionFactory.setPropertiesForAppname(Version.getAppName()+"-QueryWindow", "IGNORE_DONE_IN_PROC", "true");
					DbxConnection.setPropertyForAppname(Version.getAppName()+"-QueryWindow", "IGNORE_DONE_IN_PROC", "true");
	
//					Connection conn = AseConnectionFactory.getConnection(null, Version.getAppName()+"-QueryWindow", null);
//					Connection conn = getNewConnection(Version.getAppName()+"-QueryWindow");
					DbxConnection conn = getNewConnection(Version.getAppName()+"-QueryWindow");
					QueryWindow qf = new QueryWindow(conn, true, WindowType.JFRAME);
					qf.openTheWindow();
				}
				catch (Exception ex) 
				{
//					JOptionPane.showMessageDialog(
//						MainFrame.this, 
//						"Problems open SQL Query Window\n" + ex.getMessage(),
//						"Error", JOptionPane.ERROR_MESSAGE);
					SwingUtils.showErrorMessage(MainFrame.this, "Error", 
						"Problems open Connection for SQL Query Window\n" + ex, ex);
				}
			} // end: to ASE
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
			// While doing GC show GUI
			if (MainFrame.getInstance().isActive())
			{
				WaitForExecDialog execWait = new WaitForExecDialog(MainFrame.getInstance(), "Forcing Java Garbage Collection.");
				execWait.setState("Note: This is requested by the user.");
				BgExecutor doWork = new BgExecutor(execWait)
				{
					@Override
					public Object doWork()
					{
						// just sleep 10ms, so the GUI will have a chance to become visible
						try {Thread.sleep(10);}
						catch(InterruptedException ignore) {}
	
						System.gc();
						
						return null;
					}
				};
				execWait.execAndWait(doWork, 0);
			}
			else
				System.gc();

			setStatus(MainFrame.ST_MEMORY);
		}

		if (ACTION_OPEN_ABOUT.equals(actionCmd))
			action_about(e);

		if (ACTION_VIEW_LOG_TAIL.equals(actionCmd))
			action_viewLogTail(e);
		
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
					if (imch == null)
					{
						SwingUtils.showInfoMessage("Information", "The in-memory history Handler isn't installed.");
						_viewStorage_chk.setSelected(false);
						return;
					}
					if (imch.getSize() == 0)
					{
						SwingUtils.showInfoMessage("Information", "The in-memory history is empty.");
						_viewStorage_chk.setSelected(false);
						return;
					}
					if (_currentPc == null)
						_currentPc = imch.getRight(); // this is the last added

					CountersModel cm = null;
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

			setForcedRefresh(true);
			
//			GetCounters getCnt = AseTune.getCounterCollector();
//			if (getCnt != null)
//				getCnt.doRefresh();
			if (CounterController.hasInstance())
				CounterController.getInstance().doRefresh();
		}

		if (ACTION_GOTO_BLOCKED_TAB.equals(actionCmd))
		{
			_logger.debug("called: ACTION_GOTO_BLOCKED_TAB");

			String toTabName = CmBlocking.SHORT_NAME; // "Blocking"
			GTabbedPane tabPane = getTabbedPane();
			if (tabPane != null)
				tabPane.setSelectedTitle(toTabName);
		}

		if (ACTION_GOTO_DATABASE_TAB.equals(actionCmd))
		{
			_logger.debug("called: ACTION_GOTO_DATABASE_TAB");

			String toTabName = CmOpenDatabases.SHORT_NAME; // "Databases"
			GTabbedPane tabPane = getTabbedPane();
			if (tabPane != null)
				tabPane.setSelectedTitle(toTabName);
		}


		if (ACTION_OUT_OF_MEMORY.equals(actionCmd))
		{
			_logger.debug("called: ACTION_OUT_OF_MEMORY");

			InMemoryCounterHandler imch = InMemoryCounterHandler.getInstance();
			imch.clear(true);

//			TrendGraphDashboardPanel tgdp = SummaryPanel.getInstance().getGraphPanel();
			TrendGraphDashboardPanel tgdp = CounterController.getSummaryPanel().getGraphPanel();
			tgdp.setInMemHistoryEnable(false);

			// While doing GC show GUI
			if (MainFrame.getInstance().isActive())
			{
				WaitForExecDialog execWait = new WaitForExecDialog(MainFrame.getInstance(), "Forcing Java Garbage Collection.");
				execWait.setState("Note: This is \"out of memory\" Garbage Collection, which can't be disabled.");
				BgExecutor doWork = new BgExecutor(execWait)
				{
					@Override
					public Object doWork()
					{
						// just sleep 10ms, so the GUI will have a chance to become visible
						try {Thread.sleep(10);}
						catch(InterruptedException ignore) {}
	
						System.gc();
						
						return null;
					}
				};
				execWait.execAndWait(doWork, 0);
			}
			else
				System.gc();

			int maxConfigMemInMB = (int) (Runtime.getRuntime().maxMemory() / 1024 / 1024);
			int mbLeftAfterGc = Memory.getMemoryLeftInMB();

			String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());

			// OK, this is non-modal, but the OK button doesn't work, fix this later, and use the X on the window instead
			JOptionPane optionPane = new JOptionPane(
					"<html>" +
					  "<h2>Sorry, out-of-memory (or very close to).</h2>" +
					  "I have cleared the In Memory Counter History! <br>" +
					  "This will hopefully get us going again. <br>" +
					  "<br>" +
					  "The In Memory Counter History, will also be set to 1 minute! <br>" +
					  "This reduce the memory usage in the future.<br>" +
					  "<br>" +
					  "<b>Note</b>: you can raise the memory parameter <code>-Xmx###m</code> in the "+Version.getAppName()+" start script.<br>" +
					  "Current max memory setting seems to be around "+maxConfigMemInMB+" MB.<br>" +
					  "After Garbage Collection, you now have "+mbLeftAfterGc+" free MB.<br>" +
					"</html>", 
					JOptionPane.INFORMATION_MESSAGE);
			JDialog dialog = optionPane.createDialog(this, "out-of-memory @ "+dateStr); 
			dialog.setModal(false);
			dialog.setVisible(true);

			setStatus(MainFrame.ST_MEMORY);
		}

		if (ACTION_LOW_ON_MEMORY.equals(actionCmd))
		{
			_logger.debug("called: ACTION_LOW_ON_MEMORY");
			
			// boolean doJavaGcAfterRefresh = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_doJavaGcAfterRefresh, DEFAULT_doJavaGcAfterRefresh);
			
			if ( ! _optDoGcAfterRefresh_mi.isSelected() )
			{
				int memLeft = Memory.getFreeMemoryInMB();

				if (memLeft < CounterCollectorThreadGui.DEFAULT_MEMORY_LOW_ON_MEMORY_THRESHOLD_IN_MB)
				{
					int maxConfigMemInMB = (int) (Runtime.getRuntime().maxMemory() / 1024 / 1024);

					_optDoGcAfterRefresh_mi.doClick();

					String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());

					// OK, this is non-modal, but the OK button doesn't work, fix this later, and use the X on the window instead
					JOptionPane optionPane = new JOptionPane(
							"<html>" +
							  "<h2>Sorry, FREE Memory starts to get LOW </h2>" +
							  "Low Memory Threshold limit is "+CounterCollectorThreadGui.MEMORY_LOW_ON_MEMORY_THRESHOLD_IN_MB+" MB<br>" +
							  "Currently there are "+memLeft+" MB left.<br>" +
							  "<br>" +
							  "I have <b>enabled</b> 'system' Garbage Collection after each data sample! <br>" +
							  "This can be disabled at: Menu-&gt;View-&gt;Preferences <br>" +
							  "<br>" +
							  "<b>" +
							  "The GUI will probably not respond as previously...<br>" +
							  "Small pauses might be expected.<br>" +
							  "Or the GUI might simply feel sluggish.<br>" +
							  "Especially after data has been refreshed and it's doing Garbage Collection.<br>" +
							  "</b>" +
							  "<br>" +
							  "<b>Note</b>: you can raise the memory parameter <code>-Xmx###m</code> in the "+Version.getAppName()+" start script.<br>" +
							  "Current max memory setting seems to be around "+maxConfigMemInMB+" MB.<br>" +
							"</html>",
							JOptionPane.INFORMATION_MESSAGE);
					JDialog dialog = optionPane.createDialog(this, "low-on-memory @ "+dateStr);
					dialog.setModal(false);
					dialog.setVisible(true);
				}
				else
				{
					_logger.warn("Caught a memory peek of where some subsystem thought we were running lower than "+CounterCollectorThreadGui.DEFAULT_MEMORY_LOW_ON_MEMORY_THRESHOLD_IN_MB+" MB free, but when double checking in the action handler, we still got "+memLeft+" MB Left. So aborting this 'LOW_ON_MEMORY' action.");
				}
			}

			setStatus(MainFrame.ST_MEMORY);
		}

		if (ACTION_SCREENSHOT.equals(actionCmd))
		{
//			final String NL = System.getProperty("line.separator");
			final String NL = "\n";

			String appName = Version.getAppName();
			if (StringUtil.isNullOrBlank(appName))
				appName = "asetune";

			String fileList = "";
			String extraInfo = Version.getAppName() + ", Version: "+ Version.getVersionStr();
			String main = Screenshot.windowScreenshot(this, Version.APP_STORE_DIR, appName+".main", true, extraInfo);
			fileList += main + NL;

			// LOOP all CounterModels, and check if they got any windows open, then screenshot that also
			for (CountersModel cm : CounterController.getInstance().getCmList())
			{
				if (cm == null)
					continue;

				if ( cm.getTabPanel() != null)
				{
					GTabbedPane tp = getTabbedPane();
					if (tp.isTabUnDocked(cm.getDisplayName()))
					{
						JFrame frame = tp.getTabUnDockedFrame(cm.getDisplayName());
						String fn = Screenshot.windowScreenshot(frame, Version.APP_STORE_DIR, appName+"."+cm.getName(), true, extraInfo);
						fileList += fn + NL;
					}
				}
			}

			// TODO: grab "all" other windows as well: SQL Windows, Capture SQL...

			String msg = 
				"<html>" +
				"<b>Screenshot was captured.</b> <br>" +
				"Copy + Paste from the list below, for fast access.<br>" +
				"<br>" +
				"<b>The file(s) was stored as:</b>" +
				"<pre>" + fileList + "</pre>" +
				"<br>" +
				"</html>";
			SwingUtils.showInfoMessage(this, "Screenshot captured", msg);
		}

		if (ACTION_SAMPLING_PAUSE.equals(actionCmd))
		{
			setPauseSampling(true);
		}

		if (ACTION_SAMPLING_PLAY.equals(actionCmd))
		{
			setPauseSampling(false);
		}
		
		if (ACTION_CHANGE_SAMPLE_INTERVAL.equals(actionCmd))
		{
			_refreshInterval = ((Number)_sampleInterval_cbx.getSelectedItem()).intValue();

			saveProps();
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

		if (ACTION_CM_NAVIGATOR_PREV.equals(actionCmd))
		{
			_cmNavigatorStackLevel++;
			if (_cmNavigatorStackLevel < _cmNavigatorPrevStack.size())
			{
				String currentTabName = _mainTabbedPane.getSelectedTitle(true);
				String toTabName      = _cmNavigatorPrevStack.get(_cmNavigatorStackLevel);
	
				_cmNavigatorNextStack.remove(currentTabName);
				_cmNavigatorNextStack.addFirst(currentTabName);
	
				_mainTabbedPane.setSelectedTitle(toTabName);

				// disable button, if we are at the end...
				_prevCmNavigator_but.setEnabled( _cmNavigatorStackLevel + 1 < _cmNavigatorPrevStack.size() );
			}
			else
				_cmNavigatorStackLevel = _cmNavigatorPrevStack.size() - 1;
				

//			System.out.println("ACTION_CM_NAVIGATOR_PREV: _cmNavigatorStackLevel="+_cmNavigatorStackLevel+", _cmNavigatorNextStack="+_cmNavigatorNextStack+", _cmNavigatorPrevStack='"+_cmNavigatorPrevStack+"'.");
		}
		if (ACTION_CM_NAVIGATOR_NEXT.equals(actionCmd))
		{
			_cmNavigatorStackLevel--;
			if (_cmNavigatorStackLevel < 0)
				_cmNavigatorStackLevel = 0;
			if ( ! _cmNavigatorNextStack.isEmpty() )
			{
				String toTabName = _cmNavigatorNextStack.getFirst();
				_cmNavigatorNextStack.removeFirst();
				_mainTabbedPane.setSelectedTitle(toTabName);
			}
//			System.out.println("ACTION_CM_NAVIGATOR_NEXT: _cmNavigatorStackLevel="+_cmNavigatorStackLevel+", _cmNavigatorNextStack="+_cmNavigatorNextStack+", _cmNavigatorPrevStack='"+_cmNavigatorPrevStack+"'.");
		}

		if (ACTION_SLIDER_LEFT       .equals(actionCmd)) action_sliderKeyLeft(e);
		if (ACTION_SLIDER_RIGHT      .equals(actionCmd)) action_sliderKeyRight(e);
		if (ACTION_SLIDER_LEFT_LEFT  .equals(actionCmd)) action_sliderKeyLeftLeft(e);
		if (ACTION_SLIDER_RIGHT_RIGHT.equals(actionCmd)) action_sliderKeyRightRight(e);
		if (ACTION_SLIDER_LEFT_NEXT  .equals(actionCmd)) action_sliderKeyLeftNext(e);
		if (ACTION_SLIDER_RIGHT_NEXT .equals(actionCmd)) action_sliderKeyRightNext(e);

		// Call any subclass/implementors
		actionPerformed(e, source, actionCmd);

		_inActionCommand = null;
	}

	public abstract void actionPerformed(ActionEvent e, Object source, String actionCmd);

	/*---------------------------------------------------
	** END: implementing ActionListener
	**---------------------------------------------------
	*/

	/*---------------------------------------------------
	** BEGIN: implementing ChangeListener
	**---------------------------------------------------
	*/
	@Override
	public void stateChanged(ChangeEvent e)
	{
		Object source = e.getSource();

		// Visibility for navigator buttons.
		_nextCmNavigator_but.setEnabled( ! _cmNavigatorNextStack.isEmpty() );
		_prevCmNavigator_but.setEnabled( _cmNavigatorStackLevel < _cmNavigatorPrevStack.size() );
		if (ACTION_CM_NAVIGATOR_PREV.equals(_inActionCommand) || ACTION_CM_NAVIGATOR_NEXT.equals(_inActionCommand))
		{
			// do nothing
		}
		else
		{
			// a tab was pressed, but not from the "prev/next" buttons, then reset the stack level, and emty the "next" stack
			_cmNavigatorStackLevel = 0;
			while (_cmNavigatorNextStack.size() > 0)
				_cmNavigatorNextStack.removeLast();
		}

		//------------------------------------------------------
		// TabPane changes
		//------------------------------------------------------
		if ( source instanceof JTabbedPane && _mainTabbedPane.contains((JTabbedPane)source) )
		{
			String selectedTabTitle = _mainTabbedPane.getSelectedTitle(true);
			if (selectedTabTitle == null)
				return;	
//System.out.println("stateChanged:_mainTabbedPane:toTitle='"+selectedTabTitle+"'.");
_cmNavigatorPrevStack.remove(selectedTabTitle);
_cmNavigatorPrevStack.addFirst(selectedTabTitle);

//new Exception("DUMMY").printStackTrace();

	
			// LOOP all TabularCntrPanel to check which is the current one...
			// if it should be done
			for (TabularCntrPanel tcp : _TcpMap.values())
			{
				if (selectedTabTitle.equals(tcp.getPanelName()))
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
	
//			if ((_currentPanel != null) && (AseTune.getCounterCollector().getMonConnection() != null) && (_currentPanel.getCm() != null) && (!_currentPanel.getCm().isDataInitialized()))
			if ((_currentPanel != null) && (CounterController.getInstance().getMonConnection() != null) && (_currentPanel.getCm() != null) && (!_currentPanel.getCm().isDataInitialized()))
			{
				CounterController.getInstance().setWaitEvent("data to be initialization in the panel '"+_currentPanel.getPanelName()+"'...");
				//statusFld.setText("Waiting for data to be initialization in the panel '"+currentPanel.getPanelName()+"'...");
			}

			// Automatically do "refresh" when tab is changed
//			if (AseTune.hasCounterCollector() && AseTune.getCounterCollector().isMonConnectedStatus())
			if (CounterController.hasInstance() && CounterController.getInstance().isMonConnectedStatus())
			{
				if (_autoRefreshOnTabChange_mi.isSelected())
				{
					_refreshNow_but.doClick();
				}
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
//				CountersModel summaryCm = _currentPc.getCm(SummaryPanel.CM_NAME);
				CountersModel summaryCm = _currentPc.getCm(CounterController.getSummaryCmName());
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
//		CountersModel summaryCm = GetCounters.getInstance().getCmByName(SummaryPanel.CM_NAME);
		CountersModel summaryCm = CounterController.getSummaryCm();
		if (summaryCm != null && summaryCm.hasTrendGraph() )
			for (TrendGraph tg : summaryCm.getTrendGraphs().values())
				tg.setTimeLineMarker(ts);

//		_offlineReadWatermark.setWatermarkText("");
	}

	
	
	/*---------------------------------------------------
	 ** BEGIN: implementing ConnectionProvider
	 **---------------------------------------------------
	 */
	@Override
//	public Connection getConnection()
	public DbxConnection getConnection()
	{
//		if (GetCounters.getInstance().isMonConnected())
//		{
//			return GetCounters.getInstance().getMonConnection();
		if (CounterController.getInstance().isMonConnected())
		{
			return CounterController.getInstance().getMonConnection();
		}
		else
		{
			return PersistReader.getInstance().getConnection();
		}
	}
	@Override
//	public Connection getNewConnection(String appName)
	public DbxConnection getNewConnection(String appName)
	{
		try
		{
//			return AseConnectionFactory.getConnection(null, appName, null);
			return DbxConnection.connect(this, appName);
		}
		catch (Exception e)
		{
			_logger.warn("Trying to get a new SQL Connection for AppName='"+appName+"', Caught: "+e, e);
			return null;
		}
//		throw new RuntimeException("MainFrame has not implemented the method 'getNewConnection(String)'");
	}
	/*---------------------------------------------------
	 ** END: implementing ConnectionProvider
	 **---------------------------------------------------
	 */


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

//	public boolean showConnectTdsSource()  { return true; }
//	public boolean showConnectJdbcSource() { return false; }
	
	public void action_connect(ActionEvent e)
	{
		Object source = e.getSource();
		String action = e.getActionCommand();

//		if (AseTune.getCounterCollector().isMonConnected())
		if (CounterController.getInstance().isMonConnected())
		{
			SwingUtils.showInfoMessage(this, "DB Server - connect", "Connection already opened, Please close the connection first...");
			return;
		}

		Options connDialogOptions = getConnectionDialogOptions();
		if (connDialogOptions == null)
			throw new RuntimeException("getConnectionDialogOptions() returned null, this is NOT expected.");

//		// Create a new dialog Window
////		boolean checkAseCfg    = true;
//		ConnectionProgressExtraActions srvExtraChecks = createConnectionProgressExtraActions();
//		boolean showAseTab      = showConnectTdsSource();
//		boolean showAseOptions  = showConnectTdsSource();
//		boolean showHostmonTab  = true;
//		boolean showPcsTab      = true;
//		boolean showOfflineTab  = true;
//		boolean showJdbcTab     = showConnectJdbcSource();
//		boolean showJdbcOptions = showConnectJdbcSource();
//		if (DbxTune.hasDevVersionExpired())
//		{
////			checkAseCfg     = true;
//			srvExtraChecks  = null;
//			showAseTab      = false;
//			showAseOptions  = true;
//			showHostmonTab  = false;
//			showPcsTab      = false;
//			showOfflineTab  = true;
//			showJdbcTab     = false;
//			showJdbcOptions = false;
//		}


		if (DbxTune.hasDevVersionExpired())
		{
//			checkAseCfg     = true;
			connDialogOptions._srvExtraChecks           = null;
			connDialogOptions._showAseTab               = false;
			connDialogOptions._showDbxTuneOptionsInTds  = false;
			connDialogOptions._showHostmonTab           = false;
			connDialogOptions._showPcsTab               = false;
			connDialogOptions._showOfflineTab           = true;
			connDialogOptions._showJdbcTab              = false;
			connDialogOptions._showDbxTuneOptionsInJdbc = false;
		}
		
		
		ConnectionDialog connDialog = new ConnectionDialog(this, connDialogOptions);
		if (source instanceof CounterCollectorThreadGui && connDialogOptions._showAseTab)
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
			connDialog.setDesiredProductName(CounterController.getInstance().getSupportedProductName());
			connDialog.setVisible(true);
			connDialog.dispose();
		}

		// Get what was connected to...
		int connType = connDialog.getConnectionType();
		try  { _connectedToProductName = connDialog.getDatabaseProductName(); }
		catch(SQLException ignore) {} 

		if ( connType == ConnectionDialog.CANCEL)
			return;
		
		if ( connType == ConnectionDialog.TDS_CONN)
		{
			if (DbxTune.hasDevVersionExpired())
				throw new RuntimeException(Version.getAppName()+" DEV Version has expired, can't connect to a DB Server. only 'PCS - Read mode' is available.");

			CounterController.getInstance().setMonConnection(     connDialog.getAseConn() );
			CounterController.getInstance().setMonDisConnectTime( connDialog.getDisConnectTime() );
			CounterController.getInstance().setHostMonConnection( connDialog.getSshConn() );

			if (CounterController.getInstance().isMonConnected())
			{
				setStatus(ST_CONNECT);

				connectMonitorHookin();

				if (CounterController.hasInstance())
				{
					CounterController.getInstance().enableRefresh();
				}
				
				if (PersistentCounterHandler.hasInstance())
				{
					PersistentCounterHandler.getInstance().addChangeListener(this);
				}

				DbxConnectInfo ci = new DbxConnectInfo(connDialog.getAseConn(), true);
				ci.setSshTunnelInfo(connDialog.getAseSshTunnelInfo());
				CheckForUpdates.getInstance().sendConnectInfoNoBlock(ci);
//				CheckForUpdates.getInstance().sendConnectInfoNoBlock(connType, connDialog.getAseSshTunnelInfo());

			}
		} // end: TDS_CONN

		if ( connType == ConnectionDialog.JDBC_CONN)
		{
			if (DbxTune.hasDevVersionExpired())
				throw new RuntimeException(Version.getAppName()+" DEV Version has expired, can't connect to a DB Server. only 'PCS - Read mode' is available.");

			CounterController.getInstance().setMonConnection(     connDialog.getJdbcConn() );
			CounterController.getInstance().setMonDisConnectTime( connDialog.getDisConnectTime() );
			CounterController.getInstance().setHostMonConnection( connDialog.getSshConn() );

			if (CounterController.getInstance().isMonConnected())
			{
				setStatus(ST_CONNECT);

				connectMonitorHookin();

				if (CounterController.hasInstance())
				{
					CounterController.getInstance().enableRefresh();
				}
				
				if (PersistentCounterHandler.hasInstance())
				{
					PersistentCounterHandler.getInstance().addChangeListener(this);
				}

				DbxConnectInfo ci = new DbxConnectInfo(connDialog.getJdbcConn(), true);
				ci.setSshTunnelInfo(connDialog.getJdbcSshTunnelInfo());
				CheckForUpdates.getInstance().sendConnectInfoNoBlock(ci);
//				CheckForUpdates.getInstance().sendConnectInfoNoBlock(connType, connDialog.getJdbcSshTunnelInfo());
			}
		} // end: JDBC

		if ( connType == ConnectionDialog.OFFLINE_CONN)
		{
//			_summaryPanel.setLocalServerName("Offline-read");
			CounterController.getSummaryPanel().setLocalServerName("Offline-read");
			setStatus(ST_OFFLINE_CONNECT);
//			setOfflineMenuMode();

			setOfflineConnection( connDialog.getOfflineConn() );
			if (getOfflineConnection() != null)
			{
				if ( ! PersistReader.hasInstance() )
				{
					PersistReader reader = new PersistReader(getOfflineConnection());
					PersistReader.setInstance(reader);
					
					reader.setJdbcDriver(connDialog.getOfflineJdbcDriver());
					reader.setJdbcUrl   (connDialog.getOfflineJdbcUrl());
					reader.setJdbcUser  (connDialog.getOfflineJdbcUser());
					reader.setJdbcPasswd(connDialog.getOfflineJdbcPasswd());
				}
//				_offlineSlider.setVisible(true);

				openOfflineSessionView(true);

				connectOfflineHookin();

//				// Read in the MonTablesDictionary from the offline store
//				// This will serve as a dictionary for ToolTip
//				MonTablesDictionary.getInstance().initializeMonTabColHelper(getOfflineConnection(), true);
//				GetCounters.initExtraMonTablesDictionary();
//
//				// initialize ASE Config Dictionary
//				AseConfig.getInstance().initialize(getOfflineConnection(), true, true, null);
//
////				// initialize ASE Cache Config Dictionary
////				AseCacheConfig.getInstance().initialize(getOfflineConnection(), true, true, null);
//
//				// initialize ASE Config Text Dictionary
//				AseConfigText.initializeAll(getOfflineConnection(), true, true, null);
//
//				// TODO: start the reader thread & register as a listener
////				PersistReader _reader = new PersistReader();
////				_reader.setConnection(getOfflineConnection());
////				setStatus(ST_OFFLINE_CONNECT, "Reading sessions from the offline storage...");
////				_reader.refreshSessions();
////
////				// Register listeners
////				_reader.addChangeListener(MainFrame.getInstance());
////				_reader.addChangeListener(_offlineView);
////
////				// Start it
////				_reader.start();
				
//				CheckForUpdates.sendConnectInfoNoBlock(connType, null);
//				sendConnectInfoNoBlock(connType, null);

				DbxConnectInfo ci = new DbxConnectInfo(connDialog.getOfflineConn(), false);
//				ci.setSshTunnelInfo(connDialog.getOfflineSshTunnelInfo());
				CheckForUpdates.getInstance().sendConnectInfoNoBlock(ci);
//				CheckForUpdates.getInstance().sendConnectInfoNoBlock(connType, null);
			}
		} // end: OFFLINE_CONN
	}

	public abstract Options getConnectionDialogOptions();
//	public abstract ConnectionProgressExtraActions createConnectionProgressExtraActions();
	public abstract void connectMonitorHookin();
	public abstract void connectOfflineHookin();

	public abstract boolean disconnectAbort(boolean canBeAborted);
	public abstract void    disconnectHookin(WaitForExecDialog waitDialog);

	/**
	 * Get the default GUI refresh interval for this specific implementation of the DbXTune tool
	 * @return
	 */
	public abstract int getDefaultRefreshInterval();


	/**
	 * 
	 */
	public void action_disconnect()
	{
		action_disconnect(null, false);
	}

	/**
	 * TODO: use the action_disconnectWithProgress() instead, but this needs more work before it's used...
	 * @param e
	 */
	protected void action_disconnect(ActionEvent e, final boolean canBeAborted)
	{
		String disconnectFrom = null;

//		boolean aseConnected     = AseTune.getCounterCollector().isMonConnected();
		boolean aseConnected     = CounterController.getInstance().isMonConnected();
		boolean offlineConnected = PersistReader.hasInstance();
		
		if (aseConnected)     disconnectFrom = "DB Server";
		if (offlineConnected) disconnectFrom = "Offline Session";

		if (disconnectFrom == null)
			return;

		_logger.info("Starting a thread that will do disconnect after this sample session is finished.");

		WaitForExecDialog wait = new WaitForExecDialog(MainFrame.getInstance(), "Disconnecting from "+disconnectFrom);

		// Kick this of as it's own thread, otherwise the sleep below, might block the Swing Event Dispatcher Thread
		BgExecutor terminateConnectionTask = new BgExecutor(wait)
		{
			@Override
			public Object doWork()
			{
				//--------------------------
				// - Stop the counter refresh thread
				// - Wait for current refresh period to end.
//				GetCounters getCnt = AseTune.getCounterCollector();
//				if (getCnt != null)
				if (CounterController.hasInstance())
				{
					ICounterController getCnt = CounterController.getInstance();

					// say to GetCounters to not continuing doing refresh of counter data
					getCnt.disableRefresh();
					long startTime = System.currentTimeMillis();
					char[] progressChars = new char[] {'-', '\\', '|', '/'}; 
					for (int i=0; ; i++)
					{
						// wait until the current refresh loop has finished
						if (!getCnt.isRefreshing())
							break;

						// don't sleep forever, lets wait 60 seconds.
						int timeoutAfter = 60;
						long sleptSoFar = System.currentTimeMillis() - startTime;
						if (sleptSoFar > (timeoutAfter * 1000) )
							break;

						char pc = progressChars[ i % 4 ];
						_logger.info("Waiting for CounterController to stop before I can: Clearing components... Waited for "+sleptSoFar+" ms so far. Giving up after "+timeoutAfter+" seconds");
						getWaitDialog().setState("Waiting for 'refresh' to end "+pc);

						try { Thread.sleep(500); }
						catch (InterruptedException ignore) {}
					}
				}
		
				// Do disable of monitoring ???
//				if (AseTune.getCounterCollector().isMonConnected())
				if (CounterController.getInstance().isMonConnected())
				{
					if ( disconnectAbort(canBeAborted) )
					{
						if (CounterController.hasInstance())
							CounterController.getInstance().enableRefresh();

						return null;
					}
////					int answer = AseConfigMonitoringDialog.onExit(_instance, AseTune.getCounterCollector().getMonConnection(), canBeAborted);
//					int answer = AseConfigMonitoringDialog.onExit(_instance, CounterController.getInstance().getMonConnection(), canBeAborted);
//
//					// This means that a "prompt" was raised to ask for "disable monitoring"
//					// AND the user pressed ABORT/CANCEL button
//					if (answer > 0)
//					{
////						if (getCnt != null)
////							getCnt.enableRefresh();
//						if (CounterController.hasInstance())
//							CounterController.getInstance().enableRefresh();
//						return null;
//					}
				}

				// Send usage info, as a background thread.
				// Clearing components etc, will hopefully take some time...
				// But it's a time "hole" here, which can be done better
				// also sendCounterUsageInfo(true) is done in the JVM shutdown hook
				// Counter RESET will be done in the CheckForUpdates.sendCounterUsageInfo()
//				CheckForUpdates.sendCounterUsageInfo(false);
//				sendCounterUsageInfo(false);
				CheckForUpdates.getInstance().sendCounterUsageInfo(false);

				//--------------------------
				// Clearing all cm's
				_logger.info("Clearing components...");

				getWaitDialog().setState("Clearing Summary Fields.");
//				SummaryPanel.getInstance().clearSummaryData();
				CounterController.getSummaryPanel().clearSummaryData();

				for (CountersModel cm : CounterController.getInstance().getCmList())
				{
					if (cm != null)
					{
						getWaitDialog().setState("Clearing Performance Counter '"+cm.getDisplayName()+"'.");
						cm.clear();
					}
				}
				getWaitDialog().setState("Clearing Summary Graphs.");
//				SummaryPanel.getInstance().clearGraph();
				CounterController.getSummaryPanel().clearGraph();
				
				//--------------------------
				// Close all cm's
				for (CountersModel cm : CounterController.getInstance().getCmList())
				{
					getWaitDialog().setState("SQL Closing CM '"+cm.getDisplayName()+"'.");
					cm.close();
				}
				
				//--------------------------
				// Close the database connection
				getWaitDialog().setState("Closing DB Server Connection.");
//				AseTune.getCounterCollector().closeMonConnection();
				CounterController.getInstance().closeMonConnection();
				AseConnectionFactory.reset();
		
				//--------------------------
				// Close Host Monitor connection
				getWaitDialog().setState("Closing Host Monitor Connection.");
//				AseTune.getCounterCollector().closeHostMonConnection();
				CounterController.getInstance().closeHostMonConnection();
		
				//--------------------------
				// Close the Offline database connection
				getWaitDialog().setState("Closing Offline Database Connection.");
				closeOfflineConnection();
				if (_offlineView != null)
				{
					_offlineView.setVisible(false);
					_offlineView.dispose();
					_offlineView = null;
				}
		
				//--------------------------
				// Close the DDL View
				if (_ddlViewer != null)
				{
					_ddlViewer.setVisible(false);
					_ddlViewer.dispose();
					_ddlViewer = null;
				}

				//--------------------------
				// If we have a PersistentCounterHandler, stop it...
				if ( PersistentCounterHandler.hasInstance() )
				{
					getWaitDialog().setState("Stopping Persist Storage Handler, if H2 do SHUTDOWN.");
					PersistentCounterHandler.getInstance().stop(true, 10*1000);
					
					getWaitDialog().setState("Removing Persist Storage Handler Change listener.");
					PersistentCounterHandler.getInstance().removeChangeListener(MainFrame.getInstance());

					getWaitDialog().setState("UnRegister the Persist Storage Handler");
					PersistentCounterHandler.setInstance(null);
				}

				//--------------------------
				// If we have a Reader, stop it...
				if ( PersistReader.hasInstance() )
				{
					getWaitDialog().setState("Stopping Offline Reader Handler.");
					PersistReader.getInstance().shutdown();
					PersistReader.setInstance(null);
				}
				
		//FIXME: need much more work
				//--------------------------
				// Empty various Dictionaries
				disconnectHookin(getWaitDialog());
//				getWaitDialog().setState("Clearing ASE Config Dictionary.");
//				AseConfig.reset();
//				AseConfigText.reset();
//
//				getWaitDialog().setState("Clearing Mon Tables Dictionary.");
//				MonTablesDictionary.reset();      // Most probably need to work more on this one...
//
//				//wait.setState("Clearing Wait Event Dictionary.");
//				//MonWaitEventIdDictionary.reset(); // Do not need to be reset, it's not getting anything from DB

				for (CountersModel cm : CounterController.getInstance().getCmList())
				{
					if (cm != null)
					{
						getWaitDialog().setState("Resetting Performance Counter '"+cm.getDisplayName()+"'.");
						cm.reset();
					}
				}
				getWaitDialog().setState("Resetting Counter Collector.");
//				GetCounters.getInstance().reset(false); // Which does reset on all CM objects
				CounterController.getInstance().reset(false); // Which does reset on all CM objects

				//--------------------------
				// Update status fields
				setStatus(ST_DISCONNECT);

				getWaitDialog().setState("Disconnected.");

				_connectedToProductName = null;

				_logger.info("The disconnect thread is ending.");
				
				return null;
			}
		};
		wait.execAndWait(terminateConnectionTask);
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

	private void action_toggleAutoRefreshOnTabChange(ActionEvent e)
	{
		saveProps();
	}
	private void action_toggleGroupTcpInTabPane(ActionEvent e)
	{
		saveProps();
		SwingUtils.showInfoMessage(this, "Restart is needed to take effect", 
			"<html>" +
			"A <b>restart</b> of the application is needed for this change to take effect.<br>" +
			"Sorry for that..." +
			"</html>");
	}
	private void action_toggleDoJavaGcAfterXMinutes(ActionEvent e)
	{
		saveProps();
	}
	private void action_toggleDoJavaGcAfterXMinutesValue(ActionEvent e)
	{
		String key1 = "Do Java Garbage Collect Every X Minute";

		LinkedHashMap<String, String> in = new LinkedHashMap<String, String>();
		in.put(key1, Integer.toString(Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_doJavaGcAfterXMinutesValue, DEFAULT_doJavaGcAfterXMinutesValue)));

		Map<String,String> results = ParameterDialog.showParameterDialog(this, "Do Java Garbage Collect", in, false);

		if (results != null)
		{
			Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);

			int val = Integer.parseInt(results.get(key1));
			tmpConf.setProperty(PROPKEY_doJavaGcAfterXMinutesValue, val);

			saveProps();
		}
	}
	private void action_toggleDoJavaGcAfterRefresh(ActionEvent e)
	{
		saveProps();
	}
	private void action_toggleDoJavaGcAfterRefreshShowGui(ActionEvent e)
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
//		int ret = TrendGraphPanelReorderDialog.showDialog(this, SummaryPanel.getInstance().getGraphPanel());
		int ret = TrendGraphPanelReorderDialog.showDialog(this, CounterController.getSummaryPanel().getGraphPanel());
		if (ret == JOptionPane.OK_OPTION)
		{
		}
	}

	private void action_openAseMonitorConfigDialog(ActionEvent e)
	{
//		Connection conn = AseTune.getCounterCollector().getMonConnection();
//		Connection conn = CounterController.getInstance().getMonConnection();
		DbxConnection conn = CounterController.getInstance().getMonConnection();
		AseConfigMonitoringDialog.showDialog(this, conn, -1);

		// If monitoring is NOT enabled anymore, do disconnect
		// By the way, changes can only be made if you have SA_ROLE
		boolean hasSaRole           = AseConnectionUtils.hasRole(conn, AseConnectionUtils.SA_ROLE);
		boolean isMonitoringEnabled = AseConnectionUtils.getAseConfigRunValueBooleanNoEx(conn, "enable monitoring");
		if ( ! isMonitoringEnabled && hasSaRole )
			action_disconnect(e, false);
	}

	private void action_about(ActionEvent e)
	{
		AboutBox.show(this);
	}

	private void action_viewLogTail(ActionEvent e)
	{
//		Connection conn = AseTune.getCounterCollector().getMonConnection();
		Connection conn = CounterController.getInstance().getMonConnection();
		LogTailWindow logTailDialog = new LogTailWindow(conn);
		logTailDialog.setVisible(true);
		logTailDialog.startTail();
	}

	private void moveSlider(JSlider slider, int moveValue)
	{
		int toPos  = slider.getValue() + moveValue;
		if (toPos >= 0 && toPos <= slider.getMaximum())
			slider.setValue(toPos); // This will call the stateChanged, which does the rest of the work.
	}

	private void action_sliderKeyLeft(ActionEvent e)
	{
//		if (AseTune.getCounterCollector().isMonConnectedStatus())
		if (CounterController.getInstance().isMonConnectedStatus())
			moveSlider(_readSlider, -1);

		else if (isOfflineConnected())
			moveSlider(_offlineSlider, -1);
	}

	private void action_sliderKeyRight(ActionEvent e)
	{
//		if (AseTune.getCounterCollector().isMonConnectedStatus())
		if (CounterController.getInstance().isMonConnectedStatus())
			moveSlider(_readSlider, 1);

		else if (isOfflineConnected())
			moveSlider(_offlineSlider, 1);
	}

	private void action_sliderKeyLeftLeft(ActionEvent e)
	{
//		if (AseTune.getCounterCollector().isMonConnectedStatus())
		if (CounterController.getInstance().isMonConnectedStatus())
			moveSlider(_readSlider, -10);

		else if (isOfflineConnected())
			moveSlider(_offlineSlider, -10);
	}

	private void action_sliderKeyRightRight(ActionEvent e)
	{
//		if (AseTune.getCounterCollector().isMonConnectedStatus())
		if (CounterController.getInstance().isMonConnectedStatus())
			moveSlider(_readSlider, 10);

		else if (isOfflineConnected())
			moveSlider(_offlineSlider, 10);
	}

	private void action_sliderKeyLeftNext(ActionEvent e)
	{
//		if (AseTune.getCounterCollector().isMonConnectedStatus())
		if (CounterController.getInstance().isMonConnectedStatus())
			_logger.info("No action has been assigned 'Ctrl+Shift+left' in 'onlinde mode'");

		else if (isOfflineConnected())
		{
			if (_currentPanel != null)
				_currentPanel.OfflineRewind();
		}
	}

	private void action_sliderKeyRightNext(ActionEvent e)
	{
//		if (AseTune.getCounterCollector().isMonConnectedStatus())
		if (CounterController.getInstance().isMonConnectedStatus())
			_logger.info("No action has been assigned 'Ctrl+Shift+right' in 'onlinde mode'");

		else if (isOfflineConnected())
		{
			if (_currentPanel != null)
				_currentPanel.OfflineFastForward();
		}
	}

	public void action_openDdlViewer(String dbname, String objectname)
	{
//		if (AseTune.getCounterCollector().isMonConnectedStatus())
		if (CounterController.getInstance().isMonConnectedStatus())
		{
			SwingUtils.showInfoMessage("DDL Viewer", 
				"<html>" +
				    "Only supported in 'offline' mode<br>" +
				    "meaning: when you view a recorded session.<br>" +
				    "<br>" +
				    "<i>This functionality, will be implemented for 'online mode' in the future.</i><br>" +
				"<html>");
		}
		else if (isOfflineConnected())
		{
			if (_ddlViewer == null)
				_ddlViewer = new DdlViewer();

			if ( ! StringUtil.isNullOrBlank(objectname) )
				_ddlViewer.setViewEntry(dbname, objectname);
				
			_ddlViewer.setVisible(true);
		}
	}

	public boolean isPauseSampling()
	{
		return _isSamplingPaused;
	}

	public void setPauseSampling(boolean pause)
	{
		_isSamplingPaused = pause;
		
		_samplePause_but  .setVisible( ! _isSamplingPaused );
		_samplePauseTb_but.setVisible( ! _isSamplingPaused );
		_samplePlay_but   .setVisible(   _isSamplingPaused );
		_samplePlayTb_but .setVisible(   _isSamplingPaused );

//		GetCounters.getInstance().doInterruptSleep();
		CounterController.getInstance().doInterruptSleep();
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

	@Override
	public void addPanel(JPanel panel)
	{
		if ( ! DbxTune.hasGui() )
			return;

		if (panel instanceof TabularCntrPanel)
		{
			addTcp( (TabularCntrPanel)panel );
		}
		else if (panel instanceof ISummaryPanel)
		{
			setSummaryPanel( (ISummaryPanel)panel );
		}
		else
		{
			_mainTabbedPane.addTab(panel.getName(), null, panel, null);
		}
	}

	public void setSummaryPanel(ISummaryPanel panel)
	{
		int indexPos = 0;
//		_mainTabbedPane.addTab("Summary", _summaryPanel.getIcon(), _summaryPanel, "Trend Graphs");
		_mainTabbedPane.insertTab(panel.getPanelName(), panel.getIcon(), (Component) panel, panel.getDescription(), indexPos);

		_mainTabbedPane.setSelectedIndex(indexPos);
	}

	/**
	 * Add a Component to the Tab
	 * @param tcp the component to add
	 */
	public static void addTcp(TabularCntrPanel tcp)
	{
		// We are probably in NO-GUI mode
		if ( ! DbxTune.hasGui() )
			return;

		String groupName = tcp.getGroupName();
		if ( ! StringUtil.isNullOrBlank(groupName) && useTcpGroups())
		{
			_logger.debug("MainFrame.addTcp(): adding to group "+StringUtil.left("'"+groupName+"',", 20)+" tcpName='"+tcp.getName()+"'.");
			GTabbedPane gtp = _mainTabbedPane;
			int index = gtp.indexOfTab(groupName);
			if (index >= 0)
			{
				Component comp = _mainTabbedPane.getComponentAt(index);
				if (comp instanceof GTabbedPane)
					gtp = (GTabbedPane) comp;
			}
			else
			{
				gtp = new GTabbedPane();
				_mainTabbedPane.addTab(groupName, getInstance().getGroupIcon(groupName), gtp, getInstance().getGroupToolTipText(groupName));
			}
			gtp.addTab(tcp.getPanelName(), tcp.getIcon(), tcp, tcp.getCm().getDescription());
		}
		else
		{
			_logger.debug("MainFrame.addTcp(): NO GROUP groupName='"+groupName+"', tcpName='"+tcp.getName()+"'.");
			_mainTabbedPane.addTab(tcp.getPanelName(), tcp.getIcon(), tcp, tcp.getCm().getDescription());
		}

		_TcpMap.put(tcp.getPanelName(), tcp);
	}

	/**
	 * get icon for a specific group
	 * @param groupName
	 * @return an icon for the specified group, null if unknown/undefined group
	 */
	protected Icon getGroupIcon(String groupName)
	{
		if (TCP_GROUP_OBJECT_ACCESS.equals(groupName)) return TCP_GROUP_ICON_OBJECT_ACCESS;
		if (TCP_GROUP_SERVER       .equals(groupName)) return TCP_GROUP_ICON_SERVER;
		if (TCP_GROUP_CACHE        .equals(groupName)) return TCP_GROUP_ICON_CACHE;
		if (TCP_GROUP_DISK         .equals(groupName)) return TCP_GROUP_ICON_DISK;
		if (TCP_GROUP_REP_AGENT    .equals(groupName)) return TCP_GROUP_ICON_REP_AGENT;
		if (TCP_GROUP_HOST_MONITOR .equals(groupName)) return TCP_GROUP_ICON_HOST_MONITOR;
		if (TCP_GROUP_UDC          .equals(groupName)) return TCP_GROUP_ICON_UDC;
		return null;
	}

	/**
	 * get tooltip for a specific group
	 * @param groupName
	 * @return an tooltip text for the specified group, null if unknown/undefined group
	 */
	protected String getGroupToolTipText(String groupName)
	{
		if (TCP_GROUP_OBJECT_ACCESS.equals(groupName)) return "<html>Performace Counters on Object and various Statements that Accesses data</html>";
		if (TCP_GROUP_SERVER       .equals(groupName)) return "<html>Performace Counters on a Server Level</html>";
		if (TCP_GROUP_CACHE        .equals(groupName)) return "<html>Performace Counters for various Caches</html>";
		if (TCP_GROUP_DISK         .equals(groupName)) return "<html>Performace Counters for Devices / Disk acesses</html>";
		if (TCP_GROUP_REP_AGENT    .equals(groupName)) return "<html>Performace Counters for ASE Replication Agents</html>";
		if (TCP_GROUP_HOST_MONITOR .equals(groupName)) return "<html>Performace Counters on a Operating System Level</html>";
		if (TCP_GROUP_UDC          .equals(groupName)) return "<html>Performace Counters that <b>you</b> have defined</html>";
		return null;
	}
	
	/**
	 * Should this tab group be added ?<br>
	 * Used by any subclass to "remove" some tabs
	 * 
	 * @param tcpGroupServer
	 * @return true to create it
	 */
	protected boolean addTabGroup(String groupName)
	{
		return true;
	}

	/**
	 * @param mainTabbedPane 
	 */
	public GTabbedPane createGroupTabbedPane(GTabbedPane mainTabbedPane)
	{
		GTabbedPane tabGroupServer       = new GTabbedPane("MainFrame_TabbedPane_Server");
		GTabbedPane tabGroupObjectAccess = new GTabbedPane("MainFrame_TabbedPane_ObjectAccess");
		GTabbedPane tabGroupCache        = new GTabbedPane("MainFrame_TabbedPane_Cache");
		GTabbedPane tabGroupDisk         = new GTabbedPane("MainFrame_TabbedPane_Disk");
		GTabbedPane tabGroupRepAgent     = new GTabbedPane("MainFrame_TabbedPane_RepAgent");
		GTabbedPane tabGroupHostMonitor  = new GTabbedPane("MainFrame_TabbedPane_HostMonitor");
		GTabbedPane tabGroupUdc          = new GTabbedPane("MainFrame_TabbedPane_Udc");

		// Lets do setTabLayoutPolicy for all sub tabs...
		tabGroupServer      .setTabLayoutPolicy(_mainTabbedPane.getTabLayoutPolicy());
		tabGroupObjectAccess.setTabLayoutPolicy(_mainTabbedPane.getTabLayoutPolicy());
		tabGroupCache       .setTabLayoutPolicy(_mainTabbedPane.getTabLayoutPolicy());
		tabGroupDisk        .setTabLayoutPolicy(_mainTabbedPane.getTabLayoutPolicy());
		tabGroupRepAgent    .setTabLayoutPolicy(_mainTabbedPane.getTabLayoutPolicy());
		tabGroupHostMonitor .setTabLayoutPolicy(_mainTabbedPane.getTabLayoutPolicy());
		tabGroupUdc         .setTabLayoutPolicy(_mainTabbedPane.getTabLayoutPolicy());

		if (addTabGroup(TCP_GROUP_SERVER))        _mainTabbedPane.addTab(TCP_GROUP_SERVER,        getGroupIcon(TCP_GROUP_SERVER),        tabGroupServer,       getGroupToolTipText(TCP_GROUP_SERVER));
		if (addTabGroup(TCP_GROUP_OBJECT_ACCESS)) _mainTabbedPane.addTab(TCP_GROUP_OBJECT_ACCESS, getGroupIcon(TCP_GROUP_OBJECT_ACCESS), tabGroupObjectAccess, getGroupToolTipText(TCP_GROUP_OBJECT_ACCESS));
		if (addTabGroup(TCP_GROUP_CACHE))         _mainTabbedPane.addTab(TCP_GROUP_CACHE,         getGroupIcon(TCP_GROUP_CACHE),         tabGroupCache,        getGroupToolTipText(TCP_GROUP_CACHE));
		if (addTabGroup(TCP_GROUP_DISK))          _mainTabbedPane.addTab(TCP_GROUP_DISK,          getGroupIcon(TCP_GROUP_DISK),          tabGroupDisk,         getGroupToolTipText(TCP_GROUP_DISK));
		if (addTabGroup(TCP_GROUP_REP_AGENT))     _mainTabbedPane.addTab(TCP_GROUP_REP_AGENT,     getGroupIcon(TCP_GROUP_REP_AGENT),     tabGroupRepAgent,     getGroupToolTipText(TCP_GROUP_REP_AGENT));
		if (addTabGroup(TCP_GROUP_HOST_MONITOR))  _mainTabbedPane.addTab(TCP_GROUP_HOST_MONITOR,  getGroupIcon(TCP_GROUP_HOST_MONITOR),  tabGroupHostMonitor,  getGroupToolTipText(TCP_GROUP_HOST_MONITOR));
		if (addTabGroup(TCP_GROUP_UDC))           _mainTabbedPane.addTab(TCP_GROUP_UDC,           getGroupIcon(TCP_GROUP_UDC),           tabGroupUdc,          getGroupToolTipText(TCP_GROUP_UDC));
		
		tabGroupUdc.setEmptyTabMessage(
			"No User Defined Performance Counters has been added.\n" +
			"\n" +
			"To create one just follow the Wizard under:\n" +
			"Menu -> Tools -> Create 'User Defined Counter' Wizard...\n" +
			"\n" +
			"This enables you to write Performance Counters on you'r Application Tables,\n" +
			"which enables you to measure Application specific performance issues.\n" +
			"Or simply write you'r own MDA table queries...");
		
		return mainTabbedPane;
	}

	/**
	 */
	@Override
	public GTabbedPane getTabbedPane()
	{
		// We are probably in NO-GUI mode
		if ( ! DbxTune.hasGui() )
			return null;

		return _mainTabbedPane;
	}

	/**
	 * Add a "enable/disable" checkbox in the view menu
	 * @param mi The <code>JMenuItem</code> to add.
	 */
	public static void addGraphViewMenu(JMenuItem mi)
	{
		MainFrame.getInstance()._graphs_m.add(mi);
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


	/**
	 * Set the FULL TRANSACTION LOG to be visible or not.
	 * @param visible
	 */
	public void setOldestOpenTran(boolean visible, int seconds)
	{
		if (seconds > 0)
			_oldestOpenTran_but.setText("Oldest Open Transaction is "+seconds+" seconds, ");
		else
			_oldestOpenTran_but.setText("");

		_oldestOpenTran_but.setVisible(visible);
	}


//	public static JMenu createPredefinedSqlMenu(final QueryWindow sqlWindowInstance)
//	{
//		_logger.debug("createPredefinedSqlMenu(): called.");
//
//		final JMenu menu = new JMenu("Predefined SQL Statements");
//		menu.setToolTipText("<html>This is a bunch of stored procedures...<br>If the prcocedure doesn't exist. It will be created.<br>The user you are logged in as need to have the correct priviliges to create procedues in sybsystemprocs.</html>");;
//		menu.setIcon(SwingUtils.readImageIcon(Version.class, "images/pre_defined_sql_statement.png"));
//
//		Configuration systmp = new Configuration();
//		
////		menu.getPopupMenu().addPopupMenuListener(new PopupMenuListener()
////		{
////			@Override
////			public void popupMenuWillBecomeVisible(PopupMenuEvent e)
////			{
////				menu.removeAll();
////				
////				Configuration conf = null;
////				String connectedToProductName = _connectedToProductName;
////				if (sqlWindowInstance != null)
////					sqlWindowInstance.getConnectedToProductName();
////
////				if      (DbUtils.isProductName(connectedToProductName, DbUtils.DB_PROD_NAME_SYBASE_ASE)) conf = MainFrameAse.getPredefinedSqlMenuConfiguration();
////				else if (DbUtils.isProductName(connectedToProductName, DbUtils.DB_PROD_NAME_SYBASE_IQ))  conf = MainFrameIq .getPredefinedSqlMenuConfiguration();
////
////				createPredefinedSqlMenu(menu, "system.predefined.sql.", conf, sqlWindowInstance);
////				createPredefinedSqlMenu(menu, "user.predefined.sql.",   null, sqlWindowInstance);
////
////				if ( menu.getMenuComponentCount() == 0 )
////				{
////					JMenuItem empty = new JMenuItem("No Predefined SQL Statements available.");
////					empty.setEnabled(false);
////					menu.add(empty);
////				}
////			}
////			
////			@Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
////			@Override public void popupMenuCanceled(PopupMenuEvent e) {}
////		});
//
//		//----- sp_list_unused_indexes.sql -----
//		systmp.setProperty("system.predefined.sql.01.name",                        "<html><b>sp_list_unused_indexes</b> - <i><font color=\"green\">List unused indexes in all databases.</font></i></html>");
////		systmp.setProperty("system.predefined.sql.01.name",                        "List unused indexes in all databases.");
//		systmp.setProperty("system.predefined.sql.01.execute",                     "exec sp_list_unused_indexes");
//		systmp.setProperty("system.predefined.sql.01.install.needsVersion",        "0");
//		systmp.setProperty("system.predefined.sql.01.install.dbname",              "sybsystemprocs");
//		systmp.setProperty("system.predefined.sql.01.install.procName",            "sp_list_unused_indexes");
//		systmp.setProperty("system.predefined.sql.01.install.procDateThreshold",   VersionInfo.SP_LIST_UNUSED_INDEXES_CR_STR);
//		systmp.setProperty("system.predefined.sql.01.install.scriptLocation",      com.asetune.cm.sql.VersionInfo.class.getName());
//		systmp.setProperty("system.predefined.sql.01.install.scriptName",          "sp_list_unused_indexes.sql");
//		systmp.setProperty("system.predefined.sql.01.install.needsRole",           "sa_role");
//
//		//----- sp_whoisw.sql -----
//		systmp.setProperty("system.predefined.sql.02.name",                        "<html><b>sp_whoisw</b> - <i><font color=\"green\">Who Is Working (list SPID's that are doing stuff).</font></i></html>");
////		systmp.setProperty("system.predefined.sql.02.name",                        "sp_whoisw - Who Is Working (list SPID's that are doing stuff).");
//		systmp.setProperty("system.predefined.sql.02.execute",                     "exec sp_whoisw");
//		systmp.setProperty("system.predefined.sql.02.install.needsVersion",        "0");
//		systmp.setProperty("system.predefined.sql.02.install.dbname",              "sybsystemprocs");
//		systmp.setProperty("system.predefined.sql.02.install.procName",            "sp_whoisw");
//		systmp.setProperty("system.predefined.sql.02.install.procDateThreshold",   VersionInfo.SP_WHOISW_CR_STR);
//		systmp.setProperty("system.predefined.sql.02.install.scriptLocation",      com.asetune.cm.sql.VersionInfo.class.getName());
//		systmp.setProperty("system.predefined.sql.02.install.scriptName",          "sp_whoisw.sql");
//		systmp.setProperty("system.predefined.sql.02.install.needsRole",           "sa_role");
//
//		//----- sp_whoisb.sql -----
//		systmp.setProperty("system.predefined.sql.03.name",                        "<html><b>sp_whoisb</b> - <i><font color=\"green\">Who Is Blocking (list info about SPID's taht are blocking other SPID's from running).</font></i></html>");
////		systmp.setProperty("system.predefined.sql.03.name",                        "sp_whoisb - Who Is Blocking (list info about SPID's taht are blocking other SPID's from running).");
//		systmp.setProperty("system.predefined.sql.03.execute",                     "exec sp_whoisb");
//		systmp.setProperty("system.predefined.sql.03.install.needsVersion",        "0");
//		systmp.setProperty("system.predefined.sql.03.install.dbname",              "sybsystemprocs");
//		systmp.setProperty("system.predefined.sql.03.install.procName",            "sp_whoisb");
//		systmp.setProperty("system.predefined.sql.03.install.procDateThreshold",   VersionInfo.SP_WHOISB_CR_STR);
//		systmp.setProperty("system.predefined.sql.03.install.scriptLocation",      com.asetune.cm.sql.VersionInfo.class.getName());
//		systmp.setProperty("system.predefined.sql.03.install.scriptName",          "sp_whoisb.sql");
//		systmp.setProperty("system.predefined.sql.03.install.needsRole",           "sa_role");
//
//		//----- sp_opentran.sql -----
//		systmp.setProperty("system.predefined.sql.04.name",                        "<html><b>sp_opentran</b> - <i><font color=\"green\">List information about the SPID that is holding the oldest open transaction (using syslogshold).</font></i></html>");
////		systmp.setProperty("system.predefined.sql.04.name",                        "sp_opentran - List information about the SPID that is holding the oldest open transaction (using syslogshold).");
//		systmp.setProperty("system.predefined.sql.04.execute",                     "exec sp_opentran");
//		systmp.setProperty("system.predefined.sql.04.install.needsVersion",        "0");
//		systmp.setProperty("system.predefined.sql.04.install.dbname",              "sybsystemprocs");
//		systmp.setProperty("system.predefined.sql.04.install.procName",            "sp_opentran");
//		systmp.setProperty("system.predefined.sql.04.install.procDateThreshold",   VersionInfo.SP_OPENTRAN_CR_STR);
//		systmp.setProperty("system.predefined.sql.04.install.scriptLocation",      com.asetune.cm.sql.VersionInfo.class.getName());
//		systmp.setProperty("system.predefined.sql.04.install.scriptName",          "sp_opentran.sql");
//		systmp.setProperty("system.predefined.sql.04.install.needsRole",           "sa_role");
//
//		//----- sp_lock2.sql -----
//		systmp.setProperty("system.predefined.sql.05.name",                        "<html><b>sp_lock2</b> - <i><font color=\"green\">More or less the same as sp_lock, but uses 'table name' instead of 'table id'.</font></i></html>");
////		systmp.setProperty("system.predefined.sql.05.name",                        "sp_lock2 - More or less the same as sp_lock, but uses 'table name' instead of 'table id'.");
//		systmp.setProperty("system.predefined.sql.05.execute",                     "exec sp_lock2");
//		systmp.setProperty("system.predefined.sql.05.install.needsVersion",        "0");
//		systmp.setProperty("system.predefined.sql.05.install.dbname",              "sybsystemprocs");
//		systmp.setProperty("system.predefined.sql.05.install.procName",            "sp_lock2");
//		systmp.setProperty("system.predefined.sql.05.install.procDateThreshold",   VersionInfo.SP_LOCK2_CR_STR);
//		systmp.setProperty("system.predefined.sql.05.install.scriptLocation",      com.asetune.cm.sql.VersionInfo.class.getName());
//		systmp.setProperty("system.predefined.sql.05.install.scriptName",          "sp_lock2.sql");
//		systmp.setProperty("system.predefined.sql.05.install.needsRole",           "sa_role");
//
//		//----- sp_locksum.sql -----
//		systmp.setProperty("system.predefined.sql.06.name",                        "<html><b>sp_locksum</b> - <i><font color=\"green\">Prints number of locks each SPID has.</font></i></html>");
////		systmp.setProperty("system.predefined.sql.06.name",                        "sp_locksum - Prints number of locks each SPID has.");
//		systmp.setProperty("system.predefined.sql.06.execute",                     "exec sp_locksum");
//		systmp.setProperty("system.predefined.sql.06.install.needsVersion",        "0");
//		systmp.setProperty("system.predefined.sql.06.install.dbname",              "sybsystemprocs");
//		systmp.setProperty("system.predefined.sql.06.install.procName",            "sp_locksum");
//		systmp.setProperty("system.predefined.sql.06.install.procDateThreshold",   VersionInfo.SP_LOCKSUM_CR_STR);
//		systmp.setProperty("system.predefined.sql.06.install.scriptLocation",      com.asetune.cm.sql.VersionInfo.class.getName());
//		systmp.setProperty("system.predefined.sql.06.install.scriptName",          "sp_locksum.sql");
//		systmp.setProperty("system.predefined.sql.06.install.needsRole",           "sa_role");
//
//		//----- sp_spaceused2.sql -----
//		systmp.setProperty("system.predefined.sql.07.name",                        "<html><b>sp_spaceused2</b> - <i><font color=\"green\">List space and row used by each table in the current database.</font></i></html>");
////		systmp.setProperty("system.predefined.sql.07.name",                        "sp_spaceused2 - List space and row used by each table in the current database");
//		systmp.setProperty("system.predefined.sql.07.execute",                     "exec sp_spaceused2");
////		systmp.setProperty("system.predefined.sql.07.install.needsVersion",        "15000");
////		systmp.setProperty("system.predefined.sql.07.install.needsVersion",        "1500000");
//		systmp.setProperty("system.predefined.sql.07.install.needsVersion",        Ver.ver(15,0));
//		systmp.setProperty("system.predefined.sql.07.install.dbname",              "sybsystemprocs");
//		systmp.setProperty("system.predefined.sql.07.install.procName",            "sp_spaceused2");
//		systmp.setProperty("system.predefined.sql.07.install.procDateThreshold",   VersionInfo.SP_SPACEUSED2_CR_STR);
//		systmp.setProperty("system.predefined.sql.07.install.scriptLocation",      com.asetune.cm.sql.VersionInfo.class.getName());
//		systmp.setProperty("system.predefined.sql.07.install.scriptName",          "sp_spaceused2.sql");
//		systmp.setProperty("system.predefined.sql.07.install.needsRole",           "sa_role");
//
//		createPredefinedSqlMenu(menu, "system.predefined.sql.", systmp, sqlWindowInstance);
//		createPredefinedSqlMenu(menu, "user.predefined.sql.",   null,   sqlWindowInstance);
//
//		if ( menu.getMenuComponentCount() == 0 )
//		{
//			_logger.warn("No Menuitems has been assigned for the '"+menu.getText()+"'.");
//			return null;
//
////			JMenuItem empty = new JMenuItem("No Predefined SQL Statements available.");
////			empty.setEnabled(false);
////			menu.add(empty);
////
////			return menu;
//		}
//		else
//			return menu;
//	}
//
//	/**
//	 * 
//	 * @param menu   if null a new JMenuPopup will be created otherwise it will be appended to it
//	 * @param prefix prefix of the property string. Should contain a '.' at the end
//	 * @param conf
//	 * @param sqlWindowInstance can be null
//	 * @return
//	 */
//	protected static JMenu createPredefinedSqlMenu(JMenu menu, String prefix, Configuration conf, final QueryWindow sqlWindowInstance)
//	{
//		if (prefix == null)           throw new IllegalArgumentException("prefix cant be null.");
//		if (prefix.trim().equals("")) throw new IllegalArgumentException("prefix cant be empty.");
//		prefix = prefix.trim();
//		if ( ! prefix.endsWith(".") )
//			prefix = prefix + ".";
//
//		if (conf == null)
//			conf = Configuration.getCombinedConfiguration();
//
//		_logger.debug("createMenu(): prefix='"+prefix+"'.");		
//
//		//Create the menu, if it didnt exists. 
//		if (menu == null)
//			menu = new JMenu();
//
//		boolean firstAdd = true;
//		for (String prefixStr : conf.getUniqueSubKeys(prefix, true))
//		{
//			_logger.debug("createPredefinedSqlMenu(): found prefix '"+prefixStr+"'.");
//
//			// Read properties
//			final String menuItemName      = conf.getProperty(prefixStr   +".name");
//			final String sqlStr            = conf.getProperty(prefixStr   +".execute");
//			final int    needsVersion      = conf.getIntProperty(prefixStr+".install.needsVersion", 0); 
//			final String dbname            = conf.getProperty(prefixStr   +".install.dbname"); 
//			final String procName          = conf.getProperty(prefixStr   +".install.procName"); 
//			final String procDateThreshStr = conf.getProperty(prefixStr   +".install.procDateThreshold"); 
//			final String scriptLocationStr = conf.getProperty(prefixStr   +".install.scriptLocation"); 
//			final String scriptName        = conf.getProperty(prefixStr   +".install.scriptName"); 
//			final String needsRole         = conf.getProperty(prefixStr   +".install.needsRole"); 
//
//			//---------------------------------------
//			// Check that we got everything we needed
//			//---------------------------------------
//			if (menuItemName == null)
//			{
//				_logger.warn("Missing property '"+prefixStr+".name'");
//				continue;
//			}
//			if (sqlStr == null)
//			{
//				_logger.warn("Missing property '"+prefixStr+".execute'");
//				continue;
//			}
//
//			//---------------------------------------
//			// is any "install" there?
//			//---------------------------------------
//			boolean tmpDoCheckCreate = false;
//			if (dbname != null)
//			{
//				tmpDoCheckCreate = true;
//
//				String missing = "";
//				// Check rest of the mandatory parameters
//				if (procName          == null) missing += prefixStr   +".install.procName, ";
//				if (procDateThreshStr == null) missing += prefixStr   +".install.procDateThreshold, ";
////				if (scriptLocationStr == null) missing += prefixStr   +".install.scriptLocation, ";
//				if (scriptName        == null) missing += prefixStr   +".install.scriptName, ";
//				
//				if ( ! missing.equals("") )
//				{
//					_logger.warn("Missing property '"+missing+"'.");
//					continue;
//				}
//			}
//			final boolean doCheckCreate = tmpDoCheckCreate;
//
//			
//			//---------------------------------------
//			// if 'install.scriptLocation' is used...
//			//---------------------------------------
//			Class<?> tmpScriptLocation = null;
//			if (scriptLocationStr != null)
//			{
//				try { tmpScriptLocation = Class.forName(scriptLocationStr); }
//				catch (ClassNotFoundException e)
//				{
//					_logger.warn("Property "+prefixStr+".install.scriptLocation, contained '"+scriptLocationStr+"', the class can't be loaded. This should be a classname in asetune.jar, If it's a path name you want to specify, please use the property '"+prefixStr+".install.scriptName' instead.");
//					continue;
//				}
//			}
//			final Class<?> scriptLocation = tmpScriptLocation;
//
//			
//			//---------------------------------------
//			// if 'install.procDateThreshold' is used...
//			//---------------------------------------
//			java.util.Date tmpProcDateThreshold = null;
//			if (procDateThreshStr != null)
//			{
//				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//				try { tmpProcDateThreshold = sdf.parse(procDateThreshStr); }
//				catch (ParseException e)
//				{
//					_logger.warn("Property "+prefixStr+".install.procDateThreshold, contained '"+procDateThreshStr+"', Problems parsing the string, it should look like 'yyyy-MM-dd'.");
//					continue;
//				}
//			}
//			final java.util.Date procDateThreshold = tmpProcDateThreshold;
//
//			
//			//---------------------------------------
//			// Create the executor class
//			//---------------------------------------
//			ActionListener action = new ActionListener()
//			{
//				@Override
//				public void actionPerformed(ActionEvent e)
//				{
//					Connection conn = null;
//					try
//					{
//						if (sqlWindowInstance == null)
//						{
//							// Check that we are connected
////							if ( ! AseTune.getCounterCollector().isMonConnected(true, true) )
//							if ( ! CounterController.getInstance().isMonConnected(true, true) )
//							{
//								SwingUtils.showInfoMessage(MainFrame.getInstance(), "Not connected", "Not yet connected to a server.");
//								return;
//							}
//							// Get a new connection
//							conn = AseConnectionFactory.getConnection(null, Version.getAppName()+"-PreDefinedSql", null);
//							
//							// Check if the procedure exists (and create it if it dosn't)
//							if (doCheckCreate)
//								AseConnectionUtils.checkCreateStoredProc(conn, needsVersion, dbname, procName, procDateThreshold, scriptLocation, scriptName, needsRole);
//
//							// Open the SQL Window
//							QueryWindow qf = new QueryWindow(conn, sqlStr, null, true, WindowType.JFRAME, null);
//							qf.openTheWindow();
//						}
//						else
//						{
//							// Get the Connection used by sqlWindow
//							conn = sqlWindowInstance.getConnection();
//
//							// Check if the procedure exists (and create it if it dosn't)
//							if (doCheckCreate)
//								AseConnectionUtils.checkCreateStoredProc(conn, needsVersion, dbname, procName, procDateThreshold, scriptLocation, scriptName, needsRole);
//
//							// Execute the query
//							sqlWindowInstance.displayQueryResults(sqlStr, 0, false);
//						}
//					}
//					catch (Throwable t)
//					{
//						String msg = "Problems when checking/creating the procedure";
//						if (conn == null) 
//							msg = "Problems when getting a database connection";
//
//						SwingUtils.showErrorMessage("Problems executing Predefined SQL statement", 
//								msg + "\n\n" + t.getMessage(), t);
//					}
//				}
//			};
//
//			JMenuItem menuItem = new JMenuItem(menuItemName);
//			menuItem.addActionListener(action);
//
//			if ( firstAdd )
//			{
//				firstAdd = false;
//				if (menu.getMenuComponentCount() > 0)
//					menu.addSeparator();
//			}
//			menu.add(menuItem);
//		}
//
////		menu.addPopupMenuListener( createPopupMenuListener() );
//
//		return menu;
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
//	public static void setOfflineConnection(Connection conn)
	public static void setOfflineConnection(DbxConnection conn)
	{
		_offlineConn = conn;
	}

	/**
	 * Gets the <code>Connection</code> to the monitored server.
	 */
//	public static Connection getOfflineConnection()
	public static DbxConnection getOfflineConnection()
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
	@Override
	public Component getActiveTab()
	{
		return _mainTabbedPane.getSelectedComponent(true);
	}
	
	/**
	 * Clears fields in the SummaryPanel
	 */
	public static void clearSummaryData()
	{
//		_summaryPanel.clearSummaryData();
		CounterController.getSummaryPanel().clearSummaryData();
	}

	/**
	 * Updates fields in the SummaryPanel
	 */
	public static void setSummaryData(CountersModel cm)
	{
//		_summaryPanel.setSummaryData(cm);
		CounterController.getSummaryPanel().setSummaryData(cm, false);
	}

//	/**
//	 * Clears fields in the SummaryPanel
//	 */
//	public static ISummaryPanel getSummaryPanel()
//	{
//		return CounterController.getSummaryPanel();
//	}
//	public static SummaryPanel getSummaryPanel()
//	{
//		return _summaryPanel;
//	}


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
	 * Enable disable various menu options based on what type we are in
	 * @param status = ST_CONNECT, ST_OFFLINE_CONNECT, ST_DISCONNECT
	 */
	public void setMenuMode(int status)
	{
		MainFrame mf = MainFrame.getInstance();

		if (status == ST_CONNECT)
		{
			// TOOLBAR
			mf._connectTb_but                .setEnabled(false);
			mf._disConnectTb_but             .setEnabled(true);
			mf._screenshotTb_but             .setEnabled(true); // always TRUE
			mf._samplePauseTb_but            .setEnabled(true); // always TRUE
			mf._samplePlayTb_but             .setEnabled(true); // always TRUE
			mf._viewStorage_chk              .setEnabled(true);

			// File
			mf._file_m                       .setEnabled(true); // always TRUE
			mf._connect_mi                   .setEnabled(false);
			mf._disconnect_mi                .setEnabled(true);
			mf._exit_mi                      .setEnabled(true); // always TRUE

			// View
			mf._view_m                       .setEnabled(true); // always TRUE
			mf._logView_mi                   .setEnabled(true); // always TRUE
			mf._viewSrvLogFile_mi            .setEnabled(true);
			mf._offlineSessionsView_mi       .setEnabled(false);
			mf._preferences_m                .setEnabled(true); // always TRUE
			mf._refreshRate_mi               .setEnabled(true);
			mf._autoResizePcTable_mi         .setEnabled(true); // always TRUE
			mf._autoRefreshOnTabChange_mi    .setEnabled(true); // always TRUE
			mf._groupTcpInTabPane_mi         .setEnabled(true); // always TRUE
			mf._optDoGc_m                    .setEnabled(true); // always TRUE
			mf._optDoGcAfterXMinutes_mi      .setEnabled(true); // always TRUE
			mf._optDoGcAfterXMinutesValue_mi .setEnabled(true); // always TRUE
			mf._optDoGcAfterRefresh_mi       .setEnabled(true); // always TRUE
			mf._optDoGcAfterRefreshShowGui_mi.setEnabled(true); // always TRUE
			mf._dbmsConfigView_mi            .setEnabled(DbmsConfigManager.hasInstance());
			mf._tcpSettingsConf_mi           .setEnabled(true); // always TRUE
			mf._counterTabView_mi            .setEnabled(true); // always TRUE
			mf._graphView_mi                 .setEnabled(true); // always TRUE
			mf._graphs_m                     .setEnabled(true);
			
			// Tools
			mf._tools_m                      .setEnabled(true); // always TRUE
//			mf._aseConfMon_mi                .setEnabled(true);
//			mf._captureSql_mi                .setEnabled(true);
//			mf._aseAppTrace_mi               .setEnabled(true);
//			mf._aseStackTraceAnalyzer_mi     .setEnabled(true);
			mf._ddlView_mi                   .setEnabled(false);
//			mf._preDefinedSql_m              .setEnabled(true);
			mf._sqlQuery_mi                  .setEnabled(true);
//			mf._lockTool_mi                  .setEnabled();
			mf._createOffline_mi             .setEnabled(true);
			mf._wizardCrUdCm_mi              .setEnabled(true);
			mf._doGc_mi                      .setEnabled(true); // always TRUE

			// Help
			mf._help_m                       .setEnabled(true); // always TRUE
			mf._about_mi                     .setEnabled(true); // always TRUE
		}
		else if (status == ST_OFFLINE_CONNECT)
		{
			// TOOLBAR
			mf._connectTb_but                .setEnabled(false);
			mf._disConnectTb_but             .setEnabled(true);
			mf._screenshotTb_but             .setEnabled(true); // always TRUE
			mf._samplePauseTb_but            .setEnabled(true); // always TRUE
			mf._samplePlayTb_but             .setEnabled(true); // always TRUE
			mf._viewStorage_chk              .setEnabled(true);

			// File
			mf._file_m                       .setEnabled(true); // always TRUE
			mf._connect_mi                   .setEnabled(false);
			mf._disconnect_mi                .setEnabled(true);
			mf._exit_mi                      .setEnabled(true); // always TRUE

			// View
			mf._view_m                       .setEnabled(true); // always TRUE
			mf._logView_mi                   .setEnabled(true); // always TRUE
			mf._viewSrvLogFile_mi            .setEnabled(false);
			mf._offlineSessionsView_mi       .setEnabled(true);
			mf._preferences_m                .setEnabled(true); // always TRUE
			mf._refreshRate_mi               .setEnabled(false);
			mf._autoResizePcTable_mi         .setEnabled(true); // always TRUE
			mf._autoRefreshOnTabChange_mi    .setEnabled(true); // always TRUE
			mf._groupTcpInTabPane_mi         .setEnabled(true); // always TRUE
			mf._optDoGc_m                    .setEnabled(true); // always TRUE
			mf._optDoGcAfterXMinutes_mi      .setEnabled(true); // always TRUE
			mf._optDoGcAfterXMinutesValue_mi .setEnabled(true); // always TRUE
			mf._optDoGcAfterRefresh_mi       .setEnabled(true); // always TRUE
			mf._optDoGcAfterRefreshShowGui_mi.setEnabled(true); // always TRUE
			mf._dbmsConfigView_mi            .setEnabled(DbmsConfigManager.hasInstance());
			mf._tcpSettingsConf_mi           .setEnabled(true); // always TRUE
			mf._counterTabView_mi            .setEnabled(true); // always TRUE
			mf._graphView_mi                 .setEnabled(true); // always TRUE
			mf._graphs_m                     .setEnabled(true);
			
			// Tools
			mf._tools_m                      .setEnabled(true); // always TRUE
//			mf._aseConfMon_mi                .setEnabled(false);
//			mf._captureSql_mi                .setEnabled(false);
//			mf._aseAppTrace_mi               .setEnabled(false);
//			mf._aseStackTraceAnalyzer_mi     .setEnabled(true);
			mf._ddlView_mi                   .setEnabled(true);
//			mf._preDefinedSql_m              .setEnabled(false);
			mf._sqlQuery_mi                  .setEnabled(true);
//			mf._lockTool_mi                  .setEnabled();
			mf._createOffline_mi             .setEnabled(false);
			mf._wizardCrUdCm_mi              .setEnabled(false);
			mf._doGc_mi                      .setEnabled(true); // always TRUE

			// Help
			mf._help_m                       .setEnabled(true); // always TRUE
			mf._about_mi                     .setEnabled(true); // always TRUE
		}
		else if (status == ST_DISCONNECT)
		{
			// TOOLBAR
			mf._connectTb_but                .setEnabled(true);
			mf._disConnectTb_but             .setEnabled(false);
			mf._screenshotTb_but             .setEnabled(true); // always TRUE
			mf._samplePauseTb_but            .setEnabled(true); // always TRUE
			mf._samplePlayTb_but             .setEnabled(true); // always TRUE
			mf._viewStorage_chk              .setEnabled(false);

			// File
			mf._file_m                       .setEnabled(true); // always TRUE
			mf._connect_mi                   .setEnabled(true);
			mf._disconnect_mi                .setEnabled(false);
			mf._exit_mi                      .setEnabled(true); // always TRUE

			// View
			mf._view_m                       .setEnabled(true); // always TRUE
			mf._logView_mi                   .setEnabled(true); // always TRUE
			mf._viewSrvLogFile_mi            .setEnabled(false);
			mf._offlineSessionsView_mi       .setEnabled(false);
			mf._preferences_m                .setEnabled(true); // always TRUE
			mf._refreshRate_mi               .setEnabled(false);
			mf._autoResizePcTable_mi         .setEnabled(true); // always TRUE
			mf._autoRefreshOnTabChange_mi    .setEnabled(true); // always TRUE
			mf._groupTcpInTabPane_mi         .setEnabled(true); // always TRUE
			mf._optDoGc_m                    .setEnabled(true); // always TRUE
			mf._optDoGcAfterXMinutes_mi      .setEnabled(true); // always TRUE
			mf._optDoGcAfterXMinutesValue_mi .setEnabled(true); // always TRUE
			mf._optDoGcAfterRefresh_mi       .setEnabled(true); // always TRUE
			mf._optDoGcAfterRefreshShowGui_mi.setEnabled(true); // always TRUE
			mf._dbmsConfigView_mi            .setEnabled(false);
			mf._tcpSettingsConf_mi           .setEnabled(true); // always TRUE
			mf._counterTabView_mi            .setEnabled(true); // always TRUE
			mf._graphView_mi                 .setEnabled(true); // always TRUE
			mf._graphs_m                     .setEnabled(false);
			
			// Tools
			mf._tools_m                      .setEnabled(true); // always TRUE
//			mf._aseConfMon_mi                .setEnabled(false);
//			mf._captureSql_mi                .setEnabled(false);
//			mf._aseAppTrace_mi               .setEnabled(false);
//			mf._aseStackTraceAnalyzer_mi     .setEnabled(true);
			mf._ddlView_mi                   .setEnabled(false);
//			mf._preDefinedSql_m              .setEnabled(false);
			mf._sqlQuery_mi                  .setEnabled(false);
//			mf._lockTool_mi                  .setEnabled();
			mf._createOffline_mi             .setEnabled(false);
			mf._wizardCrUdCm_mi              .setEnabled(false);
			mf._doGc_mi                      .setEnabled(true); // always TRUE

			// Help
			mf._help_m                       .setEnabled(true); // always TRUE
			mf._about_mi                     .setEnabled(true); // always TRUE

			// Other stuff to hide when disconnected
			mf._viewStorage_chk              .setSelected(false);
			mf._readSlider                   .setVisible(false);
			mf._offlineSlider                .setVisible(false);
		}
		else
		{
			// UNKNOWN type.
			throw new RuntimeException("Unknown menu status '"+status+"'.");
		}
	}

	/** implemets PersistentCounterHandler.PcsQueueChange */
	@Override
	public void pcsStorageQueueChange(int pcsQueueSize, int ddlLookupQueueSize, int ddlStoreQueueSize)
	{
		_statusPcsQueueSize         .setVisible(true);
		_statusPcsDdlLookupQueueSize.setVisible(true);
		_statusPcsDdlStoreQueueSize .setVisible(true);

		_statusPcsQueueSize         .setText("" + pcsQueueSize);
		_statusPcsDdlLookupQueueSize.setText("" + ddlLookupQueueSize);
		_statusPcsDdlStoreQueueSize .setText("" + ddlStoreQueueSize);
	}
	@Override
	public void pcsConsumeInfo(String persistWriterName, Timestamp sessionStartTime, Timestamp mainSampleTime, int persistTimeInMs, int inserts, int updates, int deletes, int createTables, int alterTables, int dropTables)
	{
		_statusPcsPersistInfo.setVisible(true);
		_statusPcsPersistInfo.setText("(" + persistTimeInMs + " ms)");
		
		StringBuilder sb = new StringBuilder();
			sb.append("<html>\n");
			sb.append("Last PCS Information, For Example: How long did the last persist took.<br>\n");
			sb.append("<br>\n");
			sb.append("<TABLE ALIGN=\"left\" BORDER=0 CELLSPACING=0 CELLPADDING=0 WIDTH=\"100%\">\n");
			sb.append("    <TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>WriterName      </TD> <TD>").append( persistWriterName ).append("</TD></TR>\n");
			sb.append("    <TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>sessionStartTime</TD> <TD>").append( sessionStartTime  ).append("</TD></TR>\n");
			sb.append("    <TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>mainSampleTime  </TD> <TD>").append( mainSampleTime    ).append("</TD></TR>\n");
			sb.append("    <TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>persistTimeInMs </TD> <TD>").append( persistTimeInMs   ).append("</TD></TR>\n");
			sb.append("    <TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>inserts         </TD> <TD>").append( inserts           ).append("</TD></TR>\n");
			sb.append("    <TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>updates         </TD> <TD>").append( updates           ).append("</TD></TR>\n");
			sb.append("    <TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>deletes         </TD> <TD>").append( deletes           ).append("</TD></TR>\n");
			sb.append("    <TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>createTables    </TD> <TD>").append( createTables      ).append("</TD></TR>\n");
			sb.append("    <TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>alterTables     </TD> <TD>").append( alterTables       ).append("</TD></TR>\n");
			sb.append("    <TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>dropTables      </TD> <TD>").append( dropTables        ).append("</TD></TR>\n");
			sb.append("</TABLE>\n");
			sb.append("</html>");
		_statusPcsPersistInfo.setToolTipText(sb.toString());
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
	public void setStatus(int type)
	{
		setStatus(type, null);
	}

	/**
	 * This class is used to apply setStatus(int type, String param) is it's NOT called by the EDT (Event Dispatch Thread)<br>
	 * Created a single class, so we don't have to create a new Runnable class "on the fly" for every call... 
	 */
	private static class StatusWrapper
	implements Runnable
	{
		private boolean _invokeAndWait = DEFAULT_setStatus_invokeAndWait;
		private int    _type;
		private String _param;

		public StatusWrapper(boolean invokeAndWait)
		{
			_invokeAndWait = invokeAndWait;
		}

		public void setStatus(int type, String param)
		{
			_type  = type;
			_param = param;
			
			if ( SwingUtils.isEventQueueThread() )
				setStatus(type, param);
			else
			{
				if (_invokeAndWait)
				{
    				try { SwingUtilities.invokeAndWait(this); }
    				catch (InterruptedException e)      { _logger.info("StatusWrapper.setStatus(), calling SwingUtilities.invokeAndWait(), Caught: "+e); }
    				catch (InvocationTargetException e) { _logger.warn("StatusWrapper.setStatus(), calling SwingUtilities.invokeAndWait(), Caught: "+e, e); }
				}
				else
				{
					SwingUtilities.invokeLater(this);
				}
			}
		}
		@Override
		public void run()
		{
			MainFrame.getInstance().setStatus(_type, _param);
		}
	}
	private static StatusWrapper _statusWrapper;

	/**
	 * Sets values in the status panel.
	 * @param type <code>ST_CONNECT, ST_DISCONNECT, ST_STATUS_FIELD, ST_MEMORY</code>
	 * @param param The actual string to set (this is only used for <code>ST_STATUS_FIELD</code>)
	 */
	public void setStatus(int type, String param)
	{
		// If this is NOT the event queue thread, dispatch it to that thread
		// But instead of creating a Runnable class for every call, reuse the Runnable, implemented in _statusWrapper
		if ( ! SwingUtils.isEventQueueThread() )
		{
			if (_logger.isDebugEnabled())
				_logger.debug("MainFrame.setStatus() -NOT-IN-EDT-: ThreadName=='"+Thread.currentThread().getName()+"', type="+type+", param='"+param+"'.");

//System.out.println("MainFrame.setStatus() -NOT-IN-EDT-: ThreadName=='"+Thread.currentThread().getName()+"', type="+type+", param='"+param+"'.");
			if (_statusWrapper == null)
				_statusWrapper = new StatusWrapper(Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_setStatus_invokeAndWait, DEFAULT_setStatus_invokeAndWait));

			_statusWrapper.setStatus(type, param);
			return;
		}

		if (type == ST_OFFLINE_CONNECT)
		{
			if (isOfflineConnected())
			{
				// If current status already this, get out of here
				if (_lastKnownStatus == type)
					return;

				_statusStatus    .setText("Offline...");
				_statusStatus2   .setText(ST_DEFAULT_STATUS2_FIELD);
				_statusServerName.setText("Offline-read");
				_statusServerListeners.setText("Offline-read");

				_statusPcsQueueSize         .setVisible(false);
				_statusPcsPersistInfo       .setVisible(false);
				_statusPcsDdlLookupQueueSize.setVisible(false);
				_statusPcsDdlStoreQueueSize .setVisible(false);

				if (CounterController.hasInstance())
				{
//					_summaryPanel.setLocalServerName("Offline-read");
					CounterController.getSummaryPanel().setLocalServerName("Offline-read");

					// SET WATERMARK
//					SummaryPanel.getInstance().setWatermark();
					CounterController.getSummaryPanel().setWatermark();
				}

				setMenuMode(ST_OFFLINE_CONNECT);
				_lastKnownStatus = ST_OFFLINE_CONNECT;
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
//			if (AseTune.getCounterCollector().isMonConnected(true, true))
			if (CounterController.getInstance().isMonConnected(true, true))
			{
				// If current status already this, get out of here
				if (_lastKnownStatus == type)
					return;

				_statusStatus    .setText("Just Connected...");
				_statusStatus2   .setText(ST_DEFAULT_STATUS2_FIELD);

//				_statusServerName.setText(
//						AseConnectionFactory.getServer() + " (" +
//						AseConnectionFactory.getHost()   + ":" +
//						AseConnectionFactory.getPort()   + ")"
//						);
				_statusServerName.setText(
						AseConnectionFactory.getUser() +
						" - " + AseConnectionFactory.getServer() +
						" ("  + AseConnectionFactory.getHostPortStr() + ")" );

				if (DbUtils.isProductName(_connectedToProductName, DbUtils.DB_PROD_NAME_SYBASE_ASE))
				{
					Connection conn = CounterController.getInstance().getMonConnection();
					if (AseConnectionUtils.hasRole(conn, AseConnectionUtils.SA_ROLE))
						_statusServerListeners.setText(AseConnectionUtils.getListeners(conn, true, true, _instance));
					else
						_statusServerListeners.setText("Need 'sa_role' to get listeners.");
				}
				else
				{
					_statusServerListeners.setText("");
				}

				if (CounterController.hasInstance())
				{
//					_summaryPanel.setLocalServerName(_statusServerName.getText());
					CounterController.getSummaryPanel().setLocalServerName(_statusServerName.getText());

					// SET WATERMARK
//					SummaryPanel.getInstance().setWatermark();
					CounterController.getSummaryPanel().setWatermark();
				}
				
				// Set servername in windows - title
				String aseSrv      = AseConnectionFactory.getServer();
				String aseHostPort = AseConnectionFactory.getHostPortStr();
				String srvStr      = aseSrv != null ? aseSrv : aseHostPort; 
				getInstance().setTitle(Version.getAppName(), srvStr);

				boolean hasPcsStorage = PersistentCounterHandler.hasInstance();
				_statusPcsQueueSize         .setVisible(hasPcsStorage);
				_statusPcsPersistInfo       .setVisible(hasPcsStorage);
				_statusPcsDdlLookupQueueSize.setVisible(hasPcsStorage);
				_statusPcsDdlStoreQueueSize .setVisible(hasPcsStorage);

				setMenuMode(ST_CONNECT);
				_lastKnownStatus = ST_CONNECT;
			}
			else
			{
				type = ST_DISCONNECT;
			}
		}
			
			
		// DISCONNECT
		if (type == ST_DISCONNECT)
		{
			// If current status already this, get out of here
			if (_lastKnownStatus == type)
				return;

			_statusServerName     .setText(ST_DEFAULT_SERVER_NAME);
			_statusServerListeners.setText(ST_DEFAULT_SERVER_LISTENERS);
			_statusStatus         .setText(ST_DEFAULT_STATUS_FIELD);
			_statusStatus2        .setText(ST_DEFAULT_STATUS2_FIELD);

			_statusPcsQueueSize         .setVisible(false);
			_statusPcsPersistInfo       .setVisible(false);
			_statusPcsDdlLookupQueueSize.setVisible(false);
			_statusPcsDdlStoreQueueSize .setVisible(false);

			if (CounterController.hasInstance())
			{
//				_summaryPanel.setLocalServerName("");
				CounterController.getSummaryPanel().setLocalServerName("");
				
				// SET WATERMARK
//				SummaryPanel.getInstance().setWatermark();
				CounterController.getSummaryPanel().setWatermark();
			}

			for (TabularCntrPanel tcp : _TcpMap.values())
			{
				tcp.setWatermark();
			}

			// Reset servername in windows - title
			getInstance().setTitle(Version.getAppName(), null);

			setMenuMode(ST_DISCONNECT);
			_lastKnownStatus = ST_DISCONNECT;
		}

		// STATUS
		if (type == ST_STATUS_FIELD)
		{
			String curVal = _statusStatus.getText();
			if ( ! curVal.equals(param) )
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

	/**
	 * Set the windws title
	 * @param appName name of the application.
	 * @param srvStr servername we are connected to, null = not connected.
	 */
	private void setTitle(String appName, String srvStr)
	{
		if (appName == null)
			appName = Version.getAppName();

		String title = appName;
		
		if (srvStr != null)
			title += " - " + srvStr;
		
		if (_windowTitleAppend != null)
			title += " - " + _windowTitleAppend;
		
		setTitle(title);
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
		if (sql == null)
		{
			if (    "SPID"          .equalsIgnoreCase(colName) // From a bunch of places
			     || "OldestTranSpid".equalsIgnoreCase(colName) // from CmOpenDatabases
			     || "KPID"          .equalsIgnoreCase(colName) // From a bunch of places
			     || "OwnerPID"      .equalsIgnoreCase(colName) // CmSpinlockActivity
			     || "LastOwnerPID"  .equalsIgnoreCase(colName) // CmSpinlockActivity
			   )
			{
				String monWaitEventInfoWhere = "";
				if (MonTablesDictionaryManager.hasInstance())
				{
//					if (MonTablesDictionary.getInstance().getMdaVersion() >= 15700)
//					if (MonTablesDictionary.getInstance().getMdaVersion() >= 1570000)
					if (MonTablesDictionaryManager.getInstance().getMdaVersion() >= Ver.ver(15,7))
						monWaitEventInfoWhere = " and W.Language = 'en_US'";
				}

				// Determine the COLUMN name to be used in the search
				String whereColName = "MP.SPID";
				if (    "KPID"          .equalsIgnoreCase(colName) // From a bunch of places
					 || "OwnerPID"      .equalsIgnoreCase(colName) // CmSpinlockActivity
					 || "LastOwnerPID"  .equalsIgnoreCase(colName) // CmSpinlockActivity
				   )
				{
					whereColName = "MP.KPID";
				}

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
					" WaitEventDescription = (select W.Description from master..monWaitEventInfo W where W.WaitEventID = MP.WaitEventID "+monWaitEventInfoWhere+"), " +
					" MP.BlockingSPID, " +
					" procname = (select isnull(object_name(sp.id, sp.dbid), object_name(sp.id, 2)) from master..sysprocesses sp where sp.spid = MP.SPID), " +
					" MP.BatchID, " +
					" MP.LineNumber, " +
					" MP.BlockingXLOID, " +
					" MP.MasterTransactionID" +
					" from monProcess MP " +
					" where "+whereColName+" = ? ";
			}
		}
		
		return sql;
	}

	public static boolean isSamplingPaused()
	{
		return _isSamplingPaused;
	}

	/** Check if someone has hit F5, so we can "override" the PAUSED fuctionality */
	public static boolean isForcedRefresh()
	{
		return _isForcedRefresh;
	}

	/** set this when hit F5, so we can "override" the PAUSED fuctionality */
	public static void setForcedRefresh(boolean b)
	{
		_isForcedRefresh = b;
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
		// do not save stuff during initialization
		if ( ! _initialized )
			return;

		//_logger.debug("xxxxx: " + e.getWindow().getSize());
		Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);

		tmpConf.setProperty("main.refresh.interval", _refreshInterval);
		tmpConf.setProperty("nogui.sleepTime",       _refreshNoGuiInterval);

		tmpConf.setProperty("TabularCntrPanel.autoAdjustTableColumnWidth", _autoResizePcTable_mi.isSelected());
		tmpConf.setProperty("TabularCntrPanel.autoRefreshOnTabChange",     _autoRefreshOnTabChange_mi.isSelected());
		tmpConf.setProperty(PROPKEY_useTcpGroups,                          _groupTcpInTabPane_mi.isSelected());
		tmpConf.setProperty(PROPKEY_doJavaGcAfterXMinutes,                 _optDoGcAfterXMinutes_mi.isSelected());
		tmpConf.setProperty(PROPKEY_doJavaGcAfterRefresh,                  _optDoGcAfterRefresh_mi.isSelected());
		tmpConf.setProperty(PROPKEY_doJavaGcAfterRefreshShowGui,           _optDoGcAfterRefreshShowGui_mi.isSelected());

		tmpConf.setProperty(PROPKEY_summaryOperations_showAbs,             _optSummaryOperShowAbs_mi .isSelected());
		tmpConf.setProperty(PROPKEY_summaryOperations_showDiff,            _optSummaryOperShowDiff_mi.isSelected());
		tmpConf.setProperty(PROPKEY_summaryOperations_showRate,            _optSummaryOperShowRate_mi.isSelected());

//		tmpConf.setProperty("mainTabbedPane.tabLayoutPolicy", _mainTabbedPane.getTabLayoutPolicy());

		tmpConf.setProperty("window.width",  getSize().width);
		tmpConf.setProperty("window.height", getSize().height);
		if (isVisible())
		{
			tmpConf.setProperty("window.pos.x",  getLocationOnScreen().x);
			tmpConf.setProperty("window.pos.y",  getLocationOnScreen().y);
		}
		
//		getSummaryPanel().saveLayoutProps();
		CounterController.getSummaryPanel().saveLayoutProps();

		tmpConf.save();
	}

	private void loadProps()
	{
//		Configuration tmpConf = Configuration.getInstance(Configuration.TEMP);
		Configuration tmpConf = Configuration.getCombinedConfiguration();

//		Dimension crtSize = Toolkit.getDefaultToolkit().getScreenSize();
		Rectangle crtSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();

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

//			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			Rectangle screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
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
			if ( ! SwingUtils.isOutOfScreen(winPosX, winPosY, width, height) )
			{
				_logger.debug("Open main window in last known position.");
				setLocation(winPosX, winPosY);
			}
		}

		_refreshInterval      = tmpConf.getIntProperty(PROPKEY_refreshInterval,      getDefaultRefreshInterval());
		_refreshNoGuiInterval = tmpConf.getIntProperty(PROPKEY_refreshIntervalNoGui, DEFAULT_refreshIntervalNoGui);
		
//		_sampleInterval_cbx.getModel().setSelectedItem( _refreshInterval );
		boolean foundRefreshRate = false;
		for (int i=0; i<_sampleInterval_cbx.getItemCount(); i++)
		{
			if (_sampleInterval_cbx.getItemAt(i).equals(_refreshInterval))
			{
				foundRefreshRate = true;
				_sampleInterval_cbx.setSelectedIndex(i);
			}
		}
		if ( ! foundRefreshRate )
			_sampleInterval_cbx.addItem(_refreshInterval);
			

//		int tabLayoutPolicy = tmpConf.getIntProperty("mainTabbedPane.tabLayoutPolicy", JTabbedPane.WRAP_TAB_LAYOUT);
//		_mainTabbedPane.setTabLayoutPolicy(tabLayoutPolicy);

		boolean bool; 
		bool = tmpConf.getBooleanProperty("TabularCntrPanel.autoAdjustTableColumnWidth", _autoResizePcTable_mi.isSelected());
		_autoResizePcTable_mi.setSelected(bool);

		bool = tmpConf.getBooleanProperty("TabularCntrPanel.autoRefreshOnTabChange",     _autoRefreshOnTabChange_mi.isSelected());
		_autoRefreshOnTabChange_mi.setSelected(bool);

		_groupTcpInTabPane_mi         .setSelected(tmpConf.getBooleanProperty(PROPKEY_useTcpGroups,                DEFAULT_useTcpGroups));
		_optDoGcAfterXMinutes_mi      .setSelected(tmpConf.getBooleanProperty(PROPKEY_doJavaGcAfterXMinutes,       DEFAULT_doJavaGcAfterXMinutes));
		_optDoGcAfterRefresh_mi       .setSelected(tmpConf.getBooleanProperty(PROPKEY_doJavaGcAfterRefresh,        DEFAULT_doJavaGcAfterRefresh));
		_optDoGcAfterRefreshShowGui_mi.setSelected(tmpConf.getBooleanProperty(PROPKEY_doJavaGcAfterRefreshShowGui, DEFAULT_doJavaGcAfterRefreshShowGui));

		_optSummaryOperShowAbs_mi .setSelected(tmpConf.getBooleanProperty(PROPKEY_summaryOperations_showAbs,  DEFAULT_summaryOperations_showAbs));
		_optSummaryOperShowDiff_mi.setSelected(tmpConf.getBooleanProperty(PROPKEY_summaryOperations_showDiff, DEFAULT_summaryOperations_showDiff));
		_optSummaryOperShowRate_mi.setSelected(tmpConf.getBooleanProperty(PROPKEY_summaryOperations_showRate, DEFAULT_summaryOperations_showRate));
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
	
		@Override
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
		@Override
		public void actionPerformed(ActionEvent actionevent)
		{
			WaitForExecDialog execWait = new WaitForExecDialog(_mainframe, "Loading in-memory history entry.");
			BgExecutor doWork = new BgExecutor(execWait)
			{
				@Override
				public Object doWork()
				{
					_mainframe.readSliderMoveToCurrentTs();
					_mainframe._readSelectionTimer.stop();
					
					return null;
				}
			};
			execWait.execAndWait(doWork, 100);
//			_mainframe.readSliderMoveToCurrentTs();
//			_mainframe._readSelectionTimer.stop();
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
		@Override
		public void actionPerformed(ActionEvent actionevent)
		{
			_mainframe.offlineSliderMoveToCurrentTs();
			_mainframe._offlineSelectionTimer.stop();
		}
	}

	/**
	 * Extended GTabbedPane, which checks all subTabs if they have valid data, if so paint a green stripe...<br>
	 * This will simply "highlight" that we have valid data under this tab.
	 */
	private static class XGTabbedPane
	extends GTabbedPane 
	{
		private static final long serialVersionUID = 1L;

		// Decides if we should place the indicator to left or right on the icon.
		private boolean _indicatorToLeft = true;
		
		private Icon[] _originIcons    = {};
		private Icon[] _indicatorIcons = {};

		public XGTabbedPane(String name)
		{
			super(name);

			// check what look and feel we are using, then decide where the indicator marker goes
			// only one I know that has the 'tab' icons to right of the text is 'GTK'
			String lookAndFeelName = UIManager.getLookAndFeel().getName();
			_indicatorToLeft = true;
			if ( lookAndFeelName != null && lookAndFeelName.equals("GTK look and feel") )
				_indicatorToLeft = false;
		}

		@Override
		public void paintSpecial()
		{
			// First call the super to do it's job
			super.paintSpecial();

			// resize origin/indicator array
			if (_originIcons.length < getTabCount())
			{
				_originIcons    = new Icon[getTabCount()];
				_indicatorIcons = new Icon[getTabCount()];

				// Initialize the 'origin' icon
				// NOTE: this will not work if we have changed the original icon since we have added icons... 
				//       then it might be better to wrok with a ArrayList instead 
				for (int t=0; t<getTabCount(); t++)
					_originIcons[t] = getIconAt(t);
			}

			// Loop the tabs, in this (the main tabs)
			for (int tcL1=0; tcL1<getTabCount(); tcL1++)
			{
				// We only need to check sub components if the tab is a TabbedPane
				Component compL1 = getComponentAt(tcL1);
				if (compL1 instanceof JTabbedPane)
				{
					JTabbedPane tpL1 = (JTabbedPane) compL1;

					// If we dont have a icon for the tab, go to the next tab
					Icon icon = _originIcons[tcL1];
					if (icon == null)
						continue;

					boolean anyTcpHasValidSampleData = false;

					// Now loop the second level
					for (int tcL2=0; tcL2<tpL1.getTabCount(); tcL2++)
					{
						// we only chek "valid sample data" for TabularCntrPanel components
						Component compL2 = tpL1.getComponentAt(tcL2);
						if (compL2 instanceof TabularCntrPanel)
						{
							TabularCntrPanel tcp = (TabularCntrPanel) compL2;
							if (tcp.hasValidSampleData())
							{
								anyTcpHasValidSampleData = true;
								break; // noo need to contune when we have found one...
							}
						}
					}

					// set INDICATOR or ORIGINAL icon
					if (anyTcpHasValidSampleData)
					{
						// Get cached indicator icon, if none was found create one...
						Icon indicatorIcon = _indicatorIcons[tcL1]; 
						if ( indicatorIcon == null )
						{
							// Image, which we will paint a green stripe and the Icon on
							BufferedImage im = new BufferedImage(icon.getIconWidth() + 2, icon.getIconHeight(), BufferedImage.TRANSLUCENT);
							Graphics2D img = im.createGraphics();
							img.setColor(Color.GREEN);

							if ( _indicatorToLeft )
							{
								icon.paintIcon(null, img, 2, 0);
								img.fillRect(0, 0, 2, icon.getIconHeight());
							}
							else
							{
								icon.paintIcon(null, img, 0, 0);
								img.fillRect(icon.getIconWidth(), 0, 2, icon.getIconHeight());
							}

							// Cache the created indicator icon
							indicatorIcon = new ImageIcon(im);
							_indicatorIcons[tcL1] = indicatorIcon;
						}

						// set the "green" striped icon
						setIconAt(tcL1, indicatorIcon);
					}
					else
					{
						// Swap icon back to original
						Icon originIcon = _originIcons[tcL1]; 
						setIconAt(tcL1, originIcon);
					}
				} // end: compL1
			} // end: loop tabs on mainTabbedPane
		} // end: paintSpecial()
	} // end: XGTabbedPane

} // END: MAIN CLASS
