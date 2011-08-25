/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.Filter;
import org.jdesktop.swingx.decorator.FilterPipeline;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.Highlighter;

import asemon.gui.swing.FilterValueAndLogLevel;
import asemon.gui.swing.JColorComboBox;
import asemon.utils.SwingUtils;


public class Log4jViewer
extends JFrame
implements ActionListener, TableModelListener
{
	private static Logger _logger = Logger.getLogger(Log4jViewer.class);
	private static final long serialVersionUID = 4578428349487389745L;

	private static final Color LOG_LEVEL_COLOR_FATAL = Color.BLUE;
	private static final Color LOG_LEVEL_COLOR_ERROR = Color.RED;
	private static final Color LOG_LEVEL_COLOR_WARN  = new Color(255,204,0);  // Color.YELLOW;
	private static final Color LOG_LEVEL_COLOR_INFO  = Color.BLACK;
	private static final Color LOG_LEVEL_COLOR_DEBUG = Color.GRAY;
	private static final Color LOG_LEVEL_COLOR_TRACE = Color.LIGHT_GRAY;

	private JFrame    _owner     = null;


	//-------------------------------------------------
	// Filter Panel
	private JPanel         _filterPanel;
	private JLabel         _filterLevel_lbl        = new JLabel("Show Level");
	private JRadioButton   _filterTrace_rb         = new JRadioButton("Trace",   true);
	private JRadioButton   _filterDebug_rb         = new JRadioButton("Debug",   true);
	private JRadioButton   _filterInfo_rb          = new JRadioButton("Info",    true);
	private JRadioButton   _filterWarn_rb          = new JRadioButton("Warning", true);
	private JRadioButton   _filterError_rb         = new JRadioButton("Error",   true);
	private JRadioButton   _filterFatal_rb         = new JRadioButton("Fatal",   true);

	private JLabel         _filterThreadName_lbl   = new JLabel("Thread Name");
	private JTextField     _filterThreadName_tf    = new JTextField();
	private JLabel         _filterClassName_lbl    = new JLabel("Class Name");
	private JTextField     _filterClassName_tf     = new JTextField();
	private JLabel         _filterMessage_lbl      = new JLabel("Message");
	private JTextField     _filterMessage_tf       = new JTextField();

	private JCheckBox      _filterUseFilter_cb     = new JCheckBox("Filters are used", true);

	//-------------------------------------------------
	// OPTIONS panel
	private JPanel             _optionsPanel;
	private JCheckBox          _optionTail_cb      = new JCheckBox("Move to last log entry when new are entered.", true);

	private LogLevelDialog     _logLevelDialog     = null;
	private JButton            _logLevel_but       = new JButton("Set log level");
	private JButton            _clearAll_but       = new JButton("Clear all log records");
	private JButton            _test_but           = new JButton("Log Test records");
	private JLabel             _maxLogRecords_lbl  = new JLabel("Max log records");
	private JSpinner           _maxLogRecords_sp   = null;
	private SpinnerNumberModel _maxLogRecords_spm  = null;
	
	//-------------------------------------------------
	// DATA TABLE panel
	private Log4jTableModel    _log4jTableModel    = null;
	private JXTable            _dataTable          = null;
	private JPopupMenu         _tablePopupMenu     = null;

	// Data table filters
	private FilterValueAndLogLevel  _tableValAndLogLevelFilter = null;
	private FilterPipeline          _tablePipelineFilter       = null;
	private boolean                 _tableFilterIsSet          = false;

	//-------------------------------------------------

	
	
	
	/*---------------------------------------------------
	** BEGIN: constructors
	**---------------------------------------------------
	*/
	
	public Log4jViewer(JFrame owner)
	{
		_owner     = owner;

		// Initialize various table filters
		_tableValAndLogLevelFilter = new FilterValueAndLogLevel();
		_tablePipelineFilter      = new FilterPipeline( new Filter[] { _tableValAndLogLevelFilter } );

		_log4jTableModel = GuiLogAppender.getTableModel();
		initComponents();

//		this.pack();
		SwingUtils.setLocationNearTopLeft(_owner, this);
	}
	/*---------------------------------------------------
	** END: constructors
	**---------------------------------------------------
	*/

	/** actions when the window becomes visible on the screen */
	public void setVisible(boolean b)
	{
		_test_but.setVisible(false);
		if (b)
		{
			_dataTable.packAll();
			if (_logger.isDebugEnabled())
				_test_but.setVisible(true);
		}
		super.setVisible(b);
	}

	
	/*---------------------------------------------------
	** BEGIN: component initialization
	**---------------------------------------------------
	*/
	protected void initComponents() 
	{
		if (_owner != null)
			setIconImage(_owner.getIconImage());
		
		setTitle("Log4j - Log and Trace view");

		JPanel panel = new JPanel();
//		panel.setLayout(new MigLayout("debug","",""));
		panel.setLayout(new BorderLayout());

//		panel.add(createTopPanel(),   "wrap");
//		panel.add(createTablePanel(), "wrap");
//		panel.add(createTablePanel(), "width 100%, height 100%, wrap");

		panel.add(createTopPanel(),   BorderLayout.NORTH);
		panel.add(createTablePanel(), BorderLayout.CENTER);

		// Set initial size
		int width  = (3 * Toolkit.getDefaultToolkit().getScreenSize().width)  / 4;
		int height = (3 * Toolkit.getDefaultToolkit().getScreenSize().height) / 4;
		setSize(width, height);

		setContentPane(panel);
		
		initComponentActions();
	}
	
	private JPanel createTopPanel() 
	{
		JPanel panel = SwingUtils.createPanel("Top", false);
		panel.setLayout(new MigLayout("ins 3 10 3 3", //ins T L B R???
				"",
				""));

		_filterPanel   = createFilterPanel();
		_optionsPanel  = createOptionsPanel();

		panel.add(_filterPanel,   "top, grow");
		panel.add(_optionsPanel,  "top, grow");

		return panel;
	}

	private JPanel createFilterPanel() 
	{
		JPanel panel = SwingUtils.createPanel("Filter", true);
//		panel.setLayout(new MigLayout("ins 0", "[grow]", ""));
		panel.setLayout(new MigLayout("ins 0 5 0 5", "", "")); // ins Top Left Bottom Right

		_filterLevel_lbl     .setToolTipText("Show following errorlevels in the table.");
		_filterFatal_rb      .setToolTipText("Show FATAL messages in the table.");
		_filterError_rb      .setToolTipText("Show ERROR messages in the table.");
		_filterWarn_rb       .setToolTipText("Show WARNING messages in the table.");
		_filterInfo_rb       .setToolTipText("Show INFORMATIONAL messages in the table.");
		_filterDebug_rb      .setToolTipText("Show DEBUG messages in the table.");
		_filterTrace_rb      .setToolTipText("Show TRACE messages in the table.");
		
		_filterThreadName_lbl.setToolTipText("Show only these thread names in the tabel (regexp can be used)");
		_filterThreadName_tf .setToolTipText("Show only these thread names in the tabel (regexp can be used)");
		
		_filterClassName_lbl .setToolTipText("Show only these class names in the tabel (regexp can be used)");
		_filterClassName_tf  .setToolTipText("Show only these class names in the tabel (regexp can be used)");
		
		_filterMessage_lbl   .setToolTipText("Show only these messages in the tabel (regexp can be used)");
		_filterMessage_tf    .setToolTipText("Show only these messages in the tabel (regexp can be used)");
		
		panel.add(_filterUseFilter_cb, "span");
		panel.add(_filterLevel_lbl,"");
		panel.add(_filterFatal_rb, "split 99");
		panel.add(_filterError_rb, "");
		panel.add(_filterWarn_rb,  "");
		panel.add(_filterInfo_rb,  "");
		panel.add(_filterDebug_rb, "");
		panel.add(_filterTrace_rb, "wrap");

		panel.add(_filterThreadName_lbl, "");
		panel.add(_filterThreadName_tf,  "grow, wrap");
		
		panel.add(_filterClassName_lbl, "");
		panel.add(_filterClassName_tf,  "grow, wrap");
		
		panel.add(_filterMessage_lbl, "");
		panel.add(_filterMessage_tf,  "grow, wrap");
		
		panel.add(_filterUseFilter_cb, "span");
		
		return panel;		
	}

	private JPanel createOptionsPanel() 
	{
		JPanel panel = SwingUtils.createPanel("Options", true);
//		panel.setLayout(new MigLayout("ins 0", "", "0[0]0"));
		panel.setLayout(new MigLayout("ins 0 5 0 5", "", "")); // ins Top Left Bottom Right

		_maxLogRecords_spm = new SpinnerNumberModel(_log4jTableModel.getMaxRecords(), 1, Integer.MAX_VALUE, 50);
		_maxLogRecords_sp  = new JSpinner(_maxLogRecords_spm);

		_optionTail_cb    .setToolTipText("Move to the last record in the table when new log records are added.");

		_maxLogRecords_lbl.setToolTipText("How many record should the table hold.");
		_maxLogRecords_sp .setToolTipText("How many record should the table hold.");

		_test_but         .setToolTipText("Generate one log record for each log level. This Button will only be visible when we are in log level DEBUG.");
		_logLevel_but     .setToolTipText("Open a dialog where you can change log level for classes that has registered themself with log4j.");
		_clearAll_but     .setToolTipText("Clear all records from the table.");

		
		panel.add(_optionTail_cb, "wrap");

		panel.add(_maxLogRecords_lbl, "split");
		panel.add(_maxLogRecords_sp,  "wrap");

		panel.add(_test_but, "wrap");
		panel.add(_logLevel_but,  "bottom, left, push, split");
		panel.add(_clearAll_but,  "bottom, right, push, wrap");
		

		return panel;
	}
	
	
	private JPanel createTablePanel() 
	{
		JPanel panel = SwingUtils.createPanel("Actual Data Table", false);
//		panel.setLayout(new MigLayout("debug, ins 0", 
//				"", 
//				"0[0]0"));
		panel.setLayout(new BorderLayout()); 

		_log4jTableModel.addTableModelListener(this);

		// Extend the JXTable to get tooptip stuff
		_dataTable = new JXTable()
		{
	        private static final long serialVersionUID = 0L;

			public String getToolTipText(MouseEvent e) 
			{
				String tip = null;
				Point p = e.getPoint();
				int row = rowAtPoint(p);
				if (row > 0)
				{
					row = super.convertRowIndexToModel(row);

					TableModel model = getModel();
					if (model instanceof Log4jTableModel)
					{
						Log4jLogRecord l = ((Log4jTableModel)model).getRecord(row);
						tip = l.getToolTipText();
					}
				}
				return tip;
			}
		};

		_dataTable.setModel(GuiLogAppender.getTableModel());
		_dataTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		_dataTable.packAll(); // set size so that all content in all cells are visible
		_dataTable.setSortable(true);
		_dataTable.setColumnControlVisible(true);
		_dataTable.setHighlighters(_highlitersLogLevelAtColId_2); // a variant of cell render

		_tablePopupMenu = createDataTablePopupMenu();
		_dataTable.setComponentPopupMenu(_tablePopupMenu);

		JScrollPane scroll = new JScrollPane(_dataTable);

		panel.add(scroll, BorderLayout.CENTER);
//		panel.add(scroll, "width 100%, height 100%");
		return panel;
	}
	
	

	/*---------------------------------------------------
	** BEGIN: PopupMenu on the table
	**---------------------------------------------------
	*/
	/** Get the JMeny attached to the GTabbedPane */
	public JPopupMenu getDataTablePopupMenu()
	{
		return _tablePopupMenu;
	}

	/** 
	 * Creates the JMenu on the Component, this can be overrided by a subclass.<p>
	 * If you want to add stuff to the menu, its better to use 
	 * getTabPopupMenu(), then add entries to the menu. This is much 
	 * better than subclass the GTabbedPane
	 */
	public JPopupMenu createDataTablePopupMenu()
	{
		return null;
	}

	/*---------------------------------------------------
	** END: PopupMenu on the table
	**---------------------------------------------------
	*/

	
	
	/*---------------------------------------------------
	** BEGIN: implementing TableModelListener, and helper methods for it
	**---------------------------------------------------
	*/
	/** Called when the TableModel itself changes */
	public void tableChanged(TableModelEvent e)
	{
//		TableModel tm = (TableModel) e.getSource();
//		int type      = e.getType();
//		int column    = e.getColumn();
//		int firstRow  = e.getFirstRow();
//		int lastRow   = e.getLastRow();
//		// NOTE: you can NOT do _logger.xxxx call here, that leads to 
//		//       recursive calls to this method (tableChanged)  
//		System.out.println("TableModelEvent: type="+type+", column="+column+", firstRow="+firstRow+", lastRow="+lastRow);
	
		if (_logger.isDebugEnabled())
			_test_but.setVisible(true);

		if (_optionTail_cb.isSelected())
		{
			showLastTableRow();
		}
	}

	/** go to the last record of the log table and show it in the viewport */
	public void showLastTableRow()
	{
		// "lookFor column before 'Message', and use that col"
		// if not found use column 0
		int colId = _log4jTableModel.findColumn("Message");
		colId = (colId > 0) ? colId -1 : 0;

		showCell(_dataTable.getRowCount()-1, colId, true);
	}

	/** go to the a specific row and column of the log table and show it in the viewport */
	public void showCell(int row, int column, boolean selectTheRow)
	{
		if (row < 0 || column < 0)
			return;

		Rectangle rect = _dataTable.getCellRect(row, column, true);
		_dataTable.scrollRectToVisible(rect);
		
		if (selectTheRow)
		{
			_dataTable.clearSelection();
			_dataTable.setRowSelectionInterval(row, row);
		}
	}
	/*---------------------------------------------------
	** END: implementing TableModelListener
	**---------------------------------------------------
	*/
	
	/*---------------------------------------------------
	** BEGIN: Action Listeners, and helper methods for it
	**---------------------------------------------------
	*/
	private void initComponentActions()
	{
		//---- FILTER PANEL -----
		_filterFatal_rb     .addActionListener(this);
		_filterError_rb     .addActionListener(this);
		_filterWarn_rb      .addActionListener(this);
		_filterInfo_rb      .addActionListener(this);
		_filterDebug_rb     .addActionListener(this);
		_filterTrace_rb     .addActionListener(this);

		_filterThreadName_tf.addActionListener(this);
		_filterClassName_tf .addActionListener(this);		
		_filterMessage_tf   .addActionListener(this);

		_filterUseFilter_cb .addActionListener(this);

		//---- OPTIONS PANEL -----
		_optionTail_cb      .addActionListener(this);

		_test_but           .addActionListener(this);
		_logLevel_but       .addActionListener(this);
		_clearAll_but       .addActionListener(this);
		
		_maxLogRecords_sp.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent ce)
			{
				int records = _maxLogRecords_spm.getNumber().intValue();
				_log4jTableModel.setMaxRecords(records);
			}
		});
	}

	public void actionPerformed(ActionEvent e)
    {
		Object source = e.getSource();

		// FILTERS
		if (_filterFatal_rb.equals(source)) setFilter();
		if (_filterError_rb.equals(source)) setFilter();
		if (_filterWarn_rb .equals(source)) setFilter();
		if (_filterInfo_rb .equals(source)) setFilter();
		if (_filterDebug_rb.equals(source)) setFilter();
		if (_filterTrace_rb.equals(source)) setFilter();

		if (_filterThreadName_tf.equals(source)) setFilter();
		if (_filterClassName_tf .equals(source)) setFilter();
		if (_filterMessage_tf   .equals(source)) setFilter();

		if (_filterUseFilter_cb .equals(source)) setFilter();

		// CHECKBOX: TAIL
		if (_optionTail_cb.equals(source))
		{
			_logger.debug("CHECKBOX: TAIL = "+_optionTail_cb.isSelected());
			if (_log4jTableModel != null)
				_log4jTableModel.fireTableDataChanged();
		}

		// BUTTON: LOG-LEVEL
		if (_logLevel_but.equals(source))
		{
			if (_logLevelDialog == null)
				_logLevelDialog = new LogLevelDialog(Log4jViewer.this);
			_logLevelDialog.setVisible(true);
		}

		// BUTTON: CLEAR-ALL
		if (_clearAll_but.equals(source))
		{
			GuiLogAppender.getTableModel().clear();
		}

		// BUTTON: TEST
		if (_test_but.equals(source))
		{
			_logger.fatal("FATAL TEST message with a stacktrace.", new Exception("FATAL TEST"));
			_logger.error("ERROR TEST message with a stacktrace.", new Exception("ERROR TEST"));
			_logger.warn ("WARN  TEST message.");
			_logger.info ("INFO  TEST message.");
			_logger.debug("DEBUG TEST message.");
			_logger.trace("TRACE TEST message.");
		}
    }

	private void setFilter()
	{
		_logger.trace("setFilter() was called");
		_logger.debug("CHECKBOX: USE-FILTER = "+_filterUseFilter_cb.isSelected());
		if (_filterUseFilter_cb.isSelected() == false)
		{
			// FIXME: set the filter to OFF
			_tableValAndLogLevelFilter.resetFilter();
			return;
		}

		String threadName = _filterThreadName_tf.getText();
		String className  = _filterClassName_tf .getText();
		String message    = _filterMessage_tf   .getText();
		int    level      = 0;

		if (_filterFatal_rb.isSelected()) level = level | 1;
		if (_filterError_rb.isSelected()) level = level | 2;
		if (_filterWarn_rb .isSelected()) level = level | 4;
		if (_filterInfo_rb .isSelected()) level = level | 8;
		if (_filterDebug_rb.isSelected()) level = level | 16;
		if (_filterTrace_rb.isSelected()) level = level | 32;

		_logger.debug("FILTER: threadName='"+threadName+"', className='"+className+"', message="+message+".");

		if (_tableValAndLogLevelFilter == null || threadName == null || className == null || message == null)
			return;

		if ( ! _tableFilterIsSet )
		{
			_tableFilterIsSet = true;
			_logger.debug("No table filter was priviously set, so lets set it now.");
			_dataTable.setFilters(_tablePipelineFilter);
		}
		
		// set column position to the filter
		int levelColId      = _log4jTableModel.findColumn("Level");
		int threadNameColId = _log4jTableModel.findColumn("Thread Name");
		int classNameColId  = _log4jTableModel.findColumn("Class Name");
		int messageColId    = _log4jTableModel.findColumn("Message");
		_tableValAndLogLevelFilter.setFilterColId(levelColId, threadNameColId, classNameColId, messageColId);

		// set column values to the filter
		_tableValAndLogLevelFilter.setFilter(level, threadName, className, message);
	}
	/*---------------------------------------------------
	** END: Action Listeners
	**---------------------------------------------------
	*/


	
	/*---------------------------------------------------
	** BEGIN: Highlighter stuff for the JXTable
	**---------------------------------------------------
	*/
	private class LogLevelPredicate implements HighlightPredicate
	{
		int    _logLevelColId = -1;
		String _logLevelStr   = "";
		public LogLevelPredicate(int logLevelColId, String logLevelStr)
		{
			_logLevelColId = logLevelColId;
			_logLevelStr   = logLevelStr;
		}
		public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
		{
			Object value = adapter.getFilteredValueAt(adapter.row, _logLevelColId);
			if (value != null)
				value = value.toString();
			boolean ret = _logLevelStr.equals(value);
			//System.out.println("LogLevelPredicate: ret="+ret+", row="+adapter.row+", col="+adapter.column+", llcid="+_logLevelColId+", llStr='"+_logLevelStr+"', value='"+value+"', value.class='"+value.getClass().getName()+"'.");
			return ret;
		}
	};

	private Highlighter[] _highlitersLogLevelAtColId_1 = 
	{
			new HighlighterLogLevel(new LogLevelPredicate(1, "FATAL"), LOG_LEVEL_COLOR_FATAL),
			new HighlighterLogLevel(new LogLevelPredicate(1, "ERROR"), LOG_LEVEL_COLOR_ERROR),
			new HighlighterLogLevel(new LogLevelPredicate(1, "WARN"),  LOG_LEVEL_COLOR_WARN),
			new HighlighterLogLevel(new LogLevelPredicate(1, "INFO"),  LOG_LEVEL_COLOR_INFO),
			new HighlighterLogLevel(new LogLevelPredicate(1, "DEBUG"), LOG_LEVEL_COLOR_DEBUG),
			new HighlighterLogLevel(new LogLevelPredicate(1, "TRACE"), LOG_LEVEL_COLOR_TRACE)
	};

	private Highlighter[] _highlitersLogLevelAtColId_2 = 
	{
			new HighlighterLogLevel(new LogLevelPredicate(2, "FATAL"), LOG_LEVEL_COLOR_FATAL),
			new HighlighterLogLevel(new LogLevelPredicate(2, "ERROR"), LOG_LEVEL_COLOR_ERROR),
			new HighlighterLogLevel(new LogLevelPredicate(2, "WARN"),  LOG_LEVEL_COLOR_WARN),
			new HighlighterLogLevel(new LogLevelPredicate(2, "INFO"),  LOG_LEVEL_COLOR_INFO),
			new HighlighterLogLevel(new LogLevelPredicate(2, "DEBUG"), LOG_LEVEL_COLOR_DEBUG),
			new HighlighterLogLevel(new LogLevelPredicate(2, "TRACE"), LOG_LEVEL_COLOR_TRACE)
	};


	private static class HighlighterLogLevel 
	extends AbstractHighlighter 
	{ 
		Color _color = null;
		public HighlighterLogLevel(HighlightPredicate predicate, Color color) 
		{
			super(predicate);
			_color = color;
		}

		protected Component doHighlight(Component comp, ComponentAdapter adapter) 
		{
			comp.setForeground(_color);
			return comp;
		}
	}
	/*---------------------------------------------------
	** END: Highlighter stuff for the JXTable
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
	private class LogSorter
	implements Comparator
	{
		public int compare(Object o1, Object o2) 
		{
			return ((Logger)o1).getName().compareTo( ((Logger)o2).getName() );
		}
	}
	//---------------------------------------
	// The GUI that shows what Classes is added to LOG4J
	// And the ability to change log level for each lo4j logger class
	//---------------------------------------
	private class LogLevelDialog 
	extends JDialog
	implements ActionListener, TableModelListener
	{
		private static final long serialVersionUID = 8370470094367910481L;

		private JFrame             _owner          = null;
		
		private String             _logLevels[]    = { "FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE" };
		private Color        _logLevelsColors[]    = { LOG_LEVEL_COLOR_FATAL, LOG_LEVEL_COLOR_ERROR, LOG_LEVEL_COLOR_WARN, LOG_LEVEL_COLOR_INFO, LOG_LEVEL_COLOR_DEBUG, LOG_LEVEL_COLOR_TRACE };

		private LogLevelTableModel _tm             = new LogLevelTableModel();
		private JLabel             _allToLevel_lbl = new JLabel("Set level for all classes to");
		private JColorComboBox     _allToLevel_cb  = new JColorComboBox(JColorComboBox.TEXT_ONLY, _logLevelsColors, _logLevels);
//		private JComboBox          _allToLevel_cb  = new JComboBox(_logLevels);
		private JButton            _ok             = new JButton("OK");
		private JButton            _cancel         = new JButton("Cancel");
		private JButton            _apply          = new JButton("Apply");
		private JXTable            _dataTable      = new JXTable();

		private LogLevelDialog(JFrame frame)
		{
			super(frame);
			_owner = frame;

			refreshData();

			setContentPane(createTablePanel());

//			this.pack();
//			this.setLocationRelativeTo(frame);

			int width  = 400;
			int height = 600;
			setSize(width, height);
//			Dimension d = _dataTable.getPreferredSize();
//			setSize(d.width+27, d.height);
//			this.setLocationByPlatform(true);

			SwingUtils.setLocationNearTopLeft(_owner, this, 40, 40);

			this.setVisible(true);
		}

		public void setVisible(boolean b)
		{
			if (b)
			{
				refreshData();
			}
			super.setVisible(b);
		}
		
		private void refreshData()
		{
			_tm.clear();

			List logList = Collections.list(LogManager.getCurrentLoggers());
			Collections.sort( logList, new LogSorter() );
			Iterator iter = logList.iterator();
			while (iter.hasNext())
			{
				Logger logger = (Logger) iter.next();

				_tm.addRows(new Log4jClass(logger.getName(), logger.getEffectiveLevel()));
			}
			
			_dataTable.packAll();
			_apply.setEnabled(false);
		}

		private JPanel createTablePanel() 
		{
			// How do I set the icon?
//			if (_parentFrame != null)
//				_parentFrame.setIconImage(_frameIcon.getImage());
			
			setTitle("Log4j - Set log levels");
			
			JPanel panel = SwingUtils.createPanel("Actual Data Table", false);
			panel.setLayout(new MigLayout("ins 0", "", ""));
//			panel.setLayout(new BorderLayout()); 

			_tm.addTableModelListener(this);

			_dataTable.setModel(_tm);
//			_dataTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//			_dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			_dataTable.packAll(); // set size so that all content in all cells are visible
			_dataTable.setSortable(true);
			_dataTable.setColumnControlVisible(true);
			_dataTable.setHighlighters(_highlitersLogLevelAtColId_1); // a variant of cell render

//			_dataTable.setDefaultEditor(  Log4jClass.class, new DefaultCellEditor(new JComboBox(_logLevels)));
			_dataTable.setDefaultEditor(  Log4jClass.class, new DefaultCellEditor(new JColorComboBox(JColorComboBox.TEXT_ONLY, _logLevelsColors, _logLevels)));
			_dataTable.setDefaultRenderer(Log4jClass.class, new DefaultTableCellRenderer());

			JScrollPane scroll = new JScrollPane(_dataTable);

			panel.add(scroll, "grow, push, height 100%, wrap 15");

			panel.add(_allToLevel_lbl  , "align right, split");
			panel.add(_allToLevel_cb   , "gapright 15, wrap 15");

			panel.add(_ok,     "tag ok,     split");
			panel.add(_cancel, "tag cancel, split");
			panel.add(_apply,  "tag apply,  gapright 15, wrap 15");

			_allToLevel_cb.addActionListener(this);
			_ok           .addActionListener(this);
			_cancel       .addActionListener(this);
			_apply        .addActionListener(this);

			return panel;
		}

		private void apply()
		{
			for (int r=0; r<_tm.getRowCount(); r++)
			{
				Log4jClass l = (Log4jClass) _tm.getValueAt(r, LogLevelTableModel.LEVEL_COL_POS); //col 1 = Level
				if ( ! l._originalLevel.equals(l._level) )
				{
					_logger.info("Setting new Level for '"+l._className+"' from '"+l._originalLevel+"', to '"+l._level+"'.");
					Logger logger = LogManager.exists(l._className);

					if (logger == null)
					{
						_logger.error("Class name '"+l._className+"' cant be found, check available with: log4j get" );
						return;
					}

					if      ( "TRACE".equalsIgnoreCase(l._level) ) logger.setLevel( Level.TRACE );
					else if ( "DEBUG".equalsIgnoreCase(l._level) ) logger.setLevel( Level.DEBUG );
					else if ( "INFO" .equalsIgnoreCase(l._level) ) logger.setLevel( Level.INFO  );
					else if ( "WARN" .equalsIgnoreCase(l._level) ) logger.setLevel( Level.WARN  );
					else if ( "ERROR".equalsIgnoreCase(l._level) ) logger.setLevel( Level.ERROR );
					else if ( "FATAL".equalsIgnoreCase(l._level) ) logger.setLevel( Level.FATAL );
					else logger.setLevel( Level.DEBUG );
				}
			}
		}
		
		public void actionPerformed(ActionEvent e)
		{
			Object source = e.getSource();

			// --- CHECKBOX: ALL-TO-LEVEL ---
			if (_allToLevel_cb.equals(source))
			{
				Object obj = _allToLevel_cb.getSelectedItem();
				//System.out.println("JColorComboBox: objClass="+obj.getClass().getName()+", obj="+obj);
				for (int r=0; r<_tm.getRowCount(); r++)
				{
					_tm.setValueAt(obj, r, LogLevelTableModel.LEVEL_COL_POS); //col 1 = Level
				}
			}
			
			// --- BUTTON: OK ---
			if (_ok.equals(source))
			{
				apply();
				setVisible(false);
			}

			// --- BUTTON: CANCEL ---
			if (_cancel.equals(source))
			{
				setVisible(false);
			}

			// --- BUTTON: APPLY ---
			if (_apply.equals(source))
			{
				apply();
				refreshData();
			}
		}

		//
		// implementing: TableModelListener, listen for changes and enable the "apply" button
		//
		public void tableChanged(TableModelEvent e)
		{
//			TableModel tm = (TableModel) e.getSource();
			int type      = e.getType();
			int column    = e.getColumn();
			int firstRow  = e.getFirstRow();
			int lastRow   = e.getLastRow();
			_logger.debug("TableModelEvent: type="+type+", column="+column+", firstRow="+firstRow+", lastRow="+lastRow);
		
			// event: AbstactTableModel.fireTableStructureChanged
			if (column >= 0)
			{
				_apply.setEnabled(true);
			}
		}
		
	}

	//---------------------------------------
	// The data class that Table model will show data from
	//---------------------------------------
	private class Log4jClass
	{
		protected String  _className     = "";
		protected Level   _levelObj      = null;
		protected String  _level         = "";
		protected String  _originalLevel = "";

		public Log4jClass(String className, Level level)
		{
			_className      = className;
			_levelObj       = level;
			_level          = level.toString();
			_originalLevel  = level.toString();
		}
		public String toString()
		{
			return _level;
		}
	}

	//---------------------------------------
	// The table model
	//---------------------------------------
	private class LogLevelTableModel extends AbstractTableModel
	{
		private static final long serialVersionUID = -8311230082363270019L;
		private static final int CLASS_NAME_COL_POS = 0;
		private static final int LEVEL_COL_POS      = 1;
		private ArrayList _rows = new ArrayList();
		private String[] _cols = {"Class Name", "Level"};

		public void clear()
		{
			_rows.clear();
			fireTableDataChanged();
		}
		public void addRows(Log4jClass c)
		{
			_rows.add(c);
			fireTableDataChanged();
		}

		public String getColumnName(int col)
		{
			return _cols[col];
		}
	
		public int getColumnCount()
		{
			return _cols.length;
		}
	
		public int getRowCount()
		{
			return _rows.size();
		}
	
		public Object getValueAt(int row, int col)
		{
			Log4jClass r = (Log4jClass) _rows.get(row);
			switch (col)
			{
			case CLASS_NAME_COL_POS: return r._className;
			case LEVEL_COL_POS:      return r;
			}
			return null;
		}

		public void setValueAt(Object obj, int row, int col)
		{
			_logger.trace("row="+row+", col="+col+", obj='"+obj.getClass().getName()+"', obj.toString='"+obj+"'.");
			Log4jClass r = (Log4jClass) _rows.get(row);
			switch (col)
			{
			case LEVEL_COL_POS: r._level = obj.toString();
			}
			fireTableCellUpdated(row, 1);
		}

		public Class getColumnClass(int col)
		{
			if (col == LEVEL_COL_POS) return Log4jClass.class;
			return String.class;
		}

		public boolean isCellEditable(int row, int col)
		{
			return col == LEVEL_COL_POS;
		}
		
	}

}
