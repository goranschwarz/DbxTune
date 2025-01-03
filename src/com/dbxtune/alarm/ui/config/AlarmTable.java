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
package com.dbxtune.alarm.ui.config;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.lang.invoke.MethodHandles;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTableHeader;

import com.dbxtune.alarm.ui.config.AlarmTableModel.AlarmEntry;

/** Extend the JXTable */
public class AlarmTable extends JXTable
{
	private static final long serialVersionUID = 0L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	//	protected int           _lastTableHeaderPointX = -1;
	protected int           _lastTableHeaderColumn = -1;
	private   JPopupMenu    _popupMenu             = null;
	private   JPopupMenu    _headerPopupMenu       = null;

	AlarmDetailsPanel _alarmDetailsPanel;
	AlarmTableModel   _alarmTableModel;
	
	public AlarmTable(AlarmTableModel alarmTableModel, AlarmDetailsPanel alarmDetailsPanel)
	{
		super();
		
		_alarmTableModel   = alarmTableModel;
		_alarmDetailsPanel = alarmDetailsPanel;

		setModel(_alarmTableModel);

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

		// Populate the table
//		refreshTable();
		
		// Select first row
		if (getRowCount() > 0)
			getSelectionModel().setSelectionInterval(0, 0);
		
		getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				AlarmTable table = AlarmTable.this;
				if (e.getValueIsAdjusting())
					return;
				
				// In the detailes section: Load the selected row 
				int vrow = table.getSelectedRow();
				if (vrow == -1)
					return;
				int mrow = table.convertRowIndexToModel(vrow);

				AlarmEntry alarmEntry = _alarmTableModel.getAlarmForRow(mrow);

//				CountersModel cm = CounterController.getInstance().getCmByName(cmName);
//				_alarmDetailsPanel.setCm(cm);
				_alarmDetailsPanel.setAlarmEntry(alarmEntry);
			}
		});
	}

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

				tip = _alarmTableModel.getToolTipText(mcol);

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

	/** CELL tool tip */
	@Override
	public String getToolTipText(MouseEvent e)
	{
		String tip = null;
		Point p = e.getPoint();
		int vrow = rowAtPoint(p);
		int vcol = columnAtPoint(p);
		if (vrow == -1 || vcol == -1)
			return null;
		
		// Translate View row to Model row
		int mrow = super.convertRowIndexToModel(    vrow );
		int mcol = super.convertColumnIndexToModel( vcol );

		// is Boolean and isEditable then show the below tooltip
		TableModel tm = getModel();
		if (tm.getColumnClass(mcol).equals(Boolean.class))
		{
			if (tm.isCellEditable(mrow, mcol))
				tip = "Right click on the header column to mark or unmark all rows.";
			else
				tip = "Sorry you can't change this value. (it's a <i>static</i> field)";
		}
		else
		{
			int description_pos = -1;
			for (int c=0; c<tm.getColumnCount(); c++)
			{
				if (tm.getColumnName(c).equalsIgnoreCase("Description"))
					description_pos = c;
			}
			if (description_pos != -1)
				return tm.getValueAt(mrow, description_pos) + "";
		}

		if (tip == null)
			return null;
		return "<html>" + tip + "</html>";
	}

	/** Populate information in the table */
	protected void refreshTable()
	{
		_alarmTableModel.refreshTable();
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
		JMenuItem show = new JMenuItem("Dummy entry, does nothing");

		popup.add(show);

		show.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
//				doActionShow();
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
							tm.setValueAt(Boolean.valueOf(true), r, col);
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
							tm.setValueAt(Boolean.valueOf(false), r, col);
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
** END: class LocalTable
**---------------------------------------------------
*/
