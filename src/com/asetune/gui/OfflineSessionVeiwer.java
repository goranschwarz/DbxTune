/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */

package com.asetune.gui;

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
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
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
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.treetable.TreeTableModel;

import com.asetune.CounterController;
import com.asetune.CounterControllerAbstract;
import com.asetune.Version;
import com.asetune.config.dbms.DbmsConfigManager;
import com.asetune.config.dbms.DbmsConfigTextManager;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.config.ui.DbmsConfigViewDialog;
import com.asetune.gui.OfflineSessionModel.SessionLevel;
import com.asetune.gui.swing.AbstractComponentDecorator;
import com.asetune.pcs.PersistReader;
import com.asetune.pcs.PersistReader.SessionInfo;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class OfflineSessionVeiwer 
extends JFrame
//implements Runnable, ActionListener//, TableModelListener
implements ActionListener, PersistReader.INotificationListener//, TableModelListener
{
	private static Logger _logger = Logger.getLogger(OfflineSessionVeiwer.class);
	private static final long serialVersionUID = -113668702537883167L;

	private static final Color TAB_CMNAMESUM_COL_BG = new Color(240, 240, 240);

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
	private JComboBox<String>      _day_cbx         = new JComboBox<String>(new String[] {"1"});
	private JComboBox<String>      _hour_cbx        = new JComboBox<String>(new String[] {"1", "2", "3", "4", "6", "12"});
	private JComboBox<String>      _minute_cbx      = new JComboBox<String>(new String[] {"1", "2", "5", "10", "15", "20", "30"});
	private int                    _dayLevel        = 1;
	private int                    _hourLevel       = 1;
	private int                    _minuteLevel     = 10;
//	private JTextField             _day_txt         = new JTextField();
//	private JTextField             _hour_txt        = new JTextField();
//	private JTextField             _minute_txt      = new JTextField();
	private JButton                _refresh_but     = new JButton("Refresh");
	private JCheckBox              _showCounterInfo_chk = new JCheckBox("Show Counter Info", false);
	private JCheckBox              _showCiAsSamples_chk = new JCheckBox("As Samples",        true);
	private JCheckBox              _linkSliderTree_chk  = new JCheckBox("Link Slider with Tree", true);

	// Bottom panel
	private JLabel                 _status_lbl      = new JLabel("");
	private JButton                _show_but        = new JButton("Show");

	
	private Watermark              _watermark       = null;
	private int                    _colSessionsPrefSize = 250;
	private int                    _colSessionsActSize  = 250;

	// Remember what period we have currently loaded in the GUI
	private Timestamp              _currentPeriodStartTime = null;
	private Timestamp              _currentPeriodEndTime   = null;

	
	
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
//		if (_owner != null)
//			setIconImage(_owner.getIconImage());
		if (_owner != null)
			setIconImages(_owner.getIconImages());
		
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
		_showCounterInfo_chk.setToolTipText(
				"<html>" +
				  "Show how many <b>rows</b> each individual Performance Counter Sample has.<br>" +
				  "This is presented to the right hand side.<br>" +
				  "<b>NOTE:</b> This doesn't work wery well yet." +
				"</html>");
		_showCiAsSamples_chk.setToolTipText(
				"<html>" +
				  "Show how many <b>samples</b> each individual Performance Counter Sample has.<br>" +
				  "This is presented to the right hand side.<br>" +
				  "<b>NOTE:</b> This doesn't work wery well yet." +
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

		// Just a dummy to make formating easier, everything after this will be on the right hand side
		panel.add(new JLabel(""),  "grow, push, right");
		
		panel.add(_showCounterInfo_chk, "");
		panel.add(_showCiAsSamples_chk, "");
		panel.add(_refresh_but, "");

		return panel;
	}

	private JPanel createBottomPanel() 
	{
		JPanel panel = SwingUtils.createPanel("Bottom", false);
		panel.setLayout(new MigLayout("ins 3 10 3 3", //ins T L B R???
				"",
				""));

		_linkSliderTree_chk.setToolTipText(
				"<html>" +
				  "When you click on the view-slider or graph-content, to position what data to view.<br>" +
				  "Make the selected timestamp node visible in the tree view as well." +
				"</html>");

		_show_but   .setToolTipText("Show the selected sample period in the summary graphs.");
		
		panel.add(_status_lbl,  "grow, push, left");

		// Just a dummy to make formating easier, everything after this will be on the right hand side
		panel.add(new JLabel(""),  "grow, push, right");

		panel.add(_linkSliderTree_chk, "");
		panel.add(_show_but,    "");
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

			@Override
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
//			/** set some color to CM NAME SUM COLUMNS */
//			public Component prepareRenderer(TableCellRenderer renderer, int row, int column)
//			{
//				Component c = super.prepareRenderer(renderer, row, column);
//				if (column >= 5) // FIXME: 5 should NOT be hard coded...
//					c.setBackground(TAB_CMNAMESUM_COL_BG);
//				return c;
//			}
		};

//		if (_conn == null)
//			_conn = getOfflineConnection();

//		_model = new OfflineSessionModel(_conn);
		_model = new OfflineSessionModel(_showCounterInfo_chk.isSelected(), _showCiAsSamples_chk.isSelected());
//		_model.init(null);

		_treeTable.setTreeTableModel(_model);
		_treeTable.setTreeCellRenderer(new IconRenderer());

		_treeTable.setHighlighters(_highliters);

//		_treeTable.setModel(GuiLogAppender.getTableModel());
		_treeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_treeTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		_treeTable.packAll(); // set size so that all content in all cells are visible
		_treeTable.setSortable(true);
		_treeTable.setColumnControlVisible(true);

//		_treeTable.addKeyListener(new TreeTableNavigationEnhancer(_treeTable));
		TreeTableNavigationEnhancer ttne = new TreeTableNavigationEnhancer(_treeTable);
		_treeTable.addKeyListener(ttne);
		_treeTable.addMouseListener(ttne);
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

		JPopupMenu popup    = new JPopupMenu();
		JMenuItem show      = new JMenuItem("Show");
		JMenuItem cfgView   = new JMenuItem("View DMBS Configuration...");
		JMenuItem pcsDbInfo = new JMenuItem("Application Version that persisted this session...");

		popup.add(show);
		popup.add(cfgView);
		popup.add(pcsDbInfo);

		show.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				doActionShow();
			}
		});

		cfgView.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				doActionShowAseConfig();
			}
		});

		pcsDbInfo.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				doActionShowPcsDbInfo();
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
	** BEGIN: implementing: PersistReader.INotificationListener
	**---------------------------------------------------
	*/
	@Override
	public void setWatermark(String text)
	{
		setWatermarkText(text);
	}
	@Override
	public void setStatusText(String status)
	{
		_status_lbl.setText(status);
	}
	/** Called by PersistReader when <code>loadSessionList()</code> has been executed. */
	@Override
	public void setSessionList(List<SessionInfo> sessionList)
	{
		refreshTable(sessionList);
	}

	/*---------------------------------------------------
	** END: implementing: PersistReader.INotificationListener
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
	private void refreshTable(List<SessionInfo> sessionList)
	{
		refreshTable(this.isVisible(), sessionList);
	}
	private void refreshTable(boolean saveGuiStuff, List<SessionInfo> sessionList)
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
		_model = new OfflineSessionModel(_showCounterInfo_chk.isSelected(), _showCiAsSamples_chk.isSelected());
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
			@Override
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
			@Override
			public void windowClosing(WindowEvent e)
			{
				saveProps();
			}
		});
	}

	@Override
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

//	/**
//	 * Set Busy/wait cusor or the default cursor.
//	 */
//	public void setBusyCursor(boolean to)
//	{
//System.out.println("OfflineSessionViewer: Setting busy cursor to: "+to);
//		Cursor cursor = to ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor();
//		Frame.getFrames()[0].setCursor(cursor);
//	}


	private void loadSessionLevel(PersistReader reader, SessionLevel sl)
	{
		reader.loadSessionGraphs (sl.getSampleId(), sl.getPeriodStartTime(), sl.getPeriodEndTime(), sl._numOfSamples);
		reader.loadTimelineSlider(sl.getSampleId(), sl.getPeriodStartTime(), sl.getPeriodEndTime());

		_currentPeriodStartTime = sl.getPeriodStartTime();
		_currentPeriodEndTime   = sl.getPeriodEndTime();

		// Read in the configuration for this period
//		AseConfig     .getInstance().initialize(reader.getConnection(), true, true, sl.getPeriodStartTime());
////		AseCacheConfig.getInstance().initialize(reader.getConnection(), true, true, sl.getPeriodStartTime());
//		AseConfigText.initializeAll(reader.getConnection(), true, true, sl.getPeriodStartTime());

		try
		{
			if (DbmsConfigManager.hasInstance())
				DbmsConfigManager.getInstance().initialize(reader.getConnection(), true, true, sl.getPeriodStartTime());

			if (DbmsConfigTextManager.hasInstances())
				DbmsConfigTextManager.initializeAll(reader.getConnection(), true, true, sl.getPeriodStartTime());
		}
		catch(SQLException ex) 
		{
			_logger.info("Initialization of the DBMS Configuration did not succeed. Caught: "+ex); 
		}

		// load the column tool tip (if any was saved)
		if (MonTablesDictionaryManager.hasInstance())
			reader.loadMonTablesDictionary(sl.getSampleId());
		
		// Read User Defined Counters, to check for any new UDC that isn't loaded/created
		Configuration udcConf = reader.getUdcProperties(sl.getSampleId());
		if (udcConf != null && udcConf.size() > 0)
		{
			_logger.info("Loading UDC, User Defined Counters from Offline Storage Database into the GUI.");
			CounterControllerAbstract.createUserDefinedCounterModels(udcConf);
		}
		else
		{
			_logger.info("No UDC, User Defined Counters was found in the Offline Storage Database.");
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

		if (o instanceof SessionLevel)
		{
			SessionLevel sl = (SessionLevel) o;
			loadSessionLevel(reader, sl);
		}
		else if (o instanceof Timestamp)
		{
			Timestamp ts = (Timestamp) o;
//			reader.loadSessionCms (ts);

			// Check if this timestamp parent has been loaded
			boolean loadSuper = false;
			if (_currentPeriodStartTime == null || _currentPeriodEndTime == null)
				loadSuper = true;
			if (loadSuper == false)
			{
				// If not within current loaded period
				if ( ! (ts.getTime() >= _currentPeriodStartTime.getTime() && ts.getTime() <= _currentPeriodEndTime.getTime()) )
					loadSuper = true;
			}
			// Load the parent into the GUI
			if (loadSuper)
			{
				OfflineSessionModel model = (OfflineSessionModel) _treeTable.getTreeTableModel();
				List<Object> ol = model.getObjectPathForSampleTime(ts);
				if (ol.size() > 0)
				{
					Object parentObj = ol.get( ol.size() - 2 ); // -2 would be next last...
					if (parentObj instanceof SessionLevel)
						loadSessionLevel(reader, (SessionLevel)parentObj);
				}
			}

			// Load current timestamp into the GUI
			reader.loadSessionCmIndicators(ts);
			reader.loadSummaryCm(ts);
		}

		final TrendGraphDashboardPanel tgdp = CounterController.getSummaryPanel().getGraphPanel();
		if (tgdp != null)
		{
			// FIXME: This do not seems to work... maybe do it in a timer with a delay of 500ms instead
			SwingUtilities.invokeLater(new Runnable() { @Override public void run() { tgdp.reLayout(); } });
		}
	}

	public void doActionShowAseConfig()
	{
		int row = _treeTable.getSelectedRow();
		Object o = _treeTable.getValueAt(row, 0);
//		_logger.info("SHOW_BUT was pressed, currentRow="+row+", value='"+o+"'.");

		PersistReader reader = PersistReader.getInstance();
		if (reader == null)
			throw new RuntimeException("The 'PersistReader' has not been initialized.");

		if (o instanceof SessionLevel)
		{
			SessionLevel sl = (SessionLevel) o;

			// Read in the configuration for this period
//			AseConfig     .getInstance().initialize(reader.getConnection(), true, true, sl.getPeriodStartTime());
//			//AseCacheConfig.getInstance().initialize(reader.getConnection(), true, true, sl.getPeriodStartTime());
//			AseConfigText.initializeAll(reader.getConnection(), true, true, sl.getPeriodStartTime());

			try
			{
				if (DbmsConfigManager.hasInstance())
					DbmsConfigManager.getInstance().initialize(reader.getConnection(), true, true, sl.getPeriodStartTime());

				if (DbmsConfigTextManager.hasInstances())
					DbmsConfigTextManager.initializeAll(reader.getConnection(), true, true, sl.getPeriodStartTime());
			}
			catch(SQLException ex) 
			{
				_logger.info("Initialization of the DBMS Configuration did not succeed. Caught: "+ex); 
			}
		}
		
		DbmsConfigViewDialog.showDialog(this, reader);
	}
	
	public void doActionShowPcsDbInfo()
	{
		int row = _treeTable.getSelectedRow();
		Object o = _treeTable.getValueAt(row, 0);
//		_logger.info("SHOW_BUT was pressed, currentRow="+row+", value='"+o+"'.");

		PersistReader reader = PersistReader.getInstance();
		if (reader == null)
			throw new RuntimeException("The 'PersistReader' has not been initialized.");

		if (o instanceof SessionLevel)
		{
			SessionLevel sl = (SessionLevel) o;

			// Read the DBINFO for this period
			PersistReader.MonVersionInfo monVersionInfo = reader.getMonVersionInfo(sl.getPeriodStartTime());

			if (monVersionInfo != null)
			{
				String msg = 
					"<HTML>" +
					"<TABLE BORDER=0 CELLSPACING=0 CELLPADDING=1>" +
					"   <TR> <TD><B>Name:            <B>&nbsp;</TD> <TD>"+ monVersionInfo._productString +"</TD> </TR>" +
					"   <TR> <TD><B>                 <B>&nbsp;</TD> <TD></TD> </TR>" +
					"   <TR> <TD><B>Version:         <B>&nbsp;</TD> <TD>"+ monVersionInfo._versionString +"</TD> </TR>" +
					"   <TR> <TD><B>Build:           <B>&nbsp;</TD> <TD>"+ monVersionInfo._buildString   +"</TD> </TR>" +
					"   <TR> <TD><B>                 <B>&nbsp;</TD> <TD></TD> </TR>" +
					"   <TR> <TD><B>Source Date:     <B>&nbsp;</TD> <TD>"+ monVersionInfo._sourceDate    +"</TD> </TR>" +
					"   <TR> <TD><B>Source Revision: <B>&nbsp;</TD> <TD>"+ monVersionInfo._sourceRev     +"</TD> </TR>" +
//					"   <TR> <TD><B>DB Product Name: <B>&nbsp;</TD> <TD>"+ monVersionInfo._dbProductName +"</TD> </TR>" +
					"</TABLE>" +
					"</HTML>";
				SwingUtils.showInfoMessage(this, Version.getAppName()+" Version used when storing counters", msg);
			}
		}
	}

	/*---------------------------------------------------
	** END: Action Listeners
	**---------------------------------------------------
	*/

	/**
	 * Make the node having <code>Timestamp</code> visible in the tree view.
	 */
//	int xxxx=0;
	public void setCurrentSampleTimeView(Timestamp ts)
	{
		if (_linkSliderTree_chk.isSelected())
		{
			// FIXME: Get the correct node in the list, and show it in the tree view
//			System.out.println("FIXME: Get the correct node in the list, and show it in the tree view... ts='"+ts+"'.");

////			_treeTable.getSelectionModel().setSelectionInterval(xxxx, xxxx); xxxx++;
//			TreePath tp  = _model.getTreePathForSampleTime(ts);
//			TreePath ctp = _treeTable.getTreeSelectionModel().getSelectionPath();
////System.out.println("CurrentTreeSelectionPath="+ctp);
////System.out.println("TreePathArray="+tp);
////System.out.println("getRowForPath="+_treeTable.getRowForPath(tp));
//			_treeTable.getTreeSelectionModel().setSelectionPath(tp);
//			_treeTable.expandPath(tp);
//			_treeTable.scrollPathToVisible(tp);
		}
	}
	

	private void saveProps()
  	{
		Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
		String base = "offlineSessionViewer.";

		if (tmpConf != null)
		{
			tmpConf.setLayoutProperty(base + "window.width",  this.getSize().width);
			tmpConf.setLayoutProperty(base + "window.height", this.getSize().height);
			tmpConf.setLayoutProperty(base + "window.pos.x",  this.getLocationOnScreen().x);
			tmpConf.setLayoutProperty(base + "window.pos.y",  this.getLocationOnScreen().y);

			tmpConf.setProperty(base + "colSessionsPrefSize", _colSessionsPrefSize);
			tmpConf.setProperty(base + "colSessionsActSize",  _colSessionsActSize);

			tmpConf.setProperty(base + "dayLevel",    _dayLevel);
			tmpConf.setProperty(base + "hourLevel",   _hourLevel);
			tmpConf.setProperty(base + "minuteLevel", _minuteLevel);
			
			tmpConf.setProperty(base + "option.showCounterInfo",          _showCounterInfo_chk.isSelected());
			tmpConf.setProperty(base + "option.showCounterInfoAsSamples", _showCiAsSamples_chk.isSelected());
			tmpConf.setProperty(base + "option.linkSliderTree",           _linkSliderTree_chk.isSelected());

			tmpConf.save();
		}
  	}

  	private void loadProps()
  	{
		int     width     = SwingUtils.hiDpiScale(600);
		int     height    = SwingUtils.hiDpiScale(350);
		int     x         = -1;
		int     y         = -1;

//		Configuration tmpConf = Configuration.getInstance(Configuration.TEMP);
		Configuration tmpConf = Configuration.getCombinedConfiguration();
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

		boolean bool;
		bool = tmpConf.getBooleanProperty(base + "option.showCounterInfo", _showCounterInfo_chk.isSelected());
		_showCounterInfo_chk.setSelected(bool);

		bool = tmpConf.getBooleanProperty(base + "option.showCounterInfoAsSamples", _showCiAsSamples_chk.isSelected());
		_showCiAsSamples_chk.setSelected(bool);

		bool = tmpConf.getBooleanProperty(base + "option.linkSliderTree", _linkSliderTree_chk.isSelected());
		_linkSliderTree_chk.setSelected(bool);


		// Set initial size
//		int defWidth  = (3 * Toolkit.getDefaultToolkit().getScreenSize().width)  / 4;
//		int defHeight = (3 * Toolkit.getDefaultToolkit().getScreenSize().height) / 4;

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
		Configuration.setInstance(Configuration.USER_TEMP, conf);

//		OfflineSessionVeiwer view = new OfflineSessionVeiwer(null, null, true);
		OfflineSessionVeiwer view = new OfflineSessionVeiwer(null);
		view.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		view.setVisible(true);
	}


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

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value,	boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) 
		{
//			return super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			Component scomp = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

			if (value instanceof OfflineSessionModel.MinuteLevel)
			{
				setIcon(minuteIcon);
				setText( ((OfflineSessionModel.SessionLevel)value).getDisplayString() );
			}
			else if (value instanceof OfflineSessionModel.HourLevel)
			{
				setIcon(hourIcon);
				setText( ((OfflineSessionModel.SessionLevel)value).getDisplayString() );
			}
			else if (value instanceof OfflineSessionModel.DayLevel)
			{
				setIcon(dayIcon);
				setText( ((OfflineSessionModel.SessionLevel)value).getDisplayString() );
			}
			else if (value instanceof OfflineSessionModel.SessionLevel)
			{
				setIcon(sessionIcon);
				setText( ((OfflineSessionModel.SessionLevel)value).getDisplayString() );
			}
			else 
			{
				setIcon(sampleIcon);
			}
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
//						Icon icon = SwingUtils.readImageIcon(Version.class, "images/asetune_icon_old.gif");
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
//			}, SwingUtils.readImageIcon(Version.class, "images/asetune_icon_old.gif"))
//			, new IconHighlighter(new HighlightPredicate() 
//			{
//				public boolean isHighlighted(Component renderer, ComponentAdapter adapter) 
//				{
//					return false;
//				}
//			}, SwingUtils.readImageIcon(Version.class, "images/asetune_icon.gif"))
//			new ColorHighlighter(new HighlightPredicate() 
//			{
//				public boolean isHighlighted(Component renderer, ComponentAdapter adapter) 
//				{
//					return true;
//				}
//			}, Color.CYAN, Color.RED)
//			HighlighterFactory.createSimpleStriping()
//			new ColorHighlighter(new HighlightPredicate() 
//			{
//				public boolean isHighlighted(Component renderer, ComponentAdapter adapter) 
//				{
//					return adapter.column > 4; // FIXME: 4 should NOT be hard coded...
//				}
//			}, TAB_CMNAMESUM_COL_BG, Color.BLACK)
			
			new CmSummaryFieldHighlighter(new HighlightPredicate() 
			{
				@Override
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter) 
				{
					return adapter.column > 4; // FIXME: 4 should NOT be hard coded...
				}
			})
	};

	private static class CmSummaryFieldHighlighter extends AbstractHighlighter
	{
		public CmSummaryFieldHighlighter(HighlightPredicate predicate)
		{
			super(predicate);
		}

		@Override
		protected Component doHighlight(Component comp, ComponentAdapter adapter)
		{
			Object value = adapter.getFilteredValueAt(adapter.row, adapter.convertColumnIndexToModel(adapter.column));
			if (comp instanceof JLabel)
	            ((JLabel) comp).setHorizontalAlignment(SwingConstants.RIGHT);

			if ("---".equals(value))
			{
//				if (comp instanceof JLabel)
//		            ((JLabel) comp).setHorizontalAlignment(SwingConstants.CENTER);
				comp.setForeground(Color.BLUE);
			}
			
			comp.setBackground(TAB_CMNAMESUM_COL_BG);
			return comp;
		}
	}





	public class TreeTableNavigationEnhancerXXX
	implements KeyListener 
	{
		private JXTreeTable _treeTable;

		public TreeTableNavigationEnhancerXXX(JXTreeTable treeTable) 
		{
			this._treeTable = treeTable;
		}

		@Override public void keyTyped   (KeyEvent e) { }
		@Override public void keyReleased(KeyEvent e) { }

		@Override
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
	implements KeyListener, MouseListener
	{
		private JXTreeTable _treeTable;

		public TreeTableNavigationEnhancer(JXTreeTable treeTable) 
		{
			this._treeTable = treeTable;
		}

		@Override public void keyTyped   (KeyEvent e) { }
		@Override public void keyReleased(KeyEvent e) { }

		@Override
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

		@Override
		public void mouseClicked(MouseEvent e)
		{
			if (e.getClickCount() == 2 && !e.isConsumed()) 
			{
				e.consume();
				doActionShow();
			}
		}

		@Override
		public void mousePressed(MouseEvent e)
		{
		}

		@Override
		public void mouseReleased(MouseEvent e)
		{
		}

		@Override
		public void mouseEntered(MouseEvent e)
		{
		}

		@Override
		public void mouseExited(MouseEvent e)
		{
		}
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
	
		@Override
		public void paint(Graphics graphics)
		{
			if (_textBr == null || _textBr != null && _textBr.length < 0)
				return;
	
			r = getDecorationBounds();
			g = (Graphics2D) graphics;
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Font f = g.getFont();
//			g.setFont(f.deriveFont(Font.BOLD, f.getSize() * 2.0f));
			g.setFont(f.deriveFont(Font.BOLD, f.getSize() * 2.0f * SwingUtils.getHiDpiScale() ));
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
