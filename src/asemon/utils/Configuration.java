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
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.Vector;
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
	private static HashMap       _instMap  = new HashMap();

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
		Configuration conf = (Configuration)_instMap.get(confName);
		if ( conf == null )
		{
			_logger.warn("Cant find any configuration named '"+confName+"', creating a new one.");
			conf = new Configuration();
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


	public void removeAll(String prefix)
	{
		Enumeration e = this.keys();
		while (e.hasMoreElements())
		{
			String key = (String) e.nextElement();

			if (key.startsWith(prefix))
			{
				remove(key);
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
	public Enumeration getKeys(String prefix)
	{
		Enumeration keys = this.keys();
		Vector matchingKeys = new Vector();

		while (keys.hasMoreElements())
		{
			Object key = keys.nextElement();
			if (key instanceof String && ((String) key).startsWith(prefix))
			{
				matchingKeys.addElement(key);
			}
		}
		Collections.sort(matchingKeys);
		return matchingKeys.elements();
	}

	/**
	 * Get the list of unique sub-keys contained in the configuration repository that
	 * match the specified prefix.
	 * 
	 * @param prefix The property prefix to test against.
	 * @param keepPrefix should the return values consist of the prefix 
	 *        string + the next key value or just the key value itself.
	 * @return An Enumeration of keys that match the prefix.
	 */
	public Enumeration getUniqueSubKeys(String prefix, boolean keepPrefix)
	{
		Vector uniqueNames = new Vector();

		// Compose a list of unique prefix.xxxx. strings
		Enumeration e = getKeys(prefix);
		while(e.hasMoreElements())
		{
			String key = (String) e.nextElement();

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
		return uniqueNames.elements();
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
	public Map getPropertyValuesInSortedList(String propPrefix)
	{
		LinkedHashMap dest = new LinkedHashMap();

		// Put all the keys that starts with the prefix in a list
		List tmpKeyList = new LinkedList();
		Enumeration keys = this.getKeys(propPrefix);
		while (keys.hasMoreElements())
		{
			String key = (String) keys.nextElement();

			// Add KEY to tmpKeyList, this so we later can sort it and add
			// values to the "real" check list in the "correct order"
			_logger.debug("tmpKeyList.add: key='"+key+"'.");
			tmpKeyList.add(key);
		}
		// Get the keys, sort them, add them in a sorted manner to the list: destList
		if (tmpKeyList.size() > 0)
		{
			Collections.sort(tmpKeyList);
			for (Iterator iter = tmpKeyList.iterator(); iter.hasNext();)
			{
				String key = (String) iter.next();
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



	/** Get a int value for property */
	public int getIntMandatoryProperty(String propName)
	throws MandatoryPropertyException
	{
		String val = getProperty(propName);
		if (val == null)
			throw new MandatoryPropertyException("The property '"+propName+"' is mandatory.");
		return Integer.parseInt(val);
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

		String s1;
		String s2;

		String sectionStr = "";
		String sectionStrSave = "";

		for (Iterator it = new TreeMap(this).keySet().iterator();
			it.hasNext();
			writeln(bw, (new StringBuffer()).append(s1).append("=").append(s2).toString()))
//			writeln(bw, (new StringBuilder()).append(s1).append("=").append(s2).toString()))
		{
			s1 = (String) it.next();
			s2 = (String) get(s1);
			s1 = saveConvert(s1, true);
			s2 = saveConvert(s2, false);

			// Write a new-line after every property "group"
			sectionStr = s1.substring(0, s1.indexOf("."));
			if ( ! sectionStr.equals(sectionStrSave))
			{
				writeln(bw, "");
				sectionStrSave = sectionStr;
			}
		}

		//		for (Enumeration enumeration = keys(); enumeration.hasMoreElements(); writeln(bufferedwriter, (new StringBuilder()).append(s1).append("=").append(s2).toString()))
//		{
//			s1 = (String) enumeration.nextElement();
//			s2 = (String) get(s1);
//			s1 = saveConvert(s1, true);
//			s2 = saveConvert(s2, false);
//		}

		bw.flush();
	}

	/** code stolen from Properties */
	private static void writeln(BufferedWriter bufferedwriter, String s) throws IOException
	{
		bufferedwriter.write(s);
		bufferedwriter.newLine();
	}

	/** code stolen from Properties */
	private String saveConvert(String s, boolean flag)
	{
		int i = s.length();
		int j = i * 2;
		if (j < 0)
			j = 2147483647;
		StringBuffer stringbuffer = new StringBuffer(j);
		for (int k = 0; k < i; k++)
		{
			char c = s.charAt(k);
			if (c > '=' && c < '\177')
			{
				if (c == '\\')
				{
					stringbuffer.append('\\');
					stringbuffer.append('\\');
				}
				else
				{
					stringbuffer.append(c);
				}
				continue;
			}
			switch (c)
			{
			case 32: // ' '
				if (k == 0 || flag)
					stringbuffer.append('\\');
				stringbuffer.append(' ');
				break;

			case 9: // '\t'
				stringbuffer.append('\\');
				stringbuffer.append('t');
				break;

			case 10: // '\n'
				stringbuffer.append('\\');
				stringbuffer.append('n');
				break;

			case 13: // '\r'
				stringbuffer.append('\\');
				stringbuffer.append('r');
				break;

			case 12: // '\f'
				stringbuffer.append('\\');
				stringbuffer.append('f');
				break;

			case 33: // '!'
			case 35: // '#'
			case 58: // ':'
			case 61: // '='
				stringbuffer.append('\\');
				stringbuffer.append(c);
				break;

			default:
				if (c < ' ' || c > '~')
				{
					stringbuffer.append('\\');
					stringbuffer.append('u');
					stringbuffer.append(toHex(c >> 12 & 15));
					stringbuffer.append(toHex(c >> 8 & 15));
					stringbuffer.append(toHex(c >> 4 & 15));
					stringbuffer.append(toHex(c & 15));
				}
				else
				{
					stringbuffer.append(c);
				}
				break;
			}
		}

		return stringbuffer.toString();
	}

	/** code stolen from Properties */
	private static final char	hexDigit[]	     = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
	private static char toHex(int i)
	{
		return hexDigit[i & 15];
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
