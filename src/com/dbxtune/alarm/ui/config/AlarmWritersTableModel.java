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
import java.util.Set;

import javax.swing.table.AbstractTableModel;

import org.reflections.Reflections;

import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.writers.IAlarmWriter;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.pcs.IPersistWriter;

/*---------------------------------------------------
** BEGIN: class LocalTableModel
**---------------------------------------------------
*/
/** LocalTableModel */
public class AlarmWritersTableModel extends AbstractTableModel
{
	private static final long serialVersionUID = 1L;

	public static final String[] TAB_HEADER = {"Use", "Name", "Class Name", "Description"};
	public static final int TAB_POS_USE          = 0;
	public static final int TAB_POS_NAME         = 1;
	public static final int TAB_POS_CLASSNAME    = 2;
	public static final int TAB_POS_DESCRIPTION  = 3;

	public String getToolTipText(int col)
	{
		switch(col)
		{
		case TAB_POS_USE:         return "Should this entry be used";
		case TAB_POS_NAME:        return "Name of the Writer.";
		case TAB_POS_CLASSNAME:   return "The classname of the Writer.";
		case TAB_POS_DESCRIPTION: return "Description";
		}
		return null;
	}

	public AlarmWritersTableModel()
	{
		super();
	}

	private List<AlarmWriterEntry> _entries = new ArrayList<>();
	
	public static class AlarmWriterEntry
	{
		boolean _modified = false;
		boolean _selected;

		String  _name;
		String  _className;
		String  _description;
		
//		List<AlarmWriterSettingsEntry> _settings = new ArrayList<>();
		List<CmSettingsHelper> _settings = new ArrayList<>();
		List<CmSettingsHelper> _filters = new ArrayList<>();
		
		public boolean                 isSelected()     { return _selected; }
		public String                  getName()        { return _name; }
		public String                  getClassName()   { return _className; }
		public String                  getDescription() { return _description; }
		public List<CmSettingsHelper>  getSettings()    { return _settings; }
		public List<CmSettingsHelper>  getFilters()     { return _filters; }
		
		public boolean checkSettingsForMandatoryData()
		{
//			for (AlarmWriterSettingsEntry awse : _settings)
			for (CmSettingsHelper awse : _settings)
			{
//				if (awse.isMandatory() && ! awse.hasValue())
				if ( awse.isMandatory() && ! awse.hasValidValue() )
					return true;
//				if (awse._value.indexOf(CmSettingsHelper.MANDATORY) != -1)
//					return true;
			}
			return false;
		}

		public boolean checkSettingsForProbableData()
		{
//			for (AlarmWriterSettingsEntry awse : _settings)
			for (CmSettingsHelper awse : _settings)
			{
//				if (awse._selected)
//				{
//					if (awse.isOptional() && awse.hasOptionalValue())
//						return true;
//					if (awse.isTemplate() && awse.hasTemplateValue())
//						return true;
//				}
				if (awse.isSelected())
				{
					if ( awse.isProbable() && ! awse.hasValidValue() )
						return true;
				}
			}
			return false;
		}

		public int getSettingsUsedCount()
		{
			int count = 0;
//			for (AlarmWriterSettingsEntry awse : _settings)
//			{
//				if (awse._selected)
//					count++;
//			}
			for (CmSettingsHelper awse : _settings)
			{
				if (awse.isSelected())
					count++;
			}
			return count;
		}
	}

//	protected class AlarmWriterSettingsEntry
//	{
//		boolean _modified = false;
//		boolean _selected;
//		Type    _settingsType;
//
//		String  _name;
//		String  _propKey;
//		String  _datatype;
//		String  _value;
//		String  _description;
//
//		public boolean isMandatory() { return Type.MANDATORY.equals(_settingsType);	}
//		public boolean isOptional()  { return Type.OPTIONAL .equals(_settingsType);	}
//		public boolean isTemplate()  { return Type.TEMPLATE .equals(_settingsType);	}
//
//		public boolean hasValue()
//		{
//			return StringUtil.hasValue(_value);
//		}
//		public boolean hasOptionalValue()
//		{
//			if (StringUtil.isNullOrBlank(_value))
//				return false;
//			return _value.matches("<.*?>");
//		}
//		public boolean hasTemplateValue()
//		{
//			if (StringUtil.isNullOrBlank(_value))
//				return false;
//			return _value.matches("<.*?>");
//		}
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
		AlarmWriterEntry e = _entries.get(row);
		switch (col)
		{
		case TAB_POS_USE:         return e._selected;
		case TAB_POS_NAME:        return e._name;
		case TAB_POS_CLASSNAME:   return e._className;
		case TAB_POS_DESCRIPTION: return e._description;
		}
		
		return null;
	}

	@Override
	public void setValueAt(Object newVal, int row, int col)
	{
		if ( ! isCellEditable(row, col) )
			return;

		AlarmWriterEntry e = _entries.get(row);
		Object oldVal = getValueAt(row, col);

		// has the value changed: mark it as modified
		if (oldVal != null )
			if ( ! oldVal.equals(newVal) )
				e._modified = true;
		if (newVal != null )
			if ( ! newVal.equals(oldVal) )
				e._modified = true;

		// Set the value
		if (col == TAB_POS_USE)
			e._selected = Boolean.valueOf(newVal.toString());

		fireTableCellUpdated(row, col);
	}

	@Override
	public Class<?> getColumnClass(int col)
	{
		if (col == TAB_POS_USE) return Boolean.class;

		return Object.class;
	}

	@Override
	public boolean isCellEditable(int row, int col)
	{
		if (col == TAB_POS_USE) return true;
		
		return false;
	}



	/** Populate information in the table */
	public void refreshTable()
	{
		_entries.clear();

		try
		{
			ArrayList<Class<?>> writerClasses = new ArrayList<>();
			
			// ADD writers in package: com.dbxtune.alarm.writers
			Reflections reflections = new Reflections("com.dbxtune.alarm.writers");
			Set<Class<? extends IAlarmWriter>> subTypes = reflections.getSubTypesOf(IAlarmWriter.class);
			for (Class<? extends IAlarmWriter> clazz : subTypes)
			{
				if (    ! "IAlarmWriter"               .equals(clazz.getSimpleName())
				     && ! "AlarmWriterAbstract"        .equals(clazz.getSimpleName())
				     && ! "AlarmWriterToTableModel"    .equals(clazz.getSimpleName())
				     && ! "AlarmWriterToPcsJdbc"       .equals(clazz.getSimpleName())
				     && ! "AlarmWriterToApplicationLog".equals(clazz.getSimpleName())
				   )
					writerClasses.add(clazz);
			}

//			Collections.sort(writerClasses);

//			// Get classes under the package "com.dbxtune.alarm.writers" that implements IAlarmWriter
//			ArrayList<Class<?>> writerClasses = new ArrayList<>();
//			Class<?>[] xxx = getClasses("com.dbxtune.alarm.writers");
//			for (Class<?> clazz : xxx)
//			{
//				if (IAlarmWriter.class.isAssignableFrom(clazz)) 
//				{
//					if (    ! "IAlarmWriter"               .equals(clazz.getSimpleName())
//					     && ! "AlarmWriterAbstract"        .equals(clazz.getSimpleName())
//					     && ! "AlarmWriterToTableModel"    .equals(clazz.getSimpleName())
//					     && ! "AlarmWriterToPcsJdbc"       .equals(clazz.getSimpleName())
//					     && ! "AlarmWriterToApplicationLog".equals(clazz.getSimpleName())
//					   )
//						writerClasses.add(clazz);
//				}
//			}

			// Get "active" alarm writers from the AlarmHandler
			// Used to set "Use" when refreshing the table model
			List<String> activeWriterClassNames = new ArrayList<>();
			if (AlarmHandler.hasInstance())
			{
				for (IAlarmWriter writer : AlarmHandler.getInstance().getAlarmWriters())
					activeWriterClassNames.add(writer.getClass().getName());
			}


			for (Class<?> clazz : writerClasses)
			{
//				System.out.println("xxxx: "+clazz.getSimpleName());

				IAlarmWriter inst = (IAlarmWriter) clazz.newInstance();

				// if already in the AlarmHandler:s list of added writers
				boolean selected = activeWriterClassNames.contains(clazz.getName());

				// Add WRITERS
				AlarmWriterEntry awe = new AlarmWriterEntry();
				awe._selected    = selected;
				awe._name        = inst.getName();
				awe._className   = clazz.getName();
				awe._description = inst.getDescription();

				awe._settings = inst.getAvailableSettings();
				awe._filters  = inst.getAvailableFilters();
				
				// Set as SELECTED setting
				// - mandatory values is set by the CmSettingsHelper itself
				// - for probable values
				// - for settings values is NOT the same as the default value  
				for (CmSettingsHelper sh : awe._settings)
				{
					if (sh.isProbable() && sh.hasValue())
						sh.setSelected(true);

					// If value it's NOT the same as the default; then check the "selected" box... 
					if ( ! sh.isDefaultValue() )
						sh.setSelected(true);
				}

//				// Add SETTINGS for this WRITERS
//				for (CmSettingsHelper sh : inst.getAvailableSettings())
//				{
//					AlarmWriterSettingsEntry awse = new AlarmWriterSettingsEntry();
//
////					boolean use = false;
////					String strVal = sh.getStringValue();
////					if (strVal.indexOf(CmSettingsHelper.MANDATORY) != -1)
////						use = true;
//					
//					awse._settingsType = sh.getType();
//					
////					awse._selected    = use;
//					awse._selected    = sh.isMandatory();
//					awse._name        = sh.getName();
//					awse._propKey     = sh.getPropName();
//					awse._datatype    = sh.getDataTypeString();
//					awse._value       = sh.getStringValue();
//					awse._description = sh.getDescription();
//
//					awe._settings.add(awse);
//				}
				
				_entries.add(awe);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		fireTableDataChanged();
	}

	public AlarmWriterEntry getWriterEntryForRow(int row)
	{
		AlarmWriterEntry e = _entries.get(row);
		return e;
	}

//	public List<AlarmWriterSettingsEntry> getSettingsForRow(int row)
//	{
//		AlarmWriterEntry e = _entries.get(row);
//		return e._settings;
//	}
	public List<CmSettingsHelper> getSettingsForRow(int row)
	{
		AlarmWriterEntry e = _entries.get(row);
		return e._settings;
	}

	public List<CmSettingsHelper> getFiltersForRow(int row)
	{
		AlarmWriterEntry e = _entries.get(row);
		return e._filters;
	}

	public String getClassNameForRow(int row)
	{
		AlarmWriterEntry e = _entries.get(row);
		return e._className;
	}

	/** Check if this model has changed */
	public boolean isDirty()
	{
		for (AlarmWriterEntry awe : _entries)
		{
			if (awe._modified)
				return true;
			
//			for (AlarmWriterSettingsEntry awse : awe._settings)
//			{
//				if (awse._modified)
//					return true;
//			}
			for (CmSettingsHelper awse : awe._settings)
			{
				if (awse.isModified())
					return true;
			}
			for (CmSettingsHelper awse : awe._filters)
			{
				if (awse.isModified())
					return true;
			}
		}

		// Finally: no changes 
		return false;
	}

	public int getUsedCount()
	{
		int count = 0;
		for (int r=0; r<getRowCount(); r++)
		{
			boolean val = (boolean) getValueAt(r, TAB_POS_USE);
			if (val)
				count++;
		}
		return count;
	}


	public int getModifiedRowCount()
	{
		int count = 0;
		for (AlarmWriterEntry awe : _entries)
		{
			if (awe._modified)
			{
				count++;
				continue;
			}
			
//			for (AlarmWriterSettingsEntry awse : awe._settings)
//			{
//				if (awse._modified)
//				{
//					count++;
//					break; // skip Settings and continue with next Writer
//				}
//			}
			for (CmSettingsHelper awse : awe._settings)
			{
				if (awse.isModified())
				{
					count++;
					break; // skip Settings and continue with next Writer
				}
			}
		}
		return count;
	}


	/** 
	 * Return first problem we discover in the model, note: They may contain HTML tags 
	 * @return null if no problems otherwise a text 
	 */
	public String getProblem()
	{
		int count = 0;
		for (AlarmWriterEntry awe : _entries)
		{
			if (awe._selected)
				count++;
		}
		if (count == 0)
			return "<html>You need to <i>select</i> what Writers you want to use. Click the <i>Use</i> cell.  Note: You can select <b>several</b> writers...</html>";

		for (AlarmWriterEntry awe : _entries)
		{
			if (awe._selected)
			{
				if (awe.checkSettingsForMandatoryData())
				{
					return "Mandatory values need to be filled in. For writer '"+awe._name+"'.";
				}

				if (awe.checkSettingsForProbableData())
				{
					return "Optional/Template values need to be replaced with real values. For writer '"+awe._name+"'.";
				}
			}
		}

		// OK 
		return null;
	}

	public static void main(String[] args)
	{
//		try
//		{
//			ArrayList<Class<?>> writerClasses = new ArrayList<>();
//			Class<?>[] xxx = getClasses("com.dbxtune.alarm.writers");
//			for (Class<?> clazz : xxx)
//			{
//				if (IAlarmWriter.class.isAssignableFrom(clazz)) 
//				{
//					if (    ! "IAlarmWriter"               .equals(clazz.getSimpleName())
//					     && ! "AlarmWriterToTableModel"    .equals(clazz.getSimpleName())
//					     && ! "AlarmWriterToPcsJdbc"       .equals(clazz.getSimpleName())
//					     && ! "AlarmWriterToApplicationLog".equals(clazz.getSimpleName())
//					   )
//						writerClasses.add(clazz);
//				}
//			}
//			
//			for (Class<?> clazz : writerClasses)
//			{
//				System.out.println("xxxx: "+clazz.getSimpleName());
//			}
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//		}

		String packageName = "com.dbxtune.alarm.writers";
		System.out.println("\n\n---- START ---- "+packageName+" ----");
		Reflections reflections = new Reflections(packageName);

		Set<Class<? extends IAlarmWriter>> subTypes = reflections.getSubTypesOf(IAlarmWriter.class);
		
		for (Class<? extends IAlarmWriter> clazz : subTypes)
		{
			System.out.println("xxxx: "+clazz.getSimpleName());
		}
		System.out.println("---- END ---- "+packageName+" ----");
		

	
	
		packageName = "com.dbxtune.pcs";
		System.out.println("\n\n---- START ----  "+packageName+" ----");
		Reflections reflections2 = new Reflections(packageName);

		Set<Class<? extends IPersistWriter>> subTypes2 = reflections2.getSubTypesOf(IPersistWriter.class);
		
		for (Class<? extends IPersistWriter> clazz : subTypes2)
		{
			System.out.println("xxxx: "+clazz.getSimpleName());
		}
		System.out.println("---- END ----  "+packageName+" ----");
		
	
	}

//	/**
//	 * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
//	 *
//	 * @param packageName     The base package
//	 * @return                The classes
//	 * 
//	 * @throws ClassNotFoundException
//	 * @throws IOException
//	 */
//	@SuppressWarnings("rawtypes")
//	private static Class[] getClasses(String packageName) throws ClassNotFoundException, IOException
//	{
//		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
//		assert classLoader != null;
//		String path = packageName.replace('.', '/');
//		Enumeration<URL> resources = classLoader.getResources(path);
//		List<File> dirs = new ArrayList<File>();
//		while (resources.hasMoreElements())
//		{
//			URL resource = resources.nextElement();
//			dirs.add(new File(resource.getFile()));
//		}
//		ArrayList<Class> classes = new ArrayList<Class>();
//		for (File directory : dirs)
//		{
//			classes.addAll(findClasses(directory, packageName));
//		}
//		return classes.toArray(new Class[classes.size()]);
//	}
//
//	/**
//	 * Recursive method used to find all classes in a given directory and subdirs.
//	 *
//	 * @param directory
//	 *            The base directory
//	 * @param packageName
//	 *            The package name for classes found inside the base directory
//	 * @return The classes
//	 * @throws ClassNotFoundException
//	 */
//	@SuppressWarnings("rawtypes")
//	private static List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException
//	{
//		List<Class> classes = new ArrayList<Class>();
//		if ( !directory.exists() )
//		{
//			return classes;
//		}
//		File[] files = directory.listFiles();
//		for (File file : files)
//		{
//			if ( file.isDirectory() )
//			{
//				assert !file.getName().contains(".");
//				classes.addAll(findClasses(file, packageName + "." + file.getName()));
//			}
//			else if ( file.getName().endsWith(".class") )
//			{
//				classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
//			}
//		}
//		return classes;
//	}
}
/*---------------------------------------------------
** END: class LocalCmAlarmSettingsModel
**---------------------------------------------------
*/
