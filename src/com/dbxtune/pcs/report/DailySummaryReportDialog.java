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
package com.dbxtune.pcs.report;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

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
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.dbxtune.Version;
import com.dbxtune.gui.swing.GTabbedPane;
import com.dbxtune.pcs.IPersistWriter;
import com.dbxtune.pcs.MonRecordingInfo;
import com.dbxtune.pcs.PersistReader;
import com.dbxtune.pcs.PersistWriterJdbc;
import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.pcs.report.content.DailySummaryReportContent;
import com.dbxtune.pcs.report.content.IReportEntry;
import com.dbxtune.sql.conn.ConnectionProp;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.ConnectionProvider;
import com.dbxtune.utils.PlatformUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;
import com.dbxtune.utils.TimeUtils;

import net.miginfocom.swing.MigLayout;

public class DailySummaryReportDialog
extends JDialog
//extends JFrame
implements ActionListener, FocusListener, KeyListener, ChangeListener
{
	private static final long serialVersionUID = 1L;
	private static Logger _logger = Logger.getLogger(DailySummaryReportDialog.class);

	private static final String H2DB_FILE_TEMPLATE = "jdbc:h2:file:${filename};IFEXISTS=TRUE";

	// PANEL: OK-CANCEL
	private JLabel                 _warning         = new JLabel();
	private JButton                _ok              = new JButton("OK");
	private JButton                _cancel          = new JButton("Cancel");

	@SuppressWarnings("unused")
	private Window                 _owner           = null;
	private ConnectionProvider     _connProvider    = null;
	private boolean                _isOfflineConnected = false;
	private ConnectionProp         _pcsJdbcWriterConnProps;  // has value if we has connection INFO to the Persistence Counter Storage
	
	private ReportSwingWorker      _bgWorker;
	private boolean                _bgWorkerIsExecuting = false;
	
	public  static final String DEFAULT_OUTPUT_FILENAME_LONG  = "${tmpDir}/${srvName}.${beginDate}_${beginTime}.${endDate}_${endTime}.html";
	public  static final String DEFAULT_OUTPUT_FILENAME_SHORT = "${tmpDir}/${srvName}.${beginDate}_${beginTime}.${endTime}.html";

	private static final String TAB_CONNECTED     = "This Recording";
	private static final String TAB_NOT_CONNECTED = "Other Recordings";

	private GTabbedPane            _tabPane                          = new GTabbedPane();

	// Connected to a PCS
	//------------------------------------------------------------------------------------
	private JPanel                 _cPeriodPanel;
//	private JCheckBox              _cPeriodFullDay_chk     = new JCheckBox("Full Day");
	private JCheckBox              _cPeriodFullDay_chk     = new JCheckBox("<html>Make Report on the <b>Full Day</b>, or choose a Time Period</html>");

	private JLabel                 _cPeriodBeginHour_lbl   = new JLabel("Period Begin Hour");
	private SpinnerNumberModel     _cPeriodBeginHour_spm   = new SpinnerNumberModel(0, 0, 23, 1); // value, min, max, step
	private JSpinner               _cPeriodBeginHour_sp    = new JSpinner(_cPeriodBeginHour_spm);

	private JLabel                 _cPeriodBeginMinute_lbl = new JLabel(", Minute");
	private SpinnerNumberModel     _cPeriodBeginMinute_spm = new SpinnerNumberModel(0, 0, 59, 1); // value, min, max, step
	private JSpinner               _cPeriodBeginMinute_sp  = new JSpinner(_cPeriodBeginMinute_spm);

	private JLabel                 _cPeriodEndHour_lbl     = new JLabel("Period End Hour");
	private SpinnerNumberModel     _cPeriodEndHour_spm     = new SpinnerNumberModel(0, 0, 23, 1); // value, min, max, step
	private JSpinner               _cPeriodEndHour_sp      = new JSpinner(_cPeriodEndHour_spm);

	private JLabel                 _cPeriodEndMinute_lbl   = new JLabel(", Minute");
	private SpinnerNumberModel     _cPeriodEndMinute_spm   = new SpinnerNumberModel(0, 0, 59, 1); // value, min, max, step
	private JSpinner               _cPeriodEndMinute_sp    = new JSpinner(_cPeriodEndMinute_spm);


	// NotConnected to a PCS
	//------------------------------------------------------------------------------------
	private JPanel                 _ncPeriodPanel;
	
	private JLabel                 _ncH2dbFileName_lbl      = new JLabel("H2 Database File");
	private JTextField             _ncH2dbFileName_txt      = new JTextField();
	private JButton                _ncH2dbFileName_but      = new JButton("...");
	private String                 _ncH2dbFileLastLoaded    = null;

//	private JLabel                 _ncJdbcDriver_lbl        = new JLabel("JDBC Driver");
//	private JComboBox<String>      _ncJdbcDriver_cbx        = new JComboBox<String>();

	private JLabel                 _ncJdbcUrl_lbl           = new JLabel("JDBC Url"); 
	private JComboBox<String>      _ncJdbcUrl_cbx           = new JComboBox<String>();
	private JButton                _ncJdbcUrl_but           = new JButton("...");

	private JLabel                 _ncJdbcUsername_lbl      = new JLabel("Username");
	private JTextField             _ncJdbcUsername_txt      = new JTextField("sa");

	private JLabel                 _ncJdbcPassword_lbl      = new JLabel("Password");
	private JTextField             _ncJdbcPassword_txt      = new JPasswordField(); // set to JPasswordField or JTextField depending on debug level
	private JCheckBox              _ncJdbcSavePassword_chk  = new JCheckBox("Save password", true);

//	private JCheckBox              _ncPeriodFullDay_chk     = new JCheckBox("Full Day");
	private JCheckBox              _ncPeriodFullDay_chk     = new JCheckBox("<html>Make Report on the <b>Full Day</b>, or choose a Time Period</html>");

	private JLabel                 _ncPeriodBeginHour_lbl   = new JLabel("Period Begin Hour");
	private SpinnerNumberModel     _ncPeriodBeginHour_spm   = new SpinnerNumberModel(0, 0, 23, 1); // value, min, max, step
	private JSpinner               _ncPeriodBeginHour_sp    = new JSpinner(_ncPeriodBeginHour_spm);

	private JLabel                 _ncPeriodBeginMinute_lbl = new JLabel(", Minute");
	private SpinnerNumberModel     _ncPeriodBeginMinute_spm = new SpinnerNumberModel(0, 0, 59, 1); // value, min, max, step
	private JSpinner               _ncPeriodBeginMinute_sp  = new JSpinner(_ncPeriodBeginMinute_spm);

	private JLabel                 _ncPeriodEndHour_lbl     = new JLabel("Period End Hour");
	private SpinnerNumberModel     _ncPeriodEndHour_spm     = new SpinnerNumberModel(0, 0, 23, 1); // value, min, max, step
	private JSpinner               _ncPeriodEndHour_sp      = new JSpinner(_ncPeriodEndHour_spm);

	private JLabel                 _ncPeriodEndMinute_lbl   = new JLabel(", Minute");
	private SpinnerNumberModel     _ncPeriodEndMinute_spm   = new SpinnerNumberModel(0, 0, 59, 1); // value, min, max, step
	private JSpinner               _ncPeriodEndMinute_sp    = new JSpinner(_ncPeriodEndMinute_spm);

	// Feedback panel
	//------------------------------------------------------------------------------------
	private JPanel                 _feebackPanel;

	private JLabel                 _outputFile_lbl   = new JLabel("Output File");
//	private JTextField             _outputFile_txt   = new JTextField();
	private JComboBox<String>      _outputFile_cbx   = new JComboBox<>();
	private JButton                _outputFile_but   = new JButton("...");
	private JCheckBox              _outputFile_chk   = new JCheckBox("Delete the file when the tool exits.", true);
	private String                 _outputFileLast   = null;

	private JLabel                 _cmdLine_lbl      = new JLabel("Command Line");
	private JTextField             _cmdLine_txt      = new JTextField();

	private JLabel                 _progress_lbl     = new JLabel("At Step:");
	private JTextField             _progress_txt     = new JTextField();
	private JProgressBar           _progress_bar     = new JProgressBar(0, 100);
	private JButton                _continueInBg_but = new JButton("Continue in background");
	private JTextField             _crFilename_txt   = new JTextField();

	private DailySummaryReportDialog(Frame owner, ConnectionProvider connProvider, boolean offline)
	{
		super(owner, "Create - Daily Summary Report", true);
//		super("DBMS Configuration Issues");
//		setModalityType(ModalityType.MODELESS);
		init(owner, connProvider, offline);
	}
	private DailySummaryReportDialog(Dialog owner, ConnectionProvider connProvider, boolean offline)
	{
		super(owner, "Create - Daily Summary Report", true);
//		super("DBMS Configuration Issues");
//		setModalityType(ModalityType.MODELESS);
		init(owner, connProvider, offline);
	}

	public static void showDialog(Frame owner, ConnectionProvider connProvider, boolean offline)
	{
		DailySummaryReportDialog dialog = new DailySummaryReportDialog(owner, connProvider, offline);
		dialog.setVisible(true);
//		dialog.dispose();
	}
	public static void showDialog(Dialog owner, ConnectionProvider connProvider, boolean offline)
	{
		DailySummaryReportDialog dialog = new DailySummaryReportDialog(owner, connProvider, offline);
		dialog.setVisible(true);
//		dialog.dispose();
	}
	public static void showDialog(Component owner, ConnectionProvider connProvider, boolean offline)
	{
		DailySummaryReportDialog dialog = null;
		if (owner instanceof Frame)
			dialog = new DailySummaryReportDialog((Frame)owner, connProvider, offline);
		else if (owner instanceof Dialog)
			dialog = new DailySummaryReportDialog((Dialog)owner, connProvider, offline);
		else
			dialog = new DailySummaryReportDialog((Dialog)null, connProvider, offline);

		dialog.setVisible(true);
//		dialog.dispose();
	}

	@Override
	public void setVisible(boolean visible)
	{
		// Refresh only enabled if connected to ASE, not offline for the moment
		if (visible)
		{
			if (PersistentCounterHandler.hasInstance())
			{
				for (IPersistWriter writer : PersistentCounterHandler.getInstance().getWriters())
				{
					if (writer instanceof PersistWriterJdbc)
					{
						PersistWriterJdbc pcsJdbcWriter = (PersistWriterJdbc) writer;

						DbxConnection tmpConn = pcsJdbcWriter.getStorageConnection();
						if (tmpConn != null)
						{
							_pcsJdbcWriterConnProps = tmpConn.getConnPropOrDefault();
						}
					}
				} 
				
			}
			else
			{
				if ( ! _isOfflineConnected)
				{
					_tabPane.setSelectedTitle(TAB_NOT_CONNECTED);
					_tabPane.setEnabledAtTitle(TAB_CONNECTED, false);
				}
			}

			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					setInitialStates();
				}
			});
		}

		// Do this at THE END
		super.setVisible(visible);
	}
	
	private void init(Window owner, ConnectionProvider connProvider, boolean offline)
	{
		_owner = owner;
		_connProvider = connProvider;
		_isOfflineConnected = offline;

		initComponents();

		pack();

//		Dimension size = getPreferredSize();
//		size.width += 200;
//
//		setPreferredSize(size);
////		setMinimumSize(size);
//		setSize(size);

		setLocationRelativeTo(owner);

//		setFocus();
	}

	protected void initComponents()
	{
		// Set the icon, if we "just" do setIconImage() on the JDialog
		// it will not be the "correct" icon in the Alt-Tab list on Windows
		// So we need to grab the owner, and set that since the icon is grabbed from the owner...
		ImageIcon icon16 = SwingUtils.readImageIcon(Version.class, "images/dsr_16.png");
		ImageIcon icon32 = SwingUtils.readImageIcon(Version.class, "images/dsr_32.png");
		if (icon16 != null || icon32 != null)
		{
			ArrayList<Image> iconList = new ArrayList<Image>();
			if (icon16 != null) iconList.add(icon16.getImage());
			if (icon32 != null) iconList.add(icon32.getImage());

//			Object owner = getOwner();
//			if (owner != null && owner instanceof Frame)
//				((Frame)owner).setIconImages(iconList);
//			else
				setIconImages(iconList);
		}

//		super(_owner);
//		if (_owner != null)
//			setIconImage(_owner.getIconImage());

//		setTitle("DBMS Configuration Issues");

		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("insets 0, wrap 1","",""));   // insets Top Left Bottom Right

		//JTabbedPane tabPane = new JTabbedPane();
//		_tabPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

		
		JPanel header_pan = new JPanel(new MigLayout());
//		JLabel header_lbl = new JLabel("<html>"
//				+ "Below are issues that has been detected when checking various DBMS configurations when connecting.<br>"
//				+ "Hover over the records to see <b>Description</b> and a <b>Proposed Resolution</b>.<br>"
//				+ "<br>"
//				+ "If you do <b>not</b> want this dialog to show after connect:"
//				+ "<ul>"
//				+ "  <li>Apply the Proposed Resolution</li>"
//				+ "  <li>Or press the <b>Discard</b> checkbox at the row/issue, <i>which discards this issue until the DBMS is restarted</i></li>"
//				+ "</ul>"
//				+ "Any issues can be reviewed later: Menu -&gt; View -&gt; View DBMS Configuration... Then click on button 'Show Issues', at the lower left corner"
//				+ "</html>");
		JLabel header_lbl = new JLabel("<html>"
				+ "In this dialog you can create Daily Summary Reports.<br>"
				+ "<br>"
				+ "If you are connected to a Recorded Session"
				+ "<ul>"
				+ "  <li>You can choose a specific time period (Begin/End time) of the current recording, or just the full period.</li>"
				+ "  <li>Or Load another period (stored in another database)</li>"
				+ "</ul>"
				
				+ "If you are NOT connected to a Recorded Session (not connected at all... or connected to a <i>add hook </i>"
				+ "<ul>"
				+ "  <li>Create a Report based on DbxTune Recorded Session</li>"
				+ "</ul>"
				+ "</html>");
		header_pan.add(header_lbl, "grow, push");
		

		_tabPane.addTab(TAB_CONNECTED,     createPcsConnectedPanel());
		_tabPane.addTab(TAB_NOT_CONNECTED, createNoConnectionPanel());
		
		_tabPane.addChangeListener(this);

		//		panel.add(header_pan,                 "growx, pushx, wrap");
//		panel.add(createPcsConnectedPanel(),  "grow, push, wrap");
//		panel.add(createNoConnectionPanel(),  "grow, push, wrap");
		panel.add(_tabPane,                   "grow, push, wrap");
		panel.add(createFeedbackPanel(),      "grow, push, wrap");
		panel.add(createOkCancelPanel(),      "growx, pushx, bottom");

		SwingUtils.installEscapeButton(this, _cancel);

		this.addWindowListener(new java.awt.event.WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				saveProps();
			}
		});

		loadProps();

		setContentPane(panel);
	}

	private void setInitialStates()
	{
		_cPeriodEndHour_spm  .setValue(23);
		_cPeriodEndMinute_spm.setValue(59);
		
		_ncPeriodEndHour_spm  .setValue(23);
		_ncPeriodEndMinute_spm.setValue(59);

		_cPeriodFullDay_chk .doClick();
		_ncPeriodFullDay_chk.doClick();
		
//		File tmpDir = new File(System.getProperty("java.io.tmpdir"));
//		File suggestFileName = new File(tmpDir, "dsr.temp." + Long.toString(System.currentTimeMillis()) + ".html");
//		_outputFile_txt.setText(suggestFileName.getAbsolutePath());

		_outputFile_cbx.setSelectedItem(DEFAULT_OUTPUT_FILENAME_SHORT);
		
		checkForProblem();
	}

	private JPanel createPcsConnectedPanel()
	{
		JPanel panel = SwingUtils.createGPanel("Current Connection to a Recorded Session", false);
		panel.setLayout(new MigLayout("","",""));   // insets Top Left Bottom Right

		JLabel header_lbl = new JLabel("<html>"
				+ "Create Report for a Recorded Session, which you are connected to."
				+ "<ul>"
				+ "  <li>You can choose a specific time period (Begin/End time) of the current recording, or just the full period. <i>in this panel</i></li>"
				+ "  <li>Or Load another period (stored in another database) <i>done in below panel</i></li>"
				+ "</ul>"
				+ "</html>");

		// ADD Components
		panel.add(header_lbl,             "span, wrap");

		panel.add(_cPeriodFullDay_chk,    "span, wrap");

		panel.add(_cPeriodBeginHour_lbl,   "");
		panel.add(_cPeriodBeginHour_sp,    "");
		panel.add(_cPeriodBeginMinute_lbl, "");
		panel.add(_cPeriodBeginMinute_sp,  "wrap");

		panel.add(_cPeriodEndHour_lbl,     "");
		panel.add(_cPeriodEndHour_sp,      "");
		panel.add(_cPeriodEndMinute_lbl,   "");
		panel.add(_cPeriodEndMinute_sp,    "wrap");

		// Initialize some fields.

		// ADD ACTIONS TO COMPONENTS
		_cPeriodFullDay_chk.addActionListener(this);
		
		_cPeriodPanel = panel;
		return panel;
	}

	private JPanel createNoConnectionPanel()
	{
		JPanel panel = SwingUtils.createGPanel("Not Connected", false);
		panel.setLayout(new MigLayout("","",""));   // insets Top Left Bottom Right

		
		//-------------------------------------------------------
		// Examples in the JDBC URL DropDown
		//-------------------------------------------------------
		_ncJdbcUrl_cbx.addItem("");
		// H2
		_ncJdbcUrl_cbx.addItem("jdbc:h2:file:[<path>]<dbname>;IFEXISTS=TRUE");
		_ncJdbcUrl_cbx.addItem("jdbc:h2:tcp://<host>[:<port>]/<dbname>");
		_ncJdbcUrl_cbx.addItem("jdbc:h2:ssl://<host>[:<port>]/<dbname>");
		// SQL-Server
		_ncJdbcUrl_cbx.addItem("jdbc:sqlserver://<host>:<port>;databaseName=<dbname> ");
		_ncJdbcUrl_cbx.addItem("jdbc:sqlserver://<host>:1433;databaseName=<dbname>;integratedSecurity=true");
		// DB2
		_ncJdbcUrl_cbx.addItem("jdbc:db2://<host>/<dbname>");
		// Postgres
		_ncJdbcUrl_cbx.addItem("jdbc:postgresql://<host>/<dbname>");
		// MySQL
		_ncJdbcUrl_cbx.addItem("jdbc:mysql://<host>/<dbname>");


		
		JLabel header_lbl = new JLabel("<html>"
				+ "Create Report, which you are <b>not</b> currently connected to."
				+ "<ul>"
				+ "  <li>Select a H2 database file or a URL where the Recording is stored in</li>"
				+ "  <li>Choose <i>Full Day</i> or the begin/end timw you want to see</li>"
				+ "</ul>"
				+ "</html>");

		// ADD Components
		panel.add(header_lbl,               "span, wrap");

		panel.add(_ncH2dbFileName_lbl,      "");
		panel.add(_ncH2dbFileName_txt,      "split, pushx, growx");
		panel.add(_ncH2dbFileName_but,      "wrap");

//		panel.add(_ncJdbcDriver_lbl,        "");
//		panel.add(_ncJdbcDriver_cbx,        "pushx, growx, wrap");
		
		panel.add(_ncJdbcUrl_lbl,           "");
		panel.add(_ncJdbcUrl_cbx,           "pushx, growx, wrap");
//		panel.add(_ncJdbcUrl_but,           "wrap");
		
		panel.add(_ncJdbcUsername_lbl,      "");
		panel.add(_ncJdbcUsername_txt,      "pushx, growx, wrap");
		                                    
		panel.add(_ncJdbcPassword_lbl,      "");
		panel.add(_ncJdbcPassword_txt,      "split, pushx, growx");
		panel.add(_ncJdbcSavePassword_chk,  "wrap");
		
		panel.add(_ncPeriodFullDay_chk,     "span, wrap");

		panel.add(_ncPeriodBeginHour_lbl,   "");
		panel.add(_ncPeriodBeginHour_sp,    "split");
		panel.add(_ncPeriodBeginMinute_lbl, "");
		panel.add(_ncPeriodBeginMinute_sp,  "wrap");

		panel.add(_ncPeriodEndHour_lbl,     "");
		panel.add(_ncPeriodEndHour_sp,      "split");
		panel.add(_ncPeriodEndMinute_lbl,   "");
		panel.add(_ncPeriodEndMinute_sp,    "wrap");

		panel.add(_cmdLine_lbl,             "");
		panel.add(_cmdLine_txt,             "split, pushx, growx, wrap");


		// Set properties
		_ncJdbcUrl_cbx.setEditable(true);

		// ADD ACTIONS TO COMPONENTS
		_ncH2dbFileName_txt    .addActionListener(this);
		_ncH2dbFileName_but    .addActionListener(this);

		_ncJdbcUrl_cbx         .addActionListener(this);
		_ncJdbcUsername_txt    .addActionListener(this);
		_ncJdbcPassword_txt    .addActionListener(this);

		_ncPeriodFullDay_chk   .addActionListener(this);
		_ncPeriodBeginHour_sp  .addChangeListener(this);
		_ncPeriodBeginMinute_sp.addChangeListener(this);
		_ncPeriodEndHour_sp    .addChangeListener(this);
		_ncPeriodEndMinute_sp  .addChangeListener(this);

		// Focus listeners
		_ncH2dbFileName_txt    .addFocusListener(this);
		_ncJdbcUrl_cbx         .addFocusListener(this);
		_ncJdbcUsername_txt    .addFocusListener(this);
		_ncJdbcPassword_txt    .addFocusListener(this);
		
		// Key listeners
		_ncH2dbFileName_txt    .addKeyListener(this);
		_ncJdbcUrl_cbx         .addKeyListener(this);
		_ncJdbcUrl_cbx.getEditor().getEditorComponent().addKeyListener(this);
		_ncJdbcUsername_txt    .addKeyListener(this);
		_ncJdbcPassword_txt    .addKeyListener(this);
		
		_ncPeriodPanel = panel;
		return panel;
	}

	private JPanel createFeedbackPanel()
	{
		JPanel panel = SwingUtils.createGPanel("Output", true);
		panel.setLayout(new MigLayout("","",""));   // insets Top Left Bottom Right

		_cmdLine_lbl.setToolTipText("This is the Command Line equivalant, if you want to create the report from the command line.");
		_cmdLine_txt.setToolTipText(_cmdLine_lbl.getToolTipText());
		
		_continueInBg_but.setToolTipText("Do not wait for the report to complete. The result will be opened in a Web Browser when it's created...");

//		_outputFile_txt.setToolTipText("If blank a temporary file will be created...");
		_outputFile_cbx.setToolTipText("If blank a temporary file will be created...");

		//-------------------------------------------------------
		// Examples in the OUTPUT FILE DropDown
		//-------------------------------------------------------
		_outputFile_cbx.addItem("");
		_outputFile_cbx.addItem(DEFAULT_OUTPUT_FILENAME_SHORT);
		_outputFile_cbx.addItem(DEFAULT_OUTPUT_FILENAME_LONG);

		// ADD the OK, Cancel, Apply buttons
		panel.add(_outputFile_lbl,   "");
//		panel.add(_outputFile_txt,   "split, pushx, growx");
		panel.add(_outputFile_cbx,   "split, pushx, growx");
		panel.add(_outputFile_but,   "wrap");

		panel.add(_outputFile_chk,   "skip, wrap");

		panel.add(_progress_lbl,     "");
		panel.add(_progress_txt,     "split, pushx, growx, wrap");
		panel.add(_progress_bar,     "skip, split, pushx, growx, hidemode 3");
		panel.add(_continueInBg_but, "wrap, hidemode 3");
		panel.add(_crFilename_txt,   "skip, split, pushx, growx, hidemode 3, wrap");

		_progress_lbl    .setVisible(false);
		_progress_txt    .setVisible(true);
		_progress_bar    .setVisible(false);
		_continueInBg_but.setVisible(false);
		_crFilename_txt  .setVisible(true);
		
		_cmdLine_txt .setEditable(false);
		_progress_txt.setEditable(false);
		_progress_txt.setBorder(null);;

		// Initialize some fields.
		_outputFile_cbx.setEditable(true);
		_crFilename_txt.setEditable(false);
		_crFilename_txt.setBorder(null);

		// ADD ACTIONS TO COMPONENTS
		_outputFile_but  .addActionListener(this);
		_continueInBg_but.addActionListener(this);

//		_outputFile_txt.addActionListener(this);
//		_outputFile_txt.addFocusListener(this);
//		_outputFile_txt.addKeyListener(this);
		_outputFile_cbx.addActionListener(this);
		_outputFile_cbx.addFocusListener(this);
		_outputFile_cbx.addKeyListener(this);
		

//		_outputFile_txt.addKeyListener(new KeyListener()
		_outputFile_cbx.getEditor().getEditorComponent().addKeyListener(new KeyListener()
		{
			@Override
			public void keyReleased(KeyEvent e)
			{
//				String fullName  = _outputFile_txt.getText();
				String fullName  = getComboBoxValue(_outputFile_cbx);
				// fix ${tmpDir}
				fullName = translateOutputFile(fullName);

//				String shortName = new File(fullName).getName().toLowerCase();
//				Boolean selected = shortName.contains("temp") || shortName.contains("tmp");
				
				fullName = fullName.toLowerCase();
				boolean selected = fullName.contains("temp") || fullName.contains("tmp");
				_outputFile_chk.setSelected(selected);
				
				checkForProblem();
			}
			
			@Override public void keyTyped(KeyEvent e) {}
			@Override public void keyPressed(KeyEvent e) {}
		});
		
		_feebackPanel = panel;
		return panel;
	}

	private JPanel createOkCancelPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("","",""));   // insets Top Left Bottom Right

		_warning.setForeground(Color.RED);
		
		// ADD the OK, Cancel, Apply buttons
		panel.add(_warning,"growx, pushx");
		panel.add(_ok,     "tag ok");
		panel.add(_cancel, "tag cancel");
//		panel.add(_apply,  "tag apply");

//		_apply.setEnabled(false);

		// Initialize some fields.

		// ADD ACTIONS TO COMPONENTS
		_ok           .addActionListener(this);
		_cancel       .addActionListener(this);
//		_apply        .addActionListener(this);

		return panel;
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{
		checkForProblem();
	}

	// Focus Listeners
	@Override public void focusGained(FocusEvent e) { }
	@Override public void focusLost(FocusEvent e) { checkForProblem(); }

	// Key Listeners
	@Override public void keyTyped(KeyEvent e) {}
	@Override public void keyPressed(KeyEvent e) {}
	@Override public void keyReleased(KeyEvent e) { System.out.println("keyReleased(): e="+e); checkForProblem(); }

	private static String getComboBoxValue(JComboBox<String> cbx)
	{
		if (cbx == null)
			return "";
		
		Object obj = cbx.getSelectedItem();
		if (obj == null)
			return "";
		
//		String fullName  = cbx.getEditor().getItem().toString();
		JTextComponent editor = (JTextComponent) cbx.getEditor().getEditorComponent();
		if (editor != null)
			return editor.getText();

		return obj.toString();
	}
	
	private void setInfoFields()
	{
		// Enable/Disable: Begin/end time fields
		_cPeriodBeginHour_sp   .setEnabled( ! _cPeriodFullDay_chk.isSelected() );
		_cPeriodBeginMinute_sp .setEnabled( ! _cPeriodFullDay_chk.isSelected() );
		_cPeriodEndHour_sp     .setEnabled( ! _cPeriodFullDay_chk.isSelected() );
		_cPeriodEndMinute_sp   .setEnabled( ! _cPeriodFullDay_chk.isSelected() );
		
		_ncPeriodBeginHour_sp  .setEnabled( ! _ncPeriodFullDay_chk.isSelected() );
		_ncPeriodBeginMinute_sp.setEnabled( ! _ncPeriodFullDay_chk.isSelected() );
		_ncPeriodEndHour_sp    .setEnabled( ! _ncPeriodFullDay_chk.isSelected() );
		_ncPeriodEndMinute_sp  .setEnabled( ! _ncPeriodFullDay_chk.isSelected() );

		// set CMD LINE text field
		String cmdLineBase = StringUtil.envVariableSubstitution(PlatformUtils.isWindows() 
				? "${DBXTUNE_HOME}\\bin\\dsr.bat" 
				: "${DBXTUNE_HOME}/bin/dsr.sh");

		if (StringUtil.hasValue(getComboBoxValue(_ncJdbcUrl_cbx)))
			cmdLineBase += " --url '" + getComboBoxValue(_ncJdbcUrl_cbx) + "'";

		if (StringUtil.hasValue(_ncJdbcUsername_txt.getText()))
			cmdLineBase += " --username '" + _ncJdbcUsername_txt.getText() + "'";
		
		if (StringUtil.hasValue(_ncJdbcPassword_txt.getText()))
			cmdLineBase += " --password '" + _ncJdbcPassword_txt.getText() + "'";

		if ( ! _ncPeriodFullDay_chk.isSelected() )
		{
			String beginTimeH = StringUtils.right("00" + _ncPeriodBeginHour_spm  .getNumber(), 2);
			String beginTimeM = StringUtils.right("00" + _ncPeriodBeginMinute_spm.getNumber(), 2);
			String endTimeH   = StringUtils.right("00" + _ncPeriodEndHour_spm    .getNumber(), 2);
			String endTimeM   = StringUtils.right("00" + _ncPeriodEndMinute_spm  .getNumber(), 2);
			
			cmdLineBase += " --begin-time '" + beginTimeH + ":" + beginTimeM + "'";
			cmdLineBase += " --end-time '"   + endTimeH   + ":" + endTimeM   + "'";
		}

//		if (StringUtil.hasValue(_outputFile_txt.getText()))
//			cmdLineBase += " --ofile '" + _outputFile_txt.getText() + "'";
		if (StringUtil.hasValue(getComboBoxValue(_outputFile_cbx)))
			cmdLineBase += " --ofile '" + getComboBoxValue(_outputFile_cbx) + "'";

		_cmdLine_txt.setText(cmdLineBase);
		_cmdLine_txt.setCaretPosition(0);
	}

	private void checkForProblem()
	{
		String warning = "";

		String inTabName = _tabPane.getSelectedTitle(false);

		if (TAB_NOT_CONNECTED.equals(inTabName))
		{
			// empty URL
			if ( StringUtil.isNullOrBlank(getComboBoxValue(_ncJdbcUrl_cbx)) )
			{
				if (StringUtil.hasValue(warning)) 
					warning += ", ";
				warning += "No URL has been specified";
			}
			
			// H2 DB File
			if ( StringUtil.hasValue( _ncH2dbFileName_txt.getText()) && ! _ncH2dbFileName_txt.getText().endsWith(".mv.db") )
			{
				if (StringUtil.hasValue(warning)) 
					warning += ", ";
				warning += "H2 Database File = has to end with '.mv.db'";
			}
			
			// URL place holders
			if ( getComboBoxValue(_ncJdbcUrl_cbx).indexOf("<") != -1 || getComboBoxValue(_ncJdbcUrl_cbx).indexOf(">") != -1  )
			{
				if (StringUtil.hasValue(warning)) 
					warning += ", ";
				warning += "Found placeholders '<>' in the URL";
			}
		}

		// Output file
//		String currentOutputFile = _outputFile_txt.getText();
		String currentOutputFile = getComboBoxValue(_outputFile_cbx);
		if ( StringUtil.hasValue(currentOutputFile) && ! currentOutputFile.toLowerCase().endsWith(".html") )
		{
			if (StringUtil.hasValue(warning)) 
				warning += ", ";
			warning += "Output file must end with '.html'";
		}
		// check if the output DIR exists
		if ( StringUtil.hasValue(currentOutputFile) )
		{
			// fix ${tmpDir}
			currentOutputFile = translateOutputFile(currentOutputFile);

			File f = new File(currentOutputFile);
			File dir = f.getParentFile();
			if (dir != null)
			{
				if (! dir.exists())
				{
					if (StringUtil.hasValue(warning)) 
						warning += ", ";
					warning += "Output directory '" + f.getParent() + "' do not exists.";
				}
			}
		}

		setInfoFields();

		_warning.setText(warning);
		// enable/disable OK button
		if ( ! _bgWorkerIsExecuting )
			_ok.setEnabled( ! StringUtil.hasValue(warning) );
	}
	
	private String translateOutputFile(String filename)
	{
		filename = filename.replace("${tmpDir}" , System.getProperty("java.io.tmpdir"));
		// ${srvName}, ${beginTime} and ${endTime} is not yet known... so we can't translate them
		
		return filename;
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		checkForProblem();

		if (e == null)
			return;
		
		Object source = e.getSource();

		// --- BUTTON: H2 DB File ---
		if (_ncH2dbFileName_but.equals(source))
		{
			String envName = "DBXTUNE_SAVE_DIR";
			String saveDir = StringUtil.getEnvVariableValue(envName);
					
			JFileChooser fc = new JFileChooser(_ncH2dbFileLastLoaded);
//			JFileChooser fc = new JFileChooser();
			if (saveDir != null)
				fc.setCurrentDirectory(new File(saveDir));

			int returnVal = fc.showOpenDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION) 
	        {
				File file = fc.getSelectedFile();
				_ncH2dbFileLastLoaded = file.toString();

				_ncH2dbFileName_txt.setText(file.toString());
				
				if ( ! file.toString().endsWith(".mv.db") )
				{
					SwingUtils.showErrorMessage(this, "Not a H2 Database File", "This is NOT a H2 Database File... it should end with '.mv.db'", null);
					return;
				}
				
				String tmpUrl = H2DB_FILE_TEMPLATE.replace("${filename}", _ncH2dbFileLastLoaded.replace(".mv.db", ""));
				_ncJdbcUrl_cbx.setSelectedItem(tmpUrl);
	        }
		}

//		// --- BUTTON: JDBC URL ---
//		if (_ncJdbcUrl_but.equals(source))
//		{
//		}

		// --- BUTTON: Output File ---
		if (_outputFile_but.equals(source))
		{
			String envName = "DBXTUNE_SAVE_DIR";
			String saveDir = StringUtil.getEnvVariableValue(envName);
					
			JFileChooser fc = new JFileChooser(_outputFileLast);
//			JFileChooser fc = new JFileChooser();
			if (saveDir != null)
				fc.setCurrentDirectory(new File(saveDir));

			int returnVal = fc.showOpenDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION) 
	        {
				File file = fc.getSelectedFile();
				_outputFileLast = file.toString();

//				_outputFile_txt.setText(_outputFileLast);
				_outputFile_cbx.setSelectedItem(_outputFileLast);
	        }
		}

		// --- BUTTON: Continue in BG ---
		if (_continueInBg_but.equals(source))
		{
			setVisible(false);
		}

		// --- BUTTON: CANCEL ---
		if (_cancel.equals(source))
		{
			doCancel();
		}

		// --- BUTTON: OK ---
		if (_ok.equals(source))
		{
			if (StringUtil.isNullOrBlank(_warning.getText()))
			{
				doApply();
				saveProps();
//				setVisible(false);
			}
		}

		// after any actions check for problems
		checkForProblem();
	}

	private void doApply()
	{
		Configuration tmpCfg = Configuration.getInstance(Configuration.USER_TEMP);
		if (tmpCfg == null)
			return;

//		for (DbmsConfigIssue issue : _dbmsConfigIssuesList)
//		{
//			String  key = issue.getDiscardPropKey();
//			boolean val = issue.isDiscarded();
//			
//			tmpCfg.setProperty(key, val);
//		}
//		tmpCfg.save();
		
		createReport();
	}

	private void doCancel()
	{
		if ( _bgWorker != null && ! _bgWorker.isDone() )
		{
			// Show a message
			String htmlMsg = "<html>"
					+ "<h3>A Report is currently under creation</h3>"
					+ "</html>";

			Object[] options = {
					"Continue in Background & Close Dialog",
					"Stop Current Report Creation & Close Dialog",
					"Cancel"
					};

			int answer = JOptionPane.showOptionDialog(this, 
					htmlMsg,
					"Cancel Action", // title
					JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					null,     //do not use a custom Icon
					options,  //the titles of buttons
					options[0]); //default button title

			if (answer == 0) // Continue in background
			{
				setVisible(false); // NOTE this MUST be FALSE, otherwise H2 Databases may be corrupted.
			}
			else if (answer == 1) // Stop Report
			{
				_bgWorker.cancel(true);
				setVisible(false);
			}
			else if (answer == 2) // Cancel
			{
				// Do nothing
			}
		}
		else
		{
			setVisible(false);
		}
	}

	private void createReport()
	{
		_crFilename_txt  .setVisible(false);
		_progress_lbl    .setVisible(true);
		_progress_txt    .setVisible(true);
		_progress_bar    .setVisible(true);
		_continueInBg_but.setVisible(true);
		_ok              .setEnabled(false);

		// If we want to resize to both height and width
		//pack();
		
//		ReportSwingWorker bgWorker = new ReportSwingWorker(this, _outputFile_txt.getText());
		_bgWorker = new ReportSwingWorker(this, getComboBoxValue(_outputFile_cbx));
		
		_bgWorker.execute();
	}

	/*---------------------------------------------------
	** BEGIN: Property handling
	**---------------------------------------------------
	*/
	private void saveProps()
  	{
		Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
		String base = this.getClass().getSimpleName() + ".";

		if (tmpConf != null)
		{
			tmpConf.setLayoutProperty(base + "window.width", this.getSize().width);
			tmpConf.setLayoutProperty(base + "window.height", this.getSize().height);
			tmpConf.setLayoutProperty(base + "window.pos.x", this.getLocationOnScreen().x);
			tmpConf.setLayoutProperty(base + "window.pos.y", this.getLocationOnScreen().y);

			tmpConf.save();
		}
  	}

  	private void loadProps()
  	{
		int     width     = SwingUtils.hiDpiScale(1160);  // initial window with   if not opened before
		int     height    = SwingUtils.hiDpiScale(700);   // initial window height if not opened before
		int     x         = -1;
		int     y         = -1;

//		Configuration tmpConf = Configuration.getInstance(Configuration.TEMP);
		Configuration tmpConf = Configuration.getCombinedConfiguration();
		String base = this.getClass().getSimpleName() + ".";

		setSize(width, height);

		if (tmpConf == null)
			return;

		width  = tmpConf.getLayoutProperty(base + "window.width",  width);
		height = tmpConf.getLayoutProperty(base + "window.height", height);
		x      = tmpConf.getLayoutProperty(base + "window.pos.x",  -1);
		y      = tmpConf.getLayoutProperty(base + "window.pos.y",  -1);

		if (width != -1 && height != -1)
		{
			setSize(width, height);
		}
		if (x != -1 && y != -1)
		{
			if ( ! SwingUtils.isOutOfScreen(x, y, width, height) )
				this.setLocation(x, y);
		}
		else
		{
			SwingUtils.centerWindow(this);
		}
	}
	/*---------------------------------------------------
	** END: Property handling
	**---------------------------------------------------
	*/

//  	@Override
//	public DbxConnection getConnection()
//	{
//		return _connProvider.getConnection();
//	}
//	@Override
//	public DbxConnection getNewConnection(String appname)
//	{
//		return _connProvider.getNewConnection(appname);
//	}

	private static class ProgressEntry
	{
		IReportEntry reportEntry;
		String       msg;
		int          pctDone;
		
		public ProgressEntry(IReportEntry reportEntry, String msg, int pctDone)
		{
			this.reportEntry = reportEntry;
			this.msg         = msg        ;
			this.pctDone     = pctDone    ;
		}
	}
	
	private static class ReportSwingWorker
	extends SwingWorker<Void, ProgressEntry> 
	{
		private final String _outputFilename;
		private File         _outputFile;
		private DailySummaryReportDialog _dsrd;

		public ReportSwingWorker(DailySummaryReportDialog dsrd, String outputFilename)
		{
			_dsrd = dsrd;
			_outputFilename = outputFilename;
		}
 
		@Override
		protected Void doInBackground() throws Exception
		{
			DbxConnection conn = null;
			_dsrd._bgWorkerIsExecuting = true;

			String inTabName = _dsrd._tabPane.getSelectedTitle(false);
			String jdbcUrl  = null;
			boolean createdConnection = false;

			if (DailySummaryReportDialog.TAB_CONNECTED.equals(inTabName))
			{
				if (_dsrd._pcsJdbcWriterConnProps == null)
				{
					// Get a OFFLINE connection using ConnectionProvider
					conn = _dsrd._connProvider.getConnection();
				}
				else
				{
					jdbcUrl = _dsrd._pcsJdbcWriterConnProps.getUrl();

					// Create a new connection to the PCS 
					_logger.info("Connecting to Current - Persistence Counter Storage - URL: " + jdbcUrl);
					publish(new ProgressEntry(null, "Connecting to Current - Persistence Counter Storage - URL: " + jdbcUrl, 0));

					conn = DbxConnection.connect(null, _dsrd._pcsJdbcWriterConnProps);
					createdConnection = true;

					// - Check if it's a PCS database
					if ( ! PersistReader.isOfflineDb(conn) )
						throw new RuntimeException("This do NOT look like a DbxTune recording... can't continue.");
				}
			}
			else
			{
//				String  jdbcUrl  = _dsrd._ncJdbcUrl_cbx.getSelectedItem().toString();
				        jdbcUrl  = getComboBoxValue(_dsrd._ncJdbcUrl_cbx);
				String  jdbcUser = _dsrd._ncJdbcUsername_txt.getText();
				String  jdbcPass = _dsrd._ncJdbcPassword_txt.getText();

				ConnectionProp cp = new ConnectionProp();
				cp.setUrl(jdbcUrl);
				cp.setUsername(jdbcUser);
				cp.setPassword(jdbcPass);

				// Connect
				_logger.info("Connecting to URL: " + jdbcUrl);
				publish(new ProgressEntry(null, "Connecting to URL: " + jdbcUrl, 0));

				conn = DbxConnection.connect(null, cp);
				createdConnection = true;
				
				// - Check if it's a PCS database
				// - get DbxTune TYPE (AseTune, RsTune, SqlServerTune, etc...)
				// - get Server Name
				// - get start/first sample Timestamp
				// - get   end/last  sample Timestamp
				if ( ! PersistReader.isOfflineDb(conn) )
					throw new RuntimeException("This do NOT look like a DbxTune recording... can't continue.");
			}

			MonRecordingInfo monRecordingInfo = PersistReader.getMonRecordingInfo(conn, null);
//			MonVersionInfo   monVersionInfo   = PersistReader.getMonVersionInfo(conn, null);
			
			String dbxCollector  = monRecordingInfo.getRecDbxAppName();
			String reportSrvName = monRecordingInfo.getDbmsServerName();
//System.out.println("dbxCollector='"+dbxCollector+"'.");
//System.out.println("reportSrvName='"+reportSrvName+"'.");

			System.getProperties().setProperty(DailySummaryReportFactory.PROPKEY_reportClassname, "com.dbxtune.pcs.report.DailySummaryReport" + dbxCollector);
			System.getProperties().setProperty(DailySummaryReportFactory.PROPKEY_senderClassname, "com.dbxtune.pcs.report.senders.ReportSenderNoOp");
			
			IDailySummaryReport report = DailySummaryReportFactory.createDailySummaryReport();
			if (report == null)
			{
				_logger.error("Daily Summary Report: create did not pass a valid report instance, skipping report creation.");
				throw new RuntimeException("Daily Summary Report: create did not pass a valid report instance, skipping report creation.");
			}

			// Set a "Report Progress" object
			report.setProgressReporter(new IProgressReporter()
			{
				@Override
				public boolean setProgress(IProgressReporter.State state, IReportEntry entry, String msg, int guessedPercentDone)
				{
					if (IProgressReporter.State.BEFORE.equals(state))
						return true;

					//System.out.println("DailySummaryReport.setProgress(status='"+msg+"', guessedPercentDone="+guessedPercentDone+")");
					publish(new ProgressEntry(entry, msg, guessedPercentDone));
					
					if (isCancelled())
						return false;
					return true;
				}
			});


			report.setConnection(conn);
			report.setServerName(reportSrvName);

			if (DailySummaryReportDialog.TAB_CONNECTED.equals(inTabName))
			{
				if ( ! _dsrd._cPeriodFullDay_chk.isSelected() )
				{
					int beginHour   = _dsrd._cPeriodBeginHour_spm  .getNumber().intValue();
					int beginMinute = _dsrd._cPeriodBeginMinute_spm.getNumber().intValue();
					int endHour     = _dsrd._cPeriodEndHour_spm    .getNumber().intValue();
					int endMinute   = _dsrd._cPeriodEndMinute_spm  .getNumber().intValue();
					
					report.setReportPeriodBeginTime(beginHour, beginMinute);
					report.setReportPeriodEndTime(  endHour,   endMinute);
				}
			}
			if (DailySummaryReportDialog.TAB_NOT_CONNECTED.equals(inTabName))
			{
				if ( ! _dsrd._ncPeriodFullDay_chk.isSelected() )
				{
					int beginHour   = _dsrd._ncPeriodBeginHour_spm  .getNumber().intValue();
					int beginMinute = _dsrd._ncPeriodBeginMinute_spm.getNumber().intValue();
					int endHour     = _dsrd._ncPeriodEndHour_spm    .getNumber().intValue();
					int endMinute   = _dsrd._ncPeriodEndMinute_spm  .getNumber().intValue();
					
					report.setReportPeriodBeginTime(beginHour, beginMinute);
					report.setReportPeriodEndTime(  endHour,   endMinute);
				}
			}

			
			// INFO
			publish(new ProgressEntry(null, "Creating Report for '" + reportSrvName + "' recorded by '" + dbxCollector + "'.", 1));

			// Initialize the Report, which also initialized the ReportSender
			report.init();

			// Create & and Send the report
			report.create();
//			report.send();

			// Save the report
//			report.save();
			// ReUse the above save()... and save it to the OUTPUT FILE 

			// Get Content and the HTML output
			DailySummaryReportContent content = report.getReportContent();
//			String htmlReport = content.getReportAsHtml();
			
//			RecordingInfo recInfo = content.getRecordingInfo();
//			Timestamp reportBeginTime    = report.getReportBeginTime();
//			Timestamp reportEndTime      = report.getReportEndTime();
			String    reportBeginDateStr = TimeUtils.getCurrentTimeForFileNameYmd(report.getReportBeginTime().getTime());
			String    reportBeginTimeStr = TimeUtils.getCurrentTimeForFileNameHm (report.getReportBeginTime().getTime());
			String    reportEndDateStr   = TimeUtils.getCurrentTimeForFileNameYmd(report.getReportEndTime()  .getTime());
			String    reportEndTimeStr   = TimeUtils.getCurrentTimeForFileNameHm (report.getReportEndTime()  .getTime());

			// Compose a filename (which we save the results to)
			String outFilename = _outputFilename;
			if ( ! StringUtil.isNullOrBlank(outFilename) )
			{
				outFilename = DEFAULT_OUTPUT_FILENAME_SHORT;
			}
			// Translate variables in the filename to a "real" filename
			outFilename = outFilename.replace("${tmpDir}"   , System.getProperty("java.io.tmpdir"));
			outFilename = outFilename.replace("${srvName}"  , com.dbxtune.utils.FileUtils.toSafeFileName(reportSrvName));
			outFilename = outFilename.replace("${beginDate}", reportBeginDateStr);
			outFilename = outFilename.replace("${beginTime}", reportBeginTimeStr);
			outFilename = outFilename.replace("${endDate}"  , reportEndDateStr);
			outFilename = outFilename.replace("${endTime}"  , reportEndTimeStr);
			_outputFile = new File(outFilename);

			// Write the file
//			try
//			{
				_logger.info("Saving of DailyReport to file '" + _outputFile.getAbsolutePath() + "'.");
				
				if (_dsrd._outputFile_chk.isSelected())
					_outputFile.deleteOnExit();
				
//				FileUtils.write(_outputFile, htmlReport, StandardCharsets.UTF_8.name());
				content.saveReportAsFile(_outputFile);
//			}
//			catch (IOException ex)
//			{
//				_logger.error("Problems writing Daily Report to file '" + saveToFile + "'. Caught: "+ex, ex);
//			}
			
			if (createdConnection)
			{
				_logger.info("Closing DBMS Connection to url='" + jdbcUrl + "'. conn=" + conn);
				conn.closeNoThrow();
			}
//			if (DailySummaryReportDialog.TAB_NOT_CONNECTED.equals(inTabName))
//			{
//				_logger.info("Closing DBMS Connection to url='" + jdbcUrl + "'. conn=" + conn);
//				conn.closeNoThrow();
//			}

			_logger.info("End of Create Dialy Summary Report for server '" + reportSrvName + "', report file was written to '" + _outputFile + "'.");

			
			return null;
		}

	    @Override
		protected void process(List<ProgressEntry> chunks)
		{
			  // The last value in this array is all we care about.
			ProgressEntry entry = chunks.get( chunks.size() - 1 );

			_dsrd._progress_bar.setValue(entry.pctDone);

//			_dsrd._progress_lbl.setText("Creating ReportEntry '" + entry.reportEntry.getClass().getSimpleName() + "', with Subject '" + entry.reportEntry.getSubject() + "'.");
			_dsrd._progress_txt.setText(entry.msg);
			_dsrd._progress_txt.setCaretPosition(0);
		}

		@Override
		protected void done()
		{
			try
			{
				// This will throw exception in issues
				get();

				_dsrd._progress_txt.setText("Open Web Browser");
				
				if (Desktop.isDesktopSupported())
				{
					Desktop desktop = Desktop.getDesktop();
					if ( desktop.isSupported(Desktop.Action.BROWSE) )
					{
//						File f = new File(_dsrd._outputFile_txt.getText());
						File f = _outputFile;
						try
						{
							desktop.browse(f.toURI());
						}
						catch (Exception ex)
						{
							SwingUtils.showErrorMessage(null, "Problems Open HTML", "Problems when open the URL '"+f.toURI()+"'.", ex);
						}
					}
				}

//				_dsrd._progress_txt.setText("The Report has been created... Check your Web Browser, " + _outputFile.getAbsolutePath());
				_dsrd._progress_txt.setText("The Report has been created... Check your Web Browser");
				_dsrd._progress_txt.setCaretPosition(0);

				_dsrd._crFilename_txt.setText(_outputFile.getAbsolutePath());
				_dsrd._crFilename_txt.setCaretPosition(0);

				_dsrd._progress_lbl    .setVisible(false);
//				_dsrd._progress_txt    .setVisible(false);
				_dsrd._progress_bar    .setVisible(false);
				_dsrd._continueInBg_but.setVisible(false);
				_dsrd._crFilename_txt  .setVisible(true);
				_dsrd._ok              .setEnabled(true);
				_dsrd._bgWorkerIsExecuting = false;
			}
			catch (CancellationException | InterruptedException ex)
			{
				_dsrd._progress_txt.setText("Report Creation was Cancelled/Interrupted.");
				_dsrd._progress_txt.setCaretPosition(0);

				_dsrd._progress_lbl    .setVisible(false);
//				_dsrd._progress_txt    .setVisible(false);
				_dsrd._progress_bar    .setVisible(false);
				_dsrd._continueInBg_but.setVisible(false);
				_dsrd._crFilename_txt  .setVisible(true);
				_dsrd._ok              .setEnabled(true);
				_dsrd._bgWorkerIsExecuting = false;
			}
			catch (Exception ex)
			{
				SwingUtils.showErrorMessage(_dsrd, "Problems Creating Daily Summary Report", "Problems Creating Daily Summary Report: " + ex, ex);

				_dsrd._progress_txt.setText("Problems Creating Report: " + ex.getMessage());
				_dsrd._progress_txt.setCaretPosition(0);

				_dsrd._progress_lbl    .setVisible(false);
//				_dsrd._progress_txt    .setVisible(false);
				_dsrd._progress_bar    .setVisible(false);
				_dsrd._continueInBg_but.setVisible(false);
				_dsrd._crFilename_txt  .setVisible(true);
				_dsrd._ok              .setEnabled(true);
				_dsrd._bgWorkerIsExecuting = false;
			}
		}
	}
}
