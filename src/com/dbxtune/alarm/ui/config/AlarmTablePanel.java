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
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.swing.GTableFilter;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

import net.miginfocom.swing.MigLayout;

public class AlarmTablePanel
extends JPanel 
implements TableModelListener
{
	private static final long serialVersionUID = 1L;

	private AlarmTable      _alarmTable;
	private AlarmTableModel _alarmTableModel;

	private AlarmDetailsPanel _alarmDetailsPanel;

	public AlarmTablePanel(AlarmDetailsPanel alarmDetailsPanel)
	{
		super();
		
		Border border = BorderFactory.createTitledBorder("Alarms Overview");
		setBorder(border);

		setLayout(new MigLayout("insets 0 0 0 0", "", ""));

		// Create the table
		_alarmDetailsPanel = alarmDetailsPanel;
		_alarmTableModel   = new AlarmTableModel();
		_alarmTable        = new AlarmTable(_alarmTableModel, _alarmDetailsPanel);
		_alarmTableModel.addTableModelListener(this); // call this.tableChanged(TableModelEvent) when the table changed

		GTableFilter filter = new GTableFilter(_alarmTable, GTableFilter.ROW_COUNT_LAYOUT_LEFT, true);
		filter.setText("where ["+AlarmTableModel.TAB_HEADER[AlarmTableModel.TAB_POS_HAS_SYSTEM]+"] = 'true' or ["+AlarmTableModel.TAB_HEADER[AlarmTableModel.TAB_POS_HAS_UD]+"] = 'true'");

		JScrollPane scroll = new JScrollPane(_alarmTable);
		add(filter, "pushx, growx, gapleft 10, gapright 10, wrap");
		add(scroll, "push, grow, height 100%, wrap");

		_alarmTable.refreshTable();

		// Select first row
		if (_alarmTable.getRowCount() > 0)
			_alarmTable.getSelectionModel().setSelectionInterval(0, 0);
	}

	@Override
	public void tableChanged(TableModelEvent e)
	{
		firePropertyChange("tableChanged", "alarmTable", e.getSource());
	}

	public void fireUserDefinedAlarmMayHaveChanged(String cmName)
	{
		int selectedRow = _alarmTable.getSelectedRow();
		_alarmTableModel.fireTableDataChanged();
		if (selectedRow != -1)
			_alarmTable.addRowSelectionInterval(selectedRow, selectedRow);
	}

//	public void fireUserDefinedAlarmMayHaveChanged(String cmName)
//	{
//		int cmName_pos = _alarmTable.convertColumnIndexToView(AlarmTableModel.TAB_POS_CM_NAME);
////		int useUd_pos  = _alarmTable.convertColumnIndexToView(AlarmTableModel.TAB_POS_USE_UD);
////		int hasUd_pos  = _alarmTable.convertColumnIndexToView(AlarmTableModel.TAB_POS_HAS_UD);
//
////		if (cmName_pos < 0 || useUd_pos < 0 || hasUd_pos < 0)
//		if (cmName_pos < 0)
//		{
//			_alarmTableModel.fireTableDataChanged();
//			return;
//		}
//
//		int rowToFire = -1;
//		for (int r=0; r<_alarmTable.getRowCount(); r++)
//		{
//			String tCmName = _alarmTable.getValueAt(r, cmName_pos)+"";
//			if (tCmName.equals(cmName))
//			{
//				rowToFire = r;
//				break;
//			}
//		}
//System.out.println("fireUserDefinedAlarmMayHaveChanged(cmName='"+cmName+"'): rowToFire="+rowToFire);
//		if (rowToFire != -1)
//			_alarmTableModel.fireTableRowsUpdated(rowToFire, rowToFire);
//		else
//			_alarmTableModel.fireTableDataChanged();
//			
////		_alarmTableModel.fireTableCellUpdated(rowToFire, useUd_pos);
////		_alarmTableModel.fireTableCellUpdated(rowToFire, hasUd_pos);
//	}

	public String getProblem()
	{
		return _alarmDetailsPanel.getProblem();
	}

	public boolean isDirty()
	{
		return _alarmDetailsPanel.isDirty();		
	}

	public Configuration getConfig()
	{
		Configuration conf = new Configuration();

		// Set config for WRITERS SETTINGS
		for (int r=0; r<_alarmTableModel.getRowCount(); r++)
		{
			AlarmEntry ae = _alarmTableModel.getAlarmForRow(r);

			// SYSTEM Settings
			if (ae._hasSystem)
			{
				// <CMNAME>.alarm.enabled
				// <CMNAME>.alarm.system.enabled
//				conf.setProperty(CountersModel.replaceCmName(ae._cmName, CountersModel.PROPKEY_ALARM_isAlarmsEnabled),       ae._isEnabled);
//				conf.setProperty(CountersModel.replaceCmName(ae._cmName, CountersModel.PROPKEY_ALARM_isSystemAlarmsEnabled), ae._isSystemEnabled);
				conf.setProperty(CountersModel.replaceCmName(ae._cmName, CountersModel.PROPKEY_ALARM_isAlarmsEnabled),       ae._isEnabled       == CountersModel.DEFAULT_ALARM_isAlarmsEnabled       ? Configuration.USE_DEFAULT_PREFIX + ae._isEnabled       : ae._isEnabled+"");
				conf.setProperty(CountersModel.replaceCmName(ae._cmName, CountersModel.PROPKEY_ALARM_isSystemAlarmsEnabled), ae._isSystemEnabled == CountersModel.DEFAULT_ALARM_isSystemAlarmsEnabled ? Configuration.USE_DEFAULT_PREFIX + ae._isSystemEnabled : ae._isSystemEnabled+"");

//				// Write for all system settings
//				for (AlarmSettingsEntry ase : ae._settings)
//				{
//					// <CMNAME>.alarm.system.enabled.<COLNAME>
//					conf.setProperty(CountersModel.replaceCmAndColName(ae._cmName, CountersModel.PROPKEY_ALARM_isSystemAlarmsForColumnEnabled, ase._name), ase._enabled);
//
//					// Various properties defined by the CounterModel
//					// probably looks like: <CMNAME>.alarm.system.if.<COLNAME>.gt
////					if (ase._enabled)
//						conf.setProperty(ase._propKey, ase._value);
//				}
				// Write for all system settings
				for (CmSettingsHelper ase : ae._settings)
				{
					// <CMNAME>.alarm.system.enabled.<COLNAME>
					// Only for names that do not contin ' ' spaces... FIXME: This is UGGLY, create a type/property which describes if we should write the '*.enable.*' property or not...
//					if ( ase.getName().indexOf(' ') == -1)
					if (ase.isAlarmSwitch())
						conf.setProperty(CountersModel.replaceCmAndColName(ae._cmName, CountersModel.PROPKEY_ALARM_isSystemAlarmsForColumnEnabled, ase.getName()), ase.isDefaultValue() ? Configuration.USE_DEFAULT_PREFIX + ase.isSelected() : ase.isSelected()+"");

					// Various properties defined by the CounterModel
					// probably looks like: <CMNAME>.alarm.system.if.<COLNAME>.gt
//					if (ase._enabled)
						conf.setProperty(ase.getPropName(), ase.isDefaultValue() ? Configuration.USE_DEFAULT_PREFIX + ase.getStringValue() : ase.getStringValue());
				}
			}

			// UserDefined Settings
			if (ae._hasUserDefined)
			{
				// <CMNAME>.alarm.enabled
				// <CMNAME>.alarm.userdefined.enabled
				// <CMNAME>.alarm.userdefined.source.filename
				conf.setProperty(CountersModel.replaceCmName(ae._cmName, CountersModel.PROPKEY_ALARM_isAlarmsEnabled),            ae._isEnabled            == CountersModel.DEFAULT_ALARM_isAlarmsEnabled            ? Configuration.USE_DEFAULT_PREFIX + ae._isEnabled            : ae._isEnabled+"");
				conf.setProperty(CountersModel.replaceCmName(ae._cmName, CountersModel.PROPKEY_ALARM_isUserdefinedAlarmsEnabled), ae._isUserDefinedEnabled == CountersModel.DEFAULT_ALARM_isUserdefinedAlarmsEnabled ? Configuration.USE_DEFAULT_PREFIX + ae._isUserDefinedEnabled : ae._isUserDefinedEnabled+"");
				//conf.setProperty(CountersModel.replaceCmName(ae._cmName, CountersModel.PROPKEY_ALARM_userdefinedSourceFilename),  ae._sourceFilename); // Maby in the future... if we want to specify the source file...
			}
		}
		
		return conf;
	}

	public void setSelectedCmName(String cmName)
	{
		if (StringUtil.isNullOrBlank(cmName))
			return;

		// Set config for WRITERS SETTINGS
		for (int mr=0; mr<_alarmTableModel.getRowCount(); mr++)
		{
			AlarmEntry ae = _alarmTableModel.getAlarmForRow(mr);
			if (cmName.equals(ae._cmName))
			{
				int vr = _alarmTable.convertRowIndexToView(mr);

				// Only position if it's visible
				if (vr != -1)
					_alarmTable.setRowSelectionInterval(vr, vr);

				break;
			}
		}
	}
}
