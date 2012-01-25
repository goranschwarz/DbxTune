/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.tools;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.MouseInfo;
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
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
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;
import javax.swing.UIManager;
import javax.swing.border.Border;
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
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jdesktop.swingx.JXTable;

import com.asetune.DebugOptions;
import com.asetune.Version;
import com.asetune.gui.AsePlanViewer;
import com.asetune.gui.ConnectionDialog;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.AseSqlScript;
import com.asetune.utils.Configuration;
import com.asetune.utils.ConnectionFactory;
import com.asetune.utils.Debug;
import com.asetune.utils.JavaVersion;
import com.asetune.utils.JdbcCompleationProvider;
import com.asetune.utils.Logging;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;
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
	implements ActionListener, SybMessageHandler, ConnectionFactory, JdbcCompleationProvider.ConnectionProvider
{
	private static Logger _logger = Logger.getLogger(QueryWindow.class);
	private static final long serialVersionUID = 1L;

	public enum WindowType 
	{
		/** Create the QueryWindow using a JFrame, meaning it would have a Icon in the Task bar, but from CmdLine */
		CMDLINE_JFRAME, 

		/** Create the QueryWindow using a JFrame, meaning it would have a Icon in the Task bar */
		JFRAME, 

		/** Create the QueryWindow using a JDialog, meaning it would NOT have a Icon in the Task bar */
		JDIALOG, 

		/** Create the QueryWindow using a JDialog, with modal option set to true. */
		JDIALOG_MODAL 
	}

	/** Completion Provider for RSyntaxTextArea */
	private JdbcCompleationProvider _jdbcTableCompleationProvider = null;

	//-------------------------------------------------
	// Actions
	public static final String ACTION_CONNECT                   = "CONNECT";
	public static final String ACTION_DISCONNECT                = "DISCONNECT";
	public static final String ACTION_EXIT                      = "EXIT";

	private Connection  _conn            = null;
	private int         _connType        = -1;
//	private JTextArea	_query           = new JTextArea();           // A field to enter a query in
	private RSyntaxTextArea	_query       = new RSyntaxTextArea();     // A field to enter a query in
	private RTextScrollPane _queryScroll     = new RTextScrollPane(_query);
	private JButton     _exec            = new JButton("Exec");       // Execute the
	private JButton     _execGuiShowplan = new JButton("Exec, GUI Showplan");    // Execute, but display it with a GUI showplan
	private JButton     _setOptions      = new JButton("Set");        // Do various set ... options
	private JButton     _copy            = new JButton("Copy Res");    // Copy All resultsets to clipboard
//	private JCheckBox   _showplan        = new JCheckBox("GUI Showplan", false);
	private JCheckBox   _rsInTabs        = new JCheckBox("Resultsets in Tabbed Panel", false);
	private JComboBox   _dbs_cobx        = new JComboBox();
	private JPanel      _resPanel        = new JPanel();
	private JScrollPane _resPanelScroll  = new JScrollPane(_resPanel);
	private JLabel	    _msgline         = new JLabel("");	     // For displaying messages
	private JSplitPane  _splitPane       = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	private int         _lastTabIndex    = -1;
	private boolean     _closeConnOnExit = true;
	private Font        _aseMsgFont      = null;
	private ArrayList<JComponent> _resultCompList  = null;
	
	private int         _aseVersion      = 0;

	// The base Window can be either a JFrame or a JDialog
	private Window      _window          = null;
	private JFrame      _jframe          = null;
	private JDialog     _jdialog         = null;
	private String      _titlePrefix     = null;

	private JButton     _connect_but     = SwingUtils.makeToolbarButton(Version.class, "connect16.gif",    ACTION_CONNECT,    this, "Connect to a ASE",         "Connect");
	private JButton     _disconnect_but  = SwingUtils.makeToolbarButton(Version.class, "disconnect16.gif", ACTION_DISCONNECT, this, "Close the ASE Connection", "Disconnect");

	// if we start from the CMD Line, add a few extra stuff
	//---------------------------------------
	private JMenuBar            _main_mb                = new JMenuBar();

	private JToolBar            _toolbar                = new JToolBar();

	// File
	private JMenu               _file_m                 = new JMenu("File");
	private JMenuItem           _connect_mi             = new JMenuItem("Connect...");
	private JMenuItem           _disconnect_mi          = new JMenuItem("Disconnect");
	private JMenuItem           _exit_mi                = new JMenuItem("Exit");
	//---------------------------------------

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
		if (cmd.hasOption('U'))	aseUsername = cmd.getOptionValue('U');
		if (cmd.hasOption('P'))	asePassword = cmd.getOptionValue('P');
		if (cmd.hasOption('S'))	aseServer   = cmd.getOptionValue('S');
		if (cmd.hasOption('D'))	aseDbname   = cmd.getOptionValue('D');
		if (cmd.hasOption('q'))	sqlQuery    = cmd.getOptionValue('q');

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

		String hostPortStr = "";
		if (aseServer.indexOf(":") == -1)
			hostPortStr = AseConnectionFactory.getIHostPortStr(aseServer);
		else
			hostPortStr = aseServer;

		_logger.info("Connecting as user '"+aseUsername+"' to server='"+aseServer+"'. Which is located on '"+hostPortStr+"'.");
		Connection conn = null;
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


		// Create a QueryWindow component that uses the factory object.
		QueryWindow qw = new QueryWindow(conn, sqlQuery, true, QueryWindow.WindowType.CMDLINE_JFRAME);
		qw.openTheWindow();
	}

	/**
	 * This constructor method creates a simple GUI and hooks up an event
	 * listener that updates the table when the user enters a new query.
	 **/
	public QueryWindow(Connection conn, WindowType winType)
	{
		this(conn, null, true, winType);
	}
	public QueryWindow(Connection conn, boolean closeConnOnExit, WindowType winType)
	{
		this(conn, null, closeConnOnExit, winType);
	}
	public QueryWindow(Connection conn, String sql, WindowType winType)
	{
		this(conn, sql, true, winType);
	}
	public QueryWindow(Connection conn, String sql, boolean closeConnOnExit, WindowType winType)
	{
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

		//--------------------------
		// MENU - composition
//		if (_jframe != null)
		if (winType == WindowType.CMDLINE_JFRAME)
		{
			_jframe.setJMenuBar(_main_mb);
	
			_main_mb.add(_file_m);
	
			_file_m.add(_connect_mi);
			_file_m.add(_disconnect_mi);
			_file_m.add(_exit_mi);
	
			_file_m .setMnemonic(KeyEvent.VK_F);
	
			_connect_mi        .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK));
			_disconnect_mi     .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.ALT_MASK));

			// TOOLBAR
//			_connect_but    = SwingUtils.makeToolbarButton(Version.class, "connect16.gif",    ACTION_CONNECT,    this, "Connect to a ASE",         "Connect");
//			_disConnect_but = SwingUtils.makeToolbarButton(Version.class, "disconnect16.gif", ACTION_DISCONNECT, this, "Close the ASE Connection", "Disconnect");

			_toolbar.setLayout(new MigLayout("insets 0 0 0 3", "", "")); // insets Top Left Bottom Right
			_toolbar.add(_connect_but);
			_toolbar.add(_disconnect_but);

			//--------------------------
			// MENU - Icons
			_connect_mi   .setIcon(SwingUtils.readImageIcon(Version.class, "images/connect16.gif"));
			_disconnect_mi.setIcon(SwingUtils.readImageIcon(Version.class, "images/disconnect16.gif"));
			_exit_mi      .setIcon(SwingUtils.readImageIcon(Version.class, "images/close.gif"));

			//--------------------------
			// MENU - Actions
			_connect_mi   .setActionCommand(ACTION_CONNECT);
			_disconnect_mi.setActionCommand(ACTION_DISCONNECT);
			_exit_mi      .setActionCommand(ACTION_EXIT);

			//--------------------------
			// And the action listener
			_connect_mi   .addActionListener(this);
			_disconnect_mi.addActionListener(this);
			_exit_mi      .addActionListener(this);
		}


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
			public void windowClosing(WindowEvent e)
			{
				if (_closeConnOnExit)
					close();
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
		
		_dbs_cobx.setToolTipText("<html>Change database context.</html>");
		_rsInTabs.setToolTipText("<html>Check this if you want to have multiple result sets in individual tabs.</html>");
		_copy    .setToolTipText("<html>Copy All resultsets to clipboard, tables will be into ascii format.</html>");
		_query   .setToolTipText("<html>" +
									"Put your SQL query here.<br>" +
									"If you select text and press 'exec' only the highlighted text will be sent to the ASE.<br>" +
									"<br>" +
									"Note: <b>Ctrl+Space</b> Brings up code completion. This is <b>not</b> working good for the moment, but it will be enhanced.<br>" +
									"<br>" +
									"<br>" +
								"</html>");

		_query.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
		_queryScroll.setLineNumbersEnabled(true);
		
		// Setup Auto-Completion for SQL
		CompletionProvider acProvider = createCompletionProvider();
		AutoCompletion ac = new AutoCompletion(acProvider);
		ac.install(_query);
		ac.setShowDescWindow(true); // enable the "extra" descriptive window to the right of completion.
//		ac.setChoicesWindowSize(600, 600);
		ac.setDescriptionWindowSize(600, 600);

		JPopupMenu menu =_query.getPopupMenu();
		menu.addSeparator();
		
		JCheckBoxMenuItem mi;
		mi = new JCheckBoxMenuItem("Word Wrap", _query.getLineWrap());
		mi.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				_query.setLineWrap( ! _query.getLineWrap() );
			}
		});
		menu.add(mi);

		mi = new JCheckBoxMenuItem("Line Numbers", _queryScroll.getLineNumbersEnabled());
		mi.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				_queryScroll.setLineNumbersEnabled( ! _queryScroll.getLineNumbersEnabled() );
			}
		});
		menu.add(mi);

		mi = new JCheckBoxMenuItem("Current Line Highlight", _queryScroll.getLineNumbersEnabled());
		mi.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				_query.setHighlightCurrentLine( ! _query.getHighlightCurrentLine() );
			}
		});
		menu.add(mi);

		// FIXME: new JScrollPane(_query)
		// But this is not working as I want it
		// It disables the "auto grow" of the _query window, which is problematic
		// maybe add a JSplitPane or simular...

		// Place the components within this window
		Container contentPane = _jframe != null ? _jframe.getContentPane() : _jdialog.getContentPane();
		contentPane.setLayout(new BorderLayout());
		JPanel top    = new JPanel(new BorderLayout());
		JPanel bottom = new JPanel(new MigLayout());
		_splitPane.setTopComponent(top);
		_splitPane.setBottomComponent(bottom);
		_splitPane.setContinuousLayout(true);
//		_splitPane.setOneTouchExpandable(true);

		if (winType == WindowType.CMDLINE_JFRAME)
			contentPane.add(_toolbar, BorderLayout.NORTH);
		contentPane.add(_splitPane);

		top.add(_queryScroll, BorderLayout.CENTER);
		top.setMinimumSize(new Dimension(300, 100));

		bottom.add(_dbs_cobx,       "split 5");
		bottom.add(_exec,           "");
		bottom.add(_execGuiShowplan,"");
		bottom.add(_rsInTabs,       "");
		bottom.add(_setOptions,     "");
//		bottom.add(_showplan,       "");
		bottom.add(_copy,           "right, wrap");
		bottom.add(_resPanelScroll, "span 4, width 100%, height 100%");
		bottom.add(_msgline, "dock south");

		_resPanelScroll.getVerticalScrollBar()  .setUnitIncrement(16);
		_resPanelScroll.getHorizontalScrollBar().setUnitIncrement(16);

		// Set components if visible, enabled etc...
		setComponentVisibility();
		
		// ADD Ctrl+e, F5, F9
		_query.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_E,  InputEvent.CTRL_DOWN_MASK), "execute");
		_query.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0), "execute");
		_query.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "execute");

		// ADD Ctrl+Shift+e, Shift+F5, Shift+F9
		_query.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_E,  InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK), "executeGuiShowplan");
		_query.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F9, InputEvent.SHIFT_DOWN_MASK), "executeGuiShowplan");
		_query.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, InputEvent.SHIFT_DOWN_MASK), "executeGuiShowplan");

		_query.getActionMap().put("execute", new AbstractAction("execute")
		{
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e)
			{
				_exec.doClick();
			}
		});
		_query.getActionMap().put("executeGuiShowplan", new AbstractAction("executeGuiShowplan")
		{
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e)
			{
				_execGuiShowplan.doClick();
			}
		});


		_exec           .addActionListener(this);
		_execGuiShowplan.addActionListener(this);
		_dbs_cobx       .addActionListener(this);
		_copy           .addActionListener(this);

//		// ACTION for "exec"
//		_exec.addActionListener(new ActionListener()
//		{
//			public void actionPerformed(ActionEvent e)
//			{
//				actionExecute(e, false);
//			}
//		});
//
//		// ACTION for "exec"
//		_execGuiShowplan.addActionListener(new ActionListener()
//		{
//			public void actionPerformed(ActionEvent e)
//			{
//				actionExecute(e, true);
//			}
//		});
//
//		// ACTION for "database context"
//		_dbs_cobx.addActionListener(new ActionListener()
//		{
//			public void actionPerformed(ActionEvent e)
//			{
//				useDb( (String) _dbs_cobx.getSelectedItem() );
//				
//				// mark code completion for refresh
//				_jdbcTableCompleationProvider.setNeedRefresh();
//			}
//		});
//		
//		// ACTION for "copy"
//		_copy.addActionListener(new ActionListener()
//		{
//			public void actionPerformed(ActionEvent e)
//			{
//				actionCopy(e);
//			}
//		});


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

		// Set initial size of the JFrame, and make it visable
		//this.setSize(600, 400);
		//this.setVisible(true);
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

		// ACTION for "exec"
		if (_exec.equals(source))
			actionExecute(e, false);

		// ACTION for "GUI exec"
		if (_execGuiShowplan.equals(source))
			actionExecute(e, true);

		// ACTION for "database context"
		if (_dbs_cobx.equals(source))
		{
			useDb( (String) _dbs_cobx.getSelectedItem() );
			
			// mark code completion for refresh
			_jdbcTableCompleationProvider.setNeedRefresh();
		}
		
		// ACTION for "copy"
		if (_copy.equals(source))
			actionCopy(e);


		setComponentVisibility();
	}
	/*---------------------------------------------------
	** END: implementing ActionListener
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

		// mark code completion for refresh
		_jdbcTableCompleationProvider.setNeedRefresh();

		ConnectionDialog connDialog = new ConnectionDialog(_jframe, checkAseCfg, showAseTab, showAseOptions, showHostmonTab, showPcsTab, showOfflineTab);
		// Show the dialog and wait for response
		connDialog.setVisible(true);
		connDialog.dispose();

		// Get what was connected to...
		int connType = connDialog.getConnectionType();

		if ( connType == ConnectionDialog.CANCEL)
			return;
		
		if ( connType == ConnectionDialog.ASE_CONN)
		{
			_conn = connDialog.getAseConn();

//			if (_conn != null)
			if (AseConnectionUtils.isConnectionOk(_conn, true, _jframe))
			{
				_connType = ConnectionDialog.ASE_CONN;
				
				setDbNames();
				_aseVersion = AseConnectionUtils.getAseVersionNumber(_conn);

				_setOptions.setComponentPopupMenu( createSetOptionButtonPopupMenu(_aseVersion) );

				setComponentVisibility();
			}
		}
		else if ( connType == ConnectionDialog.OFFLINE_CONN)
		{
			_conn = connDialog.getOfflineConn();
			_connType = ConnectionDialog.OFFLINE_CONN;

			_aseVersion = -1;

			setComponentVisibility();
		}
	}
	private void setComponentVisibility()
	{
		
//		if (_conn == null)
		if ( ! AseConnectionUtils.isConnectionOk(_conn, false, null) )
		{
			_connect_mi     .setEnabled(true);
			_connect_but    .setEnabled(true);
			_disconnect_mi  .setEnabled(false);
			_disconnect_but .setEnabled(false);
			
			_dbs_cobx       .setEnabled(false);
			_exec           .setEnabled(false);
			_rsInTabs       .setEnabled(false);
			_setOptions     .setEnabled(false);
			_execGuiShowplan.setEnabled(false);
			
			setSrvInTitle("not connected");

			return;
		}
		else
		{
			_connect_mi     .setEnabled(false);
			_connect_but    .setEnabled(false);
			_disconnect_mi  .setEnabled(true);
			_disconnect_but .setEnabled(true);
		}

		if ( _connType == ConnectionDialog.ASE_CONN)
		{
			_dbs_cobx       .setEnabled(true);
			_exec           .setEnabled(true);
			_rsInTabs       .setEnabled(true);
			_setOptions     .setEnabled(true);
			_execGuiShowplan.setEnabled( (_aseVersion >= 15000) );

			// Set servername in windows - title
			String aseSrv      = AseConnectionFactory.getServer();
			String aseHostPort = AseConnectionFactory.getHostPortStr();
			String srvStr      = aseSrv != null ? aseSrv : aseHostPort; 

			setSrvInTitle(srvStr);
		}

		if ( _connType == ConnectionDialog.OFFLINE_CONN)
		{
			_dbs_cobx       .setEnabled(false);
			_exec           .setEnabled(true);
			_rsInTabs       .setEnabled(true);
			_setOptions     .setEnabled(false);
			_execGuiShowplan.setEnabled(false);

			setSrvInTitle(_conn.toString());
		}
	}

	private void action_disconnect(ActionEvent e)
	{
		if (_conn != null)
		{
			try
			{
				_conn.close();
				_conn = null;
				_connType = -1;

				_dbs_cobx       .setEnabled(false);
				_exec           .setEnabled(false);
				_rsInTabs       .setEnabled(false);
				_setOptions     .setEnabled(false);
				_execGuiShowplan.setEnabled(false);

				setSrvInTitle(null);
			}
			catch (SQLException ex)
			{
				_logger.error("Problems closing database connection.", ex);
			}
		}
	}

	private void action_exit(ActionEvent e)
	{
		_jframe.dispatchEvent(new WindowEvent(_jframe, WindowEvent.WINDOW_CLOSING));
	}


	private void actionExecute(ActionEvent e, boolean guiExec)
	{
		if ( ! AseConnectionUtils.isConnectionOk(_conn, false, null) )
			action_connect(e);

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
			displayQueryResults(q, guiExec);
		else
			displayQueryResults(_query.getText(), guiExec);
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
	
	public void openTheWindow() 
	{
		openTheWindow(600, 400);
	}
	public void openTheWindow(int width, int height) 
	{
//		this.setSize(width, width);
		_window.setSize(width, width);

		// Create a Runnable to set the main visible, and get Swing to invoke.
		SwingUtilities.invokeLater(new Runnable() 
		{
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

//		this.setVisible(true);
		_window.setVisible(true);
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
			_dbs_cobx.setSelectedItem(cwdb);
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
			_dbs_cobx.setModel(cbm);

			_dbs_cobx.setSelectedItem(cwdb);
		}
		catch(SQLException e)
		{
			DefaultComboBoxModel cbm = new DefaultComboBoxModel();
			cbm.addElement("Problems getting dbnames");
			_dbs_cobx.setModel(cbm);
		}
	}

	/*---------------------------------------------------
	** BEGIN: implementing ConnectionFactory
	**---------------------------------------------------
	*/
	public Connection getConnection(String appname)
	{
		try
		{
			return AseConnectionFactory.getConnection(null, appname, null);
		}
		catch (Exception e)  // SQLException, ClassNotFoundException
		{
			_logger.error("Problems getting a new Connection", e);
			return null;
		}
	}
	/*---------------------------------------------------
	** END: implementing ConnectionFactory
	**---------------------------------------------------
	*/

	/*---------------------------------------------------
	** BEGIN: implementing JdbcCompletionConnectionProvider
	**--------------------------------------------------*/
	public Connection getConnection()
	{
		return _conn;
	}
	/*---------------------------------------------------
	** END: implementing JdbcCompletionConnectionProvider
	**--------------------------------------------------*/




	private JPopupMenu createDataTablePopupMenu(JTable table)
	{
		_logger.debug("createDataTablePopupMenu(): called.");

		JPopupMenu popup = new JPopupMenu();

		TablePopupFactory.createCopyTable(popup);

		TablePopupFactory.createMenu(popup, 
			TablePopupFactory.TABLE_PUPUP_MENU_PREFIX, 
//			Configuration.getInstance(Configuration.CONF), 
			Configuration.getCombinedConfiguration(), 
			table, this);

		TablePopupFactory.createMenu(popup, 
			"QueryWindow." + TablePopupFactory.TABLE_PUPUP_MENU_PREFIX, 
//			Configuration.getInstance(Configuration.CONF), 
			Configuration.getCombinedConfiguration(), 
			table, this);
		
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

		private JLabel _label = new JLabel("Executing SQL at ASE Server", JLabel.CENTER);

		private Connection      _conn = null;
		private JLabel _state_lbl = new JLabel();
		private RSyntaxTextArea _allSql_txt   = new RSyntaxTextArea();
		private RTextScrollPane _allSql_sroll = new RTextScrollPane(_allSql_txt);
		private JButton         _cancel       = new JButton("Cancel");

		public SqlProgressDialog(Window owner, Connection conn, String sql)
		{
			super((Frame)null, "Waiting for server...", true);
			setLayout(new MigLayout());

			_conn = conn;

			_label.setFont(new java.awt.Font(Font.DIALOG, Font.BOLD, 16));

			_cancel.setToolTipText("Send a CANCEL request to the server.");

			_allSql_txt.setText(sql);
			_allSql_txt.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
			_allSql_txt.setHighlightCurrentLine(false);
			//_sql_txt.setLineWrap(true);
			//_sql_sroll.setLineNumbersEnabled(true);

			add(_label,        "push, grow, wrap");
			add(_state_lbl,    "wrap");
			add(_allSql_sroll, "push, grow, wrap");
			add(_cancel,       "center");

			_cancel.addActionListener(this);

			pack();
			setSize( getSize().width + 100, getSize().height + 70);
			setLocationRelativeTo(owner);
		}
		
		public void setCurrentSqlText(String sql)
		{
			if ( ! StringUtil.isNullOrBlank(sql) )
				_allSql_txt.markAll(sql, false, false, false);
		}

		public void setState(String string)
		{
			_state_lbl.setText(string);
		}
		
		/**
		 * Called by SwingWorker on completion<br>
		 * Note: need to register on the SwingWorker using: workerThread.addPropertyChangeListener( "this SqlProgressDialog" );
		 */
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
	}

	public void displayQueryResults(final String sql, final boolean execGui)
	{
		final SqlProgressDialog progress = new SqlProgressDialog(_window, _conn, sql);

		// Execute in a Swing Thread
		SwingWorker<String, Object> doBgThread = new SwingWorker<String, Object>()
		{
			@Override
			protected String doInBackground() throws Exception
			{
				if (execGui)
				{
					_resPanel.removeAll();
					_resPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0 0, wrap"));
					
					JAseMessage noRsMsg = new JAseMessage("No result sets will be displayed in GUI exec mode.");
					_resPanel.add(noRsMsg, "gapy 1, growx, pushx");

					new AsePlanViewer(_conn, sql);
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
		doBgThread.addPropertyChangeListener(progress);
		doBgThread.execute();

		//the dialog will be visible until the SwingWorker is done
		progress.setVisible(true); 

		// We will continue here, when results has been sent by server
		//System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");

		if (execGui)
		{
			_resPanel.removeAll();
			_resPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0 0, wrap"));
			
			JAseMessage noRsMsg = new JAseMessage("No result sets will be displayed in GUI exec mode.");
			_resPanel.add(noRsMsg, "gapy 1, growx, pushx");
		}
	}

	
	private void putSqlWarningMsgs(ResultSet rs, ArrayList<JComponent> resultCompList, String debugStr)
	{
		if (rs == null)
			return;
		try
		{
			putSqlWarningMsgs(rs.getWarnings(), resultCompList, debugStr);
			rs.clearWarnings();
		}
		catch (SQLException e)
		{
		}
	}
	private void putSqlWarningMsgs(Statement stmnt, ArrayList<JComponent> resultCompList, String debugStr)
	{
		if (stmnt == null)
			return;
		try
		{
			putSqlWarningMsgs(stmnt.getWarnings(), resultCompList, debugStr);
			stmnt.clearWarnings();
		}
		catch (SQLException e)
		{
		}
	}

	private void putSqlWarningMsgs(SQLException sqe, ArrayList<JComponent> resultCompList, String debugStr)
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
							"Line " + eedi.getLineNumber() +
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
	private void displayQueryResults(Connection conn, String goSql, SqlProgressDialog progress)
	{
		// If we've called close(), then we can't call this method
		if (conn == null)
			throw new IllegalStateException("Connection already closed.");

		// It may take a while to get the results, so give the user some
		// immediate feedback that their query was accepted.
		_msgline.setText("Sending SQL to ASE...");

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
			int batchCount = AseSqlScript.countSqlGoBatches(goSql);
			int cmdCount = 0;
			BufferedReader br = new BufferedReader( new StringReader(goSql) );
			for(String s1=AseSqlScript.readCommand(br); s1!=null; s1=AseSqlScript.readCommand(br))
			{
				sql = s1;

				cmdCount++;
				progress.setState("Sending SQL to ASE for statement "+cmdCount);

				// This can't be part of the for loop, then it just stops if empty row
				if ( StringUtil.isNullOrBlank(sql) )
					continue;

				if (batchCount > 1)
					progress.setCurrentSqlText(sql);
			
				_logger.debug("Executing SQL statement: "+sql);
				// Execute
				boolean hasRs = stmnt.execute(sql);
	
				progress.setState("Waiting for ASE to deliver resultset.");
				_msgline.setText("Waiting for ASE to deliver resultset.");
	
				// iterate through each result set
				int rsCount = 0;
				do
				{
					// Append, messages and Warnings to _resultCompList, if any
					putSqlWarningMsgs(stmnt, _resultCompList, "-before-hasRs-");
	
					if(hasRs)
					{
						rsCount++;
						_msgline.setText("Reading resultset "+rsCount+".");
	
						// Get next resultset to work with
						rs = stmnt.getResultSet();
	
						// Append, messages and Warnings to _resultCompList, if any
						putSqlWarningMsgs(stmnt, _resultCompList, "-after-getResultSet()-Statement-");
						putSqlWarningMsgs(rs,    _resultCompList, "-after-getResultSet()-ResultSet-");
	
						// Convert the ResultSet into a TableModel, which fits on a JTable
						ResultSetTableModel tm = new ResultSetTableModel(rs, true);
						for (SQLWarning sqlw : tm.getSQLWarningList())
							putSqlWarningMsgs(sqlw, _resultCompList, "-after-ResultSetTableModel()-tm.getSQLWarningList()-");
	
						// Create the JTable, using the just created TableModel/ResultSet
						JXTable tab = new JXTable(tm);
						tab.setSortable(true);
						tab.setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);
						tab.packAll(); // set size so that all content in all cells are visible
						tab.setColumnControlVisible(true);
						tab.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
						tab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
//						SwingUtils.calcColumnWidths(tab);
	
						// Add a popup menu
						tab.setComponentPopupMenu( createDataTablePopupMenu(tab) );
	
//						for(int i=0; i<tm.getColumnCount(); i++)
//						{
//							Object o = tm.getValueAt(0, i);
//							if (o!=null)
//								System.out.println("Col="+i+", Class="+o.getClass()+", Comparable="+((o instanceof Comparable)?"true":"false"));
//							else
//								System.out.println("Col="+i+", ---NULL--");
//						}
						// Add the JTable to a list for later use
						_resultCompList.add(tab);
	
						// Append, messages and Warnings to _resultCompList, if any
						putSqlWarningMsgs(stmnt, _resultCompList, "-before-rs.close()-");
	
						// Close it
						rs.close();
					}
	
					// Treat update/row count(s)
					rowsAffected = stmnt.getUpdateCount();
					if (rowsAffected >= 0)
					{
//						rso.add(rowsAffected);
					}
	
					// Check if we have more resultsets
					// If any SQLWarnings has not been found above, it will throw one here
					// so catch raiserrors or other stuff that is not SQLWarnings.
					hasRs = stmnt.getMoreResults();
	
					_logger.trace( "--hasRs="+hasRs+", rowsAffected="+rowsAffected );
				}
				while (hasRs || rowsAffected != -1);
	
				// Append, messages and Warnings to _resultCompList, if any
				putSqlWarningMsgs(stmnt, _resultCompList, "-before-stmnt.close()-");
			}
			br.close();

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

			int numOfTables = countTables(_resultCompList);
			if (numOfTables == 1)
			{
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
				_msgline.setText(" "+rowCount+" rows, and "+msgCount+" messages.");
			}
			else if (numOfTables > 1)
			{
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
					_msgline.setText(" "+numOfTables+" ResultSet with totally "+rowCount+" rows, and "+msgCount+" messages.");
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
					_msgline.setText(" "+numOfTables+" ResultSet with totally "+rowCount+" rows, and "+msgCount+" messages.");
				}
			}
			else
			{
				_logger.trace("NO RS: "+_resultCompList.size());
				int msgCount = 0;

				StringBuilder sb = new StringBuilder();
				sb.append("<html>");
				sb.append("<head>");
				sb.append("<style type=\"text/css\">");
				sb.append("<!-- body {font-family: Courier New; margin: 0px} -->");
				sb.append("<!-- pre  {font-family: Courier New; margin: 0px} -->");
				sb.append("</style>");
				sb.append("</head>");
				sb.append("<body>");
				sb.append("<pre>");
				// There might be "just" print statements... 
				for (JComponent jcomp: _resultCompList)
				{
					if (jcomp instanceof JAseMessage)
					{
						JAseMessage msg = (JAseMessage) jcomp;
//						msg.setFont( _aseMsgFont );
//						_logger.trace("NO-RS: JAseMessage: "+msg.getText());
//						_resPanel.add(msg, "");
//						sb.append("<P>").append(msg.getText()).append("</P>\n");
//						sb.append(msg.getText()).append("<BR>\n");
						sb.append(msg.getText()).append("\n");

						msgCount++;
					}
				}
				sb.append("</pre>");
				sb.append("</body>");
				sb.append("</html>");

//				JTextPane text = new JTextPane();
				JEditorPane textPane = new JEditorPane("text/html", sb.toString());
				textPane.setEditable(false);
				textPane.setOpaque(false);
//				if (_aseMsgFont == null)
//					_aseMsgFont = new Font("Courier", Font.PLAIN, 12);
//				textPane.setFont(_aseMsgFont);
				
				_resPanel.add(textPane, "");

				_msgline.setText("NO ResultSet, but "+msgCount+" messages.");
			}
			
			// We're done, so clear the feedback message
			//_msgline.setText(" ");
		}
		catch (SQLException ex)
		{
			// If something goes wrong, clear the message line
			_msgline.setText("Error: "+ex.getMessage());
			ex.printStackTrace();

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

	public SQLException messageHandler(SQLException sqe)
	{
		// Pass Warning on...
		if (sqe instanceof SQLWarning)
			return sqe;

		// Discard SQLExceptions... but first send them to the _resultCompList
		// This is a bit ugly...
		putSqlWarningMsgs(sqe, _resultCompList, "-from-messageHandler()-");
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
			_init();
		}

		private void _init()
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
	 * Create a simple provider that adds some SQL completions.
	 *
	 * @return The completion provider.
	 */
	private CompletionProvider createCompletionProvider()
	{
		// A DefaultCompletionProvider is the simplest concrete implementation
		// of CompletionProvider. This provider has no understanding of
		// language semantics. It simply checks the text entered up to the
		// caret position for a match against known completions. This is all
		// that is needed in the majority of cases.
		JdbcCompleationProvider         jdbcProvider = new JdbcCompleationProvider(_window, this);

		// Add completions for all SQL keywords. A BasicCompletion is just a straightforward word completion.
		jdbcProvider.addCompletion(new BasicCompletion(jdbcProvider, "SELECT * FROM "));
		jdbcProvider.addCompletion(new BasicCompletion(jdbcProvider, "SELECT row_count(db_id()), object_id('') "));
		jdbcProvider.addCompletion(new BasicCompletion(jdbcProvider, "CASE WHEN x=1 THEN 'x=1' WHEN x=2 THEN 'x=2' ELSE 'not' END"));
		jdbcProvider.addCompletion(new BasicCompletion(jdbcProvider, "SELECT * FROM master..monTables ORDER BY TableName"));
		jdbcProvider.addCompletion(new BasicCompletion(jdbcProvider, "SELECT * FROM master..monTableColumns WHERE TableName = 'monXXX' ORDER BY ColumnID"));

		// Add a couple of "shorthand" completions. These completions don't
		// require the input text to be the same thing as the replacement text.
		jdbcProvider.addCompletion(new ShorthandCompletion(jdbcProvider, "sp_cacheconfig",     "exec sp_cacheconfig 'default data cache', '#G'",                                                  "Cache Size"));
		jdbcProvider.addCompletion(new ShorthandCompletion(jdbcProvider, "sp_cacheconfig",     "exec sp_cacheconfig 'default data cache', 'cache_partitions=#'",                                  "Cache Partitions"));
		jdbcProvider.addCompletion(new ShorthandCompletion(jdbcProvider, "sp_bindcache",       "exec sp_bindcache 'cache name', 'dbname' -- [,tab_name [,index_name]]",                           "Bind db/object to cache"));
		jdbcProvider.addCompletion(new ShorthandCompletion(jdbcProvider, "sp_unbindcache_all", "exec sp_unbindcache_all 'cache name'",                                                            "Unbind all from cache"));
		jdbcProvider.addCompletion(new ShorthandCompletion(jdbcProvider, "sp_poolconfig",      "exec sp_poolconfig 'default data cache', 'sizeM|G', 'toPool_K' --[,'fromPool_K']",                "Pool Size"));
		jdbcProvider.addCompletion(new ShorthandCompletion(jdbcProvider, "sp_poolconfig",      "exec sp_poolconfig 'default data cache', 'affected_poolK', 'wash=size[P|K|M|G]'",                 "Pool Wash Size"));
		jdbcProvider.addCompletion(new ShorthandCompletion(jdbcProvider, "sp_poolconfig",      "exec sp_poolconfig 'default data cache', 'affected_poolK', 'local async prefetch limit=percent'", "Pool Local Async Prefetch Limit"));
		jdbcProvider.addCompletion(new ShorthandCompletion(jdbcProvider, "sp_configure",       "exec sp_configure 'memory'",                                                                      "Memory left for reconfigure"));
		jdbcProvider.addCompletion(new ShorthandCompletion(jdbcProvider, "sp_configure",       "exec sp_configure 'Monitoring'",                                                                  "Check Monitor configuration"));
		jdbcProvider.addCompletion(new ShorthandCompletion(jdbcProvider, "sp_configure",       "exec sp_configure 'nondefault'",                                                                  "Get changed configuration parameters"));
		jdbcProvider.addCompletion(new ShorthandCompletion(jdbcProvider, "sp_helptext",        "exec sp_helptext 'tabName', NULL/*startRow*/, NULL/*numOfRows*/, 'showsql,linenumbers'",          "Get procedure text, with line numbers"));
		jdbcProvider.addCompletion(new ShorthandCompletion(jdbcProvider, "sp_helptext",        "exec sp_helptext 'tabName', NULL, NULL, 'showsql,ddlgen'",                                        "Get procedure text, as DDL"));

		jdbcProvider.addCompletion(new ShorthandCompletion(jdbcProvider, "sp_password",        "sp_password caller_password, new_password [,login_name]",                                         "Change password"));

		jdbcProvider.addCompletion(new ShorthandCompletion(jdbcProvider, "sp_cacheconfig",
				"/*  \n" +
				"** Below is commands/instructions to setup a log cache on a 2K server \n" +
				"** If you have another server page size, values needs to be changed \n" +
				"** select @@maxpagesize/1024 to get the servers page size \n" +
				"*/ \n" +
				"-- Create a cache that holds transaction log(s) \n" +
				"exec sp_cacheconfig 'log_cache', '500M', 'logonly', 'relaxed', 'cache_partition=1' \n" +
				" \n" +
				"-- Most of the memory should be in the 4K pool (2 pages per IO) \n" +
				"sp_poolconfig 'log_cache', '495M', '4K', '2K' -- size the 4K pool to #MB, grab memory from the 2K pool \n" +
				" \n" +
				"-- and maybe some in the 16K (8 pages) pool \n" +
				"-- sp_poolconfig 'log_cache', '#M', '16K', '2K'  \n" +
				" \n" +
				"-- To bind a database transaction log to a Named Cache, it has to be in single user mode \n" +
				"exec sp_dboption  'dbname', 'single', 'true' \n" +
				"exec sp_bindcache 'log_cache', 'dbname', 'syslogs' \n" +
				"exec sp_dboption  'dbname', 'single', 'false' \n" +
				" \n" +
				"-- Change the LOG IO SIZE (default is 4K or: 2 pages per IO) \n" +
				"--dbname..sp_logiosize '8' -- to use the 8K memory pool (4 pages per IO) \n" +
				"",
				"Create a 'log cache' and bind database(s) to it."));
		
		// monTables
		jdbcProvider.addCompletion(new ShorthandCompletion(jdbcProvider, 
				"monTables",  
				"select TableID, TableName, Columns, Description from monTables where TableName like 'mon%'", 
				"Get monitor tables in this system."));
		// monColumns
		jdbcProvider.addCompletion(new ShorthandCompletion(jdbcProvider, 
				"monColumns", 
				"select TableName, ColumnName, TypeName, Length, Description from monTableColumns where TableName like 'mon%'", 
				"Get monitor tables and columns in this system."));
		
		_jdbcTableCompleationProvider = jdbcProvider;
		return jdbcProvider;
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

			String miText = "<html>set <b>"+opt.getText()+"</b> - <i>"+opt.getTooltip()+"</i></html>";
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
				true, QueryWindow.WindowType.JFRAME);
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

class NormalExitException
extends Exception
{
	private static final long serialVersionUID = 1L;
	public NormalExitException()
	{
		super();
	}
	public NormalExitException(String msg)
	{
		super(msg);
	}
}
