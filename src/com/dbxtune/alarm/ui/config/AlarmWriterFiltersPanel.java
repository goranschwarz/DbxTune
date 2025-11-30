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

import java.awt.Color;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.graph.TrendGraphColors;
import com.dbxtune.gui.swing.GTableFilter;
import com.dbxtune.utils.Configuration;

import net.miginfocom.swing.MigLayout;

public class AlarmWriterFiltersPanel
extends JPanel
implements TableModelListener
{
	private static final long serialVersionUID = 1L;

	AlarmWriterFiltersTable      _alarmWriterFiltersTable;
	AlarmWriterFiltersTableModel _alarmWriterFiltersTableModel;
	
	private JLabel _warning_lbl = new JLabel();
	private JLabel _debug_lbl   = new JLabel();
	
	public AlarmWriterFiltersPanel()
	{
		super();
		
		Border border = BorderFactory.createTitledBorder("Available Filters for the Alarm Writer");
		setBorder(border);

		setLayout(new MigLayout("insets 0 0 0 0", "", ""));

		// Create the table
		_alarmWriterFiltersTableModel = new AlarmWriterFiltersTableModel();
		_alarmWriterFiltersTable      = new AlarmWriterFiltersTable(_alarmWriterFiltersTableModel);
		_alarmWriterFiltersTableModel.addTableModelListener(this); // call this.tableChanged(TableModelEvent) when the table changed

		GTableFilter filter = new GTableFilter(_alarmWriterFiltersTable, GTableFilter.ROW_COUNT_LAYOUT_LEFT, true);
		filter.setText("");

		JScrollPane scroll = new JScrollPane(_alarmWriterFiltersTable);
		add(filter,       "pushx, growx, gapleft 10, gapright 10, wrap");
		add(_warning_lbl, "split, pushx, growx, hidemode 2");
		add(_debug_lbl,   "hidemode 2, wrap");
		add(scroll,       "push, grow, height 100%, wrap");

		_warning_lbl.setVisible(false);
		_debug_lbl  .setVisible(false);
		
		_alarmWriterFiltersTable.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				if (e.getValueIsAdjusting())
					return;

				checkContent();
			}
		});
	}

//	public void setWriterFilters(List<AlarmWriterFiltersEntry> filters)
//	{
//		_alarmWriterFiltersTable.refreshTable(filters);
//		checkContent();
//	}
	public void setWriterFilters(List<CmSettingsHelper> filters, String writerClassName)
	{
		_alarmWriterFiltersTable.refreshTable(filters, writerClassName);
		checkContent();
	}
//	public void setWriterName(String className)
//	{
//		_alarmWriterFiltersTable.refreshTable(className);
//		checkContent();
//	}

	@Override
	public void tableChanged(TableModelEvent e)
	{
		checkContent();
		firePropertyChange("tableChanged", "alarmWriterFiltersTable", e.getSource());
	}

	public void checkContent()
	{
		_debug_lbl.setText("ChangedRowCount=" + _alarmWriterFiltersTableModel.getModifiedRowCount());
		
		_warning_lbl.setText("");
		_warning_lbl.setVisible(false);
		_warning_lbl.setForeground(UIManager.getColor("Label.foreground"));

		if (_alarmWriterFiltersTableModel.getRowCount() == 0)
		{
			_warning_lbl.setText("No filters was available for this writer");
			_warning_lbl.setVisible(true);
			_warning_lbl.setForeground(Color.BLUE);
			return;
		}

		if (_alarmWriterFiltersTableModel.checkForMandatoryData())
		{
			_warning_lbl.setText("Mandatory values need to be filled in");
			_warning_lbl.setVisible(true);
			_warning_lbl.setForeground(Color.RED);
			return;
		}

		if (_alarmWriterFiltersTableModel.checkForProbableData())
		{
			_warning_lbl.setText("Optional/Template values need to be replaced with real values");
			_warning_lbl.setVisible(true);
			_warning_lbl.setForeground(Color.RED);
			return;
		}

		if (_alarmWriterFiltersTableModel.getUsedCount() == 0)
		{
			_warning_lbl.setText("<html>You need to <i>select</i> what filters you want to use. Otherwise all filters will use defaults. Click the <i>Use</i> cell<html>");
			_warning_lbl.setVisible(true);
			_warning_lbl.setForeground(Color.ORANGE);
			return;
		}

		if (_alarmWriterFiltersTable.getSelectedRow() != -1)
		{
			int selectedRow = _alarmWriterFiltersTable.getSelectedRow();
			int mrow = _alarmWriterFiltersTable.convertRowIndexToModel(selectedRow);
			String val = _alarmWriterFiltersTableModel.getValueAt(mrow, AlarmWriterFiltersTableModel.TAB_POS_PROPERTY_KEY) + "";
			if (val.toLowerCase().indexOf("template") != -1)
			{
				_warning_lbl.setText("<html>TIP: The selected row is a <b>template</b>. Right click on the row and choose <b>Template Edit</b>... There you can see the translated content.<html>");
				_warning_lbl.setVisible(true);
				_warning_lbl.setForeground(TrendGraphColors.VERY_DARK_GREEN);
			}
		}
	}

	/**
	 * Check if we have changed anything
	 * @return
	 */
	public boolean isDirty()
	{
		return _alarmWriterFiltersTable.isDirty();
//		return false;
	}

	/**
	 * return true if this panel has any configurable data
	 * @return
	 */
	public boolean hasData()
	{
		return _alarmWriterFiltersTable.getModel().getRowCount() > 0;
	}
	public boolean hasCheckedRows()
	{
		return _alarmWriterFiltersTableModel.getSelectedRowCount() > 0;
	}

	/**
	 * Save filters
	 */
	public void save()
	{
		System.out.println(this.getClass().getSimpleName() + ": SAVE - not yet implemented");
	}

	public Configuration getConfig(String className)
	{
		// TODO Auto-generated method stub
		return null;
	}
}
