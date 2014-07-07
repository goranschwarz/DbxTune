/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.asetune.Version;

public class Configuration
extends Properties
{
	/*---------------------------------------------------
	** class members
	**---------------------------------------------------
	*/
	private static final long serialVersionUID = 5707562050158600080L;

	private static final String ENCRYPTED_PREFIX = "encrypted:";


	public static final String SYSTEM_CONF = "SYSTEM_CONF"; 
	public static final String USER_CONF   = "USER_CONF"; 
	public static final String USER_TEMP   = "USER_TEMP"; 
	public static final String PCS         = "PCS"; 

	public static final String TAIL_TEMP   = "TAIL_TEMP"; 
	
	/** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(Configuration.class);

	// implements singleton pattern
//	private static Configuration _instance = null;
	private static HashMap<String,Configuration> _instMap  = new HashMap<String,Configuration>();

	private String _propFileName = null;
	private boolean _saveOnExit = false;

	/** If the save() method should save or just simply return */
	private boolean _saveIsEnabled = true;
	
	/** The name of the config */
	private String _confName = "";

//	private List _writers = new ArrayList();

	// original serialVersionUID = 5707562050158600080L
	private static String encrypterBaseKey = "qazZSE44wsxXDR55"+serialVersionUID+"edcCFT66rfvVGY77";
//	private static Encrypter baseEncrypter = new Encrypter(encrypterBaseKey);

	private String _embeddedMessage = "This file will be overwritten and maintained by "+Version.getAppName();

	public static final String HAS_GUI     = "application.gui";
	public static boolean hasGui()
	{
		return System.getProperty(HAS_GUI, "true").equalsIgnoreCase("true");
	}
	public static void setGui(boolean hasGui)
	{
		System.setProperty(HAS_GUI, Boolean.toString(hasGui));
	}
	

	/*---------------------------------------------------
	** Constructors
	**---------------------------------------------------
	*/
	public Configuration()
	{
	}

	public Configuration(String filename)
	{
		load(filename);
	}

	/*---------------------------------------------------
	** Methods
	**---------------------------------------------------
	*/

	//////////////////////////////////////////////
	//// Instance
	//////////////////////////////////////////////
	public static Configuration getInstance(String confName)
	{
		Configuration conf = _instMap.get(confName);
		if ( conf == null )
		{
			_logger.warn("Can't find any configuration named '"+confName+"', creating a new one.");
			conf = new Configuration();
			_instMap.put(confName, conf);
		}
		return conf;
	}

	public static boolean hasInstance(String confName)
	{
		return _instMap.containsKey(confName);
//		return _instance != null;
	}

	public static void setInstance(String confName, Configuration configuration)
	{
//		_instance = configuration;
		configuration._confName = confName;
		_instMap.put(confName, configuration);
	}

//	public void addWriter(IAseTunePropsWriter writer)
//	{
//		_writers.add(writer);
//	}
//
//	public void removeWriter(IAseTunePropsWriter writer)
//	{
//		_writers.remove(writer);
//	}

	public String getConfName()
	{
		return _confName;
	}

	public String getFilename()
	{
		return _propFileName;
	}

	public void setFilename(String filename)
	{
		_propFileName = filename;
	}

	public String getEmbeddedMessage() {
		return _embeddedMessage;
	}

	public void setEmbeddedMessage(String embeddedMessage) {
		_embeddedMessage = embeddedMessage;
	}

	public void setSaveOnExit(boolean b)
	{
		_saveOnExit = b;
	}

	public void append(String str, String responsible)
	throws IOException
	{
		if (str == null)
			return;

		// DO NOT FORGET: APPEND MODE to the file
		FileOutputStream os = new FileOutputStream(new File(_propFileName), true);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, "8859_1"));

		if ( ! str.endsWith("\n") )
			str += "\n";
		
		bw.write("\n");
		bw.write("\n");
		bw.write("#--------------------------------------------------------------------\n");
		bw.write("# The below entries was Append at: "+new Date().toString()+"\n");
		bw.write("# By: "+responsible+"\n");
		bw.write("#--------------------------------------------------------------------\n");
		bw.write(str);
		bw.write("#--------------------------------------------------------------------\n");
		
		bw.flush();
		os.close();
	}

	public void setSaveEnable(boolean enable)
	{
		_saveIsEnabled = enable;
	}

	public void save()
	{
		save(false);
	}

	public void save(boolean withOverride)
	{
		if ( ! _saveIsEnabled && ! withOverride)
		{
			_logger.debug("Save is disabled for the configuration '"+getConfName()+"', which uses the file '"+getFilename()+"'.");
			return;
		}
		
		if (_propFileName == null)
		{
			_logger.debug("No filename has been assigned to this property file, can't save...");
			return;
		}

		try
		{
			FileOutputStream os = new FileOutputStream(new File(_propFileName));
			store(os, getEmbeddedMessage());
			//super.storeToXML(os, "This file will be overwritten and maintained by "+Version.getAppName();
			os.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void reload()
	{
		super.clear();
		load(_propFileName);
	}

	public void load()
	{
		load(_propFileName);
	}
	public void load(String filename)
	{
		setFilename(filename);

		if (filename == null)
		{
			_logger.warn("No config file was passed, filename=null, continuing anyway.");
			return;
		}

		try
		{
			FileInputStream in = new FileInputStream(filename);
			super.load(in);
			//super.loadFromXML(in);
			in.close();
		}
		catch (FileNotFoundException e)
		{
			_logger.warn("The file '"+filename+"' could not be loaded, continuing anyway.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}


	/**
	 * remove all keys that starts with the prefix
	 * @param prefix
	 */
	public void removeAll(String prefix)
	{
		for (Iterator<Object> it = this.keySet().iterator(); it.hasNext();)
		{
			String key = (String) it.next();
			if (key.startsWith(prefix))
			{
				it.remove();
			}
		}
	}

	/**
	 * Get the list of the keys contained in the configuration repository that
	 * match the specified prefix.
	 *
	 * @param prefix The property prefix to test against.
	 * @return An List of keys that match the prefix. If no keys was found, return a empty List object
	 */
	public List<String> getKeys(String prefix)
	{
		List<String> matchingKeys = new ArrayList<String>();

		for (Iterator<Object> it = this.keySet().iterator(); it.hasNext();)
		{
			String key = (String) it.next();
			if (prefix == null || key.startsWith(prefix))
			{
				matchingKeys.add(key);
			}
		}
		Collections.sort(matchingKeys);
		return matchingKeys;
	}
	public List<String> getKeys()
	{
		return getKeys(null);
	}

	/**
	 * Get the list of unique sub-keys contained in the configuration repository that
	 * match the specified prefix.
	 * <p>
	 * So if you had the following Configuration:
	 * <pre>
	 * udc.name1.prop1=value
	 * udc.name1.prop2=value
	 * udc.name2.prop1=value
	 * udc.name2.prop2=value
	 * udc.name3.prop1=value
	 * udc.name3.prop2=value
	 * udc.name3.prop3=value
	 * </pre>
	 * and called <code>conf.getUniqueSubKeys("udc.", true)</code>
	 * The return List would be: udc.name1, udc.name2, udc.name3
	 * <p>
	 * if the call would be <code>conf.getUniqueSubKeys("udc.", false)</code>
	 * The return List would be: name1, name2, name3
	 * <p>
	 * 
	 * @param prefix The property prefix to test against.
	 * @param keepPrefix should the return values consist of the prefix 
	 *        string + the next key value or just the key value itself.
	 * @return An List of keys that match the prefix. If no keys was found, return a empty List object
	 */
	public List<String> getUniqueSubKeys(String prefix, boolean keepPrefix)
	{
		List<String> uniqueNames = new ArrayList<String>();

		// Compose a list of unique prefix.xxxx. strings
		for (String key : getKeys(prefix))
		{
			String name;
			int start = keepPrefix ? 0 : prefix.length();
			int end   = key.indexOf(".", prefix.length());

			if ( end < 0)
				end = key.length();

			name = key.substring(start, end);

			if ( ! uniqueNames.contains(name) )
			{
				uniqueNames.add(name);
			}
		}

		Collections.sort(uniqueNames);
		return uniqueNames;
	}

	/**
	 * Return all *values* from the properties that starts with 'propPrefix' in a sorted list
	 * <p>
	 * <pre>
	 * PREFIX.UNIQUE_NAME_01=value1
	 * PREFIX.UNIQUE_NAME_02=value2
	 * PREFIX.UNIQUE_NAME_03=value3
	 * </pre>
	 * @return A LinkedHashMap of the above sorted on everything in the key
 	 */
//	public Map<String,String> getPropertyValuesInSortedList(String propPrefix)
//	{
//		LinkedHashMap<String,String> dest = new LinkedHashMap<String,String>();
//
//		// Put all the keys that starts with the prefix in a list
//		List<String> tmpKeyList = new LinkedList<String>();
//		for (String key : this.getKeys(propPrefix))
//		{
//			// Add KEY to tmpKeyList, this so we later can sort it and add
//			// values to the "real" check list in the "correct order"
//			_logger.debug("tmpKeyList.add: key='"+key+"'.");
//			tmpKeyList.add(key);
//		}
//
//		// Get the keys, sort them, add them in a sorted manner to the list: destList
//		if (tmpKeyList.size() > 0)
//		{
//			Collections.sort(tmpKeyList);
//			for (String key : tmpKeyList)
//			{
//				String val = this.getProperty(key);
//
//				String keyLastWord = key.substring( key.lastIndexOf('.')+1 );
//				//val = keyLastWord + ":" + val;
//
//				// Now add the value to the check list.
//				_logger.debug("destList.add: val='"+val+"'.");
//				dest.put(keyLastWord, val);
//			}
//		}
//
//		return dest;
//	}


	/** Does the property exists within the configuration ? */
	public boolean hasProperty(String propName)
	{
		return getProperty(propName) != null;
	}

	/** Get a int value for property */
	public int getIntMandatoryProperty(String propName)
	throws MandatoryPropertyException
	{
		String val = getProperty(propName);
		if (val == null)
			throw new MandatoryPropertyException("The property '"+propName+"' is mandatory.");
		try
		{
			return Integer.parseInt(val);
		}
		catch (NumberFormatException e)
		{
			throw new NumberFormatException("The property '"+propName+"' must be a number. I found value '"+val+"'.");
		}
	}
	/** Get a int value for property */
	public int getIntProperty(String propName)
	{
		String val = getProperty(propName);
		return Integer.parseInt(val);
	}
	/** Get a int value for property */
	public int getIntProperty(String propName, int defaultValue)
	{
		return getIntProperty(propName, Integer.toString(defaultValue));
	}
	/** Get a int value for property */
	public int getIntProperty(String propName, String defaultValue)
	{
		String val = getProperty(propName, defaultValue);
		if (val != null && val.equals(""))
			val = defaultValue;
		return Integer.parseInt(val);
	}




	/** Get a long value for property */
	public long getLongMandatoryProperty(String propName)
	throws MandatoryPropertyException
	{
		String val = getProperty(propName);
		if (val == null)
			throw new MandatoryPropertyException("The property '"+propName+"' is mandatory.");
		return Long.parseLong(val);
	}
	/** Get a long value for property */
	public long getLongProperty(String propName)
	{
		String val = getProperty(propName);
		return Long.parseLong(val);
	}
	/** Get a long value for property */
	public long getLongProperty(String propName, long defaultValue)
	{
		return getLongProperty(propName, Long.toString(defaultValue));
	}
	/** Get a long value for property */
	public long getLongProperty(String propName, String defaultValue)
	{
		String val = getProperty(propName, defaultValue);
		if (val != null && val.equals(""))
			val = defaultValue;
		return Long.parseLong(val);
	}




	/** Get a boolean value for property */
	public boolean getBooleanMandatoryProperty(String propName)
	throws MandatoryPropertyException
	{
		String val = getProperty(propName);
		if (val == null)
			throw new MandatoryPropertyException("The property '"+propName+"' is mandatory.");
		return val.equalsIgnoreCase("true");
	}
	/** Get a boolean value for property */
	public boolean getBooleanProperty(String propName, boolean defaultValue)
	{
		String val = getProperty(propName, Boolean.toString(defaultValue));
		if (val == null)
			return false;
		return val.equalsIgnoreCase("true");
	}
	/** Get a boolean value for property */
	public boolean getBooleanProperty(String propName, String defaultValue)
	{
		String val = getProperty(propName, defaultValue);
		if (val == null)
			return false;
		return val.equalsIgnoreCase("true");
	}



	/** Get a String value for property */
	public String getMandatoryProperty(String propName)
	throws MandatoryPropertyException
	{
		String val = getProperty(propName);
		if (val == null)
			throw new MandatoryPropertyException("The property '"+propName+"' is mandatory.");
		return val;
	}

	/** Get a String value for property, trim() has been called on it, if the property can't be found null will be returned */
	@Override
	public String getProperty(String propName)
	{
		String val = super.getProperty(propName);
		if (val == null)
		{
			// If the propName wasn't found in the properties.
			// Get the Registered Application Default 
			val = getRegisteredDefaultValue(propName);
		}
		else
		{
			val = val.trim();
			
			// get/extract USE_DEFAULT values
			val = getUseDefaultValue(propName, val);
		}
		return parseProperty( propName, val );
	}

	/** Get a String value for property */
	@Override
	public String getProperty(String propName, String defaultValue)
	{
		String val = getProperty(propName);
		return val != null ? val : defaultValue;
	}

	
	
	/** Get a String value for property */
	public String getMandatoryPropertyRaw(String propName)
	throws MandatoryPropertyException
	{
		String val = getPropertyRaw(propName);
		if (val == null)
			throw new MandatoryPropertyException("The property '"+propName+"' is mandatory.");
		return val;
	}

	/** Get a String value for property */
	public String getPropertyRaw(String propName)
	{
		String val = super.getProperty(propName);
		if (val != null)
		{
			val = val.trim();
			
			// get/extract USE_DEFAULT values
			val = getUseDefaultValue(propName, val);
		}
		return val;
	}

	/** Get a String value for property */
	public String getPropertyRaw(String propName, String defaultValue)
	{
		String val = getPropertyRaw(propName);
		return val != null ? val : defaultValue;
	}

	/** just for testing: Used to get RAW property values */
	private String getPropertyRaw_test(String propName)
	{
		String val = super.getProperty(propName);
		if (val != null)
			val = val.trim();
		return val;
	}

	
	
	/** Get a String value for property, just use the Properties.getProperty (no str.trim(), no nothing ) */
	public String getMandatoryPropertyRawVal(String propName)
	throws MandatoryPropertyException
	{
		String val = getPropertyRawVal(propName);
		if (val == null)
			throw new MandatoryPropertyException("The property '"+propName+"' is mandatory.");
		return val;
	}

	/** Get a String value for property, just use the Properties.getProperty (no str.trim(), no nothing ) */
	public String getPropertyRawVal(String propName)
	{
		return super.getProperty(propName);
	}

	/** Get a String value for property, just use the Properties.getProperty (no str.trim(), no nothing ) */
	public String getPropertyRawVal(String propName, String defaultValue)
	{
		String val = getPropertyRawVal(propName);
		return val != null ? val : defaultValue;
	}

	
	/** 
	 * Encrypt a property value, most possibly a password.
	 * 
	 * @param propName Name of the property string to be used in the property file.
	 * @param str      The string you want to encrypt
	 * @return         The encrypted string to be stored in a property file.
	 *                 The returned string i prefixed with 'encrypted:' which is used by 
	 *                 the property reader to determine that this property needs to be decrypted.
	 */
	public static String encryptPropertyValue(String propName, String str)
	{
		if (str == null)
			return null;

		Encrypter propEncrypter = new Encrypter(encrypterBaseKey+propName);
		String encryptedStr = propEncrypter.encrypt(str);
		return ENCRYPTED_PREFIX + encryptedStr;
	}

	/** 
	 * Decrypt a property value, most possibly a password.
	 * 
	 * @param propName Name of the property string to be used in the property file.
	 * @param str      The string you want to decrypt (must starts with "encrypted:")
	 * @return         The decrypted string 
	 */
	public static String decryptPropertyValue(String propName, String str)
	{
		if (str == null)
			return null;

		if (str.startsWith(ENCRYPTED_PREFIX))
		{
			str = str.substring( ENCRYPTED_PREFIX.length() );
    
    		Encrypter encrypter = new Encrypter(encrypterBaseKey+propName);
    		str = encrypter.decrypt(str);
		}
		return str;
	}
	
	/**
	 * Check if the value starts with "encrypted:"
	 * @param str
	 * @return
	 */
	public static boolean isEncryptedValue(String str)
	{
		if (str == null)
			return false;

		return str.startsWith(ENCRYPTED_PREFIX);
	}

//	public Object setEncrypedProperty(String propName, String str)
//	{
//		return super.setProperty( propName, encryptPropertyValue(propName, str) );
//	}

//	@Override
//	public Object setProperty(String propName, String str)
//	{
//		return super.setProperty( propName, str );
//	}
	@Override
	public Object setProperty(String propName, String str)
	{
		return setProperty(propName, str, false);
	}

	public Object setProperty(String propName, String str, boolean encryptPropertyValue)
	{
		if (str != null && encryptPropertyValue)
		{
			str = encryptPropertyValue(propName, str);			
		}
		// If we have a registered default value for this property
		// and the passed value is same as the Application Default value 
		// add 'USE_DEFAULT:' as a prefix to the value
		//
		// When reading the property using getProperty(...) we now have the ability to change
		// a saved property value and change it to the application current default values (if it changes in later releases)
		// SO: Even if the saved property value and the appDefault is not the same.
		// We will still use the latest application default value...
		//
		// Saving the stored value 'USE_DEFAULT:*value*' is just a precaution, meaning:
		// If the we delete aRregistered Application Default from the application
		// we could still use the stored *value* in the saved property file.
		//
		String regDefVal = getRegisteredDefaultValue(propName);
		if (regDefVal != null)
		{
			if (regDefVal.equals(str))
				str = USE_DEFAULT + str;
		}

		// set the property in super object
		Object prev = super.setProperty( propName, str );
		
		// If the previously stored value, is having 'USE_DEFAULT:' as a prefix
		// simply remove it and return the *actual* value it previously had.
		if (prev != null && prev instanceof String)
		{
			String prevStr = (String) prev;
			if (prevStr.startsWith(USE_DEFAULT))
				prev = prevStr.substring(USE_DEFAULT.length());
		}
		return prev;
	}

	public int setProperty(String propName, int t)
	{
		Object prev = setProperty( propName, Integer.toString(t) );
		return prev==null ? -1 : parseInt( (String)prev );
	}

	public long setProperty(String propName, long l)
	{
		Object prev = setProperty( propName, Long.toString(l) );
		return prev==null ? -1 : parseLong( (String)prev );
	}

	public boolean setProperty(String propName, boolean b)
	{
		Object prev = setProperty( propName, Boolean.toString(b) );
		return prev==null ? false : parseBoolean( (String)prev );
	}
	private int     parseInt(String str)     {try {return Integer.parseInt(str);}     catch(Throwable e) {return 0;}}
	private long    parseLong(String str)    {try {return Long.parseLong(str);}       catch(Throwable e) {return 0;}}
	private boolean parseBoolean(String str) {try {return Boolean.parseBoolean(str);} catch(Throwable e) {return false;}}

//			public static final String USE_DEFAULT = "USE_DEFAULT:";
//			//-----------------------
//			// with application defaults, example: propname = USE_DEFAULT:value
//			//-----------------------
//			public Object setProperty(String propName, String str, String appDefault)
//			{
//				if (str.equals(appDefault))
//					str = USE_DEFAULT + str;
//				return super.setProperty( propName, str );
//			}
//		
//			public int setProperty(String propName, int t, int appDefault)
//			{
//				String str = Integer.toString(t);
//				if (t == appDefault)
//					str = USE_DEFAULT + t;
//				Object prev = setProperty( propName, str );
//				if (prev != null && prev instanceof String)
//					if (((String) prev).startsWith(USE_DEFAULT))
//						prev = ((String) prev).substring(USE_DEFAULT.length());
//				return prev==null ? -1 : Integer.parseInt( (String)prev );
//			}
//		
//			public long setProperty(String propName, long l, long appDefault)
//			{
//				String str = Long.toString(l);
//				if (l == appDefault)
//					str = USE_DEFAULT + l;
//				Object prev = setProperty( propName, str );
//				if (prev != null && prev instanceof String)
//					if (((String) prev).startsWith(USE_DEFAULT))
//						prev = ((String) prev).substring(USE_DEFAULT.length());
//				return prev==null ? -1 : Long.parseLong( (String)prev );
//			}
//		
//			public boolean setProperty(String propName, boolean b, boolean appDefault)
//			{
//				String str = Boolean.toString(b);
//				if (b == appDefault)
//					str = USE_DEFAULT + b;
//				Object prev = setProperty( propName, str );
//				if (prev != null && prev instanceof String)
//					if (((String) prev).startsWith(USE_DEFAULT))
//						prev = ((String) prev).substring(USE_DEFAULT.length());
//				return prev==null ? false : Boolean.parseBoolean( (String)prev );
//			}

	/** prefix for DEFAULT vales */
	private static final String USE_DEFAULT = "USE_DEFAULT:";
	/** What are the default vales for: each registered property name */

	private static HashMap<String, String> _registeredDefault = new HashMap<String, String>(); 

	public static void registerDefaultValue(String propName, String defaultValue)
	{
		_registeredDefault.put(propName, defaultValue);
	}

	public static void registerDefaultValue(String propName, int defaultValue)
	{
		registerDefaultValue(propName, Integer.toString(defaultValue));
	}

	public static void registerDefaultValue(String propName, long defaultValue)
	{
		registerDefaultValue(propName, Long.toString(defaultValue));
	}

	public static void registerDefaultValue(String propName, boolean defaultValue)
	{
		registerDefaultValue(propName, Boolean.toString(defaultValue));
	}

	public static boolean hasRegisteredDefaultValue(String propName)
	{
		return _registeredDefault.containsKey(propName);
	}

	public static String getRegisteredDefaultValue(String propName)
	{
		return _registeredDefault.get(propName);
	}

	/**
	 * If the input <code>value</code> has a <code>'USE_DEFAULT:'</code> prefix.<br>
	 * Change the value to the Application Registered Default Value<br>
	 * If the <code>value</code> has a <code>'USE_DEFAULT:'</code> prefix but not any registered default value
	 * simply take away the <code>'USE_DEFAULT:'</code> prefix and return the value.<br>
	 * 
	 * @param propName name of the property to check for default values
	 * @param value the value got from the Properties
	 * @return see above.
	 */
	public static String getUseDefaultValue(String propName, String value)
	{
		// get registered default value
		String regDefVal = getRegisteredDefaultValue(propName);
		
		// if the passed value string is not null and start with a USE_DEFAULT string
		if (value != null && value.startsWith(USE_DEFAULT))
		{
			// if a default value has been registered, use this valie
			if (regDefVal != null)
				return regDefVal;

			// If no default value has been registered
			// and the value string starts with USE_DEFAULT:
			// extract the value after the keyword 'USE_DEFAULT:'
			// This is used as a fallback strategy
			value = value.substring(USE_DEFAULT.length()).trim();
			
//			// if value is empty: I think Properties.getProperty(...) returns "" if empty value
//			if (value.length() == 0)
//				return ""; 
//				//return null;
		}

		return value;
	}

	
	/**
	 * Interrogate a property value to check if there are any extra actions
	 * we need to take.
	 * A property value starting with:
	 * oscmd:   executes a Operating System command
	 * oscmd-n: executes a Operating System command and strips of all new lines
	 * prop:   just reads a dependant property value...
	 * @param val
	 * @return
	 */
	public String parseProperty(String propName, String val)
	{
		if (val == null)
			return null;

		// Extract Environment variables
		// search for ${ENV_NAME}
		Pattern compiledRegex = Pattern.compile("\\$\\{.*\\}");
		while( compiledRegex.matcher(val).find() )
		{
			String envVal  = null;
			String envName = val.substring( val.indexOf("${")+2, val.indexOf("}") );

			// Get value for a specific env variable
			// But some java runtimes does not do getenv(),
			// then we need to revert back to getProperty() from the system property
			// then the user needs to pass that as a argument -Dxxx=yyy to the JVM
			try
			{
				envVal  = System.getenv(envName);
			}
			catch (Throwable t)
			{
				envVal = System.getProperty(envName);
				if (envVal == null)
				{
					_logger.warn("System.getenv(): Is not supported on this platform or version of Java. Please pass '-D"+envName+"=value' when starting the JVM.");
				}
			}
			if (envVal == null)
			{
				_logger.warn("The Environment variable '"+envName+"' can't be found, replacing it with an empty string ''.");
				envVal="";
			}
			// Backslashes does not work that good in replaceFirst()...
			// So change them to / instead...
			envVal = envVal.replace('\\', '/');

			_logger.debug("The Environment variable '"+envName+"' will be substituted with the value of '"+envVal+"'.");

			// NOW substitute the ENVVARIABLE with a real value...
			val = val.replaceFirst("\\$\\{"+envName+"\\}", envVal);
		}

		// Get the value from another property
		if (val.startsWith("prop:"))
		{
			val = getProperty( val.substring( "prop:".length() ) );
		}

		// Get the value from another property
		if (val.startsWith(ENCRYPTED_PREFIX))
		{
			val = val.substring( ENCRYPTED_PREFIX.length() );

			Encrypter propEncrypter = new Encrypter(encrypterBaseKey+propName);
			String decryptedStr = propEncrypter.decrypt(val);
			val = decryptedStr;
		}

		// Execute a Operating system command to get the value...
		if (val != null)
		{
			if (val.startsWith("oscmd:"))
			{
				val = osCmd( val.substring( "oscmd:".length() ), false );
			}
			else if (val.startsWith("oscmd-n:"))
			{
				val = osCmd( val.substring( "oscmd-n:".length() ), true );
			}
		}

		return val;
	}

	private String osCmd(String osCmdStr, boolean discardNewlines)
	{
		try
		{
			OSCommand osCmd = OSCommand.execute(osCmdStr);
			String retVal = osCmd.getOutput();

			if (discardNewlines)
			{
				retVal = retVal.replaceAll("\r", "");
				retVal = retVal.replaceAll("\n", "");
			}

			return retVal;
		}
		catch(IOException e)
		{
			_logger.error("Problems when executing the OS Command '"+osCmdStr+"'. Caught: "+e);
			return e.toString();
		}

	}




	// Hopefully this is kicked of when the JVM dies aswell...
	@Override
	protected void finalize() throws Throwable
	{
	    super.finalize();

		if (_saveOnExit)
		    save();
	}





	/////////////////////////////////////////////////////////////
	// code stolen from Properties
	/////////////////////////////////////////////////////////////

	/** code stolen from Properties */
	@Override
	public synchronized void store(OutputStream outputstream, String s)
	throws IOException
	{
//		if ( ! _saveOnExit )
//			return;

		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(outputstream, "8859_1"));
		writeln(bw, "#=======================================================");
		if (s != null)
		{
		writeln(bw, "# " + s);
		}
		writeln(bw, "# Last save time: "+new Date().toString());
		writeln(bw, "#-------------------------------------------------------");
		writeln(bw, "");

		String sectionStr = "";
		String sectionStrSave = "";

		bw.flush();
		boolean escUnicode = true;
		synchronized (this)
		{
			SortedMap<Object, Object> aSortedOne = new TreeMap<Object, Object>(this);
			for (Iterator<Object> it=aSortedOne.keySet().iterator(); it.hasNext();)
			{
				String key = (String) it.next();
				String val = (String) get(key);

				key = saveConvert(key, true, escUnicode);

				// Write a new-line after every property "group"
				int end   = key.indexOf(".");
				if ( end < 0)
					end = key.length();

				sectionStr = key.substring(0, end);
				if ( ! sectionStr.equals(sectionStrSave))
				{
					writeln(bw, "");
					sectionStrSave = sectionStr;
				}

				/*
				 * No need to escape embedded and trailing spaces for value,
				 * hence pass false to flag.
				 */
				val = saveConvert(val, false, escUnicode);
				bw.write(key + "=" + val);
				bw.newLine();
			}
			
//			for (Enumeration<Object> e = keys(); e.hasMoreElements();)
//			{
//				String key = (String) e.nextElement();
//				String val = (String) get(key);
//				key = saveConvert(key, true, escUnicode);
//				/*
//				 * No need to escape embedded and trailing spaces for value,
//				 * hence pass false to flag.
//				 */
//				val = saveConvert(val, false, escUnicode);
//				bw.write(key + "=" + val);
//				bw.newLine();
//
//				// Write a new-line after every property "group"
//				sectionStr = key.substring(0, key.indexOf("."));
//				if ( ! sectionStr.equals(sectionStrSave))
//				{
//					writeln(bw, "");
//					sectionStrSave = sectionStr;
//				}
//			}
		}
		bw.flush();
	}

	/** code stolen from Properties */
	private static void writeln(BufferedWriter bufferedwriter, String s) throws IOException
	{
		bufferedwriter.write(s);
		bufferedwriter.newLine();
	}


	/** code stolen from Properties */
	private static final char	hexDigit[]	     = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
	private static char toHex(int i)
	{
		return hexDigit[i & 15];
	}

	/**
	 * Converts unicodes to encoded &#92;uxxxx and escapes special characters
	 * with a preceding slash
	 * 
	 * @param theString      input string to convert
	 * @param escapeSpace    should spaces have a extra backslash in front of it
	 * @param escapeUnicode  escape unicode characters
	 * @return
	 */
	public static String saveConvert(String theString, boolean escapeSpace, boolean escapeUnicode)
	{
		int len = theString.length();
		int bufLen = len * 2;
		if ( bufLen < 0 )
		{
			bufLen = Integer.MAX_VALUE;
		}
		StringBuffer outBuffer = new StringBuffer(bufLen);

		for (int x = 0; x < len; x++)
		{
			char aChar = theString.charAt(x);
			// Handle common case first, selecting largest block that
			// avoids the specials below
			if ( (aChar > 61) && (aChar < 127) )
			{
				if ( aChar == '\\' )
				{
					outBuffer.append('\\');
					outBuffer.append('\\');
					continue;
				}
				outBuffer.append(aChar);
				continue;
			}
			switch (aChar)
			{
			case ' ':
				if ( x == 0 || escapeSpace )
					outBuffer.append('\\');
				outBuffer.append(' ');
				break;
			case '\t':
				outBuffer.append('\\');
				outBuffer.append('t');
				break;
			case '\n':
				outBuffer.append('\\');
				outBuffer.append('n');
				break;
			case '\r':
				outBuffer.append('\\');
				outBuffer.append('r');
				break;
			case '\f':
				outBuffer.append('\\');
				outBuffer.append('f');
				break;
			case '=': // Fall through
			case ':': // Fall through
			case '#': // Fall through
			case '!':
				outBuffer.append('\\');
				outBuffer.append(aChar);
				break;
			default:
				if ( ((aChar < 0x0020) || (aChar > 0x007e)) & escapeUnicode )
				{
					outBuffer.append('\\');
					outBuffer.append('u');
					outBuffer.append(toHex((aChar >> 12) & 0xF));
					outBuffer.append(toHex((aChar >> 8) & 0xF));
					outBuffer.append(toHex((aChar >> 4) & 0xF));
					outBuffer.append(toHex(aChar & 0xF));
				}
				else
				{
					outBuffer.append(aChar);
				}
			}
		}
		return outBuffer.toString();
	}

	//--------------------------------------------------------------------------
	//--------------------------------------------------------------------------
	// BEGIN: Combined search
	//--------------------------------------------------------------------------
	//--------------------------------------------------------------------------
	private static class CombinedConfiguration
	extends Configuration
	{
		private static final long	serialVersionUID	= 1L;

		//------------------------------------------
		// The below will "might" be supported in the future
		//------------------------------------------
		@Override
		public Enumeration<?> propertyNames()
		{
			throw new RuntimeException("propertyNames() operation is not supported on the Combined Configuration, this has to be done on the individual Configurations.");
		}

		@Override
		public synchronized boolean isEmpty()
		{
			throw new RuntimeException("isEmpty() operation is not supported on the Combined Configuration, this has to be done on the individual Configurations.");
		}

		@Override
		public synchronized Enumeration<Object> keys()
		{
			throw new RuntimeException("keys() operation is not supported on the Combined Configuration, this has to be done on the individual Configurations.");
		}

		@Override
		public synchronized Enumeration<Object> elements()
		{
			throw new RuntimeException("elements() operation is not supported on the Combined Configuration, this has to be done on the individual Configurations.");
		}

		@Override
		public synchronized boolean contains(Object value)
		{
			throw new RuntimeException("contains() operation is not supported on the Combined Configuration, this has to be done on the individual Configurations.");
		}

		@Override
		public boolean containsValue(Object value)
		{
			throw new RuntimeException("containsValue() operation is not supported on the Combined Configuration, this has to be done on the individual Configurations.");
		}

		@Override
		public synchronized boolean containsKey(Object key)
		{
			throw new RuntimeException("containsKey() operation is not supported on the Combined Configuration, this has to be done on the individual Configurations.");
		}

		@Override
		public Set<Object> keySet()
		{
			throw new RuntimeException("keySet() operation is not supported on the Combined Configuration, this has to be done on the individual Configurations.");
		}

		@Override
		public Set<Entry<Object, Object>> entrySet()
		{
			throw new RuntimeException("entrySet() operation is not supported on the Combined Configuration, this has to be done on the individual Configurations.");
		}

		@Override
		public Collection<Object> values()
		{
			throw new RuntimeException("values() operation is not supported on the Combined Configuration, this has to be done on the individual Configurations.");
		}

		@Override
		public synchronized int size()
		{
			throw new RuntimeException("size() operation is not supported on the Combined Configuration, this has to be done on the individual Configurations.");
		}
		
		
		//------------------------------------------
		// The below will "never" be supported
		//------------------------------------------

		@Override
		public void save()
		{
			throw new RuntimeException("save() operation is not supported on the Combined Configuration, this has to be done on the individual Configurations.");
		}
		
//		@Override
//		public Object setEncrypedProperty(String propName, String str)
//		{
//			throw new RuntimeException("setEncrypedProperty() operation is not supported on the Combined Configuration, this has to be done on the individual Configurations.");
//		}

		@Override
		public Object setProperty(String propName, String str)
		{
			throw new RuntimeException("setProperty() operation is not supported on the Combined Configuration, this has to be done on the individual Configurations.");
		}

		@Override
		public int setProperty(String propName, int t)
		{
			throw new RuntimeException("setProperty() operation is not supported on the Combined Configuration, this has to be done on the individual Configurations.");
		}

		@Override
		public long setProperty(String propName, long l)
		{
			throw new RuntimeException("setProperty() operation is not supported on the Combined Configuration, this has to be done on the individual Configurations.");
		}

		@Override
		public boolean setProperty(String propName, boolean b)
		{
			throw new RuntimeException("setProperty() operation is not supported on the Combined Configuration, this has to be done on the individual Configurations.");
		}

//				@Override
//				public Object setProperty(String propName, String str, String appDefault)
//				{
//					throw new RuntimeException("setProperty() operation is not supported on the Combined Configuration, this has to be done on the individual Configurations.");
//				}
//		
//				@Override
//				public int setProperty(String propName, int t, int appDefault)
//				{
//					throw new RuntimeException("setProperty() operation is not supported on the Combined Configuration, this has to be done on the individual Configurations.");
//				}
//		
//				@Override
//				public long setProperty(String propName, long l, long appDefault)
//				{
//					throw new RuntimeException("setProperty() operation is not supported on the Combined Configuration, this has to be done on the individual Configurations.");
//				}
//		
//				@Override
//				public boolean setProperty(String propName, boolean b, boolean appDefault)
//				{
//					throw new RuntimeException("setProperty() operation is not supported on the Combined Configuration, this has to be done on the individual Configurations.");
//				}

		@Override
		public void setFilename(String filename)
		{
			throw new RuntimeException("setFilename() operation is not supported on the Combined Configuration, this has to be done on the individual Configurations.");
		}

		@Override
		public String getEmbeddedMessage() 
		{
			throw new RuntimeException("getEmbeddedMessage() operation is not supported on the Combined Configuration, this has to be done on the individual Configurations.");
		}

		@Override
		public void setEmbeddedMessage(String embeddedMessage) 
		{
			throw new RuntimeException("setEmbeddedMessage() operation is not supported on the Combined Configuration, this has to be done on the individual Configurations.");
		}

		@Override
		public void setSaveOnExit(boolean b)
		{
			throw new RuntimeException("setSaveOnExit() operation is not supported on the Combined Configuration, this has to be done on the individual Configurations.");
		}

		@Override
		public void append(String str, String responsible)
		throws IOException
		{
			throw new RuntimeException("append() operation is not supported on the Combined Configuration, this has to be done on the individual Configurations.");
		}

		@Override
		public void removeAll(String prefix)
		{
			throw new RuntimeException("removeAll() operation is not supported on the Combined Configuration, this has to be done on the individual Configurations.");
		}

		@Override
		public String getFilename()
		{
			String filenames = "";
			for (String instName : _searchOrder)
			{
				Configuration conf = Configuration.getInstance(instName);
				if (conf != null)
					filenames += conf.getFilename() + ", ";
			}
			// get rid of last ", " if any
			if (filenames.endsWith(", "))
				filenames = filenames.substring(0, filenames.length() - 2);

			return "Combined Configuration of files: "+filenames;
		}

		/** Does the property exists within any of the configurations ? */
		@Override
		public boolean hasProperty(String propName)
		{
			for (String instName : _searchOrder)
			{
				Configuration conf = Configuration.getInstance(instName);
				if (conf != null && conf.hasProperty(propName))
					return true;
			}
			return false;
		}

		/**
		 * Get the list of the keys contained in the configuration repository that
		 * match the specified prefix.
		 *
		 * @param prefix The property prefix to test against.
		 * @return An List of keys that match the prefix. If no keys was found, return a empty List object
		 */
		@Override
		public List<String> getKeys(String prefix)
		{
			List<String> matchingKeys = new ArrayList<String>();

			for (String instName : _searchOrder)
			{
				Configuration conf = Configuration.getInstance(instName);
				if (conf != null)
				{
					for (Iterator<Object> it = conf.keySet().iterator(); it.hasNext();)
					{
						String key = (String) it.next();
						if (prefix == null || key.startsWith(prefix))
						{
							if ( ! matchingKeys.contains(key) )
								matchingKeys.add(key);
						}
					}					
				}
			}

			Collections.sort(matchingKeys);
			return matchingKeys;
		}
		@Override
		public List<String> getKeys()
		{
			return getKeys(null);
		}

		/**
		 * Get the list of unique sub-keys contained in the configuration repository that
		 * match the specified prefix.
		 * <p>
		 * So if you had the following Configuration:
		 * <pre>
		 * udc.name1.prop1=value
		 * udc.name1.prop2=value
		 * udc.name2.prop1=value
		 * udc.name2.prop2=value
		 * udc.name3.prop1=value
		 * udc.name3.prop2=value
		 * udc.name3.prop3=value
		 * </pre>
		 * and called <code>conf.getUniqueSubKeys("udc.", true)</code>
		 * The return List would be: udc.name1, udc.name2, udc.name3
		 * <p>
		 * if the call would be <code>conf.getUniqueSubKeys("udc.", false)</code>
		 * The return List would be: name1, name2, name3
		 * <p>
		 * 
		 * @param prefix The property prefix to test against.
		 * @param keepPrefix should the return values consist of the prefix 
		 *        string + the next key value or just the key value itself.
		 * @return An List of keys that match the prefix. If no keys was found, return a empty List object
		 */
		@Override
		public List<String> getUniqueSubKeys(String prefix, boolean keepPrefix)
		{
			List<String> uniqueNames = new ArrayList<String>();

			for (String instName : _searchOrder)
			{
				Configuration conf = Configuration.getInstance(instName);
				if (conf != null)
				{
					// Compose a list of unique prefix.xxxx. strings
					for (String key : conf.getKeys(prefix))
					{
						String name;
						int start = keepPrefix ? 0 : prefix.length();
						int end   = key.indexOf(".", prefix.length());

						if ( end < 0)
							end = key.length();

						name = key.substring(start, end);
		
						if ( ! uniqueNames.contains(name) )
						{
							uniqueNames.add(name);
						}
					}
				}
			}

			Collections.sort(uniqueNames);
			return uniqueNames;
		}

		//---------------------------------------------------------------
		// INT methods
		// LONG methods
		// BOOLEAN methods
		//---------------------------------------------------------------
		// The above methods are ALL using String getProperty() methods
		// so we do not need to override those

		//---------------------------------------------------------------
		// String methods
		//---------------------------------------------------------------
		@Override
		public String getMandatoryProperty(String propName)
		throws MandatoryPropertyException
		{
			String val = getProperty(propName);
			if (val == null)
				throw new MandatoryPropertyException("The property '"+propName+"' is mandatory.");
			return val;
		}

		/** Get a String value for property, trim() has been called on it, if the property can't be found null will be returned */
		@Override
		public String getProperty(String propName)
		{
			for (String instName : _searchOrder)
			{
				Configuration conf = Configuration.getInstance(instName);
				if (conf != null)
				{
					// In the combined props, we need to check if the prop *exists*
					// Because the Configuration.getProperty(propName) will return the
					// registered default value if prop was not found...
					// so val will contain the default value, and we want to check other configs
					// in the search order to get value...
					if ( ! conf.containsKey(propName) )
						continue;

					String val = conf.getProperty(propName);
					if (val != null)
						return val;
				}
			}
			return getRegisteredDefaultValue(propName);
		}


		/** Get a String value for property */
		@Override
		public String getProperty(String propName, String defaultValue)
		{
			String val = getProperty(propName);
			return val != null ? val : defaultValue;
		}



		//---------------------------------------------------------------
		// RAW String methods
		//---------------------------------------------------------------
		/** Get a String value for property */
		@Override
		public String getMandatoryPropertyRaw(String propName)
		throws MandatoryPropertyException
		{
			String val = getPropertyRaw(propName);
			if (val == null)
				throw new MandatoryPropertyException("The property '"+propName+"' is mandatory.");
			return val;
		}

		/** Get a String value for property */
		@Override
		public String getPropertyRaw(String propName)
		{
			for (String instName : _searchOrder)
			{
				Configuration conf = Configuration.getInstance(instName);
				if (conf != null)
				{
					String val = conf.getPropertyRaw(propName);
					if (val != null)
						return val;
				}
			}
			return null;
		}

		/** Get a String value for property */
		@Override
		public String getPropertyRaw(String propName, String defaultValue)
		{
			String val = getPropertyRaw(propName);
			return val != null ? val : defaultValue;
		}

		//---------------------------------------------------------------
		// RAWRAW String methods
		//---------------------------------------------------------------
		/** Get a String value for property */
		@Override
		public String getMandatoryPropertyRawVal(String propName)
		throws MandatoryPropertyException
		{
			String val = getPropertyRawVal(propName);
			if (val == null)
				throw new MandatoryPropertyException("The property '"+propName+"' is mandatory.");
			return val;
		}

		/** Get a String value for property */
		@Override
		public String getPropertyRawVal(String propName)
		{
			for (String instName : _searchOrder)
			{
				Configuration conf = Configuration.getInstance(instName);
				if (conf != null)
				{
					String val = conf.getPropertyRawVal(propName);
					if (val != null)
						return val;
				}
			}
			return null;
		}

		/** Get a String value for property */
		@Override
		public String getPropertyRawVal(String propName, String defaultValue)
		{
			String val = getPropertyRawVal(propName);
			return val != null ? val : defaultValue;
		}
	}

	/** Hold the specified search order for the Combined Configuration */
	private static String[] _searchOrder = new String[] {USER_TEMP, USER_CONF, SYSTEM_CONF};
	/**
	 * Set the search order for the Combined Configuration
	 * @param searchOrder
	 */
	public static void setSearchOrder(String... searchOrder)
	{
		_searchOrder = searchOrder;
	}
	/**
	 * Get the search order for the Combined Configuration
	 * @return String array of configured search order, if none is set an empty array will be returned
	 */
	public static String[] getSearchOrder()
	{
		return _searchOrder;
	}

	/** Holds the Combined Configuration Object */
	private static Configuration _combinedConfig = new CombinedConfiguration();

	/**
	 * Get a Configuration that searches all Configurations that has been set with the setSearchOrder()
	 * @return will never return null
	 */
	public static Configuration getCombinedConfiguration()
	{
		return _combinedConfig;
	}

	/**
	 * Parse a string looking like <code>key1=val, key2=val,key3=val</code>
	 * @param parseStr input string to parse
	 * @return a Configuration object
	 * @throws ParseException
	 */
	public static Configuration parse(String parseStr)
	throws ParseException
	{
		return parse(parseStr, ",");
	}

	/**
	 * Parse a string looking like <code>key1=val&lt;delimiter&gt; key2=val&lt;delimiter&gt;key3=val</code>
	 * @param parseStr input string to parse
	 * @param delimiter character(s) used between two parameters
	 * @return a Configuration object
	 * @throws ParseException
	 */
	public static Configuration parse(String parseStr, String delimiter)
	throws ParseException
	{
		Configuration conf = new Configuration();
		String[] strArr = parseStr.split(delimiter);
		for (int i=0; i<strArr.length; i++)
		{
			strArr[i] = strArr[i].trim();

			_logger.trace("parse() strArr["+i+"]='"+strArr[i]+"'.");

			String[] strKeyVal = strArr[i].split("=");
			if (strKeyVal.length < 2)
				throw new ParseException("Faulty key=value representation '"+strArr[i]+"' at position '"+i+"' in the string '"+strArr[i]+"'.", i);

			String key = strKeyVal[0].trim();
			String val = strKeyVal[1].trim();


			// now STORE the value...
			conf.put(key, val);
		}

		return conf;
	}
	
	/**
	 * Add the input configuration to the current object.
	 * <p>
	 * If the key already exists in "this" object, then it will be overwritten by the input Configuration
	 * @param conf
	 */
	public void add(Configuration conf)
	{
		putAll(conf);
	}


	
	//--------------------------------------------------------------------------
	//--------------------------------------------------------------------------
	// main / test code.
	//--------------------------------------------------------------------------
	//--------------------------------------------------------------------------

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		Configuration tConf = new Configuration();
		Configuration uConf = new Configuration();
		Configuration sConf = new Configuration();
		
		tConf.setFilename("c:\\tmpConfigFile.prop");
		uConf.setFilename("c:\\userConfigFile.prop");
		sConf.setFilename("c:\\systemConfigFile.prop");

		Configuration.setInstance(Configuration.USER_TEMP,   tConf);
		Configuration.setInstance(Configuration.USER_CONF,   uConf);
		Configuration.setInstance(Configuration.SYSTEM_CONF, sConf);

		Configuration.setSearchOrder(
			Configuration.USER_TEMP, 
			Configuration.USER_CONF, 
			Configuration.SYSTEM_CONF);
		
		tConf.setProperty("tmp1",    "-tmp1-");
		uConf.setProperty("user1",   "-user1-");
		sConf.setProperty("system1", "-system1-");

		tConf.setProperty("prop1", "-prop1-tmp-");
		uConf.setProperty("prop1", "-prop1-user-");
		sConf.setProperty("prop1", "-prop1-system-");

		
		tConf.setProperty("prop2.p1", "-tmp-prop2.p1-");
		tConf.setProperty("prop2.p2", "-tmp-prop2.p2-");
		tConf.setProperty("prop2.p3", "-tmp-prop2.p3-");
		
		uConf.setProperty("prop2.p2", "-user-prop2.p2-");
		uConf.setProperty("prop2.p3", "-user-prop2.p3-");
		uConf.setProperty("prop2.p4", "-user-prop2.p4-");
		
		sConf.setProperty("prop2.p1", "-system-prop2.p1-");
		sConf.setProperty("prop2.p3", "-system-prop2.p3-");
		sConf.setProperty("prop2.p5", "-system-prop2.p5-");

		tConf.setProperty("udc.prop2.p1", "-tmp-udc.prop2.p1-");
		tConf.setProperty("udc.prop2.p2", "-tmp-udc.prop2.p2-");
		tConf.setProperty("udc.prop2.p3", "-tmp-udc.prop2.p3-");
		
		uConf.setProperty("udc.prop2.p2", "-user-udc.prop2.p2-");
		uConf.setProperty("udc.prop2.p3", "-user-udc.prop2.p3-");
		uConf.setProperty("udc.prop2.p4", "-user-udc.prop2.p4-");
		
		sConf.setProperty("udc.prop2.p1", "-system-udc.prop2.p1-");
		sConf.setProperty("udc.prop2.p3", "-system-udc.prop2.p3-");
		sConf.setProperty("udc.prop2.p5", "-system-udc.prop2.p5-");

		Configuration cfg = Configuration.getCombinedConfiguration();
		System.out.println("tmp      FILENAME='"+tConf.getFilename()+"'.");
		System.out.println("user     FILENAME='"+uConf.getFilename()+"'.");
		System.out.println("system   FILENAME='"+sConf.getFilename()+"'.");
		System.out.println("Combined FILENAME='"+cfg.getFilename()+"'.");
		System.out.println();
		System.out.println("TMP:    tmp1    = '"+tConf.getProperty("tmp1")+"'.");
		System.out.println("USER:   user1   = '"+uConf.getProperty("user1")+"'.");
		System.out.println("SYSTEM: system1 = '"+sConf.getProperty("system1")+"'.");
		System.out.println();
		System.out.println("notFound= '"+cfg.getProperty("notFound")+"'.");
		System.out.println("tmp1    = '"+cfg.getProperty("tmp1")+"'.");
		System.out.println("user1   = '"+cfg.getProperty("user1")+"'.");
		System.out.println("system1 = '"+cfg.getProperty("system1")+"'.");
		System.out.println();
		System.out.println("prop1 = '"+cfg.getProperty("prop1")+"'.");
		System.out.println();
		System.out.println("prop2.p1 = '"+cfg.getProperty("prop2.p1")+"'.");
		System.out.println("prop2.p2 = '"+cfg.getProperty("prop2.p2")+"'.");
		System.out.println("prop2.p3 = '"+cfg.getProperty("prop2.p3")+"'.");
		System.out.println("prop2.p4 = '"+cfg.getProperty("prop2.p4")+"'.");
		System.out.println("prop2.p5 = '"+cfg.getProperty("prop2.p5")+"'.");
		System.out.println();
		System.out.println("getKeys('prop2.')     = '"+cfg.getKeys("prop2.")+"'.");
		System.out.println("getKeys('udc.prop2.') = '"+cfg.getKeys("udc.prop2.")+"'.");
		System.out.println();
		System.out.println("getUniqueSubKeys(TMP: 'udc.prop2.',true)  = '"+tConf.getUniqueSubKeys("udc.prop2.",true)+"'.");
		System.out.println("getUniqueSubKeys(TMP: 'udc.prop2.',false) = '"+tConf.getUniqueSubKeys("udc.prop2.",false)+"'.");
		System.out.println();
		System.out.println("getUniqueSubKeys('prop2.',true)  = '"+cfg.getUniqueSubKeys("prop2.",true)+"'.");
		System.out.println("getUniqueSubKeys('prop2.',false) = '"+cfg.getUniqueSubKeys("prop2.",false)+"'.");

		
		
		
		// Register Application Default
		System.out.println();
		System.out.println("--- Registered Application Defaults --------");
		Configuration.registerDefaultValue("xxx.str",       "str");
		Configuration.registerDefaultValue("xxx.int",       Integer.MAX_VALUE);
		Configuration.registerDefaultValue("xxx.long",      Long.MAX_VALUE);
		Configuration.registerDefaultValue("xxx.boolean.1", Boolean.TRUE);
		Configuration.registerDefaultValue("xxx.boolean.2", Boolean.TRUE);
//		Configuration.registerDefaultValue("xxx.boolean.3", Boolean.TRUE); // NOT REGISTERED

		System.out.println("RegisteredDefaultValue: xxx.str       = '" + Configuration.getRegisteredDefaultValue("xxx.str")       + "'.");
		System.out.println("RegisteredDefaultValue: xxx.int       = '" + Configuration.getRegisteredDefaultValue("xxx.int")       + "'.");
		System.out.println("RegisteredDefaultValue: xxx.long      = '" + Configuration.getRegisteredDefaultValue("xxx.long")      + "'.");
		System.out.println("RegisteredDefaultValue: xxx.boolean.1 = '" + Configuration.getRegisteredDefaultValue("xxx.boolean.1") + "'.");
		System.out.println("RegisteredDefaultValue: xxx.boolean.2 = '" + Configuration.getRegisteredDefaultValue("xxx.boolean.2") + "'.");
		System.out.println("RegisteredDefaultValue: xxx.boolean.3 = '" + Configuration.getRegisteredDefaultValue("xxx.boolean.3") + "'  ------- NOT REGISTERED -------");


		System.out.println("--- SOME DEFAULT VALUES --------");
		tConf.setProperty("xxx.password",  "someSecretPassword", true); // encrypted
		tConf.setProperty("xxx.str",       "str");
		tConf.setProperty("xxx.int",       Integer.MAX_VALUE);
		tConf.setProperty("xxx.long",      Long.MAX_VALUE);
		tConf.setProperty("xxx.boolean.1", Boolean.TRUE);
//		tConf.setProperty("xxx.boolean.2", Boolean.TRUE); // DO NOT SET THIS VALUE

		System.out.println("xxx.password  = '" + tConf.getProperty("xxx.password")    + "'.");
		System.out.println("xxx.str       = '" + tConf.getProperty("xxx.str")         + "'.");
		System.out.println("xxx.int       = '" + tConf.getIntProperty("xxx.int")      + "'.");
		System.out.println("xxx.long      = '" + tConf.getLongProperty("xxx.long")    + "'.");
		System.out.println("xxx.boolean.1 = '" + tConf.getBooleanProperty("xxx.boolean.1", true) + "'.");
		System.out.println("xxx.boolean.2 = '" + tConf.getBooleanProperty("xxx.boolean.2", false) + "'.");
		System.out.println("xxx.boolean.3 = '" + tConf.getBooleanProperty("xxx.boolean.3", false) + "'.");

		System.out.println("RAW: xxx.password  = '" + tConf.getPropertyRaw_test("xxx.password")  + "'.");
		System.out.println("RAW: xxx.str       = '" + tConf.getPropertyRaw_test("xxx.str")       + "'.");
		System.out.println("RAW: xxx.int       = '" + tConf.getPropertyRaw_test("xxx.int")       + "'.");
		System.out.println("RAW: xxx.long      = '" + tConf.getPropertyRaw_test("xxx.long")      + "'.");
		System.out.println("RAW: xxx.boolean.1 = '" + tConf.getPropertyRaw_test("xxx.boolean.1") + "'.");
		System.out.println("RAW: xxx.boolean.2 = '" + tConf.getPropertyRaw_test("xxx.boolean.2") + "'.");
		System.out.println("RAW: xxx.boolean.3 = '" + tConf.getPropertyRaw_test("xxx.boolean.3") + "'.");

		testShouldHaveValue(tConf.getProperty("xxx.password"));
		testShouldHaveValue(tConf.getProperty("xxx.str"));
		testShouldHaveValue(tConf.getProperty("xxx.int"));
		testShouldHaveValue(tConf.getProperty("xxx.long"));
		testShouldHaveValue(tConf.getProperty("xxx.boolean.1"));
		testShouldHaveValue(tConf.getProperty("xxx.boolean.2"));
		testShouldBeNull   (tConf.getProperty("xxx.boolean.3"));

		testShouldBeEqual("someSecretPassword", tConf.getProperty       ("xxx.password"));
		testShouldBeEqual("str",                tConf.getProperty       ("xxx.str"));
		testShouldBeEqual(Integer.MAX_VALUE,    tConf.getIntProperty    ("xxx.int"));
		testShouldBeEqual(Long.MAX_VALUE,       tConf.getLongProperty   ("xxx.long"));
		testShouldBeEqual(Boolean.TRUE,         tConf.getBooleanProperty("xxx.boolean.1", true));
		testShouldBeEqual(Boolean.TRUE,         tConf.getBooleanProperty("xxx.boolean.2", false)); // in RepAppDef, not in Props, use RepAppDef(true)
		testShouldBeEqual(Boolean.FALSE,        tConf.getBooleanProperty("xxx.boolean.3", false)); // Not in RepAppDef, use default value

		testShouldNotBeEqual("someSecretPassword",       tConf.getPropertyRaw_test("xxx.password"));
		testShouldBeEqual(USE_DEFAULT+"str",             tConf.getPropertyRaw_test("xxx.str"));
		testShouldBeEqual(USE_DEFAULT+Integer.MAX_VALUE, tConf.getPropertyRaw_test("xxx.int"));
		testShouldBeEqual(USE_DEFAULT+Long.MAX_VALUE,    tConf.getPropertyRaw_test("xxx.long"));
		testShouldBeEqual(USE_DEFAULT+Boolean.TRUE,      tConf.getPropertyRaw_test("xxx.boolean.1"));
		testShouldBeNull(                                tConf.getPropertyRaw_test("xxx.boolean.2"));
		testShouldBeNull(                                tConf.getPropertyRaw_test("xxx.boolean.3"));

		
		System.out.println("--- SOME NON DEFAULT VALUES --------");
		tConf.setProperty("xxx.password",  "testPasswd", true); // encrypted
		tConf.setProperty("xxx.str",       "another str");
		tConf.setProperty("xxx.int",       1);
		tConf.setProperty("xxx.long",      1L);
		tConf.setProperty("xxx.boolean.1", Boolean.FALSE);

		System.out.println("xxx.password  = '" + tConf.getProperty       ("xxx.password")         + "'.");
		System.out.println("xxx.str       = '" + tConf.getProperty       ("xxx.str")              + "'.");
		System.out.println("xxx.int       = '" + tConf.getIntProperty    ("xxx.int")              + "'.");
		System.out.println("xxx.long      = '" + tConf.getLongProperty   ("xxx.long")             + "'.");
		System.out.println("xxx.boolean.1 = '" + tConf.getBooleanProperty("xxx.boolean.1", true)  + "'.");
		System.out.println("xxx.boolean.2 = '" + tConf.getBooleanProperty("xxx.boolean.2", false) + "'.");
		System.out.println("xxx.boolean.3 = '" + tConf.getBooleanProperty("xxx.boolean.3", false) + "'.");

		System.out.println("RAW: xxx.password  = '" + tConf.getPropertyRaw_test("xxx.password")  + "'.");
		System.out.println("RAW: xxx.str       = '" + tConf.getPropertyRaw_test("xxx.str")       + "'.");
		System.out.println("RAW: xxx.int       = '" + tConf.getPropertyRaw_test("xxx.int")       + "'.");
		System.out.println("RAW: xxx.long      = '" + tConf.getPropertyRaw_test("xxx.long")      + "'.");
		System.out.println("RAW: xxx.boolean.1 = '" + tConf.getPropertyRaw_test("xxx.boolean.1") + "'.");
		System.out.println("RAW: xxx.boolean.2 = '" + tConf.getPropertyRaw_test("xxx.boolean.2") + "'.");
		System.out.println("RAW: xxx.boolean.3 = '" + tConf.getPropertyRaw_test("xxx.boolean.3") + "'.");

		testShouldHaveValue(tConf.getProperty("xxx.password"));
		testShouldHaveValue(tConf.getProperty("xxx.str"));
		testShouldHaveValue(tConf.getProperty("xxx.int"));
		testShouldHaveValue(tConf.getProperty("xxx.long"));
		testShouldHaveValue(tConf.getProperty("xxx.boolean.1"));
		testShouldHaveValue(tConf.getProperty("xxx.boolean.2"));
		testShouldBeNull   (tConf.getProperty("xxx.boolean.3"));

		testShouldBeEqual("testPasswd",      tConf.getProperty       ("xxx.password"));
		testShouldBeEqual("another str",     tConf.getProperty       ("xxx.str"));
		testShouldBeEqual(1,                 tConf.getIntProperty    ("xxx.int"));
		testShouldBeEqual(1L,                tConf.getLongProperty   ("xxx.long"));
		testShouldBeEqual(Boolean.FALSE,     tConf.getBooleanProperty("xxx.boolean.1", true));
		testShouldBeEqual(Boolean.TRUE,      tConf.getBooleanProperty("xxx.boolean.2", false)); // in RepAppDef, not in Props, use RepAppDef(true)
		testShouldBeEqual(Boolean.FALSE,     tConf.getBooleanProperty("xxx.boolean.3", false)); // Not in RepAppDef

		testShouldNotBeEqual("testPasswd",                         tConf.getPropertyRaw_test("xxx.password"));
		testShouldBeEqual("another str",                           tConf.getPropertyRaw_test("xxx.str"));
		testShouldBeEqual(1,                 Integer.parseInt(     tConf.getPropertyRaw_test("xxx.int")));
		testShouldBeEqual(1L,                Long   .parseLong(    tConf.getPropertyRaw_test("xxx.long")));
		testShouldBeEqual(Boolean.FALSE,     Boolean.parseBoolean( tConf.getPropertyRaw_test("xxx.boolean.1")));
		testShouldBeNull(                                          tConf.getPropertyRaw_test("xxx.boolean.2"));
		testShouldBeNull(                                          tConf.getPropertyRaw_test("xxx.boolean.3"));
	}

	private static void testShouldBeNull(Object v1)
	{
		if (v1 != null) 
			throw new RuntimeException("testShouldBeNull: Object SHOULD be NULL, it has value='"+v1+"'.");
	}
	private static void testShouldHaveValue(Object v1)
	{
		if (v1 == null) 
			throw new RuntimeException("testShouldHaveValue: Object should HAVE value, it's null");
	}
	private static void testShouldBeEqual(Object v1, Object v2)
	{
		if (v1 == null && v2 == null)
			return;
		if (v1 == null && v2 != null) throw new RuntimeException("testEqual: Values are NOT equal. v1='"+v1+"', v2='"+v2+"'");
		if (v2 == null && v1 != null) throw new RuntimeException("testEqual: Values are NOT equal. v1='"+v1+"', v2='"+v2+"'");
		if (! v1.equals(v2))          throw new RuntimeException("testEqual: Values are NOT equal. v1='"+v1+"', v2='"+v2+"', v1='"+v1.getClass().getName()+"', v2='"+v2.getClass().getName()+"'.");
	}
	private static void testShouldNotBeEqual(Object v1, Object v2)
	{
		if (v1 == v2)
			return;
		if (v1 == null && v2 != null) return;
		if (v2 == null && v1 != null) return;
		if (v1.equals(v2))            throw new RuntimeException("testNotEqual: Values ARE equal. v1='"+v1+"', v2='"+v2+"', v1='"+v1.getClass().getName()+"', v2='"+v2.getClass().getName()+"'.");
	}
}
