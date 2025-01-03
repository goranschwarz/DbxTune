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
package com.dbxtune.gui;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;

import com.dbxtune.Version;
import com.dbxtune.gui.swing.GTable;
import com.dbxtune.gui.swing.GTableFilter;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class TableModelViewDialog
//extends JDialog
extends JFrame
implements ActionListener
{
	private static final long serialVersionUID = 1L;
//	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	// PANEL: OK-CANCEL
//	private JButton                _ok              = new JButton("OK");
//	private JButton                _cancel          = new JButton("Cancel");
	private JButton                _close           = new JButton("Close");

	private JButton                _copy            = new JButton("Copy");
	
	@SuppressWarnings("unused")
	private Window                 _owner           = null;

//	private JButton                _refresh         = new JButton("Refresh");
//	private JLabel                 _freeMb          = new JLabel();
	
//	private JTabbedPane            _tabPane                          = new JTabbedPane();
	private LocalTable             _table;
//	private LocalTableModel        _tm;
	private TableModel             _tm;
	
	private TableModelViewDialog(Frame owner, TableModel tm)
	{
		super("Table Viewer");
		init(owner, tm);
	}
	private TableModelViewDialog(Dialog owner, TableModel tm)
	{
		super("Table Viewer");
		init(owner, tm);
	}

	public static void showDialog(Frame owner, TableModel tm)
	{
		TableModelViewDialog dialog = new TableModelViewDialog(owner, tm);
		dialog.setVisible(true);
//		dialog.dispose();
	}
	public static void showDialog(Dialog owner, TableModel tm)
	{
		TableModelViewDialog dialog = new TableModelViewDialog(owner, tm);
		dialog.setVisible(true);
//		dialog.dispose();
	}
	public static void showDialog(Component owner, TableModel tm)
	{
		TableModelViewDialog dialog = null;
		if (owner instanceof Frame)
			dialog = new TableModelViewDialog((Frame)owner, tm);
		else if (owner instanceof Dialog)
			dialog = new TableModelViewDialog((Dialog)owner, tm);
		else
			dialog = new TableModelViewDialog((Dialog)null, tm);

		dialog.setVisible(true);
//		dialog.dispose();
	}

	private void init(Window owner, TableModel tm)
	{
		_owner = owner;

		_tm = tm;

		initComponents();

//		pack();

//		Dimension size = getPreferredSize();
//		size.width += 200;
//
//		setPreferredSize(size);
////		setMinimumSize(size);
//		setSize(size);

		setLocationRelativeTo(owner);
		SwingUtils.installEscapeButton(this, _close);

//		setFocus();
	}

	protected void initComponents()
	{
		// Set the icon, if we "just" do setIconImage() on the JDialog
		// it will not be the "correct" icon in the Alt-Tab list on Windows
		// So we need to grab the owner, and set that since the icon is grabbed from the owner...
		ImageIcon icon16 = SwingUtils.readImageIcon(Version.class, "images/table_view_16.png");
		ImageIcon icon32 = SwingUtils.readImageIcon(Version.class, "images/table_view_32.png");
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

		JPanel panel = new JPanel();
//		panel.setLayout(new MigLayout("insets 0, wrap 1","",""));   // insets Top Left Bottom Right
		panel.setLayout(new MigLayout("wrap 1", "", ""));

		_table = new LocalTable(_tm);
		JScrollPane scroll = new JScrollPane(_table);
		_table.packAll(); // set size so that all content in all cells are visible

		// Initialize a watermark
		_table.setWatermarkAnchor(scroll);

		GTableFilter tableFilter = new GTableFilter(_table, GTableFilter.ROW_COUNT_LAYOUT_LEFT, true);
		
		_copy.setToolTipText("Copy Current table as 'ascii' table to the clipboard!");
		_copy.addActionListener(this);
		
		JPanel header_pan = new JPanel(new MigLayout());
//		JLabel header_lbl = new JLabel("<html>"
//				+ "Below are issues that has been detected when checking various DBMS configurations when connecting.<br>"
//				+ "Hover over the records to see <b>Description</b> and a <b>Proposed Resolution</b>.<br>"
//				+ "<br>"
//				+ "If you do <b>not</b> want this dialog to show after connect:"
//				+ "<ul>"
//				+ "  <li>Apply the Proposed Resolution</li>"
//				+ "  <li>Or press the <b>Discard</b> checkbox at the row/issue, <i>which discards this issue until the DBMS is restarted</i></li>"
//				+ "</ul>"
//				+ "Any issues can be reviewed later: Menu -&gt; View -&gt; View DBMS Configuration... Then click on button 'Show Issues', at the lower left corner"
//				+ "</html>");
//		header_pan.add(header_lbl, "grow, push");
		header_pan.add(tableFilter, "grow, push");
		header_pan.add(_copy);
		
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
		panel.add(_close,  "push, tag ok");
//		panel.add(_ok,     "push, tag ok");
//		panel.add(_cancel, "tag cancel");
//		panel.add(_apply,  "tag apply");

//		_apply.setEnabled(false);

		// Initialize some fields.
//		_freeMb.setToolTipText("How much memory is available for reconfiguration, same value as you can get from sp_configure 'memory'.");
//		_freeMb.setText(DbmsConfigTextManager.hasInstances() ? DbmsConfigManager.getInstance().getFreeMemoryStr() : "");
//
//		_refresh.setToolTipText("Re-read the configuration.");

		// ADD ACTIONS TO COMPONENTS
		_close        .addActionListener(this);
//		_ok           .addActionListener(this);
//		_cancel       .addActionListener(this);
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

		// --- BUTTON: COPY ---
		if (_copy.equals(source))
		{
			String asciiTable = SwingUtils.tableToString(_table);
			
			StringSelection data = new StringSelection(asciiTable);
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(data, data);
		}

		// --- BUTTON: CLOSE ---
		if (_close.equals(source))
		{
			setVisible(false);
		}

//		// --- BUTTON: CANCEL ---
//		if (_cancel.equals(source))
//		{
//			setVisible(false);
//		}
//
//		// --- BUTTON: OK ---
//		if (_ok.equals(source))
//		{
//			doApply();
//			saveProps();
//			setVisible(false);
//		}

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

	private class LocalTable extends GTable
	{
		private static final long serialVersionUID = 0L;
		private TableModel _tm;
		
		public LocalTable(TableModel tm)
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
			
//			_tm.addTableModelListener(new TableModelListener()
//			{
//				@Override
//				public void tableChanged(TableModelEvent e)
//				{
//					String watermark = null;
//					
//					if (_tm.getRowCount() == 0)
//						watermark = "No issues was found";
//
//					if (_tm.getRowCount() > 0)
//					{
//						if ( ! _dbmsConfig.hasConfigIssues() )
//							watermark = "All issues is mapped to be discarded...";
//					}
//
//					setWatermarkText(watermark);
//				}
//			});
			
//			// invoke a table change later... 
//			SwingUtilities.invokeLater(new Runnable()
//			{
//				@Override
//				public void run()
//				{
//					_tm.fireTableDataChanged();
//				}
//			});
		}

//		/** TABLE HEADER tool tip. */
//		@Override
//		protected JTableHeader createDefaultTableHeader()
//		{
//			JTableHeader tabHeader = new JXTableHeader(getColumnModel())
//			{
//	            private static final long serialVersionUID = 0L;
//
//				@Override
//				public String getToolTipText(MouseEvent e)
//				{
//					String tip = null;
//
//					int vcol = getColumnModel().getColumnIndexAtX(e.getPoint().x);
//					if (vcol == -1) return null;
//
//					int mcol = convertColumnIndexToModel(vcol);
//					if (mcol == -1) return null;
//
//					tip = _tm.getColumnToolTipText(mcol);
//
//					if (tip == null)
//						return null;
//					return "<html>" + tip + "</html>";
//				}
//			};
//
//			// Track where we are in the TableHeader, this is used by the Popup menus
//			// to decide what column of the TableHeader we are currently located on.
////			tabHeader.addMouseMotionListener(new MouseMotionListener()
////			{
////				@Override
////				public void mouseMoved(MouseEvent e)
////				{
////					_lastTableHeaderColumn = getColumnModel().getColumnIndexAtX(e.getX());
////					if (_lastTableHeaderColumn >= 0)
////						_lastTableHeaderColumn = convertColumnIndexToModel(_lastTableHeaderColumn);
////				}
////				@Override
////				public void mouseDragged(MouseEvent e) {/*ignore*/}
////			});
//
//			return tabHeader;
//		}

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
		
}
