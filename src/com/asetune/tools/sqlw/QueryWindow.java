/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.tools.sqlw;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.FileLocation;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTableHeader;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.asetune.AseConfig;
import com.asetune.AseConfigText;
import com.asetune.AseConfigText.ConfigType;
import com.asetune.DebugOptions;
import com.asetune.Version;
import com.asetune.check.CheckForUpdates;
import com.asetune.check.CheckForUpdates.SqlwConnectInfo;
import com.asetune.check.CheckForUpdates.SqlwUsageInfo;
import com.asetune.gui.AseConfigViewDialog;
import com.asetune.gui.AsePlanViewer;
import com.asetune.gui.CommandHistoryDialog;
import com.asetune.gui.ConnectionDialog;
import com.asetune.gui.FavoriteCommandDialog;
import com.asetune.gui.FavoriteCommandDialog.FavoriteCommandEntry;
import com.asetune.gui.MainFrame;
import com.asetune.gui.ParameterDialog;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.gui.focusabletip.FocusableTip;
import com.asetune.gui.swing.AbstractComponentDecorator;
import com.asetune.gui.swing.EventQueueProxy;
import com.asetune.gui.swing.RXTextUtilities;
import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.gui.swing.WaitForExecDialog.BgExecutor;
import com.asetune.parser.ParserProperties;
import com.asetune.sql.SqlProgressDialog;
import com.asetune.sql.pipe.PipeCommand;
import com.asetune.sql.pipe.PipeCommandBcp;
import com.asetune.sql.pipe.PipeCommandException;
import com.asetune.sql.pipe.PipeCommandGrep;
import com.asetune.tools.NormalExitException;
import com.asetune.tools.WindowType;
import com.asetune.tools.sqlcapture.ProcessDetailFrame;
import com.asetune.tools.sqlw.StatusBar.ServerInfo;
import com.asetune.tools.tailw.LogTailWindow;
import com.asetune.ui.autocomplete.CompletionProviderAbstract;
import com.asetune.ui.autocomplete.CompletionProviderAbstractSql;
import com.asetune.ui.autocomplete.CompletionProviderAse;
import com.asetune.ui.autocomplete.CompletionProviderJdbc;
import com.asetune.ui.autocomplete.CompletionProviderRepServer;
import com.asetune.ui.rsyntaxtextarea.AsetuneSyntaxConstants;
import com.asetune.ui.rsyntaxtextarea.AsetuneTokenMaker;
import com.asetune.ui.rsyntaxtextarea.RSyntaxTextAreaX;
import com.asetune.ui.rsyntaxtextarea.RSyntaxUtilitiesX;
import com.asetune.ui.tooltip.suppliers.ToolTipSupplierAbstract;
import com.asetune.ui.tooltip.suppliers.ToolTipSupplierAsa;
import com.asetune.ui.tooltip.suppliers.ToolTipSupplierAse;
import com.asetune.ui.tooltip.suppliers.ToolTipSupplierH2;
import com.asetune.ui.tooltip.suppliers.ToolTipSupplierHana;
import com.asetune.ui.tooltip.suppliers.ToolTipSupplierIq;
import com.asetune.ui.tooltip.suppliers.ToolTipSupplierJdbc;
import com.asetune.ui.tooltip.suppliers.ToolTipSupplierMsSql;
import com.asetune.ui.tooltip.suppliers.ToolTipSupplierOracle;
import com.asetune.ui.tooltip.suppliers.ToolTipSupplierRepServer;
import com.asetune.ui.tooltip.suppliers.ToolTipSupplierTester;
import com.asetune.ui.tooltip.suppliers.TtpEntryCompletionProvider;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.AseSqlScriptReader;
import com.asetune.utils.ColorUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.DbUtils;
import com.asetune.utils.Debug;
import com.asetune.utils.Encrypter;
import com.asetune.utils.FileUtils;
import com.asetune.utils.GoSyntaxException;
import com.asetune.utils.JavaVersion;
import com.asetune.utils.JdbcDriverHelper;
import com.asetune.utils.Logging;
import com.asetune.utils.PlatformUtils;
import com.asetune.utils.PropPropEntry;
import com.asetune.utils.RepServerUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;
import com.asetune.utils.TimeUtils;
import com.asetune.utils.Ver;
import com.asetune.utils.WatchdogIsFileChanged;
import com.asetune.xmenu.TablePopupFactory;
import com.sybase.jdbcx.EedInfo;
import com.sybase.jdbcx.SybConnection;
import com.sybase.jdbcx.SybMessageHandler;

/**
 * This class creates a Swing GUI that allows the user to enter a SQL query.
 * It then obtains a ResultSetTableModel for the query and uses it to display
 * the results of the query in a scrolling JTable component.
 **/
public class QueryWindow
//	extends JFrame
//	extends JDialog
//implements ActionListener, SybMessageHandler, ConnectionProvider, CaretListener, CommandHistoryDialog.HistoryExecutor
	implements ActionListener, ConnectionProvider, CaretListener, DocumentListener, CommandHistoryDialog.HistoryExecutor, WatchdogIsFileChanged.WatchdogIsFileChangedChecker
{
	private static Logger _logger = Logger.getLogger(QueryWindow.class);

	private static final String LOCAL_JVM = ManagementFactory.getRuntimeMXBean().getName();
	
	private final static String NOT_CONNECTED_STR              = "not connected";

	public final static String  APP_NAME                       = "SqlWindow";

	public final static String  PROPKEY_APP_PREFIX             = "QueryWindow.";

	public final static String  PROPKEY_showAppNameInTitle     = PROPKEY_APP_PREFIX + "showAppNameInTitle";
	public final static boolean DEFAULT_showAppNameInTitle     = true;

	public final static String  PROPKEY_horizontalOrientation  = PROPKEY_APP_PREFIX + "horizontalOrientation";
	public final static boolean DEFAULT_horizontalOrientation  = false;

	public final static String  PROPKEY_commandPanelInToolbar  = PROPKEY_APP_PREFIX + "commandPanelInToolbar";
	public final static boolean DEFAULT_commandPanelInToolbar  = false;

	public final static String  PROPKEY_saveBeforeExecute      = PROPKEY_APP_PREFIX + "saveBeforeExecute";
	public final static boolean DEFAULT_saveBeforeExecute      = false;
	
	public final static String  PROPKEY_asPlainText            = PROPKEY_APP_PREFIX + "asPlainText";
	public final static boolean DEFAULT_asPlainText            = false;
	
	public final static String  PROPKEY_showRowCount           = PROPKEY_APP_PREFIX + "showRowCount";
	public final static boolean DEFAULT_showRowCount           = true;

	public final static String  PROPKEY_limitRsRowsRead        = PROPKEY_APP_PREFIX + "limitRsRowsRead";
	public final static boolean DEFAULT_limitRsRowsRead        = false;

	public final static String  PROPKEY_limitRsRowsReadCount   = PROPKEY_APP_PREFIX + "limitRsRowsReadCount";
	public final static int     DEFAULT_limitRsRowsReadCount   = 1000;

	public final static String  PROPKEY_showSentSql            = PROPKEY_APP_PREFIX + "showSentSql";
	public final static boolean DEFAULT_showSentSql            = false;
	
	public final static String  PROPKEY_printRsInfo            = PROPKEY_APP_PREFIX + "printRsInfo";
	public final static boolean DEFAULT_printRsInfo            = false;
	
	public final static String  PROPKEY_clientTiming           = PROPKEY_APP_PREFIX + "clientTiming";
	public final static boolean DEFAULT_clientTiming           = false;
	
	public final static String  PROPKEY_useSemicolonHack       = PROPKEY_APP_PREFIX + "useSemicolonHack";
	public final static boolean DEFAULT_useSemicolonHack       = false;
	
	public final static String  PROPKEY_enableDbmsOutput       = PROPKEY_APP_PREFIX + "enableDbmsOutput";
	public final static boolean DEFAULT_enableDbmsOutput       = false;
	
	public final static String  PROPKEY_enableDbmsInitSize     = PROPKEY_APP_PREFIX + "enableDbmsInitSize";
	public final static int     DEFAULT_enableDbmsInitSize     = 1000000;
	
	public final static String  PROPKEY_getObjectTextOnError   = PROPKEY_APP_PREFIX + "getObjectTextOnError";
	public final static boolean DEFAULT_getObjectTextOnError   = true;
	
	public final static String  PROPKEY_appendResults          = PROPKEY_APP_PREFIX + "appendResults";
	public final static boolean DEFAULT_appendResults          = false;
	
	public final static String  PROPKEY_jdbcAutoCommit         = PROPKEY_APP_PREFIX + "jdbc.autoCommit";
	public final static boolean DEFAULT_jdbcAutoCommit         = true;
	
	public final static String  PROPKEY_jdbcAutoCommitShow     = PROPKEY_APP_PREFIX + "jdbc.autoCommit.show";
	public final static boolean DEFAULT_jdbcAutoCommitShow     = false;
	
	public final static String  PROPKEY_lastFileNameSaveMax    = "LastFileList.saveSize";
	public final static int     DEFAULT_lastFileNameSaveMax    = 20;

	public final static String  PROPKEY_historyFileName        = PROPKEY_APP_PREFIX + "CommandHistory.filename";
	public final static String  DEFAULT_historyFileName        = Version.APP_STORE_DIR + File.separator + APP_NAME + ".command.history.xml";

	public final static String  PROPKEY_favoriteCmdFileNameSql = PROPKEY_APP_PREFIX + "FavoritCommands.filename.sql";
	public final static String  DEFAULT_favoriteCmdFileNameSql = Version.APP_STORE_DIR + File.separator + APP_NAME + ".favorite.sql.commands.xml";

	public final static String  PROPKEY_favoriteCmdFileNameRcl = PROPKEY_APP_PREFIX + "FavoritCommands.filename.rcl";
	public final static String  DEFAULT_favoriteCmdFileNameRcl = Version.APP_STORE_DIR + File.separator + APP_NAME + ".favorite.rcl.commands.xml";

	public final static String  PROPKEY_restoreWinSizeForConn  = PROPKEY_APP_PREFIX + "restoreWinSizeForConn";
	public final static boolean DEFAULT_restoreWinSizeForConn  = false;
	
	public final static String  PROPKEY_sqlBatchTerminator     = AseSqlScriptReader.PROPKEY_sqlBatchTerminator;
	public final static String  DEFAULT_sqlBatchTerminator     = AseSqlScriptReader.DEFAULT_sqlBatchTerminator;

	static
	{
		Configuration.registerDefaultValue(PROPKEY_asPlainText,         DEFAULT_asPlainText);
		Configuration.registerDefaultValue(PROPKEY_lastFileNameSaveMax, DEFAULT_lastFileNameSaveMax);
	}
	
	/** Completion Provider for RSyntaxTextArea */
	private CompletionProviderAbstract _compleationProviderAbstract = null;
	private ToolTipSupplierAbstract    _tooltipProviderAbstract     = null;

	//-------------------------------------------------
	// Actions
	public static final String ACTION_CONNECT                   = "CONNECT";
	public static final String ACTION_DISCONNECT                = "DISCONNECT";
	public static final String ACTION_CLONE_CONNECT             = "CLONE_CONNECT";
	public static final String ACTION_FILE_NEW                  = "FILE_NEW";
	public static final String ACTION_FILE_OPEN                 = "FILE_OPEN";
//	public static final String ACTION_FILE_CLOSE                = "FILE_CLOSE";
	public static final String ACTION_FILE_SAVE                 = "FILE_SAVE";
	public static final String ACTION_FILE_SAVE_AS              = "FILE_SAVE_AS";
	public static final String ACTION_EXIT                      = "EXIT";

	public static final String ACTION_EXECUTE                   = "EXECUTE";
	public static final String ACTION_EXECUTE_GUI_SHOWPLAN      = "EXECUTE_GUI_SHOWPLAN";

	public static final String ACTION_SPLITPANE_TOGGLE          = "SPLITPANE_TOGGLE";

	public static final String ACTION_CMD_SQL                   = "CMD_SQL";
	public static final String ACTION_CMD_RCL                   = "CMD_RCL";

	public static final String ACTION_VIEW_CMD_HISTORY          = "VIEW_CMD_HISTORY";
	public static final String ACTION_VIEW_LOG_TAIL             = "VIEW_LOG_TAIL";
	public static final String ACTION_VIEW_ASE_CONFIG           = "VIEW_ASE_CONFIG";
	public static final String ACTION_RS_GENERATE_CHANGED_DDL   = "RS_GENERATE_CHANGED_DDL";
	public static final String ACTION_RS_GENERATE_ALL_DDL       = "RS_GENERATE_ALL_DDL";
	public static final String ACTION_RS_DUMP_QUEUE             = "RS_DUMP_QUEUE";
	public static final String ACTION_RS_WHO_IS_DOWN            = "RS_WHO_IS_DOWN";
	public static final String ACTION_ASE_CAPTURE_SQL           = "ASE_CAPTURE_SQL";

	public static final String ACTION_PREV_ERROR                = "PREV_ERROR";
	public static final String ACTION_NEXT_ERROR                = "NEXT_ERROR";

	private static final Color DEFAULT_OUTPUT_ERROR_HIGHLIGHT_COLOR	= new Color(255,255,170);

	private Connection  _conn            = null;
	private int         _connType        = -1;
	private AseConnectionUtils.ConnectionStateInfo _aseConnectionStateInfo  = null;
	private DbUtils.JdbcConnectionStateInfo        _jdbcConnectionStateInfo = null;
	

//	private JTextArea	      _query_txt                  = new JTextArea();           // A field to enter a query in
//	private RSyntaxTextArea	  _query_txt                  = new RSyntaxTextArea();     // A field to enter a query in
	private TextEditorPane	  _query_txt                  = new TextEditorPane();    // A field to enter a query in
	private RTextScrollPane   _queryScroll                = new RTextScrollPane(_query_txt, true);
	private ErrorStrip        _queryErrStrip              = new ErrorStrip(_query_txt);
	private RSyntaxTextAreaX  _result_txt                 = null;
	private JButton           _exec_but                   = new ExecButton("Exec");       // Execute the
	private JButton           _execGuiShowplan_but        = new ExecButton("Exec, GUI Showplan");    // Execute, but display it with a GUI showplan
	private JButton           _setAseOptions_but          = new JButton("Set");        // Do various set ... options
	private JButton           _copy_but                   = new JButton("Copy Res");    // Copy All resultsets to clipboard
	private JButton           _nextErr_but                = new JButton("Next");
	private JButton           _prevErr_but                = new JButton("Prev");

	private JCheckBoxMenuItem _rsInTabs_chk               = new JCheckBoxMenuItem("In Tabbed Panel", false);
	private JCheckBoxMenuItem _asPlainText_chk            = new JCheckBoxMenuItem("As Plain Text", DEFAULT_asPlainText);
	private JCheckBoxMenuItem _showRowCount_chk           = new JCheckBoxMenuItem("Row Count", DEFAULT_showRowCount);
	private JCheckBoxMenuItem _limitRsRowsRead_chk        = new JCheckBoxMenuItem("Limit ResultSet to # rows", DEFAULT_limitRsRowsRead);
	private JMenuItem         _limitRsRowsReadDialog_mi   = new JMenuItem        ("Limit ResultSet to # rows, settings...");
	private JCheckBoxMenuItem _showSentSql_chk            = new JCheckBoxMenuItem("Print Sent SQL", DEFAULT_showSentSql);
	private JCheckBoxMenuItem _printRsInfo_chk            = new JCheckBoxMenuItem("Print ResultSet Info", DEFAULT_printRsInfo);
	private JCheckBoxMenuItem _clientTiming_chk           = new JCheckBoxMenuItem("Time SQL Statement", DEFAULT_clientTiming);
	private JCheckBoxMenuItem _useSemicolonHack_chk       = new JCheckBoxMenuItem("Use Semicolon as Alternative SQL Send", DEFAULT_useSemicolonHack);
	private JCheckBoxMenuItem _enableDbmsOutput_chk       = new JCheckBoxMenuItem("Oracle/DB2 Enable DBMS Output", DEFAULT_enableDbmsOutput);
	private JCheckBoxMenuItem _appendResults_chk          = new JCheckBoxMenuItem("Append Results", DEFAULT_appendResults);
	private JCheckBoxMenuItem _rsTrimStrings_chk          = new JCheckBoxMenuItem("Trim String values", ResultSetTableModel.DEFAULT_StringTrim);
	private JCheckBoxMenuItem _rsShowRowNumber_chk        = new JCheckBoxMenuItem("Show Row Number", ResultSetTableModel.DEFAULT_ShowRowNumber);
	private boolean           _appendResults_scriptReader = false;
	private JCheckBoxMenuItem _getObjectTextOnError_chk   = new JCheckBoxMenuItem("Get Object Text on Error", DEFAULT_getObjectTextOnError);
	private JCheckBoxMenuItem _jdbcAutoCommit_chk         = new JCheckBoxMenuItem("JDBC AutoCommit", DEFAULT_jdbcAutoCommit);
	private JMenuItem         _sqlBatchTermDialog_mi      = new JMenuItem        ("Change SQL Batch Terminator");

	private JButton           _appOptions_but             = new JButton("Options");
	private JButton           _codeCompletionOpt_but      = new JButton(); 

	private JCheckBox         _splitPane_chk              = new JCheckBox();
	private JSplitPane        _splitPane                  = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

	private JPanel            _mainPane                   = new JPanel( new MigLayout("insets 0 0 0 0") );
	private JPanel            _controlPane                = new JPanel( new MigLayout("insets 0 0 0 0") );
//	private JPanel            _possibleCtrlPane           = new JPanel( new MigLayout("insets 0 0 0 0") );
	private JPanel            _possibleCtrlPane           = new JPanel( new BorderLayout() );
	private JPanel            _topPane                    = new JPanel( new BorderLayout() );
	private JPanel            _bottomPane                 = new JPanel( new MigLayout("insets 0 0 0 0") );



	private JSeparator        _cntrlPrefix_sep            = new JSeparator(SwingConstants.VERTICAL);
	private JComboBox         _dbnames_cbx                = new JComboBox();
	private JPanel            _resPanel                   = new JPanel( new MigLayout("insets 0 0 0 0, gap 0 0, wrap") );
	private JScrollPane       _resPanelScroll             = new JScrollPane(_resPanel);
	private RTextScrollPane   _resPanelTextScroll         = new RTextScrollPane();
//	private JLabel	          _msgline                    = new JLabel("");	     // For displaying messages
	private StatusBar         _statusBar                  = new StatusBar();
	private int               _lastTabIndex               = -1;
	private boolean           _closeConnOnExit            = true;
	private static Font       _aseMsgFont                 = null;
	private ArrayList<JComponent> _resultCompList         = null;
	
	private int         _srvVersion                       = 0;
	private long        _connectedAtTime                  = 0;
	private String      _connectedDriverName              = null;
	private String      _connectedDriverVersion           = null;
	private String      _connectedToProductName           = null;
	private String      _connectedToProductVersion        = null;
	private String      _connectedToServerName            = null;
	private String      _connectedToSysListeners          = null;
	private String      _connectedSrvCharset              = null;
	private String      _connectedSrvSortorder            = null;
	private String      _connectedAsUser                  = null;
	private String      _connectedWithUrl                 = null;

	private String      _connectedClientCharsetId         = null;
	private String      _connectedClientCharsetName       = null;
	private String      _connectedClientCharsetDesc       = null;
	
	private String      _currentDbName                    = null; // probably only maintained for ASE
	private boolean     _dbmsOutputIsEnabled              = false; // only maintained for Oracle & DB2

	/** if DB returns a error message, stop executions */
	private boolean     _abortOnDbMessages                = false;
	
	// Last File
	private LinkedList<String>_lastFileNameList           = new LinkedList<String>();
	private int         _lastFileNameSaveMax              = DEFAULT_lastFileNameSaveMax;

	// The base Window can be either a JFrame or a JDialog
	private Window      _window          = null;
	private JFrame      _jframe          = null;
	private JDialog     _jdialog         = null;
	private String      _titlePrefix     = null;
	private String      _titleSrvStr     = null;
	private WindowType  _windowType      = null;

	private JButton     _connect_but     = SwingUtils.makeToolbarButton(Version.class, "connect16.gif",    ACTION_CONNECT,    this, "Connect to a ASE",         "Connect");
	private JButton     _disconnect_but  = SwingUtils.makeToolbarButton(Version.class, "disconnect16.gif", ACTION_DISCONNECT, this, "Close the ASE Connection", "Disconnect");

	private JButton     _cmdSql_but      = null;
	private JButton     _cmdRcl_but      = null;
//	private JButton     _cmdSql_but      = SwingUtils.makeToolbarButton(Version.class, "command_sql.png",          ACTION_CMD_SQL,        this, "Execute some predefined SQL Statements",                          "SQL");
//	private JButton     _cmdRcl_but      = SwingUtils.makeToolbarButton(Version.class, "command_rcl.png",          ACTION_CMD_RCL,        this, "Execute some predefined RCL Statements",                          "RCL");
	private JButton     _rsWhoIsDown_but = SwingUtils.makeToolbarButton(Version.class, "rs_admin_who_is_down.png", ACTION_RS_WHO_IS_DOWN, this, "Execute Replication Server Command 'admin who_is_down' (Ctrl-w)", "who_is_down");
	private JButton     _viewLogFile_but = SwingUtils.makeToolbarButton(Version.class, "tail_logfile.png",         ACTION_VIEW_LOG_TAIL,  this, "Show Server logfile",                                    "logfile");

	private JLabel      _jvm_lbl         = new JLabel(LOCAL_JVM);
	private CommandHistoryDialog _cmdHistoryDialog = null;
	private String      _cmdHistoryFilename        = DEFAULT_historyFileName;

	private FavoriteCommandManagerSql _favoriteCmdManagerSql  = null;
	private String                    _favoriteCmdFilenameSql = DEFAULT_favoriteCmdFileNameSql;
	private FavoriteCommandManagerRcl _favoriteCmdManagerRcl  = null;
	private String                    _favoriteCmdFilenameRcl = DEFAULT_favoriteCmdFileNameRcl;

	// if we start from the CMD Line, add a few extra stuff
	//---------------------------------------
	private JMenuBar            _main_mb                = new JMenuBar();

	private JToolBar            _toolbar                = new JToolBar();

	// File
	private JMenu               _file_m                 = new JMenu("File");
	private JMenuItem           _connect_mi             = new JMenuItem("Connect...");
	private JMenuItem           _disconnect_mi          = new JMenuItem("Disconnect");
	private JMenuItem           _cloneConnect_mi        = new JMenuItem("New Window, Clone Connection");
	private JMenuItem           _fNew_mi                = new JMenuItem("New File");
	private JMenuItem           _fOpen_mi               = new JMenuItem("Open File...");
//	private JMenuItem           _fClose_mi              = new JMenuItem("Close");
	private JMenuItem           _fSave_mi               = new JMenuItem("Save");
	private JCheckBoxMenuItem   _fSaveBeforeExec_mi     = new JCheckBoxMenuItem("Save Before Execute");
	private JMenuItem           _fSaveAs_mi             = new JMenuItem("Save As...");
	private JMenu               _fHistory_m             = new JMenu("Last Used Files");
	private JMenuItem           _exit_mi                = new JMenuItem("Exit");

	// View
	private JMenu               _view_m                 = new JMenu("View");
	private JMenuItem           _viewCmdHistory_mi      = new JMenuItem("Command History");
	private JMenuItem           _viewLogFile_mi         = new JMenuItem("Tail on Server Log File");
	private JMenuItem           _ase_viewConfig_mi      = new JMenuItem("View ASE Configuration...");
	private JMenuItem           _rs_configChangedDdl_mi = new JMenuItem("View RCL for changed configurations...");
	private JMenuItem           _rs_configAllDdl_mi     = new JMenuItem("View RCL for ALL configurations...");
	private JMenuItem           _rs_dumpQueue_mi        = new JMenuItem("View Stable Queue Content...");
	private JMenuItem           _rsWhoIsDown_mi         = new JMenuItem("Admin who_is_down");

	private JMenu               _preferences_m              = new JMenu("Preferences");
	private JCheckBoxMenuItem   _prefWinOnConnect_mi        = new JCheckBoxMenuItem("Restore Window Position, based on Connection", DEFAULT_restoreWinSizeForConn);
	private JCheckBoxMenuItem   _prefShowAppNameInTitle_mi  = new JCheckBoxMenuItem("Show '"+APP_NAME+"' as Prefix in Window Title", DEFAULT_showAppNameInTitle);
	private JCheckBoxMenuItem   _prefSplitHorizontal_mi     = new JCheckBoxMenuItem("Editor and Output Windows side-by-side", DEFAULT_horizontalOrientation);
	private JCheckBoxMenuItem   _prefPlaceCntrlInToolbar_mi = new JCheckBoxMenuItem("Place 'Execute' and other control buttons in the Toolbar", DEFAULT_commandPanelInToolbar);
	//---------------------------------------

	// Tools
	private JMenu               _tools_m                = new JMenu("Tools");
	private JMenuItem           _toolDummy_mi           = new JMenuItem("Dummy entry");
	private JMenuItem           _aseCaptureSql_mi       = new JMenuItem("Capture SQL...");

	// Start time
	private static long _startTime = System.currentTimeMillis();
	public static long getStartTime()
	{
		return _startTime;
	}
	
	/**
	 * Constructor for CommandLine parameters
	 * @param cmd
	 * @throws Exception
	 */
	public QueryWindow(final CommandLine cmd)
	throws Exception
	{
		Version.setAppName(APP_NAME);
		
		// Create store dir if it did not exists.
		File appStoreDir = new File(Version.APP_STORE_DIR);
		if ( ! appStoreDir.exists() )
		{
			if (appStoreDir.mkdir())
				System.out.println("Creating directory '"+appStoreDir+"' to hold various files for "+Version.getAppName());
		}

		// -----------------------------------------------------------------
		// CHECK/SETUP information from the CommandLine switches
		// -----------------------------------------------------------------
		final String CONFIG_FILE_NAME      = System.getProperty("CONFIG_FILE_NAME",      "asetune.properties");
		final String USER_CONFIG_FILE_NAME = System.getProperty("USER_CONFIG_FILE_NAME", "asetune.user.properties");
		final String TMP_CONFIG_FILE_NAME  = System.getProperty("TMP_CONFIG_FILE_NAME",  "sqlw.save.properties");
		final String SQLW_HOME             = System.getProperty("SQLW_HOME");
		
		String defaultPropsFile     = (SQLW_HOME             != null) ? SQLW_HOME             + File.separator + CONFIG_FILE_NAME      : CONFIG_FILE_NAME;
		String defaultUserPropsFile = (Version.APP_STORE_DIR != null) ? Version.APP_STORE_DIR + File.separator + USER_CONFIG_FILE_NAME : USER_CONFIG_FILE_NAME;
		String defaultTmpPropsFile  = (Version.APP_STORE_DIR != null) ? Version.APP_STORE_DIR + File.separator + TMP_CONFIG_FILE_NAME  : TMP_CONFIG_FILE_NAME;
		String defaultTailPropsFile = LogTailWindow.getDefaultPropFile();

		// Compose MAIN CONFIG file (first USER_HOME then ASETUNE_HOME)
		String filename = Version.APP_STORE_DIR + File.separator + CONFIG_FILE_NAME;
		if ( (new File(filename)).exists() )
			defaultPropsFile = filename;

		String propFile        = cmd.getOptionValue("config",     defaultPropsFile);
		String userPropFile    = cmd.getOptionValue("userConfig", defaultUserPropsFile);
		String tmpPropFile     = cmd.getOptionValue("tmpConfig",  defaultTmpPropsFile);
		String tailPropFile    = cmd.getOptionValue("tailConfig", defaultTailPropsFile);

		// Check if the configuration file exists
		if ( ! (new File(propFile)).exists() )
			throw new FileNotFoundException("The configuration file '"+propFile+"' doesn't exists.");

		// -----------------------------------------------------------------
		// CHECK JAVA JVM VERSION
		// -----------------------------------------------------------------
		int javaVersionInt = JavaVersion.getVersion();
		if (   javaVersionInt != JavaVersion.VERSION_NOTFOUND 
		    && javaVersionInt <  JavaVersion.VERSION_1_6
		   )
		{
			System.out.println("");
			System.out.println("===============================================================");
			System.out.println(" "+Version.getAppName()+" needs a runtime JVM 1.6 or higher.");
			System.out.println(" java.version = " + System.getProperty("java.version"));
			System.out.println(" which is parsed into the number: " + JavaVersion.getVersion());
			System.out.println("---------------------------------------------------------------");
			System.out.println("");
			throw new Exception(Version.getAppName()+" needs a runtime JVM 1.6 or higher.");
		}

		// The SAVE Properties for shared Tail
		Configuration tailSaveProps = new Configuration(tailPropFile);
		Configuration.setInstance(Configuration.TAIL_TEMP, tailSaveProps);

		// The SAVE Properties...
		Configuration appSaveProps = new Configuration(tmpPropFile);
		Configuration.setInstance(Configuration.USER_TEMP, appSaveProps);

		// Get the USER properties that could override CONF
		Configuration appUserProps = new Configuration(userPropFile);
		Configuration.setInstance(Configuration.USER_CONF, appUserProps);

		// Get the "OTHER" properties that has to do with LOGGING etc...
		Configuration appProps = new Configuration(propFile);
		Configuration.setInstance(Configuration.SYSTEM_CONF, appProps);

		// Set the Configuration search order when using the: Configuration.getCombinedConfiguration()
		Configuration.setSearchOrder(
			Configuration.TAIL_TEMP,    // First
			Configuration.USER_TEMP,    // Second
			Configuration.USER_CONF,    // Third
			Configuration.SYSTEM_CONF); // Forth

		//---------------------------------------------------------------
		// OK, lets get ASE user/passwd/server/dbname
		//---------------------------------------------------------------
		String aseUsername  = System.getProperty("user.name"); 
		String asePassword  = null;
		String aseServer    = System.getenv("DSQUERY");
		String aseDbname    = "";
		String jdbcUsername = System.getProperty("user.name");
		String jdbcPassword = null;
		String jdbcUrl      = "";
		String jdbcDriver   = "";
		String connProfile  = "";
		String sqlQuery     = "";
		String sqlFile      = "";
		if (cmd.hasOption('U'))	aseUsername  = cmd.getOptionValue('U');
		if (cmd.hasOption('P'))	asePassword  = cmd.getOptionValue('P');
		if (cmd.hasOption('S'))	aseServer    = cmd.getOptionValue('S');
		if (cmd.hasOption('D'))	aseDbname    = cmd.getOptionValue('D');
		if (cmd.hasOption('q'))	sqlQuery     = cmd.getOptionValue('q');
		if (cmd.hasOption('U'))	jdbcUsername = cmd.getOptionValue('U');
		if (cmd.hasOption('P'))	jdbcPassword = cmd.getOptionValue('P');
		if (cmd.hasOption('u'))	jdbcUrl      = cmd.getOptionValue('u');
		if (cmd.hasOption('d'))	jdbcDriver   = cmd.getOptionValue('d');
		if (cmd.hasOption('p'))	connProfile  = cmd.getOptionValue('p');
		if (cmd.hasOption('i'))	sqlFile      = cmd.getOptionValue('i');

		if (aseServer == null)
			aseServer = "SYBASE";

		DebugOptions.init();
		if (cmd.hasOption('x'))
		{
			String cmdLineDebug = cmd.getOptionValue('x');
			String[] sa = cmdLineDebug.split(",");
			for (int i=0; i<sa.length; i++)
			{
				String str = sa[i].trim();

				if (str.equalsIgnoreCase("list"))
				{
					System.out.println();
					System.out.println(" Option          Description");
					System.out.println(" --------------- -------------------------------------------------------------");
					for (Map.Entry<String,String> entry : Debug.getKnownDebugs().entrySet()) 
					{
						String debugOption = entry.getKey();
						String description = entry.getValue();

						System.out.println(" "+StringUtil.left(debugOption, 15, true) + " " + description);
					}
					System.out.println();
					// Get of of here if it was a list option
					throw new NormalExitException("List of debug options");
				}
				else
				{
					// add debug option
					Debug.addDebug(str);
				}
			}
		}

//		System.setProperty("Logging.print.noDefaultLoggerMessage", "false");
		Logging.init("sqlw.", propFile);
		
    	try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Do a dummy encryption, this will hopefully speedup, so that the connection dialog wont hang for a long time during initialization
		long initStartTime=System.currentTimeMillis();
		Encrypter propEncrypter = new Encrypter("someDummyStringToInitialize");
		String encrypedValue = propEncrypter.encrypt("TheDummyValueToEncrypt... this is just a dummy string...");
		propEncrypter.decrypt(encrypedValue); // Don't care about the result...
		_logger.info("Initializing 'encrypt/decrypt' package took: " + (System.currentTimeMillis() - initStartTime) + " ms.");

		String hostPortStr = "";
		if (aseServer.indexOf(":") == -1)
			hostPortStr = AseConnectionFactory.getIHostPortStr(aseServer);
		else
			hostPortStr = aseServer;

		// use IGNORE_DONE_IN_PROC=true, if not set in the options in the connection dialog
		AseConnectionFactory.setPropertiesForAppname(APP_NAME, "IGNORE_DONE_IN_PROC", "true");
		
//		// Try make an initial connection...
//		Connection conn = null;
//		if ( ! StringUtil.isNullOrBlank(aseUsername) )
//		{
//			// If -P was not passed: try to get any saved password 
//			if (!cmd.hasOption('P') && asePassword == null)
//				asePassword = ConnectionDialog.getPasswordForServer(hostPortStr);
//
//			_logger.info("Connecting as user '"+aseUsername+"' to server='"+aseServer+"'. Which is located on '"+hostPortStr+"'.");
//			try
//			{
//				Properties props = new Properties();
//	//			props.put("CHARSET", "iso_1");
//				conn = AseConnectionFactory.getConnection(hostPortStr, aseDbname, aseUsername, asePassword, APP_NAME, null, props, null);
//	
//				// Set the correct dbname, if it hasnt already been done
//				AseConnectionUtils.useDbname(conn, aseDbname);
//			}
//			catch (SQLException e)
//			{
//				_logger.error("Problems connecting: " + AseConnectionUtils.sqlExceptionToString(e));
//	//			throw e;
//			}
//		}
//
//		// Create a QueryWindow component that uses the factory object.
//		QueryWindow qw = new QueryWindow(conn, sqlQuery, sqlFile, true, WindowType.CMDLINE_JFRAME, null);
//		qw.openTheWindow();
////		init(conn, sqlQuery, sqlFile, true, WindowType.CMDLINE_JFRAME, null);
////		openTheWindow();
		// Try make an initial connection...

		// Create a QueryWindow component that uses the factory object.
		final QueryWindow qw = new QueryWindow(null, sqlQuery, sqlFile, true, WindowType.CMDLINE_JFRAME, null);

		// if command line parameters, try to connect (using all that pre-built logic) 
		// But defer the action so that the wind has time to open.
		if ( cmd.hasOption('U') )
		{
			// If -P was not passed: try to get any saved password 
			if (!cmd.hasOption('P') && asePassword == null)
				asePassword = ConnectionDialog.getPasswordForServer(hostPortStr);

			// FIXME: we should really try to fill in a ConnectionProfile instead of doing it the below way...
			String tmpStr = "";
			if (cmd.hasOption('U'))	tmpStr += ",aseUsername="  + aseUsername;
			if (cmd.hasOption('P'))	tmpStr += ",asePassword="  + asePassword;
			if (cmd.hasOption('S'))	tmpStr += ",aseServer="    + aseServer;
			if (cmd.hasOption('D'))	tmpStr += ",aseDbname="    + aseDbname;
			if (cmd.hasOption('U'))	tmpStr += ",jdbcUsername=" + jdbcUsername;
			if (cmd.hasOption('P'))	tmpStr += ",jdbcPassword=" + jdbcPassword;
			if (cmd.hasOption('u'))	tmpStr += ",jdbcUrl="      + jdbcUrl;
			if (cmd.hasOption('d'))	tmpStr += ",jdbcDriver="   + jdbcDriver;
			if (cmd.hasOption('p'))	tmpStr += ",connProfile="  + connProfile;
			// if we got URL but no driver, lets guess the driver, based on the URL
			if (cmd.hasOption('u') && !cmd.hasOption('d'))
				tmpStr += ",jdbcDriver="   + JdbcDriverHelper.guessDriverForUrl(jdbcUrl);
			// remove first comma
			tmpStr = tmpStr.substring(1);

			final String ppeStr = ConnectionDialog.CONF_OPTION_CONNECT_ON_STARTUP + "={" + tmpStr + "}"; 

			Runnable deferedAction = new Runnable()
			{
				@Override
				public void run()
				{
					qw.action_connect(new ActionEvent(cmd, 1, ppeStr));
				}
			};
			SwingUtilities.invokeLater(deferedAction);
		}

		// Install shutdown hook: used to send Usage Information
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			@Override
			public void run()
			{
				_logger.debug("----Start Shutdown Hook");
				sendExecStatistics(true);
				_logger.debug("----End Shutdown Hook");
			}
		});

		// Install our own EventQueue handler, to handle/log strange exceptions in the event dispatch thread
		EventQueueProxy.install();

		// Now open the window
		qw.openTheWindow();
	}

	/**
	 * This constructor method creates a simple GUI and hooks up an event
	 * listener that updates the table when the user enters a new query.
	 **/
	public QueryWindow(Connection conn, WindowType winType)
	{
		this(conn, null, null, true, winType, null);
	}
	public QueryWindow(Connection conn, boolean closeConnOnExit, WindowType winType)
	{
		this(conn, null, null, closeConnOnExit, winType, null);
	}
	public QueryWindow(Connection conn, String sql, WindowType winType)
	{
		this(conn, sql, null, true, winType, null);
	}
	public QueryWindow(Connection conn, String sql, String inputFile, boolean closeConnOnExit, WindowType winType, Configuration conf)
	{
		init(conn, sql, inputFile, closeConnOnExit, winType, conf);
	}

	private void init(Connection conn, String sql, String inputFile, boolean closeConnOnExit, WindowType winType, Configuration conf)
	{
		_windowType = winType;

		if (winType == WindowType.CMDLINE_JFRAME)
		{
			//_titlePrefix = Version.getAppName()+" Query Window";
			_titlePrefix = Version.getAppName();
			_jframe  = new JFrame(_titlePrefix);
			_jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			_window  = _jframe;
		}
		if (winType == WindowType.JFRAME)
		{
			_titlePrefix = Version.getAppName()+" Query";
			_jframe  = new JFrame(_titlePrefix);
			_window  = _jframe;
		}
		if (winType == WindowType.JDIALOG)
		{
			_titlePrefix = Version.getAppName()+" Query";
			_jdialog = new JDialog((Dialog)null, _titlePrefix);
			_window  = _jdialog;
		}
		if (winType == WindowType.JDIALOG_MODAL)
		{
			_titlePrefix = Version.getAppName()+" Query";
			_jdialog = new JDialog((Dialog)null, _titlePrefix, true);
			_window  = _jdialog;
		}
		if (_window == null)
			throw new RuntimeException("_window is null, this should never happen.");

		// Create some buttons
		_cmdSql_but = createSqlCommandsButton(null, 0);
		_cmdRcl_but = createRclCommandsButton(null, 0);

		//--------------------------
		// MENU - composition
//		if (_jframe != null)
		if (winType == WindowType.CMDLINE_JFRAME)
		{
			_jframe.setJMenuBar(_main_mb);
	
			_main_mb.add(_file_m);
			_main_mb.add(_view_m);
			_main_mb.add(_tools_m);

			// FILE
			_file_m.add(_connect_mi);
			_file_m.add(_disconnect_mi);
			_file_m.add(_cloneConnect_mi);
			_file_m.addSeparator();
			_file_m.add(_fNew_mi);
			_file_m.add(_fOpen_mi);
			_file_m.add(_fHistory_m);
//			_file_m.add(_fClose_mi);
//			_file_m.addSeparator();
			_file_m.add(_fSave_mi);
			_file_m.add(_fSaveBeforeExec_mi);
			_file_m.add(_fSaveAs_mi);
			_file_m.addSeparator();
			_file_m.add(_exit_mi);
	
			_file_m .setMnemonic(KeyEvent.VK_F);
	
			// VIEW
			_view_m.add(_viewCmdHistory_mi);
			_view_m.add(_viewLogFile_mi);
			_view_m.add(_ase_viewConfig_mi);
			_view_m.add(_rs_configChangedDdl_mi);   
			_view_m.add(_rs_configAllDdl_mi);   
			_view_m.add(_rs_dumpQueue_mi);
			_view_m.add(_rsWhoIsDown_mi);

			_view_m.add(_preferences_m);
			_preferences_m.add(_prefWinOnConnect_mi);
			_preferences_m.add(_prefShowAppNameInTitle_mi);
			_preferences_m.add(_prefSplitHorizontal_mi);
			_preferences_m.add(_prefPlaceCntrlInToolbar_mi);
			
			_prefShowAppNameInTitle_mi.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					saveProps();
				}
			});
			_prefSplitHorizontal_mi.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					action_splitPaneToggle(e);
//					saveProps();
//					reLayoutSomeStuff(_prefSplitHorizontal_mi.isSelected(), _prefPlaceCntrlInToolbar_mi.isSelected());
//					//SwingUtils.showInfoMessage(_window, "Restart is needed to take effect",	"<html>Please <b>restart</b> the application<br>It's needed for the change to take <b>FULL</b> effect.<br>Sorry for that...</html>");
				}
			});
			_prefPlaceCntrlInToolbar_mi.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					saveProps();
					reLayoutSomeStuff(_prefSplitHorizontal_mi.isSelected(), _prefPlaceCntrlInToolbar_mi.isSelected());
					//SwingUtils.showInfoMessage(_window, "Restart is needed to take effect",	"<html>Please <b>restart</b> the application<br>It's needed for the change to take <b>FULL</b> effect.<br>Sorry for that...</html>");
				}
			});

			
			_ase_viewConfig_mi     .setVisible(false);
			_rs_configChangedDdl_mi.setVisible(false);
			_rs_configAllDdl_mi    .setVisible(false);
			_rs_dumpQueue_mi       .setVisible(false);
			_rsWhoIsDown_mi        .setVisible(false);
	
			// TOOLS
			_tools_m.add(_toolDummy_mi);
			_tools_m.add(_aseCaptureSql_mi);

			_toolDummy_mi    .setVisible(false);
			_aseCaptureSql_mi.setVisible(false);

			
			_file_m .setMnemonic(KeyEvent.VK_T);

			//			_connect_mi        .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK));
//			_disconnect_mi     .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.ALT_MASK));
			_connect_mi        .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));
			_disconnect_mi     .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));
			_cloneConnect_mi   .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));

			_fNew_mi           .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			_fOpen_mi          .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			_fSave_mi          .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
//			_fSaveAs_mi        .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));
// reserve Ctrl+Shift+s for when we have "multiple editor tabs"
//			_fSaveAll_mi       .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));
			

			_rsWhoIsDown_mi    .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

			_viewCmdHistory_mi .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

			// TOOLBAR
//			_connect_but    = SwingUtils.makeToolbarButton(Version.class, "connect16.gif",    ACTION_CONNECT,    this, "Connect to a ASE",         "Connect");
//			_disConnect_but = SwingUtils.makeToolbarButton(Version.class, "disconnect16.gif", ACTION_DISCONNECT, this, "Close the ASE Connection", "Disconnect");

			_splitPane_chk.setActionCommand(ACTION_SPLITPANE_TOGGLE);
			_splitPane_chk.addActionListener(this);

			_splitPane_chk .setIcon(        SwingUtils.readImageIcon(Version.class, "images/app_split_vertical.png"));
			_splitPane_chk .setSelectedIcon(SwingUtils.readImageIcon(Version.class, "images/app_split_horizontal.png"));
			_splitPane_chk .setToolTipText("<html>Query Window and Output Window layout mode" +
				"<ul>" +
				"   <li>Top to Bottom - <i>default</i></li>" +
				"   <li>Side by Side</li>" +
				"</ul>" +
				"</html>");
			
			_toolbar.setLayout(new MigLayout("insets 0 0 0 3", "", "")); // insets Top Left Bottom Right
			_toolbar.add(_connect_but);
			_toolbar.add(_disconnect_but);
			_toolbar.add(_splitPane_chk);
			_toolbar.add(new JSeparator(SwingConstants.VERTICAL), "grow");
			_toolbar.add(_viewLogFile_but);
			_toolbar.add(_cmdSql_but,      "hidemode 3");
			_toolbar.add(_cmdRcl_but,      "hidemode 3");
			_toolbar.add(_rsWhoIsDown_but, "hidemode 3");
//			_toolbar.add(new JLabel(),     "pushx, growx"); // Dummy entry to "grow" so next will be located to the right
			_toolbar.add(_possibleCtrlPane, "pushx, growx"); // Dummy entry to "grow" so next will be located to the right
			_toolbar.add(_jvm_lbl,         "");

			// Visibility for TOOLBAR components at startup
			_viewLogFile_but.setEnabled(false);
			_cmdSql_but     .setVisible(false);
			_cmdRcl_but     .setVisible(false);
			_rsWhoIsDown_but.setVisible(false);

			_jvm_lbl.setToolTipText("<html>This is <i>PID@hostname</i>, where this java process is running.</html>");

			//--------------------------
			// MENU - Icons
			_connect_mi            .setIcon(SwingUtils.readImageIcon(Version.class, "images/connect16.gif"));
			_disconnect_mi         .setIcon(SwingUtils.readImageIcon(Version.class, "images/disconnect16.gif"));
			_cloneConnect_mi       .setIcon(SwingUtils.readImageIcon(Version.class, "images/clone_connect_16.png"));
			_exit_mi               .setIcon(SwingUtils.readImageIcon(Version.class, "images/close.gif"));
			_viewLogFile_mi        .setIcon(SwingUtils.readImageIcon(Version.class, "images/tail_logfile.png"));
			_viewCmdHistory_mi     .setIcon(SwingUtils.readImageIcon(Version.class, "images/command_history.png"));
			_ase_viewConfig_mi     .setIcon(SwingUtils.readImageIcon(Version.class, "images/config_ase_view.png"));
			_rs_configAllDdl_mi    .setIcon(SwingUtils.readImageIcon(Version.class, "images/repserver_config.png"));
			_rs_configChangedDdl_mi.setIcon(SwingUtils.readImageIcon(Version.class, "images/repserver_config.png"));
			_rs_dumpQueue_mi       .setIcon(SwingUtils.readImageIcon(Version.class, "images/view_rs_queue.png"));
			_rsWhoIsDown_mi        .setIcon(SwingUtils.readImageIcon(Version.class, "images/rs_admin_who_is_down.png"));
			_aseCaptureSql_mi      .setIcon(SwingUtils.readImageIcon(Version.class, "images/capture_sql_tool.gif"));

			//--------------------------
			// MENU - Actions
			_connect_mi     .setActionCommand(ACTION_CONNECT);
			_disconnect_mi  .setActionCommand(ACTION_DISCONNECT);
			_cloneConnect_mi.setActionCommand(ACTION_CLONE_CONNECT);
			_fNew_mi        .setActionCommand(ACTION_FILE_NEW);
			_fOpen_mi       .setActionCommand(ACTION_FILE_OPEN);
//			_fClose_mi      .setActionCommand(ACTION_FILE_CLOSE);
			_fSave_mi       .setActionCommand(ACTION_FILE_SAVE);
			_fSaveAs_mi     .setActionCommand(ACTION_FILE_SAVE_AS);
			_exit_mi        .setActionCommand(ACTION_EXIT);

			_viewCmdHistory_mi     .setActionCommand(ACTION_VIEW_CMD_HISTORY);
			_viewLogFile_mi        .setActionCommand(ACTION_VIEW_LOG_TAIL);
			_ase_viewConfig_mi     .setActionCommand(ACTION_VIEW_ASE_CONFIG);
			_rs_configChangedDdl_mi.setActionCommand(ACTION_RS_GENERATE_CHANGED_DDL);
			_rs_configAllDdl_mi    .setActionCommand(ACTION_RS_GENERATE_ALL_DDL);
			_rs_dumpQueue_mi       .setActionCommand(ACTION_RS_DUMP_QUEUE);
			_rsWhoIsDown_mi        .setActionCommand(ACTION_RS_WHO_IS_DOWN);
			_aseCaptureSql_mi      .setActionCommand(ACTION_ASE_CAPTURE_SQL);

			//--------------------------
			// And the action listener
			_connect_mi     .addActionListener(this);
			_disconnect_mi  .addActionListener(this);
			_cloneConnect_mi.addActionListener(this);
			_fNew_mi        .addActionListener(this);
			_fOpen_mi       .addActionListener(this);
//			_fClose_mi      .addActionListener(this);
			_fSave_mi       .addActionListener(this);
			_fSaveAs_mi     .addActionListener(this);
			_exit_mi        .addActionListener(this);

			_viewCmdHistory_mi     .addActionListener(this);
			_viewLogFile_mi        .addActionListener(this);
			_ase_viewConfig_mi     .addActionListener(this);
			_rs_configChangedDdl_mi.addActionListener(this);
			_rs_configAllDdl_mi    .addActionListener(this);
			_rs_dumpQueue_mi       .addActionListener(this);
			_rsWhoIsDown_mi        .addActionListener(this);
			_aseCaptureSql_mi      .addActionListener(this);
		}

//		final JPopupMenu fileHistoryPopupMenu = new JPopupMenu();
//		_fHistory_m.setComponentPopupMenu(fileHistoryPopupMenu);
//		fileHistoryPopupMenu.add(new JMenuItem("No file has been used yet"));
//		fileHistoryPopupMenu.addPopupMenuListener(new PopupMenuListener()
//		{
//			@Override
//			public void popupMenuWillBecomeVisible(PopupMenuEvent e)
//			{
//System.out.println("_fHistory_m:popupMenuWillBecomeVisible()");
//				// remove all old items (if any)
//				fileHistoryPopupMenu.removeAll();
//
//				// Now create menu items
//				for (String name : _lastFileNameList)
//				{
//					JMenuItem mi = new JMenuItem();
//					mi.setText(name);
//					mi.addActionListener(new ActionListener()
//					{
//						@Override
//						public void actionPerformed(ActionEvent e)
//						{
//							Object o = e.getSource();
//							if (o instanceof JMenuItem)
//							{
//								JMenuItem mi = (JMenuItem) o;
//								String filename = mi.getText();
//								action_fileOpen(null, filename);
//							}
//						}
//					});
//					fileHistoryPopupMenu.add(mi);
//				}
//			}
//			@Override
//			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {/*empty*/}
//			@Override
//			public void popupMenuCanceled(PopupMenuEvent e)	{/*empty*/}
//		});

		//super();
		//super.setTitle(Version.getAppName()+" Query"); // Set window title
//		ImageIcon icon = new ImageIcon(getClass().getResource("swing/images/query16.gif"));
		ImageIcon icon16 = SwingUtils.readImageIcon(Version.class, "images/sql_query_window.png");
		ImageIcon icon32 = SwingUtils.readImageIcon(Version.class, "images/sql_query_window_32.png");
//		super.setIconImage(icon.getImage()); // works if we are a JFrame
//		((Frame)this.getOwner()).setIconImage(icon.getImage()); // works if we are a JDialog

//		_window.setIconImage(icon16.getImage());
//		_window.setIconImage(icon32.getImage());
		ArrayList<Image> iconList = new ArrayList<Image>();
		if (icon16 != null) iconList.add(icon16.getImage());
		if (icon32 != null) iconList.add(icon32.getImage());
		_window.setIconImages(iconList);

		_closeConnOnExit = closeConnOnExit;

		// Arrange to quit the program when the user closes the window
		_window.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				saveProps();
				saveWinProps();
				sendExecStatistics(false);

				if (_closeConnOnExit)
					close();

				File f = new File(_query_txt.getFileFullPath());
				if (f.exists() && _query_txt.isDirty())
				{
					Object[] buttons = {"Save File", "Discard changes"};
					int answer = JOptionPane.showOptionDialog(_window, 
							"The File '"+f+"' has not been saved.",
							"Save file?", 
							JOptionPane.DEFAULT_OPTION,
							JOptionPane.QUESTION_MESSAGE,
							null,
							buttons,
							buttons[0]);
					// Save
					if (answer == 0) 
					{
						try
						{
							_query_txt.save();
							_statusBar.setFilename(_query_txt.getFileFullPath(), _query_txt.getEncoding());
							_statusBar.setFilenameDirty(_query_txt.isDirty());

							// No need since we are shuting down
							if (_watchdogIsFileChanged != null)
								_watchdogIsFileChanged.setFile(_query_txt.getFileFullPath());
						}
						catch (IOException ex)
						{
							SwingUtils.showErrorMessage("Problems Saving file", "Problems saving file", ex);
						}
					}
				}

				// This will stop the thread (in a couple of seconds)
				if (_watchdogIsFileChanged != null)
					_watchdogIsFileChanged.shutdown();
			}
			
			@Override
			public void windowActivated(WindowEvent e)
			{
				super.windowActivated(e);
				
//				checkIfCurrentFileIsUpdated();

				// If the window is ACTIVE check for file changes
				if (_watchdogIsFileChanged != null)
					_watchdogIsFileChanged.setPaused(false);
			}
			@Override
			public void windowDeactivated(WindowEvent e)
			{
				super.windowDeactivated(e);

				// If the window is NOT active we don't need to check for file changes
				if (_watchdogIsFileChanged != null)
					_watchdogIsFileChanged.setPaused(true);
			}
		});

		if (AseConnectionUtils.isConnectionOk(conn, false, null))
		{
			// Remember the factory object that was passed to us
			_conn = conn;

			if (_conn instanceof SybConnection)
			{
				// Setup a message handler
		//		((SybConnection)_conn).setSybMessageHandler(this);
				_srvVersion = AseConnectionUtils.getAseVersionNumber(conn);
				
				_connType = ConnectionDialog.TDS_CONN;
			}
			else
			{
				_connType = ConnectionDialog.OFFLINE_CONN;
//				_connType = ConnectionDialog.JDBC_CONN;
			}
		}

		// Set icons for some buttons
		_exec_but           .setIcon(SwingUtils.readImageIcon(Version.class, "images/exec.png"));
		_execGuiShowplan_but.setIcon(SwingUtils.readImageIcon(Version.class, "images/exec_with_plan.png"));
		_exec_but           .setText(null);
		_execGuiShowplan_but.setText(null);
		
		_prevErr_but.setIcon(SwingUtils.readImageIcon(Version.class, "images/prev_error.png"));
		_prevErr_but.setText(null);
		_prevErr_but.setContentAreaFilled(false);
		_prevErr_but.setMargin( new Insets(0,0,0,0) );
		_prevErr_but.setToolTipText("<html>Goto <b>previous</b> error message in the SQL Text.</html>");
		_prevErr_but.setVisible(false);
		_prevErr_but.addActionListener(this);
		_prevErr_but.setActionCommand(ACTION_PREV_ERROR);;

		_nextErr_but.setIcon(SwingUtils.readImageIcon(Version.class, "images/next_error.png"));
		_nextErr_but.setText(null);
		_nextErr_but.setContentAreaFilled(false);
		_nextErr_but.setMargin( new Insets(0,0,0,0) );
		_nextErr_but.setToolTipText("<html>Goto <b>next</b>     error message in the SQL Text.</html>");
		_nextErr_but.setVisible(false);
		_nextErr_but.addActionListener(this);
		_nextErr_but.setActionCommand(ACTION_NEXT_ERROR);;

		
		// Set various components
		_exec_but.setToolTipText(
			"<html>" +
			"<b>Execute</b><br>" +
			"<br>" +
			"Executes the <b>select</b> SQL Statement(s) above.<br>" +
			"If no text is selected, everything in the window will be executed.<br>" +
			"<br>" +
			"<b>Keybord shortcuts</b>: Ctrl-e, Alt+e, F5, F9<br>" +
			"<b>Tip 1</b>: You can <b><i>drop</i></b> any text on this button using <b><i>Drag & Drop</i></b>, example from any history window (Ctrl-h)...<br>" +
			"<b>Tip 2</b>: If you don't want to execute everything in the window by accident. Add the following as the first line:<br>" +
			"<code>print 'Execute the whole thing... Well I didnt think so...'</code><br>" +
			"<code>exit</code><br>" +
			"</html>"); 
		_exec_but.setMnemonic('e');

		_execGuiShowplan_but.setToolTipText(
			"<html>" +
			"<b>Execute with GUI Showplan</b><br>" +
			"<br>" +
			"Executes the <b>select</b> SQL Statement(s) above, but use the GUI Showplan. <i>only in ASE 15.0 or above</i><br>" +
			"If no text is selected, everything in the window will be executed.<br>" +
			"<br>" +
			"<b>Keybord shortcuts</b>: Ctrl-Shift+e, Alt+Shift+e, Shift+F5, Shift+F9<br>" +
			"<b>Tip 1</b>: You can <b><i>drop</i></b> any text on this button using <b><i>Drag & Drop</i></b>, example from any history window (Ctrl-h)...<br>" +
			"<b>Tip 2</b>: If you don't want to execute everything in the window by accident. Add the following as the first line:<br>" +
			"<code>print 'Execute the whole thing... Well I didnt think so...'</code><br>" +
			"<code>exit</code><br>" +
			"</html>"); 
		_execGuiShowplan_but.setMnemonic('E');

		_fSaveBeforeExec_mi.setToolTipText(
			"<html>" +
			"Save current working file before sending SQL to server<br>" +
			"<b>Note</b>: If no file is assigned (only temporary text), then do <b>not</b> save.<br>" +
			"<b>Usage</b>: For example if you share a file between two or more "+Version.getAppName()+" instances, then you want the other instance to reload any changes you make. This so you don't have conflicting edits in the different editors.<br>" +
			"</html>");
		
		_prefWinOnConnect_mi.setToolTipText(
			"<html>" +
			"Restore Window size and position, based on the connected server name.<br>" +
			"This simply means, when a new connection is made, the last known Window Properties will be restored for that connection name.<br>" +
			"</html>");

		_prefShowAppNameInTitle_mi.setToolTipText(
				"<html>" +
				"Show Application Name as a prefix in the Window Title bar.<br>" +
				"If this is disabled, the only the connected server name will be in the title.<br>" +
				"</html>");

		_prefPlaceCntrlInToolbar_mi.setToolTipText(
				"<html>" +
				"The 'Execute' the other buttons in that Panel can be embedded in the Toolbar at the top.<br>" +
				"Note: restart is required.<br>" +
				"</html>");

		_prefSplitHorizontal_mi.setToolTipText(
				"<html>" +
				"Have the Command Input Window and the Output Window <i>side by side</i>.<br>" +
				"Note: restart is required.<br>" +
				"</html>");

		try {_appOptions_but = createAppOptionButton(null);}
		catch (Throwable ex) {_logger.error("Problems creating the 'Application Options' button.",ex);}

		try {_setAseOptions_but = createSetAseOptionButton(null, _srvVersion);}
		catch (Throwable ex) {_logger.error("Problems creating the 'set options' button.",ex);}

		try {_codeCompletionOpt_but = createCodeCompletionOptionButton(null);}
		catch (Throwable ex) {_logger.error("Problems creating the 'Code Completion Options' button.",ex);}

		try {_copy_but = createCopyResultsButton(null);}
		catch (Throwable ex) {_logger.error("Problems creating the 'Copy results' button.",ex);}

		_dbnames_cbx     .setToolTipText("<html>Change database context.</html>");
		_rsInTabs_chk    .setToolTipText("<html>Check this if you want to have multiple result sets in individual tabs.</html>");
		_asPlainText_chk .setToolTipText("<html>No fancy output, just write the output as 'plan text'.</html>");
		_showRowCount_chk.setToolTipText("<html>" +
		                             "Show Row Count after each executes SQL Statement<br>" +
		                             "<br>" +
		                             "One problem with this is if/when you execute a Stored Procedure.<br>" +
		                             "A stored procedure normally has <b>many</b> upd/ins/del and selects.<br>" +
		                             "In those cases <b>several</b> Row Count will be displayed. (many more that <code>isql</code> will show you)<br>" +
		                             "This is just how the jConnect JDBC driver works.<br>" +
		                             "<br>" +
		                             "That's why this option is visible, so you can turn this on/off as you which!" +
		                             "</html>");
		_limitRsRowsRead_chk.setToolTipText(
				"<html>" +
				"<b>Stop</b> reading the ResultSet after # rows.<br>" +
				"To change the number of rows to stop after, choose the next menu item (dialog)<br>" +
				"<br>" +
				"This could also be accomplished with the 'go top 500' command.<br>" +
				"which would limit number or records read for the current SQL Batch.<br>" +
				"Example:<br>" +
				"<code>select * from someTable</code><br>" +
				"<code>go top 500</code><br>" +
				"</html>");
		_limitRsRowsReadDialog_mi.setToolTipText(
				"<html>" +
				"Open a dialog where you can change how many rows we should stop after." +
				"</html>");
		_showSentSql_chk.setToolTipText("<html>Include the sent/executed SQL Statement in the output.<br></html>");
		_printRsInfo_chk.setToolTipText("<html>Print Information about the ResultSet in the output.<br></html>");
		_clientTiming_chk.setToolTipText("<html>Time how long the SQL Statement takes, from the client side.<br>Clock starts when sending the SQL, clock stops when client receives first answer back from the server</html>");
		_useSemicolonHack_chk.setToolTipText(
				"<html>" +
				"Send SQL Statement to server whenever a semicolon character ';' is at the end of a line.<br>" +
				"This emulates Oracle SQL*plus to a certain point<br>" +
				"It do <b>not</b> take into consideration if we are in inside a <code>create <i>something</i></code> it will still just send the SQL.<br>" +
				"Example:" +
				"<table border=1 cellspacing=1 cellpadding=1>" +
				"<tr> <th>Statement</th> <th>Description</th> </tr>" +
				"<tr> <td> <code>select * from tab;                     </code> </td> <td> Simply sends the SQL to the server                                         </td> </tr>" +
				"<tr> <td> <code>select * from tab; --some comment      </code> </td> <td> This does <b>not</b> send the query to the server. No semicolon at the end </td> </tr>" +
				"<tr> <td> <code>select * from tab --some comment;      </code> </td> <td> This is ok, since the last character on the line is ';'.                   </td> </tr>" +
				"<tr> <td> <code>select * <br>from tab<br>where id = 99;</code> </td> <td> This is ok, since the last character on the line is ';'.                   </td> </tr>" +
//				"<tr> <td> " + // BEGIN complex row
//				"<pre>" +
//				"declare \n" +
//				"   l_sum number; \n" +
//				"begin \n" +
//				"   l_sum := 10 + 20; \n" +
//				"end; \n" +
//				"</pre> " +
//				"</td> " +
//				"<td> " +
//				"This is <b>not</b> ok, it will try to send the statement after <code>l_sum number;</code/><br>" +
//				"<b>disable</b> this option and use 'go' to send the statement. <br>" +
//				"Example: (where 'go' is the <i>terminator</i> which means, send the <i>SQL batch</i> to the server)<br>" +
//				"<pre>" +
//				"declare \n" +
//				"   l_sum number; \n" +
//				"begin \n" +
//				"   l_sum := 10 + 20; \n" +
//				"end; \n" +
//				"go \n" +
//				"</pre> " +
//				"</td> " +
//				"</tr>" +// END complex row
				"<tr> <td> " + // BEGIN complex row
				"<pre>" +
				"declare l_sum number; \n" +
				"begin \n" +
				"   l_sum := 10 + 20; \n" +
				"end; \n" +
				"</pre> " +
				"</td> " +
				"<td> " +
				"This is also ok. It will try to figgure out if the first SQL Statement in<br>" +
				"the \"batch\" is one of the following words <code>(begin|declare|create|alter)</code>.<br>" +
				"If that is the case send on Semicolon is disabled until next \"batch\".<br>" +
				"Send will then happen at next \"send terminator\", which usually is 'go' or '/'<br>" +
				"<br>" +
				"<b>NOTE</b> If the above SqlPlus simulation logic doesn't work...<br>" +
				"Just <b>disable</b> this option and use 'go' to send the statement. <br>" +
				"</td> " +
				"</tr>" +// END complex row
				"</table>" +
				"</html>");
		_enableDbmsOutput_chk.setToolTipText(
				"<html>" +
				"Enable DBMS OUTPUT trace statements to be received at the client.<br>" +
				"At start we will execute: <code>dbms_output.enable("+ Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_enableDbmsInitSize, DEFAULT_enableDbmsInitSize) +")</code><br>" +
				"For each SQL 'batch' sent, we will execute: <code>dbms_output.get_line</code><br>" +
				"</html>");
		_appendResults_chk.setToolTipText(
				"<html>" +
				"Do <b>not</b> clear results from previous executions.<br>" +
				"Instead append all results to the and of the results panel." +
				"</html>");
		_getObjectTextOnError_chk.setToolTipText(
				"<html>" +
				"On errors when you execute a procedure, get the Object Text and mark the line in the text.<br>" +
				"This makes debugging easier.<br>" +
				"<b>NOTE</b> This is <b>not</b> supported for all vendors...<br>" +
				"The Error Messaage will have to be parsed to figure out what object to display." +
				"</html>");
		_jdbcAutoCommit_chk.setToolTipText(
				"<html>" +
				"JDBC AutoCommit settings.<br>" +
				"</html>");
		_sqlBatchTermDialog_mi.setToolTipText(
				"<html>" +
				"Open a dialog where you can change the string used to send a SQL Batch to the server." +
				"</html>");
		_rsTrimStrings_chk.setToolTipText(
				"<html>" +
				"Do you want to remove leading/trailing blanks from datatypes that is treated as a string." +
				"</html>");
		_rsShowRowNumber_chk.setToolTipText(
				"<html>" +
				"Add a row number as first column when displaying data." +
				"</html>");
//		_copy_but    .setToolTipText("<html>Copy All resultsets to clipboard, tables will be into ascii format.</html>");
//		_query_txt   .setToolTipText("<html>" +
//									"Put your SQL query here.<br>" +
//									"If you select text and press 'exec' only the highlighted text will be sent to the ASE.<br>" +
//									"<br>" +
//									"Note: <b>Ctrl+Space</b> Brings up code completion. This is <b>not</b> working good for the moment, but it will be enhanced.<br>" +
//									"<br>" +
//								"</html>");
//		_query_txt.setUseFocusableTips(false);
		
		_query_txt.addCaretListener(this);
		_query_txt.getDocument().addDocumentListener(this);

		ToolTipManager.sharedInstance().registerComponent(_query_txt);

		_query_txt.setMarkOccurrences(true);
//		_query_txt.setMarkOccurrencesColor(color)
		
		// To set all RSyntaxTextAreaX components to use "_"
		RSyntaxUtilitiesX.setCharsAllowedInWords("_");
		// To set all _query_txt components to use "_", this since it's of TextEditorPane, which extends RSyntaxTextArea
		RSyntaxUtilitiesX.setCharsAllowedInWords(_query_txt, "_");

		_queryScroll.setLineNumbersEnabled(true);
		_queryScroll.setIconRowHeaderEnabled(true);
		
		// Install some extra Syntax Highlighting for RCL and TSQL
		AsetuneTokenMaker.init();  

		// Setup Auto-Completion for SQL and install error "Parser"
		_compleationProviderAbstract = CompletionProviderAbstractSql.installAutoCompletion(conn, _query_txt, _queryScroll, _queryErrStrip, _window, this);

		// add stuff to the right click menu of the text area 
		RSyntaxUtilitiesX.installRightClickMenuExtentions(_queryScroll, _window);

		// FIXME: new JScrollPane(_query_txt)
		// But this is not working as I want it
		// It disables the "auto grow" of the _query_txt window, which is problematic
		// maybe add a JSplitPane or simular...

		// Place the components within this window
		Container contentPane = _jframe != null ? _jframe.getContentPane() : _jdialog.getContentPane();
		contentPane.setLayout(new BorderLayout());
		
		setWatermarkAnchor(_topPane);
		setWatermark();

		// Get some decisions how we should layout stuff
		boolean horizontalOrientation = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_horizontalOrientation, DEFAULT_horizontalOrientation);
		boolean cmdPanelInToolbar     = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_commandPanelInToolbar, DEFAULT_commandPanelInToolbar);
		
		// How should the command/output window be oriented
		if (horizontalOrientation)
			_splitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		else
			_splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);

		_splitPane.setTopComponent(_topPane);
		_splitPane.setBottomComponent(_bottomPane);
		_splitPane.setContinuousLayout(true);
//		_splitPane.setOneTouchExpandable(true);

		if (winType == WindowType.CMDLINE_JFRAME)
			contentPane.add(_toolbar, BorderLayout.NORTH);
//		contentPane.add(_splitPane);
		contentPane.add(_mainPane);

//		_topPane.add(_queryScroll, BorderLayout.CENTER);
		_topPane.add(_queryScroll, BorderLayout.CENTER);
		_topPane.add(_queryErrStrip, BorderLayout.LINE_END);
//		_topPane.add(_queryErrStrip, BorderLayout.LINE_START);
		_topPane.setMinimumSize(new Dimension(300, 100));

//		_queryErrStrip.setVisible(false);
		_queryErrStrip.setVisible(true);

		// Add components to Control Panel
		_controlPane.add(_cntrlPrefix_sep,       "grow, gap 30, hidemode 3");
		_controlPane.add(_dbnames_cbx,           "hidemode 2");
		_controlPane.add(_exec_but,              "");
		_controlPane.add(_execGuiShowplan_but,   "");
		_controlPane.add(_appOptions_but,        "gap 30");
		_controlPane.add(_setAseOptions_but,     "");
		_controlPane.add(_codeCompletionOpt_but, "");
		_controlPane.add(new JLabel(),           "growx, pushx"); // dummy label to "grow" the _copy to the right side
		_controlPane.add(_copy_but,              "");
		_controlPane.add(_nextErr_but,           "hidemode 2");
		_controlPane.add(_prevErr_but,           "hidemode 2, wrap");

		// Keyboard shortcut for next/prev error, hmmm it didn't work 
//		_resPanel.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP,   Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), ACTION_PREV_ERROR);
//		_resPanel.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), ACTION_NEXT_ERROR);
//
//		_resPanel.getActionMap().put(ACTION_PREV_ERROR, new AbstractAction(ACTION_PREV_ERROR) { @Override public void actionPerformed(ActionEvent e) { _prevErr_but.doClick(); } });
//		_resPanel.getActionMap().put(ACTION_NEXT_ERROR, new AbstractAction(ACTION_NEXT_ERROR) { @Override public void actionPerformed(ActionEvent e) { _nextErr_but.doClick(); } });


		// Add 
		reLayoutSomeStuff(horizontalOrientation, cmdPanelInToolbar);
			
		_mainPane.add(_splitPane,         "dock center");
		_mainPane.add(_statusBar,         "dock south");

////		_bottomPane.add(_dbnames_cbx,           "split 8, hidemode 2");
////		_bottomPane.add(_exec_but,              "");
////		_bottomPane.add(_execGuiShowplan_but,   "");
//////		_bottomPane.add(_rsInTabs,              "hidemode 2");
//////		_bottomPane.add(_asPlainText,           "");
//////		_bottomPane.add(_showRowCount,          "");
////		_bottomPane.add(_appOptions_but,        "gap 30");
////		_bottomPane.add(_setAseOptions_but,     "");
////		_bottomPane.add(_codeCompletionOpt_but, "");
//////		_bottomPane.add(_showplan,              "");
////		_bottomPane.add(new JLabel(),           "growx, pushx"); // dummy label to "grow" the _copy to the right side
////		_bottomPane.add(_copy_but,              "");
////		_bottomPane.add(_nextErr_but,           "hidemode 2");
////		_bottomPane.add(_prevErr_but,           "hidemode 2, wrap");
//		
//		_bottomPane.add(_resPanelScroll,        "span 4, width 100%, height 100%, hidemode 3");
//		_bottomPane.add(_resPanelTextScroll,    "span 4, width 100%, height 100%, hidemode 3");
////		_bottomPane.add(_msgline,               "dock south");
////		_bottomPane.add(_statusBar,             "dock south");

		_resPanelScroll.getVerticalScrollBar()  .setUnitIncrement(16);
		_resPanelScroll.getHorizontalScrollBar().setUnitIncrement(16);

		_resPanelScroll    .setVisible(true);
		_resPanelTextScroll.setVisible(false);
		
		// ADD Ctrl+e, F5, F9
		_query_txt.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_E,  Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), ACTION_EXECUTE);
		_query_txt.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0), ACTION_EXECUTE);
		_query_txt.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), ACTION_EXECUTE);

		// ADD Ctrl+Shift+e, Shift+F5, Shift+F9
		_query_txt.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_E,  InputEvent.SHIFT_DOWN_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), ACTION_EXECUTE_GUI_SHOWPLAN);
		_query_txt.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F9, InputEvent.SHIFT_DOWN_MASK), ACTION_EXECUTE_GUI_SHOWPLAN);
		_query_txt.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, InputEvent.SHIFT_DOWN_MASK), ACTION_EXECUTE_GUI_SHOWPLAN);

		_query_txt.getActionMap().put(ACTION_EXECUTE, new AbstractAction(ACTION_EXECUTE)
		{
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e)
			{
				boolean isConnected = true;
				if (_conn == null)
					isConnected = false;
				else
				{
					try { isConnected = ! _conn.isClosed(); } 
					catch (SQLException ex) {/*ignore*/}
				}
				
				if ( ! isConnected )
					action_connect(null);
				_exec_but.doClick();
			}
		});
		_query_txt.getActionMap().put(ACTION_EXECUTE_GUI_SHOWPLAN, new AbstractAction(ACTION_EXECUTE_GUI_SHOWPLAN)
		{
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e)
			{
				_execGuiShowplan_but.doClick();
			}
		});


		_exec_but           .addActionListener(this);
		_execGuiShowplan_but.addActionListener(this);
		_dbnames_cbx        .addActionListener(this);

		_exec_but           .setActionCommand(ACTION_EXECUTE);
		_execGuiShowplan_but.setActionCommand(ACTION_EXECUTE_GUI_SHOWPLAN);

		// Set how many items the DBList can have before a JScrollBar is visible
		_dbnames_cbx.setMaximumRowCount(50);

		// Refresh the database list (if ASE)
		if (_conn != null && _connType == ConnectionDialog.TDS_CONN)
			setDbNames();

		// Refresh the database list (if MSSQL)
		if (_conn != null && DbUtils.isProductName(DbUtils.DB_PROD_NAME_MSSQL, _connectedToProductName))
			setDbNames();

		// Write some initial text, and mark it
		// or Kick of a initial SQL query, if one is specified.
		if (StringUtil.isNullOrBlank(sql))
		{
			// if no input file
			if ( StringUtil.isNullOrBlank(inputFile) )
			{
//				Runnable doLater = new Runnable()
//				{
//					@Override
//					public void run()
//					{
//						_query_txt.requestFocus();
//
//						String helper = "Write your SQL query here";
//						_query_txt.setText(helper);
//						_query_txt.setSelectionStart(0);
//						_query_txt.setSelectionEnd(helper.length());
//						_query_txt.setDirty(false);
//					}
//				};
//				SwingUtilities.invokeLater(doLater);
			}
		}
		else
		{
			_query_txt.setText(sql);
			if (_conn != null)
				displayQueryResults(sql, 0, false);
		}
		_query_txt.setDirty(false);
		_statusBar.setFilename(_query_txt.getFileFullPath(), _query_txt.getEncoding());

		// Try to load the input file.
		if ( ! StringUtil.isNullOrBlank(inputFile) )
		{
			File f = new File(inputFile);
			if ( ! f.exists() )
			{
				SwingUtils.showInfoMessage("File doesn't exists", "The input file '"+inputFile+"' doesn't exists.");
			}
			else
			{
//				try
//				{
//					FileLocation loc = FileLocation.create(f);
//					_query_txt.load(loc, null);
//					_statusBar.setFilename(_query_txt.getFileFullPath());
//				}
//				catch (IOException ex)
//				{
//					SwingUtils.showErrorMessage("Problems Loading file", "Problems Loading file", ex);
//				}
				openFile(f);
			}
		}

		// Set initial size of the JFrame, and make it visable
		//this.setSize(600, 400);
		//this.setVisible(true);

		// load windows properties
		// note this is done in openTheWindow()
//		loadWinProps();

		loadProps();
		
		// Set components if visible, enabled etc...
		setComponentVisibility();
		
		if (winType == WindowType.CMDLINE_JFRAME)
		{
			_logger.info("Checking for new release...");
			
			// Create a thread that does this...
			// Apparently the noBlockCheckSqlWindow() hits problems when it accesses the CheckForUpdates, which uses ProxyVole
			// My guess is that ProxyVole want's to unpack it's DDL, which takes time...
			Thread checkForUpdatesThread = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					CheckForUpdates.noBlockCheckSqlWindow(_jframe, false, true);
				}
			}, "checkForUpdatesThread");
			checkForUpdatesThread.setDaemon(true);
			checkForUpdatesThread.start();
//			CheckForUpdates.noBlockCheckSqlWindow(_jframe, false, true);

			// Only start the watchdog if started from cmd line
			_watchdogIsFileChanged = createWatchdogIsFileChanged();
		}
	}

	private void reLayoutSomeStuff(boolean horizontalOrientation, boolean cmdPanelInToolbar)
	{
		_bottomPane.remove(_resPanelScroll);
		_bottomPane.remove(_resPanelTextScroll);

		_cntrlPrefix_sep.setVisible( horizontalOrientation || cmdPanelInToolbar );
		
		if ( horizontalOrientation )
		{
			_splitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
			_possibleCtrlPane.add(_controlPane);
		}
		else
		{
			_splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
			if (cmdPanelInToolbar)
				_possibleCtrlPane.add(_controlPane);
			else
				_bottomPane.add(_controlPane,     "growx, pushx, wrap");
		}
			
		_bottomPane.add(_resPanelScroll,        "span 4, width 100%, height 100%, hidemode 3");
		_bottomPane.add(_resPanelTextScroll,    "span 4, width 100%, height 100%, hidemode 3");
		
		// maybe repaint
		_toolbar.revalidate();
		_splitPane.revalidate();
		_bottomPane.revalidate();
	}

	/** Watchdog to check if files are changed */
	protected WatchdogIsFileChanged _watchdogIsFileChanged = null;
	protected WatchdogIsFileChanged createWatchdogIsFileChanged()
	{
		WatchdogIsFileChanged watchdog = new WatchdogIsFileChanged(this, 5000);
		watchdog.start();

		return watchdog;
	}

	private void loadProps()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		
		_lastFileNameSaveMax =                   conf.getIntProperty(    PROPKEY_lastFileNameSaveMax,               DEFAULT_lastFileNameSaveMax);
		_fSaveBeforeExec_mi        .setSelected( conf.getBooleanProperty(PROPKEY_saveBeforeExecute,                 DEFAULT_saveBeforeExecute) );
		_prefWinOnConnect_mi       .setSelected( conf.getBooleanProperty(PROPKEY_restoreWinSizeForConn,             DEFAULT_restoreWinSizeForConn) );
		_prefShowAppNameInTitle_mi .setSelected( conf.getBooleanProperty(PROPKEY_showAppNameInTitle,                DEFAULT_showAppNameInTitle) );
		_prefPlaceCntrlInToolbar_mi.setSelected( conf.getBooleanProperty(PROPKEY_commandPanelInToolbar,             DEFAULT_commandPanelInToolbar) );
		_prefSplitHorizontal_mi    .setSelected( conf.getBooleanProperty(PROPKEY_horizontalOrientation,             DEFAULT_horizontalOrientation) );
		_asPlainText_chk           .setSelected( conf.getBooleanProperty(PROPKEY_asPlainText,                       DEFAULT_asPlainText) );
		_showRowCount_chk          .setSelected( conf.getBooleanProperty(PROPKEY_showRowCount,                      DEFAULT_showRowCount) );
		_limitRsRowsRead_chk       .setSelected( conf.getBooleanProperty(PROPKEY_limitRsRowsRead,                   DEFAULT_limitRsRowsRead) );
		_showSentSql_chk           .setSelected( conf.getBooleanProperty(PROPKEY_showSentSql,                       DEFAULT_showSentSql) );
		_printRsInfo_chk           .setSelected( conf.getBooleanProperty(PROPKEY_printRsInfo,                       DEFAULT_printRsInfo) );
		_rsTrimStrings_chk         .setSelected( conf.getBooleanProperty(ResultSetTableModel.PROPKEY_StringTrim,    ResultSetTableModel.DEFAULT_StringTrim) );
		_rsShowRowNumber_chk       .setSelected( conf.getBooleanProperty(ResultSetTableModel.PROPKEY_ShowRowNumber, ResultSetTableModel.DEFAULT_ShowRowNumber) );
		_clientTiming_chk          .setSelected( conf.getBooleanProperty(PROPKEY_clientTiming,                      DEFAULT_clientTiming) );
		_useSemicolonHack_chk      .setSelected( conf.getBooleanProperty(PROPKEY_useSemicolonHack,                  DEFAULT_useSemicolonHack) );
		_enableDbmsOutput_chk      .setSelected( conf.getBooleanProperty(PROPKEY_enableDbmsOutput,                  DEFAULT_enableDbmsOutput) );
		_appendResults_chk         .setSelected( conf.getBooleanProperty(PROPKEY_appendResults,                     DEFAULT_appendResults) );
		_getObjectTextOnError_chk  .setSelected( conf.getBooleanProperty(PROPKEY_getObjectTextOnError,              DEFAULT_getObjectTextOnError) );
		_jdbcAutoCommit_chk        .setSelected( conf.getBooleanProperty(PROPKEY_jdbcAutoCommit,                    DEFAULT_jdbcAutoCommit) );
		_jdbcAutoCommit_chk        .setVisible(  conf.getBooleanProperty(PROPKEY_jdbcAutoCommitShow,                DEFAULT_jdbcAutoCommitShow) );

		_cmdHistoryFilename     = conf.getProperty(PROPKEY_historyFileName,        DEFAULT_historyFileName);
		_favoriteCmdFilenameSql = conf.getProperty(PROPKEY_favoriteCmdFileNameSql, DEFAULT_favoriteCmdFileNameSql);
		_favoriteCmdFilenameRcl = conf.getProperty(PROPKEY_favoriteCmdFileNameRcl, DEFAULT_favoriteCmdFileNameRcl);

		loadFileHistory();
	}

	private void saveProps()
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null)
			return;

		if (_windowType == null || ( _windowType != null && _windowType != WindowType.CMDLINE_JFRAME) )
			return;
			
		conf.setProperty(PROPKEY_lastFileNameSaveMax,               _lastFileNameSaveMax);
		conf.setProperty(PROPKEY_saveBeforeExecute,                 _fSaveBeforeExec_mi        .isSelected());
		conf.setProperty(PROPKEY_restoreWinSizeForConn,             _prefWinOnConnect_mi       .isSelected());
		conf.setProperty(PROPKEY_showAppNameInTitle,                _prefShowAppNameInTitle_mi .isSelected());
		conf.setProperty(PROPKEY_commandPanelInToolbar,             _prefPlaceCntrlInToolbar_mi.isSelected());
		conf.setProperty(PROPKEY_horizontalOrientation,             _prefSplitHorizontal_mi    .isSelected());
		conf.setProperty(PROPKEY_asPlainText,                       _asPlainText_chk           .isSelected());
		conf.setProperty(PROPKEY_showRowCount,                      _showRowCount_chk          .isSelected());
		conf.setProperty(PROPKEY_limitRsRowsRead,                   _limitRsRowsRead_chk       .isSelected());	
		conf.setProperty(PROPKEY_showSentSql,                       _showSentSql_chk           .isSelected());
		conf.setProperty(PROPKEY_printRsInfo,                       _printRsInfo_chk           .isSelected());
		conf.setProperty(ResultSetTableModel.PROPKEY_StringTrim,    _rsTrimStrings_chk         .isSelected());
		conf.setProperty(ResultSetTableModel.PROPKEY_ShowRowNumber, _rsShowRowNumber_chk       .isSelected());
		conf.setProperty(PROPKEY_clientTiming,                      _clientTiming_chk          .isSelected());
		conf.setProperty(PROPKEY_useSemicolonHack,                  _useSemicolonHack_chk      .isSelected());
		conf.setProperty(PROPKEY_enableDbmsOutput,                  _enableDbmsOutput_chk      .isSelected());
		conf.setProperty(PROPKEY_appendResults,                     _appendResults_chk         .isSelected());
		conf.setProperty(PROPKEY_getObjectTextOnError,              _getObjectTextOnError_chk  .isSelected());
		conf.setProperty(PROPKEY_jdbcAutoCommit,                    _jdbcAutoCommit_chk        .isSelected());
		conf.setProperty(PROPKEY_jdbcAutoCommitShow,                _jdbcAutoCommit_chk        .isVisible());
		
		conf.setProperty(PROPKEY_historyFileName,                   _cmdHistoryFilename);
		conf.setProperty(PROPKEY_favoriteCmdFileNameSql,            _favoriteCmdFilenameSql);
		conf.setProperty(PROPKEY_favoriteCmdFileNameRcl,            _favoriteCmdFilenameRcl);

		conf.save();
	}

	/**
	 * Saves some properties about the window
	 * <p>
	 * NOTE: normally you would load window size via loadWinProps(), but this is done openTheWindow()...
	 */
	public void saveWinProps()
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null)
			return;

		if (_window == null)
			return;

		String screenResStr = SwingUtils.getScreenResulutionAsString();
		String srvStr       = _titleSrvStr;
		boolean saveWinPropsForConn = _prefWinOnConnect_mi.isSelected() && srvStr != null && _conn != null;

		// Save Window ScreenSize/ScreenPosition/divierLocation based on the what server we are connected to
		// or just simply save ScreenSize/ScreenPosition/divierLocation
		if (saveWinPropsForConn)
		{
			conf.setProperty("QueryWindow."+screenResStr+"."+srvStr+".size.width",         _window.getSize().width);
			conf.setProperty("QueryWindow."+screenResStr+"."+srvStr+".size.height",        _window.getSize().height);
			conf.setProperty("QueryWindow."+screenResStr+"."+srvStr+".splitPane.location", _splitPane.getDividerLocation());
		}
		else
		{
			conf.setProperty("QueryWindow.size.width",         _window.getSize().width);
			conf.setProperty("QueryWindow.size.height",        _window.getSize().height);
			conf.setProperty("QueryWindow.splitPane.location", _splitPane.getDividerLocation());
	
			conf.setProperty("QueryWindow."+screenResStr+".size.width",         _window.getSize().width);
			conf.setProperty("QueryWindow."+screenResStr+".size.height",        _window.getSize().height);
			conf.setProperty("QueryWindow."+screenResStr+".splitPane.location", _splitPane.getDividerLocation());
		}
		
		if (_window.isVisible())
		{
			if (saveWinPropsForConn)
			{
				conf.setProperty("QueryWindow."+screenResStr+"."+srvStr+".size.pos.x",  _window.getLocationOnScreen().x);
				conf.setProperty("QueryWindow."+screenResStr+"."+srvStr+".size.pos.y",  _window.getLocationOnScreen().y);
			}
			else
			{
				conf.setProperty("QueryWindow.size.pos.x",  _window.getLocationOnScreen().x);
				conf.setProperty("QueryWindow.size.pos.y",  _window.getLocationOnScreen().y);
	
				conf.setProperty("QueryWindow."+screenResStr+".size.pos.x",  _window.getLocationOnScreen().x);
				conf.setProperty("QueryWindow."+screenResStr+".size.pos.y",  _window.getLocationOnScreen().y);
			}
		}
		
		File f = new File(_query_txt.getFileFullPath());
		if (f.exists())
			conf.setProperty("QueryWindow.lastFileName", f.toString());
		else
			conf.remove("QueryWindow.lastFileName");
		
		conf.save();
	}
	private void loadWinPropsForSrv(String srvStr)
	{
		if (srvStr == null)                   return;
		if (NOT_CONNECTED_STR.equals(srvStr)) return;

		// Return if this is NOT enabled
		if ( ! _prefWinOnConnect_mi.isSelected() )
			return;

		Configuration conf  = Configuration.getCombinedConfiguration();
		String screenResStr = SwingUtils.getScreenResulutionAsString();

		int width   = 600;
		int height  = 550;
		int winPosX = -1;
		int winPosY = -1;
		int divLoc  = -1;

//		width   = conf.getIntProperty("QueryWindow.size.width",         width);
//		height  = conf.getIntProperty("QueryWindow.size.height",        height);
//		winPosX = conf.getIntProperty("QueryWindow.size.pos.x",         winPosX);
//		winPosY = conf.getIntProperty("QueryWindow.size.pos.y",         winPosY);
//		divLoc  = conf.getIntProperty("QueryWindow.splitPane.location", divLoc);
//
//		width   = conf.getIntProperty("QueryWindow."+screenResStr+".size.width",         width);
//		height  = conf.getIntProperty("QueryWindow."+screenResStr+".size.height",        height);
//		winPosX = conf.getIntProperty("QueryWindow."+screenResStr+".size.pos.x",         winPosX);
//		winPosY = conf.getIntProperty("QueryWindow."+screenResStr+".size.pos.y",         winPosY);
//		divLoc  = conf.getIntProperty("QueryWindow."+screenResStr+".splitPane.location", divLoc);

		width   = conf.getIntProperty("QueryWindow."+screenResStr+"."+srvStr+".size.width",         width);
		height  = conf.getIntProperty("QueryWindow."+screenResStr+"."+srvStr+".size.height",        height);
		winPosX = conf.getIntProperty("QueryWindow."+screenResStr+"."+srvStr+".size.pos.x",         winPosX);
		winPosY = conf.getIntProperty("QueryWindow."+screenResStr+"."+srvStr+".size.pos.y",         winPosY);
		divLoc  = conf.getIntProperty("QueryWindow."+screenResStr+"."+srvStr+".splitPane.location", divLoc);
		
		// Set size
		if (width >= 0 && height >= 0)
			_window.setSize(width, height);

		// Set to last known position
		if ( ! SwingUtils.isOutOfScreen(winPosX, winPosY, width, height) )
		{
			_logger.debug("Open main window in last known position.");
			_window.setLocation(winPosX, winPosY);
		}
		
		// Set the split pane location
		if (divLoc >= 0)
			_splitPane.setDividerLocation(divLoc);
	}

	/**
	 * 
	 * @param width
	 * @param height
	 */
	public void setSize(int width, int height)
	{
		_window.setSize(width, height);
	}
	/**
	 * 
	 * @param comp
	 */
	public void setLocationRelativeTo(Component comp)
	{
		_window.setLocationRelativeTo(comp);
	}
	/**
	 * 
	 * @param b
	 */
	public void setVisible(boolean b)
	{
		_window.setVisible(b);
	}


	/*---------------------------------------------------
	** BEGIN: implementing ActionListener
	**--------------------------------------------------*/
	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source    = e.getSource();
		String actionCmd = e.getActionCommand();

		_logger.debug("ACTION '"+actionCmd+"'.");

		if (ACTION_CONNECT.equals(actionCmd))
			action_connect(e);

		if (ACTION_DISCONNECT.equals(actionCmd))
			action_disconnect(e);
		
		if (ACTION_SPLITPANE_TOGGLE.equals(actionCmd))
			action_splitPaneToggle(e);
		
		if (ACTION_CLONE_CONNECT.equals(actionCmd))
			action_cloneConnect(e);
		
		if (ACTION_FILE_NEW.equals(actionCmd))
			action_fileNew(e);

		if (ACTION_FILE_OPEN.equals(actionCmd))
			action_fileOpen(e, null);

//		if (ACTION_FILE_CLOSE.equals(actionCmd))
//			action_fileClose(e);

		if (ACTION_FILE_SAVE.equals(actionCmd))
			action_fileSave(e);

		if (ACTION_FILE_SAVE_AS.equals(actionCmd))
			action_fileSaveAs(e);

		if (ACTION_EXIT.equals(actionCmd))
			action_exit(e);

		if (ACTION_VIEW_ASE_CONFIG.equals(actionCmd))
			action_viewAseConfig(e);

		if (ACTION_RS_GENERATE_CHANGED_DDL.equals(actionCmd))
			action_rsGenerateDdl(e, ACTION_RS_GENERATE_CHANGED_DDL);

		if (ACTION_RS_GENERATE_ALL_DDL.equals(actionCmd))
			action_rsGenerateDdl(e, ACTION_RS_GENERATE_ALL_DDL);

		if (ACTION_CMD_SQL.equals(actionCmd))
			SwingUtils.showInfoMessage(null, "Not yet implemented", "Not yet implemented.");

		if (ACTION_CMD_RCL.equals(actionCmd))
			SwingUtils.showInfoMessage(null, "Not yet implemented", "Not yet implemented.");

		if (ACTION_RS_DUMP_QUEUE.equals(actionCmd))
			action_rsDumpQueue(e);

		if (ACTION_RS_WHO_IS_DOWN.equals(actionCmd))
			action_rsWhoIsDown(e);

		if (ACTION_ASE_CAPTURE_SQL.equals(actionCmd))
			action_aseCaptureSql(e);

		if (ACTION_VIEW_CMD_HISTORY.equals(actionCmd))
			action_viewCmdHistory(e);

		if (ACTION_VIEW_LOG_TAIL.equals(actionCmd))
			action_viewLogTail(e);

		// ACTION for "exec"
		if (ACTION_EXECUTE.equals(actionCmd))
			actionExecute(e, false);

		// ACTION for "GUI exec"
		if (ACTION_EXECUTE_GUI_SHOWPLAN.equals(actionCmd))
			actionExecute(e, true);

		// ACTION for "database context"
		if (_dbnames_cbx.equals(source))
		{
			useDb( (String) _dbnames_cbx.getSelectedItem() );
			
			// mark code completion for refresh
			if (_compleationProviderAbstract != null)
				_compleationProviderAbstract.setNeedRefresh(true);
		}
		
//		// ACTION for "copy"
//		if (_copy_but.equals(source))
//			actionCopy(e);

		if (ACTION_PREV_ERROR.equals(actionCmd))
			action_prevError(e);

		if (ACTION_NEXT_ERROR.equals(actionCmd))
			action_nextError(e);

		
		setComponentVisibility();
		setWatermark();
	}
	/*---------------------------------------------------
	** END: implementing ActionListener
	**--------------------------------------------------*/

	
	
	/*---------------------------------------------------
	** END: implementing CaretListener
	**--------------------------------------------------*/
	@Override
	public void caretUpdate(CaretEvent e)
	{
		boolean isDirty = _query_txt.isDirty();
		_statusBar.setFilenameDirty(isDirty);

		_fSave_mi  .setEnabled(isDirty);
		_fSaveAs_mi.setEnabled(isDirty);
		
		_statusBar.setEditorPos(_query_txt.getCaretLineNumber(), _query_txt.getCaretOffsetFromLineStart());

		setWatermark();
	}
	/*---------------------------------------------------
	** END: implementing CaretListener
	**--------------------------------------------------*/


	/*---------------------------------------------------
	** END: implementing DocumentListener
	**--------------------------------------------------*/
	@Override
	public void insertUpdate(DocumentEvent e)
	{
		resetParserDbMessages();
	}
	@Override
	public void removeUpdate(DocumentEvent e)
	{
		resetParserDbMessages();
	}
	@Override
	public void changedUpdate(DocumentEvent e)
	{
		resetParserDbMessages();
	}
	/**
	 * Reset the Messages in the Document, next time the parser is executing red lines will be gone/disabled
	 */
	private void resetParserDbMessages()
	{
		_query_txt.getDocument().putProperty(ParserProperties.DB_MESSAGES, null);
		_nextErr_but.setVisible(false);
		_prevErr_but.setVisible(false);
	}
	/*---------------------------------------------------
	** END: implementing DocumentListener
	**--------------------------------------------------*/


	/**
	 * Set the windws title
	 * @param srvStr servername we are connected to, null = not connected.
	 */
	private void setSrvInTitle(String srvStr)
	{
		_titleSrvStr = srvStr;
		String title = _titlePrefix;
		if (srvStr != null)
			title += " - " + srvStr;

		boolean showAppNameInTitle = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_showAppNameInTitle, DEFAULT_showAppNameInTitle);
		if ( ! showAppNameInTitle )
			title = StringUtil.hasValue(srvStr) ? srvStr : "unknown";
		
		if (_jframe  != null) _jframe .setTitle(title);
		if (_jdialog != null) _jdialog.setTitle(title);
	}

	private boolean isNull(String str)
	{
		if (str == null)        return true;
		if (str.equals("null")) return true;
		return false;
	}
	private void action_connect(ActionEvent e)
	{
		// just for debugging...
		//new Exception("action_connect called from").printStackTrace();

		Object source = null;
		String action = null;
		if (e != null)
		{
			source = e.getSource();
			action = e.getActionCommand();
		}

		// Create a new dialog Window
		boolean checkAseCfg    = false;
		boolean showAseTab     = true;
		boolean showAseOptions = false;
		boolean showHostmonTab = false;
		boolean showPcsTab     = false;
		boolean showOfflineTab = true;
		boolean showJdbcTab    = true;

		_srvVersion                 = 0;
		_connectedAtTime            = 0;
		_connectedDriverName        = null;
		_connectedDriverVersion     = null;
		_connectedToProductName     = null;
		_connectedToProductVersion  = null;
		_connectedToServerName      = null;
		_connectedToSysListeners    = null;
		_connectedSrvCharset        = null;
		_connectedSrvSortorder      = null;
		_connectedAsUser            = null;
		_connectedWithUrl           = null;
		_connectedClientCharsetId   = null;
		_connectedClientCharsetName = null;
		_connectedClientCharsetDesc = null;
		
		_currentDbName              = null;
		_dbmsOutputIsEnabled        = false; // I don't think you need to disable it, hopefully it will be disabled when disconnecting

		// Reset statistics
		resetExecStatistics();

		// mark code completion for refresh
		if (_compleationProviderAbstract != null)
			_compleationProviderAbstract.setNeedRefresh(true);

		// set default database name, in case if it's a ASE Server
		String aseDbname = null;
		
		// Create the connection dialog
		ConnectionDialog connDialog = new ConnectionDialog(_jframe, checkAseCfg, showAseTab, showAseOptions, showHostmonTab, showPcsTab, showOfflineTab, showJdbcTab);

		// If the source is "CommandLine" Parameters
		//   - call the ConnectionDialogs logic to just connect
		// else
		//   - open the dialog
		if (source != null && source instanceof CommandLine && showAseTab)
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

					// ASE Connect
					if ( ! isNull(ppe.getProperty(key, "aseServer")) )
					{
						connDialog.setSelectedTab(ConnectionDialog.TDS_CONN);
						if (!isNull(ppe.getProperty(key, "aseUsername"))) connDialog.setAseUsername(ppe.getProperty(key, "aseUsername"));
						if (!isNull(ppe.getProperty(key, "asePassword"))) connDialog.setAsePassword(ppe.getProperty(key, "asePassword"));
						if (!isNull(ppe.getProperty(key, "aseServer"))  ) connDialog.setAseServer  (ppe.getProperty(key, "aseServer"));
						if (!isNull(ppe.getProperty(key, "aseDbname"))  ) aseDbname = ppe.getProperty(key, "aseDbname");
	
	
	//					if (!isNull(ppe.getProperty(key, "sshUsername"))) connDialog.setSshUsername(ppe.getProperty(key, "sshUsername"));
	//					if (!isNull(ppe.getProperty(key, "sshPassword"))) connDialog.setSshPassword(ppe.getProperty(key, "sshPassword"));
	//					if (!isNull(ppe.getProperty(key, "sshHostname"))) connDialog.setSshHostname(ppe.getProperty(key, "sshHostname"));
	//					if (!isNull(ppe.getProperty(key, "sshPort"))    ) connDialog.setSshPort    (ppe.getProperty(key, "sshPort"));
					}

					// JDBC Connect
					if ( ! isNull(ppe.getProperty(key, "jdbcUrl")) )
					{
						connDialog.setSelectedTab(ConnectionDialog.JDBC_CONN);
						if (!isNull(ppe.getProperty(key, "jdbcDriver")))   connDialog.setJdbcDriver(  ppe.getProperty(key, "jdbcDriver")); // do this first, it will trigger select action... which sets the URL to default value
						if (!isNull(ppe.getProperty(key, "jdbcUrl")))      connDialog.setJdbcUrl(     ppe.getProperty(key, "jdbcUrl"));
						if (!isNull(ppe.getProperty(key, "jdbcUsername"))) connDialog.setJdbcUsername(ppe.getProperty(key, "jdbcUsername"));
						if (!isNull(ppe.getProperty(key, "jdbcPassword"))) connDialog.setJdbcPassword(ppe.getProperty(key, "jdbcPassword"));
					}

					// Connection Profile
					if ( ! isNull(ppe.getProperty(key, "connProfile")) )
					{
						connDialog.setConnProfileName(ppe.getProperty(key, "connProfile"));
					}

					connDialog.actionPerformed(new ActionEvent(this, 0, ConnectionDialog.ACTION_OK));
				}
				catch(Exception ex)
				{
					_logger.warn(ex.getMessage());
				}
			}
		}
		else
		{
			// Show the dialog and wait for response
			connDialog.setVisible(true);
			connDialog.dispose();
		}

		// Get what was connected to...
		int connType = connDialog.getConnectionType();

		if ( connType == ConnectionDialog.CANCEL)
			return;

		// Get product info
		try	
		{
			_srvVersion                 = 0;
			_connectedAtTime            = System.currentTimeMillis();
			_connectedDriverName        = connDialog.getDriverName();
			_connectedDriverVersion     = connDialog.getDriverVersion();
			_connectedToProductName     = connDialog.getDatabaseProductName(); 
			_connectedToProductVersion  = connDialog.getDatabaseProductVersion(); 
			_connectedToServerName      = connDialog.getDatabaseServerName();
			_connectedToSysListeners    = null;
			_connectedSrvCharset        = null;
			_connectedSrvSortorder      = null;
			_connectedAsUser            = connDialog.getUsername();
			_connectedWithUrl           = connDialog.getUrl();
			_connectedClientCharsetId   = null;
			_connectedClientCharsetName = null;
			_connectedClientCharsetDesc = null;

			_logger.info("Connected to DatabaseProductName='"+_connectedToProductName+"', DatabaseProductVersion='"+_connectedToProductVersion+"', DatabaseServerName='"+_connectedToServerName+"' with Username='"+_connectedAsUser+"', toURL='"+_connectedWithUrl+"', using Driver='"+_connectedDriverName+"', DriverVersion='"+_connectedDriverVersion+"'.");
		} 
		catch (Throwable ex) 
		{
			if (_logger.isDebugEnabled())
				_logger.warn("Problems getting DatabaseProductName, DatabaseProductVersion, DatabaseServerName or Username. Caught: "+ex, ex);
			else
				_logger.warn("Problems getting DatabaseProductName, DatabaseProductVersion, DatabaseServerName or Username. Caught: "+ex);
		}
		
		if ( connType == ConnectionDialog.TDS_CONN)
		{
			_conn = connDialog.getAseConn();

//			if (_conn != null)
			if (AseConnectionUtils.isConnectionOk(_conn, true, _jframe))
			{
				_connType = ConnectionDialog.TDS_CONN;

				if (connDialog.isDatabaseProduct(DbUtils.DB_PROD_NAME_SYBASE_ASE))
				{
					if (aseDbname != null)
						AseConnectionUtils.useDbname(_conn, aseDbname);

					setDbNames();

					_compleationProviderAbstract = CompletionProviderAse.installAutoCompletion(_query_txt, _queryScroll, _queryErrStrip, _window, this);
					_tooltipProviderAbstract     = new ToolTipSupplierAse(_window, _compleationProviderAbstract, this);
					_query_txt.setToolTipSupplier(_tooltipProviderAbstract);

					_srvVersion              = AseConnectionUtils.getAseVersionNumber(_conn);

					// Only SA_ROLE can get listeners
					_connectedToSysListeners = "Sorry you need 'sa_role' to query master..syslisteners";
					if (AseConnectionUtils.hasRole(_conn, AseConnectionUtils.SA_ROLE))
						_connectedToSysListeners = AseConnectionUtils.getListeners(_conn, false, false, _window);

					// Sortorder & charset
					_connectedSrvCharset        = AseConnectionUtils.getAseCharset(_conn);
					_connectedSrvSortorder      = AseConnectionUtils.getAseSortorder(_conn);
					_connectedClientCharsetId   = AseConnectionUtils.getClientCharsetId(_conn);
					_connectedClientCharsetName = AseConnectionUtils.getClientCharsetName(_conn);
					_connectedClientCharsetDesc = AseConnectionUtils.getClientCharsetDesc(_conn);

					// Also get "various statuses" like if we are in a transaction or not
					_aseConnectionStateInfo = AseConnectionUtils.getAseConnectionStateInfo(_conn, true);
					_statusBar.setAseConnectionStateInfo(_aseConnectionStateInfo);
					setWatermark();
				}
				else if (connDialog.isDatabaseProduct(DbUtils.DB_PROD_NAME_SYBASE_RS))
				{
					_compleationProviderAbstract = CompletionProviderRepServer.installAutoCompletion(_query_txt, _queryScroll, _queryErrStrip, _window, this);
					_tooltipProviderAbstract     = new ToolTipSupplierRepServer(_window, _compleationProviderAbstract, this);
					_query_txt.setToolTipSupplier(_tooltipProviderAbstract);

					_srvVersion = AseConnectionUtils.getRsVersionNumber(_conn);

					// Sortorder & charset
					_connectedSrvCharset   = RepServerUtils.getRsCharset(_conn);
					_connectedSrvSortorder = RepServerUtils.getRsSortorder(_conn);

					// JDBC Specific statuses DO NOT DO THIS FOR REPSERVER
					//_jdbcConnectionStateInfo = DbUtils.getJdbcConnectionStateInfo(_conn);
					//_statusBar.setJdbcConnectionStateInfo(_jdbcConnectionStateInfo);
					
					_logger.info("Connected to Replication Server version '"+_srvVersion+"'.");
				}
				else if (connDialog.isDatabaseProduct(DbUtils.DB_PROD_NAME_SYBASE_ASA))
				{
					_compleationProviderAbstract = CompletionProviderJdbc.installAutoCompletion(_query_txt, _queryScroll, _queryErrStrip, _window, this);
					_tooltipProviderAbstract     = new ToolTipSupplierAsa(_window, _compleationProviderAbstract, this);
					_query_txt.setToolTipSupplier(_tooltipProviderAbstract);

//					_aseVersion = AseConnectionUtils.getAsaVersionNumber(_conn); // FIXME: ASA has another "system"

					// Sortorder & charset
					_connectedSrvCharset   = AseConnectionUtils.getAsaCharset(_conn);
					_connectedSrvSortorder = AseConnectionUtils.getAsaSortorder(_conn);

					// JDBC Specific statuses
					_jdbcConnectionStateInfo = DbUtils.getJdbcConnectionStateInfo(_conn);
					_statusBar.setJdbcConnectionStateInfo(_jdbcConnectionStateInfo);
					
					_logger.info("Connected to SQL Anywhere version '"+_srvVersion+"'.");
				}
				else if (connDialog.isDatabaseProduct(DbUtils.DB_PROD_NAME_SYBASE_IQ))
				{
					_compleationProviderAbstract = CompletionProviderJdbc.installAutoCompletion(_query_txt, _queryScroll, _queryErrStrip, _window, this);
					_tooltipProviderAbstract     = new ToolTipSupplierIq(_window, _compleationProviderAbstract, this);
					_query_txt.setToolTipSupplier(_tooltipProviderAbstract);

//					_aseVersion = AseConnectionUtils.getIqVersionNumber(_conn); // FIXME: IQ has another "system"

					// Sortorder & charset
					_connectedSrvCharset   = AseConnectionUtils.getAsaCharset(_conn);
					_connectedSrvSortorder = AseConnectionUtils.getAsaSortorder(_conn);

					// JDBC Specific statuses
					_jdbcConnectionStateInfo = DbUtils.getJdbcConnectionStateInfo(_conn);
					_statusBar.setJdbcConnectionStateInfo(_jdbcConnectionStateInfo);
					
					_logger.info("Connected to Sybase IQ version '"+_srvVersion+"'.");
				}
				else
				{
					_compleationProviderAbstract = CompletionProviderJdbc.installAutoCompletion(_query_txt, _queryScroll, _queryErrStrip, _window, this);
					_tooltipProviderAbstract     = new ToolTipSupplierJdbc(_window, _compleationProviderAbstract, this);
					_query_txt.setToolTipSupplier(_tooltipProviderAbstract);

					_logger.info("Connected to 'other' Sybase TDS server with product name'"+_connectedToProductName+"'.");
				}

				_setAseOptions_but.setComponentPopupMenu( createSetOptionButtonPopupMenu(_srvVersion) );


				// Load Windown Props for this server
				String aseSrv      = AseConnectionFactory.getServer();
				String aseHostPort = AseConnectionFactory.getHostPortStr();
				String srvStr      = aseSrv != null ? aseSrv : aseHostPort; 
				loadWinPropsForSrv(srvStr);


				// Send connection info
				final SqlwConnectInfo connInfo = new CheckForUpdates.SqlwConnectInfo(connType);
				connInfo.setProdName         (_connectedToProductName);
				connInfo.setProdVersionStr   (_connectedToProductVersion);
				connInfo.setJdbcDriverName   (_connectedDriverName);
				connInfo.setJdbcDriverVersion(_connectedDriverVersion);
				connInfo.setJdbcDriver       (AseConnectionFactory.getDriver());
				connInfo.setJdbcUrl          (_connectedWithUrl); 
				connInfo.setSrvVersionInt    (_srvVersion);
				connInfo.setSrvName          (_connectedToServerName); 
				connInfo.setSrvUser          (_connectedAsUser); 
				connInfo.setSrvCharset       (_connectedSrvCharset); 
				connInfo.setSrvSortorder     (_connectedSrvSortorder); 
				connInfo.setSshTunnelInfo    (connDialog.getAseSshTunnelInfo());
//				connInfo.setClientCharsetId  (_connectedClientCharsetId); 
//				connInfo.setClientCharsetName(_connectedClientCharsetName); 
//				connInfo.setClientCharsetDesc(_connectedClientCharsetDesc); 

//				CheckForUpdates.sendSqlwConnectInfoNoBlock(connInfo);

				// Create a thread that does this...
				// Apparently the noBlockCheckSqlWindow() hits problems when it accesses the CheckForUpdates, which uses ProxyVole
				// My guess is that ProxyVole want's to unpack it's DDL, which takes time...
				Thread checkForUpdatesThread = new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						CheckForUpdates.sendSqlwConnectInfoNoBlock(connInfo);
					}
				}, "checkForUpdatesThread");
				checkForUpdatesThread.setDaemon(true);
				checkForUpdatesThread.start();

			} // end: connectionIsOk
			else
			{
			}
		}
		else if ( connType == ConnectionDialog.OFFLINE_CONN)
		{
			_conn = connDialog.getOfflineConn();
			_connType = ConnectionDialog.OFFLINE_CONN;

			_srvVersion = -1;

			// Code Completion 
			_compleationProviderAbstract = CompletionProviderJdbc.installAutoCompletion(_query_txt, _queryScroll, _queryErrStrip, _window, this);

			// Tooltip supplier
			if (connDialog.isDatabaseProduct(DbUtils.DB_PROD_NAME_H2))
				_tooltipProviderAbstract = new ToolTipSupplierH2(_window, _compleationProviderAbstract, this);
			else
				_tooltipProviderAbstract = new ToolTipSupplierJdbc(_window, _compleationProviderAbstract, this);
			_query_txt.setToolTipSupplier(_tooltipProviderAbstract);

			// JDBC Specific statuses
			_jdbcConnectionStateInfo = DbUtils.getJdbcConnectionStateInfo(_conn);
			_statusBar.setJdbcConnectionStateInfo(_jdbcConnectionStateInfo);

			// Load Windown Props for this server
			loadWinPropsForSrv(_conn.toString());

			// Send connection info
			final SqlwConnectInfo connInfo = new CheckForUpdates.SqlwConnectInfo(connType);
			connInfo.setProdName         (_connectedToProductName);
			connInfo.setProdVersionStr   (_connectedToProductVersion);
			connInfo.setJdbcDriverName   (_connectedDriverName);
			connInfo.setJdbcDriverVersion(_connectedDriverVersion);
			connInfo.setJdbcDriver       (connDialog.getOfflineJdbcDriver());
			connInfo.setJdbcUrl          (_connectedWithUrl); 
			connInfo.setSrvVersionInt    (0);
			connInfo.setSrvName          (_connectedToServerName); 
			connInfo.setSrvUser          (_connectedAsUser); 
			connInfo.setSrvCharset       (_connectedSrvCharset); 
			connInfo.setSrvSortorder     (_connectedSrvSortorder); 
//			connInfo.setSshTunnelInfo    (connDialog.getOfflineSshTunnelInfo());
//			connInfo.setClientCharsetId  (_connectedClientCharsetId); 
//			connInfo.setClientCharsetName(_connectedClientCharsetName); 
//			connInfo.setClientCharsetDesc(_connectedClientCharsetDesc); 

			CheckForUpdates.sendSqlwConnectInfoNoBlock(connInfo);

			// Create a thread that does this...
			// Apparently the noBlockCheckSqlWindow() hits problems when it accesses the CheckForUpdates, which uses ProxyVole
			// My guess is that ProxyVole want's to unpack it's DDL, which takes time...
			Thread checkForUpdatesThread = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					CheckForUpdates.sendSqlwConnectInfoNoBlock(connInfo);
				}
			}, "checkForUpdatesThread");
			checkForUpdatesThread.setDaemon(true);
			checkForUpdatesThread.start();

		}
		else if ( connType == ConnectionDialog.JDBC_CONN)
		{
			_conn = connDialog.getJdbcConn();
			_connType = ConnectionDialog.JDBC_CONN;

			_srvVersion = -1;

			//---------------------------------------------------
			// Get specific stuff for various products
			
			// H2
			if (DbUtils.isProductName(_connectedToProductName, DbUtils.DB_PROD_NAME_H2))
			{
				// Code Completion 
				_compleationProviderAbstract = CompletionProviderJdbc.installAutoCompletion(_query_txt, _queryScroll, _queryErrStrip, _window, this);

				// Tooltip supplier
				_tooltipProviderAbstract = new ToolTipSupplierH2(_window, _compleationProviderAbstract, this);
				_query_txt.setToolTipSupplier(_tooltipProviderAbstract);
			}
			// HANA
			else if (DbUtils.isProductName(_connectedToProductName, DbUtils.DB_PROD_NAME_HANA))
			{
				// Code Completion 
				_compleationProviderAbstract = CompletionProviderJdbc.installAutoCompletion(_query_txt, _queryScroll, _queryErrStrip, _window, this);

				// Sortorder & charset
				_connectedSrvCharset   = "UTF8";
				_connectedSrvSortorder = "BINARY";

				// Tooltip supplier
				_tooltipProviderAbstract = new ToolTipSupplierHana(_window, _compleationProviderAbstract, this);
				_query_txt.setToolTipSupplier(_tooltipProviderAbstract);
			}
			// MaxDB
			else if (DbUtils.isProductName(_connectedToProductName, DbUtils.DB_PROD_NAME_MAXDB))
			{
				// Code Completion 
				_compleationProviderAbstract = CompletionProviderJdbc.installAutoCompletion(_query_txt, _queryScroll, _queryErrStrip, _window, this);

				// Sortorder & charset
				_connectedSrvCharset   = DbUtils.getMaxDbCharset(_conn);
				_connectedSrvSortorder = DbUtils.getMaxDbSortorder(_conn);

				// Tooltip supplier
				_tooltipProviderAbstract = new ToolTipSupplierHana(_window, _compleationProviderAbstract, this);
				_query_txt.setToolTipSupplier(_tooltipProviderAbstract);
			}
			// ORACLE
			else if (DbUtils.isProductName(_connectedToProductName, DbUtils.DB_PROD_NAME_ORACLE))
			{
				// Code Completion 
				_compleationProviderAbstract = CompletionProviderJdbc.installAutoCompletion(_query_txt, _queryScroll, _queryErrStrip, _window, this);

				// Sortorder & charset
				_connectedSrvCharset   = DbUtils.getOracleCharset(_conn);
				_connectedSrvSortorder = DbUtils.getOracleSortorder(_conn);

				// Tooltip supplier
				_tooltipProviderAbstract = new ToolTipSupplierOracle(_window, _compleationProviderAbstract, this);
				_query_txt.setToolTipSupplier(_tooltipProviderAbstract);
			}
			// MS-SQL
			else if (DbUtils.isProductName(_connectedToProductName, DbUtils.DB_PROD_NAME_MSSQL))
			{
				// Code Completion 
				_compleationProviderAbstract = CompletionProviderJdbc.installAutoCompletion(_query_txt, _queryScroll, _queryErrStrip, _window, this);

				// Sortorder & charset
				//_connectedSrvCharset   = DbUtils.getMsSqlCharset(_conn);
				//_connectedSrvSortorder = DbUtils.getMsSqlSortorder(_conn);

				// Tooltip supplier
				_tooltipProviderAbstract = new ToolTipSupplierMsSql(_window, _compleationProviderAbstract, this);
				_query_txt.setToolTipSupplier(_tooltipProviderAbstract);
			}
			// DB2
			else if (DbUtils.isProductName(_connectedToProductName, DbUtils.DB_PROD_NAME_DB2_UX))
			{
				// Code Completion 
				_compleationProviderAbstract = CompletionProviderJdbc.installAutoCompletion(_query_txt, _queryScroll, _queryErrStrip, _window, this);

				// Sortorder & charset
				_connectedSrvCharset   = DbUtils.getDb2Charset(_conn);
				_connectedSrvSortorder = DbUtils.getDb2Sortorder(_conn);

				// Tooltip supplier
				_tooltipProviderAbstract = new ToolTipSupplierOracle(_window, _compleationProviderAbstract, this);
				_query_txt.setToolTipSupplier(_tooltipProviderAbstract);
			}
			else
			{
				// Code Completion 
				_compleationProviderAbstract = CompletionProviderJdbc.installAutoCompletion(_query_txt, _queryScroll, _queryErrStrip, _window, this);

				// Tooltip supplier
				_tooltipProviderAbstract = new ToolTipSupplierJdbc(_window, _compleationProviderAbstract, this);
				_query_txt.setToolTipSupplier(_tooltipProviderAbstract);
			}
			

			
			//---------------------------------------------------
			// Connection STATE information
			//---------------------------------------------------
			if (DbUtils.isProductName(_connectedToProductName, DbUtils.DB_PROD_NAME_MSSQL))
			{
				// Connection info status: USE ASE stuff...
				setDbNames();

				// Also get "various statuses" like if we are in a transaction or not
				_aseConnectionStateInfo = AseConnectionUtils.getAseConnectionStateInfo(_conn, false);
				_statusBar.setAseConnectionStateInfo(_aseConnectionStateInfo);

			}
			else
			{
				//---------------------------------------------------
				// JDBC Specific status - refreshed after each execution
				_jdbcConnectionStateInfo = DbUtils.getJdbcConnectionStateInfo(_conn);
				_statusBar.setJdbcConnectionStateInfo(_jdbcConnectionStateInfo);
			}

			

			// Load Windown Props for this server
			loadWinPropsForSrv(_connectedWithUrl);

			// Send connection info
			final SqlwConnectInfo connInfo = new CheckForUpdates.SqlwConnectInfo(connType);
			connInfo.setProdName         (_connectedToProductName);
			connInfo.setProdVersionStr   (_connectedToProductVersion);
			connInfo.setJdbcDriverName   (_connectedDriverName);
			connInfo.setJdbcDriverVersion(_connectedDriverVersion);
			connInfo.setJdbcDriver       (connDialog.getJdbcDriver());
			connInfo.setJdbcUrl          (_connectedWithUrl); 
			connInfo.setSrvVersionInt    (0);
			connInfo.setSrvName          (_connectedToServerName); 
			connInfo.setSrvUser          (_connectedAsUser); 
			connInfo.setSrvCharset       (_connectedSrvCharset); 
			connInfo.setSrvSortorder     (_connectedSrvSortorder); 
//			connInfo.setSshTunnelInfo    (connDialog.getJdbcSshTunnelInfo());
//			connInfo.setClientCharsetId  (_connectedClientCharsetId); 
//			connInfo.setClientCharsetName(_connectedClientCharsetName); 
//			connInfo.setClientCharsetDesc(_connectedClientCharsetDesc); 

			CheckForUpdates.sendSqlwConnectInfoNoBlock(connInfo);

			// Create a thread that does this...
			// Apparently the noBlockCheckSqlWindow() hits problems when it accesses the CheckForUpdates, which uses ProxyVole
			// My guess is that ProxyVole want's to unpack it's DDL, which takes time...
			Thread checkForUpdatesThread = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					CheckForUpdates.sendSqlwConnectInfoNoBlock(connInfo);
				}
			}, "checkForUpdatesThread");
			checkForUpdatesThread.setDaemon(true);
			checkForUpdatesThread.start();
		}
		
		// What Components should be enabled/visible
		setComponentVisibility();

		// Finally update the watermark
		setWatermark();
	}
	private void setComponentVisibility()
	{
		// Do we want to show the _rsInTab or not
		//_rsInTabs.setVisible(false);

		// Set all to invisible, later set the ones that should be visible to true
		_ase_viewConfig_mi     .setVisible(false);
		_rs_configChangedDdl_mi.setVisible(false);
		_rs_configAllDdl_mi    .setVisible(false);
		_cmdSql_but            .setVisible(false);
		_cmdRcl_but            .setVisible(false);
		_rs_dumpQueue_mi       .setVisible(false);
		_rsWhoIsDown_mi        .setVisible(false);
		_aseCaptureSql_mi      .setVisible(false);

		// view and tools menu might be empty...
		// if so hide the main menu entry as well
		SwingUtils.hideMenuIfNoneIsVisible(_view_m);
		SwingUtils.hideMenuIfNoneIsVisible(_tools_m);

		// sets Save, SaveAs to enabled or not
		caretUpdate(null);

		_dbnames_cbx.setVisible(true);
		
//		if (_conn == null)
		if ( ! AseConnectionUtils.isConnectionOk(_conn, false, null) )
		{
			_connect_mi                .setEnabled(true);
			_connect_but               .setEnabled(true);
			_disconnect_mi             .setEnabled(false);
			_disconnect_but            .setEnabled(false);
			_cloneConnect_mi           .setEnabled(false);
			
			_viewLogFile_but           .setEnabled(false);
			_viewLogFile_mi            .setEnabled(false);

			_dbnames_cbx               .setEnabled(false);
			_exec_but                  .setEnabled(false);
			_rsInTabs_chk              .setEnabled(false);
			_asPlainText_chk           .setEnabled(false);
			_showRowCount_chk          .setEnabled(false);
			_limitRsRowsRead_chk       .setEnabled(false);
			_limitRsRowsReadDialog_mi  .setEnabled(false);
			_showSentSql_chk           .setEnabled(false);
			_printRsInfo_chk           .setEnabled(false);
			_rsTrimStrings_chk         .setEnabled(false);
			_rsShowRowNumber_chk       .setEnabled(false);
			_clientTiming_chk          .setEnabled(false);
			_useSemicolonHack_chk      .setEnabled(false);
			_enableDbmsOutput_chk      .setEnabled(false);
			_appendResults_chk         .setEnabled(false);
			_getObjectTextOnError_chk  .setEnabled(false);
			_jdbcAutoCommit_chk        .setEnabled(false);
			_setAseOptions_but         .setEnabled(false);
			_execGuiShowplan_but       .setEnabled(false);
			
			setSrvInTitle(NOT_CONNECTED_STR);
			_statusBar.setNotConnected();

			return;
		}
		else
		{
			_connect_mi     .setEnabled(false);
			_connect_but    .setEnabled(false);
			_disconnect_mi  .setEnabled(true);
			_disconnect_but .setEnabled(true);
			_cloneConnect_mi.setEnabled(true);

			_viewLogFile_but.setEnabled(true);
			_viewLogFile_mi .setEnabled(true);
		}

		if ( _connType == ConnectionDialog.TDS_CONN)
		{
			// Set server name in windows - title
			String aseSrv      = AseConnectionFactory.getServer();
			String aseHostPort = AseConnectionFactory.getHostPortStr();
			String srvStr      = aseSrv != null ? aseSrv : aseHostPort; 

			setSrvInTitle(srvStr);
			ServerInfo srvInfo = new ServerInfo(srvStr, _connectedToProductName, _connectedToProductVersion, _connectedToServerName, _connectedAsUser, _connectedWithUrl, _connectedToSysListeners, _connectedSrvCharset, _connectedSrvSortorder, _connectedClientCharsetId, _connectedClientCharsetName, _connectedClientCharsetDesc);
			_statusBar.setServerInfo(srvInfo);
			
			if (_connectedToProductName != null && _connectedToProductName.equals(DbUtils.DB_PROD_NAME_SYBASE_ASE))
			{
				_ase_viewConfig_mi         .setVisible(true);// _ase_viewConfig_mi.setEnabled(true);
				_cmdSql_but                .setVisible(true);
				_aseCaptureSql_mi          .setVisible(true);

				_dbnames_cbx               .setEnabled(true);
				_exec_but                  .setEnabled(true);
				_rsInTabs_chk              .setEnabled(true);
				_asPlainText_chk           .setEnabled(true);
				_showRowCount_chk          .setEnabled(true);
				_limitRsRowsRead_chk       .setEnabled(true);
				_limitRsRowsReadDialog_mi  .setEnabled(true);
				_showSentSql_chk           .setEnabled(true);
				_printRsInfo_chk           .setEnabled(true);
				_rsTrimStrings_chk         .setEnabled(true);
				_rsShowRowNumber_chk       .setEnabled(true);
				_clientTiming_chk          .setEnabled(true);
				_useSemicolonHack_chk      .setEnabled(true);
				_enableDbmsOutput_chk      .setEnabled(true);
				_appendResults_chk         .setEnabled(true);
				_getObjectTextOnError_chk  .setEnabled(true);
				_jdbcAutoCommit_chk        .setEnabled(true);
				_setAseOptions_but         .setEnabled(true);
//				_execGuiShowplan_but       .setEnabled( (_aseVersion >= 15000) );
//				_execGuiShowplan_but       .setEnabled( (_srvVersion >= 1500000) );
				_execGuiShowplan_but       .setEnabled( (_srvVersion >= Ver.ver(15,0)) );
			}
			else if (_connectedToProductName != null && _connectedToProductName.equals(DbUtils.DB_PROD_NAME_SYBASE_RS))
			{
				_rs_configChangedDdl_mi    .setVisible(true);
				_rs_configAllDdl_mi        .setVisible(true);
				_cmdRcl_but                .setVisible(true);
				_rs_dumpQueue_mi           .setVisible(true);
				_rsWhoIsDown_but           .setVisible(true);
				_rsWhoIsDown_mi            .setVisible(true);

				_dbnames_cbx               .setVisible(false);
				_exec_but                  .setEnabled(true);
				_rsInTabs_chk              .setEnabled(true);
				_asPlainText_chk           .setEnabled(true);
				_showRowCount_chk          .setEnabled(true);
				_limitRsRowsRead_chk       .setEnabled(true);
				_limitRsRowsReadDialog_mi  .setEnabled(true);
				_showSentSql_chk           .setEnabled(true);
				_printRsInfo_chk           .setEnabled(true);
				_rsTrimStrings_chk         .setEnabled(true);
				_rsShowRowNumber_chk       .setEnabled(true);
				_clientTiming_chk          .setEnabled(true);
				_useSemicolonHack_chk      .setEnabled(true);
				_enableDbmsOutput_chk      .setEnabled(true);
				_appendResults_chk         .setEnabled(true);
				_getObjectTextOnError_chk  .setEnabled(true);
				_jdbcAutoCommit_chk        .setEnabled(true);
				_setAseOptions_but         .setEnabled(false);
				_execGuiShowplan_but       .setEnabled(false);
			}
			else // Probably IQ, SQL Anywhere or some other TDS implementation like OpenServer or jTDS
			{
//				_logger.info("Connected to the Sybase TDS service with product name '"+_connectedToProductName+"', only esential functionality is enabled.");

				_dbnames_cbx               .setVisible(false);
				_exec_but                  .setEnabled(true);
				_rsInTabs_chk              .setEnabled(true);
				_asPlainText_chk           .setEnabled(true);
				_showRowCount_chk          .setEnabled(true);
				_limitRsRowsRead_chk       .setEnabled(true);
				_limitRsRowsReadDialog_mi  .setEnabled(true);
				_showSentSql_chk           .setEnabled(true);
				_printRsInfo_chk           .setEnabled(true);
				_rsTrimStrings_chk         .setEnabled(true);
				_rsShowRowNumber_chk       .setEnabled(true);
				_clientTiming_chk          .setEnabled(true);
				_useSemicolonHack_chk      .setEnabled(true);
				_enableDbmsOutput_chk      .setEnabled(true);
				_appendResults_chk         .setEnabled(true);
				_getObjectTextOnError_chk  .setEnabled(true);
				_jdbcAutoCommit_chk        .setEnabled(true);
				_setAseOptions_but         .setEnabled(false);
				_execGuiShowplan_but       .setEnabled(false);
			}
		}

		if ( _connType == ConnectionDialog.OFFLINE_CONN)
		{
			_dbnames_cbx               .setEnabled(false);
			_exec_but                  .setEnabled(true);
			_rsInTabs_chk              .setEnabled(true);
			_asPlainText_chk           .setEnabled(true);
			_showRowCount_chk          .setEnabled(true);
			_limitRsRowsRead_chk       .setEnabled(true);
			_limitRsRowsReadDialog_mi  .setEnabled(true);
			_showSentSql_chk           .setEnabled(true);
			_printRsInfo_chk           .setEnabled(true);
			_rsTrimStrings_chk         .setEnabled(true);
			_rsShowRowNumber_chk       .setEnabled(true);
			_clientTiming_chk          .setEnabled(true);
			_useSemicolonHack_chk      .setEnabled(true);
			_enableDbmsOutput_chk      .setEnabled(true);
			_appendResults_chk         .setEnabled(true);
			_getObjectTextOnError_chk  .setEnabled(true);
			_jdbcAutoCommit_chk        .setEnabled(true);
			_setAseOptions_but         .setEnabled(false);
			_execGuiShowplan_but       .setEnabled(false);

			setSrvInTitle(_conn.toString());
			ServerInfo srvInfo = new ServerInfo(_conn.toString(), _connectedToProductName, _connectedToProductVersion, _connectedToServerName, _connectedAsUser, _connectedWithUrl, _connectedToSysListeners, _connectedSrvCharset, _connectedSrvSortorder, _connectedClientCharsetId, _connectedClientCharsetName, _connectedClientCharsetDesc);
			_statusBar.setServerInfo(srvInfo);
		}

		if ( _connType == ConnectionDialog.JDBC_CONN)
		{
			_dbnames_cbx               .setEnabled(false);
			_exec_but                  .setEnabled(true);
			_rsInTabs_chk              .setEnabled(true);
			_asPlainText_chk           .setEnabled(true);
			_showRowCount_chk          .setEnabled(true);
			_limitRsRowsRead_chk       .setEnabled(true);
			_limitRsRowsReadDialog_mi  .setEnabled(true);
			_showSentSql_chk           .setEnabled(true);
			_printRsInfo_chk           .setEnabled(true);
			_rsTrimStrings_chk         .setEnabled(true);
			_rsShowRowNumber_chk       .setEnabled(true);
			_clientTiming_chk          .setEnabled(true);
			_useSemicolonHack_chk      .setEnabled(true);
			_enableDbmsOutput_chk      .setEnabled(true);
			_appendResults_chk         .setEnabled(true);
			_getObjectTextOnError_chk  .setEnabled(true);
			_jdbcAutoCommit_chk        .setEnabled(true);
			_setAseOptions_but         .setEnabled(false);
			_execGuiShowplan_but       .setEnabled(false);

			setSrvInTitle(_connectedWithUrl);
			ServerInfo srvInfo = new ServerInfo(_connectedWithUrl, _connectedToProductName, _connectedToProductVersion, _connectedToServerName, _connectedAsUser, _connectedWithUrl, _connectedToSysListeners, _connectedSrvCharset, _connectedSrvSortorder, _connectedClientCharsetId, _connectedClientCharsetName, _connectedClientCharsetDesc);
			_statusBar.setServerInfo(srvInfo);
		}
		
		if (DbUtils.isProductName(_connectedToProductName, DbUtils.DB_PROD_NAME_ORACLE, DbUtils.DB_PROD_NAME_DB2_UX))
		{
			_enableDbmsOutput_chk.setVisible(true);
		}
		else
		{
			_enableDbmsOutput_chk.setVisible(false);
		}

		// MS SQL
		if (DbUtils.isProductName(_connectedToProductName, DbUtils.DB_PROD_NAME_MSSQL))
		{
			_dbnames_cbx               .setEnabled(true);
		}

		// view and tools menu might be empty...
		// if so hide the main menu entry as well
		SwingUtils.hideMenuIfNoneIsVisible(_view_m);
		SwingUtils.hideMenuIfNoneIsVisible(_tools_m);
	}

	private void action_disconnect(ActionEvent e)
	{
		saveWinProps();
		sendExecStatistics(false);

		_srvVersion                 = 0;
		_connectedAtTime            = 0;
		_connectedDriverName        = null;
		_connectedDriverVersion     = null;
		_connectedToProductName     = null;
		_connectedToProductVersion  = null;
		_connectedToServerName      = null;
		_connectedAsUser            = null;
		_connectedWithUrl           = null;
		_connectedToSysListeners    = null;
		_connectedSrvCharset        = null;
		_connectedSrvSortorder      = null;
		_connectedClientCharsetId   = null;
		_connectedClientCharsetName = null;
		_connectedClientCharsetDesc = null;

		_currentDbName              = null;
		_dbmsOutputIsEnabled        = false; // Mark this as false, it will be marked as true when executing...

		if (_conn != null)
		{
			try
			{
				_rsWhoIsDown_but           .setVisible(false);
				_viewLogFile_but           .setEnabled(false);
				_viewLogFile_mi            .setEnabled(false);

				_dbnames_cbx               .setEnabled(false);
				_exec_but                  .setEnabled(false);
				_rsInTabs_chk              .setEnabled(false);
				_asPlainText_chk           .setEnabled(false);
				_showRowCount_chk          .setEnabled(false);
				_limitRsRowsRead_chk       .setEnabled(false);
				_limitRsRowsReadDialog_mi  .setEnabled(false);
				_showSentSql_chk           .setEnabled(false);
				_printRsInfo_chk           .setEnabled(false);
				_rsTrimStrings_chk         .setEnabled(false);
				_rsShowRowNumber_chk       .setEnabled(false);
				_clientTiming_chk          .setEnabled(false);
				_useSemicolonHack_chk      .setEnabled(false);
				_enableDbmsOutput_chk      .setEnabled(false);
				_appendResults_chk         .setEnabled(false);
				_getObjectTextOnError_chk  .setEnabled(false);
				_jdbcAutoCommit_chk        .setEnabled(false);
				_setAseOptions_but         .setEnabled(false);
				_execGuiShowplan_but       .setEnabled(false);

				setSrvInTitle(null);
				_statusBar.setNotConnected();

				// close the connection "at the end" of the method, if it causes an exception
				Connection tmpConn = _conn;
				_conn = null;
				_connType = -1;

				tmpConn.close();
			}
			catch (SQLException ex)
			{
				_logger.error("Problems closing database connection.", ex);
			}
		}
	}

	private void action_splitPaneToggle(ActionEvent e)
	{
		Object source = e.getSource();
		
		if (_splitPane_chk.equals(source))          _prefSplitHorizontal_mi.setSelected(_splitPane_chk.isSelected());
		if (_prefSplitHorizontal_mi.equals(source)) _splitPane_chk.setSelected(_prefSplitHorizontal_mi.isSelected());
			
		saveProps();
		reLayoutSomeStuff(_prefSplitHorizontal_mi.isSelected(), _prefPlaceCntrlInToolbar_mi.isSelected());
	}

	private void action_cloneConnect(ActionEvent e)
	{
		// Get environemnt
		final String SQLW_HOME = System.getProperty("SQLW_HOME");

		// Create base command
		String baseCmd = "sqlw.bat";
		if (PlatformUtils.getCurrentPlattform() != PlatformUtils.Platform_WIN)
			baseCmd = "./sqlw.sh";

		// Create command line parameters
		ArrayList<String> cmdLineParams = new ArrayList<String>();

		cmdLineParams.add(baseCmd);
		cmdLineParams.add("-U" + _connectedAsUser);
		cmdLineParams.add("-S" + _connectedToServerName);
//		cmdLineParams.add("--cloneStart");

		if (_dbnames_cbx.isVisible() && _dbnames_cbx.getItemCount() > 0)
		{
			String dbname = (String) _dbnames_cbx.getSelectedItem();
			cmdLineParams.add("-D" + dbname);
		}

		_logger.debug("-----BEGIN-CLONE-CONNECT-----------------------------------------------");
		_logger.debug("OS Start Directory: " + SQLW_HOME );
		_logger.debug("OS Command Array:   " + cmdLineParams);
		_logger.debug("-----END---CLONE-CONNECT-----------------------------------------------");

		System.out.println("-----BEGIN-CLONE-CONNECT-----------------------------------------------");
		System.out.println("OS Start Directory: " + SQLW_HOME );
		System.out.println("OS Command Array:   " + cmdLineParams);
		System.out.println("-----END---CLONE-CONNECT-----------------------------------------------");

		// Now execute a OS Command
		try
		{
//			Runtime.getRuntime().exec(osCmd);

			int winPosX = _window.getLocationOnScreen().x;
			int winPosY = _window.getLocationOnScreen().y;
			String propPropEntryStr = "WinProps={x="+winPosX+", y="+winPosY+"}"; // see PropPropEntry

			ProcessBuilder pb = new ProcessBuilder(cmdLineParams);
			Map<String, String> env = pb.environment();
			env.put("SQLW_CLONE_CONNECT_PROPS", propPropEntryStr);
			pb.directory(new File(SQLW_HOME));
			pb.redirectErrorStream(true);
			Process p = pb.start();

			// If we don't read the stream(s) from Process, it may simply not "start" or start slowly
			final InputStream stdOut = p.getInputStream();
			Thread stdInOutReaderThread = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						byte[] buffer = new byte[8192];
						int len = -1;
						while((len = stdOut.read(buffer)) > 0)
//							; // just throw the buffer away, so we don't block on outputs...
							System.out.write(buffer, 0, len);
					}
					catch (IOException ignore) {}
				}
			});
			stdInOutReaderThread.setDaemon(true);
			stdInOutReaderThread.setName("cloneConnect:stdInOutReader");
			stdInOutReaderThread.start();
			
//			String[] strArr = new String[cmdLineParams.size()];
//			int i=0;
//			for (String str : cmdLineParams)
//				strArr[i++] = str;
//				
//			Runtime.getRuntime().exec(strArr, new String[] {"SQLW_CLONE_CONNECT_PROPS="+LOCAL_JVM}, new File(SQLW_HOME));
		}
		catch (IOException ex)
		{
			SwingUtils.showErrorMessage(_window, "Problems starting new "+Version.getAppName(), 
				"<html>" +
				"<h2>Problems starting a new "+Version.getAppName()+"</h2>" +
				"OS Start Directory: " + SQLW_HOME + "<br>" +
				"OS Command Array: " + cmdLineParams +  "<br>" +
				"</html>", ex);
		}
		// maybe use:      http://docs.oracle.com/javase/6/docs/api/java/lang/ProcessBuilder.html
		// or fallback to: http://docs.oracle.com/javase/6/docs/api/java/lang/Runtime.html#exec%28java.lang.String[]%29
	}


	private void action_fileNew(ActionEvent e)
	{
//System.out.println("action_fileNew");

		boolean tryToSaveFile = false;
		if (_query_txt.isDirty())
		{
			tryToSaveFile = true;

			File f = new File(_query_txt.getFileFullPath());
			
			Object[] buttons = {"Save File", "Save As new file", "Discard changes", "Cancel"};
			int answer = JOptionPane.showOptionDialog(_window, 
					"The File '"+f+"' has not been saved.",
					"Save file?", 
					JOptionPane.DEFAULT_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					null,
					buttons,
					f.exists() ? buttons[0] : buttons[1]);
			// Save
			if (answer == 0) 
			{
				action_fileSave(e);
				tryToSaveFile = true;
			}
			// Save As
			else if (answer == 1) 
			{
				action_fileSaveAs(e);
				tryToSaveFile = true;
			}
			// CANCEL, continue to work on current file
			else if (answer == 3) 
			{
				return;  // <<<<<<<---------- RETURN ---------------
			}
			// Discard
			else
			{
				tryToSaveFile = false;
			}
		}

		if (tryToSaveFile && _query_txt.isDirty())
		{
			SwingUtils.showInfoMessage("New File", "Sorry the file couldn't be saved. Can't continue open another file.");
		}


		SwingUtils.showInfoMessage("New File", "Sorry not fully implemented yet.");

		// hmm do I need to do...
//		_query_txt = new TextEditorPane();
//		...and then install all the "stuff" on _query_txt
//		or probably a new method that does it all..._query_txt like...
//		public TextEditorPane createNewEditor()
//		{
//			TextEditorPane text = new TextEditorPane();
//			_tabs.add(text);
//			_editorPanes.add(text);
//			return text;
//		}
		
//		// Set as NEW FILE
//		try
//		{
//			FileLocation loc = FileLocation.create(file);
//			_query_txt.load(loc, null);
//			_statusBar.setFilename(_query_txt.getFileFullPath());
//			_statusBar.setFilenameDirty(_query_txt.isDirty());
//		}
//		catch (IOException ex)
//		{
//			SwingUtils.showErrorMessage("Problems Loading file", "Problems Loading file", ex);
//		}
	}

	private void action_fileOpen(ActionEvent e, String fileToOpen)
	{
//System.out.println("action_fileOpen");

		boolean tryToSaveFile = false;
		if (_query_txt.isDirty())
		{
			tryToSaveFile = true;

			File f = new File(_query_txt.getFileFullPath());
			
			Object[] buttons = {"Save File", "Save As new file", "Discard changes", "Cancel"};
			int answer = JOptionPane.showOptionDialog(_window, 
					"The File '"+f+"' has not been saved.",
					"Save file?", 
					JOptionPane.DEFAULT_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					null,
					buttons,
					f.exists() ? buttons[0] : buttons[1]);
			// Save
			if (answer == 0) 
			{
				action_fileSave(e);
				tryToSaveFile = true;
			}
			// Save As
			else if (answer == 1) 
			{
				action_fileSaveAs(e);
				tryToSaveFile = true;
			}
			// CANCEL, continue to work on current file
			else if (answer == 3) 
			{
				return;  // <<<<<<<---------- RETURN ---------------
			}
			// Discard
			else
			{
				tryToSaveFile = false;
			}
		}

		if (tryToSaveFile && _query_txt.isDirty())
		{
			SwingUtils.showInfoMessage("Open File", "Sorry the file couldn't be saved. Can't continue open another file.");
		}


		// Open a file chooser
		if (fileToOpen == null)
		{
			JFileChooser fc = new JFileChooser(getDefaultFilePath());
			fc.setDialogType(JFileChooser.OPEN_DIALOG);
	
			int returnVal = fc.showOpenDialog(_window);
			if (returnVal == JFileChooser.APPROVE_OPTION) 
			{
				openFile(fc.getSelectedFile());
			}
		}
		else
		{
			openFile(fileToOpen);
		}
	}

	private void checkOpenedFileType()
	{
		String filename = _query_txt.getFileFullPath();
		
		// make special this here...
		if (filename != null && filename.endsWith("_tooltip_provider.xml"))
		{
			// install a special ToolTipProvider
			_tooltipProviderAbstract = new ToolTipSupplierTester(_window, this);
			_query_txt.setToolTipSupplier(_tooltipProviderAbstract);

			// Install XML Syntax Higligtning and CodeCompletion for ToolTipProviderEntry
			TtpEntryCompletionProvider.installAutoCompletion(_query_txt);
		}
	}

	private void openFile(String filename)
	{
		openFile( new File(filename) );
	}
	private void openFile(File file)
	{
		try
		{
			FileLocation loc = FileLocation.create(file);
			
			_query_txt.load(loc, FileUtils.getFileEncoding(file));
			_statusBar.setFilename(_query_txt.getFileFullPath(), _query_txt.getEncoding());
			_statusBar.setFilenameDirty(_query_txt.isDirty());
			checkOpenedFileType();
			
			addFileHistory(file);
			
			if (_watchdogIsFileChanged != null)
				_watchdogIsFileChanged.setFile(_query_txt.getFileFullPath());
			
			// reset the UNDO manager
			_query_txt.discardAllEdits();
		}
		catch (IOException ex)
		{
			SwingUtils.showErrorMessage("Problems Loading file", "Problems Loading file", ex);
		}
	}

	private void saveFile()
	{
		try
		{
			_query_txt.save();
			_statusBar.setFilename(_query_txt.getFileFullPath(), _query_txt.getEncoding());
			_statusBar.setFilenameDirty(_query_txt.isDirty());

			if (_watchdogIsFileChanged != null)
				_watchdogIsFileChanged.setFile(_query_txt.getFileFullPath());

		}
		catch (IOException ex)
		{
			SwingUtils.showErrorMessage("Problems Saving file", "Problems saving file", ex);
		}
	}

		
//	private void action_fileClose(ActionEvent e)
//	{
//		SwingUtils.showInfoMessage("action_fileClose:Not yet implemented", "action_fileClose:Not yet implemented");
//		System.out.println("action_fileClose");
//		_statusBar.setFilename(_query_txt.getFileFullPath());
//		_statusBar.setFilenameDirty(_query_txt.isDirty());
//	}

	private boolean isFileUntitled()
	{
		String fp = _query_txt.getFileFullPath();
		String fn = _query_txt.getFileName();

		File f = new File(fp);
		if ("Untitled.txt".equals(fn) & !f.exists())
			return true;

		return false;
	}

	private void action_fileSave(ActionEvent e)
	{
//System.out.println("action_fileSave");
		if (isFileUntitled())
		{
			action_fileSaveAs(e);
			return;
		}

		saveFile();
	}


//	public synchronized boolean saveCurrentFileAs() {
//
//		// Ensures text area gets focus after save for saves that don't bring
//		// up an extra window (Save As, etc.).  Without this, the text area
//		// would lose focus.
//		currentTextArea.requestFocusInWindow();
//
//		// Get the new filename they'd like to use.
//		RTextFileChooser chooser = owner.getFileChooser();
//		chooser.setMultiSelectionEnabled(false);	// Disable multiple file selection.
//		File initialSelection = new File(currentTextArea.getFileFullPath());
//		chooser.setSelectedFile(initialSelection);
//		chooser.setOpenedFiles(getOpenFiles());
//		// Set encoding to what it was read-in or last saved as.
//		chooser.setEncoding(currentTextArea.getEncoding());
//
//		int returnVal = chooser.showSaveDialog(owner);
//
//		// If they entered a new filename and clicked "OK", save the flie!
//		if(returnVal == RTextFileChooser.APPROVE_OPTION) {
//
//			File chosenFile = chooser.getSelectedFile();
//			String chosenFileName = chosenFile.getName();
//			String chosenFilePath = chosenFile.getAbsolutePath();
//			String encoding = chooser.getEncoding();
//
//			// If the current file filter has an obvious extension
//			// associated with it, use it if the specified filename has
//			// no extension.  Get the extension from the filter by
//			// checking whether the filter is of the form
//			// "Foobar Files (*.foo)", and it if is, use the ".foo"
//			// extension.
//			String extension = chooser.getFileFilter().getDescription();
//			int leftParen = extension.indexOf("(*");
//			if (leftParen>-1) {
//				int start = leftParen + 2; // Skip "(*".
//				int end = extension.indexOf(')', start);
//				int comma = extension.indexOf(',', start);
//				if (comma>-1 && comma<end)
//					end = comma;
//				if (end>start+1) { // Ensure a ')' or ',' was found.
//					extension = extension.substring(start, end);
//					// If the file name they entered has no extension,
//					// add this extension to it.
//					if (chosenFileName.indexOf('.')==-1) {
//						chosenFileName = chosenFileName + extension;
//						chosenFilePath = chosenFilePath + extension;
//						chosenFile = new File(chosenFilePath);
//					}
//				}
//			}
//
//			// If the file already exists, prompt them to see whether
//			// or not they want to overwrite it.
//			if (chosenFile.exists()) {
//				String temp = owner.getString("FileAlreadyExists",
//										chosenFile.getName());
//				if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(
//						this, temp, owner.getString("ConfDialogTitle"),
//						JOptionPane.YES_NO_OPTION)) {
//					return false;
//				}
//			}
//
//			// If necessary, change the current file's encoding.
//			String oldEncoding = currentTextArea.getEncoding();
//			if (encoding!=null && !encoding.equals(oldEncoding))
//				currentTextArea.setEncoding(encoding);
//
//			// Try to save the file with a new name.
//			return saveCurrentFileAs(FileLocation.create(chosenFilePath));
//
//		} // End of if(returnVal == RTextFileChooser.APPROVE_OPTION).
//
//		// If they cancel the save...
//		return false;
//	}

	/**
	 * If current file working file is under the Application "HOME", open another dir as the default. 
	 */
	private File getDefaultFilePath()
	{
		File currentFilePath = new File(_query_txt.getFileFullPath()).getParentFile();

		// If currentFilePath points to "HOME", then bring up the default save place at $HOME/.asetune/saved_files
		final String SQLW_HOME = System.getProperty("SQLW_HOME", "");
		if (currentFilePath != null && currentFilePath.toString().equals(SQLW_HOME))
		{
			File defaultSaveAsDir = new File(Version.APP_STORE_DIR + File.separator + "saved_files");
			if ( ! defaultSaveAsDir.exists() )
			{
				if (defaultSaveAsDir.mkdir())
					_logger.info("Creating directory '"+defaultSaveAsDir+"' to hold various files for "+Version.getAppName());
			}
			currentFilePath = defaultSaveAsDir;
		}
		
		return currentFilePath;
	}

	private void action_fileSaveAs(ActionEvent e)
	{
		JFileChooser fc = new JFileChooser(getDefaultFilePath());
		fc.setDialogType(JFileChooser.SAVE_DIALOG);
//		fc.setDialogType(JFileChooser.CUSTOM_DIALOG);
//		fc.setApproveButtonText("Save As");
//System.out.println("JFileChooser.CUSTOM_DIALOG: 'Save As'");
		

//		if (System.getProperty("ASETUNE_SAVE_DIR") != null)
//			fc.setCurrentDirectory(new File(System.getProperty("ASETUNE_SAVE_DIR")));

		int returnVal = fc.showDialog(_window, "Save As");
//		int returnVal = fc.showOpenDialog(_window);
		if (returnVal == JFileChooser.APPROVE_OPTION) 
        {
			File file = fc.getSelectedFile();

			//This is where a real application would open the file.
			String filename = file.getAbsolutePath();

			// If the file already exists, prompt them to see whether
			// or not they want to overwrite it.
			if (file.exists()) 
			{
				int answer = JOptionPane.showConfirmDialog(_window, "The File '"+file.getName()+"' Already Exists", "File Already Exists", JOptionPane.YES_NO_OPTION);
				if (answer != JOptionPane.YES_OPTION) 
				{
					return;
				}
			}
			FileLocation loc = FileLocation.create(filename);
			try
			{
				_query_txt.saveAs(loc);
				_statusBar.setFilename(_query_txt.getFileFullPath(), _query_txt.getEncoding());
				_statusBar.setFilenameDirty(_query_txt.isDirty());

				addFileHistory(_query_txt.getFileFullPath());

				if (_watchdogIsFileChanged != null)
					_watchdogIsFileChanged.setFile(_query_txt.getFileFullPath());
			}
			catch (IOException ex)
			{
				SwingUtils.showErrorMessage("Problems Saving file", "Problems saving file", ex);
			}
        }		
	}


	private void action_exit(ActionEvent e)
	{
		_jframe.dispatchEvent(new WindowEvent(_jframe, WindowEvent.WINDOW_CLOSING));
	}


	public static class RclViewer extends JFrame 
	{

		private static final long serialVersionUID = 1L;

		public RclViewer(String rcl) 
		{
			JPanel cp = new JPanel(new BorderLayout());

			RSyntaxTextAreaX textArea = new RSyntaxTextAreaX(40, 100);
//			textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
			textArea.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_RCL);
			textArea.setCodeFoldingEnabled(true);
			textArea.setAntiAliasingEnabled(true);
			RTextScrollPane sp = new RTextScrollPane(textArea);
			sp.setFoldIndicatorEnabled(true);
			cp.add(sp);

			RSyntaxUtilitiesX.installRightClickMenuExtentions(sp, this);

			textArea.setText(rcl);

			setContentPane(cp);
			setTitle("RCL for Replication Server. RS, Connections, Logical Connections, Routes");
			setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			pack();
			setLocationRelativeTo(null);
		}
	}
	
	private void action_viewAseConfig(ActionEvent e)
	{
		WaitForExecDialog wait = new WaitForExecDialog(MainFrame.getInstance(), "Getting ASE Configuration");

		// Kick this of as it's own thread, otherwise the sleep below, might block the Swing Event Dispatcher Thread
		BgExecutor bgExec = new BgExecutor(wait)
		{
			@Override
			public Object doWork()
			{
				getWaitDialog().setState("Getting sp_configure settings");
				AseConfig aseCfg = AseConfig.getInstance();
				if ( ! aseCfg.isInitialized() )
					aseCfg.initialize(getConnection(), true, false, null);

				// initialize ASE Config Text Dictionary
				//AseConfigText.initializeAll(getConnection(), true, false, null);
				for (ConfigType t : ConfigType.values())
				{
					AseConfigText aseConfigText = AseConfigText.getInstance(t);
					if ( ! aseConfigText.isInitialized() )
					{
						getWaitDialog().setState("Getting '"+t+"' settings");
						aseConfigText.initialize(getConnection(), true, false, null);
					}
				}

				getWaitDialog().setState("Done");

				return null;
			}
		};
		wait.execAndWait(bgExec);

		AseConfigViewDialog.showDialog(_window, this);
	}


	private void action_rsGenerateDdl(ActionEvent e, String cmdStr)
	{
		final boolean skipDefaultConfigs = ACTION_RS_GENERATE_ALL_DDL.equals(cmdStr) ? false : true;

		// Create a Waitfor Dialog and Executor, then execute it.
		WaitForExecDialog wait = new WaitForExecDialog(_window, "Reading Replication Server Configuration");

		WaitForExecDialog.BgExecutor doWork = new WaitForExecDialog.BgExecutor(wait)
		{
			@Override
			public Object doWork()
			{
				_logger.info("Generating RCL for Replication Server Configurations");
				String ddl = RepServerUtils.printConfig(_conn, skipDefaultConfigs, getWaitDialog());
				_logger.info("DONE, Generating RCL for Replication Server Configurations");

				return ddl;
			}
		}; // END: new WaitForExecDialog.BgExecutor()
		
		// Execute and WAIT
		String ddl = (String)wait.execAndWait(doWork);

		RclViewer rclViewer = new RclViewer(ddl);
		rclViewer.setVisible(true);
	}


	private void action_rsDumpQueue(ActionEvent e)
	{
		RsDumpQueueDialog dumpQueueDialog = new RsDumpQueueDialog(_conn, WindowType.JFRAME);
		dumpQueueDialog.setVisible(true);

//		// Create a WaitFor Dialog and Executor, then execute it.
//		WaitForExecDialog wait = new WaitForExecDialog(_window, "Reading Replication Server Configuration");
//
//		WaitForExecDialog.BgExecutor doWork = new WaitForExecDialog.BgExecutor(wait)
//		{
//			@Override
//			public Object doWork()
//			{
//				_logger.info("Reading Connection Information");
//				List<String> logConnList = RepServerUtils.getLogicalConnections(_conn);
//
//				return logConnList;
//			}
//		}; // END: new WaitForExecDialog.BgExecutor()
//		
//		// Execute and WAIT
//		wait.execAndWait(doWork);
	}

	private void action_rsWhoIsDown(ActionEvent e)
	{
		displayQueryResults(
			"admin health       \n" +
			"admin who_is_down  \n", 
			0, false);
	}

	private void action_aseCaptureSql(ActionEvent e)
	{
		new ProcessDetailFrame(this, -1, -1);
	}

	private void action_viewCmdHistory(ActionEvent e)
	{
		if (_cmdHistoryDialog == null)
			_cmdHistoryDialog = new CommandHistoryDialog(this, _window);
		_cmdHistoryDialog.setVisible(true);
	}

	private void action_viewLogTail(ActionEvent e)
	{
//		SwingUtils.showInfoMessage("Not yet implemented", "<html><h2>Sorry NOT Yet Implemented.</h2></html>");
		LogTailWindow logTailDialog = new LogTailWindow(_conn);
		logTailDialog.setVisible(true);
		logTailDialog.startTail();
	}

	
	private void action_nextError(ActionEvent e)
	{
		int atQueryLine = _query_txt.getCaretLineNumber() + 1;
		int newQueryLine = -1;
		
		@SuppressWarnings("unchecked")
		ArrayList<JAseMessage> errorInfo = (ArrayList<JAseMessage>) _query_txt.getDocument().getProperty(ParserProperties.DB_MESSAGES);
		if (errorInfo == null)
			return;

		for (JAseMessage msg : errorInfo)
		{
			if (atQueryLine >= msg._scriptRow)
				continue;
			newQueryLine = msg._scriptRow;
			break;
		}

		if (newQueryLine > 0)
		{
			try { _query_txt.setCaretPosition(_query_txt.getLineStartOffset(newQueryLine - 1)); }
			catch (BadLocationException ble) { UIManager.getLookAndFeel().provideErrorFeedback(_query_txt);	}

			RXTextUtilities.possiblyMoveLineInScrollPane(_query_txt);
			
			// FIXME: find the component in the output window and make it visible (scroll to rect)
			for (int i=0; i<_resPanel.getComponentCount(); i++)
			{
				Component comp = _resPanel.getComponent(i);
				if (comp instanceof JAseMessage)
				{
					JAseMessage msg = (JAseMessage)comp;
					if (msg._scriptRow == newQueryLine)
					{
						msg.setBackground(DEFAULT_OUTPUT_ERROR_HIGHLIGHT_COLOR);
						msg.setSelectionStart(0);
						msg.setSelectionEnd(msg.getText().length());
						//break;
					}
					else
						msg.setBackground(Color.WHITE); //FIXME: Maybe not hardcode this color, get it from UI
				}
			}
		}
		else
		{
			UIManager.getLookAndFeel().provideErrorFeedback(_query_txt);
		}
	}

	private void action_prevError(ActionEvent e)
	{
		int atQueryLine = _query_txt.getCaretLineNumber() + 1;
		int newQueryLine = -1;
		
		@SuppressWarnings("unchecked")
		ArrayList<JAseMessage> errorInfo = (ArrayList<JAseMessage>) _query_txt.getDocument().getProperty(ParserProperties.DB_MESSAGES);
		if (errorInfo == null)
			return;

		for (JAseMessage msg : errorInfo)
		{
			if (msg._scriptRow < atQueryLine)
			{
				newQueryLine = msg._scriptRow;
				continue;
			}
			break;
		}
		if (atQueryLine < newQueryLine)
			newQueryLine = -1;

		if (newQueryLine > 0)
		{
			try { _query_txt.setCaretPosition(_query_txt.getLineStartOffset(newQueryLine - 1)); }
			catch (BadLocationException ble) { UIManager.getLookAndFeel().provideErrorFeedback(_query_txt); }

			RXTextUtilities.possiblyMoveLineInScrollPane(_query_txt);

			// FIXME: find the component in the output window and make it visible (scroll to rect)
			for (int i=0; i<_resPanel.getComponentCount(); i++)
			{
				Component comp = _resPanel.getComponent(i);
				if (comp instanceof JAseMessage)
				{
					JAseMessage msg = (JAseMessage)comp;
					if (msg._scriptRow == newQueryLine)
					{
						msg.setBackground(DEFAULT_OUTPUT_ERROR_HIGHLIGHT_COLOR);
						msg.setSelectionStart(0);
						msg.setSelectionEnd(msg.getText().length());
						//break;
					}
					else
						msg.setBackground(Color.WHITE); //FIXME: Maybe not hardcode this color, get it from UI
				}
			}
		}
		else
		{
			UIManager.getLookAndFeel().provideErrorFeedback(_query_txt);
		}
	}


	//////////////////////////////////////////////////////////////
	// BEGIN: IMPLEMEMNTS the CommandHistoryDialog HistoryExecutor
	//////////////////////////////////////////////////////////////
	@Override
	public void historyExecute(String cmd)
	{
		displayQueryResults(cmd, 0, false);
	}
	@Override
	public void saveHistoryFilename(String filename)
	{
		_cmdHistoryFilename = filename;
		saveProps();
	}

	@Override
	public String getHistoryFilename()
	{
		return _cmdHistoryFilename;
	}
	
	@Override
	public String getSourceId() 
	{
		return LOCAL_JVM;
	}
	//////////////////////////////////////////////////////////////
	// END: IMPLEMEMNTS the CommandHistoryDialog HistoryExecutor
	//////////////////////////////////////////////////////////////
	
	private void actionExecute(ActionEvent e, boolean guiShowplanExec)
	{
		// If we had an JTabbedPane, what was the last index
		_lastTabIndex = -1;
		for (int i=0; i<_resPanel.getComponentCount(); i++)
		{
			Component comp = (Component) _resPanel.getComponent(i);
			if (comp instanceof JTabbedPane)
			{
				JTabbedPane tp = (JTabbedPane) comp;
				_lastTabIndex = tp.getSelectedIndex();
				_logger.trace("Save last tab index pos as "+_lastTabIndex+", tp="+tp);
			}
		}

		// is "save before execute" enabled, save file
		if ( _query_txt.isDirty() && _fSaveBeforeExec_mi.isSelected() )
		{
			// Only save if we have an *assigned* and valid file
			if ( ! isFileUntitled() )
				saveFile();
		}

		// Get the user's query and pass to displayQueryResults()
		String q = _query_txt.getSelectedText();
		if ( q != null && !q.equals(""))
		{
			// Get the line number where the selection started
			int selectedTextStartAtRow = 0;
			try { selectedTextStartAtRow = _query_txt.getLineOfOffset(_query_txt.getSelectionStart()); }
			catch (BadLocationException ignore) {}

			displayQueryResults(q, selectedTextStartAtRow, guiShowplanExec);
		}
		else
			displayQueryResults(_query_txt.getText(), 0, guiShowplanExec);
	}
	
//	private void actionCopy(ActionEvent e)
//	{
////		System.out.println("-------COPY---------");
//		StringBuilder sb = getResultPanelAsText(_resPanel);
//
//		if (sb != null)
//		{
//			StringSelection data = new StringSelection(sb.toString());
//			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
//			clipboard.setContents(data, data);
//		}
//	}


//	public void openTheWindow()
//	{
//		Configuration conf = Configuration.getCombinedConfiguration();
//		int width   = conf.getIntProperty("QueryWindow.size.width",  600);
//		int height  = conf.getIntProperty("QueryWindow.size.height", 400);
//		
//		openTheWindow(width, height);
//	}
	public void openTheWindow()
	{
		Configuration conf = Configuration.getCombinedConfiguration();

		if (_window == null)
			return;

		String screenResStr = SwingUtils.getScreenResulutionAsString();

		int width   = 600;
		int height  = 550;
		int winPosX = -1;
		int winPosY = -1;
		int divLoc  = -1;

		width   = conf.getIntProperty("QueryWindow.size.width",         width);
		height  = conf.getIntProperty("QueryWindow.size.height",        height);
		winPosX = conf.getIntProperty("QueryWindow.size.pos.x",         winPosX);
		winPosY = conf.getIntProperty("QueryWindow.size.pos.y",         winPosY);
		divLoc  = conf.getIntProperty("QueryWindow.splitPane.location", divLoc);

		width   = conf.getIntProperty("QueryWindow."+screenResStr+".size.width",         width);
		height  = conf.getIntProperty("QueryWindow."+screenResStr+".size.height",        height);
		winPosX = conf.getIntProperty("QueryWindow."+screenResStr+".size.pos.x",         winPosX);
		winPosY = conf.getIntProperty("QueryWindow."+screenResStr+".size.pos.y",         winPosY);
		divLoc  = conf.getIntProperty("QueryWindow."+screenResStr+".splitPane.location", divLoc);

		// If this window is a "cloned" window...
		// Or if other SQL Windows processes is started FIXME: implement this
		// Then move the window slightly
		//FIXME: Jps.java -- to get othet running applications...
		//       This will also cover when you start several windows but from the command line (or not cloning)

		if (System.getenv("SQLW_CLONE_CONNECT_PROPS") != null)
		{
			String propPropStr = System.getenv("SQLW_CLONE_CONNECT_PROPS");
			_logger.info("SQLW_CLONE_CONNECT_PROPS was passed with the values: "+propPropStr);
			
			PropPropEntry ppe = new PropPropEntry(propPropStr);
			winPosX = ppe.getIntProperty("WinProps", "x", winPosX);
			winPosY = ppe.getIntProperty("WinProps", "y", winPosY);
			if (winPosX > 0) winPosX -= 70;
			if (winPosY > 0) winPosY -= 20;
		}

//		winPosX=0;
//		winPosY=0;
		openTheWindow(width, height, winPosX, winPosY, divLoc);
	}
	public void openTheWindow(int width, int height, int winPosX, int winPosY, int dividerLocation) 
	{
//System.out.println("openTheWindow(width="+width+", height="+height+", winPosX="+winPosX+", winPosY="+winPosY+", dividerLocation="+dividerLocation+")");

		// Set size
		if (width >= 0 && height >= 0)
		{
			_window.setSize(width, height);
		}

		//Center the window
		if (winPosX == -1  && winPosY == -1)
		{
			_logger.debug("Open window in center of screen.");

//			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			Rectangle screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
			Dimension frameSize = _window.getSize();

			// We can't be larger than the screen
			if (frameSize.height > screenSize.height) frameSize.height = screenSize.height;
			if (frameSize.width  > screenSize.width)  frameSize.width  = screenSize.width;

			_window.setLocation((screenSize.width - frameSize.width) / 2,
			        (screenSize.height - frameSize.height) / 2);
		}
		// Set to last known position
		else
		{
			if ( ! SwingUtils.isOutOfScreen(winPosX, winPosY, width, height) )
			{
				_logger.debug("Open main window in last known position.");
				_window.setLocation(winPosX, winPosY);
			}
		}
		
		// Set the split pane location
		if (dividerLocation >= 0)
		{
			_splitPane.setDividerLocation(dividerLocation);
		}

		// Create a Runnable to set the main visible, and get Swing to invoke.
		SwingUtilities.invokeLater(new Runnable() 
		{
			@Override
			public void run() 
			{
				openTheWindowAsThread();
				_logger.debug("openTheWindowAsThread() AFTER... tread is terminating...");
			}
		});
	}

	public void setSql(String sql)
	{
		_query_txt.setText(sql);
	}
	public String getSql()
	{
		return _query_txt.getText();
	}
	
	
	
	private void openTheWindowAsThread()
	{
//		//		super.isTrayIconWindow
//		final Window w=this;
//		AccessController.doPrivileged(new PrivilegedAction()
//		{
//			Class windowClass;
//			Field fieldIsTrayIconWindow;
//
//		    public Object run() 
//		    {
//		        try {
//		            windowClass = Class.forName("java.awt.Window");
//		            fieldIsTrayIconWindow = windowClass.getDeclaredField("isTrayIconWindow");
//		            fieldIsTrayIconWindow.setAccessible(true);
//
//		        } catch (NoSuchFieldException e) {
//		            _logger.error("Unable to initialize WindowAccessor: ", e);
//		        } catch (ClassNotFoundException e) {
//		        	_logger.error("Unable to initialize WindowAccessor: ", e);
//		        }
//			    try { fieldIsTrayIconWindow.set(w, true); }
//			    catch (IllegalAccessException e) {_logger.error("Unable to access the Window object", e);}
//		        return null;
//		    }
//		});

		this.setVisible(true);
//		_window.setVisible(true);
	}


	/** close the db connection */
	private void close()
	{
		if (_conn != null)
		{
			try { _conn.close(); }
			catch (SQLException sqle) {/*ignore*/}
		}
		_conn = null;
	}

	/** Automatically close the connection when we're garbage collected */
	@Override
	protected void finalize()
	{
		if (_closeConnOnExit)
			close();
	}
	
	/**
	 * Change database context in the ASE 
	 * @param dbname name of the database to change to
	 * @return true on success
	 */
	private boolean useDb(String dbname)
	{
		if (dbname == null || (dbname!=null && dbname.trim().equals("")))
			return false;

		try
		{
// Just for test purposes
//System.out.println("useDb(dbname='"+dbname+"'): PreparedStatement");
//PreparedStatement ps = _conn.prepareStatement("use ?");
//ps.setString(1, dbname);
//ps.execute();
			_conn.createStatement().execute("use "+dbname);
			_currentDbName = dbname;
			return true;
		}
		catch(SQLException e)
		{
			// Then display the error in a dialog box
			JOptionPane.showMessageDialog(
//					QueryWindow.this, 
					_window, 
					"Error Number: "+e.getErrorCode()+"\n" + e.getMessage(),
					"Error", JOptionPane.ERROR_MESSAGE);
			getCurrentDb();
			//e.printStackTrace();
			return false;
		}
	}

	/**
	 * What is current working database
	 * @return database name, null on failure
	 */
	private String getCurrentDb()
	{
		try
		{
			Statement stmnt   = _conn.createStatement();
			ResultSet rs      = stmnt.executeQuery("select db_name()");
			String cwdb = "";
			while (rs.next())
			{
				cwdb = rs.getString(1);
			}
			_currentDbName = cwdb;

			String currentSelectedDb = (String) _dbnames_cbx.getSelectedItem();
			if ( ! cwdb.equals(currentSelectedDb) )
			{
				// Note this triggers: code completion for refresh
				_dbnames_cbx.setSelectedItem(cwdb);
			}

			return cwdb;
		}
		catch(SQLException e)
		{
			JOptionPane.showMessageDialog(
//					QueryWindow.this, 
					_window, 
					"Problems getting current Working Database:\n" +
					"Error Number: "+e.getErrorCode()+"\n" + e.getMessage(),
					"Error", JOptionPane.ERROR_MESSAGE);
			return null;
		}
	}

	/**
	 * Get 'all' databases from ASE, and set the ComboBox to Current Working database
	 */
	private boolean setDbNames()
	{
		try
		{
			Statement stmnt   = _conn.createStatement();
			ResultSet rs      = stmnt.executeQuery("select name, db_name() from master..sysdatabases readpast order by name");
			DefaultComboBoxModel cbm = new DefaultComboBoxModel();
			String cwdb = "";
			while (rs.next())
			{
				cbm.addElement(rs.getString(1));
				cwdb = rs.getString(2);
			}
			_currentDbName = cwdb;

			// Check if number of databases has changed
			int currentDbCount = _dbnames_cbx.getItemCount();
			if (cbm.getSize() != currentDbCount)
			{
				_dbnames_cbx.setModel(cbm);
			}

			// Check if Current Working database has changed
			String currentSelectedDb = (String) _dbnames_cbx.getSelectedItem();
			if ( ! cwdb.equals(currentSelectedDb) )
			{
				// Note this triggers: code completion for refresh
				_dbnames_cbx.setSelectedItem(cwdb);
			}
			return true;
		}
		catch(SQLException e)
		{
			DefaultComboBoxModel cbm = new DefaultComboBoxModel();
			cbm.addElement("Problems getting dbnames");
			_dbnames_cbx.setModel(cbm);
			return false;
		}
	}

	private void doDummySelect()
	throws SQLException
	{
		Statement stmnt   = _conn.createStatement();
		ResultSet rs      = stmnt.executeQuery("select 1");
		while (rs.next())
		{
		}
		rs.close();
		stmnt.close();
	}

	/*---------------------------------------------------
	** BEGIN: implementing ConnectionProvider
	**---------------------------------------------------
	*/
	@Override
	public Connection getNewConnection(String appname)
	{
		try
		{
			return AseConnectionFactory.getConnection(null, appname, null);
		}
		catch (Exception e)  // SQLException, ClassNotFoundException
		{
			_logger.error("Problems creating a new Connection", e);
			return null;
		}
	}
	@Override
	public Connection getConnection()
	{
		return _conn;
	}
	/*---------------------------------------------------
	** END: implementing ConnectionProvider
	**---------------------------------------------------
	*/





	private JPopupMenu createDataTablePopupMenu(JTable table)
	{
		_logger.debug("createDataTablePopupMenu(): called.");

		JPopupMenu popup = new JPopupMenu();

		// FIXME: Create 'execute selected Cells'
		
		// Create COPY entries
		TablePopupFactory.createCopyTable(popup);

		// Create EXEC entries
		JMenuItem menuItem = null;
		menuItem = new JMenuItem("Execute Content in Column of Selected Row(s)")
		{
			private static final long	serialVersionUID	= 1L;

			@Override
			public String getToolTipText(MouseEvent event) 
			{
				Component invoker = TablePopupFactory.getPopupMenuInvoker((JMenuItem)event.getSource());
				if (invoker instanceof ResultSetJXTable)
				{
					ResultSetJXTable table = (ResultSetJXTable)invoker;
					int atCol = table.columnAtPoint(table.getLastMouseClickPoint());
					if (atCol < 0)
						atCol = 0;
					int[] selRows = table.getSelectedRows();
					StringBuilder sb = new StringBuilder("<html><b>The following SQL will be executed:</b><pre>");
					for (int r : selRows)
					{
						String val = table.getValueAt(r, atCol) + "";
						sb.append(val).append("<br>").append(AseSqlScriptReader.getConfiguredGoTerminator()).append("<br>");
					}
					if (selRows.length > 0)
					{
						sb.append("</pre></html>");
						return sb.toString();
					}
				}
				return invoker.toString();
			}
		};
		ToolTipManager.sharedInstance().registerComponent(menuItem); // register the component with tooltip...
		menuItem.setActionCommand(TablePopupFactory.ENABLE_MENU_ROW_IS_SELECTED);
		menuItem.addActionListener(new ActionListener()
		{
			@Override 
			public void actionPerformed(ActionEvent e)
			{
				Component invoker = TablePopupFactory.getPopupMenuInvoker((JMenuItem)e.getSource());
				if (invoker instanceof ResultSetJXTable)
				{
					ResultSetJXTable table = (ResultSetJXTable)invoker;
					int atCol = table.columnAtPoint(table.getLastMouseClickPoint());
					if (atCol < 0)
						atCol = 0;
					int[] selRows = table.getSelectedRows();
					StringBuilder sb = new StringBuilder();
					for (int r : selRows)
					{
						String val = table.getValueAt(r, atCol) + "";
						sb.append(val).append("\n").append(AseSqlScriptReader.getConfiguredGoTerminator()).append("\n");
					}
					if (selRows.length > 0)
					{
						displayQueryResults(sb.toString(), 0, false);
					}
				}
			}
		});
		popup.add(menuItem);


		// create pupup depending on what we are connected to
		String propPostfix = "";
		if (DbUtils.isProductName(_connectedToProductName, DbUtils.DB_PROD_NAME_SYBASE_ASE))
		{
			propPostfix = "ase.";
		}
		else if (DbUtils.isProductName(_connectedToProductName, DbUtils.DB_PROD_NAME_SYBASE_RS))
		{
			propPostfix = "rs.";
		}
		else if (DbUtils.isProductName(_connectedToProductName, DbUtils.DB_PROD_NAME_SYBASE_ASA))
		{
			propPostfix = "asa.";
		}
		else if (DbUtils.isProductName(_connectedToProductName, DbUtils.DB_PROD_NAME_SYBASE_IQ))
		{
			propPostfix = "iq.";
		}
		else if (DbUtils.isProductName(_connectedToProductName, DbUtils.DB_PROD_NAME_HANA))
		{
			propPostfix = "hana.";
		}
		else if (DbUtils.isProductName(_connectedToProductName, DbUtils.DB_PROD_NAME_MSSQL))
		{
			propPostfix = "mssql.";
		}
		else if (DbUtils.isProductName(_connectedToProductName, DbUtils.DB_PROD_NAME_ORACLE))
		{
			propPostfix = "ora.";
		}
		else if (DbUtils.isProductName(_connectedToProductName, DbUtils.DB_PROD_NAME_DB2_UX))
		{
			propPostfix = "db2.";
		}
		else
		{
			propPostfix = "jdbc.";
		}

		TablePopupFactory.createMenu(popup, 
			TablePopupFactory.TABLE_PUPUP_MENU_PREFIX + propPostfix, 
//			Configuration.getInstance(Configuration.CONF), 
			Configuration.getCombinedConfiguration(), 
			table, this, _window);

		TablePopupFactory.createMenu(popup, 
			PROPKEY_APP_PREFIX + TablePopupFactory.TABLE_PUPUP_MENU_PREFIX + propPostfix, 
//			Configuration.getInstance(Configuration.CONF), 
			Configuration.getCombinedConfiguration(), 
			table, this, _window);
		
		if (popup.getComponentCount() == 0)
			return null;
		else
			return popup;
	}

	private void resetResultsetPanel()
	{
		if ( _appendResults_chk.isSelected() || _appendResults_scriptReader)
		{
			String dateTimeNowStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
			String appenModeStr   = 
				"#######################################################\n" +
				"## "+dateTimeNowStr+" - Append Mode is ON.\n" +
				"#######################################################";

			if (_asPlainText_chk.isSelected())
			{
				if (_result_txt == null)
				{
					RSyntaxTextAreaX out = new RSyntaxTextAreaX();
					RSyntaxUtilitiesX.installRightClickMenuExtentions(out, _resPanelTextScroll, _window);
					installResultTextExtraMenuEntries(out);
					_resPanelTextScroll.setViewportView(out);
					_resPanelTextScroll.setLineNumbersEnabled(true);

					// set this globaly as well
					_result_txt = out;
				}
				_result_txt.append("\n");
				_result_txt.append(appenModeStr);
				_result_txt.append("\n\n");
			}
			else
			{
				JAseMessage noRsMsg = new JAseMessage(appenModeStr, null);
				JButton clear = new JButton("Clear all results");
				clear.addActionListener(new ActionListener()
				{
					@Override
					public void actionPerformed(ActionEvent e)
					{
						_resPanel.removeAll();
						_resPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0 0, wrap"));
						_resPanel.repaint();
					}
				});
	
				_resPanel.add(clear,   "gapy 1");
				_resPanel.add(noRsMsg, "gapy 1, growx, pushx");
			}
		}
		else
		{
			_resPanel.removeAll();
			_resPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0 0, wrap"));
		}
	}

	public void displayQueryResults(final String sql, final int startRowInSelection, final boolean guiShowplanExec)
	{
		if (_conn == null)
		{
			SwingUtils.showErrorMessage(_window, "Not Connected", "Can't Execute, since we are not connected to any server.", null);
			return;
		}

		if (true)
		{
			if (_cmdHistoryDialog == null)
				_cmdHistoryDialog = new CommandHistoryDialog(this, _window);

			_cmdHistoryDialog.addEntry(_connectedToServerName, _connectedAsUser, _currentDbName, sql);
		}

		final SqlProgressDialog progress = new SqlProgressDialog(_window, _conn, sql);

		// Execute in a Swing Thread
		SwingWorker<String, Object> doBgThread = new SwingWorker<String, Object>()
		{
			@Override
			protected String doInBackground() throws Exception
			{
				// reset the error information, before execute otherwise there will be "haning" errors that are not "real"
				resetParserDbMessages();
				// Should we notify the Error Parser that something has changed???
				// It's done in the end, but if we raise an exception then the "red" lines will not be removed...

				// Check/set JDBC AutoCommit...
				if (_jdbcAutoCommit_chk.isVisible())
				{
					boolean isAutoCommit = _conn.getAutoCommit();
					if (isAutoCommit != _jdbcAutoCommit_chk.isSelected())
					{
						_logger.info("Setting JDBC AutoCommit to: " + _jdbcAutoCommit_chk.isSelected());
						_conn.setAutoCommit(_jdbcAutoCommit_chk.isSelected());
					}
				}

				if (guiShowplanExec)
				{
					resetResultsetPanel();
					
					JAseMessage noRsMsg = new JAseMessage("No result sets will be displayed in GUI exec mode.", null);
					_resPanel.add(noRsMsg, "gapy 1, growx, pushx");

					AsePlanViewer pv = new AsePlanViewer(_conn, sql);
					pv.setVisible(true);
				}
				else
				{
					resetResultsetPanel();
					
					JAseMessage noRsMsg = new JAseMessage("Sending Query to server.", null);
					_resPanel.add(noRsMsg, "gapy 1, growx, pushx");

					displayQueryResults(_conn, sql, startRowInSelection, progress, false);
				}
				return null;
			}

		};
//		progress.setSwingWorker(doBgThread);
		doBgThread.addPropertyChangeListener(progress);
		doBgThread.execute();

		// A dialog will be visible until the SwingWorker is done (if bgThread takes less than 200ms, the vialog will be skipped)
		progress.waitForExec(doBgThread, 200);
		//System.out.println("Background Executor is done = "+doBgThread.isDone());

		// We will continue here, when results has been sent by server
		//System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");

		try
		{
			doBgThread.get();
		}
		catch (Throwable originEx)
		{
			Throwable ex = originEx;
			if (ex instanceof ExecutionException)
				ex = ex.getCause();
			
			// lets write the exception to the output window...
			_resPanelScroll    .setVisible(false);
			_resPanelTextScroll.setVisible(true);

			_logger.trace("NO RS: "+ ( _resultCompList != null ? _resultCompList.size() : -1 ));

			RSyntaxTextAreaX out = new RSyntaxTextAreaX();
			RSyntaxUtilitiesX.installRightClickMenuExtentions(out, _resPanelTextScroll, _window);
			installResultTextExtraMenuEntries(out);
			_resPanelTextScroll.setViewportView(out);
			_resPanelTextScroll.setLineNumbersEnabled(true);

			// set this globaly as well
			_result_txt = out;

			// add exception text to OUTPUT TEXT
			out.setText(ex.toString());

			// DO POPUP
//			if ( ! (ex instanceof SQLException) )
//			{
//			// TODO: maybe add a "dont show this message again... only show it in the output window"
//			}

			String msg = ex.getMessage();
			if (ex instanceof GoSyntaxException)
			{
				msg = "<html>" + ex.getMessage().replace("\n", "<br>") + "</html>";
			}

			SwingUtils.showErrorMessage(_window, "Problems when executing Statement", 
				"<html>" +
				"<h2>Problems when executing Statement</h2>" +
				msg +
				"</html>", 
				originEx);
		}

		if (guiShowplanExec)
		{
			if ( _appendResults_chk.isSelected() || _appendResults_scriptReader)
			{
				// Simply do nothing: multiple negations could be missread, so this is easier to understand
			}
			else
			{
				_resPanel.removeAll();
				_resPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0 0, wrap"));
			}
			
			JAseMessage noRsMsg = new JAseMessage("No result sets will be displayed in GUI exec mode.", null);
			_resPanel.add(noRsMsg, "gapy 1, growx, pushx");
		}
		

//System.out.println("_connectedToProductName='"+_connectedToProductName+"'.");
		try 
		{
			// are we still connected?
			_conn.isClosed(); 

			// if ASE, refresh the database list and currect working database
//			if (_connectedToProductName != null && _connectedToProductName.equals(DbUtils.DB_PROD_NAME_SYBASE_ASE))
			if (DbUtils.isProductName(_connectedToProductName, DbUtils.DB_PROD_NAME_SYBASE_ASE, DbUtils.DB_PROD_NAME_MSSQL))
			{
				// issue 'select 1' to check if the connection is valid
				doDummySelect();

				// getCurrentDb() is also done in setDbNames()
				// it only refreshes the DB Combobox if number of databases has changed.
				setDbNames();

				// Also get "various statuses" like if we are in a transaction or not
				boolean getTranState = DbUtils.isProductName(_connectedToProductName, DbUtils.DB_PROD_NAME_SYBASE_ASE);
				_aseConnectionStateInfo = AseConnectionUtils.getAseConnectionStateInfo(_conn, getTranState);
				_statusBar.setAseConnectionStateInfo(_aseConnectionStateInfo);
			}
			else if (DbUtils.isProductName(_connectedToProductName, DbUtils.DB_PROD_NAME_SYBASE_RS))
			{
				// Do nothing
			}
			else
			{
				_jdbcConnectionStateInfo = DbUtils.getJdbcConnectionStateInfo(_conn);
				_statusBar.setJdbcConnectionStateInfo(_jdbcConnectionStateInfo);
			}
		}
		catch (SQLException e) 
		{
			_conn = null; 
		}

		setWatermark();
	}

	
	private void putSqlWarningMsgs(ResultSet rs, ArrayList<JComponent> resultCompList, PipeCommand pipeCommand, String debugStr, int batchStartRow, int startRowInSelection, String currentSql)
	{
		if (rs == null)
			return;
		try
		{
			putSqlWarningMsgs(rs.getWarnings(), resultCompList, pipeCommand, debugStr, batchStartRow, startRowInSelection, currentSql);
			rs.clearWarnings();
		}
		catch (SQLException e)
		{
		}
	}
	private void putSqlWarningMsgs(Statement stmnt, ArrayList<JComponent> resultCompList, PipeCommand pipeCommand, String debugStr, int batchStartRow, int startRowInSelection, String currentSql)
	{
		if (stmnt == null)
			return;
		try
		{
			putSqlWarningMsgs(stmnt.getWarnings(), resultCompList, pipeCommand, debugStr, batchStartRow, startRowInSelection, currentSql);
			stmnt.clearWarnings();
		}
		catch (SQLException e)
		{
		}
	}
	private void putSqlWarningMsgs(Connection conn, ArrayList<JComponent> resultCompList, PipeCommand pipeCommand, String debugStr, int batchStartRow, int startRowInSelection, String currentSql)
	{
		if (conn == null)
			return;
		try
		{
			putSqlWarningMsgs(conn.getWarnings(), resultCompList, pipeCommand, debugStr, batchStartRow, startRowInSelection, currentSql);
			conn.clearWarnings();
		}
		catch (SQLException e)
		{
		}
	}

	private void putSqlWarningMsgs(SQLException sqe, ArrayList<JComponent> resultCompList, PipeCommand pipeCommand, String debugStr, int batchStartRow, int startRowInSelection, String currentSql)
	{
		if (startRowInSelection < 0)
			startRowInSelection = 0;

		while (sqe != null)
		{
			int    msgNum      = sqe.getErrorCode();
			String msgText     = StringUtil.removeLastNewLine(sqe.getMessage());
			int    msgSeverity = -1;
			String objectText  = null;
			
			StringBuilder sb = new StringBuilder();
			int scriptRow = -1;
			if(sqe instanceof EedInfo) // Message from jConnect
			{
				// Error is using the addtional TDS error data.
				EedInfo eedi = (EedInfo) sqe;
				msgSeverity  = eedi.getSeverity();
				
				// Try to figgure out what we should write out in the 'script row'
				// Normally it's *nice* to print out *where* in the "whole" document the error happened, especially syntax errors etc (and not just "within" the SQL batch, because you would have several in a file)
				// BUT: if we *call* a stored procedure, and that stored procedure produces errors, then we want to write from what "script line" (or where) the procedure was called at
				// BUT: if we are developing creating procedures/functions etc we would want *where* in the "script line" (within the prcedure text) the error is produced (easier to find syntax errors, faulty @var names, table nemaes etc...)
				int lineNumber = eedi.getLineNumber();
				int lineNumberAdjust = 0;

				// for some product LineNumber starts at 0, so lets just adjust for this in the calculated (script row ###)
				if (    DbUtils.DB_PROD_NAME_SYBASE_ASA.equals(_connectedToProductName)
				     || DbUtils.DB_PROD_NAME_SYBASE_IQ .equals(_connectedToProductName) )
				{
					lineNumberAdjust = 1;

					// Parse SQL Anywhere messages that looks like: 
					//     Msg 102, Level 15, State 0:
					//     Line 0 (script row 884), Status 0, TranState 1:
					//     SQL Anywhere Error -131: Syntax error near 'x' on line 4
					// Get the last part 'on line #' as the lineNumberAdjust
					if (msgText.matches(".*on line [0-9]+[ ]*.*"))
					{
						int startPos = msgText.indexOf("on line ");
						if (startPos >= 0)
						{
							startPos += "on line ".length();
							int endPos = msgText.indexOf(" ", startPos);
							if (endPos <= 0)
								endPos = msgText.length();
							
							String lineNumStr = msgText.substring(startPos, endPos);

							try { lineNumberAdjust = Integer.parseInt(lineNumStr); }
							catch(NumberFormatException ignore) {}
						}
					}
				}

				// print messages, take some specific actions
				if (msgSeverity <= 10)
				{
					// If message originates from a Stored Procedures
					// do not use the Line Number from the Stored Procs, instead use the SQL Batch start...
					// ERROR messages get's handle in a TOTAL different way
					if(eedi.getProcedureName() != null)
					{
						lineNumber = 1;

						// If batch starts with empty lines, increment the lineNumber...
						lineNumber += StringUtil.getFirstInputLine(currentSql);
					}
				}

				// which row in the script was this at... not this might change if (msgSeverity > 10)
				scriptRow = startRowInSelection + batchStartRow + lineNumber + lineNumberAdjust;

				// Fill in some extra information for error messages
				if (msgSeverity > 10)
				{
					boolean firstOnLine = true;
					sb.append("Msg " + sqe.getErrorCode() +
							", Level " + eedi.getSeverity() + ", State " +
							eedi.getState() + ":\n");

					if( eedi.getServerName() != null)
					{
						sb.append("Server '" + eedi.getServerName() + "'");
						firstOnLine = false;
					}
					if(eedi.getProcedureName() != null)
					{
						sb.append( (firstOnLine ? "" : ", ") +
								"Procedure '" + eedi.getProcedureName() + "'");
						firstOnLine = false;
					}

					// If message is from a procedure, get some extra...
					String extraDesc  = "";
					if ( StringUtil.hasValue(eedi.getProcedureName()) )
					{
						String regex    = "(create|alter)\\s+(procedure|proc|trigger|view|function)";
						Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

						// SQL has create proc etc in it... figgure out the line number...
						if (pattern.matcher(currentSql).find())
						{
							// Keep current scriptRow (line number in the procedure)
							lineNumber = eedi.getLineNumber();
							extraDesc  = "";
						}
						// we are only executing the procedure, so take another approach
						// lets line number for first occurance of the procname in current SQL... 
						else 
						{
							String searchForName = eedi.getProcedureName();

							// also try to get the procedure text, which will be added to the message
							// but not for print statement
							if (_getObjectTextOnError_chk.isSelected() && eedi.getSeverity() > 10)
							{
								objectText = AseConnectionUtils.getObjectText(_conn, null, searchForName, null, _srvVersion);
								if (objectText == null && searchForName.startsWith("sp_"))
									objectText = AseConnectionUtils.getObjectText(_conn, "sybsystemprocs", searchForName, null, _srvVersion);
								objectText = StringUtil.markTextAtLine(objectText, lineNumber, true);
							}

							// loop the sql and find the first row that matches the procedure text
							String procRegex = searchForName + "[^a-z,^A-Z,^0-9]";
							Pattern procPat  = Pattern.compile(procRegex);

							Scanner scanner = new Scanner(currentSql);
							int rowNumber = 0;
							while (scanner.hasNextLine()) 
							{
								rowNumber++;
								String line = scanner.nextLine();
								// stop at first match
								if (procPat.matcher(line).find())
									break;
							}

							lineNumber = rowNumber;
							extraDesc  = "Called from ";
						}
					}
					scriptRow = startRowInSelection + batchStartRow + lineNumber + lineNumberAdjust;
					String scriptRowStr = (batchStartRow >= 0 ? " ("+extraDesc+"script row "+scriptRow+")" : "");

					sb.append( (firstOnLine ? "" : ", ") +
							"Line " + eedi.getLineNumber() + scriptRowStr +
							", Status " + eedi.getStatus() + 
							", TranState " + eedi.getTranState() + ":\n");
				}

				// Now print the error or warning
				String msg = sqe.getMessage();
				if (msg.endsWith("\n"))
					sb.append(msg);
				else
					sb.append(msg+"\n");

			} // end: if(sqe instanceof EedInfo) -- jConnect message
			else
			{
				// jConnect: SqlState: 010P4 java.sql.SQLWarning: 010P4: An output parameter was received and ignored.
				if ( "010P4".equals(sqe.getSQLState()) )
				{
					// Simply ignore: 010P4: An output parameter was received and ignored.
					// This is when a Stored Procedure return code is returned, which is Output Parameter 1
				}
				else if ( "010SL".equals(sqe.getSQLState()) )
				{
					// IGNORE: 010SL: Out-of-date metadata accessor information was found on this database.  Ask your database administrator to load the latest scripts.
				}
				// OK, jTDS drivers etc, will have warnings etc in print statements
				// Lets try to see if it's one of those.
				else if (sqe.getErrorCode() == 0 && sqe instanceof SQLWarning)
				{
					if (StringUtil.isNullOrBlank(msgText))
						sb.append(" ");
					else
						sb.append(msgText);
				}
				else
				{
					String msg = "Unexpected SQLException: " +
						_connectedToProductName + ": ErrorCode "+sqe.getErrorCode()+", SQLState "+sqe.getSQLState()+", ExceptionClass: " + sqe.getClass().getName() + "\n"
						+ sqe.getMessage();
					sb.append(msg);
				}
			}
			
			// Add the info to the list
			if (sb.length() > 0)
			{
				// If new-line At the end, remove it
				if ( sb.charAt(sb.length()-1) == '\n' )
					sb.deleteCharAt(sb.length()-1);

				String aseMsg = sb.toString();

				// Grep on individual rows in the Message
				if (pipeCommand != null && pipeCommand.isGrep())
				{
					PipeCommandGrep grep = (PipeCommandGrep)pipeCommand.getCmd();
					// If VALID for Message
					if ( grep.isValidForType(PipeCommandGrep.TYPE_MSG) )
					{
						String regexpStr = ".*" + grep.getConfig() + ".*";
						String newMessage = "";
				
						Scanner scanner = new Scanner(aseMsg);
						while (scanner.hasNextLine()) 
						{
							String line = scanner.nextLine();
				
							boolean aMatch = line.matches(regexpStr);
							System.out.println(">>>>> aMatch="+aMatch+", isOptV="+grep.isOptV()+", regexp='"+regexpStr+"'.");
							if (grep.isOptV())
								aMatch = !aMatch;
							if (aMatch)
								newMessage += line + "\n";
						}

						aseMsg = StringUtil.removeLastNewLine(newMessage);
					}
				}
				
				
				// Dummy to just KEEP the text message, and strip off Msg, Severity etc...
				// but only if Msg > 20000 and Severity == 16
				if (pipeCommand != null && pipeCommand.isGrep())
				{
					PipeCommandGrep grep = (PipeCommandGrep)pipeCommand.getCmd();
					// X option, mage the message a "one line"
					if ( grep.isOptX() )
					{
//						if (msgNum > 20000 && msgSeverity == 16)
							aseMsg = "Msg "+msgNum+": " + msgText;
					}
				}

//				if ( ! StringUtil.isNullOrBlank(aseMsg))
				resultCompList.add( new JAseMessage(aseMsg, msgNum, msgText, msgSeverity, scriptRow, currentSql, objectText, _query_txt) );

				if (_logger.isTraceEnabled())
					_logger.trace("ASE Msg("+debugStr+"): "+aseMsg);
			}

			sqe = sqe.getNextException();
		}
	}

	/*----------------------------------------------------------------------
	** BEGIN: class SqlStatementInfo, RpcParam
	**----------------------------------------------------------------------*/ 
	private static class SqlStatementInfo
	{
		private boolean                         _useCallableStatement = false;
		private ArrayList<QueryWindow.RpcParam> _rpcParams = null;
		private String                          _sql       = null;
		private boolean                         _doReturnCode = false;

		// Used when SQL String do NOT start with "exec "
		// Used during ResultSet loop
		private Statement _stmnt = null;

		// Used when SQL String starts with "exec "
		private CallableStatement _cstmnt = null;

		public Statement getStatement()
		{
			return (_useCallableStatement) ? _cstmnt : _stmnt;  
		}
		
		public boolean execute() 
		throws SQLException
		{
			return (_useCallableStatement) ? _cstmnt.execute() : _stmnt.execute(_sql);
		}
		
		public SqlStatementInfo(Connection conn, String sql, String dbProductName, ArrayList<JComponent> resultCompList)
		throws SQLException
		{
			_useCallableStatement = false;
			_sql = sql.trim();
			
			// Should we try to make a RPC call to the server
			if ( _sql.startsWith("\\exec ") || _sql.startsWith("\\rpc ") || _sql.startsWith("\\call ") )
			{
				// Initialize this to be empty at start
				_rpcParams = new ArrayList<QueryWindow.RpcParam>();

				// If we are connected to a server that has return codes
				if (DbUtils.isProductName(dbProductName, DbUtils.DB_PROD_NAME_SYBASE_ASE, DbUtils.DB_PROD_NAME_SYBASE_ASA, DbUtils.DB_PROD_NAME_SYBASE_IQ, DbUtils.DB_PROD_NAME_SYBASE_RS, DbUtils.DB_PROD_NAME_MSSQL))
					_doReturnCode = true;

				boolean forceRpc = false;
				if (_sql.startsWith("\\rpc "))
					forceRpc = true;

				// Flag that this is officially a CallableStatement now
				_useCallableStatement = true;

				String procName = "";
				String parameters = "";

				String rpcParamsStr = null;
				int rpcParamsBeginIdx = _sql.indexOf(":(");
				int rpcParamsEndIdx   = StringUtil.indexOfEndBrace(_sql, rpcParamsBeginIdx + 2, ')');

				int firstSpace = _sql.indexOf(" ");
				firstSpace++;
				int secondSpace = StringUtil.indexOf(_sql, firstSpace, ' ', '(');
				
				//System.out.println("firstSpace="+firstSpace+", secondSpace="+secondSpace);

				if (secondSpace > firstSpace)
				{
					procName = _sql.substring(firstSpace, secondSpace);
					secondSpace++; // move one char to right to get parameters
					_logger.debug("procName: |"+procName+"|.");

					if (rpcParamsBeginIdx == -1)
						parameters = _sql.substring(secondSpace).trim();
					else
						parameters = _sql.substring(secondSpace, rpcParamsBeginIdx).trim();
					
					// remove any trailing ')' if the SQL looks like: \call name(?,?)
					if ( parameters.endsWith(")") )
						parameters = parameters.substring(0, parameters.length()-1);
					_logger.debug("parameters: |"+parameters+"|.");

					if (rpcParamsBeginIdx != -1)
					{
						rpcParamsBeginIdx += 2;
						rpcParamsStr = _sql.substring(rpcParamsBeginIdx, rpcParamsEndIdx);
						_logger.debug("rpcParamsStr: |"+rpcParamsStr+"|.");
					}
				}
				else
				{
					procName = _sql.substring(firstSpace);
					parameters = "";
				}

				// RPC Parameters spec ':({int|string}=val[ out][,...])'
				// \exec procname ?, ?, ? :(int=3,string=val, int=99)
				if (rpcParamsStr != null)
				{
					_rpcParams = RpcParam.parse(rpcParamsStr);
				}

				if (_doReturnCode)
					_sql = "{?=call "+procName+"("+parameters+")}";
				else
					_sql = "{call "+procName+"("+parameters+")}";

				_logger.debug("NEW PROC SQL: |"+_sql+"|.");

				int questionMarkCount = StringUtil.charCount(parameters, '?');
				if (questionMarkCount == 0 && parameters.length() > 0)
				{
					String rpcParamSpec        = ":( {int|string} = val [ out] [,...] )";
					String rpcParamSpecExample = ":( int = 99, string = 'abc', int = 999 out )";
					String rpcfullExample      = "\\exec sp_who ? :( string = '2' )";
					
					String msg = "EXEC PROCEDURE AS RPC LOGIC: \n" +
					             "Trying to execute a stored procedure via RPC method. The parameter list dosn't contain any Question Marks('?') This will probably *force* the JDBC driver to deliver the call as a 'language' statament to the server. \n" +
					             "Current CallableStatement sql looks like '"+_sql+"'. \n" +
					             "If you want it to be sent as a RPC to the server, you needs to use Question Marks(?) for the parameter(s), and a parameter specification (se below) so that the JDBC driver can issue the call in an aproperiate way. \n" +
					             "Format of the Parameter Specification is '"+rpcParamSpec+"'. \n" +
					             "Example of the Parameter Specification '"+rpcParamSpecExample+"'. \n" +
					             "Example of a SQL that will be translated to a RPC call '"+rpcfullExample+"'.";

					// if issued by '\rpc' then throw an exception
					// otherwise just write a warning message and continue
					if (forceRpc)
						throw new SQLException(msg);

					// logg... but remove all NewLines
					_logger.warn(msg.replace("\n", ""));

					// Add WARNING Message to the result
					resultCompList.add(new JAseMessage(Version.getAppName()+": WARNING - "+msg, _sql));
				}
				if (questionMarkCount != _rpcParams.size())
				{
					String rpcParamSpec        = ":( {{int|string} = val [ out] | [ora_rs]} [,...] )";
					String rpcParamSpecExample = ":( int = 99, string = 'abc', int = 999 out )";
					String rpcfullExample      = "\\exec sp_who ? :( string = '2' )";
					
					String msg = "EXEC PROCEDURE AS RPC LOGIC: \n" +
					             "Trying to execute a stored procedure via RPC method. Number of Question Marks('?') doesn't match the paramete specification count (QuestionMarkCount="+questionMarkCount+", paramSpecCount="+_rpcParams.size()+"). \n" +
					             "Current CallableStatement sql looks like '"+_sql+"'. \n" +
					             "ParameterSpecification looks like '"+rpcParamsStr+"'. \n" +
					             "Format of the Parameter Specification is '"+rpcParamSpec+"'. \n" +
					             "Example of the Parameter Specification '"+rpcParamSpecExample+"'. \n" +
					             "Example of a SQL that will be translated to a RPC call '"+rpcfullExample+"'.";
					throw new SQLException(msg);
				}
			} // end: exec or call

			// Get a Statement or a CallableStatement
			if (_useCallableStatement)
			{
				// Get a CallableStatement...
				_cstmnt = conn.prepareCall(_sql);

				// Register the return-status
				if (_doReturnCode)
					_cstmnt.registerOutParameter(1, Types.INTEGER);

				// Set RPC Parameters
				if (_rpcParams != null)
				{
					int pos = _doReturnCode ? 1 : 0;
					for (RpcParam param : _rpcParams)
					{
						pos++; // first pos will be 2, since #1 is used as return status

						if (param._isOutput)
							_cstmnt.registerOutParameter(pos, param._sqlType);
						else
							_cstmnt.setObject(pos, param._val, param._sqlType);
					}
				}
			}
			else
			{
				// Get a "regular" Statement...
				_stmnt = conn.createStatement();
			}
		}

		public void readRpcReturnCodeAndOutputParameters(ArrayList<JComponent> resultCompList, boolean asPlainText) 
		throws SQLException
		{
			if (_useCallableStatement)
			{
				if (_doReturnCode)
				{
					int returnStatus = _cstmnt.getInt(1);
					resultCompList.add( new JAseProcRetCode(returnStatus, _sql) );
				}
				
				// Set RPC Parameters
				if (_rpcParams != null)
				{
					int pos       = _doReturnCode ? 1 : 0;
					int posAdjust = _doReturnCode ? 1 : 0;
					for (RpcParam param : _rpcParams)
					{
						pos++;
						
						if (param._isOutput)
						{
							Object outParamVal = _cstmnt.getObject(pos);

							// If OUTput parameter is ORACLE SYS_REFCURSOR, then read the ResultSet
							if (    outParamVal != null 
							     && outParamVal instanceof ResultSet 
							     && param._sqlType == RpcParam.ORACLE_CURSOR_TYPE
							   )
							{
								ResultSet rs = (ResultSet) outParamVal;

								// For the moment, don't support pipe/filters etc... just make this simple
								if (asPlainText)
								{
									ResultSetTableModel rstm = new ResultSetTableModel(rs, true, "Oracle ResultSet Cursor");
									
									resultCompList.add(new JPlainResultSet(rstm));
								}
								else
								{
									// Convert the ResultSet into a TableModel, which fits on a JTable
									ResultSetTableModel rstm = new ResultSetTableModel(rs, true, "Oracle ResultSet Cursor");
				
									// Create the JTable, using the just created TableModel/ResultSet
									JXTable tab = new ResultSetJXTable(rstm);
									tab.setSortable(true);
									tab.setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);
									tab.packAll(); // set size so that all content in all cells are visible
									tab.setColumnControlVisible(true);
									tab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	
									// Add the JTable to a list for later use
									resultCompList.add(tab);
								}
							}
							else
							{
								resultCompList.add( new JAseProcRetParam(pos-posAdjust, outParamVal, param._sqlType, _sql) );
							}
						} // end: isOutput parameter
					}// end: loop RpcParam
				}
			}
		} // end: method

	} // end: class SqlStatementInfo

	
	private static class RpcParam
	{
		private int     _sqlType  = Types.OTHER;
		private Object  _val      = null;
		private boolean _isOutput = false;

		 /**
		  * The SQL TYPE for an Oracle CURSOR in a Oracle Stored Procedure.
		  * This duplicates the OracleTypes.CURSOR, but with this constant 
		  * we do not need to import com.oracle.* jars into this project.
		  * However this class is still 100% dependent on Oracle at runtime 
		  * and cannot be unit tested without Oracle.
		  */
		private static int ORACLE_CURSOR_TYPE = -10;

		public static RpcParam parseEntry(String entry)
		{
			if (entry == null)
				throw new RuntimeException("Problem parsing RPC Parameter entry '"+entry+"', is NULL.");

			boolean isOracleResultSetOutputParameter = entry.trim().equalsIgnoreCase("ora_rs");

			if (isOracleResultSetOutputParameter)
			{
				RpcParam p = new RpcParam();
				p._isOutput = true;
				p._sqlType  = ORACLE_CURSOR_TYPE; 
				p._val      = null;

				return p;
			}

			int eqPos   = entry.indexOf('=');
			if (eqPos == -1)
				throw new RuntimeException("Problem parsing RPC Parameter entry '"+entry+"', no equal char is found. Expecting: 'int|string = value' or 'ora_rs'");
			String type = entry.substring(0, eqPos).trim();
			String val  = entry.substring(eqPos+1).trim();
			
			RpcParam p = new RpcParam();

			if (val.endsWith(" out") || val.endsWith(" OUT") )
			{
				p._isOutput = true;
				val = val.substring(0, val.length() - " out".length()).trim();
			}
			if ( (val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'")) )
			{
				val = val.substring(1, val.length() - 1);
			}

			if      ("int"   .equals(type)) {p._sqlType = Types.INTEGER; p._val = new Integer(val); }
			else if ("string".equals(type)) {p._sqlType = Types.VARCHAR; p._val = new String(val); }
			else throw new RuntimeException("Unknown RPC Datatype '"+type+"'.");

			return p;
		}
		public static ArrayList<RpcParam> parse(String rpcParamsStr)
		{
			ArrayList<RpcParam> retList = new ArrayList<RpcParam>();

			String[] tmp = rpcParamsStr.split(",");
			if ( tmp.length == 1 && StringUtil.isNullOrBlank(tmp[0]) )
				throw new RuntimeException("Problem parsing RPC Parameter String '"+rpcParamsStr+"', it looks like it's empty. Expecting: 'int|string = value' or 'ora_rs'.");

			for (int i=0; i<tmp.length; i++)
			{
				String entry = tmp[i].trim();
				RpcParam p = parseEntry(entry);
				if (_logger.isDebugEnabled())
					_logger.debug("RPC PARAM "+i+": '"+entry+"'. type=|"+p._sqlType+"|, val=|"+p._val+"|, isOutParam="+p._isOutput+".");
				retList.add(p);
			}
			return retList;
		}
	}
	/*----------------------------------------------------------------------
	** END: class SqlStatementInfo, RpcParam
	**----------------------------------------------------------------------*/ 


	/**
	 * This method uses the supplied SQL query string, and the
	 * ResultSetTableModelFactory object to create a TableModel that holds
	 * the results of the database query.  It passes that TableModel to the
	 * JTable component for display.
	 **/
	private void displayQueryResults(Connection conn, String goSql, int startRowInSelection, final SqlProgressDialog progress, boolean guiShowErrors)
	throws Exception
	{
		// If we've called close(), then we can't call this method
		if (conn == null)
			throw new IllegalStateException("Connection already closed.");

		// It may take a while to get the results, so give the user some
		// immediate feedback that their query was accepted.
//		_msgline.setText("Sending SQL to ASE...");
		_statusBar.setMsg("Sending SQL to ASE...");

		// Setup a message handler
		// Set an empty Message handler
		SybMessageHandler curMsgHandler = null;
		if (conn instanceof SybConnection)
		{
			curMsgHandler = ((SybConnection)conn).getSybMessageHandler();
//			((SybConnection)conn).setSybMessageHandler(null);
			((SybConnection)conn).setSybMessageHandler(new SybMessageHandler()
			{
				@Override
				public SQLException messageHandler(SQLException sqle)
				{
					// When connecting to repserver we get those messages, so discard them
					// Msg 32, Level 12, State 0:
					// Server 'GORAN_1_RS', Line 0, Status 0, TranState 0:
					// Unknown rpc received.
					if (DbUtils.DB_PROD_NAME_SYBASE_RS.equals(_connectedToProductName))
					{
						if (sqle.getErrorCode() == 32)
						{
							if (_logger.isDebugEnabled())
								_logger.debug("Discarding RepServer Message: "+ AseConnectionUtils.sqlExceptionToString(sqle));
							return null;
						}
					}
					
					// Increment Usage Statistics
					if (sqle instanceof SQLWarning)
						incSqlWarningCount();
					else
						incSqlExceptionCount();

					// Add it to the progress dialog
					progress.addMessage(sqle);

					// If we want to STOP if we get any errors...
					// Then we should return the origin Exception
					// SQLException will abort current SQL Batch, while SQLWarnings will continue to execute
					if (_abortOnDbMessages)
						return sqle;

					// Downgrade ALL messages to SQLWarnings, so executions wont be interuppted.
					return AseConnectionUtils.sqlExceptionToWarning(sqle);
				}
			});
		}

		// Vendor specific setting before we start to execute
		enableOrDisableVendorSpecifics(conn);

		
		// Increment Usage Statistics
		incExecMainCount();

		// The script reader might throw Exception that we want to abort the whole executions on
		try
		{
			// a linked list where to "store" result sets or messages
			// before "displaying" them
			_resultCompList = new ArrayList<JComponent>();

			String sql = "";

			// Get SQL Batch Terminator
			String sqlBatchTerminator = Configuration.getCombinedConfiguration().getProperty(PROPKEY_sqlBatchTerminator, DEFAULT_sqlBatchTerminator);
				
			// treat each 'go' rows as a individual execution
			// readCommand(), does the job
			AseSqlScriptReader sr = new AseSqlScriptReader(goSql, true, sqlBatchTerminator);
			if (_useSemicolonHack_chk.isSelected())
				sr.setSemiColonHack(true);

			// Set a Vendor specific SQL Execution string (default is null, Oracle & HANA: it's "/"
			sr.setAlternativeGoTerminator(getVendorSpecificSqlExecTerminatorString(_connectedToProductName));

			int batchCount = sr.getSqlTotalBatchCount();

			boolean isConnectionOk = true;

			// loop all batches
			for (sql = sr.getSqlBatchString(); sql != null; sql = sr.getSqlBatchString())
			{
				progress.setState("Sending SQL to server for statement " + (sr.getSqlBatchNumber()+1) + " of "+batchCount+", starting at row "+(sr.getSqlBatchStartLine()+1) );

				// This can't be part of the for loop, then it just stops if empty row
				if ( StringUtil.isNullOrBlank(sql) )
					continue;

				// Set "global" flag, sine it's used elsewhere
				_appendResults_scriptReader = sr.hasOption_appendOutput();
				
				progress.setCurrentSqlText(sql, batchCount, sr.getMultiExecCount());
				
				if (! isConnectionOk)
					break;

				// if 'go 10' we need to execute this 10 times
				for (int execCnt=0; execCnt<sr.getMultiExecCount(); execCnt++)
				{
					// If cancel has been pressed, do not continue to repeat the command
					if (progress.isCancelled())
						break;

					// Increment Usage Statistics
					incExecBatchCount();

					try
					{
						int rowsAffected = 0;

						// Used during ResultSet loop
						Statement stmnt = null;
	
						// RPC handling if the text starts with '\exec '
						// The for of this would be: {?=call procName(parameters)}
						SqlStatementInfo sqlStmntInfo = new SqlStatementInfo(_conn, sql, _connectedToProductName, _resultCompList);

						if (_showSentSql_chk.isSelected() || sr.hasOption_printSql())
							_resultCompList.add( new JSentSqlStatement(sql, sr.getSqlBatchStartLine() + startRowInSelection) );

						// remember the start time
						long execStartTime = System.currentTimeMillis();
						progress.setCurrentBatchStartTime(execCnt);

						// Get the Statement used for execution, which is used below when reading resultsets etc
						stmnt = sqlStmntInfo.getStatement();
						progress.setSqlStatement(stmnt); // Used to cancel() on the statement level

						// Execute the SQL
						boolean hasRs = sqlStmntInfo.execute();
	
						// calculate the execution time
						long execStopTime = System.currentTimeMillis();
	
						progress.setState("Waiting for Server to return resultset.");
						_statusBar.setMsg("Waiting for Server to return resultset.");
			
						// iterate through each result set
						int rsCount = 0;
						int loopCount = 0;
						do
						{
							loopCount++; // used for debugging

							// Append, messages and Warnings to _resultCompList, if any
							putSqlWarningMsgs(stmnt, _resultCompList, sr.getPipeCmd(), "-before-hasRs-", sr.getSqlBatchStartLine(), startRowInSelection, sql);
	
							if(hasRs)
							{
								incRsCount();
								rsCount++;
								_statusBar.setMsg("Reading resultset "+rsCount+".");
								progress.setState("Reading resultset "+rsCount+".");
			
								// Get next resultset to work with
								ResultSet rs = stmnt.getResultSet();

								ResultSetTableModel rstm = null;
			
								// Append, messages and Warnings to _resultCompList, if any
								putSqlWarningMsgs(stmnt, _resultCompList, sr.getPipeCmd(), "-after-getResultSet()-Statement-", sr.getSqlBatchStartLine(), startRowInSelection, sql);
								putSqlWarningMsgs(rs,    _resultCompList, sr.getPipeCmd(), "-after-getResultSet()-ResultSet-", sr.getSqlBatchStartLine(), startRowInSelection, sql);
	
								// Check for BCP pipe command
								PipeCommand pipeCmd = sr.getPipeCmd();
								if (pipeCmd != null && pipeCmd.isBcp())
								{
									pipeCmd.getCmd().doEndPoint(rs);

									int rowsSelected = (Integer) pipeCmd.getCmd().getEndPointResult(PipeCommandBcp.rowsSelected);
									int rowsInserted = (Integer) pipeCmd.getCmd().getEndPointResult(PipeCommandBcp.rowsInserted);
									incRsRowsCount(rowsSelected);
									incIudRowsCount(rowsInserted);

    								if (_showRowCount_chk.isSelected() || sr.hasOption_rowCount() || sr.hasOption_noData())
    									_resultCompList.add( new JAseRowCount(rowsInserted, sql) );
								}
								else
								{
									int limitRsRowsCount = -1; // do not limit
									if (_limitRsRowsRead_chk.isSelected())
										limitRsRowsCount = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_limitRsRowsReadCount, DEFAULT_limitRsRowsReadCount);
									if (sr.hasOption_topRows())
										limitRsRowsCount = sr.getOption_topRows();
									
									boolean asPlainText = _asPlainText_chk.isSelected();
									if (sr.hasOption_asPlaintText())
										asPlainText = sr.getOption_asPlaintText();

									boolean noData = false;
									if (sr.hasOption_noData())
										noData = sr.getOption_noData();
									
									if (asPlainText)
									{
										rstm = new ResultSetTableModel(rs, true, sql, limitRsRowsCount, noData, sr.getPipeCmd(), progress);
										putSqlWarningMsgs(rstm.getSQLWarning(), _resultCompList, sr.getPipeCmd(), "-after-ResultSetTableModel()-tm.getSQLWarningList()-", sr.getSqlBatchStartLine(), startRowInSelection, sql);
										
										if (_printRsInfo_chk.isSelected() || sr.hasOption_printRsi())
											_resultCompList.add( new JResultSetInfo(rstm, sql, sr.getSqlBatchStartLine() + startRowInSelection) );

										_resultCompList.add(new JPlainResultSet(rstm));
										// FIXME: use a callback interface instead
										
										if (rstm.isCancelled())
											_resultCompList.add(new JAseCancelledResultSet(sql));

										if (rstm.wasAbortedAfterXRows())
											_resultCompList.add(new JAseLimitedResultSet(rstm.getAbortedAfterXRows(), sql));
									}
									else
									{
										// Convert the ResultSet into a TableModel, which fits on a JTable
										rstm = new ResultSetTableModel(rs, true, sql, limitRsRowsCount, noData, sr.getPipeCmd(), progress);
										putSqlWarningMsgs(rstm.getSQLWarning(), _resultCompList, sr.getPipeCmd(), "-after-ResultSetTableModel()-tm.getSQLWarningList()-", sr.getSqlBatchStartLine(), startRowInSelection, sql);
					
										// Create the JTable, using the just created TableModel/ResultSet
										JXTable tab = new ResultSetJXTable(rstm);
										tab.setSortable(true);
										tab.setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);
										tab.packAll(); // set size so that all content in all cells are visible
										tab.setColumnControlVisible(true);
//										tab.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
										tab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		
										// Add a popup menu
										tab.setComponentPopupMenu( createDataTablePopupMenu(tab) );
		
										if (_printRsInfo_chk.isSelected() || sr.hasOption_printRsi())
											_resultCompList.add( new JResultSetInfo(rstm, sql, sr.getSqlBatchStartLine() + startRowInSelection) );

										// Add the JTable to a list for later use
										_resultCompList.add(tab);
										// FIXME: use a callback interface instead
										
										if (rstm.isCancelled())
											_resultCompList.add(new JAseCancelledResultSet(sql));

										if (rstm.wasAbortedAfterXRows())
											_resultCompList.add(new JAseLimitedResultSet(rstm.getAbortedAfterXRows(), sql));
									}
								} // end: normal read result set
			
								// Append, messages and Warnings to _resultCompList, if any
								putSqlWarningMsgs(stmnt, _resultCompList, sr.getPipeCmd(), "-before-rs.close()-", sr.getSqlBatchStartLine(), startRowInSelection, sql);
	
								// Get rowcount from the ResultSetTableModel
								if (rstm != null)
								{
									int readCount = rstm.getReadCount();
        							if (readCount >= 0)
        							{
        								incRsRowsCount(readCount);
        								if (_showRowCount_chk.isSelected() || sr.hasOption_rowCount() || sr.hasOption_noData())
        									_resultCompList.add( new JAseRowCount(readCount, sql) );
        							}
								}

								// Close it
								rs.close();

							} // end: hasResultSets 
							else // Treat update/row count(s) for NON RESULTSETS
							{
								// Java DOC: getUpdateCount() Retrieves the current result as an update count; if the result is a ResultSet object 
								//           or there are no more results, -1 is returned. This method should be called only once per result.
								// Without this else statement, some drivers maight fail... (MS-SQL actally did)

								rowsAffected = stmnt.getUpdateCount();

								if (rowsAffected >= 0)
								{
    								incIudRowsCount(rowsAffected);
									_logger.debug("---- DDL or DML (statement with no-resultset) Rowcount: "+rowsAffected);

									if (_showRowCount_chk.isSelected() || sr.hasOption_rowCount() || sr.hasOption_noData())
										_resultCompList.add( new JAseRowCount(rowsAffected, sql) );
								}
								else
								{
									_logger.debug("---- No more results to process.");
								}
							} // end: no-resultset
			
							// Append, messages and Warnings to _resultCompList, if any
							putSqlWarningMsgs(stmnt, _resultCompList, sr.getPipeCmd(), "-before-rs.close()-", sr.getSqlBatchStartLine(), startRowInSelection, sql);
			
							// Check if we have more resultsets
							// If any SQLWarnings has not been found above, it will throw one here
							// so catch raiserrors or other stuff that is not SQLWarnings.
							hasRs = stmnt.getMoreResults();
			
							// Append, messages and Warnings to _resultCompList, if any
							putSqlWarningMsgs(stmnt, _resultCompList, sr.getPipeCmd(), "-before-rs.close()-", sr.getSqlBatchStartLine(), startRowInSelection, sql);

							if (_logger.isTraceEnabled())
								_logger.trace( "--loopCount="+loopCount+", hasRs="+hasRs+", rowsAffected="+rowsAffected+", "+((hasRs || rowsAffected != -1) ? "continue-to-loop" : "<<< EXIT LOOP <<<") );
						}
						while (hasRs || rowsAffected != -1);
			
						progress.setState("No more results for this batch");
	
						// Read RPC returnCode and output parameters, if it was a RPC and any retCode and/or params exists
						sqlStmntInfo.readRpcReturnCodeAndOutputParameters(_resultCompList, _asPlainText_chk.isSelected());
	
						// Append, messages and Warnings to _resultCompList, if any
						putSqlWarningMsgs(stmnt, _resultCompList, sr.getPipeCmd(), "-before-stmnt.close()-", sr.getSqlBatchStartLine(), startRowInSelection, sql);
						
						// Close the statement
						stmnt.close();
						progress.setSqlStatement(null);

						// Connection level WARNINGS, Append, messages and Warnings to _resultCompList, if any
						putSqlWarningMsgs(_conn, _resultCompList, sr.getPipeCmd(), "-before-stmnt.close()-", sr.getSqlBatchStartLine(), startRowInSelection, sql);
	
						// How long did it take
						long execFinnishTime = System.currentTimeMillis();
						if (_clientTiming_chk.isSelected() || sr.hasOption_printClientTiming())
							_resultCompList.add( new JClientExecTime(execStartTime, execStopTime, execFinnishTime, startRowInSelection + sr.getSqlBatchStartLine() + 1, sql));
	
						// Increment Usage Statistics
						incExecTimeTotal  (execFinnishTime - execStartTime);
						incExecTimeSqlExec(execStopTime    - execStartTime);
						incExecTimeRsRead (execFinnishTime - execStopTime);
						
						// Sleep for a while, if that's enabled
						if (sr.getMultiExecWait() > 0)
						{
							//System.out.println("WAITING for multi exec sleep: "+sr.getMultiExecWait());
							Thread.sleep(sr.getMultiExecWait());
						}

						// Read some extra stuff
						readVendorSpecificResults(conn, progress, sql);
					}
					catch (SQLException ex)
					{
						_logger.debug("Caught SQL Exception, get the stacktrace if in debug mode...", ex);

						incSqlExceptionCount();
						progress.setSqlStatement(null);

						// If something goes wrong, clear the message line
						_statusBar.setMsg("Error: "+ex.getMessage());

						// when NOT using jConnect, I can't downgrade a SQLException to a SQLWarning
						// so we will always end up here (for the moment)
						// Try to read the SQLException and figure out on what line it happened + mark that line with an error
						errorReportingVendorSqlException(ex, _resultCompList, startRowInSelection, sr.getSqlBatchStartLine(), sql);

						// Add the information to the output window
						// This is done in: errorReportingVendorSqlException()
						
						
						// Check if we are still connected...
						if ( ! AseConnectionUtils.isConnectionOk(conn) )
						{
							isConnectionOk = false;
							break;
						}

						// If we want to STOP if we get any errors...
						// Then we should return the origin Exception
						// NOTE: THIS HAS NOT BEEN TESTED
//						if (_abortOnDbMessages)
//							throw ex;
					}

				} // end: 'go 10'
				
			} // end: read batches

			// Close the script reader
			sr.close();


			
			progress.setState("Add data to GUI result");

			// Finally, add all the results to the output
			addToResultsetPanel(_resultCompList, (_appendResults_chk.isSelected() || _appendResults_scriptReader), _asPlainText_chk.isSelected());


			//---------------------------------------
			// Mark DB Messages in the editor (red underline on the row)
			//---------------------------------------
			@SuppressWarnings("unchecked")
			ArrayList<JAseMessage> errorInfo = (ArrayList<JAseMessage>) _query_txt.getDocument().getProperty(ParserProperties.DB_MESSAGES);

			if (errorInfo == null)
				errorInfo = new ArrayList<JAseMessage>();

			for (JComponent jcomp: _resultCompList)
			{
				if (jcomp instanceof JSQLExceptionMessage)
				{
					// this is already added, in method: errorReportingVendorSqlException()
					continue;
				}
				if (jcomp instanceof JAseMessage)
				{
					JAseMessage msg = (JAseMessage) jcomp;
					if ( msg._scriptRow >= 0 && (msg._msgSeverity > 10 || msg._msgSeverity == -1) )
						errorInfo.add(msg);
				}
			}
			_query_txt.getDocument().putProperty(ParserProperties.DB_MESSAGES, errorInfo);
			for (int p=0; p<_query_txt.getParserCount(); p++)
				try { _query_txt.forceReparsing(p); } catch (Throwable ignore) {} // protect from errors in RSyntaxTextArea


			// Show/hide error buttons
			boolean showErrorButtons = errorInfo.size() > 0;
			_prevErr_but.setVisible(showErrorButtons);
			_nextErr_but.setVisible(showErrorButtons);

			// We're done, so clear the feedback message
			//_msgline.setText(" ");
		}
//		catch (SQLException ex)
//		{
//			// If something goes wrong, clear the message line
//			_statusBar.setMsg("Error: "+ex.getMessage());
//
//			// Then display the error in a dialog box
//			if (guiShowErrors)
//			{
//				JOptionPane.showMessageDialog(
//					_window, 
//					new String[] { // Display a 2-line message
//							ex.getClass().getName() + ": ", 
//							ex.getMessage() },
//					"Error", JOptionPane.ERROR_MESSAGE);
//			}
//			throw ex;
//		}
		catch (IOException ex)
		{
			_logger.error("While reading the input SQL 'go' String, caught: "+ex, ex);
			throw ex;
		}
		catch (PipeCommandException ex)
		{
			_logger.warn("Problems creating the 'go | pipeCommand'. Caught: "+ex, ex);
			if (guiShowErrors)
			{
				SwingUtils.showWarnMessage("Problems creating PipeCommand", ex.getMessage(), ex);
			}
			throw ex;
		}
		catch (GoSyntaxException ex)
		{
			_logger.warn("Problems parsing the SQL Batch Terminator ('go'). Caught: "+ex, ex);
			if (guiShowErrors)
			{
				String htmlMsg = "<html>" + ex.getMessage().replace("\n", "<br>") + "</html>";
				SwingUtils.showWarnMessage("Problems interpreting a SQL Batch Terminator", htmlMsg, ex);
			}
			throw ex;
		}
		finally
		{
			// restore old message handler
			if (curMsgHandler != null)
			{
				((SybConnection)conn).setSybMessageHandler(curMsgHandler);
			}
		}
		
		// In some cases, some of the area in not repainted
		// example: when no RS, but only messages has been displayed
		_resPanel.repaint();
	}

	/** Add components to output 
	 * @param asPlainText */
	private void addToResultsetPanel(ArrayList<JComponent> compList, boolean append, boolean asPlainText)
	{
		//-----------------------------
		// Add data... to panel(s) in various ways
		// - one result set, just add it
		// - many result sets
		//        - Add to JTabbedPane
		//        - OR: append the result sets as named panels
		//-----------------------------
//		if ( ! _appendResults_chk.isSelected() )
		if ( ! append )
		{
			_resPanel.removeAll();
			_resPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0 0, wrap"));

			// Release this, if '_asPlainText' is enabled it will be set at the end... 
			_result_txt = null;
		}

		// Remember where we started so we can move to that position if is's append mode
		final int lastComponentBeforeAddInfo = _resPanel.getComponentCount() - 3;

		
		boolean inHtmlMessage = false;
		StringBuilder htmlMessageBuffer = new StringBuilder();

		int numOfTables = countTables(compList);
		if (asPlainText)
			numOfTables = 0;

		if (numOfTables == 1)
		{
			_resPanelScroll    .setVisible(true);
			_resPanelTextScroll.setVisible(false);

			int msgCount = 0;
			int rowCount = 0;
			if (_logger.isTraceEnabled())
				_logger.trace("Only 1 RS");

			// Add ResultSet  
			for (JComponent jcomp: compList)
			{
				if (jcomp instanceof JTable)
				{
					JTable tab = (JTable) jcomp;

					JPanel p = new JPanel(new MigLayout("insets 0 0 0 0, gap 0 0, wrap"));
					if (doConvertToXmlTextPane(tab))
						p.add(createXmlTextPane(tab), "wrap");
					else
					{
						// JScrollPane is on _resPanel
						// So we need to display the table header ourself
						p.add(tab.getTableHeader(), "wrap");
						p.add(tab,                  "wrap");
					}

					if (_logger.isTraceEnabled())
						_logger.trace("1-RS: add: JTable");
					_resPanel.add(p, "");

					rowCount = tab.getRowCount();
				}
				else if (jcomp instanceof JPlainResultSet)
				{
					JPlainResultSet plainRs = (JPlainResultSet)jcomp;
					_resPanel.add(createPlainRsTextArea(plainRs), "gapy 1, growx, pushx");
				}
				else if (jcomp instanceof JAseMessage)
				{
					JAseMessage msg = (JAseMessage) jcomp;
					if (msg.hasHtmlStartTag())
						inHtmlMessage = true;

					// Add it to buffer if HTML, otherwise add the message directly
					if (inHtmlMessage)
						htmlMessageBuffer.append(msg._msgText);
					else
					{
						if (_logger.isTraceEnabled())
							_logger.trace("1-RS: JAseMessage: "+msg.getText());
						_resPanel.add(msg, "gapy 1, growx, pushx");

						msgCount++;
					}

					// On HTML end tag, create a HTML Message and add it
					if (msg.hasHtmlEndTag())
					{
						inHtmlMessage = false;
						_resPanel.add(createHtmlMessage(htmlMessageBuffer, true), "gapy 1, growx, pushx");

						msgCount++;
					}
				}
			}
//			_msgline.setText(" "+rowCount+" rows, and "+msgCount+" messages.");
			_statusBar.setMsg(" "+rowCount+" rows, and "+msgCount+" messages.");
		}
		else if (numOfTables > 1)
		{
			_resPanelScroll    .setVisible(true);
			_resPanelTextScroll.setVisible(false);

			int msgCount = 0;
			int rowCount = 0;
			if (_logger.isTraceEnabled())
				_logger.trace("Several RS: "+compList.size());
			
			// AS JTABLE
			if (_rsInTabs_chk.isSelected())
			{
				// Add Result sets to individual tabs, on a JTabbedPane 
				JTabbedPane tabPane = new JTabbedPane();
				if (_logger.isTraceEnabled())
					_logger.trace("JTabbedPane: add: JTabbedPane");
				_resPanel.add(tabPane, "");

				int i = 1;
				for (JComponent jcomp: compList)
				{
					if (jcomp instanceof JTable)
					{
						JTable tab = (JTable) jcomp;

						JPanel p = new JPanel(new MigLayout("insets 0 0 0 0, gap 0 0, wrap"));
						if (doConvertToXmlTextPane(tab))
							p.add(createXmlTextPane(tab), "wrap");
						else
						{
							// JScrollPane is on _resPanel
							// So we need to display the table header ourself
							p.add(tab.getTableHeader(), "wrap");
							p.add(tab,                  "wrap");
						}

						if (_logger.isTraceEnabled())
							_logger.trace("JTabbedPane: add: JTable("+i+")");
						tabPane.addTab("Result "+(i++), p);

						rowCount += tab.getRowCount();
					}
					else if (jcomp instanceof JPlainResultSet)
					{
						JPlainResultSet plainRs = (JPlainResultSet)jcomp;
						_resPanel.add(createPlainRsTextArea(plainRs), "gapy 1, growx, pushx");
					}
					else if (jcomp instanceof JAseMessage)
					{
//						// FIXME: this probably not work if we want to have the associated messages in the correct tab
//						JAseMessage msg = (JAseMessage) jcomp;
//						_resPanel.add(msg, "gapy 1, growx, pushx");
//						if (_logger.isTraceEnabled())
//							_logger.trace("JTabbedPane: JAseMessage: "+msg.getText());
//
//						msgCount++;
						JAseMessage msg = (JAseMessage) jcomp;
						if (msg.hasHtmlStartTag())
							inHtmlMessage = true;

						// Add it to buffer if HTML, otherwise add the message directly
						if (inHtmlMessage)
							htmlMessageBuffer.append(msg._msgText);
						else
						{
							if (_logger.isTraceEnabled())
								_logger.trace("1-RS: JAseMessage: "+msg.getText());
							_resPanel.add(msg, "gapy 1, growx, pushx");

							msgCount++;
						}

						// On HTML end tag, create a HTML Message and add it
						if (msg.hasHtmlEndTag())
						{
							inHtmlMessage = false;
							_resPanel.add(createHtmlMessage(htmlMessageBuffer, true), "gapy 1, growx, pushx");

							msgCount++;
						}
					}
				}
				if (_lastTabIndex > 0)
				{
					if (_lastTabIndex < tabPane.getTabCount())
					{
						tabPane.setSelectedIndex(_lastTabIndex);
						if (_logger.isTraceEnabled())
							_logger.trace("Restore last tab index pos to "+_lastTabIndex);
					}
				}
//				_msgline.setText(" "+numOfTables+" ResultSet with totally "+rowCount+" rows, and "+msgCount+" messages.");
				_statusBar.setMsg(" "+numOfTables+" ResultSet with totally "+rowCount+" rows, and "+msgCount+" messages.");
				
			} // end: (numOfTables > 1)
			else // not as JTable
			{
				// Add Result sets to individual panels, which are 
				// appended to the result panel
				int i = 1;
				for (JComponent jcomp: compList)
				{
					if (jcomp instanceof JTable)
					{
						JTable tab = (JTable) jcomp;

						JPanel p = new JPanel(new MigLayout("insets 0 0, gap 0 0"));
						Border border = BorderFactory.createTitledBorder("ResultSet "+(i++));
						p.setBorder(border);
						if (doConvertToXmlTextPane(tab))
							p.add(createXmlTextPane(tab), "wrap");
						else
						{
							// JScrollPane is on _resPanel
							// So we need to display the table header ourself
							p.add(tab.getTableHeader(), "wrap");
							p.add(tab,                  "wrap");
						}
						if (_logger.isTraceEnabled())
							_logger.trace("JPane: add: JTable("+i+")");
						_resPanel.add(p, "");

						rowCount += tab.getRowCount();
					}
					else if (jcomp instanceof JPlainResultSet)
					{
						JPlainResultSet plainRs = (JPlainResultSet)jcomp;
						_resPanel.add(createPlainRsTextArea(plainRs), "gapy 1, growx, pushx");
					}
					else if (jcomp instanceof JAseMessage)
					{
//						JAseMessage msg = (JAseMessage) jcomp;
//						if (_logger.isTraceEnabled())
//							_logger.trace("JPane: JAseMessage: "+msg.getText());
//						_resPanel.add(msg, "gapy 1, growx, pushx");
//
//						msgCount++;
						JAseMessage msg = (JAseMessage) jcomp;
						if (msg.hasHtmlStartTag())
							inHtmlMessage = true;

						// Add it to buffer if HTML, otherwise add the message directly
						if (inHtmlMessage)
							htmlMessageBuffer.append(msg._msgText);
						else
						{
							if (_logger.isTraceEnabled())
								_logger.trace("1-RS: JAseMessage: "+msg.getText());
							_resPanel.add(msg, "gapy 1, growx, pushx");

							msgCount++;
						}

						// On HTML end tag, create a HTML Message and add it
						if (msg.hasHtmlEndTag())
						{
							inHtmlMessage = false;
							_resPanel.add(createHtmlMessage(htmlMessageBuffer, true), "gapy 1, growx, pushx");

							msgCount++;
						}
					}
				}
//				_msgline.setText(" "+numOfTables+" ResultSet with totally "+rowCount+" rows, and "+msgCount+" messages.");
				_statusBar.setMsg(" "+numOfTables+" ResultSet with totally "+rowCount+" rows, and "+msgCount+" messages.");
			}
		}
		else // NO table output, only messages etc.
		{
			int msgCount = 0;

			if (asPlainText)
			{
				_resPanelScroll    .setVisible(false);
				_resPanelTextScroll.setVisible(true);

				if (_logger.isTraceEnabled())
					_logger.trace("NO RS: "+compList.size());

				RSyntaxTextAreaX out = null;
//				if ( _result_txt == null || ! _appendResults_chk.isSelected() )
				if ( _result_txt == null || ! append )
				{
					out = new RSyntaxTextAreaX();
					RSyntaxUtilitiesX.installRightClickMenuExtentions(out, _resPanelTextScroll, _window);
					installResultTextExtraMenuEntries(out);
					_resPanelTextScroll.setViewportView(out);
					_resPanelTextScroll.setLineNumbersEnabled(true);

					// set this globaly as well
					_result_txt = out;
				}
				out = _result_txt;

				// Copy results to the output. 
				boolean prevComponentWasResultSet = false;

				// remember where to position cursor 
				final int lastLineBeforeAddInfo = _result_txt.getLineCount() - 5;

				for (JComponent jcomp: compList)
				{
					if (jcomp instanceof JPlainResultSet)
					{
						JPlainResultSet prs = (JPlainResultSet) jcomp;
						out.append(prs.getText());
						
						// if _showRowCount is selected, the "(### rows affected)"
						// is genereated at an earlier stage, so then we do NOT need to add it once more here
						if ( ! _showRowCount_chk.isSelected() )
						{
							out.append("\n");
//							out.append("(" + prs.getRowCount() + " rows affected)\n");
							out.append("(" + prs.getRowCount() + " rows affected, in plain resultset)\n");
							out.append("\n");
						}
						prevComponentWasResultSet = true;
					}
					else if (jcomp instanceof JAseRowCount)
					{
						JAseRowCount msg = (JAseRowCount) jcomp;
						
						// If prev was RS, add an extra newlines before and after(for readability)
						// and to simulate "isql"
						if ( prevComponentWasResultSet )
						{
							out.append("\n");
							out.append(msg.getText()); out.append("\n");
							out.append("\n");
						}
						else
						{
							out.append(msg.getText()); out.append("\n");
						}

						prevComponentWasResultSet = false;
					}
					else if (jcomp instanceof JAseProcRetCode)
					{
						JAseProcRetCode msg = (JAseProcRetCode) jcomp;

						out.append(msg.getText()); 
						out.append("\n");

						prevComponentWasResultSet = false;
					}
					else if (jcomp instanceof JAseMessage)
					{
						JAseMessage msg = (JAseMessage) jcomp;

						out.append(msg.getText());
						out.append("\n");

						msgCount++;
						prevComponentWasResultSet = false;
					}
				}

				// If we have a XML text do some special stuff
				boolean hasXml = out.getText().indexOf("<?xml ") >= 0;
				if (hasXml)
				{
					out.setCodeFoldingEnabled(true);
					out.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
				}

				// Move "near" END-OF-TEXT or where the append started.
//				if (_appendResults_chk.isSelected())
				if (append)
				{
					try
					{
						// Move cursor to END-OF-TEXT
						int line = _result_txt.getLineCount() - 1;
						int pos  = _result_txt.getLineStartOffset(line);
						_result_txt.setCaretPosition( pos );

						// Move to the position where we started to to inserts
						// but do it defferred, so above position take place first
						Runnable doLater = new Runnable()
						{
							@Override
							public void run()
							{
								try 
								{
									int pos  = _result_txt.getLineStartOffset(lastLineBeforeAddInfo);
									_result_txt.setCaretPosition( pos );
								} 
								catch (BadLocationException ble) {/*ignore*/}
							}
						};
						SwingUtilities.invokeLater(doLater);
					}
					catch (BadLocationException ble) { /* ignore */ }
				}


			} // end asPlainText
			else
			{
				_resPanelScroll    .setVisible(true);
				_resPanelTextScroll.setVisible(false);

				// Add Result sets to individual panels, which are 
				// appended to the result panel
				for (JComponent jcomp: compList)
				{
					if (jcomp instanceof JAseMessage)
					{
						JAseMessage msg = (JAseMessage) jcomp;
						if (msg.hasHtmlStartTag())
							inHtmlMessage = true;

						// Add it to buffer if HTML, otherwise add the message directly
						if (inHtmlMessage)
							htmlMessageBuffer.append(msg._msgText);
						else
						{
							if (_logger.isTraceEnabled())
								_logger.trace("1-RS: JAseMessage: "+msg.getText());
							_resPanel.add(msg, "gapy 1, growx, pushx");

							msgCount++;
						}

						// On HTML end tag, create a HTML Message and add it
						if (msg.hasHtmlEndTag())
						{
							inHtmlMessage = false;
							_resPanel.add(createHtmlMessage(htmlMessageBuffer, true), "gapy 1, growx, pushx");

							msgCount++;
						}
					}
				}
			}


//			_msgline.setText("NO ResultSet, but "+msgCount+" messages.");
			_statusBar.setMsg("NO ResultSet, but "+msgCount+" messages.");
		}

		// Move scroll so that first commponent (Clear Button) that was appended will be visible
//		if (_appendResults_chk.isSelected() && lastComponentBeforeAddInfo > 0)
		if (append && lastComponentBeforeAddInfo > 0)
		{
			// Make the move after it has been displayed, otherwise comp.getY() will show 0 or some faulty value
			Runnable doLater = new Runnable()
			{
				@Override
				public void run()
				{
					Component comp   = _resPanel.getComponent(lastComponentBeforeAddInfo);
					_resPanelScroll.getVerticalScrollBar().setValue(comp.getY());
				}
			};
			SwingUtilities.invokeLater(doLater);
		}
	}


	/** Get alternative 'go' terminator strings */
	private String getVendorSpecificSqlExecTerminatorString(String productName)
	{
//		if ( _useAltExecTermStr_chk.isSelected() )
//		{
			if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_HANA))   return "/";
			if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_ORACLE)) return "/";
			if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_DB2_UX)) return "/";
//		}
		return null;
	}


	/** Enable or disable Vendor specific settings and functionality */
	private void enableOrDisableVendorSpecifics(Connection conn)
	{
		// Setup Oracle specific stuff
		if (DbUtils.isProductName(_connectedToProductName, DbUtils.DB_PROD_NAME_ORACLE, DbUtils.DB_PROD_NAME_DB2_UX))
		{
			if ( _enableDbmsOutput_chk.isSelected() )
			{
				if ( ! _dbmsOutputIsEnabled )
				{
					try
					{
						CallableStatement stmt = conn.prepareCall("{call dbms_output.enable(?) }");
						
						int initSize = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_enableDbmsInitSize, DEFAULT_enableDbmsInitSize);
						stmt.setInt(1, initSize);
						stmt.execute();

						_logger.info("Enabling Oracle/DB2 DBMS_OUTPUT, suceeded");
						_dbmsOutputIsEnabled = true;
					}
					catch (Exception e)
					{
						_logger.warn("Problem occurred while trying to enable Oracle/DB2 DBMS_OUTPUT. Caught:" + e);
					}
				}
			}
			else // not selected, check if it has been disabled otherwise do it
			{
				if ( _dbmsOutputIsEnabled )
				{
					try
					{
						CallableStatement stmt = conn.prepareCall("{call dbms_output.disable }");
						stmt.execute();

						_logger.info("Disabling Oracle DBMS_OUTPUT, suceeded");
						_dbmsOutputIsEnabled = false;
					}
					catch (Exception e)
					{
						_logger.warn("Problem occurred while trying to disable Oracle DBMS_OUTPUT. Caught:" + e);
					}
				}
			}
		}
	}

	/** Read vendor specific results */
	private void readVendorSpecificResults(Connection conn, SqlProgressDialog progress, String sql)
	{
		// Get Oracle/DB2 specific DBMS_OUTPUT  messages
		if (DbUtils.isProductName(_connectedToProductName, DbUtils.DB_PROD_NAME_ORACLE, DbUtils.DB_PROD_NAME_DB2_UX) && _enableDbmsOutput_chk.isSelected())
		{
			progress.setState("Getting Oracle: DBMS_OUTPUT");

			try
			{
				CallableStatement stmt = conn.prepareCall("{call dbms_output.get_line(?,?)}");
				stmt.registerOutParameter(1, java.sql.Types.VARCHAR);
				stmt.registerOutParameter(2, java.sql.Types.NUMERIC);
				int status = 0;
				do
				{
					stmt.execute();
					String msg = stmt.getString(1);
					status = stmt.getInt(2);

					_logger.debug("Oracle/DB2 DBMS_OUTPUT.GET_LINE: status=" + status + ", msg='" + msg + "'.");
					if (msg != null)
						_resultCompList.add(new JDbmsOuputMessage(msg, sql, _connectedToProductName));

				} while (status == 0);
				_logger.debug("End of Oracle/DB2 DBMS_OUTPUT!");
				stmt.close();
			}
			catch (Exception e)
			{
				_logger.warn("Problem occurred during dump of Oracle DBMS_OUTPUT. Caught: " + e);
			}
		} // end: Getting Oracle: DBMS_OUTPUT

	}

	/** Handle Exceptions from various Vendors to do MARK UP ERRORS 
	 * @param resultCompList */
	private void errorReportingVendorSqlException(SQLException ex, ArrayList<JComponent> resultCompList, int startRowInSelection, int scriptReaderSqlBatchStartLine, String originSql)
	{
		@SuppressWarnings("unchecked")
		ArrayList<JAseMessage> errorInfo = (ArrayList<JAseMessage>) _query_txt.getDocument().getProperty(ParserProperties.DB_MESSAGES);

		if (errorInfo == null)
			errorInfo = new ArrayList<JAseMessage>();

		// HANA
		if (DbUtils.DB_PROD_NAME_HANA.equals(_connectedToProductName))
		{
			int    line       = -1;
			int    col        = -1;
			String objectName = null;
			String objectText = null;

			String msg = ex.getMessage();
			int linePos = msg.indexOf(" line ");
			if (linePos > 0)
			{
				// parse the line number in catch block, it might fail.
				try
				{
					// Not sure how to parse/get the proc name
					//String procName = null;

					String tmp = msg.substring(linePos + " line ".length());
					line = Integer.parseInt( StringUtil.word(tmp, 0) );
					col  = Integer.parseInt( StringUtil.word(tmp, 2) );

					// If the message string points to a line, but the number of lines
					// in the SQL text, then this is most probably a procedure/function call
					// Alternative aproach is to "parse" the SQL statement to figure out if it's a create proc
					// which is done in the ASE CASE (see method putSqlWarningMsgs())
					// So lets reset the line/col information
					if ( line > StringUtil.countLines(originSql) )
					{
						// Ok, maybe parse the input to figgure out the procedure name, then download
						//proc source and attach that to the tooltip (indicating the line with red font...
						if (_getObjectTextOnError_chk.isSelected())
						{
							objectName = DbUtils.parseHanaMessageForProcName(msg);
							objectText = DbUtils.getHanaObjectText(_conn, objectName);
							objectText = StringUtil.markTextAtLine(objectText, line, true);
						}

						// Reset the line, "first" row will be marked instead.
						line = -1;
						col  = -1;
					}

					// Finally, add the position in the file 
					if ( line > 0 )
						line = line + startRowInSelection + scriptReaderSqlBatchStartLine;
					if (col > 0)
						col = col - 1;
				}
				catch (NumberFormatException nfe)
				{
					_logger.error("Problems extracting line/column from a HANA Exception", nfe);
				}
			}
			if (line < 0)
				line = startRowInSelection + scriptReaderSqlBatchStartLine + DbUtils.getLineForFirstStatement(originSql);
			JSQLExceptionMessage exMsg = new JSQLExceptionMessage(ex, _connectedToProductName, line, col, originSql, objectText, _query_txt); 

			errorInfo.add(exMsg);
			resultCompList.add(exMsg);
		}
		else
		{
			int line = startRowInSelection + scriptReaderSqlBatchStartLine + DbUtils.getLineForFirstStatement(originSql);
			JSQLExceptionMessage exMsg = new JSQLExceptionMessage(ex, _connectedToProductName, line, -1, originSql, null, _query_txt);

			errorInfo.add(exMsg);
			resultCompList.add(exMsg);
		}

		_query_txt.getDocument().putProperty(ParserProperties.DB_MESSAGES, errorInfo);
		
		// do the "repaint" of the GUI later
//		for (int p=0; p<_query_txt.getParserCount(); p++)
//			try { _query_txt.forceReparsing(p); } catch (Throwable ignore) {} // protect from errors in RSyntaxTextArea
	}


	private Component createPlainRsTextArea(JPlainResultSet plainRs)
	{
		String text = plainRs.getText();

//		boolean hasHtml = false;
//		if (text.indexOf("<html") >= 0 || text.indexOf("<HTML") >= 0)
//			hasHtml = true;
//		if (text.indexOf("<!doctype html") >= 0 || text.indexOf("<!DOCTYPE HTML") >= 0)
//			hasHtml = true;
//		if (hasHtml)
//		{
//			return createHtmlMessage(new StringBuilder(text), false);
//		}

		// TextArea
		RSyntaxTextAreaX textArea = new RSyntaxTextAreaX(text);
		textArea.setHighlightCurrentLine(false);

		boolean hasXml = text.indexOf("<?xml ") >= 0;
		if (hasXml)
		{
			textArea.setCodeFoldingEnabled(true);
			textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
		}
		else
		{
			textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
		}

		RSyntaxUtilitiesX.installRightClickMenuExtentions(textArea, null, _window);
		installResultTextExtraMenuEntries(textArea);
		return textArea;
	}

	private boolean doConvertToXmlTextPane(JTable jtable)
	{
		if (jtable == null)               return false;
		if (jtable.getColumnCount() != 1) return false;
		if (jtable.getRowCount()    != 1) return false;
		
		Object val = jtable.getValueAt(0, 0);
		if ( val == null )               return false;
		if ( ! (val instanceof String) ) return false;

		return ((String)val).startsWith("<?xml ") || ((String)val).startsWith("<?XML ");
	}
	private RSyntaxTextArea createXmlTextPane(JTable jtable)
	{
		RSyntaxTextAreaX out = new RSyntaxTextAreaX();
		RSyntaxUtilitiesX.installRightClickMenuExtentions(out, _resPanelTextScroll, _window);
		installResultTextExtraMenuEntries(out);

		out.setCodeFoldingEnabled(true);
		out.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);

		Object val = jtable.getValueAt(0, 0);
		
		out.append("" + val);
		
		return out;
	}

	private JEditorPane createHtmlMessage(StringBuilder sb, boolean resetStringBuilder)
	{
		JEditorPane editorPane = new JEditorPane();
		editorPane.setEditable(false);
//		editorPane.setContentType("text/html");

		// add a HTMLEditorKit to the editor pane
		HTMLEditorKit kit = new HTMLEditorKit();
		editorPane.setEditorKit(kit);

		// add some styles to the html
//		StyleSheet styleSheet = kit.getStyleSheet();
//		styleSheet.addRule(".condtiming { display: none; position: absolute; width: 100% }");
//		styleSheet.addRule("TD { font-family: sans-serif; font-size: 10 }");

		// create a document, set it on the jeditorpane, then add the html
		Document doc = kit.createDefaultDocument();
		editorPane.setDocument(doc);

		String htmlText = sb.toString();
		
		// Remove strange HTML StyleSheet options
		if (Configuration.getCombinedConfiguration().getBooleanProperty("htmlPlan.styleSheet.remove.condtiming", true))
		{
			String replaceStr = ".condtiming { display: none; position: absolute; width: 100% }";
			if (htmlText.indexOf(replaceStr) > 0)
				htmlText = htmlText.replace(replaceStr, "");
		}
		if (Configuration.getCombinedConfiguration().getBooleanProperty("htmlPlan.styleSheet.remove.TD", true))
		{
			String replaceStr = "TD { font-family: sans-serif; font-size: 10 }";
			if (htmlText.indexOf(replaceStr) > 0)
				htmlText = htmlText.replace(replaceStr, "");
		}

			
		editorPane.setText(htmlText);
		
		if (resetStringBuilder)
			sb.setLength(0);

		return editorPane;
	}

	private void installResultTextExtraMenuEntries(final RSyntaxTextArea textArea)
	{
		JPopupMenu menu =textArea.getPopupMenu();
		JMenuItem mi;
		
		//--------------------------------
		// EXECUTE SELECTED TEXT
		if (textArea != null)
		{
			mi = new JMenuItem("Execute Selected Text");
			mi.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					String cmd = textArea.getSelectedText();
					if (cmd == null)
					{
						SwingUtils.showInfoMessage(_window, "Nothing to execute", "You need to select/mark some text that you want to execute.");
						return;
					}
					// Get the line number where the selection started
					int selectedTextStartAtRow = 0;
					try { selectedTextStartAtRow = textArea.getLineOfOffset(textArea.getSelectionStart()); }
					catch (BadLocationException ignore) {}

					displayQueryResults(cmd, selectedTextStartAtRow, false);
				}
			});
			menu.insert(mi, 0);
		}

		//--------------------------------
		menu.insert(new JPopupMenu.Separator(), 1);
	}

	private int countTables(ArrayList<JComponent> list)
	{
		int count = 0;
		for (JComponent jcomp: list)
		{
			if (jcomp instanceof JTable)
				count++;

			if (jcomp instanceof JPlainResultSet)
				count++;
		}
		return count;
	}

	/*----------------------------------------------------------------------
	** BEGIN: various output classes
	**----------------------------------------------------------------------*/ 
	public static class JAseMessage 
	extends JTextArea
	{
		private static final long serialVersionUID = 1L;
		
		private int    _msgNum      = -1;
		private String _msgText     = null;
		private int    _msgSeverity = -1;
		private int    _scriptRow   = -1;
		private int    _scriptCol   = -1;
		private String _originSql   = null;
		private String _objectText  = null; // IF stored procedure/function is passed

		private RTextArea _sqlTextArea;

//		public JAseMessage()
//		{
//			_init();
//		}

		public JAseMessage(final String aseMsg, String originSql)
		{
			super(aseMsg);
			_originSql   = originSql;
			init();
		}

		public JAseMessage(String aseMsg, int msgNum, String msgText, int msgSeverity, int scriptRow, String originSql, RTextArea sqlTextArea)
		{
			this(aseMsg, msgNum, msgText, msgSeverity, scriptRow, -1, originSql, null, sqlTextArea);
		}

		public JAseMessage(String aseMsg, int msgNum, String msgText, int msgSeverity, int scriptRow, String originSql, String objectText, RTextArea sqlTextArea)
		{
			this(aseMsg, msgNum, msgText, msgSeverity, scriptRow, -1, originSql, objectText, sqlTextArea);
		}

		public JAseMessage(String aseMsg, int msgNum, String msgText, int msgSeverity, int scriptRow, int scriptCol, String originSql, RTextArea sqlTextArea)
		{
			this(aseMsg, msgNum, msgText, msgSeverity, scriptRow, scriptCol, originSql, null, sqlTextArea);
		}

		public JAseMessage(String aseMsg, int msgNum, String msgText, int msgSeverity, int scriptRow, int scriptCol, String originSql, String objectText, RTextArea sqlTextArea)
		{
			super(aseMsg);
			_msgNum      = msgNum;
			_msgText     = msgText;
			_msgSeverity = msgSeverity;
			_scriptRow   = scriptRow; // can be used to draw an red underline in the TextArea
			_scriptCol   = scriptCol; // can be used to draw an red underline in the TextArea
			_originSql   = originSql;
			_objectText  = objectText;
			_sqlTextArea = sqlTextArea;
			init();
//System.out.println("JAseMessage: msgNum="+msgNum+", msgSeverity="+msgSeverity+", msgText='"+msgText+"', aseMsg='"+aseMsg+"'.");
		}

		protected void init()
		{
			super.setEditable(false);

			if (StringUtil.hasValue(_originSql))
				ToolTipManager.sharedInstance().registerComponent(this);

			if (_aseMsgFont == null)
				_aseMsgFont = new Font("Courier", Font.PLAIN, 12);
			setFont(_aseMsgFont);

			if (_msgSeverity > 10)
				setForeground(ColorUtils.DARK_RED);

			setLineWrap(true);
			setWrapStyleWord(true);
//			setOpaque(false); // Transparent

			// install: GO-TO row when you inter the field
			if (_scriptRow > 0)
			{
				this.addMouseListener(new MouseListener()
				{
					@Override public void mouseReleased(MouseEvent e) {}
					@Override public void mousePressed(MouseEvent e) {}
					@Override public void mouseExited(MouseEvent e) {}
					@Override public void mouseEntered(MouseEvent e) {}
					
					@Override 
					public void mouseClicked(MouseEvent e) 
					{
						if (_sqlTextArea == null)
							return;

						try
						{
							// Move to correct line in SQL Text
							_sqlTextArea.setCaretPosition(_sqlTextArea.getLineStartOffset(_scriptRow - 1));
							RXTextUtilities.possiblyMoveLineInScrollPane(_sqlTextArea);

							// Unmark all messages
							Container parent = getParent();
							if (parent instanceof JPanel)
							{
								JPanel panel = (JPanel)parent;
								for (int i=0; i<panel.getComponentCount(); i++)
								{
									Component comp = panel.getComponent(i);
									if (comp instanceof JAseMessage)
									{
										JAseMessage msg = (JAseMessage)comp;
										msg.setBackground(Color.WHITE); //FIXME: Maybe not hardcode this color, get it from UI
									}
								}
							}
							// Mark the message
							setBackground(DEFAULT_OUTPUT_ERROR_HIGHLIGHT_COLOR);
						}
						catch (BadLocationException ble)
						{ // Never happens
							UIManager.getLookAndFeel().provideErrorFeedback(_sqlTextArea);
							ble.printStackTrace();
						}
					}
				});
			}
		}

		@Override
		public String getToolTipText()
		{
			if (StringUtil.isNullOrBlank(_originSql))
				return null;

			StringBuilder sb = new StringBuilder();
			sb.append("<html>");
			sb.append("<pre>");
			sb.append(getOriginSql()); // Show Origin SQL, on tooltip
			sb.append("</pre>");
			if (_objectText != null)
			{
				sb.append("<b>Procedure/Function Definition</b> Trying to mark error line in red.<br>");
				sb.append("<pre>");
				sb.append(getObjectText());
				sb.append("</pre>");
			}
			sb.append("<html>");
			return sb.toString();
		}

		protected boolean hasHtmlStartTag()
		{
			// Actually it looks like Msg=6248 is used as a message number for this messages... but lets keep this below login (ordinary set showplan etc uses the same message)
			return _msgSeverity <= 10 && _msgText != null && _msgText.startsWith("<HTML>");
		}

		protected boolean hasHtmlEndTag()
		{
			// Actually it looks like Msg=6248 is used as a message number for this messages... but lets keep this below login
			return _msgSeverity <= 10 && _msgText != null && _msgText.indexOf("</HTML>") > 0;
		}

		public String getFullMsgText() { return super.getText(); }
		public String getMsgText()     { return _msgText; }
		public int    getMsgNum()      { return _msgNum; }
		public int    getMsgSeverity() { return _msgSeverity; }
		public int    getScriptRow()   { return _scriptRow; }
		public int    getScriptCol()   { return _scriptCol; }
		public String getOriginSql()   { return _originSql; }
		public String getObjectText()  { return _objectText; }

		public void   setOriginSql (String sql) { _originSql  = sql; }
		public void   setObjectText(String str) { _objectText = str; }

		public String getFullMsgTextHtml()
		{
			StringBuilder sb = new StringBuilder();
			
			sb.append("<html>");
			
			sb.append("<pre>");
			sb.append(getFullMsgText());
			sb.append("</pre>");
			sb.append("<br>");
			sb.append("<b>Note:</b> The marked red lines will disappear as soon as you change the text in the editor.");
			
			if (_objectText != null)
			{
				sb.append("<br><br>");
				sb.append("<b>Procedure/Function Definition</b> Trying to mark error line in red.<br>");
				sb.append("<pre>");
				sb.append(getObjectText());
				sb.append("</pre>");
			}

			sb.append("</html>");
			
			return sb.toString();
		}
	}

	private static class JAseCancelledResultSet
	extends JAseMessage
	{
		private static final long serialVersionUID = 1L;

		public JAseCancelledResultSet(String originSql)
		{
			super("SQL was cancelled while reading the ResultSet", originSql);
//			init();
			
			setForeground(ColorUtils.DARK_RED);
		}
	}

	private static class JAseLimitedResultSet
	extends JAseMessage
	{
		private static final long serialVersionUID = 1L;

		public JAseLimitedResultSet(int numberOfRows, String originSql)
		{
			super("Reading the ResultSet was stopped after "+numberOfRows+" rows.", originSql);
//			init();

			setForeground(ColorUtils.DARK_RED);
		}
	}
	
	private static class JAseRowCount 
	extends JAseMessage
	{
		private static final long serialVersionUID = 1L;

		private int _rowCount;

		public JAseRowCount(final int rowCount, String originSql)
		{
			super("(" + rowCount + " rows affected)", originSql);
			_rowCount = rowCount;
//			init();
			
			setForeground(ColorUtils.VERY_DARK_GREEN);
		}
		
		@SuppressWarnings("unused")
		public int getRowCount()
		{
			return _rowCount;
		}
	}

	private static class JAseProcRetCode 
	extends JAseMessage
	{
		private static final long serialVersionUID = 1L;

		private int _returnCode;

		public JAseProcRetCode(final int returnCode, String originSql)
		{
			super("(return status = "+returnCode+")", originSql);
			_returnCode = returnCode;
//			init();
			
			setForeground(ColorUtils.VERY_DARK_GREEN);
		}
		
		@SuppressWarnings("unused")
		public int getReturnCode()
		{
			return _returnCode;
		}
	}

	private static class JAseProcRetParam 
	extends JAseMessage
	{
		private static final long serialVersionUID = 1L;

		private int    _pos;
		private Object _val;
		private int    _sqlType; // java.sql.Types

		private static String toValue(Object val)
		{
			if (val == null)
				return ResultSetTableModel.DEFAULT_NULL_REPLACE; // ResultSetTableModel.NULL_REPLACE;
			
			if (val instanceof String)
				return "'" + val + "'";

			return val + "";
		}
		public JAseProcRetParam(final int pos, final Object val, final int type, String originSql)
		{
			super("RPC Return parameter: pos="+pos+", value="+toValue(val), originSql);
			_pos     = pos;
			_val     = val;
			_sqlType = type;
//			init();
			
			setForeground(ColorUtils.VERY_DARK_GREEN);
		}
		
		@SuppressWarnings("unused")
		public Object getValue()
		{
			return _val;
		}
		@SuppressWarnings("unused")
		public int getPosition()
		{
			return _pos;
		}
		@SuppressWarnings("unused")
		public int getType()
		{
			return _sqlType;
		}
	}

	private static class JClientExecTime 
	extends JAseMessage
	{
		private static final long serialVersionUID = 1L;

		private long _execStartTime;
		private long _execStopTime;
		private long _execFinnishTime;

		public JClientExecTime(final long execStartTime, final long execStopTime, final long execFinnishTime, int atLine, String originSql)
		{
			super("Client Exec Time: " + TimeUtils.msToTimeStr( "%MM:%SS.%ms", execFinnishTime - execStartTime) 
					+ " (sqlExec="     + TimeUtils.msToTimeStr( "%MM:%SS.%ms", execStopTime    - execStartTime)
					+ ", readResults=" + TimeUtils.msToTimeStr( "%MM:%SS.%ms", execFinnishTime - execStopTime)
					+ "), at '"+(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()))
					+"', for SQL starting at Line "+atLine, originSql );

			_execStartTime   = execStartTime;
			_execStopTime    = execStopTime;
			_execFinnishTime = execFinnishTime;

//			init();
			
			setForeground(ColorUtils.VERY_DARK_GREEN);
		}
		
		@SuppressWarnings("unused") public Object getExecStartTime()   { return _execStartTime; }
		@SuppressWarnings("unused") public Object getExecStopTime()    { return _execStopTime; }
		@SuppressWarnings("unused") public Object getExecFinnishTime() { return _execFinnishTime; }
	}

	private static class JDbmsOuputMessage
	extends JAseMessage
	{
		private static final long serialVersionUID = 1L;
		
		private static String getDbmsType(String connectedToProductName)
		{
			if (DbUtils.isProductName(connectedToProductName, DbUtils.DB_PROD_NAME_ORACLE)) return "Oracle";
			if (DbUtils.isProductName(connectedToProductName, DbUtils.DB_PROD_NAME_DB2_UX)) return "DB2";
			return "Oracle/DB2";
		}

		public JDbmsOuputMessage(String message, String originSql, String connectedToProductName)
		{
			super(getDbmsType(connectedToProductName)+" DBMS_OUTPUT.GET_LINE(): "+message, originSql);

			setForeground(ColorUtils.VERY_DARK_BLUE);
		}
	}

	private static class JSQLExceptionMessage
	extends JAseMessage
	{
		private static final long serialVersionUID = 1L;

		private static String createMsg(SQLException ex, String productName)
		{
			String prodName = productName; 
			if (DbUtils.DB_PROD_NAME_HANA.equals(productName)) prodName = "HANA";

//SQLException xxx = ex;
//while (xxx != null)
//{
//	System.out.println("XXXX:" + prodName + ": ErrorCode "+xxx.getErrorCode()+", SQLState "+xxx.getSQLState()+", ExceptionClass: " + xxx.getClass().getName() + "\n");
//	xxx = xxx.getNextException();
//}
//System.out.println("JSQLExceptionMessage: prodName='"+prodName+"'.");
//ex.printStackTrace();
			return prodName + ": ErrorCode "+ex.getErrorCode()+", SQLState "+ex.getSQLState()+", ExceptionClass: " + ex.getClass().getName() + "\n"
//				+ "("+Version.getAppName()+": The SQL Batch was aborted due to a thrown SQLException)\n"
				+ ex.getMessage();
		}
		public JSQLExceptionMessage(SQLException ex, String productName, int line, int col, String originSql, String objectText, RTextArea sqlTextArea)
		{
			super(createMsg(ex, productName), ex.getErrorCode(), ex.getMessage(), -1, line, col, originSql, sqlTextArea);
			setObjectText(objectText);
			setForeground(ColorUtils.RED);
		}
	}

	private static class JSentSqlStatement 
	extends JAseMessage
	{
		private static final long serialVersionUID = 1L;

		@SuppressWarnings("unused")
		private int _scriptRow;

		private static String createStr(String sql)
		{
			StringBuilder sb = new StringBuilder();
			Scanner scanner = new Scanner(sql);
			int row = 0;
			while (scanner.hasNextLine()) 
			{
				String str = scanner.nextLine();
				row++;
				sb.append(row).append("> ").append(str);
				if (scanner.hasNextLine())
					sb.append("\n");
			}
			return sb.toString();
		}

		public JSentSqlStatement(String sql, int scriptRow)
		{
			super(createStr(sql), sql);
			_scriptRow = scriptRow;
			
			setForeground(ColorUtils.VERY_DARK_GREEN);
		}
	}

	private static class JResultSetInfo
	extends JAseMessage
	{
		private static final long serialVersionUID = 1L;

		@SuppressWarnings("unused")
		private int _scriptRow;

		private static String createStr(ResultSetTableModel rstm)
		{
			return rstm.getResultSetInfo();
		}

		public JResultSetInfo(ResultSetTableModel rstm, String sql, int scriptRow)
		{
			super(createStr(rstm), sql);
			_scriptRow = scriptRow;
			
			setForeground(ColorUtils.VERY_DARK_GREEN);
		}
	}
	
	private static class JPlainResultSet 
	extends JTextArea
	{
		private static final long serialVersionUID = 1L;

		ResultSetTableModel _tm = null;

		public JPlainResultSet(final ResultSetTableModel rstm)
		{
			super(rstm.toTableString());
			_tm = rstm;
			init();
		}

		public int getRowCount()
		{
			return _tm.getRowCount();
		}

		protected void init()
		{
			super.setEditable(false);

			if (_aseMsgFont == null)
				_aseMsgFont = new Font("Courier", Font.PLAIN, 12);
			setFont(_aseMsgFont);

			setLineWrap(true);
			setWrapStyleWord(true);
//			setOpaque(false); // Transparent
		}

//		public boolean isFocusable()
//		{
//			return false;
//		}
//
//		public boolean isRequestFocusEnabled()
//		{
//			return false;
//		}
	}
	/*----------------------------------------------------------------------
	** END: various output classes
	**----------------------------------------------------------------------*/ 


	/*----------------------------------------------------------------------
	** BEGIN: Statistics
	**----------------------------------------------------------------------*/ 
	// NOTE: this is not yet used, but in the future... to send of when diconnects or exists
	private int  _execMainCount     = 0;
	private int  _execBatchCount    = 0;
	private long _execTimeTotal     = 0;
	private long _execTimeSqlExec   = 0;
	private long _execTimeRsRead    = 0;
	private int  _rsCount           = 0;
	private int  _rsRowsCount       = 0;
	private int  _iudRowsCount      = 0;
	private int  _sqlWarningCount   = 0;
	private int  _sqlExceptionCount = 0;

	public void incExecMainCount()           { _execMainCount++; }
	public void incExecBatchCount()          { _execBatchCount++; }
	public void incRsCount()                 { _rsCount++; }
	public void incRsRowsCount()             { _rsRowsCount++; }
	public void incIudRowsCount()            { _iudRowsCount++; }
	public void incSqlWarningCount()         { _sqlWarningCount++; }
	public void incSqlExceptionCount()       { _sqlExceptionCount++; }

	public void incExecMainCount    (int  c) { _execMainCount     += c; }
	public void incExecBatchCount   (int  c) { _execBatchCount    += c; }
	public void incExecTimeTotal    (long t) { _execTimeTotal     += t; }
	public void incExecTimeSqlExec  (long t) { _execTimeSqlExec   += t; }
	public void incExecTimeRsRead   (long t) { _execTimeRsRead    += t; }
	public void incRsCount          (int  c) { _rsCount           += c; }
	public void incRsRowsCount      (int  c) { _rsRowsCount       += c; }
	public void incIudRowsCount     (int  c) { _iudRowsCount      += c; }
	public void incSqlWarningCount  (int  c) { _sqlWarningCount   += c; }
	public void incSqlExceptionCount(int  c) { _sqlExceptionCount += c; }

	public int  getExecMainCount()           { return _execMainCount; }
	public int  getExecBatchCount()          { return _execBatchCount; }
	public long getExecTimeTotal()           { return _execTimeTotal; }
	public long getExecTimeSqlExec()         { return _execTimeSqlExec; }
	public long getExecTimeRsRead()          { return _execTimeRsRead; }
	public int  getRsCount()                 { return _rsCount; }
	public int  getRsRowsCount()             { return _rsRowsCount; }
	public int  getIudRowsCount()            { return _iudRowsCount; }
	public int  getSqlWarningCount()         { return _sqlWarningCount; }
	public int  getSqlExceptionCount()       { return _sqlExceptionCount; }

	public void resetExecStatistics()
	{
		_execMainCount     = 0;
		_execBatchCount    = 0;
		_execTimeTotal     = 0;
		_execTimeSqlExec   = 0;
		_execTimeRsRead    = 0;
		_rsCount           = 0;
		_rsRowsCount       = 0;
		_iudRowsCount      = 0;
		_sqlWarningCount   = 0;
		_sqlExceptionCount = 0;
	}

	public void sendExecStatistics(final boolean blockingCall)
	{
		// If connectionType isn't initialized, no need to send
		if (_connType < 0)
			return;

		// send of the counters
		/* TODO */
		final SqlwUsageInfo sqlwUsageInfo = new SqlwUsageInfo();

		sqlwUsageInfo.setConnType         (_connType);
		sqlwUsageInfo.setSrvVersionInt    (_srvVersion);
		sqlwUsageInfo.setProductName      (_connectedToProductName);

		sqlwUsageInfo.setConnectTime      (_connectedAtTime);
		sqlwUsageInfo.setDisconnectTime   (System.currentTimeMillis());
		
		sqlwUsageInfo.setExecMainCount    (_execMainCount);
		sqlwUsageInfo.setExecBatchCount   (_execBatchCount);
		sqlwUsageInfo.setExecTimeTotal    (_execTimeTotal);
		sqlwUsageInfo.setExecTimeSqlExec  (_execTimeSqlExec);
		sqlwUsageInfo.setExecTimeRsRead   (_execTimeRsRead);
		sqlwUsageInfo.setRsCount          (_rsCount);
		sqlwUsageInfo.setRsRowsCount      (_rsRowsCount);
		sqlwUsageInfo.setIudRowsCount     (_iudRowsCount);
		sqlwUsageInfo.setSqlWarningCount  (_sqlWarningCount);
		sqlwUsageInfo.setSqlExceptionCount(_sqlExceptionCount);

		if (blockingCall)
		{
			CheckForUpdates.sendSqlwCounterUsageInfoNoBlock(sqlwUsageInfo, blockingCall);
		}
		else
		{
			// Create a thread that does this...
			// Apparently the noBlockCheckSqlWindow() hits problems when it accesses the CheckForUpdates, which uses ProxyVole
			// My guess is that ProxyVole want's to unpack it's DDL, which takes time...
			Thread checkForUpdatesThread = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					CheckForUpdates.sendSqlwCounterUsageInfoNoBlock(sqlwUsageInfo, blockingCall);
				}
			}, "checkForUpdatesThread");
			checkForUpdatesThread.setDaemon(true);
			checkForUpdatesThread.start();
		}

		// At the end, reset the statistics
		resetExecStatistics();
	}
	/*----------------------------------------------------------------------
	** END: Statistics
	**----------------------------------------------------------------------*/ 


	/*----------------------------------------------------------------------
	** BEGIN: 'COPY RESULTS' Button
	**----------------------------------------------------------------------*/ 
	public final static String  PROPKEY_COPY_RESULTS_ASCII           = PROPKEY_APP_PREFIX + "results.copy.as.ascii";
	public final static boolean DEFAULT_COPY_RESULTS_ASCII           = true;
	
	public final static String  PROPKEY_COPY_RESULTS_HTML            = PROPKEY_APP_PREFIX + "results.copy.as.html";
	public final static boolean DEFAULT_COPY_RESULTS_HTML            = false;
	
	public final static String  PROPKEY_COPY_RESULTS_CSV             = PROPKEY_APP_PREFIX + "results.copy.as.csv";
	public final static boolean DEFAULT_COPY_RESULTS_CSV             = false;

	
	public final static String  PROPKEY_COPY_RESULTS_CSV_HEADERS     = PROPKEY_APP_PREFIX + "results.copy.as.csv.headers";
	public final static boolean DEFAULT_COPY_RESULTS_CSV_HEADERS     = true;

	public final static String  PROPKEY_COPY_RESULTS_CSV_MESSAGES    = PROPKEY_APP_PREFIX + "results.copy.as.csv.messages";
	public final static boolean DEFAULT_COPY_RESULTS_CSV_MESSAGES    = false;

	public final static String  PROPKEY_COPY_RESULTS_CSV_COL_SEP     = PROPKEY_APP_PREFIX + "results.copy.as.csv.col.sep";
	public final static String  DEFAULT_COPY_RESULTS_CSV_COL_SEP     = ",";

	public final static String  PROPKEY_COPY_RESULTS_CSV_ROW_SEP     = PROPKEY_APP_PREFIX + "results.copy.as.csv.row.sep";
	public final static String  DEFAULT_COPY_RESULTS_CSV_ROW_SEP     = "\n";
	
	public final static String  PROPKEY_COPY_RESULTS_CSV_NULL_OUTPUT = PROPKEY_APP_PREFIX + "results.copy.as.csv.replaceNullWithValue";
	public final static String  DEFAULT_COPY_RESULTS_CSV_NULL_OUTPUT = "";

	
	public final static String  PROPKEY_COPY_RESULTS_EXCEL           = PROPKEY_APP_PREFIX + "results.copy.as.excel";
	public final static boolean DEFAULT_COPY_RESULTS_EXCEL           = false;


	private JPopupMenu createCopyResultsPopupMenu(final JButton button)
	{
		// Do PopupMenu
		final JPopupMenu popupMenu = new JPopupMenu();

		popupMenu.addPopupMenuListener(new PopupMenuListener()
		{
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e)
			{
				final Configuration conf    = Configuration.getCombinedConfiguration();
				final Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);

				// remove all old items (if any)
				popupMenu.removeAll();

				final JMenuItem ascii_mi  = new JRadioButtonMenuItem("<html><b>ASCII</b>          - <i><font color=\"green\">Tables will be copied into ascii format         </font></i></html>", conf.getBooleanProperty(PROPKEY_COPY_RESULTS_ASCII, DEFAULT_COPY_RESULTS_ASCII));
				final JMenuItem html_mi   = new JRadioButtonMenuItem("<html><b>HTML</b>           - <i><font color=\"green\">Copy as HTML Tags                               </font></i></html>", conf.getBooleanProperty(PROPKEY_COPY_RESULTS_HTML,  DEFAULT_COPY_RESULTS_HTML));
				final JMenuItem excel_mi  = new JRadioButtonMenuItem("<html><b>Excel</b>          - <i><font color=\"green\">Write to a Excel file                           </font></i></html>", conf.getBooleanProperty(PROPKEY_COPY_RESULTS_EXCEL, DEFAULT_COPY_RESULTS_EXCEL));
				final JMenuItem csv_mi    = new JRadioButtonMenuItem("<html><b>CSV</b>            - <i><font color=\"green\">Commas Separated Values Format                  </font></i></html>", conf.getBooleanProperty(PROPKEY_COPY_RESULTS_CSV,   DEFAULT_COPY_RESULTS_CSV));
				final JMenuItem csvCfg_mi = new JMenuItem           ("<html><b>CSV, Config...</b> - <i><font color=\"green\">Opens a Dialog, where you can set separators etc</font></i></html>");

				ButtonGroup group = new ButtonGroup();
				group.add(ascii_mi);
				group.add(html_mi);
				group.add(excel_mi);
				group.add(csv_mi);

				// Actions
				ActionListener groupActionListener = new ActionListener()
				{
					@Override
					public void actionPerformed(ActionEvent e)
					{
						tmpConf.setProperty(PROPKEY_COPY_RESULTS_ASCII, ascii_mi.isSelected());
						tmpConf.setProperty(PROPKEY_COPY_RESULTS_HTML,  html_mi .isSelected());
						tmpConf.setProperty(PROPKEY_COPY_RESULTS_EXCEL, excel_mi.isSelected());
						tmpConf.setProperty(PROPKEY_COPY_RESULTS_CSV,   csv_mi  .isSelected());
						saveProps();
						
						button.doClick();
					}
				};
				ascii_mi .addActionListener(groupActionListener);
				html_mi  .addActionListener(groupActionListener);
				excel_mi .addActionListener(groupActionListener);
				csv_mi   .addActionListener(groupActionListener);

				// Action for CSV Config
				csvCfg_mi.addActionListener(new ActionListener()
				{
					@Override
					public void actionPerformed(ActionEvent e)
					{
						String key1 = "Column Separator";
						String key2 = "Row Separator";
						String key3 = "NULL Value Replacement";
						String key4 = "Copy Headers";
						String key5 = "Copy Messages, etc";

						LinkedHashMap<String, String> in = new LinkedHashMap<String, String>();
						in.put(key1, Configuration.getCombinedConfiguration().getPropertyRawVal( PROPKEY_COPY_RESULTS_CSV_COL_SEP,     DEFAULT_COPY_RESULTS_CSV_COL_SEP));
						in.put(key2, Configuration.getCombinedConfiguration().getPropertyRawVal( PROPKEY_COPY_RESULTS_CSV_ROW_SEP,     DEFAULT_COPY_RESULTS_CSV_ROW_SEP));
						in.put(key3, Configuration.getCombinedConfiguration().getProperty(       PROPKEY_COPY_RESULTS_CSV_NULL_OUTPUT, DEFAULT_COPY_RESULTS_CSV_NULL_OUTPUT));
						in.put(key4, Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_COPY_RESULTS_CSV_HEADERS,     DEFAULT_COPY_RESULTS_CSV_HEADERS)  + "");
						in.put(key5, Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_COPY_RESULTS_CSV_MESSAGES,    DEFAULT_COPY_RESULTS_CSV_MESSAGES) + "");

						Map<String,String> results = ParameterDialog.showParameterDialog(_window, "CSV Separators", in, false);

						if (results != null)
						{
							tmpConf.setProperty(PROPKEY_COPY_RESULTS_CSV_COL_SEP,     results.get(key1));
							tmpConf.setProperty(PROPKEY_COPY_RESULTS_CSV_ROW_SEP,     results.get(key2));
							tmpConf.setProperty(PROPKEY_COPY_RESULTS_CSV_NULL_OUTPUT, results.get(key3));
							tmpConf.setProperty(PROPKEY_COPY_RESULTS_CSV_HEADERS,     results.get(key4));
							tmpConf.setProperty(PROPKEY_COPY_RESULTS_CSV_MESSAGES,    results.get(key5));
							
							saveProps();
						}
					}
				});

				// Add it to the Code Completion popup menu
				popupMenu.add(ascii_mi);
				popupMenu.add(html_mi);
				popupMenu.add(excel_mi);
				popupMenu.add(csv_mi);
				popupMenu.add(csvCfg_mi);
			}
			@Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {/*empty*/}
			@Override public void popupMenuCanceled(PopupMenuEvent e) {/*empty*/}
		});
		return popupMenu;
	}

	/**
	 * Create a JButton that can enable/disable available Graphs for a specific CounterModel
	 * @param button A instance of JButton, if null is passed a new Jbutton will be created.
	 * @param cmName The <b>long</b> or <b>short</b> name of the CounterModel
	 * @return a JButton (if one was passed, it's the same one, but if null was passed a new instance is created)
	 */
	private JButton createCopyResultsButton(JButton button)
	{
		if (button == null)
			button = new JButton();

//		button.setIcon(SwingUtils.readImageIcon(Version.class, "images/copy_results.png"));
		button.setToolTipText(
				"<html>" +
				"Copy All resultsets to clipboard.<br>" +
				"<b>Note</b>: Right click to choose copy <i>format</i>." +
				"</html>");
		button.setText("Copy Res");

		JPopupMenu popupMenu = createCopyResultsPopupMenu(button);
		button.setComponentPopupMenu(popupMenu);

		// Action for COPY
		button.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				boolean asAscii = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_COPY_RESULTS_ASCII, DEFAULT_COPY_RESULTS_ASCII);
				boolean asHtml  = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_COPY_RESULTS_HTML,  DEFAULT_COPY_RESULTS_HTML);
				boolean asExcel = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_COPY_RESULTS_EXCEL, DEFAULT_COPY_RESULTS_EXCEL);
				boolean asCsv   = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_COPY_RESULTS_CSV,   DEFAULT_COPY_RESULTS_CSV);

				// Check what type we should copy (ASCII is the default)
				StringBuilder sb;
				if      (asAscii) sb = getResultPanelAsAscii(_resPanel);
				else if (asHtml)  sb = getResultPanelAsHtml (_resPanel);
				else if (asExcel) sb = getResultPanelAsExcel(_resPanel);
				else if (asCsv)   sb = getResultPanelAsCsv  (_resPanel);
				else              sb = getResultPanelAsAscii(_resPanel);

				if (sb != null)
				{
					StringSelection data = new StringSelection(sb.toString());
					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					clipboard.setContents(data, data);
				}
			}
		});
		
		return button;
	}

	private StringBuilder getResultPanelAsAscii(JComponent panel)
	{
		StringBuilder sb = new StringBuilder();
//		String terminatorStr = "\n";
//		String terminatorStr = "----------------------------------------------------------------------------\n";

		for (int i=0; i<panel.getComponentCount(); i++)
		{
			Component comp = (Component) panel.getComponent(i);
			if (comp instanceof JPanel)
			{
				sb.append( getResultPanelAsAscii( (JPanel)comp ) );
			}
			else if (comp instanceof JTabbedPane)
			{
				JTabbedPane tp = (JTabbedPane) comp;
				for (int t=0; t<tp.getTabCount(); t++)
				{
					Component tabComp = tp.getComponentAt(t);
					if (tabComp instanceof JComponent)
						sb.append( getResultPanelAsAscii((JComponent)tabComp) );
				}
			}
			else if (comp instanceof JTable)
			{
				JTable table = (JTable)comp;
				String textTable = SwingUtils.tableToString(table);
				sb.append( textTable );
				if ( ! textTable.endsWith("\n") )
					sb.append("\n");
				//sb.append(terminatorStr);
			}
			else if (comp instanceof JEditorPane)
			{
				JEditorPane text = (JEditorPane)comp;
//				sb.append( StringUtil.stripHtml(text.getText()) );

				// text.getText(), will get the actual HTML content and we just want the text
				// so lets copy the stuff into the clipboard and get it from there :)
				// Striping the HTML is an alternative, but that lead to other problems
				text.selectAll();
				text.copy();
				text.select(0, 0);

				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				Transferable clipData = clipboard.getContents(this);
				String strFromClipboard;
				try { strFromClipboard = (String) clipData.getTransferData(DataFlavor.stringFlavor); } 
				catch (Exception ee) { strFromClipboard = ee.toString(); }

				sb.append( strFromClipboard );
				if ( ! strFromClipboard.endsWith("\n") )
					sb.append("\n");
				//sb.append(terminatorStr);
			}
			else if (comp instanceof JTextArea)  // JAseMessage extends JTextArea
			{
				JTextArea text = (JTextArea)comp;
				String str = text.getText();
				sb.append( str );
				if ( ! str.endsWith("\n") )
					sb.append("\n");
				//sb.append(terminatorStr);
			}
			else if (comp instanceof JTableHeader)
			{
				// discard the table header, we get that info in JTable
			}
			else
			{
				String str = comp.toString();
				sb.append( str );
				if ( ! str.endsWith("\n") )
					sb.append("\n");
				//sb.append(terminatorStr);
			}
		}
		return sb;
	}
	private StringBuilder getResultPanelAsHtml(JComponent panel)
	{
		StringBuilder sb = new StringBuilder();

		for (int i=0; i<panel.getComponentCount(); i++)
		{
			Component comp = (Component) panel.getComponent(i);
			if (comp instanceof JPanel)
			{
				sb.append( getResultPanelAsHtml( (JPanel)comp ) );
			}
			else if (comp instanceof JTabbedPane)
			{
				JTabbedPane tp = (JTabbedPane) comp;
				for (int t=0; t<tp.getTabCount(); t++)
				{
					Component tabComp = tp.getComponentAt(t);
					if (tabComp instanceof JComponent)
						sb.append( getResultPanelAsHtml((JComponent)tabComp) );
				}
			}
			else if (comp instanceof JTable)
			{
				JTable table = (JTable)comp;
				String textTable = SwingUtils.tableToHtmlString(table);
				sb.append( textTable );
				if ( ! textTable.endsWith("\n") )
					sb.append("\n");
			}
			else if (comp instanceof JEditorPane)
			{
				JEditorPane text = (JEditorPane)comp;

				// text.getText(), will get the actual HTML content (NOTE, there is most possible <html></html> tags in there)
				// TODO: Should we try to *remove* some of the content and just copy the <body></body> part????
				sb.append( text.getText() );
			}
//			else if (comp instanceof JAseMessage)
//			{
//				JAseMessage msg = (JAseMessage)comp;
//				sb.append( msg.toHtml() ); // toHtml doesnt exists... so let below section (JTextArea) just copy the text for now
//			}
			else if (comp instanceof JTextArea)  // JAseMessage extends JTextArea
			{
				JTextArea text = (JTextArea)comp;
				String str = text.getText();
				sb.append( "<pre>\n" );
				sb.append( str );
				if ( ! str.endsWith("\n") )
					sb.append("\n");
				sb.append( "</pre>\n" );
				sb.append( "<BR>\n" );
			}
			else if (comp instanceof JTableHeader)
			{
				// discard the table header, we get that info in JTable
			}
			else
			{
				String str = comp.toString();
				sb.append( "<pre>\n" );
				sb.append( str );
				if ( ! str.endsWith("\n") )
					sb.append("\n");
				sb.append( "</pre>\n" );
				sb.append( "<BR>\n" );
			}
		}
		return sb;
	}

	private StringBuilder getResultPanelAsExcel(JComponent panel)
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("Copy to Excel is not yet implemented.");

		SwingUtils.showInfoMessage(_window, sb.toString(), sb.toString());
		
		return sb;
	}

	private StringBuilder getResultPanelAsCsv(JComponent panel)
	{
		StringBuilder sb = new StringBuilder();

		boolean headers      = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_COPY_RESULTS_CSV_HEADERS,         DEFAULT_COPY_RESULTS_CSV_HEADERS);
		boolean messages     = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_COPY_RESULTS_CSV_MESSAGES,        DEFAULT_COPY_RESULTS_CSV_MESSAGES);
		String  colSep       = Configuration.getCombinedConfiguration().getPropertyRawVal( PROPKEY_COPY_RESULTS_CSV_COL_SEP,         DEFAULT_COPY_RESULTS_CSV_COL_SEP);
		String  rowSep       = Configuration.getCombinedConfiguration().getPropertyRawVal( PROPKEY_COPY_RESULTS_CSV_ROW_SEP,         DEFAULT_COPY_RESULTS_CSV_ROW_SEP);
		String  tabNullValue = Configuration.getCombinedConfiguration().getProperty(       ResultSetTableModel.PROPKEY_NULL_REPLACE, ResultSetTableModel.DEFAULT_NULL_REPLACE);
		String  outNullValue = Configuration.getCombinedConfiguration().getProperty(       PROPKEY_COPY_RESULTS_CSV_NULL_OUTPUT,     DEFAULT_COPY_RESULTS_CSV_NULL_OUTPUT);

		for (int i=0; i<panel.getComponentCount(); i++)
		{
			Component comp = (Component) panel.getComponent(i);
			if (comp instanceof JPanel)
			{
				sb.append( getResultPanelAsCsv( (JPanel)comp ) );
			}
			else if (comp instanceof JTabbedPane)
			{
				JTabbedPane tp = (JTabbedPane) comp;
				for (int t=0; t<tp.getTabCount(); t++)
				{
					Component tabComp = tp.getComponentAt(t);
					if (tabComp instanceof JComponent)
						sb.append( getResultPanelAsCsv((JComponent)tabComp) );
				}
			}
			else if (comp instanceof JTable)
			{
				JTable table = (JTable)comp;
				String textTable = SwingUtils.tableToCsvString(table, headers, colSep, rowSep, tabNullValue, outNullValue);
				sb.append( textTable );
				sb.append( "\n" ); // append a newline after each resultset, this is good if we have more that one.
			}
			else if (comp instanceof JEditorPane)
			{
				if (messages) // Copy other stuff
				{
					JEditorPane text = (JEditorPane)comp;
	//				sb.append( StringUtil.stripHtml(text.getText()) );
	
					// text.getText(), will get the actual HTML content and we just want the text
					// so lets copy the stuff into the clipboard and get it from there :)
					// Striping the HTML is an alternative, but that lead to other problems
					text.selectAll();
					text.copy();
					text.select(0, 0);
	
					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					Transferable clipData = clipboard.getContents(this);
					String strFromClipboard;
					try { strFromClipboard = (String) clipData.getTransferData(DataFlavor.stringFlavor); } 
					catch (Exception ee) { strFromClipboard = ee.toString(); }
	
					sb.append( strFromClipboard );
					if ( ! strFromClipboard.endsWith("\n") )
						sb.append("\n");
					//sb.append(terminatorStr);
				}
			}
			else if (comp instanceof JTextArea)  // JAseMessage extends JTextArea
			{
				if (messages) // Copy other stuff
				{
					JTextArea text = (JTextArea)comp;
					String str = text.getText();
					sb.append( str );
					if ( ! str.endsWith("\n") )
						sb.append("\n");
					//sb.append(terminatorStr);
				}
			}
			else if (comp instanceof JTableHeader)
			{
				// discard the table header, we get that info in JTable
			}
			else
			{
				if (messages) // Copy other stuff
				{
					String str = comp.toString();
					sb.append( str );
					if ( ! str.endsWith("\n") )
						sb.append("\n");
					//sb.append(terminatorStr);
				}
			}
		}
		return sb;
	}
	/*----------------------------------------------------------------------
	** END: 'COPY RESULTS' Button
	**----------------------------------------------------------------------*/ 


	
	/*----------------------------------------------------------------------
	** BEGIN: set OPTIONS buttin stuff
	**----------------------------------------------------------------------*/ 
	/**
	 * Private helper class for createSetOptionButton()
	 * @author gorans
	 */
	private static class AseOptionOrSwitch
	{
		public static final int SEPARATOR   = 0;
		public static final int TYPE_SET    = 1;
		public static final int TYPE_OPT    = 2;
		public static final int TYPE_SWITCH = 3;
		private int     _type;
		private String  _sqlOn;
		private String  _sqlOff;
		private String  _text;
		private boolean _defVal;
		private String  _tooltip;

		public AseOptionOrSwitch(int type)
		{
			_type    = type;
		}
		public AseOptionOrSwitch(int type, String sqlOn, String sqlOff, String text, boolean defVal, String tooltip)
		{
			_type    = type;
			_sqlOn   = sqlOn;
			_sqlOff  = sqlOff;
			_text    = text;
			_defVal  = defVal;
			_tooltip = tooltip;
		}
		public int     getType()    { return _type; }
		public String  getSqlOn()   { return _sqlOn.replace("ON-OFF", "on"); }
		public String  getSqlOff()  { return (_sqlOff != null ? _sqlOff : _sqlOn).replace("ON-OFF", "off"); }
		public String  getText()    { return _text; }
		public boolean getDefVal()  { return _defVal; }
		public String  getTooltip() { return _tooltip; } 
	}

	
	private JPopupMenu createSetOptionButtonPopupMenu(final int aseVersion)
	{
		ArrayList<AseOptionOrSwitch> options = new ArrayList<AseOptionOrSwitch>();

//		if (aseVersion >= 15020) 
//		if (aseVersion >= 1502000) 
		if (aseVersion >= Ver.ver(15,0,2)) 
		{
			boolean statementCache   = false;
			boolean literalAutoParam = false;
			try
			{
				statementCache   = AseConnectionUtils.getAseConfigRunValue(_conn, "statement cache size") > 0;
				literalAutoParam = AseConnectionUtils.getAseConfigRunValue(_conn, "enable literal autoparam") > 0;
			}
			catch (SQLException ignore) {/*ignore*/}

			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_SET, "set statement_cache ON-OFF",   null, "statement_cache",   statementCache,   "Enable/Disable using a cached query plan from the statement cache, as well as caching the current plan in the statement cache."));
			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_SET, "set literal_autoparam ON-OFF", null, "literal_autoparam", literalAutoParam, "Enable/Disable literal parameterization for current session."));
			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.SEPARATOR));
		}
		options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_SET, "set showplan ON-OFF", null, "showplan", false, "Displays the query plan"));
//		if (aseVersion >= 15030) 
//		if (aseVersion >= 1503000) 
		if (aseVersion >= Ver.ver(15,0,3)) 
			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_SWITCH, "set switch on 3604,9529 with override", "set switch off 3604,9529", "switch 3604,9529", false, "Traceflag 3604,9529: Include Lava operator execution statistics and resource use in a showplan format at most detailed level."));
		options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_SET, "set statistics io ON-OFF",            null, "statistics io",            false, "Number of logical and physical IO's per table"));
		options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_SET, "set statistics time ON-OFF",          null, "statistics time",          false, "Compile time and elapsed time"));
		options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_SET, "set statistics subquerycache ON-OFF", null, "statistics subquerycache", false, "Statistics about internal subquery optimizations"));
//		if (aseVersion >= 15000) 
//		if (aseVersion >= 1500000) 
		if (aseVersion >= Ver.ver(15,0)) 
			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_SET, "set statistics plancost ON-OFF",      null, "statistics plancost",      false, "Query plan in tree format, includes estimated/actual rows and IO's"));
//		if (aseVersion >= 15020) 
//		if (aseVersion >= 1502000) 
		if (aseVersion >= Ver.ver(15,0,2)) 
			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_SET, "set statistics resource ON-OFF",      null, "statistics resource",      false, "Resource usage, includes procedure cache and tempdb"));

//		if (aseVersion >= 1570100)
		if (aseVersion >= Ver.ver(15,7,0,100))
		{
			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.SEPARATOR));

			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_SET, "set statistics plan_html, timing_html, plan_detail_html, parallel_plan_detail_html ON-OFF",
			                                                                                                                 null, "statistics *_html",                    false, "Sets ALL of the below xxx_html settings."));
			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_SET, "set statistics plan_html ON-OFF",                 null, "statistics plan_html",                 false, "Number of rows and number of threads per operator"));
			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_SET, "set statistics timing_html ON-OFF",               null, "statistics timing_html",               false, "Execution statistics related to the timing spent in each operator per execution phase"));
			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_SET, "set statistics plan_detail_html ON-OFF",          null, "statistics plan_detail_html",          false, "Details of plan operators, such as the name, different timestamps captured during the execution, number of rows affected"));
			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_SET, "set statistics parallel_plan_detail_html ON-OFF", null, "statistics parallel_plan_detail_html", false, "Details per thread and plan fragments for query plans that are executed in parallel using severalworked threads"));
			

//			statistics plan_html
//			-----------------------------------
//			The plan_html parameter generates a graphical query plan in HTML format containing information about the number of rows and number of threads per operator. 
//			The syntax is: set statistics plan_html {on | off}
//
//
//			statistics timing_html
//			-----------------------------------
//			The timing_html parameter generates a graphical query plan in HTML format containing execution statistics related to the timing spent in each operator per execution phase. 
//			CPU usage and Wait distribution is generated for queries executed in parallel. 
//			The syntax is: set statistics timing_html {on | off}
//
//
//			statistics plan_detail_html
//			-----------------------------------
//			The plan_detail_html parameter generates a graphical query plan in HTML format containing information details of plan operators, such as the name, different timestamps captured during the execution, number of rows affected,
//			number of estimated rows, elapsed time, and so on. 
//			The syntax is: set statistics plan_detail_html {on | off}
//
//
//			statistics parallel_plan_detail_html
//			-----------------------------------
//			The parallel_plan_detail_html parameter generates a graphical query plan in HTML format containing information about details per thread and plan fragments for query plans that are executed in parallel using severalworked threads. 
//			Use this option to diagnose the behavior of specific threads and plan fragments in comparison with the global execution of the query. 
//			The syntax is: set statistics parallel_ plan_detail_html {on | off}
//
//
//			statistics plan_directory_html
//			-----------------------------------
//			The plan_directory_html parameter specifies the directory path name into which to write the HTML query plans. The file name is identified by a combination of user name, spid, and timestamp. 
//			The syntax is: set statistics plan_directory_html {dirName | on | off}
//			When set to off, the dumping of the HTML data to an external file is stopped.
//			When set to on, the dumping of HTML data to an external file in a directory
//			previously indicated is resumed. No output is generated if a directory name was not previously provided.
		}
		
//		if (aseVersion >= 15020)
//		if (aseVersion >= 1502000)
		if (aseVersion >= Ver.ver(15,0,2))
		{
			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.SEPARATOR));
			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_SWITCH, "set switch ON-OFF 3604", null, "switch 3604", false, "Set traceflag 3604 on|off, <b>the below options needs this</b>."));

			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_OPT, "set option show ON-OFF",               null, "show",               false, "Enable most but not all of the below options collectively"));
			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_OPT, "set option show_lio_costing ON-OFF",   null, "show_lio_costing",   false, "Displays logical IO's estimates (similar to traceflag 302 in pre -15)"));
			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_OPT, "set option show_abstract_plan ON-OFF", null, "show_abstract_plan", false, "Displays the full abstract plan"));
			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_OPT, "set option show_missing_stats ON-OFF", null, "show_missing_stats", false, "Displays a message when statistics are expected but missing"));
			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_OPT, "set option show_histograms ON-OFF",    null, "show_histograms",    false, "Displays information about histograms (for join/SARGs)"));
			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_OPT, "set option show_elimination ON-OFF",   null, "show_elimination",   false, "Displays information about (semantic) partition elimination"));
			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_OPT, "set option show_code_gen ON-OFF",      null, "show_code_gen",      false, "Displays internal diagnostics, incl. reformatting-related info (similar to traceflag 319/321 in pre-15)"));
			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_OPT, "set option show_search_engine ON-OFF", null, "show_search_engine", false, "Displays plan search information (includes info similar to traceflag 310/317 in pre-15"));
			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_OPT, "set option show_parallel ON-OFF",      null, "show_parallel",      false, "Shows details of parallel query optimization"));
			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_OPT, "set option show_pll_costing ON-OFF",   null, "show_pll_costing",   false, "Shows estimates relating to costing for parallel execution"));

			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.SEPARATOR));
			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_OPT, "set option show_best_plan ON-OFF",     null, "show_best_plan",     false, "Shows the details of the best query plan selected by the optimizer"));
			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_OPT, "set option show_counters ON-OFF",      null, "show_counters",      false, "Shows the optimization counters"));
			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_OPT, "set option show_pio_costing ON-OFF",   null, "show_pio_costing",   false, "Shows estimates of physical input/output (reads/writes from/to the disk)"));
			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_OPT, "set option show_log_props ON-OFF",     null, "show_log_props",     false, "Shows the logical properties evaluated"));
			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_OPT, "set option show_lop ON-OFF",           null, "show_lop",           false, "Shows the logical operators used"));
			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_OPT, "set option show_managers ON-OFF",      null, "show_managers",      false, "Shows the data structure managers used during optimization"));

			//---------------------------------------------
			// BEGIN: XML Stuff if we want that in here
			//---------------------------------------------
			// To turn an option on, specify:
			// set plan for 
			//	{show_exec_xml, show_opt_xml, show_execio_xml, show_lop_xml, show_managers_xml, 
			//		 show_log_props_xml, show_parallel_xml, show_histograms_xml, show_final_plan_xml, 
			//		 show_abstract_plan_xml, show_search_engine_xml, show_counters_xml, show_best_plan_xml, 
			//		 show_pio_costing_xml, show_lio_costing_xml, show_elimination_xml}
			//	to {client | message} on


			//	show_exec_xml		Gets the compiled plan output in XML, showing each of the query plan operators.
			//	show_opt_xml		Gets optimizer diagnostic output, which shows the different components such as logical operators, output from the managers, some of the search engine diagnostics, and the best query plan.
			//	show_execio_xml		Gets the plan output along with estimated and actual I/Os. show_execio_xml also includes the query text.
			//	show_lop_xml		Gets the output logical operator tree in XML.
			//	show_managers_xml	Shows the output of the different component managers during the preparation phase of the query optimizer.
			//	show_log_props_xml	Shows the logical properties for a given equivalence class (one or more groups of relations in the query).
			//	show_parallel_xml	Shows the diagnostics related to the optimizer while generating parallel query plans.
			//	show_histograms_xml	Shows diagnostics related to histograms and the merging of histograms.
			//	show_final_plan_xml	Gets the plan output. Does not include the estimated and actual I/Os. show_final_plan_xml includes the query text.
			//	show_abstract_plan_xml	Shows the generated abstract plan.
			//	show_search_engine_xml	Shows diagnostics related to the search engine.
			//	show_counters_xml	Shows plan object construction/destruction counters.
			//	show_best_plan_xml	Shows the best plan in XML.
			//	show_pio_costing_xml	Shows actual physical input/output costing in XML.
			//	show_lio_costing_xml	Shows actual logical input/output costing in XML.
			//	show_elimination_xml	Shows partition elimination in XML.
			//	client			When specified, output is sent to the client. By default, this is the error log. When trace flag 3604 is active, however, output is sent to the client connection.
			//	message			When specified, output is sent to an internal message buffer.
			//
			//
			//	To turn an option off, specify:
			//	set plan for 
			//		{show_exec_xml, show_opt_xml, show_execio_xml, show_lop_xml, show_managers_xml, 
			//		 show_log_props_xml, show_parallel_xml, show_histograms_xml,show_final_plan_xml 
			//		 show_abstract_plan_xml, show_search_engine_xml, show_counters_xml, show_best_plan_xml, 
			//		 show_pio_costing_xml,show_lio_costing_xml, show_elimination_xml} 
			//	off	
			//---------------------------------------------
			// END: XML Stuff if we want that in here
			//---------------------------------------------
		}

		// Do PopupMenu
		final JPopupMenu popupMenu = new JPopupMenu();
//		button.setComponentPopupMenu(popupMenu);
		
		for (AseOptionOrSwitch opt : options)
		{
			// Add Separator
			if (opt.getType() == AseOptionOrSwitch.SEPARATOR)
			{
				popupMenu.add(new JSeparator());
				continue;
			}

			// Add entry
			JCheckBoxMenuItem mi;

			String miText = "<html>set <b>"+opt.getText()+"</b> - <i><font color=\"green\">"+opt.getTooltip()+"</font></i></html>";
			String toolTipText = "<html>"+opt.getTooltip()+"<br>" +
					"<br>" +
					"SQL used to set <b>on</b>: <code>"+opt.getSqlOn()+"</code><br>" +
					"SQL used to set <b>off</b>: <code>"+opt.getSqlOff()+"</code><br>" +
					"</html>";

			mi = new JCheckBoxMenuItem();
			mi.setSelected(opt.getDefVal());
			mi.setText(miText);
//			mi.setActionCommand(opt.getSql());
			mi.setToolTipText(toolTipText);
			mi.putClientProperty(AseOptionOrSwitch.class.getName(), opt);

			mi.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					JCheckBoxMenuItem xmi = (JCheckBoxMenuItem) e.getSource();
					AseOptionOrSwitch opt = (AseOptionOrSwitch) xmi.getClientProperty(AseOptionOrSwitch.class.getName());

					boolean onOff = xmi.isSelected();
					String sql = onOff ? opt.getSqlOn() : opt.getSqlOff();

					_logger.info("Setting: "+sql);

					try
					{
						Statement stmnt = _conn.createStatement();
						stmnt.executeUpdate(sql);
						String wmsg = AseConnectionUtils.getSqlWarningMsgs(stmnt.getWarnings());
						if ( ! StringUtil.isNullOrBlank(wmsg) )
						{
							_logger.info("Change Setting output: "+wmsg);
							stmnt.clearWarnings();
						}
						stmnt.close();
					}
					catch (SQLException ex)
					{
						_logger.warn("Problems execute SQL '"+sql+"', Caught: " + ex.toString() );
						SwingUtils.showErrorMessage("Problems set option", "Problems execute SQL '"+sql+"'\n\n"+ex.getMessage(), ex);
					}
				}
			});
			popupMenu.add(mi);
		}

		return popupMenu;
	}

	/**
	 * Create a JButton that can enable/disable available Graphs for a specific CounterModel
	 * @param button A instance of JButton, if null is passed a new Jbutton will be created.
	 * @param cmName The <b>long</b> or <b>short</b> name of the CounterModel
	 * @return a JButton (if one was passed, it's the same one, but if null was passed a new instance is created)
	 */
	private JButton createSetAseOptionButton(JButton button, final int aseVersion)
	{
		if (button == null)
			button = new JButton();

		button.setIcon(SwingUtils.readImageIcon(Version.class, "images/ase16.png"));
		button.setToolTipText("<html>Set various options, for example: set showplan on|off.</html>");
		button.setText("Set");

		JPopupMenu popupMenu = createSetOptionButtonPopupMenu(aseVersion);
		button.setComponentPopupMenu(popupMenu);

		// If we click on the button, display the popup menu
		button.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Object source = e.getSource();
				if (source instanceof JButton)
				{
					JButton but = (JButton)source;
					JPopupMenu pm = but.getComponentPopupMenu();
					pm.show(but, 14, 14);
					pm.setLocation( MouseInfo.getPointerInfo().getLocation() );
				}
			}
		});
		
		return button;
	}
	/*----------------------------------------------------------------------
	** END: set OPTIONS button stuff
	**----------------------------------------------------------------------*/ 

	/*----------------------------------------------------------------------
	** BEGIN: set Application Option Button
	**----------------------------------------------------------------------*/ 
	private JPopupMenu createAppOptionPopupMenu()
	{
		// Do PopupMenu
		final JPopupMenu popupMenu = new JPopupMenu();
		
//		// Add entry
//		JCheckBoxMenuItem mi;
//
//		mi = new JCheckBoxMenuItem();
//		mi.setSelected(opt.getDefVal());
//		mi.setText(miText);
//		mi.setToolTipText(toolTipText);
//		mi.putClientProperty(AseOptionOrSwitch.class.getName(), opt);

		// ok lets not create new objects, lets resue already created objects
		// But change the text a bit...
		_asPlainText_chk           .setText("<html><b>As Plain Text</b>                 - <i><font color=\"green\">Simulate <b>isql</b> output, do not use GUI Tables</font></i> 'go plain'</html>");
		_appendResults_chk         .setText("<html><b>Append Results</b>                - <i><font color=\"green\">Do <b>not</b> clear results from previous executions. Append at the end.</font></i> 'go append'</html>");
		_showRowCount_chk          .setText("<html><b>Row Count</b>                     - <i><font color=\"green\">Print the rowcount from jConnect and not number of rows returned.</font></i> 'go rowc'</html>");
		_limitRsRowsRead_chk       .setText("SET_LATER: _limitRsRowsRead_chk");
		_limitRsRowsReadDialog_mi  .setText("<html><b>Limit ResultSet, settings...</b>  - <i><font color=\"green\">Open a dialog to change settings for limiting rows</font></i></html>");
		_showSentSql_chk           .setText("<html><b>Print Sent SQL Statement</b>      - <i><font color=\"green\">Print the Executed SQL Statement in the output.</font></i> 'go psql'</html>");
		_printRsInfo_chk           .setText("<html><b>Print ResultSet Info</b>          - <i><font color=\"green\">Print Info about the ResultSet in the output.</font></i> 'go prsi'</html>");
		_rsTrimStrings_chk         .setText("<html><b>Trim String values</b>            - <i><font color=\"green\">Do you want to remove leading/trailing blanks from \"strings\"</html>");
		_rsShowRowNumber_chk       .setText("<html><b>Show Row Number</b>               - <i><font color=\"green\">Add a Row Number as first column '"+ResultSetTableModel.ROW_NUMBER_COLNAME+"' when displaying data</html>");
		_clientTiming_chk          .setText("<html><b>Client Timing</b>                 - <i><font color=\"green\">How long does a SQL Statement takes from the clients perspective.</font></i> 'go time'</html>");
		_useSemicolonHack_chk      .setText("<html><b>Use Semicolon to Send</b>         - <i><font color=\"green\">Use semicolon ';' at the end of a line to send SQL to Server.</font></i></html>");
		_enableDbmsOutput_chk      .setText("<html><b>Enable dbms_output.get_line</b>   - <i><font color=\"green\">Receive Oracle/DB2 DBMS Output trace statements.</font></i></html>");
		_rsInTabs_chk              .setText("<html><b>Resultset in Tabs</b>             - <i><font color=\"green\">Use a GUI Tabed Pane for each Resultset</font></i></html>");
		_getObjectTextOnError_chk  .setText("<html><b>Show Proc Text on errors</b>      - <i><font color=\"green\">Show proc source code in error message</font></i></html>");
		_jdbcAutoCommit_chk        .setText("<html><b>JDBC AutoCommit</b>               - <i><font color=\"green\">Enable/disable AutoCommit in JDBC</font></i></html>");
		_sqlBatchTermDialog_mi     .setText("SET_LATER: _sqlBatchTermDialog_mi");

		// For dialogs set special icon
		_limitRsRowsReadDialog_mi.setIcon(SwingUtils.readImageIcon(Version.class, "images/settings_dialog_12.png"));
		_sqlBatchTermDialog_mi   .setIcon(SwingUtils.readImageIcon(Version.class, "images/settings_dialog_12.png"));
		
		popupMenu.add(_asPlainText_chk);
		popupMenu.add(_appendResults_chk);
		popupMenu.add(_showRowCount_chk);
		popupMenu.add(_limitRsRowsRead_chk);
		popupMenu.add(_limitRsRowsReadDialog_mi);
		popupMenu.add(_showSentSql_chk);
		popupMenu.add(_printRsInfo_chk);
		popupMenu.add(_rsTrimStrings_chk);
		popupMenu.add(_rsShowRowNumber_chk);
		popupMenu.add(_clientTiming_chk);
		popupMenu.add(_useSemicolonHack_chk);
		popupMenu.add(_sqlBatchTermDialog_mi);
		popupMenu.add(_enableDbmsOutput_chk);
		popupMenu.add(_rsInTabs_chk);
		popupMenu.add(_getObjectTextOnError_chk);
		popupMenu.add(_jdbcAutoCommit_chk);
//		popupMenu.add(new JSeparator());
		
		// Set default visibility
		_enableDbmsOutput_chk.setVisible(false);

		// Action MenuItem: _limitRsRowsReadDialog_mi
		_limitRsRowsReadDialog_mi.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				final Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);

				String key1 = "Number of rows";

				LinkedHashMap<String, String> in = new LinkedHashMap<String, String>();
				in.put(key1, Configuration.getCombinedConfiguration().getProperty( PROPKEY_limitRsRowsReadCount, DEFAULT_limitRsRowsReadCount+""));

				Map<String,String> results = ParameterDialog.showParameterDialog(_window, "Limit ResultSet", in, false);

				if (results != null)
				{
					tmpConf.setProperty(PROPKEY_limitRsRowsReadCount, Integer.parseInt(results.get(key1)));

					saveProps();
				}
			}
		});
		
		// Action MenuItem: _sqlBatchTermDialog_mi
		_sqlBatchTermDialog_mi.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				final Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);

				String key1 = "SQL Batch Terminator";

				LinkedHashMap<String, String> in = new LinkedHashMap<String, String>();
				in.put(key1, Configuration.getCombinedConfiguration().getProperty( PROPKEY_sqlBatchTerminator, DEFAULT_sqlBatchTerminator));

				Map<String,String> results = ParameterDialog.showParameterDialog(_window, "SQL Batch Terminator", in, false);

				if (results != null)
				{
					tmpConf.setProperty(PROPKEY_sqlBatchTerminator, results.get(key1));

					saveProps();
				}
			}
		});
		
		// Action CheckBox: _rsTrimStrings_chk
		_rsTrimStrings_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				saveProps();
			}
		});
		
		// Action CheckBox: _rsShowRowNumber_chk
		_rsShowRowNumber_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				saveProps();
			}
		});
		
		// On OPEN the popup we might want to change some stuff
		// This is done here
		popupMenu.addPopupMenuListener(new PopupMenuListener()
		{
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e)
			{
				Component[] compArray = popupMenu.getComponents();
				for (Component comp : compArray)
				{
					if (_limitRsRowsRead_chk.equals(comp))
					{
						String config = Configuration.getCombinedConfiguration().getProperty(PROPKEY_limitRsRowsReadCount, DEFAULT_limitRsRowsReadCount+"");
						String label  = "<html><b>Limit ResultSet to "+config+" rows</b> - <i><font color=\"green\"><b>Stop</b> reading the ResultSet after <b>"+config+"</b> rows.</font></i> 'go top "+config+"'</html>";
						_limitRsRowsRead_chk.setText(label);
					}
					if (_sqlBatchTermDialog_mi.equals(comp))
					{
						String config = Configuration.getCombinedConfiguration().getProperty(PROPKEY_sqlBatchTerminator, DEFAULT_sqlBatchTerminator);
						String label  = "<html><b>Change SQL Batch 'Send' Terminator...</b> - <i><font color=\"green\">Current terminator is '<b>"+config+"</b>'</font></i></html>";
						_sqlBatchTermDialog_mi.setText(label);
					}
				}
			}
			
			@Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {/*empty*/}
			@Override public void popupMenuCanceled(PopupMenuEvent e) {/*empty*/}
		});
		
		
		
//		// Menu items for Code Completion
//		//---------------------------------------------------
//		JMenu codeCompl_m = new JMenu("<html><b>Code Completion/Assist</b> - <i><font color=\"green\">Use <code><b>Ctrl+Space</b></code> to get Code Completion</font></i></html>");
//		popupMenu.add(codeCompl_m);
//		
//		// When the Code Completion popup becoms visible, the menu are refreshed/recreated
//		final JPopupMenu codeComplPopupMenu = codeCompl_m.getPopupMenu();
//		codeComplPopupMenu.addPopupMenuListener(new PopupMenuListener()
//		{
//			@Override
//			public void popupMenuWillBecomeVisible(PopupMenuEvent e)
//			{
//				// remove all old items (if any)
//				codeComplPopupMenu.removeAll();
//
//				JMenuItem cc_reset_mi = new JMenuItem("<html><b>Clear</b> - <i><font color=\"green\">Clear the in memory cache for the Code Completion.</font></i></html>");
//
//				JMenuItem cc_stat_mi  = new JCheckBoxMenuItem("<html><b>Static Commands</b>                   - <i><font color=\"green\">Get Static Commands or Templates that can be used <code><b>Ctrl+Space</b><code/></font></i></html>", _compleationProviderAbstract.isLookupStaticCmds());
//				JMenuItem cc_misc_mi  = new JCheckBoxMenuItem("<html><b>Miscelanious</b>                      - <i><font color=\"green\">Get Miscelanious Info, like ASE Monitoring tables</font></i></html>",                                _compleationProviderAbstract.isLookupMisc());
//				JMenuItem cc_db_mi    = new JCheckBoxMenuItem("<html><b>Database Info x</b>                   - <i><font color=\"green\">Get Database Info, prev word is <code><b>use</b><code/></font></i></html>",                          _compleationProviderAbstract.isLookupDb());
//				JMenuItem cc_tn_mi    = new JCheckBoxMenuItem("<html><b>Table Name Info x</b>                 - <i><font color=\"green\">Get Table Name Info, use <code><b>Ctrl+Space</b><code/></font></i></html>",                          _compleationProviderAbstract.isLookupTableName());
//				JMenuItem cc_tc_mi    = new JCheckBoxMenuItem("<html><b>Table Column Info x</b>               - <i><font color=\"green\">Get Table Column Info, current word start with <code><b>tableAlias.</b><code/></font></i></html>",   _compleationProviderAbstract.isLookupTableColumns());
//				JMenuItem cc_pn_mi    = new JCheckBoxMenuItem("<html><b>Procedure Info x</b>                  - <i><font color=\"green\">Get Procedure Info, prev word is <code><b>exec</b><code/></font></i></html>",                        _compleationProviderAbstract.isLookupProcedureName());
//				JMenuItem cc_pp_mi    = new JCheckBoxMenuItem("<html><b>Procedure Parameter Info x</b>        - <i><font color=\"green\">Get Procedure Parameter Info</font></i></html>",                                                     _compleationProviderAbstract.isLookupProcedureColumns());
//				JMenuItem cc_spn_mi   = new JCheckBoxMenuItem("<html><b>System Procedure Info x</b>           - <i><font color=\"green\">Get System Procedure Info, prev word is <code><b>exec sp_</b><code/></font></i></html>",             _compleationProviderAbstract.isLookupSystemProcedureName());
//				JMenuItem cc_spp_mi   = new JCheckBoxMenuItem("<html><b>System Procedure Parameter Info x</b> - <i><font color=\"green\">Get System Procedure Parameter Info</font></i></html>",                                              _compleationProviderAbstract.isLookupSystemProcedureColumns());
//
//				// Reset action
//				cc_reset_mi.addActionListener(new ActionListener()
//				{
//					@Override
//					public void actionPerformed(ActionEvent e)
//					{
//						// mark code completion for refresh
//						if (_compleationProviderAbstract != null)
//						{
//							_compleationProviderAbstract.setNeedRefresh(true);
//							_compleationProviderAbstract.setNeedRefreshSystemInfo(true);
//						}
//					}
//				});
//
//				// All other actions
//				cc_stat_mi .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { _compleationProviderAbstract.setLookupStaticCmds            (((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
//				cc_misc_mi .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { _compleationProviderAbstract.setLookupMisc                  (((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
//				cc_db_mi   .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { _compleationProviderAbstract.setLookupDb                    (((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
//				cc_tn_mi   .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { _compleationProviderAbstract.setLookupTableName             (((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
//				cc_tc_mi   .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { _compleationProviderAbstract.setLookupTableColumns          (((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
//				cc_pn_mi   .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { _compleationProviderAbstract.setLookupProcedureName         (((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
//				cc_pp_mi   .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { _compleationProviderAbstract.setLookupProcedureColumns      (((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
//				cc_spn_mi  .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { _compleationProviderAbstract.setLookupSystemProcedureName   (((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
//				cc_spp_mi  .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { _compleationProviderAbstract.setLookupSystemProcedureColumns(((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
//
//				// Add it to the Code Completion popup menu
//				codeComplPopupMenu.add(cc_reset_mi);
//				codeComplPopupMenu.add(new JSeparator());
//				codeComplPopupMenu.add(cc_stat_mi);
//				codeComplPopupMenu.add(cc_misc_mi);
//				codeComplPopupMenu.add(cc_db_mi);
//				codeComplPopupMenu.add(cc_tn_mi);
//				codeComplPopupMenu.add(cc_tc_mi);
//				codeComplPopupMenu.add(cc_pn_mi);
//				codeComplPopupMenu.add(cc_pp_mi);
//				codeComplPopupMenu.add(cc_spn_mi);
//				codeComplPopupMenu.add(cc_spp_mi);
//			}
//			@Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {/*empty*/}
//			@Override public void popupMenuCanceled(PopupMenuEvent e) {/*empty*/}
//		});
//
//		// Menu items for Code Completion
//		//---------------------------------------------------
//		JMenu ttProvider_m = new JMenu("<html><b>ToolTip Provider</b> - <i><font color=\"green\">Hower over words in the editor to get help</font></i></html>");
//		popupMenu.add(ttProvider_m);
//		
//		// When the Code Completion popup becoms visible, the menu are refreshed/recreated
//		final JPopupMenu ttProviderPopupMenu = ttProvider_m.getPopupMenu();
//		ttProviderPopupMenu.addPopupMenuListener(new PopupMenuListener()
//		{
//			@Override
//			public void popupMenuWillBecomeVisible(PopupMenuEvent e)
//			{
//				// remove all old items (if any)
//				ttProviderPopupMenu.removeAll();
//
//				JMenuItem cc_reset_mi = new JMenuItem("<html><b>Clear</b> - <i><font color=\"green\">Clear the in memory cache for the Code Completion / ToolTip Provider.</font></i></html>");
//
//				JMenuItem cc_show_mi  = new JCheckBoxMenuItem("<html><b>Show Table/Column information</b> - <i><font color=\"green\">Show table/column information when mouse is over a table name</font></i></html>", (_tooltipProviderAbstract != null) ? _tooltipProviderAbstract.getShowTableInformation() : ToolTipSupplierAbstract.DEFAULT_SHOW_TABLE_INFO);
////				JMenuItem cc_xxxx_mi  = new JCheckBoxMenuItem("<html><b>describeme</b>                    - <i><font color=\"green\">describeme</font></i></html>",                                                    (_tooltipProviderAbstract != null) ? _tooltipProviderAbstract.getXXX() : ToolTipSupplierAbstract.DEFAULT_XXX);
//
//				// Reset action
//				cc_reset_mi.addActionListener(new ActionListener()
//				{
//					@Override
//					public void actionPerformed(ActionEvent e)
//					{
//						// mark code completion for refresh
//						if (_compleationProviderAbstract != null)
//						{
//							_compleationProviderAbstract.setNeedRefresh(true);
//							_compleationProviderAbstract.setNeedRefreshSystemInfo(true);
//						}
//					}
//				});
//
//				// All other actions
//				cc_show_mi .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { if (_tooltipProviderAbstract != null) _tooltipProviderAbstract.setShowTableInformation(((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
////				cc_xxxx_mi .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { if (_tooltipProviderAbstract != null) _tooltipProviderAbstract.setSomeMethodName      (((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
//
//				// Add it to the Code Completion popup menu
//				ttProviderPopupMenu.add(cc_reset_mi);
//				ttProviderPopupMenu.add(new JSeparator());
//				ttProviderPopupMenu.add(cc_show_mi);
////				ttProviderPopupMenu.add(cc_xxxx_mi);
//			}
//			@Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {/*empty*/}
//			@Override public void popupMenuCanceled(PopupMenuEvent e) {/*empty*/}
//		});

		return popupMenu;
	}

	/**
	 * Create a JButton that can enable/disable Application Executions Options
	 * @param button A instance of JButton, if null is passed a new Jbutton will be created.
	 * @param cmName The <b>long</b> or <b>short</b> name of the CounterModel
	 * @return a JButton (if one was passed, it's the same one, but if null was passed a new instance is created)
	 */
	private JButton createAppOptionButton(JButton button)
	{
		if (button == null)
			button = new JButton();

		button.setIcon(SwingUtils.readImageIcon(Version.class, "images/settings.png"));
		button.setToolTipText("<html>Set various Application Options related to the Statement Execution</html>");
		button.setText("Options");

		JPopupMenu popupMenu = createAppOptionPopupMenu();
		button.setComponentPopupMenu(popupMenu);

		// If we click on the button, display the popup menu
		button.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Object source = e.getSource();
				if (source instanceof JButton)
				{
					JButton but = (JButton)source;
					JPopupMenu pm = but.getComponentPopupMenu();
					pm.show(but, 14, 14);
					pm.setLocation( MouseInfo.getPointerInfo().getLocation() );
				}
			}
		});
		
		return button;
	}
	/*----------------------------------------------------------------------
	** END: set Application Option Button
	**----------------------------------------------------------------------*/ 


	
	/*----------------------------------------------------------------------
	** BEGIN: Code Completions Option Button
	**----------------------------------------------------------------------*/ 
	private JPopupMenu createCodeCompletionOptionPopupMenu()
	{
		// Do PopupMenu
		final JPopupMenu popupMenu = new JPopupMenu();

		final JMenu ttProvider_m = new JMenu("<html><b>ToolTip Provider</b> - <i><font color=\"green\">Hower over words in the editor to get help</font></i></html>");
		
		// Menu items for Code Completion
		//---------------------------------------------------
//		JMenu codeCompl_m = new JMenu("<html><b>Code Completion/Assist</b> - <i><font color=\"green\">Use <code><b>Ctrl+Space</b></code> to get Code Completion</font></i></html>");
//		popupMenu.add(codeCompl_m);
		
		// When the Code Completion popup becoms visible, the menu are refreshed/recreated
//		final JPopupMenu codeComplPopupMenu = codeCompl_m.getPopupMenu();
		popupMenu.addPopupMenuListener(new PopupMenuListener()
		{
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e)
			{
				// remove all old items (if any)
				popupMenu.removeAll();

				JMenuItem cc_exec_mi  = new JMenuItem("<html><b>Open</b> - <i><font color=\"green\">Open the Code Completion window. Just like pressing <code><b>Ctrl+Space</b><code/></font></i></html>");

				JMenuItem cc_reset_mi = new JMenuItem("<html><b>Clear</b>   - <i><font color=\"green\">Clear the in memory cache for the Code Completion.</font></i></html>");

				JMenuItem cc_stat_mi  = new JCheckBoxMenuItem("<html><b>Static Commands</b>                 - <i><font color=\"green\">Get Static Commands or Templates that can be used <code><b>Ctrl+Space</b><code/></font></i></html>", _compleationProviderAbstract.isLookupStaticCmds());
				JMenuItem cc_misc_mi  = new JCheckBoxMenuItem("<html><b>Miscelanious</b>                    - <i><font color=\"green\">Get Miscelanious Info, like ASE Monitoring tables</font></i></html>",                                _compleationProviderAbstract.isLookupMisc());
				JMenuItem cc_db_mi    = new JCheckBoxMenuItem("<html><b>Database Info</b>                   - <i><font color=\"green\">Get Database Info, prev word is <code><b>use</b><code/></font></i></html>",                          _compleationProviderAbstract.isLookupDb());
				JMenuItem cc_tn_mi    = new JCheckBoxMenuItem("<html><b>Table Name Info</b>                 - <i><font color=\"green\">Get Table Name Info, use <code><b>Ctrl+Space</b><code/></font></i></html>",                          _compleationProviderAbstract.isLookupTableName());
				JMenuItem cc_tc_mi    = new JCheckBoxMenuItem("<html><b>Table Column Info</b>               - <i><font color=\"green\">Get Table Column Info, current word start with <code><b>tableAlias.</b><code/></font></i></html>",   _compleationProviderAbstract.isLookupTableColumns());
				JMenuItem cc_pn_mi    = new JCheckBoxMenuItem("<html><b>Procedure Info</b>                  - <i><font color=\"green\">Get Procedure Info, prev word is <code><b>exec</b><code/></font></i></html>",                        _compleationProviderAbstract.isLookupProcedureName());
				JMenuItem cc_pp_mi    = new JCheckBoxMenuItem("<html><b>Procedure Parameter Info</b>        - <i><font color=\"green\">Get Procedure Parameter Info</font></i></html>",                                                     _compleationProviderAbstract.isLookupProcedureColumns());
				JMenuItem cc_spn_mi   = new JCheckBoxMenuItem("<html><b>System Procedure Info</b>           - <i><font color=\"green\">Get System Procedure Info, prev word is <code><b>exec sp_</b><code/></font></i></html>",             _compleationProviderAbstract.isLookupSystemProcedureName());
				JMenuItem cc_spp_mi   = new JCheckBoxMenuItem("<html><b>System Procedure Parameter Info</b> - <i><font color=\"green\">Get System Procedure Parameter Info</font></i></html>",                                              _compleationProviderAbstract.isLookupSystemProcedureColumns());

				// exec/open action
				cc_exec_mi.addActionListener(new ActionListener()
				{
					@Override
					public void actionPerformed(final ActionEvent e)
					{
						SwingUtilities.invokeLater( new Runnable()
						{
							@Override
							public void run()
							{
								// hmmm, this doesnt seem to work...
								//_query_txt.requestFocusInWindow();
								//KeyEvent ctrlSpace = new KeyEvent(_query_txt, KeyEvent.KEY_TYPED, EventQueue.getMostRecentEventTime(), KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_SPACE, ' ');
								//_query_txt.dispatchEvent(ctrlSpace);

								// But this worked, but a bit ugly
								_query_txt.requestFocusInWindow();
								//ActionListener al = _query_txt.getActionForKeyStroke(AutoCompletion.getDefaultTriggerKey());
								ActionListener al = _query_txt.getActionMap().get("AutoComplete");
								al.actionPerformed(e);
							}
						});
					}
				});

				// Reset action
				cc_reset_mi.addActionListener(new ActionListener()
				{
					@Override
					public void actionPerformed(ActionEvent e)
					{
						// mark code completion for refresh
						if (_compleationProviderAbstract != null)
						{
							_compleationProviderAbstract.setNeedRefresh(true);
							_compleationProviderAbstract.setNeedRefreshSystemInfo(true);
						}
					}
				});

				// All other actions
				cc_stat_mi .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { _compleationProviderAbstract.setLookupStaticCmds            (((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
				cc_misc_mi .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { _compleationProviderAbstract.setLookupMisc                  (((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
				cc_db_mi   .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { _compleationProviderAbstract.setLookupDb                    (((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
				cc_tn_mi   .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { _compleationProviderAbstract.setLookupTableName             (((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
				cc_tc_mi   .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { _compleationProviderAbstract.setLookupTableColumns          (((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
				cc_pn_mi   .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { _compleationProviderAbstract.setLookupProcedureName         (((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
				cc_pp_mi   .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { _compleationProviderAbstract.setLookupProcedureColumns      (((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
				cc_spn_mi  .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { _compleationProviderAbstract.setLookupSystemProcedureName   (((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
				cc_spp_mi  .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { _compleationProviderAbstract.setLookupSystemProcedureColumns(((JCheckBoxMenuItem)e.getSource()).isSelected()); } });

				// Add it to the Code Completion popup menu
				popupMenu.add(cc_exec_mi);
				popupMenu.add(cc_reset_mi);
				popupMenu.add(new JSeparator());
				popupMenu.add(cc_stat_mi);
				popupMenu.add(cc_misc_mi);
				popupMenu.add(cc_db_mi);
				popupMenu.add(cc_tn_mi);
				popupMenu.add(cc_tc_mi);
				popupMenu.add(cc_pn_mi);
				popupMenu.add(cc_pp_mi);
				popupMenu.add(cc_spn_mi);
				popupMenu.add(cc_spp_mi);
				popupMenu.add(new JSeparator());
				popupMenu.add(ttProvider_m);
			}
			@Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {/*empty*/}
			@Override public void popupMenuCanceled(PopupMenuEvent e) {/*empty*/}
		});

		// Menu items for ToolTip Provider
		//---------------------------------------------------
		
		// When the Code Completion popup becoms visible, the menu are refreshed/recreated
		final JPopupMenu ttProviderPopupMenu = ttProvider_m.getPopupMenu();
		ttProviderPopupMenu.addPopupMenuListener(new PopupMenuListener()
		{
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e)
			{
				// remove all old items (if any)
				ttProviderPopupMenu.removeAll();

				JMenuItem cc_reset_mi = new JMenuItem("<html><b>Clear</b> - <i><font color=\"green\">Clear the in memory cache for the Code Completion / ToolTip Provider.</font></i></html>");

				JMenuItem cc_show_mi  = new JCheckBoxMenuItem("<html><b>Show Table/Column information</b> - <i><font color=\"green\">Show table/column information when mouse is over a table name</font></i></html>", (_tooltipProviderAbstract != null) ? _tooltipProviderAbstract.getShowTableInformation() : ToolTipSupplierAbstract.DEFAULT_SHOW_TABLE_INFO);
//				JMenuItem cc_xxxx_mi  = new JCheckBoxMenuItem("<html><b>describeme</b>                    - <i><font color=\"green\">describeme</font></i></html>",                                                    (_tooltipProviderAbstract != null) ? _tooltipProviderAbstract.getXXX() : ToolTipSupplierAbstract.DEFAULT_XXX);

				// Reset action
				cc_reset_mi.addActionListener(new ActionListener()
				{
					@Override
					public void actionPerformed(ActionEvent e)
					{
						// mark code completion for refresh
						if (_compleationProviderAbstract != null)
						{
							_compleationProviderAbstract.setNeedRefresh(true);
							_compleationProviderAbstract.setNeedRefreshSystemInfo(true);
						}
					}
				});

				// All other actions
				cc_show_mi .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { if (_tooltipProviderAbstract != null) _tooltipProviderAbstract.setShowTableInformation(((JCheckBoxMenuItem)e.getSource()).isSelected()); } });
//				cc_xxxx_mi .addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { if (_tooltipProviderAbstract != null) _tooltipProviderAbstract.setSomeMethodName      (((JCheckBoxMenuItem)e.getSource()).isSelected()); } });

				// Add it to the Code Completion popup menu
				ttProviderPopupMenu.add(cc_reset_mi);
				ttProviderPopupMenu.add(new JSeparator());
				ttProviderPopupMenu.add(cc_show_mi);
//				ttProviderPopupMenu.add(cc_xxxx_mi);
			}
			@Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {/*empty*/}
			@Override public void popupMenuCanceled(PopupMenuEvent e) {/*empty*/}
		});

		return popupMenu;
	}

	/**
	 * Create a JButton that can enable/disable Application Executions Options
	 * @param button A instance of JButton, if null is passed a new Jbutton will be created.
	 * @param cmName The <b>long</b> or <b>short</b> name of the CounterModel
	 * @return a JButton (if one was passed, it's the same one, but if null was passed a new instance is created)
	 */
	private JButton createCodeCompletionOptionButton(JButton button)
	{
		if (button == null)
			button = new JButton();

		button.setIcon(SwingUtils.readImageIcon(Version.class, "images/code_completion.png"));
//		button.setText("Code Completion");
		button.setToolTipText(
			"<html>" +
			"<h3>Code Completion</h3>" +
			"Set various Options related to Code Completion/Assist<br>" +
			"Use <b>Ctrl-Space</b> to activate Code Completion/Assist<br>" +
			"<br>" +
			"The second time you press <b>Ctrl-Space</b> the window will show <i>sql templates</i><br>" +
			"<br>" +
			"<b>Various Tips how it can be used</b>:<br>" +
			"<ul>" +
			"  <li><code>aaa</code><b>&lt;Ctrl-Space&gt;</b>                              - <i><font color=\"green\">Get list of tables/views/etc that starts with <b><code>aaa</code></b>   </font></i></li>" +
			"  <li><code>select t.<b>&lt;Ctrl-Space&gt;</b> from tabname t</code>         - <i><font color=\"green\">Get column names for the table aliased as <b><code>t</code></b>   </font></i></li>" +
			"  <li><code>select * from tabname t where t.<b>&lt;Ctrl-Space&gt;</b></code> - <i><font color=\"green\">Get column names for the table aliased as <b><code>t</code></b>   </font></i></li>" +
			"  <li><code>exec</code> <b>&lt;Ctrl-Space&gt;</b>                            - <i><font color=\"green\">Get stored procedures   </font></i></li>" +
			"  <li><code>:s</code><b>&lt;Ctrl-Space&gt;</b>                               - <i><font color=\"green\">Get schemas   </font></i></li>" +
			"  <li><code>use </code><b>&lt;Ctrl-Space&gt;</b>                             - <i><font color=\"green\">Get list of databases/catalogs  </font></i></li>" +
			"</ul>" +
			"</html>");

		JPopupMenu popupMenu = createCodeCompletionOptionPopupMenu();
		button.setComponentPopupMenu(popupMenu);

		// If we click on the button, display the popup menu
		button.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Object source = e.getSource();
				if (source instanceof JButton)
				{
					JButton but = (JButton)source;
					JPopupMenu pm = but.getComponentPopupMenu();
					pm.show(but, 14, 14);
					pm.setLocation( MouseInfo.getPointerInfo().getLocation() );
				}
			}
		});
		
		return button;
	}
	/*----------------------------------------------------------------------
	** END: Code Completions Option Button
	**----------------------------------------------------------------------*/ 

	
	
	/*----------------------------------------------------------------------
	** BEGIN: Favorite SQL and RCL button stuff
	**----------------------------------------------------------------------*/ 
	private JButton createSqlCommandsButton(JButton button, final int version)
	{
		if (button == null)
			button = new JButton();

		button.setToolTipText(
			"<html>Execute various predefined SQL Statements.<br>" +
			"<br>" +
			"If ${selectedText} is part of the commands text, it will be replaced with content in the following order" +
			"<ul>" +
			"   <li>Marked/Selected text in the query text</li>" +
			"   <li>Marked/Selected text in the result text</li>" +
			"   <li>Content from the Copy/Paste buffer</li>" +
			"</ul>" +
			"</html>");
		button.setIcon(SwingUtils.readImageIcon(Version.class, "images/command_sql.png"));

		JPopupMenu popupMenu = createSqlCommandsButtonPopupMenu(version);
		button.setComponentPopupMenu(popupMenu);

		// If we click on the button, display the popup menu
		button.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Object source = e.getSource();
				if (source instanceof JButton)
				{
					JButton but = (JButton)source;
					JPopupMenu pm = but.getComponentPopupMenu();
					pm.show(but, 14, 14);
					pm.setLocation( MouseInfo.getPointerInfo().getLocation() );
				}
			}
		});
		
		return button;
	}
	private JPopupMenu createSqlCommandsButtonPopupMenu(final int version)
	{
		ArrayList<FavoriteCommandEntry> commandList = new ArrayList<FavoriteCommandDialog.FavoriteCommandEntry>();
		commandList.add(new FavoriteCommandEntry("select DBNAME=db_name(), SERVER=@@servername, SRVHOST=asehostname(), VERSION=@@version", "db_name(), @@servername, @@version", "What are we using"));
		commandList.add(new FavoriteCommandEntry("sp_who",                                        "", "Who is logged in on the system"));
		commandList.add(new FavoriteCommandEntry("sp_helpdb",                                     "", "What databases are on this ASE"));
		commandList.add(new FavoriteCommandEntry("sp_helpdevice",                                 "", "What devices are on this ASE"));
		commandList.add(new FavoriteCommandEntry("sp_configure 'nondefault'",                     "", "Get <b>changed</b> configuration parameters"));
		commandList.add(new FavoriteCommandEntry("sp_helptext '${selectedText}', NULL, NULL, 'showsql,linenumbers'", "sp_helptext '${selectedText}'", "Get stored procedure text"));
		commandList.add(new FavoriteCommandEntry("sp_help '${selectedText}'",                     "", "Get more information about a object"));
		commandList.add(new FavoriteCommandEntry("sp_spaceused '${selectedText}'",                "", "How much space does a table consume"));
		commandList.add(new FavoriteCommandEntry("sp_helprotect '${selectedText}'",               "", "Who can do what with an object"));
		commandList.add(new FavoriteCommandEntry("sp_helpcache",                                  "", "Get caches and sizes"));
		commandList.add(new FavoriteCommandEntry("sp_cacheconfig",                                "", "Get cache configurations"));
		commandList.add(FavoriteCommandEntry.addSeparator());
		commandList.add(new FavoriteCommandEntry("",                                              "", "Note: Use Ctrl+Space to get code assist for table/column/procedure/etc completion..."));

		// Do PopupMenu
		final JPopupMenu popupMenu = new JPopupMenu();
		
		// Add user defined Statements
		final JMenu udMenu = new JMenu("Execute User Defined SQL Statements");
		udMenu.setIcon(SwingUtils.readImageIcon(Version.class, "images/favorite.png"));
		popupMenu.add( udMenu );

		if (_favoriteCmdManagerSql == null)
			_favoriteCmdManagerSql = new FavoriteCommandManagerSql(udMenu);
		_favoriteCmdManagerSql.rebuild();

		// Add entry: OPEN User Defined Command Editor
		JMenuItem openDialog = new JMenuItem("Edit/Open User Defined SQL Commands");
		openDialog.setToolTipText("Open User Defined SQL Commands Editor");
		openDialog.setIcon(SwingUtils.readImageIcon(Version.class, "images/favorite_file.png"));
		openDialog.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (_favoriteCmdManagerSql == null)
					_favoriteCmdManagerSql = new FavoriteCommandManagerSql(udMenu);
				_favoriteCmdManagerSql.open();
			}
		});
		popupMenu.add(openDialog);

		// add Predefined SQL from AseTune
		JMenu preDefinedSql = MainFrame.createPredefinedSqlMenu(QueryWindow.this);
//		preDefinedSql.setText("<html>Predefined SQL Statements (same as in AseTune)</html>");
		preDefinedSql.setText("<html>Execute some extra <i>system</i> stored procedures. <i>(if not exist; create it)</i></html>");
		popupMenu.add(preDefinedSql);
		
		// add SEPARATOR
		popupMenu.add( new JSeparator() );

		// add the SYSTEM functions
		createUserDefinedMenu(popupMenu, commandList);
		
		return popupMenu;
	}


	private JButton createRclCommandsButton(JButton button, final int version)
	{
		if (button == null)
			button = new JButton();

		button.setToolTipText(
			"<html>Execute various predefined RCL Statements.<br>" +
			"<br>" +
			"If ${selectedText} is part of the commands text, it will be replaced with content in the following order" +
			"<ul>" +
			"   <li>Marked/Selected text in the query text</li>" +
			"   <li>Marked/Selected text in the result text</li>" +
			"   <li>Content from the Copy/Paste buffer</li>" +
			"</ul>" +
			"<b>NOTE</b>: if first word is 'RSSD:' the command will be executed in the RSSD database<br>" +
			"This is done by issuing 'connect rssd', the 'the command', then 'disconnect'" +
			"</html>");
		button.setIcon(SwingUtils.readImageIcon(Version.class, "images/command_rcl.png"));

		JPopupMenu popupMenu = createRclCommandsButtonPopupMenu(version);
		button.setComponentPopupMenu(popupMenu);

		// If we click on the button, display the popup menu
		button.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Object source = e.getSource();
				if (source instanceof JButton)
				{
					JButton but = (JButton)source;
					JPopupMenu pm = but.getComponentPopupMenu();
					pm.show(but, 14, 14);
					pm.setLocation( MouseInfo.getPointerInfo().getLocation() );
				}
			}
		});
		
		return button;
	}
	private JPopupMenu createRclCommandsButtonPopupMenu(final int version)
	{
		ArrayList<FavoriteCommandEntry> commandList = new ArrayList<FavoriteCommandDialog.FavoriteCommandEntry>();
		commandList.add(new FavoriteCommandEntry("admin health",                       "", "Displays status information.  Status is HEALTHY when all threads are running, otherwise SUSPECT."));
		commandList.add(new FavoriteCommandEntry("admin who",                          "", "What threads are in the server"));
		commandList.add(new FavoriteCommandEntry("admin who_is_down",                  "", "Displays the threads that are not running."));
		commandList.add(new FavoriteCommandEntry("admin disk_space",                   "", "Displays the state and amount of used space for disk partitions"));
		commandList.add(new FavoriteCommandEntry("admin logical_status",               "", "Displays status of logical connections of Warm Standby"));
		commandList.add(new FavoriteCommandEntry("admin who, sqm",                     "", "Displays status information about all queues"));
		commandList.add(new FavoriteCommandEntry("admin who, sqt",                     "", "Displays status information about the transactions of each queue"));
		commandList.add(new FavoriteCommandEntry("admin who, dist",                    "", "Displays information about DIST thread"));
		commandList.add(new FavoriteCommandEntry("admin who, dsi",                     "", "Displays information about each DSI scheduler thread running in the Replication Server"));
		commandList.add(new FavoriteCommandEntry("admin show_connections",             "", "Displays information about all connections from the Replication Server to data servers and to other Replication Servers"));
		commandList.add(new FavoriteCommandEntry("admin rssd_name",                    "", "Where is the RSSD located"));
		commandList.add(new FavoriteCommandEntry("admin version",                      "", "Show Replication Server Version"));

		commandList.add(FavoriteCommandEntry.addSeparator());
		
		commandList.add(new FavoriteCommandEntry("trace 'on', 'dsi', 'dsi_buf_dump'",  "", "Turn ON: Write SQL statements executed by the DSI Threads to the RS log"));
		commandList.add(new FavoriteCommandEntry("trace 'off', 'dsi', 'dsi_buf_dump'", "", "Turn OFF: Write SQL statements executed by the DSI Threads to the RS log"));

		commandList.add(FavoriteCommandEntry.addSeparator());
		
		commandList.add(new FavoriteCommandEntry("RSSD: rmp_queue ''",                 "", "Show Queue size for each database/connection"));
		commandList.add(new FavoriteCommandEntry("RSSD: rs_helpdb",                    "", "What databases are connected to the system"));
		commandList.add(new FavoriteCommandEntry("RSSD: rs_helpdbrep",                 "", "Database Replication Definitions"));
		commandList.add(new FavoriteCommandEntry("RSSD: rs_helpdbsub",                 "", "Database Subscriptions"));
		commandList.add(new FavoriteCommandEntry("RSSD: rs_helprep",                   "", "Table Replication Definitions"));
		commandList.add(new FavoriteCommandEntry("RSSD: rs_helpsub",                   "", "Table Subscriptions"));
		commandList.add(new FavoriteCommandEntry("RSSD: rs_helpuser",                  "", "Users in <b>this</b> Replication Server"));

		commandList.add(FavoriteCommandEntry.addSeparator());
		commandList.add(new FavoriteCommandEntry("",                                   "", "Note: Use Ctrl+Space to get code assist for table/column/procedure/etc completion..."));
		
		// Do PopupMenu
		final JPopupMenu popupMenu = new JPopupMenu();

		// Add user defined Statements
		final JMenu udMenu = new JMenu("Execute User Defined RCL Statements");
		udMenu.setIcon(SwingUtils.readImageIcon(Version.class, "images/favorite.png"));
		popupMenu.add( udMenu );
//		popupMenu.add( createUserDefinedMenu(udMenu, "ud.menu.rcl.", Configuration.getInstance(Configuration.USER_CONF)) );
		if (_favoriteCmdManagerRcl == null)
			_favoriteCmdManagerRcl = new FavoriteCommandManagerRcl(udMenu);
		_favoriteCmdManagerRcl.rebuild();

		// Add entry: OPEN User Defined Command Editor
		JMenuItem openDialog = new JMenuItem("Edit/Open User Defined RCL Commands");
		openDialog.setToolTipText("Open User Defined RCL Commands Editor");
		openDialog.setIcon(SwingUtils.readImageIcon(Version.class, "images/favorite_file.png"));
		openDialog.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (_favoriteCmdManagerRcl == null)
					_favoriteCmdManagerRcl = new FavoriteCommandManagerRcl(udMenu);
				_favoriteCmdManagerRcl.open();
			}
		});
		popupMenu.add(openDialog);

		// add SEPARATOR
		popupMenu.add( new JSeparator() );
		
		// add the SYSTEM functions
		createUserDefinedMenu(popupMenu, commandList);

		return popupMenu;
	}
	
	//////////////////////////////////////////////////////////////
	// BEGIN: IMPLEMEMNTS the FavoriteCommandDialog FavoriteOwner
	//////////////////////////////////////////////////////////////
	private class FavoriteCommandManagerSql 
	implements FavoriteCommandDialog.FavoriteOwner
	{
		private FavoriteCommandDialog _favDialog = new FavoriteCommandDialog(this, _window);
		private JMenu                 _udMenu    = null; // User Defined Commands Menu

		public FavoriteCommandManagerSql(JMenu udMenu)
		{
			_udMenu = udMenu;
		}

		public void open()
		{
			_favDialog.reload();
			_favDialog.setVisible(true);
		}

		/** implemets FavoriteCommandDialog.FavoriteOwner */
		@Override
		public void rebuild()
		{
			ArrayList<FavoriteCommandEntry> entries = _favDialog.getEntries();
			createUserDefinedMenu(_udMenu, entries);
		}

		/** implemets FavoriteCommandDialog.FavoriteOwner */
		@Override
		public void saveFavoriteFilename(String filename)
		{
			_favoriteCmdFilenameSql = filename;
			saveProps();
		}

		/** implemets FavoriteCommandDialog.FavoriteOwner */
		@Override
		public String getFavoriteFilename()
		{
			return _favoriteCmdFilenameSql;
		}
		
		/** implemets FavoriteCommandDialog.doExecute */
		@Override
		public void doExecute(String statement)
		{
			displayQueryResults(statement, 0, false);
		}
	}
	private class FavoriteCommandManagerRcl 
	implements FavoriteCommandDialog.FavoriteOwner
	{
		private FavoriteCommandDialog _favDialog = new FavoriteCommandDialog(this, _window);
		private JMenu                 _udMenu    = null; // User Defined Commands Menu

		public FavoriteCommandManagerRcl(JMenu udMenu)
		{
			_udMenu = udMenu;
		}

		public void open()
		{
			_favDialog.reload();
			_favDialog.setVisible(true);
		}

		/** implemets FavoriteCommandDialog.FavoriteOwner */
		@Override
		public void rebuild()
		{
			ArrayList<FavoriteCommandEntry> entries = _favDialog.getEntries();
			createUserDefinedMenu(_udMenu, entries);
		}

		/** implemets FavoriteCommandDialog.FavoriteOwner */
		@Override
		public void saveFavoriteFilename(String filename)
		{
			_favoriteCmdFilenameRcl = filename;
			saveProps();
		}

		/** implemets FavoriteCommandDialog.FavoriteOwner */
		@Override
		public String getFavoriteFilename()
		{
			return _favoriteCmdFilenameRcl;
		}
		
		/** implemets FavoriteCommandDialog.doExecute */
		@Override
		public void doExecute(String statement)
		{
			displayQueryResults(statement, 0, false);
		}
	}
	//////////////////////////////////////////////////////////////
	// END: IMPLEMEMNTS the FavoriteCommandDialog FavoriteOwner
	//////////////////////////////////////////////////////////////
	
	public void createUserDefinedMenu(JMenu menu, ArrayList<FavoriteCommandEntry> entries)
	{
		menu.removeAll();

		// No entries
		if (entries.size() == 0)
		{
			JMenuItem mi = new JMenuItem("<html> <b>No Used Defined Command Favorities has been added</b><br> </html>");
			menu.add(mi);
			return;
		}

		createUserDefinedMenu((JComponent)menu, entries);
	}
	public void createUserDefinedMenu(JPopupMenu menu, ArrayList<FavoriteCommandEntry> entries)
	{
		createUserDefinedMenu((JComponent)menu, entries);
	}
	private void createUserDefinedMenu(JComponent menu, ArrayList<FavoriteCommandEntry> entries)
	{
		// Now add the above commands...
		for (FavoriteCommandEntry entry : entries)
		{
			if (entry.isSeparator())
			{
				menu.add(new JSeparator());
				continue;
			}

			// Add entry
			JMenuItem mi = new JMenuItem();
			FavoriteCommandDialog.setBasicInfo(mi, entry);

			mi.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					JMenuItem mi = (JMenuItem) e.getSource();

					String cmd = mi.getActionCommand();
					if (cmd.startsWith("RSSD: "))
					{
						String rssdCmd = cmd.substring("RSSD: ".length());
						cmd="connect rssd\n" +
						    "go\n" +
						    rssdCmd + "\n" +
						    "go\n" +
						    "disconnect\n" +
						    "go\n";
					}
					cmd = replaceTemplateStringForCommandButton("${selectedText}", cmd);
					if (cmd != null)
						displayQueryResults(cmd, 0, false);
				}
			});
			menu.add(mi);
		}
	}
	/** 
	 * replace <code>${selectedText}</code> with a proper command <br>
	 * If the result command has a newline, a confirm question will be asked.
	 * @return null if nothing was found in query/result/copy_pase_buffer
	 */
	private String replaceTemplateStringForCommandButton(String replace, String cmd)
	{
		if (replace == null) return cmd;
		if (cmd     == null) return null;
		
		if (cmd.indexOf(replace) >= 0)
		{
			boolean alreadyConfirmed = false;
			
			// First get it from the QUERY editor
			String selectedText = _query_txt.getSelectedText();

			// Then try the RESULT output
			if (selectedText == null && _result_txt != null)
				selectedText = _result_txt.getSelectedText();

			// Finaly try the Copy/Past buffer
			if (selectedText == null)
			{
				selectedText = SwingUtils.getClipboardContents();

				// If it was grabbed from the Copy/Paste buffer, display the query
				if (selectedText != null)
				{
					String cmdToExec = cmd.replace(replace, selectedText);

					String msg = 
						"<html>" +
						  "<h2>Grabbed text from the Copy/Paste buffer</h2><br> " +
						  "This might not be what you wanted.<br> " +
						  "You can also <i>mark/select</i> the text in the Query or Result text<br>" +
						  "Are you sure you want to execute the below command.<br> " +
						  "<br> " +
						  "<b>Command Template:</b><br> " +
						  "<hr> " + // ---------------------------------
						  "<pre>"+cmd+"</pre><br> " +
						  "<br> " +
						  "<b>Command to Execute:</b><br> " +
						  "<hr> " + // ---------------------------------
						  "<pre>"+cmdToExec+"</pre><br> " +
						"</html>";
					int answer = JOptionPane.showConfirmDialog(_window, new JLabel(msg), "Confirm", JOptionPane.YES_NO_OPTION);
					if (answer == 1)
						return null;
					
					alreadyConfirmed = true;
				}
			}

			// Check if it has newlines in it, then ask a question...
			if (selectedText != null)
			{
				if (selectedText.indexOf('\n') >= 0 && ! alreadyConfirmed)
				{
					String cmdToExec = cmd.replace(replace, selectedText);

					String msg = 
						"<html>" +
						  "<h2>The selected text contains a newline</h2><br> " +
						  "Are you sure you want to execute the below command.<br> " +
						  "<br> " +
						  "<b>Command Template:</b><br> " +
						  "<hr> " + // ---------------------------------
						  "<pre>"+cmd+"</pre><br> " +
						  "<br> " +
						  "<b>Command to Execute:</b><br> " +
						  "<hr> " + // ---------------------------------
						  "<pre>"+cmdToExec+"</pre><br> " +
						"</html>";
					int answer = JOptionPane.showConfirmDialog(_window, new JLabel(msg), "Confirm", JOptionPane.YES_NO_OPTION);
					if (answer == 1)
						return null;
				}
			}

			if (selectedText == null)
			{
				SwingUtils.showInfoMessage(_window, "Nothing selected", 
						"You need to select/mark some text in the 'query/result text or copy/paste buffer' that you want to replace with the '${selectedText}' in the predefined command you want to execute.");
				return null;
			}
			cmd = cmd.replace(replace, selectedText);
		}
		return cmd;
	}
	/*----------------------------------------------------------------------
	** END: Favorite SQL and RCL button stuff
	**----------------------------------------------------------------------*/ 


	
	/*----------------------------------------------------------------------
	** BEGIN: last used file(s) methods
	**----------------------------------------------------------------------*/ 
	/**
	 * Safe what files we have been used
	 * @param name
	 */
	public void addFileHistory(String name)
	{
		File f = new File(name);
		addFileHistory(f);
	}
	/**
	 * Safe what files we have been used
	 * @param name
	 */
	public void addFileHistory(File f)
	{
		if ( ! f.exists() )
			return;
		String name = f.toString();

		_lastFileNameList.remove(name);
		_lastFileNameList.add(0, name);
		while (_lastFileNameList.size() >= _lastFileNameSaveMax)
			_lastFileNameList.removeLast();
		
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null)
			return;

		conf.removeAll("LastFileList.");
		for (int i=0; i<_lastFileNameList.size(); i++)
		{
			String key = "LastFileList."+i+".name";
			String val = _lastFileNameList.get(i);
//System.out.println("addFileHistory(): key='"+key+"', value='"+val+"'.");
			conf.setProperty(key, val);
		}
		
		conf.setProperty("LastFileList.saveSize", _lastFileNameSaveMax);

		buildFileHistoryMenu();

		conf.save();
	}
	
	/**
	 * Safe what files we have been used
	 * @param name
	 */
	public void loadFileHistory()
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null)
			return;

		// Make a new list and initialize it with blanks
		_lastFileNameList = new LinkedList<String>();
		for (int i=0; i<_lastFileNameSaveMax; i++)
			_lastFileNameList.add("");

		// set entries...
		for (int i=0; i<_lastFileNameSaveMax; i++)
		{
			String key = "LastFileList."+i+".name";
			String val = conf.getProperty(key, "");
			_lastFileNameList.set(i, val);
		}

		// remove all empty entries
		while(_lastFileNameList.remove(""));
		
		buildFileHistoryMenu();
	}
	private void buildFileHistoryMenu()
	{
		// remove all old items (if any)
		_fHistory_m.removeAll();

		int keyStrokeNum = 0x30; // KeyEvent.VK_0 = 0x30;

		// Now create menu items
		for (String name : _lastFileNameList)
		{
			JMenuItem mi = new JMenuItem();
			mi.setText(name);

			// Add Ctrl-1 .. Ctrl-9   for the first 9 entries
			keyStrokeNum++;
			if (keyStrokeNum <=  KeyEvent.VK_9)
				mi.setAccelerator(KeyStroke.getKeyStroke(keyStrokeNum, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

			mi.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					Object o = e.getSource();
					if (o instanceof JMenuItem)
					{
						JMenuItem mi = (JMenuItem) o;
						String filename = mi.getText();
						action_fileOpen(null, filename);
					}
				}
			});
			_fHistory_m.add(mi);
		}
	}

	/**
	 * get entry 0 from the history, or last used file.
	 */
	public String getFileHistoryLastUsed()
	{
		if (_lastFileNameList.size() == 0)
			return "";

		return _lastFileNameList.get(0);
	}
	/*----------------------------------------------------------------------
	** END: last used file(s) methods
	**----------------------------------------------------------------------*/ 

	

	/*----------------------------------------------------------------------
	** BEGIN: ExecButton which accepts Drop of Strings
	**----------------------------------------------------------------------*/ 
	private class ExecButton extends JButton
	{
		private static final long serialVersionUID = 1L;
		ExecButton(String label)
		{
			super(label);

			setTransferHandler(new TransferHandlerExecButton());
		}

		private class TransferHandlerExecButton
		extends TransferHandler
		{
			private static final long serialVersionUID = 1L;

			@Override
			public boolean canImport(TransferHandler.TransferSupport support)
			{
				if ( ! support.isDataFlavorSupported(DataFlavor.stringFlavor) )
					return false;
				support.setDropAction(COPY);
				return true;
			}

			@Override
			public boolean importData(TransferHandler.TransferSupport support)
			{
				if ( ! canImport(support) )
					return false;

				// fetch the data and bail if this fails
				String data;
				try {
					data = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
				} catch (UnsupportedFlavorException e) {
					return false;
				} catch (IOException e) {
					return false;
				}

				Component targetDropComp = support.getComponent();

				// Figgure out if the is a exec that wants to show the GUI EXECUTION PLAN
				boolean showGuiExecPlan = false;
				if (targetDropComp instanceof JButton)
				{
					String actionCommand = ((JButton) targetDropComp).getActionCommand();
					if (ACTION_EXECUTE_GUI_SHOWPLAN.equals(actionCommand))
						showGuiExecPlan = true;
				}

				displayQueryResults(data, 0, showGuiExecPlan);

				return true;
			}
		};
	};
	/*----------------------------------------------------------------------
	** END: ExecButton which accepts Drop of Strings
	**----------------------------------------------------------------------*/ 

	
	/*----------------------------------------------------
	 ** BEGIN: Watermark stuff
	 **---------------------------------------------------
	 */
	private Watermark _watermark = null;

	public void setWatermark()
	{
		if ( _conn == null )
		{
			setWatermarkText("Not Connected...");
		}
		else if ( _aseConnectionStateInfo != null && ( _aseConnectionStateInfo._tranCount > 0 || _aseConnectionStateInfo.isNonNormalTranState()) )
		{
			String str;
			if (_aseConnectionStateInfo.isTranStateUsed())
				str = _aseConnectionStateInfo.getTranStateDescription() + "\n@@trancount = " + _aseConnectionStateInfo._tranCount;
			else
				str = "@@trancount = " + _aseConnectionStateInfo._tranCount;
				
			setWatermarkText(str);
		}
		// NO Query text... 
		else if ( _query_txt.getLineCount() < 10 && StringUtil.isNullOrBlank(_query_txt.getText()))
		{
			setWatermarkText("Write your SQL query here");
		}
		else
		{
			setWatermarkText(null);
		}
	}

	public void setWatermarkText(String str)
	{
		if (_watermark != null)
			_watermark.setWatermarkText(str);
	}

	public void setWatermarkAnchor(JComponent comp)
	{
		_watermark = new Watermark(comp, "");
	}

	private class Watermark extends AbstractComponentDecorator
	{
		public Watermark(JComponent target, String text)
		{
			super(target);
			if ( text == null )
				text = "";
			_textSave = text;
			_textBr   = text.split("\n");
		}

		private String[]	_textBr			= null; // Break Lines by '\n'
		private String      _textSave       = null; // Save last text so we don't need to do repaint if no changes.
		private Graphics2D	g				= null;
		private Rectangle	r				= null;

		@Override
		public void paint(Graphics graphics)
		{
			if ( _textBr == null || _textBr != null && _textBr.length < 0 )
				return;

			r = getDecorationBounds();
			g = (Graphics2D) graphics;
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Font f = g.getFont();
//			g.setFont(f.deriveFont(Font.BOLD, f.getSize() * 2.0f));
			g.setFont(f.deriveFont(Font.BOLD, f.getSize() * 1.5f));
			g.setColor(new Color(128, 128, 128, 128));

			FontMetrics fm = g.getFontMetrics();
			int maxStrWidth = 0;
			int maxStrHeight = fm.getHeight();

			// get max with for all of the lines
			for (int i = 0; i < _textBr.length; i++)
			{
				int CurLineStrWidth = fm.stringWidth(_textBr[i]);
				maxStrWidth = Math.max(maxStrWidth, CurLineStrWidth);
			}
			int xPos = (r.width - maxStrWidth) / 2;
			int yPos = (int) (r.height - ((r.height - fm.getHeight()) / 2) * 1.3);

			// Print all the lines
			for (int i = 0; i < _textBr.length; i++)
			{
				g.drawString(_textBr[i], xPos, (yPos + (maxStrHeight * i)));
			}
		}

		public void setWatermarkText(String text)
		{
			if ( text == null )
				text = "";

			// If text has NOT changed, no need to continue
			if (text.equals(_textSave))
				return;

			_textSave = text;

			_textBr = text.split("\n");
			_logger.debug("setWatermarkText: to '" + text + "'.");

			repaint();
		}
	}
	/*---------------------------------------------------
	 ** END: Watermark stuff
	 **---------------------------------------------------
	 */
	

	
	/*----------------------------------------------------------------------
	** BEGIN: implement WatchdogIsFileChanged
	**----------------------------------------------------------------------*/ 
	@Override
	public void fileHasChanged(final File file, final long savedLastModifiedTime)
	{
//		String changedFilename = file.toString();
//		String filename = _statusBar.getFilename();

		Runnable doRun = new Runnable()
		{
			@Override
			public void run()
			{
				// Add some debug info to try to find out why the WatchDog fires while 
				String debugStr = "";
				if (true)
				{
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

					debugStr = "<br><br><b>DEBUG INFO:</b><br><pre>" +
							"in: file.lastModified()   = " + sdf.format(new Date(file.lastModified())) + "<br>" +
							"in: savedLastModifiedTime = " + sdf.format(new Date(savedLastModifiedTime)) + "<br>" +
							"diff in:                  = " + TimeUtils.msToTimeStr(file.lastModified() - savedLastModifiedTime) + "<br>" +
							"isModifiedOutsideEditor() = "+ _query_txt.isModifiedOutsideEditor() + "<br>" +
							"</pre>";
				}

				// remember the carret position so we can restore it when the file has been loaded
				int saveAtRow      = _query_txt.getCaretLineNumber();
				int saveAtCol      = _query_txt.getCaretOffsetFromLineStart();
//				int caretPos = _query_txt.getCaretPosition();

//				Object[] buttons = {"Load Changes", "Not now", "Not Ever"};
				Object[] buttons = {"Load Changes", "Not now"};
				int answer = JOptionPane.showOptionDialog(_window, 
						"<html>Another application has updated the file "+file+"<br>Reload it?."+debugStr+"</html>",
						"Reload file", 
						JOptionPane.DEFAULT_OPTION,
						JOptionPane.QUESTION_MESSAGE,
						null,
						buttons,
						buttons[0]);
				// Reload
				if (answer == 0) 
				{
//System.out.println("GO AND RELOAD THE FILE");
//					openFile(_statusBar.getFilename());
					openFile(file);

					// Set back the carret position where you were before reloading the file
					try	
					{
						// if new document is smaller (has less rows), simple set saveAtRow to maxValue
						int newNumOfLines = _query_txt.getLineCount() - 1;
						if (newNumOfLines < saveAtRow)
							saveAtRow = newNumOfLines; 
							
						// if new column position, doesnt exist on the new row (new line is shorter)
						// then set position to start of the line
						int newLineStartOffs = _query_txt.getLineStartOffset(saveAtRow);
						int newLineEndOffs   = _query_txt.getLineEndOffset(saveAtRow);
						int restoreOffs      = newLineStartOffs + saveAtCol;
						if (restoreOffs > newLineEndOffs)
							restoreOffs = newLineStartOffs;
						_query_txt.setCaretPosition(restoreOffs);
					}
					catch( BadLocationException     ignore ) { /* ignore */ }
					catch( IllegalArgumentException ignore ) { /* ignore */ }
				}
				// CANCEL/not now
				else
				{
//System.out.println("CANCEL: RELOAD THE FILE (not now was pressed)");
					// Can this really be null, but why not
					if (_watchdogIsFileChanged != null)
						_watchdogIsFileChanged.setFile(file);

					// NOT IMPLEMENTED: Not Ever
					if (answer == 2)
					{
					}
				}
			}
		};
		if ( ! SwingUtilities.isEventDispatchThread() )
		{
			try	{ SwingUtilities.invokeAndWait(doRun); }
			catch (InterruptedException e)      { /* ignore e.printStackTrace();*/ }
			catch (InvocationTargetException e) { e.printStackTrace(); }
		}
		else
			doRun.run();
	}
	/*----------------------------------------------------------------------
	** END: implement WatchdogIsFileChanged
	**----------------------------------------------------------------------*/ 

	
	
	/*----------------------------------------------------------------------
	** BEGIN: class ResultSetJXTable
	**----------------------------------------------------------------------*/ 
	private final static Color NULL_VALUE_COLOR = new Color(240, 240, 240);
	
	private static class ResultSetJXTable
	extends JXTable
	{
		private static final long serialVersionUID = 1L;
		private Point _lastMouseClick = null;

		public Point getLastMouseClickPoint()
		{
			return _lastMouseClick;
		}

		public ResultSetJXTable(TableModel tm)
		{
			super(tm);
			
			addMouseListener(new MouseListener()
			{
				@Override public void mouseReleased(MouseEvent e) {}
				@Override public void mousePressed(MouseEvent e) {}
				@Override public void mouseExited(MouseEvent e) {}
				@Override public void mouseEntered(MouseEvent e) {}
				@Override
				public void mouseClicked(MouseEvent e)
				{
					_lastMouseClick = e.getPoint();
				}
			});

			// NULL Values: SET BACKGROUND COLOR
			addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				@Override
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					// Check NULL value
					String cellValue = adapter.getString();
					if (cellValue == null || ResultSetTableModel.DEFAULT_NULL_REPLACE.equals(cellValue))
						return true;
					
					// Check ROWID Column
					int mcol = adapter.convertColumnIndexToModel(adapter.column);
					String colName = adapter.getColumnName(mcol);
					if (mcol == 0 && ResultSetTableModel.ROW_NUMBER_COLNAME.equals(colName))
						return true;

					return false;
				}
			}, NULL_VALUE_COLOR, null));
		}

		// 
		// TOOL TIP for: TABLE HEADERS
		//
		@Override
		protected JTableHeader createDefaultTableHeader()
		{
			return new JXTableHeader(getColumnModel())
			{
				private static final long serialVersionUID = 1L;

				private boolean      _tabHeader_useFocusableTips = true;
				private FocusableTip _tabHeader_focusableTip     = null;

				@Override
				public String getToolTipText(MouseEvent e)
				{
					// Now get the column name, which we point at
					Point p = e.getPoint();
					int index = getColumnModel().getColumnIndexAtX(p.x);
					if ( index < 0 )
						return null;
					
					TableModel tm = getModel();
					if (tm instanceof ResultSetTableModel)
					{
						ResultSetTableModel rstm = (ResultSetTableModel) tm;
						String tooltip = rstm.getToolTipTextForTableHeader(index);

						if (_tabHeader_useFocusableTips)
						{
    						if (tooltip != null) 
    						{
    							if (_tabHeader_focusableTip == null) 
    								_tabHeader_focusableTip = new FocusableTip(this);
    
    							_tabHeader_focusableTip.toolTipRequested(e, tooltip);
    						}
    						// No tool tip text at new location - hide tip window if one is
    						// currently visible
    						else if (_tabHeader_focusableTip != null) 
    						{
    							_tabHeader_focusableTip.possiblyDisposeOfTipWindow();
    						}
    						return null;
						}
    					else
    						return tooltip;
					}
					return null;
				}
			};
		}
	}
	/*----------------------------------------------------------------------
	** END: class ResultSetJXTable
	**----------------------------------------------------------------------*/ 
	
	/**
	 * This simple main method tests the class.  It expects four command-line
	 * arguments: the driver classname, the database URL, the username, and
	 * the password
	 **/
	public static void test_main(String args[]) throws Exception
	{
		// FIXME: parse input parameters

		Properties log4jProps = new Properties();
		//log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		// Set configuration, right click menus are in there...
		Configuration conf = new Configuration("c:\\projects\\asetune\\asetune.properties");
		Configuration.setInstance(Configuration.SYSTEM_CONF, conf);

		// Create the factory object that holds the database connection using
		// the data specified on the command line
    	try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    		//UIManager.setLookAndFeel(new SubstanceOfficeSilver2007LookAndFeel());
		} catch (Exception e) {
			e.printStackTrace();
		}

		String server = "GORAN_1_DS";
//		String host = AseConnectionFactory.getIHost(server);
//		int    port = AseConnectionFactory.getIPort(server);
		String hostPortStr = AseConnectionFactory.getIHostPortStr(server);
		System.out.println("Connectiong to server='"+server+"'. Which is located on '"+hostPortStr+"'.");
		Connection conn = null;
		try
		{
			Properties props = new Properties();
			props.put("CHARSET", "iso_1");
			AseConnectionFactory.setPropertiesForAppname(Version.getAppName()+"-QueryWindow", "IGNORE_DONE_IN_PROC", "true");
			
			conn = AseConnectionFactory.getConnection(hostPortStr, null, "sa", "", Version.getAppName()+"-QueryWindow", null, props, null);
		}
		catch (SQLException e)
		{
			System.out.println("Problems connecting: " + AseConnectionUtils.sqlExceptionToString(e));
//			AseConnectionUtils.sqlWarningToString(e);
			throw e;
		}


		// Create a QueryWindow component that uses the factory object.
		QueryWindow qw = new QueryWindow(conn, 
				"print 'a very long string that starts here.......................and continues,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,with some more characters------------------------and some more++++++++++++++++++++++++++++++++++++ yes even more 00000 0 0 0 0 0 000000000 0 00000000 00000, lets do some more.......................... end it ends here. -END-'\n" +
				"print '11111111'\n" +
				"select getdate()\n" +
				"exec sp_whoisw2\n" +
				"select \"ServerName\" = @@servername\n" +
				"raiserror 123456 'A dummy message by raiserror'\n" +
				"exec sp_help sysobjects\n" +
				"select \"Current Date\" = getdate()\n" +
				"print '222222222'\n" +
				"select * from master..sysdatabases\n" +
				"print '|3-33333333'\n" +
				"print '|4-33333333'\n" +
				"print '|5-33333333'\n" +
				"print '|6-33333333'\n" +
				"print '|7-33333333'\n" +
				"print '|8-33333333'\n" +
				"print '|9-33333333'\n" +
				"print '|10-33333333'\n" +
				"                             exec sp_opentran \n" +
				"print '|11-33333333'\n" +
				"print '|12-33333333'\n" +
				"print '|13-33333333'\n" +
				"print '|14-33333333'\n" +
				"print '|15-33333333'\n" +
				"print '|16-33333333'\n" +
				"print '|17-33333333'\n" +
				"select * from sysobjects \n" +
				"select * from sysprocesses ",
				null, true, WindowType.JFRAME, null);
		qw.openTheWindow();
	}	
	/**
	 * Print command line options.
	 * @param options
	 */
	private static void printHelp(Options options, String errorStr)
	{
		PrintWriter pw = new PrintWriter(System.out);

		if (errorStr != null)
		{
			pw.println();
			pw.println(errorStr);
			pw.println();
		}

		pw.println("usage: sqlw [-U <user>] [-P <passwd>] [-S <server>] [-D <dbname>]");
		pw.println("            [-u <jdbcUrl>] [-d <jdbcDriver>]");
		pw.println("            [-q <sqlStatement>] [-h] [-v] [-x] <debugOptions> ");
		pw.println("  ");
		pw.println("options:");
		pw.println("  -h,--help                 Usage information.");
		pw.println("  -v,--version              Display "+Version.getAppName()+" and JVM Version.");
		pw.println("  -x,--debug <dbg1,dbg2>    Debug options: a comma separated string");
		pw.println("                            To get available option, do -x list");
		pw.println("  ");
		pw.println("  -U,--user <user>          Username when connecting to server.");
		pw.println("  -P,--passwd <passwd>      Password when connecting to server. null=noPasswd");
		pw.println("  -S,--server <server>      Server to connect to.");
		pw.println("  -D,--dbname <dbname>      Database to use when connecting");
		pw.println("  -u,--jdbcUrl <url>        JDBC URL. if not a sybase/TDS server");
		pw.println("  -d,--jdbcDriver <driver>  JDBC Driver. if not a sybase/TDS server");
		pw.println("                            If the JDBC drivers is registered with the ");
		pw.println("                            DriverManager, this is NOT needed");
		pw.println("  -p,--connProfile <name>   Connect using an existing Connection Profile");
		pw.println("  -q,--query <sqlStatement> SQL Statement to execute");
		pw.println("  -i,--inputFile <filename> Input File to open in editor");
		pw.println("");
		pw.flush();
	}

	/**
	 * Build the options com.asetune.parser. Has to be synchronized because of the way
	 * Options are constructed.
	 * 
	 * @return an options com.asetune.parser.
	 */
	private static synchronized Options buildCommandLineOptions()
	{
		Options options = new Options();

		// create the Options
		options.addOption( "h", "help",        false, "Usage information." );
		options.addOption( "v", "version",     false, "Display "+Version.getAppName()+" and JVM Version." );
		options.addOption( "x", "debug",       true,  "Debug options: a comma separated string dbg1,dbg2,dbg3" );

		options.addOption( "U", "user",        true, "Username when connecting to server." );
		options.addOption( "P", "passwd",      true, "Password when connecting to server. (null=noPasswd)" );
		options.addOption( "S", "server",      true, "Server to connect to." );
		options.addOption( "D", "dbname",      true, "Database use when connecting" );
		options.addOption( "u", "jdbcUrl",     true, "JDBC URL. if not a sybase/TDS server" );
		options.addOption( "d", "jdbcDriver",  true, "JDBC Driver. if not a sybase/TDS server. If the JDBC drivers is registered with the DriverManager, this is NOT needed." );
		options.addOption( "p", "connProfile", true, "Connect using an existing Connection Profile");
		options.addOption( "q", "sqlStatement",true, "SQL statement to execute" );
		options.addOption( "i", "inputFile",   true, "Input File to open in editor" );

		return options;
	}


	//---------------------------------------------------
	// Command Line Parsing
	//---------------------------------------------------
	private static CommandLine parseCommandLine(String[] args, Options options)
	throws ParseException
	{
		// create the command line com.asetune.parser
		CommandLineParser parser = new PosixParser();	
	
		// parse the command line arguments
		CommandLine cmd = parser.parse( options, args );

		// Validate any mandatory options or dependencies of switches
		

		if (_logger.isDebugEnabled())
		{
			for (@SuppressWarnings("unchecked") Iterator<Option> it=cmd.iterator(); it.hasNext();)
			{
				Option opt = it.next();
				_logger.debug("parseCommandLine: swith='"+opt.getOpt()+"', value='"+opt.getValue()+"'.");
			}
		}

		return cmd;
	}

//	private static void jConnectEnableLogging()
//	{
////		java.util.logging.Logger LOG = java.util.logging.Logger.getLogger("com.sybase.jdbc4.jdbc");
//		java.util.logging.Logger LOG = java.util.logging.Logger.getLogger("com.sybase.jdbc4.jdbc.SybConnection");
//
//		// To log class-specific log message, provide complete class name, for example:
//		//Logger.getLogger("com.sybase.jdbc4.jdbc.SybConnection");
//		//Get handle as per user's requirement 
//		Handler handler = new ConsoleHandler();
//
//		//Set logging level
////		handler.setLevel(java.util.logging.Level.ALL);
//		handler.setLevel(java.util.logging.Level.FINEST);
//
//		//Added user specific handler to logger object
//		LOG.addHandler(handler);
//
//		//Set logging level
////		LOG.setLevel(java.util.logging.Level.ALL);
//		LOG.setLevel(java.util.logging.Level.FINEST);
//	}
	//---------------------------------------------------
	// MAIN
	//---------------------------------------------------
	public static void main(String[] args)
	{
//		jConnectEnableLogging();

		Options options = buildCommandLineOptions();
		try
		{
			CommandLine cmd = parseCommandLine(args, options);

			//-------------------------------
			// HELP
			//-------------------------------
			if ( cmd.hasOption("help") )
			{
				printHelp(options, "The option '--help' was passed.");
			}
			//-------------------------------
			// VERSION
			//-------------------------------
			else if ( cmd.hasOption("version") )
			{
				System.out.println();
				System.out.println(Version.getAppName()+" Version: " + Version.getVersionStr() + " JVM: " + System.getProperty("java.version"));
				System.out.println();
			}
			//-------------------------------
			// Check for correct number of cmd line parameters
			//-------------------------------
			else if ( cmd.getArgs() != null && cmd.getArgs().length > 0 )
			{
				String error = "Unknown options: " + StringUtil.toCommaStr(cmd.getArgs());
				printHelp(options, error);
			}
			//-------------------------------
			// Start AseTune, GUI/NOGUI will be determined later on.
			//-------------------------------
			else
			{
				new QueryWindow(cmd);
			}
		}
		catch (ParseException pe)
		{
			String error = "Error: " + pe.getMessage();
			printHelp(options, error);
		}
		catch (NormalExitException e)
		{
			// This was probably throws when checking command line parameters
			// do normal exit
		}
		catch (Exception e)
		{
			System.out.println();
			System.out.println("Error: " + e.getMessage());
			System.out.println();
			System.out.println("Printing a stacktrace, where the error occurred.");
			System.out.println("--------------------------------------------------------------------");
			e.printStackTrace();
			System.out.println("--------------------------------------------------------------------");
		}
	}
}


