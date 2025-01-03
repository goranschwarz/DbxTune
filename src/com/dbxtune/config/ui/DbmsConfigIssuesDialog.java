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
package com.dbxtune.config.ui;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdesktop.swingx.JXTableHeader;

import com.dbxtune.Version;
import com.dbxtune.config.dbms.DbmsConfigIssue;
import com.dbxtune.config.dbms.DbmsConfigManager;
import com.dbxtune.config.dbms.IDbmsConfig;
import com.dbxtune.gui.swing.GTable;
import com.dbxtune.gui.swing.GTable.ITableTooltip;
import com.dbxtune.pcs.PersistReader;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.ConnectionProvider;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class DbmsConfigIssuesDialog
//extends JDialog
extends JFrame
implements ActionListener //, ConnectionProvider
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	// PANEL: OK-CANCEL
	private JButton                _ok              = new JButton("OK");
	private JButton                _cancel          = new JButton("Cancel");

	@SuppressWarnings("unused")
	private Window                 _owner           = null;

//	private JButton                _refresh         = new JButton("Refresh");
//	private JLabel                 _freeMb          = new JLabel();
	
	private IDbmsConfig            _dbmsConfig      = null;
	private List<DbmsConfigIssue>  _dbmsConfigIssuesList = null;
	private ConnectionProvider     _connProvider    = null;
	
//	private JTabbedPane            _tabPane                          = new JTabbedPane();
	private LocalTable             _table;
	private LocalTableModel        _tm;
	
	private DbmsConfigIssuesDialog(Frame owner, IDbmsConfig dbmsConfig, ConnectionProvider connProvider)
	{
//		super(owner, "DBMS Configuration Issues", true);
		super("DBMS Configuration Issues");
//		setModalityType(ModalityType.MODELESS);
		init(owner, dbmsConfig, connProvider);
	}
	private DbmsConfigIssuesDialog(Dialog owner, IDbmsConfig dbmsConfig, ConnectionProvider connProvider)
	{
//		super(owner, "DBMS Configuration Issues", true);
		super("DBMS Configuration Issues");
//		setModalityType(ModalityType.MODELESS);
		init(owner, dbmsConfig, connProvider);
	}

	public static void showDialog(Frame owner, IDbmsConfig dbmsConfig, ConnectionProvider connProvider)
	{
		DbmsConfigIssuesDialog dialog = new DbmsConfigIssuesDialog(owner, dbmsConfig, connProvider);
		dialog.setVisible(true);
//		dialog.dispose();
	}
	public static void showDialog(Dialog owner, IDbmsConfig dbmsConfig, ConnectionProvider connProvider)
	{
		DbmsConfigIssuesDialog dialog = new DbmsConfigIssuesDialog(owner, dbmsConfig, connProvider);
		dialog.setVisible(true);
//		dialog.dispose();
	}
	public static void showDialog(Component owner, IDbmsConfig dbmsConfig, ConnectionProvider connProvider)
	{
		DbmsConfigIssuesDialog dialog = null;
		if (owner instanceof Frame)
			dialog = new DbmsConfigIssuesDialog((Frame)owner, dbmsConfig, connProvider);
		else if (owner instanceof Dialog)
			dialog = new DbmsConfigIssuesDialog((Dialog)owner, dbmsConfig, connProvider);
		else
			dialog = new DbmsConfigIssuesDialog((Dialog)null, dbmsConfig, connProvider);

		dialog.setVisible(true);
//		dialog.dispose();
	}

	@Override
	public void setVisible(boolean visible)
	{
		super.setVisible(visible);

		// Refresh only enabled if connected to ASE, not offline for the moment
		if (visible)
		{
			boolean b = true;
			if (PersistReader.hasInstance())
				if (PersistReader.getInstance().isConnected())
					b = false;

//			_refresh.setEnabled(b);
		}
	}
	
	private void init(Window owner, IDbmsConfig dbmsConfig, ConnectionProvider connProvider)
	{
		_owner = owner;

		_dbmsConfig   = dbmsConfig;
		_dbmsConfigIssuesList = dbmsConfig.getConfigIssues();
		_connProvider = connProvider;
		initComponents();

//		pack();

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
		ImageIcon icon16 = SwingUtils.readImageIcon(Version.class, "images/config_dbms_view_16.png");
		ImageIcon icon32 = SwingUtils.readImageIcon(Version.class, "images/config_dbms_view_32.png");
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

//		super(_owner);
//		if (_owner != null)
//			setIconImage(_owner.getIconImage());

//		setTitle("DBMS Configuration Issues");

		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("insets 0, wrap 1","",""));   // insets Top Left Bottom Right

		//JTabbedPane tabPane = new JTabbedPane();
//		_tabPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

		if (DbmsConfigManager.hasInstance())
		{
//			DbmsConfigPanel dcp = new DbmsConfigPanel(this, DbmsConfigManager.getInstance());
//			_tabPane.add(DbmsConfigManager.getInstance().getTabLabel(), dcp);
		}
		_tm = new LocalTableModel();
		_table = new LocalTable(_tm);
		JScrollPane scroll = new JScrollPane(_table);
		_table.packAll(); // set size so that all content in all cells are visible

		// Initialize a watermark
		_table.setWatermarkAnchor(scroll);

		
		JPanel header_pan = new JPanel(new MigLayout());
		JLabel header_lbl = new JLabel("<html>"
				+ "Below are issues that has been detected when checking various DBMS configurations when connecting.<br>"
				+ "Hover over the records to see <b>Description</b> and a <b>Proposed Resolution</b>.<br>"
				+ "<br>"
				+ "If you do <b>not</b> want this dialog to show after connect:"
				+ "<ul>"
				+ "  <li>Apply the Proposed Resolution</li>"
				+ "  <li>Or press the <b>Discard</b> checkbox at the row/issue, <i>which discards this issue until the DBMS is restarted</i></li>"
				+ "</ul>"
				+ "Any issues can be reviewed later: Menu -&gt; View -&gt; View DBMS Configuration... Then click on button 'Show Issues', at the lower left corner"
				+ "</html>");
		header_pan.add(header_lbl, "grow, push");
		
		panel.add(header_pan,            "growx, pushx, wrap");
		panel.add(scroll,                "grow, height 100%, width 100%");
//		panel.add(_tabPane,              "grow, height 100%, width 100%");
		panel.add(createOkCancelPanel(), "grow, push, bottom");

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

	private JPanel createOkCancelPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("","",""));   // insets Top Left Bottom Right

//		panel.add(_refresh, "left");
//		panel.add(_freeMb,  "left");

		// ADD the OK, Cancel, Apply buttons
		panel.add(_ok,     "push, tag ok");
		panel.add(_cancel, "tag cancel");
//		panel.add(_apply,  "tag apply");

//		_apply.setEnabled(false);

		// Initialize some fields.
//		_freeMb.setToolTipText("How much memory is available for reconfiguration, same value as you can get from sp_configure 'memory'.");
//		_freeMb.setText(DbmsConfigTextManager.hasInstances() ? DbmsConfigManager.getInstance().getFreeMemoryStr() : "");
//
//		_refresh.setToolTipText("Re-read the configuration.");

		// ADD ACTIONS TO COMPONENTS
		_ok           .addActionListener(this);
		_cancel       .addActionListener(this);
//		_apply        .addActionListener(this);
//		_refresh      .addActionListener(this);

		return panel;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();

		// --- BUTTON: REFRESH ---
//		if (_refresh.equals(source))
//		{
//			doRefresh();
//		}

		// --- BUTTON: CANCEL ---
		if (_cancel.equals(source))
		{
			setVisible(false);
		}

		// --- BUTTON: OK ---
		if (_ok.equals(source))
		{
			doApply();
			saveProps();
			setVisible(false);
		}

	}

//	private void doRefresh()
//	{
//		WaitForExecDialog wait = new WaitForExecDialog(this, "Getting DBMS Configuration");
//
//		// Kick this of as it's own thread, otherwise the sleep below, might block the Swing Event Dispatcher Thread
//		BgExecutor bgExec = new BgExecutor(wait)
//		{
//			@Override
//			public Object doWork()
//			{
//				try
//				{
//					for (int t=0; t<_tabPane.getTabCount(); t++)
//					{
//						Component comp = _tabPane.getComponentAt(t);
//						String    name = _tabPane.getTitleAt(t);
//			
//						getWaitDialog().setState("Refreshing tab '"+name+"'.");
//						if (comp instanceof DbmsConfigPanel)
//						{
//							((DbmsConfigPanel)comp).refresh();
//						}
//						else if (comp instanceof DbmsConfigTextPanel)
//						{
//							((DbmsConfigTextPanel)comp).refresh();
//						}
//					}
//				}
//				catch(Exception ex) 
//				{
//					_logger.info("Initialization of the DBMS Configuration did not succeed. Caught: "+ex); 
//				}
//				getWaitDialog().setState("Done");
//
//				return null;
//			}
//		};
//		wait.execAndWait(bgExec);
//
//		_freeMb.setText(DbmsConfigTextManager.hasInstances() ? DbmsConfigManager.getInstance().getFreeMemoryStr() : "");
//	}

	private void doApply()
	{
		Configuration tmpCfg = Configuration.getInstance(Configuration.USER_TEMP);
		if (tmpCfg == null)
			return;

		for (DbmsConfigIssue issue : _dbmsConfigIssuesList)
		{
			String  key = issue.getDiscardPropKey();
			boolean val = issue.isDiscarded();
			
			tmpCfg.setProperty(key, val);
		}
		tmpCfg.save();
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

	private class LocalTable extends GTable
	{
		private static final long serialVersionUID = 0L;
		private LocalTableModel _tm;
		
		public LocalTable(LocalTableModel tm)
		{
			super();
			
			setModel(tm);
			_tm = tm;

//			setShowGrid(false);
			setSortable(true);
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			packAll(); // set size so that all content in all cells are visible
			setColumnControlVisible(true);
			setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);
//			setHighlighters(_highliters);

			// Create some PopupMenus and attach them
//			_popupMenu = createDataTablePopupMenu();
//			setComponentPopupMenu(getDataTablePopupMenu());

//			_headerPopupMenu = createDataTableHeaderPopupMenu();
//			getTableHeader().setComponentPopupMenu(getDataTableHeaderPopupMenu());

			// Populate the table
//			refreshTable();
			
			// Select first row
			if (getRowCount() > 0)
				getSelectionModel().setSelectionInterval(0, 0);
			
			getSelectionModel().addListSelectionListener(new ListSelectionListener()
			{
				@Override
				public void valueChanged(ListSelectionEvent e)
				{
//					AlarmTable table = AlarmTable.this;
//					if (e.getValueIsAdjusting())
//						return;
//					
//					// In the detailes section: Load the selected row 
//					int vrow = table.getSelectedRow();
//					if (vrow == -1)
//						return;
//					int mrow = table.convertRowIndexToModel(vrow);
//
//					AlarmEntry alarmEntry = _alarmTableModel.getAlarmForRow(mrow);
//
////					CountersModel cm = CounterController.getInstance().getCmByName(cmName);
////					_alarmDetailsPanel.setCm(cm);
//					_alarmDetailsPanel.setAlarmEntry(alarmEntry);
				}
			});
			
			_tm.addTableModelListener(new TableModelListener()
			{
				@Override
				public void tableChanged(TableModelEvent e)
				{
					String watermark = null;
					
					if (_tm.getRowCount() == 0)
						watermark = "No issues was found";

					if (_tm.getRowCount() > 0)
					{
						if ( ! _dbmsConfig.hasConfigIssues() )
							watermark = "All issues is mapped to be discarded...";
					}

					setWatermarkText(watermark);
				}
			});
			
			// invoke a table change later... 
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					_tm.fireTableDataChanged();
				}
			});
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

					int vcol = getColumnModel().getColumnIndexAtX(e.getPoint().x);
					if (vcol == -1) return null;

					int mcol = convertColumnIndexToModel(vcol);
					if (mcol == -1) return null;

					tip = _tm.getColumnToolTipText(mcol);

					if (tip == null)
						return null;
					return "<html>" + tip + "</html>";
				}
			};

			// Track where we are in the TableHeader, this is used by the Popup menus
			// to decide what column of the TableHeader we are currently located on.
//			tabHeader.addMouseMotionListener(new MouseMotionListener()
//			{
//				@Override
//				public void mouseMoved(MouseEvent e)
//				{
//					_lastTableHeaderColumn = getColumnModel().getColumnIndexAtX(e.getX());
//					if (_lastTableHeaderColumn >= 0)
//						_lastTableHeaderColumn = convertColumnIndexToModel(_lastTableHeaderColumn);
//				}
//				@Override
//				public void mouseDragged(MouseEvent e) {/*ignore*/}
//			});

			return tabHeader;
		}

//		/** CELL tool tip */
//		@Override
//		public String getToolTipText(MouseEvent e)
//		{
//			String tip = null;
//			Point p = e.getPoint();
//			int vrow = rowAtPoint(p);
//			int vcol = columnAtPoint(p);
//			if (vrow == -1 || vcol == -1)
//				return null;
//			
//			// Translate View row to Model row
//			int mrow = super.convertRowIndexToModel(    vrow );
//			int mcol = super.convertColumnIndexToModel( vcol );
//
//			// is Boolean and isEditable then show the below tooltip
//			TableModel tm = getModel();
//			if (tm.getColumnClass(mcol).equals(Boolean.class))
//			{
//				if (tm.isCellEditable(mrow, mcol))
//					tip = "Right click on the header column to mark or unmark all rows.";
//				else
//					tip = "Sorry you can't change this value. (it's a <i>static</i> field)";
//			}
//			else
//			{
//				tip = "";
//				tip += "<b>Configuration</b>: " + getValueAt(mrow, TAB_POS_CONFIG_NAME) + "<br>";
//				tip += "<b>Discarded</b>: " + getValueAt(mrow, TAB_POS_DISCARDED) + "<br>";
//				tip += "<hr>";
//				tip += "<h3>Description</h3>";
//				tip += getValueAt(mrow, TAB_POS_DESCRIPTION);
//				tip += "<h3>Proposed Resolution</h3>";
//				tip += getValueAt(mrow, TAB_POS_RESOLUTION);
//				tip += "<hr>";
//				tip += "<b>key</b>: " + getValueAt(mrow, TAB_POS_PROPERTY_KEY) + "<br>";
//			}
//
////			if (tip == null)
////				return null;
//			return "<html>" + tip + "</html>";
//		}

	}
		
	protected static final String[] TAB_HEADER = {"Discarded", "Config Name", "Severity", "Description", "Resolution", "Key"};
	protected static final int TAB_POS_DISCARDED        = 0;
	protected static final int TAB_POS_CONFIG_NAME      = 1;
	protected static final int TAB_POS_SEVERITY         = 2;
	protected static final int TAB_POS_DESCRIPTION      = 3;
	protected static final int TAB_POS_RESOLUTION       = 4;
	protected static final int TAB_POS_PROPERTY_KEY     = 5;

	private class LocalTableModel
	extends AbstractTableModel
	implements ITableTooltip
	{
		private static final long serialVersionUID = 1L;

		public String getColumnToolTipText(int col)
		{
			switch(col)
			{
			case TAB_POS_DISCARDED:    return "If this issue is discarded at connect time";
			case TAB_POS_CONFIG_NAME:  return "Configuration Name";
			case TAB_POS_SEVERITY:     return "Severity of the Issue";
			case TAB_POS_DESCRIPTION:  return "What does this issue mean";
			case TAB_POS_RESOLUTION:   return "A proposed resolution to this issue";
			case TAB_POS_PROPERTY_KEY: return "Property key used to store information about this issue.";
			}
			return null;
		}
		@Override
		public int getRowCount()
		{
			return _dbmsConfigIssuesList.size();
		}

		@Override
		public int getColumnCount()
		{
			return TAB_HEADER.length;
		}

		@Override
		public Class<?> getColumnClass(int col)
		{
			if (col == TAB_POS_DISCARDED) return Boolean.class;

			return Object.class;
		}

		@Override
		public boolean isCellEditable(int row, int col)
		{
//			if (col == TAB_POS_DISCARDED)  return true;
//			if (col == TAB_POS_RESOLUTION) return true;  // Just so we can COPY the resolution (if it for example, contains DDL that we want to copy)
//
//			return false;
			return true;
		}
		
		@Override
		public String getColumnName(int col)
		{
			return TAB_HEADER[col];
		}

		@Override
		public Object getValueAt(int row, int col)
		{
			DbmsConfigIssue issue = _dbmsConfigIssuesList.get(row);
			
			switch (col)
			{
			case TAB_POS_DISCARDED:    return issue.isDiscarded();
			case TAB_POS_CONFIG_NAME:  return issue.getConfigName();
			case TAB_POS_SEVERITY:     return issue.getSeverity();
			case TAB_POS_DESCRIPTION:  return issue.getDescription();
			case TAB_POS_RESOLUTION:   return issue.getResolution();
			case TAB_POS_PROPERTY_KEY: return issue.getPropKey();
			}
			return null;
		}

		@Override
		public void setValueAt(Object val, int row, int col)
		{
			if (col == TAB_POS_DISCARDED)
			{
				DbmsConfigIssue issue = _dbmsConfigIssuesList.get(row);
				boolean discard = (Boolean)val;

				issue.setDiscarded(discard);
				
				fireTableDataChanged();
			}
		}
		
		@Override
		public String getToolTipTextOnTableColumnHeader(String colName)
		{
			return getColumnToolTipText( findColumn(colName) );
		}
		@Override
		public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int mrow, int mcol)
		{
			DbmsConfigIssue issue = _dbmsConfigIssuesList.get(mrow);

			String tip = "";
			tip += "<b>Configuration</b>: " + issue.getConfigName() + "<br>";
			tip += "<b>Discarded</b>: "     + issue.isDiscarded()   + "<br>";
			tip += "<b>Severity</b>: "      + issue.getSeverity()   + "<br>";
			tip += "<hr>";
			tip += "<h3>Description</h3>";
			tip += issue.getDescription().replace("\n", "<br>");
			tip += "<h3>Proposed Solution</h3>";
			tip += issue.getResolution().replace("\n", "<br>");
			tip += "<br>";
			tip += "<br>";
			tip += "<br>";
			tip += "<hr>";
			tip += "<b>key</b>: <code>" + issue.getDiscardPropKey() + "</code><br>";
			tip += "<b>val</b>: <code>" + issue.isDiscarded()       + "</code><br>";
			
			return tip;
		}
  	}
}
