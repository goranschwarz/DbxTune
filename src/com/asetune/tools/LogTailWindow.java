package com.asetune.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.asetune.Version;
import com.asetune.gui.ConnectionDialog;
import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.gui.swing.WaitForExecDialog.BgExecutor;
import com.asetune.ssh.SshConnection;
import com.asetune.ui.rsyntaxtextarea.AsetuneSyntaxConstants;
import com.asetune.ui.rsyntaxtextarea.RSyntaxUtilitiesX;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.FileTail;
import com.asetune.utils.Memory;
import com.asetune.utils.RepServerUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

public class LogTailWindow
//extends JDialog
extends JFrame
implements ActionListener, FocusListener, FileTail.TraceListener, Memory.MemoryListener
{
	private static Logger _logger = Logger.getLogger(LogTailWindow.class);
	private static final long serialVersionUID = 1L;

	public enum FileType {ASE_LOG, REPSERVER_LOG, UNKNOWN_LOG}

	private FileType        _fileType               = FileType.UNKNOWN_LOG;
	private Connection      _conn                   = null;
	private String          _servername             = null;
	private String          _srvVersionStr          = null;
//	private String          _tailFileName           = null;

	private FileTail        _fileTail               = null;
	private int             _initialLinesInTail     = 50;

	private JPanel          _topPanel               = null;
	private JLabel          _warning_lbl            = new JLabel("Choose 'SSH connection' or 'remote mount'.");
	private JButton         _startTail_but          = new JButton("Start Tail");
	private JButton         _stopTail_but           = new JButton("Stop Tail");
	private JButton         _serverNameRemove_but   = new JButton("Remove");
	private JLabel          _serverName_lbl         = new JLabel("Server Name");
//	private JTextField      _serverName_txt         = new JTextField("");
	private JComboBox       _serverName_cbx         = new JComboBox();
	private JLabel          _logFilename_lbl        = new JLabel("Name of the Log File");
	private JTextField      _logFilename_txt        = new JTextField("");
	private JCheckBox       _tailNewRecordsTop_chk  = new JCheckBox("Move to last row when input is received", true);
	private JCheckBox       _tailNewRecordsBot_chk  = new JCheckBox("Move to last row when input is received", true);

	private String[]        _accessTypeArr          = new String[] {"Choose a method", "SSH Access", "Direct/Local Access"};
	private JPanel          _accessTypePanel        = null;
	private JLabel          _accessType_lbl         = new JLabel("Log File Access");
	private JComboBox       _accessType_cbx         = new JComboBox(_accessTypeArr);
	private static final int ACCESS_TYPE_NOT_SELECTED = 0;
	private static final int ACCESS_TYPE_SSH        = 1;
	private static final int ACCESS_TYPE_LOCAL      = 2;

	private JPanel          _sshPanel               = null;
	private SshConnection   _sshConn                = null;
//	private JCheckBox       _sshConnect_chk         = new JCheckBox("SSH Connection", true);
	private JLabel          _sshUsername_lbl        = new JLabel("Username");
	private JTextField      _sshUsername_txt        = new JTextField("");
	private JLabel          _sshPassword_lbl        = new JLabel("Password");
	private JTextField      _sshPassword_txt        = new JPasswordField("");
	private JCheckBox       _sshPassword_chk        = new JCheckBox("Save Password", true);
	private JLabel          _sshHostname_lbl        = new JLabel("Hostname");
	private JTextField      _sshHostname_txt        = new JTextField("");
	private JLabel          _sshPort_lbl            = new JLabel("Port num");
	private JTextField      _sshPort_txt            = new JTextField("22");
	
	private JPanel          _logTailPanel           = null;
	private RSyntaxTextArea _logTail_txt            = new RSyntaxTextArea();
	private RTextScrollPane _logTail_scroll         = new RTextScrollPane(_logTail_txt);


	/**
	 * Get servername, srvVersion and logFileName from the server
	 * 
	 * @param filetype type of passed connection
	 * @param conn 
	 */
	public LogTailWindow(Connection conn)
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


	//------------------------------------------------------------
	//------------------------------------------------------------
	//------------------------------------------------------------
	// GUI initialization code
	//------------------------------------------------------------
	//------------------------------------------------------------
	//------------------------------------------------------------
	private void init(FileType filetype, Connection conn, String servername, String srvVersionStr, String initialFile)
	{
		setTitle("Log Tail"); // Set window title
		
		// Set the icon, if we "just" do setIconImage() on the JDialog
		// it will not be the "correct" icon in the Alt-Tab list on Windows
		// So we need to grab the owner, and set that since the icon is grabbed from the owner...
		ImageIcon icon = SwingUtils.readImageIcon(Version.class, "images/log_trace_tool.png");
		if (icon != null)
		{
			Object owner = getOwner();
			if (owner != null && owner instanceof Frame)
				((Frame)owner).setIconImage(icon.getImage());
			else
				setIconImage(icon.getImage());
		}

		_fileType      = filetype;
		_conn          = conn;
		_servername    = (StringUtil.isNullOrBlank(servername) ? "UNKNOWN" : servername);
		_srvVersionStr = srvVersionStr; 
		_logFilename_txt.setText(initialFile);

		if (_conn != null && AseConnectionUtils.isConnectionOk(_conn, true, this))
		{
			try
			{
				String dbProduct = ConnectionDialog.getDatabaseProductName(_conn);
				_servername      = ConnectionDialog.getDatabaseServerName(_conn);
				_srvVersionStr   = ConnectionDialog.getDatabaseProductVersion(_conn);

				if (ConnectionDialog.DB_PROD_NAME_SYBASE_ASE.equals(dbProduct))
				{
					_fileType      = FileType.ASE_LOG;
//					_servername    = AseConnectionUtils.getAseServername(_conn);
//					_srvVersionStr = AseConnectionUtils.getAseVersionStr(_conn);
					_logFilename_txt.setText(AseConnectionUtils.getServerLogFileName(_conn));
				}
				if (ConnectionDialog.DB_PROD_NAME_SYBASE_RS.equals(dbProduct))
				{
					_fileType      = FileType.REPSERVER_LOG;
//					_servername    = RepServerUtils.getServerName(_conn);
//					_srvVersionStr = RepServerUtils.getServerVersionStr(_conn);
					_logFilename_txt.setText(RepServerUtils.getServerLogFileName(_conn));
				}
				else
				{
					// FIXME: unknown type and connection...
				}
			}
			catch (SQLException e)
			{
			}
		}
		// Set STYLE of the SYNTAX in the output/tail view
		if ( _fileType == FileType.ASE_LOG)
		{
			_logTail_txt.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL); // FIXME: create a ASE SYNTAX
		}
		else if ( _fileType == FileType.REPSERVER_LOG)
		{
			_logTail_txt.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL); // FIXME: create a RS SYNTAX
		}
		else
		{
			_logTail_txt.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
		}
//		_serverName_txt .setText(_servername);


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

		_startTail_but.setToolTipText("Start doing Tail on the log");
		_stopTail_but .setToolTipText("Stop doing Tail on the log");

		_sshPanel         = createSshPanel();
		_accessTypePanel  = createAccessPanel();

		panel.add(_accessTypePanel,      "push, grow,             hidemode 2");
		panel.add(_sshPanel,             "push, grow, span, wrap, hidemode 2");

//		panel.add(new JLabel(""),        "wrap, push"); // dummy JLabel just to do "push", it will disappear if
		panel.add(_tailNewRecordsBot_chk,"span 4, split, hidemode 2");
		panel.add(_warning_lbl,          "growx");
		panel.add(_startTail_but,        "gap 10 10 0 10, hidemode 3");   // gap left [right] [top] [bottom]
		panel.add(_stopTail_but,         "gap 10 10 0 10, hidemode 3");   // gap left [right] [top] [bottom]
		
		// disable input to some fields
		_stopTail_but         .setVisible(false);
		_tailNewRecordsBot_chk.setVisible(false);

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

		_accessType_lbl.setToolTipText("The File needs to be accessed in some way. You can choose Direct/Local access or SSH access or Simply NOT to Tail the server trace file, just control the AppTrace and read the file yourself.");
		_accessType_cbx.setToolTipText("The File needs to be accessed in some way. You can choose Direct/Local access or SSH access or Simply NOT to Tail the server trace file, just control the AppTrace and read the file yourself.");

		_serverName_lbl.setToolTipText("");
		_serverName_cbx.setToolTipText("");
		_serverNameRemove_but.setToolTipText("Removes this entry from the list");

		panel.add(_serverName_lbl,       "");
		panel.add(_serverName_cbx,       "span 2, split, pushx, growx");
		panel.add(_serverNameRemove_but, "wrap");

		panel.add(_accessType_lbl,       "");
		panel.add(_accessType_cbx,       "pushx, growx, wrap");

		panel.add(_logFilename_lbl,      "");
		panel.add(_logFilename_txt,      "pushx, growx, wrap");
		panel.add(_tailNewRecordsTop_chk,"span, wrap");

		_serverName_cbx.setEditable(true);
		
		// Add action listener
		_serverNameRemove_but .addActionListener(this);
		_accessType_cbx       .addActionListener(this);

		_logFilename_txt      .addActionListener(this);
		_serverName_cbx       .addActionListener(this);
		_tailNewRecordsTop_chk.addActionListener(this);

		// Focus action listener

		return panel;
	}

	private JPanel createSshPanel()
	{
		JPanel panel = SwingUtils.createPanel("SSH Information", true);
//		panel.setLayout(new MigLayout("insets 0 0 0 0"));
		panel.setLayout(new MigLayout());

		// Tooltip
		panel           .setToolTipText("Secure Shell (SSH) is used to access the file on the host where the ASE is located.");
//		_sshConnect_chk .setToolTipText("ASE is located on a unix/linux host that has SSH Secure Shell");
		_sshUsername_lbl.setToolTipText("<html>User name that can access the Application Log on the Server side.<br><br><b>Note:</b> The user needs to read the Log</html>");
		_sshUsername_txt.setToolTipText("<html>User name that can access the Application Log on the Server side.<br><br><b>Note:</b> The user needs to read the Log</html>");
		_sshPassword_lbl.setToolTipText("Password to the User that can access the Log on the Server side");
		_sshPassword_txt.setToolTipText("Password to the User that can access the Log on the Server side");
		_sshPassword_chk.setToolTipText("Save the password in the configuration file, and YES it's encrypted.");
		_sshHostname_lbl.setToolTipText("Host name where the Log is located");
		_sshHostname_txt.setToolTipText("Host name where the Log is located");
		_sshPort_lbl    .setToolTipText("Port number of the SSH Server, normally 22.");
		_sshPort_txt    .setToolTipText("Port number of the SSH Server, normally 22.");

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

		
		// disable input to some fields

		// for validation
		_sshUsername_txt.addActionListener(this);
		_sshPassword_txt.addActionListener(this);
		_sshPassword_chk.addActionListener(this);
		_sshHostname_txt.addActionListener(this);
		_sshPort_txt    .addActionListener(this);

		// Focus action listener
		_sshUsername_txt.addFocusListener(this);
		_sshPassword_txt.addFocusListener(this);
		_sshHostname_txt.addFocusListener(this);
		_sshPort_txt    .addFocusListener(this);

		return panel;
	}

	private JPanel createLogTailPanel()
	{
		JPanel panel = SwingUtils.createPanel("Trace Command Log", false);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));

		// Tooltip
//		panel       .setToolTipText("Tail of the Log");
//		_logTail_txt.setToolTipText("Tail of the Log");
		
		_logTail_txt.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL);

		panel.add(_logTail_scroll, "grow, push, wrap");

		RSyntaxUtilitiesX.installRightClickMenuExtentions(_logTail_scroll, this);

		return panel;
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
		
		// COMBOBOX: SERVERNAMES
		if (_serverName_cbx.equals(source))
		{
			_servername = _serverName_cbx.getSelectedItem().toString();
			addServerEntry(_servername, false);
			loadProps(getServername(), false);
		}

		// BUT: SERVERNAME REMOVE
		if (_serverNameRemove_but.equals(source))
		{
			_serverName_cbx.removeItemAt( _serverName_cbx.getSelectedIndex() );
			_servername = _serverName_cbx.getSelectedItem().toString();
			loadProps(getServername(), false);
			saveProps();
		}
		
		// CHKBOX: keep both _tailNewRecords buttons in sync... (they should be the same button)
		if (_tailNewRecordsTop_chk.equals(source))
			_tailNewRecordsBot_chk.setSelected(_tailNewRecordsTop_chk.isSelected());

		// CHKBOX: keep both _tailNewRecords buttons in sync... (they should be the same button)
		if (_tailNewRecordsBot_chk.equals(source))
			_tailNewRecordsTop_chk.setSelected(_tailNewRecordsBot_chk.isSelected());

		isConnected();   // enable/disable components
		validateInput(); // are we allowed to connect, or are we missing information
	}

	@Override
	public void newTraceRow(String row)
	{
		_logBuffer.append(row);
		if ( ! row.endsWith("\n") )
			_logBuffer.append("\n");

		if ( ! _logBufferTimer.isRunning() )
			_logBufferTimer.start();
	}
	// BEGIN: Deferred processing of the entries
	StringBuilder _logBuffer = new StringBuilder();
	Timer         _logBufferTimer = new Timer(100, new ActionListener()
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			// Make a new buffer, and send the old buffer to be processed
			StringBuilder sb = _logBuffer;
			_logBuffer = new StringBuilder();

			logBufferApply(sb);

			_logBufferTimer.stop();
		}
	});
	private void logBufferApply(StringBuilder sb)
	{
//		try { _logTail_txt.addLineHighlight(_logTail_txt.getCaretLineNumber(), Color.RED); }
//		catch (BadLocationException e) { e.printStackTrace();}

		try
		{
			_logTail_txt.append(sb.toString());

			if (_tailNewRecordsTop_chk.isSelected() || _tailNewRecordsBot_chk.isSelected())
			{
				_logTail_txt.setCaretPosition( _logTail_txt.getDocument().getLength() );
			}
		}
		catch(Throwable t)
		{
			_logger.error("Problems adding text to the tail-output window", t);
		}
	}
	// END: Deferred processing of the entries

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

		int maxConfigMemInMB = (int) Runtime.getRuntime().maxMemory() / 1024 / 1024;
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
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null)
		{
			_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
			return;
		}

		conf.setProperty("LogTail.misc."+getServername()+".tail",     _tailNewRecordsTop_chk.isSelected() || _tailNewRecordsBot_chk.isSelected() );
		conf.setProperty("LogTail.misc."+getServername()+".filename", _logFilename_txt.getText() );

		//----------------------------------
		// TYPE
		//----------------------------------
		conf.setProperty("LogTail.accessType."+getServername(), _accessType_cbx.getSelectedIndex() );

		//----------------------------------
		// SSH
		//----------------------------------
		conf.setProperty("LogTail.ssh.conn."+getServername()+".hostname",   _sshHostname_txt.getText() );
		conf.setProperty("LogTail.ssh.conn."+getServername()+".port",       _sshPort_txt.getText() );
		conf.setProperty("LogTail.ssh.conn."+getServername()+".username",   _sshUsername_txt.getText() );

		if (_sshPassword_chk.isSelected())
			conf.setProperty("LogTail.ssh.conn."+getServername()+".password", _sshPassword_txt.getText(), true);
		else
			conf.remove("LogTail.ssh.conn."+getServername()+".password");

		conf.setProperty("LogTail.ssh.conn."+getServername()+".savePassword", _sshPassword_chk.isSelected() );

		//----------------------------------
		// SERVERS
		//----------------------------------
		conf.setProperty("LogTail.serverList.active", _serverName_cbx.getSelectedItem().toString() );
		conf.setProperty("LogTail.serverList.count", _serverName_cbx.getItemCount() +"" );
		conf.removeAll  ("LogTail.serverList.entry.");
		for (int i=0; i<_serverName_cbx.getItemCount(); i++)
			conf.setProperty("LogTail.serverList.entry."+i, _serverName_cbx.getItemAt(i).toString() );


		
		//------------------
		// WINDOW
		//------------------
		conf.setProperty("LogTail.dialog.window.width",  this.getSize().width);
		conf.setProperty("LogTail.dialog.window.height", this.getSize().height);
		conf.setProperty("LogTail.dialog.window.pos.x",  this.getLocationOnScreen().x);
		conf.setProperty("LogTail.dialog.window.pos.y",  this.getLocationOnScreen().y);

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
//			String activeSrv = conf.getProperty("LogTail.serverList.entry.active");
//			if (activeSrv != null)
//				_serverName_cbx.setSelectedItem(activeSrv);
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

		_tailNewRecordsTop_chk.setSelected( conf.getBooleanProperty("LogTail.misc."+servername+".tail", true) );
		_tailNewRecordsBot_chk.setSelected( conf.getBooleanProperty("LogTail.misc."+servername+".tail", true) );
		
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
			if ( ! StringUtil.isNullOrBlank(_srvVersionStr) )
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
		_sshHostname_txt.setText( conf.getProperty("LogTail.ssh.conn."+servername+".hostname", _sshHostname_txt.getText()) );
		_sshPort_txt    .setText( conf.getProperty("LogTail.ssh.conn."+servername+".port",     _sshPort_txt    .getText()) );
		_sshUsername_txt.setText( conf.getProperty("LogTail.ssh.conn."+servername+".username", _sshUsername_txt.getText()) );
		_sshPassword_txt.setText( conf.getProperty("LogTail.ssh.conn."+servername+".password", _sshPassword_txt.getText()) );

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
		int width  = conf.getIntProperty("LogTail.dialog.window.width",  900);
		int height = conf.getIntProperty("LogTail.dialog.window.height", 740);
		int x      = conf.getIntProperty("LogTail.dialog.window.pos.x",  -1);
		int y      = conf.getIntProperty("LogTail.dialog.window.pos.y",  -1);

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
			final String user    = _sshUsername_txt.getText();
			final String passwd  = _sshPassword_txt.getText();
			final String host    = _sshHostname_txt.getText();
			final String portStr = _sshPort_txt.getText();
			int port = 22;
			try {port = Integer.parseInt(portStr);} 
			catch(NumberFormatException ignore) {}

			_sshConn = new SshConnection(host, port, user, passwd);
			WaitForExecDialog wait = new WaitForExecDialog(this, "SSH Connecting to "+host+", with user "+user);
			BgExecutor waitTask = new BgExecutor(wait)
			{
				@Override
				public Object doWork()
				{
					try
					{
						_sshConn.connect();
					}
					catch (IOException e) 
					{
						SwingUtils.showErrorMessage("SSH Connect failed", "SSH Connection to "+host+":"+portStr+" with user '"+user+"' Failed.", e);
						_sshConn = null;
					}
					return null;
				}
			};
			wait.execAndWait(waitTask);
			if (_sshConn == null)
				return false;

			// Start the TAIL on the file...
			_fileTail = new FileTail(_sshConn, getTailFilename(), _initialLinesInTail);
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
		else if (index == ACCESS_TYPE_LOCAL)
		{
			// Start the TAIL on the file...
//			_fileTail = new FileTail(getTailFilename(), true);
			_fileTail = new FileTail(getTailFilename(), _initialLinesInTail);
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
			String title = "Log Tail - "+getServername()+" - "+_logFilename_txt.getText() + " (running)";
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
			String title = "Log Tail - "+getServername()+" - "+_logFilename_txt.getText() + " (NOT running)";
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

	//--------------------------------------------------
	//--------------------------------------------------
	//--------------------------------------------------
	//--------------------------------------------------
	// TEST-CODE
	//--------------------------------------------------
	//--------------------------------------------------
	//--------------------------------------------------
	//--------------------------------------------------
	public static void main(String[] args)
	{
		Properties log4jProps = new Properties();
		//log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.rootLogger", "DEBUG, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		Configuration conf1 = new Configuration(Version.APP_STORE_DIR + "/test.save.properties");
		Configuration.setInstance(Configuration.USER_TEMP, conf1);
		
		Configuration.setSearchOrder(Configuration.USER_TEMP);

		System.setProperty("ASETUNE_SAVE_DIR", "c:/projects/asetune/data");
		
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			_logger.info("Using Look And Feel named '"+UIManager.getLookAndFeel().getName()+"', classname='"+UIManager.getLookAndFeel().getClass().getName()+"', toString='"+UIManager.getLookAndFeel()+"'.");
		}
		catch (Exception e)
		{
			_logger.warn("Problem setting the Look And Feel to 'getSystemLookAndFeelClassName()'.", e);
		}

		String srvVerStr = "Replication Server/15.7.1/EBF 20681 ESD#2 rsebf1571/NT (IX86)/Windows 2008 R2/1/OPT/Fri Nov 02 11:24:22 2012";
		String srvName   = "GORAN_1_RS";
		String filename  = "C:\\sybase\\REP-15_5\\install\\GORAN_1_RS.log";

		LogTailWindow xxx = new LogTailWindow(FileType.REPSERVER_LOG, srvName, srvVerStr, filename);
		xxx.setVisible(true);
	}
}
