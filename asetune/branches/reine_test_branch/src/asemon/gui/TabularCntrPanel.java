/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.Timestamp;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTableHeader;
import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.Filter;
import org.jdesktop.swingx.decorator.FilterPipeline;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;

import asemon.CountersModel;
import asemon.CountersModelAppend;
import asemon.MonTablesDictionary;
import asemon.MonWaitEventIdDictionary;
import asemon.gui.swing.AbstractComponentDecorator;
import asemon.gui.swing.FilterDiffCounterIsZero;
import asemon.gui.swing.FilterValueAndOp;
import asemon.gui.swing.GTabbedPane;
import asemon.gui.swing.GTabbedPaneWindowProps;
import asemon.utils.AseConnectionFactory;
import asemon.utils.Configuration;
import asemon.utils.ConnectionFactory;
import asemon.utils.SwingUtils;
import asemon.xmenu.TablePopupFactory;

public class TabularCntrPanel
extends JPanel
implements GTabbedPane.DockUndockManagement, ConnectionFactory, TableModelListener, ClipboardOwner
{
	private static Logger _logger = Logger.getLogger(TabularCntrPanel.class);

	private static final long serialVersionUID = 1L;

	private boolean _migDebug = false;

	private String _name = "";
	private String _description = "Used for tool tip";
	private Icon   _icon = null;

	// Below is members for open the Panel into a Frame
	private JButton    _tabDockUndockButton = new JButton();

	CountersModel _cm = null;

	//-------------------------------------------------
	// FILTER Panel
	private static String FILTER_NO_COLUMN_IS_SELECTED = "<none>";
	private static final int FILTER_OP_EQ = 0;  // EQual
	private static final int FILTER_OP_NE = 1;  // Not Equal
	private static final int FILTER_OP_GT = 2;  // Greater Then
	private static final int FILTER_OP_LT = 3;  // Less Then
	private static final String[] FILTER_OP_STR_ARR = {"==, Equal", "!=, Not Equal", ">, Greater Than", "<, Less Than"};

	private JPanel     _filterPanel;
	private JLabel     _filterColumn_lbl         = new JLabel("Filter column");
	private JComboBox  _filterColumn_cb          = new JComboBox();
	private JLabel     _filterOperation_lbl      = new JLabel("Operation");
	private JComboBox  _filterOperation_cb       = new JComboBox();
	private JLabel     _filterValue_lbl          = new JLabel("Value");
	private JTextField _filterValue_tf           = new JTextField();
	private JCheckBox  _filterNoZeroCounters_chk = new JCheckBox("Do NOT show unchanged counter rows");

	private FilterValueAndOp        _tableValAndOpFilter      = null;
	private FilterDiffCounterIsZero _tableDiffCntIsZeroFilter = null;
	private FilterPipeline          _tablePipelineFilter      = null;
	private boolean                 _tableFilterIsSet         = false;

	// COUNTER Panel
	private JPanel       _counterPanel;
	private JRadioButton _counterAbs_rb   = new JRadioButton("Absolute values");
	private JRadioButton _counterDelta_rb = new JRadioButton("Delta values");
	private JRadioButton _counterRate_rb  = new JRadioButton("Rate per second");
	private JLabel       _counterPct1_lbl = new JLabel("Or PCT");
	private JLabel       _counterPct2_lbl = new JLabel("Or PCT");

	// TIME info panel
	private JPanel     _timeInfoPanel;
//	private String     _timeEmptyConstant = "YYYY-MM-DD hh:mm:ss.444";
	private String     _timeEmptyConstant = "                       ";
	private JLabel     _timeClear_lbl     = new JLabel("Clear time");
	private JTextField _timeClear_txt     = new JTextField(_timeEmptyConstant);
	private JLabel     _timeSample_lbl    = new JLabel("Sample time");
	private JTextField _timeSample_txt    = new JTextField(_timeEmptyConstant);
	private JLabel     _timeIntervall_lbl = new JLabel("Intervall (ms)");
	private JTextField _timeIntervall_txt = new JTextField();

	// OPTIONS panel
	private JPanel     _optionsPanel;
	private JCheckBox _optionPauseDataPolling_chk      = new JCheckBox("Pause data polling");
	private JCheckBox _optionEnableBgPooling_chk       = new JCheckBox("Enable background data polling");
	private JCheckBox _optionPersistCounters_chk       = new JCheckBox("Persist counters");
	private JCheckBox _optionNegativeDiffCntToZero_chk = new JCheckBox("Reset negative Delta and Rate counters to zero");

	
	// DATA TABLE panel
	private JXTable    _dataTable      = new TCPTable();

	private JPopupMenu _tablePopupMenu = null;

	private Watermark  _watermark      = null;
	
	//-------------------------------------------------

	
	
	
	/*---------------------------------------------------
	** BEGIN: constructors
	**---------------------------------------------------
	*/
	public TabularCntrPanel(String name)
	{
		_name = name;

		// Initialize various table filters
		_tableValAndOpFilter      = new FilterValueAndOp();
		_tableDiffCntIsZeroFilter = new FilterDiffCounterIsZero();
		_tablePipelineFilter      = new FilterPipeline( new Filter[] { _tableValAndOpFilter, _tableDiffCntIsZeroFilter } );

		initComponents();
	}
	/*---------------------------------------------------
	** END: constructors
	**---------------------------------------------------
	*/


	public void setCounterModel(CountersModel cm)
	{
		_cm = cm;

		if (_cm != null)
		{
			_filterNoZeroCounters_chk        .setSelected( _cm.isFilterAllZero() );
			_optionPauseDataPolling_chk      .setSelected( _cm.isDataPollingPaused() );
			_optionEnableBgPooling_chk       .setSelected( _cm.isBackgroundDataPollingEnabled() );
			_optionPersistCounters_chk       .setSelected( _cm.isPersistCountersEnabled() );
			_optionNegativeDiffCntToZero_chk .setSelected( _cm.isNegativeDiffCountersToZero() );
			
			int dataSource = _cm.getDataSource();
			if (dataSource == CountersModel.DATA_ABS)  _counterAbs_rb  .setSelected(true);
			if (dataSource == CountersModel.DATA_DIFF) _counterDelta_rb.setSelected(true);
			if (dataSource == CountersModel.DATA_RATE) _counterRate_rb .setSelected(true);

			_dataTable.setModel(_cm);
			_cm.addTableModelListener(this);
		}
	}
	// implemeting: TableModelListener
	public void tableChanged(TableModelEvent e)
	{
		TableModel tm = (TableModel) e.getSource();
//		int type      = e.getType();
		int column    = e.getColumn();
		int firstRow  = e.getFirstRow();
		int lastRow   = e.getLastRow();
//		System.out.println("=========TableModelEvent: type="+type+", column="+column+", firstRow="+firstRow+", lastRow="+lastRow);

		// event: AbstactTableModel.fireTableStructureChanged
		if (column == -1 && firstRow == -1 && lastRow == -1)
		{
			refreshFilterColumns(tm);
		}
		setWatermark();
	}
	public void refreshFilterColumns(TableModel tm)
	{
		_filterColumn_cb.removeAllItems();

		// first values should be FILTER_NO_COLUMN_IS_SELECTED
		_filterColumn_cb.addItem(FILTER_NO_COLUMN_IS_SELECTED); 

		if (tm != null)
		{
			for (int i=0; i<tm.getColumnCount(); i++)
			{
				_filterColumn_cb.addItem(tm.getColumnName(i)); 
			}
		}
	}

	public String getPanelName()
	{
		return _name;
	}

	public void setActiveGraph(boolean b)
  	{
		if (b)
		{
			_optionEnableBgPooling_chk.setSelected(true);
			_optionEnableBgPooling_chk.setEnabled(false);
		}
		else
		{
			_optionEnableBgPooling_chk.setEnabled(true);
		}
  	}

	public void setEnableCounterChoice(boolean b)
  	{
		_counterPanel.setEnabled(b);
		for (int i=0; i<_counterPanel.getComponentCount(); i++)
		{
			_counterPanel.getComponent(i).setEnabled(b);
		}
  	}
	public void setEnableFilter(boolean b)
	{
		_filterPanel.setEnabled(b);
		for (int i=0; i<_filterPanel.getComponentCount(); i++)
		{
			_filterPanel.getComponent(i).setEnabled(b);
		}
	}
//	public void setTableModel(TableModel tm)
//	{
//		_dataTable.setModel(tm);
//	}
	public void adjustTableColumnWidth()
	{
		_dataTable.packAll(); // set size so that all content in all cells are visible
	}
	public boolean isTableInitialized()
	{
		return _dataTable.getColumnCount(true) > 0;
	}


	// THIS SHOULD NOT BE HERE... GET RID OF IT
//	private void updateTM()
//	{
//		if (_cm == null)
//			return;
//
//		try
//		{
//			if ( ! Asemon.getCounterCollector().isRefreshing() )
//			{
//				_cm.updateTM();
//			}
//			setWatermark();
//		}
//		catch (Exception e)
//		{
//			_logger.error("Failed when refreshing the Table GUI.", e);
//		}
//	}


	public Icon getIcon()
	{
		return _icon;
	}
	public void setIcon(Icon icon)
	{
		_icon = icon;
	}

	public String getDescription()
	{
		return _description;
	}
	public CountersModel getCounterModel()
	{
		return _cm;
	}
	public void setTimeInfo(Timestamp clearTime, Timestamp sampleTime, long intervall)
	{
		String timeClear = (clearTime == null) ? _timeEmptyConstant : clearTime.toString();
		_timeClear_txt.setText(timeClear);

		String timeSample = (sampleTime == null) ? _timeEmptyConstant : sampleTime.toString();
		_timeSample_txt.setText(timeSample);

		String timeIntervall = (intervall == 0) ? "" : Long.toString(intervall);
		_timeIntervall_txt.setText(timeIntervall);
	}
	public void setTableToolTipText(String tip)
	{
		if (_dataTable != null)
			_dataTable.setToolTipText(tip);
	}
	public void reset()
	{
		setTimeInfo(null, null, 0);
		//_counterRate_rb.setSelected(true);
	}


	/*---------------------------------------------------
	** BEGIN: component initialization
	**---------------------------------------------------
	*/
	protected void initComponents() 
	{
//		MigLayout layout = new MigLayout(_migDebug?"debug, ":""+"wrap 4","[] [grow] []","[] [grow] [grow] [grow] []");
//		MigLayout layout = new MigLayout(_migDebug?"debug":""+"",
//				"",
//				"");
//		setLayout(layout);
		setLayout(new BorderLayout());

		JSplitPane _mainSplitPan = new JSplitPane();
		_mainSplitPan.setOrientation(JSplitPane.VERTICAL_SPLIT);
		_mainSplitPan.setBorder(null);
		_mainSplitPan.add(createExtendedInfoPanel(), JSplitPane.TOP);
		_mainSplitPan.add(createTablePanel(),        JSplitPane.BOTTOM);
		_mainSplitPan.setDividerSize(3);
//		add(createTopPanel(), "wrap");
//		add(_mainSplitPan,    "");
		add(createTopPanel(), BorderLayout.NORTH);
		add(_mainSplitPan,    BorderLayout.CENTER);

		_tablePopupMenu = createDataTablePopupMenu();
		_dataTable.setComponentPopupMenu(_tablePopupMenu);

		initComponentActions();
	}
	
	private JPanel createTopPanel() 
	{
		JPanel panel = SwingUtils.createPanel("Top", false);
		panel.setLayout(new MigLayout(_migDebug?"debug":""+"ins 3 10 3 3", //ins T L B R???
				"[] [] [] []",
				""));

		JLabel title = new JLabel(_name);
		title.setFont(new java.awt.Font("Dialog", 1, 16));

		_filterPanel   = createFilterPanel();
		_counterPanel  = createCounterTypePanel();
		_timeInfoPanel = createTimePanel();
		_optionsPanel  = createOptionsPanel();

// Title over filter panel
//		panel.add(title,          "span 1 1, split 2, top, flowy");
//		panel.add(_filterPanel,   "flowx, grow");
//		panel.add(_counterPanel,  "top, grow");
//		panel.add(_timeInfoPanel, "top, grow");
//		panel.add(_optionsPanel,  "top, grow");

// Title over counter panel
		panel.add(title,          "span 1 1, split 2, top, flowy");
		panel.add(_counterPanel,  "flowx, grow");
		panel.add(_filterPanel,   "top, grow");
		panel.add(_timeInfoPanel, "top, grow");
		panel.add(_optionsPanel,  "top, grow");
		
		panel.add(_tabDockUndockButton,      "top, right, push");

		return panel;
	}

	/*---------------------------------------------------
	** BEGIN: implementing ConnectionFactory
	**---------------------------------------------------
	*/
	public Connection getConnection(String connName)
	{
		try
		{
			return AseConnectionFactory.getConnection(null, connName);
		}
		catch (Exception e)  // SQLException, ClassNotFoundException
		{
			_logger.error("Problems getting a new Connection", e);
			return null;
		}
	}

	/*---------------------------------------------------
	** END: implementing 
	**---------------------------------------------------
	*/

	/*---------------------------------------------------
	** BEGIN: PopupMenu on the table
	**---------------------------------------------------
	*/
	/** Get the JMeny attached to the GTabbedPane */
	public JPopupMenu getDataTablePopupMenu()
	{
		return _tablePopupMenu;
	}

	public void lostOwnership(Clipboard clipboard, Transferable contents)
	{
		_logger.info(_name+": lostOwnership(Clipboard='"+clipboard+"', Transferable='"+contents+"').");
	}

	public void copyTable(int dest, boolean copySampleInfo, boolean copyTableHeaders, String columnSeparator)
	{
		if ( _dataTable == null || (_dataTable != null && _dataTable.getRowCount() == 0) )
		{
			_logger.debug("copyTable(): no rows in the data table, return.");
			return;
		}

		TableModel tm = _dataTable.getModel();

		String rowTerminator = "\n";
		String rowStart = "";
		StringBuilder sb = new StringBuilder();

		if (copySampleInfo)
		{
			rowStart = _cm.getServerName() + columnSeparator + _cm.getSampleTime() + columnSeparator + _cm.getSampleInterval() + columnSeparator;
		}

		if (copyTableHeaders)
		{
			sb.append("Name")           .append(columnSeparator).append(_name)                  .append(rowTerminator);
			sb.append(rowTerminator);
			sb.append("ServerName")     .append(columnSeparator).append(_cm.getServerName())    .append(rowTerminator);
			sb.append("SampleTime")     .append(columnSeparator).append(_cm.getSampleTime())    .append(rowTerminator);
			sb.append("SampleIntervall").append(columnSeparator).append(_cm.getSampleInterval()).append(rowTerminator);
			sb.append(rowTerminator);

			if (copySampleInfo)
			{
				sb.append("ServerName").append(columnSeparator)
				  .append("SampleTime").append(columnSeparator)
				  .append("SampleIntervall").append(columnSeparator);
			}
			for (int c=0; c<tm.getColumnCount(); c++)
			{
				sb.append(tm.getColumnName(c));
				if ( c < (tm.getColumnCount() - 1) )
					sb.append(columnSeparator);
				else
					sb.append(rowTerminator);
			}
		}

		int tableCnt = _dataTable.getRowCount();
		int colCnt   = _dataTable.getColumnCount();
		for (int r=0; r<tableCnt; r++)
		{
			int tmRow = _dataTable.convertRowIndexToModel(r);

			sb.append(rowStart);
			for (int c=0; c<colCnt; c++)
			{
				sb.append(tm.getValueAt(tmRow, c));
				if ( c < (colCnt - 1) )
					sb.append(columnSeparator);
				else
					sb.append(rowTerminator);
			}
		}
		
		String data = sb.toString();
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

		clipboard.setContents( new StringSelection(data), this);
	}

	public void createCopyPasteMenu(JPopupMenu popup)
	{
		JMenuItem menuItem = null;
		
		JMenu copy         = new JMenu("Copy table to clipboard");
		JMenu copySepTab   = new JMenu("Using tab separator");
		JMenu copySepComma = new JMenu("Using comma separator");

		popup.add(copy);
		copy.add(copySepTab);
		copy.add(copySepComma);

		copy.setActionCommand("alwaysEnabled");
		
		menuItem = new JMenuItem("with headers and prefix(ServerName, SampleTime, SampleIntervall)");
		menuItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				copyTable(1, true, true, "\t");
			}
		});
		copySepTab.add(menuItem);

		menuItem = new JMenuItem("with NO headers BUT prefix(ServerName, SampleTime, SampleIntervall)");
		menuItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				copyTable(1, false, true, "\t");
			}
		});
		copySepTab.add(menuItem);

		menuItem = new JMenuItem("with NO headers and NO prefix");
		menuItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				copyTable(1, false, false, "\t");
			}
		});
		copySepTab.add(menuItem);


		
		menuItem = new JMenuItem("with headers and prefix(ServerName, SampleTime, SampleIntervall)");
		menuItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				copyTable(1, true, true, "\t");
			}
		});
		copySepComma.add(menuItem);

		menuItem = new JMenuItem("with NO headers BUT prefix(ServerName, SampleTime, SampleIntervall)");
		menuItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				copyTable(1, false, true, "\t");
			}
		});
		copySepComma.add(menuItem);

		menuItem = new JMenuItem("with NO headers and NO prefix");
		menuItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				copyTable(1, false, false, "\t");
			}
		});
		copySepComma.add(menuItem);		
	}

	/** 
	 * Creates the JMenu on the Component, this can be overrided by a subclass.<p>
	 * If you want to add stuff to the menu, its better to use 
	 * getTabPopupMenu(), then add entries to the menu. This is much 
	 * better than subclass the GTabbedPane
	 */
	public JPopupMenu createDataTablePopupMenu()
	{
		_logger.debug("createDataTablePopupMenu(): called.");

		JPopupMenu popup = new JPopupMenu();

		createCopyPasteMenu(popup);
		
		TablePopupFactory.createMenu(popup, 
			TablePopupFactory.TABLE_PUPUP_MENU_PREFIX, 
			Configuration.getInstance(Configuration.CONF), 
			_dataTable, this);

		TablePopupFactory.createMenu(popup, 
			_name.replaceAll(" ", "") + "." + TablePopupFactory.TABLE_PUPUP_MENU_PREFIX, 
			Configuration.getInstance(Configuration.CONF), 
			_dataTable, this);
		
		if (popup.getComponentCount() == 0)
		{
			_logger.warn("No PopupMenu has been assigned for the data table in the panel '"+_name+"'.");
			return null;
		}
		else
			return popup;
	}

	/*---------------------------------------------------
	** END: PopupMenu on the table
	**---------------------------------------------------
	*/

	
	
	/*---------------------------------------------------
	** BEGIN: implementing: GTabbedPane.DockUndockManagement
	**---------------------------------------------------
	*/
	// This will be called when this object is added to a GTabbedPane
	// This 
	public JButton getDockUndockButton()
	{
		_logger.debug("getDockUndockButton() called.");
		return _tabDockUndockButton;
	}
	public boolean beforeDock()   { return true; }
	public boolean beforeUndock() { return true; }

	public void    afterDock()    {}
	public void    afterUndock()  {}

	public void saveWindowProps(GTabbedPaneWindowProps wp)
	{
		Configuration conf = Configuration.getInstance(Configuration.TEMP);
		if (conf == null)
			return;

		_logger.trace(_name+": saveWindowProps(wp): "+wp);

		String base = _name + ".";
		conf.setProperty(base + "window.active", wp.undocked);

		if (wp.width  > 0) conf.setProperty(base + "window.width",  wp.width);
		if (wp.height > 0) conf.setProperty(base + "window.height", wp.height);
		if (wp.posX   > 0) conf.setProperty(base + "window.pos.x",  wp.posX);
		if (wp.posY   > 0) conf.setProperty(base + "window.pos.y",  wp.posY);

		conf.save();
	}

	public GTabbedPaneWindowProps getWindowProps()
	{
		Configuration conf = Configuration.getInstance(Configuration.TEMP);
		if (conf == null)
			return null;
		
		GTabbedPaneWindowProps wp = new GTabbedPaneWindowProps();
		String base = _name + ".";
		wp.undocked = conf.getBooleanProperty(base + "window.active", false);
		wp.width  = conf.getIntProperty(base + "window.width",  -1);
		wp.height = conf.getIntProperty(base + "window.height", -1);
		wp.posX   = conf.getIntProperty(base + "window.pos.x",  -1);
		wp.posY   = conf.getIntProperty(base + "window.pos.y",  -1);

		_logger.trace(_name+": getWindowProps(): return "+wp);

		return wp;
	}

	/*---------------------------------------------------
	** END: implementing: GTabbedPane.DockUndockManagement
	**---------------------------------------------------
	*/

	private JPanel createFilterPanel() 
	{
		JPanel panel = SwingUtils.createPanel("Filter", true);
		panel.setLayout(new MigLayout(_migDebug?"debug, ":""+"ins 0", 
				"[] [grow]", 
				""));

		_filterColumn_cb.addItem(FILTER_NO_COLUMN_IS_SELECTED);

		_filterOperation_cb.addItem(FILTER_OP_STR_ARR[FILTER_OP_EQ]);
		_filterOperation_cb.addItem(FILTER_OP_STR_ARR[FILTER_OP_NE]);
		_filterOperation_cb.addItem(FILTER_OP_STR_ARR[FILTER_OP_GT]);
		_filterOperation_cb.addItem(FILTER_OP_STR_ARR[FILTER_OP_LT]);

		_filterColumn_cb         .setToolTipText("Column that you want to filter on.");
		_filterOperation_cb      .setToolTipText("Operation to use when filtering data.");
		_filterValue_tf          .setToolTipText("Value to filter on. If column is 'string' then regexp will be used for operator 'Equal' and 'Not Equal'.");
		_filterNoZeroCounters_chk.setToolTipText("Filter out rows where all 'diff/rate' counters are 0.");

		panel.add(_filterColumn_lbl,   "");
		panel.add(_filterColumn_cb,    "growx, wrap");
		
		panel.add(_filterOperation_lbl,"");
		panel.add(_filterOperation_cb, "growx, wrap");
		
		panel.add(_filterValue_lbl,    "");
		panel.add(_filterValue_tf,     "growx, wrap");
		
		panel.add(_filterNoZeroCounters_chk, "span, wrap");

		return panel;		
	}

	private JPanel createCounterTypePanel() 
	{
		JPanel panel = SwingUtils.createPanel("Show Counter Type", true);
		panel.setLayout(new MigLayout(_migDebug?"debug, ":""+"wrap 2, ins 0",
				"",
				"0[0]0"));

		_counterDelta_rb.setForeground(Color.BLUE);
		_counterRate_rb .setForeground(Color.BLUE);
		_counterPct1_lbl.setForeground(Color.RED);
		_counterPct2_lbl.setForeground(Color.RED);

		_counterAbs_rb  .setToolTipText("Absolute values of the counters.");
		_counterDelta_rb.setToolTipText("What is the difference since previous sample. Displayed with blue color.");
		_counterRate_rb .setToolTipText("Divide the difference between two samples with time elipsed since last sample, then we get diff or rate per second. Displayed with blue color");
		
		ButtonGroup	  group	= new ButtonGroup();
		group.add(_counterAbs_rb);
		group.add(_counterDelta_rb);
		group.add(_counterRate_rb);

		panel.add(_counterAbs_rb,     "wrap");

		panel.add(_counterDelta_rb,   "");
		panel.add(_counterPct1_lbl,   "wrap");

		panel.add(_counterRate_rb,    "");
		panel.add(_counterPct2_lbl,   "Wrap");

		return panel;
	}
	
	private JPanel createTimePanel() 
	{
		JPanel panel = SwingUtils.createPanel("Time Information", true);
		panel.setLayout(new MigLayout(_migDebug?"debug, ":""+"wrap 2, ins 0", 
				"[] [grow]", 
				""));
		
		_timeClear_txt    .setEditable(false);
		_timeSample_txt   .setEditable(false);
		_timeIntervall_txt.setEditable(false);

		_timeClear_txt    .setToolTipText("If sp_sysmon is executed and clears the counters, it could be nice to know that...");
		_timeSample_txt   .setToolTipText("Date when the data showned in the table was sampled.");
		_timeIntervall_txt.setToolTipText("Milliseconds since last sample period.");

		panel.add(_timeClear_lbl,     "");
		panel.add(_timeClear_txt,     "growx, wrap");
		
		panel.add(_timeSample_lbl,    "");
		panel.add(_timeSample_txt,    "growx, wrap");
		
		panel.add(_timeIntervall_lbl, "");
		panel.add(_timeIntervall_txt, "growx, wrap");
		
		
		return panel;
	}

	private JPanel createOptionsPanel() 
	{
		JPanel panel = SwingUtils.createPanel("Options", true);
		panel.setLayout(new MigLayout(_migDebug?"debug, ":""+"ins 0, gap 0", 
				"", 
				"0[0]0"));
		
		_optionPauseDataPolling_chk     .setToolTipText("Pause data polling. This makes the values easier to read...");
		_optionEnableBgPooling_chk      .setToolTipText("Samplpe this panel even when this Tab is not active.");
		_optionPersistCounters_chk      .setToolTipText("Save this counter set to a Persistant storage, even when we are in GUI mode.");
		_optionNegativeDiffCntToZero_chk.setToolTipText("<html>If the differance between 'this' and 'previous' data sample has negative counter values, reset them to be <b>zero</b>" +
				                                        "<p>This is good for most data tables, meaning if sp_sysmon resets the counters or counters wrap..." +
				                                        "<p>It's not good for data tables where we want to watch counters that grows and shrinks, for example \"procedure cache memory usage\".</html>");

		panel.add(_optionPauseDataPolling_chk,      "wrap");
		panel.add(_optionEnableBgPooling_chk,       "wrap");
//		panel.add(_optionPersistCounters_chk,       "wrap");
		panel.add(_optionNegativeDiffCntToZero_chk, "wrap");
	
		return panel;
	}
	
	private JPanel createExtendedInfoPanel() 
	{
		JPanel panel = SwingUtils.createPanel("Extended Information", false);
//		panel.setLayout(new MigLayout(true?"debug, ":""+"wrap 2",
//				"[grow] []",
//				"[grow,:100:]"));
		
//		panel.add(new JTextArea("DUMMY text fileld...Extended Information......................"));
		panel.setLayout(new BorderLayout());
		panel.add(new JScrollPane(createTreeSpWho()), BorderLayout.CENTER);
		
		panel.setPreferredSize(new Dimension(0,0));
		panel.setMinimumSize(new Dimension(0,0));
		return panel;
	}
	private JTree createTreeSpWho()
	{
		DefaultMutableTreeNode top = new DefaultMutableTreeNode("sp_sysmon");
		DefaultMutableTreeNode heading = new DefaultMutableTreeNode("");
		DefaultMutableTreeNode subHead = new DefaultMutableTreeNode("");
		
		heading = new DefaultMutableTreeNode("Kernel Utilization");
		top.add(heading);

		heading = new DefaultMutableTreeNode("Worker Process Management");
		top.add(heading);

		heading = new DefaultMutableTreeNode("Parallel Query Management");
		top.add(heading);

		heading = new DefaultMutableTreeNode("Task Management");
		top.add(heading);


			subHead = new DefaultMutableTreeNode("Task Context Switches Due To");
			heading.add(subHead);

				subHead.add( new DefaultMutableTreeNode("Voluntary Yields"));
				subHead.add( new DefaultMutableTreeNode("Cache Search Misses"));
				subHead.add( new DefaultMutableTreeNode("Exceeding I/O batch size"));
				subHead.add( new DefaultMutableTreeNode("System Disk Writes"));
				subHead.add( new DefaultMutableTreeNode("Logical Lock Contention"));
				subHead.add( new DefaultMutableTreeNode("Address Lock Contention"));
				subHead.add( new DefaultMutableTreeNode("Latch Contention"));
				subHead.add( new DefaultMutableTreeNode("Log Semaphore Contention"));
				subHead.add( new DefaultMutableTreeNode("PLC Lock Contention"));
				subHead.add( new DefaultMutableTreeNode("Group Commit Sleeps"));
				subHead.add( new DefaultMutableTreeNode("Last Log Page Writes"));
				subHead.add( new DefaultMutableTreeNode("Modify Conflicts"));
				subHead.add( new DefaultMutableTreeNode("I/O Device Contention"));
				subHead.add( new DefaultMutableTreeNode("Network Packet Received"));
				subHead.add( new DefaultMutableTreeNode("Network Packet Sent"));
				subHead.add( new DefaultMutableTreeNode("Network services"));
				subHead.add( new DefaultMutableTreeNode("Other Causes"));

		heading = new DefaultMutableTreeNode("Application Management");
		top.add(heading);

		heading = new DefaultMutableTreeNode("ESP Management");
		top.add(heading);

		heading = new DefaultMutableTreeNode("Transaction Profile");
		top.add(heading);

		heading = new DefaultMutableTreeNode("Transaction Management");
		top.add(heading);

		heading = new DefaultMutableTreeNode("Index Management");
		top.add(heading);

		heading = new DefaultMutableTreeNode("Metadata Cache Management");
		top.add(heading);

		heading = new DefaultMutableTreeNode("Lock Management");
		top.add(heading);

		heading = new DefaultMutableTreeNode("Data Cache Management");
		top.add(heading);

		heading = new DefaultMutableTreeNode("Procedure Cache Management");
		top.add(heading);

		heading = new DefaultMutableTreeNode("Memory Management");
		top.add(heading);

		heading = new DefaultMutableTreeNode("Recovery Management");
		top.add(heading);

		heading = new DefaultMutableTreeNode("Disk I/O Management");
		top.add(heading);

		heading = new DefaultMutableTreeNode("Network I/O Management");
		top.add(heading);

		heading = new DefaultMutableTreeNode("Replication Agent");
		top.add(heading);

		return new JTree(new DefaultTreeModel(top));
	}
	
	private JPanel createTablePanel() 
	{
		JPanel panel = SwingUtils.createPanel("Actual Data Table", false);
//		panel.setLayout(new MigLayout(true?"debug, ":""+"wrap 2",
//				"[grow] []",
//				"[grow,:100:]"));
		panel.setLayout( new BorderLayout() );
//		String[] columns = {
//				"Col1 - asfkjha lkajhf alkjfhd askflakhf", 
//				"Col2 - kljhafaskjgfa kjfhkas fkajhs fgkaj faksjfdhg", 
//				"Col3 - hgfjhagdfkdjhg kjahgs fkjahg fkjagh kjgf kajgh fdkasj gf", 
//				"Col4 - alskjdfhah gfdakj gfkjahg fkjasgh kdfjaghd f"};
//		Object[][] rows = {
//				{"r1-Col-1", "r1-Col-2", "r1-Col-3", "r1-Col-4"}, 
//				{"r2-Col-1", "r2-Col-2", "r2-Col-3", "r2-Col-4"}, 
//				{"r3-Col-1", "r3-Col-2", "r3-Col-3", "r3-Col-4"}, 
//				{"r4-Col-1", "r4-Col-2", "r4-Col-3", "r4-Col-4"}, 
//				};
		
//		ResultSetTableModel tm = null;
//		try
//		{
//			Connection conn = AseConnectionFactory.getConnection("goransxp", 5000, null, "sa", "", "AseMon-TCP-Test");
//
//			Statement stmnt = conn.createStatement();
//			ResultSet rs    = null;
//			if (     _name.startsWith("0-"))
//				rs = stmnt.executeQuery("select * from master..monState");
//			else if (_name.startsWith("1-"))
//				rs = stmnt.executeQuery("select * from master..monOpenObjectActivity");
//			else if (_name.startsWith("2-"))
//				rs = stmnt.executeQuery("select * from master..monProcess");
//			else if (_name.startsWith("3-"))
//				rs = stmnt.executeQuery("select * from master..sysdatabases");
//			else if (_name.startsWith("4-"))
//				rs = stmnt.executeQuery("select * from master..monLocks");
//			else if (_name.startsWith("5-"))
//				rs = stmnt.executeQuery("select * from master..monEngine");
//			else if (_name.startsWith("6-"))
//				rs = stmnt.executeQuery("select version=@@version");
//			else if (_name.startsWith("7-"))
//				rs = stmnt.executeQuery("select * from master..monTableColumns");
//			else if (_name.startsWith("8-"))
//				rs = stmnt.executeQuery("select * from monDeviceIO");
//			else
//				rs = stmnt.executeQuery("select * from master..syslogins");
//				
//			tm = new ResultSetTableModel(rs);
//			
//			// first values should be FILTER_NO_COLUMN_IS_SELECTED
//			_filterColumn_cb.addItem(FILTER_NO_COLUMN_IS_SELECTED); 
//			for (int i=0; i<tm.getColumnCount(); i++)
//			{
//				_filterColumn_cb.addItem(tm.getColumnName(i)); 
//			}
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//		}

		_dataTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		_dataTable.packAll(); // set size so that all content in all cells are visible
		_dataTable.setSortable(true);
		_dataTable.setColumnControlVisible(true);
		_dataTable.setHighlighters(_highliters); // a variant of cell render

		JScrollPane scroll = new JScrollPane(_dataTable);
		_watermark = new Watermark(scroll, "Not Connected...");

//		panel.add(scroll, BorderLayout.CENTER);
//		panel.add(scroll, "");
		panel.add(scroll);
		return panel;
	}

	
	

	
	/*---------------------------------------------------
	** BEGIN: Action Listeners
	**---------------------------------------------------
	*/
	private void initComponentActions()
	{
		//---- FILTER PANEL -----
		_filterColumn_cb.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				filterAction(e);
			}
		});
		_filterOperation_cb.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				filterAction(e);
			}
		});
		_filterValue_tf.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				filterAction(e);
			}
		});

		
		//---- COUNTER TYPE PANEL -----
		_counterAbs_rb.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (_cm != null)
					_cm.setDataSource( CountersModel.DATA_ABS );
			}
		});

		_counterDelta_rb.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (_cm != null)
					_cm.setDataSource( CountersModel.DATA_DIFF );
			}
		});

		_counterRate_rb .addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (_cm != null)
					_cm.setDataSource( CountersModel.DATA_RATE );
			}
		});


		
		
		//---- OPTIONS PANEL -----
		_filterNoZeroCounters_chk.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (_filterNoZeroCounters_chk.isSelected())
				{
					if (_cm != null)
					{
						if ( ! _tableFilterIsSet )
						{
							_tableFilterIsSet = true;
							_logger.debug("No table filter was priviously set, so lets set it now.");
							_dataTable.setFilters(_tablePipelineFilter);
						}
						_tableDiffCntIsZeroFilter.setFilter( _dataTable, _cm.getDiffColumns(), _cm.getDiffDissColumns() );
					}
				}
				else
				{
					_tableDiffCntIsZeroFilter.resetFilter();
				}
			}
		});

		_optionPauseDataPolling_chk.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (_cm != null)
					_cm.setPauseDataPolling( _optionPauseDataPolling_chk.isSelected() );
			}
		});
		
		_optionEnableBgPooling_chk.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (_cm != null)
					_cm.setBackgroundDataPollingEnabled( _optionEnableBgPooling_chk.isSelected() );
			}
		});
		
		_optionPersistCounters_chk.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (_cm != null)
					_cm.setPersistCounters( _optionPersistCounters_chk.isSelected() );
			}
		});

		_optionNegativeDiffCntToZero_chk.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (_cm != null)
					_cm.setNegativeDiffCountersToZero( _optionNegativeDiffCntToZero_chk.isSelected() );
			}
		});
	}
	
	private void filterAction(ActionEvent e)
	{
		int    opIndex = _filterOperation_cb.getSelectedIndex();
		String column  = (String) _filterColumn_cb.getSelectedItem();
		String opStr   = (String) _filterOperation_cb.getSelectedItem();
		String text    = (String) _filterValue_tf.getText();

		_logger.debug("FILTER: col='"+column+"', op='"+opStr+"', opIndex="+opIndex+", text='"+text+"', clazz='--'.");

		if (_tableValAndOpFilter == null || column == null || text == null)
			return;

		if (column.equals(FILTER_NO_COLUMN_IS_SELECTED) || text.trim().equals(""))
		{
			_tableValAndOpFilter.resetFilter();
		}
		else
		{
			int col = _dataTable.getColumn(column).getModelIndex();
			if ( ! _tableFilterIsSet )
			{
				_tableFilterIsSet = true;
				_logger.debug("No table filter was priviously set, so lets set it now.");
				_dataTable.setFilters(_tablePipelineFilter);
			}
			_tableValAndOpFilter.setFilter(opIndex, col, text);
		}
	}
	/*---------------------------------------------------
	** END: Action Listeners
	**---------------------------------------------------
	*/




	/*---------------------------------------------------
	**---------------------------------------------------
	**---------------------------------------------------
	**---- SUBCLASSES ---- SUBCLASES ---- SUBCLASES ----- 
	**---------------------------------------------------
	**---------------------------------------------------
	**---------------------------------------------------
	*/
	private class TCPTable
	extends JXTable
	{
		private static final long serialVersionUID = 8891472887299452415L;

		private TCPTable()
		{
		}
		private TCPTable(TableModel tm)
		{
			super(tm);
		}

		/* Called on fire* has been called on the TableModel */
		public void tableChanged(TableModelEvent e)
		{
			int viewSelectedRow = getSelectedRow();
			int modelRowBefore  = -1;
			if (viewSelectedRow >= 0)
				modelRowBefore = convertRowIndexToModel(getSelectedRow());
			
			super.tableChanged(e);

			// it looks like either JTable or JXTable looses the selected row
			// after "fireTableDataChanged" has been called...
			// So try to set it back to where it previously was!
			if (modelRowBefore >= 0)
			{
				int viewRowNow = convertRowIndexToView(modelRowBefore);
				if (viewRowNow >= 0)
					getSelectionModel().setSelectionInterval(viewRowNow, viewRowNow);
			}
		}

//		public TableCellRenderer getCellRenderer(int row, int column)
//		{
//			return _tableDiffDataCellRenderer;
//			TableCellRenderer renderer = super.getCellRenderer(row, column);
//			if (_cm != null )
//			{
//				if (_cm.showAbsolute())
//					return renderer;
//
//				if (_cm.isDeltaCalculatedColumn(column))
//				{
//					return _tableDiffDataCellRenderer;
//				}
//			}
//			return renderer;
//		}

		// 
		// Implement table header tool tips.
		//
		protected JTableHeader createDefaultTableHeader() 
		{
			return new JXTableHeader(getColumnModel()) 
			{
                private static final long serialVersionUID = -4987530843165661043L;

				public String getToolTipText(MouseEvent e) 
				{
					// Now get the column name, which we point at
					Point p = e.getPoint();
					int index = getColumnModel().getColumnIndexAtX(p.x);
					if (index < 0) return null;
					Object colNameObj = getColumnModel().getColumn(index).getHeaderValue();

					// Now get the ToolTip from the CounterTableModel
					String toolTip = null;
					if (colNameObj instanceof String)
					{
						String colName = (String) colNameObj;
						if (_cm != null)
							toolTip = MonTablesDictionary.getInstance().getDescription(_cm.getMonTablesInQuery(), colName);
					}
					return toolTip;
				}
			};
		}

		// 
		// TOOL TIP for cells
		// - WaitEventID
		//
		public String getToolTipText(MouseEvent e) 
		{
			String tip = null;
			Point p = e.getPoint();
			int row = rowAtPoint(p);
			int col = columnAtPoint(p);
			if (row > 0 && col > 0)
			{
				col = super.convertColumnIndexToModel(col);
				row = super.convertRowIndexToModel(row);

				TableModel model = getModel();
				String colName = model.getColumnName(col);
				if ("WaitEventID".equals(colName))
				{
					Object cellVal = model.getValueAt(row, col);
					if (cellVal instanceof Number)
					{
						int waitEventId = ((Number)cellVal).intValue();
						if (waitEventId > 0)
							tip = MonWaitEventIdDictionary.getInstance().getToolTipText(waitEventId);
					}
				}
			}
			return tip;
		}

		
//		TableCellRenderer _tableDiffDataCellRenderer = new DefaultTableCellRenderer()
		TableCellRenderer _tableDiffDataCellRenderer = new DefaultTableRenderer()
		{
			private static final long serialVersionUID = -4439199147374261543L;

			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
			{
				Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
//				if (value == null || _cm == null) 
//					return comp;
//				if (value == null) 
//					return comp;

//				((JLabel)comp).setHorizontalAlignment(RIGHT);
//				if ( _cm.isPctColumn(column) )
//				{
//					comp.setForeground(Color.red);
//				}
//				else
//				{
//					comp.setForeground(Color.blue);
//					if ( value instanceof Number )
//					{
//						if ( ((Number)value).doubleValue() != 0.0 )
//						{
//							comp.setFont( comp.getFont().deriveFont(Font.BOLD) );
//						}
//					}
//				}
//				return comp;
				if ( value instanceof Number )
				{
					comp.setForeground(Color.blue);
//					((JLabel)comp).setHorizontalAlignment(RIGHT);
					if ( ((Number)value).doubleValue() != 0.0 )
					{
						comp.setFont( comp.getFont().deriveFont(Font.BOLD) );
					}
				}
				return comp;
			}
		};
	}


	/*---------------------------------------------------
	** BEGIN: Highlighter stuff for the JXTable
	**---------------------------------------------------
	*/
	private HighlightPredicate _highligtIfDelta = new HighlightPredicate() 
	{
		public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
		{
			if (_cm == null)              return false;
			if (!_cm.isDataInitialized()) return false;
			if (_cm.showAbsolute())       return false;
			return _cm.isDiffColumn(adapter.column);
//			Object value = adapter.getFilteredValueAt(adapter.row, adapter.column);
//			return (value instanceof Number);
		}
	};
	private HighlightPredicate _highligtIfPct = new HighlightPredicate() 
	{
		public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
		{
			if (_cm == null)              return false;
			if (!_cm.isDataInitialized()) return false;
			if (_cm.showAbsolute())       return false;
			return _cm.isPctColumn(adapter.column);
		}
	};
	private Highlighter[] _highliters = 
	{
			new HighlighterDiffData(_highligtIfDelta),
			new HighlighterPctData(_highligtIfPct)
	};


	private static class HighlighterDiffData 
	extends AbstractHighlighter 
	{ 
		public HighlighterDiffData(HighlightPredicate predicate) 
		{
			super(predicate);
		}
	 
		protected Component doHighlight(Component comp, ComponentAdapter adapter) 
		{
			Object value = adapter.getFilteredValueAt(adapter.row, adapter.column);
			if (value instanceof Number)
			{
				comp.setForeground(Color.BLUE);
				if (((Number) value).doubleValue() != 0)
				{
					comp.setFont( comp.getFont().deriveFont(Font.BOLD) );
				}
			}
			return comp;
		}
	}
	private static class HighlighterPctData 
	extends AbstractHighlighter 
	{ 
		public HighlighterPctData(HighlightPredicate predicate) 
		{
			super(predicate);
		}
	 
		protected Component doHighlight(Component comp, ComponentAdapter adapter) 
		{
			comp.setForeground(Color.RED);
			return comp;
		}
	}
	/*---------------------------------------------------
	** END: Highlighter stuff for the JXTable
	**---------------------------------------------------
	*/



	/*---------------------------------------------------
	** BEGIN: Watermark stuff
	**---------------------------------------------------
	*/
	public void setWatermarkText(String str)
	{
		_logger.debug(_name+".setWatermarkText('"+str+"')");
		_watermark.setWatermarkText(str);
	}
	public void setWatermark()
	{
		if ( ! _cm.isConnected() )
		{
			setWatermarkText("Not Connected...");
		}
		else if ( _cm.isDataPollingPaused() )
		{
			setWatermarkText("Paused...");
		}
		else if ( ! _cm.isActive() )
		{
			setWatermarkText(_cm.getProblemDesc());
		}
		else if ( ! _cm.hasAbsData() )
		{
			setWatermarkText("Waiting for first data sample...");
		}
		else if ( ! _cm.hasDiffData() && !(_cm instanceof CountersModelAppend) )
		{
			setWatermarkText("Waiting for second sample, before DIFF and RATE can be calculated...");
		}
		else if (_dataTable.getRowCount() == 0)
		{
			setWatermarkText("No rows in the table... Is filtering on?");
		}
		else
		{
			setWatermarkText(null);
		}
	}


	private class Watermark
    extends AbstractComponentDecorator
    {
		public Watermark(JComponent target, String text)
		{
			super(target);
			if (text != null)
				_text = text;
		}
		private String		_text	= "";
		private Graphics2D	g		= null;
		private Rectangle	r		= null;
	
		public void paint(Graphics graphics)
		{
			if (_text == null || _text != null && _text.equals(""))
				return;
	
			r = getDecorationBounds();
			g = (Graphics2D) graphics;
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Font f = g.getFont();
			g.setFont(f.deriveFont(Font.BOLD, f.getSize() * 2.0f));
			g.setColor(new Color(128, 128, 128, 128));
			//double theta = -Math.PI / 6;
			//        g.rotate(theta);
			//        int dx = (int)Math.abs(r.height * Math.sin(theta));
			//        g.translate(dx, r.height);
	
			FontMetrics fm = g.getFontMetrics();
			int strWidth = fm.stringWidth(_text);
			int xPos = (r.width - strWidth) / 2;
			int yPos = (int) (r.height - ((r.height - fm.getHeight()) / 2) * 1.3);
			g.drawString(_text, xPos, yPos);
		}
	
		public void setWatermarkText(String text)
		{
			_text = text;
			_logger.debug("setWatermarkText: to '" + _text + "'.");
			repaint();
		}
    }
	/*---------------------------------------------------
	** END: Watermark stuff
	**---------------------------------------------------
	*/
}
