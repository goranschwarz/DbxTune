package com.asetune.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.BadLocationException;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jdesktop.swingx.JXTable;

import com.asetune.Version;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.gui.WaitForExecDialog;
import com.asetune.gui.WaitForExecDialog.BgExecutor;
import com.asetune.hostmon.SshConnection;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.FileTail;
import com.asetune.utils.Memory;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

public class AseAppTraceDialog
extends JDialog
implements ActionListener, FileTail.TraceListener, Memory.MemoryListener
{
	private static Logger _logger = Logger.getLogger(AseAppTraceDialog.class);
	private static final long serialVersionUID = 1L;

	private FileTail        _fileTail               = null;
	
	private String[]        _aseSaveTemplateArr     = new String[] {"aseAppTrace.${SERVERNAME}.${SPID}.${DATE}"};
	private String[]        _accessTypeArr          = new String[] {"Choose a method", "SSH Access", "Direct/Local Access"};
	private final String    DEFAULT_STORED_PROC     = "No procedure is loaded.";

	/** options to reset on setTraceOff */
	HashSet<String> _currentOptions = new HashSet<String>();

	private static String   DEFAULT_aseServerName   = "UNKNOWN";

	private ArrayList<String> _dblist               = new ArrayList<String>();

	/** What ASE host name are we connected to */
	private String          _aseHostName            = "";
	private String          _aseServerName          = DEFAULT_aseServerName;
	private String          _aseVersionStr          = "";

	private JPanel          _aseOptionsPanel        = null;
	private Connection      _aseConn                = null;
	private JCheckBox       _aseOptShowSql_chk      = new JCheckBox("SQL Text",            true);
	private JCheckBox       _aseOptShowplan_chk     = new JCheckBox("Showplan",            false);
	private JCheckBox       _aseOptStatIo_chk       = new JCheckBox("Statistics IO",       false);
	private JCheckBox       _aseOptStatTime_chk     = new JCheckBox("Statistics Time",     false);
	private JCheckBox       _aseOptStatPlanCost_chk = new JCheckBox("Statistics Plancost", false);

	private JButton         _aseExtenedOption_but   = new JButton("Extended Options...");
	private OptionsDialog   _aseOptionsDialog       = new OptionsDialog(this);
	private JButton         _aseSpHelpAppTrace_but  = new JButton("sp_helpapptrace");
	private JCheckBox       _aseShowProcPanel_chk   = new JCheckBox("Show Procedure Text",   true);
	private JCheckBox       _aseDelSrvFileOnStop_chk= new JCheckBox("Delete ASE Trace File", false);

	private JLabel          _aseTraceFile_lbl       = new JLabel("ASE Trace File");
	private JTextField      _aseTraceFile_txt       = new JTextField("");
	private JLabel          _aseSaveTemplate_lbl    = new JLabel("File name template");
	private JComboBox       _aseSaveTemplate_cbx    = new JComboBox(_aseSaveTemplateArr);
	private JLabel          _aseSaveDir_lbl         = new JLabel("ASE Save Dir");
	private JTextField      _aseSaveDir_txt         = new JTextField("/tmp");
	private JLabel          _warning_lbl            = new JLabel("Choose 'SSH connection' or 'remote mount'.");
	private JLabel          _aseSpid_lbl            = new JLabel("Spid");
	private JTextField      _aseSpid_txt            = new JTextField("     ");
	private JButton         _aseSpid_but            = new JButton("...");
	private JButton         _aseStartTrace_but      = new JButton("Start Trace");
	private JButton         _aseStopTrace_but       = new JButton("Stop Trace");

	private JPanel          _accessTypePanel        = null;
	private JLabel          _accessType_lbl         = new JLabel("Trace File Access");
	private JComboBox       _accessType_cbx         = new JComboBox(_accessTypeArr);
	private static final int ACCESS_TYPE_NONE       = 0;
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
	
	private JPanel          _remotePanel            = null;
	private JLabel          _remoteMount_lbl        = new JLabel("Local Dir");
	private JTextField      _remoteMount_txt        = new JTextField("C:/");
	private JButton         _remoteMount_but        = new JButton("...");
	private JLabel          _remoteFile_lbl         = new JLabel("Local File");
	private JTextField      _remoteFile_txt         = new JTextField();

	private JSplitPane      _splitPane1             = null;
	private JSplitPane      _splitPane2             = null;

	private JPanel          _traceOutPanel          = null;
	private JCheckBox       _traceOutSave_chk       = new JCheckBox("Save trace file to location", true);
	private JTextField      _traceOutSave_txt       = new JTextField("");
	private JButton         _traceOutSave_but       = new JButton("...");
	private JCheckBox       _traceOutTail_chk       = new JCheckBox("Move to last row when input is received", true);
	private RSyntaxTextArea _traceOut_txt           = new RSyntaxTextArea();
	private RTextScrollPane _traceOut_scroll        = new RTextScrollPane(_traceOut_txt);

	private JPanel          _procPanel              = null;
	private JCheckBox       _procGet_chk            = new JCheckBox("Get Stored Procedure Text from ASE", true);
	private JCheckBox       _procSave_chk           = new JCheckBox("Save to Dir", true);
	private JTextField      _procSave_txt           = new JTextField("");
	private JButton         _procSave_but           = new JButton("...");
	private JCheckBox       _procMvToLine_chk       = new JCheckBox("Move to correct line in the procedure", true);
	private DefaultComboBoxModel _procName_cbxmdl   = new DefaultComboBoxModel();
	private JComboBox       _procName_cbx           = new JComboBox(_procName_cbxmdl);
	private RSyntaxTextArea _proc_txt               = new RSyntaxTextArea();
	private RTextScrollPane _proc_scroll            = new RTextScrollPane(_proc_txt);

	private JPanel          _traceCmdLogPanel       = null;
	private RSyntaxTextArea _traceCmdLog_txt        = new RSyntaxTextArea();
	private RTextScrollPane _traceCmdLog_scroll     = new RTextScrollPane(_traceCmdLog_txt);

	private int             _spid                   = -1;
	private boolean         _spidExistsInAse        = false; // this is set during login

	public AseAppTraceDialog(int spid, String servername, String aseVersionStr)
	{
		super();
		init(spid, servername, aseVersionStr);
	}


	//------------------------------------------------------------
	//------------------------------------------------------------
	//------------------------------------------------------------
	// GUI initialization code
	//------------------------------------------------------------
	//------------------------------------------------------------
	//------------------------------------------------------------
	private void init(int spid, String aseName, String aseVersionStr)
	{
		setTitle("ASE Application Tracing - Not Connected"); // Set window title
		ImageIcon icon = SwingUtils.readImageIcon(Version.class, "images/ase_app_trace_tool.png");
		if (icon != null)
			setIconImage(icon.getImage());

		_aseVersionStr = aseVersionStr;
		_aseServerName = aseName;
		_spid          = spid;

		if (_aseVersionStr == null) _aseVersionStr = "";
		if (_aseServerName == null) _aseServerName = DEFAULT_aseServerName;

		_aseHostName = "";
		String[] sa = AseConnectionFactory.getHostArr();
		if (sa.length > 0)
			_aseHostName = sa[0];

		setLayout( new BorderLayout() );
		
		loadProps();

		_aseOptionsPanel  = createAsePanel();
		_accessTypePanel  = createAccessPanel();
		_traceOutPanel    = createTraceOutPanel();
		_procPanel        = createProcPanel();
		_traceCmdLogPanel = createTraceCmdLogPanel();

		JPanel topPanel = new JPanel(new MigLayout());
		topPanel.add(_aseOptionsPanel, "grow, push");
		topPanel.add(_accessTypePanel, "grow, push");
		
		_splitPane1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, _traceOutPanel, _procPanel);
		_splitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT,   _splitPane1,    _traceCmdLogPanel);
		add(topPanel,   BorderLayout.NORTH);
		add(_splitPane2, BorderLayout.CENTER);

		pack();
		getSavedWindowProps();

		// Initial state of panels/fields
		_procPanel.setVisible( _aseShowProcPanel_chk.isSelected() );

		validateInput();
		setConnected(false);
		
		setSpid(spid);

		// ADD this from the out of memory listener (Note only works if it has been installed)
		Memory.addMemoryListener(this);
		final Memory.MemoryListener tmp_this = this;

		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				stopTrace();

				// Remove this from the out of memory listener
				Memory.removeMemoryListener(tmp_this);
			}
		});
	}

	private JPanel createAccessPanel()
	{
		JPanel panel = SwingUtils.createPanel("Trace File Access", false);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));

		_sshPanel         = createSshPanel();
		_remotePanel      = createRemotePanel();

		setAccessVisiblePanels();

		_accessType_lbl.setToolTipText("The Trace needs to be accessed in some way. You can choose Direct/Local access or SSH access.");
		_accessType_cbx.setToolTipText("The Trace needs to be accessed in some way. You can choose Direct/Local access or SSH access.");

		panel.add(_accessType_lbl,     "");
		panel.add(_accessType_cbx,     "pushx, growx, wrap");
		panel.add(_sshPanel,           "push, grow, span, wrap, hidemode 3");
		panel.add(_remotePanel,        "push, grow, span, wrap, hidemode 3");

		// Add action listener
		_accessType_cbx.addActionListener(this);

		return panel;
	}
	private void setAccessVisiblePanels()
	{
		int index = _accessType_cbx.getSelectedIndex();
		
		if (index == ACCESS_TYPE_NONE)
		{
			_sshPanel   .setVisible(false);
			_remotePanel.setVisible(false);
		}
		else if (index == ACCESS_TYPE_SSH)
		{
			_sshPanel   .setVisible(true);
			_remotePanel.setVisible(false);
		}
		else if (index == ACCESS_TYPE_LOCAL)
		{
			_sshPanel   .setVisible(false);
			_remotePanel.setVisible(true);
		}
	}

	private JPanel createAsePanel()
	{
		JPanel panel = SwingUtils.createPanel("Application/SPID Tracing", true);
//		panel.setLayout(new MigLayout());
		panel.setLayout(new MigLayout("insets 0 0 0 0", "[][grow]", ""));

		String tooltip;
		// Tooltip
//		panel                  .setToolTipText("");
		_aseSpHelpAppTrace_but .setToolTipText("Execute 'sp_helpapptrace' and add the output to the Command Trace");
		_aseSpid_lbl           .setToolTipText("What SPID will be traced OR are currently traced");
		_aseSpid_txt           .setToolTipText("What SPID will be traced OR are currently traced");
		_aseSpid_but           .setToolTipText("Get a list of SPID's that you can choose to trace.");

		_aseOptShowSql_chk     .setToolTipText("Show SQL issued by the client in the trace file.");
		_aseOptShowplan_chk    .setToolTipText("Show 'showplan' output in the trace file.");
		_aseOptStatIo_chk      .setToolTipText("Show 'statistics io' output in the trace file.");
		_aseOptStatTime_chk    .setToolTipText("Show 'statistics time' output in the trace file.");
		_aseOptStatPlanCost_chk.setToolTipText("Show 'statistics plancost' output in the trace file.");
		_aseExtenedOption_but  .setToolTipText("Some extra/extended trace options are available here.");

		_aseSaveDir_lbl        .setToolTipText("Directory where ASE is storing the trace file.");
		_aseSaveDir_txt        .setToolTipText("Directory where ASE is storing the trace file.");
		tooltip = 
			"<html>" +
			"This the file name the ASE Server will be creating/using when tracing a specific SPID.<br>" +
			"In here you can use variables, that will be filled in when a trace is <b>started</b>.<br>" +
			"<br>" +
			"A preview of the filename can be viewed in the \"grayed\" field \"ASE Trace File\".<br>" +
			"When a trace has been <b>started</b> this would be the \"final\" name for the trace session.<br>" +
			"<br>" +
			"The variables are:" +
			"<ul>" +
			"   <li><code>${SERVERNAME}</code> - This is the name of the ASE server. <code>@@servername</code></li>" +
			"   <li><code>${SPID}</code> - This is the ASE client thread/process id. <code>@@spid</code></li>" +
			"   <li><code>${DATE}</code> - This is simply a date stamp when the trace was started. <code>java:SimpleDateFormat(\"yyyyMMdd.HHmmss\")</code></li>" +
			"</ul>" +
			"</html>";
		_aseSaveTemplate_lbl    .setToolTipText(tooltip);
		_aseSaveTemplate_cbx    .setToolTipText(tooltip);
		_aseStartTrace_but      .setToolTipText("Start doing trace on this SPID");
		_aseStopTrace_but       .setToolTipText("Stop doing trace on this SPID");
		_aseTraceFile_lbl       .setToolTipText("<html>This is the full filename where ASE stores the trace information.<br><b>Note</b>: Before the trace is started, this is only a preview of what will the trace file name would be.</html>");
		_aseTraceFile_txt       .setToolTipText("<html>This is the full filename where ASE stores the trace information.<br><b>Note</b>: Before the trace is started, this is only a preview of what will the trace file name would be.</html>");
		_aseShowProcPanel_chk   .setToolTipText("Should the Procedure Text panel be visible.");
		_aseDelSrvFileOnStop_chk.setToolTipText("<html>" +
			"<b>Try</b> to delete the ASE Trace file on the server side.<br>" +
			"If the Local User or the SSH connection doesn't have privileges to delete the trace file which is created with the 'sybase' user/account, then an error message is displayed.<br>" +
			"<br>" +
			"<b>Note:</b> If this option is not checked or user doesn't have privileges to delete the file, manual cleanup/delete of the ASE Trace files has to be done.</html>");

		_warning_lbl.setForeground(Color.RED);
		_warning_lbl.setHorizontalAlignment(SwingConstants.RIGHT);
		_warning_lbl.setVerticalAlignment(SwingConstants.BOTTOM);

		_aseSpid_lbl.setHorizontalAlignment(SwingConstants.RIGHT);
		
		_aseTraceFile_txt.setEnabled(false);
		_aseSaveTemplate_cbx.setEditable(true);		

		panel.add(_aseOptShowSql_chk,      "span, split");
		panel.add(_aseOptShowplan_chk,     "");
		panel.add(_aseOptStatIo_chk,       "");
		panel.add(_aseOptStatTime_chk,     "");
		panel.add(_aseOptStatPlanCost_chk, "wrap");

		panel.add(new JLabel(""),          "span, split, growx, pushx"); // dummy JLabel just to do "pushx"
		panel.add(_aseExtenedOption_but,   "");
		panel.add(_aseSpHelpAppTrace_but,  "");
		panel.add(_aseShowProcPanel_chk,   "");
		panel.add(_aseDelSrvFileOnStop_chk,"wrap");
				
		panel.add(_aseTraceFile_lbl,      "");
		panel.add(_aseTraceFile_txt,      "growx, wrap");

		panel.add(_aseSaveTemplate_lbl,   "");
		panel.add(_aseSaveTemplate_cbx,   "growx, wrap");

		panel.add(_aseSaveDir_lbl,        "");
		panel.add(_aseSaveDir_txt,        "growx, wrap");

		panel.add(new JLabel(""),         "wrap, push"); // dummy JLabel just to do "push", it will disappear if

		panel.add(_warning_lbl,           "span, split, growx");
		panel.add(_aseSpid_lbl,           "gap 10");
		panel.add(_aseSpid_txt,           "w 50");
		panel.add(_aseSpid_but,           "");
		panel.add(_aseStartTrace_but,     "hidemode 3");
		panel.add(_aseStopTrace_but,      "hidemode 3");
		// disable input to some fields
		_aseStopTrace_but.setVisible(false);

		// Add action listener
		_aseShowProcPanel_chk .addActionListener(this);
		_aseSpid_txt          .addActionListener(this);
		_aseSpid_but          .addActionListener(this);
		_aseExtenedOption_but .addActionListener(this);
		_aseSpHelpAppTrace_but.addActionListener(this);
		_aseStartTrace_but    .addActionListener(this);
		_aseStopTrace_but     .addActionListener(this);

		// for validation
		_aseOptShowSql_chk      .addActionListener(this);
		_aseOptShowplan_chk     .addActionListener(this);
		_aseOptStatIo_chk       .addActionListener(this);
		_aseOptStatTime_chk     .addActionListener(this);
		_aseOptStatPlanCost_chk .addActionListener(this);
		_aseSaveDir_txt         .addActionListener(this);
		_aseSaveTemplate_cbx    .addActionListener(this);
		_aseDelSrvFileOnStop_chk.addActionListener(this);

		// initialize some fields...
		_aseTraceFile_txt.setText(getAseTraceFilePreview());

		return panel;
	}

	private JPanel createSshPanel()
	{
		JPanel panel = SwingUtils.createPanel("SSH Information", true);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));

		// Tooltip
		panel           .setToolTipText("Secure Shell (SSH) is used to access the ASE Trace file on the host where the ASE is located.");
//		_sshConnect_chk .setToolTipText("ASE is located on a unix/linux host that has SSH Secure Shell");
		_sshUsername_lbl.setToolTipText("<html>User name that can access the Application Trace Log on the Server side.<br><br><b>Note:</b> The user needs to read the Trace file created by ASE<br>Normally the trace file has access mode <b>'-rw-r-----'</b>, so user needs to be in same group as the sybase user.</html>");
		_sshUsername_txt.setToolTipText("<html>User name that can access the Application Trace Log on the Server side.<br><br><b>Note:</b> The user needs to read the Trace file created by ASE<br>Normally the trace file has access mode <b>'-rw-r-----'</b>, so user needs to be in same group as the sybase user.</html>");
		_sshPassword_lbl.setToolTipText("Password to the User that can access the Application Trace Log on the Server side");
		_sshPassword_txt.setToolTipText("Password to the User that can access the Application Trace Log on the Server side");
		_sshPassword_chk.setToolTipText("Save the password in the configuration file, and YES it's encrypted.");
		_sshHostname_lbl.setToolTipText("Host name where the ASE Server is located");
		_sshHostname_txt.setToolTipText("Host name where the ASE Server is located");
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

		return panel;
	}

	private JPanel createRemotePanel()
	{
		JPanel panel = SwingUtils.createPanel("Access the Trace file as a Local file", true);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));

		// Tooltip
		panel           .setToolTipText("If you can access the ASE Servers Trace file locally, probably mounted in some way.");
		_remoteMount_lbl.setToolTipText("Local directory name or path where the ASE Trace file can be found.");
		_remoteMount_txt.setToolTipText("Local directory name or path where the ASE Trace file can be found.");
		_remoteMount_but.setToolTipText("Open a File Chooser to help you locate the file. NOTE: you need to click a file in a directory, but it will be stripped of before returned...");
		_remoteFile_lbl .setToolTipText("This is the 'full' file name, which will be used to start a 'tail' on.");
		_remoteFile_txt .setToolTipText("This is the 'full' file name, which will be used to start a 'tail' on.");

		panel.add(_remoteMount_lbl,  "");
		panel.add(_remoteMount_txt,  "split, growx, pushx");
		panel.add(_remoteMount_but,  "wrap");
		
		panel.add(_remoteFile_lbl,   "");
		panel.add(_remoteFile_txt,   "growx, pushx");

		// disable input to some fields
		_remoteFile_txt.setEnabled(false);

		// Add action listener
		_remoteMount_but.addActionListener(this);

		// for validation
		_remoteMount_txt.addActionListener(this);

		// set some initial fields etc...
		_remoteFile_txt.setText(getLocalTraceFilePreview());

		return panel;
	}

	private JPanel createTraceOutPanel()
	{
		JPanel panel = SwingUtils.createPanel("Trace Output", false);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));

		// Tooltip
		panel            .setToolTipText("Trace Information, which the ASE server writes will be visible here.");
		_traceOutSave_chk.setToolTipText("Do you want to save this information to a local file?");
		_traceOutSave_txt.setToolTipText("Directory name where to store this trace information. One file will be created for every session, using the same filename as on the server side, appended with '.txt', which makes the file easier to open.");
		_traceOutSave_but.setToolTipText("Open a File Chooser and locate a file name to be used for this.");
		_traceOutTail_chk.setToolTipText("Simply moved the current active line to be at the end, more or less a tail.");
	
		_traceOut_txt.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);

		panel.add(_traceOutSave_chk,  "split");
		panel.add(_traceOutSave_txt,  "growx, pushx");
		panel.add(_traceOutSave_but,  "wrap");
		panel.add(_traceOutTail_chk,  "wrap");

		panel.add(_traceOut_scroll,   "grow, push, wrap");

		// Add action listener
		_traceOutSave_but.addActionListener(this);

		_traceOut_txt.addCaretListener(createTraceOutTxtCaretListener());
		
		// for validation
		_traceOutSave_chk.addActionListener(this);
		_traceOutSave_txt.addActionListener(this);

		return panel;
	}

	private JPanel createProcPanel()
	{
		JPanel panel = SwingUtils.createPanel("Procedure Text", false);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));

		// Tooltip
		panel            .setToolTipText("When 'SQL Text' is traced, it contains procedure name and Line/Row number. So in here we can fetch the procedure text and position ourself in it.");
		_procSave_chk    .setToolTipText("Save the procedure text for later usage in a file.");
		_procSave_txt    .setToolTipText("In what directory do you want to save all the procedure text files. (procname.sql will be used)");
		_procSave_but    .setToolTipText("Open a File Chooser and locate a Directory Name to store the procedures in.");
		_procMvToLine_chk.setToolTipText("Move to 'current executed line' in the stored procedure text.");
		_procName_cbx    .setToolTipText("This is simple the name of the procedure currently displayed.");
		
		// Add default entry to the ComboBox
		_procName_cbxmdl.addElement(DEFAULT_STORED_PROC);

		// SQL Style in the text editor
		_proc_txt.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);

		
		panel.add(_procGet_chk,       "wrap");
		panel.add(_procSave_chk,      "split");
		panel.add(_procSave_txt,      "growx, pushx");
		panel.add(_procSave_but,      "wrap");
		panel.add(_procMvToLine_chk,  "split");
		panel.add(_procName_cbx,      "growx, pushx, wrap");

		panel.add(_proc_scroll,   "grow, push, wrap");

		// Add action listener
		_procSave_but.addActionListener(this);
		_procName_cbx.addActionListener(this);

		// for validation
		_procSave_chk.addActionListener(this);
		_procSave_txt.addActionListener(this);

		return panel;
	}

	private JPanel createTraceCmdLogPanel()
	{
		JPanel panel = SwingUtils.createPanel("Trace Command Log", false);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));

		// Tooltip
		panel           .setToolTipText("Log of ASE Application Trace Commands");
		_traceCmdLog_txt.setToolTipText("Log of ASE Application Trace Commands");
		
		_traceCmdLog_txt.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);

		panel.add(_traceCmdLog_scroll, "grow, push, wrap");

		return panel;
	}

	
	
	
	
	//------------------------------------------------------------
	//------------------------------------------------------------
	//------------------------------------------------------------
	// Various code
	//------------------------------------------------------------
	//------------------------------------------------------------
	//------------------------------------------------------------
	

	/** save the position right before we setVisible(false) */
	private int _splitPane1_saveDividerLocation = 0;

	/**
	 * IMPLEMENTS: ActionListener
	 */
	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();

		if (_aseShowProcPanel_chk.equals(source))
		{
			boolean visible = _aseShowProcPanel_chk.isSelected();
			if ( ! visible)
				_splitPane1_saveDividerLocation = _splitPane1.getDividerLocation();

			_procPanel.setVisible(visible);
			
			if ( visible)
			{
				if (_splitPane1_saveDividerLocation <= 0)
					_splitPane1_saveDividerLocation = this.getSize().width / 2;
				_splitPane1.setDividerLocation(_splitPane1_saveDividerLocation);
			}
		}

		// SPID_but
		if (_aseSpid_but.equals(source))
		{
			SpidChooserDialog spidDialog = new SpidChooserDialog(this);
			spidDialog.setVisible(true);
			
			int spid = spidDialog.getSpid();
			setSpid(spid);
		}

		// SPID_txt
		if (_aseSpid_txt.equals(source))
		{
			String spidStr = _aseSpid_txt.getText().trim();
			try 
			{ 
				_spid = Integer.parseInt(spidStr); 
			}
			catch (NumberFormatException nfe)
			{
				SwingUtils.showErrorMessage("SPID must be a number", "Spid '"+spidStr+"' must be a number.", nfe);
			}
		}

		// COMBO: ACCESS TYPE
		if (_accessType_cbx.equals(source))
		{
			setAccessVisiblePanels();
		}
		
		// BUT: ASE START TRACE
		if (_aseStartTrace_but.equals(source))
		{
			startTrace();
		}

		// BUT: ASE STOP TRACE
		if (_aseStopTrace_but.equals(source))
		{
			stopTrace();
		}

		// BUT: local/remote MOUNT DIR BUT "..."
		if (_remoteMount_but.equals(source))
		{
			String baseDir = _remoteMount_txt.getText();
			JFileChooser fc = new JFileChooser(baseDir);

			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

			int returnVal = fc.showOpenDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION) 
			{
				String newFile = fc.getSelectedFile().toString().replace('\\', '/');
				_remoteMount_txt.setText(newFile);
			}
		}
		
		// BUT: Trace DIR BUT "..."
		if (_traceOutSave_but.equals(source))
		{
			String baseDir = _traceOutSave_txt.getText();
			if (StringUtil.isNullOrBlank(baseDir))
				baseDir = System.getProperty("ASETUNE_SAVE_DIR");
			JFileChooser fc = new JFileChooser(baseDir);

			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

			int returnVal = fc.showOpenDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION) 
			{
				String newFile = fc.getSelectedFile().toString().replace('\\', '/');
				_traceOutSave_txt.setText(newFile);
			}
		}
		
		// BUT: Proc DIR BUT "..."
		if (_procSave_but.equals(source))
		{
			String baseDir = _procSave_txt.getText();
			if (StringUtil.isNullOrBlank(baseDir))
				baseDir = System.getProperty("ASETUNE_SAVE_DIR");
			JFileChooser fc = new JFileChooser(baseDir);

			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			
			int returnVal = fc.showOpenDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION) 
			{
				String newFile = fc.getSelectedFile().toString().replace('\\', '/');
				_procSave_txt.setText(newFile);
			}
		}

		// CHECKBOX: TRACE SAVE
		if (_traceOutSave_chk.equals(source))
		{
			if (_traceOutSave_chk.isSelected())
			{
				String toDir = _traceOutSave_txt.getText().trim();
				if (StringUtil.isNullOrBlank(toDir))
				{
					_traceOutSave_chk.setSelected(false);
					
					String msg = "Please specify a directory where to store the file.";
					SwingUtils.showErrorMessage("NO Directory", msg, null);
				}
			}
		}

		// CHECKBOX: PROCNAME SAVE
		if (_procSave_chk.equals(source))
		{
			if (_procSave_chk.isSelected())
			{
				String toDir = _procSave_txt.getText().trim();
				if (StringUtil.isNullOrBlank(toDir))
				{
					_procSave_chk.setSelected(false);

					String msg = "Please specify a directory where to store the file.";
					SwingUtils.showErrorMessage("NO Directory", msg, null);
				}
			}
		}

		// SET 
		if (_aseOptShowSql_chk     .equals(source)) setOption("show_sqltext",        _aseOptShowSql_chk     .isSelected());
		if (_aseOptShowplan_chk    .equals(source)) setOption("showplan",            _aseOptShowplan_chk    .isSelected());
		if (_aseOptStatIo_chk      .equals(source)) setOption("statistics io",       _aseOptStatIo_chk      .isSelected());
		if (_aseOptStatTime_chk    .equals(source)) setOption("statistics time",     _aseOptStatTime_chk    .isSelected());
		if (_aseOptStatPlanCost_chk.equals(source)) setOption("statistics plancost", _aseOptStatPlanCost_chk.isSelected());

		// BUT: Extended Options
		if (_aseExtenedOption_but.equals(source))
		{
			_aseOptionsDialog.setVisible(true);
		}

//		// BUT: ASE Control Command Log
//		if (_traceCmdLog_but.equals(source))
//		{
//			_traceCmdLog.setVisible(true);
//		}

		// BUT: ASE Control Command Log
		if (_aseSpHelpAppTrace_but.equals(source))
		{
//			_traceCmdLog.setVisible(true);
			doSpHelpAppTrace();
		}

		// CBX: Procedure Name
		if (_procName_cbx.equals(source))
		{
			String procname = _procName_cbx.getSelectedItem().toString();
			setProcTextLocation(procname, 1);
		}

		// Not connected
		if (_aseConn == null)
		{
			_aseTraceFile_txt.setText(getAseTraceFilePreview());
			_remoteFile_txt  .setText(getLocalTraceFilePreview());
		}
		else
		{
			_aseTraceFile_txt.setText(getAseTraceFileFinal());
			_remoteFile_txt  .setText(getLocalTraceFilePreview());
		}

		isConnected();   // enable/disable components
		validateInput(); // are we allowed to connect, or are we missing information

		saveProps();
	}

	/** enable/disable some fields depending if we are connected or not, Try to figure out in what state we are in */
	public boolean isConnected()
	{
		boolean isConnected = _aseConn != null;

		setConnected(isConnected);
		
		return isConnected;
	}

	/** enable/disable some fields depending if we are connected or not */
	public void setConnected(boolean isConnected)
	{
		_aseSpid_txt.setEnabled( ! isConnected ); // Disabled when connected
		_aseSpid_but.setEnabled( ! isConnected ); // Disabled when connected
		
		_aseSpHelpAppTrace_but.setEnabled( isConnected ); // Enabled only when connected

		_aseStartTrace_but.setVisible( ! isConnected );  // Disabled when connected, so we can start
		_aseStopTrace_but .setVisible(   isConnected );  // Enable   when connected, so we can stop

		if (isConnected)
			setTitle("ASE Application Tracing on SPID "+_spid); // Set window title
		else
			setTitle("ASE Application Tracing - Not Connected"); // Set window title
	}

	/** set a spid */
	private void setSpid(int spid)
	{
		_spid = spid;
		_aseSpid_txt.setText(Integer.toString(spid));
	}

	/** Add a ENTRY that should be in the 'log' of ASE Commands sent using the control connection */
	public void addTraceCmdLog(String cmd)
	{
		String nowStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(System.currentTimeMillis()));

		_traceCmdLog_txt.append("/* " + nowStr + " */ ");
		_traceCmdLog_txt.append(cmd);
		_traceCmdLog_txt.append("\n");

		try
		{
			int rows = _traceCmdLog_txt.getLineCount();
			int position = _traceCmdLog_txt.getLineStartOffset(rows-2);
			_traceCmdLog_txt.setCaretPosition(position);
			_traceCmdLog_txt.moveCaretPosition(position);
		}
		catch (BadLocationException ignore) {}
	}

	/** called from actionPerformed() to check if we should enable/disable "START TRACE" button */
	private void validateInput()
	{
		String warn = "";

		int index = _accessType_cbx.getSelectedIndex();
		if (index == ACCESS_TYPE_NONE)
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
		if (index == ACCESS_TYPE_LOCAL)
		{
			if (StringUtil.isNullOrBlank(_remoteMount_txt.getText()))
				warn = "Local Directory can't be blank";
		}
		
		if (_traceOutSave_chk.isSelected())
			if (StringUtil.isNullOrBlank(_traceOutSave_txt.getText()))
				warn = "Save location for Trace Output has not been specified";
		
		if (_procSave_chk.isSelected())
			if (StringUtil.isNullOrBlank(_procSave_txt.getText()))
				warn = "Save location for Procedure Text has not been specified";

		if (_spid < 0)
			warn = "A valid SPID must be specified";
			
		_warning_lbl.setText(warn);
		
		boolean ok = StringUtil.isNullOrBlank(warn);
		_aseStartTrace_but.setEnabled(ok);
	}
	
	/**
	 * Execute 'exec sp_helpapptrace' on the ASE Control connection and append the output to the command log 
	 */
	private void doSpHelpAppTrace()
	{
		//hhhmmm in 15.7
		// we get error: Cannot set this command on/off while tracing another session with spid 20
		// soo, we probably need to make a new connection to do this....
		String sql = "exec sp_helpapptrace";

		try
		{
			Connection conn = AseConnectionFactory.getConnection(null, Version.getAppName()+"-helpAppTrace", null);

			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			ResultSetTableModel rstm = new ResultSetTableModel(rs);

//			_traceCmdLog.addLog(sql + "\nRESULT SET:\n" + rstm.toTableString());
			addTraceCmdLog(sql + "\nRESULT SET:\n" + rstm.toTableString());

			rs.close();
			stmt.close();
			conn.close();
		}
		catch (Exception e)
		{
			String msg = "Problems execute SQL '"+sql+"', Caught: " + e.toString();
			_logger.warn(msg);
//			_traceCmdLog.addLog(msg);
			addTraceCmdLog(msg);
			SwingUtils.showErrorMessage("ASE Problem", "Problems when doing: "+sql, e);
		}
	}

	/** 
	 * START a application tracing session on the ASE server<br>
	 * This involves
	 * <ul>
	 *   <li>make a ASE Connection, which will act as the controller SPID (this is where: set showplan on|off is done)</li>
	 *   <li>do 'tail' on the ASE Trace file. (this is done via SSH or a local file location)</li>
	 *   <li>start a thread that can fetch stored procedure text from ASE, so we can view what the proc is doing... </li>
	 * </ul>
	 */
	private boolean startTrace()
	{
		_spidExistsInAse = false;

		WaitForExecDialog aseWait = new WaitForExecDialog(this, "Connecting to ASE, for Application Tracing of SPID "+_spid);
		BgExecutor aseWaitTask = new BgExecutor()
		{
			@Override
			public void doWork()
			{
				try
				{
					_aseConn = AseConnectionFactory.getConnection(null, Version.getAppName()+"-AppTrace-"+_spid, null);
					_aseServerName = AseConnectionUtils.getAseServername(_aseConn);

					_spidExistsInAse = false;
					Statement statement = _aseConn.createStatement();
					String sql = 
						"select spid \n" +
						"from master..sysprocesses \n" +
						"where spid = "+_spid;
					ResultSet rs = statement.executeQuery(sql);
					while(rs.next())
						_spidExistsInAse = true;
					rs.close();
					statement.close();

					// ------ column 'stat'
					// 32   - Database created with for load option, or crashed while loading database, instructs recovery not to proceed
					// 256  - Database suspect | Not recovered | Cannot be opened or used | Can be dropped only with dbcc dbrepair
					// 1024 - read only
					// 2048 - dbo use only
					// 4096 - single user
					// ------ column 'stat2'
					// 16 - Database is offline
					// 32 - Database is offline until recovery completes
					// model is used during create database... so skip this one to
					_dblist.clear();
					statement = _aseConn.createStatement();
					sql = 
						"select name \n" +
						"from master..sysdatabases readpast \n" +
						"where (status  & 32  != 32 ) \n" +
						"  and (status  & 256 != 256) \n" +
						"  and (status  & 1024 != 1024) \n" +
						"  and (status  & 2048 != 2048) \n" +
						"  and (status  & 4096 != 4096) \n" +
						"  and (status2 & 16   != 16  ) \n" +
						"  and (status2 & 32   != 32  ) \n" +
						"  and name != 'model'  \n" +
						"order by dbid";
					rs = statement.executeQuery(sql);
					while(rs.next())
						_dblist.add(rs.getString(1));
					rs.close();
					statement.close();
				}
				catch (Exception e) 
				{
					SwingUtils.showErrorMessage("ASE Connect failed", "ASE Connection Failed.", e);
					_aseConn = null;
					_aseServerName = DEFAULT_aseServerName;
				}
			}
		};
		aseWait.execAndWait(aseWaitTask);
		if (_aseConn == null)
			return false;

		if ( ! _spidExistsInAse )
		{
			String msg = "SPID '"+_spid+"' did NOT exists in the ASE when the ASE AppTrace Controller Thread connected.";
			_logger.info(msg);
			SwingUtils.showWarnMessage(this, "SPID didn't exists in ASE", msg, null);
			closeAseConn();
			
			return false;
		}

		// Reset various fields
		_proc_txt       .setText("");
		_traceOut_txt   .setText("");
		_traceCmdLog_txt.setText("");

		int index = _accessType_cbx.getSelectedIndex();
		
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
			BgExecutor waitTask = new BgExecutor()
			{
				@Override
				public void doWork()
				{
					try
					{
						_sshConn.connect();
					}
					catch (IOException e) 
					{
						SwingUtils.showErrorMessage("SSH Connect failed", "SSH Connection to "+host+":"+portStr+" with user '"+user+"' Failed.", e);
						_sshConn = null;
						closeAseConn();
					}
				}
			};
			wait.execAndWait(waitTask);
			if (_sshConn == null)
				return false;

			setTraceOn();

			// Start the TAIL on the file...
			_fileTail = new FileTail(_sshConn, getAseTraceFileFinal(), true);
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
				stopTrace();
				return false;
			}
		}
		else if (index == ACCESS_TYPE_LOCAL)
		{
			setTraceOn();

			// Start the TAIL on the file...
			_fileTail = new FileTail(getLocalTraceFileFinal(), true);
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
				stopTrace();
				return false;
			}
		}

		if (_procGet_chk.isSelected())
			startProcTextReader();

		setConnected(true);
//		_aseStartTrace_but.setVisible(false);
//		_aseStopTrace_but .setVisible(true);
		
		return true;
	}

	/** execute SQL Commands to "START" application tracing in ASE */
	private void setTraceOn()
	{
		createAseTracefile(false);

		execSql("set switch on 3604 with no_info");

		String traceFile = getAseTraceFileFinal();
		execSql("set tracefile '"+traceFile+"' for "+_spid);
		
		if (_aseOptShowSql_chk.isSelected())
			setOption("show_sqltext", true);
		
		if (_aseOptShowplan_chk.isSelected())
			setOption("showplan", true);
		
		if (_aseOptStatIo_chk.isSelected())
			setOption("statistics io", true);
		
		if (_aseOptStatTime_chk.isSelected())
			setOption("statistics time", true);
		
		if (_aseOptStatPlanCost_chk.isSelected())
			setOption("statistics plancost", true);
	}

	/** set a specific trace option, this will be recorded, on stopTrace() all traces that has been turned on will be turned off */
	private void setOption(String option, boolean value)
	{
		setOption(option, value ? "on" : "off");
	}
	/** set a specific trace option, this will be recorded, on stopTrace() all traces that has been turned on will be turned off */
	private void setOption(String option, String value)
	{
		option = option.trim();

		execSql("set "+option+" "+value);
		
		if ( "on".equals(value.toLowerCase()) )
			_currentOptions.add(option);
		else
			_currentOptions.remove(option);
	}

	/** execute SQL Commands to "STOP" application tracing in ASE, all options that has been turned on in this session will be turned off before sening 'set tracefile off for SPID' */
	private void setTraceOff()
	{
		if (_aseConn != null)
		{
			// This needs to be done in 15.0.x and 15.5...
			// set tracefile off for SPID, does not seem to work ok, this starts to 
			// be written to the ASE error log if we do not turn stuff off
			for (String option : _currentOptions)
				execSql("set "+option+" off");

			execSql("set tracefile off for "+_spid);
		}
	}

	/** Stop all tracing and disconnect the control ASE Connection */
	private void stopTrace()
	{
		setTraceOff();
		

		if (_fileTail != null)
		{
			_fileTail.shutdown();
			if (_aseDelSrvFileOnStop_chk.isSelected())
			{
				try
				{
					_fileTail.removeFile();
				}
				catch (Exception e)
				{
					String msg = "Problems deleting the 'tail' file. Caught: "+e;
					_logger.info(msg);
					
					SwingUtils.showWarnMessage(this, "Problems deleting ASE Trace File", msg, e);
				}
			}
		}

		if (_sshConn != null && _sshConn.isConnected())
		{
			_sshConn.close();
			_sshConn = null;
		}

		stopProcTextReader();

		closeAseConn();

		setConnected(false);
//		_aseStartTrace_but.setVisible(true);
//		_aseStopTrace_but .setVisible(false);
	}

	/** Close the ASE Application Tracing CONTROL Connection */
	private void closeAseConn()
	{
		if (_aseConn != null)
		{
			try
			{
				_logger.debug("Closing The Control Connection to ASE");
				_aseConn.close();
			}
			catch (SQLException ignore) {}
			_aseConn = null;
			_aseServerName = DEFAULT_aseServerName;
		}
	}

	/** execute some trace command in the ASE servers control SPID */
	private void execSql(String sql)
	{
		if (_aseConn == null)
			return;

		_logger.debug("execSql(): "+sql);
		
		try
		{
			Statement stmnt = _aseConn.createStatement();
			stmnt.executeUpdate(sql);
			stmnt.close();
			
//			_traceCmdLog.addLog(sql);
			addTraceCmdLog(sql);
		}
		catch (SQLException e)
		{
			String msg = "Problems execute SQL '"+sql+"', Caught: " + e.toString();
			_logger.warn(msg);
//			_traceCmdLog.addLog(msg);
			addTraceCmdLog(msg);
			SwingUtils.showErrorMessage("ASE Problem", "Problems when doing: "+sql, e);
		}
	}

	/** Get file name of the file that the ASE Server will use as it's trace file */
	private String getAseTraceFileFinalOnlyFileName()
	{
		return _finalTraceFileName;
	}
	/** Get full filename dir+file location of the file that the ASE Server will use as it's trace file */
	private String getAseTraceFileFinal()
	{
		return _finalTraceFile;
	}
	/** Get full filename dir+file location of the file that the ASE Server will use as it's trace file, but only the preliminary filename */
	private String getAseTraceFilePreview()
	{
		return createAseTracefile(true);
	}
	/** Get LOCAL full filename dir+file location, this is the filename we will do "tail" on, but not via SSH but directly on the local machine */
	private String getLocalTraceFileFinal()
	{
		String dir = _remoteMount_txt.getText();
		if ( ! dir.endsWith("/") )
			dir += "/";
		return dir + _finalTraceFileName;
	}
	/** Get LOCAL full filename dir+file location, this is the filename we will do "tail" on, but not via SSH but directly on the local machine, but only the preliminary filename  */
	private String getLocalTraceFilePreview()
	{
		String dir = _remoteMount_txt.getText();
		if ( ! dir.endsWith("/") )
			dir += "/";
		return dir + getAseTraceFilename();
	}
	/** Internally called to replace templates into a "real" filename. */ 
	private String getAseTraceFilename()
	{
		String aseSaveTemplate = _aseSaveTemplate_cbx.getSelectedItem().toString();
		
		String nowStr = new SimpleDateFormat("yyyyMMdd.HHmmss").format(new Date(System.currentTimeMillis()));

		String aseSaveFile = aseSaveTemplate;
		aseSaveFile = aseSaveFile.replace("${SPID}",       Integer.toString(_spid));
		aseSaveFile = aseSaveFile.replace("${SERVERNAME}", _aseServerName);
		aseSaveFile = aseSaveFile.replace("${DATE}",       nowStr);
		
		return aseSaveFile;
	}
	private String createAseTracefile(boolean onlyPreview)
	{
		String aseSaveDir  = _aseSaveDir_txt.getText().trim();
		String aseSaveFile = getAseTraceFilename();

		String dirSep = "/";
		if (_aseVersionStr != null && _aseVersionStr.indexOf("Windows") >= 0)
			dirSep = "\\";

		if ( ! (aseSaveDir.endsWith("\\") || aseSaveDir.endsWith("/")) )
			aseSaveDir += dirSep;
		
		if ( onlyPreview )
		{
			return aseSaveDir + aseSaveFile;
		}

		_finalTraceFileName = aseSaveFile;
		_finalTraceFile     = aseSaveDir + aseSaveFile;

		return _finalTraceFile;
	}

	/** Only the file name: Set when you start a trace, this so we can get the name used at the server side */ 
	private String _finalTraceFileName = "";

	/** Both the directory and the file name: Set when you start a trace, this so we can get the name used at the server side */ 
	private String _finalTraceFile     = "";


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

		//----------------------------------
		// ASE OPT
		//----------------------------------
		conf.setProperty("aseAppTrace.ase.opt.ShowSqlText",         _aseOptShowSql_chk     .isSelected() );
		conf.setProperty("aseAppTrace.ase.opt.showplan",            _aseOptShowplan_chk    .isSelected() );
		conf.setProperty("aseAppTrace.ase.opt.statistics_io",       _aseOptStatIo_chk      .isSelected() );
		conf.setProperty("aseAppTrace.ase.opt.statistics_time",     _aseOptStatTime_chk    .isSelected() );
		conf.setProperty("aseAppTrace.ase.opt.statistics_plancost", _aseOptStatPlanCost_chk.isSelected() );

		conf.setProperty("aseAppTrace.ase.showProcPanel",           _aseShowProcPanel_chk  .isSelected() );
		conf.setProperty("aseAppTrace.ase.delSrvTraceFileOnStop",   _aseDelSrvFileOnStop_chk.isSelected() );
		
		conf.setProperty("aseAppTrace.ase."+_aseHostName+".saveDir",_aseSaveDir_txt        .getText() );
		conf.setProperty("aseAppTrace.ase.templateFile",   _aseSaveTemplate_cbx.getSelectedItem().toString() );


		//----------------------------------
		// TYPE
		//----------------------------------
		conf.setProperty("aseAppTrace.accessType."+_aseHostName, _accessType_cbx.getSelectedIndex() );

		//----------------------------------
		// SSH
		//----------------------------------
		conf.setProperty("aseAppTrace.ssh.conn."+_aseHostName+".hostname",   _sshHostname_txt.getText() );
		conf.setProperty("aseAppTrace.ssh.conn."+_aseHostName+".port",       _sshPort_txt.getText() );
		conf.setProperty("aseAppTrace.ssh.conn."+_aseHostName+".username",   _sshUsername_txt.getText() );

		if (_sshPassword_chk.isSelected())
			conf.setEncrypedProperty("aseAppTrace.ssh.conn."+_aseHostName+".password", _sshPassword_txt.getText());
		else
			conf.remove("aseAppTrace.ssh.conn."+_aseHostName+".password");

		conf.setProperty("aseAppTrace.ssh.conn."+_aseHostName+".savePassword", _sshPassword_chk.isSelected() );

		//----------------------------------
		// MOUNT
		//----------------------------------
		conf.setProperty("aseAppTrace.local.dir",   _remoteMount_txt.getText() );

		//----------------------------------
		// TRACE OUTPUT
		//----------------------------------
		conf.setProperty("aseAppTrace.traceOut.save",  _traceOutSave_chk.isSelected() );
		conf.setProperty("aseAppTrace.traceOut.dir",   _traceOutSave_txt.getText() );
		conf.setProperty("aseAppTrace.traceOut.tail",  _traceOutTail_chk.isSelected() );

		//----------------------------------
		// PROC
		//----------------------------------
		conf.setProperty("aseAppTrace.proc.getProcText",  _procGet_chk     .isSelected() );
		conf.setProperty("aseAppTrace.proc.save",         _procSave_chk    .isSelected() );
		conf.setProperty("aseAppTrace.proc.dir",          _procSave_txt    .getText() );
		conf.setProperty("aseAppTrace.proc.moveToLine",   _procMvToLine_chk.isSelected() );


		//------------------
		// WINDOW
		//------------------
		if (_splitPane1 != null)
		{
			if (_aseShowProcPanel_chk.isSelected())
				conf.setProperty("aseAppTrace.dialog.splitPane.dividerLocation1",  _splitPane1.getDividerLocation());
		}

		if (_splitPane2 != null)
			conf.setProperty("aseAppTrace.dialog.splitPane.dividerLocation2",  _splitPane2.getDividerLocation());

		conf.setProperty("aseAppTrace.dialog.window.width",  this.getSize().width);
		conf.setProperty("aseAppTrace.dialog.window.height", this.getSize().height);
		conf.setProperty("aseAppTrace.dialog.window.pos.x",  this.getLocationOnScreen().x);
		conf.setProperty("aseAppTrace.dialog.window.pos.y",  this.getLocationOnScreen().y);

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
		// ASE OPT
		//----------------------------------
		_aseOptShowSql_chk      .setSelected(conf.getBooleanProperty("aseAppTrace.ase.opt.ShowSqlText",         _aseOptShowSql_chk      .isSelected()));
		_aseOptShowplan_chk     .setSelected(conf.getBooleanProperty("aseAppTrace.ase.opt.showplan",            _aseOptShowplan_chk     .isSelected()));
		_aseOptStatIo_chk       .setSelected(conf.getBooleanProperty("aseAppTrace.ase.opt.statistics_io",       _aseOptStatIo_chk       .isSelected()));
		_aseOptStatTime_chk     .setSelected(conf.getBooleanProperty("aseAppTrace.ase.opt.statistics_time",     _aseOptStatTime_chk     .isSelected()));
		_aseOptStatPlanCost_chk .setSelected(conf.getBooleanProperty("aseAppTrace.ase.opt.statistics_plancost", _aseOptStatPlanCost_chk .isSelected()));

		_aseShowProcPanel_chk   .setSelected(conf.getBooleanProperty("aseAppTrace.ase.showProcPanel",           _aseShowProcPanel_chk   .isSelected()));
		_aseDelSrvFileOnStop_chk.setSelected(conf.getBooleanProperty("aseAppTrace.ase.delSrvTraceFileOnStop",   _aseDelSrvFileOnStop_chk.isSelected()));

		String saveDir = conf.getProperty("aseAppTrace.ase."+_aseHostName+".saveDir");
		if (saveDir == null)
		{
			saveDir = "/tmp/";
			if ( ! StringUtil.isNullOrBlank(_aseVersionStr) )
			{
				if (_aseVersionStr.indexOf("Windows") >= 0)
					saveDir = "c:\\";
			}
		}
		_aseSaveDir_txt.setText(saveDir);

		String templateFile = conf.getPropertyRaw("aseAppTrace.ase.templateFile");
		if (templateFile != null)
		{
			boolean foundInTemplateArray = false;
			for (String te : _aseSaveTemplateArr)
			{
				if (te.equals(templateFile))
				{
					foundInTemplateArray = true;
					_aseSaveTemplate_cbx.setSelectedItem( templateFile );
					break;
				}
			}
			if ( ! foundInTemplateArray )
			{
				_aseSaveTemplate_cbx.addItem( templateFile );
				_aseSaveTemplate_cbx.setSelectedItem( templateFile );
			}
		}


		//----------------------------------
		// TYPE
		//----------------------------------
		_accessType_cbx.setSelectedIndex( conf.getIntProperty("aseAppTrace.accessType."+_aseHostName, 0) );

		if (_accessType_cbx.getSelectedIndex() == 0)
		{
			if ( ! StringUtil.isNullOrBlank(_aseVersionStr) )
			{
				if (_aseVersionStr.indexOf("Windows") >= 0)
					_accessType_cbx.setSelectedIndex(ACCESS_TYPE_LOCAL);
				else
					_accessType_cbx.setSelectedIndex(ACCESS_TYPE_SSH);
			}
		}
		
		//----------------------------------
		// SSH
		//----------------------------------
		_sshHostname_txt.setText( conf.getProperty("aseAppTrace.ssh.conn."+_aseHostName+".hostname", _aseHostName) );
		_sshPort_txt    .setText( conf.getProperty("aseAppTrace.ssh.conn."+_aseHostName+".port",     _sshPort_txt    .getText()) );
		_sshUsername_txt.setText( conf.getProperty("aseAppTrace.ssh.conn."+_aseHostName+".username", _sshUsername_txt.getText()) );
		_sshPassword_txt.setText( conf.getProperty("aseAppTrace.ssh.conn."+_aseHostName+".password", _sshPassword_txt.getText()) );

		_sshPassword_chk.setSelected( conf.getBooleanProperty("aseAppTrace.ssh.conn."+_aseHostName+".savePassword", _sshPassword_chk.isSelected()) );


		//----------------------------------
		// MOUNT
		//----------------------------------
		_remoteMount_txt.setText( conf.getProperty("aseAppTrace.local.dir", _remoteMount_txt.getText()) );

		//----------------------------------
		// TRACE OUTPUT
		//----------------------------------
		_traceOutSave_chk.setSelected(conf.getBooleanProperty("aseAppTrace.traceOut.save", _traceOutSave_chk.isSelected()));
		_traceOutSave_txt.setText(    conf.getProperty       ("aseAppTrace.traceOut.dir",  System.getProperty("ASETUNE_SAVE_DIR")));
		_traceOutTail_chk.setSelected(conf.getBooleanProperty("aseAppTrace.traceOut.tail", _traceOutTail_chk.isSelected()));

		//----------------------------------
		// PROC
		//----------------------------------
		_procGet_chk     .setSelected(conf.getBooleanProperty("aseAppTrace.proc.getProcText", _procGet_chk.isSelected()));
		_procSave_chk    .setSelected(conf.getBooleanProperty("aseAppTrace.proc.save",        _procSave_chk.isSelected()));
		_procSave_txt    .setText(    conf.getProperty       ("aseAppTrace.proc.dir",         System.getProperty("ASETUNE_SAVE_DIR")));
		_procMvToLine_chk.setSelected(conf.getBooleanProperty("aseAppTrace.proc.moveToLine",  _procMvToLine_chk.isSelected()));
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
		int width  = conf.getIntProperty("aseAppTrace.dialog.window.width",  900);
		int height = conf.getIntProperty("aseAppTrace.dialog.window.height", 740);
		int x      = conf.getIntProperty("aseAppTrace.dialog.window.pos.x",  -1);
		int y      = conf.getIntProperty("aseAppTrace.dialog.window.pos.y",  -1);
		if (width != -1 && height != -1)
		{
			this.setSize(width, height);
		}
		if (x != -1 && y != -1)
		{
			this.setLocation(x, y);
		}
		
		int divLoc = conf.getIntProperty("aseAppTrace.dialog.splitPane.dividerLocation1",  -1);
		if (divLoc < 0)
			divLoc = width / 2;
		if (_aseShowProcPanel_chk.isSelected())
			_splitPane1.setDividerLocation(divLoc);

		divLoc = conf.getIntProperty("aseAppTrace.dialog.splitPane.dividerLocation2",  -1);
		if (divLoc < 0)
		{
			Dimension d = _aseOptionsPanel.getSize();
			divLoc = height - d.height - 120;
		}
		_splitPane2.setDividerLocation(divLoc);

	}
	/*---------------------------------------------------
	** END: implementing saveProps & loadProps
	**---------------------------------------------------
	*/	


	
	/*---------------------------------------------------
	** BEGIN: implementing Memory.MemoryListener
	**---------------------------------------------------
	*/	
	@Override
	public void outOfMemoryHandler()
	{
		_logger.debug("outOfMemoryHandler was called");

		// TRACE TEXT
		String msg = 
			"================================================\n" +
			" RESET DUE TO OUT OF MEMORY \n" +
			"------------------------------------------------\n";
		_traceCmdLog_txt.setText(msg);


		// PROCEDURE TEXT & cache
		// --- cache
		_procCache.clear();
		// --- combobox
		_procName_cbxmdl = new DefaultComboBoxModel();
		_procName_cbxmdl.addElement(DEFAULT_STORED_PROC);
		_procName_cbx.setModel(_procName_cbxmdl);
		// --- text
		_proc_txt.setText(msg);
		
		System.gc();

		int maxConfigMemInMB = (int) Runtime.getRuntime().maxMemory() / 1024 / 1024;
		int mbLeftAfterGc = Memory.getMemoryLeftInMB();

		// OK, this is non-modal, but the OK button doesnt work, fix this later, and use the X on the window instead
		JOptionPane optionPane = new JOptionPane(
				"Sorry, out-of-memory. \n" +
				"\n" +
				"I have cleared the Trace and ProcedureText fields! \n" +
				"This will hopefully get us going again. \n" +
				"\n" +
				"Note: you can raise the memory parameter -Xmx###m in the "+Version.getAppName()+" start script.\n" +
				"Current max memory setting seems to be around "+maxConfigMemInMB+" MB.\n" +
				"After Garbage Collection, you now have "+mbLeftAfterGc+" free MB.", 
				JOptionPane.INFORMATION_MESSAGE);
		JDialog dialog = optionPane.createDialog(this, "out-of-memory");
		dialog.setModal(false);
		dialog.setVisible(true);
	}

	@Override
	public void memoryConsumption(int memoryLeftInMB)
	{
	}
	/*---------------------------------------------------
	** END: implementing Memory.MemoryListener
	**---------------------------------------------------
	*/	

	
	
	
	/*---------------------------------------------------
	** BEGIN: implementing FileTail.TraceListener
	**---------------------------------------------------
	*/	
	@Override
	public void newTraceRow(String row)
	{
		_traceOut_txt.append(row);
		if ( ! row.endsWith("\n") )
			_traceOut_txt.append("\n");
		
		if (_traceOutTail_chk.isSelected())
		{
			_traceOut_txt.setCaretPosition( _traceOut_txt.getDocument().getLength() );
		}

		if ( _procGet_chk.isSelected() && _procPanel.isVisible() )
		{
			// Sproc: sp_autoformat, Line: 864
			if (row.startsWith("Sproc: "))
			{
				String procName = "";
				int procLine    = -1;
	
				String[] sa = row.split(" ");
				if (sa.length >= 2)
				{
					procName = sa[1];
					procName = StringUtil.removeLastComma(procName);
				}
	
				if (sa.length >= 4)
					procLine = Integer.parseInt(sa[3]);

				addProcTextLookup(null, procName);

				if ( _traceOutTail_chk.isSelected() )
					setProcTextLocation(procName, procLine);
			}
		}
		
		if (row.startsWith("End of Batch"))
		{
			_traceOut_txt.append("######################################################################\n\n");
		}

		// SAVE TO FILE
		if (_traceOutSave_chk.isSelected())
		{
			// Save only every 500 row or so
			if ( (_traceOut_txt.getLineCount() % 500) == 0 )
			{
				String dir = _traceOutSave_txt.getText();
				if ( ! (dir.endsWith("\\") || dir.endsWith("/")) )
					dir += "/";
				String filename = dir + getAseTraceFileFinalOnlyFileName() + ".txt";

				try
				{
					BufferedWriter out = new BufferedWriter(new FileWriter(filename));
					out.write(_traceOut_txt.getText());
					out.close();
				}
				catch (Exception e)
				{
					String msg = "Problems saving Trace output to file '"+filename+"'. Caught: "+e;
					_logger.error(msg);
					SwingUtils.showErrorMessage("Problems Saving Trace Output file.", msg, e);
				}
			}
		}
	}
	/*---------------------------------------------------
	** END: implementing FileTail.TraceListener
	**---------------------------------------------------
	*/	

	/**
	 * Create a caret listener, so we can move to stored proc line...
	 * @return
	 */
	private CaretListener createTraceOutTxtCaretListener()
	{
		return new CaretListener()
		{
			@Override
			public void caretUpdate(CaretEvent e)
			{
				try
				{
//					int caretAt = e.getDot();
//					int caretAtLine = _traceOut_txt.getLineOfOffset(caretAt);
					
					int lineStartOffs = _traceOut_txt.getLineStartOffsetOfCurrentLine();
					int lineEndOffs   = _traceOut_txt.getLineEndOffsetOfCurrentLine();
					int len = lineEndOffs - lineStartOffs - 1;
					if (len < 0)
						return;
					
					String row = _traceOut_txt.getText(lineStartOffs, len);

					// Move to stored proc row...
					// row looks like: Sproc: sp_autoformat, Line: 864
					if (row.startsWith("Sproc: "))
					{
						String procName = "";
						int procLine    = -1;
			
						String[] sa = row.split(" ");
						if (sa.length >= 2)
						{
							procName = sa[1];
							procName = StringUtil.removeLastComma(procName);
						}
			
						if (sa.length >= 4)
							procLine = Integer.parseInt(sa[3]);

						setProcTextLocation(procName, procLine);
					}
				}
				catch (BadLocationException e1)
				{
					e1.printStackTrace();
				}
			}
		};
	}



	
	

	

	private String _currentProcName = "";
	private int    _currentProcLine = 0;
	/** move carret to a specific line in the loaded proc, if the proc isn't loaded, go and load it from the cache */
	private void setProcTextLocation(String procName, int procLine)
	{
		boolean moveLine = false;

		if ( _currentProcName.equals(procName) )
		{
			if (_currentProcLine != procLine)
				moveLine = true;
		}
		else
		{
			String procText = getProcText(null, procName);
			if (procText != null || DEFAULT_STORED_PROC.equals(procName))
			{
				if ( _procName_cbxmdl.getIndexOf(procName) < 0 )
					_procName_cbxmdl.addElement(procName);

				_procName_cbx.setSelectedItem(procName);

				if (DEFAULT_STORED_PROC.equals(procName))
					_proc_txt.setText(DEFAULT_STORED_PROC);
				else
					_proc_txt.setText(procText);

				_currentProcName = procName;

				moveLine = true;
			}
		}

		if (moveLine && _procMvToLine_chk.isSelected())
		{
			try
			{
				int position = _proc_txt.getLineStartOffset(procLine - 1);
				_proc_txt.setCaretPosition(position);
				_proc_txt.moveCaretPosition(position);

				_currentProcLine = procLine;
			}
			catch(BadLocationException e)
			{
//				e.printStackTrace();
			}
		}
	}

	/**
	 * Start the Procedure text thread reader
	 */
	public void startProcTextReader()
	{
		Runnable execCode = createProcTextFetcher();

		_runningProcTextFetcherThread = new Thread(execCode);

		_runningProcTextFetcherThread.setName("SqlProcTextReader");
		_runningProcTextFetcherThread.setDaemon(true);
		_runningProcTextFetcherThread.start();
	}

	/**
	 * Stop the Procedure text thread reader
	 */
	public void stopProcTextReader()
	{
		_runningProcTextFetcher = false;
		if (_runningProcTextFetcherThread != null)
			_runningProcTextFetcherThread.interrupt();
	}
	
	/** Queue that holds procedure records that will be fetched from this thread */
	private BlockingQueue<String> _procTextLookupQueue = new LinkedBlockingQueue<String>();

	/** ADD a procedure to the cache */
	public void addProcTextLookup(String dbname, String procName)
	{
		if ( ! _procCache.containsKey(procName) )
		{
			if ( ! _procTextLookupQueue.contains(procName) )
			{
				_logger.debug("ADDING PROC '"+procName+"' TO LOOKUP QUEUE.");
				_procTextLookupQueue.add(procName);
			}
		}
	}

	/** Get procedure text from the procedures cached locally */
	public String getProcText(String dbname, String procName)
	{
		return _procCache.get(procName);
	}

	/** Here is the cache that holds procedures, in memory */
	private HashMap<String, String> _procCache = new HashMap<String, String>();

	/** Set procedure content in the in-memory cache */
	private void setProcNameText(String procname, String text)
	{
		_procCache.put(procname, text);

		// SAVE TO FILE
		if (_procSave_chk.isSelected())
		{
			String dir = _procSave_txt.getText();
			if ( ! (dir.endsWith("\\") || dir.endsWith("/")) )
				dir += "/";
			String filename = dir + procname + ".sql";
			_logger.info("AppTrace: Saving procedure name '"+procname+"' to file '"+filename+"'.");

			try
			{
				BufferedWriter out = new BufferedWriter(new FileWriter(filename));
				out.write(text);
				out.close();
			}
			catch (Exception e)
			{
				String msg = "Problems saving Procedure Text to file '"+filename+"'. Caught: "+e;
				_logger.error(msg);
				SwingUtils.showErrorMessage("Problems Saving Procedure Text to file.", msg, e);
			}
		}
		
	}
	
	private boolean _runningProcTextFetcher = false;
	private Thread  _runningProcTextFetcherThread = null;
	/** Create the Procedure Text Lookup thread */ 
	private Runnable createProcTextFetcher()
	{
		return new Runnable()
		{
			@Override
			public void run()
			{
				_logger.info("Starting SQL Procedure Text getter");;

				int lastDbnameIndex = 0;
				try
				{
					_runningProcTextFetcher = true;
					while (_runningProcTextFetcher)
					{
						try 
						{
							String procName = _procTextLookupQueue.take();

							if (procName == null)
								continue;

							boolean procWasFound = false;
							for (int i=lastDbnameIndex; i<_dblist.size(); i++)
							{
//								String dbname = "sybsystemprocs";
								String dbname = _dblist.get(i);
	
								String sql;
								//--------------------------------------------
								// GET OBJECT TEXT
								sql = " select c.text "
									+ " from "+dbname+"..sysobjects o, "+dbname+"..syscomments c "
									+ " where o.name = '"+procName+"' "
									+ "   and o.id = c.id "
									+ " order by c.number, c.colid2, c.colid ";
				
								try
								{
									StringBuilder sb = new StringBuilder();
				
									Statement statement = _aseConn.createStatement();
									ResultSet rs = statement.executeQuery(sql);
									while(rs.next())
									{
										String textPart = rs.getString(1);
										sb.append(textPart);
									}
									rs.close();
									statement.close();
				
									if (sb.length() > 0)
									{
										_logger.debug("---FINNISHED(FOUND)--- LOOKUP on PROC '"+procName+"' length="+sb.length()+", put it in the cache...");

										setProcNameText(procName, sb.toString());

										procWasFound = true;
										lastDbnameIndex = i;
										break; // loop _dblist
									}
									else
										_logger.debug("---FINNISHED(NOT FOUND): --- LOOKUP on PROC: '"+dbname+".."+procName+"'.");
								}
								catch (SQLException e)
								{
									_logger.warn("Problems getting Stored Procedure Text for '"+procName+"'. Error="+e.getErrorCode()+", SqlState="+e.getSQLState()+", Msg="+e.getMessage());
								}
							}
							// Next time the proc is requested, start from START of the list
							if ( ! procWasFound )
								lastDbnameIndex = 0;
						} 
						catch (InterruptedException ex) 
						{
							_runningProcTextFetcher = false;
						}
					}
				} 
				catch (Exception e)
				{
					_logger.error("Problems getting SQL Procedure Text", e);
				}

				_logger.info("SQL Procedure Text getter Thread was stopped.");;

			} // end: run()
		};
	} // end: method

	
	/**
	 * Dialog to choose a SPID from a list, which is fetched from the database.
	 * @author gorans
	 */
	private class SpidChooserDialog
	extends Dialog
	implements ActionListener
	{
		private static final long serialVersionUID = 1L;

		private int     _selectedSpid = -1;
		private JXTable _datatable    = new JXTable();
		private JButton _close_but    = new JButton("Close");

		public int getSpid()
		{
			return _selectedSpid;
		}

		public SpidChooserDialog(Dialog owner)
		{
			super(owner, "Choose a SPID from the list to trace", true);

			init();
		}

		private void init()
		{
			setLayout( new BorderLayout() );
			
			JPanel center = createCenterPanel();
			JPanel bottom = createBottomPanel();

			add(center, BorderLayout.CENTER);
			add(bottom, BorderLayout.SOUTH);
			
			pack();

			Dimension d = this.getSize();
			this.setSize(d.width - 800, d.height);
			SwingUtils.centerWindow(this);
		}

		private JPanel createCenterPanel()
		{
			JPanel p = SwingUtils.createPanel("", false);
			p.setLayout(new MigLayout());

			initDatatable();
			p.add(new JScrollPane(_datatable), "grow, push, wrap");
			
			return p;
		}

		private void initDatatable()
		{
//			String sql = "select * from master..sysprocesses";
			String sql = " select spid, dbname=db_name(dbid), username=suser_name(suid), status, cmd, blocked, time_blocked, hostname, hostprocess, program_name, proc_name=isnull(object_name(id,dbid),convert(varchar(20),id)), stmtnum, linenum, cpu, physical_io, memusage, tran_name, network_pktsz, clientname, clienthostname, clientapplname, loggedindatetime, ipaddr, nodeid \n" +
			             " from master..sysprocesses \n" +
			             " where suid > 0 \n" +
			             "   and spid != @@spid ";
			try
			{
				Connection conn = AseConnectionFactory.getConnection(null, Version.getAppName()+"-getSpidList", null);

				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(sql);

				ResultSetTableModel rstm = new ResultSetTableModel(rs, false);

				rs.close();
				stmt.close();
				conn.close();

				_datatable.setToolTipText("Double click on a row to choose a SPID for tracing.");

				_datatable.setSortable(true);
				_datatable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
				_datatable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

				_datatable.setModel(rstm);
				_datatable.packAll(); // set size so that all content in all cells are visible

				_datatable.addMouseListener(new MouseAdapter()
				{
					public void mouseClicked(MouseEvent e) 
					{
						if (e.getClickCount() == 2) 
						{
							JXTable t = (JXTable)e.getSource();
							AbstractTableModel tm = (AbstractTableModel) t.getModel();

						//	int col = t.getColumnSelectedColumn();
							int col = tm.findColumn("spid");
							int row = t.getSelectedRow();

						//	if (col >= 0) col = t.convertColumnIndexToModel(col);
							if (row >= 0) row = t.convertRowIndexToModel(row);

							if (row >= 0 && col >= 0)
							{
								Object spid_obj = tm.getValueAt(row, col);
								if (spid_obj instanceof Number)
								{
									_selectedSpid = ((Number)spid_obj).intValue();
									setVisible(false); // CLOSE THE DIALOG
								}
							}
						}
					}
				});
			}
			catch (Exception e)
			{
				String msg = "Problems execute SQL '"+sql+"', Caught: " + e.toString();
				_logger.warn(msg);
				SwingUtils.showErrorMessage("ASE Problem", "Problems when doing: "+sql, e);
			}
		}

		private JPanel createBottomPanel()
		{
			JPanel p = SwingUtils.createPanel("xxx", false);
			p.setLayout(new MigLayout());
			
			p.add(_close_but, "pushx, tag ok");
			
			_close_but.addActionListener(this);
			
			return p;
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			Object source = e.getSource();
			
			if (_close_but.equals(source))
			{
				_selectedSpid = -1;
				this.setVisible(false);
			}
		}
	}

	
//	private class AseControlCommandLog
//	extends Dialog
//	implements ActionListener
//	{
//		private static final long serialVersionUID = 1L;
//
//		private JButton _close_but = new JButton("Close");
//
//		private RSyntaxTextArea _cmdLog_txt    = new RSyntaxTextArea();
//		private RTextScrollPane _cmdLog_scroll = new RTextScrollPane(_cmdLog_txt);
//
//		public AseControlCommandLog(Dialog owner)
//		{
//			super(owner);
//			setTitle("Log of ASE Application Trace Commands");
//
//			init();
//		}
//
//		private void init()
//		{
//			setLayout( new BorderLayout() );
//			
//			JPanel center = createCenterPanel();
//			JPanel bottom = createBottomPanel();
//
//			add(center, BorderLayout.CENTER);
//			add(bottom, BorderLayout.SOUTH);
//			
//			pack();
//		}
//
//		private JPanel createCenterPanel()
//		{
//			JPanel p = SwingUtils.createPanel("", false);
//			p.setLayout(new MigLayout());
//
//			_cmdLog_txt.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
//			_cmdLog_scroll.setPreferredSize(new Dimension(800, 200));
//
//			p.add(_cmdLog_scroll, "grow, push, wrap");
//			
//			return p;
//		}
//
//		private JPanel createBottomPanel()
//		{
//			JPanel p = SwingUtils.createPanel("xxx", false);
//			p.setLayout(new MigLayout());
//			
//			p.add(_close_but, "pushx, tag ok");
//			
//			_close_but.addActionListener(this);
//			
//			return p;
//		}
//
//		@Override
//		public void actionPerformed(ActionEvent e)
//		{
//			Object source = e.getSource();
//			
//			if (_close_but.equals(source))
//				this.setVisible(false);
//		}
//
//		public void addLog(String cmd)
//		{
//			String nowStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(System.currentTimeMillis()));
//
//			_cmdLog_txt.append("/* " + nowStr + " */ ");
//			_cmdLog_txt.append(cmd);
//			_cmdLog_txt.append("\n");
//		}
//	}

	
	
	/**
	 * A dialog that hold ASE Options that you can turn on.<br>
	 * They are Exteneded due to the fact that it's not that normal to turn them on, and they didn't fit on the ordinary/basic panel.
	 */
	private class OptionsDialog
	extends Dialog
	implements ActionListener
	{
		private static final long serialVersionUID = 1L;

		// set option show               [normal | brief | long | on | off]
		// set option show_lop           [normal | brief | long | on | off]
		// set option show_parallel      [normal | brief | long | on | off]
		// set option show_search_engine [normal | brief | long | on | off]
		// set option show_counters      [normal | brief | long | on | off]
		// set option show_managers      [normal | brief | long | on | off]
		// set option show_histograms    [normal | brief | long | on | off]
		// set option show_abstract_plan [normal | brief | long | on | off]
		// set option show_best_plan     [normal | brief | long | on | off]
		// set option show_code_gen      [normal | brief | long | on | off]
		// set option show_pio_costing   [normal | brief | long | on | off]
		// set option show_lio_costing   [normal | brief | long | on | off]
		// set option show_log_props     [normal | brief | long | on | off]
		// set option show_elimination   [normal | brief | long | on | off]


		private JButton      _close_but                     = new JButton("Close");
		
//		private JCheckBox    _show_sqltext_chk              = new JCheckBox("set show_sqltext",        false);
//		private JCheckBox    _showplan_chk                  = new JCheckBox("set showplan",            false);
//		private JCheckBox    _statistics_io_chk             = new JCheckBox("set statistics io",       false);
//		private JCheckBox    _statistics_time_chk           = new JCheckBox("set statistics time",     false);
//		private JCheckBox    _statistics_plancost_chk       = new JCheckBox("set statistics plancost", false);

		private JCheckBox    _show_chk                      = new JCheckBox("show", false);
		private JRadioButton _show_normal_rbt               = new JRadioButton("normal", true);
		private JRadioButton _show_brief_rbt                = new JRadioButton("brief");
		private JRadioButton _show_long_rbt                 = new JRadioButton("long");

		private JCheckBox    _show_lop_chk                  = new JCheckBox("show_lop", false);
		private JRadioButton _show_lop_normal_rbt           = new JRadioButton("normal", true);
		private JRadioButton _show_lop_brief_rbt            = new JRadioButton("brief");
		private JRadioButton _show_lop_long_rbt             = new JRadioButton("long");

		private JCheckBox    _show_parallel_chk             = new JCheckBox("show_parallel", false);
		private JRadioButton _show_parallel_normal_rbt      = new JRadioButton("normal", true);
		private JRadioButton _show_parallel_brief_rbt       = new JRadioButton("brief");
		private JRadioButton _show_parallel_long_rbt        = new JRadioButton("long");

		private JCheckBox    _show_search_engine_chk        = new JCheckBox("show_search_engine", false);
		private JRadioButton _show_search_engine_normal_rbt = new JRadioButton("normal", true);
		private JRadioButton _show_search_engine_brief_rbt  = new JRadioButton("brief");
		private JRadioButton _show_search_engine_long_rbt   = new JRadioButton("long");

		private JCheckBox    _show_counters_chk             = new JCheckBox("show_counters", false);
		private JRadioButton _show_counters_normal_rbt      = new JRadioButton("normal", true);
		private JRadioButton _show_counters_brief_rbt       = new JRadioButton("brief");
		private JRadioButton _show_counters_long_rbt        = new JRadioButton("long");

		private JCheckBox    _show_managers_chk             = new JCheckBox("show_managers", false);
		private JRadioButton _show_managers_normal_rbt      = new JRadioButton("normal", true);
		private JRadioButton _show_managers_brief_rbt       = new JRadioButton("brief");
		private JRadioButton _show_managers_long_rbt        = new JRadioButton("long");

		private JCheckBox    _show_histograms_chk           = new JCheckBox("show_histograms", false);
		private JRadioButton _show_histograms_normal_rbt    = new JRadioButton("normal", true);
		private JRadioButton _show_histograms_brief_rbt     = new JRadioButton("brief");
		private JRadioButton _show_histograms_long_rbt      = new JRadioButton("long");

		private JCheckBox    _show_abstract_plan_chk        = new JCheckBox("show_abstract_plan", false);
		private JRadioButton _show_abstract_plan_normal_rbt = new JRadioButton("normal", true);
		private JRadioButton _show_abstract_plan_brief_rbt  = new JRadioButton("brief");
		private JRadioButton _show_abstract_plan_long_rbt   = new JRadioButton("long");

		private JCheckBox    _show_best_plan_chk            = new JCheckBox("show_best_plan", false);
		private JRadioButton _show_best_plan_normal_rbt     = new JRadioButton("normal", true);
		private JRadioButton _show_best_plan_brief_rbt      = new JRadioButton("brief");
		private JRadioButton _show_best_plan_long_rbt       = new JRadioButton("long");

		private JCheckBox    _show_code_gen_chk             = new JCheckBox("show_code_gen", false);
		private JRadioButton _show_code_gen_normal_rbt      = new JRadioButton("normal", true);
		private JRadioButton _show_code_gen_brief_rbt       = new JRadioButton("brief");
		private JRadioButton _show_code_gen_long_rbt        = new JRadioButton("long");

		private JCheckBox    _show_pio_costing_chk          = new JCheckBox("show_pio_costing", false);
		private JRadioButton _show_pio_costing_normal_rbt   = new JRadioButton("normal", true);
		private JRadioButton _show_pio_costing_brief_rbt    = new JRadioButton("brief");
		private JRadioButton _show_pio_costing_long_rbt     = new JRadioButton("long");

		private JCheckBox    _show_lio_costing_chk          = new JCheckBox("show_lio_costing", false);
		private JRadioButton _show_lio_costing_normal_rbt   = new JRadioButton("normal", true);
		private JRadioButton _show_lio_costing_brief_rbt    = new JRadioButton("brief");
		private JRadioButton _show_lio_costing_long_rbt     = new JRadioButton("long");

		private JCheckBox    _show_log_props_chk            = new JCheckBox("show_log_props", false);
		private JRadioButton _show_log_props_normal_rbt     = new JRadioButton("normal", true);
		private JRadioButton _show_log_props_brief_rbt      = new JRadioButton("brief");
		private JRadioButton _show_log_props_long_rbt       = new JRadioButton("long");

		private JCheckBox    _show_elimination_chk          = new JCheckBox("show_elimination", false);
		private JRadioButton _show_elimination_normal_rbt   = new JRadioButton("normal", true);
		private JRadioButton _show_elimination_brief_rbt    = new JRadioButton("brief");
		private JRadioButton _show_elimination_long_rbt     = new JRadioButton("long");


		public OptionsDialog(Dialog owner)
		{
			super(owner);
			setTitle("Extended Options");

			init();
		}

		private void init()
		{
			setLayout( new BorderLayout() );
			
//			JPanel top    = createTopPanel();
			JPanel center = createCenterPanel();
			JPanel bottom = createBottomPanel();

//			add(top,    BorderLayout.NORTH);
			add(center, BorderLayout.CENTER);
			add(bottom, BorderLayout.SOUTH);
			
			pack();
			
			SwingUtils.centerWindow(this);
		}
		
//		private JPanel createTopPanel()
//		{
//			JPanel p = SwingUtils.createPanel("Options", true);
//			p.setLayout(new MigLayout());
//
//			p.add(_show_sqltext_chk,        "wrap");
//			p.add(_showplan_chk,            "wrap");
//			p.add(_statistics_io_chk,       "wrap");
//			p.add(_statistics_time_chk,     "wrap");
//			p.add(_statistics_plancost_chk, "wrap");
//
//			_show_sqltext_chk       .addActionListener(this);
//			_showplan_chk           .addActionListener(this);
//			_statistics_io_chk      .addActionListener(this);
//			_statistics_time_chk    .addActionListener(this);
//			_statistics_plancost_chk.addActionListener(this);
//
//			return p;
//		}

		private JPanel createCenterPanel()
		{
			JPanel p = SwingUtils.createPanel("Extended Options", true);
			p.setLayout(new MigLayout());

			addChk123(p, _show_chk,               _show_normal_rbt,               _show_brief_rbt,               _show_long_rbt              );
			addChk123(p, _show_lop_chk,           _show_lop_normal_rbt,           _show_lop_brief_rbt,           _show_lop_long_rbt          );
			addChk123(p, _show_parallel_chk,      _show_parallel_normal_rbt,      _show_parallel_brief_rbt,      _show_parallel_long_rbt     );
			addChk123(p, _show_search_engine_chk, _show_search_engine_normal_rbt, _show_search_engine_brief_rbt, _show_search_engine_long_rbt);
			addChk123(p, _show_counters_chk,      _show_counters_normal_rbt,      _show_counters_brief_rbt,      _show_counters_long_rbt     );
			addChk123(p, _show_managers_chk,      _show_managers_normal_rbt,      _show_managers_brief_rbt,      _show_managers_long_rbt     );
			addChk123(p, _show_histograms_chk,    _show_histograms_normal_rbt,    _show_histograms_brief_rbt,    _show_histograms_long_rbt   );
			addChk123(p, _show_abstract_plan_chk, _show_abstract_plan_normal_rbt, _show_abstract_plan_brief_rbt, _show_abstract_plan_long_rbt);
			addChk123(p, _show_best_plan_chk,     _show_best_plan_normal_rbt,     _show_best_plan_brief_rbt,     _show_best_plan_long_rbt    );
			addChk123(p, _show_code_gen_chk,      _show_code_gen_normal_rbt,      _show_code_gen_brief_rbt,      _show_code_gen_long_rbt     );
			addChk123(p, _show_pio_costing_chk,   _show_pio_costing_normal_rbt,   _show_pio_costing_brief_rbt,   _show_pio_costing_long_rbt  );
			addChk123(p, _show_lio_costing_chk,   _show_lio_costing_normal_rbt,   _show_lio_costing_brief_rbt,   _show_lio_costing_long_rbt  );
			addChk123(p, _show_log_props_chk,     _show_log_props_normal_rbt,     _show_log_props_brief_rbt,     _show_log_props_long_rbt    );
			addChk123(p, _show_elimination_chk,   _show_elimination_normal_rbt,   _show_elimination_brief_rbt,   _show_elimination_long_rbt  );

			return p;
		}

		private void addChk123(JPanel p, JCheckBox chk, JRadioButton normal_rb, JRadioButton brief_rb, JRadioButton long_rb)
		{
			ButtonGroup bg = new ButtonGroup();
			bg.add(normal_rb);
			bg.add(brief_rb);
			bg.add(long_rb);

			p.add(chk,       "");
			p.add(normal_rb, "");
			p.add(brief_rb,  "");
			p.add(long_rb,   "wrap");

			chk      .addActionListener(this);
			normal_rb.addActionListener(this);
			brief_rb .addActionListener(this);
			long_rb  .addActionListener(this);
		}

		private JPanel createBottomPanel()
		{
			JPanel p = SwingUtils.createPanel("xxx", false);
			p.setLayout(new MigLayout());
			
			p.add(_close_but, "pushx, tag ok");
			
			_close_but.addActionListener(this);
			
			return p;
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			Object source = e.getSource();
			
			if (_close_but.equals(source))
				this.setVisible(false);

//			if (_show_sqltext_chk       .equals(source)) setOption("show_sqltext",        _show_sqltext_chk       .isSelected());
//			if (_showplan_chk           .equals(source)) setOption("showplan",            _showplan_chk           .isSelected());
//			if (_statistics_io_chk      .equals(source)) setOption("statistics io",       _statistics_io_chk      .isSelected());
//			if (_statistics_time_chk    .equals(source)) setOption("statistics time",     _statistics_time_chk    .isSelected());
//			if (_statistics_plancost_chk.equals(source)) setOption("statistics plancost", _statistics_plancost_chk.isSelected());

			setOptionOnOff123(source, "option show"               , _show_chk,               _show_normal_rbt,               _show_brief_rbt,               _show_long_rbt              );
			setOptionOnOff123(source, "option show_lop"           , _show_lop_chk,           _show_lop_normal_rbt,           _show_lop_brief_rbt,           _show_lop_long_rbt          );
			setOptionOnOff123(source, "option show_parallel"      , _show_parallel_chk,      _show_parallel_normal_rbt,      _show_parallel_brief_rbt,      _show_parallel_long_rbt     );
			setOptionOnOff123(source, "option show_search_engine" , _show_search_engine_chk, _show_search_engine_normal_rbt, _show_search_engine_brief_rbt, _show_search_engine_long_rbt);
			setOptionOnOff123(source, "option show_counters"      , _show_counters_chk,      _show_counters_normal_rbt,      _show_counters_brief_rbt,      _show_counters_long_rbt     );
			setOptionOnOff123(source, "option show_managers"      , _show_managers_chk,      _show_managers_normal_rbt,      _show_managers_brief_rbt,      _show_managers_long_rbt     );
			setOptionOnOff123(source, "option show_histograms"    , _show_histograms_chk,    _show_histograms_normal_rbt,    _show_histograms_brief_rbt,    _show_histograms_long_rbt   );
			setOptionOnOff123(source, "option show_abstract_plan" , _show_abstract_plan_chk, _show_abstract_plan_normal_rbt, _show_abstract_plan_brief_rbt, _show_abstract_plan_long_rbt);
			setOptionOnOff123(source, "option show_best_plan"     , _show_best_plan_chk,     _show_best_plan_normal_rbt,     _show_best_plan_brief_rbt,     _show_best_plan_long_rbt    );
			setOptionOnOff123(source, "option show_code_gen"      , _show_code_gen_chk,      _show_code_gen_normal_rbt,      _show_code_gen_brief_rbt,      _show_code_gen_long_rbt     );
			setOptionOnOff123(source, "option show_pio_costing"   , _show_pio_costing_chk,   _show_pio_costing_normal_rbt,   _show_pio_costing_brief_rbt,   _show_pio_costing_long_rbt  );
			setOptionOnOff123(source, "option show_lio_costing"   , _show_lio_costing_chk,   _show_lio_costing_normal_rbt,   _show_lio_costing_brief_rbt,   _show_lio_costing_long_rbt  );
			setOptionOnOff123(source, "option show_log_props"     , _show_log_props_chk,     _show_log_props_normal_rbt,     _show_log_props_brief_rbt,     _show_log_props_long_rbt    );
			setOptionOnOff123(source, "option show_elimination"   , _show_elimination_chk,   _show_elimination_normal_rbt,   _show_elimination_brief_rbt,   _show_elimination_long_rbt  );
		}

		private void setOptionOnOff123(Object source, String option, JCheckBox chk, JRadioButton normal_rb, JRadioButton brief_rb, JRadioButton long_rb)
		{
			if (chk.equals(source) || normal_rb.equals(source) || brief_rb.equals(source) || long_rb.equals(source))
			{
				String value;
				if (chk.isSelected())
				{
					value = "on";
					if (brief_rb .isSelected()) value = "brief";
					if (normal_rb.isSelected()) value = "normal";
					if (long_rb  .isSelected()) value = "long";
				}
				else
				{
					value = "off";
				}
				
				setOption(option, value);
			}
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

		Configuration conf1 = new Configuration(Version.APP_STORE_DIR + "/asetune.save.properties");
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

		int    spid      = -1;
		String srvVerStr = "";
		String srvName   = "UNKNOWN";
		// DO THE THING
		try
		{
			System.out.println("Open DB connection.");

			AseConnectionFactory.setAppName("xxx");
			AseConnectionFactory.setUser("sa");
			AseConnectionFactory.setPassword("");
			AseConnectionFactory.setHostPort("sweiq-linux", "2750");
//			AseConnectionFactory.setHostPort("gorans-xp", "5000");
//			AseConnectionFactory.setHostPort("gorans-xp", "15700");
			
			final Connection conn = AseConnectionFactory.getConnection();
			try
			{
				System.out.println("EXEC: set statement_cache off");
				Statement stmt = conn.createStatement();
				stmt.executeUpdate("set statement_cache off");
				stmt.close();
			}
			catch(SQLException e) 
			{
				e.printStackTrace();
			}

			spid      = AseConnectionUtils.getAseSpid(conn);
			srvVerStr = AseConnectionUtils.getAseVersionStr(conn);
			srvName   = AseConnectionUtils.getAseServername(conn);

			Thread bgDummy = new Thread()
			{
				@Override
				public void run()
				{
					try
					{
						while(true)
						{
							Statement stmt = conn.createStatement();
//							ResultSet rs = stmt.executeQuery("exec sp_doDummy   select javaTime='"+System.currentTimeMillis()+"', tiemNow=getdate()");
							ResultSet rs = stmt.executeQuery("exec sp_who   select javaTime='"+System.currentTimeMillis()+"', tiemNow=getdate()");
							while (rs.next())
							{
								rs.getString(1);
							}
							rs.close();
							stmt.close();
							
							Thread.sleep(3000);
						}
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			};
			bgDummy.setDaemon(true);
			bgDummy.setName("ASEDummySpid");
			bgDummy.start();
//			String sql = "exec master..sp_help 'dbo.monLocks' ";
//			System.out.println("DO SQL: "+sql);
//
//			AseSqlScript ss = new AseSqlScript(conn, 10);
//			try	{ 
//				System.out.println("NORMAL:" + ss.executeSqlStr(sql) ); 
//			} catch (SQLException e) { 
//				System.out.println("EXCEPTION:" + ss.executeSqlStr(sql) ); 
//				e.printStackTrace();
//			} finally {
//				ss.close();
//			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		AseAppTraceDialog xxx = new AseAppTraceDialog(spid, srvName, srvVerStr);
		xxx.setVisible(true);
	}
}
