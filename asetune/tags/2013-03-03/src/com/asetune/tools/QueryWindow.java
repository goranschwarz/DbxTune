/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
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
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.table.JTableHeader;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.fife.ui.rsyntaxtextarea.FileLocation;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.jdesktop.swingx.JXTable;

import com.asetune.AseConfig;
import com.asetune.AseConfigText;
import com.asetune.AseConfigText.ConfigType;
import com.asetune.DebugOptions;
import com.asetune.Version;
import com.asetune.check.CheckForUpdates;
import com.asetune.gui.AseConfigViewDialog;
import com.asetune.gui.AsePlanViewer;
import com.asetune.gui.ConnectionDialog;
import com.asetune.gui.MainFrame;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.gui.swing.AbstractComponentDecorator;
import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.gui.swing.WaitForExecDialog.BgExecutor;
import com.asetune.sql.pipe.UnknownPipeCommandException;
import com.asetune.ui.autocomplete.CompletionProviderAbstract;
import com.asetune.ui.autocomplete.CompletionProviderAbstractSql;
import com.asetune.ui.autocomplete.CompletionProviderAse;
import com.asetune.ui.autocomplete.CompletionProviderJdbc;
import com.asetune.ui.autocomplete.CompletionProviderRepServer;
import com.asetune.ui.rsyntaxtextarea.AsetuneSyntaxConstants;
import com.asetune.ui.rsyntaxtextarea.AsetuneTokenMaker;
import com.asetune.ui.rsyntaxtextarea.RSyntaxTextAreaX;
import com.asetune.ui.rsyntaxtextarea.RSyntaxUtilitiesX;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.AseSqlScriptReader;
import com.asetune.utils.Configuration;
import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.Debug;
import com.asetune.utils.Encrypter;
import com.asetune.utils.JavaVersion;
import com.asetune.utils.Logging;
import com.asetune.utils.RepServerUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;
import com.asetune.utils.TimeUtils;
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
	implements ActionListener, SybMessageHandler, ConnectionProvider, CaretListener
{
	private static Logger _logger = Logger.getLogger(QueryWindow.class);
	private static final long serialVersionUID = 1L;

	public final static String  PROPKEY_APP_PREFIX          = "QueryWindow.";

	public final static String  PROPKEY_asPlainText         = PROPKEY_APP_PREFIX + "asPlainText";
	public final static boolean DEFAULT_asPlainText         = false;
	
	public final static String  PROPKEY_showRowCount        = PROPKEY_APP_PREFIX + "showRowCount";
	public final static boolean DEFAULT_showRowCount        = true;
	
	public final static String  PROPKEY_lastFileNameSaveMax = "LastFileList.saveSize";
	public final static int     DEFAULT_lastFileNameSaveMax = 20;

	static
	{
		Configuration.registerDefaultValue(PROPKEY_asPlainText,         DEFAULT_asPlainText);
		Configuration.registerDefaultValue(PROPKEY_lastFileNameSaveMax, DEFAULT_lastFileNameSaveMax);
	}
	
	/** Completion Provider for RSyntaxTextArea */
	private CompletionProviderAbstract _compleationProviderAbstract = null;

	//-------------------------------------------------
	// Actions
	public static final String ACTION_CONNECT                   = "CONNECT";
	public static final String ACTION_DISCONNECT                = "DISCONNECT";
	public static final String ACTION_FILE_NEW                  = "FILE_NEW";
	public static final String ACTION_FILE_OPEN                 = "FILE_OPEN";
//	public static final String ACTION_FILE_CLOSE                = "FILE_CLOSE";
	public static final String ACTION_FILE_SAVE                 = "FILE_SAVE";
	public static final String ACTION_FILE_SAVE_AS              = "FILE_SAVE_AS";
	public static final String ACTION_EXIT                      = "EXIT";

	public static final String ACTION_EXECUTE                   = "EXECUTE";
	public static final String ACTION_EXECUTE_GUI_SHOWPLAN      = "EXECUTE_GUI_SHOWPLAN";

	public static final String ACTION_CMD_SQL                   = "CMD_SQL";
	public static final String ACTION_CMD_RCL                   = "CMD_RCL";

	public static final String ACTION_VIEW_LOG_TAIL             = "VIEW_LOG_TAIL";
	public static final String ACTION_VIEW_ASE_CONFIG           = "VIEW_ASE_CONFIG";
	public static final String ACTION_RS_GENERATE_CHANGED_DDL   = "RS_GENERATE_CHANGED_DDL";
	public static final String ACTION_RS_GENERATE_ALL_DDL       = "RS_GENERATE_ALL_DDL";
	public static final String ACTION_RS_DUMP_QUEUE             = "RS_DUMP_QUEUE";
	public static final String ACTION_RS_WHO_IS_DOWN            = "RS_WHO_IS_DOWN";

	private Connection  _conn            = null;
	private int         _connType        = -1;
	private AseConnectionUtils.ConnectionStateInfo _aseConnectionStateInfo = null;
	

//	private JTextArea	_query           = new JTextArea();           // A field to enter a query in
//	private RSyntaxTextArea	_query       = new RSyntaxTextArea();     // A field to enter a query in
	private TextEditorPane	_query       = new TextEditorPane();    // A field to enter a query in
	private RTextScrollPane _queryScroll = new RTextScrollPane(_query);
	private RSyntaxTextArea	_resultText  = null;
	private JButton     _exec            = new JButton("Exec");       // Execute the
	private JButton     _execGuiShowplan = new JButton("Exec, GUI Showplan");    // Execute, but display it with a GUI showplan
	private JButton     _setOptions      = new JButton("Set");        // Do various set ... options
	private JButton     _copy            = new JButton("Copy Res");    // Copy All resultsets to clipboard
//	private JCheckBox   _showplan        = new JCheckBox("GUI Showplan", false);
	private JCheckBox   _rsInTabs        = new JCheckBox("In Tabbed Panel", false);
	private JCheckBox   _asPlainText     = new JCheckBox("As Plain Text", DEFAULT_asPlainText);
	private JCheckBox   _showRowCount    = new JCheckBox("Row Count", DEFAULT_showRowCount);
	private JComboBox   _dbs_cobx        = new JComboBox();
	private JPanel      _resPanel        = new JPanel();
	private JScrollPane _resPanelScroll  = new JScrollPane(_resPanel);
	private RTextScrollPane _resPanelTextScroll  = new RTextScrollPane();
//	private JLabel	    _msgline         = new JLabel("");	     // For displaying messages
	private StatusBar   _statusBar       = new StatusBar();
	private JSplitPane  _splitPane       = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	private int         _lastTabIndex    = -1;
	private boolean     _closeConnOnExit = true;
	private Font        _aseMsgFont      = null;
	private ArrayList<JComponent> _resultCompList  = null;
	
	private int         _aseVersion                = 0;
	private String      _connectedToProductName    = null;
	private String      _connectedToProductVersion = null;
	private String      _connectedToServerName     = null;
	private String      _connectedToSysListeners   = null;
	private String      _connectedAsUser           = null;
	private String      _connectedWithUrl          = null;

	/** if DB returns a error message, stop executions */
	private boolean     _abortOnDbMessages         = false;
	
	// Last File
	private LinkedList<String>_lastFileNameList      = new LinkedList<String>();
	private int         _lastFileNameSaveMax         = DEFAULT_lastFileNameSaveMax;

	// The base Window can be either a JFrame or a JDialog
	private Window      _window          = null;
	private JFrame      _jframe          = null;
	private JDialog     _jdialog         = null;
	private String      _titlePrefix     = null;
	private WindowType  _windowType      = null;

	private JButton     _connect_but     = SwingUtils.makeToolbarButton(Version.class, "connect16.gif",    ACTION_CONNECT,    this, "Connect to a ASE",         "Connect");
	private JButton     _disconnect_but  = SwingUtils.makeToolbarButton(Version.class, "disconnect16.gif", ACTION_DISCONNECT, this, "Close the ASE Connection", "Disconnect");

	private JButton     _cmdSql_but      = null;
	private JButton     _cmdRcl_but      = null;
//	private JButton     _cmdSql_but      = SwingUtils.makeToolbarButton(Version.class, "command_sql.png",          ACTION_CMD_SQL,        this, "Execute some predefined SQL Statements",                          "SQL");
//	private JButton     _cmdRcl_but      = SwingUtils.makeToolbarButton(Version.class, "command_rcl.png",          ACTION_CMD_RCL,        this, "Execute some predefined RCL Statements",                          "RCL");
	private JButton     _rsWhoIsDown_but = SwingUtils.makeToolbarButton(Version.class, "rs_admin_who_is_down.png", ACTION_RS_WHO_IS_DOWN, this, "Execute Replication Server Command 'admin who_is_down' (Ctrl-w)", "who_is_down");
	private JButton     _viewLogFile_but = SwingUtils.makeToolbarButton(Version.class, "tail_logfile.png",         ACTION_VIEW_LOG_TAIL,  this, "Show Server logfile",                                    "logfile");


	// if we start from the CMD Line, add a few extra stuff
	//---------------------------------------
	private JMenuBar            _main_mb                = new JMenuBar();

	private JToolBar            _toolbar                = new JToolBar();

	// File
	private JMenu               _file_m                 = new JMenu("File");
	private JMenuItem           _connect_mi             = new JMenuItem("Connect...");
	private JMenuItem           _disconnect_mi          = new JMenuItem("Disconnect");
	private JMenuItem           _fNew_mi                = new JMenuItem("New File");
	private JMenuItem           _fOpen_mi               = new JMenuItem("Open File...");
//	private JMenuItem           _fClose_mi              = new JMenuItem("Close");
	private JMenuItem           _fSave_mi               = new JMenuItem("Save");
	private JMenuItem           _fSaveAs_mi             = new JMenuItem("Save As...");
	private JMenu               _fHistory_m             = new JMenu("Last Used Files");
	private JMenuItem           _exit_mi                = new JMenuItem("Exit");

	// View
	private JMenu               _view_m                 = new JMenu("View");
	private JMenuItem           _viewLogFile_mi         = new JMenuItem("Tail on Server Log File");
	private JMenuItem           _ase_viewConfig_mi      = new JMenuItem("View ASE Configuration...");
	private JMenuItem           _rs_configChangedDdl_mi = new JMenuItem("View RCL for changed configurations...");
	private JMenuItem           _rs_configAllDdl_mi     = new JMenuItem("View RCL for ALL configurations...");
	private JMenuItem           _rs_dumpQueue_mi        = new JMenuItem("View Stable Queue Content...");
	private JMenuItem           _rsWhoIsDown_mi         = new JMenuItem("Admin who_is_down");
	//---------------------------------------

	// Tools
	private JMenu               _tools_m                = new JMenu("Tools");
	private JMenuItem           _toolDummy_mi           = new JMenuItem("Dummy entry");


	/**
	 * Constructor for CommandLine parameters
	 * @param cmd
	 * @throws Exception
	 */
	public QueryWindow(CommandLine cmd)
	throws Exception
	{
		Version.setAppName("SqlWindow");
		
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

		// Compose MAIN CONFIG file (first USER_HOME then ASETUNE_HOME)
		String filename = Version.APP_STORE_DIR + File.separator + CONFIG_FILE_NAME;
		if ( (new File(filename)).exists() )
			defaultPropsFile = filename;

		String propFile        = cmd.getOptionValue("config",     defaultPropsFile);
		String userPropFile    = cmd.getOptionValue("userConfig", defaultUserPropsFile);
		String tmpPropFile     = cmd.getOptionValue("tmpConfig",  defaultTmpPropsFile);

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
			Configuration.USER_TEMP,    // First
			Configuration.USER_CONF,    // second
			Configuration.SYSTEM_CONF); // Third

		//---------------------------------------------------------------
		// OK, lets get ASE user/passwd/server/dbname
		//---------------------------------------------------------------
		String aseUsername = System.getProperty("user.name"); 
		String asePassword = "";
		String aseServer   = System.getenv("DSQUERY");
		String aseDbname   = "";
		String sqlQuery    = "";
		String sqlFile     = "";
		if (cmd.hasOption('U'))	aseUsername = cmd.getOptionValue('U');
		if (cmd.hasOption('P'))	asePassword = cmd.getOptionValue('P');
		if (cmd.hasOption('S'))	aseServer   = cmd.getOptionValue('S');
		if (cmd.hasOption('D'))	aseDbname   = cmd.getOptionValue('D');
		if (cmd.hasOption('q'))	sqlQuery    = cmd.getOptionValue('q');
		if (cmd.hasOption('i'))	sqlFile     = cmd.getOptionValue('i');

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
		AseConnectionFactory.setPropertiesForAppname("SqlWindow", "IGNORE_DONE_IN_PROC", "true");
		
		// Try make an initial connection...
		Connection conn = null;
		if ( ! StringUtil.isNullOrBlank(asePassword) )
		{
			_logger.info("Connecting as user '"+aseUsername+"' to server='"+aseServer+"'. Which is located on '"+hostPortStr+"'.");
			try
			{
				Properties props = new Properties();
	//			props.put("CHARSET", "iso_1");
				conn = AseConnectionFactory.getConnection(hostPortStr, aseDbname, aseUsername, asePassword, "SqlWindow", null, props, null);
	
				// Set the correct dbname, if it hasnt already been done
				AseConnectionUtils.useDbname(conn, aseDbname);
			}
			catch (SQLException e)
			{
				_logger.error("Problems connecting: " + AseConnectionUtils.sqlExceptionToString(e));
	//			throw e;
			}
		}

		// Create a QueryWindow component that uses the factory object.
		QueryWindow qw = new QueryWindow(conn, sqlQuery, sqlFile, true, WindowType.CMDLINE_JFRAME, null);
		qw.openTheWindow();
//		init(conn, sqlQuery, sqlFile, true, WindowType.CMDLINE_JFRAME, null);
//		openTheWindow();
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
			_file_m.addSeparator();
			_file_m.add(_fNew_mi);
			_file_m.add(_fOpen_mi);
			_file_m.add(_fHistory_m);
//			_file_m.add(_fClose_mi);
//			_file_m.addSeparator();
			_file_m.add(_fSave_mi);
			_file_m.add(_fSaveAs_mi);
			_file_m.addSeparator();
			_file_m.add(_exit_mi);
	
			_file_m .setMnemonic(KeyEvent.VK_F);
	
			// VIEW
			_view_m.add(_viewLogFile_mi);
			_view_m.add(_ase_viewConfig_mi);
			_view_m.add(_rs_configChangedDdl_mi);   
			_view_m.add(_rs_configAllDdl_mi);   
			_view_m.add(_rs_dumpQueue_mi);
			_view_m.add(_rsWhoIsDown_mi);
			
			_ase_viewConfig_mi     .setVisible(false);
			_rs_configChangedDdl_mi.setVisible(false);
			_rs_configAllDdl_mi    .setVisible(false);
			_rs_dumpQueue_mi       .setVisible(false);
			_rsWhoIsDown_mi        .setVisible(false);
	
			// TOOLS
			_tools_m.add(_toolDummy_mi);

			_toolDummy_mi.setVisible(false);

			
			_file_m .setMnemonic(KeyEvent.VK_T);

			//			_connect_mi        .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK));
//			_disconnect_mi     .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.ALT_MASK));
			_connect_mi        .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));
			_disconnect_mi     .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));

			_fNew_mi           .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			_fOpen_mi          .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			_fSave_mi          .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			_fSaveAs_mi        .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));

			_rsWhoIsDown_mi    .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

			// TOOLBAR
//			_connect_but    = SwingUtils.makeToolbarButton(Version.class, "connect16.gif",    ACTION_CONNECT,    this, "Connect to a ASE",         "Connect");
//			_disConnect_but = SwingUtils.makeToolbarButton(Version.class, "disconnect16.gif", ACTION_DISCONNECT, this, "Close the ASE Connection", "Disconnect");

			_toolbar.setLayout(new MigLayout("insets 0 0 0 3", "", "")); // insets Top Left Bottom Right
			_toolbar.add(_connect_but);
			_toolbar.add(_disconnect_but);
			_toolbar.add(new JSeparator(SwingConstants.VERTICAL), "grow");
			_toolbar.add(_viewLogFile_but);
			_toolbar.add(_cmdSql_but,      "hidemode 3");
			_toolbar.add(_cmdRcl_but,      "hidemode 3");
			_toolbar.add(_rsWhoIsDown_but, "hidemode 3");
			// Visibility for TOOLBAR components at startup
			_viewLogFile_but.setEnabled(false);
			_cmdSql_but     .setVisible(false);
			_cmdRcl_but     .setVisible(false);
			_rsWhoIsDown_but.setVisible(false);


			//--------------------------
			// MENU - Icons
			_connect_mi            .setIcon(SwingUtils.readImageIcon(Version.class, "images/connect16.gif"));
			_disconnect_mi         .setIcon(SwingUtils.readImageIcon(Version.class, "images/disconnect16.gif"));
			_exit_mi               .setIcon(SwingUtils.readImageIcon(Version.class, "images/close.gif"));
			_viewLogFile_mi        .setIcon(SwingUtils.readImageIcon(Version.class, "images/tail_logfile.png"));
			_ase_viewConfig_mi     .setIcon(SwingUtils.readImageIcon(Version.class, "images/config_ase_view.png"));
			_rs_configAllDdl_mi    .setIcon(SwingUtils.readImageIcon(Version.class, "images/repserver_config.png"));
			_rs_configChangedDdl_mi.setIcon(SwingUtils.readImageIcon(Version.class, "images/repserver_config.png"));
			_rs_dumpQueue_mi       .setIcon(SwingUtils.readImageIcon(Version.class, "images/view_rs_queue.png"));
			_rsWhoIsDown_mi        .setIcon(SwingUtils.readImageIcon(Version.class, "images/rs_admin_who_is_down.png"));

			//--------------------------
			// MENU - Actions
			_connect_mi   .setActionCommand(ACTION_CONNECT);
			_disconnect_mi.setActionCommand(ACTION_DISCONNECT);
			_fNew_mi      .setActionCommand(ACTION_FILE_NEW);
			_fOpen_mi     .setActionCommand(ACTION_FILE_OPEN);
//			_fClose_mi    .setActionCommand(ACTION_FILE_CLOSE);
			_fSave_mi     .setActionCommand(ACTION_FILE_SAVE);
			_fSaveAs_mi   .setActionCommand(ACTION_FILE_SAVE_AS);
			_exit_mi      .setActionCommand(ACTION_EXIT);

			_viewLogFile_mi        .setActionCommand(ACTION_VIEW_LOG_TAIL);
			_ase_viewConfig_mi     .setActionCommand(ACTION_VIEW_ASE_CONFIG);
			_rs_configChangedDdl_mi.setActionCommand(ACTION_RS_GENERATE_CHANGED_DDL);
			_rs_configAllDdl_mi    .setActionCommand(ACTION_RS_GENERATE_ALL_DDL);
			_rs_dumpQueue_mi       .setActionCommand(ACTION_RS_DUMP_QUEUE);
			_rsWhoIsDown_mi        .setActionCommand(ACTION_RS_WHO_IS_DOWN);

			//--------------------------
			// And the action listener
			_connect_mi     .addActionListener(this);
			_disconnect_mi  .addActionListener(this);
			_fNew_mi        .addActionListener(this);
			_fOpen_mi       .addActionListener(this);
//			_fClose_mi      .addActionListener(this);
			_fSave_mi       .addActionListener(this);
			_fSaveAs_mi     .addActionListener(this);
			_exit_mi        .addActionListener(this);

			_viewLogFile_mi        .addActionListener(this);
			_ase_viewConfig_mi     .addActionListener(this);
			_rs_configChangedDdl_mi.addActionListener(this);
			_rs_configAllDdl_mi    .addActionListener(this);
			_rs_dumpQueue_mi       .addActionListener(this);
			_rsWhoIsDown_mi        .addActionListener(this);
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
		ImageIcon icon = SwingUtils.readImageIcon(Version.class, "images/sql_query_window.png");
//		super.setIconImage(icon.getImage()); // works if we are a JFrame
//		((Frame)this.getOwner()).setIconImage(icon.getImage()); // works if we are a JDialog

		_window.setIconImage(icon.getImage());

		_closeConnOnExit = closeConnOnExit;

		// Arrange to quit the program when the user closes the window
		_window.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				if (_closeConnOnExit)
					close();

				File f = new File(_query.getFileFullPath());
				if (f.exists() && _query.isDirty())
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
							_query.save();
							_statusBar.setFilename(_query.getFileFullPath());
							_statusBar.setFilenameDirty(_query.isDirty());
						}
						catch (IOException ex)
						{
							SwingUtils.showErrorMessage("Problems Saving file", "Problems saving file", ex);
						}
					}
				}

				saveProps();
				saveWinProps();
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
				_aseVersion = AseConnectionUtils.getAseVersionNumber(conn);
				
				_connType = ConnectionDialog.ASE_CONN;
			}
			else
			{
				_connType = ConnectionDialog.OFFLINE_CONN;
//				_connType = ConnectionDialog.JDBC_CONN;
			}
		}

		// Set various components
		_exec.setToolTipText("Executes the select sql statement above (Ctrl-e)(Alt+e)(F5)(F9)."); 
		_exec.setMnemonic('e');

		_execGuiShowplan.setToolTipText("Executes the select sql statement above, but use the GUI Showplan, only in ASE 15.0 or above (Ctrl-Shift+e)(Alt+Shift+e)(Shift+F5)(Shift+F9)."); 
		_execGuiShowplan.setMnemonic('E');

		_setOptions.setToolTipText("Set various options, for example: set showplan on|off.");
		try {_setOptions = createSetOptionButton(null, _aseVersion);}
		catch (Throwable ex) {_logger.error("Problems creating the 'set options' button.",ex);}

//		_showplan.setToolTipText("<html>Show Graphical showplan for the sql statement (work with ASE 15.x).</html>");
		
		_dbs_cobx    .setToolTipText("<html>Change database context.</html>");
		_rsInTabs    .setToolTipText("<html>Check this if you want to have multiple result sets in individual tabs.</html>");
		_asPlainText .setToolTipText("<html>No fancy output, just write the output as 'plan text'.</html>");
		_showRowCount.setToolTipText("<html>" +
		                             "Show Row Count after each executes SQL Statement<br>" +
		                             "<br>" +
		                             "One problem with this is if/when you execute a Stored Procedure.<br>" +
		                             "A stored procedure normally has <b>many</b> upd/ins/del and selects.<br>" +
		                             "In those cases <b>several</b> Row Count will be displayed. (many more that <code>isql</code> will show you)<br>" +
		                             "This is just how the jConnect JDBC driver works.<br>" +
		                             "<br>" +
		                             "That's why this option is visible, so you can turn this on/off as you which!" +
		                             "</html>");
		_copy        .setToolTipText("<html>Copy All resultsets to clipboard, tables will be into ascii format.</html>");
		_query       .setToolTipText("<html>" +
									"Put your SQL query here.<br>" +
									"If you select text and press 'exec' only the highlighted text will be sent to the ASE.<br>" +
									"<br>" +
									"Note: <b>Ctrl+Space</b> Brings up code completion. This is <b>not</b> working good for the moment, but it will be enhanced.<br>" +
									"<br>" +
								"</html>");
		_query.setUseFocusableTips(false);
		
		_query.addCaretListener(this);

		// To set all RSyntaxTextAreaX components to use "_"
		RSyntaxUtilitiesX.setCharsAllowedInWords("_");
		// To set all _query components to use "_", this since it's of TextEditorPane, which extends RSyntaxTextArea
		RSyntaxUtilitiesX.setCharsAllowedInWords(_query, "_");

		_queryScroll.setLineNumbersEnabled(true);
		
		// Install some extra Syntax Highlighting for RCL and TSQL
		AsetuneTokenMaker.init();  

		// Setup Auto-Completion for SQL
		_compleationProviderAbstract = CompletionProviderAbstractSql.installAutoCompletion(conn, _query, _window, this);
//		_compleationProviderAbstract = CompletionProviderJdbc.installAutoCompletion(_query, _window, this);
//		_compleationProviderAbstract = CompletionProviderAse.installAutoCompletion(_query, _window, this);
//		_compleationProviderAbstract = CompletionProviderRepServer.installAutoCompletion(_query, _window, this);
//		CompletionProvider acProvider = createCompletionProvider();
//		AutoCompletion ac = new AutoCompletion(acProvider);
//		ac.install(_query);
//		ac.setShowDescWindow(true); // enable the "extra" descriptive window to the right of completion.
////		ac.setChoicesWindowSize(600, 600);
//		ac.setDescriptionWindowSize(600, 600);

		// add stuff to the right click menu of the text area 
		RSyntaxUtilitiesX.installRightClickMenuExtentions(_queryScroll, _window);

		// FIXME: new JScrollPane(_query)
		// But this is not working as I want it
		// It disables the "auto grow" of the _query window, which is problematic
		// maybe add a JSplitPane or simular...

		// Place the components within this window
		Container contentPane = _jframe != null ? _jframe.getContentPane() : _jdialog.getContentPane();
		contentPane.setLayout(new BorderLayout());
		JPanel top    = new JPanel(new BorderLayout());
		JPanel bottom = new JPanel(new MigLayout());
		
		setWatermarkAnchor(top);
		setWatermark();

		_splitPane.setTopComponent(top);
		_splitPane.setBottomComponent(bottom);
		_splitPane.setContinuousLayout(true);
//		_splitPane.setOneTouchExpandable(true);

		if (winType == WindowType.CMDLINE_JFRAME)
			contentPane.add(_toolbar, BorderLayout.NORTH);
		contentPane.add(_splitPane);

		top.add(_queryScroll, BorderLayout.CENTER);
		top.setMinimumSize(new Dimension(300, 100));

		bottom.add(_dbs_cobx,       "split 6, hidemode 2");
		bottom.add(_exec,           "");
		bottom.add(_execGuiShowplan,"");
		bottom.add(_rsInTabs,       "hidemode 2");
		bottom.add(_asPlainText,    "");
		bottom.add(_showRowCount,   "");
		bottom.add(_setOptions,     "");
//		bottom.add(_showplan,       "");
		bottom.add(_copy,           "right, wrap");
		bottom.add(_resPanelScroll,     "span 4, width 100%, height 100%, hidemode 3");
		bottom.add(_resPanelTextScroll, "span 4, width 100%, height 100%, hidemode 3");
//		bottom.add(_msgline, "dock south");
		bottom.add(_statusBar, "dock south");

		_resPanelScroll.getVerticalScrollBar()  .setUnitIncrement(16);
		_resPanelScroll.getHorizontalScrollBar().setUnitIncrement(16);

		_resPanelScroll    .setVisible(true);
		_resPanelTextScroll.setVisible(false);
		

		// ADD Ctrl+e, F5, F9
		_query.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_E,  Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), ACTION_EXECUTE);
		_query.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0), ACTION_EXECUTE);
		_query.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), ACTION_EXECUTE);

		// ADD Ctrl+Shift+e, Shift+F5, Shift+F9
		_query.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_E,  InputEvent.SHIFT_DOWN_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), ACTION_EXECUTE_GUI_SHOWPLAN);
		_query.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F9, InputEvent.SHIFT_DOWN_MASK), ACTION_EXECUTE_GUI_SHOWPLAN);
		_query.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, InputEvent.SHIFT_DOWN_MASK), ACTION_EXECUTE_GUI_SHOWPLAN);

		_query.getActionMap().put(ACTION_EXECUTE, new AbstractAction(ACTION_EXECUTE)
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
				_exec.doClick();
			}
		});
		_query.getActionMap().put(ACTION_EXECUTE_GUI_SHOWPLAN, new AbstractAction(ACTION_EXECUTE_GUI_SHOWPLAN)
		{
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e)
			{
				_execGuiShowplan.doClick();
			}
		});


		_exec           .addActionListener(this);
		_execGuiShowplan.addActionListener(this);
		_dbs_cobx       .addActionListener(this);
		_copy           .addActionListener(this);

		_exec           .setActionCommand(ACTION_EXECUTE);
		_execGuiShowplan.setActionCommand(ACTION_EXECUTE_GUI_SHOWPLAN);
		
		// Refresh the database list (if ASE)
		if (_conn != null && _connType == ConnectionDialog.ASE_CONN)
			setDbNames();

		// Kick of a initial SQL query, if one is specified.
		if (StringUtil.isNullOrBlank(sql))
		{
			String helper = "Write your SQL query here";
			_query.setText(helper);
			_query.setSelectionStart(0);
			_query.setSelectionEnd(helper.length());
		}
		else
		{
			_query.setText(sql);
			if (_conn != null)
				displayQueryResults(sql, false);
		}
		_query.setDirty(false);
		_statusBar.setFilename(_query.getFileFullPath());

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
				try
				{
					FileLocation loc = FileLocation.create(f);
					_query.load(loc, null);
					_statusBar.setFilename(_query.getFileFullPath());
				}
				catch (IOException ex)
				{
					SwingUtils.showErrorMessage("Problems Loading file", "Problems Loading file", ex);
				}
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
			CheckForUpdates.noBlockCheckSqlWindow(_jframe, false, true);
		}
	}

	private void loadProps()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		
		_lastFileNameSaveMax = conf.getIntProperty(PROPKEY_lastFileNameSaveMax,  DEFAULT_lastFileNameSaveMax);
		_asPlainText .setSelected( conf.getBooleanProperty(PROPKEY_asPlainText,  DEFAULT_asPlainText) );
		_showRowCount.setSelected( conf.getBooleanProperty(PROPKEY_showRowCount, DEFAULT_showRowCount) );

		loadFileHistory();
	}

	private void saveProps()
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null)
			return;

		if (_windowType == null || ( _windowType != null && _windowType != WindowType.CMDLINE_JFRAME) )
			return;
			
		conf.setProperty(PROPKEY_lastFileNameSaveMax, _lastFileNameSaveMax);
		conf.setProperty(PROPKEY_asPlainText,         _asPlainText .isSelected());
		conf.setProperty(PROPKEY_showRowCount,        _showRowCount.isSelected());
		
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

		conf.setProperty("QueryWindow.size.width",         _window.getSize().width);
		conf.setProperty("QueryWindow.size.height",        _window.getSize().height);

		conf.setProperty("QueryWindow.splitPane.location", _splitPane.getDividerLocation());

		if (_window.isVisible())
		{
			conf.setProperty("QueryWindow.size.pos.x",  _window.getLocationOnScreen().x);
			conf.setProperty("QueryWindow.size.pos.y",  _window.getLocationOnScreen().y);
		}
		
		File f = new File(_query.getFileFullPath());
		if (f.exists())
			conf.setProperty("QueryWindow.lastFileName", f.toString());
		else
			conf.remove("QueryWindow.lastFileName");
		
		conf.save();
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

		if (ACTION_VIEW_LOG_TAIL.equals(actionCmd))
			action_viewLogTail(e);

		// ACTION for "exec"
//		if (_exec.equals(source))
		if (ACTION_EXECUTE.equals(actionCmd))
			actionExecute(e, false);

		// ACTION for "GUI exec"
//		if (_execGuiShowplan.equals(source))
		if (ACTION_EXECUTE_GUI_SHOWPLAN.equals(actionCmd))
			actionExecute(e, true);

		// ACTION for "database context"
		if (_dbs_cobx.equals(source))
		{
			useDb( (String) _dbs_cobx.getSelectedItem() );
			
			// mark code completion for refresh
			if (_compleationProviderAbstract != null)
				_compleationProviderAbstract.setNeedRefresh(true);
		}
		
		// ACTION for "copy"
		if (_copy.equals(source))
			actionCopy(e);


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
		boolean isDirty = _query.isDirty();
		_statusBar.setFilenameDirty(isDirty);

		_fSave_mi  .setEnabled(isDirty);
		_fSaveAs_mi.setEnabled(isDirty);
	}
	/*---------------------------------------------------
	** END: implementing CaretListener
	**--------------------------------------------------*/


	/**
	 * Set the windws title
	 * @param srvStr servername we are connected to, null = not connected.
	 */
	private void setSrvInTitle(String srvStr)
	{
		String title = _titlePrefix;
		if (srvStr != null)
			title += " - " + srvStr;
		
		if (_jframe  != null) _jframe .setTitle(title);
		if (_jdialog != null) _jdialog.setTitle(title);
	}

	private void action_connect(ActionEvent e)
	{
		// Create a new dialog Window
		boolean checkAseCfg    = false;
		boolean showAseTab     = true;
		boolean showAseOptions = false;
		boolean showHostmonTab = false;
		boolean showPcsTab     = false;
		boolean showOfflineTab = true;
		boolean showJdbcTab    = true;

		_connectedToProductName    = null;
		_connectedToProductVersion = null;
		_connectedToServerName     = null;
		_connectedToSysListeners   = null;
		_connectedAsUser           = null;
		_connectedWithUrl          = null;

		// mark code completion for refresh
		if (_compleationProviderAbstract != null)
			_compleationProviderAbstract.setNeedRefresh(true);

		ConnectionDialog connDialog = new ConnectionDialog(_jframe, checkAseCfg, showAseTab, showAseOptions, showHostmonTab, showPcsTab, showOfflineTab, showJdbcTab);
		// Show the dialog and wait for response
		connDialog.setVisible(true);
		connDialog.dispose();

		// Get what was connected to...
		int connType = connDialog.getConnectionType();

		if ( connType == ConnectionDialog.CANCEL)
			return;

		// Get product info
		try	
		{
			_connectedToProductName    = connDialog.getDatabaseProductName(); 
			_connectedToProductVersion = connDialog.getDatabaseProductVersion(); 
			_connectedToServerName     = connDialog.getDatabaseServerName();
			_connectedToSysListeners   = null;
			_connectedAsUser           = connDialog.getUsername();
			_connectedWithUrl          = connDialog.getUrl();

			_logger.info("Connected to DatabaseProductName='"+_connectedToProductName+"', DatabaseProductVersion='"+_connectedToProductVersion+"', DatabaseServerName='"+_connectedToServerName+"' with Username='"+_connectedAsUser+"', toURL='"+_connectedWithUrl+"'.");
		} 
		catch (Throwable ex) 
		{
			if (_logger.isDebugEnabled())
				_logger.warn("Problems getting DatabaseProductName, DatabaseProductVersion, DatabaseServerName or Username. Caught: "+ex, ex);
			else
				_logger.warn("Problems getting DatabaseProductName, DatabaseProductVersion, DatabaseServerName or Username. Caught: "+ex);
		}
		
		if ( connType == ConnectionDialog.ASE_CONN)
		{
			_conn = connDialog.getAseConn();

//			if (_conn != null)
			if (AseConnectionUtils.isConnectionOk(_conn, true, _jframe))
			{
				_connType = ConnectionDialog.ASE_CONN;

				if (connDialog.isDatabaseProduct(ConnectionDialog.DB_PROD_NAME_SYBASE_ASE))
				{
					setDbNames();

					_compleationProviderAbstract = CompletionProviderAse.installAutoCompletion(_query, _window, this);

					_aseVersion              = AseConnectionUtils.getAseVersionNumber(_conn);

					// Only SA_ROLE can get listeners
					_connectedToSysListeners = "Sorry you need 'sa_role' to query master..syslisteners";
					if (AseConnectionUtils.hasRole(_conn, AseConnectionUtils.SA_ROLE))
						_connectedToSysListeners = AseConnectionUtils.getListeners(_conn, false, false, _window);

					// Also get "various statuses" like if we are in a transaction or not
					_aseConnectionStateInfo = AseConnectionUtils.getAseConnectionStateInfo(_conn);
					_statusBar.setAseConnectionStateInfo(_aseConnectionStateInfo);
					setWatermark();
				}
				else if (connDialog.isDatabaseProduct(ConnectionDialog.DB_PROD_NAME_SYBASE_RS))
				{
					_compleationProviderAbstract = CompletionProviderRepServer.installAutoCompletion(_query, _window, this);

					_aseVersion = AseConnectionUtils.getRsVersionNumber(_conn);
					_logger.info("Connected to Replication Server version '"+_aseVersion+"'.");
				}
				else
				{
					_compleationProviderAbstract = CompletionProviderJdbc.installAutoCompletion(_query, _window, this);

					_logger.info("Connected to 'other' Sybase TDS server with product name'"+_connectedToProductName+"'.");
				}

				_setOptions.setComponentPopupMenu( createSetOptionButtonPopupMenu(_aseVersion) );

				setComponentVisibility();
			}
		}
		else if ( connType == ConnectionDialog.OFFLINE_CONN)
		{
			_compleationProviderAbstract = CompletionProviderJdbc.installAutoCompletion(_query, _window, this);

			_conn = connDialog.getOfflineConn();
			_connType = ConnectionDialog.OFFLINE_CONN;

			_aseVersion = -1;

			setComponentVisibility();
		}
		else if ( connType == ConnectionDialog.JDBC_CONN)
		{
			_compleationProviderAbstract = CompletionProviderJdbc.installAutoCompletion(_query, _window, this);

			_conn = connDialog.getJdbcConn();
			_connType = ConnectionDialog.JDBC_CONN;

			_aseVersion = -1;

			setComponentVisibility();
		}
	}
	private void setComponentVisibility()
	{
		_rsInTabs.setVisible(false);

		// Set all to invisible, later set the ones that should be visible to true
		_ase_viewConfig_mi     .setVisible(false);
		_rs_configChangedDdl_mi.setVisible(false);
		_rs_configAllDdl_mi    .setVisible(false);
		_cmdSql_but            .setVisible(false);
		_cmdRcl_but            .setVisible(false);
		_rs_dumpQueue_mi       .setVisible(false);
		_rsWhoIsDown_mi        .setVisible(false);

		// view and tools menu might be empty...
		// if so hide the main menu entry as well
		SwingUtils.hideMenuIfNoneIsVisible(_view_m);
		SwingUtils.hideMenuIfNoneIsVisible(_tools_m);

		// sets Save, SaveAs to enabled or not
		caretUpdate(null);

		_dbs_cobx.setVisible(true);
		
//		if (_conn == null)
		if ( ! AseConnectionUtils.isConnectionOk(_conn, false, null) )
		{
			_connect_mi     .setEnabled(true);
			_connect_but    .setEnabled(true);
			_disconnect_mi  .setEnabled(false);
			_disconnect_but .setEnabled(false);
			
			_viewLogFile_but.setEnabled(false);
			_viewLogFile_mi .setEnabled(false);

			_dbs_cobx       .setEnabled(false);
			_exec           .setEnabled(false);
			_rsInTabs       .setEnabled(false);
			_asPlainText    .setEnabled(false);
			_showRowCount   .setEnabled(false);
			_setOptions     .setEnabled(false);
			_execGuiShowplan.setEnabled(false);
			
			setSrvInTitle("not connected");
			_statusBar.setServerName(null, null, null, null, null, null, null);

			return;
		}
		else
		{
			_connect_mi     .setEnabled(false);
			_connect_but    .setEnabled(false);
			_disconnect_mi  .setEnabled(true);
			_disconnect_but .setEnabled(true);

			_viewLogFile_but.setEnabled(true);
			_viewLogFile_mi .setEnabled(true);
		}

		if ( _connType == ConnectionDialog.ASE_CONN)
		{
			// Set server name in windows - title
			String aseSrv      = AseConnectionFactory.getServer();
			String aseHostPort = AseConnectionFactory.getHostPortStr();
			String srvStr      = aseSrv != null ? aseSrv : aseHostPort; 

			setSrvInTitle(srvStr);
			_statusBar.setServerName(srvStr, _connectedToProductName, _connectedToProductVersion, _connectedToServerName, _connectedAsUser, _connectedWithUrl, _connectedToSysListeners);
			
			if (_connectedToProductName != null && _connectedToProductName.equals(ConnectionDialog.DB_PROD_NAME_SYBASE_ASE))
			{
				_ase_viewConfig_mi     .setVisible(true);// _ase_viewConfig_mi.setEnabled(true);
				_cmdSql_but            .setVisible(true);

				_dbs_cobx       .setEnabled(true);
				_exec           .setEnabled(true);
				_rsInTabs       .setEnabled(true);
				_asPlainText    .setEnabled(true);
				_showRowCount   .setEnabled(true);
				_setOptions     .setEnabled(true);
				_execGuiShowplan.setEnabled( (_aseVersion >= 15000) );
			}
			else if (_connectedToProductName != null && _connectedToProductName.equals(ConnectionDialog.DB_PROD_NAME_SYBASE_RS))
			{
				_rs_configChangedDdl_mi.setVisible(true);
				_rs_configAllDdl_mi    .setVisible(true);
				_cmdRcl_but            .setVisible(true);
				_rs_dumpQueue_mi       .setVisible(true);
				_rsWhoIsDown_but       .setVisible(true);
				_rsWhoIsDown_mi        .setVisible(true);

				_dbs_cobx              .setVisible(false);
				_exec                  .setEnabled(true);
				_rsInTabs              .setEnabled(true);
				_asPlainText           .setEnabled(true);
				_showRowCount          .setEnabled(true);
				_setOptions            .setEnabled(false);
				_execGuiShowplan       .setEnabled(false);
			}
		}

		if ( _connType == ConnectionDialog.OFFLINE_CONN)
		{
			_dbs_cobx       .setEnabled(false);
			_exec           .setEnabled(true);
			_rsInTabs       .setEnabled(true);
			_asPlainText    .setEnabled(true);
			_showRowCount   .setEnabled(true);
			_setOptions     .setEnabled(false);
			_execGuiShowplan.setEnabled(false);

			setSrvInTitle(_conn.toString());
			_statusBar.setServerName(_conn.toString(), _connectedToProductName, _connectedToProductVersion, _connectedToServerName, _connectedAsUser, _connectedWithUrl, _connectedToSysListeners);
		}

		if ( _connType == ConnectionDialog.JDBC_CONN)
		{
			_dbs_cobx       .setEnabled(false);
			_exec           .setEnabled(true);
			_rsInTabs       .setEnabled(true);
			_asPlainText    .setEnabled(true);
			_showRowCount   .setEnabled(true);
			_setOptions     .setEnabled(false);
			_execGuiShowplan.setEnabled(false);

			setSrvInTitle(_connectedWithUrl);
			_statusBar.setServerName(_connectedWithUrl, _connectedToProductName, _connectedToProductVersion, _connectedToServerName, _connectedAsUser, _connectedWithUrl, _connectedToSysListeners);
		}
		
		// view and tools menu might be empty...
		// if so hide the main menu entry as well
		SwingUtils.hideMenuIfNoneIsVisible(_view_m);
		SwingUtils.hideMenuIfNoneIsVisible(_tools_m);
	}

	private void action_disconnect(ActionEvent e)
	{
		_connectedToProductName    = null;
		_connectedToProductVersion = null;
		_connectedToServerName     = null;
		_connectedAsUser           = null;
		_connectedWithUrl          = null;

		if (_conn != null)
		{
			try
			{
				_conn.close();
				_conn = null;
				_connType = -1;

				_rsWhoIsDown_but.setVisible(false);
				_viewLogFile_but.setEnabled(false);
				_viewLogFile_mi .setEnabled(false);

				_dbs_cobx       .setEnabled(false);
				_exec           .setEnabled(false);
				_rsInTabs       .setEnabled(false);
				_asPlainText    .setEnabled(false);
				_showRowCount   .setEnabled(false);
				_setOptions     .setEnabled(false);
				_execGuiShowplan.setEnabled(false);

				setSrvInTitle(null);
				_statusBar.setServerName(null, null, null, null, null, null, null);
			}
			catch (SQLException ex)
			{
				_logger.error("Problems closing database connection.", ex);
			}
		}
	}


	private void action_fileNew(ActionEvent e)
	{
//System.out.println("action_fileNew");

		boolean tryToSaveFile = false;
		if (_query.isDirty())
		{
			tryToSaveFile = true;

			File f = new File(_query.getFileFullPath());
			
			Object[] buttons = {"Save File", "Save As new file", "Discard changes"};
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
				return;
			}
			// Save As
			else if (answer == 1) 
			{
				action_fileSaveAs(e);
				tryToSaveFile = true;
				return;
			}
			// Discard
			else
			{
				tryToSaveFile = false;
			}
		}

		if (tryToSaveFile && _query.isDirty())
		{
			SwingUtils.showInfoMessage("New File", "Sorry the file couldn't be saved. Can't continue open another file.");
		}


		SwingUtils.showInfoMessage("New File", "Sorry not fully implemented yet.");

		// hmm do I need to do...
//		_query = new TextEditorPane();
//		...and then install all the "stuff" on _query
//		or probably a new method that does it all..._query like...
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
//			_query.load(loc, null);
//			_statusBar.setFilename(_query.getFileFullPath());
//			_statusBar.setFilenameDirty(_query.isDirty());
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
		if (_query.isDirty())
		{
			tryToSaveFile = true;

			File f = new File(_query.getFileFullPath());
			
			Object[] buttons = {"Save File", "Save As new file", "Discard changes"};
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
				return;
			}
			// Save As
			else if (answer == 1) 
			{
				action_fileSaveAs(e);
				tryToSaveFile = true;
				return;
			}
			// Discard
			else
			{
				tryToSaveFile = false;
			}
		}

		if (tryToSaveFile && _query.isDirty())
		{
			SwingUtils.showInfoMessage("Open File", "Sorry the file couldn't be saved. Can't continue open another file.");
		}


		// Open a file chooser
		if (fileToOpen == null)
		{
			File currentFilePath = new File(_query.getFileFullPath()).getParentFile();
			JFileChooser fc = new JFileChooser(currentFilePath);
			fc.setDialogType(JFileChooser.OPEN_DIALOG);
	
	//		if (System.getProperty("ASETUNE_SAVE_DIR") != null)
	//			fc.setCurrentDirectory(new File(System.getProperty("ASETUNE_SAVE_DIR")));
	
			int returnVal = fc.showOpenDialog(_window);
			if (returnVal == JFileChooser.APPROVE_OPTION) 
	        {
				File file = fc.getSelectedFile();
	
				//This is where a real application would open the file.
	//			String filename = file.getAbsolutePath();
	
	//			SwingUtils.showInfoMessage("action_fileOpen: Not yet implemented", "action_fileOpen("+filename+"): Not yet implemented");
				try
				{
					FileLocation loc = FileLocation.create(file);
					_query.load(loc, null);
					_statusBar.setFilename(_query.getFileFullPath());
					_statusBar.setFilenameDirty(_query.isDirty());
					
					addFileHistory(file);
				}
				catch (IOException ex)
				{
					SwingUtils.showErrorMessage("Problems Loading file", "Problems Loading file", ex);
				}
	        }		
		}
		else
		{
			File file = new File(fileToOpen);
			try
			{
				FileLocation loc = FileLocation.create(file);
				_query.load(loc, null);
				_statusBar.setFilename(_query.getFileFullPath());
				_statusBar.setFilenameDirty(_query.isDirty());
				
				addFileHistory(file);
			}
			catch (IOException ex)
			{
				SwingUtils.showErrorMessage("Problems Loading file", "Problems Loading file", ex);
			}
		}
	}


//	private void action_fileClose(ActionEvent e)
//	{
//		SwingUtils.showInfoMessage("action_fileClose:Not yet implemented", "action_fileClose:Not yet implemented");
//		System.out.println("action_fileClose");
//		_statusBar.setFilename(_query.getFileFullPath());
//		_statusBar.setFilenameDirty(_query.isDirty());
//	}


	private void action_fileSave(ActionEvent e)
	{
//System.out.println("action_fileSave");
		String fp = _query.getFileFullPath();
		String fn = _query.getFileName();
//System.out.println("action_fileSave: fp='"+fp+"', fn='"+fn+"'.");
		File f = new File(fp);
		if ("Untitled.txt".equals(fn) & !f.exists())
		{
			action_fileSaveAs(e);
			return;
		}

		try
		{
			_query.save();
			_statusBar.setFilename(_query.getFileFullPath());
			_statusBar.setFilenameDirty(_query.isDirty());
		}
		catch (IOException ex)
		{
			SwingUtils.showErrorMessage("Problems Saving file", "Problems saving file", ex);
		}
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

	private void action_fileSaveAs(ActionEvent e)
	{
//System.out.println("action_fileSaveAs");

		File currentFilePath = new File(_query.getFileFullPath()).getParentFile();
		JFileChooser fc = new JFileChooser(currentFilePath);
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
				_query.saveAs(loc);
				_statusBar.setFilename(_query.getFileFullPath());
				_statusBar.setFilenameDirty(_query.isDirty());

				addFileHistory(_query.getFileFullPath());
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
			false);
	}

	private void action_viewLogTail(ActionEvent e)
	{
//		SwingUtils.showInfoMessage("Not yet implemented", "<html><h2>Sorry NOT Yet Implemented.</h2></html>");
		LogTailWindow logTailDialog = new LogTailWindow(_conn);
		logTailDialog.setVisible(true);
		logTailDialog.startTail();
	}


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

		// Get the user's query and pass to displayQueryResults()
		String q = _query.getSelectedText();
		if ( q != null && !q.equals(""))
			displayQueryResults(q, guiShowplanExec);
		else
			displayQueryResults(_query.getText(), guiShowplanExec);
	}
	
	private void actionCopy(ActionEvent e)
	{
//		System.out.println("-------COPY---------");
		StringBuilder sb = getResultPanelAsText(_resPanel);

		if (sb != null)
		{
			StringSelection data = new StringSelection(sb.toString());
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(data, data);
		}
	}

	private StringBuilder getResultPanelAsText(JComponent panel)
	{
		StringBuilder sb = new StringBuilder();
//		String terminatorStr = "\n";
//		String terminatorStr = "----------------------------------------------------------------------------\n";

		for (int i=0; i<panel.getComponentCount(); i++)
		{
			Component comp = (Component) panel.getComponent(i);
			if (comp instanceof JPanel)
			{
//				JPanel p = (JPanel) comp;
//				String title = "";
//				Border border = p.getBorder();
//				if (border instanceof TitledBorder)
//				{
//					TitledBorder tb = (TitledBorder) border;
//					title = tb.getTitle();
//				}
//				sb.append("\n");
//				sb.append("#################################################################\n");
//				sb.append("## ").append(title).append("\n");
//				sb.append("#################################################################\n");
				sb.append( getResultPanelAsText( (JPanel)comp ) );
			}
			else if (comp instanceof JTabbedPane)
			{
				JTabbedPane tp = (JTabbedPane) comp;
				for (int t=0; t<tp.getTabCount(); t++)
				{
//					sb.append("\n");
//					sb.append("#################################################################\n");
//					sb.append("## ").append(tp.getTitleAt(t)).append("\n");
//					sb.append("#################################################################\n");
					Component tabComp = tp.getComponentAt(t);
					if (tabComp instanceof JComponent)
						sb.append( getResultPanelAsText((JComponent)tabComp) );
				}
			}
			else if (comp instanceof JTable)
			{
				JTable table = (JTable)comp;
//				String textTable = SwingUtils.tableToString(table.getModel());
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

		int width   = conf.getIntProperty("QueryWindow.size.width",         600);
		int height  = conf.getIntProperty("QueryWindow.size.height",        400);
		int winPosX = conf.getIntProperty("QueryWindow.size.pos.x",         -1);
		int winPosY = conf.getIntProperty("QueryWindow.size.pos.y",         -1);
		int divLoc  = conf.getIntProperty("QueryWindow.splitPane.location", -1);

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
		_query.setText(sql);
	}
	public String getSql()
	{
		return _query.getText();
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
			_conn.createStatement().execute("use "+dbname);
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

			String currentSelectedDb = (String) _dbs_cobx.getSelectedItem();
			if ( ! cwdb.equals(currentSelectedDb) )
			{
				// Note this triggers: code completion for refresh
				_dbs_cobx.setSelectedItem(cwdb);
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
	private void setDbNames()
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

			// Check if number of databases has changed
			int currentDbCount = _dbs_cobx.getItemCount();
			if (cbm.getSize() != currentDbCount)
			{
				_dbs_cobx.setModel(cbm);
			}

			// Check if Current Working database has changed
			String currentSelectedDb = (String) _dbs_cobx.getSelectedItem();
			if ( ! cwdb.equals(currentSelectedDb) )
			{
				// Note this triggers: code completion for refresh
				_dbs_cobx.setSelectedItem(cwdb);
			}
		}
		catch(SQLException e)
		{
			DefaultComboBoxModel cbm = new DefaultComboBoxModel();
			cbm.addElement("Problems getting dbnames");
			_dbs_cobx.setModel(cbm);
		}
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

		TablePopupFactory.createCopyTable(popup);

		// create pupup depending on what we are connected to
		String propPostfix = "";
		if (ConnectionDialog.DB_PROD_NAME_SYBASE_ASE.equals(_connectedToProductName))
		{
			propPostfix = "ase.";
		}
		else if (ConnectionDialog.DB_PROD_NAME_SYBASE_RS.equals(_connectedToProductName))
		{
			propPostfix = "rs.";
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

	private static class SqlProgressDialog
	extends JDialog
	implements PropertyChangeListener, ActionListener
	{
		private static final long serialVersionUID = 1L;

		private JLabel           _allSql_lbl            = new JLabel("Executing SQL at ASE Server", JLabel.CENTER);
		private JLabel           _msg_lbl               = new JLabel("Messages for current SQL Batch", JLabel.CENTER);

		private Connection       _conn                  = null;
		private JLabel           _state_lbl             = new JLabel();
		private boolean          _markSql               = false; // mark/move to current SQL in _allSql_txt 
		private RSyntaxTextAreaX _allSql_txt            = new RSyntaxTextAreaX();
		private RTextScrollPane  _allSql_sroll          = new RTextScrollPane(_allSql_txt);
		private RSyntaxTextAreaX _msg_txt               = new RSyntaxTextAreaX();
		private RTextScrollPane  _msg_sroll             = new RTextScrollPane(_msg_txt);
		private JButton          _cancel                = new JButton("Cancel");

		private Timer            _execSqlTimer          = new Timer(100, new ExecSqlTimerAction());
		private String           _currentExecSql        = null; 

		private boolean          _firstExec             = true;
		private long             _totalExecStartTime    = 0;
		private long             _batchExecStartTime    = 0;
		private JLabel           _totalExecTimeDesc_lbl = new JLabel("Total Exec Time: ");
		private JLabel           _totalExecTimeVal_lbl  = new JLabel("-");
		private JLabel           _batchExecTimeDesc_lbl = new JLabel("Batch Exec Time: ");
		private JLabel           _batchExecTimeVal_lbl  = new JLabel("-");

		private boolean          _firstMsgWasReceived   = false;
		private List<SQLException> _msgList             = new ArrayList<SQLException>();

//		private SwingWorker<String, Object>	_swingWorker = null;

		/**
		 * This timer is started just before we execute the SQL ststement that refreshes the data
		 * And it's stopped when the execution is finnished
		 * If X ms has elipsed in the database... show some info to any GUI that we are still in refresh... 
		 */
		private class ExecSqlTimerAction implements ActionListener
		{
			@Override
			public void actionPerformed(ActionEvent actionevent)
			{
				deferredTimerAction(_currentExecSql);
			}
		}

		public SqlProgressDialog(Window owner, Connection conn, String sql)
		{
			super((Frame)null, "Waiting for server...", true);
			setLayout(new MigLayout());

			_conn = conn;

			Font f = _totalExecTimeDesc_lbl.getFont();
			_allSql_lbl          .setFont(new java.awt.Font(Font.DIALOG, Font.BOLD, 16));
			_totalExecTimeVal_lbl.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
			_batchExecTimeVal_lbl.setFont(f.deriveFont(f.getStyle() | Font.BOLD));

			_cancel.setToolTipText("Send a CANCEL request to the server.");

			_allSql_txt.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL);
//			_allSql_txt.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
			_allSql_txt.setHighlightCurrentLine(true);
			//_sql_txt.setLineWrap(true);
			//_sql_sroll.setLineNumbersEnabled(true);
//			RSyntaxUtilitiesX.installRightClickMenuExtentions(_allSql_txt, this);
			_allSql_txt.setText(sql);
			_allSql_txt.setCaretPosition(0);
			_allSql_txt.setEditable(false);

//			_msg_txt.setText(sql);
//			_msg_txt.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
			_msg_txt.setHighlightCurrentLine(false);
			//_msg_txt.setLineWrap(true);
			//_msg_txt.setLineNumbersEnabled(true);
//			RSyntaxUtilitiesX.installRightClickMenuExtentions(_msg_txt, this);
			_msg_txt.setEditable(false);
			
			add(_allSql_lbl,            "pushx, growx, wrap");
			add(_state_lbl,             "wrap");
			add(_totalExecTimeDesc_lbl, "split, width 85");
			add(_totalExecTimeVal_lbl,  "wrap");
			add(_batchExecTimeDesc_lbl, "split, width 85");
			add(_batchExecTimeVal_lbl,  "wrap");
			add(_allSql_sroll,          "push, grow, wrap");
			add(_msg_lbl,               "pushx, growx, wrap, hidemode 3");
			add(_msg_sroll,             "hmin 150, push, grow, wrap, hidemode 3");
			add(_cancel,                "center");

			_msg_lbl.setVisible(false);
			_msg_sroll.setVisible(false);

			_cancel.addActionListener(this);

			pack();
			setSize( getSize().width + 100, getSize().height + 70);
			SwingUtils.setSizeWithingScreenLimit(this, 200);
			setLocationRelativeTo(owner);
		}
		
		public void setCurrentSqlText(String sql, boolean markSql)
		{
//System.out.println(">>>>>>: setCurrentSqlText(): sql="+sql);
			_markSql = markSql;
			if (_firstExec)
			{
				_firstExec = false;
				_totalExecStartTime = System.currentTimeMillis();
				_execSqlTimer.start();
			}
			_batchExecStartTime = System.currentTimeMillis();
			_currentExecSql = sql;
			_execSqlTimer.restart(); // dont kick off the times to early...

			// Update of WHAT SQL that is currently executed, is done by the deferredSetCurrentSqlText()
			// othetwise RSyntaxTextArea will have problems every now and then (if the scroll has moved and it needs to parse/update the visible rect)
		}

		/** 
		 * Executed by the Timer, which kicks of every X ms
		 * Updates, Total and Batch execution time...
		 * If a new SQL statement is executed, move to current executes SQL Statement
		 * 
		 * @param sql SQL Statement to move to in the dialog.
		 */
		private void deferredTimerAction(String sql)
		{
//			// 
//			if ( ! isVisible() && (_swingWorker != null && !_swingWorker.isDone()) )
//				setVisible(true);

//			_execSqlTimer.stop();
			_totalExecTimeVal_lbl.setText( TimeUtils.msToTimeStr("%MM:%SS.%ms", System.currentTimeMillis() - _totalExecStartTime) );
			_batchExecTimeVal_lbl.setText( TimeUtils.msToTimeStr("%MM:%SS.%ms", System.currentTimeMillis() - _batchExecStartTime) );

			_currentExecSql = null;
			if (StringUtil.isNullOrBlank(sql) && _msgList.size() > 0)
				return;

//System.out.println("XXXXXX: deferredTimerAction(): sql = " + sql);
//System.out.println("XXXXXX: deferredTimerAction(): _msgList.size() = " + _msgList.size());

			if ( ! StringUtil.isNullOrBlank(sql) )
			{
				if (_markSql)
				{
					// RSyntaxTextArea seems to have problems doing the blow, so make sure to catch problems
					try
					{
						// Reset messages
						_msg_txt.setText("");
		
						// Mark current SQL
						SearchContext sc = new SearchContext();
						sc.setSearchFor(sql);
						sc.setMatchCase(true);
						sc.setWholeWord(false);
						sc.setRegularExpression(false);
		
						// Position in text...
						SearchEngine.find(_allSql_txt, sc);
		
						// Mark it
						//_allSql_txt.markAll(sql, sc.getMatchCase(), sc.getWholeWord(), sc.isRegularExpression());
					}
					catch (Throwable t) 
					{
						_logger.warn("Problems updating current executing SQL Statement, but will continue anyway...", t);
					}
				}
			} // end: _markSql
			
			// add Messages
			if (_msgList.size() > 0)
			{
				// Copy the list and make a new one (if new messages are appended as we process them
				List<SQLException> oldMsgList = new ArrayList<SQLException>(_msgList);
				_msgList = new ArrayList<SQLException>();

				// Process messages
				for (SQLException sqle : oldMsgList)
				{
					// if RSyntaxTextArea has problem with this, catch it...
					try
					{
						// First time make the window a bit larger
						if (_firstMsgWasReceived == false)
						{
							Dimension dimW = getSize();
							dimW.height += 150;
							setSize(dimW);
		
							SwingUtils.setSizeWithingScreenLimit(this, 0);
						}
		
						_firstMsgWasReceived = true;
						_msg_lbl.setVisible(true);
						_msg_sroll.setVisible(true);
		
						String msg = AseConnectionUtils.getSqlWarningMsgs(sqle);
						_msg_txt.append(msg);
		
						_msg_txt.setCaretPosition(_msg_txt.getText().length());
		
//						SearchContext sc = new SearchContext();
//						sc.setSearchFor(msg);
//						sc.setMatchCase(true);
//						sc.setWholeWord(false);
//						sc.setRegularExpression(false);

//						// Position in text...
//						SearchEngine.find(_msg_txt, sc);
		
					}
					catch (Throwable t) 
					{
						_logger.warn("Problems adding a message to the progress dialog, but will continue anyway...", t);
					}
				}
			}
		}

		public void setState(String string)
		{
			_state_lbl.setText(string);
		}
		
		public void addMessage(SQLException sqle)
		{
			_msgList.add(sqle);
		}
//		public void addMessage(SQLException sqle)
//		{
//			// if RSyntaxTextArea has problem with this, catch it...
//			try
//			{
//				// First time make the window a bit larger
//				if (_firstMsgWasReceived == false)
//				{
////					Dimension dimS = _msg_sroll.getSize();
////					dimS.height += 150;
////					_msg_sroll.setSize(dimS);
//
//					Dimension dimW = getSize();
//					dimW.height += 150;
//					setSize(dimW);
//
//					SwingUtils.setSizeWithingScreenLimit(this, 0);
//				}
//
//				_firstMsgWasReceived = true;
//				_msg_lbl.setVisible(true);
//				_msg_sroll.setVisible(true);
//
//				String msg = AseConnectionUtils.getSqlWarningMsgs(sqle);
//				_msg_txt.append(msg);
//
//				_msg_txt.setCaretPosition(_msg_txt.getText().length());
//
////				SearchContext sc = new SearchContext();
////				sc.setSearchFor(msg);
////				sc.setMatchCase(true);
////				sc.setWholeWord(false);
////				sc.setRegularExpression(false);
////
////				// Position in text...
////				SearchEngine.find(_msg_txt, sc);
//
//			}
//			catch (Throwable t) 
//			{
//				_logger.warn("Problems adding a message to the progress dialog, but will continue anyway...", t);
//			}
//		}

		/**
		 * Called by SwingWorker on completion<br>
		 * Note: need to register on the SwingWorker using: workerThread.addPropertyChangeListener( "this SqlProgressDialog" );
		 */
		@Override
		public void propertyChange(PropertyChangeEvent event) 
		{
			// Close this window when the Swing worker has completed
			if ("state".equals(event.getPropertyName()) && StateValue.DONE == event.getNewValue()) 
			{
				setVisible(false);
				dispose();
			}
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			Object source = e.getSource();
			
			if (_cancel.equals(source))
			{
				if (_conn != null && _conn instanceof SybConnection)
				{
					try
					{
						((SybConnection)_conn).cancel();
					}
					catch(SQLException ex)
					{
						SwingUtils.showErrorMessage("Cancel", "Problems sending cancel to ASE: "+ex, ex);
					}
				}
			}
		}

//		@Override
//		public void setVisible(boolean state) 
//		{
//			super.setVisible(state);
//			if (state == false)
//				_execSqlTimer.stop();
//		}

		/**
		 * Wait for the background thread to execute before continue<br>
		 * If the execution takes longer than <code>graceTime</code> ms, then a dialog will be visible.
		 * 
		 * @param doBgThread
		 * @param graceTime wait this amount of ms until a dialog will be displayed.
		 */
		public void waitForExec(SwingWorker<String, Object> doBgThread, int graceTime)
		{
			_execSqlTimer.start();

			long startTime = System.currentTimeMillis();
			while (System.currentTimeMillis() - startTime < graceTime )
			{
				// if the bg job is done, get out of here
				if ( doBgThread.isDone() )
					break;

				// Sleep for 10ms, get out of here if we are interrupted.
				try { Thread.sleep(10); }
				catch (InterruptedException ignore) { break; }
			}

			//the dialog will be visible until the SwingWorker is done
			if ( ! doBgThread.isDone() )
			{
				setVisible(true);
			}

			_execSqlTimer.stop();
		}
	}

	public void displayQueryResults(final String sql, final boolean guiShowplanExec)
	{
		final SqlProgressDialog progress = new SqlProgressDialog(_window, _conn, sql);

		// Execute in a Swing Thread
		SwingWorker<String, Object> doBgThread = new SwingWorker<String, Object>()
		{
			@Override
			protected String doInBackground() throws Exception
			{
				if (guiShowplanExec)
				{
					_resPanel.removeAll();
					_resPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0 0, wrap"));
					
					JAseMessage noRsMsg = new JAseMessage("No result sets will be displayed in GUI exec mode.");
					_resPanel.add(noRsMsg, "gapy 1, growx, pushx");

					AsePlanViewer pv = new AsePlanViewer(_conn, sql);
					pv.setVisible(true);
				}
				else
				{
					_resPanel.removeAll();
					_resPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0 0, wrap"));
					
					JAseMessage noRsMsg = new JAseMessage("Sending Query to server.");
					_resPanel.add(noRsMsg, "gapy 1, growx, pushx");

					displayQueryResults(_conn, sql, progress);
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

		if (guiShowplanExec)
		{
			_resPanel.removeAll();
			_resPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0 0, wrap"));
			
			JAseMessage noRsMsg = new JAseMessage("No result sets will be displayed in GUI exec mode.");
			_resPanel.add(noRsMsg, "gapy 1, growx, pushx");
		}
		

		// if ASE, refresh the database list and currect working database
		if (_connectedToProductName != null && _connectedToProductName.equals(ConnectionDialog.DB_PROD_NAME_SYBASE_ASE))
		{
			// getCurrentDb() is also done in setDbNames()
			// it only refreshes the DB Combobox if number of databases has changed.
			setDbNames();
			
			// Also get "various statuses" like if we are in a transaction or not
			_aseConnectionStateInfo = AseConnectionUtils.getAseConnectionStateInfo(_conn);
			_statusBar.setAseConnectionStateInfo(_aseConnectionStateInfo);
			setWatermark();
		}
	}

	
	private void putSqlWarningMsgs(ResultSet rs, ArrayList<JComponent> resultCompList, String debugStr, int batchStartRow)
	{
		if (rs == null)
			return;
		try
		{
			putSqlWarningMsgs(rs.getWarnings(), resultCompList, debugStr, batchStartRow);
			rs.clearWarnings();
		}
		catch (SQLException e)
		{
		}
	}
	private void putSqlWarningMsgs(Statement stmnt, ArrayList<JComponent> resultCompList, String debugStr, int batchStartRow)
	{
		if (stmnt == null)
			return;
		try
		{
			putSqlWarningMsgs(stmnt.getWarnings(), resultCompList, debugStr, batchStartRow);
			stmnt.clearWarnings();
		}
		catch (SQLException e)
		{
		}
	}

	private void putSqlWarningMsgs(SQLException sqe, ArrayList<JComponent> resultCompList, String debugStr, int batchStartRow)
	{
		while (sqe != null)
		{
			StringBuilder sb = new StringBuilder();
			if(sqe instanceof EedInfo)
			{
				// Error is using the addtional TDS error data.
				EedInfo eedi = (EedInfo) sqe;
				if(eedi.getSeverity() > 10)
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
					sb.append( (firstOnLine ? "" : ", ") +
							"Line " + eedi.getLineNumber() + (batchStartRow >= 0 ? " (script row "+(batchStartRow+eedi.getLineNumber())+")" : "") +
							", Status " + eedi.getStatus() + 
							", TranState " + eedi.getTranState() + ":\n");
				}
				// Now print the error or warning
				String msg = sqe.getMessage();
				if (msg.endsWith("\n"))
					sb.append(msg);
				else
					sb.append(msg+"\n");
			}
			else
			{
				// SqlState: 010P4 java.sql.SQLWarning: 010P4: An output parameter was received and ignored.
				if ( ! sqe.getSQLState().equals("010P4") )
				{
					sb.append("Unexpected exception : " +
							"SqlState: " + sqe.getSQLState()  +
							" " + sqe.toString() +
							", ErrorCode: " + sqe.getErrorCode() + "\n");
				}
			}
			
			// Add the info to the list
			if (sb.length() > 0)
			{
				// If new-line At the end, remove it
				if ( sb.charAt(sb.length()-1) == '\n' )
					sb.deleteCharAt(sb.length()-1);

				String aseMsg = sb.toString();
				resultCompList.add( new JAseMessage(aseMsg) );

				if (_logger.isTraceEnabled())
					_logger.trace("ASE Msg("+debugStr+"): "+aseMsg);
			}

			sqe = sqe.getNextException();
		}
	}

	/**
	 * This method uses the supplied SQL query string, and the
	 * ResultSetTableModelFactory object to create a TableModel that holds
	 * the results of the database query.  It passes that TableModel to the
	 * JTable component for display.
	 **/
	private void displayQueryResults(Connection conn, String goSql, final SqlProgressDialog progress)
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
					if (ConnectionDialog.DB_PROD_NAME_SYBASE_RS.equals(_connectedToProductName))
					{
						if (sqle.getErrorCode() == 32)
						{
							if (_logger.isDebugEnabled())
								_logger.debug("Discarding RepServer Message: "+ AseConnectionUtils.sqlExceptionToString(sqle));
							return null;
						}
					}
					
					// Add it to the progress dialog
					progress.addMessage(sqle);

					// If we want to STOP if we get any errors that is a SQLError (not SQLWarnings)
					if (_abortOnDbMessages)
						return sqle;

					// Downgrade ALL messages to warnings, so executions wont be interuppted.
					return AseConnectionUtils.sqlExceptionToWarning(sqle);
				}
			});
		}

		try
		{
			Statement  stmnt = conn.createStatement();
			ResultSet  rs    = null;
			int rowsAffected = 0;

			// a linked list where to "store" result sets or messages
			// before "displaying" them
			_resultCompList = new ArrayList<JComponent>();

			String sql = "";
			// treat each 'go' rows as a individual execution
			// readCommand(), does the job
//			int batchCount = AseSqlScript.countSqlGoBatches(goSql);
//			int cmdCount = 0;
//			BufferedReader br = new BufferedReader( new StringReader(goSql) );
//			for(String s1=AseSqlScript.readCommand(br); s1!=null; s1=AseSqlScript.readCommand(br))
//			{
			AseSqlScriptReader sr = new AseSqlScriptReader(goSql, true);
			int batchCount = sr.getSqlTotalBatchCount();
			for (sql = sr.getSqlBatchString(); sql != null; sql = sr.getSqlBatchString())
			{
				// FIXME: create a Reader, 
				//        that has batchStartRow, 
				//        goExecutionsNumber(go 20)
				//        can read "exit"/"quit"/"reset" and other isql commands
				// The reader or (batchStartRow) should be passed to putSqlWarningMsgs() (probably NOT to ConnectionMessageHandler)
//				int cmdCount = sr.getSqlBatchNumber();
				progress.setState("Sending SQL to ASE for statement " + sr.getSqlBatchNumber());

				// This can't be part of the for loop, then it just stops if empty row
				if ( StringUtil.isNullOrBlank(sql) )
					continue;

				progress.setCurrentSqlText(sql, batchCount > 1);

				// if 'go 10' we need to execute this 10 times
				for (int cmd=0; cmd<sr.getMultiExecCount(); cmd++)
				{
					// Execute
					_logger.debug("Executing SQL statement: "+sql);
					boolean hasRs = stmnt.execute(sql);
		
					progress.setState("Waiting for ASE to deliver resultset.");
//					_msgline.setText("Waiting for ASE to deliver resultset.");
					_statusBar.setMsg("Waiting for ASE to deliver resultset.");
		
					// iterate through each result set
					int rsCount = 0;
					do
					{
						// Append, messages and Warnings to _resultCompList, if any
						putSqlWarningMsgs(stmnt, _resultCompList, "-before-hasRs-", sr.getSqlBatchStartLine());
		
						if(hasRs)
						{
							rsCount++;
//							_msgline.setText("Reading resultset "+rsCount+".");
							_statusBar.setMsg("Reading resultset "+rsCount+".");
		
							// Get next resultset to work with
							rs = stmnt.getResultSet();
		
							// Append, messages and Warnings to _resultCompList, if any
							putSqlWarningMsgs(stmnt, _resultCompList, "-after-getResultSet()-Statement-", sr.getSqlBatchStartLine());
							putSqlWarningMsgs(rs,    _resultCompList, "-after-getResultSet()-ResultSet-", sr.getSqlBatchStartLine());

							if (_asPlainText.isSelected())
							{
								ResultSetTableModel tm = new ResultSetTableModel(rs, true, sql, sr.getPipeCmd());
								for (SQLWarning sqlw : tm.getSQLWarningList())
									putSqlWarningMsgs(sqlw, _resultCompList, "-after-ResultSetTableModel()-tm.getSQLWarningList()-", sr.getSqlBatchStartLine());
								
								_resultCompList.add(new JPlainResultSet(tm));
								// FIXME: use a callback interface instead
							}
							else
							{
								// Convert the ResultSet into a TableModel, which fits on a JTable
								ResultSetTableModel tm = new ResultSetTableModel(rs, true, sql, sr.getPipeCmd());
								for (SQLWarning sqlw : tm.getSQLWarningList())
									putSqlWarningMsgs(sqlw, _resultCompList, "-after-ResultSetTableModel()-tm.getSQLWarningList()-", sr.getSqlBatchStartLine());
			
								// Create the JTable, using the just created TableModel/ResultSet
								JXTable tab = new JXTable(tm);
								tab.setSortable(true);
								tab.setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);
								tab.packAll(); // set size so that all content in all cells are visible
								tab.setColumnControlVisible(true);
								tab.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
								tab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
//								SwingUtils.calcColumnWidths(tab);

								// Add a popup menu
								tab.setComponentPopupMenu( createDataTablePopupMenu(tab) );

//								for(int i=0; i<tm.getColumnCount(); i++)
//								{
//									Object o = tm.getValueAt(0, i);
//									if (o!=null)
//										System.out.println("Col="+i+", Class="+o.getClass()+", Comparable="+((o instanceof Comparable)?"true":"false"));
//									else
//										System.out.println("Col="+i+", ---NULL--");
//								}
								// Add the JTable to a list for later use
								_resultCompList.add(tab);
								// FIXME: use a callback interface instead
							}
		
							// Append, messages and Warnings to _resultCompList, if any
							putSqlWarningMsgs(stmnt, _resultCompList, "-before-rs.close()-", sr.getSqlBatchStartLine());

							// Close it
							rs.close();
						}
		
						// Treat update/row count(s)
						rowsAffected = stmnt.getUpdateCount();
						if (rowsAffected >= 0)
						{
//							rso.add(rowsAffected);
//System.out.println("hasRs="+hasRs+", rowsAffected="+rowsAffected);
							if (_showRowCount.isSelected())
							{
//								System.out.println("(" + rowsAffected + " rows affected)\n");
//								_resultCompList.add( new JAseMessage("(" + rowsAffected + " rows affected)") );
								_resultCompList.add( new JAseRowCount(rowsAffected) );
							}
						}
		
						// Check if we have more resultsets
						// If any SQLWarnings has not been found above, it will throw one here
						// so catch raiserrors or other stuff that is not SQLWarnings.
						hasRs = stmnt.getMoreResults();
		
						_logger.trace( "--hasRs="+hasRs+", rowsAffected="+rowsAffected );
					}
					while (hasRs || rowsAffected != -1);
		
					// Append, messages and Warnings to _resultCompList, if any
					putSqlWarningMsgs(stmnt, _resultCompList, "-before-stmnt.close()-", sr.getSqlBatchStartLine());
					
				} // end: 'go 10'
				
			} // end: read batches
			
//			br.close();
			sr.close();

			// Close the statement
			stmnt.close();

			
			progress.setState("Add data to GUI result");


			//-----------------------------
			// Add data... to panel(s) in various ways
			// - one result set, just add it
			// - many result sets
			//        - Add to JTabbedPane
			//        - OR: append the result sets as named panels
			//-----------------------------
			_resPanel.removeAll();
			_resPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0 0, wrap"));

			// Release this, if '_asPlainText' is enabled it will be set at the end... 
			_resultText = null;



			int numOfTables = countTables(_resultCompList);
			if (numOfTables == 1)
			{
				_resPanelScroll    .setVisible(true);
				_resPanelTextScroll.setVisible(false);

				int msgCount = 0;
				int rowCount = 0;
				_logger.trace("Only 1 RS");

				// Add ResultSet  
				for (JComponent jcomp: _resultCompList)
				{
					if (jcomp instanceof JTable)
					{
						JTable tab = (JTable) jcomp;

						// JScrollPane is on _resPanel
						// So we need to display the table header ourself
						JPanel p = new JPanel(new MigLayout("insets 0 0 0 0, gap 0 0, wrap"));
						p.add(tab.getTableHeader(), "wrap");
						p.add(tab,                  "wrap");

						_logger.trace("1-RS: add: JTable");
						_resPanel.add(p, "");

						rowCount = tab.getRowCount();
					}
					else if (jcomp instanceof JAseMessage)
					{
						JAseMessage msg = (JAseMessage) jcomp;
						_logger.trace("1-RS: JAseMessage: "+msg.getText());
						_resPanel.add(msg, "gapy 1, growx, pushx");

						msgCount++;
					}
				}
//				_msgline.setText(" "+rowCount+" rows, and "+msgCount+" messages.");
				_statusBar.setMsg(" "+rowCount+" rows, and "+msgCount+" messages.");
			}
			else if (numOfTables > 1)
			{
				_resPanelScroll    .setVisible(true);
				_resPanelTextScroll.setVisible(false);

				int msgCount = 0;
				int rowCount = 0;
				_logger.trace("Several RS: "+_resultCompList.size());
				
				if (_rsInTabs.isSelected())
				{
					// Add Result sets to individual tabs, on a JTabbedPane 
					JTabbedPane tabPane = new JTabbedPane();
					_logger.trace("JTabbedPane: add: JTabbedPane");
					_resPanel.add(tabPane, "");

					int i = 1;
					for (JComponent jcomp: _resultCompList)
					{
						if (jcomp instanceof JTable)
						{
							JTable tab = (JTable) jcomp;

							// JScrollPane is on _resPanel
							// So we need to display the table header ourself
							JPanel p = new JPanel(new MigLayout("insets 0 0 0 0, gap 0 0, wrap"));
							p.add(tab.getTableHeader(), "wrap");
							p.add(tab,                  "wrap");

							_logger.trace("JTabbedPane: add: JTable("+i+")");
							tabPane.addTab("Result "+(i++), p);

							rowCount += tab.getRowCount();
						}
						else if (jcomp instanceof JAseMessage)
						{
							// FIXME: this probably not work if we want to have the associated messages in the correct tab
							JAseMessage msg = (JAseMessage) jcomp;
							_resPanel.add(msg, "gapy 1, growx, pushx");
							_logger.trace("JTabbedPane: JAseMessage: "+msg.getText());

							msgCount++;
						}
					}
					if (_lastTabIndex > 0)
					{
						if (_lastTabIndex < tabPane.getTabCount())
						{
							tabPane.setSelectedIndex(_lastTabIndex);
							_logger.trace("Restore last tab index pos to "+_lastTabIndex);
						}
					}
//					_msgline.setText(" "+numOfTables+" ResultSet with totally "+rowCount+" rows, and "+msgCount+" messages.");
					_statusBar.setMsg(" "+numOfTables+" ResultSet with totally "+rowCount+" rows, and "+msgCount+" messages.");
				}
				else
				{
					// Add Result sets to individual panels, which are 
					// appended to the result panel
					int i = 1;
					for (JComponent jcomp: _resultCompList)
					{
						if (jcomp instanceof JTable)
						{
							JTable tab = (JTable) jcomp;

							// JScrollPane is on _resPanel
							// So we need to display the table header ourself
							JPanel p = new JPanel(new MigLayout("insets 0 0, gap 0 0"));
							Border border = BorderFactory.createTitledBorder("ResultSet "+(i++));
							p.setBorder(border);
							p.add(tab.getTableHeader(), "wrap");
							p.add(tab,                  "wrap");
							_logger.trace("JPane: add: JTable("+i+")");
							_resPanel.add(p, "");

							rowCount += tab.getRowCount();
						}
						else if (jcomp instanceof JAseMessage)
						{
							JAseMessage msg = (JAseMessage) jcomp;
							_logger.trace("JPane: JAseMessage: "+msg.getText());
							_resPanel.add(msg, "gapy 1, growx, pushx");

							msgCount++;
						}
					}
//					_msgline.setText(" "+numOfTables+" ResultSet with totally "+rowCount+" rows, and "+msgCount+" messages.");
					_statusBar.setMsg(" "+numOfTables+" ResultSet with totally "+rowCount+" rows, and "+msgCount+" messages.");
				}
			}
			else
			{
				_resPanelScroll    .setVisible(false);
				_resPanelTextScroll.setVisible(true);

				_logger.trace("NO RS: "+_resultCompList.size());
				int msgCount = 0;

				RSyntaxTextAreaX out = new RSyntaxTextAreaX();
				RSyntaxUtilitiesX.installRightClickMenuExtentions(out, _resPanelTextScroll, _window);
				installResultTextExtraMenuEntries(out);
				_resPanelTextScroll.setViewportView(out);
				_resPanelTextScroll.setLineNumbersEnabled(true);

				// set this globaly as well
				_resultText = out;

				// Copy results to the output. 
				boolean prevComponentWasResultSet = false;

				for (JComponent jcomp: _resultCompList)
				{
					if (jcomp instanceof JPlainResultSet)
					{
						JPlainResultSet prs = (JPlainResultSet) jcomp;
						out.append(prs.getText());
						
						// if _showRowCount is selected, the "(### rows affected)"
						// is genereated at an earlier stage, so then we do NOT need to add it once more here
						if ( ! _showRowCount.isSelected() )
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

//				_msgline.setText("NO ResultSet, but "+msgCount+" messages.");
				_statusBar.setMsg("NO ResultSet, but "+msgCount+" messages.");
			}
			
			// We're done, so clear the feedback message
			//_msgline.setText(" ");
		}
		catch (SQLException ex)
		{
			// If something goes wrong, clear the message line
//			_msgline.setText("Error: "+ex.getMessage());
			_statusBar.setMsg("Error: "+ex.getMessage());
//			ex.printStackTrace();

			// Then display the error in a dialog box
			JOptionPane.showMessageDialog(
//					QueryWindow.this, 
					_window, 
					new String[] { // Display a 2-line message
							ex.getClass().getName() + ": ", 
							ex.getMessage() },
					"Error", JOptionPane.ERROR_MESSAGE);
		}
		catch (IOException ex)
		{
			_logger.error("While reading the input SQL 'go' String, caught: "+ex, ex);
		}
		catch (UnknownPipeCommandException ex)
		{
			_logger.warn("Problems creating the 'go | pipeCommand'. Caught: "+ex, ex);
			SwingUtils.showWarnMessage("Problems creating PipeCommand", ex.getMessage(), ex);
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
					displayQueryResults(cmd, false);
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
			{
				count++;
			}
		}
		return count;
	}

	@Override
	public SQLException messageHandler(SQLException sqe)
	{
		// Pass Warning on...
		if (sqe instanceof SQLWarning)
			return sqe;

		// Discard SQLExceptions... but first send them to the _resultCompList
		// This is a bit ugly...
		putSqlWarningMsgs(sqe, _resultCompList, "-from-messageHandler()-", -1);
		return null;
	}

	private class JAseMessage 
	extends JTextArea
	{

		private static final long serialVersionUID = 1L;

//		public JAseMessage()
//		{
//			_init();
//		}

		public JAseMessage(final String s)
		{
			super(s);
			init();
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

	private class JAseRowCount 
	extends JAseMessage
	{
		private static final long serialVersionUID = 1L;

		private int _rowCount;

		public JAseRowCount(final int rowCount)
		{
			super("(" + rowCount + " rows affected)");
			_rowCount = rowCount;
			init();
		}
		
		@SuppressWarnings("unused")
		public int getRowCount()
		{
			return _rowCount;
		}
	}

	private class JPlainResultSet 
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

		if (aseVersion >= 15020) 
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
		if (aseVersion >= 15030) 
			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_SWITCH, "set switch on 3604,9529 with override", "set switch off 3604,9529", "switch 3604,9529", false, "Traceflag 3604,9529: Include Lava operator execution statistics and resource use in a showplan format at most detailed level."));
		options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_SET, "set statistics io ON-OFF",            null, "statistics io",            false, "Number of logical and physical IO's per table"));
		options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_SET, "set statistics time ON-OFF",          null, "statistics time",          false, "Compile time and elapsed time"));
		options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_SET, "set statistics subquerycache ON-OFF", null, "statistics subquerycache", false, "Statistics about internal subquery optimizations"));
		if (aseVersion >= 15000) 
			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_SET, "set statistics plancost ON-OFF",      null, "statistics plancost",      false, "Query plan in tree format, includes estimated/actual rows and IO's"));
		if (aseVersion >= 15020) 
			options.add(new AseOptionOrSwitch(AseOptionOrSwitch.TYPE_SET, "set statistics resource ON-OFF",      null, "statistics resource",      false, "Resource usage, includes procedure cache and tempdb"));

		if (aseVersion >= 15020)
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

		//
		// If we want to get what switches are enabled, and 'x' check the box or not...
		// Lets do this LATER
		// show switch - just shows traceflags
		// I dont know how to get 'set showplan' or other options... sysoptions does NOT record them
		//
//		if (aseVersion >= 15020)
//		{
//			popupMenu.addPopupMenuListener(new PopupMenuListener()
//			{
//				@Override
//				public void popupMenuWillBecomeVisible(PopupMenuEvent e)
//				{
//					// Get current switch settings
//					String sql = "show switch";
//					try
//					{
//						Statement stmnt = _conn.createStatement();
//						stmnt.executeUpdate(sql);
//						String wmsg = AseConnectionUtils.getSqlWarningMsgs(stmnt.getWarnings());
//						if ( ! StringUtil.isNullOrBlank(wmsg) )
//						{
//							_logger.info("popupMenuWillBecomeVisible: "+wmsg);
//							stmnt.clearWarnings();
//						}
//						stmnt.close();
//
//						// Add all integers in the config to a Set
//						Set<Integer> flags = new LinkedHashSet<Integer>();
//						String configStr = wmsg;
//						String[] sa = configStr.split("\\s"); // \s = A whitespace character: [ \t\n\x0B\f\r] 
//						for (String str : sa)
//						{
//							try
//							{
//								int intTrace = Integer.parseInt(str.replace(",", ""));
//								flags.add(intTrace);
//							}
//							catch (NumberFormatException ignore) {/*ignore*/}
//						}
//						if (flags.size() > 0)
//						{
//						}
//						System.out.println("Active Trace flags: "+flags);
//					}
//					catch (SQLException ex)
//					{
//						_logger.warn("Problems execute SQL '"+sql+"', Caught: " + ex.toString(), ex);
//					}
//				}
//				@Override
//				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {/*empty*/}
//				@Override
//				public void popupMenuCanceled(PopupMenuEvent e)	{/*empty*/}
//			});
//		}

		
		return popupMenu;
	}

	/**
	 * Create a JButton that can enable/disable available Graphs for a specific CounterModel
	 * @param button A instance of JButton, if null is passed a new Jbutton will be created.
	 * @param cmName The <b>long</b> or <b>short</b> name of the CounterModel
	 * @return a JButton (if one was passed, it's the same one, but if null was passed a new instance is created)
	 */
	private JButton createSetOptionButton(JButton button, final int aseVersion)
	{
		if (button == null)
			button = new JButton();

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
			String selectedText = _query.getSelectedText();

			// Then try the RESULT output
			if (selectedText == null && _resultText != null)
				selectedText = _resultText.getSelectedText();

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

	private JPopupMenu createSqlCommandsButtonPopupMenu(final int version)
	{
		final String SEPARATOR = "SEPARATOR";
		LinkedHashMap<String, String> commands = new LinkedHashMap<String, String>();

		commands.put("select DBNAME=db_name(), SERVER=@@servername, VERSION=@@version", "What db, server are we connected to");
		commands.put("sp_who",                                        "Who is logged in on the system");
		commands.put("sp_helpdb",                                     "What databases are on this ASE");
		commands.put("sp_helpdevice",                                 "What devices are on this ASE");
		commands.put("sp_configure 'nondefault'",                     "Get <b>changed</b> configuration parameters");
		commands.put("sp_helptext '${selectedText}', NULL, NULL, 'showsql,linenumbers'", "Get stored procedure text");
		commands.put("sp_help '${selectedText}'",                     "Get more information about a object");
		commands.put("sp_spaceused '${selectedText}'",                "How much space does a table consume");
		commands.put("sp_helprotect '${selectedText}'",               "Who can do what with an object");
		commands.put("sp_helpcache",                                  "Get caches and sizes");
		commands.put("sp_cacheconfig",                                "Get cache configurations");
//		commands.put("", "");
//		commands.put("", "");
		commands.put(SEPARATOR+".99",                                 SEPARATOR);
		commands.put("",                                              "Note: Use Ctrl+Space to get code assist for more predefined SQL and table/column/procedure/etc completion...");


		// Do PopupMenu
		final JPopupMenu popupMenu = new JPopupMenu();
		
		// Add user defined Statements
		popupMenu.add( createUserDefinedMenu(null, "ud.menu.sql.", Configuration.getInstance(Configuration.USER_CONF)) );
		popupMenu.add( new JSeparator() );
		
		// Now add the above commands...
		for (String cmd : commands.keySet())
		{
			String val = commands.get(cmd);

			if (SEPARATOR.equals(val))
			{
				popupMenu.add(new JSeparator());
				continue;
			}

			// Add entry
			JMenuItem mi;

			String toolTipText = commands.get(cmd);
			String miText = "<html><b>"+cmd+"</b> - <i><font color=\"green\">"+toolTipText+"</font></i></html>";

			mi = new JMenuItem();
			mi.setText(miText);
			mi.setActionCommand(cmd);
			mi.setToolTipText(toolTipText);
//			mi.putClientProperty("CMD_SQL", cmd);

			mi.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					JMenuItem mi = (JMenuItem) e.getSource();

					String cmd = mi.getActionCommand();
					cmd = replaceTemplateStringForCommandButton("${selectedText}", cmd);
					if (cmd != null)
						displayQueryResults(cmd, false);
				}
			});
			popupMenu.add(mi);
		}
		
		return popupMenu;
	}

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

	private JPopupMenu createRclCommandsButtonPopupMenu(final int version)
	{
		final String SEPARATOR = "SEPARATOR";
		LinkedHashMap<String, String> commands = new LinkedHashMap<String, String>();

		commands.put("admin health",                       "Displays status information.  Status is HEALTHY when all threads are running, otherwise SUSPECT.");
		commands.put("admin who",                          "What threads are in the server");
		commands.put("admin who_is_down",                  "Displays the threads that are not running.");
		commands.put("admin disk_space",                   "Displays the state and amount of used space for disk partitions");
		commands.put("admin logical_status",               "Displays status of logical connections of Warm Standby");
		commands.put("admin who, sqm",                     "Displays status information about all queues");
		commands.put("admin who, sqt",                     "Displays status information about the transactions of each queue");
		commands.put("admin who, dist",                    "Displays information about DIST thread");
		commands.put("admin who, dsi",                     "Displays information about each DSI scheduler thread running in the Replication Server");
		commands.put("admin show_connections",             "Displays information about all connections from the Replication Server to data servers and to other Replication Servers");
		commands.put("admin rssd_name",                    "Where is the RSSD located");
		commands.put("admin version",                      "Show Replication Server Version");

		commands.put(SEPARATOR+".1",                       SEPARATOR);
		
		commands.put("trace 'on', 'dsi', 'dsi_buf_dump'",  "Turn ON: Write SQL statements executed by the DSI Threads to the RS log");
		commands.put("trace 'off', 'dsi', 'dsi_buf_dump'", "Turn OFF: Write SQL statements executed by the DSI Threads to the RS log");

		commands.put(SEPARATOR+".2",                       SEPARATOR);
		
		commands.put("RSSD: rmp_queue ''",                 "Show Queue size for each database/connection");
		commands.put("RSSD: rs_helpdb",                    "What databases are connected to the system");
		commands.put("RSSD: rs_helpdbrep",                 "Database Replication Definitions");
		commands.put("RSSD: rs_helpdbsub",                 "Database Subscriptions");
		commands.put("RSSD: rs_helprep",                   "Table Replication Definitions");
		commands.put("RSSD: rs_helpsub",                   "Table Subscriptions");
		commands.put("RSSD: rs_helpuser",                  "Users in <b>this</b> Replication Server");

		commands.put(SEPARATOR+".99",                      SEPARATOR);
		commands.put("",                                   "Note: Use Ctrl+Space to get code assist for more predefined RCL and other object completions...");

		// Do PopupMenu
		final JPopupMenu popupMenu = new JPopupMenu();

		// Add user defined Statements
		popupMenu.add( createUserDefinedMenu(null, "ud.menu.rcl.", Configuration.getInstance(Configuration.USER_CONF)) );
		popupMenu.add( new JSeparator() );
		
		// Now add the above commands...
		for (String cmd : commands.keySet())
		{
			String val = commands.get(cmd);

			if (SEPARATOR.equals(val))
			{
				popupMenu.add(new JSeparator());
				continue;
			}

			// Add entry
			JMenuItem mi;

			String toolTipText = val;
			String miText = "<html><b>"+cmd+"</b> - <i><font color=\"green\">"+toolTipText+"</font></i></html>";

			mi = new JMenuItem();
			mi.setText(miText);
			mi.setActionCommand(cmd);
			mi.setToolTipText(toolTipText);
//			mi.putClientProperty("CMD_SQL", cmd);

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
						displayQueryResults(cmd, false);
				}
			});
			popupMenu.add(mi);
		}
		
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
	
	/**
	 * 
	 * @param menu   if null a new JMenuPopup will be created otherwise it will be appended to it
	 * @param prefix prefix of the property string. Should contain a '.' at the end
	 * @param conf
	 * @param owner 
	 * @return
	 */
	public JMenu createUserDefinedMenu(JMenu menu, final String prefix, Configuration conf)
	{
		_logger.debug("createUserDefinedMenu(): prefix='"+prefix+"'.");

		//Create the popup menu.
		if (menu == null)
		{
			menu = new JMenu("Use Defined Statements");
		}

		boolean addedEntries = false;
		for (String prefixStr : conf.getUniqueSubKeys(prefix, true))
		{
			_logger.debug("createUserDefinedMenu(): found prefix '"+prefixStr+"'.");

			// Read cmd
			String menuCmd = conf.getPropertyRaw(prefixStr+".cmd");

			// Read menu text
			String menuText = conf.getPropertyRaw(prefixStr+".name");

			// Read menu tooltip
			String menuTooltip = conf.getPropertyRaw(prefixStr+".tooltip");

			// Read menu icon
			String menuIcon = conf.getPropertyRaw(prefixStr+".icon");

			// Check that we got everything we needed
			if (menuCmd == null)
			{
				_logger.warn("Missing property '"+prefixStr+".cmd'");
				continue;
			}

			if ("SEPARATOR".equals(menuCmd))
			{
				menu.add(new JSeparator());
				continue;
			}

			// Add entry
			JMenuItem mi;

			String text        = (menuText    == null ? menuCmd : menuText);
			String toolTipText = (menuTooltip == null ? "" : menuTooltip);
			String miText      = "<html><b>"+text+"</b> - <i><font color=\"green\">"+toolTipText+"</font></i></html>";

			mi = new JMenuItem();
			mi.setText(miText);
			mi.setActionCommand(menuCmd);
			mi.setToolTipText(menuTooltip == null ? menuCmd : menuTooltip);
			if (menuText != null && menuTooltip != null)
				mi.setToolTipText(menuCmd);
//			mi.putClientProperty("CMD_SQL", cmd);

			if (menuIcon != null)
				mi.setIcon(SwingUtils.readImageIcon(Version.class, menuIcon));

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
						displayQueryResults(cmd, false);
				}
			});
			addedEntries = true;
			menu.add(mi);
		}

		if ( ! addedEntries )
		{
			JMenuItem mi = new JMenuItem(
					"<html>" +
					"<b>Add user defined menu items by adding records in:</b><br>" +
					"File: " + conf.getFilename() + "<br>" +
					"<br>" +
					"With Entries: <br>" +
					"<code>" +
					prefix + "01.cmd     = CMD to execute        <b>(mandatory)</b><br>" +
					prefix + "01.name    = Text to show in menu  <b>(optional)</b><br>" +
					prefix + "01.tooltip = tooltip for this menu <b>(optional)</b><br>" +
					prefix + "01.icon    = path to a icon        <b>(optional)</b><br>" +
					"</code>" +
					"<br>" +
					"Menu items are added in the order of: " + prefix + "<b>##</b>.cmd<br>" +
					"Add a separator by: " + prefix + "<b>##</b>.cmd=SEPARATOR<br>" +
					"<br>" +
					"<b>Choosing this item will place a template in the copy paste buffer<br>" +
					"This will make it easy for you to add entries in the config file.</b>" +
					"</html>");
			mi.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					String template = 
						prefix + "01.cmd     = select * from sysobjects where type = 'U'\n" +
						prefix + "01.name    = List User Tables in current db\n" +
						prefix + "01.tooltip = some additional text for USER TABLES.\n" +
						prefix + "01.icon    = /path/to/icon/name.png\n" +
						"\n" +
						prefix + "02.cmd     = select * from sysobjects where type = 'P'\n" +
						prefix + "02.name    = List Procedures in current db\n" +
						prefix + "02.tooltip = some additional text for PRODECURES\n" +
						prefix + "02.icon    = /path/to/icon/name.png\n" +
						"\n";

					// Add it to clipboard
					StringSelection data = new StringSelection(template);
					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					clipboard.setContents(data, data);
				}
			});
			menu.add(mi);
		}

		return menu;
	}
	
	
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
	
	/*---------------------------------------------------
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
		else if ( _aseConnectionStateInfo != null && ( _aseConnectionStateInfo._tranCount > 0 || _aseConnectionStateInfo._tranState != AseConnectionUtils.ConnectionStateInfo.TSQL_TRAN_SUCCEED) )
		{
			String str = _aseConnectionStateInfo.getTranStateDescription() + "\n@@trancount = " + _aseConnectionStateInfo._tranCount;
			setWatermarkText(str);
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
		pw.println("  -q,--query <sqlStatement> SQL Statement to execute");
		pw.println("  -i,--inputFile <filename> Input File to open in editor");
		pw.println("");
		pw.flush();
	}

	/**
	 * Build the options parser. Has to be synchronized because of the way
	 * Options are constructed.
	 * 
	 * @return an options parser.
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
		// create the command line parser
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

	//---------------------------------------------------
	// MAIN
	//---------------------------------------------------
	public static void main(String[] args)
	{
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


