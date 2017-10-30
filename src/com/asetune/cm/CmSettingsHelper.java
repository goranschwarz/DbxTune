package com.asetune.cm;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.asetune.utils.StringUtil;

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

	public boolean isMandatory() { return Type.MANDATORY.equals(_type);	}
	public boolean isProbable()  { return Type.PROBABLY .equals(_type);	}
//	public boolean isOptional()  { return Type.OPTIONAL .equals(_type);	}
//	public boolean isTemplate()  { return Type.TEMPLATE .equals(_type);	}
	
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
	
	public CmSettingsHelper()
	{
	}

	/**
	 * Create a helper
	 * 
	 * @param type           If this parameter is mandatory/optional/template
	 * @param name           Name or Slogan of this setting
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
		_stringValue    = stringValue == null ? null : stringValue.toString();    // Should we allow null or should we set it to ""
		_defaultValue   = defaultValue == null ? null : defaultValue.toString();    // Should we allow null or should we set it to "";
		_description    = description;
		_inputValidator = inputValidator;

		if (_inputValidator == null)
			_inputValidator = new InputValidatorBasic();
		
		if (Type.MANDATORY.equals(_type))
			_isSelected = true;
	}

	/**
	 * Create a helper
	 * 
	 * @param type          If this parameter is mandatory/optional/template
	 * @param name          Name or Slogan of this setting
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
				try { new Byte(val); }
				catch(NumberFormatException ex) { throw new ValidationException("The value '"+val+"' is not a valid Byte: "+ex.getMessage()); }
			}
				
			if (Double.class == sh._dataType)
			{
				try { new Double(val); }
				catch(NumberFormatException ex) { throw new ValidationException("The value '"+val+"' is not a valid Double: "+ex.getMessage()); }
			}
				
			if (Float.class == sh._dataType)
			{
				try { new Float(val); }
				catch(NumberFormatException ex) { throw new ValidationException("The value '"+val+"' is not a valid Float: "+ex.getMessage()); }
			}
				
			if (Integer.class == sh._dataType)
			{
				try { new Integer(val); }
				catch(NumberFormatException ex) { throw new ValidationException("The value '"+val+"' is not a valid Integer: "+ex.getMessage()); }
			}
				
			if (Long.class == sh._dataType)
			{
				try { new Long(val); }
				catch(NumberFormatException ex) { throw new ValidationException("The value '"+val+"' is not a valid Long: "+ex.getMessage()); }
			}
				
			if (Short.class == sh._dataType)
			{
				try { new Short(val); }
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
}
