package com.asetune.alarm.ui.config;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSettingsHelper.ValidationException;
import com.asetune.utils.SwingUtils;

/*---------------------------------------------------
** BEGIN: class LocalTableModel
**---------------------------------------------------
*/
/** LocalTableModel */
public class AlarmWriterFiltersTableModel extends AbstractTableModel
{
	private static final long serialVersionUID = 1L;

	protected static final String[] TAB_HEADER = {"Use", "Name", "Property", "Datatype", "Value", "isDefault", "Default", "Description"};
	protected static final int TAB_POS_USE              = 0;
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
		case TAB_POS_USE:          return "Should this Setting be used";
		case TAB_POS_NAME:         return "Name of the Setting.";
		case TAB_POS_PROPERTY_KEY: return "property key for this setting.";
		case TAB_POS_DATATYPE:     return "Datatype for the setting";
		case TAB_POS_VALUE:        return "The value you want this settings to be";
		case TAB_POS_IS_DEFAULT:   return "If the 'Value' matches the 'Default'";
		case TAB_POS_DEFAULT:      return "The default value for this setting";
		case TAB_POS_DESCRIPTION:  return "Description";
		}
		return null;
	}

	AlarmWriterFiltersTableModel()
	{
		super();
	}

//	private List<AlarmWriterSettingsEntry> _settings = new ArrayList<>();
//	
//	/** Populate information in the table */
//	public void refreshTable(List<AlarmWriterSettingsEntry> settings)
//	{
//		_settings = settings;
//		if (_settings == null)
//			_settings = new ArrayList<>();
//
//		fireTableDataChanged();
//	}

	private List<CmSettingsHelper> _settings = new ArrayList<>();
	
	/** Populate information in the table */
	public void refreshTable(List<CmSettingsHelper> settings)
	{
		_settings = settings;
		if (_settings == null)
			_settings = new ArrayList<>();

		fireTableDataChanged();
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
//		AlarmWriterSettingsEntry e = _settings.get(row);
//		switch (col)
//		{
//		case TAB_POS_USE:          return e._selected;
//		case TAB_POS_NAME:         return e._name;
//		case TAB_POS_PROPERTY_KEY: return e._propKey;
//		case TAB_POS_DATATYPE:     return e._datatype;
//		case TAB_POS_VALUE:        return e._value;
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
		case TAB_POS_USE:          return e.isSelected();
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
//		AlarmWriterSettingsEntry e = _settings.get(row);
//		Object oldVal = getValueAt(row, col);
//
//		// has the value changed: mark it as modified
//		if ( ! oldVal.equals(newVal) )
//			e._modified = true;
//
//		// Set the value
//		if (col == TAB_POS_USE)   e._selected = newVal.toString().equalsIgnoreCase("true");
//		if (col == TAB_POS_VALUE) e._value    = newVal.toString();
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
		if (sh.isMandatory() && col == TAB_POS_USE)
		{ 
			SwingUtils.showTimedBalloonTip(this, row, col, 3*1000, true, "Sorry this is a mandatory field, you can not un-check it");
			return; 
		};

		// Set the value
		try
		{
			if (col == TAB_POS_USE)   sh.setSelected   (newVal.toString().equalsIgnoreCase("true"));
			if (col == TAB_POS_VALUE) sh.setStringValue(newVal.toString());
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
			fireTableCellUpdated(row, TAB_POS_USE);
			fireTableCellUpdated(row, TAB_POS_IS_DEFAULT);
		}

		fireTableCellUpdated(row, col);
	}

	@Override
	public Class<?> getColumnClass(int col)
	{
		if (col == TAB_POS_USE)        return Boolean.class;
		if (col == TAB_POS_IS_DEFAULT) return Boolean.class;

		return Object.class;
	}

	@Override
	public boolean isCellEditable(int row, int col)
	{
		if (col == TAB_POS_USE)   return true;
		if (col == TAB_POS_VALUE) return true;
		
		// Not really editable: just made it editable so it's isier to copy from... get the setValueAt() will NOT be setting the value
		if (col == TAB_POS_DEFAULT)        return true;

		return false;
	}



	
//	public AlarmWriterSettingsEntry getSettingForRow(int row)
//	{
//		return _settings.get(row);
//	}
	public CmSettingsHelper getSettingForRow(int row)
	{
		return _settings.get(row);
	}

//	/** Check if this model has changed */
//	public boolean isDirty()
//	{
//		for (AlarmWriterSettingsEntry awse : _settings)
//		{
//			if (awse._modified)
//				return true;
//		}
//
//		// Finally: no changes 
//		return false;
//	}
	/** Check if this model has changed */
	public boolean isDirty()
	{
		for (CmSettingsHelper awse : _settings)
		{
			if (awse.isModified())
				return true;
		}

		// Finally: no changes 
		return false;
	}

//	public boolean checkForMandatoryData()
//	{
//		for (int r=0; r<getRowCount(); r++)
//		{
////			String  val      = "" +      getValueAt(r, TAB_POS_VALUE);
////			boolean selected = (boolean) getValueAt(r, TAB_POS_USE);
////			if (selected && val.indexOf(CmSettingsHelper.MANDATORY) != -1)
////				return true;
//
//			String  val = "" + getValueAt(r, TAB_POS_VALUE);
//			if (val.indexOf(CmSettingsHelper.MANDATORY) != -1)
//				return true;
//		}
//		return false;
//	}
	public boolean checkForMandatoryData()
	{
		for (int r=0; r<getRowCount(); r++)
		{
			CmSettingsHelper sh = getSettingForRow(r);
			if (sh.isMandatory() && ! sh.hasValidValue())
				return true;
		}
		return false;
	}

//	public boolean checkForOptionalData()
//	{
//		for (int r=0; r<getRowCount(); r++)
//		{
//			String  val      = "" +      getValueAt(r, TAB_POS_VALUE);
//			boolean selected = (boolean) getValueAt(r, TAB_POS_USE);
//			if (selected && val.indexOf(CmSettingsHelper.OPTIONAL) != -1)
//				return true;
//			if (selected && val.indexOf(CmSettingsHelper.TEMPLATE) != -1)
//				return true;
//		}
//		return false;
//	}
	public boolean checkForProbableData()
	{
		for (int r=0; r<getRowCount(); r++)
		{
			CmSettingsHelper sh = getSettingForRow(r);
			if (sh.isProbable() && ! sh.hasValidValue())
				return true;
		}
		return false;
	}

//	public int getUsedCount()
//	{
//		int count = 0;
//		for (int r=0; r<getRowCount(); r++)
//		{
//			boolean val = (boolean) getValueAt(r, TAB_POS_USE);
//			if (val)
//				count++;
//		}
//		return count;
//	}
	public int getUsedCount()
	{
		int count = 0;
		for (int r=0; r<getRowCount(); r++)
		{
			CmSettingsHelper sh = getSettingForRow(r);
			if (sh.isSelected())
				count++;
		}
		return count;
	}

//	public int getModifiedRowCount()
//	{
//		int count = 0;
//		for (AlarmWriterSettingsEntry s : _settings)
//		{
//			if (s._modified)
//				count++;
//		}
//		return count;
//	}
	public int getModifiedRowCount()
	{
		int count = 0;
		for (CmSettingsHelper s : _settings)
		{
			if (s.isModified())
				count++;
		}
		return count;
	}

	public int getSelectedRowCount()
	{
		int count = 0;
		for (CmSettingsHelper s : _settings)
		{
			if (s.isSelected())
				count++;
		}
		return count;
	}
}
/*---------------------------------------------------
** END: class LocalCmAlarmSettingsModel
**---------------------------------------------------
*/
