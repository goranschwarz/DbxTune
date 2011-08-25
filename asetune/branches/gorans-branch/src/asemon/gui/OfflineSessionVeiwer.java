/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */

package asemon.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.treetable.TreeTableModel;

import asemon.Version;
import asemon.gui.OfflineSessionModel.SessionLevel;
import asemon.gui.swing.AbstractComponentDecorator;
import asemon.pcs.PersistReader;
import asemon.utils.Configuration;
import asemon.utils.SwingUtils;

public class OfflineSessionVeiwer 
extends JFrame
//implements Runnable, ActionListener//, TableModelListener
implements ActionListener, PersistReader.NofificationListener//, TableModelListener
{
	private static Logger _logger = Logger.getLogger(OfflineSessionVeiwer.class);
	private static final long serialVersionUID = -113668702537883167L;

	private Frame    _owner      = null;

	//-------------------------------------------------
	// DATA TABLE panel
//	private Connection             _conn            = null;
	private OfflineSessionModel    _model           = null;
	private JXTreeTable            _treeTable       = null;
	private JPopupMenu             _tablePopupMenu  = null;

//	private JPanel                 _topPanel        = null;
//	private JPanel                 _tablePanel      = null;
//	private JPanel                 _bottomPanel     = null;

	// Top panel
	private JLabel                 _day_lbl         = new JLabel("Day");
	private JLabel                 _hour_lbl        = new JLabel("Hour");
	private JLabel                 _minute_lbl      = new JLabel("Minute");
	private JComboBox              _day_cbx         = new JComboBox(new String[] {"1"});
	private JComboBox              _hour_cbx        = new JComboBox(new String[] {"1", "2", "3", "4", "6", "12"});
	private JComboBox              _minute_cbx      = new JComboBox(new String[] {"1", "2", "5", "10", "15", "20", "30"});
	private int                    _dayLevel        = 1;
	private int                    _hourLevel       = 1;
	private int                    _minuteLevel     = 10;
//	private JTextField             _day_txt         = new JTextField();
//	private JTextField             _hour_txt        = new JTextField();
//	private JTextField             _minute_txt      = new JTextField();
	private JButton                _refresh_but     = new JButton("Refresh");
	private JCheckBox              _linkSliderTree_chk  = new JCheckBox("Link Slider with Tree", true);

	// Bottom panel
	private JLabel                 _status_lbl      = new JLabel("");
	private JButton                _show_but        = new JButton("Show");

	
	private Watermark              _watermark       = null;
	private int                    _colSessionsPrefSize = 250;
	private int                    _colSessionsActSize  = 250;

	
	
	/*---------------------------------------------------
	** BEGIN: constructors
	**---------------------------------------------------
	*/
	
//	public OfflineSessionVeiwer(Frame owner, Connection conn, boolean closeConnOnExit)
	public OfflineSessionVeiwer(Frame owner)
	{
		_owner           = owner;
//		_conn            = conn;

		initComponents();

//		this.pack();
//		SwingUtils.setLocationNearTopLeft(_owner, this);

		// Start the COMMAND READER THREAD
//		start();
	}
	/*---------------------------------------------------
	** END: constructors
	**---------------------------------------------------
	*/

	/*---------------------------------------------------
	** BEGIN: component initialization
	**---------------------------------------------------
	*/
	protected void initComponents() 
	{
		if (_owner != null)
			setIconImage(_owner.getIconImage());
		
		setTitle("Offline Session Viewer");

		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("","",""));
		panel.setLayout(new BorderLayout());

//		panel.add(createTopPanel(),   "wrap");
//		panel.add(createTablePanel(), "wrap");
//		panel.add(createTablePanel(), "width 100%, height 100%, wrap");

		panel.add(createTopPanel(),    BorderLayout.NORTH);
		panel.add(createTablePanel(),  BorderLayout.CENTER);
		panel.add(createBottomPanel(), BorderLayout.SOUTH);

		loadProps();
		setSomeValuesToModel();
		refreshTable(false);

		setContentPane(panel);
		
		initComponentActions();
	}

	private JPanel createTopPanel() 
	{
		JPanel panel = SwingUtils.createPanel("Top", false);
		panel.setLayout(new MigLayout("ins 3 10 3 3", //ins T L B R???
				"",
				""));

		_refresh_but.setToolTipText("Refresh the sessions list from the database.");
		_linkSliderTree_chk.setToolTipText(
				"<html>" +
				  "When you click on the view-slider or graph-content, to position what data to view.<br>" +
				  "Make the selected timestamp node visable in the tree view as well." +
				"</html>");

		panel.add(_day_lbl,  "");
//		panel.add(_day_txt,  "width 30lp");
		panel.add(_day_cbx,  "");

		panel.add(_hour_lbl,  "gap 10");
//		panel.add(_hour_txt,  "width 30lp");
		panel.add(_hour_cbx,  "");

		panel.add(_minute_lbl,  "gap 10");
//		panel.add(_minute_txt,  "width 30lp");
		panel.add(_minute_cbx,  "");

		panel.add(_linkSliderTree_chk, "push, right");

		panel.add(_refresh_but, "push, right");

		return panel;
	}

	private JPanel createBottomPanel() 
	{
		JPanel panel = SwingUtils.createPanel("Bottom", false);
		panel.setLayout(new MigLayout("ins 3 10 3 3", //ins T L B R???
				"",
				""));

		_show_but   .setToolTipText("Show the selected sample period in the summary graphs.");
		
		panel.add(_status_lbl,  "push, left");
		panel.add(_show_but,    "push, right");
		return panel;
	}

	private JPanel createTablePanel() 
	{
		JPanel panel = SwingUtils.createPanel("Actual Data Table", false);
//		panel.setLayout(new MigLayout("debug, ins 0", 
//				"", 
//				"0[0]0"));
		panel.setLayout(new BorderLayout()); 

		// Extend the JXTable to get tooltip stuff
		_treeTable = new JXTreeTable()
		{
	        private static final long serialVersionUID = 0L;

			public String getToolTipText(MouseEvent e) 
			{
				String tip = null;
				Point p = e.getPoint();
				int row = rowAtPoint(p);
				if (row >= 0)
				{
					row = super.convertRowIndexToModel(row);

//					TableModel model = getModel();
//					if (model instanceof Log4jTableModel)
//					{
//						Log4jLogRecord l = ((Log4jTableModel)model).getRecord(row);
//						tip = l.getToolTipText();
//					}
				}
				return tip;
			}
		};

//		if (_conn == null)
//			_conn = getOfflineConnection();

//		_model = new OfflineSessionModel(_conn);
		_model = new OfflineSessionModel();
//		_model.init(null);

		_treeTable.setTreeTableModel(_model);
		_treeTable.setTreeCellRenderer(new IconRenderer());
//	    _treeTable.setTreeCellRenderer(new XIconRenderer());

//		_treeTable.setHighlighters(_highliters);

//		_treeTable.setModel(GuiLogAppender.getTableModel());
		_treeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_treeTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		_treeTable.packAll(); // set size so that all content in all cells are visible
		_treeTable.setSortable(true);
		_treeTable.setColumnControlVisible(true);
//		_treeTable.setHighlighters(_highlitersLogLevelAtColId_2); // a variant of cell render

		_treeTable.addKeyListener(new TreeTableNavigationEnhancer(_treeTable));
		_treeTable.getColumnModel().getColumn(0).setWidth(250);

		_tablePopupMenu = createDataTablePopupMenu();
		_treeTable.setComponentPopupMenu(_tablePopupMenu);

		
		JScrollPane scroll = new JScrollPane(_treeTable);
		_watermark = new Watermark(scroll, "");

		panel.add(scroll, BorderLayout.CENTER);
//		panel.add(scroll, "width 100%, height 100%");
		return panel;
	}
	/*---------------------------------------------------
	** END: component initialization
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
		JMenuItem show = new JMenuItem("Show");

		popup.add(show);

		show.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				doActionShow();
			}
		});

		if (popup.getComponentCount() == 0)
		{
			_logger.warn("No PopupMenu has been assigned for the data table in the panel.");
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
	** BEGIN: implementing: PersistReader.NofificationListener
	**---------------------------------------------------
	*/
	public void setWatermark(String text)
	{
		setWatermarkText(text);
	}
	public void setStatusText(String status)
	{
		_status_lbl.setText(status);
	}
	/** Called by PersistReader when <code>loadSessionList()</code> has been executed. */
	public void setSessionList(List sessionList)
	{
		refreshTable(sessionList);
	}

	/*---------------------------------------------------
	** END: implementing: PersistReader.NofificationListener
	**---------------------------------------------------
	*/

	
	/*---------------------------------------------------
	** BEGIN: Action Listeners, and helper methods for it
	**---------------------------------------------------
	*/
	private void setLevelCbx()
	{
		_day_cbx   .setSelectedItem(""+_dayLevel);
		_hour_cbx  .setSelectedItem(""+_hourLevel);
		_minute_cbx.setSelectedItem(""+_minuteLevel);
	}
	private void getLevelCbx()
	{
		_dayLevel    = Integer.parseInt(_day_cbx   .getSelectedItem().toString());
		_hourLevel   = Integer.parseInt(_hour_cbx  .getSelectedItem().toString());
		_minuteLevel = Integer.parseInt(_minute_cbx.getSelectedItem().toString());
	}
	private void readSomeValuesFromModel()
	{
		_dayLevel        = _model.getDayLevelCount();
		_hourLevel       = _model.getHourLevelCount();
		_minuteLevel     = _model.getMinuteLevelCount();

	}
	private void setSomeValuesToModel()
	{
		_model.setDayLevelCount(    _dayLevel    );
		_model.setHourLevelCount(   _hourLevel   );
		_model.setMinuteLevelCount( _minuteLevel );
	}
//	private void refreshTable()
//	{
//		refreshTable(this.isVisible());
//	}
	private void refreshTable(boolean saveGuiStuff)
	{
//		refreshTable(saveGuiStuff, null);
		PersistReader reader = PersistReader.getInstance();
		if (reader == null)
			throw new RuntimeException("The 'PersistReader' has not been initialized.");

		// This would be a async call...
		// the reader would call setSessionList(List sessionList) on completion
		// which then calls: refreshTable(List sessionList)
		// wchich populate the GUI
		reader.loadSessions();
		
	}
	private void refreshTable(List sessionList)
	{
		refreshTable(this.isVisible(), sessionList);
	}
	private void refreshTable(boolean saveGuiStuff, List sessionList)
	{
		PersistReader reader = PersistReader.getInstance();
		if (reader == null)
			throw new RuntimeException("The 'PersistReader' has not been initialized.");

		setSomeValuesToModel();

		if (saveGuiStuff && _treeTable.getRowCount() > 0)
		{
			_colSessionsPrefSize = _treeTable.getColumnModel().getColumn(0).getPreferredWidth();
			_colSessionsActSize  = _treeTable.getColumnModel().getColumn(0).getWidth();
			getLevelCbx();
			saveProps();
		}

//		_model = new OfflineSessionModel(_conn);
		_model = new OfflineSessionModel();
		setSomeValuesToModel();
		_model.init(sessionList);
		_treeTable.setTreeTableModel(_model);
//		_model.refresh();
//		reader.getStoredCms(true);
//		_treeTable.tableChanged(null);
		_treeTable.packAll(); // set size so that all content in all cells are visible
		_treeTable.getColumnModel().getColumn(0).setPreferredWidth(_colSessionsPrefSize);
		_treeTable.getColumnModel().getColumn(0).setWidth(_colSessionsActSize);
		
		// Change focus to the table... so that keyboard navigation is possible
		setFocus();
	}

	private void setFocus()
	{
		// The components needs to be visible for the requestFocus()
		// to work, so lets the EventThreda do it for us after the windows is visible.
		Runnable deferredAction = new Runnable()
		{
			public void run()
			{
				_treeTable.requestFocus();
			}
		};
		SwingUtilities.invokeLater(deferredAction);
	}
	

	private void initComponentActions()
	{
		readSomeValuesFromModel();

		//---- Top PANEL -----
//		_day_txt    .addActionListener(this);
//		_hour_txt   .addActionListener(this);
//		_minute_txt .addActionListener(this);
		_day_cbx    .addActionListener(this);
		_hour_cbx   .addActionListener(this);
		_minute_cbx .addActionListener(this);
		_refresh_but.addActionListener(this);

		//---- Bottom PANEL -----
		_show_but   .addActionListener(this);

		this.addWindowListener(new java.awt.event.WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				saveProps();
			}
		});
	}

	public void actionPerformed(ActionEvent e)
    {
		Object source = e.getSource();
		
//		if (_day_txt.equals(source))    setSomeValuesToModel();
//		if (_hour_txt.equals(source))   setSomeValuesToModel();
//		if (_minute_txt.equals(source)) setSomeValuesToModel();
		if (_day_cbx   .equals(source))  refreshTable(true);
		if (_hour_cbx  .equals(source))  refreshTable(true);
		if (_minute_cbx.equals(source))  refreshTable(true);
		if (_refresh_but.equals(source)) refreshTable(true);

		if (_show_but.equals(source))
		{
			doActionShow();
		}

    }

	public void doActionShow()
	{
		int row = _treeTable.getSelectedRow();
		Object o = _treeTable.getValueAt(row, 0);
		_logger.info("SHOW_BUT was pressed, currentRow="+row+", value='"+o+"'.");

		PersistReader reader = PersistReader.getInstance();
		if (reader == null)
			throw new RuntimeException("The 'PersistReader' has not been initialized.");

//		if (reader.isRunning())
//		{
//			if (o instanceof SessionLevel)
//			{
//				SessionLevel sl = (SessionLevel) o;
//				
//				reader.addCommand(new PersistReader.QueueCommand("loadSessionGraphs",  sl.getSampleId(), sl.getPeriodStartTime(), sl.getPeriodEndTime(), sl._numOfSamples));
//				reader.addCommand(new PersistReader.QueueCommand("loadTimelineSlider", sl.getSampleId(), sl.getPeriodStartTime(), sl.getPeriodEndTime(), -1));
//			}
//			else if (o instanceof Timestamp)
//			{
//				Timestamp ts = (Timestamp) o;
////				reader.addCommand(new PersistReader.QueueCommand("loadSessionCms", ts, null, null, -1));
//
//				reader.addCommand(new PersistReader.QueueCommand("loadSessionCmIndicators", ts, null, null, -1));
//				reader.addCommand(new PersistReader.QueueCommand("loadSummaryCm", ts, null, null, -1));
//			}
//		}
//		else
//		{
			if (o instanceof SessionLevel)
			{
				SessionLevel sl = (SessionLevel) o;
				reader.loadSessionGraphs (sl.getSampleId(), sl.getPeriodStartTime(), sl.getPeriodEndTime(), sl._numOfSamples);
				reader.loadTimelineSlider(sl.getSampleId(), sl.getPeriodStartTime(), sl.getPeriodEndTime());
			}
			else if (o instanceof Timestamp)
			{
				Timestamp ts = (Timestamp) o;
//				reader.loadSessionCms (ts);

				reader.loadSessionCmIndicators(ts);
				reader.loadSummaryCm(ts);
			}
//		}
	}
	
	/*---------------------------------------------------
	** END: Action Listeners
	**---------------------------------------------------
	*/

	/**
	 * Make the node having <code>Timestamp</code> visible in the tree view.
	 */
	public void setCurrentSampleTimeView(Timestamp ts)
	{
		if (_linkSliderTree_chk.isSelected())
		{
			// FIXME: Get the correct node in the list, and show it in the tree view
			System.out.println("FIXME: Get the correct node in the list, and show it in the tree view... ts='"+ts+"'.");
		}
	}
	
	
//	/*---------------------------------------------------
//	** BEGIN: Command thread stuff
//	**---------------------------------------------------
//	*/
//	/** Queue that holds commands for the _readThread */
//	private BlockingQueue  _cmdQueue   = new LinkedBlockingQueue();
//	private Thread         _readThread = null;
//	private boolean        _running    = false;
//	private int            _warnQueueSizeThresh = 10;
//
//	/** Place holder object for commands to be executed be the readThread */
//	private class QueueCommand
//	{
//		String    _cmd  = null;
//		Timestamp _ts1  = null;
//		Timestamp _ts2  = null;
//		Timestamp _ts3  = null;
//		int       _int1 = -1;
//		
//		public QueueCommand(String cmd, Timestamp ts1, Timestamp ts2, Timestamp ts3, int int1) 
//		{
//			_cmd  = cmd;
//			_ts1  = ts1;
//			_ts2  = ts2;
//			_ts3  = ts3;
//			_int1 = int1;
//		}
//	}
//
//	public void addCommand(QueueCommand qcmd)
//	{
//		int qsize = _cmdQueue.size();
//		if (qsize > _warnQueueSizeThresh)
//		{
//			_logger.warn("The Command queue has "+qsize+" entries. The CommandExecutor might not keep in pace.");
//		}
//
//		_cmdQueue.add(qcmd);
//	}
//
//	public void shutdown()
//	{
//		_running = false;
//		if (_readThread != null)
//			_readThread.interrupt();
//		_readThread = null;
//	}
//	public void start()
//	{
//		if (_readThread != null)
//		{
//			_logger.info("The thread '"+_readThread.getName()+"' is already running. Skipping the start.");
//			return;
//		}
//
//		_readThread = new Thread(this);
//		_readThread.setName("OfflineSessionReader");
//		_readThread.setDaemon(true);
//		_readThread.start();
////		SwingUtilities.invokeLater(this);
//	}
//	public boolean isRunning()
//	{
//		return _running;
//	}
//
//	public void run()
//	{
//		String threadName = _readThread.getName();
//		_logger.info("Starting a thread for the module '"+threadName+"'.");
//
//		_running = true;
//
//		while(_running)
//		{
//			if (_logger.isDebugEnabled())
//				_logger.debug("Thread '"+threadName+"', waiting on queue...");
//
//			try 
//			{
//				QueueCommand qo = (QueueCommand)_cmdQueue.take();
//
//				// Make sure the container isn't empty.
//				if (qo == null)
//					continue;
//				long xStartTime = System.currentTimeMillis();
//
//				// Known commands
//				// addCommand(new QueueCommand("loadTimelineSlider", sl.getSampleId(), sl.getPeriodStartTime(), sl.getPeriodEndTime()));
//				// addCommand(new QueueCommand("loadSessionGraphs",  sl.getSampleId(), sl.getPeriodStartTime(), sl.getPeriodEndTime()));
//				// addCommand(new QueueCommand("loadSessionCms", ts, null, null));
//
//				// DO WORK
//				String cmdStr = qo._cmd + "(ts1="+qo._ts1+", ts2="+qo._ts2+", ts3="+qo._ts3+")";
//				if      ("loadTimelineSlider".equals(qo._cmd)) loadTimelineSlider(qo._ts1, qo._ts2, qo._ts3);
//				else if ("loadSessionGraphs" .equals(qo._cmd)) loadSessionGraphs (qo._ts1, qo._ts2, qo._ts3, qo._int1);
//				else if ("loadSessionCms"    .equals(qo._cmd)) loadSessionCms    (qo._ts1);
//				else _logger.error("Unknown command '"+qo._cmd+"' was taken from the queue.");
//
//				long xStopTime = System.currentTimeMillis();
//				long consumeTimeMs = xStopTime-xStartTime;
//				_logger.debug("It took "+TimeUtils.msToTimeStr(consumeTimeMs)+" to execute: "+cmdStr);
//				
//			} 
//			catch (InterruptedException ex) 
//			{
//				_running = false;
//			}
//		}
//
//		_logger.info("Emptying the queue for module '"+threadName+"', which had "+_cmdQueue.size()+" entries.");
//		_cmdQueue.clear();
//
//		_logger.info("Thread '"+threadName+"' was stopped.");
//	}
//
////	public void run() 
////	{
////		_running = true;
////		while(true)
////		{
////			try
////			{
////			}
////			catch (Throwable t)
////			{
////			}
////		}
////	}
//	/*---------------------------------------------------
//	** END: Command thread stuff
//	**---------------------------------------------------
//	*/

	
	
//	public LinkedHashMap _offlineCmMap = null;
//	public class OfflineCm
//	{
//		public String  name    = "";
//		public boolean hasAbs  = false;
//		public boolean hasDiff = false;
//		public boolean hasRate = false;
//		public List    graphList = null;
//
//		public OfflineCm(String name) 
//		{
//			this.name = name;
//		}
//		public void add(String name, String type)
//		{
//			if (type == null)
//				return;
//			if      (type.equals("abs"))  hasAbs  = true;
//			else if (type.equals("diff")) hasDiff = true;
//			else if (type.equals("rate")) hasRate = true;
//			else
//			{
//				if (graphList == null)
//					graphList = new LinkedList();
//				graphList.add(type);
//			}
//		}
//		public String toString()
//		{
//			return "OfflineCm(name='"+name+"', hasAbs="+hasAbs+", hasDiff="+hasDiff+", hasRate="+hasRate+", graphList="+graphList+")";
//		}
//	}
//	public void addOfflineCm(String name, String type)
//	{
//		OfflineCm ocm = (OfflineCm)_offlineCmMap.get(name);
//		if (ocm == null)
//		{
//			ocm = new OfflineCm(name);
//			_offlineCmMap.put(ocm.name, ocm);
//		}
//		ocm.add(name, type);
//	}
//
//	public void getStoredCms(boolean refresh)
//	{
//		// No need to refresh this list
//		if (_offlineCmMap != null && !refresh)
//			return;
//
//		_offlineCmMap = new LinkedHashMap();
//		ResultSet rs = null;
//
//		try 
//		{
//			// Obtain a DatabaseMetaData object from our current connection
//			DatabaseMetaData dbmd = _conn.getMetaData();
//	
//	//		rs = dbmd.getTables(null, null, "%", null);
//			rs = dbmd.getTables(null, null, "%", new String[] { "TABLE" });
//
//			while(rs.next())
//			{
//				String tableName = rs.getString("TABLE_NAME");
//				String tableType = rs.getString("TABLE_TYPE");
//	
//				int sepPos = tableName.indexOf("_");
//				if (sepPos < 0)
//					continue;
//				String name = tableName.substring(0, sepPos);
//				String type = tableName.substring(sepPos+1);
//				if (_logger.isDebugEnabled())
//					_logger.debug("getStoredCms()-rs-row- TYPE='"+tableType+"', NAME='"+tableName+"'. name='"+name+"', type='"+type+"'");
//				
//				addOfflineCm(name, type);
//			}
////			ResultSetTableModel tab = new ResultSetTableModel(rs);
////			System.out.println("getStoredCms()-3\n" + tab.toTableString());
//			rs.close();
//
//			if (_logger.isDebugEnabled())
//				_logger.debug("_offlineCmMap="+_offlineCmMap);
//		}
//		catch (SQLException e)
//		{
//			_logger.error("Problems getting Offlined table names.", e);
//		}
//	}
//
//	public int loadSessionGraph(String cmName, String graphName, Timestamp sampleId, Timestamp startTime, Timestamp endTime, int expectedRows)
//	{
//		CountersModel cm = GetCounters.getCmByName(cmName);
//		if (cm == null)
//		{
//			_logger.warn("Cant find any CM named '"+cmName+"'.");
//			return 0;
//		}
//		TrendGraph tg = cm.getTrendGraph(graphName);
//		if (tg == null)
//		{
//			_logger.warn("Cant find any TrendGraph named '"+graphName+"', for the CM '"+cmName+"'.");
//			return 0;
//		}
//		tg.clearGraph();
//
//		//----------------------------------------
//		// TYPICAL look of a graph table
//		//----------------------------------------
//		// CREATE TABLE "CMengineActivity_cpuSum"
//		//   "SessionStartTime"   DATETIME        NOT NULL,
//		//   "SessionSampleTime"  DATETIME        NOT NULL,
//		//   "SampleTime"         DATETIME        NOT NULL,
//		//   "label_0"            VARCHAR(30)         NULL,
//		//   "data_0"             NUMERIC(10, 1)      NULL,
//		//   "label_1"            VARCHAR(30)         NULL,
//		//   "data_1"             NUMERIC(10, 1)      NULL,
//		//   "label_2"            VARCHAR(30)         NULL,
//		//   "data_2"             NUMERIC(10, 1)      NULL
//
//		String sql = "select * from \""+cmName+"_"+graphName+"\" " +
//		             "where \"SessionStartTime\" = ? " +
//		             "  AND \"SessionSampleTime\" >= ? " +
//		             "  AND \"SessionSampleTime\" <= ? " +
//		             "order by \"SessionSampleTime\"";
//
//		// If we expect a big graph, load only every X row
//		// if we add to many to the graph, the JVM takes 100% CPU, I'm guessing it 
//		// has to do too many repaints, we could do an "average" of X rows during the load
//		// but I took the easy way out... (or figure out why it's taking all the CPU)
//		int loadEveryXRow = expectedRows / 1000 + 1; // 1 = load every row
//
//		try
//		{
//			long fetchStartTime = System.currentTimeMillis();
//			_status_lbl.setText("Loading graph '"+graphName+"' for '"+cmName+"'.");
//
//			PreparedStatement pstmnt = _conn.prepareStatement(sql);
//			pstmnt.setTimestamp(1, sampleId);
//			pstmnt.setTimestamp(2, startTime);
//			pstmnt.setTimestamp(3, endTime);
//
//			_logger.debug("loadSessionGraph(cmName='"+cmName+"', graphName='"+graphName+"') loadEveryXRow="+loadEveryXRow+": "+pstmnt);
//
//			ResultSet rs = pstmnt.executeQuery();
//			ResultSetMetaData rsmd = rs.getMetaData();
//
//			int cols = rsmd.getColumnCount();
//			String[] labels = new String[(cols-3)/2];
//			Double[] datapt = new Double[(cols-3)/2];
//			boolean firstRow = true;
//			int row = 0;
//
////			Timestamp sessionStartTime  = null;
//			Timestamp sessionSampleTime = null;
////			Timestamp sampleTime        = null;
//
//			while (rs.next())
//			{
////				sessionStartTime  = rs.getTimestamp(1);
//				sessionSampleTime = rs.getTimestamp(2);
////				sampleTime        = rs.getTimestamp(3);
//
//				// Start to read column 4
//				// move c (colIntex) 2 cols at a time, move ca (ArrayIndex) by one
//				for (int c=4, ca=0; c<=cols; c+=2, ca++)
//				{
//					labels[ca] = rs.getString(c);
//					datapt[ca] = new Double(rs.getDouble(c+1));					
//				}
//				// Add a extra record at the BEGINING of the traces... using 0 data values
//				if (firstRow)
//				{
//					firstRow = false;
//					Double[] firstDatapt = new Double[datapt.length];
//					for (int d=0; d<firstDatapt.length; d++)
//						firstDatapt[d] = new Double(0);
//					tg.addPoint(new Timestamp(sessionSampleTime.getTime()-10),  // - 10 millisec
//							firstDatapt, 
//							labels, startTime, endTime);
//				}
//
//				// If we expect a big graph, load only every X row
//				// if we add to many to the graph, the JVM takes 100% CPU, I'm guessing it 
//				// has to do too many repaints, we could do an "average" of X rows during the load
//				// but I took the easy way out... (or figure out why it's taking all the CPU)
//				if ( row % loadEveryXRow == 0 )
//					tg.addPoint(sessionSampleTime, datapt, labels, startTime, endTime);
//
//				row++;
//			}
//			rs.close();
//			pstmnt.close();
//
//			// Add a extra record at the end of the traces... using 0 data values
//			if (sessionSampleTime != null)
//			{
//				Double[] lastDatapt = new Double[datapt.length];
//				for (int d=0; d<lastDatapt.length; d++)
//					lastDatapt[d] = new Double(0);
//				tg.addPoint(new Timestamp(sessionSampleTime.getTime()+10), // + 10 millisec
//						lastDatapt, 
//						labels, startTime, endTime);
//			}
//System.out.println("Loaded "+row+" rows into TrendGraph named '"+graphName+"', for the CM '"+cmName+"', which took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.");
//			_logger.debug("Loaded "+row+" rows into TrendGraph named '"+graphName+"', for the CM '"+cmName+"', which took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.");
//			_status_lbl.setText("");
//			
//			return loadEveryXRow;
//		}
//		catch (SQLException e)
//		{
//			_logger.error("Problems loading graph for cm='"+cmName+"', graph='"+graphName+"'.", e);
//		}
//		return 0;
//	}
//
//	public void loadSessionGraphs(Timestamp sampleId, Timestamp startTime, Timestamp endTime, int expectedRows)
//	{
//		setWatermarkText("Loading graphs...");
//		System.out.println("loadSession(sampleId='"+sampleId+"', startTime='"+startTime+"', endTime='"+endTime+"')");
//		long xStartTime = System.currentTimeMillis();
//
//		// Populate _offlineCmMap
//		getStoredCms(false);
//
//		int loadEveryXRow = 0;
//
//		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
//		String graphWatermark = sdf.format(startTime) + " - " + sdf.format(endTime);
//		MainFrame.setOfflineSamplePeriodText(graphWatermark);
//
//		// Now loop the _offlineCmMap
//		for (Iterator it1 = _offlineCmMap.keySet().iterator(); it1.hasNext();) 
//		{
//			String cmName = (String) it1.next();
//			OfflineCm ocm = (OfflineCm) _offlineCmMap.get(cmName);
//
//			if (ocm.graphList == null)
//				continue;
//
//			for (Iterator it2 = ocm.graphList.iterator(); it2.hasNext();) 
//			{
//				String graphName = (String) it2.next();
//				
//				loadEveryXRow = loadSessionGraph(cmName, graphName, sampleId, startTime, endTime, expectedRows);
//			}
//		}
//		String str = "Loading all TrendGraphs took '"+TimeUtils.msToTimeStr("%SS.%ms", System.currentTimeMillis()-xStartTime)+"' seconds.";
//		if (loadEveryXRow > 1)
//			str += " Loaded every "+(loadEveryXRow-1)+" row, graphs was to big.";
//		_status_lbl.setText(str);
//		setWatermark();
//	}
//
//	public void loadTimelineSlider(Timestamp sampleId, Timestamp startTime, Timestamp endTime)
//	{
//		long xStartTime = System.currentTimeMillis();
//		setWatermarkText("Refreshing Timeline slider...");
//
//		//AseMonSessionSamples  SessionStartTime    TIMESTAMP
//		//AseMonSessionSamples  SessionSampleTime   TIMESTAMP
//		
//		MainFrame mf = MainFrame.getInstance();
//		if ( mf == null )
//			return;
//
//		mf.resetOfflineSlider();
//		
//		String sql = "select \"SessionSampleTime\" " +
//		             "from " + PersistWriterBase.getTableName(PersistWriterBase.SESSION_SAMPLES, null, true) + " "+
//		             "where \"SessionStartTime\" = ? " +
//		             "  AND \"SessionSampleTime\" >= ? " +
//		             "  AND \"SessionSampleTime\" <= ? " +
//		             "order by \"SessionSampleTime\"";
//		try
//		{
//			PreparedStatement pstmnt = _conn.prepareStatement(sql);
//			pstmnt.setTimestamp(1, sampleId);
//			pstmnt.setTimestamp(2, startTime);
//			pstmnt.setTimestamp(3, endTime);
//
//			ResultSet rs = pstmnt.executeQuery();
//
//			ArrayList tsList = new ArrayList();
//			while (rs.next())
//			{
//				Timestamp sessionSampleTime = rs.getTimestamp(1);
//				tsList.add(sessionSampleTime);
////				mf.addOfflineSliderEntry(sessionSampleTime);
//			}
//
//			rs.close();
//			pstmnt.close();
//
//			mf.addOfflineSliderEntryList(tsList);
//		}
//		catch (SQLException e)
//		{
//			_logger.error("Problems loading Timeline slider.", e);
//		}
//System.out.println("loadTimelineSlider(sampleId='"+sampleId+"', startTime='"+startTime+"', endTime='"+endTime+"'), which took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-xStartTime)+"'.");
//		setWatermark();
//	}
//	public void loadSessionCms(Timestamp sampleId)
//	{
//		setWatermarkText("Loading Counters...");
//		System.out.println("loadSessionCms(sampleId='"+sampleId+"')");
//
//		long fetchStartTime = System.currentTimeMillis();
//		_status_lbl.setText("Loading all counters for time '"+sampleId+"'.");
//
//		// Populate _offlineCmMap
//		getStoredCms(false);
//
//		// Now loop the _offlineCmMap
//		for (Iterator it1 = _offlineCmMap.keySet().iterator(); it1.hasNext();) 
//		{
//			String cmName = (String) it1.next();
//			OfflineCm ocm = (OfflineCm) _offlineCmMap.get(cmName);
//
//			loadSessionCm(ocm, sampleId);
//		}
//
//		String str = "Loading took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.";
//		_logger.debug(str);
//		_status_lbl.setText(str);
//		setWatermark();
//	}
//	public void loadSessionCm(OfflineCm ocm, Timestamp sampleTs)
//	{
//		if (ocm == null || sampleTs == null)
//			throw new IllegalArgumentException("OfflineCm or sampleTs cant be null");
//
//		String cmName = ocm.name;
//		CountersModel cm = GetCounters.getCmByName(cmName);
//		if (cm == null)
//		{
//			_logger.warn("Cant find any CM named '"+cmName+"'.");
//			return;
//		}
//		if (ocm.hasAbs)  loadSessionCm(cm, CountersModel.DATA_ABS,  sampleTs);
//		if (ocm.hasDiff) loadSessionCm(cm, CountersModel.DATA_DIFF, sampleTs);
//		if (ocm.hasRate) loadSessionCm(cm, CountersModel.DATA_RATE, sampleTs);
//
//		cm.setDataInitialized(true);
//		cm.fireTableStructureChanged();
//		if (cm.getTabPanel() != null)
//			cm.getTabPanel().adjustTableColumnWidth();
//	}
//	public void loadSessionCm(CountersModel cm, int type, Timestamp sampleTs)
//	{
//		if (cm == null || sampleTs == null)
//			throw new IllegalArgumentException("CountersModel or sampleTs cant be null");
//		
//		String cmName = cm.getName();
//
//		String typeStr = null;
//		if      (type == CountersModel.DATA_ABS)  typeStr = "abs";
//		else if (type == CountersModel.DATA_DIFF) typeStr = "diff";
//		else if (type == CountersModel.DATA_RATE) typeStr = "rate";
//		else throw new IllegalArgumentException("Unknown type of "+type+".");
//
//		long fetchStartTime = System.currentTimeMillis();
//		_status_lbl.setText("Loading '"+typeStr+"' counters for '"+cmName+"'.");
//
//		//----------------------------------------
//		// TYPICAL look of a graph table
//		//----------------------------------------
//		// CREATE TABLE "CMengineActivity_abs"  << abs|diff|rate
//		//     "SessionStartTime"  DATETIME NOT NULL,
//		//     "SessionSampleTime" DATETIME NOT NULL,
//		//     "SampleTime"        DATETIME NOT NULL,
//		//     "SampleMs"          INT      NOT NULL,
//		//     "col1"              datatype     null,
//		//     "col2"              datatype     null,
//		//     "...."              datatype     null,
//		//
//		String sql = "select * from \""+cmName+"_"+typeStr+"\" " +
//		             "where \"SessionSampleTime\" = ? ";
//		String sql2 = "select * from \""+cmName+"_"+typeStr+"\" " +
//		             "where \"SessionSampleTime\" = '"+sampleTs+"' ";
//
//		try
//		{
////			PreparedStatement pstmnt = _conn.prepareStatement(sql);
////			pstmnt.setTimestamp(1, sampleTs);
//			Statement pstmnt = _conn.createStatement();
//			
//
//			_logger.debug("loadSessionCm(cmName='"+cmName+"', type='"+typeStr+"', sampleTs='"+sampleTs+"'): "+pstmnt);
//
////			ResultSet rs = pstmnt.executeQuery();
//			ResultSet rs = pstmnt.executeQuery(sql2);
//			ResultSetMetaData rsmd = rs.getMetaData();
//			int cols = rsmd.getColumnCount();
//			int row  = 0;
//
////			Object oa[] = new Object[cols-4]; // Object Array
////			Object ha[] = new String[cols-4]; // Header Array  (Column Names)
//			List colHead = new ArrayList(cols);
//
//			// Get headers / colNames
//			for (int c=5; c<=cols; c++)
//			{
////				ha[c-4] = rsmd.getColumnLabel(c);
//				colHead.add(rsmd.getColumnLabel(c));
//			}
//			cm.setColumnNames(type, colHead);
////			cm.setColumnName(type, c);
////			cm.setColumnClass(type, c);
//
//			// Get Rows
//			while (rs.next())
//			{
//				Timestamp sessionStartTime  = rs.getTimestamp(1);
//				Timestamp sessionSampleTime = rs.getTimestamp(2);
//				Timestamp sampleTime        = rs.getTimestamp(3);
//				int       sampleMs          = rs.getInt(4);
//
//				cm.setSampleTimeHead(sessionStartTime);
////				cm.setSampleTime(sessionSampleTime);
//				cm.setSampleTime(sampleTime);
//				cm.setSampleInterval(sampleMs);
//				
//				for (int c=5,col=0; c<=cols; c++,col++)
//				{
////					oa[col] = rs.getObject(c);
//					cm.setValueAt(type, rs.getObject(c), row, col);
//				}
//				
//				row++;
//			}
//			rs.close();
//			pstmnt.close();
//
//System.out.println("Loaded "+row+" rows into for the CM '"+cmName+"', type='"+typeStr+"', which took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.");
//			_logger.debug("Loaded "+row+" rows into for the CM '"+cmName+"', type='"+typeStr+"', which took '"+TimeUtils.msToTimeStr(System.currentTimeMillis()-fetchStartTime)+"'.");
//			_status_lbl.setText("");
//		}
//		catch (SQLException e)
//		{
//			_logger.error("Problems loading cm='"+cmName+"', type='"+typeStr+"'.", e);
//		}
//	}
	

	private void saveProps()
  	{
		Configuration tmpConf = Configuration.getInstance(Configuration.TEMP);
		String base = "offlineSessionViewer.";

		if (tmpConf != null)
		{
			tmpConf.setProperty(base + "window.width", this.getSize().width);
			tmpConf.setProperty(base + "window.height", this.getSize().height);
			tmpConf.setProperty(base + "window.pos.x", this.getLocationOnScreen().x);
			tmpConf.setProperty(base + "window.pos.y", this.getLocationOnScreen().y);

			tmpConf.setProperty(base + "colSessionsPrefSize", _colSessionsPrefSize);
			tmpConf.setProperty(base + "colSessionsActSize",  _colSessionsActSize);

			tmpConf.setProperty(base + "dayLevel",    _dayLevel);
			tmpConf.setProperty(base + "hourLevel",   _hourLevel);
			tmpConf.setProperty(base + "minuteLevel", _minuteLevel);
			
			tmpConf.setProperty(base + "option.linkSliderTree", _linkSliderTree_chk.isSelected());

			tmpConf.save();
		}
  	}

  	private void loadProps()
  	{
		int     width     = 600;
		int     height    = 350;
		int     x         = -1;
		int     y         = -1;

		Configuration tmpConf = Configuration.getInstance(Configuration.TEMP);
		String base = "offlineSessionViewer.";

		setSize(width, height);

		if (tmpConf == null)
			return;

		_colSessionsPrefSize = tmpConf.getIntProperty(base + "colSessionsPrefSize", _colSessionsPrefSize);
		_colSessionsActSize  = tmpConf.getIntProperty(base + "colSessionsActSize",  _colSessionsActSize);

		// Read presentation level
		_dayLevel        = tmpConf.getIntProperty(base + "dayLevel",    _dayLevel);
		_hourLevel       = tmpConf.getIntProperty(base + "hourLevel",   _hourLevel);
		_minuteLevel     = tmpConf.getIntProperty(base + "minuteLevel", _minuteLevel);
		setLevelCbx();

		boolean bool = tmpConf.getBooleanProperty(base + "option.linkSliderTree", _linkSliderTree_chk.isSelected());
		_linkSliderTree_chk.setSelected(bool);


		// Set initial size
//		int defWidth  = (3 * Toolkit.getDefaultToolkit().getScreenSize().width)  / 4;
//		int defHeight = (3 * Toolkit.getDefaultToolkit().getScreenSize().height) / 4;

		width  = tmpConf.getIntProperty(base + "window.width",  width);
		height = tmpConf.getIntProperty(base + "window.height", height);
		x      = tmpConf.getIntProperty(base + "window.pos.x",  -1);
		y      = tmpConf.getIntProperty(base + "window.pos.y",  -1);

		if (width != -1 && height != -1)
		{
			setSize(width, height);
		}
		if (x != -1 && y != -1)
		{
			this.setLocation(x, y);
		}
		else
		{
			SwingUtils.centerWindow(this);
		}
	}
  	

//  	private Connection getOfflineConnection()
//  	{
////  	return getOfflineConnection("asemon-offline-view", "org.h2.Driver", "jdbc:h2:file:[<path>]<dbname>;IFEXISTS=TRUE", "sa", "");
//  		return getOfflineConnection("asemon-offline-view", "org.h2.Driver", "jdbc:h2:file:C:/projects/asemon/data/xxx;IFEXISTS=TRUE", "sa", "");
////  	return getOfflineConnection("asemon-offline-view", "org.h2.Driver", "jdbc:h2:tcp://<host>[:<port>]/<dbname>", "sa", "");
//  	}
//	private Connection getOfflineConnection(String appname, String driver, String url, String user, String passwd)
//	{
//		try
//		{
//			Class.forName(driver).newInstance();
//			Properties props = new Properties();
//			props.put("user", user);
//			props.put("password", passwd);
//	
//			_logger.debug("Try getConnection to driver='"+driver+"', url='"+url+"', user='"+user+"'.");
//			Connection conn = DriverManager.getConnection(url, props);
////			conn.close();
//	
//			_logger.info("Connection succeeded to Offline storage.");
//			return conn;
//		}
//		catch (SQLException e)
//		{
//			StringBuffer sb = new StringBuffer();
//			while (e != null)
//			{
//				sb.append( "\n" );
//				sb.append( e.getMessage() );
//				e = e.getNextException();
//			}
//			_logger.error("Connection FAILED.\n\n"+sb.toString());
//		}
//		catch (Exception e)
//		{
//			_logger.error("Connection FAILED.\n\n"+e.toString());
//		}
//		return null;
//	}
  	
	public static void main(String[] args) 
	{
		Properties log4jProps = new Properties();
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		//log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		// set native L&F
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());} 
		catch (Exception e) {}

		
		Configuration conf = new Configuration("c:\\OfflineSessionsViewer.tmp.deleteme.properties");
		Configuration.setInstance(Configuration.TEMP, conf);

//		OfflineSessionVeiwer view = new OfflineSessionVeiwer(null, null, true);
		OfflineSessionVeiwer view = new OfflineSessionVeiwer(null);
		view.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		view.setVisible(true);
	}


//	public static class XIconRenderer extends DefaultTreeRenderer
//	{
//
//		protected ComponentProvider createDefaultComponentProvider() 
//		{
//			return super.createDefaultComponentProvider();
//		}
//
//		public Component getTreeCellRendererComponent(JTree tree, Object value,
//				boolean selected, boolean expanded, boolean leaf, int row,
//				boolean hasFocus) 
//		{
//			return super.getTreeCellRendererComponent(tree, value, selected, expanded,
//					leaf, row, hasFocus);
//		}
//		
//	}

	public static class IconRenderer extends DefaultTreeCellRenderer 
	{
		private static final long	serialVersionUID	= 1L;
		ImageIcon sessionIcon;
		ImageIcon dayIcon;
		ImageIcon hourIcon;
		ImageIcon minuteIcon;
		ImageIcon sampleIcon;

		public IconRenderer() 
		{
			sessionIcon = SwingUtils.readImageIcon(Version.class, "images/sample_session.png");
			dayIcon     = SwingUtils.readImageIcon(Version.class, "images/sample_day.png");
			hourIcon    = SwingUtils.readImageIcon(Version.class, "images/sample_hour.png");
			minuteIcon  = SwingUtils.readImageIcon(Version.class, "images/sample_minute.png");
			sampleIcon  = SwingUtils.readImageIcon(Version.class, "images/sample_item.png");
		}

		public Component getTreeCellRendererComponent(JTree tree, Object value,	boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) 
		{
//			return super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			Component scomp = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

			if (value instanceof OfflineSessionModel.MinuteLevel)
			{
				setIcon(minuteIcon);
//				setText( ((OfflineSessionModel.SessionLevel)value).getDuration() );
				setText( ((OfflineSessionModel.SessionLevel)value).getDisplayString() );
			}
			else if (value instanceof OfflineSessionModel.HourLevel)
			{
				setIcon(hourIcon);
//				setText( ((OfflineSessionModel.SessionLevel)value).getDuration() );
				setText( ((OfflineSessionModel.SessionLevel)value).getDisplayString() );
			}
			else if (value instanceof OfflineSessionModel.DayLevel)
			{
				setIcon(dayIcon);
//				setText( ((OfflineSessionModel.SessionLevel)value).getDuration() );
				setText( ((OfflineSessionModel.SessionLevel)value).getDisplayString() );
		}
			else if (value instanceof OfflineSessionModel.SessionLevel)
			{
				setIcon(sessionIcon);
//				setText( ((OfflineSessionModel.SessionLevel)value).getDuration() );
				setText( ((OfflineSessionModel.SessionLevel)value).getDisplayString() );
			}
			else 
			{
				setIcon(sampleIcon);
			}
//			return this;
			return scomp;
		}
	}

	Highlighter[] _highliters = 
	{
//			new AbstractHighlighter(
//					new HighlightPredicate()
//					{
//						public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
//						{
//							System.out.println("xxx.isHighlighted(): COL="+adapter.column+", ROW="+adapter.row+", renderer.class="+renderer.getClass().getName()+", adapter.class="+adapter.getClass().getName() + ":  renderer.toString()="+renderer+", adapter.toString()="+adapter);
//							return true;
//						}
//					})
//			{
//				protected Component doHighlight(Component component, ComponentAdapter adapter) 
//				{
//					System.out.println("xxx.doHighlight(): component="+component.getClass().getName()+", adapter.class="+adapter.getClass().getName() + ":  component()="+component+", adapter.toString()="+adapter);
////		            ((JLabel) component).setIcon( ) );
//
//					if (component instanceof JRendererLabel)
//					{
//						Icon icon = SwingUtils.readImageIcon(Version.class, "images/asemon_icon_old.gif");
//
//						System.out.println("xxx.doHighlight(): SETTING ICON="+icon);
//						JRendererLabel xxx = (JRendererLabel) component;
//						xxx.setIcon(icon);
//					}
//					return component;
//				}
//			}
//			new IconHighlighter(new HighlightPredicate() 
//			{
//				public boolean isHighlighted(Component renderer, ComponentAdapter adapter) 
//				{
//					System.out.println("IconHighlighter.isHighlighted(): renderer.class="+renderer.getClass().getName()+", adapter.class="+adapter.getClass().getName() + ":  renderer.toString()="+renderer+", adapter.toString()="+adapter);
//					Object o = adapter.getValue();
//					System.out.println("IconHighlighter.isHighlighted(): o.class="+o.getClass().getName()+", adapter.row="+adapter.row + ", adapter.col="+adapter.column);
//					if (adapter.isHierarchical())
//					{
//						System.out.println("IconHighlighter.isHighlighted().isHierarchical(true): o.class="+o.getClass().getName()+", adapter.row="+adapter.row + ", adapter.col="+adapter.column);
//					}
////					if (renderer instanceof OfflineSessionModel.SessionLevel)
////						return true;
//					return true;
//				}
//			}, SwingUtils.readImageIcon(Version.class, "images/asemon_icon_old.gif"))
//			, new IconHighlighter(new HighlightPredicate() 
//			{
//				public boolean isHighlighted(Component renderer, ComponentAdapter adapter) 
//				{
//					return false;
//				}
//			}, SwingUtils.readImageIcon(Version.class, "images/asemon_icon.gif"))
			new ColorHighlighter(new HighlightPredicate() 
			{
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter) 
				{
					return true;
				}
			}, Color.CYAN, Color.RED)
//			HighlighterFactory.createSimpleStriping()
	};






	public class TreeTableNavigationEnhancerXXX
	implements KeyListener 
	{
		private JXTreeTable _treeTable;

		public TreeTableNavigationEnhancerXXX(JXTreeTable treeTable) 
		{
			this._treeTable = treeTable;
		}

		public void keyTyped   (KeyEvent e) { }
		public void keyReleased(KeyEvent e) { }

		public synchronized void keyPressed (KeyEvent e) 
		{
//			KeyEvent.VK_RIGHT;

//			System.out.println("Key is: " + KeyEvent.getKeyText(e.getKeyCode()) + " - " + ((int)e.getKeyCode()));
			int keyCode = e.getKeyCode();
			if (keyCode == KeyEvent.VK_RIGHT) 
			{
//				System.out.println("Right Key");
				int col = _treeTable.getSelectedColumn();
				if (col != 0)
					return;
				int row = _treeTable.getSelectedRow();
				_treeTable.expandRow(row);
				_treeTable.setRowSelectionInterval(row+1,row+1);
				_treeTable.setColumnSelectionInterval(0,0);
				e.consume();
			} 
			else if (keyCode == KeyEvent.VK_LEFT) 
			{
				int col = _treeTable.getSelectedColumn();
				if (col != 0)
					return;
				int row = _treeTable.getSelectedRow();
				if (_treeTable.isExpanded(row)) 
				{
					_treeTable.collapseRow(row);
					_treeTable.setRowSelectionInterval(row,row);
				} 
				else 
				{
					int parentRow = getParentRow(row);
					_treeTable.setRowSelectionInterval(parentRow,parentRow);
				}
				_treeTable.setColumnSelectionInterval(0,0);
				e.consume();
			}
		}

		protected int getParentRow(int row) 
		{
			TreeTableModel m = _treeTable.getTreeTableModel();
			Object childValue = _treeTable.getValueAt(row,0);
			for (int i=row-1; i>=0; i--) 
			{
				if (_treeTable.isExpanded(i)) 
				{
//					System.out.println(treeTable.getValueAt(i,0));
//					System.out.println("\t" + childValue);
					Object p = _treeTable.getValueAt(i,0);
					int x = m.getChildCount(p);
					for (int j=0; j<x; j++)
					{
						if (m.getChild(p,j).equals(childValue))
							return i;
					}

					if (m.getIndexOfChild(_treeTable.getValueAt(i,0),childValue) >= 0) 
					{
						return i;
					}
				}
			}
			return 0;
		}
	}


	public class TreeTableNavigationEnhancer
	implements KeyListener 
	{
		private JXTreeTable _treeTable;

		public TreeTableNavigationEnhancer(JXTreeTable treeTable) 
		{
			this._treeTable = treeTable;
		}

		public void keyTyped   (KeyEvent e) { }
		public void keyReleased(KeyEvent e) { }

		public synchronized void keyPressed (KeyEvent e) 
		{
//			System.out.println("keyPressed():KeyEvent="+e);

			// If return was pressed, do same as "SHOW"
			if (e.getKeyCode() == KeyEvent.VK_ENTER)
			{
				int row = _treeTable.getSelectedRow();
				_logger.info("<---RETURN---> was pressed, currentRow="+row+", value='"+_treeTable.getValueAt(row, 0)+"'.");
				
				doActionShow();
			}

			
			// If selected node is already expanded and if this node has children, select the first of these child nodes.
			// If selected node is NOT expanded, expand it
			if (e.getKeyCode() == KeyEvent.VK_RIGHT)
			{
				if (_treeTable.isExpanded( _treeTable.getSelectedRow()) )
				{
					Object o = _treeTable.getPathForRow(_treeTable.getSelectedRow()).getLastPathComponent();
//					System.out.println("TreeTableNavigationEnhancer2: VK_RIGHT & isExpanded("+_treeTable.getSelectedRow()+"): o = "+o.getClass().getName());
					if ( o instanceof TreeNode)
						if ( ((TreeNode)o).getChildCount() > 0 )
							_treeTable.getSelectionModel().setSelectionInterval(_treeTable.getSelectedRow()+1, _treeTable.getSelectedRow()+1);
//					if ( ((TreeNode)_treeTable.getPathForRow(_treeTable.getSelectedRow()).getLastPathComponent()).getChildCount() > 0)
//						_treeTable.getSelectionModel().setSelectionInterval(_treeTable.getSelectedRow()+1, _treeTable.getSelectedRow()+1);
				}
				else
					_treeTable.expandRow(_treeTable.getSelectedRow());
			}
			// If selected node is expanded, collapse it
			// If selected node is NOT expanded, and if the node has a parent, select the parent node
			else if (e.getKeyCode() == KeyEvent.VK_LEFT)
			{
				if(_treeTable.isExpanded( _treeTable.getSelectedRow() ))
					_treeTable.collapseRow(_treeTable.getSelectedRow());
				else
				{
					int parentRow = getParentRow(_treeTable.getSelectedRow());
					_treeTable.getSelectionModel().setSelectionInterval(parentRow, parentRow);
//					if (_treeTable.getPathForRow(_treeTable.getSelectedRow()).getParentPath().getPathCount() > 1)
//						_treeTable.getSelectionModel().setSelectionInterval(_treeTable.getSelectedRow()-1, _treeTable.getSelectedRow()-1);
				}
			}
		}

		protected int getParentRow(int row) 
		{
			for (int i=row-1; i>=0; i--) 
			{
				if (_treeTable.isExpanded(i)) 
					return i;
			}
			return row;
		}
//		protected int getParentRow(int row) 
//		{
//			TreeTableModel m = _treeTable.getTreeTableModel();
//			Object childValue = _treeTable.getValueAt(row,0);
//			for (int i=row-1; i>=0; i--) 
//			{
//				if (_treeTable.isExpanded(i)) 
//				{
//					System.out.println(_treeTable.getValueAt(i,0));
//					System.out.println("\t" + childValue);
//					Object p = _treeTable.getValueAt(i,0);
//					int x = m.getChildCount(p);
//					for (int j=0; j<x; j++)
//					{
//						if (m.getChild(p,j).equals(childValue))
//							return i;
//					}
//
//					if (m.getIndexOfChild(_treeTable.getValueAt(i,0),childValue) >= 0) 
//					{
//						return i;
//					}
//				}
//			}
//			return row;
//		}
	}

	/*---------------------------------------------------
	** BEGIN: Watermark stuff
	**---------------------------------------------------
	*/
	public void setWatermarkText(String str)
	{
		_watermark.setWatermarkText(str);
	}
	public void setWatermark()
	{
		// Find out in what state we are and set the water mark to something good.
		setWatermarkText(null);
	}


	private class Watermark
    extends AbstractComponentDecorator
    {
		public Watermark(JComponent target, String text)
		{
			super(target);
			if (text == null)
				text = "";
			_textBr = text.split("\n");
		}
		private String[]    _textBr = null; // Break Lines by '\n'
		private Graphics2D  g       = null;
		private Rectangle   r       = null;
	
		public void paint(Graphics graphics)
		{
			if (_textBr == null || _textBr != null && _textBr.length < 0)
				return;
	
			r = getDecorationBounds();
			g = (Graphics2D) graphics;
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Font f = g.getFont();
			g.setFont(f.deriveFont(Font.BOLD, f.getSize() * 2.0f));
			g.setColor(new Color(128, 128, 128, 128));

			FontMetrics fm = g.getFontMetrics();
			int maxStrWidth  = 0;
			int maxStrHeight = fm.getHeight();

			// get max with for all of the lines
			for (int i=0; i<_textBr.length; i++)
			{
				int CurLineStrWidth  = fm.stringWidth(_textBr[i]);
				maxStrWidth = Math.max(maxStrWidth, CurLineStrWidth);
			}
			int xPos = (r.width - maxStrWidth) / 2;
			int yPos = (int) (r.height - ((r.height - fm.getHeight()) / 2) * 0.6);

			// Print all the lines
			for (int i=0; i<_textBr.length; i++)
			{
				g.drawString(_textBr[i], xPos, (yPos+(maxStrHeight*i)) );
			}
		}
	
		public void setWatermarkText(String text)
		{
			if (text == null)
				text = "";

			_textBr = text.split("\n");
			_logger.debug("setWatermarkText: to '" + text + "'.");

			repaint();
		}
    }
	/*---------------------------------------------------
	** END: Watermark stuff
	**---------------------------------------------------
	*/
}


//treeTable.addKeyListener(new TreeTableNavigationEnhancer(treeTable));
//
//treeTable.getColumnModel().getColumn(0).setPreferredWidth(350);
//treeTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
//    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
//        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
//        setHorizontalAlignment(SwingConstants.RIGHT);
//        setText(getText() + "   ");
//        return this;
//    }
//});


//In any event, I solved it without diving into the JXTreeTable itself (I've had a bunch of compatability issues during upgrade so I prefer to stay neutral) by the following code in a MouseListener. Just in case anyone require a similar temporary workaround, this is not pretty, but it demonstrates how to emulate left/right arrow key semantics:
//
//        // If selected node is already expanded and if this node has children, select the first of these child nodes.
//        // If selected node is NOT expanded, expand it
//        if(evt.getKeyCode() == KeyEvent.VK_RIGHT)
//        {
//            if(treeTable.isExpanded( treeTable.getSelectedRow()) )
//            {
//                if( ((TreeNode)treeTable.getPathForRow(treeTable.getSelectedRow()).getLastPathComponent()).getChildCount() > 0)
//                    treeTable.getSelectionModel().setSelectionInterval(treeTable.getSelectedRow()+1, treeTable.getSelectedRow()+1);
//            }
//            else
//                treeTable.expandRow(treeTable.getSelectedRow());
//        }
//        // If selected node is expanded, collapse it
//        // If selected node is NOT expanded, and if the node has a parent, select the parent node
//        else if(evt.getKeyCode() == KeyEvent.VK_LEFT)
//        {
//            if(treeTable.isExpanded( treeTable.getSelectedRow() ))
//                treeTable.collapseRow(treeTable.getSelectedRow());
//            else
//            {
//                if(treeTable.getPathForRow(treeTable.getSelectedRow()).getParentPath().getPathCount() > 1)
//                    treeTable.getSelectionModel().setSelectionInterval(treeTable.getSelectedRow()-1, treeTable.getSelectedRow()-1);
//            }
//        }
