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

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.dbxtune.alarm.ui.config.AlarmTableModel.AlarmEntry;
import com.dbxtune.gui.swing.GTableFilter;

import net.miginfocom.swing.MigLayout;

public class AlarmDetailSystemPanel
extends JPanel
implements TableModelListener
{
	private static final long serialVersionUID = 1L;

	AlarmSettingsTable      _alarmSettingsTable;
	AlarmSettingsTableModel _alarmSettingsTableModel;
	
	public AlarmDetailSystemPanel()
	{
		super();
		
		Border border = BorderFactory.createTitledBorder("Details for System Alarms");
		setBorder(border);

		setLayout(new MigLayout("insets 0 0 0 0", "", ""));

		// Create the table
		_alarmSettingsTableModel = new AlarmSettingsTableModel();
		_alarmSettingsTable      = new AlarmSettingsTable(_alarmSettingsTableModel);
		_alarmSettingsTableModel.addTableModelListener(this); // call this.tableChanged(TableModelEvent) when the table changed

		GTableFilter filter = new GTableFilter(_alarmSettingsTable, GTableFilter.ROW_COUNT_LAYOUT_LEFT, true);
		filter.setText("");

		JScrollPane scroll = new JScrollPane(_alarmSettingsTable);
		add(filter, "pushx, growx, gapleft 10, gapright 10, wrap");
		add(scroll, "push, grow, height 100%, wrap");

//		panel.add(new JLabel("Dummy System Alarms"),          "wrap");
	}

	@Override
	public void tableChanged(TableModelEvent e)
	{
		firePropertyChange("tableChanged", "alarmSettingsTable", e.getSource());
	}

	public void setAlarmEntry(AlarmEntry alarmEntry)
	{
		_alarmSettingsTable.refreshTable(alarmEntry);
	}
//	public void setCm(CountersModel cm)
//	{
//		if (cm != null)
//		{
//			_alarmSettingsTable.refreshTable(cm.getName());
//		}
//		else
//		{
//		}
//	}

	/**
	 * Check if we have changed anything
	 * @return
	 */
	public boolean isDirty()
	{
		return _alarmSettingsTableModel.isDirty();
//		return false;
	}

	/**
	 * return true if this panel has any configurable data
	 * @return
	 */
	public boolean hasData()
	{
		return _alarmSettingsTable.getModel().getRowCount() > 0;
	}

	/**
	 * Save settings
	 */
	public void save()
	{
		System.out.println(this.getClass().getSimpleName() + ": SAVE - not yet implemented");
	}
}
