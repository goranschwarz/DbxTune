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
package com.dbxtune.cm;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.CronUtils;
import com.dbxtune.utils.StringUtil;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import it.sauronsoftware.cron4j.InvalidPatternException;
import it.sauronsoftware.cron4j.SchedulingPattern;

/**
 * Used by the: Create 'Offline Session' Wizard<br>
 * to get "local option" details
 * 
 * @author gorans
 */
public class CmSettingsHelper
{
	private boolean  _isSelected   = false; // can be used by a Table Model or simular...
	private boolean  _isModified   = false; // can be used by a Table Model or simular... to check if the entry has been changed, should OK button be enabled/disabled

	private Type     _type         = Type.NOT_USED;
	private String   _name         = null;
	private String   _propName     = null;
	private Class<?> _dataType     = null;
	private String   _stringValue  = null;
	private String   _defaultValue = null;
	private String   _description  = null;
	
	private InputValidator _inputValidator = null;

	public interface InputValidator
	{
		public boolean isValid(CmSettingsHelper sh, String newValueStr) throws ValidationException;
	}

	/** RegExp - Input validator */
	public static class RegExpInputValidator
	implements InputValidator
	{
		@Override
		public boolean isValid(CmSettingsHelper sh, String val) throws ValidationException
		{
			try { Pattern.compile(val); }
			catch(PatternSyntaxException ex) { throw new ValidationException("The RegExp '"+val+"' seems to be faulty. Caught: "+ex.getMessage()); }
			return true;
		}
	}

	/** URL - Input validator */
	public static class UrlInputValidator
	implements InputValidator
	{
		@Override
		public boolean isValid(CmSettingsHelper sh, String val) throws ValidationException
		{
			try { new URL(val); }
			catch(MalformedURLException ex) { throw new ValidationException("The URL '"+val+"' seems to be malformed. Caught: "+ex.getMessage()); }
			return true;
		}
	}
	/** JSON - Input validator */
	public static class JsonInputValidator
	implements InputValidator
	{
		@Override
		public boolean isValid(CmSettingsHelper sh, String val) throws ValidationException
		{
			try { Gson gson = new Gson(); gson.fromJson(val, Object.class); }
			catch(JsonSyntaxException ex) { throw new ValidationException("The JSON content seems to be faulty. Caught: "+ex.getMessage()); }
			return true;
		}
	}

	/** Integer - Input validator */
	public static class IntegerInputValidator
	implements InputValidator
	{
		@Override
		public boolean isValid(CmSettingsHelper sh, String val) throws ValidationException
		{
			try { Integer.parseInt(val); }
			catch(NumberFormatException ex) { throw new ValidationException("The value '"+val+"' is not a valid Integer: "+ex.getMessage()); }
			return true;
		}
	}

	/** Map with number - Input validator */
	public static class MapNumberValidator
	implements InputValidator
	{
		@Override
		public boolean isValid(CmSettingsHelper sh, String val) throws ValidationException
		{
			Map<String, String> map = StringUtil.parseCommaStrToMap(val);
			
			for (String mapKey : map.keySet())
			{
				String mapVal = map.get(mapKey);

				// MAP-KEY: Check key (if it passes regexp check) 
				try { Pattern.compile(mapKey); }
				catch(PatternSyntaxException ex) { throw new ValidationException("The RegExp '"+mapVal+"' seems to be faulty, for key '"+mapKey+"'. Caught: "+ex.getMessage()); }

				// MAP-VAL: Check number
				try { NumberUtils.createNumber(mapVal); }
				catch (NumberFormatException ex) { throw new ValidationException("The number value '"+mapVal+"' is not a number for key '"+mapKey+"'. Caught: "+ex.getMessage()); }
			}
			
			return true;
		}
	}

	/** CronTimeRange - Input validator */
	public static class CronTimeRangeInputValidator
	implements InputValidator
	{
		@Override
		public boolean isValid(CmSettingsHelper sh, String val) throws ValidationException
		{
			String cronStr = val;

			// is this a negation/not within time period...
			//boolean negation = false;
			if (cronStr.startsWith("!"))
			{
				//negation = true;
				cronStr  = cronStr.substring(1);
			}

			try
			{
				new SchedulingPattern(cronStr);
				return true;
			}
			catch(InvalidPatternException ex)
			{
				throw new ValidationException("The specified 'cron' value '"+cronStr+"' is not a valid cron-pattern. This will be disregarded. Caught: " + ex.getMessage());
			}
		}
	}


	/** ValidationException is thrown when you try to set data to a invalid content */
	public static class ValidationException extends Exception
	{
		private static final long serialVersionUID = 1L;

		public ValidationException(String message)
		{
			super(message);
		}
	}
	
	public boolean isSelected() { return _isSelected;	}
	public boolean isModified() { return _isModified;	}

	public void setSelected(boolean selected)      { _isSelected  = selected; }
	public void setModified(boolean modified)      { _isModified  = modified; }

	public boolean isMandatory()   { return Type.MANDATORY      .equals(_type);	}
	public boolean isProbable()    { return Type.PROBABLY       .equals(_type);	}
//	public boolean isOptional()    { return Type.OPTIONAL       .equals(_type);	}
//	public boolean isTemplate()    { return Type.TEMPLATE       .equals(_type);	}
	public boolean isAlarmSwitch() { return Type.IS_ALARM_SWITCH.equals(_type);	}
	public boolean isPreCheck()    { return Type.IS_PRE_CHECK   .equals(_type);	}
	
	public boolean hasValue() 
	{
		return StringUtil.hasValue(_stringValue);
	}
	public boolean hasValidValue() 
	{
		if (StringUtil.isNullOrBlank(_stringValue))
			return false;

		// If it's a Integer, value -1 is a typical "not yet set"
		if (_dataType == Integer.class)
		{
			if ( StringUtil.parseInt(_stringValue, -1) == -1)
				return false;
		}
		
		// Starts with HTML then it's OK with tags
		if (StringUtils.startsWithIgnoreCase(_stringValue.trim(), "<html>"))
		{
			return true;
		}

		// match()
		// returns true on "<xxxxx>" 
		// returns false on "xxx <xxxxx> xxx" 
//		return _stringValue.matches("<.+>");

		// find()
		// returns true on "<xxxxx>" 
		// returns true on "xxx <xxxxx> xxx" 
		Pattern p = Pattern.compile("<.+>");
        Matcher m = p.matcher(_stringValue);
        if (m.find())
        	return false;
        
    	return true;
	}

//	public boolean hasProbableValue() 
//	{
//		if (StringUtil.isNullOrBlank(_stringValue))
//			return false;
//
//		// returns true on "<xxxxx>" 
//		// returns false on "xxx <xxxxx> xxx" 
////		return _stringValue.matches("<.+>");
//
//		// returns true on "<xxxxx>" 
//		// returns true on "xxx <xxxxx> xxx" 
//		Pattern p = Pattern.compile("<.+>");
//        Matcher m = p.matcher(_stringValue);
//        return m.find();
//	}

	/**
	 * Check if the applied setting is the same as the default value...
	 * @return If the applied <b>value</b> of the setting is equal the default value... then return true, otherwise false
	 */
	public boolean isDefaultValue()
	{
		if (getDefaultValue() == getStringValue())
			return true;
		
		if (getDefaultValue() != null && getDefaultValue().equals(getStringValue()))
			return true;

		if (getStringValue() != null && getStringValue().equals(getDefaultValue()))
			return true;
		
		return false;
	}

	public boolean isValidInput(String newValueStr)
	throws ValidationException
	{
		if (_inputValidator != null)
			return _inputValidator.isValid(this, newValueStr);
		return true;
	}
	
	public String getInputValidatorClassName()
	{
		if (_inputValidator == null)
			return "None";

		return _inputValidator.getClass().getSimpleName();
	}
	
	public Type   getType()           { return _type; }
	public String getName()           { return _name; }
	public String getPropName()       { return _propName; }
	public Class<?> getDataType()     { return _dataType; }
	public String getDataTypeString() { return _dataType.getSimpleName(); }
	public String getStringValue()    { return _stringValue; }
	public String getDefaultValue()   { return _defaultValue; }
	public String getDescription()    { return _description; }

	public void setType(Type type)                   { _type         = type; }
	public void setName(String name)                 { _name         = name; }
	public void setPropName(String propName)         { _propName     = propName; }
	public void setDataType(Class<?> dataType)       { _dataType     = dataType; }
	public void setDefaultValue(String defaultValue) { _defaultValue = defaultValue; }
	public void setDescription(String description)   { _description  = description; }

	public void setStringValueNoValidation(String stringValue)   
	{ 
		_stringValue  = stringValue; 
	}
	public void setStringValue(String stringValue)
	throws ValidationException
	{
		isValidInput(stringValue);_stringValue  = stringValue; 
	}

	public enum Type
	{
		NOT_USED(""), 
		IS_ALARM_SWITCH("isAlarmSwitch"), // used when column name is/can be enabled or disabled... '<CMNAME>.alarm.system.enabled.<COLNAME> = true|false' 
		IS_PRE_CHECK("isPreCheck"), // This parameter is for ALL alarms in this module 
		MANDATORY("<mandatory>"), 
		PROBABLY("<probably>"); 
//		OPTIONAL("<optional>"), 
//		TEMPLATE("<template>");
		
	    private final String text;
	    
	    private Type(String text) { this.text = text; }
	    public String getText() { return text; }

	};
//	/** Use this as a value if the setting is mandatory */
//	public static final String MANDATORY = "<mandatory>";
//
//	/** Use this as a value if the setting is optional */
//	public static final String OPTIONAL = "<optional>";
//	
//	/** Use this as a value if the setting needs to be changed */
//	public static final String TEMPLATE = "<template>";
	
	public static CmSettingsHelper create_isAlarmEnabled(CountersModel cm, String colname)
	{
		Configuration conf = Configuration.getCombinedConfiguration();

		// isAlarmEnabled
		String  isAlarmEnabled_propKey = cm.replaceCmAndColName(CountersModel.PROPKEY_ALARM_isSystemAlarmsForColumnEnabled, colname);
		boolean isAlarmEnabled_defVal  = CountersModel.DEFAULT_ALARM_isSystemAlarmsForColumnEnabled;
		boolean isAlarmEnabled_propVal = conf.getBooleanProperty(isAlarmEnabled_propKey, isAlarmEnabled_defVal);
		String  isAlarmEnabled_desc    = "Is the Alarm Enabled or Disabled";

		return new CmSettingsHelper(colname + " isAlarmEnabled", isAlarmEnabled_propKey, Boolean.class, isAlarmEnabled_propVal, isAlarmEnabled_defVal, isAlarmEnabled_desc);
	}

	public static CmSettingsHelper create_timeRangeCron(CountersModel cm, String colname)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		// timeRangeCron
		String timeRangeCron_propKey = cm.replaceCmAndColName(CountersModel.PROPKEY_ALARM_isSystemAlarmsForColumnInTimeRange, colname);
		String timeRangeCron_defVal  = CountersModel.DEFAULT_ALARM_isSystemAlarmsForColumnInTimeRange;
		String timeRangeCron_propVal = conf.getProperty(timeRangeCron_propKey, timeRangeCron_defVal);
		String timeRangeCron_desc    = "At what time the alarm can fire. decoded[" + CronUtils.getCronExpressionDescriptionForAlarms(timeRangeCron_propVal) + "]";

		return new CmSettingsHelper(colname + " timeRangeCron", timeRangeCron_propKey, String.class, timeRangeCron_propVal, timeRangeCron_defVal, timeRangeCron_desc, new CronTimeRangeInputValidator());
	}

	/**
	 * Add a CmSettingsHelper to the passed 'list'<br>
	 * <ul>
	 *     <li>if param: name='SomeName' and optionName=null --- Then it's an isAlarmSwitch parameter.</li>
	 *     <li>if param: name='SomeName' and optionName='SomeOption' --- Then it's an parameter to the: name='SomeName'</li>
	 * </ul>
	 * Also if this is an <i>isAlarmSwitch</i> Then some extra parameters might be added.
	 * 
	 * @param cm            CountersModel
	 * @param list          List that the CmSettingsHelper will be added to
	 * @param name          Name or Slogan of this setting
	 * @param optionName    null=isAlarmSwitch, if value then this is a 'option' / 'parameter' to the isAlarmSwitch
	 * @param propName      Property Name/Key for this setting
	 * @param dataType      Datatype for this setting
	 * @param stringValue   Current configuration, or suggested value for this setting
	 * @param defaultValue  Default value for this setting
	 * @param description   A longer description of what this setting does
	 * @param inputValidator If we have an user defined InputValidator
	 */
	public static void add(CountersModel cm, List<CmSettingsHelper> list, String name, String optionName, String propName, Class<?> dataType, Object stringValue, Object defaultValue, String description, InputValidator inputValidator)
	{
		if (list == null)
			throw new RuntimeException("Parameter 'list' can't be null.");
		if (name == null && optionName == null)
			throw new RuntimeException("Parameter 'name' and 'optionName' can't BOTH be null.");

		// Create IS_ALARM_SWITCH
		if (name != null && optionName == null)
		{
			list.add(new CmSettingsHelper(name, Type.IS_ALARM_SWITCH, propName, dataType, stringValue, defaultValue, description, inputValidator));

			// Add "extra" settings...
			list.add(create_isAlarmEnabled(cm, name));
			list.add(create_timeRangeCron (cm, name));
		}
		// Create parameters to any alarmSwitch
		else
		{
			list.add(new CmSettingsHelper(name, propName, dataType, stringValue, defaultValue, description, inputValidator));
		}
	}
	
	public CmSettingsHelper()
	{
	}

	/**
	 * Create a helper
	 * 
	 * @param name           Name or Slogan of this setting
	 * @param type           If this parameter is mandatory/optional/template
	 * @param propName       Property Name/Key for this setting
	 * @param dataType       Datatype for this setting
	 * @param stringValue    Current configuration, or suggested value for this setting
	 * @param defaultValue   Default value for this setting
	 * @param description    A longer description of what this setting does
	 * @param inputValidator If we have an user defined InputValidator
	 */
	public CmSettingsHelper(String name, Type type, String propName, Class<?> dataType, Object stringValue, Object defaultValue, String description, InputValidator inputValidator)
	{
		_name           = name;
		_type           = type;
		_propName       = propName;
		_dataType       = dataType;
		_stringValue    = stringValue  == null ? null : stringValue.toString();    // Should we allow null or should we set it to ""
		_defaultValue   = defaultValue == null ? null : defaultValue.toString();   // Should we allow null or should we set it to "";
		_description    = description;
		_inputValidator = inputValidator;

		if (_name != null)
			_name = _name.trim();

		if (_inputValidator == null)
			_inputValidator = new InputValidatorBasic();
		
		if (Type.MANDATORY.equals(_type))
			_isSelected = true;
	}

	/**
	 * Create a helper
	 * 
	 * @param name          Name or Slogan of this setting
	 * @param type          If this parameter is mandatory/optional/template
	 * @param propName      Property Name/Key for this setting
	 * @param dataType      Datatype for this setting
	 * @param stringValue   Current configuration, or suggested value for this setting
	 * @param defaultValue  Default value for this setting
	 * @param description   A longer description of what this setting does
	 */
	public CmSettingsHelper(String name, Type type, String propName, Class<?> dataType, Object stringValue, Object defaultValue, String description)
	{
		this(name, type, propName, dataType, stringValue, defaultValue, description, null);
	}

	/**
	 * Create a helper
	 * 
	 * @param name          Name or Slogan of this setting
	 * @param propName      Property Name/Key for this setting
	 * @param dataType      Datatype for this setting
	 * @param stringValue   Current configuration, or suggested value for this setting
	 * @param defaultValue  Default value for this setting
	 * @param description   A longer description of what this setting does
	 * @param inputValidator If we have an user defined InputValidator
	 */
	public CmSettingsHelper(String name, String propName, Class<?> dataType, Object stringValue, Object defaultValue, String description, InputValidator inputValidator)
	{
		this(name, Type.NOT_USED, propName, dataType, stringValue, defaultValue, description, inputValidator);
	}

	/**
	 * Create a helper
	 * 
	 * @param name          Name or Slogan of this setting
	 * @param propName      Property Name/Key for this setting
	 * @param dataType      Datatype for this setting
	 * @param stringValue   Current configuration, or suggested value for this setting
	 * @param defaultValue  Default value for this setting
	 * @param description   A longer description of what this setting does
	 */
	public CmSettingsHelper(String name, String propName, Class<?> dataType, Object stringValue, Object defaultValue, String description)
	{
		this(name, Type.NOT_USED, propName, dataType, stringValue, defaultValue, description, null);
	}

	/**
	 * Create a helper
	 * 
	 * @param propName      Property Name/Key for this setting
	 * @param dataType      Datatype for this setting
	 * @param stringValue   Current configuration, or suggested value for this setting
	 * @param description   A longer description of what this setting does
	 */
	public CmSettingsHelper(String propName, Class<?> dataType, Object stringValue, String description)
	{
		this(null, Type.NOT_USED, propName, dataType, stringValue, null, description, null);
	}




	public static class InputValidatorBasic
	implements InputValidator
	{
		/**
		 * Checks if a new value is OK, based on Datatype etc...
		 * @param newValObj
		 * @return true if OK, otherwise it throws an exception
		 * @throws Exception if NOT ok
		 */
		@Override
		public boolean isValid(CmSettingsHelper sh, String val) 
		throws ValidationException
		{
			if (val == null)
				val = "";
			
			if (BigDecimal.class == sh._dataType)
			{
				try { new BigDecimal(val); }
				catch(NumberFormatException ex) { throw new ValidationException("The value '"+val+"' is not a valid BigDecimal: "+ex.getMessage()); }
			}
				
			if (BigInteger.class == sh._dataType)
			{
				try { new BigInteger(val); }
				catch(NumberFormatException ex) { throw new ValidationException("The value '"+val+"' is not a valid BigInteger: "+ex.getMessage()); }
			}
				
			if (Byte.class == sh._dataType)
			{
				try { Byte.valueOf(val); }
				catch(NumberFormatException ex) { throw new ValidationException("The value '"+val+"' is not a valid Byte: "+ex.getMessage()); }
			}
				
			if (Double.class == sh._dataType)
			{
				try { Double.valueOf(val); }
				catch(NumberFormatException ex) { throw new ValidationException("The value '"+val+"' is not a valid Double: "+ex.getMessage()); }
			}
				
			if (Float.class == sh._dataType)
			{
				try { Float.valueOf(val); }
				catch(NumberFormatException ex) { throw new ValidationException("The value '"+val+"' is not a valid Float: "+ex.getMessage()); }
			}
				
			if (Integer.class == sh._dataType)
			{
				try { Integer.valueOf(val); }
				catch(NumberFormatException ex) { throw new ValidationException("The value '"+val+"' is not a valid Integer: "+ex.getMessage()); }
			}
				
			if (Long.class == sh._dataType)
			{
				try { Long.valueOf(val); }
				catch(NumberFormatException ex) { throw new ValidationException("The value '"+val+"' is not a valid Long: "+ex.getMessage()); }
			}
				
			if (Short.class == sh._dataType)
			{
				try { Short.valueOf(val); }
				catch(NumberFormatException ex) { throw new ValidationException("The value '"+val+"' is not a valid Short: "+ex.getMessage()); }
			}
				
			if (Boolean.class == sh._dataType)
			{
				if ( ! (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("false")) )
					throw new ValidationException("The value '"+val+"' is not an Boolean: true or false must be given");
			}
				
			return true;
		}
	}
	
	/**
	 * Get a entry from a list
	 */
	public static CmSettingsHelper getByName(String name, Collection<CmSettingsHelper> list)
	{
		if (name == null)
			return null;

		for (CmSettingsHelper cmSettingsHelper : list)
		{
			if (name.equals(cmSettingsHelper.getName()))
				return cmSettingsHelper;
		}

		// No entry was found
		return null;
	}
}
