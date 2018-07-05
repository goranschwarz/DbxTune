package com.asetune.tools.sqlcapture;

import java.awt.Font;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SortOrder;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.asetune.Version;
import com.asetune.cache.XmlPlanCache;
import com.asetune.gui.AsePlanViewer;
import com.asetune.gui.MainFrame;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.gui.swing.DateTimePicker;
import com.asetune.gui.swing.GButton;
import com.asetune.gui.swing.GTabbedPane;
import com.asetune.gui.swing.GTable;
import com.asetune.gui.swing.GTableFilter;
import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.pcs.IPersistWriter;
import com.asetune.pcs.PersistReader;
import com.asetune.pcs.PersistWriterBase;
import com.asetune.pcs.PersistWriterJdbc;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.pcs.sqlcapture.SqlCaptureBrokerAse;
import com.asetune.sql.SqlPickList;
import com.asetune.sql.StatementNormalizer;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.tools.WindowType;
import com.asetune.tools.sqlw.QueryWindow;
import com.asetune.ui.rsyntaxtextarea.RSyntaxTextAreaX;
import com.asetune.ui.rsyntaxtextarea.RSyntaxUtilitiesX;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;
import com.asetune.utils.TimeUtils;
import com.asetune.xmenu.TablePopupFactory;
import com.jidesoft.swing.RangeSlider;

import net.miginfocom.swing.MigLayout;

public class SqlCaptureOfflineView
extends JFrame
implements ActionListener, ChangeListener//, MouseListener
{
	private static final long serialVersionUID = 1L;
	private static Logger _logger = Logger.getLogger(SqlCaptureOfflineView.class);
	
	public static final String PROPKEY_BASE = "SqlCaptureOfflineView.";

	private Window _guiOwner = null;
	
	private JSplitPane         _topBot_split   = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
//	private JSplitPane         _sqlStmnt_split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	private JSplitPane         _sqlPlan_split  = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

	// Search Panel 
	private JPanel             _search_panel           = null;
	private List<Timestamp>    _searchRange_list       = null;
	private JLabel             _searchRange_lbl        = new JLabel("Range");
	private RangeSlider        _searchRange_rsl        = new RangeSlider();
	private JLabel             _searchFrom_lbl         = new JLabel("From Period");
//	private JTextField         _searchFrom_dtp         = new JTextField();
	private DateTimePicker     _searchFrom_dtp         = new DateTimePicker();
	private JButton            _searchFrom_but         = new JButton("...");
	private JLabel             _searchTo_lbl           = new JLabel("Until Period");
//	private JTextField         _searchTo_dtp           = new JTextField();
	private DateTimePicker     _searchTo_dtp           = new DateTimePicker();
	private JButton            _searchTo_but           = new JButton("...");
	private JCheckBox          _searchAuto_chk         = new JCheckBox("Auto Load Period");
	private GButton            _search_but             = new GButton("Load");
	private JLabel             _queryTimeout_lbl       = new JLabel("Query Timeout");
	private JTextField         _queryTimeout_txt       = new JTextField("0", 5);
	private JLabel             _selectedRange_lbl      = new JLabel("Selected Range Span");
	private JLabel             _selectedRange_txt      = new JLabel("-");
	private JButton            _changeSqlCapProp_but   = new JButton("Change SQL Capture Settings");
                               
	private GTabbedPane        _tabbedPane             = new GTabbedPane("StatementAndSql");
                               
	// PANEL: Stataments Tab 
	private JPanel             _statements_panel       = null;
	private GTableLocal        _statements_tab         = new GTableLocal();
	private JScrollPane        _statements_scroll      = new JScrollPane(_statements_tab);
	private GTableFilter       _statementsFilter       = new GTableFilter(_statements_tab, GTableFilter.ROW_COUNT_LAYOUT_LEFT, true);
	private GButton            _statementsExec_but     = new GButton("Execute")
	{
		private static final long serialVersionUID = 1L;
		@Override public String getToolTipText(MouseEvent event) { setToolTipText("<html><pre>" + StringUtil.toHtmlStringExceptNl(getSqlFor_StatementsTab()) + "</pre></html>"); return super.getToolTipText(event); };
	};

	// PANEL: SQL Tab
	private JPanel             _sql_panel              = null;
	private GTableLocal        _sql_tab                = new GTableLocal();
	private JScrollPane        _sql_scroll             = new JScrollPane(_sql_tab);
	private GTableFilter       _sqlFilter              = new GTableFilter(_sql_tab, GTableFilter.ROW_COUNT_LAYOUT_LEFT, true);
	private GButton            _sqlExec_but            = new GButton("Execute")
	{
		private static final long serialVersionUID = 1L;
		@Override public String getToolTipText(MouseEvent event) { setToolTipText("<html><pre>" + StringUtil.toHtmlStringExceptNl(getSqlFor_SqlTextTab()) + "</pre></html>"); return super.getToolTipText(event); };
	};

	// PANEL: Statements SUM Tab
	private JPanel             _statementsSum_panel    = null;
	private GTableLocal        _statementsSum_tab      = new GTableLocal();
	private JScrollPane        _statementsSum_scroll   = new JScrollPane(_statementsSum_tab);
	private GTableFilter       _statementsSumFilter    = new GTableFilter(_statementsSum_tab, GTableFilter.ROW_COUNT_LAYOUT_LEFT, true);
	private JLabel             _statementsSum_lbl      = new JLabel("Max Rows");
	private SpinnerNumberModel _statementsSum_spm      = new SpinnerNumberModel(100, 1, 100000, 1);
	private JSpinner           _statementsSum_sp       = new JSpinner(_statementsSum_spm);
	private GButton            _statementsSumExec_but  = new GButton("Execute")
	{
		private static final long serialVersionUID = 1L;
		@Override public String getToolTipText(MouseEvent event) { setToolTipText("<html><pre>" + StringUtil.toHtmlStringExceptNl(getSqlFor_SumStatementsTab()) + "</pre></html>"); return super.getToolTipText(event); };
	};

	// PANEL: SQL SUM Tab
	private JPanel             _sqlSum_panel           = null;
	private GTableLocal        _sqlSum_tab             = new GTableLocal();
	private JScrollPane        _sqlSum_scroll          = new JScrollPane(_sqlSum_tab);
	private GTableFilter       _sqlSumFilter           = new GTableFilter(_sqlSum_tab, GTableFilter.ROW_COUNT_LAYOUT_LEFT, true);
	private JLabel             _sqlSum_lbl             = new JLabel("Max Rows");
	private SpinnerNumberModel _sqlSum_spm             = new SpinnerNumberModel(100, 1, 100000, 1);
	private JSpinner           _sqlSum_sp              = new JSpinner(_sqlSum_spm);
	private GButton            _sqlSumExec_but         = new GButton("Execute")
	{
		private static final long serialVersionUID = 1L;
		@Override public String getToolTipText(MouseEvent event) { setToolTipText("<html><pre>" + StringUtil.toHtmlStringExceptNl(getSqlFor_SumSqlTextTab()) + "</pre></html>"); return super.getToolTipText(event); };
	};

	// PANEL: UserDefined SQL Query
	private JPanel             _udq_panel           = null;
	private GTableLocal        _udq_tab             = new GTableLocal();
	private JScrollPane        _udq_scroll          = new JScrollPane(_udq_tab);
	private GTableFilter       _udqFilter           = new GTableFilter(_udq_tab, GTableFilter.ROW_COUNT_LAYOUT_LEFT, true);
//	private JLabel             _udq_lbl             = new JLabel("Max Rows");
//	private SpinnerNumberModel _udq_spm             = new SpinnerNumberModel(100, 1, 100000, 1);
//	private JSpinner           _udq_sp              = new JSpinner(_udq_spm);
	private GButton            _udqExec_but         = new GButton("Execute")
	{
		private static final long serialVersionUID = 1L;
		@Override public String getToolTipText(MouseEvent event) { setToolTipText("<html><pre>" + StringUtil.toHtmlStringExceptNl(getSqlFor_UserDefinedQueryTab()) + "</pre></html>"); return super.getToolTipText(event); };
	};
	private JButton            _udqSqlw_but         = new JButton("Start SQL Window");
	private JSplitPane         _udq_splitpane       = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	private RSyntaxTextAreaX   _udqSqlText_txt      = new RSyntaxTextAreaX();
	private RTextScrollPane    _udqSqlText_scroll   = new RTextScrollPane(_udqSqlText_txt);

	
	// PANEL: SQL Text 
	private JPanel             _sqlText_panel           = null;
	private RSyntaxTextAreaX   _sqlText_txt             = new RSyntaxTextAreaX();
	private RTextScrollPane    _sqlText_scroll          = new RTextScrollPane(_sqlText_txt);
	private JButton            _sqlTextFormat_but       = new JButton("Format SQL");
	private JCheckBox          _sqlTextFormatAuto_chk   = new JCheckBox("Auto Format SQL");
	private JLabel             _sqlTextCurSpid_lbl1     = new JLabel("Current SPID:");
	private JLabel             _sqlTextCurSpid_lbl2     = new JLabel("---");
	private JLabel             _sqlTextCurKpid_lbl1     = new JLabel("KPID:");
	private JLabel             _sqlTextCurKpid_lbl2     = new JLabel("---");
	private JLabel             _sqlTextCurBatchId_lbl1  = new JLabel("BatchID:");
	private JLabel             _sqlTextCurBatchId_lbl2  = new JLabel("---");
	private JLabel             _sqlTextCurProcName_lbl1 = new JLabel("ProcName:");
	private JLabel             _sqlTextCurProcName_lbl2 = new JLabel("---");

	// PANEL: Showplan
	private JPanel             _showplan_panel          = null;
	private RSyntaxTextAreaX   _showplan_txt            = new RSyntaxTextAreaX();
	private RTextScrollPane    _showplan_scroll         = new RTextScrollPane(_showplan_txt);
	private JButton            _showplanXmlGui_but      = new JButton("Load XML Plan in GUI");
	private JCheckBox          _showplanXmlGuiAuto_chk  = new JCheckBox("Auto Load XML Plan in GUI");
	private JLabel             _showplanSsName_lbl      = new JLabel("Name");
	private JTextField         _showplanSsName_txt      = new JTextField();

	public SqlCaptureOfflineView(Window guiOwner)
	{
		super();
		_guiOwner = guiOwner;

		init();
	}

	private void init()
	{
		setTitle("SQL Capture, offline view");

		ImageIcon icon16 = SwingUtils.readImageIcon(Version.class, "images/sql_capture_offline_view_16.png");
		ImageIcon icon32 = null; //= SwingUtils.readImageIcon(Version.class, "images/sql_capture_offline_view_32.png");
		if (icon16 != null || icon32 != null)
		{
			ArrayList<Image> iconList = new ArrayList<Image>();
			if (icon16 != null) iconList.add(icon16.getImage());
			if (icon32 != null) iconList.add(icon32.getImage());

			setIconImages(iconList);
		}

		_search_panel        = createTopPanel();
		_statements_panel    = createStatementsPanel();
		_sql_panel           = createSqlPanel();
		_statementsSum_panel = createStatementsSumPanel();
		_sqlSum_panel        = createSqlSumPanel();
		_sqlText_panel       = createSqlTextPanel();
		_showplan_panel      = createShowplanPanel();
		_udq_panel           = createUserDefinedQuery();

		_tabbedPane.addTab("Statements",         _statements_panel);
		_tabbedPane.addTab("SQL",                _sql_panel);
		_tabbedPane.addTab("Statements Summary", _statementsSum_panel);
		_tabbedPane.addTab("SQL Summary",        _sqlSum_panel);
		_tabbedPane.addTab("User Defined Query", _udq_panel);

		_topBot_split.setTopComponent(_tabbedPane);
//		_topBot_split.setTopComponent(_statements_panel);
		_topBot_split.setBottomComponent(_sqlPlan_split);

		_sqlPlan_split.setLeftComponent(_sqlText_panel);
		_sqlPlan_split.setRightComponent(_showplan_panel);

		_topBot_split.setDividerLocation(0.5);
		_sqlPlan_split.setDividerLocation(0.5);

//		_topBot_split.setTopComponent(_sqlStmnt_split);
//		_topBot_split.setBottomComponent(_sqlPlan_split);
//
//		_sqlStmnt_split.setTopComponent(_statements_panel);
//		_sqlStmnt_split.setBottomComponent(_sql_panel);
//
//		_sqlPlan_split.setLeftComponent(_sqlText_panel);
//		_sqlPlan_split.setRightComponent(_showplan_panel);
		
		
//		setLayout(new MigLayout());
//		add(_search_panel,     "dock north");
//		add(_statements_panel, "dock center");
//		add(_sqlText_panel,        "dock south");
//		add(_showplan_panel,   "dock south");
		setLayout(new MigLayout());
		add(_search_panel,     "dock north");
		add(_topBot_split,     "dock center");

		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				saveProps();
				dispose();
			}
		});
		pack();
		loadProps();
		getSavedWindowProps();

//		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		setSize(1024, 768);
	}

	/**
	 * Local GTable object so we can change some properties in an easy way, like adding right click menu... 
	 */
	private static class GTableLocal 
	extends GTable
	{
		private static final long serialVersionUID = 1L;

		public GTableLocal()
		{
			super();
		}

		public void createDataTablePopupMenu()
		{
			JPopupMenu popup = new JPopupMenu();

			// Create COPY entries
			TablePopupFactory.createCopyTable(popup);

			// Add a popup menu
			setComponentPopupMenu( popup );
		}
	}

	private JPanel createTopPanel()
	{
		JPanel panel = SwingUtils.createPanel("Search Information", true);
//		panel.setLayout(new MigLayout("debug, insets 0 0 0 0"));
		panel.setLayout(new MigLayout("insets 0 0 0 0"));

		_searchFrom_lbl   .setToolTipText("<html>Search data from this data, in the format 'YYYY-MM-DD hh:mm:ss'</html>");
		_searchFrom_dtp   .setToolTipText(_searchFrom_lbl.getToolTipText());
		_searchFrom_but   .setToolTipText("<html>Open a dialog where you can choose a start date</html>");
		_searchTo_lbl     .setToolTipText("<html>Search data until this data, in the format 'YYYY-MM-DD hh:mm:ss'</html>");
		_searchTo_dtp     .setToolTipText(_searchTo_lbl.getToolTipText());
		_searchTo_but     .setToolTipText("<html>Open a dialog where you can choose a end date</html>");
		_selectedRange_lbl.setToolTipText("How long is the persiod you have selected, in HH:MM:SS.millisec");
		_selectedRange_txt.setToolTipText(_selectedRange_lbl.getToolTipText());
		_search_but       .setToolTipText("<html>Load the desired period from the storage.<br>Note: Check the 'Auto Load Period' if you want the data to be loaded whenever you change period in the Main Windows timeline.</html>");
		_searchAuto_chk   .setToolTipText("<html>When you move the position in the timeline, automatically fill in the 'from' and 'Until' period, and 'load' the data.</html>");

		_searchFrom_dtp.setFormats("yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd", "yyyy-MM-dd HH", "yyyy-MM-dd HH:mm", "yyyy-MM-dd HH:mm:ss");
		_searchTo_dtp  .setFormats("yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd", "yyyy-MM-dd HH", "yyyy-MM-dd HH:mm", "yyyy-MM-dd HH:mm:ss");
//		_searchFrom_dtp.setTimeFormat( new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS") );
//		_searchTo_dtp  .setTimeFormat( new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS") );
		_searchFrom_dtp.setTimeFormat( new SimpleDateFormat("HH:mm:ss") );
		_searchTo_dtp  .setTimeFormat( new SimpleDateFormat("HH:mm:ss") );

		panel.add(_searchRange_lbl,       "");
		panel.add(_searchRange_rsl,       "span, growx, pushx, wrap");
		
		panel.add(_searchFrom_lbl,        "");
		panel.add(_searchFrom_dtp,        "split, width 45mm");
		panel.add(_searchFrom_but,        "wrap");

		panel.add(_searchTo_lbl,          "");
		panel.add(_searchTo_dtp,          "split, width 45mm");
		panel.add(_searchTo_but,          "");
		panel.add(_selectedRange_lbl,     "gapleft 20");
		panel.add(_selectedRange_txt,     "wrap");

		panel.add(_search_but,            "span, split");
		panel.add(_searchAuto_chk,        "");
		panel.add(_queryTimeout_lbl,      "gapleft 20");
		panel.add(_queryTimeout_txt,      "");
		panel.add(new JLabel(),           "growx");
		panel.add(_changeSqlCapProp_but,  "right, hidemode 3, wrap");
		
		// Add action listener
		_searchFrom_dtp      .addActionListener(this);
		_searchFrom_but      .addActionListener(this);
		_searchTo_dtp        .addActionListener(this);
		_searchTo_but        .addActionListener(this);
		_search_but          .addActionListener(this);
		_changeSqlCapProp_but.addActionListener(this);

		// Focus action listener

		// Change Listerner
		_searchRange_rsl.addChangeListener(this);

		_searchRange_rsl.setToolTipText("Move the sliders to a start/stop time range");
		_searchRange_rsl.setMinimum(0);
		_searchRange_rsl.setMaximum(0);
//		_searchRange_rsl.setPaintLabels(true);
		_searchRange_rsl.setPaintTicks(true);
		_searchRange_rsl.setPaintTrack(true);
		_searchRange_rsl.setMajorTickSpacing(10);
		_searchRange_rsl.setMinorTickSpacing(1);
		
		if (MainFrame.hasInstance())
		{
			_searchRange_list = MainFrame.getInstance().getOfflineSliderTsList();
			if (_searchRange_list != null)
			{
				_searchRange_rsl.setMinimum(0);
				_searchRange_rsl.setMaximum(_searchRange_list.size()-1);
			}

		}

		
		// Default visability
		_changeSqlCapProp_but.setVisible(false);
		if (PersistentCounterHandler.hasInstance())
		{
			if (PersistentCounterHandler.getInstance().getSqlCaptureBroker() != null)
				_changeSqlCapProp_but.setVisible(true);
		}
		
		return panel;
	}

	private JPanel createStatementsPanel()
	{
		JPanel panel = SwingUtils.createPanel("Statements", true);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));

		_statements_tab.setName(PROPKEY_BASE+"_statements_tab");
		_statements_tab.setSortable(true);
		_statements_tab.setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);
		_statements_tab.packAll(); // set size so that all content in all cells are visible
		_statements_tab.setColumnControlVisible(true);
//		_statements_tab.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_statements_tab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		_statements_tab.createDataTablePopupMenu();
		
		panel.add(_statementsExec_but,      "split");
		panel.add(_statementsFilter,        "growx, pushx, wrap");
		panel.add(_statements_scroll,       "grow, push");

		_statementsExec_but.addActionListener(this);
		_statementsExec_but.setToolTipText("");
		_statementsExec_but.setUseFocusableTips(true);
		_statementsExec_but.setUseFocusableTipsSize(10);
		
		// Table Row is SELECTED
		_statements_tab.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				if (e.getValueIsAdjusting())
					return;

				GTable table = _statements_tab;

				int     vrow     = table.getSelectedRow();
				Integer spid     = null;
				Integer kpid     = null;
				Integer batchId  = null;
				String  procName = null;

				try { spid     = table.getSelectedValuesAsInteger("SPID");     } catch(RuntimeException ignore) { /* ignore */ }
				try { kpid     = table.getSelectedValuesAsInteger("KPID");     } catch(RuntimeException ignore) { /* ignore */ }
				try { batchId  = table.getSelectedValuesAsInteger("BatchID");  } catch(RuntimeException ignore) { /* ignore */ }
				try { procName = table.getSelectedValuesAsString ("ProcName"); } catch(RuntimeException ignore) { /* ignore */ }

				if (spid == null || kpid == null || batchId == null)
				{
					_logger.info("The "+table.getName()+" ResultSet Table do not contain columns 'SPID', 'KPID' and 'BatchID'. Can't load SQL and Plan Text for current selected row ("+vrow+").");
				}
				else
				{
					loadSqlTextAndPlanText(spid, kpid, batchId, procName);
				}
			}
		});
		
		return panel;
	}

	private JPanel createSqlPanel()
	{
		JPanel panel = SwingUtils.createPanel("SQL Text List", true);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));

		_sql_tab.setName(PROPKEY_BASE+"_sql_tab");
		_sql_tab.setSortable(true);
		_sql_tab.setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);
		_sql_tab.packAll(); // set size so that all content in all cells are visible
		_sql_tab.setColumnControlVisible(true);
//		_sql_tab.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_sql_tab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		_sql_tab.createDataTablePopupMenu();
		
		panel.add(_sqlExec_but,      "split");
		panel.add(_sqlFilter,        "growx, pushx, wrap");
		panel.add(_sql_scroll,       "grow, push");

		_sqlExec_but.addActionListener(this);
		_sqlExec_but.setToolTipText("");
		_sqlExec_but.setUseFocusableTips(true);
		_sqlExec_but.setUseFocusableTipsSize(10);
		
		// Table Row is SELECTED
		_sql_tab.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				if (e.getValueIsAdjusting())
					return;

				GTable table = _sql_tab;

				int     vrow     = table.getSelectedRow();
				Integer spid     = null;
				Integer kpid     = null;
				Integer batchId  = null;
				String  procName = null;

				try { spid     = table.getSelectedValuesAsInteger("SPID");     } catch(RuntimeException ignore) { /* ignore */ }
				try { kpid     = table.getSelectedValuesAsInteger("KPID");     } catch(RuntimeException ignore) { /* ignore */ }
				try { batchId  = table.getSelectedValuesAsInteger("BatchID");  } catch(RuntimeException ignore) { /* ignore */ }
				//try { procName = table.getSelectedValuesAsString ("ProcName"); } catch(RuntimeException ignore) { /* ignore */ }

				if (spid == null || kpid == null || batchId == null)
				{
					_logger.info("The "+table.getName()+" ResultSet Table do not contain columns 'SPID', 'KPID' and 'BatchID'. Can't load SQL and Plan Text for current selected row ("+vrow+").");
				}
				else
				{
					_statementsFilter.setFilterText("WHERE SPID = "+spid+" and KPID = "+kpid+" and BatchID = "+batchId);
					loadSqlTextAndPlanText(spid, kpid, batchId, procName);
				}
			}
		});
		
		return panel;
	}

	private JPanel createStatementsSumPanel()
	{
		JPanel panel = SwingUtils.createPanel("Statements Summary", true);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));

		_statementsSum_tab.setName(PROPKEY_BASE+"_statementsSum_tab");
		_statementsSum_tab.setSortable(true);
		_statementsSum_tab.setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);
		_statementsSum_tab.packAll(); // set size so that all content in all cells are visible
		_statementsSum_tab.setColumnControlVisible(true);
//		_statementsSum_tab.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_statementsSum_tab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		_statementsSum_tab.createDataTablePopupMenu();
		
		panel.add(_statementsSumExec_but,      "split");
		panel.add(_statementsSum_lbl,          "");
		panel.add(_statementsSum_sp,           "wrap");
		panel.add(_statementsSumFilter,        "growx, pushx, wrap");
		panel.add(_statementsSum_scroll,       "grow, push");

		_statementsSumExec_but.addActionListener(this);
		_statementsSumExec_but.setToolTipText("");
		_statementsSumExec_but.setUseFocusableTips(true);
		_statementsSumExec_but.setUseFocusableTipsSize(10);

		// Table Row is SELECTED
		_statementsSum_tab.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				if (e.getValueIsAdjusting())
					return;

				String  procName = _statementsSum_tab.getSelectedValuesAsString("ProcName");

				if (procName != null && (procName.startsWith("*ss") || procName.startsWith("*sq")))
				{
					_showplanSsName_txt.setText(procName);

					if (XmlPlanCache.hasInstance())
					{
						String xmlPlan = XmlPlanCache.getInstance().getPlan(procName);
						_showplan_txt.setText(xmlPlan);
						_showplan_txt.setCaretPosition(0);
					}
					if (_showplanXmlGuiAuto_chk.isSelected())
					{
						_showplanXmlGui_but.doClick();
					}
				}
			}
		});
		
		return panel;
	}

	private JPanel createSqlSumPanel()
	{
		JPanel panel = SwingUtils.createPanel("SQL Text Summary", true);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));

		_sqlSum_tab.setName(PROPKEY_BASE+"_sqlSum_tab");
		_sqlSum_tab.setSortable(true);
		_sqlSum_tab.setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);
		_sqlSum_tab.packAll(); // set size so that all content in all cells are visible
		_sqlSum_tab.setColumnControlVisible(true);
//		_sqlSum_tab.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_sqlSum_tab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		_sqlSum_tab.createDataTablePopupMenu();
		
		panel.add(_sqlSumExec_but,      "split");
		panel.add(_sqlSum_lbl,          "");
		panel.add(_sqlSum_sp,           "wrap");
		panel.add(_sqlSumFilter,        "growx, pushx, wrap");
		panel.add(_sqlSum_scroll,       "grow, push");

		_sqlSumExec_but.addActionListener(this);
		_sqlSumExec_but.setToolTipText("");
		_sqlSumExec_but.setUseFocusableTips(true);
		_sqlSumExec_but.setUseFocusableTipsSize(10);

		return panel;
	}

	private JPanel createUserDefinedQuery()
	{
		JPanel panelLeft = SwingUtils.createPanel("User Defined Query", true);
		panelLeft.setLayout(new MigLayout("insets 0 0 0 0"));

		JPanel panelRight = SwingUtils.createPanel("SQL To be executed", true);
		panelRight.setLayout(new MigLayout("insets 0 0 0 0"));

		_udqSqlText_txt.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
		RSyntaxUtilitiesX.installRightClickMenuExtentions(_udqSqlText_txt, _udqSqlText_scroll, this);

		_udqSqlText_txt.setText("-- Write a SQL Statement here, press 'Execute' button to execute it."); // Note: this is also done in loadProps()
		
		_udq_tab.setName(PROPKEY_BASE+"_udq_tab");
		_udq_tab.setSortable(true);
		_udq_tab.setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);
		_udq_tab.packAll(); // set size so that all content in all cells are visible
		_udq_tab.setColumnControlVisible(true);
//		_udq_tab.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_udq_tab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		_udq_tab.createDataTablePopupMenu();
		
		// LEFT Panel
		panelLeft.add(_udqFilter,        "growx, pushx, wrap");
		panelLeft.add(_udq_scroll,       "grow, push");

		// RIGHT Panel
		panelRight.add(_udqExec_but,       "split");
		panelRight.add(_udqSqlw_but,       "wrap");
		panelRight.add(_udqSqlText_scroll, "grow, push");

		_udqExec_but.addActionListener(this);
		_udqExec_but.setToolTipText("");
		_udqExec_but.setUseFocusableTips(true);
		_udqExec_but.setUseFocusableTipsSize(10);

		_udqSqlw_but.addActionListener(this);
		_udqSqlw_but.setToolTipText("Start SQL Window, in this window you will have full Code Compleation and other functionality.");

		_udq_splitpane.setDividerLocation(0.5);
		_udq_splitpane.setLeftComponent(panelLeft);
		_udq_splitpane.setRightComponent(panelRight);

		// Table Row is SELECTED
		_udq_tab.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				if (e.getValueIsAdjusting())
					return;

				GTable table = _udq_tab;

				int     vrow     = table.getSelectedRow();
				Integer spid     = null;
				Integer kpid     = null;
				Integer batchId  = null;
				String  procName = null;

				try { spid     = table.getSelectedValuesAsInteger("SPID");     } catch(RuntimeException ignore) { /* ignore */ }
				try { kpid     = table.getSelectedValuesAsInteger("KPID");     } catch(RuntimeException ignore) { /* ignore */ }
				try { batchId  = table.getSelectedValuesAsInteger("BatchID");  } catch(RuntimeException ignore) { /* ignore */ }
				try { procName = table.getSelectedValuesAsString ("ProcName"); } catch(RuntimeException ignore) { /* ignore */ }

				if (spid == null || kpid == null || batchId == null)
				{
					_logger.info("The "+table.getName()+" ResultSet Table do not contain columns 'SPID', 'KPID' and 'BatchID'. Can't load SQL and Plan Text for current selected row ("+vrow+").");
				}
				else
				{
					loadSqlTextAndPlanText(spid, kpid, batchId, procName);
				}
			}
		});
		
		JPanel p = new JPanel(new MigLayout("insets 0 0 0 0"));
		p.add(_udq_splitpane, "grow, push");
		
		return p;
	}

	private JPanel createSqlTextPanel()
	{
		JPanel panel = SwingUtils.createPanel("SQL Text", true);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));

		_sqlTextFormat_but    .setToolTipText("<html>Format the SQL Statement, using a <i>pretty print</i> formater.</html>");
		_sqlTextFormatAuto_chk.setToolTipText("<html>Automatically Format the SQL Statement, using a <i>pretty print</i> formater.</html>");

		_sqlText_txt.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
		RSyntaxUtilitiesX.installRightClickMenuExtentions(_sqlText_txt, _sqlText_scroll, this);

		panel.add(_sqlTextFormatAuto_chk,   "split");
		panel.add(_sqlTextFormat_but,       "");
		panel.add(_sqlTextCurSpid_lbl1,     "");
		panel.add(_sqlTextCurSpid_lbl2,     "");
		panel.add(_sqlTextCurKpid_lbl1,     "");
		panel.add(_sqlTextCurKpid_lbl2,     "");
		panel.add(_sqlTextCurBatchId_lbl1,  "");
		panel.add(_sqlTextCurBatchId_lbl2,  "");
		panel.add(_sqlTextCurProcName_lbl1, "");
		panel.add(_sqlTextCurProcName_lbl2, "wrap");
		panel.add(_sqlText_scroll,          "grow, push");

		// Make the font BOLD
		Font f = _sqlTextCurSpid_lbl2.getFont();
		_sqlTextCurSpid_lbl2    .setFont(f.deriveFont(f.getStyle() | Font.BOLD));
		_sqlTextCurKpid_lbl2    .setFont(f.deriveFont(f.getStyle() | Font.BOLD));
		_sqlTextCurBatchId_lbl2 .setFont(f.deriveFont(f.getStyle() | Font.BOLD));
		_sqlTextCurProcName_lbl2.setFont(f.deriveFont(f.getStyle() | Font.BOLD));

		_sqlTextFormat_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Use the "built-in" SQL Formater (available on the right click menu) 
				Action action = _sqlText_txt.getActionMap().get(RSyntaxTextAreaX.formatSql);
				if (action != null)
					action.actionPerformed( new ActionEvent(_sqlText_txt, 0, RSyntaxTextAreaX.formatSql) );
			}
		});
		return panel;
	}

	private JPanel createShowplanPanel()
	{
		JPanel panel = SwingUtils.createPanel("Showplan", true);
		panel.setLayout(new MigLayout("insets 0 0 0 0"));

		_showplanXmlGui_but    .setToolTipText("<html>Load XML Plan in a special GUI that will visualize the execution plan i a tree like structure</html>");
		_showplanXmlGuiAuto_chk.setToolTipText("<html>Automatically Load XML Plan in a special GUI that will visualize the execution plan i a tree like structure</html>");

		_showplan_txt.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
		RSyntaxUtilitiesX.installRightClickMenuExtentions(_showplan_txt, _showplan_scroll, this);

		panel.add(_showplanXmlGuiAuto_chk,  "split");
		panel.add(_showplanXmlGui_but,      "");
		panel.add(_showplanSsName_lbl,      "");
		panel.add(_showplanSsName_txt,      "growx, spanx, wrap");
		panel.add(_showplan_scroll,         "grow, push");

		_showplanXmlGui_but.addActionListener(this);
		_showplanSsName_txt.addActionListener(this);

		return panel;
	}

	
	@Override
	public void stateChanged(ChangeEvent e)
	{
		int lowVal  = _searchRange_rsl.getLowValue();
		int highVal = _searchRange_rsl.getHighValue();

		if (_searchRange_list != null)
		{
			Timestamp from = _searchRange_list.get(lowVal);
			Timestamp to   = _searchRange_list.get(highVal);

			_searchFrom_dtp.setDate(from == null ? null : new Date(from.getTime()));
    		_searchTo_dtp  .setDate(to   == null ? null : new Date(to  .getTime()));
    		
    		if (from != null && to != null)
    			_selectedRange_txt.setText(TimeUtils.msToTimeStr(to.getTime()-from.getTime()));

    		if (_searchAuto_chk.isSelected() && !_searchRange_rsl.getValueIsAdjusting())
        		_search_but.doClick();
		}
	}

	public void setFromToTime(Timestamp from, Timestamp to, List<Timestamp> list)
	{
		if (list != null)
		{
			if (_searchRange_list == null)
				_searchRange_list = list;

			if (_searchRange_rsl.getMaximum() != list.size()-1)
			{
				_searchRange_list = list;
				_searchRange_rsl.setMinimum(0);
				_searchRange_rsl.setMaximum(list.size()-1);
			}
		}

		if (_searchRange_list != null && _searchAuto_chk.isSelected())
		{
			int fromPos = _searchRange_list.indexOf(from);
			int toPos   = _searchRange_list.indexOf(to);

			_searchRange_rsl.setLowValue(fromPos);
			_searchRange_rsl.setHighValue(toPos);
		}

		if (_searchAuto_chk.isSelected())
		{
//    		_searchFrom_dtp.setText(from == null ? "" : from.toString());
//    		_searchTo_dtp  .setText(to   == null ? "" : to  .toString());
    		_searchFrom_dtp.setDate(from == null ? null : new Date(from.getTime()));
    		_searchTo_dtp  .setDate(to   == null ? null : new Date(to  .getTime()));

    		if (from != null && to != null)
    			_selectedRange_txt.setText(TimeUtils.msToTimeStr(to.getTime()-from.getTime()));
    		
    		_search_but.doClick();
		}
	}

	/**
	 * Get a connection to the Reader or the Offline
	 * @return
	 */
	private DbxConnection getConnection()
	{
//		DbxConnection conn = null;
		
		if (PersistReader.hasInstance())
		{
			PersistReader reader = PersistReader.getInstance();
			return reader.getConnection();
		}

		if (PersistentCounterHandler.hasInstance())
		{
			PersistentCounterHandler pcs = PersistentCounterHandler.getInstance();
			if (pcs.hasWriters())
			{
				List<IPersistWriter> writers = pcs.getWriters();
				for (IPersistWriter writer : writers)
				{
					if (writer instanceof PersistWriterJdbc)
					{
						PersistWriterJdbc jdbcWriter = (PersistWriterJdbc) writer;
						return jdbcWriter.getStorageConnection();
					}
				}
			}
		}

		throw new RuntimeException("The 'PersistReader' or 'PersistentCounterHandler.PersistWriterJdbc' has not been initialized, or a connection from those can't be used.");
	}

	private void loadStuff()
	{
		int selectedTab = _tabbedPane.getSelectedIndex();

		if (selectedTab == 0 ||_tabbedPane.isTabUnDocked(0)) loadStatementsTab();
		if (selectedTab == 1 ||_tabbedPane.isTabUnDocked(1)) loadSqlTextTab();
	}

	private void loadSqlTextAndPlanText(int spid, int kpid, int batchId, String procName)
	{
//		PersistReader reader = PersistReader.getInstance();
//		if (reader == null)
//			throw new RuntimeException("The 'PersistReader' has not been initialized.");

		String sql = "";
		try
		{
			String tabName;
			ResultSet rs;
			RSyntaxTextAreaX ta;

			String where = " where \"SPID\" = "+spid+" and KPID = "+kpid+" and \"BatchID\" = "+batchId;
			
			DbxConnection conn = getConnection();
			Statement stmnt = conn.createStatement();
			stmnt.setQueryTimeout( getQueryTimeoutSetting() );

			// GET SQL TEXT
			ta = _sqlText_txt;
			tabName = PersistWriterBase.getTableName(PersistWriterBase.SQL_CAPTURE_SQLTEXT, null, true);
			sql = "select \"SQLText\" from " + tabName + where;

			rs = stmnt.executeQuery(sql);
			ta.setText("");
			boolean foundSqlText = false;
			while(rs.next())
			{
				foundSqlText = true;
				ta.append(rs.getString(1));
				ta.append("\n");
			}
			rs.close();
			if ( ! foundSqlText )
				ta.setText("-- No SQL Text found: "+where);
			ta.setCaretPosition(0);
			if (_sqlTextFormatAuto_chk.isSelected())
				_sqlTextFormat_but.doClick();
			
			
			// GET SHOWPLAN TEXT
			ta = _showplan_txt;
			tabName = PersistWriterBase.getTableName(PersistWriterBase.SQL_CAPTURE_PLANS, null, true);
			sql = "select \"PlanText\" from " + tabName + where;

			rs = stmnt.executeQuery(sql);
			ta.setText("");
			boolean foundShowPlan = false;
			while(rs.next())
			{
				foundShowPlan = true;
				ta.append(rs.getString(1));
				ta.append("\n");
			}
			rs.close();
			if ( ! foundShowPlan )
				ta.setText("-- No Showplan Text found: "+where);
			ta.setCaretPosition(0);
			
			_sqlTextCurSpid_lbl2    .setText(Integer.toString( spid    ));
			_sqlTextCurKpid_lbl2    .setText(Integer.toString( kpid    ));
			_sqlTextCurBatchId_lbl2 .setText(Integer.toString( batchId ));
			_sqlTextCurProcName_lbl2.setText(procName);
			_showplanSsName_txt     .setText("");
			
			// GET XML Plan...
			if (procName != null && (procName.startsWith("*ss") || procName.startsWith("*sq")))
			{
				_showplanSsName_txt.setText(procName);

				if (XmlPlanCache.hasInstance())
				{
					if ( ! foundShowPlan )
					{
						String xmlPlan = XmlPlanCache.getInstance().getPlan(procName);
						_showplan_txt.setText(xmlPlan);
						_showplan_txt.setCaretPosition(0);
					}
				}
				if (_showplanXmlGuiAuto_chk.isSelected())
				{
					_showplanXmlGui_but.doClick();
				}
			}

		}
		catch(SQLException ex)
		{
			_sqlTextCurSpid_lbl2    .setText("---");
			_sqlTextCurKpid_lbl2    .setText("---");
			_sqlTextCurBatchId_lbl2 .setText("---");
			_sqlTextCurProcName_lbl2.setText("---");

			_sqlText_txt.setText(""+ex);
			
			_logger.error("Problems executing sql='"+sql+"'. Caught: "+ex, ex);
			SwingUtils.showErrorMessage(SqlCaptureOfflineView.this, "SQL Error", "Problems Executing SQL: "+sql, ex);
		}
	}

	private void loadStatementsTab()
	{
		final WaitForExecDialog wait = new WaitForExecDialog(this, "Loading Statement Information");

		// Create the Executor object
		WaitForExecDialog.BgExecutor doWork = new WaitForExecDialog.BgExecutor(wait)
		{
			@Override
			public Object doWork()
			{
				String sql = "";
				try
				{
					ResultSet rs;
					
					DbxConnection conn = getConnection();
					Statement stmnt = conn.createStatement();
					stmnt.setQueryTimeout( getQueryTimeoutSetting() );

					wait.setState("Getting Statement Info");

					sql = getSqlFor_StatementsTab();

					rs = stmnt.executeQuery(sql);
					_statements_tab.setModel(enrichRstm(new ResultSetTableModel(rs, true, "_statements_tab")));
					_statements_tab.packAllGrowOnly();
					_statementsFilter.updateRowCount();
					_statementsFilter.refreshCompletion();
					rs.close();
				}
				catch(SQLException ex)
				{
					_logger.error("Problems executing sql='"+sql+"'. Caught: "+ex, ex);
					SwingUtils.showErrorMessage(SqlCaptureOfflineView.this, "SQL Error", "Problems Executing SQL: "+sql, ex);
				}

				return null;
			}
		};

		// Now Execute and wait for it to finish
		wait.execAndWait(doWork, 200); // 200ms in GraceTime before the GUI popup is raised
	}
	
	/** if NormSQLText is empty, and SQLText has Value... create the NormSQLText content "on the fly" */
	private ResultSetTableModel enrichRstm(ResultSetTableModel rstm)
	{
		boolean disabled = true;
		if (disabled)
			return rstm;

		int SQLText_pos     = rstm.findColumn("SQLText");
		int NormSQLText_pos = rstm.findColumn("NormSQLText");
		
		if (SQLText_pos == -1 || NormSQLText_pos == -1)
		{
			_logger.info("Enriching Statements information was skipped due to: SQLText_pos="+SQLText_pos+", NormSQLText_pos="+NormSQLText_pos);
			return rstm;
		}

		// Used to anonymize or remove-constants in where clauses etc...
		StatementNormalizer stmntNorm = new StatementNormalizer();

		int rowCount = rstm.getRowCount();
		for (int r=0; r<rowCount; r++)
		{
			String sqlText     = (String) rstm.getValueAt(r, SQLText_pos);
			String normSqlText = (String) rstm.getValueAt(r, NormSQLText_pos);
			if (StringUtil.hasValue(sqlText) && StringUtil.isNullOrBlank(normSqlText))
			{
				normSqlText = stmntNorm.normalizeStatementNoThrow(SqlCaptureBrokerAse.removeKnownPrefixes(sqlText));
				rstm.setValueAt(normSqlText, r, NormSQLText_pos);
			}
		}

		return rstm;
	}


	private void loadSqlTextTab()
	{
		final WaitForExecDialog wait = new WaitForExecDialog(this, "Loading SQL Text");

		// Create the Executor object
		WaitForExecDialog.BgExecutor doWork = new WaitForExecDialog.BgExecutor(wait)
		{
			@Override
			public Object doWork()
			{
				String sql = "";
				try
				{
					ResultSet rs;
					
					DbxConnection conn = getConnection();
					Statement stmnt = conn.createStatement();
					stmnt.setQueryTimeout( getQueryTimeoutSetting() );

					wait.setState("Getting SQL Text");

					sql = getSqlFor_SqlTextTab();

					rs = stmnt.executeQuery(sql);
					_sql_tab.setModel(new ResultSetTableModel(rs, true, "_sql_tab"));
					_sql_tab.packAllGrowOnly();
					_sqlFilter.updateRowCount();
					_sqlFilter.refreshCompletion();
					rs.close();
				}
				catch(SQLException ex)
				{
					_logger.error("Problems executing sql='"+sql+"'. Caught: "+ex, ex);
					SwingUtils.showErrorMessage(SqlCaptureOfflineView.this, "SQL Error", "Problems Executing SQL: "+sql, ex);
				}

				return null;
			}
		};

		// Now Execute and wait for it to finish
		wait.execAndWait(doWork, 200); // 200ms in GraceTime before the GUI popup is raised
	}


	private void loadSumStatementsTab()
	{
		final WaitForExecDialog wait = new WaitForExecDialog(this, "Loading Summary Statement Info");

		// Create the Executor object
		WaitForExecDialog.BgExecutor doWork = new WaitForExecDialog.BgExecutor(wait)
		{
			@Override
			public Object doWork()
			{
				String sql = "";
				try
				{
					ResultSet rs;
					DbxConnection conn = getConnection();
					Statement stmnt = conn.createStatement();
					stmnt.setQueryTimeout( getQueryTimeoutSetting() );

					wait.setState("Getting Statement Info");

					sql = getSqlFor_SumStatementsTab();
					rs = stmnt.executeQuery(sql);
					_statementsSum_tab.setModel(new ResultSetTableModel(rs, true, "_statementsSum_tab"));
					_statementsSum_tab.packAllGrowOnly();
					_statementsSumFilter.updateRowCount();
					_statementsSumFilter.refreshCompletion();
					rs.close();
				}
				catch(SQLException ex)
				{
					_logger.error("Problems executing sql='"+sql+"'. Caught: "+ex, ex);
					SwingUtils.showErrorMessage(SqlCaptureOfflineView.this, "SQL Error", "Problems Executing SQL: "+sql, ex);
				}

				return null;
			}
		};

		// Now Execute and wait for it to finish
		wait.execAndWait(doWork, 200); // 200ms in GraceTime before the GUI popup is raised
	}

	private void loadSumSqlTextTab()
	{
		final WaitForExecDialog wait = new WaitForExecDialog(this, "Loading Summary SQL Text");

		// Create the Executor object
		WaitForExecDialog.BgExecutor doWork = new WaitForExecDialog.BgExecutor(wait)
		{
			@Override
			public Object doWork()
			{
				String sql = "";
				try
				{
					ResultSet rs;
					DbxConnection conn = getConnection();
					Statement stmnt = conn.createStatement();
					stmnt.setQueryTimeout( getQueryTimeoutSetting() );

					wait.setState("Getting SQL Text Info");

					sql = getSqlFor_SumSqlTextTab();
					rs = stmnt.executeQuery(sql);
					_sqlSum_tab.setModel(new ResultSetTableModel(rs, true, "_sqlSum_tab"));
					_sqlSum_tab.packAllGrowOnly();
					_sqlSumFilter.updateRowCount();
					_sqlSumFilter.refreshCompletion();
					rs.close();
				}
				catch(SQLException ex)
				{
					_logger.error("Problems executing sql='"+sql+"'. Caught: "+ex, ex);
					SwingUtils.showErrorMessage(SqlCaptureOfflineView.this, "SQL Error", "Problems Executing SQL: "+sql, ex);
				}

				return null;
			}
		};

		// Now Execute and wait for it to finish
		wait.execAndWait(doWork, 200); // 200ms in GraceTime before the GUI popup is raised
	}

	private void loadUserDefinedQueryTab()
	{
		final WaitForExecDialog wait = new WaitForExecDialog(this, "Executing User Defined Query");

		// Create the Executor object
		WaitForExecDialog.BgExecutor doWork = new WaitForExecDialog.BgExecutor(wait)
		{
			@Override
			public Object doWork()
			{
				String sql = "";
				try
				{
					ResultSet rs;
					DbxConnection conn = getConnection();
					Statement stmnt = conn.createStatement();
					stmnt.setQueryTimeout( getQueryTimeoutSetting() );

					wait.setState("Executing...");

					sql = getSqlFor_UserDefinedQueryTab();
					rs = stmnt.executeQuery(sql);
					_udq_tab.setModel(new ResultSetTableModel(rs, true, "_udq_tab"));
					_udq_tab.packAllGrowOnly();
					_udqFilter.updateRowCount();
					_udqFilter.refreshCompletion();
					rs.close();
				}
				catch(SQLException ex)
				{
					_logger.error("Problems executing sql='"+sql+"'. Caught: "+ex, ex);
					SwingUtils.showErrorMessage(SqlCaptureOfflineView.this, "SQL Error", "Problems Executing SQL: "+sql, ex);
				}

				return null;
			}
		};

		// Now Execute and wait for it to finish
		wait.execAndWait(doWork, 200); // 200ms in GraceTime before the GUI popup is raised
	}


	/**
	 * TODO:
	 *       case ....   >< = Execution WITHIN current time limit
	 *                   <  = Execution started BEFORE and ended WITHIN current time limit
	 *                   >  = Execution started AFTER  and ended WITHIN current time limit
	 *                   <> = Execution started BEFORE and are STILL_RUNNING (not finished within current time limit)
	 *       
	 *       WHERE ExecStartTime between ${fromDate} and ${toDate}
	 *          OR ExecEndTime   between ${fromDate} and ${toDate} 
	 *          OR ${fromDate} between ExecStartTime and ExecEndTime
	 * 
	 * @return
	 */
	private String getSqlFor_StatementsTab()
	{
		// Build where clause
		String fromDate = _searchFrom_dtp.getText();
		String toDate   = _searchTo_dtp.getText();
		String where    = " where 1=1 \n";
		String columns  = "  CASE WHEN t.\"SQLText\" is NULL THEN convert(0, bit) ELSE convert(1, bit) END as \"hasSqlText\", \n" +
		                  "  s.\"sampleTime\", s.\"StartTime\", s.\"EndTime\", s.\"Elapsed_ms\", s.\"SPID\", s.\"KPID\", s.\"BatchID\", s.\"LineNumber\", s.\"DBName\", s.\"ProcName\", \n" + 
		                  "  s.\"CpuTime\", s.\"WaitTime\", s.\"MemUsageKB\", s.\"PhysicalReads\", s.\"LogicalReads\", s.\"RowsAffected\", s.\"ErrorStatus\", s.\"ProcNestLevel\",  \n" + 
		                  "  s.\"StatementNumber\", s.\"QueryOptimizationTime\", s.\"PagesModified\", s.\"PacketsSent\", s.\"PacketsReceived\", s.\"NetworkPacketSize\",  \n" + 
		                  "  s.\"PlansAltered\", s.\"ContextID\", s.\"HashKey\", s.\"SsqlId\", s.\"ObjOwnerID\", s.\"InstanceID\", s.\"DBID\", s.\"ProcedureID\", s.\"PlanID\", \n" +
		                  "  s.\"NormJavaSqlHashCode\", s.\"JavaSqlHashCode\", t.\"SQLText\", t.\"NormSQLText\" ";

//		if (StringUtil.hasValue(fromDate))
//			where += " and \"sampleTime\" >= '"+fromDate+"' \n";
//		if (StringUtil.hasValue(toDate))
//			where += " and \"sampleTime\" <= '"+toDate+"' \n";
		if (StringUtil.hasValue(fromDate) && StringUtil.hasValue(toDate))
		{
			where = "WHERE s.\"StartTime\" between '" + fromDate + "' and '" + toDate + "' \n" +
			        "   OR s.\"EndTime\"   between '" + fromDate + "' and '" + toDate + "' \n" +
			        "   OR '" + fromDate + "' between s.\"StartTime\" and s.\"EndTime\" \n " +
			        "ORDER BY s.\"StartTime\"";
			
			columns = 
				"\n" +
				"  CASE \n" +
				"    -- Execution WITHIN current time limit \n" + 
				"    WHEN s.\"StartTime\" between '" + fromDate + "' and '" + toDate + "' AND s.\"EndTime\" between '" + fromDate + "' and '" + toDate + "' \n"+
				"    THEN '<html><b><font color=\"green\" face=\"monospace\">&gt;&lt;</font></b> -- <i>Exec-Within</i></html>'  \n" +
				"\n" +
				"    -- Execution started BEFORE and ended WITHIN current time limit \n" + 
				"    WHEN s.\"StartTime\" < '" + fromDate + "' AND s.\"EndTime\" between '" + fromDate + "' and '" + toDate + "' \n"+
				"    THEN '<html><b><font color=\"orange\" face=\"monospace\">&lt;!</font></b> -- <i>Start-Before</i></html>'  \n" +
				"\n" +
				"    -- Execution started AFTER  and ended WITHIN current time limit \n" + 
				"    WHEN s.\"StartTime\" > '" + fromDate + "' AND s.\"EndTime\" > '" + toDate + "' \n"+
				"    THEN '<html><b><font color=\"orange\" face=\"monospace\">!&gt;</font></b> -- <i>End-After</i></html>'  \n" +
				"\n" +
				"    -- Execution started BEFORE and are STILL_RUNNING (not finished within current time limit) \n" + 
				"    WHEN s.\"StartTime\" < '" + fromDate + "' AND s.\"EndTime\" > '" + toDate + "' \n" +
				"    THEN '<html><b><font color=\"red\" face=\"monospace\">&lt;&gt;</font></b> -- <i>StartEnd-Outside</i></html>'  \n" +
				"\n" +
				"    -- This should not happen... \n" + 
				"    ELSE '<html><b><font face=\"monospace\">??</font></b> -- <i>???</i></html>'  \n" +
				"  END as \"Indicator\", \n" +
				columns
				;
		}
		else if (StringUtil.hasValue(fromDate) || StringUtil.hasValue(toDate))
		{
			if (StringUtil.hasValue(fromDate))
				where += " and s.\"sampleTime\" >= '"+fromDate+"' \n";
			if (StringUtil.hasValue(toDate))
				where += " and s.\"sampleTime\" <= '"+toDate+"' \n";
		}

		// Build SELECT
		String tabNameStmnt   = PersistWriterBase.getTableName(PersistWriterBase.SQL_CAPTURE_STATEMENTS, null, true);
		String tabNameSqlText = PersistWriterBase.getTableName(PersistWriterBase.SQL_CAPTURE_SQLTEXT,    null, true);
		String sql = "SELECT " + columns + " \n"
				+ "FROM " + tabNameStmnt + " s \n"
				+ "LEFT OUTER JOIN "+tabNameSqlText+" t ON s.\"SPID\" = t.\"SPID\" AND s.\"KPID\" = t.\"KPID\" AND s.\"BatchID\" = t.\"BatchID\" \n"
				+ where;
		return sql;
	}

	private String getSqlFor_SqlTextTab()
	{
		// Build where clause
		String fromDate = _searchFrom_dtp.getText();
		String toDate   = _searchTo_dtp.getText();
		String where    = " where 1=1 \n";

		if (StringUtil.hasValue(fromDate))
			where += " and \"sampleTime\" >= '"+fromDate+"' \n";
		if (StringUtil.hasValue(toDate))
			where += " and \"sampleTime\" <= '"+toDate+"' \n";
		
		// Build SELECT
		String tabName = PersistWriterBase.getTableName(PersistWriterBase.SQL_CAPTURE_SQLTEXT, null, true);
		String sql = "select * \n"
				+ "from " + tabName + "\n"
				+ where;
		return sql;
	}

	private String getSqlFor_SumStatementsTab()
	{
		String tabName = PersistWriterBase.getTableName(PersistWriterBase.SQL_CAPTURE_STATEMENTS, null, true);
		String sql = ""
				+ "------------------------------------------------------------- \n"
				+ "-- Get Statements... \n"
				+ "-- Note: SQL that is not withing the StatementCache is presented with a \"ProcName\" of NULL \n"
				+ "--       If you have alot of those you need to compose some other query \n"
				+ "------------------------------------------------------------- \n"
				+ "select top " + _statementsSum_spm.getNumber().intValue() + " \n"
				+ "    \"ProcName\" \n"
				+ "   ,\"LineNumber\" \n"
				+ "   ,count(*)                       as \"records\" \n"
				+ "   ,avg(\"Elapsed_ms\")            as \"avgElapsed_ms\" \n"
				+ "   ,avg(\"CpuTime\")               as \"avgCpuTime\" \n"
				+ "   ,avg(\"WaitTime\")              as \"avgWaitTime\" \n"
				+ "   ,avg(\"MemUsageKB\")            as \"avgMemUsageKB\" \n"
				+ "   ,avg(\"PhysicalReads\")         as \"avgPhysicalReads\" \n"
				+ "   ,avg(\"LogicalReads\")          as \"avgLogicalReads\" \n"
				+ "   ,avg(\"RowsAffected\")          as \"avgRowsAffected\" \n"
				+ "   ,avg(\"QueryOptimizationTime\") as \"avgQueryOptimizationTime\" \n"
				+ "   ,avg(\"PagesModified\")         as \"avgPagesModified\" \n"
				+ "   ,avg(\"PacketsSent\")           as \"avgPacketsSent\" \n"
				+ "   ,avg(\"PacketsReceived\")       as \"avgPacketsReceived\" \n"
				+ "from " + tabName + " \n"
				+ "where 1 = 1 \n"
				+ "group by \"ProcName\", \"LineNumber\" \n"
//				+ "having count(*) > " + _statementsSum_spm.getNumber().intValue() + " \n"
				+ "order by \"records\" desc \n"
				+ " \n"
				+ "------------------------------------------------------------- \n"
				+ "-- Get Statements... NOT IN PROCS or STATEMENT-CACHE, by: JavaSqlHashCode \n"
				+ "-- to execute below: Copy it to 'User Defined Query' \n"
				+ "------------------------------------------------------------- \n"
				+ "/* \n"
				+ "select top " + _statementsSum_spm.getNumber().intValue() + " \n"
				+ "    \"JavaSqlHashCode\" \n"
				+ "   ,count(*)                       as \"records\" \n"
				+ "   ,avg(\"Elapsed_ms\")            as \"avgElapsed_ms\" \n"
				+ "   ,avg(\"CpuTime\")               as \"avgCpuTime\" \n"
				+ "   ,avg(\"WaitTime\")              as \"avgWaitTime\" \n"
				+ "   ,avg(\"MemUsageKB\")            as \"avgMemUsageKB\" \n"
				+ "   ,avg(\"PhysicalReads\")         as \"avgPhysicalReads\" \n"
				+ "   ,avg(\"LogicalReads\")          as \"avgLogicalReads\" \n"
				+ "   ,avg(\"RowsAffected\")          as \"avgRowsAffected\" \n"
				+ "   ,avg(\"QueryOptimizationTime\") as \"avgQueryOptimizationTime\" \n"
				+ "   ,avg(\"PagesModified\")         as \"avgPagesModified\" \n"
				+ "   ,avg(\"PacketsSent\")           as \"avgPacketsSent\" \n"
				+ "   ,avg(\"PacketsReceived\")       as \"avgPacketsReceived\" \n"
				+ "from " + tabName + " \n"
				+ "where \"ProcName\" is NULL \n"
				+ "group by \"JavaSqlHashCode\" \n"
//				+ "having count(*) " + _statementsSum_spm.getNumber().intValue() + " \n"
				+ "order by \"records\" desc \n"
				+ "*/ \n"
				+ "------------------------------------------------------------- \n"
				+ "-- Get Statements... NOT IN PROCS or STATEMENT-CACHE, by: NormJavaSqlHashCode \n"
				+ "-- to execute below: Copy it to 'User Defined Query' \n"
				+ "------------------------------------------------------------- \n"
				+ "/* \n"
				+ "select top " + _statementsSum_spm.getNumber().intValue() + " \n"
				+ "    \"NormJavaSqlHashCode\" \n"
				+ "   ,count(*)                       as \"records\" \n"
				+ "   ,avg(\"Elapsed_ms\")            as \"avgElapsed_ms\" \n"
				+ "   ,avg(\"CpuTime\")               as \"avgCpuTime\" \n"
				+ "   ,avg(\"WaitTime\")              as \"avgWaitTime\" \n"
				+ "   ,avg(\"MemUsageKB\")            as \"avgMemUsageKB\" \n"
				+ "   ,avg(\"PhysicalReads\")         as \"avgPhysicalReads\" \n"
				+ "   ,avg(\"LogicalReads\")          as \"avgLogicalReads\" \n"
				+ "   ,avg(\"RowsAffected\")          as \"avgRowsAffected\" \n"
				+ "   ,avg(\"QueryOptimizationTime\") as \"avgQueryOptimizationTime\" \n"
				+ "   ,avg(\"PagesModified\")         as \"avgPagesModified\" \n"
				+ "   ,avg(\"PacketsSent\")           as \"avgPacketsSent\" \n"
				+ "   ,avg(\"PacketsReceived\")       as \"avgPacketsReceived\" \n"
				+ "from " + tabName + " \n"
				+ "where \"ProcName\" is NULL \n"
				+ "group by \"NormJavaSqlHashCode\" \n"
//				+ "having count(*) " + _statementsSum_spm.getNumber().intValue() + " \n"
				+ "order by \"records\" desc \n"
				+ "*/ \n"
				+ "";
		return sql;
	}

	private String getSqlFor_SumSqlTextTab()
	{
		String tabName = PersistWriterBase.getTableName(PersistWriterBase.SQL_CAPTURE_SQLTEXT, null, true);
		String sql = ""
				+ "------------------------------------------------------------- \n"
				+ "-- Get SQL Text that has been executed mostly \n"
				+ "------------------------------------------------------------- \n"
				+ "select top " + _sqlSum_spm.getNumber().intValue() + " \n"
				+ "    count(*) as \"records\" \n"
				+ "   ,\"SQLText\" \n"
				+ "from " + tabName + " \n"
				+ "where 1 = 1 \n"
				+ "group by \"SQLText\" \n"
//				+ "having count(*) > " + _sqlSum_spm.getNumber().intValue() + " \n"
				+ "order by \"records\" desc \n"
				+ "\n"
				+ "\n"
				+ "/*----------------------------------------------------------- \n"
				+ " *---- to execute below: Copy it to 'User Defined Query' \n"
				+ " *----------------------------------------------------------- \n"
				+ "select top " + _sqlSum_spm.getNumber().intValue() + " \n"
				+ "    \"JavaSqlHashCode\" \n"
				+ "   ,count(*) as \"records\" \n"
				+ "from " + tabName + " \n"
				+ "where 1 = 1 \n"
				+ "group by \"JavaSqlHashCode\" \n"
//				+ "having count(*) > " + _sqlSum_spm.getNumber().intValue() + " \n"
				+ "order by \"records\" desc \n"
				+ "------------------------------------------------------------- */ \n"
				+ "\n"
				+ "/*----------------------------------------------------------- \n"
				+ "  ---- to execute below: Copy it to 'User Defined Query' \n"
				+ "  ----------------------------------------------------------- \n"
				+ "select top " + _sqlSum_spm.getNumber().intValue() + " \n"
				+ "    \"NormJavaSqlHashCode\" \n"
				+ "   ,count(*) as \"records\" \n"
				+ "from " + tabName + " \n"
				+ "where 1 = 1 \n"
				+ "group by \"NormJavaSqlHashCode\" \n"
//				+ "having count(*) > " + _sqlSum_spm.getNumber().intValue() + " \n"
				+ "order by \"records\" desc \n"
				+ "------------------------------------------------------------- */ \n"
				+ "";
		return sql;
	}


	private String getSqlFor_UserDefinedQueryTab()
	{
		String sql = _udqSqlText_txt.getSelectedText();
		
		if (StringUtil.isNullOrBlank(sql))
			sql = _udqSqlText_txt.getText();

		return sql;
	}


	/*---------------------------------------------------
	** BEGIN: implementing ActionListener
	**---------------------------------------------------
	*/	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();
		
		// LOAD/SEARCH
		if (_search_but.equals(source))
		{
			loadStuff();
		}

		// FROM DATE
		if (_searchFrom_dtp.equals(source))
		{
			_search_but.doClick();
		}
		// TO DATE
		if (_searchTo_dtp.equals(source))
		{
			_search_but.doClick();
		}
		
		// FROM DATE: Button ...
		if (_searchFrom_but.equals(source))
		{
			if (_searchRange_list != null)
			{
				TimestampPickList pl = new TimestampPickList(this, _searchRange_list, "From: Date and Time");
				Timestamp ts = pl.getTimestamp();
				if (ts != null)
					_searchFrom_dtp.setDate( new Date(ts.getTime()) );
			}
		}
		// TO DATE Button ...
		if (_searchTo_but.equals(source))
		{
			if (_searchRange_list != null)
			{
				TimestampPickList pl = new TimestampPickList(this, _searchRange_list, "To: Date and Time");
				Timestamp ts = pl.getTimestamp();
				if (ts != null)
					_searchTo_dtp.setDate( new Date(ts.getTime()) );
			}
		}
		
		// BUTTON: SQL Exec
		if (_statementsExec_but.equals(source))
		{
			loadStatementsTab();
		}

		// BUTTON: SQL Exec
		if (_sqlExec_but.equals(source))
		{
			loadSqlTextTab();
		}

		// BUTTON: Statements SUM Exec
		if (_statementsSumExec_but.equals(source))
		{
			loadSumStatementsTab();
		}

		// BUTTON: SQL SUM Exec
		if (_sqlSumExec_but.equals(source))
		{
			loadSumSqlTextTab();
		}

		// BUTTON: Exec - User Defined SQL
		if (_udqExec_but.equals(source))
		{
			loadUserDefinedQueryTab();
		}
		
		// BUTTON: Exec - User Defined SQL
		if (_udqSqlw_but.equals(source))
		{
			// to JDBC OFFLINE database
			//if (isOfflineConnected() && PersistReader.hasInstance())
//			if (PersistReader.hasInstance())
			if (MainFrame.hasInstance())
			{
				// FIXME: This must be done in a better way... do not grab the MainFrame instance
				//        This was done du to that the PersistReader hasn't yet implemeted getNewConnection(appname);
				try 
				{
					//DbxConnection conn = PersistReader.getInstance().getNewConnection(Version.getAppName()+"-QueryWindow");
					DbxConnection conn = MainFrame.getInstance().getNewConnection(Version.getAppName()+"-QueryWindow");
					QueryWindow qf = new QueryWindow(conn, true, WindowType.JFRAME);
					qf.openTheWindow();
				}
				catch (Exception ex) 
				{
					SwingUtils.showErrorMessage(this, "Error", "Problems open 'offline connection' for SQL Query Window\n" + ex, ex);
				}
			}
			else
			{
				//SwingUtils.showErrorMessage(this, "Error", "Could not find any 'PersistReader' instance.\nAre you connected in 'offline' mode?", null);
				SwingUtils.showErrorMessage(this, "Error", "Could not find any 'MainFrame' instance.", null);
			}
		}
		
		// BUTTON: Load XML Plan
		if (_showplanXmlGui_but.equals(source))
		{
			AsePlanViewer.getInstance().loadXmlFromCacheDeferred(_showplanSsName_txt.getText());
		}
		// TextField: Statement Plan Name
		if (_showplanSsName_txt.equals(source))
		{
			_showplanXmlGui_but.doClick();
		}

		//-----------------------------------
		// Change SQL Capture Dialog
		//-----------------------------------
		if (_changeSqlCapProp_but.equals(source))
		{
			//Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
			Configuration conf = null;
			if (PersistentCounterHandler.hasInstance())
			{
				if (PersistentCounterHandler.getInstance().getSqlCaptureBroker() != null)
					conf = PersistentCounterHandler.getInstance().getSqlCaptureBroker().getConfiguration();
			}

			ChangeSqlCaptureSettingsDialog.showDialog(_guiOwner, conf, null);
		}
		
		saveProps();
	}
	/*---------------------------------------------------
	** END: implementing ActionListener
	**---------------------------------------------------
	*/

	private int getQueryTimeoutSetting()
	{
		String str = _queryTimeout_txt.getText();
		int timeout = StringUtil.parseInt(str, DEFAULT_queryTimeout);
		if (timeout < 0)
			timeout = 0;
		
		_queryTimeout_txt.setText( Integer.toString(timeout) );
		return timeout;
	}
	
	/*---------------------------------------------------
	** BEGIN: implementing saveProps & loadProps
	**---------------------------------------------------
	*/
	private static final String PROPKEY_queryTimeout = PROPKEY_BASE + "query.timeout";
	private static final int    DEFAULT_queryTimeout = 0;
	
	private void saveProps()
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null)
		{
			_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
			return;
		}

		//----------------------------------
		// Settings
		//----------------------------------
		conf.setProperty(PROPKEY_BASE + "search.auto",                       _searchAuto_chk          .isSelected());
		conf.setProperty(PROPKEY_BASE + "search.from",                       _searchTo_dtp            .getText());
		conf.setProperty(PROPKEY_BASE + "search.to",                         _searchFrom_dtp          .getText());
		conf.setProperty(PROPKEY_queryTimeout,                               _queryTimeout_txt        .getText());
                                                                                                     
		conf.setProperty(PROPKEY_BASE + "statements.tab.filter.selected",    _statementsFilter       .isFilterChkboxSelected());
		conf.setProperty(PROPKEY_BASE + "statements.tab.filter.text",        _statementsFilter       .getText());
                                                                                                     
		conf.setProperty(PROPKEY_BASE + "sqlText.tab.filter.selected",       _sqlFilter              .isFilterChkboxSelected());
		conf.setProperty(PROPKEY_BASE + "sqlText.tab.filter.text",           _sqlFilter              .getText());
                                                                                                     
		conf.setProperty(PROPKEY_BASE + "statementsSum.tab.filter.selected", _statementsSumFilter    .isFilterChkboxSelected());
		conf.setProperty(PROPKEY_BASE + "statementsSum.tab.filter.text",     _statementsSumFilter    .getText());
		conf.setProperty(PROPKEY_BASE + "statementsSum.tab.restrict.count",  _statementsSum_spm      .getNumber().intValue());
                                                                                                     
		conf.setProperty(PROPKEY_BASE + "sqlTextSum.tab.filter.selected",    _sqlSumFilter           .isFilterChkboxSelected());
		conf.setProperty(PROPKEY_BASE + "sqlTextSum.tab.filter.text",        _sqlSumFilter           .getText());
		conf.setProperty(PROPKEY_BASE + "sqlTextSum.tab.restrict.count",     _sqlSum_spm             .getNumber().intValue());

		conf.setProperty(PROPKEY_BASE + "udq.tab.filter.selected",           _udqFilter              .isFilterChkboxSelected());
		conf.setProperty(PROPKEY_BASE + "udq.tab.filter.text",               _udqFilter              .getText());
		conf.setProperty(PROPKEY_BASE + "udq.tab.editor.text",               _udqSqlText_txt         .getText());

		conf.setProperty(PROPKEY_BASE + "sqlText.autoFormat",                _sqlTextFormatAuto_chk  .isSelected());
		conf.setProperty(PROPKEY_BASE + "showplan.autoXmlPlan",              _showplanXmlGuiAuto_chk .isSelected());
		
		//TODO: save various filters Text, filter CheckBox
		

		//------------------
		// WINDOW
		//------------------
		conf.setLayoutProperty(PROPKEY_BASE + "dialog.window.width",  this.getSize().width);
		conf.setLayoutProperty(PROPKEY_BASE + "dialog.window.height", this.getSize().height);
		conf.setLayoutProperty(PROPKEY_BASE + "dialog.window.pos.x",  this.getLocationOnScreen().x);
		conf.setLayoutProperty(PROPKEY_BASE + "dialog.window.pos.y",  this.getLocationOnScreen().y);

		conf.setLayoutProperty(PROPKEY_BASE + "dialog.splitpane.topBot.pos",  _topBot_split.getDividerLocation());
		conf.setLayoutProperty(PROPKEY_BASE + "dialog.splitpane.sqlPlan.pos", _sqlPlan_split.getDividerLocation());
		conf.setLayoutProperty(PROPKEY_BASE + "dialog.splitpane.udq.pos",     _udq_splitpane.getDividerLocation());

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
		// Settings
		//----------------------------------
		_searchAuto_chk         .setSelected(            conf.getBooleanProperty(PROPKEY_BASE + "search.auto",                       _searchAuto_chk.isSelected()));
		_searchTo_dtp           .setText(                conf.getProperty(       PROPKEY_BASE + "search.from",                       ""));
		_searchFrom_dtp         .setText(                conf.getProperty(       PROPKEY_BASE + "search.to",                         ""));
		_queryTimeout_txt       .setText(                conf.getProperty(       PROPKEY_queryTimeout,                               "0"));

		_statementsFilter       .setFilterChkboxSelected(conf.getBooleanProperty(PROPKEY_BASE + "statements.tab.filter.selected",    true));
		_statementsFilter       .setText(                conf.getProperty(       PROPKEY_BASE + "statements.tab.filter.text",        ""));

		_sqlFilter              .setFilterChkboxSelected(conf.getBooleanProperty(PROPKEY_BASE + "sqlText.tab.filter.selected",       true));
		_sqlFilter              .setText(                conf.getProperty(       PROPKEY_BASE + "sqlText.tab.filter.text",           ""));

		_statementsSumFilter    .setFilterChkboxSelected(conf.getBooleanProperty(PROPKEY_BASE + "statementsSum.tab.filter.selected", true));
		_statementsSumFilter    .setText(                conf.getProperty(       PROPKEY_BASE + "statementsSum.tab.filter.text",     ""));
		_statementsSum_spm      .setValue(               conf.getIntProperty(    PROPKEY_BASE + "statementsSum.tab.restrict.count",  100));

		_sqlSumFilter           .setFilterChkboxSelected(conf.getBooleanProperty(PROPKEY_BASE + "sqlTextSum.tab.filter.selected",    true));
		_sqlSumFilter           .setText(                conf.getProperty(       PROPKEY_BASE + "sqlTextSum.tab.filter.text",        ""));
		_sqlSum_spm             .setValue(               conf.getIntProperty(    PROPKEY_BASE + "sqlTextSum.tab.restrict.count",     100));

		_udqFilter              .setFilterChkboxSelected(conf.getBooleanProperty(PROPKEY_BASE + "udq.tab.filter.selected",           true));
		_udqFilter              .setText(                conf.getProperty(       PROPKEY_BASE + "udq.tab.filter.text",               ""));
		_udqSqlText_txt         .setText(                conf.getProperty(       PROPKEY_BASE + "udq.tab.editor.text",               "-- Write a SQL Statement here, press 'Execute' button to execute it."));

		_sqlTextFormatAuto_chk  .setSelected(            conf.getBooleanProperty(PROPKEY_BASE + "sqlText.autoFormat",                false));
		_showplanXmlGuiAuto_chk .setSelected(            conf.getBooleanProperty(PROPKEY_BASE + "showplan.autoXmlPlan",              false));

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
		int width  = conf.getLayoutProperty(PROPKEY_BASE + "dialog.window.width",  SwingUtils.hiDpiScale(1024));
		int height = conf.getLayoutProperty(PROPKEY_BASE + "dialog.window.height", SwingUtils.hiDpiScale(768));
		int x      = conf.getLayoutProperty(PROPKEY_BASE + "dialog.window.pos.x",  -1);
		int y      = conf.getLayoutProperty(PROPKEY_BASE + "dialog.window.pos.y",  -1);
		if (width != -1 && height != -1)
		{
			this.setSize(width, height);
		}
		if (x != -1 && y != -1)
		{
			if ( ! SwingUtils.isOutOfScreen(x, y, width, height) )
				this.setLocation(x, y);
		}

		int divLoc;
		divLoc = conf.getLayoutProperty(PROPKEY_BASE + "dialog.splitpane.topBot.pos",  -1);
		if (divLoc > 0)
			_topBot_split.setDividerLocation(divLoc);

		divLoc = conf.getLayoutProperty(PROPKEY_BASE + "dialog.splitpane.sqlPlan.pos", -1);
		if (divLoc > 0)
			_sqlPlan_split.setDividerLocation(divLoc);
	
		divLoc = conf.getLayoutProperty(PROPKEY_BASE + "dialog.splitpane.udq.pos", -1);
		if (divLoc > 0)
			_udq_splitpane.setDividerLocation(divLoc);
	}
	/*---------------------------------------------------
	** END: implementing saveProps & loadProps
	**---------------------------------------------------
	*/

	private static class TimestampPickList
	{
		private List<Timestamp> _list;
		private Window          _owner;
		private String          _label;

		public TimestampPickList(Window owner, List<Timestamp> list, String label)
		{
			_owner = owner;
			_list  = list;
			_label = label;
		}

		AbstractTableModel tm = new AbstractTableModel()
		{
			private static final long serialVersionUID = 1L;

			@Override public int    getRowCount()          { return _list.size(); }
			@Override public int    getColumnCount()       { return 2; }
			@Override public String getColumnName(int col) { return col==0 ? "Date" : "Time"; }
			@Override public Object getValueAt(int row, int col) 
			{ 
				String ts = "" + _list.get(row);
				return col==0 ? ts.substring(0, 10) : ts.substring(11);
			}
		};
		
		public Timestamp getTimestamp()
		{
			SqlPickList pl = new SqlPickList(_owner, tm, _label, true);
			pl.setVisible(true);
			if (pl.wasOkPressed())
			{
				Timestamp ts = _list.get(pl.getSelectedModelRow());
				return ts;
			}
			else
			{
				return null;
			}
		}
	}
	//------------------------------------------------------------
	//------------------------------------------------------------
	//------------------------------------------------------------
	// MAIN CODE
	//------------------------------------------------------------
	//------------------------------------------------------------
	//------------------------------------------------------------
	public static void main(String[] args)
	{
		System.out.println("Only used for dummy testing during development");

		Properties log4jProps = new Properties();
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		//log4jProps.setProperty("log4j.rootLogger", "DEBUG, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		Configuration conf = new Configuration(System.getProperty("user.home") + File.separator + "SqlCaptureOfflineView.save.properties");
		Configuration.setInstance(Configuration.USER_TEMP, conf);
		
		Configuration.setSearchOrder(Configuration.USER_TEMP);

		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			_logger.info("Using Look And Feel named '"+UIManager.getLookAndFeel().getName()+"', classname='"+UIManager.getLookAndFeel().getClass().getName()+"', toString='"+UIManager.getLookAndFeel()+"'.");
		}
		catch (Exception e)
		{
			_logger.warn("Problem setting the Look And Feel to 'getSystemLookAndFeelClassName()'.", e);
		}

		
		SqlCaptureOfflineView view = new SqlCaptureOfflineView(null);
		view.setVisible(true);
	}
}
