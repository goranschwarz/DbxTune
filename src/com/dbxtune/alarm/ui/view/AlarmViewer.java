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
package com.dbxtune.alarm.ui.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.table.JTableHeader;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.jdesktop.swingx.JXTableHeader;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.table.TableUtilities;

import com.dbxtune.Version;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEvent.Severity;
import com.dbxtune.alarm.writers.AlarmWriterToTableModel;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.gui.TrendGraphColors;
import com.dbxtune.gui.swing.GTable;
import com.dbxtune.gui.swing.GTableFilter;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class AlarmViewer
//extends JDialog
extends JFrame
implements ActionListener
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long serialVersionUID = 1L;

	// PANEL: OK-CANCEL
//	private JButton                _ok               = new JButton("OK");
//	private JButton                _cancel           = new JButton("Cancel");
//	private JButton                _apply            = new JButton("Apply");
	private JButton                _close            = new JButton("Close");
//	private boolean                _madeChanges      = false;

//	private   GPanel            _activeAlarms_pan;
//	private   GPanel            _historyAlarms_pan;
	private   JPanel            _top_pan;
	private   JPanel            _activeAlarms_pan;
	private   JPanel            _historyAlarms_pan;
//	private   JPanel            _okCancel_pan;
	private   JPanel            _close_pan;

	private   JSplitPane        _splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

	private JButton _openSendDummyEvent_but = new JButton("<html>Send <i>Test</i> Alarm</html>");
	
	private LocalActiveTable  _activeTable;
	private LocalHistoryTable _historyTable;

	private JCheckBox _historyMoveToLastAddedEntry_chk = new JCheckBox("Scroll to Last Added Entry", true);

	public AlarmViewer(Window owner)
	{
//		super(owner, "Alarm View", ModalityType.MODELESS);
//		super(owner, "Alarm View");
//		setModal(false);

		super("Alarm View");

		init();
		loadProps();
	}

	
	private void init()
	{
		setTitle("Alarm View"); // Set window title
		
		// Set the icon, if we "just" do setIconImage() on the JDialog
		// it will not be the "correct" icon in the Alt-Tab list on Windows
		// So we need to grab the owner, and set that since the icon is grabbed from the owner...
		ImageIcon icon16 = SwingUtils.readImageIcon(Version.class, "images/alarm_view_16.png");
		ImageIcon icon32 = SwingUtils.readImageIcon(Version.class, "images/alarm_view_32.png");
		if (icon16 != null || icon32 != null)
		{
			ArrayList<Image> iconList = new ArrayList<Image>();
			if (icon16 != null) iconList.add(icon16.getImage());
			if (icon32 != null) iconList.add(icon32.getImage());

			Object owner = getOwner();
			if (owner != null && owner instanceof Frame)
				((Frame)owner).setIconImages(iconList);
			else
				setIconImages(iconList);
		}

		_top_pan                     = createTopPanel();
		_activeAlarms_pan            = createActiveTablePanel();
		_historyAlarms_pan           = createHistoryTablePanel();
//		_okCancel_pan                = createOkCancelPanel();
		_close_pan                   = createClosePanel();

		_splitPane          = new JSplitPane(JSplitPane.VERTICAL_SPLIT, _activeAlarms_pan,      _historyAlarms_pan);

		setLayout( new MigLayout("insets 0 0 0 0") );
		add(_top_pan,            "grow, push, wrap");
		add(_splitPane,          "grow, push, wrap");
//		add(_okCancel_pan,       "right, wrap");
		add(_close_pan,          "right, wrap");

		pack();
		getSavedWindowProps();

		this.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				saveProps();
			}
		});
	}

	private JPanel createTopPanel()
	{
		JPanel panel = SwingUtils.createPanel("Top Panel", false);
//		panel.setLayout(new MigLayout("insets 0 0 0 0", "", ""));
		panel.setLayout(new MigLayout("", "", ""));

//		panel.setToolTipText("<html>Describe this</html>");

		String description = "<html>"
				+ "View Alarms that has been created/generated by "+Version.getAppName()+"<br>"
				+ "You can see both the Active alarms and alarms that has been generated earlier in this session.<br>"
				+ "<i>Tip: </i>Press button <i>Send Test Alarm</i> in the upper right corner to generate dummy alarms, so you can simulate alarms..."
				+"</html>";

		panel.add(new JLabel(description), "split");
		panel.add(new JLabel(),            "growx, pushx");		
		panel.add(_openSendDummyEvent_but, "top, right, wrap");		

		_openSendDummyEvent_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				DummyEventDialog.showDialog(AlarmViewer.this);
			}
		});
		
		return panel;
	}

//	private JPanel createOkCancelPanel()
//	{
//		JPanel panel = new JPanel();
//		panel.setLayout(new MigLayout("","",""));   // insets Top Left Bottom Right
//
//		// ADD the OK, Cancel, Apply buttons
//		panel.add(_ok,     "tag ok, right");
//		panel.add(_cancel, "tag cancel");
//		panel.add(_apply,  "tag apply");
//
//		_apply.setEnabled(false);
//
//		// ADD ACTIONS TO COMPONENTS
////		_ok           .addActionListener(this);
////		_cancel       .addActionListener(this);
////		_apply        .addActionListener(this);
//
//		return panel;
//	}

	private JPanel createClosePanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("","",""));   // insets Top Left Bottom Right

		// ADD the OK, Cancel, Apply buttons
		panel.add(_close,     "tag ok, right");

		// ADD ACTIONS TO COMPONENTS
		_close        .addActionListener(this);

		return panel;
	}

	private JPanel createActiveTablePanel()
	{
		JPanel panel = SwingUtils.createPanel("Active Alarms", true);
		panel.setLayout(new MigLayout("insets 0 0 0 0", "", ""));

		// Create the table
		_activeTable = new LocalActiveTable();

		GTableFilter filter = new GTableFilter(_activeTable, GTableFilter.ROW_COUNT_LAYOUT_LEFT, true);
		JScrollPane scroll = new JScrollPane(_activeTable);

		panel.add(filter, "pushx, growx, wrap");
		panel.add(scroll, "push, grow, height 100%, wrap");

		return panel;
	}

	private JPanel createHistoryTablePanel()
	{
		JPanel panel = SwingUtils.createPanel("Alarm History", true);
		panel.setLayout(new MigLayout("insets 0 0 0 0", "", ""));

		// Create the table
		_historyTable = new LocalHistoryTable();

		GTableFilter filter = new GTableFilter(_historyTable, GTableFilter.ROW_COUNT_LAYOUT_LEFT, true);
		JScrollPane scroll = new JScrollPane(_historyTable);
		JButton clear = new JButton("Clear History");
		clear.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				AlarmWriterToTableModel.getInstance().getHistoryTableModel().clear(true);
			}
		});
//		filter.setText("where Action in('RAISE', 'CANCEL')");
		filter.setText("where action in('RAISE', 'CANCEL')");

		panel.add(filter,                            "pushx, growx, wrap");
		panel.add(clear,                             "split");
		panel.add(_historyMoveToLastAddedEntry_chk,  "wrap");
		panel.add(scroll,                            "push, grow, height 100%, wrap");

		return panel;
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
		int width  = conf.getLayoutProperty("alarmView.dialog.window.width",  SwingUtils.hiDpiScale(900));
		int height = conf.getLayoutProperty("alarmView.dialog.window.height", SwingUtils.hiDpiScale(740));
		int x      = conf.getLayoutProperty("alarmView.dialog.window.pos.x",  -1);
		int y      = conf.getLayoutProperty("alarmView.dialog.window.pos.y",  -1);
		if (width != -1 && height != -1)
		{
			this.setSize(width, height);
		}
		if (x != -1 && y != -1)
		{
			if ( ! SwingUtils.isOutOfScreen(x, y, width, height) )
				this.setLocation(x, y);
		}
		
		int divLoc = conf.getLayoutProperty("alarmView.dialog.splitPane.dividerLocation",  -1);
		if (divLoc > 0)
		{
			_splitPane.setDividerLocation(divLoc);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		saveProps();
		setVisible(false);
	}

	private void loadProps()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		if (conf == null)
		{
			_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
			return;
		}

		_historyMoveToLastAddedEntry_chk.setSelected(conf.getBooleanProperty("alarmView.history.table.moveToLastAddedEntry", true));
	}

	private void saveProps()
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		if (conf == null)
		{
			_logger.warn("Getting Configuration for TEMP failed, probably not initialized");
			return;
		}

		conf.setProperty("alarmView.history.table.moveToLastAddedEntry", _historyMoveToLastAddedEntry_chk.isSelected());
		
		//------------------
		// WINDOW
		//------------------
		if (_splitPane != null)
			conf.setLayoutProperty("alarmView.dialog.splitPane.dividerLocation",  _splitPane.getDividerLocation());

		conf.setLayoutProperty("alarmView.dialog.window.width",  this.getSize().width);
		conf.setLayoutProperty("alarmView.dialog.window.height", this.getSize().height);
		conf.setLayoutProperty("alarmView.dialog.window.pos.x",  this.getLocationOnScreen().x);
		conf.setLayoutProperty("alarmView.dialog.window.pos.y",  this.getLocationOnScreen().y);

		conf.save();
	}


	
	//////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////
	//// SUB-CLASSES: LocalTable & LocalTableModel ///////////////
	//////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////
	
	/*---------------------------------------------------
	** BEGIN: class LocalActiveTable
	**---------------------------------------------------
	*/
	/** Extend the GTable */
	private class LocalActiveTable extends GTable
	{
		private static final long serialVersionUID = 0L;
//		protected int           _lastTableHeaderPointX = -1;
		protected int           _lastTableHeaderColumn = -1;
		private   JPopupMenu    _popupMenu             = null;
		private   JPopupMenu    _headerPopupMenu       = null;
		private   boolean       _isOffline             = false;

		LocalActiveTable()
		{
			super("alarmView.activeTable");
			setModel( AlarmWriterToTableModel.getInstance().getActiveTableModel() );

			if (MainFrame.isOfflineConnected())
			{
				DbxConnection conn = MainFrame.getOfflineConnection();

				String sql = conn.quotifySqlString("select * from [MonAlarmActive]");
						
				try ( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
				{
					ResultSetTableModel rstm = new ResultSetTableModel(rs, "alarmView.activeTable");
					setModel( rstm );
					_isOffline = true;
				}
				catch(SQLException ex)
				{
					_logger.error("Problems loading Offline Active Alarms", ex);
				}
			}
			
//			setShowGrid(false);
			setSortable(true);
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			packAll(); // set size so that all content in all cells are visible
			setColumnControlVisible(true);
//			setHighlighters(_highliters);

			addHighlighters();

			// Create some PopupMenus and attach them
			_popupMenu = createDataTablePopupMenu();
			if (_popupMenu != null)
				setComponentPopupMenu(getDataTablePopupMenu());

			_headerPopupMenu = createDataTableHeaderPopupMenu();
			if (_headerPopupMenu != null)
				getTableHeader().setComponentPopupMenu(getDataTableHeaderPopupMenu());

			// Populate the table
			refreshTable();
		}

		private void addHighlighters()
		{
			Configuration conf = Configuration.getCombinedConfiguration();
			String colorStr = null;

			// severity: WARNING
			if (conf != null) colorStr = conf.getProperty(getName()+".color.severity.warning");
			addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				@Override
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					int col = adapter.getColumnIndex("severity");
					if (col == adapter.column)
						return Severity.WARNING.equals(adapter.getValue(col));
					return false;
				}
			}, SwingUtils.parseColor(colorStr, Color.ORANGE), null));

			// severity: ERROR
			if (conf != null) colorStr = conf.getProperty(getName()+".color.severity.error");
			addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				@Override
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					int col = adapter.getColumnIndex("severity");
					if (col == adapter.column)
						return Severity.ERROR.equals(adapter.getValue(col));
					return false;
				}
			}, SwingUtils.parseColor(colorStr, Color.RED), null));

			// state: UP
			if (conf != null) colorStr = conf.getProperty(getName()+".color.state.up");
			addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				@Override
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					int col = adapter.getColumnIndex("state");
					if (col == adapter.column)
						return AlarmEvent.ServiceState.UP.equals(adapter.getValue(col));
					return false;
				}
			}, SwingUtils.parseColor(colorStr, Color.GREEN), null));

			// state: AFFECTED
			if (conf != null) colorStr = conf.getProperty(getName()+".color.state.up");
			addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				@Override
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					int col = adapter.getColumnIndex("state");
					if (col == adapter.column)
						return AlarmEvent.ServiceState.AFFECTED.equals(adapter.getValue(col));
					return false;
				}
			}, SwingUtils.parseColor(colorStr, Color.ORANGE), null));

			// state: DOWN
			if (conf != null) colorStr = conf.getProperty(getName()+".color.state.down");
			addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				@Override
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					int col = adapter.getColumnIndex("state");
					if (col == adapter.column)
						return AlarmEvent.ServiceState.DOWN.equals(adapter.getValue(col));
					return false;
				}
			}, SwingUtils.parseColor(colorStr, Color.RED), null));
		}

		@Override
		public void tableChanged(TableModelEvent e)
		{
			super.tableChanged(e);
			packAllGrowOnly();
		}

		/** What table header was the last header we visited */
		@SuppressWarnings("unused")
		public int getLastTableHeaderColumn()
		{
			return _lastTableHeaderColumn;
		}

		/** TABLE HEADER tool tip. */
		@Override
		protected JTableHeader createDefaultTableHeader()
		{
			JTableHeader tabHeader = new JXTableHeader(getColumnModel())
			{
				private static final long serialVersionUID = 0L;

				@Override
				public String getToolTipText(MouseEvent e)
				{
					String tip = null;
					int col = getColumnModel().getColumnIndexAtX(e.getPoint().x);
					if (col < 0) return null;

					switch (col)
					{
					case AlarmActiveTableModel.TAB_POS_ALARM_CLASS               : tip = "<html> Name of the Alarm. Note: 'AlarmEvent' has been stripped of for readability </html>"; break;
					case AlarmActiveTableModel.TAB_POS_SERVICE_TYPE              : tip = "<html> This will always be the tool you are using '"+Version.getAppName()+"' </html>"; break;
					case AlarmActiveTableModel.TAB_POS_SERVICE_NAME              : tip = "<html> Name of the <b>server</b> this alarm was generated for. This could be the DBMS Instance name, or the hostname where the service is running. </html>"; break;
					case AlarmActiveTableModel.TAB_POS_SERVICE_INFO              : tip = "<html> Name of the CounterModel that generated the alarm </html>"; break;
					case AlarmActiveTableModel.TAB_POS_EXTRA_INFO                : tip = "<html> This could be a database, object name, or similar that the alarm was raised for. </html>"; break;
					case AlarmActiveTableModel.TAB_POS_SEVERITY                  : tip = "<html> ERROR, WARNING, INFO </html>"; break;
					case AlarmActiveTableModel.TAB_POS_STATE                     : tip = "<html> UP, AFFECTED, DOWN </html>"; break;
					case AlarmActiveTableModel.TAB_POS_REPEAT_COUNT              : tip = "<html> How many times this alarm has been re-raised.<br> All CounterModels will send an Alarm <b>every time</b> it sees an issue. Then the AlarmHandler will figgure out if it should send a RAISE or CANCEL or simply do nothing and increment the <i>repeat count</i> due to that the alarm was overlapping between two checks.</html>"; break;
					case AlarmActiveTableModel.TAB_POS_ALARM_DURATION            : tip = "<html> How <b>long</b> in milliseconds has/was the Alarm in <i>active</i> state,</html>"; break;
					case AlarmActiveTableModel.TAB_POS_FULL_DURATION             : tip = "<html> How <b>long</b> in milliseconds has/was the Alarm in <i>active</i> state, aftr the 'fullAdjInSec' </html>"; break;
					case AlarmActiveTableModel.TAB_POS_FULL_DURATION_ADJ_IN_SEC  : tip = "<html> How many seconds was the 'fullDuaration' adjusted with</html>"; break;
					case AlarmActiveTableModel.TAB_POS_CR_TIME                   : tip = "<html> Time when the Alarm was created. </html>"; break;
					case AlarmActiveTableModel.TAB_POS_TIME_TO_LIVE              : tip = "<html> How long should this Alarm be ACTIVE for.<br> If <i>postpone</i> has been set for a CounterModel then alarms wont be send on every <i>check loop</i> so we need to set an estimated time for when we can cancel the alarm.</html>"; break;
					case AlarmActiveTableModel.TAB_POS_DATA                      : tip = "<html> Raw data value for the data the alarm was originally raised for </html>"; break;
					case AlarmActiveTableModel.TAB_POS_LAST_DATA                 : tip = "<html> Last Raw data value if the alarm was <i>re-raised</i>.<br>Meaning the value for the last data point where the alarm was raised a second time while it was still in the active list. </html>"; break;
					case AlarmActiveTableModel.TAB_POS_DESCRIPTION               : tip = "<html> Text description of the alarm. A short <i>sloogan</i> what the alarm is about.</html>"; break;
					case AlarmActiveTableModel.TAB_POS_LAST_DESCRIPTION          : tip = "<html> Last <i>description</i> if the alarm was <i>re-raised</i>.<br>Meaning the value for the description where the alarm was raised a second time while it was still in the active list. </html>"; break;
					case AlarmActiveTableModel.TAB_POS_EXTENDED_DESCRIPTION      : tip = "<html> A longer/extensive description of the alarm and it's details.<br>Note 1: This is meant for Alarm Writes like 'AlarmWriterTo<b>Mail</b>'.<br>Note 2: Not all Alarms will set this field.</html>"; break;
					case AlarmActiveTableModel.TAB_POS_LAST_EXTENDED_DESCRIPTION : tip = "<html> Last <i>extended description</i> if the alarm was <i>re-raised</i>.<br>Meaning the value for the extended description where the alarm was raised a second time while it was still in the active list. </html>"; break;
					}

					if (tip == null)
						return null;
//					return "<html>" + tip + "</html>";
					return tip;
				}
			};

//			// Track where we are in the TableHeader, this is used by the Popup menus
//			// to decide what column of the TableHeader we are currently located on.
//			tabHeader.addMouseMotionListener(new MouseMotionListener()
//			{
//				@Override
//				public void mouseMoved(MouseEvent e)
//				{
//					_lastTableHeaderColumn = getColumnModel().getColumnIndexAtX(e.getX());
//				}
//				@Override
//				public void mouseDragged(MouseEvent e) {/*ignore*/}
//			});

			if ( ! _isOffline )
				return tabHeader;
			else
				return super.createDefaultTableHeader();
		}

//		/** CELL tool tip */
//		@Override
//		public String getToolTipText(MouseEvent e)
//		{
//			String tip = null;
//
//			Point p = e.getPoint();
//			int row = rowAtPoint(p);
//			int col = columnAtPoint(p);
//			if ( row >= 0 && col >= 0 )
//			{
//				int mcol = super.convertColumnIndexToModel(col);
//				int mrow = super.convertRowIndexToModel(row);
//
//				if (mcol < 0 || mrow < 0)
//					return null;
//
//				// View Content of the following columns
//				if (    mcol == AlarmHistoryTableModel.TAB_POS_DESCRIPTION
//				     || mcol == AlarmHistoryTableModel.TAB_POS_LAST_DESCRIPTION
//				     || mcol == AlarmHistoryTableModel.TAB_POS_EXTENDED_DESCRIPTION
//				     || mcol == AlarmHistoryTableModel.TAB_POS_LAST_EXTENDED_DESCRIPTION
//					)
//					tip = (String) super.getModel().getValueAt(mrow, mcol);
//
//				if (tip == null)
//					return null;
//			}
//
////			return "<html>" + tip + "</html>";
//			return tip;
//		}

		/** Populate information in the table */
		protected void refreshTable()
		{
		}

		/*---------------------------------------------------
		** BEGIN: PopupMenu on the table
		**---------------------------------------------------
		*/
		/** Get the JMeny attached to the GTabbedPane */
		public JPopupMenu getDataTablePopupMenu()
		{
			return _popupMenu;
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

		/** Get the JMeny attached to the JTable header */
		public JPopupMenu getDataTableHeaderPopupMenu()
		{
			return _headerPopupMenu;
		}

		public JPopupMenu createDataTableHeaderPopupMenu()
		{
			return null;
		}

		/*---------------------------------------------------
		** END: PopupMenu on the table
		**---------------------------------------------------
		*/
	}

	/*---------------------------------------------------
	** END: class LocalActiveTable
	**---------------------------------------------------
	*/
	
	
	/*---------------------------------------------------
	** BEGIN: class LocalHistoryTable
	**---------------------------------------------------
	*/
	/** Extend the GTable */
	private class LocalHistoryTable extends GTable
	{
		private static final long serialVersionUID = 0L;
//		protected int           _lastTableHeaderPointX = -1;
		protected int           _lastTableHeaderColumn = -1;
		private   JPopupMenu    _popupMenu             = null;
		private   JPopupMenu    _headerPopupMenu       = null;
		private   boolean       _isOffline             = false;

		LocalHistoryTable()
		{
			super("alarmView.historyTable");
			setModel( AlarmWriterToTableModel.getInstance().getHistoryTableModel() );

			if (MainFrame.isOfflineConnected())
			{
				DbxConnection conn = MainFrame.getOfflineConnection();

				String sql = conn.quotifySqlString("select * from [MonAlarmHistory]");

				try ( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
				{
					ResultSetTableModel rstm = new ResultSetTableModel(rs, "alarmView.historyTable");
					setModel( rstm );
					_isOffline = true;
				}
				catch(SQLException ex)
				{
					_logger.error("Problems loading Offline Alarm History", ex);
				}
			}

//			setShowGrid(false);
			setSortable(true);
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			packAll(); // set size so that all content in all cells are visible
			setColumnControlVisible(true);
//			setHighlighters(_highliters);

			addHighlighters();

			// Create some PopupMenus and attach them
			_popupMenu = createDataTablePopupMenu();
			if (_popupMenu != null)
				setComponentPopupMenu(getDataTablePopupMenu());

			_headerPopupMenu = createDataTableHeaderPopupMenu();
			if (_headerPopupMenu != null)
				getTableHeader().setComponentPopupMenu(getDataTableHeaderPopupMenu());

			// Populate the table
			refreshTable();
		}

		private void addHighlighters()
		{
			Configuration conf = Configuration.getCombinedConfiguration();
			String colorStr = null;

			// LIGHT YELLOW for ACTIVE Alarms
			if (conf != null) colorStr = conf.getProperty(getName()+".color.isActive");
			addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				@Override
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					if (_isOffline)
						return false;
					return (Boolean) adapter.getValue(adapter.getColumnIndex("isActive"));
				}
			}, SwingUtils.parseColor(colorStr, TrendGraphColors.VERY_LIGHT_YELLOW), null));

			// severity: WARNING
			if (conf != null) colorStr = conf.getProperty(getName()+".color.severity.warning");
			addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				@Override
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					int col = adapter.getColumnIndex("severity");
					if (col == adapter.column)
						return Severity.WARNING.equals(adapter.getValue(col));
					return false;
				}
			}, SwingUtils.parseColor(colorStr, Color.ORANGE), null));

			// severity: ERROR
			if (conf != null) colorStr = conf.getProperty(getName()+".color.severity.error");
			addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				@Override
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					int col = adapter.getColumnIndex("severity");
					if (col == adapter.column)
						return Severity.ERROR.equals(adapter.getValue(col));
					return false;
				}
			}, SwingUtils.parseColor(colorStr, Color.RED), null));

			// state: UP
			if (conf != null) colorStr = conf.getProperty(getName()+".color.state.up");
			addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				@Override
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					int col = adapter.getColumnIndex("state");
					if (col == adapter.column)
						return AlarmEvent.ServiceState.UP.equals(adapter.getValue(col));
					return false;
				}
			}, SwingUtils.parseColor(colorStr, Color.GREEN), null));

			// state: AFFECTED
			if (conf != null) colorStr = conf.getProperty(getName()+".color.state.up");
			addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				@Override
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					int col = adapter.getColumnIndex("state");
					if (col == adapter.column)
						return AlarmEvent.ServiceState.AFFECTED.equals(adapter.getValue(col));
					return false;
				}
			}, SwingUtils.parseColor(colorStr, Color.ORANGE), null));

			// state: DOWN
			if (conf != null) colorStr = conf.getProperty(getName()+".color.state.down");
			addHighlighter( new ColorHighlighter(new HighlightPredicate()
			{
				@Override
				public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
				{
					int col = adapter.getColumnIndex("state");
					if (col == adapter.column)
						return AlarmEvent.ServiceState.DOWN.equals(adapter.getValue(col));
					return false;
				}
			}, SwingUtils.parseColor(colorStr, Color.RED), null));
		}

		/** What table header was the last header we visited */
		@SuppressWarnings("unused")
		public int getLastTableHeaderColumn()
		{
			return _lastTableHeaderColumn;
		}

		@Override
		public void tableChanged(final TableModelEvent e)
		{
			super.tableChanged(e);

			// Ignore IndexOutOfBoundsException... probably happens when we remove rows (due to keep only last ### rows) 
			try { packAllGrowOnly(); }
			catch (IndexOutOfBoundsException ignore) {}

			if (_historyMoveToLastAddedEntry_chk.isSelected())
			{
				// MAKE SURE THat the fireTableInsert has been called
				if (TableUtilities.isInsert(e))
				{
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							int viewRow = convertRowIndexToView(e.getFirstRow());
							scrollRectToVisible(getCellRect(viewRow, 0, true));
						}
					});
				}
			}
		}

		/** TABLE HEADER tool tip. */
		@Override
		protected JTableHeader createDefaultTableHeader()
		{
			JTableHeader tabHeader = new JXTableHeader(getColumnModel())
			{
				private static final long serialVersionUID = 0L;

				@Override
				public String getToolTipText(MouseEvent e)
				{
					String tip = null;
					int col = getColumnModel().getColumnIndexAtX(e.getPoint().x);
					if (col < 0) return null;

					switch (col)
					{
					case AlarmHistoryTableModel.TAB_POS_EVENT_TIME                : tip = "<html> When was this entry <b>added</b> to this list </html>"; break;
					case AlarmHistoryTableModel.TAB_POS_ACTION                    : tip = "<html> RAISE or CANCEL </html>"; break;
					case AlarmHistoryTableModel.TAB_POS_IS_ACTIVE                 : tip = "<html> If the Alrm is still active or not </html>"; break;
					case AlarmHistoryTableModel.TAB_POS_ALARM_CLASS               : tip = "<html> Name of the Alarm. Note: 'AlarmEvent' has been stripped of for readability </html>"; break;
					case AlarmHistoryTableModel.TAB_POS_SERVICE_TYPE              : tip = "<html> This will always be the tool you are using '"+Version.getAppName()+"' </html>"; break;
					case AlarmHistoryTableModel.TAB_POS_SERVICE_NAME              : tip = "<html> Name of the <b>server</b> this alarm was generated for. This could be the DBMS Instance name, or the hostname where the service is running. </html>"; break;
					case AlarmHistoryTableModel.TAB_POS_SERVICE_INFO              : tip = "<html> Name of the CounterModel that generated the alarm </html>"; break;
					case AlarmHistoryTableModel.TAB_POS_EXTRA_INFO                : tip = "<html> This could be a database, object name, or similar that the alarm was raised for. </html>"; break;
					case AlarmHistoryTableModel.TAB_POS_SEVERITY                  : tip = "<html> ERROR, WARNING, INFO </html>"; break;
					case AlarmHistoryTableModel.TAB_POS_STATE                     : tip = "<html> UP, AFFECTED, DOWN </html>"; break;
					case AlarmHistoryTableModel.TAB_POS_REPEAT_COUNT              : tip = "<html> How many times this alarm has been re-raised.<br> All CounterModels will send an Alarm <b>every time</b> it sees an issue. Then the AlarmHandler will figgure out if it should send a RAISE or CANCEL or simply do nothing and increment the <i>repeat count</i> due to that the alarm was overlapping between two checks.</html>"; break;
					case AlarmHistoryTableModel.TAB_POS_ALARM_DURATION            : tip = "<html> How <b>long</b> in milliseconds has/was the Alarm in <i>active</i> state,</html>"; break;
					case AlarmHistoryTableModel.TAB_POS_FULL_DURATION             : tip = "<html> How <b>long</b> in milliseconds has/was the Alarm in <i>active</i> state, aftr the 'fullAdjInSec' </html>"; break;
					case AlarmHistoryTableModel.TAB_POS_FULL_DURATION_ADJ_IN_SEC  : tip = "<html> How many seconds was the 'fullDuaration' adjusted with</html>"; break;
					case AlarmHistoryTableModel.TAB_POS_CR_TIME                   : tip = "<html> Time when the Alarm was created. </html>"; break;
					case AlarmHistoryTableModel.TAB_POS_CANCEL_TIME               : tip = "<html> Time when the Alarm was canceled. </html>"; break;
					case AlarmHistoryTableModel.TAB_POS_TIME_TO_LIVE              : tip = "<html> How long should this Alarm be ACTIVE for.<br> If <i>postpone</i> has been set for a CounterModel then alarms wont be send on every <i>check loop</i> so we need to set an estimated time for when we can cancel the alarm.</html>"; break;
					case AlarmHistoryTableModel.TAB_POS_DATA                      : tip = "<html> Raw data value for the data the alarm was originally raised for </html>"; break;
					case AlarmHistoryTableModel.TAB_POS_LAST_DATA                 : tip = "<html> Last Raw data value if the alarm was <i>re-raised</i>.<br>Meaning the value for the last data point where the alarm was raised a second time while it was still in the active list. </html>"; break;
					case AlarmHistoryTableModel.TAB_POS_DESCRIPTION               : tip = "<html> Text description of the alarm. A short <i>sloogan</i> what the alarm is about.</html>"; break;
					case AlarmHistoryTableModel.TAB_POS_LAST_DESCRIPTION          : tip = "<html> Last <i>description</i> if the alarm was <i>re-raised</i>.<br>Meaning the value for the description where the alarm was raised a second time while it was still in the active list. </html>"; break;
					case AlarmHistoryTableModel.TAB_POS_EXTENDED_DESCRIPTION      : tip = "<html> A longer/extensive description of the alarm and it's details.<br>Note 1: This is meant for Alarm Writes like 'AlarmWriterTo<b>Mail</b>'.<br>Note 2: Not all Alarms will set this field.</html>"; break;
					case AlarmHistoryTableModel.TAB_POS_LAST_EXTENDED_DESCRIPTION : tip = "<html> Last <i>extended description</i> if the alarm was <i>re-raised</i>.<br>Meaning the value for the extended description where the alarm was raised a second time while it was still in the active list. </html>"; break;
					}

					if (tip == null)
						return null;
//					return "<html>" + tip + "</html>";
					return tip;
				}
			};

//			// Track where we are in the TableHeader, this is used by the Popup menus
//			// to decide what column of the TableHeader we are currently located on.
//			tabHeader.addMouseMotionListener(new MouseMotionListener()
//			{
//				@Override
//				public void mouseMoved(MouseEvent e)
//				{
//					_lastTableHeaderColumn = getColumnModel().getColumnIndexAtX(e.getX());
//				}
//				@Override
//				public void mouseDragged(MouseEvent e) {/*ignore*/}
//			});

			if ( ! _isOffline )
				return tabHeader;
			else
				return super.createDefaultTableHeader();
		}

//		/** CELL tool tip */
//		@Override
//		public String getToolTipText(MouseEvent e)
//		{
//			String tip = null;
//
//			Point p = e.getPoint();
//			int row = rowAtPoint(p);
//			int col = columnAtPoint(p);
//			if ( row >= 0 && col >= 0 )
//			{
//				int mcol = super.convertColumnIndexToModel(col);
//				int mrow = super.convertRowIndexToModel(row);
//
//				if (mcol < 0 || mrow < 0)
//					return null;
//
//				// View Content of the following columns
//				if (    mcol == AlarmHistoryTableModel.TAB_POS_DESCRIPTION
//				     || mcol == AlarmHistoryTableModel.TAB_POS_LAST_DESCRIPTION
//				     || mcol == AlarmHistoryTableModel.TAB_POS_EXTENDED_DESCRIPTION
//				     || mcol == AlarmHistoryTableModel.TAB_POS_LAST_EXTENDED_DESCRIPTION
//					)
//				{
//					tip = "" + super.getModel().getValueAt(mrow, mcol);
//				}
//
//				if (tip == null)
//					return null;
//			}
//
////			return "<html>" + tip + "</html>";
//			return tip;
//		}

		/** Populate information in the table */
		protected void refreshTable()
		{
		}

		/*---------------------------------------------------
		** BEGIN: PopupMenu on the table
		**---------------------------------------------------
		*/
		/** Get the JMeny attached to the GTabbedPane */
		public JPopupMenu getDataTablePopupMenu()
		{
			return _popupMenu;
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

		/** Get the JMeny attached to the JTable header */
		public JPopupMenu getDataTableHeaderPopupMenu()
		{
			return _headerPopupMenu;
		}

		public JPopupMenu createDataTableHeaderPopupMenu()
		{
			return null;
		}

		/*---------------------------------------------------
		** END: PopupMenu on the table
		**---------------------------------------------------
		*/
	}

	/*---------------------------------------------------
	** END: class LocalTable
	**---------------------------------------------------
	*/

	
	//////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////
	//// TEST  ///////////////
	//////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////
    public static void main(String[] args)
    {
    	
		//--------------------------------------------------------
		// Alarm Handler
		//--------------------------------------------------------
//System.setProperty("AlarmHandler.WriterClass",                 "com.dbxtune.alarm.writers.AlarmWriterToStdout, com.dbxtune.alarm.writers.AlarmWriterToFile");
//System.setProperty("AlarmWriterToFile.alarms.active.filename", "c:\\tmp\\AlarmViewTest_alarm_active.log");
//System.setProperty("AlarmWriterToFile.alarms.log.filename",    "c:\\tmp\\AlarmViewTest_alarm.log");

		// Set Log4j Log Level
		Configurator.setRootLevel(Level.TRACE);

		String saveFile = "c:\\tmp\\AlarmViewTest.tmp.deleteme.properties";
		try
		{
			File f = new File(saveFile);
			f.createNewFile();

			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		Configuration conf = new Configuration(saveFile);
		Configuration.setInstance(Configuration.USER_TEMP, conf);

    	try
    	{
			AlarmHandler ah = new AlarmHandler(AlarmHandler.DEFAULT_INSTANCE);
    		ah.init(Configuration.getCombinedConfiguration(), true, false, true);
    		ah.start();
    		AlarmHandler.setInstance(AlarmHandler.DEFAULT_INSTANCE, ah);

    		JFrame frame = new JFrame();
    		AlarmViewer av = new AlarmViewer(frame);
    		av.setVisible(true);
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
    }	
}
