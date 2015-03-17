package com.asetune.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import com.asetune.AseTune;
import com.asetune.CounterController;
import com.asetune.cm.CountersModel;
import com.asetune.ui.rsyntaxtextarea.AsetuneSyntaxConstants;
import com.asetune.ui.rsyntaxtextarea.RSyntaxUtilitiesX;
import com.asetune.utils.Configuration;
import com.asetune.utils.RTextUtility;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;
import com.asetune.utils.Ver;

public class ShowCmPropertiesDialog
extends JDialog implements ActionListener, ChangeListener
{
	private static final long	serialVersionUID	= 1L;

	// I used: http://www.tayloredmktg.com/rgb/ to get colors
	private static final Color PK_COLOR   = new Color(238, 221, 130); // Light Goldenrod
	private static final Color DIFF_COLOR = new Color(100, 149, 237); // Cornflower Blue
	private static final Color PCT_COLOR  = new Color(205, 92,  92 ); // Indian Red

	
	private JButton            _ok_but                = new JButton("OK");

	// Panel where most of the other stuff is sitting on, used for windows size calculation.
	private JPanel             _xpanel                = null;
	private JScrollPane        _xscroll               = null;

	private JLabel             _cmShortName_lbl       = new JLabel("Short Name");
	private JTextField         _cmShortName_txt       = new JTextField();
	private JLabel             _cmLongName_lbl        = new JLabel("Tab Name");
	private JTextField         _cmLongName_txt        = new JTextField();

	private JLabel             _initialized_true_lbl  = new JLabel("<html>This Performance Counter has been initialized.</html>");
	private JLabel             _initialized_false_lbl = new JLabel("<html>This Performance Counter has <b>not</b> yet been initialized.</html>");

	// Version info
	private JLabel             _testVersion_lbl      = new JLabel("Server Version");
	private JLabel             _testVersionMajor_lbl = new JLabel("Major");
	private SpinnerNumberModel _testVersionMajor_spm = new SpinnerNumberModel(12, 12, 99, 1); // value, min, max, step
	private JSpinner           _testVersionMajor_sp  = new JSpinner(_testVersionMajor_spm);

	private JLabel             _testVersionMinor_lbl = new JLabel("Minor");
	private SpinnerNumberModel _testVersionMinor_spm = new SpinnerNumberModel(5, 0, 9, 1); // value, min, max, step
	private JSpinner           _testVersionMinor_sp  = new JSpinner(_testVersionMinor_spm);

	private JLabel             _testVersionMaint_lbl = new JLabel("Maint");
	private SpinnerNumberModel _testVersionMaint_spm = new SpinnerNumberModel(0, 0, 9, 1); // value, min, max, step
	private JSpinner           _testVersionMaint_sp  = new JSpinner(_testVersionMaint_spm);

	private JLabel             _testVersionEsd_lbl   = new JLabel("ESD#/SP");
	private SpinnerNumberModel _testVersionEsd_spm   = new SpinnerNumberModel(3, 0, 999, 1); // value, min, max, step
	private JSpinner           _testVersionEsd_sp    = new JSpinner(_testVersionEsd_spm);

	private JLabel             _testVersionPl_lbl    = new JLabel("Patch Level");
	private SpinnerNumberModel _testVersionPl_spm    = new SpinnerNumberModel(0, 0, 99, 1); // value, min, max, step
	private JSpinner           _testVersionPl_sp     = new JSpinner(_testVersionPl_spm);

	private JCheckBox          _testVersionIsCe_chk  = new JCheckBox("Cluster Edition", false);

	private JLabel             _testVersionShort_lbl   = new JLabel("Server Short Version");
	private JTextField         _testVersionShort_txt   = new JTextField();
	private JTextField         _testVersionInt_txt     = new JTextField();
	
	// Fields
	private JLabel             _sqlInit_lbl          = new JLabel("SQL Init - Only executed once, before first sample");
	private RSyntaxTextArea	   _sqlInit              = new RSyntaxTextArea();
//	private RTextScrollPane    _sqlInitScroll        = new RTextScrollPane(_sqlInit);

	private JLabel             _sqlExec_lbl          = new JLabel("SQL - Executed on every sample");
	private RSyntaxTextArea	   _sqlExec              = new RSyntaxTextArea();
//	private RTextScrollPane    _sqlExecScroll        = new RTextScrollPane(_sqlExec);

	private JLabel             _sqlWhere_lbl         = new JLabel("Extra Where");
	private RSyntaxTextArea	   _sqlWhere             = new RSyntaxTextArea();
//	private RTextScrollPane    _sqlWhereScroll       = new RTextScrollPane(_sqlWhere);

	private JLabel             _sqlClose_lbl         = new JLabel("SQL Close - Only executed once, when the connetion is closed");
	private RSyntaxTextArea	   _sqlClose             = new RSyntaxTextArea();
//	private RTextScrollPane    _sqlCloseScroll       = new RTextScrollPane(_sqlClose);

	private JLabel             _pkCols_lbl           = new JLabel("Primary Key Columns");
	private JTextField         _pkCols_txt           = new JTextField();
	private JCheckBox          _pkColsHighlight_chk  = new JCheckBox();

	private JLabel             _diffCols_lbl         = new JLabel("Diff Columns");
	private JTextField         _diffCols_txt         = new JTextField();
	private JCheckBox          _diffColsHighlight_chk= new JCheckBox();
	
	private JLabel             _pctCols_lbl          = new JLabel("Percent Columns");
	private JTextField         _pctCols_txt          = new JTextField();
	private JCheckBox          _pctColsHighlight_chk = new JCheckBox();

	private JLabel             _needConfig_lbl       = new JLabel("Need Server Configuration");
	private JTextField         _needConfig_txt       = new JTextField();

	private JLabel             _needRole_lbl         = new JLabel("Need Server Roles");
	private JTextField         _needRole_txt         = new JTextField();

	private JLabel             _needAseVersion_lbl   = new JLabel("Need Server Version");
	private JTextField         _needAseVersion_txt   = new JTextField();

	private JLabel             _needAseCeVersion_lbl = new JLabel("Need Cluster Edition");
	private JTextField         _needAseCeVersion_txt = new JTextField();

	private JLabel             _dependsOnCm_lbl      = new JLabel("Depends on Performance Counter");
	private JTextField         _dependsOnCm_txt      = new JTextField();

	CountersModel _cm = null;

	public ShowCmPropertiesDialog(Frame owner, Icon icon, CountersModel cm)
	{
		super(owner);

		if ( icon != null && icon instanceof ImageIcon )
			setIconImage(((ImageIcon) icon).getImage());

		_cm = cm;
		
		initComponents();

		// Set initial size
		pack();

		int winMinWidth  = 590;
		int winMinHeight = 725;
		int compWidth    = _sqlExec.getPreferredSize().width + 75; // +75 for various margins
		int compHeight   = _xpanel.getPreferredSize().height + 85; // +85 for bottom space + OK Button
		int width  = Math.max(winMinWidth,  compWidth);
		int height = Math.max(winMinHeight, compHeight);
		Dimension size = SwingUtils.getSizeWithingScreenLimit(width, height, 10);

		setPreferredSize(size);
		setSize(size);

		setLocationRelativeTo(owner);

		scrollToTop();
	}
	
	protected void initComponents()
	{
		setTitle("Properties: " + _cm.getName());

		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("ins 0", "[fill]", ""));

		_xpanel = init();
		_xscroll = new JScrollPane( _xpanel );
		_xscroll.getVerticalScrollBar()  .setUnitIncrement(16);
		_xscroll.getHorizontalScrollBar().setUnitIncrement(16);

//		xscroll.setMinimumSize(new Dimension(500, 1000));
//		xscroll.setPreferredSize(new Dimension(500, 1000));
//		xscroll.setMinimumSize(xpanel.getMinimumSize());
//		xscroll.setPreferredSize(xpanel.getPreferredSize());

//		Dimension screenSize  = Toolkit.getDefaultToolkit().getScreenSize();
//		Dimension xPrefSize = xpanel.getPreferredSize();
//		Dimension xMinSize  = xpanel.getMinimumSize();

//		int width  = Math.min(screenSize.width  - 100, 500);
//		int height = Math.min(screenSize.height - 100, 1000);
//		xscroll.setPreferredSize(new Dimension(width, height));
//		xscroll.setMinimumSize  (new Dimension(width, height));
		

		panel.add(_xscroll, "height 100%, wrap 15");
//		panel.add(init(), "height 100%, wrap 15");
		panel.add(_ok_but, "tag ok, gapright 15, bottom, right, pushx, wrap 15");
		
		setContentPane(panel);

		// ADD ACTIONS TO COMPONENTS
		_ok_but.addActionListener(this);

		_testVersionMajor_spm.addChangeListener(this);
		_testVersionMinor_spm.addChangeListener(this);
		_testVersionMaint_spm.addChangeListener(this);
		_testVersionEsd_spm  .addChangeListener(this);
		_testVersionPl_spm   .addChangeListener(this);

		_testVersionShort_txt .addActionListener(this);
		_testVersionIsCe_chk  .addActionListener(this);
		_pkColsHighlight_chk  .addActionListener(this);
		_diffColsHighlight_chk.addActionListener(this);
		_pctColsHighlight_chk .addActionListener(this);
	}

	protected JPanel init()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("", "", ""));

		JPanel namePanel    = SwingUtils.createPanel("Name Information",  false);
		JPanel aseVerPanel  = SwingUtils.createPanel("Server Version",    true);
		JPanel sqlPanel     = SwingUtils.createPanel("SQL Information",   true);
		JPanel otherPanel   = SwingUtils.createPanel("Other Information", true);

		namePanel  .setLayout(new MigLayout("", "", ""));
		aseVerPanel.setLayout(new MigLayout("", "", ""));
		sqlPanel   .setLayout(new MigLayout("", "", ""));
		otherPanel .setLayout(new MigLayout("", "", ""));

		//------------ BEGIN: TOOLTIP
		namePanel  .setToolTipText("<html>What is this Performance Counters name.</html>");
		aseVerPanel.setToolTipText("<html>Change Server Version to see what SQL will be used on other Server Versions</html>");
		sqlPanel   .setToolTipText("<html>What SQL statements are executed to get Performance Counters.<br>Change Server Version in above panel to see what SQL will be used on other Server Versions</html>");
//		otherPanel .setToolTipText("");

		_ok_but               .setToolTipText("<html>This panel is <b>read only</b>, so nothing is changed for this Performance Counter when you click OK</html>");

		_cmShortName_lbl      .setToolTipText("<html>Short name for this Performance Counter</html>");
		_cmShortName_txt      .setToolTipText("<html>Short name for this Performance Counter</html>");
		_cmLongName_lbl       .setToolTipText("<html>The name that is presented in the Tab Panel for this Performance Counter</html>");
		_cmLongName_txt       .setToolTipText("<html>The name that is presented in the Tab Panel for this Performance Counter</html>");

		_initialized_true_lbl .setToolTipText("<html>You have a connection to an Server</html>");
		_initialized_false_lbl.setToolTipText("<html>No connection to any Server, so you need to specify for what Server Version you want information on.</html>");


		_testVersion_lbl      .setToolTipText("<html>Specify what Server Version you want to see information about</html>");
		_testVersionMajor_lbl .setToolTipText("<html>Major version of the Server, Example: <b>15</b>.0.3</html>");
		_testVersionMajor_sp  .setToolTipText("<html>Major version of the Server, Example: <b>15</b>.0.3</html>");

		_testVersionMinor_lbl .setToolTipText("<html>Minor version of the Server, Example: 15.<b>0</b>.3</html>");
		_testVersionMinor_sp  .setToolTipText("<html>Minor version of the Server, Example: 15.<b>0</b>.3</html>");

		_testVersionMaint_lbl .setToolTipText("<html>Mintenance version of the Server, Example: 15.0.<b>3</b></html>");
		_testVersionMaint_sp  .setToolTipText("<html>Mintenance version of the Server, Example: 15.0.<b>3</b></html>");

		_testVersionEsd_lbl   .setToolTipText("<html>ESD or SP (ESD = Electronic Software Distribution, SP = Service Pack) Level of the Server, Example: 15.0.3 <b>ESD#2</b> or <b>SP100</b><br>SAP is using Service Packs to handle <i>bug fixes</i> and <i>minor enhancements</i>. <br>The Service Pack consist of three numbers. Here I will try to simulate ESD into SP. Example ESD#4 will be SP040 and ESD#4.1 will be SP041<br>In the summer of 2013 Sybase/SAP changed from ESD into SP.</html>");
		_testVersionEsd_sp    .setToolTipText("<html>ESD or SP (ESD = Electronic Software Distribution, SP = Service Pack) Level of the Server, Example: 15.0.3 <b>ESD#2</b> or <b>SP100</b><br>SAP is using Service Packs to handle <i>bug fixes</i> and <i>minor enhancements</i>. <br>The Service Pack consist of three numbers. Here I will try to simulate ESD into SP. Example ESD#4 will be SP040 and ESD#4.1 will be SP041<br>In the summer of 2013 Sybase/SAP changed from ESD into SP.</html>");

		_testVersionPl_lbl    .setToolTipText("<html>PL -Patch Level of the Server Version, Example: 16.0 SP01 <b>PL01</b><br>SAP is using Patch Level to handle <i>bug fixes</i> and <i>minor enhancements</i>. <br>Note: This is introduced in ASE 16.0</html>");
		_testVersionPl_sp     .setToolTipText(_testVersionPl_lbl.getToolTipText());

		_testVersionIsCe_chk  .setToolTipText("<html>Generate SQL Information for a Cluster Edition Server</html>");

		_testVersionShort_lbl .setToolTipText("<html>Here you can specify a Version string, which will be parsed into a number.</html>");
		_testVersionShort_txt .setToolTipText("<html>Here you can specify a Version string, which will be parsed into a number.</html>");
		_testVersionInt_txt   .setToolTipText("<html>Internal INTEGER used as version number.</html>");


		_sqlInit_lbl          .setToolTipText("<html>What SQL statement will be executed before we do first data sample</html>");
		_sqlInit              .setToolTipText("<html>What SQL statement will be executed before we do first data sample</html>");

		_sqlExec_lbl          .setToolTipText("<html>What SQL statement will be executed to get Performance Counters</html>");
		_sqlExec              .setToolTipText("<html>What SQL statement will be executed to get Performance Counters</html>");

		_sqlWhere_lbl         .setToolTipText("<html>Any extra where clauses that will be applied</html>");
		_sqlWhere             .setToolTipText("<html>Any extra where clauses that will be applied</html>");

		_sqlClose_lbl         .setToolTipText("<html>What SQL statement will be executed when we close the connection to the Server</html>");
		_sqlClose             .setToolTipText("<html>What SQL statement will be executed when we close the connection to the Server</html>");

		_pkCols_lbl           .setToolTipText("<html>What columns is Primary Key, which will be used when doing diff calculations</html>");
		_pkCols_txt           .setToolTipText("<html>What columns is Primary Key, which will be used when doing diff calculations</html>");
		_pkColsHighlight_chk  .setToolTipText("<html>Mark the PK columns with a background color in the SQL text</html>");

		_diffCols_lbl         .setToolTipText("<html>What columns should we do difference calculations on</html>");
		_diffCols_txt         .setToolTipText("<html>What columns should we do difference calculations on</html>");
		_diffColsHighlight_chk.setToolTipText("<html>Mark the DIFF Calculated columns with a background color in the SQL text</html>");

		_pctCols_lbl          .setToolTipText("<html>What columns should we be treated as Percentage columns</html>");
		_pctCols_txt          .setToolTipText("<html>What columns should we be treated as Percentage columns</html>");
		_pctColsHighlight_chk .setToolTipText("<html>Mark the PERCENT columns with a background color in the SQL text</html>");

		_needConfig_lbl       .setToolTipText("<html>What Server Configuration(s) does this Performance Counter depend on.</html>");
		_needConfig_txt       .setToolTipText("<html>What Server Configuration(s) does this Performance Counter depend on.</html>");

		_needRole_lbl         .setToolTipText("<html>What Server Role(s) does this Performance Counter depend on.</html>");
		_needRole_txt         .setToolTipText("<html>What Server Role(s) does this Performance Counter depend on.</html>");

		_needAseVersion_lbl   .setToolTipText("<html>We need to connect to a Server with a higher Version than this to get Performance Counters</html>");
		_needAseVersion_txt   .setToolTipText("<html>We need to connect to a Server with a higher Version than this to get Performance Counters</html>");

		_needAseCeVersion_lbl .setToolTipText("<html>If this is 0, then we will use Server Version number.</html>");
		_needAseCeVersion_txt .setToolTipText("<html>If this is 0, then we will use Server Version number.</html>");

		_dependsOnCm_lbl      .setToolTipText("<html>Do this Performance Counter depend on some other Performance Counters Data.</html>");
		_dependsOnCm_txt      .setToolTipText("<html>Do this Performance Counter depend on some other Performance Counters Data.</html>");
		//------------ END: TOOLTIP
		
		
//		int     aseVersion  = 12503;
//		int     aseVersion  = 1250030;
		int     aseVersion  = Ver.ver(12,5,0,3);
		boolean isCeEnabled = false;

		if (_cm.isRuntimeInitialized())
		{
			aseVersion  = _cm.getServerVersion();
			isCeEnabled = _cm.isClusterEnabled();
			_initialized_true_lbl .setVisible(true);
			_initialized_false_lbl.setVisible(false);
		}
		else
		{
			_initialized_true_lbl .setVisible(false);
			_initialized_false_lbl.setVisible(true);
		}

		_cmShortName_txt.setText(_cm.getName());
		_cmLongName_txt .setText(_cm.getDisplayName());

		_sqlInit.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL);
		_sqlInit.setHighlightCurrentLine(false);
		//_sqlInit.setLineWrap(true);
//		_sqlInitScroll.setLineNumbersEnabled(false);
		RSyntaxUtilitiesX.installRightClickMenuExtentions(_sqlInit, null, this);

		
		_sqlExec.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL);
		_sqlExec.setHighlightCurrentLine(false);
		//_sqlExec.setLineWrap(true);
//		_sqlExecScroll.setLineNumbersEnabled(false);
		RSyntaxUtilitiesX.installRightClickMenuExtentions(_sqlExec, null, this);
		
		_sqlWhere.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL);
		_sqlWhere.setHighlightCurrentLine(false);
		//_sqlWhere.setLineWrap(true);
//		_sqlWhereScroll.setLineNumbersEnabled(false);
		RSyntaxUtilitiesX.installRightClickMenuExtentions(_sqlWhere, null, this);
		
		_sqlClose.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL);
		_sqlClose.setHighlightCurrentLine(false);
		//_sqlClose.setLineWrap(true);
//		_sqlCloseScroll.setLineNumbersEnabled(false);
		RSyntaxUtilitiesX.installRightClickMenuExtentions(_sqlClose, null, this);
		
		
		_pkColsHighlight_chk  .setSelected( Configuration.getCombinedConfiguration().getBooleanProperty("ShowCmPropertiesDialog.highlight.pkCols",   _pkColsHighlight_chk  .isSelected()) );
		_diffColsHighlight_chk.setSelected( Configuration.getCombinedConfiguration().getBooleanProperty("ShowCmPropertiesDialog.highlight.diffCols", _diffColsHighlight_chk.isSelected()) );
		_pctColsHighlight_chk .setSelected( Configuration.getCombinedConfiguration().getBooleanProperty("ShowCmPropertiesDialog.highlight.pctCols",  _pctColsHighlight_chk .isSelected()) );
		
		_testVersionInt_txt.setEditable(false);

		namePanel.add(_cmShortName_lbl, "");
		namePanel.add(_cmShortName_txt, "growx, pushx, wrap");

		namePanel.add(_cmLongName_lbl,  "");
		namePanel.add(_cmLongName_txt,  "growx, pushx, wrap");

		aseVerPanel.add(_initialized_true_lbl,  "span, hidemode 3, wrap 20");
		aseVerPanel.add(_initialized_false_lbl, "span, hidemode 3, wrap 20");

		aseVerPanel.add(_testVersionMajor_lbl, "w 10mm, skip 1, span, split");
		aseVerPanel.add(_testVersionMinor_lbl, "w 10mm");
		aseVerPanel.add(_testVersionMaint_lbl, "w 10mm");
		aseVerPanel.add(_testVersionEsd_lbl,   "w 10mm");
		aseVerPanel.add(_testVersionPl_lbl,    "w 10mm, wrap");

		aseVerPanel.add(_testVersion_lbl,      "");
		aseVerPanel.add(_testVersionMajor_sp,  "w 10mm, span, split");
		aseVerPanel.add(_testVersionMinor_sp,  "w 10mm, ");
		aseVerPanel.add(_testVersionMaint_sp,  "w 10mm, ");
		aseVerPanel.add(_testVersionEsd_sp,    "w 10mm, ");
		aseVerPanel.add(_testVersionPl_sp,     "w 10mm, ");
		aseVerPanel.add(_testVersionIsCe_chk,  "wrap");

		aseVerPanel.add(_testVersionShort_lbl, "");
		aseVerPanel.add(_testVersionShort_txt, "growx, pushx");
		aseVerPanel.add(_testVersionInt_txt,   "wrap");

		sqlPanel.add(_sqlInit_lbl,             "wrap");
//		sqlPanel.add(_sqlInitScroll,           "span, growx, pushx, wrap");
		sqlPanel.add(_sqlInit,                 "span, growx, pushx, wrap");

		sqlPanel.add(_sqlExec_lbl,             "wrap");
//		sqlPanel.add(_sqlExecScroll,           "span, growx, pushx, wrap");
		sqlPanel.add(_sqlExec,                 "span, growx, pushx, wrap");

		sqlPanel.add(_sqlWhere_lbl,            "wrap");
//		sqlPanel.add(_sqlWhereScroll,          "span, growx, pushx, wrap");
		sqlPanel.add(_sqlWhere,                "span, growx, pushx, wrap");

		sqlPanel.add(_sqlClose_lbl,            "wrap");
//		sqlPanel.add(_sqlCloseScroll,          "span, growx, pushx, wrap");
		sqlPanel.add(_sqlClose,                "span, growx, pushx, wrap");


		otherPanel.add(_pkCols_lbl,            "");
		otherPanel.add(_pkColsHighlight_chk,   "");
		otherPanel.add(_pkCols_txt,            "growx, pushx, wrap");

		otherPanel.add(_diffCols_lbl,          "");
		otherPanel.add(_diffColsHighlight_chk, "");
		otherPanel.add(_diffCols_txt,          "growx, pushx, wrap");

		otherPanel.add(_pctCols_lbl,           "");
		otherPanel.add(_pctColsHighlight_chk,  "");
		otherPanel.add(_pctCols_txt,           "growx, pushx, wrap");

		otherPanel.add(_needConfig_lbl,        "");
		otherPanel.add(_needConfig_txt,        "skip, growx, pushx, wrap");

		otherPanel.add(_needRole_lbl,          "");
		otherPanel.add(_needRole_txt,          "skip, growx, pushx, wrap");

		otherPanel.add(_needAseVersion_lbl,    "");
		otherPanel.add(_needAseVersion_txt,    "skip, growx, pushx, wrap");

		otherPanel.add(_needAseCeVersion_lbl,  "");
		otherPanel.add(_needAseCeVersion_txt,  "skip, growx, pushx, wrap");

		otherPanel.add(_dependsOnCm_lbl,       "");
		otherPanel.add(_dependsOnCm_txt,       "skip, growx, pushx, wrap");

		panel.add(namePanel,   "growx, pushx, wrap");
		panel.add(aseVerPanel, "wrap");
		panel.add(sqlPanel,    "wrap");
		panel.add(otherPanel,  "growx, pushx, wrap");
		
		loadFieldsUsingVersion(aseVersion, isCeEnabled);

		return panel;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);

		// OK
		if ( _ok_but.equals(source) )
		{
			dispose();
		}

		if ( _testVersionIsCe_chk.equals(source) ) 
			stateChanged(null);

		if ( _pkColsHighlight_chk.equals(source) )
		{
			stateChanged(null);
			if (conf != null)
			{
				conf.setProperty("ShowCmPropertiesDialog.highlight.pkCols", _pkColsHighlight_chk.isSelected());
				conf.save();
			}
		}
		if ( _diffColsHighlight_chk.equals(source) ) 
		{
			stateChanged(null);
			if (conf != null)
			{
				conf.setProperty("ShowCmPropertiesDialog.highlight.diffCols", _diffColsHighlight_chk.isSelected());
				conf.save();
			}
		}
		if ( _pctColsHighlight_chk .equals(source) ) 
		{
			stateChanged(null);
			if (conf != null)
			{
				conf.setProperty("ShowCmPropertiesDialog.highlight.pctCols", _pctColsHighlight_chk.isSelected());
				conf.save();
			}
		}

		if ( _testVersionShort_txt.equals(source) )
		{
			String versionStr = _testVersionShort_txt.getText();
			parseVersionString(versionStr);
		}
	}
	@Override
	public void stateChanged(ChangeEvent e)
	{
		int major = _testVersionMajor_spm.getNumber().intValue();
		int minor = _testVersionMinor_spm.getNumber().intValue();
		int maint = _testVersionMaint_spm.getNumber().intValue();
		int esd   = _testVersionEsd_spm  .getNumber().intValue();
		int pl    = _testVersionPl_spm   .getNumber().intValue();

//		int ver = (major * 1000) + (minor * 100) + (maint * 10) + esd;
//		int ver = (major * 100000) + (minor * 10000) + (maint * 1000) + esd;
//System.out.println("stateChanged: ver="+ver+", major="+major+", minor="+minor+", maint="+maint+", esd="+esd+".");
		int ver = Ver.ver(major, minor, maint, esd, pl);

		boolean isCeEnabled = _testVersionIsCe_chk.isSelected();

		loadFieldsUsingVersion(ver, isCeEnabled);
	}

	private void parseVersionString(String versionStr)
	{
		if (StringUtil.isNullOrBlank(versionStr) || "0.0.0".equals(versionStr))
			versionStr = "00.0.0"; // then the below sybVersionStringToNumber() work better

		int version = Ver.sybVersionStringToNumber(versionStr);
		
		int major = Ver.versionIntPart(version, Ver.VERSION_MAJOR);
		int minor = Ver.versionIntPart(version, Ver.VERSION_MINOR);
		int maint = Ver.versionIntPart(version, Ver.VERSION_MAINTENANCE);
		int esd   = Ver.versionIntPart(version, Ver.VERSION_SERVICE_PACK);
		int pl    = Ver.versionIntPart(version, Ver.VERSION_PATCH_LEVEL);

//System.out.println("parseVersionString: versionStr='"+versionStr+"', version="+version+", major="+major+", minor="+minor+", maint="+maint+", esd="+esd+", pl="+pl+".");
		_testVersionMajor_spm.setValue(major);
		_testVersionMinor_spm.setValue(minor);
		_testVersionMaint_spm.setValue(maint);
		_testVersionEsd_spm  .setValue(esd);
		_testVersionPl_spm   .setValue(pl);

//		_testVersionShort_txt.setText(AseConnectionUtils.versionIntToStr(version));
		_testVersionInt_txt  .setText(Integer.toString(version));
	}

	private void loadFieldsUsingVersion(int aseVersion, boolean isCeEnabled)
	{
//		Connection conn = AseTune.getCounterCollector().getMonConnection();
		Connection conn = CounterController.getInstance().getMonConnection();

		String sqlInit  = _cm.getSqlInitForVersion (conn, aseVersion, isCeEnabled);
		String sqlExec  = _cm.getSqlForVersion     (conn, aseVersion, isCeEnabled);
		String sqlWhere = _cm.getSqlWhere();
		String sqlClose = _cm.getSqlCloseForVersion(conn, aseVersion, isCeEnabled);

		if ( sqlInit == null )  sqlInit = "";
		if ( sqlExec == null )  sqlExec = "";
		if ( sqlWhere == null ) sqlWhere = "";
		if ( sqlClose == null )	sqlClose = "";

		String pkCols           = StringUtil.toCommaStr(_cm.getPkForVersion(conn, aseVersion, isCeEnabled));
		String diffCols         = StringUtil.toCommaStr(_cm.getDiffColumns());
		String pctCols          = StringUtil.toCommaStr(_cm.getPctColumns());
		String needConfig       = StringUtil.toCommaStr(_cm.getDependsOnConfigForVersion(conn, aseVersion, isCeEnabled));
		String needRole         = StringUtil.toCommaStr(_cm.getDependsOnRole());
		String needAseVersion   = _cm.getDependsOnVersionStr();
		String needAseCeVersion = _cm.getDependsOnCeVersionStr();
		String dependsOnCm      = StringUtil.toCommaStr(_cm.getDependsOnCm());

		_sqlInit .setText(sqlInit);
		_sqlExec .setText(sqlExec);
		_sqlWhere.setText(sqlWhere);
		_sqlClose.setText(sqlClose);

		String aseVersionStr = Ver.versionIntToStr(aseVersion);
//System.out.println("loadFieldsUsingVersion(): version="+aseVersion+", aseVersionStr='"+aseVersionStr+"'.");
		_testVersionShort_txt.setText(aseVersionStr);
		parseVersionString(aseVersionStr);

		_testVersionIsCe_chk.setSelected(isCeEnabled);

		_pkCols_txt          .setText(pkCols);
		_diffCols_txt        .setText(diffCols);
		_pctCols_txt         .setText(pctCols);
		_needConfig_txt      .setText(needConfig);
		_needRole_txt        .setText(needRole);
		_needAseVersion_txt  .setText(needAseVersion);
		_needAseCeVersion_txt.setText(needAseCeVersion);
		_dependsOnCm_txt     .setText(dependsOnCm);

		_pkCols_txt  .setBackground( _pkColsHighlight_chk  .isSelected() ? PK_COLOR   : _needConfig_txt.getBackground() );
		_diffCols_txt.setBackground( _diffColsHighlight_chk.isSelected() ? DIFF_COLOR : _needConfig_txt.getBackground() );
		_pctCols_txt .setBackground( _pctColsHighlight_chk .isSelected() ? PCT_COLOR  : _needConfig_txt.getBackground() );
		
//		_sqlExec.clearMarkAllHighlights();
		SearchContext context = new SearchContext();
		context.setMarkAll(false);
		SearchEngine.find(_sqlExec, context);

		if (_pkColsHighlight_chk  .isSelected()) RTextUtility.markAll(_sqlExec, PK_COLOR,   _cm.getPkForVersion(conn, aseVersion, isCeEnabled));
		if (_diffColsHighlight_chk.isSelected()) RTextUtility.markAll(_sqlExec, DIFF_COLOR, _cm.getDiffColumns());
		if (_pctColsHighlight_chk .isSelected()) RTextUtility.markAll(_sqlExec, PCT_COLOR,  _cm.getPctColumns());
		
		scrollToTop();
	}

	private void scrollToTop()
	{
		if (_xscroll == null)
			return;

		// Defer the execution...
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				JScrollBar verticalScrollBar   = _xscroll.getVerticalScrollBar();
				JScrollBar horizontalScrollBar = _xscroll.getHorizontalScrollBar();
				
				verticalScrollBar  .setValue(verticalScrollBar  .getMinimum());
				horizontalScrollBar.setValue(horizontalScrollBar.getMinimum());
			}
		});
	}
}
