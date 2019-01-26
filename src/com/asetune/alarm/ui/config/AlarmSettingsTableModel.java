/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.alarm.ui.config;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.asetune.alarm.ui.config.AlarmTableModel.AlarmEntry;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSettingsHelper.ValidationException;
import com.asetune.utils.SwingUtils;

/*---------------------------------------------------
** BEGIN: class LocalTableModel
**---------------------------------------------------
*/
/** LocalTableModel */
public class AlarmSettingsTableModel extends AbstractTableModel
{
	private static final long serialVersionUID = 1L;

	protected static final String[] TAB_HEADER = {"Enabled", "Name", "Property", "Datatype", "Value", "isDefault", "Default", "Description"};
	protected static final int TAB_POS_ENABLED          = 0;
	protected static final int TAB_POS_NAME             = 1;
	protected static final int TAB_POS_PROPERTY_KEY     = 2;
	protected static final int TAB_POS_DATATYPE         = 3;
	protected static final int TAB_POS_VALUE            = 4;
	protected static final int TAB_POS_IS_DEFAULT       = 5;
	protected static final int TAB_POS_DEFAULT          = 6;
	protected static final int TAB_POS_DESCRIPTION      = 7;

	public String getToolTipText(int col)
	{
		switch(col)
		{
		case TAB_POS_ENABLED:       return "If This alarm is enabled or not";
		case TAB_POS_NAME:          return "Slogan or Name of this setting";
		case TAB_POS_PROPERTY_KEY:  return "Property key used in the Configuration";
		case TAB_POS_DATATYPE:      return "Datatype that the value has";
		case TAB_POS_VALUE:         return "The configgured value or threshold";
		case TAB_POS_IS_DEFAULT:    return "If the 'Value' matches the 'Default'";
		case TAB_POS_DEFAULT:       return "The default value or threshold";
		case TAB_POS_DESCRIPTION:   return "Description of this setting";
		}
		return null;
	}

	AlarmSettingsTableModel()
	{
		super();
	}


	@SuppressWarnings("unused")
	private AlarmEntry _alarmEntry = null;
	
//	private List<AlarmSettingsEntry> _settings = new ArrayList<>();
	private List<CmSettingsHelper> _settings = new ArrayList<>();
	
	/** Populate information in the table */
//	public void refreshTable(List<AlarmSettingsEntry> settings)
	public void refreshTable(AlarmEntry alarmEntry)
	{
		_alarmEntry = alarmEntry;

		_settings = alarmEntry._settings;
		if (_settings == null)
			_settings = new ArrayList<>();

		fireTableDataChanged();
	}

	public CmSettingsHelper getSettingForRow(int row)
	{
		return _settings.get(row);
	}

	@Override
	public int getRowCount()
	{
		return _settings.size();
	}

	@Override
	public int getColumnCount()
	{
		return TAB_HEADER.length;
	}

	@Override
	public String getColumnName(int col)
	{
		return TAB_HEADER[col];
	}

//	@Override
//	public Object getValueAt(int row, int col)
//	{
//		AlarmSettingsEntry e = _settings.get(row);
//		switch (col)
//		{
//		case TAB_POS_ENABLED:      return e._enabled;
//		case TAB_POS_NAME:         return e._name;
//		case TAB_POS_PROPERTY_KEY: return e._propKey;
//		case TAB_POS_DATATYPE:     return e._datatype;
//		case TAB_POS_VALUE:        return e._value;
//		case TAB_POS_DEFAULT:      return e._default;
//		case TAB_POS_DESCRIPTION:  return e._description;
//		}
//		
//		return null;
//	}
	@Override
	public Object getValueAt(int row, int col)
	{
		CmSettingsHelper e = _settings.get(row);
		switch (col)
		{
		case TAB_POS_ENABLED:      return e.isSelected();
		case TAB_POS_NAME:         return e.getName();
		case TAB_POS_PROPERTY_KEY: return e.getPropName();
		case TAB_POS_DATATYPE:     return e.getDataTypeString();
		case TAB_POS_VALUE:        return e.getStringValue();
		case TAB_POS_IS_DEFAULT:   return e.isDefaultValue();
		case TAB_POS_DEFAULT:      return e.getDefaultValue();
		case TAB_POS_DESCRIPTION:  return e.getDescription();
		}
		
		return null;
	}

//	@Override
//	public void setValueAt(Object newVal, int row, int col)
//	{
//		if ( ! isCellEditable(row, col) )
//			return;
//
//		AlarmSettingsEntry e = _settings.get(row);
//		Object oldVal = getValueAt(row, col);
//
//		// has the value changed: mark it as modified
//		if (oldVal != null )
//			if ( ! oldVal.equals(newVal) )
//				e._modified = true;
//		if (newVal != null )
//			if ( ! newVal.equals(oldVal) )
//				e._modified = true;
//
//		// Set the value
//		if (col == TAB_POS_ENABLED) e._enabled = newVal.toString().equalsIgnoreCase("true");
//		if (col == TAB_POS_VALUE)   e._value   = newVal.toString();
//
//		fireTableCellUpdated(row, col);
//	}
	@Override
	public void setValueAt(Object newVal, int row, int col)
	{
		if ( ! isCellEditable(row, col) )
			return;

		CmSettingsHelper sh = _settings.get(row);
		Object oldVal = getValueAt(row, col);

		// if it's a mandatory field, you can NOT "uncheck" it
		if (sh.isMandatory() && col == TAB_POS_ENABLED)
		{ 
			SwingUtils.showTimedBalloonTip(this, row, col, 3*1000, true, "Sorry this is a mandatory field, you can not un-check it");
			return; 
		};

		// Set the value
		try
		{
			if (col == TAB_POS_ENABLED) sh.setSelected   (newVal.toString().equalsIgnoreCase("true"));
			if (col == TAB_POS_VALUE)   sh.setStringValue(newVal.toString());
		}
		catch (ValidationException ex)
		{
//			SwingUtils.showTimedBalloonTip(this, row, col, 10*1000, true, "Validation error: "+ex.getMessage());
			SwingUtils.showTimedBalloonTip(this, row, col, 10*1000, true, "<html>Validation error: <b>Value will be discarded</b><br><pre>"+ex.getMessage()+"</pre></html>");
			return;
		}

		// has the value changed: mark it as modified
		if (oldVal != null )
			if ( ! oldVal.equals(newVal) )
				sh.setModified(true);
		if (newVal != null )
			if ( ! newVal.equals(oldVal) )
				sh.setModified(true);

		// If we change the value, simply MARK it as selected
		if (col == TAB_POS_VALUE) 
		{
			sh.setSelected(true);
			fireTableCellUpdated(row, TAB_POS_ENABLED);
			fireTableCellUpdated(row, TAB_POS_IS_DEFAULT);
		}
		
		fireTableCellUpdated(row, col);
	}

	@Override
	public Class<?> getColumnClass(int col)
	{
		if (col == TAB_POS_ENABLED)          return Boolean.class;
		if (col == TAB_POS_IS_DEFAULT)       return Boolean.class;

		return Object.class;
	}

	@Override
	public boolean isCellEditable(int row, int col)
	{
		if (col == TAB_POS_ENABLED)        return true;
		if (col == TAB_POS_VALUE)          return true;

		// Not really editable: just made it editable so it's isier to copy from... get the setValueAt() will NOT be setting the value
		if (col == TAB_POS_DEFAULT)        return true;

		return false;
	}

//	/** Check if this model has changed */
//	public boolean isDirty()
//	{
//		for (AlarmSettingsEntry ase : _settings)
//		{
//			if (ase._modified)
//				return true;
//		}
//
//		// Finally: no changes 
//		return false;
//	}
	/** Check if this model has changed */
	public boolean isDirty()
	{
		for (CmSettingsHelper ase : _settings)
		{
			if (ase.isModified())
				return true;
		}

		// Finally: no changes 
		return false;
	}
}

/*---------------------------------------------------
** END: class LocalCmAlarmSettingsModel
**---------------------------------------------------
*/
