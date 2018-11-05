package com.asetune.alarm.ui.config;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.asetune.alarm.ui.config.AlarmTableModel.AlarmEntry;
import com.asetune.gui.swing.GTableFilter;

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
