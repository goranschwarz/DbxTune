/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.gui.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.apache.log4j.PropertyConfigurator;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.table.TableColumnModelExt;

import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;


public class GTabbedPaneViewDialog
	extends JDialog
	implements ActionListener, TableModelListener
{
//	private static Logger _logger = Logger.getLogger(GTabbedPaneViewDialog.class);
	private static final long serialVersionUID = 1L;

//	private ImageIcon               _iconTab        = new ImageIcon(GTabbedPaneViewDialog.class.getResource("images/tab.gif"));
//	private ImageIcon               _iconUp         = new ImageIcon(GTabbedPaneViewDialog.class.getResource("images/up.png"));
//	private ImageIcon               _iconDown       = new ImageIcon(GTabbedPaneViewDialog.class.getResource("images/down.png"));
	private ImageIcon               _iconTab        = SwingUtils.readImageIcon(GTabbedPaneViewDialog.class, "images/tab.gif");
	private ImageIcon               _iconUp         = SwingUtils.readImageIcon(GTabbedPaneViewDialog.class, "images/up.png");
	private ImageIcon               _iconDown       = SwingUtils.readImageIcon(GTabbedPaneViewDialog.class, "images/down.png");

	private MultiLineLabel          _description1   = new MultiLineLabel("Choose in what order the Tabs 'Title names' will be arranged on the Tab Pane.");
	private MultiLineLabel          _description2   = new MultiLineLabel("You can also hide some Tabs, which you think you may never use.");
	private JButton                 _up             = new JButton();
	private JButton                 _down           = new JButton();
	private JButton                 _toStartOrder   = new JButton("To Start Order");
	private JButton                 _toOriginOrder  = new JButton("To Original Order");
	private JButton                 _rmTabOrderVis  = new JButton("Clear saved info");
	private DefaultTableModel       _tableModel     = null;
	private JXTable                 _table          = null;
	private GTableFilter            _tableFilter    = null;
	
	private GTabbedPane             _gTabbedPane    = null;
	private List<String>            _modelOrder     = new ArrayList<String>(); // The way the Model looks like
	private List<String>            _orderAtStart   = new ArrayList<String>();  // what the order was when this dialog was called
	private DefaultTableModel       _tableModelAtStart = null; // this is used in method checkForChanges()

	private JButton                 _ok             = new JButton("OK");
	private JButton                 _cancel         = new JButton("Cancel");
	private JButton                 _apply          = new JButton("Apply");
	private int                     _dialogReturnSt = JOptionPane.CANCEL_OPTION; //JOptionPane.CLOSED_OPTION;

	/** used to determine if the columns 'GroupIcon' and 'GroupName' should be visible when showing the dialog */
	private boolean _hasGroups = false;

	private enum TabPos {GroupIcon, GroupName, TabIcon, TabVisible, TabName, TabDescription}; 

	private GTabbedPaneViewDialog(Frame owner, GTabbedPane gTabbedPane)
	{
		super(owner, "Change Tabs Order and Visibility", true);
		_gTabbedPane = gTabbedPane;

		_modelOrder   = gTabbedPane.getModelTabOrder();
		_orderAtStart = gTabbedPane.getTabOrder(true);

		initComponents();
		_tableModelAtStart = SwingUtils.copyTableModel(_tableModel);
		pack();

		// Try to fit all rows on the open window
		Dimension size = getSize();
		size.height += (_table.getRowCount() - 6) * 18; // lets say 6 rows is the default showed AND each row takes 18 pixels
		size.width = Math.min(size.width, 700);
		setSize(size);
	}

	/**
	 * Show dialog which can hide/show tabs or change the tab order. 
	 * @param owner
	 * @param gTabbedPane
	 * @return JOptionPane.CANCEL_OPTION or JOptionPane.OK_OPTION
	 */
	public static int showDialog(Frame owner, GTabbedPane gTabbedPane)
	{
		GTabbedPaneViewDialog dialog = new GTabbedPaneViewDialog(owner, gTabbedPane);

		dialog.setLocationRelativeTo(owner);
		dialog.setVisible(true);
		dialog.dispose();
		
		return dialog._dialogReturnSt;
	}

	/*---------------------------------------------------
	** BEGIN: component initialization
	**---------------------------------------------------
	*/
	protected void initComponents() 
	{
		JPanel panel = new JPanel();
//		panel.setLayout(new MigLayout("insets 20 20","[][grow]",""));   // insets Top Left Bottom Right
		panel.setLayout(new MigLayout());   // insets Top Left Bottom Right

		panel.add(_description1, "grow, wrap");
		panel.add(_description2, "grow, wrap 10");

//		panel.add(_table,          "grow, wrap");
		_table = createTable();
		_tableFilter = new GTableFilter(_table);
		JScrollPane jScrollPane = new JScrollPane();
		jScrollPane.setViewportView(_table);

		panel.add(_tableFilter, "growx, pushx, wrap");
		panel.add(jScrollPane, "span, grow, height 100%, push, wrap");


		panel.add(_up,             "tag left, span, split");
		panel.add(_down,           "tag left");
		panel.add(_toStartOrder,   "tag left");
		panel.add(_toOriginOrder,  "tag left");
		panel.add(_rmTabOrderVis,  "tag right, wrap push");
		
		panel.add(createOkPanel(), "gap top 20, right");

		// Initial state for buttons
		_apply.setEnabled(false);
		
		setContentPane(panel);

		// ADD ACTIONS TO COMPONENTS
		_up           .addActionListener(this);
		_down         .addActionListener(this);
		_toStartOrder .addActionListener(this);
		_toOriginOrder.addActionListener(this);
		_rmTabOrderVis.addActionListener(this);
		
		// Set some tooltip
		_up           .setToolTipText("Move the tab up (or to left in the TabbedPane)");
		_down         .setToolTipText("Move the tab down (or to right in the TabbedPane)");
		_toStartOrder .setToolTipText("Restore the tabs to the order it had when this dialog was opened.");
		_toOriginOrder.setToolTipText("Restore the tabs to the order it originally was when TabbedPane was created.");
		_rmTabOrderVis.setToolTipText("<html>" +
		                                   "Remove/clear the persisted values for TabTitles order and the visibility.<br>" +
		                                   "This is usually stored in the configuration or properties file." +
		                              "</html>");

		_up  .setIcon(_iconUp);
		_down.setIcon(_iconDown);
		if (_iconTab != null)
			setIconImage(_iconTab.getImage());
//		_up  .setIcon(SwingUtils.readImageIcon(GTabbedPaneViewDialog.class, "images/up.png"));
//		_down.setIcon(SwingUtils.readImageIcon(GTabbedPaneViewDialog.class, "images/down.png"));
//		setIconImage( SwingUtils.readImageIcon(GTabbedPaneViewDialog.class, "images/tab.gif").getImage());

		if (_up  .getIcon() == null) _up  .setText("Move Up");
		if (_down.getIcon() == null) _down.setText("Move Down");

		pack();
	}

	JPanel createOkPanel()
	{
		// ADD the OK, Cancel, Apply buttons
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("insets 0 0","",""));
		panel.add(_ok,     "tag ok");
		panel.add(_cancel, "tag cancel");
		panel.add(_apply,  "tag apply");
		
		_ok    .addActionListener(this);
		_cancel.addActionListener(this);
		_apply .addActionListener(this);

		return panel;
	}
	/*---------------------------------------------------
	** END: component initialization
	**---------------------------------------------------
	*/
	private void apply()
	{
		// select NO rows.
		_table.clearSelection();

		// keep the new tab order in a list.
		List<String> newTabOrder = new ArrayList<String>();

		// Loop all the rows in the table and make changes
		TableModel tm = _table.getModel();
		for (int r=0; r<tm.getRowCount(); r++)
		{
			String groupName = (String) tm.getValueAt(r, TabPos.GroupName .ordinal());
			String tabName   = (String) tm.getValueAt(r, TabPos.TabName   .ordinal());
			Boolean visible  = (Boolean)tm.getValueAt(r, TabPos.TabVisible.ordinal());

			_gTabbedPane.setVisibleAtModel(groupName, tabName, visible);

			String addStr = tabName;
			if ( ! StringUtil.isNullOrBlank(groupName) )
				addStr = groupName + GTabbedPane.GROUP_STR_SEPARATOR + tabName;

			if (visible)
				newTabOrder.add(addStr);
		}

		// make the new tab order in the GTabbedPane
		_gTabbedPane.setTabOrder(newTabOrder);

		// save the info...
		_gTabbedPane.saveTabOrderAndVisibility();

		_apply.setEnabled(false);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();

		// --- BUTTON: UP ---
		if (_up.equals(source))
		{
			int row   = _table.getSelectedRow();
			if (row < 0)
			{
				SwingUtils.showInfoMessage(this, "Select a row", "No row in the table is selected.");
				return;
			}
			int toRow = row - 1;
			DefaultTableModel dtm = (DefaultTableModel) _table.getModel();
			if (toRow >= 0)
			{
				String atGroupName = dtm.getValueAt(row,   TabPos.GroupName.ordinal()) + "";
				String toGroupName = dtm.getValueAt(toRow, TabPos.GroupName.ordinal()) + "";

				if (atGroupName.equals(toGroupName))
				{
					dtm.moveRow(row, row, toRow);
					_table.getSelectionModel().setSelectionInterval(toRow, toRow);
				}
				else
				{
					SwingUtils.showInfoMessage(this, "Can't move to another group", "<html>Trying to move into a new group<br>This is not supported for the moment<br><br>From group: <b>"+atGroupName+"</b><br>To group: <b>"+toGroupName+"</b><br></html>");
					return;
				}
			}
		}

		// --- BUTTON: DOWN ---
		if (_down.equals(source))
		{
			int row   = _table.getSelectedRow();
			if (row < 0)
			{
				SwingUtils.showInfoMessage(this, "Select a row", "No row in the table is selected.");
				return;
			}
			int toRow = row + 1;
			DefaultTableModel dtm = (DefaultTableModel) _table.getModel();
			if (toRow < dtm.getRowCount())
			{
				String atGroupName = dtm.getValueAt(row,   TabPos.GroupName.ordinal()) + "";
				String toGroupName = dtm.getValueAt(toRow, TabPos.GroupName.ordinal()) + "";

				if (atGroupName.equals(toGroupName))
				{
					dtm.moveRow(row, row, toRow);
					_table.getSelectionModel().setSelectionInterval(toRow, toRow);
				}
				else
				{
					SwingUtils.showInfoMessage(this, "Can't move to another group", "<html>Trying to move into a new group<br>This is not supported for the moment<br><br>From group: <b>"+atGroupName+"</b><br>To group: <b>"+toGroupName+"</b><br></html>");
					return;
				}
			}
		}

		// --- BUTTON: TO_START_ORDER ---
		if (_toStartOrder.equals(source))
		{
			toTabOrder(_tableModel, _orderAtStart);
		}

		// --- BUTTON: TO_ORIGIN_ORDER ---
		if (_toOriginOrder.equals(source))
		{
			toTabOrder(_tableModel, _modelOrder);
		}

		// --- BUTTON: REMOVE_TAB_VISIBILITY_AND_ORDER ---
		if (_rmTabOrderVis.equals(source))
		{
			_gTabbedPane.removeTabOrderAndVisibility();
		}

		// --- BUTTON: OK ---
		if (_ok.equals(source))
		{
			apply();
			_dialogReturnSt = JOptionPane.OK_OPTION;
			setVisible(false);
		}

		// --- BUTTON: CANCEL ---
		if (_cancel.equals(source))
		{
			_dialogReturnSt = JOptionPane.CANCEL_OPTION;
			setVisible(false);
		}

		// --- BUTTON: APPLY ---
		if (_apply.equals(source))
		{
			apply();
		}
	}

	private void toTabOrder(DefaultTableModel tm, List<String> tabOrder)
	{
//		System.out.println("toTabOrder(): ___CALLED___: originTabOrder()="+originTabOrder.size());
//		System.out.println("toTabOrder(): originTabOrderList="+originTabOrder);
		for (int oi=0; oi<tabOrder.size(); oi++)
		{
			String entry = tabOrder.get(oi);
			String[] sa = entry.split(GTabbedPane.GROUP_STR_SEPARATOR);

			String groupName = "";
			String tabName   = entry;
			if (sa.length > 1)
			{
				groupName = sa[ sa.length - 2 ];
				tabName   = sa[ sa.length - 1 ];
			}
			int moveRow = -1;
			for (int tr=0; tr<tm.getRowCount(); tr++)
			{
//				System.out.println("toTabOrder()-SEARCH: originListIndex="+oi+", originTabName="+StringUtil.left(tabName, 20)+", tabRow="+tr+", tabRowName="+tm.getValueAt(tr, TabPos.TabName.ordinal()));
				Object rowTabName   = tm.getValueAt(tr, TabPos.TabName  .ordinal());
				Object rowGroupName = tm.getValueAt(tr, TabPos.GroupName.ordinal());
				if (rowGroupName == null)
					rowGroupName = "";

				if ( tabName.equals(rowTabName) && (groupName.equals(rowGroupName)) )
				{
					moveRow = tr;
//					System.out.println("toTabOrder()--FOUND: tabRow="+tr+", tabRowName="+tm.getValueAt(tr, TabPos.TabName.ordinal()));
					break;
				}
			}
			if (moveRow >= 0 && oi != moveRow)
			{
//				System.out.println("toTabOrder()---MOVE: from="+moveRow+", to="+oi+", tabName="+tabName);
				tm.moveRow(moveRow, moveRow, oi);
			}
		}
//		_table.getSelectionModel().setSelectionInterval(-1, -1);
		
	}
//	private ActionListener     _actionListener  = new ActionListener()
//	{
//		public void actionPerformed(ActionEvent actionevent)
//		{
//			checkForChanges();
//		}
//	};
//	private KeyListener        _keyListener  = new KeyListener()
//	{
//		 // Changes in the fields are visible first when the key has been released.
//		public void keyPressed (KeyEvent keyevent) {}
//		public void keyTyped   (KeyEvent keyevent) {}
//		public void keyReleased(KeyEvent keyevent) { checkForChanges(); }
//	};

	private void checkForChanges()
	{
//		System.out.println("checkForChanges()--------------------------");
		boolean enabled = false;

		// Check for changes
		TableModel tm = _table.getModel();
		for (int r=0; r<tm.getRowCount(); r++)
		{
			String  rowTabName = (String) tm.getValueAt(r, TabPos.TabName.ordinal());
			Boolean rowVisible = (Boolean)tm.getValueAt(r, TabPos.TabVisible.ordinal());

			String  startTabName = (String) _tableModelAtStart.getValueAt(r, TabPos.TabName.ordinal());
			Boolean startVisible = (Boolean)_tableModelAtStart.getValueAt(r, TabPos.TabVisible.ordinal());

			if ( ! rowTabName.equals(startTabName) ) enabled = true;
			if ( ! rowVisible.equals(startVisible) ) enabled = true;

//			System.out.println("checkForChanges() CURRENT: row="+r+", tabName="+StringUtil.left(rowTabName,   20)+", visible="+rowVisible);
//			System.out.println("checkForChanges()   START: row="+r+", tabName="+StringUtil.left(startTabName, 20)+", visible="+startVisible);
//			System.out.println("checkForChanges()   enabled="+enabled);
			
			if (enabled)
				break;
		}
		_apply.setEnabled(enabled);
	}

	/* Called on fire* has been called on the TableModel */
	@Override
	public void tableChanged(final TableModelEvent e)
	{
		checkForChanges();
	}

	public JXTable createTable()
	{
		// Create a TABLE
		Vector<String> tabHead = new Vector<String>();
		tabHead.setSize(TabPos.values().length);
		tabHead.set(TabPos.GroupIcon     .ordinal(), "Group");
		tabHead.set(TabPos.GroupName     .ordinal(), "GroupName");
		tabHead.set(TabPos.TabIcon       .ordinal(), "Icon");
		tabHead.set(TabPos.TabVisible    .ordinal(), "Visible");
		tabHead.set(TabPos.TabName       .ordinal(), "Name");
		tabHead.set(TabPos.TabDescription.ordinal(), "Description");

		Vector<Vector<Object>> tabData = populateTable();

		_tableModel = new DefaultTableModel(tabData, tabHead)
		{
            private static final long serialVersionUID = 1L;

			@Override
			public Class<?> getColumnClass(int column) 
			{
				if (column == TabPos.GroupIcon .ordinal()) return Icon.class;
				if (column == TabPos.TabIcon   .ordinal()) return Icon.class;
				if (column == TabPos.TabVisible.ordinal()) return Boolean.class;
				return Object.class;
			}
			@Override
			public boolean isCellEditable(int row, int col)
			{
				if (col == TabPos.TabVisible.ordinal())
					return true;

				return false;
			}
		};
		toTabOrder(_tableModel, _orderAtStart);
		_tableModel.addTableModelListener(this);

		JXTable table = new JXTable(_tableModel);
//		table.setModel( defaultTabModel );
		table.setSortable(false);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setShowGrid(false);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// Hide columns if not in group mode
		if ( ! _hasGroups )
		{
			TableColumnModelExt tcmx = (TableColumnModelExt)table.getColumnModel();
			tcmx.getColumnExt("Group")    .setVisible(false);
			tcmx.getColumnExt("GroupName").setVisible(false);
		}

		SwingUtils.calcColumnWidths(table);

		return table;
	}

//	private Vector<Vector<Object>> populateTable()
//	{
//		Vector<Vector<Object>> tab = new Vector<Vector<Object>>();
//		Vector<Object>         row = new Vector<Object>();
//
//		for (int t=0; t<_gTabbedPane.getModelTabCount(); t++)
//		{
//			row = new Vector<Object>();
//			row.setSize(TabPos.values().length);
//			
//			row.set(TabPos.TabIcon       .ordinal(), _gTabbedPane.getIconAtModel(t));
//			row.set(TabPos.TabVisible    .ordinal(), _gTabbedPane.isVisibleAtModel(t));
//			row.set(TabPos.TabName       .ordinal(), _gTabbedPane.getTitleAtModel(t));
//			row.set(TabPos.TabDescription.ordinal(), StringUtil.stripHtml(_gTabbedPane.getToolTipTextAtModel(t)));
//
//			tab.add(row);
//		}
//
//		return tab;
//	}
	private Vector<Vector<Object>> populateTable()
	{
		Vector<Vector<Object>> tab = new Vector<Vector<Object>>();
		populateTable(null, 0, _gTabbedPane, tab);

//System.out.println("GTabbedPane:populateTable() table="+tab);
//		for (int i=0; i<tab.size(); i++)
//			System.out.println("row("+i+")="+tab.get(i));
//		for (Vector<Object> v : tab)
//			System.out.println("row="+v);

		return tab;
	}

	private void populateTable(GTabbedPane parentTabPane, int parentPos, GTabbedPane tabPane, Vector<Vector<Object>> table)
	{
//System.out.println("GTabbedPane:populateTable() parentTabPane="+(parentTabPane==null?"null":parentTabPane.getTitleAtModel(parentPos))+", parentPos="+parentPos);
		for (int t=0; t<tabPane.getModelTabCount(); t++)
		{
			Component comp = tabPane.getComponentAtModel(t);
			if (comp instanceof GTabbedPane)
				populateTable(tabPane, t, (GTabbedPane)comp, table);
			else
			{
				Vector<Object>         row = new Vector<Object>();

				row = new Vector<Object>();
				row.setSize(TabPos.values().length);
				
				if (parentTabPane != null)
				{
					row.set(TabPos.GroupIcon.ordinal(), parentTabPane.getIconAtModel(parentPos));
					row.set(TabPos.GroupName.ordinal(), parentTabPane.getTitleAtModel(parentPos));
					
					_hasGroups = true;
				}

				row.set(TabPos.TabIcon       .ordinal(), tabPane.getIconAtModel(t));
				row.set(TabPos.TabVisible    .ordinal(), tabPane.isVisibleAtModel(t));
				row.set(TabPos.TabName       .ordinal(), tabPane.getTitleAtModel(t));
				row.set(TabPos.TabDescription.ordinal(), StringUtil.stripHtml(tabPane.getToolTipTextAtModel(t)));

//System.out.println("  GTabbedPane:populateTable() row.set(tabName='"+tabPane.getTitleAtModel(t)+"')");
//System.out.println("  -table.add:row="+row);
				table.add(row);				
			}

		}
	}

	//--------------------------------------------------
	//--------------------------------------------------
	//--------------------------------------------------
	// TEST-CODE
	//--------------------------------------------------
	//--------------------------------------------------
	//--------------------------------------------------
	
	@SuppressWarnings("serial")
	private static class TestClass
	extends JFrame
	implements ChangeListener
	{
		GTabbedPane _gtabs = new GTabbedPane();

		TestClass()
		{
			setTitle("MigLayout Samples");
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			
			_gtabs.add(  "0-Summary",     new TabularCntrPanel("0-Summary") );
			_gtabs.add(  "1-Object",      new TabularCntrPanel("1-Object") );
			_gtabs.add(  "2-Processes",   new TabularCntrPanel("2-Processes") );
			_gtabs.add(  "3-Databases",   new TabularCntrPanel("3-Databases") );
			_gtabs.add(  "4-Waits",       new TabularCntrPanel("4-Waits") );
			_gtabs.add(  "5-Engines",     new TabularCntrPanel("5-Engines") );
			_gtabs.add(  "6-Data Caches", new TabularCntrPanel("6-Data Caches") );
			_gtabs.add(  "7-Pools",       new TabularCntrPanel("7-Pools") );
			_gtabs.add(  "8-Devices",     new TabularCntrPanel("8-Devices") );
			
			GTabbedPane grp1 = new GTabbedPane();
				grp1.add("0-grp1", new TabularCntrPanel("0-grp1"));
				grp1.add("1-grp1", new TabularCntrPanel("1-grp1"));
				grp1.add("2-grp1", new TabularCntrPanel("2-grp1"));
				grp1.add("3-grp1", new TabularCntrPanel("3-grp1"));
			GTabbedPane grp2 = new GTabbedPane();
				grp2.add("0-grp2", new TabularCntrPanel("0-grp2"));
				grp2.add("1-grp2", new TabularCntrPanel("1-grp2"));
				grp2.add("2-grp2", new TabularCntrPanel("2-grp2"));
			GTabbedPane grp3 = new GTabbedPane();
				grp3.add("0-grp3", new TabularCntrPanel("0-grp3"));
				grp3.add("1-grp3", new TabularCntrPanel("1-grp3"));

			_gtabs.add("Group-1", grp1);
			_gtabs.add("Group-2", grp2);
			_gtabs.add("Group-3", grp3);

			add(_gtabs);

			_gtabs.addChangeListener(this);
			
			JMenuBar main_mb = new JMenuBar();
			setJMenuBar(main_mb);

			JMenu menu = new JMenu("Test Cases");
			main_mb.add(menu);

			JMenuItem mi = null;

			//----------------------------------------------------------
			mi = new JMenuItem("Show Counter View Dialog");
			mi.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					System.out.println("ACTION: Show Counter View Dialog.");
					int ret = GTabbedPaneViewDialog.showDialog(null, _gtabs);
					System.out.println("RESPONSE: GTabbedPaneViewDialog.showDialog(...) returned="+ret);
				}
			});
			menu.add(mi);

			//----------------------------------------------------------
			mi = new JMenuItem("Hide all columns");
			mi.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					System.out.println("ACTION: Hide all columns.");
//					int loopCount = final_gtabs.getTabCount();
//					for (int i=0; i<loopCount; i++)
//					{
//						String tabName = final_gtabs.getTitleAt(0);
//						final_gtabs.setVisibleAtModel(tabName, false);
//					}

					// Another way to do it.
					_gtabs.setVisibleAtModel(_gtabs.getTabOrder(false), false);
				}
			});
			menu.add(mi);

			//----------------------------------------------------------
			final String[] sa1 = {"1-Object", "4-Waits", "7-Pools"};
			mi = new JMenuItem("setTabOrder(String[]: '"+Arrays.asList(sa1)+"'.");
			mi.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					System.out.println("ACTION: setTabOrder(String[]: '"+Arrays.asList(sa1)+"'.");
					_gtabs.setTabOrder(sa1);
				}
			});
			menu.add(mi);

			//----------------------------------------------------------
			final String[] sa2 = {"1-Object=true", "4-Waits=false", "7-Pools"};
			mi = new JMenuItem("setTabOrderAndVisibility(String[]: '"+Arrays.asList(sa2)+"'.");
			mi.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					System.out.println("ACTION: setTabOrderAndVisibility(String[]: '"+Arrays.asList(sa2)+"'.");
					_gtabs.setTabOrderAndVisibility(sa2);
				}
			});
			menu.add(mi);

			//----------------------------------------------------------
			final String str = "1-Object=true, 4-Waits,7-Pools=FALSE";
			mi = new JMenuItem("setTabOrderAndVisibility(String: '"+str+"')");
			mi.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					System.out.println("ACTION: setTabOrderAndVisibility(String: '"+str+"')");
					_gtabs.setTabOrderAndVisibility(str);
				}
			});
			menu.add(mi);

			//----------------------------------------------------------
			mi = new JMenuItem("getTabOrderAndVisibility()");
			mi.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					System.out.println("ACTION: getTabOrderAndVisibility()");
					System.out.println("RESULT STRING: '"+_gtabs.getTabOrderAndVisibility()+"'.");
				}
			});
			menu.add(mi);

			pack();
		}

		@Override
		public void stateChanged(ChangeEvent e)
		{
			Object source = e.getSource();

			System.out.println("TestClass: stateChanged() source="+source+", ChangeEvent="+e);
			if (source.equals(_gtabs))
			{
				int selectedTab = _gtabs.getSelectedIndex();
				if (selectedTab < 0)
				{
					System.out.println("TestClass: stateChanged(): NO TAB was selected.");
					return;
				}

				String currentTab = _gtabs.getTitleAt(selectedTab);
				System.out.println("TestClass: stateChanged(): selected tab = '"+currentTab+"'.");
			}
		}
	}

	public static void main(String[] args)
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		Properties log4jProps = new Properties();
		//log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		Configuration conf2 = new Configuration("c:\\projects\\asetune\\conf\\dbxtune.properties");
		Configuration.setInstance(Configuration.SYSTEM_CONF, conf2);

		TestClass testme = new TestClass();
		testme.setVisible(true);
	}
}

