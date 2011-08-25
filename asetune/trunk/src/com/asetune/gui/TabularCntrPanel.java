/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultRowSorter;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTableHeader;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.event.TableColumnModelExtListener;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.renderer.StringValue;
import org.jdesktop.swingx.renderer.StringValues;
import org.jdesktop.swingx.table.ColumnControlButton;
import org.jdesktop.swingx.table.TableColumnExt;
import org.jdesktop.swingx.table.TableColumnModelExt;

import com.asetune.GetCounters;
import com.asetune.Version;
import com.asetune.cm.CountersModel;
import com.asetune.gui.focusabletip.FocusableTip;
import com.asetune.gui.swing.AbstractComponentDecorator;
import com.asetune.gui.swing.GTabbedPane;
import com.asetune.gui.swing.GTabbedPaneWindowProps;
import com.asetune.gui.swing.MultiSortTableCellHeaderRenderer;
import com.asetune.gui.swing.RowFilterDiffCounterIsZero;
import com.asetune.gui.swing.RowFilterValueAndOp;
import com.asetune.pcs.InMemoryCounterHandler;
import com.asetune.pcs.PersistReader;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.Configuration;
import com.asetune.utils.ConnectionFactory;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;
import com.asetune.utils.TimeUtils;
import com.asetune.xmenu.TablePopupFactory;


public class TabularCntrPanel 
extends JPanel 
implements GTabbedPane.DockUndockManagement, GTabbedPane.ShowProperties, GTabbedPane.SpecialTabPainter, ConnectionFactory, TableModelListener, ClipboardOwner
{
	private static Logger			_logger								= Logger.getLogger(TabularCntrPanel.class);

	private static final long		serialVersionUID					= 1L;

	private boolean					_migDebug							= false;

	private String					_displayName						= "";
	private String					_description						= "Used for tool tip";
	private Icon					_icon								= null;
	private ImageIcon				_indicatorIcon						= null;
	// Decides if we should place the indicator to left or right on the icon.
	private boolean					_indicatorToLeft					= true;	

	// Below is members for open the Panel into a Frame
	private JButton					_tabDockUndockButton				= new JButton();

	private CountersModel			_cm									= null;
	private CountersModel			_cmDisplay							= null;
	private boolean					_tailMode							= true;

	/** Various types of how we do Automatic Column Width Adjustment in the JTable*/
	public enum AutoAdjustTableColumnWidth { GLOBAL, AUTO_ON, AUTO_OFF }

	// -------------------------------------------------
	// FILTER Panel
	private static String			FILTER_NO_COLUMN_IS_SELECTED		= "<none>";
	private static final int		FILTER_OP_EQ						= 0; // EQual
	private static final int		FILTER_OP_NE						= 1; // Not Equal
	private static final int		FILTER_OP_GT						= 2; // Greater Then
	private static final int		FILTER_OP_LT						= 3; // Less Then
	private static final String[]	FILTER_OP_STR_ARR					= { "==, Equal", "!=, Not Equal", ">, Greater Than", "<, Less Than" };
	private static final String[]	FILTER_OP_STR_ARR_SHORT				= { "EQ", "NE", "GT", "LT" };

	private JPanel					_filterPanel;
	private JLabel					_filterColumn_lbl					= new JLabel("Filter column");
	private JComboBox				_filterColumn_cb					= new JComboBox();
	private JLabel					_filterOperation_lbl				= new JLabel("Operation");
	private JComboBox				_filterOperation_cb					= new JComboBox();
	private JLabel					_filterValue_lbl					= new JLabel("Value");
	private JTextField				_filterValue_tf						= new JTextField();
	private JCheckBox				_filterNoZeroCounters_chk			= new JCheckBox("Do NOT show unchanged counter rows");

	private boolean					            _tableRowFilterIsSet         = false;
	private RowFilter<TableModel, Integer>      _tableRowFilter              = null;
	private List<RowFilter<TableModel,Integer>>	_tableRowFilterList	         = new ArrayList<RowFilter<TableModel,Integer>>();
	private RowFilterValueAndOp                 _tableRowFilterValAndOp      = null;
	private RowFilterDiffCounterIsZero          _tableRowFilterDiffCntIsZero = null;
	
	// COUNTER Panel
	private JPanel					_counterPanel;
	private JRadioButton			_counterAbs_rb						= new JRadioButton("Absolute values");
	private JRadioButton			_counterDelta_rb					= new JRadioButton("Delta values");
	private JRadioButton			_counterRate_rb						= new JRadioButton("Rate per second");
	private JLabel					_counterRows_lbl					= new JLabel("");
	private JLabel					_counterPct1_lbl					= new JLabel("Or PCT");
	private JLabel					_counterPct2_lbl					= new JLabel("Or PCT");

	// TIME info panel
	private JPanel					_timeInfoPanel;
	// private String _timeEmptyConstant = "YYYY-MM-DD hh:mm:ss.444";
	private String					_timeEmptyConstant					= "                       ";
	private JLabel					_timeClear_lbl						= new JLabel("Clear time");
	private JTextField				_timeClear_txt						= new JTextField(_timeEmptyConstant);
	private JLabel					_timeSample_lbl						= new JLabel("Sample time");
	private JTextField				_timeSample_txt						= new JTextField(_timeEmptyConstant);
	private JLabel					_timeIntervall_lbl					= new JLabel("Intervall (ms)");
	private JTextField				_timeIntervall_txt					= new JTextField();
	private JLabel					_timePostpone_lbl					= new JLabel("Postpone time");
	private JTextField				_timePostpone_txt					= new JTextField();
	// private JTextField _timePostpone_txt = new JFormattedTextField(new
	// DefaultFormatterFactory(new NumberFormatter()));
	private JLabel					_timeViewStored_lbl					= new JLabel("Viewing stored data");
	private JButton                 _timeOfflineRewind_but              = new JButton(); // offline_rewind.png
	private JButton                 _timeOfflineFastForward_but         = new JButton(); // offline_fastforward.png

	// OPTIONS panel
	private JPanel					_optionsPanel;
	private JCheckBox				_optionPauseDataPolling_chk			= new JCheckBox("Pause data polling");
	private JCheckBox				_optionEnableBgPolling_chk			= new JCheckBox("Enable background data polling");
	private JLabel					_optionHasActiveGraphs_lbl			= new JLabel("<html><b>has</b> active graphs</html>");
	private JButton					_optionTrendGraphs_but				= new JButton();
	private JCheckBox				_optionPersistCounters_chk			= new JCheckBox("Store Counter Data in a database");
	private JCheckBox				_optionPersistCountersAbs_chk		= new JCheckBox("Abs");
	private JCheckBox				_optionPersistCountersDiff_chk		= new JCheckBox("Diff");
	private JCheckBox				_optionPersistCountersRate_chk		= new JCheckBox("Rate");
	private JCheckBox				_optionNegativeDiffCntToZero_chk	= new JCheckBox("Reset negative Delta and Rate counters to zero");

	// LOCAL OPTIONS panel
	private JPanel					_localOptionsPanel;
	// This panel will be used for any checkboxes etc that is local to any specific tables.

	// DATA TABLE panel
	private TCPTable				_dataTable							= new TCPTable();

	private JPopupMenu				_tablePopupMenu						= null;

	private Watermark				_watermark							= null;

	// Refresh column with every minute
	private long					_lastColWithRefresh					= 0;
	private long					_colWithRefreshSec					= 60;

	// -------------------------------------------------

	/*---------------------------------------------------
	 ** BEGIN: constructors
	 **---------------------------------------------------
	 */
	public TabularCntrPanel(String displayName)
	{
		_displayName = displayName;

		// Initialize various table filters
		_tableRowFilterValAndOp      = new RowFilterValueAndOp(_dataTable);
		_tableRowFilterDiffCntIsZero = new RowFilterDiffCounterIsZero(_dataTable);

		_tableRowFilterList.add(_tableRowFilterValAndOp);
		_tableRowFilterList.add(_tableRowFilterDiffCntIsZero);
		_tableRowFilter = RowFilter.andFilter(_tableRowFilterList);
		// This has to be done later ??? maybe the Sorter IS NOT yet installed...
		//_dataTable.setRowFilter(_tableRowFilter); 

		initComponents();

		// check what look and feel we are using, then decide where the
		// indicator marker goes
		// only one I know that has the 'tab' icons to right of the text is
		// 'GTK'
		String lookAndFeelName = UIManager.getLookAndFeel().getName();
		_indicatorToLeft = true;
		if ( lookAndFeelName != null && lookAndFeelName.equals("GTK look and feel") )
			_indicatorToLeft = false;
	}

	/*---------------------------------------------------
	 ** END: constructors
	 **---------------------------------------------------
	 */

	public void setGraphTimeLineMarker(CountersModel cm)
	{
		if ( cm == null )
			return;
		_logger.trace("TabularCntrlPanel.setGraphTimeLineMarker(): name='" + _displayName + "'.");

		if ( _cm.hasTrendGraph() )
		{
			for (TrendGraph tg : _cm.getTrendGraphs().values())
			{
				tg.setTimeLineMarker(cm.getSampleTime());
			}
		}
	}

	/**
	 * @param cm
	 *            the Counter Data that should be view, null if not available
	 * @param tailMode
	 *            false if cm data is valid, true if we should display last
	 *            refreshed data
	 */
	public void setDisplayCm(CountersModel cm, boolean tailMode)
	{
		// new Exception().printStackTrace();
		_logger.trace("-------------- setDisplayCm(cm=" + (cm == null ? "null" : cm.getName()) + ", tailMode=" + tailMode + ")-----------");
		_cmDisplay = cm;
		_tailMode = tailMode;

		if ( _tailMode )
		{
			_timePostpone_lbl.setVisible(true);
			_timePostpone_txt.setVisible(true);
			_timeViewStored_lbl.setVisible(false);
			_timeOfflineRewind_but.setVisible(false);
			_timeOfflineFastForward_but.setVisible(false);

			_cmDisplay = null;

			// Restore old CM
			try { setCm(_cm); } 
			catch (Throwable e) 
			{
				_logger.info(getName()+" had some issues when doing setCm(_cm) back to the original cm. Caught and ignored: "+e);
			}

			loadFilterProps();

			// Reset markers in graph
			if ( _cm.hasTrendGraph() )
			{
				for (TrendGraph tg : _cm.getTrendGraphs().values())
				{
					tg.setTimeLineMarker(null);
				}
			}

			// if Summary has attached, graphs, go and set the time line marker
			// I know, wee should do this a bit further out (but I was lazy)
			CountersModel summaryCm = GetCounters.getCmByName(SummaryPanel.CM_NAME);
			if (summaryCm != null && summaryCm.hasTrendGraph() )
				for (TrendGraph tg : summaryCm.getTrendGraphs().values())
					tg.setTimeLineMarker(null);
		}
		else
		{
			_timePostpone_lbl.setVisible(false);
			_timePostpone_txt.setVisible(false);
			_timeViewStored_lbl.setVisible(true);
			if (MainFrame.isOfflineConnected())
			{
				_timeOfflineRewind_but.setVisible(true);
				_timeOfflineFastForward_but.setVisible(true);
			}

			if ( _cmDisplay != null )
			{
//System.out.println(Thread.currentThread().getName()+": _cmDisplay, rows="+_cmDisplay.getRowCount()+", name='"+_cmDisplay.getName()+"'.");
				// Set what data to show according to what is chosen in the GUI
				if ( _counterAbs_rb.isSelected() )
					_cmDisplay.setDataSource(CountersModel.DATA_ABS);
				if ( _counterDelta_rb.isSelected() )
					_cmDisplay.setDataSource(CountersModel.DATA_DIFF);
				if ( _counterRate_rb.isSelected() )
					_cmDisplay.setDataSource(CountersModel.DATA_RATE);

//if ("CMobjActivity".equals(getName()))
//_dataTable.printColumnLayout("BEFORE setModel(): ");
				_dataTable.setModel(_cmDisplay);
//if ("CMobjActivity".equals(getName()))
//_dataTable.printColumnLayout("after setModel(): ");
				loadFilterProps();
//if ("CMobjActivity".equals(getName()))
//_dataTable.printColumnLayout("after loadFilterProps(): ");

				if ( _cm.hasTrendGraph() )
				{
					for (TrendGraph tg : _cm.getTrendGraphs().values())
					{
						tg.setTimeLineMarker(_cmDisplay.getSampleTime());
					}
				}

				// if Summary has attached, graphs, go and set the time line marker
				// I know, wee should do this a bit further out (but I was lazy)
				CountersModel summaryCm = GetCounters.getCmByName(SummaryPanel.CM_NAME);
				if (summaryCm != null && summaryCm.hasTrendGraph() )
					for (TrendGraph tg : summaryCm.getTrendGraphs().values())
						tg.setTimeLineMarker(_cmDisplay.getSampleTime());
			}
			else
			{
				_dataTable.setModel(new DefaultTableModel());
			}

			adjustTableColumnWidth();

			if ( _cmDisplay != null )
				setTimeInfo(_cmDisplay.getCounterClearTime(), _cmDisplay.getSampleTime(), _cmDisplay.getSampleInterval());
			else
				setTimeInfo(null, null, 0);
		}
		setWatermark();
	}

	public CountersModel getDisplayCm()
	{
		return _cmDisplay;
	}

	public void setCm(CountersModel cm)
	{
		// Remove old stuff..
		if ( _cm != null )
			_cm.removeTableModelListener(this);

		_cm = cm;
		if ( _cm != null )
		{
			_dataTable.setName(_cm.getName());

			_filterNoZeroCounters_chk       .setSelected(_cm.isFilterAllZero());
			_optionPauseDataPolling_chk     .setSelected(_cm.isDataPollingPaused());
			_optionEnableBgPolling_chk      .setSelected(_cm.isBackgroundDataPollingEnabled());
			_optionHasActiveGraphs_lbl      .setVisible (_cm.hasActiveGraphs());
			_optionTrendGraphs_but          .setVisible (_cm.hasTrendGraph());
			_optionPersistCounters_chk      .setSelected(_cm.isPersistCountersEnabled());
			_optionPersistCountersAbs_chk   .setSelected(_cm.isPersistCountersAbsEnabled());
			_optionPersistCountersDiff_chk  .setSelected(_cm.isPersistCountersDiffEnabled());
			_optionPersistCountersRate_chk  .setSelected(_cm.isPersistCountersRateEnabled());
			_optionNegativeDiffCntToZero_chk.setSelected(_cm.isNegativeDiffCountersToZero());

			_optionPersistCountersAbs_chk   .setEnabled(_cm.isPersistCountersAbsEditable());
			_optionPersistCountersDiff_chk  .setEnabled(_cm.isPersistCountersDiffEditable());
			_optionPersistCountersRate_chk  .setEnabled(_cm.isPersistCountersRateEditable());

			int dataSource = _cm.getDataSource();
			if ( dataSource == CountersModel.DATA_ABS )  _counterAbs_rb  .setSelected(true);
			if ( dataSource == CountersModel.DATA_DIFF ) _counterDelta_rb.setSelected(true);
			if ( dataSource == CountersModel.DATA_RATE ) _counterRate_rb .setSelected(true);

			// Disable the what counters we can show
			if ( !_cm.isDiffCalcEnabled() )
			{
				setEnableCounterChoice(false);
				_counterAbs_rb.setSelected(true);
			}
			setPostponeTime(_cm.getPostponeTime());

			_dataTable.setModel(_cm);
			_cm.addTableModelListener(this);
			adjustTableColumnWidth();

			// remove the JXTable listener...
			// it will be called from tableChanged() if we are NOT looking
			// at at the history...
			_cm.removeTableModelListener(_dataTable);
		}
	}

	public CountersModel getCm()
	{
		return _cm;
	}

	public void setOptionPauseDataPolling(boolean b)
	{
		_optionPauseDataPolling_chk.setSelected(b);
	}

	public void setOptionEnableBgPolling(boolean b)
	{
		_optionEnableBgPolling_chk.setSelected(b);
	}

	public void setOptionPersistCountersAbs(boolean b)
	{
		_optionPersistCountersAbs_chk.setSelected(b);
	}

	public void setOptionPersistCountersDiff(boolean b)
	{
		_optionPersistCountersDiff_chk.setSelected(b);
	}

	public void setOptionPersistCountersRate(boolean b)
	{
		_optionPersistCountersRate_chk.setSelected(b);
	}

	public void setOptionNegativeDiffCntToZero(boolean b)
	{
		_optionNegativeDiffCntToZero_chk.setSelected(b);
	}

	public void setOptionPersistCounters(boolean b)
	{
		_optionPersistCounters_chk.setSelected(b);
		_optionPersistCountersAbs_chk .setEnabled(b && _cm.isPersistCountersAbsEditable());
		_optionPersistCountersDiff_chk.setEnabled(b && _cm.isPersistCountersDiffEditable());
		_optionPersistCountersRate_chk.setEnabled(b && _cm.isPersistCountersRateEditable());
	}

	/** gray out the JCheckboxes for this component */
	public void enableOptionPersistCounters(boolean b)
	{
		_optionPersistCounters_chk.setEnabled(b);
		_optionPersistCountersAbs_chk .setEnabled(b && _cm.isPersistCountersAbsEditable());
		_optionPersistCountersDiff_chk.setEnabled(b && _cm.isPersistCountersDiffEditable());
		_optionPersistCountersRate_chk.setEnabled(b && _cm.isPersistCountersRateEditable());
	}

	/**
	 * Called from cm.init(), which is called from GetCounter.initCounters()<br>
	 * This happens when we connect to a ASE
	 * <p>
	 * This should be used to initialize the GUI code for specifics...
	 */
	public void onCmInit()
	{
		// No need to continue if the CM is not known
		if (_cm == null)
			return;

//		if ( _cm.hasActiveGraphs() )
//		{
//			_cm.setBackgroundDataPollingEnabled( true, false );
//			setEnableBgPollingCheckbox( false );
//		}
//		else
//		{
//			_cm.setBackgroundDataPollingEnabled( false, false );
//			setEnableBgPollingCheckbox( true );
//		}
	}

	/** Get the CM name */
	@Override
	public String getName()
	{
		return _cm != null ? _cm.getName() : null;
	}

	// implementing: TableModelListener
	@Override
	public void tableChanged(TableModelEvent e)
	{
		TableModel tm = (TableModel) e.getSource();
//		int column = e.getColumn();
//		int firstRow = e.getFirstRow();
//		int lastRow = e.getLastRow();
//		int type = e.getType();

		// DEBUG
		//if (getName().equals("CMprocActivity"))
		//{
		//	System.out.println(Thread.currentThread().getName()+":=========TableModelEvent: cm="+StringUtil.left(getName(),20)+", type="+type+", column="+column+", firstRow="+firstRow+", lastRow="+lastRow);
		//	System.out.println("1111: tm.RowCount="+tm.getRowCount()+", :::: tab.RowCount="+_dataTable.getRowCount()+".");
		//}

		// Kick off the changes in the (super) JXTable
		// NOTE: call this FIRST in this method, otherwise rowCount() in the *view* will be faulty
		//       which means that "old" rows in the model that do not exists anymore will
		//       be accessed by the "sorter, highlighter & filers"
		if ( _tailMode )
		{
			//System.out.println("    : kicking off _dataTable.tableChanged(e);");
			_dataTable.tableChanged(e);
		}

		// DEBUG
		//if (getName().equals("CMprocActivity"))
		//{
		//	System.out.println("2222: tm.RowCount="+tm.getRowCount()+", :::: tab.RowCount="+_dataTable.getRowCount()+".");
		//}

		// event: AbstactTableModel.fireTableStructureChanged
		if ( SwingUtils.isStructureChanged(e) )
		{
			refreshFilterColumns(tm);

			// Use the TableModel here, because the _datatable(view) is not yet materialized...
			if (tm.getColumnCount() > 0)
				loadFilterProps();

			adjustTableColumnWidth();
		}

		setWatermark();
		// _cm.printTableModelListener();

		// If we are looking "at live data", 
		if ( _tailMode )
		{
			// every now and then, re-calculate the with of the columns in the
			// table. or if number of rows is less than 20
			if (   (System.currentTimeMillis() - _lastColWithRefresh) > _colWithRefreshSec * 1000 
			     || _dataTable.getRowCount() < 20
			   )
			{
				adjustTableColumnWidth();
			}
			// Reset the 'adjustTableColumnWidth()' timer, so that it will be executed
			// next time we have data in the table.
			if (_dataTable.getRowCount() == 0)
				_lastColWithRefresh = 0;

			// Update sample time info
			if ( _cm != null )
				setTimeInfo(_cm.getCounterClearTime(), _cm.getSampleTime(), _cm.getSampleInterval());

			// Kick off the changes in the JXTable
			// NOTE: this should be done at teh START of this method
			//_dataTable.tableChanged(e);
		}
	}

	public void refreshFilterColumns(TableModel tm)
	{
		_filterColumn_cb.removeAllItems();

		// first values should be FILTER_NO_COLUMN_IS_SELECTED
		_filterColumn_cb.addItem(FILTER_NO_COLUMN_IS_SELECTED);

		if ( tm != null )
		{
			for (int i = 0; i < tm.getColumnCount(); i++)
			{
				_filterColumn_cb.addItem(tm.getColumnName(i));
			}
		}
	}

	public String getPanelName()
	{
		return _displayName;
	}

	public void setEnableBgPollingCheckbox(boolean b)
	{
		_optionEnableBgPolling_chk.setEnabled(b);
	}

	public void setEnableCounterChoice(boolean b)
	{
		_counterPanel.setEnabled(b);
		for (int i = 0; i < _counterPanel.getComponentCount(); i++)
		{
			_counterPanel.getComponent(i).setEnabled(b);
		}
	}

	public void setEnableFilter(boolean b)
	{
		_filterPanel.setEnabled(b);
		for (int i = 0; i < _filterPanel.getComponentCount(); i++)
		{
			_filterPanel.getComponent(i).setEnabled(b);
		}
	}

	// public void setTableModel(TableModel tm)
	// {
	// _dataTable.setModel(tm);
	// }
	public void adjustTableColumnWidth()
	{
		// Get out of here if do not want to adjust
		if ( ! doAutoAdjustTableColumnWidth() )
			return;

		// Defer this work, a bit...
		// probably most viable if we call it from tableChanged()... 
		// this so all events has been processed by the Event Dispatcher before this is done
		// NOTE: this is a test, the packAll(), did not seem to do a correct work all the time, lets see if it's better now. 
		Runnable doWork = new Runnable()
		{
			public void run()
			{
				_lastColWithRefresh = System.currentTimeMillis();
				//SwingUtils.calcColumnWidths(_dataTable);
				_dataTable.packAll(); // set size so that all content in all cells are visible
			} // end: run method
		};
		SwingUtilities.invokeLater(doWork);
	}

	public boolean isTableInitialized()
	{
		return _dataTable.getColumnCount(true) > 0;
	}

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

	public void setTimeInfo(Timestamp clearTime, Timestamp sampleTime, long intervall)
	{
		String timeClear = (clearTime == null) ? _timeEmptyConstant : clearTime.toString();
		_timeClear_txt.setText(timeClear);

		String timeSample = (sampleTime == null) ? _timeEmptyConstant : sampleTime.toString();
		_timeSample_txt.setText(timeSample);

		String timeIntervall = (intervall == 0) ? "" : Long.toString(intervall);
		_timeIntervall_txt.setText(timeIntervall);
	}

	public void setPostponeTime(int postponeTime)
	{
		if ( postponeTime == 0 )
			_timePostpone_txt.setText("");
		else
			_timePostpone_txt.setText(Integer.toString(postponeTime));
	}

	public void setTableToolTipText(String tip)
	{
		if ( _dataTable != null )
			_dataTable.setToolTipText(tip);
	}

	public void reset()
	{
		setTimeInfo(null, null, 0);
//		setDisplayCm(null, _tailMode); // hmmm.. this did not "reset" the jtable etc...
	}

	public void putTableClientProperty(Object key, Object value)
	{
		if ( _dataTable != null )
			_dataTable.putClientProperty(key, value);
	}

	public Object getTableClientProperty(Object key)
	{
		if ( _dataTable != null )
			return _dataTable.getClientProperty(key);
		return null;
	}

	/*---------------------------------------------------
	 ** BEGIN: component initialization
	 **---------------------------------------------------
	 */
	protected void initComponents()
	{
		// MigLayout layout = new
		// MigLayout(_migDebug?"debug, ":""+"wrap 4","[] [grow] []","[] [grow] [grow] [grow] []");
		// MigLayout layout = new MigLayout(_migDebug?"debug":""+"",
		// "",
		// "");
		// setLayout(layout);
		setLayout(new BorderLayout());

		JSplitPane _mainSplitPan = new JSplitPane();
		_mainSplitPan.setOrientation(JSplitPane.VERTICAL_SPLIT);
		_mainSplitPan.setBorder(null);
		_mainSplitPan.add(createExtendedInfoPanel(), JSplitPane.TOP);
		_mainSplitPan.add(createTablePanel(), JSplitPane.BOTTOM);
		_mainSplitPan.setDividerSize(3);
		// add(createTopPanel(), "wrap");
		// add(_mainSplitPan, "");
		add(createTopPanel(), BorderLayout.NORTH);
		add(_mainSplitPan, BorderLayout.CENTER);

		_tablePopupMenu = createDataTablePopupMenu();
		_dataTable.setComponentPopupMenu(_tablePopupMenu);

		initComponentActions();
	}

	private JPanel createTopPanel()
	{
		JPanel panel = SwingUtils.createPanel("Top", false);
		panel.setLayout(new MigLayout(_migDebug ? "debug" : "" + "ins 3 10 3 3", // ins T L B R???
				"[] [] [] []", ""));

		JLabel title = new JLabel(_displayName);
		title.setFont(new java.awt.Font(Font.DIALOG, Font.BOLD, 16));

		_filterPanel       = createFilterPanel();
		_counterPanel      = createCounterTypePanel();
		_timeInfoPanel     = createTimePanel();
		_optionsPanel      = createOptionsPanel();
		_localOptionsPanel = createLocalOptionsPanel();

		// Title over filter panel
		// panel.add(title, "span 1 1, split 2, top, flowy");
		// panel.add(_filterPanel, "flowx, grow");
		// panel.add(_counterPanel, "top, grow");
		// panel.add(_timeInfoPanel, "top, grow");
		// panel.add(_optionsPanel, "top, grow");

		// Title over counter panel
		panel.add(title, "span 1 1, split 2, top, flowy");
		panel.add(_counterPanel, "flowx, grow");
		panel.add(_filterPanel, "top, grow");
		panel.add(_timeInfoPanel, "top, grow");
		panel.add(_optionsPanel, "top, grow");
		if (_localOptionsPanel != null)
			panel.add(_localOptionsPanel, "top, grow");
			

		panel.add(_tabDockUndockButton, "top, right, push");

		return panel;
	}

	/*---------------------------------------------------
	 ** BEGIN: implementing ConnectionFactory
	 **---------------------------------------------------
	 */
	@Override
	public Connection getConnection(String connName)
	{
		try
		{
			return AseConnectionFactory.getConnection(null, connName, null);
		}
		catch (Exception e) // SQLException, ClassNotFoundException
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
	 ** BEGIN: PopupMenu on the table
	 **---------------------------------------------------
	 */
	/** Get the JMeny attached to the GTabbedPane */
	public JPopupMenu getDataTablePopupMenu()
	{
		return _tablePopupMenu;
	}

	@Override
	public void lostOwnership(Clipboard clipboard, Transferable contents)
	{
		_logger.info(_displayName + ": lostOwnership(Clipboard='" + clipboard + "', Transferable='" + contents + "').");
	}

	public void copyCell(boolean stripHtml)
	{
		if ( _dataTable == null || (_dataTable != null && _dataTable.getRowCount() == 0) )
		{
			_logger.debug("copyCell(): no rows in the data table, return.");
			return;
		}

		String cellValueStr = null;
		if ( _dataTable.isLastMousePressedAtModelRowColValid() )
		{
			Object cellValueObj = _dataTable.getModel().getValueAt(_dataTable.getLastMousePressedAtModelRow(), _dataTable.getLastMousePressedAtModelCol());
			if ( cellValueObj != null )
				cellValueStr = cellValueObj.toString();
		}

		if ( cellValueStr != null )
		{
			String data = stripHtml ? StringUtil.stripHtml(cellValueStr) : cellValueStr;
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(new StringSelection(data), this);
		}
	}

	public void copyRow(String columnSeparator, boolean withColNames, boolean stripHtml)
	{
		if ( _dataTable == null || (_dataTable != null && _dataTable.getRowCount() == 0) )
		{
			_logger.debug("copyRow(): no rows in the data table, return.");
			return;
		}
		int row = _dataTable.getLastMousePressedAtModelRow();
		if ( row < 0 )
		{
			_logger.debug("copyRow(): You are not pointing at a row, return.");
			return;
		}
		String rowTerminator = "\n";

		StringBuilder sb = new StringBuilder();
		TableModel tm = _dataTable.getModel();
		int colCnt = _dataTable.getColumnCount();

		if ( withColNames )
		{
			for (int c = 0; c < colCnt; c++)
			{
				sb.append(tm.getColumnName(c));
				if ( c < (tm.getColumnCount() - 1) )
					sb.append(columnSeparator);
			}
			sb.append(rowTerminator);
		}

		for (int c = 0; c < colCnt; c++)
		{
			sb.append(tm.getValueAt(row, c));
			if ( c < (colCnt - 1) )
				sb.append(columnSeparator);
		}

		String data = stripHtml ? StringUtil.stripHtml(sb.toString()) : sb.toString();
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(new StringSelection(data), this);
	}

	public void copyTable(int formatType, boolean copySampleInfo, boolean copyTableHeaders, String columnSeparator)
	{
		if ( _dataTable == null || (_dataTable != null && _dataTable.getRowCount() == 0) )
		{
			_logger.debug("copyTable(): no rows in the data table, return.");
			return;
		}

		TableModel tm = _dataTable.getModel();

		CountersModel cm = _cm;
		if ( !_tailMode )
			cm = _cmDisplay;

		String rowTerminator = "\n";
		String rowStart = "";
		StringBuilder sb = new StringBuilder();

		if ( formatType == 1 )
		{
			if ( copySampleInfo )
			{
				rowStart = cm.getServerName() + columnSeparator + cm.getSampleTime() + columnSeparator + cm.getSampleInterval() + columnSeparator;
			}

			if ( copyTableHeaders )
			{
				sb.append("Collector Name").append(columnSeparator).append(_displayName).append(rowTerminator);
				sb.append(rowTerminator);
				sb.append("ServerName").append(columnSeparator).append(cm.getServerName()).append(rowTerminator);
				sb.append("SampleTime").append(columnSeparator).append(cm.getSampleTime()).append(rowTerminator);
				sb.append("SampleIntervall").append(columnSeparator).append(cm.getSampleInterval()).append(rowTerminator);
				sb.append(rowTerminator);

				if ( copySampleInfo )
				{
					sb.append("ServerName").append(columnSeparator).append("SampleTime").append(columnSeparator).append("SampleIntervall").append(columnSeparator);
				}
				for (int c = 0; c < tm.getColumnCount(); c++)
				{
					sb.append(tm.getColumnName(c));
					if ( c < (tm.getColumnCount() - 1) )
						sb.append(columnSeparator);
					else
						sb.append(rowTerminator);
				}
			}

			int tableCnt = _dataTable.getRowCount();
			int colCnt = _dataTable.getColumnCount();
			for (int r = 0; r < tableCnt; r++)
			{
				int tmRow = _dataTable.convertRowIndexToModel(r);

				sb.append(rowStart);
				for (int c = 0; c < colCnt; c++)
				{
					sb.append(tm.getValueAt(tmRow, c));
					if ( c < (colCnt - 1) )
						sb.append(columnSeparator);
					else
						sb.append(rowTerminator);
				}
			}
		}
		else
		{
			if ( copyTableHeaders )
			{
				sb.append("Collector Name").append(columnSeparator).append(_displayName).append(rowTerminator);
				sb.append(rowTerminator);
				sb.append("ServerName").append(columnSeparator).append(cm.getServerName()).append(rowTerminator);
				sb.append("SampleTime").append(columnSeparator).append(cm.getSampleTime()).append(rowTerminator);
				sb.append("SampleIntervall").append(columnSeparator).append(cm.getSampleInterval()).append(rowTerminator);
				sb.append(rowTerminator);
			}

			String[] extraColNames = null;
			String[] extraColData = null;
			if ( copySampleInfo )
			{
				extraColNames = new String[] { "ServerName", "SampleTime", "SampleIntervall" };
				extraColData = new String[] { cm.getServerName(), cm.getSampleTime() + "", cm.getSampleInterval() + "" };
			}
			sb.append(SwingUtils.tableToString(tm, true, extraColNames, extraColData));
		}

		String data = sb.toString();
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(new StringSelection(data), this);
	}

	public void createColumnControlMenu(JPopupMenu popup)
	{
		JMenuItem menuItem = null;

		// COLUMN CONTROL
		menuItem = new JMenuItem("Open 'Column Control' Menu");
		menuItem.setActionCommand(TablePopupFactory.ENABLE_MENU_ALWAYS);

		popup.add(menuItem);

		menuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				JComponent comp = _dataTable.getColumnControl();
				if (comp instanceof ColumnControlButton)
				{
					((ColumnControlButton)comp).doClick();
				}
			}
		});

		// RESTORE ORIGINAL COLUMN LAYOUT
		menuItem = new JMenuItem("Reset to Original Column Layout");
		menuItem.setActionCommand(TablePopupFactory.ENABLE_MENU_ALWAYS);

		popup.add(menuItem);

		menuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				_dataTable.setOriginalColumnLayout();
			}
		});

		// ADJUST COLUMN WIDTH
		menuItem = new JMenuItem("Adjust Column Width"); // Resizes all columns to fit their content
		menuItem.setActionCommand(TablePopupFactory.ENABLE_MENU_ALWAYS);

		popup.add(menuItem);

		menuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				_dataTable.packAll();
				// save is done by: addColumnModelListener:columnModelListener in the TCP table
			}
		});


		// Automatically Adjust Column Width
		JMenu autoAdjust = new JMenu("Automatically Adjust Column Width");
		popup.add(autoAdjust);
		autoAdjust.setActionCommand(TablePopupFactory.ENABLE_MENU_ALWAYS);

		JRadioButtonMenuItem useGlobal = new JRadioButtonMenuItem("Use Global settings (in Preferences)");
		JRadioButtonMenuItem autoOn    = new JRadioButtonMenuItem("Automatically Adjust Column Width");
		JRadioButtonMenuItem autoOff   = new JRadioButtonMenuItem("Use Saved Column Width");

		autoAdjust.add(useGlobal);
		autoAdjust.add(autoOn);
		autoAdjust.add(autoOff);

		ButtonGroup group = new ButtonGroup();
		group.add(useGlobal);
		group.add(autoOn);
		group.add(autoOff);

		// Get saved value... if not found: use "global"
		String key = getPanelName()+".autoAdjustTableColumnWidth";
		Configuration conf = Configuration.getCombinedConfiguration();
		String autoAdjustTableColumnWidth = conf.getProperty("", AutoAdjustTableColumnWidth.GLOBAL.toString());

		if      (autoAdjustTableColumnWidth.equals(AutoAdjustTableColumnWidth.GLOBAL  .toString())) useGlobal.setSelected(true);
		else if (autoAdjustTableColumnWidth.equals(AutoAdjustTableColumnWidth.AUTO_ON .toString())) autoOn   .setSelected(true);
		else if (autoAdjustTableColumnWidth.equals(AutoAdjustTableColumnWidth.AUTO_OFF.toString())) autoOff  .setSelected(true);
		else _logger.warn(getPanelName()+" Can't find appropriate value for "+key+" = '"+autoAdjustTableColumnWidth+"'.");

		useGlobal.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				setAutoAdjustTableColumnWidth(AutoAdjustTableColumnWidth.GLOBAL);
			}
		});
		autoOn.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				setAutoAdjustTableColumnWidth(AutoAdjustTableColumnWidth.AUTO_ON);
			}
		});
		autoOff.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				setAutoAdjustTableColumnWidth(AutoAdjustTableColumnWidth.AUTO_OFF);
			}
		});
		
		// Separator
		popup.addSeparator();
	}

	public void createCopyPasteMenu(JPopupMenu popup)
	{
		JMenuItem menuItem = null;

		// ----------------------------------------
		// COPY CELL
		// ----------------------------------------
		menuItem = new JMenuItem("Copy Cell to clipboard");
		menuItem.setActionCommand(TablePopupFactory.ENABLE_MENU_IF_ON_A_ROW);

		popup.add(menuItem);

		menuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				copyCell(true);
			}
		});

		// ----------------------------------------
		// COPY ROW
		// ----------------------------------------
		JMenu copyRow = new JMenu("Copy Row to clipboard");
		copyRow.setActionCommand(TablePopupFactory.ENABLE_MENU_IF_ON_A_ROW);

		popup.add(copyRow);

		menuItem = new JMenuItem("Tab separator (\\t), With Column Names");
		menuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				copyRow("\t", true, true);
			}
		});
		copyRow.add(menuItem);

		menuItem = new JMenuItem("Tab separator (\\t)");
		menuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				copyRow("\t", false, true);
			}
		});
		copyRow.add(menuItem);

		menuItem = new JMenuItem("Comma separator (,), With Column Names");
		menuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				copyRow(",", true, true);
			}
		});
		copyRow.add(menuItem);

		menuItem = new JMenuItem("Comma separator (,)");
		menuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				copyRow(",", false, true);
			}
		});
		copyRow.add(menuItem);

		// ----------------------------------------
		// COPY TABLE
		// ----------------------------------------
		JMenu copyTab = new JMenu("Copy Table to clipboard");

		JMenu copyFmtTab = new JMenu("Ascii Table Format");
		JMenu copySepTab = new JMenu("Tab separator (\\t)");
		JMenu copySepComma = new JMenu("Comma separator (,)");

		popup.add(copyTab);

		copyTab.add(copyFmtTab);
		copyTab.add(copySepTab);
		copyTab.add(copySepComma);

		copyTab.setActionCommand(TablePopupFactory.ENABLE_MENU_ALWAYS);

		menuItem = new JMenuItem("Header Info + Prefixed Data Rows with(ServerName, SampleTime, SampleIntervall)");
		menuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				copyTable(2, true, true, ": ");
			}
		});
		copyFmtTab.add(menuItem);

		menuItem = new JMenuItem("Header Info + Data Rows in the Table");
		menuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				copyTable(2, false, true, ": ");
			}
		});
		copyFmtTab.add(menuItem);

		menuItem = new JMenuItem("Only Data Rows in the Table");
		menuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				copyTable(2, false, false, ": ");
			}
		});
		copyFmtTab.add(menuItem);

		menuItem = new JMenuItem("Header Info + Prefixed Data Rows with(ServerName, SampleTime, SampleIntervall)");
		menuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				copyTable(1, true, true, "\t");
			}
		});
		copySepTab.add(menuItem);

		menuItem = new JMenuItem("Header Info + Data Rows in the Table");
		menuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				copyTable(1, false, true, "\t");
			}
		});
		copySepTab.add(menuItem);

		menuItem = new JMenuItem("Only Data Rows in the Table");
		menuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				copyTable(1, false, false, "\t");
			}
		});
		copySepTab.add(menuItem);

		menuItem = new JMenuItem("Header Info + Prefixed Data Rows with(ServerName, SampleTime, SampleIntervall)");
		menuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				copyTable(1, true, true, ",");
			}
		});
		copySepComma.add(menuItem);

		menuItem = new JMenuItem("Header Info + Data Rows in the Table");
		menuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				copyTable(1, false, true, ",");
			}
		});
		copySepComma.add(menuItem);

		menuItem = new JMenuItem("Only Data Rows in the Table");
		menuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				copyTable(1, false, false, ",");
			}
		});
		copySepComma.add(menuItem);
	}

	/**
	 * Creates the JMenu on the Component, this can be overrided by a subclass.
	 * <p>
	 * If you want to add stuff to the menu, its better to use
	 * getTabPopupMenu(), then add entries to the menu. This is much better than
	 * subclass the GTabbedPane
	 */
	public JPopupMenu createDataTablePopupMenu()
	{
		_logger.debug("createDataTablePopupMenu(): called.");

		JPopupMenu popup = new JPopupMenu();

		createColumnControlMenu(popup);

		createCopyPasteMenu(popup);

//		TablePopupFactory.createMenu(popup, TablePopupFactory.TABLE_PUPUP_MENU_PREFIX, Configuration.getInstance(Configuration.CONF), _dataTable, this);
//		TablePopupFactory.createMenu(popup, _displayName.replaceAll(" ", "") + "." + TablePopupFactory.TABLE_PUPUP_MENU_PREFIX, Configuration.getInstance(Configuration.CONF), _dataTable, this);
		TablePopupFactory.createMenu(popup, TablePopupFactory.TABLE_PUPUP_MENU_PREFIX, Configuration.getCombinedConfiguration(), _dataTable, this);
		TablePopupFactory.createMenu(popup, _displayName.replaceAll(" ", "") + "." + TablePopupFactory.TABLE_PUPUP_MENU_PREFIX, Configuration.getCombinedConfiguration(), _dataTable, this);

		if ( popup.getComponentCount() == 0 )
		{
			_logger.warn("No PopupMenu has been assigned for the data table in the panel '" + _displayName + "'.");
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
	 ** BEGIN: implementing: GTabbedPane.ShowProperties
	 **---------------------------------------------------
	 */
	@Override
	public void showProperties()
	{
		if ( !_cm.isRuntimeInitialized() )
		{
			SwingUtils.showInfoMessage(this, "Show Properties", "Not yet connected to any ASE Server, try later.");
			return;
		}

		new ShowPropertiesDialog(null);
	}

	/*---------------------------------------------------
	 ** BEGIN: implementing: GTabbedPane.ShowProperties
	 **---------------------------------------------------
	 */

	/*---------------------------------------------------
	 ** BEGIN: implementing: GTabbedPane.DockUndockManagement
	 **---------------------------------------------------
	 */
	// This will be called when this object is added to a GTabbedPane
	// This
	@Override
	public JButton getDockUndockButton()
	{
		_logger.debug("getDockUndockButton() called.");
		return _tabDockUndockButton;
	}

	@Override
	public boolean beforeDock()
	{
		return true;
	}

	@Override
	public boolean beforeUndock()
	{
		return true;
	}

	@Override
	public void afterDock()
	{
	}

	@Override
	public void afterUndock()
	{
	}

	@Override
	public void saveWindowProps(GTabbedPaneWindowProps wp)
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if ( conf == null )
			return;

		_logger.trace(_displayName + ": saveWindowProps(wp): " + wp);

		String base = _displayName + ".";
		conf.setProperty(base + "window.active", wp.undocked);

		if ( wp.width > 0 )
			conf.setProperty(base + "window.width", wp.width);
		if ( wp.height > 0 )
			conf.setProperty(base + "window.height", wp.height);
		if ( wp.posX > 0 )
			conf.setProperty(base + "window.pos.x", wp.posX);
		if ( wp.posY > 0 )
			conf.setProperty(base + "window.pos.y", wp.posY);

		conf.save();
	}

	@Override
	public GTabbedPaneWindowProps getWindowProps()
	{
//		Configuration conf = Configuration.getInstance(Configuration.TEMP);
		Configuration conf = Configuration.getCombinedConfiguration();
		if ( conf == null )
			return null;

		GTabbedPaneWindowProps wp = new GTabbedPaneWindowProps();
		String base = _displayName + ".";
		wp.undocked = conf.getBooleanProperty(base + "window.active", false);
		wp.width = conf.getIntProperty(base + "window.width", -1);
		wp.height = conf.getIntProperty(base + "window.height", -1);
		wp.posX = conf.getIntProperty(base + "window.pos.x", -1);
		wp.posY = conf.getIntProperty(base + "window.pos.y", -1);

		_logger.trace(_displayName + ": getWindowProps(): return " + wp);

		return wp;
	}

	/*---------------------------------------------------
	 ** END: implementing: GTabbedPane.DockUndockManagement
	 **---------------------------------------------------
	 */

	private JPanel createFilterPanel()
	{
		JPanel panel = SwingUtils.createPanel("Filter", true);
		panel.setLayout(new MigLayout(_migDebug ? "debug, " : "" + "ins 0", "[] [grow]", ""));

		_filterColumn_cb.addItem(FILTER_NO_COLUMN_IS_SELECTED);

		_filterOperation_cb.addItem(FILTER_OP_STR_ARR[FILTER_OP_EQ]);
		_filterOperation_cb.addItem(FILTER_OP_STR_ARR[FILTER_OP_NE]);
		_filterOperation_cb.addItem(FILTER_OP_STR_ARR[FILTER_OP_GT]);
		_filterOperation_cb.addItem(FILTER_OP_STR_ARR[FILTER_OP_LT]);

		// set auto completion
		AutoCompleteDecorator.decorate(_filterColumn_cb);
		AutoCompleteDecorator.decorate(_filterOperation_cb);
		
		_filterColumn_cb.setToolTipText("Column that you want to filter on.");
		_filterOperation_cb.setToolTipText("Operation to use when filtering data.");
		_filterValue_tf.setToolTipText("Value to filter on. If column is 'string' then regexp will be used for operator 'Equal' and 'Not Equal'.");
		_filterNoZeroCounters_chk.setToolTipText("Filter out rows where all 'diff/rate' counters are 0.");

		panel.add(_filterColumn_lbl, "");
		panel.add(_filterColumn_cb, "growx, wrap");

		panel.add(_filterOperation_lbl, "");
		panel.add(_filterOperation_cb, "growx, wrap");

		panel.add(_filterValue_lbl, "");
		panel.add(_filterValue_tf, "growx, wrap");

		panel.add(_filterNoZeroCounters_chk, "span, wrap");

		return panel;
	}

	private JPanel createCounterTypePanel()
	{
		JPanel panel = SwingUtils.createPanel("Show Counter Type", true);
		panel.setLayout(new MigLayout(_migDebug ? "debug, " : "" + "wrap 2, ins 0", "", "0[0]0"));

		_counterDelta_rb.setForeground(Color.BLUE);
		_counterRate_rb.setForeground(Color.BLUE);
		_counterPct1_lbl.setForeground(Color.RED);
		_counterPct2_lbl.setForeground(Color.RED);

		_counterRows_lbl.setToolTipText("Number of rows in the actual/visible. Where acual is numer of rows in the data model, and visible is rows after filtering...");
		_counterAbs_rb.setToolTipText("Absolute values of the counters.");
		_counterDelta_rb.setToolTipText("What is the difference since previous sample. Displayed with blue color.");
		_counterRate_rb.setToolTipText("Divide the difference between two samples with time elipsed since last sample, then we get diff or rate per second. Displayed with blue color");

		ButtonGroup group = new ButtonGroup();
		group.add(_counterAbs_rb);
		group.add(_counterDelta_rb);
		group.add(_counterRate_rb);

		panel.add(_counterAbs_rb, "");
		panel.add(_counterRows_lbl, "wrap");

		panel.add(_counterDelta_rb, "");
		panel.add(_counterPct1_lbl, "wrap");

		panel.add(_counterRate_rb, "");
		panel.add(_counterPct2_lbl, "wrap");

		return panel;
	}

	private JPanel createTimePanel()
	{
//		JPanel panel = SwingUtils.createPanel("Sample Information", true);
		JPanel panel = new JPanel()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getToolTipText()
			{
				CountersModel cm = _cmDisplay;
				if (cm == null)	cm = _cm;
				if (cm == null)	return null;
				return "<html>" +
						"SQL Refresh time: "+cm.getSqlRefreshTime()+" ms.<br>" +
						"GUI Refresh Time: "+cm.getGuiRefreshTime()+" ms.<br>" +
						"Local Calculation Time: "+cm.getLcRefreshTime()+" ms.<br>" +
						"</html>";
			}
		};
		panel.setToolTipText(""); // need to set the tooltip, otherwise it will grab parents tooltip.
		panel.setBorder(BorderFactory.createTitledBorder("Sample Information"));

//		panel.setLayout(new MigLayout(_migDebug ? "debug, " : "" + "wrap 2, ins 0", "[growx] [growx]", ""));
		panel.setLayout(new MigLayout("wrap 2, ins 0", "[fill] [fill]", ""));

		_timeClear_txt.setEditable(false);
		_timeSample_txt.setEditable(false);
		_timeIntervall_txt.setEditable(false);

		_timeViewStored_lbl.setVisible(false);
		_timeViewStored_lbl.setFont(_timeViewStored_lbl.getFont().deriveFont(Font.BOLD));
		_timeViewStored_lbl.setHorizontalTextPosition(JLabel.CENTER);

		_timeClear_txt     .setToolTipText("If sp_sysmon is executed and clears the counters, it could be nice to know that...");
		_timeSample_txt    .setToolTipText("Date when the data showned in the table was sampled.");
		_timeIntervall_txt .setToolTipText("Milliseconds since last sample period.");
		_timePostpone_txt  .setToolTipText("<html>If you want to skip some intermidiate samples, Here you can specify minimum seconds between samples.<br>tip: '10m' is 10 minutes, '24h' is 24 hours</html>");
		_timeViewStored_lbl.setToolTipText("You are viewing data that has been stored in the In Memory Counter Storage or the Persistent Counter Storage");

		_timeOfflineRewind_but.setToolTipText("Get privious sample with data");
		_timeOfflineRewind_but.setIcon(SwingUtils.readImageIcon(Version.class, "images/offline_rewind.png"));
		_timeOfflineRewind_but.setText(null);
		_timeOfflineRewind_but.setContentAreaFilled(false);
		_timeOfflineRewind_but.setMargin( new Insets(0,0,0,0) );
		_timeOfflineRewind_but.setVisible(false);

		_timeOfflineFastForward_but.setToolTipText("Get next sample with data");
		_timeOfflineFastForward_but.setIcon(SwingUtils.readImageIcon(Version.class, "images/offline_fastforward.png"));
		_timeOfflineFastForward_but.setText(null);
		_timeOfflineFastForward_but.setContentAreaFilled(false);
		_timeOfflineFastForward_but.setMargin( new Insets(0,0,0,0) );
		_timeOfflineFastForward_but.setVisible(false);
		
		panel.add(_timeClear_lbl, "");
		panel.add(_timeClear_txt, "width 132lp!, growx, wrap");

		panel.add(_timeSample_lbl, "");
		panel.add(_timeSample_txt, "width 132lp!, growx, wrap");

		panel.add(_timeIntervall_lbl, "");
		panel.add(_timeIntervall_txt, "width 132lp!, growx, wrap");

		panel.add(_timePostpone_lbl, "hidemode 3");
		panel.add(_timePostpone_txt, "hidemode 3, growx, wrap");

		panel.add(_timeOfflineRewind_but,      "hidemode 3, left, bottom, span 2, split 3");
		panel.add(_timeViewStored_lbl,         "hidemode 3, growx, center, bottom");
		panel.add(_timeOfflineFastForward_but, "hidemode 3, right,  bottom");
//		panel.add(_timeViewStored_lbl,         "hidemode 3, span, center, bottom");

		return panel;
	}

	private JPanel createOptionsPanel()
	{
		JPanel panel = SwingUtils.createPanel("Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		_optionPauseDataPolling_chk     .setToolTipText("<html>Pause data polling for this Tab. This makes the values easier to read...</html>");
		_optionEnableBgPolling_chk      .setToolTipText("<html>Sample this panel even when this Tab is not active.<br><b>Note:</b> If there are active graphs attached, it implies that data will be sampled even if this option is <b>not</b> enabled.</html>");
		_optionHasActiveGraphs_lbl      .setToolTipText("<html>Just an indicator if any Graphs are active or not, if this is true it means that this Performance Counter will be refreshed.</html>");
		_optionPersistCounters_chk      .setToolTipText("<html>Save this counter set to a Persistent storage, even when we are in GUI mode.<br>Note: This is only enabled/available if you specified a Counter Storage when you connected.</html>");
		_optionPersistCountersAbs_chk   .setToolTipText("<html>Store Absolute Counter values.</html>");
		_optionPersistCountersDiff_chk  .setToolTipText("<html>Store Difference Calculation between two samples.</html>");
		_optionPersistCountersRate_chk  .setToolTipText("<html>Store the Calculated Numers per Second.</html>");
		_optionNegativeDiffCntToZero_chk.setToolTipText("<html>If the differance between 'this' and 'previous' data sample has negative counter values, reset them to be <b>zero</b>" + "<p>This is good for most data tables, meaning if sp_sysmon resets the counters or counters wrap..." + "<p>It's not good for data tables where we want to watch counters that grows and shrinks, for example \"procedure cache memory usage\".</html>");

		// Fix up the _optionTrendGraphs_but
		TrendGraph.createGraphAccessButton(_optionTrendGraphs_but, _displayName);

		// Always have the has some stuff disabled...
		_optionHasActiveGraphs_lbl.setVisible(false);
		_optionTrendGraphs_but    .setVisible(false);

		panel.add(_optionPauseDataPolling_chk,      "push, grow, left, split");
		panel.add(_optionHasActiveGraphs_lbl,       "top");
		panel.add(_optionTrendGraphs_but,           "top, wrap");
		panel.add(_optionEnableBgPolling_chk,       "wrap");
		panel.add(_optionPersistCounters_chk,       "split 4");
		panel.add(_optionPersistCountersAbs_chk,    "");
		panel.add(_optionPersistCountersDiff_chk,   "");
		panel.add(_optionPersistCountersRate_chk,   "wrap");
		panel.add(_optionNegativeDiffCntToZero_chk, "wrap");

		return panel;
	}

	protected JPanel createLocalOptionsPanel()
	{
//		JPanel panel = SwingUtils.createPanel("Local Options", true);
//		panel.setLayout(new MigLayout(_migDebug ? "debug, " : "" + "ins 0, gap 0", "", "0[0]0"));
//
//		_optionPauseDataPolling_chk.setToolTipText("<html>Pause data polling for this Tab. This makes the values easier to read...</html>");
//
//		panel.add(_optionPauseDataPolling_chk, "wrap");
//
//		return panel;
		return null;
	}

	private JPanel createExtendedInfoPanel()
	{
		JPanel panel = SwingUtils.createPanel("Extended Information", false);
		// panel.setLayout(new MigLayout(true?"debug, ":""+"wrap 2",
		// "[grow] []",
		// "[grow,:100:]"));

		// panel.add(new
		// JTextArea("DUMMY text fileld...Extended Information......................"));
		panel.setLayout(new BorderLayout());
		panel.add(new JScrollPane(createTreeSpSysmon()), BorderLayout.CENTER);

		panel.setPreferredSize(new Dimension(0, 0));
		panel.setMinimumSize(new Dimension(0, 0));
		return panel;
	}

	private JTree createTreeSpSysmon()
	{
		DefaultMutableTreeNode top = new DefaultMutableTreeNode("sp_sysmon");
		DefaultMutableTreeNode heading = new DefaultMutableTreeNode("");
		DefaultMutableTreeNode subHead = new DefaultMutableTreeNode("");

		heading = new DefaultMutableTreeNode("Kernel Utilization");
		top.add(heading);
		subHead = new DefaultMutableTreeNode("Config");
		heading.add(subHead);
		subHead.add(new DefaultMutableTreeNode("Runnable Process Search Count"));
		subHead.add(new DefaultMutableTreeNode("I/O Polling Process Count"));

		subHead = new DefaultMutableTreeNode("Engine Busy Utilization");
		heading.add(subHead);
		subHead.add(new DefaultMutableTreeNode("Engine 0"));

		subHead = new DefaultMutableTreeNode("CPU Yields by Engine");
		heading.add(subHead);
		subHead.add(new DefaultMutableTreeNode("Engine 0"));

		subHead = new DefaultMutableTreeNode("Network Checks");
		heading.add(subHead);
		subHead.add(new DefaultMutableTreeNode("Non-Blocking"));
		subHead.add(new DefaultMutableTreeNode("Blocking"));
		subHead.add(new DefaultMutableTreeNode("Total Network I/O Checks"));
		subHead.add(new DefaultMutableTreeNode("Avg Net I/Os per Check"));

		subHead = new DefaultMutableTreeNode("Disk I/O Checks");
		heading.add(subHead);
		subHead.add(new DefaultMutableTreeNode("Total Disk I/O Checks"));
		subHead.add(new DefaultMutableTreeNode("Checks Returning I/O"));
		subHead.add(new DefaultMutableTreeNode("Avg Disk I/Os Returned"));

		heading = new DefaultMutableTreeNode("Worker Process Management");
		top.add(heading);

		heading = new DefaultMutableTreeNode("Parallel Query Management");
		top.add(heading);

		heading = new DefaultMutableTreeNode("Task Management");
		top.add(heading);

		subHead = new DefaultMutableTreeNode("Task Context Switches by Engine");
		heading.add(subHead);
		subHead.add(new DefaultMutableTreeNode("Engine 0"));

		subHead = new DefaultMutableTreeNode("Task Context Switches Due To");
		heading.add(subHead);

		subHead.add(new DefaultMutableTreeNode("Voluntary Yields"));
		subHead.add(new DefaultMutableTreeNode("Cache Search Misses"));
		subHead.add(new DefaultMutableTreeNode("Exceeding I/O batch size"));
		subHead.add(new DefaultMutableTreeNode("System Disk Writes"));
		subHead.add(new DefaultMutableTreeNode("Logical Lock Contention"));
		subHead.add(new DefaultMutableTreeNode("Address Lock Contention"));
		subHead.add(new DefaultMutableTreeNode("Latch Contention"));
		subHead.add(new DefaultMutableTreeNode("Log Semaphore Contention"));
		subHead.add(new DefaultMutableTreeNode("PLC Lock Contention"));
		subHead.add(new DefaultMutableTreeNode("Group Commit Sleeps"));
		subHead.add(new DefaultMutableTreeNode("Last Log Page Writes"));
		subHead.add(new DefaultMutableTreeNode("Modify Conflicts"));
		subHead.add(new DefaultMutableTreeNode("I/O Device Contention"));
		subHead.add(new DefaultMutableTreeNode("Network Packet Received"));
		subHead.add(new DefaultMutableTreeNode("Network Packet Sent"));
		subHead.add(new DefaultMutableTreeNode("Network services"));
		subHead.add(new DefaultMutableTreeNode("Other Causes"));

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
		// panel.setLayout(new MigLayout(true?"debug, ":""+"wrap 2",
		// "[grow] []",
		// "[grow,:100:]"));
		panel.setLayout(new BorderLayout());
		// String[] columns = {
		// "Col1 - asfkjha lkajhf alkjfhd askflakhf",
		// "Col2 - kljhafaskjgfa kjfhkas fkajhs fgkaj faksjfdhg",
		// "Col3 - hgfjhagdfkdjhg kjahgs fkjahg fkjagh kjgf kajgh fdkasj gf",
		// "Col4 - alskjdfhah gfdakj gfkjahg fkjasgh kdfjaghd f"};
		// Object[][] rows = {
		// {"r1-Col-1", "r1-Col-2", "r1-Col-3", "r1-Col-4"},
		// {"r2-Col-1", "r2-Col-2", "r2-Col-3", "r2-Col-4"},
		// {"r3-Col-1", "r3-Col-2", "r3-Col-3", "r3-Col-4"},
		// {"r4-Col-1", "r4-Col-2", "r4-Col-3", "r4-Col-4"},
		// };

		// ResultSetTableModel tm = null;
		// try
		// {
		// Connection conn = AseConnectionFactory.getConnection("goransxp",
		// 5000, null, "sa", "", Version.getAppName()+"-TCP-Test");
		//
		// Statement stmnt = conn.createStatement();
		// ResultSet rs = null;
		// if ( _displayName.startsWith("0-"))
		// rs = stmnt.executeQuery("select * from master..monState");
		// else if (_displayName.startsWith("1-"))
		// rs =
		// stmnt.executeQuery("select * from master..monOpenObjectActivity");
		// else if (_displayName.startsWith("2-"))
		// rs = stmnt.executeQuery("select * from master..monProcess");
		// else if (_displayName.startsWith("3-"))
		// rs = stmnt.executeQuery("select * from master..sysdatabases");
		// else if (_displayName.startsWith("4-"))
		// rs = stmnt.executeQuery("select * from master..monLocks");
		// else if (_displayName.startsWith("5-"))
		// rs = stmnt.executeQuery("select * from master..monEngine");
		// else if (_displayName.startsWith("6-"))
		// rs = stmnt.executeQuery("select version=@@version");
		// else if (_displayName.startsWith("7-"))
		// rs = stmnt.executeQuery("select * from master..monTableColumns");
		// else if (_displayName.startsWith("8-"))
		// rs = stmnt.executeQuery("select * from monDeviceIO");
		// else
		// rs = stmnt.executeQuery("select * from master..syslogins");
		//				
		// tm = new ResultSetTableModel(rs);
		//			
		// // first values should be FILTER_NO_COLUMN_IS_SELECTED
		// _filterColumn_cb.addItem(FILTER_NO_COLUMN_IS_SELECTED);
		// for (int i=0; i<tm.getColumnCount(); i++)
		// {
		// _filterColumn_cb.addItem(tm.getColumnName(i));
		// }
		// }
		// catch (Exception e)
		// {
		// e.printStackTrace();
		// }

		_dataTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		_dataTable.packAll(); // set size so that all content in all cells are
								// visible
		_dataTable.setSortable(true);
		_dataTable.setColumnControlVisible(true);
		_dataTable.setHighlighters(_highliters); // a variant of cell render

		// Fixing/setting background selection color... on some platforms it
		// seems to be a strange color
		// on XP a gray color of "r=178,g=180,b=191" is the default, which looks
		// good on the screen
//		Configuration conf = Configuration.getInstance(Configuration.CONF);
		Configuration conf = Configuration.getCombinedConfiguration();
		if ( conf != null )
		{
			if ( conf.getBooleanProperty("table.setSelectionBackground", true) )
			{
				Color newBg = new Color(conf.getIntProperty("table.setSelectionBackground.r", 178), conf.getIntProperty("table.setSelectionBackground.g", 180), conf.getIntProperty("table.setSelectionBackground.b", 191));

				_logger.debug("table.setSelectionBackground(" + newBg + ").");
				_dataTable.setSelectionBackground(newBg);
			}
		}
		else
		{
			Color bgc = _dataTable.getSelectionBackground();
			if ( !(bgc.getRed() == 178 && bgc.getGreen() == 180 && bgc.getBlue() == 191) )
			{
				Color newBg = new Color(178, 180, 191);
				_logger.debug("table.setSelectionBackground(" + newBg + "). Config could not be read, trusting defaults...");
				_dataTable.setSelectionBackground(newBg);
			}
		}

		JScrollPane scroll = new JScrollPane(_dataTable);
		_watermark = new Watermark(scroll, "Not Connected...");

		// panel.add(scroll, BorderLayout.CENTER);
		// panel.add(scroll, "");
		panel.add(scroll);
		return panel;
	}

	/*---------------------------------------------------
	 ** BEGIN: Action Listeners
	 **---------------------------------------------------
	 */
	private void initComponentActions()
	{
		// ---- FILTER PANEL -----
		_filterColumn_cb.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				filterAction(e, "COLUMN");
				saveFilterProps();
			}
		});
		_filterOperation_cb.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				filterAction(e, "OPERATION");
				saveFilterProps();
			}
		});
		_filterValue_tf.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				filterAction(e, "VALUE");
				saveFilterProps();
			}
		});

		_filterNoZeroCounters_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				filterAction(e, "NO_ZERO_COUNTERS");
				saveFilterProps();
			}
		});

		// ---- COUNTER TYPE PANEL -----
		_counterAbs_rb.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				CountersModel cm = _cm;
				if ( !_tailMode )
					cm = _cmDisplay;

				if ( cm != null )
					cm.setDataSource(CountersModel.DATA_ABS);
			}
		});

		_counterDelta_rb.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				CountersModel cm = _cm;
				if ( !_tailMode )
					cm = _cmDisplay;

				if ( cm != null )
					cm.setDataSource(CountersModel.DATA_DIFF);
			}
		});

		_counterRate_rb.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				CountersModel cm = _cm;
				if ( !_tailMode )
					cm = _cmDisplay;

				if ( cm != null )
					cm.setDataSource(CountersModel.DATA_RATE);
			}
		});

		// ---- SAMPLE TIME PANEL -----
		_timePostpone_txt.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				int postponeTime = parsePostponeTime(_timePostpone_txt.getText(), true);
				if ( postponeTime >= 0 )
				{
					CountersModel cm = _cm;
					if ( cm != null )
						cm.setPostponeTime(postponeTime, true);
				}
			}
		});
		_timeOfflineRewind_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Timestamp sampleId          = null;
				Timestamp currentSampleTime = null;
				String    cmName            = null;

				CountersModel cm = _cmDisplay;
				if ( cm != null )
				{
					sampleId          = cm.getSampleTimeHead();
					currentSampleTime = cm.getSampleTimeHead();
					cmName            = cm.getName();
				}
				else
				{
					sampleId          = MainFrame.getInstance().getCurrentSliderTs();
					currentSampleTime = MainFrame.getInstance().getCurrentSliderTs();
					cmName            = _cm.getName();
				}
				if (sampleId          == null) return;
				if (currentSampleTime == null) return;
				if (cmName            == null) return;

				PersistReader reader = PersistReader.getInstance();
				if (reader == null)
					throw new RuntimeException("The 'PersistReader' has not been initialized.");

				Timestamp ts = reader.getPrevSample(sampleId, currentSampleTime, cmName);
				if (ts != null)
				{
					reader.loadSessionCmIndicators(ts);
					reader.loadSummaryCm(ts);
				}
			}
		});
		_timeOfflineFastForward_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Timestamp sampleId          = null;
				Timestamp currentSampleTime = null;
				String    cmName            = null;

				CountersModel cm = _cmDisplay;
				if ( cm != null )
				{
					sampleId          = cm.getSampleTimeHead();
					currentSampleTime = cm.getSampleTimeHead();
					cmName            = cm.getName();
				}
				else
				{
					sampleId          = MainFrame.getInstance().getCurrentSliderTs();
					currentSampleTime = MainFrame.getInstance().getCurrentSliderTs();
					cmName            = _cm.getName();
				}
				if (sampleId          == null) return;
				if (currentSampleTime == null) return;
				if (cmName            == null) return;

				PersistReader reader = PersistReader.getInstance();
				if (reader == null)
					throw new RuntimeException("The 'PersistReader' has not been initialized.");

				Timestamp ts = reader.getNextSample(sampleId, currentSampleTime, cmName);
				if (ts != null)
				{
					reader.loadSessionCmIndicators(ts);
					reader.loadSummaryCm(ts);
				}
			}
		});

		// ---- OPTIONS PANEL -----
		_optionPauseDataPolling_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if ( _cm != null )
					_cm.setPauseDataPolling(_optionPauseDataPolling_chk.isSelected(), true);
			}
		});

		_optionEnableBgPolling_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if ( _cm != null )
					_cm.setBackgroundDataPollingEnabled(_optionEnableBgPolling_chk.isSelected(), true);
			}
		});

		_optionPersistCounters_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if ( _cm != null )
					_cm.setPersistCounters(_optionPersistCounters_chk.isSelected(), true);
			}
		});

		_optionPersistCountersAbs_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if ( _cm != null )
					_cm.setPersistCountersAbs(_optionPersistCountersAbs_chk.isSelected(), true);
			}
		});

		_optionPersistCountersDiff_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if ( _cm != null )
					_cm.setPersistCountersDiff(_optionPersistCountersDiff_chk.isSelected(), true);
			}
		});

		_optionPersistCountersRate_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if ( _cm != null )
					_cm.setPersistCountersRate(_optionPersistCountersRate_chk.isSelected(), true);
			}
		});

		_optionNegativeDiffCntToZero_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if ( _cm != null )
					_cm.setNegativeDiffCountersToZero(_optionNegativeDiffCntToZero_chk.isSelected(), true);
			}
		});
	}

	private void filterAction(ActionEvent e, String type)
	{
		if ("NO_ZERO_COUNTERS".equals(type))
		{
			if ( _filterNoZeroCounters_chk.isSelected() )
			{
				CountersModel cm = _cm;
				if ( !_tailMode )
					cm = _cmDisplay;

				if ( cm != null )
				{
					_tableRowFilterIsSet = _tableRowFilter.equals(_dataTable.getRowFilter());
					if ( !_tableRowFilterIsSet )
					{
						_tableRowFilterIsSet = true;
						_logger.debug("No table filter was priviously set, so lets set it now.");
						_dataTable.setRowFilter(_tableRowFilter);
					}
					_tableRowFilterDiffCntIsZero.setFilter(_dataTable, cm.getDiffColumns(), cm.getDiffDissColumns());
				}
			}
			else
			{
				_tableRowFilterDiffCntIsZero.resetFilter();
			}
			setWatermark();
			return;
		}

		//-------------------------------------------------------
		// for ALL other types... (COLUMN, OPERATION, VALUE)
		//-------------------------------------------------------
		int opIndex = _filterOperation_cb.getSelectedIndex();
		String column = (String) _filterColumn_cb.getSelectedItem();
		String opStr = (String) _filterOperation_cb.getSelectedItem();
		String text = (String) _filterValue_tf.getText();

		_logger.debug("FILTER: col='" + column + "', op='" + opStr + "', opIndex=" + opIndex + ", text='" + text + "', clazz='--'.");

		if ( _tableRowFilterValAndOp == null || column == null || text == null )
			return;

		if ( column.equals(FILTER_NO_COLUMN_IS_SELECTED) || text.trim().equals("") )
		{
			_tableRowFilterValAndOp.resetFilter();
		}
		else
		{
			try
			{
				int col = ((AbstractTableModel)_dataTable.getModel()).findColumn(column);
				_tableRowFilterIsSet = _tableRowFilter.equals(_dataTable.getRowFilter());
				if ( !_tableRowFilterIsSet )
				{
					_tableRowFilterIsSet = true;
					_logger.debug("No table filter was priviously set, so lets set it now.");
					_dataTable.setRowFilter(_tableRowFilter);
				}
				_tableRowFilterValAndOp.setFilter(opIndex, col, text);
			}
			catch(IllegalArgumentException ex) // When row wasn't found in: _dataTable.getColumn(column).getModelIndex()
			{
				_logger.info("Cant find column '"+column+"' in the datatable '"+_dataTable.getName()+"'.");
			}
		}
		setWatermark();
	}

	/*---------------------------------------------------
	 ** END: Action Listeners
	 **---------------------------------------------------
	 */

	/**
	 * Set how we should do Auto Adjustment of Column With in the JTable<br>
	 * - global = Use global settings, (default is false)<br>
	 * - true = Do automatic Adjustment for this TabularCntrPanel<br>
	 * - false = Use the saved settings<br>
	 * 
	 * @param val should be global/true/false
	 */
	public void setAutoAdjustTableColumnWidth(AutoAdjustTableColumnWidth val)
	{
		if (val == null)
			return;

		Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
		if (tmpConf != null)
		{
			tmpConf.setProperty(getPanelName()+".autoAdjustTableColumnWidth", val.toString());
			tmpConf.save();
		}
	}

	/**
	 * Get if we should do Automatic Column With adjustments<br>
	 * - First check the boolean Configuration "TabularCntrPanel.autoAdjustTableColumnWidth"<br>
	 * - Then get the local settings..
	 * 
	 * @return
	 */
	public boolean doAutoAdjustTableColumnWidth()
	{
		Configuration conf = Configuration.getCombinedConfiguration();

		//		boolean doAdjust = true;
		boolean doAdjust = false;
		doAdjust = conf.getBooleanProperty("TabularCntrPanel.autoAdjustTableColumnWidth", doAdjust);

		String key = getPanelName()+".autoAdjustTableColumnWidth";
		String autoAdjustTableColumnWidth = conf.getProperty(getPanelName()+".autoAdjustTableColumnWidth", AutoAdjustTableColumnWidth.GLOBAL.toString());

		if      (autoAdjustTableColumnWidth.equals(AutoAdjustTableColumnWidth.GLOBAL  .toString())) { /*keep doAdjust from above*/ }
		else if (autoAdjustTableColumnWidth.equals(AutoAdjustTableColumnWidth.AUTO_ON .toString())) doAdjust = true;
		else if (autoAdjustTableColumnWidth.equals(AutoAdjustTableColumnWidth.AUTO_OFF.toString())) doAdjust = false;
		else _logger.warn(getPanelName()+" Can't find appropriate value for "+key+" = '"+autoAdjustTableColumnWidth+"'.");

		// If the proprty 'XXXXXX.gui.column.header.props' has NOT been written, then DO auto adjust.
		// This means that it's probably the first time we see data...
		String headerProps = conf.getProperty(getName()+".gui.column.header.props");
		if (headerProps == null)
			doAdjust = true;

		return doAdjust;
	}
	
	/**
	 * helper method to convert a postpone time into seconds
	 * <p>
	 * The input String <code>postponeStr</code> can be in the format:
	 * <ul>
	 * <li>'10m' is 10 Minutes which is translated into 600</li>
	 * <li>'10h' is 10 Hours which is translated into 36000</li>
	 * </ul>
	 */
	public static int parsePostponeTime(String postponeStr, boolean guiError)
	{
		if ( postponeStr == null )
			return 0;
		postponeStr = postponeStr.trim();
		if ( postponeStr.equals("") )
			return 0;

		try
		{
			int postponeTime = 0;
			int multiplyBy = 1;

			if ( postponeStr.endsWith("M") || postponeStr.endsWith("m") )
			{
				multiplyBy = 60;
				postponeStr = postponeStr.substring(0, postponeStr.length() - 1);
			}
			if ( postponeStr.endsWith("H") || postponeStr.endsWith("h") )
			{
				multiplyBy = 3600;
				postponeStr = postponeStr.substring(0, postponeStr.length() - 1);
			}
			postponeTime = Integer.parseInt(postponeStr.trim()) * multiplyBy;
			return postponeTime;
		}
		catch (NumberFormatException ex)
		{
			if ( guiError )
				SwingUtils.showInfoMessage(null, "Not a Number", "The postpone needs to be specified as a number.");
			else
				_logger.warn("The postpone needs to be specified as a number.");
		}
		return -1;
	}

	/** called from MainFrame when the main window closes */
	protected void saveProps()
	{
		// This is now done in the TCPTable, when columns are moved or removed
		//if (_dataTable != null)
		//_dataTable.saveColumnLayout();
	}

	/**  */
	protected void saveFilterProps()
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null) 
			return;

		// If nothing in table, it's probably a call on initialization...
		if (_dataTable.getColumnCount() == 0)
			return;
		if (_filterColumn_cb.getItemCount() <= 1) // one rows is probably there FILTER_NO_COLUMN_IS_SELECTED <none>
			return;

		// Get prefix name to use
		String cmName = getName(); // Name of the JTable component, which should be the CM name
		if (cmName == null)
		{
			_logger.warn("Can't save Filters, because the JTable has not been assigned a name. getName() on the JTable is null.");
			return;
		}

		String keyPrefix = cmName + ".filter.";

		conf.setProperty(keyPrefix+"column",        _filterColumn_cb.getSelectedItem().toString());
		conf.setProperty(keyPrefix+"operation",     FILTER_OP_STR_ARR_SHORT[_filterOperation_cb.getSelectedIndex()]);
		conf.setProperty(keyPrefix+"value",         _filterValue_tf.getText());
		conf.setProperty(keyPrefix+"noZeroCounter", _filterNoZeroCounters_chk.isSelected());
		conf.save();
	}

	/**  */
	protected void loadFilterProps()
	{
//		Configuration conf = Configuration.getInstance(Configuration.TEMP);
		Configuration conf = Configuration.getCombinedConfiguration();
		if (conf == null) 
			return;

		// Get prefix name to use
		String cmName = getName(); // Name of the JTable component, which should be the CM name
		if (cmName == null)
		{
			_logger.warn("Can't load Filters, because the JTable has not been assigned a name. getName() on the JTable is null.");
			return;
		}

		String keyPrefix = cmName + ".filter.";

		String column        = conf.getProperty(keyPrefix+"column");
		String operation     = conf.getProperty(keyPrefix+"operation");
		String value         = conf.getProperty(keyPrefix+"value");
		String noZeroCounter = conf.getProperty(keyPrefix+"noZeroCounter");

		// Load values...
		if (column != null)
			_filterColumn_cb.setSelectedItem(column);

		if (operation != null)
		{
			int opIndex = 0;
			for (int i=0; i<FILTER_OP_STR_ARR_SHORT.length; i++)
				if (operation.equals(FILTER_OP_STR_ARR_SHORT[i]))
					opIndex = i;
			_filterOperation_cb.setSelectedIndex(opIndex);
		}

		if (value != null)
			_filterValue_tf.setText(value);

		if (noZeroCounter != null)
			_filterNoZeroCounters_chk.setSelected(noZeroCounter.equalsIgnoreCase("true"));

		// kick of the action...
		if (column != null || operation != null || value != null)
			filterAction(null, "COLUMN,OPERATION,VALUE");

		if (noZeroCounter != null)
			filterAction(null, "NO_ZERO_COUNTERS");
	}

	/*---------------------------------------------------
	 **---------------------------------------------------
	 **---------------------------------------------------
	 **---- SUBCLASSES ---- SUBCLASES ---- SUBCLASES ----- 
	 **---------------------------------------------------
	 **---------------------------------------------------
	 **---------------------------------------------------
	 */
	/**
	 * This timer is started when a colomn in the table has been moved/removed
	 * It will save the column order layout...
	 * A timer is needed because, when we move a column the method columnMoved() is kicked of
	 * for every pixel we move the mouse.
	 */
	private class ColumnLayoutTimerAction implements ActionListener
	{
		private TCPTable _tab = null;
		ColumnLayoutTimerAction(TCPTable tab)
		{
			_tab = tab;
		}
		@Override
		public void actionPerformed(ActionEvent actionevent)
		{
			_tab.saveColumnLayout();
			_tab._columnLayoutTimer.stop();
		}
	}

	public class TCPTable extends JXTable
	{
		private static final long	serialVersionUID			= 8891472887299452415L;
		private int					_lastMousePressedAtModelCol	= -1;
		private int					_lastMousePressedAtModelRow	= -1;
		private TCPTable            _thisTable                  = null;
//		private boolean             _hasNewModel                = true;
		private boolean             _tableStructureChangedFlag  = true;

		/** If columns are reordered, save it after X seconds inactivity */
		protected Timer             _columnLayoutTimer          = null;

		private TCPTable()
		{
			init();
		}

		private TCPTable(TableModel tm)
		{
			super(tm);
			init();
		}

		public int getLastMousePressedAtModelCol()
		{
			return _lastMousePressedAtModelCol;
		}

		public int getLastMousePressedAtModelRow()
		{
			return _lastMousePressedAtModelRow;
		}

		public boolean isLastMousePressedAtModelRowColValid()
		{
			return _lastMousePressedAtModelRow >= 0 && _lastMousePressedAtModelCol >= 0;
		}

		/** just wrap the super setModel() */
		@Override
		public void setModel(TableModel newModel)
		{
//			_hasNewModel = true;
			super.setModel(newModel);
			loadColumnLayout();
		}

		private void init()
		{
			// wait 1 seconds before column layout is saved, this simply means less config writes...
			_columnLayoutTimer = new Timer(1000, new ColumnLayoutTimerAction(this));
			_thisTable = this;

			//
			// Cell renderer changes to "Rate" Counters
			//
			// The normal formatter doesn't add '.0' if values are even
			// Make '0'     -> '0.0' 
			//  and '123'   -> '123.0' 
			//  and '123.5' -> '123.5'
			@SuppressWarnings("serial")
			StringValue sv = new StringValue() 
			{
				NumberFormat nf = null;
				{ // init/constructor section
					try
					{
						nf = new DecimalFormat();
						nf.setMinimumFractionDigits(1);
					}
					catch (Throwable t)
					{
						nf = NumberFormat.getInstance();
					}
				}
				public String getString(Object value) 
				{
					if ( ! (value instanceof BigDecimal) ) 
						return StringValues.TO_STRING.getString(value);
					return nf.format(value);
				}
			};
			// bind the RATE values (which happens to be BigDecimal)
			setDefaultRenderer(BigDecimal.class, new DefaultTableRenderer(sv, JLabel.RIGHT));

			//--------------------------------------------------------------------
			// Add mouse listener to be used to identify what row/col we are at.
			// this is used from the context menu, to do copy of cell or row
			//--------------------------------------------------------------------
			addMouseListener(new MouseAdapter()
			{
				// public void mouseClicked(MouseEvent e)

				// Done on left&right click
				// if you any want left-click(select) use method mouseClicked()
				// instead
				@Override
				public void mousePressed(MouseEvent e)
				{
					_lastMousePressedAtModelCol = -1;
					_lastMousePressedAtModelRow = -1;

					Point p = new Point(e.getX(), e.getY());
					int col = columnAtPoint(p);
					int row = rowAtPoint(p);

					if ( row >= 0 && col >= 0 )
					{
						_lastMousePressedAtModelCol = convertColumnIndexToModel(col);
						_lastMousePressedAtModelRow = convertRowIndexToModel(row);
					}
				}
			});

			//--------------------------------------------------------------------
			// listen on changes in the column header.
			// Used to save/restore column order
			//--------------------------------------------------------------------
			TableColumnModelExtListener columnModelListener = new TableColumnModelExtListener() 
			{
				@Override
				public void columnPropertyChange(PropertyChangeEvent e) {}
				@Override
				public void columnMarginChanged(ChangeEvent e)          {columnMovedOrRemoved(null);}
				@Override
				public void columnSelectionChanged(ListSelectionEvent e){}

				@Override
				public void columnAdded(TableColumnModelEvent e)
				{
					// If a new model has been loaded AND it's the LAST column we are adding
					// then load the column layout
					//System.out.println("------columnAdded(): tabName='"+getName()+"', _hasNewModel="+_hasNewModel+", modelCount="+getModel().getColumnCount()+", getToIndex="+e.getToIndex()+".");
					
//					if (_hasNewModel && getModel().getColumnCount()-1 == e.getToIndex())
//					{
//						_logger.debug("columnAdded(): tabName='"+getName()+"', TIME TO LOAD COL ORDER.");
//						_hasNewModel = false;
//						_thisTable.loadColumnLayout();
//					}
//System.out.println("tabname="+StringUtil.left(getName(), 30)+", modelColCount-1="+(getModel().getColumnCount()-1)+", toIndex="+e.getToIndex());
					if (_tableStructureChangedFlag && getModel().getColumnCount()-1 == e.getToIndex())
					{
//if (getName().equals("CMobjActivity"))
//System.out.println("columnAdded(): tabName='"+getName()+"', TIME TO LOAD COL ORDER.");
						_logger.debug("columnAdded(): tabName='"+getName()+"', TIME TO LOAD COL ORDER.");
						_tableStructureChangedFlag = false;
						_thisTable.loadColumnLayout();
					}
				}

				@Override
				public void columnRemoved(TableColumnModelEvent e)      {columnMovedOrRemoved(e);}
				@Override
				public void columnMoved(TableColumnModelEvent e)        {columnMovedOrRemoved(e);}
				private void columnMovedOrRemoved(TableColumnModelEvent e)
				{
					if (_columnLayoutTimer.isRunning())
						_columnLayoutTimer.restart();
					else
						_columnLayoutTimer.start();
				}
			};
			getColumnModel().addColumnModelListener(columnModelListener);

			// Set special Render to print multiple columns sorts
			_thisTable.getTableHeader().setDefaultRenderer(new MultiSortTableCellHeaderRenderer());

			//--------------------------------------------------------------------
			// New SORTER that toggles from DESCENDING -> ASCENDING -> UNSORTED
			//--------------------------------------------------------------------
			_thisTable.setSortOrderCycle(SortOrder.DESCENDING, SortOrder.ASCENDING, SortOrder.UNSORTED);
		}

		public void printColumnLayout(String prefix)
		{
			if (getColumnCount() == 0)
				return;

			// Get prefix name to use
			String cmName = getName(); // Name of the JTable component, which should be the CM name
			if (cmName == null)
			{
				_logger.debug("Can't print Column Layout, because the JTable has not been assigned a name. getName() on the JTable is null.");
				return;
			}

			TableColumnModel tcm = getColumnModel();

			for (TableColumn tc : getColumns(true))
			{
				TableColumnExt tcx     = (TableColumnExt) tc;
				String         colName = tc.getHeaderValue().toString();

				// Visible
				boolean colIsVisible = tcx.isVisible();

				// Sorted
				SortOrder colSort = getSortOrder(colName);

				// View/model position
				int colModelPos = tcx.getModelIndex();
				int colViewPos  = -1;
				try {colViewPos = tcm.getColumnIndex(colName);}
				catch (IllegalArgumentException ignore) {}

				System.out.println(prefix + "printColumnLayout() cm='"+cmName+"': colName="+StringUtil.left(colName,30)+", modelPos="+colModelPos+", viewPos="+colViewPos+", isVisible="+colIsVisible+", sort="+colSort+", identifier='"+tcx.getIdentifier()+"', toString="+tc);
			}
		}
		/**
		 * Load column order/layout from the saved vales in the temporary properties file.
		 */
		public void loadColumnLayout()
		{
//if ("CMobjActivity".equals(getName()))
//new Exception("loadColumnLayout() was CALLED from").printStackTrace();
//			Configuration conf = Configuration.getInstance(Configuration.TEMP);
			Configuration conf = Configuration.getCombinedConfiguration();
			if (conf == null) 
				return;

			if (getColumnCount() == 0)
				return;

			// Get prefix name to use
			String cmName = getName(); // Name of the JTable component, which should be the CM name
			if (cmName == null)
			{
				_logger.debug("Can't load Column Layout, because the JTable has not been assigned a name. getName() on the JTable is null.");
				return;
			}

			// get the values from configuration
			String confKey = cmName + ".gui.column.header.props";
			String confVal = conf.getProperty(confKey);
			if (confVal == null)
				return;

			// split on '; ' and stuff the entries in a Map object
			LinkedHashMap<String, ColumnHeaderPropsEntry> colProps = new LinkedHashMap<String, ColumnHeaderPropsEntry>();
			String[] strArr = confVal.split("; ");
			for (int i=0; i<strArr.length; i++)
			{
				try 
				{
					// each entry looks like: colName={modelPos=1,viewPos=1,isVisible=true,sort=unsorted}
					// where modelPos=int[0..999], viewPos=int[0..999], isVisible=boolean[true|false], sort=String[unsorted|ascending|descending]
					ColumnHeaderPropsEntry chpe = ColumnHeaderPropsEntry.parseKeyValue(strArr[i]);
					colProps.put(chpe._colName, chpe);
				}
				catch (ParseException e)
				{
					_logger.info("Problems parsing '"+confKey+"' with string '"+strArr[i]+"'. Caught: "+e);
					continue;
				}
			}

			// If cable model and config are "out of sync", do not load
			if (colProps.size() != getModel().getColumnCount())
			{
				_logger.info(confKey + " has '"+colProps.size()+"' values and the table model has '"+getModel().getColumnCount()+"' columns. I will skip moving columns around, the original column layout will be used.");
				return;
			}

			// Now move the columns in right position
			// make it recursive until no more has to be moved
			for (int i=0; i<colProps.size(); i++)
			{
				if (loadColumnLayout(colProps) == 0)
					break;
			}

			// SETTING SORT ORDER
			// Find the highest sorted column
			int maxSortOrderPos = -1;
			for (ColumnHeaderPropsEntry chpe : colProps.values())
				maxSortOrderPos = Math.max(maxSortOrderPos, chpe._sortOrderPos);

			// now APPLY the sorts in the correct order.
			// Starting with the highest number... 
			// The LAST one you do setSortOrder() will be SORT COLUMN 1
			for (int i=maxSortOrderPos; i>0; i--)
			{
				for (ColumnHeaderPropsEntry chpe : colProps.values())
				{
					if (chpe._sortOrderPos == i)
					{
						if (_logger.isDebugEnabled())
							_logger.debug(i+": Setting '"+StringUtil.left(chpe._colName,20)+"', viewPos="+chpe._viewPos+",  to "+chpe._sortOrder+", sortOrderPos="+chpe._sortOrderPos+", ModelColumnCount="+getModel().getColumnCount()+", RowSorterModelColumnCount="+getRowSorter().getModel().getColumnCount()+", name="+getName());

						if (chpe._viewPos < getRowSorter().getModel().getColumnCount())
							setSortOrder(chpe._viewPos, chpe._sortOrder);
						else
							_logger.debug("Can't set the sort order for column '"+chpe._colName+"'. viewPos < RowSorterModelColumnCount, this will be retried later? Info RowSorterModelColumnCount="+getRowSorter().getModel().getColumnCount()+", TableModelColumnCount="+getModel().getColumnCount()+", viewPos="+chpe._viewPos+", TableName="+getName());
					}
				}
			}
			
		}
		protected int loadColumnLayout(Map<String, ColumnHeaderPropsEntry> colProps)
		{
			int fixCount = 0;
			TableColumnModelExt tcmx = (TableColumnModelExt)getColumnModel();
			for (Map.Entry<String,ColumnHeaderPropsEntry> entry : colProps.entrySet()) 
			{
				String                 colName = entry.getKey();
				ColumnHeaderPropsEntry chpe    = entry.getValue();

				// Hide/show column
				TableColumnExt tcx = tcmx.getColumnExt(colName);
				if (tcx != null)
				{
					if ( chpe._isVisible == false && tcx.isVisible() )
					{
						_logger.trace("loadColumnLayout() cm='"+getName()+"': ACTION -> HIDE '"+colName+"'.");
						tcx.setVisible(false);
						fixCount++;
					}

					if ( chpe._isVisible == true && !tcx.isVisible() )
					{
						_logger.trace("loadColumnLayout() cm='"+getName()+"': ACTION -> SHOW '"+colName+"'.");
						tcx.setVisible(true);
						fixCount++;
					}
				}

				// Move column
				int colViewPos = -1;
				try {colViewPos = tcmx.getColumnIndex(colName);}
				catch (IllegalArgumentException ignore) {}
				
				int propViewPos = chpe._viewPos; 

				_logger.trace("loadColumnLayout() cm='"+getName()+"': info '"+StringUtil.left(colName,30)+"' colViewPos(from)='"+colViewPos+"', chpe._viewPos(to)='"+chpe._viewPos+"'.");
				if (colViewPos >= 0 && propViewPos >= 0)
				{
					if (colViewPos != propViewPos)
					{
						_logger.trace("loadColumnLayout() cm='"+getName()+"': ACTION -> MOVE '"+colName+"' from '"+colViewPos+"' -> '"+propViewPos+"'.");

						// hmmm, this will trigger columnMove
						// but we have the timer before saveColumnLayout is kicked of, so we should be fine
						// and also since we have already read it into local variables it doesn't matter.
						tcmx.moveColumn(colViewPos, propViewPos);
						fixCount++;
					}
				}

//				// sorting
//				SortOrder currentSortOrder = SortOrder.UNSORTED;
//				if (colViewPos >= 0) 
//					currentSortOrder = getSortOrder(colViewPos);
////if (getName().equals("CMobjActivity"))
////System.out.println("loadColumnLayout() SORT TO: cm='"+getName()+"': info '"+StringUtil.left(colName,30)+"' chpe._sortOrder='"+chpe._sortOrder+"', currentSortOrder='"+currentSortOrder+"'.");
//				if (chpe._sortOrder != currentSortOrder)
//				{
////if (getName().equals("CMobjActivity"))
////System.out.println("loadColumnLayout() CHANGING SORT ORDER to: chpe._viewPos="+chpe._viewPos+", chpe._sortOrder="+chpe._sortOrder);
//					_logger.trace("loadColumnLayout() SORT TO: cm='"+getName()+"': info '"+StringUtil.left(colName,30)+"' viewPos='"+chpe._viewPos+"', sortOrder(to)='"+chpe._sortOrder+"'.");
//					setSortOrder(chpe._viewPos, chpe._sortOrder);
//				}

				// WIDTH
				int colWidth = chpe._width; 
				if (colWidth > 0)
				{
					if (tcx != null)
					{
						tcx.setPreferredWidth(colWidth);
						tcx.setWidth(colWidth);
					}
				}

				// Initially set all columns to UNSORTED
				// setting the order will be done later
				if (colViewPos >= 0) 
				{
					if (getSortOrder(colViewPos) != SortOrder.UNSORTED)
						setSortOrder(colViewPos, SortOrder.UNSORTED);
				}
			}
			return fixCount;
		}

		/** Save column order/layout in the temporary properties file. */
		public void saveColumnLayout()
		{
			saveColumnLayout(false);
		}
		/** Save column order/layout in the temporary properties file. 
		 * @param toOriginalLayout if we want to save the original layout, which makes restore esier.
		 */
		public void saveColumnLayout(boolean toOriginalLayout)
		{
			Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
			if (conf == null) 
				return;

			if (getColumnCount() == 0)
				return;

			// Get prefix name to use
			String cmName = getName(); // Name of the JTable component, which should be the CM name
			if (cmName == null)
			{
				_logger.debug("Can't load Column Layout, because the JTable has not been assigned a name. getName() on the JTable is null.");
				return;
			}

			String confKey = cmName + ".gui.column.header.props";
			String confVal = "";

			TableColumnModel tcm = getColumnModel();

			for (TableColumn tc : getColumns(true))
			{
				TableColumnExt tcx     = (TableColumnExt) tc;
				String         colName = tc.getHeaderValue().toString();

				// Visible
				boolean colIsVisible = tcx.isVisible();

				// View/model position
				int colModelPos = tcx.getModelIndex();
				int colViewPos  = -1;
				try {colViewPos = tcm.getColumnIndex(colName);}
				catch (IllegalArgumentException ignore) {}

				// Column width
				int colWidth = tc.getWidth();

				// Sorted
				SortOrder colSort    = getSortOrder(colName);
				int       colSortPos = getSortOrderIndex(colName);

//if (getName().equals("CMobjActivity"))
//System.out.println("saveColumnLayout() cm='"+cmName+"': colName="+StringUtil.left(colName,30)+", modelPos="+colModelPos+", viewPos="+colViewPos+", isVisible="+colIsVisible+", sort="+colSort+", identifier='"+tcx.getIdentifier()+"', toString="+tc);
				_logger.debug("saveColumnLayout() cm='"+cmName+"': colName="+StringUtil.left(colName,30)+", modelPos="+colModelPos+", viewPos="+colViewPos+", isVisible="+colIsVisible+", sort="+colSort+", sortPos="+colSortPos+", identifier='"+tcx.getIdentifier()+"', width="+colWidth+", toString="+tc);

				ColumnHeaderPropsEntry chpe = new ColumnHeaderPropsEntry(colName, colModelPos, colViewPos, colIsVisible, colSort, colSortPos, colWidth);
				if (toOriginalLayout)
				{
					chpe._viewPos      = colModelPos;
					chpe._isVisible    = true;
					chpe._sortOrder    = SortOrder.UNSORTED;
					chpe._sortOrderPos = 0;
					chpe._width        = -1;
				}

				// Append to the Config Value
				confVal += chpe+"; ";
			}
			confVal = confVal.substring(0, confVal.length()-2);
			_logger.debug("saveColumnLayout() SAVE PROPERTY: "+confKey+"="+confVal);

			conf.setProperty(confKey, confVal);
			conf.save();
		}
		
		/** 
		 * restore original column layout, the original layout is the same as the order from the model 
		 */
		public void setOriginalColumnLayout()
		{
			saveColumnLayout(true);
			loadColumnLayout();
		}

		/**
		 * Get the sort index for a specific column.
		 * @param colModelIndex
		 * @return -1 if the column is not sorted, else it would be a number greater than 0.
		 */
		public int getSortOrderIndex(int colModelIndex)
		{
			List<? extends SortKey> sortKeys = this.getRowSorter().getSortKeys();
			if ( sortKeys == null || sortKeys.size() == 0 )
				return -1;

			int sortIndex = 1;
			for (SortKey sortKey : sortKeys)
			{
				if (sortKey.getSortOrder() == SortOrder.UNSORTED)
					continue;

				if ( sortKey.getColumn() == colModelIndex )
					return sortIndex;

				sortIndex++;
			}

			return -1;
		}

		/**
		 * Get the sort index for a specific column.
		 * @param colName
		 * @return -1 if the column is not sorted, else it would be a number greater than 0.
		 */
		public int getSortOrderIndex(String colName)
		{
			try
			{
				int colModelIndex = this.getColumn(colName).getModelIndex();
				return getSortOrderIndex(colModelIndex);
			}
			catch (IllegalArgumentException ignore)
			{
				return -1;
			}
		}

		/**
		 * To be able select/UN-SELECT rows in a table Called when a row/cell is
		 * about to change. getSelectedRow(), still shows what the *current*
		 * selection is
		 */
		@Override
		public void changeSelection(int row, int column, boolean toggle, boolean extend)
		{
			_logger.debug("changeSelection(row=" + row + ", column=" + column + ", toggle=" + toggle + ", extend=" + extend + "), getSelectedRow()=" + getSelectedRow() + ", getSelectedColumn()=" + getSelectedColumn());

			// if "row we clicked on" is equal to "currently selected row"
			// and also check that we do not do "left/right on keyboard"
			if ( row == getSelectedRow() && (column == getSelectedColumn() || getSelectedColumn() < 0) )
			{
				toggle = true;
				_logger.debug("changeSelection(): change toggle to " + toggle + ".");
			}

			super.changeSelection(row, column, toggle, extend);
		}

		/* Called on fire* has been called on the TableModel */
		@Override
		public void tableChanged(final TableModelEvent e)
		{
			if ( ! SwingUtilities.isEventDispatchThread() )
			{
//			    SwingUtilities.invokeLater(new Runnable() {
//			    	public void run() {
//			    		privateTableChanged(e);
//			    	}
//			    });
			    try
				{
					SwingUtilities.invokeAndWait(new Runnable() {
					    public void run() {
					    	privateTableChanged(e);
					    }
					});
				}
				catch (InterruptedException e1)      { _logger.info("SwingUtilities.invokeAndWait(privateTableChanged), Caught: "+e1); }
				catch (InvocationTargetException e1) { _logger.info("SwingUtilities.invokeAndWait(privateTableChanged), threw exception: "+e1, e1); }
			}
			else
	        	privateTableChanged(e);
				
		}
		private void privateTableChanged(TableModelEvent e)
		{
			// new Exception().printStackTrace();
			int viewSelectedRow = getSelectedRow();
			int modelRowBefore = -1;
			if ( viewSelectedRow >= 0 )
				modelRowBefore = convertRowIndexToModel(getSelectedRow());

			super.tableChanged(e);

			// it looks like either JTable or JXTable looses the selected row
			// after "fireTableDataChanged" has been called...
			// So try to set it back to where it previously was!
			if ( modelRowBefore >= 0 )
			{
				int viewRowNow = convertRowIndexToView(modelRowBefore);
				if ( viewRowNow >= 0 )
					getSelectionModel().setSelectionInterval(viewRowNow, viewRowNow);
			}

			// event: AbstactTableModel.fireTableStructureChanged
			if ( SwingUtils.isStructureChanged(e) )
			{
				_tableStructureChangedFlag = true;
				loadColumnLayout();
			}
		}

		// public TableCellRenderer getCellRenderer(int row, int column)
		// {
		// return _tableDiffDataCellRenderer;
		// TableCellRenderer renderer = super.getCellRenderer(row, column);
		// if (_cm != null )
		// {
		// if (_cm.showAbsolute())
		// return renderer;
		//
		// if (_cm.isDeltaCalculatedColumn(column))
		// {
		// return _tableDiffDataCellRenderer;
		// }
		// }
		// return renderer;
		// }

		// 
		// TOOL TIP for: TABLE HEADERS
		//
		@Override
		protected JTableHeader createDefaultTableHeader()
		{
			return new JXTableHeader(getColumnModel())
			{
				private static final long	serialVersionUID	= -4987530843165661043L;

				@Override
				public String getToolTipText(MouseEvent e)
				{
					// Now get the column name, which we point at
					Point p = e.getPoint();
					int index = getColumnModel().getColumnIndexAtX(p.x);
					if ( index < 0 )
						return null;
					Object colNameObj = getColumnModel().getColumn(index).getHeaderValue();

					// Now get the ToolTip from the CounterTableModel
					String toolTip = null;
					if ( colNameObj instanceof String )
					{
						String colName = (String) colNameObj;
						if ( _cm != null )
							toolTip = _cm.getToolTipTextOnTableColumn(colName);
					}
					return toolTip;
				}
			};
		}

		// 
		// TOOL TIP for: CELLS
		//
		@Override
		public String getToolTipText(MouseEvent e)
		{
			String tip = null;
			Point p = e.getPoint();
			int row = rowAtPoint(p);
			int col = columnAtPoint(p);
			if ( row >= 0 && col >= 0 )
			{
				col = super.convertColumnIndexToModel(col);
				row = super.convertRowIndexToModel(row);

				TableModel model = getModel();
				String colName = model.getColumnName(col);
				Object cellValue = model.getValueAt(row, col);

				if ( model instanceof CountersModel )
				{
					CountersModel cm = (CountersModel) model;
					tip = cm.getToolTipTextOnTableCell(e, colName, cellValue, row, col);

					// Do we want to use "focusable" tips?
					if (tip != null) 
					{
						if (_focusableTip == null) 
							_focusableTip = new FocusableTip(this);

//							_focusableTip.setImageBase(imageBase);
						_focusableTip.toolTipRequested(e, tip);
					}
					// No tooltip text at new location - hide tip window if one is
					// currently visible
					else if (_focusableTip!=null) 
					{
						_focusableTip.possiblyDisposeOfTipWindow();
					}
					return null;
				}
			}
//			if ( tip != null )
//				return tip;
			return getToolTipText();
		}

		// // TableCellRenderer _tableDiffDataCellRenderer = new
		// DefaultTableCellRenderer()
		// TableCellRenderer _tableDiffDataCellRenderer = new
		// DefaultTableRenderer()
		// {
		// private static final long serialVersionUID = -4439199147374261543L;
		//
		// public Component getTableCellRendererComponent(JTable table, Object
		// value, boolean isSelected, boolean hasFocus, int row, int column)
		// {
		// Component comp = super.getTableCellRendererComponent(table, value,
		// isSelected, hasFocus, row, column);
		// // if (value == null || _cm == null)
		// // return comp;
		// // if (value == null)
		// // return comp;
		//
		// // ((JLabel)comp).setHorizontalAlignment(RIGHT);
		// // if ( _cm.isPctColumn(column) )
		// // {
		// // comp.setForeground(Color.red);
		// // }
		// // else
		// // {
		// // comp.setForeground(Color.blue);
		// // if ( value instanceof Number )
		// // {
		// // if ( ((Number)value).doubleValue() != 0.0 )
		// // {
		// // comp.setFont( comp.getFont().deriveFont(Font.BOLD) );
		// // }
		// // }
		// // }
		// // return comp;
		// if ( value instanceof Number )
		// {
		// comp.setForeground(Color.blue);
		// // ((JLabel)comp).setHorizontalAlignment(RIGHT);
		// if ( ((Number)value).doubleValue() != 0.0 )
		// {
		// comp.setFont( comp.getFont().deriveFont(Font.BOLD) );
		// }
		// }
		// return comp;
		// }
		// };
	}
	private FocusableTip _focusableTip;
	
	/*---------------------------------------------------
	 ** BEGIN: Highlighter stuff for the JXTable
	 **---------------------------------------------------
	 */
	public void addHighlighter(Highlighter highlighter)
	{
		_dataTable.addHighlighter(highlighter);
	}

	private HighlightPredicate	_highligtIfDelta	= new HighlightPredicate()
	{
		@Override
		public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
		{
			CountersModel cm = _cm;
			if ( !_tailMode )
				cm = _cmDisplay;
	
			if ( cm == null )                               return false;
			if ( !cm.isDataInitialized() )                  return false;
			if ( cm.discardDiffPctHighlighterOnAbsTable() ) return false;
			return cm.isDiffColumn(adapter.convertColumnIndexToModel(adapter.column));
		}
	};
	private HighlightPredicate	_highligtIfPct		= new HighlightPredicate()
	{
		@Override
		public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
		{
			CountersModel cm = _cm;
			if ( !_tailMode )
				cm = _cmDisplay;

			if ( cm == null )                               return false;
			if ( !cm.isDataInitialized() )                  return false;
			if ( cm.discardDiffPctHighlighterOnAbsTable() ) return false;
			return cm.isPctColumn(adapter.convertColumnIndexToModel(adapter.column));
		}
	};
	private Highlighter[] _highliters = { 
			new HighlighterDiffData(_highligtIfDelta), 
			new HighlighterPctData(_highligtIfPct)
			// ,HighlighterFactory.createSimpleStriping()
		};

	private static class HighlighterDiffData extends AbstractHighlighter
	{
		public HighlighterDiffData(HighlightPredicate predicate)
		{
			super(predicate);
		}

		@Override
		protected Component doHighlight(Component comp, ComponentAdapter adapter)
		{
			Object value = adapter.getFilteredValueAt(adapter.row, adapter.convertColumnIndexToModel(adapter.column));
			if ( value instanceof Number )
			{
				comp.setForeground(Color.BLUE);
				if ( ((Number) value).doubleValue() != 0 )
				{
					comp.setFont(comp.getFont().deriveFont(Font.BOLD));
				}
			}
			return comp;
		}
	}

	private static class HighlighterPctData extends AbstractHighlighter
	{
		public HighlighterPctData(HighlightPredicate predicate)
		{
			super(predicate);
		}

		@Override
		protected Component doHighlight(Component comp, ComponentAdapter adapter)
		{
			Object value = adapter.getFilteredValueAt(adapter.row, adapter.convertColumnIndexToModel(adapter.column));
			if ( value instanceof Number )
			{
				comp.setForeground(Color.RED);
				if ( ((Number) value).doubleValue() != 0 )
				{
					comp.setFont(comp.getFont().deriveFont(Font.BOLD));
				}
			}
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
		_logger.debug(_displayName + ".setWatermarkText('" + str + "')");
		_watermark.setWatermarkText(str);
	}

	public void setWatermark()
	{
		if ( MainFrame.isOfflineConnected() )
		{
			PersistReader read = PersistReader.getInstance();
			if ( read == null )
			{
				setWatermarkText("No 'PersistReader' instance.");
			}
			else if ( _offlineCm == null )
			{
				if ( _offlineSampleTime == null )
					setWatermarkText("Choose a sample period from the\nOffline Sessions Viewer first.");
				else if ( _offlineSampleHasBeenRead )
					setWatermarkText("No offline data for this tab exists.");
				else
					setWatermarkText("Reading offline data...");
			}
			else
			{
				setWatermarkText("O f f l i n e  - D a t a");
			}
		}
		// NORMAL MODE
		else if ( _tailMode )
		{
			// set some informational fields before we establish a watermark
			_optionHasActiveGraphs_lbl.setVisible(_cm.hasActiveGraphs());
			_optionTrendGraphs_but    .setVisible(_cm.hasTrendGraph());

			// Set a watermark
			if ( !_cm.isConnected() )
			{
				setWatermarkText("Not Connected...");
			}
			else if ( _cm.isDataPollingPaused() )
			{
				setWatermarkText("Paused...");
			}
			else if ( !_cm.isActive() )
			{
				setWatermarkText(_cm.getProblemDesc());
			}
			else if ( _cm.getSampleException() != null )
			{
				// Make long string a little bit more readable split with
				// newline after '. ' and ': '
				setWatermarkText(_cm.getSampleException().toString().replaceFirst(": ", ": \n").replaceAll("\\. ", "\\. \n"));
				//_logger.info(_cm.getSampleException().toString(), _cm.getSampleException());
			}
			else if ( _cm.getTimeToNextPostponedRefresh() > 0 )
			{
				setWatermarkText("Postponing next sample refresh until '" + TimeUtils.msToTimeStr("%HH:%MM:%SS", _cm.getTimeToNextPostponedRefresh()) + "'.");
			}
			else if ( _cm.getDependantCmThatHasPostponeTime() != null )
			{
				CountersModel dcm = _cm.getDependantCmThatHasPostponeTime();
				setWatermarkText("Postponing next sample refresh until '" + TimeUtils.msToTimeStr("%HH:%MM:%SS", dcm.getTimeToNextPostponedRefresh()) + "'.\n" +
					"Waiting for dependant Performance Counter '"+dcm.getDisplayName()+"'.");
			}
			else if ( !_cm.hasAbsData() )
			{
				setWatermarkText("Waiting for first data sample...");
			}
			else if ( _cm.isDiffCalcEnabled() && !_cm.hasDiffData() )
			{
				setWatermarkText("Waiting for second sample, before DIFF and RATE can be calculated...");
			}
			else if ( _dataTable.getColumnCount() == 0 )
			{
				setWatermarkText("No columns in the table...");
			}
			else if ( _dataTable.getModel().getRowCount() == 0 )
			{
				setWatermarkText("No rows in the table model...");
			}
			else if ( _dataTable.getRowCount() == 0 )
			{
				setWatermarkText("No visible rows in the table... Is filtering on?");
			}
			else
			{
				setWatermarkText(null);
			}
		}
		else
		// Some READ MODE
		{
			if ( _cmDisplay == null )
			{
				setWatermarkText("No Stored data for the intervall was found.");
			}
			else if ( !_cmDisplay.hasDiffData() )
			{
				setWatermarkText("No DIFF and RATE data, this might be first sample?");
			}
			else if ( _dataTable.getModel().getRowCount() == 0 )
			{
				setWatermarkText("No rows in the table model...");
			}
			else if ( _dataTable.getRowCount() == 0 )
			{
				setWatermarkText("No visible rows in the table... Is filtering on?");
			}
			else
			{
				setWatermarkText(null);
			}
		}
		// FIXME: should this be here or somewhere else
		// paintTabHeader();
		paintTabHeader(null);
		
		// FIXME: maybe do this somewhere else as well
		// set how many rows we have in the table
		_counterRows_lbl.setText(_dataTable.getModel().getRowCount() + " / " + _dataTable.getRowCount());
	}

	private class Watermark extends AbstractComponentDecorator
	{
		public Watermark(JComponent target, String text)
		{
			super(target);
			if ( text == null )
				text = "";
			_textSave = text;
			_textBr   = text.split("\n");
		}

		private String		_restartText	= "Note: Restart "+Version.getAppName()+" after you have enabled the configuration.";
		private String[]	_textBr			= null; // Break Lines by '\n'
		private String      _textSave       = null; // Save last text so we dont need to do repaint if no changes.
		private Graphics2D	g				= null;
		private Rectangle	r				= null;

		@Override
		public void paint(Graphics graphics)
		{
			if ( _textBr == null || _textBr != null && _textBr.length < 0 )
				return;

			r = getDecorationBounds();
			g = (Graphics2D) graphics;
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Font f = g.getFont();
			g.setFont(f.deriveFont(Font.BOLD, f.getSize() * 2.0f));
			g.setColor(new Color(128, 128, 128, 128));

			FontMetrics fm = g.getFontMetrics();
			int maxStrWidth = 0;
			int maxStrHeight = fm.getHeight();

			// get max with for all of the lines
			for (int i = 0; i < _textBr.length; i++)
			{
				int CurLineStrWidth = fm.stringWidth(_textBr[i]);
				maxStrWidth = Math.max(maxStrWidth, CurLineStrWidth);
			}
			int xPos = (r.width - maxStrWidth) / 2;
			int yPos = (int) (r.height - ((r.height - fm.getHeight()) / 2) * 1.3);

			int spConfigureCount = 0;

			// Print all the lines
			for (int i = 0; i < _textBr.length; i++)
			{
				g.drawString(_textBr[i], xPos, (yPos + (maxStrHeight * i)));

				if ( _textBr[i].startsWith("sp_configure") )
					spConfigureCount++;
			}

			if ( spConfigureCount > 0 )
			{
				int yPosRestartText = yPos + (maxStrHeight * (_textBr.length + 1));
				g.drawString(_restartText, xPos, yPosRestartText);
			}
		}

		public void setWatermarkText(String text)
		{
			if ( text == null )
				text = "";

			// If text has NOT changed, no need to continue
			if (text.equals(_textSave))
				return;

			_textSave = text;

			_textBr = text.split("\n");
			_logger.debug("setWatermarkText: to '" + text + "'.");

			repaint();
		}
	}

	/*---------------------------------------------------
	 ** END: Watermark stuff
	 **---------------------------------------------------
	 */

	/*---------------------------------------------------
	 ** BEGIN: ShowPropertiesDialog
	 **---------------------------------------------------
	 */
	private class ShowPropertiesDialog extends JDialog implements ActionListener
	{
		private static final long	serialVersionUID	= 1L;
		private JButton				_ok_but				= new JButton("OK");

		private ShowPropertiesDialog(Frame owner)
		{
			super(owner);

			if ( _icon != null && _icon instanceof ImageIcon )
				((Frame) this.getOwner()).setIconImage(((ImageIcon) _icon).getImage());

			initComponents();

			// Set initial size
			// int width = (3 *
			// Toolkit.getDefaultToolkit().getScreenSize().width) / 4;
			// int height = (3 *
			// Toolkit.getDefaultToolkit().getScreenSize().height) / 4;
			// setSize(width, height);
			pack();

			Dimension size = getPreferredSize();
			size.width = 700;

			setPreferredSize(size);
			// setMinimumSize(size);
			setSize(size);

			setLocationRelativeTo(owner);

			setVisible(true);
		}

		protected void initComponents()
		{
			setTitle("Properties: " + _cm.getName());

			JPanel panel = new JPanel();
			panel.setLayout(new MigLayout("ins 0", "[fill]", ""));

			// JScrollPane scroll = new JScrollPane( init() );
			// scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

			panel.add(init(), "height 100%, wrap 15");
			panel.add(_ok_but, "tag ok, gapright 15, bottom, right, pushx, wrap 15");

			setContentPane(panel);

			// ADD ACTIONS TO COMPONENTS
			_ok_but.addActionListener(this);
		}

		protected JPanel init()
		{
			JPanel panel = new JPanel();
			panel.setLayout(new MigLayout("insets 20 20 20 20", "[grow]", ""));

			String sqlInit = _cm.getSqlInit();
			String sqlExec = _cm.getSql();
			String sqlWhere = _cm.getSqlWhere();
			String sqlClose = _cm.getSqlClose();

			if ( sqlInit != null )
				sqlInit = sqlInit.replaceAll("\\n", "<br>");
			if ( sqlExec != null )
				sqlExec = sqlExec.replaceAll("\\n", "<br>");
			if ( sqlWhere != null )
				sqlWhere = sqlWhere.replaceAll("\\n", "<br>");
			if ( sqlClose != null )
				sqlClose = sqlClose.replaceAll("\\n", "<br>");

			if ( sqlInit == null )
				sqlInit = "";
			if ( sqlExec == null )
				sqlExec = "";
			if ( sqlWhere == null )
				sqlWhere = "";
			if ( sqlClose == null )
				sqlClose = "";

			List<String> pkList = _cm.getPk();
			String[] diffCols = _cm.getDiffColumns();
			String[] pctCols = _cm.getPctColumns();

			// Lets COLOR code some stuff
			if ( pkList != null )
				for (String col : pkList)
					sqlExec = sqlExec.replaceFirst(col, "<b>" + col + "</b>");

			// BLUE: <font color="#0000FF">True</font>
			for (String col : diffCols)
				sqlExec = sqlExec.replaceFirst(col, "<font color=\"#0000FF\">" + col + "</font>");

			// RED: <font color="#FF0000">True</font>
			for (String col : pctCols)
				sqlExec = sqlExec.replaceFirst(col, "<font color=\"#FF0000\">" + col + "</font>");

			String str = "<html>" + "<HEAD> " + "<style type=\"text/css\"> " + "<!-- " + "body {font-family: Arial, Helvetica, sans-serif;} " + "--> " + "</style> " + "</HEAD> " +

			"<H1>" + _cm.getName() + "</H1>" +

			"<H2>SQL Init</H2>" + sqlInit + "<H2>SQL</H2>" + sqlExec + "<br><b>Color Code Explanation:</b> <br>" + "- Primary Key Columns in <b>BOLD</b><br>" + "- Diff Columns in <font color=\"#0000FF\">BLUE</font><br>" + "- Pct Columns in <font color=\"#FF0000\">RED</font>)" + "<H2>Extra Where clauses</H2>" + sqlWhere + "<H2>SQL Close</H2>" + sqlClose +

			"<H2>Primary Key Columns</H2>" + pkList +

			"<H2>Diff Columns</H2>" + Arrays.deepToString(diffCols) +

			"<H2>Pct Columns</H2>" + Arrays.deepToString(pctCols) +

			"</html>";

			JEditorPane feedback = new JEditorPane("text/html", str);
			feedback.setEditable(false);
			feedback.setOpaque(false);

			panel.add(feedback, "wrap 20");

			return panel;
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			if ( _ok_but.equals(e.getSource()) )
			{
				dispose();
			}
		}
	}

	/*---------------------------------------------------
	 ** END: ShowPropertiesDialog
	 **---------------------------------------------------
	 */

	/**
	 * Method is called by the <code>MainFrame.stateChanged(ChangeEvent)</code>
	 * whenever the tab is selected.
	 * <p>
	 * Used to do deffered read from the offlina and in-memory storage.<br>
	 * This means that we will read values from the storage when we click on tabs.
	 */
	public void tabSelected()
	{
		if ( MainFrame.isOfflineConnected() )
		{
			// If data hasn't been read from the Offline storage, do so now...
			if ( !_offlineSampleHasBeenRead )
			{
//				readOfflineSample();
				readOfflineSample_withProgressDialog();
			}
		}
		else
		{
			// read from previous InMemory counters or are we viewing last data
			if ( _tailMode )
			{
			}
			else
			{
				// If data hasn't been read from the in-memory storage, do so now...
				if ( !_inMemHistSampleHasBeenRead )
				{
					readInMemHistSample();
				}
			}
		}
	}

	/*---------------------------------------------------
	 ** BEGIN: inmemory/history methods
	 **---------------------------------------------------
	 */
	/** */
	private CountersModel _inMemHistCm = null;

	/** Read this timestamp when the tab read is deferred, for example by 'tab' activation */
	private Timestamp	_inMemHistSampleTime = null;

	/** Flag to indicate that a deferred action to read from In-Memory History should be done */
	private boolean		_inMemHistSampleHasBeenRead	= false;

	/** Timer used by the InMemory reader watermark during read from InMemory History Storage */
	private Timer		_inMemHistRefreshTimer = new Timer(200, new InMemHistRefreshTimerAction());

	/**
	 * Set a inMemeHist timestamp to be read from the In-Memory History storage<br>
	 * The read is deferred to a later stage, for example when the user activates the GUI 'tab'
	 * 
	 * @param ts
	 */
	public void setInMemHistSampleTime(CountersModel cm, Timestamp ts)
	{
		_inMemHistSampleTime = ts;
		_inMemHistSampleHasBeenRead = false;

		// Check if we are having a CM in the in-memory storage
		// this is needed, to display the vertical bar on the tab (tab indicator, for "has" counter)
		if (cm != null)
		{
			_inMemHistCm = cm;
		}
		else
		{
			InMemoryCounterHandler imch = InMemoryCounterHandler.getInstance();
			if ( imch == null )
				return;
			_inMemHistCm = imch.getCmForSample(this.getName(), _inMemHistSampleTime);
		}
	}

	/**
	 * Read a timestamp from the In-Memory History storage
	 * <p>
	 * The timestamp to read has previously been set by
	 * <code>setInMemHistSampleTime(Timestamp)</code>
	 */
	public void readInMemHistSample()
	{
		InMemoryCounterHandler imch = InMemoryCounterHandler.getInstance();
		if ( imch == null )
			return;

		if ( _inMemHistSampleTime == null )
		{
			_logger.warn("In Memory History Sample Time has not been set...");
			return;
		}

		// set watermark for local tab... read in progress
		// Start the timer which will be kicked of after X ms
		// This so we can do something if the refresh takes to long time
		_inMemHistRefreshTimer.start();

		// update current, display with the one selected
//		_inMemHistCm = imch.getCmForSample(this.getName(), _inMemHistSampleTime);
		setDisplayCm(_inMemHistCm, false);

		// No CM was available for this sample
		if ( _inMemHistCm == null )
		{
			// do anything here? ... most of the stuff is done in setDisplayCm()
			_logger.debug("readInMemHistSample('"+_inMemHistSampleTime+"'): cm=null");
		}

		// Mark that we have already read data from in-memory storage
		// no need to re-read the data from storage
		_inMemHistSampleHasBeenRead = true;

		// Stop the timer.
		_inMemHistRefreshTimer.stop();

		// Refresh the watermark
		setWatermark();
	}

	/**
	 * This timer is started just before we get offline data And it's stopped
	 * when the execution is finnished If X ms has elipsed in the database...
	 * show some info to any GUI that we are still in refresh...
	 */
	private class InMemHistRefreshTimerAction implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent actionevent)
		{
			// maybe use property change listeners instead:
			// firePropChanged("status", "refreshing");
			setWatermarkText("Getting In-Memory data...");
		}
	}
	/*---------------------------------------------------
	 ** END: inmemory/history methods
	 **---------------------------------------------------
	 */

	/*---------------------------------------------------
	 ** BEGIN: off-line methods
	 **---------------------------------------------------
	 */
	/** */
	private CountersModel _offlineCm = null;

	/**Read this timestamp when the tab offline read is deferred, for example by 'tab' activation */
	private Timestamp	_offlineSampleTime = null;

	/** Flag to indicate that a deferred action to read from Persistent Storage should be done */
	private boolean		_offlineSampleHasBeenRead = false;

	/** Timer used by the offline reader watermark during read from Persistent Storage */
	private Timer		_offlineRefreshTimer = new Timer(200, new OfflineRefreshTimerAction());

	/**
	 * Set a offline timestamp to be read from the Persistent Storage database<br>
	 * The read is deferred to a later stage, for example when the user
	 * activates the GUI 'tab'
	 * 
	 * @param ts
	 */
	public void setOfflineSampleTime(Timestamp ts)
	{
		_offlineSampleTime = ts;
		_offlineSampleHasBeenRead = false;
	}

	/**
	 * Read a timestamp from the Persistent Storage database
	 * <p>
	 * The timestamp to read has previously been set by
	 * <code>setOfflineSampleTime(Timestamp)</code>
	 */
//	public void readOfflineSample_THIS_DOES_NOT_WORK()
	public void readOfflineSample_withProgressDialog()
	{
		final String name = this.getName();
		final PersistReader read = PersistReader.getInstance();
		if ( read == null )
			return;

		if ( _offlineSampleTime == null )
		{
			_logger.warn("Offline Sample Time has not been set...");
			return;
		}

		// set watermark for local tab... read in progress
		// Start the timer which will be kicked of after X ms
		// This so we can do something if the refresh takes to long time
//		_offlineRefreshTimer.start();
//		setWatermarkText("Getting offline data...");

		SwingWorker<String, Object> readOfflineThread = new SwingWorker<String, Object>()
		{
			@Override
			protected String doInBackground() throws Exception
			{
				// Go and get the data
				_offlineCm = read.getCmForSample(name, _offlineSampleTime);
				return _offlineCm == null ? "FAILED" : "SUCCEED";
			}

		};
		JDialog dialog = new JDialog((Frame)null, "Waiting for offline read...", true);
		JLabel label = new JLabel("Reading data from offline storage", JLabel.CENTER);
		label.setFont(new java.awt.Font(Font.DIALOG, Font.BOLD, 16));
		dialog.add(label);
		dialog.pack();
		dialog.setSize( dialog.getSize().width + 100, dialog.getSize().height + 70);
		dialog.setLocationRelativeTo(this);
		readOfflineThread.addPropertyChangeListener(new SwingWorkerCompletionWaiter(dialog));

//		MainFrame.getInstance().setBusyCursor(true);

		readOfflineThread.execute();
		//the dialog will be visible until the SwingWorker is done
		dialog.setVisible(true); 

		System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
//		MainFrame.getInstance().setBusyCursor(false);

		setDisplayCm(_offlineCm, false);

		// No CM was available for this sample
		if ( _offlineCm == null )
		{
			// do anything here? ... most of the stuff is done in setDisplayCm()
			_logger.debug("readOfflineSample('"+_offlineSampleTime+"'): _cmOffline=null");
		}

		// Mark that we have already read data from offline storage
		// no need to reread the data from offline storage
		_offlineSampleHasBeenRead = true;

		// Stop the timer.
//		_offlineRefreshTimer.stop();

		// Refresh the watermark
		setWatermark();
	}
	private class SwingWorkerCompletionWaiter implements PropertyChangeListener
	{
		private JDialog _dialog;
	 
		public SwingWorkerCompletionWaiter(JDialog dialog) 
		{
			_dialog = dialog;
		}
	 
		public void propertyChange(PropertyChangeEvent event) 
		{
			if ("state".equals(event.getPropertyName()) && SwingWorker.StateValue.DONE == event.getNewValue()) 
			{
				_dialog.setVisible(false);
				_dialog.dispose();
			}
		}
	}

	/**
	 * Read a timestamp from the Persistent Storage database
	 * <p>
	 * The timestamp to read has previously been set by
	 * <code>setOfflineSampleTime(Timestamp)</code>
	 */
	public void readOfflineSample()
	{
		PersistReader read = PersistReader.getInstance();
		if ( read == null )
			return;

		if ( _offlineSampleTime == null )
		{
			_logger.warn("Offline Sample Time has not been set...");
			return;
		}

		// set watermark for local tab... read in progress
		// Start the timer which will be kicked of after X ms
		// This so we can do something if the refresh takes to long time
		try
		{
	//		_offlineRefreshTimer.start();
			setWatermarkText("Getting offline data...");
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	
			// Go and get the data
			_offlineCm = read.getCmForSample(this.getName(), _offlineSampleTime);
			setDisplayCm(_offlineCm, false);
	
			// No CM was available for this sample
			if ( _offlineCm == null )
			{
				// do anything here? ... most of the stuff is done in setDisplayCm()
				_logger.debug("readOfflineSample('"+_offlineSampleTime+"'): _cmOffline=null");
			}
	
			// Mark that we have already read data from offline storage
			// no need to reread the data from offline storage
			_offlineSampleHasBeenRead = true;
	
			// Stop the timer.
	//		_offlineRefreshTimer.stop();
		}
		finally
		{
			// Refresh the watermark
			setWatermark();
			setCursor(Cursor.getDefaultCursor());
		}
	}

	/**
	 * This timer is started just before we get offline data And it's stopped
	 * when the execution is finnished If X ms has elipsed in the database...
	 * show some info to any GUI that we are still in refresh...
	 */
	private class OfflineRefreshTimerAction implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent actionevent)
		{
			// maybe use property change listeners instead:
			// firePropChanged("status", "refreshing");
			setWatermarkText("Getting offline data...");
//System.out.println(getName()+": Getting offline data...");
		}
	}

	/*---------------------------------------------------
	 ** END: off-line methods
	 **---------------------------------------------------
	 */

	/*---------------------------------------------------
	 ** BEGIN: special painting code
	 **---------------------------------------------------
	 */
	// public void paintComponent(Graphics g)
	// {
	// super.paintComponent(g);
	// paintTabHeader();
	// }

	// private JTabbedPane _tabPane = null;
	// public void paintTabHeader()
	// {
	// System.out.println("paintTabHeader() for '"+_displayName+"'.");
	// if ( ! _cm.isOfflineConnected() )
	// return;
	// if (_tabPane == null)
	// {
	// for (Container c=getParent(); c!=null; c=getParent())
	// {
	// if (c instanceof JTabbedPane)
	// {
	// _tabPane = (JTabbedPane) c;
	// break;
	// }
	// }
	// }
	// if (_tabPane != null)
	// {
	// int tabCount = _tabPane.getTabCount();
	// for (int t=0; t<tabCount; t++)
	// {
	// if (_tabPane.getComponentAt(t).equals(this))
	// {
	// Rectangle r = _tabPane.getUI().getTabBounds(_tabPane, t);
	// Graphics g = _tabPane.getGraphics().create(r.x, r.y, r.width, r.height);
	// System.out.println("paintTabHeader().tab("+t+").getTabBounds(xxx,"+t+"): Rectangle="+r+", g="+g);
	//	
	// paintTabHeader((Graphics2D)g);
	// }
	//
	// // try {Thread.sleep(10);}
	// // catch (InterruptedException ignore) {}
	// }
	// }
	// }

	/**
	 * Paint some special stuff on the tab header.
	 * 
	 * @param g    the Graphics2D would be set to just cover the tab header
	 */
	@Override
	public void paintTabHeader(Graphics2D g)
	{
		// Rectangle r = g.getClipBounds();

		// Write at position
		// int pX = r.width - 4;
		// int pY = 3;
		// int pW = 2;
		// int pH = r.height - 7;

		// pX = r.x;
		// pY = r.y;
		// pW = r.width;
		// pH = r.height;

		// well if we do not have an Icon attached here, no need to continue
		if ( _icon == null )
			return;

		if ( hasValidSampleData() )
		{
			// g.setColor(Color.GREEN);
			// g.fillRect(pX, pY, pW, pH);

			// Swap icon to one indicating there is data available

			// URL url1;
			BufferedImage im;
			Graphics2D img;

			if ( _indicatorIcon == null )
			{
				im = new BufferedImage(_icon.getIconWidth() + 2, _icon.getIconHeight(), BufferedImage.TRANSLUCENT);
				img = im.createGraphics();
				img.setColor(Color.GREEN);

				if ( _indicatorToLeft )
				{
					_icon.paintIcon(null, img, 2, 0);
					img.fillRect(0, 0, 2, _icon.getIconHeight());
				}
				else
				{
					_icon.paintIcon(null, img, 0, 0);
					img.fillRect(_icon.getIconWidth(), 0, 2, _icon.getIconHeight());
				}

				_indicatorIcon = new ImageIcon(im);
			}
			JTabbedPane jtp = MainFrame.getTabbedPane();
			int jtpIndex = jtp.indexOfTab(_icon);
			if ( jtpIndex != -1 )
			{
				jtp.setIconAt(jtpIndex, _indicatorIcon);
			}
		}
		else
		{
			// This only sets the area to the background color, not causing the
			// region to repaint
			// I guess this would be enough for now...
			// g.clearRect(pX, pY, pW, pH);

			// Swap icon back to original
			JTabbedPane jtp = MainFrame.getTabbedPane();
			int jtpIndex = jtp.indexOfTab(_indicatorIcon);
			if ( jtpIndex != -1 )
			{
				jtp.setIconAt(jtpIndex, _icon);
			}
		}
	}

	/*---------------------------------------------------
	 ** END: special painting code
	 **---------------------------------------------------
	 */
	/**
	 * this should return true if the current sample has any data to show
	 */
	public boolean hasValidSampleData()
	{
		if ( MainFrame.isOfflineConnected() )
		{
			PersistReader reader = PersistReader.getInstance();
			if ( reader == null )
				return false;

			// Go and check if the "map" for this sample has data...
			return reader.hasCountersForCm(_cm.getName());
		}
		else
		{
			// so we need to be in online mode...
			if ( _tailMode )
				return _cm.hasValidSampleData();

			// Viewing in-memory history.
			// FIXME: need t change stuff here for "deffered" read when tabIsActiveted, 
			//        then we need som kind of indicator
			if (_inMemHistCm != null && _inMemHistCm.hasValidSampleData() )
				return true;

//			if ( _cmDisplay != null && _cmDisplay.hasValidSampleData() )
//				return true;

			return false;
		}
	}

	/**
	 * Used to control/check/set local components that has been created 
	 * during the TabularControlPanel in GetCounters.createCounters()
	 * <p>
	 * This method should be overriden by the TabularControlPanel in GetCounters.createCounters()
	 */
	public void checkLocalComponents()
	{
	}

	/**
	 * an attempt to do sorting on the table...
	 */
	public void sortDatatable()
	{
		RowSorter<?> rowSorter = _dataTable.getRowSorter();
		if (rowSorter instanceof DefaultRowSorter)
		{
			DefaultRowSorter<?, ?> drs = (DefaultRowSorter<?, ?>) rowSorter;
			drs.sort();
		}
		else
			rowSorter.allRowsChanged();
		
	}
}
