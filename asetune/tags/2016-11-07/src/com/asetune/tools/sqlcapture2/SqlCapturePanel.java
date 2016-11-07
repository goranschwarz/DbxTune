package com.asetune.tools.sqlcapture2;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;

import org.apache.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.asetune.Version;
import com.asetune.gui.swing.GPanel;
import com.asetune.gui.swing.GTable;
import com.asetune.ui.rsyntaxtextarea.AsetuneSyntaxConstants;
import com.asetune.ui.rsyntaxtextarea.RSyntaxTextAreaX;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class SqlCapturePanel
extends JPanel
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger        = Logger.getLogger(SqlCapturePanel.class);

	public final static String DISCARD_APP_NAME = "AseTune";

	// Properties and it's defaults
	private static final String PROP_PREFIX = "SqlCapture";

	// Active
	public static final String       PROPKEY_active_sample_doSpShowplanfull = PROP_PREFIX + ".active.sample.SpShowplanfull";
	public static final boolean      DEFAULT_active_sample_doSpShowplanfull = true;

	public static final String       PROPKEY_active_hide_waitEventId250     = PROP_PREFIX + ".active.hide.waitEventId250";
	public static final boolean      DEFAULT_active_hide_waitEventId250     = false;

	public static final String       PROPKEY_active_sqlWhereClause          = PROP_PREFIX + ".active.sql.where";
	public static final List<String> DEFAULT_active_sqlWhereClause          = Arrays.asList(
			"WaitTime > 1000", 
			"LogicalReads > 100", 
			"PhysicalReads > 10", 
			"P.SPID in (select blocked from master..sysprocesses where blocked > 0) -- SPID's that blocks others", 
			"P.BlockingSPID > 0 -- Blocked SPID's", 
			"object_name(S.ProcedureID,S.DBID) = 'any_proc_name'", 
			"db_name(S.DBID) = 'any_db_name'");

	public static final String       PROPKEY_active_sqlOrderBy              = PROP_PREFIX + ".active.sql.orderby";
	public static final List<String> DEFAULT_active_sqlOrderBy              = Arrays.asList(
			"SPID", 
			"LogicalReads desc", 
			"PhysicalReads desc");

	// History
	public static final String       PROPKEY_history_discard_onOpen         = PROP_PREFIX + ".history.discard.onOpen";
	public static final boolean      DEFAULT_history_discard_onOpen         = true;

	public static final String       PROPKEY_history_discard_aseTune        = PROP_PREFIX + ".history.discard.aseTune";
	public static final boolean      DEFAULT_history_discard_aseTune        = true;

	public static final String       PROPKEY_history_sqlWhereClause         = PROP_PREFIX + ".history.sql.orderby";
	public static final List<String> DEFAULT_history_sqlWhereClause         = Arrays.asList(
			"WaitTime > 1000", 
			"LogicalReads > 100",
			"LogicalReads > 1000",
			"LogicalReads > 5000",
			"LogicalReads > 10000",
			"(LogicalReads > 10000 or PhysicalReads > 300)",
			"datediff(ms, StartTime, EndTime) > 1000",
			"datediff(ms, StartTime, EndTime) > 5000",
			"SPID in (select spid from master.dbo.sysprocesses where program_name = 'isql')");

	// SQL Text/batch
	public static final String       PROPKEY_sqlText_sampleSqlText          = PROP_PREFIX + ".sqlText.sample";
	public static final boolean      DEFAULT_sqlText_sampleSqlText          = true;
	
	public static final String       PROPKEY_sqlText_showSpText             = PROP_PREFIX + ".sqlText.showSpText";
	public static final boolean      DEFAULT_sqlText_showSpText             = true;
	
	// SQL Plan
	public static final String       PROPKEY_sqlPlan_samplePlanText         = PROP_PREFIX + ".sqlPlan.sample";
	public static final boolean      DEFAULT_sqlPlan_samplePlanText         = false;
	
	public static final String       PROPKEY_sqlPlan_autoLoadXmlPlanInGui   = PROP_PREFIX + ".sqlPlan.autoLoadXmlPlanInGui";
	public static final boolean      DEFAULT_sqlPlan_autoLoadXmlPlanInGui   = true;

	// Split panes
	private   JSplitPane        _split_TopAndMiddle          = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	private   JSplitPane        _split_MiddlewAndBottom      = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

	private   JSplitPane        _split_ProfileAndStmnt_pan   = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	private   JSplitPane        _split_SqlTextAndPlan_pan    = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

	// Active Statements
	private   GPanel            _activeStmnt_pan;
	private   JLabel            _activeStmntTableCount_lbl             = new JLabel("0 Rows");
	private   JCheckBox         _activeStmntDoExecSpShowplanfull_chk   = new JCheckBox("<html>Exec: sp_showplanfull <i>SPID</i>, on <i>first</i> row of <b>active</b> statements, in the table below.</html>", true);
	private   JCheckBox         _activeStmntHideActiveSqlWaitId250_chk = new JCheckBox("<html>Hide active SQL Statements with WaitEventID=250 (<i>waiting for incoming network data</i>)</html>", true);
	private   JLabel            _activeStmntSqlWhereClause_lbl         = new JLabel("Where: ");
	protected JComboBox<String> _activeStmntSqlWhereClause_cbx         = new JComboBox<String>();
	protected JButton           _activeStmntSqlWhereClause_but         = new JButton("Remove from template");
	private   JLabel            _activeStmntSqlOrderBy_lbl             = new JLabel("Order By: ");
	protected JComboBox<String> _activeStmntSqlOrderBy_cbx             = new JComboBox<String>();
	protected JButton           _activeStmntSqlOrderBy_but             = new JButton("Remove from template");
	private   GTable            _activeStmnt_tab                       = new GTable();

	// Historical Profiler
	private   JPanel            _historyProfile_pan;

	// Historical Statements
	private   GPanel            _historyStmnt_pan;
	private   JLabel            _historyStmntTableCount_lbl        = new JLabel("0 Rows");
	private   int               _historyStmntTableCount_prev       = 0;
	private   JLabel            _historyStmntWhereSql_lbl          = new JLabel(" Restrict to: WHERE");
	protected JComboBox<String> _historyStmntWhereSql_cbx          = new JComboBox<String>();
	private   JButton           _historyStmntWhereSql_but          = new JButton("Remove from template");
	private   JCheckBox         _historyStmntDiscardBeforeOpen_chk = new JCheckBox("Discard historical/captured statements that happened before the dialogue was opened.", true);
	private   JCheckBox         _historyStmntDiscardAseTuneApp_chk = new JCheckBox("Discard Statements from application '"+DISCARD_APP_NAME+"'.", true);
	private   GTable            _historyStmnt_tab                  = new GTable();

	// SQL Text
	private   JPanel            _sqlText_pan;
	private   JCheckBox         _sqlTextSample_chk                 = new JCheckBox("Sample SQL batch text", true);
	protected JCheckBox         _sqlTextShowProcSrc_chk            = new JCheckBox("Show Stored Procedure source code in Batch text window", true);
	private   RSyntaxTextAreaX  _sqlText_txt                       = new RSyntaxTextAreaX();
	private   RTextScrollPane   _sqlText_scroll                    = new RTextScrollPane(_sqlText_txt);

	// SQL Plan
	private   JPanel            _sqlPlan_pan;
	private   JCheckBox         _sqlPlanSample_chk                 = new JCheckBox("Sample showplan text", false);
	protected JCheckBox         _sqlPlanAutoLoadXmlGui_chk         = new JCheckBox("Automatically load XML Plan in GUI", true);
	protected JButton           _sqlPlanOpenXmlPlanInGui_but       = new JButton("XML Plan in GUI");
	private   RSyntaxTextAreaX  _sqlPlan_txt                       = new RSyntaxTextAreaX();
	private   RTextScrollPane   _sqlPlan_scroll                    = new RTextScrollPane(_sqlPlan_txt);


	public SqlCapturePanel()
	{
		init();
		loadProps();
	}

	private void init()
	{
		_activeStmnt_pan          = createActiveStatementPanel();
		_historyProfile_pan    = createHistoricalProfilePanel();
		_historyStmnt_pan      = createHistoricalStatementPanel();
		_sqlText_pan              = createSqlTextPanel();
		_sqlPlan_pan              = createSqlPlanPanel();

		_split_SqlTextAndPlan_pan    = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, _sqlText_pan,           _sqlPlan_pan);
		_split_ProfileAndStmnt_pan   = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, _historyProfile_pan, _historyStmnt_pan);

		_split_MiddlewAndBottom      = new JSplitPane(JSplitPane.VERTICAL_SPLIT, _split_ProfileAndStmnt_pan, _split_SqlTextAndPlan_pan);
		_split_TopAndMiddle          = new JSplitPane(JSplitPane.VERTICAL_SPLIT, _activeStmnt_pan,      _split_MiddlewAndBottom);

		setLayout( new MigLayout("insets 0 0 0 0") );
		add(_split_TopAndMiddle, "grow, push, wrap");
	}

	private GPanel createActiveStatementPanel()
	{
		GPanel panel = SwingUtils.createGPanel("Active Statements", true);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));
		
		panel                                 .setToolTipText("SQL Statements that are currently execting at the server will be showed here.");
		_activeStmntDoExecSpShowplanfull_chk  .setToolTipText("<html>Execute the procedure sp_showplanfull <i>SPID</i> on the first row in the <i>active</i> SQL Table.<br>This will enable you to see Showplan of the current running SQL Statement even if sp_configure 'plan text pipe active' and 'plan text pipe max messages' is turned off.</html>");
		_activeStmntHideActiveSqlWaitId250_chk.setToolTipText("<html>In case the Active SQL table contains WaitEventId=250 and has been waiting for more than 60 seconds... <br>Hide those statements... This is a workaround for some bug in some ASE Versions</html>");
		_activeStmntSqlWhereClause_cbx        .setToolTipText("Add your extra where clauses on the monXXX table. make sure that only columns in that table are used. "+Version.getAppName()+"'s errorlog will show faulty SQL statements.");
		_activeStmntSqlWhereClause_but        .setToolTipText("Remove the 'extra where' from the template.");
		_activeStmntSqlOrderBy_cbx            .setToolTipText("Change 'order by' clauses on the monXXX table. "+Version.getAppName()+"'s errorlog will show faulty SQL statements.");
		_activeStmntSqlOrderBy_but            .setToolTipText("Remove the 'order by' from the template.");

		_activeStmntSqlWhereClause_cbx.setEditable(true);
		_activeStmntSqlOrderBy_cbx    .setEditable(true);

		JPanel fields = new JPanel(new MigLayout("insets 0 0 0 0"));
		fields.add(_activeStmntDoExecSpShowplanfull_chk,   "gap 5, split, span");
		fields.add(_activeStmntHideActiveSqlWaitId250_chk, "");
		fields.add(_activeStmntTableCount_lbl,             "tag right, wrap");
		
		fields.add(_activeStmntSqlWhereClause_lbl,         "gap 5 5 0 0");
		fields.add(_activeStmntSqlWhereClause_cbx,         "growx, pushx");
		fields.add(_activeStmntSqlWhereClause_but,         "gap 5 5 0 0, wrap");

		fields.add(_activeStmntSqlOrderBy_lbl,             "gap 5 5 0 0");
		fields.add(_activeStmntSqlOrderBy_cbx,             "growx, pushx");
		fields.add(_activeStmntSqlOrderBy_but,             "gap 5 5 0 0, wrap");

//		panel.add(new JLabel("Active STATEMENTS"),   "pushx, growx");
		
		JScrollPane tabScroll = new JScrollPane(_activeStmnt_tab);
		panel.add(fields,    "growx, pushx, wrap");
		panel.add(tabScroll, "grow, push, wrap");

		return panel;
	}

	private JPanel createHistoricalProfilePanel()
	{
		JPanel panel = SwingUtils.createPanel("Profiler", true);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));
		
		panel.add(new JLabel("History PROFILER"),    "pushx, growx, wrap");
		panel.add(new JLabel("NOT YET IMPLEMENTED"), "pushx, growx");

		return panel;
	}

	private GPanel createHistoricalStatementPanel()
	{
		GPanel panel = SwingUtils.createGPanel("Historical Statements", true);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));
		
		panel                             .setToolTipText("SQL Text for 'historical' SQL Statements will be showed here.");
		_historyStmntDiscardBeforeOpen_chk.setToolTipText("If you want to discard or show events from monSysStatements that happened prior to the window was open.");
		_historyStmntDiscardAseTuneApp_chk.setToolTipText("If you want to discard or show events from monSysStatements that was generated by application '"+DISCARD_APP_NAME+"'.");
		_historyStmntWhereSql_cbx         .setToolTipText("Add your extra where clauses on the monSysStatement table. make sure that only columns in theat table are used. "+Version.getAppName()+"'s errorlog will show faulty SQL statements.");
		_historyStmntWhereSql_but         .setToolTipText("Remove the current restriction from the template.");

		_historyStmntWhereSql_cbx.setEditable(true);

		JPanel fields = new JPanel(new MigLayout("insets 0 0 0 0"));
		fields.add(_historyStmntDiscardBeforeOpen_chk, "gap 5, split, span");
		fields.add(_historyStmntDiscardAseTuneApp_chk, "");
		fields.add(_historyStmntTableCount_lbl,        "tag right, wrap");
		fields.add(_historyStmntWhereSql_lbl,          "gap 5 5 0 0");
		fields.add(_historyStmntWhereSql_cbx,          "pushx, growx");
		fields.add(_historyStmntWhereSql_but,          "gap 5 5 0 0");
		
		JScrollPane tabScroll = new JScrollPane(_historyStmnt_tab);
		panel.add(fields,    "growx, pushx, wrap");
		panel.add(tabScroll, "grow, push, wrap");

		return panel;
	}

	private JPanel createSqlTextPanel()
	{
		JPanel panel = SwingUtils.createPanel("SQL Text", true);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));
	
		_sqlTextSample_chk     .setToolTipText("<html>Get SQL Statements from monSysSQLText<br>Note: Even if sp_configure 'sql text pipe active' is not configured. SQL from an ACTIVE SQL Statement might be visible.</html>");
		_sqlTextShowProcSrc_chk.setToolTipText("<html>Get the Stored Procedure text and display that (if it was a procedure that was executed)</html>");

		JPanel fields = new JPanel(new MigLayout("insets 0 0 0 0"));
		fields.add(_sqlTextSample_chk,      "");
		fields.add(_sqlTextShowProcSrc_chk, "wrap");

		panel.add(fields,          "growx, pushx, wrap");
		panel.add(_sqlText_scroll, "grow, push, wrap");

		_sqlText_txt.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL);

		return panel;
	}

	private JPanel createSqlPlanPanel()
	{
		JPanel panel = SwingUtils.createPanel("Execution Plan", true);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));
		
		_sqlPlanSample_chk          .setToolTipText("<html>Get SQL Showplans from monSysPlanText<br>Note: Even if sp_configure 'plan text pipe active' is not configured. Showplans from an ACTIVE SQL Statement might be visible.</html>");
		_sqlPlanAutoLoadXmlGui_chk  .setToolTipText("Automatically Load the XML Plan in the ASE Plan Viewer GUI when the row is selected and it has an XML Plan attached to it.");
		_sqlPlanOpenXmlPlanInGui_but.setToolTipText("Opens a GUI window and load the current/below XML plan in that tool");

		JPanel fields = new JPanel(new MigLayout("insets 0 0 0 0"));
		fields.add(_sqlPlanSample_chk,           "");
		fields.add(_sqlPlanAutoLoadXmlGui_chk,   "tag right");
		fields.add(_sqlPlanOpenXmlPlanInGui_but, "hidemode 3, wrap");

		panel.add(fields,          "growx, pushx, wrap");
		panel.add(_sqlPlan_scroll, "grow, push, wrap");

		return panel;
	}

	
	
//	/**
//	 * Called from the Refresh Process, at the top<br>
//	 * Sp here we can check various stuff and set statuses etc.
//	 */
//	public void updateGuiStatus()
//	{
//		setWatermark();
//		if (_counterCollector != null)
//		{
//			boolean isPaused = _counterCollector.isPaused();
//			paused_chk.setSelected(isPaused);
////			pauseButton .setVisible(!isPaused);
////			resumeButton.setVisible( isPaused);
//
//			_activeStmnt_pan .setToolTipText("<html><b>Last SQL Used To get ACTIVE SQL Statements </b><br><code><pre>"+_counterCollector.getActiveStatementsSql() +"</pre></code></html>");
//			_historyStmnt_pan.setToolTipText("<html><b>Last SQL Used To get HISTORY SQL Statements</b><br><code><pre>"+_counterCollector.getHistoryStatementsSql()+"</pre></code></html>");
//
//		}
//	}

	public void setSqlText(String text, String procname, int line)
	{
		RSyntaxTextAreaX rsta = _sqlText_txt;

		// Set border title
		((TitledBorder)_sqlText_pan.getBorder()).setTitle( StringUtil.hasValue(procname) ? procname : "SQL Text" );
		
		// Set the text
		rsta.setText(text);

		// Position you on the correct line
		if (line > 0)
		{
    		try
    		{
    			int position = rsta.getLineStartOffset(line - 1);
    			rsta.setCaretPosition(position);
    			rsta.moveCaretPosition(position);
    		}
    		catch(BadLocationException ignore) { /* ignore */ }
		}
		rsta.discardAllEdits();
	}

	public void setPlanText(String text, int line)
	{
		RSyntaxTextAreaX rsta = _sqlPlan_txt;

		// Set the text
		rsta.setText(text);

		// Position you on the correct line
		if (text.indexOf("<?xml") >= 0)
		{
			rsta.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
			rsta.setCodeFoldingEnabled(true);
		}
		else
		{
			rsta.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
			rsta.setCodeFoldingEnabled(false);
		}

		if (line > 0)
		{
    		try
    		{
    			int position = rsta.getLineStartOffset(line - 1);
    			rsta.setCaretPosition(position);
    			rsta.moveCaretPosition(position);
    		}
    		catch(BadLocationException ignore) { /* ignore */ }
		}
		rsta.discardAllEdits();
	}

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

		// PANEL SIZE
		conf.setLayoutProperty(PROP_PREFIX + ".active.statements.width",   _activeStmnt_pan.getSize().width);
		conf.setLayoutProperty(PROP_PREFIX + ".active.statements.height",  _activeStmnt_pan.getSize().height);

		conf.setLayoutProperty(PROP_PREFIX + ".history.statements.width",  _historyStmnt_pan.getSize().width);
		conf.setLayoutProperty(PROP_PREFIX + ".history.statements.height", _historyStmnt_pan.getSize().height);

		conf.setLayoutProperty(PROP_PREFIX + ".history.profiler.width",    _historyProfile_pan.getSize().width);
		conf.setLayoutProperty(PROP_PREFIX + ".history.profiler.height",   _historyProfile_pan.getSize().height);

		conf.setLayoutProperty(PROP_PREFIX + ".sql.text.width",            _sqlText_pan.getSize().width);
		conf.setLayoutProperty(PROP_PREFIX + ".sql.text.height",           _sqlText_pan.getSize().height);

		conf.setLayoutProperty(PROP_PREFIX + ".sql.plan.width",            _sqlPlan_pan.getSize().width);
		conf.setLayoutProperty(PROP_PREFIX + ".sql.plan.height",           _sqlPlan_pan.getSize().height);

		
		// ACTIVE 
		conf.setProperty(  PROPKEY_active_sample_doSpShowplanfull, _activeStmntDoExecSpShowplanfull_chk  .isSelected());
		conf.setProperty(  PROPKEY_active_hide_waitEventId250,     _activeStmntHideActiveSqlWaitId250_chk.isSelected());
		saveCombobox(conf, PROPKEY_active_sqlWhereClause,          _activeStmntSqlWhereClause_cbx);
		saveCombobox(conf, PROPKEY_active_sqlOrderBy,              _activeStmntSqlOrderBy_cbx);

		// HISTORY
		conf.setProperty(  PROPKEY_history_discard_onOpen,         _historyStmntDiscardBeforeOpen_chk.isSelected());
		conf.setProperty(  PROPKEY_history_discard_aseTune,        _historyStmntDiscardAseTuneApp_chk.isSelected());
		saveCombobox(conf, PROPKEY_history_sqlWhereClause,         _historyStmntWhereSql_cbx);
		
		// SQL TEXT
		conf.setProperty(PROPKEY_sqlText_sampleSqlText,            _sqlTextSample_chk     .isSelected());
		conf.setProperty(PROPKEY_sqlText_showSpText,               _sqlTextShowProcSrc_chk.isSelected());
		
		// SQL PLAN
		conf.setProperty(PROPKEY_sqlPlan_samplePlanText,           _sqlPlanSample_chk        .isSelected());
		conf.setProperty(PROPKEY_sqlPlan_autoLoadXmlPlanInGui,     _sqlPlanAutoLoadXmlGui_chk.isSelected());

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
		int width;
		int height;

		// PANEL SIZE
		width  = conf.getLayoutProperty(PROP_PREFIX + ".active.statements.width",  SwingUtils.hiDpiScale(1200));
		height = conf.getLayoutProperty(PROP_PREFIX + ".active.statements.height", SwingUtils.hiDpiScale(300));
		if (width != -1 && height != -1)
		{
			_activeStmnt_pan.setPreferredSize(new Dimension(width, height));
		}

		width  = conf.getLayoutProperty(PROP_PREFIX + ".history.profiler.width",  SwingUtils.hiDpiScale(300));
		height = conf.getLayoutProperty(PROP_PREFIX + ".history.profiler.height", SwingUtils.hiDpiScale(300));
		if (width != -1 && height != -1)
		{
			_historyProfile_pan.setPreferredSize(new Dimension(width, height));
		}

		width  = conf.getLayoutProperty(PROP_PREFIX + ".history.statements.width",  SwingUtils.hiDpiScale(900));
		height = conf.getLayoutProperty(PROP_PREFIX + ".history.statements.height", SwingUtils.hiDpiScale(300));
		if (width != -1 && height != -1)
		{
			_historyStmnt_pan.setPreferredSize(new Dimension(width, height));
		}

		width  = conf.getLayoutProperty(PROP_PREFIX + ".sql.text.width",  500);
		height = conf.getLayoutProperty(PROP_PREFIX + ".sql.text.height", 300);
		if (width != -1 && height != -1)
		{
			_sqlText_pan.setPreferredSize(new Dimension(width, height));
		}

		width  = conf.getLayoutProperty(PROP_PREFIX + ".sql.plan.width",  700);
		height = conf.getLayoutProperty(PROP_PREFIX + ".sql.plan.height", 300);
		if (width != -1 && height != -1)
		{
			_sqlPlan_pan.setPreferredSize(new Dimension(width, height));
		}

		// ACTIVE 
		_activeStmntDoExecSpShowplanfull_chk  .setSelected(conf.getBooleanProperty(PROPKEY_active_sample_doSpShowplanfull, DEFAULT_active_sample_doSpShowplanfull));
		_activeStmntHideActiveSqlWaitId250_chk.setSelected(conf.getBooleanProperty(PROPKEY_active_hide_waitEventId250,     DEFAULT_active_hide_waitEventId250));
		loadCombobox(conf, PROPKEY_active_sqlWhereClause, _activeStmntSqlWhereClause_cbx, DEFAULT_active_sqlWhereClause);
		loadCombobox(conf, PROPKEY_active_sqlOrderBy,     _activeStmntSqlOrderBy_cbx,     DEFAULT_active_sqlOrderBy);

		// HISTORY
		_historyStmntDiscardBeforeOpen_chk    .setSelected(conf.getBooleanProperty(PROPKEY_history_discard_onOpen,         DEFAULT_history_discard_onOpen));
		_historyStmntDiscardAseTuneApp_chk    .setSelected(conf.getBooleanProperty(PROPKEY_history_discard_aseTune,        DEFAULT_history_discard_aseTune));
		loadCombobox(conf, PROPKEY_history_sqlWhereClause, _historyStmntWhereSql_cbx, DEFAULT_history_sqlWhereClause);
		
		// SQL TEXT
		_sqlTextSample_chk                    .setSelected(conf.getBooleanProperty(PROPKEY_sqlText_sampleSqlText,          DEFAULT_sqlText_sampleSqlText));
		_sqlTextShowProcSrc_chk               .setSelected(conf.getBooleanProperty(PROPKEY_sqlText_showSpText,             DEFAULT_sqlText_showSpText));
		
		// SQL PLAN
		_sqlPlanSample_chk                    .setSelected(conf.getBooleanProperty(PROPKEY_sqlPlan_samplePlanText,         DEFAULT_sqlPlan_samplePlanText));
		_sqlPlanAutoLoadXmlGui_chk            .setSelected(conf.getBooleanProperty(PROPKEY_sqlPlan_autoLoadXmlPlanInGui,   DEFAULT_sqlPlan_autoLoadXmlPlanInGui));
	}

	private void loadCombobox(Configuration conf, String propStart, JComboBox<String> combobox, List<String> defaults)
	{
		combobox.removeAllItems();
		combobox.addItem("");

		int count   = conf.getIntProperty(propStart + ".count",  -1);
		int active  = conf.getIntProperty(propStart + ".active", -1);
		if ( count != -1  && active != -1 )
		{
			active = Math.max(0, active);
			for (int i=1; i<=count; i++)
			{
				String str = conf.getProperty(propStart + "." + i).trim();
				combobox.insertItemAt(str, i);
			}
			combobox.setSelectedIndex(active);
		}
		else
		{
			if (defaults != null)
			{
				for (String val : defaults)
				{
					combobox.addItem(val);
				}
			}
		}
	}
	private void saveCombobox(Configuration conf, String propStart, JComboBox<String> combobox)
	{
		conf.removeAll(propStart + ".");
		int saveCount = 0;
		for (int i=1; i<combobox.getItemCount(); i++)
		{
			String str = combobox.getItemAt(i);
			if (StringUtil.hasValue(str))
			{
				saveCount++;
				conf.setProperty(propStart + "." + saveCount, str);
			}
		}
		conf.setProperty( propStart + ".count", saveCount);
		conf.setProperty( propStart + ".active", Math.max(0, combobox.getSelectedIndex()));
	}
	/*---------------------------------------------------
	** END: implementing saveProps & loadProps
	**---------------------------------------------------
	*/	
}
