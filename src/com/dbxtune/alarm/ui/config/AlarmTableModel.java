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

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.table.AbstractTableModel;

import com.dbxtune.CounterController;
import com.dbxtune.alarm.IUserDefinedAlarmInterrogator;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CountersModel;

/*---------------------------------------------------
** BEGIN: class LocalTableModel
**---------------------------------------------------
*/
/** AlarmTableModel */
public class AlarmTableModel extends AbstractTableModel
{
	private static final long serialVersionUID = 1L;

	protected static final String[] TAB_HEADER = {"Icon", "Tab Name", "CM Name", "Group Name", 
//            "Enabled", "Has System", "Use System", "Has UserDefined", "Use UserDefined"};
            "Alarm(s) is Enabled", "Has System", "System is Enabled", "Has UserDefined", "UserDefined is Enabled"};
	protected static final int TAB_POS_ICON             = 0;
	protected static final int TAB_POS_TAB_NAME         = 1;
	protected static final int TAB_POS_CM_NAME          = 2;
	protected static final int TAB_POS_GROUP_NAME       = 3;

	protected static final int TAB_POS_ENABLED          = 4;
	protected static final int TAB_POS_HAS_SYSTEM       = 5;
	protected static final int TAB_POS_USE_SYSTEM       = 6;
	protected static final int TAB_POS_HAS_UD           = 7;
	protected static final int TAB_POS_USE_UD           = 8;

	public String getToolTipText(int col)
	{
		switch(col)
		{
		case TAB_POS_ICON:          return null;
		case TAB_POS_TAB_NAME:      return "Name of Tab/Collector.";
		case TAB_POS_CM_NAME:       return "Internal Name of Tab/Collector. This is also what is used in most configuration files.";
		case TAB_POS_GROUP_NAME:    return "What Group does this performance counter belong to";

		case TAB_POS_ENABLED   :    return "Is Alarms Enabled or Disabled for this CM. (both System and User Defined Alarms)";
		case TAB_POS_HAS_SYSTEM:    return "If there are any Alarms implemeted by the system code.";
		case TAB_POS_USE_SYSTEM:    return "If you should use/execute the built in alarm(s) for this CM.";
		case TAB_POS_HAS_UD    :    return "If there are any User Defined Alarms code that you have provided.";
		case TAB_POS_USE_UD    :    return "If you should use/execute the User Defined alarm(s) for this CM.";
		}
		return null;
	}


	AlarmTableModel()
	{
		super();
	}

	private List<AlarmEntry> _entries = new ArrayList<>();
	
	protected static class AlarmEntry
	{
		boolean _modified = false;

		Icon    _icon;
		String  _tabName;
		String  _cmName;
		String  _groupName;
		boolean _isEnabled;
		boolean _hasSystem;
		boolean _isSystemEnabled;
		boolean _hasUserDefined;
		boolean _isUserDefinedEnabled;
		
//		List<AlarmSettingsEntry> _settings = new ArrayList<>();
		List<CmSettingsHelper> _settings = new ArrayList<>();
		IUserDefinedAlarmInterrogator _alarmInterrogator = null;
	}

//	protected class AlarmSettingsEntry
//	{
//		boolean _modified = false;
//		boolean _enabled;
//
//		String  _name;
//		String  _propKey;
//		String  _datatype;
//		String  _value;
//		String  _default;
//		String  _description;
//	}
	
	@Override
	public int getRowCount()
	{
		return _entries.size();
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

	@Override
	public Object getValueAt(int row, int col)
	{
		AlarmEntry e = _entries.get(row);
		switch (col)
		{
		case TAB_POS_ICON:       return e._icon;
		case TAB_POS_TAB_NAME:   return e._tabName;
		case TAB_POS_CM_NAME:    return e._cmName;
		case TAB_POS_GROUP_NAME: return e._groupName;
		
		case TAB_POS_ENABLED:    return e._isEnabled;
		case TAB_POS_HAS_SYSTEM: return e._hasSystem;
		case TAB_POS_USE_SYSTEM: return e._isSystemEnabled;
		case TAB_POS_HAS_UD:     return e._hasUserDefined;
		case TAB_POS_USE_UD:     return e._isUserDefinedEnabled;
		}
		
		return null;
	}

	@Override
	public void setValueAt(Object newVal, int row, int col)
	{
		if ( ! isCellEditable(row, col) )
			return;

		AlarmEntry e = _entries.get(row);
		Object oldVal = getValueAt(row, col);

		// has the value changed: mark it as modified
		if (oldVal != null )
			if ( ! oldVal.equals(newVal) )
				e._modified = true;
		if (newVal != null )
			if ( ! newVal.equals(oldVal) )
				e._modified = true;

		// Set the value
		if (col == TAB_POS_ENABLED)    e._isEnabled            = newVal.toString().equalsIgnoreCase("true");
		if (col == TAB_POS_USE_SYSTEM) e._isSystemEnabled      = newVal.toString().equalsIgnoreCase("true");
		if (col == TAB_POS_USE_UD)     e._isUserDefinedEnabled = newVal.toString().equalsIgnoreCase("true");

		fireTableCellUpdated(row, col);
	}

	@Override
	public Class<?> getColumnClass(int col)
	{
		if (col == TAB_POS_ICON)             return Icon.class;
		if (col == TAB_POS_ENABLED)          return Boolean.class;
		if (col == TAB_POS_HAS_SYSTEM)       return Boolean.class;
		if (col == TAB_POS_USE_SYSTEM)       return Boolean.class;
		if (col == TAB_POS_HAS_UD)           return Boolean.class;
		if (col == TAB_POS_USE_UD)           return Boolean.class;

		return Object.class;
	}

	@Override
	public boolean isCellEditable(int row, int col)
	{
		if (col == TAB_POS_ENABLED)          return true;
		if (col == TAB_POS_USE_SYSTEM)       return true;
		if (col == TAB_POS_USE_UD)           return true;

		return false;
	}



	/** Populate information in the table */
	protected void refreshTable()
	{
		_entries.clear();

		List<CountersModel> cmList = CounterController.getInstance().getCmList();
		if (cmList == null)
			return;
		if (cmList.size() == 0)
			return;

//		Configuration config = Configuration.getCombinedConfiguration();

		for (CountersModel cm : cmList)
		{
			AlarmEntry ae = new AlarmEntry();

			ae._icon                 = cm.getTabPanel() == null ? null : cm.getTabPanel().getIcon();
			ae._tabName              = cm.getDisplayName();
			ae._cmName               = cm.getName();
			ae._groupName            = cm.getGroupName();
			ae._isEnabled            = cm.isAlarmEnabled();
			ae._hasSystem            = cm.hasSystemAlarms();
			ae._isSystemEnabled      = cm.isSystemAlarmsEnabled();
			ae._hasUserDefined       = cm.hasUserDefinedAlarmInterrogator();
			ae._isUserDefinedEnabled = cm.isUserDefinedAlarmsEnabled();
			ae._alarmInterrogator    = cm.getUserDefinedAlarmInterrogator();

			ae._settings = cm.getLocalAlarmSettings();
			for (CmSettingsHelper sh : ae._settings)
			{
				sh.setSelected( cm.isSystemAlarmsForColumnEnabled(sh.getName()) );
			}
			
//			// Add SETTINGS for this WRITERS
//			for (CmSettingsHelper sh : cm.getLocalAlarmSettings())
//			{
//				AlarmSettingsEntry ase = new AlarmSettingsEntry();
//
////				// enable key: CMName.alarm.system.enabled.COLNAME
//////				String key = ae._cmName+".alarm.system.enabled."+sh.getName();
////				String key = CountersModel.replaceCmAndColName(ae._cmName, CountersModel.PROPKEY_ALARM_isSystemAlarmsForColumnEnabled, sh.getName());
////				boolean enabled = config.getBooleanProperty(key, true);
//				
////				ase._enabled     = enabled;
//				ase._enabled     = cm.isSystemAlarmsForColumnEnabled(sh.getName());
//				ase._name        = sh.getName();
//				ase._propKey     = sh.getPropName();
//				ase._datatype    = sh.getDataTypeString();
//				ase._value       = sh.getStringValue();
//				ase._default     = sh.getDefaultValue();
//				ase._description = sh.getDescription();
//
//				ae._settings.add(ase);
//			}

			_entries.add(ae);
		}

//		org.apache.commons.lang3.SerializationUtils.clone(_entries);
//		_entriesAtStart = (List<AlarmEntry>) deepClone(_entries);
//System.out.println("--DONE--deepClone(): "+this.getClass().getSimpleName());
		
//		Cloner cloner = new Cloner();
//System.out.println("----Cloner.start");
//		_entriesAtStart = cloner.deepClone(_entries);
//System.out.println("----Cloner.end");
// the above uses: cloning-1.9.6.jar && objenesis-2.6.jar 
// remove/add the above from the dbxtune.bat, asetune.sh, iqtune.sh

		
		fireTableDataChanged();
	}

//	private List<AlarmEntry> _entriesAtStart = new ArrayList<>();
	
//	private void deepCopyModel()
//	{
//		_entriesAtStart = new ArrayList<>();
//		for (AlarmEntry ae : _entries)
//		{
//			AlarmEntry n = new AlarmEntry();
//			n._alarmInterrogator = ae._alarmInterrogator;
//		}
//	}
	
	
//	/**
//	 * This method makes a "deep clone" of any Java object it is given. but they have to be serializable
//	 */
//	public static Object deepClone(Object object)
//	{
//		try
//		{
//			ByteArrayOutputStream baos = new ByteArrayOutputStream();
//			ObjectOutputStream oos = new ObjectOutputStream(baos);
//			oos.writeObject(object);
//			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
//			ObjectInputStream ois = new ObjectInputStream(bais);
//			return ois.readObject();
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//			return null;
//		}
//	}

	public AlarmEntry getAlarmForRow(int row)
	{
		AlarmEntry e = _entries.get(row);
		return e;
	}

//	public List<AlarmSettingsEntry> getSettingsForRow(int row)
//	{
//		AlarmEntry e = _entries.get(row);
//		return e._settings;
//	}
	public List<CmSettingsHelper> getSettingsForRow(int row)
	{
		AlarmEntry e = _entries.get(row);
		return e._settings;
	}

	/** 
	 * Return first problem we discover in the model, note: They may contain HTML tags 
	 * @return null if no problems otherwise a text 
	 */
	public String getProblem()
	{
		return null;
	}
}
/*---------------------------------------------------
** END: class LocalTableModel
**---------------------------------------------------
*/
