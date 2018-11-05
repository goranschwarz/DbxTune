package com.asetune.gui.swing;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.table.TableColumnExt;

import com.asetune.Version;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class GTableHeaderPropertiesDialog
extends JDialog
implements ActionListener, TableModelListener
{
//	private static Logger _logger = Logger.getLogger(TrendGraphPanelReorderDialog.class);
	private static final long serialVersionUID = 1L;

////	private ImageIcon                _iconDialog      = new ImageIcon(Version.class.getResource("images/graph.png"));
//	private ImageIcon                _iconUp          = new ImageIcon(Version.class.getResource("images/up.png"));
//	private ImageIcon                _iconDown        = new ImageIcon(Version.class.getResource("images/down.png"));
//	private ImageIcon                _iconDialog      = SwingUtils.readImageIcon(Version.class, "images/graph.png");
	private ImageIcon                _iconUp          = SwingUtils.readImageIcon(Version.class, "images/up.png");
	private ImageIcon                _iconDown        = SwingUtils.readImageIcon(Version.class, "images/down.png");

	private MultiLineLabel           _description1    = new MultiLineLabel("Choose in what order the Columns will be arranged.");
	private MultiLineLabel           _description2    = new MultiLineLabel("You can also enable or disable Columns from here.");
	private JButton                  _up              = new JButton();
	private JButton                  _down            = new JButton();
	private JButton                  _toStartOrder    = new JButton("To Start Order");
	private JButton                  _toOriginOrder   = new JButton("To Original Order");
	private JButton                  _rmOrderAndVis   = new JButton("Clear saved info");
	private DefaultTableModel        _tableModel      = null;
	private JXTable                  _table           = null;
	private GTableFilter             _tableFilter     = null;
	
	private GTable                   _originTable     = null;
	private List<String>             _originOrder     = new ArrayList<String>(); // The way the Model looks like
	private List<String>             _orderAtStart    = new ArrayList<String>();  // what the order was when this dialog was called
	private DefaultTableModel        _tableModelAtStart = null; // this is used in method checkForChanges()

	private JButton                  _ok              = new JButton("OK");
	private JButton                  _cancel          = new JButton("Cancel");
	private JButton                  _apply           = new JButton("Apply");
	private int                      _dialogReturnSt  = JOptionPane.CANCEL_OPTION; //JOptionPane.CLOSED_OPTION;

	private enum     TabPos        { isVisible, ColumnName, ColumnToolTip }; 
	private String[] _tabHeadArr = {"Visible",  "Column Name", "ToolTip Description"};

	private GTableHeaderPropertiesDialog(Frame owner, GTable table)
	{
		super(owner, "Change Column Order and Visibility", true);
		_originTable = table;

		_originOrder  = _originTable.getOriginColumnOrderStrList();
		_orderAtStart = _originTable.getCurrentColumnOrderStrList();

		initComponents();
		_tableModelAtStart = SwingUtils.copyTableModel(_tableModel);
		pack();

		// Try to fit all rows on the open window
		Dimension size = getSize();
		size.height += (_table.getRowCount() - 6) * 18; // lets say 6 rows is the default showed AND each row takes 18 pixels
		size.width = Math.min(size.width, 645);
		setSize(size);
		
		SwingUtils.setSizeWithingScreenLimit(this, 5);
		
		// Focus to 'OK', escape to 'CANCEL'
		SwingUtils.installEscapeButton(this, _cancel);
		SwingUtils.setFocus(_ok);
	}

	/**
	 * Show dialog which can hide/show tabs or change the tab order. 
	 * @param owner
	 * @param gTabbedPane
	 * @return JOptionPane.CANCEL_OPTION or JOptionPane.OK_OPTION
	 */
	public static int showDialog(Frame owner, GTable table)
	{
		GTableHeaderPropertiesDialog dialog = new GTableHeaderPropertiesDialog(owner, table);

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
		JScrollPane jScrollPane = new JScrollPane();
		jScrollPane.setViewportView(_table);
		_tableFilter = new GTableFilter(_table);

		panel.add(_tableFilter, "growx, pushx, wrap");
		panel.add(jScrollPane, "span, grow, height 100%, push, wrap");


		panel.add(_up,             "tag left, span, split");
		panel.add(_down,           "tag left");
		panel.add(_toStartOrder,   "tag left");
		panel.add(_toOriginOrder,  "tag left");
		panel.add(_rmOrderAndVis,  "tag right, wrap push");
		
		panel.add(createOkPanel(), "gap top 20, right");

		// Initial state for buttons
		_apply.setEnabled(false);
		
		setContentPane(panel);

		// ADD ACTIONS TO COMPONENTS
		_up           .addActionListener(this);
		_down         .addActionListener(this);
		_toStartOrder .addActionListener(this);
		_toOriginOrder.addActionListener(this);
		_rmOrderAndVis.addActionListener(this);
		
		// Set some tooltip
		_up           .setToolTipText("Move the Column up");
		_down         .setToolTipText("Move the Column down)");
		_toStartOrder .setToolTipText("Restore the Columns to the order it had when this dialog was opened.");
		_toOriginOrder.setToolTipText("Restore the Columns to the order it originally was created as (restore to factory setting).");
		_rmOrderAndVis.setToolTipText("<html>" +
		                                   "Remove/clear the persisted values for Column order and the visibility.<br>" +
		                                   "This is usually stored in the configuration or properties file." +
		                              "</html>");

		_up  .setIcon(_iconUp);
		_down.setIcon(_iconDown);
//		if (_iconDialog != null)
//			setIconImage(_iconDialog.getImage());

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
		LinkedHashMap<String, ColumnHeaderPropsEntry> newOrder = new LinkedHashMap<String, ColumnHeaderPropsEntry>();

		// Loop all the rows in the table and make changes
		// NOTE: we might want to use the model instead, then we don't have to reset the filter before apply...
		for (int r=0; r<_tableModel.getRowCount(); r++)
		{
			String  colName = (String) _tableModel.getValueAt(r, TabPos.ColumnName.ordinal());
			Boolean visible = (Boolean)_tableModel.getValueAt(r, TabPos.isVisible.ordinal());

			newOrder.put(colName, new ColumnHeaderPropsEntry(colName, -1, r, visible, SortOrder.UNSORTED, -1, -1));
		}
//		// Loop all the rows in the table and make changes
//		for (int r=0; r<_table.getRowCount(); r++)
//		{
//			String  colName = (String) _table.getValueAt(r, TabPos.ColumnName.ordinal());
//			Boolean visible = (Boolean)_table.getValueAt(r, TabPos.isVisible.ordinal());
//
//			newOrder.put(colName, new ColumnHeaderPropsEntry(colName, -1, r, visible, SortOrder.UNSORTED, -1, -1));
//		}

		// make the new order
		_originTable.loadColumnLayout(newOrder);

		// save the info...
		_originTable.saveColumnLayout();

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
				dtm.moveRow(row, row, toRow);
				_table.getSelectionModel().setSelectionInterval(toRow, toRow);
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
				dtm.moveRow(row, row, toRow);
				_table.getSelectionModel().setSelectionInterval(toRow, toRow);
			}
		}

		// --- BUTTON: TO_START_ORDER ---
		if (_toStartOrder.equals(source))
		{
			toColumnOrder(_tableModel, _orderAtStart);
		}

		// --- BUTTON: TO_ORIGIN_ORDER ---
		if (_toOriginOrder.equals(source))
		{
			toColumnOrder(_tableModel, _originOrder);
		}

		// --- BUTTON: REMOVE_TAB_VISIBILITY_AND_ORDER ---
		if (_rmOrderAndVis.equals(source))
		{
			_originTable.setOriginalColumnLayout();
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

	private void toColumnOrder(DefaultTableModel tm, List<String> originColumnOrder)
	{
//		System.out.println("toColumnOrder(): ___CALLED___: originTabOrder()="+originTabOrder.size());
//		System.out.println("toColumnOrder(): originTabOrderList="+originTabOrder);
		for (int oi=0; oi<originColumnOrder.size(); oi++)
		{
			String columnhName = originColumnOrder.get(oi);
			int moveRow = -1;
			for (int tr=0; tr<tm.getRowCount(); tr++)
			{
//				System.out.println("toGraphOrder()-SEARCH: originListIndex="+oi+", originTabName="+StringUtil.left(tabName, 20)+", tabRow="+tr+", tabRowName="+tm.getValueAt(tr, TabPos.Name.ordinal()));
				if ( columnhName.equals( tm.getValueAt(tr, TabPos.ColumnName.ordinal()) ) )
				{
					moveRow = tr;
//					System.out.println("toGraphOrder()--FOUND: tabRow="+tr+", tabRowName="+tm.getValueAt(tr, TabPos.Name.ordinal()));
					break;
				}
			}
			if (moveRow >= 0 && oi != moveRow)
			{
//				System.out.println("toGraphOrder()---MOVE: from="+moveRow+", to="+oi+", tabName="+tabName);
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
		for (int r=0; r<_table.getRowCount(); r++)
		{
			String  rowColName = (String) _table.getValueAt(r, TabPos.ColumnName.ordinal());
			Boolean rowVisible = (Boolean)_table.getValueAt(r, TabPos.isVisible.ordinal());

			String  startColName = (String) _tableModelAtStart.getValueAt(r, TabPos.ColumnName.ordinal());
			Boolean startVisible = (Boolean)_tableModelAtStart.getValueAt(r, TabPos.isVisible.ordinal());

			if ( ! rowColName.equals(startColName) ) enabled = true;
			if ( ! rowVisible.equals(startVisible) ) enabled = true;

//			System.out.println("checkForChanges() CURRENT: row="+r+", colName="+StringUtil.left(rowColName,   20)+", enabled="+rowEnabled);
//			System.out.println("checkForChanges()   START: row="+r+", colName="+StringUtil.left(startColName, 20)+", enabled="+startEnabled);
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
		tabHead.set(TabPos.isVisible    .ordinal(), _tabHeadArr[TabPos.isVisible    .ordinal()]);
		tabHead.set(TabPos.ColumnName   .ordinal(), _tabHeadArr[TabPos.ColumnName   .ordinal()]);
		tabHead.set(TabPos.ColumnToolTip.ordinal(), _tabHeadArr[TabPos.ColumnToolTip.ordinal()]);

		Vector<Vector<Object>> tabData = populateTable();

		_tableModel = new DefaultTableModel(tabData, tabHead)
		{
            private static final long serialVersionUID = 1L;

			@Override
			public Class<?> getColumnClass(int column) 
			{
				if (column == TabPos.isVisible.ordinal()) return Boolean.class;
				return Object.class;
			}
			@Override
			public boolean isCellEditable(int row, int col)
			{
				if (col == TabPos.isVisible.ordinal())
					return true;

				return false;
			}
		};
		toColumnOrder(_tableModel, _orderAtStart);
		_tableModel.addTableModelListener(this);

		JXTable table = new JXTable(_tableModel);
//		table.setModel( defaultTabModel );
		table.setSortable(false);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setShowGrid(false);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// Hide column GRAPH NAME
//		TableColumnExt tcx = ((TableColumnModelExt)table.getColumnModel()).getColumnExt(TabPos.GraphName.ordinal());
//		if (tcx != null)
//			tcx.setVisible(false);

		SwingUtils.calcColumnWidths(table);

		return table;
	}

	private Vector<Vector<Object>> populateTable()
	{
		Vector<Vector<Object>> tab = new Vector<Vector<Object>>();
		Vector<Object>         row = new Vector<Object>();

		// Create all column checkbox entries "on the fly"
		for (TableColumn tc : _originTable.getColumns(true))
		{
			final TableColumnExt tcx = (TableColumnExt) tc;

			boolean colIsVisible = tcx.isVisible();
			String  columnName   = tcx.getHeaderValue() + "";
			String  toolTip      = StringUtil.stripHtml(_originTable.getToolTipTextForColumn(columnName));


			row = new Vector<Object>();
			row.setSize(TabPos.values().length);
			
			row.set(TabPos.isVisible    .ordinal(), colIsVisible);
			row.set(TabPos.ColumnName   .ordinal(), columnName);
			row.set(TabPos.ColumnToolTip.ordinal(), toolTip);

			tab.add(row);
		}

		return tab;
	}

	//--------------------------------------------------
	//--------------------------------------------------
	//--------------------------------------------------
	// TEST-CODE
	//--------------------------------------------------
	//--------------------------------------------------
	//--------------------------------------------------
	
}
