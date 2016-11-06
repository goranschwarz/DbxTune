package com.asetune.tools.sqlw;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jdesktop.swingx.JXTable;

import com.asetune.DebugOptions;
import com.asetune.Version;
import com.asetune.gui.ConnectionDialog;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.gui.swing.WaitForExecDialog.BgExecutor;
import com.asetune.sql.conn.TdsConnection;
import com.asetune.tools.NormalExitException;
import com.asetune.tools.WindowType;
import com.asetune.tools.sqlw.RsLastcommit.OriginNotFoundException;
import com.asetune.ui.rsyntaxtextarea.AsetuneSyntaxConstants;
import com.asetune.ui.rsyntaxtextarea.AsetuneTokenMaker;
import com.asetune.ui.rsyntaxtextarea.RSyntaxTextAreaX;
import com.asetune.ui.rsyntaxtextarea.RSyntaxUtilitiesX;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;
import com.asetune.utils.Debug;
import com.asetune.utils.Encrypter;
import com.asetune.utils.JavaVersion;
import com.asetune.utils.Logging;
import com.asetune.utils.RepServerUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;
import com.sybase.jdbcx.SybConnection;
import com.sybase.jdbcx.SybMessageHandler;

import net.miginfocom.swing.MigLayout;

public class RsDumpQueueDialog
implements ActionListener
//implements ActionListener, SybMessageHandler, ConnectionProvider
{
	private static Logger _logger = Logger.getLogger(RsDumpQueueDialog.class);
//	private static final long serialVersionUID = 1L;

	//-------------------------------------------------
	// Actions
	public static final String ACTION_CONNECT              = "CONNECT";
	public static final String ACTION_DISCONNECT           = "DISCONNECT";
	public static final String ACTION_EXIT                 = "EXIT";

	public static final String ACTION_SHOW                 = "SHOW";
	public static final String ACTION_CLOSE                = "CLOSE";
//	public static final String ACTION_PURGE_FIRST          = "PURGE_FIRST";

	private Connection      _conn                          = null;
	private StatusBar       _statusBar                     = new StatusBar();
	private JPanel          _topPanel                      = null;
	private JPanel          _dataPanel                     = null;
	private JButton         _show_but                      = new JButton("Show");
	private JButton         _close_but                     = new JButton("Close");
//	private JButton         _purgeFirst_but                = new JButton("Purge First");

	private RsDatabases     _rsDatabases                   = null;
	
	// Output Format
//	public static final String OUT_TEXT_SHORT              = "Plain Text (short)";
//	public static final String OUT_TEXT_LONG               = "Plain Text (long)";
//	public static final String OUT_JTABLE                  = "Table Format";
	
	private JLabel            _outputFormat_lbl              = new JLabel("Show Output As");
//	private final String[]    _outputFormat_arr              = {OUT_TEXT_SHORT, OUT_TEXT_LONG, OUT_JTABLE};
//	private JComboBox         _outputFormat_cbx              = new JComboBox(_outputFormat_arr);
	private JCheckBox         _outputFile_chk                = new JCheckBox("Save to File", DEFAULT_outputFile_chk);
	private JTextField        _outputFile_txt                = new JTextField(DEFAULT_outputFile_txt);
	private JRadioButton      _outputFormatTxtShort_rbt      = new JRadioButton("Plain Text (short)");
	private JRadioButton      _outputFormatTxtLong_rbt       = new JRadioButton("Plain Text (long)");
	private JRadioButton      _outputFormatTab_rbt           = new JRadioButton("Table Format");
                              
	private String            _selectedDestSrvName   = null;
	private String            _selectedDestDbName    = null;
	private String            _selectedDestRouteName = null;
	// Queue name/type        
	private JLabel            _queueName1_lbl                = new JLabel("Connection Type");
	private JLabel            _queueName2_lbl                = new JLabel("Connection Name");
	private JRadioButton      _queueNameLconn_rbt            = new JRadioButton("Warm Standby Connections");
	private JRadioButton      _queueNamePconn_rbt            = new JRadioButton("Regular DB Connections");
	private JRadioButton      _queueNameRoute_rbt            = new JRadioButton("Routes");
//	public static final String QUEUE_TYPE_OUT              = "Outbound Queue (0)";
//	public static final String QUEUE_TYPE_IN               = "Inbound Queue (1)";
//	private JLabel            _queueNameLconn_lbl            = new JLabel("Warm Standby Connections");
	private JComboBox         _queueNameLconn_cbx            = new JComboBox(); // Logical Server.DB name
//	private JLabel            _queueNamePconn_lbl            = new JLabel("Physical Connections");
	private JComboBox         _queueNamePconn_cbx            = new JComboBox(); // Physical Server.DB name
//	private JLabel            _queueNameRoute_lbl            = new JLabel("Routes");
	private JComboBox         _queueNameRoute_cbx            = new JComboBox(); // Physical Server.DB name
	private JLabel            _queueType_lbl                 = new JLabel("Queue Type");
//	private final String[]    _queueType_arr                 = {QUEUE_TYPE_OUT, QUEUE_TYPE_IN};
//	private JComboBox         _queueType_cbx                 = new JComboBox(_queueType_arr);
	private JRadioButton      _queueTypeIn_rbt               = new JRadioButton("Inbound Queue (1)");
	private JRadioButton      _queueTypeOut_rbt              = new JRadioButton("Outbound Queue (0)");
//	private JLabel            _wsConnDesc_lbl                = new JLabel();

	// Filter Options
	private JCheckBox         _doSimpleDumpQueue             = new JCheckBox("Simple Dump Queue", DEFAULT_filterOutAppliedTrans);
	private JCheckBox         _filterOutAppliedTrans_chk     = new JCheckBox("<html>Do <b>not</b> show already replicated records</html>", DEFAULT_filterOutAppliedTrans);
//	private JCheckBox         _filterOutAppliedTrans_chk     = new JCheckBox("Filter out already applied transactions", true);
	private JCheckBox         _filterDebugAppliedTrans_chk   = new JCheckBox("<html>Include discarded trans as comment", false);
	private JLabel            _maintUsername_lbl             = new JLabel("Maintenance User");
	private JTextField        _maintUsername_txt             = new JTextField(DEFAULT_maintUsername);
	private JLabel            _maintPassword_lbl             = new JLabel("Maintenance Password");
	private JTextField        _maintPassword_txt             = new JTextField(DEFAULT_maintPassword);
                              
	private JLabel            _filterSeg_lbl                 = new JLabel("Segment");
	private SpinnerModel      _filterSeg_spm                 = new SpinnerNumberModel(0, 0, 999999, 1);
	private JSpinner          _filterSeg_sp                  = new JSpinner(_filterSeg_spm);
	private JCheckBox         _filterSegStartFirstActive_chk = new JCheckBox("Start at first active segment", true);
	private JCheckBox         _filterSegShowDeletedData_chk  = new JCheckBox("Show deleted data", false);
                              
	private JLabel            _filterStartBlk_lbl            = new JLabel("Starting Block");
	private SpinnerModel      _filterStartBlk_spm            = new SpinnerNumberModel(0, 0, 999999, 1);
	private JSpinner          _filterStartBlk_sp             = new JSpinner(_filterStartBlk_spm);
                              
	private JLabel            _filterBlkCount_lbl            = new JLabel("Block Count");
	private SpinnerModel      _filterBlkCount_spm            = new SpinnerNumberModel(0, 0, 999999, 1);
	private JSpinner          _filterBlkCount_sp             = new JSpinner(_filterBlkCount_spm);
	private JCheckBox         _filterBlkCountToEndSeg_chk    = new JCheckBox("View to end of segment", true);
	private JCheckBox         _filterBlkCountToEndQueue_chk  = new JCheckBox("View to end of queue",   false);
                              
	private JLabel            _filterRows_lbl                = new JLabel("Rows");
	private SpinnerModel      _filterRows_spm                = new SpinnerNumberModel(0, 0, 999999, 1);
	private JSpinner          _filterRows_sp                 = new JSpinner(_filterRows_spm);
	private JCheckBox         _filterRowsShowAll_chk         = new JCheckBox("All rows", true);
                              
	private JCheckBox         _rclCmd_chk                    = new JCheckBox("Use RCS Command", DEFAULT_rclCmd_chk);
	private JTextField        _rclCmd_txt                    = new JTextField(DEFAULT_rclCmd_txt); // show the RCL which will be executed
//	private String dummyRcl   = "sysadmin dump_queue, 'GORAN_DS', 'wsdb3', 1, -1, -2, -1, L0, client";
//	private JCheckBox         _rclCmd_chk                    = new JCheckBox("Use RCS Command", true);
//	private JTextField        _rclCmd_txt                    = new JTextField(dummyRcl); // show the RCL which will be executed

	// Queue Content
	private JXTable           _dumpQueueTab          = new JXTable();
	private JScrollPane       _dumpQueueTabScroll    = new JScrollPane(_dumpQueueTab);
	private RSyntaxTextAreaX  _dumpQueueTxt          = new RSyntaxTextAreaX();
	private RTextScrollPane   _dumpQueueTxtScroll    = new RTextScrollPane(_dumpQueueTxt);

	
	private int               _srvVersion                = 0;
	private String            _connectedToProductName    = null;
	private String            _connectedToProductVersion = null;
	private String            _connectedToServerName     = null;
	private String            _connectedAsUser           = null;
	private String            _connectedWithUrl          = null;

	// The base Window can be either a JFrame or a JDialog
	private Window            _window          = null;
	private JFrame            _jframe          = null;
	private JDialog           _jdialog         = null;
	private String            _titlePrefix     = null;
                              
	private JButton           _connect_but     = SwingUtils.makeToolbarButton(Version.class, "images/connect_16.png",    ACTION_CONNECT,    this, "Connect to a ASE",         "Connect");
	private JButton           _disconnect_but  = SwingUtils.makeToolbarButton(Version.class, "images/disconnect_16.png", ACTION_DISCONNECT, this, "Close the ASE Connection", "Disconnect");

	private JMenuBar          _main_mb                = new JMenuBar();
	private JToolBar          _toolbar                = new JToolBar();

	// File
	private JMenu             _file_m                 = new JMenu("File");
	private JMenuItem         _connect_mi             = new JMenuItem("Connect...");
	private JMenuItem         _disconnect_mi          = new JMenuItem("Disconnect");
	private JMenuItem         _fNew_mi                = new JMenuItem("New File");
	private JMenuItem         _fOpen_mi               = new JMenuItem("Open File...");
//	private JMenuItem         _fClose_mi              = new JMenuItem("Close");
	private JMenuItem         _fSave_mi               = new JMenuItem("Save");
	private JMenuItem         _fSaveAs_mi             = new JMenuItem("Save As...");
	private JMenu             _fHistory_m             = new JMenu("Last Used Files");
	private JMenuItem         _exit_mi                = new JMenuItem("Exit");

	// Tools
	private JMenu             _view_m                 = new JMenu("View");
	private JMenuItem         _ase_viewConfig_mi      = new JMenuItem("View ASE Configuration...");
	private JMenuItem         _rs_configChangedDdl_mi = new JMenuItem("View RCL for changed configurations...");
	private JMenuItem         _rs_configAllDdl_mi     = new JMenuItem("View RCL for ALL configurations...");
	//---------------------------------------

	/**
	 * Constructor for CommandLine parameters
	 * @param cmd
	 * @throws Exception
	 */
	public RsDumpQueueDialog(CommandLine cmd)
	throws Exception
	{
		Version.setAppName("Dump Queue");
		
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
		final String TMP_CONFIG_FILE_NAME  = System.getProperty("TMP_CONFIG_FILE_NAME",  "rsDumpQueue.save.properties");
		final String RS_DUMP_QUEUE_HOME    = System.getProperty("RS_DUMP_QUEUE_HOME");
		
		String defaultPropsFile     = (RS_DUMP_QUEUE_HOME    != null) ? RS_DUMP_QUEUE_HOME    + File.separator + CONFIG_FILE_NAME      : CONFIG_FILE_NAME;
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
		    && javaVersionInt <  JavaVersion.VERSION_1_7
		   )
		{
			System.out.println("");
			System.out.println("===============================================================");
			System.out.println(" "+Version.getAppName()+" needs a runtime JVM 1.7 or higher.");
			System.out.println(" java.version = " + System.getProperty("java.version"));
			System.out.println(" which is parsed into the number: " + JavaVersion.getVersion());
			System.out.println("---------------------------------------------------------------");
			System.out.println("");
			throw new Exception(Version.getAppName()+" needs a runtime JVM 1.7 or higher.");
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
		String rsSrvDbStr  = "";
//		String sqlQuery    = "";
//		String sqlFile     = "";
		if (cmd.hasOption('U'))	aseUsername = cmd.getOptionValue('U');
		if (cmd.hasOption('P'))	asePassword = cmd.getOptionValue('P');
		if (cmd.hasOption('S'))	aseServer   = cmd.getOptionValue('S');
//		if (cmd.hasOption('D'))	aseDbname   = cmd.getOptionValue('D');
		if (cmd.hasOption('D'))	rsSrvDbStr  = cmd.getOptionValue('D');
//		if (cmd.hasOption('q'))	sqlQuery    = cmd.getOptionValue('q');
//		if (cmd.hasOption('i'))	sqlFile     = cmd.getOptionValue('i');

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
		Logging.init("rsDumpQueue.", propFile);
		
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

		Connection conn = null;
		if ( ! StringUtil.isNullOrBlank(asePassword) )
		{
			_logger.info("Connecting as user '"+aseUsername+"' to server='"+aseServer+"'. Which is located on '"+hostPortStr+"'.");
			try
			{
				Properties props = new Properties();
	//			props.put("CHARSET", "iso_1");
				conn = AseConnectionFactory.getConnection(hostPortStr, aseDbname, aseUsername, asePassword, "RS Dump Queue", null, props, null);
	
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
		RsDumpQueueDialog dqd = new RsDumpQueueDialog(conn, rsSrvDbStr, true, WindowType.CMDLINE_JFRAME, null);
//		dqd.openTheWindow();
		dqd.setVisible(true);
	}

	/**
	 * This constructor method creates a simple GUI and hooks up an event
	 * listener that updates the table when the user enters a new query.
	 **/
	public RsDumpQueueDialog(Connection conn, WindowType winType)
	{
		this(conn, null, true, winType, null);
	}
	public RsDumpQueueDialog(Connection conn, boolean closeConnOnExit, WindowType winType)
	{
		this(conn, null, closeConnOnExit, winType, null);
	}
	public RsDumpQueueDialog(Connection conn, String sql, WindowType winType)
	{
		this(conn, null, true, winType, null);
	}
	public RsDumpQueueDialog(Connection conn, String rsSrvDbStr, boolean closeConnOnExit, WindowType winType, Configuration conf)
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
			_main_mb.add(_view_m);

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
	
			// TOOLS
			_view_m.add(_ase_viewConfig_mi);
			_view_m.add(_rs_configChangedDdl_mi);   
			_view_m.add(_rs_configAllDdl_mi);   

			_ase_viewConfig_mi     .setVisible(false);
			_rs_configChangedDdl_mi.setVisible(false);
			_rs_configAllDdl_mi    .setVisible(false);
	
			_file_m .setMnemonic(KeyEvent.VK_T);

			//			_connect_mi        .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK));
//			_disconnect_mi     .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.ALT_MASK));
			_connect_mi        .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));
			_disconnect_mi     .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));

			_fNew_mi           .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			_fOpen_mi          .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			_fSave_mi          .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			_fSaveAs_mi        .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));

			// TOOLBAR
//			_connect_but    = SwingUtils.makeToolbarButton(Version.class, "images/connect_16.png",    ACTION_CONNECT,    this, "Connect to a ASE",         "Connect");
//			_disConnect_but = SwingUtils.makeToolbarButton(Version.class, "images/disconnect_16.png", ACTION_DISCONNECT, this, "Close the ASE Connection", "Disconnect");

			_toolbar.setLayout(new MigLayout("insets 0 0 0 3", "", "")); // insets Top Left Bottom Right
			_toolbar.add(_connect_but);
			_toolbar.add(_disconnect_but);

			//--------------------------
			// MENU - Icons
			_connect_mi   .setIcon(SwingUtils.readImageIcon(Version.class, "images/connect_16.png"));
			_disconnect_mi.setIcon(SwingUtils.readImageIcon(Version.class, "images/disconnect_16.png"));
			_exit_mi      .setIcon(SwingUtils.readImageIcon(Version.class, "images/close.gif"));

			//--------------------------
			// MENU - Actions
			_connect_mi   .setActionCommand(ACTION_CONNECT);
			_disconnect_mi.setActionCommand(ACTION_DISCONNECT);
			_exit_mi      .setActionCommand(ACTION_EXIT);

			//--------------------------
			// And the action listener
			_connect_mi     .addActionListener(this);
			_disconnect_mi  .addActionListener(this);
			_exit_mi        .addActionListener(this);
		}

//		ImageIcon icon16 = SwingUtils.readImageIcon(Version.class, "images/rs_dump_queue.png");
//		ImageIcon icon32 = SwingUtils.readImageIcon(Version.class, "images/rs_dump_queue_32.png");
		ImageIcon icon16 = SwingUtils.readImageIcon(Version.class, "images/view_rs_queue.png");
		ImageIcon icon32 = SwingUtils.readImageIcon(Version.class, "images/view_rs_queue_32.png");
		if (icon16 != null || icon32 != null)
		{
			ArrayList<Image> iconList = new ArrayList<Image>();
			if (icon16 != null) iconList.add(icon16.getImage());
			if (icon32 != null) iconList.add(icon32.getImage());

			_window.setIconImages(iconList);
		}
//		if (icon != null)
//			_window.setIconImage(icon.getImage());

		// Arrange to quit the program when the user closes the window
		_window.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				saveWinProps();
			}
		});

		if (AseConnectionUtils.isConnectionOk(conn, false, null))
		{
			// Remember the factory object that was passed to us
			_conn = conn;

			if (_conn instanceof SybConnection || _conn instanceof TdsConnection)
			{
				// Setup a message handler
		//		((SybConnection)_conn).setSybMessageHandler(this);
//				_srvVersion = AseConnectionUtils.getAseVersionNumber(conn);
				_srvVersion = AseConnectionUtils.getRsVersionNumber(_conn);
			}
			refreshConnectionNames();
		}

		// Place the components within this window
		Container contentPane = _jframe != null ? _jframe.getContentPane() : _jdialog.getContentPane();
		contentPane.setLayout(new BorderLayout());

		if (winType == WindowType.CMDLINE_JFRAME)
			contentPane.add(_toolbar, BorderLayout.NORTH);

		_topPanel = createTopPanel();
		_dataPanel = createBottomPanel();
		contentPane.add(_topPanel,  BorderLayout.NORTH);
		contentPane.add(_dataPanel, BorderLayout.CENTER);
		contentPane.add(_statusBar, BorderLayout.SOUTH);

		_window.pack();

//		bottom.add(_dbs_cobx,       "split 6, hidemode 2");
//		bottom.add(_exec,           "");
//		bottom.add(_asPlainText,    "");
////		bottom.add(_showplan,       "");
//		bottom.add(_resPanelScroll, "span 4, width 100%, height 100%");
////		bottom.add(_msgline, "dock south");
//		bottom.add(_statusBar, "dock south");
//
//		_resPanelScroll.getVerticalScrollBar()  .setUnitIncrement(16);
//		_resPanelScroll.getHorizontalScrollBar().setUnitIncrement(16);

		// Refresh the database list (if ASE)
//		if (_conn != null)
//			setDbNames();

		loadProps();

		// This will initiate various things.
		actionPerformed(null);

		// Set components if visible, enabled etc...
		setComponentVisibility();
	}

	private JPanel createTopPanel()
	{
		JPanel panel = SwingUtils.createPanel("Top", false);
		panel.setLayout(new MigLayout());
//		panel.setLayout(new MigLayout("insets 0 0 0 0", "[][grow]", ""));

		panel.add(createQueueSelectionPanel(),      "push, grow");
		panel.add(createActionPanel(),              "wrap");
		panel.add(createQueueFilterPanel(),         "push, grow, wrap");
		panel.add(createOutputFormatPanel(),        "push, grow, wrap");

		return panel;
	}

	private JPanel createActionPanel()
	{
		JPanel panel = SwingUtils.createPanel("Actions", false);
		panel.setLayout(new MigLayout());
//		panel.setLayout(new MigLayout("insets 0 0 0 0", "[][grow]", ""));

		// Tooltip
		_show_but .setToolTipText("Execute the RCL Command and show the output");
		_close_but.setToolTipText("Close this dialog");
		
		// ADD Components
		panel.add(_show_but,  "wrap");
		panel.add(_close_but, "wrap");

		// Action Commands
		_show_but .setActionCommand(ACTION_SHOW);
		_close_but.setActionCommand(ACTION_CLOSE);

		// Add action listener
		_show_but .addActionListener(this);
		_close_but.addActionListener(this);

		return panel;
	}

	private JPanel createQueueSelectionPanel()
	{
		JPanel panel = SwingUtils.createPanel("Queue Selection", true);
		panel.setLayout(new MigLayout());
//		panel.setLayout(new MigLayout("insets 0 0 0 0", "[][grow]", ""));

		// Tooltip
		_queueName1_lbl    .setToolTipText("What Connection or Queue Type do you want to dump");
		_queueNameLconn_rbt.setToolTipText("Warm Standby Connections");
		_queueNamePconn_rbt.setToolTipText("Connections not connected to a Logical Connection");
		_queueNameRoute_rbt.setToolTipText("Routes to other Replication Servers");

		_queueName2_lbl     .setToolTipText("Name of the Connection or Queue you want to view");
//		_queueNameLconn_lbl   .setToolTipText("Warm Standby Connection name, or Logical Connection Name");
		_queueNameLconn_cbx   .setToolTipText("Warm Standby Connection name, or Logical Connection Name");
//		_queueNamePconn_lbl   .setToolTipText("Physical Connection name (Warm Standby connection will not be visible here)");
		_queueNamePconn_cbx   .setToolTipText("Physical Connection name (Warm Standby connection will not be visible here)");
//		_queueNameRoute_lbl   .setToolTipText("Routes");
		_queueNameRoute_cbx   .setToolTipText("Routes");

		_queueType_lbl.setToolTipText("Input or Output Queue, for Warm Standby this is irrelevant");
//		_queueType_cbx.setToolTipText("Input or Output Queue, for Warm Standby this is irrelevant");
		_queueTypeIn_rbt .setToolTipText("In-bound Queue");
		_queueTypeOut_rbt.setToolTipText("Out-bound Queue");

		ButtonGroup gt = new ButtonGroup();
		gt.add(_queueTypeIn_rbt);
		gt.add(_queueTypeOut_rbt);
		_queueTypeOut_rbt.setSelected(true);

		gt = new ButtonGroup();
		gt.add(_queueNameLconn_rbt);
		gt.add(_queueNamePconn_rbt);
		gt.add(_queueNameRoute_rbt);
		_queueNameLconn_rbt.setSelected(true);

		// ADD Components
		panel.add(_queueName1_lbl,     "");
		panel.add(_queueNameLconn_rbt, "split");
		panel.add(_queueNamePconn_rbt, "");
		panel.add(_queueNameRoute_rbt, "wrap");

//		panel.add(_queueNameLconn_lbl,    "hidemode 3");
//		panel.add(_queueNameLconn_cbx,    "growx, pushx, hidemode 3");
//
//		panel.add(_queueNamePconn_lbl,    "hidemode 3");
//		panel.add(_queueNamePconn_cbx,    "growx, pushx, hidemode 3");
//
//		panel.add(_queueNameRoute_lbl,    "hidemode 3");
//		panel.add(_queueNameRoute_cbx,    "growx, pushx, hidemode 3");

		panel.add(_queueName2_lbl,        "");
		panel.add(_queueNameLconn_cbx,    "growx, pushx, hidemode 3, wrap");
		panel.add(_queueNamePconn_cbx,    "growx, pushx, hidemode 3, wrap");
		panel.add(_queueNameRoute_cbx,    "growx, pushx, hidemode 3, wrap");

		panel.add(_queueType_lbl, "");
//		panel.add(_queueType_cbx, "growx, pushx, wrap");
		panel.add(_queueTypeIn_rbt,  "split");
		panel.add(_queueTypeOut_rbt, "wrap");
//		panel.add(_wsConnDesc_lbl,   "wrap");

		// Action Commands
//		_lSrvDb_cbx   .setActionCommand(ACTION_XXX);
//		_pSrvDb_cbx   .setActionCommand(ACTION_XXX);
//		_queueType_cbx.setActionCommand(ACTION_XXX);

		// Add action listener
		_queueNameLconn_cbx.addActionListener(this);
		_queueNamePconn_cbx.addActionListener(this);
		_queueNameRoute_cbx.addActionListener(this);
//		_queueType_cbx.addActionListener(this);
		_queueTypeIn_rbt   .addActionListener(this);
		_queueTypeOut_rbt  .addActionListener(this);

		_queueNameLconn_rbt.addActionListener(this);
		_queueNamePconn_rbt.addActionListener(this);
		_queueNameRoute_rbt.addActionListener(this);

		return panel;
	}
	private JPanel createQueueFilterPanel()
	{
		JPanel panel         = SwingUtils.createPanel("Queue Filter", true);
		JPanel panelBasic    = SwingUtils.createPanel("Basic",        true);
		JPanel panelAdvanced = SwingUtils.createPanel("Advanced",     true);
		panel        .setLayout(new MigLayout());
		panelBasic   .setLayout(new MigLayout());
		panelAdvanced.setLayout(new MigLayout());
//		panel.setLayout(new MigLayout("insets 0 0 0 0", "[][grow]", ""));

		// Tooltip
		_doSimpleDumpQueue            .setToolTipText("");
		_filterOutAppliedTrans_chk    .setToolTipText("");
		_filterDebugAppliedTrans_chk  .setToolTipText("");
		_maintUsername_lbl            .setToolTipText("<html>Used to connect to server to get records from the rs_lastcommit table.<br><br><b>Note</b>: <code>&lt;dbname&gt;</code> is a template which will be replaced with the current selected database name.</html>");
		_maintUsername_txt            .setToolTipText("<html>Used to connect to server to get records from the rs_lastcommit table.<br><br><b>Note</b>: <code>&lt;dbname&gt;</code> is a template which will be replaced with the current selected database name.</html>");
		_maintPassword_lbl            .setToolTipText("<html>Used to connect to server to get records from the rs_lastcommit table.<br><br><b>Note</b>: <code>&lt;dbname&gt;</code> is a template which will be replaced with the current selected database name.</html>");
		_maintPassword_txt            .setToolTipText("<html>Used to connect to server to get records from the rs_lastcommit table.<br><br><b>Note</b>: <code>&lt;dbname&gt;</code> is a template which will be replaced with the current selected database name.</html>");

		_filterSeg_lbl                .setToolTipText("");
		_filterSeg_sp                 .setToolTipText("");
		_filterSegStartFirstActive_chk.setToolTipText("");
		_filterSegShowDeletedData_chk .setToolTipText("");

		_filterStartBlk_lbl           .setToolTipText("");
		_filterStartBlk_sp            .setToolTipText("");

		_filterBlkCount_lbl           .setToolTipText("");
		_filterBlkCount_sp            .setToolTipText("");
		_filterBlkCountToEndSeg_chk   .setToolTipText("");
		_filterBlkCountToEndQueue_chk .setToolTipText("");

		_filterRows_lbl               .setToolTipText("");
		_filterRows_sp                .setToolTipText("");
		_filterRowsShowAll_chk        .setToolTipText("");

		_rclCmd_chk                   .setToolTipText("Check this if you want to specify your own command parameters");
		_rclCmd_txt                   .setToolTipText("This is the RCL command this will be executed.");

//		// ADD Components
//		panel.add(_filterOutAppliedTrans_chk,     "span, split, wrap");
//
//		panel.add(_filterSeg_lbl,                 "");
//		panel.add(_filterSeg_sp,                  "");
//		panel.add(_filterSegStartFirstActive_chk, "split");
//		panel.add(_filterSegShowDeletedData_chk,  "wrap");
//
//		panel.add(_filterStartBlk_lbl,            "");
//		panel.add(_filterStartBlk_sp,             "wrap");
//
//		panel.add(_filterBlkCount_lbl,            "");
//		panel.add(_filterBlkCount_sp,             "");
//		panel.add(_filterBlkCountToEndSeg_chk,    "split");
//		panel.add(_filterBlkCountToEndQueue_chk,  "wrap");
//
//		panel.add(_filterRows_lbl,                "");
//		panel.add(_filterRows_sp,                 "");
//		panel.add(_filterRowsShowAll_chk,         "wrap");
//
//		panel.add(_rclCmd_chk,                    "");
//		panel.add(_rclCmd_txt,                    "split, span, pushx, growx, wrap");

		// ADD Components
		panelBasic.add(_doSimpleDumpQueue,                "wrap");

		panelBasic.add(_filterOutAppliedTrans_chk,        "");
		panelBasic.add(_filterDebugAppliedTrans_chk,      "wrap");
		
		panelBasic.add(_maintUsername_lbl,                "");
		panelBasic.add(_maintUsername_txt,                "growx, pushx, wrap");
		panelBasic.add(_maintPassword_lbl,                "");
		panelBasic.add(_maintPassword_txt,                "growx, pushx, wrap");

		panelAdvanced.add(_filterSeg_lbl,                 "");
		panelAdvanced.add(_filterSeg_sp,                  "");
		panelAdvanced.add(_filterSegStartFirstActive_chk, "split");
		panelAdvanced.add(_filterSegShowDeletedData_chk,  "wrap");

		panelAdvanced.add(_filterStartBlk_lbl,            "");
		panelAdvanced.add(_filterStartBlk_sp,             "wrap");

		panelAdvanced.add(_filterBlkCount_lbl,            "");
		panelAdvanced.add(_filterBlkCount_sp,             "");
		panelAdvanced.add(_filterBlkCountToEndSeg_chk,    "split");
		panelAdvanced.add(_filterBlkCountToEndQueue_chk,  "wrap");

		panelAdvanced.add(_filterRows_lbl,                "");
		panelAdvanced.add(_filterRows_sp,                 "");
		panelAdvanced.add(_filterRowsShowAll_chk,         "wrap");

		panel.add(panelBasic,                    "");
		panel.add(panelAdvanced,                 "wrap");
		panel.add(_rclCmd_chk,                    "split, span");
		panel.add(_rclCmd_txt,                    "pushx, growx, wrap");

		// Special settings
		_rclCmd_txt.setEditable(false);
		_rclCmd_txt.setEnabled(false);
		
		// Action Commands

		// Add action listener
		_doSimpleDumpQueue            .addActionListener(this);
		_filterOutAppliedTrans_chk    .addActionListener(this);
		_filterSegStartFirstActive_chk.addActionListener(this);
		_filterSegShowDeletedData_chk .addActionListener(this);
		_filterBlkCountToEndSeg_chk   .addActionListener(this);
		_filterBlkCountToEndQueue_chk .addActionListener(this);
		_filterRowsShowAll_chk        .addActionListener(this);

		_rclCmd_chk                   .addActionListener(this);

		SwingUtils.setEnabled(panelAdvanced, false);
		return panel;
	}
	private JPanel createOutputFormatPanel()
	{
		JPanel panel = SwingUtils.createPanel("Output Format", true);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));
//		panel.setLayout(new MigLayout("insets 0 0 0 0", "[][grow]", ""));

		// Tooltip
		_outputFormat_lbl.setToolTipText("How should the data be presented");
//		_outputFormat_cbx.setToolTipText("How should the data be presented");
		_outputFile_chk  .setToolTipText("Save the output to a file instead of present it here");
		_outputFile_txt  .setToolTipText("File to save output in");
		_outputFormatTxtShort_rbt.setToolTipText(
			"<html>" +
				"View Stable Queue Content as text, using a \"short\" comment as header.<br>" +
				"<b>Note</b>: Content will be sorted in Commit Order, even for Logical Connections.<br>" +
			"</html>");
		_outputFormatTxtLong_rbt .setToolTipText(
			"<html>" +
				"View Stable Queue Content as text, using a \"longer\" comment as header.<br>" +
				"<b>Note</b>: Content will be sorted in Commit Order, even for Logical Connections.<br>" +
			"</html>");
		_outputFormatTab_rbt     .setToolTipText(
			"<html>" +
				"View Stable Queue Content as a JTable.<br>" +
				"<b>Note</b>: Content will be listed as they were found in the queue.<br>" +
			"</html>");
		
		ButtonGroup bg = new ButtonGroup();
		bg.add(_outputFormatTxtShort_rbt);
		bg.add(_outputFormatTxtLong_rbt);
		bg.add(_outputFormatTab_rbt);
		_outputFormatTxtLong_rbt.setSelected(true);

		// ADD Components
		panel.add(_outputFormat_lbl, "");
//		panel.add(_outputFormat_cbx, "growx, pushx, wrap");
		panel.add(_outputFormatTxtShort_rbt, "split");
		panel.add(_outputFormatTxtLong_rbt,  "");
		panel.add(_outputFormatTab_rbt,      "wrap");

		panel.add(_outputFile_chk,   "");
		panel.add(_outputFile_txt,   "growx, pushx, wrap");

		// Action Commands
//		_outputFormat_cbx .setActionCommand(ACTION_XXX);
//		_outputFile_txt   .setActionCommand(ACTION_XXX);

		// Add action listener
//		_outputFormat_cbx .addActionListener(this);
//		_outputFile_txt   .addActionListener(this);

		return panel;
	}

	private JPanel createBottomPanel()
	{
		JPanel panel = SwingUtils.createPanel("Output", false);
		panel.setLayout(new MigLayout());
//		panel.setLayout(new MigLayout("insets 0 0 0 0", "[][grow]", ""));

		setupDumpQueueTextArea(_dumpQueueTxt, _dumpQueueTxtScroll);
		
		RSyntaxUtilitiesX.installRightClickMenuExtentions(_dumpQueueTxtScroll, _window);

		panel.add(_dumpQueueTxtScroll, "grow, push, hidemode 3");
		panel.add(_dumpQueueTabScroll, "grow, push, hidemode 3");

		// Special settings
		_dumpQueueTxtScroll.setVisible(true);
		_dumpQueueTabScroll.setVisible(true);
		
		return panel;
	}
	
	private void setupDumpQueueTextArea(final RSyntaxTextArea textArea, final RTextScrollPane textScroll)
	{
		// To set all RSyntaxTextAreaX components to use "_"
		RSyntaxUtilitiesX.setCharsAllowedInWords("_");
		// To set all _query components to use "_", this since it's of TextEditorPane, which extends RSyntaxTextArea
		RSyntaxUtilitiesX.setCharsAllowedInWords(textArea, "_");

		textScroll.setLineNumbersEnabled(true);
		
		// Install some extra Syntax Highlighting for RCL and TSQL
		AsetuneTokenMaker.init();  

		// Setup SQL Syntax
		textArea.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL);
//		textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);

		// Add menu items to the OUTPUT RSyntaxTextArea 
		JPopupMenu menu = textArea.getPopupMenu();
		menu.addSeparator();
		
		JCheckBoxMenuItem mi;
		mi = new JCheckBoxMenuItem("Word Wrap", textArea.getLineWrap());
		mi.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				textArea.setLineWrap( ! textArea.getLineWrap() );
			}
		});
		menu.add(mi);

		mi = new JCheckBoxMenuItem("Line Numbers", textScroll.getLineNumbersEnabled());
		mi.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				textScroll.setLineNumbersEnabled( ! textScroll.getLineNumbersEnabled() );
			}
		});
		menu.add(mi);

		mi = new JCheckBoxMenuItem("Current Line Highlight", textScroll.getLineNumbersEnabled());
		mi.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				textArea.setHighlightCurrentLine( ! textArea.getHighlightCurrentLine() );
			}
		});
		menu.add(mi);
	}

	/**
	 * Saves some properties about the window
	 * <p>
	 * NOTE: normally you would load window size via loadWinProps(), but this is done openTheWindow()...
	 */
	public void saveWinProps()
	{
		String prefix = "rsDumpQueue.";
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null)
			return;

		if (_window == null)
			return;

		conf.setLayoutProperty(prefix + "size.width",         _window.getSize().width);
		conf.setLayoutProperty(prefix + "size.height",        _window.getSize().height);

//		conf.setProperty(prefix + "splitPane.location", _splitPane.getDividerLocation());

		if (_window.isVisible())
		{
			conf.setLayoutProperty(prefix + "size.pos.x",  _window.getLocationOnScreen().x);
			conf.setLayoutProperty(prefix + "size.pos.y",  _window.getLocationOnScreen().y);
		}
		
		conf.save();
	}

	public static final String  PROPKEY_simpleDumpQueue       = "RsDumpQueue.simple.queue.dump";
	public static final boolean DEFAULT_simpleDumpQueue       = true;

	public static final String  PROPKEY_filterOutAppliedTrans = "RsDumpQueue.discard.applid.dsi.trans";
	public static final boolean DEFAULT_filterOutAppliedTrans = true;

	public static final String  PROPKEY_maintUsername         = "RsDumpQueue.maint.username";
	public static final String  DEFAULT_maintUsername         = "<dbname>_maint";

	public static final String  PROPKEY_maintPassword         = "RsDumpQueue.maint.password";
	public static final String  DEFAULT_maintPassword         = "<dbname>_maint_ps";

	public static final String  PROPKEY_rclCmd_chk            = "RsDumpQueue.rcl.cmd.chk";
	public static final boolean DEFAULT_rclCmd_chk            = false;

	public static final String  PROPKEY_rclCmd_txt            = "RsDumpQueue.rcl.cmd.txt";
	public static final String  DEFAULT_rclCmd_txt            = "";


	public static final String  PROPKEY_outputFile_chk        = "RsDumpQueue.output.file.chk";
	public static final boolean DEFAULT_outputFile_chk        = false;

	public static final String  PROPKEY_outputFile_txt        = "RsDumpQueue.output.file.txt";
	public static final String  DEFAULT_outputFile_txt        = "";

//	public static final String  PROPKEY_xxx = "";
//	public static final String  DEFAULT_xxx = true;

	public static final String  PROPKEY_outputFormat          = "RsDumpQueue.output.format";
	public static final String  PROPKEY_queueNameType         = "RsDumpQueue.queue.name.type";
	public static final String  PROPKEY_queueNameLconn        = "RsDumpQueue.queue.name.lconn.index";
	public static final String  PROPKEY_queueNamePconn        = "RsDumpQueue.queue.name.pconn.index";
	public static final String  PROPKEY_queueNameRoute        = "RsDumpQueue.queue.name.route.index";
	public static final String  PROPKEY_queueType             = "RsDumpQueue.queue.type";

	public void loadProps()
	{
		Configuration conf = Configuration.getCombinedConfiguration();

		_doSimpleDumpQueue        .setSelected( conf.getBooleanProperty(PROPKEY_simpleDumpQueue,       DEFAULT_simpleDumpQueue) );
		_filterOutAppliedTrans_chk.setSelected( conf.getBooleanProperty(PROPKEY_filterOutAppliedTrans, DEFAULT_filterOutAppliedTrans) );
		_maintUsername_txt        .setText(     conf.getProperty(       PROPKEY_maintUsername,         DEFAULT_maintUsername) );
		_maintPassword_txt        .setText(     conf.getProperty(       PROPKEY_maintPassword,         DEFAULT_maintPassword) );
		_rclCmd_chk               .setSelected( conf.getBooleanProperty(PROPKEY_rclCmd_chk,            DEFAULT_rclCmd_chk) );
		_rclCmd_txt               .setText(     conf.getProperty(       PROPKEY_rclCmd_txt,            DEFAULT_rclCmd_txt) );

		_outputFile_chk           .setSelected( conf.getBooleanProperty(PROPKEY_outputFile_chk,        DEFAULT_outputFile_chk) );
		_outputFile_txt           .setText(     conf.getProperty(       PROPKEY_outputFile_txt,        DEFAULT_outputFile_txt) );

		// FORMAT
		String str = conf.getProperty(PROPKEY_outputFormat);
		if ("SHORT".equals(str)) _outputFormatTxtShort_rbt.setSelected(true);
		if ("LONG" .equals(str)) _outputFormatTxtLong_rbt .setSelected(true);
		if ("TAB"  .equals(str)) _outputFormatTab_rbt     .setSelected(true);
		
		// QUEUE TYPE
		str = conf.getProperty(PROPKEY_queueNameType);
		if ("LCONN".equals(str)) _queueNameLconn_rbt.setSelected(true);
		if ("PCONN".equals(str)) _queueNamePconn_rbt.setSelected(true);
		if ("ROUTE".equals(str)) _queueNameRoute_rbt.setSelected(true);
		

		int lConnIndex = conf.getIntProperty(PROPKEY_queueNameLconn, 0); 
		int pConnIndex = conf.getIntProperty(PROPKEY_queueNamePconn, 0); 
		int routeIndex = conf.getIntProperty(PROPKEY_queueNameRoute, 0); 
		if (lConnIndex < _queueNameLconn_cbx.getItemCount()) _queueNameLconn_cbx.setSelectedIndex(lConnIndex);
		if (pConnIndex < _queueNamePconn_cbx.getItemCount()) _queueNamePconn_cbx.setSelectedIndex(pConnIndex);
		if (routeIndex < _queueNameRoute_cbx.getItemCount()) _queueNameRoute_cbx.setSelectedIndex(routeIndex);

		// QUEUE TYPE
		str = conf.getProperty(PROPKEY_queueType);
		if ("IN" .equals(str)) _queueTypeIn_rbt .setSelected(true);
		if ("OUT".equals(str)) _queueTypeOut_rbt.setSelected(true);
	}
	public void saveProps()
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);

		conf.setProperty(PROPKEY_simpleDumpQueue,       _doSimpleDumpQueue.isSelected());
		conf.setProperty(PROPKEY_filterOutAppliedTrans, _filterOutAppliedTrans_chk.isSelected());
		conf.setProperty(PROPKEY_maintUsername,         _maintUsername_txt.getText());
		conf.setProperty(PROPKEY_maintPassword,         _maintPassword_txt.getText(), true);
		conf.setProperty(PROPKEY_rclCmd_chk,            _rclCmd_chk.isSelected());
		conf.setProperty(PROPKEY_rclCmd_txt,            _rclCmd_txt.getText());

		conf.setProperty(PROPKEY_outputFile_chk,        _outputFile_chk.isSelected());
		conf.setProperty(PROPKEY_outputFile_txt,        _outputFile_txt.getText());

		if (_outputFormatTxtShort_rbt.isSelected()) conf.setProperty(PROPKEY_outputFormat, "SHORT");
		if (_outputFormatTxtLong_rbt .isSelected()) conf.setProperty(PROPKEY_outputFormat, "LONG");
		if (_outputFormatTab_rbt     .isSelected()) conf.setProperty(PROPKEY_outputFormat, "TAB");
		
		if (_queueNameLconn_rbt.isSelected()) conf.setProperty(PROPKEY_queueNameType, "LCONN");
		if (_queueNamePconn_rbt.isSelected()) conf.setProperty(PROPKEY_queueNameType, "PCONN");
		if (_queueNameRoute_rbt.isSelected()) conf.setProperty(PROPKEY_queueNameType, "ROUTE");

		conf.setProperty(PROPKEY_queueNameLconn,     _queueNameLconn_cbx.getSelectedIndex());
		conf.setProperty(PROPKEY_queueNamePconn,     _queueNamePconn_cbx.getSelectedIndex());
		conf.setProperty(PROPKEY_queueNameRoute,     _queueNameRoute_cbx.getSelectedIndex());

		if (_queueTypeIn_rbt .isSelected()) conf.setProperty(PROPKEY_queueType, "IN");
		if (_queueTypeOut_rbt.isSelected()) conf.setProperty(PROPKEY_queueType, "OUT");
		
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
		Object source    = (e == null ? null : e.getSource());
		String actionCmd = (e == null ? null : e.getActionCommand());

//System.out.println("ACTION LISTENER: source='"+source+"', actionCmd='"+actionCmd+"'.");

		_logger.debug("ACTION '"+actionCmd+"'.");

		boolean showMaint = _filterOutAppliedTrans_chk.isSelected();
		_maintUsername_lbl.setEnabled(showMaint);
		_maintUsername_txt.setEnabled(showMaint);
		_maintPassword_lbl.setEnabled(showMaint);
		_maintPassword_txt.setEnabled(showMaint);

		//
		boolean lConn = _queueNameLconn_rbt.isSelected();
		boolean pConn = _queueNamePconn_rbt.isSelected();
		boolean route = _queueNameRoute_rbt.isSelected();
//		_queueNameLconn_lbl.setVisible(lConn);
		_queueNameLconn_cbx.setVisible(lConn);

//		_queueNamePconn_lbl.setVisible(pConn);
		_queueNamePconn_cbx.setVisible(pConn);

//		_queueNameRoute_lbl.setVisible(route);
		_queueNameRoute_cbx.setVisible(route);
		if (lConn)
		{
			_queueTypeIn_rbt .setSelected(true);
			
			_queueTypeIn_rbt .setEnabled(false);
			_queueTypeOut_rbt.setEnabled(false);
			
//			_wsConnDesc_lbl.setText("Active '', Standby ''.");
		}
		if (pConn)
		{
			// First time we choose Physical Connection, Change to OutBound Queue
			if ( ! _queueTypeOut_rbt.isEnabled())
				_queueTypeOut_rbt.setSelected(true);

			_queueTypeIn_rbt .setEnabled(true);
			_queueTypeOut_rbt.setEnabled(true);

//			_wsConnDesc_lbl.setText("");
		}
		if (route)
		{
			_queueTypeOut_rbt.setSelected(true);
			
			_queueTypeIn_rbt .setEnabled(false);
			_queueTypeOut_rbt.setEnabled(false);

//			_wsConnDesc_lbl.setText("");
		}

		// Simulate ButtonGroup, which could have "none" selected
		if (_filterSegStartFirstActive_chk.equals(source) && _filterSegStartFirstActive_chk.isSelected()) _filterSegShowDeletedData_chk .setSelected(false);
		if (_filterSegShowDeletedData_chk .equals(source) && _filterSegShowDeletedData_chk .isSelected()) _filterSegStartFirstActive_chk.setSelected(false);
		if (_filterBlkCountToEndSeg_chk   .equals(source) && _filterBlkCountToEndSeg_chk   .isSelected()) _filterBlkCountToEndQueue_chk .setSelected(false);
		if (_filterBlkCountToEndQueue_chk .equals(source) && _filterBlkCountToEndQueue_chk .isSelected()) _filterBlkCountToEndSeg_chk   .setSelected(false);
		
		_filterSeg_sp     .setEnabled( ! (_filterSegStartFirstActive_chk.isSelected() || _filterSegShowDeletedData_chk.isSelected()) );
		_filterStartBlk_sp.setEnabled(true);
		_filterBlkCount_sp.setEnabled( ! (_filterBlkCountToEndSeg_chk.isSelected() || _filterBlkCountToEndQueue_chk.isSelected()) );
		_filterRows_sp    .setEnabled( ! _filterRowsShowAll_chk.isSelected() );

		// Enable editing of RCL Command
		_rclCmd_txt.setEnabled( _rclCmd_chk.isSelected());
		_rclCmd_txt.setEditable(_rclCmd_chk.isSelected());

		
		
		// If not checked, compose a command to use
		if ( _rclCmd_chk.isSelected() )
		{
			_filterOutAppliedTrans_chk.setSelected(false);
			_selectedDestSrvName   = null;
			_selectedDestDbName    = null;
			_selectedDestRouteName = null;
		}
		else
		{
			setRclCommand(e);
		}

		//---------------------
		// ACTIONS
		//---------------------
		if (ACTION_CONNECT.equals(actionCmd))
			action_connect(e);

		if (ACTION_DISCONNECT.equals(actionCmd))
			action_disconnect(e);

		if (ACTION_EXIT.equals(actionCmd))
			action_exit(e);

		if (ACTION_SHOW.equals(actionCmd))
			action_show(e);

		setComponentVisibility();
	}

	private void setRclCommand(ActionEvent e)
	{
		// seg > 0, blk = 1..64: start dumping at specified segment and block
		// seg -1,  blk = 1:     start dumping at the start of the first active segment
		// seg -1,  blk = -1:    start dumping at the first undeleted block in the queue
		// seg -1,  blk = -2:    start dumping at the first unread block in the queue
		// seg -2,  blk = 1:     start dumping at the first queue segment (including inactive segments still allocated due to a "save_interval" setting) and the specified block 

		// cnt: -1: Dumps until the end of the segment
		// cnt: -2: Dumps until the end of the queue
		
		// opt: L0=, l1=, L2=
		
		String dbid    = null;
		String qid     = null;
		String seg     = "";
		String blk     = "";
		String cnt     = "";
		String numCmds = "";
		String opt     = "L0";

		_selectedDestSrvName   = null;
		_selectedDestDbName    = null;
		_selectedDestRouteName = null;
		
		// SERVER and DBNAME
		if (_queueNameLconn_rbt.isSelected())
		{
			Object lSrv = _queueNameLconn_cbx.getSelectedItem();
			if ( lSrv instanceof LogicalConnectionItem )
			{
				String[] sa = ((LogicalConnectionItem) lSrv)._logicalName.split("\\.");
				_selectedDestSrvName   = sa.length >= 1 ? sa[0] : null;
				_selectedDestDbName    = sa.length >= 2 ? sa[1] : null;
				dbid = "'" + _selectedDestSrvName + "', '" + _selectedDestDbName + "'";
	
				// Set the below values to STANDBY server instead of the LOGICAL name
				sa = ((LogicalConnectionItem) lSrv)._standbyName.split("\\.");
				_selectedDestSrvName   = sa.length >= 1 ? sa[0] : null;
				_selectedDestDbName    = sa.length >= 2 ? sa[1] : null;
			}
		}

		if (_queueNamePconn_rbt.isSelected())
		{
			Object pSrv = _queueNamePconn_cbx.getSelectedItem();
			if ( pSrv instanceof PhysicalConnectionItem )
			{
				String[] sa = ((PhysicalConnectionItem) pSrv)._name.split("\\.");
				_selectedDestSrvName   = sa.length >= 1 ? sa[0] : null;
				_selectedDestDbName    = sa.length >= 2 ? sa[1] : null;
				dbid = "'" + _selectedDestSrvName + "', '" + _selectedDestDbName + "'";
			}
		}

		if (_queueNameRoute_rbt.isSelected())
		{
			Object route = _queueNameRoute_cbx.getSelectedItem();
			if ( route instanceof RouteItem )
			{
				// FIXME: this is not how to do it, we need to lookup the RSID... I think...
				_selectedDestRouteName = ((RouteItem) route)._name;
				dbid = "'" + _selectedDestRouteName + "'";
			}
		}

		if (dbid == null)
			dbid = "<DBID>";

		// QUEUE TYPE
//		String queueType = _queueType_cbx.getSelectedItem() + "";
//		if ( QUEUE_TYPE_IN.equals(queueType) )
		if (_queueTypeIn_rbt.isSelected())
			qid = "1";
		else
			qid = "0";

//		if (qid == null)
//			qid = "<0|1>";
		
		if (_filterSeg_sp.isEnabled())                   seg = ""+_filterSeg_spm.getValue();
		if (_filterSegStartFirstActive_chk.isSelected()) seg = "-1";
		if (_filterSegShowDeletedData_chk .isSelected()) seg = "-2";

		if (_filterStartBlk_sp.isEnabled())              blk = ""+_filterStartBlk_spm.getValue();
		
		if (_filterBlkCount_sp.isEnabled())              blk = ""+_filterBlkCount_spm.getValue();
		if (_filterBlkCountToEndSeg_chk   .isSelected()) cnt = "-1";
		if (_filterBlkCountToEndQueue_chk .isSelected()) cnt = "-2";

		if (_filterRows_sp.isEnabled())                  numCmds = ""+_filterRows_spm.getValue();
		if (_filterRowsShowAll_chk.isSelected())         numCmds = "-1";

//		Syntax in 15.7.1
//		sysadmin dump_queue {, q_number | server[,database]}, qtype
//		{, seg, blk, cnt [, num_cmds] [, {L0 | L1 | L2 | L3}] [, {RSSD | client | "log" | file_name}]
//		   | "next" [, num_cmds]
//		}
		
		if (_doSimpleDumpQueue.isSelected())
		{
			seg     = "-1";
			blk     = "-2";
			cnt     = "-1";
//			numCmds = "-1";
			opt     = "L0";
			_rclCmd_txt.setText("sysadmin dump_queue, "+dbid+", "+qid+", "+seg+", "+blk+", "+cnt+", "+opt+", client");
		}
		else
		{
			_rclCmd_txt.setText("sysadmin dump_queue, "+dbid+", "+qid+", "+seg+", "+blk+", "+cnt+", "+numCmds+", "+opt+", client");
		}
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
//		boolean showAseTab     = true;
//		boolean showAseOptions = false;
//		boolean showHostmonTab = false;
//		boolean showPcsTab     = false;
//		boolean showOfflineTab = false;
//		boolean showJdbcTab    = false;

		_connectedToProductName    = null;
		_connectedToProductVersion = null;
		_connectedToServerName     = null;
		_connectedAsUser           = null;
		_connectedWithUrl          = null;

		com.asetune.gui.ConnectionDialog.Options connDialogOptions = new com.asetune.gui.ConnectionDialog.Options();
		connDialogOptions._srvExtraChecks           = null;
		connDialogOptions._showAseTab               = true;
		connDialogOptions._showDbxTuneOptionsInTds  = false;
		connDialogOptions._showHostmonTab           = false;
		connDialogOptions._showPcsTab               = false;
		connDialogOptions._showOfflineTab           = false;
		connDialogOptions._showJdbcTab              = false;
		connDialogOptions._showDbxTuneOptionsInJdbc = false;

//		ConnectionDialog connDialog = new ConnectionDialog(_jframe, null, showAseTab, showAseOptions, showHostmonTab, showPcsTab, showOfflineTab, showJdbcTab, false);
		ConnectionDialog connDialog = new ConnectionDialog(_jframe, connDialogOptions);

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
		
		if ( connType == ConnectionDialog.TDS_CONN)
		{
			_conn = connDialog.getAseConn();

//			if (_conn != null)
			if (AseConnectionUtils.isConnectionOk(_conn, true, _jframe))
			{
				if (connDialog.isDatabaseProduct(DbUtils.DB_PROD_NAME_SYBASE_RS))
				{
					_srvVersion = AseConnectionUtils.getRsVersionNumber(_conn);
					_logger.info("Connected to Replication Server version '"+_srvVersion+"'.");
				}
				else
				{
					_logger.info("Connected to 'other' Sybase TDS server with product name'"+_connectedToProductName+"'.");
				}

				setComponentVisibility();
			}
		}
	}
	private void setComponentVisibility()
	{
	}

	/**
	 * 
	 * @param dsdb
	 */
	public boolean setDsDbName(String dsdb)
	{
System.out.println("----- setDsDbName(): dsdb='"+dsdb+"'.");

		if (StringUtil.isNullOrBlank(dsdb))
			return false;

//		String[] sa = dsdb.split("\\.");
//		String dsname = sa.length >= 1 ? sa[0] : null;
//		String dbname = sa.length >= 2 ? sa[1] : null;
//		
//		RsDatabases.Entry entry = _rsDatabases.getEntry(dsdb);

		JComboBox cbx;
		JRadioButton rbt;
		boolean found = false;
		// CHECK LOGICAL CONNECTIONS
		if ( ! found )
		{
			cbx = _queueNameLconn_cbx;
			rbt = _queueNameLconn_rbt;
			for (int i=0; i<cbx.getItemCount(); i++)
			{
				Object obj = cbx.getItemAt(i);
				if (obj instanceof LogicalConnectionItem)
				{
					if ( dsdb.equals( ((LogicalConnectionItem) obj)._standbyName ) )
					{
						cbx.setSelectedIndex(i);
						found = true;
						rbt.setSelected(true);
System.out.println("----- setDsDbName(): FOUND in LOGICAL: index="+i+", dsdb='"+dsdb+"'.");
						break;
					}
				}
			}
		}
		// CHECK PHYSICAL CONNECTIONS
		if ( ! found )
		{
			cbx = _queueNamePconn_cbx;
			rbt = _queueNamePconn_rbt;
			for (int i=0; i<cbx.getItemCount(); i++)
			{
				Object obj = cbx.getItemAt(i);
				if (obj instanceof PhysicalConnectionItem)
				{
					if ( dsdb.equals( ((PhysicalConnectionItem) obj)._name ) )
					{
						cbx.setSelectedIndex(i);
						found = true;
						rbt.setSelected(true);
System.out.println("----- setDsDbName(): FOUND in PHYSICAL: index="+i+", dsdb='"+dsdb+"'.");
						break;
					}
				}
			}
		}
		// CHECK ROUTES
		if ( ! found )
		{
			cbx = _queueNameRoute_cbx;
			rbt = _queueNameRoute_rbt;
			for (int i=0; i<cbx.getItemCount(); i++)
			{
				Object obj = cbx.getItemAt(i);
				if (obj instanceof RouteItem)
				{
					if ( dsdb.equals( ((RouteItem) obj)._name ) )
					{
						cbx.setSelectedIndex(i);
						found = true;
						rbt.setSelected(true);
System.out.println("----- setDsDbName(): FOUND in ROUTES: index="+i+", dsdb='"+dsdb+"'.");
						break;
					}
				}
			}
		}

		// NOT FOUND
		if ( ! found )
		{
			System.out.println("----- setDsDbName(): -NOT-FOUND-  dsdb='"+dsdb+"'.");
		}
		
		return found;
	}

	/**
	 * 
	 * @param dbid
	 */
	public boolean setDbId(int dbid)
	{
System.out.println("----- setDbId(): dbid="+dbid+".");
		return setDsDbName(_rsDatabases.getSrvDb(dbid));
	}

	/**
	 * Set the queue type
	 * @param queueType 1 = Inbound Queue, 0 = Outbound Queue
	 * @throws IllegalArgumentException if queueType is not 1 or 0
	 */
	public void setQueueType(int queueType)
	{
System.out.println("----- setQueueType(): queueType="+queueType+".");
		// IN queue
		if (queueType == 1)
		{
			_queueTypeIn_rbt.setSelected(true);
		}
		// OUT queue
		else if (queueType == 0)
		{
			_queueTypeOut_rbt.setSelected(true);
		}
		else
		{
			throw new IllegalArgumentException("setQueueType() must be 1 for inbound queue or 0 for outbound queue");
		}
	}

	public void readQueue()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				_show_but.doClick();
			}
		});
	}

	private void refreshConnectionNames()
	{
		List<String> lSrvDb = RepServerUtils.getLogicalConnections(_conn, true);
		List<String> pSrvDb = RepServerUtils.getConnections(_conn);
		List<String> routes = RepServerUtils.getRoutes(_conn);
		
		_rsDatabases = RsDatabases.getRsDatabases(_conn);

		_queueNameLconn_cbx.removeAllItems();
		_queueNameLconn_cbx.addItem("<Choose a Warm Standby Connection>");
		for (String name : lSrvDb)
		{
			String[] sa = name.split(":");
			String lStr = sa[0]; // Logical
			String aStr = sa[1]; // Active
			String sStr = sa[2]; // Standby

			_queueNameLconn_cbx.addItem(new LogicalConnectionItem(lStr, aStr, sStr));
		}

		_queueNamePconn_cbx.removeAllItems();
		_queueNamePconn_cbx.addItem("<Choose a Connection>");
		for (String name : pSrvDb)
			_queueNamePconn_cbx.addItem(new PhysicalConnectionItem(name));

		_queueNameRoute_cbx.removeAllItems();
		_queueNameRoute_cbx.addItem("<Choose a Route>");
		for (String name : routes)
			_queueNameRoute_cbx.addItem(new RouteItem(name));
	}
	
	/** class to be added to JComboBox */
	private class LogicalConnectionItem
	{
		String _logicalName = "";
		String _activeName  = "";
		String _standbyName = "";
		public LogicalConnectionItem(String logicalName, String activeName, String standbyName)
		{
			_logicalName = logicalName;
			_activeName  = activeName;
			_standbyName = standbyName;
		}
		@Override
		public String toString()
		{
			return "<html><font color=\"blue\">" + _logicalName + "</font> -- <i><font color=\"green\"><b>Active</b> "+_activeName+", <b>Standby</b> "+_standbyName+"</font></i></html>";
		}
	}
	/** class to be added to JComboBox */
	private class PhysicalConnectionItem
	{
		String _name = "";
		public PhysicalConnectionItem(String name)
		{
			_name = name;
		}
		@Override
		public String toString()
		{
			return "<html><font color=\"blue\">" + _name + "</font></html>";
		}
	}
	/** class to be added to JComboBox */
	private class RouteItem
	{
		String _name = "";
		public RouteItem(String name)
		{
			_name = name;
		}
		@Override
		public String toString()
		{
			return "<html><font color=\"blue\">" + _name + "</font></html>";
		}
	}

//	private void setComponentVisibility()
//	{
////		_rsInTabs.setVisible(false);
//		_dbs_cobx.setVisible(true);
//		
////		if (_conn == null)
//		if ( ! AseConnectionUtils.isConnectionOk(_conn, false, null) )
//		{
//			_connect_mi     .setEnabled(true);
//			_connect_but    .setEnabled(true);
//			_disconnect_mi  .setEnabled(false);
//			_disconnect_but .setEnabled(false);
//			
//			_dbs_cobx       .setEnabled(false);
//			_exec           .setEnabled(false);
//			_asPlainText    .setEnabled(false);
//			
//			setSrvInTitle("not connected");
//			_statusBar.setServerName(null, null, null, null, null, null);
//
//			return;
//		}
//		else
//		{
//			_connect_mi     .setEnabled(false);
//			_connect_but    .setEnabled(false);
//			_disconnect_mi  .setEnabled(true);
//			_disconnect_but .setEnabled(true);
//		}
//
////		if ( _connType == ConnectionDialog.TDS_CONN)
////		{
////			// Set server name in windows - title
////			String aseSrv      = AseConnectionFactory.getServer();
////			String aseHostPort = AseConnectionFactory.getHostPortStr();
////			String srvStr      = aseSrv != null ? aseSrv : aseHostPort; 
////
////			setSrvInTitle(srvStr);
////			_statusBar.setServerName(srvStr, _connectedToProductName, _connectedToProductVersion, _connectedToServerName, _connectedAsUser, _connectedWithUrl);
////			
////			if (_connectedToProductName != null && _connectedToProductName.equals(ConnectionDialog.DB_PROD_NAME_SYBASE_ASE))
////			{
////				_ase_viewConfig_mi.setVisible(true);
////
////				_dbs_cobx       .setEnabled(true);
////				_exec           .setEnabled(true);
////				_rsInTabs       .setEnabled(true);
////				_asPlainText    .setEnabled(true);
////				_setOptions     .setEnabled(true);
////				_execGuiShowplan.setEnabled( (_aseVersion >= 15000) );
////			}
////			else if (_connectedToProductName != null && _connectedToProductName.equals(ConnectionDialog.DB_PROD_NAME_SYBASE_RS))
////			{
////				_rs_configChangedDdl_mi.setVisible(true);
////				_rs_configAllDdl_mi    .setVisible(true);
////
////				_dbs_cobx              .setVisible(false);
////				_exec                  .setEnabled(true);
////				_rsInTabs              .setEnabled(true);
////				_asPlainText           .setEnabled(true);
////				_setOptions            .setEnabled(false);
////				_execGuiShowplan       .setEnabled(false);
////			}
////		}
//	}

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
//				_connType = -1;

//				_dbs_cobx       .setEnabled(false);
//				_exec           .setEnabled(false);
//				_asPlainText    .setEnabled(false);

				setSrvInTitle(null);
//				_statusBar.setServerName(null, null, null, null, null, null, null);
				_statusBar.setNotConnected();
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

	private void action_show(ActionEvent e)
	{
		final Connection conn = _conn;
		final String rcl = _rclCmd_txt.getText();
		
System.out.println("RCL TO EXECUTE: "+rcl);

if (e != null)
saveProps();

		// If we've called close(), then we can't call this method
		if (conn == null)
			throw new IllegalStateException("Connection already closed.");

		// get rs_lastcommit information from Destinstion Server
		// This so we can filter out already applied transactions.
		RsLastcommit tmp_rsLastcommit = null; 
		if (_filterOutAppliedTrans_chk.isSelected())
		{
			try
			{
				String username = _maintUsername_txt.getText();
				String password = _maintPassword_txt.getText();

				username = username.replaceAll("<dbname>", _selectedDestDbName);
				password = password.replaceAll("<dbname>", _selectedDestDbName);
				
//				tmp_rsLastcommit = RsLastcommit.getRsLastcommit(conn, _selectedDestSrvName, _selectedDestDbName);
				tmp_rsLastcommit = RsLastcommit.getRsLastcommit(_selectedDestSrvName, _selectedDestDbName, username, password);
			}
			catch (SQLException e1)
			{
				tmp_rsLastcommit = null;
				String msg = "<html>" +
						"Can <b>not</b> filter out Already Applied or Replicated records<br>" +
						"<br>" +
						"Problems accessing the table '<b>rs_lastcommit</b>' in ASE '<b>"+_selectedDestSrvName+"</b>' db '<b>"+_selectedDestDbName+"</b>' <br>" +
						"<br>" +
						"Problem: <i>"+e1.getMessage()+"</i>" +
						"</html>";
				SwingUtils.showErrorMessage("Problems getting RsLastcommit info", msg, e1);
			}
		}
		final RsLastcommit rsLastcommit = tmp_rsLastcommit;
			
//		if (_filterOutAppliedTrans_chk.isSelected())
//			rsLastcommit = RsLastcommit.getRsLastcommit(conn, _selectedSrvName, _selectedDbName);
		
		// It may take a while to get the results, so give the user some
		// immediate feedback that their query was accepted.
		_statusBar.setMsg("Sending RCL to RepServer...");

		// Reset OUTPUT
		_dumpQueueTxt.setText("");
		_dumpQueueTab.setModel(new DefaultTableModel());


		WaitForExecDialog wait = new WaitForExecDialog(_window, rcl);
		// Kick this of as it's own thread, otherwise the sleep below, might block the Swing Event Dispatcher Thread
		BgExecutor bgExec = new BgExecutor(wait)
		{
			@Override
			public boolean canDoCancel()
			{
				return true;
			}
			
			@Override
			public void cancel() 
			{
				if (conn != null && (conn instanceof SybConnection || conn instanceof TdsConnection) )
				{
					try
					{
						if (conn instanceof SybConnection) ((SybConnection)conn).cancel();
						if (conn instanceof TdsConnection) ((TdsConnection)conn).cancel();
					}
					catch(SQLException ex)
					{
						SwingUtils.showErrorMessage("Cancel", "Problems sending cancel to ASE: "+ex, ex);
					}
				}
				//super.cancel();
			};

			@Override
			public Object doWork()
			{
				// Setup a message handler
				// Set an empty Message handler
				SybMessageHandler curMsgHandler = null;
				if (conn instanceof SybConnection || conn instanceof TdsConnection)
				{
					SybMessageHandler newMessageHandler = new SybMessageHandler()
					{
						@Override
						public SQLException messageHandler(SQLException sqle)
						{
//							progress.addMessage(sqle);

							return AseConnectionUtils.sqlExceptionToWarning(sqle);
						}
					};
					
					if (conn instanceof SybConnection)
					{
						curMsgHandler = ((SybConnection)conn).getSybMessageHandler();
						((SybConnection)conn).setSybMessageHandler(newMessageHandler);
					}

					// Set a TDS Message Handler
					if (conn instanceof TdsConnection)
						((TdsConnection)conn).setSybMessageHandler(newMessageHandler);
				}

				try
				{
					Statement  stmnt = conn.createStatement();

					boolean hasRs = stmnt.execute(rcl);
//					ResultSet rs = stmnt.executeQuery(rcl);

					ResultSet rs = null;
					if (hasRs)
						rs = stmnt.getResultSet();
					else
					{
System.out.println("NO RESULT SET ");
						return null;
					}

//					progress.setState("Waiting for Server to return resultset.");
					_statusBar.setMsg("Waiting for Server to return resultset.");
			
					_statusBar.setMsg("Reading resultset.");
			
//					String outputFormat = _outputFormat_cbx.getSelectedItem() + "";
//					boolean asTable = OUT_JTABLE.equals(outputFormat);
					boolean asTable = _outputFormatTab_rbt.isSelected();
					if (asTable)
					{
System.out.println("TO TABLE");

						// reset the Text Area, to reduce memory
						_dumpQueueTxt.setText("");

						// Convert the ResultSet into a TableModel, which fits on a JTable
						ResultSetTableModel tm = new ResultSetTableModel(rs, true, rcl);
//System.out.println("ResultSetTableModel.toTableString():\n"+tm.toTableString());

						_dumpQueueTab.setModel(tm);
						_dumpQueueTab.setSortable(true);
						_dumpQueueTab.setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);
						_dumpQueueTab.packAll(); // set size so that all content in all cells are visible
						_dumpQueueTab.setColumnControlVisible(true);
						_dumpQueueTab.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
						_dumpQueueTab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

//						// Add a popup menu
////						tab.setComponentPopupMenu( createDataTablePopupMenu(tab) );

						_dumpQueueTabScroll.setVisible(true);
						_dumpQueueTxtScroll.setVisible(false);
					}
					else
					{
System.out.println("TO TXT");

						// reset the Table Model, to reduce memory
						_dumpQueueTab.setModel(new DefaultTableModel());

//						boolean shortDesc = OUT_TEXT_SHORT.equals(outputFormat);
						boolean shortDesc = _outputFormatTxtShort_rbt.isSelected();
						_dumpQueueTxt.setText(buildDumpString(rs, rsLastcommit, shortDesc, getWaitDialog()));

						_dumpQueueTabScroll.setVisible(false);
						_dumpQueueTxtScroll.setVisible(true);
						//_resultCompList.add(new JPlainResultSet(tm));				
					}

					// Close it
					rs.close();
					stmnt.close();

					// To get sizing correct
					_dataPanel.repaint();
					
//					progress.setState("Add data to GUI result");

				}
				catch (SQLException ex)
				{
					// If something goes wrong, clear the message line
//					_msgline.setText("Error: "+ex.getMessage());
					_statusBar.setMsg("Error: "+ex.getMessage());
					ex.printStackTrace();

					// Then display the error in a dialog box
					JOptionPane.showMessageDialog(
//							QueryWindow.this, 
							_window, 
							new String[] { // Display a 2-line message
									ex.getClass().getName() + ": ", 
									ex.getMessage() },
							"Error", JOptionPane.ERROR_MESSAGE);
				}
				finally
				{
					// restore old message handler
					if (curMsgHandler != null)
					{
						((SybConnection)conn).setSybMessageHandler(curMsgHandler);
					}

					// Restore old message handler
					if (conn instanceof TdsConnection)
						((TdsConnection)conn).restoreSybMessageHandler();
				}
				getWaitDialog().setState("Done");

				return null;
			}
		};
		wait.execAndWait(bgExec);
		_statusBar.setMsg("");

		
		// In some cases, some of the area in not repainted
		// example: when no RS, but only messages has been displayed
//		_resPanel.repaint();
		
	}

	private String buildDumpString(ResultSet rs, RsLastcommit rsLastcommit, boolean shortDesc, WaitForExecDialog wait)
//	throws SQLException
	{
//		ResultSetTableModel tm = new ResultSetTableModel(rs, true);
//		return tm.toString();

		StringBuilder sb = new StringBuilder();
		List<List<String>> dumpList = getDumpQueue(rs, rsLastcommit, shortDesc, wait);
		for (List<String> sqlList : dumpList)
		{
			for (String str : sqlList)
			{
				sb.append(str);
				sb.append("\n");
			}
			sb.append("\n\n");
		}
		return sb.toString();
	}
	private List<List<String>> getDumpQueue(ResultSet rs, RsLastcommit rsLastcommit, boolean shortDesc, WaitForExecDialog wait)
	{
		// To be able to "sort" the transaction and append info to them...
		// the key would be: tranId, and the object would be a linked list with Strings
		List<List<String>> transList = new ArrayList<List<String>>();

		// hold transactions until they are "closed"
		LinkedHashMap<String, List<String>> tmpTransTable = new LinkedHashMap<String, List<String>>();

//		// The list below will keep the "order" of transactions.
//		List transOrderedList = new LinkedList();

		RsLastcommitEntry usedToParseOqid = new RsLastcommitEntry();

		List<String> discardQueueRecordsList = new ArrayList<String>();
		int discardQueueRecordCount = 0;
		if (rsLastcommit != null)
		{
			discardQueueRecordsList.add("-- Removing Queue Records with a OriginQueueID less than OQID");
			for (RsLastcommitEntry rse : rsLastcommit.values())
				discardQueueRecordsList.add("-- Origin: "+rse._origin+" OQID: 0x"+rse._origin_qid);

			transList.add(discardQueueRecordsList);
		}

		try
		{
			int cmdsRead = 0;
			int reportEveryRow = 10;
//			ResultSetTableModel tm = new ResultSetTableModel(rs, true);
//			return tm.toTableString();
			while (rs.next())
			{
				cmdsRead++;
				if (wait != null && (cmdsRead % reportEveryRow) == 0)
					wait.setState("Read "+cmdsRead+" Command from the Queue.");

//				try { Thread.sleep(200); }
//				catch (InterruptedException ignore) {}

				int       qNumber           = rs.getInt      (1);
				int       qType             = rs.getInt      (2);
				int       segment           = rs.getInt      (3);
				int       block             = rs.getInt      (4);
				int       row               = rs.getInt      (5);
				int       messageLen        = rs.getInt      (6);
				int       orgnSiteid        = rs.getInt      (7);
				Timestamp orgnTime          = rs.getTimestamp(8);
				String    orgnQid           = rs.getString   (9);
				String    orgnUser          = rs.getString   (10);
				String    tranName          = rs.getString   (11);
				String    localQid          = rs.getString   (12);
				int       status            = rs.getInt      (13);
				String    tranid            = rs.getString   (14);
				int       logicalOrgnSiteid = rs.getInt      (15);
				int       version           = rs.getInt      (16);
				int       commandLen        = rs.getInt      (17);
				int       seqNo             = rs.getInt      (18);
				String    command           = rs.getString   (19);

				boolean alreadyAppliedToDest = false;
//				if (destLastcommit._origin_qid.compareTo(orgnQid) > 0)
//				{
//					alreadyAppliedToDest = true;
//				}
				if (rsLastcommit != null)
				{
					try
					{
						alreadyAppliedToDest = rsLastcommit.hasOqidBeenApplied(orgnSiteid, orgnQid);
					}
					catch (OriginNotFoundException e)
					{
						e.printStackTrace();
					}
				}

				if ( alreadyAppliedToDest )
				{
					// FIXME: move this _CHK to a input variable
					if (_filterDebugAppliedTrans_chk.isSelected())
						discardQueueRecordsList.add("/* discarded: Origin="+orgnSiteid+", OQID="+orgnQid+", seqNo="+seqNo+", Cmd: */ "+command);

					discardQueueRecordCount++;
					
//					String header
//						="originSiteId='"    + orgnSiteid
//						+"', originTime='"   + orgnTime
//						+"', originUser='"   + orgnUser
//						+"', TranName='"     + tranName
//						+"', generationId='" + usedToParseOqid._generationId
//						+"', logPage='"      + usedToParseOqid._logPage
//						+"', logRow='"       + usedToParseOqid._logRow
//						+"', LogTs='"        + usedToParseOqid._logTimestamp
//						+"'. CMD='"          + command + "'";
					//System.out.println("AlreadyApplied: "+header);
				}
				else
				{
					List<String> trans = tmpTransTable.get(tranid);
					if (trans == null)
					{
						trans = new LinkedList<String>();
						tmpTransTable.put(tranid, trans);

						// This if we want various parts of the OQID
						usedToParseOqid._origin_qid = orgnQid;
						usedToParseOqid.parseOriginQId();

//						// Add a first row, which would contain info about the transaction
//						String header
//							="originSiteId='"    + orgnSiteid
//							+"', originTime='"   + orgnTime
//							+"', originUser='"   + orgnUser
//							+"', TranName='"     + tranName
//							+"', generationId='" + usedToParseOqid._generationId
//							+"', logPage='"      + usedToParseOqid._logPage
//							+"', logRow='"       + usedToParseOqid._logRow
//							+"', LogTs='"        + usedToParseOqid._logTimestamp
//							+"'.";

						String originSiteName = "FIXME:nameOfTheId";
						if (_rsDatabases != null)
							originSiteName = _rsDatabases.getSrvDb(orgnSiteid);

						StringBuilder sb = new StringBuilder();
						sb.append("/*=========================================================*\n");
						sb.append(" * Origin-Begin-Time '").append(orgnTime).append("'")
							.append(", Origin-Commit-Time '").append("ORIGIN_COMMIT_TIME").append("'")
							.append(", Origin-User '").append(orgnUser).append("'")
							.append(", Origin-Siteid ").append(orgnSiteid).append(" (").append(originSiteName).append(")")
							.append("\n");
//						sb.append(" * Origin Time: ").append(orgnTime).append("\n");
//						sb.append(" * Origin User: ").append(orgnUser).append("\n");
//						sb.append(" * Origin Siteid: ").append(orgnSiteid).append(" (").append("FIXME:nameOfTheId").append(")\n");

						if (! shortDesc)
						{
							sb.append(" * QNumber(").append(qNumber).append(")")
								.append(", QType(").append(qType).append(")")
								.append(", Segment(").append(segment).append(")")
								.append(", Block(").append(block).append(")")
								.append(", Row(").append(row)
								.append(")\n");
							sb.append(" * Origin-DBGen(").append(usedToParseOqid._generationId).append(")")
								.append(", TranID(").append(usedToParseOqid._logPage).append(",").append(usedToParseOqid._logRow).append(")")
//								.append(", BeginTID(").append("FIXME").append(")")
								.append(", LogPageTS(0x").append(usedToParseOqid._logTimestamp)
								.append(")\n");
							sb.append(" * QID=0x").append(orgnQid).append("\n");
						}


//		/*				if ( !((Status & SQM_STATUS_BEGIN_M)		== SQM_STATUS_BEGIN_M) )*/
//						if (    Status != SQM_STATUS_BEGIN_M
//						     && Status != SQM_STATUS_ROLLBACK_M
//						     && Status != (SQM_STATUS_BEGIN_M | SQM_STATUS_SYSTRAN_M)
//						   )
//						{
//						inside_unknown_message = 1;
//						sb.append(" * No program logics for Status(%d)\n", Status);
//						sb.append(" * TURN ON THE TRACEFLAG '-X explain_status'\n");
//						sb.append(" * Run again and mail me the output (goran_schwarz@hotmail.com)\n");
//						explain_status(buf, Status, 0);
//						sb.append(" * Status short explain(%s)\n", buf);
//						explain_status(buf, Status, 1);
//						sb.append(" * Status long  explain(%s)\n", buf);
//						}
//						else
//						{
//						inside_unknown_message = 0;
//						}
//
//						if (config->only_active  &&  !config->dest_conn)
//						{
//						sb.append(" * NOTE: Can't get OQID from the destination database.\n");
//						sb.append(" *       This means that transactions that has already been applied to the\n");
//						sb.append(" *       destination database may also show up in the dump output.\n");
//						sb.append(" *       This is because no connection to the RDB was established.\n");
//						}
						sb.append(" *---------------------------------------------------------*/");
						
//						trans.add(header);
						trans.add(sb.toString());
					}

					if (seqNo == 0)
						trans.add(command);
					else
					{
						int listPos = trans.size()-1;
						String cmdInList = trans.get( listPos );
						cmdInList += command;
						trans.set(listPos, cmdInList);
					}

					// FIXME: is status bit 1 = "no-more-records"
					//        if so then it's time to add/move the records to the "sorted transaction order", since it's complete
					if ((status & 1) == 1)
					{
						if (status == 1)
							trans.add("go");
						else
							trans.add("reset");
					}
					if ((status & 2) == 2)
						trans.add("reset");


					// FIXME: is status bit 1 = "no-more-records"
					//        if so then it's time to add/move the records to the "sorted transaction order", since it's complete
					if ( ((status & 1) == 1) || ((status & 2) == 2) )
					{
						List<String> moveMe = tmpTransTable.get(tranid);
						if (moveMe != null)
						{
							// FIX comment 'ORIGIN_COMMIT_TIME'
							String cmdInList = trans.get( 0 );
							cmdInList = cmdInList.replace("ORIGIN_COMMIT_TIME", orgnTime.toString());
							trans.set(0, cmdInList);

							// move from tmp list to output list
							transList.add(moveMe);
							tmpTransTable.remove(tranid);
						}
						else
						{
							System.out.println("hmmm tran '"+tranid+"' not in tmpMap.");
						}
					}
				}

				if ( _logger.isDebugEnabled() )
				{
					_logger.debug("dumpWsQueue(alreadyAppliedToDest="+alreadyAppliedToDest+"): qNumber='"+qNumber+"', qType='"+qType+"', segment='"+segment
						+"', block='"+block+"', row='"+row+"', messageLen='"+messageLen+"', orgnSiteid='"+orgnSiteid
						+"', orgnTime='"+orgnTime+"', orgnQid='"+orgnQid+"', orgnUser='"+orgnUser+"', tranName='"+tranName
						+"', localQid='"+localQid+"', status='"+status+"', tranid='"+tranid+"', logicalOrgnSiteid='"+logicalOrgnSiteid
						+"', version='"+version+"', commandLen='"+commandLen+"', seqNo='"+seqNo+"', command='"+command+"'.");
				}

//				// If negative number, log everything
//				if (onlyXFirstRecords > 0)
//				{
//					onlyXFirstRecords--;
//					if (onlyXFirstRecords == 0)
//						break;
//				}
			} // end: rs.getNext()
//			rs.close();

			if (rsLastcommit != null)
			{
				discardQueueRecordsList.add("-- "+discardQueueRecordCount+" Records were discared.");
				discardQueueRecordsList.add("reset");
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		// Lets DUMP/PRINT transactions that was not CLOSED
		// Probably to large... and not part of the 'sysadmin dump_queue *cnt*'
		if (tmpTransTable.size() > 0)
		{
			for (List<String> list : tmpTransTable.values())
			{
//				System.out.println("ERROR: left in tmpTransTable:");
//				for (String str : list)
//					System.out.println("       ROW: "+str);
				
				String warnHeader = "#####################################################################\n" +
				                    "#### WARNING #### WARNING #### WARNING #### WARNING #### WARNING ####\n" +
				                    "#### BELOW TRANSACTION IS NOT COMPLETE - CNT WAS PROBABLY TO LOW ####\n" +
				                    "#### " + list.size()+ " records was found in this un-closed transaction ####\n" +
				                    "#####################################################################\n";
	
				String warnFooter = "#####################################################################\n" +
				                    "#### WARNING #### WARNING #### WARNING #### WARNING #### WARNING ####\n" +
				                    "#### ABOVE TRANSACTION IS NOT COMPLETE - CNT WAS PROBABLY TO LOW ####\n" +
				                    "#### " + list.size()+ " records was found in this un-closed transaction ####\n" +
				                    "#####################################################################\n" +
				                    "reset";

				// Add warning header and footer
				list.add(0, warnHeader);
				list.add(warnFooter);
				
				// Add it to the end of the OUTPUT transaction list
				transList.add(list);
				
				// Dont remove it from tmpTransTable, will probably throw "ConcurrentModificationException"
				//tmpTransTable.remove(tranid); // NOTE: this is just for info... tranid is not found here...
			}
		}
		
		return transList;
	}
//	///////////////////////////////////////////////////////////////////////
//	///////////////////////////////////////////////////////////////////////
//	//// dump queue
//	///////////////////////////////////////////////////////////////////////
//	///////////////////////////////////////////////////////////////////////
//	/**
//	 * Dumps the Stable queue for a specific destination.
//	 * <p>
//	 * The returned list is sorted on the transaction start order not the commit order.
//	 * But since a WS is not applying in commit order but "first in first out" this would work...
//	 * But there are room for improvements.
//	 * <p>
//	 * Every row in the output list is a List itself, that list has first row with a "header"
//	 * then the commands comes.
//	 *
//	 * @param lsrv Logical server connaction name
//	 * @param ldb  Logical database connaction name
//	 * @param destLastcommit a RsLastcommitEntry record fetched from the destination database.
//	 * @param onlyXFirstRecords only read the first X rows from the repserver before giving up -1 would be to read all records.
//	 * @return A List of transactions
//	 */
//	public List dumpWsQueue(String lsrv, String ldb, RsLastcommitEntry destLastcommit, int onlyXFirstRecords)
//	throws ManageException
//	{
//		ifClosedThrow();
//
//		if (destLastcommit == null)
//		{
//			_logger.info("Skipping dumpWsQueue, a null pointer was sent in for the destLastcommit.");
//			return new LinkedList();
//		}
//
//		int rsdbid = getRsDbIdForLogicalConnection(lsrv, ldb);
//
//		String cmd = null;
//		try
//		{
//			// sysadmin dump_queue, q_number, q_type, seg, blk, cnt [, RSSD | client]
//			//   seg - blk
//			//    - Setting seg to -1 starts with the first active segment in the queue
//			//    - Setting seg to -2 starts with the first segment in the queue,
//			//      including any inactive segments retained by setting a save interval
//			//    - Setting seg to -1 and blk to -1 starts with the first undeleted block in the queue.
//			//    - Setting seg to -1 and blk to -2 starts with the first unread block in the queue.
//			//   cnt
//			//    - Specifies the number of blocks to dump. This number can span multiple
//			//      segments. If cnt is set to -1, the end of the current segment is the last block
//			//      dumped. If it is set to -2, the end of the queue is the last block dumped.
//			//
//			// so: rsdbid, 1, -1, 1, -1 = Take In queue and first segment read if from start (blk=1) and read only info on this segment (only span 1MB)
//			//
//			// The seems to work better (if last committed row is at block 64, then this hops over to next segment)
//			// = Take "first active segment", "first 'unread' block (whatever that means), read to end of current segment (with the exception if last tran starts at segment 64, it jumps over to next segment"
//			// sysadmin dump_queue, 111, 1, -1, -2, -1, client
//			//
//			cmd = "sysadmin dump_queue, "+rsdbid+", 1, -1, -2, -1, client";
//
//			//--------------------------
//			// Output example
//			//--------------------------
//			// Q Number    Q Type      Segment     Block       Row         Message Len Orgn Siteid Orgn Time                      Orgn Qid                                                                   Orgn User                      Tran Name                      Local Qid                                                                  Status      Tranid                                                                                                                                                                                                                                             Logical Orgn Siteid Version     Command Len Seq No.     Command
//			// ----------- ----------- ----------- ----------- ----------- ----------- ----------- ---------                      --------                                                                   ---------                      ---------                      ---------                                                                  ----------- ------                                                                                                                                                                                                                                             ------------------- ----------- ----------- ----------- -------
//			//        107           1          73           1           0         276         108 Oct  9 2007  9:10PM            0x0000000000025eaf0000f4d200080000f4d20008000099c2015ce2280000000000000000 sa                             _upd                           0x000000000000000000000000000000000000000000000000000000000000004900010000           4 0x0000000000025eaf000847313530676f72616e3100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000                 107        1100          19           0 begin transaction
//			//        107           1          73           1           1         756         108 Jan  1 1900 12:00AM            0x0000000000025eaf0000f4d2000a0000f4d20008000099c2015ce2280000000000000000 NULL                           NULL                           0x000000000000000000000000000000000000000000000000000000000000004900010001     2097152 0x0000000000025eaf000847313530676f72616e3100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000                 107        1100         233           0 update dbo.rsm_heartbeat set lastbeat='20071009 21:10:15:746' where dbid=108 and server='G150' and dbname='goran1' and seconds=10 and seconds_left=10 and spid=25 and lastbeat='20071009 21:09:54:653' and rsm_server='mredMonitor'
//			//        107           1          73           1           2         204         108 Oct  9 2007  9:10PM            0x0000000000025eaf0000f4d2000b0000f4d20008000099c2015ce2280000000000000000 NULL                           NULL                           0x000000000000000000000000000000000000000000000000000000000000004900010002           1 0x0000000000025eaf000847313530676f72616e3100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000                 107        1100          20           0 commit transaction
//			//--------------------------
//
//			_rsMsgHandler.setPrefix("dumpWsQueue(): ");
//
//			Statement stmt = _conn.createStatement();
//			if ( getQueryTimeout() > 0 )
//				stmt.setQueryTimeout( getQueryTimeout() );
//			ResultSet rs = stmt.executeQuery(cmd);
//
//			// To be able to "sort" the transaction and append info to them...
//			// the key would be: tranId, and the object would be a linked list with Strings
//			Hashtable transTable = new Hashtable();
//			// The list below will keep the "order" of transactions.
//			List transOrderedList = new LinkedList();
//
//			RsLastcommitEntry usedToParseOqid = new RsLastcommitEntry();
//
//			while (rs.next())
//			{
//				int       qNumber           = rs.getInt      (1);
//				int       qType             = rs.getInt      (2);
//				int       segment           = rs.getInt      (3);
//				int       block             = rs.getInt      (4);
//				int       row               = rs.getInt      (5);
//				int       messageLen        = rs.getInt      (6);
//				int       orgnSiteid        = rs.getInt      (7);
//				Timestamp orgnTime          = rs.getTimestamp(8);
//				String    orgnQid           = rs.getString   (9);
//				String    orgnUser          = rs.getString   (10);
//				String    tranName          = rs.getString   (11);
//				String    localQid          = rs.getString   (12);
//				int       status            = rs.getInt      (13);
//				String    tranid            = rs.getString   (14);
//				int       logicalOrgnSiteid = rs.getInt      (15);
//				int       version           = rs.getInt      (16);
//				int       commandLen        = rs.getInt      (17);
//				int       seqNo             = rs.getInt      (18);
//				String    command           = rs.getString   (19);
//
//				boolean alreadyAppliedToDest = false;
//				if (destLastcommit._origin_qid.compareTo(orgnQid) > 0)
//				{
//					alreadyAppliedToDest = true;
//				}
//
//				if ( ! alreadyAppliedToDest )
//				{
//					List trans = (List)transTable.get(tranid);
//					if (trans == null)
//					{
//						trans = new LinkedList();
//						transTable.put(tranid, trans);
//						transOrderedList.add(trans);
//
//						// This if we want various parts of the OQID
//						usedToParseOqid._origin_qid = orgnQid;
//						usedToParseOqid.parseOriginQId();
//
//						// Add a first row, which would contain info about the transaction
//						String header
//							="originSiteId='"    + orgnSiteid
//							+"', originTime='"   + orgnTime
//							+"', originUser='"   + orgnUser
//							+"', TranName='"     + tranName
//							+"', generationId='" + usedToParseOqid._generationId
//							+"', logPage='"      + usedToParseOqid._logPage
//							+"', logRow='"       + usedToParseOqid._logRow
//							+"', LogTs='"        + usedToParseOqid._logTimestamp
//							+"'.";
//						trans.add(header);
//					}
//					if (seqNo == 0)
//						trans.add(command);
//					else
//					{
//						int listPos = trans.size()-1;
//						String cmdInList = (String) trans.get( listPos );
//						cmdInList += command;
//						trans.set(listPos, cmdInList);
//					}
//				}
//
//				if ( _logger.isDebugEnabled() )
//				{
//					_logger.debug("dumpWsQueue(alreadyAppliedToDest="+alreadyAppliedToDest+"): qNumber='"+qNumber+"', qType='"+qType+"', segment='"+segment
//						+"', block='"+block+"', row='"+row+"', messageLen='"+messageLen+"', orgnSiteid='"+orgnSiteid
//						+"', orgnTime='"+orgnTime+"', orgnQid='"+orgnQid+"', orgnUser='"+orgnUser+"', tranName='"+tranName
//						+"', localQid='"+localQid+"', status='"+status+"', tranid='"+tranid+"', logicalOrgnSiteid='"+logicalOrgnSiteid
//						+"', version='"+version+"', commandLen='"+commandLen+"', seqNo='"+seqNo+"', command='"+command+"'.");
//				}
//
//				// If negative number, log everything
//				if (onlyXFirstRecords > 0)
//				{
//					onlyXFirstRecords--;
//					if (onlyXFirstRecords == 0)
//						break;
//				}
//			}
//			rs.close();
//			stmt.close();
//
//			return transOrderedList;
//		}
//		catch (SQLException sqle)
//		{
//			checkForProblems(sqle, cmd);
//
//			if (sqle instanceof EedInfo)
//			{
//				EedInfo sybsqle = (EedInfo) sqle;
//				if (sybsqle.getSeverity() == 0)
//				{
//					String msg = "Got SQLException when executing '"+cmd+"' in RepServer '"+getSrvDbname()+"'. But Severity was 0, so I'm going to skip this. More info about the message";
//					_logger.debug(msg + sqlExceptionToString(sqle));
//
//					return null;
//				}
//			}
//
//			// if no queue entries was found... RepServer isn't sending us a Empty ResultSet
//			// so we need to look for: java.sql.SQLException: JZ0R2: No result set for this query.
//			if (sqle.getSQLState().equals("JZ0R2"))
//			{
//				_logger.info("When executing '"+cmd+"' we got a empty resultset, so the queue must be empty...");
//				return new LinkedList();
//			}
//			String msg = "Problems when executing '"+cmd+"' in RepServer '"+getSrvDbname()+"'.";
//			_logger.error(msg + sqlExceptionToString(sqle));
//			throw new ManageException(msg, sqle);
//		}
//	}
//
//	public void printDumpQueueList(List transOrderedList, int onlyXFirstTrans, boolean printHeader)
//	{
//		if (transOrderedList == null)
//			return;
//
//		int tranNum = 0;
//		for (Iterator transIter = transOrderedList.iterator(); transIter.hasNext();)
//		{
//			List element = (List) transIter.next();
//
//			tranNum++;
//			int numOfRecordsInTran = element.size()-1;
//			int rownum = 0;
//			for (Iterator sqlIter = element.iterator(); sqlIter.hasNext();)
//			{
//				String sqlCmd = (String) sqlIter.next();
//
//				sqlCmd = sqlCmd.replaceAll("\n", "");
//
//				if (rownum == 0)
//				{
//					if (printHeader)
//						_logger.info("Transaction("+tranNum+", has "+numOfRecordsInTran+" rows) info: "+sqlCmd);
//				}
//				else
//				{
//					_logger.info(rownum + "> " + sqlCmd);
//				}
//				rownum++;
//			}
//
//			// If negative number, log everything
//			if (onlyXFirstTrans > 0)
//			{
//				onlyXFirstTrans--;
//				if (onlyXFirstTrans == 0)
//					break;
//			}
//		}
//	}
	
//	/*---------------------------------------------------
//	** BEGIN: implementing ConnectionProvider
//	**---------------------------------------------------
//	*/
//	@Override
//	public Connection getNewConnection(String appname)
//	{
//		try
//		{
//			return AseConnectionFactory.getConnection(null, appname, null);
//		}
//		catch (Exception e)  // SQLException, ClassNotFoundException
//		{
//			_logger.error("Problems getting a new Connection", e);
//			return null;
//		}
//	}
//	@Override
//	public Connection getConnection()
//	{
//		return _conn;
//	}
//	/*---------------------------------------------------
//	** END: implementing ConnectionProvider
//	**---------------------------------------------------
//	*/






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

		pw.println("usage: dumpq [-U <user>] [-P <passwd>] [-S <server>] [-D <srv.dbname>]");
		pw.println("             [-h] [-v] [-x] <debugOptions> ");
		pw.println("  ");
		pw.println("options:");
		pw.println("  -h,--help                 Usage information.");
		pw.println("  -v,--version              Display "+Version.getAppName()+" and JVM Version.");
		pw.println("  -x,--debug <dbg1,dbg2>    Debug options: a comma separated string");
		pw.println("                            To get available option, do -x list");
		pw.println("  ");
		pw.println("  -U,--user <user>          RS Username when connecting to server.");
		pw.println("  -P,--passwd <passwd>      RS Password when connecting to server. null=noPasswd");
		pw.println("  -S,--server <server>      RepServer to connect to.");
		pw.println("  -D,--srvDbname <srv.db>   Server.Database to dump");
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
		options.addOption( "D", "srv.db",      true, "Srv.Database to dump" );

		return options;
	}


	//---------------------------------------------------
	// Command Line Parsing
	//---------------------------------------------------
	private static CommandLine parseCommandLine(String[] args, Options options)
	throws ParseException
	{
		// create the command line parser
		CommandLineParser parser = new DefaultParser();	
	
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
				new RsDumpQueueDialog(cmd);
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
