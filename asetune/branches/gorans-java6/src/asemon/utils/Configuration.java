/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;




//Deleted file:
//----------------------------------------
// file: IAsemonPropsWriter.java
//----------------------------------------
//package asemon;
//
///**
// * Iterface that implemets a "writer" for a subsection of a property file.
// *
// * @author gorans
// */
//public interface IAsemonPropsWriter
//{
//	public StringBuffer propsWriter();
//}
//----------------------------------------

public class Configuration
extends Properties
{
	/*---------------------------------------------------
	** class members
	**---------------------------------------------------
	*/
    private static final long serialVersionUID = 5707562050158600080L;

    private static final String ENCRYPTED_PREFIX = "encrypted:";


    public static final String CONF = "CONF"; 
    public static final String TEMP = "TEMP"; 
    public static final String PCS  = "PCS"; 
    
    /** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(Configuration.class);

	// implements singleton pattern
//	private static Configuration _instance = null;
	private static HashMap<String,Configuration> _instMap  = new HashMap<String,Configuration>();

	private String _propFileName = null;
	private boolean _saveOnExit = false;

//	private List _writers = new ArrayList();

	// original serialVersionUID = 5707562050158600080L
	private static String encrypterBaseKey = "qazZSE44wsxXDR55"+serialVersionUID+"edcCFT66rfvVGY77";
//	private static Encrypter baseEncrypter = new Encrypter(encrypterBaseKey);

	private String _embeddedMessage = "This file will be overwritten and maintained by asemon";


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
			_logger.warn("Cant find any configuration named '"+confName+"', creating a new one.");
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
		_instMap.put(confName, configuration);
	}

//	public void addWriter(IAsemonPropsWriter writer)
//	{
//		_writers.add(writer);
//	}
//
//	public void removeWriter(IAsemonPropsWriter writer)
//	{
//		_writers.remove(writer);
//	}

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

	public void save()
	{
		if (_propFileName == null)
		{
			_logger.debug("No filename has been assigned to this property file, cant save...");
			return;
		}

		try
		{
//			StringBuffer sb = new StringBuffer();
//			sb.append("#=======================================================\n");
//			sb.append("# This file will be overwritten and maintained by asemon\n");
//			sb.append("# Last save time: "+new Date().toString()+"\n");
//			sb.append("#-------------------------------------------------------\n");
//			sb.append("\n\n\n");
//
//			BufferedWriter bufferedwriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(_propFileName)), "8859_1"));
//			Iterator i = _writers.iterator();
//			while (i.hasNext())
//			{
//				IAsemonPropsWriter w = (IAsemonPropsWriter) i.next();
//
//				sb.append( w.propsWriter() );
//				sb.append("\n\n\n");
//			}
//
//			bufferedwriter.write(sb.toString());
//			bufferedwriter.flush();

			FileOutputStream os = new FileOutputStream(new File(_propFileName));
			store(os, getEmbeddedMessage());
			//super.storeToXML(os, "This file will be overwritten and maintained by asemon");
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


//	public void removeAll(String prefix)
//	{
//		Enumeration e = this.keys();
//		while (e.hasMoreElements())
//		{
//			String key = (String) e.nextElement();
//
//			if (key.startsWith(prefix))
//			{
//				remove(key);
//			}
//		}
//	}
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
	 * @return An Enumeration of keys that match the prefix.
	 */
//	public Enumeration getKeys(String prefix)
//	{
//		Enumeration keys = this.keys();
//		Vector matchingKeys = new Vector();
//
//		while (keys.hasMoreElements())
//		{
//			Object key = keys.nextElement();
//			if (key instanceof String && ((String) key).startsWith(prefix))
//			{
//				matchingKeys.addElement(key);
//			}
//		}
//		Collections.sort(matchingKeys);
//		return matchingKeys.elements();
//	}
	public List<String> getKeys(String prefix)
	{
		List<String> matchingKeys = new ArrayList<String>();

		for (Iterator<Object> it = this.keySet().iterator(); it.hasNext();)
		{
			String key = (String) it.next();
			if (key.startsWith(prefix))
			{
				matchingKeys.add(key);
			}
		}
		Collections.sort(matchingKeys);
		return matchingKeys;
	}

//	/**
//	 * Get the list of unique sub-keys contained in the configuration repository that
//	 * match the specified prefix.
//	 * 
//	 * @param prefix The property prefix to test against.
//	 * @param keepPrefix should the return values consist of the prefix 
//	 *        string + the next key value or just the key value itself.
//	 * @return An Enumeration of keys that match the prefix.
//	 */
//	public Enumeration getUniqueSubKeys(String prefix, boolean keepPrefix)
//	{
//		Vector uniqueNames = new Vector();
//
//		// Compose a list of unique prefix.xxxx. strings
//		Enumeration e = getKeys(prefix);
//		while(e.hasMoreElements())
//		{
//			String key = (String) e.nextElement();
//
//			String name;
//			if (keepPrefix)
//				name = key.substring(0, key.indexOf(".", prefix.length()));
//			else
//				name = key.substring(prefix.length(), key.indexOf(".", prefix.length()));
//				
//			if ( ! uniqueNames.contains(name) )
//			{
//				uniqueNames.add(name);
//			}
//		}
//
//		Collections.sort(uniqueNames);
//		return uniqueNames.elements();
//	}
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
	 * @return List<String> keys that match the prefix.
	 */
	public List<String> getUniqueSubKeys(String prefix, boolean keepPrefix)
	{
		List<String> uniqueNames = new ArrayList<String>();

		// Compose a list of unique prefix.xxxx. strings
		for (String key : getKeys(prefix))
		{
			String name;
			if (keepPrefix)
				name = key.substring(0, key.indexOf(".", prefix.length()));
			else
				name = key.substring(prefix.length(), key.indexOf(".", prefix.length()));

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
//	public Map getPropertyValuesInSortedList(String propPrefix)
//	{
//		LinkedHashMap dest = new LinkedHashMap();
//
//		// Put all the keys that starts with the prefix in a list
//		List tmpKeyList = new LinkedList();
//		Enumeration keys = this.getKeys(propPrefix);
//		while (keys.hasMoreElements())
//		{
//			String key = (String) keys.nextElement();
//
//			// Add KEY to tmpKeyList, this so we later can sort it and add
//			// values to the "real" check list in the "correct order"
//			_logger.debug("tmpKeyList.add: key='"+key+"'.");
//			tmpKeyList.add(key);
//		}
//		// Get the keys, sort them, add them in a sorted manner to the list: destList
//		if (tmpKeyList.size() > 0)
//		{
//			Collections.sort(tmpKeyList);
//			for (Iterator iter = tmpKeyList.iterator(); iter.hasNext();)
//			{
//				String key = (String) iter.next();
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
	public Map<String,String> getPropertyValuesInSortedList(String propPrefix)
	{
		LinkedHashMap<String,String> dest = new LinkedHashMap<String,String>();

		// Put all the keys that starts with the prefix in a list
		List<String> tmpKeyList = new LinkedList<String>();
		for (String key : this.getKeys(propPrefix))
		{
			// Add KEY to tmpKeyList, this so we later can sort it and add
			// values to the "real" check list in the "correct order"
			_logger.debug("tmpKeyList.add: key='"+key+"'.");
			tmpKeyList.add(key);
		}

		// Get the keys, sort them, add them in a sorted manner to the list: destList
		if (tmpKeyList.size() > 0)
		{
			Collections.sort(tmpKeyList);
			for (String key : tmpKeyList)
			{
				String val = this.getProperty(key);

				String keyLastWord = key.substring( key.lastIndexOf('.')+1 );
				//val = keyLastWord + ":" + val;

				// Now add the value to the check list.
				_logger.debug("destList.add: val='"+val+"'.");
				dest.put(keyLastWord, val);
			}
		}

		return dest;
	}


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
		String val = getProperty(propName, Integer.toString(defaultValue));
		return Integer.parseInt(val);
	}
	/** Get a int value for property */
	public int getIntProperty(String propName, String defaultValue)
	{
		String val = getProperty(propName, defaultValue);
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
		String val = getProperty(propName, Long.toString(defaultValue));
		return Long.parseLong(val);
	}
	/** Get a long value for property */
	public long getLongProperty(String propName, String defaultValue)
	{
		String val = getProperty(propName, defaultValue);
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
		String val = super.getProperty(propName);
		if (val == null)
			throw new MandatoryPropertyException("The property '"+propName+"' is mandatory.");
		val = val.trim();
		return parseProperty( propName, val );
	}

	/** Get a String value for property */
	public String getProperty(String propName)
	{
		String val = super.getProperty(propName);
		if (val != null)
			val = val.trim();
		return parseProperty( propName, val );
	}

	/** Get a String value for property */
	public String getProperty(String propName, String defaultValue)
	{
		String val = super.getProperty(propName, defaultValue);
		if (val != null)
			val = val.trim();
		return parseProperty( propName, val );
	}

	
	
	/** Get a String value for property */
	public String getMandatoryPropertyRaw(String propName)
	throws MandatoryPropertyException
	{
		String val = super.getProperty(propName);
		if (val == null)
			throw new MandatoryPropertyException("The property '"+propName+"' is mandatory.");
		val = val.trim();
		return val;
	}

	/** Get a String value for property */
	public String getPropertyRaw(String propName)
	{
		String val = super.getProperty(propName);
		if (val != null)
			val = val.trim();
		return val;
	}

	/** Get a String value for property */
	public String getPropertyRaw(String propName, String defaultValue)
	{
		String val = super.getProperty(propName, defaultValue);
		if (val != null)
			val = val.trim();
		return val;
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
		Encrypter propEncrypter = new Encrypter(encrypterBaseKey+propName);
		String encryptedStr = propEncrypter.encrypt(str);
		return ENCRYPTED_PREFIX + encryptedStr;
	}

	public Object setEncrypedProperty(String propName, String str)
	{
		return super.setProperty( propName, encryptPropertyValue(propName, str) );
	}

	public Object setProperty(String propName, String str)
	{
		return super.setProperty( propName, str );
	}

	public int setProperty(String propName, int t)
	{
		Object prev = setProperty( propName, Integer.toString(t) );
		return prev==null ? -1 : Integer.parseInt( (String)prev );
	}

	public long setProperty(String propName, long l)
	{
		Object prev = setProperty( propName, Long.toString(l) );
		return prev==null ? -1 : Long.parseLong( (String)prev );
	}

	public boolean setProperty(String propName, boolean b)
	{
		Object prev = setProperty( propName, Boolean.toString(b) );
		return prev==null ? false : Boolean.parseBoolean( (String)prev );
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
				_logger.warn("The Environment variable '"+envName+"' cant be found, replacing it with an empty string ''.");
				envVal="";
			}
			// Backslashes does not work that good in replaceFirst()...
			// So change them to / instead...
			envVal = envVal.replace('\\', '/');

			_logger.debug("The Environment variable '"+envName+"' will be substituted with the value of '"+envVal+"'.");

			// NOW substityte the ENVVARIABLE with a real value...
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
			_logger.error("Problems when executing the OS Command '"+osCmdStr+"'. Cought: "+e);
			return e.toString();
		}

	}




	// Hopefully this is kicked of when the JVM dies aswell...
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
				sectionStr = key.substring(0, key.indexOf("."));
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

	/*
	 * Converts unicodes to encoded &#92;uxxxx and escapes special characters
	 * with a preceding slash
	 */
	private String saveConvert(String theString, boolean escapeSpace, boolean escapeUnicode)
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

	/////////////////////////////////////////////////////////////
	// main / test code.
	/////////////////////////////////////////////////////////////

//	/**
//	 * @param args
//	 */
//	public static void main(String[] args)
//	{
//	}

}
