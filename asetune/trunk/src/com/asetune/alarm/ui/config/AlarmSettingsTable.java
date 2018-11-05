package com.asetune.alarm.ui.config;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTableHeader;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.asetune.alarm.ui.config.AlarmTableModel.AlarmEntry;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

public class AlarmSettingsTable extends JXTable
{
	private static final long serialVersionUID = 0L;
	private static Logger _logger = Logger.getLogger(AlarmSettingsTable.class);
	
//	protected int           _lastTableHeaderPointX = -1;
	protected int           _lastTableHeaderColumn = -1;
	private   JPopupMenu    _popupMenu             = null;
	private   JPopupMenu    _headerPopupMenu       = null;

	AlarmSettingsTableModel _alarmSettingsTableModel;
	
	AlarmSettingsTable(AlarmSettingsTableModel alarmSettingsTableModel)
	{
		super();
		
		_alarmSettingsTableModel = alarmSettingsTableModel;
		setModel( alarmSettingsTableModel );

		setShowGrid(false);
		setSortable(true);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		packAll(); // set size so that all content in all cells are visible
		setColumnControlVisible(true);
		setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);
//		setHighlighters(_highliters);

		// Create some PopupMenus and attach them
		_popupMenu = createDataTablePopupMenu();
		setComponentPopupMenu(getDataTablePopupMenu());

		_headerPopupMenu = createDataTableHeaderPopupMenu();
		getTableHeader().setComponentPopupMenu(getDataTableHeaderPopupMenu());

		// COLOR CODE SOME ROWS/CELLS
		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

		if (conf != null) colorStr = conf.getProperty(getName()+".color.isNotDefaultValue");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				if (AlarmSettingsTableModel.TAB_POS_IS_DEFAULT == convertColumnIndexToModel(adapter.column))
				{
					CmSettingsHelper awse = _alarmSettingsTableModel.getSettingForRow(convertRowIndexToModel(adapter.row));
					if ( ! awse.isDefaultValue() )
						return true;
				}
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.LIGHT_GRAY), null));

		// Populate the table
//		refreshTable("CmXxXxXxXxXxXxXx");
		
//		getSelectionModel().addListSelectionListener(new ListSelectionListener()
//		{
//			@Override
//			public void valueChanged(ListSelectionEvent e)
//			{
//				AlarmSettingsTable table = AlarmSettingsTable.this;
//				System.out.println("AlarmConfigDialog.LocalCmAlarmSettingsTable: table.getSelectedRow()="+table.getSelectedRow());
//				System.out.println("AlarmConfigDialog.LocalCmAlarmSettingsTable.valueChanged(): e="+e);
//				if (e.getValueIsAdjusting())
//					return;
//				
//				// In the detailes section: Load the selected row 
//				int vrow = table.getSelectedRow();
//				if (vrow == -1)
//					return;
//				int mrow = table.convertRowIndexToModel(vrow);
//				String cmName = table.getModel().getValueAt(mrow, AlarmTableModel.TAB_POS_CM_NAME) + "";
//				System.out.println("AlarmConfigDialog.LocalCmAlarmSettingsTable: LOAD CM='"+cmName+"'.");
//			}
//		});
	}

//	public void setCm(String cmName)
//	{
////		_javaEditor_txt.putClientProperty("currentCmName", cmName);
//
//		refreshTable(cmName);
//	}

	/** What table header was the last header we visited */
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

				int vcol = getColumnModel().getColumnIndexAtX(e.getPoint().x);
				if (vcol == -1) return null;

				int mcol = convertColumnIndexToModel(vcol);
				if (mcol == -1) return null;

				tip = _alarmSettingsTableModel.getToolTipText(mcol);

				if (tip == null)
					return null;
				return "<html>" + tip + "</html>";
			}
		};

		// Track where we are in the TableHeader, this is used by the Popup menus
		// to decide what column of the TableHeader we are currently located on.
		tabHeader.addMouseMotionListener(new MouseMotionListener()
		{
			@Override
			public void mouseMoved(MouseEvent e)
			{
				_lastTableHeaderColumn = getColumnModel().getColumnIndexAtX(e.getX());
				if (_lastTableHeaderColumn >= 0)
					_lastTableHeaderColumn = convertColumnIndexToModel(_lastTableHeaderColumn);
			}
			@Override
			public void mouseDragged(MouseEvent e) {/*ignore*/}
		});

		return tabHeader;
	}

	protected void refreshTable(AlarmEntry alarmEntry)
	{
		_alarmSettingsTableModel.refreshTable(alarmEntry);
		packAll(); // set size so that all content in all cells are visible
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
		_logger.debug("createDataTablePopupMenu(): called.");

		JPopupMenu popup = new JPopupMenu();
//		JMenuItem show = new JMenuItem("Dummy entry, does nothing");
		JMenuItem toDefault = new JMenuItem("Set the Default value for the selected row");

//		popup.add(show);
		popup.add(toDefault);

//		show.addActionListener(new ActionListener()
//		{
//			@Override
//			public void actionPerformed(ActionEvent e)
//			{
////				doActionShow();
//			}
//		});

		toDefault.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				JTable table = AlarmSettingsTable.this;
				int vrow = getSelectedRow();
				if (vrow == -1)
					return;

				int vcol_value   = SwingUtils.findColumnView(table, "Value");
				int vcol_default = SwingUtils.findColumnView(table, "Default");
				
				String defaultVal = table.getValueAt(vrow, vcol_default) + "";
				table.setValueAt(defaultVal, vrow, vcol_value);
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

	/** Get the JMeny attached to the JTable header */
	public JPopupMenu getDataTableHeaderPopupMenu()
	{
		return _headerPopupMenu;
	}

	public JPopupMenu createDataTableHeaderPopupMenu()
	{
		_logger.debug("createDataTableHeaderPopupMenu(): called.");
		JPopupMenu popup = new JPopupMenu();
		JMenuItem mark   = new JMenuItem("Mark all rows for this column");
		JMenuItem unmark = new JMenuItem("UnMark all rows for this column");

		popup.add(mark);
		popup.add(unmark);

		mark.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				TableModel tm = getModel();
				int col = getLastTableHeaderColumn();
				if (tm.getColumnClass(col).equals(Boolean.class))
				{
					for (int r=0; r<tm.getRowCount(); r++)
					{
						if (tm.isCellEditable(r, col))
							tm.setValueAt(new Boolean(true), r, col);
					}
				}
			}
		});

		unmark.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				TableModel tm = getModel();
				int col = getLastTableHeaderColumn();
				if (tm.getColumnClass(col).equals(Boolean.class))
				{
					for (int r=0; r<tm.getRowCount(); r++)
					{
						if (tm.isCellEditable(r, col))
							tm.setValueAt(new Boolean(false), r, col);
					}
				}
			}
		});

		// add something like:
		// popup.preShow()... so we can enable/disable menu items when we are on specific columns
		//popup.add

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
}

/*---------------------------------------------------
** END: class CmAlarmSettings
**---------------------------------------------------
*/
