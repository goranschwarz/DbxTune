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
package com.dbxtune.tools.tailw;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.folding.Fold;
import org.fife.ui.rsyntaxtextarea.folding.FoldManager;
import org.fife.ui.rsyntaxtextarea.parser.AbstractParser;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice;
import org.fife.ui.rtextarea.GutterIconInfo;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.dbxtune.AppDir;
import com.dbxtune.Version;
import com.dbxtune.gui.ConnectionDialog;
import com.dbxtune.gui.swing.GLabel;
import com.dbxtune.gui.swing.GTextField;
import com.dbxtune.gui.swing.WaitForExecDialog;
import com.dbxtune.gui.swing.WaitForExecDialog.BgExecutor;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.ssh.RemoteFileSystemView;
import com.dbxtune.ssh.SshConnection;
import com.dbxtune.tools.NormalExitException;
import com.dbxtune.tools.tailw.LogFileFilterAndColorManager.FilterEntry;
import com.dbxtune.ui.rsyntaxtextarea.RSyntaxTextAreaX;
import com.dbxtune.ui.rsyntaxtextarea.RSyntaxUtilitiesX;
import com.dbxtune.utils.AseConnectionFactory;
import com.dbxtune.utils.AseConnectionUtils;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.FileTail;
import com.dbxtune.utils.JavaVersion;
import com.dbxtune.utils.Memory;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class LogTailWindow
//extends JDialog
extends JFrame
implements ActionListener, CaretListener, FocusListener, FileTail.TraceListener, Memory.MemoryListener
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long serialVersionUID = 1L;

	public enum FileType {ASE_LOG, REPSERVER_LOG, IQ_LOG, UNKNOWN_LOG}

	public final static String APP_NAME              = "tailw";
	public static final String TAIL_CONFIG_FILE_NAME = System.getProperty("TAIL_CONFIG_FILE_NAME", "conf"+File.separatorChar+"tailw.save.properties");

	
	private boolean            _startTailAfterSartup   = false;
                               
	private FileType           _fileType               = FileType.UNKNOWN_LOG;
	private DbxConnection      _conn                   = null;
	private String             _servername             = null;
	private String             _srvVersionStr          = null;
//	private String             _tailFileName           = null;
                               
	private FileTail           _fileTail               = null;
//	private int                _initialLinesInTail     = 50;
                               
	private JPanel             _topPanel               = null;
	private JLabel             _warning_lbl            = new JLabel("Choose 'SSH connection' or 'remote mount'.");
	private JLabel             _filterColorSchema_lbl  = new JLabel("Filter and Color Schema");
//	private JComboBox<String>  _filterColorSchema_cbx  = new JComboBox<String>(new String[] {"Not Yet Implemented"});
	private JComboBox<String>  _filterColorSchema_cbx  = new JComboBox<String>(LogFileFilterAndColorManager.getInstance().getFilterGroups().toArray(new String[0]));
	private JButton            _startTail_but          = new JButton("Start Tail");
	private JButton            _stopTail_but           = new JButton("Stop Tail");
	private JButton            _serverNameRemove_but   = new JButton("Remove");
	private JLabel             _serverName_lbl         = new JLabel("Server/Label Name");
	private JComboBox<String>  _serverName_cbx         = new JComboBox<String>();
	private JLabel             _logFilename_lbl        = new JLabel("Name of the Log File");
	private JTextField         _logFilename_txt        = new JTextField("");
	private JButton            _logFilename_but        = new JButton("...");
	private JCheckBox          _tailNewRecordsTop_chk  = new JCheckBox("Move to last row when input is received", true);
	private JCheckBox          _tailNewRecordsBot_chk  = new JCheckBox("Move to last row when input is received", true);

	private JLabel             _tailSize_lbl           = new JLabel("Start at line from end");
	private SpinnerNumberModel _tailSize_spm           = new SpinnerNumberModel(DEFAULT_TAIL_SIZE, 10, 99999, 10);
	private JSpinner           _tailSize_sp            = new JSpinner(_tailSize_spm);
	private JCheckBox          _tailFromStart_cbx      = new JCheckBox("Start at begining of file", DEFAULT_TAIL_FROM_START);
	
	
	private String[]           _accessTypeArr          = new String[] {"Choose a method", "SSH Access", "Direct/Local Access"};
	private JPanel             _accessTypePanel        = null;
	private JLabel             _accessType_lbl         = new JLabel("Log File Access");
	private JComboBox<String>  _accessType_cbx         = new JComboBox<String>(_accessTypeArr);
	private static final int ACCESS_TYPE_NOT_SELECTED  = 0;
	private static final int ACCESS_TYPE_SSH           = 1;
	private static final int ACCESS_TYPE_LOCAL         = 2;

	private JPanel             _sshPanel               = null;
	private SshConnection      _sshConn                = null;
//	private JCheckBox          _sshConnect_chk         = new JCheckBox("SSH Connection", true);
	private JLabel             _sshUsername_lbl        = new JLabel("Username");
	private JTextField         _sshUsername_txt        = new JTextField("");
	private JLabel             _sshPassword_lbl        = new JLabel("Password");
	private JTextField         _sshPassword_txt        = new JPasswordField("");
	private JCheckBox          _sshPassword_chk        = new JCheckBox("Save Password", true);
	private JLabel             _sshHostname_lbl        = new JLabel("Hostname");
	private JTextField         _sshHostname_txt        = new JTextField("");
	private JLabel             _sshPort_lbl            = new JLabel("Port num");
	private JTextField         _sshPort_txt            = new JTextField("22");
	private JLabel             _sshKeyFile_lbl         = new JLabel("Key File");
	private JTextField         _sshKeyFile_txt         = new JTextField("");
	private JButton            _sshKeyFile_but         = new JButton("...");
	private GLabel             _sshTailOsCmd_lbl       = new GLabel("Tail Cmd");
	private GTextField         _sshTailOsCmd_txt       = new GTextField("");
	                           
	private JPanel             _logTailPanel           = null;
	private RSyntaxTextAreaX   _logTail_txt            = new RSyntaxTextAreaX();
	private RTextScrollPane    _logTail_scroll         = new RTextScrollPane(_logTail_txt);
	private ErrorStrip         _logErrStrip            = new ErrorStrip(_logTail_txt);

	private boolean            _discardMessages        = Configuration.getCombinedConfiguration().getBooleanProperty("LogTailWindow.messages.discard",      true);
	private boolean            _foldDiscardedMessages  = Configuration.getCombinedConfiguration().getBooleanProperty("LogTailWindow.messages.discard.fold", true);

	private LogParser               _logParser          = null;
	private DefaultParseResult      _parserResult       = null;
	private ArrayList<TrackingIcon> _addedTrackingIcons = new ArrayList<TrackingIcon>();

	
//	private static final String  PROPKEY_TAIL_SIZE       = "LogTail.size";
	private static final int     DEFAULT_TAIL_SIZE       = 100;
	
//	private static final String  PROPKEY_TAIL_FROM_START = "LogTail.start_at_begining_of_file";
	private static final boolean DEFAULT_TAIL_FROM_START = false;

	public LogTailWindow()
	{
		super();
	}

	/**
	 * Get servername, srvVersion and logFileName from the server
	 * 
	 * @param filetype type of passed connection
	 * @param conn 
	 */
	public LogTailWindow(DbxConnection conn)
	{
		super();
		init(FileType.UNKNOWN_LOG, conn, null, null, null);
	}

	/**
	 * 
	 * @param filetype
	 * @param servername
	 * @param srvVersionStr
	 * @param initialFile
	 */
	public LogTailWindow(FileType filetype, String servername, String srvVersionStr, String initialFile)
	{
		super();
		init(filetype, null, servername, srvVersionStr, initialFile);
	}

	/**
	 * Constructor for CommandLine parameters
	 * @param cmd
	 * @throws Exception
	 */
	public LogTailWindow(final CommandLine cmd)
	throws Exception
	{
		Version.setAppName(APP_NAME);

		// Set Log4j Log Level
//		Configurator.setRootLevel(Level.TRACE);
		
//		// Create store dir if it did not exists.
//		File appStoreDir = new File(AppDir.getAppStoreDir());
//		if ( ! appStoreDir.exists() )
//		{
//			if (appStoreDir.mkdir())
//				System.out.println("Creating directory '"+appStoreDir+"' to hold various files for "+Version.getAppName());
//		}

		// -----------------------------------------------------------------
		// CHECK/SETUP information from the CommandLine switches
		// -----------------------------------------------------------------
//		final String CONFIG_FILE_NAME      = System.getProperty("CONFIG_FILE_NAME",      "dbxtune.properties");
//		final String USER_CONFIG_FILE_NAME = System.getProperty("USER_CONFIG_FILE_NAME", "dbxtune.user.properties");
//		final String TMP_CONFIG_FILE_NAME  = System.getProperty("TMP_CONFIG_FILE_NAME",  "sqlw.save.properties");
//		final String TAILW_HOME            = System.getProperty("TAILW_HOME");
		
//		String defaultPropsFile     = (TAILW_HOME              != null) ? TAILW_HOME              + File.separator + CONFIG_FILE_NAME      : CONFIG_FILE_NAME;
//		String defaultUserPropsFile = (AppDir.getAppStoreDir() != null) ? AppDir.getAppStoreDir() + File.separator + USER_CONFIG_FILE_NAME : USER_CONFIG_FILE_NAME;
//		String defaultTmpPropsFile  = (AppDir.getAppStoreDir() != null) ? AppDir.getAppStoreDir() + File.separator + TMP_CONFIG_FILE_NAME  : TMP_CONFIG_FILE_NAME;
		String defaultTailPropsFile = LogTailWindow.getDefaultPropFile();

//		// Compose MAIN CONFIG file (first USER_HOME then DBXTUNE_HOME)
//		String filename = AppDir.getAppStoreDir() + File.separator + CONFIG_FILE_NAME;
//		if ( (new File(filename)).exists() )
//			defaultPropsFile = filename;

//		String propFile        = cmd.getOptionValue("config",     defaultPropsFile);
//		String userPropFile    = cmd.getOptionValue("userConfig", defaultUserPropsFile);
//		String tmpPropFile     = cmd.getOptionValue("tmpConfig",  defaultTmpPropsFile);
		String tailPropFile    = cmd.getOptionValue("tailConfig", defaultTailPropsFile);

		// Check if the configuration file exists
//		if ( ! (new File(propFile)).exists() )
//			throw new FileNotFoundException("The configuration file '"+propFile+"' doesn't exists.");

		// -----------------------------------------------------------------
		// CHECK JAVA JVM VERSION
		// -----------------------------------------------------------------
		int javaVersionInt = JavaVersion.getVersion();
		if (   javaVersionInt != JavaVersion.VERSION_NOTFOUND 
		    && javaVersionInt <  JavaVersion.VERSION_7
		   )
		{
			System.out.println("");
			System.out.println("===============================================================");
			System.out.println(" "+Version.getAppName()+" needs a runtime Java 7 or higher.");
			System.out.println(" java.version = " + System.getProperty("java.version"));
			System.out.println(" which is parsed into the number: " + JavaVersion.getVersion());
			System.out.println("---------------------------------------------------------------");
			System.out.println("");
			throw new Exception(Version.getAppName()+" needs a runtime Java 7 or higher.");
		}

		// The SAVE Properties for shared Tail
		Configuration tailSaveProps = new Configuration(tailPropFile);
		Configuration.setInstance(Configuration.TAIL_TEMP, tailSaveProps);

//		// The SAVE Properties...
//		Configuration appSaveProps = new Configuration(tmpPropFile);
//		Configuration.setInstance(Configuration.USER_TEMP, appSaveProps);
//
//		// Get the USER properties that could override CONF
//		Configuration appUserProps = new Configuration(userPropFile);
//		Configuration.setInstance(Configuration.USER_CONF, appUserProps);
//
//		// Get the "OTHER" properties that has to do with LOGGING etc...
//		Configuration appProps = new Configuration(propFile);
//		Configuration.setInstance(Configuration.SYSTEM_CONF, appProps);
//
//		// Set the Configuration search order when using the: Configuration.getCombinedConfiguration()
//		Configuration.setSearchOrder(
//			Configuration.TAIL_TEMP,    // First
//			Configuration.USER_TEMP,    // Second
//			Configuration.USER_CONF,    // Third
//			Configuration.SYSTEM_CONF); // Forth

		// Set the Configuration search order when using the: Configuration.getCombinedConfiguration()
		Configuration.setSearchOrder(Configuration.TAIL_TEMP);

		//---------------------------------------------------------------
		// OK, lets get ASE user/passwd/server/dbname
		//---------------------------------------------------------------
		String tailLabel   = null;
		String tailFile    = null;
		boolean startTail  = false;

		String tdsUsername  = System.getProperty("user.name"); 
		String tdsPassword  = null;
		String tdsServer    = null;

		String sshUsername = System.getProperty("user.name");
		String sshPassword = null;
		String sshHostname = null;
		String sshKeyFile  = null;

		if (cmd.hasOption('l'))	tailLabel    = cmd.getOptionValue('l');
		if (cmd.hasOption('f'))	tailFile     = cmd.getOptionValue('f');
		if (cmd.hasOption('a'))	startTail    = true;

		if (cmd.hasOption('U'))	tdsUsername  = cmd.getOptionValue('U');
		if (cmd.hasOption('P'))	tdsPassword  = cmd.getOptionValue('P');
		if (cmd.hasOption('S'))	tdsServer    = cmd.getOptionValue('S');

		if (cmd.hasOption('u'))	sshUsername  = cmd.getOptionValue('u');
		if (cmd.hasOption('p'))	sshPassword  = cmd.getOptionValue('p');
		if (cmd.hasOption('s'))	sshHostname  = cmd.getOptionValue('s');
		if (cmd.hasOption('k'))	sshKeyFile   = cmd.getOptionValue('k');

//		System.setProperty("Logging.print.noDefaultLoggerMessage", "false");
//		Logging.init("tailw.", propFile);
		
    	try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

//		// Do a dummy encryption, this will hopefully speedup, so that the connection dialog wont hang for a long time during initialization
//		long initStartTime=System.currentTimeMillis();
//		Encrypter propEncrypter = new Encrypter("someDummyStringToInitialize");
//		String encrypedValue = propEncrypter.encrypt("TheDummyValueToEncrypt... this is just a dummy string...");
//		propEncrypter.decrypt(encrypedValue); // Don't care about the result...
//		_logger.info("Initializing 'encrypt/decrypt' package took: " + (System.currentTimeMillis() - initStartTime) + " ms.");
//
//		String hostPortStr = "";
//		if (aseServer.indexOf(":") == -1)
//			hostPortStr = AseConnectionFactory.getIHostPortStr(aseServer);
//		else
//			hostPortStr = aseServer;

		// Try make an initial connection...
    	DbxConnection conn = null;
		if ( StringUtil.hasValue(tdsServer) )
		{
			// If -P was not passed: try to get any saved password 
//			if (!cmd.hasOption('P') && tdsPassword == null)
//				tdsPassword = ConnectionDialog.getPasswordForServer(tdsServer);

			_logger.info("Connecting as user '"+tdsUsername+"' to server='"+tdsServer+"'. Which is located on '"+tdsServer+"'.");
			try
			{
				Properties props = new Properties();
				props.put("CHARSET", "iso_1");
				
				
				String hostPortStr = tdsServer;
				if (hostPortStr.indexOf(":") < 0) // no host:port, go and get the hst:port from the interfaces file
					hostPortStr = AseConnectionFactory.getIHostPortStr(hostPortStr);

				
				Connection jdbcConn = AseConnectionFactory.getConnection(hostPortStr, null, tdsUsername, tdsPassword, APP_NAME, Version.getVersionStr(), null, props, null);
				conn = DbxConnection.createDbxConnection(jdbcConn);
			}
			catch (SQLException e)
			{
				_logger.error("Problems connecting: " + AseConnectionUtils.sqlExceptionToString(e));
				//throw e;
			}
		}

		// Create a TAIL component.
		LogTailWindow tailw = new LogTailWindow();
		tailw.init(FileType.UNKNOWN_LOG, conn, tdsServer, null, tailFile);

		// Load properties for a LABEL if we got a label
		if (StringUtil.hasValue(tailLabel))
			tailw.setActiveLabel(tailLabel, true);

		
		// Override options from the command line with the one found in the label
		//------------------------

		// Filename
		if (StringUtil.hasValue(tailFile))    _logFilename_txt.setText(tailFile);
		
		// SSH Properties
		if (StringUtil.hasValue(sshUsername)) _sshUsername_txt.setText(sshUsername);
		if (StringUtil.hasValue(sshPassword)) _sshPassword_txt.setText(sshPassword);
		if (StringUtil.hasValue(sshHostname)) _sshHostname_txt.setText(sshHostname);
		if (StringUtil.hasValue(sshKeyFile))  _sshKeyFile_txt .setText(sshKeyFile);

		// Start the tail, after the window is visible
		tailw.setStartTailAfterStartup(startTail);

		// If no input params, restore last used label settings 
		if ( ! hasValidInputParams(cmd) )
		{
			Configuration conf = Configuration.getCombinedConfiguration();
			String lastUsed = conf.getProperty("LogTail.serverList.active");

			if (StringUtil.hasValue(lastUsed))
				tailw.setActiveLabel(lastUsed, true);
		}
		
		// Now open the window
		tailw.setVisible(true);
	}

	//------------------------------------------------------------
	//------------------------------------------------------------
	//------------------------------------------------------------
	// GUI initialization code
	//------------------------------------------------------------
	//------------------------------------------------------------
	//------------------------------------------------------------
	private void init(FileType filetype, DbxConnection conn, String servername, String srvVersionStr, String initialFile)
	{
		setTitle("Tail"); // Set window title
		
		// Set the icon, if we "just" do setIconImage() on the JDialog
		// it will not be the "correct" icon in the Alt-Tab list on Windows
		// So we need to grab the owner, and set that since the icon is grabbed from the owner...
		ImageIcon icon16 = SwingUtils.readImageIcon(Version.class, "images/log_trace_tool.png");
		ImageIcon icon32 = SwingUtils.readImageIcon(Version.class, "images/log_trace_tool_32.png");
		if (icon16 != null || icon32 != null)
		{
			ArrayList<Image> iconList = new ArrayList<Image>();
			if (icon16 != null) iconList.add(icon16.getImage());
			if (icon32 != null) iconList.add(icon32.getImage());

			Object owner = getOwner();
			if (owner != null && owner instanceof Frame)
				((Frame)owner).setIconImages(iconList);
			else
				setIconImages(iconList);
		}

		_fileType      = filetype;
		_conn          = conn;
		_servername    = (StringUtil.isNullOrBlank(servername) ? "UNKNOWN" : servername);
		_srvVersionStr = srvVersionStr; 
		_logFilename_txt.setText(initialFile);

		// Set current username as SSH username, will be overwritten if one found in the configuration
		_sshUsername_txt.setText(System.getProperty("user.name"));
		
		if (_conn != null && AseConnectionUtils.isConnectionOk(_conn, true, this))
		{
			_logger.info("Getting logfile name information from the database server '"+_servername+"'.");

			// Try to get what hostname to do SSH to
			// NOTE: this is probably not the best way, but at least a start
			String srvHostName = "";
			String[] sa = AseConnectionFactory.getHostArr();
			if (sa.length > 0)
				srvHostName = sa[0];

			// Set current hostname (last time we connected) as SSH hostname, will be overwritten if one found in the configuration
			_sshHostname_txt.setText(srvHostName);

			try
			{
				String dbProduct = ConnectionDialog.getDatabaseProductName(_conn);
				_servername      = ConnectionDialog.getDatabaseServerName(_conn);
				_srvVersionStr   = ConnectionDialog.getDatabaseProductVersion(_conn);
				String logName   = DbUtils         .getServerLogFileName(dbProduct, _conn);

				_logFilename_txt.setText(logName);
				
				if      (DbUtils.isProductName(dbProduct, DbUtils.DB_PROD_NAME_SYBASE_ASE)) _fileType = FileType.ASE_LOG;
				else if (DbUtils.isProductName(dbProduct, DbUtils.DB_PROD_NAME_SYBASE_RS))  _fileType = FileType.REPSERVER_LOG;
				else if (DbUtils.isProductName(dbProduct, DbUtils.DB_PROD_NAME_SYBASE_IQ))  _fileType = FileType.IQ_LOG;
				else
				{
					// FIXME: unknown type and connection...
				}

				_logger.debug("DB Server Info: dbProduct     = '" + dbProduct      + "'.");
				_logger.debug("DB Server Info: servername    = '" + _servername    + "'.");
				_logger.debug("DB Server Info: srvVersionStr = '" + _srvVersionStr + "'.");
				_logger.debug("DB Server Info: logFilename   = '" + _logFilename_txt.getText() + "'.");
			}
			catch (SQLException e)
			{
			}
		}

		// Set STYLE of the SYNTAX in the output/tail view
		_logTail_txt.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
//		_logTail_txt.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
//		FoldParserManager.get().addFoldParserMapping(SyntaxConstants.SYNTAX_STYLE_SQL, new CurlyFoldParser());
		if (_foldDiscardedMessages)
		{
			_logTail_txt.setCodeFoldingEnabled(true);
			_logTail_scroll.setFoldIndicatorEnabled(true);
			
//			// Collapse by default
//			FoldCollapser collapser = new FoldCollapser() 
//			{
//				@Override
//				public boolean getShouldCollapse(Fold fold) {
//					return true;
//				}
//			};
//			collapser.collapseFolds(_logTail_txt.getFoldManager());
		}

		if ( _fileType == FileType.ASE_LOG)
		{
//			_logTail_txt.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL_LOG); // FIXME: create a ASE LOG SYNTAX
		}
		else if ( _fileType == FileType.REPSERVER_LOG)
		{
//			_logTail_txt.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_RCL_LOG); // FIXME: create a RS LOG SYNTAX
		}
		else if ( _fileType == FileType.IQ_LOG)
		{
//			_logTail_txt.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_IQ_LOG); // FIXME: create a RS LOG SYNTAX
		}
		else
		{
//			_logTail_txt.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
//			_logTail_txt.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
		}


		setLayout( new BorderLayout() );
		
		loadProps();

		_topPanel         = createTopPanel();
		_logTailPanel     = createLogTailPanel();

		setAccessVisiblePanels();

		
		add(_topPanel,     BorderLayout.NORTH);
		add(_logTailPanel, BorderLayout.CENTER);

		pack();
		getSavedWindowProps();

		validateInput();
		setConnected(false);

//System.out.println("XXX: _logTail_txt.getSyntaxEditingStyle() = " + _logTail_txt.getSyntaxEditingStyle());

		// ADD this from the out of memory listener (Note only works if it has been installed)
		Memory.addMemoryListener(this);
		if ( ! Memory.isRunning() )
			Memory.start();

		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				stopTail();
				destroy();
			}
		});
	}


	/** call this when window is closing */
	private void destroy()
	{
		// Remove this from the out of memory listener
		Memory.removeMemoryListener(this);
		
		// Memory doesn't seems to be released, I don't know why, so lets try to reset some data structures
		_logTail_txt    = null;
		_logTail_scroll = null;

		dispose();
	}
	private JPanel createTopPanel()
	{
		JPanel panel = SwingUtils.createPanel("Top Panel", false);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));

		_warning_lbl  .setToolTipText("Why we can't start a Fail Tail at this time.");
		_warning_lbl  .setForeground(Color.RED);
		_warning_lbl  .setHorizontalAlignment(SwingConstants.RIGHT);
		_warning_lbl  .setVerticalAlignment(SwingConstants.BOTTOM);

		_filterColorSchema_lbl.setToolTipText("<html>Apply a Color and Filter functionality for this session.<br><br>You can edit the file '"+LogFileFilterAndColorManager.DEFAULT_filename+"', or copy it from '$DBXTUNE_HOME/lib/dbxtune.jar:resources/LogFileFilters.xml'</html>");
		_filterColorSchema_cbx.setToolTipText(_filterColorSchema_lbl.getToolTipText());
		_startTail_but        .setToolTipText("Start doing Tail on the log");
		_stopTail_but         .setToolTipText("Stop doing Tail on the log");

		_sshPanel         = createSshPanel();
		_accessTypePanel  = createAccessPanel();

		panel.add(_accessTypePanel,       "push, grow,             hidemode 2");
		panel.add(_sshPanel,              "push, grow, span, wrap, hidemode 2");

//		panel.add(new JLabel(""),         "wrap, push"); // dummy JLabel just to do "push", it will disappear if
		panel.add(_tailNewRecordsBot_chk, "span 4, split, hidemode 2");
		panel.add(_warning_lbl,           "growx");
		panel.add(_filterColorSchema_lbl, "");
		panel.add(_filterColorSchema_cbx, "");
		panel.add(_startTail_but,         "gap 10 10 0 10, hidemode 3");   // gap left [right] [top] [bottom]
		panel.add(_stopTail_but,          "gap 10 10 0 10, hidemode 3");   // gap left [right] [top] [bottom]

		// disable input to some fields
		_stopTail_but         .setVisible(false);
		_tailNewRecordsBot_chk.setVisible(false);

		_filterColorSchema_cbx.addActionListener(this);
		_tailNewRecordsBot_chk.addActionListener(this);
		_startTail_but        .addActionListener(this);
		_stopTail_but         .addActionListener(this);

		// Focus action listener

		return panel;
	}
	private JPanel createAccessPanel()
	{
		JPanel panel = SwingUtils.createPanel("Tail File Access", true);
//		panel.setLayout(new MigLayout("insets 0 0 0 0"));
		panel.setLayout(new MigLayout());

		_serverName_lbl      .setToolTipText("<html>This is a <b><i>name</i></b> which all other settings will be saved as.<br>Simply choose a previously saved server/label to load settings for that saved session.</html>");
		_serverName_cbx      .setToolTipText(_serverName_lbl.getToolTipText());
		_serverNameRemove_but.setToolTipText("Removes this entry from the list");

		_accessType_lbl      .setToolTipText("The File needs to be accessed in some way. You can choose Direct/Local access or SSH access or Simply NOT to Tail the server trace file, just control the AppTrace and read the file yourself.");
		_accessType_cbx      .setToolTipText(_accessType_lbl.getToolTipText());

		_logFilename_lbl      .setToolTipText("This is the filename that will be 'tailed'.");
		_logFilename_txt      .setToolTipText(_logFilename_lbl.getToolTipText());
		_logFilename_but      .setToolTipText("Open a File Chooser and pick a file");

		_tailSize_lbl        .setToolTipText("<html>How many old entries from the file should we include<br> <b>Note</b>: For Linux/Unix systems the max value is 999.</html>");
		_tailSize_sp         .setToolTipText(_tailSize_lbl.getToolTipText());
		_tailFromStart_cbx   .setToolTipText("<html>Start from the beginning of the file...</html>");
		

		panel.add(_serverName_lbl,       "");
		panel.add(_serverName_cbx,       "span 2, split, pushx, growx");
		panel.add(_serverNameRemove_but, "wrap");

		panel.add(_accessType_lbl,       "");
		panel.add(_accessType_cbx,       "pushx, growx, wrap");

		panel.add(_logFilename_lbl,      "");
		panel.add(_logFilename_txt,      "span 2, split, pushx, growx");
		panel.add(_logFilename_but,      "wrap");

		panel.add(_tailSize_lbl,         "");
		panel.add(_tailSize_sp,          "split");
		panel.add(_tailFromStart_cbx,    "gapleft 20, wrap");

		panel.add(_tailNewRecordsTop_chk,"span, wrap");

		_serverName_cbx.setEditable(true);
		_serverName_cbx.setMaximumRowCount(50);
		
		// Add action listener
		_serverNameRemove_but .addActionListener(this);
		_accessType_cbx       .addActionListener(this);

		_logFilename_txt      .addActionListener(this);
		_logFilename_but      .addActionListener(this);
		_serverName_cbx       .addActionListener(this);
		_tailFromStart_cbx    .addActionListener(this);
		_tailNewRecordsTop_chk.addActionListener(this);

		// Focus action listener

		// Initial visibility
		_tailSize_sp.setEnabled( ! _tailFromStart_cbx.isSelected() );

		return panel;
	}

	private JPanel createSshPanel()
	{
		JPanel panel = SwingUtils.createPanel("SSH Information", true);
//		panel.setLayout(new MigLayout("insets 0 0 0 0"));
		panel.setLayout(new MigLayout());

		// Tooltip
		panel            .setToolTipText("Secure Shell (SSH) is used to access the file on the host where the ASE is located.");
//		_sshConnect_chk  .setToolTipText("ASE is located on a unix/linux host that has SSH Secure Shell");
		_sshUsername_lbl .setToolTipText("<html>User name that can access the Application Log on the Server side.<br><br><b>Note:</b> The user needs to read the Log</html>");
		_sshUsername_txt .setToolTipText("<html>User name that can access the Application Log on the Server side.<br><br><b>Note:</b> The user needs to read the Log</html>");
		_sshPassword_lbl .setToolTipText("<html>"
		                                  + "Password to the User that can access the Log on the Server side<br>"
		                                  + "<br>"
		                                  + "If SSH Authentication model is 'publickey' and you have a password for the <i>private key file</i>, then type this password here.<br>"
		                                  + "If the <i>private key file</i> does <b>not</b> have a password, just type <i>anything here</i> so the button is enabled.<br>"
		                                  + "</html>");
		_sshPassword_txt .setToolTipText(_sshPassword_lbl.getToolTipText());
		_sshPassword_chk .setToolTipText("Save the password in the configuration file, and YES it's encrypted.");
		_sshHostname_lbl .setToolTipText("Host name where the Log is located");
		_sshHostname_txt .setToolTipText("Host name where the Log is located");
		_sshKeyFile_lbl  .setToolTipText("SSH Private Key File... where the Log is located");
		_sshKeyFile_txt  .setToolTipText("SSH Private Key File... where the Log is located");
		_sshPort_lbl     .setToolTipText("Port number of the SSH Server, normally 22.");
		_sshPort_txt     .setToolTipText("Port number of the SSH Server, normally 22.");
//		_sshTailOsCmd_lbl.setToolTipText("<html>Execute OS command(s) before we start to tail the file.<br>This is if you need to do <b><code>su username</code></b> or <b><code>sudo cmd</code></b> or similar stuff to be able to read the file.</html>");
		String tooltip = "<html>"
				+ "The normal OS Command to view log file changes are: <code>tail -n 999 -f /var/logs/somefile</code><br>"
				+ "Note: <i>command may vary basen on the Operating System</i><br>"
				+ "<br>"
				+ "With this you can change how you access the logfile.<br>"
				+ "If you need to change account/user due to improper authorizations, this is where you do it.<br>"
				+ "<br>"
				+ "Here are a couple of examples:"
				+ "<ul>"
				+ "    <li>Do tail as another user, using <code>sudo</code>.<br>"
				+ "    <code>echo '${password}' | sudo -p '' -S -u sybase ${cmd}</code></li>"
				+ ""
				+ "    <li>Do tail as another user, using <code>su</code><br>Note: this only works on some platforms.<br>"
				+ "    <code>su - sybase -c '${cmd}' &lt;&lt;&lt; '$password' </code></li>"
				+ ""
				+ "    <li>Using a <i>jump</i> server, where you start another SSH Session.<br>Note: this needs keygen files, due to password.<br>Note2: Test this before you use it.<br>"
				+ "    <code>ssh sybase@hostname '${cmd}'</code></li>"
				+ ""
				+ "    <li>Execute your own shell script that does whatever you need to do...<br>"
				+ "    <code>yourShellScript -a '${password}' -b '${filename}'</code></li>"
				+ ""
				+ "</ul>"
				+ "The below variables that will be substituted before execution"
				+ "<ul>"
				+ "    <li><code>${password}</code> - The password in the above password field</li>"
				+ "    <li><code>${filename}</code> - The filename which we want to look at</li>"
				+ "    <li><code>${cmd}</code>      - The tail command we would normally use, example: <code>tail -n 999 -f /var/logs/somefile</code></li>"
				+ "</ul>"
				+ "</html>";
		_sshTailOsCmd_lbl.setToolTipText(tooltip);
		_sshTailOsCmd_txt.setToolTipText(tooltip);

//		panel.add(_sshConnect_chk,     "span, wrap");

		panel.add(_sshUsername_lbl,    "");
		panel.add(_sshUsername_txt,    "growx, pushx, wrap");

		panel.add(_sshPassword_lbl,    "");
		panel.add(_sshPassword_txt,    "growx, pushx, wrap");
		panel.add(_sshPassword_chk,    "skip, wrap 10");

		panel.add(_sshHostname_lbl,    "");
		panel.add(_sshHostname_txt,    "growx, pushx, wrap");

		panel.add(_sshPort_lbl,        "");
		panel.add(_sshPort_txt,        "growx, pushx, wrap");

		panel.add(_sshKeyFile_lbl,     "");
		panel.add(_sshKeyFile_txt,     "split, growx, pushx");
		panel.add(_sshKeyFile_but,     "wrap");

		panel.add(_sshTailOsCmd_lbl,   "");
		panel.add(_sshTailOsCmd_txt,   "growx, pushx, wrap");

		
		// disable input to some fields

		// for validation
		_sshUsername_txt.addActionListener(this);
		_sshPassword_txt.addActionListener(this);
		_sshPassword_chk.addActionListener(this);
		_sshHostname_txt.addActionListener(this);
		_sshPort_txt    .addActionListener(this);
		_sshKeyFile_txt .addActionListener(this);
		_sshKeyFile_but .addActionListener(this);

		// Focus action listener
		_sshUsername_txt.addFocusListener(this);
		_sshPassword_txt.addFocusListener(this);
		_sshHostname_txt.addFocusListener(this);
		_sshPort_txt    .addFocusListener(this);
		_sshKeyFile_txt .addFocusListener(this);
		
		// Caret listener
		_sshUsername_txt.addCaretListener(this);
		_sshPassword_txt.addCaretListener(this);
		_sshHostname_txt.addCaretListener(this);
		_sshPort_txt    .addCaretListener(this);
		_sshKeyFile_txt .addCaretListener(this);

		return panel;
	}

	private JPanel createLogTailPanel()
	{
		JPanel panel = SwingUtils.createPanel("Trace Command Log", false);
//		panel.setLayout(new MigLayout("insets 0 0 0 0"));
		panel.setLayout(new BorderLayout());

		// Tooltip
//		panel       .setToolTipText("Tail of the Log");
//		_logTail_txt.setToolTipText("Tail of the Log");
		
//		_logTail_txt.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL);

		// enable the "icon" area on the left side
		_logTail_scroll.setLineNumbersEnabled(true);
		_logTail_scroll.setIconRowHeaderEnabled(true);
		_logErrStrip.setVisible(true);
		_logErrStrip.setLevelThreshold(ParserNotice.Level.INFO); // Show everyting 

		// Parser
		_logParser = new LogParser();
		_parserResult = new DefaultParseResult(_logParser);
		_logTail_txt.addParser(_logParser);

//		panel.add(_logTail_scroll, "grow, push");
//		panel.add(_logErrStrip,    "wrap");
		panel.add(_logTail_scroll, BorderLayout.CENTER);
		panel.add(_logErrStrip,    BorderLayout.LINE_END);


		RSyntaxUtilitiesX.installRightClickMenuExtentions(_logTail_scroll, this);

		return panel;
	}
	
	private class LogParser
	extends AbstractParser
	{
		@Override 
		public ParseResult parse(RSyntaxDocument doc, String style)
		{
			// Check the list of TrackerIcons (icons at the Gutter), if some hasn't been added, add them
			for (TrackingIcon ti : _addedTrackingIcons)
			{
				if (ti._gii == null)
				{
					try
					{
						GutterIconInfo gii = _logTail_scroll.getGutter().addLineTrackingIcon(ti._line, ti._icon, ti._toolTip);
						ti._gii = gii;
					}
					catch (BadLocationException ignore) { /* ignore */ }
				}
			}
			
			return _parserResult;
		}
	}

	@Override
	public void setVisible(boolean visible) 
	{
		if (visible && _startTailAfterSartup)
		{
			SwingUtilities.invokeLater( new Runnable()
			{
				@Override
				public void run()
				{
					//startTail();
					_startTail_but.doClick();
				}
			});
		}
		super.setVisible(visible);
	}
	
	/**
	 * Set and load properties for a specific label
	 * @param tailLabel
	 */
	public void setActiveLabel(String labelName, boolean xxx)
	{
		_servername = labelName;
		addServerEntry(labelName, xxx);
		loadProps(labelName, xxx);
	}

	/*---------------------------------------------------
	** BEGIN: implementing: ActionListener, FocusListener, FileTail.TraceListener, Memory.MemoryListener
	**---------------------------------------------------
	*/	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();

		// COMBO: ACCESS TYPE
		if (_accessType_cbx.equals(source))
		{
			setAccessVisiblePanels();
		}
		
		// BUT: ASE START TRACE
		if (_startTail_but.equals(source))
		{
			startTail();
		}

		// BUT: ASE STOP TRACE
		if (_stopTail_but.equals(source))
		{
			stopTail();
		}
		
		// ComboBox: Filters
		if (_filterColorSchema_cbx.equals(source))
		{
			String filterSchema = StringUtil.getSelectedItemString(_filterColorSchema_cbx);
			LogFileFilterAndColorManager.getInstance().setFilterGroup(filterSchema);
			
			// Restart if already started.
			if (_fileTail != null)
			{
				stopTail();
				startTail();
			}
		}

		// COMBOBOX: SERVERNAMES
		if (_serverName_cbx.equals(source))
		{
			setActiveLabel( _serverName_cbx.getSelectedItem().toString(), false );
		}

		// BUT: SERVERNAME REMOVE
		if (_serverNameRemove_but.equals(source))
		{
			_serverName_cbx.removeItemAt( _serverName_cbx.getSelectedIndex() );
			_servername = _serverName_cbx.getSelectedItem().toString();
			loadProps(getServername(), false);
			saveProps();
		}
		
		// BUT: FILENAME ...
		if (_logFilename_but.equals(source))
		{
			String filename = _logFilename_txt.getText();

			int index = _accessType_cbx.getSelectedIndex();

			if (index == ACCESS_TYPE_SSH)
			{
//				SwingUtils.showInfoMessage(this, "Not yet supported", "Sorry: Pick a file from a Remote server is currently NOT possible.");
				
				SshConnection sshConn = sshConnect();
				if (sshConn != null)
				{
					try
					{
//						SshFileSystemView fsv = new SshFileSystemView(sshConn);
						RemoteFileSystemView fsv = new RemoteFileSystemView(sshConn);
						
						String hostPortLabel = _sshHostname_txt.getText() + ":" +_sshPort_txt.getText();

		    			JFileChooser fc = new JFileChooser(filename, fsv);
						fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
//						fc.setApproveButtonText("Choose");
						fc.setDialogTitle("SSH Remote Files at "+hostPortLabel+" (NOTE: This is NOT working in a good manner)");
//						fc.setAccessory( new JLabel("NOTE: This is NOT working in a good manner...") );
						
//						String str = _logFilename_txt.getText();
//						if (StringUtil.hasValue(str))
//							fc.setSelectedFile(new File(str));

		    			int returnVal = fc.showOpenDialog(this);
		    			if(returnVal == JFileChooser.APPROVE_OPTION)
		    			{
		    				// NOTE: well the FileChooser seems to return windows file path (if we run this on windows) so strip some stuff off
		    				String selectedFile = fc.getSelectedFile().getAbsolutePath();
		    				if (selectedFile.startsWith("c:") || selectedFile.startsWith("C:"))
		    					selectedFile = selectedFile.substring(2);
		    				selectedFile = selectedFile.replace('\\', '/');

		    				_logFilename_txt.setText( selectedFile );
		    			}
					}
					catch(Exception ex)
					{
						ex.printStackTrace();
					}
					sshConn.close();
				}
			}
			else
			{
    			JFileChooser fc = new JFileChooser(filename);
    			int returnVal = fc.showOpenDialog(this);
    			if(returnVal == JFileChooser.APPROVE_OPTION) 
    				_logFilename_txt.setText( fc.getSelectedFile().getAbsolutePath() );
			}

			saveProps();
		}
		
		// BUT: SSH KEY FILENAME ...
		if (_sshKeyFile_but.equals(source))
		{
			String dir = System.getProperty("user.home") + File.separatorChar + ".ssh";

			JFileChooser fc = new JFileChooser(dir);
			int returnVal = fc.showOpenDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION) 
			{
				String filename = fc.getSelectedFile().getAbsolutePath();
				_sshKeyFile_txt.setText(filename);
			}
		}

		
		// CHKBOX: keep both _tailNewRecords buttons in sync... (they should be the same button)
		if (_tailNewRecordsTop_chk.equals(source))
			_tailNewRecordsBot_chk.setSelected(_tailNewRecordsTop_chk.isSelected());

		// CHKBOX: keep both _tailNewRecords buttons in sync... (they should be the same button)
		if (_tailNewRecordsBot_chk.equals(source))
			_tailNewRecordsTop_chk.setSelected(_tailNewRecordsBot_chk.isSelected());

		// visibility
		_tailSize_sp.setEnabled( ! _tailFromStart_cbx.isSelected() );

		isConnected();   // enable/disable components
		validateInput(); // are we allowed to connect, or are we missing information
	}

//	@Override
//	public void newTraceRow(String row)
//	{
//		_logBuffer.append(row);
//		if ( ! row.endsWith("\n") )
//			_logBuffer.append("\n");
//
//		if ( _logBufferTimer.isRunning() )
//			_logBufferTimer.restart();
//		else
//			_logBufferTimer.start();
//	}
//	// BEGIN: Deferred processing of the entries
//	StringBuilder _logBuffer = new StringBuilder();
//	Timer         _logBufferTimer = new Timer(100, new ActionListener()
//	{
//		@Override
//		public void actionPerformed(ActionEvent e)
//		{
//			// Make a new buffer, and send the old buffer to be processed
//			synchronized (LogTailWindow.this)
//			{
//				StringBuilder sb = _logBuffer;
//				_logBuffer = new StringBuilder();
//
//				logBufferApply(sb);
//
//				_logBufferTimer.stop();
//			}
//		}
//	});
//	private void logBufferApply(StringBuilder logBuffer)
//	{
//		// Get the Filter "manager"
//		LogFileFilterAndColorManager filter = LogFileFilterAndColorManager.getInstance();
//
//		// Loop the input
//		try (BufferedReader reader = new BufferedReader(new StringReader(logBuffer.toString()))) 
//		{
//			// Keep discarded records in a List...
//			List<String> discardedRecords = new LinkedList<>();
//			
//			// Read first line... subsequential lines is read at the end...
//			String line = reader.readLine();
//			while (line != null) 
//			{
//				// This will holds records that we can add directly...
//				String lineToAdd = null;
//				
//				// Get "properties" for this row... It cab be a Warning/Error row... or it it should be discarded...
//				FilterEntry fe = filter.getFilterEntryForRow(line);
//				
//				// Check if this record should be displayed or not
//				boolean allowRecord = fe.isAllowed();
//				
//				// If we should NOT discard messages, then set it as ALLOWED
//				if (_discardMessages == false)
//					allowRecord = true;
//
//				if (allowRecord)
//				{
//					// CLOSE any discarded message group...
//					// and add all records to:
//					// - The tooltip gutter
//					// - And possibly: add the discarded records as a "folded group" in the editor...
//					if (discardedRecords.size() > 0)
//					{
//						handleDiscardedRecords(discardedRecords, _foldDiscardedMessages);
//						
//						// Start a new List to store discarded messages.
//						discardedRecords = new LinkedList<>();
//					}
//
//					// This line should soon be appended to the log
//					lineToAdd = line;
//				}
//				else
//				{
//					// If not displayed, maybe do it later... use the list as a counter of discarded records
//					discardedRecords.add(line);
//
//					lineToAdd = null;
//				}
//
//				// Here is where we ADD the record to the log, if it wasn't discarded.
//				// Also: get next line... we might need to concatenate next line to current if it's not probely "terminated"
//				line = reader.readLine();
//				if (lineToAdd != null)
//				{
//					_logTail_txt.append(lineToAdd);
//
//					// This is a special fix for Replication Server (should probably be done in a better way, possibly by the FilterEntry "manager")
//					// If next line is "'." then it means it's a RepServer Message that can be added on same line as this line...
//					// FIXME: Fix this as a Property for the FilterGroup...
//					if ("'.".equals(line))
//					{
//						_logTail_txt.append(line);
//						line = reader.readLine(); // And read next line
//					}
//
//					// Append a <newline>
//					_logTail_txt.append("\n");
//					
//					// possibly MARK the line as Warning/Error
//					if (fe.getIcon() != null)
//						addParserNotice(-1, fe.getIcon(), fe.getLevel(), lineToAdd);
//				}
//			} // end: looping the input
//
//			// Add possibly records that has been discarded
//			handleDiscardedRecords(discardedRecords, _foldDiscardedMessages);
//
//			// Should we position ourself "at the end"
//			if (_tailNewRecordsTop_chk.isSelected() || _tailNewRecordsBot_chk.isSelected())
//			{
//				_logTail_txt.setCaretPosition( _logTail_txt.getDocument().getLength() );
//			}
//		}
//		catch(Throwable t)
//		{
//			_logger.error("Problems adding text to the tail-output window", t);
//		}
//	}
	@Override
	public void newTraceRow(String row)
	{
		_logBufferList.add(row);

		if ( _logBufferTimer.isRunning() )
			_logBufferTimer.restart();
		else
			_logBufferTimer.start();
	}
	// BEGIN: Deferred processing of the entries
	LinkedList<String> _logBufferList  = new LinkedList<>();
	Timer              _logBufferTimer = new Timer(100, new ActionListener()
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			// Make a new buffer, and send the old buffer to be processed
			synchronized (LogTailWindow.this)
			{
				LinkedList<String> currentBufferList = _logBufferList;
				_logBufferList = new LinkedList<>();

				try
				{
					_logTail_txt.beginAtomicEdit();
    				logBufferApply(currentBufferList);
				}
				finally
				{
					_logTail_txt.endAtomicEdit();
				}

				_logBufferTimer.stop();
			}
		}
	});
	private void logBufferApply(LinkedList<String> logBuffer)
	{
		// Get the Filter "manager"
		LogFileFilterAndColorManager filter = LogFileFilterAndColorManager.getInstance();
		
		String currentFilterGroupName = filter.getCurrentFilterGroupName(); 

		// Keep discarded records in a List...
		List<String> discardedRecords = new LinkedList<>();
		
		// Loop the list of records
		for( int bufCnt=0; bufCnt<logBuffer.size(); bufCnt++ )
		{
			String line = logBuffer.get(bufCnt);

			// This will holds records that we can add directly...
			String lineToAdd = null;
			
			// Get "properties" for this row... It cab be a Warning/Error row... or it it should be discarded...
			FilterEntry fe = filter.getFilterEntryForRow(line);
			
			// Check if this record should be displayed or not
			boolean allowRecord = fe.isAllowed();
			
			// If we should NOT discard messages, then set it as ALLOWED
			if (_discardMessages == false)
				allowRecord = true;

			if (allowRecord)
			{
				// CLOSE any discarded message group...
				// and add all records to:
				// - The tooltip gutter
				// - And possibly: add the discarded records as a "folded group" in the editor...
				if (discardedRecords.size() > 0)
				{
					handleDiscardedRecords(discardedRecords, _foldDiscardedMessages);
					
					// Start a new List to store discarded messages.
					discardedRecords = new LinkedList<>();
				}

				// This line should soon be appended to the log
				lineToAdd = line;
			}
			else
			{
				// If not displayed, maybe do it later... use the list as a counter of discarded records
				discardedRecords.add(line);

				lineToAdd = null;
			}

			// Here is where we ADD the record to the log, if it wasn't discarded.
			if (lineToAdd != null)
			{
				_logTail_txt.append(lineToAdd);

				// This is a special fix for Replication Server (should probably be done in a better way, possibly by the FilterEntry "manager")
				// If next line is "'." then it means it's a RepServer Message that can be added on same line as this line...
				// FIXME: Fix this as a Property for the FilterGroup...
				if ("RepServer".equals(currentFilterGroupName))
				{
					// Also: get next line... we might need to concatenate next line to current if it's not probely "terminated"
					String nextLine = (bufCnt+1 < logBuffer.size()) ? logBuffer.get(bufCnt+1) : null;

					if ("'.".equals(nextLine))
					{
						_logTail_txt.append(nextLine);
						bufCnt++; // mark next line as "read"
					}
				}

				// Append a <newline>
				_logTail_txt.append("\n");
				
				// possibly MARK the line as Warning/Error
				if (fe.getIcon() != null)
					addParserNotice(-1, fe.getIcon(), fe.getLevel(), lineToAdd);
			}
		} // end: looping the input

		// Add possibly records that has been discarded
		handleDiscardedRecords(discardedRecords, _foldDiscardedMessages);

		// Should we position ourself "at the end"
		if (_tailNewRecordsTop_chk.isSelected() || _tailNewRecordsBot_chk.isSelected())
		{
			_logTail_txt.setCaretPosition( _logTail_txt.getDocument().getLength() );
		}
	}

	private void handleDiscardedRecords(List<String> discardedRecords, boolean foldMessages)
	{
		if (discardedRecords.isEmpty())
			return;

		StringBuilder discToLog = new StringBuilder();
		StringBuilder toToolTip = new StringBuilder("<html><code>");
		for (String disardedRow : discardedRecords)
		{
			discToLog.append(disardedRow).append("\n");
			toToolTip.append(disardedRow).append("<br>\n");
		}
		toToolTip.append("</code></html>");
		String toToolTipStr = toToolTip.toString();

		// append BEGIN to log
		if (foldMessages)
		{
			String discardStr = "{ // >>>>>>>>>>>> begin: Discarding " + discardedRecords.size() + " records from the log. >>>>>>>>>>>>\n";
			_logTail_txt.append(discardStr);
			addParserNotice(-1, LogFileFilterAndColorManager.FILTER_ROW_DISCARDED_ICON, LogFileFilterAndColorManager.Level.Discard, toToolTipStr);

			// add the "hidden" records
			_logTail_txt.append(discToLog.toString());
		}
		
		// append END to log
		String discardPrefix = foldMessages ? "} // <<<<<<<<<<<< end:" : "############";
		String discardStr    = discardPrefix + " Discarded "+discardedRecords.size()+" record from the log. <<<<<<<<<<<<\n";

		// Write "end" message and add a "notation" to the gutter
		_logTail_txt.append(discardStr);
		addParserNotice(-1, LogFileFilterAndColorManager.FILTER_ROW_DISCARDED_ICON, LogFileFilterAndColorManager.Level.Discard, toToolTipStr);

		// Fold the last message "group"
		if (foldMessages)
		{
			// We need to check for new fold sections...
			FoldManager foldManager = _logTail_txt.getFoldManager();
			foldManager.reparse();

			// Get last fold section
			int foldCount = foldManager.getFoldCount();
			Fold fold = null;
			if (foldCount > 0)
				fold = foldManager.getFold( foldCount - 1 );

			// collapse
			if (fold != null)
			{
				if ( ! fold.isCollapsed() )
					fold.setCollapsed(true);
			}
		}
	}

	private void addParserNotice(int rownum, final Icon icon, LogFileFilterAndColorManager.Level level, final String tooltip)
	{
		final int finalRowNum = rownum > 0 ? rownum : _logTail_txt.getLineCount() - 2;
		
		DefaultParserNotice notice = new DefaultParserNotice(_logParser, tooltip, finalRowNum);

		if      (LogFileFilterAndColorManager.Level.Error  .equals(level)) notice.setLevel(ParserNotice.Level.ERROR);
		else if (LogFileFilterAndColorManager.Level.Warning.equals(level)) notice.setLevel(ParserNotice.Level.WARNING);
		else                                                               notice.setLevel(ParserNotice.Level.INFO);

		_parserResult.addNotice(notice);

		// Add the Icon to a list, which will be added by the LogParser (at a later step)
		_addedTrackingIcons.add( new TrackingIcon(finalRowNum, icon, tooltip) );
	}

	/** Just a holding object */
	private static class TrackingIcon
	{
		int            _line    = -1;
		Icon           _icon    = null;
		String         _toolTip = null;
		GutterIconInfo _gii     = null; // is null until it's added to the Gutter

		public TrackingIcon(int line, Icon icon, String toolTip)
		{
			_line    = line;
			_icon    = icon;
			_toolTip = toolTip;
			_gii     = null;
		}
	}


	@Override
	public void focusGained(FocusEvent e)
	{
	}

	@Override
	public void focusLost(FocusEvent e)
	{
		// Simply use the action listener...
		actionPerformed( new ActionEvent(e.getSource(), e.getID(), "focusLost") );
	}

	@Override
	public void caretUpdate(CaretEvent e)
	{
		validateInput();
	}


	
	/**
	 * Start to tail initial file after the GUI has been started.
	 * @param startTail
	 */
	public void setStartTailAfterStartup(boolean startTail)
	{
		_startTailAfterSartup = startTail;
	}


	@Override
	public void outOfMemoryHandler()
	{
		_logger.debug("outOfMemoryHandler was called");
		
		if ( ! isConnected() )
			return;

		// TRACE TEXT
		String msg = 
			"====================================================================\n" +
			" CLOSE TO OUT OF MEMORY \n" +
			" Tail of the Log File has been stopped. \n" +
			" Close the window and start a new session, or view the data here. \n" +
			"--------------------------------------------------------------------\n";
		
		stopTail();

		int maxConfigMemInMB = (int) (Runtime.getRuntime().maxMemory() / 1024 / 1024);
		int mbLeftAfterGc = Memory.getMemoryLeftInMB();

		String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());

		// OK, this is non-modal, but the OK button doesnt work, fix this later, and use the X on the window instead
		JOptionPane optionPane = new JOptionPane(
				"Sorry, out-of-memory. \n" +
				"\n" +
				"I have STOPPED tail of the Log File \n" +
				"To start a new Trace, Close the Trace window... \n" +
				"\n" +
				"Note: you can raise the memory parameter -Xmx###m in the "+Version.getAppName()+" start script.\n" +
				"Current max memory setting seems to be around "+maxConfigMemInMB+" MB.\n" +
				"After Garbage Collection, you now have "+mbLeftAfterGc+" free MB.", 
				JOptionPane.INFORMATION_MESSAGE);
		JDialog dialog = optionPane.createDialog(this, "out-of-memory @ "+dateStr);
		dialog.setModal(false);
		dialog.setVisible(true);

		if (_fileTail != null)
		{
			_fileTail.waitForShutdownToComplete();
		}
		// Append the message to TEXT, and TEXT FILE
		// set caret at end of TEXT
		_logTail_txt.append(msg);
		_logTail_txt.setCaretPosition( _logTail_txt.getDocument().getLength() );


	}

	@Override
	public void memoryConsumption(int memoryLeftInMB)
	{
		if (memoryLeftInMB < 50)
		{
			_logger.info("Looks like free memory is below "+memoryLeftInMB+" MB, lets do cleanup...");
			outOfMemoryHandler();
		}
	}
	/*---------------------------------------------------
	** END implementing: ActionListener, FocusListener, FileTail.TraceListener, Memory.MemoryListener
	**---------------------------------------------------
	*/
	
	/*---------------------------------------------------
	** BEGIN: implementing saveProps & loadProps
	**---------------------------------------------------
	*/	
	private String getServername()
	{
		return _servername;
	}
	
	/** 
	 * Add a server
	 * @param servername name of the server
	 * @param setAsActive set this to be the active entry
	 * @return true if the entry existed, false if it was added.
	 */
	private boolean addServerEntry(String servername, boolean setAsActive)
	{
		boolean foundServername = false;
		for (int i=0; i<_serverName_cbx.getItemCount(); i++)
		{
			String item = _serverName_cbx.getItemAt(i).toString();
			if (item.equals(servername))
			{
				if (setAsActive)
					_serverName_cbx.setSelectedIndex(i);
				foundServername = true;
				break;
			}
		}
		if ( ! foundServername )
		{
			_serverName_cbx.addItem(servername);
			if (setAsActive)
				_serverName_cbx.setSelectedItem(servername);
		}
		return ! foundServername;
	}

	private void saveProps()
	{
		Configuration conf = Configuration.getInstance(Configuration.TAIL_TEMP);
		if (conf == null)
		{
			_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
			return;
		}

		conf.setProperty("LogTail.misc."+getServername()+".tail",     _tailNewRecordsTop_chk.isSelected() || _tailNewRecordsBot_chk.isSelected() );
		conf.setProperty("LogTail.misc."+getServername()+".filename", _logFilename_txt.getText() );

		conf.setProperty("LogTail.misc."+getServername()+".tail_size",       _tailSize_spm      .getNumber().intValue());
		conf.setProperty("LogTail.misc."+getServername()+".from_file_start", _tailFromStart_cbx.isSelected());

		conf.setProperty("LogTail.misc."+getServername()+".filterGroup", StringUtil.getSelectedItemString(_filterColorSchema_cbx));
		
		//----------------------------------
		// TYPE
		//----------------------------------
		conf.setProperty("LogTail.accessType."+getServername(), _accessType_cbx.getSelectedIndex() );

		//----------------------------------
		// SSH
		//----------------------------------
		if ( ! StringUtil.isNullOrBlank(_sshHostname_txt.getText()) )
			conf.setProperty("LogTail.ssh.conn."+getServername()+".hostname",   _sshHostname_txt.getText() );

		conf.setProperty("LogTail.ssh.conn."+getServername()+".port",       _sshPort_txt.getText() );

		if ( ! StringUtil.isNullOrBlank(_sshUsername_txt.getText()) )
			conf.setProperty("LogTail.ssh.conn."+getServername()+".username",   _sshUsername_txt.getText() );

		if ( ! StringUtil.isNullOrBlank(_sshTailOsCmd_txt.getText()) )
			conf.setProperty("LogTail.ssh.conn."+getServername()+".tailOsCmd",   _sshTailOsCmd_txt.getText() );

		if (_sshPassword_chk.isSelected())
			conf.setProperty("LogTail.ssh.conn."+getServername()+".password", _sshPassword_txt.getText(), true);
		else
			conf.remove("LogTail.ssh.conn."+getServername()+".password");

		conf.setProperty("LogTail.ssh.conn."+getServername()+".savePassword", _sshPassword_chk.isSelected() );

		if ( ! StringUtil.isNullOrBlank(_sshKeyFile_txt.getText()) )
			conf.setProperty("LogTail.ssh.conn."+getServername()+".keyFile",  _sshKeyFile_txt.getText() );

		//----------------------------------
		// SERVERS
		//----------------------------------
		if (_serverName_cbx.getSelectedItem() != null)
			conf.setProperty("LogTail.serverList.active", _serverName_cbx.getSelectedItem() +"" );

		conf.setProperty("LogTail.serverList.count", _serverName_cbx.getItemCount() +"" );
		conf.removeAll  ("LogTail.serverList.entry.");
		for (int i=0; i<_serverName_cbx.getItemCount(); i++)
			conf.setProperty("LogTail.serverList.entry."+i, _serverName_cbx.getItemAt(i).toString() );


		
		//------------------
		// WINDOW
		//------------------
		if (isVisible())
		{
			conf.setLayoutProperty("LogTail.dialog.window.width",  this.getSize().width);
			conf.setLayoutProperty("LogTail.dialog.window.height", this.getSize().height);
			conf.setLayoutProperty("LogTail.dialog.window.pos.x",  this.getLocationOnScreen().x);
			conf.setLayoutProperty("LogTail.dialog.window.pos.y",  this.getLocationOnScreen().y);
		}

		conf.save();
	}

	private void loadProps()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		if (conf == null)
		{
			_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
			return;
		}

		//----------------------------------
		// SERVERS
		//----------------------------------
		int itemCount = conf.getIntProperty("LogTail.serverList.count", -1);
		if (itemCount >= 0)
		{
			for (int i=0; i<itemCount; i++)
			{
				String item = conf.getProperty("LogTail.serverList.entry."+i);
				if (item != null)
					_serverName_cbx.addItem(item);
			}
			String activeSrv = conf.getProperty("LogTail.serverList.active");
			if (activeSrv != null)
				_serverName_cbx.setSelectedItem(activeSrv);
		}

		// Add/set CURRENT server to be the active one 
		addServerEntry(getServername(), true);

		loadProps(getServername(), true);
	}

	private void loadProps(String servername, boolean atStartup)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		if (conf == null)
		{
			_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
			return;
		}

		_tailNewRecordsTop_chk.setSelected(     conf.getBooleanProperty("LogTail.misc."+servername+".tail", true) );
		_tailNewRecordsBot_chk.setSelected(     conf.getBooleanProperty("LogTail.misc."+servername+".tail", true) );
		
		_tailSize_spm     .setValue(            conf.getIntProperty(    "LogTail.misc."+servername+".tail_size",       DEFAULT_TAIL_SIZE));
		_tailFromStart_cbx.setSelected(         conf.getBooleanProperty("LogTail.misc."+servername+".from_file_start", DEFAULT_TAIL_FROM_START));
		
		_tailSize_sp.setEnabled( ! _tailFromStart_cbx.isSelected() );

		String filterSchema = conf.getProperty("LogTail.misc."+servername+".filterGroup", LogFileFilterAndColorManager.DEFAULT_FILTER_GROUP);
		_filterColorSchema_cbx.setSelectedItem(filterSchema);
		LogFileFilterAndColorManager.getInstance().setFilterGroup(filterSchema);

		if ( ! atStartup )
		{
			String logfile = conf.getProperty("LogTail.misc."+servername+".filename");
			if (logfile != null)
				_logFilename_txt.setText(logfile);
		}
			
		
		//----------------------------------
		// TYPE
		//----------------------------------
		_accessType_cbx.setSelectedIndex( conf.getIntProperty("LogTail.accessType."+servername, 0) );

		if (_accessType_cbx.getSelectedIndex() == 0)
		{
			if ( StringUtil.hasValue(_srvVersionStr) )
			{
				if (_srvVersionStr.indexOf("Windows") >= 0)
					_accessType_cbx.setSelectedIndex(ACCESS_TYPE_LOCAL);
				else
					_accessType_cbx.setSelectedIndex(ACCESS_TYPE_SSH);
			}
		}
		
		//----------------------------------
		// SSH
		//----------------------------------
		_sshHostname_txt .setText( conf.getProperty   ("LogTail.ssh.conn."+servername+".hostname",  _sshHostname_txt .getText()) );
		_sshPort_txt     .setText( conf.getProperty   ("LogTail.ssh.conn."+servername+".port",      _sshPort_txt     .getText()) );
		_sshTailOsCmd_txt.setText( conf.getPropertyRaw("LogTail.ssh.conn."+servername+".tailOsCmd", _sshTailOsCmd_txt.getText()) ); // This contains variables etc
		_sshUsername_txt .setText( conf.getProperty   ("LogTail.ssh.conn."+servername+".username",  _sshUsername_txt .getText()) );
		_sshPassword_txt .setText( conf.getProperty   ("LogTail.ssh.conn."+servername+".password",  _sshPassword_txt .getText()) );
		_sshKeyFile_txt  .setText( conf.getProperty   ("LogTail.ssh.conn."+servername+".keyFile",   _sshKeyFile_txt  .getText()) );

		_sshPassword_chk.setSelected( conf.getBooleanProperty("LogTail.ssh.conn."+servername+".savePassword", _sshPassword_chk.isSelected()) );
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
		int width  = conf.getLayoutProperty("LogTail.dialog.window.width",  SwingUtils.hiDpiScale(900));
		int height = conf.getLayoutProperty("LogTail.dialog.window.height", SwingUtils.hiDpiScale(740));
		int x      = conf.getLayoutProperty("LogTail.dialog.window.pos.x",  -1);
		int y      = conf.getLayoutProperty("LogTail.dialog.window.pos.y",  -1);

		if (width != -1 && height != -1)
			this.setSize(width, height);

		if (x != -1 && y != -1)
			if ( ! SwingUtils.isOutOfScreen(x, y, width, height) )
				this.setLocation(x, y);

	}
	/*---------------------------------------------------
	** END: implementing saveProps & loadProps
	**---------------------------------------------------
	*/	
	
	public void clearLogView()
	{
		_logTail_txt.setText("");
	}
	
	/** Stop all tracing and disconnect SSH connection if any */
	public void stopTail()
	{
		if (_fileTail != null)
		{
			_fileTail.shutdown();
			_fileTail = null;
		}

		if (_sshConn != null && _sshConn.isConnected())
		{
			_sshConn.close();
			_sshConn = null;
		}
	}

	public SshConnection sshConnect()
	{
		final String user    = _sshUsername_txt.getText();
		final String passwd  = _sshPassword_txt.getText();
		final String host    = _sshHostname_txt.getText();
		final String portStr = _sshPort_txt.getText();
		final String keyFile = _sshKeyFile_txt.getText();
//		final String initCmd = _sshTailOsCmd_txt.getText();

		int port = 22;
		try {port = Integer.parseInt(portStr);} 
		catch(NumberFormatException ignore) {}

		final SshConnection sshConn = new SshConnection(host, port, user, passwd, keyFile);
		WaitForExecDialog wait = new WaitForExecDialog(this, "SSH Connecting to "+host+", with user "+user);
		sshConn.setWaitForDialog(wait);

		BgExecutor waitTask = new BgExecutor(wait)
		{
			@Override
			public Object doWork()
			{
				try
				{
					sshConn.connect();
					
//					if (StringUtil.hasValue(initCmd))
//					{
//						String[] cmdArr = initCmd.split(";");
//						for (int i=0; i<cmdArr.length; i++)
//						{
//							String osCmd = cmdArr[i].trim();
//							if (StringUtil.isNullOrBlank(osCmd))
//								continue;
//							
//							_logger.info("SSH Connect, Init Cmd, Executing Command ("+(i+1)+" of "+cmdArr.length+") = '"+osCmd+"'. When Connection to "+host+":"+portStr+" with user '"+user+"'.");
//							try
//							{
//    							String output = sshConn.execCommandOutputAsStr(osCmd);
//    							if (StringUtil.hasValue(output))
//    							{
//    								String htmlStr = 
//    									"<html>" +
//    									"Init Command ("+(i+1)+" of "+cmdArr.length+") '<code>"+osCmd+"</code>' produced the following output" +
//    									"<pre>" + output + "</pre>" +
//    									"</html>";
//    								SwingUtils.showInfoMessage(LogTailWindow.this, "Init Command Output", htmlStr);
//    							}
//							}
//							catch (IOException e) 
//							{
//								SwingUtils.showErrorMessage(LogTailWindow.this, "Init Command Failed", "Init Command '"+osCmd+"' Failed...", e);
//							}
//						}
//					}
				}
				catch (Exception e) 
				{
					SwingUtils.showErrorMessage("SSH Connect failed", "SSH Connection to "+host+":"+portStr+" with user '"+user+"' Failed.", e);
//					sshConn = null;
				}
				return null;
			}
		};
		wait.execAndWait(waitTask);
//		if (sshConn == null)
//			return false;
		if (sshConn.isConnected())
			return sshConn;
		else 
			return null;
	}

	/** 
	 * START a file tail session<br>
	 * This involves
	 * <ul>
	 *   <li>Connect to the SSH Server if remote tail</li>
	 *   <li>do 'tail' on the file. (this is done via SSH or a local file location)</li>
	 * </ul>
	 */
	public boolean startTail()
	{
		if ( ! validateInput() )
			return false;

		saveProps();

		int index = _accessType_cbx.getSelectedIndex();

		clearLogView();

		if (index == ACCESS_TYPE_SSH)
		{
//			final String user    = _sshUsername_txt.getText();
//			final String passwd  = _sshPassword_txt.getText();
//			final String host    = _sshHostname_txt.getText();
//			final String portStr = _sshPort_txt.getText();
//			final String initCmd = _sshTailOsCmd_txt.getText();
//
//			int port = 22;
//			try {port = Integer.parseInt(portStr);} 
//			catch(NumberFormatException ignore) {}
//
//			_sshConn = new SshConnection(host, port, user, passwd);
//			WaitForExecDialog wait = new WaitForExecDialog(this, "SSH Connecting to "+host+", with user "+user);
//			_sshConn.setWaitForDialog(wait);
//
//			BgExecutor waitTask = new BgExecutor(wait)
//			{
//				@Override
//				public Object doWork()
//				{
//					try
//					{
//						_sshConn.connect();
//						
//						if (StringUtil.hasValue(initCmd))
//						{
//							String[] cmdArr = initCmd.split(";");
//							for (int i=0; i<cmdArr.length; i++)
//							{
//								String osCmd = cmdArr[i].trim();
//								if (StringUtil.isNullOrBlank(osCmd))
//									continue;
//								
//								_logger.info("SSH Connect, Init Cmd, Executing Command ("+(i+1)+" of "+cmdArr.length+") = '"+osCmd+"'. When Connection to "+host+":"+portStr+" with user '"+user+"'.");
//								try
//								{
//	    							String output = _sshConn.execCommandOutputAsStr(osCmd);
//	    							if (StringUtil.hasValue(output))
//	    							{
//	    								String htmlStr = 
//	    									"<html>" +
//	    									"Init Command ("+(i+1)+" of "+cmdArr.length+") '<code>"+osCmd+"</code>' produced the following output" +
//	    									"<pre>" + output + "</pre>" +
//	    									"</html>";
//	    								SwingUtils.showInfoMessage(LogTailWindow.this, "Init Command Output", htmlStr);
//	    							}
//								}
//								catch (IOException e) 
//								{
//									SwingUtils.showErrorMessage(LogTailWindow.this, "Init Command Failed", "Init Command '"+osCmd+"' Failed...", e);
//								}
//							}
//						}
//					}
//					catch (IOException e) 
//					{
//						SwingUtils.showErrorMessage("SSH Connect failed", "SSH Connection to "+host+":"+portStr+" with user '"+user+"' Failed.", e);
//						_sshConn = null;
//					}
//					return null;
//				}
//			};
//			wait.execAndWait(waitTask);
//			if (_sshConn == null)
//				return false;

			_sshConn = sshConnect();
			if (_sshConn == null)
				return false;

			// Start the TAIL on the file...
			if (_tailFromStart_cbx.isSelected())
				_fileTail = new FileTail(_sshConn, getTailFilename(), true);
			else
				_fileTail = new FileTail(_sshConn, getTailFilename(), _tailSize_spm.getNumber().intValue());

			// If we use "special command for tail", then do NOT check if file exists 
			if (StringUtil.hasValue(_sshTailOsCmd_txt.getText()))
			{
				final String tailCmd = _sshTailOsCmd_txt.getText();
				final String passwd  = _sshPassword_txt.getText();
				if (StringUtil.hasValue(tailCmd))
					_fileTail.setOsCmd(tailCmd, passwd);

				_fileTail.addTraceListener(this);
				_fileTail.start();
			}
			else
			{
				if (_fileTail.doFileExist())
				{
					_fileTail.addTraceListener(this);
					_fileTail.start();
				}
				else
				{
					String msg = "The trace file '"+_fileTail.getFilename()+"' was not found. (SSH Access mode)";
					_logger.error(msg);
					SwingUtils.showErrorMessage("Trace file not found", msg, null);
					stopTail();
					return false;
				}
			}
		}
		else if (index == ACCESS_TYPE_LOCAL)
		{
			// Start the TAIL on the file...
			if (_tailFromStart_cbx.isSelected())
				_fileTail = new FileTail(getTailFilename(), true);
			else
				_fileTail = new FileTail(getTailFilename(), _tailSize_spm.getNumber().intValue());

			if (_fileTail.doFileExist())
			{
				_fileTail.addTraceListener(this);
				_fileTail.start();
			}
			else
			{
				String msg = "The trace file '"+_fileTail.getFilename()+"' was not found. (Local Access mode)";
				_logger.error(msg);
				SwingUtils.showErrorMessage("Trace file not found", msg, null);
				stopTail();
				return false;
			}
		}

		setConnected(true);
		
		return true;
	}


	/** enable/disable some fields depending if we are connected or not, Try to figure out in what state we are in */
	public boolean isConnected()
	{
		boolean isConnected = _sshConn != null || _fileTail != null;

		setConnected(isConnected);
		
		return isConnected;
	}

	/** enable/disable some fields depending if we are connected or not */
	public void setConnected(boolean isConnected)
	{
		_startTail_but.setVisible( ! isConnected );  // Disabled when connected, so we can start
		_stopTail_but .setVisible(   isConnected );  // Enable   when connected, so we can stop

		if (isConnected)
		{
			String title = "Tail - "+getServername()+" - "+_logFilename_txt.getText() + " (running)";
			if ( ! title.equals(getTitle()) )
			{
				setTitle(title); // Set window title
				SwingUtils.setEnabled(_accessTypePanel, false, _tailNewRecordsTop_chk);
				SwingUtils.setEnabled(_sshPanel,        false);
			}
			_tailNewRecordsBot_chk.setVisible(true);
			_accessTypePanel      .setVisible(false);
			_sshPanel             .setVisible(false);
		}
		else
		{
			String title = "Tail - "+getServername()+" - "+_logFilename_txt.getText() + " (NOT running)";
			if ( ! title.equals(getTitle()) )
			{
				setTitle(title); // Set window title
				SwingUtils.setEnabled(_accessTypePanel, true, _tailNewRecordsTop_chk);
				SwingUtils.setEnabled(_sshPanel,        true);
			}
			_tailNewRecordsBot_chk.setVisible(false);
			_accessTypePanel      .setVisible(true);
			// setAccessVisiblePanels() sets the _sshPanel.setVisible() to true or not...
			setAccessVisiblePanels();
		}
	}

	private String getTailFilename()
	{
		return _logFilename_txt.getText();
	}

	/** called from actionPerformed() to check if we should enable/disable "START TRACE" button */
	private boolean validateInput()
	{
		String warn = "";

		int index = _accessType_cbx.getSelectedIndex();
		if (index == ACCESS_TYPE_NOT_SELECTED)
		{
			warn = "Choose a 'Trace File Access' method";
		}
		if (index == ACCESS_TYPE_SSH)
		{
			if (StringUtil.isNullOrBlank(_sshHostname_txt.getText()))
				warn = "SSH Host Name can't be blank";

			if (StringUtil.isNullOrBlank(_sshPort_txt.getText()))
				warn = "SSH Port Number can't be blank";

			if (StringUtil.isNullOrBlank(_sshUsername_txt.getText()))
				warn = "SSH User Name can't be blank";

			if (StringUtil.isNullOrBlank(_sshPassword_txt.getText()))
				warn = "SSH Password can't be blank";
		}

		if (StringUtil.isNullOrBlank(_logFilename_txt.getText()))
			warn = "Filename can't be blank";
		
		_warning_lbl.setText(warn);
		
		boolean ok = StringUtil.isNullOrBlank(warn);
		_startTail_but.setEnabled(ok);
		
		return ok;
	}

	private void setAccessVisiblePanels()
	{
		int index = _accessType_cbx.getSelectedIndex();
		
		if (index == ACCESS_TYPE_NOT_SELECTED)
		{
			_sshPanel   .setVisible(false);
		}
		else if (index == ACCESS_TYPE_SSH)
		{
			_sshPanel   .setVisible(true);
		}
		else if (index == ACCESS_TYPE_LOCAL)
		{
			_sshPanel   .setVisible(false);
		}
	}

	public static String getDefaultPropFile()
	{
		if (AppDir.getAppStoreDir() != null) 
			return AppDir.getAppStoreDir() + File.separator + TAIL_CONFIG_FILE_NAME;

		return TAIL_CONFIG_FILE_NAME;
	}

	//--------------------------------------------------
	//--------------------------------------------------
	//--------------------------------------------------
	//--------------------------------------------------
	// TEST-CODE
	//--------------------------------------------------
	//--------------------------------------------------
	//--------------------------------------------------
	//--------------------------------------------------
	public static void main_test(String[] args)
	{
		// Set Log4j Log Level
//		Configurator.setRootLevel(Level.TRACE);

		Configuration tailTempProps = new Configuration(getDefaultPropFile());
		Configuration.setInstance(Configuration.TAIL_TEMP, tailTempProps);
		
		Configuration.setSearchOrder(Configuration.TAIL_TEMP);

//		System.setProperty("DBXTUNE_SAVE_DIR", "c:/projects/dbxtune/data");
		
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			_logger.info("Using Look And Feel named '"+UIManager.getLookAndFeel().getName()+"', classname='"+UIManager.getLookAndFeel().getClass().getName()+"', toString='"+UIManager.getLookAndFeel()+"'.");
		}
		catch (Exception e)
		{
			_logger.warn("Problem setting the Look And Feel to 'getSystemLookAndFeelClassName()'.", e);
		}

//		String srvVerStr = "Replication Server/15.7.1/EBF 20681 ESD#2 rsebf1571/NT (IX86)/Windows 2008 R2/1/OPT/Fri Nov 02 11:24:22 2012";
//		String srvName   = "GORAN_1_RS";
//		String filename  = "C:\\sybase\\REP-15_5\\install\\GORAN_1_RS.log";
//
//		LogTailWindow xxx = new LogTailWindow(FileType.REPSERVER_LOG, srvName, srvVerStr, filename);
//		xxx.setVisible(true);

		String srvVerStr = null;
		String srvName   = null;
		String filename  = null;

		LogTailWindow xxx = new LogTailWindow(FileType.UNKNOWN_LOG, srvName, srvVerStr, filename);
		xxx.setVisible(true);
	}



	
	
	

	//---------------------------------------------------
	//---------------------------------------------------
	//---------------------------------------------------
	// MAIN
	//---------------------------------------------------
	//---------------------------------------------------
	//---------------------------------------------------
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

		pw.println("usage: tailw [-l <label>] [-f <filename>] [-a]");
		pw.println("             [-U <user>] [-P <passwd>] [-S <server>]");
		pw.println("             [-u <sshUser>] [-p <sshPasswd>] [-s <sshHost>] [-k <file>]");
		pw.println("             [-h] [-v]");
		pw.println("  ");
		pw.println("options:");
		pw.println("  -h,--help                 Usage information.");
		pw.println("  -v,--version              Display "+Version.getAppName()+" and JVM Version.");
		pw.println("  ");
		pw.println("  -l,--label <label>        Label name used in the configuration file");
		pw.println("  -f,--file  <filename>     File to tail");
		pw.println("  -a,--autostart            Start the tail when the GUI is visible");
		pw.println("  ");
		pw.println("  -U,--srvUser   <user>     Username when connecting to ASE/RS.");
		pw.println("  -P,--srvPasswd <passwd>   Password when connecting to ASE/RS. null=noPasswd");
		pw.println("  -S,--srvServer <server>   Connect to ASE/RS and get logname.");
		pw.println("  ");
		pw.println("  -u,--sshUser   <user>     SSH username, if file is remote");
		pw.println("  -p,--sshPasswd <passwd>   SSH password, if file is remote");
		pw.println("  -s,--sshServer <host>     SSH hostname, if file is remote");
		pw.println("  -k,--sshKeyFile <file>    SSH Private Key File, if file is remote");
		pw.println("");
		pw.flush();
	}

	/**
	 * Build the options com.dbxtune.parser. Has to be synchronized because of the way
	 * Options are constructed.
	 * 
	 * @return an options com.dbxtune.parser.
	 */
	private static synchronized Options buildCommandLineOptions()
	{
		Options options = new Options();

		// create the Options
		options.addOption( "h", "help",        false, "Usage information." );
		options.addOption( "v", "version",     false, "Display "+Version.getAppName()+" and JVM Version." );

		options.addOption( "l", "label",       true, "Label name used in the configuration file" );
		options.addOption( "f", "file",        true, "File to tail" );
		options.addOption( "a", "autostart",   false, "Start the tail when the GUI is visible" );

		options.addOption( "U", "srvUser",     true, "Username when connecting to ASE/RS." );
		options.addOption( "P", "srvPasswd",   true, "Password when connecting to ASE/RS. (null=noPasswd)" );
		options.addOption( "S", "srvServer",   true, "Connect to ASE/RS and get logname." );

		options.addOption( "u", "sshUser",     true, "SSH username, if file is remote" );
		options.addOption( "p", "sshPasswd",   true, "SSH password, if file is remote" );
		options.addOption( "s", "sshServer",   true, "SSH hostname, if file is remote" );
		options.addOption( "k", "sshKeyFile",  true, "SSH Private Key File, if file is remote" );

		return options;
	}
	
	private static boolean hasValidInputParams(CommandLine cmd)
	{
		boolean hasParams = false;

		if (cmd.hasOption('l')) hasParams = true;
		if (cmd.hasOption('f')) hasParams = true;
		if (cmd.hasOption('U')) hasParams = true;
		if (cmd.hasOption('P')) hasParams = true;
		if (cmd.hasOption('S')) hasParams = true;
		if (cmd.hasOption('u')) hasParams = true;
		if (cmd.hasOption('p')) hasParams = true;
		if (cmd.hasOption('s')) hasParams = true;
		if (cmd.hasOption('k')) hasParams = true;

		return hasParams;
	}


	//---------------------------------------------------
	// Command Line Parsing
	//---------------------------------------------------
	private static CommandLine parseCommandLine(String[] args, Options options)
	throws ParseException
	{
		// create the command line com.dbxtune.parser
		CommandLineParser parser = new DefaultParser();	
	
		// parse the command line arguments
		CommandLine cmd = parser.parse( options, args );

		// Validate any mandatory options or dependencies of switches
		

		if (_logger.isDebugEnabled())
		{
			for (Iterator<Option> it=cmd.iterator(); it.hasNext();)
			{
				Option opt = it.next();
				_logger.debug("parseCommandLine: swith='"+opt.getOpt()+"', value='"+opt.getValue()+"'.");
			}
		}

		return cmd;
	}

	public static void main(String[] args)
	{
		Version.setAppName(APP_NAME);

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
			// Start GUI/NOGUI will be determined later on.
			//-------------------------------
			else
			{
				new LogTailWindow(cmd);
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
